/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.ref.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.lang.ref.Reference;

// 可读写、直接缓冲区，采用与平台字节顺序不同的字节序，其他部分与DirectLongBufferU相同
class DirectLongBufferS extends LongBuffer implements DirectBuffer {
    
    // Cached unaligned-access capability
    protected static final boolean UNALIGNED = Bits.unaligned();
    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(long[].class);
    
    // Base address, used in all indexing calculations
    // NOTE: moved up to Buffer.java for speed in JNI GetDirectBufferAddress
    //    protected long address;
    // An object attached to this buffer. If this buffer is a view of another
    // buffer then we use this field to keep a reference to that buffer to
    // ensure that its memory isn't freed before we are done with it.
    private final Object att;
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // For duplicates and slices
    DirectLongBufferS(DirectBuffer db, int mark, int pos, int lim, int cap, int off) {
        super(mark, pos, lim, cap);
        address = db.address() + off;
        att = db;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 可读写/直接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 只读/可读写
    public boolean isReadOnly() {
        return false;
    }
    
    // 直接缓冲区/非直接缓冲区
    public boolean isDirect() {
        return true;
    }
    
    /*▲ 可读写/直接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    public LongBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        int off = (pos << 3);
        assert (off >= 0);
        return new DirectLongBufferS(this, -1, 0, rem, rem, off);
    }
    
    // 副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立
    public LongBuffer duplicate() {
        return new DirectLongBufferS(this, this.markValue(), this.position(), this.limit(), this.capacity(), 0);
    }
    
    // 只读副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立
    public LongBuffer asReadOnlyBuffer() {
        return new DirectLongBufferRS(this, this.markValue(), this.position(), this.limit(), this.capacity(), 0);
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get/读取 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 读取position处（可能需要加offset）的long，然后递增position。
    public long get() {
        try {
            return (Bits.swap(UNSAFE.getLong(ix(nextGetIndex()))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 读取i处（可能需要加offset）的long（有越界检查）
    public long get(int i) {
        try {
            return (Bits.swap(UNSAFE.getLong(ix(checkIndex(i)))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 复制源缓存区的length个元素到dst数组offset索引处
    public LongBuffer get(long[] dst, int offset, int length) {
        if(((long) length << 3) > Bits.JNI_COPY_TO_ARRAY_THRESHOLD) {
            checkBounds(offset, length, dst.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if(length > rem)
                throw new BufferUnderflowException();
            
            long dstOffset = ARRAY_BASE_OFFSET + ((long) offset << 3);
            try {
                if(order() != ByteOrder.nativeOrder())
                    UNSAFE.copySwapMemory(null, ix(pos), dst, dstOffset, (long) length << 3, (long) 1 << 3);
                else
                    UNSAFE.copyMemory(null, ix(pos), dst, dstOffset, (long) length << 3);
            } finally {
                Reference.reachabilityFence(this);
            }
            position(pos + length);
        } else {
            super.get(dst, offset, length);
        }
        return this;
    }
    
    /*▲ get/读取 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ put/写入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向position处（可能需要加offset）写入long，并将position递增
    public LongBuffer put(long x) {
        try {
            UNSAFE.putLong(ix(nextPutIndex()), Bits.swap((x)));
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    // 向i处（可能需要加offset）写入long
    public LongBuffer put(int i, long x) {
        try {
            UNSAFE.putLong(ix(checkIndex(i)), Bits.swap((x)));
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    // 将源缓冲区src的内容全部写入到当前缓冲区
    public LongBuffer put(LongBuffer src) {
        
        if(src instanceof DirectLongBufferS) {
            if(src == this)
                throw createSameBufferException();
            DirectLongBufferS sb = (DirectLongBufferS) src;
            
            int spos = sb.position();
            int slim = sb.limit();
            assert (spos <= slim);
            int srem = (spos <= slim ? slim - spos : 0);
            
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            
            if(srem > rem)
                throw new BufferOverflowException();
            try {
                UNSAFE.copyMemory(sb.ix(spos), ix(pos), (long) srem << 3);
            } finally {
                Reference.reachabilityFence(sb);
                Reference.reachabilityFence(this);
            }
            sb.position(spos + srem);
            position(pos + srem);
        } else if(src.hb != null) {
            int spos = src.position();
            int slim = src.limit();
            assert (spos <= slim);
            int srem = (spos <= slim ? slim - spos : 0);
            
            put(src.hb, src.offset + spos, srem);
            src.position(spos + srem);
            
        } else {
            super.put(src);
        }
        return this;
    }
    
    // 从源long数组src的offset处开始，复制length个元素，写入到当前缓冲区
    public LongBuffer put(long[] src, int offset, int length) {
        if(((long) length << 3) > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD) {
            checkBounds(offset, length, src.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if(length > rem)
                throw new BufferOverflowException();
            
            long srcOffset = ARRAY_BASE_OFFSET + ((long) offset << 3);
            try {
                if(order() != ByteOrder.nativeOrder())
                    UNSAFE.copySwapMemory(src, srcOffset, null, ix(pos), (long) length << 3, (long) 1 << 3);
                else
                    UNSAFE.copyMemory(src, srcOffset, null, ix(pos), (long) length << 3);
            } finally {
                Reference.reachabilityFence(this);
            }
            position(pos + length);
        } else {
            super.put(src, offset, length);
        }
        return this;
    }
    
    /*▲ put/写入 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 压缩缓冲区，将当前未读完的数据挪到容器起始处，可用于读模式到写模式的切换，但又不丢失之前读入的数据
    public LongBuffer compact() {
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        try {
            UNSAFE.copyMemory(ix(pos), ix(0), (long) rem << 3);
        } finally {
            Reference.reachabilityFence(this);
        }
        position(rem);
        limit(capacity());
        discardMark();
        return this;
    }
    
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回该缓冲区的字节序（大端还是小端）
    public ByteOrder order() {
        return ((ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }
    
    /*▲ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回内部存储结构的引用（一般用于非直接缓存区）
    @Override
    Object base() {
        return null;
    }
    
    private long ix(int i) {
        return address + ((long) i << 3);
    }
    
    
    
    /*▼ 实现DirectBuffer接口 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回直接缓冲区的【绝对】起始<地址>
    public long address() {
        return address;
    }
    
    // 返回附件，一般是指母体缓冲区的引用
    public Object attachment() {
        return att;
    }
    
    // 返回该缓冲区的清理器
    public Cleaner cleaner() {
        return null;
    }
    
    /*▲ 实现DirectBuffer接口 ████████████████████████████████████████████████████████████████████████████████┛ */
}
