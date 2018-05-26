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

// 可读写、直接缓冲区，采用与平台字节顺序相同的字节序，其他部分与DirectCharBufferS相同
class DirectCharBufferU extends CharBuffer implements DirectBuffer {
    // Cached unaligned-access capability
    protected static final boolean UNALIGNED = Bits.unaligned();
    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(char[].class);
    
    // Base address, used in all indexing calculations
    // NOTE: moved up to Buffer.java for speed in JNI GetDirectBufferAddress protected long address;
    // An object attached to this buffer.
    // If this buffer is a view of another buffer then we use this field to keep a reference to that buffer to ensure that its memory isn't freed before we are done with it.
    private final Object att;   // 指向母体缓冲区
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 用于#duplicates和#slices
    DirectCharBufferU(DirectBuffer db, int mark, int pos, int lim, int cap, int off) {
        super(mark, pos, lim, cap);
        address = db.address() + off;   // 新缓冲区【绝对】起始地址 = 旧缓冲区的【绝对】起始地址+新缓冲区相对于旧缓冲区的位移
        att = db;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 可读写/直接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public boolean isReadOnly() {
        return false;
    }
    
    public boolean isDirect() {
        return true;
    }
    
    /*▲ 可读写/直接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立。
    public CharBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        int off = (pos << 1);   // 每个char是两个字节，所以这里要乘以2
        assert (off >= 0);
        return new DirectCharBufferU(this, -1, 0, rem, rem, off);
    }
    
    // 副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public CharBuffer duplicate() {
        return new DirectCharBufferU(this, this.markValue(), this.position(), this.limit(), this.capacity(), 0);
    }
    
    // 只读副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public CharBuffer asReadOnlyBuffer() {
        return new DirectCharBufferRU(this, this.markValue(), this.position(), this.limit(), this.capacity(), 0);
    }
    
    // 副本，新缓冲区的【活跃区域】取自旧缓冲区【活跃区域】的[start，end)部分
    public CharBuffer subSequence(int start, int end) {
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        pos = (pos <= lim ? pos : lim);
        int len = lim - pos;
        
        if((start < 0) || (end > len) || (start > end))
            throw new IndexOutOfBoundsException();
        
        return new DirectCharBufferU(this, -1, pos + start, pos + end, capacity(), offset);
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get/读取 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 读取position处的char，然后递增position。
    public char get() {
        try {
            // 直接从本地内存中访问
            return (UNSAFE.getChar(ix(nextGetIndex())));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 读取i处的char（有越界检查）
    public char get(int i) {
        try {
            return ((UNSAFE.getChar(ix(checkIndex(i)))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 读取i处的char（无越界检查）
    char getUnchecked(int i) {
        try {
            return ((UNSAFE.getChar(ix(i))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 复制当前缓存区的length个元素到dst数组offset索引处
    public CharBuffer get(char[] dst, int offset, int length) {
        // 复制的元素数量超过某个性能阙值才执行此段
        if(((long) length << 1) > Bits.JNI_COPY_TO_ARRAY_THRESHOLD) {
            checkBounds(offset, length, dst.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if(length > rem)
                throw new BufferUnderflowException();
            
            long dstOffset = ARRAY_BASE_OFFSET + ((long) offset << 1);
            try {
                // null参数代表直接从内存地址拷贝
                if(order() != ByteOrder.nativeOrder()) {    // 缓冲区与本地字节序不一致，拷贝字节时涉及到大小端转换
                    UNSAFE.copySwapMemory(null, ix(pos), dst, dstOffset, (long) length << 1, (long) 1 << 1);
                } else {    // 缓冲区与本地字节序一致，直接拷贝
                    UNSAFE.copyMemory(null, ix(pos), dst, dstOffset, (long) length << 1);
                }
            } finally {
                Reference.reachabilityFence(this);
            }
            // 更新当前缓冲区的position
            position(pos + length);
        } else {
            // 不值当用上面那一堆方法
            super.get(dst, offset, length);
        }
        return this;
    }
    
    /*▲ get/读取 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ put/写入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向position处写入char，并将position递增
    public CharBuffer put(char x) {
        try {
            UNSAFE.putChar(ix(nextPutIndex()), x);
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    // 向i处写入char
    public CharBuffer put(int i, char x) {
        try {
            UNSAFE.putChar(ix(checkIndex(i)), ((x)));
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    // 将源缓冲区src的内容全部写入到当前缓冲区
    public CharBuffer put(CharBuffer src) {
        if(src instanceof DirectCharBufferU) {
            if(src == this)
                throw createSameBufferException();
            DirectCharBufferU sb = (DirectCharBufferU) src;
            
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
                UNSAFE.copyMemory(sb.ix(spos), ix(pos), (long) srem << 1);
            } finally {
                Reference.reachabilityFence(sb);
                Reference.reachabilityFence(this);
            }
            sb.position(spos + srem);
            position(pos + srem);
        } else if(src.hb != null) { // 不同类型的缓冲区
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
    
    // 从源字符数组src的offset处开始，复制length个元素，写入到当前缓冲区【活跃区域】内（考虑偏移量）
    public CharBuffer put(char[] src, int offset, int length) {
        if(((long) length << 1) > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD) {
            checkBounds(offset, length, src.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if(length > rem)
                throw new BufferOverflowException();
            
            long srcOffset = ARRAY_BASE_OFFSET + ((long) offset << 1);
            try {
                if(order() != ByteOrder.nativeOrder()) {
                    UNSAFE.copySwapMemory(src, srcOffset, null, ix(pos), (long) length << 1, (long) 1 << 1);
                } else {
                    UNSAFE.copyMemory(src, srcOffset, null, ix(pos), (long) length << 1);
                }
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
    
    // 压缩缓冲区，将当前未读完的数据挪到容器起始处，可用于读模式到写模式的切换，但又不丢失之前读入的数据。
    public CharBuffer compact() {
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        try {
            UNSAFE.copyMemory(ix(pos), ix(0), (long) rem << 1);
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
    
    // 返回与平台字节顺序相同的字节序
    public ByteOrder order() {
        return ((ByteOrder.nativeOrder() != ByteOrder.BIG_ENDIAN) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }
    
    // 返回‘char’的字节序（此类中与平台字节序相同）
    ByteOrder charRegionOrder() {
        return order();
    }
    
    /*▲ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 返回内部存储结构的引用（一般用于非直接缓存区）
    @Override
    Object base() {
        return null;
    }
    
    // 返回索引i处的元素的<地址>
    private long ix(int i) {
        // 转换为本地内存地址
        return address + ((long) i << 1);
    }
    
    // 构造新的子串
    public String toString(int start, int end) {
        if((end > limit()) || (start > end))
            throw new IndexOutOfBoundsException();
        try {
            int len = end - start;
            char[] ca = new char[len];
            CharBuffer cb = CharBuffer.wrap(ca);
            CharBuffer db = this.duplicate();
            db.position(start);
            db.limit(end);
            cb.put(db);
            return new String(ca);
        } catch(StringIndexOutOfBoundsException x) {
            throw new IndexOutOfBoundsException();
        }
    }
    
    
    
    /*▼ 实现DirectBuffer接口 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回直接缓冲区的起始<地址>
    public long address() {
        return address;
    }
    
    // 返回指向母体缓冲区的引用
    public Object attachment() {
        return att;
    }
    
    public Cleaner cleaner() {
        return null;
    }
    
    /*▲ 实现DirectBuffer接口 ████████████████████████████████████████████████████████████████████████████████┛ */
}
