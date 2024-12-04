package net.biesemeyer.funlock;

import com.google.common.collect.MapMaker;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

abstract class AbstractWeakrefRegistry<K, L> {
    private final ConcurrentMap<K, L> map = new MapMaker().weakKeys().makeMap();
    private final Supplier<L> lockFactory;

    protected AbstractWeakrefRegistry(Class<K> keyClass, Supplier<L> lockFactory) {
        // Prevent usage where GC is unlikely to reclaim keys, or is likely to claim the auto-box around a primitive key
        if (keyClass.isPrimitive()) { throw new IllegalArgumentException("key type must not be primitive"); }
        if (keyClass.getPackageName().startsWith("java.")) { throw new IllegalArgumentException("key type must be from package outside Java core"); }

        this.lockFactory = lockFactory;
    }

    protected L get(K key) {
        return map.computeIfAbsent(key, (k) -> {
            return lockFactory.get();
        });
    }

    protected <R, E extends Exception> R getWithInstance(K key, Checked.Function<L, R, E> lockHandler) throws E {
        return lockHandler.apply(get(key));
    }

    protected <E extends Exception> void runWithInstance(K key, Checked.Consumer<L, E> lockHandler) throws E {
        lockHandler.accept(get(key));
    }

    boolean containsValue(L value) {
        return map.containsValue(value);
    }
}
