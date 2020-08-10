/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Constants used by the SOCKS protocol implementation.
 */
// SOCKS协议中常用的常量
interface SocksConsts {
    
    static final int DEFAULT_PORT = 1080; // Socket代理默认端口号
    
    // SOCKS协议版本号
    static final int PROTO_VERS4 = 4;    // SOCKS4代理只支持TCP协议（即传输控制协议）
    static final int PROTO_VERS = 5;    // SOCKS5代理既支持TCP协议又支持UDP协议（即用户数据包协议），还支持各种身份验证机制、服务器端域名解析等
    
    // 认证方法，为SOCKS5协议支持
    static final int NO_AUTH = 0;   // 无验证
    static final int GSSAPI = 1;   // 通用安全服务应用程序接口验证，会在将来支持
    static final int USER_PASSW = 2;   // 用户名/密码验证
    static final int NO_METHODS = -1;   // 对验证方法未知
    
    // SOCKS协议指定，由客户端向代理端发送
    static final int CONNECT = 1;    // 连接
    static final int BIND = 2;    // 绑定
    static final int UDP_ASSOC = 3;    // 关联UDP端口
    
    // [远程地址结构]的地址类型
    static final int IPV4 = 1;    // IP4地址
    static final int DOMAIN_NAME = 3;    // 域名
    static final int IPV6 = 4;    // IP6地址
    
    // 代理端对客户端的最终响应
    static final int REQUEST_OK = 0;    // request granted
    static final int GENERAL_FAILURE = 1;    // general failure
    static final int NOT_ALLOWED = 2;    // connection not allowed by ruleset
    static final int NET_UNREACHABLE = 3;    // network unreachable
    static final int HOST_UNREACHABLE = 4;    // host unreachable
    static final int CONN_REFUSED = 5;    // connection refused by destination host
    static final int TTL_EXPIRED = 6;    // TTL expired
    static final int CMD_NOT_SUPPORTED = 7;    // command not supported / protocol error
    static final int ADDR_TYPE_NOT_SUP = 8;    // address type not supported
    
}
