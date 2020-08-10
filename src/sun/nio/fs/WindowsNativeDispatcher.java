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

import java.security.AccessController;
import java.security.PrivilegedAction;
import jdk.internal.misc.Unsafe;

/**
 * Win32 and library calls.
 */
// windows操作分派器，用来完成与本地库的交互
class WindowsNativeDispatcher {
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    
    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                // nio.dll has dependency on net.dll
                System.loadLibrary("net");
                System.loadLibrary("nio");
                return null;
            }
        });
        initIDs();
    }
    
    
    private WindowsNativeDispatcher() {
    }
    
    
    
    /*▼ 文件属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * DWORD GetFileAttributes(
     * LPCTSTR lpFileName
     * )
     */
    // 获取文件属性，参见WindowsConstants##attributes
    static int GetFileAttributes(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return GetFileAttributes0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    
    /**
     * SetFileAttributes(
     *   LPCTSTR lpFileName,
     *   DWORD dwFileAttributes
     * )
     */
    // 设置文件属性
    static void SetFileAttributes(String path, int dwFileAttributes) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            SetFileAttributes0(buffer.address(), dwFileAttributes);
        } finally {
            buffer.release();
        }
    }
    
    /**
     * GetFileAttributesEx(
     *   LPCTSTR lpFileName,
     *   GET_FILEEX_INFO_LEVELS fInfoLevelId,
     *   LPVOID lpFileInformation
     * );
     */
    // 获取文件(扩展)属性，除了可以获取GetFileAttributes中获取到的属性，还能够获取到文件的创建日期，最后读写日期以及文件大小等信息
    static void GetFileAttributesEx(String path, long address) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            GetFileAttributesEx0(buffer.address(), address);
        } finally {
            buffer.release();
        }
    }
    
    /**
     * GetFileInformationByHandle(
     *   HANDLE hFile,
     *   LPBY_HANDLE_FILE_INFORMATION lpFileInformation
     * )
     */
    // 检索handle文件的属性信息，将其存入address指示的内存
    static native void GetFileInformationByHandle(long handle, long address) throws WindowsException;
    
    private static native void GetFileAttributesEx0(long lpFileName, long address) throws WindowsException;
    
    private static native int GetFileAttributes0(long lpFileName) throws WindowsException;
    
    private static native void SetFileAttributes0(long lpFileName, int dwFileAttributes) throws WindowsException;
    
    /*▲ 文件属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 创建/打开文件
    static long CreateFile(String path, int dwDesiredAccess, int dwShareMode, int dwCreationDisposition, int dwFlagsAndAttributes) throws WindowsException {
        return CreateFile(path, dwDesiredAccess, dwShareMode, 0L, dwCreationDisposition, dwFlagsAndAttributes);
    }
    
    /**
     * HANDLE CreateFile(
     *   LPCTSTR lpFileName,                            // 普通文件名或者设备文件名
     *   DWORD dwDesiredAccess,                         // 访问模式（写/读）
     *   DWORD dwShareMode,                             // 共享模式
     *   LPSECURITY_ATTRIBUTES lpSecurityAttributes,    // 指向安全属性的指针
     *   DWORD dwCreationDisposition,                   // 创建模式
     *   DWORD dwFlagsAndAttributes,                    // 文件属性
     *   HANDLE hTemplateFile                           // 模板文件。如果不为零，则指定一个文件句柄，新文件将从这个文件中复制扩展属性
     * )
     */
    // 创建/打开文件
    static long CreateFile(String path, int dwDesiredAccess, int dwShareMode, long lpSecurityAttributes, int dwCreationDisposition, int dwFlagsAndAttributes) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return CreateFile0(buffer.address(), dwDesiredAccess, dwShareMode, lpSecurityAttributes, dwCreationDisposition, dwFlagsAndAttributes);
        } finally {
            buffer.release();
        }
    }
    
    /**
     * DeleteFile(
     *   LPCTSTR lpFileName
     * )
     */
    // 删除文件
    static void DeleteFile(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            DeleteFile0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    
    /**
     * CreateDirectory(
     *   LPCTSTR lpPathName,
     *   LPSECURITY_ATTRIBUTES lpSecurityAttributes
     * )
     */
    // 创建目录
    static void CreateDirectory(String path, long lpSecurityAttributes) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            CreateDirectory0(buffer.address(), lpSecurityAttributes);
        } finally {
            buffer.release();
        }
    }
    
    /**
     * RemoveDirectory(
     *   LPCTSTR lpPathName
     * )
     */
    // 删除目录
    static void RemoveDirectory(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            RemoveDirectory0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    
    /**
     * CopyFileEx(
     *   LPCWSTR lpExistingFileName
     *   LPCWSTR lpNewFileName,
     *   LPPROGRESS_ROUTINE lpProgressRoutine
     *   LPVOID lpData,
     *   LPBOOL pbCancel,
     *   DWORD dwCopyFlags
     * )
     */
    // 复制
    static void CopyFileEx(String source, String target, int flags, long addressToPollForCancel) throws WindowsException {
        NativeBuffer sourceBuffer = asNativeBuffer(source);
        NativeBuffer targetBuffer = asNativeBuffer(target);
        try {
            CopyFileEx0(sourceBuffer.address(), targetBuffer.address(), flags, addressToPollForCancel);
        } finally {
            targetBuffer.release();
            sourceBuffer.release();
        }
    }
    
    /**
     * MoveFileEx(
     *   LPCTSTR lpExistingFileName,
     *   LPCTSTR lpNewFileName,
     *   DWORD dwFlags
     * )
     */
    // 移动
    static void MoveFileEx(String source, String target, int flags) throws WindowsException {
        NativeBuffer sourceBuffer = asNativeBuffer(source);
        NativeBuffer targetBuffer = asNativeBuffer(target);
        try {
            MoveFileEx0(sourceBuffer.address(), targetBuffer.address(), flags);
        } finally {
            targetBuffer.release();
            sourceBuffer.release();
        }
    }
    
    /**
     * CreateSymbolicLink(
     *   LPCWSTR lpSymlinkFileName,
     *   LPCWSTR lpTargetFileName,
     *   DWORD dwFlags
     * )
     */
    // 创建符号链接
    static void CreateSymbolicLink(String link, String target, int flags) throws WindowsException {
        NativeBuffer linkBuffer = asNativeBuffer(link);
        NativeBuffer targetBuffer = asNativeBuffer(target);
        try {
            CreateSymbolicLink0(linkBuffer.address(), targetBuffer.address(), flags);
        } finally {
            targetBuffer.release();
            linkBuffer.release();
        }
    }
    
    /**
     * CreateHardLink(
     *    LPCTSTR lpFileName,
     *    LPCTSTR lpExistingFileName,
     *    LPSECURITY_ATTRIBUTES lpSecurityAttributes
     * )
     */
    // 创建文件的硬链接
    static void CreateHardLink(String newFile, String existingFile) throws WindowsException {
        NativeBuffer newFileBuffer = asNativeBuffer(newFile);
        NativeBuffer existingFileBuffer = asNativeBuffer(existingFile);
        try {
            CreateHardLink0(newFileBuffer.address(), existingFileBuffer.address());
        } finally {
            existingFileBuffer.release();
            newFileBuffer.release();
        }
    }
    
    /**
     * CloseHandle(
     *   HANDLE hObject
     * )
     */
    // 关闭一个打开的句柄
    static native void CloseHandle(long handle);
    
    private static native long CreateFile0(long lpFileName, int dwDesiredAccess, int dwShareMode, long lpSecurityAttributes, int dwCreationDisposition, int dwFlagsAndAttributes) throws WindowsException;
    private static native void CreateDirectory0(long lpFileName, long lpSecurityAttributes) throws WindowsException;
    private static native void CreateSymbolicLink0(long linkAddress, long targetAddress, int flags) throws WindowsException;
    private static native void CreateHardLink0(long newFileBuffer, long existingFileBuffer) throws WindowsException;
    private static native void DeleteFile0(long lpFileName) throws WindowsException;
    private static native void RemoveDirectory0(long lpFileName) throws WindowsException;
    private static native void CopyFileEx0(long existingAddress, long newAddress, int flags, long addressToPollForCancel) throws WindowsException;
    private static native void MoveFileEx0(long existingAddress, long newAddress, int flags) throws WindowsException;
    
    /*▲ 文件操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * HANDLE FindFirstFile(
     *   LPCTSTR lpFileName,                // 用于指定搜索目录和文件类型，可以用通配符
     *   LPWIN32_FIND_DATA lpFindFileData   // 用于保存搜索得到的文件信息
     * )
     */
    // 返回指定目录的第一个文件，返回搜索到的文件信息
    static FirstFile FindFirstFile(String path) throws WindowsException {
        // 将str中的数据拷贝到一块本地缓存中后返回
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            FirstFile data = new FirstFile();
            FindFirstFile0(buffer.address(), data);
            return data;
        } finally {
            buffer.release();
        }
    }
    
    /**
     * HANDLE FindFirstFile(
     *   LPCTSTR lpFileName,
     *   LPWIN32_FIND_DATA lpFindFileData
     * )
     */
    // 查找path标识的文件，如果找到，将其属性信息存入address指示的内存
    static long FindFirstFile(String path, long address) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return FindFirstFile1(buffer.address(), address);
        } finally {
            buffer.release();
        }
    }
    
    /**
     * HANDLE FindFirstStreamW(
     *   LPCWSTR lpFileName,
     *   STREAM_INFO_LEVELS InfoLevel,
     *   LPVOID lpFindStreamData,
     *   DWORD dwFlags
     * )
     */
    // 获取到file上首个备用数据流(对于目录，返回null)
    static FirstStream FindFirstStream(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            FirstStream data = new FirstStream();
            FindFirstStream0(buffer.address(), data);
            if(data.handle() == WindowsConstants.INVALID_HANDLE_VALUE)
                return null;
            return data;
        } finally {
            buffer.release();
        }
    }
    
    /*
     * FindNextStreamW(
     *   HANDLE hFindStream,
     *   LPVOID lpFindStreamData
     * )
     */
    // 获取后续的备用数据流
    static native String FindNextStream(long handle) throws WindowsException;
    
    /**
     * FindNextFile(
     *   HANDLE hFindFile,                  // 上一次FindFirstFile或FindNextFile得到的HANDLE
     *   LPWIN32_FIND_DATA lpFindFileData   // 用于保存搜索得到的文件信息
     * )
     *
     * @return lpFindFileData->cFileName or null
     */
    // 基于上次的搜索(如FindFirstFile或FindNextFile)来搜索下一个文件，搜索到的文件信息存储到address指示的内存中
    static native String FindNextFile(long handle, long address) throws WindowsException;
    
    private static native void FindFirstFile0(long lpFileName, FirstFile obj) throws WindowsException;
    
    private static native long FindFirstFile1(long lpFileName, long address) throws WindowsException;
    
    private static native void FindFirstStream0(long lpFileName, FirstStream obj) throws WindowsException;
    
    /**
     * FindClose(
     *   HANDLE hFindFile
     * )
     */
    // 关闭由FindFirstFile函数创建的一个搜索句柄
    static native void FindClose(long handle) throws WindowsException;
    
    /*▲ 查找 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ SID ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * ConvertStringSidToSid(
     *   LPCTSTR StringSid,
     *   PSID* pSid
     * )
     *
     * @return pSid
     */
    // 将字符串形式的SID转换为long类型
    static long ConvertStringSidToSid(String sidString) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(sidString);
        try {
            return ConvertStringSidToSid0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    
    /**
     * ConvertSidToStringSid(
     *   PSID Sid,
     *   LPTSTR* StringSid
     * )
     *
     * @return StringSid
     */
    // 获取SID的字符串形式
    static native String ConvertSidToStringSid(long sidAddress) throws WindowsException;
    
    private static native long ConvertStringSidToSid0(long lpStringSid) throws WindowsException;
    
    /*▲ SID ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * GetFileSecurity(
     *   LPCTSTR lpFileName,
     *   SECURITY_INFORMATION RequestedInformation,
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   DWORD nLength,
     *   LPDWORD lpnLengthNeeded
     * )
     */
    /*
     * 读取指定文件的安全属性，从中可以获取到文件的所有者和用户组信息；如果读取成功，返回读到的信息长度
     *
     * path                : 待读取文件
     * requestedInformation: 一个SECURITY_INFORMATION值，指示请求读取的安全信息
     * pSecurityDescriptor : 指向缓冲区的指针，该缓冲区存储读取到的安全描述符信息
     * nLength             : 上述缓冲区的大小
     */
    static int GetFileSecurity(String path, int requestedInformation, long pSecurityDescriptor, int nLength) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return GetFileSecurity0(buffer.address(), requestedInformation, pSecurityDescriptor, nLength);
        } finally {
            buffer.release();
        }
    }
    
    /**
     * SetFileSecurity(
     *   LPCTSTR lpFileName,
     *   SECURITY_INFORMATION SecurityInformation,
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor
     * )
     */
    // 为文件设置安全属性
    static void SetFileSecurity(String path, int securityInformation, long pSecurityDescriptor) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            SetFileSecurity0(buffer.address(), securityInformation, pSecurityDescriptor);
        } finally {
            buffer.release();
        }
    }
    
    /**
     * GetAclInformation(
     *   PACL pAcl,
     *   LPVOID pAclInformation,
     *   DWORD nAclInformationLength,
     *   ACL_INFORMATION_CLASS dwAclInformationClass
     * )
     */
    // 从指定的安全描述符中获取DACL实体信息
    static AclInformation GetAclInformation(long aclAddress) {
        AclInformation info = new AclInformation();
        GetAclInformation0(aclAddress, info);
        return info;
    }
    
    /**
     * LookupAccountSid(
     *   LPCTSTR lpSystemName,
     *   PSID Sid,
     *   LPTSTR Name,
     *   LPDWORD cbName,
     *   LPTSTR ReferencedDomainName,
     *   LPDWORD cbReferencedDomainName,
     *   PSID_NAME_USE peUse
     * )
     */
    // 将SID转换为账户信息
    static Account LookupAccountSid(long sidAddress) throws WindowsException {
        Account acc = new Account();
        LookupAccountSid0(sidAddress, acc);
        return acc;
    }
    
    /**
     * LookupAccountName(
     *   LPCTSTR lpSystemName,
     *   LPCTSTR lpAccountName,
     *   PSID Sid,
     *   LPDWORD cbSid,
     *   LPTSTR ReferencedDomainName,
     *   LPDWORD cbReferencedDomainName,
     *   PSID_NAME_USE peUse
     * )
     *
     * @return cbSid
     */
    // 查询账户信息
    static int LookupAccountName(String accountName, long pSid, int cbSid) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(accountName);
        try {
            return LookupAccountName0(buffer.address(), pSid, cbSid);
        } finally {
            buffer.release();
        }
    }
    
    static native void SetFileSecurity0(long lpFileName, int securityInformation, long pSecurityDescriptor) throws WindowsException;
    
    private static native int GetFileSecurity0(long lpFileName, int requestedInformation, long pSecurityDescriptor, int nLength) throws WindowsException;
    
    /**
     * InitializeSecurityDescriptor(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   DWORD dwRevision
     * )
     */
    static native void InitializeSecurityDescriptor(long sdAddress) throws WindowsException;
    
    /**
     * GetSecurityDescriptorOwner(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor
     *   PSID *pOwner,
     *   LPBOOL lpbOwnerDefaulted
     * )
     *
     * @return pOwner
     */
    static native long GetSecurityDescriptorOwner(long pSecurityDescriptor) throws WindowsException;
    
    /**
     * SetSecurityDescriptorOwner(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   PSID pOwner,
     *   BOOL bOwnerDefaulted
     * )
     */
    static native void SetSecurityDescriptorOwner(long pSecurityDescriptor, long pOwner) throws WindowsException;
    
    /**
     * GetSecurityDescriptorDacl(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   LPBOOL lpbDaclPresent,
     *   PACL *pDacl,
     *   LPBOOL lpbDaclDefaulted
     * )
     */
    static native long GetSecurityDescriptorDacl(long pSecurityDescriptor);
    
    /**
     * SetSecurityDescriptorDacl(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   BOOL bDaclPresent,
     *   PACL pDacl,
     *   BOOL bDaclDefaulted
     * )
     */
    static native void SetSecurityDescriptorDacl(long pSecurityDescriptor, long pAcl) throws WindowsException;
    
    /**
     * InitializeAcl(
     *   PACL pAcl,
     *   DWORD nAclLength,
     *   DWORD dwAclRevision
     * )
     */
    static native void InitializeAcl(long aclAddress, int size) throws WindowsException;
    
    private static native void GetAclInformation0(long aclAddress, AclInformation obj);
    
    /**
     * GetAce(
     *   PACL pAcl,
     *   DWORD dwAceIndex,
     *   LPVOID *pAce
     * )
     */
    static native long GetAce(long aclAddress, int aceIndex);
    
    /**
     * AddAccessAllowedAceEx(
     *   PACL pAcl,
     *   DWORD dwAceRevision,
     *   DWORD AceFlags,
     *   DWORD AccessMask,
     *   PSID pSid
     * )
     */
    static native void AddAccessAllowedAceEx(long aclAddress, int flags, int mask, long sidAddress) throws WindowsException;
    
    /**
     * AddAccessDeniedAceEx(
     *   PACL pAcl,
     *   DWORD dwAceRevision,
     *   DWORD AceFlags,
     *   DWORD AccessMask,
     *   PSID pSid
     * )
     */
    static native void AddAccessDeniedAceEx(long aclAddress, int flags, int mask, long sidAddress) throws WindowsException;
    
    private static native void LookupAccountSid0(long sidAddress, Account obj) throws WindowsException;
    
    private static native int LookupAccountName0(long lpAccountName, long pSid, int cbSid) throws WindowsException;
    
    /**
     * DWORD GetLengthSid(
     *   PSID pSid
     * )
     */
    static native int GetLengthSid(long sidAddress);
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 磁盘 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * DWORD GetLogicalDrives(VOID)
     */
    // 返回驱动器的盘符列表；返回值是一个32位的值，每一位表示一个逻辑驱动器是否存在，比如A盘存在，那么第0位将被设置为1
    static native int GetLogicalDrives() throws WindowsException;
    
    /**
     * UINT GetDriveType(
     * LPCTSTR lpRootPathName
     * )
     */
    // 获取指定盘符的磁盘类型，参见WindowsConstants->drive types
    static int GetDriveType(String root) throws WindowsException {
        // 将root中的数据拷贝到一块缓存中后返回
        NativeBuffer buffer = asNativeBuffer(root);
        try {
            return GetDriveType0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    
    /**
     * GetVolumeInformation(
     *   LPCTSTR lpRootPathName,
     *   LPTSTR lpVolumeNameBuffer,
     *   DWORD nVolumeNameSize,
     *   LPDWORD lpVolumeSerialNumber,
     *   LPDWORD lpMaximumComponentLength,
     *   LPDWORD lpFileSystemFlags,
     *   LPTSTR lpFileSystemNameBuffer,
     *   DWORD nFileSystemNameSize
     * )
     */
    // 获取指定盘符的磁盘卷信息
    static VolumeInformation GetVolumeInformation(String root) throws WindowsException {
        // 将root中的数据拷贝到一块缓存中后返回
        NativeBuffer buffer = asNativeBuffer(root);
        
        try {
            VolumeInformation info = new VolumeInformation();
            // 获取卷信息
            GetVolumeInformation0(buffer.address(), info);
            return info;
        } finally {
            buffer.release();
        }
    }
    
    /**
     * GetVolumePathName(
     *   LPCTSTR lpszFileName,
     *   LPTSTR lpszVolumePathName,
     *   DWORD cchBufferLength
     * )
     *
     * @return lpFileName
     */
    // 获取磁盘路径信息
    static String GetVolumePathName(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return GetVolumePathName0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    
    private static native int GetDriveType0(long lpRoot) throws WindowsException;
    private static native void GetVolumeInformation0(long lpRoot, VolumeInformation obj) throws WindowsException;
    private static native String GetVolumePathName0(long lpFileName) throws WindowsException;
    
    /*▲ 磁盘 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 完成端口 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * HANDLE CreateIoCompletionPort (
     *   HANDLE FileHandle,
     *   HANDLE ExistingCompletionPort,
     *   ULONG_PTR CompletionKey,
     *   DWORD NumberOfConcurrentThreads
     * )
     */
    // 用于创建一个完成端口对象，或将一个文件句柄与已有的完成端口关联到一起
    static native long CreateIoCompletionPort(long fileHandle, long existingPort, long completionKey) throws WindowsException;
    
    /**
     * GetQueuedCompletionStatus(
     *   HANDLE CompletionPort,
     *   LPDWORD lpNumberOfBytesTransferred,
     *   PULONG_PTR lpCompletionKey,
     *   LPOVERLAPPED *lpOverlapped,
     *   DWORD dwMilliseconds
     */
    // 无限阻塞，直到"完成端口"completionPort有新的通知就绪时，获取该通知的内容，并存入status中后返回
    static CompletionStatus GetQueuedCompletionStatus(long completionPort) throws WindowsException {
        CompletionStatus status = new CompletionStatus();
        /*
         * completionPort：指向"完成端口"内核对象的指针
         * status：存储IO结束后的返回的通知（数据）
         */
        GetQueuedCompletionStatus0(completionPort, status);
        return status;
    }
    
    /**
     * PostQueuedCompletionStatus(
     *   HANDLE CompletionPort,
     *   DWORD dwNumberOfBytesTransferred,
     *   ULONG_PTR dwCompletionKey,
     *   LPOVERLAPPED lpOverlapped
     * )
     */
    /*
     * 向"完成端口"completionPort处的"完成端口"对象发送消息，可以唤醒阻塞的getQueuedCompletionStatus()方法；
     * 换句话说，这可以用来与阻塞在getQueuedCompletionStatus()的线程进行通信。
     * 注：postQueuedCompletionStatus中传递的数据会到达GetQueuedCompletionStatus()的CompletionStatus参数中
     */
    static native void PostQueuedCompletionStatus(long completionPort, long completionKey) throws WindowsException;
    
    // 无限阻塞，直到"完成端口"completionPort有新的通知就绪时，获取该通知的内容，并存入status中后返回
    private static native void GetQueuedCompletionStatus0(long completionPort, CompletionStatus status) throws WindowsException;
    
    /**
     * CancelIo(
     *   HANDLE hFile
     * )
     */
    // 取消完成队列中尚未处理的IO操作
    static native void CancelIo(long hFile) throws WindowsException;
    
    /**
     * GetOverlappedResult(
     *   HANDLE hFile,
     *   LPOVERLAPPED lpOverlapped,
     *   LPDWORD lpNumberOfBytesTransferred,
     *   BOOL bWait
     * );
     */
    static native int GetOverlappedResult(long hFile, long lpOverlapped) throws WindowsException;
    
    /*▲ 完成端口 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存储器容量 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * GetDiskFreeSpace(
     *   LPCTSTR lpRootPathName,
     *   LPDWORD lpSectorsPerCluster,
     *   LPDWORD lpBytesPerSector,
     *   LPDWORD lpNumberOfFreeClusters,
     *   LPDWORD lpTotalNumberOfClusters
     * )
     */
    // 获取存储器的容量信息，限定在2G以内的磁盘上使用
    static DiskFreeSpace GetDiskFreeSpace(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            DiskFreeSpace space = new DiskFreeSpace();
            GetDiskFreeSpace0(buffer.address(), space);
            return space;
        } finally {
            buffer.release();
        }
    }
    
    /**
     * GetDiskFreeSpaceEx(
     *   LPCTSTR lpDirectoryName,
     *   PULARGE_INTEGER lpFreeBytesAvailableToCaller,
     *   PULARGE_INTEGER lpTotalNumberOfBytes,
     *   PULARGE_INTEGER lpTotalNumberOfFreeBytes
     * )
     */
    // 获取存储器的容量信息
    static DiskFreeSpace GetDiskFreeSpaceEx(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            DiskFreeSpace space = new DiskFreeSpace();
            GetDiskFreeSpaceEx0(buffer.address(), space);
            return space;
        } finally {
            buffer.release();
        }
    }
    
    private static native void GetDiskFreeSpace0(long lpRootPathName, DiskFreeSpace obj) throws WindowsException;
    
    private static native void GetDiskFreeSpaceEx0(long lpDirectoryName, DiskFreeSpace obj) throws WindowsException;
    
    /*▲ 存储器容量 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * HANDLE GetCurrentProcess(VOID)
     */
    static native long GetCurrentProcess();
    
    /**
     * HANDLE GetCurrentThread(VOID)
     */
    static native long GetCurrentThread();
    
    /**
     * OpenProcessToken(
     *   HANDLE ProcessHandle,
     *   DWORD DesiredAccess,
     *   PHANDLE TokenHandle
     * )
     */
    static native long OpenProcessToken(long hProcess, int desiredAccess) throws WindowsException;
    
    /**
     * OpenThreadToken(
     *   HANDLE ThreadHandle,
     *   DWORD DesiredAccess,
     *   BOOL OpenAsSelf,
     *   PHANDLE TokenHandle
     * )
     */
    static native long OpenThreadToken(long hThread, int desiredAccess, boolean openAsSelf) throws WindowsException;
    
    /**
     * SetThreadToken(
     *   PHANDLE Thread,
     *   HANDLE Token
     * )
     */
    static native void SetThreadToken(long thread, long hToken) throws WindowsException;
    
    /**
     * GetTokenInformation(
     *   HANDLE TokenHandle,
     *   TOKEN_INFORMATION_CLASS TokenInformationClass,
     *   LPVOID TokenInformation,
     *   DWORD TokenInformationLength,
     *   PDWORD ReturnLength
     * )
     */
    static native int GetTokenInformation(long token, int tokenInfoClass, long pTokenInfo, int tokenInfoLength) throws WindowsException;
    
    static native long DuplicateTokenEx(long hThread, int desiredAccess) throws WindowsException;
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    private static native void initIDs();
    
    // 将str中的数据拷贝到一块本地缓存中后返回
    static NativeBuffer asNativeBuffer(String str) {
        int stringLengthInBytes = str.length() << 1;
        
        int sizeInBytes = stringLengthInBytes + 2;  // char terminator
        
        // 从缓存池中获取容量至少为sizeInBytes的缓存
        NativeBuffer buffer = NativeBuffers.getNativeBufferFromCache(sizeInBytes);  // get a native buffer of sufficient size
        
        // 如果没获取到缓存，则需要新建一块缓存
        if(buffer == null) {
            // 获取新建的缓存(使用了本地内存)
            buffer = NativeBuffers.allocNativeBuffer(sizeInBytes);
        } else {
            // buffer already contains the string contents
            if(buffer.owner() == str) {
                // 如果buffer来自缓存池，且已经有owner，则可以直接返回
                return buffer;
            }
        }
        
        // 将str中的数据存储到chars中
        char[] chars = str.toCharArray();   // copy into buffer and zero terminate
        
        // 将chars中的数据拷贝到buffer中
        unsafe.copyMemory(chars, Unsafe.ARRAY_CHAR_BASE_OFFSET, null, buffer.address(), stringLengthInBytes);
        
        // 将buffer后面没数据的空槽填充为0
        unsafe.putChar(buffer.address() + stringLengthInBytes, (char) 0);
        
        // 为buffer设置owner
        buffer.setOwner(str);
        
        return buffer;
    }
    
    /**
     * GetFullPathName(
     *   LPCTSTR lpFileName,
     *   DWORD nBufferLength,
     *   LPTSTR lpBuffer,
     *   LPTSTR *lpFilePart
     * )
     */
    // 获取指定路径path的完整路径
    static String GetFullPathName(String path) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(path);
        try {
            return GetFullPathName0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    
    private static native String GetFullPathName0(long pathAddress) throws WindowsException;
    
    /**
     * Marks a file as a sparse file.
     *
     * DeviceIoControl(
     *   FSCTL_SET_SPARSE
     * )
     */
    static native void DeviceIoControlSetSparse(long handle) throws WindowsException;
    
    /**
     * Retrieves the reparse point data associated with the file or directory.
     *
     * DeviceIoControl(
     *   FSCTL_GET_REPARSE_POINT
     * )
     */
    // 检索与handle处的资源关联的reparse point数据
    static native void DeviceIoControlGetReparsePoint(long handle, long bufferAddress, int bufferSize) throws WindowsException;
    
    static long LookupPrivilegeValue(String name) throws WindowsException {
        NativeBuffer buffer = asNativeBuffer(name);
        try {
            return LookupPrivilegeValue0(buffer.address());
        } finally {
            buffer.release();
        }
    }
    
    private static native long LookupPrivilegeValue0(long lpName) throws WindowsException;
    
    /**
     * AdjustTokenPrivileges(
     *   HANDLE TokenHandle,
     *   BOOL DisableAllPrivileges
     *   PTOKEN_PRIVILEGES NewState
     *   DWORD BufferLength
     *   PTOKEN_PRIVILEGES
     *   PDWORD ReturnLength
     * )
     */
    static native void AdjustTokenPrivileges(long token, long luid, int attributes) throws WindowsException;
    
    /**
     * SetFileTime(
     *   HANDLE hFile,
     *   CONST FILETIME *lpCreationTime,
     *   CONST FILETIME *lpLastAccessTime,
     *   CONST FILETIME *lpLastWriteTime
     * )
     */
    static native void SetFileTime(long handle, long createTime, long lastAccessTime, long lastWriteTime) throws WindowsException;
    
    /**
     * SetEndOfFile(
     *   HANDLE hFile
     * )
     */
    static native void SetEndOfFile(long handle) throws WindowsException;
    
    /**
     * AccessCheck(
     *   PSECURITY_DESCRIPTOR pSecurityDescriptor,
     *   HANDLE ClientToken,
     *   DWORD DesiredAccess,
     *   PGENERIC_MAPPING GenericMapping,
     *   PPRIVILEGE_SET PrivilegeSet,
     *   LPDWORD PrivilegeSetLength,
     *   LPDWORD GrantedAccess,
     *   LPBOOL AccessStatus
     * )
     */
    static native boolean AccessCheck(long token, long securityInfo, int accessMask, int genericRead, int genericWrite, int genericExecute, int genericAll) throws WindowsException;
    
    /**
     * GetFinalPathNameByHandle(
     *   HANDLE hFile,
     *   LPTSTR lpszFilePath,
     *   DWORD cchFilePath,
     *   DWORD dwFlags
     * )
     */
    static native String GetFinalPathNameByHandle(long handle) throws WindowsException;
    
    /**
     * FormatMessage(
     *   DWORD dwFlags,
     *   LPCVOID lpSource,
     *   DWORD dwMessageId,
     *   DWORD dwLanguageId,
     *   LPTSTR lpBuffer,
     *   DWORD nSize,
     *   va_list *Arguments
     * )
     */
    static native String FormatMessage(int errorCode);
    
    /**
     * LocalFree(
     *   HLOCAL hMem
     * )
     */
    static native void LocalFree(long address);
    
    /**
     * HANDLE CreateEvent(
     *   LPSECURITY_ATTRIBUTES lpEventAttributes,   // 安全属性
     *   BOOL bManualReset,     // 设置信号复位方式为自动恢复为无信号状态(false)还是手动恢复为无信号状态(true)
     *   BOOL bInitialState,    // 信号初始状态，有信号(true)还是无信号(false)
     *   PCTSTR lpName          // 信号名称，可以为空
     * );
     */
    // 用来创建或打开一个命名的或无名的事件对象
    static native long CreateEvent(boolean bManualReset, boolean bInitialState) throws WindowsException;
    
    /**
     * ReadDirectoryChangesW (
     *   HANDLE  hDirectory,        // 要监视的目录的句柄
     *   LPVOID  lpBuffer,          // 指向DWORD对齐格式缓冲区的指针，将在其中返回读取结果
     *   DWORD   nBufferLength,     // lpBuffer参数指向的缓冲区大小，以字节为单位
     *   BOOL    bWatchSubtree,     // 是否递归监控目录
     *   DWORD   dwNotifyFilter,    // 需要关注的更改事件
     *   LPDWORD lpBytesReturned,   // 对于同步调用，此参数接收传输到lpBuffer参数中的字节数。对于异步调用，该参数未定义
     *   LPOVERLAPPED lpOverlapped, // 指向OVERLAPPED结构的指针
     *   LPOVERLAPPED_COMPLETION_ROUTINE lpCompletionRoutine
     * )
     */
    // 监听指定的目录，如果有感兴趣的事件到达，会填充到OVERLAPPED结构中的hEvent字段中
    static native void ReadDirectoryChangesW(long hDirectory, long bufferAddress, int bufferLength, boolean watchSubTree, int filter, long bytesReturnedAddress, long pOverlapped) throws WindowsException;
    
    
    // 卷信息
    static class VolumeInformation {
        private String fileSystemName;  // 文件系统
        private String volumeName;      // 卷名
        private int volumeSerialNumber; // 卷序列号
        private int flags;
        
        private VolumeInformation() {
        }
        
        public String fileSystemName() {
            return fileSystemName;
        }
        
        public String volumeName() {
            return volumeName;
        }
        
        public int volumeSerialNumber() {
            return volumeSerialNumber;
        }
        
        public int flags() {
            return flags;
        }
    }
    
    // 指定目录内首个直接子项，通常是"."，即目录自身
    static class FirstFile {
        private long handle;        // 文件引用
        private String name;        // 文件名称
        private int attributes;     // 文件属性
        
        private FirstFile() {
        }
        
        public long handle() {
            return handle;
        }
        
        public String name() {
            return name;
        }
        
        public int attributes() {
            return attributes;
        }
    }
    
    // 备用数据流
    static class FirstStream {
        private long handle;
        private String name;
        
        private FirstStream() {
        }
        
        public long handle() {
            return handle;
        }
        
        public String name() {
            return name;
        }
    }
    
    // 磁盘空间
    static class DiskFreeSpace {
        private long freeBytesAvailable;
        private long totalNumberOfBytes;
        private long totalNumberOfFreeBytes;
        private long bytesPerSector;
        
        private DiskFreeSpace() {
        }
        
        public long freeBytesAvailable() {
            return freeBytesAvailable;
        }
        
        public long totalNumberOfBytes() {
            return totalNumberOfBytes;
        }
        
        public long totalNumberOfFreeBytes() {
            return totalNumberOfFreeBytes;
        }
        
        public long bytesPerSector() {
            return bytesPerSector;
        }
    }
    
    // ACL
    static class AclInformation {
        private int aceCount;
        
        private AclInformation() {
        }
        
        public int aceCount() {
            return aceCount;
        }
    }
    
    // 账户信息
    static class Account {
        private String domain;
        private String name;
        private int use;
        
        private Account() {
        }
        
        public String domain() {
            return domain;
        }
        
        public String name() {
            return name;
        }
        
        public int use() {
            return use;
        }
    }
    
    // 包装GetQueuedCompletionStatus()方法被解除阻塞时收到的数据
    static class CompletionStatus {
        private int error;              // Iocp的扩展错误信息，如超时、断网等
        private int bytesTransferred;   // 写入或读取重叠结构的字节数
        private long completionKey;     // 完成键编号
        
        private CompletionStatus() {
        }
        
        int error() {
            return error;
        }
        
        int bytesTransferred() {
            return bytesTransferred;
        }
        
        long completionKey() {
            return completionKey;
        }
    }
    
}
