/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOError;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.NotLinkException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import jdk.internal.misc.Unsafe;

import static sun.nio.fs.WindowsConstants.ERROR_INVALID_LEVEL;
import static sun.nio.fs.WindowsConstants.ERROR_NOT_A_REPARSE_POINT;
import static sun.nio.fs.WindowsConstants.IO_REPARSE_TAG_SYMLINK;
import static sun.nio.fs.WindowsConstants.MAXIMUM_REPARSE_DATA_BUFFER_SIZE;
import static sun.nio.fs.WindowsNativeDispatcher.CloseHandle;
import static sun.nio.fs.WindowsNativeDispatcher.DeviceIoControlGetReparsePoint;
import static sun.nio.fs.WindowsNativeDispatcher.FindClose;
import static sun.nio.fs.WindowsNativeDispatcher.FindFirstFile;
import static sun.nio.fs.WindowsNativeDispatcher.FirstFile;
import static sun.nio.fs.WindowsNativeDispatcher.GetFileAttributes;
import static sun.nio.fs.WindowsNativeDispatcher.GetFinalPathNameByHandle;
import static sun.nio.fs.WindowsNativeDispatcher.GetFullPathName;

/**
 * Utility methods for symbolic link support on Windows Vista and newer.
 */
// 工具类，支持处理符号链接
class WindowsLinkSupport {
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    private WindowsLinkSupport() {
    }
    
    /**
     * Returns the target of a symbolic link
     */
    // 返回path处符号链接的路径
    static String readLink(WindowsPath path) throws IOException {
        long handle = 0L;
    
        try {
            // 打开path处的文件/目录以便访问其属性(对于符号链接，不会将其链接到目标文件)
            handle = path.openForReadAttributeAccess(false);
        } catch(WindowsException x) {
            x.rethrowAsIOException(path);
        }
    
        try {
            // 返回handle处符号链接的路径
            return readLinkImpl(handle);
        } finally {
            CloseHandle(handle);
        }
    }
    
    /**
     * Returns the final path (all symbolic links resolved) or null if this operation is not supported.
     */
    // 返回符号链接的最终路径
    static String getFinalPath(WindowsPath input) throws IOException {
        long h = 0;
    
        try {
            // 打开input标识的文件/目录以便访问其属性(对于符号链接，需要将其链接到目标文件)
            h = input.openForReadAttributeAccess(true);
        } catch(WindowsException x) {
            x.rethrowAsIOException(input);
        }
    
        try {
            // 获取符号链接的最终路径
            String path = GetFinalPathNameByHandle(h);
        
            // 剪掉一些前缀后返回
            return stripPrefix(path);
        } catch(WindowsException x) {
            // ERROR_INVALID_LEVEL is the error returned when not supported (a sym link to file on FAT32 or Samba server for example)
            if(x.lastError() != ERROR_INVALID_LEVEL) {
                x.rethrowAsIOException(input);
            }
        } finally {
            CloseHandle(h);
        }
    
        return null;
    }
    
    /**
     * Returns the final path of a given path as a String.
     * This should be used prior to calling Win32 system calls that do not follow links.
     */
    /*
     * 返回input处文件的最终路径；followLinks指示对于符号链接，是否将其链接到目标文件
     *
     * 对于符号链接来说，最终路径就是目标文件的路径；其他情形下，最终路径就是其自身。
     */
    static String getFinalPath(WindowsPath input, boolean followLinks) throws IOException {
        WindowsFileSystem fs = input.getFileSystem();
        
        try {
            /* if not following links then don't need final path */
            // 如果对于符号链接，不需要将其链接到目标文件，则不必获取最终路径
            if(!followLinks) {
                return input.getPathForWin32Calls();
            }
            
            // 返回input指示的文件/目录的属性信息(对于符号链接，不会将其链接到目标文件)
            WindowsFileAttributes attrs = WindowsFileAttributes.get(input, false);
            
            /* if file is not a sym link then don't need final path */
            // 如果attrs的宿主资源不是符号链接，则同样不必获取最终路径
            if(!attrs.isSymbolicLink()) {
                return input.getPathForWin32Calls();
            }
        } catch(WindowsException x) {
            x.rethrowAsIOException(input);
        }
        
        /* The file is a symbolic link so attempt to get the final path */
        // 获取符号链接的最终路径
        String result = getFinalPath(input);
        if(result != null) {
            return result;
        }
        
        /* 上述操作均失败后，执行回退操作 */
        
        /* Fallback: read target of link, resolve against parent, and repeat until file is not a link. */
        WindowsPath target = input;
        int linkCount = 0;
        do {
            try {
                WindowsFileAttributes attrs = WindowsFileAttributes.get(target, false);
                
                /* non a link so we are done */
                if(!attrs.isSymbolicLink()) {
                    return target.getPathForWin32Calls();
                }
            } catch(WindowsException x) {
                x.rethrowAsIOException(target);
            }
            
            // 返回target处符号链接的路径
            String symbolicLink = readLink(target);
            
            // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
            WindowsPath link = WindowsPath.createFromNormalizedPath(fs, symbolicLink);
            WindowsPath parent = target.getParent();
            if(parent == null) {
                /* no parent so use parent of absolute path */
                final WindowsPath path = target;
                target = AccessController.doPrivileged(new PrivilegedAction<WindowsPath>() {
                    @Override
                    public WindowsPath run() {
                        return path.toAbsolutePath();
                    }
                });
                parent = target.getParent();
            }
            
            target = parent.resolve(link);
            
        } while(++linkCount<32);
        
        throw new FileSystemException(input.getPathForExceptionMessage(), null, "Too many links");
    }
    
    /**
     * Returns the actual path of a file, optionally resolving all symbolic links.
     */
    // 返回input处文件的真实路径，resolveLinks指示是否解析路径各组件上所有符号链接
    static String getRealPath(WindowsPath input, boolean resolveLinks) throws IOException {
        WindowsFileSystem fs = input.getFileSystem();
        
        // Start with absolute path
        String path = null;
        try {
            path = input.toAbsolutePath().toString();
        } catch(IOError x) {
            throw (IOException) (x.getCause());
        }
        
        // Collapse "." and ".."
        if(path.indexOf('.') >= 0) {
            try {
                path = GetFullPathName(path);
            } catch(WindowsException x) {
                x.rethrowAsIOException(input);
            }
        }
        
        // string builder to build up components of path
        StringBuilder sb = new StringBuilder(path.length());
        
        // Copy root component
        int start;
        char c0 = path.charAt(0);
        char c1 = path.charAt(1);
        if((c0<='z' && c0 >= 'a' || c0<='Z' && c0 >= 'A') && c1 == ':' && path.charAt(2) == '\\') {
            // Driver specifier
            sb.append(Character.toUpperCase(c0));
            sb.append(":\\");
            start = 3;
        } else if(c0 == '\\' && c1 == '\\') {
            // UNC pathname, begins with "\\\\host\\share"
            int last = path.length() - 1;
            int pos = path.indexOf('\\', 2);
            // skip both server and share names
            if(pos == -1 || (pos == last)) {
                // The UNC does not have a share name (collapsed by GetFullPathName)
                throw new FileSystemException(input.getPathForExceptionMessage(), null, "UNC has invalid share");
            }
            pos = path.indexOf('\\', pos + 1);
            if(pos<0) {
                pos = last;
                sb.append(path).append("\\");
            } else {
                sb.append(path, 0, pos + 1);
            }
            start = pos + 1;
        } else {
            throw new AssertionError("path type not recognized");
        }
        
        // if the result is only a root component then we simply check it exists
        if(start >= path.length()) {
            String result = sb.toString();
            try {
                GetFileAttributes(result);
            } catch(WindowsException x) {
                x.rethrowAsIOException(path);
            }
            return result;
        }
        
        // iterate through each component to get its actual name in the directory
        int curr = start;
        while(curr<path.length()) {
            int next = path.indexOf('\\', curr);
            int end = (next == -1) ? path.length() : next;
            String search = sb.toString() + path.substring(curr, end);
            try {
                FirstFile fileData = FindFirstFile(WindowsPath.addPrefixIfNeeded(search));
                FindClose(fileData.handle());
                
                // if a reparse point is encountered then we must return the final path.
                if(resolveLinks && WindowsFileAttributes.isReparsePoint(fileData.attributes())) {
                    String result = getFinalPath(input);
                    
                    if(result == null) {
                        // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
                        WindowsPath windowsPath = WindowsPath.createFromNormalizedPath(fs, path);
                        
                        /* Fallback to slow path, usually because there is a sym link to a file system that doesn't support sym links */
                        // 解析windowsPath中的所有符号链接，会逐个遍历path中的非根组件
                        WindowsPath resolved = resolveAllLinks(windowsPath);
                        
                        // 获取resolved处文件的真实路径，此处不会再处理符号链接，因为上面已经处理过了
                        result = getRealPath(resolved, false);
                    }
                    
                    return result;
                }
                
                // add the name to the result
                sb.append(fileData.name());
                if(next != -1) {
                    sb.append('\\');
                }
            } catch(WindowsException e) {
                e.rethrowAsIOException(path);
            }
            curr = end + 1;
        }
        
        return sb.toString();
    }
    
    /**
     * Resolve all symbolic-links in a given absolute and normalized path
     */
    // 解析path中的所有符号链接，会逐个遍历path中的非根组件
    private static WindowsPath resolveAllLinks(WindowsPath path) throws IOException {
        assert path.isAbsolute();
        
        // 获取path所属的windows文件系统
        WindowsFileSystem fs = path.getFileSystem();
        
        // iterate through each name element of the path, resolving links as we go.
        int linkCount = 0;
        int elem = 0;
        
        // 遍历路径中的非根组件
        while(elem<path.getNameCount()) {
            
            // 获取[0, elem+1)范围内的路径组件
            WindowsPath subpath = path.subpath(0, elem + 1);
            
            // 基于path的根组件解析subpath路径；由于subpath是相对路径，故返回"根组件+subpath"
            WindowsPath current = path.getRoot().resolve(subpath);
            
            WindowsFileAttributes attrs = null;
            try {
                // 返回current处文件/目录的属性信息，对于符号链接，不会将其链接到目标文件
                attrs = WindowsFileAttributes.get(current, false);
            } catch(WindowsException x) {
                x.rethrowAsIOException(current);
            }
            
            // 如果current不是符号链接，直接略过
            if(!attrs.isSymbolicLink()) {
                elem++; // 前进到下一个组件
                
                // 遇到了符号链接
            } else {
                /*
                 * If a symbolic link then we resolve it against the parent of the current name element.
                 * We then resolve any remaining part of the path against the result.
                 * The target of the link may have "." and ".." components so re-normalize and restart the process from the first element.
                 */
                
                linkCount++;
                if(linkCount>32) {
                    throw new IOException("Too many links");
                }
                
                // 获取current处符号链接的路径
                String symbolicLink = readLink(current);
                
                // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
                WindowsPath target = WindowsPath.createFromNormalizedPath(fs, symbolicLink);
                WindowsPath remainder = null;
                
                int count = path.getNameCount();
                if((elem + 1)<count) {
                    // 获取[elem+1, count)范围内的路径组件
                    remainder = path.subpath(elem + 1, count);
                }
                
                path = current.getParent().resolve(target);
                
                try {
                    String full = GetFullPathName(path.toString());
                    if(!full.equals(path.toString())) {
                        // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
                        path = WindowsPath.createFromNormalizedPath(fs, full);
                    }
                } catch(WindowsException x) {
                    x.rethrowAsIOException(path);
                }
                
                // 拼接上剩余的的路径组件
                if(remainder != null) {
                    path = path.resolve(remainder);
                }
                
                // reset
                elem = 0;
            }
        }
        
        return path;
    }
    
    /**
     * Returns target of a symbolic link given the handle of an open file (that should be a link).
     */
    // 读取handle处符号链接的路径
    private static String readLinkImpl(long handle) throws IOException {
        int size = MAXIMUM_REPARSE_DATA_BUFFER_SIZE;
        
        NativeBuffer buffer = NativeBuffers.getNativeBuffer(size);
        
        try {
            try {
                DeviceIoControlGetReparsePoint(handle, buffer.address(), size);
            } catch(WindowsException x) {
                // FIXME: exception doesn't have file name
                if(x.lastError() == ERROR_NOT_A_REPARSE_POINT) {
                    throw new NotLinkException(null, null, x.errorString());
                }
                
                x.rethrowAsIOException((String) null);
            }
            
            /*
             * typedef struct _REPARSE_DATA_BUFFER {
             *     ULONG  ReparseTag;
             *     USHORT  ReparseDataLength;
             *     USHORT  Reserved;
             *     union {
             *         struct {
             *             USHORT  SubstituteNameOffset;
             *             USHORT  SubstituteNameLength;
             *             USHORT  PrintNameOffset;
             *             USHORT  PrintNameLength;
             *             WCHAR  PathBuffer[1];
             *         } SymbolicLinkReparseBuffer;
             *         struct {
             *             USHORT  SubstituteNameOffset;
             *             USHORT  SubstituteNameLength;
             *             USHORT  PrintNameOffset;
             *             USHORT  PrintNameLength;
             *             WCHAR  PathBuffer[1];
             *         } MountPointReparseBuffer;
             *         struct {
             *             UCHAR  DataBuffer[1];
             *         } GenericReparseBuffer;
             *     };
             * } REPARSE_DATA_BUFFER
             */
            final short OFFSETOF_REPARSETAG = 0;
            final short OFFSETOF_PATHOFFSET = 8;
            final short OFFSETOF_PATHLENGTH = 10;
            final short OFFSETOF_PATHBUFFER = 16 + 4;   // check this
            
            int tag = (int) unsafe.getLong(buffer.address() + OFFSETOF_REPARSETAG);
            if(tag != IO_REPARSE_TAG_SYMLINK) {
                // FIXME: exception doesn't have file name
                throw new NotLinkException(null, null, "Reparse point is not a symbolic link");
            }
            
            // get offset and length of target
            short nameOffset = unsafe.getShort(buffer.address() + OFFSETOF_PATHOFFSET);
            short nameLengthInBytes = unsafe.getShort(buffer.address() + OFFSETOF_PATHLENGTH);
            if((nameLengthInBytes % 2) != 0) {
                throw new FileSystemException(null, null, "Symbolic link corrupted");
            }
            
            // copy into char array
            char[] name = new char[nameLengthInBytes / 2];
            unsafe.copyMemory(null, buffer.address() + OFFSETOF_PATHBUFFER + nameOffset, name, Unsafe.ARRAY_CHAR_BASE_OFFSET, nameLengthInBytes);
            
            // remove special prefix
            String target = stripPrefix(new String(name));
            if(target.length() == 0) {
                throw new IOException("Symbolic link target is invalid");
            }
            
            return target;
        } finally {
            buffer.release();
        }
    }
    
    /**
     * Strip long path or symbolic link prefix from path
     */
    // 从路径中删除长路径或符号链接前缀
    private static String stripPrefix(String path) {
        // prefix for resolved/long path
        if(path.startsWith("\\\\?\\")) {
            if(path.startsWith("\\\\?\\UNC\\")) {
                path = "\\" + path.substring(7);
            } else {
                path = path.substring(4);
            }
        
            return path;
        }
    
        // prefix for target of symbolic link
        if(path.startsWith("\\??\\")) {
            if(path.startsWith("\\??\\UNC\\")) {
                path = "\\" + path.substring(7);
            } else {
                path = path.substring(4);
            }
        
            return path;
        }
    
        return path;
    }
}
