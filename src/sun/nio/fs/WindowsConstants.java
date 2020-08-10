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

/**
 * Win32 APIs constants.
 */
// win32 API 常量
class WindowsConstants {
    
    // general
    public static final long INVALID_HANDLE_VALUE = -1L;    // 标记一个无效的句柄
    
    // generic rights - 通用属性
    public static final int GENERIC_READ = 0x80000000;   // 读文件，指示从文件中读取数据 , 并且移动文件指针
    public static final int GENERIC_WRITE = 0x40000000;   // 写文件，指示向文件中写入数据 , 并且移动文件指针
    
    // share modes - 共享属性
    public static final int FILE_SHARE_READ = 0x00000001;  // 后续请求读访问权的打开操作将会成功
    public static final int FILE_SHARE_WRITE = 0x00000002;  // 后续请求写访问权的打开操作将会成功
    public static final int FILE_SHARE_DELETE = 0x00000004;  // 后续请求删除访问权的打开操作将会成功
    
    // creation modes - 文件创建/打开模式
    public static final int CREATE_NEW = 1;  // 创建文件；如果目标文件存在，则操作失败；否则新建文件
    public static final int CREATE_ALWAYS = 2;  // 创建文件；如果目标文件存在，将其长度截断为0；否则新建文件
    public static final int OPEN_EXISTING = 3;  // 打开文件；如果目标文件存在，则打开文件；否则操作失败
    public static final int OPEN_ALWAYS = 4;  // 打开文件；如果目标文件存在，则打开文件；否则新建文件
    public static final int TRUNCATE_EXISTING = 5;  // 打开文件；如果目标文件存在，将其长度截断为0；否则操作失败
    
    // attributes - 文件属性
    public static final int FILE_ATTRIBUTE_READONLY = 0x00000001;   // 只读文件/目录
    public static final int FILE_ATTRIBUTE_HIDDEN = 0x00000002;   // 隐藏文件/目录
    public static final int FILE_ATTRIBUTE_SYSTEM = 0x00000004;   // 系统文件/目录
    public static final int FILE_ATTRIBUTE_DIRECTORY = 0x00000010;   // 目录
    public static final int FILE_ATTRIBUTE_ARCHIVE = 0x00000020;   // 已存档，用在备份操作中
    public static final int FILE_ATTRIBUTE_DEVICE = 0x00000040;   // 该值保留，以供系统使用
    public static final int FILE_ATTRIBUTE_NORMAL = 0x00000080;   // 没有设置其他属性的文件
    public static final int FILE_ATTRIBUTE_REPARSE_POINT = 0x00000400;   // 重解析点:文件/目录符号链接或目录(硬)链接
    
    // flags - 文件标记
    public static final int FILE_FLAG_NO_BUFFERING = 0x20000000;   // 打开文件时不使用系统缓存，如果此时写入数据，注意磁盘对齐
    public static final int FILE_FLAG_OVERLAPPED = 0x40000000;   // 使用重叠(异步)IO，而不是默认的同步IO
    public static final int FILE_FLAG_WRITE_THROUGH = 0x80000000;   // 文件直接写入磁盘，不使用缓存
    public static final int FILE_FLAG_BACKUP_SEMANTICS = 0x02000000;   // 指示系统为文件的打开或创建执行一个备份或恢复操作
    public static final int FILE_FLAG_DELETE_ON_CLOSE = 0x04000000;   // 指示系统在文件所有打开的句柄关闭后立即删除文件
    public static final int FILE_FLAG_OPEN_REPARSE_POINT = 0x00200000;   // 指示系统打开重解析点，而不是打开链接的目标文件
    
    // stream ids
    public static final int BACKUP_ALTERNATE_DATA = 0x00000004;
    public static final int BACKUP_SPARSE_BLOCK = 0x00000009;
    
    // reparse point/symbolic link related constants
    public static final int IO_REPARSE_TAG_SYMLINK = 0xA000000C;   // 符号链接
    public static final int MAXIMUM_REPARSE_DATA_BUFFER_SIZE = 16 * 1024;    // reparse point数据的最大尺寸
    public static final int SYMBOLIC_LINK_FLAG_DIRECTORY = 0x1;          // 目录的符号链接
    
    // volume flags - 卷标记
    public static final int FILE_CASE_SENSITIVE_SEARCH = 0x00000001;   // 文件系统支持大小写敏感文件名查询
    public static final int FILE_CASE_PRESERVED_NAMES = 0x00000002;   // 当文件名被放在磁盘上时，它的大小写被保留了
    public static final int FILE_PERSISTENT_ACLS = 0x00000008;   // 文件系统保留和强制使用了访问控制列表(只用于NTFS)
    public static final int FILE_VOLUME_IS_COMPRESSED = 0x00008000;   // 文件系统支持基于卷的压缩(NTFS关闭，FAT开启)
    public static final int FILE_NAMED_STREAMS = 0x00040000;   //
    public static final int FILE_READ_ONLY_VOLUME = 0x00080000;   //
    
    // error codes
    public static final int ERROR_FILE_NOT_FOUND = 2;    // The system cannot find the file specified.
    public static final int ERROR_PATH_NOT_FOUND = 3;    // The system cannot find the path specified.
    public static final int ERROR_ACCESS_DENIED = 5;    // Access is denied.
    public static final int ERROR_INVALID_HANDLE = 6;    // The handle is invalid.
    public static final int ERROR_INVALID_DATA = 13;   // The data is invalid.
    public static final int ERROR_NOT_SAME_DEVICE = 17;   // The system cannot move the file to a different disk drive.
    public static final int ERROR_NOT_READY = 21;   // The device is not ready.
    public static final int ERROR_SHARING_VIOLATION = 32;   // The process cannot access the file because it is being used by another process.
    public static final int ERROR_FILE_EXISTS = 80;   // The file exists.
    public static final int ERROR_INVALID_PARAMETER = 87;   // The parameter is incorrect.
    public static final int ERROR_DISK_FULL = 112;  // There is not enough space on the disk.
    public static final int ERROR_INSUFFICIENT_BUFFER = 122;  // The data area passed to a system call is too small.
    public static final int ERROR_INVALID_LEVEL = 124;  // The system call level is not correct.
    public static final int ERROR_DIR_NOT_ROOT = 144;  // The directory is not a subdirectory of the root directory.
    public static final int ERROR_DIR_NOT_EMPTY = 145;  // The directory is not empty.
    public static final int ERROR_ALREADY_EXISTS = 183;  // Cannot create a file when that file already exists.
    public static final int ERROR_MORE_DATA = 234;  // More data is available.
    public static final int ERROR_DIRECTORY = 267;  // The directory name is invalid.
    public static final int ERROR_NOTIFY_ENUM_DIR = 1022; // A notify change request is being completed and the information is not being returned in the caller's buffer. The caller now needs to enumerate the files to find the changes.
    public static final int ERROR_NONE_MAPPED = 1332; // No mapping between account names and security IDs was done.
    public static final int ERROR_NOT_A_REPARSE_POINT = 4390; // The file or directory is not a reparse point.
    public static final int ERROR_INVALID_REPARSE_DATA = 4392; // The data present in the reparse point buffer is invalid.
    
    // 文件系统变化通知参数(注册)
    public static final int FILE_NOTIFY_CHANGE_FILE_NAME = 0x00000001;    // 文件被创建、重命名或删除
    public static final int FILE_NOTIFY_CHANGE_DIR_NAME = 0x00000002;    // 目录被创建、重命名或删除
    public static final int FILE_NOTIFY_CHANGE_ATTRIBUTES = 0x00000004;    // 文件属性被修改
    public static final int FILE_NOTIFY_CHANGE_SIZE = 0x00000008;    // 文件大小被修改
    public static final int FILE_NOTIFY_CHANGE_LAST_WRITE = 0x00000010;    // 文件创建时间被修改
    public static final int FILE_NOTIFY_CHANGE_LAST_ACCESS = 0x00000020;    // 文件的最后写时间被修改
    public static final int FILE_NOTIFY_CHANGE_CREATION = 0x00000040;    // 文件的休后访问时间被修改
    public static final int FILE_NOTIFY_CHANGE_SECURITY = 0x00000100;    // 目录或文件的安全描述符被修改
    
    // 文件系统变化通知参数(反馈)
    public static final int FILE_ACTION_ADDED = 0x00000001;    // 文件创建(文件被添加到目录中)
    public static final int FILE_ACTION_REMOVED = 0x00000002;    // 文件移除(文件从目录中删除)
    public static final int FILE_ACTION_MODIFIED = 0x00000003;    // 文件修改(比如时间戳或者属性的变化)
    public static final int FILE_ACTION_RENAMED_OLD_NAME = 0x00000004;    // 文件移除旧名(重命名)
    public static final int FILE_ACTION_RENAMED_NEW_NAME = 0x00000005;    // 文件创建新名(重命名)
    
    // copy flags
    public static final int COPY_FILE_FAIL_IF_EXISTS = 0x00000001;    // 文件已存在时不拷贝
    public static final int COPY_FILE_COPY_SYMLINK = 0x00000800;    // 如果源文件是符号链接，则目标文件也是符号链接，且它指向源符号链接指向的同一文件。
    
    // move flags
    public static final int MOVEFILE_REPLACE_EXISTING = 0x00000001;
    public static final int MOVEFILE_COPY_ALLOWED = 0x00000002;
    
    // drive types - 磁盘类型
    public static final int DRIVE_UNKNOWN = 0;  // 未知
    public static final int DRIVE_NO_ROOT_DIR = 1;  // 无效的磁盘（如未挂载有效的卷）
    public static final int DRIVE_REMOVABLE = 2;  // U盘或软盘
    public static final int DRIVE_FIXED = 3;  // 本地硬盘(包括移动硬盘)
    public static final int DRIVE_REMOTE = 4;  // 网络磁盘
    public static final int DRIVE_CDROM = 5;  // CD-ROM
    public static final int DRIVE_RAMDISK = 6;  // RAM磁盘
    
    // file security - 安全属性
    public static final int OWNER_SECURITY_INFORMATION = 0x00000001;   // 所有者信息
    public static final int GROUP_SECURITY_INFORMATION = 0x00000002;   // 用户组信息
    public static final int DACL_SECURITY_INFORMATION = 0x00000004;   // 自主访问控制列表，指示user和group的访问权限
    public static final int SACL_SECURITY_INFORMATION = 0x00000008;   // 系统访问控制列表，指示对该文件读/写/执行的权限细节
    
    // sid type
    public static final int SidTypeUser = 1;
    public static final int SidTypeGroup = 2;
    public static final int SidTypeDomain = 3;
    public static final int SidTypeAlias = 4;
    public static final int SidTypeWellKnownGroup = 5;
    public static final int SidTypeDeletedAccount = 6;
    public static final int SidTypeInvalid = 7;
    public static final int SidTypeUnknown = 8;
    public static final int SidTypeComputer = 9;
    
    // 访问控制权限，参见AclEntryType
    public static final byte ACCESS_ALLOWED_ACE_TYPE = 0x0;
    public static final byte ACCESS_DENIED_ACE_TYPE = 0x1;
    
    // ACE继承规则，参见AclEntryFlag
    public static final byte OBJECT_INHERIT_ACE = 0x1;
    public static final byte CONTAINER_INHERIT_ACE = 0x2;
    public static final byte NO_PROPAGATE_INHERIT_ACE = 0x4;
    public static final byte INHERIT_ONLY_ACE = 0x8;
    
    // 实体访问权限，参见AclEntryPermission
    public static final int FILE_LIST_DIRECTORY = 0x0001;
    public static final int FILE_READ_DATA = 0x0001;
    public static final int FILE_WRITE_DATA = 0x0002;
    public static final int FILE_APPEND_DATA = 0x0004;
    public static final int FILE_READ_EA = 0x0008;
    public static final int FILE_WRITE_EA = 0x0010;
    public static final int FILE_EXECUTE = 0x0020;
    public static final int FILE_DELETE_CHILD = 0x0040;
    public static final int FILE_READ_ATTRIBUTES = 0x0080;
    public static final int FILE_WRITE_ATTRIBUTES = 0x0100;
    public static final int DELETE = 0x00010000;
    public static final int READ_CONTROL = 0x00020000;
    public static final int WRITE_DAC = 0x00040000;
    public static final int WRITE_OWNER = 0x00080000;
    public static final int SYNCHRONIZE = 0x00100000;
    
    public static final int FILE_GENERIC_READ = 0x00120089;
    public static final int FILE_GENERIC_WRITE = 0x00120116;
    public static final int FILE_GENERIC_EXECUTE = 0x001200a0;
    public static final int FILE_ALL_ACCESS = 0x001f01ff;
    
    // operating system security
    public static final int TOKEN_DUPLICATE = 0x0002;
    public static final int TOKEN_IMPERSONATE = 0x0004;
    public static final int TOKEN_QUERY = 0x0008;
    public static final int TOKEN_ADJUST_PRIVILEGES = 0x0020;
    
    public static final int SE_PRIVILEGE_ENABLED = 0x00000002;
    
    public static final int TokenUser = 1;
    public static final int PROCESS_QUERY_INFORMATION = 0x0400;
    
    private WindowsConstants() {
    }
    
}
