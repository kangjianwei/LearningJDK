/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.io.IOException;
import java.nio.channels.MembershipKey;
import java.nio.channels.MulticastChannel;
import java.util.HashSet;

/**
 * MembershipKey implementation.
 */
// 组播小组成员的实现
class MembershipKeyImpl extends MembershipKey {
    
    private final MulticastChannel channel; // 当前成员所属的通道
    private final InetAddress group;   // 组播小组地址
    private final NetworkInterface interf;  // 接收消息的网络接口(网卡)
    private final InetAddress source;  // 过滤消息的地址
    
    private volatile boolean invalid;       // 指示当前成员是否有效
    
    /** set of source addresses that are blocked */
    // 记录屏蔽的消息源
    private HashSet<InetAddress> blockedSet;
    
    /** lock used when creating or accessing blockedSet */
    private final Object stateLock = new Object();
    
    private MembershipKeyImpl(MulticastChannel channel, InetAddress group, NetworkInterface interf, InetAddress source) {
        this.channel = channel;
        this.group = group;
        this.interf = interf;
        this.source = source;
    }
    
    // 将当前组播小组成员设置为无效
    void invalidate() {
        invalid = true;
    }
    
    // 判断当前组播小组成员是否已经无效
    public boolean isValid() {
        return !invalid;
    }
    
    // 从通道的组播注册表中移除当前组播小组成员；实际操作是将目标组播Socket从所在的组播小组中移除
    public void drop() {
        // delegate to channel
        ((DatagramChannelImpl) channel).drop(this);
    }
    
    // 屏蔽source处发来的消息，即禁止从source处接收消息；如果该组播小组已经设置了过滤，则抛异常
    @Override
    public MembershipKey block(InetAddress source) throws IOException {
        // 组播小组已经限定了只从source处接收消息，那么当前屏蔽方法无法执行下去
        if(source != null) {
            throw new IllegalStateException("key is source-specific");
        }
        
        synchronized(stateLock) {
            if((blockedSet != null) && blockedSet.contains(source)) {
                // already blocked, nothing to do
                return this;
            }
            
            ((DatagramChannelImpl) channel).block(this, source);
            
            // created blocked set if required and add source address
            if(blockedSet == null) {
                blockedSet = new HashSet<>();
            }
            
            blockedSet.add(source);
        }
        
        return this;
    }
    
    // 解除对source地址的屏蔽，即允许接收source处的消息
    @Override
    public MembershipKey unblock(InetAddress source) {
        synchronized(stateLock) {
            if((blockedSet == null) || !blockedSet.contains(source)) {
                throw new IllegalStateException("not blocked");
            }
            
            ((DatagramChannelImpl) channel).unblock(this, source);
            
            blockedSet.remove(source);
        }
        
        return this;
    }
    
    // 返回当前成员所属的通道
    @Override
    public MulticastChannel channel() {
        return channel;
    }
    
    // 返回组播小组地址
    @Override
    public InetAddress group() {
        return group;
    }
    
    // 返回接收消息的网络接口(网卡)
    @Override
    public NetworkInterface networkInterface() {
        return interf;
    }
    
    // 返回过滤消息的地址
    @Override
    public InetAddress sourceAddress() {
        return source;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('<');
        sb.append(group.getHostAddress());
        sb.append(',');
        sb.append(interf.getName());
        if(source != null) {
            sb.append(',');
            sb.append(source.getHostAddress());
        }
        sb.append('>');
        return sb.toString();
    }
    
    
    /**
     * MembershipKey will additional context for IPv4 membership
     */
    // IP4类型的组播小组成员
    static class Type4 extends MembershipKeyImpl {
        private final int groupAddress;  // 组播小组地址
        private final int interfAddress; // 接收消息的网络接口的地址
        private final int sourceAddress; // 过滤消息的地址
        
        Type4(MulticastChannel ch, InetAddress group, NetworkInterface interf, InetAddress source, int groupAddress, int interfAddress, int sourceAddress) {
            super(ch, group, interf, source);
            this.groupAddress = groupAddress;
            this.interfAddress = interfAddress;
            this.sourceAddress = sourceAddress;
        }
        
        int groupAddress() {
            return groupAddress;
        }
        
        int interfaceAddress() {
            return interfAddress;
        }
        
        int source() {
            return sourceAddress;
        }
    }
    
    /**
     * MembershipKey will additional context for IPv6 membership
     */
    // IP6类型的组播小组成员
    static class Type6 extends MembershipKeyImpl {
        private final byte[] groupAddress;  // 组播小组地址
        private final int index;         // 接收消息的网络接口的索引
        private final byte[] sourceAddress; // 过滤消息的地址
        
        Type6(MulticastChannel ch, InetAddress group, NetworkInterface interf, InetAddress source, byte[] groupAddress, int index, byte[] sourceAddress) {
            super(ch, group, interf, source);
            this.groupAddress = groupAddress;
            this.index = index;
            this.sourceAddress = sourceAddress;
        }
        
        byte[] groupAddress() {
            return groupAddress;
        }
        
        int index() {
            return index;
        }
        
        byte[] source() {
            return sourceAddress;
        }
    }
    
}
