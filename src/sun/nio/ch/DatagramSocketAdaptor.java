/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.util.Objects;

/**
 * Make a datagram-socket channel look like a datagram socket.
 * The methods in this class are defined in exactly the same order as in
 * java.net.DatagramSocket so as to simplify tracking future changes to that class.
 */
// DatagramSocket适配器，用来将DatagramChannelImpl当做DatagramSocket使唤
public class DatagramSocketAdaptor extends DatagramSocket {
    
    /**
     * A dummy implementation of DatagramSocketImpl that can be passed to the DatagramSocket constructor
     * so that no native resources are allocated in super class.
     */
    /*
     * 一个空白的"UDP-Socket委托"，用来在构造器中欺骗UDP-Socket。
     * 后续会用一个UDP-Socket通道来伪装成"UDP-Socket委托"供UDP-Socket使用。
     */
    private static final DatagramSocketImpl dummyDatagramSocket = new DatagramSocketImpl() {
        public Object getOption(int optID) throws SocketException {
            return null;
        }
        
        public void setOption(int optID, Object value) throws SocketException {
        }
        
        protected void create() throws SocketException {
        }
        
        protected void bind(int lport, InetAddress laddr) throws SocketException {
        }
        
        protected void send(DatagramPacket packet) throws IOException {
        }
        
        protected int peek(InetAddress i) throws IOException {
            return 0;
        }
        
        protected int peekData(DatagramPacket packet) throws IOException {
            return 0;
        }
        
        protected void receive(DatagramPacket packet) throws IOException {
        }
        
        @Deprecated
        protected byte getTTL() throws IOException {
            return 0;
        }
        
        @Deprecated
        protected void setTTL(byte ttl) throws IOException {
        }
        
        protected int getTimeToLive() throws IOException {
            return 0;
        }
        
        protected void setTimeToLive(int ttl) throws IOException {
        }
        
        protected void join(InetAddress inetaddr) throws IOException {
        }
        
        protected void leave(InetAddress inetaddr) throws IOException {
        }
        
        protected void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        }
        
        protected void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        }
        
        protected void close() {
        }
    };
    
    // 将要被适配的UDP-Socket通道；适配之后，该通道可以被当成普通的UDP-Socket使用
    private final DatagramChannelImpl datagramChannel;
    
    /** Timeout "option" value for receives */
    // 等待接收数据时的超时设置
    private volatile int timeout;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 构造UDP-Socket适配器，用来将指定的UDP-Socket通道"伪装"为UDP-Socket。
     *
     * 在这里，不再需要传统的"UDP-Scoket委托"，而是需要参数中的UDP-Socket通道来做委托。
     * 由于这里的构造器中需要显式调用父类的构造器，但又不能使用传统的"UDP-Scoket委托"，
     * 因此给父构造器赋了一个空白的"UDP-Socket委托"值来欺骗父构造器。
     */
    private DatagramSocketAdaptor(DatagramChannelImpl datagramChannel) throws IOException {
        /*
         * Invoke the DatagramSocketAdaptor(SocketAddress) constructor,
         * passing a dummy DatagramSocketImpl object to avoid any native
         * resource allocation in super class and invoking our bind method
         * before the dc field is initialized.
         */
        super(dummyDatagramSocket);
        this.datagramChannel = datagramChannel;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回由指定的UDP-Socket通道适配而成的UDP-Socket
    public static DatagramSocket create(DatagramChannelImpl datagramChannel) {
        try {
            return new DatagramSocketAdaptor(datagramChannel);
        } catch(IOException x) {
            throw new Error(x);
        }
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 对UDP-Socket执行【bind】操作。
     *
     * local: 待绑定的地址(ip+port)
     */
    public void bind(SocketAddress local) throws SocketException {
        try {
            if(local == null) {
                local = new InetSocketAddress(0);
            }
            
            // 将UDP-Socket(通道)dc绑定到指定的本地地址
            datagramChannel.bind(local);
        } catch(Exception x) {
            Net.translateToSocketException(x);
        }
    }
    
    /*
     * 对UDP-Socket执行【connect】操作。
     *
     * 将UDP-Socket连接到远程地址，之后只能向该远程地址发送数据，或从该远程地址接收数据。
     *
     * 注：这与TCP-Socket中的绑定意义完全不同，不会经过握手验证
     */
    public void connect(InetAddress address, int port) {
        try {
            connectInternal(new InetSocketAddress(address, port));
        } catch(SocketException x) {
            // Yes, j.n.DatagramSocket really does this
        }
    }
    
    /*
     * 对UDP-Socket执行【connect】操作。
     *
     * 将UDP-Socket连接到远程地址，之后只能向该远程地址发送数据，或从该远程地址接收数据。
     *
     * 注：这与TCP-Socket中的绑定意义完全不同，不会经过握手验证
     */
    public void connect(SocketAddress remote) throws SocketException {
        Objects.requireNonNull(remote, "Address can't be null");
        connectInternal(remote);
    }
    
    /*
     * 对UDP-Socket执行【disconnect】操作，即断开连接。
     */
    public void disconnect() {
        try {
            datagramChannel.disconnect();
        } catch(IOException x) {
            throw new Error(x);
        }
    }
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向远端(目的地)发送packet中存储的UDP数据包
    public void send(DatagramPacket packet) throws IOException {
        synchronized(datagramChannel.blockingLock()) {
            
            // 如果是非阻塞Socket，则抛出异常
            if(!datagramChannel.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
            
            try {
                synchronized(packet) {
                    ByteBuffer bb = ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());
                    
                    // 已连接到远端
                    if(datagramChannel.isConnected()) {
                        // 如果UDP数据包还未设置目的地地址
                        if(packet.getAddress() == null) {
                            /* Legacy DatagramSocket will send in this case and set address and port of the packet */
                            // 获取连接的远程地址
                            InetSocketAddress isa = datagramChannel.remoteAddress();
                            packet.setPort(isa.getPort());
                            packet.setAddress(isa.getAddress());
                            datagramChannel.write(bb);
                        } else {
                            // Target address may not match connected address
                            datagramChannel.send(bb, packet.getSocketAddress());
                        }
                        
                        // 未设置连接
                    } else {
                        // Not connected so address must be valid or throw
                        datagramChannel.send(bb, packet.getSocketAddress());
                    }
                }
            } catch(IOException x) {
                Net.translateException(x);
            }
        }
    }
    
    // 从远端UDP-Socket(通道)接收数据并存入packet
    public void receive(DatagramPacket packet) throws IOException {
        synchronized(datagramChannel.blockingLock()) {
            
            // 如果是非阻塞Socket，则抛出异常
            if(!datagramChannel.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
            
            try {
                synchronized(packet) {
                    ByteBuffer bb = ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());
                    // 从远端接收数据存入bb，并返回接收到字节数
                    SocketAddress sender = receive(bb);
                    packet.setSocketAddress(sender);
                    packet.setLength(bb.position() - packet.getOffset());
                }
            } catch(IOException x) {
                Net.translateException(x);
            }
        }
    }
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭UDP-Socket
    public void close() {
        try {
            datagramChannel.close();
        } catch(IOException x) {
            throw new Error(x);
        }
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回当前UDP-Socket绑定的本地IP
    public InetAddress getLocalAddress() {
        if(isClosed()) {
            return null;
        }
        
        // 获取绑定的本地地址
        InetSocketAddress local = datagramChannel.localAddress();
        if(local == null) {
            local = new InetSocketAddress(0);
        }
        
        InetAddress result = local.getAddress();
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            try {
                sm.checkConnect(result.getHostAddress(), -1);
            } catch(SecurityException x) {
                return new InetSocketAddress(0).getAddress();
            }
        }
        
        return result;
    }
    
    // 返回当前UDP-Socket绑定的本地端口
    public int getLocalPort() {
        if(isClosed()) {
            return -1;
        }
        
        try {
            // 获取绑定的本地地址
            InetSocketAddress local = datagramChannel.localAddress();
            if(local != null) {
                return local.getPort();
            }
        } catch(Exception x) {
        }
        
        return 0;
    }
    
    
    // 返回当前UDP-Socket连接的远端IP
    public InetAddress getInetAddress() {
        // 获取连接的远程地址
        InetSocketAddress remote = datagramChannel.remoteAddress();
        return (remote != null) ? remote.getAddress() : null;
    }
    
    // 返回当前UDP-Socket连接的远端端口
    public int getPort() {
        // 获取连接的远程地址
        InetSocketAddress remote = datagramChannel.remoteAddress();
        return (remote != null) ? remote.getPort() : -1;
    }
    
    /*▲ 地址 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断UDP-Socket是否已绑定
    public boolean isBound() {
        return datagramChannel.localAddress() != null;
    }
    
    // 判断UDP-Socket是否已连接
    public boolean isConnected() {
        return datagramChannel.remoteAddress() != null;
    }
    
    // 判断UDP-Socket是否已关闭
    public boolean isClosed() {
        return !datagramChannel.isOpen();
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取超时约束的时间
    public int getSoTimeout() throws SocketException {
        return timeout;
    }
    
    // 设置超时约束的时间
    public void setSoTimeout(int timeout) throws SocketException {
        this.timeout = timeout;
    }
    
    // 获取输出流缓冲区大小
    public int getSendBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_SNDBUF);
    }
    
    // 设置输出流缓冲区大小
    public void setSendBufferSize(int size) throws SocketException {
        if(size<=0) {
            throw new IllegalArgumentException("Invalid send size");
        }
        setIntOption(StandardSocketOptions.SO_SNDBUF, size);
    }
    
    // 获取输入流缓冲区大小
    public int getReceiveBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_RCVBUF);
    }
    
    // 设置输入流缓冲区大小
    public void setReceiveBufferSize(int size) throws SocketException {
        if(size<=0) {
            throw new IllegalArgumentException("Invalid receive size");
        }
        setIntOption(StandardSocketOptions.SO_RCVBUF, size);
    }
    
    // 获取是否允许立刻重用已关闭的socket端口
    public boolean getReuseAddress() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_REUSEADDR);
        
    }
    
    // 设置是否允许立刻重用已关闭的socket端口
    public void setReuseAddress(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_REUSEADDR, on);
    }
    
    // 判断是否允许发送广播
    public boolean getBroadcast() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_BROADCAST);
    }
    
    // 设置是否允许发送广播
    public void setBroadcast(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_BROADCAST, on);
    }
    
    // 获取IP头部的Type-of-Service字段的值
    public int getTrafficClass() throws SocketException {
        return getIntOption(StandardSocketOptions.IP_TOS);
    }
    
    // 设置IP参数，即设置IP头部的Type-of-Service字段，用于描述IP包的优先级和QoS选项
    public void setTrafficClass(int tc) throws SocketException {
        setIntOption(StandardSocketOptions.IP_TOS, tc);
    }
    
    
    // 返回Boolean类型的配置参数值
    private boolean getBooleanOption(SocketOption<Boolean> name) throws SocketException {
        try {
            return datagramChannel.getOption(name);
        } catch(IOException x) {
            Net.translateToSocketException(x);
            return false;       // keep compiler happy
        }
    }
    
    // 设置Boolean类型的配置参数值
    private void setBooleanOption(SocketOption<Boolean> name, boolean value) throws SocketException {
        try {
            datagramChannel.setOption(name, value);
        } catch(IOException x) {
            Net.translateToSocketException(x);
        }
    }
    
    // 返回Integer类型的配置参数值
    private int getIntOption(SocketOption<Integer> name) throws SocketException {
        try {
            return datagramChannel.getOption(name);
        } catch(IOException x) {
            Net.translateToSocketException(x);
            return -1;          // keep compiler happy
        }
    }
    
    // 设置Integer类型的配置参数值
    private void setIntOption(SocketOption<Integer> name, int value) throws SocketException {
        try {
            datagramChannel.setOption(name, value);
        } catch(IOException x) {
            Net.translateToSocketException(x);
        }
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回被当前UDP-Socket适配器适配的UDP-Scoket通道
    public DatagramChannel getChannel() {
        return datagramChannel;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 对UDP-Socket执行【connect】操作
    private void connectInternal(SocketAddress remote) throws SocketException {
        // Socket地址类型转换
        InetSocketAddress isa = Net.asInetSocketAddress(remote);
        
        // 获取远程端口号，以判断是否合规
        int port = isa.getPort();
        if(port<0 || port>0xFFFF) {
            throw new IllegalArgumentException("connect: " + port);
        }
        
        if(remote == null) {
            throw new IllegalArgumentException("connect: null address");
        }
        
        try {
            // 将UDP-Socket通道连接到指定的远程地址
            datagramChannel.connect(remote);
        } catch(ClosedChannelException e) {
            // ignore
        } catch(Exception x) {
            Net.translateToSocketException(x);
        }
    }
    
    // 从远端接收数据存入bb，并返回接收到字节数
    private SocketAddress receive(ByteBuffer bb) throws IOException {
        assert Thread.holdsLock(datagramChannel.blockingLock()) && datagramChannel.isBlocking();
        
        long to = this.timeout;
        
        // 如果允许一直阻塞
        if(to == 0) {
            // 从远端接收数据存入bb，并返回接收到字节数
            return datagramChannel.receive(bb);
        }
        
        for(; ; ) {
            if(!datagramChannel.isOpen()) {
                throw new ClosedChannelException();
            }
            
            // 获取超时时间
            long st = System.currentTimeMillis();
            
            // 注册监听可读事件(Net.POLLIN)，当通道内有数据可读时，当前Socket会收到通知
            if(datagramChannel.pollRead(to)) {
                // 从远端接收数据存入bb，并返回接收到字节数
                return datagramChannel.receive(bb);
            }
            
            // 计算剩余允许阻塞的时间
            to -= System.currentTimeMillis() - st;
            
            // 如果已经超时了，则直接抛异常
            if(to<=0) {
                throw new SocketTimeoutException();
            }
        }
    }
    
}
