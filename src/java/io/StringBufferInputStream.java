/*
 * Copyright (c) 1995, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * This class allows an application to create an input stream in
 * which the bytes read are supplied by the contents of a string.
 * Applications can also read bytes from a byte array by using a
 * <code>ByteArrayInputStream</code>.
 * <p>
 * Only the low eight bits of each character in the string are used by
 * this class.
 *
 * @author Arthur van Hoff
 * @see java.io.ByteArrayInputStream
 * @see java.io.StringReader
 * @since 1.0
 * @deprecated This class does not properly convert characters into bytes.  As
 * of JDK&nbsp;1.1, the preferred way to create a stream from a
 * string is via the <code>StringReader</code> class.
 */
/*
 * 字符输入流：将字符串作为输入源
 *
 * 特别注意：该输入流只支持对单字节字符（如ASCII码中的字符）进行读取，
 * 对于双字节字符，甚至四字节字符，使用该输入流是无法读取的。
 *
 * 当然，如果仅仅是用来逐字节读取数据，不关注其代表的字符含义，使用该输入流是可以的。
 * 但是，此种情形下，使用BufferedInputStream就可以了。
 *
 * 该类已被标记为过时，过时原因是无法处理多字节字符，建议使用StringReader替代
 */
@Deprecated
public class StringBufferInputStream extends InputStream {
    
    /**
     * The string from which bytes are read.
     */
    protected String buffer;
    
    /**
     * The number of valid characters in the input stream buffer.
     *
     * @see java.io.StringBufferInputStream#buffer
     */
    protected int count;    // 待读取字符(字节)数量
    
    /**
     * The index of the next character to read from the input stream buffer.
     *
     * @see java.io.StringBufferInputStream#buffer
     */
    protected int pos;      // 读游标
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a string input stream to read data from the specified string.
     *
     * @param s the underlying input buffer.
     */
    public StringBufferInputStream(String s) {
        this.buffer = s;
        count = s.length();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads the next byte of data from this input stream. The value
     * byte is returned as an <code>int</code> in the range
     * <code>0</code> to <code>255</code>. If no byte is available
     * because the end of the stream has been reached, the value
     * <code>-1</code> is returned.
     * <p>
     * The <code>read</code> method of
     * <code>StringBufferInputStream</code> cannot block. It returns the
     * low eight bits of the next character in this input stream's buffer.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     */
    /*
     * 尝试从当前输入流读取一个字节，读取成功直接返回，读取失败返回-1
     */
    public synchronized int read() {
        return (pos<count) ? (buffer.charAt(pos++) & 0xFF) : -1;
    }
    
    /**
     * Reads up to <code>len</code> bytes of data from this input stream
     * into an array of bytes.
     * <p>
     * The <code>read</code> method of
     * <code>StringBufferInputStream</code> cannot block. It copies the
     * low eight bits from the characters in this input stream's buffer into
     * the byte array argument.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the maximum number of bytes read.
     *
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the stream has been reached.
     */
    /*
     * 尝试从当前输入流读取len个字节，并将读到的内容插入到字节数组b的off索引处
     * 返回值表示成功读取的字节数量(可能小于预期值)，返回-1表示已经没有可读内容了
     */
    @SuppressWarnings("deprecation")
    public synchronized int read(byte[] b, int off, int len) {
        if(b == null) {
            throw new NullPointerException();
        } else if((off<0) || (off>b.length) || (len<0) || ((off + len)>b.length) || ((off + len)<0)) {
            throw new IndexOutOfBoundsException();
        }
        
        if(pos >= count) {
            return -1;
        }
        
        int avail = count - pos;
        
        if(len>avail) {
            len = avail;
        }
        
        if(len<=0) {
            return 0;
        }
        
        buffer.getBytes(pos, pos + len, b, off);
        
        pos += len;
        
        return len;
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存档 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Resets the input stream to begin reading from the first character
     * of this input stream's underlying buffer.
     */
    // 重置其"读游标"为0
    public synchronized void reset() {
        pos = 0;
    }
    
    /*▲ 存档 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of bytes that can be read from the input
     * stream without blocking.
     *
     * @return the value of <code>count&nbsp;-&nbsp;pos</code>, which is the
     * number of bytes remaining to be read from the input buffer.
     */
    public synchronized int available() {
        return count - pos;
    }
    
    /**
     * Skips <code>n</code> bytes of input from this input stream. Fewer
     * bytes might be skipped if the end of the input stream is reached.
     *
     * @param n the number of bytes to be skipped.
     *
     * @return the actual number of bytes skipped.
     */
    public synchronized long skip(long n) {
        if(n<0) {
            return 0;
        }
        if(n>count - pos) {
            n = count - pos;
        }
        pos += n;
        return n;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
