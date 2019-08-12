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

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Provides a framework for implementing blocking locks and related
 * synchronizers (semaphores, events, etc) that rely on
 * first-in-first-out (FIFO) wait queues.  This class is designed to
 * be a useful basis for most kinds of synchronizers that rely on a
 * single atomic {@code int} value to represent state. Subclasses
 * must define the protected methods that change this state, and which
 * define what that state means in terms of this object being acquired
 * or released.  Given these, the other methods in this class carry
 * out all queuing and blocking mechanics. Subclasses can maintain
 * other state fields, but only the atomically updated {@code int}
 * value manipulated using methods {@link #getState}, {@link
 * #setState} and {@link #compareAndSetState} is tracked with respect
 * to synchronization.
 *
 * <p>Subclasses should be defined as non-public internal helper
 * classes that are used to implement the synchronization properties
 * of their enclosing class.  Class
 * {@code AbstractQueuedSynchronizer} does not implement any
 * synchronization interface.  Instead it defines methods such as
 * {@link #acquireInterruptibly} that can be invoked as
 * appropriate by concrete locks and related synchronizers to
 * implement their public methods.
 *
 * <p>This class supports either or both a default <em>exclusive</em>
 * mode and a <em>shared</em> mode. When acquired in exclusive mode,
 * attempted acquires by other threads cannot succeed. Shared mode
 * acquires by multiple threads may (but need not) succeed. This class
 * does not &quot;understand&quot; these differences except in the
 * mechanical sense that when a shared mode acquire succeeds, the next
 * waiting thread (if one exists) must also determine whether it can
 * acquire as well. Threads waiting in the different modes share the
 * same FIFO queue. Usually, implementation subclasses support only
 * one of these modes, but both can come into play for example in a
 * {@link ReadWriteLock}. Subclasses that support only exclusive or
 * only shared modes need not define the methods supporting the unused mode.
 *
 * <p>This class defines a nested {@link ConditionObject} class that
 * can be used as a {@link Condition} implementation by subclasses
 * supporting exclusive mode for which method {@link
 * #isHeldExclusively} reports whether synchronization is exclusively
 * held with respect to the current thread, method {@link #release}
 * invoked with the current {@link #getState} value fully releases
 * this object, and {@link #acquire}, given this saved state value,
 * eventually restores this object to its previous acquired state.  No
 * {@code AbstractQueuedSynchronizer} method otherwise creates such a
 * condition, so if this constraint cannot be met, do not use it.  The
 * behavior of {@link ConditionObject} depends of course on the
 * semantics of its synchronizer implementation.
 *
 * <p>This class provides inspection, instrumentation, and monitoring
 * methods for the internal queue, as well as similar methods for
 * condition objects. These can be exported as desired into classes
 * using an {@code AbstractQueuedSynchronizer} for their
 * synchronization mechanics.
 *
 * <p>Serialization of this class stores only the underlying atomic
 * integer maintaining state, so deserialized objects have empty
 * thread queues. Typical subclasses requiring serializability will
 * define a {@code readObject} method that restores this to a known
 * initial state upon deserialization.
 *
 * <h3>Usage</h3>
 *
 * <p>To use this class as the basis of a synchronizer, redefine the
 * following methods, as applicable, by inspecting and/or modifying
 * the synchronization state using {@link #getState}, {@link
 * #setState} and/or {@link #compareAndSetState}:
 *
 * <ul>
 * <li>{@link #tryAcquire}
 * <li>{@link #tryRelease}
 * <li>{@link #tryAcquireShared}
 * <li>{@link #tryReleaseShared}
 * <li>{@link #isHeldExclusively}
 * </ul>
 *
 * Each of these methods by default throws {@link
 * UnsupportedOperationException}.  Implementations of these methods
 * must be internally thread-safe, and should in general be short and
 * not block. Defining these methods is the <em>only</em> supported
 * means of using this class. All other methods are declared
 * {@code final} because they cannot be independently varied.
 *
 * <p>You may also find the inherited methods from {@link
 * AbstractOwnableSynchronizer} useful to keep track of the thread
 * owning an exclusive synchronizer.  You are encouraged to use them
 * -- this enables monitoring and diagnostic tools to assist users in
 * determining which threads hold locks.
 *
 * <p>Even though this class is based on an internal FIFO queue, it
 * does not automatically enforce FIFO acquisition policies.  The core
 * of exclusive synchronization takes the form:
 *
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *        <em>enqueue thread if it is not already queued</em>;
 *        <em>possibly block current thread</em>;
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *        <em>unblock the first queued thread</em>;
 * </pre>
 *
 * (Shared mode is similar but may involve cascading signals.)
 *
 * <p id="barging">Because checks in acquire are invoked before
 * enqueuing, a newly acquiring thread may <em>barge</em> ahead of
 * others that are blocked and queued.  However, you can, if desired,
 * define {@code tryAcquire} and/or {@code tryAcquireShared} to
 * disable barging by internally invoking one or more of the inspection
 * methods, thereby providing a <em>fair</em> FIFO acquisition order.
 * In particular, most fair synchronizers can define {@code tryAcquire}
 * to return {@code false} if {@link #hasQueuedPredecessors} (a method
 * specifically designed to be used by fair synchronizers) returns
 * {@code true}.  Other variations are possible.
 *
 * <p>Throughput and scalability are generally highest for the
 * default barging (also known as <em>greedy</em>,
 * <em>renouncement</em>, and <em>convoy-avoidance</em>) strategy.
 * While this is not guaranteed to be fair or starvation-free, earlier
 * queued threads are allowed to recontend before later queued
 * threads, and each recontention has an unbiased chance to succeed
 * against incoming threads.  Also, while acquires do not
 * &quot;spin&quot; in the usual sense, they may perform multiple
 * invocations of {@code tryAcquire} interspersed with other
 * computations before blocking.  This gives most of the benefits of
 * spins when exclusive synchronization is only briefly held, without
 * most of the liabilities when it isn't. If so desired, you can
 * augment this by preceding calls to acquire methods with
 * "fast-path" checks, possibly prechecking {@link #hasContended}
 * and/or {@link #hasQueuedThreads} to only do so if the synchronizer
 * is likely not to be contended.
 *
 * <p>This class provides an efficient and scalable basis for
 * synchronization in part by specializing its range of use to
 * synchronizers that can rely on {@code int} state, acquire, and
 * release parameters, and an internal FIFO wait queue. When this does
 * not suffice, you can build synchronizers from a lower level using
 * {@link java.util.concurrent.atomic atomic} classes, your own custom
 * {@link java.util.Queue} classes, and {@link LockSupport} blocking
 * support.
 *
 * <h3>Usage Examples</h3>
 *
 * <p>Here is a non-reentrant mutual exclusion lock class that uses
 * the value zero to represent the unlocked state, and one to
 * represent the locked state. While a non-reentrant lock
 * does not strictly require recording of the current owner
 * thread, this class does so anyway to make usage easier to monitor.
 * It also supports conditions and exposes some instrumentation methods:
 *
 * <pre> {@code
 * class Mutex implements Lock, java.io.Serializable {
 *
 *   // Our internal helper class
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // Acquires the lock if state is zero
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // Otherwise unused
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // Releases the lock by setting state to zero
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused
 *       if (!isHeldExclusively())
 *         throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // Reports whether in locked state
 *     public boolean isLocked() {
 *       return getState() != 0;
 *     }
 *
 *     public boolean isHeldExclusively() {
 *       // a data race, but safe due to out-of-thin-air guarantees
 *       return getExclusiveOwnerThread() == Thread.currentThread();
 *     }
 *
 *     // Provides a Condition
 *     public Condition newCondition() {
 *       return new ConditionObject();
 *     }
 *
 *     // Deserializes properly
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // reset to unlocked state
 *     }
 *   }
 *
 *   // The sync object does all the hard work. We just forward to it.
 *   private final Sync sync = new Sync();
 *
 *   public void lock()              { sync.acquire(1); }
 *   public boolean tryLock()        { return sync.tryAcquire(1); }
 *   public void unlock()            { sync.release(1); }
 *   public Condition newCondition() { return sync.newCondition(); }
 *   public boolean isLocked()       { return sync.isLocked(); }
 *   public boolean isHeldByCurrentThread() {
 *     return sync.isHeldExclusively();
 *   }
 *   public boolean hasQueuedThreads() {
 *     return sync.hasQueuedThreads();
 *   }
 *   public void lockInterruptibly() throws InterruptedException {
 *     sync.acquireInterruptibly(1);
 *   }
 *   public boolean tryLock(long timeout, TimeUnit unit)
 *       throws InterruptedException {
 *     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
 *   }
 * }}</pre>
 *
 * <p>Here is a latch class that is like a
 * {@link java.util.concurrent.CountDownLatch CountDownLatch}
 * except that it only requires a single {@code signal} to
 * fire. Because a latch is non-exclusive, it uses the {@code shared}
 * acquire and release methods.
 *
 * <pre> {@code
 * class BooleanLatch {
 *
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     boolean isSignalled() { return getState() != 0; }
 *
 *     protected int tryAcquireShared(int ignore) {
 *       return isSignalled() ? 1 : -1;
 *     }
 *
 *     protected boolean tryReleaseShared(int ignore) {
 *       setState(1);
 *       return true;
 *     }
 *   }
 *
 *   private final Sync sync = new Sync();
 *   public boolean isSignalled() { return sync.isSignalled(); }
 *   public void signal()         { sync.releaseShared(1); }
 *   public void await() throws InterruptedException {
 *     sync.acquireSharedInterruptibly(1);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
// 同步队列，是一个带头结点的双向链表，用于实现锁的语义
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements Serializable {
    
    private static final long serialVersionUID = 7373984972572414691L;
    
    /**
     * The number of nanoseconds for which it is faster to spin rather than to use timed park.
     * A rough estimate suffices to improve responsiveness with very short timeouts.
     */
    static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000L;
    
    /**
     * Head of the wait queue, lazily initialized.  Except for
     * initialization, it is modified only via method setHead.  Note:
     * If head exists, its waitStatus is guaranteed not to be
     * CANCELLED.
     */
    private transient volatile Node head;   // 【|同步队列|】的头结点
    
    /**
     * Tail of the wait queue, lazily initialized.  Modified only via
     * method enq to add new wait node.
     */
    private transient volatile Node tail;   // 【|同步队列|】的尾结点
    
    /**
     * The synchronization state.
     */
    // 重入锁计数/许可证数量，在不同的锁中，使用方式有所不同
    private volatile int state;
    
    // VarHandle mechanics
    private static final VarHandle STATE;   // 保存字段 state 的内存地址
    private static final VarHandle HEAD;    // 保存字段 head  的内存地址
    private static final VarHandle TAIL;    // 保存字段 tail  的内存地址
    
    
    static {
        try {
            // 获取这些字段的内存地址
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE = l.findVarHandle(AbstractQueuedSynchronizer.class, "state", int.class);
            HEAD = l.findVarHandle(AbstractQueuedSynchronizer.class, "head", Node.class);
            TAIL = l.findVarHandle(AbstractQueuedSynchronizer.class, "tail", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
        
        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new {@code AbstractQueuedSynchronizer} instance
     * with initial synchronization state of zero.
     */
    protected AbstractQueuedSynchronizer() {
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 独占锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /* 申请 ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼  */
    
    /**
     * Acquires in exclusive mode, ignoring interrupts.
     * Implemented by invoking at least once {@link #tryAcquire}, returning on success.
     * Otherwise the thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquire} until success.
     * This method can be used to implement method {@link Lock#lock}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *            {@link #tryAcquire} but is otherwise uninterpreted and
     *            can represent anything you like.
     */
    // 申请独占锁，允许阻塞带有中断标记的线程（会先将其标记清除）
    public final void acquire(int arg) {
        // 尝试申请独占锁
        if(!tryAcquire(arg)){
            /*
             * 如果当前线程没有申请到独占锁，则需要去排队
             * 注：线程被封装到Node中去排队
             */
            
            // 向【|同步队列|】添加一个[独占模式Node](持有争锁线程)作为排队者
            Node node = addWaiter(Node.EXCLUSIVE);
            
            // 当node进入排队后再次尝试申请锁，如果还是失败，则可能进入阻塞
            if(acquireQueued(node, arg)){
                // 如果线程解除阻塞时拥有中断标记，此处要进行设置
                selfInterrupt();
            }
        }
    }
    
    /**
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *            passed to an acquire method, or is the value saved on entry
     *            to a condition wait.  The value is otherwise uninterpreted
     *            and can represent anything you like.
     *
     * @return {@code true} if successful. Upon success, this object has
     * been acquired.
     *
     * @throws IllegalMonitorStateException  if acquiring would place this
     *                                       synchronizer in an illegal state. This exception must be
     *                                       thrown in a consistent fashion for synchronization to work
     *                                       correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    // 申请一次独占锁，具体的行为模式由子类实现
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Acquires in exclusive uninterruptible mode for thread already in queue.
     * Used by condition wait methods as well as acquire.
     *
     * @param node the node
     * @param arg  the acquire argument
     *
     * @return {@code true} if interrupted while waiting
     */
    // 当node进入排队后再次尝试申请锁，如果还是失败，则可能进入阻塞
    final boolean acquireQueued(final Node node, int arg) {
        
        // 记录当前线程从阻塞中醒来时的中断标记（阻塞(park)期间也可设置中断标记）
        boolean interrupted = false;
        
        try {
            /*
             * 死循环，成功申请到锁后退出
             *
             * 每个陷入阻塞的线程醒来后，需要重新申请锁
             * 只有当自身排在队首时，才有权利申请锁
             * 申请成功后，需要丢弃原来的头结点，并将自身作为头结点，然后返回
             */
            for(; ; ) {
                // 获取node结点的前驱
                final Node p = node.predecessor();
                
                // 如果node结点目前排在了队首，则node线程有权利申请锁
                if(p == head) {
                    // 再次尝试申请锁
                    if(tryAcquire(arg)){
                        // 设置node为头结点（即丢掉了原来的头结点）
                        setHead(node);
                        
                        // 切断旧的头结点与后一个结点的联系，以便GC
                        p.next = null;
                        
                        // 返回线程当前的中断标记（如果线程在阻塞期间被标记为中断，这里会返回true）
                        return interrupted;
                    }
                }
                
                // 抢锁失败时，尝试为node的前驱设置阻塞标记（每个结点的阻塞标记设置在其前驱上）
                if(shouldParkAfterFailedAcquire(p, node)) {
                    /*
                     * 使线程陷入阻塞
                     *
                     * 如果首次到达这里时线程被标记为中断，则此步只是简单地清除中断标记，并返回true
                     * 接下来，通过死循环，线程再次来到这里，然后进入阻塞(park)...
                     *
                     * 如果首次到达这里时线程没有被标记为中断，则直接进入阻塞(park)
                     *
                     * 当线程被唤醒后，返回线程当前的中断标记（阻塞(park)期间也可设置中断标记）
                     */
                    interrupted |= parkAndCheckInterrupt();
                }
            }
        } catch(Throwable t) {
            // 如果中途有异常发生，应当撤销当前线程对锁的申请
            cancelAcquire(node);
            
            // 如果发生异常时拥有中断标记，此处要进行设置
            if(interrupted) {
                selfInterrupt();
            }
            
            throw t;
        }
    }
    
    /**
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *            {@link #tryAcquire} but is otherwise uninterpreted and
     *            can represent anything you like.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    // 申请独占锁，不允许阻塞带有中断标记的线程
    public final void acquireInterruptibly(int arg) throws InterruptedException {
        // 测试当前线程是否已经中断，线程的中断状态会被清除
        if(Thread.interrupted()) {
            // 如果当前线程有中断标记，则抛出异常
            throw new InterruptedException();
        }
        
        // 尝试申请独占锁
        if(!tryAcquire(arg)) {
            doAcquireInterruptibly(arg);
        }
    }
    
    /**
     * Acquires in exclusive interruptible mode.
     *
     * @param arg the acquire argument
     */
    // 抢锁失败后，尝试将其阻塞
    private void doAcquireInterruptibly(int arg) throws InterruptedException {
        // 向【|同步队列|】添加一个[独占模式Node]作为排队者
        final Node node = addWaiter(Node.EXCLUSIVE);
        
        try {
            // 死循环，成功申请到锁后退出
            for(; ; ) {
                // 获取node结点的前驱
                final Node p = node.predecessor();
                
                // 如果node结点目前排在了队首，则node线程有权利申请锁
                if(p == head) {
                    // 尝试申请锁
                    if(tryAcquire(arg)){
                        // 设置node为头结点（即丢掉了原来的头结点）
                        setHead(node);
                        // 切断旧的头结点与后一个结点的联系，以便GC
                        p.next = null;
                        return;
                    }
                }
                
                // 抢锁失败时，尝试为node的前驱设置阻塞标记（每个结点的阻塞标记设置在其前驱上）
                if(shouldParkAfterFailedAcquire(p, node)) {
                    // 设置当前线程进入阻塞状态，并清除当前线程的中断状态
                    if(parkAndCheckInterrupt()){
                        // 如果线程被唤醒时拥有中断标记（在阻塞期间设置的），这里抛出异常
                        throw new InterruptedException();
                    }
                }
            }
        } catch(Throwable t) {
            // 如果中途有异常发生，应当撤销当前线程对锁的申请
            cancelAcquire(node);
            throw t;
        }
    }
    
    /**
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param arg          the acquire argument.  This value is conveyed to
     *                     {@link #tryAcquire} but is otherwise uninterpreted and
     *                     can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     *
     * @return {@code true} if acquired; {@code false} if timed out
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    // 申请独占锁，不允许阻塞带有中断标记的线程（一次失败后，带着超时标记继续申请）
    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        // 测试当前线程是否已经中断，线程的中断状态会被清除
        if(Thread.interrupted()) {
            // 如果当前线程有中断标记，则抛出异常
            throw new InterruptedException();
        }
        
        return tryAcquire(arg) // 申请一次锁
            || doAcquireNanos(arg, nanosTimeout); // 抢锁失败的线程再次尝试抢锁（设置了超时）
    }
    
    /**
     * Acquires in exclusive timed mode.
     *
     * @param arg          the acquire argument
     * @param nanosTimeout max wait time
     *
     * @return {@code true} if acquired
     */
    /*
     * 申请独占锁，带有超时标记
     *
     * 如果nanosTimeout<=1000，则在1000纳秒内，不断轮询，尝试获取锁
     * 如果nanosTimeout>1000，则线程抢锁失败后，会进入阻塞（累计阻塞时长不超过nanosTimeout纳秒）
     * 阻塞可能中途被唤醒，也可能自然醒来
     * 不管哪种方式醒来的，只要醒来就再次尝试获取锁
     * 如果是中途醒来的，且获取锁失败，那么会继续阻塞剩余的时长，直至超时
     * 如果是自然醒来的，且抢锁失败，那么说明已经超时了
     * 只要到了超时，则需要取消任务，并返回fasle，代表抢锁失败
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        // 已经超时的话就返回
        if(nanosTimeout<=0L) {
            return false;
        }
        
        // 计算结束时间
        final long deadline = System.nanoTime() + nanosTimeout;
        
        // 向【|同步队列|】添加一个[独占模式Node]作为排队者
        final Node node = addWaiter(Node.EXCLUSIVE);
        
        try {
            // 死循环，成功申请到锁后退出
            for(; ; ) {
                // 获取node结点的前驱
                final Node p = node.predecessor();
                
                // 如果node结点目前排在了队首，则node线程有权利申请锁
                if(p == head) {
                    // 尝试申请独占锁
                    if(tryAcquire(arg)){
                        // 设置node为头结点（即丢掉了原来的头结点）
                        setHead(node);
                        // 切断旧的头结点与后一个结点的联系，以便GC
                        p.next = null;
                        return true;
                    }
                }
                
                // 判断是否超时（因为可能是半道被唤醒的）
                nanosTimeout = deadline - System.nanoTime();
                
                // 已经超时，取消任务
                if(nanosTimeout<=0L) {
                    // 标记node结点为Node.CANCELLED（取消）状态
                    cancelAcquire(node);
                    return false;
                }
                
                // 抢锁失败时，尝试为node的前驱设置阻塞标记（每个结点的阻塞标记设置在其前驱上）
                if(shouldParkAfterFailedAcquire(p, node)) {
                    if(nanosTimeout>SPIN_FOR_TIMEOUT_THRESHOLD){
                        // 使线程阻塞nanosTimeout（单位：纳秒）时长后自动醒来（中途可被唤醒）
                        LockSupport.parkNanos(this, nanosTimeout);
                    }
                }
                
                // 测试当前线程是否已经中断，线程的中断状态会被清除
                if(Thread.interrupted()) {
                    // 如果当前线程有中断标记，则抛出异常
                    throw new InterruptedException();
                }
            }
        } catch(Throwable t) {
            // 如果中途有异常发生，应当撤销当前线程对锁的申请
            cancelAcquire(node);
            throw t;
        }
    }
    
    /* 申请 ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  */
    
    
    /* 释放 ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ */
    
    /**
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     *
     * @param arg the release argument.  This value is conveyed to
     *            {@link #tryRelease} but is otherwise uninterpreted and
     *            can represent anything you like.
     *
     * @return the value returned from {@link #tryRelease}
     */
    // 释放锁，如果锁已被完全释放，则唤醒后续的阻塞线程。返回值表示本次操作后锁是否自由
    public final boolean release(int arg) {
        // 释放一次锁，返回值表示同步锁是否处于自由状态（无线程持有）
        if(tryRelease(arg)) {
            /* 如果锁已经处于自由状态，则可以唤醒下一个阻塞的线程了 */
            
            Node h = head;
            if(h != null && h.waitStatus != 0) {
                // 唤醒h后面陷入阻塞的“后继”
                unparkSuccessor(h);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Attempts to set the state to reflect a release in exclusive
     * mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *            passed to a release method, or the current state value upon
     *            entry to a condition wait.  The value is otherwise
     *            uninterpreted and can represent anything you like.
     *
     * @return {@code true} if this object is now in a fully released
     * state, so that any waiting threads may attempt to acquire;
     * and {@code false} otherwise.
     *
     * @throws IllegalMonitorStateException  if releasing would place this
     *                                       synchronizer in an illegal state. This exception must be
     *                                       thrown in a consistent fashion for synchronization to work
     *                                       correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    // 释放一次锁，返回值表示同步锁是否处于自由状态（无线程持有）
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }
    
    /* 释放 ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ */
    
    /*▲ 独占锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 共享锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /* 申请 ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼  */
    
    /**
     * Acquires in shared mode, ignoring interrupts.  Implemented by
     * first invoking at least once {@link #tryAcquireShared},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquireShared} until success.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *            {@link #tryAcquireShared} but is otherwise uninterpreted
     *            and can represent anything you like.
     */
    // 申请共享锁，允许阻塞带有中断标记的线程（会先将其标记清除）
    public final void acquireShared(int arg) {
        // 尝试申请锁，返回值<0说明刚才抢锁失败
        if(tryAcquireShared(arg)<0) {
            doAcquireShared(arg);
        }
    }
    
    /**
     * Attempts to acquire in shared mode. This method should query if
     * the state of the object permits it to be acquired in the shared
     * mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread.
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *            passed to an acquire method, or is the value saved on entry
     *            to a condition wait.  The value is otherwise uninterpreted
     *            and can represent anything you like.
     *
     * @return a negative value on failure; zero if acquisition in shared
     * mode succeeded but no subsequent shared-mode acquire can
     * succeed; and a positive value if acquisition in shared
     * mode succeeded and subsequent shared-mode acquires might
     * also succeed, in which case a subsequent waiting thread
     * must check availability. (Support for three different
     * return values enables this method to be used in contexts
     * where acquires only sometimes act exclusively.)  Upon
     * success, this object has been acquired.
     *
     * @throws IllegalMonitorStateException  if acquiring would place this
     *                                       synchronizer in an illegal state. This exception must be
     *                                       thrown in a consistent fashion for synchronization to work
     *                                       correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    // 申请共享锁，具体的行为模式由子类实现
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Acquires in shared uninterruptible mode.
     *
     * @param arg the acquire argument
     */
    // 当node进入排队后再次尝试申请锁，如果还是失败，则可能进入阻塞
    private void doAcquireShared(int arg) {
        // 向【|同步队列|】添加一个[共享模式Node]作为排队者
        final Node node = addWaiter(Node.SHARED);
        
        // 记录当前线程从阻塞中醒来时的中断状态（阻塞(park)期间也可设置中断标记）
        boolean interrupted = false;
        
        try {
            // 死循环，成功申请到锁后退出
            for(; ; ) {
                // 获取node结点的前驱
                final Node p = node.predecessor();
                
                // 如果node结点目前排在了队首，则node线程有权利申请锁
                if(p == head) {
                    // 再次尝试申请锁
                    int r = tryAcquireShared(arg);
                    if(r >= 0) {
                        // 更新头结点为node，并为其设置Node.PROPAGATE标记，或唤醒其后续结点
                        setHeadAndPropagate(node, r);
                        
                        // 切断旧的头结点与后一个结点的联系，以便GC
                        p.next = null;
                        
                        return;
                    }
                }
                
                // 抢锁失败时，尝试为node的前驱设置阻塞标记（每个结点的阻塞标记设置在其前驱上）
                if(shouldParkAfterFailedAcquire(p, node)) {
                    /*
                     * 如果首次到达这里时线程被标记为中断，则此步只是简单地清除中断标记，并返回true
                     * 接下来，通过死循环，线程再次来到这里，然后进入阻塞(park)...
                     *
                     * 如果首次到达这里时线程没有被标记为中断，则直接进入阻塞(park)
                     *
                     * 当线程被唤醒后，返回线程当前的中断标记（阻塞(park)期间也可设置中断标记）
                     */
                    interrupted |= parkAndCheckInterrupt();
                }
            }
        } catch(Throwable t) {
            // 如果中途有异常发生，应当撤销当前线程对锁的申请
            cancelAcquire(node);
            throw t;
        } finally {
            // 如果线程解除阻塞时拥有中断标记，此处要进行设置
            if(interrupted) {
                selfInterrupt();
            }
        }
    }
    
    /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     *
     * @param arg the acquire argument.
     *            This value is conveyed to {@link #tryAcquireShared} but is
     *            otherwise uninterpreted and can represent anything
     *            you like.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    // 申请共享锁，不允许阻塞带有中断标记的线程
    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        // 测试当前线程是否已经中断，线程的中断状态会被清除
        if(Thread.interrupted()) {
            // 如果当前线程有中断标记，则抛出异常
            throw new InterruptedException();
        }
        
        // 尝试申请共享锁
        if(tryAcquireShared(arg)<0) {
            doAcquireSharedInterruptibly(arg);
        }
    }
    
    /**
     * Acquires in shared interruptible mode.
     *
     * @param arg the acquire argument
     */
    // 抢锁失败后，尝试将其阻塞
    private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
        // 向【|同步队列|】添加一个[共享模式Node]作为排队者
        final Node node = addWaiter(Node.SHARED);
        try {
            // 死循环，成功申请到锁后退出
            for(; ; ) {
                // 获取node结点的前驱
                final Node p = node.predecessor();
                
                // 如果node结点目前排在了队首，则node线程有权利申请锁
                if(p == head) {
                    // 尝试申请锁
                    int r = tryAcquireShared(arg);
                    if(r >= 0) {
                        // 更新头结点为node，并为其设置Node.PROPAGATE标记，或唤醒其后续结点
                        setHeadAndPropagate(node, r);
                        // 切断旧的头结点与后一个结点的联系，以便GC
                        p.next = null;
                        return;
                    }
                }
                
                // 抢锁失败时，尝试为node的前驱设置阻塞标记（每个结点的阻塞标记设置在其前驱上）
                if(shouldParkAfterFailedAcquire(p, node)) {
                    // 设置当前线程进入阻塞状态，并清除当前线程的中断状态
                    if(parkAndCheckInterrupt()){
                        // 如果线程被唤醒时拥有中断标记（在阻塞期间设置的），这里抛出异常
                        throw new InterruptedException();
                    }
                }
            }
        } catch(Throwable t) {
            // 如果中途有异常发生，应当撤销当前线程对锁的申请
            cancelAcquire(node);
            throw t;
        }
    }
    
    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
     *
     * @param arg          the acquire argument.  This value is conveyed to
     *                     {@link #tryAcquireShared} but is otherwise uninterpreted
     *                     and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     *
     * @return {@code true} if acquired; {@code false} if timed out
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    // 申请共享锁，不允许阻塞带有中断标记的线程（一次失败后，带着超时标记继续申请）
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        // 测试当前线程是否已经中断，线程的中断状态会被清除
        if(Thread.interrupted()) {
            // 如果当前线程有中断标记，则抛出异常
            throw new InterruptedException();
        }
        
        return tryAcquireShared(arg) >= 0   // 申请锁
            || doAcquireSharedNanos(arg, nanosTimeout); // 抢锁失败的线程再次尝试抢锁（设置了超时）
    }
    
    /**
     * Acquires in shared timed mode.
     *
     * @param arg          the acquire argument
     * @param nanosTimeout max wait time
     *
     * @return {@code true} if acquired
     */
    /*
     * 申请共享锁，带有超时标记
     *
     * 如果nanosTimeout<=1000，则在1000纳秒内，不断轮询，尝试获取锁
     * 如果nanosTimeout>1000，则线程抢锁失败后，会进入阻塞（累计阻塞时长不超过nanosTimeout纳秒）
     * 阻塞可能中途被唤醒，也可能自然醒来
     * 不管哪种方式醒来的，只要醒来就再次尝试获取锁
     * 如果是中途醒来的，且获取锁失败，那么会继续阻塞剩余的时长，直至超时
     * 如果是自然醒来的，且抢锁失败，那么说明已经超时了
     * 只要到了超时，则需要取消任务，并返回fasle，代表抢锁失败
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        // 已经超时的话就返回
        if(nanosTimeout<=0L) {
            return false;
        }
        
        // 计算结束时间
        final long deadline = System.nanoTime() + nanosTimeout;
        
        // 向【|同步队列|】添加一个[共享模式Node]作为排队者
        final Node node = addWaiter(Node.SHARED);
        try {
            // 死循环，成功申请到锁后退出
            for(; ; ) {
                // 获取node结点的前驱
                final Node p = node.predecessor();
                
                // 如果node结点目前排在了队首，则node线程有权利申请独占锁
                if(p == head) {
                    // 尝试申请锁
                    int r = tryAcquireShared(arg);
                    if(r >= 0) {
                        // 更新头结点为node，并为其设置Node.PROPAGATE标记，或唤醒其后续结点
                        setHeadAndPropagate(node, r);
                        
                        // 切断旧的头结点与后一个结点的联系，以便GC
                        p.next = null;
                        
                        return true;
                    }
                }
                
                // 判断是否超时（因为可能是半道被唤醒的）
                nanosTimeout = deadline - System.nanoTime();
                
                // 已经超时，取消任务
                if(nanosTimeout<=0L) {
                    // 标记node结点为Node.CANCELLED（取消）状态
                    cancelAcquire(node);
                    return false;
                }
                
                // 抢锁失败时，尝试为node的前驱设置阻塞标记（每个结点的阻塞标记设置在其前驱上）
                if(shouldParkAfterFailedAcquire(p, node)) {
                    if(nanosTimeout>SPIN_FOR_TIMEOUT_THRESHOLD){
                        // 使线程阻塞nanosTimeout（单位：纳秒）时长后自动醒来（中途可被唤醒）
                        LockSupport.parkNanos(this, nanosTimeout);
                    }
                }
                
                // 测试当前线程是否已经中断，线程的中断状态会被清除
                if(Thread.interrupted()) {
                    // 如果当前线程有中断标记，则抛出异常
                    throw new InterruptedException();
                }
            }
        } catch(Throwable t) {
            // 如果中途有异常发生，应当撤销当前线程对锁的申请
            cancelAcquire(node);
            throw t;
        }
    }
    
    /* 申请 ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  */
    
    
    /* 释放 ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼  */
    
    /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *
     * @param arg the release argument.  This value is conveyed to
     *            {@link #tryReleaseShared} but is otherwise uninterpreted
     *            and can represent anything you like.
     *
     * @return the value returned from {@link #tryReleaseShared}
     */
    // 释放锁，并唤醒排队的结点
    public final boolean releaseShared(int arg) {
        // 释放锁，即归还许可证
        if(tryReleaseShared(arg)) {
            // 此处用作唤醒后续阻塞的结点
            doReleaseShared();
            return true;
        }
        return false;
    }
    
    /**
     * Attempts to set the state to reflect a release in shared mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *            passed to a release method, or the current state value upon
     *            entry to a condition wait.  The value is otherwise
     *            uninterpreted and can represent anything you like.
     *
     * @return {@code true} if this release of shared mode may permit a
     * waiting acquire (shared or exclusive) to succeed; and
     * {@code false} otherwise
     *
     * @throws IllegalMonitorStateException  if releasing would place this
     *                                       synchronizer in an illegal state. This exception must be
     *                                       thrown in a consistent fashion for synchronization to work
     *                                       correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    // 释放锁，归还许可证
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Release action for shared mode -- signals successor and ensures
     * propagation. (Note: For exclusive mode, release just amounts
     * to calling unparkSuccessor of head if it needs signal.)
     */
    /*
     * 为共享结点设置Node.PROPAGATE标记，或唤醒其下一个结点
     *
     * 在setHeadAndPropagate()中被调用时，
     * 可能用来为头结点设置Node.PROPAGATE标记，也可能是唤醒下一个结点
     *
     * 在releaseShared()中被调用时，只是用来唤醒其下一个结点
     */
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         */
        for(; ; ) {
            Node h = head;
            
            // 如果队列中已经没有排队者，则到下面直接退出
            if(h != null && h != tail) {
                int ws = h.waitStatus;
                
                // 需要唤醒后续结点
                if(ws == Node.SIGNAL) {
                    if(!h.compareAndSetWaitStatus(Node.SIGNAL, 0)){
                        continue;            // loop to recheck cases
                    }
                    
                    // 唤醒node后面陷入阻塞的“后继”
                    unparkSuccessor(h);
                } else {
                    // 尝试设置Node.PROPAGATE标记
                    if(ws == 0) {
                        if(!h.compareAndSetWaitStatus(0, Node.PROPAGATE)){
                            continue;                // loop on failed CAS
                        }
                    }
                }
            }
            
            // loop if head changed
            if(h == head){
                break;
            }
        }
    }
    
    /* 释放 ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  */
    
    /*▲ 共享锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 许可证 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     *
     * @return current state value
     */
    // 获取当前许可证数量
    protected final int getState() {
        return state;
    }
    
    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     *
     * @param newState the new state value
     */
    // 更新许可证数量
    protected final void setState(int newState) {
        state = newState;
    }
    
    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     *
     * @param expect the expected value
     * @param update the new value
     *
     * @return {@code true} if successful. False return indicates that the actual
     * value was not equal to the expected value.
     */
    // 原子地更新许可证数量为update，返回true代表更新成功
    protected final boolean compareAndSetState(int expect, int update) {
        return STATE.compareAndSet(this, expect, update);
    }
    
    /*▲ 许可证 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 同步队列 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    // 判断【|同步队列|】中是否存在排队的结点（线程）
    public final boolean hasQueuedThreads() {
        for(Node p = tail, h = head; p != h && p != null; p = p.prev) {
            if(p.waitStatus<=0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Queries whether any threads have ever contended to acquire this synchronizer;
     * that is, if an acquire method has ever blocked.
     *
     * In this implementation, this operation returns in constant time.
     *
     * @return {@code true} if there has ever been contention
     */
    // 判断是否存在线程争用
    public final boolean hasContended() {
        return head != null;
    }
    
    /**
     * Returns true if the given thread is currently queued.
     *
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     *
     * @param thread the thread
     *
     * @return {@code true} if the given thread is on the queue
     *
     * @throws NullPointerException if the thread is null
     */
    // 判断指定的线程是否在【|同步队列|】中排队
    public final boolean isQueued(Thread thread) {
        if(thread == null) {
            throw new NullPointerException();
        }
        
        for(Node p = tail; p != null; p = p.prev) {
            if(p.thread == thread) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns {@code true} if the apparent first queued thread, if one exists, is waiting in exclusive mode.
     * If this method returns {@code true}, and the current thread is attempting to acquire in shared mode
     * (that is, this method is invoked from {@link #tryAcquireShared})
     * then it is guaranteed that the current thread is not the first queued thread.
     * Used only as a heuristic in ReentrantReadWriteLock.
     */
    // 判断【|同步队列|】中首个排队者是否为[独占模式Node]
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null && (s = h.next) != null && !s.isShared() && s.thread != null;
    }
    
    /**
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     * <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread()
     *   && hasQueuedThreads()}</pre>
     *
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other thread will acquire before the current
     * thread.  Likewise, it is possible for another thread to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer.html#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     *
     * <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     * current thread, and {@code false} if the current thread
     * is at the head of the queue or the queue is empty
     *
     * @since 1.7
     */
    // 判断【|同步队列|】的队头是否还有其他（非当前线程）的线程在排队，返回true代表有，反之则没有
    public final boolean hasQueuedPredecessors() {
        Node h, s;
        if((h = head) != null) {
            if((s = h.next) == null || s.waitStatus>0) {
                s = null; // traverse in case of concurrent cancellation
                for(Node p = tail; p != h && p != null; p = p.prev) {
                    if(p.waitStatus<=0) {
                        s = p;
                    }
                }
            }
            return s != null && s.thread != Thread.currentThread();
        }
        return false;
    }
    
    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     *
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *
     * @return the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued
     */
    // 返回【|同步队列|】中首个排队的线程（可能为空）
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }
    
    /**
     * Version of getFirstQueuedThread called when fastpath fails.
     */
    // 返回【|同步队列|】中首个排队的线程（存在排队线程的情形下才使用该方法）
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next.
         * Try to get its thread field, ensuring consistent reads:
         * If thread field is nulled out or s.prev is no longer head,
         * then some other thread(s) concurrently performed setHead in between some of our reads.
         * We try this twice before  resorting to traversal.
         */
        Node h, s;
        Thread st;
        if(((h = head) != null && (s = h.next) != null && s.prev == head && (st = s.thread) != null)
            || ((h = head) != null && (s = h.next) != null && s.prev == head && (st = s.thread) != null)) {
            return st;
        }
        
        /*
         * Head's next field might not have been set yet, or may have been unset after setHead.
         * So we must check to see if tail is actually first node.
         * If not, we continue on, safely traversing from tail back to head to find first, guaranteeing termination.
         */
        
        /*
         * 由于排队结点可能被取消，故会造成序列“紊乱”
         * 如果上面的常规方式没有找到首个结点，则需要从队尾开始向前搜索
         */
        
        Thread firstThread = null;
        for(Node p = tail; p != null && p != head; p = p.prev) {
            Thread t = p.thread;
            if(t != null) {
                firstThread = t;
            }
        }
        return firstThread;
    }
    
    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization control.
     *
     * @return the estimated number of threads waiting to acquire
     */
    // 获取【|同步队列|】中排队的结点数量
    public final int getQueueLength() {
        int n = 0;
        for(Node p = tail; p != null; p = p.prev) {
            if(p.thread != null) {
                ++n;
            }
        }
        return n;
    }
    
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    // 返回一个集合，包含了所有正在【|同步队列|】中排队的Node中的线程
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for(Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if(t != null) {
                list.add(t);
            }
        }
        return list;
    }
    
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in exclusive mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to an exclusive acquire.
     *
     * @return the collection of threads
     */
    // 返回一个集合，包含了所有正在【|同步队列|】中排队的[独占模式Node]中的线程
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for(Node p = tail; p != null; p = p.prev) {
            if(!p.isShared()) {
                Thread t = p.thread;
                if(t != null) {
                    list.add(t);
                }
            }
        }
        return list;
    }
    
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in shared mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to a shared acquire.
     *
     * @return the collection of threads
     */
    // 返回一个集合，包含了所有正在【|同步队列|】中排队的[共享模式Node]中的线程
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for(Node p = tail; p != null; p = p.prev) {
            if(p.isShared()) {
                Thread t = p.thread;
                if(t != null) {
                    list.add(t);
                }
            }
        }
        return list;
    }
    
    /*▲ 同步队列 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ ConditionObject ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Queries whether the given ConditionObject uses this synchronizer as its lock.
     *
     * @param condition the condition
     *
     * @return {@code true} if owned
     *
     * @throws NullPointerException if the condition is null
     */
    // 判断给定的条件对象是否归当前的同步队列（锁）所有
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }
    
    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     *
     * @return {@code true} if there are any waiting threads
     *
     * @throws IllegalMonitorStateException if exclusive synchronization
     *                                      is not held
     * @throws IllegalArgumentException     if the given condition is
     *                                      not associated with this synchronizer
     * @throws NullPointerException         if the condition is null
     */
    // 判断当前【|条件队列|】中是否存在等待者
    public final boolean hasWaiters(ConditionObject condition) {
        if(!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.hasWaiters();
    }
    
    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this synchronizer. Note that
     * because timeouts and interrupts may occur at any time, the
     * estimate serves only as an upper bound on the actual number of
     * waiters.  This method is designed for use in monitoring system
     * state, not for synchronization control.
     *
     * @param condition the condition
     *
     * @return the estimated number of waiting threads
     *
     * @throws IllegalMonitorStateException if exclusive synchronization
     *                                      is not held
     * @throws IllegalArgumentException     if the given condition is
     *                                      not associated with this synchronizer
     * @throws NullPointerException         if the condition is null
     */
    // 返回condition中【|条件队列|】长度
    public final int getWaitQueueLength(ConditionObject condition) {
        if(!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.getWaitQueueLength();
    }
    
    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this
     * synchronizer.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate. The elements of the
     * returned collection are in no particular order.
     *
     * @param condition the condition
     *
     * @return the collection of threads
     *
     * @throws IllegalMonitorStateException if exclusive synchronization
     *                                      is not held
     * @throws IllegalArgumentException     if the given condition is
     *                                      not associated with this synchronizer
     * @throws NullPointerException         if the condition is null
     */
    // 返回一个集合，该集合包含了condition的【|条件队列|】中所有结点内缓存的线程引用
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if(!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.getWaitingThreads();
    }
    
    /**
     * Returns true if a node, always one that was initially placed on a condition queue,
     * is now waiting to reacquire on sync queue.
     *
     * @param node the node
     *
     * @return true if is reacquiring
     */
    // 如果结点（最初放置在条件队列中）现在正位于同步队列中等待重新获取锁，则返回true
    final boolean isOnSyncQueue(Node node) {
        if(node.waitStatus == Node.CONDITION || node.prev == null) {
            // 位于条件队列中
            return false;
        }
        
        // If has successor, it must be on queue
        if(node.next != null) {
            // 位于同步队列中
            return true;
        }
        
        /*
         * node.prev can be non-null, but not yet on queue because the CAS to place it on queue can fail.
         * So we have to traverse from tail to make sure it actually made it.
         * It will always be near the tail in calls to this method,
         * and unless the CAS failed (which is unlikely), it will be there,
         * so we hardly ever traverse much.
         */
        return findNodeFromTail(node);
    }
    
    /**
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     *
     * @param node the node
     *
     * @return true if successfully transferred (else the node was cancelled before signal)
     */
    // 尝试将node状态码更新为0，并追加到【|同步队列|】，并为其前驱设置Node.SIGNAL标记（很重要的一步）
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         */
        // 首先尝试将node的状态码更新为0
        if(!node.compareAndSetWaitStatus(Node.CONDITION, 0)) {
            // 如果更新失败，返回false
            return false;
        }
        
        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         */
        // 使用尾插法将node添加到【|同步队列|】，并返回旧的队尾
        Node oldTail = enq(node);
        int ws = oldTail.waitStatus;
        if(ws>0 || !oldTail.compareAndSetWaitStatus(ws, Node.SIGNAL)) {
            LockSupport.unpark(node.thread);
        }
        
        return true;
    }
    
    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     *
     * @param node the node
     *
     * @return true if cancelled before the node was signalled
     */
    // 尝试将【|条件队列|】中的node状态码更新为0，并追加到【|同步队列|】
    final boolean transferAfterCancelledWait(Node node) {
        // 更新状态码为0
        if(node.compareAndSetWaitStatus(Node.CONDITION, 0)) {
            // 使用尾插法将node添加到【|同步队列|】
            enq(node);
            return true;
        }
        
        /*
         * If we lost out to a signal(), then we can't proceed until it finishes its enq().
         * Cancelling during an incomplete transfer is both rare and transient, so just spin.
         */
        // 判断node是否正位于同步队列中等待重新获取锁
        while(!isOnSyncQueue(node)) {
            // 当前线程让出CPU时间片，大家重新抢占执行权
            Thread.yield();
        }
        
        return false;
    }
    
    /**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     *
     * @param node the condition node for this wait
     *
     * @return previous sync state
     */
    // 针对独占锁，丢弃node所在线程持有的所有许可证，并唤醒【|同步队列|】后续的阻塞线程
    final int fullyRelease(Node node) {
        try {
            // 获取当前线程持有的所有许可证
            int savedState = getState();
            
            // 尝试完全释放锁，并唤醒后续的阻塞线程。返回值表示本次操作后锁是否自由
            if(release(savedState)) {
                // 返回释放前的许可证数量
                return savedState;
            }
            
            throw new IllegalMonitorStateException();
        } catch(Throwable t) {
            node.waitStatus = Node.CANCELLED;
            throw t;
        }
    }
    
    /**
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     *
     * @return true if present
     */
    // 从【|同步队列|】队尾出发，向前查找node
    private boolean findNodeFromTail(Node node) {
        // We check for node first, since it's likely to be at or near tail.
        // tail is known to be non-null, so we could re-order to "save"
        // one null check, but we leave it this way to help the VM.
        for(Node p = tail; ; ) {
            if(p == node) {
                return true;
            }
            
            if(p == null) {
                return false;
            }
            
            p = p.prev;
        }
    }
    
    /*▲ ConditionObject ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ 对锁的支持 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a {@link ConditionObject} method.
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link ConditionObject} methods, so need
     * not be defined if conditions are not used.
     *
     * @return {@code true} if synchronization is held exclusively;
     * {@code false} otherwise
     *
     * @throws UnsupportedOperationException if conditions are not supported
     */
    // 判断当前线程是否为锁的占用者，由子类实现
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Wakes up node's successor, if one exists.
     *
     * @param node the node
     */
    /*
     * 唤醒node后面陷入阻塞的“后继”
     *
     * 注：
     * 由于有些结点可能会被标记为Node.CANCELLED（取消），
     * 所以这里的后继可能不是node.next，需要进一步搜索后才能确定
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try to clear in anticipation of signalling.
         * It is OK if this fails or if status is changed by waiting thread.
         */
        int ws = node.waitStatus;
        if(ws<0) {
            // 如果node状态码为负，则将其重置为0
            node.compareAndSetWaitStatus(ws, 0);
        }
        
        /*
         * Thread to unpark is held in successor, which is normally just the next node.
         * But if cancelled or apparently null, traverse backwards from tail to find the actual non-cancelled successor.
         */
        Node s = node.next;
        /*
         * 如果s==null，说明node已经是尾结点，后面没有需要唤醒的结点了
         *
         * 如果s!=null，且s.waitStatus>0，说明node被标记为Node.CANCELLED（取消）
         * 此时，需要从尾端向前遍历，找到离s最近的正处于阻塞的后继，以便后续唤醒它
         */
        if(s == null || s.waitStatus>0) {
            s = null;
            for(Node p = tail; p != node && p != null; p = p.prev) {
                if(p.waitStatus<=0) {
                    s = p;
                }
            }
        }
        
        // 唤醒node的后继
        if(s != null) {
            LockSupport.unpark(s.thread);
        }
    }
    
    /**
     * Inserts node into queue, initializing if necessary. See picture above.
     *
     * @param node the node to insert
     *
     * @return node's predecessor
     */
    // 使用尾插法将node添加到【|同步队列|】，并返回旧的队尾
    private Node enq(Node node) {
        for(; ; ) {
            Node oldTail = tail;
            if(oldTail != null) {
                // 设置node的前驱为oldTail
                node.setPrevRelaxed(oldTail);
                
                // 更新队尾游标指向node
                if(compareAndSetTail(oldTail, node)) {
                    // 链接旧的队尾与node，形成一个双向链表
                    oldTail.next = node;
                    return oldTail;
                }
            } else {
                // 【|同步队列|】不存在时，需要初始化一个头结点
                initializeSyncQueue();
            }
        }
    }
    
    /**
     * Initializes head and tail fields on first contention.
     */
    // 【|同步队列|】不存在时，需要初始化一个头结点
    private final void initializeSyncQueue() {
        Node h;
        if(HEAD.compareAndSet(this, null, (h = new Node()))) {
            tail = h;
        }
    }
    
    /**
     * CASes tail field.
     */
    // 更新队尾游标指向update
    private final boolean compareAndSetTail(Node expect, Node update) {
        return TAIL.compareAndSet(this, expect, update);
    }
    
    /**
     * Creates and enqueues node for current thread and given mode.
     *
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
     *
     * @return the new node
     */
    /*
     * 向【|同步队列|】添加一个排队者(线程)
     *
     * mode有两种可能：
     * 1.独占模式：Node.EXCLUSIVE
     * 2.共享模式：Node.SHARED
     * 由此，可创建[独占模式Node]或[共享模式Node]
     * 创建的[模式Node]会记下当前线程的引用，并进入同步队列进行排队
     */
    private Node addWaiter(Node mode) {
        // 创建一个独占/共享模式的node，该node存储了当前线程的引用
        Node node = new Node(mode);
        
        // 使用尾插法将node添加到【|同步队列|】
        enq(node);
        
        // 返回刚加入【|同步队列|】的[模式Node]
        return node;
    }
    
    /**
     * Sets head of queue to be node, thus dequeuing. Called only by
     * acquire methods.  Also nulls out unused fields for sake of GC
     * and to suppress unnecessary signals and traversals.
     *
     * @param node the node
     */
    // 设置node为头结点（即丢掉了原来的头结点）
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;   // 切断与前一个结点的联系，以便GC
    }
    
    /**
     * Sets head of queue, and checks if successor may be waiting
     * in shared mode, if so propagating if either propagate > 0 or
     * PROPAGATE status was set.
     *
     * @param node      the node
     * @param propagate the return value from a tryAcquireShared
     */
    // 更新头结点为node，并为其设置Node.PROPAGATE标记，或唤醒其后续结点
    private void setHeadAndPropagate(Node node, int propagate) {
        // 记下旧的头结点
        Node h = head; // Record old head for check below
        
        // 设置node为头结点（即丢掉了原来的头结点）
        setHead(node);
        
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode, or we don't know, because it appears null
         *
         * The conservatism in both of these checks may cause unnecessary wake-ups,
         * but only when there are multiple racing acquires/releases,
         * so most need signals now or soon anyway.
         */
        if(propagate>0 || h == null || h.waitStatus<0 || (h = head) == null || h.waitStatus<0) {
            Node s = node.next;
            if(s == null || s.isShared()) {
                // 为共享结点设置Node.PROPAGATE标记，或唤醒其下一个结点
                doReleaseShared();
            }
        }
    }
    
    /**
     * Convenience method to interrupt current thread.
     */
    // 为线程设置中断标记
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }
    
    /**
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     *
     * @param pred node's predecessor holding status
     * @param node the node
     *
     * @return {@code true} if thread should block
     */
    /*
     * 抢锁失败时，尝试为node的前驱设置阻塞标记
     *
     * 每个结点的阻塞标记设置在其前驱上，原因是：
     * 每个正在活动的结点都将成为头结点，当活动的结点（头结点）执行完之后，
     * 需要根据自身上面的阻塞标记，以确定要不要唤醒后续的结点
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        // node前驱的状态
        int ws = pred.waitStatus;
        
        // 如果node前驱的状态为Node.SIGNAL，则表示node需要进入阻塞状态
        if(ws == Node.SIGNAL){
            /* This node has already set status asking a release to signal it, so it can safely park. */
            return true;
        }
        
        // 如果node前驱的状态被标记为取消，则顺着其前驱向前遍历，将紧邻的待取消结点连成一片
        if(ws>0) {
            /* Predecessor was cancelled. Skip over predecessors and indicate retry. */
            do {
                node.prev = pred = pred.prev;
            } while(pred.waitStatus>0);
            
            pred.next = node;
        } else {
            /*
             * waitStatus must be 0 or PROPAGATE.
             * Indicate that we need a signal, but don't park yet.
             * Caller will need to retry to make sure it cannot acquire before parking.
             */
            // 更新node前驱的状态为Node.SIGNAL，即使node陷入阻塞
            pred.compareAndSetWaitStatus(ws, Node.SIGNAL);
        }
        
        return false;
    }
    
    /**
     * Convenience method to park and then check if interrupted.
     *
     * @return {@code true} if interrupted
     */
    // 设置线程进入阻塞状态，并清除线程的中断状态。返回值代表之前线程是否处于阻塞状态
    private final boolean parkAndCheckInterrupt() {
        // 设置线程阻塞（对标记为中断的线程无效）
        LockSupport.park(this);
        return Thread.interrupted();
    }
    
    /**
     * Cancels an ongoing attempt to acquire.
     *
     * @param node the node
     */
    // 标记node结点为Node.CANCELLED（取消）状态
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if(node == null) {
            return;
        }
        
        // 删除对线程的引用
        node.thread = null;
        
        // Skip cancelled predecessors
        Node pred = node.prev;
        // 顺着node的前驱向前遍历，将标记为取消的node结点连成一片
        while(pred.waitStatus>0) {
            node.prev = pred = pred.prev;
        }
        
        // predNext is the apparent node to unsplice.
        // CASes below will fail if not, in which case, we lost race vs another cancel or signal,
        // so no further action is necessary, although with a possibility that a cancelled node may transiently remain reachable.
        Node predNext = pred.next;
        
        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        // 标记node线程进入取消状态
        node.waitStatus = Node.CANCELLED;
        
        // If we are the tail, remove ourselves.
        if(node == tail && compareAndSetTail(node, pred)) {
            pred.compareAndSetNext(predNext, null);
        } else {
            // If successor needs signal, try to set pred's next-link so it will get one.
            // Otherwise wake it up to propagate.
            int ws;
            
            if(pred != head
                && ((ws = pred.waitStatus) == Node.SIGNAL || (ws<=0 && pred.compareAndSetWaitStatus(ws, Node.SIGNAL)))
                && pred.thread != null) {
                Node next = node.next;
                // 将处于阻塞状态的node连成一片，通过后继向后遍历即可获得
                if(next != null && next.waitStatus<=0) {
                    pred.compareAndSetNext(predNext, next);
                }
            } else {
                // 唤醒node后面陷入阻塞的“后继”
                unparkSuccessor(node);
            }
            
            node.next = node; // node后继指向自身，目的是为了便于GC
        }
    }
    
    /*▲ 对锁的支持 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     *
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        return super.toString() + "[State = " + getState() + ", " + (hasQueuedThreads() ? "non" : "") + "empty queue]";
    }
    
    
    
    /**
     * Wait queue node class.
     *
     * <p>The wait queue is a variant of a "CLH" (Craig, Landin, and
     * Hagersten) lock queue. CLH locks are normally used for
     * spinlocks.  We instead use them for blocking synchronizers, but
     * use the same basic tactic of holding some of the control
     * information about a thread in the predecessor of its node.  A
     * "status" field in each node keeps track of whether a thread
     * should block.  A node is signalled when its predecessor
     * releases.  Each node of the queue otherwise serves as a
     * specific-notification-style monitor holding a single waiting
     * thread. The status field does NOT control whether threads are
     * granted locks etc though.  A thread may try to acquire if it is
     * first in the queue. But being first does not guarantee success;
     * it only gives the right to contend.  So the currently released
     * contender thread may need to rewait.
     *
     * <p>To enqueue into a CLH lock, you atomically splice it in as new
     * tail. To dequeue, you just set the head field.
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>Insertion into a CLH queue requires only a single atomic
     * operation on "tail", so there is a simple atomic point of
     * demarcation from unqueued to queued. Similarly, dequeuing
     * involves only updating the "head". However, it takes a bit
     * more work for nodes to determine who their successors are,
     * in part to deal with possible cancellation due to timeouts
     * and interrupts.
     *
     * <p>The "prev" links (not used in original CLH locks), are mainly
     * needed to handle cancellation. If a node is cancelled, its
     * successor is (normally) relinked to a non-cancelled
     * predecessor. For explanation of similar mechanics in the case
     * of spin locks, see the papers by Scott and Scherer at
     * http://www.cs.rochester.edu/u/scott/synchronization/
     *
     * <p>We also use "next" links to implement blocking mechanics.
     * The thread id for each node is kept in its own node, so a
     * predecessor signals the next node to wake up by traversing
     * next link to determine which thread it is.  Determination of
     * successor must avoid races with newly queued nodes to set
     * the "next" fields of their predecessors.  This is solved
     * when necessary by checking backwards from the atomically
     * updated "tail" when a node's successor appears to be null.
     * (Or, said differently, the next-links are an optimization
     * so that we don't usually need a backward scan.)
     *
     * <p>Cancellation introduces some conservatism to the basic
     * algorithms.  Since we must poll for cancellation of other
     * nodes, we can miss noticing whether a cancelled node is
     * ahead or behind us. This is dealt with by always unparking
     * successors upon cancellation, allowing them to stabilize on
     * a new predecessor, unless we can identify an uncancelled
     * predecessor who will carry this responsibility.
     *
     * <p>CLH queues need a dummy header node to get started. But
     * we don't create them on construction, because it would be wasted
     * effort if there is never contention. Instead, the node
     * is constructed and head and tail pointers are set upon first
     * contention.
     *
     * <p>Threads waiting on Conditions use the same nodes, but
     * use an additional link. Conditions only need to link nodes
     * in simple (non-concurrent) linked queues because they are
     * only accessed when exclusively held.  Upon await, a node is
     * inserted into a condition queue.  Upon signal, the node is
     * transferred to the main queue.  A special value of status
     * field is used to mark which queue a node is on.
     *
     * <p>Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill
     * Scherer and Michael Scott, along with members of JSR-166
     * expert group, for helpful ideas, discussions, and critiques
     * on the design of this class.
     */
    // 【|同步队列|】或【|条件队列|】的结点
    static final class Node {
        /** Marker to indicate a node is waiting in shared mode */
        static final Node SHARED = new Node();  // 【共享】模式
        /** Marker to indicate a node is waiting in exclusive mode */
        static final Node EXCLUSIVE = null;     // 【独占】模式
        
        /** waitStatus value to indicate thread has cancelled. */
        /*
         * 该标记指示结点应当被取消，不再参与排队
         * 如果在线程阻塞期间发生异常的话，会为其所在的结点设置此标记
         */
        static final int CANCELLED = 1;
        /** waitStatus value to indicate successor's thread needs unparking. */
        /*
         * 该标记指示结点在等待一个信号，即说明此结点正处于阻塞状态，需要被唤醒
         * 如果一个结点处于阻塞，那么会在它的【前驱】上设置Node.SIGNAL标记
         * 每个处于阻塞但即将执行的结点（不考虑插队），它一定位于头结点之后，
         * 且该头结点上必定有Node.SIGNAL标记
         * 换句话说，当前结点或线程（有些线程不排队，所以不在结点中）执行完成后，
         * 只需要检查头结点的Node.SIGNAL标记就可以顺手唤醒下个结点
         */
        static final int SIGNAL = -1;
        
        /** waitStatus value to indicate thread is waiting on condition. */
        /*
         * 仅用于【|条件队列|】的状态码
         *
         * 该标记表示此结点正位于【|条件队列|】中
         * 且该结点包含的线程被暂时阻塞，稍后被唤醒后追加到【|同步队列|】中继续（等待）执行
         */
        static final int CONDITION = -2;
        
        /** waitStatus value to indicate the next acquireShared should unconditionally propagate. */
        /*
         * 该标记指示结点已经位于同步队列之外
         * 当一个共享结点没有经过阻塞就直接获取锁时，
         * 会将头结点更新为该结点，并且为该结点设置PROPAGATE标记，
         * 接下来，如果其后续结点也不需要经过阻塞，那么该结点保持PROPAGATE标记，
         * 反之，如果其后续结点需要经过阻塞，那么该标记会被修改为SIGNAL
         */
        static final int PROPAGATE = -3;
        
        /**
         * Status field, taking on only the values:
         * SIGNAL:     The successor of this node is (or will soon be)
         * blocked (via park), so the current node must
         * unpark its successor when it releases or
         * cancels. To avoid races, acquire methods must
         * first indicate they need a signal,
         * then retry the atomic acquire, and then,
         * on failure, block.
         * CANCELLED:  This node is cancelled due to timeout or interrupt.
         * Nodes never leave this state. In particular,
         * a thread with cancelled node never again blocks.
         * CONDITION:  This node is currently on a condition queue.
         * It will not be used as a sync queue node
         * until transferred, at which time the status
         * will be set to 0. (Use of this value here has
         * nothing to do with the other uses of the
         * field, but simplifies mechanics.)
         * PROPAGATE:  A releaseShared should be propagated to other
         * nodes. This is set (for head node only) in
         * doReleaseShared to ensure propagation
         * continues, even if other operations have
         * since intervened.
         * 0:          None of the above
         *
         * The values are arranged numerically to simplify use.
         * Non-negative values mean that a node doesn't need to
         * signal. So, most code doesn't need to check for particular
         * values, just for sign.
         *
         * The field is initialized to 0 for normal sync nodes, and
         * CONDITION for condition nodes.  It is modified using CAS
         * (or when possible, unconditional volatile writes).
         */
        // 状态码，有CANCELLED/SIGNAL/CONDITION/PROPAGATE四种取值，默认为0
        volatile int waitStatus;
        
        /**
         * Link to predecessor node that current node/thread relies on
         * for checking waitStatus. Assigned during enqueuing, and nulled
         * out (for sake of GC) only upon dequeuing.  Also, upon
         * cancellation of a predecessor, we short-circuit while
         * finding a non-cancelled one, which will always exist
         * because the head node is never cancelled: A node becomes
         * head only as a result of successful acquire. A
         * cancelled thread never succeeds in acquiring, and a thread only
         * cancels itself, not any other node.
         */
        // 用于【|同步队列|】，表示排队结点的前驱，顺着前驱遍历可以跳过被取消的node线程
        volatile Node prev;
        
        /**
         * Link to the successor node that the current node/thread
         * unparks upon release. Assigned during enqueuing, adjusted
         * when bypassing cancelled predecessors, and nulled out (for
         * sake of GC) when dequeued.  The enq operation does not
         * assign next field of a predecessor until after attachment,
         * so seeing a null next field does not necessarily mean that
         * node is at end of queue. However, if a next field appears
         * to be null, we can scan prev's from the tail to
         * double-check.  The next field of cancelled nodes is set to
         * point to the node itself instead of null, to make life
         * easier for isOnSyncQueue.
         */
        // 用于【|同步队列|】，表示排队结点的后继，顺着后继遍历可以找到陷入阻塞的node线程
        volatile Node next;
        
        /**
         * The thread that enqueued this node.  Initialized on
         * construction and nulled out after use.
         */
        // node内存储的线程引用，表面上是node在排队，实际上是thread在排队
        volatile Thread thread;
        
        /**
         * Link to next node waiting on condition, or the special
         * value SHARED.  Because condition queues are accessed only
         * when holding in exclusive mode, we just need a simple
         * linked queue to hold nodes while they are waiting on
         * conditions. They are then transferred to the queue to
         * re-acquire. And because conditions can only be exclusive,
         * we save a field by using special value to indicate shared
         * mode.
         */
        /*
         * 用于【|同步队列|】时，该引用仅作为标记使用
         * 它表示参与排队的node是[独占模式node]，或者是[共享模式node]
         *
         * 用于【|条件队列|】时，该引用表示参与排队的下一个结点
         */
        Node nextWaiter;
        
        // VarHandle mechanics
        private static final VarHandle WAITSTATUS;  // 保存字段 waitStatus 的内存地址
        private static final VarHandle PREV;        // 保存字段 prev       的内存地址
        private static final VarHandle NEXT;        // 保存字段 next       的内存地址
        private static final VarHandle THREAD;      // 保存字段 thread     的内存地址
        
        // 获取字段地址
        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                WAITSTATUS = l.findVarHandle(Node.class, "waitStatus", int.class);
                PREV       = l.findVarHandle(Node.class, "prev", Node.class);
                NEXT       = l.findVarHandle(Node.class, "next", Node.class);
                THREAD     = l.findVarHandle(Node.class, "thread", Thread.class);
            } catch(ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        
        /** Establishes initial head or SHARED marker. */
        // 创建一个空的Node，用作头结点或共享标记
        Node() {
        }
        
        /** Constructor used by addWaiter. */
        // 创建一个独占/共享模式的node
        Node(Node nextWaiter) {
            this.nextWaiter = nextWaiter;   // 记录当前node的模式
            THREAD.set(this, Thread.currentThread());   // 继续当前线程
        }
        
        /** Constructor used by addConditionWaiter. */
        // 创建一个状态为waitStatus的node
        Node(int waitStatus) {
            WAITSTATUS.set(this, waitStatus);
            THREAD.set(this, Thread.currentThread());
        }
        
        /**
         * Returns true if node is waiting in shared mode.
         */
        // 是否为共享结点
        final boolean isShared() {
            return nextWaiter == SHARED;
        }
        
        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null.  The null check could
         * be elided, but is present to help the VM.
         *
         * @return the predecessor of this node
         */
        // 获取当前结点的前驱
        final Node predecessor() {
            Node p = prev;
            if(p == null) {
                throw new NullPointerException();
            } else {
                return p;
            }
        }
        
        /** CASes waitStatus field. */
        // 原子地更新当前结点的waitStatus为update
        final boolean compareAndSetWaitStatus(int expect, int update) {
            return WAITSTATUS.compareAndSet(this, expect, update);
        }
        
        /** CASes next field. */
        // 原子地更新当前结点的next为update
        final boolean compareAndSetNext(Node expect, Node update) {
            return NEXT.compareAndSet(this, expect, update);
        }
        
        // 设置当前结点的prev为p
        final void setPrevRelaxed(Node p) {
            PREV.set(this, p);
        }
    }
    
    /**
     * Condition implementation for a {@link AbstractQueuedSynchronizer}
     * serving as the basis of a {@link Lock} implementation.
     *
     * <p>Method documentation for this class describes mechanics,
     * not behavioral specifications from the point of view of Lock
     * and Condition users. Exported versions of this class will in
     * general need to be accompanied by documentation describing
     * condition semantics that rely on those of the associated
     * {@code AbstractQueuedSynchronizer}.
     *
     * <p>This class is Serializable, but all fields are transient,
     * so deserialized conditions have no waiters.
     */
    // {同步条件}对象，用于更精细地指导线程的同步行为
    public class ConditionObject implements Condition, Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        
        /** Mode meaning to reinterrupt on exit from wait */
        // 刚刚唤醒的线程带有中断标记，且该线程node仍在【|条件队列|】，此时需要为线程恢复中断标记
        private static final int REINTERRUPT = 1;
        
        /** Mode meaning to throw InterruptedException on exit from wait */
        // 刚刚唤醒的线程带有中断标记，且该线程node已进入了【|同步队列|】，此时需要抛出异常
        private static final int THROW_IE = -1;
        
        // 【|条件队列|】以单链表形式组织，firstWaiter和lastWaiter是首尾结点，不存在头结点
        private transient Node firstWaiter, lastWaiter;
        
        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() {
        }
        
        /*
         * For interruptible waits, we need to track whether to throw InterruptedException,
         * if interrupted while blocked on condition, versus reinterrupt current thread,
         * if interrupted while blocked waiting to re-acquire.
         */
        
        
        /*▼ 暂时阻塞 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Implements uninterruptible condition wait.
         * <ol>
         * <li>Save lock state returned by {@link #getState}.
         * <li>Invoke {@link #release} with saved state as argument,
         * throwing IllegalMonitorStateException if it fails.
         * <li>Block until signalled.
         * <li>Reacquire by invoking specialized version of
         * {@link #acquire} with saved state as argument.
         * </ol>
         */
        /*
         * 使当前活跃的线程暂时陷入阻塞，进入【|条件队列|】排队，并唤醒【|同步队列|】中的等待者...
         * 直到收到signal()命令，该线程才恢复运行
         *
         * 允许阻塞带有中断标记的线程
         */
        public final void awaitUninterruptibly() {
            // 添加一个新的[条件Node](缓存了当前线程的引用)到【|条件队列|】，并返回刚添加的node
            Node node = addConditionWaiter();
            
            /*
             * 针对独占锁，丢弃node所在线程持有的所有许可证（数量由savedState保存），并唤醒【|同步队列|】后续的阻塞线程
             * 这样一来，同步队列中排在首位的线程又可以开始抢锁了
             */
            int savedState = fullyRelease(node);
            
            boolean interrupted = false;
            
            // 判断node是否正位于【|同步队列|】中等待重新获取锁
            while(!isOnSyncQueue(node)) {
                // 如果结点位于【|条件队列|】，则暂停运行，陷入阻塞
                LockSupport.park(this);
                
                /* 从这里被signal()唤醒后，node已成为【|同步队列|】的首个结点（不是头结点），并准备去抢锁 */
                
                // 唤醒阻塞线程后，首先检查（并清除）其当前是否有中断标记，如果有的话用interrupted记下来
                if(Thread.interrupted()) {
                    interrupted = true;
                }
            }
            
            /*
             * 之前被阻塞的线程现在已经可以去争锁了
             * 而且，争锁时会携带之前保存的许可证数量
             * 争锁成功后，该结点会成为【|同步队列|】的头结点，并恢复运行
             *
             * 在争锁成功时需要为当前线程设置中断标记
             */
            if(acquireQueued(node, savedState) || interrupted) {
                // 设置当前线程的中断状态
                selfInterrupt();
            }
        }
        
        /**
         * Implements interruptible condition wait.
         * <ol>
         * <li>If current thread is interrupted, throw InterruptedException.
         * <li>Save lock state returned by {@link #getState}.
         * <li>Invoke {@link #release} with saved state as argument,
         * throwing IllegalMonitorStateException if it fails.
         * <li>Block until signalled or interrupted.
         * <li>Reacquire by invoking specialized version of
         * {@link #acquire} with saved state as argument.
         * <li>If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        /*
         * 使当前活跃的线程暂时陷入阻塞，进入【|条件队列|】排队，并唤醒【|同步队列|】中的等待者...
         * 直到收到signal()命令，该线程才恢复运行
         *
         * 禁止阻塞带有中断标记的线程
         *
         * 操作步骤：
         * 1.将该线程的引用封装到{条件node}中，并进入【|条件队列|】排队
         * 2.释放掉该线程持有的全部许可证，即让锁重新处于可用状态
         * 3.唤醒【|同步队列|】中的等待者重新争锁
         * 4.将自身陷入阻塞，等待signal()唤醒
         * 5.被signal()唤醒后，将排队的{条件node}移入【|同步队列|】，
         *   并恢复其许可证，让其继续执行...
         */
        public final void await() throws InterruptedException {
            // 如果线程带有中断标记，则抛出异常
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            
            // 添加一个新的[条件Node](缓存了当前线程的引用)到【|条件队列|】，并返回刚添加的node
            Node node = addConditionWaiter();
            
            /*
             * 针对独占锁，丢弃node所在线程持有的所有许可证（数量由savedState保存），并唤醒【|同步队列|】后续的阻塞线程
             * 这样一来，同步队列中排在首位的线程又可以开始抢锁了
             */
            int savedState = fullyRelease(node);
            
            int interruptMode = 0;
            
            // 判断node是否正位于【|同步队列|】中等待重新获取锁
            while(!isOnSyncQueue(node)) {
                // 如果结点位于【|条件队列|】，则暂停运行，陷入阻塞
                LockSupport.park(this);
                
                /* 从这里被signal()唤醒后，node已成为【|同步队列|】的首个结点（不是头结点），并准备去抢锁 */
                
                // 唤醒阻塞线程后，首先检查其当前是否有中断标记，如果有的话直接跳出循环
                if((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
            }
            
            /*
             * 之前被阻塞的线程现在已经可以去争锁了
             * 而且，争锁时会携带之前保存的许可证数量
             * 争锁成功后，该结点会成为【|同步队列|】的头结点，并恢复运行
             */
            if(acquireQueued(node, savedState)) {
                // 如果不需要抛异常，则修改标记为REINTERRUPT，代表稍后要恢复线程的中断状态
                if(interruptMode != THROW_IE){
                    interruptMode = REINTERRUPT;
                }
            }
            
            // clean up if cancelled
            if(node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            
            // 有中断标记的情况下
            if(interruptMode != 0) {
                // 对刚刚唤醒的带有中断标记的线程进行特殊处理
                reportInterruptAfterWait(interruptMode);
            }
        }
        
        /**
         * Implements timed condition wait.
         * <ol>
         * <li>If current thread is interrupted, throw InterruptedException.
         * <li>Save lock state returned by {@link #getState}.
         * <li>Invoke {@link #release} with saved state as argument,
         * throwing IllegalMonitorStateException if it fails.
         * <li>Block until signalled, interrupted, or timed out.
         * <li>Reacquire by invoking specialized version of
         * {@link #acquire} with saved state as argument.
         * <li>If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        /*
         * 使当前活跃的线程暂时陷入阻塞，进入【|条件队列|】排队，并唤醒【|同步队列|】中的等待者...
         * 直到收到signal()的信号，或超时醒来，该线程才恢复运行
         *
         * 禁止阻塞带有中断标记的线程
         *
         * nanosTimeout是相对时间，代表最长阻塞时间，过了这个时间，即使没有收到signal()的信号，也会自己醒来
         */
        public final long awaitNanos(long nanosTimeout) throws InterruptedException {
            // 如果线程带有中断标记，则抛出异常
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            
            /* We don't check for nanosTimeout <= 0L here, to allow awaitNanos(0) as a way to "yield the lock" */
            
            // 计算结束时间
            final long deadline = System.nanoTime() + nanosTimeout;
            
            long initialNanos = nanosTimeout;
            
            // 添加一个新的[条件Node]到【|条件队列|】，并返回刚添加的node
            Node node = addConditionWaiter();
            
            /*
             * 针对独占锁，丢弃node所在线程持有的所有许可证（数量由savedState保存），并唤醒【|同步队列|】后续的阻塞线程
             * 这样一来，同步队列中排在首位的线程又可以开始抢锁了
             */
            int savedState = fullyRelease(node);
            
            int interruptMode = 0;
            
            // 判断node是否正位于【|同步队列|】中等待重新获取锁
            while(!isOnSyncQueue(node)) {
                // 已经超时
                if(nanosTimeout<=0L) {
                    // 立即将【|条件队列|】中的node状态码更新为0，并追加到【|同步队列|】
                    transferAfterCancelledWait(node);
                    
                    // 跳出循环
                    break;
                }
                
                // 如果时间过短，就不阻塞了，一直循环，直到超时
                if(nanosTimeout>SPIN_FOR_TIMEOUT_THRESHOLD) {
                    // 使线程阻塞nanosTimeout（单位：纳秒）时长后自动醒来（中途可被唤醒）
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                
                /*
                 * 从这里被signal()唤醒后，node已成为【|同步队列|】的首个结点（不是头结点），
                 * 此时while循环就进不来了，该线程可以去抢锁了
                 *
                 * 当然，这里也有可能是超时后自然醒来的
                 * 如果该线程是超时后自己醒来的，则会将自身追加到【|同步队列|】并跳出循环
                 */
                
                // 线程醒来后，首先检查其当前是否有中断标记，如果有的话直接跳出循环
                if((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
                
                // 判断是否超时（因为可能是半道被唤醒的）
                nanosTimeout = deadline - System.nanoTime();
            }
            
            /*
             * 之前被阻塞的线程现在已经可以去争锁了
             * 而且，争锁时会携带之前保存的许可证数量
             * 争锁成功后，该结点会成为【|同步队列|】的头结点，并恢复运行
             */
            if(acquireQueued(node, savedState)) {
                // 如果不需要抛异常，则修改标记为REINTERRUPT，代表稍后要恢复线程的中断状态
                if(interruptMode != THROW_IE){
                    interruptMode = REINTERRUPT;
                }
            }
            
            if(node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            
            // 有中断标记的情况下
            if(interruptMode != 0) {
                // 对刚刚醒来的带有中断标记的线程进行特殊处理
                reportInterruptAfterWait(interruptMode);
            }
            
            // 计算残留的时间，小心溢出（如果是自然醒来，此处为负，如果是中途被唤醒，此处可能为正）
            long remaining = deadline - System.nanoTime(); // avoid overflow
            
            return (remaining<=initialNanos) ? remaining : Long.MIN_VALUE;
        }
        
        /**
         * Implements timed condition wait.
         * <ol>
         * <li>If current thread is interrupted, throw InterruptedException.
         * <li>Save lock state returned by {@link #getState}.
         * <li>Invoke {@link #release} with saved state as argument,
         * throwing IllegalMonitorStateException if it fails.
         * <li>Block until signalled, interrupted, or timed out.
         * <li>Reacquire by invoking specialized version of
         * {@link #acquire} with saved state as argument.
         * <li>If interrupted while blocked in step 4, throw InterruptedException.
         * <li>If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        // 与awaitNanos(long)方法作用一致，只不过纳秒时间由time转换而来
        public final boolean await(long time, TimeUnit unit) throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            
            // We don't check for nanosTimeout <= 0L here, to allow await(0, unit) as a way to "yield the lock".
            final long deadline = System.nanoTime() + nanosTimeout;
            
            Node node = addConditionWaiter();
            
            int savedState = fullyRelease(node);
            
            boolean timedout = false;
            
            int interruptMode = 0;
            
            while(!isOnSyncQueue(node)) {
                if(nanosTimeout<=0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                
                if(nanosTimeout>SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                
                if((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
                
                nanosTimeout = deadline - System.nanoTime();
            }
            
            if(acquireQueued(node, savedState) && interruptMode != THROW_IE) {
                interruptMode = REINTERRUPT;
            }
            
            if(node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            
            if(interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            
            return !timedout;
        }
        
        /**
         * Implements absolute timed condition wait.
         * <ol>
         * <li>If current thread is interrupted, throw InterruptedException.
         * <li>Save lock state returned by {@link #getState}.
         * <li>Invoke {@link #release} with saved state as argument,
         * throwing IllegalMonitorStateException if it fails.
         * <li>Block until signalled, interrupted, or timed out.
         * <li>Reacquire by invoking specialized version of
         * {@link #acquire} with saved state as argument.
         * <li>If interrupted while blocked in step 4, throw InterruptedException.
         * <li>If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        /*
         * 使当前活跃的线程暂时陷入阻塞，进入【|条件队列|】排队，并唤醒【|同步队列|】中的等待者...
         * 直到收到signal()的信号，或超时醒来，该线程才恢复运行
         *
         * 禁止阻塞带有中断标记的线程
         *
         * deadline是绝对时间，代表阻塞截止时间，过了这个时间，即使没有收到signal()的信号，也会自己醒来
         */
        public final boolean awaitUntil(Date deadline) throws InterruptedException {
            // 获取代表该日期的Unix时间戳（毫秒）
            long abstime = deadline.getTime();
            
            // 如果线程带有中断标记，则抛出异常
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            
            // 添加一个新的[条件Node]到【|条件队列|】，并返回刚添加的node
            Node node = addConditionWaiter();
            
            /*
             * 针对独占锁，丢弃node所在线程持有的所有许可证（数量由savedState保存），并唤醒【|同步队列|】后续的阻塞线程
             * 这样一来，同步队列中排在首位的线程又可以开始抢锁了
             */
            int savedState = fullyRelease(node);
            
            boolean timedout = false;
            
            int interruptMode = 0;
            
            // 判断node是否正位于【|同步队列|】中等待重新获取锁
            while(!isOnSyncQueue(node)) {
                // 已经超时
                if(System.currentTimeMillis() >= abstime) {
                    // 立即将【|条件队列|】中的node状态码更新为0，并追加到【|同步队列|】
                    timedout = transferAfterCancelledWait(node);
                    
                    // 跳出循环
                    break;
                }
                
                // 使线程陷入阻塞，直到deadline（以毫秒为单位的Unix时间戳）时间点时才醒来（中途可被唤醒）
                LockSupport.parkUntil(this, abstime);
                
                // 线程醒来后，首先检查其当前是否有中断标记，如果有的话直接跳出循环
                if((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
            }
            
            /*
             * 之前被阻塞的线程现在已经可以去争锁了
             * 而且，争锁时会携带之前保存的许可证数量
             * 争锁成功后，该结点会成为【|同步队列|】的头结点，并恢复运行
             */
            if(acquireQueued(node, savedState)) {
                // 如果不需要抛异常，则修改标记为REINTERRUPT，代表稍后要恢复线程的中断状态
                if(interruptMode != THROW_IE){
                    interruptMode = REINTERRUPT;
                }
            }
            
            if(node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            
            // 有中断标记的情况下
            if(interruptMode != 0) {
                // 对刚刚醒来的带有中断标记的线程进行特殊处理
                reportInterruptAfterWait(interruptMode);
            }
            
            return !timedout;
        }
        
        /*▲ 暂时阻塞 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 准备唤醒 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *                                      returns {@code false}
         */
        // 将【|条件队列|】中的首结点追加到【|同步队列|】中，并让其处于待唤醒状态
        public final void signal() {
            // 如果当前线程不是锁的占用者，抛出异常
            if(!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            
            Node first = firstWaiter;
            if(first != null) {
                doSignal(first);
            }
        }
        
        /**
         * Removes and transfers nodes until hit non-cancelled one or null.
         * Split out from signal in part to encourage compilers to inline the case of no waiters.
         *
         * @param first (non-null) the first node on condition queue
         */
        // 将【|条件队列|】中的首结点追加到【|同步队列|】中，并让其处于待唤醒状态
        private void doSignal(Node first) {
            do {
                // 从【|条件队列|】中删除首结点
                if((firstWaiter = first.nextWaiter) == null) {
                    lastWaiter = null;
                }
                
                // 切断首结点与后续结点的关联
                first.nextWaiter = null;
                
                /*
                 * 尝试将node状态码更新为0，并追加到【|同步队列|】，并为其前驱设置Node.SIGNAL标记
                 * 如果更新状态码时就失败了，需要摘取【|条件队列|】的下一个结点继续尝试
                 */
            } while(!transferForSignal(first) && (first = firstWaiter) != null);
        }
        
        /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *                                      returns {@code false}
         */
        // 将【|条件队列|】中所有结点挨个追加到【|同步队列|】中，并让其处于待唤醒状态
        public final void signalAll() {
            // 如果当前线程不是锁的占用者，抛出异常
            if(!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            
            Node first = firstWaiter;
            if(first != null) {
                doSignalAll(first);
            }
        }
        
        /**
         * Removes and transfers all nodes.
         *
         * @param first (non-null) the first node on condition queue
         */
        // 将【|条件队列|】中所有结点挨个追加到【|同步队列|】中，并让其处于待唤醒状态
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while(first != null);
        }
        
        /*▲ 准备唤醒 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *
         * @return {@code true} if there are any waiting threads
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *                                      returns {@code false}
         */
        // 判断当前【|条件队列|】中是否存在等待者
        protected final boolean hasWaiters() {
            // 如果当前线程不是锁的占用者，抛出异常
            if(!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            
            for(Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if(w.waitStatus == Node.CONDITION) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *                                      returns {@code false}
         */
        // 返回【|条件队列|】长度
        protected final int getWaitQueueLength() {
            // 如果当前线程不是锁的占用者，抛出异常
            if(!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int n = 0;
            for(Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if(w.waitStatus == Node.CONDITION) {
                    ++n;
                }
            }
            return n;
        }
        
        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         *
         * @return the collection of threads
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *                                      returns {@code false}
         */
        // 返回一个集合，该集合包含了【|条件队列|】中所有结点内缓存的线程引用
        protected final Collection<Thread> getWaitingThreads() {
            // 如果当前线程不是锁的占用者，抛出异常
            if(!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            
            ArrayList<Thread> list = new ArrayList<>();
            for(Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if(w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if(t != null)
                        list.add(t);
                }
            }
            return list;
        }
        
        /**
         * Returns true if this condition was created by the given synchronization object.
         *
         * @return {@code true} if owned
         */
        // 判断当前条件对象是否归同步队列（锁）sync所有
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }
        
        /**
         * Adds a new waiter to wait queue.
         *
         * @return its new wait node
         */
        /*
         * 添加一个新的[条件Node]到【|条件队列|】，该Node缓存了对当前线程的引用
         * 添加完成后，返回刚添加的node
         */
        private Node addConditionWaiter() {
            // 如果当前线程不是锁的占用者，抛出异常
            if(!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            if(t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            
            // 创建一个带有Node.CONDITION标记的Node
            Node node = new Node(Node.CONDITION);
            
            if(t == null) {
                firstWaiter = node;
            } else {
                t.nextWaiter = node;
            }
            
            lastWaiter = node;
            
            return node;
        }
        
        /**
         * Unlinks cancelled waiter nodes from condition queue. Called only while holding lock.
         * This is called when cancellation occurred during condition wait,
         * and upon insertion of a new waiter when lastWaiter is seen to have been cancelled.
         * This method is needed to avoid garbage retention in the absence of signals.
         * So even though it may require a full traversal,
         * it comes into play only when timeouts or cancellations occur in the absence of signals.
         * It traverses all nodes rather than stopping at a particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation storms.
         */
        // 从【|条件队列|】中移除已取消的等待者
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while(t != null) {
                Node next = t.nextWaiter;
                if(t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if(trail == null) {
                        firstWaiter = next;
                    } else {
                        trail.nextWaiter = next;
                    }
                    if(next == null) {
                        lastWaiter = trail;
                    }
                } else {
                    trail = t;
                }
                t = next;
            }
        }
        
        /**
         * Checks for interrupt, returning THROW_IE if interrupted before signalled, REINTERRUPT if after signalled, or 0 if not interrupted.
         */
        /*
         * 检查当前线程是否有中断标记，并清除中断标记
         *
         * 返回0：无中断标记
         * 返回THROW_IE(-1)：有中断标记，且node成功加入到了【|同步队列|】末尾
         * 返回REINTERRUPT(1)：有中断标记，node还在【|条件队列|】中
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ? (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : 0;
        }
        
        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         */
        // 对刚刚唤醒的带有中断标记的线程进行特殊处理
        private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
            // 如果该线程已经在【|同步队列|】，则抛出异常
            if(interruptMode == THROW_IE) {
                throw new InterruptedException();
            } else if(interruptMode == REINTERRUPT) {
                // 否则，说明该线程node仍在【|条件队列|】，此时需要为线程恢复中断标记
                selfInterrupt();
            }
        }
    }
    
}
