/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.internal.misc.SharedSecrets;
import sun.nio.ch.FileChannelImpl;
import sun.nio.ch.ThreadPool;
import sun.nio.ch.WindowsAsynchronousFileChannelImpl;

import static sun.nio.fs.WindowsConstants.CREATE_NEW;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_NORMAL;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_DELETE_ON_CLOSE;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_OPEN_REPARSE_POINT;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_OVERLAPPED;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_WRITE_THROUGH;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_DELETE;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_READ;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_WRITE;
import static sun.nio.fs.WindowsConstants.GENERIC_READ;
import static sun.nio.fs.WindowsConstants.GENERIC_WRITE;
import static sun.nio.fs.WindowsConstants.OPEN_ALWAYS;
import static sun.nio.fs.WindowsConstants.OPEN_EXISTING;
import static sun.nio.fs.WindowsConstants.TRUNCATE_EXISTING;
import static sun.nio.fs.WindowsNativeDispatcher.CloseHandle;
import static sun.nio.fs.WindowsNativeDispatcher.CreateFile;
import static sun.nio.fs.WindowsNativeDispatcher.DeviceIoControlSetSparse;
import static sun.nio.fs.WindowsNativeDispatcher.SetEndOfFile;

/**
 * Factory to create FileChannels and AsynchronousFileChannels.
 */
// 文件通道工厂的本地实现，用来创建同步/异步文件通道
class WindowsChannelFactory {
    
    /**
     * Do not follow reparse points when opening an existing file. Do not fail if the file is a reparse point.
     */
    // 打开现有文件时不要追踪reparse points。如果文件是reparse point，不会打开失败
    static final OpenOption OPEN_REPARSE_POINT = new OpenOption() {
    };
    
    // 访问来自FileDescriptor的后门方法
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();
    
    private WindowsChannelFactory() {
    }
    
    /**
     * Open/creates file, returning FileChannel to access the file
     *
     * @param pathForWindows The path of the file to open/create
     * @param pathToCheck    The path used for permission checks (if security manager)
     */
    // 创建/打开一个文件，并返回其关联的非异步文件通道
    static FileChannel newFileChannel(String pathForWindows, String pathToCheck, Set<? extends OpenOption> options, long pSecurityDescriptor) throws WindowsException {
        Flags flags = Flags.toFlags(options);
        
        // default is reading; append => writing
        if(!flags.read && !flags.write) {
            if(flags.append) {
                flags.write = true;
            } else {
                flags.read = true;  // 默认使用"读"模式
            }
        }
        
        // validation
        if(flags.read && flags.append) {
            throw new IllegalArgumentException("READ + APPEND not allowed");
        }
        
        if(flags.append && flags.truncateExisting) {
            throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed");
        }
        
        // 创建/打开一个文件，并返回其文件描述符
        FileDescriptor fdObj = open(pathForWindows, pathToCheck, flags, pSecurityDescriptor);
        
        // 返回为文件关联的IO通道
        return FileChannelImpl.open(fdObj, pathForWindows, flags.read, flags.write, flags.direct, null);
    }
    
    /**
     * Open/creates file, returning AsynchronousFileChannel to access the file
     *
     * @param pathForWindows The path of the file to open/create
     * @param pathToCheck    The path used for permission checks (if security manager)
     * @param pool           The thread pool that the channel is associated with
     */
    // 创建/打开一个文件，并返回其关联的异步文件通道，工作线程在这个过程中会被启动并阻塞
    static AsynchronousFileChannel newAsynchronousFileChannel(String pathForWindows, String pathToCheck, Set<? extends OpenOption> options, long pSecurityDescriptor, ThreadPool pool) throws IOException {
        // 解析文件操作属性
        Flags flags = Flags.toFlags(options);
        
        /* Overlapped I/O required */
        // 强制使用重叠IO结构（异步通道必须）
        flags.overlapped = true;
        
        /* default is reading */
        // 默认使文件通道可读
        if(!flags.read && !flags.write) {
            flags.read = true;
        }
        
        // 要求文件可追加
        if(flags.append) {
            throw new UnsupportedOperationException("APPEND not allowed");
        }
        
        // open file for overlapped I/O
        FileDescriptor fdObj;
        try {
            // 创建/打开一个文件，并返回其文件描述符
            fdObj = open(pathForWindows, pathToCheck, flags, pSecurityDescriptor);
        } catch(WindowsException x) {
            x.rethrowAsIOException(pathForWindows);
            return null;
        }
        
        // create the AsynchronousFileChannel
        try {
            // 返回为文件关联的异步IO通道，此过程中会将工作线程就绪并阻塞，等待新的操作到达并执行完之后，再唤醒工作线程
            return WindowsAsynchronousFileChannelImpl.open(fdObj, flags.read, flags.write, pool);
        } catch(IOException x) {
            // IOException is thrown if the file handle cannot be associated
            // with the completion port. All we can do is close the file.
            fdAccess.close(fdObj);
            throw x;
        }
    }
    
    /**
     * Opens file based on parameters and options,
     * returning a FileDescriptor encapsulating the handle to the open file.
     */
    // 创建/打开一个文件，并返回其文件描述符
    private static FileDescriptor open(String pathForWindows, String pathToCheck, Flags flags, long pSecurityDescriptor) throws WindowsException {
        
        // 打开文件时是否将其长度截断为0
        boolean truncateAfterOpen = false; // set to true if file must be truncated after open
        
        /* map options */
        // 访问属性(读/写)
        int dwDesiredAccess = 0;
        if(flags.read) {
            dwDesiredAccess |= GENERIC_READ;
        }
        if(flags.write) {
            dwDesiredAccess |= GENERIC_WRITE;
        }
        
        // 共享属性
        int dwShareMode = 0;
        if(flags.shareRead) {
            dwShareMode |= FILE_SHARE_READ;
        }
        if(flags.shareWrite) {
            dwShareMode |= FILE_SHARE_WRITE;
        }
        if(flags.shareDelete) {
            dwShareMode |= FILE_SHARE_DELETE;
        }
        
        // 文件属性
        int dwFlagsAndAttributes = FILE_ATTRIBUTE_NORMAL;
        
        // 创建属性
        int dwCreationDisposition = OPEN_EXISTING;
        
        if(flags.write) {
            if(flags.createNew) {
                dwCreationDisposition = CREATE_NEW;
                // force create to fail if file is orphaned reparse point
                dwFlagsAndAttributes |= FILE_FLAG_OPEN_REPARSE_POINT;
            } else {
                if(flags.create) {
                    dwCreationDisposition = OPEN_ALWAYS;
                }
                
                if(flags.truncateExisting) {
                    /*
                     * Windows doesn't have a creation disposition that exactly
                     * corresponds to CREATE + TRUNCATE_EXISTING so we use
                     * the OPEN_ALWAYS mode and then truncate the file.
                     */
                    if(dwCreationDisposition == OPEN_ALWAYS) {
                        truncateAfterOpen = true;
                    } else {
                        dwCreationDisposition = TRUNCATE_EXISTING;
                    }
                }
            }
        }
        
        if(flags.dsync || flags.sync) {
            dwFlagsAndAttributes |= FILE_FLAG_WRITE_THROUGH;
        }
        
        if(flags.overlapped) {
            dwFlagsAndAttributes |= FILE_FLAG_OVERLAPPED;
        }
        
        if(flags.deleteOnClose) {
            dwFlagsAndAttributes |= FILE_FLAG_DELETE_ON_CLOSE;
        }
        
        // 是否可以处理符号链接
        boolean okayToFollowLinks = true;   // NOFOLLOW_LINKS and NOFOLLOW_REPARSEPOINT mean open reparse point
        
        if(dwCreationDisposition != CREATE_NEW && (flags.noFollowLinks || flags.openReparsePoint || flags.deleteOnClose)) {
            if(flags.noFollowLinks || flags.deleteOnClose) {
                okayToFollowLinks = false;
            }
            
            dwFlagsAndAttributes |= FILE_FLAG_OPEN_REPARSE_POINT;
        }
        
        // permission check
        if(pathToCheck != null) {
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                if(flags.read) {
                    sm.checkRead(pathToCheck);
                }
                
                if(flags.write) {
                    sm.checkWrite(pathToCheck);
                    
                }
                
                if(flags.deleteOnClose) {
                    sm.checkDelete(pathToCheck);
                }
            }
        }
        
        // 创建/打开一个文件，返回文件句柄（底层引用）
        long handle = CreateFile(pathForWindows, dwDesiredAccess, dwShareMode, pSecurityDescriptor, dwCreationDisposition, dwFlagsAndAttributes);
        
        // make sure this isn't a symbolic link.
        if(!okayToFollowLinks) {
            try {
                // 返回handle文件的windows文件属性信息
                WindowsFileAttributes fileAttributes = WindowsFileAttributes.readAttributes(handle);
                // 如果handle文件是符号链接，则抛异常
                if(fileAttributes.isSymbolicLink()) {
                    throw new WindowsException("File is symbolic link");
                }
            } catch(WindowsException x) {
                CloseHandle(handle);
                throw x;
            }
        }
        
        // truncate file (for CREATE + TRUNCATE_EXISTING case)
        if(truncateAfterOpen) {
            try {
                SetEndOfFile(handle);
            } catch(WindowsException x) {
                CloseHandle(handle);
                throw x;
            }
        }
        
        // make the file sparse if needed
        if(dwCreationDisposition == CREATE_NEW && flags.sparse) {
            try {
                DeviceIoControlSetSparse(handle);
            } catch(WindowsException x) {
                // ignore as sparse option is hint
            }
        }
        
        // create FileDescriptor and return
        FileDescriptor fdObj = new FileDescriptor();
        fdAccess.setHandle(fdObj, handle);
        fdAccess.setAppend(fdObj, flags.append);
        fdAccess.registerCleanup(fdObj);
        
        return fdObj;
    }
    
    
    /**
     * Represents the flags from a user-supplied set of open options.
     */
    // 收集文件操作属性
    private static class Flags {
        boolean read;
        boolean write;
        boolean append;
        boolean truncateExisting;
        boolean create;
        boolean createNew;
        boolean deleteOnClose;
        boolean sparse;
        boolean overlapped;
        boolean sync;
        boolean dsync;
        boolean direct;
        
        // non-standard
        boolean shareRead = true;
        boolean shareWrite = true;
        boolean shareDelete = true;
        boolean noFollowLinks;
        boolean openReparsePoint;
        
        // 将OpenOption选项转换为Flags
        static Flags toFlags(Set<? extends OpenOption> options) {
            Flags flags = new Flags();
            
            for(OpenOption option : options) {
                if(option instanceof StandardOpenOption) {
                    switch((StandardOpenOption) option) {
                        case READ:
                            flags.read = true;
                            break;
                        case WRITE:
                            flags.write = true;
                            break;
                        case APPEND:
                            flags.append = true;
                            break;
                        case TRUNCATE_EXISTING:
                            flags.truncateExisting = true;
                            break;
                        case CREATE:
                            flags.create = true;
                            break;
                        case CREATE_NEW:
                            flags.createNew = true;
                            break;
                        case DELETE_ON_CLOSE:
                            flags.deleteOnClose = true;
                            break;
                        case SPARSE:
                            flags.sparse = true;
                            break;
                        case SYNC:
                            flags.sync = true;
                            break;
                        case DSYNC:
                            flags.dsync = true;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                    continue;
                }
                
                if(option == LinkOption.NOFOLLOW_LINKS) {
                    flags.noFollowLinks = true;
                    continue;
                }
                
                if(option == OPEN_REPARSE_POINT) {
                    flags.openReparsePoint = true;
                    continue;
                }
                
                if(ExtendedOptions.NOSHARE_READ.matches(option)) {
                    flags.shareRead = false;
                    continue;
                }
                
                if(ExtendedOptions.NOSHARE_WRITE.matches(option)) {
                    flags.shareWrite = false;
                    continue;
                }
                
                if(ExtendedOptions.NOSHARE_DELETE.matches(option)) {
                    flags.shareDelete = false;
                    continue;
                }
                
                if(ExtendedOptions.DIRECT.matches(option)) {
                    flags.direct = true;
                    continue;
                }
                
                if(option == null) {
                    throw new NullPointerException();
                }
                
                throw new UnsupportedOperationException();
            }
            
            return flags;
        }
    }
    
}
