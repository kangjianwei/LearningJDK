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

/**
 * A <code>PushbackInputStream</code> adds
 * functionality to another input stream, namely
 * the  ability to "push back" or "unread" bytes,
 * by storing pushed-back bytes in an internal buffer.
 * This is useful in situations where
 * it is convenient for a fragment of code
 * to read an indefinite number of data bytes
 * that  are delimited by a particular byte
 * value; after reading the terminating byte,
 * the  code fragment can "unread" it, so that
 * the next read operation on the input stream
 * will reread the byte that was pushed back.
 * For example, bytes representing the  characters
 * constituting an identifier might be terminated
 * by a byte representing an  operator character;
 * a method whose job is to read just an identifier
 * can read until it  sees the operator and
 * then push the operator back to be re-read.
 *
 * @author David Connelly
 * @author Jonathan Payne
 * @since 1.0
 */
// 回推输入流，可以将一些字节暂时填充到回推缓冲区以便后续读取
public class PushbackInputStream extends FilterInputStream {
    
    /**
     * The pushback buffer.
     *
     * @since 1.1
     */
    protected byte[] buf;   // 回推缓冲区
    
    /**
     * The position within the pushback buffer from which the next byte will
     * be read.  When the buffer is empty, <code>pos</code> is equal to
     * <code>buf.length</code>; when the buffer is full, <code>pos</code> is
     * equal to zero.
     *
     * @since 1.1
     */
    protected int pos;      // 指向回退数据的起点
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a <code>PushbackInputStream</code>
     * with a 1-byte pushback buffer, and saves its argument, the input stream
     * <code>in</code>, for later use. Initially,
     * the pushback buffer is empty.
     *
     * @param in the input stream from which bytes will be read.
     */
    // 构造带有容量为1的回推缓冲区的回推输入流
    public PushbackInputStream(InputStream in) {
        this(in, 1);
    }
    
    /**
     * Creates a <code>PushbackInputStream</code>
     * with a pushback buffer of the specified <code>size</code>,
     * and saves its argument, the input stream
     * <code>in</code>, for later use. Initially,
     * the pushback buffer is empty.
     *
     * @param in   the input stream from which bytes will be read.
     * @param size the size of the pushback buffer.
     *
     * @throws IllegalArgumentException if {@code size <= 0}
     * @since 1.1
     */
    // 构造带有容量为size的回推缓冲区的回推输入流
    public PushbackInputStream(InputStream in, int size) {
        super(in);
        
        if(size<=0) {
            throw new IllegalArgumentException("size <= 0");
        }
        
        this.buf = new byte[size];
        this.pos = size;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads the next byte of data from this input stream. The value
     * byte is returned as an <code>int</code> in the range
     * <code>0</code> to <code>255</code>. If no byte is available
     * because the end of the stream has been reached, the value
     * <code>-1</code> is returned. This method blocks until input data
     * is available, the end of the stream is detected, or an exception
     * is thrown.
     *
     * <p> This method returns the most recently pushed-back byte, if there is
     * one, and otherwise calls the <code>read</code> method of its underlying
     * input stream and returns whatever value that method returns.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream has been reached.
     *
     * @throws IOException if this input stream has been closed by
     *                     invoking its {@link #close()} method,
     *                     or an I/O error occurs.
     * @see java.io.InputStream#read()
     */
    // 从回推输入流读取一个字节并返回
    public int read() throws IOException {
        ensureOpen();
        
        // 如果回推缓冲区不为空，则从回推缓冲区读取数据
        if(pos<buf.length) {
            return buf[pos++] & 0xff;
            
            // 从包装的输入流读取数据
        } else {
            return super.read();
        }
    }
    
    /**
     * Reads up to <code>len</code> bytes of data from this input stream into
     * an array of bytes.  This method first reads any pushed-back bytes; after
     * that, if fewer than <code>len</code> bytes have been read then it
     * reads from the underlying input stream. If <code>len</code> is not zero, the method
     * blocks until at least 1 byte of input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
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
     * @throws IOException               if this input stream has been closed by
     *                                   invoking its {@link #close()} method,
     *                                   or an I/O error occurs.
     * @see java.io.InputStream#read(byte[], int, int)
     */
    // 从回推输入流读取len个字节存入字节数组b的off处，返回读取到的字节数
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        
        if(b == null) {
            throw new NullPointerException();
        } else if(off<0 || len<0 || len>b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return 0;
        }
        
        int avail = buf.length - pos;
        
        // 如果回推缓冲区不为空，则从回推缓冲区读取数据
        if(avail>0) {
            if(len<avail) {
                avail = len;
            }
            System.arraycopy(buf, pos, b, off, avail);
            pos += avail;
            off += avail;
            len -= avail;
        }
        
        // 没有读够指定数量的字节
        if(len>0) {
            // 继续从包装的输入流中读取
            len = super.read(b, off, len);
            if(len == -1) {
                return avail == 0 ? -1 : avail;
            }
            
            return avail + len;
        }
        
        return avail;
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 回推 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Pushes back a byte by copying it to the front of the pushback buffer.
     * After this method returns, the next byte to be read will have the value
     * <code>(byte)b</code>.
     *
     * @param b the <code>int</code> value whose low-order
     *          byte is to be pushed back.
     *
     * @throws IOException If there is not enough room in the pushback
     *                     buffer for the byte, or this input stream has been closed by
     *                     invoking its {@link #close()} method.
     */
    // 将指定的字节存入回推缓冲区
    public void unread(int b) throws IOException {
        ensureOpen();
        
        if(pos == 0) {
            throw new IOException("Push back buffer is full");
        }
        
        buf[--pos] = (byte) b;
    }
    
    /**
     * Pushes back an array of bytes by copying it to the front of the
     * pushback buffer.  After this method returns, the next byte to be read
     * will have the value <code>b[0]</code>, the byte after that will have the
     * value <code>b[1]</code>, and so forth.
     *
     * @param b the byte array to push back
     *
     * @throws IOException If there is not enough room in the pushback
     *                     buffer for the specified number of bytes,
     *                     or this input stream has been closed by
     *                     invoking its {@link #close()} method.
     * @since 1.1
     */
    // 将字节数组b中所有字节存入回推缓冲区
    public void unread(byte[] b) throws IOException {
        unread(b, 0, b.length);
    }
    
    /**
     * Pushes back a portion of an array of bytes by copying it to the front
     * of the pushback buffer.  After this method returns, the next byte to be
     * read will have the value <code>b[off]</code>, the byte after that will
     * have the value <code>b[off+1]</code>, and so forth.
     *
     * @param b   the byte array to push back.
     * @param off the start offset of the data.
     * @param len the number of bytes to push back.
     *
     * @throws IOException If there is not enough room in the pushback
     *                     buffer for the specified number of bytes,
     *                     or this input stream has been closed by
     *                     invoking its {@link #close()} method.
     * @since 1.1
     */
    // 将字节数组b中off处起的len个字节存入回推缓冲区
    public void unread(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        
        if(len>pos) {
            throw new IOException("Push back buffer is full");
        }
        
        pos -= len;
        
        System.arraycopy(b, off, buf, pos, len);
    }
    
    /*▲ 回推 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存档 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tests if this input stream supports the <code>mark</code> and
     * <code>reset</code> methods, which it does not.
     *
     * @return <code>false</code>, since this class does not support the
     * <code>mark</code> and <code>reset</code> methods.
     *
     * @see java.io.InputStream#mark(int)
     * @see java.io.InputStream#reset()
     */
    // 判断当前输入流是否支持存档标记：默认不支持
    public boolean markSupported() {
        return false;
    }
    
    /**
     * Marks the current position in this input stream.
     *
     * <p> The <code>mark</code> method of <code>PushbackInputStream</code>
     * does nothing.
     *
     * @param readlimit the maximum limit of bytes that can be read before
     *                  the mark position becomes invalid.
     *
     * @see java.io.InputStream#reset()
     */
    // 设置存档标记，当前输入流不支持标记行为，所以也不会设置存档标记
    public synchronized void mark(int readlimit) {
    }
    
    /**
     * Repositions this stream to the position at the time the
     * <code>mark</code> method was last called on this input stream.
     *
     * <p> The method <code>reset</code> for class
     * <code>PushbackInputStream</code> does nothing except throw an
     * <code>IOException</code>.
     *
     * @throws IOException if this method is invoked.
     * @see java.io.InputStream#mark(int)
     * @see java.io.IOException
     */
    // 对于支持设置存档的输入流，可以重置其"读游标"到存档区的起始位置，此处默认不支持重置操作
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
    
    /*▲ 存档 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes this input stream and releases any system resources
     * associated with the stream.
     * Once the stream has been closed, further read(), unread(),
     * available(), reset(), or skip() invocations will throw an IOException.
     * Closing a previously closed stream has no effect.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 关闭内部包装的输入流，且置空回推缓冲区
    public synchronized void close() throws IOException {
        if(in == null) {
            return;
        }
        in.close();
        in = null;
        buf = null;
    }
    
    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation might be
     * the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     *
     * <p> The method returns the sum of the number of bytes that have been
     * pushed back and the value returned by {@link
     * java.io.FilterInputStream#available available}.
     *
     * @return the number of bytes that can be read (or skipped over) from
     * the input stream without blocking.
     *
     * @throws IOException if this input stream has been closed by
     *                     invoking its {@link #close()} method,
     *                     or an I/O error occurs.
     * @see java.io.FilterInputStream#in
     * @see java.io.InputStream#available()
     */
    // 返回当前回推输入流中未读(可用)的字节数量
    public int available() throws IOException {
        ensureOpen();
        
        // 先获取回推缓冲区中剩余的字节数量
        int n = buf.length - pos;
        
        // 再获取包装的输入流中的字节数量
        int avail = super.available();
        
        return n>(Integer.MAX_VALUE - avail) ? Integer.MAX_VALUE : n + avail;
    }
    
    /**
     * Skips over and discards <code>n</code> bytes of data from this
     * input stream. The <code>skip</code> method may, for a variety of
     * reasons, end up skipping over some smaller number of bytes,
     * possibly zero.  If <code>n</code> is negative, no bytes are skipped.
     *
     * <p> The <code>skip</code> method of <code>PushbackInputStream</code>
     * first skips over the bytes in the pushback buffer, if any.  It then
     * calls the <code>skip</code> method of the underlying input stream if
     * more bytes need to be skipped.  The actual number of bytes skipped
     * is returned.
     *
     * @param n {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws IOException if the stream has been closed by
     *                     invoking its {@link #close()} method,
     *                     {@code in.skip(n)} throws an IOException,
     *                     or an I/O error occurs.
     * @see java.io.FilterInputStream#in
     * @see java.io.InputStream#skip(long n)
     * @since 1.2
     */
    // 跳过n个字节
    public long skip(long n) throws IOException {
        ensureOpen();
        
        if(n<=0) {
            return 0;
        }
        
        long pskip = buf.length - pos;
        if(pskip>0) {
            if(n<pskip) {
                pskip = n;
            }
            pos += pskip;
            n -= pskip;
        }
        
        if(n>0) {
            pskip += super.skip(n);
        }
        
        return pskip;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Check to make sure that this stream has not been closed
     */
    // 确保包装的输入流未关闭
    private void ensureOpen() throws IOException {
        if(in == null) {
            throw new IOException("Stream closed");
        }
    }
    
}
