/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import jdk.internal.vm.annotation.ReservedStackAccess;

/**
 * An implementation of {@link ReadWriteLock} supporting similar
 * semantics to {@link ReentrantLock}.
 * <p>This class has the following properties:
 *
 * <ul>
 * <li><b>Acquisition order</b>
 *
 * <p>This class does not impose a reader or writer preference
 * ordering for lock access.  However, it does support an optional
 * <em>fairness</em> policy.
 *
 * <dl>
 * <dt><b><i>Non-fair mode (default)</i></b>
 * <dd>When constructed as non-fair (the default), the order of entry
 * to the read and write lock is unspecified, subject to reentrancy
 * constraints.  A nonfair lock that is continuously contended may
 * indefinitely postpone one or more reader or writer threads, but
 * will normally have higher throughput than a fair lock.
 *
 * <dt><b><i>Fair mode</i></b>
 * <dd>When constructed as fair, threads contend for entry using an
 * approximately arrival-order policy. When the currently held lock
 * is released, either the longest-waiting single writer thread will
 * be assigned the write lock, or if there is a group of reader threads
 * waiting longer than all waiting writer threads, that group will be
 * assigned the read lock.
 *
 * <p>A thread that tries to acquire a fair read lock (non-reentrantly)
 * will block if either the write lock is held, or there is a waiting
 * writer thread. The thread will not acquire the read lock until
 * after the oldest currently waiting writer thread has acquired and
 * released the write lock. Of course, if a waiting writer abandons
 * its wait, leaving one or more reader threads as the longest waiters
 * in the queue with the write lock free, then those readers will be
 * assigned the read lock.
 *
 * <p>A thread that tries to acquire a fair write lock (non-reentrantly)
 * will block unless both the read lock and write lock are free (which
 * implies there are no waiting threads).  (Note that the non-blocking
 * {@link ReadLock#tryLock()} and {@link WriteLock#tryLock()} methods
 * do not honor this fair setting and will immediately acquire the lock
 * if it is possible, regardless of waiting threads.)
 * </dl>
 *
 * <li><b>Reentrancy</b>
 *
 * <p>This lock allows both readers and writers to reacquire read or
 * write locks in the style of a {@link ReentrantLock}. Non-reentrant
 * readers are not allowed until all write locks held by the writing
 * thread have been released.
 *
 * <p>Additionally, a writer can acquire the read lock, but not
 * vice-versa.  Among other applications, reentrancy can be useful
 * when write locks are held during calls or callbacks to methods that
 * perform reads under read locks.  If a reader tries to acquire the
 * write lock it will never succeed.
 *
 * <li><b>Lock downgrading</b>
 * <p>Reentrancy also allows downgrading from the write lock to a read lock,
 * by acquiring the write lock, then the read lock and then releasing the
 * write lock. However, upgrading from a read lock to the write lock is
 * <b>not</b> possible.
 *
 * <li><b>Interruption of lock acquisition</b>
 * <p>The read lock and write lock both support interruption during lock
 * acquisition.
 *
 * <li><b>{@link Condition} support</b>
 * <p>The write lock provides a {@link Condition} implementation that
 * behaves in the same way, with respect to the write lock, as the
 * {@link Condition} implementation provided by
 * {@link ReentrantLock#newCondition} does for {@link ReentrantLock}.
 * This {@link Condition} can, of course, only be used with the write lock.
 *
 * <p>The read lock does not support a {@link Condition} and
 * {@code readLock().newCondition()} throws
 * {@code UnsupportedOperationException}.
 *
 * <li><b>Instrumentation</b>
 * <p>This class supports methods to determine whether locks
 * are held or contended. These methods are designed for monitoring
 * system state, not for synchronization control.
 * </ul>
 *
 * <p>Serialization of this class behaves in the same way as built-in
 * locks: a deserialized lock is in the unlocked state, regardless of
 * its state when serialized.
 *
 * <p><b>Sample usages</b>. Here is a code sketch showing how to perform
 * lock downgrading after updating a cache (exception handling is
 * particularly tricky when handling multiple locks in a non-nested
 * fashion):
 *
 * <pre> {@code
 * class CachedData {
 *   Object data;
 *   boolean cacheValid;
 *   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *
 *   void processCachedData() {
 *     rwl.readLock().lock();
 *     if (!cacheValid) {
 *       // Must release read lock before acquiring write lock
 *       rwl.readLock().unlock();
 *       rwl.writeLock().lock();
 *       try {
 *         // Recheck state because another thread might have
 *         // acquired write lock and changed state before we did.
 *         if (!cacheValid) {
 *           data = ...
 *           cacheValid = true;
 *         }
 *         // Downgrade by acquiring read lock before releasing write lock
 *         rwl.readLock().lock();
 *       } finally {
 *         rwl.writeLock().unlock(); // Unlock write, still hold read
 *       }
 *     }
 *
 *     try {
 *       use(data);
 *     } finally {
 *       rwl.readLock().unlock();
 *     }
 *   }
 * }}</pre>
 *
 * ReentrantReadWriteLocks can be used to improve concurrency in some
 * uses of some kinds of Collections. This is typically worthwhile
 * only when the collections are expected to be large, accessed by
 * more reader threads than writer threads, and entail operations with
 * overhead that outweighs synchronization overhead. For example, here
 * is a class using a TreeMap that is expected to be large and
 * concurrently accessed.
 *
 * <pre> {@code
 * class RWDictionary {
 *   private final Map<String, Data> m = new TreeMap<>();
 *   private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *   private final Lock r = rwl.readLock();
 *   private final Lock w = rwl.writeLock();
 *
 *   public Data get(String key) {
 *     r.lock();
 *     try { return m.get(key); }
 *     finally { r.unlock(); }
 *   }
 *   public List<String> allKeys() {
 *     r.lock();
 *     try { return new ArrayList<>(m.keySet()); }
 *     finally { r.unlock(); }
 *   }
 *   public Data put(String key, Data value) {
 *     w.lock();
 *     try { return m.put(key, value); }
 *     finally { w.unlock(); }
 *   }
 *   public void clear() {
 *     w.lock();
 *     try { m.clear(); }
 *     finally { w.unlock(); }
 *   }
 * }}</pre>
 *
 * <h3>Implementation Notes</h3>
 *
 * <p>This lock supports a maximum of 65535 recursive write locks
 * and 65535 read locks. Attempts to exceed these limits result in
 * {@link Error} throws from locking methods.
 *
 * @author Doug Lea
 * @since 1.5
 */
// 读/写锁，多个线程可以同时读，但不能同时写
public class ReentrantReadWriteLock implements ReadWriteLock, Serializable {
    private static final long serialVersionUID = -6992448646407690164L;
    
    /** Performs all synchronization mechanics */
    final Sync sync;
    
    /** Inner class providing readlock */
    private final ReentrantReadWriteLock.ReadLock readerLock;
    
    /** Inner class providing writelock */
    private final ReentrantReadWriteLock.WriteLock writerLock;
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new {@code ReentrantReadWriteLock} with
     * default (nonfair) ordering properties.
     */
    public ReentrantReadWriteLock() {
        this(false);
    }
    
    /**
     * Creates a new {@code ReentrantReadWriteLock} with
     * the given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 获取"读"锁实例
    public ReentrantReadWriteLock.ReadLock readLock() {
        return readerLock;
    }
    
    // 获取"写"锁实例
    public ReentrantReadWriteLock.WriteLock writeLock() {
        return writerLock;
    }
    
    
    /**
     * Returns {@code true} if this lock has fairness set true.
     *
     * @return {@code true} if this lock has fairness set true
     */
    // 判断当前锁是否为公平锁
    public final boolean isFair() {
        return sync instanceof FairSync;
    }
    
    /**
     * Queries whether any threads are waiting to acquire the read or
     * write lock. Note that because cancellations may occur at any
     * time, a {@code true} return does not guarantee that any other
     * thread will ever acquire a lock.  This method is designed
     * primarily for use in monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     * acquire the lock
     */
    // 判断【|同步队列|】中是否存在排队的结点（线程）
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }
    
    /**
     * Queries whether the given thread is waiting to acquire either
     * the read or write lock. Note that because cancellations may
     * occur at any time, a {@code true} return does not guarantee
     * that this thread will ever acquire a lock.  This method is
     * designed primarily for use in monitoring of the system state.
     *
     * @param thread the thread
     *
     * @return {@code true} if the given thread is queued waiting for this lock
     *
     * @throws NullPointerException if the thread is null
     */
    // 判断指定的线程是否在【|同步队列|】中排队
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }
    
    /**
     * Returns an estimate of the number of threads waiting to acquire
     * either the read or write lock.  The value is only an estimate
     * because the number of threads may change dynamically while this
     * method traverses internal data structures.  This method is
     * designed for use in monitoring system state, not for
     * synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    // 获取【|同步队列|】中排队的结点数量
    public final int getQueueLength() {
        return sync.getQueueLength();
    }
    
    /**
     * Queries if the write lock is held by the current thread.
     *
     * @return {@code true} if the current thread holds the write lock and
     * {@code false} otherwise
     */
    // 判断当前线程是否为锁的占用者
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }
    
    /**
     * Queries if the write lock is held by any thread. This method is
     * designed for use in monitoring system state, not for
     * synchronization control.
     *
     * @return {@code true} if any thread holds the write lock and
     * {@code false} otherwise
     */
    // 当前的锁是否为"写"锁
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }
    
    /**
     * Queries the number of read locks held for this lock. This
     * method is designed for use in monitoring system state, not for
     * synchronization control.
     *
     * @return the number of read locks held
     */
    // 获取"读"锁的许可证总数
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }
    
    /**
     * Queries the number of reentrant write holds on this lock by the
     * current thread.  A writer thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * @return the number of holds on the write lock by the current thread,
     * or zero if the write lock is not held by the current thread
     */
    // 获取当前线程持有的"写"锁的许可证数量
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }
    
    /**
     * Queries the number of reentrant read holds on this lock by the
     * current thread.  A reader thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * @return the number of holds on the read lock by the current thread,
     * or zero if the read lock is not held by the current thread
     *
     * @since 1.6
     */
    // 获取当前线程持有的"读"锁的许可证数量
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }
    
    /**
     * Queries whether any threads are waiting on the given condition
     * associated with the write lock. Note that because timeouts and
     * interrupts may occur at any time, a {@code true} return does
     * not guarantee that a future {@code signal} will awaken any
     * threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     *
     * @return {@code true} if there are any waiting threads
     *
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException     if the given condition is
     *                                      not associated with this lock
     * @throws NullPointerException         if the condition is null
     */
    // 判断condition的【|条件队列|】中是否存在等待者
    public boolean hasWaiters(Condition condition) {
        if(condition == null) {
            throw new NullPointerException();
        }
        
        if(!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        
        // 判断condition的【|条件队列|】中是否存在等待者
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject) condition);
    }
    
    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with the write lock. Note that because
     * timeouts and interrupts may occur at any time, the estimate
     * serves only as an upper bound on the actual number of waiters.
     * This method is designed for use in monitoring of the system
     * state, not for synchronization control.
     *
     * @param condition the condition
     *
     * @return the estimated number of waiting threads
     *
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException     if the given condition is
     *                                      not associated with this lock
     * @throws NullPointerException         if the condition is null
     */
    // 返回condition中【|条件队列|】长度
    public int getWaitQueueLength(Condition condition) {
        if(condition == null) {
            throw new NullPointerException();
        }
        
        if(!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject) condition);
    }
    
    /**
     * Returns the thread that currently owns the write lock, or
     * {@code null} if not owned. When this method is called by a
     * thread that is not the owner, the return value reflects a
     * best-effort approximation of current lock status. For example,
     * the owner may be momentarily {@code null} even if there are
     * threads trying to acquire the lock but have not yet done so.
     * This method is designed to facilitate construction of
     * subclasses that provide more extensive lock monitoring
     * facilities.
     *
     * @return the owner, or {@code null} if not owned
     */
    // 返回"写"锁的持有者，如果当前线程不持有"写"锁，返回null
    protected Thread getOwner() {
        return sync.getOwner();
    }
    
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire the write lock.  Because the actual set of threads may
     * change dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive lock monitoring facilities.
     *
     * @return the collection of threads
     */
    // 返回一个集合，包含了所有正在【|同步队列|】中排队的"写"锁线程
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }
    
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire the read lock.  Because the actual set of threads may
     * change dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive lock monitoring facilities.
     *
     * @return the collection of threads
     */
    // 返回一个集合，包含了所有正在【|同步队列|】中排队的"读"锁线程
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }
    
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire either the read or write lock.  Because the actual set
     * of threads may change dynamically while constructing this
     * result, the returned collection is only a best-effort estimate.
     * The elements of the returned collection are in no particular
     * order.  This method is designed to facilitate construction of
     * subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    // 返回一个集合，包含了所有正在【|同步队列|】中排队的所有线程
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }
    
    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with the write lock.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a
     * best-effort estimate. The elements of the returned collection
     * are in no particular order.  This method is designed to
     * facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     *
     * @param condition the condition
     *
     * @return the collection of threads
     *
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException     if the given condition is
     *                                      not associated with this lock
     * @throws NullPointerException         if the condition is null
     */
    // 返回一个集合，该集合包含了condition的【|条件队列|】中所有的线程
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if(condition == null) {
            throw new NullPointerException();
        }
        
        if(!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject) condition);
    }
    
    
    
    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes the String {@code "Write locks ="}
     * followed by the number of reentrantly held write locks, and the
     * String {@code "Read locks ="} followed by the number of held
     * read locks.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);
        
        return super.toString() + "[Write locks = " + w + ", Read locks = " + r + "]";
    }
    
    
    
    /**
     * The lock returned by method {@link ReentrantReadWriteLock#readLock}.
     */
    // "读"锁，共享锁
    public static class ReadLock implements Lock, Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        
        private final Sync sync;
        
        /**
         * Constructor for use by subclasses.
         *
         * @param lock the outer lock object
         *
         * @throws NullPointerException if the lock is null
         */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }
        
        /**
         * Throws {@code UnsupportedOperationException} because
         * {@code ReadLocks} do not support conditions.
         *
         * @throws UnsupportedOperationException always
         */
        // 获取条件对象实例
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
        
        /**
         * Acquires the read lock.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately.
         *
         * <p>If the write lock is held by another thread then
         * the current thread becomes disabled for thread scheduling
         * purposes and lies dormant until the read lock has been acquired.
         */
        // 申请"读"锁，允许阻塞带有中断标记的线程（不一定成功）
        public void lock() {
            sync.acquireShared(1);
        }
        
        /**
         * Acquires the read lock unless the current thread is
         * {@linkplain Thread#interrupt interrupted}.
         *
         * <p>Acquires the read lock if the write lock is not held
         * by another thread and returns immediately.
         *
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling
         * purposes and lies dormant until one of two things happens:
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         *
         * </ul>
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        // 申请"读"锁，不允许阻塞带有中断标记的线程（不一定成功）
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }
        
        /**
         * Acquires the read lock only if the write lock is not held by
         * another thread at the time of invocation.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value
         * {@code true}. Even when this lock has been set to use a
         * fair ordering policy, a call to {@code tryLock()}
         * <em>will</em> immediately acquire the read lock if it is
         * available, whether or not other threads are currently
         * waiting for the read lock.  This &quot;barging&quot; behavior
         * can be useful in certain circumstances, even though it
         * breaks fairness. If you want to honor the fairness setting
         * for this lock, then use {@link #tryLock(long, TimeUnit)
         * tryLock(0, TimeUnit.SECONDS)} which is almost equivalent
         * (it also detects interruption).
         *
         * <p>If the write lock is held by another thread then
         * this method will return immediately with the value
         * {@code false}.
         *
         * @return {@code true} if the read lock was acquired
         */
        // 申请"读"锁，只申请一次，失败后不再尝试
        public boolean tryLock() {
            return sync.tryReadLock();
        }
        
        /**
         * Acquires the read lock if the write lock is not held by
         * another thread within the given waiting time and the
         * current thread has not been {@linkplain Thread#interrupt
         * interrupted}.
         *
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value
         * {@code true}. If this lock has been set to use a fair
         * ordering policy then an available lock <em>will not</em> be
         * acquired if any other threads are waiting for the
         * lock. This is in contrast to the {@link #tryLock()}
         * method. If you want a timed {@code tryLock} that does
         * permit barging on a fair lock then combine the timed and
         * un-timed forms together:
         *
         * <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling
         * purposes and lies dormant until one of three things happens:
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses.
         *
         * </ul>
         *
         * <p>If the read lock is acquired then the value {@code true} is
         * returned.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul> then {@link InterruptedException} is thrown and the
         * current thread's interrupted status is cleared.
         *
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * @param timeout the time to wait for the read lock
         * @param unit    the time unit of the timeout argument
         *
         * @return {@code true} if the read lock was acquired
         *
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        // 申请"读"锁，不允许阻塞带有中断标记的线程（一次失败后，带着超时标记继续申请）
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }
        
        /**
         * Attempts to release this lock.
         *
         * <p>If the number of readers is now zero then the lock
         * is made available for write lock attempts. If the current
         * thread does not hold this lock then {@link
         * IllegalMonitorStateException} is thrown.
         *
         * @throws IllegalMonitorStateException if the current thread
         *                                      does not hold this lock
         */
        // 释放"读"锁
        public void unlock() {
            sync.releaseShared(1);
        }
        
        /**
         * Returns a string identifying this lock, as well as its lock state.
         * The state, in brackets, includes the String {@code "Read locks ="}
         * followed by the number of held read locks.
         *
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() + "[Read locks = " + r + "]";
        }
    }
    
    /**
     * The lock returned by method {@link ReentrantReadWriteLock#writeLock}.
     */
    // "写"锁。独占锁
    public static class WriteLock implements Lock, Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        
        private final Sync sync;
        
        /**
         * Constructor for use by subclasses.
         *
         * @param lock the outer lock object
         *
         * @throws NullPointerException if the lock is null
         */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }
        
        /**
         * Returns a {@link Condition} instance for use with this
         * {@link Lock} instance.
         * <p>The returned {@link Condition} instance supports the same
         * usages as do the {@link Object} monitor methods ({@link
         * Object#wait() wait}, {@link Object#notify notify}, and {@link
         * Object#notifyAll notifyAll}) when used with the built-in
         * monitor lock.
         *
         * <ul>
         *
         * <li>If this write lock is not held when any {@link
         * Condition} method is called then an {@link
         * IllegalMonitorStateException} is thrown.  (Read locks are
         * held independently of write locks, so are not checked or
         * affected. However it is essentially always an error to
         * invoke a condition waiting method when the current thread
         * has also acquired read locks, since other threads that
         * could unblock it will not be able to acquire the write
         * lock.)
         *
         * <li>When the condition {@linkplain Condition#await() waiting}
         * methods are called the write lock is released and, before
         * they return, the write lock is reacquired and the lock hold
         * count restored to what it was when the method was called.
         *
         * <li>If a thread is {@linkplain Thread#interrupt interrupted} while
         * waiting then the wait will terminate, an {@link
         * InterruptedException} will be thrown, and the thread's
         * interrupted status will be cleared.
         *
         * <li>Waiting threads are signalled in FIFO order.
         *
         * <li>The ordering of lock reacquisition for threads returning
         * from waiting methods is the same as for threads initially
         * acquiring the lock, which is in the default case not specified,
         * but for <em>fair</em> locks favors those threads that have been
         * waiting the longest.
         *
         * </ul>
         *
         * @return the Condition object
         */
        // 获取条件对象实例
        public Condition newCondition() {
            return sync.newCondition();
        }
        
        /**
         * Acquires the write lock.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately, setting the write lock hold count to
         * one.
         *
         * <p>If the current thread already holds the write lock then the
         * hold count is incremented by one and the method returns
         * immediately.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until the write lock has been acquired, at which
         * time the write lock hold count is set to one.
         */
        // 申请"写"锁，允许阻塞带有中断标记的线程（不一定成功）
        public void lock() {
            sync.acquire(1);
        }
        
        /**
         * Acquires the write lock unless the current thread is
         * {@linkplain Thread#interrupt interrupted}.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately, setting the write lock hold count to
         * one.
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * immediately.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of two things happens:
         *
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         *
         * </ul>
         *
         * <p>If the write lock is acquired by the current thread then the
         * lock hold count is set to one.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        // 申请"写"锁，不允许阻塞带有中断标记的线程（不一定成功）
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }
        
        /**
         * Acquires the write lock only if it is not held by another thread
         * at the time of invocation.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately with the value {@code true},
         * setting the write lock hold count to one. Even when this lock has
         * been set to use a fair ordering policy, a call to
         * {@code tryLock()} <em>will</em> immediately acquire the
         * lock if it is available, whether or not other threads are
         * currently waiting for the write lock.  This &quot;barging&quot;
         * behavior can be useful in certain circumstances, even
         * though it breaks fairness. If you want to honor the
         * fairness setting for this lock, then use {@link
         * #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS)}
         * which is almost equivalent (it also detects interruption).
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * {@code true}.
         *
         * <p>If the lock is held by another thread then this method
         * will return immediately with the value {@code false}.
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held
         * by the current thread; and {@code false} otherwise.
         */
        // 申请"写"锁，只申请一次，失败后不再尝试
        public boolean tryLock() {
            return sync.tryWriteLock();
        }
        
        /**
         * Acquires the write lock if it is not held by another thread
         * within the given waiting time and the current thread has
         * not been {@linkplain Thread#interrupt interrupted}.
         *
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately with the value {@code true},
         * setting the write lock hold count to one. If this lock has been
         * set to use a fair ordering policy then an available lock
         * <em>will not</em> be acquired if any other threads are
         * waiting for the write lock. This is in contrast to the {@link
         * #tryLock()} method. If you want a timed {@code tryLock}
         * that does permit barging on a fair lock then combine the
         * timed and un-timed forms together:
         *
         * <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * {@code true}.
         *
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of three things happens:
         *
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses
         *
         * </ul>
         *
         * <p>If the write lock is acquired then the value {@code true} is
         * returned and the write lock hold count is set to one.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * @param timeout the time to wait for the write lock
         * @param unit    the time unit of the timeout argument
         *
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held by the
         * current thread; and {@code false} if the waiting time
         * elapsed before the lock could be acquired.
         *
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        // 申请"写"锁，不允许阻塞带有中断标记的线程（一次失败后，带着超时标记继续申请）
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }
        
        /**
         * Attempts to release this lock.
         *
         * <p>If the current thread is the holder of this lock then
         * the hold count is decremented. If the hold count is now
         * zero then the lock is released.  If the current thread is
         * not the holder of this lock then {@link
         * IllegalMonitorStateException} is thrown.
         *
         * @throws IllegalMonitorStateException if the current thread does not
         *                                      hold this lock
         */
        // 释放"写"锁
        public void unlock() {
            sync.release(1);
        }
        
        /**
         * Queries if this write lock is held by the current thread.
         * Identical in effect to {@link
         * ReentrantReadWriteLock#isWriteLockedByCurrentThread}.
         *
         * @return {@code true} if the current thread holds this lock and
         * {@code false} otherwise
         *
         * @since 1.6
         */
        // 判断当前线程是否为锁的占用者
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }
        
        /**
         * Queries the number of holds on this write lock by the current
         * thread.  A thread has a hold on a lock for each lock action
         * that is not matched by an unlock action.  Identical in effect
         * to {@link ReentrantReadWriteLock#getWriteHoldCount}.
         *
         * @return the number of holds on this lock by the current thread,
         * or zero if this lock is not held by the current thread
         *
         * @since 1.6
         */
        // 获取当前线程持有的"写"锁的许可证数量
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
        
        /**
         * Returns a string identifying this lock, as well as its lock
         * state.  The state, in brackets includes either the String
         * {@code "Unlocked"} or the String {@code "Locked by"}
         * followed by the {@linkplain Thread#getName name} of the owning thread.
         *
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ? "[Unlocked]" : "[Locked by thread " + o.getName() + "]");
        }
    }
    
    /**
     * Synchronization implementation for ReentrantReadWriteLock.
     * Subclassed into fair and nonfair versions.
     */
    /*
     * 同步队列的实现者，实现了读/写锁的语义
     *
     * 读/写锁的许可证数量存储在state中
     * state的高16位存储"读"锁（共享锁）的许可证（所有线程）
     * state的低16位存储"写"锁（独占锁）的许可证（所有线程）
     *
     * 总结：
     *
     * >>>> 申请"写"锁
     * 1.存在"读"锁的情形下，不允许申请"写"锁
     * 2.某一个时刻，最多有一个线程持有"写"锁
     * 3.在不违背规则1、2的情形下：
     * 3.1.如果申请"写"锁的线程不被阻塞，则其总能申请到"写"锁
     * 3.2.如果申请"写"锁的线程需要被阻塞，则当前线程无法申请到"写"锁
     * ▋注：关于申请"写"锁的线程被阻塞的情形：
     * ▋▋争夺公平锁时，如果【|同步队列|】中存在其他线程在队首排队，则当前线程被阻塞
     *
     * >>>> 申请"读"锁
     * 1.在其他线程持有"写"锁时，不允许当前线程申请"读"锁
     * 2.在当前线程持有"写"锁时，依然可以申请到"读"锁（锁降级）
     * 3.在不违背1/2的情形下：
     * 3.1.如果申请"读"锁的线程不被阻塞，则其总能申请到"读"锁
     * 3.2.如果申请"读"锁的线程需要被阻塞，则只有发生"重入"现象的线程可以申请到"读"锁
     * ▋注：关于申请"读"锁的线程被阻塞的情形：
     * ▋▋争夺公平锁时，如果【|同步队列|】中存在其他线程在队首排队，则当前线程被阻塞
     * ▋▋争夺非公平锁时，如果【|同步队列|】的首位是持有"写"锁的线程，则当前线程被阻塞
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;
        
        static final int SHARED_SHIFT = 16;
        
        /*
         * Read vs write count extraction constants and functions.
         * Lock state is logically divided into two unsigned shorts:
         * The lower one representing the exclusive (writer) lock hold count,
         * and the upper the shared (reader) hold count.
         */
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);      // 0x10000  // 共享锁掩码，高16位
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;  // 0x0FFFF  // 独占锁掩码，低16位
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;  // 0x0FFFF  // 两种锁的许可证上限均为65535
        
        /**
         * firstReader is the first thread to have acquired the read lock.
         * firstReaderHoldCount is firstReader's hold count.
         *
         * <p>More precisely, firstReader is the unique thread that last
         * changed the shared count from 0 to 1, and has not released the
         * read lock since then; null if there is no such thread.
         *
         * <p>Cannot cause garbage retention unless the thread terminated
         * without relinquishing its read locks, since tryReleaseShared
         * sets it to null.
         *
         * <p>Accessed via a benign data race; relies on the memory
         * model's out-of-thin-air guarantees for references.
         *
         * <p>This allows tracking of read holds for uncontended read
         * locks to be very cheap.
         */
        // 当前所有持有"读"锁的线程中，首个申请"读"锁的线程
        private transient Thread firstReader;
        // 首个申请"读"锁的线程对"读"锁的重入次数（许可证数量）
        private transient int firstReaderHoldCount;
        
        /**
         * The hold count of the last thread to successfully acquire
         * readLock. This saves ThreadLocal lookup in the common case
         * where the next thread to release is the last one to
         * acquire. This is non-volatile since it is just used
         * as a heuristic, and would be great for threads to cache.
         *
         * <p>Can outlive the Thread for which it is caching the read
         * hold count, but avoids garbage retention by not retaining a
         * reference to the Thread.
         *
         * <p>Accessed via a benign data race; relies on the memory
         * model's final field and out-of-thin-air guarantees.
         */
        /*
         * 缓存申请"读"锁的线程的信息
         *
         * 该线程时当前所有持有"读"锁的线程中，非首个申请"读"锁的线程
         *
         * 每次出现非首个申请"读"锁的线程申请"读"锁时，都要先判断该缓存是否仍然有效，
         * 有效的依据是缓存不为空，且缓存中的线程与当前申请"读"锁的线程是一个线程
         *
         * 如果缓存已经失效，需要重新创建一个新的HoldCounter作为缓存，
         * 如果缓存未失效，还要判断该缓存是否关联到了当前线程中（有可能之前关联进去了，但后来被剔除了），
         * 如果该缓存与当前申请"读"锁的线程已无关联，则需要重建进行关联
         *
         * 当缓存与当前线程关联好之后，累计许可证数量
         *
         * 简单地说，除首个申请"读"锁的线程之外，
         * 其他每个线程内部都缓存了一个HoldCounter，用来记录各自对"读"锁的申请（重入）次数
         * 当重入次数为0时，需要将缓存的HoldCounter从线程中移除
         */
        private transient HoldCounter cachedHoldCounter;
        
        /**
         * The number of reentrant read locks held by current thread.
         * Initialized only in constructor and readObject.
         * Removed whenever a thread's read hold count drops to 0.
         */
        // 提供线程局部变量的缓存机制，并且在适当的时候负责创建新的HoldCounter
        private transient ThreadLocalHoldCounter readHolds;
        
        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // ensures visibility of readHolds
        }
        
        /*
         * Note that tryRelease and tryAcquire can be called by Conditions.
         * So it is possible that their arguments contain both read and write holds
         * that are all released during a condition wait and re-established in tryAcquire.
         */
        
        
        /*▼ 读锁 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Returns true if the current thread, when trying to acquire
         * the read lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        /*
         * 是否需要阻塞当前申请"读"锁的线程
         * 在非公平锁语境下，如果【|同步队列|】的首位是持有"写"锁的线程，则阻塞当前申请"读"锁的线程
         * 在  公平锁语境下，如果【|同步队列|】的首位是其他排队线程，则阻塞当前申请"读"锁的线程
         */
        abstract boolean readerShouldBlock();
        
        // 申请"读"锁，形参unused没有用，因为申请"读"锁成功一次，就固定生产一张许可证
        @ReservedStackAccess
        protected final int tryAcquireShared(int unused) {
            /*
             * Walkthrough:
             * 1. If write lock held by another thread, fail.
             * 2. Otherwise, this thread is eligible for
             *    lock wrt state, so ask if it should block
             *    because of queue policy. If not, try
             *    to grant by CASing state and updating count.
             *    Note that step does not check for reentrant
             *    acquires, which is postponed to full version
             *    to avoid having to check hold count in
             *    the more typical non-reentrant case.
             * 3. If step 2 fails either because thread
             *    apparently not eligible or CAS fails or count
             *    saturated, chain to version with full retry loop.
             */
            
            // 获取当前申请"读"锁的线程
            Thread current = Thread.currentThread();
            
            // 获取许可证数量
            int c = getState();
            
            // 获取独占锁（"写"锁）的许可证数量
            if(exclusiveCount(c) != 0) {
                // 如果存在"写"锁，进一步判断当前线程是否为"写"锁的持有者
                if(getExclusiveOwnerThread() != current){
                    // 如果是别的线程持有"写"锁，当前线程又去申请"读"锁，则申请失败
                    return -1;
                }
            }
            
            // 获取共享锁（"读"锁）的许可证数量
            int r = sharedCount(c);
            
            // 判断是否需要阻塞当前申请"读"锁的线程
            if(!readerShouldBlock()) {
                // 判断许可证的数量是否超标
                if(r<MAX_COUNT){
                    /*
                     * "写"锁许可证数量不变，"读"锁许可证数量增一
                     * 在这儿如果有别的线程抢先更改了锁的状态，
                     * 那么此处的设置将失败
                     */
                    if(compareAndSetState(c, c + SHARED_UNIT)){
                        // 如果当前的"读"锁空闲，说明出现了首个申请"读"锁的线程
                        if(r == 0) {
                            // 记下首个申请"读"锁的线程
                            firstReader = current;
                            // 记下首个申请"读"锁的线程对"读"锁的重入次数
                            firstReaderHoldCount = 1;
                        } else {
                            /*
                             * 至此，说明出现了"读"锁重入的现象
                             * 这个重入可能是由首个申请"读"锁的线程引起的
                             * 也可能是由非首个申请"读"锁的线程引起的
                             */
                            
                            // 如果首个申请"读"锁的线程再次申请"读"锁
                            if(firstReader == current){
                                // 只是累加首个申请"读"锁的线程的许可证数量
                                firstReaderHoldCount++;
                            } else {
                                /* 如果是非首个申请"读"锁的线程在申请"读"锁 */
                                
                                // 尝试中缓存中获取HoldCounter
                                HoldCounter rh = cachedHoldCounter;
                                
                                /*
                                 * 如果rh == null，说明第一次出现非首个申请"读"锁的线程在申请"读"锁
                                 * 此时，需要创建一个全新的HoldCounter，并缓存到cachedHoldCounter
                                 */
                                if(rh == null){
                                    cachedHoldCounter = rh = readHolds.get();
                                } else {
                                    /* 至此，说明这不是第一次出现非首个申请"读"锁的线程在申请"读"锁 */
                                    
                                    /*
                                     * 如果rh.tid != LockSupport.getThreadId(current)
                                     * 说明缓存中的HoldCounter不是由当前申请"读"锁的线程创建的，换句话说，这个缓存是失效的
                                     * 那么，仍然需要重新创建一个全新的HoldCounter，并缓存到cachedHoldCounter
                                     */
                                    if(rh.tid != LockSupport.getThreadId(current)){
                                        cachedHoldCounter = rh = readHolds.get();
                                    } else {
                                        /*
                                         * 至此，说明缓存中存在HoldCounter，
                                         * 且这个HoldCounter就是由当前申请"读"锁的线程在早些时候创建的
                                         */
                                        
                                        // 缓存仍然有效，不过已经从当前线程剔除了，此时需要重新将其关联到当前线程
                                        if(rh.count == 0) {
                                            readHolds.set(rh);
                                        }
                                    }
                                }
                                
                                // 锁的重入次数递增
                                rh.count++;
                            }
                        }
                        
                        return 1;
                    } // if(compareAndSetState(c, c + SHARED_UNIT))
                } // if(r<MAX_COUNT)
            } // if(!readerShouldBlock())
            
            return fullTryAcquireShared(current);
        }
        
        /**
         * Full version of acquire for reads, that handles CAS misses
         * and reentrant reads not dealt with in tryAcquireShared.
         */
        // 当申请"读"锁的线程需要被阻塞时，需要到这里检查是否存在"重入"
        final int fullTryAcquireShared(Thread current) {
            /*
             * This code is in part redundant with that in
             * tryAcquireShared but is simpler overall by not
             * complicating tryAcquireShared with interactions between
             * retries and lazily reading hold counts.
             */
            HoldCounter rh = null;
            
            for(; ; ) {
                // 获取许可证数量
                int c = getState();
                
                // 获取独占锁（"写"锁）的许可证数量
                if(exclusiveCount(c) != 0) {
                    if(getExclusiveOwnerThread() != current) {
                        // 如果已经有其他线程获取到了"写"锁，则本次申请"读"锁失败
                        return -1;
                    }
                } else {
                    /* 至此，说明当前没有持有"写"锁的线程 */
                    
                    // 判断是否需要阻塞当前申请"读"锁的线程
                    if(readerShouldBlock()){
                        /* Make sure we're not acquiring read lock reentrantly */
                        if(firstReader == current) {
                            /* assert firstReaderHoldCount > 0; */
                            
                            /* 至此，说明是首个申请"读"锁的线程再次申请"读"锁（重入） */
                        } else {
                            if(rh == null) {
                                rh = cachedHoldCounter;
                                
                                if(rh==null || rh.tid != LockSupport.getThreadId(current)){
                                    // 这里可能会获取到一个全新的HoldCounter，也可能是一个已经存在的HoldCounter
                                    rh = readHolds.get();
                                    
                                    // 全新的HoldCounter中count必然为0，此时需要将其从当前的线程中移除（切断与当前线程的关联）
                                    if(rh.count == 0) {
                                        readHolds.remove();
                                    }
                                }
                            }
                            
                            // 如果rh.count==0，则说明不是重入行为，需要被阻塞
                            if(rh.count == 0) {
                                return -1;
                            }
                            
                            /* 至此，说明是非首个申请"读"锁的线程再次申请"读"锁 */
                        }
                        
                        /* 至此，说明当前线程发生了重入行为，否则在上面就返回了 */
                    } // if(readerShouldBlock())
                }
                
                // "读"锁的许可证数量已经超标
                if(sharedCount(c) == MAX_COUNT) {
                    throw new Error("Maximum lock count exceeded");
                }
                
                // 原子地将"读"锁数量增一
                if(compareAndSetState(c, c + SHARED_UNIT)) {
                    // 获取共享锁（"读"锁）的许可证数量
                    if(sharedCount(c) == 0) {
                        // 至此说明当前不存在持有"读"锁的线程
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else {
                        if(firstReader == current) {
                            // 如果是首个申请"读"锁的线程再次申请"读"锁，将其重入次数增一
                            firstReaderHoldCount++;
                        } else {
                            if(rh == null) {
                                rh = cachedHoldCounter;
                            }
                            
                            if(rh == null){
                                rh = readHolds.get();
                            } else {
                                if(rh.tid != LockSupport.getThreadId(current)){
                                    rh = readHolds.get();
                                } else {
                                    if(rh.count == 0){
                                        readHolds.set(rh);
                                    }
                                }
                            }
                            
                            rh.count++;
                            cachedHoldCounter = rh; // cache for release
                        }
                    }
                    return 1;
                } // if(compareAndSetState(c, c + SHARED_UNIT))
            } // for(; ; )
        }
        
        /**
         * Performs tryLock for read, enabling barging in both modes.
         * This is identical in effect to tryAcquireShared except for
         * lack of calls to readerShouldBlock.
         */
        // 申请"读"锁，不考虑阻塞的情形
        @ReservedStackAccess
        final boolean tryReadLock() {
            // 获取当前申请"读"锁的线程
            Thread current = Thread.currentThread();
            
            for(; ; ) {
                // 获取许可证数量
                int c = getState();
                
                // 如果存在其他线程持有"写"锁，则申请"读"锁失败
                if(exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current) {
                    return false;
                }
                
                // 获取共享锁（"读"锁）的许可证数量
                int r = sharedCount(c);
                if(r == MAX_COUNT) {
                    throw new Error("Maximum lock count exceeded");
                }
                
                // "读"锁许可证数量增一
                if(compareAndSetState(c, c + SHARED_UNIT)) {
                    // 当前"读"锁空闲
                    if(r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else {
                        if(firstReader == current) {
                            firstReaderHoldCount++;
                        } else {
                            HoldCounter rh = cachedHoldCounter;
                            if(rh == null || rh.tid != LockSupport.getThreadId(current)) {
                                cachedHoldCounter = rh = readHolds.get();
                            } else if(rh.count == 0) {
                                readHolds.set(rh);
                            }
                            rh.count++;
                        }
                    }
                    return true;
                }
            }
        }
        
        // 释放"读"锁，返回值代表当前线程是否还持有"读"锁
        @ReservedStackAccess
        protected final boolean tryReleaseShared(int unused) {
            
            Thread current = Thread.currentThread();
            
            if(firstReader == current) {
                // assert firstReaderHoldCount > 0;
                if(firstReaderHoldCount == 1) {
                    firstReader = null;
                } else {
                    firstReaderHoldCount--;
                }
            } else {
                HoldCounter rh = cachedHoldCounter;
                
                if(rh == null || rh.tid != LockSupport.getThreadId(current)) {
                    rh = readHolds.get();
                }
                
                int count = rh.count;
                if(count<=1) {
                    // 当前线程已经不再持有"读"锁了
                    readHolds.remove();
                    if(count<=0) {
                        throw unmatchedUnlockException();
                    }
                }
                
                --rh.count;
            }
            
            // 减少许可证数量（每次消费一张许可证）
            for(; ; ) {
                int c = getState();
                int nextc = c - SHARED_UNIT;
                if(compareAndSetState(c, nextc)) {
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    
                    return nextc == 0;
                }
            }
        }
        
        /*▲ 读锁 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 写锁 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Returns true if the current thread, when trying to acquire the write lock,
         * and otherwise eligible to do so, should block because of policy for overtaking other waiting threads.
         */
        /*
         * 是否需要阻塞当前申请"写"锁的线程
         * 在非公平锁语境下，永远不阻塞
         * 在  公平锁语境下，如果【|同步队列|】的首位是其他排队线程，则阻塞当前申请"写"锁的线程
         */
        abstract boolean writerShouldBlock();
        
        // 申请"写"锁，返回值代表是否申请成功
        @ReservedStackAccess
        protected final boolean tryAcquire(int acquires) {
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             */
            
            // 获取当前申请"写"锁的线程
            Thread current = Thread.currentThread();
            
            // 获取许可证数量
            int c = getState();
            
            // 获取独占锁（"写"锁）的许可证数量
            int w = exclusiveCount(c);
            
            // 如果许可证数量不为0，说明存在"读"锁或"写"锁
            if(c != 0) {
                // (Note: if c != 0 and w == 0 then shared count != 0)
                if(w == 0 || current != getExclusiveOwnerThread()) {
                    /*
                     * 如果c!=0且w==0，说明已经存在持有"读"锁的线程（无论是不是自身），则本次申请失败（有读锁的情形下不能申请写锁）
                     * 如果c!=0且w!=0，但current != getExclusiveOwnerThread()，
                     * 说明存在"写"锁，但该"写"锁不是由当前线程持有的，那么本次申请同样失败（写锁是独占的）
                     */
                    return false;
                }
                
                // "写"锁数量超标
                if(w + exclusiveCount(acquires)>MAX_COUNT) {
                    throw new Error("Maximum lock count exceeded");
                }
                
                // 更新"写"锁数量（"写"锁可重入）
                setState(c + acquires);
                
                return true;
            }
            
            /* 至此，说明锁处于空闲状态 */
            
            // "写"锁应当被阻塞
            if(writerShouldBlock()) {
                return false;
            }
            
            // 原子地更新许可证数量
            if(!compareAndSetState(c, c + acquires)){
                return false;
            }
            
            // 更新独占锁的占用者为当前线程
            setExclusiveOwnerThread(current);
            
            return true;
        }
        
        /**
         * Performs tryLock for write, enabling barging in both modes.
         * This is identical in effect to tryAcquire except for lack
         * of calls to writerShouldBlock.
         */
        // 申请"写"锁，不考虑阻塞的情形
        @ReservedStackAccess
        final boolean tryWriteLock() {
            // 获取当前申请"写"锁的线程
            Thread current = Thread.currentThread();
            
            // 获取许可证数量
            int c = getState();
            
            if(c != 0) {
                // 获取独占锁（"写"锁）的许可证数量
                int w = exclusiveCount(c);
                
                // 如果存在"读"锁，或者其它线程已有"写'锁，则不允许申请"写"锁
                if(w == 0 || current != getExclusiveOwnerThread()) {
                    return false;
                }
                
                if(w == MAX_COUNT) {
                    throw new Error("Maximum lock count exceeded");
                }
            }
            
            // 更新写锁
            if(!compareAndSetState(c, c + 1)) {
                return false;
            }
            
            setExclusiveOwnerThread(current);
            
            return true;
        }
        
        // 释放"写"锁，返回值代表"写"锁是否自由
        @ReservedStackAccess
        protected final boolean tryRelease(int releases) {
            if(!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            
            int nextc = getState() - releases;
            
            boolean free = exclusiveCount(nextc) == 0;
            
            if(free) {
                setExclusiveOwnerThread(null);
            }
            
            setState(nextc);
            
            return free;
        }
        
        /*▲ 写锁 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        // 返回{条件对象}实例
        final ConditionObject newCondition() {
            return new ConditionObject();
        }
        
        // 判断当前线程是否为锁的占用者
        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
        
        // 当前的锁是否为"写"锁
        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }
        
        // 返回"写"锁的持有者，如果当前线程不持有"写"锁，返回null
        final Thread getOwner() {
            // Must read state before owner to ensure memory consistency
            return ((exclusiveCount(getState()) == 0) ? null : getExclusiveOwnerThread());
        }
        
        /** Returns the number of shared holds represented in count. */
        // 计算共享锁（"读"锁）的许可证数量
        static int sharedCount(int c) {
            // 将state右移16位，移除所有低位信息
            return c >>> SHARED_SHIFT;
        }
        
        /** Returns the number of exclusive holds represented in count. */
        // 计算独占锁（"写"锁）的许可证数量
        static int exclusiveCount(int c) {
            // 对state应用独占锁掩码，抹掉所有高位信息
            return c & EXCLUSIVE_MASK;
        }
        
        // 获取许可证总数
        final int getCount() {
            return getState();
        }
        
        // 获取"读"锁的许可证总数
        final int getReadLockCount() {
            return sharedCount(getState());
        }
        
        // 获取当前线程持有的"写"锁的许可证数量
        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }
        
        // 获取当前线程持有的"读"锁的许可证数量
        final int getReadHoldCount() {
            if(getReadLockCount() == 0) {
                return 0;
            }
            
            Thread current = Thread.currentThread();
            if(firstReader == current) {
                return firstReaderHoldCount;
            }
            
            HoldCounter rh = cachedHoldCounter;
            if(rh != null && rh.tid == LockSupport.getThreadId(current)) {
                return rh.count;
            }
            
            int count = readHolds.get().count;
            if(count == 0) {
                readHolds.remove();
            }
            return count;
        }
        
        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            readHolds = new ThreadLocalHoldCounter();
            setState(0); // reset to unlocked state
        }
        
        private static IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException("attempt to unlock read lock, not locked by current thread");
        }
        
        /** A counter for per-thread read hold counts. Maintained as a ThreadLocal; cached in cachedHoldCounter. */
        // 线程局部变量，主要用来记录当前线程申请"读"锁的次数
        static final class HoldCounter {
            // 使用线程ID而不是线程引用，以方便垃圾清理
            final long tid = LockSupport.getThreadId(Thread.currentThread());
            int count;          // 当前线程申请"读"锁的次数，初始值为0
        }
        
        /** ThreadLocal subclass. Easiest to explicitly define for sake of deserialization mechanics. */
        // 提供线程局部变量的缓存机制
        static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }
    }
    
    /**
     * Nonfair version of Sync
     */
    // 非公平锁
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;
        
        // 如果【|同步队列|】的首位是持有"写"锁的线程，则阻塞当前申请"读"锁的线程
        final boolean readerShouldBlock() {
            /*
             * As a heuristic to avoid indefinite writer starvation,
             * block if the thread that momentarily appears to be head
             * of queue, if one exists, is a waiting writer.  This is
             * only a probabilistic effect since a new reader will not
             * block if there is a waiting writer behind other enabled
             * readers that have not yet drained from the queue.
             */
            // 判断【|同步队列|】的首位是否为[独占模式Node]
            return apparentlyFirstQueuedIsExclusive();
        }
        
        // 不阻塞"写"锁
        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }
    }
    
    /**
     * Fair version of Sync
     */
    // 公平锁
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;
        
        // 如果【|同步队列|】的首位是其他排队线程，则阻塞当前申请"读"锁的线程
        final boolean readerShouldBlock() {
            // 判断【|同步队列|】的队头是否还有其他（非当前线程）的线程在排队
            return hasQueuedPredecessors();
        }
        
        // 如果【|同步队列|】的首位是其他排队线程，则阻塞当前申请"写"锁的线程
        final boolean writerShouldBlock() {
            // 判断【|同步队列|】的队头是否还有其他（非当前线程）的线程在排队
            return hasQueuedPredecessors();
        }
        
    }
    
}
