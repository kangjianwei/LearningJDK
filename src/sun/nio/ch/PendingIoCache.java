/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.channels.*;
import java.util.*;
import jdk.internal.misc.Unsafe;

/**
 * Maintains a mapping of pending I/O requests (identified by the address of
 * an OVERLAPPED structure) to Futures.
 */
// 重叠IO结构的缓存池：用来缓存重叠IO结构，以便复用
class PendingIoCache {
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    // 本地指针长度
    private static final int addressSize = unsafe.addressSize();
    
    /*
     * typedef struct _OVERLAPPED {
     *     DWORD  Internal;
     *     DWORD  InternalHigh;
     *     DWORD  Offset;
     *     DWORD  OffsetHigh;
     *     HANDLE hEvent;
     * } OVERLAPPED;
     */
    // 重叠IO结构在本地(nvtive层)所占的字节数
    private static final int SIZEOF_OVERLAPPED = dependsArch(20, 32);
    
    /** maps OVERLAPPED to PendingFuture */
    /*
     * 任务结果映射集，key用来在本地(native)存储执行结果，value用来在Java层存储执行结果
     *
     * key  : OVERLAPPED结构的本地引用
     * value: "已挂起的结果"，等待填充执行结果
     */
    @SuppressWarnings("rawtypes")
    private final Map<Long, PendingFuture> pendingIoMap = new HashMap<Long, PendingFuture>();
    
    /** per-channel cache of OVERLAPPED structures */
    private long[] overlappedCache = new long[4];   // 重叠IO结构的缓存
    private int overlappedCacheCount = 0;           // 重叠IO结果的缓存数量
    
    /** set to true when thread is waiting for all I/O operations to complete */
    // 是否正在清空任务结果映射集(pendingIoMap)
    private boolean closePending;
    
    /** set to true when closed */
    // 是否已经关闭当前重叠IO结构的缓存池
    private boolean closed;
    
    
    PendingIoCache() {
    }
    
    
    // 向任务结果映射集添加一条记录
    long add(PendingFuture<?, ?> future) {
        synchronized(this) {
            if(closed) {
                throw new AssertionError("Should not get here");
            }
            
            long ov;
            
            // 如果存在可用的重叠IO结构缓存，则优先使用缓存
            if(overlappedCacheCount>0) {
                ov = overlappedCache[--overlappedCacheCount];
            } else {
                // 没有缓存的话，新建一个重叠IO结构：申请SIZEOF_OVERLAPPED字节的本地内存，并返回分配的内存地址
                ov = unsafe.allocateMemory(SIZEOF_OVERLAPPED);
            }
            
            pendingIoMap.put(ov, future);
            
            return ov;
        }
    }
    
    // 从任务结果映射集移除一条记录；会尝试缓存重叠IO结构，缓存池已满时则直接释放重叠IO结构的本地内存
    @SuppressWarnings("unchecked")
    <V, A> PendingFuture<V, A> remove(long overlapped) {
        synchronized(this) {
            PendingFuture<V, A> future = pendingIoMap.remove(overlapped);
            if(future == null) {
                return future;
            }
            
            // 缓存已经用完的重叠IO结构，以便后续复用
            if(overlappedCacheCount<overlappedCache.length) {
                overlappedCache[overlappedCacheCount++] = overlapped;
            } else {
                // 如果缓存已经满了，则释放本地内存
                unsafe.freeMemory(overlapped);
            }
            
            if(closePending) {
                this.notifyAll();
            }
            
            return future;
        }
    }
    
    // 关闭重叠IO结构的缓存池
    void close() {
        synchronized(this) {
            if(closed) {
                return;
            }
            
            /* handle case where I/O operations that have not completed */
            /*
             * 如果任务结果映射集非空，则清空任务结果映射集，包含两部分操作：
             * 1.将key中的重叠IO结构标记为失效
             * 2.向value中"已挂起的结果"填充失败信息
             */
            if(!pendingIoMap.isEmpty()) {
                clearPendingIoMap();
            }
            
            // release memory for any cached OVERLAPPED structures */
            // 释放所有重叠IO结构所占的本地内存
            while(overlappedCacheCount>0) {
                unsafe.freeMemory(overlappedCache[--overlappedCacheCount]);
            }
            
            // done
            closed = true;
        }
    }
    
    /*
     * 清空任务结果映射集，包含两部分操作：
     * 1.将key中的重叠IO结构标记为失效
     * 2.向value中"已挂起的结果"填充失败信息
     */
    private void clearPendingIoMap() {
        assert Thread.holdsLock(this);
        
        /* wait up to 50ms for the I/O operations to complete */
        closePending = true;
        try {
            this.wait(50);
        } catch(InterruptedException x) {
            Thread.currentThread().interrupt();
        }
        
        closePending = false;
        
        if(pendingIoMap.isEmpty()) {
            return;
        }
        
        /* cause all pending I/O operations to fail simulate the failure of all pending I/O operations */
        // 遍历任务结果映射集
        for(Long ov : pendingIoMap.keySet()) {
            PendingFuture<?, ?> future = pendingIoMap.get(ov);
            assert !future.isDone();
            
            /* make I/O port aware of the stale OVERLAPPED structure */
            Iocp iocp = (Iocp) ((Groupable) future.channel()).group();
            
            // 将指定的重叠IO结构标记为失效
            iocp.makeStale(ov);
            
            /* execute a task that invokes the result handler's failed method */
            final Iocp.ResultHandler rh = (Iocp.ResultHandler) future.getContext();
            
            // 构造一个用来设置任务结果的task
            Runnable task = new Runnable() {
                public void run() {
                    // 指示与该重叠IO结果相关的任务以失败告终
                    rh.failed(-1, new AsynchronousCloseException());
                }
            };
            
            /*
             * 让异步IO线程池处理指定的task。
             *
             * 如果线程池容量固定，则唤醒正在阻塞的工作线程处理task。
             * 如果线程池容量不固定，则将task提交到线程池中以启动新的线程来处理task。
             */
            iocp.executeOnPooledThread(task);
        }
        
        // 清空任务结果映射集
        pendingIoMap.clear();
    }
    
    
    private static int dependsArch(int value32, int value64) {
        return (addressSize == 4) ? value32 : value64;
    }
    
}
