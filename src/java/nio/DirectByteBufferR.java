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

import sun.nio.ch.DirectBuffer;

import java.io.FileDescriptor;

// 只读、直接缓冲区，内部存储结构为本地内存块，是DirectByteBuffer的只读版本
class DirectByteBufferR extends DirectByteBuffer implements DirectBuffer {
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // For memory-mapped buffers -- invoked by FileChannelImpl via reflection
    protected DirectByteBufferR(int cap, long addr, FileDescriptor fd, Runnable unmapper) {
        super(cap, addr, fd, unmapper);
        this.isReadOnly = true;
    }
    
    // Primary constructor
    DirectByteBufferR(int cap) {                   // package-private
        super(cap);
        this.isReadOnly = true;
    }
    
    // For duplicates and slices
    DirectByteBufferR(DirectBuffer db, int mark, int pos, int lim, int cap, int off) {
        super(db, mark, pos, lim, cap, off);
        this.isReadOnly = true;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 只读/直接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public boolean isDirect() {
        return true;
    }
    
    public boolean isReadOnly() {
        return true;
    }
    
    /*▲ 只读/直接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public ByteBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        int off = (pos << 0);
        assert (off >= 0);
        return new DirectByteBufferR(this, -1, 0, rem, rem, off);
    }
    
    public ByteBuffer slice(int pos, int lim) {
        assert (pos >= 0);
        assert (pos <= lim);
        int rem = lim - pos;
        return new DirectByteBufferR(this, -1, 0, rem, rem, pos);
    }
    
    public ByteBuffer duplicate() {
        return new DirectByteBufferR(this, this.markValue(), this.position(), this.limit(), this.capacity(), 0);
    }
    
    public ByteBuffer asReadOnlyBuffer() {
        return duplicate();
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 只读缓冲区，禁止写入操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public ByteBuffer put(byte x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer put(int i, byte x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer put(ByteBuffer src) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer put(byte[] src, int offset, int length) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putChar(char x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putChar(int i, char x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putShort(short x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putShort(int i, short x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putInt(int x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putInt(int i, int x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putLong(long x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putLong(int i, long x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putFloat(float x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putFloat(int i, float x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putDouble(double x) {
        throw new ReadOnlyBufferException();
    }
    
    public ByteBuffer putDouble(int i, double x) {
        throw new ReadOnlyBufferException();
    }
    
    private ByteBuffer putChar(long a, char x) {
        throw new ReadOnlyBufferException();
    }
    
    private ByteBuffer putShort(long a, short x) {
        throw new ReadOnlyBufferException();
    }
    
    private ByteBuffer putInt(long a, int x) {
        throw new ReadOnlyBufferException();
    }
    
    private ByteBuffer putLong(long a, long x) {
        throw new ReadOnlyBufferException();
    }
    
    private ByteBuffer putFloat(long a, float x) {
        throw new ReadOnlyBufferException();
    }
    
    private ByteBuffer putDouble(long a, double x) {
        throw new ReadOnlyBufferException();
    }
    
    /*▲ 只读缓冲区，禁止写入操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ asXXXBuffer ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public CharBuffer asCharBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 1;
        if(!UNALIGNED && ((address + off) % (1 << 1) != 0)) {
            return (bigEndian
                ? (CharBuffer) (new ByteBufferAsCharBufferRB(this, -1, 0, size, size, address + off))
                : (CharBuffer) (new ByteBufferAsCharBufferRL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder
                ? (CharBuffer) (new DirectCharBufferRU(this, -1, 0, size, size, off))
                : (CharBuffer) (new DirectCharBufferRS(this, -1, 0, size, size, off)));
        }
    }
    
    public ShortBuffer asShortBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 1;
        if(!UNALIGNED && ((address + off) % (1 << 1) != 0)) {
            return (bigEndian
                ? (ShortBuffer) (new ByteBufferAsShortBufferRB(this, -1, 0, size, size, address + off))
                : (ShortBuffer) (new ByteBufferAsShortBufferRL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder
                ? (ShortBuffer) (new DirectShortBufferRU(this, -1, 0, size, size, off))
                : (ShortBuffer) (new DirectShortBufferRS(this, -1, 0, size, size, off)));
        }
    }
    
    public IntBuffer asIntBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 2;
        if(!UNALIGNED && ((address + off) % (1 << 2) != 0)) {
            return (bigEndian
                ? (IntBuffer) (new ByteBufferAsIntBufferRB(this, -1, 0, size, size, address + off))
                : (IntBuffer) (new ByteBufferAsIntBufferRL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder
                ? (IntBuffer) (new DirectIntBufferRU(this, -1, 0, size, size, off))
                : (IntBuffer) (new DirectIntBufferRS(this, -1, 0, size, size, off)));
        }
    }
    
    public LongBuffer asLongBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 3;
        if(!UNALIGNED && ((address + off) % (1 << 3) != 0)) {
            return (bigEndian
                ? (LongBuffer) (new ByteBufferAsLongBufferRB(this, -1, 0, size, size, address + off))
                : (LongBuffer) (new ByteBufferAsLongBufferRL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder
                ? (LongBuffer) (new DirectLongBufferRU(this, -1, 0, size, size, off))
                : (LongBuffer) (new DirectLongBufferRS(this, -1, 0, size, size, off)));
        }
    }
    
    public FloatBuffer asFloatBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 2;
        if(!UNALIGNED && ((address + off) % (1 << 2) != 0)) {
            return (bigEndian
                ? (FloatBuffer) (new ByteBufferAsFloatBufferRB(this, -1, 0, size, size, address + off))
                : (FloatBuffer) (new ByteBufferAsFloatBufferRL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder
                ? (FloatBuffer) (new DirectFloatBufferRU(this, -1, 0, size, size, off))
                : (FloatBuffer) (new DirectFloatBufferRS(this, -1, 0, size, size, off)));
        }
    }
    
    public DoubleBuffer asDoubleBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 3;
        if(!UNALIGNED && ((address + off) % (1 << 3) != 0)) {
            return (bigEndian
                ? (DoubleBuffer) (new ByteBufferAsDoubleBufferRB(this, -1, 0, size, size, address + off))
                : (DoubleBuffer) (new ByteBufferAsDoubleBufferRL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder
                ? (DoubleBuffer) (new DirectDoubleBufferRU(this, -1, 0, size, size, off))
                : (DoubleBuffer) (new DirectDoubleBufferRS(this, -1, 0, size, size, off)));
        }
    }
    
    /*▲ asXXXBuffer ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 禁止压缩，因为禁止写入，压缩没意义 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public ByteBuffer compact() {
        throw new ReadOnlyBufferException();
    }
    
    /*▲ 禁止压缩，因为禁止写入，压缩没意义 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    @Override
    Object base() {
        return null;
    }
}
