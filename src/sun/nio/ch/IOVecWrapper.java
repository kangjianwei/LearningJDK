/*
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import jdk.internal.ref.CleanerFactory;


/**
 * Manipulates a native array of iovec structs on Solaris:
 *
 * typedef struct iovec {
 * caddr_t  iov_base;
 * int      iov_len;
 * } iovec_t;
 *
 * @author Mike McCloskey
 * @since 1.4
 */
/*
 * IOVecWrapper代表底层结构体iovec的数组，结构体iovec用作读取操作中的src或写入操作中的dst
 *
 * typedef struct {
 *     void* iov_base;  // 本地堆内存的基址
 *     int   iov_len;   // 本地内存块容量
 * } iovec;
 */
class IOVecWrapper {
    
    /** Miscellaneous constants */
    private static final int BASE_OFFSET = 0;   // iov_base变量在结构体iovec中的偏移量
    private static final int LEN_OFFSET;        // iov_len变量在结构体iovec中的偏移量
    private static final int SIZE_IOVEC;        // 结构体iovec所占内存大小
    
    /** per thread IOVecWrapper */
    // 缓存，线程私有
    private static final ThreadLocal<IOVecWrapper> cached = new ThreadLocal<IOVecWrapper>();
    
    // iovec数组在JAVA层的表示（注：在JAVA层存储的是每个结构体本地内存的地址）
    private final AllocatedNativeObject vecArray;
    
    /** Base address of this array */
    // 本地内存块vecArray使用的起始地址
    final long address;
    
    // 本机指针的大小（以字节为单位）。此值为4或8
    static int addressSize;
    
    /** Number of elements in iovec array */
    // vecArray中的数组容量（不是字节数，是按数组元素计算的）
    private final int size;
    
    /** Buffers and position/remaining corresponding to elements in iovec array */
    // 记录待写的缓冲区或者待读的数据源的信息
    private final ByteBuffer[] buf;
    private final int[] position;
    private final int[] remaining;
    
    /** Shadow buffers for cases when original buffer is substituted */
    // 记录影子缓冲区的信息
    private final ByteBuffer[] shadow;
    
    
    static {
        // 获取本机指针的大小（以字节为单位）。此值为4或8
        addressSize = Util.unsafe().addressSize();
        
        // iov_len变量在结构体iovec中的偏移量
        LEN_OFFSET = addressSize;
        
        /*
         * 为结构体iovec分配的内存大小
         *
         * 由于它的一个变量是指针类型，而另一个变量是int类型，
         * 且int类型与本地指针大小往往一致
         * 所以这里需要分配的容量为指针大小的2倍
         */
        SIZE_IOVEC = (short) (addressSize * 2);
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private IOVecWrapper(int size) {
        this.size = size;
        this.buf = new ByteBuffer[size];
        this.position = new int[size];
        this.remaining = new int[size];
        this.shadow = new ByteBuffer[size];
        this.vecArray = new AllocatedNativeObject(size * SIZE_IOVEC, false);
        this.address = vecArray.address();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 创建长度为size的结构体iovec的数组
    static IOVecWrapper get(int size) {
        IOVecWrapper wrapper = cached.get();
        
        // 如果已存在缓存，但其容量不足，则释放其内存，以便重新创建
        if(wrapper != null && wrapper.size<size) {
            // not big enough; eagerly release memory
            wrapper.vecArray.free();
            wrapper = null;
        }
        
        // 创建新的对象
        if(wrapper == null) {
            wrapper = new IOVecWrapper(size);
            // 向Cleaner注册跟踪的对象wrapper和清理动作Deallocator#run()
            CleanerFactory.cleaner().register(wrapper, new Deallocator(wrapper.vecArray));
            cached.set(wrapper);
        }
        
        return wrapper;
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 缓冲区 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 在IOVecWrapper中记录buf的存储信息<buf, pos, rem>
    void setBuffer(int i, ByteBuffer buf, int pos, int rem) {
        this.buf[i] = buf;
        this.position[i] = pos;
        this.remaining[i] = rem;
    }
    
    ByteBuffer getBuffer(int i) {
        return buf[i];
    }
    
    int getPosition(int i) {
        return position[i];
    }
    
    int getRemaining(int i) {
        return remaining[i];
    }
    
    
    // 在IOVecWrapper中记录影子缓冲区buf的信息
    void setShadow(int i, ByteBuffer buf) {
        shadow[i] = buf;
    }
    
    ByteBuffer getShadow(int i) {
        return shadow[i];
    }
    
    
    void clearRefs(int i) {
        buf[i] = null;
        shadow[i] = null;
    }
    
    /*▲ 缓冲区 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 本地内存操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 记录本地堆内存的基址（指向一块本地内存）
    void putBase(int i, long base) {
        int offset = SIZE_IOVEC * i + BASE_OFFSET;
        
        if(addressSize == 4) {
            vecArray.putInt(offset, (int) base);
        } else {
            vecArray.putLong(offset, base);
        }
    }
    
    // 记录本地内存容量
    void putLen(int i, long len) {
        int offset = SIZE_IOVEC * i + LEN_OFFSET;
        
        if(addressSize == 4) {
            vecArray.putInt(offset, (int) len);
        } else {
            vecArray.putLong(offset, len);
        }
    }
    
    /*▲ 本地内存操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 用作清理本地内存
    private static class Deallocator implements Runnable {
        private final AllocatedNativeObject obj;
        
        Deallocator(AllocatedNativeObject obj) {
            this.obj = obj;
        }
        
        // 释放本地内存
        public void run() {
            obj.free();
        }
    }
    
}
