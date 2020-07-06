/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Enumeration;

import static java.net.InetAddress.PREFER_IPV6_VALUE;
import static java.net.InetAddress.PREFER_SYSTEM_VALUE;

/**
 * Package private implementation of InetAddressImpl for dual IPv4/IPv6 stack.
 * <p>
 * If InetAddress.preferIPv6Address is true then anyLocalAddress(),
 * loopbackAddress(), and localHost() will return IPv6 addresses,
 * otherwise IPv4 addresses.
 *
 * @since 1.4
 */
// Inet6Address类的补充
class Inet6AddressImpl implements InetAddressImpl {
    
    private InetAddress anyLocalAddress;    // 通配符地址
    private InetAddress loopbackAddress;    // 本地环回地址
    
    // 本地主机名称
    public native String getLocalHostName() throws UnknownHostException;
    
    // 本地环回地址
    public synchronized InetAddress loopbackAddress() {
        if(loopbackAddress == null) {
            if(InetAddress.preferIPv6Address == PREFER_IPV6_VALUE || InetAddress.preferIPv6Address == PREFER_SYSTEM_VALUE) {
                byte[] loopback = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};
                loopbackAddress = new Inet6Address("localhost", loopback);
            } else {
                loopbackAddress = (new Inet4AddressImpl()).loopbackAddress();
            }
        }
        
        return loopbackAddress;
    }
    
    // 通配符地址（特殊地址，字节全为0）
    public synchronized InetAddress anyLocalAddress() {
        if(anyLocalAddress == null) {
            if(InetAddress.preferIPv6Address == PREFER_IPV6_VALUE || InetAddress.preferIPv6Address == PREFER_SYSTEM_VALUE) {
                anyLocalAddress = new Inet6Address();
                anyLocalAddress.holder().hostName = "::";
            } else {
                anyLocalAddress = (new Inet4AddressImpl()).anyLocalAddress();
            }
        }
        
        return anyLocalAddress;
    }
    
    // 通过指定的网络接口判断给定的网络地址是否可用，ttl代表网络跳数
    public boolean isReachable(InetAddress addr, int timeout, NetworkInterface netif, int ttl) throws IOException {
        byte[] ifaddr = null;
        int scope = -1;
        int netif_scope = -1;
        
        if(netif != null) {
            /*
             * Let's make sure we bind to an address of the proper family.
             * Which means same family as addr because at this point it could
             * be either an IPv6 address or an IPv4 address (case of a dual
             * stack system).
             */
            Enumeration<InetAddress> it = netif.getInetAddresses();
            InetAddress inetaddr = null;
            while(it.hasMoreElements()) {
                inetaddr = it.nextElement();
                if(inetaddr.getClass().isInstance(addr)) {
                    ifaddr = inetaddr.getAddress();
                    if(inetaddr instanceof Inet6Address) {
                        netif_scope = ((Inet6Address) inetaddr).getScopeId();
                    }
                    break;
                }
            }
            if(ifaddr == null) {
                // Interface doesn't support the address family of the destination
                return false;
            }
        }
        
        if(addr instanceof Inet6Address) {
            scope = ((Inet6Address) addr).getScopeId();
        }
        
        return isReachable0(addr.getAddress(), scope, timeout, ifaddr, ttl, netif_scope);
    }
    
    private native boolean isReachable0(byte[] addr, int scope, int timeout, byte[] inf, int ttl, int if_scope) throws IOException;
    
    
    // 将主机名称或主机地址映射为InetAddress实例
    public native InetAddress[] lookupAllHostAddr(String hostname) throws UnknownHostException;
    
    // 根据主机地址查找映射的主机名称
    public native String getHostByAddr(byte[] addr) throws UnknownHostException;
    
}
