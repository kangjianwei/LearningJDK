/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;


import jdk.internal.misc.VM;

/**
 * Package-private utility class containing data structures and logic governing the virtual-machine shutdown sequence.
 *
 * @author Mark Reinhold
 * @see java.io.Console
 * @see ApplicationShutdownHooks
 * @see java.io.DeleteOnExitHook
 * @since 1.3
 */
// 虚拟机关闭控制器，可在此注册一些需要在虚拟机关闭时执行的钩子
class Shutdown {
    
    /**
     * The system shutdown hooks are registered with a predefined slot.
     * The list of shutdown hooks is as follows:
     * (0) Console restore hook
     * (1) ApplicationShutdownHooks that invokes all registered application shutdown hooks and waits until they finish
     * (2) DeleteOnExit hook
     */
    /*
     * 钩子数量(编号)上限
     *
     * 系统中已经占用的钩子编号：
     * (0) Console
     * (1) ApplicationShutdownHooks
     * (2) DeleteOnExit
     */
    private static final int MAX_SYSTEM_HOOKS = 10;
    
    // 钩子列表
    private static final Runnable[] hooks = new Runnable[MAX_SYSTEM_HOOKS];
    
    private static Object lock = new Lock();
    
    /** Lock object for the native halt method */
    private static Object haltLock = new Lock();
    
    /** the index of the currently running shutdown hook to the hooks array */
    private static int currentRunningHook = -1;
    
    /**
     * Add a new system shutdown hook.
     * Checks the shutdown state and the hook itself, but does not do any security checks.
     *
     * The registerShutdownInProgress parameter should be false except
     * registering the DeleteOnExitHook since the first file may
     * be added to the delete on exit list by the application shutdown
     * hooks.
     *
     * @param slot                       the slot in the shutdown hook array, whose element will be invoked in order during shutdown
     * @param registerShutdownInProgress true to allow the hook to be registered even if the shutdown is in progress.
     * @param hook                       the hook to be registered
     *
     * @throws IllegalStateException if registerShutdownInProgress is false and shutdown is in progress; or
     *                               if registerShutdownInProgress is true and the shutdown process
     *                               already passes the given slot
     */
    // 将指定的钩子hook注册到编号为slot的插槽中，以便在虚拟机关闭时处理这些钩子
    static void add(int slot, boolean registerShutdownInProgress, Runnable hook) {
        // 要求slot处为空
        if(slot<0 || slot >= MAX_SYSTEM_HOOKS) {
            throw new IllegalArgumentException("Invalid slot: " + slot);
        }
    
        synchronized(lock) {
            // 此处已经存在回调
            if(hooks[slot] != null) {
                throw new InternalError("Shutdown hook at slot " + slot + " already registered");
            }
        
            // 如果关闭过程中(调用runHooks()的过程中)禁止注册钩子
            if(!registerShutdownInProgress) {
                if(currentRunningHook >= 0) {
                    throw new IllegalStateException("Shutdown in progress");
                }
            
                // 即使在关闭过程中(还未关闭)也可以注册钩子
            } else {
                /*
                 * 确保钩子编号要大于currentRunningHook，
                 * 即在关闭过程中，即使某个插槽的钩子已被执行，插槽已置空，
                 * 此处也不能再放入新的钩子
                 */
                if(VM.isShutdown() || slot<=currentRunningHook) {
                    throw new IllegalStateException("Shutdown in progress");
                }
            }
        
            hooks[slot] = hook;
        }
    }
    
    /**
     * Invoked by the JNI DestroyJavaVM procedure when the last non-daemon thread has finished.
     * Unlike the exit method, this method does not actually halt the VM.
     */
    // 执行所有钩子，并标记虚拟机进入关闭状态(还未真正关闭)
    static void shutdown() {
        synchronized(Shutdown.class) {
            runHooks();
        }
    }
    
    /**
     * Invoked by Runtime.exit, which does all the security checks.
     * Also invoked by handlers for system-provided termination events,
     * which should pass a nonzero status code.
     */
    // 执行所有钩子，并退出(关闭)虚拟机
    static void exit(int status) {
        synchronized(lock) {
            // 如果是非0的状态码，且虚拟机已被标记为关闭状态
            if(status != 0 && VM.isShutdown()) {
                /* Halt immediately on nonzero status */
                halt(status);   // 立即关闭虚拟机
            }
        }
        
        synchronized(Shutdown.class) {
            /*
             * Synchronize on the class object, causing any other thread
             * that attempts to initiate shutdown to stall indefinitely
             */
            beforeHalt();   // 准备关闭
            runHooks();     // 执行钩子
            halt(status);   // 关闭虚拟机
        }
    }
    
    /**
     * Run all system shutdown hooks.
     *
     * The system shutdown hooks are run in the thread synchronized on Shutdown.class.
     * Other threads calling Runtime::exit, Runtime::halt or JNI DestroyJavaVM will block indefinitely.
     *
     * ApplicationShutdownHooks is registered as one single hook that starts all application shutdown hooks and waits until they finish.
     */
    // 执行所有注册在系统关闭时的钩子
    private static void runHooks() {
        synchronized(lock) {
            /*
             * Guard against the possibility of a daemon thread invoking exit
             * after DestroyJavaVM initiates the shutdown sequence
             */
            // 如果虚拟机已进入关闭状态，直接返回
            if(VM.isShutdown()) {
                return;
            }
        }
        
        // 遍历所有注册的钩子
        for(int i = 0; i<MAX_SYSTEM_HOOKS; i++) {
            try {
                Runnable hook;
                
                synchronized(lock) {
                    // acquire the lock to make sure the hook registered during shutdown is visible here.
                    currentRunningHook = i;
                    hook = hooks[i];
                }
                
                if(hook != null) {
                    hook.run(); // 执行钩子方法
                }
            } catch(Throwable t) {
                if(t instanceof ThreadDeath) {
                    ThreadDeath td = (ThreadDeath) t;
                    throw td;
                }
            }
        }
        
        // set shutdown state
        VM.shutdown();  // 标记虚拟机进入关闭状态
    }
    
    /* Notify the VM that it's time to halt. */
    // 通知虚拟机程序该终止了
    static native void beforeHalt();
    
    /**
     * The halt method is synchronized on the halt lock to avoid corruption of the delete-on-shutdown file list.
     * It invokes the true native halt method.
     */
    // 关闭虚拟机
    static void halt(int status) {
        synchronized(haltLock) {
            halt0(status);
        }
    }
    
    // 关闭虚拟机的内部实现，status为关闭时的状态码，一般用非0的状态码表示异常退出状态
    static native void halt0(int status);
    
    /* The preceding static fields are protected by this lock */
    private static class Lock {
    }
    
}
