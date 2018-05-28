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

// ByteBuffer转为CharBuffer，使用可读写的缓冲区。采用大端字节序，其他部分与ByteBufferAsCharBufferL相同
class ByteBufferAsCharBufferB extends CharBuffer {
    
    protected final ByteBuffer bb;  // 待转换的ByteBuffer
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    ByteBufferAsCharBufferB(ByteBuffer bb) {
        // 从byte到char转换，容量要减半
        super(-1, 0, bb.remaining() >> 1, bb.remaining() >> 1);
        this.bb = bb;
        // enforce limit == capacity
        int cap = this.capacity();
        this.limit(cap);
        int pos = this.position();
        assert (pos <= cap);
        address = bb.address;
    }
    
    ByteBufferAsCharBufferB(ByteBuffer bb, int mark, int pos, int lim, int cap, long addr) {
        super(mark, pos, lim, cap);
        this.bb = bb;
        address = addr;
        assert address >= bb.address;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 可读写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public boolean isReadOnly() {
        return false;
    }
    
    // 是直接缓冲区还是非直接缓冲区，取决于传入的ByteBuffer
    public boolean isDirect() {
        return bb.isDirect();
    }
    
    /*▲ 可读写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public CharBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        long addr = byteOffset(pos);
        return new ByteBufferAsCharBufferB(bb, -1, 0, rem, rem, addr);
    }
    
    public CharBuffer duplicate() {
        return new ByteBufferAsCharBufferB(bb, this.markValue(), this.position(), this.limit(), this.capacity(), address);
    }
    
    public CharBuffer asReadOnlyBuffer() {
        return new ByteBufferAsCharBufferRB(bb, this.markValue(), this.position(), this.limit(), this.capacity(), address);
    }
    
    public CharBuffer subSequence(int start, int end) {
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        pos = (pos <= lim ? pos : lim);
        int len = lim - pos;
        
        if((start < 0) || (end > len) || (start > end))
            throw new IndexOutOfBoundsException();
        return new ByteBufferAsCharBufferB(bb, -1, pos + start, pos + end, capacity(), address);
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /* getCharUnaligned和putCharUnaligned方法中，最后一个参数为true，代表以大端法存取字节 */
    
    /*▼ get/读取 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public char get() {
        char x = UNSAFE.getCharUnaligned(bb.hb, byteOffset(nextGetIndex()), true);
        return (x);
    }
    
    public char get(int i) {
        char x = UNSAFE.getCharUnaligned(bb.hb, byteOffset(checkIndex(i)), true);
        return (x);
    }
    
    char getUnchecked(int i) {
        char x = UNSAFE.getCharUnaligned(bb.hb, byteOffset(i), true);
        return (x);
    }
    
    /*▲ get/读取 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ put/写入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public CharBuffer put(char x) {
        char y = (x);
        UNSAFE.putCharUnaligned(bb.hb, byteOffset(nextPutIndex()), y, true);
        return this;
    }
    
    public CharBuffer put(int i, char x) {
        char y = (x);
        UNSAFE.putCharUnaligned(bb.hb, byteOffset(checkIndex(i)), y, true);
        return this;
    }
    
    /*▲ put/写入 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    
    public ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }
    
    ByteOrder charRegionOrder() {
        return order();
    }
    
    /*▲ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    // 返回的是原ByteBuffer内部的存储结构
    @Override
    Object base() {
        return bb.hb;
    }
    
    // 获取当前缓冲区索引i处的元素的<地址>
    protected long byteOffset(long i) {
        // 一个char是2个字节，这里将i乘以2
        return address + (i << 1);
    }
    
    // 将CharBuffer的索引i转换为ByteBuffer中的<地址>后返回
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
