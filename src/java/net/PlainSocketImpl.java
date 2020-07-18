/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.security.AccessController;
import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.internal.misc.SharedSecrets;
import sun.security.action.GetPropertyAction;

/**
 * On Windows system we simply delegate to native methods.
 *
 * @author Chris Hegarty
 */
// 普通"Socket委托"在windows上的实现，"普通"的意思是该"Socket委托"不涉及网络代理
class PlainSocketImpl extends AbstractPlainSocketImpl {
    
    static final int WOULDBLOCK = -2;       // Nothing available (non-blocking)
    
    // 是否偏向使用IP4地址，默认值是false
    private static final boolean preferIPv4Stack = Boolean.parseBoolean(AccessController.doPrivileged(new GetPropertyAction("java.net.preferIPv4Stack", "false")));
    
    /** Empty value of sun.net.useExclusiveBind is treated as 'true'. */
    // 是否使用独占式端口绑定，默认为true
    private static final boolean useExclusiveBind;
    
    /** emulates SO_REUSEADDR when useExclusiveBind is true */
    // 是否允许立刻重用已关闭的socket端口，默认为false
    private boolean isReuseAddress;
    
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();
    
    
    static {
        initIDs();
        
        String exclBindProp = AccessController.doPrivileged(new GetPropertyAction("sun.net.useExclusiveBind", ""));
        useExclusiveBind = exclBindProp.isEmpty() || Boolean.parseBoolean(exclBindProp);
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs an empty instance.
     */
    public PlainSocketImpl() {
    }
    
    /**
     * Constructs an instance with the given file descriptor.
     */
    public PlainSocketImpl(FileDescriptor fd) {
        this.fd = fd;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 创建Socket文件，并将其本地文件描述符存储到Java层的文件描述符中
     *
     * stream==true ：创建TCP Socket
     * stream==false：创建UDP Socket
     */
    @Override
    void socketCreate(boolean stream) throws IOException {
        if(fd == null) {
            throw new SocketException("Socket closed");
        }
        
        // 创建一个socket文件，并返回其本地文件描述符
        int newfd = socket0(stream);
        
        // 将其本地文件描述符存储到Java层的Socket文件描述符中
        fdAccess.set(fd, newfd);
    }
    
    
    // 创建一个socket文件，并返回其文件描述符编号
    static native int socket0(boolean stream) throws IOException;
    
    /*▲ 创建 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    @Override
    void socketConnect(InetAddress address, int port, int timeout) throws IOException {
        // 获取本地Socket文件描述符的本地引用(第一个"本地"是指相对于远端的本地，第二个"本地"是指相对于"Java层"的本地)
        int nativefd = checkAndReturnNativeFD();
        
        if(address == null) {
            throw new NullPointerException("inet address argument is null.");
        }
        
        // 如果偏向使用IP4地址，但给定的地址类型不是IP4，则抛异常
        if(preferIPv4Stack && !(address instanceof Inet4Address)) {
            throw new SocketException("Protocol family not supported");
        }
        
        int connectResult;
        
        // 如果未设置超时限制，理论上允许一直等待(实际上仅会等待一段时间)
        if(timeout<=0) {
            // 如果成功连接到远端，则返回0
            connectResult = connect0(nativefd, address, port);
            
            // 有超时约束的连接
        } else {
            // 临时更改本地Socket为非阻塞模式
            configureBlocking(nativefd, false);
            
            try {
                // 开启非阻塞的连接
                connectResult = connect0(nativefd, address, port);
                
                // 如果没连上，则进行超时等待，直到时间到了以后再退出
                if(connectResult == WOULDBLOCK) {
                    // 在timeout时间内等待本地Socket与远端Socket建立连接
                    waitForConnect(nativefd, timeout);
                }
            } finally {
                // 恢复本地Socket为阻塞模式
                configureBlocking(nativefd, true);
            }
        }
        
        /*
         * We need to set the local port field.
         * If bind was called previous to the connect (by the client) then localport field will already be set.
         */
        // 如果本地Socket未绑定本地端口，则会从本地随机选择一个端口向远端发起连接，这里需要获取本地Socket的本地端口
        if(localport == 0) {
            localport = localPort0(nativefd);
        }
    }
    
    // 本地Socket向指定IP地址处的远端Socket发起连接；理论上允许一直等待(实际上仅会等待一段时间)
    static native int connect0(int fd, InetAddress remote, int remotePort) throws IOException;
    
    // 在timeout时间内等待本地Socket与远端Socket建立连接
    static native void waitForConnect(int fd, int timeout) throws IOException;
    
    
    // 通过当前"Socket委托"为引用的Socket绑定IP与端口号
    @Override
    void socketBind(InetAddress address, int port) throws IOException {
        // 获取[客户端Socket]/[服务端Socket(监听)]文件描述符的本地引用
        int nativefd = checkAndReturnNativeFD();
        
        if(address == null) {
            throw new NullPointerException("inet address argument is null.");
        }
        
        if(preferIPv4Stack && !(address instanceof Inet4Address)) {
            throw new SocketException("Protocol family not supported");
        }
        
        // 为指定的Socket绑定IP地址与端口号
        bind0(nativefd, address, port, useExclusiveBind);
        
        // 如果给定的端口为0，则会将指定的Socket随机绑定到一个端口
        if(port == 0) {
            // 获取从本地随机选择的端口号
            localport = localPort0(nativefd);
        } else {
            // 使用预设的端口
            localport = port;
        }
        
        // 记录绑定的IP地址
        this.address = address;
    }
    
    // 为指定的Socket绑定IP地址与端口号
    static native void bind0(int fd, InetAddress localAddress, int localport, boolean exclBind) throws IOException;
    
    
    // 通过当前"Socket委托"为引用的[服务端Socket(监听)]开启监听，backlog代表允许积压的待处理连接数
    @Override
    void socketListen(int backlog) throws IOException {
        // 获取[服务端Socket(监听)]文件描述符的本地引用
        int nativefd = checkAndReturnNativeFD();
        
        // 使[服务端Socket(监听)]开启监听
        listen0(nativefd, backlog);
    }
    
    // ServerSocketSocket开启监听
    static native void listen0(int fd, int backlog) throws IOException;
    
    
    /*
     * 由ServerSocket的"Socket委托"调用，对[服务端Socket(监听)]执行【accept】操作，
     * accept成功后，会获取到新生成的[服务端Socket(通信)]的文件描述符，
     * 随后，将客户端端口/客户端地址/服务端端口/[服务端Socket(通信)]文件描述符设置到参数中的"Socket委托"中。
     *
     * socketImpl: [服务端Socket(通信)]的"Socket委托"，
     *             accept成功后会为其填充相关的地址信息，以及为其关联[服务端Socket(通信)](的文件描述符)
     */
    @Override
    void socketAccept(SocketImpl socketImpl) throws IOException {
        // 获取[服务端Socket(监听)]文件描述符的本地引用
        int nativefd = checkAndReturnNativeFD();
        
        if(socketImpl == null) {
            throw new NullPointerException("socket is null");
        }
        
        // [服务端Socket(通信)]文件描述符的本地引用
        int newfd = -1;
        
        InetSocketAddress[] isaa = new InetSocketAddress[1];
        
        // 如果未设置超时限制，理论上允许一直阻塞
        if(timeout<=0) {
            /*
             * 服务端收到客户端的连接请求后，返回在服务端新建的[服务端Socket(通信)]文件描述符的本地引用；
             * 如果收不到连接请求，则一直等待...
             */
            newfd = accept0(nativefd, isaa);
        } else {
            // 临时更改[服务端Socket(监听)]为非阻塞模式
            configureBlocking(nativefd, false);
            
            try {
                // 在timeout时间内服务端等待客户端的连接到来
                waitForNewConnection(nativefd, timeout);
                
                /*
                 * 服务端收到客户端的连接请求后，返回在服务端新建的[服务端Socket(通信)]文件描述符的本地引用；
                 * 由于上面已经更改[服务端Socket(监听)]为非阻塞模式，因此这里会立即返回[服务端Socket(通信)]的文件描述符。
                 */
                newfd = accept0(nativefd, isaa);
                
                // 如果成功获取到[服务端Socket(通信)]文件描述符
                if(newfd != -1) {
                    // 设置[服务端Socket(通信)]为阻塞模式
                    configureBlocking(newfd, true);
                }
            } finally {
                // 不论如何，最终恢复[服务端Socket(监听)]为阻塞模式
                configureBlocking(nativefd, true);
            }
        }
        
        /* Update (SocketImpl)s' fd */
        // 将[服务端Socket(通信)]文件描述符的本地引用设置到Java层的[服务端Socket(通信)]文件描述符中
        fdAccess.set(socketImpl.fd, newfd);
        
        /* Update socketImpls remote port, address and localport */
        // 为[服务端Socket(通信)]的"Socket委托"设置地址信息
        InetSocketAddress isa = isaa[0];
        socketImpl.address = isa.getAddress();    // 远程地址，这里是客户端的地址
        socketImpl.port = isa.getPort();       // 远程端口，这里是客户端的端口号
        socketImpl.localport = localport;           // 本地端口，即服务端的端口
        
        if(preferIPv4Stack && !(socketImpl.address instanceof Inet4Address)) {
            throw new SocketException("Protocol family not supported");
        }
    }
    
    // 在timeout时间内服务端等待客户端的连接到来
    static native void waitForNewConnection(int fd, int timeout) throws IOException;
    
    // 服务端收到客户端的连接请求后，返回在服务端新建的[服务端Socket(通信)]文件描述符的本地引用
    static native int accept0(int fd, InetSocketAddress[] isaa) throws IOException;
    
    
    // 临时更改fd指示的Socket为非阻塞模式
    static native void configureBlocking(int fd, boolean blocking) throws IOException;
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取在非阻塞下socket中剩余可读字节数
    @Override
    int socketAvailable() throws IOException {
        // 获取Socket文件描述符的本地引用
        int nativefd = checkAndReturnNativeFD();
        return available0(nativefd);
    }
    
    
    // 获取在非阻塞下socket中剩余可读字节数
    static native int available0(int fd) throws IOException;
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭Socket
    @Override
    void socketClose0(boolean useDeferredClose/*unused*/) throws IOException {
        if(fd == null) {
            throw new SocketException("Socket closed");
        }
        
        if(!fd.valid()) {
            return;
        }
        
        final int nativefd = fdAccess.get(fd);
        fdAccess.set(fd, -1);
        
        close0(nativefd);
    }
    
    // 关闭Socket
    static native void close0(int fd) throws IOException;
    
    
    // 关闭Socket的输入/输出功能
    @Override
    void socketShutdown(int howto) throws IOException {
        // 获取Socket文件描述符的本地引用
        int nativefd = checkAndReturnNativeFD();
        shutdown0(nativefd, howto);
    }
    
    // 关闭Socket的输入/输出功能
    static native void shutdown0(int fd, int howto) throws IOException;
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取Socket配置参数，参数列表参见SocketOptions
    @Override
    int socketGetOption(int opt, Object iaContainerObj) throws SocketException {
        
        // SO_REUSEPORT is not supported on Windows.
        if(opt == SO_REUSEPORT) {
            // windows上不支持该选项
            throw new UnsupportedOperationException("unsupported option");
        }
        
        // 获取Socket文件描述符的本地引用
        int nativefd = checkAndReturnNativeFD();
        
        // SO_BINDADDR is not a socket option.
        if(opt == SO_BINDADDR) {
            // 获取Socket绑定的本地地址，并将其存入iaContainerObj中
            localAddress(nativefd, (InetAddressContainer) iaContainerObj);
            return 0;  // return value doesn't matter.
        }
        
        // SO_REUSEADDR emulated when using exclusive bind
        if(opt == SO_REUSEADDR && useExclusiveBind) {
            // 是否允许立刻重用已关闭的socket端口，默认为false
            return isReuseAddress ? 1 : -1;
        }
        
        // 获取指定参数的值，返回0代表false
        int value = getIntOption(nativefd, opt);
        
        switch(opt) {
            case TCP_NODELAY:
            case SO_OOBINLINE:
            case SO_KEEPALIVE:
            case SO_REUSEADDR:
                return (value == 0) ? -1 : 1;
        }
        
        return value;
    }
    
    /** Intentional fallthrough after SO_REUSEADDR */
    // 设置Socket配置参数，参数列表参见SocketOptions
    @SuppressWarnings("fallthrough")
    @Override
    void socketSetOption(int opt, boolean on, Object value) throws SocketException {
        
        // SO_REUSEPORT is not supported on Windows.
        if(opt == SO_REUSEPORT) {
            // windows上不支持该选项
            throw new UnsupportedOperationException("unsupported option");
        }
        
        // 获取Socket文件描述符的本地引用
        int nativefd = checkAndReturnNativeFD();
        
        // 设置超时约束
        if(opt == SO_TIMEOUT) {
            if(preferIPv4Stack) {
                // Don't enable the socket option on ServerSocket as it's meaningless (we don't receive on a ServerSocket).
                if(serverSocket == null) {
                    setSoTimeout0(nativefd, (Integer) value);
                }
            } // else timeout is implemented through select.
            
            return;
        }
        
        int optionValue = 0;
        
        switch(opt) {
            case SO_REUSEADDR:
                // 是否使用独占式端口绑定，默认为true
                if(useExclusiveBind) {
                    // SO_REUSEADDR emulated when using exclusive bind
                    isReuseAddress = on;
                    return;
                }
                // intentional fallthrough
            case TCP_NODELAY:
            case SO_OOBINLINE:
            case SO_KEEPALIVE:
                optionValue = on ? 1 : 0;
                break;
            case SO_SNDBUF:
            case SO_RCVBUF:
            case IP_TOS:
                optionValue = (Integer) value;
                break;
            case SO_LINGER:
                if(on) {
                    optionValue = (Integer) value;
                } else {
                    optionValue = -1;
                }
                break;
            default:/* shouldn't get here */
                throw new SocketException("Option not supported");
        }
        
        setIntOption(nativefd, opt, optionValue);
    }
    
    static native int getIntOption(int fd, int cmd) throws SocketException;
    
    static native void setIntOption(int fd, int cmd, int optionValue) throws SocketException;
    
    
    static native void setSoTimeout0(int fd, int timeout) throws SocketException;
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 发送一个字节的"紧急数据"，参见SocketOptions#SO_OOBINLINE参数
    @Override
    void socketSendUrgentData(int data) throws IOException {
        // 获取Socket文件描述符的本地引用
        int nativefd = checkAndReturnNativeFD();
        sendOOB(nativefd, data);
    }
    
    static native void sendOOB(int fd, int data) throws IOException;
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回Socket文件描述符的本地引用
    private int checkAndReturnNativeFD() throws SocketException {
        if(fd == null || !fd.valid()) {
            throw new SocketException("Socket closed");
        }
        
        return fdAccess.get(fd);
    }
    
    static native void initIDs();
    
    static native int localPort0(int fd) throws IOException;
    
    static native void localAddress(int fd, InetAddressContainer in) throws SocketException;
    
}
