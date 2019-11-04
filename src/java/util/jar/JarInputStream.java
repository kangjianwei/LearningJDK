/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
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
import sun.security.util.ManifestEntryVerifier;
import jdk.internal.util.jar.JarIndex;

/**
 * The <code>JarInputStream</code> class is used to read the contents of
 * a JAR file from any input stream. It extends the class
 * <code>java.util.zip.ZipInputStream</code> with support for reading
 * an optional <code>Manifest</code> entry. The <code>Manifest</code>
 * can be used to store meta-information about the JAR file and its entries.
 *
 * @author David Connelly
 * @see Manifest
 * @see java.util.zip.ZipInputStream
 * @since 1.2
 */
// jar输入流：读取jar文件，将其解压为原始数据后填充到指定的内存
public class JarInputStream extends ZipInputStream {
    
    private Manifest man;        // 如果jar具有标准结构，则用man对象来指示META-INF/MANIFEST.MF文件
    
    private JarEntry first;      // 在标准jar结构中，临时存储代表INDEX.LIST的实体
    
    private boolean tryManifest; // 是否需要在遇到META-INF/INDEX.LIST之后继续搜寻META-INF/MANIFEST.MF
    
    // 是否需要验证
    private final boolean doVerify;
    
    private JarVerifier jv;
    private ManifestEntryVerifier mev;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new <code>JarInputStream</code> and reads the optional
     * manifest. If a manifest is present, also attempts to verify
     * the signatures if the JarInputStream is signed.
     *
     * @param in the actual input stream
     *
     * @throws IOException if an I/O error has occurred
     */
    /*
     * 使用指定的源头输入流构造支持utf8字符集的jar输入流。
     * 使用的解压器具有默认解压级别，且兼容GZIP。
     */
    public JarInputStream(InputStream in) throws IOException {
        this(in, true);
    }
    
    /**
     * Creates a new <code>JarInputStream</code> and reads the optional manifest.
     * If a manifest is present and verify is true, also attempts to verify the signatures if the JarInputStream is signed.
     *
     * @param in     the actual input stream
     * @param verify whether or not to verify the JarInputStream if it is signed.
     *
     * @throws IOException if an I/O error has occurred
     */
    /*
     * 使用指定的源头输入流构造支持utf8字符集的jar输入流。
     * 使用的解压器具有默认解压级别，且兼容GZIP。
     */
    public JarInputStream(InputStream in, boolean verify) throws IOException {
        super(in);
        
        this.doVerify = verify;
        
        /*
         * This implementation assumes the META-INF/MANIFEST.MF entry should be either the first
         * or the second entry (when preceded by the dir META-INF/).
         * It skips the META-INF/ and then "consumes" the MANIFEST.MF to initialize the Manifest object.
         *
         * 此实现假定META-INF/MANIFEST.MF条目应该是第一个条目或第二个条目（在目录META-INF/之后）。
         * 它跳过META-INF/，然后"使用"MANIFEST.MF来初始化Manifest对象。
         */
        // 返回待解压实体
        JarEntry entry = (JarEntry) super.getNextEntry();
        
        // 如果必要，跳过META-INF/实体
        if(entry != null && entry.getName().equalsIgnoreCase("META-INF/")) {
            // 获取下一个条目，如果jar是标准结构，则可获取到META-INF/MANIFEST.MF实体
            entry = (JarEntry) super.getNextEntry();
        }
        
        // 检查entry，返回首个非META-INF/MANIFEST.MF的条目
        first = checkManifest(entry);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 解压前 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads the next JAR file entry and positions the stream at the
     * beginning of the entry data. If verification has been enabled,
     * any invalid signature detected while positioning the stream for
     * the next entry will result in an exception.
     *
     * @return the next JAR file entry, or null if there are no more entries
     *
     * @throws ZipException      if a ZIP file error has occurred
     * @throws IOException       if an I/O error has occurred
     * @throws SecurityException if any of the jar file entries
     *                           are incorrectly signed.
     */
    // 返回待解压JarEntry，该方法需要在每一个实体read开始之前被调用
    public JarEntry getNextJarEntry() throws IOException {
        return (JarEntry) getNextEntry();
    }
    
    /**
     * Reads the next ZIP file entry and positions the stream at the
     * beginning of the entry data. If verification has been enabled,
     * any invalid signature detected while positioning the stream for
     * the next entry will result in an exception.
     *
     * @throws ZipException      if a ZIP file error has occurred
     * @throws IOException       if an I/O error has occurred
     * @throws SecurityException if any of the jar file entries
     *                           are incorrectly signed.
     */
    // 返回待解压ZipEntry，该方法需要在每一个实体read开始之前被调用
    public ZipEntry getNextEntry() throws IOException {
        JarEntry entry;
        
        // 如果存在首个实体
        if(first != null) {
            // 记录首个实体
            entry = first;
            
            // 如果该条目是META-INF/INDEX.LIST
            if(first.getName().equalsIgnoreCase(JarIndex.INDEX_NAME)) {
                // 后续需要查找META-INF/MANIFEST.MF条目
                tryManifest = true;
            }
            
            // 无论如何，置空first域
            first = null;
        } else {
            // 获取下一个条目
            entry = (JarEntry) super.getNextEntry();
            
            if(tryManifest) {
                /*
                 * 检查entry是否为META-INF/MANIFEST.MF条目。
                 *
                 * 如果是的话，返回jar中下一个条目。
                 * 如果不是的话，直接返回entry。
                 */
                entry = checkManifest(entry);
                
                // 仅检查这一次，所以要求jar的结构必须合规
                tryManifest = false;
            }
        }
        
        if(jv != null && entry != null) {
            /*
             * At this point, we might have parsed all the meta-inf entries and have nothing to verify.
             * If we have nothing to verify, get rid of the JarVerifier object.
             */
            if(jv.nothingToVerify()) {
                jv = null;
                mev = null;
            } else {
                jv.beginEntry(entry, mev);
            }
        }
        
        return entry;
    }
    
    /*▲ 解压前 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/解压 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads from the current JAR file entry into an array of bytes.
     * If <code>len</code> is not zero, the method
     * blocks until some input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
     * If verification has been enabled, any invalid signature
     * on the current entry will be reported at some point before the
     * end of the entry is reached.
     *
     * @param b   the buffer into which the data is read
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes to read
     *
     * @return the actual number of bytes read, or -1 if the end of the
     * entry is reached
     *
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws ZipException              if a ZIP file error has occurred
     * @throws IOException               if an I/O error has occurred
     * @throws SecurityException         if any of the jar file entries
     *                                   are incorrectly signed.
     */
    /*
     * 从当前jar文件的某个条目中读取len个解压后的字节，并将其存储到b的off处
     *
     * 返回值表示实际得到的解压后的字节数，如果返回-1，表示已经没有可解压字节，
     * 但不代表解压器缓冲区内或输入流中没有字节(因为有些数据并不需要解压)。
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int n;
        
        // "首个"条目解析完之后，可以对当前条目进行解压操作，并将解压后的len个字节写入b的off处
        if(first == null) {
            n = super.read(b, off, len);
        } else {
            n = -1;
        }
        
        if(jv != null) {
            jv.update(n, b, off, len, mev);
        }
        
        return n;
    }
    
    /*▲ 读/解压 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns the <code>Manifest</code> for this JAR file, or
     * <code>null</code> if none.
     *
     * @return the <code>Manifest</code> for this JAR file, or
     * <code>null</code> if none.
     */
    // 获取jar中的MANIFEST.MF文件
    public Manifest getManifest() {
        return man;
    }
    
    /**
     * Creates a new <code>JarEntry</code> (<code>ZipEntry</code>) for the
     * specified JAR file entry name. The manifest attributes of
     * the specified JAR file entry name will be copied to the new
     * <CODE>JarEntry</CODE>.
     *
     * @param name the name of the JAR/ZIP file entry
     *
     * @return the <code>JarEntry</code> object just created
     */
    // 创建指定名称的实体
    protected ZipEntry createZipEntry(String name) {
        JarEntry e = new JarEntry(name);
        
        if(man != null) {
            e.attr = man.getAttributes(name);
        }
        
        return e;
    }
    
    /*
     * 检查entry是否为META-INF/MANIFEST.MF条目。
     *
     * 如果是的话，返回jar中下一个条目。
     * 如果不是的话，直接返回entry。
     */
    private JarEntry checkManifest(JarEntry entry) throws IOException {
        
        // 如果e是META-INF/MANIFEST.MF条目，则为其创建Manifest对象
        if(entry != null && JarFile.MANIFEST_NAME.equalsIgnoreCase(entry.getName())) {
            man = new Manifest();
            
            // 获取MANIFEST.MF中的内容
            byte[] bytes = getBytes(new BufferedInputStream(this));
            
            // 根据MANIFEST.MF中的内容来初始化Manifest对象
            man.read(new ByteArrayInputStream(bytes));
            
            // 关闭/保存当前待解压实体以便进行下一个实体的读取
            closeEntry();
            
            // 需要做验证
            if(doVerify) {
                jv = new JarVerifier(bytes);
                mev = new ManifestEntryVerifier(man);
            }
            
            // 返回下一个条目
            return (JarEntry) super.getNextEntry();
        }
        
        return entry;
    }
    
    // 返回输入流is中的所有字节
    private byte[] getBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        
        int n;
        while((n = is.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, n);
        }
        
        return baos.toByteArray();
    }
    
}
