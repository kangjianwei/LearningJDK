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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import sun.net.NetHooks;
import sun.net.ext.ExtendedSocketOptions;
import sun.net.util.SocketExceptions;

import static sun.net.ext.ExtendedSocketOptions.SOCK_STREAM;

/**
 * An implementation of SocketChannels
 */
// Socket通道的本地实现
class SocketChannelImpl extends SocketChannel implements SelChImpl {
    
    /** State, increases monotonically */
    // Socket状态参数
    private static final int ST_UNCONNECTED = 0;  // 未连接
    private static final int ST_CONNECTIONPENDING = 1;  // 正在连接
    private static final int ST_CONNECTED = 2;  // 已连接
    private static final int ST_CLOSING = 3;  // 正在断开连接(关闭)
    private static final int ST_KILLPENDING = 4;  // 正在销毁
    private static final int ST_KILLED = 5;  // 已销毁
    
    /** need stateLock to change */
    private volatile int state;  // SocketChannel状态
    
    
    /** Socket adaptor, created on demand */
    // 由当前Socket通道适配而成的Socket
    private Socket socket;
    
    private final FileDescriptor fd;    // [客户端Socket]/[服务端Socket(通信)]在Java层的文件描述符
    private final int fdVal;            // [客户端Socket]/[服务端Socket(通信)]在本地(native层)的文件描述符
    
    /*
     * 本地地址
     *
     * [客户端Socket] 　　 : 客户端地址
     * [服务端Socket(通信)]: 服务端地址
     */
    private InetSocketAddress localAddress;
    
    /*
     * 远程地址
     *
     * [客户端Socket] 　　 : 服务端的地址
     * [服务端Socket(通信)]: 客户端的地址
     */
    private InetSocketAddress remoteAddress;
    
    /** Used to make native read and write calls */
    private static NativeDispatcher nd; // 完成Socket读写的工具类
    
    /** Input/Output closed */
    private volatile boolean isInputClosed;    // 是否关闭了从当前通道读取数据的功能
    private volatile boolean isOutputClosed;   // 是否关闭了向当前通道写入数据的功能
    
    /** set true when exclusive binding is on and SO_REUSEADDR is emulated */
    private boolean isReuseAddress; // 是否允许立刻重用已关闭的socket端口
    
    /** IDs of native threads doing reads and writes, for signalling */
    private long readerThread;  // 当前通道开始读/连接操作时所在的native线程号
    private long writerThread;  // 当前通道开始写操作时所在的native线程号
    
    /** Lock held by current reading or connecting thread */
    private final ReentrantLock readLock = new ReentrantLock();     // 读锁
    /** Lock held by current writing or connecting thread */
    private final ReentrantLock writeLock = new ReentrantLock();    // 写锁
    /**
     * Lock held by any thread that modifies the state fields declared below
     * DO NOT invoke a blocking I/O operation while holding this lock!
     */
    private final Object stateLock = new Object();  // 状态锁
    
    
    static {
        IOUtil.load();
        nd = new SocketDispatcher();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Constructor for normal connecting sockets */
    /*
     * 构造一个未绑定的[客户端Socket]，内部初始化了该Socket的文件描述符
     */
    SocketChannelImpl(SelectorProvider provider) throws IOException {
        super(provider);
        
        // 创建[客户端Socket]，并返回其在Java层的文件描述符
        this.fd = Net.socket(true);
        
        // 获取[客户端Socket]在本地(native层)的文件描述符
        this.fdVal = IOUtil.fdVal(fd);
    }
    
    /** Constructor for sockets obtained from server sockets */
    /*
     * 构造一个[服务端Socket(通信)]
     *
     * 注：该方法是在ServerSocketChannelImpl中被调用的
     */
    SocketChannelImpl(SelectorProvider provider, FileDescriptor fd, InetSocketAddress isa) throws IOException {
        super(provider);
        
        // 记录[服务端Socket(通信)]在Java层的文件描述符
        this.fd = fd;
        
        // 获取[服务端Socket(通信)]在本地(native层)的文件描述符
        this.fdVal = IOUtil.fdVal(fd);
        
        synchronized(stateLock) {
            this.localAddress = Net.localAddress(fd);
            this.remoteAddress = isa;
            this.state = ST_CONNECTED;
        }
    }
    
    /*
     * 构造一个Socket
     */
    SocketChannelImpl(SelectorProvider provider, FileDescriptor fd, boolean bound) throws IOException {
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
            this.localAddress = Net.localAddress(fd);
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 适配 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回由当前Socket通道适配而成的Socket
    @Override
    public Socket socket() {
        synchronized(stateLock) {
            if(socket == null) {
                socket = SocketAdaptor.create(this);
            }
            
            return socket;
        }
    }
    
    /*▲ 适配 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 对[客户端Socket]执行【bind】操作；local为null时，使用通配IP和随机端口
    @Override
    public SocketChannel bind(SocketAddress local) throws IOException {
        readLock.lock();
        
        try {
            writeLock.lock();
            
            try {
                synchronized(stateLock) {
                    // 确保通道已经开启，否则抛异常
                    ensureOpen();
                    
                    // 如果正在连接中，则抛出异常
                    if(state == ST_CONNECTIONPENDING) {
                        throw new ConnectionPendingException();
                    }
                    
                    // 如果已绑定，则抛出异常
                    if(localAddress != null) {
                        throw new AlreadyBoundException();
                    }
                    
                    // 确定绑定到哪个地址
                    InetSocketAddress isa = (local == null) ? new InetSocketAddress(0)  // 如果未指定绑定地址，则使用通配IP和随机端口
                        : Net.checkAddress(local);
                    
                    // 权限校验
                    SecurityManager sm = System.getSecurityManager();
                    if(sm != null) {
                        sm.checkListen(isa.getPort());
                    }
                    
                    // Socket进行bind操作之前的回调
                    NetHooks.beforeTcpBind(fd, isa.getAddress(), isa.getPort());
                    
                    // 为[客户端Socket]绑定IP地址与端口号
                    Net.bind(fd, isa.getAddress(), isa.getPort());
                    
                    // 记录绑定的本地地址
                    localAddress = Net.localAddress(fd);
                }
            } finally {
                writeLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
        
        return this;
    }
    
    /*
     * 对[客户端Socket]执行【connect】操作，以便连接到远端Socket
     *
     * 在非阻塞模式下：如果成功建立了连接，则返回true；如果连接失败，则返回false，并且需要后续调用finishConnect()来完成连接操作。
     * 在阻塞模式下，则连接操作会陷入阻塞，直到成功建立连接，或者发生IO异常。
     *
     * 在连接过程中发起的读写操作将被阻塞，直到连接完成
     */
    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        // 检查远程Socket地址是否合规
        InetSocketAddress isa = Net.checkAddress(remote);
        
        // 权限校验
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());
        }
        
        InetAddress ia = isa.getAddress();
        // 如果远程地址是通配地址
        if(ia.isAnyLocalAddress()) {
            // 获取本地主机地址，以便后续连接使用
            ia = InetAddress.getLocalHost();
        }
        
        try {
            readLock.lock();
            try {
                writeLock.lock();
                try {
                    int n = 0;
                    
                    // 判断当前通道是否为阻塞模式
                    boolean blocking = isBlocking();
                    
                    try {
                        /*
                         * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                         * 此处会将通道设置为正在连接状态...
                         */
                        beginConnect(blocking, isa);
                        
                        do {
                            // 当前Socket向指定地址处的ServerSocket发起连接（如果采用堵塞模式，会一直等待，直到成功或出现异常）
                            n = Net.connect(fd, ia, isa.getPort());
                        } while(n == IOStatus.INTERRUPTED && isOpen());
                    } finally {
                        // 在当前Socket发起连接之后，移除为线程中断设置的回调，在连接成功的情形下，设置本地地址，并更新连接状态为已连接
                        endConnect(blocking, (n>0));
                    }
                    
                    assert IOStatus.check(n);
                    
                    return n>0;
                } finally {
                    writeLock.unlock();
                }
            } finally {
                readLock.unlock();
            }
        } catch(IOException ioe) {
            // connect failed, close the channel
            close();
            throw SocketExceptions.of(ioe, isa);
        }
    }
    
    /*
     * 促进当前Socket完成与远程的连接
     *
     * 在非阻塞模式下：如果成功建立了连接，则返回true；如果连接失败，则返回false
     * 在阻塞模式下 ：不断检查连接是否成功，直到连接成功建立，或发生IO异常。
     */
    @Override
    public boolean finishConnect() throws IOException {
        try {
            readLock.lock();
            try {
                writeLock.lock();
                try {
                    // 如果已经连接，立即返回true
                    if(isConnected()) {
                        return true;
                    }
    
                    // 判断当前通道是否为阻塞模式
                    boolean blocking = isBlocking();
    
                    boolean connected = false;
    
                    try {
                        /*
                         * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                         * 如果通道未处于"正在连接"状态，则抛异常...
                         */
                        beginFinishConnect(blocking);
        
                        int n = 0;
        
                        if(blocking) {
                            do {
                                // 阻塞模式下，不断检查连接是否成功，直到连接成功建立，或发生异常
                                n = checkConnect(fd, true);
                            } while((n == 0 || n == IOStatus.INTERRUPTED) && isOpen());
                        } else {
                            // 非阻塞模式下，检查一次连接是否成功
                            n = checkConnect(fd, false);
                        }
        
                        connected = (n>0);
                    } finally {
                        // 在检测Socket是否完成连接之后，移除为线程中断设置的回调，在Socket成功连接的情形下，设置本地地址，并更新连接状态为已连接
                        endFinishConnect(blocking, connected);
                    }
    
                    assert (blocking && connected) ^ !blocking;
    
                    return connected;
                } finally {
                    writeLock.unlock();
                }
            } finally {
                readLock.unlock();
            }
        } catch(IOException ioe) {
            // connect failed, close the channel
            close();
            throw SocketExceptions.of(ioe, remoteAddress);
        }
    }
    
    
    /**
     * Poll this channel's socket for a connection, up to the given timeout.
     *
     * @return {@code true} if the socket is polled
     */
    /*
     * 注册监听连接事件(Net.POLLCONN)，连接成功后，当前Socket会收到通知。
     * timeout是允许可阻塞socket等待连接的时间。
     * 返回值指示是否在阻塞时间内收到了"成功建立连接"的信号。
     */
    boolean pollConnected(long timeout) throws IOException {
        boolean blocking = isBlocking();
        assert Thread.holdsLock(blockingLock()) && blocking;
        
        readLock.lock();
        try {
            writeLock.lock();
            try {
                boolean polled = false;
                
                try {
                    /*
                     * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                     * 如果通道未处于"正在连接"状态，则抛异常...
                     */
                    beginFinishConnect(blocking);
                    
                    // 注册Net.POLLIN事件，远端发来"已连接"的消息时，会通知当前Socket
                    int events = Net.poll(fd, Net.POLLCONN, timeout);
                    
                    polled = (events != 0);
                } finally {
                    /*
                     * invoke endFinishConnect with completed=false so that the state is not changed to ST_CONNECTED.
                     * The socket adaptor will use finishConnect to finish.
                     */
                    endFinishConnect(blocking, /*completed*/false);
                }
                return polled;
            } finally {
                writeLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
    }
    
    /*▲ socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Invoked by implCloseChannel to close the channel.
     *
     * This method waits for outstanding I/O operations to complete.
     * When in blocking mode, the socket is pre-closed and the threads in blocking I/O operations are signalled to ensure that the outstanding I/O operations complete quickly.
     *
     * If the socket is connected then it is shutdown by this method.
     * The shutdown ensures that the peer reads EOF for the case that the socket is not pre-closed or closed by this method.
     *
     * The socket is closed by this method when it is not registered with a Selector.
     * Note that a channel configured blocking may be registered with a Selector.
     * This arises when a key is canceled and the channel configured to blocking mode before the key is flushed from the Selector.
     */
    // 实现对"可选择"通道的关闭操作
    @Override
    protected void implCloseSelectableChannel() throws IOException {
        assert !isOpen();
    
        boolean blocking;
        boolean connected;
        boolean interrupted = false;
    
        // 进入正在断开连接的状态
        synchronized(stateLock) {
            assert state<ST_CLOSING;
        
            blocking = isBlocking();
            connected = (state == ST_CONNECTED);
        
            // 标记通道进入正在断开连接的状态
            state = ST_CLOSING;
        }
    
        /* wait for any outstanding I/O operations to complete */
        // 对于阻塞式通道，需要等到该通道上其他阻塞操作结束之后才能进行关闭
        if(blocking) {
            synchronized(stateLock) {
                assert state == ST_CLOSING;
            
                long reader = readerThread;
                long writer = writerThread;
            
                // 如果该通道上存在其他阻塞的读/写操作，则唤醒那些阻塞的操作，以促使它们结束运行
                if(reader != 0 || writer != 0) {
                    nd.preClose(fd);
                    connected = false; // fd is no longer connected socket
                
                    if(reader != 0) {
                        // 唤醒阻塞的reader线程
                        NativeThread.signal(reader);
                    }
                
                    if(writer != 0) {
                        // 唤醒阻塞的writer线程
                        NativeThread.signal(writer);
                    }
                
                    // wait for blocking I/O operations to end
                    while(readerThread != 0 || writerThread != 0) {
                        try {
                            stateLock.wait();
                        } catch(InterruptedException e) {
                            interrupted = true;
                        }
                    }
                }
            }
        
            // 对于非阻塞通道，确保其他读写操作已经完成
        } else {
            // non-blocking mode: wait for read/write to complete
            readLock.lock();
            try {
                writeLock.lock();
                writeLock.unlock();
            } finally {
                readLock.unlock();
            }
        }
    
        synchronized(stateLock) {
            assert state == ST_CLOSING;
        
            /*
             * if connected and the channel is registered with a Selector
             * then shutdown the output if possible so that the peer reads EOF.
             * If SO_LINGER is enabled and set to a non-zero value
             * then it needs to be disabled so that the Selector does not wait
             * when it closes the socket.
             */
            if(connected && isRegistered()) {
                try {
                    // 是否启用延时关闭
                    SocketOption<Integer> opt = StandardSocketOptions.SO_LINGER;
                    int interval = (int) Net.getSocketOption(fd, Net.UNSPEC, opt);
                    if(interval != 0) {
                        if(interval>0) {
                            // disable SO_LINGER
                            Net.setSocketOption(fd, Net.UNSPEC, opt, -1);
                        }
                    
                        Net.shutdown(fd, Net.SHUT_WR);
                    }
                } catch(IOException ignore) {
                }
            }
        
            // 进入正在销毁的状态
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
                // 通道进入销毁状态
                state = ST_KILLED;
                // 释放对本地文件描述符的引用
                nd.close(fd);
            }
        }
    }
    
    
    // 关闭从当前通道读取数据的功能
    @Override
    public SocketChannel shutdownInput() throws IOException {
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            if(!isConnected()) {
                throw new NotYetConnectedException();
            }
            
            if(!isInputClosed) {
                Net.shutdown(fd, Net.SHUT_RD);
                
                long reader = readerThread;
                
                if(reader != 0) {
                    // 唤醒阻塞的thread线程
                    NativeThread.signal(reader);
                }
                
                isInputClosed = true;
            }
            
            return this;
        }
    }
    
    // 关闭向当前通道写入数据的功能
    @Override
    public SocketChannel shutdownOutput() throws IOException {
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            if(!isConnected()) {
                throw new NotYetConnectedException();
            }
            
            if(!isOutputClosed) {
                Net.shutdown(fd, Net.SHUT_WR);
                
                long writer = writerThread;
                
                if(writer != 0) {
                    // 唤醒阻塞的thread线程
                    NativeThread.signal(writer);
                }
                
                isOutputClosed = true;
            }
            
            return this;
        }
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 从当前通道中读取数据，读到的内容写入dst
    @Override
    public int read(ByteBuffer dst) throws IOException {
        Objects.requireNonNull(dst);
        
        readLock.lock();
        try {
            boolean blocking = isBlocking();
            int n = 0;
            try {
                /*
                 * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                 * 此处要确保通道处于"已连接"状态...
                 */
                beginRead(blocking);
                
                // check if input is shutdown
                if(isInputClosed) {
                    return IOStatus.EOF;
                }
                
                /*
                 * 接下来，从当前通道中起始位置处读取，读到的内容写入dst后，
                 * 返回值表示从当前通道读取的字节数量
                 */
                
                // 阻塞模式下，如果当前通道中没有可读数据，则陷入阻塞
                if(blocking) {
                    do {
                        n = IOUtil.read(fd, dst, -1, nd);
                    } while(n == IOStatus.INTERRUPTED && isOpen());
                } else {
                    n = IOUtil.read(fd, dst, -1, nd);
                }
            } finally {
                // 标记可能阻塞的I/O操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endRead(blocking, n>0);
                
                if(n<=0 && isInputClosed) {
                    return IOStatus.EOF;
                }
            }
            
            return IOStatus.normalize(n);
        } finally {
            readLock.unlock();
        }
    }
    
    //【散射】从当前通道中读取数据，读到的内容依次写入dsts中offset处起的length个缓冲区
    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, dsts.length);
        
        readLock.lock();
        
        try {
            // 当前通道是否为阻塞模式
            boolean blocking = isBlocking();
            
            long n = 0;
            
            try {
                /*
                 * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                 * 此处要确保通道处于"已连接"状态...
                 */
                beginRead(blocking);
                
                // check if input is shutdown
                if(isInputClosed) {
                    return IOStatus.EOF;
                }
                
                /*
                 * 接下来，从当前通道中读取，读到的内容依次写入dsts[offset, offset+length-1]中各个缓冲区后，
                 * 返回值表示从当前通道读取的字节数量
                 */
                
                // 阻塞模式下，如果当前通道中没有可读数据，则陷入阻塞
                if(blocking) {
                    do {
                        // 从文件描述符fd（关联的socket）中读取，读到的内容依次存入dsts中offset处起的length个缓冲区中，返回读到的字节数量
                        n = IOUtil.read(fd, dsts, offset, length, nd);
                    } while(n == IOStatus.INTERRUPTED && isOpen());
                } else {
                    // 从文件描述符fd（关联的socket）中读取，读到的内容依次存入dsts中offset处起的length个缓冲区中，返回读到的字节数量
                    n = IOUtil.read(fd, dsts, offset, length, nd);
                }
            } finally {
                // 标记可能阻塞的I/O操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endRead(blocking, n>0);
                if(n<=0 && isInputClosed) {
                    return IOStatus.EOF;
                }
            }
            
            return IOStatus.normalize(n);
        } finally {
            readLock.unlock();
        }
    }
    
    
    // 从src中读取数据，读到的内容向当前通道中写入
    @Override
    public int write(ByteBuffer src) throws IOException {
        Objects.requireNonNull(src);
        
        writeLock.lock();
        
        try {
            boolean blocking = isBlocking();
            
            int n = 0;
            
            try {
                /*
                 * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                 * 此处要确保通道处于"已连接"状态...
                 */
                beginWrite(blocking);
                
                /*
                 * 接下来，从缓冲区src读取，读到的内容向当前通道中追加，返回值表示写入当前通道的字节数量
                 * 注：不使用DirectIO
                 */
                
                // 阻塞模式下，如果当前通道中没有可读数据，则陷入阻塞
                if(blocking) {
                    do {
                        n = IOUtil.write(fd, src, -1, nd);
                    } while(n == IOStatus.INTERRUPTED && isOpen());
                } else {
                    n = IOUtil.write(fd, src, -1, nd);
                }
            } finally {
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endWrite(blocking, n>0);
                if(n<=0 && isOutputClosed) {
                    throw new AsynchronousCloseException();
                }
            }
            return IOStatus.normalize(n);
        } finally {
            writeLock.unlock();
        }
    }
    
    //【聚集】从srcs中offset处起的length个缓冲区读取数据，读到的内容向当前通道中写入
    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, srcs.length);
        
        writeLock.lock();
        
        try {
            boolean blocking = isBlocking();
            
            long n = 0;
            
            try {
                /*
                 * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                 * 此处要确保通道处于"已连接"状态...
                 */
                beginWrite(blocking);
                
                /*
                 * 接下来，从srcs[offset, offset+length-1]中各个缓冲区读取数据，读到的内容向当前通道中起始位置处写入，返回值表示写入的字节数量
                 * 注：不使用DirectIO
                 */
                
                if(blocking) {
                    do {
                        //
                        n = IOUtil.write(fd, srcs, offset, length, nd);
                    } while(n == IOStatus.INTERRUPTED && isOpen());
                } else {
                    n = IOUtil.write(fd, srcs, offset, length, nd);
                }
            } finally {
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endWrite(blocking, n>0);
                if(n<=0 && isOutputClosed) {
                    throw new AsynchronousCloseException();
                }
            }
            
            return IOStatus.normalize(n);
        } finally {
            writeLock.unlock();
        }
    }
    
    
    /**
     * Poll this channel's socket for reading up to the given timeout.
     *
     * @return {@code true} if the socket is polled
     */
    /*
     * 注册监听可读事件(Net.POLLIN)，当通道内有数据可读时，当前Socket会收到通知。
     * timeout是允许可阻塞socket等待可读数据的时间。
     * 返回值指示是否在阻塞时间内收到了"可读取数据"的信号。
     */
    boolean pollRead(long timeout) throws IOException {
        boolean blocking = isBlocking();
        
        assert Thread.holdsLock(blockingLock()) && blocking;
        
        readLock.lock();
        
        try {
            boolean polled = false;
            
            try {
                /*
                 * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                 * 此处要确保通道处于"已连接"状态...
                 */
                beginRead(blocking);
                
                // 注册Net.POLLIN事件，远端发来"有可读数据"的消息时，会通知当前Socket
                int events = Net.poll(fd, Net.POLLIN, timeout);
                
                polled = (events != 0);
            } finally {
                // 标记可能阻塞的I/O操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endRead(blocking, polled);
            }
            return polled;
        } finally {
            readLock.unlock();
        }
    }
    
    /*▲ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 监听参数/事件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Translates an interest operation set into a native poll event set
     */
    /*
     * 翻译Socket通道注册的监听事件，返回对ops的翻译结果
     *
     * 方向：Java层 --> native层
     * 　　　SelectionKey.XXX --> Net.XXX
     */
    public int translateInterestOps(int ops) {
        int newOps = 0;
        
        if((ops & SelectionKey.OP_READ) != 0) {
            newOps |= Net.POLLIN;
        }
        
        if((ops & SelectionKey.OP_WRITE) != 0) {
            newOps |= Net.POLLOUT;
        }
        
        if((ops & SelectionKey.OP_CONNECT) != 0) {
            newOps |= Net.POLLCONN;
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
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, ski.nioReadyOps(), ski);
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
     * Translates native poll revent ops into a ready operation ops
     */
    /*
     *【增量更新】已就绪事件(基于initialOps叠加)
     *
     * 将本地(native)反馈的就绪信号ops翻译并存储到Java层的就绪事件readyOps中，
     * 返回值指示上一次反馈的事件与本次反馈的事件是否发生了改变。
     *
     * 通道收到有效的反馈事件后，会将其【叠加】在initialOps上，换句话说是对已就绪事件的增量更新。
     * 如果本地(native)反馈了错误或挂起信号，则将已就绪事件直接设置为通道注册的监听事件
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
        
        // 获取注册的监听事件：SelectionKey.XXX(不会验证当前"选择键"是否有效)
        int intOps = selectionKey.nioInterestOps();
        
        // 获取已就绪事件
        int oldOps = selectionKey.nioReadyOps();
        int newOps = initialOps;
        
        // 本地(native)反馈了错误或挂起的信号
        if((ops & (Net.POLLERR | Net.POLLHUP)) != 0) {
            newOps = intOps;
            // 直接将通道注册的监听事件设置为已就绪事件
            selectionKey.nioReadyOps(newOps);
            return (newOps & ~oldOps) != 0;
        }
        
        // 该通道监听了"连接"事件，且通道已经连接到远端
        if(((ops & Net.POLLCONN) != 0) && ((intOps & SelectionKey.OP_CONNECT) != 0) && isConnectionPending()) {
            newOps |= SelectionKey.OP_CONNECT;
        }
        
        // 判断当前通道是否已连接到远端
        boolean connected = isConnected();
        
        // 该通道监听了"可读"事件，且通道内有数据可读
        if(((ops & Net.POLLIN) != 0) && ((intOps & SelectionKey.OP_READ) != 0) && connected) {
            newOps |= SelectionKey.OP_READ;
        }
        
        // 该通道监听了"可写"事件，且可以向通道写入数据
        if(((ops & Net.POLLOUT) != 0) && ((intOps & SelectionKey.OP_WRITE) != 0) && connected) {
            newOps |= SelectionKey.OP_WRITE;
        }
        
        // 将newOps设置为已就绪事件
        selectionKey.nioReadyOps(newOps);
        
        return (newOps & ~oldOps) != 0;
    }
    
    /*▲ 就绪参数/事件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 设置指定名称的Socket配置参数
    @Override
    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
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
                return this;
            }
            
            // no options that require special handling
            Net.setSocketOption(fd, Net.UNSPEC, name, value);
            
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
            
            // special handling for IP_TOS: always return 0 when IPv6
            if(name == StandardSocketOptions.IP_TOS) {
                ProtocolFamily family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
                return (T) Net.getSocketOption(fd, family, name);
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
     * Marks the beginning of a connect operation that might block.
     *
     * @param blocking true if configured blocking
     * @param isa      the remote address
     *
     * @throws ClosedChannelException     if the channel is closed
     * @throws AlreadyConnectedException  if already connected
     * @throws ConnectionPendingException is a connection is pending
     * @throws IOException                if the pre-connect hook fails
     */
    /*
     * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
     * 此处会将通道设置为正在连接状态...
     */
    private void beginConnect(boolean blocking, InetSocketAddress isa) throws IOException {
        // 如果通道处于阻塞模式，则设置一个线程中断回调，以便可以利用"中断"操作来退出阻塞
        if(blocking) {
            begin();
        }
        
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            int state = this.state;
            if(state == ST_CONNECTED) {
                // 如果当前Socket已连接
                throw new AlreadyConnectedException();
            }
            
            if(state == ST_CONNECTIONPENDING) {
                // 如果当前Socket正在连接
                throw new ConnectionPendingException();
            }
            
            assert state == ST_UNCONNECTED;
            
            // 进入"正在连接"状态
            this.state = ST_CONNECTIONPENDING;
            
            if(localAddress == null) {
                // Socket进行connect操作之前的回调
                NetHooks.beforeTcpConnect(fd, isa.getAddress(), isa.getPort());
            }
            
            // 更新远程地址
            remoteAddress = isa;
            
            // 如果是阻塞型通道
            if(blocking) {
                // 记录当前通道开始连接操作时所在的native线程引用
                readerThread = NativeThread.current();
            }
        }
    }
    
    /**
     * Marks the end of a connect operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this
     *                                    thread being interrupted on a blocking connect operation.
     * @throws IOException                if completed and unable to obtain the local address
     */
    // 在socket发起连接之后，移除为线程中断设置的回调，在连接成功的情形下，设置本地地址，并更新连接状态为已连接
    private void endConnect(boolean blocking, boolean completed) throws IOException {
        // 标记可能阻塞的I/O操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
        endRead(blocking, completed);
        
        // 如果socket成功连接到服务端socket
        if(completed) {
            synchronized(stateLock) {
                if(state == ST_CONNECTIONPENDING) {
                    // 更新本地地址
                    localAddress = Net.localAddress(fd);
                    // 连接状态更新为已连接
                    state = ST_CONNECTED;
                }
            }
        }
    }
    
    /**
     * Marks the beginning of a finishConnect operation that might block.
     *
     * @throws ClosedChannelException       if the channel is closed
     * @throws NoConnectionPendingException if no connection is pending
     */
    /*
     * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
     * 如果通道未处于"正在连接"状态，则抛异常...
     */
    private void beginFinishConnect(boolean blocking) throws ClosedChannelException {
        // 如果通道处于阻塞模式，则设置一个线程中断回调，以便可以利用"中断"操作来退出阻塞
        if(blocking) {
            begin();
        }
        
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            if(state != ST_CONNECTIONPENDING) {
                throw new NoConnectionPendingException();
            }
            
            // 如果是阻塞型通道
            if(blocking) {
                // 记录当前通道开始连接操作时所在的native线程引用
                readerThread = NativeThread.current();
            }
        }
    }
    
    /**
     * Marks the end of a finishConnect operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this
     *                                    thread being interrupted on a blocking connect operation.
     * @throws IOException                if completed and unable to obtain the local address
     */
    // 在检测完连接是否连接之后，移除为线程中断设置的回调，在连接成功的情形下，设置本地地址，并更新连接状态为已连接
    private void endFinishConnect(boolean blocking, boolean completed) throws IOException {
        // 标记可能阻塞的I/O操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
        endRead(blocking, completed);
        
        // 如果成功建立了连接，
        if(completed) {
            synchronized(stateLock) {
                if(state == ST_CONNECTIONPENDING) {
                    // 更新本地地址
                    localAddress = Net.localAddress(fd);
                    // 连接状态更新为已连接
                    state = ST_CONNECTED;
                }
            }
        }
    }
    
    /**
     * Marks the beginning of a read operation that might block.
     *
     * @throws ClosedChannelException   if the channel is closed
     * @throws NotYetConnectedException if the channel is not yet connected
     */
    /*
     * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
     * 此处要确保通道处于"已连接"状态...
     */
    private void beginRead(boolean blocking) throws ClosedChannelException {
        // 如果不是阻塞型通道，直接返回
        if(!blocking) {
            // 确保通道处于"已连接"状态
            ensureOpenAndConnected();
            return;
        }
        
        // 如果通道处于阻塞模式，则设置一个线程中断回调，以便可以利用"中断"操作来退出阻塞
        begin();
        
        synchronized(stateLock) {
            // 确保通道处于"已连接"状态
            ensureOpenAndConnected();
            
            // 记录当前通道开始读操作时所在的native线程引用
            readerThread = NativeThread.current();
        }
    }
    
    /**
     * Marks the end of a read operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this thread being interrupted on a blocking read operation.
     */
    // 标记可能阻塞的I/O操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
    private void endRead(boolean blocking, boolean completed) throws AsynchronousCloseException {
        // 如果不是阻塞型通道，直接返回
        if(!blocking) {
            return;
        }
        
        synchronized(stateLock) {
            // 读/连接操作结束后，置空线程号
            readerThread = 0;
            
            // notify any thread waiting in implCloseSelectableChannel
            if(state == ST_CLOSING) {
                stateLock.notifyAll();
            }
        }
        
        // 清除之前设置的线程中断回调
        end(completed);
    }
    
    /**
     * Marks the beginning of a write operation that might block.
     *
     * @throws ClosedChannelException   if the channel is closed or output shutdown
     * @throws NotYetConnectedException if the channel is not yet connected
     */
    /*
     * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
     * 此处要确保通道处于"已连接"状态...
     */
    private void beginWrite(boolean blocking) throws ClosedChannelException {
        // 如果不是阻塞型通道，直接返回
        if(!blocking) {
            // 确保通道处于"已连接"状态
            ensureOpenAndConnected();
            return;
        }
        
        // 如果通道处于阻塞模式，则设置一个线程中断回调，以便可以利用"中断"操作来退出阻塞
        begin();
        
        synchronized(stateLock) {
            // 确保通道处于"已连接"状态
            ensureOpenAndConnected();
            
            if(isOutputClosed) {
                throw new ClosedChannelException();
            }
            
            // 记录当前通道开始写操作时所在的native线程引用
            writerThread = NativeThread.current();
        }
    }
    
    /**
     * Marks the end of a write operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this
     *                                    thread being interrupted on a blocking write operation.
     */
    // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
    private void endWrite(boolean blocking, boolean completed) throws AsynchronousCloseException {
        // 如果不是阻塞型通道，直接返回
        if(!blocking) {
            return;
        }
        
        synchronized(stateLock) {
            // 写操作结束后，置空线程号
            writerThread = 0;
            
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
        readLock.lock();
        try {
            writeLock.lock();
            try {
                synchronized(stateLock) {
                    // 确保通道已经开启，否则抛异常
                    ensureOpen();
                    // 是否阻塞(block)文件描述符fd关联的通道
                    IOUtil.configureBlocking(fd, block);
                }
            } finally {
                writeLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
    }
    
    /*▲ 阻塞 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断当前通道是否正在尝试连接，但还未连接上
    @Override
    public boolean isConnectionPending() {
        return (state == ST_CONNECTIONPENDING);
    }
    
    // 判断当前通道是否已连接到远端
    @Override
    public boolean isConnected() {
        return (state == ST_CONNECTED);
    }
    
    // 判断是否可以使用输入流(从当前通道读取)
    boolean isInputOpen() {
        return !isInputClosed;
    }
    
    // 判断是否可以使用输出流(向当前通道写入)
    boolean isOutputOpen() {
        return !isOutputClosed;
    }
    
    /**
     * Checks that the channel is open.
     *
     * @throws ClosedChannelException if channel is closed (or closing)
     */
    // 确保通道已经开启，否则抛异常
    private void ensureOpen() throws ClosedChannelException {
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
    }
    
    /**
     * Checks that the channel is open and connected.
     *
     * @throws ClosedChannelException   if channel is closed (or closing)
     * @throws NotYetConnectedException if open and not connected
     * @apiNote This method uses the "state" field to check if the channel is
     * open. It should never be used in conjuncion with isOpen or ensureOpen
     * as these methods check AbstractInterruptibleChannel's closed field - that
     * field is set before implCloseSelectableChannel is called and so before
     * the state is changed.
     */
    // 确保通道处于"已连接"状态
    private void ensureOpenAndConnected() throws ClosedChannelException {
        int state = this.state;
        
        if(state<ST_CONNECTED) {
            throw new NotYetConnectedException();
        } else if(state>ST_CONNECTED) {
            throw new ClosedChannelException();
        }
    }
    
    // 检查Socket是否成功建立连接
    private static native int checkConnect(FileDescriptor fd, boolean block) throws IOException;
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取绑定的本地地址
    @Override
    public SocketAddress getLocalAddress() throws IOException {
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            // 对指定的地址进行安全校验
            return Net.getRevealedLocalAddress(localAddress);
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
    
    
    // 获取连接到的远程地址
    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            return remoteAddress;
        }
    }
    
    /**
     * Returns the remote address, or null if not connected
     */
    // 获取连接到的远程地址
    InetSocketAddress remoteAddress() {
        synchronized(stateLock) {
            return remoteAddress;
        }
    }
    
    /*▲ 地址信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回通道在Java层的文件描述符
    public FileDescriptor getFD() {
        return fd;
    }
    
    // 返回通道在本地(native层)的文件描述符
    public int getFDVal() {
        return fdVal;
    }
    
    /**
     * Writes a byte of out of band data.
     */
    // 发送一个字节的"紧急数据"，参见SocketOptions#SO_OOBINLINE参数
    int sendOutOfBandData(byte b) throws IOException {
        writeLock.lock();
        
        try {
            boolean blocking = isBlocking();
            int n = 0;
            
            try {
                /*
                 * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                 * 此处要确保通道处于"已连接"状态...
                 */
                beginWrite(blocking);
                
                if(blocking) {
                    do {
                        n = sendOutOfBandData(fd, b);
                    } while(n == IOStatus.INTERRUPTED && isOpen());
                } else {
                    n = sendOutOfBandData(fd, b);
                }
            } finally {
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endWrite(blocking, n>0);
                if(n<=0 && isOutputClosed) {
                    throw new AsynchronousCloseException();
                }
            }
            return IOStatus.normalize(n);
        } finally {
            writeLock.unlock();
        }
    }
    
    // 发送一个字节的"紧急数据"，参见SocketOptions#SO_OOBINLINE参数
    private static native int sendOutOfBandData(FileDescriptor fd, byte data) throws IOException;
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSuperclass().getName());
        sb.append('[');
        if(!isOpen()) {
            sb.append("closed");
        } else {
            synchronized(stateLock) {
                switch(state) {
                    case ST_UNCONNECTED:
                        sb.append("unconnected");
                        break;
                    case ST_CONNECTIONPENDING:
                        sb.append("connection-pending");
                        break;
                    case ST_CONNECTED:
                        sb.append("connected");
                        if(isInputClosed) {
                            sb.append(" ishut");
                        }
                        if(isOutputClosed) {
                            sb.append(" oshut");
                        }
                        break;
                }
                InetSocketAddress addr = localAddress();
                if(addr != null) {
                    sb.append(" local=");
                    sb.append(Net.getRevealedLocalAddressAsString(addr));
                }
                if(remoteAddress() != null) {
                    sb.append(" remote=");
                    sb.append(remoteAddress().toString());
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }
    
    
    // 当前Socket支持的配置参数集合
    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();
        
        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>();
            set.add(StandardSocketOptions.SO_SNDBUF);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_KEEPALIVE);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            if(Net.isReusePortAvailable()) {
                set.add(StandardSocketOptions.SO_REUSEPORT);
            }
            set.add(StandardSocketOptions.SO_LINGER);
            set.add(StandardSocketOptions.TCP_NODELAY);
            // additional options required by socket adaptor
            set.add(StandardSocketOptions.IP_TOS);
            set.add(ExtendedSocketOption.SO_OOBINLINE);
            set.addAll(ExtendedSocketOptions.options(SOCK_STREAM));
            return Collections.unmodifiableSet(set);
        }
    }
    
}
