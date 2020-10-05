/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * An asynchronous channel for reading, writing, and manipulating a file.
 *
 * <p> An asynchronous file channel is created when a file is opened by invoking
 * one of the {@link #open open} methods defined by this class. The file contains
 * a variable-length sequence of bytes that can be read and written and whose
 * current size can be {@link #size() queried}. The size of the file increases
 * when bytes are written beyond its  current size; the size of the file decreases
 * when it is {@link #truncate truncated}.
 *
 * <p> An asynchronous file channel does not have a <i>current position</i>
 * within the file. Instead, the file position is specified to each read and
 * write method that initiates asynchronous operations. A {@link CompletionHandler}
 * is specified as a parameter and is invoked to consume the result of the I/O
 * operation. This class also defines read and write methods that initiate
 * asynchronous operations, returning a {@link Future} to represent the pending
 * result of the operation. The {@code Future} may be used to check if the
 * operation has completed, wait for its completion, and retrieve the result.
 *
 * <p> In addition to read and write operations, this class defines the
 * following operations: </p>
 *
 * <ul>
 *
 *   <li><p> Updates made to a file may be {@link #force <i>forced
 *   out</i>} to the underlying storage device, ensuring that data are not
 *   lost in the event of a system crash.  </p></li>
 *
 *   <li><p> A region of a file may be {@link #lock <i>locked</i>} against
 *   access by other programs.  </p></li>
 *
 * </ul>
 *
 * <p> An {@code AsynchronousFileChannel} is associated with a thread pool to
 * which tasks are submitted to handle I/O events and dispatch to completion
 * handlers that consume the results of I/O operations on the channel. The
 * completion handler for an I/O operation initiated on a channel is guaranteed
 * to be invoked by one of the threads in the thread pool (This ensures that the
 * completion handler is run by a thread with the expected <em>identity</em>).
 * Where an I/O operation completes immediately, and the initiating thread is
 * itself a thread in the thread pool, then the completion handler may be invoked
 * directly by the initiating thread. When an {@code AsynchronousFileChannel} is
 * created without specifying a thread pool then the channel is associated with
 * a system-dependent default thread pool that may be shared with other
 * channels. The default thread pool is configured by the system properties
 * defined by the {@link AsynchronousChannelGroup} class.
 *
 * <p> Channels of this type are safe for use by multiple concurrent threads. The
 * {@link Channel#close close} method may be invoked at any time, as specified
 * by the {@link Channel} interface. This causes all outstanding asynchronous
 * operations on the channel to complete with the exception {@link
 * AsynchronousCloseException}. Multiple read and write operations may be
 * outstanding at the same time. When multiple read and write operations are
 * outstanding then the ordering of the I/O operations, and the order that the
 * completion handlers are invoked, is not specified; they are not, in particular,
 * guaranteed to execute in the order that the operations were initiated. The
 * {@link java.nio.ByteBuffer ByteBuffers} used when reading or writing are not
 * safe for use by multiple concurrent I/O operations. Furthermore, after an I/O
 * operation is initiated then care should be taken to ensure that the buffer is
 * not accessed until after the operation has completed.
 *
 * <p> As with {@link FileChannel}, the view of a file provided by an instance of
 * this class is guaranteed to be consistent with other views of the same file
 * provided by other instances in the same program.  The view provided by an
 * instance of this class may or may not, however, be consistent with the views
 * seen by other concurrently-running programs due to caching performed by the
 * underlying operating system and delays induced by network-filesystem protocols.
 * This is true regardless of the language in which these other programs are
 * written, and whether they are running on the same machine or on some other
 * machine.  The exact nature of any such inconsistencies are system-dependent
 * and are therefore unspecified.
 *
 * @since 1.7
 */
// 异步文件通道
public abstract class AsynchronousFileChannel implements AsynchronousChannel {
    
    // 空白的文件属性
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final FileAttribute<?>[] NO_ATTRIBUTES = new FileAttribute[0];
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class.
     */
    protected AsynchronousFileChannel() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens or creates a file for reading and/or writing, returning an
     * asynchronous file channel to access the file.
     *
     * <p> An invocation of this method behaves in exactly the same way as the
     * invocation
     * <pre>
     *     ch.{@link #open(Path, Set, ExecutorService, FileAttribute[])
     *       open}(file, opts, null, new FileAttribute&lt;?&gt;[0]);
     * </pre>
     * where {@code opts} is a {@code Set} containing the options specified to
     * this method.
     *
     * <p> The resulting channel is associated with default thread pool to which
     * tasks are submitted to handle I/O events and dispatch to completion
     * handlers that consume the result of asynchronous operations performed on
     * the resulting channel.
     *
     * @param file    The path of the file to open or create
     * @param options Options specifying how the file is opened
     *
     * @return A new asynchronous file channel
     *
     * @throws IllegalArgumentException      If the set contains an invalid combination of options
     * @throws UnsupportedOperationException If the {@code file} is associated with a provider that does not
     *                                       support creating file channels, or an unsupported open option is
     *                                       specified
     * @throws IOException                   If an I/O error occurs
     * @throws SecurityException             If a security manager is installed and it denies an
     *                                       unspecified permission required by the implementation.
     *                                       In the case of the default provider, the {@link
     *                                       SecurityManager#checkRead(String)} method is invoked to check
     *                                       read access if the file is opened for reading. The {@link
     *                                       SecurityManager#checkWrite(String)} method is invoked to check
     *                                       write access if the file is opened for writing
     */
    // 创建/打开一个文件，并返回其关联的异步文件通道，工作线程在这个过程中会被启动并阻塞
    public static AsynchronousFileChannel open(Path file, OpenOption... options) throws IOException {
        Set<OpenOption> set;
        
        if(options.length == 0) {
            set = Collections.emptySet();
        } else {
            set = new HashSet<>();
            Collections.addAll(set, options);
        }
        
        return open(file, set, null, NO_ATTRIBUTES);
    }
    
    /**
     * Opens or creates a file for reading and/or writing, returning an
     * asynchronous file channel to access the file.
     *
     * <p> The {@code options} parameter determines how the file is opened.
     * The {@link StandardOpenOption#READ READ} and {@link StandardOpenOption#WRITE
     * WRITE} options determines if the file should be opened for reading and/or
     * writing. If neither option is contained in the array then an existing file
     * is opened for  reading.
     *
     * <p> In addition to {@code READ} and {@code WRITE}, the following options
     * may be present:
     *
     * <table class="striped">
     * <caption style="display:none">additional options</caption>
     * <thead>
     * <tr> <th scope="col">Option</th> <th scope="col">Description</th> </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <th scope="row"> {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} </th>
     *   <td> When opening an existing file, the file is first truncated to a
     *   size of 0 bytes. This option is ignored when the file is opened only
     *   for reading.</td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link StandardOpenOption#CREATE_NEW CREATE_NEW} </th>
     *   <td> If this option is present then a new file is created, failing if
     *   the file already exists. When creating a file the check for the
     *   existence of the file and the creation of the file if it does not exist
     *   is atomic with respect to other file system operations. This option is
     *   ignored when the file is opened only for reading. </td>
     * </tr>
     * <tr>
     *   <th scope="row" > {@link StandardOpenOption#CREATE CREATE} </th>
     *   <td> If this option is present then an existing file is opened if it
     *   exists, otherwise a new file is created. When creating a file the check
     *   for the existence of the file and the creation of the file if it does
     *   not exist is atomic with respect to other file system operations. This
     *   option is ignored if the {@code CREATE_NEW} option is also present or
     *   the file is opened only for reading. </td>
     * </tr>
     * <tr>
     *   <th scope="row" > {@link StandardOpenOption#DELETE_ON_CLOSE DELETE_ON_CLOSE} </th>
     *   <td> When this option is present then the implementation makes a
     *   <em>best effort</em> attempt to delete the file when closed by
     *   the {@link #close close} method. If the {@code close} method is not
     *   invoked then a <em>best effort</em> attempt is made to delete the file
     *   when the Java virtual machine terminates. </td>
     * </tr>
     * <tr>
     *   <th scope="row">{@link StandardOpenOption#SPARSE SPARSE} </th>
     *   <td> When creating a new file this option is a <em>hint</em> that the
     *   new file will be sparse. This option is ignored when not creating
     *   a new file. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link StandardOpenOption#SYNC SYNC} </th>
     *   <td> Requires that every update to the file's content or metadata be
     *   written synchronously to the underlying storage device. (see <a
     *   href="../file/package-summary.html#integrity"> Synchronized I/O file
     *   integrity</a>). </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link StandardOpenOption#DSYNC DSYNC} </th>
     *   <td> Requires that every update to the file's content be written
     *   synchronously to the underlying storage device. (see <a
     *   href="../file/package-summary.html#integrity"> Synchronized I/O file
     *   integrity</a>). </td>
     * </tr>
     * </tbody>
     * </table>
     *
     * <p> An implementation may also support additional options.
     *
     * <p> The {@code executor} parameter is the {@link ExecutorService} to
     * which tasks are submitted to handle I/O events and dispatch completion
     * results for operations initiated on resulting channel.
     * The nature of these tasks is highly implementation specific and so care
     * should be taken when configuring the {@code Executor}. Minimally it
     * should support an unbounded work queue and should not run tasks on the
     * caller thread of the {@link ExecutorService#execute execute} method.
     * Shutting down the executor service while the channel is open results in
     * unspecified behavior.
     *
     * <p> The {@code attrs} parameter is an optional array of file {@link
     * FileAttribute file-attributes} to set atomically when creating the file.
     *
     * <p> The new channel is created by invoking the {@link
     * FileSystemProvider#newFileChannel newFileChannel} method on the
     * provider that created the {@code Path}.
     *
     * @param file     The path of the file to open or create
     * @param options  Options specifying how the file is opened
     * @param executor The thread pool or {@code null} to associate the channel with
     *                 the default thread pool
     * @param attrs    An optional list of file attributes to set atomically when
     *                 creating the file
     *
     * @return A new asynchronous file channel
     *
     * @throws IllegalArgumentException      If the set contains an invalid combination of options
     * @throws UnsupportedOperationException If the {@code file} is associated with a provider that does not
     *                                       support creating asynchronous file channels, or an unsupported
     *                                       open option is specified, or the array contains an attribute that
     *                                       cannot be set atomically when creating the file
     * @throws IOException                   If an I/O error occurs
     * @throws SecurityException             If a security manager is installed and it denies an
     *                                       unspecified permission required by the implementation.
     *                                       In the case of the default provider, the {@link
     *                                       SecurityManager#checkRead(String)} method is invoked to check
     *                                       read access if the file is opened for reading. The {@link
     *                                       SecurityManager#checkWrite(String)} method is invoked to check
     *                                       write access if the file is opened for writing
     */
    /*
     * 创建/打开一个文件，并返回其关联的异步文件通道，工作线程在这个过程中会被启动并阻塞
     *
     * path    : 文件路径
     * options : 文件操作属性
     * executor: 工作线程的线程池
     * attrs   : 文件权限属性，参见Windows安全描述符
     *
     * 注：attrs是文件权限属性，允许为空数组(默认)，但不能为null，其实现依平台实现而定：
     * windows  : 要求attrs的name()方法返回"acl:acl"，且value()方法返回List<AclEntry>类型的对象
     * linux/mac: 要求attrs的name()方法返回"posix:permissions"或"unix:permissions"，且value()方法返回Set<PosixFilePermission>类型的对象
     */
    public static AsynchronousFileChannel open(Path path, Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
        // 获取当前的文件系统工厂
        FileSystemProvider provider = path.getFileSystem().provider();
        return provider.newAsynchronousFileChannel(path, options, executor, attrs);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads a sequence of bytes from this channel into the given buffer,
     * starting at the given file position.
     *
     * <p> This method initiates the reading of a sequence of bytes from this
     * channel into the given buffer, starting at the given file position. This
     * method returns a {@code Future} representing the pending result of the
     * operation. The {@code Future}'s {@link Future#get() get} method returns
     * the number of bytes read or {@code -1} if the given position is greater
     * than or equal to the file's size at the time that the read is attempted.
     *
     * <p> This method works in the same manner as the {@link
     * AsynchronousByteChannel#read(ByteBuffer)} method, except that bytes are
     * read starting at the given file position. If the given file position is
     * greater than the file's size at the time that the read is attempted then
     * no bytes are read.
     *
     * @param dst      The buffer into which bytes are to be transferred
     * @param position The file position at which the transfer is to begin;
     *                 must be non-negative
     *
     * @return A {@code Future} object representing the pending result
     *
     * @throws IllegalArgumentException    If the position is negative or the buffer is read-only
     * @throws NonReadableChannelException If this channel was not opened for reading
     */
    /*
     * 从当前通道的position位置处读取数据以填充缓冲区dst（读取的字节数量最多填满缓冲区的剩余空间）
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否读取完成，以及获取实际读取到的字节数
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    public abstract Future<Integer> read(ByteBuffer dst, long position);
    
    /**
     * Reads a sequence of bytes from this channel into the given buffer,
     * starting at the given file position.
     *
     * <p> This method initiates the reading of a sequence of bytes from this
     * channel into the given buffer, starting at the given file position. The
     * result of the read is the number of bytes read or {@code -1} if the given
     * position is greater than or equal to the file's size at the time that the
     * read is attempted.
     *
     * <p> This method works in the same manner as the {@link
     * AsynchronousByteChannel#read(ByteBuffer, Object, CompletionHandler)}
     * method, except that bytes are read starting at the given file position.
     * If the given file position is greater than the file's size at the time
     * that the read is attempted then no bytes are read.
     *
     * @param <A>        The type of the attachment
     * @param dst        The buffer into which bytes are to be transferred
     * @param position   The file position at which the transfer is to begin;
     *                   must be non-negative
     * @param attachment The object to attach to the I/O operation; can be {@code null}
     * @param handler    The handler for consuming the result
     *
     * @throws IllegalArgumentException    If the position is negative or the buffer is read-only
     * @throws NonReadableChannelException If this channel was not opened for reading
     */
    /*
     * 从当前通道的position位置处读取数据以填充缓冲区dst（读取的字节数量最多填满缓冲区的剩余空间）
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际读到的字节数
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    public abstract <A> void read(ByteBuffer dst, long position, A attachment, CompletionHandler<Integer, ? super A> handler);
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes a sequence of bytes to this channel from the given buffer, starting
     * at the given file position.
     *
     * <p> This method initiates the writing of a sequence of bytes to this
     * channel from the given buffer, starting at the given file position. The
     * method returns a {@code Future} representing the pending result of the
     * write operation. The {@code Future}'s {@link Future#get() get} method
     * returns the number of bytes written.
     *
     * <p> This method works in the same manner as the {@link
     * AsynchronousByteChannel#write(ByteBuffer)} method, except that bytes are
     * written starting at the given file position. If the given position is
     * greater than the file's size, at the time that the write is attempted,
     * then the file will be grown to accommodate the new bytes; the values of
     * any bytes between the previous end-of-file and the newly-written bytes
     * are unspecified.
     *
     * @param src      The buffer from which bytes are to be transferred
     * @param position The file position at which the transfer is to begin;
     *                 must be non-negative
     *
     * @return A {@code Future} object representing the pending result
     *
     * @throws IllegalArgumentException    If the position is negative
     * @throws NonWritableChannelException If this channel was not opened for writing
     */
    /*
     * 从源缓冲区src中读取数据，并将读到的内容写入到当前通道中，position代表起始写入位置
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否写入完成，以及获取实际写入的字节数
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    public abstract Future<Integer> write(ByteBuffer src, long position);
    
    /**
     * Writes a sequence of bytes to this channel from the given buffer, starting
     * at the given file position.
     *
     * <p> This method works in the same manner as the {@link
     * AsynchronousByteChannel#write(ByteBuffer, Object, CompletionHandler)}
     * method, except that bytes are written starting at the given file position.
     * If the given position is greater than the file's size, at the time that
     * the write is attempted, then the file will be grown to accommodate the new
     * bytes; the values of any bytes between the previous end-of-file and the
     * newly-written bytes are unspecified.
     *
     * @param <A>        The type of the attachment
     * @param src        The buffer from which bytes are to be transferred
     * @param position   The file position at which the transfer is to begin;
     *                   must be non-negative
     * @param attachment The object to attach to the I/O operation; can be {@code null}
     * @param handler    The handler for consuming the result
     *
     * @throws IllegalArgumentException    If the position is negative
     * @throws NonWritableChannelException If this channel was not opened for writing
     */
    /*
     * 从源缓冲区src中读取数据，并将读到的内容写入到当前通道中，position代表起始写入位置
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际写入的字节数
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    public abstract <A> void write(ByteBuffer src, long position, A attachment, CompletionHandler<Integer, ? super A> handler);
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Acquires an exclusive lock on this channel's file.
     *
     * <p> This method initiates an operation to acquire an exclusive lock on this
     * channel's file. The method returns a {@code Future} representing the
     * pending result of the operation. The {@code Future}'s {@link Future#get()
     * get} method returns the {@link FileLock} on successful completion.
     *
     * <p> An invocation of this method behaves in exactly the same way as the
     * invocation
     * <pre>
     *     ch.{@link #lock(long, long, boolean) lock}(0L, Long.MAX_VALUE, false)
     * </pre>
     *
     * @return a {@code Future} object representing the pending result
     *
     * @throws OverlappingFileLockException If a lock is already held by this Java virtual machine, or there
     *                                      is already a pending attempt to lock a region
     * @throws NonWritableChannelException  If this channel was not opened for writing
     */
    /*
     * 对当前通道文件进行加锁，加锁范围是整个文件，申请的是独占锁
     * 返回值一个包含IO操作结果的Future，主线程轮询此Future以判断是否加锁成功，以及获取到申请到的文件锁
     *
     * 注：此IO操作的结果是成功申请到的文件锁。如果IO操作没成效，则执行结果可以是异常。
     */
    public final Future<FileLock> lock() {
        return lock(0L, Long.MAX_VALUE, false);
    }
    
    /**
     * Acquires a lock on the given region of this channel's file.
     *
     * <p> This method initiates an operation to acquire a lock on the given
     * region of this channel's file.  The method behaves in exactly the same
     * manner as the {@link #lock(long, long, boolean, Object, CompletionHandler)}
     * method except that instead of specifying a completion handler, this
     * method returns a {@code Future} representing the pending result. The
     * {@code Future}'s {@link Future#get() get} method returns the {@link
     * FileLock} on successful completion.
     *
     * @param position The position at which the locked region is to start; must be
     *                 non-negative
     * @param size     The size of the locked region; must be non-negative, and the sum
     *                 {@code position}&nbsp;+&nbsp;{@code size} must be non-negative
     * @param shared   {@code true} to request a shared lock, in which case this
     *                 channel must be open for reading (and possibly writing);
     *                 {@code false} to request an exclusive lock, in which case this
     *                 channel must be open for writing (and possibly reading)
     *
     * @return a {@code Future} object representing the pending result
     *
     * @throws OverlappingFileLockException If a lock is already held by this Java virtual machine, or there
     *                                      is already a pending attempt to lock a region
     * @throws IllegalArgumentException     If the preconditions on the parameters do not hold
     * @throws NonReadableChannelException  If {@code shared} is true but this channel was not opened for reading
     * @throws NonWritableChannelException  If {@code shared} is false but this channel was not opened for writing
     */
    /*
     * 对当前通道文件进行加锁(文件锁)
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否加锁成功，以及获取到申请到的文件锁
     *
     * 注：此IO操作的结果是成功申请到的文件锁。如果IO操作没成效，则执行结果可以是异常。
     */
    public abstract Future<FileLock> lock(long position, long size, boolean shared);
    
    /**
     * Acquires an exclusive lock on this channel's file.
     *
     * <p> This method initiates an operation to acquire a lock on the given
     * region of this channel's file. The {@code handler} parameter is a
     * completion handler that is invoked when the lock is acquired (or the
     * operation fails). The result passed to the completion handler is the
     * resulting {@code FileLock}.
     *
     * <p> An invocation of this method of the form {@code ch.lock(att,handler)}
     * behaves in exactly the same way as the invocation
     * <pre>
     *     ch.{@link #lock(long, long, boolean, Object, CompletionHandler) lock}(0L, Long.MAX_VALUE, false, att, handler)
     * </pre>
     *
     * @param <A>        The type of the attachment
     * @param attachment The object to attach to the I/O operation; can be {@code null}
     * @param handler    The handler for consuming the result
     *
     * @throws OverlappingFileLockException If a lock is already held by this Java virtual machine, or there
     *                                      is already a pending attempt to lock a region
     * @throws NonWritableChannelException  If this channel was not opened for writing
     */
    /*
     * 对当前通道文件进行加锁，加锁范围是整个文件，申请的是独占锁
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际写入的字节数
     *
     * 注：此IO操作的结果是成功申请到的文件锁。如果IO操作没成效，则执行结果可以是异常。
     */
    public final <A> void lock(A attachment, CompletionHandler<FileLock, ? super A> handler) {
        lock(0L, Long.MAX_VALUE, false, attachment, handler);
    }
    
    /**
     * Acquires a lock on the given region of this channel's file.
     *
     * <p> This method initiates an operation to acquire a lock on the given
     * region of this channel's file. The {@code handler} parameter is a
     * completion handler that is invoked when the lock is acquired (or the
     * operation fails). The result passed to the completion handler is the
     * resulting {@code FileLock}.
     *
     * <p> The region specified by the {@code position} and {@code size}
     * parameters need not be contained within, or even overlap, the actual
     * underlying file.  Lock regions are fixed in size; if a locked region
     * initially contains the end of the file and the file grows beyond the
     * region then the new portion of the file will not be covered by the lock.
     * If a file is expected to grow in size and a lock on the entire file is
     * required then a region starting at zero, and no smaller than the
     * expected maximum size of the file, should be locked.  The two-argument
     * {@link #lock(Object, CompletionHandler)} method simply locks a region
     * of size {@link Long#MAX_VALUE}. If a lock that overlaps the requested
     * region is already held by this Java virtual machine, or this method has
     * been invoked to lock an overlapping region and that operation has not
     * completed, then this method throws {@link OverlappingFileLockException}.
     *
     * <p> Some operating systems do not support a mechanism to acquire a file
     * lock in an asynchronous manner. Consequently an implementation may
     * acquire the file lock in a background thread or from a task executed by
     * a thread in the associated thread pool. If there are many lock operations
     * outstanding then it may consume threads in the Java virtual machine for
     * indefinite periods.
     *
     * <p> Some operating systems do not support shared locks, in which case a
     * request for a shared lock is automatically converted into a request for
     * an exclusive lock.  Whether the newly-acquired lock is shared or
     * exclusive may be tested by invoking the resulting lock object's {@link
     * FileLock#isShared() isShared} method.
     *
     * <p> File locks are held on behalf of the entire Java virtual machine.
     * They are not suitable for controlling access to a file by multiple
     * threads within the same virtual machine.
     *
     * @param <A>        The type of the attachment
     * @param position   The position at which the locked region is to start; must be
     *                   non-negative
     * @param size       The size of the locked region; must be non-negative, and the sum
     *                   {@code position}&nbsp;+&nbsp;{@code size} must be non-negative
     * @param shared     {@code true} to request a shared lock, in which case this
     *                   channel must be open for reading (and possibly writing);
     *                   {@code false} to request an exclusive lock, in which case this
     *                   channel must be open for writing (and possibly reading)
     * @param attachment The object to attach to the I/O operation; can be {@code null}
     * @param handler    The handler for consuming the result
     *
     * @throws OverlappingFileLockException If a lock that overlaps the requested region is already held by
     *                                      this Java virtual machine, or there is already a pending attempt
     *                                      to lock an overlapping region
     * @throws IllegalArgumentException     If the preconditions on the parameters do not hold
     * @throws NonReadableChannelException  If {@code shared} is true but this channel was not opened for reading
     * @throws NonWritableChannelException  If {@code shared} is false but this channel was not opened for writing
     */
    /*
     * 对当前通道文件进行加锁(文件锁)
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际写入的字节数
     *
     * 注：此IO操作的结果是成功申请到的文件锁。如果IO操作没成效，则执行结果可以是异常。
     */
    public abstract <A> void lock(long position, long size, boolean shared, A attachment, CompletionHandler<FileLock, ? super A> handler);
    
    
    /**
     * Attempts to acquire an exclusive lock on this channel's file.
     *
     * <p> An invocation of this method of the form {@code ch.tryLock()}
     * behaves in exactly the same way as the invocation
     *
     * <pre>
     *     ch.{@link #tryLock(long, long, boolean) tryLock}(0L, Long.MAX_VALUE, false) </pre>
     *
     * @return A lock object representing the newly-acquired lock,
     * or {@code null} if the lock could not be acquired
     * because another program holds an overlapping lock
     *
     * @throws ClosedChannelException       If this channel is closed
     * @throws OverlappingFileLockException If a lock that overlaps the requested region is already held by
     *                                      this Java virtual machine, or if another thread is already
     *                                      blocked in this method and is attempting to lock an overlapping
     *                                      region
     * @throws NonWritableChannelException  If {@code shared} is false but this channel was not opened for writing
     * @throws IOException                  If some other I/O error occurs
     * @see #lock(Object, CompletionHandler)
     * @see #lock(long, long, boolean, Object, CompletionHandler)
     * @see #tryLock(long, long, boolean)
     */
    // 尝试对整个通道文件进行加锁，申请的是独占锁；如果文件锁申请失败了，则返回null
    public final FileLock tryLock() throws IOException {
        return tryLock(0L, Long.MAX_VALUE, false);
    }
    
    /**
     * Attempts to acquire a lock on the given region of this channel's file.
     *
     * <p> This method does not block. An invocation always returns immediately,
     * either having acquired a lock on the requested region or having failed to
     * do so.  If it fails to acquire a lock because an overlapping lock is held
     * by another program then it returns {@code null}.  If it fails to acquire
     * a lock for any other reason then an appropriate exception is thrown.
     *
     * @param position The position at which the locked region is to start; must be
     *                 non-negative
     * @param size     The size of the locked region; must be non-negative, and the sum
     *                 {@code position}&nbsp;+&nbsp;{@code size} must be non-negative
     * @param shared   {@code true} to request a shared lock,
     *                 {@code false} to request an exclusive lock
     *
     * @return A lock object representing the newly-acquired lock,
     * or {@code null} if the lock could not be acquired
     * because another program holds an overlapping lock
     *
     * @throws IllegalArgumentException     If the preconditions on the parameters do not hold
     * @throws ClosedChannelException       If this channel is closed
     * @throws OverlappingFileLockException If a lock that overlaps the requested region is already held by
     *                                      this Java virtual machine, or if another thread is already
     *                                      blocked in this method and is attempting to lock an overlapping
     *                                      region of the same file
     * @throws NonReadableChannelException  If {@code shared} is true but this channel was not opened for reading
     * @throws NonWritableChannelException  If {@code shared} is false but this channel was not opened for writing
     * @throws IOException                  If some other I/O error occurs
     * @see #lock(Object, CompletionHandler)
     * @see #lock(long, long, boolean, Object, CompletionHandler)
     * @see #tryLock()
     */
    // 尝试对指定区域的文件通道进行加锁；如果文件锁申请失败了，则返回null
    public abstract FileLock tryLock(long position, long size, boolean shared) throws IOException;
    
    /*▲ 文件锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current size of this channel's file.
     *
     * @return The current size of this channel's file, measured in bytes
     *
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    // 返回当前通道文件的尺寸
    public abstract long size() throws IOException;
    
    /**
     * Truncates this channel's file to the given size.
     *
     * <p> If the given size is less than the file's current size then the file
     * is truncated, discarding any bytes beyond the new end of the file.  If
     * the given size is greater than or equal to the file's current size then
     * the file is not modified. </p>
     *
     * @param size The new size, a non-negative byte count
     *
     * @return This file channel
     *
     * @throws NonWritableChannelException If this channel was not opened for writing
     * @throws ClosedChannelException      If this channel is closed
     * @throws IllegalArgumentException    If the new size is negative
     * @throws IOException                 If some other I/O error occurs
     */
    // 用新尺寸size截短通道(文件)；如果新尺寸比当前通道文件尺寸还大，则无操作
    public abstract AsynchronousFileChannel truncate(long size) throws IOException;
    
    /**
     * Forces any updates to this channel's file to be written to the storage
     * device that contains it.
     *
     * <p> If this channel's file resides on a local storage device then when
     * this method returns it is guaranteed that all changes made to the file
     * since this channel was created, or since this method was last invoked,
     * will have been written to that device.  This is useful for ensuring that
     * critical information is not lost in the event of a system crash.
     *
     * <p> If the file does not reside on a local device then no such guarantee
     * is made.
     *
     * <p> The {@code metaData} parameter can be used to limit the number of
     * I/O operations that this method is required to perform.  Passing
     * {@code false} for this parameter indicates that only updates to the
     * file's content need be written to storage; passing {@code true}
     * indicates that updates to both the file's content and metadata must be
     * written, which generally requires at least one more I/O operation.
     * Whether this parameter actually has any effect is dependent upon the
     * underlying operating system and is therefore unspecified.
     *
     * <p> Invoking this method may cause an I/O operation to occur even if the
     * channel was only opened for reading.  Some operating systems, for
     * example, maintain a last-access time as part of a file's metadata, and
     * this time is updated whenever the file is read.  Whether or not this is
     * actually done is system-dependent and is therefore unspecified.
     *
     * <p> This method is only guaranteed to force changes that were made to
     * this channel's file via the methods defined in this class.
     *
     * @param metaData If {@code true} then this method is required to force changes
     *                 to both the file's content and metadata to be written to
     *                 storage; otherwise, it need only force content changes to be
     *                 written
     *
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    // 是否需要实时更新文件的元数据到本地
    public abstract void force(boolean metaData) throws IOException;
    
    /*▲ 杂项  ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
