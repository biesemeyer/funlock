package net.biesemeyer.funlock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * A {@code FunctionalReadWriteLock} is a functional API on top of a traditional {@link ReadWriteLock},
 * freeing the caller from the details of acquiring and separately freeing the locks in question.
 */
public class FunctionalReadWriteLock {

    public static <K> WeakrefRegistry<K> createWeakrefRegistry(final Class<K> keyClass) {
        return createWeakrefRegistry(keyClass, DEFAULT_LOCK_FACTORY);
    }
    public static <K> WeakrefRegistry<K> createWeakrefRegistry(final Class<K> keyClass, Supplier<ReadWriteLock> rwLockSupplier) {
        return new WeakrefRegistry<>(keyClass, rwLockSupplier);
    }
    private static final Supplier<ReadWriteLock> DEFAULT_LOCK_FACTORY = ReentrantReadWriteLock::new;

    private final Lock readLock;
    private final Lock writeLock;

    public FunctionalReadWriteLock() {
        this(DEFAULT_LOCK_FACTORY.get());
    }

    FunctionalReadWriteLock(final ReadWriteLock readWriteLock) {
        this.writeLock = readWriteLock.writeLock();
        this.readLock = readWriteLock.readLock();
    }

    /**
     * Run code while holding a read lock.
     *
     * @param runnable produces a value while holding a read lock
     * @param <E> the exception type of the checked supplier
     * @throws E an exception thrown by the checked supplier
     */
    public <E extends Exception> void runWithReadLock(final Checked.Runnable<E> runnable) throws E {
        this.getWithReadLock(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Produce a result while holding a read lock.
     *
     * @param supplier produces a value while holding a read lock
     * @return the result of the checked supplier
     * @param <T> the return type of the checked supplier
     * @param <E> the exception type of the checked supplier
     * @throws E an exception thrown by the checked supplier
     */
    public <T, E extends Exception> T getWithReadLock(final Checked.Supplier<T,E> supplier) throws E {
        return getWithSpecificLock(this.readLock, supplier);
    }

    /**
     * Run code while holding a write lock.
     *
     * @param runnable produces a value while holding a read lock
     * @param <E> the exception type of the checked supplier
     * @throws E an exception thrown by the checked supplier
     */
    public <E extends Exception> void runWithWriteLock(final Checked.Runnable<E> runnable) throws E {
        getWithWriteLock(()->{
            runnable.run();
            return null;
        });
    }

    /**
     * Produce a result while holding a write lock.
     *
     * @param supplier produces a value while holding a write lock
     * @return the result of the checked supplier
     * @param <T> the return type of the checked supplier
     * @param <E> the exception type of the checked supplier
     * @throws E an exception thrown by the checked supplier
     */
    public <T, E extends Exception> T getWithWriteLock(final Checked.Supplier<T,E> supplier) throws E {
        return getWithSpecificLock(this.writeLock, supplier);
    }

    /**
     * Run code while holding a write lock, with the option to <em>downgrade</em> to a read-lock before
     * execution is complete. If the lock was already held prior to beginning, downgrading has no net effect.
     *
     * @param controlConsumer produces a value while holding a read lock
     * @param <E>             the exception type of the checked supplier
     * @throws E an exception thrown by the checked supplier
     */
    public <E extends Exception> void runWithWriteLock(final CheckedRunnableSupportsDowngrade<E> controlConsumer) throws E {
        getWithWriteLock((downgrader) -> {
            controlConsumer.run(downgrader);
            return null;
        });
    }

    /**
     * Produce a result while holding a write lock, with the option to <em>downgrade</em> to a read-lock before
     * production of the result is complete. If the lock was already held prior to beginning, downgrading has no
     * net effect.
     *
     * @param controlFunction produces a value while holding a lock that <em>begins</em> as a write lock and may be downgraded with {@link Downgrader#downgrade()}.
     * @return the result of the checked supplier
     * @param <T> the return type of the checked supplier
     * @param <E> the exception type of the checked supplier
     * @throws E an exception thrown by the checked supplier
     */
    public <T, E extends Exception> T getWithWriteLock(final CheckedSupplierSupportsDowngrade<T,E> controlFunction) throws E {
        this.writeLock.lock();
        final AtomicBoolean isDowngraded = new AtomicBoolean();
        try {
            final Downgrader downgrader = () -> {
                if (isDowngraded.compareAndSet(false, true)) {
                    this.readLock.lock();
                    this.writeLock.unlock();
                }
            };
            return controlFunction.get(downgrader);
        } finally {
            if (isDowngraded.get()) {
                this.readLock.unlock();
            } else {
                this.writeLock.unlock();
            }
        }
    }

    private <T, E extends Exception> T getWithSpecificLock(final Lock lock, final Checked.Supplier<T, E> supplier) throws E{
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    public interface Downgrader {
        /**
         * Downgrades the currently-held write lock to become a read lock,
         * preventing other write locks from being acquired until the resulting
         * read lock is released.
         *
         * <p>If the lock implementation is reentrant and the current thread holds
         * more than one write lock,
         */
        void downgrade();
    }

    @FunctionalInterface
    public interface CheckedSupplierSupportsDowngrade<T, E extends Exception> {
        T get(Downgrader downgrader) throws E;
    }

    @FunctionalInterface
    public interface CheckedRunnableSupportsDowngrade<E extends Exception> {
        void run(Downgrader downgrader) throws E;
    }

    public static final class WeakrefRegistry<K> extends AbstractWeakrefRegistry<K, FunctionalReadWriteLock> {
        WeakrefRegistry(Class<K> keyClass, Supplier<ReadWriteLock> rwLockFactory) {
            super(keyClass, () -> new FunctionalReadWriteLock(rwLockFactory.get()));
        }

        public <E extends Exception> void runWithReadLockOn(final K key, final Checked.Runnable<E> runnable) throws E {
            super.runWithInstance(key, (rwLock -> rwLock.runWithReadLock(runnable)));
        }

        public <T, E extends Exception> T getWithReadLockOn(final K key, final Checked.Supplier<T, E> supplier) throws E {
            return super.getWithInstance(key, (rwLock -> rwLock.getWithReadLock(supplier)));
        }

        public <E extends Exception> void runWithWriteLockOn(final K key, final Checked.Runnable<E> runnable) throws E {
            super.runWithInstance(key, (rwLock -> rwLock.runWithWriteLock(runnable)));
        }

        public <T, E extends Exception> T getWithWriteLockOn(final K key, final Checked.Supplier<T, E> supplier) throws E {
            return super.getWithInstance(key, (rwLock -> rwLock.getWithWriteLock(supplier)));
        }

        public <E extends Exception> void runWithWriteLockOn(final K key, final CheckedRunnableSupportsDowngrade<E> runnable) throws E {
            super.runWithInstance(key, (rwLock -> rwLock.runWithWriteLock(runnable)));
        }

        public <T, E extends Exception> T getWithWriteLockOn(final K key, final CheckedSupplierSupportsDowngrade<T, E> supplier) throws E {
            return super.getWithInstance(key, (rwLock -> rwLock.getWithWriteLock(supplier)));
        }
    }
}
