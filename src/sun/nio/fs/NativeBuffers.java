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

import jdk.internal.misc.TerminatingThreadLocal;
import jdk.internal.misc.Unsafe;

/**
 * Factory for native buffers.
 */
// 缓存池，缓存本地内存，避免频繁创建/释放本地内存
class NativeBuffers {
    private static final int TEMP_BUF_POOL_SIZE = 3;
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    // 缓存池
    private static ThreadLocal<NativeBuffer[]> threadLocal = new TerminatingThreadLocal<>() {
        // 对buffers做一些收尾操作，这里的实现是回收其内存
        @Override
        protected void threadTerminated(NativeBuffer[] buffers) {
            // threadLocal may be initialized but with initialValue of null
            if(buffers != null) {
                for(int i = 0; i<TEMP_BUF_POOL_SIZE; i++) {
                    NativeBuffer buffer = buffers[i];
                    if(buffer != null) {
                        buffer.free();
                        buffers[i] = null;
                    }
                }
            }
        }
    };
    
    private NativeBuffers() {
    }
    
    /**
     * Returns a native buffer, of at least the given size. The native buffer
     * is taken from the thread local cache if possible; otherwise it is
     * allocated from the heap.
     */
    // 返回容量至少为size的缓存(owner为null)
    static NativeBuffer getNativeBuffer(int size) {
        // 从缓存池中获取容量至少为size的缓存
        NativeBuffer buffer = getNativeBufferFromCache(size);
        
        // 如果获取成功，则返回该缓存
        if(buffer != null) {
            // 清理owner
            buffer.setOwner(null);
            
            return buffer;
            
            // 如果获取失败，则需要新建缓存
        } else {
            // 返回新建的缓存(使用了本地内存)
            return allocNativeBuffer(size);
        }
    }
    
    /**
     * Allocates a native buffer, of at least the given size, from the heap.
     */
    // 返回新建的缓存(使用了本地内存)
    static NativeBuffer allocNativeBuffer(int size) {
        // Make a new one of at least 2k
        if(size<2048) {
            size = 2048;    // 最少分配2K内存
        }
        
        return new NativeBuffer(size);
    }
    
    /**
     * Returns a native buffer, of at least the given size, from the thread local cache.
     */
    // 从缓存池中获取容量至少为size的缓存
    static NativeBuffer getNativeBufferFromCache(int size) {
        // 尝试从缓存池中获取
        NativeBuffer[] buffers = threadLocal.get(); // return from cache if possible
    
        if(buffers != null) {
            for(int i = 0; i<TEMP_BUF_POOL_SIZE; i++) {
                NativeBuffer buffer = buffers[i];
                if(buffer != null && buffer.size() >= size) {
                    buffers[i] = null;
                    return buffer;
                }
            }
        }
    
        return null;
    }
    
    /**
     * Releases the given buffer.
     * If there is space in the thread local cache then the buffer goes into the cache; otherwise the memory is deallocated.
     */
    // 将指定的buffer放入缓存池。如果没必要缓存，则释放它
    static void releaseNativeBuffer(NativeBuffer buffer) {
        // create cache if it doesn't exist
        NativeBuffer[] buffers = threadLocal.get();
        if(buffers == null) {
            buffers = new NativeBuffer[TEMP_BUF_POOL_SIZE];
            buffers[0] = buffer;
            threadLocal.set(buffers);
            return;
        }
    
        // Put it in an empty slot if such exists
        for(int i = 0; i<TEMP_BUF_POOL_SIZE; i++) {
            // 缓存池有空槽的话，将buffer放入缓冲池
            if(buffers[i] == null) {
                buffers[i] = buffer;
                return;
            }
        }
    
        // Otherwise replace a smaller one in the cache if such exists
        for(int i = 0; i<TEMP_BUF_POOL_SIZE; i++) {
            NativeBuffer existing = buffers[i];
            // 缓冲池没空槽的话，如果buffer容量大于现有缓存池中缓存的容量，则将buffer放入缓冲池
            if(existing.size()<buffer.size()) {
                existing.free();
                buffers[i] = buffer;
                return;
            }
        }
    
        // buffer没必要缓存时，将其释放
        buffer.free();
    }
    
    /**
     * Copies a byte array and zero terminator into a given native buffer.
     */
    // 将cstr中的数据拷贝到buffer中，buffer多余的空间会被填充为0
    static void copyCStringToNativeBuffer(byte[] cstr, NativeBuffer buffer) {
        long offset = Unsafe.ARRAY_BYTE_BASE_OFFSET;
        long len = cstr.length;
    
        assert buffer.size() >= (len + 1);
    
        // 将cstr中的数据拷贝到buffer中
        unsafe.copyMemory(cstr, offset, null, buffer.address(), len);
    
        // 将buffer后面没数据的空槽填充为0
        unsafe.putByte(buffer.address() + len, (byte) 0);
    }
    
    /**
     * Copies a byte array and zero terminator into a native buffer, returning
     * the buffer.
     */
    // 将cstr中的数据拷贝到一块缓存中后返回
    static NativeBuffer asNativeBuffer(byte[] cstr) {
        // 获取容量至少为cstr.length + 1的缓存(owner为null)
        NativeBuffer buffer = getNativeBuffer(cstr.length + 1);
    
        // 将cstr中的数据拷贝到buffer中，buffer多余的空间会被填充为0
        copyCStringToNativeBuffer(cstr, buffer);
    
        return buffer;
    }
    
}
