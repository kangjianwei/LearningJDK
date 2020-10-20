/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 */

package sun.nio.ch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureRandom;
import java.util.Random;

/**
 * A simple Pipe implementation based on a socket connection.
 */
/*
 * 单向同步管道的实现类，由写通道SinkChannel和读通道SourceChannel组成，可以从写通道写入数据，从读通道读取数据。
 * 读写管道实际上是通过Socket进行通信的。
 */
class PipeImpl extends Pipe {
    
    // Number of bytes in the secret handshake.
    private static final int NUM_SECRET_BYTES = 16;
    
    // Random object for handshake values
    private static final Random RANDOM_NUMBER_GENERATOR = new SecureRandom();
    
    // 读通道，从这里读取数据
    private SourceChannel source;
    
    // 写通道，向这里写入数据
    private SinkChannel sink;
    
    // 构造管道
    PipeImpl(final SelectorProvider provider) throws IOException {
        try {
            // 这里会调用initializer的run方法，为管道建立读通道和写通道，并连通它们
            AccessController.doPrivileged(new Initializer(provider));
        } catch(PrivilegedActionException x) {
            throw (IOException) x.getCause();
        }
    }
    
    // 返回管道中的写通道，可以向这里写入数据
    public SinkChannel sink() {
        return sink;
    }
    
    // 返回管道中的读通道，可以从这里读取数据
    public SourceChannel source() {
        return source;
    }
    
    // 管道初始化器，用于为管道建立读通道和写通道，并连通它们
    private class Initializer implements PrivilegedExceptionAction<Void> {
        // 构造了当前管道的选择器工厂
        private final SelectorProvider provider;
        
        // 收集初始化管道中发生的异常
        private IOException ioe = null;
        
        private Initializer(SelectorProvider provider) {
            this.provider = provider;
        }
        
        // 初始化管道
        @Override
        public Void run() throws IOException {
            
            // 构造本地环回连接器
            LoopbackConnector connector = new LoopbackConnector();
            
            // 在管道内构造读通道和写通道，打通管道内部的通信
            connector.run();
            
            if(ioe instanceof ClosedByInterruptException) {
                ioe = null;
                
                Thread connThread = new Thread(connector) {
                    @Override
                    public void interrupt() {
                    }
                };
                
                connThread.start();
                
                for(; ; ) {
                    try {
                        connThread.join();
                        break;
                    } catch(InterruptedException ex) {
                    }
                }
                
                Thread.currentThread().interrupt();
            }
            
            if(ioe != null) {
                throw new IOException("Unable to establish loopback connection", ioe);
            }
            
            return null;
        }
        
        // 本地环回连接器，完成读写通道的连通操作
        private class LoopbackConnector implements Runnable {
            
            // 在管道内构造读通道和写通道，打通管道内部的通信
            @Override
            public void run() {
                ServerSocketChannel serverSocketChannel = null; // [服务端Socket(监听)]
                SocketChannel socketClient = null;  // [客户端Socket]
                SocketChannel socketServer = null;  // [服务端Socket(通信)]
                
                try {
                    // 创建堆内存缓冲区HeapByteBuffer
                    ByteBuffer secret = ByteBuffer.allocate(NUM_SECRET_BYTES);
                    ByteBuffer buffer = ByteBuffer.allocate(NUM_SECRET_BYTES);
                    
                    // 获取本地回环地址
                    InetAddress ip = InetAddress.getLoopbackAddress();
                    assert (ip.isLoopbackAddress());
                    
                    // 服务端绑定的地址
                    InetSocketAddress serverAddr = null;
                    
                    for(; ; ) {
                        // Bind ServerSocketChannel to a port on the loopback address
                        if(serverSocketChannel == null || !serverSocketChannel.isOpen()) {
                            // 创建一个未绑定地址和端口的ServerSocket，并返回其关联的Socket通道
                            serverSocketChannel = ServerSocketChannel.open();
                            
                            // 返回由当前ServerSocket通道适配而成的ServerSocket
                            ServerSocket serverSocket = serverSocketChannel.socket();
                            
                            // 待绑定地址：环回IP+随机端口
                            InetSocketAddress socketAddress = new InetSocketAddress(ip, 0);
                            
                            // 创建[服务端Socket(监听)]，并对其执行【bind】和【listen】操作，此处允许积压(排队)的待处理连接数为50
                            serverSocket.bind(socketAddress);
                            
                            // 获取本地端口，即服务端的端口
                            int localPort = serverSocket.getLocalPort();
                            
                            // 获取到服务端的绑定地址，此时端口号已被解析出来了
                            serverAddr = new InetSocketAddress(ip, localPort);
                        }
                        
                        /* Establish connection (assume connections are eagerly accepted) */
                        // 构造一个[客户端Socket]，并将其连接到远端
                        socketClient = SocketChannel.open(serverAddr);
                        
                        // 使用随机字节填充secret数组
                        RANDOM_NUMBER_GENERATOR.nextBytes(secret.array());
                        do {
                            // 客户端通道开始写入数据，待写入数据存储在secret中
                            socketClient.write(secret);
                        } while(secret.hasRemaining());
                        
                        // 重置secret的游标与标记
                        secret.rewind();
                        
                        /* Get a connection and verify it is legitimate */
                        // [服务端Socket(监听)]等待客户端的连接请求；连接成功后，返回与[客户端Socket]建立连接的[服务端Socket(通信)]
                        socketServer = serverSocketChannel.accept();
                        
                        do {
                            // 服务端通道开始读取数据，读到的内容存入bb
                            socketServer.read(buffer);
                        } while(buffer.hasRemaining());
                        
                        // 重置buffer的游标与标记
                        buffer.rewind();
                        
                        // 如果两端数据一致，说明两端是可以互相通信的
                        if(buffer.equals(secret)) {
                            break;
                        }
                        
                        socketServer.close();
                        socketClient.close();
                    } // for(;;)
                    
                    // 创建读通道
                    source = new SourceChannelImpl(provider, socketClient);
                    
                    // 创建写通道
                    sink = new SinkChannelImpl(provider, socketServer);
                    
                    /* 至此，管道已经打通，内部可以通过读管道和写管道进行通信 */
                    
                } catch(IOException e) {
                    try {
                        if(socketClient != null) {
                            socketClient.close();
                        }
                        
                        if(socketServer != null) {
                            socketServer.close();
                        }
                    } catch(IOException e2) {
                    }
                    
                    ioe = e;
                } finally {
                    try {
                        if(serverSocketChannel != null) {
                            serverSocketChannel.close();
                        }
                    } catch(IOException e2) {
                    }
                }
            }
        }
    }
    
}
