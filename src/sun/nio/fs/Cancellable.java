/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import jdk.internal.misc.Unsafe;
import java.util.concurrent.ExecutionException;

/**
 * Base implementation of a task (typically native) that polls a memory location during execution so that it may be aborted/cancelled before completion.
 * The task is executed by invoking the {@link runInterruptibly} method defined here and cancelled by invoking Thread.interrupt.
 */
// 可取消的任务，该任务受线程中断标记的影响
abstract class Cancellable implements Runnable {
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    private final Object lock = new Object();
    
    private final long pollingAddress;  // 指向本地内存，保存"取消"状态码
    
    // the following require lock when examining or changing
    private boolean completed;  // 当前任务是否已完成
    
    private Throwable exception;    // 异常信息
    
    protected Cancellable() {
        // 申请4字节的本地内存，并返回分配的内存地址
        pollingAddress = unsafe.allocateMemory(4);
        // 置空
        unsafe.putIntVolatile(null, pollingAddress, 0);
    }
    
    @Override
    public final void run() {
        try {
            // 执行当前任务
            implRun();
        } catch(Throwable t) {
            synchronized(lock) {
                exception = t;
            }
        } finally {
            synchronized(lock) {
                completed = true;
                unsafe.freeMemory(pollingAddress);
            }
        }
    }
    
    /**
     * The task body. This should periodically poll the memory location to check for cancellation.
     */
    // 对当前任务执行的具体实现由不同的平台自行实现
    abstract void implRun() throws Throwable;
    
    /**
     * Returns the memory address of a 4-byte int that should be polled to detect cancellation.
     */
    protected long addressToPollForCancel() {
        return pollingAddress;
    }
    
    /**
     * The value to write to the polled memory location to indicate that the task has been cancelled.
     * If this method is not overridden then it defaults to MAX_VALUE.
     */
    // 返回取消任务时的状态码，可覆盖此实现
    protected int cancelValue() {
        return Integer.MAX_VALUE;
    }
    
    /**
     * Invokes the given task in its own thread. If this (meaning the current)
     * thread is interrupted then an attempt is make to cancel the background
     * thread by writing into the memory location that it polls cooperatively.
     */
    // 执行指定的任务，会阻塞发起当前操作的线程；该任务会响应线程中断
    static void runInterruptibly(Cancellable task) throws ExecutionException {
        
        // 启动任务
        Thread copyThread = new Thread(null, task, "NIO-Task", 0, false);
        copyThread.start();
        
        boolean cancelledByInterrupt = false;
        
        while(copyThread.isAlive()) {
            try {
                // 使copyThread线程进入WAITING状态，直到copyThread线程执行完成，或被中断之后，再唤醒runInterruptibly()的调用者所在的线程
                copyThread.join();
            } catch(InterruptedException e) {
                cancelledByInterrupt = true;
                task.cancel();  // 响应中断，停止copyThread
            }
        }
        
        if(cancelledByInterrupt) {
            // 中断线程（只是给线程预设一个标记，不是立即让线程停下来）
            Thread.currentThread().interrupt();
        }
        
        // 如果发生异常，需要抛出
        Throwable exc = task.exception();
        if(exc != null) {
            throw new ExecutionException(exc);
        }
    }
    
    /**
     * "cancels" the task by writing bits into memory location that it polled by the task.
     */
    // 取消当前任务
    final void cancel() {
        synchronized(lock) {
            if(!completed) {
                // 保存"取消"状态码
                unsafe.putIntVolatile(null, pollingAddress, cancelValue());
            }
        }
    }
    
    /**
     * Returns the exception thrown by the task or null if the task completed successfully.
     */
    // 返回执行过程中的异常信息
    private Throwable exception() {
        synchronized(lock) {
            return exception;
        }
    }
    
}
