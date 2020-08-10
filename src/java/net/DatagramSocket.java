/*
 * Copyright (c) 1995, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.DatagramChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Set;

/**
 * This class represents a socket for sending and receiving datagram packets.
 *
 * <p>A datagram socket is the sending or receiving point for a packet
 * delivery service. Each packet sent or received on a datagram socket
 * is individually addressed and routed. Multiple packets sent from
 * one machine to another may be routed differently, and may arrive in
 * any order.
 *
 * <p> Where possible, a newly constructed {@code DatagramSocket} has the
 * {@link SocketOptions#SO_BROADCAST SO_BROADCAST} socket option enabled so as
 * to allow the transmission of broadcast datagrams. In order to receive
 * broadcast packets a DatagramSocket should be bound to the wildcard address.
 * In some implementations, broadcast packets may also be received when
 * a DatagramSocket is bound to a more specific address.
 * <p>
 * Example:
 * {@code
 * DatagramSocket s = new DatagramSocket(null);
 * s.bind(new InetSocketAddress(8888));
 * }
 * Which is equivalent to:
 * {@code
 * DatagramSocket s = new DatagramSocket(8888);
 * }
 * Both cases will create a DatagramSocket able to receive broadcasts on
 * UDP port 8888.
 *
 * @author Pavani Diwanji
 * @see java.net.DatagramPacket
 * @see java.nio.channels.DatagramChannel
 * @since 1.0
 */
/*
 * 无连接的Socket（使用UDP Socket）
 *
 *
 * Linux上的UDP通信方法：
 *
 * 服务端                客户端
 *
 * sokcet()             sokcet()
 *   ↓                     ↓
 * bind()               bind()
 *   ↓                     ↓
 * readfrom()              ↓
 *   ↓                     ↓
 * 阻塞，等待客户端连接     ↓
 *   ↓                     ↓
 *   ↓     客户端发出请求   ↓
 *   █    ←←←←←←←←←←←← sendto()
 *   ↓                     ↓
 * 服务端处理请求           ↓
 * 服务端做出响应           ↓
 *   ↓                     ↓
 *   ↓     客户端接收响应   ↓
 * sendto() →→→→→→→→→→→ readfrom()
 *   ↓                     ↓
 * close()              close()
 *
 *
 * UDP-Scoket可以实现单播、广播、组播：
 * 单播和广播均可以用DatagramSocket类完成，而组播需要使用其子类MulticastSocket来完成。
 *
 * 注：客户端与服务端是一个相对的概念，没有绝对的客户端或绝对的服务端
 */
public class DatagramSocket implements Closeable {
    
    /**
     * The implementation of this DatagramSocket.
     */
    // "UDP-Socket委托"，用来完成位于双端的UDP-Socket之间的通讯
    DatagramSocketImpl impl;
    
    /**
     * Are we using an older DatagramSocketImpl?
     */
    // 是否在使用旧式的"UDP-Socket委托"(JDK1.5起始，该值均为false)
    boolean oldImpl = false;
    
    /**
     * User defined factory for all datagram sockets.
     */
    // "UDP-Socket委托"的工厂
    static DatagramSocketImplFactory factory;
    
    /**
     * Connection state:
     *
     * ST_NOT_CONNECTED     = socket not connected
     * ST_CONNECTED         = socket connected
     * ST_CONNECTED_NO_IMPL = socket connected but not at impl level
     */
    // UDP-Socket的连接状态标记
    static final int ST_NOT_CONNECTED = 0;  // 未连接
    static final int ST_CONNECTED = 1;  // 已连接
    static final int ST_CONNECTED_NO_IMPL = 2;  // "模拟已连接"，通常是发起了连接操作，但是连接失败了，或者是本地禁止了连接
    
    // UDP-Socket的连接状态初始化为未连接
    int connectState = ST_NOT_CONNECTED;
    
    /**
     * Connected address & port
     */
    InetAddress connectedAddress = null;    // 远端的连接IP
    int connectedPort = -1;      // 远端的连接端口
    
    private static Set<SocketOption<?>> options;  // DatagramSocket配置参数
    private static boolean optionsSet = false;    // 懒加载，标记是否初始化了DatagramSocket配置参数
    
    /**
     * Various states of this socket.
     */
    private boolean created = false;    // UDP-Socket是否已创建
    private boolean bound = false;    // UDP-Socket是否已绑定
    private boolean closed = false;    // UDP-Socket是否已关闭
    
    private Object closeLock = new Object();
    
    /**
     * Set when a socket is ST_CONNECTED until we are certain
     * that any packets which might have been received prior
     * to calling connect() but not read by the application
     * have been read. During this time we check the source
     * address of all packets received to be sure they are from
     * the connected destination. Other packets are read but
     * silently dropped.
     */
    // 是否需要显式校验/过滤数据
    private boolean explicitFilter = false;
    
    // 记录需要被检验/过滤的字节数量，初始时与输入缓冲区的容量大小一致
    private int bytesLeftToFilter;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a datagram socket, bound to the specified local
     * socket address.
     * <p>
     * If, if the address is {@code null}, creates an unbound socket.
     *
     * <p>If there is a security manager,
     * its {@code checkListen} method is first called
     * with the port from the socket address
     * as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param bindaddr local socket address to bind, or {@code null}
     *                 for an unbound socket.
     *
     * @throws SocketException   if the socket could not be opened,
     *                           or the socket could not bind to the specified local port.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkListen} method doesn't allow the operation.
     * @see SecurityManager#checkListen
     * @since 1.4
     */
    // ▶ 1 构造UDP-Socket，并将其绑定到指定的Socket地址
    public DatagramSocket(SocketAddress bindaddr) throws SocketException {
        // 创建UDP-Socket和"UDP-Socket委托"，并完成它们之间的双向引用
        createImpl();
    
        if(bindaddr == null) {
            return;
        }
    
        try {
            bind(bindaddr);
        } finally {
            if(!isBound()) {
                close();
            }
        }
    }
    
    /**
     * Constructs a datagram socket and binds it to any available port
     * on the local host machine.  The socket will be bound to the
     * {@link InetAddress#isAnyLocalAddress wildcard} address,
     * an IP address chosen by the kernel.
     *
     * <p>If there is a security manager,
     * its {@code checkListen} method is first called
     * with 0 as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @throws SocketException   if the socket could not be opened,
     *                           or the socket could not bind to the specified local port.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkListen} method doesn't allow the operation.
     * @see SecurityManager#checkListen
     */
    // ▶ 1-1 构造UDP-Socket，并将其绑定到通配IP与随机端口
    public DatagramSocket() throws SocketException {
        this(new InetSocketAddress(0));
    }
    
    /**
     * Creates a datagram socket, bound to the specified local
     * address.  The local port must be between 0 and 65535 inclusive.
     * If the IP address is 0.0.0.0, the socket will be bound to the
     * {@link InetAddress#isAnyLocalAddress wildcard} address,
     * an IP address chosen by the kernel.
     *
     * <p>If there is a security manager,
     * its {@code checkListen} method is first called
     * with the {@code port} argument
     * as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param port  local port to use
     * @param laddr local address to bind
     *
     * @throws SocketException   if the socket could not be opened,
     *                           or the socket could not bind to the specified local port.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkListen} method doesn't allow the operation.
     * @see SecurityManager#checkListen
     * @since 1.1
     */
    // ▶ 1-2 构造UDP-Socket，并将其绑定到指定的IP和端口
    public DatagramSocket(int port, InetAddress laddr) throws SocketException {
        this(new InetSocketAddress(laddr, port));
    }
    
    /**
     * Constructs a datagram socket and binds it to the specified port
     * on the local host machine.  The socket will be bound to the
     * {@link InetAddress#isAnyLocalAddress wildcard} address,
     * an IP address chosen by the kernel.
     *
     * <p>If there is a security manager,
     * its {@code checkListen} method is first called
     * with the {@code port} argument
     * as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param port port to use.
     *
     * @throws SocketException   if the socket could not be opened,
     *                           or the socket could not bind to the specified local port.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkListen} method doesn't allow the operation.
     * @see SecurityManager#checkListen
     */
    // ▶ 1-2-1 构造UDP-Socket，并将其绑定到通配IP和指定的端口
    public DatagramSocket(int port) throws SocketException {
        this(port, null);
    }
    
    /**
     * Creates an unbound datagram socket with the specified
     * DatagramSocketImpl.
     *
     * @param impl an instance of a <B>DatagramSocketImpl</B>
     *             the subclass wishes to use on the DatagramSocket.
     *
     * @since 1.4
     */
    // ▶ 2 构造一个未绑定的UDP-Socket，并显式指定"UDP-Socket委托"
    protected DatagramSocket(DatagramSocketImpl impl) {
        if(impl == null) {
            throw new NullPointerException();
        }
        
        this.impl = impl;
        
        checkOldImpl();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Binds this DatagramSocket to a specific address and port.
     * <p>
     * If the address is {@code null}, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     *
     * @param addr The address and port to bind to.
     *
     * @throws SocketException          if any error happens during the bind, or if the
     *                                  socket is already bound.
     * @throws SecurityException        if a security manager exists and its
     *                                  {@code checkListen} method doesn't allow the operation.
     * @throws IllegalArgumentException if addr is a SocketAddress subclass
     *                                  not supported by this socket.
     * @since 1.4
     */
    /*
     * 对UDP-Socket执行【bind】操作。
     *
     * bindpoint: 待绑定的地址(ip+port)
     */
    public synchronized void bind(SocketAddress bindpoint) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 禁止重复绑定
        if(isBound()) {
            throw new SocketException("already bound");
        }
        
        if(bindpoint == null) {
            bindpoint = new InetSocketAddress(0);
        }
        
        if(!(bindpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type!");
        }
        
        InetSocketAddress epoint = (InetSocketAddress) bindpoint;
        if(epoint.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        
        // 从Socket地址中解析出IP
        InetAddress iaddr = epoint.getAddress();
        // 从Socket地址中解析出端口
        int port = epoint.getPort();
        
        checkAddress(iaddr, "bind");
        
        SecurityManager sec = System.getSecurityManager();
        if(sec != null) {
            sec.checkListen(port);
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        try {
            // 将当前UDP-Socket绑定到指定的IP和端口
            impl.bind(port, iaddr);
        } catch(SocketException e) {
            impl.close();
            throw e;
        }
        
        bound = true;
    }
    
    /**
     * Connects the socket to a remote address for this socket. When a
     * socket is connected to a remote address, packets may only be
     * sent to or received from that address. By default a datagram
     * socket is not connected.
     *
     * <p>If the remote destination to which the socket is connected does not
     * exist, or is otherwise unreachable, and if an ICMP destination unreachable
     * packet has been received for that address, then a subsequent call to
     * send or receive may throw a PortUnreachableException. Note, there is no
     * guarantee that the exception will be thrown.
     *
     * <p> If a security manager has been installed then it is invoked to check
     * access to the remote address. Specifically, if the given {@code address}
     * is a {@link InetAddress#isMulticastAddress multicast address},
     * the security manager's {@link
     * java.lang.SecurityManager#checkMulticast(InetAddress)
     * checkMulticast} method is invoked with the given {@code address}.
     * Otherwise, the security manager's {@link
     * java.lang.SecurityManager#checkConnect(String, int) checkConnect}
     * and {@link java.lang.SecurityManager#checkAccept checkAccept} methods
     * are invoked, with the given {@code address} and {@code port}, to
     * verify that datagrams are permitted to be sent and received
     * respectively.
     *
     * <p> When a socket is connected, {@link #receive receive} and
     * {@link #send send} <b>will not perform any security checks</b>
     * on incoming and outgoing packets, other than matching the packet's
     * and the socket's address and port. On a send operation, if the
     * packet's address is set and the packet's address and the socket's
     * address do not match, an {@code IllegalArgumentException} will be
     * thrown. A socket connected to a multicast address may only be used
     * to send packets.
     *
     * @param address the remote address for the socket
     * @param port    the remote port for the socket.
     *
     * @throws IllegalArgumentException if the address is null, or the port is out of range.
     * @throws SecurityException        if a security manager has been installed and it does
     *                                  not permit access to the given remote address
     * @see #disconnect
     */
    /*
     * 对UDP-Socket执行【connect】操作。
     *
     * 将UDP-Socket连接到远程地址，之后只能向该远程地址发送数据，或从该远程地址接收数据。
     *
     * 注：这与TCP-Socket中的绑定意义完全不同，不会经过握手验证
     */
    public void connect(InetAddress endpoint, int port) {
        try {
            // 通过"UDP-Socket委托"与远端建立连接
            connectInternal(endpoint, port);
        } catch(SocketException se) {
            throw new Error("connect failed", se);
        }
    }
    
    /**
     * Connects this socket to a remote socket address (IP address + port number).
     *
     * <p> If given an {@link InetSocketAddress InetSocketAddress}, this method
     * behaves as if invoking {@link #connect(InetAddress, int) connect(InetAddress,int)}
     * with the given socket addresses IP address and port number.
     *
     * @param addr The remote address.
     *
     * @throws SocketException          if the connect fails
     * @throws IllegalArgumentException if {@code addr} is {@code null}, or {@code addr} is a SocketAddress
     *                                  subclass not supported by this socket
     * @throws SecurityException        if a security manager has been installed and it does
     *                                  not permit access to the given remote address
     * @since 1.4
     */
    /*
     * 对UDP-Socket执行【connect】操作。
     *
     * 将UDP-Socket连接到远程地址，之后只能向该远程地址发送数据，或从该远程地址接收数据。
     *
     * 注：这与TCP-Socket中的绑定意义完全不同，不会经过握手验证
     */
    public void connect(SocketAddress endpoint) throws SocketException {
        if(endpoint == null) {
            throw new IllegalArgumentException("Address can't be null");
        }
        
        if(!(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        
        InetSocketAddress epoint = (InetSocketAddress) endpoint;
        if(epoint.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        
        // 通过"UDP-Socket委托"与远端建立连接
        connectInternal(epoint.getAddress(), epoint.getPort());
    }
    
    /**
     * Disconnects the socket. If the socket is closed or not connected,
     * then this method has no effect.
     *
     * @see #connect
     */
    /*
     * 对UDP-Socket执行【disconnect】操作，即断开连接。
     */
    public void disconnect() {
        synchronized(this) {
            if(isClosed()) {
                return;
            }
            
            if(connectState == ST_CONNECTED) {
                impl.disconnect();
            }
            
            connectedAddress = null;
            connectedPort = -1;
            connectState = ST_NOT_CONNECTED;
            explicitFilter = false;
        }
    }
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sends a datagram packet from this socket. The
     * {@code DatagramPacket} includes information indicating the
     * data to be sent, its length, the IP address of the remote host,
     * and the port number on the remote host.
     *
     * <p>If there is a security manager, and the socket is not currently
     * connected to a remote address, this method first performs some
     * security checks. First, if {@code p.getAddress().isMulticastAddress()}
     * is true, this method calls the
     * security manager's {@code checkMulticast} method
     * with {@code p.getAddress()} as its argument.
     * If the evaluation of that expression is false,
     * this method instead calls the security manager's
     * {@code checkConnect} method with arguments
     * {@code p.getAddress().getHostAddress()} and
     * {@code p.getPort()}. Each call to a security manager method
     * could result in a SecurityException if the operation is not allowed.
     *
     * @param p the {@code DatagramPacket} to be sent.
     *
     * @throws IOException                                    if an I/O error occurs.
     * @throws SecurityException                              if a security manager exists and its
     *                                                        {@code checkMulticast} or {@code checkConnect}
     *                                                        method doesn't allow the send.
     * @throws PortUnreachableException                       may be thrown if the socket is connected
     *                                                        to a currently unreachable destination. Note, there is no
     *                                                        guarantee that the exception will be thrown.
     * @throws java.nio.channels.IllegalBlockingModeException if this socket has an associated channel,
     *                                                        and the channel is in non-blocking mode.
     * @throws IllegalArgumentException                       if the socket is connected,
     *                                                        and connected address and packet address differ.
     * @revised 1.4
     * @spec JSR-51
     * @see java.net.DatagramPacket
     * @see SecurityManager#checkMulticast(InetAddress)
     * @see SecurityManager#checkConnect
     */
    // 向远端(目的地)发送packet中存储的UDP数据包
    public void send(DatagramPacket packet) throws IOException {
        InetAddress packetAddress = null;
        
        synchronized(packet) {
            if(isClosed()) {
                throw new SocketException("Socket is closed");
            }
            
            checkAddress(packet.getAddress(), "send");
            
            // 如果UDP-Socket未建立连接，则需要先通过安全管理器检查远端地址
            if(connectState == ST_NOT_CONNECTED) {
                // check the address is ok wiht the security manager on every send.
                SecurityManager security = System.getSecurityManager();
                
                /*
                 * The reason you want to synchronize on datagram packet
                 * is because you don't want an applet to change the address
                 * while you are trying to send the packet for example
                 * after the security check but before the send.
                 */
                if(security != null) {
                    if(packet.getAddress().isMulticastAddress()) {
                        security.checkMulticast(packet.getAddress());
                    } else {
                        security.checkConnect(packet.getAddress().getHostAddress(), packet.getPort());
                    }
                }
                
                // UDP-Socket已与远端建立连接时，需要确保数据包中包含有效的目的地地址
            } else {
                // 获取远端IP(数据包将要被发送到的IP地址)
                packetAddress = packet.getAddress();
                if(packetAddress == null) {
                    packet.setAddress(connectedAddress);
                    packet.setPort(connectedPort);
                } else if((!packetAddress.equals(connectedAddress)) || packet.getPort() != connectedPort) {
                    throw new IllegalArgumentException("connected address " + "and packet address" + " differ");
                }
            }
            
            // 如果UDP-Socket还未完成绑定，则必须先将其绑定到本地地址
            if(!isBound()) {
                bind(new InetSocketAddress(0));
            }
            
            // 获取"UDP-Socket委托"
            DatagramSocketImpl impl = getImpl();
            
            // 向目的地发送UDP数据包，待发送的数据存储在packet中
            impl.send(packet);
        }
    }
    
    /**
     * Receives a datagram packet from this socket. When this method
     * returns, the {@code DatagramPacket}'s buffer is filled with
     * the data received. The datagram packet also contains the sender's
     * IP address, and the port number on the sender's machine.
     * <p>
     * This method blocks until a datagram is received. The
     * {@code length} field of the datagram packet object contains
     * the length of the received message. If the message is longer than
     * the packet's length, the message is truncated.
     * <p>
     * If there is a security manager, a packet cannot be received if the
     * security manager's {@code checkAccept} method
     * does not allow it.
     *
     * @param p the {@code DatagramPacket} into which to place
     *          the incoming data.
     *
     * @throws IOException                                    if an I/O error occurs.
     * @throws SocketTimeoutException                         if setSoTimeout was previously called
     *                                                        and the timeout has expired.
     * @throws PortUnreachableException                       may be thrown if the socket is connected
     *                                                        to a currently unreachable destination. Note, there is no guarantee that the
     *                                                        exception will be thrown.
     * @throws java.nio.channels.IllegalBlockingModeException if this socket has an associated channel,
     *                                                        and the channel is in non-blocking mode.
     * @revised 1.4
     * @spec JSR-51
     * @see java.net.DatagramPacket
     * @see java.net.DatagramSocket
     */
    // 从远端UDP-Socket接收UDP数据包并存入packet
    public synchronized void receive(DatagramPacket packet) throws IOException {
        synchronized(packet) {
            if(!isBound()) {
                bind(new InetSocketAddress(0));
            }
            
            // 获取"UDP-Socket委托"
            DatagramSocketImpl impl = getImpl();
            
            /*
             * 如果已建立连接，则需要从固定的远端地址接收数据包，
             * 那么在此之前，先丢掉那些地址信息与连接到的远端地址不匹配的那些数据包
             */
            if(connectState == ST_NOT_CONNECTED) {
                // 每次接收前，都需要通过安全管理员确认地址是否正确
                SecurityManager security = System.getSecurityManager();
                if(security != null) {
                    while(true) {
                        String peekAd = null;
                        int peekPort = 0;
                        
                        // 查看远端积压的数据包，获取其IP和端口
                        if(!oldImpl) {
                            // We can use the new peekData() API
                            DatagramPacket peekPacket = new DatagramPacket(new byte[1], 1);
                            peekPort = impl.peekData(peekPacket);
                            peekAd = peekPacket.getAddress().getHostAddress();
                        } else {
                            InetAddress adr = new InetAddress();
                            peekPort = impl.peek(adr);
                            peekAd = adr.getHostAddress();
                        }
                        
                        try {
                            // 校验远端数据包的IP和端口
                            security.checkAccept(peekAd, peekPort);
                            
                            // 校验成功的情形下，结束循环，后续可以正常接收数据包了
                            break;
                        } catch(SecurityException se) {
                            // 校验失败的情形下，先消耗这个地址不匹配的数据包
                            DatagramPacket tmp = new DatagramPacket(new byte[1], 1);
                            impl.receive(tmp);
                            
                            /*
                             * silently discard the offending packet and continue:
                             * unknown/malicious entities on nets should not make runtime throw security exception
                             * and disrupt the applet by sending random datagram packets.
                             */
                        }
                    } // while
                } // if
            }
            
            DatagramPacket tmp = null;
            
            /*
             * 如果需要模拟已连接状态，或者是需要显式过滤一批数据，
             * 那么接下来仍然要对远端积压的数据包进行校验/过滤。
             */
            if((connectState == ST_CONNECTED_NO_IMPL) || explicitFilter) {
                /*
                 * We have to do the filtering the old fashioned way since the native impl doesn't support connect
                 * or the connect via the impl failed,
                 * or .. "explicitFilter" may be set when a socket is connected via the impl,
                 * for a period of time when packets from other sources might be queued on socket.
                 */
                while(true) {
                    InetAddress peekAddress = null;
                    int peekPort = -1;
                    
                    // 查看远端积压的数据包，获取其IP和端口
                    if(!oldImpl) {
                        // We can use the new peekData() API
                        DatagramPacket peekPacket = new DatagramPacket(new byte[1], 1);
                        peekPort = impl.peekData(peekPacket);
                        peekAddress = peekPacket.getAddress();
                    } else {
                        // this api only works for IPv4
                        peekAddress = new InetAddress();
                        peekPort = impl.peek(peekAddress);
                    }
                    
                    // 如果数据包的地址信息与连接到的远程地址信息是匹配的，说明这些数据包可信，直接放行
                    if((connectedAddress.equals(peekAddress)) && (connectedPort == peekPort)) {
                        break;
                    }
                    
                    // 消耗这批不可信的数据包，相当于丢弃
                    tmp = new DatagramPacket(new byte[1024], 1024);
                    impl.receive(tmp);
                    
                    // 在已经过滤掉packet数据包中的数据后，判断后续是否仍然需要继续过滤
                    if(explicitFilter && checkFiltering(tmp)) {
                        // 如果过滤完成，则不需要再过滤，直接退出循环即可
                        break;
                    }
                } // while
            } // if
            
            /* If the security check succeeds, or the datagram is connected then receive the packet */
            // 接收UDP数据包，接收到的数据存储到packet中
            impl.receive(packet);
            
            // 如果需要显式校验/过滤，且前面没有丢弃过数据，则需要在此尝试过滤
            if(explicitFilter && tmp == null) {
                // 注：这里的packet没有被过滤，但是会将其当成已过滤(丢弃)一样进行计数
                checkFiltering(packet);
            }
        }
    }
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes this datagram socket.
     * <p>
     * Any thread currently blocked in {@link #receive} upon this socket
     * will throw a {@link SocketException}.
     *
     * <p> If this socket has an associated channel then the channel is closed
     * as well.
     *
     * @revised 1.4
     * @spec JSR-51
     */
    // 关闭UDP-Socket
    public void close() {
        synchronized(closeLock) {
            if(isClosed()) {
                return;
            }
            
            impl.close();
            
            closed = true;
        }
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the address of the endpoint this socket is bound to.
     *
     * @return a {@code SocketAddress} representing the local endpoint of this
     * socket, or {@code null} if it is closed or not bound yet.
     *
     * @see #getLocalAddress()
     * @see #getLocalPort()
     * @see #bind(SocketAddress)
     * @since 1.4
     */
    // 返回当前UDP-Socket绑定的本地Socket地址
    public SocketAddress getLocalSocketAddress() {
        if(isClosed()) {
            return null;
        }
        
        if(!isBound()) {
            return null;
        }
        
        return new InetSocketAddress(getLocalAddress(), getLocalPort());
    }
    
    /**
     * Gets the local address to which the socket is bound.
     *
     * <p>If there is a security manager, its
     * {@code checkConnect} method is first called
     * with the host address and {@code -1}
     * as its arguments to see if the operation is allowed.
     *
     * @return the local address to which the socket is bound,
     * {@code null} if the socket is closed, or
     * an {@code InetAddress} representing
     * {@link InetAddress#isAnyLocalAddress wildcard}
     * address if either the socket is not bound, or
     * the security manager {@code checkConnect}
     * method does not allow the operation
     *
     * @see SecurityManager#checkConnect
     * @since 1.1
     */
    // 返回当前UDP-Socket绑定的本地IP
    public InetAddress getLocalAddress() {
        if(isClosed()) {
            return null;
        }
    
        InetAddress in = null;
    
        try {
            // 获取"UDP-Socket委托"
            DatagramSocketImpl impl = getImpl();
        
            // 获取UDP-Socket绑定的本地IP
            in = (InetAddress) impl.getOption(SocketOptions.SO_BINDADDR);
            if(in.isAnyLocalAddress()) {
                in = InetAddress.anyLocalAddress();
            }
        
            SecurityManager s = System.getSecurityManager();
            if(s != null) {
                s.checkConnect(in.getHostAddress(), -1);
            }
        } catch(Exception e) {
            in = InetAddress.anyLocalAddress(); // "0.0.0.0"
        }
    
        return in;
    }
    
    /**
     * Returns the port number on the local host to which this socket
     * is bound.
     *
     * @return the port number on the local host to which this socket is bound,
     * {@code -1} if the socket is closed, or
     * {@code 0} if it is not bound yet.
     */
    // 返回当前UDP-Socket绑定的本地端口
    public int getLocalPort() {
        if(isClosed()) {
            return -1;
        }
    
        try {
            // 获取"UDP-Socket委托"
            DatagramSocketImpl impl = getImpl();
        
            // 返回当前UDP-Socket绑定的本地端口
            return impl.getLocalPort();
        } catch(Exception e) {
            return 0;
        }
    }
    
    
    /**
     * Returns the address of the endpoint this socket is connected to, or
     * {@code null} if it is unconnected.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return the connected address
     * after the socket is closed.
     *
     * @return a {@code SocketAddress} representing the remote
     * endpoint of this socket, or {@code null} if it is
     * not connected yet.
     *
     * @see #getInetAddress()
     * @see #getPort()
     * @see #connect(SocketAddress)
     * @since 1.4
     */
    // 返回当前UDP-Socket连接的远端Socket地址
    public SocketAddress getRemoteSocketAddress() {
        if(!isConnected()) {
            return null;
        }
        
        return new InetSocketAddress(getInetAddress(), getPort());
    }
    
    /**
     * Returns the address to which this socket is connected. Returns
     * {@code null} if the socket is not connected.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return the connected address
     * after the socket is closed.
     *
     * @return the address to which this socket is connected.
     */
    // 返回当前UDP-Socket连接的远端IP
    public InetAddress getInetAddress() {
        return connectedAddress;
    }
    
    /**
     * Returns the port number to which this socket is connected.
     * Returns {@code -1} if the socket is not connected.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return the connected port number
     * after the socket is closed.
     *
     * @return the port number to which this socket is connected.
     */
    // 返回当前UDP-Socket连接的远端端口
    public int getPort() {
        return connectedPort;
    }
    
    /*▲ 地址 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the binding state of the socket.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return {@code true}
     * after the socket is closed.
     *
     * @return true if the socket successfully bound to an address
     *
     * @since 1.4
     */
    // 判断UDP-Socket是否已绑定
    public boolean isBound() {
        return bound;
    }
    
    /**
     * Returns the connection state of the socket.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return {@code true}
     * after the socket is closed.
     *
     * @return true if the socket successfully connected to a server
     *
     * @since 1.4
     */
    // 判断UDP-Socket是否已连接
    public boolean isConnected() {
        return connectState != ST_NOT_CONNECTED;
    }
    
    /**
     * Returns whether the socket is closed or not.
     *
     * @return true if the socket has been closed
     *
     * @since 1.4
     */
    // 判断UDP-Socket是否已关闭
    public boolean isClosed() {
        synchronized(closeLock) {
            return closed;
        }
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieve setting for SO_TIMEOUT.  0 returns implies that the
     * option is disabled (i.e., timeout of infinity).
     *
     * @return the setting for SO_TIMEOUT
     *
     * @throws SocketException if there is an error in the underlying protocol, such as an UDP error.
     * @see #setSoTimeout(int)
     * @since 1.1
     */
    // 获取超时约束的时间
    public synchronized int getSoTimeout() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        if(impl == null) {
            return 0;
        }
        
        Object o = impl.getOption(SocketOptions.SO_TIMEOUT);
        
        /* extra type safety */
        if(o instanceof Integer) {
            return (Integer) o;
        } else {
            return 0;
        }
    }
    
    /**
     * Enable/disable SO_TIMEOUT with the specified timeout, in
     * milliseconds. With this option set to a non-zero timeout,
     * a call to receive() for this DatagramSocket
     * will block for only this amount of time.  If the timeout expires,
     * a <B>java.net.SocketTimeoutException</B> is raised, though the
     * DatagramSocket is still valid.  The option <B>must</B> be enabled
     * prior to entering the blocking operation to have effect.  The
     * timeout must be {@code > 0}.
     * A timeout of zero is interpreted as an infinite timeout.
     *
     * @param timeout the specified timeout in milliseconds.
     *
     * @throws SocketException if there is an error in the underlying protocol, such as an UDP error.
     * @see #getSoTimeout()
     * @since 1.1
     */
    // 设置超时约束的时间
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.SO_TIMEOUT, timeout);
    }
    
    /**
     * Get value of the SO_SNDBUF option for this {@code DatagramSocket}, that is the
     * buffer size used by the platform for output on this {@code DatagramSocket}.
     *
     * @return the value of the SO_SNDBUF option for this {@code DatagramSocket}
     *
     * @throws SocketException if there is an error in
     *                         the underlying protocol, such as an UDP error.
     * @see #setSendBufferSize
     */
    // 获取输出流缓冲区大小
    public synchronized int getSendBufferSize() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        int result = 0;
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        Object o = impl.getOption(SocketOptions.SO_SNDBUF);
        
        if(o instanceof Integer) {
            result = ((Integer) o).intValue();
        }
        
        return result;
    }
    
    /**
     * Sets the SO_SNDBUF option to the specified value for this
     * {@code DatagramSocket}. The SO_SNDBUF option is used by the
     * network implementation as a hint to size the underlying
     * network I/O buffers. The SO_SNDBUF setting may also be used
     * by the network implementation to determine the maximum size
     * of the packet that can be sent on this socket.
     * <p>
     * As SO_SNDBUF is a hint, applications that want to verify
     * what size the buffer is should call {@link #getSendBufferSize()}.
     * <p>
     * Increasing the buffer size may allow multiple outgoing packets
     * to be queued by the network implementation when the send rate
     * is high.
     * <p>
     * Note: If {@link #send(DatagramPacket)} is used to send a
     * {@code DatagramPacket} that is larger than the setting
     * of SO_SNDBUF then it is implementation specific if the
     * packet is sent or discarded.
     *
     * @param size the size to which to set the send buffer
     *             size. This value must be greater than 0.
     *
     * @throws SocketException          if there is an error
     *                                  in the underlying protocol, such as an UDP error.
     * @throws IllegalArgumentException if the value is 0 or is
     *                                  negative.
     * @see #getSendBufferSize()
     */
    // 设置输出流缓冲区大小
    public synchronized void setSendBufferSize(int size) throws SocketException {
        if(!(size>0)) {
            throw new IllegalArgumentException("negative send size");
        }
        
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.SO_SNDBUF, size);
    }
    
    /**
     * Get value of the SO_RCVBUF option for this {@code DatagramSocket}, that is the
     * buffer size used by the platform for input on this {@code DatagramSocket}.
     *
     * @return the value of the SO_RCVBUF option for this {@code DatagramSocket}
     *
     * @throws SocketException if there is an error in the underlying protocol, such as an UDP error.
     * @see #setReceiveBufferSize(int)
     */
    // 获取输入流缓冲区大小
    public synchronized int getReceiveBufferSize() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        int result = 0;
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        // 获取输入流缓冲区大小
        Object o = impl.getOption(SocketOptions.SO_RCVBUF);
        if(o instanceof Integer) {
            result = (Integer) o;
        }
        
        return result;
    }
    
    /**
     * Sets the SO_RCVBUF option to the specified value for this
     * {@code DatagramSocket}. The SO_RCVBUF option is used by
     * the network implementation as a hint to size the underlying
     * network I/O buffers. The SO_RCVBUF setting may also be used
     * by the network implementation to determine the maximum size
     * of the packet that can be received on this socket.
     * <p>
     * Because SO_RCVBUF is a hint, applications that want to
     * verify what size the buffers were set to should call
     * {@link #getReceiveBufferSize()}.
     * <p>
     * Increasing SO_RCVBUF may allow the network implementation
     * to buffer multiple packets when packets arrive faster than
     * are being received using {@link #receive(DatagramPacket)}.
     * <p>
     * Note: It is implementation specific if a packet larger
     * than SO_RCVBUF can be received.
     *
     * @param size the size to which to set the receive buffer
     *             size. This value must be greater than 0.
     *
     * @throws SocketException          if there is an error in
     *                                  the underlying protocol, such as an UDP error.
     * @throws IllegalArgumentException if the value is 0 or is
     *                                  negative.
     * @see #getReceiveBufferSize()
     */
    // 设置输入流缓冲区大小
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        if(size<=0) {
            throw new IllegalArgumentException("invalid receive size");
        }
        
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.SO_RCVBUF, size);
    }
    
    /**
     * Tests if SO_REUSEADDR is enabled.
     *
     * @return a {@code boolean} indicating whether or not SO_REUSEADDR is enabled.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as an UDP error.
     * @see #setReuseAddress(boolean)
     * @since 1.4
     */
    // 获取是否允许立刻重用已关闭的socket端口
    public synchronized boolean getReuseAddress() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        Object o = impl.getOption(SocketOptions.SO_REUSEADDR);
        
        return (Boolean) o;
    }
    
    /**
     * Enable/disable the SO_REUSEADDR socket option.
     * <p>
     * For UDP sockets it may be necessary to bind more than one
     * socket to the same socket address. This is typically for the
     * purpose of receiving multicast packets
     * (See {@link java.net.MulticastSocket}). The
     * {@code SO_REUSEADDR} socket option allows multiple
     * sockets to be bound to the same socket address if the
     * {@code SO_REUSEADDR} socket option is enabled prior
     * to binding the socket using {@link #bind(SocketAddress)}.
     * <p>
     * Note: This functionality is not supported by all existing platforms,
     * so it is implementation specific whether this option will be ignored
     * or not. However, if it is not supported then
     * {@link #getReuseAddress()} will always return {@code false}.
     * <p>
     * When a {@code DatagramSocket} is created the initial setting
     * of {@code SO_REUSEADDR} is disabled.
     * <p>
     * The behaviour when {@code SO_REUSEADDR} is enabled or
     * disabled after a socket is bound (See {@link #isBound()})
     * is not defined.
     *
     * @param on whether to enable or disable the
     *
     * @throws SocketException if an error occurs enabling or
     *                         disabling the {@code SO_RESUEADDR} socket option,
     *                         or the socket is closed.
     * @see #getReuseAddress()
     * @see #bind(SocketAddress)
     * @see #isBound()
     * @see #isClosed()
     * @since 1.4
     */
    // 设置是否允许立刻重用已关闭的socket端口
    public synchronized void setReuseAddress(boolean on) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
    
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
    
        // Integer instead of Boolean for compatibility with older DatagramSocketImpl
        if(oldImpl) {
            impl.setOption(SocketOptions.SO_REUSEADDR, on ? -1 : 0);
        } else {
            impl.setOption(SocketOptions.SO_REUSEADDR, on);
        }
    }
    
    /**
     * Tests if SO_BROADCAST is enabled.
     *
     * @return a {@code boolean} indicating whether or not SO_BROADCAST is enabled.
     *
     * @throws SocketException if there is an error
     *                         in the underlying protocol, such as an UDP error.
     * @see #setBroadcast(boolean)
     * @since 1.4
     */
    // 判断是否允许发送广播
    public synchronized boolean getBroadcast() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        return (Boolean) (impl.getOption(SocketOptions.SO_BROADCAST));
    }
    
    /**
     * Enable/disable SO_BROADCAST.
     *
     * <p> Some operating systems may require that the Java virtual machine be
     * started with implementation specific privileges to enable this option or
     * send broadcast datagrams.
     *
     * @param on whether or not to have broadcast turned on.
     *
     * @throws SocketException if there is an error in the underlying protocol, such as an UDP
     *                         error.
     * @see #getBroadcast()
     * @since 1.4
     */
    // 设置是否允许发送广播
    public synchronized void setBroadcast(boolean on) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
    
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
    
        impl.setOption(SocketOptions.SO_BROADCAST, on);
    }
    
    /**
     * Gets traffic class or type-of-service in the IP datagram
     * header for packets sent from this DatagramSocket.
     * <p>
     * As the underlying network implementation may ignore the
     * traffic class or type-of-service set using {@link #setTrafficClass(int)}
     * this method may return a different value than was previously
     * set using the {@link #setTrafficClass(int)} method on this
     * DatagramSocket.
     *
     * @return the traffic class or type-of-service already set
     *
     * @throws SocketException if there is an error obtaining the
     *                         traffic class or type-of-service value.
     * @see #setTrafficClass(int)
     * @since 1.4
     */
    // 获取IP头部的Type-of-Service字段的值
    public synchronized int getTrafficClass() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        return (Integer) (impl.getOption(SocketOptions.IP_TOS));
    }
    
    /**
     * Sets traffic class or type-of-service octet in the IP
     * datagram header for datagrams sent from this DatagramSocket.
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
     * for Internet Protocol v6 {@code tc} is the value that
     * would be placed into the sin6_flowinfo field of the IP header.
     *
     * @param tc an {@code int} value for the bitset.
     *
     * @throws SocketException if there is an error setting the
     *                         traffic class or type-of-service
     * @see #getTrafficClass
     * @since 1.4
     */
    // 设置IP参数，即设置IP头部的Type-of-Service字段，用于描述IP包的优先级和QoS选项
    public synchronized void setTrafficClass(int tc) throws SocketException {
        if(tc<0 || tc>255) {
            throw new IllegalArgumentException("tc is not in range 0 -- 255");
        }
    
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
    
        try {
            // 获取"UDP-Socket委托"
            DatagramSocketImpl impl = getImpl();
        
            impl.setOption(SocketOptions.IP_TOS, tc);
        } catch(SocketException se) {
            // not supported if socket already connected Solaris returns error in such cases
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
     * may be empty if the socket's DatagramSocketImpl cannot be created.
     *
     * @since 9
     */
    // 获取UDP-Socket支持的所有参数
    public Set<SocketOption<?>> supportedOptions() {
        synchronized(DatagramSocket.class) {
            if(optionsSet) {
                return options;
            }
            
            try {
                // 获取"UDP-Socket委托"
                DatagramSocketImpl impl = getImpl();
                
                options = Collections.unmodifiableSet(impl.supportedOptions());
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
     * @throws UnsupportedOperationException if the datagram socket
     *                                       does not support the option.
     * @throws IOException                   if an I/O error occurs, or if the socket is closed.
     * @throws NullPointerException          if name is {@code null}
     * @throws SecurityException             if a security manager is set and if the socket
     *                                       option requires a security permission and if the caller does
     *                                       not have the required permission.
     *                                       {@link java.net.StandardSocketOptions StandardSocketOptions}
     *                                       do not require any security permission.
     * @since 9
     */
    // 获取指定名称的UDP-Socket配置参数
    public <T> T getOption(SocketOption<T> name) throws IOException {
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
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
     * @return this DatagramSocket
     *
     * @throws UnsupportedOperationException if the datagram socket
     *                                       does not support the option.
     * @throws IllegalArgumentException      if the value is not valid for
     *                                       the option.
     * @throws IOException                   if an I/O error occurs, or if the socket is closed.
     * @throws SecurityException             if a security manager is set and if the socket
     *                                       option requires a security permission and if the caller does
     *                                       not have the required permission.
     *                                       {@link java.net.StandardSocketOptions StandardSocketOptions}
     *                                       do not require any security permission.
     * @throws NullPointerException          if name is {@code null}
     * @since 9
     */
    // 设置指定名称的UDP-Socket配置参数
    public <T> DatagramSocket setOption(SocketOption<T> name, T value) throws IOException {
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.setOption(name, value);
        return this;
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets the datagram socket implementation factory for the
     * application. The factory can be specified only once.
     * <p>
     * When an application creates a new datagram socket, the socket
     * implementation factory's {@code createDatagramSocketImpl} method is
     * called to create the actual datagram socket implementation.
     * <p>
     * Passing {@code null} to the method is a no-op unless the factory
     * was already set.
     *
     * <p>If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param fac the desired factory.
     *
     * @throws IOException       if an I/O error occurs when setting the
     *                           datagram socket factory.
     * @throws SocketException   if the factory is already defined.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkSetFactory} method doesn't allow the operation.
     * @see java.net.DatagramSocketImplFactory#createDatagramSocketImpl()
     * @see SecurityManager#checkSetFactory
     * @since 1.3
     */
    // 设置一个"UDP-Socket委托"工厂，以便用来构造"UDP-Socket委托"实例
    public static synchronized void setDatagramSocketImplFactory(DatagramSocketImplFactory fac) throws IOException {
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
     * Returns the unique {@link java.nio.channels.DatagramChannel} object
     * associated with this datagram socket, if any.
     *
     * <p> A datagram socket will have a channel if, and only if, the channel
     * itself was created via the {@link java.nio.channels.DatagramChannel#open
     * DatagramChannel.open} method.
     *
     * @return the datagram channel associated with this datagram socket,
     * or {@code null} if this socket was not created for a channel
     *
     * @spec JSR-51
     * @since 1.4
     */
    /*
     * 返回当前UDP-Socket关联的通道源
     *
     * 目前只有当前类被实现为UDP-Socket适配器时，才返回已适配的UDP-Scoket通道；否则，返回null
     */
    public DatagramChannel getChannel() {
        return null;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*
     * 判断"UDP-Socket委托"是否为旧式委托。
     *
     * 如果该委托中实现了peekData()方法，则说明这不是旧式委托，即oldImpl=false。
     * JDK 5开始都在使用新的委托了。
     */
    private void checkOldImpl() {
        if(impl == null) {
            return;
        }
        
        /*
         * DatagramSocketImpl.peekdata() is a protected method, therefore we need to use getDeclaredMethod,
         * therefore we need permission to access the member
         */
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                public Void run() throws NoSuchMethodException {
                    Class<?>[] cl = new Class<?>[1];
                    cl[0] = DatagramPacket.class;
                    // 获取DatagramPacket实现类中的peekData()方法
                    impl.getClass().getDeclaredMethod("peekData", cl);
                    return null;
                }
            });
        } catch(java.security.PrivilegedActionException e) {
            oldImpl = true;
        }
    }
    
    // 检查IP类型
    void checkAddress(InetAddress addr, String op) {
        if(addr == null) {
            return;
        }
        
        if(!(addr instanceof Inet4Address || addr instanceof Inet6Address)) {
            throw new IllegalArgumentException(op + ": invalid address type");
        }
    }
    
    // 在已经过滤掉packet数据包中的数据后，后续是否仍然需要继续过滤
    private boolean checkFiltering(DatagramPacket packet) throws SocketException {
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        /*
         * 获取接收到的数据包的长度
         *
         * 注：该方法由客户端间接调用
         */
        int len = packet.getLength();
        
        // 在已经过滤掉一波数据后，查看剩余的数据量
        bytesLeftToFilter -= len;
        
        // 如果仍有剩余数据，则返回false，意思是后续还得继续过滤
        if(bytesLeftToFilter>0 && impl.dataAvailable()>0) {
            return false;
        }
        
        explicitFilter = false;
        
        return true;
    }
    
    // 创建UDP-Socket和"UDP-Socket委托"，并完成它们之间的双向引用
    void createImpl() throws SocketException {
        // 如果还未创建"UDP-Socket委托"
        if(impl == null) {
            // 如果存在用户指定的"UDP-Socket委托"工厂，则通过该工厂来创建"UDP-Socket委托"
            if(factory != null) {
                impl = factory.createDatagramSocketImpl();
                checkOldImpl();
                
                // 如果不存在用户指定的"UDP-Socket委托"工厂
            } else {
                // 首先判断是否为组播Socket
                boolean isMulticast = this instanceof MulticastSocket;
                
                // 使用内置的"UDP-Socket委托"工厂来创建"UDP-Socket委托"
                impl = DefaultDatagramSocketImplFactory.createDatagramSocketImpl(isMulticast);
                
                checkOldImpl();
            }
        }
        
        // 创建Socket文件，并记下其文件描述符到fd字段中（该文件描述符会被清理器追踪）
        impl.create();
        
        // 将当前UDP-Socket关联到指定的"UDP-Socket委托"中
        impl.setDatagramSocket(this);
        
        created = true;
    }
    
    /**
     * Get the {@code DatagramSocketImpl} attached to this socket,
     * creating it if necessary.
     *
     * @return the {@code DatagramSocketImpl} attached to that
     * DatagramSocket
     *
     * @throws SocketException if creation fails.
     * @since 1.4
     */
    // 返回"UDP-Socket委托"
    DatagramSocketImpl getImpl() throws SocketException {
        // 如果"UDP-Socket委托"还未创建，则需要先创建它
        if(!created) {
            createImpl();
        }
        
        return impl;
    }
    
    /**
     * Connects this socket to a remote socket address (IP address + port number).
     * Binds socket if not already bound.
     *
     * @param endpoint The remote address.
     * @param port     The remote port
     *
     * @throws SocketException if binding the socket fails.
     */
    // UDP-Socket连接的内部实现，通过"UDP-Socket委托"与远端建立连接
    private synchronized void connectInternal(InetAddress endpoint, int port) throws SocketException {
        if(port<0 || port>0xFFFF) {
            throw new IllegalArgumentException("connect: " + port);
        }
        
        if(endpoint == null) {
            throw new IllegalArgumentException("connect: null address");
        }
        
        checkAddress(endpoint, "connect");
        
        if(isClosed()) {
            return;
        }
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            if(endpoint.isMulticastAddress()) {
                security.checkMulticast(endpoint);
            } else {
                security.checkConnect(endpoint.getHostAddress(), port);
                security.checkAccept(endpoint.getHostAddress(), port);
            }
        }
        
        // 如果还未绑定，则绑定到本地地址
        if(!isBound()) {
            bind(new InetSocketAddress(0));
        }
        
        // old impls do not support connect/disconnect */
        // 旧式"UDP-Socket委托"不支持连接。新式委托下，需要调用nativeConnectDisabled()判断是否禁用了连接
        if(oldImpl || (impl instanceof AbstractPlainDatagramSocketImpl && ((AbstractPlainDatagramSocketImpl) impl).nativeConnectDisabled())) {
            // 禁用了连接
            connectState = ST_CONNECTED_NO_IMPL;
            
            // 如果允许使用UDP连接
        } else {
            try {
                // 获取"UDP-Socket委托"
                DatagramSocketImpl impl = getImpl();
                
                // 执行连接过程
                impl.connect(endpoint, port);
                
                // 连接正常建立
                connectState = ST_CONNECTED;
                
                /* Do we need to filter some packets */
                /*
                 * 连接完成后，获取连接前就积压的数据包长度。
                 *
                 * 因为建立连接后，要求接收的数据都来自固定的远端地址处，
                 * 所以需要对连接前就积压的数据进行一个校验/过滤
                 */
                int avail = impl.dataAvailable();
                if(avail == -1) {
                    throw new SocketException();
                }
                
                // 如果连接前就存在积压的数据，则后续在接收数据时需要进行一个显式的校验/过滤操作
                explicitFilter = avail>0;
                
                /*
                 * 如果需要显式校验/过滤数据，则需要获取输入流缓冲区大小。
                 * 原因是连接前积压的数据量不会比输入缓冲区容量还大，
                 * 后续只需要对这批数据进行校验/过滤即可。
                 */
                if(explicitFilter) {
                    // 获取输入流缓冲区大小
                    bytesLeftToFilter = getReceiveBufferSize();
                }
            } catch(SocketException se) {
                /* connection will be emulated by DatagramSocket */
                // 连接失败
                connectState = ST_CONNECTED_NO_IMPL;
            }
        }
        
        connectedAddress = endpoint;
        connectedPort = port;
    }
    
}
