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

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import sun.nio.cs.StreamDecoder;

/**
 * An InputStreamReader is a bridge from byte streams to character streams: It
 * reads bytes and decodes them into characters using a specified {@link
 * java.nio.charset.Charset charset}.  The charset that it uses
 * may be specified by name or may be given explicitly, or the platform's
 * default charset may be accepted.
 *
 * <p> Each invocation of one of an InputStreamReader's read() methods may
 * cause one or more bytes to be read from the underlying byte-input stream.
 * To enable the efficient conversion of bytes to characters, more bytes may
 * be read ahead from the underlying stream than are necessary to satisfy the
 * current read operation.
 *
 * <p> For top efficiency, consider wrapping an InputStreamReader within a
 * BufferedReader.  For example:
 *
 * <pre>
 * BufferedReader in
 *   = new BufferedReader(new InputStreamReader(System.in));
 * </pre>
 *
 * @author Mark Reinhold
 * @see BufferedReader
 * @see InputStream
 * @see java.nio.charset.Charset
 * @since 1.1
 */
// 带有解码器的字符输入流：从源头输入流中读取字节，并将其转换为字符后，存储到指定的容器中
public class InputStreamReader extends Reader {
    
    // 字符输入流解码器：读取字节输入流中的数据，将其解码为字符
    private final StreamDecoder streamDecoder;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates an InputStreamReader that uses the default charset.
     *
     * @param in An InputStream
     */
    // in：源头输入流，是读取字节的地方
    public InputStreamReader(InputStream in) {
        super(in);
        
        try {
            // 返回输入流解码器，in是输入源
            streamDecoder = StreamDecoder.forInputStreamReader(in, this, (String) null); // ## check lock object
        } catch(UnsupportedEncodingException e) {
            // The default encoding should always be available
            throw new Error(e);
        }
    }
    
    /**
     * Creates an InputStreamReader that uses the named charset.
     *
     * @param in          An InputStream
     * @param charsetName The name of a supported
     *                    {@link java.nio.charset.Charset charset}
     *
     * @throws UnsupportedEncodingException If the named charset is not supported
     */
    /*
     * in：源头输入流，是读取字节的地方
     * charsetName：解码字节流时用到的字符集
     */
    public InputStreamReader(InputStream in, String charsetName) throws UnsupportedEncodingException {
        super(in);
        
        if(charsetName == null) {
            throw new NullPointerException("charsetName");
        }
        
        // 返回输入流解码器，in是输入源
        streamDecoder = StreamDecoder.forInputStreamReader(in, this, charsetName);
    }
    
    /**
     * Creates an InputStreamReader that uses the given charset.
     *
     * @param in An InputStream
     * @param cs A charset
     *
     * @spec JSR-51
     * @since 1.4
     */
    /*
     * in：源头输入流，是读取字节的地方
     * charsetName：解码字节流时用到的字符集
     */
    public InputStreamReader(InputStream in, Charset charsetName) {
        super(in);
        
        if(charsetName == null) {
            throw new NullPointerException("charset");
        }
        
        // 返回输入流解码器，in是输入源
        streamDecoder = StreamDecoder.forInputStreamReader(in, this, charsetName);
    }
    
    /**
     * Creates an InputStreamReader that uses the given charset decoder.
     *
     * @param in  An InputStream
     * @param dec A charset decoder
     *
     * @spec JSR-51
     * @since 1.4
     */
    /*
     * in：源头输入流，是读取字节的地方
     * dec：解码字节流时用到的解码器
     */
    public InputStreamReader(InputStream in, CharsetDecoder dec) {
        super(in);
        
        if(dec == null) {
            throw new NullPointerException("charset decoder");
        }
        
        // 返回输入流解码器，in是输入源
        streamDecoder = StreamDecoder.forInputStreamReader(in, this, dec);
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
    // 返回一个从输入流中解码的字符
    public int read() throws IOException {
        return streamDecoder.read();
    }
    
    /**
     * Reads characters into a portion of an array.
     *
     * @param cbuf   Destination buffer
     * @param offset Offset at which to start storing characters
     * @param length Maximum number of characters to read
     *
     * @return The number of characters read, or -1 if the end of the
     * stream has been reached
     *
     * @throws IOException               If an I/O error occurs
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 尝试从字符输入流中读取length个char，并将其填充到cbuf的offset处。返回实际填充的字符数量
    public int read(char[] cbuf, int offset, int length) throws IOException {
        return streamDecoder.read(cbuf, offset, length);
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭输入流
    public void close() throws IOException {
        streamDecoder.close();
    }
    
    /**
     * Tells whether this stream is ready to be read.
     * An InputStreamReader is ready if its input buffer is not empty,
     * or if bytes are available to be read from the underlying byte stream.
     *
     * @throws IOException If an I/O error occurs
     */
    // 判断当前流是否已准备好被读取
    public boolean ready() throws IOException {
        return streamDecoder.ready();
    }
    
    /**
     * Returns the name of the character encoding being used by this stream.
     *
     * <p> If the encoding has an historical name then that name is returned;
     * otherwise the encoding's canonical name is returned.
     *
     * <p> If this instance was created with the {@link
     * #InputStreamReader(InputStream, String)} constructor then the returned
     * name, being unique for the encoding, may differ from the name passed to
     * the constructor. This method will return <code>null</code> if the
     * stream has been closed.
     * </p>
     *
     * @return The historical name of this encoding, or
     * <code>null</code> if the stream has been closed
     *
     * @revised 1.4
     * @spec JSR-51
     * @see java.nio.charset.Charset
     */
    // 返回当前输入流的字节解码器使用的字符集名称
    public String getEncoding() {
        return streamDecoder.getEncoding();
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
