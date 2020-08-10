/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.annotation.Native;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import jdk.internal.misc.JavaNetInetAddressAccess;
import jdk.internal.misc.SharedSecrets;
import sun.net.InetAddressCachePolicy;
import sun.net.util.IPAddressUtil;
import sun.security.action.GetPropertyAction;

/**
 * This class represents an Internet Protocol (IP) address.
 *
 * <p> An IP address is either a 32-bit or 128-bit unsigned number
 * used by IP, a lower-level protocol on which protocols like UDP and
 * TCP are built. The IP address architecture is defined by <a
 * href="http://www.ietf.org/rfc/rfc790.txt"><i>RFC&nbsp;790:
 * Assigned Numbers</i></a>, <a
 * href="http://www.ietf.org/rfc/rfc1918.txt"> <i>RFC&nbsp;1918:
 * Address Allocation for Private Internets</i></a>, <a
 * href="http://www.ietf.org/rfc/rfc2365.txt"><i>RFC&nbsp;2365:
 * Administratively Scoped IP Multicast</i></a>, and <a
 * href="http://www.ietf.org/rfc/rfc2373.txt"><i>RFC&nbsp;2373: IP
 * Version 6 Addressing Architecture</i></a>. An instance of an
 * InetAddress consists of an IP address and possibly its
 * corresponding host name (depending on whether it is constructed
 * with a host name or whether it has already done reverse host name
 * resolution).
 *
 * <h3> Address types </h3>
 *
 * <table class="striped" style="margin-left:2em">
 * <caption style="display:none">Description of unicast and multicast address types</caption>
 * <thead>
 * <tr><th scope="col">Address Type</th><th scope="col">Description</th></tr>
 * </thead>
 * <tbody>
 * <tr><th scope="row" style="vertical-align:top">unicast</th>
 * <td>An identifier for a single interface. A packet sent to
 * a unicast address is delivered to the interface identified by
 * that address.
 *
 * <p> The Unspecified Address -- Also called anylocal or wildcard
 * address. It must never be assigned to any node. It indicates the
 * absence of an address. One example of its use is as the target of
 * bind, which allows a server to accept a client connection on any
 * interface, in case the server host has multiple interfaces.
 *
 * <p> The <i>unspecified</i> address must not be used as
 * the destination address of an IP packet.
 *
 * <p> The <i>Loopback</i> Addresses -- This is the address
 * assigned to the loopback interface. Anything sent to this
 * IP address loops around and becomes IP input on the local
 * host. This address is often used when testing a
 * client.</td></tr>
 * <tr><th scope="row" style="vertical-align:top">multicast</th>
 * <td>An identifier for a set of interfaces (typically belonging
 * to different nodes). A packet sent to a multicast address is
 * delivered to all interfaces identified by that address.</td></tr>
 * </tbody>
 * </table>
 *
 * <h4> IP address scope </h4>
 *
 * <p> <i>Link-local</i> addresses are designed to be used for addressing
 * on a single link for purposes such as auto-address configuration,
 * neighbor discovery, or when no routers are present.
 *
 * <p> <i>Site-local</i> addresses are designed to be used for addressing
 * inside of a site without the need for a global prefix.
 *
 * <p> <i>Global</i> addresses are unique across the internet.
 *
 * <h4> Textual representation of IP addresses </h4>
 *
 * The textual representation of an IP address is address family specific.
 *
 * <p>
 *
 * For IPv4 address format, please refer to <A
 * HREF="Inet4Address.html#format">Inet4Address#format</A>; For IPv6
 * address format, please refer to <A
 * HREF="Inet6Address.html#format">Inet6Address#format</A>.
 *
 * <P>There is a <a href="doc-files/net-properties.html#Ipv4IPv6">couple of
 * System Properties</a> affecting how IPv4 and IPv6 addresses are used.</P>
 *
 * <h4> Host Name Resolution </h4>
 *
 * Host name-to-IP address <i>resolution</i> is accomplished through
 * the use of a combination of local machine configuration information
 * and network naming services such as the Domain Name System (DNS)
 * and Network Information Service(NIS). The particular naming
 * services(s) being used is by default the local machine configured
 * one. For any host name, its corresponding IP address is returned.
 *
 * <p> <i>Reverse name resolution</i> means that for any IP address,
 * the host associated with the IP address is returned.
 *
 * <p> The InetAddress class provides methods to resolve host names to
 * their IP addresses and vice versa.
 *
 * <h4> InetAddress Caching </h4>
 *
 * The InetAddress class has a cache to store successful as well as
 * unsuccessful host name resolutions.
 *
 * <p> By default, when a security manager is installed, in order to
 * protect against DNS spoofing attacks,
 * the result of positive host name resolutions are
 * cached forever. When a security manager is not installed, the default
 * behavior is to cache entries for a finite (implementation dependent)
 * period of time. The result of unsuccessful host
 * name resolution is cached for a very short period of time (10
 * seconds) to improve performance.
 *
 * <p> If the default behavior is not desired, then a Java security property
 * can be set to a different Time-to-live (TTL) value for positive
 * caching. Likewise, a system admin can configure a different
 * negative caching TTL value when needed.
 *
 * <p> Two Java security properties control the TTL values used for
 * positive and negative host name resolution caching:
 *
 * <dl style="margin-left:2em">
 * <dt><b>networkaddress.cache.ttl</b></dt>
 * <dd>Indicates the caching policy for successful name lookups from
 * the name service. The value is specified as an integer to indicate
 * the number of seconds to cache the successful lookup. The default
 * setting is to cache for an implementation specific period of time.
 * <p>
 * A value of -1 indicates "cache forever".
 * </dd>
 * <dt><b>networkaddress.cache.negative.ttl</b> (default: 10)</dt>
 * <dd>Indicates the caching policy for un-successful name lookups
 * from the name service. The value is specified as an integer to
 * indicate the number of seconds to cache the failure for
 * un-successful lookups.
 * <p>
 * A value of 0 indicates "never cache".
 * A value of -1 indicates "cache forever".
 * </dd>
 * </dl>
 *
 * @author Chris Warth
 * @see java.net.InetAddress#getByAddress(byte[])
 * @see java.net.InetAddress#getByAddress(java.lang.String, byte[])
 * @see java.net.InetAddress#getAllByName(java.lang.String)
 * @see java.net.InetAddress#getByName(java.lang.String)
 * @see java.net.InetAddress#getLocalHost()
 * @since 1.0
 */
/*
 * InetAddress代表一个IP地址，包括IP4和IP6。
 *
 * IP地址由一串无符号字节组成，IP4是用点号分割的4个无符号字节（共计32位），IP6是用冒号分割的8个区块，每个区块是4个十六进制数字（共计128位）。
 */
public class InetAddress implements Serializable {
    
    @Native
    static final int PREFER_IPV4_VALUE = 0;
    @Native
    static final int PREFER_IPV6_VALUE = 1;
    @Native
    static final int PREFER_SYSTEM_VALUE = 2;
    
    /**
     * Specify the address family: Internet Protocol, Version 4
     *
     * @since 1.4
     */
    @Native
    static final int IPv4 = 1;
    
    /**
     * Specify the address family: Internet Protocol, Version 6
     *
     * @since 1.4
     */
    @Native
    static final int IPv6 = 2;
    
    /** Specify address family preference */
    // 标记使用IP4地址还是IP6地址
    static transient final int preferIPv6Address;
    
    // 用来映射主机地址/名称，以及判断网络地址的可用性
    static final InetAddressImpl impl;
    
    /** Used to store the serializable fields of InetAddress */
    // 存储IP4地址的可序列化字段
    final transient InetAddressHolder holder;
    
    /** Used to store the name service provider */
    // IP-域名映射服务
    private static transient NameService nameService = null;
    
    /** mapping from host name to Addresses - either NameServiceAddresses (while still being looked-up by NameService(s)) or CachedAddresses when cached */
    // 如果不使用缓存，则存储NameServiceAddresses，如果使用缓存，则替换为CachedAddresses
    private static final ConcurrentMap<String, Addresses> cache = new ConcurrentHashMap<>();
    
    /** CachedAddresses that have to expire are kept ordered in this NavigableSet which is scanned on each access */
    // 缓存带有有效期的查询出来的网络地址（为了快速筛选出过期的缓存）
    private static final NavigableSet<CachedAddresses> expirySet = new ConcurrentSkipListSet<>();
    
    // 缓存本地主机名称和地址
    private static volatile CachedLocalHost cachedLocalHost;
    
    /**
     * Used to store the best available hostname.
     * Lazily initialized via a data race; safe because Strings are immutable.
     */
    // 当前网络地址的主机名
    private transient String canonicalHostName = null;
    
    
    /* Load net library into runtime, and perform initializations */
    static {
        String str = AccessController.doPrivileged(new GetPropertyAction("java.net.preferIPv6Addresses"));
        if(str == null) {
            // 默认使用IP4地址
            preferIPv6Address = PREFER_IPV4_VALUE;
        } else if(str.equalsIgnoreCase("true")) {
            preferIPv6Address = PREFER_IPV6_VALUE;
        } else if(str.equalsIgnoreCase("false")) {
            preferIPv6Address = PREFER_IPV4_VALUE;
        } else if(str.equalsIgnoreCase("system")) {
            preferIPv6Address = PREFER_SYSTEM_VALUE;
        } else {
            preferIPv6Address = PREFER_IPV4_VALUE;
        }
        
        AccessController.doPrivileged(new java.security.PrivilegedAction<>() {
            public Void run() {
                System.loadLibrary("net");
                return null;
            }
        });
        
        SharedSecrets.setJavaNetInetAddressAccess(new JavaNetInetAddressAccess() {
            public String getOriginalHostName(InetAddress ia) {
                return ia.holder.getOriginalHostName();
            }
            
            public InetAddress getByName(String hostName, InetAddress hostAddress) throws UnknownHostException {
                return InetAddress.getByName(hostName, hostAddress);
            }
        });
        
        init();
    }
    
    static {
        // 创建InetAddressImpl的实现类（分为IP4和IP6两种）
        impl = InetAddressImplFactory.create();
        
        // 构建一个IP-域名映射服务
        nameService = createNameService();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructor for the Socket.accept() method.
     * This creates an empty InetAddress, which is filled in by
     * the accept() method.  This InetAddress, however, is not
     * put in the address cache, since it is not created by name.
     */
    InetAddress() {
        holder = new InetAddressHolder();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an {@code InetAddress} object given the raw IP address .
     * The argument is in network byte order: the highest order
     * byte of the address is in {@code getAddress()[0]}.
     *
     * <p> This method doesn't block, i.e. no reverse name service lookup
     * is performed.
     *
     * <p> IPv4 address byte array must be 4 bytes long and IPv6 byte array
     * must be 16 bytes long
     *
     * @param addr the raw IP address in network byte order
     *
     * @return an InetAddress object created from the raw IP address.
     *
     * @throws UnknownHostException if IP address is of illegal length
     * @since 1.4
     */
    // 用给定主机地址创建一个InetAddress实例
    public static InetAddress getByAddress(byte[] addr) throws UnknownHostException {
        return getByAddress(null, addr);
    }
    
    /**
     * Creates an InetAddress based on the provided host name and IP address.
     * No name service is checked for the validity of the address.
     *
     * <p> The host name can either be a machine name, such as
     * "{@code java.sun.com}", or a textual representation of its IP
     * address.
     * <p> No validity checking is done on the host name either.
     *
     * <p> If addr specifies an IPv4 address an instance of Inet4Address
     * will be returned; otherwise, an instance of Inet6Address
     * will be returned.
     *
     * <p> IPv4 address byte array must be 4 bytes long and IPv6 byte array
     * must be 16 bytes long
     *
     * @param host the specified host
     * @param addr the raw IP address in network byte order
     *
     * @return an InetAddress object created from the raw IP address.
     *
     * @throws UnknownHostException if IP address is of illegal length
     * @since 1.4
     */
    // 用给定主机名称与主机地址创建一个InetAddress实例
    public static InetAddress getByAddress(String host, byte[] addr) throws UnknownHostException {
        // 去掉host外层的[]
        if(host != null && host.length()>0 && host.charAt(0) == '[') {
            if(host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
            }
        }
        
        if(addr != null) {
            if(addr.length == Inet4Address.INADDRSZ) {
                return new Inet4Address(host, addr);
            } else if(addr.length == Inet6Address.INADDRSZ) {
                // 将IP6地址转换为IP4地址，转换失败则返回null
                byte[] newAddr = IPAddressUtil.convertFromIPv4MappedAddress(addr);
                if(newAddr != null) {
                    // 本质是IP4地址
                    return new Inet4Address(host, newAddr);
                } else {
                    // 确实是IP6地址
                    return new Inet6Address(host, addr);
                }
            }
        }
        
        throw new UnknownHostException("addr is of illegal length");
    }
    
    /**
     * Returns the loopback address.
     * <p>
     * The InetAddress returned will represent the IPv4
     * loopback address, 127.0.0.1, or the IPv6 loopback
     * address, ::1. The IPv4 loopback address returned
     * is only one of many in the form 127.*.*.*
     *
     * @return the InetAddress loopback instance.
     *
     * @since 1.7
     */
    // 返回本地回环地址
    public static InetAddress getLoopbackAddress() {
        return impl.loopbackAddress();
    }
    
    /**
     * Returns the address of the local host. This is achieved by retrieving
     * the name of the host from the system, then resolving that name into
     * an {@code InetAddress}.
     *
     * <P>Note: The resolved address may be cached for a short period of time.
     * </P>
     *
     * <p>If there is a security manager, its
     * {@code checkConnect} method is called
     * with the local host name and {@code -1}
     * as its arguments to see if the operation is allowed.
     * If the operation is not allowed, an InetAddress representing
     * the loopback address is returned.
     *
     * @return the address of the local host.
     *
     * @throws UnknownHostException if the local host name could not
     *                              be resolved into an address.
     * @see SecurityManager#checkConnect
     * @see java.net.InetAddress#getByName(java.lang.String)
     */
    // 返回本地主机地址
    public static InetAddress getLocalHost() throws UnknownHostException {
        
        SecurityManager security = System.getSecurityManager();
        
        try {
            // is cached data still valid?
            CachedLocalHost clh = cachedLocalHost;
            
            // 缓存未过时的话，直接从缓存中获取
            if(clh != null && (clh.expiryTime - System.nanoTime()) >= 0L) {
                if(security != null) {
                    security.checkConnect(clh.host, -1);
                }
                return clh.addr;
            }
            
            // 获取本地主机名称
            String local = impl.getLocalHostName();
            
            if(security != null) {
                security.checkConnect(local, -1);
            }
            
            InetAddress localAddr;
            
            // shortcut for "localhost" host name
            if(local.equals("localhost")) {
                // 获取本地环回地址
                localAddr = impl.loopbackAddress();
            } else {
                // call getAllByName0 without security checks and without using cached data
                try {
                    localAddr = getAllByName0(local, null, false, false)[0];
                } catch(UnknownHostException uhe) {
                    // Rethrow with a more informative error message.
                    UnknownHostException uhe2 = new UnknownHostException(local + ": " + uhe.getMessage());
                    uhe2.initCause(uhe);
                    throw uhe2;
                }
            }
            
            // 将本地主机名称和地址加入到缓存中
            cachedLocalHost = new CachedLocalHost(local, localAddr);
            
            return localAddr;
        } catch(SecurityException e) {
            return impl.loopbackAddress();
        }
    }
    
    /**
     * Determines the IP address of a host, given the host's name.
     *
     * <p> The host name can either be a machine name, such as
     * "{@code java.sun.com}", or a textual representation of its
     * IP address. If a literal IP address is supplied, only the
     * validity of the address format is checked.
     *
     * <p> For {@code host} specified in literal IPv6 address,
     * either the form defined in RFC 2732 or the literal IPv6 address
     * format defined in RFC 2373 is accepted. IPv6 scoped addresses are also
     * supported. See <a href="Inet6Address.html#scoped">here</a> for a description of IPv6
     * scoped addresses.
     *
     * <p> If the host is {@code null} or {@code host.length()} is equal
     * to zero, then an {@code InetAddress} representing an address of the
     * loopback interface is returned.
     * See <a href="http://www.ietf.org/rfc/rfc3330.txt">RFC&nbsp;3330</a>
     * section&nbsp;2 and <a href="http://www.ietf.org/rfc/rfc2373.txt">RFC&nbsp;2373</a>
     * section&nbsp;2.5.3.
     *
     * <p> If there is a security manager, and {@code host} is not {@code null}
     * or {@code host.length() } is not equal to zero, the security manager's
     * {@code checkConnect} method is called with the hostname and {@code -1}
     * as its arguments to determine if the operation is allowed.
     *
     * @param host the specified host, or {@code null}.
     *
     * @return an IP address for the given host name.
     *
     * @throws UnknownHostException if no IP address for the
     *                              {@code host} could be found, or if a scope_id was specified
     *                              for a global IPv6 address.
     * @throws SecurityException    if a security manager exists
     *                              and its checkConnect method doesn't allow the operation
     */
    // ▶ 1-1-1-1 根据主机名称或主机IP，创建/查找其对应的InetAddress实例（有多个实例时只选择第一个）
    public static InetAddress getByName(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host)[0];
    }
    
    /**
     * Given the name of a host, returns an array of its IP addresses,
     * based on the configured name service on the system.
     *
     * <p> The host name can either be a machine name, such as
     * "{@code java.sun.com}", or a textual representation of its IP
     * address. If a literal IP address is supplied, only the
     * validity of the address format is checked.
     *
     * <p> For {@code host} specified in <i>literal IPv6 address</i>,
     * either the form defined in RFC 2732 or the literal IPv6 address
     * format defined in RFC 2373 is accepted. A literal IPv6 address may
     * also be qualified by appending a scoped zone identifier or scope_id.
     * The syntax and usage of scope_ids is described
     * <a href="Inet6Address.html#scoped">here</a>.
     *
     * <p> If the host is {@code null} or {@code host.length()} is equal
     * to zero, then an {@code InetAddress} representing an address of the
     * loopback interface is returned.
     * See <a href="http://www.ietf.org/rfc/rfc3330.txt">RFC&nbsp;3330</a>
     * section&nbsp;2 and <a href="http://www.ietf.org/rfc/rfc2373.txt">RFC&nbsp;2373</a>
     * section&nbsp;2.5.3. </p>
     *
     * <p> If there is a security manager, and {@code host} is not {@code null}
     * or {@code host.length() } is not equal to zero, the security manager's
     * {@code checkConnect} method is called with the hostname and {@code -1}
     * as its arguments to determine if the operation is allowed.
     *
     * @param host the name of the host, or {@code null}.
     *
     * @return an array of all the IP addresses for a given host name.
     *
     * @throws UnknownHostException if no IP address for the
     *                              {@code host} could be found, or if a scope_id was specified
     *                              for a global IPv6 address.
     * @throws SecurityException    if a security manager exists and its
     *                              {@code checkConnect} method doesn't allow the operation.
     * @see SecurityManager#checkConnect
     */
    // ▶ 1-1-1 根据主机名称或主机IP，创建/查找其对应的InetAddress实例
    public static InetAddress[] getAllByName(String host) throws UnknownHostException {
        return getAllByName(host, null);
    }
    
    /** called from deployment cache manager */
    // ▶ 1-1-2 根据主机名称或主机IP，创建/查找其对应的InetAddress实例（有多个实例时只选择第一个）
    private static InetAddress getByName(String host, InetAddress reqAddr) throws UnknownHostException {
        return InetAddress.getAllByName(host, reqAddr)[0];
    }
    
    // ▶ 1-2-1 根据主机名称，创建/查找其对应的InetAddress实例
    private static InetAddress[] getAllByName0(String host) throws UnknownHostException {
        return getAllByName0(host, true);
    }
    
    // ▶ 1-1 根据主机名称或主机IP，创建/查找其对应的InetAddress实例
    private static InetAddress[] getAllByName(String host, InetAddress reqAddr) throws UnknownHostException {
        // host为空，则使用本地环回地址
        if(host == null || host.length() == 0) {
            InetAddress[] ret = new InetAddress[1];
            ret[0] = impl.loopbackAddress();
            return ret;
        }
        
        boolean ipv6Expected = false;
        
        // 去掉host外层的[]
        if(host.charAt(0) == '[') {
            // This is supposed to be an IPv6 literal
            if(host.length()>2 && host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
                ipv6Expected = true;
            } else {
                // This was supposed to be a IPv6 address, but it's not!
                throw new UnknownHostException(host + ": invalid IPv6 address");
            }
        }
        
        // 如果host是IP地址，则不需要进行查找
        if(Character.digit(host.charAt(0), 16) != -1 || (host.charAt(0) == ':')) {
            byte[] addr = null;
            int numericZone = -1;
            String ifname = null;
            
            // 将文本形式的IP4地址转换为二进制形式
            addr = IPAddressUtil.textToNumericFormatV4(host);
            if(addr == null) {
                // This is supposed to be an IPv6 literal
                // Check if a numeric or string zone id is present
                int pos;
                if((pos = host.indexOf('%')) != -1) {
                    numericZone = checkNumericZone(host);
                    if(numericZone == -1) { /* remainder of string must be an ifname */
                        ifname = host.substring(pos + 1);
                    }
                }
                
                // 将文本形式的IP6地址转换为二进制形式
                if((addr = IPAddressUtil.textToNumericFormatV6(host)) == null && host.contains(":")) {
                    throw new UnknownHostException(host + ": invalid IPv6 address");
                }
            } else if(ipv6Expected) {
                // Means an IPv4 litteral between brackets!
                throw new UnknownHostException("[" + host + "]");
            }
            
            InetAddress[] ret = new InetAddress[1];
            
            if(addr != null) {
                if(addr.length == Inet4Address.INADDRSZ) {
                    ret[0] = new Inet4Address(null, addr);
                } else {
                    if(ifname != null) {
                        ret[0] = new Inet6Address(null, addr, ifname);
                    } else {
                        ret[0] = new Inet6Address(null, addr, numericZone);
                    }
                }
                return ret;
            }
        } else if(ipv6Expected) {
            // We were expecting an IPv6 Litteral, but got something else
            throw new UnknownHostException("[" + host + "]");
        }
        
        return getAllByName0(host, reqAddr, true, true);
    }
    
    /**
     * package private so SocketPermission can call it
     */
    // ▶ 1-2 根据主机名称，创建/查找其对应的InetAddress实例
    static InetAddress[] getAllByName0(String host, boolean check) throws UnknownHostException {
        return getAllByName0(host, null, check, true);
    }
    
    /**
     * Designated lookup method.
     *
     * @param host     host name to look up
     * @param reqAddr  requested address to be the 1st in returned array
     * @param check    perform security check
     * @param useCache use cached value if not expired else always perform name service lookup (and cache the result)
     *
     * @return array of InetAddress(es)
     *
     * @throws UnknownHostException if host name is not found
     */
    /*
     * ▶ 1
     *
     * 根据主机名称，创建/查找其对应的InetAddress实例
     *
     * 如果给定的是主机名称，则需要查询其对应的IP地址，一个主机名称可能对应多个主机地址，
     * 此时，返回结果将是一个InetAddress列表，
     *
     * reqAddr代表预期的InetAddress，如果最终的InetAddress列表中包含reqAddr，则将其调整到列表的首位
     */
    private static InetAddress[] getAllByName0(String host, InetAddress reqAddr, boolean check, boolean useCache) throws UnknownHostException {
        
        /* If it gets here it is presumed to be a hostname */
        
        // make sure the connection to the host is allowed, before we give out a hostname
        if(check) {
            SecurityManager security = System.getSecurityManager();
            if(security != null) {
                security.checkConnect(host, -1);
            }
        }
        
        // remove expired addresses from cache - expirySet keeps them ordered by expiry time so we only need to iterate the prefix of the NavigableSet...
        long now = System.nanoTime();
        
        // 从expirySet中查找匹配的缓存
        for(CachedAddresses caddrs : expirySet) {
            /*
             * compare difference of time instants rather than time instants directly, to avoid possible overflow.
             * (see System.nanoTime() recommendations...)
             */
            // 顺路清除过期缓存
            if((caddrs.expiryTime - now)<0L) {
                // ConcurrentSkipListSet uses weakly consistent iterator, so removing while iterating is OK...
                if(expirySet.remove(caddrs)) {
                    // ... remove from cache
                    cache.remove(caddrs.host, caddrs);
                }
            } else {
                // we encountered 1st element that expires in future
                break;
            }
        }
        
        // look-up or remove from cache
        Addresses addrs;
        
        // 如果需要使用缓存
        if(useCache) {
            // 从缓存池中获取元素
            addrs = cache.get(host);
        } else {
            // 从缓存池中移除元素
            addrs = cache.remove(host);
            
            // 如果成功移除了相应的元素
            if(addrs != null) {
                if(addrs instanceof CachedAddresses) {
                    // try removing from expirySet too if CachedAddresses
                    expirySet.remove(addrs);
                }
                addrs = null;
            }
        }
        
        if(addrs == null) {
            /* create a NameServiceAddresses instance which will look up the name service and install it within cache... */
            
            // 创建一个缓存元素
            addrs = new NameServiceAddresses(host, reqAddr);
            
            // 将缓存元素加入到缓存池中
            Addresses oldAddrs = cache.putIfAbsent(host, addrs);
            
            // 缓存中已经存在该host（可能被别的线程抢先缓存了）
            if(oldAddrs != null) {
                // 更新addrs为当前缓存中的Addresses
                addrs = oldAddrs;
            }
        }
        
        // ask Addresses to get an array of InetAddress(es) and clone it
        return addrs.get().clone();
    }
    
    
    // 通过IP-域名映射服务，查询出host对应的网络地址列表。如果reqAddr也在其中，将其设置到列表首位。
    static InetAddress[] getAddressesFromNameService(String host, InetAddress reqAddr) throws UnknownHostException {
        InetAddress[] addresses = null;
        UnknownHostException ex = null;
        
        try {
            // 将主机名称或主机地址映射为InetAddress实例
            addresses = nameService.lookupAllHostAddr(host);
        } catch(UnknownHostException uhe) {
            if(host.equalsIgnoreCase("localhost")) {
                addresses = new InetAddress[]{impl.loopbackAddress()};
            } else {
                ex = uhe;
            }
        }
        
        if(addresses == null) {
            throw ex == null ? new UnknownHostException(host) : ex;
        }
        
        // 如果需要的话，将reqAddr调整到addresses首位
        if(reqAddr != null && addresses.length>1 && !addresses[0].equals(reqAddr)) {
            // Find it?
            int i = 1;
            
            for(; i<addresses.length; i++) {
                if(addresses[i].equals(reqAddr)) {
                    break;
                }
            }
            
            // 将reqAddr插入到addresses首位
            if(i<addresses.length) {
                InetAddress tmp, tmp2 = reqAddr;
                for(int j = 0; j<i; j++) {
                    tmp = addresses[j];
                    addresses[j] = tmp2;
                    tmp2 = tmp;
                }
                addresses[i] = tmp2;
            }
        }
        
        return addresses;
    }
    
    /**
     * Returns the InetAddress representing anyLocalAddress (typically 0.0.0.0 or ::0)
     */
    // 获取通配地址
    static InetAddress anyLocalAddress() {
        return impl.anyLocalAddress();
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Test whether that address is reachable. Best effort is made by the
     * implementation to try to reach the host, but firewalls and server
     * configuration may block requests resulting in a unreachable status
     * while some specific ports may be accessible.
     * A typical implementation will use ICMP ECHO REQUESTs if the
     * privilege can be obtained, otherwise it will try to establish
     * a TCP connection on port 7 (Echo) of the destination host.
     * <p>
     * The timeout value, in milliseconds, indicates the maximum amount of time
     * the try should take. If the operation times out before getting an
     * answer, the host is deemed unreachable. A negative value will result
     * in an IllegalArgumentException being thrown.
     *
     * @param timeout the time, in milliseconds, before the call aborts
     *
     * @return a {@code boolean} indicating if the address is reachable.
     *
     * @throws IOException              if a network error occurs
     * @throws IllegalArgumentException if {@code timeout} is negative.
     * @since 1.5
     */
    // 判断给定的网络地址是否可用
    public boolean isReachable(int timeout) throws IOException {
        return isReachable(null, 0, timeout);
    }
    
    /**
     * Test whether that address is reachable. Best effort is made by the
     * implementation to try to reach the host, but firewalls and server
     * configuration may block requests resulting in a unreachable status
     * while some specific ports may be accessible.
     * A typical implementation will use ICMP ECHO REQUESTs if the
     * privilege can be obtained, otherwise it will try to establish
     * a TCP connection on port 7 (Echo) of the destination host.
     * <p>
     * The {@code network interface} and {@code ttl} parameters
     * let the caller specify which network interface the test will go through
     * and the maximum number of hops the packets should go through.
     * A negative value for the {@code ttl} will result in an
     * IllegalArgumentException being thrown.
     * <p>
     * The timeout value, in milliseconds, indicates the maximum amount of time
     * the try should take. If the operation times out before getting an
     * answer, the host is deemed unreachable. A negative value will result
     * in an IllegalArgumentException being thrown.
     *
     * @param netif   the NetworkInterface through which the test will be done, or null for any interface
     * @param ttl     the maximum numbers of hops to try or 0 for the default
     * @param timeout the time, in milliseconds, before the call aborts
     *
     * @return a {@code boolean}indicating if the address is reachable.
     *
     * @throws IllegalArgumentException if either {@code timeout}
     *                                  or {@code ttl} are negative.
     * @throws IOException              if a network error occurs
     * @since 1.5
     */
    // 通过指定的网络接口判断给定的网络地址是否可用，ttl代表网络跳数
    public boolean isReachable(NetworkInterface netif, int ttl, int timeout) throws IOException {
        if(ttl<0) {
            throw new IllegalArgumentException("ttl can't be negative");
        }
        
        if(timeout<0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        
        return impl.isReachable(this, timeout, netif, ttl);
    }
    
    
    /**
     * Gets the host name for this IP address.
     *
     * <p>If this InetAddress was created with a host name,
     * this host name will be remembered and returned;
     * otherwise, a reverse name lookup will be performed
     * and the result will be returned based on the system
     * configured name lookup service. If a lookup of the name service
     * is required, call
     * {@link #getCanonicalHostName() getCanonicalHostName}.
     *
     * <p>If there is a security manager, its
     * {@code checkConnect} method is first called
     * with the hostname and {@code -1}
     * as its arguments to see if the operation is allowed.
     * If the operation is not allowed, it will return
     * the textual representation of the IP address.
     *
     * @return the host name for this IP address, or if the operation
     * is not allowed by the security check, the textual
     * representation of the IP address.
     *
     * @see InetAddress#getCanonicalHostName
     * @see SecurityManager#checkConnect
     */
    // 获取当前网络地址的主机名
    public String getHostName() {
        return getHostName(true);
    }
    
    /**
     * Gets the fully qualified domain name for this IP address.
     * Best effort method, meaning we may not be able to return
     * the FQDN depending on the underlying system configuration.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkConnect} method
     * with the hostname and {@code -1}
     * as its arguments to see if the calling code is allowed to know
     * the hostname for this IP address, i.e., to connect to the host.
     * If the operation is not allowed, it will return
     * the textual representation of the IP address.
     *
     * @return the fully qualified domain name for this IP address,
     * or if the operation is not allowed by the security check,
     * the textual representation of the IP address.
     *
     * @see SecurityManager#checkConnect
     * @since 1.4
     */
    // 获取当前网络地址的主机名
    public String getCanonicalHostName() {
        String value = canonicalHostName;
        if(value == null) {
            canonicalHostName = value = InetAddress.getHostFromNameService(this, true);
        }
        return value;
    }
    
    /**
     * Returns the raw IP address of this {@code InetAddress}
     * object. The result is in network byte order: the highest order
     * byte of the address is in {@code getAddress()[0]}.
     *
     * @return the raw IP address of this object.
     */
    // 返回IP地址的字节形式
    public byte[] getAddress() {
        return null;
    }
    
    /**
     * Returns the IP address string in textual presentation.
     *
     * @return the raw IP address in a string format.
     *
     * @since 1.0.2
     */
    // 返回IP地址的文本形式
    public String getHostAddress() {
        return null;
    }
    
    /**
     * Returns the hostname for this address.
     * If the host is equal to null, then this address refers to any
     * of the local machine's available network addresses.
     * this is package private so SocketPermission can make calls into
     * here without a security check.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkConnect} method
     * with the hostname and {@code -1}
     * as its arguments to see if the calling code is allowed to know
     * the hostname for this IP address, i.e., to connect to the host.
     * If the operation is not allowed, it will return
     * the textual representation of the IP address.
     *
     * @param check make security check if true
     *
     * @return the host name for this IP address, or if the operation
     * is not allowed by the security check, the textual
     * representation of the IP address.
     *
     * @see SecurityManager#checkConnect
     */
    // 获取当前网络地址的主机名
    String getHostName(boolean check) {
        if(holder().getHostName() == null) {
            holder().hostName = InetAddress.getHostFromNameService(this, check);
        }
    
        return holder().getHostName();
    }
    
    /**
     * Returns the hostname for this address.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkConnect} method
     * with the hostname and {@code -1}
     * as its arguments to see if the calling code is allowed to know
     * the hostname for this IP address, i.e., to connect to the host.
     * If the operation is not allowed, it will return
     * the textual representation of the IP address.
     *
     * @param check make security check if true
     *
     * @return the host name for this IP address, or if the operation
     * is not allowed by the security check, the textual
     * representation of the IP address.
     *
     * @see SecurityManager#checkConnect
     */
    // 返回指定网络地址的主机名
    private static String getHostFromNameService(InetAddress addr, boolean check) {
        String host = null;
        try {
            // first lookup the hostname
            host = nameService.getHostByAddr(addr.getAddress());
        
            /* check to see if calling code is allowed to know
             * the hostname for this IP address, ie, connect to the host
             */
            if(check) {
                SecurityManager sec = System.getSecurityManager();
                if(sec != null) {
                    sec.checkConnect(host, -1);
                }
            }
        
            /* now get all the IP addresses for this hostname,
             * and make sure one of them matches the original IP
             * address. We do this to try and prevent spoofing.
             */
        
            InetAddress[] arr = InetAddress.getAllByName0(host, check);
            boolean ok = false;
        
            if(arr != null) {
                for(int i = 0; !ok && i<arr.length; i++) {
                    ok = addr.equals(arr[i]);
                }
            }
        
            //XXX: if it looks a spoof just return the address?
            if(!ok) {
                host = addr.getHostAddress();
                return host;
            }
        } catch(SecurityException e) {
            host = addr.getHostAddress();
        } catch(UnknownHostException e) {
            host = addr.getHostAddress();
            // let next provider resolve the hostname
        }
        return host;
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 特殊地址 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Utility routine to check if the InetAddress is a loopback address.
     *
     * @return a {@code boolean} indicating if the InetAddress is
     * a loopback address; or false otherwise.
     *
     * @since 1.4
     */
    // 判断当前地址是否为本地环回地址
    public boolean isLoopbackAddress() {
        return false;
    }
    
    /**
     * Utility routine to check if the InetAddress is a wildcard address.
     *
     * @return a {@code boolean} indicating if the Inetaddress is
     * a wildcard address.
     *
     * @since 1.4
     */
    // 判断当前地址是否为通配地址
    public boolean isAnyLocalAddress() {
        return false;
    }
    
    /**
     * Utility routine to check if the InetAddress is an link local address.
     *
     * @return a {@code boolean} indicating if the InetAddress is
     * a link local address; or false if address is not a link local unicast address.
     *
     * @since 1.4
     */
    // 判断当前地址是否为链路本地地址
    public boolean isLinkLocalAddress() {
        return false;
    }
    
    /**
     * Utility routine to check if the InetAddress is a site local address.
     *
     * @return a {@code boolean} indicating if the InetAddress is
     * a site local address; or false if address is not a site local unicast address.
     *
     * @since 1.4
     */
    // 判断当前地址是否为站点本地地址
    public boolean isSiteLocalAddress() {
        return false;
    }
    
    /**
     * Utility routine to check if the InetAddress is an
     * IP multicast address.
     *
     * @return a {@code boolean} indicating if the InetAddress is
     * an IP multicast address
     *
     * @since 1.1
     */
    // 判断当前地址是否为组播地址
    public boolean isMulticastAddress() {
        return false;
    }
    
    /**
     * Utility routine to check if the multicast address has node scope.
     *
     * @return a {@code boolean} indicating if the address has
     * is a multicast address of node-local scope, false if it is not
     * of node-local scope or it is not a multicast address
     *
     * @since 1.4
     */
    // 判断当前地址是否为节点（或接口）本地范围的组播地址
    public boolean isMCNodeLocal() {
        return false;
    }
    
    /**
     * Utility routine to check if the multicast address has link scope.
     *
     * @return a {@code boolean} indicating if the address has
     * is a multicast address of link-local scope, false if it is not
     * of link-local scope or it is not a multicast address
     *
     * @since 1.4
     */
    // 判断当前地址是否为链路本地范围的组播地址
    public boolean isMCLinkLocal() {
        return false;
    }
    
    /**
     * Utility routine to check if the multicast address has site scope.
     *
     * @return a {@code boolean} indicating if the address has
     * is a multicast address of site-local scope, false if it is not
     * of site-local scope or it is not a multicast address
     *
     * @since 1.4
     */
    // 判断当前地址是否为站点本地范围的组播地址
    public boolean isMCSiteLocal() {
        return false;
    }
    
    /**
     * Utility routine to check if the multicast address has organization scope.
     *
     * @return a {@code boolean} indicating if the address has
     * is a multicast address of organization-local scope,
     * false if it is not of organization-local scope
     * or it is not a multicast address
     *
     * @since 1.4
     */
    // 判断当前地址是否为机构本地范围的组播地址
    public boolean isMCOrgLocal() {
        return false;
    }
    
    /**
     * Utility routine to check if the multicast address has global scope.
     *
     * @return a {@code boolean} indicating if the address has
     * is a multicast address of global scope, false if it is not
     * of global scope or it is not a multicast address
     *
     * @since 1.4
     */
    // 判断当前地址是否为全局范围的组播地址
    public boolean isMCGlobal() {
        return false;
    }
    
    /*▲ 特殊地址 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = 3286316764910316507L;
    
    private static final jdk.internal.misc.Unsafe UNSAFE = jdk.internal.misc.Unsafe.getUnsafe();
    
    private static final long FIELDS_OFFSET = UNSAFE.objectFieldOffset(InetAddress.class, "holder");
    
    /* needed because the serializable fields no longer exist */
    
    /**
     * @serialField hostName String
     * @serialField address int
     * @serialField family int
     */
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("hostName", String.class), new ObjectStreamField("address", int.class), new ObjectStreamField("family", int.class),};
    
    private void writeObject(ObjectOutputStream s) throws IOException {
        if(getClass().getClassLoader() != null) {
            throw new SecurityException("invalid address type");
        }
        PutField pf = s.putFields();
        pf.put("hostName", holder().getHostName());
        pf.put("address", holder().getAddress());
        pf.put("family", holder().getFamily());
        s.writeFields();
    }
    
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        if(getClass().getClassLoader() != null) {
            throw new SecurityException("invalid address type");
        }
        GetField gf = s.readFields();
        String host = (String) gf.get("hostName", null);
        int address = gf.get("address", 0);
        int family = gf.get("family", 0);
        InetAddressHolder h = new InetAddressHolder(host, address, family);
        UNSAFE.putObject(this, FIELDS_OFFSET, h);
    }
    
    private void readObjectNoData() throws IOException, ClassNotFoundException {
        if(getClass().getClassLoader() != null) {
            throw new SecurityException("invalid address type");
        }
    }
    
    /**
     * Replaces the de-serialized object with an Inet4Address object.
     *
     * @return the alternate object to the de-serialized object.
     *
     * @throws ObjectStreamException if a new object replacing this
     *                               object could not be created
     */
    private Object readResolve() throws ObjectStreamException {
        // will replace the deserialized 'this' object
        return new Inet4Address(holder().getHostName(), holder().getAddress());
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回序列化字段容器
    InetAddressHolder holder() {
        return holder;
    }
    
    /**
     * Load and instantiate an underlying impl class
     */
    // 加载并实例化InetAddressImpl的实现类（分为IP4实现和IP6实现两种）
    static InetAddressImpl loadImpl(String implName) {
        Object impl = null;
        
        /*
         * Property "impl.prefix" will be prepended to the classname
         * of the implementation object we instantiate, to which we
         * delegate the real work (like native methods).  This
         * property can vary across implementations of the java.
         * classes.  The default is an empty String "".
         */
        String prefix = GetPropertyAction.privilegedGetProperty("impl.prefix", "");
        
        try {
            @SuppressWarnings("deprecation")
            Object tmp = Class.forName("java.net." + prefix + implName).newInstance();
            impl = tmp;
        } catch(ClassNotFoundException e) {
            System.err.println("Class not found: java.net." + prefix + implName + ":\ncheck impl.prefix property " + "in your properties file.");
        } catch(InstantiationException e) {
            System.err.println("Could not instantiate: java.net." + prefix + implName + ":\ncheck impl.prefix property " + "in your properties file.");
        } catch(IllegalAccessException e) {
            System.err.println("Cannot access class: java.net." + prefix + implName + ":\ncheck impl.prefix property " + "in your properties file.");
        }
        
        if(impl == null) {
            try {
                @SuppressWarnings("deprecation")
                Object tmp = Class.forName(implName).newInstance();
                impl = tmp;
            } catch(Exception e) {
                throw new Error("System property impl.prefix incorrect");
            }
        }
        
        return (InetAddressImpl) impl;
    }
    
    /**
     * Create an instance of the NameService interface based on
     * the setting of the {@code jdk.net.hosts.file} system property.
     *
     * <p>The default NameService is the PlatformNameService, which typically
     * delegates name and address resolution calls to the underlying
     * OS network libraries.
     *
     * <p> A HostsFileNameService is created if the {@code jdk.net.hosts.file}
     * system property is set. If the specified file doesn't exist, the name or
     * address lookup will result in an UnknownHostException. Thus, non existent
     * hosts file is handled as if the file is empty.
     *
     * @return a NameService
     */
    /*
     * 构建一个IP-域名映射服务
     *
     * 默认使用PlatformNameService实现，
     * 如果启用了系统属性jdk.net.hosts.file，则使用实现HostsFileNameService
     */
    private static NameService createNameService() {
        NameService theNameService;
        
        String hostsFileName = GetPropertyAction.privilegedGetProperty("jdk.net.hosts.file");
        if(hostsFileName != null) {
            theNameService = new HostsFileNameService(hostsFileName);
        } else {
            theNameService = new PlatformNameService();
        }
        return theNameService;
    }
    
    /**
     * check if the literal address string has %nn appended
     * returns -1 if not, or the numeric value otherwise.
     *
     * %nn may also be a string that represents the displayName of
     * a currently available NetworkInterface.
     */
    private static int checkNumericZone(String s) throws UnknownHostException {
        int percent = s.indexOf('%');
        int slen = s.length();
        int digit, zone = 0;
        if(percent == -1) {
            return -1;
        }
        for(int i = percent + 1; i<slen; i++) {
            char c = s.charAt(i);
            if(c == ']') {
                if(i == percent + 1) {
                    /* empty per-cent field */
                    return -1;
                }
                break;
            }
            if((digit = Character.digit(c, 10))<0) {
                return -1;
            }
            zone = (zone * 10) + digit;
        }
        return zone;
    }
    
    /**
     * Perform class load-time initializations.
     */
    private static native void init();
    
    
    /**
     * Returns a hashcode for this IP address.
     *
     * @return a hash code value for this IP address.
     */
    @Override
    public int hashCode() {
        return -1;
    }
    
    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and it represents the same IP address as
     * this object.
     * <p>
     * Two instances of {@code InetAddress} represent the same IP
     * address if the length of the byte arrays returned by
     * {@code getAddress} is the same for both, and each of the
     * array components is the same for the byte arrays.
     *
     * @param obj the object to compare against.
     *
     * @return {@code true} if the objects are the same;
     * {@code false} otherwise.
     *
     * @see java.net.InetAddress#getAddress()
     */
    @Override
    public boolean equals(Object obj) {
        return false;
    }
    
    /**
     * Converts this IP address to a {@code String}. The
     * string returned is of the form: hostname / literal IP
     * address.
     *
     * If the host name is unresolved, no reverse name service lookup
     * is performed. The hostname part will be represented by an empty string.
     *
     * @return a string representation of this IP address.
     */
    @Override
    public String toString() {
        String hostName = holder().getHostName();
        return Objects.toString(hostName, "") + "/" + getHostAddress();
    }
    
    
    // 序列化字段容器，存储IP地址的可序列化字段
    static class InetAddressHolder {
        /**
         * Reserve the original application specified hostname.
         *
         * The original hostname is useful for domain-based endpoint
         * identification (see RFC 2818 and RFC 6125).  If an address
         * was created with a raw IP address, a reverse name lookup
         * may introduce endpoint identification security issue via
         * DNS forging.
         *
         * Oracle JSSE provider is using this original hostname, via
         * jdk.internal.misc.JavaNetAccess, for SSL/TLS endpoint identification.
         *
         * Note: May define a new public method in the future if necessary.
         */
        String originalHostName;
        
        // 主机名称
        String hostName;
        
        /**
         * Holds a 32-bit IPv4 address.
         */
        // IP4-主机地址
        int address;
        
        /**
         * Specifies the address family type, for instance, '1' for IPv4 addresses, and '2' for IPv6 addresses.
         */
        // IP地址所属协议族：IP4或IP6
        int family;
        
        InetAddressHolder() {
        }
        
        InetAddressHolder(String hostName, int address, int family) {
            this.originalHostName = hostName;
            this.hostName = hostName;
            this.address = address;
            this.family = family;
        }
        
        void init(String hostName, int family) {
            this.originalHostName = hostName;
            this.hostName = hostName;
            if(family != -1) {
                this.family = family;
            }
        }
        
        String getHostName() {
            return hostName;
        }
        
        String getOriginalHostName() {
            return originalHostName;
        }
        
        int getAddress() {
            return address;
        }
        
        int getFamily() {
            return family;
        }
    }
    
    
    /**
     * NameService provides host and address lookup service
     *
     * @since 9
     */
    // IP-域名映射服务
    private interface NameService {
        
        /**
         * Lookup a host mapping by name. Retrieve the IP addresses associated with a host
         *
         * @param host the specified hostname
         *
         * @return array of IP addresses for the requested host
         *
         * @throws UnknownHostException if no IP address for the {@code host} could be found
         */
        // 将主机名称或主机地址映射为InetAddress实例
        InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException;
        
        /**
         * Lookup the host corresponding to the IP address provided
         *
         * @param addr byte array representing an IP address
         *
         * @return {@code String} representing the host name mapping
         *
         * @throws UnknownHostException if no host found for the specified IP address
         */
        // 根据主机地址查找映射的主机名称
        String getHostByAddr(byte[] addr) throws UnknownHostException;
        
    }
    
    /**
     * The default NameService implementation, which delegates to the underlying OS network libraries to resolve host address mappings.
     *
     * @since 9
     */
    // 默认的NameService实现，它委托给底层OS网络库以解析主机地址映射
    private static final class PlatformNameService implements NameService {
        // 将主机名称或主机地址映射为InetAddress实例
        public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
            return impl.lookupAllHostAddr(host);
        }
        
        // 根据主机地址查找映射的主机名称
        public String getHostByAddr(byte[] addr) throws UnknownHostException {
            return impl.getHostByAddr(addr);
        }
    }
    
    /**
     * The HostsFileNameService provides host address mapping by reading the entries in a hosts file,
     * which is specified by {@code jdk.net.hosts.file} system property
     *
     * The file format is that which corresponds with the /etc/hosts file IP Address host alias list.
     *
     * When the file lookup is enabled it replaces the default NameService implementation
     *
     * @since 9
     */
    /*
     * HostsFileNameService是NameService的实现之一
     * HostsFileNameService通过读取hosts文件中来提供主机地址映射
     * hosts文件的地址通过系统属性jdk.net.hosts.file设置，或使用/etc/hosts文件
     */
    private static final class HostsFileNameService implements NameService {
        
        private final String hostsFile;
        
        public HostsFileNameService(String hostsFileName) {
            this.hostsFile = hostsFileName;
        }
        
        /**
         * Lookup the host name  corresponding to the IP address provided.
         * Search the configured host file a host name corresponding to the specified IP address.
         *
         * @param addr byte array representing an IP address
         *
         * @return {@code String} representing the host name mapping
         *
         * @throws UnknownHostException if no host found for the specified IP address
         */
        // 根据主机地址查找映射的主机名称
        @Override
        public String getHostByAddr(byte[] addr) throws UnknownHostException {
            String hostEntry;
            String host = null;
    
            String addrString = addrToString(addr);
            try(Scanner hostsFileScanner = new Scanner(new File(hostsFile), "UTF_8")) {
                while(hostsFileScanner.hasNextLine()) {
                    hostEntry = hostsFileScanner.nextLine();
                    if(!hostEntry.startsWith("#")) {
                        hostEntry = removeComments(hostEntry);
                        if(hostEntry.contains(addrString)) {
                            host = extractHost(hostEntry, addrString);
                            if(host != null) {
                                break;
                            }
                        }
                    }
                }
            } catch(FileNotFoundException e) {
                throw new UnknownHostException("Unable to resolve address " + addrString + " as hosts file " + hostsFile + " not found ");
            }
    
            if((host == null) || (host.equals("")) || (host.equals(" "))) {
                throw new UnknownHostException("Requested address " + addrString + " resolves to an invalid entry in hosts file " + hostsFile);
            }
            return host;
        }
        
        /**
         * <p>Lookup a host mapping by name. Retrieve the IP addresses
         * associated with a host.
         *
         * <p>Search the configured hosts file for the addresses assocaited with
         * with the specified host name.
         *
         * @param host the specified hostname
         *
         * @return array of IP addresses for the requested host
         *
         * @throws UnknownHostException if no IP address for the {@code host} could be found
         */
        // 将主机名称或主机地址映射为InetAddress实例
        public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
            String hostEntry;
            String addrStr = null;
            InetAddress[] res = null;
            byte[] addr = new byte[4];
            ArrayList<InetAddress> inetAddresses = null;
            
            // lookup the file and create a list InetAddress for the specfied host
            try(Scanner hostsFileScanner = new Scanner(new File(hostsFile), "UTF_8")) {
                while(hostsFileScanner.hasNextLine()) {
                    hostEntry = hostsFileScanner.nextLine();
                    if(!hostEntry.startsWith("#")) {
                        hostEntry = removeComments(hostEntry);
                        if(hostEntry.contains(host)) {
                            addrStr = extractHostAddr(hostEntry, host);
                            if((addrStr != null) && (!addrStr.equals(""))) {
                                // 将文本形式的IP地址转换为字节形式
                                addr = createAddressByteArray(addrStr);
                                if(inetAddresses == null) {
                                    inetAddresses = new ArrayList<>(1);
                                }
                                if(addr != null) {
                                    inetAddresses.add(InetAddress.getByAddress(host, addr));
                                }
                            }
                        }
                    }
                }
            } catch(FileNotFoundException e) {
                throw new UnknownHostException("Unable to resolve host " + host + " as hosts file " + hostsFile + " not found ");
            }
            
            if(inetAddresses != null) {
                res = inetAddresses.toArray(new InetAddress[inetAddresses.size()]);
            } else {
                throw new UnknownHostException("Unable to resolve host " + host + " in hosts file " + hostsFile);
            }
            return res;
        }
        
        private String addrToString(byte[] addr) {
            String stringifiedAddress = null;
            
            // 可能是IP4地址
            if(addr.length == Inet4Address.INADDRSZ) {
                stringifiedAddress = Inet4Address.numericToTextFormat(addr);
                
                // 可能是IP6地址
            } else { // treat as an IPV6 jobby
                // 将IP6地址转换为IP4地址，转换失败则返回null
                byte[] newAddr = IPAddressUtil.convertFromIPv4MappedAddress(addr);
                if(newAddr != null) {
                    // 本质是IP4地址
                    stringifiedAddress = Inet4Address.numericToTextFormat(addr);
                } else {
                    // 确实是IP6地址
                    stringifiedAddress = Inet6Address.numericToTextFormat(addr);
                }
            }
            
            return stringifiedAddress;
        }
        
        private String removeComments(String hostsEntry) {
            String filteredEntry = hostsEntry;
            int hashIndex;
            
            if((hashIndex = hostsEntry.indexOf("#")) != -1) {
                filteredEntry = hostsEntry.substring(0, hashIndex);
            }
            return filteredEntry;
        }
        
        // 将文本形式的IP地址转换为字节形式
        private byte[] createAddressByteArray(String addrStr) {
            byte[] addrArray;
            // 将文本形式的IP4地址转换为二进制形式
            addrArray = IPAddressUtil.textToNumericFormatV4(addrStr);
            if(addrArray == null) {
                // 将文本形式的IP6地址转换为二进制形式
                addrArray = IPAddressUtil.textToNumericFormatV6(addrStr);
            }
            return addrArray;
        }
        
        /** host to ip address mapping */
        private String extractHostAddr(String hostEntry, String host) {
            String[] mapping = hostEntry.split("\\s+");
            String hostAddr = null;
    
            if(mapping.length >= 2) {
                // look at the host aliases
                for(int i = 1; i<mapping.length; i++) {
                    if(mapping[i].equalsIgnoreCase(host)) {
                        hostAddr = mapping[0];
                    }
                }
            }
            return hostAddr;
        }
        
        /**
         * IP Address to host mapping
         * use first host alias in list
         */
        private String extractHost(String hostEntry, String addrString) {
            String[] mapping = hostEntry.split("\\s+");
            String host = null;
    
            if(mapping.length >= 2) {
                if(mapping[0].equalsIgnoreCase(addrString)) {
                    host = mapping[1];
                }
            }
            return host;
        }
    }
    
    
    // 用于缓存的框架的接口
    private interface Addresses {
        // 获取缓存的网络地址
        InetAddress[] get() throws UnknownHostException;
    }
    
    /** a name service lookup based Addresses implementation which replaces itself in cache when the result is obtained */
    // 基于IP-域名映射服务的Addresses实现，需要进一步查询才能获取到网络地址
    private static final class NameServiceAddresses implements Addresses {
        private final String host;              // 待搜索的host
        private final InetAddress reqAddr;      // 期望的网络地址
        
        NameServiceAddresses(String host, InetAddress reqAddr) {
            this.host = host;
            this.reqAddr = reqAddr;
        }
        
        // 基于IP-域名映射服务，查找host对应的InetAddress列表
        @Override
        public InetAddress[] get() throws UnknownHostException {
            Addresses addresses;
            
            // only one thread is doing lookup to name service for particular host at any time.
            synchronized(this) {
                // re-check that we are still us + re-install us if slot empty
                addresses = cache.putIfAbsent(host, this);
                
                // 之前缓存的host失效了
                if(addresses == null) {
                    // this can happen when we were replaced by CachedAddresses in some other thread,
                    // then CachedAddresses expired and were removed from cache while we were waiting for lock...
                    addresses = this;
                }
                
                // 保证缓存存在，且未过期
                if(addresses == this) {
                    // lookup name services
                    InetAddress[] inetAddresses;
                    UnknownHostException ex;
                    int cachePolicy;    // 缓存策略
                    
                    try {
                        // 通过IP-域名映射服务，查询出host对应的网络地址列表。如果reqAddr也在其中，将其设置到列表首位。
                        inetAddresses = getAddressesFromNameService(host, reqAddr);
                        ex = null;
                        // 获取当前的命中缓存策略
                        cachePolicy = InetAddressCachePolicy.get();
                    } catch(UnknownHostException uhe) {
                        inetAddresses = null;
                        ex = uhe;
                        // 获取当前的未命中缓存策略
                        cachePolicy = InetAddressCachePolicy.getNegative();
                    }
                    
                    // remove or replace us with cached addresses according to cachePolicy
                    if(cachePolicy == InetAddressCachePolicy.NEVER) {
                        // 如果不需要缓存该host，则将其从缓存中移除
                        cache.remove(host, this);
                    } else {
                        // 计算过期时间（注意秒到纳秒的换算）
                        long expiryTime = cachePolicy == InetAddressCachePolicy.FOREVER ? 0L : System.nanoTime() + 1000_000_000L * cachePolicy;// cachePolicy is in [s] - we need [ns]
                        
                        // 创建缓存元素
                        CachedAddresses cachedAddresses = new CachedAddresses(host, inetAddresses, expiryTime);
                        
                        // 更新缓存，如果更新成功，且缓存策略不是FOREVER
                        if(cache.replace(host, this, cachedAddresses) && cachePolicy != InetAddressCachePolicy.FOREVER) {
                            // 将缓存元素加入到expirySet
                            expirySet.add(cachedAddresses);
                        }
                    }
                    
                    if(inetAddresses == null) {
                        throw ex == null ? new UnknownHostException(host) : ex;
                    }
                    
                    return inetAddresses;
                }
                
                /* else addresses != this */
            }
            
            /*
             * delegate to different addresses when we are already replaced
             * but outside of synchronized block to avoid any chance of dead-locking
             */
            // 之前缓存的host没有失效，但其对应的Addresses已经更新了，这里需要重新查找
            return addresses.get();
        }
    }
    
    /** a holder for cached addresses with required metadata */
    // 基于IP-域名映射服务查询出来的网络地址，可直接获取到缓存的网络地址
    private static final class CachedAddresses implements Addresses, Comparable<CachedAddresses> {
        final String host;
        final InetAddress[] inetAddresses;
        final long expiryTime; // time of expiry (in terms of System.nanoTime())
        final long id = seq.incrementAndGet(); // each instance is unique
        private static final AtomicLong seq = new AtomicLong();
        
        CachedAddresses(String host, InetAddress[] inetAddresses, long expiryTime) {
            this.host = host;
            this.inetAddresses = inetAddresses;
            this.expiryTime = expiryTime;
        }
        
        // 获取缓存的网络地址
        @Override
        public InetAddress[] get() throws UnknownHostException {
            if(inetAddresses == null) {
                throw new UnknownHostException(host);
            }
            return inetAddresses;
        }
        
        @Override
        public int compareTo(CachedAddresses other) {
            // natural order is expiry time -
            // compare difference of expiry times rather than
            // expiry times directly, to avoid possible overflow.
            // (see System.nanoTime() recommendations...)
            long diff = this.expiryTime - other.expiryTime;
            if(diff<0L) {
                return -1;
            }
            
            if(diff>0L) {
                return 1;
            }
            
            // ties are broken using unique id
            return Long.compare(this.id, other.id);
        }
    }
    
    
    // 缓存本地主机名称和地址
    private static final class CachedLocalHost {
        final String host;
        final InetAddress addr;
        // 缓存时长默认为5秒
        final long expiryTime = System.nanoTime() + 5000_000_000L; // now + 5s;
        
        CachedLocalHost(String host, InetAddress addr) {
            this.host = host;
            this.addr = addr;
        }
    }
    
}
