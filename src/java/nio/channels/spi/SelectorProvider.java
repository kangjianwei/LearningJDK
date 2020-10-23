/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels.spi;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import sun.nio.ch.DefaultSelectorProvider;

/**
 * Service-provider class for selectors and selectable channels.
 *
 * <p> A selector provider is a concrete subclass of this class that has a
 * zero-argument constructor and implements the abstract methods specified
 * below.  A given invocation of the Java virtual machine maintains a single
 * system-wide default provider instance, which is returned by the {@link
 * #provider() provider} method.  The first invocation of that method will locate
 * the default provider as specified below.
 *
 * <p> The system-wide default provider is used by the static {@code open}
 * methods of the {@link java.nio.channels.DatagramChannel#open
 * DatagramChannel}, {@link java.nio.channels.Pipe#open Pipe}, {@link
 * java.nio.channels.Selector#open Selector}, {@link
 * java.nio.channels.ServerSocketChannel#open ServerSocketChannel}, and {@link
 * java.nio.channels.SocketChannel#open SocketChannel} classes.  It is also
 * used by the {@link java.lang.System#inheritedChannel System.inheritedChannel()}
 * method. A program may make use of a provider other than the default provider
 * by instantiating that provider and then directly invoking the {@code open}
 * methods defined in this class.
 *
 * <p> All of the methods in this class are safe for use by multiple concurrent
 * threads.  </p>
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */
// 选择器/通道/管道工厂，除了可以生产选择器工厂本身，还可以生产通道选择器、TCP/UDP-Socket通道、管道
public abstract class SelectorProvider {
    
    private static final Object lock = new Object();
    
    // 单例下的选择器工厂
    private static SelectorProvider provider = null;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class.
     *
     * @throws SecurityException If a security manager has been installed and it denies
     *                           {@link RuntimePermission}{@code ("selectorProvider")}
     */
    protected SelectorProvider() {
        this(checkPermission());
    }
    
    private SelectorProvider(Void ignore) {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the system-wide default selector provider for this invocation of the Java virtual machine.
     *
     * <p> The first invocation of this method locates the default provider object as follows: </p>
     *
     * <ol>
     *
     * <li><p> If the system property
     * {@code java.nio.channels.spi.SelectorProvider} is defined then it is
     * taken to be the fully-qualified name of a concrete provider class.
     * The class is loaded and instantiated; if this process fails then an
     * unspecified error is thrown.  </p></li>
     *
     * <li><p> If a provider class has been installed in a jar file that is
     * visible to the system class loader, and that jar file contains a
     * provider-configuration file named
     * {@code java.nio.channels.spi.SelectorProvider} in the resource
     * directory {@code META-INF/services}, then the first class name
     * specified in that file is taken.  The class is loaded and
     * instantiated; if this process fails then an unspecified error is
     * thrown.  </p></li>
     *
     * <li><p> Finally, if no provider has been specified by any of the above
     * means then the system-default provider class is instantiated and the
     * result is returned.  </p></li>
     *
     * </ol>
     *
     * <p> Subsequent invocations of this method return the provider that was
     * returned by the first invocation.  </p>
     *
     * @return The system-wide default selector provider
     */
    // 构造并返回默认的选择器工厂(接受用户的定义)
    public static SelectorProvider provider() {
        synchronized(lock) {
            if(provider != null) {
                return provider;
            }
        
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                public SelectorProvider run() {
                    // 1. 从系统属性"java.nio.channels.spi.SelectorProvider"中获取待加载的SelectorProvider的名称，并初始化其实例
                    if(loadProviderFromProperty()) {
                        return provider;
                    }
                
                    // 2. 通过ServiceLoader加载预先配置的SelectorProvider
                    if(loadProviderAsService()) {
                        return provider;
                    }
                
                    // 3. 加载系统默认的SelectorProvider
                    provider = DefaultSelectorProvider.create();
                
                    return provider;
                }
            });
        }
    }
    
    
    /**
     * Opens a selector.
     *
     * @return The new selector
     *
     * @throws IOException If an I/O error occurs
     */
    // 生产Selector，由各平台自行实现
    public abstract AbstractSelector openSelector() throws IOException;
    
    /**
     * Opens a socket channel.
     *
     * @return The new channel
     *
     * @throws IOException If an I/O error occurs
     */
    // 构造一个未绑定的[客户端Socket]，内部初始化了该Socket的文件描述符
    public abstract SocketChannel openSocketChannel() throws IOException;
    
    /**
     * Opens a server-socket channel.
     *
     * @return The new channel
     *
     * @throws IOException If an I/O error occurs
     */
    // 构造一个未绑定的ServerSocket，本质是创建了[服务端Socket(监听)]，内部初始化了该Socket的文件描述符
    public abstract ServerSocketChannel openServerSocketChannel() throws IOException;
    
    /**
     * Opens a datagram channel.
     *
     * @return The new channel
     *
     * @throws IOException If an I/O error occurs
     */
    // 构造DatagramSocket通道，内部初始化了该Socket的文件描述符
    public abstract DatagramChannel openDatagramChannel() throws IOException;
    
    /**
     * Opens a datagram channel.
     *
     * @param family The protocol family
     *
     * @return A new datagram channel
     *
     * @throws UnsupportedOperationException If the specified protocol family is not supported
     * @throws IOException                   If an I/O error occurs
     * @since 1.7
     */
    // 构造DatagramSocket通道，内部初始化了该Socket的文件描述符，其支持的协议族由参数family指定
    public abstract DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException;
    
    /**
     * Opens a pipe.
     *
     * @return The new pipe
     *
     * @throws IOException If an I/O error occurs
     */
    // 生产Pipe
    public abstract Pipe openPipe() throws IOException;
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the channel inherited from the entity that created this
     * Java virtual machine.
     *
     * <p> On many operating systems a process, such as a Java virtual
     * machine, can be started in a manner that allows the process to
     * inherit a channel from the entity that created the process. The
     * manner in which this is done is system dependent, as are the
     * possible entities to which the channel may be connected. For example,
     * on UNIX systems, the Internet services daemon (<i>inetd</i>) is used to
     * start programs to service requests when a request arrives on an
     * associated network port. In this example, the process that is started,
     * inherits a channel representing a network socket.
     *
     * <p> In cases where the inherited channel represents a network socket
     * then the {@link java.nio.channels.Channel Channel} type returned
     * by this method is determined as follows:
     *
     * <ul>
     *
     * <li><p> If the inherited channel represents a stream-oriented connected
     * socket then a {@link java.nio.channels.SocketChannel SocketChannel} is
     * returned. The socket channel is, at least initially, in blocking
     * mode, bound to a socket address, and connected to a peer.
     * </p></li>
     *
     * <li><p> If the inherited channel represents a stream-oriented listening
     * socket then a {@link java.nio.channels.ServerSocketChannel
     * ServerSocketChannel} is returned. The server-socket channel is, at
     * least initially, in blocking mode, and bound to a socket address.
     * </p></li>
     *
     * <li><p> If the inherited channel is a datagram-oriented socket
     * then a {@link java.nio.channels.DatagramChannel DatagramChannel} is
     * returned. The datagram channel is, at least initially, in blocking
     * mode, and bound to a socket address.
     * </p></li>
     *
     * </ul>
     *
     * <p> In addition to the network-oriented channels described, this method
     * may return other kinds of channels in the future.
     *
     * <p> The first invocation of this method creates the channel that is
     * returned. Subsequent invocations of this method return the same
     * channel. </p>
     *
     * @return The inherited channel, if any, otherwise {@code null}.
     *
     * @throws IOException       If an I/O error occurs
     * @throws SecurityException If a security manager has been installed and it denies
     *                           {@link RuntimePermission}{@code ("inheritedChannel")}
     * @since 1.5
     */
    public Channel inheritedChannel() throws IOException {
        return null;
    }
    
    private static Void checkPermission() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("selectorProvider"));
        }
        return null;
    }
    
    // 1. 从系统属性"java.nio.channels.spi.SelectorProvider"中获取待加载的SelectorProvider的名称，并初始化其实例
    private static boolean loadProviderFromProperty() {
        String cn = System.getProperty("java.nio.channels.spi.SelectorProvider");
        
        if(cn == null) {
            return false;
        }
        
        try {
            @SuppressWarnings("deprecation")
            Object tmp = Class.forName(cn, true, ClassLoader.getSystemClassLoader()).newInstance();
            provider = (SelectorProvider) tmp;
            return true;
        } catch(ClassNotFoundException | IllegalAccessException | InstantiationException | SecurityException x) {
            throw new ServiceConfigurationError(null, x);
        }
    }
    
    // 2. 通过ServiceLoader加载预先配置的SelectorProvider服务
    private static boolean loadProviderAsService() {
        // 加载指定的服务，使用指定的类加载器
        ServiceLoader<SelectorProvider> sl = ServiceLoader.load(SelectorProvider.class, ClassLoader.getSystemClassLoader());
        
        Iterator<SelectorProvider> iterator = sl.iterator();
        
        for(; ; ) {
            try {
                if(!iterator.hasNext()) {
                    return false;
                }
                provider = iterator.next();
                return true;
            } catch(ServiceConfigurationError sce) {
                if(sce.getCause() instanceof SecurityException) {
                    // Ignore the security exception, try the next provider
                    continue;
                }
                throw sce;
            }
        }
    }
    
}
