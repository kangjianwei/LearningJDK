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
 * This class provides support for general purpose decompression using the
 * popular ZLIB compression library. The ZLIB compression library was
 * initially developed as part of the PNG graphics standard and is not
 * protected by patents. It is fully described in the specifications at
 * the <a href="package-summary.html#package.description">java.util.zip
 * package description</a>.
 * <p>
 * This class inflates sequences of ZLIB compressed bytes. The input byte
 * sequence is provided in either byte array or byte buffer, via one of the
 * {@code setInput()} methods. The output byte sequence is written to the
 * output byte array or byte buffer passed to the {@code deflate()} methods.
 * <p>
 * The following code fragment demonstrates a trivial compression
 * and decompression of a string using {@code Deflater} and
 * {@code Inflater}.
 *
 * <blockquote><pre>
 * try {
 *     // Encode a String into bytes
 *     String inputString = "blahblahblah\u20AC\u20AC";
 *     byte[] input = inputString.getBytes("UTF-8");
 *
 *     // Compress the bytes
 *     byte[] output = new byte[100];
 *     Deflater compresser = new Deflater();
 *     compresser.setInput(input);
 *     compresser.finish();
 *     int compressedDataLength = compresser.deflate(output);
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
 * To release resources used by this {@code Inflater}, the {@link #end()} method
 * should be called explicitly. Subclasses are responsible for the cleanup of resources
 * acquired by the subclass. Subclasses that override {@link #finalize()} in order
 * to perform cleanup should be modified to use alternative cleanup mechanisms such
 * as {@link java.lang.ref.Cleaner} and remove the overriding {@code finalize} method.
 *
 * @implSpec
 * If this {@code Inflater} has been subclassed and the {@code end} method has been
 * overridden, the {@code end} method will be called by the finalization when the
 * inflater is unreachable. But the subclasses should not depend on this specific
 * implementation; the finalization is not reliable and the {@code finalize} method
 * is deprecated to be removed.
 *
 * @see         Deflater
 * @author      David Connelly
 * @since 1.1
 *
 */
// 解压器
public class Inflater {
    
    private final InflaterZStreamRef zsRef; // 资源清理器
    
    // 存储待解压数据(可能会混杂不需要解压的数据)
    private byte[] inputArray;          // 1号缓冲区
    private int inputPos, inputLim;     // 1号缓冲区的读游标与未处理字节数量
    
    // 存储待解压数据(可能会混杂不需要解压的数据)
    private ByteBuffer input = ZipUtils.defaultBuf; // 2号缓冲区，如果存在，会被优先处理
    
    private boolean finished;   // 是否完成解压。完成解压之时，缓冲区中可能还有一些不必解压的附加信息
    
    private long bytesRead;     // 累计读取了多少解压前的字节
    private long bytesWritten;  // 累计写入了多少解压后的字节
    
    /*
     * These fields are used as an "out" parameter from JNI
     * when a DataFormatException is thrown during the inflate operation.
     */
    private int inputConsumed;  // 由JNI层设置，记录已读取的解压前的数据量
    private int outputConsumed; // 由JNI层设置，记录已写入的解压后的数据量
    
    /*
     * 是否需要字典
     *
     * 如果待解压数据解压时需要用到字典，
     * 但是未提前给解压器设置字典，那么needDict将被设置为true。
     * 如果needDict为false，说明已经提前设置好了字典，
     * 或者解压这些数据根本用不到字典。
     */
    private boolean needDict;
    
    
    
    static {
        ZipUtils.loadLibrary();
        initIDs();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new decompressor.
     */
    // 构造解压器(不兼容GZIP)
    public Inflater() {
        this(false);
    }
    
    /**
     * Creates a new decompressor. If the parameter 'nowrap' is true then
     * the ZLIB header and checksum fields will not be used. This provides
     * compatibility with the compression format used by both GZIP and PKZIP.
     * <p>
     * Note: When using the 'nowrap' option it is also necessary to provide
     * an extra "dummy" byte as input. This is required by the ZLIB native
     * library in order to support certain optimizations.
     *
     * @param nowrap if true then support GZIP compatible compression
     */
    // 构造解压器，nowrap指示是否兼容GZIP
    public Inflater(boolean nowrap) {
        this.zsRef = InflaterZStreamRef.get(this, init(nowrap));
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 解压 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Uncompresses bytes into specified buffer. Returns actual number
     * of bytes uncompressed. A return value of 0 indicates that
     * needsInput() or needsDictionary() should be called in order to
     * determine if more input data or a preset dictionary is required.
     * In the latter case, getAdler() can be used to get the Adler-32
     * value of the dictionary required.
     * <p>
     * The {@linkplain #getRemaining() remaining byte count} will be reduced by
     * the number of consumed input bytes.  If the {@link #setInput(ByteBuffer)}
     * method was called to provide a buffer for input, the input buffer's position
     * will be advanced the number of consumed bytes.
     * <p>
     * These byte totals, as well as
     * the {@linkplain #getBytesRead() total bytes read}
     * and the {@linkplain #getBytesWritten() total bytes written}
     * values, will be updated even in the event that a {@link DataFormatException}
     * is thrown to reflect the amount of data consumed and produced before the
     * exception occurred.
     *
     * @param output the buffer for the uncompressed data
     *
     * @return the actual number of uncompressed bytes
     *
     * @throws DataFormatException if the compressed data format is invalid
     * @see Inflater#needsInput
     * @see Inflater#needsDictionary
     */
    // 向解压缓冲区output中填充解压后的数据，返回实际填充的字节数
    public int inflate(byte[] output) throws DataFormatException {
        return inflate(output, 0, output.length);
    }
    
    /**
     * Uncompresses bytes into specified buffer. Returns actual number
     * of bytes uncompressed. A return value of 0 indicates that
     * needsInput() or needsDictionary() should be called in order to
     * determine if more input data or a preset dictionary is required.
     * In the latter case, getAdler() can be used to get the Adler-32
     * value of the dictionary required.
     * <p>
     * If the {@link #setInput(ByteBuffer)} method was called to provide a buffer
     * for input, the input buffer's position will be advanced by the number of bytes
     * consumed by this operation, even in the event that a {@link DataFormatException}
     * is thrown.
     * <p>
     * The {@linkplain #getRemaining() remaining byte count} will be reduced by
     * the number of consumed input bytes.  If the {@link #setInput(ByteBuffer)}
     * method was called to provide a buffer for input, the input buffer's position
     * will be advanced the number of consumed bytes.
     * <p>
     * These byte totals, as well as
     * the {@linkplain #getBytesRead() total bytes read}
     * and the {@linkplain #getBytesWritten() total bytes written}
     * values, will be updated even in the event that a {@link DataFormatException}
     * is thrown to reflect the amount of data consumed and produced before the
     * exception occurred.
     *
     * @param output the buffer for the uncompressed data
     * @param off    the start offset of the data
     * @param len    the maximum number of uncompressed bytes
     *
     * @return the actual number of uncompressed bytes
     *
     * @throws DataFormatException if the compressed data format is invalid
     * @see Inflater#needsInput
     * @see Inflater#needsDictionary
     */
    // 向解压缓冲区output的指定范围填充解压后的数据，返回实际填充的字节数
    public int inflate(byte[] output, int off, int len) throws DataFormatException {
        if(off<0 || len<0 || off>output.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        
        synchronized(zsRef) {
            ensureOpen();
            
            ByteBuffer input = this.input;
            long result;
            int inputPos;
            
            try {
                // 2号缓冲区为空
                if(input == null) {
                    // 获取1号缓冲区的读游标
                    inputPos = this.inputPos;
                    
                    try {
                        // 将1号缓冲区中的数据解压后存入解压缓冲区output
                        result = inflateBytesBytes(zsRef.address(), inputArray, inputPos, inputLim - inputPos, output, off, len);
                    } catch(DataFormatException e) {
                        this.inputPos = inputPos + inputConsumed;
                        throw e;
                    }
                    
                    // 2号缓冲区不为空
                } else {
                    // 获取2号缓冲区的读游标
                    inputPos = input.position();
                    // 获取2号缓冲区中剩余的字节数量
                    int inputRem = Math.max(input.limit() - inputPos, 0);
                    
                    try {
                        // 2号缓冲区是直接缓冲区
                        if(input.isDirect()) {
                            try {
                                // 获取2号缓冲区起始地址
                                long inputAddress = ((DirectBuffer) input).address();
                                
                                // 将2号缓冲区地址处缓存的数据解压后存入解压缓冲区output
                                result = inflateBufferBytes(zsRef.address(), inputAddress + inputPos, inputRem, output, off, len);
                            } finally {
                                Reference.reachabilityFence(input);
                            }
                            
                            // 2号缓冲区不是直接缓冲区
                        } else {
                            // 获取2号缓冲区中所有字节
                            byte[] inputArray = ZipUtils.getBufferArray(input);
                            // 获取2号缓冲区起始地址
                            int inputOffset = ZipUtils.getBufferOffset(input);
                            
                            // 将2号缓冲区中的数据解压后存入解压缓冲区output
                            result = inflateBytesBytes(zsRef.address(), inputArray, inputOffset + inputPos, inputRem, output, off, len);
                        }
                    } catch(DataFormatException e) {
                        input.position(inputPos + inputConsumed);
                        throw e;
                    }
                }
            } catch(DataFormatException e) {
                bytesRead += inputConsumed;
                inputConsumed = 0;
                int written = outputConsumed;
                bytesWritten += written;
                outputConsumed = 0;
                throw e;
            }
            
            // 读取了多少解压前的字节数
            int read = (int) (result & 0x7fff_ffffL);
            
            // 写入了多少解压后的字节数
            int written = (int) (result >>> 31 & 0x7fff_ffffL);
            
            if((result >>> 62 & 1) != 0) {
                finished = true;    // 标记解压完成
            }
            
            if((result >>> 63 & 1) != 0) {
                needDict = true;    // 需要字典
            }
            
            // 如果2号缓冲区不为空
            if(input != null) {
                // 游标前进，跳过已读的解压前的字节
                input.position(inputPos + read);
                
                // 2号缓冲区为空时，需要处理1号缓冲区的游标
            } else {
                // 游标前进，跳过已读的解压前的字节
                this.inputPos = inputPos + read;
            }
            
            bytesWritten += written;
            bytesRead += read;
            
            return written;
        }
    }
    
    /**
     * Uncompresses bytes into specified buffer. Returns actual number
     * of bytes uncompressed. A return value of 0 indicates that
     * needsInput() or needsDictionary() should be called in order to
     * determine if more input data or a preset dictionary is required.
     * In the latter case, getAdler() can be used to get the Adler-32
     * value of the dictionary required.
     * <p>
     * On success, the position of the given {@code output} byte buffer will be
     * advanced by as many bytes as were produced by the operation, which is equal
     * to the number returned by this method.  Note that the position of the
     * {@code output} buffer will be advanced even in the event that a
     * {@link DataFormatException} is thrown.
     * <p>
     * The {@linkplain #getRemaining() remaining byte count} will be reduced by
     * the number of consumed input bytes.  If the {@link #setInput(ByteBuffer)}
     * method was called to provide a buffer for input, the input buffer's position
     * will be advanced the number of consumed bytes.
     * <p>
     * These byte totals, as well as
     * the {@linkplain #getBytesRead() total bytes read}
     * and the {@linkplain #getBytesWritten() total bytes written}
     * values, will be updated even in the event that a {@link DataFormatException}
     * is thrown to reflect the amount of data consumed and produced before the
     * exception occurred.
     *
     * @param output the buffer for the uncompressed data
     *
     * @return the actual number of uncompressed bytes
     *
     * @throws DataFormatException     if the compressed data format is invalid
     * @throws ReadOnlyBufferException if the given output buffer is read-only
     * @see Inflater#needsInput
     * @see Inflater#needsDictionary
     * @since 11
     */
    // 向解压缓冲区output的可用范围填充解压后的数据，返回实际填充的字节数
    public int inflate(ByteBuffer output) throws DataFormatException {
        if(output.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        
        synchronized(zsRef) {
            ensureOpen();
            
            ByteBuffer input = this.input;
            long result;
            int inputPos;
            int outputPos = output.position();
            int outputRem = Math.max(output.limit() - outputPos, 0);
            
            try {
                // 2号缓冲区为空
                if(input == null) {
                    // 1号缓冲区的读游标
                    inputPos = this.inputPos;
                    
                    try {
                        // 如果解压缓冲区output是直接缓冲区
                        if(output.isDirect()) {
                            // 获取解压缓冲区output起始地址
                            long outputAddress = ((DirectBuffer) output).address();
                            
                            try {
                                // 将1号缓冲区中的数据解压后存入解压缓冲区地址处的存储中
                                result = inflateBytesBuffer(zsRef.address(), inputArray, inputPos, inputLim - inputPos, outputAddress + outputPos, outputRem);
                            } finally {
                                Reference.reachabilityFence(output);
                            }
                            
                            // 解压缓冲区不是直接缓冲区
                        } else {
                            byte[] outputArray = ZipUtils.getBufferArray(output);
                            int outputOffset = ZipUtils.getBufferOffset(output);
                            
                            // 将1号缓冲区中的数据解压后存入解压缓冲区output
                            result = inflateBytesBytes(zsRef.address(), inputArray, inputPos, inputLim - inputPos, outputArray, outputOffset + outputPos, outputRem);
                        }
                    } catch(DataFormatException e) {
                        this.inputPos = inputPos + inputConsumed;
                        throw e;
                    }
                    
                    // 2号缓冲区不为空
                } else {
                    inputPos = input.position();
                    int inputRem = Math.max(input.limit() - inputPos, 0);
                    
                    try {
                        // 2号缓冲区是直接缓冲区
                        if(input.isDirect()) {
                            // 获取2号缓冲区起始地址
                            long inputAddress = ((DirectBuffer) input).address();
                            
                            try {
                                // 解压缓冲区是直接缓冲区
                                if(output.isDirect()) {
                                    // 获取解压缓冲区起始地址
                                    long outputAddress = ((DirectBuffer) output).address();
                                    
                                    try {
                                        // 将2号缓冲区地址处缓存的数据解压后存入解压缓冲区地址处的存储中
                                        result = inflateBufferBuffer(zsRef.address(), inputAddress + inputPos, inputRem, outputAddress + outputPos, outputRem);
                                    } finally {
                                        Reference.reachabilityFence(output);
                                    }
                                    
                                    // 解压缓冲区不是直接缓冲区
                                } else {
                                    byte[] outputArray = ZipUtils.getBufferArray(output);
                                    int outputOffset = ZipUtils.getBufferOffset(output);
                                    
                                    // 将2号缓冲区地址处缓存的数据解压后存入解压缓冲区outputArray
                                    result = inflateBufferBytes(zsRef.address(), inputAddress + inputPos, inputRem, outputArray, outputOffset + outputPos, outputRem);
                                }
                            } finally {
                                Reference.reachabilityFence(input);
                            }
                            
                            // 2号缓冲区不是直接缓冲区
                        } else {
                            byte[] inputArray = ZipUtils.getBufferArray(input);
                            int inputOffset = ZipUtils.getBufferOffset(input);
                            
                            // 解压缓冲区是直接缓冲区
                            if(output.isDirect()) {
                                long outputAddress = ((DirectBuffer) output).address();
                                
                                try {
                                    // 将2号缓冲区中的数据解压后存入解压缓冲区地址处的存储中
                                    result = inflateBytesBuffer(zsRef.address(), inputArray, inputOffset + inputPos, inputRem, outputAddress + outputPos, outputRem);
                                } finally {
                                    Reference.reachabilityFence(output);
                                }
                                
                                // 解压缓冲区不是直接缓冲区
                            } else {
                                byte[] outputArray = ZipUtils.getBufferArray(output);
                                int outputOffset = ZipUtils.getBufferOffset(output);
                                
                                // 将2号缓冲区中的数据解压后存入解压缓冲区output
                                result = inflateBytesBytes(zsRef.address(), inputArray, inputOffset + inputPos, inputRem, outputArray, outputOffset + outputPos, outputRem);
                            }
                        }
                    } catch(DataFormatException e) {
                        input.position(inputPos + inputConsumed);
                        throw e;
                    }
                }
            } catch(DataFormatException e) {
                bytesRead += inputConsumed;
                inputConsumed = 0;
                int written = outputConsumed;
                output.position(outputPos + written);
                bytesWritten += written;
                outputConsumed = 0;
                throw e;
            }
            
            // 读取了多少解压前的字节数
            int read = (int) (result & 0x7fff_ffffL);
            
            // 写入了多少解压后的字节数
            int written = (int) (result >>> 31 & 0x7fff_ffffL);
            
            if((result >>> 62 & 1) != 0) {
                finished = true;    // 标记解压完成
            }
            
            if((result >>> 63 & 1) != 0) {
                needDict = true;    // 需要字典
            }
            
            // 如果2号缓冲区不为空
            if(input != null) {
                // 游标前进，跳过已读的解压前的字节
                input.position(inputPos + read);
                
                // 2号缓冲区为空时，需要处理1号缓冲区的游标
            } else {
                // 游标前进，跳过已读的解压前的字节
                this.inputPos = inputPos + read;
            }
            
            /* Note: this method call also serves to keep the byteBuffer ref alive */
            // 处理解压缓冲区的游标
            output.position(outputPos + written);
            
            bytesWritten += written;
            bytesRead += read;
            
            return written;
        }
    }
    
    /*▲ 解压 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ set ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets input data for decompression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     *
     * @param input the input data bytes
     *
     * @see Inflater#needsInput
     */
    // (使用1号缓冲区)设置待解压数据(input中所有数据)
    public void setInput(byte[] input) {
        setInput(input, 0, input.length);
    }
    
    /**
     * Sets input data for decompression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     *
     * @param input the input data bytes
     * @param off   the start offset of the input data
     * @param len   the length of the input data
     *
     * @see Inflater#needsInput
     */
    // (使用1号缓冲区)设置待解压数据(input中off处起的len个字节)
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
     * Sets input data for decompression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     * <p>
     * The given buffer's position will be advanced as inflate
     * operations are performed, up to the buffer's limit.
     * The input buffer may be modified (refilled) between inflate
     * operations; doing so is equivalent to creating a new buffer
     * and setting it with this method.
     * <p>
     * Modifying the input buffer's contents, position, or limit
     * concurrently with an inflate operation will result in
     * undefined behavior, which may include incorrect operation
     * results or operation failure.
     *
     * @param input the input data bytes
     *
     * @see Inflater#needsInput
     * @since 11
     */
    // (使用2号缓冲区)设置待解压数据(input中所有数据)
    public void setInput(ByteBuffer input) {
        Objects.requireNonNull(input);
        
        synchronized(zsRef) {
            this.input = input;
            this.inputArray = null;
        }
    }
    
    /**
     * Sets the preset dictionary to the given array of bytes. Should be
     * called when inflate() returns 0 and needsDictionary() returns true
     * indicating that a preset dictionary is required. The method getAdler()
     * can be used to get the Adler-32 value of the dictionary needed.
     *
     * @param dictionary the dictionary data bytes
     *
     * @see Inflater#needsDictionary
     * @see Inflater#getAdler
     */
    // 设置字典数据
    public void setDictionary(byte[] dictionary) {
        setDictionary(dictionary, 0, dictionary.length);
    }
    
    /**
     * Sets the preset dictionary to the given array of bytes. Should be
     * called when inflate() returns 0 and needsDictionary() returns true
     * indicating that a preset dictionary is required. The method getAdler()
     * can be used to get the Adler-32 value of the dictionary needed.
     *
     * @param dictionary the dictionary data bytes
     * @param off        the start offset of the data
     * @param len        the length of the data
     *
     * @see Inflater#needsDictionary
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
            needDict = false;
        }
    }
    
    /**
     * Sets the preset dictionary to the bytes in the given buffer. Should be
     * called when inflate() returns 0 and needsDictionary() returns true
     * indicating that a preset dictionary is required. The method getAdler()
     * can be used to get the Adler-32 value of the dictionary needed.
     * <p>
     * The bytes in given byte buffer will be fully consumed by this method.  On
     * return, its position will equal its limit.
     *
     * @param dictionary the dictionary data bytes
     *
     * @see Inflater#needsDictionary
     * @see Inflater#getAdler
     * @since 11
     */
    // 设置字典数据
    public void setDictionary(ByteBuffer dictionary) {
        synchronized(zsRef) {
            int position = dictionary.position();
            int remaining = Math.max(dictionary.limit() - position, 0);
            
            ensureOpen();
            
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
            
            needDict = false;
        }
    }
    
    /*▲ set ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the total number of bytes remaining in the input buffer.
     * This can be used to find out what bytes still remain in the input
     * buffer after decompression has finished.
     *
     * @return the total number of bytes remaining in the input buffer
     */
    // 返回解压器内部缓冲区中剩余未处理的字节数量
    public int getRemaining() {
        synchronized(zsRef) {
            ByteBuffer input = this.input;
            return input == null ? inputLim - inputPos : input.remaining();
        }
    }
    
    /**
     * Returns the total number of compressed bytes input so far.
     *
     * <p>Since the number of bytes may be greater than
     * Integer.MAX_VALUE, the {@link #getBytesRead()} method is now
     * the preferred means of obtaining this information.</p>
     *
     * @return the total number of compressed bytes input so far
     */
    // 返回累计读取了多少解压前的字节
    public int getTotalIn() {
        return (int) getBytesRead();
    }
    
    /**
     * Returns the total number of uncompressed bytes output so far.
     *
     * <p>Since the number of bytes may be greater than
     * Integer.MAX_VALUE, the {@link #getBytesWritten()} method is now
     * the preferred means of obtaining this information.</p>
     *
     * @return the total number of uncompressed bytes output so far
     */
    // 返回累计写入了多少解压后的字节
    public int getTotalOut() {
        return (int) getBytesWritten();
    }
    
    /**
     * Returns the total number of compressed bytes input so far.
     *
     * @return the total (non-negative) number of compressed bytes input so far
     *
     * @since 1.5
     */
    // 返回累计读取了多少解压前的字节
    public long getBytesRead() {
        synchronized(zsRef) {
            ensureOpen();
            return bytesRead;
        }
    }
    
    /**
     * Returns the total number of uncompressed bytes output so far.
     *
     * @return the total (non-negative) number of uncompressed bytes output so far
     *
     * @since 1.5
     */
    // 返回累计写入了多少解压后的字节
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
    // 返回解压过程中用到的一些附加信息在本地内存中的地址的ADLER-32值
    public int getAdler() {
        synchronized(zsRef) {
            ensureOpen();
            return getAdler(zsRef.address());
        }
    }
    
    /*▲ get ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns true if no data remains in the input buffer. This can
     * be used to determine if one of the {@code setInput()} methods should be
     * called in order to provide more input.
     *
     * @return true if no data remains in the input buffer
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
     * Returns true if a preset dictionary is needed for decompression.
     *
     * @return true if a preset dictionary is needed for decompression
     *
     * @see Inflater#setDictionary
     */
    // 判断解压器是否需要字典
    public boolean needsDictionary() {
        synchronized(zsRef) {
            return needDict;
        }
    }
    
    /**
     * Returns true if the end of the compressed data stream has been
     * reached.
     *
     * @return true if the end of the compressed data stream has been
     * reached
     */
    // 判断解压器是否完成解压。完成解压之时，缓冲区中可能还有一些不必解压的附加信息
    public boolean finished() {
        synchronized(zsRef) {
            return finished;
        }
    }
    
    /**
     * Resets inflater so that a new set of input data can be processed.
     */
    // 重置解压器，以便解压器接收新数据进行解压
    public void reset() {
        synchronized(zsRef) {
            ensureOpen();
            reset(zsRef.address());
            input = ZipUtils.defaultBuf;
            inputArray = null;
            finished = false;
            needDict = false;
            bytesRead = bytesWritten = 0;
        }
    }
    
    /**
     * Closes the decompressor and discards any unprocessed input.
     *
     * This method should be called when the decompressor is no longer
     * being used. Once this method is called, the behavior of the
     * Inflater object is undefined.
     */
    /*
     * 关闭解压器(本地)并丢弃所有未处理的输入。
     * 当不再使用解压器时，应调用此方法。
     * 调用此方法后，Inflater对象的行为处于未定义状态。
     */
    public void end() {
        synchronized(zsRef) {
            zsRef.clean();
            input = ZipUtils.defaultBuf;
            inputArray = null;
        }
    }
    
    /**
     * Closes the decompressor when garbage is collected.
     *
     * @implSpec If this {@code Inflater} has been subclassed and the {@code end} method
     * has been overridden, the {@code end} method will be called when the
     * inflater is unreachable.
     * @deprecated The {@code finalize} method has been deprecated and will be
     * removed. It is implemented as a no-op. Subclasses that override
     * {@code finalize} in order to perform cleanup should be modified to use
     * alternative cleanup mechanisms and remove the overriding {@code finalize}
     * method. The recommended cleanup for compressor is to explicitly call
     * {@code end} method when it is no longer in use. If the {@code end} is
     * not invoked explicitly the resource of the compressor will be released
     * when the instance becomes unreachable,
     */
    @Deprecated(since = "9", forRemoval = true)
    protected void finalize() {
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 确保解压器(本地)处于工作状态
    private void ensureOpen() {
        assert Thread.holdsLock(zsRef);
        if(zsRef.address() == 0) {
            throw new NullPointerException("Inflater has been closed");
        }
    }
    
    // 将字节数组inputArray中的数据解压后存入解压缓冲区outputArray
    private native long inflateBytesBytes(long addr, byte[] inputArray, int inputOff, int inputLen, byte[] outputArray, int outputOff, int outputLen) throws DataFormatException;
    // 将字节数组inputArray中的数据解压后存入outputAddress地址处的解压缓冲区
    private native long inflateBytesBuffer(long addr, byte[] inputArray, int inputOff, int inputLen, long outputAddress, int outputLen) throws DataFormatException;
    // 将地址inputAddress处缓存的数据解压后存入解压缓冲区outputArray
    private native long inflateBufferBytes(long addr, long inputAddress, int inputLen, byte[] outputArray, int outputOff, int outputLen) throws DataFormatException;
    // 将地址inputAddress处缓存的数据解压后存入outputAddress地址处的解压缓冲区
    private native long inflateBufferBuffer(long addr, long inputAddress, int inputLen, long outputAddress, int outputLen) throws DataFormatException;
    private static native void initIDs();
    private static native long init(boolean nowrap);
    private static native void setDictionary(long addr, byte[] b, int off, int len);
    private static native void setDictionaryBuffer(long addr, long bufAddress, int len);
    private static native int getAdler(long addr);
    private static native void reset(long addr);
    private static native void end(long addr);
    
    
    
    
    
    
    /**
     * A reference to the native zlib's z_stream structure. It also
     * serves as the "cleaner" to clean up the native resource when
     * the Inflater is ended, closed or cleaned.
     */
    // 资源清理器
    static class InflaterZStreamRef implements Runnable {
        private final Cleanable cleanable;  // 清理器
        private long address;               // 待清理资源的地址
        
        private InflaterZStreamRef(Inflater owner, long addr) {
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
        
        /**
         * If {@code Inflater} has been subclassed and the {@code end} method is overridden,
         * uses {@code finalizer} mechanism for resource cleanup.
         * So {@code end} method can be called when the {@code Inflater} is unreachable.
         * This mechanism will be removed when the {@code finalize} method is removed from {@code Inflater}.
         */
        /*
         * 如果Inflater被子类化，且end方法被覆盖，则使用finalizer机制进行资源清理。
         * 否则，使用清理器Cleanable这种资源清理机制
         */
        static InflaterZStreamRef get(Inflater owner, long addr) {
            Class<?> clz = owner.getClass();
            
            // 如果clz是Inflater子类
            while(clz != Inflater.class) {
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
            return new InflaterZStreamRef(owner, addr);
        }
        
        long address() {
            return address;
        }
        
        private static class FinalizableZStreamRef extends InflaterZStreamRef {
            final Inflater owner;
            
            FinalizableZStreamRef(Inflater owner, long addr) {
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
