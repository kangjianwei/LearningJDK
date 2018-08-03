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
 * A read/write HeapByteBuffer.
 */

// 可读写、非直接缓冲区，内部存储结构实现为byte[]
class HeapByteBuffer extends ByteBuffer {
    
    // 寻找byte[]类型数组中的元素时约定的起始偏移地址，与#arrayIndexScale配合使用
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    // byte[]类型数组中每个元素所占字节大小，这里是byte[]，每个byte占1个字节
    private static final long ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(byte[].class);
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 最后一个参数用于确定当前缓冲区在内部存储器上的【绝对】起始地址
    protected HeapByteBuffer(byte[] buf, int mark, int pos, int lim, int cap, int off) {
        super(mark, pos, lim, cap, buf, off);
        this.address = ARRAY_BASE_OFFSET + off * ARRAY_INDEX_SCALE;
    }
    
    HeapByteBuffer(int cap, int lim) {
        super(-1, 0, lim, cap, new byte[cap], 0);
        this.address = ARRAY_BASE_OFFSET;
    }
    
    HeapByteBuffer(byte[] buf, int off, int len) {
        super(-1, off, off + len, buf.length, buf, 0);
        this.address = ARRAY_BASE_OFFSET;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 可读写/非直接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public boolean isReadOnly() {
        return false;
    }
    
    public boolean isDirect() {
        return false;
    }
    
    /*▲ 可读写/非直接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public ByteBuffer slice() {
        return new HeapByteBuffer(hb, -1, 0, this.remaining(), this.remaining(), this.position() + offset);
    }
    
    ByteBuffer slice(int pos, int lim) {
        assert (pos >= 0);
        assert (pos <= lim);
        int rem = lim - pos;
        return new HeapByteBuffer(hb, -1, 0, rem, rem, pos + offset);
    }
    
    public ByteBuffer duplicate() {
        return new HeapByteBuffer(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }
    
    public ByteBuffer asReadOnlyBuffer() {
        return new HeapByteBufferR(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get/读取 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 读取position+offset处的byte，然后递增position。
    public byte get() {
        return hb[ix(nextGetIndex())];
    }
    
    // 读取i+offset处的byte（有越界检查）
    public byte get(int i) {
        return hb[ix(checkIndex(i))];
    }
    
    // 复制当前缓存区的length个元素到dst数组offset索引处
    public ByteBuffer get(byte[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        if(length > remaining())
            throw new BufferUnderflowException();
        System.arraycopy(hb, ix(position()), dst, offset, length);
        // 更新当前缓冲区的position
        position(position() + length);
        return this;
    }
    
    // 一次读2个字节，按char解析，将position增加2个单位
    public char getChar() {
        return UNSAFE.getCharUnaligned(hb, byteOffset(nextGetIndex(2)), bigEndian);
    }
    
    // 读取i处2个字节解析为char（有越界检查）
    public char getChar(int i) {
        return UNSAFE.getCharUnaligned(hb, byteOffset(checkIndex(i, 2)), bigEndian);
    }
    
    // 一次读2个字节，按short解析，将position增加2个单位
    public short getShort() {
        return UNSAFE.getShortUnaligned(hb, byteOffset(nextGetIndex(2)), bigEndian);
    }
    
    // 读取i处2个字节解析为short（有越界检查）
    public short getShort(int i) {
        return UNSAFE.getShortUnaligned(hb, byteOffset(checkIndex(i, 2)), bigEndian);
    }
    
    // 一次读4个字节，按int解析，将position增加4个单位
    public int getInt() {
        return UNSAFE.getIntUnaligned(hb, byteOffset(nextGetIndex(4)), bigEndian);
    }
    
    // 读取i处4个字节解析为int（有越界检查）
    public int getInt(int i) {
        return UNSAFE.getIntUnaligned(hb, byteOffset(checkIndex(i, 4)), bigEndian);
    }
    
    // 一次读8个字节，按long解析，将position增加8个单位
    public long getLong() {
        return UNSAFE.getLongUnaligned(hb, byteOffset(nextGetIndex(8)), bigEndian);
    }
    
    // 读取i处8个字节解析为long（有越界检查）
    public long getLong(int i) {
        return UNSAFE.getLongUnaligned(hb, byteOffset(checkIndex(i, 8)), bigEndian);
    }
    
    // 一次读4个字节，按float解析，将position增加4个单位
    public float getFloat() {
        int x = UNSAFE.getIntUnaligned(hb, byteOffset(nextGetIndex(4)), bigEndian);
        return Float.intBitsToFloat(x);
    }
    
    // 读取i处4个字节解析为float（有越界检查）
    public float getFloat(int i) {
        int x = UNSAFE.getIntUnaligned(hb, byteOffset(checkIndex(i, 4)), bigEndian);
        return Float.intBitsToFloat(x);
    }
    
    // 一次读8个字节，按double解析，将position增加8个单位
    public double getDouble() {
        long x = UNSAFE.getLongUnaligned(hb, byteOffset(nextGetIndex(8)), bigEndian);
        return Double.longBitsToDouble(x);
    }
    
    // 读取i处8个字节解析为double（有越界检查）
    public double getDouble(int i) {
        long x = UNSAFE.getLongUnaligned(hb, byteOffset(checkIndex(i, 8)), bigEndian);
        return Double.longBitsToDouble(x);
    }
    
    /*▲ get/读取 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ put/写入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向position+offset处写入byte，并将position递增
    public ByteBuffer put(byte x) {
        hb[ix(nextPutIndex())] = x;
        return this;
    }
    
    // 向i+offset处写入byte
    public ByteBuffer put(int i, byte x) {
        hb[ix(checkIndex(i))] = x;
        return this;
    }
    
    // 从源字节数组src的offset处开始，复制length个元素，写入到当前缓冲区【活跃区域】内（考虑偏移量）
    public ByteBuffer put(byte[] src, int offset, int length) {
        checkBounds(offset, length, src.length);
        if(length > remaining())
            throw new BufferOverflowException();
        System.arraycopy(src, offset, hb, ix(position()), length);
        position(position() + length);
        return this;
    }
    
    // 将源缓冲区src的内容全部写入到当前缓冲区
    public ByteBuffer put(ByteBuffer src) {
        if(src instanceof HeapByteBuffer) {
            if(src == this)
                throw createSameBufferException();
            HeapByteBuffer sb = (HeapByteBuffer) src;
            int n = sb.remaining();
            if(n > remaining())
                throw new BufferOverflowException();
            System.arraycopy(sb.hb, sb.ix(sb.position()), hb, ix(position()), n);
            sb.position(sb.position() + n);
            position(position() + n);
        } else if(src.isDirect()) {
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
    
    // 将char转为byte存入缓冲区，将position增加2个单位
    public ByteBuffer putChar(char x) {
        UNSAFE.putCharUnaligned(hb, byteOffset(nextPutIndex(2)), x, bigEndian);
        return this;
    }
    
    // 将char转为byte存入缓冲区索引i处
    public ByteBuffer putChar(int i, char x) {
        UNSAFE.putCharUnaligned(hb, byteOffset(checkIndex(i, 2)), x, bigEndian);
        return this;
    }
    
    // 将short转为byte存入缓冲区，将position增加2个单位
    public ByteBuffer putShort(short x) {
        UNSAFE.putShortUnaligned(hb, byteOffset(nextPutIndex(2)), x, bigEndian);
        return this;
    }
    
    // 将short转为byte存入缓冲区索引i处
    public ByteBuffer putShort(int i, short x) {
        UNSAFE.putShortUnaligned(hb, byteOffset(checkIndex(i, 2)), x, bigEndian);
        return this;
    }
    
    // 将int转为byte存入缓冲区，将position增加4个单位
    public ByteBuffer putInt(int x) {
        UNSAFE.putIntUnaligned(hb, byteOffset(nextPutIndex(4)), x, bigEndian);
        return this;
    }
    
    // 将int转为byte存入缓冲区索引i处
    public ByteBuffer putInt(int i, int x) {
        UNSAFE.putIntUnaligned(hb, byteOffset(checkIndex(i, 4)), x, bigEndian);
        return this;
    }
    
    // 将long转为byte存入缓冲区，将position增加8个单位
    public ByteBuffer putLong(long x) {
        UNSAFE.putLongUnaligned(hb, byteOffset(nextPutIndex(8)), x, bigEndian);
        return this;
    }
    
    // 将long转为byte存入缓冲区索引i处
    public ByteBuffer putLong(int i, long x) {
        UNSAFE.putLongUnaligned(hb, byteOffset(checkIndex(i, 8)), x, bigEndian);
        return this;
    }
    
    // 将float转为byte存入缓冲区，将position增加4个单位
    public ByteBuffer putFloat(float x) {
        int y = Float.floatToRawIntBits(x);
        UNSAFE.putIntUnaligned(hb, byteOffset(nextPutIndex(4)), y, bigEndian);
        return this;
    }
    
    // 将float转为byte存入缓冲区索引i处
    public ByteBuffer putFloat(int i, float x) {
        int y = Float.floatToRawIntBits(x);
        UNSAFE.putIntUnaligned(hb, byteOffset(checkIndex(i, 4)), y, bigEndian);
        return this;
    }
    
    // 将double转为byte存入缓冲区，将position增加8个单位
    public ByteBuffer putDouble(double x) {
        long y = Double.doubleToRawLongBits(x);
        UNSAFE.putLongUnaligned(hb, byteOffset(nextPutIndex(8)), y, bigEndian);
        return this;
    }
    
    // 将double转为byte存入缓冲区索引i处
    public ByteBuffer putDouble(int i, double x) {
        long y = Double.doubleToRawLongBits(x);
        UNSAFE.putLongUnaligned(hb, byteOffset(checkIndex(i, 8)), y, bigEndian);
        return this;
    }
    
    /*▲ put/写入 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 压缩缓冲区，将当前未读完的数据挪到容器起始处，可用于读模式到写模式的切换，但又不丢失之前读入的数据。
    public ByteBuffer compact() {
        System.arraycopy(hb, ix(position()), hb, ix(0), remaining());
        position(remaining());
        limit(capacity());
        discardMark();
        return this;
    }
    
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ asXXXBuffer ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // ByteBuffer转为CharBuffer
    public CharBuffer asCharBuffer() {
        int size = this.remaining() >> 1;
        long addr = address + position();
        return (bigEndian
            ? new ByteBufferAsCharBufferB(this, -1, 0, size, size, addr)
            : new ByteBufferAsCharBufferL(this, -1, 0, size, size, addr));
    }
    
    // ByteBuffer转为ShortBuffer
    public ShortBuffer asShortBuffer() {
        int size = this.remaining() >> 1;
        long addr = address + position();
        return (bigEndian
            ? new ByteBufferAsShortBufferB(this, -1, 0, size, size, addr)
            : new ByteBufferAsShortBufferL(this, -1, 0, size, size, addr));
    }
    
    // ByteBuffer转为IntBuffer
    public IntBuffer asIntBuffer() {
        int size = this.remaining() >> 2;
        long addr = address + position();
        return (bigEndian
            ? new ByteBufferAsIntBufferB(this, -1, 0, size, size, addr)
            : new ByteBufferAsIntBufferL(this, -1, 0, size, size, addr));
    }
    
    // ByteBuffer转为LongBuffer
    public LongBuffer asLongBuffer() {
        int size = this.remaining() >> 3;
        long addr = address + position();
        return (bigEndian
            ? new ByteBufferAsLongBufferB(this, -1, 0, size, size, addr)
            : new ByteBufferAsLongBufferL(this, -1, 0, size, size, addr));
    }
    
    // ByteBuffer转为FloatBuffer
    public FloatBuffer asFloatBuffer() {
        int size = this.remaining() >> 2;
        long addr = address + position();
        return (bigEndian
            ? new ByteBufferAsFloatBufferB(this, -1, 0, size, size, addr)
            : new ByteBufferAsFloatBufferL(this, -1, 0, size, size, addr));
    }
    
    // ByteBuffer转为DoubleBuffer
    public DoubleBuffer asDoubleBuffer() {
        int size = this.remaining() >> 3;
        long addr = address + position();
        return (bigEndian
            ? new ByteBufferAsDoubleBufferB(this, -1, 0, size, size, addr)
            : new ByteBufferAsDoubleBufferL(this, -1, 0, size, size, addr));
    }
    
    /*▲ asXXXBuffer ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回索引i处的元素的偏移索引
    protected int ix(int i) {
        return i + offset;
    }
    
    // 获取当前缓冲区索引i处的元素的<地址>
    private long byteOffset(long i) {
        // 一个byte是1个字节，这里直接加上i
        return address + i;
    }
    
    byte _get(int i) {                          // package-private
        return hb[i];
    }
    
    void _put(int i, byte b) {                  // package-private
        hb[i] = b;
    }
}
