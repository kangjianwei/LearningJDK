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

package java.util.concurrent;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A counting semaphore.  Conceptually, a semaphore maintains a set of
 * permits.  Each {@link #acquire} blocks if necessary until a permit is
 * available, and then takes it.  Each {@link #release} adds a permit,
 * potentially releasing a blocking acquirer.
 * However, no actual permit objects are used; the {@code Semaphore} just
 * keeps a count of the number available and acts accordingly.
 *
 * <p>Semaphores are often used to restrict the number of threads than can
 * access some (physical or logical) resource. For example, here is
 * a class that uses a semaphore to control access to a pool of items:
 * <pre> {@code
 * class Pool {
 *   private static final int MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *   public Object getItem() throws InterruptedException {
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) {
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *         used[i] = true;
 *         return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *         if (used[i]) {
 *           used[i] = false;
 *           return true;
 *         } else
 *           return false;
 *       }
 *     }
 *     return false;
 *   }
 * }}</pre>
 *
 * <p>Before obtaining an item each thread must acquire a permit from
 * the semaphore, guaranteeing that an item is available for use. When
 * the thread has finished with the item it is returned back to the
 * pool and a permit is returned to the semaphore, allowing another
 * thread to acquire that item.  Note that no synchronization lock is
 * held when {@link #acquire} is called as that would prevent an item
 * from being returned to the pool.  The semaphore encapsulates the
 * synchronization needed to restrict access to the pool, separately
 * from any synchronization needed to maintain the consistency of the
 * pool itself.
 *
 * <p>A semaphore initialized to one, and which is used such that it
 * only has at most one permit available, can serve as a mutual
 * exclusion lock.  This is more commonly known as a <em>binary
 * semaphore</em>, because it only has two states: one permit
 * available, or zero permits available.  When used in this way, the
 * binary semaphore has the property (unlike many {@link java.util.concurrent.locks.Lock}
 * implementations), that the &quot;lock&quot; can be released by a
 * thread other than the owner (as semaphores have no notion of
 * ownership).  This can be useful in some specialized contexts, such
 * as deadlock recovery.
 *
 * <p>The constructor for this class optionally accepts a
 * <em>fairness</em> parameter. When set false, this class makes no
 * guarantees about the order in which threads acquire permits. In
 * particular, <em>barging</em> is permitted, that is, a thread
 * invoking {@link #acquire} can be allocated a permit ahead of a
 * thread that has been waiting - logically the new thread places itself at
 * the head of the queue of waiting threads. When fairness is set true, the
 * semaphore guarantees that threads invoking any of the {@link
 * #acquire() acquire} methods are selected to obtain permits in the order in
 * which their invocation of those methods was processed
 * (first-in-first-out; FIFO). Note that FIFO ordering necessarily
 * applies to specific internal points of execution within these
 * methods.  So, it is possible for one thread to invoke
 * {@code acquire} before another, but reach the ordering point after
 * the other, and similarly upon return from the method.
 * Also note that the untimed {@link #tryAcquire() tryAcquire} methods do not
 * honor the fairness setting, but will take any permits that are
 * available.
 *
 * <p>Generally, semaphores used to control resource access should be
 * initialized as fair, to ensure that no thread is starved out from
 * accessing a resource. When using semaphores for other kinds of
 * synchronization control, the throughput advantages of non-fair
 * ordering often outweigh fairness considerations.
 *
 * <p>This class also provides convenience methods to {@link
 * #acquire(int) acquire} and {@link #release(int) release} multiple
 * permits at a time. These methods are generally more efficient and
 * effective than loops. However, they do not establish any preference
 * order. For example, if thread A invokes {@code s.acquire(3}) and
 * thread B invokes {@code s.acquire(2)}, and two permits become
 * available, then there is no guarantee that thread B will obtain
 * them unless its acquire came first and Semaphore {@code s} is in
 * fair mode.
 *
 * <p>Memory consistency effects: Actions in a thread prior to calling
 * a "release" method such as {@code release()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions following a successful "acquire" method such as {@code acquire()}
 * in another thread.
 *
 * @author Doug Lea
 * @since 1.5
 */
/*
 * 信号量，适合多个线程一起工作，即在某个时间段，可以有多个线程同时持有锁（线程数量受许可证总量限制）
 *
 * 内部实现了两种锁：【共享-非公平锁】和【共享-公平锁】
 *
 * 初始化锁（同步队列）时，会生产一定数量的许可证
 * 申请锁的过程，可以看做是借出许可证，线程拿到锁的控制权时，许可证总量会减少
 * 释放锁的过程，可以看做是归还许可证，线程丧失锁的控制权时，许可证总量会增加
 */
public class Semaphore implements Serializable {
    
    private static final long serialVersionUID = -3222578661600680210L;
    
    /** All mechanics via AbstractQueuedSynchronizer subclass */
    private final Sync sync;
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code Semaphore} with the given number of
     * permits and nonfair fairness setting.
     *
     * @param permits the initial number of permits available.
     *                This value may be negative, in which case releases
     *                must occur before any acquires will be granted.
     */
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }
    
    /**
     * Creates a {@code Semaphore} with the given number of
     * permits and the given fairness setting.
     *
     * @param permits the initial number of permits available.
     *                This value may be negative, in which case releases
     *                must occur before any acquires will be granted.
     * @param fair    {@code true} if this semaphore will guarantee
     *                first-in first-out granting of permits under contention,
     *                else {@code false}
     */
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 申请/释放锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Acquires a permit from this semaphore, blocking until one is
     * available.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for a permit then it will continue to wait, but the
     * time at which the thread is assigned a permit may change compared to
     * the time it would have received the permit had no interruption
     * occurred.  When the thread does return from this method its interrupt
     * status will be set.
     */
    // 申请共享锁，允许阻塞带有中断标记的线程（会先将其标记清除）
    public void acquireUninterruptibly() {
        // 借出一张许可证
        sync.acquireShared(1);
    }
    
    /**
     * Acquires the given number of permits from this semaphore,
     * blocking until all are available.
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount. This method has the same effect as the
     * loop {@code for (int i = 0; i < permits; ++i) acquireUninterruptibly();}
     * except that it atomically acquires the permits all at once:
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes one of the {@link #release() release}
     * methods for this semaphore and the current thread is next to be assigned
     * permits and the number of available permits satisfies this request.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for permits then it will continue to wait and its
     * position in the queue is not affected.  When the thread does return
     * from this method its interrupt status will be set.
     *
     * @param permits the number of permits to acquire
     *
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    // 申请共享锁，允许阻塞带有中断标记的线程（会先将其标记清除）
    public void acquireUninterruptibly(int permits) {
        if(permits<0) {
            throw new IllegalArgumentException();
        }
        // 借出permits张许可证
        sync.acquireShared(permits);
    }
    
    /**
     * Acquires a permit from this semaphore, blocking until one is
     * available, or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    // 申请共享锁，不允许阻塞带有中断标记的线程
    public void acquire() throws InterruptedException {
        // 借出一张许可证
        sync.acquireSharedInterruptibly(1);
    }
    
    /**
     * Acquires the given number of permits from this semaphore,
     * blocking until all are available,
     * or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount. This method has the same effect as the
     * loop {@code for (int i = 0; i < permits; ++i) acquire();} except
     * that it atomically acquires the permits all at once:
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore and the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread are instead
     * assigned to other threads trying to acquire permits, as if
     * permits had been made available by a call to {@link #release()}.
     *
     * @param permits the number of permits to acquire
     *
     * @throws InterruptedException     if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    // 申请共享锁，不允许阻塞带有中断标记的线程
    public void acquire(int permits) throws InterruptedException {
        if(permits<0) {
            throw new IllegalArgumentException();
        }
        // 借出permits张许可证
        sync.acquireSharedInterruptibly(permits);
    }
    
    /**
     * Acquires a permit from this semaphore, if one becomes available
     * within the given waiting time and the current thread has not
     * been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of three things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If a permit is acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit    the time unit of the {@code timeout} argument
     *
     * @return {@code true} if a permit was acquired and {@code false}
     * if the waiting time elapsed before a permit was acquired
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    // 申请共享锁，不允许阻塞带有中断标记的线程（一次失败后，带着超时标记继续申请）
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        // 借出一张许可证
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }
    
    /**
     * Acquires the given number of permits from this semaphore, if all
     * become available within the given waiting time and the current
     * thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the given number of permits, if they are available and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore and the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If the permits are acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire the permits,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread, are instead
     * assigned to other threads trying to acquire permits, as if
     * the permits had been made available by a call to {@link #release()}.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.  Any permits that were to be assigned to this
     * thread, are instead assigned to other threads trying to acquire
     * permits, as if the permits had been made available by a call to
     * {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit    the time unit of the {@code timeout} argument
     *
     * @return {@code true} if all permits were acquired and {@code false}
     * if the waiting time elapsed before all permits were acquired
     *
     * @throws InterruptedException     if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    // 申请共享锁，不允许阻塞带有中断标记的线程（一次失败后，带着超时标记继续申请）
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        if(permits<0) {
            throw new IllegalArgumentException();
        }
        // 借出permits张许可证
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }
    
    /**
     * Acquires a permit from this semaphore, only if one is available at the
     * time of invocation.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then this method will return
     * immediately with the value {@code false}.
     *
     * <p>Even when this semaphore has been set to use a
     * fair ordering policy, a call to {@code tryAcquire()} <em>will</em>
     * immediately acquire a permit if one is available, whether or not
     * other threads are currently waiting.
     * This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor
     * the fairness setting, then use
     * {@link #tryAcquire(long, TimeUnit) tryAcquire(0, TimeUnit.SECONDS)}
     * which is almost equivalent (it also detects interruption).
     *
     * @return {@code true} if a permit was acquired and {@code false}
     * otherwise
     */
    // 申请一次非公平锁
    public boolean tryAcquire() {
        // 借出一张许可证
        return sync.nonfairTryAcquireShared(1) >= 0;
    }
    
    /**
     * Acquires the given number of permits from this semaphore, only
     * if all are available at the time of invocation.
     *
     * <p>Acquires the given number of permits, if they are available, and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then this method will return
     * immediately with the value {@code false} and the number of available
     * permits is unchanged.
     *
     * <p>Even when this semaphore has been set to use a fair ordering
     * policy, a call to {@code tryAcquire} <em>will</em>
     * immediately acquire a permit if one is available, whether or
     * not other threads are currently waiting.  This
     * &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to
     * honor the fairness setting, then use {@link #tryAcquire(int,
     * long, TimeUnit) tryAcquire(permits, 0, TimeUnit.SECONDS)}
     * which is almost equivalent (it also detects interruption).
     *
     * @param permits the number of permits to acquire
     *
     * @return {@code true} if the permits were acquired and
     * {@code false} otherwise
     *
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    // 申请一次非公平锁
    public boolean tryAcquire(int permits) {
        if(permits<0) {
            throw new IllegalArgumentException();
        }
        // 借出permits张许可证
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }
    
    /**
     * Releases a permit, returning it to the semaphore.
     *
     * <p>Releases a permit, increasing the number of available permits by
     * one.  If any threads are trying to acquire a permit, then one is
     * selected and given the permit that was just released.  That thread
     * is (re)enabled for thread scheduling purposes.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link #acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     */
    // 释放锁，并唤醒排队的结点
    public void release() {
        // 归还一张许可证
        sync.releaseShared(1);
    }
    
    /**
     * Releases the given number of permits, returning them to the semaphore.
     *
     * <p>Releases the given number of permits, increasing the number of
     * available permits by that amount.
     * If any threads are trying to acquire permits, then one thread
     * is selected and given the permits that were just released.
     * If the number of available permits satisfies that thread's request
     * then that thread is (re)enabled for thread scheduling purposes;
     * otherwise the thread will wait until sufficient permits are available.
     * If there are still permits available
     * after this thread's request has been satisfied, then those permits
     * are assigned in turn to other threads trying to acquire permits.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link Semaphore#acquire acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     *
     * @param permits the number of permits to release
     *
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    // 释放锁，并唤醒排队的结点
    public void release(int permits) {
        if(permits<0) {
            throw new IllegalArgumentException();
        }
        
        // 归还permits张许可证
        sync.releaseShared(permits);
    }
    
    /*▲ 申请/释放锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns {@code true} if this semaphore has fairness set true.
     *
     * @return {@code true} if this semaphore has fairness set true
     */
    // 判断当前线程持有的锁是否为公平锁
    public boolean isFair() {
        return sync instanceof FairSync;
    }
    
    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations may occur at any time, a {@code true}
     * return does not guarantee that any other thread will ever
     * acquire.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     * acquire the lock
     */
    // 判断【|同步队列|】中是否存在排队的结点（线程）
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }
    
    /**
     * Returns an estimate of the number of threads waiting to acquire.
     * The value is only an estimate because the number of threads may
     * change dynamically while this method traverses internal data
     * structures.  This method is designed for use in monitoring
     * system state, not for synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    // 获取【|同步队列|】中排队的结点数量
    public final int getQueueLength() {
        return sync.getQueueLength();
    }
    
    /**
     * Returns the current number of permits available in this semaphore.
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the number of permits available in this semaphore
     */
    // 获取剩余可用的许可证数量
    public int availablePermits() {
        return sync.getPermits();
    }
    
    /**
     * Acquires and returns all permits that are immediately
     * available, or if negative permits are available, releases them.
     * Upon return, zero permits are available.
     *
     * @return the number of permits acquired or, if negative, the
     * number released
     */
    // 清空当前可用的许可证数量
    public int drainPermits() {
        return sync.drainPermits();
    }
    
    /**
     * Shrinks the number of available permits by the indicated
     * reduction. This method can be useful in subclasses that use
     * semaphores to track resources that become unavailable. This
     * method differs from {@code acquire} in that it does not block
     * waiting for permits to become available.
     *
     * @param reduction the number of permits to remove
     *
     * @throws IllegalArgumentException if {@code reduction} is negative
     */
    // 将可用的许可证数量减少reduction张
    protected void reducePermits(int reduction) {
        if(reduction<0) {
            throw new IllegalArgumentException();
        }
        sync.reducePermits(reduction);
    }
    
    /**
     * Returns a collection containing threads that may be waiting to acquire.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a best-effort
     * estimate.  The elements of the returned collection are in no particular
     * order.  This method is designed to facilitate construction of
     * subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    // 返回一个集合，包含了所有正在【|同步队列|】中排队的Node中的线程
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }
    
    
    
    /**
     * Returns a string identifying this semaphore, as well as its state.
     * The state, in brackets, includes the String {@code "Permits ="}
     * followed by the number of permits.
     *
     * @return a string identifying this semaphore, as well as its state
     */
    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
    
    
    
    /**
     * Synchronization implementation for semaphore.  Uses AQS state
     * to represent permits. Subclassed into fair and nonfair
     * versions.
     */
    /*
     * 同步队列的实现者，实现了锁的语义
     *
     * 许可证数量==0：当前锁的许可证已全部借出，后来的线程只能等待
     * 许可证数量>0，当前锁仍然有可用的许可证，后来的线程可以尝试申请锁
     *
     * 当线程申请锁时，需要借出许可证，许可证数量减少，直到为0
     * 当线程释放锁时，需要归还许可证，许可证数量增加，直到设定的初始值
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;
        
        // 初始化锁时，生成一定数量的许可证
        Sync(int permits) {
            setState(permits);
        }
        
        // 允许单个或多个线程多次申请锁（借出许可证）
        final int nonfairTryAcquireShared(int acquires) {
            for(; ; ) {
                // 获取当前可用的许可证数量
                int available = getState();
                
                // 计算借出一定数量许可证之后，还剩余的许可证数量
                int remaining = available - acquires;
                
                // 如果许可证数量不足，说明本次抢锁失败
                if(remaining<0) {
                    // 返回【预期剩余】的许可证数量，<0
                    return remaining;
                }
                
                /* 至此，说明许可证数量充足，即成功借到了许可证 */
                
                // 更新许可证数量
                if(compareAndSetState(available, remaining)) {
                    // 返回【实际剩余】的许可证数量，>=0
                    return remaining;
                }
            }
        }
        
        // 允许单个或多个线程多次申请锁（借出许可证）
        final int fairTryAcquireShared(int acquires) {
            for(; ; ) {
                /* 每次借出许可证时，需要检查有没有其他线程在排队，如果没人在排队，才尝试借用许可证，这也是"公平"所在 */
                
                // 判断【|同步队列|】的队头是否还有其他（非当前线程）的线程在排队
                if(hasQueuedPredecessors()) {
                    return -1;
                }
                
                // 获取当前可用的许可证数量
                int available = getState();
                
                // 计算借出一定数量许可证之后，还剩余的许可证数量
                int remaining = available - acquires;
                
                // 如果许可证数量不足，说明本次抢锁失败
                if(remaining<0) {
                    // 返回【预期剩余】的许可证数量，<0
                    return remaining;
                }
                
                /* 至此，说明许可证数量充足，即成功借到了许可证 */
                
                // 更新许可证数量
                if(compareAndSetState(available, remaining)) {
                    // 返回【实际剩余】的许可证数量，>=0
                    return remaining;
                }
            }
        }
        
        // 释放锁，即归还许可证
        protected final boolean tryReleaseShared(int releases) {
            for(; ; ) {
                // 计算应该剩余的许可证数量
                int current = getState();
                
                // 计算归还许可证之后的许可证数量
                int next = current + releases;
                if(next<current) {
                    // 归还许可证之后许可证数量应该增加而不是减少
                    throw new Error("Maximum permit count exceeded");
                }
                
                // 原子地更新许可证数量
                if(compareAndSetState(current, next)) {
                    return true;
                }
            }
        }
        
        // 将可用的许可证数量减少reduction张
        final void reducePermits(int reductions) {
            for(; ; ) {
                int current = getState();
                int next = current - reductions;
                // underflow
                if(next>current) {
                    throw new Error("Permit count underflow");
                }
                if(compareAndSetState(current, next)) {
                    return;
                }
            }
        }
        
        // 获取剩余可用的许可证数量
        final int getPermits() {
            return getState();
        }
        
        // 清空当前可用的许可证数量
        final int drainPermits() {
            for(; ; ) {
                int current = getState();
                if(current == 0 || compareAndSetState(current, 0)) {
                    return current;
                }
            }
        }
    }
    
    /**
     * NonFair version
     */
    /*
     * 非公平锁
     *
     * 许可证数量充足时：（体现了非公平性）
     * 如果线程T进入了抢锁状态，则不管同步队列中有没有其他排队线程，都会抢锁成功
     *
     * 许可证数量不足时：
     * 不管谁来抢锁，都会失败
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;
        
        NonfairSync(int permits) {
            super(permits);
        }
        
        // 申请一次非公平锁
        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }
    
    /**
     * Fair version
     */
    /*
     * 公平锁
     *
     * 许可证数量充足时：（体现了公平性）
     * 如果线程T进入了抢锁状态，则需要先检查同步队列队头是否还有其他排队线程
     * 如果同步队列中有其他排队线程，则本次抢锁失败，否则，抢锁成功
     *
     * 许可证数量不足时：
     * 不管谁来抢锁，都会失败
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;
        
        FairSync(int permits) {
            super(permits);
        }
        
        // 申请一次公平锁
        protected int tryAcquireShared(int acquires) {
            return fairTryAcquireShared(acquires);
        }
    }
    
}
