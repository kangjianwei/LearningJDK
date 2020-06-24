/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static sun.nio.fs.WindowsConstants.ERROR_DIRECTORY;
import static sun.nio.fs.WindowsNativeDispatcher.FindClose;
import static sun.nio.fs.WindowsNativeDispatcher.FindFirstFile;
import static sun.nio.fs.WindowsNativeDispatcher.FindNextFile;
import static sun.nio.fs.WindowsNativeDispatcher.FirstFile;

/**
 * Windows implementation of DirectoryStream
 */
// 目录流，用来搜寻目录内的直接子项，不会递归遍历
class WindowsDirectoryStream implements DirectoryStream<Path> {
    
    // 锁，关闭流和迭代流的动作的互斥的
    private final Object closeLock = new Object();
    
    // 当前目录的路径
    private final WindowsPath dir;
    
    // 当前目录内首个直接子项的文件句柄，该子项通常是"."，即目录自身
    private final long handle;  /* handle to directory */
    
    // 当前目录内首个直接子项的文件名称，该子项通常是"."，即目录自身
    private final String firstName; /* first entry in the directory */
    
    // 目录流过滤器，用来筛选感兴趣的文件/目录
    private final DirectoryStream.Filter<? super Path> filter;
    
    // 本地内存，存储搜索到的直接子项
    private final NativeBuffer findDataBuffer;  /* buffer for WIN32_FIND_DATA structure that receives information about file */
    
    // 目录迭代器
    private Iterator<Path> iterator;
    
    // 标记当前目录流是否处于开启状态
    private boolean isOpen = true;  // need closeLock to access these
    
    
    WindowsDirectoryStream(WindowsPath dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        this.dir = dir;         // 待遍历的目录
        
        this.filter = filter;   // 目录流过滤器
        
        try {
            /* Need to append * or \* to match entries in directory */
            // 解析指定的windows路径为适用windows系统的绝对路径
            String search = dir.getPathForWin32Calls();
            
            // 将search的结尾更新为":*"或为"\*"
            char last = search.charAt(search.length() - 1);
            if(last == ':' || last == '\\') {
                search += "*";
            } else {
                search += "\\*";
            }
            
            // 获取search目录内首个直接子项，通常是"."，即目录自身
            FirstFile first = FindFirstFile(search);
            
            // 首个直接子项的文件句柄，该子项通常是"."，即目录自身
            this.handle = first.handle();
            
            // 首个直接子项的文件名称，该子项通常是"."，即目录自身
            this.firstName = first.name();
            
            // 获取一块本地内存，用于存储查找到的文件信息
            this.findDataBuffer = WindowsFileAttributes.getBufferForFindData();
        } catch(WindowsException x) {
            if(x.lastError() == ERROR_DIRECTORY) {
                throw new NotDirectoryException(dir.getPathForExceptionMessage());
            }
            x.rethrowAsIOException(dir);
            
            // keep compiler happy
            throw new AssertionError();
        }
    }
    
    
    // 返回目录流迭代器，用来遍历目录内的直接子项
    @Override
    public Iterator<Path> iterator() {
        if(!isOpen) {
            throw new IllegalStateException("Directory stream is closed");
        }
        
        synchronized(this) {
            if(iterator != null) {
                throw new IllegalStateException("Iterator already obtained");
            }
            
            // 传入当前目录中首个直接子项的名称(该子项通常是"."，即目录自身)，构造一个目录流迭代器
            iterator = new WindowsDirectoryIterator(firstName);
            
            return iterator;
        }
    }
    
    // 关闭目录流(已遍历完，或因其他原因关闭)
    @Override
    public void close() throws IOException {
        synchronized(closeLock) {
            if(!isOpen) {
                return;
            }
            
            // 标记当前目录流已经关闭
            isOpen = false;
        }
        
        // 释放本地内存
        findDataBuffer.release();
        
        try {
            // 关闭文件句柄
            FindClose(handle);
        } catch(WindowsException x) {
            x.rethrowAsIOException(dir);
        }
    }
    
    
    // 目录迭代器（仅限内部使用）
    private class WindowsDirectoryIterator implements Iterator<Path> {
        
        private String first;   // 当前目录内首个直接子项，该子项通常是"."，即目录自身
        private Path nextEntry; // 下一个搜索到的直接子项（与first平级）
        
        private String prefix;  // 待遍历的直接子项的前缀，通常是当前目录的名称
        
        private boolean atEof;  // 对当前目录的搜索是否结束
        
        
        // 根据当前目录内的首个直接子项(通常是目录自身)构造一个目录流迭代器
        WindowsDirectoryIterator(String first) {
            atEof = false;
            
            this.first = first;
            
            // 如果绝对化dir时是否需要先插入"\"
            if(dir.needsSlashWhenResolving()) {
                prefix = dir.toString() + "\\"; // 插入'\'，因为这是windows系统
            } else {
                prefix = dir.toString();
            }
        }
        
        // 是否存在下一个直接子项(会忽略"."和".."，且需要通过目录流过滤器对遇到的路径进行筛选)
        @Override
        public synchronized boolean hasNext() {
            if(nextEntry == null && !atEof) {
                // 获取下一个直接子项
                nextEntry = readNextEntry();
            }
            
            return nextEntry != null;
        }
        
        // 获取下一个直接子项(会忽略"."和".."，且需要通过目录流过滤器对遇到的路径进行筛选)
        @Override
        public synchronized Path next() {
            Path result = null;
            
            if(nextEntry == null && !atEof) {
                // 获取下一个直接子项(文件/目录)
                result = readNextEntry();
            } else {
                result = nextEntry;
                nextEntry = null;
            }
            
            if(result == null) {
                throw new NoSuchElementException();
            }
            
            return result;
        }
        
        // 不支持移除动作
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        /** links to self and parent directories are ignored */
        // 判断是否为"."或为".."目录
        private boolean isSelfOrParent(String name) {
            return name.equals(".") || name.equals("..");
        }
        
        /** applies filter and also ignores "." and ".." */
        // 使用目录流过滤器，筛选出感兴趣的路径
        private Path acceptEntry(String path, BasicFileAttributes attrs) {
            // 路径工厂，创建windows平台的路径对象(不会做本地化操作)，允许缓存指定的"basic"文件属性
            Path entry = WindowsPath.createFromNormalizedPath(dir.getFileSystem(), prefix + path, attrs);
            
            try {
                if(filter.accept(entry)) {
                    return entry;
                }
            } catch(IOException ioe) {
                throw new DirectoryIteratorException(ioe);
            }
            
            return null;
        }
        
        /** reads next directory entry */
        // 查找下一个直接子项(会忽略"."和".."，且需要通过目录流过滤器对遇到的路径进行筛选)
        private Path readNextEntry() {
            /* handle first element returned by search */
            if(first != null) {
                // 尝试用目录内首个直接子项作为新的目录流的递归遍历起点
                nextEntry = isSelfOrParent(first) ? null                          // 如果为"."或为".."目录，返回null
                    : acceptEntry(first, null);     // 否则，使用目录流过滤器，筛选出感兴趣的文件/目录
                
                // 置空first，因为首个直接子项已经完成了交接
                first = null;
                
                if(nextEntry != null) {
                    return nextEntry;
                }
            }
            
            // 进入死循环，直到找出下个可以作为目录流根目录的直接子项
            for(; ; ) {
                String name = null;
                WindowsFileAttributes attrs;
                
                /* synchronize on closeLock to prevent close while reading */
                synchronized(closeLock) {
                    // 本地内存地址
                    long address = findDataBuffer.address();
                    
                    try {
                        if(isOpen) {
                            // 基于上次的搜索(如FindFirstFile或FindNextFile)来搜索下一个文件，搜索到的文件信息存储到address指示的内存中
                            name = FindNextFile(handle, address);
                        }
                    } catch(WindowsException x) {
                        IOException ioe = x.asIOException(dir);
                        throw new DirectoryIteratorException(ioe);
                    }
                    
                    /* NO_MORE_FILES or stream closed */
                    // 如果已经搜索完，或者流已经关闭，返回null
                    if(name == null) {
                        // 标记本目录已搜索完
                        atEof = true;
                        return null;
                    }
                    
                    /* ignore link to self and parent directories */
                    // 如果为"."或为".."目录，则忽略它
                    if(isSelfOrParent(name)) {
                        continue;
                    }
                    
                    /*
                     * grab the attributes from the WIN32_FIND_DATA structure
                     * (needs to be done while holding closeLock because close will release the buffer)
                     */
                    // 从address指示的内存中获取windows文件属性信息，并构造WindowsFileAttributes成对象后返回。应用于WIN32_FIND_DATA结构体
                    attrs = WindowsFileAttributes.fromFindData(address);
                }
                
                /* return entry if accepted by filter */
                // 使用目录流过滤器，筛选出感兴趣的文件/目录
                Path entry = acceptEntry(name, attrs);
                
                // 如果找到了合适的直接子项，则返回它，否则需要继续循环
                if(entry != null) {
                    return entry;
                }
            }// for(; ; )
        }
    }
}
