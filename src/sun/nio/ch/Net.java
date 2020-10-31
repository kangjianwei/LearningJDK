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
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import sun.net.ext.ExtendedSocketOptions;
import sun.security.action.GetPropertyAction;

// Socket的IO工具类：大部分Socket操作的底层实现
public class Net {
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private Net() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 创建Socket，并返回其在Java层的文件描述符
     * 如果平台支持IP6协议，则创建出Socket也支持IP6
     *
     * stream：指示使用TCP(true)还是UDP(false)通信
     *
     * 注：该方法目前用在创建[客户端Socket]中
     */
    static FileDescriptor socket(boolean stream) throws IOException {
        return socket(UNSPEC, stream);
    }
    
    /*
     * 创建Socket，并返回其在Java层的文件描述符
     *
     * family: 指定Socket兼容的协议
     * stream：指示使用TCP(true)还是UDP(false)通信
     *
     * 注：该方法目前用在创建TCP-[客户端Socket]或UDP-Socket中
     */
    static FileDescriptor socket(ProtocolFamily family, boolean stream) throws IOException {
        // 确定socket需要兼容的IP协议
        boolean preferIPv6 = isIPv6Available() && (family != StandardProtocolFamily.INET);
        
        // 创建客户端socket（默认不可重用），返回其文件描述符的值
        int fdv = socket0(preferIPv6, stream, false, fastLoopback);
        
        // 将本地(native层)的文件描述符fdv包装为一个Java层的文件描述符对象后返回
        return IOUtil.newFD(fdv);
    }
    
    /*
     * 创建Socket，并返回其在Java层的文件描述符
     *
     * stream：指示使用TCP(true)还是UDP(false)通信
     *
     * 注：该方法目前用在创建[服务端Socket(监听)]中
     */
    static FileDescriptor serverSocket(boolean stream) {
        // 创建服务端socket（默认可重用），返回其文件描述符的值
        int fdv = socket0(isIPv6Available(), stream, true, fastLoopback);
        
        // 将本地(native层)的文件描述符fdv包装为一个Java层的文件描述符对象后返回
        return IOUtil.newFD(fdv);
    }
    
    /** Due to oddities SO_REUSEADDR on windows reuse is ignored */
    /*
     * 创建socket，并返回其在Java层的文件描述符
     *
     * preferIPv6：是否支持IP6协议
     * stream    ：指示使用TCP(true)还是UDP(false)通信
     * reuse     ：指示socket是否可以重用，参考SO_REUSEADDR参数
     *
     * SO_REUSEADDR可以用在以下四种情况下：
     * 1> 当已有socket1处于TIME_WAIT状态，且新启动的socket2需要占用与socket1相同的地址和端口，则该程序就要用到该选项。
     * 2> SO_REUSEADDR允许同一port上启动同一服务器的多个实例(多个进程)。但每个实例绑定的IP地址是不能相同的。在有多块网卡或用IP Alias技术的机器可以测试这种情况。
     * 3> SO_REUSEADDR允许单个进程绑定相同的端口到多个socket上，但每个socket绑定的ip地址不同。这和2很相似，区别请看UNPv1。
     * 4> SO_REUSEADDR允许完全相同的地址和端口的重复绑定。但这只用于UDP的组播，不用于TCP。
     * 详见《Unix网络编程》卷一
     */
    private static native int socket0(boolean preferIPv6, boolean stream, boolean reuse, boolean fastLoopback);
    
    
    /*
     * 为[客户端Socket]/[服务端Socket(监听)]绑定IP地址与端口号
     *
     * fd  : 待绑定Socket的文件描述符
     * addr: 需要绑定到的IP
     * port: 需要绑定到的端口
     */
    public static void bind(FileDescriptor fd, InetAddress addr, int port) throws IOException {
        bind(UNSPEC, fd, addr, port);
    }
    
    /*
     * 为指定协议族的Socket绑定IP地址与端口号
     *
     * family: Socket支持的IP协议
     * fd    : 待绑定Socket的文件描述符
     * addr  : 需要绑定到的IP
     * port  : 需要绑定到的端口
     */
    static void bind(ProtocolFamily family, FileDescriptor fd, InetAddress addr, int port) throws IOException {
        // 确定socket需要兼容的IP协议
        boolean preferIPv6 = isIPv6Available() && (family != StandardProtocolFamily.INET);
        
        // 执行绑定操作，exclusiveBind指示是否使用独占式绑定
        bind0(fd, preferIPv6, exclusiveBind, addr, port);
    }
    
    private static native void bind0(FileDescriptor fd, boolean preferIPv6, boolean useExclBind, InetAddress addr, int port) throws IOException;
    
    
    // 使[服务端Socket(监听)]开启监听，backlog代表允许积压的待处理连接数
    static native void listen(FileDescriptor fd, int backlog) throws IOException;
    
    
    // 客户端Socket向指定地址处的远端Socket发起连接（如果采用阻塞模式，会一直等待，直到成功或出现异常）
    static int connect(FileDescriptor fd, InetAddress remote, int remotePort) throws IOException {
        return connect(UNSPEC, fd, remote, remotePort);
    }
    
    // 客户端Socket向指定地址处的指定协议族的远端Socket发起连接（如果采用阻塞模式，会一直等待，直到成功或出现异常）
    static int connect(ProtocolFamily family, FileDescriptor fd, InetAddress remote, int remotePort) throws IOException {
        // 确定socket需要兼容的IP协议
        boolean preferIPv6 = isIPv6Available() && (family != StandardProtocolFamily.INET);
        
        return connect0(preferIPv6, fd, remote, remotePort);
    }
    
    // 客户端Socket向指定地址处的远端Socket发起连接（如果采用阻塞模式，会一直等待，直到成功或出现异常）
    private static native int connect0(boolean preferIPv6, FileDescriptor fd, InetAddress remote, int remotePort) throws IOException;
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 组播操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Join IPv4 multicast group
     */
    /*
     * 将fd处组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据；interf是用来接收消息的网络接口(网卡)的IP
     * 注：如果source不为0，则会过滤消息，即只处理源地址是source的那些数据包
     */
    static int join4(FileDescriptor fd, int group, int interf, int source) throws IOException {
        return joinOrDrop4(true, fd, group, interf, source);
    }
    
    /**
     * Drop membership of IPv4 multicast group
     */
    // 将fd处的组播Socket从所在的组播小组中移除(处理IP4地址)
    static void drop4(FileDescriptor fd, int group, int interf, int source) throws IOException {
        joinOrDrop4(false, fd, group, interf, source);
    }
    
    /**
     * Block IPv4 source
     */
    // 屏蔽source处的消息(处理IP4地址)
    static int block4(FileDescriptor fd, int group, int interf, int source) throws IOException {
        return blockOrUnblock4(true, fd, group, interf, source);
    }
    
    /**
     * Unblock IPv6 source
     */
    // 解除对source的屏蔽(处理IP4地址)
    static void unblock4(FileDescriptor fd, int group, int interf, int source) throws IOException {
        blockOrUnblock4(false, fd, group, interf, source);
    }
    
    
    /**
     * Join IPv6 multicast group
     */
    /*
     * 将fd处组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据；index是用来接收消息的网络接口(网卡)的索引
     * 注：如果source不为0，则会过滤消息，即只处理源地址是source的那些数据包
     *
     * 与join4()不同的是，这里面对的是IP6地址
     */
    static int join6(FileDescriptor fd, byte[] group, int index, byte[] source) throws IOException {
        return joinOrDrop6(true, fd, group, index, source);
    }
    
    /**
     * Drop membership of IPv6 multicast group
     */
    // 将fd处的组播Socket从所在的组播小组中移除(处理IP6地址)
    static void drop6(FileDescriptor fd, byte[] group, int index, byte[] source) throws IOException {
        joinOrDrop6(false, fd, group, index, source);
    }
    
    /**
     * Block IPv6 source
     */
    // 屏蔽source处的消息(处理IP6地址)
    static int block6(FileDescriptor fd, byte[] group, int index, byte[] source) throws IOException {
        return blockOrUnblock6(true, fd, group, index, source);
    }
    
    /**
     * Unblock IPv6 source
     */
    // 解除对source的屏蔽(处理IP6地址)
    static void unblock6(FileDescriptor fd, byte[] group, int index, byte[] source) throws IOException {
        blockOrUnblock6(false, fd, group, index, source);
    }
    
    private static native int joinOrDrop4(boolean join, FileDescriptor fd, int group, int interf, int source) throws IOException;
    
    private static native int joinOrDrop6(boolean join, FileDescriptor fd, byte[] group, int index, byte[] source) throws IOException;
    
    private static native int blockOrUnblock4(boolean block, FileDescriptor fd, int group, int interf, int source) throws IOException;
    
    static native int blockOrUnblock6(boolean block, FileDescriptor fd, byte[] group, int index, byte[] source) throws IOException;
    
    /*▲ 组播操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public static final int SHUT_RD = 0;  // 代表通道的读功能
    public static final int SHUT_WR = 1;  // 代表通道的写功能
    public static final int SHUT_RDWR = 2;  // 代表通道的读写功能
    
    // 关闭通道的读/写功能，参见上述参数
    static native void shutdown(FileDescriptor fd, int how) throws IOException;
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 从指定的Socket文件描述符中解析出本地Socket地址后返回
    public static InetSocketAddress localAddress(FileDescriptor fd) throws IOException {
        return new InetSocketAddress(localInetAddress(fd), localPort(fd));
    }
    
    // 从指定的Socket文件描述符中解析本地地址
    private static native InetAddress localInetAddress(FileDescriptor fd) throws IOException;
    
    // 从指定的Socket文件描述符中解析本地端口
    private static native int localPort(FileDescriptor fd) throws IOException;
    
    
    // 从指定的Socket文件描述符中解析出远程Socket地址后返回
    static InetSocketAddress remoteAddress(FileDescriptor fd) throws IOException {
        return new InetSocketAddress(remoteInetAddress(fd), remotePort(fd));
    }
    
    // 从指定的Socket文件描述符中解析远程地址
    private static native InetAddress remoteInetAddress(FileDescriptor fd) throws IOException;
    
    // 从指定的Socket文件描述符中解析远程端口
    private static native int remotePort(FileDescriptor fd) throws IOException;
    
    
    /**
     * Returns the local address after performing a SecurityManager#checkConnect.
     */
    // 对指定的地址进行安全校验
    static InetSocketAddress getRevealedLocalAddress(InetSocketAddress addr) {
        SecurityManager sm = System.getSecurityManager();
        if(addr == null || sm == null) {
            return addr;
        }
        
        try {
            sm.checkConnect(addr.getAddress().getHostAddress(), -1);
            // Security check passed
        } catch(SecurityException e) {
            // Return loopback address only if security check fails
            addr = getLoopbackAddress(addr.getPort());
        }
        
        return addr;
    }
    
    /*▲ 地址信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 未指定的协议族
    static final ProtocolFamily UNSPEC = new ProtocolFamily() {
        public String name() {
            return "UNSPEC";
        }
    };
    
    // 扩展的Socket参数
    static final ExtendedSocketOptions extendedOptions = ExtendedSocketOptions.getInstance();
    
    private static volatile boolean checkedIPv6;          // 是否检查过isIPv6Available参数
    private static volatile boolean isIPv6Available;      // 是否可以支持IP6协议
    private static volatile boolean checkedReusePort;     // 是否检查过isReusePortAvailable参数
    private static volatile boolean isReusePortAvailable; // 是否允许多个socket绑定相同的地址和端口对
    
    
    // 设置指定名称的Socket配置参数
    static void setSocketOption(FileDescriptor fd, ProtocolFamily family, SocketOption<?> name, Object value) throws IOException {
        if(value == null) {
            throw new IllegalArgumentException("Invalid option value");
        }
        
        // only simple values supported by this method
        Class<?> type = name.type();
        
        if(extendedOptions.isOptionSupported(name)) {
            extendedOptions.setOption(fd, name, value);
            return;
        }
        
        if(type != Integer.class && type != Boolean.class) {
            throw new AssertionError("Should not reach here");
        }
        
        // special handling
        if(name == StandardSocketOptions.SO_RCVBUF || name == StandardSocketOptions.SO_SNDBUF) {
            int i = (Integer) value;
            if(i<0) {
                throw new IllegalArgumentException("Invalid send/receive buffer size");
            }
        }
        
        if(name == StandardSocketOptions.SO_LINGER) {
            int i = (Integer) value;
            if(i<0) {
                value = -1;
            }
            if(i>65535) {
                value = 65535;
            }
        }
        
        if(name == StandardSocketOptions.IP_TOS) {
            int i = (Integer) value;
            if(i<0 || i>255) {
                throw new IllegalArgumentException("Invalid IP_TOS value");
            }
        }
        
        if(name == StandardSocketOptions.IP_MULTICAST_TTL) {
            int i = (Integer) value;
            if(i<0 || i>255) {
                throw new IllegalArgumentException("Invalid TTL/hop value");
            }
        }
        
        // map option name to platform level/name
        OptionKey key = SocketOptionRegistry.findOption(name, family);
        if(key == null) {
            throw new AssertionError("Option not found");
        }
        
        int arg;
        if(type == Integer.class) {
            arg = (Integer) value;
        } else {
            boolean b = (Boolean) value;
            arg = (b) ? 1 : 0;
        }
        
        boolean mayNeedConversion = (family == UNSPEC);
        boolean isIPv6 = (family == StandardProtocolFamily.INET6);
        
        setIntOption0(fd, mayNeedConversion, key.level(), key.name(), arg, isIPv6);
    }
    
    // 获取指定名称的Socket配置参数
    static Object getSocketOption(FileDescriptor fd, ProtocolFamily family, SocketOption<?> name) throws IOException {
        Class<?> type = name.type();
        
        if(extendedOptions.isOptionSupported(name)) {
            return extendedOptions.getOption(fd, name);
        }
        
        // only simple values supported by this method
        if(type != Integer.class && type != Boolean.class) {
            throw new AssertionError("Should not reach here");
        }
        
        // map option name to platform level/name
        OptionKey key = SocketOptionRegistry.findOption(name, family);
        if(key == null) {
            throw new AssertionError("Option not found");
        }
        
        boolean mayNeedConversion = (family == UNSPEC);
        int value = getIntOption0(fd, mayNeedConversion, key.level(), key.name());
        
        if(type == Integer.class) {
            return Integer.valueOf(value);
        } else {
            return (value == 0) ? Boolean.FALSE : Boolean.TRUE;
        }
    }
    
    /**
     * Tells whether SO_REUSEPORT is supported.
     */
    // 是否支持重用Socket地址
    static boolean isReusePortAvailable() {
        if(!checkedReusePort) {
            isReusePortAvailable = isReusePortAvailable0();
            checkedReusePort = true;
        }
        
        return isReusePortAvailable;
    }
    
    /**
     * Returns true if exclusive binding is on
     */
    static boolean useExclusiveBind() {
        return exclusiveBind;
    }
    
    private static native int getIntOption0(FileDescriptor fd, boolean mayNeedConversion, int level, int opt) throws IOException;
    
    private static native void setIntOption0(FileDescriptor fd, boolean mayNeedConversion, int level, int opt, int arg, boolean isIPv6) throws IOException;
    
    private static native boolean isReusePortAvailable0();
    
    static native void setInterface4(FileDescriptor fd, int interf) throws IOException;
    
    static native int getInterface4(FileDescriptor fd) throws IOException;
    
    static native void setInterface6(FileDescriptor fd, int index) throws IOException;
    
    static native int getInterface6(FileDescriptor fd) throws IOException;
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 异常 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    static void translateToSocketException(Exception x) throws SocketException {
        if(x instanceof SocketException) {
            throw (SocketException) x;
        }
        
        Exception nx = x;
        if(x instanceof ClosedChannelException) {
            nx = new SocketException("Socket is closed");
        } else if(x instanceof NotYetConnectedException) {
            nx = new SocketException("Socket is not connected");
        } else if(x instanceof AlreadyBoundException) {
            nx = new SocketException("Already bound");
        } else if(x instanceof NotYetBoundException) {
            nx = new SocketException("Socket is not bound yet");
        } else if(x instanceof UnsupportedAddressTypeException) {
            nx = new SocketException("Unsupported address type");
        } else if(x instanceof UnresolvedAddressException) {
            nx = new SocketException("Unresolved address");
        }
        
        if(nx != x) {
            nx.initCause(x);
        }
        
        if(nx instanceof SocketException) {
            throw (SocketException) nx;
        } else if(nx instanceof RuntimeException) {
            throw (RuntimeException) nx;
        } else {
            throw new Error("Untranslated exception", nx);
        }
    }
    
    static void translateException(Exception x, boolean unknownHostForUnresolved) throws IOException {
        if(x instanceof IOException) {
            throw (IOException) x;
        }
        
        // Throw UnknownHostException from here since it cannot be thrown as a SocketException
        if(unknownHostForUnresolved && (x instanceof UnresolvedAddressException)) {
            throw new UnknownHostException();
        }
        
        translateToSocketException(x);
    }
    
    static void translateException(Exception x) throws IOException {
        translateException(x, false);
    }
    
    /*▲ 异常 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ IO事件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Event masks for the various poll system calls.
     * They will be set platform dependent in the static initializer below.
     */
    /*
     * 底层内核的Socket操作参数/事件
     * 参见SelectionKey中的JAVA层的Socket操作参数/事件
     */
    public static final short POLLIN;   // 文件描述符可读
    public static final short POLLOUT;  // 文件描述符可写（不阻塞）
    public static final short POLLERR;  // 错误
    public static final short POLLHUP;  // 挂起
    public static final short POLLNVAL; // 不合规的文件描述符
    public static final short POLLCONN; // 连接事件
    
    // 注册Net.POLLIN事件，远端发来"有可读数据"/"已连接"的消息时，会通知当前Socket
    static native int poll(FileDescriptor fd, int events, long timeout) throws IOException;
    
    static native short pollinValue();
    
    static native short polloutValue();
    
    static native short pollerrValue();
    
    static native short pollhupValue();
    
    static native short pollnvalValue();
    
    static native short pollconnValue();
    
    static {
        IOUtil.load();
        initIDs();
        
        POLLIN = pollinValue();
        POLLOUT = polloutValue();
        POLLERR = pollerrValue();
        POLLHUP = pollhupValue();
        POLLNVAL = pollnvalValue();
        POLLCONN = pollconnValue();
    }
    
    /*▲ IO事件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 检查指定的Socket地址是否合规
    public static InetSocketAddress checkAddress(SocketAddress sa) {
        if(sa == null) {
            throw new NullPointerException();
        }
        
        if(!(sa instanceof InetSocketAddress)) {
            throw new UnsupportedAddressTypeException(); // ## needs arg
        }
        
        InetSocketAddress isa = (InetSocketAddress) sa;
        // 如果为"未解析"的socket地址，抛异常
        if(isa.isUnresolved()) {
            throw new UnresolvedAddressException(); // ## needs arg
        }
        
        // 如果是未知类型的IP地址，抛异常
        InetAddress addr = isa.getAddress();
        if(!(addr instanceof Inet4Address || addr instanceof Inet6Address)) {
            throw new IllegalArgumentException("Invalid address type");
        }
        
        return isa;
    }
    
    // 检查指定的地址与协议是否匹配
    static InetSocketAddress checkAddress(SocketAddress address, ProtocolFamily family) {
        InetSocketAddress isa = checkAddress(address);
        
        if(family == StandardProtocolFamily.INET) {
            InetAddress addr = isa.getAddress();
            if(!(addr instanceof Inet4Address)) {
                throw new UnsupportedAddressTypeException();
            }
        }
        
        return isa;
    }
    
    // Socket地址类型转换
    static InetSocketAddress asInetSocketAddress(SocketAddress sa) {
        if(!(sa instanceof InetSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        
        return (InetSocketAddress) sa;
    }
    
    /**
     * Tells whether dual-IPv4/IPv6 sockets should be used.
     */
    // 判断双协议栈是否可用（主机同时运行IPv4和IPv6两套协议栈，同时支持两套协议）
    static boolean isIPv6Available() {
        if(!checkedIPv6) {
            isIPv6Available = isIPv6Available0();
            checkedIPv6 = true;
        }
        
        return isIPv6Available;
    }
    
    /**
     * Tells whether IPv6 sockets can join IPv4 multicast groups
     */
    static boolean canIPv6SocketJoinIPv4Group() {
        return canIPv6SocketJoinIPv4Group0();
    }
    
    /**
     * Tells whether {@link #join6} can be used to join an IPv4
     * multicast group (IPv4 group as IPv4-mapped IPv6 address)
     */
    static boolean canJoin6WithIPv4Group() {
        return canJoin6WithIPv4Group0();
    }
    
    /**
     * Returns any IPv4 address of the given network interface,
     * or null if the interface does not have any IPv4 addresses.
     */
    // 返回从给定的网络接口上找到的首个绑定的IP4地址
    static Inet4Address anyInet4Address(final NetworkInterface interf) {
        return AccessController.doPrivileged(new PrivilegedAction<Inet4Address>() {
            public Inet4Address run() {
                // 获取绑定到网络接口interf上的IP地址（包括IP4和IP6地址）
                Enumeration<InetAddress> addrs = interf.getInetAddresses();
                // 遍历找到的IP，返回首个遇到的IP4地址
                while(addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if(addr instanceof Inet4Address) {
                        return (Inet4Address) addr;
                    }
                }
                return null;
            }
        });
    }
    
    /**
     * Returns an InetAddress from the given IPv4 address represented as an int.
     */
    // 将int形式的IP4地址以InetAddress返回
    static InetAddress inet4FromInt(int address) {
        byte[] addr = new byte[4];
        addr[0] = (byte) ((address >>> 24) & 0xFF);
        addr[1] = (byte) ((address >>> 16) & 0xFF);
        addr[2] = (byte) ((address >>> 8) & 0xFF);
        addr[3] = (byte) (address & 0xFF);
        try {
            return InetAddress.getByAddress(addr);
        } catch(UnknownHostException uhe) {
            throw new AssertionError("Should not reach here");
        }
    }
    
    /**
     * Returns an IPv4 address as an int.
     */
    // 以int形式返回指定的IP4地址
    static int inet4AsInt(InetAddress ia) {
        if(ia instanceof Inet4Address) {
            byte[] addr = ia.getAddress();
            int address = addr[3] & 0xFF;
            address |= ((addr[2] << 8) & 0xFF00);
            address |= ((addr[1] << 16) & 0xFF0000);
            address |= ((addr[0] << 24) & 0xFF000000);
            return address;
        }
        throw new AssertionError("Should not reach here");
    }
    
    /**
     * Returns an IPv6 address as a byte array
     */
    // 将指定的IP转换为IP6地址后以字节数组的形式返回
    static byte[] inet6AsByteArray(InetAddress ia) {
        if(ia instanceof Inet6Address) {
            return ia.getAddress();
        }
        
        // need to construct IPv4-mapped address
        if(ia instanceof Inet4Address) {
            byte[] ip4address = ia.getAddress();
            byte[] address = new byte[16];
            address[10] = (byte) 0xff;
            address[11] = (byte) 0xff;
            address[12] = ip4address[0];
            address[13] = ip4address[1];
            address[14] = ip4address[2];
            address[15] = ip4address[3];
            return address;
        }
        
        throw new AssertionError("Should not reach here");
    }
    
    // 返回一个本地环回IP+prot组成的地址
    private static InetSocketAddress getLoopbackAddress(int port) {
        return new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
    }
    
    static String getRevealedLocalAddressAsString(InetSocketAddress addr) {
        return System.getSecurityManager() == null ? addr.toString() : getLoopbackAddress(addr.getPort()).toString();
    }
    
    
    private static native boolean isIPv6Available0();
    
    private static native boolean canIPv6SocketJoinIPv4Group0();
    
    private static native boolean canJoin6WithIPv4Group0();
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // set to true if exclusive binding is on for Windows
    private static final boolean exclusiveBind; // 指示是否使用独占式绑定
    
    // set to true if the fast tcp loopback should be enabled on Windows
    private static final boolean fastLoopback;
    
    static {
        int availLevel = isExclusiveBindAvailable();
        if(availLevel >= 0) {
            String exclBindProp = GetPropertyAction.privilegedGetProperty("sun.net.useExclusiveBind");
            if(exclBindProp != null) {
                exclusiveBind = exclBindProp.isEmpty() || Boolean.parseBoolean(exclBindProp);
            } else {
                exclusiveBind = availLevel == 1;
            }
        } else {
            exclusiveBind = false;
        }
        
        fastLoopback = isFastTcpLoopbackRequested();
    }
    
    /*
     * Returns 1 for Windows and -1 for Solaris/Linux/Mac OS
     */
    private static native int isExclusiveBindAvailable();
    
    public static boolean isFastTcpLoopbackRequested() {
        String loopbackProp = GetPropertyAction.privilegedGetProperty("jdk.net.useFastTcpLoopback");
        boolean enable;
        if("".equals(loopbackProp)) {
            enable = true;
        } else {
            enable = Boolean.parseBoolean(loopbackProp);
        }
        return enable;
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    private static native void initIDs();
    
}
