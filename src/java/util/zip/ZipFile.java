/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.JavaUtilZipFileAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.VM;
import jdk.internal.perf.PerfCounter;
import jdk.internal.ref.CleanerFactory;
import jdk.internal.vm.annotation.Stable;

import static java.util.zip.ZipConstants64.EXTID_ZIP64;
import static java.util.zip.ZipConstants64.USE_UTF8;
import static java.util.zip.ZipConstants64.ZIP64_ENDHDR;
import static java.util.zip.ZipConstants64.ZIP64_ENDSIG;
import static java.util.zip.ZipConstants64.ZIP64_LOCHDR;
import static java.util.zip.ZipConstants64.ZIP64_LOCSIG;
import static java.util.zip.ZipConstants64.ZIP64_MAGICCOUNT;
import static java.util.zip.ZipConstants64.ZIP64_MAGICVAL;
import static java.util.zip.ZipUtils.CENCOM;
import static java.util.zip.ZipUtils.CENCRC;
import static java.util.zip.ZipUtils.CENEXT;
import static java.util.zip.ZipUtils.CENFLG;
import static java.util.zip.ZipUtils.CENHOW;
import static java.util.zip.ZipUtils.CENLEN;
import static java.util.zip.ZipUtils.CENNAM;
import static java.util.zip.ZipUtils.CENOFF;
import static java.util.zip.ZipUtils.CENSIG;
import static java.util.zip.ZipUtils.CENSIZ;
import static java.util.zip.ZipUtils.CENTIM;
import static java.util.zip.ZipUtils.ENDCOM;
import static java.util.zip.ZipUtils.ENDOFF;
import static java.util.zip.ZipUtils.ENDSIZ;
import static java.util.zip.ZipUtils.ENDTOT;
import static java.util.zip.ZipUtils.END_MAXLEN;
import static java.util.zip.ZipUtils.GETSIG;
import static java.util.zip.ZipUtils.LOCEXT;
import static java.util.zip.ZipUtils.LOCNAM;
import static java.util.zip.ZipUtils.LOCSIG;
import static java.util.zip.ZipUtils.READBLOCKSZ;
import static java.util.zip.ZipUtils.ZIP64_ENDOFF;
import static java.util.zip.ZipUtils.ZIP64_ENDSIZ;
import static java.util.zip.ZipUtils.ZIP64_ENDTOT;
import static java.util.zip.ZipUtils.ZIP64_LOCOFF;
import static java.util.zip.ZipUtils.get16;
import static java.util.zip.ZipUtils.get64;

/**
 * This class is used to read entries from a zip file.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * @author David Connelly
 * @apiNote To release resources used by this {@code ZipFile}, the {@link #close()} method
 * should be called explicitly or by try-with-resources. Subclasses are responsible
 * for the cleanup of resources acquired by the subclass. Subclasses that override
 * {@link #finalize()} in order to perform cleanup should be modified to use alternative
 * cleanup mechanisms such as {@link java.lang.ref.Cleaner} and remove the overriding
 * {@code finalize} method.
 * @implSpec If this {@code ZipFile} has been subclassed and the {@code close} method has
 * been overridden, the {@code close} method will be called by the finalization
 * when {@code ZipFile} is unreachable. But the subclasses should not depend on
 * this specific implementation; the finalization is not reliable and the
 * {@code finalize} method is deprecated to be removed.
 * @since 1.1
 */
/*
 * zip文件，适用于读取具有完整zip结构的压缩文件
 *
 * 使用该类在解压zip文件时，需要借助zip文件的核心目录信息(zip文件的第二部分)，
 * 如果zip文件的结构是不完整的，如缺失了第二部分，那么该类无法正确识别zip实体信息。
 */
public class ZipFile implements ZipConstants, Closeable {
    
    /**
     * Mode flag to open a zip file for reading.
     */
    public static final int OPEN_READ = 0x1;    // 只读
    
    /**
     * Mode flag to open a zip file and mark it for deletion.  The file will be
     * deleted some time between the moment that it is opened and the moment
     * that it is closed, but its contents will remain accessible via the
     * {@code ZipFile} object until either the close method is invoked or the
     * virtual machine exits.
     */
    public static final int OPEN_DELETE = 0x4;  // 删除，即解压后删除压缩包
    
    private static final int STORED = ZipEntry.STORED;
    private static final int DEFLATED = ZipEntry.DEFLATED;
    
    private static final JavaLangAccess JLA;
    
    private static boolean isWindows; // 当前是否处于windows系统
    
    private final String name; // zip文件原始名称(路径)
    
    @Stable
    private final ZipCoder zc; // zip编/解码器
    
    // The "resource" used by this zip file that needs to be
    // cleaned after use.
    // a) the input streams that need to be closed
    // b) the list of cached Inflater objects
    // c) the "native" source of this zip file.
    @Stable
    private final CleanableResource res; // 资源清理器
    
    private volatile boolean closeRequested; // 当前zip文件是否已关闭
    
    private String lastEntryName; // 最后访问的待压缩实体的名称（文件相对路径）
    private int lastEntryPos;     // 最后访问的的实体信息在核心目录表中的偏移量
    
    
    
    static {
        SharedSecrets.setJavaUtilZipFileAccess(new JavaUtilZipFileAccess() {
            @Override
            public boolean startsWithLocHeader(ZipFile zip) {
                return zip.res.zsrc.startsWithLoc;
            }
            
            @Override
            public String[] getMetaInfEntryNames(ZipFile zip) {
                return zip.getMetaInfEntryNames();
            }
            
            @Override
            public JarEntry getEntry(ZipFile zip, String name, Function<String, JarEntry> func) {
                return (JarEntry) zip.getEntry(name, func);
            }
            
            @Override
            public Enumeration<JarEntry> entries(ZipFile zip, Function<String, JarEntry> func) {
                return zip.entries(func);
            }
            
            @Override
            public Stream<JarEntry> stream(ZipFile zip, Function<String, JarEntry> func) {
                return zip.stream(func);
            }
            
            @Override
            public Stream<String> entryNameStream(ZipFile zip) {
                return zip.entryNameStream();
            }
        });
        
        JLA = jdk.internal.misc.SharedSecrets.getJavaLangAccess();
        
        isWindows = VM.getSavedProperty("os.name").contains("Windows");
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens a zip file for reading.
     *
     * <p>First, if there is a security manager, its {@code checkRead}
     * method is called with the {@code name} argument as its argument
     * to ensure the read is allowed.
     *
     * <p>The UTF-8 {@link java.nio.charset.Charset charset} is used to
     * decode the entry names and comments.
     *
     * @param name the name of the zip file
     *
     * @throws ZipException      if a ZIP format error has occurred
     * @throws IOException       if an I/O error has occurred
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkRead} method doesn't allow read access to the file.
     * @see SecurityManager#checkRead(java.lang.String)
     */
    // 打开指定名称的ZipFile，使用UTF8编码作为解压字符集
    public ZipFile(String name) throws IOException {
        this(new File(name), OPEN_READ);
    }
    
    /**
     * Opens a zip file for reading.
     *
     * <p>First, if there is a security manager, its {@code checkRead}
     * method is called with the {@code name} argument as its argument
     * to ensure the read is allowed.
     *
     * @param name    the name of the zip file
     * @param charset the {@linkplain java.nio.charset.Charset charset} to
     *                be used to decode the ZIP entry name and comment that are not
     *                encoded by using UTF-8 encoding (indicated by entry's general
     *                purpose flag).
     *
     * @throws ZipException      if a ZIP format error has occurred
     * @throws IOException       if an I/O error has occurred
     * @throws SecurityException if a security manager exists and its {@code checkRead}
     *                           method doesn't allow read access to the file
     * @see SecurityManager#checkRead(java.lang.String)
     * @since 1.7
     */
    // 打开指定名称的ZipFile，使用charset作为解压字符集
    public ZipFile(String name, Charset charset) throws IOException {
        this(new File(name), OPEN_READ, charset);
    }
    
    /**
     * Opens a ZIP file for reading given the specified File object.
     *
     * <p>The UTF-8 {@link java.nio.charset.Charset charset} is used to
     * decode the entry names and comments.
     *
     * @param file the ZIP file to be opened for reading
     *
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException  if an I/O error has occurred
     */
    // 打开指定的ZipFile，使用UTF8编码作为解压字符集
    public ZipFile(File file) throws ZipException, IOException {
        this(file, OPEN_READ);
    }
    
    /**
     * Opens a new {@code ZipFile} to read from the specified {@code File} object in the specified mode.
     * The mode argument must be either {@code OPEN_READ} or {@code OPEN_READ | OPEN_DELETE}.
     *
     * <p>First, if there is a security manager, its {@code checkRead}
     * method is called with the {@code name} argument as its argument to
     * ensure the read is allowed.
     *
     * <p>The UTF-8 {@link java.nio.charset.Charset charset} is used to
     * decode the entry names and comments
     *
     * @param file the ZIP file to be opened for reading
     * @param mode the mode in which the file is to be opened
     *
     * @throws ZipException             if a ZIP format error has occurred
     * @throws IOException              if an I/O error has occurred
     * @throws SecurityException        if a security manager exists and
     *                                  its {@code checkRead} method
     *                                  doesn't allow read access to the file,
     *                                  or its {@code checkDelete} method doesn't allow deleting
     *                                  the file when the {@code OPEN_DELETE} flag is set.
     * @throws IllegalArgumentException if the {@code mode} argument is invalid
     * @see SecurityManager#checkRead(java.lang.String)
     * @since 1.3
     */
    /*
     * 打开指定的ZipFile，使用UTF8编码作为解压字符集
     * mode指示打开模式，一般为OPEN_READ或OPEN_READ|OPEN_DELETE
     */
    public ZipFile(File file, int mode) throws IOException {
        this(file, mode, StandardCharsets.UTF_8);
    }
    
    /**
     * Opens a ZIP file for reading given the specified File object.
     *
     * @param file    the ZIP file to be opened for reading
     * @param charset The {@linkplain java.nio.charset.Charset charset} to be
     *                used to decode the ZIP entry name and comment (ignored if
     *                the <a href="package-summary.html#lang_encoding"> language
     *                encoding bit</a> of the ZIP entry's general purpose bit
     *                flag is set).
     *
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException  if an I/O error has occurred
     * @since 1.7
     */
    // 打开指定的ZipFile，使用charset作为解压字符集
    public ZipFile(File file, Charset charset) throws IOException {
        this(file, OPEN_READ, charset);
    }
    
    /**
     * Opens a new {@code ZipFile} to read from the specified {@code File} object in the specified mode.
     * The mode argument must be either {@code OPEN_READ} or {@code OPEN_READ | OPEN_DELETE}.
     *
     * <p>First, if there is a security manager, its {@code checkRead}
     * method is called with the {@code name} argument as its argument to
     * ensure the read is allowed.
     *
     * @param file    the ZIP file to be opened for reading
     * @param mode    the mode in which the file is to be opened
     * @param charset the {@linkplain java.nio.charset.Charset charset} to
     *                be used to decode the ZIP entry name and comment that are not
     *                encoded by using UTF-8 encoding (indicated by entry's general
     *                purpose flag).
     *
     * @throws ZipException             if a ZIP format error has occurred
     * @throws IOException              if an I/O error has occurred
     * @throws SecurityException        if a security manager exists and its {@code checkRead}
     *                                  method doesn't allow read access to the file,or its
     *                                  {@code checkDelete} method doesn't allow deleting the
     *                                  file when the {@code OPEN_DELETE} flag is set
     * @throws IllegalArgumentException if the {@code mode} argument is invalid
     * @see SecurityManager#checkRead(java.lang.String)
     * @since 1.7
     */
    /*
     * 打开指定的ZipFile，使用charset作为解压字符集
     * mode指示打开模式，一般为OPEN_READ或OPEN_READ|OPEN_DELETE
     */
    public ZipFile(File file, int mode, Charset charset) throws IOException {
        if(((mode & OPEN_READ) == 0) || ((mode & ~(OPEN_READ | OPEN_DELETE)) != 0)) {
            throw new IllegalArgumentException("Illegal mode: 0x" + Integer.toHexString(mode));
        }
        
        // 获取【文件/目录】的原始路径
        String name = file.getPath();
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkRead(name);
            if((mode & OPEN_DELETE) != 0) {
                sm.checkDelete(name);
            }
        }
        
        Objects.requireNonNull(charset, "charset");
        
        this.zc = ZipCoder.get(charset);
        this.name = name;
        long t0 = System.nanoTime();
        
        this.res = CleanableResource.get(this, file, mode);
        
        PerfCounter.getZipFileOpenTime().addElapsedTimeFrom(t0);
        PerfCounter.getZipFileCount().increment();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the path name of the ZIP file.
     *
     * @return the path name of the ZIP file
     */
    // 返回(整个)zip文件原始名称(路径)
    public String getName() {
        return name;
    }
    
    /**
     * Returns the zip file comment, or null if none.
     *
     * @return the comment string for the zip file, or null if none
     *
     * @throws IllegalStateException if the zip file has been closed
     * @since 1.7
     */
    // 返回(整个)zip文件的注释信息
    public String getComment() {
        synchronized(this) {
            ensureOpen();
            
            if(res.zsrc.comment == null) {
                return null;
            }
            
            return zc.toString(res.zsrc.comment);
        }
    }
    
    /**
     * Returns the zip file entry for the specified name, or null
     * if not found.
     *
     * @param name the name of the entry
     *
     * @return the zip file entry, or null if not found
     *
     * @throws IllegalStateException if the zip file has been closed
     */
    // 返回zip文件中指定名称的实体信息
    public ZipEntry getEntry(String name) {
        return getEntry(name, ZipEntry::new);
    }
    
    /**
     * Returns an input stream for reading the contents of the specified
     * zip file entry.
     * <p>
     * Closing this ZIP file will, in turn, close all input streams that
     * have been returned by invocations of this method.
     *
     * @param entry the zip file entry
     *
     * @return the input stream for reading the contents of the specified
     * zip file entry.
     *
     * @throws ZipException          if a ZIP format error has occurred
     * @throws IOException           if an I/O error has occurred
     * @throws IllegalStateException if the zip file has been closed
     */
    // 返回针对指定ZipEntry条目的(解压)输入流，可从中读取解压后的数据
    public InputStream getInputStream(ZipEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        
        int pos = -1;   // 指定实体的偏移位置
        Source zsrc = res.zsrc;
        Set<InputStream> istreams = res.istreams;
        
        synchronized(this) {
            ensureOpen();
            
            if(Objects.equals(lastEntryName, entry.name)) {
                pos = lastEntryPos;
            } else if(!zc.isUTF8() && (entry.flag & USE_UTF8) != 0) {
                pos = zsrc.getEntryPos(zc.getBytesUTF8(entry.name), false);
            } else {
                pos = zsrc.getEntryPos(zc.getBytes(entry.name), false);
            }
            
            if(pos == -1) {
                return null;
            }
            
            // 返回针对指定ZipEntry的输入流
            ZipFileInputStream in = new ZipFileInputStream(zsrc.cen, pos);
            
            // 判断该实体的压缩/解压方式
            switch(CENHOW(zsrc.cen, pos)) {
                case STORED:
                    synchronized(istreams) {
                        istreams.add(in);
                    }
                    
                    // 对于未压缩的zip文件(没有本地文件头与数据描述符信息)，直接返回输入流就可以读取
                    return in;
                
                case DEFLATED:
                    /*
                     * Inflater likes a bit of slack
                     * MORE: Compute good size for inflater stream:
                     */
                    long size = CENLEN(zsrc.cen, pos) + 2;
                    if(size>65536) {
                        size = 8192;
                    }
                    
                    if(size<=0) {
                        size = 4096;
                    }
                    
                    // 构造(解压)输入流
                    InputStream is = new ZipFileInflaterInputStream(in, res, (int) size);
                    synchronized(istreams) {
                        istreams.add(is);
                    }
                    
                    // 对于压缩过的zip文件，需要返回对应的(解压)输入流才能对其解压
                    return is;
                
                default:
                    throw new ZipException("invalid compression method");
            }
        }
    }
    
    /**
     * Returns the number of entries in the ZIP file.
     *
     * @return the number of entries in the ZIP file
     *
     * @throws IllegalStateException if the zip file has been closed
     */
    // 返回实体数目
    public int size() {
        synchronized(this) {
            ensureOpen();
            return res.zsrc.total;
        }
    }
    
    /*▲ get ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an enumeration of the ZIP file entries.
     *
     * @return an enumeration of the ZIP file entries
     *
     * @throws IllegalStateException if the zip file has been closed
     */
    // 返回一个ZipEntry实体迭代器
    public Enumeration<? extends ZipEntry> entries() {
        synchronized(this) {
            ensureOpen();
            return new ZipEntryIterator<ZipEntry>(res.zsrc.total, ZipEntry::new);
        }
    }
    
    /**
     * Returns an ordered {@code Stream} over the ZIP file entries.
     *
     * Entries appear in the {@code Stream} in the order they appear in
     * the central directory of the ZIP file.
     *
     * @return an ordered {@code Stream} of entries in this ZIP file
     *
     * @throws IllegalStateException if the zip file has been closed
     * @since 1.8
     */
    // 返回ZipEntry实体流
    public Stream<? extends ZipEntry> stream() {
        synchronized(this) {
            ensureOpen();
            EntrySpliterator<ZipEntry> spliterator = new EntrySpliterator<>(0, res.zsrc.total, pos -> getZipEntry(null, null, pos, ZipEntry::new));
            return StreamSupport.stream(spliterator, false);
        }
    }
    
    /*▲ 序列 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes the ZIP file.
     *
     * <p> Closing this ZIP file will close all of the input streams
     * previously returned by invocations of the {@link #getInputStream
     * getInputStream} method.
     *
     * @throws IOException if an I/O error has occurred
     */
    // 关闭zip文件
    public void close() throws IOException {
        if(closeRequested) {
            return;
        }
        
        closeRequested = true;
        
        synchronized(this) {
            // Close streams, release their inflaters, release cached inflaters
            // and release zip source
            try {
                res.clean();
            } catch(UncheckedIOException ioe) {
                throw ioe.getCause();
            }
        }
    }
    
    /**
     * Ensures that the system resources held by this ZipFile object are
     * released when there are no more references to it.
     *
     * @throws IOException if an I/O error has occurred
     * @deprecated The {@code finalize} method has been deprecated and will be
     * removed. It is implemented as a no-op. Subclasses that override
     * {@code finalize} in order to perform cleanup should be modified to
     * use alternative cleanup mechanisms and to remove the overriding
     * {@code finalize} method. The recommended cleanup for ZipFile object
     * is to explicitly invoke {@code close} method when it is no longer in
     * use, or use try-with-resources. If the {@code close} is not invoked
     * explicitly the resources held by this object will be released when
     * the instance becomes unreachable.
     */
    @Deprecated(since = "9", forRemoval = true)
    protected void finalize() throws IOException {
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 返回指定偏移处的实体的名称
    private String getEntryName(int pos) {
        byte[] cen = res.zsrc.cen;
        
        int nlen = CENNAM(cen, pos);
        
        if(!zc.isUTF8() && (CENFLG(cen, pos) & USE_UTF8) != 0) {
            return zc.toStringUTF8(cen, pos + CENHDR, nlen);
        } else {
            return zc.toString(cen, pos + CENHDR, nlen);
        }
    }
    
    /** Checks ensureOpen() before invoke this method */
    // 返回指定偏移处名为name的实体条目
    private ZipEntry getZipEntry(String name, byte[] bname, int pos, Function<String, ? extends ZipEntry> func) {
        byte[] cen = res.zsrc.cen;
        
        int nlen = CENNAM(cen, pos);    // 实体名长度
        int elen = CENEXT(cen, pos);    // 扩展区长度
        int clen = CENCOM(cen, pos);    // 实体注释长度
        int flag = CENFLG(cen, pos);    // 通用位标记
        
        if(name == null || bname.length != nlen) {
            /*
             * to use the entry name stored in cen, if the passed in name is
             * (1) null, invoked from iterator, or
             * (2) not equal to the name stored, a slash is appended during getEntryPos() search.
             */
            if(!zc.isUTF8() && (flag & USE_UTF8) != 0) {
                name = zc.toStringUTF8(cen, pos + CENHDR, nlen);
            } else {
                name = zc.toString(cen, pos + CENHDR, nlen);
            }
        }
        
        // ZipEntry e = new ZipEntry(name);
        ZipEntry e = func.apply(name);
        
        e.flag = flag;                  // 通用位标记
        e.xdostime = CENTIM(cen, pos);  // 文件最后修改时间(日期)
        e.crc = CENCRC(cen, pos);       // crc-32校验码
        e.size = CENLEN(cen, pos);      // 压缩前的大小
        e.csize = CENSIZ(cen, pos);     // 压缩后的大小
        e.method = CENHOW(cen, pos);    // 压缩方法
        
        if(elen != 0) {
            int start = pos + CENHDR + nlen;
            e.setExtra0(Arrays.copyOfRange(cen, start, start + elen), true);
        }
        
        if(clen != 0) {
            int start = pos + CENHDR + nlen + elen;
            if(!zc.isUTF8() && (flag & USE_UTF8) != 0) {
                e.comment = zc.toStringUTF8(cen, start, clen);
            } else {
                e.comment = zc.toString(cen, start, clen);
            }
        }
        
        lastEntryName = e.name;
        lastEntryPos = pos;
        
        return e;
    }
    
    /**
     * Returns the zip file entry for the specified name, or null if not found.
     *
     * @param name the name of the entry
     * @param func the function that creates the returned entry
     *
     * *@return the zip file entry, or null if not found
     * @throws IllegalStateException if the zip file has been closed
     */
    // 返回指定名称的ZipEntry实体信息
    private ZipEntry getEntry(String name, Function<String, ? extends ZipEntry> func) {
        Objects.requireNonNull(name, "name");
        
        synchronized(this) {
            ensureOpen();
            
            // 编码：将字符串中的字符编码为字节后返回
            byte[] bname = zc.getBytes(name);
            
            // 返回拥有指定名称的实体信息在核心目录表中的偏移量
            int pos = res.zsrc.getEntryPos(bname, true);
            if(pos != -1) {
                // 返回指定偏移处名为name的实体条目
                return getZipEntry(name, bname, pos, func);
            }
        }
        
        return null;
    }
    
    // 返回一个JarEntry实体迭代器
    private Enumeration<JarEntry> entries(Function<String, JarEntry> func) {
        synchronized(this) {
            ensureOpen();
            return new ZipEntryIterator<JarEntry>(res.zsrc.total, func);
        }
    }
    
    /**
     * Returns an ordered {@code Stream} over the zip file entries.
     *
     * Entries appear in the {@code Stream} in the order they appear in
     * the central directory of the jar file.
     *
     * @param func the function that creates the returned entry
     * @return an ordered {@code Stream} of entries in this zip file
     * @throws IllegalStateException if the zip file has been closed
     * @since 10
     */
    // 返回JarEntry实体流
    private Stream<JarEntry> stream(Function<String, JarEntry> func) {
        synchronized(this) {
            ensureOpen();
            EntrySpliterator<JarEntry> spliterator = new EntrySpliterator<>(0, res.zsrc.total, pos -> (JarEntry) getZipEntry(null, null, pos, func));
            return StreamSupport.stream(spliterator, false);
        }
    }
    
    /**
     * Returns an ordered {@code Stream} over the zip file entry names.
     *
     * Entry names appear in the {@code Stream} in the order they appear in
     * the central directory of the ZIP file.
     *
     * @return an ordered {@code Stream} of entry names in this zip file
     * @throws IllegalStateException if the zip file has been closed
     * @since 10
     */
    // 实体名称流
    private Stream<String> entryNameStream() {
        synchronized(this) {
            ensureOpen();
            EntrySpliterator<String> spliterator = new EntrySpliterator<>(0, res.zsrc.total, this::getEntryName);
            return StreamSupport.stream(spliterator, false);
        }
    }
    
    /**
     * Returns the names of all non-directory entries that begin with
     * "META-INF/" (case ignored). This method is used in JarFile, via
     * SharedSecrets, as an optimization when looking up manifest and
     * signature file entries. Returns null if no entries were found.
     */
    // 返回元数据名称(路径)列表(即META-INF(子)目录内的文件名称)
    private String[] getMetaInfEntryNames() {
        synchronized(this) {
            ensureOpen();
            
            // zip文件的元数据
            Source zsrc = res.zsrc;
            if(zsrc.metanames == null) {
                return null;
            }
            
            String[] names = new String[zsrc.metanames.length];
            byte[] cen = zsrc.cen;
            for(int i = 0; i<names.length; i++) {
                int pos = zsrc.metanames[i];
                names[i] = new String(cen, pos + CENHDR, CENNAM(cen, pos), StandardCharsets.UTF_8);
            }
            
            return names;
        }
    }
    
    private void ensureOpen() {
        if(closeRequested) {
            throw new IllegalStateException("zip file closed");
        }
        if(res.zsrc == null) {
            throw new IllegalStateException("The object is not initialized.");
        }
    }
    
    private void ensureOpenOrZipException() throws IOException {
        if(closeRequested) {
            throw new ZipException("ZipFile closed");
        }
    }
    
    
    
    
    
    
    // zip文件的元数据
    private static class Source {
        private static final int ZIP_ENDCHAIN = -1;
        private static final int BUF_SIZE = 8192;
        
        // zip文件
        private RandomAccessFile zfile;      // zfile of the underlying zip file
        
        // zip文件及其基础文件属性的映射
        private final Key key;               // the key in files
        
        // 缓存key和对应的Source
        private static final HashMap<Key, Source> files = new HashMap<>();
        
        // 记录缓存命中次数
        private int refs = 1;
        
        // (整个)zip文件的注释信息
        private byte[] comment;              // zip file comment
        
        // 记录zip文件第二部分的全部内容和第三部分的起始标记
        private byte[] cen;                  // CEN & ENDHDR
        
        // 空隙
        private long locpos;                 // position of first LOC header (usually 0)
        
        // 实体数量
        private int total;                   // total number of entries
        
        // 元数据信息列表(即META-INF(子)目录内的文件实体对应的核心目录信息)
        private int[] metanames;             // list of meta entries in META-INF dir
        
        /**
         * A Hashmap for all entries.
         * A cen entry of Zip/JAR file. As we have one for every entry in every active Zip/JAR,
         * We might have a lot of these in a typical system. In order to save space we don't
         * keep the name in memory, but merely remember a 32 bit {@code hash} value of the
         * entry name and its offset {@code pos} in the central directory hdeader.
         * private static class Entry {
         *     int hash;       // 32 bit hashcode on name
         *     int next;       // hash chain: index into entries
         *     int pos;        // Offset of central directory file header
         * }
         * private Entry[] entries;             // array of hashed cen entry
         * To reduce the total size of entries further, we use a int[] here to store 3 "int"
         * {@code hash}, {@code next and {@code "pos for each entry. The entry can then be
         * referred by their index of their positions in the {@code entries}.
         */
        // 实体信息列表
        private int[] entries;               // array of hashed cen entry
        
        // 哈希表，记录实体信息位置
        private int[] table;                 // Hash chain heads: indexes into entries
        // 哈希表长度
        private int tablelen;                // number of hash heads
        
        // 该zip文件是否以LOCSIG标记(属于zip文件的第一部分的第1小节)开头
        private final boolean startsWithLoc; // true, if zip file starts with LOCSIG (usually true)
        
        
        // 构造zip文件的元数据，toDelete指示zip解压后是否将原压缩包删除
        private Source(Key key, boolean toDelete) throws IOException {
            this.key = key;
            
            // zip访问结束后需要将其删除
            if(toDelete) {
                // 在windows系统上，靠着O_TEMPORARY参数完成访问后删除这个动作
                if(isWindows) {
                    this.zfile = SharedSecrets.getJavaIORandomAccessFileAccess().openAndDelete(key.file, "r");
                    
                    // 非windows系统上，不支持O_TEMPORARY参数，所以在缓存中打开zip后，直接将其原文件删除
                } else {
                    this.zfile = new RandomAccessFile(key.file, "r");
                    key.file.delete();
                }
                
                // 不删除原压缩包
            } else {
                this.zfile = new RandomAccessFile(key.file, "r");
            }
            
            try {
                // 解析zip文件的元数据(读取核心目录区域(zip文件的第二部分))
                initCEN(-1);
                
                byte[] buf = new byte[4];
                readFullyAt(buf, 0, 4, 0);
                
                this.startsWithLoc = (LOCSIG(buf) == LOCSIG);
            } catch(IOException x) {
                try {
                    this.zfile.close();
                } catch(IOException xx) {
                }
                throw x;
            }
        }
        
        
        // 工厂方法，构造zip文件的元数据对象，toDelete指示zip解压后是否将原压缩包删除
        static Source get(File file, boolean toDelete) throws IOException {
            // 获取指定路径标识的文件的基础文件属性
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            
            Key key = new Key(file, attrs);
            
            Source src;
            synchronized(files) {
                // 先尝试从缓存中获取src
                src = files.get(key);
                if(src != null) {
                    src.refs++; // 如果获取到了目标值，则引用计数增一
                    return src;
                }
            }
            
            // 构造新的Source对象
            src = new Source(key, toDelete);
            
            synchronized(files) {
                if(files.containsKey(key)) {    // someone else put in first
                    src.close();                // close the newly created one
                    src = files.get(key);
                    src.refs++;
                    return src;
                }
                
                // 加入缓存
                files.put(key, src);
                
                return src;
            }
        }
        
        /* Reads zip file central directory */
        // 解析zip文件的元数据(读取核心目录区域(zip文件的第二部分))
        private void initCEN(int knownTotal) throws IOException {
            // 实体数量未知
            if(knownTotal == -1) {
                // 获取核心目录相关信息(zip文件第二部分)
                End end = findEND();
                if(end.endpos == 0) {
                    locpos = 0;
                    total = 0;
                    entries = new int[0];
                    cen = null;
                    return;         // only END header present
                }
                
                if(end.cenlen>end.endpos) {
                    zerror("invalid END header (bad central directory size)");
                }
                
                // 核心目录表(zip文件第二部分)在zip文件中的起始位置
                long cenpos = end.endpos - end.cenlen;     // position of CEN table
                
                /*
                 * Get position of first local file (LOC) header,
                 * taking into account that there may be a stub prefixed to the zip file.
                 */
                // 空隙
                locpos = cenpos - end.cenoff;
                if(locpos<0) {
                    zerror("invalid END header (bad central directory offset)");
                }
                
                // read in the CEN and END
                cen = new byte[(int) (end.cenlen + ENDHDR)];
                if(readFullyAt(cen, 0, cen.length, cenpos) != end.cenlen + ENDHDR) {
                    zerror("read CEN tables failed");
                }
                
                total = end.centot;
            } else {
                total = knownTotal;
            }
            
            // hash table for entries
            entries = new int[total * 3];
            tablelen = ((total / 2) | 1); // Odd -> fewer collisions
            table = new int[tablelen];
            
            Arrays.fill(table, ZIP_ENDCHAIN);
            
            int idx = 0;
            int hash = 0;
            int next = -1;
            
            // list for all meta entries
            ArrayList<Integer> metanamesList = null;
            
            // Iterate through the entries in the central directory
            int i = 0;
            int hsh = 0;
            int pos = 0;
            int limit = cen.length - ENDHDR;
            
            // 遍历每个实体对应的核心目录信息
            while(pos + CENHDR<=limit) {
                if(i >= total) {
                    /*
                     * This will only happen if the zip file has an incorrect ENDTOT field,
                     * which usually means it contains more than 65535 entries.
                     */
                    initCEN(countCENHeaders(cen, limit));
                    return;
                }
                
                if(CENSIG(cen, pos) != CENSIG) {
                    zerror("invalid CEN header (bad signature)");
                }
                
                int method = CENHOW(cen, pos);  // 压缩方法
                int nlen = CENNAM(cen, pos);    // 实体名长度
                int elen = CENEXT(cen, pos);    // 扩展区长度
                int clen = CENCOM(cen, pos);    // 实体注释长度
                
                if((CENFLG(cen, pos) & 1) != 0) {
                    zerror("invalid CEN header (encrypted entry)");
                }
                
                if(method != STORED && method != DEFLATED) {
                    zerror("invalid CEN header (bad compression method: " + method + ")");
                }
                
                if(pos + CENHDR + nlen>limit) {
                    zerror("invalid CEN header (bad header size)");
                }
                
                // Record the CEN offset and the name hash in our hash cell.
                hash = hashN(cen, pos + CENHDR, nlen);  // 对该小段核心目录信息计算一个哈希值
                hsh = (hash & 0x7fffffff) % tablelen;
                next = table[hsh];  // 链接到下一个同哈希值的元素
                table[hsh] = idx;
                
                // 记录一个实体信息
                idx = addEntry(idx, hash, next, pos);
                
                // Adds name to metanames.
                if(isMetaName(cen, pos + CENHDR, nlen)) {
                    if(metanamesList == null) {
                        metanamesList = new ArrayList<>(4);
                    }
                    metanamesList.add(pos);
                }
                
                // skip ext and comment
                pos += (CENHDR + nlen + elen + clen);
                
                i++;
            }
            
            total = i;
            
            if(metanamesList != null) {
                metanames = new int[metanamesList.size()];
                for(int j = 0, len = metanames.length; j<len; j++) {
                    metanames[j] = metanamesList.get(j);
                }
            }
            
            if(pos + ENDHDR != cen.length) {
                zerror("invalid CEN header (bad header size)");
            }
        }
        
        /**
         * Searches for end of central directory (END) header.
         * The contents of the END header will be read and placed in endbuf.
         * Returns the file position of the END header, otherwise returns -1 if the END header was not found or an error occurred.
         */
        // 返回核心目录相关信息(zip文件第二部分)
        private End findEND() throws IOException {
            // zip文件大小
            long ziplen = zfile.length();
            if(ziplen<=0) {
                zerror("zip file is empty");
            }
            
            End end = new End();
            
            byte[] buf = new byte[READBLOCKSZ];
            
            long minHDR = (ziplen - END_MAXLEN)>0 ? ziplen - END_MAXLEN : 0;
            
            long minPos = minHDR - (buf.length - ENDHDR);
            
            // 在zip中查找ENDSIG标记(核心目录结束标记(zip文件的第三部分))
            for(long pos = ziplen - buf.length; pos >= minPos; pos -= (buf.length - ENDHDR)) {
                int off = 0;
                if(pos<0) {
                    // Pretend there are some NUL bytes before start of file
                    off = (int) -pos;
                    Arrays.fill(buf, 0, off, (byte) 0);
                }
                
                int len = buf.length - off;
                if(readFullyAt(buf, off, len, pos + off) != len) {
                    zerror("zip END header not found");
                }
                
                /* Now scan the block backwards for END header signature */
                // 从buf的后面22个字节开始，向前查找ENDSIG标记
                for(int i = buf.length - ENDHDR; i >= 0; i--) {
                    // 索引i处指向了ENDSIG(核心目录结束标记(zip文件的第三部分))，如果压缩包没有注释，此处刚刚满足条件
                    if(buf[i + 0] != (byte) 'P' || buf[i + 1] != (byte) 'K' || buf[i + 2] != (byte) '\005' || buf[i + 3] != (byte) '\006') {
                        continue;   // 查找第三部分的起始
                    }
                    
                    // Found ENDSIG header
                    byte[] endbuf = Arrays.copyOfRange(buf, i, i + ENDHDR); // 记录zip文件第三部分前面非注释的内容
                    end.centot = ENDTOT(endbuf);    // 核心目录中记载的实体数量(理论上，每个实体都会对应一个目录)
                    end.cenlen = ENDSIZ(endbuf);    // 核心目录区域所占字节数(zip文件第二部分)
                    end.cenoff = ENDOFF(endbuf);    // 核心目录区域的起始位置相对于文件起始的偏移量
                    end.endpos = pos + i;           // 核心目录区域的结束位置(同时也是zip文件的第三部分的起始)
                    
                    // 注释长度
                    int comlen = ENDCOM(endbuf);
                    
                    if(end.endpos + ENDHDR + comlen != ziplen) {
                        /*
                         * ENDSIG matched, however the size of file comment in it does not match the real size.
                         * One "common" cause for this problem is some "extra" bytes are padded at the end of the zipfile.
                         * Let's do some extra verification, we don't care about the performance in this situation.
                         */
                        byte[] sbuf = new byte[4];
                        long cenpos = end.endpos - end.cenlen;
                        long locpos = cenpos - end.cenoff;
                        if(cenpos<0 || locpos<0 || readFullyAt(sbuf, 0, sbuf.length, cenpos) != 4 || GETSIG(sbuf) != CENSIG || readFullyAt(sbuf, 0, sbuf.length, locpos) != 4 || GETSIG(sbuf) != LOCSIG) {
                            continue;
                        }
                    }
                    
                    // this zip file has comlen
                    if(comlen>0) {
                        // 记录zip文件的注释信息
                        comment = new byte[comlen];
                        if(readFullyAt(comment, 0, comlen, end.endpos + ENDHDR) != comlen) {
                            zerror("zip comment read failed");
                        }
                    }
                    
                    // must check for a zip64 end record; it is always permitted to be present
                    try {
                        byte[] loc64 = new byte[ZIP64_LOCHDR];
                        if(end.endpos<ZIP64_LOCHDR || readFullyAt(loc64, 0, loc64.length, end.endpos - ZIP64_LOCHDR) != loc64.length || GETSIG(loc64) != ZIP64_LOCSIG) {
                            return end;
                        }
                        
                        long end64pos = ZIP64_LOCOFF(loc64);
                        byte[] end64buf = new byte[ZIP64_ENDHDR];
                        if(readFullyAt(end64buf, 0, end64buf.length, end64pos) != end64buf.length || GETSIG(end64buf) != ZIP64_ENDSIG) {
                            return end;
                        }
                        
                        // end64 candidate found,
                        long cenlen64 = ZIP64_ENDSIZ(end64buf);
                        long cenoff64 = ZIP64_ENDOFF(end64buf);
                        long centot64 = ZIP64_ENDTOT(end64buf);
                        // double-check
                        if(cenlen64 != end.cenlen && end.cenlen != ZIP64_MAGICVAL || cenoff64 != end.cenoff && end.cenoff != ZIP64_MAGICVAL || centot64 != end.centot && end.centot != ZIP64_MAGICCOUNT) {
                            return end;
                        }
                        
                        // to use the end64 values
                        end.cenlen = cenlen64;
                        end.cenoff = cenoff64;
                        end.centot = (int) centot64; // assume total < 2g
                        end.endpos = end64pos;
                    } catch(IOException x) {
                        // no zip64 loc/end
                    }
                    
                    return end;
                }
            }
            
            zerror("zip END header not found");
            
            return null; //make compiler happy
        }
        
        /**
         * Returns true if the bytes represent a non-directory name
         * beginning with "META-INF/", disregarding ASCII case.
         */
        private static boolean isMetaName(byte[] name, int off, int len) {
            // Use the "oldest ASCII trick in the book"
            return len>9                       // "META-INF/".length()
                && name[off + len - 1] != '/'  // non-directory
                && (name[off++] | 0x20) == 'm'
                && (name[off++] | 0x20) == 'e'
                && (name[off++] | 0x20) == 't'
                && (name[off++] | 0x20) == 'a'
                && (name[off++]) == '-'
                && (name[off++] | 0x20) == 'i'
                && (name[off++] | 0x20) == 'n'
                && (name[off++] | 0x20) == 'f'
                && (name[off]) == '/';
        }
        
        // 记录每个实体元数据在核心目录表中的位置信息
        private int addEntry(int index, int hash, int next, int pos) {
            entries[index++] = hash;    // 当前实体信息的哈希值
            entries[index++] = next;    // 链接到下一个同哈希值的元素
            entries[index++] = pos;     // 当前实体信息在核心目录表中的偏移量
            return index;
        }
        
        private int getEntryHash(int index) {
            return entries[index];
        }
        
        private int getEntryNext(int index) {
            return entries[index + 1];
        }
        
        private int getEntryPos(int index) {
            return entries[index + 2];
        }
        
        private final int readFullyAt(byte[] buf, int off, int len, long pos) throws IOException {
            synchronized(zfile) {
                zfile.seek(pos);
                int N = len;
                while(N>0) {
                    int n = Math.min(BUF_SIZE, N);
                    zfile.readFully(buf, off, n);
                    off += n;
                    N -= n;
                }
                return len;
            }
        }
        
        private final int readAt(byte[] buf, int off, int len, long pos) throws IOException {
            synchronized(zfile) {
                zfile.seek(pos);
                return zfile.read(buf, off, len);
            }
        }
        
        private static final int hashN(byte[] a, int off, int len) {
            int h = 1;
            while(len-->0) {
                h = 31 * h + a[off++];
            }
            return h;
        }
        
        private static final int hash_append(int hash, byte b) {
            return hash * 31 + b;
        }
        
        /**
         * Returns the number of CEN headers in a central directory.
         * Will not throw, even if the zip file is corrupt.
         *
         * @param cen  copy of the bytes in a zip file's central directory
         * @param size number of bytes in central directory
         */
        private static int countCENHeaders(byte[] cen, int size) {
            int count = 0;
            for(int p = 0; p + CENHDR<=size; p += CENHDR + CENNAM(cen, p) + CENEXT(cen, p) + CENCOM(cen, p)) {
                count++;
            }
            return count;
        }
        
        /**
         * Returns the {@code pos} of the zip cen entry corresponding to the specified entry name, or -1 if not found.
         */
        // 返回拥有指定名称的实体信息在核心目录表中的偏移量
        private int getEntryPos(byte[] name, boolean addSlash) {
            if(total == 0) {
                return -1;
            }
            
            int hsh = hashN(name, 0, name.length);
            int idx = table[(hsh & 0x7fffffff) % tablelen];
            
            /*
             * This while loop is an optimization where a double lookup for name and name+/ is being performed.
             * The name char array has enough room at the end to try again with a slash appended if the first table lookup does not succeed.
             */
            while(true) {
                /*
                 * Search down the target hash chain for a entry whose
                 * 32 bit hash matches the hashed name.
                 */
                while(idx != ZIP_ENDCHAIN) {
                    if(getEntryHash(idx) == hsh) {
                        // The CEN name must match the specfied one
                        int pos = getEntryPos(idx);
                        if(name.length == CENNAM(cen, pos)) {
                            boolean matched = true;
                            int nameoff = pos + CENHDR;
                            for(byte b : name) {
                                if(b != cen[nameoff++]) {
                                    matched = false;
                                    break;
                                }
                            }
                            if(matched) {
                                return pos;
                            }
                        }
                    }
                    idx = getEntryNext(idx);
                }
                
                /* If not addSlash, or slash is already there, we are done */
                if(!addSlash || name.length == 0 || name[name.length - 1] == '/') {
                    return -1;
                }
                
                /* Add slash and try once more */
                name = Arrays.copyOf(name, name.length + 1);
                name[name.length - 1] = '/';
                hsh = hash_append(hsh, (byte) '/');
                //idx = table[hsh % tablelen];
                idx = table[(hsh & 0x7fffffff) % tablelen];
                addSlash = false;
            }
        }
        
        private static void zerror(String msg) throws ZipException {
            throw new ZipException(msg);
        }
        
        static void release(Source src) throws IOException {
            synchronized(files) {
                if(src != null && --src.refs == 0) {
                    files.remove(src.key);
                    src.close();
                }
            }
        }
        
        private void close() throws IOException {
            zfile.close();
            zfile = null;
            cen = null;
            entries = null;
            table = null;
            metanames = null;
        }
        
        
        // zip文件及其基础文件属性的映射
        private static class Key {
            File file;                  // zip文件
            BasicFileAttributes attrs;  // file对应的基础文件属性
            
            public Key(File file, BasicFileAttributes attrs) {
                this.attrs = attrs;
                this.file = file;
            }
            
            public int hashCode() {
                long t = attrs.lastModifiedTime().toMillis();
                return ((int) (t ^ (t >>> 32))) + file.hashCode();
            }
            
            public boolean equals(Object obj) {
                if(obj instanceof Key) {
                    Key key = (Key) obj;
                    if(!attrs.lastModifiedTime().equals(key.attrs.lastModifiedTime())) {
                        return false;
                    }
                    Object fk = attrs.fileKey();
                    if(fk != null) {
                        return fk.equals(key.attrs.fileKey());
                    } else {
                        return file.equals(key.file);
                    }
                }
                return false;
            }
        }
        
        // 核心目录相关信息(zip文件第二部分)
        private static class End {
            int centot;      // 4 bytes | 核心目录中记载的实体数量(理论上，每个实体都会对应一个目录)
            long cenlen;     // 4 bytes | 核心目录区域所占字节数(zip文件第二部分)
            long cenoff;     // 4 bytes | 核心目录区域的起始位置相对于文件起始的偏移量
            long endpos;     // 4 bytes | 核心目录区域的结束位置(同时也是zip文件的第三部分的起始)
        }
    }
    
    /**
     * Inner class implementing the input stream used to read a (possibly compressed) zip file entry.
     */
    // 单个ZipEntry的输入流
    private class ZipFileInputStream extends InputStream {
        // 实体压缩后的剩余未读的字节数
        protected long rem;     // number of remaining bytes within entry
        // 实体压缩前的大小
        protected long size;    // uncompressed size of this entry
        // 当前实体对应的本地文件头(zip文件第一部分的第1小节)的起始位置在zip文件中的相对位移
        private long pos;       // current position within entry data
        private volatile boolean closeRequested;
        
        /**
         * 构造针对某个ZipEntry的输入流
         *
         * cen    - 记录zip文件第二部分的全部内容和第三部分的起始标记
         * cenpos - 某个实体信息在cen中的偏移量
         */
        ZipFileInputStream(byte[] cen, int cenpos) {
            rem = CENSIZ(cen, cenpos);
            size = CENLEN(cen, cenpos);
            pos = CENOFF(cen, cenpos);
            
            // zip64
            if(rem == ZIP64_MAGICVAL || size == ZIP64_MAGICVAL || pos == ZIP64_MAGICVAL) {
                checkZIP64(cen, cenpos);
            }
            
            // negative for lazy initialization, see getDataOffset();
            pos = -(pos + ZipFile.this.res.zsrc.locpos);
        }
        
        // 返回zip实体中的一个原始字节
        public int read() throws IOException {
            byte[] b = new byte[1];
            
            if(read(b, 0, 1) == 1) {
                return b[0] & 0xff;
            } else {
                return -1;
            }
        }
        
        // 读取zip实体中的len个字节，并将其存入字节数组b的off处
        public int read(byte[] b, int off, int len) throws IOException {
            synchronized(ZipFile.this) {
                ensureOpenOrZipException();
                
                initDataOffset();
                
                if(rem == 0) {
                    return -1;
                }
                
                if(len>rem) {
                    len = (int) rem;
                }
                
                if(len<=0) {
                    return 0;
                }
                
                // 需要从zip文件的pos处读取数据，因为当前实体就位于pos处
                len = ZipFile.this.res.zsrc.readAt(b, off, len, pos);
                if(len>0) {
                    pos += len;
                    rem -= len;
                }
            }
            
            if(rem == 0) {
                close();
            }
            
            return len;
        }
        
        // 跳过该实体中的n个字节
        public long skip(long n) throws IOException {
            synchronized(ZipFile.this) {
                initDataOffset();
                
                if(n>rem) {
                    n = rem;
                }
                pos += n;
                rem -= n;
            }
            
            if(rem == 0) {
                close();
            }
            
            return n;
        }
        
        // 返回实体中剩余未读的字节数
        public int available() {
            return rem>Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rem;
        }
        
        // 返回实体压缩前的大小
        public long size() {
            return size;
        }
        
        public void close() {
            if(closeRequested) {
                return;
            }
            closeRequested = true;
            rem = 0;
            synchronized(res.istreams) {
                res.istreams.remove(this);
            }
        }
        
        private void checkZIP64(byte[] cen, int cenpos) {
            int off = cenpos + CENHDR + CENNAM(cen, cenpos);
            int end = off + CENEXT(cen, cenpos);
            while(off + 4<end) {
                int tag = get16(cen, off);
                int sz = get16(cen, off + 2);
                off += 4;
                
                if(off + sz>end) {
                    break;  // invalid data
                }
                
                if(tag == EXTID_ZIP64) {
                    if(size == ZIP64_MAGICVAL) {
                        if(sz<8 || (off + 8)>end) {
                            break;
                        }
                        size = get64(cen, off);
                        sz -= 8;
                        off += 8;
                    }
                    
                    if(rem == ZIP64_MAGICVAL) {
                        if(sz<8 || (off + 8)>end) {
                            break;
                        }
                        rem = get64(cen, off);
                        sz -= 8;
                        off += 8;
                    }
                    
                    if(pos == ZIP64_MAGICVAL) {
                        if(sz<8 || (off + 8)>end) {
                            break;
                        }
                        pos = get64(cen, off);
                        sz -= 8;
                        off += 8;
                    }
                    
                    break;
                }
                
                off += sz;
            }
        }
        
        /*
         * The Zip file spec explicitly allows the LOC extra data size to be different from the CEN extra data size.
         * Since we cannot trust the CEN extra data size, we need to read the LOC to determine the entry data offset.
         */
        private long initDataOffset() throws IOException {
            if(pos<=0) {
                byte[] loc = new byte[LOCHDR];
                pos = -pos;
                int len = ZipFile.this.res.zsrc.readFullyAt(loc, 0, loc.length, pos);
                
                if(len != LOCHDR) {
                    throw new ZipException("ZipFile error reading zip file");
                }
                
                if(LOCSIG(loc) != LOCSIG) {
                    throw new ZipException("ZipFile invalid LOC header (bad signature)");
                }
                
                pos += LOCHDR + LOCNAM(loc) + LOCEXT(loc);
            }
            
            return pos;
        }
    }
    
    /*
     * (解压)输入流：从指定的源头输入流读取已压缩的数据，将其解压后填充到给定的内存
     * 适用于读取被压缩过的zip实体的输入流，然后将其解压
     */
    private class ZipFileInflaterInputStream extends InflaterInputStream {
        private final Cleanable cleanable;  // 针对对解压器的清理器
        private volatile boolean closeRequested;
        private boolean eof = false;
        
        ZipFileInflaterInputStream(ZipFileInputStream zfin, CleanableResource res, int size) {
            this(zfin, res, res.getInflater(), size);
        }
        
        private ZipFileInflaterInputStream(ZipFileInputStream zfin, CleanableResource res, Inflater inf, int size) {
            super(zfin, inf, size);
            this.cleanable = CleanerFactory.cleaner().register(this, new InflaterCleanupAction(inf, res));
        }
        
        // 返回剩余可读(可解压)的字节数
        public int available() throws IOException {
            if(closeRequested) {
                return 0;
            }
            
            long avail = ((ZipFileInputStream) in).size() - inf.getBytesWritten();
            
            return (avail>(long) Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) avail);
        }
        
        // 关闭当前的解压输入流，会清理资源
        public void close() throws IOException {
            if(closeRequested) {
                return;
            }
            
            closeRequested = true;
            
            super.close();
            
            synchronized(res.istreams) {
                res.istreams.remove(this);
            }
            
            cleanable.clean();
        }
        
        /**
         * Override fill() method to provide an extra "dummy" byte at the end of the input stream.
         * This is required when using the "nowrap" Inflater option.
         */
        // 向解压器填充待解压的数据
        protected void fill() throws IOException {
            if(eof) {
                throw new EOFException("Unexpected end of ZLIB input stream");
            }
            
            len = in.read(buf, 0, buf.length);
            if(len == -1) {
                buf[0] = 0;
                len = 1;
                eof = true;
            }
            
            inf.setInput(buf, 0, len);
        }
    }
    
    // 实体迭代器
    private class ZipEntryIterator<T extends ZipEntry> implements Enumeration<T>, Iterator<T> {
        
        private final int entryCount;   // 实体总量
        private int i = 0;              // 已遍历的实体数量
        private final Function<String, T> gen;
        
        public ZipEntryIterator(int entryCount, Function<String, T> gen) {
            this.entryCount = entryCount;
            this.gen = gen;
        }
        
        // 是否包含更多未读实体
        @Override
        public boolean hasMoreElements() {
            return hasNext();
        }
        
        // 返回下一个实体条目
        @Override
        public T nextElement() {
            return next();
        }
        
        // 是否包含更多未读实体
        @Override
        public boolean hasNext() {
            return i<entryCount;
        }
        
        // 返回下一个实体条目
        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            synchronized(ZipFile.this) {
                ensureOpen();
                
                if(!hasNext()) {
                    throw new NoSuchElementException();
                }
                
                // each "entry" has 3 ints in table entries
                return (T) getZipEntry(null, null, res.zsrc.getEntryPos(i++ * 3), gen);
            }
        }
        
        // 实体迭代器
        @Override
        public Iterator<T> asIterator() {
            return this;
        }
    }
    
    // 实体的可分割迭代器
    private class EntrySpliterator<T> extends Spliterators.AbstractSpliterator<T> {
        private final int fence;
        private final IntFunction<T> gen;
        private int index;
        
        EntrySpliterator(int index, int fence, IntFunction<T> gen) {
            super(fence, Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL);
            this.index = index;
            this.fence = fence;
            this.gen = gen;
        }
        
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            if(index >= 0 && index<fence) {
                synchronized(ZipFile.this) {
                    ensureOpen();
                    int pos = res.zsrc.getEntryPos(index++ * 3);
                    T apply = gen.apply(pos);
                    action.accept(apply);
                }
                return true;
            }
            
            return false;
        }
    }
    
    // 资源清理器
    private static class CleanableResource implements Runnable {
        final Cleanable cleanable;  // 清理器
        
        // The outstanding inputstreams that need to be closed
        final Set<InputStream> istreams;    // 记录访问过的实体的输入流
        
        // List of cached Inflater objects for decompression
        Deque<Inflater> inflaterCache;      // 解压器缓存
        
        Source zsrc;    // zip文件的元数据
        
        CleanableResource(File file, int mode) throws IOException {
            // 未设置清理器
            this.cleanable = null;
            this.istreams = Collections.newSetFromMap(new WeakHashMap<>());
            this.inflaterCache = new ArrayDeque<>();
            
            // 工厂方法，构造zip文件的元数据对象
            this.zsrc = Source.get(file, (mode & OPEN_DELETE) != 0);
        }
        
        CleanableResource(ZipFile zf, File file, int mode) throws IOException {
            // 向清理器注册跟踪的对象zf和清理动作run
            this.cleanable = CleanerFactory.cleaner().register(zf, this);
            this.istreams = Collections.newSetFromMap(new WeakHashMap<>());
            this.inflaterCache = new ArrayDeque<>();
            
            // 工厂方法，构造zip文件的元数据对象
            this.zsrc = Source.get(file, (mode & OPEN_DELETE) != 0);
        }
        
        // 由清理器Cleanable最终调用，清理本地内存中的资源
        void clean() {
            cleanable.clean();
        }
        
        // 由clean()回调
        public void run() {
            IOException ioe = null;
            
            // Release cached inflaters and close the cache first
            Deque<Inflater> inflaters = this.inflaterCache;
            if(inflaters != null) {
                synchronized(inflaters) {
                    // no need to double-check as only one thread gets a
                    // chance to execute run() (Cleaner guarantee)...
                    Inflater inf;
                    while((inf = inflaters.poll()) != null) {
                        inf.end();
                    }
                    // close inflaters cache
                    this.inflaterCache = null;
                }
            }
            
            // Close streams, release their inflaters
            if(istreams != null) {
                synchronized(istreams) {
                    if(!istreams.isEmpty()) {
                        InputStream[] copy = istreams.toArray(new InputStream[0]);
                        istreams.clear();
                        for(InputStream is : copy) {
                            try {
                                is.close();
                            } catch(IOException e) {
                                if(ioe == null)
                                    ioe = e;
                                else
                                    ioe.addSuppressed(e);
                            }
                        }
                    }
                }
            }
            
            // Release zip src
            if(zsrc != null) {
                synchronized(zsrc) {
                    try {
                        Source.release(zsrc);
                        zsrc = null;
                    } catch(IOException e) {
                        if(ioe == null)
                            ioe = e;
                        else
                            ioe.addSuppressed(e);
                    }
                }
            }
            if(ioe != null) {
                throw new UncheckedIOException(ioe);
            }
        }
        
        /**
         * If {@code ZipFile} has been subclassed and the {@code close} method is overridden,
         * uses the {@code finalizer} mechanism for resource cleanup.
         * So {@code close} method can be called when the the {@code ZipFile} is unreachable.
         * This mechanism will be removed when {@code finalize} method is removed from {@code ZipFile}.
         */
        /*
         * 如果ZipFile被子类化，且close方法被覆盖，则使用finalizer机制进行资源清理。
         * 否则，使用清理器Cleanable这种资源清理机制
         */
        static CleanableResource get(ZipFile zf, File file, int mode) throws IOException {
            Class<?> clz = zf.getClass();
            
            // 如果clz是ZipFile子类
            while(clz != ZipFile.class && clz != JarFile.class) {
                // 获取当前类中名为"close"的无参方法，但不包括父类/父接口中的方法
                if(JLA.getDeclaredPublicMethods(clz, "close").size() != 0) {
                    return new FinalizableResource(zf, file, mode);
                }
                
                // 如果该子类中不包含close方法，则向父类查找
                clz = clz.getSuperclass();
            }
            
            return new CleanableResource(zf, file, mode);
        }
        
        /**
         * Gets an inflater from the list of available inflaters or allocates a new one.
         */
        // 返回一个解压器，兼容GZIP
        Inflater getInflater() {
            Inflater inf;
            
            synchronized(inflaterCache) {
                if((inf = inflaterCache.poll()) != null) {
                    return inf;
                }
            }
            
            return new Inflater(true);
        }
        
        /**
         * Releases the specified inflater to the list of available inflaters.
         */
        // 释放(回收)解压器
        void releaseInflater(Inflater inf) {
            Deque<Inflater> inflaters = this.inflaterCache;
            
            if(inflaters != null) {
                synchronized(inflaters) {
                    // double checked!
                    if(inflaters == this.inflaterCache) {
                        inf.reset();
                        inflaters.add(inf);
                        return;
                    }
                }
            }
            
            // inflaters cache already closed - just end it.
            inf.end();
        }
        
        static class FinalizableResource extends CleanableResource {
            ZipFile zf;
            
            FinalizableResource(ZipFile zf, File file, int mode) throws IOException {
                super(file, mode);
                this.zf = zf;
            }
            
            @Override
            @SuppressWarnings("deprecation")
            protected void finalize() throws IOException {
                zf.close(); // 调用到clean()方法
            }
            
            @Override
            void clean() {
                run();      // 执行run动作
            }
        }
    }
    
    // 对解压器的清理动作
    private static class InflaterCleanupAction implements Runnable {
        private final Inflater inf;
        private final CleanableResource res;
        
        InflaterCleanupAction(Inflater inf, CleanableResource res) {
            this.inf = inf;
            this.res = res;
        }
        
        @Override
        public void run() {
            res.releaseInflater(inf);
        }
    }
    
}
