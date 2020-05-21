/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.*;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

/**
 * Utility methods for copying and moving files.
 */
// 文件复制/剪切的工具类
class WindowsFileCopy {
    
    private WindowsFileCopy() {
    }
    
    /**
     * Copy file from source to target
     */
    /*
     * 将source处的文件/目录复制到target处，默认不允许覆盖，
     * 如果源目录不为空，则不会复制其子项，
     * 复制成功后，复制源依然保留。
     */
    static void copy(final WindowsPath source, final WindowsPath target, CopyOption... options) throws IOException {
        // map options
        boolean replaceExisting = false;    // 是否允许覆盖文件
        boolean copyAttributes = false;    // 是否需要复制安全属性信息
        boolean followLinks = true;     // 对于符号链接，是否将其链接到目标文件
        boolean interruptible = false;    // 是否响应线程中断
        
        // 解析复制参数
        for(CopyOption option : options) {
            if(option == StandardCopyOption.REPLACE_EXISTING) {
                replaceExisting = true;
                continue;
            }
            
            if(option == StandardCopyOption.COPY_ATTRIBUTES) {
                copyAttributes = true;
                continue;
            }
            
            if(option == LinkOption.NOFOLLOW_LINKS) {
                followLinks = false;
                continue;
            }
            
            if(ExtendedOptions.INTERRUPTIBLE.matches(option)) {
                interruptible = true;
                continue;
            }
            
            if(option == null) {
                throw new NullPointerException();
            }
            
            throw new UnsupportedOperationException("Unsupported copy option");
        }
        
        // check permissions. If the source file is a symbolic link then later we must also check LinkPermission
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            source.checkRead();
            target.checkWrite();
        }
        
        // get attributes of source file
        // attempt to get attributes of target file
        // if both files are the same there is nothing to do
        // if target exists and !replace then throw exception
        
        WindowsFileAttributes sourceAttrs = null;
        WindowsFileAttributes targetAttrs = null;
        
        long sourceHandle = 0L;
        try {
            // 打开路径source标识的文件/目录以便访问其属性
            sourceHandle = source.openForReadAttributeAccess(followLinks);
        } catch(WindowsException x) {
            x.rethrowAsIOException(source);
        }
        
        try {
            // source attributes
            try {
                // 获取sourceHandle文件的windows文件属性信息，用于BY_HANDLE_FILE_INFORMATION结构体
                sourceAttrs = WindowsFileAttributes.readAttributes(sourceHandle);
            } catch(WindowsException x) {
                x.rethrowAsIOException(source);
            }
            
            // open target (don't follow links)
            long targetHandle = 0L;
            try {
                // 打开路径target标识的文件/目录以便访问其属性(对于符号链接，不需要将其链接到目标文件)
                targetHandle = target.openForReadAttributeAccess(false);
                try {
                    // 获取targetHandle文件的windows文件属性信息，用于BY_HANDLE_FILE_INFORMATION结构体
                    targetAttrs = WindowsFileAttributes.readAttributes(targetHandle);
    
                    // 如果两个文件属性相同，则直接返回
                    if(WindowsFileAttributes.isSameFile(sourceAttrs, targetAttrs)) {
                        return;
                    }
    
                    // 如果目标文件/目录存在，但是不允许覆盖，抛异常
                    if(!replaceExisting) {
                        throw new FileAlreadyExistsException(target.getPathForExceptionMessage());
                    }
                } finally {
                    CloseHandle(targetHandle);
                }
            } catch(WindowsException x) {
                // ignore
            }
        } finally {
            CloseHandle(sourceHandle);
        }
        
        // if source file is a symbolic link then we must check for LinkPermission
        if(sm != null && sourceAttrs.isSymbolicLink()) {
            sm.checkPermission(new LinkPermission("symbolic"));
        }
        
        final String sourcePath = asWin32Path(source);
        final String targetPath = asWin32Path(target);
        
        // 如果目标文件存在，则先删除它(前面已经确认允许覆盖了)
        if(targetAttrs != null) {
            try {
                // 如果target是目录(不管是不是符号链接)
                if(targetAttrs.isDirectory() || targetAttrs.isDirectoryLink()) {
                    // 删除目录
                    RemoveDirectory(targetPath);
                } else {
                    // 删除文件
                    DeleteFile(targetPath);
                }
            } catch(WindowsException x) {
                // 如果删除的目录非空，抛异常
                if(targetAttrs.isDirectory()) {
                    // ERROR_ALREADY_EXISTS is returned when attempting to delete non-empty directory on SAMBA servers.
                    if(x.lastError() == ERROR_DIR_NOT_EMPTY || x.lastError() == ERROR_ALREADY_EXISTS) {
                        throw new DirectoryNotEmptyException(target.getPathForExceptionMessage());
                    }
                }
                x.rethrowAsIOException(target);
            }
        }
        
        // 如果source是文件(不管是不是符号链接)
        if(!sourceAttrs.isDirectory() && !sourceAttrs.isDirectoryLink()) {
            final int flags = followLinks ? 0 : COPY_FILE_COPY_SYMLINK;
            
            // 如果需要响应线程中断
            if(interruptible) {
                // 构造一个可中断的复制任务
                Cancellable copyTask = new Cancellable() {
                    @Override
                    public int cancelValue() {
                        return 1;  // TRUE
                    }
                    
                    @Override
                    public void implRun() throws IOException {
                        try {
                            // 进行复制操作
                            CopyFileEx(sourcePath, targetPath, flags, addressToPollForCancel());
                        } catch(WindowsException x) {
                            x.rethrowAsIOException(source, target);
                        }
                    }
                };
                
                try {
                    // 执行指定的(复制)任务，会阻塞发起复制操作的线程；该任务会响应线程中断
                    Cancellable.runInterruptibly(copyTask);
                } catch(ExecutionException e) {
                    Throwable t = e.getCause();
                    if(t instanceof IOException) {
                        throw (IOException) t;
                    }
                    throw new IOException(t);
                }
                
                // 如果不需要响应线程中断，则直接进行复制
            } else {
                try {
                    CopyFileEx(sourcePath, targetPath, flags, 0L);
                } catch(WindowsException x) {
                    x.rethrowAsIOException(source, target);
                }
            }
            
            // 如果不需要进行属性复制，则此处可以直接返回了
            if(!copyAttributes) {
                return;
            }
            
            // CopyFileEx does not copy security attributes
            try {
                // 复制安全属性信息
                copySecurityAttributes(source, target, followLinks);
            } catch(IOException ignored) {
            }
            
            // 如果source是目录(不管是不是符号链接)
        } else {
            try {
                // 对目录进行复制
                if(sourceAttrs.isDirectory()) {
                    CreateDirectory(targetPath, 0L);
                    
                    // 对目录的符号链接进行复制
                } else {
                    // 获取source处符号链接的路径
                    String linkTarget = WindowsLinkSupport.readLink(source);
                    // 创建目录的符号链接
                    CreateSymbolicLink(targetPath, WindowsPath.addPrefixIfNeeded(linkTarget), SYMBOLIC_LINK_FLAG_DIRECTORY);
                }
            } catch(WindowsException x) {
                x.rethrowAsIOException(target);
            }
            
            // 如果不需要进行属性复制，则此处可以直接返回了
            if(!copyAttributes) {
                return;
            }
            
            // 创建指定target的"dos"文件属性视图(对于符号链接，不会将其链接到目标文件)
            WindowsFileAttributeViews.Dos view = WindowsFileAttributeViews.createDosView(target, false);
            
            try {
                // 为target设置(复制)source的属性
                view.setAttributes(sourceAttrs);
            } catch(IOException x) {
                if(sourceAttrs.isDirectory()) {
                    try {
                        RemoveDirectory(targetPath);
                    } catch(WindowsException ignore) {
                    }
                }
            }
            
            // copy security attributes. If this fail it doesn't cause the move to fail.
            try {
                // 复制安全属性信息
                copySecurityAttributes(source, target, followLinks);
            } catch(IOException ignore) {
            }
        }
    }
    
    /**
     * Move file from source to target
     */
    /*
     * 将source处的文件/目录移动到target处，默认不允许覆盖，
     * 如果源目录不为空，且在不同的磁盘间复制，则不会复制其子项，
     * 复制成功后，复制源会被删除。
     */
    static void move(WindowsPath source, WindowsPath target, CopyOption... options) throws IOException {
        // map options
        boolean atomicMove = false;
        boolean replaceExisting = false;    // 是否允许覆盖文件
        
        // 解析剪切参数
        for(CopyOption option : options) {
            
            if(option == StandardCopyOption.REPLACE_EXISTING) {
                replaceExisting = true;
                continue;
            }
            
            if(option == StandardCopyOption.ATOMIC_MOVE) {
                atomicMove = true;
                continue;
            }
            
            if(option == LinkOption.NOFOLLOW_LINKS) {
                // ignore
                continue;
            }
            
            if(option == null) {
                throw new NullPointerException();
            }
            
            throw new UnsupportedOperationException("Unsupported copy option");
        }
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            source.checkWrite();
            target.checkWrite();
        }
        
        final String sourcePath = asWin32Path(source);
        final String targetPath = asWin32Path(target);
        
        // 原子地复制
        if(atomicMove) {
            try {
                MoveFileEx(sourcePath, targetPath, MOVEFILE_REPLACE_EXISTING);
            } catch(WindowsException x) {
                if(x.lastError() == ERROR_NOT_SAME_DEVICE) {
                    throw new AtomicMoveNotSupportedException(source.getPathForExceptionMessage(), target.getPathForExceptionMessage(), x.errorString());
                }
                x.rethrowAsIOException(source, target);
            }
            
            return;
        }
        
        // get attributes of source file
        // attempt to get attributes of target file
        // if both files are the same there is nothing to do
        // if target exists and !replace then throw exception
        
        WindowsFileAttributes sourceAttrs = null;
        WindowsFileAttributes targetAttrs = null;
        
        long sourceHandle = 0L;
        try {
            // 打开路径source标识的文件/目录以便访问其属性(对于符号链接，不需要将其链接到目标文件)
            sourceHandle = source.openForReadAttributeAccess(false);
        } catch(WindowsException x) {
            x.rethrowAsIOException(source);
        }
        
        try {
            // source attributes
            try {
                // 获取sourceHandle文件的windows文件属性信息，用于BY_HANDLE_FILE_INFORMATION结构体
                sourceAttrs = WindowsFileAttributes.readAttributes(sourceHandle);
            } catch(WindowsException x) {
                x.rethrowAsIOException(source);
            }
            
            // open target (don't follow links)
            long targetHandle = 0L;
            try {
                // 打开路径target标识的文件/目录以便访问其属性(对于符号链接，不需要将其链接到目标文件)
                targetHandle = target.openForReadAttributeAccess(false);
                try {
                    // 获取targetHandle文件的windows文件属性信息，用于BY_HANDLE_FILE_INFORMATION结构体
                    targetAttrs = WindowsFileAttributes.readAttributes(targetHandle);
    
                    // 如果两个文件属性相同，直接返回
                    if(WindowsFileAttributes.isSameFile(sourceAttrs, targetAttrs)) {
                        return;
                    }
    
                    // 如果目标文件/目录存在，但是不允许覆盖，抛异常
                    if(!replaceExisting) {
                        throw new FileAlreadyExistsException(target.getPathForExceptionMessage());
                    }
    
                } finally {
                    CloseHandle(targetHandle);
                }
            } catch(WindowsException x) {
                // ignore
            }
            
        } finally {
            CloseHandle(sourceHandle);
        }
        
        // 如果目标文件存在，则先删除它(前面已经确认允许覆盖了)
        if(targetAttrs != null) {
            try {
                // 如果target是目录(不管是不是符号链接)
                if(targetAttrs.isDirectory() || targetAttrs.isDirectoryLink()) {
                    // 删除目录
                    RemoveDirectory(targetPath);
                } else {
                    // 删除文件
                    DeleteFile(targetPath);
                }
            } catch(WindowsException x) {
                // 如果删除的目录非空，抛异常
                if(targetAttrs.isDirectory()) {
                    // ERROR_ALREADY_EXISTS is returned when attempting to delete non-empty directory on SAMBA servers.
                    if(x.lastError() == ERROR_DIR_NOT_EMPTY || x.lastError() == ERROR_ALREADY_EXISTS) {
                        throw new DirectoryNotEmptyException(target.getPathForExceptionMessage());
                    }
                }
                
                x.rethrowAsIOException(target);
            }
        }
        
        /*
         * first try MoveFileEx (no options).
         * If target is on same volume then all attributes (including security attributes) are preserved.
         */
        try {
            MoveFileEx(sourcePath, targetPath, 0);
            return;
        } catch(WindowsException x) {
            // 如果出现了跨磁盘移动的错误，则忽略该错误
            if(x.lastError() != ERROR_NOT_SAME_DEVICE) {
                x.rethrowAsIOException(source, target);
            }
        }
        
        /* target is on different volume so use MoveFileEx with copy option */
        // 如果source是文件(不管是不是符号链接)
        if(!sourceAttrs.isDirectory() && !sourceAttrs.isDirectoryLink()) {
            try {
                // 移动文件
                MoveFileEx(sourcePath, targetPath, MOVEFILE_COPY_ALLOWED);
            } catch(WindowsException x) {
                x.rethrowAsIOException(source, target);
            }
            
            // MoveFileEx does not copy security attributes when moving across volumes.
            try {
                // 复制安全属性信息
                copySecurityAttributes(source, target, false);
            } catch(IOException x) {
                // ignore
            }
            
            // 如果source是目录
        } else {
            /* moving directory or directory-link to another file system */
            // 确保source是目录(不管是不是符号链接)
            assert sourceAttrs.isDirectory() || sourceAttrs.isDirectoryLink();
            
            // create new directory or directory junction
            try {
                // 如果source是目录，此处需要确保其为空目录
                if(sourceAttrs.isDirectory()) {
                    // 确保source为空目录
                    ensureEmptyDir(source);
                    // 在目标磁盘中创建新的目录
                    CreateDirectory(targetPath, 0L);
                    
                    // 如果source是目录的符号链接
                } else {
                    // 获取source处符号链接的路径
                    String linkTarget = WindowsLinkSupport.readLink(source);
                    // 在目标磁盘中创建目录的符号链接
                    CreateSymbolicLink(targetPath, WindowsPath.addPrefixIfNeeded(linkTarget), SYMBOLIC_LINK_FLAG_DIRECTORY);
                }
            } catch(WindowsException x) {
                x.rethrowAsIOException(target);
            }
            
            /* copy timestamps/DOS attributes */
            // 创建target的"dos"文件属性视图(对于符号链接，不会链接到目标文件)
            WindowsFileAttributeViews.Dos view = WindowsFileAttributeViews.createDosView(target, false);
            try {
                // 为target设置source的"dos"属性信息
                view.setAttributes(sourceAttrs);
            } catch(IOException x) {
                // rollback
                try {
                    RemoveDirectory(targetPath);
                } catch(WindowsException ignore) {
                }
                throw x;
            }
            
            // copy security attributes. If this fails it doesn't cause the move to fail.
            try {
                // 复制安全属性信息
                copySecurityAttributes(source, target, false);
            } catch(IOException ignore) {
            }
            
            // 删除源，如果源是非空目录，则抛异常
            try {
                RemoveDirectory(sourcePath);
            } catch(WindowsException x) {
                // 如果删除失败，则将复制好的目标也删除
                try {
                    RemoveDirectory(targetPath);
                } catch(WindowsException ignore) {
                }
                
                // ERROR_ALREADY_EXISTS is returned when attempting to delete non-empty directory on SAMBA servers.
                if(x.lastError() == ERROR_DIR_NOT_EMPTY || x.lastError() == ERROR_ALREADY_EXISTS) {
                    throw new DirectoryNotEmptyException(target.getPathForExceptionMessage());
                }
                
                x.rethrowAsIOException(source);
            }
        }
    }
    
    /** throw a DirectoryNotEmpty exception if not empty */
    // 确保dir为空目录
    static void ensureEmptyDir(WindowsPath dir) throws IOException {
        try(
            WindowsDirectoryStream dirStream = new WindowsDirectoryStream(dir, (e) -> true)
        ) {
            if(dirStream.iterator().hasNext()) {
                throw new DirectoryNotEmptyException(dir.getPathForExceptionMessage());
            }
        }
    }
    
    // 将path解析为适用windows系统的绝对路径
    private static String asWin32Path(WindowsPath path) throws IOException {
        try {
            return path.getPathForWin32Calls();
        } catch(WindowsException x) {
            x.rethrowAsIOException(path);
            return null;
        }
    }
    
    /**
     * Copy DACL/owner/group from source to target
     */
    // 复制安全属性信息
    private static void copySecurityAttributes(WindowsPath source, WindowsPath target, boolean followLinks) throws IOException {
        String path = WindowsLinkSupport.getFinalPath(source, followLinks);
        
        // may need SeRestorePrivilege to set file owner
        WindowsSecurity.Privilege priv = WindowsSecurity.enablePrivilege("SeRestorePrivilege");
        try {
            int request = (DACL_SECURITY_INFORMATION | OWNER_SECURITY_INFORMATION | GROUP_SECURITY_INFORMATION);
            NativeBuffer buffer = WindowsAclFileAttributeView.getFileSecurity(path, request);
            try {
                try {
                    SetFileSecurity(target.getPathForWin32Calls(), request, buffer.address());
                } catch(WindowsException x) {
                    x.rethrowAsIOException(target);
                }
            } finally {
                buffer.release();
            }
        } finally {
            priv.drop();
        }
    }
    
}
