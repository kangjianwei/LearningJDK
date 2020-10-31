/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.io.IOException;
import java.util.Objects;

/**
 * A token representing a lock on a region of a file.
 *
 * <p> A file-lock object is created each time a lock is acquired on a file via
 * one of the {@link FileChannel#lock(long, long, boolean) lock} or {@link
 * FileChannel#tryLock(long, long, boolean) tryLock} methods of the
 * {@link FileChannel} class, or the {@link
 * AsynchronousFileChannel#lock(long, long, boolean, Object, CompletionHandler) lock}
 * or {@link AsynchronousFileChannel#tryLock(long, long, boolean) tryLock}
 * methods of the {@link AsynchronousFileChannel} class.
 *
 * <p> A file-lock object is initially valid.  It remains valid until the lock
 * is released by invoking the {@link #release release} method, by closing the
 * channel that was used to acquire it, or by the termination of the Java
 * virtual machine, whichever comes first.  The validity of a lock may be
 * tested by invoking its {@link #isValid isValid} method.
 *
 * <p> A file lock is either <i>exclusive</i> or <i>shared</i>.  A shared lock
 * prevents other concurrently-running programs from acquiring an overlapping
 * exclusive lock, but does allow them to acquire overlapping shared locks.  An
 * exclusive lock prevents other programs from acquiring an overlapping lock of
 * either type.  Once it is released, a lock has no further effect on the locks
 * that may be acquired by other programs.
 *
 * <p> Whether a lock is exclusive or shared may be determined by invoking its
 * {@link #isShared isShared} method.  Some platforms do not support shared
 * locks, in which case a request for a shared lock is automatically converted
 * into a request for an exclusive lock.
 *
 * <p> The locks held on a particular file by a single Java virtual machine do
 * not overlap.  The {@link #overlaps overlaps} method may be used to test
 * whether a candidate lock range overlaps an existing lock.
 *
 * <p> A file-lock object records the file channel upon whose file the lock is
 * held, the type and validity of the lock, and the position and size of the
 * locked region.  Only the validity of a lock is subject to change over time;
 * all other aspects of a lock's state are immutable.
 *
 * <p> File locks are held on behalf of the entire Java virtual machine.
 * They are not suitable for controlling access to a file by multiple
 * threads within the same virtual machine.
 *
 * <p> File-lock objects are safe for use by multiple concurrent threads.
 *
 *
 * <a id="pdep"></a><h2> Platform dependencies </h2>
 *
 * <p> This file-locking API is intended to map directly to the native locking
 * facility of the underlying operating system.  Thus the locks held on a file
 * should be visible to all programs that have access to the file, regardless
 * of the language in which those programs are written.
 *
 * <p> Whether or not a lock actually prevents another program from accessing
 * the content of the locked region is system-dependent and therefore
 * unspecified.  The native file-locking facilities of some systems are merely
 * <i>advisory</i>, meaning that programs must cooperatively observe a known
 * locking protocol in order to guarantee data integrity.  On other systems
 * native file locks are <i>mandatory</i>, meaning that if one program locks a
 * region of a file then other programs are actually prevented from accessing
 * that region in a way that would violate the lock.  On yet other systems,
 * whether native file locks are advisory or mandatory is configurable on a
 * per-file basis.  To ensure consistent and correct behavior across platforms,
 * it is strongly recommended that the locks provided by this API be used as if
 * they were advisory locks.
 *
 * <p> On some systems, acquiring a mandatory lock on a region of a file
 * prevents that region from being {@link java.nio.channels.FileChannel#map
 * <i>mapped into memory</i>}, and vice versa.  Programs that combine
 * locking and mapping should be prepared for this combination to fail.
 *
 * <p> On some systems, closing a channel releases all locks held by the Java
 * virtual machine on the underlying file regardless of whether the locks were
 * acquired via that channel or via another channel open on the same file.  It
 * is strongly recommended that, within a program, a unique channel be used to
 * acquire all locks on any given file.
 *
 * <p> Some network filesystems permit file locking to be used with
 * memory-mapped files only when the locked regions are page-aligned and a
 * whole multiple of the underlying hardware's page size.  Some network
 * filesystems do not implement file locks on regions that extend past a
 * certain position, often 2<sup>30</sup> or 2<sup>31</sup>.  In general, great
 * care should be taken when locking files that reside on network filesystems.
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */
/*
 * 文件锁，应用在进程级别，可以锁定同步文件通道与异步文件通道
 *
 * 锁的作用对象是文件而不是通道或线程，这意味着文件锁不适用于判别同一台Java虚拟机上的多个线程发起的访问。
 *
 * 如果一个线程在某个文件上获得了一个独占锁，然后第二个线程利用一个单独打开的通道来请求该文件的独占锁，那么第二个线程的请求会被批准。
 * 但如果这两个线程运行在不同的Java虚拟机上，那么第二个线程会阻塞，因为锁最终是由操作系统或文件系统来判别的，并且几乎总是在进程级而非线程级上判别。
 * 锁都是与一个文件关联的，而不是与单个的文件句柄或通道关联。
 *
 * FileLock对象是线程安全的，多个线程可以并发访问一个锁对象
 */
public abstract class FileLock implements AutoCloseable {
    
    private final Channel channel;  // 被锁定文件的通道（当前锁由channel创建）
    
    // 文件锁锁定区域的起点
    private final long position;
    
    /*
     * 文件锁锁定区域的尺寸
     *
     * 锁定区域的范围不一定要限制在文件的size值以内，锁可以扩展从而超出文件尾。
     * 因此，我们可以提前把待写入数据的区域锁定，也可以锁定一个不包含任何文件内容的区域，比如文件最后一个字节以外的区域。
     * 如果之后文件增长到提前锁定的区域，那么文件锁依然可以保护该区域的文件内容。
     * 相反，如果锁定了文件的某一块有限区域，而文件的增长超出了那块区域，那么新增加的文件内容将不会受到该文件锁的保护。
     */
    private final long size;
    
    // 文件锁是否为共享
    private final boolean shared;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class.
     *
     * @param channel  The file channel upon whose file this lock is held
     * @param position The position within the file at which the locked region starts;
     *                 must be non-negative
     * @param size     The size of the locked region; must be non-negative, and the sum
     *                 {@code position}&nbsp;+&nbsp;{@code size} must be non-negative
     * @param shared   {@code true} if this lock is shared,
     *                 {@code false} if it is exclusive
     *
     * @throws IllegalArgumentException If the preconditions on the parameters do not hold
     */
    // 构造作用于同步文件通道的文件锁
    protected FileLock(FileChannel channel, long position, long size, boolean shared) {
        Objects.requireNonNull(channel, "Null channel");
        
        if(position<0) {
            throw new IllegalArgumentException("Negative position");
        }
        
        if(size<0) {
            throw new IllegalArgumentException("Negative size");
        }
        
        if(position + size<0) {
            throw new IllegalArgumentException("Negative position + size");
        }
        
        this.channel = channel;
        this.position = position;
        this.size = size;
        this.shared = shared;
    }
    
    /**
     * Initializes a new instance of this class.
     *
     * @param channel  The channel upon whose file this lock is held
     * @param position The position within the file at which the locked region starts;
     *                 must be non-negative
     * @param size     The size of the locked region; must be non-negative, and the sum
     *                 {@code position}&nbsp;+&nbsp;{@code size} must be non-negative
     * @param shared   {@code true} if this lock is shared,
     *                 {@code false} if it is exclusive
     *
     * @throws IllegalArgumentException If the preconditions on the parameters do not hold
     * @since 1.7
     */
    // 构造作用于异步文件通道的文件锁
    protected FileLock(AsynchronousFileChannel channel, long position, long size, boolean shared) {
        Objects.requireNonNull(channel, "Null channel");
        
        if(position<0) {
            throw new IllegalArgumentException("Negative position");
        }
        
        if(size<0) {
            throw new IllegalArgumentException("Negative size");
        }
        
        if(position + size<0) {
            throw new IllegalArgumentException("Negative position + size");
        }
        
        this.channel = channel;
        this.position = position;
        this.size = size;
        this.shared = shared;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the channel upon whose file this lock was acquired.
     *
     * @return The channel upon whose file this lock was acquired.
     *
     * @since 1.7
     */
    // 返回被锁定文件的通道
    public Channel acquiredBy() {
        return channel;
    }
    
    /**
     * Returns the file channel upon whose file this lock was acquired.
     *
     * <p> This method has been superseded by the {@link #acquiredBy acquiredBy}
     * method.
     *
     * @return The file channel, or {@code null} if the file lock was not
     * acquired by a file channel.
     */
    // 返回被锁定文件的通道，该方法已被acquiredBy()取代
    public final FileChannel channel() {
        return (channel instanceof FileChannel) ? (FileChannel) channel : null;
    }
    
    /**
     * Returns the position within the file of the first byte of the locked
     * region.
     *
     * <p> A locked region need not be contained within, or even overlap, the
     * actual underlying file, so the value returned by this method may exceed
     * the file's current size.  </p>
     *
     * @return The position
     */
    // 文件锁锁定区域的起点
    public final long position() {
        return position;
    }
    
    /**
     * Returns the size of the locked region in bytes.
     *
     * <p> A locked region need not be contained within, or even overlap, the
     * actual underlying file, so the value returned by this method may exceed
     * the file's current size.  </p>
     *
     * @return The size of the locked region
     */
    // 文件锁锁定区域的尺寸
    public final long size() {
        return size;
    }
    
    /**
     * Tells whether this lock is shared.
     *
     * @return {@code true} if lock is shared,
     * {@code false} if it is exclusive
     */
    /*
     * 当前的文件锁是否为共享。
     *
     * 如果底层的操作系统或文件系统不支持共享锁，那么该方法将总是返回false值，即使申请锁时传递的参数值是true，这里也依旧返回false。
     */
    public final boolean isShared() {
        return shared;
    }
    
    /**
     * Tells whether or not this lock is valid.
     *
     * <p> A lock object remains valid until it is released or the associated
     * file channel is closed, whichever comes first.  </p>
     *
     * @return {@code true} if, and only if, this lock is valid
     */
    /*
     * 当前的文件锁是否有效
     *
     * 一个FileLock对象创建之后即有效，直到它的release()方法被调用，或它所关联的通道被关闭，或Java虚拟机关闭时才会失效。
     * 一个锁的有效性可能会随着时间而改变，不过它的其他属性：位置(position)、范围大小(size)和独占性(exclusivity)在创建时即被确定，不会随着时间而改变。
     */
    public abstract boolean isValid();
    
    
    /**
     * Releases this lock.
     *
     * <p> If this lock object is valid then invoking this method releases the
     * lock and renders the object invalid.  If this lock object is invalid
     * then invoking this method has no effect.  </p>
     *
     * @throws ClosedChannelException If the channel that was used to acquire this lock
     *                                is no longer open
     * @throws IOException            If an I/O error occurs
     */
    // 释放文件锁，并将其标记为失效（尤其是在发生异常时一定要释放锁，不然可能会影响下次的锁定操作）
    public abstract void release() throws IOException;
    
    /**
     * This method invokes the {@link #release} method. It was added
     * to the class so that it could be used in conjunction with the
     * automatic resource management block construct.
     *
     * @since 1.7
     */
    // 释放文件锁，并将其标记为失效
    public final void close() throws IOException {
        release();
    }
    
    
    /**
     * Tells whether or not this lock overlaps the given lock range.
     *
     * @param position The starting position of the lock range
     * @param size     The size of the lock range
     *
     * @return {@code true} if, and only if, this lock and the given lock
     * range overlap by at least one byte
     */
    /*
     * 判断当前文件锁锁定区域是否与给定的区域重叠
     *
     * 注：即使返回值是false，也不能保证一定可以在指定的区域上获得一个锁，
     * 　　因为Java虚拟机上的其他地方或者外部进程可能已经在该期望区域上有一个或多个锁了（最好使用tryLock()确认一下）。
     */
    public final boolean overlaps(long position, long size) {
        if(position + size<=this.position) {
            return false;               // That is below this
        }
        
        // This is below that
        return this.position + this.size>position;
    }
    
    
    /**
     * Returns a string describing the range, type, and validity of this lock.
     *
     * @return A descriptive string
     */
    public final String toString() {
        return (this.getClass().getName() + "[" + position + ":" + size + " " + (shared ? "shared" : "exclusive") + " " + (isValid() ? "valid" : "invalid") + "]");
    }
    
}
