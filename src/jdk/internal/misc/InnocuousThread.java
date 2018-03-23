/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.misc;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread that has no permissions, is not a member of any user-defined
 * ThreadGroup and supports the ability to erase ThreadLocals.
 */
// "无害"线程，系统内部使用，用完之后会清理ThreadLocal信息
public final class InnocuousThread extends Thread {
    
    private static final long THREAD_LOCALS;                    // 获取Thread中threadLocals字段的地址
    private static final long INHERITABLE_THREAD_LOCALS;        // 获取Thread中inheritableThreadLocals字段的地址
    private static final long INHERITEDACCESSCONTROLCONTEXT;    // 获取Thread中inheritedAccessControlContext字段的地址
    private static final long CONTEXTCLASSLOADER;               // 获取Thread中contextClassLoader字段的地址
    
    private static final jdk.internal.misc.Unsafe UNSAFE;
    
    private static final AccessControlContext ACC;
    private static final ThreadGroup INNOCUOUSTHREADGROUP;
    
    // 原子计数器
    private static final AtomicInteger threadNumber = new AtomicInteger(1);
    
    // ensure run method is run only once
    private volatile boolean hasRun;    // 确保run方法只运行一次
    
    // Use Unsafe to access Thread group and ThreadGroup parent fields
    static {
        try {
            ACC = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, null)});
            
            UNSAFE = jdk.internal.misc.Unsafe.getUnsafe();
            
            Class<?> tk = Thread.class;
            Class<?> gk = ThreadGroup.class;
            
            THREAD_LOCALS = UNSAFE.objectFieldOffset(tk, "threadLocals");
            INHERITABLE_THREAD_LOCALS = UNSAFE.objectFieldOffset(tk, "inheritableThreadLocals");
            INHERITEDACCESSCONTROLCONTEXT = UNSAFE.objectFieldOffset(tk, "inheritedAccessControlContext");
            CONTEXTCLASSLOADER = UNSAFE.objectFieldOffset(tk, "contextClassLoader");
            
            long tg = UNSAFE.objectFieldOffset(tk, "group");
            long gp = UNSAFE.objectFieldOffset(gk, "parent");
            
            // 存储根线程组
            ThreadGroup group = (ThreadGroup) UNSAFE.getObject(Thread.currentThread(), tg);
    
            // Find and use topmost ThreadGroup as parent of new group
            while(group != null) {
                ThreadGroup parent = (ThreadGroup) UNSAFE.getObject(group, gp);
                if(parent == null)
                    break;
                group = parent;
            }
            
            final ThreadGroup root = group;
            INNOCUOUSTHREADGROUP = AccessController.doPrivileged(new PrivilegedAction<ThreadGroup>() {
                @Override
                public ThreadGroup run() {
                    return new ThreadGroup(root, "InnocuousThreadGroup");
                }
            });
        } catch(Exception e) {
            throw new Error(e);
        }
    }
    
    private InnocuousThread(ThreadGroup group, Runnable target, String name, ClassLoader tccl) {
        super(group, target, name, 0L, false);
        UNSAFE.putObjectRelease(this, INHERITEDACCESSCONTROLCONTEXT, ACC);
        UNSAFE.putObjectRelease(this, CONTEXTCLASSLOADER, tccl);
    }
    
    /*
     * Returns a new InnocuousThread with an auto-generated thread name and its context class loader is set to the system class loader.
     *
     * 返回一个新的"无害"线程(InnocuousThread)。该线程的名称是自动生成的，其类加载器被设置为AppClassLoader。
     */
    public static Thread newThread(Runnable target) {
        return newThread(newName(), target);
    }
    
    /*
     * Returns a new InnocuousThread with its context class loader set to the system class loader.
     *
     * 返回一个新的"无害"线程(InnocuousThread)，其类加载器被设置为AppClassLoader。
     */
    public static Thread newThread(String name, Runnable target) {
        return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
            @Override
            public Thread run() {
                return new InnocuousThread(INNOCUOUSTHREADGROUP, target, name, ClassLoader.getSystemClassLoader());
            }
        });
    }
    
    /**
     * Returns a new InnocuousThread with an auto-generated thread name. Its context class loader is set to null.
     *
     * 返回一个新的"无害"线程(InnocuousThread)。该线程的名称是自动生成的，其类加载器被设置为null。
     */
    public static Thread newSystemThread(Runnable target) {
        return newSystemThread(newName(), target);
    }
    
    /*
     * Returns a new InnocuousThread with null context class loader.
     *
     * 返回一个新的"无害"线程(InnocuousThread)，其类加载器被设置为null。
     */
    public static Thread newSystemThread(String name, Runnable target) {
        return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
            @Override
            public Thread run() {
                return new InnocuousThread(INNOCUOUSTHREADGROUP, target, name, null);
            }
        });
    }
    
    // 自动生成线程名称
    private static String newName() {
        return "InnocuousThread-" + threadNumber.getAndIncrement();
    }
    
    /*
     * Drops all thread locals (and inherited thread locals).
     *
     * 删除Thread中的threadLocals和inheritableThreadLocals。
     * 即清除该线程中所有ThreadLocal信息。
     */
    public final void eraseThreadLocals() {
        UNSAFE.putObject(this, THREAD_LOCALS, null);
        UNSAFE.putObject(this, INHERITABLE_THREAD_LOCALS, null);
    }
    
    @Override
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler x) {
        // silently fail
    }
    
    @Override
    public void setContextClassLoader(ClassLoader cl) {
        // Allow clearing of the TCCL to remove the reference to the system classloader.
        if(cl == null)
            super.setContextClassLoader(null);
        else
            throw new SecurityException("setContextClassLoader");
    }
    
    // 执行线程的动作（只执行一次）
    @Override
    public void run() {
        if(Thread.currentThread() == this && !hasRun) {
            hasRun = true;
            super.run();
        }
    }
}
