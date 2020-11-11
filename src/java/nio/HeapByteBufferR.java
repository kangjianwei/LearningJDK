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
 * A read-only HeapByteBuffer.  This class extends the corresponding
 * read/write class, overriding the mutation methods to throw a {@link
 * ReadOnlyBufferException} and overriding the view-buffer methods to return an
 * instance of this class rather than of the superclass.
 */
// 只读、非直接缓冲区，内部存储结构实现为byte[]，是HeapByteBuffer的只读版本
class HeapByteBufferR extends HeapByteBuffer {
    
    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    // Cached array base offset
    private static final long ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(byte[].class);
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    protected HeapByteBufferR(byte[] buf, int mark, int pos, int lim, int cap, int off) {
        super(buf, mark, pos, lim, cap, off);
        this.isReadOnly = true;
    }
    
    HeapByteBufferR(int cap, int lim) {            // package-private
        super(cap, lim);
        this.isReadOnly = true;
    }
    
    HeapByteBufferR(byte[] buf, int off, int len) { // package-private
        super(buf, off, len);
        this.isReadOnly = true;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 只读缓冲区 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 只读/可读写
    public boolean isReadOnly() {
        return true;
    }
    
    /*▲ 只读缓冲区 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    public ByteBuffer slice() {
        return new HeapByteBufferR(hb, -1, 0, this.remaining(), this.remaining(), this.position() + offset);
    }
    
    // 切片，截取旧缓冲区【活跃区域】中pos~lim中的一段，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    ByteBuffer slice(int pos, int lim) {
        assert (pos >= 0);
        assert (pos <= lim);
        int rem = lim - pos;
        return new HeapByteBufferR(hb, -1, 0, rem, rem, pos + offset);
    }
    
    // 副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public ByteBuffer duplicate() {
        return new HeapByteBufferR(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }
    
    // 只读副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public ByteBuffer asReadOnlyBuffer() {
        return duplicate();
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 只读缓冲区，禁止写入操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向position+offset处写入byte，并将position递增
    public ByteBuffer put(byte x) {
        throw new ReadOnlyBufferException();
    }
    
    // 向i+offset处写入byte
    public ByteBuffer put(int i, byte x) {
        throw new ReadOnlyBufferException();
    }
    
    // 从源字节数组src的offset处开始，复制length个元素，写入到当前缓冲区【活跃区域】内（考虑偏移量）
    public ByteBuffer put(byte[] src, int offset, int length) {
        throw new ReadOnlyBufferException();
    }
    
    // 将源缓冲区src的内容全部写入到当前缓冲区
    public ByteBuffer put(ByteBuffer src) {
        throw new ReadOnlyBufferException();
    }
    
    // 将char转为byte存入缓冲区，将position增加2个单位
    public ByteBuffer putChar(char x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将char转为byte存入缓冲区索引i处
    public ByteBuffer putChar(int i, char x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将short转为byte存入缓冲区，将position增加2个单位
    public ByteBuffer putShort(short x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将short转为byte存入缓冲区索引i处
    public ByteBuffer putShort(int i, short x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将int转为byte存入缓冲区，将position增加4个单位
    public ByteBuffer putInt(int x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将int转为byte存入缓冲区索引i处
    public ByteBuffer putInt(int i, int x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将long转为byte存入缓冲区，将position增加8个单位
    public ByteBuffer putLong(long x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将long转为byte存入缓冲区索引i处
    public ByteBuffer putLong(int i, long x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将float转为byte存入缓冲区，将position增加4个单位
    public ByteBuffer putFloat(float x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将float转为byte存入缓冲区索引i处
    public ByteBuffer putFloat(int i, float x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将double转为byte存入缓冲区，将position增加8个单位
    public ByteBuffer putDouble(double x) {
        throw new ReadOnlyBufferException();
    }
    
    // 将double转为byte存入缓冲区索引i处
    public ByteBuffer putDouble(int i, double x) {
        throw new ReadOnlyBufferException();
    }
    
    /*▲ 只读缓冲区，禁止写入操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 禁止压缩，因为禁止写入，压缩没意义 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 压缩缓冲区，将当前未读完的数据挪到容器起始处，可用于读模式到写模式的切换，但又不丢失之前读入的数据。
    public ByteBuffer compact() {
        throw new ReadOnlyBufferException();
    }
    
    /*▲ 禁止压缩，因为禁止写入，压缩没意义 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ asXXXBuffer ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // ByteBuffer转为CharBuffer
    public CharBuffer asCharBuffer() {
        int size = this.remaining() >> 1;
        long addr = address + position();
        return (bigEndian ? new ByteBufferAsCharBufferRB(this, -1, 0, size, size, addr) : new ByteBufferAsCharBufferRL(this, -1, 0, size, size, addr));
    }
    
    // ByteBuffer转为ShortBuffer
    public ShortBuffer asShortBuffer() {
        int size = this.remaining() >> 1;
        long addr = address + position();
        return (bigEndian ? new ByteBufferAsShortBufferRB(this, -1, 0, size, size, addr) : new ByteBufferAsShortBufferRL(this, -1, 0, size, size, addr));
    }
    
    // ByteBuffer转为IntBuffer
    public IntBuffer asIntBuffer() {
        int size = this.remaining() >> 2;
        long addr = address + position();
        return (bigEndian ? new ByteBufferAsIntBufferRB(this, -1, 0, size, size, addr) : new ByteBufferAsIntBufferRL(this, -1, 0, size, size, addr));
    }
    
    // ByteBuffer转为LongBuffer
    public LongBuffer asLongBuffer() {
        int size = this.remaining() >> 3;
        long addr = address + position();
        return (bigEndian ? new ByteBufferAsLongBufferRB(this, -1, 0, size, size, addr) : new ByteBufferAsLongBufferRL(this, -1, 0, size, size, addr));
    }
    
    // ByteBuffer转为FloatBuffer
    public FloatBuffer asFloatBuffer() {
        int size = this.remaining() >> 2;
        long addr = address + position();
        return (bigEndian ? new ByteBufferAsFloatBufferRB(this, -1, 0, size, size, addr) : new ByteBufferAsFloatBufferRL(this, -1, 0, size, size, addr));
    }
    
    // ByteBuffer转为DoubleBuffer
    public DoubleBuffer asDoubleBuffer() {
        int size = this.remaining() >> 3;
        long addr = address + position();
        return (bigEndian ? new ByteBufferAsDoubleBufferRB(this, -1, 0, size, size, addr) : new ByteBufferAsDoubleBufferRL(this, -1, 0, size, size, addr));
    }
    
    /*▲ asXXXBuffer ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    byte _get(int i) {                          // package-private
        return hb[i];
    }
    
    void _put(int i, byte b) {                  // package-private
        throw new ReadOnlyBufferException();
    }
    
}
