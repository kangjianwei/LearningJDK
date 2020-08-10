/*
 * Copyright (c) 2002, 2005, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Package private interface to "implementation" used by {@link InetAddress}.
 *
 * See {@link java.net.Inet4AddressImpl} and {@link java.net.Inet6AddressImpl}.
 *
 * @since 1.4
 */
// InetAddress类的一些补充，主要用来映射主机地址/名称，以及判断网络地址的可用性
interface InetAddressImpl {
    
    // 本地主机名称
    String getLocalHostName() throws UnknownHostException;
    
    // 本地环回地址
    InetAddress loopbackAddress();
    
    // 通配符地址（特殊地址，字节全为0）
    InetAddress anyLocalAddress();
    
    // 通过指定的网络接口判断给定的网络地址是否可用，ttl代表网络跳数
    boolean isReachable(InetAddress addr, int timeout, NetworkInterface netif, int ttl) throws IOException;
    
    // 将主机名称或主机地址映射为InetAddress实例
    InetAddress[] lookupAllHostAddr(String hostname) throws UnknownHostException;
    
    // 根据主机地址查找映射的主机名称
    String getHostByAddr(byte[] addr) throws UnknownHostException;
    
}
