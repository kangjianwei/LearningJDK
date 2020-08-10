/*
 * Copyright (c) 2003, 2018 Oracle and/or its affiliates. All rights reserved.
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

package sun.net.spi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import sun.net.NetProperties;
import sun.net.SocksProxy;

import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Supports proxy settings using system properties This proxy selector
 * provides backward compatibility with the old http protocol handler
 * as far as how proxy is set
 *
 * Most of the implementation copied from the old http protocol handler
 *
 * Supports http/https/ftp.proxyHost, http/https/ftp.proxyPort,
 * proxyHost, proxyPort, and http/https/ftp.nonProxyHost, and socks.
 * NOTE: need to do gopher as well
 */
// 代理选择器的默认实现
public class DefaultProxySelector extends ProxySelector {
    
    /**
     * This is where we define all the valid System Properties we have to
     * support for each given protocol.
     * The format of this 2 dimensional array is :
     * - 1 row per protocol (http, ftp, ...)
     * - 1st element of each row is the protocol name
     * - subsequent elements are prefixes for Host & Port properties
     * listed in order of priority.
     * Example:
     * {"ftp", "ftp.proxy", "ftpProxy", "proxy", "socksProxy"},
     * means for FTP we try in that oder:
     * + ftp.proxyHost & ftp.proxyPort
     * + ftpProxyHost & ftpProxyPort
     * + proxyHost & proxyPort
     * + socksProxyHost & socksProxyPort
     *
     * Note that the socksProxy should *always* be the last on the list
     */
    // 代理协议及其该协议下可用的前缀信息
    static final String[][] props = {
        /* protocol, Property prefix 1, Property prefix 2, ... */
        {"http", "http.proxy", "proxy", "socksProxy"}, {"https", "https.proxy", "proxy", "socksProxy"}, {"ftp", "ftp.proxy", "ftpProxy", "proxy", "socksProxy"}, {"gopher", "gopherProxy", "socksProxy"}, {"socket", "socksProxy"}};
    
    private static final String SOCKS_PROXY_VERSION = "socksProxyVersion";
    
    private static final List<Proxy> NO_PROXY_LIST = List.of(Proxy.NO_PROXY);
    
    // 是否存在系统代理设置
    private static boolean hasSystemProxies = false;
    
    
    static {
        final String key = "java.net.useSystemProxies";
        
        // 查找预设的"java.net.useSystemProxies"属性的值，用来判断是否需要加载系统代理设置
        Boolean b = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                // 从运行参数中获取布尔类型的网络属性；如果未找到，则从net.properties配置中查找
                return NetProperties.getBoolean(key);
            }
        });
        
        if(b != null && b) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("net");
                    return null;
                }
            });
            
            hasSystemProxies = init();
        }
    }
    
    
    // 返回运行参数中指定的SOCKS代理版本；如果未指定，则使用SOCKS V5版本
    public static int socksProxyVersion() {
        return AccessController.doPrivileged(new PrivilegedAction<Integer>() {
            @Override
            public Integer run() {
                return NetProperties.getInteger(SOCKS_PROXY_VERSION, 5);
            }
        });
    }
    
    /**
     * select() method.
     *
     * Where all the hard work is done.
     * Build a list of proxies depending on URI.
     * Since we're only providing compatibility with the system properties from previous releases (see list above),
     * that list will typically contain one single proxy, default being NO_PROXY.
     * If we can get a system proxy it might contain more entries.
     */
    // 根据指定的目标URI解析出可用的网络代理
    public List<Proxy> select(URI uri) {
        if(uri == null) {
            throw new IllegalArgumentException("URI can't be null.");
        }
        
        // uri中的协议
        String protocol = uri.getScheme();
        
        // uri中的域名/IP
        String host = uri.getHost();
        
        if(host == null) {
            /*
             * This is a hack to ensure backward compatibility in two cases:
             * 1. hostnames contain non-ascii characters, internationalized domain names. in which case, URI will return null, see BugID 4957669;
             * 2. Some hostnames can contain '_' chars even though it's not supposed to be legal, in which case URI will return null for getHost, but not for getAuthority() See BugID 4913253
             */
            // 尝试从登陆信息中解析host
            String auth = uri.getAuthority();
            if(auth != null) {
                int i;
                i = auth.indexOf('@');
                if(i >= 0) {
                    auth = auth.substring(i + 1);
                }
                i = auth.lastIndexOf(':');
                if(i >= 0) {
                    auth = auth.substring(0, i);
                }
                host = auth;
            }
        }
        
        if(protocol == null || host == null) {
            throw new IllegalArgumentException("protocol = " + protocol + " host = " + host);
        }
        
        NonProxyInfo pinfo = null;
        
        // 判断协议类型
        if("http".equalsIgnoreCase(protocol)) {
            pinfo = NonProxyInfo.httpNonProxyInfo;
        } else if("https".equalsIgnoreCase(protocol)) {
            // HTTPS uses the same property as HTTP, for backward compatibility
            pinfo = NonProxyInfo.httpNonProxyInfo;
        } else if("ftp".equalsIgnoreCase(protocol)) {
            pinfo = NonProxyInfo.ftpNonProxyInfo;
        } else if("socket".equalsIgnoreCase(protocol)) {
            pinfo = NonProxyInfo.socksNonProxyInfo;
        }
        
        /*
         * Let's check the System properties for that protocol
         */
        final String proto = protocol;
        final NonProxyInfo nprop = pinfo;
        final String urlhost = host.toLowerCase();
        
        /*
         * This is one big doPrivileged call, but we're trying to optimize the code as much as possible.
         * Since we're checking quite a few System properties it does help having only 1 call to doPrivileged.
         * Be mindful what you do in here though!
         */
        Proxy[] proxyArray = AccessController.doPrivileged(new PrivilegedAction<Proxy[]>() {
            public Proxy[] run() {
                int i, j;
                String phost = null;
                int pport = 0;
                String nphosts = null;
                InetSocketAddress saddr = null;
                
                /* Then let's walk the list of protocols in our array */
                // 遍历系统支持的协议列表，找到与uri匹配的协议，协议名称应当是{"http", "https", "ftp", "gopher", "socket"}中的一种
                for(i = 0; i<props.length; i++) {
                    // 跳过不匹配的协议
                    if(!props[i][0].equalsIgnoreCase(proto)) {
                        continue;
                    }
                    
                    // 遍历该协议支持的前缀
                    for(j = 1; j<props[i].length; j++) {
                        /*
                         * System.getProp() will give us an empty String,
                         * "" for a defined but "empty" property.
                         */
                        /*
                         * 查找与当前协议匹配的代理主机地址
                         *
                         * 先从运行参数中获取指定的网络属性；如果未找到，则从net.properties配置中查找
                         *
                         * 拿"socket"协议举例，这里会查找"socketProxyHost"属性的值
                         */
                        phost = NetProperties.get(props[i][j] + "Host");
                        if(phost != null && phost.length() != 0) {
                            break;
                        }
                    }
                    
                    // 如果未找到与当前协议匹配的主机地址
                    if(phost == null || phost.length() == 0) {
                        /*
                         * No system property defined for that protocol.
                         * Let's check System Proxy settings (Gnome, MacOsX & Windows) if we were instructed to.
                         */
                        // 如果不存在系统代理设置，直接返回
                        if(!hasSystemProxies) {
                            return null;
                        }
                        
                        String sproto;
                        
                        if(proto.equalsIgnoreCase("socket")) {
                            sproto = "socks";   // 尝试将"socket"名称回退为"socks"名称
                        } else {
                            sproto = proto;
                        }
                        
                        // 查询系统代理设置
                        return getSystemProxies(sproto, urlhost);
                    }
                    
                    /* If a Proxy Host is defined for that protocol. Let's get the NonProxyHosts property */
                    if(nprop != null) {
                        /*
                         * 从运行参数中获取指定的网络属性；如果未找到，则从net.properties配置中查找
                         * 这里需要查找的是在无代理下的一些默认设置。
                         */
                        nphosts = NetProperties.get(nprop.property);
                        synchronized(nprop) {
                            if(nphosts == null) {
                                if(nprop.defaultVal != null) {
                                    nphosts = nprop.defaultVal;
                                } else {
                                    nprop.hostsSource = null;
                                    nprop.pattern = null;
                                }
                            } else if(nphosts.length() != 0) {
                                // add the required default patterns
                                // but only if property no set. If it
                                // is empty, leave empty.
                                nphosts += "|" + NonProxyInfo.defStringVal;
                            }
                            
                            if(nphosts != null) {
                                if(!nphosts.equals(nprop.hostsSource)) {
                                    nprop.pattern = toPattern(nphosts);
                                    nprop.hostsSource = nphosts;
                                }
                            }
                            
                            // 判断正则表达式nprop.pattern与目标字符串urlhost是否完全匹配
                            if(shouldNotUseProxyFor(nprop.pattern, urlhost)) {
                                return null;
                            }
                        }
                    }
                    
                    /* We got a host, let's check for port */
                    
                    // 类似地，下面开始查找预设的代理端口号
                    pport = NetProperties.getInteger(props[i][j] + "Port", 0);
                    
                    // 回退，查找其他前缀对应的端口号
                    if(pport == 0 && j<(props[i].length - 1)) {
                        /*
                         * Can't find a port with same prefix as Host.
                         * AND it's not a SOCKS proxy.
                         * Let's try the other prefixes for that proto.
                         */
                        for(int k = 1; k<(props[i].length - 1); k++) {
                            if((k != j) && (pport == 0)) {
                                pport = NetProperties.getInteger(props[i][k] + "Port", 0);
                            }
                        }
                    }
                    
                    // Still couldn't find a port, let's use default
                    if(pport == 0) {
                        if(j == (props[i].length - 1)) {
                            pport = defaultPort("socket");
                        } else {
                            pport = defaultPort(proto);
                        }
                    }
                    
                    // We did find a proxy definition.
                    // Let's create the address, but don't resolve it
                    // as this will be done at connection time
                    saddr = InetSocketAddress.createUnresolved(phost, pport);
                    
                    /* Socks is *always* the last on the list */
                    if(j == (props[i].length - 1)) {
                        // 获取运行参数中指定的SOCKS代理版本；如果未指定，则使用SOCKS V5版本
                        int proxyVersion = socksProxyVersion();
                        // 工厂方法，创建Socket代理对象
                        SocksProxy socksProxy = SocksProxy.create(saddr, proxyVersion);
                        // 返回找到的socket代理信息
                        return new Proxy[]{socksProxy};
                    }
                    
                    return new Proxy[]{new Proxy(Proxy.Type.HTTP, saddr)};
                }
                
                return null;
            }
        });
        
        // If no specific proxy was found, return a standard list containing only one NO_PROXY entry.
        if(proxyArray == null) {
            return NO_PROXY_LIST;
        }
        
        // Remove duplicate entries, while preserving order.
        return Stream.of(proxyArray).distinct().collect(collectingAndThen(toList(), Collections::unmodifiableList));
    }
    
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if(uri == null || sa == null || ioe == null) {
            throw new IllegalArgumentException("Arguments can't be null.");
        }
        // ignored
    }
    
    /**
     * @return {@code true} if given this pattern for non-proxy hosts and this
     * urlhost the proxy should NOT be used to access this urlhost
     */
    // 判断正则表达式pattern与目标字符串urlhost是否完全匹配
    static boolean shouldNotUseProxyFor(Pattern pattern, String urlhost) {
        if(pattern == null || urlhost.isEmpty()) {
            return false;
        }
    
        return pattern.matcher(urlhost).matches();
    }
    
    /**
     * @param mask non-null mask
     *
     * @return {@link java.util.regex.Pattern} corresponding to this mask or {@code null} in case mask should not match anything
     */
    static Pattern toPattern(String mask) {
        boolean disjunctionEmpty = true;
        StringJoiner joiner = new StringJoiner("|");
        for(String disjunct : mask.split("\\|")) {
            if(disjunct.isEmpty()) {
                continue;
            }
            disjunctionEmpty = false;
            String regex = disjunctToRegex(disjunct.toLowerCase());
            joiner.add(regex);
        }
        return disjunctionEmpty ? null : Pattern.compile(joiner.toString());
    }
    
    /**
     * @param disjunct non-null mask disjunct
     *
     * @return java regex string corresponding to this mask
     */
    static String disjunctToRegex(String disjunct) {
        String regex;
    
        if(disjunct.startsWith("*") && disjunct.endsWith("*")) {
            regex = ".*" + quote(disjunct.substring(1, disjunct.length() - 1)) + ".*";
        } else if(disjunct.startsWith("*")) {
            regex = ".*" + quote(disjunct.substring(1));
        } else if(disjunct.endsWith("*")) {
            regex = quote(disjunct.substring(0, disjunct.length() - 1)) + ".*";
        } else {
            regex = quote(disjunct);
        }
    
        return regex;
    }
    
    // 返回指定协议对应的默认端口号；如果协议无法识别，返回-1
    private int defaultPort(String protocol) {
        if("http".equalsIgnoreCase(protocol)) {
            return 80;
        } else if("https".equalsIgnoreCase(protocol)) {
            return 443;
        } else if("ftp".equalsIgnoreCase(protocol)) {
            return 80;
        } else if("socket".equalsIgnoreCase(protocol)) {
            return 1080;
        } else if("gopher".equalsIgnoreCase(protocol)) {
            return 80;
        } else {
            return -1;
        }
    }
    
    private synchronized native Proxy[] getSystemProxies(String protocol, String host);
    
    private static native boolean init();
    
    
    /**
     * How to deal with "non proxy hosts":
     * since we do have to generate a pattern we don't want to do that if it's not necessary.
     * Therefore we do cache the result, on a per-protocol basis,
     * and change it only when the "source", i.e. the system property, did change.
     */
    // 无代理设置
    static class NonProxyInfo {
        // Default value for nonProxyHosts, this provides backward compatibility by excluding localhost and its litteral notations.
        static final String defStringVal = "localhost|127.*|[::1]|0.0.0.0|[::0]";
        
        static NonProxyInfo ftpNonProxyInfo = new NonProxyInfo("ftp.nonProxyHosts", null, null, defStringVal);
        static NonProxyInfo httpNonProxyInfo = new NonProxyInfo("http.nonProxyHosts", null, null, defStringVal);
        static NonProxyInfo socksNonProxyInfo = new NonProxyInfo("socksNonProxyHosts", null, null, defStringVal);
        
        final String property;
        final String defaultVal;
        String hostsSource;
        Pattern pattern;
        
        NonProxyInfo(String p, String s, Pattern pattern, String d) {
            this.property = p;
            this.hostsSource = s;
            this.pattern = pattern;
            this.defaultVal = d;
        }
    }
    
}
