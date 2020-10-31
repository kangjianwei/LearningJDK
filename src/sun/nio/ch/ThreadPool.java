/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import jdk.internal.misc.InnocuousThread;
import sun.security.action.GetPropertyAction;

/**
 * Encapsulates a thread pool associated with a channel group.
 */
// 异步IO线程池：用于异步通道的工作线程线程池，只是作为指定的【任务执行框架】的一个包装
public class ThreadPool {
    
    // 用户定义的线程工厂；默认无定义
    private static final String DEFAULT_THREAD_POOL_THREAD_FACTORY = "java.nio.channels.DefaultThreadPool.threadFactory";
    // 用户定义的线程池容量；默认无定义
    private static final String DEFAULT_THREAD_POOL_INITIAL_SIZE = "java.nio.channels.DefaultThreadPool.initialSize";
    
    // 被包装的【任务执行框架】
    private final ExecutorService executor;
    
    /** indicates if thread pool is fixed size */
    // 指示线程池是否为固定大小
    private final boolean isFixed;
    
    /** indicates the pool size (for a fixed thread pool configuratin this is the maximum pool size; for other thread pools it is the initial size) */
    /*
     * 线程池容量
     *
     * 对于固定容量的线程池，这是最大容量值；对于非固定容量的线程池，这是初始容量值。
     */
    private final int poolSize;
    
    
    /*
     * 构造异步IO线程池
     *
     * executor: 被包装的【任务执行框架】
     * isFixed : 线程池容量是否固定
     * poolSize: 线程池初始容量，对于固定容量的线程池来说，这就是其容量的最大值
     */
    private ThreadPool(ExecutorService executor, boolean isFixed, int poolSize) {
        this.executor = executor;
        this.isFixed = isFixed;
        this.poolSize = poolSize;
    }
    
    
    /** create using given parameters */
    // 构造一个异步IO线程池：容量固定，固定容量为nThreads
    static ThreadPool create(int nThreads, ThreadFactory factory) {
        if(nThreads<=0) {
            throw new IllegalArgumentException("'nThreads' must be > 0");
        }
        
        // 创建【固定容量线程池】，线程池【核心阙值】/【最大阙值】为nThreads
        ExecutorService executor = Executors.newFixedThreadPool(nThreads, factory);
        
        // 创建异步IO线程池：容量固定，固定容量为nThreads
        return new ThreadPool(executor, true, nThreads);
    }
    
    /** wrap a user-supplied executor */
    // 将指定的【任务执行框架】包装为异步IO线程池：容量非固定，初始容量为initialSize(具体值还需要进一步计算)
    public static ThreadPool wrap(ExecutorService executor, int initialSize) {
        if(executor == null) {
            throw new NullPointerException("'executor' is null");
        }
        
        // attempt to check if cached thread pool
        if(executor instanceof ThreadPoolExecutor) {
            int max = ((ThreadPoolExecutor) executor).getMaximumPoolSize();
            if(max == Integer.MAX_VALUE) {
                if(initialSize<0) {
                    initialSize = Runtime.getRuntime().availableProcessors();
                } else {
                    // not a cached thread pool so ignore initial size
                    initialSize = 0;
                }
            }
        } else {
            // some other type of thread pool
            if(initialSize<0) {
                initialSize = 0;
            }
        }
        
        // 创建异步IO线程池：容量非固定，初始容量为initialSize
        return new ThreadPool(executor, false, initialSize);
    }
    
    /** return the default (system-wide) thread pool */
    // 构造一个异步IO线程池：容量非固定，初始容量默认与处理器数量一致(接受用户的自定义)
    static ThreadPool getDefault() {
        return DefaultThreadPoolHolder.defaultThreadPool;
    }
    
    /** create thread using default settings (configured by system properties) */
    // 构造一个异步IO线程池：容量非固定，初始容量默认与处理器数量一致(接受用户的自定义)
    static ThreadPool createDefault() {
        /* default the number of fixed threads to the hardware core count */
        // 获取用户定义的线程池容量；默认无定义，返回-1
        int initialSize = getDefaultThreadPoolInitialSize();
        
        // 如果用户没有做出定义(默认)，则初始化目标线程池容量为虚拟机可用的处理器数量
        if(initialSize<0) {
            initialSize = Runtime.getRuntime().availableProcessors();
        }
        
        /* default to thread factory that creates daemon threads */
        // 获取用户定义的线程工厂；默认无定义，返回null
        ThreadFactory threadFactory = getDefaultThreadPoolThreadFactory();
        
        // 如果用户没有做出定义(默认)，则初始化目标线程工厂为默认的守护线程工厂
        if(threadFactory == null) {
            threadFactory = defaultThreadFactory();
        }
        
        // 创建【缓冲线程池】
        ExecutorService executor = Executors.newCachedThreadPool(threadFactory);
        
        // 创建异步IO线程池：容量非固定，初始容量为initialSize
        return new ThreadPool(executor, false, initialSize);
    }
    
    // 返回用户定义的线程池容量；默认无定义，返回-1
    private static int getDefaultThreadPoolInitialSize() {
        String propValue = AccessController.doPrivileged(new GetPropertyAction(DEFAULT_THREAD_POOL_INITIAL_SIZE));
        if(propValue == null) {
            return -1;
        }
        
        try {
            return Integer.parseInt(propValue);
        } catch(NumberFormatException x) {
            throw new Error("Value of property '" + DEFAULT_THREAD_POOL_INITIAL_SIZE + "' is invalid: " + x);
        }
    }
    
    // 返回用户定义的线程工厂；默认无定义，返回null
    private static ThreadFactory getDefaultThreadPoolThreadFactory() {
        String propValue = AccessController.doPrivileged(new GetPropertyAction(DEFAULT_THREAD_POOL_THREAD_FACTORY));
        if(propValue == null) {
            return null;
        }
        
        try {
            @SuppressWarnings("deprecation")
            Object tmp = Class.forName(propValue, true, ClassLoader.getSystemClassLoader()).newInstance();
            return (ThreadFactory) tmp;
        } catch(ClassNotFoundException | InstantiationException | IllegalAccessException x) {
            throw new Error(x);
        }
    }
    
    // 返回一个默认的守护线程工厂
    static ThreadFactory defaultThreadFactory() {
        if(System.getSecurityManager() == null) {
            return (Runnable r) -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            };
        } else {
            return (Runnable r) -> {
                PrivilegedAction<Thread> action = () -> {
                    Thread t = InnocuousThread.newThread(r);
                    t.setDaemon(true);
                    return t;
                };
                return AccessController.doPrivileged(action);
            };
        }
    }
    
    
    // 返回异步IO线程池内包装的【任务执行框架】
    ExecutorService executor() {
        return executor;
    }
    
    // 判断异步IO线程池容量是否固定
    boolean isFixedThreadPool() {
        return isFixed;
    }
    
    /*
     * 返回异步IO线程池的容量。
     * 对于固定容量的线程池，这是最大容量值；对于非固定容量的线程池，这是初始容量值。
     */
    int poolSize() {
        return poolSize;
    }
    
    
    // 默认的线程池引用
    private static class DefaultThreadPoolHolder {
        // 获取一个异步IO线程池：容量非固定，初始容量默认与处理器数量一致(接受用户的自定义)
        static final ThreadPool defaultThreadPool = createDefault();
    }
    
}
