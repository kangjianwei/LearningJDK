/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import sun.net.ResourceManager;
import sun.security.action.GetPropertyAction;

/**
 * Abstract datagram and multicast socket implementation base class.
 * Note: This is not a public class, so that applets cannot call
 * into the implementation directly and hence cannot bypass the
 * security checks present in the DatagramSocket and MulticastSocket
 * classes.
 *
 * @author Pavani Diwanji
 */
// 普通"UDP-Socket委托"的抽象实现，更进一步的实现取决于不同的平台
abstract class AbstractPlainDatagramSocketImpl extends DatagramSocketImpl {
    
    // 获取操作系统(平台)名称
    private static final String os = GetPropertyAction.privilegedGetProperty("os.name");
    /**
     * flag set if the native connect() call not to be used
     */
    // 是否禁用了UDP连接(苹果系统下默认是禁止的)
    private static final boolean connectDisabled = os.contains("OS X");
    
    boolean connected = false;  // 是否建立了连接
    
    protected InetAddress connectedAddress = null; // 连接到的远程IP
    private int connectedPort = -1;   // 连接到的远程端口
    
    // 是否对isReusePortAvailable进行过检验
    private static volatile boolean checkedReusePort;
    // 是否允许多个socket监听相同的地址和端口
    private static volatile boolean isReusePortAvailable;
    
    /** timeout value for receive() */
    // 接收(查看)数据的超时
    int timeout = 0;
    
    // IP参数，用于设置IP头部的Type-of-Service字段
    private int trafficClass = 0;
    
    
    /**
     * Load net library into runtime.
     */
    static {
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<>() {
            public Void run() {
                System.loadLibrary("net");
                return null;
            }
        });
    }
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a datagram socket
     */
    // 创建Socket文件，并记下其文件描述符到fd字段中（该文件描述符会被清理器追踪）
    protected synchronized void create() throws SocketException {
        // 创建UDP-Socket之时的回调
        ResourceManager.beforeUdpCreate();
        
        // 创建Java层的Socket文件描述符中
        fd = new FileDescriptor();
        
        try {
            // 创建一个socket文件，并将其本地文件描述符存储到Java层的文件描述符中
            datagramSocketCreate();
            
            // 注册Socket的文件描述符到清理器
            SocketCleanable.register(fd);
        } catch(SocketException ioe) {
            // 关闭UDP-Socket之时的回调
            ResourceManager.afterUdpClose();
            fd = null;
            throw ioe;
        }
    }
    
    /**
     * Binds a datagram socket to a local port.
     */
    // 将UDP-Socket绑定到指定的IP和端口
    protected synchronized void bind(int lport, InetAddress laddr) throws SocketException {
        bind0(lport, laddr);
    }
    
    /**
     * Connects a datagram socket to a remote destination.
     * This associates the remote address with the local socket
     * so that datagrams may only be sent to this destination and received from this destination.
     *
     * @param address the remote InetAddress to connect to
     * @param port    the remote port number
     */
    // 将UDP-Socket连接到远程地址，之后只能向该远程地址发送数据，或从该远程地址接收数据。
    protected void connect(InetAddress address, int port) throws SocketException {
        connect0(address, port);
        connectedAddress = address;
        connectedPort = port;
        connected = true;
    }
    
    /**
     * Disconnects a previously connected socket.
     * Does nothing if the socket was not connected already.
     */
    // 断开UDP-Socket与远程的连接
    protected void disconnect() {
        disconnect0(connectedAddress.holder().getFamily());
        connected = false;
        connectedAddress = null;
        connectedPort = -1;
    }
    
    
    protected abstract void datagramSocketCreate() throws SocketException;
    
    protected abstract void bind0(int lport, InetAddress laddr) throws SocketException;
    
    protected abstract void connect0(InetAddress address, int port) throws SocketException;
    
    protected abstract void disconnect0(int family);
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sends a datagram packet.
     * The packet contains the data and the destination address to send the packet to.
     *
     * @param p the packet to be sent.
     */
    // 发送UDP数据包，待发送的数据存储在packet中
    protected abstract void send(DatagramPacket packet) throws IOException;
    
    /**
     * Receive the datagram packet.
     *
     * @param packet the packet to receive into
     */
    // 接收UDP数据包，接收到的数据存储到packet中
    protected synchronized void receive(DatagramPacket packet) throws IOException {
        receive0(packet);
    }
    
    
    /**
     * Peek at the packet to see who it is from.
     *
     * @param address the address to populate with the sender address
     */
    // 预读：查看指定远端IP处积压的首个UDP数据包，并返回其存储的端口号
    protected abstract int peek(InetAddress address) throws IOException;
    
    // 预读：复制远端积压的首个UDP数据包到packet中，并返回其存储的端口号
    protected abstract int peekData(DatagramPacket packet) throws IOException;
    
    
    protected abstract void receive0(DatagramPacket packet) throws IOException;
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Close the socket.
     */
    // 关闭UDP-Socket
    protected void close() {
        if(fd == null) {
            return;
        }
        
        // 取消清理器对fd的追踪
        SocketCleanable.unregister(fd);
        
        datagramSocketClose();
        
        // 关闭UDP-Socket之时的回调
        ResourceManager.afterUdpClose();
        
        fd = null;
    }
    
    // 关闭UDP-Socket的内部实现
    protected abstract void datagramSocketClose();
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断UDP-Socket是否已关闭
    protected boolean isClosed() {
        return fd == null;
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Get the TTL (time-to-live) option.
     */
    // 获取网络跳数
    protected abstract int getTimeToLive() throws IOException;
    
    /**
     * Set the TTL (time-to-live) option.
     *
     * @param ttl TTL to be set.
     */
    // 设置网络跳数
    protected abstract void setTimeToLive(int ttl) throws IOException;
    
    /**
     * Get the TTL (time-to-live) option.
     */
    // 获取网络跳数，已过时，参见getTimeToLive()
    @Deprecated
    protected abstract byte getTTL() throws IOException;
    
    /**
     * Set the TTL (time-to-live) option.
     *
     * @param ttl TTL to be set.
     */
    // 设置网络跳数，已过时，参见setTimeToLive(int)
    @Deprecated
    protected abstract void setTTL(byte ttl) throws IOException;
    
    
    /**
     * Returns a set of SocketOptions supported by this impl and by this impl's
     * socket (Socket or ServerSocket)
     *
     * @return a Set of SocketOptions
     */
    // 获取当前UDP-Socket支持的所有参数
    @Override
    protected Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> options;
        
        if(isReusePortAvailable()) {
            options = new HashSet<>();
            options.addAll(super.supportedOptions());
            options.add(StandardSocketOptions.SO_REUSEPORT);
            options = Collections.unmodifiableSet(options);
        } else {
            options = super.supportedOptions();
        }
        
        return options;
    }
    
    // 根据UDP-Socket参数ID（名称），返回参数的值
    public Object getOption(int optID) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket Closed");
        }
        
        Object result;
        
        switch(optID) {
            case SO_TIMEOUT:
                result = timeout;
                break;
            
            case IP_TOS:
                result = socketGetOption(optID);
                if((Integer) result == -1) {
                    result = trafficClass;
                }
                break;
            
            case SO_BINDADDR:
            case IP_MULTICAST_IF:
            case IP_MULTICAST_IF2:
            case SO_RCVBUF:
            case SO_SNDBUF:
            case IP_MULTICAST_LOOP:
            case SO_REUSEADDR:
            case SO_BROADCAST:
                result = socketGetOption(optID);
                break;
            
            case SO_REUSEPORT:
                if(!supportedOptions().contains(StandardSocketOptions.SO_REUSEPORT)) {
                    throw new UnsupportedOperationException("unsupported option");
                }
                result = socketGetOption(optID);
                break;
            
            default:
                throw new SocketException("invalid option: " + optID);
        }
        
        return result;
    }
    
    /**
     * set a value - since we only support (setting) binary options
     * here, o must be a Boolean
     */
    // 根据UDP-Socket参数ID（名称），设置参数的值
    public void setOption(int optID, Object o) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket Closed");
        }
        
        switch(optID) {
            /*
             * check type safety b4 going native.
             * These should never fail, since only java.Socket* has access to PlainSocketImpl.setOption().
             */
            case SO_TIMEOUT:
                if(!(o instanceof Integer)) {
                    throw new SocketException("bad argument for SO_TIMEOUT");
                }
                int tmp = (Integer) o;
                if(tmp<0) {
                    throw new IllegalArgumentException("timeout < 0");
                }
                timeout = tmp;
                return;
            case IP_TOS:
                if(!(o instanceof Integer)) {
                    throw new SocketException("bad argument for IP_TOS");
                }
                trafficClass = (Integer) o;
                break;
            case SO_REUSEADDR:
                if(!(o instanceof Boolean)) {
                    throw new SocketException("bad argument for SO_REUSEADDR");
                }
                break;
            case SO_BROADCAST:
                if(!(o instanceof Boolean)) {
                    throw new SocketException("bad argument for SO_BROADCAST");
                }
                break;
            case SO_BINDADDR:
                throw new SocketException("Cannot re-bind Socket");
            case SO_RCVBUF:
            case SO_SNDBUF:
                if(!(o instanceof Integer) || (Integer) o<0) {
                    throw new SocketException("bad argument for SO_SNDBUF or " + "SO_RCVBUF");
                }
                break;
            case IP_MULTICAST_IF:
                if(!(o instanceof InetAddress)) {
                    throw new SocketException("bad argument for IP_MULTICAST_IF");
                }
                break;
            case IP_MULTICAST_IF2:
                if(!(o instanceof NetworkInterface)) {
                    throw new SocketException("bad argument for IP_MULTICAST_IF2");
                }
                break;
            case IP_MULTICAST_LOOP:
                if(!(o instanceof Boolean)) {
                    throw new SocketException("bad argument for IP_MULTICAST_LOOP");
                }
                break;
            case SO_REUSEPORT:
                if(!(o instanceof Boolean)) {
                    throw new SocketException("bad argument for SO_REUSEPORT");
                }
                if(!supportedOptions().contains(StandardSocketOptions.SO_REUSEPORT)) {
                    throw new UnsupportedOperationException("unsupported option");
                }
                break;
            default:
                throw new SocketException("invalid option: " + optID);
        }
        socketSetOption(optID, o);
    }
    
    
    protected abstract void socketSetOption(int opt, Object val) throws SocketException;
    
    protected abstract Object socketGetOption(int opt) throws SocketException;
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 组播操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Join the multicast group.
     *
     * @param group multicast address to join.
     */
    // 将当前组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据
    protected void join(InetAddress group) throws IOException {
        join(group, null);
    }
    
    /**
     * Leave the multicast group.
     *
     * @param group multicast address to leave.
     */
    // 将当前组播Socket从group处的组播小组中移除，后续它将无法接收到该小组中的数据
    protected void leave(InetAddress group) throws IOException {
        leave(group, null);
    }
    
    /**
     * Join the multicast group.
     *
     * @param group  multicast address to join.
     * @param interf specifies the local interface to receive multicast datagram packets
     *
     * @throws IllegalArgumentException if mcastaddr is null or is a SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    // 将当前组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据；interf是用来接收消息的网络接口(网卡)
    protected void joinGroup(SocketAddress group, NetworkInterface interf) throws IOException {
        if(!(group instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        join(((InetSocketAddress) group).getAddress(), interf);
    }
    
    /**
     * Leave the multicast group.
     *
     * @param group  multicast address to leave.
     * @param interf specified the local interface to leave the group at
     *
     * @throws IllegalArgumentException if mcastaddr is null or is a SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    // 将当前组播Socket从group处的组播小组中移除，后续它将无法接收到该小组中的数据；interf是用来接收消息的网络接口(网卡)
    protected void leaveGroup(SocketAddress group, NetworkInterface interf) throws IOException {
        if(!(group instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        leave(((InetSocketAddress) group).getAddress(), interf);
    }
    
    
    protected abstract void join(InetAddress group, NetworkInterface interf) throws IOException;
    
    protected abstract void leave(InetAddress group, NetworkInterface interf) throws IOException;
    
    /*▲ 组播操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回当前Socket中积压的待读数据量，默认实现为0
    abstract int dataAvailable();
    
    // 是否禁用了UDP连接
    protected boolean nativeConnectDisabled() {
        return connectDisabled;
    }
    
    /**
     * Tells whether SO_REUSEPORT is supported.
     */
    // 判断是否允许多个socket监听相同的地址和端口
    static boolean isReusePortAvailable() {
        if(!checkedReusePort) {
            isReusePortAvailable = isReusePortAvailable0();
            checkedReusePort = true;
        }
        
        return isReusePortAvailable;
    }
    
    private static native boolean isReusePortAvailable0();
    
}
