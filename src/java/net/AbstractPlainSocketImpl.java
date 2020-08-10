/*
 * Copyright (c) 1995, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import sun.net.ConnectionResetException;
import sun.net.NetHooks;
import sun.net.ResourceManager;
import sun.net.util.SocketExceptions;

/**
 * Default Socket Implementation. This implementation does
 * not implement any security checks.
 * Note this class should <b>NOT</b> be public.
 *
 * @author Steven B. Byrne
 */
// 普通"Socket委托"的抽象实现
abstract class AbstractPlainSocketImpl extends SocketImpl {
    
    public static final int SHUT_RD = 0;    // 关闭输入流标记
    public static final int SHUT_WR = 1;    // 关闭输出流标记
    
    /** whether this Socket is a stream (TCP) socket or not (UDP) */
    protected boolean stream;   // 当前Socket是TCP连接还是UDP连接
    
    /** number of threads using the FileDescriptor */
    protected int fdUseCount = 0;           // Socket文件描述符被引用的次数
    
    /** indicates a close is pending on the file descriptor */
    protected boolean closePending = false; // 表示Socket文件正在等待关闭（处于关闭过程中）
    
    /** instance variable for SO_TIMEOUT */
    int timeout;   // 超时约束，默认为0（详见SO_TIMEOUT参数）
    
    private static volatile boolean checkedReusePort;       // 是否已经检查过isReusePortAvailable字段
    private static volatile boolean isReusePortAvailable;   // 是否允许多个socket监听相同的IP地址和端口
    
    /** indicates connection reset state */
    private volatile boolean connectionReset;   // 连接是否已经重置
    
    // traffic class
    private int trafficClass;           // IP头部的Type-of-Service字段
    
    private boolean shut_rd = false;    // 标记输入流是否已关闭
    private boolean shut_wr = false;    // 标记输出流是否已关闭
    
    private SocketInputStream socketInputStream = null;     // Socket输入流
    private SocketOutputStream socketOutputStream = null;   // Socket输出流
    
    /* lock when increment/decrementing fdUseCount */
    protected final Object fdLock = new Object();
    
    
    /* Load net library into runtime */
    static {
        AccessController.doPrivileged(new PrivilegedAction<>() {
            public Void run() {
                System.loadLibrary("net");
                return null;
            }
        });
    }
    
    
    
    /*▼ 创建 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a socket with a boolean that specifies whether this is a stream socket (true) or an unconnected UDP socket (false).
     */
    /*
     * 创建Socket文件，并记下其文件描述符到fd字段中（该文件描述符会被清理器追踪）
     *
     * stream==true ：创建TCP Socket
     * stream==false：创建UDP Socket
     */
    protected synchronized void create(boolean stream) throws IOException {
        this.stream = stream;
        
        // stream=true则需要创建TCP连接
        if(stream) {
            // 创建Java层的Socket文件描述符中
            fd = new FileDescriptor();
            
            // 创建TCP Socket文件，并将其本地文件描述符存储到Java层的文件描述符中
            socketCreate(true);
            
            // 注册Socket的文件描述符到清理器
            SocketCleanable.register(fd);
            
            // stream=false则需要创建UDP连接
        } else {
            // 创建UDP-Socket之时的回调
            ResourceManager.beforeUdpCreate();
            
            /* only create the fd after we know we will be able to create the socket */
            // 创建Java层的Socket文件描述符中
            fd = new FileDescriptor();
            
            try {
                // 创建UDP Socket文件，并将其本地文件描述符存储到Java层的文件描述符中
                socketCreate(false);
                
                // 注册Socket的文件描述符到清理器
                SocketCleanable.register(fd);
            } catch(IOException ioe) {
                // 关闭UDP-Socket之时的回调
                ResourceManager.afterUdpClose();
                fd = null;
                throw ioe;
            }
        }
        
        // 标记[客户端Socket]/[服务端Socket(通信)]已创建
        if(socket != null) {
            socket.setCreated();
        }
        
        // 标记[服务端Socket(监听)]已创建
        if(serverSocket != null) {
            serverSocket.setCreated();
        }
    }
    
    /*
     * 创建Socket文件，并将其本地文件描述符存储到Java层的文件描述符中
     *
     * stream==true ：创建TCP Socket
     * stream==false：创建UDP Socket
     */
    abstract void socketCreate(boolean stream) throws IOException;
    
    /*▲ 创建 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a socket and connects it to the specified address on
     * the specified port.
     *
     * @param address the address
     * @param timeout the timeout value in milliseconds, or zero for no timeout.
     *
     * @throws IOException              if connection fails
     * @throws IllegalArgumentException if address is null or is a
     *                                  SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    /*
     * 本地Socket向远端Socket发起连接，允许指定超时时间
     *
     *                  本地Socket            远端Socket
     * 1.不存在代理     [客户端Socket]       [服务端Socket(通信)]
     * 2.存在正向代理   [客户端Socket]        代理端Socket
     * 3.存在反向代理   [服务端Socket(通信)]  代理端Socket
     *
     * endpoint: 在情形1和情形2下，该参数为[服务端Socket(通信)]地址；在情形3下，该参数为代理端Socket地址
     * timeout : 超时时间，即允许连接等待的时间
     */
    protected void connect(SocketAddress endpoint, int timeout) throws IOException {
        boolean connected = false;
        
        try {
            if(!(endpoint instanceof InetSocketAddress)) {
                throw new IllegalArgumentException("unsupported address type");
            }
            
            // 记录远端的Socket地址(ip + port)
            InetSocketAddress epoint = (InetSocketAddress) endpoint;
            // 如果该地址还未解析(如域名)，抛异常
            if(epoint.isUnresolved()) {
                throw new UnknownHostException(epoint.getHostName());
            }
            
            // 获取远端的IP地址
            this.port = epoint.getPort();
            // 获取远端的端口号
            this.address = epoint.getAddress();
            
            // 连接到远端
            connectToAddress(this.address, port, timeout);
            
            connected = true;
        } finally {
            if(!connected) {
                try {
                    close();
                } catch(IOException ioe) {
                    // Do nothing. If connect threw an exception then it will be passed up the call stack.
                }
            }
        }
    }
    
    /**
     * Creates a socket and connects it to the specified port on the specified host.
     *
     * @param host the specified host
     * @param port the specified port
     */
    /*
     * [兼容旧式Socket委托]
     *
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；未设置超时
     *
     *                  本地Socket            远端Socket
     * 1.不存在代理     [客户端Socket]       [服务端Socket(通信)]
     * 2.存在正向代理   [客户端Socket]        代理端Socket
     * 3.存在反向代理   [服务端Socket(通信)]  代理端Socket
     *
     * host: 待连接的远端主机名/域名；在情形1和情形2下，该参数为[服务端Socket(通信)]的host；在情形3下，该参数为代理端的host
     * port: 待连接的远端端口
     */
    protected void connect(String host, int port) throws IOException {
        boolean connected = false;
        
        try {
            // 由主机名(域名)解析出IP
            InetAddress address = InetAddress.getByName(host);
            
            // 获取远端的IP地址
            this.address = address;
            // 获取远端的端口号
            this.port = port;
            
            // 连接到远端
            connectToAddress(address, port, timeout);
            
            // 标记为已连接
            connected = true;
        } finally {
            if(!connected) {
                try {
                    close();
                } catch(IOException ioe) {
                    // Do nothing. If connect threw an exception then it will be passed up the call stack
                }
            }
        }
    }
    
    /**
     * Creates a socket and connects it to the specified address on
     * the specified port.
     *
     * @param address the address
     * @param port    the specified port
     */
    /*
     * [兼容旧式Socket委托]
     *
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；未设置超时
     *
     *                  本地Socket            远端Socket
     * 1.不存在代理     [客户端Socket]       [服务端Socket(通信)]
     * 2.存在正向代理   [客户端Socket]        代理端Socket
     * 3.存在反向代理   [服务端Socket(通信)]  代理端Socket
     *
     * address: 待连接的远端IP；在情形1和情形2下，该参数为[服务端Socket(通信)]的IP；在情形3下，该参数为代理端的IP
     * port   : 待连接的远端端口
     */
    protected void connect(InetAddress address, int port) throws IOException {
        // 获取远端的IP地址
        this.address = address;
        // 获取远端的端口号
        this.port = port;
        
        try {
            // 连接到远端
            connectToAddress(address, port, timeout);
        } catch(IOException e) {
            // everything failed
            close();
            throw e;
        }
    }
    
    /*
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；允许指定超时，以便等待远端就绪。
     *
     *                  本地Socket            远端Socket
     * 1.不存在代理     [客户端Socket]       [服务端Socket(通信)]
     * 2.存在正向代理   [客户端Socket]        代理端Socket
     * 3.存在反向代理   [服务端Socket(通信)]  代理端Socket
     *
     * address: 远端IP；在情形1和情形2下，该参数为[服务端Socket(通信)]IP；在情形3下，该参数为代理端IP
     * port   : 远端端口
     * timeout: 超时时间，即允许连接等待的时间
     */
    private void connectToAddress(InetAddress address, int port, int timeout) throws IOException {
        // 如果远端地址是一个通配地址(比如未指定远端地址的情形下)
        if(address.isAnyLocalAddress()) {
            // 获取本地主机IP
            InetAddress localHost = InetAddress.getLocalHost();
            // 默认连接到本机IP
            doConnect(localHost, port, timeout);
        } else {
            // 连接到指定的远端IP
            doConnect(address, port, timeout);
        }
    }
    
    /**
     * The workhorse of the connection operation.
     * Tries several times to establish a connection to the given <host, port>.
     * If unsuccessful, throws an IOException indicating what went wrong.
     */
    /*
     * 本地Socket向远端Socket发起连接，允许指定超时时间
     *
     *                  本地Socket            远端Socket
     * 1.不存在代理     [客户端Socket]       [服务端Socket(通信)]
     * 2.存在正向代理   [客户端Socket]        代理端Socket
     * 3.存在反向代理   [服务端Socket(通信)]  代理端Socket
     *
     * address: 远端IP；在情形1和情形2下，该参数为[服务端Socket(通信)]IP；在情形3下，该参数为代理端IP
     * port   : 远端端口
     * timeout: 超时时间，即允许连接等待的时间
     *
     * 注：由于上一级的调用可能未明确设置address，此时address是本地IP
     */
    synchronized void doConnect(InetAddress address, int port, int timeout) throws IOException {
        synchronized(fdLock) {
            // 如果socket连接还未关闭
            if(!closePending) {
                // 如果当前socket还未绑定
                if(socket == null || !socket.isBound()) {
                    // 执行Socket进行connect操作之前的回调
                    NetHooks.beforeTcpConnect(fd, address, port);
                    
                    // 注：如果自行提前进行了绑定，则不会执行此回调
                }
            }
        }
        
        try {
            // Socket文件引用次数增一
            acquireFD();
            
            try {
                // 本地Socket向远端Socket发起连接，允许指定超时时间
                socketConnect(address, port, timeout);
                
                /* socket may have been closed during poll/select */
                synchronized(fdLock) {
                    if(closePending) {
                        throw new SocketException("Socket closed");
                    }
                }
                
                /*
                 * If we have a ref. to the Socket, then sets the flags created, bound & connected to true.
                 * This is normally done in Socket.connect() but some subclasses of Socket may call impl.connect() directly!
                 */
                // 标记本地Socket已绑定和已连接
                if(socket != null) {
                    socket.setBound();
                    socket.setConnected();
                }
            } finally {
                // Socket文件引用次数减一
                releaseFD();
            }
        } catch(IOException e) {
            close();
            throw SocketExceptions.of(e, new InetSocketAddress(address, port));
        }
    }
    
    /*
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；允许指定超时，以便等待远端就绪。
     *
     *                  本地Socket            远端Socket
     * 1.不存在代理     [客户端Socket]       [服务端Socket(通信)]
     * 2.存在正向代理   [客户端Socket]        代理端Socket
     * 3.存在反向代理   [服务端Socket(通信)]  代理端Socket
     *
     * address: 远端IP；在情形1和情形2下，该参数为[服务端Socket(通信)]IP；在情形3下，该参数为代理端IP
     * port   : 远端端口
     * timeout: 超时时间，即允许连接等待的时间
     */
    abstract void socketConnect(InetAddress address, int port, int timeout) throws IOException;
    
    
    /**
     * Binds the socket to the specified address of the specified local port.
     *
     * @param address the address
     * @param lport   the port
     */
    /*
     * 通过当前"Socket委托"为引用的Socket绑定IP与端口号
     *
     * [客户端Socket]：需要绑定到客户端的IP和port上；
     * [服务端Socket]：需要绑定到服务端的IP和port上；
     */
    protected synchronized void bind(InetAddress address, int lport) throws IOException {
        synchronized(fdLock) {
            if(!closePending && (socket == null || !socket.isBound())) {
                // Socket进行bind操作之前的回调
                NetHooks.beforeTcpBind(fd, address, lport);
            }
        }
        
        // 通过当前"Socket委托"为引用的Socket绑定IP与端口号
        socketBind(address, lport);
        
        // 标记[客户端Socket]已绑定
        if(socket != null) {
            socket.setBound();
        }
        
        // 标记[服务端Socket(监听)]已绑定
        if(serverSocket != null) {
            serverSocket.setBound();
        }
    }
    
    // 通过当前"Socket委托"为引用的Socket绑定IP与端口号
    abstract void socketBind(InetAddress address, int port) throws IOException;
    
    
    /**
     * Listens, for a specified amount of time, for connections.
     *
     * @param count the amount of time to listen for connections
     */
    // 通过当前"Socket委托"为引用的[服务端Socket(监听)]开启监听，backlog代表允许积压的待处理连接数
    protected synchronized void listen(int backlog) throws IOException {
        socketListen(backlog);
    }
    
    // 通过当前"Socket委托"为引用的[服务端Socket(监听)]开启监听，backlog代表允许积压的待处理连接数
    abstract void socketListen(int backlog) throws IOException;
    
    
    /**
     * Accepts connections.
     *
     * @param s the connection
     */
    /*
     * 由ServerSocket的"Socket委托"调用，对[服务端Socket(监听)]执行【accept】操作。
     *
     * accept成功后，会获取到新生成的[服务端Socket(通信)]的文件描述符，
     * 随后，将客户端端口/客户端地址/服务端端口/[服务端Socket(通信)]文件描述符设置到参数中的"Socket委托"中。
     *
     * socketImpl: [服务端Socket(通信)]的"Socket委托"，
     *             accept成功后会为其填充相关的地址信息，以及为其关联[服务端Socket(通信)](的文件描述符)
     */
    protected void accept(SocketImpl socketImpl) throws IOException {
        // Socket文件描述符引用次数增一
        acquireFD();
        try {
            // 由ServerSocket的"Socket委托"调用，对[服务端Socket(监听)]执行【accept】操作
            socketAccept(socketImpl);
        } finally {
            // Socket文件描述符引用次数减一
            releaseFD();
        }
    }
    
    /*
     * 由ServerSocket的"Socket委托"调用，对[服务端Socket(监听)]执行【accept】操作。
     *
     * accept成功后，会获取到新生成的[服务端Socket(通信)]的文件描述符，
     * 随后，将客户端端口/客户端地址/服务端端口/[服务端Socket(通信)]文件描述符设置到参数中的"Socket委托"中。
     *
     * socketImpl: [服务端Socket(通信)]的"Socket委托"，
     *             accept成功后会为其填充相关的地址信息，以及为其关联[服务端Socket(通信)](的文件描述符)
     */
    abstract void socketAccept(SocketImpl socketImpl) throws IOException;
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets an InputStream for this socket.
     */
    // 获取Socket输入流，从中读取数据
    protected synchronized InputStream getInputStream() throws IOException {
        synchronized(fdLock) {
            if(isClosedOrPending()) {
                throw new IOException("Socket Closed");
            }
            
            if(shut_rd) {
                throw new IOException("Socket input is shutdown");
            }
            
            if(socketInputStream == null) {
                socketInputStream = new SocketInputStream(this);
            }
        }
        
        return socketInputStream;
    }
    
    /**
     * Gets an OutputStream for this socket.
     */
    // 获取Socket输出流，向其写入数据
    protected synchronized OutputStream getOutputStream() throws IOException {
        synchronized(fdLock) {
            if(isClosedOrPending()) {
                throw new IOException("Socket Closed");
            }
        
            if(shut_wr) {
                throw new IOException("Socket output is shutdown");
            }
        
            if(socketOutputStream == null) {
                socketOutputStream = new SocketOutputStream(this);
            }
        }
    
        return socketOutputStream;
    }
    
    // 设置Socket输入流
    void setInputStream(SocketInputStream in) {
        socketInputStream = in;
    }
    
    
    /**
     * Returns the number of bytes that can be read without blocking.
     */
    // 获取在非阻塞下socket中剩余可读字节数
    protected synchronized int available() throws IOException {
        if(isClosedOrPending()) {
            throw new IOException("Stream closed.");
        }
        
        /*
         * If connection has been reset or shut down for input, then return 0
         * to indicate there are no buffered bytes.
         */
        if(isConnectionReset() || shut_rd) {
            return 0;
        }
        
        /*
         * If no bytes available and we were previously notified
         * of a connection reset then we move to the reset state.
         *
         * If are notified of a connection reset then check
         * again if there are bytes buffered on the socket.
         */
        int n = 0;
        try {
            n = socketAvailable();
        } catch(ConnectionResetException e) {
            // 设置连接已重置
            setConnectionReset();
        }
        return n;
    }
    
    // 获取在非阻塞下socket中剩余可读字节数
    abstract int socketAvailable() throws IOException;
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes the socket.
     */
    // 关闭socket连接
    protected void close() throws IOException {
        synchronized(fdLock) {
            if(fd == null) {
                return;
            }
            
            if(!stream) {
                ResourceManager.afterUdpClose();
            }
            
            // 如果Socket处于空闲状态
            if(fdUseCount == 0) {
                if(closePending) {
                    return;
                }
                
                closePending = true;
                
                /*
                 * We close the FileDescriptor in two-steps :
                 * first the "pre-close" which closes the socket but doesn't release the underlying file descriptor.
                 * This operation may be lengthy due to untransmitted data and a long linger interval.
                 * Once the pre-close is done we do the actual socket to release the fd.
                 */
                try {
                    // 通过复制文件描述符“预关闭”套接字，这使得套接字可以在不释放文件描述符的情况下关闭
                    socketPreClose();
                } finally {
                    // 关闭socket流，释放文件描述符
                    socketClose();
                }
                
                fd = null;
                
                return;
            }
            
            /*
             * If a thread has acquired the fd and a close
             * isn't pending then use a deferred close.
             * Also decrement fdUseCount to signal the last
             * thread that releases the fd to close it.
             */
            if(!closePending) {
                closePending = true;
                fdUseCount--;
                // 通过复制文件描述符“预关闭”套接字，这使得套接字可以在不释放文件描述符的情况下关闭
                socketPreClose();
            }
        }
    }
    
    /**
     * Close the socket (and release the file descriptor).
     */
    // 关闭socket流，释放文件描述符
    protected void socketClose() throws IOException {
        SocketCleanable.unregister(fd);
        socketClose0(false);
    }
    
    /**
     * "Pre-close" a socket by dup'ing the file descriptor :
     * this enables the socket to be closed without releasing the file descriptor.
     */
    // 通过复制文件描述符“预关闭”套接字，这使得套接字可以在不释放文件描述符的情况下关闭
    private void socketPreClose() throws IOException {
        socketClose0(true);
    }
    
    // 关闭Socket
    abstract void socketClose0(boolean useDeferredClose) throws IOException;
    
    
    /**
     * Shutdown read-half of the socket connection;
     */
    // 关闭读取功能，后续的read()会返回-1
    protected void shutdownInput() throws IOException {
        if(fd == null) {
            return;
        }
        
        socketShutdown(SHUT_RD);
        
        if(socketInputStream != null) {
            // 标记输入流已关闭
            socketInputStream.setEOF(true);
        }
        
        shut_rd = true;
    }
    
    /**
     * Shutdown write-half of the socket connection;
     */
    // 关闭输入功能，后续的write()会抛出异常
    protected void shutdownOutput() throws IOException {
        if(fd == null) {
            return;
        }
        
        socketShutdown(SHUT_WR);
        
        shut_wr = true;
    }
    
    // 关闭Socket的输入/输出功能
    abstract void socketShutdown(int howto) throws IOException;
    
    
    // 重置：包括关闭socket流和重置连接状态
    void reset() throws IOException {
        if(fd != null) {
            // 关闭socket流，释放文件描述符
            socketClose();
        }
        
        fd = null;
        
        // 重置连接信息
        super.reset();
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Return the current value of SO_TIMEOUT
     */
    // 获取超时约束值
    public int getTimeout() {
        return timeout;
    }
    
    // 设置Socket-远程IP地址或ServerSocket-本地IP地址
    void setAddress(InetAddress address) {
        this.address = address;
    }
    
    // 设置远程端口
    void setPort(int port) {
        this.port = port;
    }
    
    // 设置本地端口
    void setLocalPort(int localport) {
        this.localport = localport;
    }
    
    // 设置Socket文件的文件描述符
    void setFileDescriptor(FileDescriptor fd) {
        this.fd = fd;
    }
    
    /**
     * "Acquires" and returns the FileDescriptor for this impl
     *
     * A corresponding releaseFD is required to "release" the FileDescriptor.
     */
    // 返回Socket文件的文件描述符，并将其引用次数增一
    FileDescriptor acquireFD() {
        synchronized(fdLock) {
            fdUseCount++;
            return fd;
        }
    }
    
    /**
     * "Release" the FileDescriptor for this impl.
     *
     * If the use count goes to -1 then the socket is closed.
     */
    // Socket文件引用次数减一
    void releaseFD() {
        synchronized(fdLock) {
            fdUseCount--;
            if(fdUseCount == -1) {
                if(fd != null) {
                    try {
                        // 关闭socket流，释放文件描述符
                        socketClose();
                    } catch(IOException e) {
                        //
                    } finally {
                        fd = null;
                    }
                }
            }
        }
    }
    
    /**
     * Return true if already closed or close is pending
     */
    // 判断Socket文件是否正在等待关闭（处于关闭过程中）
    public boolean isClosedOrPending() {
        /*
         * Lock on fdLock to ensure that we wait if a
         * close is in progress.
         */
        synchronized(fdLock) {
            return closePending || (fd == null);
        }
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a set of SocketOptions supported by this impl and by this impl's
     * socket (Socket or ServerSocket)
     *
     * @return a Set of SocketOptions
     */
    // 获取Socket或ServerSocket支持的配置参数选项
    @Override
    protected Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> options;
        
        Set<SocketOption<?>> optionSet = super.supportedOptions();
        
        // 如果允许多个socket监听相同的IP地址和端口（复用）
        if(isReusePortAvailable()) {
            options = new HashSet<>(optionSet);
            options.add(StandardSocketOptions.SO_REUSEPORT);
            options = Collections.unmodifiableSet(options);
        } else {
            options = optionSet;
        }
        
        return options;
    }
    
    // 获取Socket配置参数，参数列表参见SocketOptions
    public Object getOption(int opt) throws SocketException {
        if(isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }
        
        if(opt == SO_TIMEOUT) {
            return timeout;
        }
        
        
        /*
         * The native socketGetOption() knows about 3 options.
         * The 32 bit value it returns will be interpreted according
         * to what we're asking.  A return of -1 means it understands
         * the option but its turned off.  It will raise a SocketException
         * if "opt" isn't one it understands.
         */
        
        int ret = 0;
        
        switch(opt) {
            case TCP_NODELAY:
                ret = socketGetOption(opt, null);
                return ret != -1;
            case SO_OOBINLINE:
                ret = socketGetOption(opt, null);
                return ret != -1;
            case SO_LINGER:
                ret = socketGetOption(opt, null);
                return (ret == -1) ? Boolean.FALSE : (Object) (ret);
            case SO_REUSEADDR:
                ret = socketGetOption(opt, null);
                return ret != -1;
            case SO_BINDADDR:
                InetAddressContainer in = new InetAddressContainer();
                ret = socketGetOption(opt, in);
                return in.addr;
            case SO_SNDBUF:
            case SO_RCVBUF:
                ret = socketGetOption(opt, null);
                return ret;
            case IP_TOS:
                try {
                    ret = socketGetOption(opt, null);
                    if(ret == -1) { // ipv6 tos
                        return trafficClass;
                    } else {
                        return ret;
                    }
                } catch(SocketException se) {
                    // TODO - should make better effort to read TOS or TCLASS
                    return trafficClass; // ipv6 tos
                }
            case SO_KEEPALIVE:
                ret = socketGetOption(opt, null);
                return ret != -1;
            case SO_REUSEPORT:
                if(!supportedOptions().contains(StandardSocketOptions.SO_REUSEPORT)) {
                    throw new UnsupportedOperationException("unsupported option");
                }
                ret = socketGetOption(opt, null);
                return ret != -1;
            // should never get here
            default:
                return null;
        }
    }
    
    // 设置Socket配置参数，参数列表参见SocketOptions
    public void setOption(int opt, Object val) throws SocketException {
        if(isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }
        
        boolean on = true;
        
        switch(opt) {
            /* check type safety b4 going native.  These should never
             * fail, since only java.Socket* has access to
             * PlainSocketImpl.setOption().
             */
            case SO_LINGER:
                if(val == null || (!(val instanceof Integer) && !(val instanceof Boolean))) {
                    throw new SocketException("Bad parameter for option");
                }
                if(val instanceof Boolean) {
                    /* true only if disabling - enabling should be Integer */
                    on = false;
                }
                break;
            case SO_TIMEOUT:
                if(val == null || (!(val instanceof Integer))) {
                    throw new SocketException("Bad parameter for SO_TIMEOUT");
                }
                int tmp = (Integer) val;
                if(tmp<0) {
                    throw new IllegalArgumentException("timeout < 0");
                }
                timeout = tmp;
                break;
            case IP_TOS:
                if(val == null || !(val instanceof Integer)) {
                    throw new SocketException("bad argument for IP_TOS");
                }
                trafficClass = (Integer) val;
                break;
            case SO_BINDADDR:
                throw new SocketException("Cannot re-bind socket");
            case TCP_NODELAY:
                if(val == null || !(val instanceof Boolean))
                    throw new SocketException("bad parameter for TCP_NODELAY");
                on = (Boolean) val;
                break;
            case SO_SNDBUF:
            case SO_RCVBUF:
                if(val == null || !(val instanceof Integer) || !(((Integer) val).intValue()>0)) {
                    throw new SocketException("bad parameter for SO_SNDBUF " + "or SO_RCVBUF");
                }
                break;
            case SO_KEEPALIVE:
                if(val == null || !(val instanceof Boolean)) {
                    throw new SocketException("bad parameter for SO_KEEPALIVE");
                }
                on = (Boolean) val;
                break;
            case SO_OOBINLINE:
                if(val == null || !(val instanceof Boolean)) {
                    throw new SocketException("bad parameter for SO_OOBINLINE");
                }
                on = (Boolean) val;
                break;
            case SO_REUSEADDR:
                if(val == null || !(val instanceof Boolean)) {
                    throw new SocketException("bad parameter for SO_REUSEADDR");
                }
                on = (Boolean) val;
                break;
            case SO_REUSEPORT:
                if(val == null || !(val instanceof Boolean)) {
                    throw new SocketException("bad parameter for SO_REUSEPORT");
                }
                if(!supportedOptions().contains(StandardSocketOptions.SO_REUSEPORT)) {
                    throw new UnsupportedOperationException("unsupported option");
                }
                on = (Boolean) val;
                break;
            default:
                throw new SocketException("unrecognized TCP option: " + opt);
        }
        
        socketSetOption(opt, on, val);
    }
    
    // 获取Socket配置参数，参数列表参见SocketOptions
    abstract int socketGetOption(int opt, Object iaContainerObj) throws SocketException;
    
    // 设置Socket配置参数，参数列表参见SocketOptions
    abstract void socketSetOption(int cmd, boolean on, Object value) throws SocketException;
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 是否支持客户端Socket发送"紧急数据"，必须由子类覆盖
    protected boolean supportsUrgentData() {
        return true;
    }
    
    // 发送一个字节的"紧急数据"，参见SocketOptions#SO_OOBINLINE参数
    protected void sendUrgentData(int data) throws IOException {
        if(fd == null) {
            throw new IOException("Socket Closed");
        }
        socketSendUrgentData(data);
    }
    
    // 发送一个字节的"紧急数据"，参见SocketOptions#SO_OOBINLINE参数
    abstract void socketSendUrgentData(int data) throws IOException;
    
    
    /**
     * Tells whether SO_REUSEPORT is supported.
     */
    // 是否允许多个socket监听相同的IP地址和端口（复用）
    static boolean isReusePortAvailable() {
        if(!checkedReusePort) {
            isReusePortAvailable = isReusePortAvailable0();
            checkedReusePort = true;
        }
        
        return isReusePortAvailable;
    }
    
    // 是否允许多个socket监听相同的IP地址和端口（复用）
    private static native boolean isReusePortAvailable0();
    
    
    // 判断连接是否已重置
    boolean isConnectionReset() {
        return connectionReset;
    }
    
    // 设置连接已重置
    void setConnectionReset() {
        connectionReset = true;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
