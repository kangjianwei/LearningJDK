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
import sun.nio.ch.FileChannelImpl;

/**
 * A <code>FileInputStream</code> obtains input bytes
 * from a file in a file system. What files
 * are  available depends on the host environment.
 *
 * <p><code>FileInputStream</code> is meant for reading streams of raw bytes
 * such as image data. For reading streams of characters, consider using
 * <code>FileReader</code>.
 *
 * @author Arthur van Hoff
 * @apiNote To release resources used by this stream {@link #close} should be called
 * directly or by try-with-resources. Subclasses are responsible for the cleanup
 * of resources acquired by the subclass.
 * Subclasses that override {@link #finalize} in order to perform cleanup
 * should be modified to use alternative cleanup mechanisms such as
 * {@link java.lang.ref.Cleaner} and remove the overriding {@code finalize} method.
 * @implSpec If this FileInputStream has been subclassed and the {@link #close}
 * method has been overridden, the {@link #close} method will be
 * called when the FileInputStream is unreachable.
 * Otherwise, it is implementation specific how the resource cleanup described in
 * {@link #close} is performed.
 * @see java.io.File
 * @see java.io.FileDescriptor
 * @see java.io.FileOutputStream
 * @see java.nio.file.Files#newInputStream
 * @since 1.0
 */
// 文件输入流：将文件作为输入源
public class FileInputStream extends InputStream {
    
    private final Object closeLock = new Object();
    
    /* File Descriptor - handle to the open file */
    // 该输入流关联的文件描述符
    private final FileDescriptor fd;
    
    /**
     * The path of the referenced file
     * (null if the stream is created with a file descriptor)
     */
    // 输入流的path（获取数据的源头），使用File的path来初始化
    private final String path;
    
    // 应用于当前输入流的终结器
    private final Object altFinalizer;
    
    // 当前输入流关联的通道，调用getChannel()后才会对其初始化
    private volatile FileChannel channel;
    
    // 输入流是否已关闭
    private volatile boolean closed;
    
    
    static {
        initIDs();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a <code>FileInputStream</code> by
     * opening a connection to an actual file,
     * the file named by the path name <code>name</code>
     * in the file system.  A new <code>FileDescriptor</code>
     * object is created to represent this file
     * connection.
     * <p>
     * First, if there is a security
     * manager, its <code>checkRead</code> method
     * is called with the <code>name</code> argument
     * as its argument.
     * <p>
     * If the named file does not exist, is a directory rather than a regular
     * file, or for some other reason cannot be opened for reading then a
     * <code>FileNotFoundException</code> is thrown.
     *
     * @param name the system-dependent file name.
     *
     * @throws FileNotFoundException if the file does not exist,
     *                               is a directory rather than a regular file,
     *                               or for some other reason cannot be opened for
     *                               reading.
     * @throws SecurityException     if a security manager exists and its
     *                               <code>checkRead</code> method denies read access
     *                               to the file.
     * @see java.lang.SecurityManager#checkRead(java.lang.String)
     */
    // 创建指定文件（名）的输入流
    public FileInputStream(String name) throws FileNotFoundException {
        this(name != null ? new File(name) : null);
    }
    
    /**
     * Creates a <code>FileInputStream</code> by
     * opening a connection to an actual file,
     * the file named by the <code>File</code>
     * object <code>file</code> in the file system.
     * A new <code>FileDescriptor</code> object
     * is created to represent this file connection.
     * <p>
     * First, if there is a security manager,
     * its <code>checkRead</code> method  is called
     * with the path represented by the <code>file</code>
     * argument as its argument.
     * <p>
     * If the named file does not exist, is a directory rather than a regular
     * file, or for some other reason cannot be opened for reading then a
     * <code>FileNotFoundException</code> is thrown.
     *
     * @param file the file to be opened for reading.
     *
     * @throws FileNotFoundException if the file does not exist,
     *                               is a directory rather than a regular file,
     *                               or for some other reason cannot be opened for
     *                               reading.
     * @throws SecurityException     if a security manager exists and its
     *                               <code>checkRead</code> method denies read access to the file.
     * @see java.io.File#getPath()
     * @see java.lang.SecurityManager#checkRead(java.lang.String)
     */
    // 创建指定文件的输入流
    public FileInputStream(File file) throws FileNotFoundException {
        String name = (file != null ? file.getPath() : null);
        
        // 检查访问权限
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkRead(name);
        }
        
        if(name == null) {
            throw new NullPointerException();
        }
        
        if(file.isInvalid()) {
            throw new FileNotFoundException("Invalid file path");
        }
        
        // 为当前流锁定文件描述符
        fd = new FileDescriptor();
        
        // 与此同时，将当前文件(流)记录到关联的文件描述符fd中
        fd.attach(this);
        
        // 用File的path初始化输入流的path（获取数据的源头）
        path = name;
        
        // 打开指定名称的文件，以便读取内容
        open(name);
        
        // 初始化应用于当前输入流的终结器
        altFinalizer = getFinalizer(this);
        // 如果该输入流没有终结器，需要为其注册清理器
        if(altFinalizer == null) {
            // 注册文件描述符fd到清理器
            FileCleanable.register(fd);       // open set the fd, register the cleanup
        }
    }
    
    /**
     * Creates a <code>FileInputStream</code> by using the file descriptor
     * <code>fdObj</code>, which represents an existing connection to an
     * actual file in the file system.
     * <p>
     * If there is a security manager, its <code>checkRead</code> method is
     * called with the file descriptor <code>fdObj</code> as its argument to
     * see if it's ok to read the file descriptor. If read access is denied
     * to the file descriptor a <code>SecurityException</code> is thrown.
     * <p>
     * If <code>fdObj</code> is null then a <code>NullPointerException</code>
     * is thrown.
     * <p>
     * This constructor does not throw an exception if <code>fdObj</code>
     * is {@link java.io.FileDescriptor#valid() invalid}.
     * However, if the methods are invoked on the resulting stream to attempt
     * I/O on the stream, an <code>IOException</code> is thrown.
     *
     * @param fdObj the file descriptor to be opened for reading.
     *
     * @throws SecurityException if a security manager exists and its
     *                           <code>checkRead</code> method denies read access to the
     *                           file descriptor.
     * @see SecurityManager#checkRead(java.io.FileDescriptor)
     */
    // 直接使用文件描述符初始化输入流
    public FileInputStream(FileDescriptor fdObj) {
        SecurityManager security = System.getSecurityManager();
        if(fdObj == null) {
            throw new NullPointerException();
        }
        
        if(security != null) {
            security.checkRead(fdObj);
        }
        
        fd = fdObj;
        path = null;
        altFinalizer = null;
        
        /*
         * FileDescriptor is being shared by streams.
         * Register this stream with FileDescriptor tracker.
         */
        // 将当前文件(流)记录到关联的文件描述符fd中
        fd.attach(this);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads a byte of data from this input stream. This method blocks
     * if no input is yet available.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * file is reached.
     *
     * @throws IOException if an I/O error occurs.
     */
    /*
     * 尝试从当前输入流读取一个字节，读取成功直接返回，读取失败返回-1
     */
    public int read() throws IOException {
        return read0();
    }
    
    /**
     * Reads up to <code>b.length</code> bytes of data from this input
     * stream into an array of bytes. This method blocks until some input
     * is available.
     *
     * @param b the buffer into which the data is read.
     *
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the file has been reached.
     *
     * @throws IOException if an I/O error occurs.
     */
    /*
     * 尝试从当前输入流读取dst.length个字节，并将读到的内容插入到dst的起点处
     * 返回值表示成功读取的字节数量(可能小于预期值)，返回-1表示已经没有可读内容了
     */
    public int read(byte[] b) throws IOException {
        return readBytes(b, 0, b.length);
    }
    
    /**
     * Reads up to <code>len</code> bytes of data from this input stream
     * into an array of bytes. If <code>len</code> is not zero, the method
     * blocks until some input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read.
     *
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the file has been reached.
     *
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws IOException               if an I/O error occurs.
     */
    /*
     * 尝试从当前输入流读取len个字节，并将读到的内容插入到字节数组b的off索引处
     * 返回值表示成功读取的字节数量(可能小于预期值)，返回-1表示已经没有可读内容了
     */
    public int read(byte[] b, int off, int len) throws IOException {
        return readBytes(b, off, len);
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the unique {@link java.nio.channels.FileChannel FileChannel}
     * object associated with this file input stream.
     *
     * <p> The initial {@link java.nio.channels.FileChannel#position()
     * position} of the returned channel will be equal to the
     * number of bytes read from the file so far.  Reading bytes from this
     * stream will increment the channel's position.  Changing the channel's
     * position, either explicitly or by reading, will change this stream's
     * file position.
     *
     * @return the file channel associated with this file input stream
     *
     * @spec JSR-51
     * @since 1.4
     */
    // 返回当前文件输入流关联的只读通道
    public FileChannel getChannel() {
        FileChannel fc = this.channel;
        
        if(fc == null) {
            synchronized(this) {
                fc = this.channel;
                // 线程安全，双重检查机制
                if(fc == null) {
                    // 为输入流关联一个通道（懒加载）
                    this.channel = fc = FileChannelImpl.open(fd, path, true, false, false, this);
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
     * Returns the <code>FileDescriptor</code>
     * object  that represents the connection to
     * the actual file in the file system being
     * used by this <code>FileInputStream</code>.
     *
     * @return the file descriptor object associated with this stream.
     *
     * @throws IOException if an I/O error occurs.
     * @see java.io.FileDescriptor
     */
    // 返回当前输入流锁定的文件描述符
    public final FileDescriptor getFD() throws IOException {
        if(fd != null) {
            return fd;
        }
        
        throw new IOException();
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes this file input stream and releases any system resources
     * associated with the stream.
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
    // 关闭文件输入流
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
    
    /**
     * Returns an estimate of the number of remaining bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. Returns 0 when the file
     * position is beyond EOF. The next invocation might be the same thread
     * or another thread. A single read or skip of this many bytes will not
     * block, but may read or skip fewer bytes.
     *
     * <p> In some cases, a non-blocking read (or skip) may appear to be
     * blocked when it is merely slow, for example when reading large
     * files over slow networks.
     *
     * @return an estimate of the number of remaining bytes that can be read
     * (or skipped over) from this input stream without blocking.
     *
     * @throws IOException if this file input stream has been closed by calling
     *                     {@code close} or an I/O error occurs.
     */
    // 返回剩余可不被阻塞地读取（或跳过）的字节数（估计值）
    public int available() throws IOException {
        return available0();
    }
    
    /**
     * Skips over and discards <code>n</code> bytes of data from the
     * input stream.
     *
     * <p>The <code>skip</code> method may, for a variety of
     * reasons, end up skipping over some smaller number of bytes,
     * possibly <code>0</code>. If <code>n</code> is negative, the method
     * will try to skip backwards. In case the backing file does not support
     * backward skip at its current position, an <code>IOException</code> is
     * thrown. The actual number of bytes skipped is returned. If it skips
     * forwards, it returns a positive value. If it skips backwards, it
     * returns a negative value.
     *
     * <p>This method may skip more bytes than what are remaining in the
     * backing file. This produces no exception and the number of bytes skipped
     * may include some number of bytes that were beyond the EOF of the
     * backing file. Attempting to read from the stream after skipping past
     * the end will result in -1 indicating the end of the file.
     *
     * @param n the number of bytes to be skipped.
     *
     * @return the actual number of bytes skipped.
     *
     * @throws IOException if n is negative, if the stream does not
     *                     support seek, or if an I/O error occurs.
     */
    // 向前/向后跳过n个字节
    public long skip(long n) throws IOException {
        return skip0(n);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Ensures that the {@link #close} method of this file input stream is
     * called when there are no more references to it.
     * The {@link #finalize} method does not call {@link #close} directly.
     *
     * @throws IOException if an I/O error occurs.
     * @apiNote To release resources used by this stream {@link #close} should be called
     * directly or by try-with-resources.
     * @implSpec If this FileInputStream has been subclassed and the {@link #close}
     * method has been overridden, the {@link #close} method will be
     * called when the FileInputStream is unreachable.
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
     * Opens the specified file for reading.
     *
     * @param name the name of the file
     */
    /*
     * 打开指定名称的文件，以便读取内容
     *
     * 该步骤会为FileDescriptor中的handle（文件句柄）赋值
     */
    private void open(String name) throws FileNotFoundException {
        open0(name);
    }
    
    /**
     * Opens the specified file for reading.
     *
     * @param name the name of the file
     */
    private native void open0(String name) throws FileNotFoundException;
    
    private native int read0() throws IOException;
    
    /**
     * Reads a subarray as a sequence of bytes.
     *
     * @param b   the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     *
     * @throws IOException If an I/O error has occurred.
     */
    private native int readBytes(byte[] b, int off, int len) throws IOException;
    
    private native int available0() throws IOException;
    
    private native long skip0(long n) throws IOException;
    
    /**
     * Returns a finalizer object if the FIS needs a finalizer; otherwise null.
     * If the FIS has a close method; it needs an AltFinalizer.
     */
    /*
     * 为FileInputStream的子类构造一个终结器
     *
     * 如果输入流是FileInputStream类型，则返回null
     * 如果输入流是FileInputStream的子类，则递归向上查找该类及其父类中的close方法，
     * 如果一直找不到close方法，返回null，
     * 如果查找过程中发现了close方法，则返回包装了fis的一个终结器
     */
    private static Object getFinalizer(FileInputStream fis) {
        Class<?> clazz = fis.getClass();
        
        while(clazz != FileInputStream.class) {
            try {
                clazz.getDeclaredMethod("close");
                return new AltFinalizer(fis);
            } catch(NoSuchMethodException nsme) {
                // ignore
            }
            clazz = clazz.getSuperclass();
        }
        
        return null;
    }
    
    
    
    /**
     * Class to call {@code FileInputStream.close} when finalized.
     * If finalization of the stream is needed, an instance is created
     * in its constructor(s).  When the set of instances
     * related to the stream is unreachable, the AltFinalizer performs
     * the needed call to the stream's {@code close} method.
     */
    // 终结器
    static class AltFinalizer {
        private final FileInputStream fis;
        
        AltFinalizer(FileInputStream fis) {
            this.fis = fis;
        }
        
        @Override
        @SuppressWarnings("deprecation")
        protected final void finalize() {
            try {
                if((fis.fd != null) && (fis.fd != FileDescriptor.in)) {
                    /*
                     * if fd is shared, the references in FileDescriptor
                     * will ensure that finalizer is only called when
                     * safe to do so. All references using the fd have
                     * become unreachable. We can call close()
                     */
                    fis.close();
                }
            } catch(IOException ioe) {
                // ignore
            }
        }
    }
}
