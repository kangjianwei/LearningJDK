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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Implements an output stream filter for uncompressing data stored in the
 * "deflate" compression format.
 *
 * @author David R Tribble (david@tribble.com)
 * @see InflaterInputStream
 * @see DeflaterInputStream
 * @see DeflaterOutputStream
 * @since 1.6
 */
// (解压)输出流：从指定的内存读取已压缩的数据，将其压缩后填充到最终输出流
public class InflaterOutputStream extends FilterOutputStream {
    
    /** Decompressor for this stream. */
    protected final Inflater inf; // 解压器
    
    /** Default decompressor is used. */
    private boolean usesDefaultInflater = false; // 是否使用了具有默认解压级别的解压器
    
    /** Output buffer for writing uncompressed data. */
    protected final byte[] buf; // 解压缓冲区，缓存解压后的数据
    
    /** Temporary write buffer. */
    private final byte[] wbuf = new byte[1]; // 临时存储单个待解压字节
    
    /** true iff {@link #close()} has been called. */
    private boolean closed = false; // 输出流是否已关闭
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new output stream with a default decompressor and buffer
     * size.
     *
     * @param out output stream to write the uncompressed data to
     *
     * @throws NullPointerException if {@code out} is null
     */
    /*
     * 用指定的最终输出流构造(解压)输出流。
     * 默认使用具有默认解压级别的解压器(不兼容GZIP)，且解压缓冲区的容量为512个字节。
     */
    public InflaterOutputStream(OutputStream out) {
        this(out, new Inflater());
        usesDefaultInflater = true;
    }
    
    /**
     * Creates a new output stream with the specified decompressor and a
     * default buffer size.
     *
     * @param out  output stream to write the uncompressed data to
     * @param infl decompressor ("inflater") for this stream
     *
     * @throws NullPointerException if {@code out} or {@code infl} is null
     */
    /*
     * 用指定的最终输出流和指定的解压器构造(解压)输出流。
     * 其中，解压缓冲区的容量为512个字节。
     */
    public InflaterOutputStream(OutputStream out, Inflater infl) {
        this(out, infl, 512);
    }
    
    /**
     * Creates a new output stream with the specified decompressor and
     * buffer size.
     *
     * @param out    output stream to write the uncompressed data to
     * @param infl   decompressor ("inflater") for this stream
     * @param bufLen decompression buffer size
     *
     * @throws IllegalArgumentException if {@code bufLen <= 0}
     * @throws NullPointerException     if {@code out} or {@code infl} is null
     */
    /*
     * 用指定的最终输出流out、解压器infl、解压缓冲区容量size来构造(解压)输出流。
     */
    public InflaterOutputStream(OutputStream out, Inflater infl, int bufLen) {
        super(out);
        
        // Sanity checks
        if(out == null) {
            throw new NullPointerException("Null output");
        }
        
        if(infl == null) {
            throw new NullPointerException("Null inflater");
        }
        
        if(bufLen<=0) {
            throw new IllegalArgumentException("Buffer size < 1");
        }
        
        // Initialize
        inf = infl;
        buf = new byte[bufLen];
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写/解压 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes a byte to the uncompressed output stream.
     *
     * @param b a single byte of compressed data to decompress and write to
     *          the output stream
     *
     * @throws IOException  if an I/O error occurs or this stream is already
     *                      closed
     * @throws ZipException if a compression (ZIP) format error occurs
     */
    // 将已压缩字节b解压后写入最终输出流
    public void write(int b) throws IOException {
        // Write a single byte of data
        wbuf[0] = (byte) b;
        write(wbuf, 0, 1);
    }
    
    /**
     * Writes an array of bytes to the uncompressed output stream.
     *
     * @param b   buffer containing compressed data to decompress and write to the output stream
     * @param off starting offset of the compressed data within {@code b}
     * @param len number of bytes to decompress from {@code b}
     *
     * @throws IndexOutOfBoundsException if {@code off < 0}, or if {@code len < 0}, or if {@code len > b.length - off}
     * @throws IOException               if an I/O error occurs or this stream is already closed
     * @throws NullPointerException      if {@code b} is null
     * @throws ZipException              if a compression (ZIP) format error occurs
     */
    // 将字节数组b中off处起的len个已压缩字节解压后写入最终输出流
    public void write(byte[] b, int off, int len) throws IOException {
        // Sanity checks
        ensureOpen();
        
        if(b == null) {
            throw new NullPointerException("Null buffer for read");
        } else if(off<0 || len<0 || len>b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return;
        }
        
        // Write uncompressed data to the output stream
        try {
            for(; ; ) {
                int n;
                
                /* Fill the decompressor buffer with output data */
                // 如果需要输入更多数据(解压器内部缓冲区中没有数据)
                if(inf.needsInput()) {
                    int part;
                    
                    if(len<1) {
                        break;
                    }
                    
                    // 每次最多截取512个字节
                    part = (len<512 ? len : 512);
                    
                    // 向1号缓冲区设置待解压数据
                    inf.setInput(b, off, part);
                    
                    off += part;
                    len -= part;
                }
                
                /* Decompress and write blocks of output data */
                do {
                    // 向字节数组buf中填充解压后的数据，返回实际填充的字节数
                    n = inf.inflate(buf, 0, buf.length);
                    if(n>0) {
                        // 如果解压成功，则向最终输出流写入解压后的数据
                        out.write(buf, 0, n);
                    }
                } while(n>0);
                
                /* Check the decompressor */
                // 如果解压器已经完成解压，则需要退出
                if(inf.finished()) {
                    break;
                }
                
                // 如果待解压数据需要为其设置字典，则抛异常（字典需要提前设置进来）
                if(inf.needsDictionary()) {
                    throw new ZipException("ZLIB dictionary missing");
                }
            }
        } catch(DataFormatException ex) {
            // Improperly formatted compressed (ZIP) data
            String msg = ex.getMessage();
            if(msg == null) {
                msg = "Invalid ZLIB data format";
            }
            throw new ZipException(msg);
        }
    }
    
    /*▲ 写/解压 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Flushes this output stream, forcing any pending buffered output bytes to be
     * written.
     *
     * @throws IOException if an I/O error occurs or this stream is already
     *                     closed
     */
    /*
     * 刷新缓冲区的数据到最终输出流
     *
     * 如果解压器中还有缓存的未解压数据，则先将其解压后再写入最终输出流，
     * 其次，将最终输出流的内部缓冲区中的数据刷新到输出端。
     */
    public void flush() throws IOException {
        ensureOpen();
        
        /* Finish decompressing and writing pending output data */
        // 如果解压器还未完成解压
        if(!inf.finished()) {
            try {
                // 如果解压器还未完成解压，且不需要输入更多数据(内部缓冲区中已有数据)
                while(!inf.finished() && !inf.needsInput()) {
                    int n;
                    
                    /* Decompress pending output data */
                    // 向解压缓冲区buf中填充解压后的数据，返回实际填充的字节数
                    n = inf.inflate(buf, 0, buf.length);
                    if(n<1) {
                        break;
                    }
                    
                    /* Write the uncompressed output data block */
                    // 向最终输出流写入解压后的数据
                    out.write(buf, 0, n);
                }
                
                // 将最终输出流的内部缓冲区中的数据刷新到输出端
                super.flush();
            } catch(DataFormatException ex) {
                // Improperly formatted compressed (ZIP) data
                String msg = ex.getMessage();
                if(msg == null) {
                    msg = "Invalid ZLIB data format";
                }
                throw new ZipException(msg);
            }
        }
    }
    
    /**
     * Finishes writing uncompressed data to the output stream without closing
     * the underlying stream.  Use this method when applying multiple filters in
     * succession to the same output stream.
     *
     * @throws IOException if an I/O error occurs or this stream is already
     *                     closed
     */
    // 结束本次解压，但不关闭(解压)输出流
    public void finish() throws IOException {
        ensureOpen();
        
        /* Finish decompressing and writing pending output data */
        flush();    // 先刷新缓冲区
        
        if(usesDefaultInflater) {
            inf.end();
        }
    }
    
    /**
     * Writes any remaining uncompressed data to the output stream and closes
     * the underlying output stream.
     *
     * @throws IOException if an I/O error occurs
     */
    // 关闭(解压)输出流
    public void close() throws IOException {
        if(!closed) {
            // Complete the uncompressed output
            try {
                finish();
            } finally {
                out.close();
                closed = true;
            }
        }
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Checks to make sure that this stream has not been closed.
     */
    // 确保(解压)输出流处于开启状态
    private void ensureOpen() throws IOException {
        if(closed) {
            throw new IOException("Stream closed");
        }
    }
    
}
