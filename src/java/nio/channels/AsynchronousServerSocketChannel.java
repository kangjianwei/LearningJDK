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
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.Future;

/**
 * An asynchronous channel for stream-oriented listening sockets.
 *
 * <p> An asynchronous server-socket channel is created by invoking the
 * {@link #open open} method of this class.
 * A newly-created asynchronous server-socket channel is open but not yet bound.
 * It can be bound to a local address and configured to listen for connections
 * by invoking the {@link #bind(SocketAddress, int) bind} method. Once bound,
 * the {@link #accept(Object, CompletionHandler) accept} method
 * is used to initiate the accepting of connections to the channel's socket.
 * An attempt to invoke the {@code accept} method on an unbound channel will
 * cause a {@link NotYetBoundException} to be thrown.
 *
 * <p> Channels of this type are safe for use by multiple concurrent threads
 * though at most one accept operation can be outstanding at any time.
 * If a thread initiates an accept operation before a previous accept operation
 * has completed then an {@link AcceptPendingException} will be thrown.
 *
 * <p> Socket options are configured using the {@link #setOption(SocketOption, Object)
 * setOption} method. Channels of this type support the following options:
 * <blockquote>
 * <table class="striped">
 * <caption style="display:none">Socket options</caption>
 * <thead>
 * <tr>
 * <th scope="col">Option Name</th>
 * <th scope="col">Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <th scope="row"> {@link java.net.StandardSocketOptions#SO_RCVBUF SO_RCVBUF} </th>
 * <td> The size of the socket receive buffer </td>
 * </tr>
 * <tr>
 * <th scope="row"> {@link java.net.StandardSocketOptions#SO_REUSEADDR SO_REUSEADDR} </th>
 * <td> Re-use address </td>
 * </tr>
 * </tbody>
 * </table>
 * </blockquote>
 * Additional (implementation specific) options may also be supported.
 *
 * <p> <b>Usage Example:</b>
 * <pre>
 *  final AsynchronousServerSocketChannel listener =
 *      AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(5000));
 *
 *  listener.accept(null, new CompletionHandler&lt;AsynchronousSocketChannel,Void&gt;() {
 *      public void completed(AsynchronousSocketChannel ch, Void att) {
 *          // accept the next connection
 *          listener.accept(null, this);
 *
 *          // handle this connection
 *          handle(ch);
 *      }
 *      public void failed(Throwable exc, Void att) {
 *          ...
 *      }
 *  });
 * </pre>
 *
 * @since 1.7
 */
// 异步ServerSocket通道
public abstract class AsynchronousServerSocketChannel implements AsynchronousChannel, NetworkChannel {
    
    private final AsynchronousChannelProvider provider;     // 异步Socket通道工厂
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class.
     *
     * @param provider The provider that created this channel
     */
    protected AsynchronousServerSocketChannel(AsynchronousChannelProvider provider) {
        this.provider = provider;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens an asynchronous server-socket channel.
     *
     * <p> This method returns an asynchronous server socket channel that is
     * bound to the <em>default group</em>. This method is equivalent to evaluating
     * the expression:
     * <blockquote><pre>
     * open((AsynchronousChannelGroup)null);
     * </pre></blockquote>
     *
     * @return A new asynchronous server socket channel
     *
     * @throws IOException If an I/O error occurs
     */
    /*
     * 创建并返回一个异步ServerSocket通道
     *
     * 这里使用了默认的异步Socket通道工厂，在使用该工厂创建异步ServerSocket通道时，会自动启动工作线程。
     */
    public static AsynchronousServerSocketChannel open() throws IOException {
        return open(null);
    }
    
    /**
     * Opens an asynchronous server-socket channel.
     *
     * <p> The new channel is created by invoking the {@link
     * java.nio.channels.spi.AsynchronousChannelProvider#openAsynchronousServerSocketChannel
     * openAsynchronousServerSocketChannel} method on the {@link
     * java.nio.channels.spi.AsynchronousChannelProvider} object that created
     * the given group. If the group parameter is {@code null} then the
     * resulting channel is created by the system-wide default provider, and
     * bound to the <em>default group</em>.
     *
     * @param group The group to which the newly constructed channel should be bound,
     *              or {@code null} for the default group
     *
     * @return A new asynchronous server socket channel
     *
     * @throws ShutdownChannelGroupException If the channel group is shutdown
     * @throws IOException                   If an I/O error occurs
     */
    /*
     * 创建并返回一个异步ServerSocket通道，group是该通道关联的异步通道组
     *
     * 如果group为null，则使用默认的异步Socket通道工厂；
     * 如果group不为null，则应当使用group中提供的异步Socket通道工厂。
     *
     * 在使用异步Socket通道工厂创建异步ServerSocket通道时，应当启动工作线程。
     */
    public static AsynchronousServerSocketChannel open(AsynchronousChannelGroup group) throws IOException {
        AsynchronousChannelProvider provider = (group == null) ? AsynchronousChannelProvider.provider() : group.provider();
        return provider.openAsynchronousServerSocketChannel(group);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Binds the channel's socket to a local address and configures the socket to
     * listen for connections.
     *
     * <p> An invocation of this method is equivalent to the following:
     * <blockquote><pre>
     * bind(local, 0);
     * </pre></blockquote>
     *
     * @param local The local address to bind the socket, or {@code null} to bind
     *              to an automatically assigned socket address
     *
     * @return This channel
     *
     * @throws AlreadyBoundException           {@inheritDoc}
     * @throws UnsupportedAddressTypeException {@inheritDoc}
     * @throws SecurityException               {@inheritDoc}
     * @throws ClosedChannelException          {@inheritDoc}
     * @throws IOException                     {@inheritDoc}
     */
    /*
     * 对[服务端Socket(监听)]执行【bind】和【listen】操作；这里默认允许积压(排队)的待处理连接数为50
     *
     * local: 既作为服务端的绑定地址(包含端口)，也作为开启监听的地址(包含端口)
     */
    public final AsynchronousServerSocketChannel bind(SocketAddress local) throws IOException {
        return bind(local, 0);
    }
    
    /**
     * Binds the channel's socket to a local address and configures the socket to
     * listen for connections.
     *
     * <p> This method is used to establish an association between the socket and
     * a local address. Once an association is established then the socket remains
     * bound until the associated channel is closed.
     *
     * <p> The {@code backlog} parameter is the maximum number of pending
     * connections on the socket. Its exact semantics are implementation specific.
     * In particular, an implementation may impose a maximum length or may choose
     * to ignore the parameter altogther. If the {@code backlog} parameter has
     * the value {@code 0}, or a negative value, then an implementation specific
     * default is used.
     *
     * @param local   The local address to bind the socket, or {@code null} to bind
     *                to an automatically assigned socket address
     * @param backlog The maximum number of pending connections
     *
     * @return This channel
     *
     * @throws AlreadyBoundException           If the socket is already bound
     * @throws UnsupportedAddressTypeException If the type of the given address is not supported
     * @throws SecurityException               If a security manager has been installed and its {@link
     *                                         SecurityManager#checkListen checkListen} method denies the operation
     * @throws ClosedChannelException          If the channel is closed
     * @throws IOException                     If some other I/O error occurs
     */
    /*
     * 对[服务端Socket(监听)]执行【bind】和【listen】操作
     *
     * local  : 既作为服务端的绑定地址(包含端口)，也作为开启监听的地址(包含端口)
     * backlog: 允许积压(排队)的待处理连接数；如果backlog<1，则取默认值50
     */
    public abstract AsynchronousServerSocketChannel bind(SocketAddress local, int backlog) throws IOException;
    
    /**
     * Accepts a connection.
     *
     * <p> This method initiates an asynchronous operation to accept a
     * connection made to this channel's socket. The method behaves in exactly
     * the same manner as the {@link #accept(Object, CompletionHandler)} method
     * except that instead of specifying a completion handler, this method
     * returns a {@code Future} representing the pending result. The {@code
     * Future}'s {@link Future#get() get} method returns the {@link
     * AsynchronousSocketChannel} to the new connection on successful completion.
     *
     * @return a {@code Future} object representing the pending result
     *
     * @throws AcceptPendingException If an accept operation is already in progress on this channel
     * @throws NotYetBoundException   If this channel's socket has not yet been bound
     */
    /*
     * [服务端Socket(监听)]等待客户端的连接请求；
     * 返回值一个包含[服务端Socket(通信)]的Future，主线程轮询此Future以判断是否accept完成，
     * 以及获取到与[客户端Socket]建立连接的[服务端Socket(通信)]。
     */
    public abstract Future<AsynchronousSocketChannel> accept();
    
    /**
     * Accepts a connection.
     *
     * <p> This method initiates an asynchronous operation to accept a
     * connection made to this channel's socket. The {@code handler} parameter is
     * a completion handler that is invoked when a connection is accepted (or
     * the operation fails). The result passed to the completion handler is
     * the {@link AsynchronousSocketChannel} to the new connection.
     *
     * <p> When a new connection is accepted then the resulting {@code
     * AsynchronousSocketChannel} will be bound to the same {@link
     * AsynchronousChannelGroup} as this channel. If the group is {@link
     * AsynchronousChannelGroup#isShutdown shutdown} and a connection is accepted,
     * then the connection is closed, and the operation completes with an {@code
     * IOException} and cause {@link ShutdownChannelGroupException}.
     *
     * <p> To allow for concurrent handling of new connections, the completion
     * handler is not invoked directly by the initiating thread when a new
     * connection is accepted immediately (see <a
     * href="AsynchronousChannelGroup.html#threading">Threading</a>).
     *
     * <p> If a security manager has been installed then it verifies that the
     * address and port number of the connection's remote endpoint are permitted
     * by the security manager's {@link SecurityManager#checkAccept checkAccept}
     * method. The permission check is performed with privileges that are restricted
     * by the calling context of this method. If the permission check fails then
     * the connection is closed and the operation completes with a {@link
     * SecurityException}.
     *
     * @param <A>        The type of the attachment
     * @param attachment The object to attach to the I/O operation; can be {@code null}
     * @param handler    The handler for consuming the result
     *
     * @throws AcceptPendingException        If an accept operation is already in progress on this channel
     * @throws NotYetBoundException          If this channel's socket has not yet been bound
     * @throws ShutdownChannelGroupException If the channel group has terminated
     */
    /*
     * [服务端Socket(监听)]等待客户端的连接请求；
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，
     * 以便主线程获取到与[客户端Socket]建立连接的[服务端Socket(通信)]。
     */
    public abstract <A> void accept(A attachment, CompletionHandler<AsynchronousSocketChannel, ? super A> handler);
    
    /*▲ socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
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
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ClosedChannelException   {@inheritDoc}
     * @throws IOException              {@inheritDoc}
     */
    // 设置指定名称的Socket配置参数
    public abstract <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException;
    
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
