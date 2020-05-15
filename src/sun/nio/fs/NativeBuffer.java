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

import java.lang.ref.Cleaner.Cleanable;
import jdk.internal.misc.Unsafe;
import jdk.internal.ref.CleanerFactory;

/**
 * A light-weight buffer in native memory.
 */
// 轻量级的缓存，使用了本地内存
class NativeBuffer {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    private final Cleanable cleanable;  // 清理器，用来释放本地内存
    
    private final long address; // 本地内存地址
    private final int size;     // 本地内存容量
    
    // optional "owner" to avoid copying (only safe for use by thread-local caches)
    private Object owner;       // 记录当前缓冲区的所有者
    
    NativeBuffer(int size) {
        // 申请size字节的本地内存，并返回分配的内存地址
        this.address = unsafe.allocateMemory(size);
        this.size = size;
        // 将当前内存的地址注册到清理器，以待后续清理该内存
        this.cleanable = CleanerFactory.cleaner().register(this, new Deallocator(address));
    }
    
    // 返回当前缓存使用的本地内存地址
    long address() {
        return address;
    }
    
    // 返回当前缓存使用的本地内存容量
    int size() {
        return size;
    }
    
    // 释放当前缓存使用的本地内存
    void free() {
        cleanable.clean();
    }
    
    // 将当前缓存放入缓存池。如果没必要缓存，则释放它
    void release() {
        NativeBuffers.releaseNativeBuffer(this);
    }
    
    // not synchronized; only safe for use by thread-local caches
    // 设置当前缓冲区的所有者
    void setOwner(Object owner) {
        this.owner = owner;
    }
    
    // not synchronized; only safe for use by thread-local caches
    // 返回当前缓冲区的所有者
    Object owner() {
        return owner;
    }
    
    
    private static class Deallocator implements Runnable {
        private final long address;
        
        Deallocator(long address) {
            this.address = address;
        }
        
        public void run() {
            // 用于释放allocateMemory和reallocateMemory申请的内存
            unsafe.freeMemory(address);
        }
    }
}
