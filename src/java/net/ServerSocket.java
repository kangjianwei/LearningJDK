/*
 * Copyright (c) 1995, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.ServerSocketChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Set;
import jdk.internal.misc.JavaNetSocketAccess;
import jdk.internal.misc.SharedSecrets;

/**
 * This class implements server sockets.
 * A server socket waits for requests to come in over the network.
 * It performs some operation based on that request, and then possibly returns a result to the requester.
 * <p>
 * The actual work of the server socket is performed by an instance of the {@code SocketImpl} class.
 * An application can change the socket factory that creates the socket implementation to configure itself
 * to create sockets appropriate to the local firewall.
 *
 * @author unascribed
 * @see java.net.SocketImpl
 * @see java.net.ServerSocket#setSocketFactory(java.net.SocketImplFactory)
 * @see java.nio.channels.ServerSocketChannel
 * @since 1.0
 */
/*
 * 面向连接的ServerSocket(使用TCP Socket)，用在服务端
 *
 * 注：ServerSocket可以看做是对[服务端Socket(监听)]的"间接"包装。
 *
 *
 * Linux上的TCP通信方法（本类中实现的API与底层API略有区别）：
 *
 * 服务端                客户端
 *
 * sokcet()             sokcet()
 *   ↓                     ↓
 * bind()               bind()-可选
 *   ↓                     ↓
 * listen()                ↓
 *   ↓                     ↓
 * accept()                ↓
 *   ↓                     ↓
 * 阻塞，等待客户端连接      ↓
 *   ↓                     ↓
 *   █    ←←←←←←←←←←←←← connect()
 *   ↓                     ↓
 *   ↓     客户端发出请求   ↓
 * read() ←←←←←←←←←←←←← write()
 *   ↓                     ↓
 * 服务端处理请求           ↓
 * 服务端做出响应           ↓
 *   ↓                     ↓
 *   ↓     客户端接收响应   ↓
 * write() →→→→→→→→→→→→ read()
 *   ↓                     ↓
 * close()              close()
 *
 *
 * 注：客户端与服务端是一个相对的概念，没有绝对的客户端或绝对的服务端
 */
public class ServerSocket implements Closeable {
    
    /**
     * The implementation of this Socket.
     */
    // [服务端Socket(监听)]的"Socket委托"
    private SocketImpl impl;
    
    /**
     * The factory for all server sockets.
     */
    // "Socket委托"的工厂
    private static SocketImplFactory factory = null;
    
    /**
     * Various states of this socket.
     */
    private boolean created = false;    // 指示[服务端Socket(监听)]是否已经创建
    private boolean bound = false;    // 指示[服务端Socket(监听)]是否已绑定
    
    private boolean closed = false;    // 指示[服务端Socket(监听)]是否已关闭
    
    /**
     * Are we using an older SocketImpl?
     */
    private boolean oldImpl = false;    // 是否在使用旧式的"Socket委托"(JDK1.5起始，该值均为false)
    
    private static Set<SocketOption<?>> options;    // ServerSocket配置参数
    private static boolean optionsSet = false;      // 懒加载，标记是否初始化了Socket配置参数
    
    private Object closeLock = new Object();
    
    
    static {
        SharedSecrets.setJavaNetSocketAccess(new JavaNetSocketAccess() {
            @Override
            public ServerSocket newServerSocket(SocketImpl impl) {
                return new ServerSocket(impl);
            }
            
            @Override
            public SocketImpl newSocketImpl(Class<? extends SocketImpl> implClass) {
                try {
                    Constructor<? extends SocketImpl> ctor = implClass.getDeclaredConstructor();
                    return ctor.newInstance();
                } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new AssertionError(e);
                }
            }
        });
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Create a server with the specified port, listen backlog, and
     * local IP address to bind to.  The <i>bindAddr</i> argument
     * can be used on a multi-homed host for a ServerSocket that
     * will only accept connect requests to one of its addresses.
     * If <i>bindAddr</i> is null, it will default accepting
     * connections on any/all local addresses.
     * The port must be between 0 and 65535, inclusive.
     * A port number of {@code 0} means that the port number is
     * automatically allocated, typically from an ephemeral port range.
     * This port number can then be retrieved by calling
     * {@link #getLocalPort getLocalPort}.
     *
     * <P>If there is a security manager, this method
     * calls its {@code checkListen} method
     * with the {@code port} argument
     * as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * The {@code backlog} argument is the requested maximum number of
     * pending connections on the socket. Its exact semantics are implementation
     * specific. In particular, an implementation may impose a maximum length
     * or may choose to ignore the parameter altogther. The value provided
     * should be greater than {@code 0}. If it is less than or equal to
     * {@code 0}, then an implementation specific default will be used.
     *
     * @param port     the port number, or {@code 0} to use a port number that is automatically allocated.
     * @param backlog  requested maximum length of the queue of incoming connections.
     * @param bindAddr the local InetAddress the server will bind to
     *
     * @throws SecurityException        if a security manager exists and
     *                                  its {@code checkListen} method doesn't allow the operation.
     * @throws IOException              if an I/O error occurs when opening the socket.
     * @throws IllegalArgumentException if the port parameter is outside
     *                                  the specified range of valid port values, which is between
     *                                  0 and 65535, inclusive.
     * @see SocketOptions
     * @see SocketImpl
     * @see SecurityManager#checkListen
     * @since 1.1
     */
    /*
     * ▶ 1 构造ServerSocket，本质是创建了[服务端Socket(监听)]，并对其执行了【bind】和【listen】操作。
     *
     * port     [服务端Socket]绑定的本地端口
     * backlog  允许积压(排队)的待处理连接数
     * bindAddr [服务端Socket]绑定的本地IP
     */
    public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        // 创建"Socket委托"，并为其关联当前ServerSocket
        setImpl();
        
        // 端口号必须位于[0, 65535]之间
        if(port<0 || port>0xFFFF) {
            throw new IllegalArgumentException("Port value out of range: " + port);
        }
        
        // 默认允许积压的待处理连接数为50
        if(backlog<1) {
            backlog = 50;
        }
        
        try {
            // 使用指定的IP地址和端口号构造一个Socket地址以便绑定
            InetSocketAddress endpoint = new InetSocketAddress(bindAddr, port);
            
            // 创建[服务端Socket(监听)]，并对其执行【bind】和【listen】操作
            bind(endpoint, backlog);
        } catch(SecurityException | IOException e) {
            close();
            throw e;
        }
    }
    
    /**
     * Creates a server socket, bound to the specified port. A port number
     * of {@code 0} means that the port number is automatically
     * allocated, typically from an ephemeral port range. This port
     * number can then be retrieved by calling {@link #getLocalPort getLocalPort}.
     * <p>
     * The maximum queue length for incoming connection indications (a
     * request to connect) is set to {@code 50}. If a connection
     * indication arrives when the queue is full, the connection is refused.
     * <p>
     * If the application has specified a server socket factory, that
     * factory's {@code createSocketImpl} method is called to create
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager,
     * its {@code checkListen} method is called
     * with the {@code port} argument
     * as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param port the port number, or {@code 0} to use a port
     *             number that is automatically allocated.
     *
     * @throws IOException              if an I/O error occurs when opening the socket.
     * @throws SecurityException        if a security manager exists and its {@code checkListen}
     *                                  method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside
     *                                  the specified range of valid port values, which is between
     *                                  0 and 65535, inclusive.
     * @see java.net.SocketImpl
     * @see java.net.SocketImplFactory#createSocketImpl()
     * @see java.net.ServerSocket#setSocketFactory(java.net.SocketImplFactory)
     * @see SecurityManager#checkListen
     */
    /*
     * ▶ 1-1 构造ServerSocket，本质是创建了[服务端Socket(监听)]，并对其执行了【bind】和【listen】操作，
     *       默认绑定到服务端的本地IP上，且允许积压(排队)的待处理连接数为50。
     *
     * port [服务端Socket]绑定的本地端口
     */
    public ServerSocket(int port) throws IOException {
        this(port, 50, null);
    }
    
    /**
     * Creates a server socket and binds it to the specified local port
     * number, with the specified backlog.
     * A port number of {@code 0} means that the port number is
     * automatically allocated, typically from an ephemeral port range.
     * This port number can then be retrieved by calling
     * {@link #getLocalPort getLocalPort}.
     * <p>
     * The maximum queue length for incoming connection indications (a
     * request to connect) is set to the {@code backlog} parameter. If
     * a connection indication arrives when the queue is full, the
     * connection is refused.
     * <p>
     * If the application has specified a server socket factory, that
     * factory's {@code createSocketImpl} method is called to create
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager,
     * its {@code checkListen} method is called
     * with the {@code port} argument
     * as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * The {@code backlog} argument is the requested maximum number of
     * pending connections on the socket. Its exact semantics are implementation
     * specific. In particular, an implementation may impose a maximum length
     * or may choose to ignore the parameter altogther. The value provided
     * should be greater than {@code 0}. If it is less than or equal to
     * {@code 0}, then an implementation specific default will be used.
     *
     * @param port    the port number, or {@code 0} to use a port
     *                number that is automatically allocated.
     * @param backlog requested maximum length of the queue of incoming
     *                connections.
     *
     * @throws IOException              if an I/O error occurs when opening the socket.
     * @throws SecurityException        if a security manager exists and its {@code checkListen}
     *                                  method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside
     *                                  the specified range of valid port values, which is between
     *                                  0 and 65535, inclusive.
     * @see java.net.SocketImpl
     * @see java.net.SocketImplFactory#createSocketImpl()
     * @see java.net.ServerSocket#setSocketFactory(java.net.SocketImplFactory)
     * @see SecurityManager#checkListen
     */
    /*
     * ▶ 1-2 构造ServerSocket，本质是创建了[服务端Socket(监听)]，并对其执行了【bind】和【listen】操作。
     *       默认绑定到服务端的本地IP上。
     *
     * port     [服务端Socket]绑定的本地端口
     * backlog  允许积压(排队)的待处理连接数
     */
    public ServerSocket(int port, int backlog) throws IOException {
        this(port, backlog, null);
    }
    
    /**
     * Creates an unbound server socket.
     *
     * @throws IOException IO error when opening the socket.
     * @revised 1.4
     */
    // ▶ 2 构造一个"未就绪"的ServerSocket，需要在后续调用bind()方法来创建[服务端Socket(监听)]，以便进行【bind】和【listen】操作
    public ServerSocket() throws IOException {
        setImpl();  // 创建"Socket委托"，并为其关联当前ServerSocket
    }
    
    /**
     * Package-private constructor to create a ServerSocket associated with the given SocketImpl.
     */
    // ▶ 3 用指定的"Socket委托"构造一个"未就绪"的ServerSocket，需要在后续调用bind()方法来创建[服务端Socket(监听)]，以便进行【bind】和【listen】操作
    ServerSocket(SocketImpl impl) {
        this.impl = impl;
        // 为指定的"Socket委托"关联ServerSocket
        impl.setServerSocket(this);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Binds the {@code ServerSocket} to a specific address (IP address and port number).
     * <p>
     * If the address is {@code null}, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     *
     * @param endpoint The IP address and port number to bind to.
     *
     * @throws IOException              if the bind operation fails, or if the socket
     *                                  is already bound.
     * @throws SecurityException        if a {@code SecurityManager} is present and
     *                                  its {@code checkListen} method doesn't allow the operation.
     * @throws IllegalArgumentException if endpoint is a
     *                                  SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    /*
     * 创建[服务端Socket(监听)]，并对其执行【bind】和【listen】操作，此处允许积压(排队)的待处理连接数为50
     *
     * endpoint: 既作为服务端的绑定地址(包含端口)，也作为开启监听的地址(包含端口)
     */
    public void bind(SocketAddress endpoint) throws IOException {
        bind(endpoint, 50);
    }
    
    /**
     * Binds the {@code ServerSocket} to a specific address
     * (IP address and port number).
     * <p>
     * If the address is {@code null}, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     * <P>
     * The {@code backlog} argument is the requested maximum number of
     * pending connections on the socket. Its exact semantics are implementation
     * specific. In particular, an implementation may impose a maximum length
     * or may choose to ignore the parameter altogther. The value provided
     * should be greater than {@code 0}. If it is less than or equal to
     * {@code 0}, then an implementation specific default will be used.
     *
     * @param endpoint The IP address and port number to bind to.
     * @param backlog  requested maximum length of the queue of
     *                 incoming connections.
     *
     * @throws IOException              if the bind operation fails, or if the socket
     *                                  is already bound.
     * @throws SecurityException        if a {@code SecurityManager} is present and
     *                                  its {@code checkListen} method doesn't allow the operation.
     * @throws IllegalArgumentException if endpoint is a
     *                                  SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    /*
     * 创建[服务端Socket(监听)]，并对其执行【bind】和【listen】操作
     *
     * endpoint: 既作为服务端的绑定地址(包含端口)，也作为开启监听的地址(包含端口)
     * backlog : 允许积压(排队)的待处理连接数；如果backlog<1，则取默认值50
     */
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        // 如果ServerSocket已关闭，抛异常
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 如果ServerSocket已绑定，抛异常(JDK1.5起始，oldImpl总是为false)
        if(!oldImpl && isBound()) {
            throw new SocketException("Already bound");
        }
        
        if(endpoint == null) {
            endpoint = new InetSocketAddress(0);
        }
        
        if(!(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        
        // 记录服务端的本地Socket地址(ip + port)
        InetSocketAddress epoint = (InetSocketAddress) endpoint;
        
        // 此处要求为具体的IP，而不是域名
        if(epoint.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        
        // 默认允许排队的连接数为50
        if(backlog<1) {
            backlog = 50;
        }
        
        try {
            SecurityManager security = System.getSecurityManager();
            if(security != null) {
                security.checkListen(epoint.getPort());
            }
            
            // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
            SocketImpl impl = getImpl();
            
            // 通过指定的"Socket委托"为引用的[服务端Socket(监听)]绑定IP与端口号
            impl.bind(epoint.getAddress(), epoint.getPort());
            
            // 通过指定的"Socket委托"为引用的[服务端Socket(监听)]开启监听，backlog代表允许积压的待处理连接数
            impl.listen(backlog);
            
            bound = true;
        } catch(SecurityException | IOException e) {
            bound = false;
            throw e;
        }
    }
    
    
    /**
     * Listens for a connection to be made to this socket and accepts
     * it. The method blocks until a connection is made.
     *
     * <p>A new Socket {@code s} is created and, if there
     * is a security manager,
     * the security manager's {@code checkAccept} method is called
     * with {@code s.getInetAddress().getHostAddress()} and
     * {@code s.getPort()}
     * as its arguments to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @return the new Socket
     *
     * @throws IOException                                    if an I/O error occurs when waiting for a
     *                                                        connection.
     * @throws SecurityException                              if a security manager exists and its
     *                                                        {@code checkAccept} method doesn't allow the operation.
     * @throws SocketTimeoutException                         if a timeout was previously set with setSoTimeout and
     *                                                        the timeout has been reached.
     * @throws java.nio.channels.IllegalBlockingModeException if this socket has an associated channel, the channel is in
     *                                                        non-blocking mode, and there is no connection ready to be
     *                                                        accepted
     * @revised 1.4
     * @spec JSR-51
     * @see SecurityManager#checkAccept
     */
    /*
     * [服务端Socket(监听)]等待客户端的连接请求；
     * 连接成功后，返回与[客户端Socket]建立连接的[服务端Socket(通信)]
     */
    public Socket accept() throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        if(!isBound()) {
            throw new SocketException("Socket is not bound yet");
        }
        
        // 创建[服务端Socket(通信)]，未设置"Socket委托"
        Socket socket = new Socket((SocketImpl) null);
        
        // [服务端Socket(监听)]等待客户端的连接请求；连接成功后，进一步完善参数中的[服务端Socket(通信)]
        implAccept(socket);
        
        // 返回与[客户端Socket]建立连接的[服务端Socket(通信)]
        return socket;
    }
    
    /**
     * Subclasses of ServerSocket use this method to override accept()
     * to return their own subclass of socket.  So a FooServerSocket
     * will typically hand this method an <i>empty</i> FooSocket.  On
     * return from implAccept the FooSocket will be connected to a client.
     *
     * @param s the Socket
     *
     * @throws java.nio.channels.IllegalBlockingModeException if this socket has an associated channel,
     *                                                        and the channel is in non-blocking mode
     * @throws IOException                                    if an I/O error occurs when waiting
     *                                                        for a connection.
     * @revised 1.4
     * @spec JSR-51
     * @since 1.1
     */
    /*
     * [服务端Socket(监听)]等待客户端的连接请求；
     * 连接成功后，进一步完善参数中的[服务端Socket(通信)]
     */
    protected final void implAccept(Socket socket) throws IOException {
        // [服务端Socket(通信)]的"Socket委托"
        SocketImpl socketImpl = null;
        
        try {
            if(socket.impl == null) {
                // 初始化"Socket委托"，并为其关联参数中的socket
                socket.setImpl();
            } else {
                // 重置连接信息
                socket.impl.reset();
            }
            
            // 获取上面创建的"Socket委托"
            socketImpl = socket.impl;
            // 置空socket内的"Socket委托"
            socket.impl = null;
            
            socketImpl.address = new InetAddress();
            socketImpl.fd = new FileDescriptor();
            
            // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
            SocketImpl impl = getImpl();
            
            /*
             * 由[服务端Socket(监听)]的"Socket委托"调用，对[服务端Socket(监听)]执行【accept】操作，
             * accept成功后，会获取到新生成的[服务端Socket(通信)]的文件描述符，
             * 随后，将客户端端口/客户端地址/服务端端口/[服务端Socket(通信)]文件描述符设置到参数中的"Socket委托"中。
             *
             * socketImpl: [服务端Socket(通信)]的"Socket委托"，
             *             accept成功后会为其填充相关的地址信息，以及为其关联[服务端Socket(通信)](的文件描述符)
             */
            impl.accept(socketImpl);
            
            // 将[服务端Socket(通信)]的文件描述符注册到清理器
            SocketCleanable.register(socketImpl.fd);   // raw fd has been set
            
            SecurityManager security = System.getSecurityManager();
            if(security != null) {
                security.checkAccept(socketImpl.getInetAddress().getHostAddress(), socketImpl.getPort());
            }
        } catch(IOException | SecurityException e) {
            if(socketImpl != null) {
                // 重置连接信息
                socketImpl.reset();
            }
            socket.impl = socketImpl;
            throw e;
        }
        
        // 设置"Socket委托"
        socket.impl = socketImpl;
        
        // 指示[服务端Socket(通信)]已就绪
        socket.postAccept();
    }
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes this socket.
     *
     * Any thread currently blocked in {@link #accept()} will throw
     * a {@link SocketException}.
     *
     * <p> If this socket has an associated channel then the channel is closed
     * as well.
     *
     * @throws IOException if an I/O error occurs when closing the socket.
     * @revised 1.4
     * @spec JSR-51
     */
    // 关闭socket连接
    public void close() throws IOException {
        synchronized(closeLock) {
            if(isClosed()) {
                return;
            }
    
            if(created) {
                impl.close();
            }
    
            closed = true;
        }
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the address of the endpoint this socket is bound to.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return the address of the endpoint
     * after the socket is closed.
     * <p>
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * a {@code SocketAddress} representing the
     * {@link InetAddress#getLoopbackAddress loopback} address and the local
     * port to which the socket is bound is returned.
     *
     * @return a {@code SocketAddress} representing the local endpoint of
     * this socket, or a {@code SocketAddress} representing the
     * loopback address if denied by the security manager,
     * or {@code null} if the socket is not bound yet.
     *
     * @see #getInetAddress()
     * @see #getLocalPort()
     * @see #bind(SocketAddress)
     * @see SecurityManager#checkConnect
     * @since 1.4
     */
    // 返回本地Socket地址(ip+port)，即服务端地址；如果还未绑定，则返回null
    public SocketAddress getLocalSocketAddress() {
        if(!isBound()) {
            return null;
        }
        
        // 获取本地IP，即服务端IP；如果没有绑定，则返回null
        InetAddress address = getInetAddress();
        
        // 获取本地端口
        int port = getLocalPort();
        
        // 包装为Socket地址后返回，如果IP地址为null，使用通配地址
        return new InetSocketAddress(address, port);
    }
    
    /**
     * Returns the local address of this server socket.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return the local address
     * after the socket is closed.
     * <p>
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * the {@link InetAddress#getLoopbackAddress loopback} address is returned.
     *
     * @return the address to which this socket is bound,
     * or the loopback address if denied by the security manager,
     * or {@code null} if the socket is unbound.
     *
     * @see SecurityManager#checkConnect
     */
    // 返回本地IP，即服务端IP；如果没有绑定，则返回null
    public InetAddress getInetAddress() {
        if(!isBound()) {
            return null;
        }
        
        try {
            // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
            SocketImpl impl = getImpl();
            
            // 获取本地IP，即服务端IP
            InetAddress in = impl.getInetAddress();
            
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                sm.checkConnect(in.getHostAddress(), -1);
            }
            
            // 返回本地IP
            return in;
        } catch(SecurityException e) {
            return InetAddress.getLoopbackAddress();
        } catch(SocketException e) {
            // nothing,
            // If we're bound, the impl has been created,
            // so we shouldn't get here.
        }
        
        return null;
    }
    
    /**
     * Returns the port number on which this socket is listening.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return the port number
     * after the socket is closed.
     *
     * @return the port number to which this socket is listening or
     * -1 if the socket is not bound yet.
     */
    // 返回本地端口，即服务端的端口
    public int getLocalPort() {
        if(!isBound()) {
            return -1;
        }
        
        try {
            // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
            SocketImpl impl = getImpl();
            
            // 返回本地端口，即服务端的端口
            return impl.getLocalPort();
        } catch(SocketException e) {
            // nothing. If we're bound, the impl has been created so we shouldn't get here
        }
        
        return -1;
    }
    
    /*▲ 地址 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 标记[服务端Socket(监听)]已创建
    void setCreated() {
        created = true;
    }
    
    // 标记[服务端Socket(监听)]已绑定
    void setBound() {
        bound = true;
    }
    
    
    /**
     * Returns the binding state of the ServerSocket.
     *
     * @return true if the ServerSocket successfully bound to an address
     *
     * @since 1.4
     */
    // 判断[服务端Socket(监听)]是否已完成绑定
    public boolean isBound() {
        // Before 1.3 ServerSockets were always bound during creation
        return bound || oldImpl;    // JDK1.5起始，oldImpl总是为false
    }
    
    /**
     * Returns the closed state of the ServerSocket.
     *
     * @return true if the socket has been closed
     *
     * @since 1.4
     */
    // 判断[服务端Socket(监听)]是否已关闭
    public boolean isClosed() {
        synchronized(closeLock) {
            return closed;
        }
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Enable/disable {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT} with the
     * specified timeout, in milliseconds.  With this option set to a non-zero
     * timeout, a call to accept() for this ServerSocket
     * will block for only this amount of time.  If the timeout expires,
     * a <B>java.net.SocketTimeoutException</B> is raised, though the
     * ServerSocket is still valid.  The option <B>must</B> be enabled
     * prior to entering the blocking operation to have effect.  The
     * timeout must be {@code > 0}.
     * A timeout of zero is interpreted as an infinite timeout.
     *
     * @param timeout the specified timeout, in milliseconds
     *
     * @throws SocketException if there is an error in
     *                         the underlying protocol, such as a TCP error.
     * @see #getSoTimeout()
     * @since 1.1
     */
    // 设置超时约束的时间
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
    
        // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
        SocketImpl impl = getImpl();
    
        impl.setOption(SocketOptions.SO_TIMEOUT, timeout);
    }
    
    /**
     * Retrieve setting for {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT}.
     * 0 returns implies that the option is disabled (i.e., timeout of infinity).
     *
     * @return the {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT} value
     *
     * @throws IOException if an I/O error occurs
     * @see #setSoTimeout(int)
     * @since 1.1
     */
    // 获取超时约束的时间
    public synchronized int getSoTimeout() throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
    
        // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
        SocketImpl impl = getImpl();
    
        Object o = impl.getOption(SocketOptions.SO_TIMEOUT);
    
        /* extra type safety */
        if(o instanceof Integer) {
            return (Integer) o;
        } else {
            return 0;
        }
    }
    
    /**
     * Sets a default proposed value for the
     * {@link SocketOptions#SO_RCVBUF SO_RCVBUF} option for sockets
     * accepted from this {@code ServerSocket}. The value actually set
     * in the accepted socket must be determined by calling
     * {@link Socket#getReceiveBufferSize()} after the socket
     * is returned by {@link #accept()}.
     * <p>
     * The value of {@link SocketOptions#SO_RCVBUF SO_RCVBUF} is used both to
     * set the size of the internal socket receive buffer, and to set the size
     * of the TCP receive window that is advertized to the remote peer.
     * <p>
     * It is possible to change the value subsequently, by calling
     * {@link Socket#setReceiveBufferSize(int)}. However, if the application
     * wishes to allow a receive window larger than 64K bytes, as defined by RFC1323
     * then the proposed value must be set in the ServerSocket <B>before</B>
     * it is bound to a local address. This implies, that the ServerSocket must be
     * created with the no-argument constructor, then setReceiveBufferSize() must
     * be called and lastly the ServerSocket is bound to an address by calling bind().
     * <p>
     * Failure to do this will not cause an error, and the buffer size may be set to the
     * requested value but the TCP receive window in sockets accepted from
     * this ServerSocket will be no larger than 64K bytes.
     *
     * @param size the size to which to set the receive buffer
     *             size. This value must be greater than 0.
     *
     * @throws SocketException          if there is an error
     *                                  in the underlying protocol, such as a TCP error.
     * @throws IllegalArgumentException if the
     *                                  value is 0 or is negative.
     * @see #getReceiveBufferSize
     * @since 1.4
     */
    // 设置输入流缓冲区大小
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        if(!(size>0)) {
            throw new IllegalArgumentException("negative receive size");
        }
        
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
        SocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.SO_RCVBUF, size);
    }
    
    /**
     * Gets the value of the {@link SocketOptions#SO_RCVBUF SO_RCVBUF} option
     * for this {@code ServerSocket}, that is the proposed buffer size that
     * will be used for Sockets accepted from this {@code ServerSocket}.
     *
     * <p>Note, the value actually set in the accepted socket is determined by
     * calling {@link Socket#getReceiveBufferSize()}.
     *
     * @return the value of the {@link SocketOptions#SO_RCVBUF SO_RCVBUF}
     * option for this {@code Socket}.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #setReceiveBufferSize(int)
     * @since 1.4
     */
    // 获取输入流缓冲区大小
    public synchronized int getReceiveBufferSize() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        int result = 0;
        
        // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
        SocketImpl impl = getImpl();
        
        Object o = impl.getOption(SocketOptions.SO_RCVBUF);
        if(o instanceof Integer) {
            result = (Integer) o;
        }
        
        return result;
    }
    
    /**
     * Enable/disable the {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR}
     * socket option.
     * <p>
     * When a TCP connection is closed the connection may remain
     * in a timeout state for a period of time after the connection
     * is closed (typically known as the {@code TIME_WAIT} state
     * or {@code 2MSL} wait state).
     * For applications using a well known socket address or port
     * it may not be possible to bind a socket to the required
     * {@code SocketAddress} if there is a connection in the
     * timeout state involving the socket address or port.
     * <p>
     * Enabling {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} prior to
     * binding the socket using {@link #bind(SocketAddress)} allows the socket
     * to be bound even though a previous connection is in a timeout state.
     * <p>
     * When a {@code ServerSocket} is created the initial setting
     * of {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} is not defined.
     * Applications can use {@link #getReuseAddress()} to determine the initial
     * setting of {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR}.
     * <p>
     * The behaviour when {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} is
     * enabled or disabled after a socket is bound (See {@link #isBound()})
     * is not defined.
     *
     * @param on whether to enable or disable the socket option
     *
     * @throws SocketException if an error occurs enabling or
     *                         disabling the {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR}
     *                         socket option, or the socket is closed.
     * @see #getReuseAddress()
     * @see #bind(SocketAddress)
     * @see #isBound()
     * @see #isClosed()
     * @since 1.4
     */
    /*
     * 设置是否允许立刻重用已关闭的socket端口
     *
     * 比如在连接异常关闭后，端口可能还没有被释放。
     * 这时再次尝试绑定该端口时，如果没有启用SO_REUSEADDR，那么将会绑定失败。
     *
     * 需要注意的是setReuseAddress(boolean on)方法必须在socket还未绑定到一个本地端口之前调用，否则无效
     */
    public void setReuseAddress(boolean on) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
        SocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.SO_REUSEADDR, on);
    }
    
    /**
     * Tests if {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} is enabled.
     *
     * @return a {@code boolean} indicating whether or not
     * {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} is enabled.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #setReuseAddress(boolean)
     * @since 1.4
     */
    // 获取是否允许立刻重用已关闭的socket端口
    public boolean getReuseAddress() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
        SocketImpl impl = getImpl();
        
        return (Boolean) (impl.getOption(SocketOptions.SO_REUSEADDR));
    }
    
    
    /**
     * Returns a set of the socket options supported by this server socket.
     *
     * This method will continue to return the set of options even after
     * the socket has been closed.
     *
     * @return A set of the socket options supported by this socket. This set
     * may be empty if the socket's SocketImpl cannot be created.
     *
     * @since 9
     */
    // 获取ServerSocket可选参数
    public Set<SocketOption<?>> supportedOptions() {
        synchronized(ServerSocket.class) {
            if(optionsSet) {
                return options;
            }
            
            try {
                // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
                SocketImpl impl = getImpl();
                
                Set<SocketOption<?>> optionSet = impl.supportedOptions();
                
                options = Collections.unmodifiableSet(optionSet);
            } catch(IOException e) {
                options = Collections.emptySet();
            }
            
            optionsSet = true;
            
            return options;
        }
    }
    
    /**
     * Returns the value of a socket option.
     *
     * @param <T>  The type of the socket option value
     * @param name The socket option
     *
     * @return The value of the socket option.
     *
     * @throws UnsupportedOperationException if the server socket does not
     *                                       support the option.
     * @throws IOException                   if an I/O error occurs, or if the socket is closed.
     * @throws NullPointerException          if name is {@code null}
     * @throws SecurityException             if a security manager is set and if the socket
     *                                       option requires a security permission and if the caller does
     *                                       not have the required permission.
     *                                       {@link java.net.StandardSocketOptions StandardSocketOptions}
     *                                       do not require any security permission.
     * @since 9
     */
    // 获取指定名称的Socket配置参数
    public <T> T getOption(SocketOption<T> name) throws IOException {
        // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
        SocketImpl impl = getImpl();
        
        return impl.getOption(name);
    }
    
    /**
     * Sets the value of a socket option.
     *
     * @param <T>   The type of the socket option value
     * @param name  The socket option
     * @param value The value of the socket option. A value of {@code null}
     *              may be valid for some options.
     *
     * @return this ServerSocket
     *
     * @throws UnsupportedOperationException if the server socket does not
     *                                       support the option.
     * @throws IllegalArgumentException      if the value is not valid for
     *                                       the option.
     * @throws IOException                   if an I/O error occurs, or if the socket is closed.
     * @throws NullPointerException          if name is {@code null}
     * @throws SecurityException             if a security manager is set and if the socket
     *                                       option requires a security permission and if the caller does
     *                                       not have the required permission.
     *                                       {@link java.net.StandardSocketOptions StandardSocketOptions}
     *                                       do not require any security permission.
     * @since 9
     */
    // 设置指定名称的Socket配置参数
    public <T> ServerSocket setOption(SocketOption<T> name, T value) throws IOException {
        // 获取[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
        SocketImpl impl = getImpl();
        
        impl.setOption(name, value);
        
        return this;
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets performance preferences for this ServerSocket.
     *
     * <p> Sockets use the TCP/IP protocol by default.  Some implementations
     * may offer alternative protocols which have different performance
     * characteristics than TCP/IP.  This method allows the application to
     * express its own preferences as to how these tradeoffs should be made
     * when the implementation chooses from the available protocols.
     *
     * <p> Performance preferences are described by three integers
     * whose values indicate the relative importance of short connection time,
     * low latency, and high bandwidth.  The absolute values of the integers
     * are irrelevant; in order to choose a protocol the values are simply
     * compared, with larger values indicating stronger preferences.  If the
     * application prefers short connection time over both low latency and high
     * bandwidth, for example, then it could invoke this method with the values
     * {@code (1, 0, 0)}.  If the application prefers high bandwidth above low
     * latency, and low latency above short connection time, then it could
     * invoke this method with the values {@code (0, 1, 2)}.
     *
     * <p> Invoking this method after this socket has been bound
     * will have no effect. This implies that in order to use this capability
     * requires the socket to be created with the no-argument constructor.
     *
     * @param connectionTime An {@code int} expressing the relative importance of a short
     *                       connection time
     * @param latency        An {@code int} expressing the relative importance of low
     *                       latency
     * @param bandwidth      An {@code int} expressing the relative importance of high
     *                       bandwidth
     *
     * @since 1.5
     */
    // 设置SeverSocket性能参数
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        /* Not implemented yet */
    }
    
    /**
     * Sets the server socket implementation factory for the
     * application. The factory can be specified only once.
     * <p>
     * When an application creates a new server socket, the socket
     * implementation factory's {@code createSocketImpl} method is
     * called to create the actual socket implementation.
     * <p>
     * Passing {@code null} to the method is a no-op unless the factory
     * was already set.
     * <p>
     * If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param fac the desired factory.
     *
     * @throws IOException       if an I/O error occurs when setting the
     *                           socket factory.
     * @throws SocketException   if the factory has already been defined.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkSetFactory} method doesn't allow the operation.
     * @see java.net.SocketImplFactory#createSocketImpl()
     * @see SecurityManager#checkSetFactory
     */
    // 设置一个"Socket委托"工厂，以便用来构造"Socket委托"实例
    public static synchronized void setSocketFactory(SocketImplFactory fac) throws IOException {
        if(factory != null) {
            throw new SocketException("factory already defined");
        }
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkSetFactory();
        }
        
        factory = fac;
    }
    
    /**
     * Returns the unique {@link java.nio.channels.ServerSocketChannel} object associated with this socket, if any.
     *
     * A server socket will have a channel if, and only if,
     * the channel itself was created via the {@link java.nio.channels.ServerSocketChannel#open ServerSocketChannel.open} method.
     *
     * @return the server-socket channel associated with this socket,
     * or {@code null} if this socket was not created for a channel
     *
     * @spec JSR-51
     * @since 1.4
     */
    /*
     * 返回当前ServerSocket关联的通道
     *
     * 这里返回null，因为直接创建出来的Socket不会关联通道，
     * 可以通过ServerSocketChannel#open()一边创建Socket，一边关联通道。
     */
    public ServerSocketChannel getChannel() {
        return null;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 检查是否使用了旧式"Socket委托"(JDK1.5开始，均不是旧式委托)
    private void checkOldImpl() {
        if(impl == null) {
            return;
        }
        
        // SocketImpl.connect() is a protected method, therefore we need to use getDeclaredMethod, therefore we need permission to access the member
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws NoSuchMethodException {
                    // 如果包含void connect(SocketAddress, int)方法，则不属于旧式委托
                    impl.getClass().getDeclaredMethod("connect", SocketAddress.class, int.class);
                    return null;
                }
            });
        } catch(PrivilegedActionException e) {
            oldImpl = true;
        }
    }
    
    // (1)创建"Socket委托"，并为其关联当前ServerSocket
    private void setImpl() {
        // 如果存在"Socket委托"工厂
        if(factory != null) {
            // 从"Socket委托"工厂生成"Socket委托"
            impl = factory.createSocketImpl();
            // 检查是否为旧式"Socket委托"
            checkOldImpl();
        } else {
            // No need to do a checkOldImpl() here, we know it's an up to date SocketImpl!
            impl = new SocksSocketImpl();
        }
        
        if(impl != null) {
            // 为"Socket委托"关联ServerSocket
            impl.setServerSocket(this);
        }
    }
    
    /**
     * Creates the socket implementation.
     *
     * @throws IOException if creation fails
     * @since 1.4
     */
    // (2)创建[服务端Socket(监听)]
    void createImpl() throws SocketException {
        if(impl == null) {
            setImpl();  // 创建"Socket委托"，并为其关联当前ServerSocket
        }
        
        try {
            // 创建[服务端Socket(监听)]，并将其文件描述符记录到impl中
            impl.create(true);
            created = true;
        } catch(IOException e) {
            throw new SocketException(e.getMessage());
        }
    }
    
    /**
     * Get the {@code SocketImpl} attached to this socket, creating it if necessary.
     *
     * @return the {@code SocketImpl} attached to that ServerSocket.
     *
     * @throws SocketException if creation fails.
     * @since 1.4
     */
    // (3)返回[服务端Socket(监听)]的"Socket委托"，期间已创建了[服务端Socket(监听)]
    SocketImpl getImpl() throws SocketException {
        if(!created) {
            // 创建"Socket委托"，随后通过该委托创建[服务端Socket(监听)]
            createImpl();
        }
        
        return impl;
    }
    
    
    /**
     * Returns the implementation address and implementation port of
     * this socket as a {@code String}.
     * <p>
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * an {@code InetAddress} representing the
     * {@link InetAddress#getLoopbackAddress loopback} address is returned as
     * the implementation address.
     *
     * @return a string representation of this socket.
     */
    @Override
    public String toString() {
        if(!isBound()) {
            return "ServerSocket[unbound]";
        }
        
        InetAddress in;
        if(System.getSecurityManager() != null) {
            in = InetAddress.getLoopbackAddress();
        } else {
            in = impl.getInetAddress();
        }
        
        return "ServerSocket[addr=" + in + ",localport=" + impl.getLocalPort() + "]";
    }
    
}
