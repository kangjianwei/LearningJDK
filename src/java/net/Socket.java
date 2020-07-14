/*
 * Copyright (c) 1995, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Set;
import sun.net.ApplicationProxy;

/**
 * This class implements client sockets (also called just "sockets").
 * A socket is an endpoint for communication between two machines.
 * <p>
 * The actual work of the socket is performed by an instance of the {@code SocketImpl} class.
 * An application, by changing the socket factory that creates the socket implementation,
 * can configure itself to create sockets appropriate to the local firewall.
 *
 * @author unascribed
 * @see java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
 * @see java.net.SocketImpl
 * @see java.nio.channels.SocketChannel
 * @since 1.0
 */
/*
 * 面向连接的Socket(使用TCP Socket)，用在客户端与服务端
 *
 * Socket用于在客户端和服务端之间进行通信，
 * 该类的实例位于客户端时，我将其称为：[客户端Socket]，
 * 该类的实例位于服务端时，我将其称为：[服务端Socket]，
 *
 * 对于[服务端Socket]，又分为两类：
 * 其中一类用来监听客户端的连接请求，我将其称为[服务端Socket(监听)]，
 * 还有一类用来与[客户端Socket]进行通信，我将其称为[服务端Socket(通信)]，
 * 对于同一个监听地址，[服务端Socket(监听)]只有一个，而[服务端Socket(通信)]会有多个。
 *
 * 注：当存在反向代理时，[服务端Socket(通信)]会与代理端的Socket通信。
 *
 * 特别注意的是，当前语境下，[服务端Socket]与ServerSocket要做区分。
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
public class Socket implements Closeable {
    
    /**
     * The implementation of this Socket.
     */
    /*
     * [客户端Socket]和[服务端Socket(通信)]的"Socket委托"，用来与远端通信
     *
     * 注：[客户端Socket]与[服务端Socket(通信)]互为远端
     */ SocketImpl impl;
    
    /**
     * Are we using an older SocketImpl?
     */
    // 是否在使用旧式的"Socket委托"(JDK1.5起始，该值均为false)
    private boolean oldImpl = false;
    
    /**
     * The factory for all client sockets.
     */
    // "Socket委托"的工厂
    private static SocketImplFactory factory = null;
    
    /**
     * Various states of this socket.
     */
    private boolean created = false;  // 指示[客户端Socket]/[服务端Socket(通信)]是否已创建
    private boolean bound = false;  // 指示[客户端Socket]/[服务端Socket(通信)]是否已绑定
    private boolean connected = false;  // 指示[客户端Socket]/[服务端Socket(通信)]是否已连接
    
    private boolean closed = false;  // 指示socket连接是否已关闭
    
    private boolean shutIn = false;    // 是否关闭了读取功能
    private boolean shutOut = false;    // 是否关闭了写入功能
    
    private static Set<SocketOption<?>> options;    // Socket配置参数
    private static boolean optionsSet = false;      // 懒加载，标记是否初始化了Socket配置参数
    
    private Object closeLock = new Object();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * ▶ 1
     *
     * 构造一个[客户端Socket]，并对其执行【bind】和【connect】操作
     *
     * 【bind】过程是可选的，只有指定了非空的localAddr才显式执行【bind】过程
     *
     * address  : [服务端Socket]地址
     * localAddr: 客户端待绑定的本地地址
     * stream   : 创建TCP(true)/UDP(false)连接
     */
    private Socket(SocketAddress address, SocketAddress localAddr, boolean stream) throws IOException {
        // 初始化Socket的委托，并为其关联客户端Socket
        setImpl();
        
        // backward compatibility
        if(address == null) {
            throw new NullPointerException();
        }
        
        try {
            // 创建[客户端Socket]文件，并记下其文件描述符
            createImpl(stream);
            
            // 如果设置了本地socket地址，则将其绑定到[客户端Socket]上
            if(localAddr != null) {
                // 对[客户端Socket]执行【bind】操作
                bind(localAddr);
            }
            
            connect(address);
        } catch(IOException | IllegalArgumentException | SecurityException e) {
            try {
                close();
            } catch(IOException ce) {
                e.addSuppressed(ce);
            }
            throw e;
        }
    }
    
    /**
     * Creates a stream socket and connects it to the specified port
     * number on the named host.
     * <p>
     * If the specified host is {@code null} it is the equivalent of
     * specifying the address as
     * {@link java.net.InetAddress#getByName InetAddress.getByName}{@code (null)}.
     * In other words, it is equivalent to specifying an address of the
     * loopback interface. </p>
     * <p>
     * If the application has specified a server socket factory, that
     * factory's {@code createSocketImpl} method is called to create
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager, its
     * {@code checkConnect} method is called
     * with the host address and {@code port}
     * as its arguments. This could result in a SecurityException.
     *
     * @param host the host name, or {@code null} for the loopback address.
     * @param port the port number.
     *
     * @throws UnknownHostException     if the IP address of
     *                                  the host could not be determined.
     * @throws IOException              if an I/O error occurs when creating the socket.
     * @throws SecurityException        if a security manager exists and its
     *                                  {@code checkConnect} method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside
     *                                  the specified range of valid port values, which is between
     *                                  0 and 65535, inclusive.
     * @see java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see java.net.SocketImpl
     * @see java.net.SocketImplFactory#createSocketImpl()
     * @see SecurityManager#checkConnect
     */
    /*
     * ▶ 1-1
     *
     * 构造一个[客户端Socket](TCP)，并对其执行【connect】操作
     *
     * host: 待连接的远端域名/地址；如果host为null，则默认连接到本地(环回地址)
     * port: 待连接的远端端口
     */
    public Socket(String host, int port) throws UnknownHostException, IOException {
        this(host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(InetAddress.getByName(null), port), null, true);
    }
    
    /**
     * Creates a stream socket and connects it to the specified port
     * number at the specified IP address.
     * <p>
     * If the application has specified a socket factory, that factory's
     * {@code createSocketImpl} method is called to create the
     * actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager, its
     * {@code checkConnect} method is called
     * with the host address and {@code port}
     * as its arguments. This could result in a SecurityException.
     *
     * @param address the IP address.
     * @param port    the port number.
     *
     * @throws IOException              if an I/O error occurs when creating the socket.
     * @throws SecurityException        if a security manager exists and its
     *                                  {@code checkConnect} method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside
     *                                  the specified range of valid port values, which is between
     *                                  0 and 65535, inclusive.
     * @throws NullPointerException     if {@code address} is null.
     * @see java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see java.net.SocketImpl
     * @see java.net.SocketImplFactory#createSocketImpl()
     * @see SecurityManager#checkConnect
     */
    /*
     * ▶ 1-2
     *
     * 构造一个[客户端Socket](TCP)，并对其执行【connect】操作
     *
     * address: 待连接的远端IP；如果address为null，则抛出异常
     * port   : 待连接的远端端口
     */
    public Socket(InetAddress address, int port) throws IOException {
        this(address != null ? new InetSocketAddress(address, port) : null, null, true);
    }
    
    /**
     * Creates a socket and connects it to the specified remote host on
     * the specified remote port. The Socket will also bind() to the local
     * address and port supplied.
     * <p>
     * If the specified host is {@code null} it is the equivalent of
     * specifying the address as
     * {@link java.net.InetAddress#getByName InetAddress.getByName}{@code (null)}.
     * In other words, it is equivalent to specifying an address of the
     * loopback interface. </p>
     * <p>
     * A local port number of {@code zero} will let the system pick up a
     * free port in the {@code bind} operation.</p>
     * <p>
     * If there is a security manager, its
     * {@code checkConnect} method is called
     * with the host address and {@code port}
     * as its arguments. This could result in a SecurityException.
     *
     * @param host      the name of the remote host, or {@code null} for the loopback address.
     * @param port      the remote port
     * @param localAddr the local address the socket is bound to, or
     *                  {@code null} for the {@code anyLocal} address.
     * @param localPort the local port the socket is bound to, or
     *                  {@code zero} for a system selected free port.
     *
     * @throws IOException              if an I/O error occurs when creating the socket.
     * @throws SecurityException        if a security manager exists and its
     *                                  {@code checkConnect} method doesn't allow the connection
     *                                  to the destination, or if its {@code checkListen} method
     *                                  doesn't allow the bind to the local port.
     * @throws IllegalArgumentException if the port parameter or localPort
     *                                  parameter is outside the specified range of valid port values,
     *                                  which is between 0 and 65535, inclusive.
     * @see SecurityManager#checkConnect
     * @since 1.1
     */
    /*
     * ▶ 1-3
     *
     * 构造一个[客户端Socket](TCP)，并对其执行【bind】和【connect】操作
     *
     * host     : 待连接的远端域名/地址；如果host为null，则默认连接到本地(环回地址)
     * port     : 待连接的远端端口
     * localAddr: 客户端待绑定的本地地址
     */
    public Socket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        this(host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(InetAddress.getByName(null), port), new InetSocketAddress(localAddr, localPort), true);
    }
    
    /**
     * Creates a socket and connects it to the specified remote address on
     * the specified remote port. The Socket will also bind() to the local
     * address and port supplied.
     * <p>
     * If the specified local address is {@code null} it is the equivalent of
     * specifying the address as the AnyLocal address
     * (see {@link java.net.InetAddress#isAnyLocalAddress InetAddress.isAnyLocalAddress}{@code ()}).
     * <p>
     * A local port number of {@code zero} will let the system pick up a
     * free port in the {@code bind} operation.</p>
     * <p>
     * If there is a security manager, its
     * {@code checkConnect} method is called
     * with the host address and {@code port}
     * as its arguments. This could result in a SecurityException.
     *
     * @param address   the remote address
     * @param port      the remote port
     * @param localAddr the local address the socket is bound to, or
     *                  {@code null} for the {@code anyLocal} address.
     * @param localPort the local port the socket is bound to or
     *                  {@code zero} for a system selected free port.
     *
     * @throws IOException              if an I/O error occurs when creating the socket.
     * @throws SecurityException        if a security manager exists and its
     *                                  {@code checkConnect} method doesn't allow the connection
     *                                  to the destination, or if its {@code checkListen} method
     *                                  doesn't allow the bind to the local port.
     * @throws IllegalArgumentException if the port parameter or localPort
     *                                  parameter is outside the specified range of valid port values,
     *                                  which is between 0 and 65535, inclusive.
     * @throws NullPointerException     if {@code address} is null.
     * @see SecurityManager#checkConnect
     * @since 1.1
     */
    /*
     * ▶ 1-4
     *
     * 构造一个[客户端Socket](TCP)，并对其执行【bind】和【connect】操作
     *
     * address  : 待连接的远端IP；如果address为null，则抛出异常
     * port     : 待连接的远端端口
     * localAddr: 客户端待绑定的本地地址
     */
    public Socket(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
        this(address != null ? new InetSocketAddress(address, port) : null, new InetSocketAddress(localAddr, localPort), true);
    }
    
    /**
     * Creates a stream socket and connects it to the specified port
     * number on the named host.
     * <p>
     * If the specified host is {@code null} it is the equivalent of
     * specifying the address as
     * {@link java.net.InetAddress#getByName InetAddress.getByName}{@code (null)}.
     * In other words, it is equivalent to specifying an address of the
     * loopback interface. </p>
     * <p>
     * If the stream argument is {@code true}, this creates a
     * stream socket. If the stream argument is {@code false}, it
     * creates a datagram socket.
     * <p>
     * If the application has specified a server socket factory, that
     * factory's {@code createSocketImpl} method is called to create
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager, its
     * {@code checkConnect} method is called
     * with the host address and {@code port}
     * as its arguments. This could result in a SecurityException.
     * <p>
     * If a UDP socket is used, TCP/IP related socket options will not apply.
     *
     * @param host   the host name, or {@code null} for the loopback address.
     * @param port   the port number.
     * @param stream a {@code boolean} indicating whether this is
     *               a stream socket or a datagram socket.
     *
     * @throws IOException              if an I/O error occurs when creating the socket.
     * @throws SecurityException        if a security manager exists and its
     *                                  {@code checkConnect} method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside
     *                                  the specified range of valid port values, which is between
     *                                  0 and 65535, inclusive.
     * @see java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see java.net.SocketImpl
     * @see java.net.SocketImplFactory#createSocketImpl()
     * @see SecurityManager#checkConnect
     *
     * @deprecated Use DatagramSocket instead for UDP transport.
     */
    /*
     * ▶ 1-5
     *
     * 构造一个[客户端Socket]，并对其执行【connect】操作
     *
     * host  : 待连接的远端域名/地址；如果host为null，则默认连接到本地(环回地址)
     * port  : 待连接的远端端口
     * stream: 创建TCP(true)/UDP(false)连接
     *
     * ※ 该方法已过时
     */
    @Deprecated
    public Socket(String host, int port, boolean stream) throws IOException {
        this(host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(InetAddress.getByName(null), port), null, stream);
    }
    
    /**
     * Creates a socket and connects it to the specified port number at
     * the specified IP address.
     * <p>
     * If the stream argument is {@code true}, this creates a
     * stream socket. If the stream argument is {@code false}, it
     * creates a datagram socket.
     * <p>
     * If the application has specified a server socket factory, that
     * factory's {@code createSocketImpl} method is called to create
     * the actual socket implementation. Otherwise a "plain" socket is created.
     *
     * <p>If there is a security manager, its
     * {@code checkConnect} method is called
     * with {@code host.getHostAddress()} and {@code port}
     * as its arguments. This could result in a SecurityException.
     * <p>
     * If UDP socket is used, TCP/IP related socket options will not apply.
     *
     * @param host   the IP address.
     * @param port   the port number.
     * @param stream if {@code true}, create a stream socket;
     *               otherwise, create a datagram socket.
     *
     * @throws IOException              if an I/O error occurs when creating the socket.
     * @throws SecurityException        if a security manager exists and its
     *                                  {@code checkConnect} method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside
     *                                  the specified range of valid port values, which is between
     *                                  0 and 65535, inclusive.
     * @throws NullPointerException     if {@code host} is null.
     * @see java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see java.net.SocketImpl
     * @see java.net.SocketImplFactory#createSocketImpl()
     * @see SecurityManager#checkConnect
     *
     * @deprecated Use DatagramSocket instead for UDP transport.
     */
    /*
     * ▶ 1-6
     *
     * 构造一个[客户端Socket]，并对其执行【bind】和【connect】操作
     *
     * address: 待连接的远端IP；如果address为null，则抛出异常
     * port   : 待连接的远端端口
     * stream : 创建TCP(true)/UDP(false)连接
     *
     * ※ 该方法已过时
     */
    @Deprecated
    public Socket(InetAddress address, int port, boolean stream) throws IOException {
        this(address != null ? new InetSocketAddress(address, port) : null, new InetSocketAddress(0), stream);
    }
    
    /**
     * Creates an unconnected socket, with the
     * system-default type of SocketImpl.
     *
     * @revised 1.4
     * @since 1.1
     */
    // ▶ 2 构造一个未连接的Socket（会初始化其"Socket委托"）
    public Socket() {
        // 初始化"Socket委托"，并为其关联客户端Socket
        setImpl();
    }
    
    /**
     * Creates an unconnected socket, specifying the type of proxy, if any,
     * that should be used regardless of any other settings.
     * <P>
     * If there is a security manager, its {@code checkConnect} method
     * is called with the proxy host address and port number
     * as its arguments. This could result in a SecurityException.
     * <P>
     * Examples:
     * <UL> <LI>{@code Socket s = new Socket(Proxy.NO_PROXY);} will create
     * a plain socket ignoring any other proxy configuration.</LI>
     * <LI>{@code Socket s = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("socks.mydom.com", 1080)));}
     * will create a socket connecting through the specified SOCKS proxy
     * server.</LI>
     * </UL>
     *
     * @param proxy a {@link java.net.Proxy Proxy} object specifying what kind
     *              of proxying should be used.
     *
     * @throws IllegalArgumentException if the proxy is of an invalid type
     *                                  or {@code null}.
     * @throws SecurityException        if a security manager is present and
     *                                  permission to connect to the proxy is
     *                                  denied.
     * @see java.net.ProxySelector
     * @see java.net.Proxy
     * @since 1.5
     */
    /*
     * ▶ 3 使用指定的代理构造一个未连接的Socket（会初始化其"Socket委托"）
     *
     * 该Socket使用的"Socket委托"可能是：
     * PlainSocketImpl          - 无代理
     * SocksSocketImpl          - Socket代理
     * HttpConnectSocketImpl(p) - HTTP Socket代理
     */
    public Socket(Proxy proxy) {
        // Create a copy of Proxy as a security measure
        if(proxy == null) {
            throw new IllegalArgumentException("Invalid Proxy");
        }
        
        // 通过工厂方法，创建代理对象
        Proxy p = (proxy == Proxy.NO_PROXY) ? Proxy.NO_PROXY : ApplicationProxy.create(proxy);
        
        // 获取代理类型
        Proxy.Type type = p.type();
        
        // 如果使用了有效的代理
        if(type == Proxy.Type.SOCKS || type == Proxy.Type.HTTP) {
            
            // 获取代理地址
            InetSocketAddress epoint = (InetSocketAddress) p.address();
            if(epoint.getAddress() != null) {
                checkAddress(epoint.getAddress(), "Socket");
            }
            
            SecurityManager security = System.getSecurityManager();
            if(security != null) {
                if(epoint.isUnresolved()) {
                    epoint = new InetSocketAddress(epoint.getHostName(), epoint.getPort());
                }
                
                if(epoint.isUnresolved()) {
                    security.checkConnect(epoint.getHostName(), epoint.getPort());
                } else {
                    security.checkConnect(epoint.getAddress().getHostAddress(), epoint.getPort());
                }
            }
            
            impl = (type == Proxy.Type.SOCKS) ? new SocksSocketImpl(p)            // 创建Socket(V4 & V5)实现
                : new HttpConnectSocketImpl(p);     // 创建HTTP Socket实现
            
            // 为"Socket委托"关联本地Socket
            impl.setSocket(this);
            
            // 如果没有使用代理（NO_PROXY等同于没有代理）
        } else {
            if(p == Proxy.NO_PROXY) {
                if(factory == null) {
                    impl = new PlainSocketImpl();   // 创建普通的Socket实现
                    // 为"Socket委托"关联本地Socket
                    impl.setSocket(this);
                } else {
                    // 初始化客户端Socket的委托，并为其关联客户端Socket
                    setImpl();
                }
            } else {
                throw new IllegalArgumentException("Invalid Proxy");
            }
        }
    }
    
    /**
     * Creates an unconnected Socket with a user-specified SocketImpl.
     *
     * @param impl an instance of a <B>SocketImpl</B> the subclass wishes to use on the Socket.
     *
     * @throws SocketException if there is an error in the underlying protocol, such as a TCP error.
     * @since 1.1
     */
    /*
     * ▶ 4 使用指定的"Socket委托"构造一个未连接的Socket
     *
     * 注：此构造器往往用来创建[服务端Socket(通信)]
     */
    protected Socket(SocketImpl impl) throws SocketException {
        this.impl = impl;
        
        if(impl != null) {
            checkOldImpl();
            // 为"Socket委托"关联客户端Socket
            this.impl.setSocket(this);
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Binds the socket to a local address.
     * <P>
     * If the address is {@code null}, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     *
     * @param bindpoint the {@code SocketAddress} to bind to
     *
     * @throws IOException              if the bind operation fails, or if the socket
     *                                  is already bound.
     * @throws IllegalArgumentException if bindpoint is a
     *                                  SocketAddress subclass not supported by this socket
     * @throws SecurityException        if a security manager exists and its
     *                                  {@code checkListen} method doesn't allow the bind
     *                                  to the local port.
     * @see #isBound
     * @since 1.4
     */
    /*
     * 对[客户端Socket]执行【bind】操作。
     *
     * bindpoint: 待绑定的地址(ip+port)
     *
     * 注：[服务端Socket(通信)]会在accept期间完成绑定，故不会也不应再调用此方法
     */
    public void bind(SocketAddress bindpoint) throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        if(!oldImpl && isBound()) {
            throw new SocketException("Already bound");
        }
        
        if(bindpoint != null && (!(bindpoint instanceof InetSocketAddress))) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        
        // 对本地地址进行强转
        InetSocketAddress epoint = (InetSocketAddress) bindpoint;
        
        if(epoint != null && epoint.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        
        if(epoint == null) {
            epoint = new InetSocketAddress(0);
        }
        
        InetAddress addr = epoint.getAddress(); // 获取待绑定的本地IP
        int port = epoint.getPort();            // 获取待绑定的本地端口号
        
        checkAddress(addr, "bind");
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkListen(port);
        }
        
        /*
         * 获取[客户端Socket]的"Socket委托"
         *
         * 注：[服务端Socket(通信)]会在accept期间完成绑定
         */
        SocketImpl impl = getImpl();
        
        // 为指定的"Socket委托"绑定IP与端口号
        impl.bind(addr, port);
        
        bound = true;
    }
    
    /**
     * Connects this socket to the server.
     *
     * @param endpoint the {@code SocketAddress}
     *
     * @throws IOException                                    if an error occurs during the connection
     * @throws java.nio.channels.IllegalBlockingModeException if this socket has an associated channel,
     *                                                        and the channel is in non-blocking mode
     * @throws IllegalArgumentException                       if endpoint is null or is a
     *                                                        SocketAddress subclass not supported by this socket
     * @spec JSR-51
     * @since 1.4
     */
    /*
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；如果远端还未就绪，则立即返回。
     *
     *                  本地Socket            远端Socket
     * 1.不存在代理     [客户端Socket]       [服务端Socket(通信)]
     * 2.存在正向代理   [客户端Socket]        代理端Socket
     * 3.存在反向代理   [服务端Socket(通信)]  代理端Socket
     *
     * endpoint: 在情形1和情形2下，该参数为[服务端Socket(通信)]地址；在情形3下，该参数为代理端Socket地址。
     *
     * 注：当不存在代理时，直接调用此方法完成[客户端Socket]到[服务端Socket(通信)]的连接；
     * 　　当存在正向代理时，依然需要按照不存在代理时的情形调用此方法，但是在系统内部，实际完成的是[客户端Socket]到代理端Socket的连接；
     * 　　当存在反向代理时，该方法会被【间接调用】，以完成[服务端Socket(通信)]到代理端Socket的连接
     */
    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, 0);
    }
    
    /**
     * Connects this socket to the server with a specified timeout value.
     * A timeout of zero is interpreted as an infinite timeout. The connection
     * will then block until established or an error occurs.
     *
     * @param endpoint the {@code SocketAddress}
     * @param timeout  the timeout value to be used in milliseconds.
     *
     * @throws IOException                                    if an error occurs during the connection
     * @throws SocketTimeoutException                         if timeout expires before connecting
     * @throws java.nio.channels.IllegalBlockingModeException if this socket has an associated channel,
     *                                                        and the channel is in non-blocking mode
     * @throws IllegalArgumentException                       if endpoint is null or is a
     *                                                        SocketAddress subclass not supported by this socket
     * @spec JSR-51
     * @since 1.4
     */
    /*
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；允许指定超时，以便等待远端就绪。
     *
     *                  本地Socket            远端Socket
     * 1.不存在代理     [客户端Socket]       [服务端Socket(通信)]
     * 2.存在正向代理   [客户端Socket]        代理端Socket
     * 3.存在反向代理   [服务端Socket(通信)]  代理端Socket
     *
     * endpoint: 远端地址；在情形1和情形2下，该参数为[服务端Socket(通信)]地址；在情形3下，该参数为代理端Socket地址
     * timeout : 超时时间，即允许连接等待的时间
     *
     * 注：当不存在代理时，直接调用此方法完成[客户端Socket]到[服务端Socket(通信)]的连接；
     * 　　当存在正向代理时，依然需要按照不存在代理时的情形调用此方法，但是在系统内部，实际完成的是[客户端Socket]到代理端Socket的连接；
     * 　　当存在反向代理时，该方法会被【间接调用】，以完成[服务端Socket(通信)]到代理端Socket的连接
     */
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if(endpoint == null) {
            throw new IllegalArgumentException("connect: The address can't be null");
        }
        
        if(timeout<0) {
            throw new IllegalArgumentException("connect: timeout can't be negative");
        }
        
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        if(!oldImpl && isConnected()) {
            throw new SocketException("already connected");
        }
        
        if(!(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        
        // 记录远端的Socket地址(ip + port)
        InetSocketAddress epoint = (InetSocketAddress) endpoint;
        
        // 获取远端的IP
        InetAddress addr = epoint.getAddress();
        
        // 获取远端的端口号
        int port = epoint.getPort();
        
        checkAddress(addr, "connect");
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            if(epoint.isUnresolved()) {
                security.checkConnect(epoint.getHostName(), port);
            } else {
                security.checkConnect(addr.getHostAddress(), port);
            }
        }
        
        /*
         * 如果[客户端Socket]/[服务端Socket(通信)]还未创建
         * 在"正常"使用中，created为false出现于存在反向代理的情形下
         */
        if(!created) {
            // 创建[客户端Socket]/[服务端Socket(通信)]文件，并记下其文件描述符；true指示创建的是TCP Socket
            createImpl(true);
        }
        
        // 如果不是使用旧式的"Socket委托"(JDK1.5起始，oldImpl总是为false)
        if(!oldImpl) {
            impl.connect(epoint, timeout);
            
            // 处理旧式"Socket委托"
        } else if(timeout == 0) {
            if(epoint.isUnresolved()) {
                impl.connect(addr.getHostName(), port);
            } else {
                impl.connect(addr, port);
            }
        } else {
            throw new UnsupportedOperationException("SocketImpl.connect(addr, timeout)");
        }
        
        connected = true;
        
        /*
         * If the socket was not bound before the connect, it is now because
         * the kernel will have picked an ephemeral port & a local address
         */
        bound = true;
    }
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an input stream for this socket.
     *
     * <p> If this socket has an associated channel then the resulting input
     * stream delegates all of its operations to the channel.  If the channel
     * is in non-blocking mode then the input stream's {@code read} operations
     * will throw an {@link java.nio.channels.IllegalBlockingModeException}.
     *
     * <p>Under abnormal conditions the underlying connection may be
     * broken by the remote host or the network software (for example
     * a connection reset in the case of TCP connections). When a
     * broken connection is detected by the network software the
     * following applies to the returned input stream :-
     *
     * <ul>
     *
     * <li><p>The network software may discard bytes that are buffered
     * by the socket. Bytes that aren't discarded by the network
     * software can be read using {@link java.io.InputStream#read read}.
     *
     * <li><p>If there are no bytes buffered on the socket, or all
     * buffered bytes have been consumed by
     * {@link java.io.InputStream#read read}, then all subsequent
     * calls to {@link java.io.InputStream#read read} will throw an
     * {@link java.io.IOException IOException}.
     *
     * <li><p>If there are no bytes buffered on the socket, and the
     * socket has not been closed using {@link #close close}, then
     * {@link java.io.InputStream#available available} will
     * return {@code 0}.
     *
     * </ul>
     *
     * <p> Closing the returned {@link java.io.InputStream InputStream}
     * will close the associated socket.
     *
     * @return an input stream for reading bytes from this socket.
     *
     * @throws IOException if an I/O error occurs when creating the
     *                     input stream, the socket is closed, the socket is
     *                     not connected, or the socket input has been shutdown
     *                     using {@link #shutdownInput()}
     * @revised 1.4
     * @spec JSR-51
     */
    // 获取Socket输入流，从中读取数据
    public InputStream getInputStream() throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
    
        if(!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
    
        if(isInputShutdown()) {
            throw new SocketException("Socket input is shutdown");
        }
    
        InputStream is = null;
        try {
            is = AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                public InputStream run() throws IOException {
                    return impl.getInputStream();
                }
            });
        } catch(PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    
        return is;
    }
    
    /**
     * Returns an output stream for this socket.
     *
     * <p> If this socket has an associated channel then the resulting output
     * stream delegates all of its operations to the channel.  If the channel
     * is in non-blocking mode then the output stream's {@code write}
     * operations will throw an {@link
     * java.nio.channels.IllegalBlockingModeException}.
     *
     * <p> Closing the returned {@link java.io.OutputStream OutputStream}
     * will close the associated socket.
     *
     * @return an output stream for writing bytes to this socket.
     *
     * @throws IOException if an I/O error occurs when creating the
     *                     output stream or if the socket is not connected.
     * @revised 1.4
     * @spec JSR-51
     */
    // 获取Socket输出流，向其写入数据
    public OutputStream getOutputStream() throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
    
        if(!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
    
        if(isOutputShutdown()) {
            throw new SocketException("Socket output is shutdown");
        }
    
        OutputStream os = null;
        try {
            os = AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                public OutputStream run() throws IOException {
                    return impl.getOutputStream();
                }
            });
        } catch(PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    
        return os;
    }
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes this socket.
     * <p>
     * Any thread currently blocked in an I/O operation upon this socket
     * will throw a {@link SocketException}.
     * <p>
     * Once a socket has been closed, it is not available for further networking
     * use (i.e. can't be reconnected or rebound). A new socket needs to be
     * created.
     *
     * <p> Closing this socket will also close the socket's
     * {@link java.io.InputStream InputStream} and
     * {@link java.io.OutputStream OutputStream}.
     *
     * <p> If this socket has an associated channel then the channel is closed
     * as well.
     *
     * @throws IOException if an I/O error occurs when closing this socket.
     * @revised 1.4
     * @spec JSR-51
     * @see #isClosed
     */
    // 关闭socket连接
    public synchronized void close() throws IOException {
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
    
    /**
     * Places the input stream for this socket at "end of stream".
     * Any data sent to the input stream side of the socket is acknowledged and then silently discarded.
     * <p>
     * If you read from a socket input stream after invoking this method on the socket,
     * the stream's {@code available} method will return 0, and its {@code read} methods will return {@code -1} (end of stream).
     *
     * @throws IOException if an I/O error occurs when shutting down this socket.
     * @see java.net.Socket#shutdownOutput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @see #isInputShutdown
     * @since 1.3
     */
    // 关闭读取功能
    public void shutdownInput() throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        if(!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        
        if(isInputShutdown()) {
            throw new SocketException("Socket input is already shutdown");
        }
        
        // 获取[客户端Socket]/[服务端Socket(通信)]的"Socket委托"
        SocketImpl impl = getImpl();
        
        // 结束Socket输入，后续的read()会返回-1
        impl.shutdownInput();
        
        shutIn = true;
    }
    
    /**
     * Disables the output stream for this socket.
     * For a TCP socket, any previously written data will be sent
     * followed by TCP's normal connection termination sequence.
     *
     * If you write to a socket output stream after invoking
     * shutdownOutput() on the socket, the stream will throw
     * an IOException.
     *
     * @throws IOException if an I/O error occurs when shutting down this
     *                     socket.
     * @see java.net.Socket#shutdownInput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @see #isOutputShutdown
     * @since 1.3
     */
    // 关闭写入功能
    public void shutdownOutput() throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        if(!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        
        if(isOutputShutdown()) {
            throw new SocketException("Socket output is already shutdown");
        }
        
        // 获取[客户端Socket]/[服务端Socket(通信)]的"Socket委托"
        SocketImpl impl = getImpl();
        
        // 结束Socket输出，后续的write()会抛出异常
        impl.shutdownOutput();
        
        shutOut = true;
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the address of the endpoint this socket is bound to.
     * <p>
     * If a socket bound to an endpoint represented by an
     * {@code InetSocketAddress } is {@link #close closed},
     * then this method will continue to return an {@code InetSocketAddress}
     * after the socket is closed. In that case the returned
     * {@code InetSocketAddress}'s address is the
     * {@link InetAddress#isAnyLocalAddress wildcard} address
     * and its port is the local port that it was bound to.
     * <p>
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * a {@code SocketAddress} representing the
     * {@link InetAddress#getLoopbackAddress loopback} address and the local
     * port to which this socket is bound is returned.
     *
     * @return a {@code SocketAddress} representing the local endpoint of
     * this socket, or a {@code SocketAddress} representing the
     * loopback address if denied by the security manager, or
     * {@code null} if the socket is not bound yet.
     *
     * @see #getLocalAddress()
     * @see #getLocalPort()
     * @see #bind(SocketAddress)
     * @see SecurityManager#checkConnect
     * @since 1.4
     */
    // 返回本地Socket地址(ip+port)
    public SocketAddress getLocalSocketAddress() {
        if(!isBound()) {
            return null;
        }
        
        // 获取本地IP
        InetAddress address = getLocalAddress();
        // 获取本地端口
        int port = getLocalPort();
        
        return new InetSocketAddress(address, port);
    }
    
    /**
     * Gets the local address to which the socket is bound.
     * <p>
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * the {@link InetAddress#getLoopbackAddress loopback} address is returned.
     *
     * @return the local address to which the socket is bound,
     * the loopback address if denied by the security manager, or
     * the wildcard address if the socket is closed or not bound yet.
     *
     * @see SecurityManager#checkConnect
     * @since 1.1
     */
    // 返回本地IP；如果还未绑定，则返回通配地址
    public InetAddress getLocalAddress() {
        // This is for backward compatibility
        if(!isBound()) {
            return InetAddress.anyLocalAddress();
        }
        
        InetAddress in = null;
        
        try {
            // 获取[客户端Socket]/[服务端Socket(通信)]的"Socket委托"
            SocketImpl impl = getImpl();
            
            // 获取Socket绑定的本地IP
            in = (InetAddress) impl.getOption(SocketOptions.SO_BINDADDR);
            
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                sm.checkConnect(in.getHostAddress(), -1);
            }
            
            // 如果指定的地址为通配地址
            if(in.isAnyLocalAddress()) {
                // 获取通配地址
                in = InetAddress.anyLocalAddress();
            }
        } catch(SecurityException e) {
            in = InetAddress.getLoopbackAddress();
        } catch(Exception e) {
            in = InetAddress.anyLocalAddress(); // "0.0.0.0"
        }
        
        return in;
    }
    
    /**
     * Returns the local port number to which this socket is bound.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return the local port number
     * after the socket is closed.
     *
     * @return the local port number to which this socket is bound or -1
     * if the socket is not bound yet.
     */
    // 返回本地端口
    public int getLocalPort() {
        if(!isBound()) {
            return -1;
        }
        
        try {
            // 获取[客户端Socket]/[服务端Socket(通信)]的"Socket委托"
            SocketImpl impl = getImpl();
            
            // 返回本地端口
            return impl.getLocalPort();
        } catch(SocketException e) {
            // shouldn't happen as we're bound
        }
        
        return -1;
    }
    
    
    /**
     * Returns the address of the endpoint this socket is connected to, or
     * {@code null} if it is unconnected.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return the connected address
     * after the socket is closed.
     *
     * @return a {@code SocketAddress} representing the remote endpoint of this
     * socket, or {@code null} if it is not connected yet.
     *
     * @see #getInetAddress()
     * @see #getPort()
     * @see #connect(SocketAddress, int)
     * @see #connect(SocketAddress)
     * @since 1.4
     */
    // 返回远程Socket地址(ip+port)
    public SocketAddress getRemoteSocketAddress() {
        if(!isConnected()) {
            return null;
        }
        
        // 获取远程IP，如果还未连接就返回null
        InetAddress address = getInetAddress();
        // 获取远程端口
        int port = getPort();
        
        return new InetSocketAddress(address, port);
    }
    
    /**
     * Returns the address to which the socket is connected.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return the connected address
     * after the socket is closed.
     *
     * @return the remote IP address to which this socket is connected,
     * or {@code null} if the socket is not connected.
     */
    // 返回远程IP，如果还未连接就返回null
    public InetAddress getInetAddress() {
        if(!isConnected()) {
            return null;
        }
        
        try {
            // 获取[客户端Socket]/[服务端Socket(通信)]的"Socket委托"
            SocketImpl impl = getImpl();
            
            // 获取远程IP
            return impl.getInetAddress();
        } catch(SocketException e) {
        }
        
        return null;
    }
    
    /**
     * Returns the remote port number to which this socket is connected.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return the connected port number
     * after the socket is closed.
     *
     * @return the remote port number to which this socket is connected, or
     * 0 if the socket is not connected yet.
     */
    // 返回远程端口
    public int getPort() {
        if(!isConnected()) {
            return 0;
        }
        
        try {
            // 获取[客户端Socket]/[服务端Socket(通信)]的"Socket委托"
            SocketImpl impl = getImpl();
            
            // 返回远程端口
            return impl.getPort();
        } catch(SocketException e) {
            // Shouldn't happen as we're connected
        }
        
        return -1;
    }
    
    /*▲ 地址 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 标记[客户端Socket]已创建
    void setCreated() {
        created = true;
    }
    
    // 标记[客户端Socket]已绑定
    void setBound() {
        bound = true;
    }
    
    // 标记[客户端Socket]已连接
    void setConnected() {
        connected = true;
    }
    
    /**
     * set the flags after an accept() call.
     */
    // 标记[服务端Socket(通信)]已就绪
    final void postAccept() {
        created = true;
        connected = true;
        bound = true;
    }
    
    
    /**
     * Returns the connection state of the socket.
     * <p>
     * Note: Closing a socket doesn't clear its connection state, which means
     * this method will return {@code true} for a closed socket
     * (see {@link #isClosed()}) if it was successfuly connected prior
     * to being closed.
     *
     * @return true if the socket was successfuly connected to a server
     *
     * @since 1.4
     */
    // 判断当前Socket是否已完成连接（关闭Socket不会清除此状态）
    public boolean isConnected() {
        // Before 1.3 Sockets were always connected during creation
        return connected || oldImpl;
    }
    
    /**
     * Returns the binding state of the socket.
     * <p>
     * Note: Closing a socket doesn't clear its binding state, which means
     * this method will return {@code true} for a closed socket
     * (see {@link #isClosed()}) if it was successfuly bound prior
     * to being closed.
     *
     * @return true if the socket was successfuly bound to an address
     *
     * @see #bind
     * @since 1.4
     */
    // 判断当前Socket是否已完成绑定
    public boolean isBound() {
        // Before 1.3 Sockets were always bound during creation
        return bound || oldImpl;
    }
    
    /**
     * Returns the closed state of the socket.
     *
     * @return true if the socket has been closed
     *
     * @see #close
     * @since 1.4
     */
    // 判断客户端的Socket是否已关闭
    public boolean isClosed() {
        synchronized(closeLock) {
            return closed;
        }
    }
    
    /**
     * Returns whether the read-half of the socket connection is closed.
     *
     * @return true if the input of the socket has been shutdown
     *
     * @see #shutdownInput
     * @since 1.4
     */
    // 判断Socket是否关闭了读取功能
    public boolean isInputShutdown() {
        return shutIn;
    }
    
    /**
     * Returns whether the write-half of the socket connection is closed.
     *
     * @return true if the output of the socket has been shutdown
     *
     * @see #shutdownOutput
     * @since 1.4
     */
    // 判断Socket是否关闭了写入功能
    public boolean isOutputShutdown() {
        return shutOut;
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns setting for {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT}.
     * 0 returns implies that the option is disabled (i.e., timeout of infinity).
     *
     * @return the setting for {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT}
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #setSoTimeout(int)
     * @since 1.1
     */
    // 获取超时约束的时间
    public synchronized int getSoTimeout() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        Object o = impl.getOption(SocketOptions.SO_TIMEOUT);
        
        /* extra type safety */
        if(o instanceof Integer) {
            return ((Integer) o).intValue();
        } else {
            return 0;
        }
    }
    
    /**
     * Enable/disable {@link SocketOptions#SO_TIMEOUT SO_TIMEOUT}
     * with the specified timeout, in milliseconds. With this option set
     * to a non-zero timeout, a read() call on the InputStream associated with
     * this Socket will block for only this amount of time.  If the timeout
     * expires, a <B>java.net.SocketTimeoutException</B> is raised, though the
     * Socket is still valid. The option <B>must</B> be enabled
     * prior to entering the blocking operation to have effect. The
     * timeout must be {@code > 0}.
     * A timeout of zero is interpreted as an infinite timeout.
     *
     * @param timeout the specified timeout, in milliseconds.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #getSoTimeout()
     * @since 1.1
     */
    // 设置超时约束的时间
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
    
        if(timeout<0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
    
        SocketImpl impl = getImpl();
    
        impl.setOption(SocketOptions.SO_TIMEOUT, timeout);
    }
    
    /**
     * Get value of the {@link SocketOptions#SO_SNDBUF SO_SNDBUF} option
     * for this {@code Socket}, that is the buffer size used by the platform
     * for output on this {@code Socket}.
     *
     * @return the value of the {@link SocketOptions#SO_SNDBUF SO_SNDBUF}
     * option for this {@code Socket}.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #setSendBufferSize(int)
     * @since 1.2
     */
    // 获取输出流缓冲区大小
    public synchronized int getSendBufferSize() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        int result = 0;
        
        SocketImpl impl = getImpl();
        
        Object o = impl.getOption(SocketOptions.SO_SNDBUF);
        if(o instanceof Integer) {
            result = ((Integer) o).intValue();
        }
        
        return result;
    }
    
    /**
     * Sets the {@link SocketOptions#SO_SNDBUF SO_SNDBUF} option to the
     * specified value for this {@code Socket}.
     * The {@link SocketOptions#SO_SNDBUF SO_SNDBUF} option is used by the
     * platform's networking code as a hint for the size to set the underlying
     * network I/O buffers.
     *
     * <p>Because {@link SocketOptions#SO_SNDBUF SO_SNDBUF} is a hint,
     * applications that want to verify what size the buffers were set to
     * should call {@link #getSendBufferSize()}.
     *
     * @param size the size to which to set the send buffer
     *             size. This value must be greater than 0.
     *
     * @throws SocketException          if there is an error
     *                                  in the underlying protocol, such as a TCP error.
     * @throws IllegalArgumentException if the
     *                                  value is 0 or is negative.
     * @see #getSendBufferSize()
     * @since 1.2
     */
    // 设置输出流缓冲区大小
    public synchronized void setSendBufferSize(int size) throws SocketException {
        if(!(size>0)) {
            throw new IllegalArgumentException("negative send size");
        }
        
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.SO_SNDBUF, size);
    }
    
    /**
     * Gets the value of the {@link SocketOptions#SO_RCVBUF SO_RCVBUF} option
     * for this {@code Socket}, that is the buffer size used by the platform
     * for input on this {@code Socket}.
     *
     * @return the value of the {@link SocketOptions#SO_RCVBUF SO_RCVBUF}
     * option for this {@code Socket}.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #setReceiveBufferSize(int)
     * @since 1.2
     */
    // 获取输入流缓冲区大小
    public synchronized int getReceiveBufferSize() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        int result = 0;
        
        SocketImpl impl = getImpl();
        
        Object o = impl.getOption(SocketOptions.SO_RCVBUF);
        if(o instanceof Integer) {
            result = ((Integer) o).intValue();
        }
        
        return result;
    }
    
    /**
     * Sets the {@link SocketOptions#SO_RCVBUF SO_RCVBUF} option to the
     * specified value for this {@code Socket}. The
     * {@link SocketOptions#SO_RCVBUF SO_RCVBUF} option is
     * used by the platform's networking code as a hint for the size to set
     * the underlying network I/O buffers.
     *
     * <p>Increasing the receive buffer size can increase the performance of
     * network I/O for high-volume connection, while decreasing it can
     * help reduce the backlog of incoming data.
     *
     * <p>Because {@link SocketOptions#SO_RCVBUF SO_RCVBUF} is a hint,
     * applications that want to verify what size the buffers were set to
     * should call {@link #getReceiveBufferSize()}.
     *
     * <p>The value of {@link SocketOptions#SO_RCVBUF SO_RCVBUF} is also used
     * to set the TCP receive window that is advertized to the remote peer.
     * Generally, the window size can be modified at any time when a socket is
     * connected. However, if a receive window larger than 64K is required then
     * this must be requested <B>before</B> the socket is connected to the
     * remote peer. There are two cases to be aware of:
     * <ol>
     * <li>For sockets accepted from a ServerSocket, this must be done by calling
     * {@link ServerSocket#setReceiveBufferSize(int)} before the ServerSocket
     * is bound to a local address.</li>
     * <li>For client sockets, setReceiveBufferSize() must be called before
     * connecting the socket to its remote peer.</li></ol>
     *
     * @param size the size to which to set the receive buffer
     *             size. This value must be greater than 0.
     *
     * @throws IllegalArgumentException if the value is 0 or is
     *                                  negative.
     * @throws SocketException          if there is an error
     *                                  in the underlying protocol, such as a TCP error.
     * @see #getReceiveBufferSize()
     * @see ServerSocket#setReceiveBufferSize(int)
     * @since 1.2
     */
    // 设置输入流缓冲区大小
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        if(size<=0) {
            throw new IllegalArgumentException("invalid receive size");
        }
        
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.SO_RCVBUF, size);
    }
    
    /**
     * Tests if {@link SocketOptions#TCP_NODELAY TCP_NODELAY} is enabled.
     *
     * @return a {@code boolean} indicating whether or not
     * {@link SocketOptions#TCP_NODELAY TCP_NODELAY} is enabled.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #setTcpNoDelay(boolean)
     * @since 1.1
     */
    // 获取是否禁用Nagle算法
    public boolean getTcpNoDelay() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        return ((Boolean) impl.getOption(SocketOptions.TCP_NODELAY)).booleanValue();
    }
    
    /**
     * Enable/disable {@link SocketOptions#TCP_NODELAY TCP_NODELAY}
     * (disable/enable Nagle's algorithm).
     *
     * @param on {@code true} to enable TCP_NODELAY,
     *           {@code false} to disable.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #getTcpNoDelay()
     * @since 1.1
     */
    // 设置是否禁用Nagle算法
    public void setTcpNoDelay(boolean on) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.TCP_NODELAY, Boolean.valueOf(on));
    }
    
    /**
     * Returns setting for {@link SocketOptions#SO_LINGER SO_LINGER}.
     * -1 returns implies that the
     * option is disabled.
     *
     * The setting only affects socket close.
     *
     * @return the setting for {@link SocketOptions#SO_LINGER SO_LINGER}.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #setSoLinger(boolean, int)
     * @since 1.1
     */
    // 获取是否启用延时关闭
    public int getSoLinger() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        Object o = impl.getOption(SocketOptions.SO_LINGER);
        
        if(o instanceof Integer) {
            return ((Integer) o).intValue();
        } else {
            return -1;
        }
    }
    
    /**
     * Enable/disable {@link SocketOptions#SO_LINGER SO_LINGER} with the
     * specified linger time in seconds. The maximum timeout value is platform
     * specific.
     *
     * The setting only affects socket close.
     *
     * @param on     whether or not to linger on.
     * @param linger how long to linger for, if on is true.
     *
     * @throws SocketException          if there is an error
     *                                  in the underlying protocol, such as a TCP error.
     * @throws IllegalArgumentException if the linger value is negative.
     * @see #getSoLinger()
     * @since 1.1
     */
    // 设置是否启用延时关闭
    public void setSoLinger(boolean on, int linger) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        if(!on) {
            impl.setOption(SocketOptions.SO_LINGER, on);
        } else {
            if(linger<0) {
                throw new IllegalArgumentException("invalid value for SO_LINGER");
            }
            
            if(linger>65535) {
                linger = 65535;
            }
            
            impl.setOption(SocketOptions.SO_LINGER, linger);
        }
    }
    
    /**
     * Tests if {@link SocketOptions#SO_OOBINLINE SO_OOBINLINE} is enabled.
     *
     * @return a {@code boolean} indicating whether or not
     * {@link SocketOptions#SO_OOBINLINE SO_OOBINLINE}is enabled.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #setOOBInline(boolean)
     * @since 1.4
     */
    // 获取是否允许发送"紧急数据"
    public boolean getOOBInline() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        return (Boolean) impl.getOption(SocketOptions.SO_OOBINLINE);
    }
    
    /**
     * Enable/disable {@link SocketOptions#SO_OOBINLINE SO_OOBINLINE}
     * (receipt of TCP urgent data)
     *
     * By default, this option is disabled and TCP urgent data received on a
     * socket is silently discarded. If the user wishes to receive urgent data, then
     * this option must be enabled. When enabled, urgent data is received
     * inline with normal data.
     * <p>
     * Note, only limited support is provided for handling incoming urgent
     * data. In particular, no notification of incoming urgent data is provided
     * and there is no capability to distinguish between normal data and urgent
     * data unless provided by a higher level protocol.
     *
     * @param on {@code true} to enable
     *           {@link SocketOptions#SO_OOBINLINE SO_OOBINLINE},
     *           {@code false} to disable.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #getOOBInline()
     * @since 1.4
     */
    // 设置是否允许发送"紧急数据"
    public void setOOBInline(boolean on) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.SO_OOBINLINE, Boolean.valueOf(on));
    }
    
    /**
     * Tests if {@link SocketOptions#SO_KEEPALIVE SO_KEEPALIVE} is enabled.
     *
     * @return a {@code boolean} indicating whether or not
     * {@link SocketOptions#SO_KEEPALIVE SO_KEEPALIVE} is enabled.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #setKeepAlive(boolean)
     * @since 1.3
     */
    // 获取是否开启设置心跳机制
    public boolean getKeepAlive() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        return ((Boolean) impl.getOption(SocketOptions.SO_KEEPALIVE)).booleanValue();
    }
    
    /**
     * Enable/disable {@link SocketOptions#SO_KEEPALIVE SO_KEEPALIVE}.
     *
     * @param on whether or not to have socket keep alive turned on.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as a TCP error.
     * @see #getKeepAlive()
     * @since 1.3
     */
    // 设置是否开启设置心跳机制
    public void setKeepAlive(boolean on) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.SO_KEEPALIVE, Boolean.valueOf(on));
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
        
        SocketImpl impl = getImpl();
        
        return ((Boolean) (impl.getOption(SocketOptions.SO_REUSEADDR))).booleanValue();
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
     * Enabling {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR}
     * prior to binding the socket using {@link #bind(SocketAddress)} allows
     * the socket to be bound even though a previous connection is in a timeout
     * state.
     * <p>
     * When a {@code Socket} is created the initial setting
     * of {@link SocketOptions#SO_REUSEADDR SO_REUSEADDR} is disabled.
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
     * @see #isClosed()
     * @see #isBound()
     * @since 1.4
     */
    // 设置是否允许立刻重用已关闭的socket端口
    public void setReuseAddress(boolean on) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        SocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.SO_REUSEADDR, Boolean.valueOf(on));
    }
    
    /**
     * Gets traffic class or type-of-service in the IP header for packets sent from this Socket
     * <p>
     * As the underlying network implementation may ignore the
     * traffic class or type-of-service set using {@link #setTrafficClass(int)}
     * this method may return a different value than was previously
     * set using the {@link #setTrafficClass(int)} method on this Socket.
     *
     * @return the traffic class or type-of-service already set
     *
     * @throws SocketException if there is an error obtaining the
     *                         traffic class or type-of-service value.
     * @see #setTrafficClass(int)
     * @see SocketOptions#IP_TOS
     * @since 1.4
     */
    // 获取IP头部的Type-of-Service字段的值
    public int getTrafficClass() throws SocketException {
        SocketImpl impl = getImpl();
        
        return (Integer) (impl.getOption(SocketOptions.IP_TOS));
    }
    
    /**
     * Sets traffic class or type-of-service octet in the IP
     * header for packets sent from this Socket.
     * As the underlying network implementation may ignore this
     * value applications should consider it a hint.
     *
     * <P> The tc <B>must</B> be in the range {@code 0 <= tc <=
     * 255} or an IllegalArgumentException will be thrown.
     * <p>Notes:
     * <p>For Internet Protocol v4 the value consists of an
     * {@code integer}, the least significant 8 bits of which
     * represent the value of the TOS octet in IP packets sent by
     * the socket.
     * RFC 1349 defines the TOS values as follows:
     *
     * <UL>
     * <LI><CODE>IPTOS_LOWCOST (0x02)</CODE></LI>
     * <LI><CODE>IPTOS_RELIABILITY (0x04)</CODE></LI>
     * <LI><CODE>IPTOS_THROUGHPUT (0x08)</CODE></LI>
     * <LI><CODE>IPTOS_LOWDELAY (0x10)</CODE></LI>
     * </UL>
     * The last low order bit is always ignored as this
     * corresponds to the MBZ (must be zero) bit.
     * <p>
     * Setting bits in the precedence field may result in a
     * SocketException indicating that the operation is not
     * permitted.
     * <p>
     * As RFC 1122 section 4.2.4.2 indicates, a compliant TCP
     * implementation should, but is not required to, let application
     * change the TOS field during the lifetime of a connection.
     * So whether the type-of-service field can be changed after the
     * TCP connection has been established depends on the implementation
     * in the underlying platform. Applications should not assume that
     * they can change the TOS field after the connection.
     * <p>
     * For Internet Protocol v6 {@code tc} is the value that
     * would be placed into the sin6_flowinfo field of the IP header.
     *
     * @param tc an {@code int} value for the bitset.
     *
     * @throws SocketException if there is an error setting the
     *                         traffic class or type-of-service
     * @see #getTrafficClass
     * @see SocketOptions#IP_TOS
     * @since 1.4
     */
    // 设置IP参数，即设置IP头部的Type-of-Service字段，用于描述IP包的优先级和QoS选项
    public void setTrafficClass(int tc) throws SocketException {
        if(tc<0 || tc>255) {
            throw new IllegalArgumentException("tc is not in range 0 -- 255");
        }
    
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
    
        try {
            SocketImpl impl = getImpl();
        
            impl.setOption(SocketOptions.IP_TOS, tc);
        } catch(SocketException se) {
            // not supported if socket already connected
            // Solaris returns error in such cases
            if(!isConnected()) {
                throw se;
            }
        }
    }
    
    
    /**
     * Returns a set of the socket options supported by this socket.
     *
     * This method will continue to return the set of options even after
     * the socket has been closed.
     *
     * @return A set of the socket options supported by this socket. This set
     * may be empty if the socket's SocketImpl cannot be created.
     *
     * @since 9
     */
    // 获取Socket支持的所有参数
    public Set<SocketOption<?>> supportedOptions() {
        synchronized(Socket.class) {
            if(optionsSet) {
                return options;
            }
            
            try {
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
     * @throws UnsupportedOperationException if the socket does not support
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
    // 获取指定名称的Socket配置参数
    @SuppressWarnings("unchecked")
    public <T> T getOption(SocketOption<T> name) throws IOException {
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
     * @return this Socket
     *
     * @throws UnsupportedOperationException if the socket does not support
     *                                       the option.
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
    public <T> Socket setOption(SocketOption<T> name, T value) throws IOException {
        SocketImpl impl = getImpl();
        impl.setOption(name, value);
        return this;
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Send one byte of urgent data on the socket.
     * The byte to be sent is the lowest eight bits of the data parameter.
     * The urgent byte is sent after any preceding writes to the socket OutputStream and before any future writes to the OutputStream.
     *
     * @param data The byte of data to send
     *
     * @throws IOException if there is an error sending the data.
     * @since 1.4
     */
    // 发送一个字节的"紧急数据"，参见SocketOptions#SO_OOBINLINE参数
    public void sendUrgentData(int data) throws IOException {
        SocketImpl impl = getImpl();
        
        if(!impl.supportsUrgentData()) {
            throw new SocketException("Urgent data not supported");
        }
        
        impl.sendUrgentData(data);
    }
    
    /**
     * Sets performance preferences for this socket.
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
     * compared, with larger values indicating stronger preferences. Negative
     * values represent a lower priority than positive values. If the
     * application prefers short connection time over both low latency and high
     * bandwidth, for example, then it could invoke this method with the values
     * {@code (1, 0, 0)}.  If the application prefers high bandwidth above low
     * latency, and low latency above short connection time, then it could
     * invoke this method with the values {@code (0, 1, 2)}.
     *
     * <p> Invoking this method after this socket has been connected
     * will have no effect.
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
    // 设置Socket性能参数
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        /* Not implemented yet */
    }
    
    /**
     * Sets the client socket implementation factory for the
     * application. The factory can be specified only once.
     * <p>
     * When an application creates a new client socket, the socket
     * implementation factory's {@code createSocketImpl} method is
     * called to create the actual socket implementation.
     * <p>
     * Passing {@code null} to the method is a no-op unless the factory
     * was already set.
     * <p>If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param fac the desired factory.
     *
     * @throws IOException       if an I/O error occurs when setting the
     *                           socket factory.
     * @throws SocketException   if the factory is already defined.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkSetFactory} method doesn't allow the operation.
     * @see java.net.SocketImplFactory#createSocketImpl()
     * @see SecurityManager#checkSetFactory
     */
    // 设置一个"Socket委托"工厂，以便用来构造"Socket委托"实例
    public static synchronized void setSocketImplFactory(SocketImplFactory fac) throws IOException {
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
     * Returns the unique {@link java.nio.channels.SocketChannel SocketChannel}
     * object associated with this socket, if any.
     *
     * <p> A socket will have a channel if, and only if, the channel itself was
     * created via the {@link java.nio.channels.SocketChannel#open
     * SocketChannel.open} or {@link
     * java.nio.channels.ServerSocketChannel#accept ServerSocketChannel.accept}
     * methods.
     *
     * @return the socket channel associated with this socket,
     * or {@code null} if this socket was not created
     * for a channel
     *
     * @spec JSR-51
     * @since 1.4
     */
    /*
     * 返回当前Socket关联的通道源
     *
     * 目前只有当前类被实现为Socket适配器时，才返回已适配的Scoket通道；否则，返回null
     */
    public SocketChannel getChannel() {
        return null;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 检查是否使用了旧式"Socket委托"(JDK1.5开始，均不是旧式委托)
    private void checkOldImpl() {
        if(impl == null) {
            return;
        }
        
        /*
         * SocketImpl.connect() is a protected method,
         * therefore we need to use getDeclaredMethod,
         * therefore we need permission to access the member
         */
        
        oldImpl = AccessController.doPrivileged(new PrivilegedAction<>() {
            public Boolean run() {
                // 获取"Socket委托"类对象
                Class<?> clazz = impl.getClass();
                
                while(true) {
                    try {
                        // 如果包含void connect(SocketAddress, int)方法，则不属于旧式委托
                        clazz.getDeclaredMethod("connect", SocketAddress.class, int.class);
                        return Boolean.FALSE;
                    } catch(NoSuchMethodException e) {
                        clazz = clazz.getSuperclass();
                        // java.net.SocketImpl class will always have this abstract method.
                        // If we have not found it by now in the hierarchy then it does not exist, we are an old style impl.
                        if(clazz.equals(java.net.SocketImpl.class)) {
                            return Boolean.TRUE;
                        }
                    }
                }
            }
        });
    }
    
    // 检查地址是否合规
    private void checkAddress(InetAddress addr, String op) {
        if(addr == null) {
            return;
        }
        
        if(!(addr instanceof Inet4Address || addr instanceof Inet6Address)) {
            throw new IllegalArgumentException(op + ": invalid address type");
        }
    }
    
    /**
     * Sets impl to the system-default type of SocketImpl.
     *
     * @since 1.4
     */
    // (1)初始化"Socket委托"，并为其关联当前Socket
    void setImpl() {
        // 如果存在"Socket委托"工厂
        if(factory != null) {
            // 从"Socket委托"工厂生成"Socket委托"
            impl = factory.createSocketImpl();
            checkOldImpl();
        } else {
            // No need to do a checkOldImpl() here, we know it's an up to date SocketImpl!
            impl = new SocksSocketImpl();
        }
        
        if(impl != null) {
            // 为"Socket委托"关联Socket
            impl.setSocket(this);
        }
    }
    
    /**
     * Creates the socket implementation.
     *
     * @param stream a {@code boolean} value : {@code true} for a TCP socket, {@code false} for UDP.
     *
     * @throws IOException if creation fails
     * @since 1.4
     */
    /*
     * (2)创建[客户端Socket]/[服务端Socket(通信)]文件，并记下其文件描述符
     *
     * stream==true ：创建TCP Socket
     * stream==false：创建UDP Socket
     *
     * 注：当存在反向代理时，需要调用此方法创建[服务端Socket(通信)]在本地的实现
     * 　　否则，[服务端Socket(通信)]会在ServerSocket中被创建，此处仅创建[客户端Socket]
     */
    void createImpl(boolean stream) throws SocketException {
        // 当存在反向代理时，此处的impl非空
        if(impl == null) {
            // 初始化[客户端Socket]的委托，并为其关联客户端Socket
            setImpl();
        }
        
        try {
            // 创建[客户端Socket]/[服务端Socket(通信)]，并将其文件描述符记录到impl中
            impl.create(stream);
            created = true;
        } catch(IOException e) {
            throw new SocketException(e.getMessage());
        }
    }
    
    /**
     * Get the {@code SocketImpl} attached to this socket, creating
     * it if necessary.
     *
     * @return the {@code SocketImpl} attached to that ServerSocket.
     *
     * @throws SocketException if creation fails
     * @since 1.4
     */
    // (3)返回[客户端Socket]/[服务端Socket(通信)]的"Socket委托"
    SocketImpl getImpl() throws SocketException {
        // 如果[客户端Socket]/[服务端Socket(通信)]还未创建
        if(!created) {
            // 创建[客户端Socket]/[服务端Socket(通信)]文件，并记下其文件描述符；true指示创建的是TCP Socket
            createImpl(true);
        }
        
        return impl;
    }
    
    
    /**
     * Converts this socket to a {@code String}.
     *
     * @return a string representation of this socket.
     */
    @Override
    public String toString() {
        try {
            if(isConnected()) {
                return "Socket[addr=" + getImpl().getInetAddress() + ",port=" + getImpl().getPort() + ",localport=" + getImpl().getLocalPort() + "]";
            }
        } catch(SocketException e) {
        }
        
        return "Socket[unconnected]";
    }
    
}
