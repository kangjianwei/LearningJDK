/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.Channel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import sun.security.action.GetIntegerAction;

/**
 * Base implementation of AsynchronousChannelGroup
 */
// 异步IO通道组的基础实现
abstract class AsynchronousChannelGroupImpl extends AsynchronousChannelGroup implements Executor {
    
    /**
     * number of internal threads handling I/O events when using an unbounded thread pool.
     * Internal threads do not dispatch to completion handlers.
     */
    /*
     * 工作线程的保底数量，接受用户的定义，默认值是1。
     * 当通道组中的异步IO线程池容量不固定时，该常量用来对启动的工作线程数量保底。
     *
     * 注：在该常量限制下启动的线程，我们称之为保底线程。
     * 　　保底线程也是工作线程，只是不会与通道组绑定。
     *
     * 参见：Invoker#myGroupAndInvokeCount
     * 　　　AsynchronousChannelGroupImpl#startThreads()
     */
    private static final int internalThreadCount = AccessController.doPrivileged(new GetIntegerAction("sun.nio.ch.internalThreadPoolSize", 1));
    
    /** associated thread pool */
    // 通道组中的异步IO线程池
    private final ThreadPool pool;
    
    /** associated Executor for timeouts */
    //【定时任务线程池】，用于执行一次性或周期性的定时任务
    private ScheduledThreadPoolExecutor timeoutExecutor;
    
    /** task queue for when using a fixed thread pool. In that case, a thread waiting on I/O events must be awoken to poll tasks from this queue */
    // 任务队列，存储提交给阻塞的工作线程的任务
    private final Queue<Runnable> taskQueue;
    
    /** number of tasks running (including internal) */
    // Java层工作线程的数量(包含了保底线程的数量)
    private final AtomicInteger threadCount = new AtomicInteger();
    
    /** group shutdown */
    // 指示通道组是否准备关闭
    private final AtomicBoolean shutdown = new AtomicBoolean();
    
    // 指示通道组是否已经关闭
    private volatile boolean terminateInitiated;
    
    private final Object shutdownNowLock = new Object();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    AsynchronousChannelGroupImpl(AsynchronousChannelProvider provider, ThreadPool pool) {
        super(provider);
        
        // 初始化异步IO线程池
        this.pool = pool;
        
        // 如果给定的线程池容量固定，则初始化一个线程安全的链式无界单向队列作为任务队列
        if(pool.isFixedThreadPool()) {
            taskQueue = new ConcurrentLinkedQueue<>();
        } else {
            taskQueue = null;   // not used
        }
        
        /* use default thread factory as thread should not be visible to application (it doesn't execute completion handlers) */
        // 创建一个【定时任务线程池】(线程池中只有一个守护线程)
        this.timeoutExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, ThreadPool.defaultThreadFactory());
        
        // 允许移除阻塞队列中被中止的任务
        this.timeoutExecutor.setRemoveOnCancelPolicy(true);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 线程池/工作线程 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回通道组中的异步IO线程池内包装的【任务执行框架】
    final ExecutorService executor() {
        return pool.executor();
    }
    
    // 判断通道组中的异步IO线程池容量是否固定
    final boolean isFixedThreadPool() {
        return pool.isFixedThreadPool();
    }
    
    /*
     * 返回异步IO线程池中的工作线程数量上限
     * 该参数用来告知IO完成端口在同一时间内最多能有多少个线程处于可运行状态。
     */
    final int fixedThreadCount() {
        // 如果通道组中的异步IO线程池容量是固定的，那么直接返回异步IO线程池的容量即可
        if(isFixedThreadPool()) {
            return pool.poolSize();
            
            // 如果通道组中的异步IO线程池容量不固定，那么需要返回异步IO线程池的容量加上工作线程的保底数量
        } else {
            return pool.poolSize() + internalThreadCount;
        }
    }
    
    // 返回Java层工作线程的数量(包含了保底线程的数量)
    final int threadCount() {
        return threadCount.get();
    }
    
    /**
     * Invoked by tasks as they terminate
     */
    /*
     * Java层工作线程(包含了保底线程)在退出时需要执行的方法。
     *
     * task     : 待执行任务
     * replaceMe: 如果工作线程是因为错误/异常而退出，那么replaceMe为true。
     *            此时意味着需要重启一个线程来执行task。
     */
    final int threadExit(Runnable task, boolean replaceMe) {
        if(replaceMe) {
            try {
                // 如果当前线程设置了(递归)调用计数器，说明这是一个保底线程之外的工作线程
                if(Invoker.isBoundToAnyGroup()) {
                    // 对指定的task进行包装：即在执行task之前，会为当前线程设置一个(递归)调用计数器
                    task = bindToGroup(task);
                    // submit new task to replace this thread
                    pool.executor().execute(task);
                    
                    // 如果是普通的保底线程
                } else {
                    // 创建并启动一个守护线程作为工作线程，以执行指定的轮询任务
                    startInternalThread(task);
                }
                
                // 返回Java层工作线程的数量(包含了保底线程的数量)
                return threadCount.get();
            } catch(RejectedExecutionException x) {
                // unable to replace
            }
        }
        
        // 将Java层工作线程的数量减一后返回
        return threadCount.decrementAndGet();
    }
    
    
    /*
     * 批量启动工作线程，以执行指定的轮询任务
     *
     * 对于保底线程，它会直接执行轮询任务；
     * 对于其他工作线程，在执行轮询任务之前，会先为该线程绑定一个"直调"计数器。
     *
     * 这里会同时启动Java层与本地(native层)的工作线程，它们是一一对应的。
     * 启动的后的阻塞往往会阻塞在getQueuedCompletionStatus()处，以等待IO完成队列中有元素进来。
     *
     * 涉及的完成端口操作：
     *【2】启动Java层的工作线程；
     *　 　启动本地(native层)的工作线程；
     *　 　Java层与本地(native层)的工作线程是一一对应的；
     *
     * 参见：Iocp#getQueuedCompletionStatus()
     * 　　　EventHandlerTask#run()
     */
    protected final void startThreads(Runnable task) {
        // 如果通道组中的异步IO线程池容量不固定，则需要先启动一批保底数量的工作线程
        if(!isFixedThreadPool()) {
            for(int i = 0; i<internalThreadCount; i++) {
                /*
                 * 创建并启动一个守护线程作为工作线程，以执行指定的轮询任务。
                 * 注：为了与下面的工作线程作区分，我们将这里启动的工作线程称为保底线程。
                 */
                startInternalThread(task);
                threadCount.incrementAndGet();
            }
        }
        
        /*
         * 获取异步IO线程池的容量。
         * 对于固定容量的线程池，这是最大容量值；对于非固定容量的线程池，这是初始容量值。
         */
        int poolSize = pool.poolSize();
        // 如果没有有效的容量值，直接返回
        if(poolSize<=0) {
            return;
        }
        
        /*
         * 对指定的task进行包装：即在执行task之前，会为当前线程设置一个(递归)调用计数器
         *
         * ★★★ 上面的保底线程没有这个绑定过程
         */
        task = bindToGroup(task);
        
        try {
            // 获取异步IO线程池内包装的【任务执行框架】
            ExecutorService executor = pool.executor();
            
            // 启用一批工作线程，以执行包装后的轮询任务
            for(int i = 0; i<poolSize; i++) {
                executor.execute(task);
                threadCount.incrementAndGet();
            }
        } catch(RejectedExecutionException x) {
            // nothing we can do
        }
    }
    
    // 创建并启动一个守护线程作为工作线程，以执行指定的轮询任务
    private void startInternalThread(final Runnable task) {
        AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Void run() {
                // internal threads should not be visible to application so cannot use user-supplied thread factory
                ThreadPool.defaultThreadFactory().newThread(task).start();
                return null;
            }
        });
    }
    
    /*▲ 线程池/工作线程 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 执行任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Executes the given command on one of the channel group's pooled threads.
     */
    /*
     * 让异步IO线程池处理指定的task(会经过安全管理器的校验)。
     *
     * 如果线程池容量固定，则唤醒正在阻塞的工作线程处理task。
     * 如果线程池容量不固定，则将task提交到线程池中以启动新的线程来处理task。
     */
    @Override
    public final void execute(Runnable task) {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            // when a security manager is installed then the user's task must be run with the current calling context
            final AccessControlContext acc = AccessController.getContext();
            final Runnable delegate = task;
            task = new Runnable() {
                @Override
                public void run() {
                    AccessController.doPrivileged(new PrivilegedAction<>() {
                        @Override
                        public Void run() {
                            delegate.run();
                            return null;
                        }
                    }, acc);
                }
            };
        }
        
        // 让异步IO线程池处理指定的task
        executeOnPooledThread(task);
    }
    
    /**
     * For a fixed thread pool the task is queued to a thread waiting on I/O events.
     * For other thread pools we simply submit the task to the thread pool.
     */
    /*
     * 让异步IO线程池处理指定的task。
     *
     * 如果线程池容量固定，则唤醒正在阻塞的工作线程处理task。
     * 如果线程池容量不固定，则将task提交到线程池中以启动新的线程来处理task。
     */
    final void executeOnPooledThread(Runnable task) {
        // 如果通道组中的异步IO线程池容量固定，则需要唤醒工作线程处理task
        if(isFixedThreadPool()) {
            /*
             * 将指定的任务推送到任务队列中，并且向阻塞的工作线程发送模拟IO信号，
             * 以唤醒工作线程来处理任务队列中的task。
             */
            executeOnHandlerTask(task);
            
            // 如果通道组中的异步IO线程池容量不固定，则将任务交给线程池处理
        } else {
            // 对指定的task进行包装：即在执行task之前，会为当前线程设置一个(递归)调用计数器
            task = bindToGroup(task);
            pool.executor().execute(task);
        }
    }
    
    /**
     * Wakes up a thread waiting for I/O events to execute the given task.
     */
    /*
     * 将指定的任务推送到任务队列中，并且向阻塞的工作线程发送模拟IO信号，
     * 以唤醒工作线程来处理任务队列中的task。
     */
    abstract void executeOnHandlerTask(Runnable task);
    
    /*▲ 执行任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 任务队列 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将task添加到任务队列，不会队满，不阻塞
    final void offerTask(Runnable task) {
        taskQueue.offer(task);
    }
    
    // 从任务队列中取出一个task
    final Runnable pollTask() {
        return (taskQueue == null) ? null : taskQueue.poll();
    }
    
    /*▲ 任务队列 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 外部通道 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Attaches a foreign channel to this group.
     */
    // 添加一个外部通道(不会与完成端口绑定)，返回其对应的完成键
    abstract Object attachForeignChannel(Channel channel, FileDescriptor fdo) throws IOException;
    
    /**
     * Detaches a foreign channel from this group.
     */
    // 根据指定的完成键，移除其对应的外部通道
    abstract void detachForeignChannel(Object key);
    
    /*▲ 外部通道 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 尝试关闭通道组；如果通道组内的通道未关闭，则只是将当前通道组标记为准备关闭状态
    @Override
    public final void shutdown() {
        if(shutdown.getAndSet(true)) {
            // already shutdown
            return;
        }
        
        /* if there are channels in the group then shutdown will continue when the last channel is closed. */
        // 如果通道组不为空，则直接返回
        if(!isEmpty()) {
            return;
        }
        
        /*
         * initiate termination
         * (acquire shutdownNowLock to ensure that other threads invoking shutdownNow will block).
         */
        synchronized(shutdownNowLock) {
            if(!terminateInitiated) {
                terminateInitiated = true;
                
                // 关闭所有工作线程(包括保底线程)
                shutdownHandlerTasks();
                
                // 关闭异步IO线程池与【定时任务线程池】
                shutdownExecutors();
            }
        }
    }
    
    // 立即关闭异步IO通道组，包括：关闭通道、关闭工作线程、关闭线程池
    @Override
    public final void shutdownNow() throws IOException {
        shutdown.set(true);
        
        synchronized(shutdownNowLock) {
            if(!terminateInitiated) {
                terminateInitiated = true;
                
                // 关闭通道组内关联的所有通道
                closeAllChannels();
                
                // 关闭所有工作线程(包括保底线程)
                shutdownHandlerTasks();
                
                // 关闭异步IO线程池与【定时任务线程池】
                shutdownExecutors();
            }
        }
    }
    
    // 判断异步IO通道组是否准备关闭
    @Override
    public final boolean isShutdown() {
        return shutdown.get();
    }
    
    // 判断通道组中的异步IO线程池是否已关闭(同时也指示通道组是否已经关闭)
    @Override
    public final boolean isTerminated() {
        return pool.executor().isTerminated();
    }
    
    // 等待通道组中的异步IO线程池关闭；成功关闭后，返回true(同时也指示通道组是否已经关闭)
    @Override
    public final boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return pool.executor().awaitTermination(timeout, unit);
    }
    
    
    /**
     * Closes all channels in the group
     */
    // 关闭通道组内关联的所有通道
    abstract void closeAllChannels() throws IOException;
    
    /**
     * For use by AsynchronousFileChannel to release resources without shutting down the thread pool.
     */
    // 在异步IO通道组已经为空时，关闭所有工作线程(包括保底线程)
    final void detachFromThreadPool() {
        if(shutdown.getAndSet(true)) {
            throw new AssertionError("Already shutdown");
        }
        
        // 需要确保通道组已经为空
        if(!isEmpty()) {
            throw new AssertionError("Group not empty");
        }
        
        // 关闭所有工作线程(包括保底线程)
        shutdownHandlerTasks();
    }
    
    /**
     * Shutdown all tasks waiting for I/O events.
     */
    // 关闭所有工作线程(包括保底线程)
    abstract void shutdownHandlerTasks();
    
    // 关闭异步IO线程池与【定时任务线程池】
    private void shutdownExecutors() {
        AccessController.doPrivileged(new PrivilegedAction<>() {
            public Void run() {
                pool.executor().shutdown();
                timeoutExecutor.shutdown();
                return null;
            }
        }, null, new RuntimePermission("modifyThread"));
    }
    
    /*▲ 关闭  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns true if there are no channels in the group
     */
    // 判断通道组是否为空，即该通道组内是否包含通道
    abstract boolean isEmpty();
    
    // 执行一次性的定时任务task，并返回任务本身：在任务启动后的timeout时长后开始执行
    final Future<?> schedule(Runnable task, long timeout, TimeUnit unit) {
        try {
            return timeoutExecutor.schedule(task, timeout, unit);
        } catch(RejectedExecutionException rej) {
            if(terminateInitiated) {
                // no timeout scheduled as group is terminating
                return null;
            }
            
            throw new AssertionError(rej);
        }
    }
    
    // 对指定的task进行包装：即在执行task之前，会为当前线程设置一个(递归)调用计数器
    private Runnable bindToGroup(final Runnable task) {
        // 获取当前通道组的引用
        final AsynchronousChannelGroupImpl thisGroup = this;
        
        return new Runnable() {
            public void run() {
                // 为当前线程设置一个(递归)调用计数器
                Invoker.bindToGroup(thisGroup);
                task.run();
            }
        };
    }
    
}
