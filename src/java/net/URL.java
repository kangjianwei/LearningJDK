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

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.net.spi.URLStreamHandlerProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import jdk.internal.misc.JavaNetURLAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.VM;
import sun.net.ApplicationProxy;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;

/**
 * Class {@code URL} represents a Uniform Resource
 * Locator, a pointer to a "resource" on the World
 * Wide Web. A resource can be something as simple as a file or a
 * directory, or it can be a reference to a more complicated object,
 * such as a query to a database or to a search engine. More
 * information on the types of URLs and their formats can be found at:
 * <a href=
 * "http://web.archive.org/web/20051219043731/http://archive.ncsa.uiuc.edu/SDG/Software/Mosaic/Demo/url-primer.html">
 * <i>Types of URL</i></a>
 * <p>
 * In general, a URL can be broken into several parts. Consider the
 * following example:
 * <blockquote><pre>
 *     http://www.example.com/docs/resource1.html
 * </pre></blockquote>
 * <p>
 * The URL above indicates that the protocol to use is
 * {@code http} (HyperText Transfer Protocol) and that the
 * information resides on a host machine named
 * {@code www.example.com}. The information on that host
 * machine is named {@code /docs/resource1.html}. The exact
 * meaning of this name on the host machine is both protocol
 * dependent and host dependent. The information normally resides in
 * a file, but it could be generated on the fly. This component of
 * the URL is called the <i>path</i> component.
 * <p>
 * A URL can optionally specify a "port", which is the
 * port number to which the TCP connection is made on the remote host
 * machine. If the port is not specified, the default port for
 * the protocol is used instead. For example, the default port for
 * {@code http} is {@code 80}. An alternative port could be
 * specified as:
 * <blockquote><pre>
 *     http://www.example.com:1080/docs/resource1.html
 * </pre></blockquote>
 * <p>
 * The syntax of {@code URL} is defined by  <a
 * href="http://www.ietf.org/rfc/rfc2396.txt"><i>RFC&nbsp;2396: Uniform
 * Resource Identifiers (URI): Generic Syntax</i></a>, amended by <a
 * href="http://www.ietf.org/rfc/rfc2732.txt"><i>RFC&nbsp;2732: Format for
 * Literal IPv6 Addresses in URLs</i></a>. The Literal IPv6 address format
 * also supports scope_ids. The syntax and usage of scope_ids is described
 * <a href="Inet6Address.html#scoped">here</a>.
 * <p>
 * A URL may have appended to it a "fragment", also known
 * as a "ref" or a "reference". The fragment is indicated by the sharp
 * sign character "#" followed by more characters. For example,
 * <blockquote><pre>
 *     http://java.sun.com/index.html#chapter1
 * </pre></blockquote>
 * <p>
 * This fragment is not technically part of the URL. Rather, it
 * indicates that after the specified resource is retrieved, the
 * application is specifically interested in that part of the
 * document that has the tag {@code chapter1} attached to it. The
 * meaning of a tag is resource specific.
 * <p>
 * An application can also specify a "relative URL",
 * which contains only enough information to reach the resource
 * relative to another URL. Relative URLs are frequently used within
 * HTML pages. For example, if the contents of the URL:
 * <blockquote><pre>
 *     http://java.sun.com/index.html
 * </pre></blockquote>
 * contained within it the relative URL:
 * <blockquote><pre>
 *     FAQ.html
 * </pre></blockquote>
 * it would be a shorthand for:
 * <blockquote><pre>
 *     http://java.sun.com/FAQ.html
 * </pre></blockquote>
 * <p>
 * The relative URL need not specify all the components of a URL. If
 * the protocol, host name, or port number is missing, the value is
 * inherited from the fully specified URL. The file component must be
 * specified. The optional fragment is not inherited.
 * <p>
 * The URL class does not itself encode or decode any URL components
 * according to the escaping mechanism defined in RFC2396. It is the
 * responsibility of the caller to encode any fields, which need to be
 * escaped prior to calling URL, and also to decode any escaped fields,
 * that are returned from URL. Furthermore, because URL has no knowledge
 * of URL escaping, it does not recognise equivalence between the encoded
 * or decoded form of the same URL. For example, the two URLs:<br>
 * <pre>    http://foo.com/hello world/ and http://foo.com/hello%20world</pre>
 * would be considered not equal to each other.
 * <p>
 * Note, the {@link java.net.URI} class does perform escaping of its
 * component fields in certain circumstances. The recommended way
 * to manage the encoding and decoding of URLs is to use {@link java.net.URI},
 * and to convert between these two classes using {@link #toURI()} and
 * {@link URI#toURL()}.
 * <p>
 * The {@link URLEncoder} and {@link URLDecoder} classes can also be
 * used, but only for HTML form encoding, which is not the same
 * as the encoding scheme defined in RFC2396.
 *
 * @author James Gosling
 * @since 1.0
 */
/*
 * 统一资源定位符URL，属于URI的一种分类
 *
 * URL       = [scheme:][//authority]path[?query][#reference]
 * authority = [userinfo@]host[:port]
 */
public final class URL implements Serializable {
    
    static final long serialVersionUID = -7627629688361524110L;
    
    static final String BUILTIN_HANDLERS_PREFIX = "sun.net.www.protocol";
    
    /**
     * The property which specifies the package prefix list to be scanned
     * for protocol handlers.  The value of this property (if any) should
     * be a vertical bar delimited list of package names to search through
     * for a protocol handler to load.  The policy of this class is that
     * all protocol handlers will be in a class called <protocolname>.Handler,
     * and each package in the list is examined in turn for a matching
     * handler.  If none are found (or the property is not specified), the
     * default package prefix, sun.net.www.protocol, is used.  The search
     * proceeds from the first package in the list to the last and stops
     * when a match is found.
     */
    private static final String protocolPathProp = "java.protocol.handler.pkgs";
    
    // The protocol to use (ftp, http, nntp, ... etc.)
    private String protocol;            // 协议
    // The authority part of this URL
    private String authority;           // 登录信息
    // The userinfo part of this URL
    private transient String userInfo;  // 用户信息
    // The host name to connect to
    private String host;                // 主机地址
    // The protocol port to connect to
    private int port = -1;              // 端口号
    // The path part of this URL
    private transient String path;      // 路径
    // The query part of this URL
    private transient String query;     // 查询串
    // #reference
    private String ref;                 // 锚点，相当于URI中的fragment
    
    
    /**
     * The specified file name on that host.
     * {@code file} is defined as {@code path[?query]}
     */
    private String file;                // 文件路径，通常是path[?query]
    
    /**
     * The host's IP address, used in equals and hashCode.
     * Computed on demand. An uninitialized or unknown hostAddress is null.
     */
    transient InetAddress hostAddress;  // 主机地址
    
    
    /**
     * The URLStreamHandler for this URL.
     */
    // 流协议处理器
    transient URLStreamHandler handler;
    
    /*
     * A table of protocol handlers
     */
    // 缓存使用过的协议及其对应的流协议处理器
    static Hashtable<String, URLStreamHandler> handlers = new Hashtable<>();
    
    /**
     * The URLStreamHandler factory
     */
    // 流协议处理器的工厂
    private static volatile URLStreamHandlerFactory factory;
    
    // 内置的流协议处理器工厂
    private static final URLStreamHandlerFactory defaultFactory = new DefaultFactory();
    
    private static final Object streamHandlerLock = new Object();
    
    // Thread-local gate to prevent recursive provider lookups
    private static ThreadLocal<Object> gate = new ThreadLocal<>();
    
    
    /**
     * Our hash code.
     *
     * @serial
     */
    private int hashCode = -1;
    
    // 存储反序列化对象
    private transient UrlDeserializedState tempState;
    
    
    static {
        SharedSecrets.setJavaNetURLAccess(new JavaNetURLAccess() {
            @Override
            public URLStreamHandler getHandler(URL url) {
                return url.handler;
            }
        });
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a URL from the specified {@code protocol}
     * name, {@code host} name, and {@code file} name. The
     * default port for the specified protocol is used.
     * <p>
     * This constructor is equivalent to the four-argument
     * constructor with the only difference of using the
     * default port for the specified protocol.
     *
     * No validation of the inputs is performed by this constructor.
     *
     * @param protocol the name of the protocol to use.
     * @param host     the name of the host.
     * @param file     the file on the host.
     *
     * @throws MalformedURLException if an unknown protocol is specified.
     * @see java.net.URL#URL(java.lang.String, java.lang.String,
     * int, java.lang.String)
     */
    public URL(String protocol, String host, String file) throws MalformedURLException {
        this(protocol, host, -1, file);
    }
    
    /**
     * Creates a {@code URL} object from the specified
     * {@code protocol}, {@code host}, {@code port}
     * number, and {@code file}.<p>
     *
     * {@code host} can be expressed as a host name or a literal
     * IP address. If IPv6 literal address is used, it should be
     * enclosed in square brackets ({@code '['} and {@code ']'}), as
     * specified by <a
     * href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>;
     * However, the literal IPv6 address format defined in <a
     * href="http://www.ietf.org/rfc/rfc2373.txt"><i>RFC&nbsp;2373: IP
     * Version 6 Addressing Architecture</i></a> is also accepted.<p>
     *
     * Specifying a {@code port} number of {@code -1}
     * indicates that the URL should use the default port for the
     * protocol.<p>
     *
     * If this is the first URL object being created with the specified
     * protocol, a <i>stream protocol handler</i> object, an instance of
     * class {@code URLStreamHandler}, is created for that protocol:
     * <ol>
     * <li>If the application has previously set up an instance of
     *     {@code URLStreamHandlerFactory} as the stream handler factory,
     *     then the {@code createURLStreamHandler} method of that instance
     *     is called with the protocol string as an argument to create the
     *     stream protocol handler.
     * <li>If no {@code URLStreamHandlerFactory} has yet been set up,
     *     or if the factory's {@code createURLStreamHandler} method
     *     returns {@code null}, then the {@linkplain java.util.ServiceLoader
     *     ServiceLoader} mechanism is used to locate {@linkplain
     *     java.net.spi.URLStreamHandlerProvider URLStreamHandlerProvider}
     *     implementations using the system class
     *     loader. The order that providers are located is implementation
     *     specific, and an implementation is free to cache the located
     *     providers. A {@linkplain java.util.ServiceConfigurationError
     *     ServiceConfigurationError}, {@code Error} or {@code RuntimeException}
     *     thrown from the {@code createURLStreamHandler}, if encountered, will
     *     be propagated to the calling thread. The {@code
     *     createURLStreamHandler} method of each provider, if instantiated, is
     *     invoked, with the protocol string, until a provider returns non-null,
     *     or all providers have been exhausted.
     * <li>If the previous step fails to find a protocol handler, the
     *     constructor reads the value of the system property:
     *     <blockquote>{@code
     *         java.protocol.handler.pkgs
     *     }</blockquote>
     *     If the value of that system property is not {@code null},
     *     it is interpreted as a list of packages separated by a vertical
     *     slash character '{@code |}'. The constructor tries to load
     *     the class named:
     *     <blockquote>{@code
     *         <package>.<protocol>.Handler
     *     }</blockquote>
     *     where {@code <package>} is replaced by the name of the package
     *     and {@code <protocol>} is replaced by the name of the protocol.
     *     If this class does not exist, or if the class exists but it is not
     *     a subclass of {@code URLStreamHandler}, then the next package
     *     in the list is tried.
     * <li>If the previous step fails to find a protocol handler, then the
     *     constructor tries to load a built-in protocol handler.
     *     If this class does not exist, or if the class exists but it is not a
     *     subclass of {@code URLStreamHandler}, then a
     *     {@code MalformedURLException} is thrown.
     * </ol>
     *
     * <p>Protocol handlers for the following protocols are guaranteed
     * to exist on the search path :-
     * <blockquote><pre>
     *     http, https, file, and jar
     * </pre></blockquote>
     * Protocol handlers for additional protocols may also be  available.
     * Some protocol handlers, for example those used for loading platform
     * classes or classes on the class path, may not be overridden. The details
     * of such restrictions, and when those restrictions apply (during
     * initialization of the runtime for example), are implementation specific
     * and therefore not specified
     *
     * <p>No validation of the inputs is performed by this constructor.
     *
     * @param protocol the name of the protocol to use.
     * @param host     the name of the host.
     * @param port     the port number on the host.
     * @param file     the file on the host
     *
     * @throws MalformedURLException if an unknown protocol or the port
     *                               is a negative number other than -1
     * @see java.lang.System#getProperty(java.lang.String)
     * @see java.net.URL#setURLStreamHandlerFactory(
     *java.net.URLStreamHandlerFactory)
     * @see java.net.URLStreamHandler
     * @see java.net.URLStreamHandlerFactory#createURLStreamHandler(
     *java.lang.String)
     */
    public URL(String protocol, String host, int port, String file) throws MalformedURLException {
        this(protocol, host, port, file, null);
    }
    
    /**
     * Creates a {@code URL} object from the specified
     * {@code protocol}, {@code host}, {@code port}
     * number, {@code file}, and {@code handler}. Specifying
     * a {@code port} number of {@code -1} indicates that
     * the URL should use the default port for the protocol. Specifying
     * a {@code handler} of {@code null} indicates that the URL
     * should use a default stream handler for the protocol, as outlined
     * for:
     * java.net.URL#URL(java.lang.String, java.lang.String, int,
     * java.lang.String)
     *
     * <p>If the handler is not null and there is a security manager,
     * the security manager's {@code checkPermission}
     * method is called with a
     * {@code NetPermission("specifyStreamHandler")} permission.
     * This may result in a SecurityException.
     *
     * No validation of the inputs is performed by this constructor.
     *
     * @param protocol the name of the protocol to use.
     * @param host     the name of the host.
     * @param port     the port number on the host.
     * @param file     the file on the host
     * @param handler  the stream handler for the URL.
     *
     * @throws MalformedURLException if an unknown protocol or the port
     *                               is a negative number other than -1
     * @throws SecurityException     if a security manager exists and its
     *                               {@code checkPermission} method doesn't allow
     *                               specifying a stream handler explicitly.
     * @see java.lang.System#getProperty(java.lang.String)
     * @see java.net.URL#setURLStreamHandlerFactory(
     *java.net.URLStreamHandlerFactory)
     * @see java.net.URLStreamHandler
     * @see java.net.URLStreamHandlerFactory#createURLStreamHandler(
     *java.lang.String)
     * @see SecurityManager#checkPermission
     * @see java.net.NetPermission
     */
    public URL(String protocol, String host, int port, String file, URLStreamHandler handler) throws MalformedURLException {
        if(handler != null) {
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                // check for permission to specify a handler
                checkSpecifyHandler(sm);
            }
        }
        
        // 协议名转小写
        protocol = toLowerCase(protocol);
        
        this.protocol = protocol;
        
        if(host != null) {
            /* if host is a literal IPv6 address, we will make it conform to RFC 2732 */
            if(host.indexOf(':') >= 0 && !host.startsWith("[")) {
                host = "[" + host + "]";
            }
            
            this.host = host;
            
            // 有host时必须有port
            if(port<-1) {
                throw new MalformedURLException("Invalid port number :" + port);
            }
            
            this.port = port;
            
            authority = (port == -1) ? host : host + ":" + port;
        }
        
        int index = file.indexOf('#');
        
        this.ref = index<0 ? null : file.substring(index + 1);
        
        file = index<0 ? file : file.substring(0, index);
        
        int q = file.lastIndexOf('?');
        
        if(q != -1) {
            this.query = file.substring(q + 1);
            this.path = file.substring(0, q);
            this.file = path + "?" + query;
        } else {
            this.path = file;
            this.file = path;
        }
        
        /*
         * Note: we don't do validation of the URL here.
         * Too risky to change right now, but worth considering for future reference.
         */
        
        // 获取指定协议的URLStreamHandler
        if(handler == null && (handler = getURLStreamHandler(protocol)) == null) {
            throw new MalformedURLException("unknown protocol: " + protocol);
        }
        
        this.handler = handler;
    }
    
    /**
     * Creates a {@code URL} object from the {@code String} representation.
     * <p>
     * This constructor is equivalent to a call to the two-argument
     * constructor with a {@code null} first argument.
     *
     * @param spec the {@code String} to parse as a URL.
     *
     * @throws MalformedURLException if no protocol is specified, or an
     *                               unknown protocol is found, or {@code spec} is {@code null},
     *                               or the parsed URL fails to comply with the specific syntax
     *                               of the associated protocol.
     * @see java.net.URL#URL(java.net.URL, java.lang.String)
     */
    // 返回使用spec构造的URL
    public URL(String spec) throws MalformedURLException {
        this(null, spec);
    }
    
    /**
     * Creates a URL by parsing the given spec within a specified context.
     *
     * The new URL is created from the given context URL and the spec
     * argument as described in
     * RFC2396 &quot;Uniform Resource Identifiers : Generic * Syntax&quot; :
     * <blockquote><pre>
     *          &lt;scheme&gt;://&lt;authority&gt;&lt;path&gt;?&lt;query&gt;#&lt;fragment&gt;
     * </pre></blockquote>
     * The reference is parsed into the scheme, authority, path, query and
     * fragment parts. If the path component is empty and the scheme,
     * authority, and query components are undefined, then the new URL is a
     * reference to the current document. Otherwise, the fragment and query
     * parts present in the spec are used in the new URL.
     * <p>
     * If the scheme component is defined in the given spec and does not match
     * the scheme of the context, then the new URL is created as an absolute
     * URL based on the spec alone. Otherwise the scheme component is inherited
     * from the context URL.
     * <p>
     * If the authority component is present in the spec then the spec is
     * treated as absolute and the spec authority and path will replace the
     * context authority and path. If the authority component is absent in the
     * spec then the authority of the new URL will be inherited from the
     * context.
     * <p>
     * If the spec's path component begins with a slash character
     * &quot;/&quot; then the
     * path is treated as absolute and the spec path replaces the context path.
     * <p>
     * Otherwise, the path is treated as a relative path and is appended to the
     * context path, as described in RFC2396. Also, in this case,
     * the path is canonicalized through the removal of directory
     * changes made by occurrences of &quot;..&quot; and &quot;.&quot;.
     * <p>
     * For a more detailed description of URL parsing, refer to RFC2396.
     *
     * @param context the context in which to parse the specification.
     * @param spec    the {@code String} to parse as a URL.
     *
     * @throws MalformedURLException if no protocol is specified, or an
     *                               unknown protocol is found, or {@code spec} is {@code null},
     *                               or the parsed URL fails to comply with the specific syntax
     *                               of the associated protocol.
     * @see java.net.URL#URL(java.lang.String, java.lang.String,
     * int, java.lang.String)
     * @see java.net.URLStreamHandler
     * @see java.net.URLStreamHandler#parseURL(java.net.URL,
     * java.lang.String, int, int)
     */
    // 返回使用spec构造的基于context的URL
    public URL(URL context, String spec) throws MalformedURLException {
        this(context, spec, null);
    }
    
    /**
     * Creates a URL by parsing the given spec with the specified handler within a specified context.
     * If the handler is null, the parsing occurs as with the two argument constructor.
     *
     * @param context the context in which to parse the specification.
     * @param spec    the {@code String} to parse as a URL.
     * @param handler the stream handler for the URL.
     *
     * @throws MalformedURLException if no protocol is specified, or an
     *                               unknown protocol is found, or {@code spec} is {@code null},
     *                               or the parsed URL fails to comply with the specific syntax
     *                               of the associated protocol.
     * @throws SecurityException     if a security manager exists and its
     *                               {@code checkPermission} method doesn't allow
     *                               specifying a stream handler.
     * @see java.net.URL#URL(java.lang.String, java.lang.String, int, java.lang.String)
     * @see java.net.URLStreamHandler
     * @see java.net.URLStreamHandler#parseURL(java.net.URL, java.lang.String, int, int)
     */
    // 基于context解析spec，从spec中解析到的URL组件会覆盖到context中以形成新的URL返回，如果spec是相对路径，则会追加在context原有路径上；handler显式指定了URLStreamHandler
    public URL(URL context, String spec, URLStreamHandler handler) throws MalformedURLException {
        String original = spec;
        int i, limit, c;
        int start = 0;
        String newProtocol = null;
        boolean aRef = false;
        boolean isRelative = false;
        
        // Check for permission to specify a handler
        if(handler != null) {
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                checkSpecifyHandler(sm);
            }
        }
        
        try {
            limit = spec.length();
            
            // 忽略spec后面的空白
            while((limit>0) && (spec.charAt(limit - 1)<=' ')) {
                limit--;        //eliminate trailing whitespace
            }
            
            // 忽略spec前面的空白
            while((start<limit) && (spec.charAt(start)<=' ')) {
                start++;        // eliminate leading whitespace
            }
            
            // 忽略spec前面的"url:"
            if(spec.regionMatches(true, start, "url:", 0, 4)) {
                start += 4;
            }
            
            // 如果此时spec以"#"开头，则说明是spec锚点
            if(start<spec.length() && spec.charAt(start) == '#') {
                /*
                 * we're assuming this is a ref relative to the context URL.
                 * This means protocols cannot start w/ '#', but we must parse ref URL's like: "hello:there" w/ a ':' in them.
                 */
                aRef = true;
            }
            
            // 在spec不是锚点的情形下，继续向后查找":"，如果遇到"/"则结束
            for(i = start; !aRef && (i<limit) && ((c = spec.charAt(i)) != '/'); i++) {
                if(c != ':') {
                    continue;
                }
                
                /* 至此，找到了":" */
                
                // 截取":"之前的部分
                String s = toLowerCase(spec.substring(start, i));
                
                // spec中遇到了协议名称
                if(isValidProtocol(s)) {
                    newProtocol = s;
                    start = i + 1;
                }
                
                break;
            }
            
            // Only use our context if the protocols match.
            protocol = newProtocol; // 保存新协议名称
            
            if((context != null) && ((newProtocol == null) || newProtocol.equalsIgnoreCase(context.protocol))) {
                // inherit the protocol handler from the context if not specified to the constructor
                if(handler == null) {
                    handler = context.handler;
                }
                
                // If the context is a hierarchical URL scheme and the spec
                // contains a matching scheme then maintain backwards
                // compatibility and treat it as if the spec didn't contain
                // the scheme; see 5.2.3 of RFC2396
                if(context.path != null && context.path.startsWith("/")) {
                    newProtocol = null;
                }
                
                if(newProtocol == null) {
                    protocol = context.protocol;
                    authority = context.authority;
                    userInfo = context.userInfo;
                    host = context.host;
                    port = context.port;
                    file = context.file;
                    path = context.path;
                    isRelative = true;
                }
            }
            
            if(protocol == null) {
                throw new MalformedURLException("no protocol: " + original);
            }
            
            /* Get the protocol handler if not specified or the protocol of the context could not be used */
            // 获取指定协议的URLStreamHandler
            if(handler == null && (handler = getURLStreamHandler(protocol)) == null) {
                throw new MalformedURLException("unknown protocol: " + protocol);
            }
            
            this.handler = handler;
            
            i = spec.indexOf('#', start);
            if(i >= 0) {
                ref = spec.substring(i + 1, limit);
                limit = i;
            }
            
            // Handle special case inheritance of query and fragment implied by RFC2396 section 5.2.2
            if(isRelative && start == limit) {
                query = context.query;
                if(ref == null) {
                    ref = context.ref;
                }
            }
            
            // 基于当前url解析spec中指定范围内的字符串，从spec中解析到的URL组件会覆盖到当前url中以形成新的URL返回，如果spec是相对路径，则会追加在当前url的原有路径上
            handler.parseURL(this, spec, start, limit);
            
        } catch(MalformedURLException e) {
            throw e;
        } catch(Exception e) {
            MalformedURLException exception = new MalformedURLException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ URI组成部分 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the protocol name of this {@code URL}.
     *
     * @return the protocol of this {@code URL}.
     */
    // 返回协议(protocol)
    public String getProtocol() {
        return protocol;
    }
    
    /**
     * Gets the authority part of this {@code URL}.
     *
     * @return the authority part of this {@code URL}
     *
     * @since 1.3
     */
    // 返回登录信息(authority)
    public String getAuthority() {
        return authority;
    }
    
    /**
     * Gets the userInfo part of this {@code URL}.
     *
     * @return the userInfo part of this {@code URL}, or
     * <CODE>null</CODE> if one does not exist
     *
     * @since 1.3
     */
    // 返回用户信息(userinfo)
    public String getUserInfo() {
        return userInfo;
    }
    
    /**
     * Gets the host name of this {@code URL}, if applicable.
     * The format of the host conforms to RFC 2732, i.e. for a
     * literal IPv6 address, this method will return the IPv6 address
     * enclosed in square brackets ({@code '['} and {@code ']'}).
     *
     * @return the host name of this {@code URL}.
     */
    // 返回主机(host)
    public String getHost() {
        return host;
    }
    
    /**
     * Gets the port number of this {@code URL}.
     *
     * @return the port number, or -1 if the port is not set
     */
    // 返回端口(port)
    public int getPort() {
        return port;
    }
    
    /**
     * Gets the path part of this {@code URL}.
     *
     * @return the path part of this {@code URL}, or an
     * empty string if one does not exist
     *
     * @since 1.3
     */
    // 返回路径(path)
    public String getPath() {
        return path;
    }
    
    /**
     * Gets the query part of this {@code URL}.
     *
     * @return the query part of this {@code URL},
     * or <CODE>null</CODE> if one does not exist
     *
     * @since 1.3
     */
    // 返回查询串(query)
    public String getQuery() {
        return query;
    }
    
    /**
     * Gets the anchor (also known as the "reference") of this
     * {@code URL}.
     *
     * @return the anchor (also known as the "reference") of this
     * {@code URL}, or <CODE>null</CODE> if one does not exist
     */
    // 返回锚点(fragment)
    public String getRef() {
        return ref;
    }
    
    
    /**
     * Gets the default port number of the protocol associated
     * with this {@code URL}. If the URL scheme or the URLStreamHandler
     * for the URL do not define a default port number,
     * then -1 is returned.
     *
     * @return the port number
     *
     * @since 1.4
     */
    // 返回当前协议下的默认端口，有些协议需要端口支持
    public int getDefaultPort() {
        return handler.getDefaultPort();
    }
    
    /**
     * Gets the file name of this {@code URL}.
     * The returned file portion will be
     * the same as <CODE>getPath()</CODE>, plus the concatenation of
     * the value of <CODE>getQuery()</CODE>, if any. If there is
     * no query portion, this method and <CODE>getPath()</CODE> will
     * return identical results.
     *
     * @return the file name of this {@code URL},
     * or an empty string if one does not exist
     */
    // 返回文件名
    public String getFile() {
        return file;
    }
    
    /*▲ URI组成部分 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 流协议处理器的工厂 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets an application's {@code URLStreamHandlerFactory}.
     * This method can be called at most once in a given Java Virtual
     * Machine.
     *
     * <p> The {@code URLStreamHandlerFactory} instance is used to
     * construct a stream protocol handler from a protocol name.
     *
     * <p> If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param fac the desired factory.
     *
     * @throws Error             if the application has already set a factory.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkSetFactory} method doesn't allow
     *                           the operation.
     * @see java.net.URL#URL(java.lang.String, java.lang.String,
     * int, java.lang.String)
     * @see java.net.URLStreamHandlerFactory
     * @see SecurityManager#checkSetFactory
     */
    // 设置流协议处理器的工厂，这是静态方法，需要在构造URL对象之前调用
    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory fac) {
        synchronized(streamHandlerLock) {
            if(factory != null) {
                throw new Error("factory already defined");
            }
            
            SecurityManager security = System.getSecurityManager();
            if(security != null) {
                security.checkSetFactory();
            }
            
            handlers.clear();
            
            // safe publication of URLStreamHandlerFactory with volatile write
            factory = fac;
        }
    }
    
    /*▲ 流协议处理器的工厂 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 资源连接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a {@link java.net.URLConnection URLConnection} instance that
     * represents a connection to the remote object referred to by the
     * {@code URL}.
     *
     * <P>A new instance of {@linkplain java.net.URLConnection URLConnection} is
     * created every time when invoking the
     * {@linkplain java.net.URLStreamHandler#openConnection(URL)
     * URLStreamHandler.openConnection(URL)} method of the protocol handler for
     * this URL.</P>
     *
     * <P>It should be noted that a URLConnection instance does not establish
     * the actual network connection on creation. This will happen only when
     * calling {@linkplain java.net.URLConnection#connect() URLConnection.connect()}.</P>
     *
     * <P>If for the URL's protocol (such as HTTP or JAR), there
     * exists a public, specialized URLConnection subclass belonging
     * to one of the following packages or one of their subpackages:
     * java.lang, java.io, java.util, java.net, the connection
     * returned will be of that subclass. For example, for HTTP an
     * HttpURLConnection will be returned, and for JAR a
     * JarURLConnection will be returned.</P>
     *
     * @return a {@link java.net.URLConnection URLConnection} linking
     * to the URL.
     *
     * @throws IOException if an I/O exception occurs.
     * @see java.net.URL#URL(java.lang.String, java.lang.String,
     * int, java.lang.String)
     */
    // 打开URL资源连接
    public URLConnection openConnection() throws IOException {
        return handler.openConnection(this);
    }
    
    /**
     * Same as {@link #openConnection()}, except that the connection will be
     * made through the specified proxy; Protocol handlers that do not
     * support proxing will ignore the proxy parameter and make a
     * normal connection.
     *
     * Invoking this method preempts the system's default
     * {@link java.net.ProxySelector ProxySelector} settings.
     *
     * @param proxy the Proxy through which this connection
     *              will be made. If direct connection is desired,
     *              Proxy.NO_PROXY should be specified.
     *
     * @return a {@code URLConnection} to the URL.
     *
     * @throws IOException                   if an I/O exception occurs.
     * @throws SecurityException             if a security manager is present
     *                                       and the caller doesn't have permission to connect
     *                                       to the proxy.
     * @throws IllegalArgumentException      will be thrown if proxy is null,
     *                                       or proxy has the wrong type
     * @throws UnsupportedOperationException if the subclass that
     *                                       implements the protocol handler doesn't support
     *                                       this method.
     * @see java.net.URL#URL(java.lang.String, java.lang.String,
     * int, java.lang.String)
     * @see java.net.URLConnection
     * @see java.net.URLStreamHandler#openConnection(java.net.URL,
     * java.net.Proxy)
     * @since 1.5
     */
    // 使用指定的代理打开URL资源连接
    public URLConnection openConnection(java.net.Proxy proxy) throws java.io.IOException {
        if(proxy == null) {
            throw new IllegalArgumentException("proxy can not be null");
        }
        
        // Create a copy of Proxy as a security measure
        Proxy p = (proxy == Proxy.NO_PROXY) ? Proxy.NO_PROXY : ApplicationProxy.create(proxy);
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null && p.type() != Proxy.Type.DIRECT) {
            InetSocketAddress epoint = (InetSocketAddress) p.address();
            
            // 如果是"未解析"的socket地址
            if(epoint.isUnresolved()) {
                sm.checkConnect(epoint.getHostName(), epoint.getPort());
            } else {
                sm.checkConnect(epoint.getAddress().getHostAddress(), epoint.getPort());
            }
        }
        
        // 返回连接
        return handler.openConnection(this, p);
    }
    /*▲ 资源连接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节流 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens a connection to this {@code URL} and returns an
     * {@code InputStream} for reading from that connection. This
     * method is a shorthand for:
     * <blockquote><pre>
     *     openConnection().getInputStream()
     * </pre></blockquote>
     *
     * @return an input stream for reading from the URL connection.
     *
     * @throws IOException if an I/O exception occurs.
     * @see java.net.URL#openConnection()
     * @see java.net.URLConnection#getInputStream()
     */
    // 返回指向URL资源的输入流，可以从中读取数据
    public final InputStream openStream() throws IOException {
        return openConnection().getInputStream();
    }
    
    /*▲ 字节流 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 资源内容 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the contents of this URL. This method is a shorthand for:
     * <blockquote><pre>
     *     openConnection().getContent()
     * </pre></blockquote>
     *
     * @return the contents of this URL.
     *
     * @throws IOException if an I/O exception occurs.
     * @see java.net.URLConnection#getContent()
     */
    // 返回目标资源的内容，返回的形式取决于资源的类型(不一定总是输入流)
    public final Object getContent() throws IOException {
        URLConnection connection = openConnection();
        return connection.getContent();
    }
    
    /**
     * Gets the contents of this URL. This method is a shorthand for:
     * <blockquote><pre>
     *     openConnection().getContent(classes)
     * </pre></blockquote>
     *
     * @param classes an array of Java types
     *
     * @return the content object of this URL that is the first match of
     * the types specified in the classes array.
     * null if none of the requested types are supported.
     *
     * @throws IOException if an I/O exception occurs.
     * @see java.net.URLConnection#getContent(Class[])
     * @since 1.3
     */
    // 返回目标资源的内容，且限定该"内容"只能是指定的类型；内容的返回形式取决于资源的类型(不一定总是输入流)
    public final Object getContent(Class<?>[] classes) throws IOException {
        URLConnection connection = openConnection();
        return connection.getContent(classes);
    }
    
    /*▲ 资源内容 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @serialField protocol String
     *
     * @serialField host String
     *
     * @serialField port int
     *
     * @serialField authority String
     *
     * @serialField file String
     *
     * @serialField    ref String
     *
     * @serialField    hashCode int
     *
     */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("protocol", String.class),
        new ObjectStreamField("host", String.class),
        new ObjectStreamField("port", int.class),
        new ObjectStreamField("authority", String.class),
        new ObjectStreamField("file", String.class),
        new ObjectStreamField("ref", String.class),
        new ObjectStreamField("hashCode", int.class),};
    
    /**
     * WriteObject is called to save the state of the URL to an
     * ObjectOutputStream. The handler is not saved since it is
     * specific to this system.
     *
     * @serialData the default write object value. When read back in,
     * the reader must ensure that calling getURLStreamHandler with
     * the protocol variable returns a valid URLStreamHandler and
     * throw an IOException if it does not.
     */
    private synchronized void writeObject(java.io.ObjectOutputStream s) throws IOException {
        s.defaultWriteObject(); // write the fields
    }
    
    /**
     * readObject is called to restore the state of the URL from the
     * stream.  It reads the components of the URL and finds the local
     * stream handler.
     */
    private synchronized void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        GetField gf = s.readFields();
        String protocol = (String) gf.get("protocol", null);
        if(getURLStreamHandler(protocol) == null) {
            throw new IOException("unknown protocol: " + protocol);
        }
        String host = (String) gf.get("host", null);
        int port = gf.get("port", -1);
        String authority = (String) gf.get("authority", null);
        String file = (String) gf.get("file", null);
        String ref = (String) gf.get("ref", null);
        int hashCode = gf.get("hashCode", -1);
        if(authority == null && ((host != null && host.length()>0) || port != -1)) {
            if(host == null)
                host = "";
            authority = (port == -1) ? host : host + ":" + port;
        }
        tempState = new UrlDeserializedState(protocol, host, port, authority, file, ref, hashCode);
    }
    
    /**
     * Replaces the de-serialized object with an URL object.
     *
     * @return a newly created object from deserialized data
     *
     * @throws ObjectStreamException if a new object replacing this object could not be created
     */
    private Object readResolve() throws ObjectStreamException {
        
        URLStreamHandler handler = null;
        
        // already been checked in readObject
        handler = getURLStreamHandler(tempState.getProtocol());
        
        URL replacementURL = null;
        if(isBuiltinStreamHandler(handler.getClass().getName())) {
            replacementURL = fabricateNewURL();
        } else {
            replacementURL = setDeserializedFields(handler);
        }
        
        return replacementURL;
    }
    
    private URL setDeserializedFields(URLStreamHandler handler) {
        URL replacementURL;
        String userInfo = null;
        String protocol = tempState.getProtocol();
        String host = tempState.getHost();
        int port = tempState.getPort();
        String authority = tempState.getAuthority();
        String file = tempState.getFile();
        String ref = tempState.getRef();
        int hashCode = tempState.getHashCode();
        
        
        // Construct authority part
        if(authority == null && ((host != null && host.length()>0) || port != -1)) {
            if(host == null)
                host = "";
            authority = (port == -1) ? host : host + ":" + port;
            
            // Handle hosts with userInfo in them
            int at = host.lastIndexOf('@');
            if(at != -1) {
                userInfo = host.substring(0, at);
                host = host.substring(at + 1);
            }
        } else if(authority != null) {
            // Construct user info part
            int ind = authority.indexOf('@');
            if(ind != -1)
                userInfo = authority.substring(0, ind);
        }
        
        // Construct path and query part
        String path = null;
        String query = null;
        if(file != null) {
            // Fix: only do this if hierarchical?
            int q = file.lastIndexOf('?');
            if(q != -1) {
                query = file.substring(q + 1);
                path = file.substring(0, q);
            } else
                path = file;
        }
        
        // Set the object fields.
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.file = file;
        this.authority = authority;
        this.ref = ref;
        this.hashCode = hashCode;
        this.handler = handler;
        this.query = query;
        this.path = path;
        this.userInfo = userInfo;
        replacementURL = this;
        return replacementURL;
    }
    
    private URL fabricateNewURL() throws InvalidObjectException {
        // create URL string from deserialized object
        URL replacementURL = null;
        String urlString = tempState.reconstituteUrlString();
        
        try {
            replacementURL = new URL(urlString);
        } catch(MalformedURLException mEx) {
            resetState();
            InvalidObjectException invoEx = new InvalidObjectException("Malformed URL:  " + urlString);
            invoEx.initCause(mEx);
            throw invoEx;
        }
        replacementURL.setSerializedHashCode(tempState.getHashCode());
        resetState();
        return replacementURL;
    }
    
    // 是否为内置的URLStreamHandler(来自"sun.net.www.protocol"包下)
    private boolean isBuiltinStreamHandler(String handlerClassName) {
        return handlerClassName.startsWith(BUILTIN_HANDLERS_PREFIX);
    }
    
    private void resetState() {
        this.protocol = null;
        this.host = null;
        this.port = -1;
        this.file = null;
        this.authority = null;
        this.ref = null;
        this.hashCode = -1;
        this.handler = null;
        this.query = null;
        this.path = null;
        this.userInfo = null;
        this.tempState = null;
    }
    
    private void setSerializedHashCode(int hc) {
        this.hashCode = hc;
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns a {@link java.net.URI} equivalent to this URL.
     * This method functions in the same way as {@code new URI (this.toString())}.
     * <p>Note, any URL instance that complies with RFC 2396 can be converted
     * to a URI. However, some URLs that are not strictly in compliance
     * can not be converted to a URI.
     *
     * @return a URI instance equivalent to this URL.
     *
     * @throws URISyntaxException if this URL is not formatted strictly according to
     *                            to RFC2396 and cannot be converted to a URI.
     * @since 1.5
     */
    // 将URL转换为URI
    public URI toURI() throws URISyntaxException {
        return new URI(toString());
    }
    
    /**
     * Compares two URLs, excluding the fragment component.<p>
     *
     * Returns {@code true} if this {@code URL} and the
     * {@code other} argument are equal without taking the
     * fragment component into consideration.
     *
     * @param other the {@code URL} to compare against.
     *
     * @return {@code true} if they reference the same remote object;
     * {@code false} otherwise.
     */
    // 判断当前URL与other是否相等(排除fragment)
    public boolean sameFile(URL other) {
        return handler.sameFile(this, other);
    }
    
    /**
     * Constructs a string representation of this {@code URL}. The
     * string is created by calling the {@code toExternalForm}
     * method of the stream protocol handler for this object.
     *
     * @return a string representation of this object.
     *
     * @see java.net.URL#URL(java.lang.String, java.lang.String,
     * int, java.lang.String)
     * @see java.net.URLStreamHandler#toExternalForm(java.net.URL)
     */
    public String toExternalForm() {
        return handler.toExternalForm(this);
    }
    
    
    /**
     * Constructs a string representation of this {@code URL}. The
     * string is created by calling the {@code toExternalForm}
     * method of the stream protocol handler for this object.
     *
     * @return a string representation of this object.
     *
     * @see java.net.URL#URL(java.lang.String, java.lang.String, int,
     * java.lang.String)
     * @see java.net.URLStreamHandler#toExternalForm(java.net.URL)
     */
    public String toString() {
        return toExternalForm();
    }
    
    /**
     * Creates an integer suitable for hash table indexing.<p>
     *
     * The hash code is based upon all the URL components relevant for URL
     * comparison. As such, this operation is a blocking operation.
     *
     * @return a hash code for this {@code URL}.
     */
    public synchronized int hashCode() {
        if(hashCode != -1) {
            return hashCode;
        }
        
        hashCode = handler.hashCode(this);
        
        return hashCode;
    }
    
    /**
     * Compares this URL for equality with another object.<p>
     *
     * If the given object is not a URL then this method immediately returns
     * {@code false}.<p>
     *
     * Two URL objects are equal if they have the same protocol, reference
     * equivalent hosts, have the same port number on the host, and the same
     * file and fragment of the file.<p>
     *
     * Two hosts are considered equivalent if both host names can be resolved
     * into the same IP addresses; else if either host name can't be
     * resolved, the host names must be equal without regard to case; or both
     * host names equal to null.<p>
     *
     * Since hosts comparison requires name resolution, this operation is a
     * blocking operation. <p>
     *
     * Note: The defined behavior for {@code equals} is known to
     * be inconsistent with virtual hosting in HTTP.
     *
     * @param obj the URL to compare against.
     *
     * @return {@code true} if the objects are the same;
     * {@code false} otherwise.
     */
    public boolean equals(Object obj) {
        if(!(obj instanceof URL)) {
            return false;
        }
        
        URL u2 = (URL) obj;
        
        return handler.equals(this, u2);
    }
    
    
    /**
     * Creates a URL from a URI, as if by invoking {@code uri.toURL()}.
     *
     * @see java.net.URI#toURL()
     */
    // 将URI转换为URL
    static URL fromURI(URI uri) throws MalformedURLException {
        if(!uri.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        
        String protocol = uri.getScheme();
        
        // In general we need to go via Handler.parseURL, but for the jrt
        // protocol we enforce that the Handler is not overrideable and can
        // optimize URI to URL conversion.
        //
        // Case-sensitive comparison for performance; malformed protocols will
        // be handled correctly by the slow path.
        if(protocol.equals("jrt") && !uri.isOpaque() && uri.getRawFragment() == null) {
            
            String query = uri.getRawQuery();
            String path = uri.getRawPath();
            String file = (query == null) ? path : path + "?" + query;
            
            // URL represent undefined host as empty string while URI use null
            String host = uri.getHost();
            if(host == null) {
                host = "";
            }
            
            int port = uri.getPort();
            
            return new URL("jrt", host, port, file, null);
        } else {
            return new URL((URL) null, uri.toString(), null);
        }
    }
    
    /**
     * Returns the protocol in lower case.
     * Special cases known protocols to avoid loading locale classes during startup.
     */
    // 协议名转换为小写
    static String toLowerCase(String protocol) {
        if(protocol.equals("jrt") || protocol.equals("file") || protocol.equals("jar")) {
            return protocol;
        } else {
            return protocol.toLowerCase(Locale.ROOT);
        }
    }
    
    /**
     * Non-overrideable protocols: "jrt" and "file"
     *
     * Character-based comparison for performance reasons;
     * also ensures case-insensitive comparison in a locale-independent fashion.
     */
    // 是否为允许自定义实现的协议(除"jrt"和"file"之外)
    static boolean isOverrideable(String protocol) {
        if(protocol.length() == 3) {
            return (Character.toLowerCase(protocol.charAt(0)) != 'j') || (Character.toLowerCase(protocol.charAt(1)) != 'r') || (Character.toLowerCase(protocol.charAt(2)) != 't');
        } else if(protocol.length() == 4) {
            return (Character.toLowerCase(protocol.charAt(0)) != 'f') || (Character.toLowerCase(protocol.charAt(1)) != 'i') || (Character.toLowerCase(protocol.charAt(2)) != 'l') || (Character.toLowerCase(protocol.charAt(3)) != 'e');
        }
        
        return true;
    }
    
    /**
     * Returns the Stream Handler.
     *
     * @param protocol the protocol to use
     */
    // 返回指定协议的URLStreamHandler
    static URLStreamHandler getURLStreamHandler(String protocol) {
        
        URLStreamHandler handler = handlers.get(protocol);
        
        // 先从缓存中查找
        if(handler != null) {
            return handler;
        }
        
        URLStreamHandlerFactory fac;
        
        // 是否通过工厂方法创建
        boolean checkedWithFactory = false;
        
        // 如果不是"jrt"或"file"协议，且虚拟机已经完全启动
        if(isOverrideable(protocol) && VM.isBooted()) {
            // Use the factory (if any). Volatile read makes
            // URLStreamHandlerFactory appear fully initialized to current thread.
            
            fac = factory;
            
            // 如果设置了URLStreamHandler工厂，则使用该工厂创建URLStreamHandler
            if(fac != null) {
                handler = fac.createURLStreamHandler(protocol);
                checkedWithFactory = true;
            }
            
            // 如果不是"jar"协议
            if(handler == null && !protocol.equalsIgnoreCase("jar")) {
                // 尝试从URLStreamHandlerProvider服务的提供者中生产protocol类型的URLStreamHandler
                handler = lookupViaProviders(protocol);
            }
            
            if(handler == null) {
                // 尝试加载自定义的URLStreamHandler
                handler = lookupViaProperty(protocol);
            }
        }
        
        synchronized(streamHandlerLock) {
            if(handler == null) {
                // 尝试使用默认的工厂来创建URLStreamHandler
                handler = defaultFactory.createURLStreamHandler(protocol);
            } else {
                URLStreamHandler otherHandler = null;
                
                /* Check again with hashtable just in case another thread created a handler since we last checked */
                // 如果别的线程抢先加载到该协议对应的URLStreamHandler，则直接返回
                otherHandler = handlers.get(protocol);
                if(otherHandler != null) {
                    return otherHandler;
                }
                
                /* Check with factory if another thread set a factory since our last check */
                // 如果之前没试过使用工厂加载，则最后一次尝试
                if(!checkedWithFactory && (fac = factory) != null) {
                    otherHandler = fac.createURLStreamHandler(protocol);
                }
                
                // 优先使用来自工厂加载的Handler
                if(otherHandler != null) {
                    // The handler from the factory must be given more importance.
                    // Discard the default handler that this thread created.
                    handler = otherHandler;
                }
            }
            
            // Insert this handler into the hashtable
            if(handler != null) {
                // 缓存
                handlers.put(protocol, handler);
            }
        }
        
        return handler;
    }
    
    // 尝试加载自定义的URLStreamHandler
    private static URLStreamHandler lookupViaProperty(String protocol) {
        // 获取handler包前缀列表
        String packagePrefixList = GetPropertyAction.privilegedGetProperty(protocolPathProp);
        if(packagePrefixList == null) {
            return null;    // not set
        }
        
        // 获取包前缀列表(用"|"分割)
        String[] packagePrefixes = packagePrefixList.split("\\|");
        
        URLStreamHandler handler = null;
        
        for(String prefix : packagePrefixes) {
            String packagePrefix = prefix.trim();
            
            try {
                // 拼接处handler类名
                String clsName = packagePrefix + "." + protocol + ".Handler";
                Class<?> cls = null;
                try {
                    cls = Class.forName(clsName);
                } catch(ClassNotFoundException e) {
                    ClassLoader cl = ClassLoader.getSystemClassLoader();
                    if(cl != null) {
                        cls = cl.loadClass(clsName);
                    }
                }
                
                if(cls != null) {
                    handler = (URLStreamHandler) cls.newInstance();
                    if(handler != null) {
                        break;
                    }
                }
            } catch(Exception e) {
                // any number of exceptions can get thrown here
            }
        }
        
        return handler;
    }
    
    // 返回URLStreamHandlerProvider服务提供者的迭代器
    private static Iterator<URLStreamHandlerProvider> providers() {
        return new Iterator<>() {
            // 获取system类加载器，可能是内置的AppClassLoader，也可能是自定义的类加载器
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            
            // 使用类加载器classLoader加载URLStreamHandlerProvider服务
            ServiceLoader<URLStreamHandlerProvider> serviceLoader = ServiceLoader.load(URLStreamHandlerProvider.class, classLoader);
            
            // 返回服务提供者迭代器
            Iterator<URLStreamHandlerProvider> iterator = serviceLoader.iterator();
            
            URLStreamHandlerProvider next = null;
            
            public boolean hasNext() {
                return getNext();
            }
            
            public URLStreamHandlerProvider next() {
                if(!getNext()) {
                    throw new NoSuchElementException();
                }
                
                URLStreamHandlerProvider n = next;
                next = null;
                
                return n;
            }
            
            private boolean getNext() {
                while(next == null) {
                    try {
                        if(!iterator.hasNext()) {
                            return false;
                        }
                        next = iterator.next();
                    } catch(ServiceConfigurationError sce) {
                        if(sce.getCause() instanceof SecurityException) {
                            // Ignore security exceptions
                            continue;
                        }
                        throw sce;
                    }
                }
                
                return true;
            }
        };
    }
    
    // 尝试从URLStreamHandlerProvider服务的提供者中获取protocol类型的URLStreamHandler
    private static URLStreamHandler lookupViaProviders(final String protocol) {
        if(gate.get() != null) {
            throw new Error("Circular loading of URL stream handler providers detected");
        }
        
        gate.set(gate);
        
        try {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                public URLStreamHandler run() {
                    // 获取URLStreamHandlerProvider服务提供者的迭代器
                    Iterator<URLStreamHandlerProvider> iterator = providers();
                    while(iterator.hasNext()) {
                        URLStreamHandlerProvider provider = iterator.next();
                        URLStreamHandler handler = provider.createURLStreamHandler(protocol);
                        if(handler != null) {
                            return handler;
                        }
                    }
                    return null;
                }
            });
        } finally {
            gate.set(null);
        }
    }
    
    /**
     * Returns true if specified string is a valid protocol name.
     */
    // 判断protocol是否可以作为协议名称
    private boolean isValidProtocol(String protocol) {
        int len = protocol.length();
        if(len<1) {
            return false;
        }
        
        char c = protocol.charAt(0);
        if(!Character.isLetter(c)) {
            return false;
        }
        
        for(int i = 1; i<len; i++) {
            c = protocol.charAt(i);
            if(!Character.isLetterOrDigit(c) && c != '.' && c != '+' && c != '-') {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks for permission to specify a stream handler.
     */
    private void checkSpecifyHandler(SecurityManager sm) {
        sm.checkPermission(SecurityConstants.SPECIFY_HANDLER_PERMISSION);
    }
    
    /**
     * Sets the fields of the URL. This is not a public method so that
     * only URLStreamHandlers can modify URL fields. URLs are
     * otherwise constant.
     *
     * @param protocol the name of the protocol to use
     * @param host     the name of the host
     * @param port     the port number on the host
     * @param file     the file on the host
     * @param ref      the internal reference in the URL
     */
    void set(String protocol, String host, int port, String file, String ref) {
        synchronized(this) {
            this.protocol = protocol;
            this.host = host;
            authority = port == -1 ? host : host + ":" + port;
            this.port = port;
            this.file = file;
            this.ref = ref;
            /* This is very important. We must recompute this after the
             * URL has been changed. */
            hashCode = -1;
            hostAddress = null;
            int q = file.lastIndexOf('?');
            if(q != -1) {
                query = file.substring(q + 1);
                path = file.substring(0, q);
            } else
                path = file;
        }
    }
    
    /**
     * Sets the specified 8 fields of the URL. This is not a public method so
     * that only URLStreamHandlers can modify URL fields. URLs are otherwise
     * constant.
     *
     * @param protocol  the name of the protocol to use
     * @param host      the name of the host
     * @param port      the port number on the host
     * @param authority the authority part for the url
     * @param userInfo  the username and password
     * @param path      the file on the host
     * @param ref       the internal reference in the URL
     * @param query     the query part of this URL
     *
     * @since 1.3
     */
    void set(String protocol, String host, int port, String authority, String userInfo, String path, String query, String ref) {
        synchronized(this) {
            this.protocol = protocol;
            this.host = host;
            this.port = port;
            this.file = query == null ? path : path + "?" + query;
            this.userInfo = userInfo;
            this.path = path;
            this.ref = ref;
            /* This is very important. We must recompute this after the URL has been changed. */
            hashCode = -1;
            hostAddress = null;
            this.query = query;
            this.authority = authority;
        }
    }
    
    
    // 默认的URLStreamHandler工厂
    private static class DefaultFactory implements URLStreamHandlerFactory {
        // 返回指定协议对应的流协议处理器
        public URLStreamHandler createURLStreamHandler(String protocol) {
            String name = BUILTIN_HANDLERS_PREFIX + "." + protocol + ".Handler";
            
            try {
                return (URLStreamHandler) Class.forName(name).newInstance();
            } catch(ClassNotFoundException x) {
                // ignore
            } catch(Exception e) {
                // For compatibility, all Exceptions are ignored.
                // any number of exceptions can get thrown here.
            }
            
            return null;
        }
    }
    
}
