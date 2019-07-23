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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.ForkJoinPool.WorkQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract base class for tasks that run within a {@link ForkJoinPool}.
 * A {@code ForkJoinTask} is a thread-like entity that is much
 * lighter weight than a normal thread.  Huge numbers of tasks and
 * subtasks may be hosted by a small number of actual threads in a
 * ForkJoinPool, at the price of some usage limitations.
 *
 * <p>A "main" {@code ForkJoinTask} begins execution when it is
 * explicitly submitted to a {@link ForkJoinPool}, or, if not already
 * engaged in a ForkJoin computation, commenced in the {@link
 * ForkJoinPool#commonPool()} via {@link #fork}, {@link #invoke}, or
 * related methods.  Once started, it will usually in turn start other
 * subtasks.  As indicated by the name of this class, many programs
 * using {@code ForkJoinTask} employ only methods {@link #fork} and
 * {@link #join}, or derivatives such as {@link
 * #invokeAll(ForkJoinTask...) invokeAll}.  However, this class also
 * provides a number of other methods that can come into play in
 * advanced usages, as well as extension mechanics that allow support
 * of new forms of fork/join processing.
 *
 * <p>A {@code ForkJoinTask} is a lightweight form of {@link Future}.
 * The efficiency of {@code ForkJoinTask}s stems from a set of
 * restrictions (that are only partially statically enforceable)
 * reflecting their main use as computational tasks calculating pure
 * functions or operating on purely isolated objects.  The primary
 * coordination mechanisms are {@link #fork}, that arranges
 * asynchronous execution, and {@link #join}, that doesn't proceed
 * until the task's result has been computed.  Computations should
 * ideally avoid {@code synchronized} methods or blocks, and should
 * minimize other blocking synchronization apart from joining other
 * tasks or using synchronizers such as Phasers that are advertised to
 * cooperate with fork/join scheduling. Subdividable tasks should also
 * not perform blocking I/O, and should ideally access variables that
 * are completely independent of those accessed by other running
 * tasks. These guidelines are loosely enforced by not permitting
 * checked exceptions such as {@code IOExceptions} to be
 * thrown. However, computations may still encounter unchecked
 * exceptions, that are rethrown to callers attempting to join
 * them. These exceptions may additionally include {@link
 * RejectedExecutionException} stemming from internal resource
 * exhaustion, such as failure to allocate internal task
 * queues. Rethrown exceptions behave in the same way as regular
 * exceptions, but, when possible, contain stack traces (as displayed
 * for example using {@code ex.printStackTrace()}) of both the thread
 * that initiated the computation as well as the thread actually
 * encountering the exception; minimally only the latter.
 *
 * <p>It is possible to define and use ForkJoinTasks that may block,
 * but doing so requires three further considerations: (1) Completion
 * of few if any <em>other</em> tasks should be dependent on a task
 * that blocks on external synchronization or I/O. Event-style async
 * tasks that are never joined (for example, those subclassing {@link
 * CountedCompleter}) often fall into this category.  (2) To minimize
 * resource impact, tasks should be small; ideally performing only the
 * (possibly) blocking action. (3) Unless the {@link
 * ForkJoinPool.ManagedBlocker} API is used, or the number of possibly
 * blocked tasks is known to be less than the pool's {@link
 * ForkJoinPool#getParallelism} level, the pool cannot guarantee that
 * enough threads will be available to ensure progress or good
 * performance.
 *
 * <p>The primary method for awaiting completion and extracting
 * results of a task is {@link #join}, but there are several variants:
 * The {@link Future#get} methods support interruptible and/or timed
 * waits for completion and report results using {@code Future}
 * conventions. Method {@link #invoke} is semantically
 * equivalent to {@code fork(); join()} but always attempts to begin
 * execution in the current thread. The "<em>quiet</em>" forms of
 * these methods do not extract results or report exceptions. These
 * may be useful when a set of tasks are being executed, and you need
 * to delay processing of results or exceptions until all complete.
 * Method {@code invokeAll} (available in multiple versions)
 * performs the most common form of parallel invocation: forking a set
 * of tasks and joining them all.
 *
 * <p>In the most typical usages, a fork-join pair act like a call
 * (fork) and return (join) from a parallel recursive function. As is
 * the case with other forms of recursive calls, returns (joins)
 * should be performed innermost-first. For example, {@code a.fork();
 * b.fork(); b.join(); a.join();} is likely to be substantially more
 * efficient than joining {@code a} before {@code b}.
 *
 * <p>The execution status of tasks may be queried at several levels
 * of detail: {@link #isDone} is true if a task completed in any way
 * (including the case where a task was cancelled without executing);
 * {@link #isCompletedNormally} is true if a task completed without
 * cancellation or encountering an exception; {@link #isCancelled} is
 * true if the task was cancelled (in which case {@link #getException}
 * returns a {@link CancellationException}); and
 * {@link #isCompletedAbnormally} is true if a task was either
 * cancelled or encountered an exception, in which case {@link
 * #getException} will return either the encountered exception or
 * {@link CancellationException}.
 *
 * <p>The ForkJoinTask class is not usually directly subclassed.
 * Instead, you subclass one of the abstract classes that support a
 * particular style of fork/join processing, typically {@link
 * RecursiveAction} for most computations that do not return results,
 * {@link RecursiveTask} for those that do, and {@link
 * CountedCompleter} for those in which completed actions trigger
 * other actions.  Normally, a concrete ForkJoinTask subclass declares
 * fields comprising its parameters, established in a constructor, and
 * then defines a {@code compute} method that somehow uses the control
 * methods supplied by this base class.
 *
 * <p>Method {@link #join} and its variants are appropriate for use
 * only when completion dependencies are acyclic; that is, the
 * parallel computation can be described as a directed acyclic graph
 * (DAG). Otherwise, executions may encounter a form of deadlock as
 * tasks cyclically wait for each other.  However, this framework
 * supports other methods and techniques (for example the use of
 * {@link Phaser}, {@link #helpQuiesce}, and {@link #complete}) that
 * may be of use in constructing custom subclasses for problems that
 * are not statically structured as DAGs. To support such usages, a
 * ForkJoinTask may be atomically <em>tagged</em> with a {@code short}
 * value using {@link #setForkJoinTaskTag} or {@link
 * #compareAndSetForkJoinTaskTag} and checked using {@link
 * #getForkJoinTaskTag}. The ForkJoinTask implementation does not use
 * these {@code protected} methods or tags for any purpose, but they
 * may be of use in the construction of specialized subclasses.  For
 * example, parallel graph traversals can use the supplied methods to
 * avoid revisiting nodes/tasks that have already been processed.
 * (Method names for tagging are bulky in part to encourage definition
 * of methods that reflect their usage patterns.)
 *
 * <p>Most base support methods are {@code final}, to prevent
 * overriding of implementations that are intrinsically tied to the
 * underlying lightweight task scheduling framework.  Developers
 * creating new basic styles of fork/join processing should minimally
 * implement {@code protected} methods {@link #exec}, {@link
 * #setRawResult}, and {@link #getRawResult}, while also introducing
 * an abstract computational method that can be implemented in its
 * subclasses, possibly relying on other {@code protected} methods
 * provided by this class.
 *
 * <p>ForkJoinTasks should perform relatively small amounts of
 * computation. Large tasks should be split into smaller subtasks,
 * usually via recursive decomposition. As a very rough rule of thumb,
 * a task should perform more than 100 and less than 10000 basic
 * computational steps, and should avoid indefinite looping. If tasks
 * are too big, then parallelism cannot improve throughput. If too
 * small, then memory and internal task maintenance overhead may
 * overwhelm processing.
 *
 * <p>This class provides {@code adapt} methods for {@link Runnable}
 * and {@link Callable}, that may be of use when mixing execution of
 * {@code ForkJoinTasks} with other kinds of tasks. When all tasks are
 * of this form, consider using a pool constructed in <em>asyncMode</em>.
 *
 * <p>ForkJoinTasks are {@code Serializable}, which enables them to be
 * used in extensions such as remote execution frameworks. It is
 * sensible to serialize tasks only before or after, but not during,
 * execution. Serialization is not relied on during execution itself.
 *
 * @since 1.7
 * @author Doug Lea
 */
/*
 * 并行任务，有多种类型的实现：
 *
 * ForkJoinTask.AdaptedCallable
 * ForkJoinTask.AdaptedRunnable
 * ForkJoinTask.AdaptedRunnableAction
 * ForkJoinTask.RunnableExecuteAction
 *
 * CountedCompleter
 * RecursiveTask
 * RecursiveAction
 *
 * CompletableFuture.AsyncRun
 * CompletableFuture.AsyncSupply
 * CompletableFuture.Completion
 *
 * SubmissionPublisher.ConsumerTask
 */
public abstract class ForkJoinTask<V> implements Future<V>, Serializable {
    private static final long serialVersionUID = -7721805057305804111L;
    
    /*
     * See the internal documentation of class ForkJoinPool for a
     * general implementation overview.  ForkJoinTasks are mainly
     * responsible for maintaining their "status" field amidst relays
     * to methods in ForkJoinWorkerThread and ForkJoinPool.
     *
     * The methods of this class are more-or-less layered into
     * (1) basic status maintenance
     * (2) execution and awaiting completion
     * (3) user-level methods that additionally report results.
     * This is sometimes hard to see because this file orders exported
     * methods in a way that flows well in javadocs.
     */
    
    /**
     * The status field holds run control status bits packed into a
     * single int to ensure atomicity.  Status is initially zero, and
     * takes on nonnegative values until completed, upon which it
     * holds (sign bit) DONE, possibly with ABNORMAL (cancelled or
     * exceptional) and THROWN (in which case an exception has been
     * stored). Tasks with dependent blocked waiting joiners have the
     * SIGNAL bit set.  Completion of a task with SIGNAL set awakens
     * any waiters via notifyAll. (Waiters also help signal others
     * upon completion.)
     *
     * These control bits occupy only (some of) the upper half (16
     * bits) of status field. The lower bits are used for user-defined
     * tags.
     */
    // 任务状态信息，包含DONE|ABNORMAL|THROWN|SIGNAL
    volatile int status; // accessed directly by pool and workers
    
    private static final int DONE     = 1 << 31; // [已完成]，must be negative
    private static final int ABNORMAL = 1 << 18; // [非正常完成]，set atomically with DONE
    private static final int THROWN   = 1 << 17; // [有异常]，set atomically with ABNORMAL
    private static final int SIGNAL   = 1 << 16; // [等待]，true if joiner waiting
    
    // 任务状态掩码
    private static final int SMASK    = 0xffff;  // short bits for tags
    
    
    /**
     * Hash table of exceptions thrown by tasks, to enable reporting
     * by callers. Because exceptions are rare, we don't directly keep
     * them with task objects, but instead use a weak ref table.  Note
     * that cancellation exceptions don't appear in the table, but are
     * instead recorded as status values.
     *
     * The exception table has a fixed capacity.
     */
    // 由异常记录哈希表，每个结点存储的是键值对<任务, 异常>
    private static final ExceptionNode[] exceptionTable = new ExceptionNode[32];
    
    /** Reference queue of stale exceptionally completed tasks. */
    // 存储GC之后“已失效”的异常记录
    private static final ReferenceQueue<ForkJoinTask<?>> exceptionTableRefQueue = new ReferenceQueue<>();
    
    /** Lock protecting access to exceptionTable. */
    // 操作异常信息时候需要的锁
    private static final ReentrantLock exceptionTableLock = new ReentrantLock();
    
    
    // VarHandle mechanics
    private static final VarHandle STATUS;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATUS = l.findVarHandle(ForkJoinTask.class, "status", int.class);
        } catch(ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    
    
    /*▼ 信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the pool hosting the current thread, or {@code null}
     * if the current thread is executing outside of any ForkJoinPool.
     *
     * <p>This method returns {@code null} if and only if {@link
     * #inForkJoinPool} returns {@code false}.
     *
     * @return the pool, or {@code null} if none
     */
    // 返回【工作线程】所属的工作池
    public static ForkJoinPool getPool() {
        Thread t = Thread.currentThread();
        return (t instanceof ForkJoinWorkerThread) ? ((ForkJoinWorkerThread) t).pool : null;
    }
    
    /**
     * Returns {@code true} if the current thread is a {@link
     * ForkJoinWorkerThread} executing as a ForkJoinPool computation.
     *
     * @return {@code true} if the current thread is a {@link
     * ForkJoinWorkerThread} executing as a ForkJoinPool computation,
     * or {@code false} otherwise
     */
    // 判断当前线程是否属于【工作线程】
    public static boolean inForkJoinPool() {
        return Thread.currentThread() instanceof ForkJoinWorkerThread;
    }
    
    /*▲ 信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 分发/移除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Arranges to asynchronously execute this task in the pool the
     * current task is running in, if applicable, or using the {@link
     * ForkJoinPool#commonPool()} if not {@link #inForkJoinPool}.  While
     * it is not necessarily enforced, it is a usage error to fork a
     * task more than once unless it has completed and been
     * reinitialized.  Subsequent modifications to the state of this
     * task or any data it operates on are not necessarily
     * consistently observable by any thread other than the one
     * executing it unless preceded by a call to {@link #join} or
     * related methods, or a call to {@link #isDone} returning {@code
     * true}.
     *
     * @return {@code this}, to simplify usage
     */
    /*
     * 分发任务。分发任务去排队，并创建/唤醒【工作线程】
     *
     * 将当前任务放入当前线程所辖【队列】的top处排队，
     * 如果在【工作线程】fork任务，该任务会放入【工作队列】top处
     * 如果在【提交线程】fork任务，该任务会统一放入【共享工作池】的【共享队列】top处
     *
     * 任务进入排队后，创建/唤醒【工作线程】
     */
    public final ForkJoinTask<V> fork() {
        // 获取当前线程的引用
        Thread thread = Thread.currentThread();
        
        if(thread instanceof ForkJoinWorkerThread) {
            // 分发任务。task进入【工作队列】top处排队
            ((ForkJoinWorkerThread) thread).workQueue.push(this);
        } else {
            // 分发任务。task进入【共享工作池】的【共享队列】top处排队
            ForkJoinPool.common.externalPush(this);
        }
        
        // 当前任务就位后，返回任务自身
        return this;
    }
    
    /**
     * Tries to unschedule this task for execution. This method will
     * typically (but is not guaranteed to) succeed if this task is
     * the most recently forked task by the current thread, and has
     * not commenced executing in another thread.  This method may be
     * useful when arranging alternative local processing of tasks
     * that could have been, but were not, stolen.
     *
     * @return {@code true} if unforked
     */
    // 移除任务，将任务从【队列】的top处移除
    public boolean tryUnfork() {
        Thread thread = Thread.currentThread();
        
        if(thread instanceof ForkJoinWorkerThread) {
            // 移除任务。【工作线程】尝试将指定的task从【工作队列】top处移除，返回值代表是否成功移除
            return ((ForkJoinWorkerThread) thread).workQueue.tryUnpush(this);
        }
        
        // 移除任务。【提交线程】尝试将指定的task从【共享工作池】上的【共享队列】top处移除，返回值代表是否成功移除
        return ForkJoinPool.common.tryExternalUnpush(this);
    }
    
    /*▲ 分发/移除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 执行 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Immediately performs the base action of this task and returns
     * true if, upon return from this method, this task is guaranteed
     * to have completed normally. This method may return false
     * otherwise, to indicate that this task is not necessarily
     * complete (or is not known to be complete), for example in
     * asynchronous actions that require explicit invocations of
     * completion methods. This method may also throw an (unchecked)
     * exception to indicate abnormal exit. This method is designed to
     * support extensions, and should not in general be called
     * otherwise.
     *
     * @return {@code true} if this task is known to have completed normally
     */
    // 执行任务，执行细节由子类实现。返回值指示任务是否正常完成
    protected abstract boolean exec();
    
    /**
     * Primary execution method for stolen tasks. Unless done, calls
     * exec and records status if completed, but doesn't wait for
     * completion otherwise.
     *
     * @return status on exit from this method
     */
    // 执行任务，并返回任务执行后的状态。任务具体的执行逻辑由子类实现
    final int doExec() {
        int s = status;
        
        boolean completed;
        
        // 如果任务没有执行完，则执行任务
        if (s >= 0) {
            try {
                // 执行任务，执行细节由子类实现
                completed = exec();
            } catch (Throwable rex) {
                completed = false;
                // 如果执行期间出现了异常，需要添加异常标记
                s = setExceptionalCompletion(rex);
            }
            
            // 如果任务已经执行完了
            if (completed) {
                // 将当前任务标记为已完成状态
                s = setDone();
            }
        }
        
        return s;
    }
    
    /**
     * Implementation for invoke, quietlyInvoke.
     *
     * @return status upon completion
     */
    /*
     * 直接执行任务，最后返回任务状态。必要时，需要等待其他任务的完成
     *
     * 有些任务还没执行完就会返回状态码，
     * 这可能会使当前线程进入wait状态，并标记任务为SIGNAL状态，
     * 直到任务执行完之后，当前线程被唤醒并返回任务执行后的状态
     */
    private int doInvoke() {
        // 执行任务，返回任务执行后的状态。任务具体的执行逻辑由子类实现
        int s = doExec();
        
        // 如果任务已经完成，直接返回任务的状态
        if(s<0) {
            return s;
        }
        
        /* 至此，任务返回了状态码，但是没有[已完成]标记，此时需要继续处理 */
        
        Thread thread = Thread.currentThread();
        
        // 如果当前操作发生在【工作线程】中
        if(thread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread) thread;
            
            // 【工作线程】尝试加速task的完成，如果无法加速，则当前的【工作线程】考虑进入wait状态，直到task完成后被唤醒
            return wt.pool.awaitJoin(wt.workQueue, this, 0L);
        }
        
        // 如果当前操作发生在【提交线程】中，【提交线程】尝试加速当前任务的完成，如果无法加速，则进入wait状态等待task完成
        return externalAwaitDone();
    }
    
    /**
     * Implementation for join, get, quietlyJoin.
     * Directly handles only cases of already-completed, external wait, and unfork+exec.
     * Others are relayed to ForkJoinPool.awaitJoin.
     *
     * @return status upon completion
     */
    // 从【工作队列】的top处取出当前任务并执行，最后返回任务状态。必要时，需要等待其他任务的完成
    private int doJoin() {
        // 获取任务当前的状态
        int s = status;
        
        // status<0代表已经执行完成
        if(s < 0){
            // 如果当前任务已经执行完了，就返回任务的状态
            return s;
        }
        
        Thread thread = Thread.currentThread();
        
        // 如果当前操作发生在【工作线程】中
        if(thread instanceof ForkJoinWorkerThread){
            // 获取当前的【工作线程】
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread)thread;
            
            // 移除任务。【工作线程】尝试将指定的task从【工作队列】top处移除，返回值代表是否成功移除
            if(wt.workQueue.tryUnpush(this)){
                
                // 如果成功移除了任务，说明该任务还未被执行，那么从这里开始执行任务，并返回任务在执行后的状态
                s = doExec();
                
                // status<0代表已经执行完成
                if(s < 0){
                    // 如果当前任务已经执行完了，就返回任务的状态
                    return s;
                }
            }
            
            /*
             * 至此，可能是没有在top处取到当前任务（可能不在栈顶，也可能被别的【工作线程】窃取了），
             * 也可能是任务没有被标记为[已完成]（是否真的完成未知）
             */
            
            // 【工作线程】尝试加速task的完成，如果无法加速，则当前的【工作线程】考虑进入wait状态，直到task完成后被唤醒
            return wt.pool.awaitJoin(wt.workQueue, this, 0L);
        }
        
        // 如果当前操作发生在【提交线程】中，【提交线程】尝试加速当前任务的完成，如果无法加速，则进入wait状态等待task完成
        return externalAwaitDone();
    }
    
    /**
     * Commences performing this task, awaits its completion if necessary, and returns its result,
     * or throws an (unchecked) {@code RuntimeException} or {@code Error} if the underlying computation did so.
     *
     * @return the computed result
     */
    // 直接执行任务，返回任务的执行结果。必要时，需要等待其他任务的完成
    public final V invoke() {
        // 直接执行任务，最后返回任务状态。必要时，需要等待其他任务的完成
        int s = doInvoke();
        
        // 如果任务带有[非正常完成]的标记，则需要报告异常
        if((s & ABNORMAL) != 0) {
            // 报告异常信息
            reportException(s);
        }
        
        // 返回当前任务的执行结果
        return getRawResult();
    }
    
    /**
     * Returns the result of the computation when it
     * {@linkplain #isDone is done}.
     * This method differs from {@link #get()} in that abnormal
     * completion results in {@code RuntimeException} or {@code Error},
     * not {@code ExecutionException}, and that interrupts of the
     * calling thread do <em>not</em> cause the method to abruptly
     * return by throwing {@code InterruptedException}.
     *
     * @return the computed result
     */
    // 从【工作队列】的top处取出当前任务并执行，最后返回任务的执行结果。必要时，需要等待其他任务的完成
    public final V join() {
        // 从【工作队列】的top处取出当前任务并执行，最后返回任务状态。必要时，需要等待其他任务的完成
        int s = doJoin();
        
        // 如果任务带有[非正常完成]的标记，则需要报告异常
        if((s & ABNORMAL) != 0) {
            // 报告异常信息
            reportException(s);
        }
        
        // 返回当前任务的执行结果
        return getRawResult();
    }
    
    /**
     * Forks the given tasks, returning when {@code isDone} holds for each task or an (unchecked) exception is encountered,
     * in which case the exception is rethrown.
     * If more than one task encounters an exception, then this method throws any one of these exceptions.
     * If any task encounters an exception, the other may be cancelled.
     * However, the execution status of individual tasks is not guaranteed upon exceptional return.
     * The status of each task may be obtained using {@link #getException()} and related methods to check if they have been cancelled,
     * completed normally or exceptionally, or left unprocessed.
     *
     * @param task1 the first task
     * @param task2 the second task
     *
     * @throws NullPointerException if any task is null
     */
    // 同时执行两个任务。task1选择doInvoke()，而task2选择fork()/doJoin()
    public static void invokeAll(ForkJoinTask<?> task1, ForkJoinTask<?> task2) {
        
        task2.fork();
        
        // 直接执行任务，最后返回任务状态。必要时，需要等待其他任务的完成
        int s1 = task1.doInvoke();
        
        // 如果任务带有[非正常完成]的标记，则需要报告异常
        if((s1 & ABNORMAL) != 0) {
            // 报告异常信息
            task1.reportException(s1);
        }
        
        // 从【工作队列】的top处取出当前任务并执行，最后返回任务状态。必要时，需要等待其他任务的完成
        int s2 = task2.doJoin();
        
        // 如果任务带有[非正常完成]的标记，则需要报告异常
        if((s2 & ABNORMAL) != 0) {
            // 报告异常信息
            task2.reportException(s2);
        }
    }
    
    /**
     * Forks the given tasks, returning when {@code isDone} holds for
     * each task or an (unchecked) exception is encountered, in which
     * case the exception is rethrown. If more than one task
     * encounters an exception, then this method throws any one of
     * these exceptions. If any task encounters an exception, others
     * may be cancelled. However, the execution status of individual
     * tasks is not guaranteed upon exceptional return. The status of
     * each task may be obtained using {@link #getException()} and
     * related methods to check if they have been cancelled, completed
     * normally or exceptionally, or left unprocessed.
     *
     * @param tasks the tasks
     *
     * @throws NullPointerException if any task is null
     */
    // 批量执行任务。第一个任务使用doInvoke()，其他任务使用fork()/doJoin()
    public static void invokeAll(ForkJoinTask<?>... tasks) {
        Throwable ex = null;
        
        for(int i = tasks.length - 1; i >= 0; --i) {
            ForkJoinTask<?> task = tasks[i];
            
            if(task == null) {
                if(ex == null) {
                    ex = new NullPointerException();
                }
            } else if(i != 0) {
                task.fork();
            } else if((task.doInvoke() & ABNORMAL) != 0 && ex == null) {
                // 如果任务被非正常关闭，则返回其异常信息
                ex = task.getException();
            }
        }
        
        for(int i = 1; i<=tasks.length - 1; ++i) {
            ForkJoinTask<?> task = tasks[i];
            if(task != null) {
                if(ex != null) {
                    // 取消任务，即将当前任务标记为[已完成]|[非正常完成]状态
                    task.cancel(false);
                } else if((task.doJoin() & ABNORMAL) != 0) {
                    // 如果任务被非正常关闭，则返回其异常信息
                    ex = task.getException();
                }
            }
        }
        
        if(ex != null) {
            rethrow(ex);
        }
    }
    
    /**
     * Forks all tasks in the specified collection, returning when
     * {@code isDone} holds for each task or an (unchecked) exception
     * is encountered, in which case the exception is rethrown. If
     * more than one task encounters an exception, then this method
     * throws any one of these exceptions. If any task encounters an
     * exception, others may be cancelled. However, the execution
     * status of individual tasks is not guaranteed upon exceptional
     * return. The status of each task may be obtained using {@link
     * #getException()} and related methods to check if they have been
     * cancelled, completed normally or exceptionally, or left
     * unprocessed.
     *
     * @param tasks the collection of tasks
     * @param <T>   the type of the values returned from the tasks
     *
     * @return the tasks argument, to simplify usage
     *
     * @throws NullPointerException if tasks or any element are null
     */
    // 批量执行任务。第一个任务使用doInvoke()，其他任务使用fork()/doJoin()。最后返回任务的集合。
    public static <T extends ForkJoinTask<?>> Collection<T> invokeAll(Collection<T> tasks) {
        if(!(tasks instanceof RandomAccess) || !(tasks instanceof List<?>)) {
            // 将所有task存入数组后再批量执行
            invokeAll(tasks.toArray(new ForkJoinTask<?>[0]));
            return tasks;
        }
        
        @SuppressWarnings("unchecked")
        List<? extends ForkJoinTask<?>> ts = (List<? extends ForkJoinTask<?>>) tasks;
        Throwable ex = null;
        int last = ts.size() - 1;
        for(int i = last; i >= 0; --i) {
            ForkJoinTask<?> t = ts.get(i);
            if(t == null) {
                if(ex == null) {
                    ex = new NullPointerException();
                }
            } else if(i != 0) {
                t.fork();
            } else if((t.doInvoke() & ABNORMAL) != 0 && ex == null) {
                // 如果任务被非正常关闭，则返回其异常信息
                ex = t.getException();
            }
        }
        
        for(int i = 1; i<=last; ++i) {
            ForkJoinTask<?> t = ts.get(i);
            if(t != null) {
                if(ex != null) {
                    // 取消任务，即将当前任务标记为[已完成]|[非正常完成]状态
                    t.cancel(false);
                } else {if((t.doJoin() & ABNORMAL) != 0) {
                    // 如果任务被非正常关闭，则返回其异常信息
                    ex = t.getException();
                }}
            }
        }
        
        if(ex != null) {
            rethrow(ex);
        }
        
        return tasks;
    }
    
    /**
     * Joins this task, without returning its result or throwing its
     * exception. This method may be useful when processing
     * collections of tasks when some have been cancelled or otherwise
     * known to have aborted.
     */
    // 从【工作队列】的top处取出当前任务并执行，不返回任务状态。必要时，需要等待其他任务的完成
    public final void quietlyJoin() {
        doJoin();
    }
    
    /**
     * Commences performing this task and awaits its completion if
     * necessary, without returning its result or throwing its
     * exception.
     */
    // 直接执行任务，不返回任务状态。必要时，需要等待其他任务的完成
    public final void quietlyInvoke() {
        doInvoke();
    }
    
    /*▲ 执行 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 加速完成任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Blocks a non-worker-thread until completion or interruption.
     */
    // 【提交线程】先尝试加速当前任务的完成。如果加速失败，则转入wait，等待当前任务执行完后再被唤醒
    private int externalInterruptibleAwaitDone() throws InterruptedException {
        // 【提交线程】尝试加速当前任务的完成
        int s = tryExternalHelp();
        
        // 等待当前任务的完成
        if(s >= 0 && (s = (int) STATUS.getAndBitwiseOr(this, SIGNAL)) >= 0) {
            synchronized(this) {
                for(; ; ) {
                    if((s = status) >= 0) {
                        wait(0L);
                    } else {
                        notifyAll();
                        break;
                    }
                }
            }
        } else {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        
        return s;
    }
    
    /**
     * Tries to help with tasks allowed for external callers.
     *
     * @return current status
     */
    // 【提交线程】尝试加速当前任务的完成
    private int tryExternalHelp() {
        // 如果任务已完成，直接返回状态标记
        if(status<0) {
            return status;
        }
        
        // 如果是CC型任务
        if(this instanceof CountedCompleter) {
            // 【提交线程】尝试加速task的完成，并在最终返回任务的状态
            return ForkJoinPool.common.externalHelpComplete((CountedCompleter<?>) this, 0);
        }
        
        // 移除任务。【提交线程】尝试将当前task从【共享工作池】上的【共享队列】top处移除，返回值代表是否成功移除
        if(ForkJoinPool.common.tryExternalUnpush(this)) {
            // 执行任务，返回任务执行后的状态
            return doExec();
        }
        
        return 0;
    }
    
    /**
     * Possibly executes tasks until the pool hosting the current task
     * {@linkplain ForkJoinPool#isQuiescent is quiescent}.  This
     * method may be of use in designs in which many tasks are forked,
     * but none are explicitly joined, instead executing them until
     * all are processed.
     */
    // 促进【工作线程】走向空闲
    public static void helpQuiesce() {
        Thread t = Thread.currentThread();
        
        if(t instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
            // 【工作线程】促进【工作队列】中任务的完成（使【工作线程】走向空闲）
            wt.pool.helpQuiescePool(wt.workQueue);
        } else {
            // 等待【共享工作池】上的所有【工作队列】变为空闲
            ForkJoinPool.quiesceCommonPool();
        }
    }
    
    /*▲ 加速完成任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 阻塞 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * If not done, sets SIGNAL status and performs Object.wait(timeout).
     * This task may or may not be done on exit. Ignores interrupts.
     *
     * @param timeout using Object.wait conventions.
     */
    // 如果任务还未完成，将任务状态标记为[等待]，并进入wait状态
    final void internalWait(long timeout) {
        if((int) STATUS.getAndBitwiseOr(this, SIGNAL) >= 0) {
            synchronized(this) {
                if(status >= 0) {
                    try {
                        wait(timeout);
                    } catch(InterruptedException ie) {
                    }
                } else {
                    notifyAll();
                }
            }
        }
    }
    
    /**
     * Blocks a non-worker-thread until completion.
     * @return status upon completion
     */
    /*
     * 【提交线程】尝试加速当前任务的完成
     * 如果加速失败，则使【提交线程】进入wait状态，
     * 直到该任务完成后才唤醒该线程
     */
    private int externalAwaitDone() {
        // 【提交线程】尝试加速当前任务的完成
        int s = tryExternalHelp();
        
        if (s >= 0) {
            // 添加阻塞标记
            s = (int)STATUS.getAndBitwiseOr(this, SIGNAL);
            
            if(s>=0){
                boolean interrupted = false;
                synchronized (this) {
                    // 陷入死循环
                    while(true) {
                        s = status;
                        
                        if (s >= 0) {
                            try {
                                wait(0L);
                            } catch (InterruptedException ie) {
                                interrupted = true;
                            }
                        } else {
                            notifyAll();
                            break;
                        }
                    }
                } // while(true)
                
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return s;
    }
    
    /*▲ 阻塞 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 完成 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 当前任务是否[已完成]
    public final boolean isDone() {
        return status<0;
    }
    
    /**
     * Returns {@code true} if this task completed without throwing an
     * exception and was not cancelled.
     *
     * @return {@code true} if this task completed without throwing an
     * exception and was not cancelled
     */
    // 当前任务[已完成]，且是正常完成
    public final boolean isCompletedNormally() {
        return (status & (DONE | ABNORMAL)) == DONE;
    }
    
    /**
     * Returns {@code true} if this task threw an exception or was cancelled.
     *
     * @return {@code true} if this task threw an exception or was cancelled
     */
    // 当前任务是否[非正常完成]
    public final boolean isCompletedAbnormally() {
        return (status & ABNORMAL) != 0;
    }
    
    /**
     * Sets DONE status and wakes up threads waiting to join this task.
     *
     * @return status on exit
     */
    // 将当前任务标记为[已完成]状态，如果当前任务带有SIGNAL标记，则需唤醒所有处于wait的线程
    private int setDone() {
        // 更新任务状态为已完成
        int s= (int) STATUS.getAndBitwiseOr(this, DONE);
        
        // 如果该任务带有阻塞标记，则唤醒全部线程
        if((s & SIGNAL) != 0) {
            synchronized(this) {
                // 被wait的任务无法定点唤醒，只能全部唤醒
                notifyAll();
            }
        }
        
        // 添加[已完成]标记
        return s | DONE;
    }
    
    /**
     * Completes this task normally without setting a value.
     * The most recent value established by {@link #setRawResult} (or {@code null} by default)
     * will be returned as the result of subsequent invocations of {@code join} and related operations.
     *
     * @since 1.8
     */
    // 静默完成，即将当前任务标记为[已完成]状态（不会改变当前任务挂起的次数）
    public final void quietlyComplete() {
        setDone();
    }
    
    /*▲ 完成 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中止 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Cancels, ignoring any exceptions thrown by cancel. Used during
     * worker and pool shutdown. Cancel is spec'ed not to throw any
     * exceptions, but if it does anyway, we have no recourse during
     * shutdown, so guard against this case.
     */
    // 取消任务，即将当前任务标记为DONE|ABNORMAL状态
    static void cancelIgnoringExceptions(ForkJoinTask<?> task) {
        // 如果task存在且未完成
        if(task != null && task.status >= 0) {
            try {
                // 取消任务，即将当前任务标记为[已完成]|[非正常完成]状态
                task.cancel(false);
            } catch(Throwable ignore) {
            }
        }
    }
    
    /**
     * Attempts to cancel execution of this task. This attempt will
     * fail if the task has already completed or could not be
     * cancelled for some other reason. If successful, and this task
     * has not started when {@code cancel} is called, execution of
     * this task is suppressed. After this method returns
     * successfully, unless there is an intervening call to {@link
     * #reinitialize}, subsequent calls to {@link #isCancelled},
     * {@link #isDone}, and {@code cancel} will return {@code true}
     * and calls to {@link #join} and related methods will result in
     * {@code CancellationException}.
     *
     * <p>This method may be overridden in subclasses, but if so, must
     * still ensure that these properties hold. In particular, the
     * {@code cancel} method itself must not throw exceptions.
     *
     * <p>This method is designed to be invoked by <em>other</em>
     * tasks. To terminate the current task, you can just return or
     * throw an unchecked exception from its computation method, or
     * invoke {@link #completeExceptionally(Throwable)}.
     *
     * @param mayInterruptIfRunning this value has no effect in the
     *                              default implementation because interrupts are not used to
     *                              control cancellation.
     *
     * @return {@code true} if this task is now cancelled
     */
    /*
     * 取消任务，即将当前任务标记为[已完成]|[非正常完成]状态
     *
     * 如果返回true，代表任务含有[非正常完成]标记，但不包含[有异常]标记
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        // 将当前任务标记为DONE|ABNORMAL状态
        int s = abnormalCompletion(DONE | ABNORMAL);
        return (s & (ABNORMAL | THROWN)) == ABNORMAL;
    }
    
    // 当前任务是否无异常地被取消
    public final boolean isCancelled() {
        return (status & (ABNORMAL | THROWN)) == ABNORMAL;
    }
    
    /*▲ 中止 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 异常 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 是否处于[有异常]状态
    static boolean isExceptionalStatus(int s) {
        return (s & THROWN) != 0;
    }
    
    /**
     * Completes this task abnormally, and if not already aborted or
     * cancelled, causes it to throw the given exception upon
     * {@code join} and related operations. This method may be used
     * to induce exceptions in asynchronous tasks, or to force
     * completion of tasks that would not otherwise complete.  Its use
     * in other situations is discouraged.  This method is
     * overridable, but overridden versions must invoke {@code super}
     * implementation to maintain guarantees.
     *
     * @param ex the exception to throw. If this exception is not a
     *           {@code RuntimeException} or {@code Error}, the actual exception
     *           thrown will be a {@code RuntimeException} with cause {@code ex}.
     */
    // 设置异常
    public void completeExceptionally(Throwable ex) {
        setExceptionalCompletion((ex instanceof RuntimeException) || (ex instanceof Error) ? ex : new RuntimeException(ex));
    }
    
    /**
     * Records exception and possibly propagates.
     *
     * @return status on exit
     */
    // 记录当前任务中的异常，并触发回调
    private int setExceptionalCompletion(Throwable ex) {
        // 将当前任务的"异常信息"打包成一个"异常记录"结点存入异常记录哈希表
        int s = recordExceptionalCompletion(ex);
        
        // 如果任务状态被标记为[有异常]
        if((s & THROWN) != 0) {
            // 执行回调方法
            internalPropagateException(ex);
        }
        
        return s;
    }
    
    /**
     * Hook for exception propagation support for tasks with completers.
     */
    // 将任务状态标记为[有异常]之后的回调
    void internalPropagateException(Throwable ex) {
    }
    
    /**
     * A version of "sneaky throw" to relay exceptions.
     */
    // 抛出异常，抑制了对未检查异常（未捕获异常）的警告信息
    static void rethrow(Throwable ex) {
        ForkJoinTask.uncheckedThrow(ex);
    }
    
    /**
     * The sneaky part of sneaky throw,
     * relying on generics limitations to evade compiler complaints about rethrowing unchecked exceptions.
     */
    // 抛出异常
    @SuppressWarnings("unchecked")
    static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        if(t != null) {
            throw (T) t; // rely on vacuous cast
        }
        
        throw new Error("Unknown Exception");
    }
    
    /**
     * Resets the internal bookkeeping state of this task, allowing a
     * subsequent {@code fork}. This method allows repeated reuse of
     * this task, but only if reuse occurs when this task has either
     * never been forked, or has been forked, then completed and all
     * outstanding joins of this task have also completed. Effects
     * under any other usage conditions are not guaranteed.
     * This method may be useful when executing
     * pre-constructed trees of subtasks in loops.
     *
     * <p>Upon completion of this method, {@code isDone()} reports
     * {@code false}, and {@code getException()} reports {@code
     * null}. However, the value returned by {@code getRawResult} is
     * unaffected. To clear this value, you can invoke {@code
     * setRawResult(null)}.
     */
    // 重置任务状态信息，并清理包含的异常
    public void reinitialize() {
        if((status & THROWN) != 0) {
            clearExceptionalCompletion();
        } else {
            status = 0;
        }
    }
    
    /**
     * Polls stale refs and removes them. Call only while holding lock.
     */
    // 清理失效的"异常记录"。即如果"任务"被GC回收，则将其所在的异常记录从异常记录哈希表中移除
    private static void expungeStaleExceptions() {
        for(Object x; (x = exceptionTableRefQueue.poll()) != null; ) {
            if(x instanceof ExceptionNode) {
                ExceptionNode[] ens = exceptionTable;
                
                int i = ((ExceptionNode) x).hashCode & (ens.length - 1);
                
                ExceptionNode e = ens[i];
                ExceptionNode pred = null;
                
                while(e != null) {
                    ExceptionNode next = e.next;
                    if(e == x) {
                        if(pred == null) {
                            ens[i] = next;
                        } else {
                            pred.next = next;
                        }
                        break;
                    }
                    pred = e;
                    e = next;
                }
            }
        }
    }
    
    /**
     * If lock is available, polls stale refs and removes them.
     * Called from ForkJoinPool when pools become quiescent.
     */
    // 清理失效的"异常记录"。即如果"任务"被GC回收，则将其所在的异常记录从异常记录哈希表中移除
    static void helpExpungeStaleExceptions() {
        final ReentrantLock lock = exceptionTableLock;
        if(lock.tryLock()) {
            try {
                expungeStaleExceptions();
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Records exception and sets status.
     *
     * @return status on exit
     */
    // 将当前任务的"异常信息"打包成一个"异常记录"结点存入异常记录哈希表
    final int recordExceptionalCompletion(Throwable ex) {
        int s = status;
        
        if(s >= 0) {
            // 获取当前任务的哈希码
            int h = System.identityHashCode(this);
            
            final ReentrantLock lock = exceptionTableLock;
            
            lock.lock();
            try {
                // 清理失效的"异常记录"。即如果"任务"被GC回收，则将其所在的异常记录从异常记录哈希表中移除
                expungeStaleExceptions();
                
                ExceptionNode[] ens = exceptionTable;
                
                // 计算当前任务在异常记录哈希表中的索引
                int i = h & (ens.length - 1);
                
                // 将具有相同哈希化索引的对象串在一起
                for(ExceptionNode e = ens[i]; ; e = e.next) {
                    if(e == null) {
                        // 头插法
                        ens[i] = new ExceptionNode(this, ex, ens[i], exceptionTableRefQueue);
                        break;
                    }
                    
                    // 如果该结点已经存在，则退出循环
                    if(e.get() == this) {
                        break;
                    }
                }
            } finally {
                lock.unlock();
            }
            
            // 将当前任务标记为[已完成]|[非正常完成]|[有异常]状态
            s = abnormalCompletion(DONE | ABNORMAL | THROWN);
        }
        
        return s;
    }
    
    /**
     * Removes exception node and clears status.
     */
    // 将当前"任务"所在的异常记录从异常记录哈希表中清除，并且需要清理所有"失效记录"
    private void clearExceptionalCompletion() {
        // 获取当前任务的哈希码
        int h = System.identityHashCode(this);
        
        final ReentrantLock lock = exceptionTableLock;
        
        lock.lock();
        try {
            ExceptionNode[] ens = exceptionTable;
            
            // 计算当前任务在异常记录哈希表中的索引
            int i = h & (ens.length - 1);
            
            ExceptionNode e = ens[i];
            ExceptionNode pred = null;
            
            while(e != null) {
                ExceptionNode next = e.next;
                if(e.get() == this) {
                    if(pred == null) {
                        ens[i] = next;
                    } else {
                        pred.next = next;
                    }
                    break;
                }
                pred = e;
                e = next;
            }
            
            // 清理失效的"异常记录"。即如果"任务"被GC回收，则将其所在的异常记录从异常记录哈希表中移除
            expungeStaleExceptions();
            
            status = 0;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Returns a rethrowable exception for this task, if available.
     * To provide accurate stack traces, if the exception was not thrown by the current thread,
     * we try to create a new exception of the same type as the one thrown,
     * but with the recorded exception as its cause.
     * If there is no such constructor, we instead try to use a no-arg constructor,
     * followed by initCause, to the same effect.
     * If none of these apply, or any fail due to other exceptions,
     * we return the recorded exception, which is still correct,
     * although it may contain a misleading stack trace.
     *
     * @return the exception, or null if none
     */
    // 获取当前任务抛出的异常
    private Throwable getThrowableException() {
        // 获取当前任务的哈希码
        int h = System.identityHashCode(this);
        
        ExceptionNode en;
        
        final ReentrantLock lock = exceptionTableLock;
        
        lock.lock();
        // 在异常记录哈希表中查找当前任务所在的结点
        try {
            // 清理失效的"异常记录"。即如果"任务"被GC回收，则将其所在的异常记录从异常记录哈希表中移除
            expungeStaleExceptions();
            ExceptionNode[] ens = exceptionTable;
            en = ens[h & (ens.length - 1)];
            while(en != null && en.get() != this) {
                en = en.next;
            }
        } finally {
            lock.unlock();
        }
        
        Throwable ex;
        // 如果不存在查找异常记录，或者该异常记录的异常信息为空，则返回null
        if(en == null || (ex = en.ex) == null) {
            return null;
        }
        
        // 如果当前线程不是之前抛异常的线程，则需要重新构造一个异常并抛出
        if(en.thrower != Thread.currentThread().getId()) {
            try {
                Constructor<?> noArgCtor = null;
                // 遍历标记为public的构造方法
                for(Constructor<?> c : ex.getClass().getConstructors()) {
                    // 获取所有形参的类型
                    Class<?>[] ps = c.getParameterTypes();
                    if(ps.length == 0) {
                        // 记下无参构造方法
                        noArgCtor = c;
                    } else {
                        // 如果存在单参数构造方法，且形参类型为Throwable，则构造异常并抛出
                        if(ps.length == 1 && ps[0] == Throwable.class) {
                            return (Throwable) c.newInstance(ex);
                        }
                    }
                }
                
                // 利用无参构造方法构造异常后抛出
                if(noArgCtor != null) {
                    Throwable wx = (Throwable) noArgCtor.newInstance();
                    wx.initCause(ex);
                    return wx;
                }
            } catch(Exception ignore) {
            }
        }
        
        // 返回当前任务抛出的异常
        return ex;
    }
    
    /**
     * Throws exception, if any, associated with the given status.
     */
    // 报告任务的异常信息
    private void reportException(int status) {
        Throwable throwable = null;
        
        // 如果当前任务包含[有异常]标记，则抛出其产生的异常
        if((status & THROWN) != 0) {
            throwable = getThrowableException();
            rethrow(throwable);
        }
        
        // 否则，抛出"已取消"异常
        throwable = new CancellationException();
        rethrow(throwable);
    }
    
    /**
     * Returns the exception thrown by the base computation, or a
     * {@code CancellationException} if cancelled, or {@code null} if
     * none or if the method has not yet completed.
     *
     * @return the exception, or {@code null} if none
     */
    // 如果任务被非正常关闭，则返回其异常信息
    public final Throwable getException() {
        int s = status;
        if((s & ABNORMAL) == 0) {
            return null;
        } else {
            if((s & THROWN) == 0) {
                // 返回"取消"异常
                return new CancellationException();
            } else {
                // 获取当前任务抛出的异常
                return getThrowableException();
            }
        }
    }
    
    /*▲ 异常 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 任务状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the tag for this task.
     *
     * @return the tag for this task
     *
     * @since 1.8
     */
    // 返回任务状态
    public final short getForkJoinTaskTag() {
        return (short) status;
    }
    
    /**
     * Atomically sets the tag value for this task and returns the old value.
     *
     * @param newValue the new tag value
     *
     * @return the previous value of the tag
     *
     * @since 1.8
     */
    // 设置任务状态
    public final short setForkJoinTaskTag(short newValue) {
        for(int s; ; ) {
            if(STATUS.weakCompareAndSet(this, s = status, (s & ~SMASK) | (newValue & SMASK))) {
                return (short) s;
            }
        }
    }
    
    /**
     * Atomically conditionally sets the tag value for this task.
     * Among other applications, tags can be used as visit markers
     * in tasks operating on graphs, as in methods that check: {@code
     * if (task.compareAndSetForkJoinTaskTag((short)0, (short)1))}
     * before processing, otherwise exiting because the node has
     * already been visited.
     *
     * @param expect the expected tag value
     * @param update the new tag value
     *
     * @return {@code true} if successful; i.e., the current value was
     * equal to {@code expect} and was changed to {@code update}.
     *
     * @since 1.8
     */
    // 原子地更新任务状态
    public final boolean compareAndSetForkJoinTaskTag(short expect, short update) {
        for(int s; ; ) {
            if((short) (s = status) != expect) {
                return false;
            }
            
            if(STATUS.weakCompareAndSet(this, s, (s & ~SMASK) | (update & SMASK))) {
                return true;
            }
        }
    }
    
    /**
     * Marks cancelled or exceptional completion unless already done.
     *
     * @param completion must be DONE | ABNORMAL, ORed with THROWN if exceptional
     * @return status on exit
     */
    // 将当前任务标记为completion状态（多数情形下为[已完成]|[非正常完成]状态，特殊情形下需要与THROWN协作）
    private int abnormalCompletion(int completion) {
        for(; ; ) {
            int s = status;
            
            // 如果该任务已完成，则直接返回
            if(s<0) {
                return s;
            } else {
                // 添加完成标记
                int ns = s | completion;
                // 更新任务状态
                if(STATUS.weakCompareAndSet(this, s, ns)) {
                    // 如果该任务是阻塞的，这里需要唤醒它(们)
                    if((s & SIGNAL) != 0) {
                        synchronized(this) {
                            notifyAll();
                        }
                    }
                    // 返回更新后的标记
                    return ns;
                }
            }
        }
    }
    
    /*▲ 任务状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 任务结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Forces the given value to be returned as a result.
     * This method is designed to support extensions, and should not in general be called otherwise.
     *
     * @param value the value
     */
    // 设置指定的值为当前任务的执行结果，设置期间可以对其进一步操作，具体逻辑由子类实现
    protected abstract void setRawResult(V value);
    
    /**
     * Completes this task, and if not already aborted or cancelled,
     * returning the given value as the result of subsequent invocations of {@code join} and related operations.
     * This method may be used to provide results for asynchronous tasks,
     * or to provide alternative handling for tasks that would not otherwise complete normally.
     * Its use in other situations is discouraged.
     * This method is overridable, but overridden versions must invoke {@code super} implementation to maintain guarantees.
     *
     * @param value the result value for this task
     */
    /*
     * 传入(某个)任务的执行结果，并继续对其操作
     * 只有当任务没有被取消或没有抛出异常时才应该使用
     * 通常用于执行异步任务，或者用于处理一系列任务中的某个阶段
     */
    public void complete(V value) {
        try {
            setRawResult(value);
        } catch(Throwable rex) {
            // 记录当前异常，并触发回调
            setExceptionalCompletion(rex);
            return;
        }
        
        // 将当前任务标记为[已完成]状态
        setDone();
    }
    
    /**
     * Returns the result that would be returned by {@link #join}, even if this task completed abnormally,
     * or {@code null} if this task is not known to have been completed.
     * This method is designed to aid debugging, as well as to support extensions.
     * Its use in any other context is discouraged.
     *
     * @return the result, or {@code null} if not completed
     */
    // 返回当前任务的执行结果，具体逻辑由子类实现
    public abstract V getRawResult();
    
    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     *
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException    if the computation threw an
     *                               exception
     * @throws InterruptedException  if the current thread is not a
     *                               member of a ForkJoinPool and was interrupted while waiting
     */
    // 返回任务执行结果（中途会触发计算）。如果任务未完成，则线程转入wait状态，直到任务完成后被唤醒
    public final V get()
        throws InterruptedException, ExecutionException {
        int s;
        
        // 【工作线程】从【工作队列】的top处取出当前任务并执行，最后返回任务状态。必要时，需要等待其他任务的完成
        if(Thread.currentThread() instanceof ForkJoinWorkerThread) {
            s = doJoin();
        } else {
            // 【提交线程】先尝试加速当前任务的完成。如果加速失败，则转入wait，等待当前任务执行完后再被唤醒
            s = externalInterruptibleAwaitDone();
        }
        
        if((s & THROWN) != 0) {
            // 获取当前任务抛出的异常
            Throwable throwable = getThrowableException();
            // 将抛出的异常包装到"执行异常"中之后再抛出
            throw new ExecutionException(throwable);
        } else if((s & ABNORMAL) != 0) {
            // 抛出"已取消"异常
            throw new CancellationException();
        } else {
            // 返回计算结果
            return getRawResult();
        }
    }
    
    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     *
     * @return the computed result
     *
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException    if the computation threw an
     *                               exception
     * @throws InterruptedException  if the current thread is not a
     *                               member of a ForkJoinPool and was interrupted while waiting
     * @throws TimeoutException      if the wait timed out
     */
    // 返回任务执行结果（中途会触发计算）。如果任务在指定的时间内未完成，则抛出异常
    public final V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        int s;
        
        long nanos = unit.toNanos(timeout);
        
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        
        if((s = status) >= 0 && nanos>0L) {
            long d = System.nanoTime() + nanos;
            long deadline = (d == 0L) ? 1L : d; // avoid 0
            Thread t = Thread.currentThread();
            
            if(t instanceof ForkJoinWorkerThread) {
                ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
                
                // 【工作线程】尝试加速task的完成，如果无法加速，则当前的【工作线程】考虑进入wait状态，直到task完成后被唤醒
                s = wt.pool.awaitJoin(wt.workQueue, this, deadline);
            } else {
                if(this instanceof CountedCompleter){
                    // 【提交线程】尝试加速task的完成，并在最终返回任务的状态
                    s = ForkJoinPool.common.externalHelpComplete((CountedCompleter<?>) this, 0);
                } else {
                    // 移除任务。【提交线程】尝试将指定的task从【共享工作池】上的【共享队列】top处移除，返回值代表是否成功移除
                    if(ForkJoinPool.common.tryExternalUnpush(this)){
                        // 执行任务，返回任务执行后的状态
                        s = doExec();
                    } else {
                        s = 0;
                    }
                }
                
                if(s >= 0) {
                    long ns, ms; // measure in nanosecs, but wait in millisecs
                    // 未超时，且任务还未完成，则让【工作线程】继续等待
                    while((s = status) >= 0 && (ns = deadline - System.nanoTime())>0L) {
                        if((ms = TimeUnit.NANOSECONDS.toMillis(ns))>0L && (s = (int) STATUS.getAndBitwiseOr(this, SIGNAL)) >= 0) {
                            synchronized(this) {
                                if(status >= 0) {
                                    wait(ms); // OK to throw InterruptedException
                                } else {
                                    notifyAll();
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if(s >= 0) {
            throw new TimeoutException();
        } else if((s & THROWN) != 0) {
            throw new ExecutionException(getThrowableException());
        } else if((s & ABNORMAL) != 0) {
            throw new CancellationException();
        } else {
            return getRawResult();
        }
    }
    
    /*▲ 任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns, but does not unschedule or execute, a task queued by
     * the current thread but not yet executed, if one is immediately
     * available. There is no guarantee that this task will actually
     * be polled or executed next. Conversely, this method may return
     * null even if a task exists but cannot be accessed without
     * contention with other threads.  This method is designed
     * primarily to support extensions, and is unlikely to be useful
     * otherwise.
     *
     * @return the next task, or {@code null} if none are available
     */
    /*
     * 从【队列】中获取任务（由调用线程决定从哪类【队列】上获取任务）
     * 如果【队列】带有FIFO标记，则从base处获取task，否则从top处获取任务
     */
    protected static ForkJoinTask<?> peekNextLocalTask() {
        Thread t = Thread.currentThread();
        WorkQueue wq;
        
        if(t instanceof ForkJoinWorkerThread) {
            // 获取当前【工作线程】管辖的【工作队列】
            wq = ((ForkJoinWorkerThread) t).workQueue;
        } else {
            // 获取当前【提交线程】管辖的【共享队列】（从【共享工作池】中获取）
            wq = ForkJoinPool.commonSubmitterQueue();
        }
        
        // 如果【队列】带有FIFO标记，则从base处获取task，否则从top处获取任务
        return (wq == null) ? null : wq.peek();
    }
    
    /**
     * If the current thread is operating in a ForkJoinPool,
     * unschedules and returns, without executing, the next task
     * queued by the current thread but not yet executed, if one is
     * available, or if not available, a task that was forked by some
     * other thread, if available. Availability may be transient, so a
     * {@code null} result does not necessarily imply quiescence of
     * the pool this task is operating in.  This method is designed
     * primarily to support extensions, and is unlikely to be useful
     * otherwise.
     *
     * @return a task, or {@code null} if none are available
     */
    // 如果在【工作线程】上调用，则返回一个[本地任务]或窃取的[外部任务]
    protected static ForkJoinTask<?> pollTask() {
        Thread thread = Thread.currentThread();
        
        if(thread instanceof ForkJoinWorkerThread){
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread) thread;
            
            // 获取一个[本地任务]或窃取的[外部任务]
            return wt.pool.nextTaskFor(wt.workQueue);
        }
        
        return null;
    }
    
    /**
     * Unschedules and returns, without executing, the next task
     * queued by the current thread but not yet executed, if the
     * current thread is operating in a ForkJoinPool.  This method is
     * designed primarily to support extensions, and is unlikely to be
     * useful otherwise.
     *
     * @return the next task, or {@code null} if none are available
     */
    // 如果在【工作线程】上调用，则返回一个[本地任务]
    protected static ForkJoinTask<?> pollNextLocalTask() {
        Thread t = Thread.currentThread();
        
        if(t instanceof ForkJoinWorkerThread){
            // 让【工作线程】尝试从自身的【工作队列】中取出[本地任务]
            return ((ForkJoinWorkerThread) t).workQueue.nextLocalTask();
        }
        
        return null;
    }
    
    /**
     * If the current thread is operating in a ForkJoinPool,
     * unschedules and returns, without executing, a task externally
     * submitted to the pool, if one is available. Availability may be
     * transient, so a {@code null} result does not necessarily imply
     * quiescence of the pool.  This method is designed primarily to
     * support extensions, and is unlikely to be useful otherwise.
     *
     * @return a task, or {@code null} if none are available
     *
     * @since 9
     */
    // 如果在【工作线程】上调用，则随机从某个【共享队列】中获取一个[外部任务]
    protected static ForkJoinTask<?> pollSubmission() {
        Thread t = Thread.currentThread();
        
        if(t instanceof ForkJoinWorkerThread){
            // 让【工作线程】随机从某个【共享队列】中获取一个任务
            return ((ForkJoinWorkerThread) t).pool.pollSubmission();
        }
        
        return null;
    }
    
    /*▲ 获取任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 统计信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an estimate of the number of tasks that have been
     * forked by the current worker thread but not yet executed. This
     * value may be useful for heuristic decisions about whether to
     * fork other tasks.
     *
     * @return the number of tasks
     */
    // 返回【队列】中的任务数（近似）
    public static int getQueuedTaskCount() {
        Thread t = Thread.currentThread();
        WorkQueue wq;
        
        if(t instanceof ForkJoinWorkerThread) {
            // 获取当前【工作线程】管辖的【工作队列】
            wq = ((ForkJoinWorkerThread) t).workQueue;
        } else {
            // 获取当前【提交线程】管辖的【共享队列】（从【共享工作池】中获取）
            wq = ForkJoinPool.commonSubmitterQueue();
        }
        
        // 返回【队列】中的任务数（近似）
        return (wq == null) ? 0 : wq.queueSize();
    }
    
    /**
     * Returns an estimate of how many more locally queued tasks are
     * held by the current worker thread than there are other worker
     * threads that might steal them, or zero if this thread is not
     * operating in a ForkJoinPool. This value may be useful for
     * heuristic decisions about whether to fork other tasks. In many
     * usages of ForkJoinTasks, at steady state, each worker should
     * aim to maintain a small constant surplus (for example, 3) of
     * tasks, and to process computations locally if this threshold is
     * exceeded.
     *
     * @return the surplus number of tasks, which may be negative
     */
    // 估算当前【工作线程】剩余的排队任务数量
    public static int getSurplusQueuedTaskCount() {
        return ForkJoinPool.getSurplusQueuedTaskCount();
    }
    
    /*▲ 统计信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 适配方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new {@code ForkJoinTask} that performs the {@code run}
     * method of the given {@code Runnable} as its action, and returns
     * the given result upon {@link #join}.
     *
     * @param runnable the runnable action
     * @param result   the result upon completion
     * @param <T>      the type of the result
     *
     * @return the task
     */
    // 适配方法，返回AdaptedRunnable类型的任务
    public static <T> ForkJoinTask<T> adapt(Runnable runnable, T result) {
        return new AdaptedRunnable<T>(runnable, result);
    }
    
    /**
     * Returns a new {@code ForkJoinTask} that performs the {@code run}
     * method of the given {@code Runnable} as its action, and returns
     * a null result upon {@link #join}.
     *
     * @param runnable the runnable action
     *
     * @return the task
     */
    // 适配方法，返回AdaptedRunnableAction类型的任务
    public static ForkJoinTask<?> adapt(Runnable runnable) {
        return new AdaptedRunnableAction(runnable);
    }
    
    /**
     * Returns a new {@code ForkJoinTask} that performs the {@code call}
     * method of the given {@code Callable} as its action, and returns
     * its result upon {@link #join}, translating any checked exceptions
     * encountered into {@code RuntimeException}.
     *
     * @param callable the callable action
     * @param <T>      the type of the callable's result
     *
     * @return the task
     */
    // 适配方法，返回AdaptedCallable类型的任务
    public static <T> ForkJoinTask<T> adapt(Callable<? extends T> callable) {
        return new AdaptedCallable<T>(callable);
    }
    
    /*▲ 适配方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Saves this task to a stream (that is, serializes it).
     *
     * @param s the stream
     *
     * @throws java.io.IOException if an I/O error occurs
     * @serialData the current run status and the exception thrown
     * during execution, or {@code null} if none
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeObject(getException());
    }
    
    /**
     * Reconstitutes this task from a stream (that is, deserializes it).
     *
     * @param s the stream
     *
     * @throws ClassNotFoundException if the class of a serialized object
     *                                could not be found
     * @throws java.io.IOException    if an I/O error occurs
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        Object ex = s.readObject();
        if(ex != null) {
            setExceptionalCompletion((Throwable) ex);
        }
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    /**
     * Adapter for Runnables.
     * This implements RunnableFuture to be compliant with AbstractExecutorService constraints when used in ForkJoinPool.
     */
    /*
     * 通过继承与组合的方式，兼容了Runnable
     * 如果需要该类任务有返回值，则应该在计算过程中动态赋值
     */
    static final class AdaptedRunnable<T> extends ForkJoinTask<T> implements RunnableFuture<T> {
        private static final long serialVersionUID = 5232453952276885070L;
        
        final Runnable runnable;
        T result;
        
        AdaptedRunnable(Runnable runnable, T result) {
            if(runnable == null) {
                throw new NullPointerException();
            }
            this.runnable = runnable;
            this.result = result; // OK to set this even before completion
        }
        
        public final T getRawResult() {
            return result;
        }
        
        public final void setRawResult(T v) {
            result = v;
        }
        
        // 通过继承的方式兼容Runnable，间接执行组合的Runnable中的任务（动作）
        public final void run() {
            // 并行执行任务
            invoke();
        }
        
        /*
         * 通过组合的方式兼容Runnable，直接执行组合的Runnable中的任务（动作）
         * 该方法总是true，代表任务执行完成
         */
        public final boolean exec() {
            runnable.run();
            return true;
        }
        
        public String toString() {
            return super.toString() + "[Wrapped task = " + runnable + "]";
        }
    }
    
    /**
     * Adapter for Runnables without results.
     */
    /*
     * 通过继承与组合的方式，兼容了Runnable
     * 该类任务没有返回值
     */
    static final class AdaptedRunnableAction extends ForkJoinTask<Void> implements RunnableFuture<Void> {
        private static final long serialVersionUID = 5232453952276885070L;
        
        final Runnable runnable;
        
        AdaptedRunnableAction(Runnable runnable) {
            if(runnable == null) {
                throw new NullPointerException();
            }
            this.runnable = runnable;
        }
        
        public final Void getRawResult() {
            return null;
        }
        
        public final void setRawResult(Void v) {
        }
        
        // 通过继承的方式兼容Runnable，间接执行组合的Runnable中的任务（动作）
        public final void run() {
            invoke();
        }
        
        /*
         * 通过组合的方式兼容Runnable，直接执行组合的Runnable中的任务（动作）
         * 该方法总是true，代表任务执行完成
         */
        public final boolean exec() {
            runnable.run();
            return true;
        }
        
        public String toString() {
            return super.toString() + "[Wrapped task = " + runnable + "]";
        }
    }
    
    /**
     * Adapter for Runnables in which failure forces worker exception.
     */
    // 通过组合的方式，兼容了Runnable，该类任务没有返回值
    static final class RunnableExecuteAction extends ForkJoinTask<Void> {
        private static final long serialVersionUID = 5232453952276885070L;
        
        final Runnable runnable;
        
        RunnableExecuteAction(Runnable runnable) {
            if(runnable == null) {
                throw new NullPointerException();
            }
            this.runnable = runnable;
        }
        
        public final Void getRawResult() {
            return null;
        }
        
        public final void setRawResult(Void v) {
        }
        
        /*
         * 通过组合的方式兼容Runnable，直接执行组合的Runnable中的任务（动作）
         * 该方法总是true，代表任务执行完成
         */
        public final boolean exec() {
            runnable.run();
            return true;
        }
        
        void internalPropagateException(Throwable ex) {
            rethrow(ex); // rethrow outside exec() catches.
        }
    }
    
    /**
     * Adapter for Callables.
     */
    /*
     * 既通过继承的方式，兼容了Runnable
     * 又通过组合的方式，兼容了Callable
     * 如果需要该类任务有返回值，则应该在计算过程中动态赋值
     */
    static final class AdaptedCallable<T> extends ForkJoinTask<T> implements RunnableFuture<T> {
        private static final long serialVersionUID = 2838392045355241008L;
        
        final Callable<? extends T> callable;
        T result;
        
        
        AdaptedCallable(Callable<? extends T> callable) {
            if(callable == null) {
                throw new NullPointerException();
            }
            this.callable = callable;
        }
        
        public final T getRawResult() {
            return result;
        }
        
        public final void setRawResult(T v) {
            result = v;
        }
        
        // 通过继承的方式兼容Runnable，间接执行组合的Callable中的任务（动作）
        public final void run() {
            invoke();
        }
        
        /*
         * 通过组合的方式兼容Callable，直接执行组合的Callable中的任务（动作）
         * 该方法总是true，代表任务执行完成
         */
        public final boolean exec() {
            try {
                result = callable.call();
                return true;
            } catch(RuntimeException rex) {
                throw rex;
            } catch(Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        
        public String toString() {
            return super.toString() + "[Wrapped task = " + callable + "]";
        }
    }
    
    
    
    /**
     * Key-value nodes for exception table.
     * The chained hash table uses identity comparisons, full locking, and weak references for keys.
     * The table has a fixed capacity because it only maintains task exceptions long enough for joiners to access them,
     * so should never become very large for sustained periods.
     * However, since we do not know when the last joiner completes, we must use weak references and expunge them.
     * We do so on each operation (hence full locking).
     * Also, some thread in any ForkJoinPool will call helpExpungeStaleExceptions when its pool becomes isQuiescent.
     */
    // 异常记录结点（可以组成一张哈希表）【该类继承自弱引用】
    static final class ExceptionNode extends WeakReference<ForkJoinTask<?>> {
        final Throwable ex;
        ExceptionNode next;
        
        // 记下抛出异常ex的线程
        final long thrower;  // use id not ref to avoid weak cycles
        
        // 记下异常所属的任务的哈希码，将来用作计算该任务在哈希表中的索引
        final int hashCode;  // store task hashCode before weak ref disappears
        
        ExceptionNode(ForkJoinTask<?> task, Throwable ex, ExceptionNode next,
                      ReferenceQueue<ForkJoinTask<?>> exceptionTableRefQueue) {
            super(task, exceptionTableRefQueue);
            this.ex = ex;
            this.next = next;
            this.thrower = Thread.currentThread().getId();
            this.hashCode = System.identityHashCode(task);
        }
    }
    
}
