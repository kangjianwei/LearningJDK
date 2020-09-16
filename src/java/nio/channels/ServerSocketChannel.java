/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * A selectable channel for stream-oriented listening sockets.
 *
 * <p> A server-socket channel is created by invoking the {@link #open() open}
 * method of this class.  It is not possible to create a channel for an arbitrary,
 * pre-existing {@link ServerSocket}. A newly-created server-socket channel is
 * open but not yet bound.  An attempt to invoke the {@link #accept() accept}
 * method of an unbound server-socket channel will cause a {@link NotYetBoundException}
 * to be thrown. A server-socket channel can be bound by invoking one of the
 * {@link #bind(java.net.SocketAddress,int) bind} methods defined by this class.
 *
 * <p> Socket options are configured using the {@link #setOption(SocketOption,Object)
 * setOption} method. Server-socket channels support the following options:
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
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_RCVBUF SO_RCVBUF} </th>
 *     <td> The size of the socket receive buffer </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_REUSEADDR SO_REUSEADDR} </th>
 *     <td> Re-use address </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 * Additional (implementation specific) options may also be supported.
 *
 * <p> Server-socket channels are safe for use by multiple concurrent threads.
 * </p>
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */
// ServerSocket通道，支持在非阻塞模式下运行
public abstract class ServerSocketChannel extends AbstractSelectableChannel implements NetworkChannel {
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class.
     *
     * @param provider The provider that created this channel
     */
    protected ServerSocketChannel(SelectorProvider provider) {
        super(provider);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens a server-socket channel.
     *
     * <p> The new channel is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openServerSocketChannel
     * openServerSocketChannel} method of the system-wide default {@link
     * java.nio.channels.spi.SelectorProvider} object.
     *
     * <p> The new channel's socket is initially unbound; it must be bound to a
     * specific address via one of its socket's {@link
     * java.net.ServerSocket#bind(SocketAddress) bind} methods before
     * connections can be accepted.  </p>
     *
     * @return A new socket channel
     *
     * @throws IOException If an I/O error occurs
     */
    // 构造一个未绑定的ServerSocket，本质是创建了[服务端Socket(监听)]，内部初始化了该Socket的文件描述符
    public static ServerSocketChannel open() throws IOException {
        return SelectorProvider.provider().openServerSocketChannel();
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 适配 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves a server socket associated with this channel.
     *
     * <p> The returned object will not declare any public methods that are not
     * declared in the {@link java.net.ServerSocket} class.  </p>
     *
     * @return A server socket associated with this channel
     */
    // 返回由当前ServerSocket通道适配而成的ServerSocket
    public abstract ServerSocket socket();
    
    /*▲ 适配 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Binds the channel's socket to a local address and configures the socket to listen for connections.
     *
     * <p> An invocation of this method is equivalent to the following:
     * <blockquote><pre>
     * bind(local, 0);
     * </pre></blockquote>
     *
     * @param local The local address to bind the socket, or {@code null} to bind to an automatically assigned socket address
     *
     * @return This channel
     *
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
     * 对[服务端Socket(监听)]执行【bind】和【listen】操作；
     * 此处默认允许积压(排队)的待处理连接数为50。
     *
     * endpoint: 既作为服务端的绑定地址(包含端口)，也作为开启监听的地址(包含端口)
     */
    public final ServerSocketChannel bind(SocketAddress local) throws IOException {
        return bind(local, 0);
    }
    
    /**
     * Binds the channel's socket to a local address and configures the socket to
     * listen for connections.
     *
     * <p> This method is used to establish an association between the socket and
     * a local address. Once an association is established then the socket remains
     * bound until the channel is closed.
     *
     * <p> The {@code backlog} parameter is the maximum number of pending
     * connections on the socket. Its exact semantics are implementation specific.
     * In particular, an implementation may impose a maximum length or may choose
     * to ignore the parameter altogther. If the {@code backlog} parameter has
     * the value {@code 0}, or a negative value, then an implementation specific
     * default is used.
     *
     * @param local   The address to bind the socket, or {@code null} to bind to an
     *                automatically assigned socket address
     * @param backlog The maximum number of pending connections
     *
     * @return This channel
     *
     * @throws AlreadyBoundException           If the socket is already bound
     * @throws UnsupportedAddressTypeException If the type of the given address is not supported
     * @throws ClosedChannelException          If this channel is closed
     * @throws IOException                     If some other I/O error occurs
     * @throws SecurityException               If a security manager has been installed and its {@link
     *                                         SecurityManager#checkListen checkListen} method denies the
     *                                         operation
     * @since 1.7
     */
    /*
     * 对[服务端Socket(监听)]执行【bind】和【listen】操作
     *
     * endpoint: 既作为服务端的绑定地址(包含端口)，也作为开启监听的地址(包含端口)
     * backlog : 允许积压(排队)的待处理连接数；如果backlog<1，则取默认值50
     */
    public abstract ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException;
    
    /**
     * Accepts a connection made to this channel's socket.
     *
     * <p> If this channel is in non-blocking mode then this method will
     * immediately return {@code null} if there are no pending connections.
     * Otherwise it will block indefinitely until a new connection is available
     * or an I/O error occurs.
     *
     * <p> The socket channel returned by this method, if any, will be in
     * blocking mode regardless of the blocking mode of this channel.
     *
     * <p> This method performs exactly the same security checks as the {@link
     * java.net.ServerSocket#accept accept} method of the {@link
     * java.net.ServerSocket} class.  That is, if a security manager has been
     * installed then for each new connection this method verifies that the
     * address and port number of the connection's remote endpoint are
     * permitted by the security manager's {@link
     * java.lang.SecurityManager#checkAccept checkAccept} method.  </p>
     *
     * @return The socket channel for the new connection,
     * or {@code null} if this channel is in non-blocking mode
     * and no connection is available to be accepted
     *
     * @throws ClosedChannelException     If this channel is closed
     * @throws AsynchronousCloseException If another thread closes this channel
     *                                    while the accept operation is in progress
     * @throws ClosedByInterruptException If another thread interrupts the current thread
     *                                    while the accept operation is in progress, thereby
     *                                    closing the channel and setting the current thread's
     *                                    interrupt status
     * @throws NotYetBoundException       If this channel's socket has not yet been bound
     * @throws SecurityException          If a security manager has been installed
     *                                    and it does not permit access to the remote endpoint
     *                                    of the new connection
     * @throws IOException                If some other I/O error occurs
     */
    /*
     * [服务端Socket(监听)]等待客户端的连接请求；
     * 连接成功后，返回与[客户端Socket]建立连接的[服务端Socket(通信)]。
     *
     * 注：此处返回的SocketChannel对象默认是阻塞式的，可以后续将其设置为非阻塞模式
     */
    public abstract SocketChannel accept() throws IOException;
    
    /*▲ socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an operation set identifying this channel's supported operations.
     *
     * Server-socket channels only support the accepting of new connections,
     * so this method returns {@link SelectionKey#OP_ACCEPT}.
     *
     * @return The valid-operation set
     */
    // 返回当前通道允许的有效操作参数集
    public final int validOps() {
        return SelectionKey.OP_ACCEPT;
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
    public abstract <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException;
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
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
    
    /*▲ 地址信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
