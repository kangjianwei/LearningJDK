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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * A reusable synchronization barrier, similar in functionality to
 * {@link CyclicBarrier} and {@link CountDownLatch} but supporting
 * more flexible usage.
 *
 * <p><b>Registration.</b> Unlike the case for other barriers, the
 * number of parties <em>registered</em> to synchronize on a phaser
 * may vary over time.  Tasks may be registered at any time (using
 * methods {@link #register}, {@link #bulkRegister}, or forms of
 * constructors establishing initial numbers of parties), and
 * optionally deregistered upon any arrival (using {@link
 * #arriveAndDeregister}).  As is the case with most basic
 * synchronization constructs, registration and deregistration affect
 * only internal counts; they do not establish any further internal
 * bookkeeping, so tasks cannot query whether they are registered.
 * (However, you can introduce such bookkeeping by subclassing this
 * class.)
 *
 * <p><b>Synchronization.</b> Like a {@code CyclicBarrier}, a {@code
 * Phaser} may be repeatedly awaited.  Method {@link
 * #arriveAndAwaitAdvance} has effect analogous to {@link
 * java.util.concurrent.CyclicBarrier#await CyclicBarrier.await}. Each
 * generation of a phaser has an associated phase number. The phase
 * number starts at zero, and advances when all parties arrive at the
 * phaser, wrapping around to zero after reaching {@code
 * Integer.MAX_VALUE}. The use of phase numbers enables independent
 * control of actions upon arrival at a phaser and upon awaiting
 * others, via two kinds of methods that may be invoked by any
 * registered party:
 *
 * <ul>
 *
 *   <li><b>Arrival.</b> Methods {@link #arrive} and
 *       {@link #arriveAndDeregister} record arrival.  These methods
 *       do not block, but return an associated <em>arrival phase
 *       number</em>; that is, the phase number of the phaser to which
 *       the arrival applied. When the final party for a given phase
 *       arrives, an optional action is performed and the phase
 *       advances.  These actions are performed by the party
 *       triggering a phase advance, and are arranged by overriding
 *       method {@link #onAdvance(int, int)}, which also controls
 *       termination. Overriding this method is similar to, but more
 *       flexible than, providing a barrier action to a {@code
 *       CyclicBarrier}.
 *
 *   <li><b>Waiting.</b> Method {@link #awaitAdvance} requires an
 *       argument indicating an arrival phase number, and returns when
 *       the phaser advances to (or is already at) a different phase.
 *       Unlike similar constructions using {@code CyclicBarrier},
 *       method {@code awaitAdvance} continues to wait even if the
 *       waiting thread is interrupted. Interruptible and timeout
 *       versions are also available, but exceptions encountered while
 *       tasks wait interruptibly or with timeout do not change the
 *       state of the phaser. If necessary, you can perform any
 *       associated recovery within handlers of those exceptions,
 *       often after invoking {@code forceTermination}.  Phasers may
 *       also be used by tasks executing in a {@link ForkJoinPool}.
 *       Progress is ensured if the pool's parallelismLevel can
 *       accommodate the maximum number of simultaneously blocked
 *       parties.
 *
 * </ul>
 *
 * <p><b>Termination.</b> A phaser may enter a <em>termination</em>
 * state, that may be checked using method {@link #isTerminated}. Upon
 * termination, all synchronization methods immediately return without
 * waiting for advance, as indicated by a negative return value.
 * Similarly, attempts to register upon termination have no effect.
 * Termination is triggered when an invocation of {@code onAdvance}
 * returns {@code true}. The default implementation returns {@code
 * true} if a deregistration has caused the number of registered
 * parties to become zero.  As illustrated below, when phasers control
 * actions with a fixed number of iterations, it is often convenient
 * to override this method to cause termination when the current phase
 * number reaches a threshold. Method {@link #forceTermination} is
 * also available to abruptly release waiting threads and allow them
 * to terminate.
 *
 * <p><b>Tiering.</b> Phasers may be <em>tiered</em> (i.e.,
 * constructed in tree structures) to reduce contention. Phasers with
 * large numbers of parties that would otherwise experience heavy
 * synchronization contention costs may instead be set up so that
 * groups of sub-phasers share a common parent.  This may greatly
 * increase throughput even though it incurs greater per-operation
 * overhead.
 *
 * <p>In a tree of tiered phasers, registration and deregistration of
 * child phasers with their parent are managed automatically.
 * Whenever the number of registered parties of a child phaser becomes
 * non-zero (as established in the {@link #Phaser(Phaser,int)}
 * constructor, {@link #register}, or {@link #bulkRegister}), the
 * child phaser is registered with its parent.  Whenever the number of
 * registered parties becomes zero as the result of an invocation of
 * {@link #arriveAndDeregister}, the child phaser is deregistered
 * from its parent.
 *
 * <p><b>Monitoring.</b> While synchronization methods may be invoked
 * only by registered parties, the current state of a phaser may be
 * monitored by any caller.  At any given moment there are {@link
 * #getRegisteredParties} parties in total, of which {@link
 * #getArrivedParties} have arrived at the current phase ({@link
 * #getPhase}).  When the remaining ({@link #getUnarrivedParties})
 * parties arrive, the phase advances.  The values returned by these
 * methods may reflect transient states and so are not in general
 * useful for synchronization control.  Method {@link #toString}
 * returns snapshots of these state queries in a form convenient for
 * informal monitoring.
 *
 * <p><b>Sample usages:</b>
 *
 * <p>A {@code Phaser} may be used instead of a {@code CountDownLatch}
 * to control a one-shot action serving a variable number of parties.
 * The typical idiom is for the method setting this up to first
 * register, then start all the actions, then deregister, as in:
 *
 * <pre> {@code
 * void runTasks(List<Runnable> tasks) {
 *   Phaser startingGate = new Phaser(1); // "1" to register self
 *   // create and start threads
 *   for (Runnable task : tasks) {
 *     startingGate.register();
 *     new Thread(() -> {
 *       startingGate.arriveAndAwaitAdvance();
 *       task.run();
 *     }).start();
 *   }
 *
 *   // deregister self to allow threads to proceed
 *   startingGate.arriveAndDeregister();
 * }}</pre>
 *
 * <p>One way to cause a set of threads to repeatedly perform actions
 * for a given number of iterations is to override {@code onAdvance}:
 *
 * <pre> {@code
 * void startTasks(List<Runnable> tasks, int iterations) {
 *   Phaser phaser = new Phaser() {
 *     protected boolean onAdvance(int phase, int registeredParties) {
 *       return phase >= iterations - 1 || registeredParties == 0;
 *     }
 *   };
 *   phaser.register();
 *   for (Runnable task : tasks) {
 *     phaser.register();
 *     new Thread(() -> {
 *       do {
 *         task.run();
 *         phaser.arriveAndAwaitAdvance();
 *       } while (!phaser.isTerminated());
 *     }).start();
 *   }
 *   // allow threads to proceed; don't wait for them
 *   phaser.arriveAndDeregister();
 * }}</pre>
 *
 * If the main task must later await termination, it
 * may re-register and then execute a similar loop:
 * <pre> {@code
 *   // ...
 *   phaser.register();
 *   while (!phaser.isTerminated())
 *     phaser.arriveAndAwaitAdvance();}</pre>
 *
 * <p>Related constructions may be used to await particular phase numbers
 * in contexts where you are sure that the phase will never wrap around
 * {@code Integer.MAX_VALUE}. For example:
 *
 * <pre> {@code
 * void awaitPhase(Phaser phaser, int phase) {
 *   int p = phaser.register(); // assumes caller not already registered
 *   while (p < phase) {
 *     if (phaser.isTerminated())
 *       // ... deal with unexpected termination
 *     else
 *       p = phaser.arriveAndAwaitAdvance();
 *   }
 *   phaser.arriveAndDeregister();
 * }}</pre>
 *
 * <p>To create a set of {@code n} tasks using a tree of phasers, you
 * could use code of the following form, assuming a Task class with a
 * constructor accepting a {@code Phaser} that it registers with upon
 * construction. After invocation of {@code build(new Task[n], 0, n,
 * new Phaser())}, these tasks could then be started, for example by
 * submitting to a pool:
 *
 * <pre> {@code
 * void build(Task[] tasks, int lo, int hi, Phaser ph) {
 *   if (hi - lo > TASKS_PER_PHASER) {
 *     for (int i = lo; i < hi; i += TASKS_PER_PHASER) {
 *       int j = Math.min(i + TASKS_PER_PHASER, hi);
 *       build(tasks, i, j, new Phaser(ph));
 *     }
 *   } else {
 *     for (int i = lo; i < hi; ++i)
 *       tasks[i] = new Task(ph);
 *       // assumes new Task(ph) performs ph.register()
 *   }
 * }}</pre>
 *
 * The best value of {@code TASKS_PER_PHASER} depends mainly on
 * expected synchronization rates. A value as low as four may
 * be appropriate for extremely small per-phase task bodies (thus
 * high rates), or up to hundreds for extremely large ones.
 *
 * <p><b>Implementation notes</b>: This implementation restricts the
 * maximum number of parties to 65535. Attempts to register additional
 * parties result in {@code IllegalStateException}. However, you can and
 * should create tiered phasers to accommodate arbitrarily large sets
 * of participants.
 *
 * @since 1.7
 * @author Doug Lea
 */
/*
 * Phaser可用于在分阶段任务的不同步骤之间同步
 *
 * 在同步思想上，Phaser跟CyclicBarrier和CountDownLatch差不多，
 * 都是在所有的信号到达屏障之后再集体放行。
 * 不同的是，Phaser的控制更精细，除了可以对线程整体控制，
 * 还可以对线程（任务）内的不同阶段做同步控制，
 * 甚至，可以将一个大任务划分成小任务，然后在各个小任务之间保持同步
 *
 * 术语约定：
 * unarrived parties -- Phaser中未达下一个屏障的信号数量
 * parties           -- Phaser中注册的信号总数
 * phase             -- Phaser中所处的阶段
 * terminated        -- 标记Phaser中所有屏障已经终止（Phaser不再发挥拦截作用）
 *
 * state状态位：
 * |63        63|62   32|31     16|15                0|
 * |-terminated-|-phase-|-parties-|-unarrived parties-|
 *
 * 注：parties有时统称为信号
 */
public class Phaser {
    /*
     * This class implements an extension of X10 "clocks".
     * Thanks to Vijay Saraswat for the idea, and to Vivek Sarkar for enhancements to extend functionality.
     */
    
    /**
     * Primary state representation, holding four bit-fields:
     *
     * unarrived  -- the number of parties yet to hit barrier (bits  0-15)
     * parties    -- the number of parties to wait            (bits 16-31)
     * phase      -- the generation of the barrier            (bits 32-62)
     * terminated -- set if barrier is terminated             (bit  63 / sign)
     *
     * Except that a phaser with no registered parties is
     * distinguished by the otherwise illegal state of having zero
     * parties and one unarrived parties (encoded as EMPTY below).
     *
     * To efficiently maintain atomicity, these values are packed into
     * a single (atomic) long. Good performance relies on keeping
     * state decoding and encoding simple, and keeping race windows
     * short.
     *
     * All state updates are performed via CAS except initial
     * registration of a sub-phaser (i.e., one with a non-null
     * parent).  In this (relatively rare) case, we use built-in
     * synchronization to lock while first registering with its
     * parent.
     *
     * The phase of a subphaser is allowed to lag that of its
     * ancestors until it is actually accessed -- see method
     * reconcileState.
     */
    // Phaser状态位，集结了terminated、phase、parties、unarrived的信息，指引每一轮操作
    private volatile long state;
    
    // terminated [63]
    private static final long TERMINATION_BIT = 1L << 63;
    
    // phase [32, 62]
    private static final int  MAX_PHASE       = Integer.MAX_VALUE;
    private static final int  PHASE_SHIFT     = 32;
    
    // parties [16, 31]
    private static final int  MAX_PARTIES     = 0xffff;
    private static final int  PARTIES_SHIFT   = 16;
    private static final long PARTIES_MASK    = 0xffff_0000L; // to mask longs
    
    // unarrived parties [0, 15]
    private static final int  UNARRIVED_MASK  = 0x0000_ffff;  // to mask ints
    
    // parties - unarrived parties
    private static final long COUNTS_MASK     = 0xffff_ffffL;
    
    // 一个unarrived parties
    private static final int  ONE_ARRIVAL     = 1;                      // 0x0000_0001
    
    // 一个parties
    private static final int  ONE_PARTY       = 1 << PARTIES_SHIFT;     // 0x0001_0000
    
    // 一个parties和一个unarrived parties
    private static final int  ONE_DEREGISTER  = ONE_ARRIVAL|ONE_PARTY;  // 0x0001_0001
    
    // 特殊标记，代表Phaser中存在初始化信号
    private static final int  EMPTY           = 1;
    
    
    /** The number of CPUs, for spin control */
    // 返回虚拟机可用的处理器数量
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    
    /**
     * The number of times to spin before blocking while waiting for
     * advance, per arrival while waiting. On multiprocessors, fully
     * blocking and waking up a large number of threads all at once is
     * usually a very slow process, so we use rechargeable spins to
     * avoid it when threads regularly arrive: When a thread in
     * internalAwaitAdvance notices another arrival before blocking,
     * and there appear to be enough CPUs available, it spins
     * SPINS_PER_ARRIVAL more times before blocking. The value trades
     * off good-citizenship vs big unnecessary slowdowns.
     */
    // 忙等待中的空转次数
    static final int SPINS_PER_ARRIVAL = (NCPU < 2) ? 1 : 1 << 8;
    
    /**
     * The parent of this phaser, or null if none.
     */
    private final Phaser parent;
    
    /**
     * The root of phaser tree. Equals this if not in a tree.
     */
    private final Phaser root;
    
    /**
     * Heads of Treiber stacks for waiting threads. To eliminate
     * contention when releasing some threads while adding others, we
     * use two of them, alternating across even and odd phases.
     * Subphasers share queues with root to speed up releases.
     */
    // 阻塞队列
    private final AtomicReference<QNode> evenQ;
    private final AtomicReference<QNode> oddQ;
    
    
    // VarHandle mechanics
    private static final VarHandle STATE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE = l.findVarHandle(Phaser.class, "state", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
        
        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new phaser with no initially registered parties, no
     * parent, and initial phase number 0. Any thread using this
     * phaser will need to first register for it.
     */
    public Phaser() {
        this(null, 0);
    }
    
    /**
     * Creates a new phaser with the given number of registered
     * unarrived parties, no parent, and initial phase number 0.
     *
     * @param parties the number of parties required to advance to the
     *                next phase
     *
     * @throws IllegalArgumentException if parties less than zero
     *                                  or greater than the maximum number of parties supported
     */
    public Phaser(int parties) {
        this(null, parties);
    }
    
    /**
     * Equivalent to {@link #Phaser(Phaser, int) Phaser(parent, 0)}.
     *
     * @param parent the parent phaser
     */
    public Phaser(Phaser parent) {
        this(parent, 0);
    }
    
    /**
     * Creates a new phaser with the given parent and number of
     * registered unarrived parties.  When the given parent is non-null
     * and the given number of parties is greater than zero, this
     * child phaser is registered with its parent.
     *
     * @param parent  the parent phaser
     * @param parties the number of parties required to advance to the
     *                next phase
     *
     * @throws IllegalArgumentException if parties less than zero
     *                                  or greater than the maximum number of parties supported
     */
    public Phaser(Phaser parent, int parties) {
        if(parties >>> PARTIES_SHIFT != 0) {
            throw new IllegalArgumentException("Illegal number of parties");
        }
        
        int phase = 0;
        
        this.parent = parent;
        
        // 如果存在parent
        if(parent != null) {
            final Phaser root = parent.root;
            
            this.root = root;
            
            // 共用阻塞队列
            this.evenQ = root.evenQ;
            this.oddQ = root.oddQ;
            
            if(parties != 0) {
                /*
                 * 向parent也注册一个parties和一个unarrived parties，
                 * 这意味着parent会受到当前Phaser的制约，
                 * 如果parent想要进入下一个阶段，除了自身的信号要到达屏障之外，
                 * 还需要当前Phaser的所有信号也到达屏障
                 */
                phase = parent.doRegister(1);
            }
        } else {
            this.root = this;
            this.evenQ = new AtomicReference<QNode>();
            this.oddQ = new AtomicReference<QNode>();
        }
        
        this.state = (parties == 0)
            ? (long) EMPTY
            : ((long) phase << PHASE_SHIFT) | ((long) parties << PARTIES_SHIFT) | ((long) parties);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注册信号 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Adds a new unarrived party to this phaser.
     * If an ongoing invocation of {@link #onAdvance} is in progress,
     * this method may await its completion before returning.
     * If this phaser has a parent, and this phaser previously had no registered parties,
     * this child phaser is also registered with its parent.
     * If this phaser is terminated, the attempt to register has no effect, and a negative value is returned.
     *
     * @return the arrival phase number to which this registration applied.
     * If this value is negative, then this phaser has terminated, in which case registration has no effect.
     *
     * @throws IllegalStateException if attempting to register more
     *                               than the maximum supported number of parties
     */
    // 注册一个信号（增加一个parties和一个unarrived parties）
    public int register() {
        return doRegister(1);
    }
    
    /**
     * Adds the given number of new unarrived parties to this phaser.
     * If an ongoing invocation of {@link #onAdvance} is in progress,
     * this method may await its completion before returning.
     * If this phaser has a parent, and the given number of parties is greater than zero,
     * and this phaser previously had no registered parties,
     * this child phaser is also registered with its parent.
     * If this phaser is terminated, the attempt to register has no effect,
     * and a negative value is returned.
     *
     * @param parties the number of additional parties required to advance to the next phase
     *
     * @return the arrival phase number to which this registration applied.
     * If this value is negative, then this phaser has terminated, in which case registration has no effect.
     *
     * @throws IllegalStateException    if attempting to register more than the maximum supported number of parties
     * @throws IllegalArgumentException if {@code parties < 0}
     */
    // 批量注册信号（增加num个parties和num个unarrived parties）
    public int bulkRegister(int num) {
        if(num<0) {
            throw new IllegalArgumentException();
        }
        
        if(num == 0) {
            return getPhase();
        }
        
        return doRegister(num);
    }
    
    /**
     * Implementation of register, bulkRegister.
     *
     * @param registrations number to add to both parties and
     *                      unarrived fields. Must be greater than zero.
     */
    // 注册指定数量的信号（增加num个parties和num个unarrived parties）
    private int doRegister(int num) {
        // adjustment to state
        long adjust = ((long) num << PARTIES_SHIFT) | num;
        
        final Phaser parent = this.parent;
        
        int phase;
        
        for(; ; ) {
            long s = (parent == null) ? state : reconcileState();
            
            // 获取当前parties和unarrived
            int counts = (int) s;
            int parties = counts >>> PARTIES_SHIFT;     //  parties
            int unarrived = counts & UNARRIVED_MASK;    //  unarrived parties
            
            if(num>MAX_PARTIES - parties) {
                throw new IllegalStateException(badRegister(s));
            }
            
            phase = (int) (s >>> PHASE_SHIFT);          // phase
            if(phase<0) {
                break;
            }
            
            // 如果Phaser中不存在初始信号
            if(counts != EMPTY) {                  // not 1st registration
                if(parent == null || reconcileState() == s) {
                    if(unarrived == 0) {           // wait out advance
                        root.internalAwaitAdvance(phase, null);
                    } else if(STATE.compareAndSet(this, s, s + adjust)) {
                        break;
                    }
                }
                
                // 如果Phaser中存在初始信号，但不存在parent
            } else if(parent == null) {              // 1st root registration
                long next = ((long) phase << PHASE_SHIFT) | adjust;
                if(STATE.compareAndSet(this, s, next)) {
                    break;
                }
                
                // 如果Phaser中存在初始信号，且存在parent
            } else {
                synchronized(this) {               // 1st sub registration
                    // 如果相等，说明root-Phaser与当前Phaser所处的阶段一致
                    if(state == s) {               // recheck under lock
                        // 此时需要向parent注册信号（增加一个parties和一个unarrived parties）
                        phase = parent.doRegister(1);
                        if(phase<0) {
                            break;
                        }
                        
                        // finish registration whenever parent registration succeeded,
                        // even when racing with termination, since these are part of the same "transaction".
                        while(!STATE.weakCompareAndSet(this, s, ((long) phase << PHASE_SHIFT) | adjust)) {
                            s = state;
                            phase = (int) (root.state >>> PHASE_SHIFT);
                            // assert (int)s == EMPTY;
                        }
                        break;
                    }
                }
            }
        }
        
        return phase;
    }
    
    /*▲ 注册信号 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 到达屏障 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Arrives at this phaser and awaits others. Equivalent in effect to {@code awaitAdvance(arrive())}.
     * If you need to await with interruption or timeout,
     * you can arrange this with an analogous construction using one of the other forms of the {@code awaitAdvance} method.
     * If instead you need to deregister upon arrival, use {@code awaitAdvance(arriveAndDeregister())}.
     *
     * It is a usage error for an unregistered party to invoke this method.
     * However, this error may result in an {@code IllegalStateException} only upon some subsequent operation on this phaser, if ever.
     *
     * @return the arrival phase number, or the (negative) {@linkplain #getPhase() current phase} if terminated
     *
     * @throws IllegalStateException if not terminated and the number of unarrived parties would become negative
     */
    /*
     * 到达屏障，并酌情等待其他任务
     * 如果有未到屏障的信号，则当前任务会尝试阻塞
     * 如果所有信都已到达屏障，则当前阶段的所有任务都会被唤醒
     */
    public int arriveAndAwaitAdvance() {
        // Specialization of doArrive+awaitAdvance eliminating some reads/paths
        final Phaser root = this.root;
        
        for(; ; ) {
            // 获取state更新前的快照信息
            long s = (root == this) ? state : reconcileState();
            
            // 获取当前所处的阶段
            int phase = (int) (s >>> PHASE_SHIFT);
            if(phase<0) {
                return phase;
            }
            
            int counts = (int) s;
            
            // 获取Phaser中当前unarrived party的数量
            int unarrived = (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);
            if(unarrived<=0) {
                throw new IllegalStateException(badArrive(s));
            }
            
            // 更新state：原子地将unarrived party数量减一
            if(!STATE.compareAndSet(this, s, s -= ONE_ARRIVAL)) {
                continue;
            }
            
            /*
             * 每个信号达到屏障后，都要检查自身是不是最后一个到达的
             * 如果当前信号已经是最后一个到达屏障的信号，
             * 则需要重置unarrived parties数量，并更新phase信息，
             * 以便开始下一轮操作
             */
            
            // 如果还有其他unarrived party
            if(unarrived>1) {
                // 尝试让当前线程陷入阻塞，返回当前所处的阶段
                return root.internalAwaitAdvance(phase, null);
            }
            
            /*
             * 如果更新前的unarrived parties数量为1，
             * 在不考虑并发的影响下，说明此次更新后，
             * unarrived parties数量应当为0，
             * 即：所有信号已经到达屏障处
             */
            
            if(root != this) {
                return parent.arriveAndAwaitAdvance();
            }
            
            /*
             * 从当前state的快照信息中解析出parties数量,
             * 以作为下一轮操作的unarrived parties数量
             */
            long n = s & PARTIES_MASK;  // base of next state
            int nextUnarrived = (int) n >>> PARTIES_SHIFT;
            
            // 如果需要终止屏障
            if(onAdvance(phase, nextUnarrived)) {
                n |= TERMINATION_BIT;
                
                // 已经没有注册的parties
            } else if(nextUnarrived == 0) {
                n |= EMPTY;
            } else {
                n |= nextUnarrived;
            }
            
            // 将Phaser推进一个阶段
            int nextPhase = (phase + 1) & MAX_PHASE;
            n |= (long) nextPhase << PHASE_SHIFT;
            
            // 原子地更新state信息，以作为下一轮操作的指引
            if(!STATE.compareAndSet(this, s, n)) {
                // 如果state更新失败，则返回当前所处的阶段
                return (int) (state >>> PHASE_SHIFT); // terminated
            }
            
            // 唤醒phase阶段阻塞的线程
            releaseWaiters(phase);
            
            return nextPhase;
        }
    }
    
    /**
     * Arrives at this phaser, without waiting for others to arrive.
     *
     * <p>It is a usage error for an unregistered party to invoke this
     * method.  However, this error may result in an {@code
     * IllegalStateException} only upon some subsequent operation on
     * this phaser, if ever.
     *
     * @return the arrival phase number, or a negative value if terminated
     *
     * @throws IllegalStateException if not terminated and the number
     *                               of unarrived parties would become negative
     */
    /*
     * 到达屏障，不会等待其他任务
     *
     * 调整state信息：减少unarrived parties的数量
     * 如果unarrived parties的数量减为0，则需要推进phase到下一个阶段，并唤醒当前阶段阻塞的线程
     */
    public int arrive() {
        return doArrive(ONE_ARRIVAL);
    }
    
    /**
     * Arrives at this phaser and deregisters from it without waiting
     * for others to arrive. Deregistration reduces the number of
     * parties required to advance in future phases.  If this phaser
     * has a parent, and deregistration causes this phaser to have
     * zero parties, this phaser is also deregistered from its parent.
     *
     * <p>It is a usage error for an unregistered party to invoke this
     * method.  However, this error may result in an {@code
     * IllegalStateException} only upon some subsequent operation on
     * this phaser, if ever.
     *
     * @return the arrival phase number, or a negative value if terminated
     *
     * @throws IllegalStateException if not terminated and the number
     *                               of registered or unarrived parties would become negative
     */
    /*
     * 到达屏障，并取消注册
     *
     * 调整state信息：减少parties和unarrived parties的数量
     * 其中，减少parties意味着移除已注册的信号，是一个反注册动作
     *
     * 如果unarrived parties的数量减为0，则需要推进phase到下一个阶段，并唤醒当前阶段阻塞的线程
     */
    public int arriveAndDeregister() {
        return doArrive(ONE_DEREGISTER);
    }
    
    /**
     * Main implementation for methods arrive and arriveAndDeregister.
     * Manually tuned to speed up and minimize race windows for the
     * common case of just decrementing unarrived field.
     *
     * @param adjust value to subtract from state;
     *               ONE_ARRIVAL for arrive,
     *               ONE_DEREGISTER for arriveAndDeregister
     */
    /*
     * 处理到达事件
     * 调整state信息，如果unarrived parties的数量减为0，则需要唤醒当前阶段阻塞的线程
     */
    private int doArrive(int adjust) {
        final Phaser root = this.root;
        
        for(; ; ) {
            // 获取state更新前的快照信息
            long s = (root == this) ? state : reconcileState();
            
            // 获取Phaser当前所处的阶段
            int phase = (int) (s >>> PHASE_SHIFT);
            if(phase<0) {
                return phase;
            }
            
            int counts = (int) s;
            
            // 获取Phaser中当前的unarrived parties数量
            int unarrived = (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);
            if(unarrived<=0) {
                throw new IllegalStateException(badArrive(s));
            }
            
            /*
             * 更新state的快照信息和state信息，
             * 更新动作是：尝试减少parties和unarrived parties的数量
             * 如果更新失败，则需要重试
             */
            if(!STATE.compareAndSet(this, s, s -= adjust)) {
                continue;
            }
            
            /*
             * 每个信号达到屏障后，都要检查自身是不是最后一个到达的
             * 如果当前信号已经是最后一个到达屏障的信号，
             * 则需要重置unarrived parties数量，并更新phase信息，
             * 以便开始下一轮操作
             */
            
            /*
             * 如果更新前的unarrived parties数量为1，
             * 在不考虑并发的影响下，说明此次更新后，
             * unarrived parties数量应当为0，
             * 即：所有信号已经到达屏障处
             */
            if(unarrived == 1) {
                /*
                 * 从当前state的快照信息中解析出parties数量,
                 * 以作为下一轮操作的unarrived parties数量
                 */
                long n = s & PARTIES_MASK;  // base of next state
                int nextUnarrived = (int) n >>> PARTIES_SHIFT;
                
                if(root == this) {
                    // 如果需要终止屏障
                    if(onAdvance(phase, nextUnarrived)) {
                        n |= TERMINATION_BIT;
                        
                        // 已经没有注册的parties
                    } else if(nextUnarrived == 0) {
                        n |= EMPTY;
                    } else {
                        n |= nextUnarrived;
                    }
                    
                    // 将Phaser推进一个阶段
                    int nextPhase = (phase + 1) & MAX_PHASE;
                    n |= (long) nextPhase << PHASE_SHIFT;
                    
                    // 原子地更新state信息，以作为下一轮操作的指引
                    STATE.compareAndSet(this, s, n);
                    
                    // 唤醒phase阶段阻塞的线程
                    releaseWaiters(phase);
                    
                    // 如果此时已经没有注册的信号
                } else if(nextUnarrived == 0) { // propagate deregistration
                    // 需要从parent中也同步移除注册信息
                    phase = parent.doArrive(ONE_DEREGISTER);
                    STATE.compareAndSet(this, s, s | EMPTY);
                } else {
                    // 将信号到达事件传递给parent
                    phase = parent.doArrive(ONE_ARRIVAL);
                }
            }
            
            return phase;
        }
    }
    
    /**
     * Awaits the phase of this phaser to advance from the given phase
     * value, returning immediately if the current phase is not equal
     * to the given phase value or this phaser is terminated.
     *
     * @param phase an arrival phase number, or negative value if
     *              terminated; this argument is normally the value returned by a
     *              previous call to {@code arrive} or {@code arriveAndDeregister}.
     *
     * @return the next arrival phase number, or the argument if it is
     * negative, or the (negative) {@linkplain #getPhase() current phase}
     * if terminated
     */
    // 尝试让phase阶段的线程陷入阻塞，并返回当前所处的阶段（不考虑线程的中断与超时）
    public int awaitAdvance(int phase) {
        final Phaser root = this.root;
        
        // 获取state当前的快照信息
        long s = (root == this) ? state : reconcileState();
        
        // 获取Phaser当前所处的阶段
        int p = (int) (s >>> PHASE_SHIFT);
        if(phase<0) {
            return phase;
        }
        
        if(p == phase) {
            // 尝试让phase阶段的线程陷入阻塞，并返回当前所处的阶段
            return root.internalAwaitAdvance(phase, null);
        }
        
        return p;
    }
    
    /**
     * Awaits the phase of this phaser to advance from the given phase
     * value, throwing {@code InterruptedException} if interrupted
     * while waiting, or returning immediately if the current phase is
     * not equal to the given phase value or this phaser is
     * terminated.
     *
     * @param phase an arrival phase number, or negative value if
     *              terminated; this argument is normally the value returned by a
     *              previous call to {@code arrive} or {@code arriveAndDeregister}.
     *
     * @return the next arrival phase number, or the argument if it is
     * negative, or the (negative) {@linkplain #getPhase() current phase}
     * if terminated
     *
     * @throws InterruptedException if thread interrupted while waiting
     */
    // 尝试让phase阶段的线程陷入阻塞，并返回当前所处的阶段（需要考虑线程的中断）
    public int awaitAdvanceInterruptibly(int phase) throws InterruptedException {
        final Phaser root = this.root;
        
        // 获取state当前的快照信息
        long s = (root == this) ? state : reconcileState();
        
        // 获取Phaser当前所处的阶段
        int p = (int) (s >>> PHASE_SHIFT);
        if(phase<0) {
            return phase;
        }
        
        if(p == phase) {
            // 第3个参数为true，说明需要处理中断
            QNode node = new QNode(this, phase, true, false, 0L);
            p = root.internalAwaitAdvance(phase, node);
            if(node.wasInterrupted) {
                throw new InterruptedException();
            }
        }
        
        return p;
    }
    
    /**
     * Awaits the phase of this phaser to advance from the given phase
     * value or the given timeout to elapse, throwing {@code
     * InterruptedException} if interrupted while waiting, or
     * returning immediately if the current phase is not equal to the
     * given phase value or this phaser is terminated.
     *
     * @param phase   an arrival phase number, or negative value if
     *                terminated; this argument is normally the value returned by a
     *                previous call to {@code arrive} or {@code arriveAndDeregister}.
     * @param timeout how long to wait before giving up, in units of
     *                {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     *
     * @return the next arrival phase number, or the argument if it is
     * negative, or the (negative) {@linkplain #getPhase() current phase}
     * if terminated
     *
     * @throws InterruptedException if thread interrupted while waiting
     * @throws TimeoutException     if timed out while waiting
     */
    // 尝试让phase阶段的线程陷入阻塞，并返回当前所处的阶段（需要考虑线程的中断与超时）
    public int awaitAdvanceInterruptibly(int phase, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        // 将当前时间单位下的timeout换算为纳秒
        long nanos = unit.toNanos(timeout);
        
        final Phaser root = this.root;
        
        // 获取state当前的快照信息
        long s = (root == this) ? state : reconcileState();
        
        // 获取Phaser当前所处的阶段
        int p = (int) (s >>> PHASE_SHIFT);
        if(phase<0) {
            return phase;
        }
        
        if(p == phase) {
            // 第3个参数为true，说明需要处理中断；第4个参数为true，说明需要处理超时
            QNode node = new QNode(this, phase, true, true, nanos);
            p = root.internalAwaitAdvance(phase, node);
            
            if(node.wasInterrupted) {
                throw new InterruptedException();
            } else if(p == phase) {
                throw new TimeoutException();
            }
        }
        
        return p;
    }
    
    /*▲ 到达屏障 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 阻塞/唤醒 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Possibly blocks and waits for phase to advance unless aborted.
     * Call only on root phaser.
     *
     * @param phase current phase
     * @param node  if non-null, the wait node to track interrupt and timeout;
     *              if null, denotes noninterruptible wait
     *
     * @return current phase
     */
    // 尝试让phase阶段的线程陷入阻塞，并返回当前所处的阶段
    private int internalAwaitAdvance(int phase, QNode node) {
        /* assert root == this; */
        
        // 首先确保上个阶段的线程已被唤醒
        releaseWaiters(phase - 1);        // ensure old queue clean
        
        boolean queued = false;           // true when node is enqueued
        int lastUnarrived = 0;            // to increase spins upon change
        int spins = SPINS_PER_ARRIVAL;
        long s;
        int p;
        
        // 确保仍处在当前阶段内
        while((p = (int) ((s = state) >>> PHASE_SHIFT)) == phase) {
            // 如果当前还不存在阻塞结点
            if(node == null) {
                /* spinning in noninterruptible mode */
                
                // 获取Phaser中当前的unarrived parties数量
                int unarrived = (int) s & UNARRIVED_MASK;
                
                // 如果当前未达下一个屏障的信号数量较少，则增加空转次数以待处理（空转比阻塞的开销小）
                if(unarrived != lastUnarrived && (lastUnarrived = unarrived)<NCPU) {
                    spins += SPINS_PER_ARRIVAL;
                }
                
                // 测试当前线程是否已经中断，线程的中断状态会被清除
                boolean interrupted = Thread.interrupted();
                if(interrupted || --spins<0) {
                    // 如果空转结束了，则需要创建阻塞结点，以记录即将被阻塞的线程与其他附加信息
                    node = new QNode(this, phase, false, false, 0L);    // need node to record intr
                    node.wasInterrupted = interrupted;
                } else {
                    // 忙等待
                    Thread.onSpinWait();
                }
                
                // 如果存在阻塞结点，但是当前线程需要解除阻塞，那么跳出循环
            } else if(node.isReleasable()) { // done or aborted
                break;
                
                // 如果阻塞结点还未进入排队状态
            } else if(!queued) {             // push onto queue
                // 定位到当前阶段对应的阻塞队列的队头
                AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
                
                // 使用头插法将当前阻塞结点加入阻塞队列
                QNode q = node.next = head.get();
                if((q == null || q.phase == phase) && (int) (state >>> PHASE_SHIFT) == phase) { // avoid stale enq
                    queued = head.compareAndSet(q, node);
                }
                
                // 阻塞结点已进入排队状态
            } else {
                try {
                    // 将阻塞节点内包含的（当前）线程阻塞
                    ForkJoinPool.managedBlock(node);
                } catch(InterruptedException cantHappen) {
                    node.wasInterrupted = true;
                }
            }
        }
        
        if(node != null) {
            // 清空阻塞线程信息
            if(node.thread != null) {
                node.thread = null;       // avoid need for unpark()
            }
            
            // 如果当前线程已被中断，但是不需要处理中断
            if(node.wasInterrupted && !node.interruptible) {
                // 重新为线程设置中断标记
                Thread.currentThread().interrupt();
            }
            
            // 如果Phaser所处阶段没有发生变化
            if(p == phase && (p = (int) (state >>> PHASE_SHIFT)) == phase) {
                return abortWait(phase); // possibly clean up on abort
            }
        }
        
        // 唤醒phase阶段阻塞的线程
        releaseWaiters(phase);
        
        return p;
    }
    
    /**
     * Removes and signals threads from queue for phase.
     */
    // 唤醒phase阶段阻塞的线程
    private void releaseWaiters(int phase) {
        //        QNode q;
        //        Thread t;
        //
        //        // 定位到当前阶段对应的阻塞队列的队头
        //        AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
        //
        //        // 如果存在排队的结点（结点的phase域要跟Phaser当前的phase域不一致才算，因为需要唤醒的是上一轮操作中阻塞的线程）
        //        while((q = head.get()) != null && q.phase != (int) (root.state >>> PHASE_SHIFT)) {
        //            // 摘下目标结点
        //            if(head.compareAndSet(q, q.next) && (t = q.thread) != null) {
        //                q.thread = null;
        //                // 唤醒阻塞的线程
        //                LockSupport.unpark(t);
        //            }
        //        }
        
        // 定位到当前阶段对应的阻塞队列的队头
        AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
        
        for(; ; ){
            Thread t; // first element of queue
            QNode q = head.get();   // its thread
            
            // Phaser当前所处阶段
            int p = (int) (root.state >>> PHASE_SHIFT);
            
            // 如果阻塞队列为null，直接返回
            if(q==null){
                return;
            }
            
            // 如果阻塞结点所处的阶段与Phaser当前所处阶段一致，则不需要唤醒
            if(q.phase==p){
                return;
            }
            
            // 摘下目标结点
            if(head.compareAndSet(q, q.next) && (t = q.thread) != null) {
                q.thread = null;
                // 唤醒阻塞的线程
                LockSupport.unpark(t);
            }
        }
    }
    
    /**
     * Variant of releaseWaiters that additionally tries to remove any nodes no longer waiting for advance due to timeout or interrupt.
     * Currently, nodes are removed only if they are at head of queue, which suffices to reduce memory footprint in most usages.
     *
     * @return current phase on exit
     */
    /*
     * releaseWaiters的变体，除了唤醒phase阶段阻塞的线程，还需要清理一些不包含thread的阻塞结点
     *
     * 出现不包含thread的阻塞结点的原因是：
     * Phaser所处的阶段发生了改变、需要处理线程中出现的中断、线程阻塞超时（参考isReleasable()中的判断）
     */
    private int abortWait(int phase) {
        // 定位到当前阶段对应的阻塞队列的队头
        AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
        
        for(; ; ) {
            Thread t;
            QNode q = head.get();
            
            // Phaser当前所处阶段
            int p = (int) (root.state >>> PHASE_SHIFT);
            
            // 如果阻塞队列为null，直接返回
            if(q == null) {
                return p;
            }
            
            /*
             * 如果t为空，后面直接跳过该结点
             * 如果t不为空，那么如果阻塞结点所处的阶段与Phaser当前阶段一致，则不需要唤醒，
             */
            if((t = q.thread) != null && q.phase == p){
                return p;
            }
            
            // 摘下队头结点，并唤醒其中被阻塞的线程
            if(head.compareAndSet(q, q.next) && t != null) {
                q.thread = null;
                LockSupport.unpark(t);
            }
        }
    }
    
    /*▲ 阻塞/唤醒 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current phase number. The maximum phase number is
     * {@code Integer.MAX_VALUE}, after which it restarts at
     * zero. Upon termination, the phase number is negative,
     * in which case the prevailing phase prior to termination
     * may be obtained via {@code getPhase() + Integer.MIN_VALUE}.
     *
     * @return the phase number, or a negative value if terminated
     */
    // 获取root-Phaser所处的阶段
    public final int getPhase() {
        return (int) (root.state >>> PHASE_SHIFT);
    }
    
    /**
     * Returns the parent of this phaser, or {@code null} if none.
     *
     * @return the parent of this phaser, or {@code null} if none
     */
    // 获取parent-Phaser
    public Phaser getParent() {
        return parent;
    }
    
    /**
     * Returns the root ancestor of this phaser, which is the same as
     * this phaser if it has no parent.
     *
     * @return the root ancestor of this phaser
     */
    // 获取root-Phaser
    public Phaser getRoot() {
        return root;
    }
    
    /**
     * Returns the number of parties registered at this phaser.
     *
     * @return the number of parties
     */
    // 获取parties数量
    public int getRegisteredParties() {
        return partiesOf(state);
    }
    
    /**
     * Returns the number of registered parties that have arrived at
     * the current phase of this phaser. If this phaser has terminated,
     * the returned value is meaningless and arbitrary.
     *
     * @return the number of arrived parties
     */
    // 获取已到达屏障的信号数量
    public int getArrivedParties() {
        return arrivedOf(reconcileState());
    }
    
    /**
     * Returns the number of registered parties that have not yet
     * arrived at the current phase of this phaser. If this phaser has
     * terminated, the returned value is meaningless and arbitrary.
     *
     * @return the number of unarrived parties
     */
    // 获取未到达屏障的信号数量
    public int getUnarrivedParties() {
        return unarrivedOf(reconcileState());
    }
    
    /**
     * Returns {@code true} if this phaser has been terminated.
     *
     * @return {@code true} if this phaser has been terminated
     */
    // Phaser是否停止了对屏障的响应
    public boolean isTerminated() {
        return root.state<0L;
    }
    
    /**
     * Forces this phaser to enter termination state.  Counts of
     * registered parties are unaffected.  If this phaser is a member
     * of a tiered set of phasers, then all of the phasers in the set
     * are terminated.  If this phaser is already terminated, this
     * method has no effect.  This method may be useful for
     * coordinating recovery after one or more tasks encounter
     * unexpected exceptions.
     */
    // 使Phaser强制进入终止状态，即屏障不再有效
    public void forceTermination() {
        // Only need to change root state
        final Phaser root = this.root;
        long s;
        
        while((s = root.state) >= 0) {
            if(STATE.compareAndSet(root, s, s | TERMINATION_BIT)) {
                // signal all threads
                releaseWaiters(0); // Waiters on evenQ
                releaseWaiters(1); // Waiters on oddQ
                return;
            }
        }
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 回调 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Overridable method to perform an action upon impending phase advance, and to control termination.
     * This method is invoked upon arrival of the party advancing this phaser (when all other waiting parties are dormant).
     * If this method returns {@code true}, this phaser will be set to a final termination state upon advance,
     * and subsequent calls to {@link #isTerminated} will return true.
     * Any (unchecked) Exception or Error thrown by an invocation of this method is propagated to the party attempting to advance this phaser,
     * in which case no advance occurs.
     *
     * The arguments to this method provide the state of the phaser prevailing for the current transition.
     * The effects of invoking arrival, registration,
     * and waiting methods on this phaser from within {@code onAdvance} are unspecified and should not be relied on.
     *
     * If this phaser is a member of a tiered set of phasers,
     * then{@code onAdvance} is invoked only for its root phaser on each advance.
     *
     * To support the most common use cases, the default implementation of this method returns {@code true}
     * when the number of registered parties has become zero as the result of a party invoking {@code arriveAndDeregister}.
     * You can disable this behavior, thus enabling continuation upon future registrations,
     * by overriding this method to always return {@code false}:
     *
     * <pre> {@code
     * Phaser phaser = new Phaser() {
     *   protected boolean onAdvance(int phase, int parties) { return false; }
     * }}</pre>
     *
     * @param phase             the current phase number on entry to this method,
     *                          before this phaser is advanced
     * @param registeredParties the current number of registered parties
     *
     * @return {@code true} if this phaser should terminate
     */
    /**
     * 所有信号到达某个屏障时的回调
     * phase             - Phaser所处的阶段
     * registeredParties - Phaser中当前注册的所有信号数量（包括初始信号数量）
     *
     * 返回true表示需要结束屏障（Phaser不再发挥拦截作用）
     */
    protected boolean onAdvance(int phase, int registeredParties) {
        return registeredParties == 0;
    }
    
    /*▲ 回调 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // The following unpacking methods are usually manually inlined
    
    /**
     * Resolves lagged phase propagation from root if necessary.
     * Reconciliation normally occurs when root has advanced but subphasers have not yet done so,
     * in which case they must finish their own advance by setting unarrived to parties
     * (or if parties is zero, resetting to unregistered EMPTY state).
     *
     * @return reconciled state
     */
    // 将当前Phaser所处的phase与root-Phaser所处的phase比对同步后，返回当前Phaser的state信息
    private long reconcileState() {
        final Phaser root = this.root;
        
        // 当前Phaser的状态
        long s = state;
        
        if(root != this) {
            while(true) {
                // 获取root-Phaser所处的阶段
                int phase = (int) (root.state >>> PHASE_SHIFT);
                
                // 如果当前Phaser跟root-Phaser所处的阶段一致，则结束循环
                if(phase==(int) (s >>> PHASE_SHIFT)){
                    break;
                }
                
                long sta;
                
                // 如果root-Phaser已经处于终止状态
                if(phase<0){
                    sta = ((long) phase << PHASE_SHIFT) | (s & COUNTS_MASK);
                } else {
                    // 当前Phaser中parties数量
                    int p = (int) s >>> PARTIES_SHIFT;
                    
                    // 初始化parties以及unarrived parties
                    long p_up = (p==0) ? EMPTY : ((s & PARTIES_MASK) | p);
                    
                    sta = ((long) phase << PHASE_SHIFT) | (p_up);
                }
                
                // 使用root-Phaser的phase跟当前Phaser的parties以及unarrived parties信息更新state
                if(STATE.weakCompareAndSet(this, s, s=sta)){
                    break;
                }
                
                s = state;
            }
            
            
            // CAS to root phase with current parties, tripping unarrived
            //            while((phase = (int) (root.state >>> PHASE_SHIFT)) != (int) (s >>> PHASE_SHIFT)
            //                && !STATE.weakCompareAndSet(this, s, s = (((long) phase << PHASE_SHIFT) | ((phase<0) ? (s & COUNTS_MASK) : (((p = (int) s >>> PARTIES_SHIFT) == 0) ? EMPTY : ((s & PARTIES_MASK) | p)))))) {
            //                s = state;
            //            }
        }
        
        return s;
    }
    
    // 获取已到达屏障的信号数量
    private static int arrivedOf(long s) {
        int counts = (int)s;
        return (counts == EMPTY) ? 0 : (counts >>> PARTIES_SHIFT) - (counts & UNARRIVED_MASK);
    }
    
    // 获取未到达屏障的信号数量
    private static int unarrivedOf(long s) {
        int counts = (int)s;
        return (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);
    }
    
    // 获取parties数量
    private static int partiesOf(long s) {
        return (int)s >>> PARTIES_SHIFT;
    }
    
    // 获取Phaser所处的阶段
    private static int phaseOf(long s) {
        return (int)(s >>> PHASE_SHIFT);
    }
    
    /**
     * Implementation of toString and string-based error messages.
     */
    private String stateToString(long s) {
        return super.toString() + "[phase = " + phaseOf(s) + " parties = " + partiesOf(s) + " arrived = " + arrivedOf(s) + "]";
    }
    
    /**
     * Returns message string for bounds exceptions on arrival.
     */
    private String badArrive(long s) {
        return "Attempted arrival of unregistered party for " + stateToString(s);
    }
    
    /**
     * Returns message string for bounds exceptions on registration.
     */
    private String badRegister(long s) {
        return "Attempt to register more than " + MAX_PARTIES + " parties for " + stateToString(s);
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns a string identifying this phaser, as well as its
     * state.  The state, in brackets, includes the String {@code
     * "phase = "} followed by the phase number, {@code "parties = "}
     * followed by the number of registered parties, and {@code
     * "arrived = "} followed by the number of arrived parties.
     *
     * @return a string identifying this phaser, as well as its state
     */
    public String toString() {
        return stateToString(reconcileState());
    }
    
    
    
    /**
     * Wait nodes for Treiber stack representing wait queue.
     */
    // 阻塞结点，存储阻塞线程及其他相关信息，以便对线程进行阻塞/唤醒操作
    static final class QNode implements ForkJoinPool.ManagedBlocker {
        final Phaser phaser;
        final int phase;                // 当前阻塞结点所处的阶段
        final boolean interruptible;    // 是否处理中断
        final boolean timed;            // 是否处理超时
        long nanos;                     // 超时时长
        
        final long deadline;            // 超时截止时间
        volatile Thread thread;         // 阻塞线程（nulled to cancel wait）
        
        boolean wasInterrupted;         // 阻塞线程是否已中断（带有中断标记）
        
        QNode next; // 链接到下一个（更早入队的）QNode
        
        QNode(Phaser phaser, int phase, boolean interruptible, boolean timed, long nanos) {
            this.phaser = phaser;
            this.phase = phase;
            this.interruptible = interruptible;
            this.nanos = nanos;
            this.timed = timed;
            this.deadline = timed ? System.nanoTime() + nanos : 0L;
            thread = Thread.currentThread();
        }
        
        // 当前线程是否需要解除阻塞
        public boolean isReleasable() {
            // 没有需要阻塞的线程
            if(thread == null) {
                return true;
            }
            
            // Phaser所处的阶段发生了改变
            if(phaser.getPhase() != phase) {
                thread = null;
                return true;
            }
            
            // 测试当前线程是否已经中断，线程的中断状态会被清除
            if(Thread.interrupted()) {
                wasInterrupted = true;
            }
            
            // 如果当前线程已被中断，且需要处理中断，则当前线程需要解除阻塞
            if(wasInterrupted && interruptible) {
                thread = null;
                return true;
            }
            
            // 开启了超时，且已经超时
            if(timed && (nanos<=0L || (nanos = deadline - System.nanoTime())<=0L)) {
                thread = null;
                return true;
            }
            
            return false;
        }
        
        // 阻塞当前线程，返回true代表阻塞结束
        public boolean block() {
            while(!isReleasable()) {
                if(timed) {
                    // 带有超时的阻塞
                    LockSupport.parkNanos(this, nanos);
                } else {
                    // 阻塞
                    LockSupport.park(this);
                }
            }
            
            return true;
        }
    }
    
}
