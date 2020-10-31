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

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * Pipe.SinkChannel implementation based on socket connection.
 */
// 管道中的写通道的实现类，向这里写入数据。读写通道往往共享一个Buffer来直接通信
class SinkChannelImpl extends Pipe.SinkChannel implements SelChImpl {
    
    /** The SocketChannel assoicated with this pipe */
    // 实际使用的通道
    final SocketChannel socketChannel;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    SinkChannelImpl(SelectorProvider provider, SocketChannel socketChannel) {
        super(provider);
        this.socketChannel = socketChannel;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 实现对"可选择"通道的关闭操作
    protected void implCloseSelectableChannel() throws IOException {
        // 如果当前通道上已经没有已注册的SelectionKey，则销毁通道
        if(!isRegistered()) {
            kill();
        }
    }
    
    // 销毁当前通道，即释放对Socket文件描述符的引用
    public void kill() throws IOException {
        socketChannel.close();
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向当前通道中写入src包含的内容
    public int write(ByteBuffer src) throws IOException {
        try {
            return socketChannel.write(src);
        } catch(AsynchronousCloseException x) {
            close();
            throw x;
        }
    }
    
    // 向当前通道中写入srcs中各个缓冲区包含的内容
    public long write(ByteBuffer[] srcs) throws IOException {
        try {
            return socketChannel.write(srcs);
        } catch(AsynchronousCloseException x) {
            close();
            throw x;
        }
    }
    
    // 向当前通道中写入srcs[offset, offset+length-1]中各个缓冲区包含的内容
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if((offset<0) || (length<0) || (offset>srcs.length - length)) {
            throw new IndexOutOfBoundsException();
        }
        
        try {
            return write(Util.subsequence(srcs, offset, length));
        } catch(AsynchronousCloseException x) {
            close();
            throw x;
        }
    }
    
    /*▲ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 监听参数/事件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 翻译Sink通道监听的事件，返回对ops的翻译结果
     *
     * 方向：Java层 --> native层
     * 　　　SelectionKey.XXX --> Net.XXX
     */
    public int translateInterestOps(int ops) {
        int newOps = 0;
        
        if((ops & SelectionKey.OP_WRITE) != 0) {
            newOps |= Net.POLLOUT;
        }
        
        return newOps;
    }
    
    /*▲ 监听参数/事件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 就绪参数/事件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     *【增量更新】已就绪事件
     *
     * 将本地(native)反馈的就绪信号ops翻译并存储到Java层的就绪事件readyOps中，
     * 返回值指示上一次反馈的事件与本次反馈的事件是否发生了改变。
     *
     * 通道收到有效的反馈事件后，会【增量更新】上次记录的已就绪事件，
     * 如果本地(native)反馈了错误或挂起信号，则将已就绪事件直接设置为通道注册的监听事件。
     *
     * 方向：native层 --> Java层
     * 　　　Net.XXX --> SelectionKey.XXX
     *
     * 参见：SelectionKeyImpl#readyOps
     */
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl selectionKey) {
        return translateReadyOps(ops, selectionKey.nioReadyOps(), selectionKey);
    }
    
    /*
     *【覆盖更新】已就绪事件
     *
     * 将本地(native)反馈的就绪信号ops翻译并存储到Java层的就绪事件readyOps中，
     * 返回值指示上一次反馈的事件与本次反馈的事件是否发生了改变。
     *
     * 通道收到有效的反馈事件后，会【覆盖】(selectionKey中)上次记录的已就绪事件，
     * 如果本地(native)反馈了错误或挂起信号，则将已就绪事件直接设置为通道注册的监听事件。
     *
     * 方向：native层 --> Java层
     * 　　　Net.XXX --> SelectionKey.XXX
     *
     * 参见：SelectionKeyImpl#readyOps
     */
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl selectionKey) {
        return translateReadyOps(ops, 0, selectionKey);
    }
    
    /*
     *【增量更新】已就绪事件(基于initialOps叠加)
     *
     * 将本地(native)反馈的就绪信号ops翻译并存储到Java层的就绪事件readyOps中，
     * 返回值指示上一次反馈的事件与本次反馈的事件是否发生了改变。
     *
     * 通道收到有效的反馈事件后，会将其【叠加】在initialOps上，换句话说是对已就绪事件的增量更新。
     * 如果本地(native)反馈了错误或挂起信号，则将已就绪事件直接设置为通道注册的监听事件
     *
     * 方向：native层 --> Java层
     * 　　　Net.XXX --> SelectionKey.XXX
     *
     * 参见：SelectionKeyImpl#readyOps
     */
    public boolean translateReadyOps(int ops, int initialOps, SelectionKeyImpl selectionKey) {
        // 不合规的文件描述符
        if((ops & Net.POLLNVAL) != 0) {
            throw new Error("POLLNVAL detected");
        }
        
        // 获取注册的监听事件：SelectionKey.XXX(不会验证当前"选择键"是否有效)
        int intOps = selectionKey.nioInterestOps();
        
        // 获取已就绪事件
        int oldOps = selectionKey.nioReadyOps();
        int newOps = initialOps;
        
        // 本地(native)反馈了错误或挂起的信号
        if((ops & (Net.POLLERR | Net.POLLHUP)) != 0) {
            newOps = intOps;
            // 直接将通道注册的监听事件设置为已就绪事件
            selectionKey.nioReadyOps(newOps);
            return (newOps & ~oldOps) != 0;
        }
        
        // 该通道监听了"可写"事件，且可以向通道写入数据
        if(((ops & Net.POLLOUT) != 0) && ((intOps & SelectionKey.OP_WRITE) != 0)) {
            newOps |= SelectionKey.OP_WRITE;
        }
        
        // 将newOps设置为已就绪事件
        selectionKey.nioReadyOps(newOps);
        
        return (newOps & ~oldOps) != 0;
    }
    
    /*▲ 就绪参数/事件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 是否设置当前通道为阻塞模式
    protected void implConfigureBlocking(boolean block) throws IOException {
        socketChannel.configureBlocking(block);
    }
    
    // 返回通道在Java层的文件描述符
    public FileDescriptor getFD() {
        return ((SocketChannelImpl) socketChannel).getFD();
    }
    
    // 返回通道在本地(native层)的文件描述符
    public int getFDVal() {
        return ((SocketChannelImpl) socketChannel).getFDVal();
    }
    
}
