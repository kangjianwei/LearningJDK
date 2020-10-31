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

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import sun.net.NetHooks;
import sun.net.ext.ExtendedSocketOptions;

import static sun.net.ext.ExtendedSocketOptions.SOCK_STREAM;

/**
 * An implementation of ServerSocketChannels
 */
// ServerSocket通道的本地实现
class ServerSocketChannelImpl extends ServerSocketChannel implements SelChImpl {
    
    /** Channel state, increases monotonically */
    // ServerSocket状态参数
    private static final int ST_INUSE = 0; // 使用中
    private static final int ST_CLOSING = 1; // 正在断开连接(关闭)
    private static final int ST_KILLPENDING = 2; // 正在销毁
    private static final int ST_KILLED = 3; // 已销毁
    
    private int state;  // ServerSocketChannel状态
    
    
    /** Our socket adaptor, if any */
    // 由当前ServerSocket通道适配而成的ServerSocket
    private ServerSocket serverSocket;
    
    private final FileDescriptor fd;    // [服务端Socket(监听)]在Java层的文件描述符
    private final int fdVal;            // [服务端Socket(监听)]在本地(native层)的文件描述符
    
    /** Binding（null => unbound） */
    /*
     * 本地地址
     *
     * [服务端Socket(监听)]: 即服务端地址
     */
    private InetSocketAddress localAddress;
    
    /** Used to make native close and configure calls */
    private static NativeDispatcher nd; // 完成Socket读写的工具类
    
    /** ID of native thread currently blocked in this channel, for signalling */
    private long thread;    // 当前通道开始accept操作时所在的native线程号
    
    /** set true when exclusive binding is on and SO_REUSEADDR is emulated */
    private boolean isReuseAddress; // 是否允许立刻重用已关闭的socket端口
    
    /** Lock held by thread currently blocked on this channel */
    private final ReentrantLock acceptLock = new ReentrantLock();   // 等待连接使用的锁
    /**
     * Lock held by any thread that modifies the state fields declared below
     * DO NOT invoke a blocking I/O operation while holding this lock!
     */
    private final Object stateLock = new Object();  // 状态锁
    
    
    static {
        IOUtil.load();
        initIDs();
        nd = new SocketDispatcher();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 构造一个未绑定的ServerSocket，本质是创建了[服务端Socket(监听)]，内部初始化了该Socket的文件描述符
     */
    ServerSocketChannelImpl(SelectorProvider provider) throws IOException {
        super(provider);
        
        // 创建[服务端Socket(监听)]，并返回其在Java层的文件描述符
        this.fd = Net.serverSocket(true);
        
        // 获取[服务端Socket(监听)]在本地(native层)的文件描述符
        this.fdVal = IOUtil.fdVal(fd);
    }
    
    /*
     * 构造一个ServerSocket，其涉及的文件描述符由参数中的fd给出
     */
    ServerSocketChannelImpl(SelectorProvider provider, FileDescriptor fd, boolean bound) throws IOException {
        super(provider);
        
        // 记录当前Socket在Java层的文件描述符
        this.fd = fd;
        
        // 获取当前Socket在本地(native层)的文件描述符
        this.fdVal = IOUtil.fdVal(fd);
        
        // 如果无需绑定，直接返回
        if(!bound) {
            return;
        }
        
        synchronized(stateLock) {
            localAddress = Net.localAddress(fd);
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 适配 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回由当前ServerSocket通道适配而成的ServerSocket
    @Override
    public ServerSocket socket() {
        synchronized(stateLock) {
            if(serverSocket == null) {
                serverSocket = ServerSocketAdaptor.create(this);
            }
            
            return serverSocket;
        }
    }
    
    /*▲ 适配 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 对[服务端Socket(监听)]执行【bind】和【listen】操作
     *
     * endpoint: 既作为服务端的绑定地址(包含端口)，也作为开启监听的地址(包含端口)
     * backlog : 允许积压(排队)的待处理连接数；如果backlog<1，则取默认值50
     */
    @Override
    public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            // 不允许重复绑定
            if(localAddress != null) {
                throw new AlreadyBoundException();
            }
            
            // 确定绑定到哪个地址
            InetSocketAddress isa = (local == null) ? new InetSocketAddress(0)  // 如果未指定绑定地址，则使用通配IP和随机端口
                : Net.checkAddress(local);
            
            // 如果存在安全管理器，则检查端口号
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                sm.checkListen(isa.getPort());
            }
            
            // ServerSocket进行bind操作之前的回调
            NetHooks.beforeTcpBind(fd, isa.getAddress(), isa.getPort());
            
            // 为[服务端Socket(监听)]绑定IP地址与端口号
            Net.bind(fd, isa.getAddress(), isa.getPort());
            
            // 使[服务端Socket(监听)]开启监听，backlog代表允许积压的待处理连接数
            Net.listen(fd, backlog<1 ? 50 : backlog);
            
            // 记录绑定的本地地址
            localAddress = Net.localAddress(fd);
        }
        
        return this;
    }
    
    /*
     * [服务端Socket(监听)]等待客户端的连接请求；
     * 连接成功后，返回与[客户端Socket]建立连接的[服务端Socket(通信)]。
     *
     * 注：此处返回的SocketChannel对象默认是阻塞式的，可以后续将其设置为非阻塞模式
     */
    @Override
    public SocketChannel accept() throws IOException {
        acceptLock.lock();
        
        try {
            int n = 0;
            
            /*
             * [服务端Socket(监听)]与客户端成功建立连接后，
             * 会将建立连接的[服务端Socket(通信)]的文件描述符存储到newfd中
             */
            FileDescriptor newfd = new FileDescriptor();
            
            // 远程Socket地址，即[客户端Socket]的地址
            InetSocketAddress[] remote = new InetSocketAddress[1];
            
            // 当前通道是否为阻塞模式
            boolean blocking = isBlocking();
            
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
                begin(blocking);
                
                do {
                    // [服务端Socket(监听)]等待客户端的连接请求；连接成功后返回1
                    n = accept(this.fd, newfd, remote);
                    
                    // 如果遇到了线程中断，且通道未关闭，则重试
                } while(n == IOStatus.INTERRUPTED && isOpen());
            } finally {
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                end(blocking, n>0);
                assert IOStatus.check(n);
            }
            
            // 如果连接失败的话返回null
            if(n<1) {
                return null;
            }
            
            /* newly accepted socket is initially in blocking mode */
            // 阻塞[服务端Socket(通信)]的文件描述符newfd关联的通道
            IOUtil.configureBlocking(newfd, true);
            
            // 构造一个[服务端Socket(通信)]对象，并在结尾返回它
            SocketChannel socketChannel = new SocketChannelImpl(provider(), newfd, remote[0]);
            
            // check permitted to accept connections from the remote address
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                try {
                    sm.checkAccept(remote[0].getAddress().getHostAddress(), remote[0].getPort());
                } catch(SecurityException x) {
                    socketChannel.close();
                    throw x;
                }
            }
            
            return socketChannel;
        } finally {
            acceptLock.unlock();
        }
    }
    
    /**
     * Accept a connection on a socket.
     *
     * @implNote Wrap native call to allow instrumentation.
     */
    /*
     * [服务端Socket(监听)]等待客户端的连接请求；连接成功后返回1
     *
     * fd   : [服务端Socket(监听)]的文件描述符
     * newfd: 与[客户端Socket]成功建立连接的[服务端Socket(通信)]的文件描述符
     * isaa : 远程连接地址，即客户端的地址
     */
    private int accept(FileDescriptor fd, FileDescriptor newfd, InetSocketAddress[] isaa) throws IOException {
        return accept0(fd, newfd, isaa);
    }
    
    /*
     * Accepts a new connection, setting the given file descriptor to refer to the new socket
     * and setting isaa[0] to the socket's remote address.
     * Returns 1 on success,
     * or IOStatus.UNAVAILABLE (if non-blocking and no connections are pending)
     * or IOStatus.INTERRUPTED.
     */
    // [服务端Socket(监听)]等待与客户端建立连接的本地实现
    private native int accept0(FileDescriptor ssfd, FileDescriptor newfd, InetSocketAddress[] isaa) throws IOException;
    
    
    /**
     * Poll this channel's socket for a new connection up to the given timeout.
     *
     * @return {@code true} if there is a connection to accept
     */
    /*
     * 注册等待连接(Net.POLLIN)事件，客户端连接到服务端之后，当前Socket会收到通知。
     * timeout是允许可阻塞socket等待建立连接的时间。
     * 返回值指示是否在阻塞时间内收到了"客户端已连接到服务端"的信号。
     */
    boolean pollAccept(long timeout) throws IOException {
        assert Thread.holdsLock(blockingLock()) && isBlocking();
        
        acceptLock.lock();
        try {
            boolean polled = false;
            
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
                begin(true);
                
                // 注册Net.POLLIN事件，远端发来"有可读数据"的消息时，会通知当前Socket
                int events = Net.poll(fd, Net.POLLIN, timeout);
                
                polled = (events != 0);
            } finally {
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                end(true, polled);
            }
            
            return polled;
        } finally {
            acceptLock.unlock();
        }
    }
    
    /*▲ socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Invoked by implCloseChannel to close the channel.
     *
     * This method waits for outstanding I/O operations to complete. When in
     * blocking mode, the socket is pre-closed and the threads in blocking I/O
     * operations are signalled to ensure that the outstanding I/O operations
     * complete quickly.
     *
     * The socket is closed by this method when it is not registered with a
     * Selector. Note that a channel configured blocking may be registered with
     * a Selector. This arises when a key is canceled and the channel configured
     * to blocking mode before the key is flushed from the Selector.
     */
    // 实现对"可选择"通道的关闭操作
    @Override
    protected void implCloseSelectableChannel() throws IOException {
        assert !isOpen();
    
        boolean interrupted = false;
        boolean blocking;
    
        // 进入正在正在断开连接状态
        synchronized(stateLock) {
            assert state<ST_CLOSING;
            state = ST_CLOSING;
            blocking = isBlocking();
        }
    
        // wait for any outstanding accept to complete
        if(blocking) {
            synchronized(stateLock) {
                assert state == ST_CLOSING;
            
                long th = thread;
                if(th != 0) {
                    nd.preClose(fd);
                
                    // 唤醒阻塞的th线程
                    NativeThread.signal(th);
                
                    // 等待accept操作的结束，然后唤醒此处阻塞的线程
                    while(thread != 0) {
                        try {
                            stateLock.wait();
                        } catch(InterruptedException e) {
                            // 发生了中断异常
                            interrupted = true;
                        }
                    }
                }
            }
        } else {
            // non-blocking mode: wait for accept to complete
            acceptLock.lock();
            acceptLock.unlock();
        }
    
        // 进入正在销毁状态
        synchronized(stateLock) {
            assert state == ST_CLOSING;
            state = ST_KILLPENDING;
        }
    
        /* close socket if not registered with Selector */
        // 如果通道已经没有在任何选择器上注册，则销毁它
        if(!isRegistered()) {
            kill();
        }
    
        // 恢复中断状态
        if(interrupted) {
            Thread.currentThread().interrupt();
        }
    }
    
    // 销毁通道，即释放对Socket文件描述符的引用
    @Override
    public void kill() throws IOException {
        synchronized(stateLock) {
            if(state == ST_KILLPENDING) {
                // 通道进入已销毁状态
                state = ST_KILLED;
                // 释放对文件描述符的引用
                nd.close(fd);
            }
        }
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 监听参数/事件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Translates an interest operation set into a native poll event set
     */
    /*
     * 翻译ServerSocket通道注册的监听事件，返回对ops的翻译结果
     *
     * 方向：Java层 --> native层
     * 　　　SelectionKey.XXX --> Net.XXX
     */
    public int translateInterestOps(int ops) {
        int newOps = 0;
        
        if((ops & SelectionKey.OP_ACCEPT) != 0) {
            newOps |= Net.POLLIN;
        }
        
        return newOps;
    }
    
    /*▲ 监听参数/事件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 就绪参数/事件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     *【增量更新】已就绪事件
     *
     * 将本地(native)反馈的就绪信号ops翻译并存储到Java层的就绪事件readyOps中，
     * 返回值指示上一次反馈的事件与本次反馈的事件是否发生了改变。
     *
     * 通道收到有效的反馈事件后，会【增量更新】上次记录的已就绪事件，
     * 如果本地(native)反馈了错误或挂起信号，则将已就绪事件直接设置为通道注册的监听事件。
     *
     * 方向：native层 --> Java层
     * 　　　Net.XXX --> SelectionKey.XXX
     *
     * 参见：SelectionKeyImpl#readyOps
     */
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl selectionKey) {
        return translateReadyOps(ops, selectionKey.nioReadyOps(), selectionKey);
    }
    
    /*
     *【覆盖更新】已就绪事件
     *
     * 将本地(native)反馈的就绪信号ops翻译并存储到Java层的就绪事件readyOps中，
     * 返回值指示上一次反馈的事件与本次反馈的事件是否发生了改变。
     *
     * 通道收到有效的反馈事件后，会【覆盖】上次记录的已就绪事件，
     * 如果本地(native)反馈了错误或挂起信号，则将已就绪事件直接设置为通道注册的监听事件。
     *
     * 方向：native层 --> Java层
     * 　　　Net.XXX --> SelectionKey.XXX
     *
     * 参见：SelectionKeyImpl#readyOps
     */
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl selectionKey) {
        return translateReadyOps(ops, 0, selectionKey);
    }
    
    /**
     * Translates native poll revent set into a ready operation set
     */
    /*
     *【增量更新】已就绪事件(基于initialOps叠加)
     *
     * 将本地(native)反馈的就绪信号ops翻译并存储到Java层的就绪事件readyOps中，
     * 返回值指示上一次反馈的事件与本次反馈的事件是否发生了改变。
     *
     * 通道收到有效的反馈事件后，会将其【叠加】在initialOps上，换句话说是对已就绪事件的增量更新。
     * 如果本地(native)反馈了错误或挂起信号，则将已就绪事件直接设置为通道注册的监听事件。
     *
     * 方向：native层 --> Java层
     * 　　　Net.XXX --> SelectionKey.XXX
     *
     * 参见：SelectionKeyImpl#readyOps
     */
    public boolean translateReadyOps(int ops, int initialOps, SelectionKeyImpl selectionKey) {
        // 不合规的文件描述符
        if((ops & Net.POLLNVAL) != 0) {
            /*
             * This should only happen if this channel is pre-closed while a selection operation is in progress
             * ## Throw an error if this channel has not been pre-closed
             */
            return false;
        }
        
        // 获取通道注册的监听事件：SelectionKey.XXX(不会验证当前"选择键"是否有效)
        int intOps = selectionKey.nioInterestOps();
        
        // 获取已就绪事件
        int oldOps = selectionKey.nioReadyOps();
        int newOps = initialOps;
        
        // 本地(native)反馈了错误或挂起的信号
        if((ops & (Net.POLLERR | Net.POLLHUP)) != 0) {
            newOps = intOps;
            // 设置已就绪通道上收到的响应事件为通道注册的监听事件
            selectionKey.nioReadyOps(newOps);
            return (newOps & ~oldOps) != 0;
        }
        
        // 该通道监听了"等待连接"事件，且通道已获取到来自客户端的连接
        if(((ops & Net.POLLIN) != 0) && ((intOps & SelectionKey.OP_ACCEPT) != 0)) {
            newOps |= SelectionKey.OP_ACCEPT;
        }
        
        // 将newOps设置为已就绪事件
        selectionKey.nioReadyOps(newOps);
        
        return (newOps & ~oldOps) != 0;
    }
    
    /*▲ 就绪参数/事件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 设置指定名称的Socket配置参数
    @Override
    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        Objects.requireNonNull(name);
        
        if(!supportedOptions().contains(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            if(name == StandardSocketOptions.IP_TOS) {
                ProtocolFamily family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
                Net.setSocketOption(fd, family, name, value);
                return this;
            }
            
            if(name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                // SO_REUSEADDR emulated when using exclusive bind
                isReuseAddress = (Boolean) value;
            } else {
                // no options that require special handling
                Net.setSocketOption(fd, Net.UNSPEC, name, value);
            }
            return this;
        }
    }
    
    // 获取指定名称的Socket配置参数
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOption(SocketOption<T> name) throws IOException {
        Objects.requireNonNull(name);
        
        if(!supportedOptions().contains(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            if(name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                // SO_REUSEADDR emulated when using exclusive bind
                return (T) Boolean.valueOf(isReuseAddress);
            }
            
            // no options that require special handling
            return (T) Net.getSocketOption(fd, Net.UNSPEC, name);
        }
    }
    
    // 获取当前通道支持的Socket配置参数集合
    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中断回调 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Marks the beginning of an I/O operation that might block.
     *
     * @throws ClosedChannelException if the channel is closed
     * @throws NotYetBoundException   if the channel's socket has not been bound yet
     */
    // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
    private void begin(boolean blocking) throws ClosedChannelException {
        // 如果通道处于阻塞模式，则设置一个线程中断回调，以便可以利用"中断"操作来退出阻塞
        if(blocking) {
            begin();
        }
        
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            if(localAddress == null) {
                throw new NotYetBoundException();
            }
            
            // 如果是阻塞型通道
            if(blocking) {
                // 记录当前通道开始accept操作时所在的native线程引用
                thread = NativeThread.current();
            }
        }
    }
    
    /**
     * Marks the end of an I/O operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this
     *                                    thread being interrupted on a blocking I/O operation.
     */
    // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
    private void end(boolean blocking, boolean completed) throws AsynchronousCloseException {
        // 如果不是阻塞型通道，直接返回
        if(!blocking) {
            return;
        }
        
        synchronized(stateLock) {
            // accept操作结束后，置空其线程号
            thread = 0;
            
            // notify any thread waiting in implCloseSelectableChannel
            if(state == ST_CLOSING) {
                stateLock.notifyAll();
            }
        }
        
        // 清除之前设置的线程中断回调
        end(completed);
    }
    
    /*▲ 中断回调 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 阻塞 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 是否阻塞(block)当前的socket通道
    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        acceptLock.lock();
        try {
            synchronized(stateLock) {
                // 确保通道已经开启，否则抛异常
                ensureOpen();
                // 是否阻塞(block)文件描述符fd关联的通道
                IOUtil.configureBlocking(fd, block);
            }
        } finally {
            acceptLock.unlock();
        }
    }
    
    /*▲ 阻塞 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns true if channel's socket is bound
     */
    // 判断当前通道是否绑定到了本地地址
    boolean isBound() {
        synchronized(stateLock) {
            return localAddress != null;
        }
    }
    
    /** @throws ClosedChannelException if channel is closed */
    // 确保通道已经开启，否则抛异常
    private void ensureOpen() throws ClosedChannelException {
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取绑定的本地地址
    @Override
    public SocketAddress getLocalAddress() throws IOException {
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            // 对指定的地址进行安全校验
            return (localAddress == null) ? null : Net.getRevealedLocalAddress(localAddress);
        }
    }
    
    /**
     * Returns the local address, or null if not bound
     */
    // 获取绑定的本地地址
    InetSocketAddress localAddress() {
        synchronized(stateLock) {
            return localAddress;
        }
    }
    
    /*▲ 地址信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回[服务端Socket(监听)]在Java层的文件描述符
    public FileDescriptor getFD() {
        return fd;
    }
    
    // 返回[服务端Socket(监听)]在本地(native层)的文件描述符
    public int getFDVal() {
        return fdVal;
    }
    
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());
        sb.append('[');
        if(!isOpen()) {
            sb.append("closed");
        } else {
            synchronized(stateLock) {
                InetSocketAddress addr = localAddress;
                if(addr == null) {
                    sb.append("unbound");
                } else {
                    sb.append(Net.getRevealedLocalAddressAsString(addr));
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }
    
    
    // 当前ServerSocket支持的配置参数集合
    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();
        
        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>();
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            if(Net.isReusePortAvailable()) {
                set.add(StandardSocketOptions.SO_REUSEPORT);
            }
            set.add(StandardSocketOptions.IP_TOS);
            set.addAll(ExtendedSocketOptions.options(SOCK_STREAM));
            return Collections.unmodifiableSet(set);
        }
    }
    
    private static native void initIDs();
    
}
