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
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Set;

import static sun.nio.fs.WindowsConstants.ERROR_INVALID_PARAMETER;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_ARCHIVE;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_HIDDEN;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_READONLY;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_SYSTEM;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_BACKUP_SEMANTICS;
import static sun.nio.fs.WindowsConstants.FILE_FLAG_OPEN_REPARSE_POINT;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_DELETE;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_READ;
import static sun.nio.fs.WindowsConstants.FILE_SHARE_WRITE;
import static sun.nio.fs.WindowsConstants.FILE_WRITE_ATTRIBUTES;
import static sun.nio.fs.WindowsConstants.OPEN_EXISTING;
import static sun.nio.fs.WindowsNativeDispatcher.CloseHandle;
import static sun.nio.fs.WindowsNativeDispatcher.CreateFile;
import static sun.nio.fs.WindowsNativeDispatcher.GetFileAttributes;
import static sun.nio.fs.WindowsNativeDispatcher.SetFileAttributes;
import static sun.nio.fs.WindowsNativeDispatcher.SetFileTime;

// windows文件属性视图工具类，可以构造"basic"文件属性视图和"dos"文件属性视图
class WindowsFileAttributeViews {
    
    // 创建指定file的"basic"文件属性视图，followLinks指示对于符号链接，是否将其链接到目标文件
    static Basic createBasicView(WindowsPath file, boolean followLinks) {
        return new Basic(file, followLinks);
    }
    
    // 创建指定file的"dos"文件属性视图，followLinks指示对于符号链接，是否将其链接到目标文件
    static Dos createDosView(WindowsPath file, boolean followLinks) {
        return new Dos(file, followLinks);
    }
    
    
    // windows平台上实现的"basic"文件属性视图
    private static class Basic extends AbstractBasicFileAttributeView {
        final WindowsPath file;     // "basic"文件属性视图依托的文件
        final boolean followLinks;  // 对于符号链接，是否将其链接到目标文件
        
        Basic(WindowsPath file, boolean followLinks) {
            this.file = file;
            this.followLinks = followLinks;
        }
        
        // 返回"basic"文件属性视图
        @Override
        public WindowsFileAttributes readAttributes() throws IOException {
            file.checkRead();
            
            try {
                // 返回file标记的文件/目录的属性信息，followLinks指示对于符号链接，是否将其链接到目标文件
                return WindowsFileAttributes.get(file, followLinks);
            } catch(WindowsException x) {
                x.rethrowAsIOException(file);
                return null;    // keep compiler happy
            }
        }
        
        // 更新文件的"最后修改时间"/"最后访问时间"/"创建时间"这几个属性中的部分或全部，依实现而定
        @Override
        public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
            // if all null then do nothing
            if(lastModifiedTime == null && lastAccessTime == null && createTime == null) {
                // no effect
                return;
            }
            
            // permission check
            file.checkWrite();
            
            // update times
            long t1 = (createTime == null) ? -1L : WindowsFileAttributes.toWindowsTime(createTime);
            long t2 = (lastAccessTime == null) ? -1L : WindowsFileAttributes.toWindowsTime(lastAccessTime);
            long t3 = (lastModifiedTime == null) ? -1L : WindowsFileAttributes.toWindowsTime(lastModifiedTime);
            
            setFileTimes(t1, t2, t3);
        }
        
        /**
         * Parameter values in Windows times.
         */
        // 更新文件的"最后修改时间"/"最后访问时间"/"创建时间"这几个属性中的部分或全部，依实现而定
        void setFileTimes(long createTime, long lastAccessTime, long lastWriteTime) throws IOException {
            long handle = -1L;
            try {
                int flags = FILE_FLAG_BACKUP_SEMANTICS;
                if(!followLinks) {
                    flags |= FILE_FLAG_OPEN_REPARSE_POINT;
                }
                
                handle = CreateFile(file.getPathForWin32Calls(), FILE_WRITE_ATTRIBUTES, (FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE), OPEN_EXISTING, flags);
            } catch(WindowsException x) {
                x.rethrowAsIOException(file);
            }
            
            // update times
            try {
                SetFileTime(handle, createTime, lastAccessTime, lastWriteTime);
            } catch(WindowsException x) {
                // If ERROR_INVALID_PARAMETER is returned and the volume is FAT then adjust to the FAT epoch and retry.
                if(followLinks && x.lastError() == ERROR_INVALID_PARAMETER) {
                    try {
                        if(WindowsFileStore.create(file).type().equals("FAT")) {
                            SetFileTime(handle, adjustForFatEpoch(createTime), adjustForFatEpoch(lastAccessTime), adjustForFatEpoch(lastWriteTime));
                            // retry succeeded
                            x = null;
                        }
                    } catch(SecurityException | WindowsException | IOException ignore) {
                        // ignore exceptions to let original exception be thrown
                    }
                    
                }
                
                if(x != null) {
                    x.rethrowAsIOException(file);
                }
            } finally {
                CloseHandle(handle);
            }
        }
        
        /**
         * Adjusts a Windows time for the FAT epoch.
         */
        private long adjustForFatEpoch(long time) {
            // 1/1/1980 in Windows Time
            final long FAT_EPOCH = 119600064000000000L;
            
            if(time != -1L && time<FAT_EPOCH) {
                return FAT_EPOCH;
            } else {
                return time;
            }
        }
    }
    
    // windows平台上实现的"dos"文件属性视图
    static class Dos extends Basic implements DosFileAttributeView {
        
        private static final String ATTRIBUTES_NAME = "attributes"; // 文件属性，可选常量参见https://docs.microsoft.com/zh-cn/windows/win32/fileio/file-attribute-constants
        private static final String READONLY_NAME = "readonly";   // 是否为只读文件
        private static final String ARCHIVE_NAME = "archive";    // 是否为待归档文件
        private static final String SYSTEM_NAME = "system";     // 是否为系统文件
        private static final String HIDDEN_NAME = "hidden";     // 是否为隐藏文件
        
        /** the names of the DOS attributes (includes basic) */
        // 当前属性视图中所有允许出现的属性名
        static final Set<String> dosAttributeNames = Util.newSet(basicAttributeNames, READONLY_NAME, ARCHIVE_NAME, SYSTEM_NAME, HIDDEN_NAME, ATTRIBUTES_NAME);
        
        Dos(WindowsPath file, boolean followLinks) {
            super(file, followLinks);
        }
        
        // 返回当前属性视图的名称："dos"
        @Override
        public String name() {
            return "dos";
        }
        
        /*
         * 向当前"dos"文件属性视图中设置attName属性，设置的属性值为value，可设置的属性包括：
         * -  creationTime    : 创建时间，其值是FileTime类型的对象
         * -  lastAccessTime  : 最后访问时间，其值是FileTime类型的对象
         * -  lastModifiedTime: 最后修改时间，其值是FileTime类型的对象
         * -- readonly        : 是否为只读文件，其值是布尔类型的值
         * -- archive         : 是否为待归档文件，其值是布尔类型的值
         * -- system          : 是否为系统文件，其值是布尔类型的值
         * -- hidden          : 是否为隐藏文件，其值是布尔类型的值
         *
         * 注：这里的属性可以是"basic"文件属性视图，因为"dos"继承了"basic"
         */
        @Override
        public void setAttribute(String attName, Object value) throws IOException {
            
            // 设置"只读"文件属性
            if(attName.equals(READONLY_NAME)) {
                setReadOnly((Boolean) value);
                return;
            }
            
            // 设置"已存档"文件属性
            if(attName.equals(ARCHIVE_NAME)) {
                setArchive((Boolean) value);
                return;
            }
            
            // 设置"系统"文件属性
            if(attName.equals(SYSTEM_NAME)) {
                setSystem((Boolean) value);
                return;
            }
            
            // 设置"隐藏"文件属性
            if(attName.equals(HIDDEN_NAME)) {
                setHidden((Boolean) value);
                return;
            }
            
            super.setAttribute(attName, value);
        }
        
        /*
         * 从当前"dos"文件属性视图中获取一批属性的值；这批属性的名称由attNames给出，获取到的属性以<属性名, 属性值>的形式返回，可获取的属性包括：
         * -  creationTime    : 创建时间，其值是FileTime类型的对象
         * -  lastAccessTime  : 最后访问时间，其值是FileTime类型的对象
         * -  lastModifiedTime: 最后修改时间，其值是FileTime类型的对象
         * -- readonly        : 是否为只读文件，其值是布尔类型的值
         * -- archive         : 是否为待归档文件，其值是布尔类型的值
         * -- system          : 是否为系统文件，其值是布尔类型的值
         * -- hidden          : 是否为隐藏文件，其值是布尔类型的值
         *
         * 注：这里的属性可以是"basic"文件属性视图，因为"dos"继承了"basic"
         */
        @Override
        public Map<String, Object> readAttributes(String[] attNames) throws IOException {
            
            // 创建一个属性构建器，dosAttributeNames提供了所有允许出现的属性名
            AttributesBuilder builder = AttributesBuilder.create(dosAttributeNames, attNames);
            
            // 获取"dos"文件属性视图
            WindowsFileAttributes attrs = readAttributes();
            
            // 构造"dos"文件属性映射
            addRequestedBasicAttributes(attrs, builder);
            
            
            /* 下面处理非"basic"文件属性 */
            
            if(builder.match(READONLY_NAME)) {
                builder.add(READONLY_NAME, attrs.isReadOnly());
            }
            
            if(builder.match(ARCHIVE_NAME)) {
                builder.add(ARCHIVE_NAME, attrs.isArchive());
            }
            
            if(builder.match(SYSTEM_NAME)) {
                builder.add(SYSTEM_NAME, attrs.isSystem());
            }
            
            if(builder.match(HIDDEN_NAME)) {
                builder.add(HIDDEN_NAME, attrs.isHidden());
            }
            
            if(builder.match(ATTRIBUTES_NAME)) {
                builder.add(ATTRIBUTES_NAME, attrs.attributes());
            }
            
            // 以只读Map的形式返回"dos"文件属性映射
            return builder.unmodifiableMap();
        }
        
        // 设置"只读"文件属性
        @Override
        public void setReadOnly(boolean value) throws IOException {
            updateAttributes(FILE_ATTRIBUTE_READONLY, value);
        }
        
        // 设置"隐藏"文件属性
        @Override
        public void setHidden(boolean value) throws IOException {
            updateAttributes(FILE_ATTRIBUTE_HIDDEN, value);
        }
        
        // 设置"系统"文件属性
        @Override
        public void setSystem(boolean value) throws IOException {
            updateAttributes(FILE_ATTRIBUTE_SYSTEM, value);
        }
        
        // 设置"已存档"文件属性
        @Override
        public void setArchive(boolean value) throws IOException {
            updateAttributes(FILE_ATTRIBUTE_ARCHIVE, value);
        }
        
        /** Copy given attributes to the file */
        // 从attrs解析出"dos"文件属性设置到当前属性视图中
        void setAttributes(WindowsFileAttributes attrs) throws IOException {
            // copy DOS attributes to target
            int flags = 0;
            
            if(attrs.isReadOnly()) {
                flags |= FILE_ATTRIBUTE_READONLY;
            }
            
            if(attrs.isHidden()) {
                flags |= FILE_ATTRIBUTE_HIDDEN;
            }
            
            if(attrs.isArchive()) {
                flags |= FILE_ATTRIBUTE_ARCHIVE;
            }
            
            if(attrs.isSystem()) {
                flags |= FILE_ATTRIBUTE_SYSTEM;
            }
            
            updateAttributes(flags, true);
            
            // copy file times to target - must be done after updating FAT attributes as otherwise the last modified time may be wrong.
            setFileTimes(WindowsFileAttributes.toWindowsTime(attrs.creationTime()), WindowsFileAttributes.toWindowsTime(attrs.lastModifiedTime()), WindowsFileAttributes.toWindowsTime(attrs.lastAccessTime()));
        }
        
        /**
         * Update DOS attributes
         */
        // 更新"dos"文件属性
        private void updateAttributes(int flag, boolean enable) throws IOException {
            file.checkWrite();
            
            // GetFileAttributes & SetFileAttributes do not follow links so when following links we need the final target
            String path = WindowsLinkSupport.getFinalPath(file, followLinks);
            
            try {
                int oldValue = GetFileAttributes(path);
                int newValue = oldValue;
                if(enable) {
                    newValue |= flag;
                } else {
                    newValue &= ~flag;
                }
                if(newValue != oldValue) {
                    SetFileAttributes(path, newValue);
                }
            } catch(WindowsException x) {
                // don't reveal target in exception
                x.rethrowAsIOException(file);
            }
        }
    }
    
}
