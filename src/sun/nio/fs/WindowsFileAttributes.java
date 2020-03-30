/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;
import jdk.internal.misc.Unsafe;
import sun.security.action.GetPropertyAction;

import static sun.nio.fs.WindowsConstants.ERROR_SHARING_VIOLATION;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_ARCHIVE;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_DEVICE;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_DIRECTORY;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_HIDDEN;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_READONLY;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_REPARSE_POINT;
import static sun.nio.fs.WindowsConstants.FILE_ATTRIBUTE_SYSTEM;
import static sun.nio.fs.WindowsConstants.IO_REPARSE_TAG_SYMLINK;
import static sun.nio.fs.WindowsConstants.MAXIMUM_REPARSE_DATA_BUFFER_SIZE;
import static sun.nio.fs.WindowsNativeDispatcher.CloseHandle;
import static sun.nio.fs.WindowsNativeDispatcher.DeviceIoControlGetReparsePoint;
import static sun.nio.fs.WindowsNativeDispatcher.FindClose;
import static sun.nio.fs.WindowsNativeDispatcher.FindFirstFile;
import static sun.nio.fs.WindowsNativeDispatcher.GetFileAttributesEx;
import static sun.nio.fs.WindowsNativeDispatcher.GetFileInformationByHandle;

/**
 * Windows implementation of DosFileAttributes/BasicFileAttributes
 */
// windows平台上的"dos"文件属性
class WindowsFileAttributes implements DosFileAttributes {
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    /*
     * 文件或目录的属性信息，GetFileAttributesEx函数的参数使用这种结构。
     *
     * // 参见https://docs.microsoft.com/en-us/previous-versions/windows/embedded/ms892377(v%3Dmsdn.10)
     * typedef struct _WIN32_FILE_ATTRIBUTE_DATA {
     *     DWORD dwFileAttributes;
     *     FILETIME ftCreationTime;
     *     FILETIME ftLastAccessTime;
     *     FILETIME ftLastWriteTime;
     *     DWORD nFileSizeHigh;
     *     DWORD nFileSizeLow;
     * } WIN32_FILE_ATTRIBUTE_DATA;
     *
     * nFileSizeHigh与nFileSizeLow涉及到文件尺寸的计算，具体参见：
     * https://stackoverflow.com/questions/15209077/finding-correct-filesize-over-4gb-in-windows
     *
     * WIN32_FILE_ATTRIBUTE_DATA结构的含义：
     */
    private static final short SIZEOF_FILE_ATTRIBUTE_DATA = 36;                     // 文件(扩展)属性结构体长度
    private static final short OFFSETOF_FILE_ATTRIBUTE_DATA_ATTRIBUTES = 0;    // 文件属性，参见https://docs.microsoft.com/zh-cn/windows/win32/fileio/file-attribute-constants
    private static final short OFFSETOF_FILE_ATTRIBUTE_DATA_CREATETIME = 4;    // 创建时间
    private static final short OFFSETOF_FILE_ATTRIBUTE_DATA_LASTACCESSTIME = 12;   // 最后访问时间
    private static final short OFFSETOF_FILE_ATTRIBUTE_DATA_LASTWRITETIME = 20;   // 最后修改时间
    private static final short OFFSETOF_FILE_ATTRIBUTE_DATA_SIZEHIGH = 28;   // 大文件尺寸的高位数据
    private static final short OFFSETOF_FILE_ATTRIBUTE_DATA_SIZELOW = 32;   // 大文件尺寸的低位数据，或为小文件尺寸
    
    /*
     * 文件信息：此结构包含由GetFileInformationByHandle函数检索的信息
     *
     * // 参见https://docs.microsoft.com/en-us/previous-versions/windows/embedded/aa516973(v=msdn.10)
     * typedef struct _BY_HANDLE_FILE_INFORMATION {
     *     DWORD    dwFileAttributes;
     *     FILETIME ftCreationTime;
     *     FILETIME ftLastAccessTime;
     *     FILETIME ftLastWriteTime;
     *     DWORD    dwVolumeSerialNumber;
     *     DWORD    nFileSizeHigh;
     *     DWORD    nFileSizeLow;
     *     DWORD    nNumberOfLinks; // 指向该文件的链接数。对于FAT文件系统，该成员始终为1。对于NTFS文件系统，它可以大于1
     *     DWORD    nFileIndexHigh;
     *     DWORD    nFileIndexLow;
     * } BY_HANDLE_FILE_INFORMATION;
     *
     * 文件标识与卷序列号唯一地标识一个文件。但注意文件标识
     */
    private static final short SIZEOF_FILE_INFORMATION = 52;                   // 文件信息结构体长度
    private static final short OFFSETOF_FILE_INFORMATION_ATTRIBUTES = 0;   // 文件属性，参见https://docs.microsoft.com/zh-cn/windows/win32/fileio/file-attribute-constants
    private static final short OFFSETOF_FILE_INFORMATION_CREATETIME = 4;   // 创建时间
    private static final short OFFSETOF_FILE_INFORMATION_LASTACCESSTIME = 12;  // 最后访问时间
    private static final short OFFSETOF_FILE_INFORMATION_LASTWRITETIME = 20;  // 最后修改时间
    private static final short OFFSETOF_FILE_INFORMATION_VOLSERIALNUM = 28;  // 包含文件的卷序列号
    private static final short OFFSETOF_FILE_INFORMATION_SIZEHIGH = 32;  // 大文件尺寸(高位)
    private static final short OFFSETOF_FILE_INFORMATION_SIZELOW = 36;  // 大文件尺寸(低位)，或为小文件尺寸
    private static final short OFFSETOF_FILE_INFORMATION_INDEXHIGH = 44;  // 文件标识(高位)
    private static final short OFFSETOF_FILE_INFORMATION_INDEXLOW = 48;  // 文件标识(低位)
    
    /*
     * 文件信息：此结构描述了通过FindFirstFile、FindFirstFileEx、FindNextFile函数找到的文件。
     *
     * // 参见https://docs.microsoft.com/en-us/previous-versions/windows/embedded/ms892378(v=msdn.10)
     * typedef struct _WIN32_FIND_DATA {
     *     DWORD dwFileAttributes;
     *     FILETIME ftCreationTime;
     *     FILETIME ftLastAccessTime;
     *     FILETIME ftLastWriteTime;
     *     DWORD nFileSizeHigh;
     *     DWORD nFileSizeLow;
     *     DWORD dwReserved0;
     *     DWORD dwReserved1;
     *     TCHAR cFileName[MAX_PATH];
     *     TCHAR cAlternateFileName[14];
     * } WIN32_FIND_DATA;
     */
    private static final short SIZEOF_FIND_DATA = 592;
    private static final short OFFSETOF_FIND_DATA_ATTRIBUTES = 0;   // 文件属性，参见https://docs.microsoft.com/zh-cn/windows/win32/fileio/file-attribute-constants
    private static final short OFFSETOF_FIND_DATA_CREATETIME = 4;   // 创建时间
    private static final short OFFSETOF_FIND_DATA_LASTACCESSTIME = 12;  // 最后访问时间
    private static final short OFFSETOF_FIND_DATA_LASTWRITETIME = 20;  // 最后修改时间
    private static final short OFFSETOF_FIND_DATA_SIZEHIGH = 28;  // 大文件尺寸(高位)
    private static final short OFFSETOF_FIND_DATA_SIZELOW = 32;  // 大文件尺寸(低位)，或为小文件尺寸
    private static final short OFFSETOF_FIND_DATA_RESERVED0 = 36;  // reparse point
    
    // used to adjust values between Windows and java epoch
    private static final long WINDOWS_EPOCH_IN_MICROSECONDS = -11644473600000000L;
    
    // attributes
    private final int fileAttrs;       // 系统属性消息
    private final long creationTime;    // 创建时间
    private final long lastAccessTime;  // 最后访问时间
    private final long lastWriteTime;   // 最后修改时间
    private final long size;            // 文件尺寸(字节数)
    
    // additional attributes when using GetFileInformationByHandle
    private final int volSerialNumber;  // 卷序列号
    private final int fileIndexHigh;    // 文件标识(高位)
    private final int fileIndexLow;     // 文件标识(低位)
    
    private final int reparseTag;       // 宿主资源的reparse point信息，可以判断宿主资源是否为符号链接(参见WindowsConstants#FILE_ATTRIBUTE_REPARSE_POINT)
    
    /** indicates if accurate metadata is required (interesting on NTFS only) */
    // 指示是否需要准确的元数据（仅在NTFS文件系统上使用），默认为false
    private static final boolean ensureAccurateMetadata;
    
    static {
        String propValue = GetPropertyAction.privilegedGetProperty("sun.nio.fs.ensureAccurateMetadata", "false");
        ensureAccurateMetadata = (propValue.length() == 0) || Boolean.parseBoolean(propValue);
    }
    
    /**
     * Initialize a new instance of this class
     */
    private WindowsFileAttributes(int fileAttrs, long creationTime, long lastAccessTime, long lastWriteTime, long size, int reparseTag, int volSerialNumber, int fileIndexHigh, int fileIndexLow) {
        this.fileAttrs = fileAttrs;
        this.creationTime = creationTime;
        this.lastAccessTime = lastAccessTime;
        this.lastWriteTime = lastWriteTime;
        this.size = size;
        
        this.reparseTag = reparseTag;
        this.volSerialNumber = volSerialNumber;
        this.fileIndexHigh = fileIndexHigh;
        this.fileIndexLow = fileIndexLow;
    }
    
    // 返回系统属性消息
    int attributes() {
        return fileAttrs;
    }
    
    // 返回创建时间
    @Override
    public FileTime creationTime() {
        return toFileTime(creationTime);
    }
    
    // 返回最后访问时间
    @Override
    public FileTime lastAccessTime() {
        return toFileTime(lastAccessTime);
    }
    
    // 返回最后修改时间
    @Override
    public FileTime lastModifiedTime() {
        return toFileTime(lastWriteTime);
    }
    
    // 返回文件大小(字节数)
    @Override
    public long size() {
        return size;
    }
    
    // 返回卷序列号
    int volSerialNumber() {
        return volSerialNumber;
    }
    
    // 返回文件标识(高位)
    int fileIndexHigh() {
        return fileIndexHigh;
    }
    
    // 返回文件标识(低位)
    int fileIndexLow() {
        return fileIndexLow;
    }
    
    // 返回唯一标识给定文件的对象。如果文件标识不可用(例如windows上)，则返回null
    @Override
    public Object fileKey() {
        return null;
    }
    
    // 判断当前属性的宿主资源是否为符号链接或目录(硬)链接
    boolean isReparsePoint() {
        return isReparsePoint(fileAttrs);
    }
    
    /**
     * Returns true if the attributes are of a file with a reparse point.
     */
    // 判断attributes的宿主资源是否为符号链接或目录(硬)链接
    static boolean isReparsePoint(int attributes) {
        return (attributes & FILE_ATTRIBUTE_REPARSE_POINT) != 0;
    }
    
    // 判断当前属性的宿主资源是否为符号链接
    @Override
    public boolean isSymbolicLink() {
        return reparseTag == IO_REPARSE_TAG_SYMLINK;
    }
    
    /*
     * 判断当前属性的宿主资源是否为目录的符号链接
     *
     * ( isDirectory() ||  isDirectoryLink()) ==> 目录(不管是不是符号链接)
     * (!isDirectory() && !isDirectoryLink()) ==> 文件(不管是不是符号链接)
     */
    boolean isDirectoryLink() {
        return isSymbolicLink() && ((fileAttrs & FILE_ATTRIBUTE_DIRECTORY) != 0);
    }
    
    /*
     * 判断当前属性的宿主资源是否为非符号链接的目录
     *
     * ( isDirectory() ||  isDirectoryLink()) ==> 目录(不管是不是符号链接)
     * (!isDirectory() && !isDirectoryLink()) ==> 文件(不管是不是符号链接)
     */
    @Override
    public boolean isDirectory() {
        // 不是符号链接，且包含目录标志
        return !isSymbolicLink() && ((fileAttrs & FILE_ATTRIBUTE_DIRECTORY) != 0);
    }
    
    /*
     * 判断当前属性的宿主资源是否为"不透明的"常规文件
     *
     * 在类unix系统中，该类文件是永久存储在文件系统中的字节序列；
     * 在windows上，比如普通文件、文件硬链接，均属于"不透明的"常规文件；
     * 对于符号链接，如果需要将其链接到目标文件，那么文件的符号链接也属于"不透明的"常规文件。
     */
    @Override
    public boolean isRegularFile() {
        return !isSymbolicLink() && !isDirectory() && !isOther();
    }
    
    /*
     * 判断当前属性的宿主资源是否为符号链接/常规文件/目录之外的其他文件
     *
     * 在windows上，如果不需要链接到符号链接的目标文件，那么目录硬链接(mklink /J link target)会被认为属于Other。
     */
    @Override
    public boolean isOther() {
        // return true if device or reparse point
        return !isSymbolicLink() && ((fileAttrs & (FILE_ATTRIBUTE_DEVICE | FILE_ATTRIBUTE_REPARSE_POINT)) != 0);
    }
    
    // 判断是否为只读文件
    @Override
    public boolean isReadOnly() {
        return (fileAttrs & FILE_ATTRIBUTE_READONLY) != 0;
    }
    
    // 判断是否为隐藏文件
    @Override
    public boolean isHidden() {
        return (fileAttrs & FILE_ATTRIBUTE_HIDDEN) != 0;
    }
    
    // 判断是否为系统文件
    @Override
    public boolean isSystem() {
        return (fileAttrs & FILE_ATTRIBUTE_SYSTEM) != 0;
    }
    
    // 判断是否为已存档文件
    @Override
    public boolean isArchive() {
        return (fileAttrs & FILE_ATTRIBUTE_ARCHIVE) != 0;
    }
    
    /**
     * Returns true if the attributes are of the same file - both files must be open.
     */
    // 判断两个文件属性是否相同
    static boolean isSameFile(WindowsFileAttributes attrs1, WindowsFileAttributes attrs2) {
        // volume serial number and file index must be the same
        return (attrs1.volSerialNumber == attrs2.volSerialNumber) && (attrs1.fileIndexHigh == attrs2.fileIndexHigh) && (attrs1.fileIndexLow == attrs2.fileIndexLow);
    }
    
    /**
     * Convert 64-bit value representing the number of 100-nanosecond intervals since January 1, 1601 to a FileTime.
     */
    // 将long类型的time转换为FileTime
    static FileTime toFileTime(long time) {
        // 100ns -> us
        time /= 10L;
        
        // adjust to java epoch
        time += WINDOWS_EPOCH_IN_MICROSECONDS;
        
        return FileTime.from(time, TimeUnit.MICROSECONDS);
    }
    
    /**
     * Convert FileTime to 64-bit value representing the number of 100-nanosecond
     * intervals since January 1, 1601.
     */
    // 将FileTime转换为long类型的time
    static long toWindowsTime(FileTime time) {
        long value = time.to(TimeUnit.MICROSECONDS);
    
        // adjust to Windows epoch+= 11644473600000000L;
        value -= WINDOWS_EPOCH_IN_MICROSECONDS;
    
        // us -> 100ns
        value *= 10L;
    
        return value;
    }
    
    /**
     * Allocates a native buffer for a WIN32_FIND_DATA structure
     */
    // 返回容量至少为SIZEOF_FIND_DATA的缓存(owner为null)，用于WIN32_FIND_DATA结构体
    static NativeBuffer getBufferForFindData() {
        return NativeBuffers.getNativeBuffer(SIZEOF_FIND_DATA);
    }
    
    /**
     * Returns attributes of given file.
     */
    // 返回path处文件/目录的属性信息，followLinks指示对于符号链接，是否将其链接到目标文件
    static WindowsFileAttributes get(WindowsPath path, boolean followLinks) throws WindowsException {
        
        // 如果不需要精确的元数据
        if(!ensureAccurateMetadata) {
            WindowsException firstException = null;
            
            // GetFileAttributesEx is the fastest way to read the attributes
            NativeBuffer buffer = NativeBuffers.getNativeBuffer(SIZEOF_FILE_ATTRIBUTE_DATA);
            try {
                // 本地内存地址
                long address = buffer.address();
                
                // 解析path为适用windows系统的绝对路径
                String absolute = path.getPathForWin32Calls();
                
                // 获取文件(扩展)属性
                GetFileAttributesEx(absolute, address);
                
                /* if reparse point then file may be a sym link; otherwise just return the attributes */
                // 获取文件属性
                int fileAttrs = unsafe.getInt(address + OFFSETOF_FILE_ATTRIBUTE_DATA_ATTRIBUTES);
                
                // 如果path处的资源不是符号链接，也不是目录(硬)链接
                if(!isReparsePoint(fileAttrs)) {
                    // 从address指示的内存中获取windows文件属性信息，并构造WindowsFileAttributes成对象后返回。用于WIN32_FILE_ATTRIBUTE_DATA结构体
                    return fromFileAttributeData(address, 0);
                }
            } catch(WindowsException x) {
                if(x.lastError() != ERROR_SHARING_VIOLATION) {
                    throw x;
                }
                firstException = x;
            } finally {
                buffer.release();
            }
            
            /* For sharing violations, fallback to FindFirstFile if the file is not a root directory */
            // 如果出现了ERROR_SHARING_VIOLATION异常，说明别的进程正在使用path处的文件，此时回退为使用FindFirstFile
            if(firstException != null) {
                
                // 解析path路径为适用windows系统的绝对路径
                String search = path.getPathForWin32Calls();
                
                char last = search.charAt(search.length() - 1);
                if(last == ':' || last == '\\') {
                    // 如果以:或\结尾，则抛异常
                    throw firstException;
                }
                
                // 返回容量至少为SIZEOF_FIND_DATA的缓存(owner为null)，用于WIN32_FIND_DATA结构体
                buffer = getBufferForFindData();
                
                try {
                    long address = buffer.address();
                    
                    // 查找search标识的文件，如果找到，将其属性信息存入address指示的内存
                    long handle = FindFirstFile(search, address);
                    
                    // 关闭由FindFirstFile函数创建的一个搜索句柄
                    FindClose(handle);
                    
                    // 从address指示的内存中获取windows文件属性信息，并构造WindowsFileAttributes成对象后返回。应用于WIN32_FIND_DATA结构体
                    WindowsFileAttributes attrs = fromFindData(address);
                    
                    /*
                     * FindFirstFile does not follow sym links.
                     * Even if followLinks is false, there isn't sufficient information in the WIN32_FIND_DATA structure to know
                     * if the reparse point is a sym link.
                     */
                    // 如果attrs的宿主资源为符号链接或目录(硬)链接，直接抛异常
                    if(attrs.isReparsePoint()) {
                        throw firstException;
                    }
                    
                    return attrs;
                } catch(WindowsException ignore) {
                    throw firstException;
                } finally {
                    buffer.release();
                }
            }
        }
        
        /* file is reparse point so need to open file to get attributes */
        // 打开当前路径标识的文件/目录以便访问其属性
        long handle = path.openForReadAttributeAccess(followLinks);
        
        try {
            // 返回handle文件的windows文件属性信息，用于BY_HANDLE_FILE_INFORMATION结构体
            return readAttributes(handle);
        } finally {
            // 关闭一个打开的对象句柄
            CloseHandle(handle);
        }
    }
    
    /**
     * Reads the attributes of an open file
     */
    // 返回handle文件的windows文件属性信息，用于BY_HANDLE_FILE_INFORMATION结构体
    static WindowsFileAttributes readAttributes(long handle) throws WindowsException {
        
        // 返回容量至少为SIZEOF_FILE_INFORMATION的缓存(owner为null)，适用于BY_HANDLE_FILE_INFORMATION结构体
        NativeBuffer buffer = NativeBuffers.getNativeBuffer(SIZEOF_FILE_INFORMATION);
        
        try {
            long address = buffer.address();
            
            // 检索handle文件的属性信息，将其存入address指示的内存
            GetFileInformationByHandle(handle, address);
            
            // if file is a reparse point then read the tag
            int reparseTag = 0;
            
            // 获取文件属性消息
            int fileAttrs = unsafe.getInt(address + OFFSETOF_FILE_INFORMATION_ATTRIBUTES);
            
            // 如果handle处的资源为符号链接或为目录(硬)链接
            if(isReparsePoint(fileAttrs)) {
                int size = MAXIMUM_REPARSE_DATA_BUFFER_SIZE;
                
                // 返回容量至少为size的缓存(owner为null)
                NativeBuffer reparseBuffer = NativeBuffers.getNativeBuffer(size);
                
                try {
                    // 检索与handle处的资源关联的reparse point数据
                    DeviceIoControlGetReparsePoint(handle, reparseBuffer.address(), size);
                    
                    // 获取reparse point信息
                    reparseTag = (int) unsafe.getLong(reparseBuffer.address());
                } finally {
                    reparseBuffer.release();
                }
            }
            
            // 从address指示的内存中获取windows文件属性信息，并构造WindowsFileAttributes成对象后返回。用于BY_HANDLE_FILE_INFORMATION结构体
            return fromFileInformation(address, reparseTag);
        } finally {
            buffer.release();
        }
    }
    
    /**
     * Create a WindowsFileAttributes from a WIN32_FILE_ATTRIBUTE_DATA structure
     */
    // 从address指示的内存中获取windows文件属性信息，并构造WindowsFileAttributes成对象后返回。用于WIN32_FILE_ATTRIBUTE_DATA结构体
    private static WindowsFileAttributes fromFileAttributeData(long address, int reparseTag) {
        // 文件属性
        int fileAttrs = unsafe.getInt(address + OFFSETOF_FILE_ATTRIBUTE_DATA_ATTRIBUTES);
        // 创建时间
        long creationTime = unsafe.getLong(address + OFFSETOF_FILE_ATTRIBUTE_DATA_CREATETIME);
        // 最后访问时间
        long lastAccessTime = unsafe.getLong(address + OFFSETOF_FILE_ATTRIBUTE_DATA_LASTACCESSTIME);
        // 最后修改时间
        long lastWriteTime = unsafe.getLong(address + OFFSETOF_FILE_ATTRIBUTE_DATA_LASTWRITETIME);
        // 文件尺寸
        long size = ((long) (unsafe.getInt(address + OFFSETOF_FILE_ATTRIBUTE_DATA_SIZEHIGH)) << 32) + (unsafe.getInt(address + OFFSETOF_FILE_ATTRIBUTE_DATA_SIZELOW) & 0xFFFFFFFFL);
        
        return new WindowsFileAttributes(fileAttrs, creationTime, lastAccessTime, lastWriteTime, size, reparseTag, 0, 0, 0);
    }
    
    /**
     * Create a WindowsFileAttributes from a BY_HANDLE_FILE_INFORMATION structure
     */
    // 从address指示的内存中获取windows文件属性信息，并构造WindowsFileAttributes成对象后返回。用于BY_HANDLE_FILE_INFORMATION结构体
    private static WindowsFileAttributes fromFileInformation(long address, int reparseTag) {
        // 文件属性
        int fileAttrs = unsafe.getInt(address + OFFSETOF_FILE_INFORMATION_ATTRIBUTES);
        // 创建时间
        long creationTime = unsafe.getLong(address + OFFSETOF_FILE_INFORMATION_CREATETIME);
        // 最后访问时间
        long lastAccessTime = unsafe.getLong(address + OFFSETOF_FILE_INFORMATION_LASTACCESSTIME);
        // 最后修改时间
        long lastWriteTime = unsafe.getLong(address + OFFSETOF_FILE_INFORMATION_LASTWRITETIME);
        // 文件尺寸
        long size = ((long) (unsafe.getInt(address + OFFSETOF_FILE_INFORMATION_SIZEHIGH)) << 32) + (unsafe.getInt(address + OFFSETOF_FILE_INFORMATION_SIZELOW) & 0xFFFFFFFFL);
        // 卷序列号
        int volSerialNumber = unsafe.getInt(address + OFFSETOF_FILE_INFORMATION_VOLSERIALNUM);
        // 文件标识(高位)
        int fileIndexHigh = unsafe.getInt(address + OFFSETOF_FILE_INFORMATION_INDEXHIGH);
        // 文件标识(低位)
        int fileIndexLow = unsafe.getInt(address + OFFSETOF_FILE_INFORMATION_INDEXLOW);
        
        return new WindowsFileAttributes(fileAttrs, creationTime, lastAccessTime, lastWriteTime, size, reparseTag, volSerialNumber, fileIndexHigh, fileIndexLow);
    }
    
    /**
     * Create a WindowsFileAttributes from a WIN32_FIND_DATA structure
     */
    // 从address指示的内存中获取windows文件属性信息，并构造WindowsFileAttributes成对象后返回。应用于WIN32_FIND_DATA结构体
    static WindowsFileAttributes fromFindData(long address) {
        // 文件属性
        int fileAttrs = unsafe.getInt(address + OFFSETOF_FIND_DATA_ATTRIBUTES);
        // 创建时间
        long creationTime = unsafe.getLong(address + OFFSETOF_FIND_DATA_CREATETIME);
        // 最后访问时间
        long lastAccessTime = unsafe.getLong(address + OFFSETOF_FIND_DATA_LASTACCESSTIME);
        // 最后修改时间
        long lastWriteTime = unsafe.getLong(address + OFFSETOF_FIND_DATA_LASTWRITETIME);
        // 文件尺寸
        long size = ((long) (unsafe.getInt(address + OFFSETOF_FIND_DATA_SIZEHIGH)) << 32) + (unsafe.getInt(address + OFFSETOF_FIND_DATA_SIZELOW) & 0xFFFFFFFFL);
        // reparse point
        int reparseTag = isReparsePoint(fileAttrs) ? unsafe.getInt(address + OFFSETOF_FIND_DATA_RESERVED0) : 0;
        
        return new WindowsFileAttributes(fileAttrs, creationTime, lastAccessTime, lastWriteTime, size, reparseTag, 0, 0, 0);
    }
    
}
