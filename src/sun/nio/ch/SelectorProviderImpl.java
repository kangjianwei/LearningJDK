/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

// 选择器工厂的抽象实现，实现了SelectorProvider的大部分特性（除了生成Selector没有实现）
public abstract class SelectorProviderImpl extends SelectorProvider {
    
    // 生产Selector，由各平台自行实现
    public abstract AbstractSelector openSelector() throws IOException;
    
    // 构造一个未绑定的[客户端Socket]，内部初始化了该Socket的文件描述符
    public SocketChannel openSocketChannel() throws IOException {
        return new SocketChannelImpl(this);
    }
    
    // 构造一个未绑定的ServerSocket，本质是创建了[服务端Socket(监听)]，内部初始化了该Socket的文件描述符
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return new ServerSocketChannelImpl(this);
    }
    
    // 构造DatagramSocket通道，内部初始化了该Socket的文件描述符
    public DatagramChannel openDatagramChannel() throws IOException {
        return new DatagramChannelImpl(this);
    }
    
    // 构造DatagramSocket通道，内部初始化了该Socket的文件描述符，其支持的协议族由参数family指定
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        return new DatagramChannelImpl(this, family);
    }
    
    // 生产Pipe
    public Pipe openPipe() throws IOException {
        return new PipeImpl(this);
    }
    
}
