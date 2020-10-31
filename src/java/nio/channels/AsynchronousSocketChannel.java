/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An asynchronous channel for stream-oriented connecting sockets.
 *
 * <p> Asynchronous socket channels are created in one of two ways. A newly-created
 * {@code AsynchronousSocketChannel} is created by invoking one of the {@link
 * #open open} methods defined by this class. A newly-created channel is open but
 * not yet connected. A connected {@code AsynchronousSocketChannel} is created
 * when a connection is made to the socket of an {@link AsynchronousServerSocketChannel}.
 * It is not possible to create an asynchronous socket channel for an arbitrary,
 * pre-existing {@link java.net.Socket socket}.
 *
 * <p> A newly-created channel is connected by invoking its {@link #connect connect}
 * method; once connected, a channel remains connected until it is closed.  Whether
 * or not a socket channel is connected may be determined by invoking its {@link
 * #getRemoteAddress getRemoteAddress} method. An attempt to invoke an I/O
 * operation upon an unconnected channel will cause a {@link NotYetConnectedException}
 * to be thrown.
 *
 * <p> Channels of this type are safe for use by multiple concurrent threads.
 * They support concurrent reading and writing, though at most one read operation
 * and one write operation can be outstanding at any time.
 * If a thread initiates a read operation before a previous read operation has
 * completed then a {@link ReadPendingException} will be thrown. Similarly, an
 * attempt to initiate a write operation before a previous write has completed
 * will throw a {@link WritePendingException}.
 *
 * <p> Socket options are configured using the {@link #setOption(SocketOption,Object)
 * setOption} method. Asynchronous socket channels support the following options:
 * <blockquote>
 * <table class="striped">
 * <caption style="display:none">Socket options</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Option Name</th>
 *     <th scope="col">Description</th>
 *   </tr>
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_SNDBUF SO_SNDBUF} </th>
 *     <td> The size of the socket send buffer </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_RCVBUF SO_RCVBUF} </th>
 *     <td> The size of the socket receive buffer </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_KEEPALIVE SO_KEEPALIVE} </th>
 *     <td> Keep connection alive </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_REUSEADDR SO_REUSEADDR} </th>
 *     <td> Re-use address </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#TCP_NODELAY TCP_NODELAY} </th>
 *     <td> Disable the Nagle algorithm </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 * Additional (implementation specific) options may also be supported.
 *
 * <h2>Timeouts</h2>
 *
 * <p> The {@link #read(ByteBuffer,long,TimeUnit,Object,CompletionHandler) read}
 * and {@link #write(ByteBuffer,long,TimeUnit,Object,CompletionHandler) write}
 * methods defined by this class allow a timeout to be specified when initiating
 * a read or write operation. If the timeout elapses before an operation completes
 * then the operation completes with the exception {@link
 * InterruptedByTimeoutException}. A timeout may leave the channel, or the
 * underlying connection, in an inconsistent state. Where the implementation
 * cannot guarantee that bytes have not been read from the channel then it puts
 * the channel into an implementation specific <em>error state</em>. A subsequent
 * attempt to initiate a {@code read} operation causes an unspecified runtime
 * exception to be thrown. Similarly if a {@code write} operation times out and
 * the implementation cannot guarantee bytes have not been written to the
 * channel then further attempts to {@code write} to the channel cause an
 * unspecified runtime exception to be thrown. When a timeout elapses then the
 * state of the {@link ByteBuffer}, or the sequence of buffers, for the I/O
 * operation is not defined. Buffers should be discarded or at least care must
 * be taken to ensure that the buffers are not accessed while the channel remains
 * open. All methods that accept timeout parameters treat values less than or
 * equal to zero to mean that the I/O operation does not timeout.
 *
 * @since 1.7
 */
// 异步Socket通道
public abstract class AsynchronousSocketChannel implements AsynchronousByteChannel, NetworkChannel {
    
    private final AsynchronousChannelProvider provider;     // 异步Socket通道工厂
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class.
     *
     * @param provider The provider that created this channel
     */
    protected AsynchronousSocketChannel(AsynchronousChannelProvider provider) {
        this.provider = provider;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens an asynchronous socket channel.
     *
     * <p> This method returns an asynchronous socket channel that is bound to
     * the <em>default group</em>.This method is equivalent to evaluating the
     * expression:
     * <blockquote><pre>
     * open((AsynchronousChannelGroup)null);
     * </pre></blockquote>
     *
     * @return A new asynchronous socket channel
     *
     * @throws IOException If an I/O error occurs
     */
    /*
     * 创建并返回一个异步Socket通道
     *
     * 这里使用了默认的异步Socket通道工厂，在使用该工厂创建异步Socket通道时，会自动启动工作线程。
     */
    public static AsynchronousSocketChannel open() throws IOException {
        return open(null);
    }
    
    /**
     * Opens an asynchronous socket channel.
     *
     * <p> The new channel is created by invoking the {@link
     * AsynchronousChannelProvider#openAsynchronousSocketChannel
     * openAsynchronousSocketChannel} method on the {@link
     * AsynchronousChannelProvider} that created the group. If the group parameter
     * is {@code null} then the resulting channel is created by the system-wide
     * default provider, and bound to the <em>default group</em>.
     *
     * @param group The group to which the newly constructed channel should be bound,
     *              or {@code null} for the default group
     *
     * @return A new asynchronous socket channel
     *
     * @throws ShutdownChannelGroupException If the channel group is shutdown
     * @throws IOException                   If an I/O error occurs
     */
    /*
     * 创建并返回一个异步Socket通道，group是该通道关联的异步通道组
     *
     * 如果group为null，则使用默认的异步Socket通道工厂；
     * 如果group不为null，则应当使用group中提供的异步Socket通道工厂。
     *
     * 在使用异步Socket通道工厂创建异步Socket通道时，应当启动工作线程。
     */
    public static AsynchronousSocketChannel open(AsynchronousChannelGroup group) throws IOException {
        AsynchronousChannelProvider provider = (group == null) ? AsynchronousChannelProvider.provider() : group.provider();
        return provider.openAsynchronousSocketChannel(group);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws ConnectionPendingException      If a connection operation is already in progress on this channel
     * @throws AlreadyBoundException           {@inheritDoc}
     * @throws UnsupportedAddressTypeException {@inheritDoc}
     * @throws ClosedChannelException          {@inheritDoc}
     * @throws IOException                     {@inheritDoc}
     * @throws SecurityException               If a security manager has been installed and its
     *                                         {@link SecurityManager#checkListen checkListen} method denies
     *                                         the operation
     */
    // 对[客户端Socket]执行【bind】操作；local为null时，使用通配IP和随机端口
    @Override
    public abstract AsynchronousSocketChannel bind(SocketAddress local) throws IOException;
    
    /**
     * Connects this channel.
     *
     * <p> This method initiates an operation to connect this channel. This
     * method behaves in exactly the same manner as the {@link
     * #connect(SocketAddress, Object, CompletionHandler)} method except that
     * instead of specifying a completion handler, this method returns a {@code
     * Future} representing the pending result. The {@code Future}'s {@link
     * Future#get() get} method returns {@code null} on successful completion.
     *
     * @param remote The remote address to which this channel is to be connected
     *
     * @return A {@code Future} object representing the pending result
     *
     * @throws UnresolvedAddressException      If the given remote address is not fully resolved
     * @throws UnsupportedAddressTypeException If the type of the given remote address is not supported
     * @throws AlreadyConnectedException       If this channel is already connected
     * @throws ConnectionPendingException      If a connection operation is already in progress on this channel
     * @throws SecurityException               If a security manager has been installed
     *                                         and it does not permit access to the given remote endpoint
     */
    /*
     * 对[客户端Socket]执行【connect】操作，以便连接到远端Socket；
     * 返回值是一个包含Void的Future，主线程轮询此Future以判断是否accept完成。
     *
     * 注：这里的返回值包装Void的原因是connect操作本来就没有返回值，Void在这里只是用来占位。
     * 　　又由于需要一个判断异步IO操作是否完成的机制，所以引入了Future。
     */
    public abstract Future<Void> connect(SocketAddress remote);
    
    /**
     * Connects this channel.
     *
     * <p> This method initiates an operation to connect this channel. The
     * {@code handler} parameter is a completion handler that is invoked when
     * the connection is successfully established or connection cannot be
     * established. If the connection cannot be established then the channel is
     * closed.
     *
     * <p> This method performs exactly the same security checks as the {@link
     * java.net.Socket} class.  That is, if a security manager has been
     * installed then this method verifies that its {@link
     * java.lang.SecurityManager#checkConnect checkConnect} method permits
     * connecting to the address and port number of the given remote endpoint.
     *
     * @param <A>        The type of the attachment
     * @param remote     The remote address to which this channel is to be connected
     * @param attachment The object to attach to the I/O operation; can be {@code null}
     * @param handler    The handler for consuming the result
     *
     * @throws UnresolvedAddressException      If the given remote address is not fully resolved
     * @throws UnsupportedAddressTypeException If the type of the given remote address is not supported
     * @throws AlreadyConnectedException       If this channel is already connected
     * @throws ConnectionPendingException      If a connection operation is already in progress on this channel
     * @throws ShutdownChannelGroupException   If the channel group has terminated
     * @throws SecurityException               If a security manager has been installed
     *                                         and it does not permit access to the given remote endpoint
     * @see #getRemoteAddress
     */
    /*
     * 对[客户端Socket]执行【connect】操作，以便连接到远端Socket；
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程。
     *
     * 注：这里的回调句柄包装Void的原因是connect操作本来就没有返回值，Void在这里只是用来占位。
     * 　　又由于需要一个回调机制来向主线程反馈任务执行结果，所以引入了CompletionHandler。
     */
    public abstract <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler);
    
    /*▲ socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取消/关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Shutdown the connection for reading without closing the channel.
     *
     * <p> Once shutdown for reading then further reads on the channel will
     * return {@code -1}, the end-of-stream indication. If the input side of the
     * connection is already shutdown then invoking this method has no effect.
     * The effect on an outstanding read operation is system dependent and
     * therefore not specified. The effect, if any, when there is data in the
     * socket receive buffer that has not been read, or data arrives subsequently,
     * is also system dependent.
     *
     * @return The channel
     *
     * @throws NotYetConnectedException If this channel is not yet connected
     * @throws ClosedChannelException   If this channel is closed
     * @throws IOException              If some other I/O error occurs
     */
    // 关闭从当前通道读取数据的功能
    public abstract AsynchronousSocketChannel shutdownInput() throws IOException;
    
    /**
     * Shutdown the connection for writing without closing the channel.
     *
     * <p> Once shutdown for writing then further attempts to write to the
     * channel will throw {@link ClosedChannelException}. If the output side of
     * the connection is already shutdown then invoking this method has no
     * effect. The effect on an outstanding write operation is system dependent
     * and therefore not specified.
     *
     * @return The channel
     *
     * @throws NotYetConnectedException If this channel is not yet connected
     * @throws ClosedChannelException   If this channel is closed
     * @throws IOException              If some other I/O error occurs
     */
    // 关闭向当前通道写入数据的功能
    public abstract AsynchronousSocketChannel shutdownOutput() throws IOException;
    
    /*▲ 取消/关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ReadPendingException     {@inheritDoc}
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    /*
     * 从当前通道读取数据并填充到缓冲区dst中（读取的字节数量最多填满缓冲区的剩余空间）
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否读取完成，以及获取实际读取到的字节数
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    @Override
    public abstract Future<Integer> read(ByteBuffer dst);
    
    /**
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws ReadPendingException          {@inheritDoc}
     * @throws NotYetConnectedException      If this channel is not yet connected
     * @throws ShutdownChannelGroupException If the channel group has terminated
     */
    /*
     * 从当前通道读取数据并填充到缓冲区dst中
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际读到的字节数
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    @Override
    public final <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
        read(dst, 0L, TimeUnit.MILLISECONDS, attachment, handler);
    }
    
    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p> This method initiates an asynchronous read operation to read a
     * sequence of bytes from this channel into the given buffer. The {@code
     * handler} parameter is a completion handler that is invoked when the read
     * operation completes (or fails). The result passed to the completion
     * handler is the number of bytes read or {@code -1} if no bytes could be
     * read because the channel has reached end-of-stream.
     *
     * <p> If a timeout is specified and the timeout elapses before the operation
     * completes then the operation completes with the exception {@link
     * InterruptedByTimeoutException}. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been read, or will not
     * be read from the channel into the given buffer, then further attempts to
     * read from the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * <p> Otherwise this method works in the same manner as the {@link
     * AsynchronousByteChannel#read(ByteBuffer, Object, CompletionHandler)}
     * method.
     *
     * @param <A>        The type of the attachment
     * @param dst        The buffer into which bytes are to be transferred
     * @param timeout    The maximum time for the I/O operation to complete
     * @param unit       The time unit of the {@code timeout} argument
     * @param attachment The object to attach to the I/O operation; can be {@code null}
     * @param handler    The handler for consuming the result
     *
     * @throws IllegalArgumentException      If the buffer is read-only
     * @throws ReadPendingException          If a read operation is already in progress on this channel
     * @throws NotYetConnectedException      If this channel is not yet connected
     * @throws ShutdownChannelGroupException If the channel group has terminated
     */
    /*
     * 从当前通道读取数据并填充到缓冲区dst中
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际读到的字节数
     * 允许设置超时时间，即在指定时间内没有完成读取操作的话，抛出异常
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    public abstract <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler);
    
    /**
     * Reads a sequence of bytes from this channel into a subsequence of the
     * given buffers. This operation, sometimes called a <em>scattering read</em>,
     * is often useful when implementing network protocols that group data into
     * segments consisting of one or more fixed-length headers followed by a
     * variable-length body. The {@code handler} parameter is a completion
     * handler that is invoked when the read operation completes (or fails). The
     * result passed to the completion handler is the number of bytes read or
     * {@code -1} if no bytes could be read because the channel has reached
     * end-of-stream.
     *
     * <p> This method initiates a read of up to <i>r</i> bytes from this channel,
     * where <i>r</i> is the total number of bytes remaining in the specified
     * subsequence of the given buffer array, that is,
     *
     * <blockquote><pre>
     * dsts[offset].remaining()
     *     + dsts[offset+1].remaining()
     *     + ... + dsts[offset+length-1].remaining()</pre></blockquote>
     *
     * at the moment that the read is attempted.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is read, where
     * {@code 0}&nbsp;{@code <}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;<i>r</i>.
     * Up to the first {@code dsts[offset].remaining()} bytes of this sequence
     * are transferred into buffer {@code dsts[offset]}, up to the next
     * {@code dsts[offset+1].remaining()} bytes are transferred into buffer
     * {@code dsts[offset+1]}, and so forth, until the entire byte sequence
     * is transferred into the given buffers.  As many bytes as possible are
     * transferred into each buffer, hence the final position of each updated
     * buffer, except the last updated buffer, is guaranteed to be equal to
     * that buffer's limit. The underlying operating system may impose a limit
     * on the number of buffers that may be used in an I/O operation. Where the
     * number of buffers (with bytes remaining), exceeds this limit, then the
     * I/O operation is performed with the maximum number of buffers allowed by
     * the operating system.
     *
     * <p> If a timeout is specified and the timeout elapses before the operation
     * completes then it completes with the exception {@link
     * InterruptedByTimeoutException}. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been read, or will not
     * be read from the channel into the given buffers, then further attempts to
     * read from the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * @param <A>        The type of the attachment
     * @param dsts       The buffers into which bytes are to be transferred
     * @param offset     The offset within the buffer array of the first buffer into which
     *                   bytes are to be transferred; must be non-negative and no larger than
     *                   {@code dsts.length}
     * @param length     The maximum number of buffers to be accessed; must be non-negative
     *                   and no larger than {@code dsts.length - offset}
     * @param timeout    The maximum time for the I/O operation to complete
     * @param unit       The time unit of the {@code timeout} argument
     * @param attachment The object to attach to the I/O operation; can be {@code null}
     * @param handler    The handler for consuming the result
     *
     * @throws IndexOutOfBoundsException     If the pre-conditions for the {@code offset}  and {@code length}
     *                                       parameter aren't met
     * @throws IllegalArgumentException      If the buffer is read-only
     * @throws ReadPendingException          If a read operation is already in progress on this channel
     * @throws NotYetConnectedException      If this channel is not yet connected
     * @throws ShutdownChannelGroupException If the channel group has terminated
     */
    /*
     * 从当前通道读取数据并填充到缓冲区组dsts中(填充到dsts中offset处起的length个缓冲区中)
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际读到的字节数
     * 允许设置超时时间，即在指定时间内没有完成读取操作的话，抛出异常
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    public abstract <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler);
    
    
    /**
     * @throws WritePendingException    {@inheritDoc}
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    /*
     * 从源缓冲区src中读取数据，并将读到的内容写入到当前通道中
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否写入完成，以及获取实际写入的字节数
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    @Override
    public abstract Future<Integer> write(ByteBuffer src);
    
    /**
     * @throws WritePendingException         {@inheritDoc}
     * @throws NotYetConnectedException      If this channel is not yet connected
     * @throws ShutdownChannelGroupException If the channel group has terminated
     */
    /*
     * 从源缓冲区src中读取数据，并将读到的内容写入到当前通道中
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际写入的字节数
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    @Override
    public final <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler) {
        write(src, 0L, TimeUnit.MILLISECONDS, attachment, handler);
    }
    
    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * <p> This method initiates an asynchronous write operation to write a
     * sequence of bytes to this channel from the given buffer. The {@code
     * handler} parameter is a completion handler that is invoked when the write
     * operation completes (or fails). The result passed to the completion
     * handler is the number of bytes written.
     *
     * <p> If a timeout is specified and the timeout elapses before the operation
     * completes then it completes with the exception {@link
     * InterruptedByTimeoutException}. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been written, or will
     * not be written to the channel from the given buffer, then further attempts
     * to write to the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * <p> Otherwise this method works in the same manner as the {@link
     * AsynchronousByteChannel#write(ByteBuffer, Object, CompletionHandler)}
     * method.
     *
     * @param <A>        The type of the attachment
     * @param src        The buffer from which bytes are to be retrieved
     * @param timeout    The maximum time for the I/O operation to complete
     * @param unit       The time unit of the {@code timeout} argument
     * @param attachment The object to attach to the I/O operation; can be {@code null}
     * @param handler    The handler for consuming the result
     *
     * @throws WritePendingException         If a write operation is already in progress on this channel
     * @throws NotYetConnectedException      If this channel is not yet connected
     * @throws ShutdownChannelGroupException If the channel group has terminated
     */
    /*
     * 从源缓冲区src中读取数据，并将读到的内容写入到当前通道中
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际写入的字节数
     * 允许设置超时时间，即在指定时间内没有完成写入操作的话，抛出异常
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    public abstract <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler);
    
    /**
     * Writes a sequence of bytes to this channel from a subsequence of the given
     * buffers. This operation, sometimes called a <em>gathering write</em>, is
     * often useful when implementing network protocols that group data into
     * segments consisting of one or more fixed-length headers followed by a
     * variable-length body. The {@code handler} parameter is a completion
     * handler that is invoked when the write operation completes (or fails).
     * The result passed to the completion handler is the number of bytes written.
     *
     * <p> This method initiates a write of up to <i>r</i> bytes to this channel,
     * where <i>r</i> is the total number of bytes remaining in the specified
     * subsequence of the given buffer array, that is,
     *
     * <blockquote><pre>
     * srcs[offset].remaining()
     *     + srcs[offset+1].remaining()
     *     + ... + srcs[offset+length-1].remaining()</pre></blockquote>
     *
     * at the moment that the write is attempted.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is written, where
     * {@code 0}&nbsp;{@code <}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;<i>r</i>.
     * Up to the first {@code srcs[offset].remaining()} bytes of this sequence
     * are written from buffer {@code srcs[offset]}, up to the next
     * {@code srcs[offset+1].remaining()} bytes are written from buffer
     * {@code srcs[offset+1]}, and so forth, until the entire byte sequence is
     * written.  As many bytes as possible are written from each buffer, hence
     * the final position of each updated buffer, except the last updated
     * buffer, is guaranteed to be equal to that buffer's limit. The underlying
     * operating system may impose a limit on the number of buffers that may be
     * used in an I/O operation. Where the number of buffers (with bytes
     * remaining), exceeds this limit, then the I/O operation is performed with
     * the maximum number of buffers allowed by the operating system.
     *
     * <p> If a timeout is specified and the timeout elapses before the operation
     * completes then it completes with the exception {@link
     * InterruptedByTimeoutException}. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been written, or will
     * not be written to the channel from the given buffers, then further attempts
     * to write to the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * @param <A>        The type of the attachment
     * @param srcs       The buffers from which bytes are to be retrieved
     * @param offset     The offset within the buffer array of the first buffer from which
     *                   bytes are to be retrieved; must be non-negative and no larger
     *                   than {@code srcs.length}
     * @param length     The maximum number of buffers to be accessed; must be non-negative
     *                   and no larger than {@code srcs.length - offset}
     * @param timeout    The maximum time for the I/O operation to complete
     * @param unit       The time unit of the {@code timeout} argument
     * @param attachment The object to attach to the I/O operation; can be {@code null}
     * @param handler    The handler for consuming the result
     *
     * @throws IndexOutOfBoundsException     If the pre-conditions for the {@code offset}  and {@code length}
     *                                       parameter aren't met
     * @throws WritePendingException         If a write operation is already in progress on this channel
     * @throws NotYetConnectedException      If this channel is not yet connected
     * @throws ShutdownChannelGroupException If the channel group has terminated
     */
    /*
     * 从源缓冲区组srcs中offset处起的length个缓冲区中读取数据，并将读到的内容写入到当前通道中
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际写入的字节数
     * 允许设置超时时间，即在指定时间内没有完成写入操作的话，抛出异常
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    public abstract <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler);
    
    /*▲ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     * <p>
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * a {@code SocketAddress} representing the
     * {@link java.net.InetAddress#getLoopbackAddress loopback} address and the
     * local port of the channel's socket is returned.
     *
     * @return The {@code SocketAddress} that the socket is bound to, or the
     * {@code SocketAddress} representing the loopback address if
     * denied by the security manager, or {@code null} if the
     * channel's socket is not bound
     *
     * @throws ClosedChannelException {@inheritDoc}
     * @throws IOException            {@inheritDoc}
     */
    // 获取绑定的本地地址
    public abstract SocketAddress getLocalAddress() throws IOException;
    
    /**
     * Returns the remote address to which this channel's socket is connected.
     *
     * <p> Where the channel is bound and connected to an Internet Protocol
     * socket address then the return value from this method is of type {@link
     * java.net.InetSocketAddress}.
     *
     * @return The remote address; {@code null} if the channel's socket is not
     * connected
     *
     * @throws ClosedChannelException If the channel is closed
     * @throws IOException            If an I/O error occurs
     */
    // 获取连接到的远程地址
    public abstract SocketAddress getRemoteAddress() throws IOException;
    
    /*▲ 地址信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ClosedChannelException   {@inheritDoc}
     * @throws IOException              {@inheritDoc}
     */
    // 设置指定名称的Socket配置参数
    @Override
    public abstract <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException;
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the provider that created this channel.
     *
     * @return The provider that created this channel
     */
    // 返回异步Socket通道工厂
    public final AsynchronousChannelProvider provider() {
        return provider;
    }
    
}
