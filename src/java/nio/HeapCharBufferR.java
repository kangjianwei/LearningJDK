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
 * A read-only HeapCharBuffer.  This class extends the corresponding
 * read/write class, overriding the mutation methods to throw a {@link
 * ReadOnlyBufferException} and overriding the view-buffer methods to return an
 * instance of this class rather than of the superclass.
 */

// 只读、非直接缓冲区，是HeapCharBuffer的只读版本，禁止写入操作，内部存储结构实现为char[]
class HeapCharBufferR extends HeapCharBuffer {
    // 寻找char[]类型数组中的元素时约定的起始偏移地址，与#arrayIndexScale配合使用
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(char[].class);
    // char[]类型数组中每个元素所占字节大小，这里是char[]，每个char占2个字节
    private static final long ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(char[].class);
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    protected HeapCharBufferR(char[] buf, int mark, int pos, int lim, int cap, int off) {
        super(buf, mark, pos, lim, cap, off);
        this.isReadOnly = true;
    }
    
    HeapCharBufferR(int cap, int lim) {            // package-private
        super(cap, lim);
        this.isReadOnly = true;
    }
    
    HeapCharBufferR(char[] buf, int off, int len) { // package-private
        super(buf, off, len);
        this.isReadOnly = true;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 只读/非直接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // HeapCharBufferR是直接缓冲区，返回true
    public boolean isReadOnly() {
        return true;
    }
    
    /*▲ 只读/非直接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    public CharBuffer slice() {
        return new HeapCharBufferR(hb, -1, 0, this.remaining(), this.remaining(), this.position() + offset);
    }
    
    // 副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public CharBuffer duplicate() {
        return new HeapCharBufferR(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }
    
    // 只读副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public CharBuffer asReadOnlyBuffer() {
        return duplicate();
    }
    
    // 副本，新缓冲区的【活跃区域】取自旧缓冲区【活跃区域】的[start，end)部分
    public CharBuffer subSequence(int start, int end) {
        if((start < 0) || (end > length()) || (start > end))
            throw new IndexOutOfBoundsException();
        int pos = position();
        return new HeapCharBufferR(hb, -1, pos + start, pos + end, capacity(), offset);
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 只读缓冲区，禁止写入操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public CharBuffer put(char x) {
        throw new ReadOnlyBufferException();
    }
    
    public CharBuffer put(int i, char x) {
        throw new ReadOnlyBufferException();
    }
    
    public CharBuffer put(char[] src, int offset, int length) {
        throw new ReadOnlyBufferException();
    }
    
    public CharBuffer put(CharBuffer src) {
        throw new ReadOnlyBufferException();
    }
    
    /*▲ 只读缓冲区，禁止写入操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 禁止压缩，因为禁止写入，压缩没意义 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public CharBuffer compact() {
        throw new ReadOnlyBufferException();
    }
    
    /*▲ 禁止压缩，因为禁止写入，压缩没意义 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
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
    
    
    
    // 构造新的子串。从start + offset起始，截取end - start个元素，转为String返回
    String toString(int start, int end) {               // package-private
        try {
            return new String(hb, start + offset, end - start);
        } catch(StringIndexOutOfBoundsException x) {
            throw new IndexOutOfBoundsException();
        }
    }
}
