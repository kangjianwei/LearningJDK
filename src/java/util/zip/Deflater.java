/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Objects;

import jdk.internal.ref.CleanerFactory;
import sun.nio.ch.DirectBuffer;

/**
 * This class provides support for general purpose compression using the
 * popular ZLIB compression library. The ZLIB compression library was
 * initially developed as part of the PNG graphics standard and is not
 * protected by patents. It is fully described in the specifications at
 * the <a href="package-summary.html#package.description">java.util.zip
 * package description</a>.
 * <p>
 * This class deflates sequences of bytes into ZLIB compressed data format.
 * The input byte sequence is provided in either byte array or byte buffer,
 * via one of the {@code setInput()} methods. The output byte sequence is
 * written to the output byte array or byte buffer passed to the
 * {@code deflate()} methods.
 * <p>
 * The following code fragment demonstrates a trivial compression
 * and decompression of a string using {@code Deflater} and
 * {@code Inflater}.
 *
 * <blockquote><pre>
 * try {
 *     // Encode a String into bytes
 *     String inputString = "blahblahblah";
 *     byte[] input = inputString.getBytes("UTF-8");
 *
 *     // Compress the bytes
 *     byte[] output = new byte[100];
 *     Deflater compresser = new Deflater();
 *     compresser.setInput(input);
 *     compresser.finish();
 *     int compressedDataLength = compresser.deflate(output);
 *     compresser.end();
 *
 *     // Decompress the bytes
 *     Inflater decompresser = new Inflater();
 *     decompresser.setInput(output, 0, compressedDataLength);
 *     byte[] result = new byte[100];
 *     int resultLength = decompresser.inflate(result);
 *     decompresser.end();
 *
 *     // Decode the bytes into a String
 *     String outputString = new String(result, 0, resultLength, "UTF-8");
 * } catch (java.io.UnsupportedEncodingException ex) {
 *     // handle
 * } catch (java.util.zip.DataFormatException ex) {
 *     // handle
 * }
 * </pre></blockquote>
 *
 * @apiNote
 * To release resources used by this {@code Deflater}, the {@link #end()} method
 * should be called explicitly. Subclasses are responsible for the cleanup of resources
 * acquired by the subclass. Subclasses that override {@link #finalize()} in order
 * to perform cleanup should be modified to use alternative cleanup mechanisms such
 * as {@link java.lang.ref.Cleaner} and remove the overriding {@code finalize} method.
 *
 * @implSpec
 * If this {@code Deflater} has been subclassed and the {@code end} method has been
 * overridden, the {@code end} method will be called by the finalization when the
 * deflater is unreachable. But the subclasses should not depend on this specific
 * implementation; the finalization is not reliable and the {@code finalize} method
 * is deprecated to be removed.
 *
 * @see         Inflater
 * @author      David Connelly
 * @since 1.1
 */
// 压缩器
public class Deflater {
    
    /**
     * Compression method for the deflate algorithm (the only one currently supported).
     */
    public static final int DEFLATED = 8;   // 压缩算法：目前仅支持deflate算法
    
    /**
     * Default compression level.
     */
    public static final int DEFAULT_COMPRESSION = -1;   // 压缩级别：默认
    /**
     * Compression level for no compression.
     */
    public static final int NO_COMPRESSION = 0;         // 压缩级别：不压缩
    /**
     * Compression level for fastest compression.
     */
    public static final int BEST_SPEED = 1;             // 压缩级别：速度最快
    /**
     * Compression level for best compression.
     */
    public static final int BEST_COMPRESSION = 9;       // 压缩级别：文件最小
    
    /**
     * Default compression strategy.
     */
    public static final int DEFAULT_STRATEGY = 0;   // 压缩策略：默认
    /**
     * Compression strategy best used for data consisting mostly of small values with a somewhat random distribution.
     * Forces more Huffman coding and less string matching.
     */
    public static final int FILTERED = 1;           // 压缩策略：压缩数据时对数据进行过滤，以达到更好的压缩效果
    /**
     * Compression strategy for Huffman coding only.
     */
    public static final int HUFFMAN_ONLY = 2;       // 压缩策略：哈夫曼压缩
    
    
    /**
     * Compression flush mode used to achieve best compression result.
     *
     * @see Deflater#deflate(byte[], int, int, int)
     * @since 1.7
     */
    public static final int NO_FLUSH = 0;   // 刷新模式：数据被输出之前，预先决定每次压缩多少数据，以实现最大化压缩
    /**
     * Compression flush mode used to flush out all pending output;
     * may degrade compression for some compression algorithms.
     *
     * @see Deflater#deflate(byte[], int, int, int)
     * @since 1.7
     */
    public static final int SYNC_FLUSH = 2; // 刷新模式：将压缩数据分为若干个压缩快，每次将一个压缩块中的数据写入输出缓存区，然后在数据块后面写入一个10位长度的空白数据块.
    /**
     * Compression flush mode used to flush out all pending output and reset the deflater.
     * Using this mode too often can seriously degrade compression.
     *
     * @see Deflater#deflate(byte[], int, int, int)
     * @since 1.7
     */
    public static final int FULL_FLUSH = 3; // 刷新模式：写入方式与SYNC_FLUSH相同，但在数据块写完后复位压缩状态
    /**
     * Flush mode to use at the end of output.  Can only be provided by the
     * user by way of {@link #finish()}.
     */
    private static final int FINISH = 4;    // 刷新模式：输出缓存区中的剩余数据将被全部输出
    
    private final DeflaterZStreamRef zsRef; // 资源清理器
    
    // 存储待压缩数据
    private byte[] inputArray;      // 1号缓冲区
    private int inputPos, inputLim; // 1号缓冲区的读游标与未处理字节数量
    
    // 存储待压缩数据
    private ByteBuffer input = ZipUtils.defaultBuf; // 2号缓冲区，如果存在，会被优先处理
    
    private int level;          // 压缩级别
    private int strategy;       // 压缩策略
    private boolean setParams;  // 是否需要设置level和strategy参数
    
    private boolean finish;     // 是否使用刷新模式：FINISH
    
    private boolean finished;   // 是否完成压缩
    
    private long bytesRead;     // 累计读取了多少压缩前的字节
    private long bytesWritten;  // 累计写入了多少压缩后的字节
    
    
    static {
        ZipUtils.loadLibrary();
    }
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new compressor with the default compression level.
     * Compressed data will be generated in ZLIB format.
     */
    // 构造具有默认压缩级别的压缩器(不兼容GZIP)
    public Deflater() {
        this(DEFAULT_COMPRESSION, false);
    }
    
    /**
     * Creates a new compressor using the specified compression level.
     * Compressed data will be generated in ZLIB format.
     *
     * @param level the compression level (0-9)
     */
    // 构造具有指定压缩级别的压缩器(不兼容GZIP)
    public Deflater(int level) {
        this(level, false);
    }
    
    /**
     * Creates a new compressor using the specified compression level.
     * If 'nowrap' is true then the ZLIB header and checksum fields will
     * not be used in order to support the compression format used in
     * both GZIP and PKZIP.
     *
     * @param level  the compression level (0-9)
     * @param nowrap if true then use GZIP compatible compression
     */
    // 构造具有默认压缩级别的压缩器，nowrap指示是否兼容GZIP
    public Deflater(int level, boolean nowrap) {
        this.level = level;
        this.strategy = DEFAULT_STRATEGY;
        // 初始化一块本地缓存存放压缩过程中用到的一些附加信息，如字典信息、压缩策略、压缩级别等
        long addr = init(level, DEFAULT_STRATEGY, nowrap);
        this.zsRef = DeflaterZStreamRef.get(this, addr);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Compresses the input data and fills specified buffer with compressed
     * data. Returns actual number of bytes of compressed data. A return value
     * of 0 indicates that {@link #needsInput() needsInput} should be called
     * in order to determine if more input data is required.
     *
     * <p>This method uses {@link #NO_FLUSH} as its compression flush mode.
     * An invocation of this method of the form {@code deflater.deflate(b)}
     * yields the same result as the invocation of
     * {@code deflater.deflate(b, 0, b.length, Deflater.NO_FLUSH)}.
     *
     * @param output the buffer for the compressed data
     *
     * @return the actual number of bytes of compressed data written to the output buffer
     */
    // 向压缩缓冲区output填充压缩后的数据(默认刷新模式为NO_FLUSH)，返回实际填充的字节数
    public int deflate(byte[] output) {
        return deflate(output, 0, output.length, NO_FLUSH);
    }
    
    /**
     * Compresses the input data and fills specified buffer with compressed
     * data. Returns actual number of bytes of compressed data. A return value
     * of 0 indicates that {@link #needsInput() needsInput} should be called
     * in order to determine if more input data is required.
     *
     * <p>This method uses {@link #NO_FLUSH} as its compression flush mode.
     * An invocation of this method of the form {@code deflater.deflate(b, off, len)}
     * yields the same result as the invocation of
     * {@code deflater.deflate(b, off, len, Deflater.NO_FLUSH)}.
     *
     * @param output the buffer for the compressed data
     * @param off    the start offset of the data
     * @param len    the maximum number of bytes of compressed data
     *
     * @return the actual number of bytes of compressed data written to the output buffer
     */
    // 向压缩缓冲区output的指定范围填充压缩后的数据(默认刷新模式为NO_FLUSH)，返回实际填充的字节数
    public int deflate(byte[] output, int off, int len) {
        return deflate(output, off, len, NO_FLUSH);
    }
    
    /**
     * Compresses the input data and fills the specified buffer with compressed
     * data. Returns actual number of bytes of data compressed.
     *
     * <p>Compression flush mode is one of the following three modes:
     *
     * <ul>
     * <li>{@link #NO_FLUSH}: allows the deflater to decide how much data
     * to accumulate, before producing output, in order to achieve the best
     * compression (should be used in normal use scenario). A return value
     * of 0 in this flush mode indicates that {@link #needsInput()} should
     * be called in order to determine if more input data is required.
     *
     * <li>{@link #SYNC_FLUSH}: all pending output in the deflater is flushed,
     * to the specified output buffer, so that an inflater that works on
     * compressed data can get all input data available so far (In particular
     * the {@link #needsInput()} returns {@code true} after this invocation
     * if enough output space is provided). Flushing with {@link #SYNC_FLUSH}
     * may degrade compression for some compression algorithms and so it
     * should be used only when necessary.
     *
     * <li>{@link #FULL_FLUSH}: all pending output is flushed out as with
     * {@link #SYNC_FLUSH}. The compression state is reset so that the inflater
     * that works on the compressed output data can restart from this point
     * if previous compressed data has been damaged or if random access is
     * desired. Using {@link #FULL_FLUSH} too often can seriously degrade
     * compression.
     * </ul>
     *
     * <p>In the case of {@link #FULL_FLUSH} or {@link #SYNC_FLUSH}, if
     * the return value is {@code len}, the space available in output
     * buffer {@code b}, this method should be invoked again with the same
     * {@code flush} parameter and more output space. Make sure that
     * {@code len} is greater than 6 to avoid flush marker (5 bytes) being
     * repeatedly output to the output buffer every time this method is
     * invoked.
     *
     * <p>If the {@link #setInput(ByteBuffer)} method was called to provide a buffer
     * for input, the input buffer's position will be advanced by the number of bytes
     * consumed by this operation.
     *
     * @param output the buffer for the compressed data
     * @param off    the start offset of the data
     * @param len    the maximum number of bytes of compressed data
     * @param flush  the compression flush mode
     *
     * @return the actual number of bytes of compressed data written to the output buffer
     *
     * @throws IllegalArgumentException if the flush mode is invalid
     * @since 1.7
     */
    // 向压缩缓冲区output的指定范围填充压缩后的数据，刷新模式由flush指定，返回实际填充的字节数
    public int deflate(byte[] output, int off, int len, int flush) {
        if(off<0 || len<0 || off>output.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        
        if(flush != NO_FLUSH && flush != SYNC_FLUSH && flush != FULL_FLUSH) {
            throw new IllegalArgumentException();
        }
        
        synchronized(zsRef) {
            ensureOpen();
            
            ByteBuffer input = this.input;
            
            // 如果需要使用压缩模式：FINISH
            if(finish) {
                // disregard given flush mode in this case
                flush = FINISH;
            }
            
            int params;
            
            if(setParams) {
                // bit 0: true to set params
                // bit 1-2: strategy (0, 1, or 2)
                // bit 3-31: level (0..9 or -1)
                params = 1 | strategy << 1 | level << 3;
            } else {
                params = 0;
            }
            
            int inputPos;
            long result;
            
            // 2号缓冲区为空
            if(input == null) {
                // 获取1号缓冲区的读游标
                inputPos = this.inputPos;
                
                // 将1号缓冲区中的数据压缩后存入压缩缓冲区output
                result = deflateBytesBytes(zsRef.address(), inputArray, inputPos, inputLim - inputPos, output, off, len, flush, params);
                
                // 2号缓冲区不为空
            } else {
                // 获取2号缓冲区的读游标
                inputPos = input.position();
                // 获取2号缓冲区中剩余的字节数量
                int inputRem = Math.max(input.limit() - inputPos, 0);
                
                // 2号缓冲区是直接缓冲区
                if(input.isDirect()) {
                    try {
                        // 获取2号缓冲区起始地址
                        long inputAddress = ((DirectBuffer) input).address();
                        
                        // 将2号缓冲区地址处缓存的数据压缩后存入压缩缓冲区output
                        result = deflateBufferBytes(zsRef.address(), inputAddress + inputPos, inputRem, output, off, len, flush, params);
                    } finally {
                        Reference.reachabilityFence(input);
                    }
                    
                    // 2号缓冲区不是直接缓冲区
                } else {
                    // 获取2号缓冲区中所有字节
                    byte[] inputArray = ZipUtils.getBufferArray(input);
                    // 获取2号缓冲区起始地址
                    int inputOffset = ZipUtils.getBufferOffset(input);
                    
                    // 将2号缓冲区中的数据压缩后存入压缩缓冲区output
                    result = deflateBytesBytes(zsRef.address(), inputArray, inputOffset + inputPos, inputRem, output, off, len, flush, params);
                }
            }
            
            // 读取了多少压缩前的字节数
            int read = (int) (result & 0x7fff_ffffL);
            
            // 写入了多少压缩后的字节数
            int written = (int) (result >>> 31 & 0x7fff_ffffL);
            
            if((result >>> 62 & 1) != 0) {
                finished = true;    // 标记完成压缩
            }
            
            if(params != 0 && (result >>> 63 & 1) == 0) {
                setParams = false;
            }
            
            // 如果2号缓冲区不为空
            if(input != null) {
                // 游标前进，跳过已读的压缩前的字节
                input.position(inputPos + read);
                
                // 2号缓冲区为空时，需要处理1号缓冲区的游标
            } else {
                // 游标前进，跳过已读的压缩前的字节
                this.inputPos = inputPos + read;
            }
            
            bytesWritten += written;
            bytesRead += read;
            
            return written;
        }
    }
    
    /**
     * Compresses the input data and fills specified buffer with compressed
     * data. Returns actual number of bytes of compressed data. A return value
     * of 0 indicates that {@link #needsInput() needsInput} should be called
     * in order to determine if more input data is required.
     *
     * <p>This method uses {@link #NO_FLUSH} as its compression flush mode.
     * An invocation of this method of the form {@code deflater.deflate(output)}
     * yields the same result as the invocation of
     * {@code deflater.deflate(output, Deflater.NO_FLUSH)}.
     *
     * @param output the buffer for the compressed data
     *
     * @return the actual number of bytes of compressed data written to the output buffer
     *
     * @since 11
     */
    // 向压缩缓冲区output的可用范围填充压缩后的数据，刷新模式为NO_FLUSH，返回实际填充的字节数
    public int deflate(ByteBuffer output) {
        return deflate(output, NO_FLUSH);
    }
    
    /**
     * Compresses the input data and fills the specified buffer with compressed
     * data. Returns actual number of bytes of data compressed.
     *
     * <p>Compression flush mode is one of the following three modes:
     *
     * <ul>
     * <li>{@link #NO_FLUSH}: allows the deflater to decide how much data
     * to accumulate, before producing output, in order to achieve the best
     * compression (should be used in normal use scenario). A return value
     * of 0 in this flush mode indicates that {@link #needsInput()} should
     * be called in order to determine if more input data is required.
     *
     * <li>{@link #SYNC_FLUSH}: all pending output in the deflater is flushed,
     * to the specified output buffer, so that an inflater that works on
     * compressed data can get all input data available so far (In particular
     * the {@link #needsInput()} returns {@code true} after this invocation
     * if enough output space is provided). Flushing with {@link #SYNC_FLUSH}
     * may degrade compression for some compression algorithms and so it
     * should be used only when necessary.
     *
     * <li>{@link #FULL_FLUSH}: all pending output is flushed out as with
     * {@link #SYNC_FLUSH}. The compression state is reset so that the inflater
     * that works on the compressed output data can restart from this point
     * if previous compressed data has been damaged or if random access is
     * desired. Using {@link #FULL_FLUSH} too often can seriously degrade
     * compression.
     * </ul>
     *
     * <p>In the case of {@link #FULL_FLUSH} or {@link #SYNC_FLUSH}, if
     * the return value is equal to the {@linkplain ByteBuffer#remaining() remaining space}
     * of the buffer, this method should be invoked again with the same
     * {@code flush} parameter and more output space. Make sure that
     * the buffer has at least 6 bytes of remaining space to avoid the
     * flush marker (5 bytes) being repeatedly output to the output buffer
     * every time this method is invoked.
     *
     * <p>On success, the position of the given {@code output} byte buffer will be
     * advanced by as many bytes as were produced by the operation, which is equal
     * to the number returned by this method.
     *
     * <p>If the {@link #setInput(ByteBuffer)} method was called to provide a buffer
     * for input, the input buffer's position will be advanced by the number of bytes
     * consumed by this operation.
     *
     * @param output the buffer for the compressed data
     * @param flush  the compression flush mode
     *
     * @return the actual number of bytes of compressed data written to the output buffer
     *
     * @throws IllegalArgumentException if the flush mode is invalid
     * @since 11
     */
    // 向压缩缓冲区output的可用范围填充压缩后的数据，刷新模式由flush指定，返回实际填充的字节数
    public int deflate(ByteBuffer output, int flush) {
        if(output.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        
        if(flush != NO_FLUSH && flush != SYNC_FLUSH && flush != FULL_FLUSH) {
            throw new IllegalArgumentException();
        }
        
        synchronized(zsRef) {
            ensureOpen();
            
            ByteBuffer input = this.input;
            
            // 如果需要使用压缩模式：FINISH
            if(finish) {
                // disregard given flush mode in this case
                flush = FINISH;
            }
            
            int params;
            
            if(setParams) {
                // bit 0: true to set params
                // bit 1-2: strategy (0, 1, or 2)
                // bit 3-31: level (0..9 or -1)
                params = 1 | strategy << 1 | level << 3;
            } else {
                params = 0;
            }
            
            int outputPos = output.position();
            int outputRem = Math.max(output.limit() - outputPos, 0);
            int inputPos;
            long result;
            
            // 2号缓冲区为空
            if(input == null) {
                // 1号缓冲区的读游标
                inputPos = this.inputPos;
                
                // 如果压缩缓冲区output是直接缓冲区
                if(output.isDirect()) {
                    // 获取压缩缓冲区output起始地址
                    long outputAddress = ((DirectBuffer) output).address();
                    
                    try {
                        // 将1号缓冲区中的数据压缩后存入压缩缓冲区地址处的存储中
                        result = deflateBytesBuffer(zsRef.address(), inputArray, inputPos, inputLim - inputPos, outputAddress + outputPos, outputRem, flush, params);
                    } finally {
                        Reference.reachabilityFence(output);
                    }
                    
                    // 压缩缓冲区不是直接缓冲区
                } else {
                    byte[] outputArray = ZipUtils.getBufferArray(output);
                    int outputOffset = ZipUtils.getBufferOffset(output);
                    
                    // 将1号缓冲区中的数据压缩后存入压缩缓冲区output
                    result = deflateBytesBytes(zsRef.address(), inputArray, inputPos, inputLim - inputPos, outputArray, outputOffset + outputPos, outputRem, flush, params);
                }
                
                // 2号缓冲区不为空
            } else {
                inputPos = input.position();
                int inputRem = Math.max(input.limit() - inputPos, 0);
                
                // 2号缓冲区是直接缓冲区
                if(input.isDirect()) {
                    // 获取2号缓冲区起始地址
                    long inputAddress = ((DirectBuffer) input).address();
                    
                    try {
                        // 压缩缓冲区是直接缓冲区
                        if(output.isDirect()) {
                            // 获取压缩缓冲区起始地址
                            long outputAddress = outputPos + ((DirectBuffer) output).address();
                            
                            try {
                                // 将2号缓冲区地址处缓存的数据压缩后存入压缩缓冲区地址处的存储中
                                result = deflateBufferBuffer(zsRef.address(), inputAddress + inputPos, inputRem, outputAddress, outputRem, flush, params);
                            } finally {
                                Reference.reachabilityFence(output);
                            }
                            
                            // 压缩缓冲区不是直接缓冲区
                        } else {
                            byte[] outputArray = ZipUtils.getBufferArray(output);
                            int outputOffset = ZipUtils.getBufferOffset(output);
                            
                            // 将2号缓冲区地址处缓存的数据压缩后存入压缩缓冲区outputArray
                            result = deflateBufferBytes(zsRef.address(), inputAddress + inputPos, inputRem, outputArray, outputOffset + outputPos, outputRem, flush, params);
                        }
                    } finally {
                        Reference.reachabilityFence(input);
                    }
                    
                    // 2号缓冲区不是直接缓冲区
                } else {
                    byte[] inputArray = ZipUtils.getBufferArray(input);
                    int inputOffset = ZipUtils.getBufferOffset(input);
                    
                    // 压缩缓冲区是直接缓冲区
                    if(output.isDirect()) {
                        long outputAddress = ((DirectBuffer) output).address();
                        
                        try {
                            // 将2号缓冲区中的数据压缩后存入压缩缓冲区地址处的存储中
                            result = deflateBytesBuffer(zsRef.address(), inputArray, inputOffset + inputPos, inputRem, outputAddress + outputPos, outputRem, flush, params);
                        } finally {
                            Reference.reachabilityFence(output);
                        }
                        
                        // 压缩缓冲区不是直接缓冲区
                    } else {
                        byte[] outputArray = ZipUtils.getBufferArray(output);
                        int outputOffset = ZipUtils.getBufferOffset(output);
                        
                        // 将2号缓冲区中的数据压缩后存入压缩缓冲区output
                        result = deflateBytesBytes(zsRef.address(), inputArray, inputOffset + inputPos, inputRem, outputArray, outputOffset + outputPos, outputRem, flush, params);
                    }
                }
            }
            
            // 读取了多少压缩前的字节数
            int read = (int) (result & 0x7fff_ffffL);
            
            // 写入了多少压缩后的字节数
            int written = (int) (result >>> 31 & 0x7fff_ffffL);
            
            if((result >>> 62 & 1) != 0) {
                finished = true;    // 标记完成压缩
            }
            
            if(params != 0 && (result >>> 63 & 1) == 0) {
                setParams = false;
            }
            
            // 如果2号缓冲区不为空
            if(input != null) {
                // 游标前进，跳过已读的压缩前的字节
                input.position(inputPos + read);
                
                // 2号缓冲区为空时，需要处理1号缓冲区的游标
            } else {
                // 游标前进，跳过已读的压缩前的字节
                this.inputPos = inputPos + read;
            }
            
            // 处理压缩缓冲区的游标
            output.position(outputPos + written);
            
            bytesWritten += written;
            bytesRead += read;
            
            return written;
        }
    }
    
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ set ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets input data for compression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     * <p>
     *
     * @param input the input data bytes
     *
     * @see Deflater#needsInput
     */
    // (使用1号缓冲区)设置待压缩数据(input中所有数据)
    public void setInput(byte[] input) {
        setInput(input, 0, input.length);
    }
    
    /**
     * Sets input data for compression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     * <p>
     *
     * @param input the input data bytes
     * @param off   the start offset of the data
     * @param len   the length of the data
     *
     * @see Deflater#needsInput
     */
    // (使用1号缓冲区)设置待压缩数据(input中off处起的len个字节)
    public void setInput(byte[] input, int off, int len) {
        if(off<0 || len<0 || off>input.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        
        synchronized(zsRef) {
            this.input = null;
            this.inputArray = input;
            this.inputPos = off;
            this.inputLim = off + len;
        }
    }
    
    /**
     * Sets input data for compression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     * <p>
     * The given buffer's position will be advanced as deflate
     * operations are performed, up to the buffer's limit.
     * The input buffer may be modified (refilled) between deflate
     * operations; doing so is equivalent to creating a new buffer
     * and setting it with this method.
     * <p>
     * Modifying the input buffer's contents, position, or limit
     * concurrently with an deflate operation will result in
     * undefined behavior, which may include incorrect operation
     * results or operation failure.
     *
     * @param input the input data bytes
     *
     * @see Deflater#needsInput
     * @since 11
     */
    // (使用2号缓冲区)设置待压缩数据(input中所有数据)
    public void setInput(ByteBuffer input) {
        Objects.requireNonNull(input);
        
        synchronized(zsRef) {
            this.input = input;
            this.inputArray = null;
        }
    }
    
    /**
     * Sets preset dictionary for compression.
     * A preset dictionary is used when the history buffer can be predetermined.
     * When the data is later uncompressed with Inflater.inflate(), Inflater.getAdler()
     * can be called in order to get the Adler-32 value of the dictionary required for decompression.
     *
     * @param dictionary the dictionary data bytes
     *
     * @see Inflater#inflate
     * @see Inflater#getAdler
     */
    // 设置字典数据
    public void setDictionary(byte[] dictionary) {
        setDictionary(dictionary, 0, dictionary.length);
    }
    
    /**
     * Sets preset dictionary for compression.
     * A preset dictionary is used when the history buffer can be predetermined.
     * When the data is later uncompressed with Inflater.inflate(), Inflater.getAdler()
     * can be called in order to get the Adler-32 value of the dictionary required for decompression.
     *
     * @param dictionary the dictionary data bytes
     * @param off        the start offset of the data
     * @param len        the length of the data
     *
     * @see Inflater#inflate
     * @see Inflater#getAdler
     */
    // 设置字典数据
    public void setDictionary(byte[] dictionary, int off, int len) {
        if(off<0 || len<0 || off>dictionary.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        
        synchronized(zsRef) {
            ensureOpen();
            setDictionary(zsRef.address(), dictionary, off, len);
        }
    }
    
    /**
     * Sets preset dictionary for compression.
     * A preset dictionary is used when the history buffer can be predetermined.
     * When the data is later uncompressed with Inflater.inflate(), Inflater.getAdler()
     * can be called in order to get the Adler-32 value of the dictionary required for decompression.
     *
     * The bytes in given byte buffer will be fully consumed by this method.
     * On return, its position will equal its limit.
     *
     * @param dictionary the dictionary data bytes
     *
     * @see Inflater#inflate
     * @see Inflater#getAdler
     */
    // 设置字典数据
    public void setDictionary(ByteBuffer dictionary) {
        synchronized(zsRef) {
            ensureOpen();
            
            int position = dictionary.position();
            int remaining = Math.max(dictionary.limit() - position, 0);
            
            if(dictionary.isDirect()) {
                long address = ((DirectBuffer) dictionary).address();
                try {
                    setDictionaryBuffer(zsRef.address(), address + position, remaining);
                } finally {
                    Reference.reachabilityFence(dictionary);
                }
            } else {
                byte[] array = ZipUtils.getBufferArray(dictionary);
                int offset = ZipUtils.getBufferOffset(dictionary);
                setDictionary(zsRef.address(), array, offset + position, remaining);
            }
            
            dictionary.position(position + remaining);
        }
    }
    
    /**
     * Sets the compression strategy to the specified value.
     *
     * <p> If the compression strategy is changed, the next invocation
     * of {@code deflate} will compress the input available so far with
     * the old strategy (and may be flushed); the new strategy will take
     * effect only after that invocation.
     *
     * @param strategy the new compression strategy
     *
     * @throws IllegalArgumentException if the compression strategy is
     *                                  invalid
     */
    // 设置压缩策略
    public void setStrategy(int strategy) {
        switch(strategy) {
            case DEFAULT_STRATEGY:
            case FILTERED:
            case HUFFMAN_ONLY:
                break;
            default:
                throw new IllegalArgumentException();
        }
        
        synchronized(zsRef) {
            if(this.strategy != strategy) {
                this.strategy = strategy;
                setParams = true;
            }
        }
    }
    
    /**
     * Sets the compression level to the specified value.
     *
     * <p> If the compression level is changed, the next invocation
     * of {@code deflate} will compress the input available so far
     * with the old level (and may be flushed); the new level will
     * take effect only after that invocation.
     *
     * @param level the new compression level (0-9)
     *
     * @throws IllegalArgumentException if the compression level is invalid
     */
    // 设置压缩级别
    public void setLevel(int level) {
        if((level<0 || level>9) && level != DEFAULT_COMPRESSION) {
            throw new IllegalArgumentException("invalid compression level");
        }
        
        synchronized(zsRef) {
            if(this.level != level) {
                this.level = level;
                setParams = true;
            }
        }
    }
    
    /**
     * When called, indicates that compression should end with the current contents of the input buffer.
     */
    // 设置刷新模式为FINISH
    public void finish() {
        synchronized(zsRef) {
            finish = true;
        }
    }
    
    /*▲ set ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the total number of uncompressed bytes input so far.
     *
     * <p>Since the number of bytes may be greater than
     * Integer.MAX_VALUE, the {@link #getBytesRead()} method is now
     * the preferred means of obtaining this information.</p>
     *
     * @return the total number of uncompressed bytes input so far
     */
    // 返回累计读取了多少压缩前的字节
    public int getTotalIn() {
        return (int) getBytesRead();
    }
    
    /**
     * Returns the total number of compressed bytes output so far.
     *
     * <p>Since the number of bytes may be greater than
     * Integer.MAX_VALUE, the {@link #getBytesWritten()} method is now
     * the preferred means of obtaining this information.</p>
     *
     * @return the total number of compressed bytes output so far
     */
    // 返回累计写入了多少压缩后的字节
    public int getTotalOut() {
        return (int) getBytesWritten();
    }
    
    /**
     * Returns the total number of uncompressed bytes input so far.
     *
     * @return the total (non-negative) number of uncompressed bytes input so far
     *
     * @since 1.5
     */
    // 返回累计读取了多少压缩前的字节
    public long getBytesRead() {
        synchronized(zsRef) {
            ensureOpen();
            return bytesRead;
        }
    }
    
    /**
     * Returns the total number of compressed bytes output so far.
     *
     * @return the total (non-negative) number of compressed bytes output so far
     *
     * @since 1.5
     */
    // 返回累计写入了多少压缩后的字节
    public long getBytesWritten() {
        synchronized(zsRef) {
            ensureOpen();
            return bytesWritten;
        }
    }
    
    /**
     * Returns the ADLER-32 value of the uncompressed data.
     *
     * @return the ADLER-32 value of the uncompressed data
     */
    // 返回压缩过程中用到的一些附加信息在本地内存中的地址的ADLER-32值
    public int getAdler() {
        synchronized(zsRef) {
            ensureOpen();
            
            return getAdler(zsRef.address());
        }
    }
    
    /*▲ get ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns true if no data remains in the input buffer.
     * This can be used to determine if one of the {@code setInput()} methods should be called in order to provide more input.
     *
     * @return true if the input data buffer is empty and setInput() should be called in order to provide more input
     */
    /*
     * 判断是否需要输入更多数据
     *
     * 如果内部缓冲区中没有数据，则返回true，表示需要更多数据。
     * 如果内部缓冲区中已有数据，则返回false，表示不需要更多数据。
     * 这可用于确定是否应调用setInput()方法以提供更多输入。
     */
    public boolean needsInput() {
        synchronized(zsRef) {
            ByteBuffer input = this.input;
            return input == null
                ? inputLim == inputPos      // 如果2号缓冲区为空，则验证1号缓冲区是否为空
                : !input.hasRemaining();    // 如果2号缓冲区不为空，则验证2号缓冲区中是否有更多数据
        }
    }
    
    /**
     * Returns true if the end of the compressed data output stream has been reached.
     *
     * @return true if the end of the compressed data output stream has been reached
     */
    // 判断压缩器是否已经完成压缩
    public boolean finished() {
        synchronized(zsRef) {
            return finished;
        }
    }
    
    /**
     * Resets deflater so that a new set of input data can be processed.
     * Keeps current compression level and strategy settings.
     */
    // 重置压缩器，以便接收新数据进行压缩。压缩级别和压缩策略参数会被保留
    public void reset() {
        synchronized(zsRef) {
            ensureOpen();
            reset(zsRef.address());
            finish = false;
            finished = false;
            input = ZipUtils.defaultBuf;
            inputArray = null;
            bytesRead = bytesWritten = 0;
        }
    }
    
    /**
     * Closes the compressor and discards any unprocessed input.
     *
     * This method should be called when the compressor is no longer being used.
     * Once this method is called, the behavior of the Deflater object is undefined.
     */
    /*
     * 关闭压缩器(本地)并丢弃所有未处理的输入。
     * 当不再使用压缩器时，应调用此方法。
     * 调用此方法后，Deflater对象的行为处于未定义状态。
     */
    public void end() {
        synchronized(zsRef) {
            zsRef.clean();
            input = ZipUtils.defaultBuf;
        }
    }
    
    /**
     * Closes the compressor when garbage is collected.
     *
     * @deprecated The {@code finalize} method has been deprecated and will be
     * removed. It is implemented as a no-op. Subclasses that override
     * {@code finalize} in order to perform cleanup should be modified to use
     * alternative cleanup mechanisms and to remove the overriding {@code finalize}
     * method. The recommended cleanup for compressor is to explicitly call
     * {@code end} method when it is no longer in use. If the {@code end} is
     * not invoked explicitly the resource of the compressor will be released
     * when the instance becomes unreachable.
     */
    @Deprecated(since = "9", forRemoval = true)
    protected void finalize() {
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 确保压缩器(本地)处于工作状态
    private void ensureOpen() {
        // 判断是否只有当前线程持有zsRef锁
        assert Thread.holdsLock(zsRef);
        
        if(zsRef.address() == 0) {
            throw new NullPointerException("Deflater has been closed");
        }
    }
    
    // 将字节数组inputArray中的数据压缩后存入压缩缓冲区outputArray
    private native long deflateBytesBytes(long addr, byte[] inputArray, int inputOff, int inputLen, byte[] outputArray, int outputOff, int outputLen, int flush, int params);
    // 将字节数组inputArray中的数据压缩后存入outputAddress地址处的压缩缓冲区
    private native long deflateBytesBuffer(long addr, byte[] inputArray, int inputOff, int inputLen, long outputAddress, int outputLen, int flush, int params);
    // 将地址inputAddress处缓存的数据压缩后存入压缩缓冲区outputArray
    private native long deflateBufferBytes(long addr, long inputAddress, int inputLen, byte[] outputArray, int outputOff, int outputLen, int flush, int params);
    // 将地址inputAddress处缓存的数据压缩后存入outputAddress地址处的压缩缓冲区
    private native long deflateBufferBuffer(long addr, long inputAddress, int inputLen, long outputAddress, int outputLen, int flush, int params);
    private static native long init(int level, int strategy, boolean nowrap);
    private static native void setDictionary(long addr, byte[] b, int off, int len);
    private static native void setDictionaryBuffer(long addr, long bufAddress, int len);
    private static native int getAdler(long addr);
    private static native void reset(long addr);
    private static native void end(long addr);
    
    
    
    
    
    
    /**
     * A reference to the native zlib's z_stream structure. It also
     * serves as the "cleaner" to clean up the native resource when
     * the Deflater is ended, closed or cleaned.
     */
    // 资源清理器
    static class DeflaterZStreamRef implements Runnable {
        private final Cleanable cleanable;  // 清理器
        private long address;               // 待清理资源的地址
        
        private DeflaterZStreamRef(Deflater owner, long addr) {
            // 清理器工厂
            Cleaner cleaner = CleanerFactory.cleaner();
            // 如果owner不为null，则向Cleaner注册跟踪的对象owner和清理动作clean-->run
            this.cleanable = (owner != null) ? cleaner.register(owner, this) : null;
            this.address = addr;
        }
        
        // 由清理器Cleanable最终调用，清理本地内存中的资源
        void clean() {
            cleanable.clean();
        }
        
        // 由clean()回调
        public synchronized void run() {
            long addr = address;
            address = 0;
            if(addr != 0) {
                end(addr);
            }
        }
        
        long address() {
            return address;
        }
        
        /**
         * If {@code Deflater} has been subclassed and the {@code end} method is overridden,
         * uses {@code finalizer} mechanism for resource cleanup.
         * So {@code end} method can be called when the {@code Deflater} is unreachable.
         * This mechanism will be removed when the {@code finalize} method is removed from {@code Deflater}.
         */
        /*
         * 如果Deflater被子类化，且end方法被覆盖，则使用finalizer机制进行资源清理。
         * 否则，使用清理器Cleanable这种资源清理机制
         */
        static DeflaterZStreamRef get(Deflater owner, long addr) {
            Class<?> clz = owner.getClass();
            
            // 如果clz是Deflater子类
            while(clz != Deflater.class) {
                try {
                    // 获取当前类中名为"end"的无参方法，但不包括父类/父接口中的方法
                    clz.getDeclaredMethod("end");
                    return new FinalizableZStreamRef(owner, addr);
                } catch(NoSuchMethodException nsme) {
                }
                
                // 如果该子类中不包含end方法，则向父类查找
                clz = clz.getSuperclass();
            }
            
            // 至此，说明在子类中未找到end方法
            return new DeflaterZStreamRef(owner, addr);
        }
        
        private static class FinalizableZStreamRef extends DeflaterZStreamRef {
            final Deflater owner;
            
            FinalizableZStreamRef(Deflater owner, long addr) {
                super(null, addr);
                this.owner = owner;
            }
            
            @Override
            @SuppressWarnings("deprecation")
            protected void finalize() {
                owner.end();    // 调用到clean()方法
            }
            
            @Override
            void clean() {
                run();          // 执行run动作
            }
        }
    }
}
