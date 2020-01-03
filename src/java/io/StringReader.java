/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * A character stream whose source is a string.
 *
 * @author Mark Reinhold
 * @since 1.1
 */
// String输入流：从字符串中读取字符
public class StringReader extends Reader {
    
    private String str;
    private int length;
    private int next = 0;
    private int mark = 0;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new string reader.
     *
     * @param s String providing the character stream.
     */
    public StringReader(String s) {
        this.str = s;
        this.length = s.length();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads a single character.
     *
     * @return The character read, or -1 if the end of the stream has been
     * reached
     *
     * @throws IOException If an I/O error occurs
     */
    // 返回从String字符输入流中读取的一个char，返回-1表示读取失败
    public int read() throws IOException {
        synchronized(lock) {
            ensureOpen();
            
            if(next >= length) {
                return -1;
            }
            
            return str.charAt(next++);
        }
    }
    
    /**
     * Reads characters into a portion of an array.
     *
     * @param cbuf Destination buffer
     * @param off  Offset at which to start writing characters
     * @param len  Maximum number of characters to read
     *
     * @return The number of characters read, or -1 if the end of the
     * stream has been reached
     *
     * @throws IOException               If an I/O error occurs
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 尝试从String字符输入流中读取len个char，并将其填充到cbuf的off处。返回实际填充的字符数量，返回-1表示读取失败
    public int read(char[] cbuf, int off, int len) throws IOException {
        synchronized(lock) {
            ensureOpen();
            
            // 确保cbuf中可以存的下len个字符
            if((off<0) || (off>cbuf.length) || (len<0) || ((off + len)>cbuf.length) || ((off + len)<0)) {
                throw new IndexOutOfBoundsException();
            } else if(len == 0) {
                return 0;
            }
            
            if(next >= length) {
                return -1;
            }
            
            int n = Math.min(length - next, len);
            
            // 将String内部的字节批量转换为char后存入cbuf
            str.getChars(next, next + n, cbuf, off);
            
            next += n;
            
            return n;
        }
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存档 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether this stream supports the mark() operation, which it does.
     */
    // 判断当前输入流是否支持存档标记
    public boolean markSupported() {
        return true;
    }
    
    /**
     * Marks the present position in the stream.  Subsequent calls to reset()
     * will reposition the stream to this point.
     *
     * @param readAheadLimit Limit on the number of characters that may be
     *                       read while still preserving the mark.  Because
     *                       the stream's input comes from a string, there
     *                       is no actual limit, so this argument must not
     *                       be negative, but is otherwise ignored.
     *
     * @throws IllegalArgumentException If {@code readAheadLimit < 0}
     * @throws IOException              If an I/O error occurs
     */
    // 设置存档标记，readAheadLimit在此处无效
    public void mark(int readAheadLimit) throws IOException {
        if(readAheadLimit<0) {
            throw new IllegalArgumentException("Read-ahead limit < 0");
        }
        
        synchronized(lock) {
            ensureOpen();
            mark = next;
        }
    }
    
    /**
     * Resets the stream to the most recent mark, or to the beginning of the
     * string if it has never been marked.
     *
     * @throws IOException If an I/O error occurs
     */
    // 对于支持设置存档的输入流，可以重置其"读游标"到存档区的起始位置
    public void reset() throws IOException {
        synchronized(lock) {
            ensureOpen();
            next = mark;
        }
    }
    
    /*▲ 存档 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes the stream and releases any system resources associated with
     * it. Once the stream has been closed, further read(),
     * ready(), mark(), or reset() invocations will throw an IOException.
     * Closing a previously closed stream has no effect. This method will block
     * while there is another thread blocking on the reader.
     */
    // 关闭输入流
    public void close() {
        synchronized(lock) {
            str = null;
        }
    }
    
    /**
     * Tells whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input
     *
     * @throws IOException If the stream is closed
     */
    // 判断当前流是否已准备好被读取，此处总是返回true
    public boolean ready() throws IOException {
        synchronized(lock) {
            ensureOpen();
            
            return true;
        }
    }
    
    /**
     * Skips the specified number of characters in the stream. Returns
     * the number of characters that were skipped.
     *
     * <p>The <code>ns</code> parameter may be negative, even though the
     * <code>skip</code> method of the {@link Reader} superclass throws
     * an exception in this case. Negative values of <code>ns</code> cause the
     * stream to skip backwards. Negative return values indicate a skip
     * backwards. It is not possible to skip backwards past the beginning of
     * the string.
     *
     * <p>If the entire string has been read or skipped, then this method has
     * no effect and always returns 0.
     *
     * @throws IOException If an I/O error occurs
     */
    // 读取中跳过n个字符，返回实际跳过的字符数
    public long skip(long ns) throws IOException {
        synchronized(lock) {
            ensureOpen();
            
            if(next >= length) {
                return 0;
            }
            
            // Bound skip by beginning and end of the source
            long n = Math.min(length - next, ns);
            n = Math.max(-next, n);
            next += n;
            return n;
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /** Check to make sure that the stream has not been closed */
    // 确保输入流未关闭/可用
    private void ensureOpen() throws IOException {
        if(str == null) {
            throw new IOException("Stream closed");
        }
    }
    
}
