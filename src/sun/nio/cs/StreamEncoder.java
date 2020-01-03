/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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

// 输出流编码器：将字符序列编码为字节后，写入到字节输出流
public class StreamEncoder extends Writer {
    
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
    
    private CharsetEncoder encoder; // 字符编码器。字符进来，字节出去，完成对字符序列的编码操作
    private Charset cs;             // 字符编码器对应的字符集
    
    /** Exactly one of these is non-null */
    private final OutputStream out; // 字节输出流(最终输出流)
    
    /** Factory for java.nio.channels.Channels.newWriter */
    private WritableByteChannel ch; // 输出目的地关联的通道
    
    private ByteBuffer bb;          // 内部使用的堆内存缓冲区，存储解码后的字节
    
    /**
     * All synchronization and state/argument checking is done in these public methods;
     * the concrete stream-encoder subclasses defined below need not do any such checking.
     * Leftover first char in a surrogate pair
     */
    private boolean haveLeftoverChar = false;   // 上一轮编码后，是否残留一个未处理字符
    private char leftoverChar;                  // 保存一个未处理字符
    
    private CharBuffer lcb = null;  // 临时存储待处理的字符
    
    private volatile boolean closed;
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private StreamEncoder(OutputStream out, Object lock, Charset cs) {
        this(out, lock, cs.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
    }
    
    private StreamEncoder(OutputStream out, Object lock, CharsetEncoder enc) {
        super(lock);
        this.out = out;
        this.ch = null;
        this.cs = enc.charset();
        this.encoder = enc;
        
        /* This path disabled until direct buffers are faster */
        // 已禁用此途径，将来文件使用直接缓冲区更快时，可能会解禁此途径
        if(false && out instanceof FileOutputStream) {
            // 获取当前文件输入流关联的只读通道
            ch = ((FileOutputStream) out).getChannel();
            
            if(ch != null) {
                // 创建直接内存缓冲区DirectByteBuffer
                bb = ByteBuffer.allocateDirect(DEFAULT_BYTE_BUFFER_SIZE);
            }
        }
        
        if(ch == null) {
            // 创建堆内存缓冲区HeapByteBuffer
            bb = ByteBuffer.allocate(DEFAULT_BYTE_BUFFER_SIZE);
        }
    }
    
    private StreamEncoder(WritableByteChannel ch, CharsetEncoder enc, int mbc) {
        this.out = null;
        this.ch = ch;
        this.cs = enc.charset();
        this.encoder = enc;
        this.bb = ByteBuffer.allocate(mbc<0 ? DEFAULT_BYTE_BUFFER_SIZE : mbc);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将指定的字符写入到输出流
    public void write(int c) throws IOException {
        char[] cbuf = new char[1];
        cbuf[0] = (char) c;
        write(cbuf, 0, 1);
    }
    
    // 将字符数组cbuf中off处起的len个字符写入到输出流
    public void write(char[] cbuf, int off, int len) throws IOException {
        synchronized(lock) {
            ensureOpen();
            
            if((off<0) || (off>cbuf.length) || (len<0) || ((off + len)>cbuf.length) || ((off + len)<0)) {
                throw new IndexOutOfBoundsException();
            } else if(len == 0) {
                return;
            }
            
            implWrite(cbuf, off, len);
        }
    }
    
    // 将字符串str中off处起的len个字符写入到输出流
    public void write(String str, int off, int len) throws IOException {
        /* Check the len before creating a char buffer */
        if(len<0) {
            throw new IndexOutOfBoundsException();
        }
        
        char[] cbuf = new char[len];
        str.getChars(off, off + len, cbuf, 0);
        
        write(cbuf, 0, len);
    }
    
    // 将字符缓冲区cb中的字符写入到输出流
    public void write(CharBuffer cb) throws IOException {
        int position = cb.position();
        
        try {
            synchronized(lock) {
                ensureOpen();
                
                // 从cb中读取字符，并对其编码后写到输出流
                implWrite(cb);
            }
        } finally {
            cb.position(position);
        }
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 刷新缓冲区：将内部缓冲区中的字节写到最终输出流中(该操作不会刷新最终输出流)
    public void flushBuffer() throws IOException {
        synchronized(lock) {
            if(isOpen()) {
                implFlushBuffer();
            } else {
                throw new IOException("Stream closed");
            }
        }
    }
    
    // 刷新流，不仅刷新当前输出流编码器的内部缓冲区，还刷新最终字节输出流的内部缓冲区(如果存在)
    public void flush() throws IOException {
        synchronized(lock) {
            ensureOpen();
            implFlush();
        }
    }
    
    // 关闭输出流编码器
    public void close() throws IOException {
        synchronized(lock) {
            if(closed) {
                return;
            }
            implClose();
            closed = true;
        }
    }
    
    
    // 返回字节编码器使用的字符集名称
    public String getEncoding() {
        if(isOpen()) {
            return encodingName();
        }
        
        return null;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回输出流编码器，out是输出目的地
    public static StreamEncoder forOutputStreamWriter(OutputStream out, Object lock, String charsetName) throws UnsupportedEncodingException {
        String csn = charsetName;
        
        if(csn == null)
            csn = Charset.defaultCharset().name();
        try {
            if(Charset.isSupported(csn)) {
                return new StreamEncoder(out, lock, Charset.forName(csn));
            }
        } catch(IllegalCharsetNameException x) {
        }
        
        throw new UnsupportedEncodingException(csn);
    }
    
    // 返回输出流编码器，out是输出目的地
    public static StreamEncoder forOutputStreamWriter(OutputStream out, Object lock, Charset cs) {
        return new StreamEncoder(out, lock, cs);
    }
    
    // 返回输出流编码器，out是输出目的地
    public static StreamEncoder forOutputStreamWriter(OutputStream out, Object lock, CharsetEncoder enc) {
        return new StreamEncoder(out, lock, enc);
    }
    
    // 返回输出流编码器，ch是输出目的地，minBufferCap是内部缓冲区的最小容量
    public static StreamEncoder forEncoder(WritableByteChannel ch, CharsetEncoder enc, int minBufferCap) {
        return new StreamEncoder(ch, enc, minBufferCap);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 从字符数组cbuf中off处起读取len个字符，并对其编码后写到输出流
    void implWrite(char[] cbuf, int off, int len) throws IOException {
        // 包装字符数组cbuf到缓冲区cb
        CharBuffer cb = CharBuffer.wrap(cbuf, off, len);
        
        // 从cb中读取字符，并对其编码后写到输出流
        implWrite(cb);
    }
    
    // 从cb中读取字符，并对其编码后写到输出流
    void implWrite(CharBuffer cb) throws IOException {
        // 如果包含至少一个未处理字符
        if(haveLeftoverChar) {
            /*
             * 对上一轮写入中残留下的那个字符进行编码，并将编码后的字节后写入到输出流
             * 当发生下溢时，不会抛异常，因为可以期待后续的输入
             */
            flushLeftoverChar(cb, false);
        }
        
        // 如果缓冲区还有未读字符
        while(cb.hasRemaining()) {
            /*
             * 从给定的输入缓冲区cb中编码尽可能多的字符，将结果写入给定的输出缓冲区bb。
             * 当发生下溢时，可能还会有后续的输入，不会抛异常
             */
            CoderResult cr = encoder.encode(cb, bb, false);
            
            /*
             * 发生下溢，输出缓冲区bb仍有空闲
             *
             * 出现此情形很可能遇到了四字节符号
             */
            if(cr.isUnderflow()) {
                assert (cb.remaining()<=1) : cb.remaining();
                
                // 如果ch中仅剩一个未读的字符了
                if(cb.remaining() == 1) {
                    // 暂存该未处理的字符
                    haveLeftoverChar = true;
                    leftoverChar = cb.get();
                }
                
                break;
            }
            
            // 发生上溢，输出缓冲区已经满了
            if(cr.isOverflow()) {
                assert bb.position()>0;
                
                // 将内部缓冲区中的字节写到最终输出流中
                writeBytes();
                
                continue;
            }
            
            cr.throwException();
        }
    }
    
    // 刷新流，不仅刷新当前输出流编码器的内部缓冲区，还刷新字节输出流(的内部缓冲区)
    void implFlush() throws IOException {
        implFlushBuffer();
        
        if(out != null) {
            out.flush();
        }
    }
    
    // 刷新缓冲区：将内部缓冲区中的字节写到最终输出流中
    void implFlushBuffer() throws IOException {
        // 如果内部缓冲区中包含数据
        if(bb.position()>0) {
            // 将内部缓冲区中的字节写到最终输出流中
            writeBytes();
        }
    }
    
    // 关闭输出流编码器
    void implClose() throws IOException {
        /*
         * 对上一轮写入中残留下的那个字符进行编码，并将编码后的字节后写入到输出流
         * 当发生下溢时，可能会抛异常(遇到了有缺陷的输入)
         */
        flushLeftoverChar(null, true);
        
        try {
            for(; ; ) {
                // 刷新内部缓冲区
                CoderResult cr = encoder.flush(bb);
                if(cr.isUnderflow()) {
                    break;
                }
                
                if(cr.isOverflow()) {
                    assert bb.position()>0;
                    
                    // 将内部缓冲区中的字节写到最终输出流中
                    writeBytes();
                    
                    continue;
                }
                
                cr.throwException();
            }
            
            if(bb.position()>0) {
                // 将内部缓冲区中的字节写到最终输出流中
                writeBytes();
            }
            
            if(ch != null) {
                ch.close();
            } else {
                out.close();
            }
        } catch(IOException x) {
            encoder.reset();
            throw x;
        }
    }
    
    /*
     * 对上一轮写入中残留下的那个字符进行编码，并将编码后的字节后写入到输出流
     *
     * 如果之前有残留的字符，则需要从cb中再多读一个字符以配合编码，如果此时cb中已经没有可读字符，则该字符继续被残留。
     * 如果之前没有残留的字符，则需要从cb中读取一个或读取两个字符进行编码
     *
     * endOfInput=true ：当发生下溢时，可能会抛异常(遇到了有缺陷的输入)
     * endOfInput=false：当发生下溢时，不会抛异常，因为可以期待后续的输入
     */
    private void flushLeftoverChar(CharBuffer cb, boolean endOfInput) throws IOException {
        // 之前没有残留未处理字符，且不需要关闭输入流
        if(!haveLeftoverChar && !endOfInput) {
            return;
        }
        
        /* 至此，说明之前残留待处理字符，或(且)需要立即关闭输入流 */
        
        if(lcb == null) {
            /*
             * 构造非直接缓冲区HeapCharBuffer：将缓冲区建立在JVM的内存中
             * 这里容量分配为2的原因是现存的任何符号，最多用两个char就可以表示了
             */
            lcb = CharBuffer.allocate(2);
        } else {
            // 如果不为空，先清空它
            lcb.clear();
        }
        
        if(haveLeftoverChar) {
            // 向lcb中存入之前缓存的一个待读字符
            lcb.put(leftoverChar);
        }
        
        // 如果cb中仍有可读的字符
        if((cb != null) && cb.hasRemaining()) {
            // 继续从cb中读取一个char存入lcb
            lcb.put(cb.get());
        }
        
        // 将lcb从写模式转为读模式
        lcb.flip();
        
        // 如果lcb中还有待读字符，或者中断了输入
        while(lcb.hasRemaining() || endOfInput) {
            /*
             * 从给定的输入缓冲区lcb中编码尽可能多的字符，将结果写入给定的输出缓冲区bb。
             *
             * endOfInput=true表示当发生下溢时，输入立即结束，即不会再提供更多输入
             * endOfInput=false表示当发生下溢时，可能还会有后续的输入
             */
            CoderResult cr = encoder.encode(lcb, bb, endOfInput);
            
            /*
             * 发生下溢，输出缓冲区仍有空闲
             *
             * 可能是lcb进来时有2个char，但是都被处理完了，那么后续就break了
             * 可能是lcb进来时有1个char，但这个char来自cb，此时需要如果ch处理完了，直接return，如果cb没处理完，则尝试从cb中再读一个char进来处理
             * 可能是lcb进来时有1个char，但这个char来自leftoverChar，此时cb已处理完，则leftoverChar重新记下该char，并且return
             */
            if(cr.isUnderflow()) {
                // 如果lcb中还有待读字符
                if(lcb.hasRemaining()) {
                    // 将lcb中剩下未处理的那个char缓存到leftoverChar中
                    leftoverChar = lcb.get();
                    
                    // 如果cb中仍有可读的字符
                    if(cb != null && cb.hasRemaining()) {
                        // 清空，进入写模式
                        lcb.clear();
                        
                        // 重复向lcb填充2个char，填充完成后切换到读模式
                        lcb.put(leftoverChar).put(cb.get()).flip();
                        
                        continue;
                    }
                    
                    return;
                }
                
                break;
            }
            
            // 发生上溢，输出缓冲区已经满了
            if(cr.isOverflow()) {
                assert bb.position()>0;
                
                // 将内部缓冲区中的字节写到最终输出流中
                writeBytes();
                
                continue;
            }
            
            cr.throwException();
        }
        
        haveLeftoverChar = false;
    }
    
    // 将内部缓冲区中的字节写到最终输出流中
    private void writeBytes() throws IOException {
        bb.flip();  // 将内部缓冲区从写模式切换到读模式
        
        int lim = bb.limit();
        int pos = bb.position();
        
        assert (pos<=lim);
        
        int rem = (pos<=lim ? lim - pos : 0);
        
        if(rem>0) {
            if(ch != null) {
                assert ch.write(bb) == rem : rem;
            } else {
                // 返回该buffer内部的数组
                byte[] b = bb.array();
                int off = bb.arrayOffset() + pos;
                
                // 将字节数组b中off处起的rem个字节写入到输出流
                out.write(b, off, rem);
            }
        }
        
        // 清理缓冲区，重置标记
        bb.clear();
    }
    
    // 返回字节编码器使用的字符集名称
    String encodingName() {
        return ((cs instanceof HistoricallyNamedCharset) ? ((HistoricallyNamedCharset) cs).historicalName() : cs.name());
    }
    
    // 判断输出流编码器是否处于开启状态
    private boolean isOpen() {
        return !closed;
    }
    
    // 确保输出流编码器处于开启状态
    private void ensureOpen() throws IOException {
        if(closed) {
            throw new IOException("Stream closed");
        }
    }
    
}
