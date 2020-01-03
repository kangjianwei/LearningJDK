/*
 * Copyright (c) 1994, 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.Unsafe;

/**
 * A <code>BufferedInputStream</code> adds
 * functionality to another input stream-namely,
 * the ability to buffer the input and to
 * support the <code>mark</code> and <code>reset</code>
 * methods. When  the <code>BufferedInputStream</code>
 * is created, an internal buffer array is
 * created. As bytes  from the stream are read
 * or skipped, the internal buffer is refilled
 * as necessary  from the contained input stream,
 * many bytes at a time. The <code>mark</code>
 * operation  remembers a point in the input
 * stream and the <code>reset</code> operation
 * causes all the  bytes read since the most
 * recent <code>mark</code> operation to be
 * reread before new bytes are  taken from
 * the contained input stream.
 *
 * @author Arthur van Hoff
 * @since 1.0
 */
/*
 * 带有内部缓存区的字节输入流
 *
 * 读取数据时，会先从包装的输入流中读取数据，然后暂存在内部缓冲区中，
 * 最后对外开放缓冲区，避免了频繁读取输入流造成的低效问题。
 */
public class BufferedInputStream extends FilterInputStream {
    
    /**
     * As this class is used early during bootstrap, it's motivated to use
     * Unsafe.compareAndSetObject instead of AtomicReferenceFieldUpdater
     * (or VarHandles) to reduce dependencies and improve startup time.
     */
    private static final Unsafe U = Unsafe.getUnsafe();
    
    private static final long BUF_OFFSET = U.objectFieldOffset(BufferedInputStream.class, "buf");
    
    private static int DEFAULT_BUFFER_SIZE = 8192;
    
    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
    
    /**
     * The internal buffer array where the data is stored. When necessary,
     * it may be replaced by another array of
     * a different size.
     *
     * We null this out with a CAS on close(), which is necessary since
     * closes can be asynchronous. We use nullness of buf[] as primary
     * indicator that this stream is closed. (The "in" field is also
     * nulled out on close.)
     */
    protected volatile byte[] buf;  // 内部缓冲区
    
    /**
     * The index one greater than the index of the last valid byte in the buffer.
     * This value is always in the range <code>0</code> through <code>buf.length</code>;
     * elements <code>buf[0]</code>  through <code>buf[count-1]
     * </code>contain buffered input data obtained from the underlying  input stream.
     */
    protected int count;        // 记录缓冲区buf中的字节数（包括存档数据）
    
    /**
     * The current position in the buffer. This is the index of the next
     * character to be read from the <code>buf</code> array.
     * <p>
     * This value is always in the range <code>0</code>
     * through <code>count</code>. If it is less
     * than <code>count</code>, then  <code>buf[pos]</code>
     * is the next byte to be supplied as input;
     * if it is equal to <code>count</code>, then
     * the  next <code>read</code> or <code>skip</code>
     * operation will require more bytes to be
     * read from the contained  input stream.
     *
     * @see java.io.BufferedInputStream#buf
     */
    protected int pos;          // 缓冲区游标，记录已经从buf中取出的字节
    
    /**
     * The value of the <code>pos</code> field at the time the last
     * <code>mark</code> method was called.
     * <p>
     * This value is always
     * in the range <code>-1</code> through <code>pos</code>.
     * If there is no marked position in  the input
     * stream, this field is <code>-1</code>. If
     * there is a marked position in the input
     * stream,  then <code>buf[markpos]</code>
     * is the first byte to be supplied as input
     * after a <code>reset</code> operation. If
     * <code>markpos</code> is not <code>-1</code>,
     * then all bytes from positions <code>buf[markpos]</code>
     * through  <code>buf[pos-1]</code> must remain
     * in the buffer array (though they may be
     * moved to  another place in the buffer array,
     * with suitable adjustments to the values
     * of <code>count</code>,  <code>pos</code>,
     * and <code>markpos</code>); they may not
     * be discarded unless and until the difference
     * between <code>pos</code> and <code>markpos</code>
     * exceeds <code>marklimit</code>.
     *
     * @see java.io.BufferedInputStream#mark(int)
     * @see java.io.BufferedInputStream#pos
     */
    protected int markpos = -1; // 存档游标的起始位置
    
    /**
     * The maximum read ahead allowed after a call to the <code>mark</code> method before subsequent calls to the <code>reset</code> method fail.
     * Whenever the difference between <code>pos</code> and <code>markpos</code> exceeds <code>marklimit</code>,
     * then the  mark may be dropped by setting <code>markpos</code> to <code>-1</code>.
     *
     * @see java.io.BufferedInputStream#mark(int)
     * @see java.io.BufferedInputStream#reset()
     */
    protected int marklimit;    // 存档上限
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a <code>BufferedInputStream</code>
     * and saves its  argument, the input stream
     * <code>in</code>, for later use. An internal
     * buffer array is created and  stored in <code>buf</code>.
     *
     * @param in the underlying input stream.
     */
    public BufferedInputStream(InputStream in) {
        this(in, DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Creates a <code>BufferedInputStream</code>
     * with the specified buffer size,
     * and saves its  argument, the input stream
     * <code>in</code>, for later use.  An internal
     * buffer array of length  <code>size</code>
     * is created and stored in <code>buf</code>.
     *
     * @param in   the underlying input stream.
     * @param size the buffer size.
     *
     * @throws IllegalArgumentException if {@code size <= 0}.
     */
    public BufferedInputStream(InputStream in, int size) {
        super(in);
        if(size<=0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        
        buf = new byte[size];
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * See the general contract of the <code>read</code> method of <code>InputStream</code>.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     *
     * @throws IOException if this input stream has been closed by
     *                     invoking its {@link #close()} method,
     *                     or an I/O error occurs.
     * @see java.io.FilterInputStream#in
     */
    /*
     * 尝试从当前输入流读取一个字节，读取成功直接返回，读取失败返回-1
     */
    public synchronized int read() throws IOException {
        // 如果缓冲区没有可读数据，则需要读取输入流以填充缓冲区
        if(pos >= count) {
            // 向缓冲区填充数据
            fill();
            
            // 如果缓存区已经没有可读数据（但可能存在存档数据），直接返回-1
            if(pos >= count) {
                return -1;
            }
        }
        
        // 如果输入流没有关闭，返回可用的缓冲区，否则抛出异常
        byte[] buf = getBufIfOpen();
        
        // 返回一个字节
        return buf[pos++] & 0xff;
    }
    
    /**
     * Reads bytes from this byte-input stream into the specified byte array,
     * starting at the given offset.
     *
     * <p> This method implements the general contract of the corresponding
     * <code>{@link InputStream#read(byte[], int, int) read}</code> method of
     * the <code>{@link InputStream}</code> class.  As an additional
     * convenience, it attempts to read as many bytes as possible by repeatedly
     * invoking the <code>read</code> method of the underlying stream.  This
     * iterated <code>read</code> continues until one of the following
     * conditions becomes true: <ul>
     *
     * <li> The specified number of bytes have been read,
     *
     * <li> The <code>read</code> method of the underlying stream returns
     * <code>-1</code>, indicating end-of-file, or
     *
     * <li> The <code>available</code> method of the underlying stream
     * returns zero, indicating that further input requests would block.
     *
     * </ul> If the first <code>read</code> on the underlying stream returns
     * <code>-1</code> to indicate end-of-file then this method returns
     * <code>-1</code>.  Otherwise this method returns the number of bytes
     * actually read.
     *
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many bytes as possible in the same fashion.
     *
     * @param b   destination buffer.
     * @param off offset at which to start storing bytes.
     * @param len maximum number of bytes to read.
     *
     * @return the number of bytes read, or <code>-1</code> if the end of
     * the stream has been reached.
     *
     * @throws IOException if this input stream has been closed by
     *                     invoking its {@link #close()} method,
     *                     or an I/O error occurs.
     */
    /*
     * 尝试从当前输入流读取len个字节，并将读到的内容插入到字节数组b的off索引处
     * 返回值表示成功读取的字节数量(可能小于预期值)，返回-1表示已经没有可读内容了
     */
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        /* Check for closed stream */
        // 如果输入流没有关闭，返回可用的缓冲区，否则抛出异常
        getBufIfOpen();
        
        if((off | len | (off + len) | (b.length - (off + len)))<0) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return 0;
        }
        
        int n = 0;
        
        // 如果所需字节数很多，则需要重复读取
        for(; ; ) {
            // 从包装的输入流中读取数据
            int nread = read1(b, off + n, len - n);
            if(nread<=0) {
                return (n == 0) ? nread : n;
            }
            
            n += nread;
            if(n >= len) {
                return n;
            }
            
            // if not closed but no bytes available, return
            InputStream input = in;
            if(input != null && input.available()<=0) {
                return n;
            }
        }
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存档 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tests if this input stream supports the <code>mark</code>
     * and <code>reset</code> methods. The <code>markSupported</code>
     * method of <code>BufferedInputStream</code> returns
     * <code>true</code>.
     *
     * @return a <code>boolean</code> indicating if this stream type supports
     * the <code>mark</code> and <code>reset</code> methods.
     *
     * @see java.io.InputStream#mark(int)
     * @see java.io.InputStream#reset()
     */
    // 判断当前输入流是否支持存档标记
    public boolean markSupported() {
        return true;
    }
    
    /**
     * See the general contract of the <code>mark</code> method of <code>InputStream</code>.
     *
     * @param readlimit the maximum limit of bytes that can be read before the mark position becomes invalid.
     *
     * @see java.io.BufferedInputStream#reset()
     */
    // 设置存档标记，readlimit是存档上限
    public synchronized void mark(int readlimit) {
        markpos = pos;          // 将存档的起始标记设置为当前缓冲区buf待读取的位置
        marklimit = readlimit;  // 设置存档上限
    }
    
    /**
     * See the general contract of the <code>reset</code>
     * method of <code>InputStream</code>.
     * <p>
     * If <code>markpos</code> is <code>-1</code>
     * (no mark has been set or the mark has been
     * invalidated), an <code>IOException</code>
     * is thrown. Otherwise, <code>pos</code> is
     * set equal to <code>markpos</code>.
     *
     * @throws IOException if this stream has not been marked or,
     *                     if the mark has been invalidated, or the stream
     *                     has been closed by invoking its {@link #close()}
     *                     method, or an I/O error occurs.
     * @see java.io.BufferedInputStream#mark(int)
     */
    // 重置缓冲区：将缓冲区buf的游标移动到存档游标的起始位置
    public synchronized void reset() throws IOException {
        /* Cause exception if closed */
        getBufIfOpen(); // 如果输入流没有关闭，返回可用的缓冲区，否则抛出异常
        
        if(markpos<0) {
            throw new IOException("Resetting to invalid mark");
        }
        
        pos = markpos;
    }
    
    /*▲ 存档 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes this input stream and releases any system resources
     * associated with the stream.
     * Once the stream has been closed, further read(), available(), reset(),
     * or skip() invocations will throw an IOException.
     * Closing a previously closed stream has no effect.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 关闭输入流，置空缓存区
    public void close() throws IOException {
        byte[] buffer;
        while((buffer = buf) != null) {
            if(U.compareAndSetObject(this, BUF_OFFSET, buffer, null)) {
                InputStream input = in;
                in = null;
                if(input != null) {
                    input.close();
                }
                return;
            }
            
            // Else retry in case a new buf was CASed in fill()
        }
    }
    
    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation might be
     * the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     * <p>
     * This method returns the sum of the number of bytes remaining to be read in
     * the buffer (<code>count&nbsp;- pos</code>) and the result of calling the
     * {@link java.io.FilterInputStream#in in}.available().
     *
     * @return an estimate of the number of bytes that can be read (or skipped
     * over) from this input stream without blocking.
     *
     * @throws IOException if this input stream has been closed by
     *                     invoking its {@link #close()} method,
     *                     or an I/O error occurs.
     */
    // 返回剩余可不被阻塞地读取（或跳过）的字节数（估计值），其中包含了缓冲区中剩余可读的字节
    public synchronized int available() throws IOException {
        int n = count - pos;
        
        // 如果输入流没有关闭，返回可用的输入流，否则抛出异常
        InputStream in = getInIfOpen();
        
        // 返回剩余可不被阻塞地读取（或跳过）的字节数（估计值）
        int avail = in.available();
        
        return n>(Integer.MAX_VALUE - avail) ? Integer.MAX_VALUE : n + avail;
    }
    
    /**
     * See the general contract of the <code>skip</code>
     * method of <code>InputStream</code>.
     *
     * @throws IOException if this input stream has been closed by
     *                     invoking its {@link #close()} method,
     *                     {@code in.skip(n)} throws an IOException,
     *                     or an I/O error occurs.
     */
    // 读取中跳过n个字节，返回实际跳过的字节数
    public synchronized long skip(long n) throws IOException {
        // Check for closed stream
        getBufIfOpen();
        
        if(n<=0) {
            return 0;
        }
        
        // 缓存区中剩余可读字节数
        long avail = count - pos;
        
        // 如果缓冲区已经没有可读的数据，则需要先填充缓冲区
        if(avail<=0) {
            /* If no mark position set then don't keep in buffer */
            if(markpos<0) {
                return getInIfOpen().skip(n);
            }
            
            /* Fill in buffer to save bytes for reset */
            fill();
            
            avail = count - pos;
            if(avail<=0) {
                return 0;
            }
        }
        
        long skipped = (avail<n) ? avail : n;
        
        // 跳过一部分可读字节
        pos += skipped;
        
        return skipped;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Check to make sure that underlying input stream has not been
     * nulled out due to close; if not return it;
     */
    // 如果输入流没有关闭，返回可用的输入流，否则抛出异常
    private InputStream getInIfOpen() throws IOException {
        InputStream input = in;
        
        if(input == null) {
            throw new IOException("Stream closed");
        }
        
        return input;
    }
    
    /**
     * Check to make sure that buffer has not been nulled out due to close; if not return it;
     */
    // 如果输入流没有关闭，返回可用的缓冲区，否则抛出异常
    private byte[] getBufIfOpen() throws IOException {
        byte[] buffer = buf;
        
        if(buffer == null) {
            throw new IOException("Stream closed");
        }
        
        return buffer;
    }
    
    /**
     * Fills the buffer with more data, taking into account
     * shuffling and other tricks for dealing with marks.
     * Assumes that it is being called by a synchronized method.
     * This method also assumes that all data has already been read in,
     * hence pos > count.
     */
    // 向缓冲区填充数据
    private void fill() throws IOException {
        // 如果输入流没有关闭，返回可用的缓冲区，否则抛出异常
        byte[] buffer = getBufIfOpen();
        
        /* no mark: throw away the buffer */
        // 如果没有存档标记
        if(markpos<0) {
            pos = 0;    // 缓冲区游标重置为0
            
            // 如果存在存档标记
        } else {
            /* no room left in buffer */
            // 如果缓冲区中已经没有剩余空间
            if(pos >= buffer.length) {
                /* can throw away early part of the buffer */
                // 如果存档标记大于0
                if(markpos>0) {
                    // 计算需要存档的“长度”
                    int sz = pos - markpos;
                    // 将需要存档的内容拷贝到缓冲区靠前的部分
                    System.arraycopy(buffer, markpos, buffer, 0, sz);
                    // 缓冲区游标仍然紧跟在存档后面
                    pos = sz;
                    // 此时存档标记为位置在索引0处
                    markpos = 0;
                    
                    // 存档标记等于0的情形：
                } else {
                    // 如果缓冲区容量>=存档上限，则需要抛弃存档信息
                    if(buffer.length >= marklimit) {
                        markpos = -1;   /* buffer got too big, invalidate mark */
                        pos = 0;        /* drop buffer contents */
                    } else if(buffer.length >= MAX_BUFFER_SIZE) {
                        throw new OutOfMemoryError("Required array size too large");
                        
                        // 如果缓冲区长度小于存档上限，且未超出阈值时
                    } else {
                        /* grow buffer */
                        // 尝试将缓冲区容量加倍
                        int nsz = (pos<=MAX_BUFFER_SIZE - pos) ? pos * 2 : MAX_BUFFER_SIZE;
                        
                        // 确保加倍后的容量不超过marklimit
                        if(nsz>marklimit) {
                            nsz = marklimit;
                        }
                        
                        byte[] nbuf = new byte[nsz];
                        
                        // 复制原有的存档数据到新缓冲区的前面
                        System.arraycopy(buffer, 0, nbuf, 0, pos);
                        
                        if(!U.compareAndSetObject(this, BUF_OFFSET, buffer, nbuf)) {
                            /*
                             * Can't replace buf if there was an async close.
                             * Note: This would need to be changed if fill() is ever made accessible to multiple threads.
                             * But for now, the only way CAS can fail is via close.
                             * assert buf == null;
                             */
                            throw new IOException("Stream closed");
                        }
                        
                        buffer = nbuf;
                    }
                }
            }
        }
        
        count = pos;
        
        // 如果输入流没有关闭，返回可用的输入流，否则抛出异常
        InputStream in = getInIfOpen();
        
        // 向buffer中填充数据，返回成功读取到的字节数
        int n = in.read(buf, pos, buffer.length - pos);
        
        if(n>0) {
            count = pos + n;
        }
    }
    
    /**
     * Read characters into a portion of an array, reading from the underlying stream at most once if necessary.
     */
    /*
     * 尝试从当前输入流读取len个字节，返回值表示成功读取的字节数量，返回-1表示已经没有可读内容了
     */
    private int read1(byte[] b, int off, int len) throws IOException {
        int avail = count - pos;
        
        if(avail<=0) {
            /*
             * If the requested length is at least as large as the buffer,
             * and if there is no mark/reset activity,
             * do not bother to copy the bytes into the local buffer.
             * In this way buffered streams will cascade harmlessly.
             */
            // 如果待读取数据长度已经超出了缓冲区容量，且没有存档标记
            if(len >= getBufIfOpen().length && markpos<0) {
                // 调用包装的输入流的读取方法
                return getInIfOpen().read(b, off, len);
            }
            
            // 尝试填充缓冲区
            fill();
            
            // 缓冲区中可用的字节数量（不包括存档数据）
            avail = count - pos;
            if(avail<=0) {
                return -1;
            }
        }
        
        int cnt = (avail<len) ? avail : len;
        
        // 将读到的数据复制到字节数组b中
        System.arraycopy(getBufIfOpen(), pos, b, off, cnt);
        
        pos += cnt;
        
        // 返回实际读取到的字节数量
        return cnt;
    }
    
}
