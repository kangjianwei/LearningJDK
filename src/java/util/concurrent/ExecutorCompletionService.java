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

/**
 * A {@link CompletionService} that uses a supplied {@link Executor}
 * to execute tasks.  This class arranges that submitted tasks are,
 * upon completion, placed on a queue accessible using {@code take}.
 * The class is lightweight enough to be suitable for transient use
 * when processing groups of tasks.
 *
 * <p>
 *
 * <b>Usage Examples.</b>
 *
 * Suppose you have a set of solvers for a certain problem, each
 * returning a value of some type {@code Result}, and would like to
 * run them concurrently, processing the results of each of them that
 * return a non-null value, in some method {@code use(Result r)}. You
 * could write this as:
 *
 * <pre> {@code
 * void solve(Executor e,
 *            Collection<Callable<Result>> solvers)
 *     throws InterruptedException, ExecutionException {
 *   CompletionService<Result> cs
 *       = new ExecutorCompletionService<>(e);
 *   solvers.forEach(cs::submit);
 *   for (int i = solvers.size(); i > 0; i--) {
 *     Result r = cs.take().get();
 *     if (r != null)
 *       use(r);
 *   }
 * }}</pre>
 *
 * Suppose instead that you would like to use the first non-null result
 * of the set of tasks, ignoring any that encounter exceptions,
 * and cancelling all other tasks when the first one is ready:
 *
 * <pre> {@code
 * void solve(Executor e,
 *            Collection<Callable<Result>> solvers)
 *     throws InterruptedException {
 *   CompletionService<Result> cs
 *       = new ExecutorCompletionService<>(e);
 *   int n = solvers.size();
 *   List<Future<Result>> futures = new ArrayList<>(n);
 *   Result result = null;
 *   try {
 *     solvers.forEach(solver -> futures.add(cs.submit(solver)));
 *     for (int i = n; i > 0; i--) {
 *       try {
 *         Result r = cs.take().get();
 *         if (r != null) {
 *           result = r;
 *           break;
 *         }
 *       } catch (ExecutionException ignore) {}
 *     }
 *   } finally {
 *     futures.forEach(future -> future.cancel(true));
 *   }
 *
 *   if (result != null)
 *     use(result);
 * }}</pre>
 *
 * @since 1.5
 */
/*
 * 借助Executor和BlockingQueue实现的【任务执行-剥离框架】
 *
 * 当给定的任务结束后，不管是正常结束，还是异常结束，或者是被取消，都会被存入一个阻塞队列中
 * 后续可以从阻塞队列中取出这些已结束的任务，并获取它们的返回值或任务状态
 */
public class ExecutorCompletionService<V> implements CompletionService<V> {
    // 【任务执行器】
    private final Executor executor;
    
    /**
     * 【任务执行框架】
     *
     * 只有当executor是AbstractExecutorService的实例时，该字段等同于executor，否则，该字段为null。
     * 换句话说，当executor为【线程池】ThreadPoolExecutor或【任务池】ForkJoinPool（及其子类）时，aes有效
     */
    private final AbstractExecutorService aes;
    
    // 阻塞队列，存储已结束的任务（不管是正常结束，还是异常结束，或者是被取消）
    private final BlockingQueue<Future<V>> completionQueue;
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates an ExecutorCompletionService using the supplied
     * executor for base task execution and a
     * {@link LinkedBlockingQueue} as a completion queue.
     *
     * @param executor the executor to use
     *
     * @throws NullPointerException if executor is {@code null}
     */
    public ExecutorCompletionService(Executor executor) {
        if(executor == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.aes = (executor instanceof AbstractExecutorService) ? (AbstractExecutorService) executor : null;
        this.completionQueue = new LinkedBlockingQueue<Future<V>>();
    }
    
    /**
     * Creates an ExecutorCompletionService using the supplied
     * executor for base task execution and the supplied queue as its
     * completion queue.
     *
     * @param executor        the executor to use
     * @param completionQueue the queue to use as the completion queue
     *                        normally one dedicated for use by this service. This
     *                        queue is treated as unbounded -- failed attempted
     *                        {@code Queue.add} operations for completed tasks cause
     *                        them not to be retrievable.
     *
     * @throws NullPointerException if executor or completionQueue are {@code null}
     */
    public ExecutorCompletionService(Executor executor, BlockingQueue<Future<V>> completionQueue) {
        if(executor == null || completionQueue == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.aes = (executor instanceof AbstractExecutorService) ? (AbstractExecutorService) executor : null;
        this.completionQueue = completionQueue;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 包装Callable，创建新任务
    private RunnableFuture<V> newTaskFor(Callable<V> task) {
        if(aes == null) {
            return new FutureTask<V>(task);
        } else {
            return aes.newTaskFor(task);
        }
    }
    
    // 包装Runnable，创建新任务
    private RunnableFuture<V> newTaskFor(Runnable task, V result) {
        if(aes == null) {
            return new FutureTask<V>(task, result);
        } else {
            return aes.newTaskFor(task, result);
        }
    }
    
    /*▲ 创建任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 提交/执行任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 提交/执行任务，在任务结束后，将任务存入阻塞队列
    public Future<V> submit(Callable<V> task) {
        if(task == null) {
            throw new NullPointerException();
        }
        
        RunnableFuture<V> f = newTaskFor(task);
        executor.execute(new QueueingFuture<V>(f, completionQueue));
        
        // 返回值是Future类型，目的是为了判断任务状态，获取计算结果
        return f;
    }
    
    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 提交/执行任务，在任务结束后，将任务存入阻塞队列
    public Future<V> submit(Runnable task, V result) {
        if(task == null) {
            throw new NullPointerException();
        }
        
        RunnableFuture<V> f = newTaskFor(task, result);
        executor.execute(new QueueingFuture<V>(f, completionQueue));
        
        // 返回值是Future类型，目的是为了判断任务状态，获取计算结果
        return f;
    }
    
    /*▲ 提交/执行任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取已结束任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 从阻塞队列取出/移除的一个已结束任务，可能会被阻塞
    public Future<V> take() throws InterruptedException {
        return completionQueue.take();
    }
    
    // 从阻塞队列取出/移除的一个已结束任务，不会被阻塞，但可能返回null
    public Future<V> poll() {
        return completionQueue.poll();
    }
    
    // 在指定时间内从阻塞队列取出/移除的一个已结束任务，如果超时，则返回null
    public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return completionQueue.poll(timeout, unit);
    }
    
    /*▲ 获取已结束任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * FutureTask extension to enqueue upon completion.
     */
    // 增强FutureTask，可以将已结束的任务存储到阻塞队列
    private static class QueueingFuture<V> extends FutureTask<Void> {
        // 待执行任务
        private final Future<V> task;
        
        // 存储已结束的任务
        private final BlockingQueue<Future<V>> completionQueue;
        
        QueueingFuture(RunnableFuture<V> task, BlockingQueue<Future<V>> completionQueue) {
            super(task, null);
            this.task = task;
            this.completionQueue = completionQueue;
        }
        
        // 至此，当前任务已经结束（不管是正常结束，还是异常结束，或者是被取消），所有等待结果的线程也已唤醒
        protected void done() {
            // 将已结束的任务加入阻塞队列
            completionQueue.add(task);
        }
    }
}
