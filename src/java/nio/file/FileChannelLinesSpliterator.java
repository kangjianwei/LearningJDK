/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package java.nio.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A file-based lines spliterator, leveraging a shared mapped byte buffer and
 * associated file channel, covering lines of a file for character encodings
 * where line feed characters can be easily identified from character encoded
 * bytes.
 *
 * <p>
 * When the root spliterator is first split a mapped byte buffer will be created
 * over the file for it's size that was observed when the stream was created.
 * Thus a mapped byte buffer is only required for parallel stream execution.
 * Sub-spliterators will share that mapped byte buffer.  Splitting will use the
 * mapped byte buffer to find the closest line feed characters(s) to the left or
 * right of the mid-point of covered range of bytes of the file.  If a line feed
 * is found then the spliterator is split with returned spliterator containing
 * the identified line feed characters(s) at the end of it's covered range of
 * bytes.
 *
 * <p>
 * Traversing will create a buffered reader, derived from the file channel, for
 * the range of bytes of the file.  The lines are then read from that buffered
 * reader.  Once traversing commences no further splitting can be performed and
 * the reference to the mapped byte buffer will be set to null.
 */
// 基于文件行的流迭代器
final class FileChannelLinesSpliterator implements Spliterator<String> {
    
    private final FileChannel fileChannel;  // 文件通道
    private int index;          // 文件通道内容的起始游标
    private final int fence;    // 文件通道内容的上限游标
    
    // Non-null when traversing
    private BufferedReader reader;  // 用于读取给定的文件通道
    
    // Null before first split, non-null when splitting, null when traversing
    private ByteBuffer buffer;      // 文件映射内存
    
    private final Charset charset;  // 字符集
    
    static final Set<String> SUPPORTED_CHARSET_NAMES;   // 当前迭代器支持的字符集，用于解码
    
    static {
        SUPPORTED_CHARSET_NAMES = new HashSet<>();
        SUPPORTED_CHARSET_NAMES.add(StandardCharsets.UTF_8.name());
        SUPPORTED_CHARSET_NAMES.add(StandardCharsets.ISO_8859_1.name());
        SUPPORTED_CHARSET_NAMES.add(StandardCharsets.US_ASCII.name());
    }
    
    FileChannelLinesSpliterator(FileChannel fileChannel, Charset charset, int index, int fence) {
        this.fileChannel = fileChannel;
        this.charset = charset;
        this.index = index;
        this.fence = fence;
    }
    
    private FileChannelLinesSpliterator(FileChannel fileChannel, Charset charset, int index, int fence, ByteBuffer buffer) {
        this.fileChannel = fileChannel;
        this.buffer = buffer;
        this.charset = charset;
        this.index = index;
        this.fence = fence;
    }
    
    // 分割文件内容，返回的迭代器包含了文件的前半部分
    @Override
    public Spliterator<String> trySplit() {
        // 必须确保文件还没被读取之前进行分割
        if(reader != null) {
            return null;
        }
        
        ByteBuffer buf;
        if((buf = buffer) == null) {
            // 获取一块文件映射内存
            buf = buffer = getMappedByteBuffer();
        }
        
        final int hi = fence, lo = index;
        
        // Check if line separator hits the mid point
        int mid = (lo + hi) >>> 1;
        int c = buf.get(mid);
        if(c == '\n') {
            mid++;
        } else if(c == '\r') {
            // Check if a line separator of "\r\n"
            if(++mid<hi && buf.get(mid) == '\n') {
                mid++;
            }
        } else {
            // TODO give up after a certain distance from the mid point?
            // Scan to the left and right of the mid point
            int midL = mid - 1;
            int midR = mid + 1;
            mid = 0;
            while(midL>lo && midR<hi) {
                // Sample to the left
                c = buf.get(midL--);
                if(c == '\n' || c == '\r') {
                    // If c is "\r" then no need to check for "\r\n"
                    // since the subsequent value was previously checked
                    mid = midL + 2;
                    break;
                }
                
                // Sample to the right
                c = buf.get(midR++);
                if(c == '\n' || c == '\r') {
                    mid = midR;
                    // Check if line-separator is "\r\n"
                    if(c == '\r' && mid<hi && buf.get(mid) == '\n') {
                        mid++;
                    }
                    break;
                }
            }
        }
    
        // The left spliterator will have the line-separator at the end
        return (mid>lo && mid<hi) ? new FileChannelLinesSpliterator(fileChannel, charset, lo, index = mid, buf) : null;
    }
    
    // 尝试用action消费目标通道的下一行
    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
        // 获取一行内容
        String line = readLine();
        
        // 处理该行内容
        if(line != null) {
            action.accept(line);
            return true;
        } else {
            return false;
        }
    }
    
    // 尝试用action消费目标通道的每一行
    @Override
    public void forEachRemaining(Consumer<? super String> action) {
        String line;
        while((line = readLine()) != null) {
            action.accept(line);
        }
    }
    
    // 返回剩余可读的字节数量（可能是估算值）
    @Override
    public long estimateSize() {
        /*
         * Use the number of bytes as an estimate.
         * We could divide by a constant that is the average number of characters per-line, but that constant will be factored out.
         */
        return fence - index;
    }
    
    // 返回当前情境中的元素数量（精确值）
    @Override
    public long getExactSizeIfKnown() {
        return -1;
    }
    
    // 返回当前流迭代器的参数
    @Override
    public int characteristics() {
        return Spliterator.ORDERED | Spliterator.NONNULL;
    }
    
    // 构造一个Reader以读取给定的文件通道
    private BufferedReader getBufferedReader() {
        /* A readable byte channel that reads bytes from an underlying file channel over a specified range. */
        ReadableByteChannel channel = new ReadableByteChannel() {
            @Override
            public int read(ByteBuffer dst) throws IOException {
                // 计算待读字节数量
                int bytesToRead = fence - index;
                if(bytesToRead == 0) {
                    return -1;
                }
                
                int bytesRead;
                
                // 如果dst的剩余空间足够存放文件中剩余的字节
                if(bytesToRead<dst.remaining()) {
                    /*
                     * The number of bytes to read is less than remaining bytes in the buffer
                     * Snapshot the limit, reduce it, read, then restore
                     */
                    int oldLimit = dst.limit();
                    // 设置新上限，目的是限制填充进来的字节长度
                    dst.limit(dst.position() + bytesToRead);
                    // 从文件通道的index处读取，读到的数据填充到dst中
                    bytesRead = fileChannel.read(dst, index);
                    // 恢复旧上限
                    dst.limit(oldLimit);
                    
                    // 如果文件中剩余的字节数超出了dst的剩余空间，则接下来的读取会填满dst
                } else {
                    // 从文件通道的index处读取，读到的数据填充到dst中
                    bytesRead = fileChannel.read(dst, index);
                }
                
                // 已经没有数据可读了
                if(bytesRead == -1) {
                    index = fence;
                    return bytesRead;
                }
                
                index += bytesRead;
                
                // 返回读到的字节数
                return bytesRead;
            }
            
            @Override
            public boolean isOpen() {
                return fileChannel.isOpen();
            }
            
            @Override
            public void close() throws IOException {
                fileChannel.close();
            }
        };
    
        // 字节解码器。字节进来，字符出去，完成对字节序列的解码操作
        CharsetDecoder charsetDecoder = charset.newDecoder();
        
        // 构造一个输入流解码器(继承了Reader)
        Reader reader = Channels.newReader(channel, charsetDecoder, -1);
    
        return new BufferedReader(reader);
    }
    
    // 返回读到的一行内容
    private String readLine() {
        if(reader == null) {
            reader = getBufferedReader();
            buffer = null;
        }
        
        try {
            return reader.readLine();
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    // 返回一块文件映射内存(经过了包装，加入了内存清理操作)
    private ByteBuffer getMappedByteBuffer() {
        // TODO can the mapped byte buffer be explicitly unmapped?
        // It's possible, via a shared-secret mechanism, when either
        // 1) the spliterator starts traversing, although traversal can
        //    happen concurrently for mulitple spliterators, so care is
        //    needed in this case; or
        // 2) when the stream is closed using some shared holder to pass
        //    the mapped byte buffer when it is created.
        try {
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fence);
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
}
