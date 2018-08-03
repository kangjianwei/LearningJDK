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

import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.misc.JavaNioAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.Unsafe;

import java.util.Spliterator;

/**
 * A container for data of a specific primitive type.
 *
 * <p> A buffer is a linear, finite sequence of elements of a specific
 * primitive type.  Aside from its content, the essential properties of a
 * buffer are its capacity, limit, and position: </p>
 *
 * <blockquote>
 *
 * <p> A buffer's <i>capacity</i> is the number of elements it contains.  The
 * capacity of a buffer is never negative and never changes.  </p>
 *
 * <p> A buffer's <i>limit</i> is the index of the first element that should
 * not be read or written.  A buffer's limit is never negative and is never
 * greater than its capacity.  </p>
 *
 * <p> A buffer's <i>position</i> is the index of the next element to be
 * read or written.  A buffer's position is never negative and is never
 * greater than its limit.  </p>
 *
 * </blockquote>
 *
 * <p> There is one subclass of this class for each non-boolean primitive type.
 *
 *
 * <h2> Transferring data </h2>
 *
 * <p> Each subclass of this class defines two categories of <i>get</i> and
 * <i>put</i> operations: </p>
 *
 * <blockquote>
 *
 * <p> <i>Relative</i> operations read or write one or more elements starting
 * at the current position and then increment the position by the number of
 * elements transferred.  If the requested transfer exceeds the limit then a
 * relative <i>get</i> operation throws a {@link BufferUnderflowException}
 * and a relative <i>put</i> operation throws a {@link
 * BufferOverflowException}; in either case, no data is transferred.  </p>
 *
 * <p> <i>Absolute</i> operations take an explicit element index and do not
 * affect the position.  Absolute <i>get</i> and <i>put</i> operations throw
 * an {@link IndexOutOfBoundsException} if the index argument exceeds the
 * limit.  </p>
 *
 * </blockquote>
 *
 * <p> Data may also, of course, be transferred in to or out of a buffer by the
 * I/O operations of an appropriate channel, which are always relative to the
 * current position.
 *
 *
 * <h2> Marking and resetting </h2>
 *
 * <p> A buffer's <i>mark</i> is the index to which its position will be reset
 * when the {@link #reset reset} method is invoked.  The mark is not always
 * defined, but when it is defined it is never negative and is never greater
 * than the position.  If the mark is defined then it is discarded when the
 * position or the limit is adjusted to a value smaller than the mark.  If the
 * mark is not defined then invoking the {@link #reset reset} method causes an
 * {@link InvalidMarkException} to be thrown.
 *
 *
 * <h2> Invariants </h2>
 *
 * <p> The following invariant holds for the mark, position, limit, and
 * capacity values:
 *
 * <blockquote>
 * {@code 0} {@code <=}
 * <i>mark</i> {@code <=}
 * <i>position</i> {@code <=}
 * <i>limit</i> {@code <=}
 * <i>capacity</i>
 * </blockquote>
 *
 * <p> A newly-created buffer always has a position of zero and a mark that is
 * undefined.  The initial limit may be zero, or it may be some other value
 * that depends upon the type of the buffer and the manner in which it is
 * constructed.  Each element of a newly-allocated buffer is initialized
 * to zero.
 *
 *
 * <h2> Additional operations </h2>
 *
 * <p> In addition to methods for accessing the position, limit, and capacity
 * values and for marking and resetting, this class also defines the following
 * operations upon buffers:
 *
 * <ul>
 *
 * <li><p> {@link #clear} makes a buffer ready for a new sequence of
 * channel-read or relative <i>put</i> operations: It sets the limit to the
 * capacity and the position to zero.  </p></li>
 *
 * <li><p> {@link #flip} makes a buffer ready for a new sequence of
 * channel-write or relative <i>get</i> operations: It sets the limit to the
 * current position and then sets the position to zero.  </p></li>
 *
 * <li><p> {@link #rewind} makes a buffer ready for re-reading the data that
 * it already contains: It leaves the limit unchanged and sets the position
 * to zero.  </p></li>
 *
 * <li><p> {@link #slice} creates a subsequence of a buffer: It leaves the
 * limit and the position unchanged. </p></li>
 *
 * <li><p> {@link #duplicate} creates a shallow copy of a buffer: It leaves
 * the limit and the position unchanged. </p></li>
 *
 * </ul>
 *
 *
 * <h2> Read-only buffers </h2>
 *
 * <p> Every buffer is readable, but not every buffer is writable.  The
 * mutation methods of each buffer class are specified as <i>optional
 * operations</i> that will throw a {@link ReadOnlyBufferException} when
 * invoked upon a read-only buffer.  A read-only buffer does not allow its
 * content to be changed, but its mark, position, and limit values are mutable.
 * Whether or not a buffer is read-only may be determined by invoking its
 * {@link #isReadOnly isReadOnly} method.
 *
 *
 * <h2> Thread safety </h2>
 *
 * <p> Buffers are not safe for use by multiple concurrent threads.  If a
 * buffer is to be used by more than one thread then access to the buffer
 * should be controlled by appropriate synchronization.
 *
 *
 * <h2> Invocation chaining </h2>
 *
 * <p> Methods in this class that do not otherwise have a value to return are
 * specified to return the buffer upon which they are invoked.  This allows
 * method invocations to be chained; for example, the sequence of statements
 *
 * <blockquote><pre>
 * b.flip();
 * b.position(23);
 * b.limit(42);</pre></blockquote>
 *
 * can be replaced by the single, more compact statement
 *
 * <blockquote><pre>
 * b.flip().position(23).limit(42);</pre></blockquote>
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

/*
 * 缓冲区的抽象基类，其内部实现为一个数组或一个直接缓冲区。
 *
 * 该缓冲区读写两用，靠着游标position和上界limit标记【活跃区域】。
 * 不管处于读模式还是处于写模式，【活跃区域】总是：[position, limit)。
 * 要特别区分【读模式】和【写模式】下各方法及参数的含义。
 *
 * 缓冲区有四个标记，它们的关系是: mark <= position <= limit <= capacity
 * 注意，这些标记表示的是相对于当前缓存区的位置（相对位置），而不是相对于内部存储结构的位置（绝对位置）。
 * 比如postion=1，代表的是当前缓冲区中索引为1的元素，而不是内部存储结构中索引为1的元素。
 * 当前缓冲区脱胎于其内部存储结构，该内部存储结构是共享的，可被多个缓冲区共享。
 * 区分每个缓冲区的【绝对起点】靠的是address字段和offset字段。
 *
 * 非直接缓冲区：(堆内存)
 *     通过allocate()分配缓冲区，将缓冲区建立在JVM的内存中。通过常规手段存取元素。
 * 直接缓冲区：（堆外内存，可通过-XX:MaxDirectMemorySize设置大小）
 *     通过allocateDirect()分配直接缓冲区，将缓冲区建立在物理内存中，可以提高效率。通过Unsafe存取元素。
 */
public abstract class Buffer {
    // Cached unsafe-access object
    static final Unsafe UNSAFE = Unsafe.getUnsafe();
    
    /**
     * The characteristics of Spliterators that traverse and split elements maintained in Buffers.
     */
    static final int SPLITERATOR_CHARACTERISTICS = Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED;
    
    // Used by heap byte buffers or direct buffers with Unsafe access
    // For heap byte buffers this field will be the address relative to the array base address and offset into that array.
    // The address might not align on a word boundary for slices, nor align at a long word (8 byte) boundary for byte[] allocations on 32-bit systems.
    // For direct buffers it is the start address of the memory region.
    // The address might not align on a word boundary for slices, nor when created using JNI, see NewDirectByteBuffer(void*, long).
    // Should ideally be declared final
    // NOTE: hoisted here for speed in JNI GetDirectBufferAddress
    long address;   // 缓冲区【绝对】起始地址，仅用于Unsafe类访问直接缓冲区的内部存储结构
    
    /*
     * 关系: mark <= position <= limit <= capacity
     * 这里约定一些术语：：
     * 【活跃区域】：[position, limit)范围的区域，这个区域是不断变化的
     * 【原始区域】：position和limit的初始值限定的区域，这个区域一般不变
     */
    private int mark = -1;      // 标记。一个备忘位置。调用mark()来设定mark = postion。调用reset()设定position = mark。标记在设定前是未定义的(undefined)。
    private int position = 0;   // 游标。下一个要被读或写的元素的索引。位置会自动由相应的get()和put()函数更新。
    private int limit;          // 上界。缓冲区的第一个不能被读或写的元素。或者说，缓冲区中现存元素的计数。
    private int capacity;       // 容量。缓冲区能够容纳的数据元素的最大数量。这一容量在缓冲区创建时被设定，并且永远不能被改变。
    
    static {
        // setup access to this package in SharedSecrets
        SharedSecrets.setJavaNioAccess(new JavaNioAccess() {
            @Override
            public JavaNioAccess.BufferPool getDirectBufferPool() {
                return Bits.BUFFER_POOL;
            }
            
            @Override
            public ByteBuffer newDirectByteBuffer(long addr, int cap, Object ob) {
                return new DirectByteBuffer(addr, cap, ob);
            }
            
            @Override
            public void truncate(Buffer buf) {
                buf.truncate();
            }
        });
    }
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /* Creates a new buffer with the given mark, position, limit, and capacity, after checking invariants. */
    // 初始化一个Buffer
    Buffer(int mark, int pos, int lim, int cap) {       // package-private
        if(cap < 0)
            throw createCapacityException(cap);
        this.capacity = cap;
        limit(lim);
        position(pos);
        if(mark >= 0) {
            if(mark > pos)
                throw new IllegalArgumentException("mark > position: (" + mark + " > " + pos + ")");
            this.mark = mark;
        }
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 缓冲区属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether or not this buffer is read-only.
     *
     * @return {@code true} if, and only if, this buffer is read-only
     */
    // 只读/可读写
    public abstract boolean isReadOnly();
    
    /**
     * Tells whether or not this buffer is
     * <a href="ByteBuffer.html#direct"><i>direct</i></a>.
     *
     * @return {@code true} if, and only if, this buffer is direct
     *
     * @since 1.6
     */
    // 直接缓冲区/非直接缓冲区
    public abstract boolean isDirect();
    
    /*▲ 缓冲区属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 标记操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets this buffer's mark at its position.
     *
     * @return This buffer
     */
    // 在当前游标position处设置新的mark（备忘）
    public Buffer mark() {
        mark = position;
        return this;
    }
    
    /**
     * Sets this buffer's position.  If the mark is defined and larger than the
     * new position then it is discarded.
     *
     * @param newPosition The new position value; must be non-negative
     *                    and no larger than the current limit
     *
     * @return This buffer
     *
     * @throws IllegalArgumentException If the preconditions on {@code newPosition} do not hold
     */
    // 设置新的游标position
    public Buffer position(int newPosition) {
        if(newPosition > limit | newPosition < 0)
            throw createPositionException(newPosition);
        position = newPosition;
        if(mark > position)
            mark = -1;
        return this;
    }
    
    /**
     * Sets this buffer's limit.  If the position is larger than the new limit
     * then it is set to the new limit.  If the mark is defined and larger than
     * the new limit then it is discarded.
     *
     * @param newLimit The new limit value; must be non-negative
     *                 and no larger than this buffer's capacity
     *
     * @return This buffer
     *
     * @throws IllegalArgumentException If the preconditions on {@code newLimit} do not hold
     */
    // 设置新的上界limit
    public Buffer limit(int newLimit) {
        if(newLimit > capacity | newLimit < 0)
            throw createLimitException(newLimit);
        
        limit = newLimit;
        if(position > limit)
            position = limit;
        if(mark > limit)
            mark = -1;
        return this;
    }
    
    /**
     * Resets this buffer's position to the previously-marked position.
     *
     * <p> Invoking this method neither changes nor discards the mark's
     * value. </p>
     *
     * @return This buffer
     *
     * @throws InvalidMarkException If the mark has not been set
     */
    // 将当前游标position回退到mark（备忘）位置
    public Buffer reset() {
        int m = mark;
        if(m < 0)
            throw new InvalidMarkException();
        position = m;
        return this;
    }
    
    /**
     * Clears this buffer.  The position is set to zero, the limit is set to
     * the capacity, and the mark is discarded.
     *
     * <p> Invoke this method before using a sequence of channel-read or
     * <i>put</i> operations to fill this buffer.  For example:
     *
     * <blockquote><pre>
     * buf.clear();     // Prepare buffer for reading
     * in.read(buf);    // Read data</pre></blockquote>
     *
     * <p> This method does not actually erase the data in the buffer, but it
     * is named as if it did because it will most often be used in situations
     * in which that might as well be the case. </p>
     *
     * @return This buffer
     */
    // 清理缓冲区，重置标记
    public Buffer clear() {
        position = 0;
        limit = capacity;
        mark = -1;
        return this;
    }
    
    /**
     * Flips this buffer.  The limit is set to the current position and then
     * the position is set to zero.  If the mark is defined then it is
     * discarded.
     *
     * <p> After a sequence of channel-read or <i>put</i> operations, invoke
     * this method to prepare for a sequence of channel-write or relative
     * <i>get</i> operations.  For example:
     *
     * <blockquote><pre>
     * buf.put(magic);    // Prepend header
     * in.read(buf);      // Read data into rest of buffer
     * buf.flip();        // Flip buffer
     * out.write(buf);    // Write header + data to channel</pre></blockquote>
     *
     * <p> This method is often used in conjunction with the {@link
     * java.nio.ByteBuffer#compact compact} method when transferring data from
     * one place to another.  </p>
     *
     * @return This buffer
     */
    // 修改标记，可以切换缓冲区读/写模式
    public Buffer flip() {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }
    
    /**
     * Rewinds this buffer.  The position is set to zero and the mark is
     * discarded.
     *
     * <p> Invoke this method before a sequence of channel-write or <i>get</i>
     * operations, assuming that the limit has already been set
     * appropriately.  For example:
     *
     * <blockquote><pre>
     * out.write(buf);    // Write remaining data
     * buf.rewind();      // Rewind buffer
     * buf.get(array);    // Copy data into array</pre></blockquote>
     *
     * @return This buffer
     */
    // 丢弃备忘，游标归零
    public Buffer rewind() {
        position = 0;
        mark = -1;
        return this;
    }
    
    // 返回mark
    final int markValue() {
        return mark;
    }
    
    /**
     * Returns this buffer's position.
     *
     * @return The position of this buffer
     */
    // 返回position
    public final int position() {
        return position;
    }
    
    /**
     * Returns this buffer's limit.
     *
     * @return The limit of this buffer
     */
    // 返回limit
    public final int limit() {
        return limit;
    }
    
    /**
     * Returns this buffer's capacity.
     *
     * @return The capacity of this buffer
     */
    // 返回capacity
    public final int capacity() {
        return capacity;
    }
    
    // 丢弃备忘
    final void discardMark() {
        mark = -1;
    }
    
    // 消耗缓冲区（容量清零）
    final void truncate() {
        mark = -1;
        position = 0;
        limit = 0;
        capacity = 0;
    }
    
    /*▲ 标记操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new buffer whose content is a shared subsequence of
     * this buffer's content.
     *
     * <p> The content of the new buffer will start at this buffer's current
     * position.  Changes to this buffer's content will be visible in the new
     * buffer, and vice versa; the two buffers' position, limit, and mark
     * values will be independent.
     *
     * <p> The new buffer's position will be zero, its capacity and its limit
     * will be the number of elements remaining in this buffer, its mark will be
     * undefined. The new buffer will be direct if, and only if, this buffer is
     * direct, and it will be read-only if, and only if, this buffer is
     * read-only.  </p>
     *
     * @return The new buffer
     *
     * @since 9
     */
    public abstract Buffer slice();
    
    /**
     * Creates a new buffer that shares this buffer's content.
     *
     * <p> The content of the new buffer will be that of this buffer.  Changes
     * to this buffer's content will be visible in the new buffer, and vice
     * versa; the two buffers' position, limit, and mark values will be
     * independent.
     *
     * <p> The new buffer's capacity, limit, position and mark values will be
     * identical to those of this buffer. The new buffer will be direct if, and
     * only if, this buffer is direct, and it will be read-only if, and only if,
     * this buffer is read-only.  </p>
     *
     * @return The new buffer
     *
     * @since 9
     */
    public abstract Buffer duplicate();
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    /**
     * Returns the number of elements between the current position and the limit.
     *
     * @return The number of elements remaining in this buffer
     */
    // 返回缓冲区长度（还剩多少元素/还剩多少空间）
    public final int remaining() {
        return limit - position;
    }
    
    /**
     * Tells whether there are any elements between the current position and
     * the limit.
     *
     * @return {@code true} if, and only if, there is at least one element
     * remaining in this buffer
     */
    // true：缓冲区还有剩余（未读完/未写完）
    public final boolean hasRemaining() {
        return position < limit;
    }
    
    
    
    
    /**
     * This method is intended to allow array-backed buffers to be passed to native code more efficiently.
     * Concrete subclasses provide more strongly-typed return values for this method.
     *
     * Modifications to this buffer's content will cause the returned array's content to be modified, and vice versa.
     *
     * Invoke the hasArray method before invoking this method in order to ensure that this buffer has an accessible backing array.
     *
     * @return The array that backs this buffer
     *
     * @throws ReadOnlyBufferException       If this buffer is backed by an array but is read-only
     * @throws UnsupportedOperationException If this buffer is not backed by an accessible array
     * @since 1.6
     */
    // 返回该buffer内部的非只读数组
    public abstract Object array();
    
    /**
     * Returns the offset within this buffer's backing array of the first
     * element of the buffer&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> If this buffer is backed by an array then buffer position <i>p</i>
     * corresponds to array index <i>p</i>&nbsp;+&nbsp;{@code arrayOffset()}.
     *
     * <p> Invoke the {@link #hasArray hasArray} method before invoking this
     * method in order to ensure that this buffer has an accessible backing
     * array.  </p>
     *
     * @return The offset within this buffer's array
     * of the first element of the buffer
     *
     * @throws ReadOnlyBufferException       If this buffer is backed by an array but is read-only
     * @throws UnsupportedOperationException If this buffer is not backed by an accessible array
     * @since 1.6
     */
    // 返回此缓冲区中的第一个元素在缓冲区的底层实现数组中的偏移量（可选操作）
    public abstract int arrayOffset();
    
    /**
     * Tells whether or not this buffer is backed by an accessible array.
     *
     * If this method returns {@code true} then the {@link #array() array} and {@link #arrayOffset() arrayOffset} methods may safely be invoked.
     *
     * @return {@code true} if, and only if, this buffer is backed by an array and is not read-only
     *
     * @since 1.6
     */
    // true：此buffer由可访问的数组实现
    public abstract boolean hasArray();
    
    /**
     * @return the base reference, paired with the address field, which in combination can be used for unsafe access into a heap buffer or direct byte buffer (and views of).
     */
    // 返回内部存储结构的引用（一般用于非直接缓存区）
    abstract Object base();
    
    
    
    /**
     * Checks the current position against the limit, throwing a {@link
     * BufferUnderflowException} if it is not smaller than the limit, and then
     * increments the position.
     *
     * @return The current position value, before it is incremented
     */
    // 返回position，并将position递增
    final int nextGetIndex() {
        if(position >= limit)
            throw new BufferUnderflowException();
        return position++;
    }
    
    // 返回position，并将position增加nb个单位
    final int nextGetIndex(int nb) {
        if(limit - position < nb)
            throw new BufferUnderflowException();
        int p = position;
        position += nb;
        return p;
    }
    
    /**
     * Checks the current position against the limit, throwing a {@link
     * BufferOverflowException} if it is not smaller than the limit, and then
     * increments the position.
     *
     * @return The current position value, before it is incremented
     */
    // 返回position，并将position递增
    final int nextPutIndex() {                          // package-private
        if(position >= limit)
            throw new BufferOverflowException();
        return position++;
    }
    
    // 返回position，并将position增加nb个单位
    final int nextPutIndex(int nb) {
        if(limit - position < nb)
            throw new BufferOverflowException();
        int p = position;
        position += nb;
        return p;
    }
    
    
    
    /**
     * Checks the given index against the limit, throwing an {@link
     * IndexOutOfBoundsException} if it is not smaller than the limit
     * or is smaller than zero.
     */
    // 保证  0 <= i < limit
    @HotSpotIntrinsicCandidate
    final int checkIndex(int i) {
        if((i < 0) || (i >= limit))
            throw new IndexOutOfBoundsException();
        return i;
    }
    
    // 保证 i>=0 且 nb+i<=limit
    final int checkIndex(int i, int nb) {
        if((i < 0) || (nb > limit - i))
            throw new IndexOutOfBoundsException();
        return i;
    }
    
    // 保证 off+len <= size
    static void checkBounds(int off, int len, int size) { // package-private
        if((off | len | (off + len) | (size - (off + len))) < 0)
            throw new IndexOutOfBoundsException();
    }
    
    
    
    /**
     * Returns an {@code IllegalArgumentException} indicating that the source
     * and target are the same {@code Buffer}.  Intended for use in
     * {@code put(src)} when the parameter is the {@code Buffer} on which the
     * method is being invoked.
     *
     * @return IllegalArgumentException
     * With a message indicating equal source and target buffers
     */
    static IllegalArgumentException createSameBufferException() {
        return new IllegalArgumentException("The source buffer is this buffer");
    }
    
    /**
     * Verify that the capacity is nonnegative.
     *
     * @param capacity The new buffer's capacity, in $type$s
     *
     * @throws IllegalArgumentException If the {@code capacity} is a negative integer
     */
    static IllegalArgumentException createCapacityException(int capacity) {
        assert capacity < 0 : "capacity expected to be negative";
        return new IllegalArgumentException("capacity < 0: (" + capacity + " < 0)");
    }
    
    /**
     * Verify that {@code 0 < newPosition <= limit}
     *
     * @param newPosition The new position value
     *
     * @throws IllegalArgumentException If the specified position is out of bounds.
     */
    private IllegalArgumentException createPositionException(int newPosition) {
        String msg = null;
        
        if(newPosition > limit) {
            msg = "newPosition > limit: (" + newPosition + " > " + limit + ")";
        } else { // assume negative
            assert newPosition < 0 : "newPosition expected to be negative";
            msg = "newPosition < 0: (" + newPosition + " < 0)";
        }
        
        return new IllegalArgumentException(msg);
    }
    
    /**
     * Verify that {@code 0 < newLimit <= capacity}
     *
     * @param newLimit The new limit value
     *
     * @throws IllegalArgumentException If the specified limit is out of bounds.
     */
    private IllegalArgumentException createLimitException(int newLimit) {
        String msg = null;
        
        if(newLimit > capacity) {
            msg = "newLimit > capacity: (" + newLimit + " > " + capacity + ")";
        } else { // assume negative
            assert newLimit < 0 : "newLimit expected to be negative";
            msg = "newLimit < 0: (" + newLimit + " < 0)";
        }
        
        return new IllegalArgumentException(msg);
    }
}
