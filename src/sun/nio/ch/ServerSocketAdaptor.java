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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Make a server-socket channel look like a server socket.
 * The methods in this class are defined in exactly the same order as in
 * java.net.ServerSocket so as to simplify tracking future changes to that class.
 */
// ServerSocket适配器，用来将ServerSocketChannelImpl当做ServerSocket使唤
class ServerSocketAdaptor extends ServerSocket {
    
    // 将要被适配的ServerSocket通道；适配之后，该通道可以被当成普通的ServerSocket使用
    private final ServerSocketChannelImpl serverSocketChannel;
    
    /** Timeout "option" value for accepts */
    // 等待客户端的连接时设置的超时设置
    private volatile int timeout;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** super will create a useless impl */
    private ServerSocketAdaptor(ServerSocketChannelImpl serverSocketChannel) throws IOException {
        this.serverSocketChannel = serverSocketChannel;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回由指定的ServerSocket通道适配而成的ServerSocket
    public static ServerSocket create(ServerSocketChannelImpl ssc) {
        try {
            return new ServerSocketAdaptor(ssc);
        } catch(IOException x) {
            throw new Error(x);
        }
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 创建[服务端Socket(监听)]，并对其执行【bind】和【listen】操作，此处允许积压(排队)的待处理连接数为50
     *
     * endpoint: 既作为服务端的绑定地址(包含端口)，也作为开启监听的地址(包含端口)
     */
    public void bind(SocketAddress endpoint) throws IOException {
        bind(endpoint, 50);
    }
    
    /*
     * 创建[服务端Socket(监听)]，并对其执行【bind】和【listen】操作
     *
     * endpoint: 既作为服务端的绑定地址(包含端口)，也作为开启监听的地址(包含端口)
     * backlog : 允许积压(排队)的待处理连接数；如果backlog<1，则取默认值50
     */
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        if(endpoint == null) {
            endpoint = new InetSocketAddress(0);
        }
        
        try {
            serverSocketChannel.bind(endpoint, backlog);
        } catch(Exception x) {
            Net.translateException(x);
        }
    }
    
    /*
     * [服务端Socket(监听)]等待客户端的连接请求；
     * 连接成功后，返回与[客户端Socket]建立连接的[服务端Socket(通信)]
     */
    public Socket accept() throws IOException {
        synchronized(serverSocketChannel.blockingLock()) {
            try {
                if(!serverSocketChannel.isBound()) {
                    throw new NotYetBoundException();
                }
                
                long to = this.timeout;
                
                // 如果允许一直阻塞
                if(to == 0) {
                    /*
                     * [服务端Socket(监听)]等待客户端的连接请求；
                     * 连接成功后，返回与[客户端Socket]建立连接的[服务端Socket(通信)]。
                     *
                     * 注：此处返回的SocketChannel对象默认是阻塞式的，可以后续将其设置为非阻塞模式
                     */
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    
                    if(socketChannel == null && !serverSocketChannel.isBlocking()) {
                        throw new IllegalBlockingModeException();
                    }
                    
                    // 返回由[服务端Socket(通信)]通道适配而成的Socket
                    return socketChannel.socket();
                }
                
                // 如果是非阻塞Socket，则抛出异常
                if(!serverSocketChannel.isBlocking()) {
                    throw new IllegalBlockingModeException();
                }
                
                for(; ; ) {
                    // 获取超时时间
                    long st = System.currentTimeMillis();
                    
                    // 注册等待连接(Net.POLLIN)事件，客户端连接到服务端之后，当前Socket会收到通知
                    if(serverSocketChannel.pollAccept(to)) {
                        // 连接成功后，返回由[服务端Socket(通信)]通道适配而成的Socket
                        return serverSocketChannel.accept().socket();
                    }
                    
                    // 计算剩余允许阻塞的时间
                    to -= System.currentTimeMillis() - st;
                    
                    // 如果已经超时了，则直接抛异常
                    if(to<=0) {
                        throw new SocketTimeoutException();
                    }
                }
                
            } catch(Exception x) {
                Net.translateException(x);
                assert false;
                return null;    // Never happens
            }
        }
    }
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭socket连接
    public void close() throws IOException {
        serverSocketChannel.close();
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取当前适配器中的ServerSocket通道
    public ServerSocketChannel getChannel() {
        return serverSocketChannel;
    }
    
    // 返回本地IP，即服务端IP
    public InetAddress getInetAddress() {
        InetSocketAddress local = serverSocketChannel.localAddress();
        if(local == null) {
            return null;
        }
        
        // 对指定的地址进行安全校验
        InetSocketAddress socketAddress = Net.getRevealedLocalAddress(local);
        
        return socketAddress.getAddress();
    }
    
    // 返回本地端口，即服务端的端口
    public int getLocalPort() {
        InetSocketAddress local = serverSocketChannel.localAddress();
        if(local == null) {
            return -1;
        }
        
        return local.getPort();
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断ServerSocket是否已绑定
    public boolean isBound() {
        return serverSocketChannel.isBound();
    }
    
    // 判断ServerSocket是否已关闭
    public boolean isClosed() {
        return !serverSocketChannel.isOpen();
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
    
    // 获取是否允许立刻重用已关闭的socket端口
    public boolean getReuseAddress() throws SocketException {
        try {
            return serverSocketChannel.getOption(StandardSocketOptions.SO_REUSEADDR);
        } catch(IOException x) {
            Net.translateToSocketException(x);
            return false;       // Never happens
        }
    }
    
    // 设置是否允许立刻重用已关闭的socket端口
    public void setReuseAddress(boolean on) throws SocketException {
        try {
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, on);
        } catch(IOException x) {
            Net.translateToSocketException(x);
        }
    }
    
    // 获取输入流缓冲区大小
    public int getReceiveBufferSize() throws SocketException {
        try {
            return serverSocketChannel.getOption(StandardSocketOptions.SO_RCVBUF);
        } catch(IOException x) {
            Net.translateToSocketException(x);
            return -1;          // Never happens
        }
    }
    
    // 设置输入流缓冲区大小
    public void setReceiveBufferSize(int size) throws SocketException {
        // size 0 valid for ServerSocketChannel, invalid for ServerSocket
        if(size<=0) {
            throw new IllegalArgumentException("size cannot be 0 or negative");
        }
        
        try {
            serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, size);
        } catch(IOException x) {
            Net.translateToSocketException(x);
        }
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    public String toString() {
        if(!isBound()) {
            return "ServerSocket[unbound]";
        }
        
        return "ServerSocket[addr=" + getInetAddress() + ",localport=" + getLocalPort() + "]";
    }
    
}
