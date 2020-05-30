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

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.UserDefinedFileAttributeView;

import static sun.nio.fs.WindowsConstants.DRIVE_CDROM;
import static sun.nio.fs.WindowsConstants.DRIVE_REMOVABLE;
import static sun.nio.fs.WindowsConstants.ERROR_DIR_NOT_ROOT;
import static sun.nio.fs.WindowsConstants.ERROR_NOT_READY;
import static sun.nio.fs.WindowsConstants.FILE_NAMED_STREAMS;
import static sun.nio.fs.WindowsConstants.FILE_PERSISTENT_ACLS;
import static sun.nio.fs.WindowsConstants.FILE_READ_ONLY_VOLUME;
import static sun.nio.fs.WindowsNativeDispatcher.DiskFreeSpace;
import static sun.nio.fs.WindowsNativeDispatcher.GetDiskFreeSpace;
import static sun.nio.fs.WindowsNativeDispatcher.GetDiskFreeSpaceEx;
import static sun.nio.fs.WindowsNativeDispatcher.GetDriveType;
import static sun.nio.fs.WindowsNativeDispatcher.GetVolumeInformation;
import static sun.nio.fs.WindowsNativeDispatcher.GetVolumePathName;
import static sun.nio.fs.WindowsNativeDispatcher.VolumeInformation;

/**
 * Windows implementation of FileStore.
 */
// windows文件存储
class WindowsFileStore extends FileStore {
    
    private final VolumeInformation volInfo;    // 卷信息
    
    private final int volType;                  // 磁盘类型，参见WindowsConstants->drive types
    
    private final String root;                  // 盘符
    
    private final String displayName;           // 磁盘名称
    
    private WindowsFileStore(String root) throws WindowsException {
        assert root.charAt(root.length() - 1) == '\\';
        
        this.root = root;
        
        // 获取指定盘符的磁盘卷信息
        this.volInfo = GetVolumeInformation(root);
        
        // 获取指定盘符的磁盘类型
        this.volType = GetDriveType(root);
        
        // file store "display name" is the volume name if available
        String vol = volInfo.volumeName();
        
        if(vol.length()>0) {
            this.displayName = vol;
        } else {
            // TBD - should we map all types? Does this need to be localized?
            this.displayName = (volType == DRIVE_REMOVABLE) ? "Removable Disk" : "";
        }
    }
    
    // 返回文件存储的名称（通常是显式设置的名称）
    @Override
    public String name() {
        return volInfo.volumeName();        // "SYSTEM", "DVD-RW", ...
    }
    
    // 返回文件存储的类型
    @Override
    public String type() {
        return volInfo.fileSystemName();    // "FAT", "NTFS", ...
    }
    
    // 判断当前文件存储是否只读
    @Override
    public boolean isReadOnly() {
        return ((volInfo.flags() & FILE_READ_ONLY_VOLUME) != 0);
    }
    
    // 返回当前存储器的总空间(字节)
    @Override
    public long getTotalSpace() throws IOException {
        return readDiskFreeSpaceEx().totalNumberOfBytes();
    }
    
    // 返回当前存储器的可用空间(字节)
    @Override
    public long getUsableSpace() throws IOException {
        return readDiskFreeSpaceEx().freeBytesAvailable();
    }
    
    // 返回当前存储器的未使用空间(字节)
    @Override
    public long getUnallocatedSpace() throws IOException {
        return readDiskFreeSpaceEx().freeBytesAvailable();
    }
    
    // 返回当前文件存储中每个块的字节数（块是存储的最小单位，如扇区）
    public long getBlockSize() throws IOException {
        return readDiskFreeSpace().bytesPerSector();
    }
    
    // 返回当前文件存储中指定名称的属性值
    @Override
    public Object getAttribute(String attribute) throws IOException {
        // standard
        if(attribute.equals("totalSpace")) {
            return getTotalSpace();
        }
        
        if(attribute.equals("usableSpace")) {
            return getUsableSpace();
        }
        
        if(attribute.equals("unallocatedSpace")) {
            return getUnallocatedSpace();
        }
        
        if(attribute.equals("bytesPerSector")) {
            return getBlockSize();
        }
        
        // windows specific for testing purposes
        if(attribute.equals("volume:vsn")) {
            return volInfo.volumeSerialNumber();
        }
        
        if(attribute.equals("volume:isRemovable")) {
            return volType == DRIVE_REMOVABLE;
        }
        
        if(attribute.equals("volume:isCdrom")) {
            return volType == DRIVE_CDROM;
        }
        
        throw new UnsupportedOperationException("'" + attribute + "' not recognized");
    }
    
    // 判断当前文件存储是否支持指定类型的文件属性视图
    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        if(type == null) {
            throw new NullPointerException();
        }
        
        if(type == BasicFileAttributeView.class || type == DosFileAttributeView.class) {
            return true;
        }
        
        if(type == AclFileAttributeView.class || type == FileOwnerAttributeView.class) {
            return ((volInfo.flags() & FILE_PERSISTENT_ACLS) != 0);
        }
        
        if(type == UserDefinedFileAttributeView.class) {
            return ((volInfo.flags() & FILE_NAMED_STREAMS) != 0);
        }
        
        return false;
    }
    
    // 判断当前文件存储是否支持指定类型的文件属性视图
    @Override
    public boolean supportsFileAttributeView(String name) {
        if(name.equals("basic") || name.equals("dos")) {
            return true;
        }
        
        if(name.equals("acl")) {
            return supportsFileAttributeView(AclFileAttributeView.class);
        }
        
        if(name.equals("owner")) {
            return supportsFileAttributeView(FileOwnerAttributeView.class);
        }
        
        if(name.equals("user")) {
            return supportsFileAttributeView(UserDefinedFileAttributeView.class);
        }
        
        return false;
    }
    
    // 返回windows文件系统下指定类型的文件存储属性视图；目前，JDK对该方法的实现总是返回null
    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        if(type == null) {
            throw new NullPointerException();
        }
        
        return null;
    }
    
    // 为root文件创建相应的文件存储对象；ignoreNotReady指示在设备未准备好的情形下是否抛异常
    static WindowsFileStore create(String root, boolean ignoreNotReady) throws IOException {
        try {
            return new WindowsFileStore(root);
        } catch(WindowsException x) {
            if(ignoreNotReady && x.lastError() == ERROR_NOT_READY) {
                return null;
            }
            x.rethrowAsIOException(root);
            return null; // keep compiler happy
        }
    }
    
    // 为file文件创建相应的文件存储对象
    static WindowsFileStore create(WindowsPath file) throws IOException {
        try {
            /*
             * if the file is a link then GetVolumePathName returns the volume
             * that the link is on so we need to call it with the final target
             */
            // 返回file文件的最终路径(对于符号链接，需要将其链接到目标文件)
            String target = WindowsLinkSupport.getFinalPath(file, true);
            
            try {
                // 从指定的路径获取获取磁盘路径信息，并依此构造文件存储
                return createFromPath(target);
            } catch(WindowsException e) {
                if(e.lastError() != ERROR_DIR_NOT_ROOT) {
                    throw e;
                }
                
                // 获取符号链接的最终路径
                target = WindowsLinkSupport.getFinalPath(file);
                if(target == null) {
                    throw new FileSystemException(file.getPathForExceptionMessage(), null, "Couldn't resolve path");
                }
                
                // 从指定的路径获取获取磁盘路径信息，并依此构造文件存储
                return createFromPath(target);
            }
        } catch(WindowsException x) {
            x.rethrowAsIOException(file);
            return null; // keep compiler happy
        }
    }
    
    // 返回卷信息
    VolumeInformation volumeInformation() {
        return volInfo;
    }
    
    // 返回磁盘类型，参见WindowsConstants->drive types
    int volumeType() {
        return volType;
    }
    
    // 从指定的路径获取获取磁盘路径信息，并依此构造文件存储
    private static WindowsFileStore createFromPath(String target) throws WindowsException {
        // 获取磁盘路径信息
        String root = GetVolumePathName(target);
        // 构造文件存储
        return new WindowsFileStore(root);
    }
    
    // 返回存储器的容量信息
    private DiskFreeSpace readDiskFreeSpaceEx() throws IOException {
        try {
            return GetDiskFreeSpaceEx(root);
        } catch(WindowsException x) {
            x.rethrowAsIOException(root);
            return null;
        }
    }
    
    // 返回存储器的容量信息，限定在2G以内的磁盘上使用
    private DiskFreeSpace readDiskFreeSpace() throws IOException {
        try {
            return GetDiskFreeSpace(root);
        } catch(WindowsException x) {
            x.rethrowAsIOException(root);
            return null;
        }
    }
    
    
    @Override
    public boolean equals(Object ob) {
        if(ob == this)
            return true;
        if(!(ob instanceof WindowsFileStore))
            return false;
        WindowsFileStore other = (WindowsFileStore) ob;
        return root.equals(other.root);
    }
    
    @Override
    public int hashCode() {
        return root.hashCode();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(displayName);
        if(sb.length()>0) {
            sb.append(" ");
        }
        sb.append("(");
        // drop trailing slash
        sb.append(root.subSequence(0, root.length() - 1));
        sb.append(")");
        return sb.toString();
    }
    
}
