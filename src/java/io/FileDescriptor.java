/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.ref.PhantomCleanable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Instances of the file descriptor class serve as an opaque handle
 * to the underlying machine-specific structure representing an open
 * file, an open socket, or another source or sink of bytes.
 * The main practical use for a file descriptor is to create a
 * {@link FileInputStream} or {@link FileOutputStream} to contain it.
 * <p>
 * Applications should not create their own file descriptors.
 *
 * @author Pavani Diwanji
 * @since 1.0
 */
/*
 * 文件描述符用来保存操作系统中的标准流id和对文件的引用handle
 *
 * FileDescriptor实例会被FileInputStream/FileOutputStream/RandomAccessFile持有，
 * 这三个类在打开文件时，在JNI代码中使用open执行系统调用打开文件，得到文件描述符在JNI代码中设置到FileDescriptor的fd成员变量上
 *
 * 关闭FileInputStream/FileOutputStream/RandomAccessFile时，会关闭底层对应的文件描述符。
 * 关闭是文件底层是通过使用close执行系统调用实现的。
 *
 * 简单理解：在Java中，文件描述符是"文件"的抽象表示。文件可以是：file、socket或其他IO连接
 *
 * 注：应用程序不应创建自己的文件描述符。
 */
public final class FileDescriptor {
    /**
     * A handle to the standard input stream. Usually, this file
     * descriptor is not used directly, but rather via the input stream
     * known as {@code System.in}.
     *
     * @see java.lang.System#in
     */
    // 标准输入流，被封装为System.in
    public static final FileDescriptor in = new FileDescriptor(0);
    /**
     * A handle to the standard output stream. Usually, this file
     * descriptor is not used directly, but rather via the output stream
     * known as {@code System.out}.
     *
     * @see java.lang.System#out
     */
    // 标准输出流，被封装为System.out
    public static final FileDescriptor out = new FileDescriptor(1);
    /**
     * A handle to the standard error stream. Usually, this file
     * descriptor is not used directly, but rather via the output stream
     * known as {@code System.err}.
     *
     * @see java.lang.System#err
     */
    // 标准错误流，被封装为System.err
    public static final FileDescriptor err = new FileDescriptor(2);
    
    /*
     * 文件描述符(通用概念)
     *
     * 每个打开的文件都会为它分配一个fd值作为其唯一编号，通过该值可以找到对应的文件并进行相关操作。
     *
     * 在windows上，文件描述符是文件句柄结构中的一个字段。
     */
    private int fd;
    /*
     * 文件句柄(windows下的概念)
     *
     * 句柄是Windows下各种对象的标识符，比如文件、资源、菜单、光标等等。
     * 文件句柄和文件描述符类似，它也是一个非负整数，也用于定位文件数据在内存中的位置。
     *
     * 在Windows中，文件句柄可以看做是FILE*，即文件指针。
     */
    private long handle;
    
    // 存放文件描述符关联的唯一流对象（绝大多数情况）
    private Closeable parent;
    // 存放关联了文件描述符的所有文件(流)对象（使用流的带有文件描述符参数的构造方法会用到此字段）
    private List<Closeable> otherParents;
    
    // 判断fd是否被释放
    private boolean closed;
    
    /**
     * true, if file is opened for appending.
     */
    // 判断当前文件是否处于追加(append)模式
    private boolean append;
    
    /**
     * Cleanup in case FileDescriptor is not explicitly closed.
     */
    // 在为明确关闭FileDescriptor的情况下对其进行清理
    private PhantomCleanable<FileDescriptor> cleanup;
    
    static {
        initIDs();
    }
    
    /* This routine initializes JNI field offsets for the class */
    private static native void initIDs();
    
    // Set up JavaIOFileDescriptorAccess in SharedSecrets
    static {
        SharedSecrets.setJavaIOFileDescriptorAccess(new JavaIOFileDescriptorAccess() {
            public void set(FileDescriptor fdo, int fd) {
                fdo.set(fd);
            }
            
            public int get(FileDescriptor fdo) {
                return fdo.fd;
            }
            
            public void setAppend(FileDescriptor fdo, boolean append) {
                fdo.append = append;
            }
            
            public boolean getAppend(FileDescriptor fdo) {
                return fdo.append;
            }
            
            public void close(FileDescriptor fdo) throws IOException {
                fdo.close();
            }
            
            /* Register for a normal FileCleanable fd/handle cleanup. */
            public void registerCleanup(FileDescriptor fdo) {
                FileCleanable.register(fdo);
            }
            
            /* Register a custom PhantomCleanup. */
            public void registerCleanup(FileDescriptor fdo, PhantomCleanable<FileDescriptor> cleanup) {
                fdo.registerCleanup(cleanup);
            }
            
            public void unregisterCleanup(FileDescriptor fdo) {
                fdo.unregisterCleanup();
            }
            
            public void setHandle(FileDescriptor fdo, long handle) {
                fdo.setHandle(handle);
            }
            
            public long getHandle(FileDescriptor fdo) {
                return fdo.handle;
            }
        });
    }
    
    /**
     * Constructs an (invalid) FileDescriptor object.
     * The fd or handle is set later.
     */
    // 构造一个无效的文件描述符，后续设置其fd和handle
    public FileDescriptor() {
        fd = -1;
        handle = -1;
    }
    
    /**
     * Used for standard input, output, and error only.
     * For Windows the corresponding handle is initialized.
     * For Unix the append mode is cached.
     *
     * @param fd the raw fd number (0, 1, 2)
     */
    // 构造标准流（输入流、输出流、错误流）的文件描述符
    private FileDescriptor(int fd) {
        this.fd = fd;
        this.handle = getHandle(fd);
        this.append = getAppend(fd);
    }
    
    /**
     * Tests if this file descriptor object is valid.
     *
     * @return {@code true} if the file descriptor object represents a valid, open file, socket, or other active I/O connection; {@code false} otherwise.
     */
    // 判断文件描述符是否有效，判断依据是handle或fd
    public boolean valid() {
        return (handle != -1) || (fd != -1);
    }
    
    /**
     * Force all system buffers to synchronize with the underlying device.
     * This method returns after all modified data and attributes of this FileDescriptor have been written to the relevant device(s).
     * In particular, if this FileDescriptor refers to a physical storage medium,
     * such as a file in a file system, sync will not return until all in-memory modified copies of buffers associated with this FileDescriptor have been ritten to the physical medium.
     *
     * sync is meant to be used by code that requires physical storage (such as a file) to be in a known state.
     * For example, a class that provided a simple transaction facility might use sync to ensure that all changes to a file caused by a given transaction were recorded on a storage medium.
     *
     * sync only affects buffers downstream of this FileDescriptor.
     * If any in-memory buffering is being done by the application (for example, by a BufferedOutputStream object),
     * those buffers must be flushed into the FileDescriptor (for example, by invoking OutputStream.flush) before that data will be affected by sync.
     *
     * @throws SyncFailedException Thrown when the buffers cannot be flushed,
     *         or because the system cannot guarantee that all the buffers have been synchronized with physical media.
     * @since 1.1
     */
    public native void sync() throws SyncFailedException;
    
    /*
     * On Windows return the handle for the standard streams.
     */
    // 返回标准流（输入流、输出流、错误流）的句柄
    private static native long getHandle(int d);
    
    /**
     * Set the handle.
     * Used on Windows for regular files.
     * If setting to -1, clear the cleaner.
     * The {@link #registerCleanup} method should be called for new handles.
     *
     * @param handle the handle or -1 to indicate closed
     */
    /*
     * 设置文件句柄，常用于Windows系统的普通文件上。
     * 如果handle为-1，则开始清理资源。
     */
    @SuppressWarnings("unchecked")
    void setHandle(long handle) {
        if(handle == -1 && cleanup != null) {
            cleanup.clear();
            cleanup = null;
        }
        this.handle = handle;
    }
    
    /**
     * Returns true, if the file was opened for appending.
     */
    // 文件是否以append模式打开
    private static native boolean getAppend(int fd);
    
    /**
     * Set the fd.
     * Used on Unix and for sockets on Windows and Unix.
     * If setting to -1, clear the cleaner.
     * The {@link #registerCleanup} method should be called for new fds.
     *
     * @param fd the raw fd or -1 to indicate closed
     */
    /*
     * 设置文件描述符，常用于类UNIX系统。
     * 在socket编程中，也会将此方法用于Windows系统。
     * 如果fd为-1，则开始清理资源。
     */
    @SuppressWarnings("unchecked")
    synchronized void set(int fd) {
        if(fd == -1 && cleanup != null) {
            cleanup.clear();
            cleanup = null;
        }
        this.fd = fd;
    }
    
    /**
     * Attach a Closeable to this FD for tracking.
     * parent reference is added to otherParents when needed to make closeAll simpler.
     */
    /*
     * 将指定的文件(流)记录到当前关联的文件描述符中
     *
     * 如果FileDescriptor只和一个FileInputStream/FileOutputStream/RandomAccessFile有关联，则将流简单的保存到parent成员中。
     * 如果FileDescriptor和多个FileInputStream/FileOutputStream/RandomAccessFile有关联，则所有关联的Closeable流都被保存到otherParents这个ArrayList中。
     *
     * 一般来说，一个FileDescriptor只会和一个FileInputStream/FileOutputStream/RandomAccessFile有关联。
     * 但如果调用FileInputStream(FileDescriptor fdObj)这类构造函数时，会出现多个Closeable对象关联到一个FileDescriptor的情况。
     *
     * 这里既有parent又有otherParents，相当于做了一个小的优化。
     */
    synchronized void attach(Closeable c) {
        if(parent == null) {
            // first caller gets to do this
            parent = c; // 首次调用，关联至此
        } else if(otherParents == null) {
            otherParents = new ArrayList<>();
            otherParents.add(parent);
            otherParents.add(c);
        } else {
            otherParents.add(c);
        }
    }
    
    /**
     * Register a cleanup for the current handle.
     * Used directly in java.io and indirectly via fdAccess.
     * The cleanup should be registered after the handle is set in the FileDescriptor.
     *
     * @param cleanable a PhantomCleanable to register
     */
    // 关联一个文件描述符（虚引用）清理器
    @SuppressWarnings("unchecked")
    synchronized void registerCleanup(PhantomCleanable<FileDescriptor> cleanable) {
        Objects.requireNonNull(cleanable, "cleanable");
        if(cleanup != null) {
            // 清理之前追踪的文件描述符对象
            cleanup.clear();
        }
        cleanup = cleanable;
    }
    
    /**
     * Unregister a cleanup for the current raw fd or handle.
     * Used directly in java.io and indirectly via fdAccess.
     * Normally {@link #close()} should be used except in cases where
     * it is certain the caller will close the raw fd and the cleanup
     * must not close the raw fd.  {@link #unregisterCleanup()} must be
     * called before the raw fd is closed to prevent a race that makes
     * it possible for the fd to be reallocated to another use and later
     * the cleanup might be invoked.
     */
    // 置空关联的文件描述符（虚引用）清理器
    synchronized void unregisterCleanup() {
        if(cleanup != null) {
            // 清理当前追踪的文件描述符对象
            cleanup.clear();
        }
        cleanup = null;
    }
    
    /**
     * Close the raw file descriptor or handle, if it has not already been closed.
     * The native code sets the fd and handle to -1.
     * Clear the cleaner so the close does not happen twice.
     * Package private to allow it to be used in java.io.
     *
     * @throws IOException if close fails
     */
    // 关闭文件描述符
    @SuppressWarnings("unchecked")
    synchronized void close() throws IOException {
        unregisterCleanup();
        close0();
    }
    
    /**
     * Cycle through all Closeables sharing this FD and call
     * close() on each one.
     *
     * The caller closeable gets to call close0().
     */
    /*
     * 该方法会调用其所有关联的流的close方法。
     * 在流的close方法中，辗转又调用了文件描述符的close方法。
     *
     * 换句话说，closeAll方法会关闭其所有关联的流，并释放该文件描述符。
     *
     * 反过来，调用一个流的close方法，往往也会引发关闭与其共享一个文件描述符的其他流以及这个文件描述符。
     */
    @SuppressWarnings("try")
    synchronized void closeAll(Closeable releaser) throws IOException {
        if(!closed) {
            closed = true;
            IOException ioe = null;
            try(releaser) {
                if(otherParents != null) {
                    // 遍历所有流，并调用其close方法
                    for(Closeable referent : otherParents) {
                        try {
                            referent.close();
                        } catch(IOException x) {
                            if(ioe == null) {
                                ioe = x;
                            } else {
                                ioe.addSuppressed(x);
                            }
                        }
                    }
                }
            }
//            catch(IOException ex) {
//                /*
//                 * If releaser close() throws IOException, add other exceptions as suppressed.
//                 */
//                if(ioe != null)
//                    ex.addSuppressed(ioe);
//                ioe = ex;
//            }
            finally {
                if(ioe != null)
                    throw ioe;
            }
        }
    }
    
    /**
     * Close the raw file descriptor or handle, if it has not already been closed and set the fd and handle to -1.
     */
    private native void close0() throws IOException;
    
}
