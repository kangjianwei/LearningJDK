/*
 * Copyright (c) 1995, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import sun.net.ConnectionResetException;

/**
 * This stream extends FileInputStream to implement a
 * SocketInputStream. Note that this class should <b>NOT</b> be
 * public.
 *
 * @author Jonathan Payne
 * @author Arthur van Hoff
 */
// Socket输入流
class SocketInputStream extends FileInputStream {
    
    private AbstractPlainSocketImpl impl = null;    // 该输入流所属的Socket实现类
    private Socket socket = null;                   // 该输入流所属的Socket
    
    private boolean eof;    // 输入流是否关闭
    
    private byte[] temp;
    
    /**
     * Closes the stream.
     */
    private boolean closing = false;
    
    
    static {
        init();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new SocketInputStream. Can only be called
     * by a Socket. This method needs to hang on to the owner Socket so
     * that the fd will not be closed.
     *
     * @param impl the implemented socket input stream
     */
    SocketInputStream(AbstractPlainSocketImpl impl) throws IOException {
        super(impl.getFileDescriptor());
        this.impl = impl;
        socket = impl.getSocket();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 打开/关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭socket流
    public void close() throws IOException {
        // Prevent recursion. See BugId 4484411
        if(closing) {
            return;
        }
        
        closing = true;
        if(socket != null) {
            if(!socket.isClosed()) {
                socket.close();
            }
        } else {
            impl.close();
        }
        
        closing = false;
    }
    
    
    /**
     * Overrides finalize, the fd is closed by the Socket.
     */
    @SuppressWarnings({"deprecation", "removal"})
    protected void finalize() {
    }
    
    /*▲ 打开/关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads a single byte from the socket.
     */
    /*
     * 尝试从当前输入流读取一个字节，成功读到的内容直接返回
     * 返回-1表示已经没有可读内容了
     */
    public int read() throws IOException {
        if(eof) {
            return -1;
        }
        
        temp = new byte[1];
        
        int n = read(temp, 0, 1);
        if(n<=0) {
            return -1;
        }
        
        return temp[0] & 0xff;
    }
    
    /**
     * Reads into a byte array data from the socket.
     *
     * @param b the buffer into which the data is read
     *
     * @return the actual number of bytes read, -1 is
     * returned when the end of the stream is reached.
     *
     * @throws IOException If an I/O error has occurred.
     */
    /*
     * 尝试从当前输入流读取b.length个字节，成功读到的内容存入字节数组
     * 返回值表示成功读取的字节数量，返回-1表示已经没有可读内容了
     */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    
    /**
     * Reads into a byte array <i>b</i> at offset <i>off</i>,
     * <i>length</i> bytes of data.
     *
     * @param b      the buffer into which the data is read
     * @param off    the start offset of the data
     * @param length the maximum number of bytes read
     *
     * @return the actual number of bytes read, -1 is
     * returned when the end of the stream is reached.
     *
     * @throws IOException If an I/O error has occurred.
     */
    /*
     * 尝试从当前输入流读取len个字节，读到的内容存入字节数组dst的off位置处
     * 返回值表示成功读取的字节数量，返回-1表示已经没有可读内容了
     */
    public int read(byte[] dst, int off, int len) throws IOException {
        return read(dst, off, len, impl.getTimeout());
    }
    
    /*
     * 尝试从当前输入流读取len个字节，读到的内容存入字节数组dst的off位置处
     * 返回值表示成功读取的字节数量，返回-1表示已经没有可读内容了
     *
     * 注意：这里有超时限制
     */
    int read(byte[] dst, int off, int len, int timeout) throws IOException {
        int n;
        
        // EOF already encountered
        if(eof) {
            return -1;
        }
        
        // connection reset
        if(impl.isConnectionReset()) {
            throw new SocketException("Connection reset");
        }
        
        // bounds check
        if(len<=0 || off<0 || len>dst.length - off) {
            if(len == 0) {
                return 0;
            }
            throw new ArrayIndexOutOfBoundsException("length == " + len + " off == " + off + " buffer length == " + dst.length);
        }
        
        /* acquire file descriptor and do the read */
        // 获取Socket文件的文件描述符，并将其引用次数增一
        FileDescriptor fd = impl.acquireFD();
        
        try {
            n = socketRead(fd, dst, off, len, timeout);
            if(n>0) {
                return n;
            }
        } catch(ConnectionResetException rstExc) {
            // 设置连接已重置
            impl.setConnectionReset();
        } finally {
            // Socket文件引用次数减一
            impl.releaseFD();
        }
        
        /*
         * If we get here we are at EOF, the socket has been closed,
         * or the connection has been reset.
         */
        if(impl.isClosedOrPending()) {
            throw new SocketException("Socket closed");
        }
        
        if(impl.isConnectionReset()) {
            throw new SocketException("Connection reset");
        }
        
        eof = true;
        
        return -1;
    }
    
    /**
     * Reads into an array of bytes at the specified offset using
     * the received socket primitive.
     *
     * @param fd      the FileDescriptor
     * @param b       the buffer into which the data is read
     * @param off     the start offset of the data
     * @param len     the maximum number of bytes read
     * @param timeout the read timeout in ms
     *
     * @return the actual number of bytes read, -1 is
     * returned when the end of the stream is reached.
     *
     * @throws IOException If an I/O error has occurred.
     */
    /*
     * 尝试从fd代表的输入流读取len个字节，成功读到的内容存入字节数组的[off, off+len-1]范围
     * 返回值表示成功读取的字节数量，返回-1表示已经没有可读内容了
     *
     * 注意：这里有超时限制
     */
    private int socketRead(FileDescriptor fd, byte[] dst, int off, int len, int timeout) throws IOException {
        return socketRead0(fd, dst, off, len, timeout);
    }
    
    
    /**
     * Returns the number of bytes that can be read without blocking.
     *
     * @return the number of immediately available bytes
     */
    // 返回剩余可不被阻塞地读取（或跳过）的字节数（估计值）
    public int available() throws IOException {
        return impl.available();
    }
    
    /**
     * Skips n bytes of input.
     *
     * @param numbytes the number of bytes to skip
     *
     * @return the actual number of bytes skipped.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 向前/向后跳过n个字节
    public long skip(long numbytes) throws IOException {
        if(numbytes<=0) {
            return 0;
        }
        
        long n = numbytes;
        int buflen = (int) Math.min(1024, n);
        byte[] data = new byte[buflen];
        while(n>0) {
            int r = read(data, 0, (int) Math.min((long) buflen, n));
            if(r<0) {
                break;
            }
            n -= r;
        }
        
        return numbytes - n;
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the unique {@link java.nio.channels.FileChannel FileChannel}
     * object associated with this file input stream.</p>
     *
     * The {@code getChannel} method of {@code SocketInputStream}
     * returns {@code null} since it is a socket based stream.</p>
     *
     * @return the file channel associated with this file input stream
     *
     * @spec JSR-51
     * @since 1.4
     */
    // Socket流没有文件通道，返回null
    public final FileChannel getChannel() {
        return null;
    }
    
    
    // 标记输入流已关闭
    void setEOF(boolean eof) {
        this.eof = eof;
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Perform class load-time initializations.
     */
    private static native void init();
    
    /**
     * Reads into an array of bytes at the specified offset using
     * the received socket primitive.
     *
     * @param fd      the FileDescriptor
     * @param dst     the buffer into which the data is read
     * @param off     the start offset of the data
     * @param len     the maximum number of bytes read
     * @param timeout the read timeout in ms
     *
     * @return the actual number of bytes read, -1 is
     * returned when the end of the stream is reached.
     *
     * @throws IOException If an I/O error has occurred.
     */
    private native int socketRead0(FileDescriptor fd, byte[] dst, int off, int len, int timeout) throws IOException;
}
