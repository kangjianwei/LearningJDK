/*
 * Copyright (c) 2001, 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
 */

package sun.nio.cs;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

// 输入流解码器：读取字节输入流中的数据，将其解码为字符
public class StreamDecoder extends Reader {
    
    private static final int MIN_BYTE_BUFFER_SIZE = 32;
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
    
    private CharsetDecoder decoder; // 字节解码器。字节进来，字符出去，完成对字节序列的解码操作
    private Charset cs;             // 字节解码器对应的字符集
    
    /** Exactly one of these is non-null */
    private InputStream in;         // 字节输入流(输入源)
    
    private ReadableByteChannel ch; // 输入源关联的通道
    /**
     * In the early stages of the build we haven't yet built the NIO native code,
     * so guard against that by catching the first UnsatisfiedLinkError and setting this flag so that later attempts fail quickly.
     */
    private static volatile boolean channelsAvailable = true;   // 输入源通道是否有效
    
    private ByteBuffer bb;          // 内部使用的堆内存缓冲区，存储输入源中的待解码字节
    
    /**
     * In order to handle surrogates properly we must never try to produce fewer than two characters at a time.
     * If we're only asked to return one character then the other is saved here to be returned later.
     */
    private boolean haveLeftoverChar = false;   // 是否包含至少一个待读字符
    private char leftoverChar;                  // 保存一个待读字符
    
    private volatile boolean closed;
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    StreamDecoder(InputStream in, Object lock, Charset cs) {
        this(in, lock, cs.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
    }
    
    StreamDecoder(InputStream in, Object lock, CharsetDecoder dec) {
        super(lock);
        
        this.decoder = dec;
        
        // 返回当前解码器对应的字符集
        this.cs = dec.charset();
        
        /* This path disabled until direct buffers are faster */
        // 已禁用此途径，将来文件使用直接缓冲区更快时，可能会解禁此途径
        if(false && in instanceof FileInputStream) {
            // 获取当前文件输入流关联的只读通道
            ch = getChannel((FileInputStream) in);
            
            if(ch != null) {
                // 创建直接内存缓冲区DirectByteBuffer
                bb = ByteBuffer.allocateDirect(DEFAULT_BYTE_BUFFER_SIZE);
            }
        }
        
        if(ch == null) {
            this.in = in;
            this.ch = null;
            
            // 创建堆内存缓冲区HeapByteBuffer
            bb = ByteBuffer.allocate(DEFAULT_BYTE_BUFFER_SIZE);
        }
        
        // 初始化缓冲区容量为0
        bb.flip();                      // So that bb is initially empty
    }
    
    StreamDecoder(ReadableByteChannel ch, CharsetDecoder dec, int mbc) {
        this.in = null;
        this.ch = ch;
        this.decoder = dec;
        this.cs = dec.charset();
        this.bb = ByteBuffer.allocate(mbc<0 ? DEFAULT_BYTE_BUFFER_SIZE : (mbc<MIN_BYTE_BUFFER_SIZE ? MIN_BYTE_BUFFER_SIZE : mbc));
        bb.flip();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回一个从输入流中解码的字符
    public int read() throws IOException {
        return read0();
    }
    
    // 尝试从字符输入流中读取length个char，并将其填充到cbuf的offset处。返回实际填充的字符数量
    public int read(char[] cbuf, int offset, int length) throws IOException {
        int off = offset;
        int len = length;
        
        synchronized(lock) {
            ensureOpen();
            
            if((off<0) || (off>cbuf.length) || (len<0) || ((off + len)>cbuf.length) || ((off + len)<0)) {
                throw new IndexOutOfBoundsException();
            }
            
            if(len == 0) {
                return 0;
            }
            
            int n = 0;
            
            // 如果包含至少一个待读字符
            if(haveLeftoverChar) {
                // Copy the leftover char into the buffer
                cbuf[off] = leftoverChar;   // 读取之前缓存的待读字符
                off++;
                len--;
                haveLeftoverChar = false;
                n = 1;
                
                
                if((len == 0) || !implReady()) {
                    return n;   // Return now if this is all we can produce w/o blocking
                }
            }
            
            if(len == 1) {
                // Treat single-character array reads just like read()
                int c = read0();    // 返回一个从输入流中解码的字符
                if(c == -1) {
                    return (n == 0) ? -1 : n;
                }
                cbuf[off] = (char) c;
                return n + 1;
            }
            
            // 从输入源读取字节，将其解码为字符后填充到字符数组cbuf中off处，尽量填充len个字符，返回实际填充的字符数
            return n + implRead(cbuf, off, off + len);
        }
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭输入流解码器
    public void close() throws IOException {
        synchronized(lock) {
            if(closed) {
                return;
            }
            implClose();
            closed = true;
        }
    }
    
    // 判断当前输入流解码器是否可以解码更多数据
    public boolean ready() throws IOException {
        synchronized(lock) {
            ensureOpen();
            return haveLeftoverChar || implReady();
        }
    }
    
    
    // 返回字节解码器使用的字符集名称
    public String getEncoding() {
        if(isOpen()) {
            return encodingName();
        }
        
        return null;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回输入流解码器，in是输入源
    public static StreamDecoder forInputStreamReader(InputStream in, Object lock, String charsetName) throws UnsupportedEncodingException {
        String csn = charsetName;
        if(csn == null) {
            // 获取Java虚拟机的默认字符集名称
            csn = Charset.defaultCharset().name();
        }
        
        try {
            // 如果系统支持此字符集
            if(Charset.isSupported(csn)) {
                return new StreamDecoder(in, lock, Charset.forName(csn));
            }
        } catch(IllegalCharsetNameException x) {
        }
        
        throw new UnsupportedEncodingException(csn);
    }
    
    // 返回输入流解码器，in是输入源
    public static StreamDecoder forInputStreamReader(InputStream in, Object lock, Charset cs) {
        return new StreamDecoder(in, lock, cs);
    }
    
    // 返回输入流解码器，in是输入源
    public static StreamDecoder forInputStreamReader(InputStream in, Object lock, CharsetDecoder dec) {
        return new StreamDecoder(in, lock, dec);
    }
    
    // 返回输入流解码器，ch是输入源，minBufferCap是内部缓冲区的最小容量
    public static StreamDecoder forDecoder(ReadableByteChannel ch, CharsetDecoder dec, int minBufferCap) {
        return new StreamDecoder(ch, dec, minBufferCap);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 返回字节解码器使用的字符集名称
    String encodingName() {
        return ((cs instanceof HistoricallyNamedCharset) ? ((HistoricallyNamedCharset) cs).historicalName() : cs.name());
    }
    
    // 返回当前文件输入流关联的只读通道
    private static FileChannel getChannel(FileInputStream in) {
        if(!channelsAvailable) {
            return null;
        }
        
        try {
            // 返回当前文件输入流关联的只读通道
            return in.getChannel();
        } catch(UnsatisfiedLinkError x) {
            channelsAvailable = false;
            return null;
        }
    }
    
    // 返回一个从输入流中解码的字符
    @SuppressWarnings("fallthrough")
    private int read0() throws IOException {
        synchronized(lock) {
            
            // Return the leftover char, if there is one
            if(haveLeftoverChar) {
                haveLeftoverChar = false;
                return leftoverChar;
            }
            
            // Convert more bytes
            char[] cb = new char[2];
            
            // 尝试从字符输入流中读取2个char，并将其填充到cbuf[0]处。返回实际填充的字符数量
            int n = read(cb, 0, 2);
            
            switch(n) {
                case -1:
                    return -1;
                case 2:
                    leftoverChar = cb[1];
                    haveLeftoverChar = true;
                    // FALL THROUGH
                case 1:
                    return cb[0];
                default:
                    assert false : n;
                    return -1;
            }
        }
    }
    
    // 从输入源读取字节，尽量填满内部缓冲区，返回填充结束后内部缓冲区中可读的字节数量
    private int readBytes() throws IOException {
        bb.compact();   // 压缩缓冲区，将当前未读完的数据挪到容器起始处，可用于读模式到写模式的切换，但又不丢失之前读入的数据。
        
        try {
            if(ch != null) {
                // Read from the channel
                int n = ch.read(bb);
                if(n<0) {
                    return n;
                }
            } else {
                // Read from the input stream, and then update the buffer
                int lim = bb.limit();
                int pos = bb.position();
                
                assert (pos<=lim);
                
                // 获取剩余可写空间
                int rem = (pos<=lim ? lim - pos : 0);
                
                assert rem>0;
                
                // 返回该buffer内部的数组
                byte[] b = bb.array();
                
                // 尝试从输入流in中读取rem个字节，并将读到的内容插入到字节数组b的off索引处
                int n = in.read(b, bb.arrayOffset() + pos, rem);
                if(n<0) {
                    return n;
                }
                
                if(n == 0) {
                    throw new IOException("Underlying input stream returned zero bytes");
                }
                
                assert (n<=rem) : "n = " + n + ", rem = " + rem;
                
                // 游标前进
                bb.position(pos + n);
            }
        } finally {
            // Flip even when an IOException is thrown, otherwise the stream will stutter
            bb.flip();  // 内部缓冲区从写模式切换到读模式
        }
        
        // 剩余可读字节数量
        int rem = bb.remaining();
        
        assert (rem != 0) : rem;
        
        return rem;
    }
    
    // 判断输入源是否可读
    private boolean inReady() {
        try {
            return (((in != null) && (in.available()>0)) || (ch instanceof FileChannel)); // ## RBC.available()?
        } catch(IOException x) {
            return false;
        }
    }
    
    // 从输入源读取字节，将其解码为字符后填充到字符数组cbuf中[off, end)处，返回实际填充的字符数
    int implRead(char[] cbuf, int off, int end) throws IOException {
        
        /*
         * In order to handle surrogate pairs,
         * this method requires that the invoker attempt to read at least two characters.
         * Saving the extra character, if any, at a higher level is easier than trying to deal with it here.
         */
        assert (end - off>1);
        
        // 包装一个字符数组到缓冲区cb
        CharBuffer cb = CharBuffer.wrap(cbuf, off, end - off);
        
        if(cb.position() != 0) {
            /* Ensure that cb[0] == cbuf[off] */
            cb = cb.slice();    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立
        }
        
        boolean eof = false;
        
        for(; ; ) {
            // 从输入缓冲区bb中解码尽可能多的字节，将结果写入输出缓冲区cb。
            CoderResult cr = decoder.decode(bb, cb, eof);
            
            // 如果发生下溢：输出缓冲区cb仍有空闲
            if(cr.isUnderflow()) {
                if(eof) {
                    break;
                }
                
                if(!cb.hasRemaining()) {
                    break;
                }
                
                if((cb.position()>0) && !inReady()) {
                    break;  // Block at most once
                }
                
                // 从输入源读取字节，尽量填满内部缓冲区，返回填充结束后内部缓冲区中可读的字节数量
                int n = readBytes();
                
                // 已到输入源末尾
                if(n<0) {
                    eof = true;
                    if((cb.position() == 0) && (!bb.hasRemaining())) {
                        break;
                    }
                    
                    // 重置字节解码器
                    decoder.reset();
                }
                
                continue;
            }
            
            // 发生上溢，输出缓冲区cb已经满了
            if(cr.isOverflow()) {
                assert cb.position()>0;
                break;
            }
            
            cr.throwException();
        }
        
        if(eof) {
            // ## Need to flush decoder
            decoder.reset();
        }
        
        if(cb.position() == 0) {
            if(eof) {
                return -1;
            }
            
            assert false;
        }
        
        // 此时的position代表cb中填充进去的字符数
        return cb.position();
    }
    
    // 判断是否存在待解码数据
    boolean implReady() {
        // 如果堆内存缓冲区中仍有数据，或者输入源可读，返回true
        return bb.hasRemaining() || inReady();
    }
    
    // 判断输入流解码器是否处于开启状态
    private boolean isOpen() {
        return !closed;
    }
    
    // 确保输入流解码器处于开启状态
    private void ensureOpen() throws IOException {
        if(closed) {
            throw new IOException("Stream closed");
        }
    }
    
    // 关闭输入流解码器
    void implClose() throws IOException {
        if(ch != null) {
            ch.close(); // 关闭输入源通道
        } else {
            in.close(); // 关闭输入源
        }
    }
    
}
