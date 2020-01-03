/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * A piped input stream should be connected
 * to a piped output stream; the piped  input
 * stream then provides whatever data bytes
 * are written to the piped output  stream.
 * Typically, data is read from a <code>PipedInputStream</code>
 * object by one thread  and data is written
 * to the corresponding <code>PipedOutputStream</code>
 * by some  other thread. Attempting to use
 * both objects from a single thread is not
 * recommended, as it may deadlock the thread.
 * The piped input stream contains a buffer,
 * decoupling read operations from write operations,
 * within limits.
 * A pipe is said to be <a id="BROKEN"> <i>broken</i> </a> if a
 * thread that was providing data bytes to the connected
 * piped output stream is no longer alive.
 *
 * @author James Gosling
 * @see java.io.PipedOutputStream
 * @since 1.0
 */
// (字节)读管道，需要与写管道配合使用
public class PipedInputStream extends InputStream {
    
    private static final int DEFAULT_PIPE_SIZE = 1024;
    
    /**
     * The default size of the pipe's circular input buffer.
     *
     * This used to be a constant before the pipe size was allowed to change.
     * This field will continue to be maintained for backward compatibility.
     *
     * @since 1.1
     */
    protected static final int PIPE_SIZE = DEFAULT_PIPE_SIZE;
    
    /**
     * The circular buffer into which incoming data is placed.
     *
     * @since 1.1
     */
    protected byte[] buffer;    // 被循环使用的缓冲区，从写管道接收的数据放到此处
    
    /**
     * The index of the position in the circular buffer at which the
     * next byte of data will be stored when received from the connected
     * piped output stream. <code>in&lt;0</code> implies the buffer is empty,
     * <code>in==out</code> implies the buffer is full
     *
     * @since 1.1
     */
    protected int in = -1;      // 写游标，递增以示缓冲区数据增加
    
    /**
     * The index of the position in the circular buffer at which the next
     * byte of data will be read by this piped input stream.
     *
     * @since 1.1
     */
    protected int out = 0;      // 读游标，递增以示缓冲区数据减少
    
    volatile boolean closedByReader;
    boolean closedByWriter;
    
    boolean connected;  // 读写管道是否已建立连接
    
    /*
     * REMIND: identification of the read and write sides needs to be more sophisticated.
     * Either using thread groups (but what about pipes within a thread?)
     * or using finalization (but it may be a long time until the next GC).
     */
    Thread readSide;
    Thread writeSide;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a <code>PipedInputStream</code> so
     * that it is not yet {@linkplain #connect(java.io.PipedOutputStream)
     * connected}.
     * It must be {@linkplain java.io.PipedOutputStream#connect(
     *java.io.PipedInputStream) connected} to a
     * <code>PipedOutputStream</code> before being used.
     */
    // 初始化缓冲区容量为DEFAULT_PIPE_SIZE的读管道，未与写管道建立连接
    public PipedInputStream() {
        initPipe(DEFAULT_PIPE_SIZE);
    }
    
    /**
     * Creates a <code>PipedInputStream</code> so that it is not yet
     * {@linkplain #connect(java.io.PipedOutputStream) connected} and
     * uses the specified pipe size for the pipe's buffer.
     * It must be {@linkplain java.io.PipedOutputStream#connect(
     *java.io.PipedInputStream)
     * connected} to a <code>PipedOutputStream</code> before being used.
     *
     * @param pipeSize the size of the pipe's buffer.
     *
     * @throws IllegalArgumentException if {@code pipeSize <= 0}.
     * @since 1.6
     */
    // 初始化缓冲区容量为pipeSize的读管道，未与写管道建立连接
    public PipedInputStream(int pipeSize) {
        initPipe(pipeSize);
    }
    
    /**
     * Creates a <code>PipedInputStream</code> so
     * that it is connected to the piped output
     * stream <code>src</code>. Data bytes written
     * to <code>src</code> will then be  available
     * as input from this stream.
     *
     * @param src the stream to connect to.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 初始化缓冲区容量为DEFAULT_PIPE_SIZE的读管道，并与指定的写管道建立连接
    public PipedInputStream(PipedOutputStream src) throws IOException {
        this(src, DEFAULT_PIPE_SIZE);
    }
    
    /**
     * Creates a <code>PipedInputStream</code> so that it is
     * connected to the piped output stream
     * <code>src</code> and uses the specified pipe size for
     * the pipe's buffer.
     * Data bytes written to <code>src</code> will then
     * be available as input from this stream.
     *
     * @param src      the stream to connect to.
     * @param pipeSize the size of the pipe's buffer.
     *
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if {@code pipeSize <= 0}.
     * @since 1.6
     */
    // 初始化缓冲区容量为pipeSize的读管道，并与指定的写管道建立连接
    public PipedInputStream(PipedOutputStream src, int pipeSize) throws IOException {
        initPipe(pipeSize);
        connect(src);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads the next byte of data from this piped input stream. The
     * value byte is returned as an <code>int</code> in the range
     * <code>0</code> to <code>255</code>.
     * This method blocks until input data is available, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     *
     * @throws IOException if the pipe is
     *                     {@link #connect(java.io.PipedOutputStream) unconnected},
     *                     <a href="#BROKEN"> <code>broken</code></a>, closed,
     *                     or if an I/O error occurs.
     */
    // (从缓冲区)读取一个字节并返回
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
        
        // 读取一个字节
        int ret = buffer[out++] & 0xFF;
        
        // 如果读游标越界，则循环到0
        if(out >= buffer.length) {
            out = 0;
        }
        
        // 缓冲区为空时，需要重置为初始状态
        if(in == out) {
            /* now empty */
            in = -1;
        }
        
        return ret;
    }
    
    /**
     * Reads up to <code>len</code> bytes of data from this piped input
     * stream into an array of bytes. Less than <code>len</code> bytes
     * will be read if the end of the data stream is reached or if
     * <code>len</code> exceeds the pipe's buffer size.
     * If <code>len </code> is zero, then no bytes are read and 0 is returned;
     * otherwise, the method blocks until at least 1 byte of input is
     * available, end of the stream has been detected, or an exception is
     * thrown.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read.
     *
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the stream has been reached.
     *
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws IOException               if the pipe is <a href="#BROKEN"> <code>broken</code></a>,
     *                                   {@link #connect(java.io.PipedOutputStream) unconnected},
     *                                   closed, or if an I/O error occurs.
     */
    // (从缓冲区)读取len个字节并存入字节数组b的off处，返回读取到的字节数量
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if(b == null) {
            throw new NullPointerException();
        } else if(off<0 || len<0 || len>b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return 0;
        }
        
        /* possibly wait on the first character */
        int c = read();
        if(c<0) {
            return -1;
        }
        
        b[off] = (byte) c;
        
        int rlen = 1;
        
        while((in >= 0) && (len>1)) {
            
            int available;
            
            if(in>out) {
                available = Math.min((buffer.length - out), (in - out));
            } else {
                available = buffer.length - out;
            }
            
            // A byte is read beforehand outside the loop
            if(available>(len - 1)) {
                available = len - 1;
            }
            
            System.arraycopy(buffer, out, b, off + rlen, available);
            out += available;
            rlen += available;
            len -= available;
            
            // 如果读游标越界，则循环到0
            if(out >= buffer.length) {
                out = 0;
            }
            
            /*
             * 缓冲区为空时，需要重置为初始状态
             *
             * 缓冲区是循环使用的，如果不重置in的状态，那么当out==in时，就无法区分缓冲区的满与空了
             */
            if(out == in) {
                /* now empty */
                in = -1;
            }
        }
        
        return rlen;
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes this piped input stream and releases any system resources
     * associated with the stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 关闭读管道
    public void close() throws IOException {
        closedByReader = true;
        synchronized(this) {
            in = -1;
        }
    }
    
    /**
     * Returns the number of bytes that can be read from this input
     * stream without blocking.
     *
     * @return the number of bytes that can be read from this input stream
     * without blocking, or {@code 0} if this input stream has been
     * closed by invoking its {@link #close()} method, or if the pipe
     * is {@link #connect(java.io.PipedOutputStream) unconnected}, or
     * <a href="#BROKEN"> <code>broken</code></a>.
     *
     * @throws IOException if an I/O error occurs.
     * @since 1.0.2
     */
    // 返回读通道缓冲区可读字节数量
    public synchronized int available() throws IOException {
        if(in<0) {
            return 0;
        } else if(in == out) {
            return buffer.length;
        } else if(in>out) {
            return in - out;
        } else {
            return in + buffer.length - out;
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 交互 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Causes this piped input stream to be connected
     * to the piped  output stream <code>src</code>.
     * If this object is already connected to some
     * other piped output  stream, an <code>IOException</code>
     * is thrown.
     * <p>
     * If <code>src</code> is an
     * unconnected piped output stream and <code>snk</code>
     * is an unconnected piped input stream, they
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
     * @param src The piped output stream to connect to.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 与指定的写管道建立连接
    public void connect(PipedOutputStream src) throws IOException {
        src.connect(this);
    }
    
    
    /**
     * Receives a byte of data.  This method will block if no input is
     * available.
     *
     * @param b the byte being received
     *
     * @throws IOException If the pipe is <a href="#BROKEN"> <code>broken</code></a>,
     *                     {@link #connect(java.io.PipedOutputStream) unconnected},
     *                     closed, or if an I/O error occurs.
     * @since 1.1
     */
    // 由写管道(线程)调用：接收一个字节，存入读管道的缓冲区
    protected synchronized void receive(int b) throws IOException {
        checkStateForReceive();
        
        // 写线程(receive方法由写线程调用)
        writeSide = Thread.currentThread();
        
        // 在缓冲区数据为满时阻塞当前线程(一般来说阻塞的是写线程，会定时醒来)，并释放读管道的锁(以便读线程获取该锁来读取数据或重置缓冲区)
        if(in == out) {
            awaitSpace();
        }
        
        // 如果缓冲区处于初始状态
        if(in<0) {
            in = 0;
            out = 0;
        }
        
        // 向缓冲区存入一个字节
        buffer[in++] = (byte) (b & 0xFF);
        
        // 如果写游标越界，则循环到0
        if(in >= buffer.length) {
            in = 0;
        }
    }
    
    /**
     * Receives data into an array of bytes.  This method will
     * block until some input is available.
     *
     * @param b   the buffer into which the data is received
     * @param off the start offset of the data
     * @param len the maximum number of bytes received
     *
     * @throws IOException If the pipe is <a href="#BROKEN"> broken</a>,
     *                     {@link #connect(java.io.PipedOutputStream) unconnected},
     *                     closed,or if an I/O error occurs.
     */
    // 由写管道(线程)调用：接收字节数组b中off处起的len个字节，并存入读管道的缓冲区
    synchronized void receive(byte[] b, int off, int len) throws IOException {
        checkStateForReceive();
        
        writeSide = Thread.currentThread();
        
        int bytesToTransfer = len;
        
        while(bytesToTransfer>0) {
            // 在缓冲区数据为满时阻塞当前线程(一般来说阻塞的是写线程，会定时醒来)，并释放读管道的锁(以便读线程获取该锁来读取数据或重置缓冲区)
            if(in == out) {
                awaitSpace();
            }
            
            int nextTransferAmount = 0;
            if(out<in) {
                nextTransferAmount = buffer.length - in;
            } else if(in<out) {
                if(in == -1) {
                    in = out = 0;
                    nextTransferAmount = buffer.length - in;
                } else {
                    nextTransferAmount = out - in;
                }
            }
            
            if(nextTransferAmount>bytesToTransfer) {
                nextTransferAmount = bytesToTransfer;
            }
            
            assert (nextTransferAmount>0);
            
            // 从b的off处起复制nextTransferAmount个字节到buffer的in处
            System.arraycopy(b, off, buffer, in, nextTransferAmount);
            
            bytesToTransfer -= nextTransferAmount;
            off += nextTransferAmount;
            in += nextTransferAmount;
            
            if(in >= buffer.length) {
                in = 0;
            }
        }
    }
    
    /*▲ 交互 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Notifies all waiting threads that the last byte of data has been
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
            throw new IllegalArgumentException("Pipe Size <= 0");
        }
        
        buffer = new byte[pipeSize];
    }
    
    // 阻塞当前线程(一般来说阻塞的是写线程，会定时醒来)，并释放读管道的锁(以便读线程获取该锁来读取数据或重置缓冲区)
    private void awaitSpace() throws IOException {
        while(in == out) {
            checkStateForReceive();
            
            /* full: kick any waiting readers */
            notifyAll();
            
            try {
                wait(1000);
            } catch(InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
    }
    
    // 确保读/写管道正常工作
    private void checkStateForReceive() throws IOException {
        if(!connected) {
            throw new IOException("Pipe not connected");
        } else if(closedByWriter || closedByReader) {
            throw new IOException("Pipe closed");
        } else if(readSide != null && !readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
    }
    
}
