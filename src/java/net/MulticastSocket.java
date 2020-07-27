/*
 * Copyright (c) 1995, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

/**
 * The multicast datagram socket class is useful for sending
 * and receiving IP multicast packets.  A MulticastSocket is
 * a (UDP) DatagramSocket, with additional capabilities for
 * joining "groups" of other multicast hosts on the internet.
 * <P>
 * A multicast group is specified by a class D IP address
 * and by a standard UDP port number. Class D IP addresses
 * are in the range <CODE>224.0.0.0</CODE> to <CODE>239.255.255.255</CODE>,
 * inclusive. The address 224.0.0.0 is reserved and should not be used.
 * <P>
 * One would join a multicast group by first creating a MulticastSocket
 * with the desired port, then invoking the
 * <CODE>joinGroup(InetAddress groupAddr)</CODE>
 * method:
 * <PRE>
 * // join a Multicast group and send the group salutations
 * ...
 * String msg = "Hello";
 * InetAddress group = InetAddress.getByName("228.5.6.7");
 * MulticastSocket s = new MulticastSocket(6789);
 * s.joinGroup(group);
 * DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(),
 * group, 6789);
 * s.send(hi);
 * // get their responses!
 * byte[] buf = new byte[1000];
 * DatagramPacket recv = new DatagramPacket(buf, buf.length);
 * s.receive(recv);
 * ...
 * // OK, I'm done talking - leave the group...
 * s.leaveGroup(group);
 * </PRE>
 *
 * When one sends a message to a multicast group, <B>all</B> subscribing
 * recipients to that host and port receive the message (within the
 * time-to-live range of the packet, see below).  The socket needn't
 * be a member of the multicast group to send messages to it.
 * <P>
 * When a socket subscribes to a multicast group/port, it receives
 * datagrams sent by other hosts to the group/port, as do all other
 * members of the group and port.  A socket relinquishes membership
 * in a group by the leaveGroup(InetAddress addr) method.  <B>
 * Multiple MulticastSocket's</B> may subscribe to a multicast group
 * and port concurrently, and they will all receive group datagrams.
 * <P>
 * Currently applets are not allowed to use multicast sockets.
 *
 * @author Pavani Diwanji
 * @since 1.1
 */
/*
 * 组播Socket，也是UDP-Socket的一种。
 *
 * 组播可以定向地向一组目标地址发送数据包，在"定向"上类似于单播，而在"一组"上类似于广播。
 */
public class MulticastSocket extends DatagramSocket {
    
    /**
     * Used on some platforms to record if an outgoing interface has been set for this socket.
     */
    // 是否为组播Socket显式指定了网络接口
    private boolean interfaceSet;
    
    /**
     * The "last" interface set by setInterface on this MulticastSocket
     */
    // 记录为组播Socket显式指定的网络接口的IP
    private InetAddress infAddress = null;
    
    // 缓存组播Socket支持的所有参数
    private static Set<SocketOption<?>> options;
    
    // 是否已经对options做了缓存
    private static boolean optionsSet = false;
    
    /**
     * The lock on the socket's TTL. This is for set/getTTL and
     * send(packet,ttl).
     */
    private Object ttlLock = new Object();
    
    /**
     * The lock on the socket's interface - used by setInterface
     * and getInterface
     */
    private Object infLock = new Object();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Create a MulticastSocket bound to the specified socket address.
     * <p>
     * Or, if the address is {@code null}, create an unbound socket.
     *
     * <p>If there is a security manager,
     * its {@code checkListen} method is first called
     * with the SocketAddress port as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     * <p>
     * When the socket is created the
     * {@link DatagramSocket#setReuseAddress(boolean)} method is
     * called to enable the SO_REUSEADDR socket option.
     *
     * @param bindaddr Socket address to bind to, or {@code null} for
     *                 an unbound socket.
     *
     * @throws IOException       if an I/O exception occurs
     *                           while creating the MulticastSocket
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkListen} method doesn't allow the operation.
     * @see SecurityManager#checkListen
     * @see java.net.DatagramSocket#setReuseAddress(boolean)
     * @since 1.4
     */
    // ▶ 1 构造组播Socket，并将其绑定到指定的Socket地址
    public MulticastSocket(SocketAddress bindaddr) throws IOException {
        super((SocketAddress) null);
        
        // 设置允许立刻重用已关闭的socket端口
        setReuseAddress(true);
        
        if(bindaddr == null) {
            return;
        }
        
        try {
            // 对组播Socket执行【bind】操作
            bind(bindaddr);
        } finally {
            if(!isBound()) {
                close();
            }
        }
    }
    
    /**
     * Create a multicast socket.
     *
     * <p>
     * If there is a security manager, its {@code checkListen} method is first
     * called with 0 as its argument to ensure the operation is allowed. This
     * could result in a SecurityException.
     * <p>
     * When the socket is created the
     * {@link DatagramSocket#setReuseAddress(boolean)} method is called to
     * enable the SO_REUSEADDR socket option.
     *
     * @throws IOException       if an I/O exception occurs while creating the
     *                           MulticastSocket
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkListen} method doesn't allow the operation.
     * @see SecurityManager#checkListen
     * @see java.net.DatagramSocket#setReuseAddress(boolean)
     * @see java.net.DatagramSocketImpl#setOption(SocketOption, Object)
     */
    // ▶ 1-1 构造组播Socket，并将其绑定到通配IP与随机端口
    public MulticastSocket() throws IOException {
        this(new InetSocketAddress(0));
    }
    
    /**
     * Create a multicast socket and bind it to a specific port.
     *
     * <p>If there is a security manager,
     * its {@code checkListen} method is first called
     * with the {@code port} argument
     * as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     * <p>
     * When the socket is created the
     * {@link DatagramSocket#setReuseAddress(boolean)} method is
     * called to enable the SO_REUSEADDR socket option.
     *
     * @param port port to use
     *
     * @throws IOException       if an I/O exception occurs
     *                           while creating the MulticastSocket
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkListen} method doesn't allow the operation.
     * @see SecurityManager#checkListen
     * @see java.net.DatagramSocket#setReuseAddress(boolean)
     */
    // ▶ 1-2 构造组播Socket，并将其绑定到通配IP和指定的端口
    public MulticastSocket(int port) throws IOException {
        this(new InetSocketAddress(port));
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Joins a multicast group. Its behavior may be affected by
     * {@code setInterface} or {@code setNetworkInterface}.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkMulticast} method
     * with the {@code mcastaddr} argument
     * as its argument.
     *
     * @param group is the multicast address to join
     *
     * @throws IOException       if there is an error joining,
     *                           or when the address is not a multicast address,
     *                           or the platform does not support multicasting
     * @throws SecurityException if a security manager exists and its {@code checkMulticast} method doesn't allow the join.
     * @see SecurityManager#checkMulticast(InetAddress)
     */
    // 将当前组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据
    public void joinGroup(InetAddress group) throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        checkAddress(group, "joinGroup");
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkMulticast(group);
        }
        
        // 确保为组播地址
        if(!group.isMulticastAddress()) {
            throw new SocketException("Not a multicast address");
        }
        
        /*
         * required for some platforms where it's not possible
         * to join a group without setting the interface first.
         */
        // 获取当前系统的默认网络接口
        NetworkInterface defaultInterface = NetworkInterface.getDefault();
        
        if(!interfaceSet && defaultInterface != null) {
            setNetworkInterface(defaultInterface);
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.join(group);
    }
    
    /**
     * Leave a multicast group. Its behavior may be affected by
     * {@code setInterface} or {@code setNetworkInterface}.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkMulticast} method
     * with the {@code mcastaddr} argument
     * as its argument.
     *
     * @param mcastaddr is the multicast address to leave
     *
     * @throws IOException       if there is an error leaving
     *                           or when the address is not a multicast address.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkMulticast} method doesn't allow the operation.
     * @see SecurityManager#checkMulticast(InetAddress)
     */
    // 将当前组播Socket从group处的组播小组中移除，后续它将无法接收到该小组中的数据
    public void leaveGroup(InetAddress group) throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        checkAddress(group, "leaveGroup");
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkMulticast(group);
        }
        
        // 确保为组播地址
        if(!group.isMulticastAddress()) {
            throw new SocketException("Not a multicast address");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.leave(group);
    }
    
    /**
     * Joins the specified multicast group at the specified interface.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkMulticast} method
     * with the {@code mcastaddr} argument
     * as its argument.
     *
     * @param group  is the multicast address to join
     * @param interf specifies the local interface to receive multicast datagram packets,
     *               or <i>null</i> to defer to the interface set by
     *               {@link MulticastSocket#setInterface(InetAddress)} or
     *               {@link MulticastSocket#setNetworkInterface(NetworkInterface)}
     *
     * @throws IOException              if there is an error joining,
     *                                  or when the address is not a multicast address,
     *                                  or the platform does not support multicasting
     * @throws SecurityException        if a security manager exists and its {@code checkMulticast} method doesn't allow the join.
     * @throws IllegalArgumentException if mcastaddr is null or is a SocketAddress subclass not supported by this socket
     * @see SecurityManager#checkMulticast(InetAddress)
     * @since 1.4
     */
    // 将当前组播Socket加入到group处的组播小组中，后续它将接收到该小组中的数据；interf是用来接收消息的网络接口(网卡)
    public void joinGroup(SocketAddress group, NetworkInterface interf) throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        if(!(group instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        
        if(oldImpl) {
            throw new UnsupportedOperationException();
        }
        
        checkAddress(((InetSocketAddress) group).getAddress(), "joinGroup");
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkMulticast(((InetSocketAddress) group).getAddress());
        }
        
        // 确保为组播地址
        if(!((InetSocketAddress) group).getAddress().isMulticastAddress()) {
            throw new SocketException("Not a multicast address");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.joinGroup(group, interf);
    }
    
    /**
     * Leave a multicast group on a specified local interface.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkMulticast} method
     * with the {@code mcastaddr} argument
     * as its argument.
     *
     * @param group  is the multicast address to leave
     * @param interf specifies the local interface or <i>null</i> to defer to the interface set by
     *               {@link MulticastSocket#setInterface(InetAddress)} or
     *               {@link MulticastSocket#setNetworkInterface(NetworkInterface)}
     *
     * @throws IOException              if there is an error leaving
     *                                  or when the address is not a multicast address.
     * @throws SecurityException        if a security manager exists and its
     *                                  {@code checkMulticast} method doesn't allow the operation.
     * @throws IllegalArgumentException if mcastaddr is null or is a
     *                                  SocketAddress subclass not supported by this socket
     * @see SecurityManager#checkMulticast(InetAddress)
     * @since 1.4
     */
    // 将当前组播Socket从group处的组播小组中移除，后续它将无法接收到该小组中的数据；interf是用来接收消息的网络接口(网卡)
    public void leaveGroup(SocketAddress group, NetworkInterface interf) throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        if(!(group instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        
        if(oldImpl) {
            throw new UnsupportedOperationException();
        }
        
        checkAddress(((InetSocketAddress) group).getAddress(), "leaveGroup");
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkMulticast(((InetSocketAddress) group).getAddress());
        }
        
        // 确保为组播地址
        if(!((InetSocketAddress) group).getAddress().isMulticastAddress()) {
            throw new SocketException("Not a multicast address");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.leaveGroup(group, interf);
    }
    
    /*▲ Socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数据传输 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sends a datagram packet to the destination, with a TTL (time-
     * to-live) other than the default for the socket.  This method
     * need only be used in instances where a particular TTL is desired;
     * otherwise it is preferable to set a TTL once on the socket, and
     * use that default TTL for all packets.  This method does <B>not
     * </B> alter the default TTL for the socket. Its behavior may be
     * affected by {@code setInterface}.
     *
     * <p>If there is a security manager, this method first performs some
     * security checks. First, if {@code p.getAddress().isMulticastAddress()}
     * is true, this method calls the
     * security manager's {@code checkMulticast} method
     * with {@code p.getAddress()} and {@code ttl} as its arguments.
     * If the evaluation of that expression is false,
     * this method instead calls the security manager's
     * {@code checkConnect} method with arguments
     * {@code p.getAddress().getHostAddress()} and
     * {@code p.getPort()}. Each call to a security manager method
     * could result in a SecurityException if the operation is not allowed.
     *
     * @param p   is the packet to be sent. The packet should contain
     *            the destination multicast ip address and the data to be sent.
     *            One does not need to be the member of the group to send
     *            packets to a destination multicast address.
     * @param ttl optional time to live for multicast packet.
     *            default ttl is 1.
     *
     * @throws IOException       is raised if an error occurs i.e
     *                           error while setting ttl.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkMulticast} or {@code checkConnect}
     *                           method doesn't allow the send.
     * @see DatagramSocket#send
     * @see DatagramSocket#receive
     * @see SecurityManager#checkMulticast(java.net.InetAddress, byte)
     * @see SecurityManager#checkConnect
     * @deprecated Use the following code or its equivalent instead:
     * ......
     * int ttl = mcastSocket.getTimeToLive();
     * mcastSocket.setTimeToLive(newttl);
     * mcastSocket.send(p);
     * mcastSocket.setTimeToLive(ttl);
     * ......
     */
    /*
     * 发送UDP数据包，允许指定网络跳数。
     * 该方法已过时，参见普通的send方法。
     */
    @Deprecated
    public void send(DatagramPacket packet, byte ttl) throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        checkAddress(packet.getAddress(), "send");
        
        synchronized(ttlLock) {
            synchronized(packet) {
                if(connectState == ST_NOT_CONNECTED) {
                    /*
                     * Security manager makes sure that the multicast address
                     * is allowed one and that the ttl used is less than the allowed maxttl.
                     */
                    SecurityManager security = System.getSecurityManager();
                    if(security != null) {
                        if(packet.getAddress().isMulticastAddress()) {
                            security.checkMulticast(packet.getAddress(), ttl);
                        } else {
                            security.checkConnect(packet.getAddress().getHostAddress(), packet.getPort());
                        }
                    }
                } else {
                    // we're connected
                    InetAddress packetAddress = null;
                    packetAddress = packet.getAddress();
                    if(packetAddress == null) {
                        packet.setAddress(connectedAddress);
                        packet.setPort(connectedPort);
                    } else if((!packetAddress.equals(connectedAddress)) || packet.getPort() != connectedPort) {
                        throw new SecurityException("connected address and packet address" + " differ");
                    }
                }
                
                byte dttl = getTTL();
                
                // 获取"UDP-Socket委托"
                DatagramSocketImpl impl = getImpl();
                
                try {
                    if(ttl != dttl) {
                        // set the ttl
                        impl.setTTL(ttl);
                    }
                    
                    // call the datagram method to send
                    impl.send(packet);
                } finally {
                    // set it back to default
                    if(ttl != dttl) {
                        impl.setTTL(dttl);
                    }
                }
            } // synch p
        }  //synch ttl
    } //method
    
    /*▲ 数据传输 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 网络接口 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 注：组播Socket使用了A接口后，那么就无法接收发自B接口的消息了。
     */
    
    /**
     * Retrieve the address of the network interface used for
     * multicast packets.
     *
     * @return An {@code InetAddress} representing
     * the address of the network interface used for
     * multicast packets.
     *
     * @throws SocketException if there is an error in
     *                         the underlying protocol, such as a TCP error.
     * @see #setInterface(java.net.InetAddress)
     */
    // 获取组播Socket当前使用的网络接口的IP
    public InetAddress getInterface() throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        synchronized(infLock) {
            // 获取组播Socket使用的网络接口的IP
            InetAddress ia = (InetAddress) impl.getOption(SocketOptions.IP_MULTICAST_IF);
            
            /* No previous setInterface or interface can be set using setNetworkInterface */
            // 如果之前未显式设置网络接口，那么此处直接返回该默认的网络接口的IP
            if(infAddress == null) {
                return ia;
            }
            
            /* Same interface set with setInterface? */
            // 如果ia与之前预设的网络接口的IP一致，也可以直接返回
            if(ia.equals(infAddress)) {
                return ia;
            }
            
            /*
             * Different InetAddress from what we set with setInterface
             * so enumerate the current interface to see if the
             * address set by setInterface is bound to this interface.
             */
            // 如果ia与之前预设的网络接口的IP不一致，则需要搜索与ia绑定的接口下的所有IP，看其与
            try {
                // 搜索绑定到指定IP的网络接口
                NetworkInterface ni = NetworkInterface.getByInetAddress(ia);
                
                // 遍历该网络接口下所有IP地址（包括IP4和IP6地址）
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while(addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    // 如果ia所在的网络接口上存在之前预设的IP，则返回之前预设的IP
                    if(addr.equals(infAddress)) {
                        return infAddress;
                    }
                }
                
                // No match so reset infAddress to indicate that the interface has changed via means
                infAddress = null;
                return ia;
            } catch(Exception e) {
                return ia;
            }
        }
    }
    
    /**
     * Set the multicast network interface used by methods
     * whose behavior would be affected by the value of the
     * network interface. Useful for multihomed hosts.
     *
     * @param inf the InetAddress
     *
     * @throws SocketException if there is an error in
     *                         the underlying protocol, such as a TCP error.
     * @see #getInterface()
     */
    // 设置组播Socket使用指定IP处的网络接口
    public void setInterface(InetAddress inf) throws SocketException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        checkAddress(inf, "setInterface");
        
        synchronized(infLock) {
            // 获取"UDP-Socket委托"
            DatagramSocketImpl impl = getImpl();
            
            impl.setOption(SocketOptions.IP_MULTICAST_IF, inf);
            
            infAddress = inf;
            
            interfaceSet = true;
        }
    }
    
    /**
     * Get the multicast network interface set.
     *
     * @return the multicast {@code NetworkInterface} currently set
     *
     * @throws SocketException if there is an error in
     *                         the underlying protocol, such as a TCP error.
     * @see #setNetworkInterface(NetworkInterface)
     * @since 1.4
     */
    // 获取组播Socket使用的网络接口
    public NetworkInterface getNetworkInterface() throws SocketException {
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        NetworkInterface ni = (NetworkInterface) impl.getOption(SocketOptions.IP_MULTICAST_IF2);
        
        if((ni.getIndex() == 0) || (ni.getIndex() == -1)) {
            InetAddress[] addrs = new InetAddress[1];
            addrs[0] = InetAddress.anyLocalAddress();
            
            return new NetworkInterface(addrs[0].getHostName(), 0, addrs);
        }
        
        return ni;
    }
    
    /**
     * Specify the network interface for outgoing multicast datagrams
     * sent on this socket.
     *
     * @param netIf the interface
     *
     * @throws SocketException if there is an error in
     *                         the underlying protocol, such as a TCP error.
     * @see #getNetworkInterface()
     * @since 1.4
     */
    // 设置组播Socket使用的网络接口
    public void setNetworkInterface(NetworkInterface netIf) throws SocketException {
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        synchronized(infLock) {
            impl.setOption(SocketOptions.IP_MULTICAST_IF2, netIf);
            infAddress = null;
            interfaceSet = true;
        }
    }
    
    /*▲ 网络接口 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 网络跳数阈值：
     *   0 - 同一主机
     *   1 - 同一子网
     *  32 - 同一站点
     *  64 - 同一地区
     * 128 - 同一大洲
     * 255 - 无限制
     */
    
    /**
     * Get the default time-to-live for multicast packets sent out on
     * the socket.
     *
     * @return the default time-to-live value
     *
     * @throws IOException if an I/O exception occurs while
     *                     getting the default time-to-live value
     * @see #setTimeToLive(int)
     */
    // 获取网络跳数
    public int getTimeToLive() throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        return impl.getTimeToLive();
    }
    
    /**
     * Set the default time-to-live for multicast packets sent out
     * on this {@code MulticastSocket} in order to control the
     * scope of the multicasts.
     *
     * <P> The ttl <B>must</B> be in the range {@code  0 <= ttl <=
     * 255} or an {@code IllegalArgumentException} will be thrown.
     * Multicast packets sent with a TTL of {@code 0} are not transmitted
     * on the network but may be delivered locally.
     *
     * @param ttl the time-to-live
     *
     * @throws IOException if an I/O exception occurs while setting the
     *                     default time-to-live value
     * @see #getTimeToLive()
     */
    // 设置网络跳数
    public void setTimeToLive(int ttl) throws IOException {
        if(ttl<0 || ttl>255) {
            throw new IllegalArgumentException("ttl out of range");
        }
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.setTimeToLive(ttl);
    }
    
    /**
     * Get the default time-to-live for multicast packets sent out on
     * the socket.
     *
     * @return the default time-to-live value
     *
     * @throws IOException if an I/O exception occurs
     *                     while getting the default time-to-live value
     * @see #setTTL(byte)
     * @deprecated use the getTimeToLive method instead, which returns
     * an <b>int</b> instead of a <b>byte</b>.
     */
    // 获取网络跳数，已过时，参见getTimeToLive()
    @Deprecated
    public byte getTTL() throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        return impl.getTTL();
    }
    
    /**
     * Set the default time-to-live for multicast packets sent out
     * on this {@code MulticastSocket} in order to control the
     * scope of the multicasts.
     *
     * <p>The ttl is an <b>unsigned</b> 8-bit quantity, and so <B>must</B> be
     * in the range {@code 0 <= ttl <= 0xFF }.
     *
     * @param ttl the time-to-live
     *
     * @throws IOException if an I/O exception occurs
     *                     while setting the default time-to-live value
     * @see #getTTL()
     * @deprecated use the setTimeToLive method instead, which uses
     * <b>int</b> instead of <b>byte</b> as the type for ttl.
     */
    // 设置网络跳数，已过时，参见setTimeToLive(int)
    @Deprecated
    public void setTTL(byte ttl) throws IOException {
        if(isClosed()) {
            throw new SocketException("Socket is closed");
        }
        
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.setTTL(ttl);
    }
    
    /**
     * Get the setting for local loopback of multicast datagrams.
     *
     * @return true if the LoopbackMode has been disabled
     *
     * @throws SocketException if an error occurs while getting the value
     * @see #setLoopbackMode
     * @since 1.4
     */
    // 判断是否禁用组播回环
    public boolean getLoopbackMode() throws SocketException {
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        return (Boolean) impl.getOption(SocketOptions.IP_MULTICAST_LOOP);
    }
    
    /**
     * Disable/Enable local loopback of multicast datagrams
     * The option is used by the platform's networking code as a hint
     * for setting whether multicast data will be looped back to
     * the local socket.
     *
     * <p>Because this option is a hint, applications that want to
     * verify what loopback mode is set to should call
     * {@link #getLoopbackMode()}
     *
     * @param disable {@code true} to disable the LoopbackMode
     *
     * @throws SocketException if an error occurs while setting the value
     * @see #getLoopbackMode
     * @since 1.4
     */
    // 设置是否禁用组播回环
    public void setLoopbackMode(boolean disable) throws SocketException {
        // 获取"UDP-Socket委托"
        DatagramSocketImpl impl = getImpl();
        
        impl.setOption(SocketOptions.IP_MULTICAST_LOOP, Boolean.valueOf(disable));
    }
    
    
    // 获取组播Socket支持的所有参数
    @Override
    public Set<SocketOption<?>> supportedOptions() {
        synchronized(MulticastSocket.class) {
            if(optionsSet) {
                return options;
            }
            
            try {
                // 获取"UDP-Socket委托"
                DatagramSocketImpl impl = getImpl();
                
                options = Collections.unmodifiableSet(impl.supportedOptions());
            } catch(SocketException ex) {
                options = Collections.emptySet();
            }
            
            optionsSet = true;
            
            return options;
        }
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
