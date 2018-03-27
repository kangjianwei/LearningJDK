/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.nio;

import jdk.internal.misc.JavaLangRefAccess;
import jdk.internal.misc.JavaNioAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Access to bits, native and otherwise.
 */
// 工具类，用于存储本地内存管理参数和转换字节顺序
class Bits {
    /**
     * These numbers represent the point at which we have empirically determined that the average cost of a JNI call exceeds the expense of an element by element copy.
     * These numbers may change over time.
     *
     * 这些数字代表了我们凭经验确定的使用JNI调用的平均成本超过使用元素副本开销的点。这些数字可能随时间而变化。
     */
    
    static final int JNI_COPY_TO_ARRAY_THRESHOLD = 6;
    static final int JNI_COPY_FROM_ARRAY_THRESHOLD = 6;
    
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    
    // 记录已经用掉的总容量（初值为0）
    private static final AtomicLong TOTAL_CAPACITY = new AtomicLong();
    
    // 本次成功分配的容量，记下此变量的目的是稍后还要释放此内存
    private static final AtomicLong RESERVED_MEMORY = new AtomicLong();
    // 内存分配次数，分配内存时增1，释放内存时减1
    private static final AtomicLong COUNT = new AtomicLong();
    
    static final JavaNioAccess.BufferPool BUFFER_POOL = new JavaNioAccess.BufferPool() {
        @Override
        public String getName() {
            return "direct";
        }
        
        @Override
        public long getCount() {
            return Bits.COUNT.get();
        }
        
        @Override
        public long getTotalCapacity() {
            return Bits.TOTAL_CAPACITY.get();
        }
        
        @Override
        public long getMemoryUsed() {
            return Bits.RESERVED_MEMORY.get();
        }
    };
    
    // max number of sleeps during try-reserving with exponentially increasing delay before throwing OutOfMemoryError:
    // 1, 2, 4, 8, 16, 32, 64, 128, 256 (total 511 ms ~ 0.5 s)
    // which means that OOME will be thrown after 0.5 s of trying
    private static final int MAX_SLEEPS = 9;
    
    private static int PAGE_SIZE = -1;
    
    // 返回true代表该平台支持对字节未对齐的基本类型访问
    private static boolean UNALIGNED = UNSAFE.unalignedAccess();
    
    // A user-settable upper limit on the maximum amount of allocatable direct buffer memory.
    // This value may be changed during VM initialization if it is launched with "-XX:MaxDirectMemorySize=<size>".
    private static volatile long MAX_MEMORY = VM.maxDirectMemory();
    
    // 是否获取到了直接内存分配上限（只获取一次）
    private static volatile boolean MEMORY_LIMIT_SET;
    
    private Bits() {
    }
    
    
    
    /*▼ 转换字节顺序（大小端转换） ████████████████████████████████████████████████████████████████████████████████┓ */
    
    static short swap(short x) {
        return Short.reverseBytes(x);
    }
    
    static char swap(char x) {
        return Character.reverseBytes(x);
    }
    
    static int swap(int x) {
        return Integer.reverseBytes(x);
    }
    
    static long swap(long x) {
        return Long.reverseBytes(x);
    }
    
    /*▲ 转换字节顺序（大小端转换） ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回分页大小
    static int pageSize() {
        if(PAGE_SIZE == -1)
            PAGE_SIZE = UNSAFE.pageSize();
        return PAGE_SIZE;
    }
    
    // 返回分页数量
    static int pageCount(long size) {
        return (int) (size + (long) pageSize() - 1L) / pageSize();
    }
    
    // 是否支持对非字节对齐的访问
    static boolean unaligned() {
        return UNALIGNED;
    }
    
    /**
     * These methods should be called whenever direct memory is allocated or freed.
     * They allow the user to control the amount of direct memory which a process may access.  All sizes are specified in bytes.
     *
     */
    // 尽量保障有足够多内存可以使用
    static void reserveMemory(long size, int cap) {
        // 还未获取到直接内存分配上限
        if(!MEMORY_LIMIT_SET && VM.initLevel() >= 1) {
            // 获取到了允许分配的最大直接内存容量
            MAX_MEMORY = VM.maxDirectMemory();
            MEMORY_LIMIT_SET = true;
        }
        
        // 检查申请的内存是否超量，不超量的话，设置相应的内存参数
        if(tryReserveMemory(size, cap)) {
            // 内存足够的话，直接返回
            return;
        }
        
        final JavaLangRefAccess jlra = SharedSecrets.getJavaLangRefAccess();
        boolean interrupted = false;
        try {
            
            // Retry allocation until success or there are no more references (including Cleaners that might free direct buffer memory) to process and allocation still fails.
            boolean refprocActive;
            do {
                try {
                    refprocActive = jlra.waitForReferenceProcessing();
                } catch(InterruptedException e) {
                    // Defer interrupts and keep trying.
                    interrupted = true;
                    refprocActive = true;
                }
                if(tryReserveMemory(size, cap)) {
                    return;
                }
            } while(refprocActive);
            
            // 触发垃圾回收
            System.gc();
            
            // A retry loop with exponential back-off delays.
            // Sometimes it would suffice to give up once reference processing is complete.
            // But if there are many threads competing for memory, this gives more opportunities for any given thread to make progress.
            // In particular, this seems to be enough for a stress test like DirectBufferAllocTest to (usually) succeed, while without it that test likely fails.
            // Since failure here ends in OOME, there's no need to hurry.
            long sleepTime = 1;
            int sleeps = 0;
            while(true) {
                if(tryReserveMemory(size, cap)) {
                    return;
                }
                if(sleeps >= MAX_SLEEPS) {
                    break;
                }
                try {
                    if(!jlra.waitForReferenceProcessing()) {
                        Thread.sleep(sleepTime);
                        sleepTime <<= 1;
                        sleeps++;
                    }
                } catch(InterruptedException e) {
                    interrupted = true;
                }
            }
            
            // no luck
            throw new OutOfMemoryError("Direct buffer memory");
            
        } finally {
            if(interrupted) {
                // don't swallow interrupts
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // 内存不够，则取消之前设置的内容参数
    static void unreserveMemory(long size, int cap) {
        long cnt = COUNT.decrementAndGet();
        long reservedMem = RESERVED_MEMORY.addAndGet(-size);
        long totalCap = TOTAL_CAPACITY.addAndGet(-cap);
        assert cnt >= 0 && reservedMem >= 0 && totalCap >= 0;
    }
    
    // 检查申请的内存是否超量，不超量的话，设置相应的内存参数
    private static boolean tryReserveMemory(long size, int cap) {
        // -XX:MaxDirectMemorySize limits the total capacity rather than the actual memory usage, which will differ when buffers are page aligned.
        long totalCap;
        // 判断剩余容量是否充足
        while(cap <= MAX_MEMORY - (totalCap = TOTAL_CAPACITY.get())) {
            // 增加已使用容量的计数
            if(TOTAL_CAPACITY.compareAndSet(totalCap, totalCap + cap)) {
                RESERVED_MEMORY.addAndGet(size);
                COUNT.incrementAndGet();
                return true;
            }
        }
        
        return false;
    }
}
