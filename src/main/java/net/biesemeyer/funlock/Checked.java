package net.biesemeyer.funlock;

public final class Checked {
    private Checked() { }

    @FunctionalInterface
    public interface Consumer<T,E extends Exception> {
        void accept(T t) throws E;
    }

    @FunctionalInterface
    public interface Function<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    @FunctionalInterface
    public interface Runnable<E extends Exception> {
        void run() throws E;
    }

    @FunctionalInterface
    public interface Supplier<T,E extends Exception> {
        T get() throws E;
    }
}
