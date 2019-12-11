/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import sun.nio.cs.StreamEncoder;

/**
 * An OutputStreamWriter is a bridge from character streams to byte streams:
 * Characters written to it are encoded into bytes using a specified {@link
 * java.nio.charset.Charset charset}.  The charset that it uses
 * may be specified by name or may be given explicitly, or the platform's
 * default charset may be accepted.
 *
 * <p> Each invocation of a write() method causes the encoding converter to be
 * invoked on the given character(s).  The resulting bytes are accumulated in a
 * buffer before being written to the underlying output stream.  Note that the
 * characters passed to the write() methods are not buffered.
 *
 * <p> For top efficiency, consider wrapping an OutputStreamWriter within a
 * BufferedWriter so as to avoid frequent converter invocations.  For example:
 *
 * <pre>
 * Writer out
 *   = new BufferedWriter(new OutputStreamWriter(System.out));
 * </pre>
 *
 * <p> A <i>surrogate pair</i> is a character represented by a sequence of two
 * {@code char} values: A <i>high</i> surrogate in the range '&#92;uD800' to
 * '&#92;uDBFF' followed by a <i>low</i> surrogate in the range '&#92;uDC00' to
 * '&#92;uDFFF'.
 *
 * <p> A <i>malformed surrogate element</i> is a high surrogate that is not
 * followed by a low surrogate or a low surrogate that is not preceded by a
 * high surrogate.
 *
 * <p> This class always replaces malformed surrogate elements and unmappable
 * character sequences with the charset's default <i>substitution sequence</i>.
 * The {@linkplain java.nio.charset.CharsetEncoder} class should be used when more
 * control over the encoding process is required.
 *
 * @author Mark Reinhold
 * @see BufferedWriter
 * @see OutputStream
 * @see java.nio.charset.Charset
 * @since 1.1
 */
/*
 * 带有编码器的字符输出流：将指定的字符序列转换为字节后输出到最终输出流
 *
 * 注：该类只是对输出流编码器的简单包装
 */
public class OutputStreamWriter extends Writer {
    
    // 字符输出流编码器：将字符序列编码为字节后，写入到字节输出流
    private final StreamEncoder streamEncoder;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates an OutputStreamWriter that uses the default character encoding.
     *
     * @param out An OutputStream
     */
    // out：最终输出流，是字节最终写入的地方
    public OutputStreamWriter(OutputStream out) {
        super(out);
        
        try {
            streamEncoder = StreamEncoder.forOutputStreamWriter(out, this, (String) null);
        } catch(UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }
    
    /**
     * Creates an OutputStreamWriter that uses the named charset.
     *
     * @param out         An OutputStream
     * @param charsetName The name of a supported
     *                    {@link java.nio.charset.Charset charset}
     *
     * @throws UnsupportedEncodingException If the named encoding is not supported
     */
    /*
     * out：最终输出流，是字节最终写入的地方
     * charsetName：编码字节流时用到的字符集
     */
    public OutputStreamWriter(OutputStream out, String charsetName) throws UnsupportedEncodingException {
        super(out);
        
        if(charsetName == null) {
            throw new NullPointerException("charsetName");
        }
        
        streamEncoder = StreamEncoder.forOutputStreamWriter(out, this, charsetName);
    }
    
    /**
     * Creates an OutputStreamWriter that uses the given charset.
     *
     * @param out An OutputStream
     * @param cs  A charset
     *
     * @spec JSR-51
     * @since 1.4
     */
    /*
     * out：最终输出流，是字节最终写入的地方
     * charsetName：编码字节流时用到的字符集
     */
    public OutputStreamWriter(OutputStream out, Charset charsetName) {
        super(out);
        
        if(charsetName == null) {
            throw new NullPointerException("charset");
        }
        
        streamEncoder = StreamEncoder.forOutputStreamWriter(out, this, charsetName);
    }
    
    /**
     * Creates an OutputStreamWriter that uses the given charset encoder.
     *
     * @param out An OutputStream
     * @param enc A charset encoder
     *
     * @spec JSR-51
     * @since 1.4
     */
    /*
     * out：最终输出流，是字节最终写入的地方
     * enc：编码字节流时用到的编码器
     */
    public OutputStreamWriter(OutputStream out, CharsetEncoder enc) {
        super(out);
        
        if(enc == null) {
            throw new NullPointerException("charset encoder");
        }
        
        streamEncoder = StreamEncoder.forOutputStreamWriter(out, this, enc);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes a single character.
     *
     * @throws IOException If an I/O error occurs
     */
    // 将指定的字符写入到输出流
    public void write(int c) throws IOException {
        streamEncoder.write(c);
    }
    
    /**
     * Writes a portion of an array of characters.
     *
     * @param cbuf Buffer of characters
     * @param off  Offset from which to start writing characters
     * @param len  Number of characters to write
     *
     * @throws IndexOutOfBoundsException If {@code off} is negative, or {@code len} is negative,
     *                                   or {@code off + len} is negative or greater than the length of the given array
     * @throws IOException               If an I/O error occurs
     */
    // 将字符数组cbuf中off处起的len个字符写入到输出流
    public void write(char[] cbuf, int off, int len) throws IOException {
        streamEncoder.write(cbuf, off, len);
    }
    
    /**
     * Writes a portion of a string.
     *
     * @param str A String
     * @param off Offset from which to start writing characters
     * @param len Number of characters to write
     *
     * @throws IndexOutOfBoundsException If {@code off} is negative, or {@code len} is negative,
     *                                   or {@code off + len} is negative or greater than the length
     *                                   of the given string
     * @throws IOException               If an I/O error occurs
     */
    // 将字符串str中off处起的len个字符写入到输出流
    public void write(String str, int off, int len) throws IOException {
        streamEncoder.write(str, off, len);
    }
    
    
    // 将字符序列csq的字符写入到输出流
    @Override
    public Writer append(CharSequence csq) throws IOException {
        if(csq instanceof CharBuffer) {
            streamEncoder.write((CharBuffer) csq);
        } else {
            streamEncoder.write(String.valueOf(csq));
        }
        return this;
    }
    
    // 将字符序列csq[start, end)范围的字符写入到输出流
    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        if(csq == null) {
            csq = "null";
        }
        return append(csq.subSequence(start, end));
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Flushes the output buffer to the underlying byte stream, without flushing
     * the byte stream itself.  This method is non-private only so that it may
     * be invoked by PrintStream.
     */
    // 刷新缓冲区：将输出流编码器内部缓冲区中的字节写到最终输出流中(该操作不会刷新输出流编码器内的最终输出流)
    void flushBuffer() throws IOException {
        streamEncoder.flushBuffer();
    }
    
    /**
     * Flushes the stream.
     *
     * @throws IOException If an I/O error occurs
     */
    // 刷新当前输出流：不仅刷新输出流编码器的内部缓冲区，还刷新输出流编码器内最终字节输出流的内部缓冲区(如果存在)
    public void flush() throws IOException {
        streamEncoder.flush();
    }
    
    // 关闭当前输出流，其实是关闭输出流编码器
    public void close() throws IOException {
        streamEncoder.close();
    }
    
    
    /**
     * Returns the name of the character encoding being used by this stream.
     *
     * <p> If the encoding has an historical name then that name is returned;
     * otherwise the encoding's canonical name is returned.
     *
     * <p> If this instance was created with the {@link
     * #OutputStreamWriter(OutputStream, String)} constructor then the returned
     * name, being unique for the encoding, may differ from the name passed to
     * the constructor.  This method may return {@code null} if the stream has
     * been closed. </p>
     *
     * @return The historical name of this encoding, or possibly
     * <code>null</code> if the stream has been closed
     *
     * @revised 1.4
     * @spec JSR-51
     * @see java.nio.charset.Charset
     */
    // 返回字节编码器使用的字符集名称
    public String getEncoding() {
        return streamEncoder.getEncoding();
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
