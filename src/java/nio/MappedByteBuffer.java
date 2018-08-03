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

import jdk.internal.misc.Unsafe;

import java.io.FileDescriptor;
import java.lang.ref.Reference;

/**
 * A direct byte buffer whose content is a memory-mapped region of a file.
 *
 * <p> Mapped byte buffers are created via the {@link
 * java.nio.channels.FileChannel#map FileChannel.map} method.  This class
 * extends the {@link ByteBuffer} class with operations that are specific to
 * memory-mapped file regions.
 *
 * <p> A mapped byte buffer and the file mapping that it represents remain
 * valid until the buffer itself is garbage-collected.
 *
 * <p> The content of a mapped byte buffer can change at any time, for example
 * if the content of the corresponding region of the mapped file is changed by
 * this program or another.  Whether or not such changes occur, and when they
 * occur, is operating-system dependent and therefore unspecified.
 *
 * <a id="inaccess"></a><p> All or part of a mapped byte buffer may become
 * inaccessible at any time, for example if the mapped file is truncated.  An
 * attempt to access an inaccessible region of a mapped byte buffer will not
 * change the buffer's content and will cause an unspecified exception to be
 * thrown either at the time of the access or at some later time.  It is
 * therefore strongly recommended that appropriate precautions be taken to
 * avoid the manipulation of a mapped file by this program, or by a
 * concurrently running program, except to read or write the file's content.
 *
 * <p> Mapped byte buffers otherwise behave no differently than ordinary direct
 * byte buffers. </p>
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

// 基于内存的直接字节缓冲区，该对象的数据元素是存储在磁盘上的文件
public abstract class MappedByteBuffer extends ByteBuffer {
    /*
     * This is a little bit backwards: By rights MappedByteBuffer should be a subclass of DirectByteBuffer,
     * but to keep the spec clear and simple, and for optimization purposes, it's easier to do it the other way around.
     * This works because DirectByteBuffer is a package-private class.
     */
    
    // not used, but a potential target for a store, see load() for details.
    private static byte unused;
    
    // For mapped buffers, a FileDescriptor that may be used for mapping operations if valid; null if the buffer is not mapped.
    private final FileDescriptor fd;
    
    
    
    // This should only be invoked by the DirectByteBuffer constructors
    MappedByteBuffer(int mark, int pos, int lim, int cap, FileDescriptor fd) {
        super(mark, pos, lim, cap);
        this.fd = fd;
    }
    
    MappedByteBuffer(int mark, int pos, int lim, int cap) { // package-private
        super(mark, pos, lim, cap);
        this.fd = null;
    }
    
    
    
    /*▼ 加载文件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Loads this buffer's content into physical memory.
     *
     * <p> This method makes a best effort to ensure that, when it returns,
     * this buffer's content is resident in physical memory.  Invoking this
     * method may cause some number of page faults and I/O operations to
     * occur. </p>
     *
     * @return This buffer
     */
    /*
     * 该方法的主要作用是为提前加载文件埋单，以便后续的访问速度可以尽可能的快。
     *
     * 为一个文件建立虚拟内存映射后，文件数据往往不会因此从磁盘读取到内存（这取决于操作系统）。
     * 该过程类似打开一个文件：文件先被定位，然后一个文件句柄会被创建，随后通过文件句柄来访问文件数据。
     *
     * 对于映射缓冲区，虚拟内存系统将根据需要来把文件中相应区块的数据读进来。
     * 这个页验证或防错过程需要一定的时间，因为将文件数据读取到内存需要一次或多次的磁盘访问。
     * 某些场景下，可能想先把所有的页都读进内存以实现最小的缓冲区访问延迟。
     * 如果文件的所有页都是常驻内存的，那么它的访问速度就和访问一个基于内存的缓冲区一样了。
     *
     * load()方法会加载整个文件以使它常驻内存，此映射使得操作系统的底层虚拟内存子系统可以根据需要将文件中相应区块的数据读进内存。
     * 已经在内存中或通过验证的页会占用实际内存空间，并且在它们被读进RAM时会挤出最近较少使用的其他内存页。
     *
     * 在一个映射缓冲区上调用load()方法会是一个代价高的操作，因为它会导致大量的页调入（page-in），具体数量取决于文件中被映射区域的实际大小。
     * 然而，load()方法返回并不能保证文件就会完全常驻内存，这是由于请求页面调入（demand paging）是动态的。
     * 具体结果会因某些因素而有所差异，这些因素包括：操作系统、文件系统，可用Java虚拟机内存，最大Java虚拟机内存，垃圾收集器实现过程等等。
     * 务必小心使用load()方法，它可能会导致不希望出现的结果。
     */
    public final MappedByteBuffer load() {
        if(fd == null) {
            return this;
        }
        if((address == 0) || (capacity() == 0))
            return this;
        long offset = mappingOffset();
        long length = mappingLength(offset);
        load0(mappingAddress(offset), length);
        
        // Read a byte from each page to bring it into memory.
        // A checksum is computed as we go along to prevent the compiler from otherwise considering the loop as dead code.
        Unsafe unsafe = Unsafe.getUnsafe();
        int ps = Bits.pageSize();
        int count = Bits.pageCount(length);
        long a = mappingAddress(offset);
        byte x = 0;
        try {
            for(int i = 0; i < count; i++) {
                // TODO consider changing to getByteOpaque thus avoiding dead code elimination and the need to calculate a checksum
                x ^= unsafe.getByte(a);
                a += ps;
            }
        } finally {
            Reference.reachabilityFence(this);
        }
        if(unused != 0)
            unused = x;
        
        return this;
    }
    
    /**
     * Tells whether or not this buffer's content is resident in physical
     * memory.
     *
     * <p> A return value of {@code true} implies that it is highly likely
     * that all of the data in this buffer is resident in physical memory and
     * may therefore be accessed without incurring any virtual-memory page
     * faults or I/O operations.  A return value of {@code false} does not
     * necessarily imply that the buffer's content is not resident in physical
     * memory.
     *
     * <p> The returned value is a hint, rather than a guarantee, because the
     * underlying operating system may have paged out some of the buffer's data
     * by the time that an invocation of this method returns.  </p>
     *
     * @return {@code true} if it is likely that this buffer's content
     * is resident in physical memory
     */
    /*
     * 可以通过调用isLoaded()方法来判断一个被映射的文件是否完全常驻内存了。
     * 如果该方法返回true值，那么很大概率是映射缓冲区的访问延迟很少或者根本没有延迟。
     * 不过，这也是不能保证的。
     * 同样地，返回false值并不一定意味着访问缓冲区将很慢或者该文件并未完全常驻内存。
     * isLoaded()方法的返回值只是一个暗示，由于垃圾收集的异步性质、底层操作系统以及运行系统的动态性等因素，想要在任意时刻准确判断全部映射页的状态是不可能的。
     */
    public final boolean isLoaded() {
        if(fd == null) {
            return true;
        }
        if((address == 0) || (capacity() == 0))
            return true;
        long offset = mappingOffset();
        long length = mappingLength(offset);
        return isLoaded0(mappingAddress(offset), length, Bits.pageCount(length));
    }
    
    /**
     * Forces any changes made to this buffer's content to be written to the
     * storage device containing the mapped file.
     *
     * <p> If the file mapped into this buffer resides on a local storage
     * device then when this method returns it is guaranteed that all changes
     * made to the buffer since it was created, or since this method was last
     * invoked, will have been written to that device.
     *
     * <p> If the file does not reside on a local device then no such guarantee
     * is made.
     *
     * <p> If this buffer was not mapped in read/write mode ({@link
     * java.nio.channels.FileChannel.MapMode#READ_WRITE}) then invoking this
     * method has no effect. </p>
     *
     * @return This buffer
     */
    /*
     * force()同FileChannel类中的同名方法相似，该方法会强制将映射缓冲区上的更改应用到永久磁盘存储器上。
     * 当用MappedByteBuffer对象来更新一个文件，应该总是使用MappedByteBuffer.force()而非FileChannel.force()，因为通道对象可能不清楚通过映射缓冲区做出的文件的全部更改。
     *
     * 如果映射是以MapMode.READ_ONLY或MAP_MODE.PRIVATE模式建立的，那么调用force( )方法将不起任何作用，因为永远不会有更改需要应用到磁盘上（但是这样做也没有害处）。
     */
    public final MappedByteBuffer force() {
        if(fd == null) {
            return this;
        }
        if((address != 0) && (capacity() != 0)) {
            long offset = mappingOffset();
            force0(fd, mappingAddress(offset), mappingLength(offset));
        }
        return this;
    }
    
    /*▲ 加载文件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 标记操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     */
    // 设置新的上界limit
    @Override
    public final MappedByteBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 设置新的游标position
    @Override
    public final MappedByteBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 在当前游标position处设置新的mark（备忘）
    @Override
    public final MappedByteBuffer mark() {
        super.mark();
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 将当前游标position回退到mark（备忘）位置
    @Override
    public final MappedByteBuffer reset() {
        super.reset();
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 清理缓冲区，重置标记
    @Override
    public final MappedByteBuffer clear() {
        super.clear();
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 修改标记，可以切换缓冲区读/写模式
    @Override
    public final MappedByteBuffer flip() {
        super.flip();
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 丢弃备忘，游标归零
    @Override
    public final MappedByteBuffer rewind() {
        super.rewind();
        return this;
    }
    
    /*▲ 标记操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // Returns the distance (in bytes) of the buffer from the page aligned address of the mapping.
    // Computed each time to avoid storing in every direct buffer.
    private long mappingOffset() {
        int ps = Bits.pageSize();
        long offset = address % ps;
        return (offset >= 0) ? offset : (ps + offset);
    }
    
    private long mappingAddress(long mappingOffset) {
        return address - mappingOffset;
    }
    
    private long mappingLength(long mappingOffset) {
        return (long) capacity() + mappingOffset;
    }
    
    private native boolean isLoaded0(long address, long length, int pageCount);
    
    private native void load0(long address, long length);
    
    private native void force0(FileDescriptor fd, long address, long length);
}
