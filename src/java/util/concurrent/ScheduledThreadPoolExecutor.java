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

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * A {@link ThreadPoolExecutor} that can additionally schedule
 * commands to run after a given delay, or to execute periodically.
 * This class is preferable to {@link java.util.Timer} when multiple
 * worker threads are needed, or when the additional flexibility or
 * capabilities of {@link ThreadPoolExecutor} (which this class
 * extends) are required.
 *
 * <p>Delayed tasks execute no sooner than they are enabled, but
 * without any real-time guarantees about when, after they are
 * enabled, they will commence. Tasks scheduled for exactly the same
 * execution time are enabled in first-in-first-out (FIFO) order of
 * submission.
 *
 * <p>When a submitted task is cancelled before it is run, execution
 * is suppressed.  By default, such a cancelled task is not
 * automatically removed from the work queue until its delay elapses.
 * While this enables further inspection and monitoring, it may also
 * cause unbounded retention of cancelled tasks.  To avoid this, use
 * {@link #setRemoveOnCancelPolicy} to cause tasks to be immediately
 * removed from the work queue at time of cancellation.
 *
 * <p>Successive executions of a periodic task scheduled via
 * {@link #scheduleAtFixedRate scheduleAtFixedRate} or
 * {@link #scheduleWithFixedDelay scheduleWithFixedDelay}
 * do not overlap. While different executions may be performed by
 * different threads, the effects of prior executions
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * those of subsequent ones.
 *
 * <p>While this class inherits from {@link ThreadPoolExecutor}, a few
 * of the inherited tuning methods are not useful for it. In
 * particular, because it acts as a fixed-sized pool using
 * {@code corePoolSize} threads and an unbounded queue, adjustments
 * to {@code maximumPoolSize} have no useful effect. Additionally, it
 * is almost never a good idea to set {@code corePoolSize} to zero or
 * use {@code allowCoreThreadTimeOut} because this may leave the pool
 * without threads to handle tasks once they become eligible to run.
 *
 * <p>As with {@code ThreadPoolExecutor}, if not otherwise specified,
 * this class uses {@link Executors#defaultThreadFactory} as the
 * default thread factory, and {@link ThreadPoolExecutor.AbortPolicy}
 * as the default rejected execution handler.
 *
 * <p><b>Extension notes:</b> This class overrides the
 * {@link ThreadPoolExecutor#execute(Runnable) execute} and
 * {@link AbstractExecutorService#submit(Runnable) submit}
 * methods to generate internal {@link ScheduledFuture} objects to
 * control per-task delays and scheduling.  To preserve
 * functionality, any further overrides of these methods in
 * subclasses must invoke superclass versions, which effectively
 * disables additional task customization.  However, this class
 * provides alternative protected extension method
 * {@code decorateTask} (one version each for {@code Runnable} and
 * {@code Callable}) that can be used to customize the concrete task
 * types used to execute commands entered via {@code execute},
 * {@code submit}, {@code schedule}, {@code scheduleAtFixedRate},
 * and {@code scheduleWithFixedDelay}.  By default, a
 * {@code ScheduledThreadPoolExecutor} uses a task type extending
 * {@link FutureTask}. However, this may be modified or replaced using
 * subclasses of the form:
 *
 * <pre> {@code
 * public class CustomScheduledExecutor extends ScheduledThreadPoolExecutor {
 *
 *   static class CustomTask<V> implements RunnableScheduledFuture<V> { ... }
 *
 *   protected <V> RunnableScheduledFuture<V> decorateTask(
 *                Runnable r, RunnableScheduledFuture<V> task) {
 *       return new CustomTask<V>(r, task);
 *   }
 *
 *   protected <V> RunnableScheduledFuture<V> decorateTask(
 *                Callable<V> c, RunnableScheduledFuture<V> task) {
 *       return new CustomTask<V>(c, task);
 *   }
 *   // ... add constructors, etc.
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
// 【定时任务线程池】，用于执行一次性或周期性的定时任务
public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor implements ScheduledExecutorService {
    
    /*
     * This class specializes ThreadPoolExecutor implementation by
     *
     * 1. Using a custom task type ScheduledFutureTask, even for tasks
     *    that don't require scheduling because they are submitted
     *    using ExecutorService rather than ScheduledExecutorService
     *    methods, which are treated as tasks with a delay of zero.
     *
     * 2. Using a custom queue (DelayedWorkQueue), a variant of
     *    unbounded DelayQueue. The lack of capacity constraint and
     *    the fact that corePoolSize and maximumPoolSize are
     *    effectively identical simplifies some execution mechanics
     *    (see delayedExecute) compared to ThreadPoolExecutor.
     *
     * 3. Supporting optional run-after-shutdown parameters, which
     *    leads to overrides of shutdown methods to remove and cancel
     *    tasks that should NOT be run after shutdown, as well as
     *    different recheck logic when task (re)submission overlaps
     *    with a shutdown.
     *
     * 4. Task decoration methods to allow interception and
     *    instrumentation, which are needed because subclasses cannot
     *    otherwise override submit methods to get this effect. These
     *    don't have any impact on pool control logic though.
     */
    
    /**
     * The default keep-alive time for pool threads.
     *
     * Normally, this value is unused because all pool threads will be
     * core threads, but if a user creates a pool with a corePoolSize
     * of zero (against our advice), we keep a thread alive as long as
     * there are queued tasks.  If the keep alive time is zero (the
     * historic value), we end up hot-spinning in getTask, wasting a
     * CPU.  But on the other hand, if we set the value too high, and
     * users create a one-shot pool which they don't cleanly shutdown,
     * the pool's non-daemon threads will prevent JVM termination.  A
     * small but non-zero value (relative to a JVM's lifetime) seems
     * best.
     */
    // 【N】型Worker的最大空闲时间（启用了超时设置后生效），参见ThreadPoolExecutor中的keepAliveTime
    private static final long DEFAULT_KEEPALIVE_MILLIS = 10L;
    
    /**
     * False if should cancel/suppress periodic tasks on shutdown.
     */
    /*
     * 线程池处于【关闭】状态时是否允许执行重复性任务，默认为false
     */
    private volatile boolean continueExistingPeriodicTasksAfterShutdown;
    
    /**
     * False if should cancel non-periodic not-yet-expired tasks on shutdown.
     */
    /*
     * 线程池处于【关闭】状态时是否允许执行一次性任务，默认为true
     */
    private volatile boolean executeExistingDelayedTasksAfterShutdown = true;
    
    /**
     * True if ScheduledFutureTask.cancel should remove from queue.
     */
    // 是否移除被中止的任务
    volatile boolean removeOnCancel;
    
    /**
     * Sequence number to break scheduling ties, and in turn to
     * guarantee FIFO order among tied entries.
     */
    // 任务的入队次序
    private static final AtomicLong sequencer = new AtomicLong();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new {@code ScheduledThreadPoolExecutor} with the
     * given core pool size.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *                     if they are idle, unless {@code allowCoreThreadTimeOut} is set
     *
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     */
    public ScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, MILLISECONDS, new DelayedWorkQueue());
    }
    
    /**
     * Creates a new {@code ScheduledThreadPoolExecutor} with the
     * given initial parameters.
     *
     * @param corePoolSize  the number of threads to keep in the pool, even
     *                      if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param threadFactory the factory to use when the executor
     *                      creates a new thread
     *
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @throws NullPointerException     if {@code threadFactory} is null
     */
    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, MILLISECONDS, new DelayedWorkQueue(), threadFactory);
    }
    
    /**
     * Creates a new {@code ScheduledThreadPoolExecutor} with the
     * given initial parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *                     if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param handler      the handler to use when execution is blocked
     *                     because the thread bounds and queue capacities are reached
     *
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @throws NullPointerException     if {@code handler} is null
     */
    public ScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, MILLISECONDS, new DelayedWorkQueue(), handler);
    }
    
    /**
     * Creates a new {@code ScheduledThreadPoolExecutor} with the
     * given initial parameters.
     *
     * @param corePoolSize  the number of threads to keep in the pool, even
     *                      if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param threadFactory the factory to use when the executor
     *                      creates a new thread
     * @param handler       the handler to use when execution is blocked
     *                      because the thread bounds and queue capacities are reached
     *
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @throws NullPointerException     if {@code threadFactory} or
     *                                  {@code handler} is null
     */
    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, MILLISECONDS, new DelayedWorkQueue(), threadFactory, handler);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建/执行/清理任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 立即执行一次性的定时任务(Runnable)，并返回任务本身
    public Future<?> submit(Runnable command) {
        return schedule(command, 0, NANOSECONDS);
    }
    
    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 立即执行一次性的定时任务(Runnable)，并返回任务本身；result为预设的返回结果
    public <T> Future<T> submit(Runnable command, T result) {
        return schedule(Executors.callable(command, result), 0, NANOSECONDS);
    }
    
    /**
     * Executes {@code command} with zero required delay.
     * This has effect equivalent to
     * {@link #schedule(Runnable, long, TimeUnit) schedule(command, 0, anyUnit)}.
     * Note that inspections of the queue and of the list returned by
     * {@code shutdownNow} will access the zero-delayed
     * {@link ScheduledFuture}, not the {@code command} itself.
     *
     * <p>A consequence of the use of {@code ScheduledFuture} objects is
     * that {@link ThreadPoolExecutor#afterExecute afterExecute} is always
     * called with a null second {@code Throwable} argument, even if the
     * {@code command} terminated abruptly.  Instead, the {@code Throwable}
     * thrown by such a task can be obtained via {@link Future#get}.
     *
     * @throws RejectedExecutionException at discretion of
     *                                    {@code RejectedExecutionHandler}, if the task
     *                                    cannot be accepted for execution because the
     *                                    executor has been shut down
     * @throws NullPointerException       {@inheritDoc}
     */
    // 立即执行一次性的定时任务(Runnable)
    public void execute(Runnable command) {
        schedule(command, 0, NANOSECONDS);
    }
    
    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 立即执行一次性的定时任务(Callable)，并返回任务本身
    public <T> Future<T> submit(Callable<T> command) {
        return schedule(command, 0, NANOSECONDS);
    }
    
    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 执行一次性的定时任务(Runnable)，并返回任务本身：在任务启动后的initialDelay时长后开始执行
    public ScheduledFuture<?> schedule(Runnable command, long initialDelay, TimeUnit unit) {
        if(command == null || unit == null) {
            throw new NullPointerException();
        }
        
        // 构造一次性的定时任务
        ScheduledFutureTask<Void> task = new ScheduledFutureTask<>(command, null, triggerTime(initialDelay, unit), sequencer.getAndIncrement());
        RunnableScheduledFuture<Void> future = decorateTask(command, task);
        
        delayedExecute(future);  // 执行定时任务
        
        return future;
    }
    
    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 执行一次性的定时任务(Callable)，并返回任务本身：在任务启动后的initialDelay时长后开始执行
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long initialDelay, TimeUnit unit) {
        if(callable == null || unit == null) {
            throw new NullPointerException();
        }
        
        // 构造一次性的定时任务
        ScheduledFutureTask<V> task = new ScheduledFutureTask<>(callable, triggerTime(initialDelay, unit), sequencer.getAndIncrement());
        RunnableScheduledFuture<V> future = decorateTask(callable, task);
        
        delayedExecute(future);  // 执行定时任务
        
        return future;
    }
    
    /**
     * Submits a periodic action that becomes enabled first after the
     * given initial delay, and subsequently with the given period;
     * that is, executions will commence after
     * {@code initialDelay}, then {@code initialDelay + period}, then
     * {@code initialDelay + 2 * period}, and so on.
     *
     * <p>The sequence of task executions continues indefinitely until
     * one of the following exceptional completions occur:
     * <ul>
     * <li>The task is {@linkplain Future#cancel explicitly cancelled}
     * via the returned future.
     * <li>Method {@link #shutdown} is called and the {@linkplain
     * #getContinueExistingPeriodicTasksAfterShutdownPolicy policy on
     * whether to continue after shutdown} is not set true, or method
     * {@link #shutdownNow} is called; also resulting in task
     * cancellation.
     * <li>An execution of the task throws an exception.  In this case
     * calling {@link Future#get() get} on the returned future will throw
     * {@link ExecutionException}, holding the exception as its cause.
     * </ul>
     * Subsequent executions are suppressed.  Subsequent calls to
     * {@link Future#isDone isDone()} on the returned future will
     * return {@code true}.
     *
     * <p>If any execution of this task takes longer than its period, then
     * subsequent executions may start late, but will not concurrently
     * execute.
     *
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     * @throws IllegalArgumentException   {@inheritDoc}
     */
    // 执行重复性的【固定周期】定时任务(Runnable)，并返回任务本身：在任务启动后的initialDelay时长后开始执行，以后每隔period时长就被触发一次(即使上次被触发的任务还未执行完)
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long rate, TimeUnit unit) {
        if(command == null || unit == null) {
            throw new NullPointerException();
        }
    
        if(rate<=0L) {
            throw new IllegalArgumentException();
        }
    
        // 构造重复性的定时任务
        ScheduledFutureTask<Void> task = new ScheduledFutureTask<Void>(command, null, triggerTime(initialDelay, unit), unit.toNanos(rate), sequencer.getAndIncrement());
        RunnableScheduledFuture<Void> future = decorateTask(command, task);
    
        task.outerTask = future;
    
        delayedExecute(future);  // 执行定时任务
    
        return future;
    }
    
    /**
     * Submits a periodic action that becomes enabled first after the
     * given initial delay, and subsequently with the given delay
     * between the termination of one execution and the commencement of
     * the next.
     *
     * <p>The sequence of task executions continues indefinitely until
     * one of the following exceptional completions occur:
     * <ul>
     * <li>The task is {@linkplain Future#cancel explicitly cancelled}
     * via the returned future.
     * <li>Method {@link #shutdown} is called and the {@linkplain
     * #getContinueExistingPeriodicTasksAfterShutdownPolicy policy on
     * whether to continue after shutdown} is not set true, or method
     * {@link #shutdownNow} is called; also resulting in task
     * cancellation.
     * <li>An execution of the task throws an exception.  In this case
     * calling {@link Future#get() get} on the returned future will throw
     * {@link ExecutionException}, holding the exception as its cause.
     * </ul>
     * Subsequent executions are suppressed.  Subsequent calls to
     * {@link Future#isDone isDone()} on the returned future will
     * return {@code true}.
     *
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     * @throws IllegalArgumentException   {@inheritDoc}
     */
    // 执行重复性的【固定延时】定时任务(Runnable)，并返回任务本身：在任务启动后的initialDelay时长后开始执行，任务下次的开始时间=任务上次结束时间+delay(必须等到上次的任务已经执行完)
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if(command == null || unit == null) {
            throw new NullPointerException();
        }
        
        if(delay<=0L) {
            throw new IllegalArgumentException();
        }
        
        // 构造重复性的定时任务
        ScheduledFutureTask<Void> task = new ScheduledFutureTask<Void>(command, null, triggerTime(initialDelay, unit), -unit.toNanos(delay), sequencer.getAndIncrement());
        RunnableScheduledFuture<Void> future = decorateTask(command, task);
    
        task.outerTask = future;
    
        delayedExecute(future);  // 执行定时任务
    
        return future;
    }
    
    
    /**
     * Modifies or replaces the task used to execute a runnable.
     * This method can be used to override the concrete
     * class used for managing internal tasks.
     * The default implementation simply returns the given task.
     *
     * @param runnable the submitted Runnable
     * @param task     the task created to execute the runnable
     * @param <V>      the type of the task's result
     *
     * @return a task that can execute the runnable
     *
     * @since 1.6
     */
    // 装饰任务，由子类实现；runnable是任务封装前的形态，task是任务封装后的形态
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        return task;
    }
    
    /**
     * Modifies or replaces the task used to execute a callable.
     * This method can be used to override the concrete
     * class used for managing internal tasks.
     * The default implementation simply returns the given task.
     *
     * @param callable the submitted Callable
     * @param task     the task created to execute the callable
     * @param <V>      the type of the task's result
     *
     * @return a task that can execute the callable
     *
     * @since 1.6
     */
    // 装饰任务，由子类实现；callable是任务封装前的形态，task是任务封装后的形态
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        return task;
    }
    
    
    /**
     * Main execution method for delayed or periodic tasks.
     * If pool is shut down, rejects the task. Otherwise adds task to queue
     * and starts a thread, if necessary, to run it.  (We cannot
     * prestart the thread to run the task because the task (probably)
     * shouldn't be run yet.)  If the pool is shut down while the task
     * is being added, cancel and remove it if required by state and
     * run-after-shutdown parameters.
     *
     * @param task the task
     */
    // 执行定时任务
    private void delayedExecute(RunnableScheduledFuture<?> task) {
        // 如果线程池至少处于【关闭】状态（不接收新线程）
        if(isShutdown()) {
            // 为指定的动作（任务）执行【拒绝策略】
            reject(task);
            return;
        }
    
        /* 至此，线程池处于【运行】状态 */
    
        // 将任务加入阻塞队列（这里使用了延时队列）
        super.getQueue().add(task);
    
        // 如果指定的任务在当前线程池状态下无法执行，而且成功将该任务从阻塞队列中移除
        if(!canRunInCurrentRunState(task) && remove(task)) {
            // 那么可以中止任务（但不中断线程）
            task.cancel(false);
        } else {
            // 当该任务可以执行，或者虽然不能执行，但又移除失败时，可以启动一个【N】型线程以扫描阻塞队列，以处理该任务
            ensurePrestart();
        }
    }
    
    /**
     * Returns true if can run a task given current run state and run-after-shutdown parameters.
     */
    // 判断指定的任务在当前线程池状态下是否可以执行
    boolean canRunInCurrentRunState(RunnableScheduledFuture<?> task) {
        // 如果线程池处于【运行】状态(可以接收新线程，也可以处理阻塞任务)
        if(!isShutdown()) {
            return true;    // 允许执行任务
        }
    
        // 线程池至少处于【停止】状态(不接收新线程，也不处理阻塞任务)
        if(isStopped()) {
            return false;   // 不允许执行任务
        }
    
        /* 至此，线程池处于【关闭】状态，即不接收新线程，但可以处理阻塞任务 */
        
        // 判断任务是重复性任务(true)还是一次性任务(false)
        boolean periodic = task.isPeriodic();
        
        if(periodic){
            // 如果该任务是重复性任务，则需要判断线程池处于【关闭】状态时是否允许执行重复性任务，默认为false
            return continueExistingPeriodicTasksAfterShutdown;
        } else {
            /*
             * 如果该任务是一次性任务，则需要判断线程池处于【关闭】状态时是否允许执行一次性任务，默认为true
             *
             * 对于一次性任务，还要进一步考虑其延时属性：
             * 如果不允许在线程池关闭时执行一次性任务，但是该任务已经到期了，那么此处也允许直接执行这个到期的任务，而那些没到期的任务会被丢弃
             */
            return executeExistingDelayedTasksAfterShutdown || task.getDelay(NANOSECONDS)<=0;
        }
    }
    
    /**
     * Requeues a periodic task unless current run state precludes it.
     * Same idea as delayedExecute except drops task rather than rejecting.
     *
     * @param task the task
     */
    // 处理重复性任务：将该任务再次加入阻塞队列
    void reExecutePeriodic(RunnableScheduledFuture<?> task) {
        // 如果指定的任务在当前线程池状态下可以执行
        if(canRunInCurrentRunState(task)) {
    
            // 将任务加入阻塞队列
            super.getQueue().add(task);
    
            if(canRunInCurrentRunState(task) || !remove(task)) {
                // 当该任务可以执行，或者虽然不能执行，但又移除失败时，可以启动一个【N】型线程以扫描阻塞队列，以处理该任务
                ensurePrestart();
        
                return;
            }
        }
    
        // 中止任务（但不中断线程）
        task.cancel(false);
    }
    
    /*▲ 创建/执行/清理任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 运行状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * <p>If the {@code ExecuteExistingDelayedTasksAfterShutdownPolicy}
     * has been set {@code false}, existing delayed tasks whose delays
     * have not yet elapsed are cancelled.  And unless the {@code
     * ContinueExistingPeriodicTasksAfterShutdownPolicy} has been set
     * {@code true}, future executions of existing periodic tasks will
     * be cancelled.
     *
     * @throws SecurityException {@inheritDoc}
     */
    /*
     * 先让线程池进入{0}【关闭】状态(直到成功)，再尝试进入{3}【终止】状态(不一定成功)；
     * 关闭过程中会为线程池中所有【空闲】Worker设置中断标记。
     */
    public void shutdown() {
        super.shutdown();
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
     * @return list of tasks that never commenced execution.
     * Each element of this list is a {@link ScheduledFuture}.
     * For tasks submitted via one of the {@code schedule}
     * methods, the element will be identical to the returned
     * {@code ScheduledFuture}.  For tasks submitted using
     * {@link #execute execute}, the element will be a
     * zero-delay {@code ScheduledFuture}.
     *
     * @throws SecurityException {@inheritDoc}
     */
    /*
     * 先让线程池进入{1}【停止】状态(直到成功)，再尝试进入{3}【终止】状态(不一定成功)；
     * 关闭过程中会为线程池中所有【空闲】Worker设置中断标记；
     * 返回阻塞队列中未处理的阻塞任务。
     */
    public List<Runnable> shutdownNow() {
        return super.shutdownNow();
    }
    
    
    /**
     * Cancels and clears the queue of all tasks that should not be run due to shutdown policy.
     * Invoked within super.shutdown.
     */
    // 运行在线程池【关闭】之后的回调，通常可用来处理阻塞队列中的任务，或者进行其他收尾工作
    @Override
    void onShutdown() {
        // 获取阻塞队列
        BlockingQueue<Runnable> taskQueue = super.getQueue();
        
        // 线程池处于【关闭】状态时是否允许执行一次性任务，默认为true
        boolean keepDelayed = getExecuteExistingDelayedTasksAfterShutdownPolicy();
    
        // 线程池处于【关闭】状态时是否允许执行重复性任务，默认为false
        boolean keepPeriodic = getContinueExistingPeriodicTasksAfterShutdownPolicy();
    
        /* Traverse snapshot to avoid iterator exceptions */
    
        // TODO: implement and use efficient removeIf
        // super.getQueue().removeIf(...);
    
        // 遍历阻塞队列中的任务
        for(Object task : taskQueue.toArray()) {
            // 只处理重复性或延时性任务
            if(!(task instanceof RunnableScheduledFuture)) {
                continue;
            }
        
            RunnableScheduledFuture<?> t = (RunnableScheduledFuture<?>) task;
        
            // 是否应当终止对该任务的处理
            boolean stop;
        
            // 如果是重复性任务
            if(t.isPeriodic()) {
                stop = !keepPeriodic;
            } else {
                // 对于一次性任务，还要进一步考虑其延时属性
                stop = !keepDelayed && t.getDelay(NANOSECONDS)>0;
            }
        
            // 如果任务应当被停止，或者任务已被中止
            if(stop || t.isCancelled()) { // also remove if already cancelled
                // 尝试将该任务阻塞队列中移除
                if(taskQueue.remove(t)) {
                    // 如果该任务被成功移除，重复中止任务（不中断线程）
                    t.cancel(false);
                }
            }
        }
    
        // 尝试让线程池进入{3}【终止】状态
        tryTerminate();
    }
    
    /*▲ 运行状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 【定时任务线程池】属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the policy on whether to continue executing existing
     * periodic tasks even when this executor has been {@code shutdown}.
     * In this case, executions will continue until {@code shutdownNow}
     * or the policy is set to {@code false} when already shutdown.
     * This value is by default {@code false}.
     *
     * @return {@code true} if will continue after shutdown
     *
     * @see #setContinueExistingPeriodicTasksAfterShutdownPolicy
     */
    // 返回值含义：线程池处于【关闭】状态时是否允许执行重复性任务，默认为false
    public boolean getContinueExistingPeriodicTasksAfterShutdownPolicy() {
        return continueExistingPeriodicTasksAfterShutdown;
    }
    
    /**
     * Sets the policy on whether to continue executing existing
     * periodic tasks even when this executor has been {@code shutdown}.
     * In this case, executions will continue until {@code shutdownNow}
     * or the policy is set to {@code false} when already shutdown.
     * This value is by default {@code false}.
     *
     * @param value if {@code true}, continue after shutdown, else don't
     *
     * @see #getContinueExistingPeriodicTasksAfterShutdownPolicy
     */
    // 设置线程池处于【关闭】状态时是否允许执行重复性任务
    public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean value) {
        continueExistingPeriodicTasksAfterShutdown = value;
    
        // 如果设置为不允许，且当前线程池已关闭
        if(!value && isShutdown()) {
            // 需要处理阻塞队列中的任务，并尝试终止线程池
            onShutdown();
        }
    }
    
    /**
     * Gets the policy on whether to execute existing delayed
     * tasks even when this executor has been {@code shutdown}.
     * In this case, these tasks will only terminate upon
     * {@code shutdownNow}, or after setting the policy to
     * {@code false} when already shutdown.
     * This value is by default {@code true}.
     *
     * @return {@code true} if will execute after shutdown
     *
     * @see #setExecuteExistingDelayedTasksAfterShutdownPolicy
     */
    // 返回值含义：线程池处于【关闭】状态时是否允许执行一次性任务，默认为true
    public boolean getExecuteExistingDelayedTasksAfterShutdownPolicy() {
        return executeExistingDelayedTasksAfterShutdown;
    }
    
    /**
     * Sets the policy on whether to execute existing delayed
     * tasks even when this executor has been {@code shutdown}.
     * In this case, these tasks will only terminate upon
     * {@code shutdownNow}, or after setting the policy to
     * {@code false} when already shutdown.
     * This value is by default {@code true}.
     *
     * @param value if {@code true}, execute after shutdown, else don't
     *
     * @see #getExecuteExistingDelayedTasksAfterShutdownPolicy
     */
    // 设置线程池处于【关闭】状态时是否允许执行一次性任务
    public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean value) {
        executeExistingDelayedTasksAfterShutdown = value;
    
        // 如果设置为不允许，且当前线程池已关闭
        if(!value && isShutdown()) {
            // 需要处理阻塞队列中的任务，并尝试终止线程池
            onShutdown();
        }
    }
    
    /**
     * Gets the policy on whether cancelled tasks should be immediately
     * removed from the work queue at time of cancellation.  This value is
     * by default {@code false}.
     *
     * @return {@code true} if cancelled tasks are immediately removed
     * from the queue
     *
     * @see #setRemoveOnCancelPolicy
     * @since 1.7
     */
    // 返回值含义：是否允许移除被中止的任务
    public boolean getRemoveOnCancelPolicy() {
        return removeOnCancel;
    }
    
    /**
     * Sets the policy on whether cancelled tasks should be immediately
     * removed from the work queue at time of cancellation.  This value is
     * by default {@code false}.
     *
     * @param value if {@code true}, remove on cancellation, else don't
     *
     * @see #getRemoveOnCancelPolicy
     * @since 1.7
     */
    // 设置是否允许移除被中止的任务
    public void setRemoveOnCancelPolicy(boolean value) {
        removeOnCancel = value;
    }
    
    /**
     * Returns the task queue used by this executor.  Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     *
     * <p>Each element of this queue is a {@link ScheduledFuture}.
     * For tasks submitted via one of the {@code schedule} methods, the
     * element will be identical to the returned {@code ScheduledFuture}.
     * For tasks submitted using {@link #execute execute}, the element
     * will be a zero-delay {@code ScheduledFuture}.
     *
     * <p>Iteration over this queue is <em>not</em> guaranteed to traverse
     * tasks in the order in which they will execute.
     *
     * @return the task queue
     */
    // 返回阻塞队列
    public BlockingQueue<Runnable> getQueue() {
        return super.getQueue();
    }
    
    /*▲ 【定时任务线程池】属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 任务触发时间 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the nanoTime-based trigger time of a delayed action.
     */
    // 计算任务的触发时间（=当前时刻+delay，时间单位是unit）
    private long triggerTime(long delay, TimeUnit unit) {
        return triggerTime(unit.toNanos(delay<0 ? 0 : delay));
    }
    
    /**
     * Returns the nanoTime-based trigger time of a delayed action.
     */
    // 计算任务的触发时间（=当前时刻+delay，时间单位是纳秒）。仅在内部使用，delay>=0
    long triggerTime(long delay) {
        return System.nanoTime() + ((delay<(Long.MAX_VALUE >> 1)) ? delay : overflowFree(delay));
    }
    
    /**
     * Constrains the values of all delays in the queue to be within Long.MAX_VALUE of each other, to avoid overflow in compareTo.
     * This may occur if a task is eligible to be dequeued, but has not yet been, while some other task is added with a delay of Long.MAX_VALUE.
     */
    // 处理溢出：该方法在delay>=(Long.MAX_VALUE >> 1)时被调用
    private long overflowFree(long delay) {
        // 查看队头任务
        Delayed head = (Delayed) super.getQueue().peek();
        if(head != null) {
            // 获取距任务触发还剩余的时间
            long headDelay = head.getDelay(NANOSECONDS);
            if(headDelay<0 && (delay-headDelay<0)) {
                delay = Long.MAX_VALUE + headDelay;
            }
        }
        
        return delay;
    }
    
    /*▲ 任务触发时间 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    // 定时任务，是一个带有延时特性的FutureTask
    private class ScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {
        
        /** The actual task to be re-enqueued by reExecutePeriodic */
        // 记录任务自身，以便后续重复执行
        RunnableScheduledFuture<V> outerTask = this;
        
        /**
         * Index into delay queue, to support faster cancellation.
         */
        // 当前任务/元素在延时队列（小顶堆）中的索引
        int heapIndex;
        
        /**
         * Period for repeating tasks, in nanoseconds.
         * A positive value indicates fixed-rate execution.
         * A negative value indicates fixed-delay execution.
         * A value of 0 indicates a non-repeating (one-shot) task.
         */
        /*
         * 任务的重复模式：
         *
         * 零  ：非重复任务：只执行一次
         * 正数：重复性任务：【固定周期】，从任务初次被触发开始，以后每隔period时长就被触发一次(即使上次被触发的任务还未执行完)
         * 负数：重复性任务：【固定延时】，任务下次的开始时间=任务上次结束时间+|period|(必须等到上次的任务已经执行完)
         */
        private final long period;
        
        /** The nanoTime-based time when the task is enabled to execute. */
        // 任务下次被触发的时间（绝对时间，时间单位是纳秒）
        private volatile long time;
        
        /** Sequence number to break ties FIFO */
        // 任务加入队列的次序
        private final long sequenceNumber;
        
        /**
         * Creates a one-shot action with given nanoTime-based trigger time.
         */
        // 构造一次性的定时任务：在triggerTime时刻开始执行
        ScheduledFutureTask(Runnable r, V result, long triggerTime, long sequenceNumber) {
            super(r, result);
            this.time = triggerTime;
            this.period = 0;
            this.sequenceNumber = sequenceNumber;
        }
        
        /**
         * Creates a one-shot action with given nanoTime-based trigger time.
         */
        // 构造一次性的定时任务
        ScheduledFutureTask(Callable<V> callable, long triggerTime, long sequenceNumber) {
            super(callable);
            this.time = triggerTime;
            this.period = 0;
            this.sequenceNumber = sequenceNumber;
        }
        
        /**
         * Creates a periodic action with given nanoTime-based initial
         * trigger time and period.
         */
        // 构造重复性的定时任务
        ScheduledFutureTask(Runnable r, V result, long triggerTime, long period, long sequenceNumber) {
            super(r, result);
            this.time = triggerTime;
            this.period = period;
            this.sequenceNumber = sequenceNumber;
        }
        
        /**
         * Overrides FutureTask version so as to reset/requeue if periodic.
         */
        // 执行定时任务
        public void run() {
            // 如果指定的任务在当前线程池状态下无法执行
            if(!canRunInCurrentRunState(this)) {
                // 中止任务（但不中断线程）
                cancel(false);
                
            } else if(!isPeriodic()) {
                // 非重复任务，直接执行(只执行一次)
                super.run();
    
                // 如果任务正常执行完成
            } else if(super.runAndReset()) {
                /* 对于重复性任务，执行完后要重置任务下次的触发时间 */
                
                // 设置任务下次被触发的时间
                setNextRunTime();
    
                // 处理重复性任务：将该任务再次加入阻塞队列
                reExecutePeriodic(outerTask);
            }
        }
        
        /**
         * Sets the next time to run for a periodic task.
         */
        // 设置任务下次被触发的时间
        private void setNextRunTime() {
            if(period>0) {
                // 固定周期
                time += period;
            } else {
                // 固定延时
                time = triggerTime(-period);
            }
        }
        
        // 获取距任务触发还剩余的时间
        public long getDelay(TimeUnit unit) {
            return unit.convert(time - System.nanoTime(), NANOSECONDS);
        }
        
        /**
         * Returns {@code true} if this is a periodic (not a one-shot) action.
         *
         * @return {@code true} if periodic
         */
        // 是否为重复性任务
        public boolean isPeriodic() {
            return period != 0;
        }
    
        // 中止任务，mayInterruptIfRunning指示是否需要中断线程
        public boolean cancel(boolean mayInterruptIfRunning) {
            /*
             * The racy read of heapIndex below is benign:
             * if heapIndex < 0, then OOTA guarantees that we have surely been removed;
             * else we recheck under lock in remove()
             */
            
            // 中止标记设置成功
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            
            // 如果允许移除被中止的任务
            if(cancelled && removeOnCancel && heapIndex >= 0) {
                // 移除当前任务
                remove(this);
            }
            
            return cancelled;
        }
        
        /*
         * 比较两个任务谁先被执行：
         *
         * this.compareTo(other)
         *  0: 同时执行
         * -1: this先执行
         *  1: other先执行
         */
        public int compareTo(Delayed other) {
            // 同一个任务，触发时间一致
            if(other == this) {
                // compare zero if same object
                return 0;
            }
            
            /*
             * 对于两个ScheduledFutureTask型任务，比较它们谁先开始执行策略：
             *
             * 1. 比较任务的触发时间
             * 2. 如果触发时间一致，则比较任务的入队次序
             */
            if(other instanceof ScheduledFutureTask) {
                ScheduledFutureTask<?> x = (ScheduledFutureTask<?>) other;
                long diff = time - x.time;
                if(diff<0) {
                    return -1;
                } else if(diff>0) {
                    return 1;
                } else if(sequenceNumber<x.sequenceNumber) {
                    return -1;
                } else {
                    return 1;
                }
            }
            
            /* 如果另一个任务不是ScheduledFutureTask类型，则比较它们距离触发还剩多长时间 */
            
            // 获取距任务触发还剩余的时间，然后进行比较
            long diff = getDelay(NANOSECONDS) - other.getDelay(NANOSECONDS);
            
            return (diff<0) ? -1 : (diff>0) ? 1 : 0;
        }
    }
    
    /**
     * Specialized delay queue.
     * To mesh with TPE declarations,
     * this class must be declared as a BlockingQueue<Runnable> even though it can only hold RunnableScheduledFutures.
     */
    /*
     * 周期性任务的有序队列，按触发时间先后排列任务，不到触发时间的任务无法被取出
     *
     * 顺序无界（队列容量支持扩容到Integer.MAX_VALUE）延时阻塞队列，线程安全（锁）
     *
     * 该容器的内部实现为【小顶堆】，存储着RunnableScheduledFuture类的定时任务
     */
    static class DelayedWorkQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable> {
        
        /*
         * A DelayedWorkQueue is based on a heap-based data structure
         * like those in DelayQueue and PriorityQueue, except that
         * every ScheduledFutureTask also records its index into the
         * heap array. This eliminates the need to find a task upon
         * cancellation, greatly speeding up removal (down from O(n)
         * to O(log n)), and reducing garbage retention that would
         * otherwise occur by waiting for the element to rise to top
         * before clearing. But because the queue may also hold
         * RunnableScheduledFutures that are not ScheduledFutureTasks,
         * we are not guaranteed to have such indices available, in
         * which case we fall back to linear search. (We expect that
         * most tasks will not be decorated, and that the faster cases
         * will be much more common.)
         *
         * All heap operations must record index changes -- mainly
         * within siftUp and siftDown. Upon removal, a task's
         * heapIndex is set to -1. Note that ScheduledFutureTasks can
         * appear at most once in the queue (this need not be true for
         * other kinds of tasks or work queues), so are uniquely
         * identified by heapIndex.
         */
        
        private static final int INITIAL_CAPACITY = 16;
        
        // 存储队列元素
        private RunnableScheduledFuture<?>[] queue = new RunnableScheduledFuture<?>[INITIAL_CAPACITY];
        
        // 队列长度
        private int size;
        
        /**
         * Thread designated to wait for the task at the head of the
         * queue.  This variant of the Leader-Follower pattern
         * (http://www.cs.wustl.edu/~schmidt/POSA/POSA2/) serves to
         * minimize unnecessary timed waiting.  When a thread becomes
         * the leader, it waits only for the next delay to elapse, but
         * other threads await indefinitely.  The leader thread must
         * signal some other thread before returning from take() or
         * poll(...), unless some other thread becomes leader in the
         * interim.  Whenever the head of the queue is replaced with a
         * task with an earlier expiration time, the leader field is
         * invalidated by being reset to null, and some waiting
         * thread, but not necessarily the current leader, is
         * signalled.  So waiting threads must be prepared to acquire
         * and lose leadership while waiting.
         */
        // 记录首个被阻塞的线程
        private Thread leader;
        
        
        private final ReentrantLock lock = new ReentrantLock();
        
        /**
         * Condition signalled when a newer task becomes available at the head of the queue
         * or a new thread may need to become leader.
         */
        private final Condition available = lock.newCondition();
        
        
        
        /*▼ 入队/出队 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        // 入队，线程安全。队满时扩容★
        public void put(Runnable e) {
            offer(e);
        }
        
        // 出队，线程安全。队空时阻塞
        public RunnableScheduledFuture<?> take() throws InterruptedException {
            final ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            try {
                for(; ; ) {
                    // 获取队头任务first
                    RunnableScheduledFuture<?> first = queue[0];
                    
                    // 如果队列为空，则需要阻塞
                    if(first == null) {
                        available.await();
                    } else {
                        // 队头任务获取距被触发还剩余的时间
                        long delay = first.getDelay(NANOSECONDS);
                        // 如果已经可以执行了
                        if(delay<=0L) {
                            // 队头任务已经可以开始了，移除并返回队头任务first
                            return finishPoll(first);
                        }
                        
                        // 如果队头任务还未到触发时间，则不需要保持其引用
                        first = null; // don't retain ref while waiting
                        
                        // 如果已有阻塞线程，当前线程需要无限期阻塞，直到被唤醒
                        if(leader != null) {
                            available.await();
                        } else {
                            Thread thisThread = Thread.currentThread();
                            // 记录首个被阻塞的线程
                            leader = thisThread;
                            try {
                                // 在任务触发之前阻塞
                                available.awaitNanos(delay);
                            } finally {
                                // 醒来后，置空leader，重新参与循环，以获取队头任务
                                if(leader == thisThread) {
                                    leader = null;
                                }
                            }
                        }
                    }
                }// for(; ; )
            } finally {
                // 执行完之后，需要唤醒后续等待的阻塞线程
                if(leader == null && queue[0] != null) {
                    available.signal();
                }
                
                lock.unlock();
            }
        }
        
        
        // 入队，线程安全。队满时扩容★
        public boolean offer(Runnable x) {
            if(x == null) {
                throw new NullPointerException();
            }
            
            RunnableScheduledFuture<?> e = (RunnableScheduledFuture<?>) x;
            
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                // 获取当前队列长度
                int i = size;
                if(i >= queue.length) {
                    // 扩容50%
                    grow();
                }
                
                // 任务数量增一
                size = i + 1;
                
                // 第一个任务
                if(i == 0) {
                    queue[0] = e;
                    // 记录该定时任务在延时队列中的索引
                    setIndex(e, 0);
                } else {
                    // 插入。需要从小顶堆的结点i开始，向【上】查找一个合适的位置插入e
                    siftUp(i, e);
                }
                
                // 如果入队任务排在了队首
                if(queue[0] == e) {
                    leader = null;
                    // 队头有新任务达到，唤醒阻塞线程
                    available.signal();
                }
            } finally {
                lock.unlock();
            }
            
            return true;
        }
        
        // 出队，线程安全。队空时返回null
        public RunnableScheduledFuture<?> poll() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                // 获取队头任务first
                RunnableScheduledFuture<?> first = queue[0];
                
                // 如果队列为空，则返回null
                if(first==null){
                    return null;
                }
                
                return first.getDelay(NANOSECONDS)>0
                    ? null  // 队头任务还未到触发时间
                    : finishPoll(first);    // 队头任务已经可以开始了，移除并返回队头任务first
            } finally {
                lock.unlock();
            }
        }
        
        
        // 入队，线程安全。队满时扩容★
        public boolean offer(Runnable e, long timeout, TimeUnit unit) {
            return offer(e);
        }
        
        // 出队，线程安全。队空时阻塞一段时间，超时后无法出队返回null
        public RunnableScheduledFuture<?> poll(long timeout, TimeUnit unit) throws InterruptedException {
            long nanos = unit.toNanos(timeout);
            final ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            try {
                for(; ; ) {
                    // 获取队头任务
                    RunnableScheduledFuture<?> first = queue[0];
                    
                    // 如果队列为空
                    if(first == null) {
                        // 如果已经超时，则返回null
                        if(nanos<=0L) {
                            return null;
                        } else {
                            // 如果还未超时，阻塞一段时间
                            nanos = available.awaitNanos(nanos);
                        }
                    } else {
                        // 队头任务获取距被触发还剩余的时间
                        long delay = first.getDelay(NANOSECONDS);
                        // 如果已经可以执行了
                        if(delay<=0L) {
                            // 队头任务已经可以开始了，移除并返回队头任务first
                            return finishPoll(first);
                        }
                        
                        // 如果已经超时，则返回null
                        if(nanos<=0L) {
                            return null;
                        }
                        
                        // 如果队头任务还未到触发时间，则不需要保持其引用
                        first = null; // don't retain ref while waiting
                        
                        // 如果已有阻塞线程，且未超时，当前线程需要阻塞一段时间，或者中途被唤醒
                        if(nanos<delay || leader != null) {
                            nanos = available.awaitNanos(nanos);
                        } else {
                            Thread thisThread = Thread.currentThread();
                            // 记录首个被阻塞的线程
                            leader = thisThread;
                            try {
                                // 在任务触发之前阻塞
                                long timeLeft = available.awaitNanos(delay);
                                nanos -= delay - timeLeft;  // 剩余的超时时间 = 原有超时时间-阻塞中用掉的时间
                            } finally {
                                // 醒来后，置空leader，重新获取队头任务
                                if(leader == thisThread) {
                                    leader = null;
                                }
                            }
                        }
                    }
                } // for(; ; )
            } finally {
                // 执行完之后，需要唤醒后续排队的阻塞线程
                if(leader == null && queue[0] != null) {
                    available.signal();
                }
                
                lock.unlock();
            }
        }
        
        /*▲ 入队/出队 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 添加/移除 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        // 入队/添加，线程安全。队满时扩容★
        public boolean add(Runnable e) {
            return offer(e);
        }
        
        // 移除，线程安全。移除成功则返回true
        public boolean remove(Object x) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                // 获取任务x的索引
                int i = indexOf(x);
                if(i<0) {
                    return false;
                }
                
                // 更新该定时任务在延时队列中的索引
                setIndex(queue[i], -1);
                
                int s = --size;
                
                // 摘下队尾结点
                RunnableScheduledFuture<?> replacement = queue[s];
                
                queue[s] = null;
                
                if(s!= i) {
                    // 插入。需要从小顶堆的结点i开始，向【下】查找一个合适的位置插入replacement
                    siftDown(i, replacement);
                    
                    /*
                     * 如果待replacement是以i为根结点的小顶堆上的最小值，
                     * 那么不能保证replacement比结点i的父结点元素更大，
                     * 此时需要向上搜寻replacement的一个合适的插入位置
                     */
                    if(queue[i] == replacement) {
                        // 插入。需要从小顶堆的结点i开始，向【上】查找一个合适的位置插入replacement
                        siftUp(i, replacement);
                    }
                }
                
                return true;
            } finally {
                lock.unlock();
            }
        }
        
        // 清空。将所有元素移除，不阻塞，不抛异常
        public void clear() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                for(int i = 0; i<size; i++) {
                    RunnableScheduledFuture<?> t = queue[i];
                    if(t != null) {
                        queue[i] = null;
                        setIndex(t, -1);
                    }
                }
                size = 0;
            } finally {
                lock.unlock();
            }
        }
        
        // 将队列中所有元素移除，并转移到给定的容器当中
        public int drainTo(Collection<? super Runnable> c) {
            return drainTo(c, Integer.MAX_VALUE);
        }
        
        // 将队列中前maxElements个元素移除，并转移到给定的容器当中
        public int drainTo(Collection<? super Runnable> c, int maxElements) {
            Objects.requireNonNull(c);
            if(c == this) {
                throw new IllegalArgumentException();
            }
            
            if(maxElements<=0) {
                return 0;
            }
            
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int n = 0;
                for(RunnableScheduledFuture<?> first;
                    n<maxElements && (first = queue[0]) != null && first.getDelay(NANOSECONDS)<=0;
                ) {
                    // 任务添加到容器中
                    c.add(first);   // In this order, in case add() throws.
                    // 移除队头元素first
                    finishPoll(first);
                    ++n;
                }
                return n;
            } finally {
                lock.unlock();
            }
        }
        
        /*▲ 添加/移除 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 只读 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        // 查看队头任务，如果队列为空，返回null
        public RunnableScheduledFuture<?> peek() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return queue[0];
            } finally {
                lock.unlock();
            }
        }
        
        // 判断队列中是否包含指定的任务
        public boolean contains(Object x) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return indexOf(x) != -1;
            } finally {
                lock.unlock();
            }
        }
        
        // 返回队列中的任务数
        public int size() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return size;
            } finally {
                lock.unlock();
            }
        }
        
        // 计算队列剩余空间，返回Integer.MAX_VALUE代表无限
        public int remainingCapacity() {
            return Integer.MAX_VALUE;
        }
        
        // 判断队列是否为空
        public boolean isEmpty() {
            return size() == 0;
        }
        
        /*▲ 只读 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 迭代 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        // 返回当前队列的迭代器
        public Iterator<Runnable> iterator() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return new Itr(Arrays.copyOf(queue, size));
            } finally {
                lock.unlock();
            }
        }
        
        /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 数组化 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        public Object[] toArray() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return Arrays.copyOf(queue, size, Object[].class);
            } finally {
                lock.unlock();
            }
        }
        
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                if(a.length<size)
                    return (T[]) Arrays.copyOf(queue, size, a.getClass());
                System.arraycopy(queue, 0, a, 0, size);
                if(a.length>size)
                    a[size] = null;
                return a;
            } finally {
                lock.unlock();
            }
        }
        
        /*▲ 数组化 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 小顶堆 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Sifts element added at bottom up to its heap-ordered spot.
         * Call only when holding lock.
         */
        // 插入。需要从小顶堆的结点i开始，向【上】查找一个合适的位置插入key
        private void siftUp(int i, RunnableScheduledFuture<?> key) {
            while(i>0) {
                // 获取父结点索引
                int parent = (i - 1) >>> 1;
                
                // 父结点
                RunnableScheduledFuture<?> e = queue[parent];
                
                // 如果待插入元素大于父节点中的元素，则退出循环
                if(key.compareTo(e) >= 0) {
                    break;
                }
                
                // 子结点保存父结点中的元素
                queue[i] = e;
                setIndex(e, i);
                
                // 向上搜寻合适的插入位置
                i = parent;
            }
            
            // 将元素key插入到合适的位置
            queue[i] = key;
            setIndex(key, i);
        }
        
        /**
         * Sifts element added at top down to its heap-ordered spot.
         * Call only when holding lock.
         */
        // 插入。需要从小顶堆的结点i开始，向【下】查找一个合适的位置插入key
        private void siftDown(int i, RunnableScheduledFuture<?> key) {
            // 最多搜索一半元素
            int half = size >>> 1;
            
            while(i<half) {
                int min = (i << 1) + 1;    // 左结点索引
                int right = min + 1;       // 右结点索引
                
                // 假设左结点为较小的结点
                RunnableScheduledFuture<?> m = queue[min];
                
                // 如果右结点更小
                if(right<size && m.compareTo(queue[right])>0) {
                    // 更新min指向子结点中较小的结点
                    m = queue[min = right];
                }
                
                // 如果待插入元素小于子结点中较小的元素，则退出循环
                if(key.compareTo(m)<=0) {
                    break;
                }
                
                // 父结点位置保存子结点中较小的元素
                queue[i] = m;
                setIndex(m, i);
                
                // 向下搜寻合适的插入位置
                i = min;
            }
            
            // 将元素key插入到合适的位置
            queue[i] = key;
            setIndex(key, i);
        }
        
        /*▲ 小顶堆 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /**
         * Sets f's heapIndex if it is a ScheduledFutureTask.
         */
        // 更新定时任务的heapIndex域（记录定时任务在延时队列中的索引）
        private static void setIndex(RunnableScheduledFuture<?> f, int idx) {
            if(f instanceof ScheduledFutureTask) {
                ((ScheduledFutureTask) f).heapIndex = idx;
            }
        }
        
        /**
         * Finds index of given object, or -1 if absent.
         */
        // 获取元素x的索引
        private int indexOf(Object x) {
            if(x != null) {
                if(x instanceof ScheduledFutureTask) {
                    int i = ((ScheduledFutureTask) x).heapIndex;
                    // Sanity check; x could conceivably be a
                    // ScheduledFutureTask from some other pool.
                    if(i >= 0 && i<size && queue[i] == x) {
                        return i;
                    }
                } else {
                    for(int i = 0; i<size; i++) {
                        if(x.equals(queue[i])) {
                            return i;
                        }
                    }
                }
            }
            return -1;
        }
        
        /**
         * Resizes the heap array.  Call only when holding lock.
         */
        // 扩容50%
        private void grow() {
            int oldCapacity = queue.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1); // 扩容50%
            if(newCapacity<0) { // 溢出
                newCapacity = Integer.MAX_VALUE;
            }
            queue = Arrays.copyOf(queue, newCapacity);
        }
        
        /**
         * Performs common bookkeeping for poll and take:
         * Replaces first element with last and sifts it down.
         * Call only when holding lock.
         *
         * @param f the task to remove and return
         */
        // 移除并返回队头元素f
        private RunnableScheduledFuture<?> finishPoll(RunnableScheduledFuture<?> f) {
            int s = --size;
            
            // 获取队尾元素
            RunnableScheduledFuture<?> x = queue[s];
            
            queue[s] = null;
            
            if(s != 0) {
                // 插入。需要从小顶堆的结点0开始，向【下】查找一个合适的位置插入x
                siftDown(0, x);
            }
            
            setIndex(f, -1);
            
            return f;
        }
        
        
        
        /**
         * Snapshot iterator that works off copy of underlying q array.
         */
        // 用于当前延时队列的外部迭代器
        private class Itr implements Iterator<Runnable> {
            final RunnableScheduledFuture<?>[] array;
            int cursor;        // index of next element to return; initially 0
            int lastRet = -1;  // index of last element returned; -1 if no such
            
            Itr(RunnableScheduledFuture<?>[] array) {
                this.array = array;
            }
            
            public boolean hasNext() {
                return cursor<array.length;
            }
            
            public Runnable next() {
                if(cursor >= array.length) {
                    throw new NoSuchElementException();
                }
                return array[lastRet = cursor++];
            }
            
            public void remove() {
                if(lastRet<0) {
                    throw new IllegalStateException();
                }
                DelayedWorkQueue.this.remove(array[lastRet]);
                lastRet = -1;
            }
        }
    }
}
