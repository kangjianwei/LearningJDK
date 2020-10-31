/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import sun.net.NetHooks;
import sun.net.ext.ExtendedSocketOptions;

import static sun.net.ext.ExtendedSocketOptions.SOCK_STREAM;

/**
 * Base implementation of AsynchronousServerSocketChannel.
 */
// 异步ServerSocket通道的抽象实现
abstract class AsynchronousServerSocketChannelImpl extends AsynchronousServerSocketChannel implements Cancellable, Groupable {
    
    protected final FileDescriptor fd;  // [服务端Socket(监听)]在Java层的文件描述符
    
    /** the local address to which the channel's socket is bound */
    /*
     * 本地地址
     *
     * [服务端Socket(监听)]: 即服务端地址
     */
    protected volatile InetSocketAddress localAddress;
    
    /** set true when accept operation is cancelled */
    // accept操作是否被中止
    private volatile boolean acceptKilled;
    
    // 指示当前通道是否已关闭
    private volatile boolean closed;
    
    /** set true when exclusive binding is on and SO_REUSEADDR is emulated */
    // 是否允许立刻重用已关闭的socket端口
    private boolean isReuseAddress;
    
    /** close support */
    // 读/写锁，多个线程可以同时读，但不能同时写
    private ReadWriteLock closeLock = new ReentrantReadWriteLock();
    
    /** need this lock to set local address */
    // 状态锁
    private final Object stateLock = new Object();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    AsynchronousServerSocketChannelImpl(AsynchronousChannelGroupImpl group) {
        super(group.provider());
        // 创建[服务端Socket(监听)]，并返回其在Java层的文件描述符
        this.fd = Net.serverSocket(true);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 对[服务端Socket(监听)]执行【bind】和【listen】操作
     *
     * local  : 既作为服务端的绑定地址(包含端口)，也作为开启监听的地址(包含端口)
     * backlog: 允许积压(排队)的待处理连接数；如果backlog<1，则取默认值50
     */
    @Override
    public final AsynchronousServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        // 获取待绑定的地址
        InetSocketAddress isa = (local == null) ? new InetSocketAddress(0) : Net.checkAddress(local);
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkListen(isa.getPort());
        }
        
        try {
            // 添加一个读锁
            begin();
            
            synchronized(stateLock) {
                // 如果已经绑定则抛异常
                if(localAddress != null) {
                    throw new AlreadyBoundException();
                }
                
                // Socket进行bind操作之前的回调
                NetHooks.beforeTcpBind(fd, isa.getAddress(), isa.getPort());
                
                // 为[服务端Socket(监听)]绑定IP地址与端口号
                Net.bind(fd, isa.getAddress(), isa.getPort());
                
                // 使[服务端Socket(监听)]开启监听，backlog代表允许积压的待处理连接数
                Net.listen(fd, backlog<1 ? 50 : backlog);
                
                // 记录绑定的本地地址
                localAddress = Net.localAddress(fd);
            }
        } finally {
            // 移除一个读锁
            end();
        }
        
        return this;
    }
    
    /*
     * [服务端Socket(监听)]等待客户端的连接请求；
     * 返回值是一个包含[服务端Socket(通信)]的Future，主线程轮询此Future以判断是否accept完成，
     * 以及获取到与[客户端Socket]建立连接的[服务端Socket(通信)]。
     */
    @Override
    public final Future<AsynchronousSocketChannel> accept() {
        // 实现异步IO中的"accept"操作
        return implAccept(null, null);
    }
    
    /*
     * [服务端Socket(监听)]等待客户端的连接请求；
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，
     * 以便主线程获取到与[客户端Socket]建立连接的[服务端Socket(通信)]。
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <A> void accept(A attachment, CompletionHandler<AsynchronousSocketChannel, ? super A> handler) {
        if(handler == null) {
            throw new NullPointerException("'handler' is null");
        }
        
        // 实现异步IO中的"accept"操作
        implAccept(attachment, (CompletionHandler<AsynchronousSocketChannel, Object>) handler);
    }
    
    /**
     * Invoked by accept to accept connection
     */
    // 实现异步IO中的"accept"操作
    abstract Future<AsynchronousSocketChannel> implAccept(Object attachment, CompletionHandler<AsynchronousSocketChannel, Object> handler);
    
    /*▲ socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取消/关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 异步IO操作(这里是accept)被关闭时需要执行的回调
    @Override
    public final void onCancel(PendingFuture<?, ?> task) {
        // 标记accept操作已被中止
        acceptKilled = true;
    }
    
    // 关闭异步通道
    @Override
    public final void close() throws IOException {
        /* synchronize with any threads using file descriptor/handle */
        closeLock.writeLock().lock();
        
        try {
            if(closed) {
                return;     // already closed
            }
            closed = true;
        } finally {
            closeLock.writeLock().unlock();
        }
        
        // 实现对异步IO通道的关闭操作
        implClose();
    }
    
    /**
     * Invoked to close file descriptor/handle.
     */
    // 实现对异步IO通道的关闭操作
    abstract void implClose() throws IOException;
    
    /*▲ 取消/关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取绑定的本地地址
    @Override
    public final SocketAddress getLocalAddress() throws IOException {
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
        
        // 对指定的地址进行安全校验
        return Net.getRevealedLocalAddress(localAddress);
    }
    
    /*▲ 地址信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断当前通道是否处于开启(运行)状态
    @Override
    public final boolean isOpen() {
        return !closed;
    }
    
    // 判断accept操作是否已中止
    final boolean isAcceptKilled() {
        return acceptKilled;
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取当前通道支持的Socket配置参数集合
    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }
    
    // 获取指定名称的Socket配置参数
    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getOption(SocketOption<T> name) throws IOException {
        if(name == null) {
            throw new NullPointerException();
        }
        
        if(!supportedOptions().contains(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        
        try {
            // 添加一个读锁
            begin();
            
            if(name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                // SO_REUSEADDR emulated when using exclusive bind
                return (T) Boolean.valueOf(isReuseAddress);
            }
            
            return (T) Net.getSocketOption(fd, Net.UNSPEC, name);
        } finally {
            // 移除一个读锁
            end();
        }
    }
    
    // 设置指定名称的Socket配置参数
    @Override
    public final <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        if(name == null) {
            throw new NullPointerException();
        }
        
        if(!supportedOptions().contains(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        
        try {
            // 添加一个读锁
            begin();
            
            if(name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                // SO_REUSEADDR emulated when using exclusive bind
                isReuseAddress = (Boolean) value;
            } else {
                Net.setSocketOption(fd, Net.UNSPEC, name, value);
            }
            
            return this;
        } finally {
            // 移除一个读锁
            end();
        }
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 加锁/解锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Marks beginning of access to file descriptor/handle
     */
    // 添加一个读锁
    final void begin() throws IOException {
        closeLock.readLock().lock();
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
    }
    
    /**
     * Marks end of access to file descriptor/handle
     */
    // 移除一个读锁
    final void end() {
        closeLock.readLock().unlock();
    }
    
    /*▲ 加锁/解锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());
        sb.append('[');
        if(!isOpen()) {
            sb.append("closed");
        } else {
            if(localAddress == null) {
                sb.append("unbound");
            } else {
                sb.append(Net.getRevealedLocalAddressAsString(localAddress));
            }
        }
        sb.append(']');
        return sb.toString();
    }
    
    
    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();
        
        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>(2);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            if(Net.isReusePortAvailable()) {
                set.add(StandardSocketOptions.SO_REUSEPORT);
            }
            set.addAll(ExtendedSocketOptions.options(SOCK_STREAM));
            return Collections.unmodifiableSet(set);
        }
    }
    
}
