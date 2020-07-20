/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.List;
import jdk.internal.util.StaticProperty;
import sun.net.SocksProxy;
import sun.net.spi.DefaultProxySelector;
import sun.net.www.ParseUtil;

/**
 * SOCKS (V4 & V5) TCP socket implementation (RFC 1928).
 * This is a subclass of PlainSocketImpl.
 * Note this class should <b>NOT</b> be public.
 */
/*
 * 支持SOCKS(V4 & V5)协议的"Socket委托"；参见SocksProxy类。
 * SOCKS协议通常用在代理中，可以是正向代理，也可以是反向代理。
 */
class SocksSocketImpl extends PlainSocketImpl implements SocksConsts {
    
    // 代理主机名称或主机地址，代理可能为正向代理或反向代理
    private String server = null;
    // 代理端口号，默认为1080，代理可能为正向代理或反向代理
    private int serverPort = DEFAULT_PORT;
    
    /*
     * 存在代理时，记录服务端地址
     *
     * 客户端连接到正向代理时，external_address记录远程服务端地址
     * 服务端绑定到反向代理时，external_address记录服务端本地地址
     */
    private InetSocketAddress external_address;
    
    // 是否使用了V4版本的Socket
    private boolean useV4 = false;
    
    // 存在反向代理时，用作[服务端Socket(通信)]
    private Socket cmdsock = null;
    
    /*
     * 存在正向代理时，cmdIn指[客户端Socket]的输入流，对cmdIn的赋值发生在客户端的connect操作中
     * 存在反向代理时，cmdIn指[服务端Socket(通信)]的输入流，对cmdIn的赋值发生在服务端的bind操作中
     *
     * 从该输入流读取数据，可以获取到代理端传出的数据
     */
    private InputStream cmdIn = null;
    
    /*
     * 存在正向代理时，cmdOut指[客户端Socket]的输出流，对cmdOut的赋值发生在客户端的connect操作中
     * 存在反向代理时，cmdOut指[服务端Socket(通信)]的输出流，对cmdOut的赋值发生在服务端的bind操作中
     *
     * 向该输出流写入数据，可以传递到代理端
     */
    private OutputStream cmdOut = null;
    
    /* true if the Proxy has been set programatically */
    private boolean applicationSetProxy;  /* false */
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 常用于Socket类和ServerSocket类中创建Socket实现类
    SocksSocketImpl() {
        // Nothing needed
    }
    
    // 用指定的代理地址构造"Socket委托"
    SocksSocketImpl(String server, int port) {
        this.server = server;
        this.serverPort = (port == -1 ? DEFAULT_PORT : port);
    }
    
    // 用指定的代理构造"Socket委托"
    SocksSocketImpl(Proxy proxy) {
        // 获取代理地址
        SocketAddress addr = proxy.address();
        
        // 目前SocketAddress只有InetSocketAddress一个实现类，所以只要adrr不为null，判断就成立
        if(addr instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) addr;
            /* Use getHostString() to avoid reverse lookups */
            // 获取代理主机名称或主机地址
            server = address.getHostString();
            // 获取代理端口号
            serverPort = address.getPort();
        }
        
        // 判断是否使用了V4版本的Socket
        useV4 = useV4(proxy);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Connects the Socks Socket to the specified endpoint. It will first
     * connect to the SOCKS proxy and negotiate the access. If the proxy
     * grants the connections, then the connect is successful and all
     * further traffic will go to the "real" endpoint.
     *
     * @param endpoint the {@code SocketAddress} to connect to.
     * @param timeout  the timeout value in milliseconds
     *
     * @throws IOException              if the connection can't be established.
     * @throws SecurityException        if there is a security manager and it
     *                                  doesn't allow the connection
     * @throws IllegalArgumentException if endpoint is null or a
     *                                  SocketAddress subclass not supported by this socket
     */
    /*
     * 本地Socket向远端Socket发起连接，允许指定超时时间
     *
     *                  本地Socket        远端Socket
     * 不存在代理     [客户端Socket]  [服务端Socket(通信)]
     * 存在正向代理   [客户端Socket]   代理端Socket
     *
     * endpoint: 固定是[服务端Socket(通信)]地址；当存在反向代理时，不会调用该重载方法，而是调用super中的同签名方法
     *
     * 注：如果存在正向代理，连接完成后，需要对本地Socket与正向代理端进行通信验证
     */
    @Override
    protected void connect(SocketAddress endpoint, int timeout) throws IOException {
        final long deadlineMillis;
        
        // 确定超时时间
        if(timeout == 0) {
            deadlineMillis = 0L;
        } else {
            long finish = System.currentTimeMillis() + timeout;
            deadlineMillis = finish<0 ? Long.MAX_VALUE : finish;
        }
        
        if(!(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        
        // 记录远程地址
        InetSocketAddress epoint = (InetSocketAddress) endpoint;
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            if(epoint.isUnresolved()) {
                security.checkConnect(epoint.getHostName(), epoint.getPort());
            } else {
                security.checkConnect(epoint.getAddress().getHostAddress(), epoint.getPort());
            }
        }
        
        // 如果显式设置了正向代理
        if(server != null) {
            // Connects to the SOCKS server
            try {
                // 将[客户端Socket]连接到正向代理
                privilegedConnect(server, serverPort, remainingMillis(deadlineMillis));
            } catch(IOException e) {
                throw new SocketException(e.getMessage());
            }
            
            // 如果没有显式设置正向代理
        } else {
            /* This is the general case server is not null only when the socket was created with a specified proxy in which case it does bypass the ProxySelector */
            // 获取系统使用的代理选择器，默认为DefaultProxySelector
            ProxySelector sel = AccessController.doPrivileged(new PrivilegedAction<>() {
                public ProxySelector run() {
                    return ProxySelector.getDefault();
                }
            });
            
            // 不存在代理选择器的话，直接连接（一般是存在的，即使不设置，系统也会提供默认的代理选择器）
            if(sel == null) {
                /* No default proxySelector --> direct connection */
                super.connect(epoint, remainingMillis(deadlineMillis));
                return;
            }
            
            /* Use getHostString() to avoid reverse lookups */
            // 获取远端主机名称或主机地址的字符串形式
            String host = epoint.getHostString();
            
            // 处理IP6地址(视情形添加[])
            if(epoint.getAddress() instanceof Inet6Address && (!host.startsWith("[")) && (host.indexOf(':') >= 0)) {
                host = "[" + host + "]";
            }
            
            // 将待连接地址转换为URI（使用"socket"协议）
            URI uri;
            try {
                uri = new URI("socket://" + ParseUtil.encodePath(host) + ":" + epoint.getPort());
            } catch(URISyntaxException e) {
                // This shouldn't happen
                assert false : e;
                uri = null;
            }
            
            // 根据协议信息获取可用的代理列表
            List<Proxy> proxyList = sel.select(uri);
            
            // 获取代理迭代器
            Iterator<Proxy> proxyIterator = proxyList.iterator();
            
            // 如果没有可用的代理，直接连接
            if(proxyIterator == null || !(proxyIterator.hasNext())) {
                super.connect(epoint, remainingMillis(deadlineMillis));
                return;
            }
            
            // 此处的代理可能是正向代理，也可能是反向代理
            Proxy proxy = null;
            IOException savedExc = null;
            
            // 遍历所有可用代理
            while(proxyIterator.hasNext()) {
                // 获取可用代理
                proxy = proxyIterator.next();
                
                // 如果未设置代理，或代理类型不是SOCKS，直接连接
                if(proxy == null || proxy.type() != Proxy.Type.SOCKS) {
                    super.connect(epoint, remainingMillis(deadlineMillis));
                    return;
                }
                
                if(!(proxy.address() instanceof InetSocketAddress)) {
                    throw new SocketException("Unknown address type for proxy: " + proxy);
                }
                
                /* Use getHostString() to avoid reverse lookups */
                
                // 获取代理主机名称或主机地址
                server = ((InetSocketAddress) proxy.address()).getHostString();
                // 获取代理端口号，默认为1080
                serverPort = ((InetSocketAddress) proxy.address()).getPort();
                // 判断是否使用了V4版本的SOCKS4代理
                useV4 = useV4(proxy);
                
                // Connects to the SOCKS server
                try {
                    // 将[客户端Socket]连接到正向代理
                    privilegedConnect(server, serverPort, remainingMillis(deadlineMillis));
                    // Worked, let's get outta here
                    break;
                } catch(IOException e) {
                    // Ooops, let's notify the ProxySelector
                    sel.connectFailed(uri, proxy.address(), e);
                    server = null;
                    serverPort = -1;
                    savedExc = e;
                    // Will continue the while loop and try the next proxy
                }
            } // while
            
            /* If server is still null at this point, none of the proxy worked */
            if(server == null) {
                throw new SocketException("Can't connect to SOCKS proxy:" + savedExc.getMessage());
            }
        }
        
        /* cmdIn & cmdOut were initialized during the privilegedConnect() call */
        
        // [客户端Socket]的输入流
        InputStream in = cmdIn;
        // [客户端Socket]的输出流
        BufferedOutputStream out = new BufferedOutputStream(cmdOut, 512);
        
        /*██使用了SOCKS4代理██*/
        if(useV4) {
            // SOCKS Protocol version 4 doesn't know how to deal with DOMAIN type of addresses (unresolved addresses here)
            if(epoint.isUnresolved()) {
                throw new UnknownHostException(epoint.toString());
            }
            
            // [客户端Socket]与正向代理进行通信验证，epoint是[客户端Socket]连接到的远端地址，这将被发送(告知)给正向代理
            connectV4(in, out, epoint, deadlineMillis);
            
            // 连接成功后返回
            return;
        }
        
        
        /*██使用了SOCKS5代理██*/
        
        /*
         * ★ 1.客户端向代理端发送问候信息
         *
         *        版本号 认证方法数量 认证信息
         * 字节数    1	     1        未限定
         *
         * 版本号　　　：0x05，SOCKS5协议
         * 认证方法数量：限定认证信息中使用的认证方法数量
         * 认证信息　　：指示认证方法，每种方法用一个字节表示，支持的认证方法如下：
         * 　　　　　　　0x00: No authentication
         * 　　　　　　　0x01: GSSAPI
         * 　　　　　　　0x02: Username/password
         * 　　　　　　　0x03–0x7F: methods assigned by IANA
         * 　　　　　　　0x80–0xFE: methods reserved for private use
         */
        
        out.write(PROTO_VERS);  // SOCKS5协议
        out.write(2);           // 2个认证方法
        out.write(NO_AUTH);
        out.write(USER_PASSW);
        out.flush();
        
        /*
         * ★ 2.客户端接收代理端的响应
         *
         *        版本号 选择的认证方法
         * 字节数    1	     1
         *
         * 版本号　　　　：0x05，SOCKS5协议
         * 选择的认证方法：从客户端发来的认证方法中选择一个认证方法，如果没有可接受的认证方法，则返回0xFF(-1)
         */
        byte[] data = new byte[2];
        int i = readSocksReply(in, data, deadlineMillis);
        
        // 或许，代理端不是SOCKS5代理，在放弃之前，尝试用连接SOCKS4代理的方式连接它
        if(i != 2 || ((int) data[0]) != PROTO_VERS) {
            /*
             * Maybe it's not a V5 sever after all,
             * Let's try V4 before we give up,
             * SOCKS Protocol version 4 doesn't know how to deal with DOMAIN type of addresses (unresolved addresses here).
             */
            if(epoint.isUnresolved()) {
                throw new UnknownHostException(epoint.toString());
            }
            
            // [客户端Socket]与正向代理进行通信验证，epoint是[客户端Socket]连接到的远端地址，这将被发送(告知)给正向代理
            connectV4(in, out, epoint, deadlineMillis);
            return;
        }
        
        // 代理端没有选到合适的认证方法
        if(((int) data[1]) == NO_METHODS) {
            throw new SocketException("SOCKS : No acceptable methods");
        }
        
        /*
         * ★ 3.身份验证，客户端向代理端发送认证信息(目前仅实现了对用户名/密码的验证服务)
         * ★ 4.代理端对身份验证的响应
         */
        if(!authenticate(data[1], in, out, deadlineMillis)) {
            throw new SocketException("SOCKS : authentication failed");
        }
        
        
        /*
         * ★★ 定义一个[远程地址结构]
         *
         *        地址类型 地址信息
         * 字节数    1	   未限定
         *
         * 地址类型：以下类型之一
         * 　　　　　0x01 => IPv4地址
         * 　　　　　0x02 => 域名
         * 　　　　　0x03 => IPv6地址
         * 地址信息：不同的地址类型，其地址信息的长度也不同
         * 　　　　　(1)IPv4地址，通常占用4个字节
         * 　　　　　(2)域名，1个字节的域名长度，后跟(1–255)个字节的域名
         * 　　　　　(3)IPv6地址，通常占用16个字节
         *
         *
         * ★ 5.客户端向代理端发送连接请求，发生在身份验证之后
         *
         *        版本号 连接命令 [保留值] 远程地址 远程端口
         * 字节数    1	   1        1     未限定     2
         *
         * 版本号　：0x05，SOCKS5协议
         * 连接命令：限定认证信息中使用的认证方法数量
         * 　　　　　0x01 => 建立TCP连接
         * 　　　　　0x02 => 建立TCP端口绑定
         * 　　　　　0x03 => 关联UDP端口
         * [保留值]：该值做保留值，目前总是0
         * 远程地址：远程服务端的地址，参考上面定义的[远程地址结构]
         * 远程端口：远程服务端的端口
         */
        
        out.write(PROTO_VERS);  // 版本号
        out.write(CONNECT);     // 连接命令
        out.write(0);           // [保留值]
        
        
        /* 接下来，传输远程地址信息 */
        
        /* Test for IPV4/IPV6/Unresolved */
        // (2)域名，1个字节的域名长度，后跟(1–255)个字节的域名
        if(epoint.isUnresolved()) {
            // 发送地址类型：域名
            out.write(DOMAIN_NAME);
            // 发送主机名长度
            out.write(epoint.getHostName().length());
            try {
                // 发送主机名
                out.write(epoint.getHostName().getBytes(StandardCharsets.ISO_8859_1));
            } catch(UnsupportedEncodingException uee) {
                assert false;
            }
            
            // (3)IPv6地址，通常占用16个字节
        } else if(epoint.getAddress() instanceof Inet6Address) {
            // 发送地址类型：IP6地址
            out.write(IPV6);
            // 发送IP6地址
            out.write(epoint.getAddress().getAddress());
            
            // (1)IPv4地址，通常占用4个字节
        } else {
            // 发送地址类型：IP4地址
            out.write(IPV4);
            // 发送IP4地址
            out.write(epoint.getAddress().getAddress());
        }
        
        // 按大端法传输服务端的端口号
        out.write((epoint.getPort() >> 8) & 0xff);
        out.write((epoint.getPort() >> 0) & 0xff);
        
        // 完成发送
        out.flush();
        
        /*
         * ★ 6.客户端接收代理端的响应
         *
         *        版本号 响应状态 [保留值] 远程地址 远程端口
         * 字节数    1	   1        1     未限定     2
         *
         * 版本号　：0x05，SOCKS5协议
         * 响应状态：状态码如下
         * 　　　　　0x00 => request granted(请求被允许)
         * 　　　　　0x01 => general failure
         * 　　　　　0x02 => connection not allowed by ruleset
         * 　　　　　0x03 => network unreachable
         * 　　　　　0x04 => host unreachable
         * 　　　　　0x05 => connection refused by destination host
         * 　　　　　0x06 => TTL expired
         * 　　　　　0x07 => command not supported / protocol error
         * 　　　　　0x08 => address type not supported
         * [保留值]：该值做保留值，目前总是0
         * 远程地址：远程服务端的地址，参考上面定义的[远程地址结构]
         * 远程端口：远程服务端的端口
         */
        
        data = new byte[4];
        i = readSocksReply(in, data, deadlineMillis);
        if(i != 4) {
            throw new SocketException("Reply from SOCKS server has bad length");
        }
        
        SocketException ex = null;
        
        switch(data[1]) {
            case REQUEST_OK:
                byte[] addr;
                
                // 连接成功
                switch(data[3]) {
                    case IPV4: {
                        // 获取IP4地址
                        addr = new byte[4];
                        i = readSocksReply(in, addr, deadlineMillis);
                        if(i != 4) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        
                        // 获取端口号
                        data = new byte[2];
                        i = readSocksReply(in, data, deadlineMillis);
                        if(i != 2) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        break;
                    }
                    case DOMAIN_NAME: {
                        // 获取主机名的长度信息
                        byte[] lenByte = new byte[1];
                        i = readSocksReply(in, lenByte, deadlineMillis);
                        if(i != 1) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        // 获取主机名长度
                        int len = lenByte[0] & 0xFF;
                        
                        // 获取主机名
                        byte[] host = new byte[len];
                        i = readSocksReply(in, host, deadlineMillis);
                        if(i != len) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        
                        // 获取端口号
                        data = new byte[2];
                        i = readSocksReply(in, data, deadlineMillis);
                        if(i != 2) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        break;
                    }
                    case IPV6: {
                        // 获取IP6地址
                        addr = new byte[16];
                        i = readSocksReply(in, addr, deadlineMillis);
                        if(i != 16) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        
                        // 获取端口号
                        data = new byte[2];
                        i = readSocksReply(in, data, deadlineMillis);
                        if(i != 2) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        break;
                    }
                    default:
                        ex = new SocketException("Reply from SOCKS server contains wrong code");
                        break;
                }
                break;
            case GENERAL_FAILURE:
                ex = new SocketException("SOCKS server general failure");
                break;
            case NOT_ALLOWED:
                ex = new SocketException("SOCKS: Connection not allowed by ruleset");
                break;
            case NET_UNREACHABLE:
                ex = new SocketException("SOCKS: Network unreachable");
                break;
            case HOST_UNREACHABLE:
                ex = new SocketException("SOCKS: Host unreachable");
                break;
            case CONN_REFUSED:
                ex = new SocketException("SOCKS: Connection refused");
                break;
            case TTL_EXPIRED:
                ex = new SocketException("SOCKS: TTL expired");
                break;
            case CMD_NOT_SUPPORTED:
                ex = new SocketException("SOCKS: Command not supported");
                break;
            case ADDR_TYPE_NOT_SUP:
                ex = new SocketException("SOCKS: address type not supported");
                break;
        }
        
        if(ex != null) {
            in.close();
            out.close();
            throw ex;
        }
        
        // 记录远程服务端地址
        external_address = epoint;
    }
    
    /*
     * [客户端Socket]与正向代理进行通信验证
     *
     * epoint: [客户端Socket]连接到的远端地址，这将被发送(告知)给正向代理
     *
     * SOCKS4协议，此处的C端是客户端，S端是代理端：
     *
     * ★ 1.客户端需向代理端发送以下格式的信息：
     *
     *        版本号 连接命令 远程端口 远程地址   ID
     * 字节数    1	    1	    2	     4	  未限定
     *
     * 版本号　：SOCKS版本号，此处应当发送0x04
     * 连接命令：0x01 => 建立TCP连接
     * 　　　　　0x02 => 建立TCP端口绑定
     * 远程端口：远程服务端的端口，遵循网络字节序
     * 远程地址：远程服务端的地址，遵循网络字节序
     * ID　　　：用户设定的ID，比如主机名，长度可变
     *
     *
     * ★ 2.代理端需要向客户端响应以下格式的信息：
     *
     *        版本号 响应状态码 远程端口 远程地址
     * 字节数    1	    1	    2	     4
     *
     * 版本号　　：如果未设置，就返回0；如果设置了，就返回4
     * 响应状态码：0x5A => Request granted.
     * 　　　　　　0x5B => Request rejected or failed.
     * 　　　　　　0x5C => Request failed because client is not running identd (or not reachable from server).
     * 　　　　　　0x5D => Request failed because client's identd could not confirm the user ID in the request.
     * 远程端口：远程服务端的端口，通常被忽略
     * 远程地址：远程服务端的地址，通常被忽略
     */
    private void connectV4(InputStream in, OutputStream out, InetSocketAddress endpoint, long deadlineMillis) throws IOException {
        if(!(endpoint.getAddress() instanceof Inet4Address)) {
            throw new SocketException("SOCKS V4 requires IPv4 only addresses");
        }
        
        /*
         * 注：对于in和out的使用：
         * 使用in 从客户端读取数据时，数据流向：客户端<---代理端
         * 使用out写入到客户端的数据，数据流向：客户端--->代理端
         */
        
        /*★ 1.客户端向代理端发送连接信息 */
        
        // 发送版本信息以确认代理端属性
        out.write(PROTO_VERS4);
        // 发送连接信息
        out.write(CONNECT);
        
        // 发送远程端口和远程地址信息（网络传输使用大端法）
        out.write((endpoint.getPort() >> 8) & 0xff);    // 先传大端(高位)
        out.write((endpoint.getPort() >> 0) & 0xff);    // 再传小端(低位)
        out.write(endpoint.getAddress().getAddress());  // 传输远程地址
        
        // 获取客户端主机名称，如kang
        String userName = getUserName();
        try {
            // 将主机信息作为ID发送出去
            out.write(userName.getBytes(StandardCharsets.ISO_8859_1));
        } catch(UnsupportedEncodingException uee) {
            assert false;
        }
        
        // 发送完毕
        out.write(0);
        out.flush();
        
        
        /*★ 2.客户端从代理端接收响应 */
        
        byte[] data = new byte[8];
        int n = readSocksReply(in, data, deadlineMillis);
        if(n != 8) {
            // 固定返回8个字节
            throw new SocketException("Reply from SOCKS server has bad length: " + n);
        }
        
        // data[0]代表协议版本
        if(data[0] != 0 && data[0] != 4) {
            throw new SocketException("Reply from SOCKS server has bad version");
        }
        
        SocketException ex = null;
        switch(data[1]) {
            case 0x5A:
                // Success! 记录远程服务端的地址
                external_address = endpoint;
                break;
            case 0x5B:
                ex = new SocketException("SOCKS request rejected");
                break;
            case 0x5C:
                ex = new SocketException("SOCKS server couldn't reach destination");
                break;
            case 0x5D:
                ex = new SocketException("SOCKS authentication failed");
                break;
            default:
                ex = new SocketException("Reply from SOCKS server contains bad status");
                break;
        }
        
        if(ex != null) {
            in.close();
            out.close();
            throw ex;
        }
    }
    
    // 将[客户端Socket]连接到正向代理
    private synchronized void privilegedConnect(final String host, final int port, final int timeout) throws IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                public Void run() throws IOException {
                    // 连接到正向代理
                    superConnectServer(host, port, timeout);
                    // 获取[客户端Socket]输入流
                    cmdIn = getInputStream();
                    // 获取[客户端Socket]输出流
                    cmdOut = getOutputStream();
                    return null;
                }
            });
        } catch(PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }
    }
    
    // [客户端Socket]连接到正向代理
    private void superConnectServer(String host, int port, int timeout) throws IOException {
        // 构造正向代理地址
        InetSocketAddress address = new InetSocketAddress(host, port);
        // 连接到正向代理
        super.connect(address, timeout);
    }
    
    
    /**
     * Sends the Bind request to the SOCKS proxy.
     * In the SOCKS protocol, bind means "accept incoming connection from",
     * so the SocketAddress is the one of the host we do accept connection from.
     *
     * @param saddr the Socket address of the remote host.
     *
     * @throws IOException if an I/O error occurs when binding this socket.
     */
    /*
     * [反向代理]
     *
     * 将[服务端Socket]绑定到指定的地址，连接中允许指定超时时间。
     * 绑定之前，会先对服务端与代理端建立连接，连接成功后，进行【bind】操作，最后，还需要对服务端与代理端进行通信验证。
     *
     * 当前方法执行成功后，还需要【listen】和【accept】操作的进一步配合才能使服务端就绪。
     *
     * 如果不是服务端在调用该方法，或者，不存在反向代理，则调用无效果。
     *
     * 注：实际使用时，需要视情形而实现该方法
     */
    protected synchronized void socksBind(InetSocketAddress saddr) throws IOException {
        // 如果这是客户端Socket，直接返回
        if(socket != null) {
            // this is a client socket, not a server socket, don't call the SOCKS proxy for a bind!
            return;
        }
        
        // 如果显式设置了反向代理
        if(server != null) {
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                    public Void run() throws Exception {
                        // 创建一个普通的"Socket委托"★★
                        PlainSocketImpl impl = new PlainSocketImpl();
                        
                        // 使用指定的"Socket委托"初始化一个未连接的Socket，该Socket会作为[服务端Socket(通信)]使用
                        cmdsock = new Socket(impl);
                        
                        // 构造反向代理地址
                        InetSocketAddress proxyAddr = new InetSocketAddress(server, serverPort);
                        
                        // 让[服务端Socket(通信)]连接到反向代理
                        cmdsock.connect(proxyAddr);
                        
                        // 获取[服务端Socket(通信)]的输入流
                        cmdIn = cmdsock.getInputStream();
                        
                        // 获取[服务端Socket(通信)]的输出流
                        cmdOut = cmdsock.getOutputStream();
                        
                        return null;
                    }
                });
            } catch(Exception e) {
                throw new SocketException(e.getMessage());
            }
            
            // 如果没有显式设置反向代理
        } else {
            /*
             * This is the general case:
             * server is not null only when the socket was created with a specified proxy in which case it does bypass the ProxySelector.
             */
            // 获取系统中默认的代理选择器
            ProxySelector sel = AccessController.doPrivileged(new PrivilegedAction<>() {
                public ProxySelector run() {
                    return ProxySelector.getDefault();
                }
            });
            
            // 不存在代理选择器的话，直接返回（一般是存在的，即使不设置，系统也会提供默认的代理选择器）
            if(sel == null) {
                /* No default proxySelector --> direct connection */
                return;
            }
            
            /* Use getHostString() to avoid reverse lookups */
            // 获取主机名称或主机地址
            String host = saddr.getHostString();
            
            // 处理IP6地址(视情形添加[])
            if(saddr.getAddress() instanceof Inet6Address && (!host.startsWith("[")) && (host.indexOf(':') >= 0)) {
                host = "[" + host + "]";
            }
            
            // 将待绑定地址转换为URI（使用"serversocket"协议）
            URI uri;
            try {
                uri = new URI("serversocket://" + ParseUtil.encodePath(host) + ":" + saddr.getPort());
            } catch(URISyntaxException e) {
                // This shouldn't happen
                assert false : e;
                uri = null;
            }
            
            // 根据协议信息获取可用的代理列表
            List<Proxy> proxyList = sel.select(uri);
            
            // 获取代理迭代器
            Iterator<Proxy> proxyIterator = proxyList.iterator();
            
            // 如果没有可用的代理，直接返回
            if(proxyIterator == null || !(proxyIterator.hasNext())) {
                return;
            }
            
            // 此处的代理应为反向代理
            Proxy proxy = null;
            Exception savedExc = null;
            
            // 遍历所有可用代理
            while(proxyIterator.hasNext()) {
                // 获取可用代理
                proxy = proxyIterator.next();
                
                // 如果未设置代理，或代理类型不是SOCKS，直接返回
                if(proxy == null || proxy.type() != Proxy.Type.SOCKS) {
                    return;
                }
                
                if(!(proxy.address() instanceof InetSocketAddress)) {
                    throw new SocketException("Unknown address type for proxy: " + proxy);
                }
                
                /* Use getHostString() to avoid reverse lookups */
                
                // 获取代理主机名称或主机地址
                server = ((InetSocketAddress) proxy.address()).getHostString();
                // 获取代理端口号，默认为1080
                serverPort = ((InetSocketAddress) proxy.address()).getPort();
                // 判断是否使用了V4版本的SOCKS4代理
                useV4 = useV4(proxy);
                
                // Connects to the SOCKS server
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                        public Void run() throws Exception {
                            // 创建一个普通的"Socket委托"
                            PlainSocketImpl impl = new PlainSocketImpl();
                            
                            // 使用指定的"Socket委托"初始化一个未连接的Socket，该Socket会作为[服务端Socket(通信)]使用
                            cmdsock = new Socket(impl);
                            
                            // 构造反向代理地址
                            InetSocketAddress proxyAddr = new InetSocketAddress(server, serverPort);
                            
                            // 让[服务端Socket(通信)]连接到反向代理
                            cmdsock.connect(proxyAddr);
                            
                            // 获取[服务端Socket(通信)]的输入流
                            cmdIn = cmdsock.getInputStream();
                            
                            // 获取[服务端Socket(通信)]的输出流
                            cmdOut = cmdsock.getOutputStream();
                            
                            return null;
                        }
                    });
                } catch(Exception e) {
                    // Ooops, let's notify the ProxySelector
                    sel.connectFailed(uri, proxy.address(), new SocketException(e.getMessage()));
                    server = null;
                    serverPort = -1;
                    cmdsock = null;
                    savedExc = e;
                    // Will continue the while loop and try the next proxy
                }
            } // while
            
            // If server is still null at this point, none of the proxy worked
            if(server == null || cmdsock == null) {
                throw new SocketException("Can't connect to SOCKS proxy:" + savedExc.getMessage());
            }
        }
        
        // [服务端Socket(通信)]的输入流
        InputStream in = cmdIn;
        // [服务端Socket(通信)]的输出流
        BufferedOutputStream out = new BufferedOutputStream(cmdOut, 512);
        
        /*██使用了SOCKS4代理██*/
        if(useV4) {
            /*
             * 先将[服务端Socket]绑定到指定的地址；随后，让服务端与反向代理进行通信验证。
             * baddr是[服务端Socket]绑定的本地地址，在通信验证中，会被服务端发送(告知)给反向代理。
             */
            bindV4(in, out, saddr.getAddress(), saddr.getPort());
            
            // 绑定成功后返回
            return;
        }
        
        
        /*██使用了SOCKS5代理██*/
        
        /*
         * ★ 1.服务端向代理端发送问候信息
         *
         *        版本号 认证方法数量 认证信息
         * 字节数    1	     1        未限定
         *
         * 版本号　　　：0x05，SOCKS5协议
         * 认证方法数量：限定认证信息中使用的认证方法数量
         * 认证信息　　：指示认证方法，每种方法用一个字节表示，支持的认证方法如下：
         * 　　　　　　　0x00: No authentication
         * 　　　　　　　0x01: GSSAPI
         * 　　　　　　　0x02: Username/password
         * 　　　　　　　0x03–0x7F: methods assigned by IANA
         * 　　　　　　　0x80–0xFE: methods reserved for private use
         */
        
        out.write(PROTO_VERS);  // SOCKS5协议
        out.write(2);           // 2个认证方法
        out.write(NO_AUTH);
        out.write(USER_PASSW);
        out.flush();
        
        /*
         * ★ 2.服务端接收代理端的响应
         *
         *        版本号 选择的认证方法
         * 字节数    1	     1
         *
         * 版本号　　　　：0x05，SOCKS5协议
         * 选择的认证方法：从服务端发来的认证方法中选择一个认证方法，如果没有可接受的认证方法，则返回0xFF(-1)
         */
        byte[] data = new byte[2];
        int i = readSocksReply(in, data);
        
        // 或许，代理端不是SOCKS5代理，在放弃之前，尝试用连接SOCKS4代理的方式连接它
        if(i != 2 || ((int) data[0]) != PROTO_VERS) {
            /*
             * Maybe it's not a V5 sever after all;
             * Let's try V4 before we give up.
             */
            
            /*
             * 先将[服务端Socket]绑定到指定的地址；随后，让服务端与反向代理进行通信验证。
             * baddr是[服务端Socket]绑定的本地地址，在通信验证中，会被服务端发送(告知)给反向代理。
             */
            bindV4(in, out, saddr.getAddress(), saddr.getPort());
            return;
        }
        
        // 代理端没有选到合适的认证方法
        if(((int) data[1]) == NO_METHODS) {
            throw new SocketException("SOCKS : No acceptable methods");
        }
        
        /*
         * ★ 3.身份验证，服务端向代理端发送认证信息(目前仅实现了对用户名/密码的验证服务)
         * ★ 4.代理端对身份验证的响应
         */
        if(!authenticate(data[1], in, out)) {
            throw new SocketException("SOCKS : authentication failed");
        }
        
        
        /*
         * ★★ 定义一个[远程地址结构]
         *
         *        地址类型 地址信息
         * 字节数    1	   未限定
         *
         * 地址类型：以下类型之一
         * 　　　　　0x01 => IPv4地址
         * 　　　　　0x02 => 域名
         * 　　　　　0x03 => IPv6地址
         * 地址信息：不同的地址类型，其地址信息的长度也不同
         * 　　　　　(1)IPv4地址，通常占用4个字节
         * 　　　　　(2)域名，1个字节的域名长度，后跟(1–255)个字节的域名
         * 　　　　　(3)IPv6地址，通常占用16个字节
         *
         *
         * ★ 5.服务端向代理端发送连接请求，发生在身份验证之后
         *
         *        版本号 连接命令 [保留值] 远程地址 远程端口
         * 字节数    1	   1        1     未限定     2
         *
         * 版本号　：0x05，SOCKS5协议
         * 连接命令：限定认证信息中使用的认证方法数量
         * 　　　　　0x01 => 建立TCP连接
         * 　　　　　0x02 => 建立TCP端口绑定
         * 　　　　　0x03 => 关联UDP端口
         * [保留值]：该值做保留值，目前总是0
         * 远程地址：远程服务端的地址，参考上面定义的[远程地址结构]
         * 远程端口：远程服务端的端口
         */
        
        /* We're OK. Let's issue the BIND command */
        out.write(PROTO_VERS);  // 版本号
        out.write(BIND);        // 绑定命令
        out.write(0);           // [保留值]
        
        
        /* 接下来，传输远程地址信息 */
        
        // (2)域名，1个字节的域名长度，后跟(1–255)个字节的域名
        if(saddr.isUnresolved()) {
            // 发送地址类型：域名
            out.write(DOMAIN_NAME);
            // 发送主机名长度
            out.write(saddr.getHostName().length());
            try {
                // 发送主机名
                out.write(saddr.getHostName().getBytes(StandardCharsets.ISO_8859_1));
            } catch(UnsupportedEncodingException uee) {
                assert false;
            }
            
            // (1)IPv4地址，通常占用4个字节
        } else if(saddr.getAddress() instanceof Inet4Address) {
            // 发送地址类型：IP4地址
            out.write(IPV4);
            // 发送IP4地址
            out.write(saddr.getAddress().getAddress());
            
            // (3)IPv6地址，通常占用16个字节
        } else if(saddr.getAddress() instanceof Inet6Address) {
            // 发送地址类型：IP6地址
            out.write(IPV6);
            // 发送IP6地址
            out.write(saddr.getAddress().getAddress());
        } else {
            cmdsock.close();
            throw new SocketException("unsupported address type : " + saddr);
        }
        
        // 按大端法传输服务端的端口号
        out.write((saddr.getPort() >> 8) & 0xff);
        out.write((saddr.getPort() >> 0) & 0xff);
        
        // 完成发送
        out.flush();
        
        /*
         * ★ 6.服务端接收代理端的响应
         *
         *        版本号 响应状态 [保留值] 远程地址 远程端口
         * 字节数    1	   1        1     未限定     2
         *
         * 版本号　：0x05，SOCKS5协议
         * 响应状态：状态码如下
         * 　　　　　0x00 => request granted(请求被允许)
         * 　　　　　0x01 => general failure
         * 　　　　　0x02 => connection not allowed by ruleset
         * 　　　　　0x03 => network unreachable
         * 　　　　　0x04 => host unreachable
         * 　　　　　0x05 => connection refused by destination host
         * 　　　　　0x06 => TTL expired
         * 　　　　　0x07 => command not supported / protocol error
         * 　　　　　0x08 => address type not supported
         * [保留值]：该值做保留值，目前总是0
         * 远程地址：远程服务端的地址，参考上面定义的[远程地址结构]
         * 远程端口：远程服务端的端口
         */
        
        data = new byte[4];
        i = readSocksReply(in, data);
        
        SocketException ex = null;
        
        switch(data[1]) {
            case REQUEST_OK:
                byte[] addr;
                int nport;
                int len;
                
                // success!
                switch(data[3]) {
                    case IPV4: {
                        // 获取IP4地址
                        addr = new byte[4];
                        i = readSocksReply(in, addr);
                        if(i != 4) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        
                        // 获取端口号信息
                        data = new byte[2];
                        i = readSocksReply(in, data);
                        if(i != 2) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        // 获取端口号
                        nport = ((int) data[0] & 0xff) << 8;
                        nport += ((int) data[1] & 0xff);
                        
                        // 记录服务端本地地址
                        external_address = new InetSocketAddress(new Inet4Address("", addr), nport);
                        
                        break;
                    }
                    case DOMAIN_NAME: {
                        len = data[1];
                        
                        // 获取主机名
                        byte[] host = new byte[len];
                        i = readSocksReply(in, host);
                        if(i != len) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        
                        // 获取端口号信息
                        data = new byte[2];
                        i = readSocksReply(in, data);
                        if(i != 2) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        // 获取端口号
                        nport = ((int) data[0] & 0xff) << 8;
                        nport += ((int) data[1] & 0xff);
                        
                        // 记录服务端本地地址
                        external_address = new InetSocketAddress(new String(host), nport);
                        
                        break;
                    }
                    case IPV6: {
                        len = data[1];
                        
                        // 获取IP6地址
                        addr = new byte[len];
                        i = readSocksReply(in, addr);
                        if(i != len) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        
                        // 获取端口号信息
                        data = new byte[2];
                        i = readSocksReply(in, data);
                        if(i != 2) {
                            throw new SocketException("Reply from SOCKS server badly formatted");
                        }
                        // 获取端口号
                        nport = ((int) data[0] & 0xff) << 8;
                        nport += ((int) data[1] & 0xff);
                        
                        // 记录服务端本地地址
                        external_address = new InetSocketAddress(new Inet6Address("", addr), nport);
                        
                        break;
                    }
                }
                break;
            case GENERAL_FAILURE:
                ex = new SocketException("SOCKS server general failure");
                break;
            case NOT_ALLOWED:
                ex = new SocketException("SOCKS: Bind not allowed by ruleset");
                break;
            case NET_UNREACHABLE:
                ex = new SocketException("SOCKS: Network unreachable");
                break;
            case HOST_UNREACHABLE:
                ex = new SocketException("SOCKS: Host unreachable");
                break;
            case CONN_REFUSED:
                ex = new SocketException("SOCKS: Connection refused");
                break;
            case TTL_EXPIRED:
                ex = new SocketException("SOCKS: TTL expired");
                break;
            case CMD_NOT_SUPPORTED:
                ex = new SocketException("SOCKS: Command not supported");
                break;
            case ADDR_TYPE_NOT_SUP:
                ex = new SocketException("SOCKS: address type not supported");
                break;
        }
        
        if(ex != null) {
            in.close();
            out.close();
            cmdsock.close();
            cmdsock = null;
            throw ex;
        }
        
        cmdIn = in;
        cmdOut = out;
    }
    
    /*
     * [反向代理]
     *
     * 先将[服务端Socket]绑定到指定的地址；随后，让服务端与反向代理进行通信验证。
     * baddr是[服务端Socket]绑定的本地地址，在通信验证中，会被服务端发送(告知)给反向代理。
     *
     * SOCKS4协议，此处的C端是服务端，S端是代理端：
     *
     * ★ 1.服务端需向代理端发送以下格式的信息：
     *
     *        版本号 连接命令 本地端口 本地地址   ID
     * 字节数    1	    1	    2	     4	  未限定
     *
     * 版本号　：SOCKS版本号，此处应当发送0x04
     * 连接命令：0x01 => 建立TCP连接
     * 　　　　　0x02 => 建立TCP端口绑定
     * 本地端口：服务端本地的端口，遵循网络字节序
     * 本地地址：服务端本地的地址，遵循网络字节序
     * ID　　　：用户设定的ID，比如主机名，长度可变
     *
     *
     * ★ 2.代理端需要向服务端响应以下格式的信息：
     *
     *        版本号 响应状态码 本地端口 本地地址
     * 字节数    1	    1	    2	     4
     *
     * 版本号　　：如果未设置，就返回0；如果设置了，就返回4
     * 响应状态码：0x5A => Request granted.
     * 　　　　　　0x5B => Request rejected or failed.
     * 　　　　　　0x5C => Request failed because client is not running identd (or not reachable from server).
     * 　　　　　　0x5D => Request failed because client's identd could not confirm the user ID in the request.
     * 本地端口：服务端本地的端口，通常被忽略
     * 本地地址：服务端本地的地址，通常被忽略
     */
    private void bindV4(InputStream in, OutputStream out, InetAddress baddr, int lport) throws IOException {
        if(!(baddr instanceof Inet4Address)) {
            throw new SocketException("SOCKS V4 requires IPv4 only addresses");
        }
        
        // 将[服务端Socket]绑定到指定的IP和端口
        super.bind(baddr, lport);
        
        // 将待绑定地址存储到字节数组中
        byte[] addr = baddr.getAddress();
        
        /* Test for AnyLocal */
        // 如果待绑定地址是通配地址
        if(baddr.isAnyLocalAddress()) {
            // 获取本地IP；如果还未绑定，则依然返回通配地址
            InetAddress naddr = AccessController.doPrivileged(new PrivilegedAction<>() {
                public InetAddress run() {
                    return cmdsock.getLocalAddress();
                }
            });
            
            // 重新获取IP地址的字节形式
            addr = naddr.getAddress();
        }
        
        /*★ 1.服务端向代理端发送连接信息 */
        
        // 发送版本信息以确认代理端属性
        out.write(PROTO_VERS4);
        // 发送绑定信息
        out.write(BIND);
        
        // 发送本地端口和远程地址信息（网络传输使用大端法）
        out.write((super.getLocalPort() >> 8) & 0xff);  // 先传大端(高位)
        out.write((super.getLocalPort() >> 0) & 0xff);  // 再传小端(低位)
        out.write(addr);                                // 传输本地地址
        
        // 获取用户主机名称，如kang
        String userName = getUserName();
        try {
            // 将主机信息作为ID发送出去
            out.write(userName.getBytes(StandardCharsets.ISO_8859_1));
        } catch(java.io.UnsupportedEncodingException uee) {
            assert false;
        }
        
        // 发送完毕
        out.write(0);
        out.flush();
        
        
        /*★ 2.服务端从代理端接收响应 */
        
        byte[] data = new byte[8];
        int n = readSocksReply(in, data);
        if(n != 8) {
            // 固定返回8个字节
            throw new SocketException("Reply from SOCKS server has bad length: " + n);
        }
        
        // data[0]代表协议版本
        if(data[0] != 0 && data[0] != 4) {
            throw new SocketException("Reply from SOCKS server has bad version");
        }
        
        SocketException ex = null;
        switch(data[1]) {
            case 0x5A:
                // Success! 记录服务端的本地地址
                external_address = new InetSocketAddress(baddr, lport);
                break;
            case 0x5B:
                ex = new SocketException("SOCKS request rejected");
                break;
            case 0x5C:
                ex = new SocketException("SOCKS server couldn't reach destination");
                break;
            case 0x5D:
                ex = new SocketException("SOCKS authentication failed");
                break;
            default:
                ex = new SocketException("Reply from SOCKS server contains bad status");
                break;
        }
        
        if(ex != null) {
            in.close();
            out.close();
            throw ex;
        }
    }
    
    
    /**
     * Accepts a connection from a specific host.
     *
     * @param s     the accepted connection.
     * @param saddr the socket address of the host we do accept connection from
     *
     * @throws IOException if an I/O error occurs when accepting the connection.
     */
    /*
     * [反向代理]
     *
     * 将[服务端Socket]绑定到指定的地址，并且获取到[服务端Socket(通信)]的"Socket"委托impl
     *
     * 注：实际使用时，需要视情形而实现该方法
     */
    protected void acceptFrom(SocketImpl impl, InetSocketAddress saddr) throws IOException {
        // 确保[服务端Socket(通信)]已经存在
        if(cmdsock == null) {
            // Not a Socks ServerSocket.
            return;
        }
        
        InputStream in = cmdIn;
        
        /*
         * Sends the "SOCKS BIND" request
         *
         * 对[服务端Socket]进行【bind】操作，期间应当与代理端建立连接
         */
        socksBind(saddr);
        
        /*
         * ★ 服务端接收代理端的响应
         *
         *        版本号 响应状态 [保留值] 远程地址 远程端口
         * 字节数    1	   1        1     未限定     2
         *
         * 版本号　：0x05，SOCKS5协议
         * 响应状态：状态码如下
         * 　　　　　0x00 => request granted(请求被允许)
         * 　　　　　0x01 => general failure
         * 　　　　　0x02 => connection not allowed by ruleset
         * 　　　　　0x03 => network unreachable
         * 　　　　　0x04 => host unreachable
         * 　　　　　0x05 => connection refused by destination host
         * 　　　　　0x06 => TTL expired
         * 　　　　　0x07 => command not supported / protocol error
         * 　　　　　0x08 => address type not supported
         * [保留值]：该值做保留值，目前总是0
         * 远程地址：远程服务端的地址，参考上面定义的[远程地址结构]
         * 远程端口：远程服务端的端口
         */
        
        in.read();           // 版本号
        int rep = in.read(); // 响应状态
        in.read();           // [保留值]
        
        SocketException ex = null;
        
        // 记录服务端的地址
        InetSocketAddress real_end = null;
        
        switch(rep) {
            case REQUEST_OK:
                byte[] addr;
                int nport;
                
                // 判断地址类型
                switch(in.read()) {
                    case IPV4: {
                        // 获取IP4地址
                        addr = new byte[4];
                        readSocksReply(in, addr);
                        
                        // 获取端口号
                        nport = in.read() << 8;
                        nport += in.read();
                        
                        // 构造socket地址
                        real_end = new InetSocketAddress(new Inet4Address("", addr), nport);
                        
                        break;
                    }
                    case IPV6: {
                        // 获取IP6地址
                        addr = new byte[16];
                        readSocksReply(in, addr);
                        
                        // 获取端口号
                        nport = in.read() << 8;
                        nport += in.read();
                        
                        // 构造socket地址
                        real_end = new InetSocketAddress(new Inet6Address("", addr), nport);
                        
                        break;
                    }
                    case DOMAIN_NAME: {
                        // 获取主机名长度
                        int len = in.read();
                        
                        // 获取主机名
                        addr = new byte[len];
                        readSocksReply(in, addr);
                        
                        // 获取端口号
                        nport = in.read() << 8;
                        nport += in.read();
                        
                        // 构造socket地址
                        real_end = new InetSocketAddress(new String(addr), nport);
                        
                        break;
                    }
                }
                break;
            case GENERAL_FAILURE:
                ex = new SocketException("SOCKS server general failure");
                break;
            case NOT_ALLOWED:
                ex = new SocketException("SOCKS: Accept not allowed by ruleset");
                break;
            case NET_UNREACHABLE:
                ex = new SocketException("SOCKS: Network unreachable");
                break;
            case HOST_UNREACHABLE:
                ex = new SocketException("SOCKS: Host unreachable");
                break;
            case CONN_REFUSED:
                ex = new SocketException("SOCKS: Connection refused");
                break;
            case TTL_EXPIRED:
                ex = new SocketException("SOCKS: TTL expired");
                break;
            case CMD_NOT_SUPPORTED:
                ex = new SocketException("SOCKS: Command not supported");
                break;
            case ADDR_TYPE_NOT_SUP:
                ex = new SocketException("SOCKS: address type not supported");
                break;
        }
        
        if(ex != null) {
            cmdIn.close();
            cmdOut.close();
            cmdsock.close();
            cmdsock = null;
            throw ex;
        }
        
        /*
         * This is where we have to do some fancy stuff.
         * The datastream from the socket "accepted" by the proxy will come through the cmdSocket.
         * So we have to swap the socketImpls.
         */
        if(impl instanceof SocksSocketImpl) {
            ((SocksSocketImpl) impl).external_address = real_end;
        }
        
        // 获取[服务端Socket(通信)]的"Socket委托"
        SocketImpl cmdImpl = cmdsock.getImpl();
        
        // 制作[服务端Socket(通信)]的副本
        if(impl instanceof PlainSocketImpl) {
            PlainSocketImpl psi = (PlainSocketImpl) impl;
            psi.setInputStream((SocketInputStream) in);
            psi.setFileDescriptor(cmdImpl.getFileDescriptor());
            psi.setAddress(cmdImpl.getInetAddress());
            psi.setPort(cmdImpl.getPort());
            psi.setLocalPort(cmdImpl.getLocalPort());
        } else {
            impl.fd = cmdImpl.fd;
            impl.address = cmdImpl.address;
            impl.port = cmdImpl.port;
            impl.localport = cmdImpl.localport;
        }
        
        /*
         * Need to do that so that the socket won't be closed.
         * when the ServerSocket is closed by the user.
         * It kinds of detaches the Socket because it is now used elsewhere.
         */
        // 已经被外界获取到cmdsock的副本后，可以置空cmdsock了
        cmdsock = null;
    }
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭socket连接
    @Override
    protected void close() throws IOException {
        if(cmdsock != null) {
            cmdsock.close();
        }
        
        cmdsock = null;
        
        super.close();
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
     * 存在代理时，返回服务端IP；否则，视以下情形而定：
     *
     * [客户端Socket] 　　 : 远程IP，即服务端IP；如果主动为[客户端Socket]绑定了本地IP，则此字段先被设置为本地IP，之后在连接中被覆盖为远程IP
     * [服务端Socket(监听)]: 本地IP，即服务端IP
     * [服务端Socket(通信)]: 远程IP，即客户端IP
     */
    @Override
    protected InetAddress getInetAddress() {
        // 存在代理时，返回服务端IP
        if(external_address != null) {
            return external_address.getAddress();
        }
        
        return super.getInetAddress();
    }
    
    // 返回本地端口
    @Override
    protected int getLocalPort() {
        // 客户端，返回客户端的端口
        if(socket != null) {
            return super.getLocalPort();
        }
        
        // 存在代理，返回服务端端口
        if(external_address != null) {
            return external_address.getPort();
        }
        
        // 服务端，返回服务端的端口
        return super.getLocalPort();
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
     * 存在代理时，返回服务端端口；否则，视以下情形而定：
     *
     * [客户端Socket] 　　 : 远程端口，即服务端的端口
     * [服务端Socket(监听)]: 未设置
     * [服务端Socket(通信)]: 远程端口，即客户端的端口
     */
    @Override
    protected int getPort() {
        // 存在代理时，返回服务端端口
        if(external_address != null) {
            return external_address.getPort();
        }
        
        return super.getPort();
    }
    
    /*▲ 地址 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 客户端/服务端从代理端获取响应数据；返回值指示响应数据的字节数
    private int readSocksReply(InputStream in, byte[] data) throws IOException {
        return readSocksReply(in, data, 0L);
    }
    
    // 客户端/服务端从代理端获取响应数据，允许指定超时；返回值指示响应数据的字节数
    private int readSocksReply(InputStream in, byte[] data, long deadlineMillis) throws IOException {
        int len = data.length;
        int received = 0;
        
        while(received<len) {
            int count;
            try {
                count = ((SocketInputStream) in).read(data, received, len - received, remainingMillis(deadlineMillis));
            } catch(SocketTimeoutException e) {
                throw new SocketTimeoutException("Connect timed out");
            }
            if(count<0) {
                throw new SocketException("Malformed reply from SOCKS server");
            }
            received += count;
        }
        
        return received;
    }
    
    // 获取用户主机名称，如kang
    private String getUserName() {
        String userName = "";
        if(applicationSetProxy) {
            try {
                userName = System.getProperty("user.name");
            } catch(SecurityException se) { /* swallow Exception */ }
        } else {
            userName = StaticProperty.userName();
        }
        
        return userName;
    }
    
    // 判断是否使用了SOCKS4代理
    private static boolean useV4(Proxy proxy) {
        if(proxy instanceof SocksProxy && ((SocksProxy) proxy).protocolVersion() == 4) {
            return true;
        }
        
        return DefaultProxySelector.socksProxyVersion() == 4;
    }
    
    // 设置使用SOCKS4代理
    void setV4() {
        useV4 = true;
    }
    
    // 计算超时
    private static int remainingMillis(long deadlineMillis) throws IOException {
        if(deadlineMillis == 0L) {
            return 0;
        }
        
        final long remaining = deadlineMillis - System.currentTimeMillis();
        if(remaining>0) {
            return (int) remaining;
        }
        
        throw new SocketTimeoutException();
    }
    
    /**
     * Provides the authentication machanism required by the proxy.
     */
    /*
     * ★ 3.身份验证，客户端向代理端发送认证信息(目前仅实现了对用户名/密码的验证服务)
     * ★ 4.代理端对身份验证的响应
     */
    private boolean authenticate(byte method, InputStream in, BufferedOutputStream out) throws IOException {
        return authenticate(method, in, out, 0L);
    }
    
    /*
     * ★ 3.身份验证，客户端向代理端发送认证信息(目前仅实现了对用户名/密码的验证服务)
     * ★ 4.代理端对身份验证的响应
     */
    private boolean authenticate(byte method, InputStream in, BufferedOutputStream out, long deadlineMillis) throws IOException {
        /* No Authentication required. We're done then! */
        // (1)不需要验证的话直接返回true，表示通过
        if(method == NO_AUTH) {
            return true;
        }
        
        /*
         * User/Password authentication. Try, in that order :
         * - The application provided Authenticator, if any
         * - the user.name & no password (backward compatibility behavior).
         */
        // (2)使用用户名/密码验证
        if(method == USER_PASSW) {
            String userName;
            String password = null;
            
            // 获取代理主机名称
            final InetAddress addr = InetAddress.getByName(server);
            
            // 获取认证信息(用户名/密码)
            PasswordAuthentication pw = AccessController.doPrivileged(new PrivilegedAction<>() {
                public PasswordAuthentication run() {
                    return Authenticator.requestPasswordAuthentication(server, addr, serverPort, "SOCKS5", "SOCKS authentication", null);
                }
            });
            
            // 如果获取到认证信息，则记录其用户名和密码
            if(pw != null) {
                userName = pw.getUserName();
                password = new String(pw.getPassword());
            } else {
                // 没有获取到认证信息时，此处获取用户主机名称，如kang
                userName = StaticProperty.userName();
            }
            
            if(userName == null) {
                return false;
            }
            
            /*
             * ★ 3.身份验证，客户端向代理端发送认证信息(目前仅实现了对用户名/密码的验证服务)
             *
             *        版本号 ID长度 ID(用户名) 密码长度  密码
             * 字节数    1	   1    (1-255)		 1	  (1-255)
             *
             * 版本号　　：设置为0x01，指示用于当前版本的用户名/密码认证
             * ID长度　　：通常指用户名的长度
             * ID(用户名)：通常是用户名
             * 密码长度　：密码长度
             * 密码　　　：密码
             */
            out.write(1);                   // 发送版本号
            out.write(userName.length());   // 发送ID长度
            try {
                // 发送ID(用户名)
                out.write(userName.getBytes(StandardCharsets.ISO_8859_1));
            } catch(java.io.UnsupportedEncodingException uee) {
                assert false;
            }
            
            if(password == null) {
                // 不存在密码时，仅写入一个0
                out.write(0);
            } else {
                // 发送密码长度
                out.write(password.length());
                try {
                    // 发送密码
                    out.write(password.getBytes(StandardCharsets.ISO_8859_1));
                } catch(UnsupportedEncodingException uee) {
                    assert false;
                }
            }
            
            out.flush();
            
            /*
             * ★ 4.代理端对身份验证的响应
             *
             *        版本号 状态码
             * 字节数    1	   1
             *
             * 版本号：设置为0x01，指示用于当前版本的用户名/密码认证
             * 状态码：设置为0x00表示认证成功，否则，认证失败，必须关闭连接
             */
            byte[] data = new byte[2];
            int i = readSocksReply(in, data, deadlineMillis);
            
            /* Authentication succeeded */
            // 认证成功
            if(i == 2 && data[1] == 0) {
                return true;
            }
            
            /* RFC 1929 specifies that the connection MUST be closed if authentication fails */
            // 认证失败时关闭连接
            out.close();
            in.close();
            
            return false;
        }
        
        return false;
    }
    
}
