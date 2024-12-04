# Funlock

Functional interface for locks in Java

 - `net.biesemeyer.funlock.FunctionalReadWriteLock`
   - instance methods (`run`/`get`)`With`(`Read`/`Write`)`Lock` are functional way of running code and possibly getting a result from code with the specified access
   - the `*WithWriteLock` methods can also yield a `Downgrader`, which can be used to downgrade a write-lock into a regular read-lock.
 - `net.biesemeyer.funlock.FunctionalReadWriteLock.WeakRefRegistry` is a registry of locks by their garbage-collectable keys that requires no pre-registration of those keys, and offers the same functionality.