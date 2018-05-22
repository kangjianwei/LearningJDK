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

/**
 * A read/write HeapCharBuffer.
 */

// 可读写、非直接缓冲区，内部存储结构实现为char[]
class HeapCharBuffer extends CharBuffer {
    // 寻找char[]类型数组中的元素时约定的起始偏移地址，与#arrayIndexScale配合使用
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(char[].class);
    // char[]类型数组中每个元素所占字节大小，这里是char[]，每个char占2个字节
    private static final long ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(char[].class);
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 最后一个参数用于确定当前缓冲区在内部存储器上的【绝对】起始地址
    protected HeapCharBuffer(char[] buf, int mark, int pos, int lim, int cap, int off) {
        super(mark, pos, lim, cap, buf, off);
        this.address = ARRAY_BASE_OFFSET + off * ARRAY_INDEX_SCALE;
    }
    
    HeapCharBuffer(int cap, int lim) {// package-private
        super(-1, 0, lim, cap, new char[cap], 0);
        this.address = ARRAY_BASE_OFFSET;
    }
    
    HeapCharBuffer(char[] buf, int off, int len) {// package-private
        super(-1, off, off + len, buf.length, buf, 0);
        this.address = ARRAY_BASE_OFFSET;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 可读写/非直接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // HeapCharBuffer是可读写缓冲区，返回false
    public boolean isReadOnly() {
        return false;
    }
    
    // HeapCharBuffer是非直接缓冲区，返回false
    public boolean isDirect() {
        return false;
    }
    
    /*▲ 可读写/非直接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    public CharBuffer slice() {
        return new HeapCharBuffer(hb, -1, 0, this.remaining(), this.remaining(), this.position() + offset);
    }
    
    // 副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public CharBuffer duplicate() {
        return new HeapCharBuffer(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }
    
    // 只读副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public CharBuffer asReadOnlyBuffer() {
        // 【只读】属性由子类实现
        return new HeapCharBufferR(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }
    
    // 副本，新缓冲区的【活跃区域】取自旧缓冲区【活跃区域】的[start，end)部分
    public CharBuffer subSequence(int start, int end) {
        if((start < 0) || (end > length()) || (start > end))
            throw new IndexOutOfBoundsException();
        int pos = position();
        return new HeapCharBuffer(hb, -1, pos + start, pos + end, capacity(), offset);
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get/读取 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 读取position+offset处的char，然后递增position。
    public char get() {
        return hb[ix(nextGetIndex())];
    }
    
    // 读取i+offset处的char（有越界检查）
    public char get(int i) {
        return hb[ix(checkIndex(i))];
    }
    
    // 读取i+offset处的char（无越界检查）
    char getUnchecked(int i) {
        return hb[ix(i)];
    }
    
    // 复制当前缓存区的length个元素到dst数组offset索引处
    public CharBuffer get(char[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        if(length > remaining())
            throw new BufferUnderflowException();
        System.arraycopy(hb, ix(position()), dst, offset, length);
        // 更新当前缓冲区的position
        position(position() + length);
        return this;
    }
    
    /*▲ get/读取 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ put/写入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向position+offset处写入char，并将position递增
    public CharBuffer put(char x) {
        hb[ix(nextPutIndex())] = x;
        return this;
    }
    
    // 向i+offset处写入char
    public CharBuffer put(int i, char x) {
        hb[ix(checkIndex(i))] = x;
        return this;
    }
    
    // 从源字符数组src的offset处开始，复制length个元素，写入到当前缓冲区【活跃区域】内（考虑偏移量）
    public CharBuffer put(char[] src, int offset, int length) {
        checkBounds(offset, length, src.length);
        if(length > remaining())
            throw new BufferOverflowException();
        System.arraycopy(src, offset, hb, ix(position()), length);
        position(position() + length);
        return this;
    }
    
    // 将源缓冲区src的内容全部写入到当前缓冲区
    public CharBuffer put(CharBuffer src) {
        if(src instanceof HeapCharBuffer) { // 从源缓冲区【有效区域】复制所有内容，写入到当前缓冲区【有效区域】，两个区域的position递增n
            if(src == this)
                throw createSameBufferException();
            HeapCharBuffer sb = (HeapCharBuffer) src;
            int n = sb.remaining();
            if(n > remaining())
                throw new BufferOverflowException();
            System.arraycopy(sb.hb, sb.ix(sb.position()), hb, ix(position()), n);
            sb.position(sb.position() + n);
            position(position() + n);
        } else if(src.isDirect()) { // 如果src是直接缓冲区，则向当前缓冲区写入内容后，只改变当前缓冲区的position
            int n = src.remaining();
            if(n > remaining())
                throw new BufferOverflowException();
            src.get(hb, ix(position()), n);
            position(position() + n);
        } else {
            super.put(src);
        }
        return this;
    }
    
    /*▲ put/写入 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 压缩缓冲区，将当前未读完的数据挪到容器起始处，可用于读模式到写模式的切换，但又不丢失之前读入的数据。
    public CharBuffer compact() {
        System.arraycopy(hb, ix(position()), hb, ix(0), remaining());
        position(remaining());
        limit(capacity());
        discardMark();
        return this;
    }
    
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回与平台字节顺序相同的字节序
    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }
    
    // 返回‘char’的字节序（此类中与平台字节序相同）
    ByteOrder charRegionOrder() {
        return order();
    }
    
    /*▲ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 返回索引i处的元素的<地址>
    protected int ix(int i) {
        return i + offset;
    }
    
    // 构造新的子串。从start + offset起始，截取end - start个元素，转为String返回
    String toString(int start, int end) {               // package-private
        try {
            return new String(hb, start + offset, end - start);
        } catch(StringIndexOutOfBoundsException x) {
            throw new IndexOutOfBoundsException();
        }
    }
}
