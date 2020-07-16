/*
 * Copyright (c) 1995, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Set;

/**
 * The abstract class {@code SocketImpl} is a common superclass of all classes that actually implement sockets.
 * It is used to create both client and server sockets.
 *
 * A "plain" socket implements these methods exactly as described, without attempting to go through a firewall or proxy.
 *
 * @author unascribed
 * @since 1.0
 */
/*
 * "Socket委托"，这相当于Socket的实现类，是真正实现通信的类。
 *
 * 无论是Socket还是ServerSocket，均持有一个"Socket委托"，以便完成客户端与服务端的通信工作。
 */
public abstract class SocketImpl implements SocketOptions {
    
    // [客户端Socket]/[服务端Socket(通信)]
    Socket socket = null;
    
    // [服务端Socket(监听)]的"间接"包装
    ServerSocket serverSocket = null;
    
    /**
     * The file descriptor object for this socket.
     */
    /*
     * 被当前"Socket委托"引用的Socket的文件描述符，
     * 被引用的Socket包括了[客户端Socket]、[服务端Socket(监听)]、[服务端Socket(通信)]。
     */
    protected FileDescriptor fd;
    
    /**
     * The IP address of the remote end of this socket.
     */
    /*
     * 本地/远程地址
     *
     * [客户端Socket] 　　 : 远程地址，即服务端地址；如果主动为[客户端Socket]绑定了本地地址，则此字段先被设置为本地地址，之后在连接中被覆盖为远程地址
     * [服务端Socket(监听)]: 本地地址，即服务端地址
     * [服务端Socket(通信)]: 远程地址，即客户端地址
     *
     * 特殊情形：存在反向代理时，该字段会存储代理端(远程)的地址
     */
    protected InetAddress address;
    
    /**
     * The port number on the remote host to which this socket is connected.
     */
    /*
     * 远程端口
     *
     * [客户端Socket] 　　 : 远程端口，即服务端的端口
     * [服务端Socket(监听)]: 未设置
     * [服务端Socket(通信)]: 远程端口，即客户端的端口
     *
     * 特殊情形：存在反向代理时，该字段会存储代理端(远程)的端口
     */
    protected int port;
    
    /**
     * The local port number to which this socket is connected.
     */
    /*
     * 本地端口
     *
     * [客户端Socket] 　　 : 本地端口，即客户端的端口
     * [服务端Socket(监听)]: 本地端口，即服务端的端口
     * [服务端Socket(通信)]: 本地端口，即服务端的端口
     *
     * 特殊情形：存在反向代理时，该字段依然存储的是本地端口，即服务端的端口
     */
    protected int localport;
    
    private static final Set<SocketOption<?>> socketOptions;        // Socket配置参数
    private static final Set<SocketOption<?>> serverSocketOptions;  // ServerSocket配置参数
    
    
    // 静态加载客户端与服务端可选的Socket配置参数
    static {
        socketOptions = Set.of(StandardSocketOptions.SO_KEEPALIVE, StandardSocketOptions.SO_SNDBUF, StandardSocketOptions.SO_RCVBUF, StandardSocketOptions.SO_REUSEADDR, StandardSocketOptions.SO_LINGER, StandardSocketOptions.IP_TOS, StandardSocketOptions.TCP_NODELAY);
        
        serverSocketOptions = Set.of(StandardSocketOptions.SO_RCVBUF, StandardSocketOptions.SO_REUSEADDR, StandardSocketOptions.IP_TOS);
    }
    
    
    
    /*▼ 创建 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates either a stream or a datagram socket.
     *
     * @param stream if {@code true}, create a stream socket; otherwise, create a datagram socket.
     *
     * @throws IOException if an I/O error occurs while creating the socket.
     */
    /*
     * 创建Socket文件，并记下其文件描述符（该文件描述符会被清理器追踪）
     *
     * stream==true ：创建TCP Socket
     * stream==false：创建UDP Socket
     */
    protected abstract void create(boolean stream) throws IOException;
    
    /*▲ 创建 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Connects this socket to the specified port number on the specified host.
     * A timeout of zero is interpreted as an infinite timeout. The connection
     * will then block until established or an error occurs.
     *
     * @param address the Socket address of the remote host.
     * @param timeout the timeout value, in milliseconds, or zero for no timeout.
     *
     * @throws IOException if an I/O error occurs when attempting a
     *                     connection.
     * @since 1.4
     */
    /*
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；允许指定超时时间，以便等待远端就绪。
     *
     * endpoint: 待连接的远端地址，其含义依子类实现而定
     * timeout : 超时时间，即允许连接等待的时间
     */
    protected abstract void connect(SocketAddress endpoint, int timeout) throws IOException;
    
    /**
     * Connects this socket to the specified port on the named host.
     *
     * @param host the name of the remote host.
     * @param port the port number.
     *
     * @throws IOException if an I/O error occurs when connecting to the
     *                     remote host.
     */
    /*
     * [兼容旧式Socket委托]
     *
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；未设置超时
     *
     * host: 待连接的远端主机名/域名，其含义依子类实现而定
     * port: 待连接的远端端口
     */
    protected abstract void connect(String host, int port) throws IOException;
    
    /**
     * Connects this socket to the specified port number on the specified host.
     *
     * @param address the IP address of the remote host.
     * @param port    the port number.
     *
     * @throws IOException if an I/O error occurs when attempting a
     *                     connection.
     */
    /*
     * [兼容旧式Socket委托]
     *
     * 对本地Socket执行【connect】操作，以便连接到远端Socket；未设置超时
     *
     * address: 待连接的远端IP，其含义依子类实现而定
     * port   : 待连接的远端端口
     */
    protected abstract void connect(InetAddress address, int port) throws IOException;
    
    
    /**
     * Binds this socket to the specified local IP address and port number.
     *
     * @param host an IP address that belongs to a local interface.
     * @param port the port number.
     *
     * @throws IOException if an I/O error occurs when binding this socket.
     */
    /*
     * 通过当前"Socket委托"为引用的Socket绑定IP与端口号
     *
     * [客户端Socket]：需要绑定到客户端的IP和port上；
     * [服务端Socket]：需要绑定到服务端的IP和port上；
     */
    protected abstract void bind(InetAddress host, int port) throws IOException;
    
    
    /**
     * Sets the maximum queue length for incoming connection indications (a request to connect) to the {@code count} argument.
     * If a connection indication arrives when the queue is full, the connection is refused.
     *
     * @param backlog the maximum length of the queue.
     *
     * @throws IOException if an I/O error occurs when creating the queue.
     */
    // 通过当前"Socket委托"为引用的[服务端Socket(监听)]开启监听，backlog代表允许积压的待处理连接数
    protected abstract void listen(int backlog) throws IOException;
    
    
    /**
     * Accepts a connection.
     *
     * @param s the accepted connection.
     *
     * @throws IOException if an I/O error occurs when accepting the connection.
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
    protected abstract void accept(SocketImpl socketImpl) throws IOException;
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an input stream for this socket.
     *
     * @return a stream for reading from this socket.
     *
     * @throws IOException if an I/O error occurs when creating the input stream.
     */
    // 获取Socket输入流，从中读取数据
    protected abstract InputStream getInputStream() throws IOException;
    
    /**
     * Returns an output stream for this socket.
     *
     * @return an output stream for writing to this socket.
     *
     * @throws IOException if an I/O error occurs when creating the output stream.
     */
    // 获取Socket输出流，向其写入数据
    protected abstract OutputStream getOutputStream() throws IOException;
    
    /**
     * Returns the number of bytes that can be read from this socket without blocking.
     *
     * @return the number of bytes that can be read from this socket without blocking.
     *
     * @throws IOException if an I/O error occurs when determining the number of bytes available.
     */
    // 获取在非阻塞下socket中剩余可读字节数
    protected abstract int available() throws IOException;
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes this socket.
     *
     * @throws IOException if an I/O error occurs when closing this socket.
     */
    // 关闭socket连接
    protected abstract void close() throws IOException;
    
    /**
     * Places the input stream for this socket at "end of stream".
     * Any data sent to this socket is acknowledged and then
     * silently discarded.
     *
     * If you read from a socket input stream after invoking this method on the
     * socket, the stream's {@code available} method will return 0, and its
     * {@code read} methods will return {@code -1} (end of stream).
     *
     * @throws IOException if an I/O error occurs when shutting down this
     *                     socket.
     * @see java.net.Socket#shutdownOutput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @since 1.3
     */
    // 关闭读取功能，后续的read()会返回-1
    protected void shutdownInput() throws IOException {
        throw new IOException("Method not implemented!");
    }
    
    /**
     * Disables the output stream for this socket.
     * For a TCP socket, any previously written data will be sent
     * followed by TCP's normal connection termination sequence.
     *
     * If you write to a socket output stream after invoking
     * shutdownOutput() on the socket, the stream will throw
     * an IOException.
     *
     * @throws IOException if an I/O error occurs when shutting down this socket.
     * @see java.net.Socket#shutdownInput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @since 1.3
     */
    // 关闭输入功能，后续的write()会抛出异常
    protected void shutdownOutput() throws IOException {
        throw new IOException("Method not implemented!");
    }
    
    // 重置连接信息
    void reset() throws IOException {
        address = null;
        port = 0;
        localport = 0;
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the value of this socket's {@code address} field.
     *
     * @return the value of this socket's {@code address} field.
     *
     * @see java.net.SocketImpl#address
     */
    /*
     * 返回本地/远程IP
     *
     * [客户端Socket] 　　 : 远程IP，即服务端IP；如果主动为[客户端Socket]绑定了本地IP，则此字段先被设置为本地IP，之后在连接中被覆盖为远程IP
     * [服务端Socket(监听)]: 本地IP，即服务端IP
     * [服务端Socket(通信)]: 远程IP，即客户端IP
     *
     * 特殊情形：存在反向代理时，该字段会存储代理端(远程)的地址
     */
    protected InetAddress getInetAddress() {
        return address;
    }
    
    /**
     * Returns the value of this socket's {@code localport} field.
     *
     * @return the value of this socket's {@code localport} field.
     *
     * @see java.net.SocketImpl#localport
     */
    /*
     * 返回本地端口
     *
     * [客户端Socket] 　　 : 本地端口，即客户端的端口
     * [服务端Socket(监听)]: 本地端口，即服务端的端口
     * [服务端Socket(通信)]: 本地端口，即服务端的端口
     *
     * 特殊情形：存在反向代理时，该字段依然存储的是本地端口，即服务端的端口
     */
    protected int getLocalPort() {
        return localport;
    }
    
    /**
     * Returns the value of this socket's {@code port} field.
     *
     * @return the value of this socket's {@code port} field.
     *
     * @see java.net.SocketImpl#port
     */
    /*
     * 返回远程端口
     *
     * [客户端Socket] 　　 : 远程端口，即服务端的端口
     * [服务端Socket(监听)]: 未设置
     * [服务端Socket(通信)]: 远程端口，即客户端的端口
     *
     * 特殊情形：存在反向代理时，该字段会存储代理端(远程)的端口
     */
    protected int getPort() {
        return port;
    }
    
    /*▲ 地址 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a set of SocketOptions supported by this impl and by this impl's socket (Socket or ServerSocket)
     *
     * @return a Set of SocketOptions
     *
     * @since 9
     */
    // 获取Socket或ServerSocket配置参数
    protected Set<SocketOption<?>> supportedOptions() {
        // 如果是Socket
        if(getSocket() != null) {
            return socketOptions;
        }
        
        // 否则就是ServerSocket
        return serverSocketOptions;
    }
    
    /**
     * Called to get a socket option.
     *
     * @param <T>  The type of the socket option value
     * @param name The socket option
     *
     * @return the value of the named option
     *
     * @throws UnsupportedOperationException if the SocketImpl does not
     *                                       support the option.
     * @throws IOException                   if an I/O error occurs, or if the socket is closed.
     * @since 9
     */
    // 获取指定名称的Socket配置参数
    @SuppressWarnings("unchecked")
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        if(name == StandardSocketOptions.SO_KEEPALIVE && (getSocket() != null)) {
            return (T) getOption(SocketOptions.SO_KEEPALIVE);
        } else if(name == StandardSocketOptions.SO_SNDBUF && (getSocket() != null)) {
            return (T) getOption(SocketOptions.SO_SNDBUF);
        } else if(name == StandardSocketOptions.SO_RCVBUF) {
            return (T) getOption(SocketOptions.SO_RCVBUF);
        } else if(name == StandardSocketOptions.SO_REUSEADDR) {
            return (T) getOption(SocketOptions.SO_REUSEADDR);
        } else if(name == StandardSocketOptions.SO_REUSEPORT && supportedOptions().contains(name)) {
            return (T) getOption(SocketOptions.SO_REUSEPORT);
        } else if(name == StandardSocketOptions.SO_LINGER && (getSocket() != null)) {
            return (T) getOption(SocketOptions.SO_LINGER);
        } else if(name == StandardSocketOptions.IP_TOS) {
            return (T) getOption(SocketOptions.IP_TOS);
        } else if(name == StandardSocketOptions.TCP_NODELAY && (getSocket() != null)) {
            return (T) getOption(SocketOptions.TCP_NODELAY);
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
     * @throws UnsupportedOperationException if the SocketImpl does not
     *                                       support the option
     * @throws IOException                   if an I/O error occurs, or if the socket is closed.
     * @since 9
     */
    // 设置指定名称的Socket配置参数
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        if(name == StandardSocketOptions.SO_KEEPALIVE && (getSocket() != null)) {
            setOption(SocketOptions.SO_KEEPALIVE, value);
        } else if(name == StandardSocketOptions.SO_SNDBUF && (getSocket() != null)) {
            setOption(SocketOptions.SO_SNDBUF, value);
        } else if(name == StandardSocketOptions.SO_RCVBUF) {
            setOption(SocketOptions.SO_RCVBUF, value);
        } else if(name == StandardSocketOptions.SO_REUSEADDR) {
            setOption(SocketOptions.SO_REUSEADDR, value);
        } else if(name == StandardSocketOptions.SO_REUSEPORT && supportedOptions().contains(name)) {
            setOption(SocketOptions.SO_REUSEPORT, value);
        } else if(name == StandardSocketOptions.SO_LINGER && (getSocket() != null)) {
            setOption(SocketOptions.SO_LINGER, value);
        } else if(name == StandardSocketOptions.IP_TOS) {
            setOption(SocketOptions.IP_TOS, value);
        } else if(name == StandardSocketOptions.TCP_NODELAY && (getSocket() != null)) {
            setOption(SocketOptions.TCP_NODELAY, value);
        } else {
            throw new UnsupportedOperationException("unsupported option");
        }
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns whether or not this SocketImpl supports sending
     * urgent data. By default, false is returned
     * unless the method is overridden in a sub-class
     *
     * @return true if urgent data supported
     *
     * @see java.net.SocketImpl#address
     * @since 1.4
     */
    // 是否支持客户端Socket发送"紧急数据"，必须由子类覆盖
    protected boolean supportsUrgentData() {
        return false; // must be overridden in sub-class
    }
    
    /**
     * Send one byte of urgent data on the socket.
     * The byte to be sent is the low eight bits of the parameter
     *
     * @param data The byte of data to send
     *
     * @throws IOException if there is an error
     *                     sending the data.
     * @since 1.4
     */
    // 发送一个字节的"紧急数据"，参见SocketOptions#SO_OOBINLINE参数
    protected abstract void sendUrgentData(int data) throws IOException;
    
    /**
     * Sets performance preferences for this socket.
     *
     * <p> Sockets use the TCP/IP protocol by default.  Some implementations
     * may offer alternative protocols which have different performance
     * characteristics than TCP/IP.  This method allows the application to
     * express its own preferences as to how these tradeoffs should be made
     * when the implementation chooses from the available protocols.
     *
     * <p> Performance preferences are described by three integers
     * whose values indicate the relative importance of short connection time,
     * low latency, and high bandwidth.  The absolute values of the integers
     * are irrelevant; in order to choose a protocol the values are simply
     * compared, with larger values indicating stronger preferences. Negative
     * values represent a lower priority than positive values. If the
     * application prefers short connection time over both low latency and high
     * bandwidth, for example, then it could invoke this method with the values
     * {@code (1, 0, 0)}.  If the application prefers high bandwidth above low
     * latency, and low latency above short connection time, then it could
     * invoke this method with the values {@code (0, 1, 2)}.
     *
     * By default, this method does nothing, unless it is overridden in
     * a sub-class.
     *
     * @param connectionTime An {@code int} expressing the relative importance of a short
     *                       connection time
     * @param latency        An {@code int} expressing the relative importance of low
     *                       latency
     * @param bandwidth      An {@code int} expressing the relative importance of high
     *                       bandwidth
     *
     * @since 1.5
     */
    // 设置Socket性能参数
    protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        /* Not implemented yet */
    }
    
    /**
     * Returns the value of this socket's {@code fd} field.
     *
     * @return the value of this socket's {@code fd} field.
     *
     * @see java.net.SocketImpl#fd
     */
    // 获取当前Socket文件的文件描述符
    protected FileDescriptor getFileDescriptor() {
        return fd;
    }
    
    // 返回当前"Socket委托"关联的Socket
    Socket getSocket() {
        return socket;
    }
    
    // 为当前"Socket委托"关联Socket
    void setSocket(Socket soc) {
        this.socket = soc;
    }
    
    // 返回当前"Socket委托"关联的ServerSocket
    ServerSocket getServerSocket() {
        return serverSocket;
    }
    
    // 为当前"Socket委托"关联ServerSocket
    void setServerSocket(ServerSocket soc) {
        this.serverSocket = soc;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the address and port of this socket as a {@code String}.
     *
     * @return a string representation of this socket.
     */
    @Override
    public String toString() {
        return "Socket[addr=" + getInetAddress() + ",port=" + getPort() + ",localport=" + getLocalPort() + "]";
    }
    
}
