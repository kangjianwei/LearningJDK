/*
 * Copyright (c) 1994, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.nio.channels.FileChannel;
import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.internal.misc.SharedSecrets;
import sun.nio.ch.FileChannelImpl;

/**
 * A file output stream is an output stream for writing data to a
 * <code>File</code> or to a <code>FileDescriptor</code>. Whether or not
 * a file is available or may be created depends upon the underlying
 * platform.  Some platforms, in particular, allow a file to be opened
 * for writing by only one {@code FileOutputStream} (or other
 * file-writing object) at a time.  In such situations the constructors in
 * this class will fail if the file involved is already open.
 *
 * <p><code>FileOutputStream</code> is meant for writing streams of raw bytes
 * such as image data. For writing streams of characters, consider using
 * <code>FileWriter</code>.
 *
 * @author Arthur van Hoff
 * @apiNote To release resources used by this stream {@link #close} should be called
 * directly or by try-with-resources. Subclasses are responsible for the cleanup
 * of resources acquired by the subclass.
 * Subclasses that override {@link #finalize} in order to perform cleanup
 * should be modified to use alternative cleanup mechanisms such as
 * {@link java.lang.ref.Cleaner} and remove the overriding {@code finalize} method.
 * @implSpec If this FileOutputStream has been subclassed and the {@link #close}
 * method has been overridden, the {@link #close} method will be
 * called when the FileInputStream is unreachable.
 * Otherwise, it is implementation specific how the resource cleanup described in
 * {@link #close} is performed.
 * @see java.io.File
 * @see java.io.FileDescriptor
 * @see java.io.FileInputStream
 * @see java.nio.file.Files#newOutputStream
 * @since 1.0
 */
// 文件输出流：向文件写入数据
public class FileOutputStream extends OutputStream {
    
    /**
     * Access to FileDescriptor internals.
     */
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();
    
    private final Object closeLock = new Object();
    
    /**
     * The system dependent file descriptor.
     */
    // 该输出流关联的文件描述符
    private final FileDescriptor fd;
    
    /**
     * The path of the referenced file (null if the stream is created with a file descriptor)
     */
    // 输出流的path（获取数据的源头），使用File的path来初始化
    private final String path;
    
    // 应用于当前输出流的终结器
    private final Object altFinalizer;
    
    /**
     * The associated channel, initialized lazily.
     */
    // 当前输出流关联的通道，调用getChannel()后才会对其初始化
    private volatile FileChannel channel;
    
    // 输出流是否已关闭
    private volatile boolean closed;
    
    
    static {
        initIDs();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a file output stream to write to the file with the
     * specified name. A new <code>FileDescriptor</code> object is
     * created to represent this file connection.
     * <p>
     * First, if there is a security manager, its <code>checkWrite</code>
     * method is called with <code>name</code> as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a <code>FileNotFoundException</code> is thrown.
     *
     * @param name the system-dependent filename
     *
     * @throws FileNotFoundException if the file exists but is a directory
     *                               rather than a regular file, does not exist but cannot
     *                               be created, or cannot be opened for any other reason
     * @throws SecurityException     if a security manager exists and its
     *                               <code>checkWrite</code> method denies write access
     *                               to the file.
     * @implSpec Invoking this constructor with the parameter {@code name} is
     * equivalent to invoking {@link #FileOutputStream(String, boolean)
     * new FileOutputStream(name, false)}.
     * @see java.lang.SecurityManager#checkWrite(java.lang.String)
     */
    // 打开指定名称的文件以便输出，数据从文件头部写入(覆盖模式)
    public FileOutputStream(String name) throws FileNotFoundException {
        this(name != null ? new File(name) : null, false);
    }
    
    /**
     * Creates a file output stream to write to the file with the specified
     * name.  If the second argument is <code>true</code>, then
     * bytes will be written to the end of the file rather than the beginning.
     * A new <code>FileDescriptor</code> object is created to represent this
     * file connection.
     * <p>
     * First, if there is a security manager, its <code>checkWrite</code>
     * method is called with <code>name</code> as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a <code>FileNotFoundException</code> is thrown.
     *
     * @param name   the system-dependent file name
     * @param append if <code>true</code>, then bytes will be written
     *               to the end of the file rather than the beginning
     *
     * @throws FileNotFoundException if the file exists but is a directory
     *                               rather than a regular file, does not exist but cannot
     *                               be created, or cannot be opened for any other reason.
     * @throws SecurityException     if a security manager exists and its
     *                               <code>checkWrite</code> method denies write access
     *                               to the file.
     * @see java.lang.SecurityManager#checkWrite(java.lang.String)
     * @since 1.1
     */
    // 打开指定名称的文件以便输出。如果append为true，则数据被写入文件尾部，否则，数据从文件头部写入
    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        this(name != null ? new File(name) : null, append);
    }
    
    /**
     * Creates a file output stream to write to the file represented by
     * the specified <code>File</code> object. A new
     * <code>FileDescriptor</code> object is created to represent this
     * file connection.
     * <p>
     * First, if there is a security manager, its <code>checkWrite</code>
     * method is called with the path represented by the <code>file</code>
     * argument as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a <code>FileNotFoundException</code> is thrown.
     *
     * @param file the file to be opened for writing.
     *
     * @throws FileNotFoundException if the file exists but is a directory
     *                               rather than a regular file, does not exist but cannot
     *                               be created, or cannot be opened for any other reason
     * @throws SecurityException     if a security manager exists and its
     *                               <code>checkWrite</code> method denies write access
     *                               to the file.
     * @see java.io.File#getPath()
     * @see java.lang.SecurityException
     * @see java.lang.SecurityManager#checkWrite(java.lang.String)
     */
    // 打开指定的文件以便输出，数据从文件头部写入(覆盖模式)
    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }
    
    /**
     * Creates a file output stream to write to the file represented by
     * the specified <code>File</code> object. If the second argument is
     * <code>true</code>, then bytes will be written to the end of the file
     * rather than the beginning. A new <code>FileDescriptor</code> object is
     * created to represent this file connection.
     * <p>
     * First, if there is a security manager, its <code>checkWrite</code>
     * method is called with the path represented by the <code>file</code>
     * argument as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a <code>FileNotFoundException</code> is thrown.
     *
     * @param file   the file to be opened for writing.
     * @param append if <code>true</code>, then bytes will be written
     *               to the end of the file rather than the beginning
     *
     * @throws FileNotFoundException if the file exists but is a directory
     *                               rather than a regular file, does not exist but cannot
     *                               be created, or cannot be opened for any other reason
     * @throws SecurityException     if a security manager exists and its
     *                               <code>checkWrite</code> method denies write access
     *                               to the file.
     * @see java.io.File#getPath()
     * @see java.lang.SecurityException
     * @see java.lang.SecurityManager#checkWrite(java.lang.String)
     * @since 1.4
     */
    // 打开指定的文件以便输出。如果append为true，则数据被写入文件尾部，否则，数据从文件头部写入
    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        String name = (file != null ? file.getPath() : null);
        
        // 检查访问权限
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkWrite(name);
        }
        
        if(name == null) {
            throw new NullPointerException();
        }
        
        if(file.isInvalid()) {
            throw new FileNotFoundException("Invalid file path");
        }
        
        // 为当前流锁定文件描述符
        this.fd = new FileDescriptor();
        
        // 与此同时，将当前文件(流)记录到关联的文件描述符fd中
        fd.attach(this);
        
        // 用File的path初始化输出流的path（获取数据的源头）
        this.path = name;
        
        // 更新文件底层的append标记，指示写入新内容时需要覆盖(append=false)或追加(append=true)
        open(name, append);
        
        // 初始化应用于当前输出流的终结器
        altFinalizer = getFinalizer(this);
        // 如果该输出流没有终结器，需要为其注册清理器
        if(altFinalizer == null) {
            // 注册文件描述符fd到清理器
            FileCleanable.register(fd);   // open sets the fd, register the cleanup
        }
    }
    
    /**
     * Creates a file output stream to write to the specified file
     * descriptor, which represents an existing connection to an actual
     * file in the file system.
     * <p>
     * First, if there is a security manager, its <code>checkWrite</code>
     * method is called with the file descriptor <code>fdObj</code>
     * argument as its argument.
     * <p>
     * If <code>fdObj</code> is null then a <code>NullPointerException</code>
     * is thrown.
     * <p>
     * This constructor does not throw an exception if <code>fdObj</code>
     * is {@link java.io.FileDescriptor#valid() invalid}.
     * However, if the methods are invoked on the resulting stream to attempt
     * I/O on the stream, an <code>IOException</code> is thrown.
     *
     * @param fdObj the file descriptor to be opened for writing
     *
     * @throws SecurityException if a security manager exists and its
     *                           <code>checkWrite</code> method denies
     *                           write access to the file descriptor
     * @see java.lang.SecurityManager#checkWrite(java.io.FileDescriptor)
     */
    // 直接使用文件描述符初始化输出流
    public FileOutputStream(FileDescriptor fdObj) {
        
        if(fdObj == null) {
            throw new NullPointerException();
        }
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkWrite(fdObj);
        }
        
        this.fd = fdObj;
        this.path = null;
        this.altFinalizer = null;
        
        // 将当前文件(流)记录到关联的文件描述符fd中
        fd.attach(this);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes the specified byte to this file output stream. Implements
     * the <code>write</code> method of <code>OutputStream</code>.
     *
     * @param b the byte to be written.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 将指定的字节写入到输出流
    public void write(int b) throws IOException {
        boolean append = fdAccess.getAppend(fd);
        write(b, append);
    }
    
    /**
     * Writes <code>b.length</code> bytes from the specified byte array
     * to this file output stream.
     *
     * @param b the data.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 将字节数组b的内容写入到输出流
    public void write(byte[] b) throws IOException {
        boolean append = fdAccess.getAppend(fd);
        writeBytes(b, 0, b.length, append);
    }
    
    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this file output stream.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     *
     * @throws IOException if an I/O error occurs.
     */
    // 将字节数组b中off处起的len个字节写入到输出流
    public void write(byte[] b, int off, int len) throws IOException {
        boolean append = fdAccess.getAppend(fd);
        writeBytes(b, off, len, append);
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the unique {@link java.nio.channels.FileChannel FileChannel}
     * object associated with this file output stream.
     *
     * <p> The initial {@link java.nio.channels.FileChannel#position()
     * position} of the returned channel will be equal to the
     * number of bytes written to the file so far unless this stream is in
     * append mode, in which case it will be equal to the size of the file.
     * Writing bytes to this stream will increment the channel's position
     * accordingly.  Changing the channel's position, either explicitly or by
     * writing, will change this stream's file position.
     *
     * @return the file channel associated with this file output stream
     *
     * @spec JSR-51
     * @since 1.4
     */
    // 返回当前文件输出流关联的只写通道
    public FileChannel getChannel() {
        FileChannel fc = this.channel;
        
        if(fc == null) {
            synchronized(this) {
                fc = this.channel;
                // 线程安全，双重检查机制
                if(fc == null) {
                    // 为输出流关联一个通道（懒加载）
                    this.channel = fc = FileChannelImpl.open(fd, path, false, true, false, this);
                    
                    if(closed) {
                        try {
                            // possible race with close(), benign since FileChannel.close is final and idempotent
                            fc.close();
                        } catch(IOException ioe) {
                            throw new InternalError(ioe); // should not happen
                        }
                    }
                }
            }
        }
        
        return fc;
    }
    
    /**
     * Returns the file descriptor associated with this stream.
     *
     * @return the <code>FileDescriptor</code> object that represents
     * the connection to the file in the file system being used
     * by this <code>FileOutputStream</code> object.
     *
     * @throws IOException if an I/O error occurs.
     * @see java.io.FileDescriptor
     */
    // 返回当前输出流锁定的文件描述符
    public final FileDescriptor getFD() throws IOException {
        if(fd != null) {
            return fd;
        }
        
        throw new IOException();
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes this file output stream and releases any system resources
     * associated with this stream. This file output stream may no longer
     * be used for writing bytes.
     *
     * <p> If this stream has an associated channel then the channel is closed
     * as well.
     *
     * @throws IOException if an I/O error occurs.
     * @apiNote Overriding {@link #close} to perform cleanup actions is reliable
     * only when called directly or when called by try-with-resources.
     * Do not depend on finalization to invoke {@code close};
     * finalization is not reliable and is deprecated.
     * If cleanup of native resources is needed, other mechanisms such as
     * {@linkplain java.lang.ref.Cleaner} should be used.
     * @revised 1.4
     * @spec JSR-51
     */
    // 关闭输出流
    public void close() throws IOException {
        if(closed) {
            return;
        }
        
        synchronized(closeLock) {
            if(closed) {
                return;
            }
            closed = true;
        }
        
        FileChannel fc = channel;
        if(fc != null) {
            // possible race with getChannel(), benign since
            // FileChannel.close is final and idempotent
            fc.close();
        }
        
        fd.closeAll(new Closeable() {
            public void close() throws IOException {
                fd.close();
            }
        });
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Cleans up the connection to the file, and ensures that the
     * {@link #close} method of this file output stream is
     * called when there are no more references to this stream.
     * The {@link #finalize} method does not call {@link #close} directly.
     *
     * @throws IOException if an I/O error occurs.
     * @apiNote To release resources used by this stream {@link #close} should be called
     * directly or by try-with-resources.
     * @implSpec If this FileOutputStream has been subclassed and the {@link #close}
     * method has been overridden, the {@link #close} method will be
     * called when the FileOutputStream is unreachable.
     * Otherwise, it is implementation specific how the resource cleanup described in
     * {@link #close} is performed.
     * @see java.io.FileInputStream#close()
     * @deprecated The {@code finalize} method has been deprecated and will be removed.
     * Subclasses that override {@code finalize} in order to perform cleanup
     * should be modified to use alternative cleanup mechanisms and
     * to remove the overriding {@code finalize} method.
     * When overriding the {@code finalize} method, its implementation must explicitly
     * ensure that {@code super.finalize()} is invoked as described in {@link Object#finalize}.
     * See the specification for {@link Object#finalize()} for further
     * information about migration options.
     */
    @Deprecated(since = "9", forRemoval = true)
    protected void finalize() throws IOException {
    }
    
    
    
    private static native void initIDs();
    
    /**
     * Opens a file, with the specified name, for overwriting or appending.
     *
     * @param name   name of file to be opened
     * @param append whether the file is to be opened in append mode
     */
    // 更新文件底层的append标记，指示写入新内容时需要覆盖(append=false)或追加(append=true)
    private void open(String name, boolean append) throws FileNotFoundException {
        open0(name, append);
    }
    
    /**
     * Opens a file, with the specified name, for overwriting or appending.
     *
     * @param name   name of file to be opened
     * @param append whether the file is to be opened in append mode
     */
    private native void open0(String name, boolean append) throws FileNotFoundException;
    
    /**
     * Writes the specified byte to this file output stream.
     *
     * @param b      the byte to be written.
     * @param append {@code true} if the write operation first
     *               advances the position to the end of file
     */
    private native void write(int b, boolean append) throws IOException;
    
    /**
     * Writes a sub array as a sequence of bytes.
     *
     * @param b      the data to be written
     * @param off    the start offset in the data
     * @param len    the number of bytes that are written
     * @param append {@code true} to first advance the position to the
     *               end of file
     *
     * @throws IOException If an I/O error has occurred.
     */
    private native void writeBytes(byte[] b, int off, int len, boolean append) throws IOException;
    
    /**
     * Returns a finalizer object if the FOS needs a finalizer; otherwise null.
     * If the FOS has a close method; it needs an AltFinalizer.
     */
    /*
     * 为FileOutputStream的子类构造一个终结器
     *
     * 如果输出流是FileOutputStream类型，则返回null
     * 如果输出流是FileOutputStream的子类，则递归向上查找该类及其父类中的close方法，
     * 如果一直找不到close方法，返回null，
     * 如果查找过程中发现了close方法，则返回包装了fos的一个终结器
     */
    private static Object getFinalizer(FileOutputStream fos) {
        Class<?> clazz = fos.getClass();
        
        while(clazz != FileOutputStream.class) {
            try {
                // 返回当前类中指定名称和形参的方法，但不包括父类/父接口中的方法
                clazz.getDeclaredMethod("close");
                return new AltFinalizer(fos);
            } catch(NoSuchMethodException nsme) {
                // ignore
            }
            clazz = clazz.getSuperclass();
        }
        
        return null;
    }
    
    
    
    /**
     * Class to call {@code FileOutputStream.close} when finalized.
     * If finalization of the stream is needed, an instance is created
     * in its constructor(s).  When the set of instances
     * related to the stream is unreachable, the AltFinalizer performs
     * the needed call to the stream's {@code close} method.
     */
    // 终结器
    static class AltFinalizer {
        private final FileOutputStream fos;
        
        AltFinalizer(FileOutputStream fos) {
            this.fos = fos;
        }
        
        @Override
        @SuppressWarnings("deprecation")
        protected final void finalize() {
            try {
                if(fos.fd != null) {
                    if(fos.fd == FileDescriptor.out || fos.fd == FileDescriptor.err) {
                        // Subclass may override flush; otherwise it is no-op
                        fos.flush();
                    } else {
                        /*
                         * if fd is shared, the references in FileDescriptor
                         * will ensure that finalizer is only called when
                         * safe to do so. All references using the fd have
                         * become unreachable. We can call close()
                         */
                        fos.close();
                    }
                }
            } catch(IOException ioe) {
                // ignore
            }
        }
    }
}
