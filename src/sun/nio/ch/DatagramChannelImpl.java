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

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import sun.net.ResourceManager;
import sun.net.ext.ExtendedSocketOptions;

import static sun.net.ext.ExtendedSocketOptions.SOCK_DGRAM;

/**
 * An implementation of DatagramChannels.
 */
// UDP-Socket通道的本地实现
class DatagramChannelImpl extends DatagramChannel implements SelChImpl {
    
    /** State (does not necessarily increase monotonically) */
    // Socket状态参数
    private static final int ST_UNCONNECTED = 0;  // 未连接
    private static final int ST_CONNECTED = 1;  // 已连接
    private static final int ST_CLOSING = 2;  // 正在断开连接(关闭)
    private static final int ST_KILLPENDING = 3;  // 正在销毁
    private static final int ST_KILLED = 4;  // 已销毁
    
    // DatagramChannel状态
    private int state;
    
    
    /** Our socket adaptor, if any */
    // 由当前DatagramChannel通道适配而成的DatagramChannel
    private DatagramSocket socket;
    
    private final FileDescriptor fd;    // UDP-Socket在Java层的文件描述符
    private final int fdVal;            // UDP-Socket在本地(native层)的文件描述符
    
    /** The protocol family of the socket */
    // 当前UDP-Socket使用的IP协议
    private final ProtocolFamily family;
    
    /** Binding and remote address (when connected) */
    private InetSocketAddress localAddress;   // UDP-Socket绑定的本地地址
    private InetSocketAddress remoteAddress;  // UDP-Socket连接的远程地址
    
    // 由receive0()方法设置，记录接收到的数据的发送端地址
    private SocketAddress sender;
    
    /** Used to make native read and write calls */
    private static NativeDispatcher nd = new DatagramDispatcher();  // 完成Socket读写的工具类
    
    /** IDs of native threads doing reads and writes, for signalling */
    private long readerThread;  // 当前通道开始读操作时所在的native线程号
    private long writerThread;  // 当前通道开始写操作时所在的native线程号
    
    /** Lock held by current reading or connecting thread */
    private final ReentrantLock readLock = new ReentrantLock();
    /** Lock held by current writing or connecting thread */
    private final ReentrantLock writeLock = new ReentrantLock();
    /**
     * Lock held by any thread that modifies the state fields declared below
     * DO NOT invoke a blocking I/O operation while holding this lock!
     */
    private final Object stateLock = new Object();
    
    /** Multicast support */
    // 组播Socket注册表，记录了当前组播Socket在其所有组播小组上的注册信息
    private MembershipRegistry registry;
    
    /** set true/false when socket is already bound and SO_REUSEADDR is emulated */
    private boolean isReuseAddress;
    /** set true when socket is bound and SO_REUSEADDRESS is emulated */
    private boolean reuseAddressEmulated;
    
    /** Cached InetAddress and port for unconnected DatagramChannels used by receive0 */
    private InetAddress cachedSenderInetAddress;
    private int cachedSenderPort;
    
    
    static {
        IOUtil.load();
        initIDs();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 构造DatagramSocket通道，内部初始化了该Socket的文件描述符
    public DatagramChannelImpl(SelectorProvider provider) throws IOException {
        super(provider);
        
        ResourceManager.beforeUdpCreate();
        
        try {
            // 获取当前UDP-Socket使用的IP协议
            this.family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
            
            // 创建UDP-Socket，并返回其在Java层的文件描述符
            this.fd = Net.socket(family, false);
            
            // 获取Java层的文件描述符fd在本地(native层)的引用值
            this.fdVal = IOUtil.fdVal(fd);
        } catch(IOException ioe) {
            ResourceManager.afterUdpClose();
            throw ioe;
        }
    }
    
    // 构造DatagramSocket通道，内部初始化了该Socket的文件描述符，其支持的协议族由参数family指定
    public DatagramChannelImpl(SelectorProvider provider, ProtocolFamily family) throws IOException {
        super(provider);
        
        Objects.requireNonNull(family, "'family' is null");
        if((family != StandardProtocolFamily.INET) && (family != StandardProtocolFamily.INET6)) {
            throw new UnsupportedOperationException("Protocol family not supported");
        }
        
        if(family == StandardProtocolFamily.INET6) {
            if(!Net.isIPv6Available()) {
                throw new UnsupportedOperationException("IPv6 not available");
            }
        }
        
        ResourceManager.beforeUdpCreate();
        
        try {
            // 记录当前UDP-Socket使用的IP协议
            this.family = family;
            
            // 创建UDP-Socket，并返回其在Java层的文件描述符
            this.fd = Net.socket(family, false);
            
            // 获取Java层的文件描述符fd在本地(native层)的引用值
            this.fdVal = IOUtil.fdVal(fd);
        } catch(IOException ioe) {
            ResourceManager.afterUdpClose();
            throw ioe;
        }
    }
    
    public DatagramChannelImpl(SelectorProvider provider, FileDescriptor fd) throws IOException {
        super(provider);
        
        // increment UDP count to match decrement when closing
        ResourceManager.beforeUdpCreate();
        
        // 获取当前UDP-Socket使用的IP协议
        this.family = Net.isIPv6Available() ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
        
        // 记录UDP-Socket在Java层的文件描述符
        this.fd = fd;
        
        // 获取Java层的文件描述符fd在本地(native层)的引用值
        this.fdVal = IOUtil.fdVal(fd);
        
        synchronized(stateLock) {
            this.localAddress = Net.localAddress(fd);
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 适配 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回由当前UDP-Socket通道适配而成的UDP-Socket
    @Override
    public DatagramSocket socket() {
        synchronized(stateLock) {
            if(socket == null) {
                socket = DatagramSocketAdaptor.create(this);
            }
            
            return socket;
        }
    }
    
    /*▲ 适配 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 对UDP-Socket执行【bind】操作。
     *
     * bindpoint: 待绑定的地址(ip+port)；如果为null，则绑定到本地环回IP和随机端口
     */
    @Override
    public DatagramChannel bind(SocketAddress bindpoint) throws IOException {
        readLock.lock();
        
        try {
            writeLock.lock();
            
            try {
                synchronized(stateLock) {
                    // 确保通道已经开启，否则抛异常
                    ensureOpen();
                    
                    // 如果已经绑定过了，则抛出异常
                    if(localAddress != null) {
                        throw new AlreadyBoundException();
                    }
                    
                    // 执行绑定操作的内部实现
                    bindInternal(bindpoint);
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
     * 对UDP-Socket执行【connect】操作。
     *
     * 将UDP-Socket连接到远程地址，之后只能向该远程地址发送数据，或从该远程地址接收数据。
     *
     * 注：这与TCP-Socket中的绑定意义完全不同，不会经过握手验证
     */
    @Override
    public DatagramChannel connect(SocketAddress sa) throws IOException {
        InetSocketAddress isa = Net.checkAddress(sa, family);
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            InetAddress ia = isa.getAddress();
            if(ia.isMulticastAddress()) {
                sm.checkMulticast(ia);
            } else {
                sm.checkConnect(ia.getHostAddress(), isa.getPort());
                sm.checkAccept(ia.getHostAddress(), isa.getPort());
            }
        }
        
        readLock.lock();
        try {
            writeLock.lock();
            try {
                synchronized(stateLock) {
                    // 确保通道已经开启，否则抛异常
                    ensureOpen();
                    
                    // 如果已经连接，则抛异常
                    if(state == ST_CONNECTED) {
                        throw new AlreadyConnectedException();
                    }
                    
                    // 客户端Socket向指定地址处的指定协议族的远端Socket发起连接（如果采用阻塞模式，会一直等待，直到成功或出现异常）
                    int n = Net.connect(family, fd, isa.getAddress(), isa.getPort());
                    if(n<=0) {
                        throw new Error();      // Can't happen
                    }
                    
                    // 记录连接到的远程地址
                    remoteAddress = isa;
                    
                    // 标记已连接
                    state = ST_CONNECTED;
                    
                    // 刷新(记录)本地地址
                    localAddress = Net.localAddress(fd);
                    
                    // flush any packets already received.
                    boolean blocking = isBlocking();
                    
                    // 如果通道是阻塞的，则将其临时修改为非阻塞
                    if(blocking) {
                        IOUtil.configureBlocking(fd, false);
                    }
                    
                    try {
                        /*
                         * 接收远端的UDP数据包，相当于一个测试连接的过程.
                         * 由于已经设置成了非阻塞模式，所以没有可读数据时立即返回null，退出循环。
                         */
                        ByteBuffer buf = ByteBuffer.allocate(100);
                        while(receive(buf) != null) {
                            buf.clear();
                        }
                    } finally {
                        // 恢复通道为之前的阻塞模式
                        if(blocking) {
                            IOUtil.configureBlocking(fd, true);
                        }
                    }
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
     * 对UDP-Socket执行【disconnect】操作，即断开连接。
     */
    @Override
    public DatagramChannel disconnect() throws IOException {
        readLock.lock();
        
        try {
            writeLock.lock();
            try {
                synchronized(stateLock) {
                    // 只有通道未关闭，且处于连接状态时，才有必要去断开连接
                    if(!isOpen() || (state != ST_CONNECTED)) {
                        return this;
                    }
                    
                    // disconnect socket
                    boolean isIPv6 = (family == StandardProtocolFamily.INET6);
                    
                    // 断开连接的内部实现
                    disconnect0(fd, isIPv6);
                    
                    // 清空远程地址
                    remoteAddress = null;
                    
                    // 标记UDP-Socket为未连接状态
                    state = ST_UNCONNECTED;
                    
                    // 刷新(记录)本地地址
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
     * 对UDP-Socket执行【bind】操作。
     *
     * bindpoint: 待绑定的地址(ip+port)；如果为null，则绑定到本地环回IP和随机端口
     */
    private void bindInternal(SocketAddress bindpoint) throws IOException {
        assert Thread.holdsLock(stateLock) && (localAddress == null);
        
        InetSocketAddress isa;
        
        // bindpoint为null，则绑定到本地环回IP和随机端口
        if(bindpoint == null) {
            // only Inet4Address allowed with IPv4 socket
            if(family == StandardProtocolFamily.INET) {
                isa = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0);
            } else {
                isa = new InetSocketAddress(0);
            }
        } else {
            isa = Net.checkAddress(bindpoint, family);
        }
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkListen(isa.getPort());
        }
        
        // 为指定协议族的Socket绑定IP地址与端口号
        Net.bind(family, fd, isa.getAddress(), isa.getPort());
        
        // 从指定的Socket文件描述符中解析出本地Socket地址后返回
        localAddress = Net.localAddress(fd);
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
        
        boolean blocking;
        boolean interrupted = false;
        
        /* set state to ST_CLOSING and invalid membership keys */
        synchronized(stateLock) {
            assert state<ST_CLOSING;
            
            blocking = isBlocking();
            
            // 标记通道进入关闭状态
            state = ST_CLOSING;
            
            /* if member of any multicast groups then invalidate the keys */
            if(registry != null) {
                registry.invalidateAll();
            }
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
        
        // set state to ST_KILLPENDING
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
    
    // 销毁当前通道，即释放对Socket文件描述符的引用
    @Override
    public void kill() throws IOException {
        synchronized(stateLock) {
            if(state == ST_KILLPENDING) {
                // 通道进入销毁状态
                state = ST_KILLED;
                
                try {
                    // 释放对本地文件描述符的引用
                    nd.close(fd);
                } finally {
                    // notify resource manager
                    ResourceManager.afterUdpClose();
                }
            }
        }
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向target处的远端(目的地)发送src中存储的数据，返回写入的字节数量
    @Override
    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        Objects.requireNonNull(src);
        
        // 获取远程地址
        InetSocketAddress isa = Net.checkAddress(target, family);
        
        writeLock.lock();
        try {
            boolean blocking = isBlocking();
            
            int n = 0;
            
            try {
                /*
                 * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                 * 不必保证已连接，返回远程连接地址。
                 */
                SocketAddress remote = beginWrite(blocking, false);
                
                // 如果存在远程连接地址
                if(remote != null) {
                    // 如果发送的目标地址跟远程连接地址不匹配，则抛异常
                    if(!target.equals(remote)) {
                        throw new AlreadyConnectedException();
                    }
                    
                    do {
                        /*
                         * 从缓冲区src读取，读到的内容向文件描述符fd（关联的socket）中追加后，返回写入的字节数量。
                         * 注：这里只是将数据写入到了本地fd中，因为在connect()中，已经将本地fd与远程建立了联系。
                         * 　　不要忘记，fd在底层代表的是本地Socket。
                         */
                        n = IOUtil.write(fd, src, -1, nd);
                    } while((n == IOStatus.INTERRUPTED) && isOpen());
                    
                    // 未连接
                } else {
                    SecurityManager sm = System.getSecurityManager();
                    if(sm != null) {
                        InetAddress ia = isa.getAddress();
                        if(ia.isMulticastAddress()) {
                            sm.checkMulticast(ia);
                        } else {
                            sm.checkConnect(ia.getHostAddress(), isa.getPort());
                        }
                    }
                    
                    do {
                        // 向isa处的远端(目的地)发送src中存储的数据(一次可能发不完)
                        n = send(fd, src, isa);
                    } while((n == IOStatus.INTERRUPTED) && isOpen());
                }
            } finally {
                /*
                 * 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调；
                 * 对于阻塞通道，还需要唤醒其他阻塞在关闭操作上的线程。
                 */
                endWrite(blocking, n>0);
                assert IOStatus.check(n);
            }
            
            return IOStatus.normalize(n);
        } finally {
            writeLock.unlock();
        }
    }
    
    // 从远端UDP-Socket中接收数据并存入dst，返回实际接收到的字节数
    @Override
    public SocketAddress receive(ByteBuffer dst) throws IOException {
        if(dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        
        readLock.lock();
        
        try {
            boolean blocking = isBlocking();
            
            int n = 0;
            
            ByteBuffer bb = null;
            
            try {
                /*
                 * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
                 * 不必保证已连接，返回远程连接地址。
                 */
                SocketAddress remote = beginRead(blocking, false);
                
                // 如果远程地址不为null，说明远端与当前Socket建立了连接
                boolean connected = (remote != null);
                
                SecurityManager sm = System.getSecurityManager();
                
                // 如果建立了连接，或者存在安全管理器
                if(connected || (sm == null)) {
                    // connected or no security manager
                    do {
                        // 从远端UDP-Socket接收数据并存入dst中，返回实际接收到的字节数；connected指示两端的Socket是否建立了连接
                        n = receive(fd, dst, connected);
                    } while((n == IOStatus.INTERRUPTED) && isOpen());
                    if(n == IOStatus.UNAVAILABLE) {
                        return null;
                    }
                    
                    // 如果没有建立连接，且没有安全管理器
                } else {
                    /* Cannot receive into user's buffer when running with a security manager and not connected */
                    // 获取缓冲区长度（还剩多少空间）
                    int size = dst.remaining();
                    
                    // 获取一块容量至少为size个字节的直接缓冲区
                    bb = Util.getTemporaryDirectBuffer(size);
                    
                    for(; ; ) {
                        do {
                            // 从远端UDP-Socket接收数据并存入bb中，返回实际接收到的字节数；connected指示两端的Socket是否建立了连接
                            n = receive(fd, bb, connected);
                        } while((n == IOStatus.INTERRUPTED) && isOpen());
                        if(n == IOStatus.UNAVAILABLE) {
                            return null;
                        }
                        
                        // 记录接收到的数据的发送端地址
                        InetSocketAddress isa = (InetSocketAddress) sender;
                        try {
                            sm.checkAccept(isa.getAddress().getHostAddress(), isa.getPort());
                        } catch(SecurityException se) {
                            // Ignore packet
                            bb.clear();
                            n = 0;
                            continue;
                        }
                        
                        // 将bb从写模式切换为读模式
                        bb.flip();
                        
                        // 将bb中的数据存入dst
                        dst.put(bb);
                        
                        break;
                    }
                }
                
                assert sender != null;
                
                // 返回接收到的数据的发送端地址，该地址信息由receive0()方法设置
                return sender;
            } finally {
                if(bb != null) {
                    // 采用FILO的形式(入栈模式)将bb放入Buffer缓存池以待复用
                    Util.releaseTemporaryDirectBuffer(bb);
                }
                
                /*
                 * 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调；
                 * 对于阻塞通道，还需要唤醒其他阻塞在关闭操作上的线程。
                 */
                endRead(blocking, n>0);
                assert IOStatus.check(n);
            }
        } finally {
            readLock.unlock();
        }
    }
    
    
    // 向target处的远端(目的地)发送src中存储的数据(一次可能发不完)，返回实际传输的字节数
    private int send(FileDescriptor fd, ByteBuffer src, InetSocketAddress target) throws IOException {
        // 如果src是直接缓冲区，则可以直接调用本地发送方法
        if(src instanceof DirectBuffer) {
            // 通过本地Socket(由fd指代)，将bb中的数据发送到远端target处的UDP-Socket，返回实际传输的字节数
            return sendFromNativeBuffer(fd, src, target);
        }
        
        // Substitute a native buffer
        int pos = src.position();
        int lim = src.limit();
        assert (pos<=lim);
        
        int rem = (pos<=lim ? lim - pos : 0);
        
        // 获取一块容量至少为rem个字节的直接缓冲区
        ByteBuffer bb = Util.getTemporaryDirectBuffer(rem);
        try {
            // 向bb中存入src中的数据
            bb.put(src);
            // 从写模式翻转为读模式
            bb.flip();
            
            /* Do not update src until we see how many bytes were written */
            // 暂时先让src的游标维持原状
            src.position(pos);
            
            // 通过本地Socket(由fd指代)，将bb中的数据发送到远端target处的UDP-Socket，返回实际传输的字节数
            int n = sendFromNativeBuffer(fd, bb, target);
            // 如果成功发送了一部分字节，则同步更新src的游标
            if(n>0) {
                src.position(pos + n);
            }
            
            // 返回实际发送的字节数
            return n;
        } finally {
            // 采用FILO的形式(入栈模式)将bb放入Buffer缓存池以待复用
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }
    
    // 通过本地Socket(由fd指代)，将bb中的数据发送到远端target处的UDP-Socket，返回实际传输的字节数
    private int sendFromNativeBuffer(FileDescriptor fd, ByteBuffer bb, InetSocketAddress target) throws IOException {
        int pos = bb.position();
        int lim = bb.limit();
        assert (pos<=lim);
        
        // 计算有多少待发送的字节
        int rem = (pos<=lim ? lim - pos : 0);
        
        boolean preferIPv6 = (family != StandardProtocolFamily.INET);
        
        int written;
        
        try {
            // 通过本地Socket，将bb中的数据发送到远端target处的UDP-Socket，返回实际传输的字节数
            written = send0(preferIPv6, fd, ((DirectBuffer) bb).address() + pos, rem, target.getAddress(), target.getPort());
        } catch(PortUnreachableException pue) {
            if(isConnected()) {
                throw pue;
            }
            written = rem;
        }
        
        // 将bb中的游标前进，表示跳过已经写入的字节
        if(written>0) {
            bb.position(pos + written);
        }
        
        return written;
    }
    
    // 从远端UDP-Socket接收数据并存入dst中，返回实际接收到的字节数；connected指示两端的Socket是否建立了连接
    private int receive(FileDescriptor fd, ByteBuffer dst, boolean connected) throws IOException {
        int pos = dst.position();
        int lim = dst.limit();
        assert (pos<=lim);
        
        // 计算dst的剩余空间
        int rem = (pos<=lim ? lim - pos : 0);
        
        // 如果dst已经是直接缓冲区，且有剩余空间，则直接接收数据
        if(dst instanceof DirectBuffer && rem>0) {
            // 从远端UDP-Socket接收数据并存入bb中，返回实际接收到的字节数；connected指示两端的Socket是否建立了连接
            return receiveIntoNativeBuffer(fd, dst, rem, pos, connected);
        }
        
        /*
         * Substitute a native buffer.
         * If the supplied buffer is empty we must instead use a nonempty buffer,
         * otherwise the call will not block waiting for a datagram on some platforms.
         */
        // 至少准备拥有一个字节的缓冲区
        int newSize = Math.max(rem, 1);
        
        // 获取一块容量至少为newSize个字节的直接缓冲区
        ByteBuffer bb = Util.getTemporaryDirectBuffer(newSize);
        
        try {
            // 从远端UDP-Socket接收数据并存入bb中，返回实际接收到的字节数；connected指示两端的Socket是否建立了连接
            int n = receiveIntoNativeBuffer(fd, bb, newSize, 0, connected);
            
            // 将bb从写模式切换到读模式
            bb.flip();
            
            // 将bb中的数据存入dst
            if(n>0 && rem>0) {
                dst.put(bb);
            }
            
            // 返回实际接收的字节数
            return n;
        } finally {
            // 采用FILO的形式(入栈模式)将bb放入Buffer缓存池以待复用
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }
    
    // 从远端UDP-Socket接收数据并存入bb中，返回实际接收到的字节数；connected指示两端的Socket是否建立了连接
    private int receiveIntoNativeBuffer(FileDescriptor fd, ByteBuffer bb, int rem, int pos, boolean connected) throws IOException {
        // 从远端UDP-Socket接收数据并存入bb中，返回实际接收到的字节数
        int n = receive0(fd, ((DirectBuffer) bb).address() + pos, rem, connected);
        
        // 更新bb中的游标
        if(n>0) {
            bb.position(pos + n);
        }
        
        return n;
    }
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 从UDP-Socket中读取数据，并存入dst，返回成功读到的字节数；如果是阻塞通道，会在没有可读数据时陷入阻塞
    @Override
    public int read(ByteBuffer dst) throws IOException {
        Objects.requireNonNull(dst);
        
        readLock.lock();
        
        try {
            boolean blocking = isBlocking();
            int n = 0;
            
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；必须保证已连接
                beginRead(blocking, true);
                
                do {
                    // 从文件描述符fd（关联的socket）中起始位置处读取，读到的内容存入dst后，返回读到的字节数量
                    n = IOUtil.read(fd, dst, -1, nd);
                    
                    // 不会理会中断标记，会继续读取
                } while((n == IOStatus.INTERRUPTED) && isOpen());
                
            } finally {
                /*
                 * 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调；
                 * 对于阻塞通道，还需要唤醒其他阻塞在关闭操作上的线程。
                 */
                endRead(blocking, n>0);
                assert IOStatus.check(n);
            }
            
            return IOStatus.normalize(n);
        } finally {
            readLock.unlock();
        }
    }
    
    //【散射】从UDP-Socket中读取数据，并存入dsts中offset处起的length个缓冲区中，返回成功读到的字节数；如果是阻塞通道，会在没有可读数据时陷入阻塞
    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, dsts.length);
        
        readLock.lock();
        try {
            boolean blocking = isBlocking();
            long n = 0;
            
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；必须保证已连接
                beginRead(blocking, true);
                
                do {
                    // 从文件描述符fd（关联的socket）中读取，读到的内容依次存入dsts中offset处起的length个缓冲区中，返回读到的字节数量
                    n = IOUtil.read(fd, dsts, offset, length, nd);
                    
                    // 不会理会中断标记，会继续读取
                } while((n == IOStatus.INTERRUPTED) && isOpen());
                
            } finally {
                /*
                 * 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调；
                 * 对于阻塞通道，还需要唤醒其他阻塞在关闭操作上的线程。
                 */
                endRead(blocking, n>0);
                assert IOStatus.check(n);
            }
            return IOStatus.normalize(n);
        } finally {
            readLock.unlock();
        }
    }
    
    
    // 从src中读取数据，读到的内容向当前UDP-Socket通道中写入
    @Override
    public int write(ByteBuffer src) throws IOException {
        Objects.requireNonNull(src);
        
        writeLock.lock();
        
        try {
            boolean blocking = isBlocking();
            int n = 0;
            
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；必须保证已连接
                beginWrite(blocking, true);
                
                do {
                    // 从缓冲区src读取，读到的内容向文件描述符fd（关联的socket）中追加后，返回写入的字节数量
                    n = IOUtil.write(fd, src, -1, nd);
                } while((n == IOStatus.INTERRUPTED) && isOpen());
            } finally {
                /*
                 * 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调；
                 * 对于阻塞通道，还需要唤醒其他阻塞在关闭操作上的线程。
                 */
                endWrite(blocking, n>0);
                assert IOStatus.check(n);
            }
            return IOStatus.normalize(n);
        } finally {
            writeLock.unlock();
        }
    }
    
    //【聚集】从srcs中offset处起的length个缓冲区读取数据，读到的内容向当前UDP-Socket通道中写入
    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, srcs.length);
        
        writeLock.lock();
        
        try {
            boolean blocking = isBlocking();
            long n = 0;
            
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；必须保证已连接
                beginWrite(blocking, true);
                
                do {
                    // 从srcs[offset, offset+length-1]中各个缓冲区读取数据，读到的内容向文件描述符fd（关联的socket）中起始位置处写入后，返回写入的字节数量
                    n = IOUtil.write(fd, srcs, offset, length, nd);
                } while((n == IOStatus.INTERRUPTED) && isOpen());
            } finally {
                /*
                 * 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调；
                 * 对于阻塞通道，还需要唤醒其他阻塞在关闭操作上的线程。
                 */
                endWrite(blocking, n>0);
                assert IOStatus.check(n);
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
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；不必保证已连接
                beginRead(blocking, false);
                
                // 注册Net.POLLIN事件，远端发来"有可读数据"的消息时，会通知当前Socket
                int events = Net.poll(fd, Net.POLLIN, timeout);
                
                polled = (events != 0);
            } finally {
                /*
                 * 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调；
                 * 对于阻塞通道，还需要唤醒其他阻塞在关闭操作上的线程。
                 */
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
     * 翻译Datagram通道监听的事件，返回对ops的翻译结果
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
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, ski.nioReadyOps(), ski);
    }
    
    /*
     *【覆盖更新】已就绪事件
     *
     * 将本地(native)反馈的就绪信号ops翻译并存储到Java层的就绪事件readyOps中，
     * 返回值指示上一次反馈的事件与本次反馈的事件是否发生了改变。
     *
     * 通道收到有效的反馈事件后，会【覆盖】(selectionKey中)上次记录的已就绪事件，
     * 如果本地(native)反馈了错误或挂起信号，则将已就绪事件直接设置为通道注册的监听事件。
     *
     * 方向：native层 --> Java层
     * 　　　Net.XXX --> SelectionKey.XXX
     *
     * 参见：SelectionKeyImpl#readyOps
     */
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, 0, ski);
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
        
        // 该通道监听了"可读"事件，且通道内有数据可读
        if(((ops & Net.POLLIN) != 0) && ((intOps & SelectionKey.OP_READ) != 0)) {
            newOps |= SelectionKey.OP_READ;
        }
        
        // 该通道监听了"可写"事件，且可以向通道写入数据
        if(((ops & Net.POLLOUT) != 0) && ((intOps & SelectionKey.OP_WRITE) != 0)) {
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
    public <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException {
        Objects.requireNonNull(name);
        
        if(!supportedOptions().contains(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            if(name == StandardSocketOptions.IP_TOS || name == StandardSocketOptions.IP_MULTICAST_TTL || name == StandardSocketOptions.IP_MULTICAST_LOOP) {
                // options are protocol dependent
                Net.setSocketOption(fd, family, name, value);
                return this;
            }
            
            if(name == StandardSocketOptions.IP_MULTICAST_IF) {
                if(value == null) {
                    throw new IllegalArgumentException("Cannot set IP_MULTICAST_IF to 'null'");
                }
                
                NetworkInterface interf = (NetworkInterface) value;
                
                if(family == StandardProtocolFamily.INET6) {
                    int index = interf.getIndex();
                    if(index == -1) {
                        throw new IOException("Network interface cannot be identified");
                    }
                    Net.setInterface6(fd, index);
                } else {
                    // need IPv4 address to identify interface
                    Inet4Address target = Net.anyInet4Address(interf);
                    if(target == null) {
                        throw new IOException("Network interface not configured for IPv4");
                    }
                    // 以int形式返回指定的IP4地址
                    int targetAddress = Net.inet4AsInt(target);
                    Net.setInterface4(fd, targetAddress);
                }
                
                return this;
            }
            
            if(name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind() && localAddress != null) {
                reuseAddressEmulated = true;
                this.isReuseAddress = (Boolean) value;
            }
            
            // remaining options don't need any special handling
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
            
            if(name == StandardSocketOptions.IP_TOS || name == StandardSocketOptions.IP_MULTICAST_TTL || name == StandardSocketOptions.IP_MULTICAST_LOOP) {
                return (T) Net.getSocketOption(fd, family, name);
            }
            
            if(name == StandardSocketOptions.IP_MULTICAST_IF) {
                if(family == StandardProtocolFamily.INET) {
                    int address = Net.getInterface4(fd);
                    if(address == 0) {
                        return null;    // default interface
                    }
                    
                    // 将int形式的IP4地址以InetAddress返回
                    InetAddress ia = Net.inet4FromInt(address);
                    NetworkInterface ni = NetworkInterface.getByInetAddress(ia);
                    if(ni == null) {
                        throw new IOException("Unable to map address to interface");
                    }
                    return (T) ni;
                } else {
                    int index = Net.getInterface6(fd);
                    if(index == 0) {
                        return null;    // default interface
                    }
                    
                    NetworkInterface ni = NetworkInterface.getByIndex(index);
                    if(ni == null) {
                        throw new IOException("Unable to map index to interface");
                    }
                    return (T) ni;
                }
            }
            
            if(name == StandardSocketOptions.SO_REUSEADDR && reuseAddressEmulated) {
                return (T) Boolean.valueOf(isReuseAddress);
            }
            
            // no special handling
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
     * Marks the beginning of a read operation that might block.
     *
     * @param blocking        true if configured blocking
     * @param mustBeConnected true if the socket must be connected
     *
     * @return remote address if connected
     *
     * @throws ClosedChannelException   if the channel is closed
     * @throws NotYetConnectedException if mustBeConnected and not connected
     * @throws IOException              if socket not bound and cannot be bound
     */
    /*
     * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
     * mustBeConnected指示是否必须保证已连接，返回远程连接地址。
     */
    private SocketAddress beginRead(boolean blocking, boolean mustBeConnected) throws IOException {
        // 如果通道处于阻塞模式，则设置一个线程中断回调，以便可以利用"中断"操作来退出阻塞
        if(blocking) {
            begin();
        }
        
        SocketAddress remote;
        
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            remote = remoteAddress;
            if((remote == null) && mustBeConnected) {
                throw new NotYetConnectedException();
            }
            
            if(localAddress == null) {
                bindInternal(null);
            }
            
            if(blocking) {
                // 记录当前通道开始读操作时所在的native线程引用
                readerThread = NativeThread.current();
            }
        }
        
        return remote;
    }
    
    /**
     * Marks the end of a read operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed asynchronously
     */
    /*
     * 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调；
     * 对于阻塞通道，还需要唤醒其他阻塞在关闭操作上的线程。
     */
    private void endRead(boolean blocking, boolean completed) throws AsynchronousCloseException {
        // 如果不是阻塞型通道，直接返回
        if(!blocking) {
            return;
        }
        
        synchronized(stateLock) {
            // 结束读操作，置空线程号
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
     * @param blocking        true if configured blocking
     * @param mustBeConnected true if the socket must be connected
     *
     * @return remote address if connected
     *
     * @throws ClosedChannelException   if the channel is closed
     * @throws NotYetConnectedException if mustBeConnected and not connected
     * @throws IOException              if socket not bound and cannot be bound
     */
    /*
     * 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道；
     * mustBeConnected指示是否必须保证已连接，返回远程连接地址。
     */
    private SocketAddress beginWrite(boolean blocking, boolean mustBeConnected) throws IOException {
        // 如果通道处于阻塞模式，则设置一个线程中断回调，以便可以利用"中断"操作来退出阻塞
        if(blocking) {
            begin();
        }
        
        SocketAddress remote;
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            remote = remoteAddress;
            if((remote == null) && mustBeConnected) {
                throw new NotYetConnectedException();
            }
            
            if(localAddress == null) {
                bindInternal(null);
            }
            
            if(blocking) {
                // 记录当前通道开始写操作时所在的native线程引用
                writerThread = NativeThread.current();
            }
        }
        
        return remote;
    }
    
    /**
     * Marks the end of a write operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed asynchronously
     */
    /*
     * 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调；
     * 对于阻塞通道，还需要唤醒其他阻塞在关闭操作上的线程。
     */
    private void endWrite(boolean blocking, boolean completed) throws AsynchronousCloseException {
        if(!blocking) {
            return;
        }
        
        synchronized(stateLock) {
            // 结束写操作，置空线程号
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
    
    // 是否设置当前通道为阻塞模式
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
    
    // 判断当前通道是否已建立连接
    @Override
    public boolean isConnected() {
        synchronized(stateLock) {
            return (state == ST_CONNECTED);
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
            
            /* Perform security check before returning address */
            // 对指定的地址进行安全校验
            return Net.getRevealedLocalAddress(localAddress);
        }
    }
    
    // 获取绑定的本地地址
    InetSocketAddress localAddress() {
        synchronized(stateLock) {
            return localAddress;
        }
    }
    
    
    // 获取连接的远程地址
    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            return remoteAddress;
        }
    }
    
    // 获取连接的远程地址
    InetSocketAddress remoteAddress() {
        synchronized(stateLock) {
            return remoteAddress;
        }
    }
    
    /*▲ 地址信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 组播操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 将当前组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据；interf是用来接收消息的网络接口(网卡)
     * 注：不会过滤消息
     */
    @Override
    public MembershipKey join(InetAddress group, NetworkInterface interf) throws IOException {
        return innerJoin(group, interf, null);
    }
    
    /*
     * 将当前组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据；interf是用来接收消息的网络接口(网卡)
     * 注：如果source不为null，则会过滤消息，即只处理源地址是source的那些数据包
     */
    @Override
    public MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source) throws IOException {
        Objects.requireNonNull(source);
        return innerJoin(group, interf, source);
    }
    
    
    /**
     * Joins channel's socket to the given group/interface and optional source address.
     */
    /*
     * 将当前组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据；interf是用来接收消息的网络接口(网卡)
     * 注：如果source不为null，则会过滤消息，即只处理源地址是source的那些数据包
     */
    private MembershipKey innerJoin(InetAddress group, NetworkInterface interf, InetAddress source) throws IOException {
        if(!group.isMulticastAddress()) {
            throw new IllegalArgumentException("Group not a multicast address");
        }
        
        /* check multicast address is compatible with this socket */
        // 校验组播小组的地址类型是否与当前UDP-Socket使用的IP协议匹配
        if(group instanceof Inet4Address) {
            if(family == StandardProtocolFamily.INET6 && !Net.canIPv6SocketJoinIPv4Group()) {
                throw new IllegalArgumentException("IPv6 socket cannot join IPv4 multicast group");
            }
        } else if(group instanceof Inet6Address) {
            if(family != StandardProtocolFamily.INET6) {
                throw new IllegalArgumentException("Only IPv6 sockets can join IPv6 multicast group");
            }
        } else {
            throw new IllegalArgumentException("Address type not supported");
        }
        
        /* check source address */
        // 校验过滤地址
        if(source != null) {
            if(source.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Source address is a wildcard address");
            }
            if(source.isMulticastAddress()) {
                throw new IllegalArgumentException("Source address is multicast address");
            }
            if(source.getClass() != group.getClass()) {
                throw new IllegalArgumentException("Source address is different type to group");
            }
        }
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkMulticast(group);
        }
        
        synchronized(stateLock) {
            // 确保通道已经开启，否则抛异常
            ensureOpen();
            
            /* check the registry to see if we are already a member of the group */
            // 如果组播Socket注册表还未初始化，则先构造组播Socket注册表的实例，此时，当前Socket还未注册到组播小组
            if(registry == null) {
                registry = new MembershipRegistry();
                
                // 如果组播Socket注册表已经初始化过了，接下来要检查当前Socket是否已经注册到了组播小组
            } else {
                /* return existing membership key */
                // 检查注册信息，如果当前组播Socket已经在相同的环境下注册过了，则直接返回
                MembershipKey key = registry.checkMembership(group, interf, source);
                if(key != null) {
                    return key;
                }
            }
            
            MembershipKeyImpl key;
            
            // 如果需要支持IP6协议；或者，支持join6()方法处理IP4地址
            if((family == StandardProtocolFamily.INET6) && ((group instanceof Inet6Address) || Net.canJoin6WithIPv4Group())) {
                // 获取网络接口的索引
                int index = interf.getIndex();
                if(index == -1) {
                    throw new IOException("Network interface cannot be identified");
                }
                
                /* need multicast and source address as byte arrays */
                // 组播小组地址
                byte[] groupAddress = Net.inet6AsByteArray(group);
                // 过滤消息的地址
                byte[] sourceAddress = (source == null) ? null : Net.inet6AsByteArray(source);
                
                /* join the group */
                int n = Net.join6(fd, groupAddress, index, sourceAddress);
                if(n == IOStatus.UNAVAILABLE) {
                    throw new UnsupportedOperationException();
                }
                
                // 新建一个组播小组成员，并存储组播小组成员的注册信息
                key = new MembershipKeyImpl.Type6(this, group, interf, source, groupAddress, index, sourceAddress);
                
                // 支持IP4地址
            } else {
                /* need IPv4 address to identify interface */
                // 返回从给定的网络接口上找到的首个绑定的IP4地址
                Inet4Address target = Net.anyInet4Address(interf);
                if(target == null) {
                    throw new IOException("Network interface not configured for IPv4");
                }
                
                // 组播小组地址
                int groupAddress = Net.inet4AsInt(group);
                // 接收消息的地址
                int targetAddress = Net.inet4AsInt(target);
                // 过滤消息的地址
                int sourceAddress = (source == null) ? 0 : Net.inet4AsInt(source);
                
                /* join the group */
                int n = Net.join4(fd, groupAddress, targetAddress, sourceAddress);
                if(n == IOStatus.UNAVAILABLE) {
                    throw new UnsupportedOperationException();
                }
                
                // 新建一个组播小组成员，并存储组播小组成员的注册信息
                key = new MembershipKeyImpl.Type4(this, group, interf, source, groupAddress, targetAddress, sourceAddress);
            }
            
            // 将组播小组成员加入组播注册表
            registry.add(key);
            
            return key;
        }
    }
    
    // 从组播注册表中移除指定的组播小组成员；实际操作是将目标组播Socket从所在的组播小组中移除
    void drop(MembershipKeyImpl key) {
        assert key.channel() == this;
        
        synchronized(stateLock) {
            // 如果组播小组成员key已经无效，则直接返回
            if(!key.isValid()) {
                return;
            }
            
            try {
                if(key instanceof MembershipKeyImpl.Type6) {
                    MembershipKeyImpl.Type6 key6 = (MembershipKeyImpl.Type6) key;
                    Net.drop6(fd, key6.groupAddress(), key6.index(), key6.source());
                } else {
                    MembershipKeyImpl.Type4 key4 = (MembershipKeyImpl.Type4) key;
                    Net.drop4(fd, key4.groupAddress(), key4.interfaceAddress(), key4.source());
                }
            } catch(IOException ioe) {
                // should not happen
                throw new AssertionError(ioe);
            }
            
            // 将组播小组成员key设置为无效
            key.invalidate();
            
            // 移除一条组播小组成员信息
            registry.remove(key);
        }
    }
    
    /**
     * Block datagrams from given source if a memory to receive all datagrams.
     */
    // 屏蔽source处发来的消息，即禁止从source处接收消息；如果该组播小组已经设置了过滤，则抛异常
    void block(MembershipKeyImpl key, InetAddress source) throws IOException {
        assert key.channel() == this;
        assert key.sourceAddress() == null;
        
        synchronized(stateLock) {
            // 如果组播小组成员key已经无效，则直接返回
            if(!key.isValid()) {
                throw new IllegalStateException("key is no longer valid");
            }
            
            if(source.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Source address is a wildcard address");
            }
            
            if(source.isMulticastAddress()) {
                throw new IllegalArgumentException("Source address is multicast address");
            }
            
            if(source.getClass() != key.group().getClass()) {
                throw new IllegalArgumentException("Source address is different type to group");
            }
            
            int n;
            
            if(key instanceof MembershipKeyImpl.Type6) {
                MembershipKeyImpl.Type6 key6 = (MembershipKeyImpl.Type6) key;
                n = Net.block6(fd, key6.groupAddress(), key6.index(), Net.inet6AsByteArray(source));
            } else {
                MembershipKeyImpl.Type4 key4 = (MembershipKeyImpl.Type4) key;
                n = Net.block4(fd, key4.groupAddress(), key4.interfaceAddress(), Net.inet4AsInt(source));
            }
            
            if(n == IOStatus.UNAVAILABLE) {
                // ancient kernel
                throw new UnsupportedOperationException();
            }
        }
    }
    
    /**
     * Unblock given source.
     */
    // 解除对source地址的屏蔽，即允许接收source处的消息
    void unblock(MembershipKeyImpl key, InetAddress source) {
        assert key.channel() == this;
        assert key.sourceAddress() == null;
    
        synchronized(stateLock) {
            // 如果组播小组成员key已经无效，则直接返回
            if(!key.isValid()) {
                throw new IllegalStateException("key is no longer valid");
            }
        
            try {
                if(key instanceof MembershipKeyImpl.Type6) {
                    MembershipKeyImpl.Type6 key6 = (MembershipKeyImpl.Type6) key;
                    Net.unblock6(fd, key6.groupAddress(), key6.index(), Net.inet6AsByteArray(source));
                } else {
                    MembershipKeyImpl.Type4 key4 = (MembershipKeyImpl.Type4) key;
                    Net.unblock4(fd, key4.groupAddress(), key4.interfaceAddress(), Net.inet4AsInt(source));
                }
            } catch(IOException ioe) {
                // should not happen
                throw new AssertionError(ioe);
            }
        }
    }
    
    /*▲ 组播操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回通道在Java层的文件描述符
    public FileDescriptor getFD() {
        return fd;
    }
    
    // 返回通道在本地(native层)的文件描述符
    public int getFDVal() {
        return fdVal;
    }
    
    
    private static native void initIDs();
    
    private static native void disconnect0(FileDescriptor fd, boolean isIPv6) throws IOException;
    
    private native int send0(boolean preferIPv6, FileDescriptor fd, long address, int len, InetAddress addr, int port) throws IOException;
    
    private native int receive0(FileDescriptor fd, long address, int len, boolean connected) throws IOException;
    
    
    @SuppressWarnings("deprecation")
    protected void finalize() throws IOException {
        // fd is null if constructor threw exception
        if(fd != null) {
            close();
        }
    }
    
    
    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();
        
        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>();
            set.add(StandardSocketOptions.SO_SNDBUF);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            if(Net.isReusePortAvailable()) {
                set.add(StandardSocketOptions.SO_REUSEPORT);
            }
            set.add(StandardSocketOptions.SO_BROADCAST);
            set.add(StandardSocketOptions.IP_TOS);
            set.add(StandardSocketOptions.IP_MULTICAST_IF);
            set.add(StandardSocketOptions.IP_MULTICAST_TTL);
            set.add(StandardSocketOptions.IP_MULTICAST_LOOP);
            set.addAll(ExtendedSocketOptions.options(SOCK_DGRAM));
            return Collections.unmodifiableSet(set);
        }
    }
    
}
