package net.biesemeyer.funlock;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class FunctionalReadWriteLockTest {

    private final Lock mockReadLock = Mockito.mock(Lock.class);
    private final Lock mockWriteLock = Mockito.mock(Lock.class);
    private final ReadWriteLock lock = Mockito.mock(ReadWriteLock.class);
    {
        Mockito.when(lock.readLock()).thenReturn(mockReadLock);
        Mockito.when(lock.writeLock()).thenReturn(mockWriteLock);
    }

    private final FunctionalReadWriteLock lockable = new FunctionalReadWriteLock(lock);

    @Test
    public void testGetWithReadLock() throws Exception {
        verify(mockWriteLock, never()).lock();
        verify(mockReadLock, never()).lock();
        final Object retVal = new Object();
        final Object result = lockable.getWithReadLock(() -> {
            verify(mockReadLock).lock();
            verify(mockReadLock, never()).unlock();
            return retVal;
        });
        verify(mockReadLock).unlock();
        verify(mockWriteLock, never()).lock();
        assertThat(result, is(sameInstance(retVal)));
    }

    @Test
    public void testRunWithReadLock() throws Exception {
        verify(mockWriteLock, never()).lock();
        verify(mockReadLock, never()).lock();
        lockable.runWithReadLock(() -> {
            verify(mockReadLock).lock();
            verify(mockReadLock, never()).unlock();
        });
        verify(mockReadLock).unlock();
        verify(mockWriteLock, never()).lock();
    }

    @Test
    public void testGetWithWriteLock() throws Exception {
        verify(mockReadLock, never()).lock();
        verify(mockWriteLock, never()).lock();
        final Object retVal = new Object();
        final Object result = lockable.getWithWriteLock(() -> {
            verify(mockWriteLock).lock();
            verify(mockWriteLock, never()).unlock();
            return retVal;
        });
        verify(mockWriteLock).unlock();
        verify(mockReadLock, never()).lock();
        assertThat(result, is(sameInstance(retVal)));
    }

    @Test
    public void testRunWithWriteLock() throws Exception {
        verify(mockReadLock, never()).lock();
        verify(mockWriteLock, never()).lock();
        lockable.runWithWriteLock(() -> {
            verify(mockWriteLock).lock();
            verify(mockWriteLock, never()).unlock();
        });
        verify(mockWriteLock).unlock();
        verify(mockReadLock, never()).lock();
    }

    @Test
    public void testGetWithWriteLockNoDowngrade() throws Exception {
        verify(mockReadLock, never()).lock();
        verify(mockWriteLock, never()).lock();
        final Object retVal = new Object();
        final Object result = lockable.getWithWriteLock((c) -> {
            verify(mockWriteLock).lock();
            verify(mockWriteLock, never()).unlock();
            return retVal;
        });
        verify(mockWriteLock).unlock();
        verify(mockReadLock, never()).lock();
        assertThat(result, is(sameInstance(retVal)));
    }

    @Test
    public void testRunWithWriteLockNoDowngrade() throws Exception {
        verify(mockReadLock, never()).lock();
        verify(mockWriteLock, never()).lock();
        lockable.runWithWriteLock((c) -> {
            verify(mockWriteLock).lock();
            verify(mockWriteLock, never()).unlock();
        });
        verify(mockWriteLock).unlock();
        verify(mockReadLock, never()).lock();
    }

    @Test
    public void testGetWithWriteLockDowngrade() throws Exception {
        verify(mockReadLock, never()).lock();
        verify(mockWriteLock, never()).lock();
        final Object retVal = new Object();
        final Object result = lockable.getWithWriteLock((c) -> {
            verify(mockWriteLock).lock();
            verify(mockWriteLock, never()).unlock();
            c.downgrade();
            InOrder inOrder = inOrder(mockReadLock, mockWriteLock);
            inOrder.verify(mockReadLock).lock();
            inOrder.verify(mockWriteLock).unlock();

            return retVal;
        });
        verify(mockReadLock).unlock();
        assertThat(result, is(sameInstance(retVal)));
    }

    @Test
    public void testRunWithWriteLockDowngrade() throws Exception {
        verify(mockReadLock, never()).lock();
        verify(mockWriteLock, never()).lock();
        lockable.runWithWriteLock((c) -> {
            verify(mockWriteLock).lock();
            verify(mockWriteLock, never()).unlock();
            c.downgrade();
            InOrder inOrder = inOrder(mockReadLock, mockWriteLock);
            inOrder.verify(mockReadLock).lock();
            inOrder.verify(mockWriteLock).unlock();
        });
        verify(mockReadLock).unlock();
    }
}
