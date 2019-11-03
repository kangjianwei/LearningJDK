/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static java.util.zip.ZipConstants64.*;
import static java.util.zip.ZipUtils.*;

/**
 * This class implements an input stream filter for reading files in the
 * ZIP file format. Includes support for both compressed and uncompressed
 * entries.
 *
 * @author David Connelly
 * @since 1.1
 */
/*
 * zip输入流：读取zip文件，将其解压为原始数据后填充到指定的内存
 *
 * 注：读取的压缩文件可能结构并不完整，比如缺失zip文件的第二部分与第三部分
 *
 * zip结构参见：
 * https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
 * https://pkware.cachefly.net/webdocs/APPNOTE/APPNOTE-6.3.5.TXT
 */
public class ZipInputStream extends InflaterInputStream implements ZipConstants {
    
    private static final int STORED = ZipEntry.STORED;  // 原始数据未压缩(如果设置了这种压缩方式，则必须主动设置实体压缩前的大小/压缩后的大小、crc-32校验码这三个参数)
    
    private static final int DEFLATED = ZipEntry.DEFLATED;  // 原始数据使用了默认压缩方式
    
    private int flag;   // 通用位标记
    
    private CRC32 crc = new CRC32();    // 数据校验
    
    private ZipCoder zc;    // zip编/解码器
    
    private ZipEntry entry; //待解压实体
    
    private long remaining; // 剩余待解压字节数，在待解压实体未压缩时使用
    
    private boolean closed = false; // 当前zip输入流是否已关闭
    
    /* this flag is set to true after EOF has reached for one entry */
    private boolean entryEOF = false;   // 是否到达某个zip实体末尾(一个zip压缩包里可能包含多个zip实体)
    
    private byte[] tmpbuf = new byte[512];  // 读取附加信息时用到的临时存储
    private byte[] b = new byte[256];       // 存储实体名称时用到的临时存储
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new ZIP input stream.
     *
     * <p>The UTF-8 {@link java.nio.charset.Charset charset} is used to
     * decode the entry names.
     *
     * @param in the actual input stream
     */
    /*
     * 使用指定的源头输入流构造支持utf8字符集的zip输入流。
     * 使用的解压器具有默认解压级别，且兼容GZIP。
     */
    public ZipInputStream(InputStream in) {
        this(in, StandardCharsets.UTF_8);
    }
    
    /**
     * Creates a new ZIP input stream.
     *
     * @param in      the actual input stream
     * @param charset The {@linkplain java.nio.charset.Charset charset} to be
     *                used to decode the ZIP entry name (ignored if the
     *                <a href="package-summary.html#lang_encoding"> language
     *                encoding bit</a> of the ZIP entry's general purpose bit
     *                flag is set).
     *
     * @since 1.7
     */
    /*
     * 使用指定的源头输入流构造支持指定字符集的zip输入流。
     * 使用的解压器具有默认解压级别，且兼容GZIP。
     */
    public ZipInputStream(InputStream in, Charset charset) {
        super(new PushbackInputStream(in, 512), new Inflater(true), 512);
        
        // 使用了具有默认解压级别的解压器
        usesDefaultInflater = true;
        
        if(in == null) {
            throw new NullPointerException("in is null");
        }
        
        if(charset == null) {
            throw new NullPointerException("charset is null");
        }
        
        // 获取指定字符集的Zip编/解码器
        this.zc = ZipCoder.get(charset);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 解压前 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads the next ZIP file entry and positions the stream at the
     * beginning of the entry data.
     *
     * @return the next ZIP file entry, or null if there are no more entries
     *
     * @throws ZipException if a ZIP file error has occurred
     * @throws IOException  if an I/O error has occurred
     */
    /*
     * 返回待解压实体
     *
     * 该方法需要在每一个实体read开始之前被调用。
     * 此方法包含了对文件头信息(属于zip文件的第一部分的第1小节)的读取。
     */
    public ZipEntry getNextEntry() throws IOException {
        ensureOpen();
        
        // 如果已经存在待解压实体
        if(entry != null) {
            // 关闭之前的待解压实体
            closeEntry();
        }
        
        // 重置当前校验和
        crc.reset();
        
        // 重置解压器，以便解压器接收新数据进行解压
        inf.reset();
        
        // 读取文件头信息(属于zip文件的第一部分的第1小节)，构造一个zip实体
        entry = readLOC();
        
        // 如果未发现有效的zip实体，则返回null
        if(entry == null) {
            return null;
        }
        
        // 待解压实体未压缩
        if(entry.method == STORED) {
            remaining = entry.size;
        }
        
        entryEOF = false;
        
        return entry;
    }
    
    /*▲ 解压前 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/解压 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads from the current ZIP entry into an array of bytes.
     * If <code>len</code> is not zero, the method
     * blocks until some input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
     *
     * @param b   the buffer into which the data is read
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read
     *
     * @return the actual number of bytes read, or -1 if the end of the
     * entry is reached
     *
     * @throws NullPointerException      if <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws ZipException              if a ZIP file error has occurred
     * @throws IOException               if an I/O error has occurred
     */
    /*
     * 读取解压文件数据(属于zip文件的第一部分的第2小节)
     * 读取扩展数据描述符(属于zip文件的第一部分的第3小节)(数据被默认压缩时)
     *
     * 从(解压)输入流中读取(解压)出len个解压后的字节，并将其存入b的off处。
     * 返回值表示实际得到的解压后的字节数，如果返回-1，表示已经没有可解压字节，
     * 但不代表解压器缓冲区内或输入流中没有字节(因为有些数据并不需要解压)。
     *
     * read之前，需要getNextEntry读取待压缩实体的文件头信息。
     * read之后，需要closeEntry关闭/保存当前解压实体。
     */
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        
        if(off<0 || len<0 || off>b.length - len) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return 0;
        }
        
        // 需要确保待解压实体存在
        if(entry == null) {
            return -1;
        }
        
        switch(entry.method) {
            // 原始数据采用默认压缩方式
            case DEFLATED:
                // 从(解压)输入流中读取(解压)出len个解压后的字节，并将其存入b的off处
                len = super.read(b, off, len);
                
                // 没有成功读取到解压数据，说明zip文件的第一部分的第2小节已经读完了
                if(len == -1) {
                    // 读取扩展数据描述符(属于zip文件的第一部分的第3小节)
                    readEnd(entry);
                    entryEOF = true;    // 已到达当前zip实体末尾
                    entry = null;
                } else {
                    // 用字节数组b中off处起的len个字节更新当前校验和
                    crc.update(b, off, len);
                }
                
                return len;
            
            // 原始数据未压缩
            case STORED:
                if(remaining<=0) {
                    entryEOF = true;    // 已到达当前zip实体末尾
                    entry = null;
                    return -1;
                }
                
                if(len>remaining) {
                    len = (int) remaining;
                }
                
                // 直接从源头输入流读取len个字节存入字节数组b的off处
                len = in.read(b, off, len);
                if(len == -1) {
                    throw new ZipException("unexpected EOF");
                }
                
                // 用字节数组b中off处起的len个字节更新当前校验和
                crc.update(b, off, len);
                
                remaining -= len;
                
                if(remaining == 0 && entry.crc != crc.getValue()) {
                    throw new ZipException("invalid entry CRC (expected 0x" + Long.toHexString(entry.crc) + " but got 0x" + Long.toHexString(crc.getValue()) + ")");
                }
                return len;
            
            default:
                throw new ZipException("invalid compression method");
        }
    }
    
    /*▲ 读/解压 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 解压后 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes the current ZIP entry and positions the stream for reading the next entry.
     *
     * @throws ZipException if a ZIP file error has occurred
     * @throws IOException  if an I/O error has occurred
     */
    /*
     * 关闭/保存当前待解压实体以便进行下一个实体的读取。
     *
     * 该方法需要在每一个实体read结束之后被调用。
     * 此方法丢弃了待解压实体中未读的数据。
     */
    public void closeEntry() throws IOException {
        ensureOpen();
        
        // 丢弃剩余未读的待解压数据
        while(read(tmpbuf, 0, tmpbuf.length) != -1)
            ;
        
        entryEOF = true;
    }
    
    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     *
     * @throws IOException if an I/O error has occurred
     */
    // 关闭输入流
    public void close() throws IOException {
        if(!closed) {
            super.close();
            closed = true;
        }
    }
    
    /*▲ 解压后 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 附加信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * Reads local file (LOC) header for next entry.
     */
    // 读取文件头信息(属于zip文件的第一部分的第1小节)，构造一个zip实体
    private ZipEntry readLOC() throws IOException {
        try {
            readFully(tmpbuf, 0, LOCHDR);   // 读取文件头信息
        } catch(EOFException e) {
            return null;
        }
        
        if(get32(tmpbuf, 0) != LOCSIG) {
            return null;
        }
        
        // get flag first, we need check USE_UTF8.
        flag = get16(tmpbuf, LOCFLG);       // 通用位标记(2字节)
        
        // get the entry name and create the ZipEntry first
        int len = get16(tmpbuf, LOCNAM);   // 实体名称长度(2字节)
        
        int blen = b.length;
        if(len>blen) {
            do {
                blen = blen * 2;
            } while(len>blen);
            b = new byte[blen];
        }
        
        readFully(b, 0, len);
        
        /* Force to use UTF-8 if the USE_UTF8 bit is ON */
        // 创建zip实体
        ZipEntry e = createZipEntry(((flag & USE_UTF8) != 0) ? zc.toStringUTF8(b, len) : zc.toString(b, len));
        
        // now get the remaining fields for the entry
        if((flag & 1) == 1) {
            throw new ZipException("encrypted ZIP entry not supported");
        }
        
        e.method = get16(tmpbuf, LOCHOW);   // 压缩方法(2字节)
        e.xdostime = get32(tmpbuf, LOCTIM); // 文件最后修改时间(2字节)和文件最后修改日期(2字节)
        
        // 默认压缩方式
        if((flag & 8) == 8) {
            /* "Data Descriptor" present */
            if(e.method != DEFLATED) {
                throw new ZipException("only DEFLATED entries can have EXT descriptor");
            }
            
            // 未压缩
        } else {
            e.crc = get32(tmpbuf, LOCCRC);
            e.csize = get32(tmpbuf, LOCSIZ);
            e.size = get32(tmpbuf, LOCLEN);
        }
        
        len = get16(tmpbuf, LOCEXT);
        if(len>0) {
            byte[] extra = new byte[len];
            readFully(extra, 0, len);
            // 为zip实体e填充扩展信息
            e.setExtra0(extra, e.csize == ZIP64_MAGICVAL || e.size == ZIP64_MAGICVAL);
        }
        
        return e;
    }
    
    /**
     * Reads end of deflated entry as well as EXT descriptor if present.
     *
     * Local headers for DEFLATED entries may optionally be followed by a
     * data descriptor, and that data descriptor may optionally contain a
     * leading signature (EXTSIG).
     *
     * From the zip spec http://www.pkware.com/documents/casestudies/APPNOTE.TXT
     *
     * """Although not originally assigned a signature, the value 0x08074b50
     * has commonly been adopted as a signature value for the data descriptor
     * record.  Implementers should be aware that ZIP files may be
     * encountered with or without this signature marking data descriptors
     * and should account for either case when reading ZIP files to ensure
     * compatibility."""
     */
    /*
     * 读取扩展数据描述符(属于zip文件的第一部分的第3小节)
     *
     * 用于标识该文件压缩结束，该结构只有在相应的文件头信息中通用标记字段的第3位设为1时才会出现，紧接在压缩文件源数据后。
     * 这个数据描述符常用在不能对输出的zip文件进行检索时使用。
     */
    private void readEnd(ZipEntry e) throws IOException {
        // 返回解压器内部缓冲区中剩余未处理的字节数量
        int n = inf.getRemaining();
        if(n>0) {
            // 将待解压数据推回解压缓冲区
            ((PushbackInputStream) in).unread(buf, len - n, n);
        }
        
        // 默认压缩方式
        if((flag & 8) == 8) {
            /* "Data Descriptor" present */
            // 对zip64的支持
            if(inf.getBytesWritten()>ZIP64_MAGICVAL || inf.getBytesRead()>ZIP64_MAGICVAL) {
                // ZIP64 format
                readFully(tmpbuf, 0, ZIP64_EXTHDR);
                long sig = get32(tmpbuf, 0);
                if(sig != EXTSIG) { // no EXTSIG present
                    e.crc = sig;
                    e.csize = get64(tmpbuf, ZIP64_EXTSIZ - ZIP64_EXTCRC);
                    e.size = get64(tmpbuf, ZIP64_EXTLEN - ZIP64_EXTCRC);
                    ((PushbackInputStream) in).unread(tmpbuf, ZIP64_EXTHDR - ZIP64_EXTCRC, ZIP64_EXTCRC);
                } else {
                    e.crc = get32(tmpbuf, ZIP64_EXTCRC);
                    e.csize = get64(tmpbuf, ZIP64_EXTSIZ);
                    e.size = get64(tmpbuf, ZIP64_EXTLEN);
                }
            } else {
                // 读取扩展数据描述符(属于zip文件的第一部分的第3小节)
                readFully(tmpbuf, 0, EXTHDR);
                
                long sig = get32(tmpbuf, 0);
                if(sig != EXTSIG) { // no EXTSIG present
                    e.crc = sig;
                    e.csize = get32(tmpbuf, EXTSIZ - EXTCRC);
                    e.size = get32(tmpbuf, EXTLEN - EXTCRC);
                    ((PushbackInputStream) in).unread(tmpbuf, EXTHDR - EXTCRC, EXTCRC);
                } else {
                    e.crc = get32(tmpbuf, EXTCRC);
                    e.csize = get32(tmpbuf, EXTSIZ);
                    e.size = get32(tmpbuf, EXTLEN);
                }
            }
        }
        
        if(e.size != inf.getBytesWritten()) {
            throw new ZipException("invalid entry size (expected " + e.size + " but got " + inf.getBytesWritten() + " bytes)");
        }
        
        if(e.csize != inf.getBytesRead()) {
            throw new ZipException("invalid entry compressed size (expected " + e.csize + " but got " + inf.getBytesRead() + " bytes)");
        }
        
        if(e.crc != crc.getValue()) {
            throw new ZipException("invalid entry CRC (expected 0x" + Long.toHexString(e.crc) + " but got 0x" + Long.toHexString(crc.getValue()) + ")");
        }
    }
    
    /*
     * Reads bytes, blocking until all bytes are read.
     */
    // 从源头输入流读取len个字节，并将其存入字节数组b的off处
    private void readFully(byte[] b, int off, int len) throws IOException {
        while(len>0) {
            int n = in.read(b, off, len);
            if(n == -1) {
                throw new EOFException();
            }
            
            off += n;
            len -= n;
        }
    }
    
    /*▲ 附加信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns 0 after EOF has reached for the current entry data,
     * otherwise always return 1.
     * <p>
     * Programs should not count on this method to return the actual number
     * of bytes that could be read without blocking.
     *
     * @return 1 before EOF and 0 after EOF has reached for current entry.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 判断某个zip实体是否有未读(未解压)数据
    public int available() throws IOException {
        ensureOpen();
        
        if(entryEOF) {
            return 0;
        } else {
            return 1;
        }
    }
    
    /**
     * Skips specified number of bytes in the current ZIP entry.
     *
     * @param n the number of bytes to skip
     *
     * @return the actual number of bytes skipped
     *
     * @throws ZipException             if a ZIP file error has occurred
     * @throws IOException              if an I/O error has occurred
     * @throws IllegalArgumentException if {@code n < 0}
     */
    // 跳过某个zip实体中len个解压后的字节
    public long skip(long n) throws IOException {
        if(n<0) {
            throw new IllegalArgumentException("negative skip length");
        }
        
        ensureOpen();
        
        int max = (int) Math.min(n, Integer.MAX_VALUE);
        int total = 0;
        while(total<max) {
            int len = max - total;
            if(len>tmpbuf.length) {
                len = tmpbuf.length;
            }
            
            len = read(tmpbuf, 0, len);
            if(len == -1) {
                entryEOF = true;
                break;
            }
            
            total += len;
        }
        
        return total;
    }
    
    /**
     * Creates a new <code>ZipEntry</code> object for the specified
     * entry name.
     *
     * @param name the ZIP file entry name
     *
     * @return the ZipEntry just created
     */
    // 创建一个指定名称的zip实体
    protected ZipEntry createZipEntry(String name) {
        return new ZipEntry(name);
    }
    
    /**
     * Check to make sure that this stream has not been closed
     */
    // 确保输入流可用(未关闭)
    private void ensureOpen() throws IOException {
        if(closed) {
            throw new IOException("Stream closed");
        }
    }
    
}
