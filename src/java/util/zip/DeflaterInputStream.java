/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Implements an input stream filter for compressing data in the "deflate"
 * compression format.
 *
 * @author David R Tribble (david@tribble.com)
 * @see DeflaterOutputStream
 * @see InflaterOutputStream
 * @see InflaterInputStream
 * @since 1.6
 */
// (压缩)输入流：从指定的源头输入流读取未压缩的数据，将其压缩后填充到给定的内存
public class DeflaterInputStream extends FilterInputStream {
    
    /** Compressor for this stream. */
    protected final Deflater def; // 压缩器
    
    /** Default compressor is used. */
    private boolean usesDefaultDeflater = false; // 是否使用了具有默认压缩级别的压缩器
    
    /** Input buffer for reading compressed data. */
    protected final byte[] buf; // 压缩缓冲区，缓存压缩前的数据
    
    /** Temporary read buffer. */
    private byte[] rbuf = new byte[1]; // 临时存储单个压缩后的字节
    
    /** End of the underlying input stream has been reached. */
    private boolean reachEOF = false; // 是否到达输入流的尾部
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new input stream with a default compressor and buffer
     * size.
     *
     * @param in input stream to read the uncompressed data to
     *
     * @throws NullPointerException if {@code in} is null
     */
    public DeflaterInputStream(InputStream in) {
        this(in, new Deflater());
        usesDefaultDeflater = true;
    }
    
    /**
     * Creates a new input stream with the specified compressor and a
     * default buffer size.
     *
     * @param in   input stream to read the uncompressed data to
     * @param defl compressor ("deflater") for this stream
     *
     * @throws NullPointerException if {@code in} or {@code defl} is null
     */
    public DeflaterInputStream(InputStream in, Deflater defl) {
        this(in, defl, 512);
    }
    
    /**
     * Creates a new input stream with the specified compressor and buffer
     * size.
     *
     * @param in     input stream to read the uncompressed data to
     * @param defl   compressor ("deflater") for this stream
     * @param bufLen compression buffer size
     *
     * @throws IllegalArgumentException if {@code bufLen <= 0}
     * @throws NullPointerException     if {@code in} or {@code defl} is null
     */
    public DeflaterInputStream(InputStream in, Deflater defl, int bufLen) {
        super(in);
        
        // Sanity checks
        if(in == null)
            throw new NullPointerException("Null input");
        if(defl == null)
            throw new NullPointerException("Null deflater");
        if(bufLen<1)
            throw new IllegalArgumentException("Buffer size < 1");
        
        // Initialize
        def = defl;
        buf = new byte[bufLen];
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads a single byte of compressed data from the input stream.
     * This method will block until some input can be read and compressed.
     *
     * @return a single byte of compressed data, or -1 if the end of the
     * uncompressed input stream is reached
     *
     * @throws IOException if an I/O error occurs or if this stream is
     *                     already closed
     */
    /*
     * 从源头输入流读取待压缩的数据，并将其压缩，随后返回一个压缩后的字节。
     * 如果返回-1，表示本次没有压缩数据，流已经读到尾部了。
     */
    public int read() throws IOException {
        // Read a single byte of compressed data
        int len = read(rbuf, 0, 1);
        
        if(len<=0) {
            return -1;
        }
        
        return (rbuf[0] & 0xFF);
    }
    
    /**
     * Reads compressed data into a byte array.
     * This method will block until some input can be read and compressed.
     *
     * @param b   buffer into which the data is read
     * @param off starting offset of the data within {@code b}
     * @param len maximum number of compressed bytes to read into {@code b}
     *
     * @return the actual number of bytes read, or -1 if the end of the
     * uncompressed input stream is reached
     *
     * @throws IndexOutOfBoundsException if {@code len > b.length - off}
     * @throws IOException               if an I/O error occurs or if this input stream is
     *                                   already closed
     */
    /*
     * 从源头输入流读取待压缩的数据，并将其压缩，随后向字节数组b的off处填充len个压缩后的字节。
     * 返回本次累计压缩后的字节数，如果返回-1，表示本次没有压缩数据，流已经读到尾部了。
     */
    public int read(byte[] b, int off, int len) throws IOException {
        // Sanity checks
        ensureOpen();
        
        if(b == null) {
            throw new NullPointerException("Null buffer for read");
        } else if(off<0 || len<0 || len>b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return 0;
        }
        
        /* Read and compress (deflate) input data bytes */
        int cnt = 0;
        
        // 如果存在待压缩数据，且压缩器还未完成压缩
        while(len>0 && !def.finished()) {
            int n;
            
            /* Read data from the input stream */
            // 如果需要输入更多数据(压缩器内部缓冲区中没有数据)
            if(def.needsInput()) {
                // 尝试从源头输入流读取压缩前的数据，并将其存入压缩缓冲区
                n = in.read(buf, 0, buf.length);
                
                // 如果没有读到待压缩数据
                if(n<0) {
                    // End of the input stream reached
                    def.finish();   // 如果没有待压缩数据，则设置刷新模式为FINISH
                    
                    // 如果仍有待压缩数据
                } else if(n>0) {
                    def.setInput(buf, 0, n);    // 将压缩缓冲区中的待压缩数据存入压缩器
                }
            }
            
            /* Compress the input data, filling the read buffer */
            // 向字节数组b的指定范围填充压缩后的数据(默认刷新模式为NO_FLUSH)，返回实际填充的字节数
            n = def.deflate(b, off, len);
            
            cnt += n;   // 累加压缩后的数据
            
            off += n;
            len -= n;
        }
        
        // 如果压缩器已经完成压缩
        if(cnt == 0 && def.finished()) {
            reachEOF = true;    // 指示已经到达输入流尾部
            cnt = -1;
        }
        
        return cnt;
    }
    
    /*▲ 读/压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns 0 after EOF has been reached, otherwise always return 1.
     * <p>
     * Programs should not count on this method to return the actual number
     * of bytes that could be read without blocking
     *
     * @return zero after the end of the underlying input stream has been
     * reached, otherwise always returns 1
     *
     * @throws IOException if an I/O error occurs or if this stream is
     *                     already closed
     */
    // 判断当前输入流是否可用(仍有待解压数据)
    public int available() throws IOException {
        ensureOpen();
        
        if(reachEOF) {
            return 0;
        }
        
        return 1;
    }
    
    /**
     * Skips over and discards data from the input stream.
     * This method may block until the specified number of bytes are read and
     * skipped. <em>Note:</em> While {@code n} is given as a {@code long},
     * the maximum number of bytes which can be skipped is
     * {@code Integer.MAX_VALUE}.
     *
     * @param n number of bytes to be skipped
     *
     * @return the actual number of bytes skipped
     *
     * @throws IOException if an I/O error occurs or if this stream is
     *                     already closed
     */
    // 跳过n个压缩后的字节
    public long skip(long n) throws IOException {
        if(n<0) {
            throw new IllegalArgumentException("negative skip length");
        }
        
        ensureOpen();
        
        // Skip bytes by repeatedly decompressing small blocks
        if(rbuf.length<512) {
            rbuf = new byte[512];
        }
        
        int total = (int) Math.min(n, Integer.MAX_VALUE);
        long cnt = 0;
        while(total>0) {
            // Read a small block of uncompressed bytes
            int len = read(rbuf, 0, (Math.min(total, rbuf.length)));
            
            if(len<0) {
                break;
            }
            cnt += len;
            total -= len;
        }
        
        return cnt;
    }
    
    /**
     * Always returns {@code false} because this input stream does not support
     * the {@link #mark mark()} and {@link #reset reset()} methods.
     *
     * @return false, always
     */
    // 判断当前输入流是否支持存档标记：默认不支持
    public boolean markSupported() {
        return false;
    }
    
    /**
     * <i>This operation is not supported</i>.
     *
     * @param limit maximum bytes that can be read before invalidating the position marker
     */
    // 设置存档标记，当前输入流不支持标记行为，所以也不会设置存档标记
    public void mark(int limit) {
        // Operation not supported
    }
    
    /**
     * <i>This operation is not supported</i>.
     *
     * @throws IOException always thrown
     */
    // 对于支持设置存档的输入流，可以重置其"读游标"到存档区的起始位置，此处默认不支持重置操作
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
    
    /**
     * Closes this input stream and its underlying input stream, discarding
     * any pending uncompressed data.
     *
     * @throws IOException if an I/O error occurs
     */
    // 关闭(压缩)输入流
    public void close() throws IOException {
        if(in != null) {
            try {
                // Clean up
                if(usesDefaultDeflater) {
                    def.end();
                }
                
                in.close();
            } finally {
                in = null;
            }
        }
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Check to make sure that this stream has not been closed.
     */
    // 确保(压缩)输入流未关闭
    private void ensureOpen() throws IOException {
        if(in == null) {
            throw new IOException("Stream closed");
        }
    }
    
}
