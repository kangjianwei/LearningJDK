/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.ref;

import jdk.internal.misc.InnocuousThread;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.lang.ref.ReferenceQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * CleanerImpl manages a set of object references and corresponding cleaning actions.
 * CleanerImpl provides the functionality of {@link java.lang.ref.Cleaner}.
 */
// 为清理器（Cleaner）提供清理服务，与Cleaner是一对一关系
public final class CleanerImpl implements Runnable {
    
    /**
     * An object to access the CleanerImpl from a Cleaner; set by Cleaner init.
     */
    // cleaner -> cleaner.impl，给定Cleaner对象，返回其内部的CleanerImpl
    private static Function<Cleaner, CleanerImpl> cleanerImplAccess = null;
    
    /**
     * Heads of a CleanableList for each reference type.
     */
    final SoftCleanable<?> softCleanableList;   // 软引用清理器列表
    final WeakCleanable<?> weakCleanableList;   // 弱引用清理器列表
    final PhantomCleanable<?> phantomCleanableList; // 虚引用清理器列表，是清理器Cleaner的默认实现
    
    /*
     * The ReferenceQueue of pending cleaning actions
     *
     * 存放报废的Reference，在这里存放的是报废的引用清理器，因为引用清理器本身也是Reference。
     */
    final ReferenceQueue<Object> queue;
    
    /**
     * Constructor for CleanerImpl.
     */
    // 完成引用队列和引用链表的初始化
    public CleanerImpl() {
        queue = new ReferenceQueue<>();
        softCleanableList = new SoftCleanableRef();
        weakCleanableList = new WeakCleanableRef();
        phantomCleanableList = new PhantomCleanableRef();
    }
    
    /**
     * Called by Cleaner static initialization to provide the function to map from Cleaner to CleanerImpl.
     *
     * @param access a function to map from Cleaner to CleanerImpl
     */
    // 提供函数式接口，跟普通的set方法作用一样
    public static void setCleanerImplAccess(Function<Cleaner, CleanerImpl> access) {
        if(cleanerImplAccess == null) {
            cleanerImplAccess = access;
        } else {
            throw new InternalError("cleanerImplAccess");
        }
    }
    
    /**
     * Called to get the CleanerImpl for a Cleaner.
     *
     * @param cleaner the cleaner
     *
     * @return the corresponding CleanerImpl
     */
    // 给定Cleaner对象，返回其内部的CleanerImpl
    static CleanerImpl getCleanerImpl(Cleaner cleaner) {
        return cleanerImplAccess.apply(cleaner);
    }
    
    /**
     * Starts the Cleaner implementation.
     * Ensure this is the CleanerImpl for the Cleaner.
     * When started waits for Cleanables to be queued.
     *
     * @param cleaner       the cleaner
     * @param threadFactory the thread factory
     */
    // 将清理服务放入守护线程，并启动它，主要作用是轮询ReferenceQueue，取出其中报废的Reference，执行其清理动作action
    public void start(Cleaner cleaner, ThreadFactory threadFactory) {
        // Cleaner和CleanerImpl是一对一关系
        if(getCleanerImpl(cleaner) != this) {
            throw new AssertionError("wrong cleaner");
        }
    
        /*
         * schedule a nop cleaning action for the cleaner,
         * so the associated thread will continue to run at least until the cleaner is reclaimable.
         */
        // 将Cleaner对象自身也加入追踪
        new CleanerCleanable(cleaner);
        
        // 使用清理器内部默认的"无害"线程工厂
        if(threadFactory == null) {
            threadFactory = CleanerImpl.InnocuousThreadFactory.factory();
        }
        
        /*
         * now that there's at least one cleaning action, for the cleaner, we can start the associated thread, which runs until all cleaning actions have been run.
         */
        // 将专属清理服务绑定到专属的守护线程
        Thread thread = threadFactory.newThread(this);
        thread.setDaemon(true);
    
        // 调用下面的run()方法，提供清理服务
        thread.start();
    }
    
    /**
     * Process queued Cleanables as long as the cleanable lists are not empty.
     * A Cleanable is in one of the lists for each Object and for the Cleaner itself.
     * Terminates when the Cleaner is no longer reachable and has been cleaned and there are no more Cleanable instances for which the object is reachable.
     * <p>
     * If the thread is a ManagedLocalsThread, the threadlocals are erased before each cleanup
     */
    // 提供清理服务，最终调用清理器的clean方法
    @Override
    public void run() {
        Thread t = Thread.currentThread();
        // "无害"线程，如果外界不指定自定义的线程，那么清理器内部默认使用此类型线程做守护线程
        InnocuousThread mlThread = (t instanceof InnocuousThread) ? (InnocuousThread) t : null;
        
        // 轮询清理器列表
        while(!phantomCleanableList.isListEmpty()
            || !weakCleanableList.isListEmpty()
            || !softCleanableList.isListEmpty()) {
            
            if(mlThread != null) {
                // 即清除该线程中所有ThreadLocal信息
                mlThread.eraseThreadLocals();
            }
            
            // 清理服务的核心：轮询"报废引用"队列，取出被回收引用对应的虚引用（清理器），执行其清理动作
            try {
                // Wait for a Ref, with a timeout to avoid getting hung due to a race with clear/clean
                Cleanable ref = (Cleanable) queue.remove(60 * 1000L);   // 设置轮询时间阙值，超时则陷入阻塞
                if(ref != null) {
                    ref.clean();    // 执行清理动作
                }
            } catch(Throwable e) {
                // ignore exceptions from the cleanup action (including interruption of cleanup thread)
            }
        }
    }
    
    /**
     * Perform cleaning on an unreachable SoftReference.
     */
    /*
     *    Reference
     *        │
     * SoftReference   Cleanable
     *        └──────┬────────┘
     *        SoftCleanable
     *               │
     *        SoftCleanableRef
     *
     * 软引用清理器，此类的属性既是虚引用，又是清理器
     */
    public static final class SoftCleanableRef extends SoftCleanable<Object> {
        private final Runnable action;
        
        /**
         * Constructor for a soft cleanable reference.
         *
         * @param obj     the object to monitor
         * @param cleaner the cleaner
         * @param action  the action Runnable
         */
        // 向cleaner注册跟踪的对象obj和清理动作action
        SoftCleanableRef(Object obj, Cleaner cleaner, Runnable action) {
            super(obj, cleaner);
            this.action = action;
        }
        
        /**
         * Constructor used only for root of soft cleanable list.
         */
        SoftCleanableRef() {
            super();
            this.action = null;
        }
        
        /**
         * Prevent access to referent even when it is still alive.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public Object get() {
            throw new UnsupportedOperationException("get");
        }
        
        /**
         * Direct clearing of the referent is not supported.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public void clear() {
            throw new UnsupportedOperationException("clear");
        }
    
        // CleanerImpl.run()==>清理器clean()-->清理器performCleanup()-->action.run()
        @Override
        protected void performCleanup() {
            action.run();
        }
    }
    
    /**
     * Perform cleaning on an unreachable WeakReference.
     */
    /*
     *    Reference
     *        │
     *  WeakReference     Cleanable
     *        └──────┬────────┘
     *          WeakCleanable
     *               │
     *         WeakCleanableRef
     *
     *  虚引用清理器，此类的属性既是虚引用，又是清理器
     */
    public static final class WeakCleanableRef extends WeakCleanable<Object> {
        private final Runnable action;
        
        /**
         * Constructor for a weak cleanable reference.
         *
         * @param obj     the object to monitor
         * @param cleaner the cleaner
         * @param action  the action Runnable
         */
        // 向cleaner注册跟踪的对象obj和清理动作action
        WeakCleanableRef(Object obj, Cleaner cleaner, Runnable action) {
            super(obj, cleaner);
            this.action = action;
        }
        
        /**
         * Constructor used only for root of weak cleanable list.
         */
        WeakCleanableRef() {
            super();
            this.action = null;
        }
        
        /**
         * Prevent access to referent even when it is still alive.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public Object get() {
            throw new UnsupportedOperationException("get");
        }
        
        /**
         * Direct clearing of the referent is not supported.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public void clear() {
            throw new UnsupportedOperationException("clear");
        }
    
        // CleanerImpl.run()==>清理器clean()-->清理器performCleanup()-->action.run()
        @Override
        protected void performCleanup() {
            action.run();
        }
    }
    
    /**
     * Perform cleaning on an unreachable PhantomReference.
     */
    /*
     *    Reference
     *        │
     * PhantomReference   Cleanable
     *        └──────┬────────┘
     *        PhantomCleanable
     *               │
     *        PhantomCleanableRef
     *
     * 虚引用清理器，此类的属性既是虚引用，又是清理器
     */
    public static final class PhantomCleanableRef extends PhantomCleanable<Object> {
        private final Runnable action;
        
        /**
         * Constructor for a phantom cleanable reference.
         *
         * @param obj     the object to monitor
         * @param cleaner the cleaner
         * @param action  the action Runnable
         */
        // 向cleaner注册跟踪的对象obj和清理动作action
        public PhantomCleanableRef(Object obj, Cleaner cleaner, Runnable action) {
            super(obj, cleaner);
            this.action = action;
        }
        
        /**
         * Constructor used only for root of phantom cleanable list.
         */
        PhantomCleanableRef() {
            super();
            this.action = null;
        }
        
        /**
         * Prevent access to referent even when it is still alive.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public Object get() {
            throw new UnsupportedOperationException("get");
        }
        
        /**
         * Direct clearing of the referent is not supported.
         *
         * @throws UnsupportedOperationException always
         */
        // 清理虚引用追踪的对象的引用，由父类实现
        @Override
        public void clear() {
            throw new UnsupportedOperationException("clear");
        }
        
        // CleanerImpl.run()==>清理器clean()-->清理器performCleanup()-->action.run()
        @Override
        protected void performCleanup() {
            action.run();
        }
    }
    
    /**
     * A ThreadFactory for InnocuousThreads. The factory is a singleton.
     */
    // 清理器内部默认实现的"无害"线程工厂，用于创建"无害"线程，并将清理服务绑定到此线程
    static final class InnocuousThreadFactory implements ThreadFactory {
        final static ThreadFactory factory = new InnocuousThreadFactory();
        final AtomicInteger cleanerThreadNumber = new AtomicInteger();  // 实现原子计数
        
        // 返回单例工厂对象
        static ThreadFactory factory() {
            return factory;
        }
        
        public Thread newThread(Runnable r) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public Thread run() {
                    Thread t = InnocuousThread.newThread(r);
                    t.setPriority(Thread.MAX_PRIORITY - 2);
                    t.setName("Cleaner-" + cleanerThreadNumber.getAndIncrement());
                    return t;
                }
            });
        }
    }
    
    /**
     * A PhantomCleanable implementation for tracking the Cleaner itself.
     */
    // 创建清理的时候，在启动清理服务前，将清理器自身对象加入到追踪列表
    static final class CleanerCleanable extends PhantomCleanable<Cleaner> {
        CleanerCleanable(Cleaner cleaner) {
            super(cleaner, cleaner);
        }
        
        // 默认情形下，清理器对象自身被回收时不执行回到动作
        @Override
        protected void performCleanup() {
            // no action
        }
    }
}
