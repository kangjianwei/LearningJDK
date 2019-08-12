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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

/**
 * A thread managed by a {@link ForkJoinPool}, which executes {@link ForkJoinTask}s.
 * This class is subclassable solely for the sake of adding functionality
 * -- there are no overridable methods dealing with scheduling or execution.
 * However, you can override initialization and termination methods surrounding the main task processing loop.
 * If you do create such a subclass, you will also need to supply a custom {@link ForkJoinPool.ForkJoinWorkerThreadFactory} to
 * {link ForkJoinPool#ForkJoinPool(int, ForkJoinWorkerThreadFactory, UncaughtExceptionHandler, boolean, int, int, int, Predicate, long, TimeUnit) use it}
 * in a {@code ForkJoinPool}.
 *
 * @author Doug Lea
 * @since 1.7
 */
/*
 * ForkJoinWorkerThread的实例表示【工作线程】，可用于窃取其他任务
 * 与【工作线程】相对的一个概念是【提交线程】，它是指【工作线程】之外的线程，不窃取任务
 * 两类型的线程中均可执行任务
 */
public class ForkJoinWorkerThread extends Thread {
    /*
     * ForkJoinWorkerThreads are managed by ForkJoinPools and perform ForkJoinTasks.
     * For explanation, see the internal documentation of class ForkJoinPool.
     *
     * This class just maintains links to its pool and WorkQueue.  The
     * pool field is set immediately upon construction, but the
     * workQueue field is not set until a call to registerWorker
     * completes. This leads to a visibility race, that is tolerated
     * by requiring that the workQueue field is only accessed by the
     * owning thread.
     *
     * Support for (non-public) subclass InnocuousForkJoinWorkerThread
     * requires that we break quite a lot of encapsulation (via helper
     * methods in ThreadLocalRandom) both here and in the subclass to
     * access and set Thread fields.
     */
    
    final ForkJoinPool pool;                // 【工作线程】的工作池，每个【工作线程】都需要注册到一个工作池上
    final ForkJoinPool.WorkQueue workQueue; // 由【工作线程】管理的【工作队列】
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a ForkJoinWorkerThread operating in the given pool.
     *
     * @param pool the pool this thread works in
     *
     * @throws NullPointerException if pool is null
     */
    // 创建【工作队列】和【工作线程】，并与指定的【工作池】完成三方绑定
    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        // Use a placeholder until a useful name can be set in registerWorker
        super("aForkJoinWorkerThread");
        // 注册到工作池
        this.pool = pool;
        // 为当前【工作线程】创建【工作队列】，并返回该【工作队列】
        this.workQueue = pool.registerWorker(this);
    }
    
    /**
     * Version for use by the default pool.  Supports setting the
     * context class loader.  This is a separate constructor to avoid
     * affecting the protected constructor.
     */
    // 创建【工作队列】和【工作线程】，并与指定的【工作池】完成三方绑定
    ForkJoinWorkerThread(ForkJoinPool pool, ClassLoader ccl) {
        super("aForkJoinWorkerThread");
        super.setContextClassLoader(ccl);
        // 注册到工作池
        this.pool = pool;
        // 为当前【工作线程】创建【工作队列】，并返回该【工作队列】
        this.workQueue = pool.registerWorker(this);
    }
    
    /**
     * Version for InnocuousForkJoinWorkerThread.
     */
    // 创建【工作队列】和【工作线程】，并与指定的【工作池】完成三方绑定
    ForkJoinWorkerThread(ForkJoinPool pool, ClassLoader ccl, ThreadGroup threadGroup, AccessControlContext acc) {
        super(threadGroup, null, "aForkJoinWorkerThread");
        super.setContextClassLoader(ccl);
        ThreadLocalRandom.setInheritedAccessControlContext(this, acc);
        ThreadLocalRandom.eraseThreadLocals(this); // clear before registering
        // 注册到工作池
        this.pool = pool;
        // 为当前【工作线程】创建【工作队列】，并返回该【工作队列】
        this.workQueue = pool.registerWorker(this);
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 启动 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * This method is required to be public, but should never be called explicitly.
     * It performs the main run loop to execute {@link ForkJoinTask}s.
     */
    // 启动【工作线程】
    public void run() {
        // 【工作线程】首次启动时，还没有分配到任务
        if(workQueue.array == null) {
            Throwable exception = null;
            try {
                onStart();
                // 【工作线程】开始工作：窃取并执行任务，没有足够任务时，【工作线程】转入park状态
                pool.runWorker(workQueue);
            } catch(Throwable ex) {
                exception = ex;
            } finally {
                try {
                    onTermination(exception);
                } catch(Throwable ex) {
                    if(exception == null) {
                        exception = ex;
                    }
                } finally {
                    pool.deregisterWorker(this, exception);
                }
            }
        }
    }
    
    /*▲ 启动 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 回调 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes internal state after construction but before
     * processing any tasks. If you override this method, you must
     * invoke {@code super.onStart()} at the beginning of the method.
     * Initialization requires care: Most fields must have legal
     * default values, to ensure that attempted accesses from other
     * threads work correctly even before this thread starts
     * processing tasks.
     */
    // 【工作线程】启动时的回调
    protected void onStart() {
    }
    
    /**
     * Performs cleanup associated with termination of this worker thread.
     * If you override this method, you must invoke {@code super.onTermination} at the end of the overridden method.
     *
     * @param exception the exception causing this thread to abort due
     *                  to an unrecoverable error, or {@code null} if completed normally
     */
    protected void onTermination(Throwable exception) {
    }
    
    /**
     * Non-public hook method for InnocuousForkJoinWorkerThread.
     */
    // 【工作线程】完成窃取操作后，执行的回调
    void afterTopLevelExec() {
    }
    
    /*▲ 回调 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the pool hosting this thread.
     *
     * @return the pool
     */
    // 获取【工作线程】关联的工作池
    public ForkJoinPool getPool() {
        return pool;
    }
    
    /**
     * Returns the unique index number of this thread in its pool.
     * The returned value ranges from zero to the maximum number of
     * threads (minus one) that may exist in the pool, and does not
     * change during the lifetime of the thread.  This method may be
     * useful for applications that track status or collect results
     * per-worker-thread rather than per-task.
     *
     * @return the index number
     */
    // 返回【工作线程】管辖的【工作队列】的唯一编号
    public int getPoolIndex() {
        return workQueue.getPoolIndex();
    }
    
    /*▲ 信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    /**
     * A worker thread that has no permissions, is not a member of any user-defined ThreadGroup,
     * uses the system class loader as thread context class loader, and erases all ThreadLocals after running each top-level task.
     */
    // 无害的【工作线程】，在存在SecurityManager时使用
    static final class InnocuousForkJoinWorkerThread extends ForkJoinWorkerThread {
        /** The ThreadGroup for all InnocuousForkJoinWorkerThreads */
        private static final ThreadGroup innocuousThreadGroup = AccessController.doPrivileged(new PrivilegedAction<>() {
            public ThreadGroup run() {
                ThreadGroup group = Thread.currentThread().getThreadGroup();
                for(ThreadGroup p; (p = group.getParent()) != null; ) {
                    group = p;
                }
                return new ThreadGroup(group, "InnocuousForkJoinWorkerThreadGroup");
            }
        });
        
        /** An AccessControlContext supporting no privileges */
        private static final AccessControlContext INNOCUOUS_ACC = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, null)});
        
        InnocuousForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool, ClassLoader.getSystemClassLoader(), innocuousThreadGroup, INNOCUOUS_ACC);
        }
        
        // to silently fail
        @Override
        public void setUncaughtExceptionHandler(UncaughtExceptionHandler x) {
        }
        
        // paranoically
        @Override
        public void setContextClassLoader(ClassLoader cl) {
            throw new SecurityException("setContextClassLoader");
        }
        
        // 用完之后，会擦除ThreadLocal信息
        @Override
        void afterTopLevelExec() {
            ThreadLocalRandom.eraseThreadLocals(this);
        }
    }
}
