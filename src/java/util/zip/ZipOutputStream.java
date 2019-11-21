/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Vector;
import sun.security.action.GetPropertyAction;

import static java.util.zip.ZipConstants64.EXTID_EXTT;
import static java.util.zip.ZipConstants64.EXTID_NTFS;
import static java.util.zip.ZipConstants64.EXTID_ZIP64;
import static java.util.zip.ZipConstants64.EXTT_FLAG_LAT;
import static java.util.zip.ZipConstants64.EXTT_FLAG_LMT;
import static java.util.zip.ZipConstants64.EXTT_FLAT_CT;
import static java.util.zip.ZipConstants64.USE_UTF8;
import static java.util.zip.ZipConstants64.ZIP64_ENDHDR;
import static java.util.zip.ZipConstants64.ZIP64_ENDSIG;
import static java.util.zip.ZipConstants64.ZIP64_EXTID;
import static java.util.zip.ZipConstants64.ZIP64_LOCSIG;
import static java.util.zip.ZipConstants64.ZIP64_MAGICCOUNT;
import static java.util.zip.ZipConstants64.ZIP64_MAGICVAL;
import static java.util.zip.ZipUtils.UPPER_UNIXTIME_BOUND;
import static java.util.zip.ZipUtils.WINDOWS_TIME_NOT_AVAILABLE;
import static java.util.zip.ZipUtils.fileTimeToUnixTime;
import static java.util.zip.ZipUtils.fileTimeToWinTime;
import static java.util.zip.ZipUtils.get16;

/**
 * This class implements an output stream filter for writing files in the
 * ZIP file format. Includes support for both compressed and uncompressed
 * entries.
 *
 * @author David Connelly
 * @since 1.1
 */
/*
 * zip输出流：读取指定内存处的原始数据，将其压缩为zip文件后写入最终输出流
 *
 * 注：输出的压缩文件可能结构并不完整，比如缺失zip文件的第二部分与第三部分
 *
 * zip结构参见：
 * https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
 * https://pkware.cachefly.net/webdocs/APPNOTE/APPNOTE-6.3.5.TXT
 */
public class ZipOutputStream extends DeflaterOutputStream implements ZipConstants {
    
    /**
     * Compression method for uncompressed (STORED) entries.
     */
    public static final int STORED = ZipEntry.STORED;       // 原始数据未压缩(如果设置了这种压缩方式，则必须主动设置实体压缩前的大小/压缩后的大小、crc-32校验码这三个参数)
    
    /**
     * Compression method for compressed (DEFLATED) entries.
     */
    public static final int DEFLATED = ZipEntry.DEFLATED;   // 原始数据使用了默认压缩方式
    
    private int method = DEFLATED;  // 压缩方式：默认
    
    /**
     * Whether to use ZIP64 for zip files with more than 64k entries.
     * Until ZIP64 support in zip implementations is ubiquitous, this
     * system property allows the creation of zip files which can be
     * read by legacy zip implementations which tolerate "incorrect"
     * total entry count fields, such as the ones in jdk6, and even
     * some in jdk7.
     */
    private static final boolean inhibitZip64 = Boolean.parseBoolean(GetPropertyAction.privilegedGetProperty("jdk.util.zip.inhibitZip64"));
    
    private CRC32 crc = new CRC32();    // 数据校验
    
    private final ZipCoder zc;  // zip编/解码器
    
    private XEntry current;     // 待压缩实体
    
    private Vector<XEntry> xentries = new Vector<>();   // 待压缩实体的集合
    
    private HashSet<String> names = new HashSet<>();    // 待压缩实体的名称集合
    
    private long written = 0;   // 当前输出流游标的偏移量
    private long locoff = 0;    // 上一个实体的末尾在输出流中的偏移量
    
    private byte[] comment;     // 压缩包注释
    
    private boolean finished;       // 本轮压缩是否已经完成(不关闭zip输出流)
    
    private boolean closed = false; // 当前zip输出流是否已关闭
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new ZIP output stream.
     *
     * <p>The UTF-8 {@link java.nio.charset.Charset charset} is used
     * to encode the entry names and comments.
     *
     * @param out the actual output stream
     */
    /*
     * 用指定的最终输出流构造使用utf8字符集的zip输出流。
     * 使用的压缩器具有默认压缩级别，且兼容GZIP。
     */
    public ZipOutputStream(OutputStream out) {
        this(out, StandardCharsets.UTF_8);
    }
    
    /**
     * Creates a new ZIP output stream.
     *
     * @param out     the actual output stream
     * @param charset the {@linkplain java.nio.charset.Charset charset}
     *                to be used to encode the entry names and comments
     *
     * @since 1.7
     */
    /*
     * 用指定的最终输出流构造使用指定字符集的zip输出流。
     * 使用的压缩器具有默认压缩级别，且兼容GZIP。
     */
    public ZipOutputStream(OutputStream out, Charset charset) {
        super(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
        
        if(charset == null) {
            throw new NullPointerException("charset is null");
        }
        
        // 获取指定字符集的Zip编/解码器
        this.zc = ZipCoder.get(charset);
        
        usesDefaultDeflater = true; // 使用了具有默认压缩级别的压缩器
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩前 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Begins writing a new ZIP file entry and positions the stream to the
     * start of the entry data. Closes the current entry if still active.
     * The default compression method will be used if no compression method
     * was specified for the entry, and the current time will be used if
     * the entry has no set modification time.
     *
     * @param e the ZIP entry to be written
     *
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException  if an I/O error has occurred
     */
    /*
     * 初始化待压缩实体。
     *
     * 该方法需要在每一个实体write开始之前被调用。
     * 此方法包含了对文件头信息(属于zip文件的第一部分的第1小节)的写入。
     */
    public void putNextEntry(ZipEntry e) throws IOException {
        ensureOpen();
        
        // 如果已经存在待压缩实体
        if(current != null) {
            // 关闭之前的待压缩实体
            closeEntry();
        }
        
        if(e.xdostime == -1) {
            /* by default, do NOT use extended timestamps in extra data, for now */
            // 设置当前时间为压缩实体的最后修改时间
            e.setTime(System.currentTimeMillis());
        }
        
        if(e.method == -1) {
            e.method = method;  // use default method
        }
        
        // store size, compressed size, and crc-32 in LOC header
        e.flag = 0;
        
        switch(e.method) {
            // 待压缩实体被设置为使用默认压缩方式(默认行为)
            case DEFLATED:
                // store size, compressed size, and crc-32 in data descriptor immediately following the compressed entry data
                if(e.size == -1 || e.csize == -1 || e.crc == -1) {
                    e.flag = 8;
                }
                break;
            
            // 待压缩实体被设置为不压缩
            case STORED:
                // compressed size, uncompressed size, and crc-32 must all be set for entries using STORED compression method
                if(e.size == -1) {
                    e.size = e.csize;
                } else if(e.csize == -1) {
                    e.csize = e.size;
                    
                    // 要求实体压缩前与压缩后的大小一致（因为此处不压缩）
                } else if(e.size != e.csize) {
                    throw new ZipException("STORED entry where compressed != uncompressed size");
                }
                
                // 必须设置实体压缩前的大小/压缩后的大小、crc-32校验码这三个参数
                if(e.size == -1 || e.crc == -1) {
                    throw new ZipException("STORED entry missing size, compressed size, or crc-32");
                }
                
                break;
            
            default:
                throw new ZipException("unsupported compression method");
        }
        
        // 实体名称不能重复
        if(!names.add(e.name)) {
            throw new ZipException("duplicate entry: " + e.name);
        }
        
        // 如果当前编/解码器为UTF8格式
        if(zc.isUTF8()) {
            // 添加标记
            e.flag |= USE_UTF8;
        }
        
        // 构造待压缩实体
        current = new XEntry(e, written);
        
        // 添加到待压缩实体的集合
        xentries.add(current);
        
        // 写入文件头信息(属于zip文件的第一部分的第1小节)
        writeLOC(current);
    }
    
    /**
     * Sets the compression level for subsequent entries which are DEFLATED.
     * The default setting is DEFAULT_COMPRESSION.
     *
     * @param level the compression level (0-9)
     *
     * @throws IllegalArgumentException if the compression level is invalid
     */
    // 设置压缩级别
    public void setLevel(int level) {
        def.setLevel(level);
    }
    
    /**
     * Sets the ZIP file comment.
     *
     * @param comment the comment string
     *
     * @throws IllegalArgumentException if the length of the specified
     *                                  ZIP file comment is greater than 0xFFFF bytes
     */
    // 设置压缩包注释
    public void setComment(String comment) {
        if(comment != null) {
            this.comment = zc.getBytes(comment);
            if(this.comment.length>0xffff) {
                throw new IllegalArgumentException("ZIP file comment too long.");
            }
        }
    }
    
    /**
     * Sets the default compression method for subsequent entries. This
     * default will be used whenever the compression method is not specified
     * for an individual ZIP file entry, and is initially set to DEFLATED.
     *
     * @param method the default compression method
     *
     * @throws IllegalArgumentException if the specified compression method
     *                                  is invalid
     */
    // 设置压缩方法
    public void setMethod(int method) {
        if(method != DEFLATED && method != STORED) {
            throw new IllegalArgumentException("invalid compression method");
        }
        this.method = method;
    }
    
    /*▲ 压缩前 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写/压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes an array of bytes to the current ZIP entry data. This method
     * will block until all the bytes are written.
     *
     * @param b   the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     *
     * @throws ZipException if a ZIP file error has occurred
     * @throws IOException  if an I/O error has occurred
     */
    /*
     * 写入压缩文件数据(属于zip文件的第一部分的第2小节)。
     *
     * 将压缩器的数据b中off处起的len个字节压缩后写入最终输出流。
     *
     * write之前，需要putNextEntry设置好待压缩实体。
     * write之后，需要closeEntry关闭/保存当前压缩实体。
     */
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        
        if(off<0 || len<0 || off>b.length - len) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return;
        }
        
        // 不存在待压缩实体，则会抛异常
        if(current == null) {
            throw new ZipException("no current ZIP entry");
        }
        
        ZipEntry entry = current.entry;
        
        switch(entry.method) {
            // 原始数据采用默认压缩方式
            case DEFLATED:
                // 将字节数组b中off处起的len个字节压缩后存入最终输出流
                super.write(b, off, len);
                break;
            
            // 原始数据未压缩
            case STORED:
                written += len;
                if(written - locoff>entry.size) {
                    throw new ZipException("attempt to write past end of STORED entry");
                }
                // 将字节数组b中off处起的len个字节直接存入最终输出流，不压缩，也不需要本地文件头或数据描述符信息
                out.write(b, off, len);
                break;
            
            default:
                throw new ZipException("invalid compression method");
        }
        
        // 用字节数组b中off处起的len个字节更新当前校验和
        crc.update(b, off, len);
    }
    
    /*▲ 写/压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩后 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Finishes writing the contents of the ZIP output stream without closing the underlying stream.
     * Use this method when applying multiple filters in succession to the same output stream.
     *
     * @throws ZipException if a ZIP file error has occurred
     * @throws IOException  if an I/O exception has occurred
     */
    /*
     * 结束压缩操作，但不会关闭zip输出流。
     * 该操作会向输出流写入zip文件的第二部分和第三部分。
     *
     * 调用此方法后，其他压缩器仍可以在此输出流上工作，
     * 不过此后输出的内容能不能被解压工具识别就是另一回事了。
     *
     * 注：如果压缩文件时未调用此方法，则输出的zip文件结构不完整，
     * 对于结构不完整的压缩文件，需要使用ZipInputStream来读取它的内容，
     * 如果借助ZipFile读取不完整的zip，则会发生异常。
     */
    public void finish() throws IOException {
        ensureOpen();
        
        if(finished) {
            return;
        }
        
        if(current != null) {
            closeEntry();
        }
        
        // write central directory
        long off = written;
        
        for(XEntry xentry : xentries) {
            // 写入zip文件的第二部分：压缩的目录源数据(核心目录数据)。
            writeCEN(xentry);
        }
        
        // 写入zip文件的第三部分：目录结束标识。
        writeEND(off, written - off);
        
        finished = true;
    }
    
    /**
     * Closes the current ZIP entry and positions the stream for writing the next entry.
     *
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException  if an I/O error has occurred
     */
    /*
     * 关闭/保存当前压缩实体以便进行下一个实体的写入。
     *
     * 该方法需要在每一个实体write结束之后被调用。
     * 此方法包含了对扩展数据描述符(属于zip文件的第一部分的第3小节)的写入。
     */
    public void closeEntry() throws IOException {
        ensureOpen();
        
        if(current != null) {
            ZipEntry e = current.entry;
            switch(e.method) {
                case DEFLATED:
                    // 设置刷新模式为FINISH
                    def.finish();
                    
                    // 如果压缩器还未完成压缩
                    while(!def.finished()) {
                        // 进行压缩操作
                        deflate();
                    }
                    
                    if((e.flag & 8) == 0) {
                        // verify size, compressed size, and crc-32 settings
                        if(e.size != def.getBytesRead()) {
                            throw new ZipException("invalid entry size (expected " + e.size + " but got " + def.getBytesRead() + " bytes)");
                        }
                        
                        if(e.csize != def.getBytesWritten()) {
                            throw new ZipException("invalid entry compressed size (expected " + e.csize + " but got " + def.getBytesWritten() + " bytes)");
                        }
                        if(e.crc != crc.getValue()) {
                            throw new ZipException("invalid entry CRC-32 (expected 0x" + Long.toHexString(e.crc) + " but got 0x" + Long.toHexString(crc.getValue()) + ")");
                        }
                    } else {
                        e.size = def.getBytesRead();
                        e.csize = def.getBytesWritten();
                        e.crc = crc.getValue();
                        
                        // 写入扩展数据描述符(属于zip文件的第一部分的第3小节)
                        writeEXT(e);
                    }
                    
                    def.reset();
                    written += e.csize;
                    break;
                
                case STORED:
                    // we already know that both e.size and e.csize are the same
                    if(e.size != written - locoff) {
                        throw new ZipException("invalid entry size (expected " + e.size + " but got " + (written - locoff) + " bytes)");
                    }
                    
                    if(e.crc != crc.getValue()) {
                        throw new ZipException("invalid entry crc-32 (expected 0x" + Long.toHexString(e.crc) + " but got 0x" + Long.toHexString(crc.getValue()) + ")");
                    }
                    break;
                default:
                    throw new ZipException("invalid compression method");
            }
            
            crc.reset();
            current = null;
        }
    }
    
    /**
     * Closes the ZIP output stream as well as the stream being filtered.
     *
     * @throws ZipException if a ZIP file error has occurred
     * @throws IOException  if an I/O error has occurred
     */
    /*
     * 关闭zip输出流，关闭前会先刷新缓冲区，随后还会释放压缩器的本地资源。
     *
     * 调用此方法后，其他压缩器不能再在此输出流上工作。
     */
    public void close() throws IOException {
        if(!closed) {
            super.close();
            closed = true;
        }
    }
    
    /*▲ 压缩后 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 附加信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes local file (LOC) header for specified entry.
     */
    // 写入文件头信息(属于zip文件的第一部分的第1小节)
    private void writeLOC(XEntry xentry) throws IOException {
        ZipEntry e = xentry.entry;
        
        int flag = e.flag;
        
        // 是否使用zip64格式
        boolean hasZip64 = false;
        
        // 获取扩展区长度
        int elen = getExtraLen(e.extra);
        
        writeInt(LOCSIG);               // LOC header signature | 文件头标识(4字节)
        
        // 默认压缩方式：DEFLATED
        if((flag & 8) == 8) {
            writeShort(version(e));     // version needed to extract | 解压文件所需pkware最低版本(2字节)
            writeShort(flag);           // general purpose bit flag  | 通用位标记(2字节)
            writeShort(e.method);       // compression method        | 压缩方法(2字节)
            writeInt(e.xdostime);       // last modification time    | 文件最后修改时间(2字节)和文件最后修改日期(2字节)
            // store size, uncompressed size, and crc-32 in data descriptor immediately following compressed entry data
            writeInt(0);    // Crc-32 checksum | crc-32校验码(4字节)
            writeInt(0);    // Compressed size | 压缩后的大小(4字节)
            writeInt(0);    // Uncompressed size | 压缩前的大小(4字节)
            
            // 不压缩：STORED
        } else {
            // 待压缩文件的大小或包含的条目数量超标，则需要改用zip64格式
            if(e.csize >= ZIP64_MAGICVAL || e.size >= ZIP64_MAGICVAL) {
                hasZip64 = true;    // 使用zip64格式
                writeShort(45);     // ver 4.5 for zip64 | zip64版本：4.5
            } else {
                writeShort(version(e)); // version needed to extract | 解压文件所需pkware最低版本(2字节)
            }
            
            writeShort(flag);           // general purpose bit flag | 通用位标记(2字节)
            writeShort(e.method);       // compression method | 压缩方法(2字节)
            writeInt(e.xdostime);       // last modification time | 文件最后修改时间(2字节)和文件最后修改日期(2字节)
            writeInt(e.crc);            // crc-32 | crc-32校验码(4字节)
            
            if(hasZip64) {
                writeInt(ZIP64_MAGICVAL);   // Compressed size   | 压缩后的大小(4字节)
                writeInt(ZIP64_MAGICVAL);   // Uncompressed size | 压缩前的大小(4字节)
                elen += 20;         //headid(2) + size(2) + size(8) + csize(8)
            } else {
                writeInt(e.csize);  // compressed size   | 压缩后的大小(4字节)
                writeInt(e.size);   // uncompressed size | 压缩前的大小(4字节)
            }
        }
        
        // 实体名称的字节表示
        byte[] nameBytes = zc.getBytes(e.name);
        // 写入实体名称长度
        writeShort(nameBytes.length);   // File name length | 文件名长度(2字节)
        
        int elenEXTT = 0;         // info-zip extended timestamp
        int flagEXTT = 0;
        long umtime = -1;
        long uatime = -1;
        long uctime = -1;
        
        // 文件最后修改时间(扩展区)
        if(e.mtime != null) {
            elenEXTT += 4;
            flagEXTT |= EXTT_FLAG_LMT;
            umtime = fileTimeToUnixTime(e.mtime);
        }
        
        // 文件最后访问时间(扩展区)
        if(e.atime != null) {
            elenEXTT += 4;
            flagEXTT |= EXTT_FLAG_LAT;
            uatime = fileTimeToUnixTime(e.atime);
        }
        
        // 文件创建时间(扩展区)
        if(e.ctime != null) {
            elenEXTT += 4;
            flagEXTT |= EXTT_FLAT_CT;
            uctime = fileTimeToUnixTime(e.ctime);
        }
        
        // 扩展区存在时间信息
        if(flagEXTT != 0) {
            // to use ntfs time if any m/a/ctime is beyond unixtime upper bound
            if(umtime>UPPER_UNIXTIME_BOUND || uatime>UPPER_UNIXTIME_BOUND || uctime>UPPER_UNIXTIME_BOUND) {
                elen += 36;                // NTFS time, total 36 bytes
            } else {
                elen += (elenEXTT + 5);    // headid(2) + size(2) + flag(1) + data
            }
        }
        
        writeShort(elen);   // Extra field length | 扩展区长度(2字节)
        
        writeBytes(nameBytes, 0, nameBytes.length); // File name | 文件名(n字节)
        
        /* 以下开始写入扩展区域的数据 */
        
        if(hasZip64) {
            writeShort(ZIP64_EXTID);    // 扩展区标头
            writeShort(16);
            writeLong(e.size);
            writeLong(e.csize);
        }
        
        if(flagEXTT != 0) {
            if(umtime>UPPER_UNIXTIME_BOUND || uatime>UPPER_UNIXTIME_BOUND || uctime>UPPER_UNIXTIME_BOUND) {
                writeShort(EXTID_NTFS);    // id
                writeShort(32);            // data size
                writeInt(0);               // reserved
                writeShort(0x0001);        // NTFS attr tag
                writeShort(24);
                writeLong(e.mtime == null ? WINDOWS_TIME_NOT_AVAILABLE : fileTimeToWinTime(e.mtime));
                writeLong(e.atime == null ? WINDOWS_TIME_NOT_AVAILABLE : fileTimeToWinTime(e.atime));
                writeLong(e.ctime == null ? WINDOWS_TIME_NOT_AVAILABLE : fileTimeToWinTime(e.ctime));
            } else {
                writeShort(EXTID_EXTT);
                writeShort(elenEXTT + 1);  // flag + data
                writeByte(flagEXTT);
                if(e.mtime != null) {
                    writeInt(umtime);
                }
                if(e.atime != null) {
                    writeInt(uatime);
                }
                if(e.ctime != null) {
                    writeInt(uctime);
                }
            }
        }
        
        // 写入其他扩展信息
        writeExtra(e.extra);
        
        locoff = written;
    }
    
    /**
     * Writes extra data descriptor (EXT) for specified entry.
     */
    /*
     * 写入扩展数据描述符(属于zip文件的第一部分的第3小节)
     *
     * 用于标识该文件压缩结束，该结构只有在相应的文件头信息中通用标记字段的第3位设为1时才会出现，紧接在压缩文件源数据后。
     * 这个数据描述符常用在不能对输出的zip文件进行检索时使用。
     */
    private void writeEXT(ZipEntry e) throws IOException {
        writeInt(EXTSIG);           // EXT header signature
        
        writeInt(e.crc);            // crc-32
        
        if(e.csize >= ZIP64_MAGICVAL || e.size >= ZIP64_MAGICVAL) {
            writeLong(e.csize);
            writeLong(e.size);
        } else {
            writeInt(e.csize);      // compressed size
            writeInt(e.size);       // uncompressed size
        }
    }
    
    /**
     * Write central directory (CEN) header for specified entry.
     * REMIND: add support for file attributes
     */
    /*
     * 写入zip文件的第二部分：压缩的目录源数据(核心目录数据)。
     *
     * 对于待压缩的目录而言，每一个子目录对应一个压缩目录源数据，记录该目录的描述信息。
     * 压缩包中所有目录源数据连续存储在整个压缩包的最后，这样便于向包中追加新的文件。
     */
    private void writeCEN(XEntry xentry) throws IOException {
        ZipEntry e = xentry.entry;
        int flag = e.flag;
        int version = version(e);
        long csize = e.csize;
        long size = e.size;
        long offset = xentry.offset;
        int elenZIP64 = 0;
        boolean hasZip64 = false;
        
        if(e.csize >= ZIP64_MAGICVAL) {
            csize = ZIP64_MAGICVAL;
            elenZIP64 += 8;             // csize(8)
            hasZip64 = true;
        }
        
        if(e.size >= ZIP64_MAGICVAL) {
            size = ZIP64_MAGICVAL;      // size(8)
            elenZIP64 += 8;
            hasZip64 = true;
        }
        
        if(xentry.offset >= ZIP64_MAGICVAL) {
            offset = ZIP64_MAGICVAL;
            elenZIP64 += 8;             // offset(8)
            hasZip64 = true;
        }
        
        writeInt(CENSIG);           // CEN header signature
        
        if(hasZip64) {
            writeShort(45);         // ver 4.5 for zip64
            writeShort(45);
        } else {
            writeShort(version);    // version made by
            writeShort(version);    // version needed to extract
        }
        
        writeShort(flag);           // general purpose bit flag
        writeShort(e.method);       // compression method
        writeInt(e.xdostime);       // last modification time
        writeInt(e.crc);            // crc-32
        writeInt(csize);            // compressed size
        writeInt(size);             // uncompressed size
        
        byte[] nameBytes = zc.getBytes(e.name);
        writeShort(nameBytes.length);
        
        // 返回扩展区的长度
        int elen = getExtraLen(e.extra);
        if(hasZip64) {
            elen += (elenZIP64 + 4);// + headid(2) + datasize(2)
        }
        
        // cen info-zip extended timestamp only outputs mtime but set the flag for a/ctime, if present in loc
        int flagEXTT = 0;
        long umtime = -1;
        long uatime = -1;
        long uctime = -1;
        
        if(e.mtime != null) {
            flagEXTT |= EXTT_FLAG_LMT;
            umtime = fileTimeToUnixTime(e.mtime);
        }
        
        if(e.atime != null) {
            flagEXTT |= EXTT_FLAG_LAT;
            uatime = fileTimeToUnixTime(e.atime);
        }
        
        if(e.ctime != null) {
            flagEXTT |= EXTT_FLAT_CT;
            uctime = fileTimeToUnixTime(e.ctime);
        }
        
        if(flagEXTT != 0) {
            // to use ntfs time if any m/a/ctime is beyond unixtime upper bound
            if(umtime>UPPER_UNIXTIME_BOUND || uatime>UPPER_UNIXTIME_BOUND || uctime>UPPER_UNIXTIME_BOUND) {
                elen += 36;         // NTFS time total 36 bytes
            } else {
                elen += 9;          // headid(2) + sz(2) + flag(1) + mtime (4)
            }
        }
        
        writeShort(elen);
        byte[] commentBytes;
        if(e.comment != null) {
            commentBytes = zc.getBytes(e.comment);
            writeShort(Math.min(commentBytes.length, 0xffff));
        } else {
            commentBytes = null;
            writeShort(0);
        }
        
        writeShort(0);              // starting disk number
        writeShort(0);              // internal file attributes (unused)
        writeInt(0);                // external file attributes (unused)
        writeInt(offset);           // relative offset of local header
        writeBytes(nameBytes, 0, nameBytes.length);
        
        // take care of EXTID_ZIP64 and EXTID_EXTT
        if(hasZip64) {
            writeShort(ZIP64_EXTID);// Zip64 extra
            writeShort(elenZIP64);
            
            if(size == ZIP64_MAGICVAL) {
                writeLong(e.size);
            }
            
            if(csize == ZIP64_MAGICVAL) {
                writeLong(e.csize);
            }
            
            if(offset == ZIP64_MAGICVAL) {
                writeLong(xentry.offset);
            }
        }
        
        if(flagEXTT != 0) {
            if(umtime>UPPER_UNIXTIME_BOUND || uatime>UPPER_UNIXTIME_BOUND || uctime>UPPER_UNIXTIME_BOUND) {
                writeShort(EXTID_NTFS);    // id
                writeShort(32);            // data size
                writeInt(0);               // reserved
                writeShort(0x0001);        // NTFS attr tag
                writeShort(24);
                writeLong(e.mtime == null ? WINDOWS_TIME_NOT_AVAILABLE : fileTimeToWinTime(e.mtime));
                writeLong(e.atime == null ? WINDOWS_TIME_NOT_AVAILABLE : fileTimeToWinTime(e.atime));
                writeLong(e.ctime == null ? WINDOWS_TIME_NOT_AVAILABLE : fileTimeToWinTime(e.ctime));
            } else {
                writeShort(EXTID_EXTT);
                if(e.mtime != null) {
                    writeShort(5);      // flag + mtime
                    writeByte(flagEXTT);
                    writeInt(umtime);
                } else {
                    writeShort(1);      // flag only
                    writeByte(flagEXTT);
                }
            }
        }
        
        // 写入其他扩展信息
        writeExtra(e.extra);
        
        if(commentBytes != null) {
            writeBytes(commentBytes, 0, Math.min(commentBytes.length, 0xffff));
        }
    }
    
    /**
     * Writes end of central directory (END) header.
     */
    /*
     * 写入zip文件的第三部分：目录结束标识。
     * 目录结束标识存在于整个压缩包的结尾，用于标记压缩的目录数据的结束。
     */
    private void writeEND(long off, long len) throws IOException {
        boolean hasZip64 = false;
        
        long xlen = len;
        if(xlen >= ZIP64_MAGICVAL) {
            xlen = ZIP64_MAGICVAL;
            hasZip64 = true;
        }
        
        long xoff = off;
        if(xoff >= ZIP64_MAGICVAL) {
            xoff = ZIP64_MAGICVAL;
            hasZip64 = true;
        }
        
        int count = xentries.size();
        if(count >= ZIP64_MAGICCOUNT) {
            hasZip64 |= !inhibitZip64;
            if(hasZip64) {
                count = ZIP64_MAGICCOUNT;
            }
        }
        
        if(hasZip64) {
            long off64 = written;
            //zip64 end of central directory record
            writeInt(ZIP64_ENDSIG);        // zip64 END record signature
            writeLong(ZIP64_ENDHDR - 12);  // size of zip64 end
            writeShort(45);                // version made by
            writeShort(45);                // version needed to extract
            writeInt(0);                   // number of this disk
            writeInt(0);                   // central directory start disk
            writeLong(xentries.size());    // number of directory entires on disk
            writeLong(xentries.size());    // number of directory entires
            writeLong(len);                // length of central directory
            writeLong(off);                // offset of central directory
            
            //zip64 end of central directory locator
            writeInt(ZIP64_LOCSIG);        // zip64 END locator signature
            writeInt(0);                   // zip64 END start disk
            writeLong(off64);              // offset of zip64 END
            writeInt(1);                   // total number of disks (?)
        }
        
        // 核心目录结束标识，既代表第二部分的结束，也代表第三部分的开始
        writeInt(ENDSIG);                 // END record signature
        writeShort(0);                    // number of this disk
        writeShort(0);                    // central directory start disk
        writeShort(count);                // number of directory entries on disk
        writeShort(count);                // total number of directory entries
        writeInt(xlen);                   // length of central directory
        writeInt(xoff);                   // offset of central directory
        
        // 如果存在注释
        if(comment != null) {            // zip file comment
            writeShort(comment.length);             // 压缩包注释长度
            writeBytes(comment, 0, comment.length); // 压缩包注释内容
        } else {
            writeShort(0);
        }
    }
    
    /**
     * Writes extra data without EXTT and ZIP64.
     *
     * Extra timestamp and ZIP64 data is handled/output separately in writeLOC and writeCEN.
     */
    /*
     * 写入扩展区数据。
     *
     * 扩展区数据存在于zip文件的第一部分的第1小节末尾和第二部分靠近尾部的位置。
     */
    private void writeExtra(byte[] extra) throws IOException {
        if(extra != null) {
            int len = extra.length;
            int off = 0;
            
            while(off + 4<=len) {
                int tag = get16(extra, off);
                int sz = get16(extra, off + 2);
                
                if(sz<0 || (off + 4 + sz)>len) {
                    writeBytes(extra, off, len - off);
                    return;
                }
                
                if(tag != EXTID_EXTT && tag != EXTID_ZIP64) {
                    writeBytes(extra, off, sz + 4);
                }
                
                off += (sz + 4);
            }
            
            if(off<len) {
                writeBytes(extra, off, len - off);
            }
        }
    }
    
    /**
     * Writes a 8-bit byte to the output stream.
     */
    // 向输出流写入一个byte(小端法)
    private void writeByte(int v) throws IOException {
        OutputStream out = this.out;
        out.write(v & 0xff);
        written += 1;
    }
    
    /**
     * Writes a 16-bit short to the output stream in little-endian byte order.
     */
    // 向输出流写入一个short(小端法)
    private void writeShort(int v) throws IOException {
        OutputStream out = this.out;
        out.write((v >>> 0) & 0xff);
        out.write((v >>> 8) & 0xff);
        written += 2;
    }
    
    /**
     * Writes a 32-bit int to the output stream in little-endian byte order.
     */
    // 向输出流写入一个int(小端法)
    private void writeInt(long v) throws IOException {
        OutputStream out = this.out;
        out.write((int) ((v >>> 0) & 0xff));
        out.write((int) ((v >>> 8) & 0xff));
        out.write((int) ((v >>> 16) & 0xff));
        out.write((int) ((v >>> 24) & 0xff));
        written += 4;
    }
    
    /**
     * Writes a 64-bit int to the output stream in little-endian byte order.
     */
    // 向输出流写入一个long(小端法)
    private void writeLong(long v) throws IOException {
        OutputStream out = this.out;
        out.write((int) ((v >>> 0) & 0xff));
        out.write((int) ((v >>> 8) & 0xff));
        out.write((int) ((v >>> 16) & 0xff));
        out.write((int) ((v >>> 24) & 0xff));
        out.write((int) ((v >>> 32) & 0xff));
        out.write((int) ((v >>> 40) & 0xff));
        out.write((int) ((v >>> 48) & 0xff));
        out.write((int) ((v >>> 56) & 0xff));
        written += 8;
    }
    
    /**
     * Writes an array of bytes to the output stream.
     */
    // 向输出流写入一组byte(小端法)
    private void writeBytes(byte[] b, int off, int len) throws IOException {
        super.out.write(b, off, len);
        written += len;
    }
    
    /*▲ 附加信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Checks to make sure that this stream has not been closed.
     */
    // 确保zip输出流未关闭
    private void ensureOpen() throws IOException {
        if(closed) {
            throw new IOException("Stream closed");
        }
    }
    
    // 获取对指定实体进行解压时使用的pkware版本
    private static int version(ZipEntry e) throws ZipException {
        switch(e.method) {
            case DEFLATED:
                return 20;
            case STORED:
                return 10;
            default:
                throw new ZipException("unsupported compression method");
        }
    }
    
    /**
     * Returns the length of extra data without EXTT and ZIP64.
     */
    // 返回扩展区的长度
    private int getExtraLen(byte[] extra) {
        if(extra == null) {
            return 0;
        }
        
        int skipped = 0;
        int len = extra.length;
        int off = 0;
        while(off + 4<=len) {
            int tag = get16(extra, off);
            int sz = get16(extra, off + 2);
            if(sz<0 || (off + 4 + sz)>len) {
                break;
            }
            
            if(tag == EXTID_EXTT || tag == EXTID_ZIP64) {
                skipped += (sz + 4);
            }
            
            off += (sz + 4);
        }
        
        return len - skipped;
    }
    
    
    
    
    
    
    // 待压缩实体
    private static class XEntry {
        final ZipEntry entry;   // zip实体
        final long offset;      // zip实体在输出流的偏移量
        
        public XEntry(ZipEntry entry, long offset) {
            this.entry = entry;
            this.offset = offset;
        }
    }
}
