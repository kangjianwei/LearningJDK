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

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Set;

/**
 * Abstract datagram and multicast socket implementation base class.
 *
 * @author Pavani Diwanji
 * @since 1.1
 */
/*
 * "UDP-Socket委托"，这相当于UDP-Socket的实现类，是真正实现两个UDP端通信的类
 *
 * 每个UDP端都会持有一个"UDP-Socket委托"；
 * 当然，"UDP-Socket委托"内部也保存了持有当前委托的那个"UDP-Socket"。
 */
public abstract class DatagramSocketImpl implements SocketOptions {
    
    /**
     * The DatagramSocket or MulticastSocket that owns this impl
     */
    // 被当前"UDP-Socket委托"引用的UDP-Socket，即持有当前"UDP-Socket委托"的那个"UDP-Socket"
    DatagramSocket socket;
    
    /**
     * The file descriptor object.
     */
    // 被当前"UDP-Socket委托"引用的UDP-Socket的文件描述符
    protected FileDescriptor fd;
    
    /**
     * The local port number.
     */
    // 本地端口
    protected int localPort;
    
    // 普通UDP-Socket支持的参数
    private static final Set<SocketOption<?>> dgSocketOptions;
    // 组播Socket支持的参数
    private static final Set<SocketOption<?>> mcSocketOptions;
    
    
    static {
        dgSocketOptions = Set.of(StandardSocketOptions.SO_SNDBUF, StandardSocketOptions.SO_RCVBUF, StandardSocketOptions.SO_REUSEADDR, StandardSocketOptions.IP_TOS);
        
        mcSocketOptions = Set.of(StandardSocketOptions.SO_SNDBUF, StandardSocketOptions.SO_RCVBUF, StandardSocketOptions.SO_REUSEADDR, StandardSocketOptions.IP_TOS, StandardSocketOptions.IP_MULTICAST_IF, StandardSocketOptions.IP_MULTICAST_TTL, StandardSocketOptions.IP_MULTICAST_LOOP);
    }
    
    
    
    /*▼ 创建 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a datagram socket.
     *
     * @throws SocketException if there is an error in the
     *                         underlying protocol, such as a TCP error.
     */
    // 创建Socket文件，并记下其文件描述符到fd字段中（该文件描述符会被清理器追踪）
    protected abstract void create() throws SocketException;
    
    /*▲ 创建 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Binds a datagram socket to a local port and address.
     *
     * @param lport the local port
     * @param laddr the local address
     *
     * @throws SocketException if there is an error in the
     *                         underlying protocol, such as a TCP error.
     */
    /*
     * 对UDP-Socket执行【bind】操作。
     *
     * lport: 待绑定的IP
     * laddr: 待绑定的端口
     */
    protected abstract void bind(int lport, InetAddress laddr) throws SocketException;
    
    /**
     * Connects a datagram socket to a remote destination. This associates the remote
     * address with the local socket so that datagrams may only be sent to this destination
     * and received from this destination. This may be overridden to call a native
     * system connect.
     *
     * <p>If the remote destination to which the socket is connected does not
     * exist, or is otherwise unreachable, and if an ICMP destination unreachable
     * packet has been received for that address, then a subsequent call to
     * send or receive may throw a PortUnreachableException.
     * Note, there is no guarantee that the exception will be thrown.
     *
     * @param address the remote InetAddress to connect to
     * @param port    the remote port number
     *
     * @throws SocketException may be thrown if the socket cannot be
     *                         connected to the remote destination
     * @since 1.4
     */
    /*
     * 对UDP-Socket执行【connect】操作。
     *
     * 将UDP-Socket连接到远程地址，之后只能向该远程地址发送数据，或从该远程地址接收数据。
     *
     * 注：这与TCP-Socket中的绑定意义完全不同，不会经过握手验证
     */
    protected void connect(InetAddress address, int port) throws SocketException {
    }
    
    /**
     * Disconnects a datagram socket from its remote destination.
     *
     * @since 1.4
     */
    /*
     * 对UDP-Socket执行【disconnect】操作，即断开连接。
     */
    protected void disconnect() {
    }
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sends a datagram packet.
     * The packet contains the data and the destination address to send the packet to.
     *
     * @param packet the packet to be sent.
     *
     * @throws IOException              if an I/O exception occurs while sending the
     *                                  datagram packet.
     * @throws PortUnreachableException may be thrown if the socket is connected
     *                                  to a currently unreachable destination. Note, there is no guarantee that
     *                                  the exception will be thrown.
     */
    // 发送UDP数据包，待发送的数据存储在packet中
    protected abstract void send(DatagramPacket packet) throws IOException;
    
    /**
     * Receive the datagram packet.
     *
     * @param packet the Packet Received.
     *
     * @throws IOException              if an I/O exception occurs
     *                                  while receiving the datagram packet.
     * @throws PortUnreachableException may be thrown if the socket is connected
     *                                  to a currently unreachable destination. Note, there is no guarantee that the
     *                                  exception will be thrown.
     */
    // 接收UDP数据包，接收到的数据存储到packet中
    protected abstract void receive(DatagramPacket packet) throws IOException;
    
    
    /**
     * Peek at the packet to see who it is from.
     * Updates the specified {@code InetAddress} to the address which the packet came from.
     *
     * @param address an InetAddress object
     *
     * @return the port number which the packet came from.
     *
     * @throws IOException              if an I/O exception occurs
     * @throws PortUnreachableException may be thrown if the socket is connected
     *                                  to a currently unreachable destination. Note, there is no guarantee that the
     *                                  exception will be thrown.
     */
    // 预读：查看指定远端IP处积压的首个UDP数据包，并返回其存储的端口号
    protected abstract int peek(InetAddress address) throws IOException;
    
    /**
     * Peek at the packet to see who it is from. The data is copied into the specified {@code DatagramPacket}.
     * The data is returned, but not consumed, so that a subsequent peekData/receive operation will see the same data.
     *
     * @param packet the Packet Received.
     *
     * @return the port number which the packet came from.
     *
     * @throws IOException              if an I/O exception occurs
     * @throws PortUnreachableException may be thrown if the socket is connected to a currently unreachable destination.
     *                                  Note, there is no guarantee that the exception will be thrown.
     * @since 1.4
     */
    // 预读：复制远端积压的首个UDP数据包到packet中，并返回其存储的端口号
    protected abstract int peekData(DatagramPacket packet) throws IOException;
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Close the socket.
     */
    // 关闭UDP-Socket
    protected abstract void close();
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the local port.
     *
     * @return an {@code int} representing the local port value
     */
    // 返回当前UDP-Socket绑定的本地端口
    protected int getLocalPort() {
        return localPort;
    }
    
    /*▲ 地址 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieve the TTL (time-to-live) option.
     *
     * @return an {@code int} representing the time-to-live value
     *
     * @throws IOException if an I/O exception occurs
     *                     while retrieving the time-to-live option
     * @see #setTimeToLive(int)
     */
    // 获取网络跳数
    protected abstract int getTimeToLive() throws IOException;
    
    /**
     * Set the TTL (time-to-live) option.
     *
     * @param ttl an {@code int} specifying the time-to-live value
     *
     * @throws IOException if an I/O exception occurs
     *                     while setting the time-to-live option.
     * @see #getTimeToLive()
     */
    // 设置网络跳数
    protected abstract void setTimeToLive(int ttl) throws IOException;
    
    /**
     * Retrieve the TTL (time-to-live) option.
     *
     * @return a byte representing the TTL value
     *
     * @throws IOException if an I/O exception occurs
     *                     while retrieving the time-to-live option
     * @see #setTTL(byte)
     * @deprecated use getTimeToLive instead.
     */
    // 获取网络跳数，已过时，参见getTimeToLive()
    @Deprecated
    protected abstract byte getTTL() throws IOException;
    
    /**
     * Set the TTL (time-to-live) option.
     *
     * @param ttl a byte specifying the TTL value
     *
     * @throws IOException if an I/O exception occurs while setting
     *                     the time-to-live option.
     * @see #getTTL()
     * @deprecated use setTimeToLive instead.
     */
    // 设置网络跳数，已过时，参见setTimeToLive(int)
    @Deprecated
    protected abstract void setTTL(byte ttl) throws IOException;
    
    
    /**
     * Returns a set of SocketOptions supported by this impl
     * and by this impl's socket (DatagramSocket or MulticastSocket)
     *
     * @return a Set of SocketOptions
     *
     * @since 9
     */
    // 获取当前UDP-Socket支持的所有参数
    protected Set<SocketOption<?>> supportedOptions() {
        if(getDatagramSocket() instanceof MulticastSocket) {
            return mcSocketOptions;
        } else {
            return dgSocketOptions;
        }
    }
    
    /**
     * Called to get a socket option.
     *
     * @param <T>  The type of the socket option value
     * @param name The socket option
     *
     * @return the socket option
     *
     * @throws UnsupportedOperationException if the DatagramSocketImpl does not
     *                                       support the option
     * @throws NullPointerException          if name is {@code null}
     * @throws IOException                   if an I/O problem occurs while attempting to set the option
     * @since 9
     */
    // 获取指定名称的UDP-Socket配置参数
    @SuppressWarnings("unchecked")
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        if(name == StandardSocketOptions.SO_SNDBUF) {
            return (T) getOption(SocketOptions.SO_SNDBUF);
        } else if(name == StandardSocketOptions.SO_RCVBUF) {
            return (T) getOption(SocketOptions.SO_RCVBUF);
        } else if(name == StandardSocketOptions.SO_REUSEADDR) {
            return (T) getOption(SocketOptions.SO_REUSEADDR);
        } else if(name == StandardSocketOptions.SO_REUSEPORT && supportedOptions().contains(name)) {
            return (T) getOption(SocketOptions.SO_REUSEPORT);
        } else if(name == StandardSocketOptions.IP_TOS) {
            return (T) getOption(SocketOptions.IP_TOS);
        } else if(name == StandardSocketOptions.IP_MULTICAST_IF && (getDatagramSocket() instanceof MulticastSocket)) {
            return (T) getOption(SocketOptions.IP_MULTICAST_IF2);
        } else if(name == StandardSocketOptions.IP_MULTICAST_TTL && (getDatagramSocket() instanceof MulticastSocket)) {
            Integer ttl = getTimeToLive();
            return (T) ttl;
        } else if(name == StandardSocketOptions.IP_MULTICAST_LOOP && (getDatagramSocket() instanceof MulticastSocket)) {
            return (T) getOption(SocketOptions.IP_MULTICAST_LOOP);
        } else {
            throw new UnsupportedOperationException("unsupported option");
        }
    }
    
    /**
     * Called to set a socket option.
     *
     * @param <T>   The type of the socket option value
     * @param name  The socket option
     * @param value The value of the socket option. A value of {@code null}
     *              may be valid for some options.
     *
     * @throws UnsupportedOperationException if the DatagramSocketImpl does not
     *                                       support the option
     * @throws NullPointerException          if name is {@code null}
     * @throws IOException                   if an I/O problem occurs while attempting to set the option
     * @since 9
     */
    // 设置指定名称的UDP-Socket配置参数
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        if(name == StandardSocketOptions.SO_SNDBUF) {
            setOption(SocketOptions.SO_SNDBUF, value);
        } else if(name == StandardSocketOptions.SO_RCVBUF) {
            setOption(SocketOptions.SO_RCVBUF, value);
        } else if(name == StandardSocketOptions.SO_REUSEADDR) {
            setOption(SocketOptions.SO_REUSEADDR, value);
        } else if(name == StandardSocketOptions.SO_REUSEPORT && supportedOptions().contains(name)) {
            setOption(SocketOptions.SO_REUSEPORT, value);
        } else if(name == StandardSocketOptions.IP_TOS) {
            setOption(SocketOptions.IP_TOS, value);
        } else if(name == StandardSocketOptions.IP_MULTICAST_IF && (getDatagramSocket() instanceof MulticastSocket)) {
            setOption(SocketOptions.IP_MULTICAST_IF2, value);
        } else if(name == StandardSocketOptions.IP_MULTICAST_TTL && (getDatagramSocket() instanceof MulticastSocket)) {
            if(!(value instanceof Integer)) {
                throw new IllegalArgumentException("not an integer");
            }
            setTimeToLive((Integer) value);
        } else if(name == StandardSocketOptions.IP_MULTICAST_LOOP && (getDatagramSocket() instanceof MulticastSocket)) {
            setOption(SocketOptions.IP_MULTICAST_LOOP, value);
        } else {
            throw new UnsupportedOperationException("unsupported option");
        }
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 组播操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Join the multicast group.
     *
     * @param group multicast address to join.
     *
     * @throws IOException if an I/O exception occurs while joining the multicast group.
     */
    // 将当前组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据
    protected abstract void join(InetAddress group) throws IOException;
    
    /**
     * Leave the multicast group.
     *
     * @param group multicast address to leave.
     *
     * @throws IOException if an I/O exception occurs while leaving the multicast group.
     */
    // 将当前组播Socket从group处的组播小组中移除，后续它将无法接收到该小组中的数据
    protected abstract void leave(InetAddress group) throws IOException;
    
    /**
     * Join the multicast group.
     *
     * @param group  address to join.
     * @param interf specifies the local interface to receive multicast datagram packets
     *
     * @throws IOException if an I/O exception occurs while joining the multicast group
     * @since 1.4
     */
    // 将当前组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据；interf是用来接收消息的网络接口(网卡)
    protected abstract void joinGroup(SocketAddress group, NetworkInterface interf) throws IOException;
    
    /**
     * Leave the multicast group.
     *
     * @param group  address to leave.
     * @param interf specified the local interface to leave the group at
     *
     * @throws IOException if an I/O exception occurs while leaving the multicast group
     * @since 1.4
     */
    // 将当前组播Socket从group处的组播小组中移除，后续它将无法接收到该小组中的数据；interf是用来接收消息的网络接口(网卡)
    protected abstract void leaveGroup(SocketAddress group, NetworkInterface interf) throws IOException;
    
    /*▲ 组播操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 将指定的UDP-Socket关联到当前"UDP-Socket委托"中
    void setDatagramSocket(DatagramSocket socket) {
        this.socket = socket;
    }
    
    // 返回被当前"UDP-Socket委托"引用的UDP-Socket
    DatagramSocket getDatagramSocket() {
        return socket;
    }
    
    /**
     * Gets the datagram socket file descriptor.
     *
     * @return a {@code FileDescriptor} object representing the datagram socket
     * file descriptor
     */
    // 返回被当前"UDP-Socket委托"引用的UDP-Socket的文件描述符
    protected FileDescriptor getFileDescriptor() {
        return fd;
    }
    
    // 返回当前Socket中积压的待读数据量，默认实现为0
    int dataAvailable() {
        // default impl returns zero, which disables the calling functionality
        return 0;
    }
    
}
