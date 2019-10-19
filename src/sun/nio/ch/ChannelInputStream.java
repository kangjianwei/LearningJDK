/*
 * Copyright (c) 2001, 2002, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectableChannel;

/**
 * This class is defined here rather than in java.nio.channels.Channels
 * so that code can be shared with SocketAdaptor.
 *
 * @author Mike McCloskey
 * @author Mark Reinhold
 * @since 1.4
 */
// 可读通道的输入流，允许从指定的通道中读取数据
public class ChannelInputStream extends InputStream {
    
    // 当前输入流关联的可读通道（不允许非阻塞Socket通道）
    protected final ReadableByteChannel src;
    
    private ByteBuffer bb = null;       // 包装了bs的缓冲区
    private byte[] bs = null;           // 上次批量读取数据时使用的存储容器
    
    private byte[] b1 = null;           // 上次读取一个字节的数据时使用的存储容器
    
    // 构造器，为当前输入流关联可读通道
    public ChannelInputStream(ReadableByteChannel src) {
        this.src = src;
    }
    
    // 从当前输入流关联的可读通道（不允许是非阻塞Socket通道）读取1个字节的数据后返回，返回-1表示没有读到有效数据
    public synchronized int read() throws IOException {
        if(b1 == null) {
            b1 = new byte[1];
        }
        
        int n = read(b1);
        
        // 成功读到了数据
        if(n == 1) {
            return b1[0] & 0xff;
        }
        
        return -1;
    }
    
    // 从当前输入流关联的可读通道（不允许是非阻塞Socket通道）读取len个字节的数据，读到的内容写入dst的off处
    public synchronized int read(byte[] dst, int off, int len) throws IOException {
        if((off<0) || (off>dst.length) || (len<0) || ((off + len)>dst.length) || ((off + len)<0)) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return 0;
        }
        
        ByteBuffer bb = ((this.bs == dst) ? this.bb : ByteBuffer.wrap(dst));
        bb.limit(Math.min(off + len, bb.capacity()));
        bb.position(off);
        this.bb = bb;
        this.bs = dst;
        
        return read(bb);
    }
    
    // 从当前输入流关联的可读通道（不允许是非阻塞Socket通道）读取数据，读到的内容写入dst
    protected int read(ByteBuffer dst) throws IOException {
        return ChannelInputStream.read(this.src, dst, true);
    }
    
    /**
     * 从src通道（不允许是非阻塞Socket通道）读取数据，读到的内容写入dst
     * 对于Socket通道，参数block=false时发挥作用
     */
    public static int read(ReadableByteChannel src, ByteBuffer dst, boolean block) throws IOException {
        if(src instanceof SelectableChannel) {
            SelectableChannel sc = (SelectableChannel) src;
            
            synchronized(sc.blockingLock()) {
                boolean bm = sc.isBlocking();
                if(!bm) {
                    // 如果源通道是非阻塞Socket通道，则抛出异常
                    throw new IllegalBlockingModeException();
                }
                
                // 临时更新通道的阻塞模式
                if(bm != block) {
                    sc.configureBlocking(block);
                }
                
                // 虽然不允许src为非阻塞通道，但是这里支持使用非阻塞式读取，取决于block参数
                int n = src.read(dst);
                
                // 恢复通道之前的阻塞模式
                if(bm != block) {
                    sc.configureBlocking(bm);
                }
                
                return n;
            }
        } else {
            return src.read(dst);
        }
    }
    
    // 返回剩余可不被阻塞地读取（或跳过）的字节数（估计值）
    public int available() throws IOException {
        /* special case where the channel is to a file */
        // 对于文件通道，可以获取到剩余数据量
        if(this.src instanceof SeekableByteChannel) {
            SeekableByteChannel sbc = (SeekableByteChannel) this.src;
            long rem = Math.max(0, sbc.size() - sbc.position());
            return (rem>Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) rem;
        }
        
        // 对于Socket通道，无法估计剩余数据量
        return 0;
    }
    
    // 关闭当前输入流（关联的可读通道）
    public void close() throws IOException {
        this.src.close();
    }
    
}
