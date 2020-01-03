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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class implements an output stream filter for compressing data in
 * the "deflate" compression format. It is also used as the basis for other
 * types of compression filters, such as GZIPOutputStream.
 *
 * @author David Connelly
 * @see Deflater
 * @since 1.1
 */
// (压缩)输出流：从指定的内存读取未压缩的数据，将其压缩后填充到最终输出流
public class DeflaterOutputStream extends FilterOutputStream {
    
    /**
     * Compressor for this stream.
     */
    protected Deflater def; // 压缩器
    
    boolean usesDefaultDeflater = false; // 是否使用了具有默认压缩级别的压缩器
    
    /**
     * Output buffer for writing compressed data.
     */
    protected byte[] buf; // 压缩缓冲区，缓存压缩后的数据
    
    private final boolean syncFlush; // 是否开启同步刷新，即刷新最终输出流之前，会先刷新压缩器内的压缩缓冲区
    
    /**
     * Indicates that the stream has been closed.
     */
    private boolean closed = false; // 输出流是否已关闭
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new output stream with a default compressor and buffer size.
     *
     * <p>The new output stream instance is created as if by invoking
     * the 2-argument constructor DeflaterOutputStream(out, false).
     *
     * @param out the output stream
     */
    /*
     * 用指定的最终输出流构造(压缩)输出流，且不使用同步刷新。
     * 默认使用具有默认压缩级别的压缩器(不兼容GZIP)，且压缩缓冲区的容量为512个字节。
     */
    public DeflaterOutputStream(OutputStream out) {
        this(out, false);
        usesDefaultDeflater = true;
    }
    
    /**
     * Creates a new output stream with a default compressor, a default
     * buffer size and the specified flush mode.
     *
     * @param out       the output stream
     * @param syncFlush if {@code true} the {@link #flush()} method of this
     *                  instance flushes the compressor with flush mode
     *                  {@link Deflater#SYNC_FLUSH} before flushing the output
     *                  stream, otherwise only flushes the output stream
     *
     * @since 1.7
     */
    /*
     * 用指定的最终输出流构造(压缩)输出流，syncFlush指示是否开启同步刷新。
     * 默认使用具有默认压缩级别的压缩器(不兼容GZIP)，且压缩缓冲区的容量为512个字节。
     */
    public DeflaterOutputStream(OutputStream out, boolean syncFlush) {
        this(out, new Deflater(), 512, syncFlush);
        usesDefaultDeflater = true;
    }
    
    /**
     * Creates a new output stream with the specified compressor and
     * a default buffer size.
     *
     * <p>The new output stream instance is created as if by invoking
     * the 3-argument constructor DeflaterOutputStream(out, def, false).
     *
     * @param out the output stream
     * @param def the compressor ("deflater")
     */
    /*
     * 用指定的最终输出流和指定的压缩器构造(压缩)输出流，且不使用同步刷新。
     * 其中，压缩缓冲区的容量为512个字节。
     */
    public DeflaterOutputStream(OutputStream out, Deflater def) {
        this(out, def, 512, false);
    }
    
    /**
     * Creates a new output stream with the specified compressor, flush
     * mode and a default buffer size.
     *
     * @param out       the output stream
     * @param def       the compressor ("deflater")
     * @param syncFlush if {@code true} the {@link #flush()} method of this
     *                  instance flushes the compressor with flush mode
     *                  {@link Deflater#SYNC_FLUSH} before flushing the output
     *                  stream, otherwise only flushes the output stream
     *
     * @since 1.7
     */
    /*
     * 用指定的最终输出流和指定的压缩器构造(压缩)输出流，syncFlush指示是否开启同步刷新。
     * 其中，压缩缓冲区的容量为512个字节。
     */
    public DeflaterOutputStream(OutputStream out, Deflater def, boolean syncFlush) {
        this(out, def, 512, syncFlush);
    }
    
    /**
     * Creates a new output stream with the specified compressor and
     * buffer size.
     *
     * <p>The new output stream instance is created as if by invoking
     * the 4-argument constructor DeflaterOutputStream(out, def, size, false).
     *
     * @param out  the output stream
     * @param def  the compressor ("deflater")
     * @param size the output buffer size
     *
     * @throws IllegalArgumentException if {@code size <= 0}
     */
    /*
     * 用指定的最终输出流out、压缩器def、压缩缓冲区容量size来构造(压缩)输出流，且不使用同步刷新。
     */
    public DeflaterOutputStream(OutputStream out, Deflater def, int size) {
        this(out, def, size, false);
    }
    
    /**
     * Creates a new output stream with the specified compressor,
     * buffer size and flush mode.
     *
     * @param out       the output stream
     * @param def       the compressor ("deflater")
     * @param size      the output buffer size
     * @param syncFlush if {@code true} the {@link #flush()} method of this
     *                  instance flushes the compressor with flush mode
     *                  {@link Deflater#SYNC_FLUSH} before flushing the output
     *                  stream, otherwise only flushes the output stream
     *
     * @throws IllegalArgumentException if {@code size <= 0}
     * @since 1.7
     */
    /*
     * 构造(压缩)输出流
     *
     * out       - 最终输出流，压缩后的数据会写入此处
     * def       - 压缩器
     * size      - 压缩缓冲区容量
     * syncFlush - 是否开启同步刷新
     */
    public DeflaterOutputStream(OutputStream out, Deflater def, int size, boolean syncFlush) {
        super(out);
        
        if(out == null || def == null) {
            throw new NullPointerException();
        } else if(size<=0) {
            throw new IllegalArgumentException("buffer size <= 0");
        }
        
        this.def = def;
        this.buf = new byte[size];
        this.syncFlush = syncFlush;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写/压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes a byte to the compressed output stream.
     * This method will block until the byte can be written.
     *
     * @param b the byte to be written
     *
     * @throws IOException if an I/O error has occurred
     */
    // 将单个字节压缩后写入压缩输出流。该方法将阻塞，直到可以写入字节为止
    public void write(int b) throws IOException {
        byte[] buf = new byte[1];
        buf[0] = (byte) (b & 0xff);
        write(buf, 0, 1);
    }
    
    /**
     * Writes an array of bytes to the compressed output stream.
     * This method will block until all the bytes are written.
     *
     * @param b   the data to be written
     * @param off the start offset of the data
     * @param len the length of the data
     *
     * @throws IOException if an I/O error has occurred
     */
    // 将字节数组b中off处起的len个字节压缩后写入压缩输出流。该方法将阻塞，直到可以写入字节为止
    public void write(byte[] b, int off, int len) throws IOException {
        // 如果压缩器已经完成压缩
        if(def.finished()) {
            throw new IOException("write beyond end of stream");
        }
        
        if((off | len | (off + len) | (b.length - (off + len)))<0) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return;
        }
        
        // 如果压缩器还未完成压缩
        if(!def.finished()) {
            // 向压缩器添加待压缩数据(字节数组b中off处起的len个字节)
            def.setInput(b, off, len);
            
            // 如果不需要更多输入数据了(缓冲区中有待压缩数据)，则开始压缩操作，直到待压缩数据为空为止
            while(!def.needsInput()) {
                deflate();  // 压缩数据
            }
        }
    }
    
    
    /**
     * Writes next block of compressed data to the output stream.
     *
     * @throws IOException if an I/O error has occurred
     */
    // 对压缩器中加入的原始数据进行压缩，压缩后的数据会写入最终输出流
    protected void deflate() throws IOException {
        // 向压缩缓冲区buf中填充压缩后的数据(默认刷新模式为NO_FLUSH)
        int len = def.deflate(buf, 0, buf.length);
        
        // 如果压缩后的字节数大于0，说明存在有效的压缩数据
        if(len>0) {
            // 将压缩后的数据写入最终输出流
            out.write(buf, 0, len);
        }
    }
    
    /*▲ 写/压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Flushes the compressed output stream.
     *
     * If {@link #DeflaterOutputStream(OutputStream, Deflater, int, boolean)
     * syncFlush} is {@code true} when this compressed output stream is
     * constructed, this method first flushes the underlying {@code compressor}
     * with the flush mode {@link Deflater#SYNC_FLUSH} to force
     * all pending data to be flushed out to the output stream and then
     * flushes the output stream. Otherwise this method only flushes the
     * output stream without flushing the {@code compressor}.
     *
     * @throws IOException if an I/O error has occurred
     * @since 1.7
     */
    /*
     * 刷新缓冲区的数据到最终输出流
     *
     * 如果syncFlush为true，则会首先刷新压缩器的压缩缓冲区，随后再刷新最终输出流的缓冲区。
     * 否则，仅会刷新最终输出流的缓冲区。
     */
    public void flush() throws IOException {
        // 如果允许同步刷新，且压缩器还未完成压缩
        if(syncFlush && !def.finished()) {
            int len = 0;
            
            while(true){
                // 向压缩缓冲区buf中填充压缩后的数据，刷新模式为SYNC_FLUSH
                len = def.deflate(buf, 0, buf.length, Deflater.SYNC_FLUSH);
                
                // 如果已经没有数据可压缩，则退出循环
                if(len<=0) {
                    break;
                } else {
                    // 将压缩缓冲区buf的前len个字节写入到最终输出流out
                    out.write(buf, 0, len);
                    
                    // 如果压缩后的数据已经无法填满压缩缓冲区，则退出循环
                    if(len<buf.length) {
                        break;
                    }
                }
            }
        }
        
        // 刷新最终输出流的缓冲区
        out.flush();
    }
    
    /**
     * Finishes writing compressed data to the output stream without closing
     * the underlying stream. Use this method when applying multiple filters
     * in succession to the same output stream.
     *
     * @throws IOException if an I/O error has occurred
     */
    /*
     * 设置压缩模式为FINISH，并开始压缩操作。
     *
     * 压缩完成后关闭压缩器，但不会关闭(压缩)输出流，也不会释放压缩器的本地资源。
     * 调用此方法后，其他压缩器仍可以在此输出流上工作。
     */
    public void finish() throws IOException {
        // 如果压缩器还未完成压缩
        if(!def.finished()) {
            
            // 设置刷新模式为FINISH
            def.finish();
            
            // 如果压缩器还未完成压缩，则进行压缩操作，直到完成压缩为止
            while(!def.finished()) {
                deflate();  // 压缩数据
            }
        }
    }
    
    /**
     * Writes remaining compressed data to the output stream and closes the
     * underlying stream.
     *
     * @throws IOException if an I/O error has occurred
     */
    /*
     * 关闭(压缩)输出流，关闭前会先刷新缓冲区，随后还会释放压缩器的本地资源。
     * 调用此方法后，其他压缩器不能再在此输出流上工作。
     */
    public void close() throws IOException {
        if(!closed) {
            // 刷新缓冲区的数据到最终输出流
            finish();
            
            // 如果使用了默认的压缩器
            if(usesDefaultDeflater) {
                def.end();  // 关闭压缩器(本地)并丢弃所有未处理的输入。
            }
            
            // 关闭最终输出流
            out.close();
            
            closed = true;
        }
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
