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

package java.nio.channels;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * A selectable channel for datagram-oriented sockets.
 *
 * <p> A datagram channel is created by invoking one of the {@link #open open} methods
 * of this class. It is not possible to create a channel for an arbitrary,
 * pre-existing datagram socket. A newly-created datagram channel is open but not
 * connected. A datagram channel need not be connected in order for the {@link #send
 * send} and {@link #receive receive} methods to be used.  A datagram channel may be
 * connected, by invoking its {@link #connect connect} method, in order to
 * avoid the overhead of the security checks are otherwise performed as part of
 * every send and receive operation.  A datagram channel must be connected in
 * order to use the {@link #read(java.nio.ByteBuffer) read} and {@link
 * #write(java.nio.ByteBuffer) write} methods, since those methods do not
 * accept or return socket addresses.
 *
 * <p> Once connected, a datagram channel remains connected until it is
 * disconnected or closed.  Whether or not a datagram channel is connected may
 * be determined by invoking its {@link #isConnected isConnected} method.
 *
 * <p> Socket options are configured using the {@link #setOption(SocketOption,Object)
 * setOption} method. A datagram channel to an Internet Protocol socket supports
 * the following options:
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
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_REUSEADDR SO_REUSEADDR} </th>
 *     <td> Re-use address </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_BROADCAST SO_BROADCAST} </th>
 *     <td> Allow transmission of broadcast datagrams </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#IP_TOS IP_TOS} </th>
 *     <td> The Type of Service (ToS) octet in the Internet Protocol (IP) header </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#IP_MULTICAST_IF IP_MULTICAST_IF} </th>
 *     <td> The network interface for Internet Protocol (IP) multicast datagrams </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#IP_MULTICAST_TTL
 *       IP_MULTICAST_TTL} </th>
 *     <td> The <em>time-to-live</em> for Internet Protocol (IP) multicast
 *       datagrams </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#IP_MULTICAST_LOOP
 *       IP_MULTICAST_LOOP} </th>
 *     <td> Loopback for Internet Protocol (IP) multicast datagrams </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 * Additional (implementation specific) options may also be supported.
 *
 * <p> Datagram channels are safe for use by multiple concurrent threads.  They
 * support concurrent reading and writing, though at most one thread may be
 * reading and at most one thread may be writing at any given time.  </p>
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */
// UDP-Socket通道，用在客户端与服务端，支持在非阻塞模式下运行
public abstract class DatagramChannel extends AbstractSelectableChannel implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, MulticastChannel {
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class.
     *
     * @param provider The provider that created this channel
     */
    protected DatagramChannel(SelectorProvider provider) {
        super(provider);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens a datagram channel.
     *
     * <p> The new channel is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openDatagramChannel()
     * openDatagramChannel} method of the system-wide default {@link
     * java.nio.channels.spi.SelectorProvider} object.  The channel will not be
     * connected.
     *
     * <p> The {@link ProtocolFamily ProtocolFamily} of the channel's socket
     * is platform (and possibly configuration) dependent and therefore unspecified.
     * The {@link #open(ProtocolFamily) open} allows the protocol family to be
     * selected when opening a datagram channel, and should be used to open
     * datagram channels that are intended for Internet Protocol multicasting.
     *
     * @return A new datagram channel
     *
     * @throws IOException If an I/O error occurs
     */
    // 构造DatagramSocket通道，内部初始化了该Socket的文件描述符
    public static DatagramChannel open() throws IOException {
        return SelectorProvider.provider().openDatagramChannel();
    }
    
    /**
     * Opens a datagram channel.
     *
     * <p> The {@code family} parameter is used to specify the {@link
     * ProtocolFamily}. If the datagram channel is to be used for IP multicasting
     * then this should correspond to the address type of the multicast groups
     * that this channel will join.
     *
     * <p> The new channel is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openDatagramChannel(ProtocolFamily)
     * openDatagramChannel} method of the system-wide default {@link
     * java.nio.channels.spi.SelectorProvider} object.  The channel will not be
     * connected.
     *
     * @param family The protocol family
     *
     * @return A new datagram channel
     *
     * @throws UnsupportedOperationException If the specified protocol family is not supported. For example,
     *                                       suppose the parameter is specified as {@link
     *                                       java.net.StandardProtocolFamily#INET6 StandardProtocolFamily.INET6}
     *                                       but IPv6 is not enabled on the platform.
     * @throws IOException                   If an I/O error occurs
     * @since 1.7
     */
    // 构造DatagramSocket通道，内部初始化了该Socket的文件描述符，其支持的协议族由参数family指定
    public static DatagramChannel open(ProtocolFamily family) throws IOException {
        return SelectorProvider.provider().openDatagramChannel(family);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 适配 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves a datagram socket associated with this channel.
     *
     * <p> The returned object will not declare any public methods that are not
     * declared in the {@link java.net.DatagramSocket} class.  </p>
     *
     * @return A datagram socket associated with this channel
     */
    // 返回由当前UDP-Socket通道适配而成的UDP-Socket
    public abstract DatagramSocket socket();
    
    /*▲ 适配 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws AlreadyBoundException           {@inheritDoc}
     * @throws UnsupportedAddressTypeException {@inheritDoc}
     * @throws ClosedChannelException          {@inheritDoc}
     * @throws IOException                     {@inheritDoc}
     * @throws SecurityException               If a security manager has been installed and its {@link
     *                                         SecurityManager#checkListen checkListen} method denies the
     *                                         operation
     * @since 1.7
     */
    /*
     * 对UDP-Socket执行【bind】操作。
     *
     * bindpoint: 待绑定的地址(ip+port)；如果为null，则绑定到本地环回IP和随机端口
     */
    public abstract DatagramChannel bind(SocketAddress local) throws IOException;
    
    /**
     * Connects this channel's socket.
     *
     * <p> The channel's socket is configured so that it only receives
     * datagrams from, and sends datagrams to, the given remote <i>peer</i>
     * address.  Once connected, datagrams may not be received from or sent to
     * any other address.  A datagram socket remains connected until it is
     * explicitly disconnected or until it is closed.
     *
     * <p> This method performs exactly the same security checks as the {@link
     * java.net.DatagramSocket#connect connect} method of the {@link
     * java.net.DatagramSocket} class.  That is, if a security manager has been
     * installed then this method verifies that its {@link
     * java.lang.SecurityManager#checkAccept checkAccept} and {@link
     * java.lang.SecurityManager#checkConnect checkConnect} methods permit
     * datagrams to be received from and sent to, respectively, the given
     * remote address.
     *
     * <p> This method may be invoked at any time.  It will not have any effect
     * on read or write operations that are already in progress at the moment
     * that it is invoked. If this channel's socket is not bound then this method
     * will first cause the socket to be bound to an address that is assigned
     * automatically, as if invoking the {@link #bind bind} method with a
     * parameter of {@code null}. </p>
     *
     * @param remote The remote address to which this channel is to be connected
     *
     * @return This datagram channel
     *
     * @throws AlreadyConnectedException       If this channel is already connected
     * @throws ClosedChannelException          If this channel is closed
     * @throws AsynchronousCloseException      If another thread closes this channel
     *                                         while the connect operation is in progress
     * @throws ClosedByInterruptException      If another thread interrupts the current thread
     *                                         while the connect operation is in progress, thereby
     *                                         closing the channel and setting the current thread's
     *                                         interrupt status
     * @throws UnresolvedAddressException      If the given remote address is not fully resolved
     * @throws UnsupportedAddressTypeException If the type of the given remote address is not supported
     * @throws SecurityException               If a security manager has been installed
     *                                         and it does not permit access to the given remote address
     * @throws IOException                     If some other I/O error occurs
     */
    /*
     * 对UDP-Socket执行【connect】操作。
     *
     * 将UDP-Socket连接到远程地址，之后只能向该远程地址发送数据，或从该远程地址接收数据。
     *
     * 注：这与TCP-Socket中的绑定意义完全不同，不会经过握手验证
     */
    public abstract DatagramChannel connect(SocketAddress remote) throws IOException;
    
    /**
     * Disconnects this channel's socket.
     *
     * <p> The channel's socket is configured so that it can receive datagrams
     * from, and sends datagrams to, any remote address so long as the security
     * manager, if installed, permits it.
     *
     * <p> This method may be invoked at any time.  It will not have any effect
     * on read or write operations that are already in progress at the moment
     * that it is invoked.
     *
     * <p> If this channel's socket is not connected, or if the channel is
     * closed, then invoking this method has no effect.  </p>
     *
     * @return This datagram channel
     *
     * @throws IOException If some other I/O error occurs
     */
    /*
     * 对UDP-Socket执行【disconnect】操作，即断开连接。
     */
    public abstract DatagramChannel disconnect() throws IOException;
    
    /*▲ socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sends a datagram via this channel.
     *
     * <p> If this channel is in non-blocking mode and there is sufficient room
     * in the underlying output buffer, or if this channel is in blocking mode
     * and sufficient room becomes available, then the remaining bytes in the
     * given buffer are transmitted as a single datagram to the given target
     * address.
     *
     * <p> The datagram is transferred from the byte buffer as if by a regular
     * {@link WritableByteChannel#write(java.nio.ByteBuffer) write} operation.
     *
     * <p> This method performs exactly the same security checks as the {@link
     * java.net.DatagramSocket#send send} method of the {@link
     * java.net.DatagramSocket} class.  That is, if the socket is not connected
     * to a specific remote address and a security manager has been installed
     * then for each datagram sent this method verifies that the target address
     * and port number are permitted by the security manager's {@link
     * java.lang.SecurityManager#checkConnect checkConnect} method.  The
     * overhead of this security check can be avoided by first connecting the
     * socket via the {@link #connect connect} method.
     *
     * <p> This method may be invoked at any time.  If another thread has
     * already initiated a write operation upon this channel, however, then an
     * invocation of this method will block until the first operation is
     * complete. If this channel's socket is not bound then this method will
     * first cause the socket to be bound to an address that is assigned
     * automatically, as if by invoking the {@link #bind bind} method with a
     * parameter of {@code null}. </p>
     *
     * @param src    The buffer containing the datagram to be sent
     * @param target The address to which the datagram is to be sent
     *
     * @return The number of bytes sent, which will be either the number
     * of bytes that were remaining in the source buffer when this
     * method was invoked or, if this channel is non-blocking, may be
     * zero if there was insufficient room for the datagram in the
     * underlying output buffer
     *
     * @throws AlreadyConnectedException       If this channel is connected to a different address
     *                                         from that specified by {@code target}
     * @throws ClosedChannelException          If this channel is closed
     * @throws AsynchronousCloseException      If another thread closes this channel
     *                                         while the read operation is in progress
     * @throws ClosedByInterruptException      If another thread interrupts the current thread
     *                                         while the read operation is in progress, thereby
     *                                         closing the channel and setting the current thread's
     *                                         interrupt status
     * @throws UnresolvedAddressException      If the given remote address is not fully resolved
     * @throws UnsupportedAddressTypeException If the type of the given remote address is not supported
     * @throws SecurityException               If a security manager has been installed
     *                                         and it does not permit datagrams to be sent
     *                                         to the given address
     * @throws IOException                     If some other I/O error occurs
     */
    // 向target处的远端(目的地)发送src中存储的数据，返回写入的字节数量
    public abstract int send(ByteBuffer src, SocketAddress target) throws IOException;
    
    /**
     * Receives a datagram via this channel.
     *
     * <p> If a datagram is immediately available, or if this channel is in
     * blocking mode and one eventually becomes available, then the datagram is
     * copied into the given byte buffer and its source address is returned.
     * If this channel is in non-blocking mode and a datagram is not
     * immediately available then this method immediately returns
     * {@code null}.
     *
     * <p> The datagram is transferred into the given byte buffer starting at
     * its current position, as if by a regular {@link
     * ReadableByteChannel#read(java.nio.ByteBuffer) read} operation.  If there
     * are fewer bytes remaining in the buffer than are required to hold the
     * datagram then the remainder of the datagram is silently discarded.
     *
     * <p> This method performs exactly the same security checks as the {@link
     * java.net.DatagramSocket#receive receive} method of the {@link
     * java.net.DatagramSocket} class.  That is, if the socket is not connected
     * to a specific remote address and a security manager has been installed
     * then for each datagram received this method verifies that the source's
     * address and port number are permitted by the security manager's {@link
     * java.lang.SecurityManager#checkAccept checkAccept} method.  The overhead
     * of this security check can be avoided by first connecting the socket via
     * the {@link #connect connect} method.
     *
     * <p> This method may be invoked at any time.  If another thread has
     * already initiated a read operation upon this channel, however, then an
     * invocation of this method will block until the first operation is
     * complete. If this channel's socket is not bound then this method will
     * first cause the socket to be bound to an address that is assigned
     * automatically, as if invoking the {@link #bind bind} method with a
     * parameter of {@code null}. </p>
     *
     * @param dst The buffer into which the datagram is to be transferred
     *
     * @return The datagram's source address,
     * or {@code null} if this channel is in non-blocking mode
     * and no datagram was immediately available
     *
     * @throws ClosedChannelException     If this channel is closed
     * @throws AsynchronousCloseException If another thread closes this channel
     *                                    while the read operation is in progress
     * @throws ClosedByInterruptException If another thread interrupts the current thread
     *                                    while the read operation is in progress, thereby
     *                                    closing the channel and setting the current thread's
     *                                    interrupt status
     * @throws SecurityException          If a security manager has been installed
     *                                    and it does not permit datagrams to be accepted
     *                                    from the datagram's sender
     * @throws IOException                If some other I/O error occurs
     */
    // 从远端UDP-Socket中接收数据并存入dst，返回实际接收到的字节数
    public abstract SocketAddress receive(ByteBuffer dst) throws IOException;
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads a datagram from this channel.
     *
     * <p> This method may only be invoked if this channel's socket is
     * connected, and it only accepts datagrams from the socket's peer.  If
     * there are more bytes in the datagram than remain in the given buffer
     * then the remainder of the datagram is silently discarded.  Otherwise
     * this method behaves exactly as specified in the {@link
     * ReadableByteChannel} interface.  </p>
     *
     * @throws NotYetConnectedException If this channel's socket is not connected
     */
    // 从当前UDP-Socket中读取数据，并存入dst，返回成功读到的字节数；如果是阻塞通道，会在没有可读数据时陷入阻塞
    public abstract int read(ByteBuffer dst) throws IOException;
    
    /**
     * Reads a datagram from this channel.
     *
     * <p> This method may only be invoked if this channel's socket is
     * connected, and it only accepts datagrams from the socket's peer.  If
     * there are more bytes in the datagram than remain in the given buffers
     * then the remainder of the datagram is silently discarded.  Otherwise
     * this method behaves exactly as specified in the {@link
     * ScatteringByteChannel} interface.  </p>
     *
     * @throws NotYetConnectedException If this channel's socket is not connected
     */
    //【散射】从当前UDP-Socket中读取数据，并存入dsts中offset处起的length个缓冲区中，返回成功读到的字节数；如果是阻塞通道，会在没有可读数据时陷入阻塞
    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;
    
    /**
     * Reads a datagram from this channel.
     *
     * <p> This method may only be invoked if this channel's socket is
     * connected, and it only accepts datagrams from the socket's peer.  If
     * there are more bytes in the datagram than remain in the given buffers
     * then the remainder of the datagram is silently discarded.  Otherwise
     * this method behaves exactly as specified in the {@link
     * ScatteringByteChannel} interface.  </p>
     *
     * @throws NotYetConnectedException If this channel's socket is not connected
     */
    //【散射】从当前UDP-Socket通道中读取数据，读到的内容依次写入dsts中的各个缓冲区
    public final long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }
    
    
    /**
     * Writes a datagram to this channel.
     *
     * <p> This method may only be invoked if this channel's socket is
     * connected, in which case it sends datagrams directly to the socket's
     * peer.  Otherwise it behaves exactly as specified in the {@link
     * WritableByteChannel} interface.  </p>
     *
     * @throws NotYetConnectedException If this channel's socket is not connected
     */
    // 从src中读取数据，读到的内容向当前UDP-Socket通道中写入
    public abstract int write(ByteBuffer src) throws IOException;
    
    /**
     * Writes a datagram to this channel.
     *
     * <p> This method may only be invoked if this channel's socket is
     * connected, in which case it sends datagrams directly to the socket's
     * peer.  Otherwise it behaves exactly as specified in the {@link
     * GatheringByteChannel} interface.  </p>
     *
     * @return The number of bytes sent, which will be either the number
     * of bytes that were remaining in the source buffer when this
     * method was invoked or, if this channel is non-blocking, may be
     * zero if there was insufficient room for the datagram in the
     * underlying output buffer
     *
     * @throws NotYetConnectedException If this channel's socket is not connected
     */
    //【聚集】从srcs中offset处起的length个缓冲区读取数据，读到的内容向当前UDP-Socket通道中写入
    public abstract long write(ByteBuffer[] srcs, int offset, int length) throws IOException;
    
    /**
     * Writes a datagram to this channel.
     *
     * <p> This method may only be invoked if this channel's socket is
     * connected, in which case it sends datagrams directly to the socket's
     * peer.  Otherwise it behaves exactly as specified in the {@link
     * GatheringByteChannel} interface.  </p>
     *
     * @return The number of bytes sent, which will be either the number
     * of bytes that were remaining in the source buffer when this
     * method was invoked or, if this channel is non-blocking, may be
     * zero if there was insufficient room for the datagram in the
     * underlying output buffer
     *
     * @throws NotYetConnectedException If this channel's socket is not connected
     */
    //【聚集】从srcs中各个缓冲区读取数据，读到的内容向当前UDP-Socket通道中写入
    public final long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }
    
    /*▲ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an operation set identifying this channel's supported
     * operations.
     *
     * <p> Datagram channels support reading and writing, so this method
     * returns {@code (}{@link SelectionKey#OP_READ} {@code |}&nbsp;{@link
     * SelectionKey#OP_WRITE}{@code )}.
     *
     * @return The valid-operation set
     */
    // 返回当前通道允许的有效操作参数集
    public final int validOps() {
        return (SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
    
    /*▲ Socket操作参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws ClosedChannelException        {@inheritDoc}
     * @throws IOException                   {@inheritDoc}
     * @since 1.7
     */
    // 设置指定名称的Socket配置参数
    public abstract <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException;
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether or not this channel's socket is connected.
     *
     * @return {@code true} if, and only if, this channel's socket
     * is {@link #isOpen open} and connected
     */
    // 判断当前通道是否已建立连接
    public abstract boolean isConnected();
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
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
    @Override
    public abstract SocketAddress getLocalAddress() throws IOException;
    
    /**
     * Returns the remote address to which this channel's socket is connected.
     *
     * @return The remote address; {@code null} if the channel's socket is not
     * connected
     *
     * @throws ClosedChannelException If the channel is closed
     * @throws IOException            If an I/O error occurs
     * @since 1.7
     */
    // 获取连接的远程地址
    public abstract SocketAddress getRemoteAddress() throws IOException;
    
    /*▲ 地址信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
