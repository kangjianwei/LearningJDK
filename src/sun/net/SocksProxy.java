/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.net;

import java.net.Proxy;
import java.net.SocketAddress;

/**
 * Proxy wrapper class so we can determine the socks protocol version.
 */
// Socket代理包装类，代理类型为SOCKS
public final class SocksProxy extends Proxy {
    
    /*
     * Socket代理版本，分为SOCKS4和SOCKS5
     *
     * SOCKS4和SOCKS5的不同是：
     * SOCKS4代理只支持TCP协议（即传输控制协议），而SOCKS5代理则既支持TCP协议又支持UDP协议（即用户数据包协议），还支持各种身份验证机制、服务器端域名解析等。
     * SOCK4能做到的SOCKS5都可以做到，但SOCKS5能够做到的SOCK4则不一定能做到，比如常用的通讯工具QQ在使用代理时就要求用SOCKS5代理，因为它需要使用UDP协议来传输数据。
     */
    private final int version;
    
    private SocksProxy(SocketAddress addr, int version) {
        super(Proxy.Type.SOCKS, addr);
        this.version = version;
    }
    
    // 工厂方法，创建Socket代理对象
    public static SocksProxy create(SocketAddress addr, int version) {
        return new SocksProxy(addr, version);
    }
    
    // 获取Socket代理的版本
    public int protocolVersion() {
        return version;
    }
    
}
