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

package sun.nio.ch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Make a socket channel look like a socket.
 * The methods in this class are defined in exactly the same order as in
 * java.net.Socket so as to simplify tracking future changes to that class.
 */
// Socket适配器，用来将SocketChannelImpl当做Socket使唤
class SocketAdaptor extends Socket {
    
    // 将要被适配的Socket通道；适配之后，该通道可以被当成普通的Socket使用
    private final SocketChannelImpl socketChannel;
    
    /** Timeout "option" value for reads */
    // 等待读取数据时的超时设置
    private volatile int timeout;
    
    // 指向被适配通道的输入流，单例
    private InputStream socketInputStream = null;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 构造Socket适配器，用来将指定的Socket通道"伪装"为Socket。
     *
     * 在这里，不再需要传统的"Scoket委托"，而是需要参数中的Socket通道来做委托。
     * 由于这里的构造器中需要显式调用父类的构造器，但又不能使用传统的"Scoket委托"，
     * 因此给父构造器赋了一个null值来欺骗父构造器。
     */
    private SocketAdaptor(SocketChannelImpl socketChannel) throws SocketException {
        super((SocketImpl) null);
        this.socketChannel = socketChannel;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回由指定的Socket通道适配而成的Socket
    public static Socket create(SocketChannelImpl socketChannel) {
        try {
            return new SocketAdaptor(socketChannel);
        } catch(SocketException e) {
            throw new InternalError("Should not reach here");
        }
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 对[客户端Socket]执行【bind】操作。
     *
     * local: 待绑定的地址(ip+port)
     */
    public void bind(SocketAddress local) throws IOException {
        try {
            socketChannel.bind(local);
        } catch(Exception x) {
            Net.translateException(x);
        }
    }
    
    /** Override this method just to protect against changes in the superclass */
    /*
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；如果远端还未就绪，则立即返回。
     *
     * remote: 等待连接的远端地址
     */
    public void connect(SocketAddress remote) throws IOException {
        connect(remote, 0);
    }
    
    /*
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；允许指定超时，以便等待远端就绪。
     *
     * remote : 等待连接的远端地址
     * timeout: 超时时间，即允许连接等待的时间
     */
    public void connect(SocketAddress remote, int timeout) throws IOException {
        if(remote == null) {
            throw new IllegalArgumentException("connect: The address can't be null");
        }
        
        if(timeout<0) {
            throw new IllegalArgumentException("connect: timeout can't be negative");
        }
        
        synchronized(socketChannel.blockingLock()) {
            
            // 如果是非阻塞Socket，则抛出异常
            if(!socketChannel.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
            
            try {
                // time=0表示成功连上之前，或者抛出IO异常之前，不会返回
                if(timeout == 0) {
                    socketChannel.connect(remote);
                    return;
                }
                
                // 如果time不为0，表示需要阻塞一段时间，这里临时将socket更改为非阻塞Socket
                socketChannel.configureBlocking(false);
                
                try {
                    // 非阻塞式Socket的连接，如果连接成功，直接返回，连接失败的话进行后续操作
                    if(socketChannel.connect(remote)) {
                        return;
                    }
                } finally {
                    try {
                        // 恢复为阻塞式Socket
                        socketChannel.configureBlocking(true);
                    } catch(ClosedChannelException e) {
                    }
                }
                
                // 将毫秒时间转换为纳秒
                long timeoutNanos = NANOSECONDS.convert(timeout, MILLISECONDS);
                long to = timeout;
                
                // 进入死循环，直到连接成功，或者抛出了IO异常才退出循环
                for(; ; ) {
                    long startTime = System.nanoTime();
                    
                    // 注册监听连接事件(Net.POLLCONN)，连接成功后，当前Socket会收到通知
                    if(socketChannel.pollConnected(to)) {
                        // 检测是否连接成功，由于这里已恢复为阻塞式Socket，所以内部会轮询检查连接，当有了返回结果时，就说明连接成功了
                        boolean connected = socketChannel.finishConnect();
                        assert connected;
                        break;
                    }
                    
                    // 判断连接是否超时
                    timeoutNanos -= System.nanoTime() - startTime;
                    if(timeoutNanos<=0) {
                        try {
                            socketChannel.close();
                        } catch(IOException x) {
                        }
                        throw new SocketTimeoutException();
                    }
                    
                    to = MILLISECONDS.convert(timeoutNanos, NANOSECONDS);
                }
                
            } catch(Exception x) {
                Net.translateException(x, true);
            }
        }
        
    }
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取Socket输入流，从中读取数据
    public InputStream getInputStream() throws IOException {
        if(!socketChannel.isOpen()) {
            throw new SocketException("Socket is closed");
        }
        
        if(!socketChannel.isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        
        if(!socketChannel.isInputOpen()) {
            throw new SocketException("Socket input is shutdown");
        }
        
        // 如果已经实例化过，则直接返回
        if(socketInputStream != null) {
            return socketInputStream;
        }
        
        try {
            socketInputStream = AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                public InputStream run() throws IOException {
                    return new SocketInputStream();
                }
            });
        } catch(PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
        
        return socketInputStream;
    }
    
    // 获取Socket输出流，向其写入数据
    public OutputStream getOutputStream() throws IOException {
        if(!socketChannel.isOpen()) {
            throw new SocketException("Socket is closed");
        }
        
        if(!socketChannel.isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        
        if(!socketChannel.isOutputOpen()) {
            throw new SocketException("Socket output is shutdown");
        }
        
        OutputStream os = null;
        
        try {
            os = AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
                public OutputStream run() throws IOException {
                    return Channels.newOutputStream(socketChannel);
                }
            });
        } catch(java.security.PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
        
        return os;
    }
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭socket连接
    public void close() throws IOException {
        socketChannel.close();
    }
    
    // 关闭读取功能
    public void shutdownInput() throws IOException {
        try {
            socketChannel.shutdownInput();
        } catch(Exception x) {
            Net.translateException(x);
        }
    }
    
    // 关闭写入功能
    public void shutdownOutput() throws IOException {
        try {
            socketChannel.shutdownOutput();
        } catch(Exception x) {
            Net.translateException(x);
        }
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取被当前Socket适配器适配的Socket通道
    public SocketChannel getChannel() {
        return socketChannel;
    }
    
    
    // 返回本地IP，如果没有绑定地址，则返回通配地址
    public InetAddress getLocalAddress() {
        if(socketChannel.isOpen()) {
            InetSocketAddress local = socketChannel.localAddress();
            if(local != null) {
                // 对指定的地址进行安全校验
                return Net.getRevealedLocalAddress(local).getAddress();
            }
        }
        
        return new InetSocketAddress(0).getAddress();
    }
    
    // 返回本地端口
    public int getLocalPort() {
        InetSocketAddress local = socketChannel.localAddress();
        if(local == null) {
            return -1;
        }
        
        return local.getPort();
    }
    
    
    // 获取远程IP，如果没有连接就返回null
    public InetAddress getInetAddress() {
        InetSocketAddress remote = socketChannel.remoteAddress();
        if(remote == null) {
            return null;
        }
        
        return remote.getAddress();
    }
    
    // 返回远程端口
    public int getPort() {
        InetSocketAddress remote = socketChannel.remoteAddress();
        if(remote == null) {
            return 0;
        }
        
        return remote.getPort();
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断当前Socket是否已建立连接
    public boolean isConnected() {
        return socketChannel.isConnected();
    }
    
    // 判断Socket是否已绑定本地地址
    public boolean isBound() {
        return socketChannel.localAddress() != null;
    }
    
    // 判断Socket是否已关闭
    public boolean isClosed() {
        return !socketChannel.isOpen();
    }
    
    // 判断Socket输入流是否已关闭(关闭了从当前Socket读取的功能)
    public boolean isInputShutdown() {
        return !socketChannel.isInputOpen();
    }
    
    // 判断Socket输出流是否已关闭(关闭了向当前Socket写入的功能)
    public boolean isOutputShutdown() {
        return !socketChannel.isOutputOpen();
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 设置指定名称的Socket配置参数(Boolean类型的参数)
    private void setBooleanOption(SocketOption<Boolean> name, boolean value) throws SocketException {
        try {
            socketChannel.setOption(name, value);
        } catch(IOException x) {
            Net.translateToSocketException(x);
        }
    }
    
    // 设置指定名称的Socket配置参数(Integer类型的参数)
    private void setIntOption(SocketOption<Integer> name, int value) throws SocketException {
        try {
            socketChannel.setOption(name, value);
        } catch(IOException x) {
            Net.translateToSocketException(x);
        }
    }
    
    // 获取指定名称的Socket配置参数(Boolean类型的参数)
    private boolean getBooleanOption(SocketOption<Boolean> name) throws SocketException {
        try {
            return socketChannel.getOption(name);
        } catch(IOException x) {
            Net.translateToSocketException(x);
            return false;       // keep compiler happy
        }
    }
    
    // 获取指定名称的Socket配置参数(Integer类型的参数)
    private int getIntOption(SocketOption<Integer> name) throws SocketException {
        try {
            return socketChannel.getOption(name);
        } catch(IOException x) {
            Net.translateToSocketException(x);
            return -1;          // keep compiler happy
        }
    }
    
    
    // 获取超时约束的时间
    public int getSoTimeout() throws SocketException {
        return timeout;
    }
    
    // 设置超时约束的时间
    public void setSoTimeout(int timeout) throws SocketException {
        if(timeout<0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        this.timeout = timeout;
    }
    
    // 获取输出流缓冲区大小
    public int getSendBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_SNDBUF);
    }
    
    // 设置输出流缓冲区大小
    public void setSendBufferSize(int size) throws SocketException {
        // size 0 valid for SocketChannel, invalid for Socket
        if(size<=0) {
            throw new IllegalArgumentException("Invalid send size");
        }
        setIntOption(StandardSocketOptions.SO_SNDBUF, size);
    }
    
    // 获取设置输入流缓冲区大小
    public int getReceiveBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_RCVBUF);
    }
    
    // 设置设置输入流缓冲区大小
    public void setReceiveBufferSize(int size) throws SocketException {
        // size 0 valid for SocketChannel, invalid for Socket
        if(size<=0) {
            throw new IllegalArgumentException("Invalid receive size");
        }
        setIntOption(StandardSocketOptions.SO_RCVBUF, size);
    }
    
    // 获取是否禁用Nagle算法
    public boolean getTcpNoDelay() throws SocketException {
        return getBooleanOption(StandardSocketOptions.TCP_NODELAY);
    }
    
    // 设置是否禁用Nagle算法
    public void setTcpNoDelay(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.TCP_NODELAY, on);
    }
    
    // 获取是否启用延时关闭
    public int getSoLinger() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_LINGER);
    }
    
    // 设置是否启用延时关闭
    public void setSoLinger(boolean on, int linger) throws SocketException {
        if(!on) {
            linger = -1;
        }
        setIntOption(StandardSocketOptions.SO_LINGER, linger);
    }
    
    // 获取是否允许发送"紧急数据"
    public boolean getOOBInline() throws SocketException {
        return getBooleanOption(ExtendedSocketOption.SO_OOBINLINE);
    }
    
    // 设置是否允许发送"紧急数据"
    public void setOOBInline(boolean on) throws SocketException {
        setBooleanOption(ExtendedSocketOption.SO_OOBINLINE, on);
    }
    
    // 获取是否开启设置心跳机制
    public boolean getKeepAlive() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_KEEPALIVE);
    }
    
    // 设置是否开启设置心跳机制
    public void setKeepAlive(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_KEEPALIVE, on);
    }
    
    // 获取是否允许立刻重用已关闭的socket端口
    public boolean getReuseAddress() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_REUSEADDR);
    }
    
    // 设置是否允许立刻重用已关闭的socket端口
    public void setReuseAddress(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_REUSEADDR, on);
    }
    
    // 获取IP头部的Type-of-Service字段的值
    public int getTrafficClass() throws SocketException {
        return getIntOption(StandardSocketOptions.IP_TOS);
    }
    
    // 设置IP参数，即设置IP头部的Type-of-Service字段，用于描述IP包的优先级和QoS选项
    public void setTrafficClass(int tc) throws SocketException {
        setIntOption(StandardSocketOptions.IP_TOS, tc);
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 发送一个字节的"紧急数据"，参见SocketOptions#SO_OOBINLINE参数
    public void sendUrgentData(int data) throws IOException {
        int n = socketChannel.sendOutOfBandData((byte) data);
        if(n == 0) {
            throw new IOException("Socket buffer full");
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    public String toString() {
        if(socketChannel.isConnected()) {
            return "Socket[addr=" + getInetAddress() + ",port=" + getPort() + ",localport=" + getLocalPort() + "]";
        }
        return "Socket[unconnected]";
    }
    
    
    // Socket输入流(要求适配内的通道是阻塞式的)
    private class SocketInputStream extends ChannelInputStream {
        
        private SocketInputStream() {
            super(socketChannel);
        }
        
        // 从内部通道中读取，读到的内容写入dst
        protected int read(ByteBuffer dst) throws IOException {
            synchronized(socketChannel.blockingLock()) {
                // 如果将要被适配的Socket通道是非阻塞的，则抛出异常
                if(!socketChannel.isBlocking()) {
                    throw new IllegalBlockingModeException();
                }
                
                long to = SocketAdaptor.this.timeout;
                
                // 如果允许一直阻塞
                if(to == 0) {
                    return socketChannel.read(dst);
                }
                
                // 获取超时时间
                long timeoutNanos = NANOSECONDS.convert(to, MILLISECONDS);
                
                // 轮询读取，直到成功读取到数据
                for(; ; ) {
                    // 获取当前时间
                    long startTime = System.nanoTime();
                    
                    // 注册监听可读事件(Net.POLLIN)，当通道内有数据可读时，当前Socket会收到通知
                    if(socketChannel.pollRead(to)) {
                        // 如果已经有数据达到，则立即读取
                        return socketChannel.read(dst);
                    }
                    
                    // 计算剩余允许阻塞的时间
                    timeoutNanos -= System.nanoTime() - startTime;
                    
                    // 如果已经超时了，则直接抛异常
                    if(timeoutNanos<=0) {
                        throw new SocketTimeoutException();
                    }
                    
                    // 纳秒转换为毫秒
                    to = MILLISECONDS.convert(timeoutNanos, NANOSECONDS);
                }
            }
        }
    }
    
}
