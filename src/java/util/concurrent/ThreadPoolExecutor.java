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

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link ExecutorService} that executes each submitted task using
 * one of possibly several pooled threads, normally configured
 * using {@link Executors} factory methods.
 *
 * <p>Thread pools address two different problems: they usually
 * provide improved performance when executing large numbers of
 * asynchronous tasks, due to reduced per-task invocation overhead,
 * and they provide a means of bounding and managing the resources,
 * including threads, consumed when executing a collection of tasks.
 * Each {@code ThreadPoolExecutor} also maintains some basic
 * statistics, such as the number of completed tasks.
 *
 * <p>To be useful across a wide range of contexts, this class
 * provides many adjustable parameters and extensibility
 * hooks. However, programmers are urged to use the more convenient
 * {@link Executors} factory methods {@link
 * Executors#newCachedThreadPool} (unbounded thread pool, with
 * automatic thread reclamation), {@link Executors#newFixedThreadPool}
 * (fixed size thread pool) and {@link
 * Executors#newSingleThreadExecutor} (single background thread), that
 * preconfigure settings for the most common usage
 * scenarios. Otherwise, use the following guide when manually
 * configuring and tuning this class:
 *
 * <dl>
 *
 * <dt>Core and maximum pool sizes</dt>
 *
 * <dd>A {@code ThreadPoolExecutor} will automatically adjust the
 * pool size (see {@link #getPoolSize})
 * according to the bounds set by
 * corePoolSize (see {@link #getCorePoolSize}) and
 * maximumPoolSize (see {@link #getMaximumPoolSize}).
 *
 * When a new task is submitted in method {@link #execute(Runnable)},
 * if fewer than corePoolSize threads are running, a new thread is
 * created to handle the request, even if other worker threads are
 * idle.  Else if fewer than maximumPoolSize threads are running, a
 * new thread will be created to handle the request only if the queue
 * is full.  By setting corePoolSize and maximumPoolSize the same, you
 * create a fixed-size thread pool. By setting maximumPoolSize to an
 * essentially unbounded value such as {@code Integer.MAX_VALUE}, you
 * allow the pool to accommodate an arbitrary number of concurrent
 * tasks. Most typically, core and maximum pool sizes are set only
 * upon construction, but they may also be changed dynamically using
 * {@link #setCorePoolSize} and {@link #setMaximumPoolSize}. </dd>
 *
 * <dt>On-demand construction</dt>
 *
 * <dd>By default, even core threads are initially created and
 * started only when new tasks arrive, but this can be overridden
 * dynamically using method {@link #prestartCoreThread} or {@link
 * #prestartAllCoreThreads}.  You probably want to prestart threads if
 * you construct the pool with a non-empty queue. </dd>
 *
 * <dt>Creating new threads</dt>
 *
 * <dd>New threads are created using a {@link ThreadFactory}.  If not
 * otherwise specified, a {@link Executors#defaultThreadFactory} is
 * used, that creates threads to all be in the same {@link
 * ThreadGroup} and with the same {@code NORM_PRIORITY} priority and
 * non-daemon status. By supplying a different ThreadFactory, you can
 * alter the thread's name, thread group, priority, daemon status,
 * etc. If a {@code ThreadFactory} fails to create a thread when asked
 * by returning null from {@code newThread}, the executor will
 * continue, but might not be able to execute any tasks. Threads
 * should possess the "modifyThread" {@code RuntimePermission}. If
 * worker threads or other threads using the pool do not possess this
 * permission, service may be degraded: configuration changes may not
 * take effect in a timely manner, and a shutdown pool may remain in a
 * state in which termination is possible but not completed.</dd>
 *
 * <dt>Keep-alive times</dt>
 *
 * <dd>If the pool currently has more than corePoolSize threads,
 * excess threads will be terminated if they have been idle for more
 * than the keepAliveTime (see {@link #getKeepAliveTime(TimeUnit)}).
 * This provides a means of reducing resource consumption when the
 * pool is not being actively used. If the pool becomes more active
 * later, new threads will be constructed. This parameter can also be
 * changed dynamically using method {@link #setKeepAliveTime(long,
 * TimeUnit)}.  Using a value of {@code Long.MAX_VALUE} {@link
 * TimeUnit#NANOSECONDS} effectively disables idle threads from ever
 * terminating prior to shut down. By default, the keep-alive policy
 * applies only when there are more than corePoolSize threads, but
 * method {@link #allowCoreThreadTimeOut(boolean)} can be used to
 * apply this time-out policy to core threads as well, so long as the
 * keepAliveTime value is non-zero. </dd>
 *
 * <dt>Queuing</dt>
 *
 * <dd>Any {@link BlockingQueue} may be used to transfer and hold
 * submitted tasks.  The use of this queue interacts with pool sizing:
 *
 * <ul>
 *
 * <li>If fewer than corePoolSize threads are running, the Executor
 * always prefers adding a new thread
 * rather than queuing.
 *
 * <li>If corePoolSize or more threads are running, the Executor
 * always prefers queuing a request rather than adding a new
 * thread.
 *
 * <li>If a request cannot be queued, a new thread is created unless
 * this would exceed maximumPoolSize, in which case, the task will be
 * rejected.
 *
 * </ul>
 *
 * There are three general strategies for queuing:
 * <ol>
 *
 * <li><em> Direct handoffs.</em> A good default choice for a work
 * queue is a {@link SynchronousQueue} that hands off tasks to threads
 * without otherwise holding them. Here, an attempt to queue a task
 * will fail if no threads are immediately available to run it, so a
 * new thread will be constructed. This policy avoids lockups when
 * handling sets of requests that might have internal dependencies.
 * Direct handoffs generally require unbounded maximumPoolSizes to
 * avoid rejection of new submitted tasks. This in turn admits the
 * possibility of unbounded thread growth when commands continue to
 * arrive on average faster than they can be processed.
 *
 * <li><em> Unbounded queues.</em> Using an unbounded queue (for
 * example a {@link LinkedBlockingQueue} without a predefined
 * capacity) will cause new tasks to wait in the queue when all
 * corePoolSize threads are busy. Thus, no more than corePoolSize
 * threads will ever be created. (And the value of the maximumPoolSize
 * therefore doesn't have any effect.)  This may be appropriate when
 * each task is completely independent of others, so tasks cannot
 * affect each others execution; for example, in a web page server.
 * While this style of queuing can be useful in smoothing out
 * transient bursts of requests, it admits the possibility of
 * unbounded work queue growth when commands continue to arrive on
 * average faster than they can be processed.
 *
 * <li><em>Bounded queues.</em> A bounded queue (for example, an
 * {@link ArrayBlockingQueue}) helps prevent resource exhaustion when
 * used with finite maximumPoolSizes, but can be more difficult to
 * tune and control.  Queue sizes and maximum pool sizes may be traded
 * off for each other: Using large queues and small pools minimizes
 * CPU usage, OS resources, and context-switching overhead, but can
 * lead to artificially low throughput.  If tasks frequently block (for
 * example if they are I/O bound), a system may be able to schedule
 * time for more threads than you otherwise allow. Use of small queues
 * generally requires larger pool sizes, which keeps CPUs busier but
 * may encounter unacceptable scheduling overhead, which also
 * decreases throughput.
 *
 * </ol>
 *
 * </dd>
 *
 * <dt>Rejected tasks</dt>
 *
 * <dd>New tasks submitted in method {@link #execute(Runnable)} will be
 * <em>rejected</em> when the Executor has been shut down, and also when
 * the Executor uses finite bounds for both maximum threads and work queue
 * capacity, and is saturated.  In either case, the {@code execute} method
 * invokes the {@link
 * RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)}
 * method of its {@link RejectedExecutionHandler}.  Four predefined handler
 * policies are provided:
 *
 * <ol>
 *
 * <li>In the default {@link ThreadPoolExecutor.AbortPolicy}, the handler
 * throws a runtime {@link RejectedExecutionException} upon rejection.
 *
 * <li>In {@link ThreadPoolExecutor.CallerRunsPolicy}, the thread
 * that invokes {@code execute} itself runs the task. This provides a
 * simple feedback control mechanism that will slow down the rate that
 * new tasks are submitted.
 *
 * <li>In {@link ThreadPoolExecutor.DiscardPolicy}, a task that
 * cannot be executed is simply dropped.
 *
 * <li>In {@link ThreadPoolExecutor.DiscardOldestPolicy}, if the
 * executor is not shut down, the task at the head of the work queue
 * is dropped, and then execution is retried (which can fail again,
 * causing this to be repeated.)
 *
 * </ol>
 *
 * It is possible to define and use other kinds of {@link
 * RejectedExecutionHandler} classes. Doing so requires some care
 * especially when policies are designed to work only under particular
 * capacity or queuing policies. </dd>
 *
 * <dt>Hook methods</dt>
 *
 * <dd>This class provides {@code protected} overridable
 * {@link #beforeExecute(Thread, Runnable)} and
 * {@link #afterExecute(Runnable, Throwable)} methods that are called
 * before and after execution of each task.  These can be used to
 * manipulate the execution environment; for example, reinitializing
 * ThreadLocals, gathering statistics, or adding log entries.
 * Additionally, method {@link #terminated} can be overridden to perform
 * any special processing that needs to be done once the Executor has
 * fully terminated.
 *
 * <p>If hook, callback, or BlockingQueue methods throw exceptions,
 * internal worker threads may in turn fail, abruptly terminate, and
 * possibly be replaced.</dd>
 *
 * <dt>Queue maintenance</dt>
 *
 * <dd>Method {@link #getQueue()} allows access to the work queue
 * for purposes of monitoring and debugging.  Use of this method for
 * any other purpose is strongly discouraged.  Two supplied methods,
 * {@link #remove(Runnable)} and {@link #purge} are available to
 * assist in storage reclamation when large numbers of queued tasks
 * become cancelled.</dd>
 *
 * <dt>Reclamation</dt>
 *
 * <dd>A pool that is no longer referenced in a program <em>AND</em>
 * has no remaining threads may be reclaimed (garbage collected)
 * without being explicitly shutdown. You can configure a pool to
 * allow all unused threads to eventually die by setting appropriate
 * keep-alive times, using a lower bound of zero core threads and/or
 * setting {@link #allowCoreThreadTimeOut(boolean)}.  </dd>
 *
 * </dl>
 *
 * <p><b>Extension example</b>. Most extensions of this class
 * override one or more of the protected hook methods. For example,
 * here is a subclass that adds a simple pause/resume feature:
 *
 * <pre> {@code
 * class PausableThreadPoolExecutor extends ThreadPoolExecutor {
 *   private boolean isPaused;
 *   private ReentrantLock pauseLock = new ReentrantLock();
 *   private Condition unpaused = pauseLock.newCondition();
 *
 *   public PausableThreadPoolExecutor(...) { super(...); }
 *
 *   protected void beforeExecute(Thread t, Runnable r) {
 *     super.beforeExecute(t, r);
 *     pauseLock.lock();
 *     try {
 *       while (isPaused) unpaused.await();
 *     } catch (InterruptedException ie) {
 *       t.interrupt();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void pause() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = true;
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void resume() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = false;
 *       unpaused.signalAll();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
/*
 * ThreadPoolExecutor通常被简称为【线程池】，虽然与ForkJoinPool是兄弟关系，但两者的侧重点完全不同
 *
 * ThreadPoolExecutor由两部分组成：线程池(pool)与阻塞队列(queue)
 * 线程池(workers)中存储Worker，一个Worker兼具【任务】与【线程】的含义
 * 阻塞队列(workQueue)中存储排队的【任务】
 *
 * Worker可分为两类：【R】型Worker和【N】型Worker
 * 【R】型Worker携带了【任务】，它被执行完后，转为【N】型Worker。
 * 【N】型Worker自身不携带【任务】，它专门负责从阻塞队列中取出任务以便执行。
 *
 * 【R】型线程：【R】型Worker中的线程域，一对一关系
 * 【N】型线程：【N】型Worker中的线程域，一对一关系
 *
 * ★★★ 1. 在不产生歧义的情形下，下面可能会混用【Worker】与【线程】两词
 * ★★★ 2. 在前提1下，这里将：
 *          【R】型Worker或【R】型线程简称做【R】
 *          【N】型Worker或【N】型线程简称做【N】
 *
 * 线程池状态：
 * -1 【运行】RUNNING   :   接收新线程，并可以处理阻塞任务
 *  0 【关闭】SHUTDOWN  : 不接收新线程，但可以处理阻塞任务，
 *  1 【停止】STOP      : 不接收新线程，不可以处理阻塞任务，且对正在执行的线程设置中断标记
 *  2 【完结】TIDYING   : 线程池空闲，阻塞队列为空，在调用terminated()后转入TERMINATED状态
 *  3 【终止】TERMINATED: terminated()方法已执行完
 *
 * 状态转换：
 *            shutdown()              线程池空闲，阻塞队列为空
 *           ┌─────────────▶ SHUTDOWN ────────────┐
 *           │                  │                 │            terminated()之后
 * RUNNING ──┤                  │ shutdownNow()   ├──▶ TIDYING ───────────────▶ TERMINATED
 *           │                  ▼                 │
 *           └─────────────▶   STOP   ────────────┘
 *            shutdownNow()           线程池空闲
 *
 *
 */
public class ThreadPoolExecutor extends AbstractExecutorService {
    /*
     * The main pool control state, ctl, is an atomic integer packing
     * two conceptual fields
     *   workerCount, indicating the effective number of threads
     *   runState,    indicating whether running, shutting down etc
     *
     * In order to pack them into one int, we limit workerCount to
     * (2^29)-1 (about 500 million) threads rather than (2^31)-1 (2
     * billion) otherwise representable. If this is ever an issue in
     * the future, the variable can be changed to be an AtomicLong,
     * and the shift/mask constants below adjusted. But until the need
     * arises, this code is a bit faster and simpler using an int.
     *
     * The workerCount is the number of workers that have been
     * permitted to start and not permitted to stop.  The value may be
     * transiently different from the actual number of live threads,
     * for example when a ThreadFactory fails to create a thread when
     * asked, and when exiting threads are still performing
     * bookkeeping before terminating. The user-visible pool size is
     * reported as the current size of the workers set.
     *
     * The runState provides the main lifecycle control, taking on values:
     *
     *   RUNNING:  Accept new tasks and process queued tasks
     *   SHUTDOWN: Don't accept new tasks, but process queued tasks
     *   STOP:     Don't accept new tasks, don't process queued tasks,
     *             and interrupt in-progress tasks
     *   TIDYING:  All tasks have terminated, workerCount is zero,
     *             the thread transitioning to state TIDYING
     *             will run the terminated() hook method
     *   TERMINATED: terminated() has completed
     *
     * The numerical order among these values matters, to allow
     * ordered comparisons. The runState monotonically increases over
     * time, but need not hit each state. The transitions are:
     *
     * RUNNING -> SHUTDOWN
     *    On invocation of shutdown()
     * (RUNNING or SHUTDOWN) -> STOP
     *    On invocation of shutdownNow()
     * SHUTDOWN -> TIDYING
     *    When both queue and pool are empty
     * STOP -> TIDYING
     *    When pool is empty
     * TIDYING -> TERMINATED
     *    When the terminated() hook method has completed
     *
     * Threads waiting in awaitTermination() will return when the
     * state reaches TERMINATED.
     *
     * Detecting the transition from SHUTDOWN to TIDYING is less
     * straightforward than you'd like because the queue may become
     * empty after non-empty and vice versa during SHUTDOWN state, but
     * we can only terminate if, after seeing that it is empty, we see
     * that workerCount is 0 (which sometimes entails a recheck -- see
     * below).
     */
    
    // 线程池状态标记，运行状态初始为RUNNING，工作线程数量初始为0
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    
    /* runState is stored in the high-order bits */
    
    // 线程池状态标记掩码
    private static final int COUNT_BITS = Integer.SIZE - 3;      // 29
    private static final int COUNT_MASK = (1 << COUNT_BITS) - 1; // 0b-0x 0001-FFF FFFF‬
    
    // 由线程池状态标记的前3个比特位记录线程池的运行状态
    private static final int RUNNING    = -1 << COUNT_BITS; // 0b-0x 1110-000 0000，【运行】
    private static final int SHUTDOWN   =  0 << COUNT_BITS; // 0b-0x 0000-000 0000，【关闭】
    private static final int STOP       =  1 << COUNT_BITS; // 0b-0x 0010-000 0000，【停止】
    private static final int TIDYING    =  2 << COUNT_BITS; // 0b-0x 0100-000 0000，【完结】
    private static final int TERMINATED =  3 << COUNT_BITS; // 0b-0x 0110-000 0000，【终止】
    
    private static final boolean ONLY_ONE = true;
    
    /**
     * Lock held on access to workers set and related bookkeeping.
     * While we could use a concurrent set of some sort, it turns out
     * to be generally preferable to use a lock. Among the reasons is
     * that this serializes interruptIdleWorkers, which avoids
     * unnecessary interrupt storms, especially during shutdown.
     * Otherwise exiting threads would concurrently interrupt those
     * that have not yet interrupted. It also simplifies some of the
     * associated statistics bookkeeping of largestPoolSize etc. We
     * also hold mainLock on shutdown and shutdownNow, for the sake of
     * ensuring workers set is stable while separately checking
     * permission to interrupt and actually interrupting.
     */
    // 线程池锁，线程池状态标记发生变化时使用
    private final ReentrantLock mainLock = new ReentrantLock();
    
    /**
     * Wait condition to support awaitTermination.
     */
    // 线程池{条件对象}
    private final Condition termination = mainLock.newCondition();
    
    /**
     * Set containing all worker threads in pool. Accessed only when holding mainLock.
     */
    // 线程池，存储【  核心Worker】与【非核心Worker】
    private final HashSet<Worker> workerPool = new HashSet<>();
    
    /**
     * The queue used for holding tasks and handing off to worker
     * threads.  We do not require that workQueue.poll() returning
     * null necessarily means that workQueue.isEmpty(), so rely
     * solely on isEmpty to see if the queue is empty (which we must
     * do for example when deciding whether to transition from
     * SHUTDOWN to TIDYING).  This accommodates special-purpose
     * queues such as DelayQueues for which poll() is allowed to
     * return null even if it may later return non-null when delays
     * expire.
     */
    // 阻塞队列，存储阻塞任务
    private final BlockingQueue<Runnable> workQueue;
    
    /*
     * All user control parameters are declared as volatiles so that
     * ongoing actions are based on freshest values, but without need
     * for locking, since no internal invariants depend on them
     * changing synchronously with respect to other actions.
     */
    
    /**
     * Factory for new threads. All threads are created using this
     * factory (via method addWorker).  All callers must be prepared
     * for addWorker to fail, which may reflect a system or user's
     * policy limiting the number of threads.  Even though it is not
     * treated as an error, failure to create threads may result in
     * new tasks being rejected or existing ones remaining stuck in
     * the queue.
     *
     * We go further and preserve pool invariants even in the face of
     * errors such as OutOfMemoryError, that might be thrown while
     * trying to create threads.  Such errors are rather common due to
     * the need to allocate a native stack in Thread.start, and users
     * will want to perform clean pool shutdown to clean up.  There
     * will likely be enough memory available for the cleanup code to
     * complete without encountering yet another OutOfMemoryError.
     */
    // 线程工厂，用来构造执行【任务】的线程
    private volatile ThreadFactory threadFactory;
    
    /**
     * Handler called when saturated or shutdown in execute.
     */
    // 【拒绝策略】处理器
    private volatile RejectedExecutionHandler handler;
    
    /**
     * If false (default), core threads stay alive even when idle.
     * If true, core threads use keepAliveTime to time out waiting for work.
     */
    // 是否主动启用了超时设置
    private volatile boolean allowCoreThreadTimeOut;
    
    /**
     * Timeout in nanoseconds for idle threads waiting for work.
     * Threads use this timeout when there are more than corePoolSize
     * present or if allowCoreThreadTimeOut. Otherwise they wait
     * forever for new work.
     */
    // 【N】型Worker的最大空闲时间（启用了超时设置后生效）
    private volatile long keepAliveTime;
    
    /**
     * Core pool size is the minimum number of workers to keep alive
     * (and not allow to time out etc) unless allowCoreThreadTimeOut
     * is set, in which case the minimum is zero.
     *
     * Since the worker count is actually stored in COUNT_BITS bits,
     * the effective limit is {@code corePoolSize & COUNT_MASK}.
     */
    // 线程池【核心阈值】，>=0，一般用来限制【R】的数量
    private volatile int corePoolSize;
    
    /**
     * Maximum pool size.
     *
     * Since the worker count is actually stored in COUNT_BITS bits,
     * the effective limit is {@code maximumPoolSize & COUNT_MASK}.
     */
    // 线程池【最大阈值】，>0，一般用来限制【N】的数量（线程池满，阻塞队列也满时，限制【R】的数量）
    private volatile int maximumPoolSize;
    
    /**
     * Tracks largest attained poize. Accessed onol sly under mainLock.
     */
    // 记录线程池中Worker数量达到的最大值
    private int largestPoolSize;
    
    /**
     * Counter for completed tasks. Updated only on termination of worker threads. Accessed only under mainLock.
     */
    // 记录线程池累计执行的任务数量（每个Worker退出时都会累加一下该Worker执行过的任务数量）
    private long completedTaskCount;
    
    /**
     * The default rejected execution handler.
     */
    // 默认【阻塞策略】，其行为是丢弃任务，且抛出异常
    private static final RejectedExecutionHandler defaultHandler = new AbortPolicy();
    
    /**
     * Permission required for callers of shutdown and shutdownNow.
     * We additionally require (see checkShutdownAccess) that callers
     * have permission to actually interrupt threads in the worker set
     * (as governed by Thread.interrupt, which relies on
     * ThreadGroup.checkAccess, which in turn relies on
     * SecurityManager.checkAccess). Shutdowns are attempted only if
     * these checks pass.
     *
     * All actual invocations of Thread.interrupt (see
     * interruptIdleWorkers and interruptWorkers) ignore
     * SecurityExceptions, meaning that the attempted interrupts
     * silently fail. In the case of shutdown, they should not fail
     * unless the SecurityManager has inconsistent policies, sometimes
     * allowing access to a thread and sometimes not. In such cases,
     * failure to actually interrupt threads may disable or delay full
     * termination. Other uses of interruptIdleWorkers are advisory,
     * and failure to actually interrupt will merely delay response to
     * configuration changes so is not handled exceptionally.
     */
    // 关于【关闭】线程池的权限
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters, the default thread factory and the default rejected
     * execution handler.
     *
     * <p>It may be more convenient to use one of the {@link Executors}
     * factory methods instead of this general purpose constructor.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), defaultHandler);
    }
    
    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and {@linkplain ThreadPoolExecutor.AbortPolicy
     * default rejected execution handler}.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
    }
    
    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and
     * {@linkplain Executors#defaultThreadFactory default thread factory}.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), handler);
    }
    
    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} or {@code handler} is null
     */
    /*
     * corePoolSize   ：线程池【核心阈值】，>=0，一般用来限制【R】的数量
     * maximumPoolSize：线程池【最大阈值】，>0， 一般用来限制【N】的数量（线程池满，阻塞队列也满时，限制【R】的数量）
     * keepAliveTime  ：【N】的最大空闲时间（启用了超时设置后生效）
     * unit           ：时间单位
     * workQueue      ：阻塞队列
     * threadFactory  ：线程工厂
     * handler        ：拒绝策略
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
            maximumPoolSize <= 0 ||
            maximumPoolSize < corePoolSize ||
            keepAliveTime < 0) {
            throw new IllegalArgumentException();
        }
        
        if (workQueue == null || threadFactory == null || handler == null) {
            throw new NullPointerException();
        }
        
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建/执行/清理任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@link RejectedExecutionHandler}.
     *
     * @param command the task to execute
     *
     * @throws RejectedExecutionException at discretion of
     *                                    {@code RejectedExecutionHandler}, if the task
     *                                    cannot be accepted for execution
     * @throws NullPointerException       if {@code command} is null
     */
    // 执行给定的任务（视情形将其封装到线程池，或放入阻塞队列排队）
    public void execute(Runnable command) {
        /*
         * Proceed in 3 steps:
         *
         * 1. If fewer than corePoolSize threads are running,
         * try to start a new thread with the given command as its first task.
         * The call to addWorker atomically checks runState and workerCount,
         * and so prevents false alarms that would add threads when it shouldn't,
         * by returning false.
         *
         * 2. If a task can be successfully queued,
         * then we still need to double-check whether we should have added a thread
         * (because existing ones died since last checking) or that
         * the pool shut down since entry into this method.
         * So we recheck state and if necessary roll back the enqueuing if stopped,
         * or start a new thread if there are none.
         *
         * 3. If we cannot queue task, then we try to add a new thread.
         * If it fails, we know we are shut down or saturated and so reject the task.
         */
    
        // 不能执行空任务
        if(command == null) {
            throw new NullPointerException();
        }
    
        // 获取线程池状态标记
        int state = ctl.get();
    
        // 线程池的Worker数量在【核心阈值】以内
        if(workerCountOf(state)<corePoolSize) {
        
            // 向线程池内添加并启动一个【R】
            if(addWorker(command, true)) {
                // 执行成功，直接返回
                return;
            }
        
            state = ctl.get();
        }
    
        /*
         * 至此，有三种情形：
         * 1.线程池中的Worker数量超过了【核心阈值】（有些线程池将【核心阈值】设为0）
         * 2.【R】添加失败（由于线程争用，线程池状态发生了变化）
         * 3.【R】启动失败（少见）
         *
         * 此时，会尝试将任务添加到阻塞队列
         */
    
        // {-1} 如果线程池仍处于【运行】状态（可以接收新线程），且任务可以被成功添加到阻塞队列
        if(isRunning(state) && workQueue.offer(command)) {
        
            /*
             * 至此，任务成功进入了阻塞队列
             *
             * 为了执行阻塞队列中这个任务，
             * 于是，此时，尝试向线程池中添加一个【N】，
             * 目的是借助【N】间接执行阻塞队列中的任务
             *
             * 当然，这里如果没能成功添加【N】，也不要紧，
             * 因为稍后【R】执行完后，就会转变为【N】
             */
            
            // 再次检查线程池标记
            int recheck = ctl.get();
            
            // {0123} 如果线程池已结束【运行】状态，这意味着线程池不再接收新线程了
            if(!isRunning(recheck)) {
                // 赶紧从阻塞队列中移除刚刚添加的任务
                if(remove(command)) {
                    // 任务没能保存到阻塞队列，添加失败，执行【拒绝策略】
                    reject(command);
                }
            } else {
                /* {-1} 至此，说明线程池仍在【运行】状态 */
    
                // 如果线程池为空（通常出现在【核心阈值】为0的场景）
                if(workerCountOf(recheck) == 0) {
                    // 向线程池添加一个【N】，以便处理阻塞队列中的任务
                    addWorker(null, false);
                }
                
                /* 不为null的话不用管，因为自然有别的线程去处理阻塞队列的任务 */
            }
        } else {
            /*
             * 至此，有两种情形：
             * 1. {0123} 线程池已结束【运行】状态，这意味程池不再接收新的Worker了
             * 2. {-1}   线程池正处于【运行】状态，但任务没能成功进入阻塞队列（阻塞队列满了）
             *
             * 情形1：
             * 这种情形下，addWorker(command, false)往往返回false，
             * 即任务准备进入阻塞队列时，而且恰好线程池也已关闭，那么需要执行拒绝策略
             *
             * 情形2：
             * 这种情形下，可能是阻塞队列满了，此时改用【最大阈值】最为线程池的容量限制，
             * 重新尝试将任务包装为Worker放入了线程池，如果还是失败，则返回false
             * 比如【核心阈值】与【最大阈值】一样大时，这里往往是失败的
             */
            if(!addWorker(command, false)) {
                // 任务进入线程池失败，执行【拒绝策略】
                reject(command);
            }
        }
    }
    
    /**
     * Checks if a new worker can be added with respect to current
     * pool state and the given bound (either core or maximum). If so,
     * the worker count is adjusted accordingly, and, if possible, a
     * new worker is created and started, running firstTask as its
     * first task. This method returns false if the pool is stopped or
     * eligible to shut down. It also returns false if the thread
     * factory fails to create a thread when asked.  If the thread
     * creation fails, either due to the thread factory returning
     * null, or due to an exception (typically OutOfMemoryError in
     * Thread.start()), we roll back cleanly.
     *
     * @param firstTask the task the new thread should run first (or
     * null if none). Workers are created with an initial first task
     * (in method execute()) to bypass queuing when there are fewer
     * than corePoolSize threads (in which case we always start one),
     * or when the queue is full (in which case we must bypass queue).
     * Initially idle threads are usually created via
     * prestartCoreThread or to replace other dying workers.
     *
     * @param core if true use corePoolSize as bound, else
     * maximumPoolSize. (A boolean indicator is used here rather than a
     * value to ensure reads of fresh values after checking other pool
     * state).
     *
     * @return true if successful
     */
    /*
     * 将给定任务(如果存在)包装到Worker中，然后将Worker添加到线程池，并启动Worker
     *
     * firstTask为null时添加【N】型Worker，否则，添加【R】型Worker
     * core==true表示使用【核心阈值】，core==false表示使用【最大阈值】
     *
     * 返回true代表Worker被成功添加，而且被成功启动
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
retry:
        for(int state = ctl.get(); ; ) {
            /* Check if queue empty only if necessary */
        
            // {0123} 如果线程池不再接收新线程
            if(runStateAtLeast(state, SHUTDOWN)) {
            
                // {123} 如果线程池不再处理阻塞任务，直接返回
                if(runStateAtLeast(state, STOP)) {
                    return false;
                }
            
                /* {0} 至此，线程池处于【关闭】状态，即线程池不再接收新线程，但可以处理阻塞任务 */
            
                // 如果任务不为空，直接返回（因为不再接收新线程[任务]了）
                if(firstTask != null) {
                    return false;
                }
                
                // 如果阻塞队列为空，直接返回（因为虽然可以处理阻塞任务，但已经没有阻塞任务了）
                if(workQueue.isEmpty()){
                    return false;
                }
            }
        
            /*
             * 至此，线程池有两种状态：
             *
             * 1.线程池处于【运行】状态{-1}，可以接收新线程，并可以处理阻塞任务
             * 2.线程池处于【关闭】状态{0}，但firstTask==null(是【N】型Worker)，而且阻塞队列不为空（有阻塞任务）
             */
            
            // 原子地增加线程池中的Worker数量
            for (;;) {
                // 获取当前情境下需要使用的阈值限制
                int count = (core ? corePoolSize : maximumPoolSize) & COUNT_MASK;
    
                /*
                 * 至此，由于形参的不同组合，出现了4种情形：
                 *
                 * 1.(command, true)  ==> firstTask!=null，count==corePoolSize
                 *   添加【R】到线程池，使用【核心阈值】；
                 *   线程池的Worker数量在【核心阈值】以内，此时可以持续增加【R】型Worker。
                 *
                 * 2.(command, false) ==> firstTask!=null，count==maximumPoolSize
                 *   添加【R】到线程池，使用【最大阈值】；
                 *   线程池正处于【运行】状态，且工作线程数量达到了核心阈值，而且阻塞队列也满了，
                 *   此时无法任务添加到阻塞队列，于是只能将线程池容量阈值扩大为【最大阈值】，
                 *   以便继续添加【R】型Worker去执行该任务。
                 *
                 * 3.(null, false)    ==> firstTask==null，count==maximumPoolSize
                 *   添加【N】到线程池，使用【最大阈值】；
                 *   3.1 用在最低保障启动中，参见ensurePrestart()
                 *   3.2 工作线程被异常结束，或者，工作线程数量已经低于最小预留值的保障，此时也需要新增一个【N】型Worker去处理阻塞队列中的任务
                 *   3.3 线程池为空时(例如核心阈值为0)，此时也需要专门添加一个【N】型Worker去处理阻塞队列中的任务
                 *
                 * 4.(null, true)     ==> firstTask==null，count==corePoolSize
                 *   添加【N】到线程池，使用【核心阈值】；
                 *   4.1 用在最低保障启动中，参见ensurePrestart()
                 *   4.2 用在预启动中，参见prestartAllCoreThreads()和prestartCoreThread()
                 *   4.3 扩大了【核心阈值】，此时也需要补充【N】型Worker，以便与阻塞队列的容量匹配
                 */
    
                // 如果线程池中的工作线程(Worker)数量超过了对应情形下的阈值，则返回fasle，表示无法再添加新的任务
                if(workerCountOf(state) >= count) {
                    return false;
                }
    
                // 原子地递【增】线程池中工作线程(Worker)的数量，并更新线程池状态标记
                if(compareAndIncrementWorkerCount(state)) {
                    // 任务数量更新成功，跳出双重循环
                    break retry;
                }
    
                /* 至此，说明上面的原子操作失败了，可能是发生了线程争用 */
    
                // 如果线程池中的任务数量发生了变化，需要重新获取线程池状态标记
                state = ctl.get();  // Re-read ctl
    
                // {0123} 如果线程池不再接收新线程了
                if(runStateAtLeast(state, SHUTDOWN)) {
                    // 跳到外层循环，尝试处理阻塞队列中的任务
                    continue retry;
                }
    
                /* {-1} 至此，线程池仍处于【运行】状态，重新执行内层循环，以完成刚刚的原子操作 */
    
                /* else CAS failed due to workerCount change; retry inner loop */
            } // for
        } // for
    
    
        /* 至此，说明Worker添加成功 */
        
        boolean workerAdded = false;    // Worker是否添加成功
        boolean workerStarted = false;  // Worker是否成功启动
    
        Worker worker = null;
        
        try {
            // 新建Worker
            worker = new Worker(firstTask);
            
            // 获取线程域
            final Thread workerThread = worker.workerThread;
    
            if(workerThread != null) {
                mainLock.lock();
        
                try {
                    /*
                     * Recheck while holding lock.
                     * Back out on ThreadFactory failure or if shut down before lock acquired.
                     */
                    int state = ctl.get();
            
                    // {-1} 如果线程池仍处于【运行】状态（接收新线程）
                    if(isRunning(state)
                        // {0} 或者，如果线程池处于【关闭】状态（可以处理阻塞任务），且正在添加【N】型Worker
                        || (runStateLessThan(state, STOP) && firstTask == null)) {
                
                        /* precheck that t is startable */
                        // 检查线程是否已经启动
                        if(workerThread.isAlive()) {
                            // 如果线程已经被启动了，则抛出异常
                            throw new IllegalThreadStateException();
                        }
                
                        // 添加新的Worker到线程池
                        workerPool.add(worker);
                        
                        // 更新线程池中Worker数量达到的最大值
                        int s = workerPool.size();
                        if (s > largestPoolSize) {
                            largestPoolSize = s;
                        }
                        
                        // 标记成功添加了一个Worker
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                
                // 如果成功添加了Worker，那么接下来就要启动Worker了
                if (workerAdded) {
                    // 启动Worker所在线程（开始执行任务）
                    workerThread.start();
                    
                    // 标记成功启动了Worker（任务开始了执行）
                    workerStarted = true;
                }
            }
        } finally {
            // 如果Worker启动失败
            if (!workerStarted) {
                // 移除对应的任务，并尝试让线程池进入【终止】状态
                addWorkerFailed(worker);
            }
        }
        
        // 返回值表示Worker是否成功启动
        return workerStarted;
    }
    
    /**
     * Rolls back the worker thread creation.
     * - removes worker from workers, if present
     * - decrements worker count
     * - rechecks for termination, in case the existence of this
     *   worker was holding up termination
     */
    // Worker启动失败后，需要移除该Worker，并尝试让线程池进入【终止】状态
    private void addWorkerFailed(Worker w) {
        mainLock.lock();
        try {
            if (w != null) {
                // 从线程池中移除Worker
                workerPool.remove(w);
            }
    
            // 原子地递【减】线程池中工作线程(Worker)的数量，并更新线程池状态标记
            decrementWorkerCount();
            
            // 尝试让线程池进入【终止】状态
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }
    
    /**
     * Invokes the rejected execution handler for the given command.
     * Package-protected for use by ScheduledThreadPoolExecutor.
     */
    /*
     * 为指定的动作（任务）执行【拒绝策略】
     *
     * 常见触发时机：
     * 1.任务【准备】进入阻塞队列时，恰好发现线程池已关闭
     * 2.任务进入阻塞队列【失败】（线程池仍在运行）
     * 3.任务【已经】进入阻塞队列，但后续发现线程池关闭了，
     *   此时不仅要执行拒绝策略，还要将该任务从阻塞队列中移除
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }
    
    /**
     * Main worker run loop.  Repeatedly gets tasks from queue and
     * executes them, while coping with a number of issues:
     *
     * 1. We may start out with an initial task, in which case we
     * don't need to get the first one. Otherwise, as long as pool is
     * running, we get tasks from getTask. If it returns null then the
     * worker exits due to changed pool state or configuration
     * parameters.  Other exits result from exception throws in
     * external code, in which case completedAbruptly holds, which
     * usually leads processWorkerExit to replace this thread.
     *
     * 2. Before running any task, the lock is acquired to prevent
     * other pool interrupts while the task is executing, and then we
     * ensure that unless pool is stopping, this thread does not have
     * its interrupt set.
     *
     * 3. Each task run is preceded by a call to beforeExecute, which
     * might throw an exception, in which case we cause thread to die
     * (breaking loop with completedAbruptly true) without processing
     * the task.
     *
     * 4. Assuming beforeExecute completes normally, we run the task,
     * gathering any of its thrown exceptions to send to afterExecute.
     * We separately handle RuntimeException, Error (both of which the
     * specs guarantee that we trap) and arbitrary Throwables.
     * Because we cannot rethrow Throwables within Runnable.run, we
     * wrap them within Errors on the way out (to the thread's
     * UncaughtExceptionHandler).  Any thrown exception also
     * conservatively causes thread to die.
     *
     * 5. After task.run completes, we call afterExecute, which may
     * also throw an exception, which will also cause thread to
     * die. According to JLS Sec 14.20, this exception is the one that
     * will be in effect even if task.run throws.
     *
     * The net effect of the exception mechanics is that afterExecute
     * and the thread's UncaughtExceptionHandler have as accurate
     * information as we can provide about any problems encountered by
     * user code.
     *
     * @param w the worker
     */
    /*
     * 由【工作线程】调用，执行线程池中的任务
     *
     * 如果当前Worker是【R】型，则取出【R】型Worker中的任务执行，之后，【R】型Worker转变为【N】型Worker；
     * 如果当前Worker(已经)是【N】型Worker，则需要不断从线程池的阻塞队列中取出待执行任务。
     *
     * 如果未启用超时设置，则没有待执行任务时，该工作线程会被阻塞；
     * 如果超时设置已被激活，那么没有待执行任务时，该工作线程会退出。
     */
    final void runWorker(Worker worker) {
        Thread currentThread = Thread.currentThread();
    
        // 获取Worker中的待执行任务
        Runnable runnableTask = worker.firstTask;
    
        // 置空Worker中的任务(如果该Worker是【R】型Worker，此处相当于将其从【R】型转变为【N】型)
        worker.firstTask = null;
    
        /*
         * 解锁，此处只是简单地将许可证数量置为0
         *
         * 每个Worker的锁状态被初始化为-1，
         * 这里要先将锁状态重置为0，以便可以对Worker正常加锁
         */
        worker.unlock(); // allow interrupts
    
        // 是否异常结束
        boolean completedAbruptly = true;
        
        try {
            while(true) {
                /*
                 * 此处的runnableTask为null有两种情形：
                 * 1.首次进来，且当前Worker是【N】型
                 * 2.再次进来，之前的任务执行完了
                 */
                if(runnableTask == null) {
                    /*
                     * 【N】型Worker尝试从阻塞队列中获取待执行任务
                     *
                     * 注：这里的【N】型Worker可能有两种情形：
                     * 1.待处理worker本来就是【N】型Worker
                     * 2.待处理worker原本是【R】型Worker，但取出其搭载的firstTask任务后，它转变成了【N】型Worker
                     */
                    runnableTask = getTask();
            
                    // 如果没有在阻塞队列中获取到待执行任务(可能是没有更多任务了，也可能是超时了)
                    if(runnableTask == null) {
                        break;
                    }
                }
        
                // 加锁，此处只是简单地将许可证数量从0更新到1
                worker.lock();
        
                /*
                 * If pool is stopping, ensure thread is interrupted;
                 * if not, ensure thread is not interrupted.
                 * This requires a recheck in second case to deal with shutdownNow race while clearing interrupt
                 *
                 * 确保：
                 * 线程池在{-10}状态时，线程未被中断
                 * 线程池在{123}状态时，线程被中断
                 */
        
                // 如果线程池至少已经stop(处于{123}状态)
                if(runStateAtLeast(ctl.get(), STOP)) {
                    // 如果工作线程没有中断标记(该测试不影响线程的中断状态)
                    if(!currentThread.isInterrupted()) {
                        // 需要为工作线程设置一个中断标记
                        currentThread.interrupt();
                    }
            
                    // 如果线程池处于{-10}状态，则需要清除线程的中断状态
                } else {
                    // （静态）测试当前线程是否已经中断，线程的中断状态会被清除
                    Thread.interrupted();
                }
        
                try {
                    // 任务执行前的回调
                    beforeExecute(currentThread, runnableTask);
            
                    try {
                        // 执行【R】中的任务
                        runnableTask.run();
                
                        // 任务执行后的回调
                        afterExecute(runnableTask, null);
                    } catch(Throwable ex) {
                        // 发生异常，任务执行后的回调
                        afterExecute(runnableTask, ex);
                        throw ex;
                    }
                } finally {
                    // 置空变量，以便运载新的任务
                    runnableTask = null;
            
                    // 当前Worker(工作线程)执行的任务数增一
                    worker.completedTasks++;
            
                    // 解锁，此处只是简单地将许可证数量置为0，以便后续可以对其加锁
                    worker.unlock();
                }
            }// while(true)
            
            /* 至此，没有在阻塞队列中取到任务，则【N】型Worker结束运行 */
    
            // 标记线程池正常结束(不再有阻塞的线程)
            completedAbruptly = false;
        } finally {
            /*
             * 无法在阻塞队列中取到任务时，对应的【N】型Worker结束运行
             * 当前Worker退出时，除了需要将该Worker从线程池移除，
             * 还要保证阻塞队列中的阻塞任务后续有别的Worker处理
             * 注：超时被启用时，才会走这里，否则就一直阻塞在getTask()
             */
            processWorkerExit(worker, completedAbruptly);
        }
    }
    
    /**
     * Performs blocking or timed wait for a task, depending on
     * current configuration settings, or returns null if this worker
     * must exit because of any of:
     * 1. There are more than maximumPoolSize workers (due to
     *    a call to setMaximumPoolSize).
     * 2. The pool is stopped.
     * 3. The pool is shutdown and the queue is empty.
     * 4. This worker timed out waiting for a task, and timed-out
     *    workers are subject to termination (that is,
     *    {@code allowCoreThreadTimeOut || workerCount > corePoolSize})
     *    both before and after the timed wait, and if the queue is
     *    non-empty, this worker is not the last thread in the pool.
     *
     * @return task, or null if the worker must exit, in which case
     *         workerCount is decremented
     */
    /*
     * 【N】型Worker通过此方法，从阻塞队列中取出任务，如果没取到，则返回null
     *
     * 当返回值为null时，往往伴随着线程池中Worker数量的递减
     *
     * 如果未启用超时设置，那么该方法会一直阻塞，直到从阻塞队列中取到任务
     * 如果启用了超时设置，则必须在指定时间内取到任务，否则返回null
     */
    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out?
        
        for(; ; ) {
            int state = ctl.get();
    
            /* Check if queue empty only if necessary */
    
            // {0123} 如果线程池结束了【运行】状态，即其不再接收新线程
            if(runStateAtLeast(state, SHUTDOWN)) {
        
                // {123} 如果线程池不可以处理阻塞队列中的阻塞任务了，或者可以处理，但线程池为空
                if(runStateAtLeast(state, STOP) || workQueue.isEmpty()) {
                    // 原子地递【减】线程池中工作线程(Worker)的数量，并更新线程池状态标记
                    decrementWorkerCount();
            
                    // 返回null意味着该【N】型Worker稍后将结束
                    return null;
                }
            }
            
            /* 至此，说明线程池有能力处理阻塞任务，且线程池不为空，那么就继续处理任务 */
            
            // 获取线程池中的Worker数量
            int workerCount = workerCountOf(state);
    
            /* Are workers subject to culling? */
    
            /*
             * 是否启用超时设置
             * 1.主动启用，可以通过设置allowCoreThreadTimeOut==true来实现
             * 2.自动启用，发生在线程池中的Worker数量超过【核心阈值】时
             */
            boolean timed = allowCoreThreadTimeOut || workerCount>corePoolSize;
    
            // 如果线程池中的Worker数量超过【最大阈值】（线程争用严重时会有此种情形）
            if(workerCount>maximumPoolSize
                // 或者启用了超时设置，且已经超时
                || (timed && timedOut)) {
        
                // 如果线程池中至少有一个Worker
                if(workerCount>1
                    // 或者阻塞队列为空
                    || workQueue.isEmpty()) {
            
                    /*
                     * 至此，归纳为两种情形：
                     * 1. 线程池中的Worker数量超过【最大阈值】
                     * 2. 线程池启用了超时设置，并已经超时
                     */
            
            
                    /*
                     * 原子地递【减】线程池中工作线程(Worker)的数量，并更新线程池状态标记
                     *
                     * 递减的原因是Worker数量过多了...
                     */
                    if(compareAndDecrementWorkerCount(state)) {
                        // 返回null意味着该【N】型Worker稍后将结束
                        return null;
                    }
                    
                    continue;
                }
            }
    
            /* 至此，剔除了多余的【N】 */
            
            try {
                Runnable task;
    
                // 如果启用了超时设置
                if(timed) {
                    // 出队，从阻塞队列取出任务，不满足出队条件时阻塞一段时间，如果在指定的时间内没有成功拿到元素，则返回null
                    task = workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS);
                } else {
                    // 出队，从阻塞队列取出任务，线程安全，不满足出队条件时【阻塞】
                    task = workQueue.take();
                }
    
                // 如果成功获取到任务，则返回它
                if(task != null) {
                    return task;
                }
    
                // 至此，说启用了超时设置，且确实已经超时了（【N】过时了）
                timedOut = true;
            } catch(InterruptedException retry) {
                timedOut = false;
            }
            
        } // for(; ; )
    }
    
    /**
     * Performs cleanup and bookkeeping for a dying worker. Called
     * only from worker threads. Unless completedAbruptly is set,
     * assumes that workerCount has already been adjusted to account
     * for exit.  This method removes thread from worker set, and
     * possibly terminates the pool or replaces the worker if either
     * it exited due to user task exception or if fewer than
     * corePoolSize workers are running or queue is non-empty but
     * there are no workers.
     *
     * @param w the worker
     * @param completedAbruptly if the worker died due to user exception
     */
    // 线程池中的某个Worker结束，除了需要将该Worker从线程池移除，还要保证阻塞队列中的阻塞任务后续有人处理
    private void processWorkerExit(Worker worker, boolean completedAbruptly) {
        /* If abrupt, then workerCount wasn't adjusted */
    
        // 如果工作线程是非正常退出的
        if(completedAbruptly) {
            // 原子地递【减】线程池中工作线程(Worker)的数量，并更新线程池状态标记
            decrementWorkerCount();
        }
    
        mainLock.lock();
        try {
            // 记录线程池累计执行的任务数量
            completedTaskCount += worker.completedTasks;
            // 从线程池中移除该Worker
            workerPool.remove(worker);
        } finally {
            mainLock.unlock();
        }
    
        // 尝试让线程池进入【终止】状态
        tryTerminate();
    
        int state = ctl.get();
    
        // {123}如果线程池不能接受新线程，也不能处理阻塞任务，则直接返回
        if(!runStateLessThan(state, STOP)) {
            return;
        }
    
        /* 至此，说明线程池仍然可以处理阻塞队列中的任务 */
    
        // 如果工作线程是正常退出的
        if(!completedAbruptly) {
            // 线程池中计划预留的最小Worker数量
            int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
        
            // 确保预留值最小为1（当然，如果阻塞队列也为空时，就不用预留了）
            if(min == 0 && !workQueue.isEmpty()) {
                min = 1;
            }
        
            /*
             * 如果线程池中的Worker数量超过最小预留值，则可以放心地返回了，
             * 因为不管这个Worker是【R】型还是【N】型，稍后都可以去处理阻塞队列中的任务
             */
            if(workerCountOf(state) >= min) {
                return; // replacement not needed
            }
        }
    
        /*
         * 至此，说明工作线程被异常结束，或者，工作线程数量已经低于最小预留值的保障；
         * 此时需要专门添加一个【N】型Worker到线程池，以便处理阻塞队列中的任务
         */
        addWorker(null, false);
    }
    
    /*▲ 创建/执行/清理任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 运行状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断线程池是否处于{-1}【运行】状态
    private static boolean isRunning(int workerPoolState) {
        return workerPoolState<SHUTDOWN;
    }
    
    // 判断线程池是否至少处于{0123}【关闭】状态
    public boolean isShutdown() {
        return runStateAtLeast(ctl.get(), SHUTDOWN);
    }
    
    /**
     * Returns true if this executor is in the process of terminating
     * after {@link #shutdown} or {@link #shutdownNow} but has not
     * completely terminated.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, causing this executor not
     * to properly terminate.
     *
     * @return {@code true} if terminating but not yet terminated
     */
    // 判断线程池是否至少处于{012}【关闭】状态，但还没有进入【终止】状态
    public boolean isTerminating() {
        int state = ctl.get();
        return runStateAtLeast(state, SHUTDOWN) && runStateLessThan(state, TERMINATED);
    }
    
    /** Used by ScheduledThreadPoolExecutor. */
    // 判断线程池是否至少处于{123}【停止】状态
    boolean isStopped() {
        return runStateAtLeast(ctl.get(), STOP);
    }
    
    // 判断线程池是否处于{3}【终止】状态
    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }
    
    /**
     * Transitions runState to given target, or leaves it alone if
     * already at least the given target.
     *
     * @param targetState the desired state, either SHUTDOWN or STOP
     *                    (but not TIDYING or TERMINATED -- use tryTerminate for that)
     */
    /*
     * 更新线程池的运行状态为>=targetState
     *
     * 如果已经>=targetState，则不更新，否则，更新为==targetState
     */
    private void advanceRunState(int targetState) {
        // assert targetState == SHUTDOWN || targetState == STOP;
        for(; ; ) {
            int c = ctl.get();
            
            // 如果c>=targetState
            if(runStateAtLeast(c, targetState)) {
                // 已经满足条件，结束执行
                break;
            }
            
            // 生成新的线程池状态标记
            int newValue = ctlOf(targetState, workerCountOf(c));
            
            // 更新线程池状态标记
            if(ctl.compareAndSet(c, newValue)){
                break;
            }
        }
    }
    
    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * @throws SecurityException {@inheritDoc}
     */
    /*
     * 先让线程池进入{0}【关闭】状态(直到成功)，再尝试进入{3}【终止】状态(不一定成功)；
     * 关闭过程中会为线程池中所有【空闲】Worker设置中断标记。
     */
    public void shutdown() {
    
        mainLock.lock();
    
        try {
            // 检查【关闭】权限
            checkShutdownAccess();
        
            // 更新线程池的运行状态为【关闭】，即不再接收新线程，但可以处理阻塞任务
            advanceRunState(SHUTDOWN);
            
            // 中断线程池中所有【空闲】的Worker
            interruptIdleWorkers();
            
            // 回调
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        
        // 尝试让线程池进入【终止】状态
        tryTerminate();
    }
    
    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * interrupts tasks via {@link Thread#interrupt}; any task that
     * fails to respond to interrupts may never terminate.
     *
     * @throws SecurityException {@inheritDoc}
     */
    /*
     * 先让线程池进入{1}【停止】状态(直到成功)，再尝试进入{3}【终止】状态(不一定成功)；
     * 关闭过程中会为线程池中所有【空闲】Worker设置中断标记；
     * 返回阻塞队列中未处理的阻塞任务。
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
    
        mainLock.lock();
        try {
            // 检查【关闭】权限
            checkShutdownAccess();
        
            // 更新线程池的运行状态为【停止】，即不再接收新线程，也不再处理阻塞任务（稍后还要中断正在执行的Worker）
            advanceRunState(STOP);
            
            // 中断线程池中正在【执行】的Worker
            interruptWorkers();
            
            // 取出阻塞队列中未处理的阻塞任务
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        
        // 尝试让线程池进入【终止】状态
        tryTerminate();
        
        return tasks;
    }
    
    // 等待线程池进入【终止】状态（等待过程中会释放锁，醒来后恢复锁）
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        mainLock.lock();
        try {
            // 如果线程池未【终止】，则等待线程池【终止】
            while(runStateLessThan(ctl.get(), TERMINATED)) {
                // 如果已超时，直接返回
                if(nanos<=0L) {
                    return false;
                }
                nanos = termination.awaitNanos(nanos);
            }
            return true;
        } finally {
            mainLock.unlock();
        }
    }
    
    /**
     * Transitions to TERMINATED state if either (SHUTDOWN and pool
     * and queue empty) or (STOP and pool empty).  If otherwise
     * eligible to terminate but workerCount is nonzero, interrupts an
     * idle worker to ensure that shutdown signals propagate. This
     * method must be called following any action that might make
     * termination possible -- reducing worker count or removing tasks
     * from the queue during shutdown. The method is non-private to
     * allow access from ScheduledThreadPoolExecutor.
     */
    // 尝试让线程池进入{3}【终止】状态
    final void tryTerminate() {
        for(; ; ) {
            int c = ctl.get();
            
            // {-1} 处于【运行】状态，无法终止
            if(isRunning(c)
                // {23} 或者，处于【完结】或【终止】状态，无需终止
                || runStateAtLeast(c, TIDYING)
                // {0} 或者，处于【关闭】状态（还可以处理阻塞队列的阻塞任务），且阻塞队列不为空，无法终止
                || (runStateLessThan(c, STOP) && !workQueue.isEmpty())) {
                
                // 无需，或无法【终止】线程池，直接返回
                return;
            }
            
            /*
             * 至此，有两种可能：
             * 1.线程池处于【停止】状态（不接收新线程，也不再处理阻塞任务，且正在处理的阻塞任务也要被中断）
             * 2.线程池处于【关闭】状态（还可以处理阻塞队列的阻塞任务），但阻塞队列为空（没有阻塞任务了）
             */
            
            // 如果线程池不为空
            if(workerCountOf(c) != 0) {
                /* Eligible to terminate */
                
                // 要求线程池中至少有一个【空闲】的Worker设置了中断标记
                interruptIdleWorkers(ONLY_ONE);
                
                // 无法【终止】线程池，到这里直接返回
                return;
            }
            
            /* 至此，表明线程池为空 */
            
            mainLock.lock();
            try {
                // 设置线程池运行状态为【完结】
                if(ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        // 回调
                        terminated();
                    } finally {
                        // 回调结束后，设置线程池的运行状态为【终止】
                        ctl.set(ctlOf(TERMINATED, 0));
                        
                        // 唤醒那些等待线程池【终止】的线程
                        termination.signalAll();
                    }
                    
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            
            /* else retry on failed CAS */
        } // for(; ; )
    }
    
    /**
     * If there is a security manager, makes sure caller has
     * permission to shut down threads in general (see shutdownPerm).
     * If this passes, additionally makes sure the caller is allowed
     * to interrupt each worker thread. This might not be true even if
     * first check passed, if the SecurityManager treats some threads
     * specially.
     */
    // 检查【关闭】权限
    private void checkShutdownAccess() {
        // assert mainLock.isHeldByCurrentThread();
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkPermission(shutdownPerm);
            for(Worker w : workerPool) {
                security.checkAccess(w.workerThread);
            }
        }
    }
    
    /*▲ 运行状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工作线程(Worker)数量 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Attempts to CAS-increment the workerCount field of ctl.
     */
    // 原子地递【增】线程池中工作线程(Worker)的数量，并更新线程池状态标记
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }
    
    /**
     * Attempts to CAS-decrement the workerCount field of ctl.
     */
    // 原子地递【减】线程池中工作线程(Worker)的数量，并更新线程池状态标记
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }
    
    /**
     * Decrements the workerCount field of ctl. This is called only on
     * abrupt termination of a thread (see processWorkerExit). Other
     * decrements are performed within getTask.
     */
    // 原子地递【减】线程池中工作线程(Worker)的数量，并更新线程池状态标记
    private void decrementWorkerCount() {
        ctl.addAndGet(-1);
    }
    
    /*▲ 工作线程(Worker)数量 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 线程池状态记标 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回运行状态（位于线程池状态标记的前3位）
    private static int runStateOf(int workerPoolState) {
        return workerPoolState & ~COUNT_MASK;
    }
    
    // 返回线程池中的工作线程(Worker)数量（位于线程池状态标记的后29位）
    private static int workerCountOf(int workerPoolState) {
        return workerPoolState & COUNT_MASK;
    }
    
    // 合成线程池状态标记
    private static int ctlOf(int runState, int workerCount) {
        return runState | workerCount;
    }
    
    /*
     * Bit field accessors that don't require unpacking ctl.
     * These depend on the bit layout and on workerCount being never negative.
     */
    
    private static boolean runStateLessThan(int workerPoolState, int flag) {
        return workerPoolState<flag;
    }
    
    private static boolean runStateAtLeast(int workerPoolState, int flag) {
        return workerPoolState >= flag;
    }
    
    /*▲ 线程池状态记标 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中断 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     */
    // 中断线程池中所有正在【执行】的Worker
    private void interruptWorkers() {
        // assert mainLock.isHeldByCurrentThread();
        for(Worker w : workerPool) {
            // 为当前正在执行的任务/线程设置中断标记
            w.interruptIfStarted();
        }
    }
    
    /**
     * Common form of interruptIdleWorkers,
     * to avoid having to remember what the boolean argument means.
     */
    /*
     * 中断线程池中所有【空闲】的Worker
     * 【空闲】Worker是那些正在等待从阻塞队列中获取任务的Worker
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }
    
    /**
     * Interrupts threads that might be waiting for tasks (as
     * indicated by not being locked) so they can check for
     * termination or configuration changes. Ignores
     * SecurityExceptions (in which case some threads may remain
     * uninterrupted).
     *
     * @param onlyOne If true, interrupt at most one worker. This is
     *                called only from tryTerminate when termination is otherwise
     *                enabled but there are still other workers.  In this case, at
     *                most one waiting worker is interrupted to propagate shutdown
     *                signals in case all threads are currently waiting.
     *                Interrupting any arbitrary thread ensures that newly arriving
     *                workers since shutdown began will also eventually exit.
     *                To guarantee eventual termination, it suffices to always
     *                interrupt only one idle worker, but shutdown() interrupts all
     *                idle workers so that redundant workers exit promptly, not
     *                waiting for a straggler task to finish.
     */
    /*
     * 中断线程池中【空闲】的Worker
     * 【空闲】Worker是指那些正在等待从阻塞队列中获取任务的Worker
     *
     * onlyOne==true ：要求至少有一个线程被设置了中断标记
     * onlyOne==false：要求所有线程被设置了中断标记
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        mainLock.lock();
    
        try {
            // 遍历线程池，取出Worker
            for(Worker worker : workerPool) {
                // 获取Worker所在线程
                Thread workerThread = worker.workerThread;
            
                // 如果当前线程还未中断(该测试不影响线程的中断状态)
                if(!workerThread.isInterrupted()) {
                
                    // 如果线程还没有中断标记，则尝试锁住线程，并为其设置中断标记
                    if(worker.tryLock()) {
                        /* 至此，成功加锁 */
                    
                        try {
                            // 中断线程（只是给线程预设一个标记，不是立即让线程停下来）
                            workerThread.interrupt();
                        } catch(SecurityException ignore) {
                            // ignore
                        } finally {
                            // 解锁，此处只是简单地将许可证数量置为0
                            worker.unlock();
                        }
                    }
                }
                
                // 如果要求至少有一个线程被设置了中断标记，则此处可以退出了
                if(onlyOne) {
                    break;
                }
            }// for
        } finally {
            mainLock.unlock();
        }
    }
    
    /*▲ 中断 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 阻塞队列 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the task queue used by this executor. Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     *
     * @return the task queue
     */
    // 获取线程池使用的阻塞队列
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }
    
    /**
     * Removes this task from the executor's internal queue if it is
     * present, thus causing it not to be run if it has not already
     * started.
     *
     * <p>This method may be useful as one part of a cancellation
     * scheme.  It may fail to remove tasks that have been converted
     * into other forms before being placed on the internal queue.
     * For example, a task entered using {@code submit} might be
     * converted into a form that maintains {@code Future} status.
     * However, in such cases, method {@link #purge} may be used to
     * remove those Futures that have been cancelled.
     *
     * @param task the task to remove
     *
     * @return {@code true} if the task was removed
     */
    // 从阻塞队列中移除之前添加的任务
    public boolean remove(Runnable task) {
        // 移除任务
        boolean removed = workQueue.remove(task);
        
        // 尝试让线程池进入【终止】状态
        tryTerminate(); // In case SHUTDOWN and now empty
        
        return removed;
    }
    
    /**
     * Tries to remove from the work queue all {@link Future}
     * tasks that have been cancelled. This method can be useful as a
     * storage reclamation operation, that has no other impact on
     * functionality. Cancelled tasks are never executed, but may
     * accumulate in work queues until worker threads can actively
     * remove them. Invoking this method instead tries to remove them now.
     * However, this method may fail to remove tasks in
     * the presence of interference by other threads.
     */
    // 清理阻塞队列中所有Future类型的任务
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            // 建立阻塞队列的迭代器
            Iterator<Runnable> it = q.iterator();
            while(it.hasNext()) {
                Runnable r = it.next();
                // 筛选出Future类型的任务，如果该任务已被取消，则将其从阻塞队列中移除
                if(r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    it.remove();
                }
            }
        } catch(ConcurrentModificationException fallThrough) {
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            for(Object r : q.toArray()) {
                if(r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    q.remove(r);
                }
            }
        }
        
        // 尝试让线程池进入【终止】状态
        tryTerminate(); // In case SHUTDOWN and now empty
    }
    
    /**
     * Drains the task queue into a new list, normally using
     * drainTo. But if the queue is a DelayQueue or any other kind of
     * queue for which poll or drainTo may fail to remove some
     * elements, it deletes them one by one.
     */
    // 取出阻塞队列中未处理的阻塞任务
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        
        // 取出阻塞队列中所有阻塞的任务，存入到taskList中
        ArrayList<Runnable> taskList = new ArrayList<>();
        q.drainTo(taskList);
        
        if(!q.isEmpty()) {
            for(Runnable r : q.toArray(new Runnable[0])) {
                if(q.remove(r)) {
                    taskList.add(r);
                }
            }
        }
        
        return taskList;
    }
    /*▲ 阻塞队列 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 线程池属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the thread factory used to create new threads.
     *
     * @return the current thread factory
     *
     * @see #setThreadFactory(ThreadFactory)
     */
    // 获取线程工厂，以便创建执行【任务】的【工作线程】
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }
    
    /**
     * Sets the thread factory used to create new threads.
     *
     * @param threadFactory the new thread factory
     *
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    // 设置线程工厂
    public void setThreadFactory(ThreadFactory threadFactory) {
        if(threadFactory == null) {
            throw new NullPointerException();
        }
        this.threadFactory = threadFactory;
    }
    
    /**
     * Returns the core number of threads.
     *
     * @return the core number of threads
     *
     * @see #setCorePoolSize
     */
    // 获取线程池的【核心阈值】
    public int getCorePoolSize() {
        return corePoolSize;
    }
    
    /**
     * Sets the core number of threads.
     * This overrides any value set in the constructor.
     * If the new value is smaller than the current value, excess existing threads will be terminated when they next become idle.
     * If larger, new threads will, if needed, be started to execute any queued tasks.
     *
     * @param corePoolSize the new core size
     *
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     *                                  or {@code corePoolSize} is greater than the {@linkplain
     *                                  #getMaximumPoolSize() maximum pool size}
     * @see #getCorePoolSize
     */
    // 更新线程池的【核心阈值】
    public void setCorePoolSize(int corePoolSize) {
        if(corePoolSize<0 || corePoolSize>maximumPoolSize) {
            throw new IllegalArgumentException();
        }
        
        // 需要增加多少容量
        int delta = corePoolSize - this.corePoolSize;
    
        // 更新【核心阈值】
        this.corePoolSize = corePoolSize;
    
        // 如果线程池中的任务数已经超过【核心阈值】(新值较小)
        if(workerCountOf(ctl.get())>corePoolSize) {
            // 中断线程池中所有【空闲】的Worker
            interruptIdleWorkers();
    
            // 新值较大，则需要新增【N】型Worker
        } else if(delta>0) {
            /*
             * We don't really know how many new threads are "needed".
             * As a heuristic, prestart enough new workers (up to new core size) to handle the current number of tasks in queue,
             * but stop if queue becomes empty while doing so.
             */
            // 限制新增【N】型Worker的数量
            int k = Math.min(delta, workQueue.size());
    
            // 添加【N】型Worker到线程池
            while(k-->0 && addWorker(null, true)) {
                // 如果阻塞队列中已经没有阻塞的任务了，那么就自然也不再需要增加【N】型Worker了
                if(workQueue.isEmpty()) {
                    break;
                }
            }
        }
    }
    
    /**
     * Returns the maximum allowed number of threads.
     *
     * @return the maximum allowed number of threads
     *
     * @see #setMaximumPoolSize
     */
    // 获取线程池的【最大阈值】
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }
    
    /**
     * Sets the maximum allowed number of threads. This overrides any
     * value set in the constructor. If the new value is smaller than
     * the current value, excess existing threads will be
     * terminated when they next become idle.
     *
     * @param maximumPoolSize the new maximum
     *
     * @throws IllegalArgumentException if the new maximum is
     *                                  less than or equal to zero, or
     *                                  less than the {@linkplain #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
    // 设置线程池的【最大阈值】
    public void setMaximumPoolSize(int maximumPoolSize) {
        if(maximumPoolSize<=0 || maximumPoolSize<corePoolSize) {
            throw new IllegalArgumentException();
        }
    
        // 更新【最大阈值】
        this.maximumPoolSize = maximumPoolSize;
    
        // 如果线程池中的任务数已经超过【最大阈值】
        if(workerCountOf(ctl.get())>maximumPoolSize) {
            // 中断线程池中所有【空闲】的Worker
            interruptIdleWorkers();
        }
    }
    
    /**
     * Returns the current handler for unexecutable tasks.
     *
     * @return the current handler
     *
     * @see #setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    // 获取【拒绝策略】处理器
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }
    
    /**
     * Sets a new handler for unexecutable tasks.
     *
     * @param handler the new handler
     *
     * @throws NullPointerException if handler is null
     * @see #getRejectedExecutionHandler
     */
    // 设置【拒绝策略】处理器
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if(handler == null) {
            throw new NullPointerException();
        }
        this.handler = handler;
    }
    
    /**
     * Returns true if this pool allows core threads to time out and
     * terminate if no tasks arrive within the keepAlive time, being
     * replaced if needed when new tasks arrive. When true, the same
     * keep-alive policy applying to non-core threads applies also to
     * core threads. When false (the default), core threads are never
     * terminated due to lack of incoming tasks.
     *
     * @return {@code true} if core threads are allowed to time out,
     * else {@code false}
     *
     * @since 1.6
     */
    // 线程池是否主动启用了超时设置
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }
    
    /**
     * Sets the policy governing whether core threads may time out and
     * terminate if no tasks arrive within the keep-alive time, being
     * replaced if needed when new tasks arrive. When false, core
     * threads are never terminated due to lack of incoming
     * tasks. When true, the same keep-alive policy applying to
     * non-core threads applies also to core threads. To avoid
     * continual thread replacement, the keep-alive time must be
     * greater than zero when setting {@code true}. This method
     * should in general be called before the pool is actively used.
     *
     * @param value {@code true} if should time out, else {@code false}
     *
     * @throws IllegalArgumentException if value is {@code true}
     *                                  and the current keep-alive time is not greater than zero
     * @since 1.6
     */
    // 对线程池主动启用/关闭超时设置
    public void allowCoreThreadTimeOut(boolean value) {
        if(value && keepAliveTime<=0) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        
        if(value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            
            // 如果主动启用了超时设置
            if(value) {
                // 中断线程池中所有【空闲】的Worker
                interruptIdleWorkers();
            }
        }
    }
    
    /**
     * Returns the thread keep-alive time, which is the amount of time
     * that threads may remain idle before being terminated.
     * Threads that wait this amount of time without processing a
     * task will be terminated if there are more than the core
     * number of threads currently in the pool, or if this pool
     * {@linkplain #allowsCoreThreadTimeOut() allows core thread timeout}.
     *
     * @param unit the desired time unit of the result
     *
     * @return the time limit
     *
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    // 获取【N】型Worker的空闲时长
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }
    
    /**
     * Sets the thread keep-alive time, which is the amount of time
     * that threads may remain idle before being terminated.
     * Threads that wait this amount of time without processing a
     * task will be terminated if there are more than the core
     * number of threads currently in the pool, or if this pool
     * {@linkplain #allowsCoreThreadTimeOut() allows core thread timeout}.
     * This overrides any value set in the constructor.
     *
     * @param time the time to wait.  A time value of zero will cause
     *             excess threads to terminate immediately after executing tasks.
     * @param unit the time unit of the {@code time} argument
     *
     * @throws IllegalArgumentException if {@code time} less than zero or
     *                                  if {@code time} is zero and {@code allowsCoreThreadTimeOut}
     * @see #getKeepAliveTime(TimeUnit)
     */
    // 设置【N】型Worker的空闲时长
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if(time<0) {
            throw new IllegalArgumentException();
        }
        if(time == 0 && allowsCoreThreadTimeOut()) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if(delta<0) {
            // 中断线程池中所有【空闲】的Worker
            interruptIdleWorkers();
        }
    }
    
    /*▲ 线程池属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 统计 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current number of threads in the pool.
     *
     * @return the number of threads
     */
    // 获取线程池中当前的Worker数量，如果已处于【完结】或【终止】状态，直接返回0
    public int getPoolSize() {
        mainLock.lock();
        try {
            // Remove rare and surprising possibility of isTerminated() && getPoolSize() > 0
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 : workerPool.size();
        } finally {
            mainLock.unlock();
        }
    }
    
    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     *
     * @return the number of threads
     */
    // 线程池中Worker数量达到的最大值
    public int getLargestPoolSize() {
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }
    
    /**
     * Returns the approximate number of threads that are actively
     * executing tasks.
     *
     * @return the number of threads
     */
    // 获取线程池中当前【执行】的Worker数量
    public int getActiveCount() {
        mainLock.lock();
        try {
            int n = 0;
            // 遍历Worker
            for(Worker w : workerPool) {
                // 如果Worker已上锁（正在执行）
                if(w.isLocked()) {
                    ++n;    // 累加
                }
            }
            return n;
        } finally {
            mainLock.unlock();
        }
    }
    
    /**
     * Returns the approximate total number of tasks that have ever been
     * scheduled for execution. Because the states of tasks and
     * threads may change dynamically during computation, the returned
     * value is only an approximation.
     *
     * @return the number of tasks
     */
    // 获取线程池执行过的所有Worker数量（包括此刻正在执行的任务数量和在可预计的将来会执行的任务数量）
    public long getTaskCount() {
        mainLock.lock();
        try {
            // 线程池累计执行的任务数量
            long n = completedTaskCount;
            // 遍历Worker
            for(Worker w : workerPool) {
                // 累加当前Worker执行的任务数
                n += w.completedTasks;
                // 如果Worker已上锁（正在执行）
                if(w.isLocked()) {
                    ++n;    // 累加
                }
            }
            
            // 加上当前阻塞队列中的任务数量（后续会被执行）
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }
    
    /**
     * Returns the approximate total number of tasks that have
     * completed execution. Because the states of tasks and threads
     * may change dynamically during computation, the returned value
     * is only an approximation, but one that does not ever decrease
     * across successive calls.
     *
     * @return the number of tasks
     */
    // 获取线程池执行完毕的所有Worker数量（只包括当前确定执行完的）
    public long getCompletedTaskCount() {
        mainLock.lock();
        try {
            // 线程池累计执行的任务数量
            long n = completedTaskCount;
            // 遍历Worker
            for(Worker w : workerPool) {
                // 累加当前Worker执行的任务数
                n += w.completedTasks;
            }
            return n;
        } finally {
            mainLock.unlock();
        }
    }
    
    /*▲ 统计 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 回调 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Method invoked prior to executing the given Runnable in the
     * given thread.  This method is invoked by thread {@code t} that
     * will execute task {@code r}, and may be used to re-initialize
     * ThreadLocals, or to perform logging.
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.beforeExecute} at the end of
     * this method.
     *
     * @param t the thread that will run task {@code r}
     * @param r the task that will be executed
     */
    // 任务执行前的回调，位于runWorker(Worker)中
    protected void beforeExecute(Thread workerThread, Runnable runnableTask) {
    }
    
    /**
     * Method invoked upon completion of execution of the given Runnable.
     * This method is invoked by the thread that executed the task. If
     * non-null, the Throwable is the uncaught {@code RuntimeException}
     * or {@code Error} that caused execution to terminate abruptly.
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.afterExecute} at the
     * beginning of this method.
     *
     * <p><b>Note:</b> When actions are enclosed in tasks (such as
     * {@link FutureTask}) either explicitly or via methods such as
     * {@code submit}, these task objects catch and maintain
     * computational exceptions, and so they do not cause abrupt
     * termination, and the internal exceptions are <em>not</em>
     * passed to this method. If you would like to trap both kinds of
     * failures in this method, you can further probe for such cases,
     * as in this sample subclass that prints either the direct cause
     * or the underlying exception if a task has been aborted:
     *
     * <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null
     *         && r instanceof Future<?>
     *         && ((Future<?>)r).isDone()) {
     *       try {
     *         Object result = ((Future<?>) r).get();
     *       } catch (CancellationException ce) {
     *         t = ce;
     *       } catch (ExecutionException ee) {
     *         t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *         // ignore/reset
     *         Thread.currentThread().interrupt();
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param r the runnable that has completed
     * @param t the exception that caused termination, or null if
     *          execution completed normally
     */
    // 任务执行后的回调，该方法位于runWorker(Worker)中
    protected void afterExecute(Runnable runnableTask, Throwable ex) {
    }
    
    /**
     * Method invoked when the Executor has terminated.  Default
     * implementation does nothing. Note: To properly nest multiple
     * overridings, subclasses should generally invoke
     * {@code super.terminated} within this method.
     */
    // 线程池从【完结】进入【终止】时的回调，该方法位于tryTerminate()中
    protected void terminated() {
    }
    
    /**
     * Performs any further cleanup following run state transition on
     * invocation of shutdown.  A no-op here, but used by
     * ScheduledThreadPoolExecutor to cancel delayed tasks.
     */
    // 线程池进入【关闭】之后的回调，通常可用来处理阻塞队列中的任务，或者进行其他收尾工作
    void onShutdown() {
    }
    
    /*▲ 回调 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 预启动 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Starts a core thread, causing it to idly wait for work.
     * This overrides the default policy of starting core threads only when new tasks are executed.
     * This method will return {@code false} if all core threads have already been started.
     *
     * @return {@code true} if a thread was started
     */
    // 【预启动】:在【核心阈值】的约束下，向线程池添加一个【N】型Worker并启动它
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get())<corePoolSize && addWorker(null, true);
    }
    
    /**
     * Starts all core threads, causing them to idly wait for work.
     * This overrides the default policy of starting core threads only when new tasks are executed.
     *
     * @return the number of threads started
     */
    // 【预启动】:在【核心阈值】的约束下，不断向线程池添加【N】型Worker并启动它，直到Worker数量达到阈值
    public int prestartAllCoreThreads() {
        int n = 0;
    
        while(addWorker(null, true)) {
            ++n;
        }
    
        return n;
    }
    
    /**
     * Same as prestartCoreThread except arranges that at least one
     * thread is started even if corePoolSize is 0.
     */
    // 最低保障启动：当向阻塞队列中加入一个待执行任务时，需要启动一个【N】型Worker以扫描该阻塞队列并处理任务
    void ensurePrestart() {
        int workerCount = workerCountOf(ctl.get());
    
        if(workerCount<corePoolSize) {
            // 在【核心阈值】限制下，添加一个【N】
            addWorker(null, true);
        
            // 如果线程池【核心阈值】为0
        } else if(workerCount == 0) {
            // 在【最大阈值】限制下，添加一个【N】
            addWorker(null, false);
        }
    }
    
    /*▲ 预启动 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state and estimated worker and
     * task counts.
     *
     * @return a string identifying this pool, as well as its state
     */
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        
        mainLock.lock();
        
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workerPool.size();
            for(Worker w : workerPool) {
                ncompleted += w.completedTasks;
                if(w.isLocked()) {
                    ++nactive;
                }
            }
        } finally {
            mainLock.unlock();
        }
        
        int c = ctl.get();
        
        String runState = isRunning(c) ? "Running" : runStateAtLeast(c, TERMINATED) ? "Terminated" : "Shutting down";
        
        return super.toString()
            + "["
            + runState
            + ", pool size = " + nworkers
            + ", active threads = " + nactive
            + ", queued tasks = " + workQueue.size()
            + ", completed tasks = " + ncompleted
            + "]";
    }
    
    
    /*
     * Override without "throws Throwable" for compatibility with subclasses
     * whose finalize method invokes super.finalize() (as is recommended).
     * Before JDK 11, finalize() had a non-empty method body.
     */
    
    /**
     * @implNote Previous versions of this class had a finalize method
     * that shut down this executor, but in this version, finalize
     * does nothing.
     */
    @Deprecated(since = "9")
    protected void finalize() {
    }
    
    
    
    
    
    
    /**
     * Class Worker mainly maintains interrupt control state for
     * threads running tasks, along with other minor bookkeeping.
     * This class opportunistically extends AbstractQueuedSynchronizer
     * to simplify acquiring and releasing a lock surrounding each
     * task execution.  This protects against interrupts that are
     * intended to wake up a worker thread waiting for a task from
     * instead interrupting a task being run.  We implement a simple
     * non-reentrant mutual exclusion lock rather than use
     * ReentrantLock because we do not want worker tasks to be able to
     * reacquire the lock when they invoke pool control methods like
     * setCorePoolSize.  Additionally, to suppress interrupts until
     * the thread actually starts running tasks, we initialize lock
     * state to a negative value, and clear it upon start (in
     * runWorker).
     */
    /*
     * 一个Worker兼具【任务】与【线程】的含义，它将待执行任务firstTask封装为一个线程存储到自身的thread域中以待执行
     *
     * Worker可分为两类：【R】型Worker和【N】型Worker
     * 【R】型Worker携带了【任务】，它被执行完后，转为【N】型Worker（不断从阻塞队列中取出任务执行，没有任务时就阻塞）。
     * 【N】型Worker自身不携带【任务】，它专门负责从阻塞队列中取出任务以便执行。
     *
     * 【R】型线程：【R】型Worker中的线程域，一对一关系
     * 【N】型线程：【N】型Worker中的线程域，一对一关系
     *
     * 除此之外，Worker还兼具锁的功能，可在增减Worker以及执行Worker时加锁
     */
    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
        /**
         * This class will never be serialized, but we provide a
         * serialVersionUID to suppress a javac warning.
         */
        private static final long serialVersionUID = 6138294804551838833L;
    
        /** Thread this worker is running in.  Null if factory fails. */
        // 【工作线程】：后续执行firstTask【任务】的【线程】
        final Thread workerThread;
        
        /** Initial task to run.  Possibly null. */
        // 待执行【任务】
        Runnable firstTask;
        
        /** Per-thread task counter */
        // 记录当前Worker(工作线程)执行的任务数
        volatile long completedTasks;
        
        // TODO: switch to AbstractQueuedLongSynchronizer and move completedTasks into the lock word.
        
        /**
         * Creates with given first task and thread from ThreadFactory.
         *
         * @param firstTask the first task (null if none)
         */
        Worker(Runnable firstTask) {
            // 每个任务的锁状态被初始化为-1，这使得工作线程在运行之前禁止中断
            setState(-1); // inhibit interrupts until runWorker
    
            this.firstTask = firstTask;
    
            // 创建执行【任务】的【工作线程】
            this.workerThread = getThreadFactory().newThread(this);
        }
        
        
        /** Delegates main run loop to outer runWorker. */
        // （线程开始运行）执行任务
        public void run() {
            // 执行当前任务
            runWorker(this);
        }
        
        
        /*
         * Lock methods
         *
         * The value 0 represents the unlocked state.
         * The value 1 represents the locked state.
         */
    
        // 加锁，此处只是简单地将许可证数量从0更新到1(会调用到tryAcquire()方法)
        public void lock() {
            // 申请独占锁
            acquire(1);
        }
    
        // 加锁，此处只是简单地将许可证数量从0更新到1，返回值表示是否加锁成功
        public boolean tryLock() {
            // 申请独占锁
            return tryAcquire(1);
        }
    
        // 解锁，此处只是简单地将许可证数量置为0，以便后续可以对其加锁(会调用到tryRelease()方法)
        public void unlock() {
            // 释放独占锁
            release(1);
        }
    
        // 释放独占锁，此处只是简单地将许可证数量置为0，总是返回true
        protected boolean tryRelease(int unused) {
            // 清理当前锁的持有者
            setExclusiveOwnerThread(null);
        
            // 锁标记更新为0
            setState(0);
        
            return true;
        }
    
        // 申请独占锁，此处只是简单地将许可证数量从0更新到1
        protected boolean tryAcquire(int unused) {
            if(compareAndSetState(0, 1)) {
                // 设置当前锁的持有者
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
        
            return false;
        }
    
    
        // 判断当前线程是否为锁的持有者
        public boolean isLocked() {
            return isHeldExclusively();
        }
    
        // 判断当前线程是否为锁的持有者
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }
        
        
        // 为当前正在执行的任务/线程设置中断标记
        void interruptIfStarted() {
            // 【工作线程】还未运行，禁止中断
            if(getState()<0) {
                return;
            }
    
            if(workerThread == null) {
                return;
            }
    
            // 如果线程已经中断(该测试不影响线程的中断状态)
            if(workerThread.isInterrupted()) {
                return;
            }
    
            try {
                // 为线程设置中断标记
                workerThread.interrupt();
            } catch(SecurityException ignore) {
            }
        }
    }
    
    
    
    /*▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ 预定义的拒绝策略 ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼*/
    
    /**
     * A handler for rejected tasks that throws a
     * {@link RejectedExecutionException}.
     *
     * This is the default handler for {@link ThreadPoolExecutor} and
     * {@link ScheduledThreadPoolExecutor}.
     */
    // 默认阻塞策略，丢弃任务，而且抛异常
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() {
        }
        
        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         *
         * @throws RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
        }
    }
    
    /**
     * A handler for rejected tasks that silently discards the
     * rejected task.
     */
    // 丢弃任务，但不抛异常
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardPolicy}.
         */
        public DiscardPolicy() {
        }
        
        /**
         * Does nothing, which has the effect of discarding task r.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }
    
    /**
     * A handler for rejected tasks that runs the rejected task
     * directly in the calling thread of the {@code execute} method,
     * unless the executor has been shut down, in which case the task
     * is discarded.
     */
    // 如果线程池仍处于【运行】状态下，同步运行该任务，可能会阻塞execute()方法
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code CallerRunsPolicy}.
         */
        public CallerRunsPolicy() {
        }
        
        /**
         * Executes task r in the caller's thread, unless the executor
         * has been shut down, in which case the task is discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if(!e.isShutdown()) {
                r.run();
            }
        }
    }
    
    /**
     * A handler for rejected tasks that discards the oldest unhandled
     * request and then retries {@code execute}, unless the executor
     * is shut down, in which case the task is discarded.
     */
    // 如果线程池仍处于【运行】状态下，从阻塞队列中丢弃一个最早的任务，并调用execute()重新执行该任务
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardOldestPolicy} for the given executor.
         */
        public DiscardOldestPolicy() {
        }
        
        /**
         * Obtains and ignores the next task that the executor
         * would otherwise execute, if one is immediately available,
         * and then retries execution of task r, unless the executor
         * is shut down, in which case task r is instead discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if(!e.isShutdown()) {
                // 丢弃阻塞队列中的任务
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}
