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

package java.util.zip;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;

/**
 * This class implements a stream filter for uncompressing data in the
 * "deflate" compression format. It is also used as the basis for other
 * decompression filters, such as GZIPInputStream.
 *
 * @author David Connelly
 * @see Inflater
 * @since 1.1
 */
// (解压)输入流：从指定的源头输入流读取已压缩的数据，将其解压后填充到给定的内存
public class InflaterInputStream extends FilterInputStream {
    
    /**
     * Decompressor for this stream.
     */
    protected Inflater inf;     // 解压器
    
    boolean usesDefaultInflater = false;    // 是否使用了具有默认解压级别的解压器
    
    /**
     * Input buffer for decompression.
     */
    protected byte[] buf;       // 解压缓冲区，缓存解压前的数据
    
    /**
     * Length of input buffer.
     */
    protected int len;          // 待解压的已压缩字节数
    
    private boolean closed = false;     // 输入流是否已关闭
    
    // this flag is set to true after EOF has reached
    private boolean reachEOF = false;   // 是否到达输入流的尾部
    
    private byte[] singleByteBuf = new byte[1]; // 临时存储单个解压后的字节
    private byte[] b = new byte[512];           // 临时存储需要跳过的字节
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new input stream with a default decompressor and buffer size.
     *
     * @param in the input stream
     */
    /*
     * 用指定的源头输入流构造(解压)输入流。
     * 默认使用具有默认解压级别的解压器，且解压缓冲区的容量为512个字节。
     */
    public InflaterInputStream(InputStream in) {
        this(in, new Inflater());
        usesDefaultInflater = true;
    }
    
    /**
     * Creates a new input stream with the specified decompressor and a
     * default buffer size.
     *
     * @param in  the input stream
     * @param inf the decompressor ("inflater")
     */
    /*
     * 用指定的源头输入流和指定的解压器构造(解压)输入流。
     * 解压缓冲区的容量为512个字节。
     */
    public InflaterInputStream(InputStream in, Inflater inf) {
        this(in, inf, 512);
    }
    
    /**
     * Creates a new input stream with the specified decompressor and
     * buffer size.
     *
     * @param in   the input stream
     * @param inf  the decompressor ("inflater")
     * @param size the input buffer size
     *
     * @throws IllegalArgumentException if {@code size <= 0}
     */
    /*
     * 用指定的源头输入流和指定的解压器构造(解压)输入流。
     * 解压缓冲区的容量由size参数指定。
     */
    public InflaterInputStream(InputStream in, Inflater inf, int size) {
        super(in);
        
        if(in == null || inf == null) {
            throw new NullPointerException();
        } else if(size<=0) {
            throw new IllegalArgumentException("buffer size <= 0");
        }
        
        this.inf = inf;
        
        buf = new byte[size];
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/解压 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads a byte of uncompressed data. This method will block until
     * enough input is available for decompression.
     *
     * @return the byte read, or -1 if end of compressed input is reached
     *
     * @throws IOException if an I/O error has occurred
     */
    /*
     * 从源头输入流中读取(解压)出一个解压后的字节，并将其返回。
     *
     * 如果返回-1，表示已经没有可解压字节，
     * 但不代表解压器缓冲区内没有字节(因为有些数据并不需要解压)。
     */
    public int read() throws IOException {
        ensureOpen();
        
        return read(singleByteBuf, 0, 1) == -1
            ? -1
            : Byte.toUnsignedInt(singleByteBuf[0]); // 将当前byte转换为无符号形式，用int存储
    }
    
    /**
     * Reads uncompressed data into an array of bytes. If <code>len</code> is not
     * zero, the method will block until some input can be decompressed; otherwise,
     * no bytes are read and <code>0</code> is returned.
     *
     * @param b   the buffer into which the data is read
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read
     *
     * @return the actual number of bytes read, or -1 if the end of the
     * compressed input is reached or a preset dictionary is needed
     *
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws ZipException              if a ZIP format error has occurred
     * @throws IOException               if an I/O error has occurred
     */
    /*
     * 从源头输入流中读取(解压)出len个解压后的字节，并将其存入b的off处。
     *
     * 返回值表示实际得到的解压后的字节数，如果返回-1，表示已经没有可解压字节，
     * 但不代表解压器缓冲区内或输入流中没有字节(因为有些数据并不需要解压)。
     */
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        
        if(b == null) {
            throw new NullPointerException();
        } else if(off<0 || len<0 || len>b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return 0;
        }
        
        try {
            int n;
            
            /*
             * 向字节数组b的指定范围填充解压后的数据，返回实际填充的字节数
             * 如果返回值为0，即没有成功解压数据，则需要进一步排查原因
             */
            while((n = inf.inflate(b, off, len)) == 0) {
                // 如果解压器已经完成解压，或者解压器需要字典，则结束解压
                if(inf.finished() || inf.needsDictionary()) {
                    reachEOF = true;    // 标记输入流到底
                    return -1;
                }
                
                // 如果需要更多输入数据(缓冲区中没有待解压数据)，则先向解压器填充待解压数据
                if(inf.needsInput()) {
                    fill();     // 向解压器填充待解压的数据
                }
            }
            
            return n;
        } catch(DataFormatException e) {
            String s = e.getMessage();
            throw new ZipException(s != null ? s : "Invalid ZLIB data format");
        }
    }
    
    
    /**
     * Fills input buffer with more data to decompress.
     *
     * @throws IOException if an I/O error has occurred
     */
    // 向解压器填充待解压的数据
    protected void fill() throws IOException {
        ensureOpen();
        
        // 从源头输入流读取解压前的数据
        len = in.read(buf, 0, buf.length);
        
        if(len == -1) {
            throw new EOFException("Unexpected end of ZLIB input stream");
        }
        
        // 向解压器添加待解压数据
        inf.setInput(buf, 0, len);
    }
    
    /*▲ 读/解压 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns 0 after EOF has been reached, otherwise always return 1.
     * <p>
     * Programs should not count on this method to return the actual number
     * of bytes that could be read without blocking.
     *
     * @return 1 before EOF and 0 after EOF.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 判断当前(解压)输入流中是否有未读(未解压)数据
    public int available() throws IOException {
        ensureOpen();
        
        // 如果已经读到输入流尾部
        if(reachEOF) {
            return 0;
            
            // 如果解压器已经完成解压
        } else if(inf.finished()) {
            // the end of the compressed data stream has been reached
            reachEOF = true;
            return 0;
        } else {
            return 1;
        }
    }
    
    /**
     * Skips specified number of bytes of uncompressed data.
     *
     * @param n the number of bytes to skip
     *
     * @return the actual number of bytes skipped.
     *
     * @throws IOException              if an I/O error has occurred
     * @throws IllegalArgumentException if {@code n < 0}
     */
    // 跳过当前(解压)输入流中len个解压后的字节
    public long skip(long n) throws IOException {
        if(n<0) {
            throw new IllegalArgumentException("negative skip length");
        }
        
        ensureOpen();
        
        int max = (int) Math.min(n, Integer.MAX_VALUE);
        int total = 0;
        
        while(total<max) {
            int len = max - total;
            if(len>b.length) {
                len = b.length;
            }
            
            len = read(b, 0, len);
            
            if(len == -1) {
                reachEOF = true;
                break;
            }
            
            total += len;
        }
        
        return total;
    }
    
    /**
     * Tests if this input stream supports the <code>mark</code> and
     * <code>reset</code> methods. The <code>markSupported</code>
     * method of <code>InflaterInputStream</code> returns
     * <code>false</code>.
     *
     * @return a <code>boolean</code> indicating if this stream type supports
     * the <code>mark</code> and <code>reset</code> methods.
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
     * <p> The <code>mark</code> method of <code>InflaterInputStream</code>
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
     * <code>InflaterInputStream</code> does nothing except throw an
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
    
    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     *
     * @throws IOException if an I/O error has occurred
     */
    // 关闭(解压)输入流
    public void close() throws IOException {
        if(!closed) {
            if(usesDefaultInflater) {
                inf.end();
            }
            in.close();
            closed = true;
        }
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Check to make sure that this stream has not been closed
     */
    // 确保(解压)输入流未关闭
    private void ensureOpen() throws IOException {
        if(closed) {
            throw new IOException("Stream closed");
        }
    }
    
}
