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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Provides default implementations of {@link ExecutorService}
 * execution methods. This class implements the {@code submit},
 * {@code invokeAny} and {@code invokeAll} methods using a
 * {@link RunnableFuture} returned by {@code newTaskFor}, which defaults
 * to the {@link FutureTask} class provided in this package.  For example,
 * the implementation of {@code submit(Runnable)} creates an
 * associated {@code RunnableFuture} that is executed and
 * returned. Subclasses may override the {@code newTaskFor} methods
 * to return {@code RunnableFuture} implementations other than
 * {@code FutureTask}.
 *
 * <p><b>Extension example</b>. Here is a sketch of a class
 * that customizes {@link ThreadPoolExecutor} to use
 * a {@code CustomTask} class instead of the default {@code FutureTask}:
 * <pre> {@code
 * public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
 *
 *   static class CustomTask<V> implements RunnableFuture<V> {...}
 *
 *   protected <V> RunnableFuture<V> newTaskFor(Callable<V> c) {
 *       return new CustomTask<V>(c);
 *   }
 *   protected <V> RunnableFuture<V> newTaskFor(Runnable r, V v) {
 *       return new CustomTask<V>(r, v);
 *   }
 *   // ... add constructors, etc.
 * }}</pre>
 *
 * @author Doug Lea
 * @since 1.5
 */
/*
 * 【任务执行框架】的一个抽象/简单实现
 *
 * 【任务执行框架】有两个重要的子类：【线程池】ThreadPoolExecutor与【任务池】ForkJoinPool
 */
public abstract class AbstractExecutorService implements ExecutorService {
    
    /*▼ 包装任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a {@code RunnableFuture} for the given runnable and default
     * value.
     *
     * @param runnable the runnable task being wrapped
     * @param value    the default value for the returned future
     * @param <T>      the type of the given value
     *
     * @return a {@code RunnableFuture} which, when run, will run the
     * underlying runnable and which, as a {@code Future}, will yield
     * the given value as its result and provide for cancellation of
     * the underlying task
     *
     * @since 1.6
     */
    // 将Runnable包装为一个FutureTask
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<T>(runnable, value);
    }
    
    /**
     * Returns a {@code RunnableFuture} for the given callable task.
     *
     * @param callable the callable task being wrapped
     * @param <T>      the type of the callable's result
     *
     * @return a {@code RunnableFuture} which, when run, will call the
     * underlying callable and which, as a {@code Future}, will yield
     * the callable's result as its result and provide for
     * cancellation of the underlying task
     *
     * @since 1.6
     */
    // 将Callable包装为一个FutureTask
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<T>(callable);
    }
    
    /*▲ 包装任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包装/提交/执行任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 包装/提交/执行Callable型的任务
    public <T> Future<T> submit(Callable<T> task) {
        if(task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);
        return ftask;
    }
    
    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 包装/提交/执行Runnable型的任务，不带返回值
    public Future<?> submit(Runnable task) {
        if(task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<Void> ftask = newTaskFor(task, null);
        execute(ftask);
        return ftask;
    }
    
    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 包装/提交/执行Runnable型的任务，预设一个返回结果
    public <T> Future<T> submit(Runnable task, T result) {
        if(task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> ftask = newTaskFor(task, result);
        execute(ftask);
        return ftask;
    }
    
    
    // 执行指定容器中的所有任务，返回值是所有包装后的任务列表
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
        
        if(tasks == null) {
            throw new NullPointerException();
        }
        
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            // 将容器中的Callable型任务批量转换为Future，并执行它们
            for(Callable<T> task : tasks) {
                RunnableFuture<T> future = newTaskFor(task);
                futures.add(future);
                execute(future);
            }
            
            // 遍历所有任务，如果任务未结束，等待/促进任务结束，并获取任务的计算结果
            for(Future<T> future : futures) {
                if(!future.isDone()) {
                    try {
                        future.get();
                    } catch(CancellationException | ExecutionException ignore) {
                    }
                }
            }
            
            return futures;
        } catch(Throwable t) {
            cancelAll(futures);
            throw t;
        }
    }
    
    // 在指定时间内执行指定容器中的所有任务，返回值是所有包装后的任务列表（包括超时后被取消的任务）
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
        if(tasks == null) {
            throw new NullPointerException();
        }
        
        final long nanos = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + nanos;
        
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        
        int j = 0;

timedOut:
        try {
            // 将容器中的Callable型任务批量转换为Future
            for(Callable<T> task : tasks) {
                futures.add(newTaskFor(task));
            }
            
            final int size = futures.size();
            
            /* Interleave time checks and calls to execute in case executor doesn't have any/much parallelism. */
            
            // 遍历并执行所有任务
            for(int i = 0; i<size; i++) {
                if(((i == 0) ? nanos : deadline - System.nanoTime())<=0L) {
                    break timedOut;
                }
                execute((Runnable) futures.get(i));
            }
            
            // 遍历所有任务，如果任务未结束，等待/促进任务结束，并获取任务的计算结果
            for(; j<size; j++) {
                Future<T> future = futures.get(j);
                if(!future.isDone()) {
                    try {
                        future.get(deadline - System.nanoTime(), NANOSECONDS);
                    } catch(CancellationException | ExecutionException ignore) {
                        // ignore
                    } catch(TimeoutException timedOut) {
                        break timedOut;
                    }
                }
            }
            return futures;
        } catch(Throwable t) {
            cancelAll(futures);
            throw t;
        }
        
        /* Timed out before all the tasks could be completed; cancel remaining */
        
        // 取消已经超时的任务
        cancelAll(futures, j);
        
        return futures;
    }
    
    // 从任一任务开始执行，只要发现某个任务已结束，就中断其他正在执行的任务，并返回首个被发现结束的任务的计算结果
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
        try {
            return doInvokeAny(tasks, false, 0);
        } catch(TimeoutException cannotHappen) {
            assert false;
            return null;
        }
    }
    
    // 运作方式同invokeAny(Collection)，不过这里限制这些操作要在指定的时间内结束，否则就抛出异常
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return doInvokeAny(tasks, true, unit.toNanos(timeout));
    }
    
    /**
     * the main mechanics of invokeAny.
     */
    // 从任一任务开始执行，在获取到首个已结束任务的计算结果后就返回。支持启用超时设置
    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos)
        throws InterruptedException, ExecutionException, TimeoutException {
        
        if(tasks == null) {
            throw new NullPointerException();
        }
        
        // 获取任务数量
        int ntasks = tasks.size();
        if(ntasks == 0) {
            throw new IllegalArgumentException();
        }
        
        // 保存正在执行的任务
        ArrayList<Future<T>> futures = new ArrayList<>(ntasks);
        
        // 【任务执行-分离框架】，可保存已执行完的任务
        ExecutorCompletionService<T> ecs = new ExecutorCompletionService<T>(this);
        
        /*
         * For efficiency, especially in executors with limited parallelism,
         * check to see if previously submitted tasks are done before submitting more of them.
         * This interleaving plus the exception mechanics account for messiness of main loop.
         */
        
        try {
            // Record exceptions so that if we fail to obtain any result, we can throw the last exception we got.
            ExecutionException ee = null;
            
            // 如果启用了超时设置，则需要计算截止时间
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            
            Iterator<? extends Callable<T>> it = tasks.iterator();
            
            /* Start one task for sure; the rest incrementally */
            // 先选出一个任务开始执行
            futures.add(ecs.submit(it.next()));
            // 待执行任务数减一
            --ntasks;
            // 正在执行的任务数增一
            int active = 1;
            
            for(; ; ) {
                // 取出一个已结束的任务，不会被阻塞，但可能返回null
                Future<T> future = ecs.poll();
                
                // 如果阻塞队列为空（表明还没有结束的任务）
                if(future == null) {
                    // 如果仍存在待执行任务，继续选出一个任务开始执行
                    if(ntasks>0) {
                        --ntasks;
                        futures.add(ecs.submit(it.next()));
                        ++active;
                        
                        // 已经没有正在执行的任务了
                    } else if(active == 0) {
                        break;
                        
                        // 如果启用了超时设置
                    } else if(timed) {
                        // 取出已结束的任务，取不到时阻塞一段时间
                        future = ecs.poll(nanos, NANOSECONDS);
                        // 超时后也没取到任务，则抛异常
                        if(future == null) {
                            throw new TimeoutException();
                        }
                        // 计算剩余可阻塞的时间
                        nanos = deadline - System.nanoTime();
                    } else {
                        // 取出一个已结束的任务，取不到时陷入阻塞
                        future = ecs.take();
                    }
                }
                
                // 如果从阻塞队列中取到了任一已结束的任务
                if(future != null) {
                    // 正在执行的任务数减一
                    --active;
                    try {
                        // 返回任务的执行结果
                        return future.get();
                    } catch(ExecutionException eex) {
                        ee = eex;
                    } catch(RuntimeException rex) {
                        ee = new ExecutionException(rex);
                    }
                }
            }
            
            if(ee == null) {
                ee = new ExecutionException();
            }
            
            throw ee;
        } finally {
            // 取消指定容器中的所有任务（设置中断标记）
            cancelAll(futures);
        }
    }
    
    /*▲ 包装/提交/执行任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中止任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 中止指定容器中的所有任务（设置中断标记）
    private static <T> void cancelAll(ArrayList<Future<T>> futures) {
        cancelAll(futures, 0);
    }
    
    /** Cancels all futures with index at least j. */
    // 从指定容器的下标j开始，中止后续所有任务（设置中断标记）
    private static <T> void cancelAll(ArrayList<Future<T>> futures, int j) {
        for(int size = futures.size(); j<size; j++) {
            futures.get(j).cancel(true);
        }
    }
    
    /*▲ 中止任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
