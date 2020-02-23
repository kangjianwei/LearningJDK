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
import java.util.concurrent.locks.LockSupport;

/**
 * A cancellable asynchronous computation.  This class provides a base
 * implementation of {@link Future}, with methods to start and cancel
 * a computation, query to see if the computation is complete, and
 * retrieve the result of the computation.  The result can only be
 * retrieved when the computation has completed; the {@code get}
 * methods will block if the computation has not yet completed.  Once
 * the computation has completed, the computation cannot be restarted
 * or cancelled (unless the computation is invoked using
 * {@link #runAndReset}).
 *
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or
 * {@link Runnable} object.  Because {@code FutureTask} implements
 * {@code Runnable}, a {@code FutureTask} can be submitted to an
 * {@link Executor} for execution.
 *
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating
 * customized task classes.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 */
/*
 * FutureTask表示一类将来会被完成的异步任务，这类任务带有返回值，且中途可被取消。
 *
 * 该类提供了Runnable与Future的基本实现，包括启动和取消任务，查询任务是否结束，以及获取任务执行结果。
 * 只有在任务结束后才能获取结果，如果任务尚未结束，get()方法将被阻塞。
 * 任务结束后，无法重新启动或取消计算，除非开始执行时调用的是runAndReset()。
 *
 * FutureTask可用于包装Callable或Runnable型的任务，因为FutureTask实现了Runnable，所以可将FutureTask提交给Executor执行。
 *
 * 除了作为独立的类使用，该类还定义了一些protected方法，以方便自定义子类。
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * Revision notes: This differs from previous versions of this
     * class that relied on AbstractQueuedSynchronizer, mainly to
     * avoid surprising users about retaining interrupt status during
     * cancellation races. Sync control in the current design relies
     * on a "state" field updated via CAS to track completion, along
     * with a simple Treiber stack to hold waiting threads.
     */
    
    /**
     * The run state of this task, initially NEW.  The run state
     * transitions to a terminal state only in methods set,
     * setException, and cancel.  During completion, state may take on
     * transient values of COMPLETING (while outcome is being set) or
     * INTERRUPTING (only while interrupting the runner to satisfy a
     * cancel(true)). Transitions from these intermediate to final
     * states use cheaper ordered/lazy writes because values are unique
     * and cannot be further modified.
     *
     * Possible state transitions:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    // 任务状态标记
    private volatile int state;
    
    private static final int NEW          = 0;  // 新建任务
    private static final int COMPLETING   = 1;  // 任务已结束
    private static final int NORMAL       = 2;  // 任务正常结束，并设置了返回值
    private static final int EXCEPTIONAL  = 3;  // 任务异常结束，并设置了异常信息
    private static final int CANCELLED    = 4;  // 任务已取消
    private static final int INTERRUPTING = 5;  // 任务正在中断
    private static final int INTERRUPTED  = 6;  // 任务已中断
    
    /** The underlying callable; nulled out after running */
    // 待执行任务（可由Runnable型任务包装而来）
    private Callable<V> callable;
    
    /** The result to return or exception to throw from get() */
    // 保存正常结束时的计算结果，或者保存异常结束时的异常对象
    private Object outcome; // non-volatile, protected by state reads/writes
    
    /** The thread running the callable; CASed during run() */
    // 正在执行此任务的线程
    private volatile Thread runner;
    
    /** Treiber stack of waiting threads */
    // 等待栈的栈顶游标，等待栈用于存储那些正在等待计算结果的线程
    private volatile WaitNode waiters;
    
    // VarHandle mechanics
    private static final VarHandle STATE;
    private static final VarHandle RUNNER;
    private static final VarHandle WAITERS;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE = l.findVarHandle(FutureTask.class, "state", int.class);
            RUNNER = l.findVarHandle(FutureTask.class, "runner", Thread.class);
            WAITERS = l.findVarHandle(FutureTask.class, "waiters", WaitNode.class);
        } catch(ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
        
        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Callable}.
     *
     * @param callable the callable task
     *
     * @throws NullPointerException if the callable is null
     */
    public FutureTask(Callable<V> callable) {
        if(callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }
    
    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Runnable}, and arrange that {@code get} will return the
     * given result on successful completion.
     *
     * @param runnable the runnable task
     * @param result   the result to return on successful completion. If
     *                 you don't need a particular result, consider using
     *                 constructions of the form:
     *                 {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     *
     * @throws NullPointerException if the runnable is null
     */
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // ensure visibility of callable
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 执行 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 执行任务，并设置执行结果。如果任务正常结束，则其最终状态为NORMAL
    public void run() {
        // 尝试设置当前线程为任务执行线程，如果设置失败，直接返回
        if(!(state==NEW && RUNNER.compareAndSet(this, null, Thread.currentThread()))){
            return;
        }
        
        try {
            Callable<V> task = callable;
    
            if(task != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    // 执行任务，并获取计算结果
                    result = task.call();
                    ran = true;
                } catch(Throwable ex) {
                    result = null;
                    ran = false;
                    // 异常结束，设置异常信息，唤醒所有等待线程
                    setException(ex);
                }
                
                if(ran) {
                    // 正常结束，设置计算结果，唤醒所有等待线程
                    set(result);
                }
            }
        } finally {
            /* runner must be non-null until state is settled to prevent concurrent calls to run() */
            runner = null;
            
            /* state must be re-read after nulling runner to prevent leaked interrupts */
            // 如果任务所在线程需要被中断
            if(state >= INTERRUPTING) {
                // 确保中断完成
                handlePossibleCancellationInterrupt(state);
            }
        }
        
    }
    
    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     *
     * @return {@code true} if successfully run and reset
     */
    // 执行任务，不设置执行结果。如果任务正常结束，则其最终状态仍为NEW，这意味着该调用该方法可重复执行任务
    protected boolean runAndReset() {
        // 尝试设置当前线程为任务执行线程，如果设置失败，直接返回
        if(state != NEW || !RUNNER.compareAndSet(this, null, Thread.currentThread())) {
            return false;
        }
        
        boolean ran = false;
        int s = state;
    
        try {
            // 待执行任务（可由Runnable型任务包装而来）
            Callable<V> task = callable;
        
            if(task != null && s == NEW) {
                try {
                    // 执行任务，但不获取计算结果
                    task.call(); // don't set result
                    ran = true;
                } catch(Throwable ex) {
                    // 异常结束，设置异常信息，唤醒所有等待线程
                    setException(ex);
                }
            }
        } finally {
            // runner must be non-null until state is settled to prevent concurrent calls to run()
            runner = null;  // 置空正在执行此任务的线程
            
            // state must be re-read after nulling runner to prevent leaked interrupts
            s = state;  // 获取任务状态的最新信息
        
            // 如果任务正在被中断
            if(s >= INTERRUPTING) {
                // 确保中断完成
                handlePossibleCancellationInterrupt(s);
            }
        }
        
        // 返回值表示任务是否正常结束
        return ran && s==NEW;
    }
    
    /*▲ 执行 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中止 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 中止异步任务，包括取消或中断，mayInterruptIfRunning指示是否可在任务执行期间中断线程
    public boolean cancel(boolean mayInterruptIfRunning) {
        // 决定使用中断标记还是取消标记
        int s = mayInterruptIfRunning ? INTERRUPTING : CANCELLED;
        
        // 尝试更新任务状态：NEW-->INTERRUPTING/CANCELLED，如果更新失败，则直接返回
        if(!(state==NEW && STATE.compareAndSet(this, NEW, s))) {
            return false;
        }
        
        try {
            /* in case call to interrupt throws exception */
            // 如果需要中断线程
            if(mayInterruptIfRunning) {
                try {
                    if(runner != null) {
                        // 中断线程（只是给线程预设一个标记，不是立即让线程停下来）
                        runner.interrupt();
                    }
                } finally {
                    // NEW -> INTERRUPTING -> INTERRUPTED
                    STATE.setRelease(this, INTERRUPTED);
                }
            }
        } finally {
            // 任务结束后，唤醒所有等待结果的线程
            finishCompletion();
        }
        
        return true;
    }
    
    /*▲ 中止 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets the result of this future to the given value unless
     * this future has already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon successful completion of the computation.
     *
     * @param v the value
     */
    // 设置计算结果，并将任务状态从NEW更新为COMPLETING-->NORMAL
    protected void set(V v) {
        // 尝试更新任务状态：NEW-->COMPLETING
        if(STATE.compareAndSet(this, NEW, COMPLETING)) {
            // 保存计算结果
            outcome = v;
            
            // 尝试更新任务状态：COMPLETING-->NORMAL
            STATE.setRelease(this, NORMAL); // final state
            
            // 任务结束后，唤醒所有等待线程
            finishCompletion();
        }
    }
    
    /**
     * Causes this future to report an {@link ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     *
     * @param t the cause of failure
     */
    // 设置异常信息，并将任务状态从NEW更新为COMPLETING-->EXCEPTIONAL
    protected void setException(Throwable t) {
        // 尝试更新任务状态：NEW-->COMPLETING
        if(STATE.compareAndSet(this, NEW, COMPLETING)) {
            // 保存异常信息
            outcome = t;
            
            // 尝试更新任务状态：COMPLETING-->EXCEPTIONAL
            STATE.setRelease(this, EXCEPTIONAL); // final state
            
            // 任务结束后，唤醒所有等待线程
            finishCompletion();
        }
    }
    
    /*▲ 设置结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ 结束任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Removes and signals all waiting threads, invokes done(), and nulls out callable.
     */
    // 任务结束后（不管是正常结束，还是异常结束，或者是被取消），唤醒所有等待线程
    private void finishCompletion() {
        /* assert state > COMPLETING; */
        
        for(WaitNode q; (q = waiters) != null; ) {
            // 使waiters==null，因为任务已经结束，不需要继续等待了
            if(WAITERS.weakCompareAndSet(this, q, null)) {
                // 遍历等待栈，唤醒所有等待线程
                for(; ; ) {
                    Thread t = q.thread;
                    if(t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    
                    WaitNode next = q.next;
                    if(next == null) {
                        break;
                    }
                    
                    // 帮助回收
                    q.next = null; // unlink to help gc
                    q = next;
                }
                
                break;
            }
        }
        
        done();
        
        callable = null;        // to reduce footprint
    }
    
    /**
     * Protected method invoked when this task transitions to state
     * {@code isDone} (whether normally or via cancellation). The
     * default implementation does nothing.  Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     */
    // 任务结束，且唤醒所有等待结果的线程之后的回调
    protected void done() {
    }
    
    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     */
    // 确保中断完成
    private void handlePossibleCancellationInterrupt(int s) {
        // It is possible for our interrupter to stall before getting a chance to interrupt us.  Let's spin-wait patiently.
        if(s == INTERRUPTING) {
            // 任务正在中断，则等待中断完成
            while(state == INTERRUPTING) {
                // 当前线程让出CPU时间片，大家重新抢占执行权
                Thread.yield(); // wait out pending interrupt
            }
        }
        
        // assert state == INTERRUPTED;
        
        /*
         * We want to clear any interrupt we may have received from cancel(true).
         * However, it is permissible to use interrupts as an independent mechanism for a task to communicate with its caller,
         * and there is no way to clear only the cancellation interrupt.
         */
        // Thread.interrupted();
    }
    
    /**
     * Awaits completion or aborts on interrupt or timeout.
     *
     * @param timed true if use timed waits
     * @param nanos time to wait, if timed
     *
     * @return state upon completion or at timeout
     */
    // 等待任务结束后，获取任务的状态标记，如果未启用超时，则一直阻塞，直到任务结束后被唤醒。否则，阻塞指定的一段时间后即退出
    private int awaitDone(boolean timed, long nanos) throws InterruptedException {
        /*
         * The code below is very delicate, to achieve these goals:
         * - call nanoTime exactly once for each call to park
         * - if nanos <= 0L, return promptly without allocation or nanoTime
         * - if nanos == Long.MIN_VALUE, don't underflow
         * - if nanos == Long.MAX_VALUE, and nanoTime is non-monotonic
         *   and we suffer a spurious wakeup, we will do no worse than to park-spin for a while
         */
        
        long startTime = 0L;    // Special value 0L means not yet parked
        
        // 等待结点
        WaitNode q = null;
        
        boolean queued = false;
        
        for(; ; ) {
            int s = state;
            
            // 如果任务已结束，并且设置了返回结果或异常信息，那么可以结束等待了
            if(s>COMPLETING) {
                if(q != null) {
                    q.thread = null;
                }
                // 返回任务具体的状态
                return s;
                
                // 如果任务已结束，但是还未设置返回结果或异常信息，则需要等待一会儿
            } else if(s == COMPLETING) {
                /*
                 * We may have already promised (via isDone) that we are done
                 * so never return empty-handed or throw InterruptedException
                 */
                // 当前线程让出CPU时间片，大家重新抢占执行权
                Thread.yield();
                
                // 测试当前线程是否已经中断，线程的中断状态会被清除
            } else if(Thread.interrupted()) {
                // 如果线程已被设置为中断，则注销当前等待者，并移除等待栈中所有失效结点
                removeWaiter(q);
                
                // 抛出异常
                throw new InterruptedException();
                
                // 如果等待结点为空，则新建一个等待结点
            } else if(q == null) {
                // 如果启用了超时设置，且确实已经超时了，直接返回
                if(timed && nanos<=0L) {
                    return s;
                }
                // 新建等待结点，存入当前线程
                q = new WaitNode();
                
                // 将当前线程加入等待栈，并更新等待栈的栈顶游标
            } else if(!queued) {
                queued = WAITERS.weakCompareAndSet(this, q.next=waiters, q);
                
                // 如果启用了超时设置
            } else if(timed) {
                final long parkNanos;
                
                // 如果还未设置等待起点
                if(startTime == 0L) { // first time
                    // 初始化等待起点
                    startTime = System.nanoTime();
                    if(startTime == 0L) {
                        startTime = 1L;
                    }
                    
                    // 初始化剩余等待时间
                    parkNanos = nanos;
                } else {
                    // 计算已经流逝的时间
                    long elapsed = System.nanoTime() - startTime;
                    
                    // 如果已经超时
                    if(elapsed >= nanos) {
                        // 注销当前等待者，并移除等待栈中所有失效结点
                        removeWaiter(q);
                        
                        // 返回任务状态
                        return state;
                    }
                    
                    // 如果还未超时，则计算剩余等待时间
                    parkNanos = nanos - elapsed;
                }
                
                /* nanoTime may be slow; recheck before parking */
                // 如果任务还在运行中，则阻塞线程，进入等待
                if(state<COMPLETING) {
                    LockSupport.parkNanos(this, parkNanos);
                }
                
                // 未启用超时设置时，线程无限期阻塞，直到被主动唤醒
            } else {
                // 阻塞线程，进入等待
                LockSupport.park(this);
            }
        }
    }
    
    /**
     * Tries to unlink a timed-out or interrupted wait node to avoid
     * accumulating garbage.  Internal nodes are simply unspliced
     * without CAS since it is harmless if they are traversed anyway
     * by releasers.  To avoid effects of unsplicing from already
     * removed nodes, the list is retraversed in case of an apparent
     * race.  This is slow when there are a lot of nodes, but we don't
     * expect lists to be long enough to outweigh higher-overhead
     * schemes.
     */
    // 注销给定的等待者，并移除等待栈中所有失效结点
    private void removeWaiter(WaitNode node) {
        if(node != null) {
            // 置空等待线程
            node.thread = null;
retry:
            for(; ; ) {          // restart on removeWaiter race
                for(WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if(q.thread != null) {
                        pred = q;
                    } else if(pred != null) {
                        // 移除等待栈中失效的等待结点
                        pred.next = s;
                        
                        // 可能触发栈顶游标的更新
                        if(pred.thread == null) {// check for race
                            continue retry;
                        }
                        
                        // 等待栈栈顶的等待结点失效（没有等待线程了），则需要更新等待栈的栈顶游标
                    } else if(!WAITERS.compareAndSet(this, q, s)) {
                        continue retry;
                    }
                }
                
                break;
            }
        }
    }
    
    /*▲ 结束任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ 获取结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws CancellationException {@inheritDoc}
     */
    // 获取任务计算结果，任务未结束时会一直阻塞
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        
        // 如果任务未结束
        if(s<=COMPLETING) {
            // 获取任务的状态标记，任务未结束时会一直阻塞
            s = awaitDone(false, 0L);
        }
        
        // 报告任务状态，返回任务的计算结果
        return report(s);
    }
    
    /**
     * @throws CancellationException {@inheritDoc}
     */
    // 获取任务计算结果，任务未结束时会阻塞指定的一段时间
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if(unit == null) {
            throw new NullPointerException();
        }
        
        int s = state;
        
        // 如果任务未结束
        if(s<=COMPLETING) {
            // 获取任务的状态标记，任务未结束时会阻塞指定的一段时间
            s = awaitDone(true, unit.toNanos(timeout));
            
            if(s<=COMPLETING){
                throw new TimeoutException();
            }
        }
        
        // 报告任务状态，返回任务的计算结果
        return report(s);
    }
    
    /**
     * Returns result or throws exception for completed task.
     *
     * @param s completed state value
     */
    // 报告任务状态，返回任务的计算结果（可能会抛出异常）
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        
        // 如果是正常结束，返回计算结果
        if(s == NORMAL) {
            return (V) x;
        }
        
        // 如果是异常结束，抛出异常
        if(s >= CANCELLED) {
            throw new CancellationException();
        }
        
        throw new ExecutionException((Throwable) x);
    }
    
    /*▲ 获取结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 任务是否被中止（取消或被中断）
    public boolean isCancelled() {
        return state >= CANCELLED;
    }
    
    // 任务是否结束（包装正常结束，异常结束，或中止）
    public boolean isDone() {
        return state != NEW;
    }
    
    
    
    /**
     * Returns a string representation of this FutureTask.
     *
     * @return a string representation of this FutureTask
     *
     * @implSpec The default implementation returns a string identifying this
     * FutureTask, as well as its completion state.  The state, in
     * brackets, contains one of the strings {@code "Completed Normally"},
     * {@code "Completed Exceptionally"}, {@code "Cancelled"}, or {@code
     * "Not completed"}.
     */
    public String toString() {
        final String status;
        switch(state) {
            case NORMAL:
                status = "[Completed normally]";
                break;
            case EXCEPTIONAL:
                status = "[Completed exceptionally: " + outcome + "]";
                break;
            case CANCELLED:
            case INTERRUPTING:
            case INTERRUPTED:
                status = "[Cancelled]";
                break;
            default:
                final Callable<?> callable = this.callable;
                status = (callable == null) ? "[Not completed]" : "[Not completed, task = " + callable + "]";
        }
        return super.toString() + status;
    }
    
    
    
    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     */
    // 等待结点
    static final class WaitNode {
        volatile Thread thread; // 正在等待计算结果的线程
        volatile WaitNode next; // 前一个等待结点
        
        WaitNode() {
            thread = Thread.currentThread();
        }
    }
    
}
