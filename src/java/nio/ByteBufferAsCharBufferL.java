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

// ByteBuffer转为CharBuffer，使用可读写的缓冲区。采用小端字节序，其他部分与ByteBufferAsCharBufferB相同
class ByteBufferAsCharBufferL extends CharBuffer {
    
    protected final ByteBuffer bb;  // 待转换的ByteBuffer
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    ByteBufferAsCharBufferL(ByteBuffer bb) {   // package-private
        super(-1, 0, bb.remaining() >> 1, bb.remaining() >> 1);
        this.bb = bb;
        // enforce limit == capacity
        int cap = this.capacity();
        this.limit(cap);
        int pos = this.position();
        assert (pos <= cap);
        address = bb.address;
    }
    
    ByteBufferAsCharBufferL(ByteBuffer bb, int mark, int pos, int lim, int cap, long addr) {
        super(mark, pos, lim, cap);
        this.bb = bb;
        address = addr;
        assert address >= bb.address;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 可读写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 只读/可读写
    public boolean isReadOnly() {
        return false;
    }
    
    // 直接缓冲区/非直接缓冲区
    public boolean isDirect() {
        return bb.isDirect();
    }
    
    /*▲ 可读写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    public CharBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        long addr = byteOffset(pos);
        return new ByteBufferAsCharBufferL(bb, -1, 0, rem, rem, addr);
    }
    
    // 副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public CharBuffer duplicate() {
        return new ByteBufferAsCharBufferL(bb, this.markValue(), this.position(), this.limit(), this.capacity(), address);
    }
    
    // 只读副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public CharBuffer asReadOnlyBuffer() {
        return new ByteBufferAsCharBufferRL(bb, this.markValue(), this.position(), this.limit(), this.capacity(), address);
    }
    
    // 子副本，新缓冲区的【活跃区域】取自旧缓冲区【活跃区域】的[start，end)部分
    public CharBuffer subSequence(int start, int end) {
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        pos = (pos <= lim ? pos : lim);
        int len = lim - pos;
        
        if((start < 0) || (end > len) || (start > end))
            throw new IndexOutOfBoundsException();
        return new ByteBufferAsCharBufferL(bb, -1, pos + start, pos + end, capacity(), address);
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /* getCharUnaligned和putCharUnaligned方法中，最后一个参数为false，代表以小端法存取字节 */
    
    /*▼ get/读取 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 读取position处（可能需要加offset）的char，然后递增position。
    public char get() {
        char x = UNSAFE.getCharUnaligned(bb.hb, byteOffset(nextGetIndex()), false);
        return (x);
    }
    
    // 读取index处（可能需要加offset）的char（有越界检查）
    public char get(int index) {
        char x = UNSAFE.getCharUnaligned(bb.hb, byteOffset(checkIndex(index)), false);
        return (x);
    }
    
    // 返回index处的字符，不经过越界检查
    char getUnchecked(int index) {
        char x = UNSAFE.getCharUnaligned(bb.hb, byteOffset(index), false);
        return (x);
    }
    
    /*▲ get/读取 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ put/写入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向position处（可能需要加offset）写入char，并将position递增
    public CharBuffer put(char x) {
        char y = (x);
        UNSAFE.putCharUnaligned(bb.hb, byteOffset(nextPutIndex()), y, false);
        return this;
    }
    
    // 向index处（可能需要加offset）写入char
    public CharBuffer put(int index, char x) {
        char y = (x);
        UNSAFE.putCharUnaligned(bb.hb, byteOffset(checkIndex(index)), y, false);
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
        
        ByteBuffer db = bb.duplicate();
        db.limit(ix(lim));
        db.position(ix(0));
        
        ByteBuffer sb = db.slice();
        sb.position(pos << 1);
        sb.compact();
        
        position(rem);
        limit(capacity());
        discardMark();
        return this;
    }
    
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回该缓冲区的字节序（大端还是小端）
    public ByteOrder order() {
        return ByteOrder.LITTLE_ENDIAN;
    }
    
    // 返回‘char’的字节顺序（大端还是小端），在StringCharBuffer中换回null，其他缓冲区中由实现而定。
    ByteOrder charRegionOrder() {
        return order();
    }
    
    /*▲ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回内部存储结构的引用（一般用于非直接缓存区）
    @Override
    Object base() {
        return bb.hb;
    }
    
    protected long byteOffset(long i) {
        return address + (i << 1);
    }
    
    private int ix(int i) {
        int off = (int) (address - bb.address);
        return (i << 1) + off;
    }
    
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
}
