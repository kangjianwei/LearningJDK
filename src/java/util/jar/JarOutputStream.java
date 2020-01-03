/*
 * Copyright (c) 1997, 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.util.jar;

import java.util.zip.*;
import java.io.*;

/**
 * The <code>JarOutputStream</code> class is used to write the contents
 * of a JAR file to any output stream. It extends the class
 * <code>java.util.zip.ZipOutputStream</code> with support
 * for writing an optional <code>Manifest</code> entry. The
 * <code>Manifest</code> can be used to specify meta-information about
 * the JAR file and its entries.
 *
 * @author David Connelly
 * @see Manifest
 * @see java.util.zip.ZipOutputStream
 * @since 1.2
 */
// jar输出流：读取指定内存处的原始数据，将其压缩为jar文件后写入最终输出流
public class JarOutputStream extends ZipOutputStream {
    
    private static final int JAR_MAGIC = 0xCAFE;    // jar压缩文件开头的魔数
    
    private boolean firstEntry = true;  // 标记当前jar中压入的首个文件
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new <code>JarOutputStream</code> with no manifest.
     *
     * @param out the actual output stream
     *
     * @throws IOException if an I/O error has occurred
     */
    /*
     * 用指定的最终输出流构造使用utf8字符集的jar输出流。
     * 使用的压缩器具有默认压缩级别，且兼容gzip。
     */
    public JarOutputStream(OutputStream out) throws IOException {
        super(out);
    }
    
    /**
     * Creates a new <code>JarOutputStream</code> with the specified
     * <code>Manifest</code>. The manifest is written as the first
     * entry to the output stream.
     *
     * @param out the actual output stream
     * @param man the optional <code>Manifest</code>
     *
     * @throws IOException if an I/O error has occurred
     */
    /*
     * 用指定的最终输出流构造使用utf8字符集的jar输出流。
     * 使用的压缩器具有默认压缩级别，且兼容gzip。
     *
     * 注意此处添加了一个Manifest文件，它将作为首个条目被压缩到jar中。
     */
    public JarOutputStream(OutputStream out, Manifest man) throws IOException {
        super(out);
        
        if(man == null) {
            throw new NullPointerException("man");
        }
        
        // 创建名为META-INF/MANIFEST.MF的实体
        ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
        
        // 初始化待压缩实体
        putNextEntry(e);
        
        // 将待压缩实体压缩到jar输出流
        man.write(new BufferedOutputStream(this));
        
        // 关闭/保存当前压缩实体以便进行下一个实体的写入
        closeEntry();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩前 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Begins writing a new JAR file entry and positions the stream
     * to the start of the entry data. This method will also close
     * any previous entry. The default compression method will be
     * used if no compression method was specified for the entry.
     * The current time will be used if the entry has no set modification
     * time.
     *
     * @param ze the ZIP/JAR entry to be written
     *
     * @throws ZipException if a ZIP error has occurred
     * @throws IOException  if an I/O error has occurred
     */
    /*
     * 初始化待压缩实体。
     *
     * 该方法需要在每一个实体write开始之前被调用。
     * 此方法包含了对文件头信息(属于zip文件的第一部分的第1小节)的写入。
     */
    public void putNextEntry(ZipEntry ze) throws IOException {
        // 如果是jar中首个实体，可能是Manifest文件，也可能是普通文件
        if(firstEntry) {
            // Make sure that extra field data for first JAR entry includes JAR magic number id.
            byte[] edata = ze.getExtra();   // 返回当前实体的附加信息
            
            if(edata == null || !hasMagic(edata)) {
                // 如果不存在附加信息
                if(edata == null) {
                    edata = new byte[4];
                    
                    // 如果附加信息中不包含特定的jar魔数
                } else {
                    // Prepend magic to existing extra data
                    byte[] tmp = new byte[edata.length + 4];
                    System.arraycopy(edata, 0, tmp, 4, edata.length);
                    edata = tmp;
                }
                
                // 向附加信息中添加jar魔数
                set16(edata, 0, JAR_MAGIC); // extra field id
                set16(edata, 2, 0);         // extra field size
                
                // 为待压缩实体更新/添加附加信息
                ze.setExtra(edata);
            }
            
            firstEntry = false;
        }
        
        super.putNextEntry(ze);
    }
    
    /*▲ 压缩前 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*
     * Returns true if specified byte array contains the jar magic extra field id.
     */
    // 判断字节数组edata中是否包含特定的jar魔数
    private static boolean hasMagic(byte[] edata) {
        try {
            int i = 0;
            while(i<edata.length) {
                if(get16(edata, i) == JAR_MAGIC) {
                    return true;
                }
                i += get16(edata, i + 2) + 4;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            // Invalid extra field data
        }
        return false;
    }
    
    /*
     * Fetches unsigned 16-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    private static int get16(byte[] b, int off) {
        return Byte.toUnsignedInt(b[off]) | (Byte.toUnsignedInt(b[off + 1]) << 8);
    }
    
    /*
     * Sets 16-bit value at specified offset. The bytes are assumed to
     * be in Intel (little-endian) byte order.
     */
    private static void set16(byte[] b, int off, int value) {
        b[off + 0] = (byte) value;
        b[off + 1] = (byte) (value >> 8);
    }
    
}
