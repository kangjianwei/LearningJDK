/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

/**
 * Piped character-input streams.
 *
 * @author Mark Reinhold
 * @since 1.1
 */
// (字符)读管道，需要与写管道配合使用
public class PipedReader extends Reader {
    
    /**
     * The size of the pipe's circular input buffer.
     */
    private static final int DEFAULT_PIPE_SIZE = 1024;
    
    /**
     * The circular buffer into which incoming data is placed.
     */
    char[] buffer;  // 被循环使用的缓冲区，从写管道接收的数据放到此处
    
    /**
     * The index of the position in the circular buffer at which the
     * next character of data will be stored when received from the connected
     * piped writer. <code>in&lt;0</code> implies the buffer is empty,
     * <code>in==out</code> implies the buffer is full
     */
    int in = -1;    // 写游标，递增以示缓冲区数据增加
    
    /**
     * The index of the position in the circular buffer at which the next
     * character of data will be read by this piped reader.
     */
    int out = 0;    // 读游标，递增以示缓冲区数据减少
    
    boolean closedByWriter = false;
    boolean closedByReader = false;
    
    boolean connected = false;  // 读写管道是否已建立连接
    
    /*
     * REMIND: identification of the read and write sides needs to be more sophisticated.
     * Either using thread groups (but what about pipes within a thread?)
     * or using finalization (but it may be a long time until the next GC).
     */
    Thread readSide;
    Thread writeSide;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a <code>PipedReader</code> so
     * that it is not yet {@linkplain #connect(java.io.PipedWriter)
     * connected}. It must be {@linkplain java.io.PipedWriter#connect(
     *java.io.PipedReader) connected} to a <code>PipedWriter</code>
     * before being used.
     */
    // 初始化缓冲区容量为DEFAULT_PIPE_SIZE的读管道，未与写管道建立连接
    public PipedReader() {
        initPipe(DEFAULT_PIPE_SIZE);
    }
    
    /**
     * Creates a <code>PipedReader</code> so that it is not yet
     * {@link #connect(java.io.PipedWriter) connected} and uses
     * the specified pipe size for the pipe's buffer.
     * It must be  {@linkplain java.io.PipedWriter#connect(
     *java.io.PipedReader) connected} to a <code>PipedWriter</code>
     * before being used.
     *
     * @param pipeSize the size of the pipe's buffer.
     *
     * @throws IllegalArgumentException if {@code pipeSize <= 0}.
     * @since 1.6
     */
    // 初始化缓冲区容量为pipeSize的读管道，未与写管道建立连接
    public PipedReader(int pipeSize) {
        initPipe(pipeSize);
    }
    
    /**
     * Creates a <code>PipedReader</code> so
     * that it is connected to the piped writer
     * <code>src</code>. Data written to <code>src</code>
     * will then be available as input from this stream.
     *
     * @param src the stream to connect to.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 初始化缓冲区容量为DEFAULT_PIPE_SIZE的读管道，并与指定的写管道建立连接
    public PipedReader(PipedWriter src) throws IOException {
        this(src, DEFAULT_PIPE_SIZE);
    }
    
    /**
     * Creates a <code>PipedReader</code> so that it is connected
     * to the piped writer <code>src</code> and uses the specified
     * pipe size for the pipe's buffer. Data written to <code>src</code>
     * will then be  available as input from this stream.
     *
     * @param src      the stream to connect to.
     * @param pipeSize the size of the pipe's buffer.
     *
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if {@code pipeSize <= 0}.
     * @since 1.6
     */
    // 初始化缓冲区容量为pipeSize的读管道，并与指定的写管道建立连接
    public PipedReader(PipedWriter src, int pipeSize) throws IOException {
        initPipe(pipeSize);
        connect(src);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads the next character of data from this piped stream.
     * If no character is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned.
     * This method blocks until input data is available, the end of
     * the stream is detected, or an exception is thrown.
     *
     * @return the next character of data, or <code>-1</code> if the end of the
     * stream is reached.
     *
     * @throws IOException if the pipe is
     *                     <a href=PipedInputStream.html#BROKEN> <code>broken</code></a>,
     *                     {@link #connect(java.io.PipedWriter) unconnected}, closed,
     *                     or an I/O error occurs.
     */
    // (从缓冲区)读取一个字符并返回
    public synchronized int read() throws IOException {
        if(!connected) {
            throw new IOException("Pipe not connected");
        } else if(closedByReader) {
            throw new IOException("Pipe closed");
        } else if(writeSide != null && !writeSide.isAlive() && !closedByWriter && (in<0)) {
            throw new IOException("Write end dead");
        }
        
        // 读线程
        readSide = Thread.currentThread();
        
        int trials = 2;
        
        // 如果缓冲区处于初始状态
        while(in<0) {
            if(closedByWriter) {
                /* closed by writer, return EOF */
                return -1;
            }
            
            if((writeSide != null) && (!writeSide.isAlive()) && (--trials<0)) {
                throw new IOException("Pipe broken");
            }
            
            /* might be a writer waiting */
            notifyAll();    // 主要用于唤醒写线程
            
            try {
                wait(1000); // 阻塞读线程，并释放读管道的锁(以便写线程获取该锁来存入数据)
            } catch(InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
        
        // 读取一个字符
        int ret = buffer[out++];
        
        // 如果读游标越界，则循环到0
        if(out >= buffer.length) {
            out = 0;
        }
        
        /*
         * 缓冲区为空时，需要重置为初始状态
         *
         * 缓冲区是循环使用的，如果不重置in的状态，那么当out==in时，就无法区分缓冲区的满与空了
         */
        if(in == out) {
            /* now empty */
            in = -1;
        }
        
        return ret;
    }
    
    /**
     * Reads up to <code>len</code> characters of data from this piped
     * stream into an array of characters. Less than <code>len</code> characters
     * will be read if the end of the data stream is reached or if
     * <code>len</code> exceeds the pipe's buffer size. This method
     * blocks until at least one character of input is available.
     *
     * @param cbuf the buffer into which the data is read.
     * @param off  the start offset of the data.
     * @param len  the maximum number of characters read.
     *
     * @return the total number of characters read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the stream has been reached.
     *
     * @throws IOException               if the pipe is
     *                                   <a href=PipedInputStream.html#BROKEN> <code>broken</code></a>,
     *                                   {@link #connect(java.io.PipedWriter) unconnected}, closed,
     *                                   or an I/O error occurs.
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // (从缓冲区)读取len个字符并存入字符数组b的off处，返回读取到的字符数量
    public synchronized int read(char[] cbuf, int off, int len) throws IOException {
        if(!connected) {
            throw new IOException("Pipe not connected");
        } else if(closedByReader) {
            throw new IOException("Pipe closed");
        } else if(writeSide != null && !writeSide.isAlive() && !closedByWriter && (in<0)) {
            throw new IOException("Write end dead");
        }
        
        if((off<0) || (off>cbuf.length) || (len<0) || ((off + len)>cbuf.length) || ((off + len)<0)) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return 0;
        }
        
        /* possibly wait on the first character */
        int c = read();
        if(c<0) {
            return -1;
        }
        
        cbuf[off] = (char) c;
        
        int rlen = 1;
        
        while((in >= 0) && (--len>0)) {
            cbuf[off + rlen] = buffer[out++];
            rlen++;
            
            // 如果读游标越界，则循环到0
            if(out >= buffer.length) {
                out = 0;
            }
            
            /*
             * 缓冲区为空时，需要重置为初始状态
             *
             * 缓冲区是循环使用的，如果不重置in的状态，那么当out==in时，就无法区分缓冲区的满与空了
             */
            if(in == out) {
                /* now empty */
                in = -1;
            }
        }
        
        return rlen;
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes this piped stream and releases any system resources
     * associated with the stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 关闭读管道
    public void close() throws IOException {
        in = -1;
        closedByReader = true;
    }
    
    /**
     * Tell whether this stream is ready to be read.  A piped character
     * stream is ready if the circular buffer is not empty.
     *
     * @throws IOException if the pipe is
     *                     <a href=PipedInputStream.html#BROKEN> <code>broken</code></a>,
     *                     {@link #connect(java.io.PipedWriter) unconnected}, or closed.
     */
    // 返回读通道缓冲区可读字符数量
    public synchronized boolean ready() throws IOException {
        if(!connected) {
            throw new IOException("Pipe not connected");
        } else if(closedByReader) {
            throw new IOException("Pipe closed");
        } else if(writeSide != null && !writeSide.isAlive() && !closedByWriter && (in<0)) {
            throw new IOException("Write end dead");
        }
        return in >= 0;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 交互 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Causes this piped reader to be connected
     * to the piped  writer <code>src</code>.
     * If this object is already connected to some
     * other piped writer, an <code>IOException</code>
     * is thrown.
     * <p>
     * If <code>src</code> is an
     * unconnected piped writer and <code>snk</code>
     * is an unconnected piped reader, they
     * may be connected by either the call:
     *
     * <pre><code>snk.connect(src)</code> </pre>
     * <p>
     * or the call:
     *
     * <pre><code>src.connect(snk)</code> </pre>
     * <p>
     * The two calls have the same effect.
     *
     * @param src The piped writer to connect to.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 与指定的写管道建立连接
    public void connect(PipedWriter src) throws IOException {
        src.connect(this);
    }
    
    
    /**
     * Receives a char of data. This method will block if no input is
     * available.
     */
    // 由写管道(线程)调用：接收一个字符，存入读管道的缓冲区
    synchronized void receive(int c) throws IOException {
        if(!connected) {
            throw new IOException("Pipe not connected");
        } else if(closedByWriter || closedByReader) {
            throw new IOException("Pipe closed");
        } else if(readSide != null && !readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
        
        // 写线程(receive方法由写线程调用)
        writeSide = Thread.currentThread();
        
        /*
         * 在缓冲区数据为满时阻塞当前线程(一般来说阻塞的是写线程，会定时醒来)，
         * 并释放读管道的锁(以便读线程获取该锁来读取数据或重置缓冲区)
         */
        while(in == out) {
            if((readSide != null) && !readSide.isAlive()) {
                throw new IOException("Pipe broken");
            }
            
            /* full: kick any waiting readers */
            notifyAll();
            
            try {
                wait(1000);
            } catch(InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
        
        // 如果缓冲区处于初始状态
        if(in<0) {
            in = 0;
            out = 0;
        }
        
        // 向缓冲区存入一个字符
        buffer[in++] = (char) c;
        
        // 如果写游标越界，则循环到0
        if(in >= buffer.length) {
            in = 0;
        }
    }
    
    /**
     * Receives data into an array of characters.  This method will
     * block until some input is available.
     */
    // 由写管道(线程)调用：接收字符数组c中off处起的len个字符，并存入读管道的缓冲区
    synchronized void receive(char[] c, int off, int len) throws IOException {
        while(--len >= 0) {
            receive(c[off++]);
        }
    }
    
    /*▲ 交互 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Notifies all waiting threads that the last character of data has been
     * received.
     */
    // 关闭写管道，并唤醒所有阻塞的线程
    synchronized void receivedLast() {
        closedByWriter = true;
        notifyAll();
    }
    
    // 初始化读管道的缓冲区
    private void initPipe(int pipeSize) {
        if(pipeSize<=0) {
            throw new IllegalArgumentException("Pipe size <= 0");
        }
        
        buffer = new char[pipeSize];
    }
    
}
