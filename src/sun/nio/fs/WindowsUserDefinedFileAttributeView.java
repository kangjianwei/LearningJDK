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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jdk.internal.misc.Unsafe;

import static sun.nio.fs.WindowsConstants.FILE_FLAG_BACKUP_SEMANTICS;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_OPEN_REPARSE_POINT;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_DELETE;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_READ;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_WRITE;
import static sun.nio.fs.WindowsConstants.GENERIC_READ;
import static sun.nio.fs.WindowsConstants.OPEN_EXISTING;
import static sun.nio.fs.WindowsNativeDispatcher.CloseHandle;
import static sun.nio.fs.WindowsNativeDispatcher.CreateFile;
import static sun.nio.fs.WindowsNativeDispatcher.DeleteFile;
import static sun.nio.fs.WindowsNativeDispatcher.FindClose;
import static sun.nio.fs.WindowsNativeDispatcher.FindFirstStream;
import static sun.nio.fs.WindowsNativeDispatcher.FindNextStream;
import static sun.nio.fs.WindowsNativeDispatcher.FirstStream;

/**
 * Windows emulation of NamedAttributeView using Alternative Data Streams
 */
// windows平台上实现的"user"文件属性视图，实现原理是通过"备用数据流"来存储这些自定义属性
class WindowsUserDefinedFileAttributeView extends AbstractUserDefinedFileAttributeView {
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    private final WindowsPath file;     // 当前"user"文件属性视图依托的文件
    
    private final boolean followLinks;  // 指示对于符号链接，是否将其链接到目标文件
    
    
    WindowsUserDefinedFileAttributeView(WindowsPath file, boolean followLinks) {
        this.file = file;
        this.followLinks = followLinks;
    }
    
    // 返回当前"user"文件属性视图下的所有属性
    @Override
    public List<String> list() throws IOException {
        if(System.getSecurityManager() != null) {
            checkAccess(file.getPathForPermissionCheck(), true, false);
        }
        
        return listUsingStreamEnumeration();
    }
    
    // 返回当前"user"文件属性视图下名为name的属性的值的尺寸
    @Override
    public int size(String name) throws IOException {
        if(System.getSecurityManager() != null) {
            checkAccess(file.getPathForPermissionCheck(), true, false);
        }
        
        // wrap with channel
        FileChannel fileChannel = null;
        try {
            Set<OpenOption> opts = new HashSet<>();
            opts.add(StandardOpenOption.READ);
            if(!followLinks) {
                opts.add(WindowsChannelFactory.OPEN_REPARSE_POINT);
            }
            
            // 用":"拼接字符串，返回"file:name"；file需要先被解析为windows系统上的绝对路径
            String path = join(file, name);
            
            // 创建/打开一个文件，并返回其关联的非异步文件通道
            fileChannel = WindowsChannelFactory.newFileChannel(path, null, opts, 0L);
        } catch(WindowsException x) {
            x.rethrowAsIOException(join(file.getPathForPermissionCheck(), name));
        }
        
        try {
            // 返回此通道(文件)的字节数量
            long size = fileChannel.size();
            if(size>Integer.MAX_VALUE) {
                throw new ArithmeticException("Stream too large");
            }
            return (int) size;
        } finally {
            fileChannel.close();
        }
    }
    
    /*
     * 向当前"user"文件属性视图中写入一条名称为name属性，写入的属性值为src。
     *
     * 示例：
     * UserDefinedFileAttributeView view = FIles.getFileAttributeView(path, UserDefinedFileAttributeView.class);
     * String name = "user.mimetype";
     * view.write(name, Charset.defaultCharset().encode("text/html"));
     */
    @Override
    public int write(String name, ByteBuffer src) throws IOException {
        if(System.getSecurityManager() != null) {
            checkAccess(file.getPathForPermissionCheck(), false, true);
        }
        
        /*
         * Creating a named stream will cause the unnamed stream to be created if it doesn't already exist.
         * To avoid this we open the unnamed stream for reading and hope it isn't deleted/moved while we create or replace the named stream.
         * Opening the file without sharing options may cause sharing violations with other programs that are accessing the unnamed stream.
         */
        long handle = -1L;
        try {
            int flags = FILE_FLAG_BACKUP_SEMANTICS;
            if(!followLinks) {
                flags |= FILE_FLAG_OPEN_REPARSE_POINT;
            }
            
            // 打开已存在的目标文件，并返回其句柄
            handle = CreateFile(file.getPathForWin32Calls(), GENERIC_READ, (FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE), OPEN_EXISTING, flags);
        } catch(WindowsException x) {
            x.rethrowAsIOException(file);
        }
        
        try {
            Set<OpenOption> opts = new HashSet<>();
            if(!followLinks) {
                opts.add(WindowsChannelFactory.OPEN_REPARSE_POINT);
            }
            opts.add(StandardOpenOption.CREATE);
            opts.add(StandardOpenOption.WRITE);
            opts.add(StandardOpenOption.TRUNCATE_EXISTING);
            
            FileChannel fileChannel = null;
            try {
                // 用":"拼接字符串，返回"file:name"；file需要先被解析为windows系统上的绝对路径
                String path = join(file, name);
                
                // 创建/打开一个文件，并返回其关联的非异步文件通道
                fileChannel = WindowsChannelFactory.newFileChannel(path, null, opts, 0L);
            } catch(WindowsException x) {
                x.rethrowAsIOException(join(file.getPathForPermissionCheck(), name));
            }
            
            // write value (nothing we can do if I/O error occurs)
            try {
                // 记录缓冲区长度（还剩多少元素可读）
                int rem = src.remaining();
    
                // 如果缓冲区还有剩余未读完数据
                while(src.hasRemaining()) {
                    // 从缓冲区src读取，读到的内容向fileChannel中追加写入后，返回写入的字节数量
                    fileChannel.write(src);
                }
    
                // 返回写入的字节数
                return rem;
            } finally {
                // 关闭通道
                fileChannel.close();
            }
        } finally {
            // 关闭一个打开的句柄
            CloseHandle(handle);
        }
    }
    
    /*
     * 从当前"user"文件属性视图中读取一条名称为name属性，读取的属性值存入dst中
     *
     * 示例：
     * UserDefinedFileAttributeView view = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
     * String name = "user.mimetype";
     * ByteBuffer dst = ByteBuffer.allocate(view.size(name));
     * view.read(name, dst);
     * dst.flip();
     * String value = Charset.defaultCharset().decode(dst).toString();  // 将读到的属性值转换为字符串
     */
    @Override
    public int read(String name, ByteBuffer dst) throws IOException {
        if(System.getSecurityManager() != null) {
            checkAccess(file.getPathForPermissionCheck(), true, false);
        }
        
        // wrap with channel
        FileChannel fileChannel = null;
        try {
            Set<OpenOption> opts = new HashSet<>();
            opts.add(StandardOpenOption.READ);
            if(!followLinks) {
                opts.add(WindowsChannelFactory.OPEN_REPARSE_POINT);
            }
            
            // 用":"拼接字符串，返回"file:name"；file需要先被解析为windows系统上的绝对路径
            String path = join(file, name);
            
            // 创建/打开一个文件，并返回其关联的非异步文件通道
            fileChannel = WindowsChannelFactory.newFileChannel(path, null, opts, 0L);
        } catch(WindowsException x) {
            x.rethrowAsIOException(join(file.getPathForPermissionCheck(), name));
        }
        
        // read to EOF (nothing we can do if I/O error occurs)
        try {
            if(fileChannel.size()>dst.remaining()) {
                throw new IOException("Stream too large");
            }
            
            int total = 0;
            
            // 如果缓冲区还有剩余未写完空间
            while(dst.hasRemaining()) {
                // 从fileChannel中起始处读取，读到的内容存入dst后，返回读到的字节数量
                int n = fileChannel.read(dst);
                if(n<0) {
                    break;
                }
                total += n;
            }
            
            // 返回总计读到的字节数
            return total;
        } finally {
            fileChannel.close();
        }
    }
    
    // 从当前"user"文件属性视图中删除一条名称为name属性
    @Override
    public void delete(String name) throws IOException {
        if(System.getSecurityManager() != null) {
            checkAccess(file.getPathForPermissionCheck(), false, true);
        }
        
        // 返回file处文件的最终路径
        String path = WindowsLinkSupport.getFinalPath(file, followLinks);
        // 获取待删除的属性名
        String toDelete = join(path, name);
        try {
            // 删除属性
            DeleteFile(toDelete);
        } catch(WindowsException x) {
            x.rethrowAsIOException(toDelete);
        }
    }
    
    /** syntax to address named streams */
    // 用":"拼接字符串，返回"file:name"
    private String join(String file, String name) {
        if(name == null) {
            throw new NullPointerException("'name' is null");
        }
        return file + ":" + name;
    }
    
    // 用":"拼接字符串，返回"file:name"；file需要先被解析为windows系统上的绝对路径
    private String join(WindowsPath file, String name) throws WindowsException {
        // 解析file为适用windows系统的绝对路径
        String path = file.getPathForWin32Calls();
        
        // 用":"拼接字符串，返回"path:name"
        return join(path, name);
    }
    
    /** enumerates the file streams using FindFirstStream/FindNextStream APIs */
    // 返回当前"user"文件属性视图下的所有属性
    private List<String> listUsingStreamEnumeration() throws IOException {
        List<String> list = new ArrayList<>();
        
        try {
            // 获取到file上首个备用数据流(对于目录，返回null)
            FirstStream first = FindFirstStream(file.getPathForWin32Calls());
            
            if(first != null) {
                long handle = first.handle();
                try {
                    /* first stream is always ::$DATA for files */
                    // 对于文件，其首个备用数据流的名称始终为::$DATA，即指向自身
                    String name = first.name();
                    
                    // 如果是表示文件自身的数据流，直接跳过；否则，记录下属性值
                    if(!name.equals("::$DATA")) {
                        String[] segs = name.split(":");
                        list.add(segs[1]);
                    }
                    
                    // 继续获取后续的备用数据流，从中解析出属性值
                    while((name = FindNextStream(handle)) != null) {
                        String[] segs = name.split(":");
                        list.add(segs[1]);
                    }
                } finally {
                    FindClose(handle);
                }
            }
        } catch(WindowsException x) {
            x.rethrowAsIOException(file);
        }
        
        return Collections.unmodifiableList(list);
    }
    
}
