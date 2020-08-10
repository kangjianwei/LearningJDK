/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Native;

/**
 * Interface of methods to get/set socket options.  This interface is
 * implemented by: <B>SocketImpl</B> and  <B>DatagramSocketImpl</B>.
 * Subclasses of these should override the methods
 * of this interface in order to support their own options.
 * <P>
 * The methods and constants which specify options in this interface are
 * for implementation only.  If you're not subclassing SocketImpl or
 * DatagramSocketImpl, <B>you won't use these directly.</B> There are
 * type-safe methods to get/set each of these options in Socket, ServerSocket,
 * DatagramSocket and MulticastSocket.
 *
 * @author David Brown
 * @since 1.1
 */
// Java支持的BSD风格的Socket参数名称
public interface SocketOptions {
    
    /*▼ SOL_SOCKET ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /**
     * Set a hint the size of the underlying buffers used by the platform for outgoing network I/O.
     * When used in set, this is a suggestion to the kernel from the application about the size of buffers
     * to use for the data to be sent over the socket.
     * When used in get, this must return the size of the buffer actually used by the platform
     * when sending out data on this socket.
     *
     * Valid for all sockets: SocketImpl, DatagramSocketImpl
     *
     * @see Socket#setSendBufferSize
     * @see Socket#getSendBufferSize
     * @see DatagramSocket#setSendBufferSize
     * @see DatagramSocket#getSendBufferSize
     */
    /*
     * 输出流缓冲区大小
     *
     * 在默认情况下，输出流的发送缓冲区是8096个字节（8K），这个值是Java所建议的输出缓冲区的大小。
     * 如果这个默认值不能满足要求，可以用setSendBufferSize方法来重新设置缓冲区的大小。
     *
     * 适用范围：SocketImpl和DatagramSocketImpl
     * 参数类型：int
     */
    @Native
    public static final int SO_SNDBUF = 0x1001;
    
    /**
     * Set a hint the size of the underlying buffers used by the platform for incoming network I/O.
     * When used in set, this is a suggestion to the kernel from the application about the size of
     * buffers to use for the data to be received over the socket.
     * When used in get, this must return the size of the buffer actually used by the platform when receiving in data on this socket.
     *
     * Valid for all sockets: SocketImpl, DatagramSocketImpl
     *
     * @see Socket#setReceiveBufferSize
     * @see Socket#getReceiveBufferSize
     * @see DatagramSocket#setReceiveBufferSize
     * @see DatagramSocket#getReceiveBufferSize
     */
    /*
     * 输入流缓冲区大小
     *
     * 在默认情况下，输入流的接收缓冲区是8096个字节（8K），这个值是Java所建议的输入缓冲区的大小。
     * 如果这个默认值不能满足要求，可以用setReceiveBufferSize方法来重新设置缓冲区的大小。
     *
     * 适用范围：SocketImpl和DatagramSocketImpl
     * 参数类型：int
     */
    @Native
    public static final int SO_RCVBUF = 0x1002;
    
    /**
     * When the OOBINLINE option is set, any TCP urgent data received on the socket will be received through the socket input stream.
     * When the option is disabled (which is the default) urgent data is silently discarded.
     *
     * @see Socket#setOOBInline
     * @see Socket#getOOBInline
     */
    /*
     * 是否允许发送"紧急数据"
     *
     * 如果这个Socket选项打开，可以通过Socket类的sendUrgentData方法向服务器发送一个单字节的数据。
     * 这个单字节数据并不经过输出缓冲区，而是立即发出。
     * 虽然在客户端并不是使用OutputStream向服务器发送数据，但在服务端程序中这个单字节的数据是和其它的普通数据混在一起的。
     * 因此，在服务端程序中并不知道由客户端发过来的数据是由OutputStream还是由sendUrgentData发过来的。
     *
     * 如果服务端Socket不开启SO_OOBINLINE选项，那么这个数据会被舍弃，可以利用这一点判断远程连接是否已经断开。
     */
    @Native
    public static final int SO_OOBINLINE = 0x1003;
    
    /**
     * Set a timeout on blocking Socket operations:
     * <PRE>
     * ServerSocket.accept();
     * SocketInputStream.read();
     * DatagramSocket.receive();
     * </PRE>
     *
     * The option must be set prior to entering a blocking operation to take effect.
     * If the timeout expires and the operation would continue to block, java.io.InterruptedIOException is raised.
     * The Socket is not closed in this case.
     *
     * <P> Valid for all sockets: SocketImpl, DatagramSocketImpl
     *
     * @see Socket#setSoTimeout
     * @see ServerSocket#setSoTimeout
     * @see DatagramSocket#setSoTimeout
     */
    /*
     * 为以下Socket操作设置超时约束：
     * Socket#setSoTimeout
     * ServerSocket#setSoTimeout
     * DatagramSocket#setSoTimeout
     *
     * 适用范围：SocketImpl和DatagramSocketImpl
     * 参数类型：int
     */
    @Native
    public static final int SO_TIMEOUT = 0x1006;
    
    /**
     * Sets SO_REUSEADDR for a socket.
     * This is used only for MulticastSockets in java, and it is set by default for MulticastSockets.
     * <P>
     * Valid for: DatagramSocketImpl
     */
    /*
     * 是否允许立刻重用已关闭的socket端口
     *
     * 当接收方通过socket close方法关闭socket时，如果网络上还有发送到这个socket数据，底层socket不会立即释放本地端口，
     * 而是等待一段时间，确保接收到了网络上发送过来的延迟数据，然后在释放端口。
     *
     * socket接收到延迟数据后，不会对这些数据作任何处理。
     * socket接收延迟数据目的是确保这些数据不会被其他碰巧绑定到同样的端口的新进程收到。
     * 客户端一般采用随机端口，因此出现两个客户端绑定到同样的端口可能性不大，而服务器端都是使用固定端口，
     *
     * 当服务器端程序关闭后，有可能它的端口还会被占用一段时间，如果此时立刻在此主机上重启服务器程序，
     * 由于服务器端口被占用，使得服务器程序无法绑定改端口，启动失败。
     * 为了确保一个进程关闭socket后，即使它还没释放端口，同一主机上的其他进程可以立刻重用该端口，可以调用socket的setReuseAddress(true)
     *
     * 适用范围：组播-DatagramSocketImpl
     * 参数类型：boolean
     *
     * 参见ServerSocket#setReuseAddress(boolean)
     */
    @Native
    public static final int SO_REUSEADDR = 0x04;
    
    /**
     * When the keepalive option is set for a TCP socket and no data has been exchanged across the socket in either direction for 2 hours
     * (NOTE: the actual value is implementation dependent),
     * TCP automatically sends a keepalive probe to the peer.
     * This probe is a TCP segment to which the peer must respond.
     * One of three responses is expected:
     * 1. The peer responds with the expected ACK.
     * The application is not notified (since everything is OK).
     * TCP will send another probe following another 2 hours of inactivity.
     * 2. The peer responds with an RST, which tells the local TCP that the peer host has crashed and rebooted.
     * The socket is closed.
     * 3. There is no response from the peer.
     * The socket is closed.
     *
     * The purpose of this option is to detect if the peer host crashes.
     *
     * Valid only for TCP socket: SocketImpl
     *
     * @see Socket#setKeepAlive
     * @see Socket#getKeepAlive
     */
    /*
     * 是否开启设置心跳机制
     *
     *  如果将这个参数这是为True，客户端每隔一段时间（一般不少于2小时）就像服务器发送一个试探性的数据包，服务器一般会有三种回应：
     *    1、服务器正常回一个ACK，这表明远程服务器一切OK，那么客户端不会关闭连接，而是再下一个2小时后再发个试探包。
     *    2、服务器返回一个RST，这表明远程服务器挂了，这时候客户端会关闭连接。
     *    3、如果服务器未响应这个数据包，在大约11分钟后，客户端Socket再发送一个数据包，如果在12分钟内，服务器还没响应，那么客户端Socket将关闭。
     *
     * 适用范围：TCP连接-SocketImpl
     * 参数类型：boolean
     */
    @Native
    public static final int SO_KEEPALIVE = 0x08;
    
    /**
     * Sets SO_REUSEPORT for a socket.
     * This option enables and disables the ability to have multiple sockets listen to the same address and port.
     *
     * Valid for: SocketImpl, DatagramSocketImpl
     *
     * @see StandardSocketOptions#SO_REUSEPORT
     * @since 9
     */
    /*
     * 是否允许多个socket监听相同的地址和端口（复用）
     *
     * 允许多个套接字 bind()/listen() 同一个TCP/UDP端口
     * 每一个线程都可以拥有自己的服务器套接字
     * 在服务器套接字上没有了锁的竞争，因为每个进程一个服务器套接字
     * 内核层面实现负载均衡
     * 安全层面，监听同一个端口的套接字只能位于同一个用户下面
     *
     * 适用范围：SocketImpl和DatagramSocketImpl
     * 参数类型：boolean
     */
    @Native
    public static final int SO_REUSEPORT = 0x0E;
    
    /**
     * Fetch the local address binding of a socket.
     * (this option cannot be "set" only "gotten", since sockets are bound at creation time, and so the locally bound address cannot be changed).
     * The default local address of a socket is INADDR_ANY, meaning any local address on a multi-homed host.
     * A multi-homed host can use this option to accept connections to only one of its addresses (in the case of a ServerSocket or DatagramSocket),
     * or to specify its return address to the peer (for a Socket or DatagramSocket).
     * The parameter of this option is an InetAddress.
     *
     * This option must be specified in the constructor.
     *
     * Valid for: SocketImpl, DatagramSocketImpl
     *
     * @see Socket#getLocalAddress
     * @see DatagramSocket#getLocalAddress
     */
    /*
     * 获取Socket绑定的本地IP（该参数只能指示"获取"操作，不能指示"设置"操作，因为地址绑定在创建时已经完成了）
     *
     * 适用范围：SocketImpl和DatagramSocketImpl
     * 参数类型：InetAddress
     */
    @Native
    public static final int SO_BINDADDR = 0x0F;
    
    /**
     * Sets SO_BROADCAST for a socket.
     * This option enables and disables the ability of the process to send broadcast messages.
     * It is supported for only datagram sockets and only on networks that support the concept of a broadcast message (e.g. Ethernet, token ring, etc.),
     * and it is set by default for DatagramSockets.
     *
     * @since 1.4
     */
    /*
     * 是否启用发送广播的能力
     *
     * 适用范围：DatagramSocketImpl
     * 参数类型：boolean
     */
    @Native
    public static final int SO_BROADCAST = 0x20;
    
    /**
     * Specify a linger-on-close timeout.
     * This option disables/enables immediate return from a <B>close()</B> of a TCP Socket.
     * Enabling this option with a non-zero Integer timeout means that
     * a close() will block pending the transmission and acknowledgement of all data written to the peer,
     * at which point the socket is closed gracefully.
     * Upon reaching the linger timeout, the socket is closed <I>forcefully</I>, with a TCP RST.
     * Enabling the option with a timeout of zero does a forceful close immediately.
     * If the specified timeout value exceeds 65,535 it will be reduced to 65,535.
     *
     * Valid only for TCP: SocketImpl
     *
     * @see Socket#setSoLinger
     * @see Socket#getSoLinger
     */
    /*
     * 是否启用延时关闭，如果使用了延时关闭，则调用close之后，会在指定的时间后关闭。
     *
     * 在默认情况下，当调用close关闭socke的使用，close会立即返回。
     * 但是，如果send buffer中还有数据，系统会试着先把send buffer中的数据发送出去，然后close才返回。
     *
     * 适用范围：SocketImpl
     * 参数类型：boolean/int
     *
     * 参见：Socket#setSoLinger
     */
    @Native
    public static final int SO_LINGER = 0x80;
    
    /*▲ SOL_SOCKET ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    
    /*▼ IPPROTO_IP ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /**
     * This option sets the type-of-service or traffic class field in the IP header for a TCP or UDP socket.
     *
     * @since 1.4
     */
    /*
     * IP参数，设置IP头部的Type-of-Service字段，用于描述IP包的优先级和QoS选项
     *
     * 适用范围：SocketImpl
     * 参数类型：int（指定的常量值）
     *
     * 参见：DatagramSocket#setTrafficClass(int)
     */
    @Native
    public static final int IP_TOS = 0x03;
    
    /**
     * Set which outgoing interface on which to send multicast packets.
     * Useful on hosts with multiple network interfaces, where applications want to use other than the system default.
     * Takes/returns an InetAddress.
     * <P>
     * Valid for Multicast: DatagramSocketImpl
     *
     * @see MulticastSocket#setInterface(InetAddress)
     * @see MulticastSocket#getInterface()
     */
    /*
     * 设置组播使用的网络接口
     *
     * 适用范围：组播-MulticastSocket
     * 参数类型：InetAddress
     */
    @Native
    public static final int IP_MULTICAST_IF = 0x10;
    
    /**
     * Same as above.
     * This option is introduced so that the behaviour with IP_MULTICAST_IF will be kept the same as before,
     * while this new option can support setting outgoing interfaces with either IPv4 and IPv6 addresses.
     *
     * NOTE: make sure there is no conflict with this
     *
     * @see MulticastSocket#setNetworkInterface(NetworkInterface)
     * @see MulticastSocket#getNetworkInterface()
     * @since 1.4
     */
    /*
     * 设置组播使用的网络接口，扩展了对IP6的支持
     *
     * 适用范围：组播-MulticastSocket
     * 参数类型：InetAddress
     */
    @Native
    public static final int IP_MULTICAST_IF2 = 0x1f;
    
    /**
     * This option enables or disables local loopback of multicast datagrams.
     * This option is enabled by default for Multicast Sockets.
     *
     * @since 1.4
     */
    /*
     * 是否禁用组播回环（在多播组中，默认情况下一个发出组播信息的节点也会收到自己发送的信息，这称为组播回环）
     *
     * 适用范围：组播-MulticastSocket
     * 参数类型：boolean
     */
    @Native
    public static final int IP_MULTICAST_LOOP = 0x12;
    
    /*▲ IPPROTO_IP ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    
    /*▼ IPPROTO_TCP ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /**
     * Disable Nagle's algorithm for this connection.
     * Written data to the network is not buffered pending acknowledgement of previously written data.
     * <P>
     * Valid for TCP only: SocketImpl.
     *
     * @see Socket#setTcpNoDelay
     * @see Socket#getTcpNoDelay
     */
    /*
     * 是否禁用Nagle算法
     *
     * 在连续多次写入小段数据，接着再读取的场景下，开启Nagle算法可以使得发送方凑齐一波数据再发送，降低网络拥堵，但缺点是增加了延迟。
     * 所以，在客户端应当尽量发送大段数据（可以拼接多个小段数据），并禁用Nagle算法，这样的话既减轻了网络拥堵，又不至于有太长延时。
     *
     * 适用范围：TCP连接-SocketImpl
     * 参数类型：boolean
     */
    @Native
    public static final int TCP_NODELAY = 0x0001;
    
    /*▲ IPPROTO_TCP ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    /**
     * Enable/disable the option specified by <I>optID</I>.
     * If the option is to be enabled, and it takes an option-specific "value",  this is passed in <I>value</I>.
     * The actual type of value is option-specific, and it is an error to pass something that isn't of the expected type:
     * <BR><PRE>
     * SocketImpl s;
     * ...
     * s.setOption(SO_LINGER, new Integer(10));
     * // OK - set SO_LINGER w/ timeout of 10 sec.
     * s.setOption(SO_LINGER, new Double(10));
     * // ERROR - expects java.lang.Integer
     * </PRE>
     * If the requested option is binary, it can be set using this method by
     * a java.lang.Boolean:
     * <BR><PRE>
     * s.setOption(TCP_NODELAY, Boolean.TRUE);
     * // OK - enables TCP_NODELAY, a binary option
     * </PRE>
     * <BR>
     * Any option can be disabled using this method with a Boolean.FALSE:
     * <BR><PRE>
     * s.setOption(TCP_NODELAY, Boolean.FALSE);
     * // OK - disables TCP_NODELAY
     * s.setOption(SO_LINGER, Boolean.FALSE);
     * // OK - disables SO_LINGER
     * </PRE>
     * <BR>
     * For an option that has a notion of on and off, and requires
     * a non-boolean parameter, setting its value to anything other than
     * <I>Boolean.FALSE</I> implicitly enables it.
     * <BR>
     * Throws SocketException if the option is unrecognized,
     * the socket is closed, or some low-level error occurred
     * <BR>
     *
     * @param optID identifies the option
     * @param value the parameter of the socket option
     *
     * @throws SocketException if the option is unrecognized,
     *                         the socket is closed, or some low-level error occurred
     * @see #getOption(int)
     */
    // 根据Socket参数ID（名称），设置参数的值
    void setOption(int optID, Object value) throws SocketException;
    
    /**
     * Fetch the value of an option.
     * Binary options will return java.lang.Boolean.TRUE
     * if enabled, java.lang.Boolean.FALSE if disabled, e.g.:
     * <BR><PRE>
     * SocketImpl s;
     * ...
     * Boolean noDelay = (Boolean)(s.getOption(TCP_NODELAY));
     * if (noDelay.booleanValue()) {
     * // true if TCP_NODELAY is enabled...
     * ...
     * }
     * </PRE>
     * <P>
     * For options that take a particular type as a parameter,
     * getOption(int) will return the parameter's value, else
     * it will return java.lang.Boolean.FALSE:
     * <PRE>
     * Object o = s.getOption(SO_LINGER);
     * if (o instanceof Integer) {
     * System.out.print("Linger time is " + ((Integer)o).intValue());
     * } else {
     * // the true type of o is java.lang.Boolean.FALSE;
     * }
     * </PRE>
     *
     * @param optID an {@code int} identifying the option to fetch
     *
     * @return the value of the option
     *
     * @throws SocketException if the socket is closed
     * @throws SocketException if <I>optID</I> is unknown along the
     *                         protocol stack (including the SocketImpl)
     * @see #setOption(int, java.lang.Object)
     */
    // 返回指定参数的值
    Object getOption(int optID) throws SocketException;
    
}
