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

import java.io.FilePermission;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.LinkPermission;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import jdk.internal.misc.Unsafe;
import jdk.internal.util.StaticProperty;
import sun.nio.ch.ThreadPool;
import sun.security.util.SecurityConstants;

import static sun.nio.fs.WindowsConstants.DACL_SECURITY_INFORMATION;
import static sun.nio.fs.WindowsConstants.ERROR_ACCESS_DENIED;
import static sun.nio.fs.WindowsConstants.ERROR_ALREADY_EXISTS;
import static sun.nio.fs.WindowsConstants.ERROR_DIR_NOT_EMPTY;
import static sun.nio.fs.WindowsConstants.ERROR_FILE_NOT_FOUND;
import static sun.nio.fs.WindowsConstants.ERROR_INVALID_REPARSE_DATA;
import static sun.nio.fs.WindowsConstants.ERROR_PATH_NOT_FOUND;
import static sun.nio.fs.WindowsConstants.FILE_ALL_ACCESS;
import static sun.nio.fs.WindowsConstants.FILE_EXECUTE;
import static sun.nio.fs.WindowsConstants.FILE_GENERIC_EXECUTE;
import static sun.nio.fs.WindowsConstants.FILE_GENERIC_READ;
import static sun.nio.fs.WindowsConstants.FILE_GENERIC_WRITE;
import static sun.nio.fs.WindowsConstants.FILE_READ_DATA;
import static sun.nio.fs.WindowsConstants.FILE_WRITE_DATA;
import static sun.nio.fs.WindowsConstants.GROUP_SECURITY_INFORMATION;
import static sun.nio.fs.WindowsConstants.OWNER_SECURITY_INFORMATION;
import static sun.nio.fs.WindowsConstants.SYMBOLIC_LINK_FLAG_DIRECTORY;
import static sun.nio.fs.WindowsNativeDispatcher.CloseHandle;
import static sun.nio.fs.WindowsNativeDispatcher.CreateDirectory;
import static sun.nio.fs.WindowsNativeDispatcher.CreateHardLink;
import static sun.nio.fs.WindowsNativeDispatcher.CreateSymbolicLink;
import static sun.nio.fs.WindowsNativeDispatcher.DeleteFile;
import static sun.nio.fs.WindowsNativeDispatcher.RemoveDirectory;
import static sun.nio.fs.WindowsSecurity.checkAccessMask;

// "file"文件系统工厂在windows上的实现
public class WindowsFileSystemProvider extends AbstractFileSystemProvider {
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    // 当前工厂可以提供的windows文件系统对象
    private final WindowsFileSystem theFileSystem;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public WindowsFileSystemProvider() {
        // 获取用户工作目录（如项目根目录）
        String userDir = StaticProperty.userDir();
        
        // 构造一个windows文件系统对象
        theFileSystem = new WindowsFileSystem(this, userDir);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件系统 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 返回与指定的URI匹配的文件系统(仅适用系统内部可用的文件系统)
     * 注：此处要求URI协议为"file"
     */
    @Override
    public final FileSystem getFileSystem(URI uri) {
        checkUri(uri);
        return theFileSystem;
    }
    
    /*
     * 返回与指定的URI匹配的文件系统，env是目标文件系统工厂用到的属性
     * 注1：此处要求URI协议为"file"
     * 注2：目前，"file"文件系统工厂未实现该方法
     */
    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        checkUri(uri);
        throw new FileSystemAlreadyExistsException();
    }
    
    /*▲ 文件系统 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 目录流遍历(非递归) ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回指定实体的目录流，用来搜寻目录内的直接子项（需要自定义过滤器）
    @Override
    public DirectoryStream<Path> newDirectoryStream(Path path, DirectoryStream.Filter<? super Path> filter) throws IOException {
        
        // 必须设置过滤器
        if(filter == null) {
            throw new NullPointerException();
        }
        
        // 将Path转换为WindowsPath
        WindowsPath dir = WindowsPath.toWindowsPath(path);
        // 如果存在安全管理器，则需要检查读权限
        dir.checkRead();
        
        // 构造一个目录流
        return new WindowsDirectoryStream(dir, filter);
    }
    
    /*▲ 目录流遍历(非递归) ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件通道 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 创建/打开一个文件，并返回其关联的非异步文件通道；该方法目前的实现与newFileChannel()一致
     *
     * 注：attrs是文件权限属性，允许为空数组，但不能为null，其实现依平台实现而定：
     * windows  : 要求attrs的name()方法返回"acl:acl"，且value()方法返回List<AclEntry>类型的对象
     * linux/mac: 要求attrs的name()方法返回"posix:permissions"或"unix:permissions"，且value()方法返回Set<PosixFilePermission>类型的对象
     */
    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return newFileChannel(path, options, attrs);
    }
    
    /*
     * 创建/打开一个文件，并返回其关联的非异步文件通道；该方法目前的实现与newByteChannel()一致
     *
     * 注：attrs是文件权限属性，允许为空数组，但不能为null，其实现依平台实现而定：
     * windows  : 要求attrs的name()方法返回"acl:acl"，且value()方法返回List<AclEntry>类型的对象
     * linux/mac: 要求attrs的name()方法返回"posix:permissions"或"unix:permissions"，且value()方法返回Set<PosixFilePermission>类型的对象
     */
    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        // 将Path强制转换为WindowsPath
        WindowsPath file = WindowsPath.toWindowsPath(path);
        
        // 通过指定的文件属性构造windows安全描述符，该方法仅在windows平台使用
        WindowsSecurityDescriptor sd = WindowsSecurityDescriptor.fromAttribute(attrs);
        
        try {
            // 创建/打开一个文件，并返回其关联的非异步文件通道
            return WindowsChannelFactory.newFileChannel(file.getPathForWin32Calls(), file.getPathForPermissionCheck(), options, sd.address());
        } catch(WindowsException x) {
            x.rethrowAsIOException(file);
            return null;
        } finally {
            if(sd != null) {
                sd.release();
            }
        }
    }
    
    /*
     * 创建/打开一个文件，并返回其关联的异步文件通道，工作线程在这个过程中会被启动并阻塞
     *
     * path    : 文件路径
     * options : 文件操作属性
     * executor: 工作线程的线程池
     * attrs   : 文件权限属性，允许为空数组，但不能为null
     *
     * 注：attrs是文件权限属性，允许为空数组，但不能为null，其实现依平台实现而定：
     * windows  : 要求attrs的name()方法返回"acl:acl"，且value()方法返回List<AclEntry>类型的对象
     * linux/mac: 要求attrs的name()方法返回"posix:permissions"或"unix:permissions"，且value()方法返回Set<PosixFilePermission>类型的对象
     */
    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
    
        // 将Path强制转换为WindowsPath
        WindowsPath file = WindowsPath.toWindowsPath(path);
    
        // 将指定的【任务执行框架】包装为异步IO线程池：容量非固定，初始容量为0
        ThreadPool pool = (executor == null) ? null : ThreadPool.wrap(executor, 0);
        
        // 通过指定的文件属性构造windows安全描述符，该方法仅在windows平台使用
        WindowsSecurityDescriptor sd = WindowsSecurityDescriptor.fromAttribute(attrs);
        
        try {
            // 创建/打开一个文件，并返回其关联的异步文件通道，工作线程在这个过程中会被启动并阻塞
            return WindowsChannelFactory.newAsynchronousFileChannel(file.getPathForWin32Calls(), file.getPathForPermissionCheck(), options, sd.address(), pool);
        } catch(WindowsException x) {
            x.rethrowAsIOException(file);
            return null;
        } finally {
            if(sd != null) {
                sd.release();
            }
        }
    
    }
    
    /*▲ 文件通道 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/写内容 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回path处符号链接的路径
    @Override
    public Path readSymbolicLink(Path path) throws IOException {
        // 将Path强制转换为WindowsPath
        WindowsPath link = WindowsPath.toWindowsPath(path);
        
        // 获取link路径所属的windows文件系统
        WindowsFileSystem fs = link.getFileSystem();
        
        // permission check
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            FilePermission perm = new FilePermission(link.getPathForPermissionCheck(), SecurityConstants.FILE_READLINK_ACTION);
            sm.checkPermission(perm);
        }
        
        // 获取link处符号链接的路径
        String target = WindowsLinkSupport.readLink(link);
        
        // 路径工厂，创建windows平台的路径对象(不会做本地化操作)
        return WindowsPath.createFromNormalizedPath(fs, target);
    }
    
    /*▲ 读/写内容 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 在指定的路径处创建目录，如果该目录已存在，则抛出异常
     * attrs是创建目录中用到的一些附加参数，不同的文件系统需要不同的参数集
     */
    @Override
    public void createDirectory(Path path, FileAttribute<?>... attrs) throws IOException {
        
        // 将Path强制转换为WindowsPath
        WindowsPath dir = WindowsPath.toWindowsPath(path);
        // 如果存在安全管理器，则需要检查写权限
        dir.checkWrite();
        
        // 通过指定的文件属性构造windows安全描述符，该方法仅在windows平台使用
        WindowsSecurityDescriptor sd = WindowsSecurityDescriptor.fromAttribute(attrs);
        
        try {
            // 创建目录
            CreateDirectory(dir.getPathForWin32Calls(), sd.address());
        } catch(WindowsException x) {
            // convert ERROR_ACCESS_DENIED to FileAlreadyExistsException if we can verify that the directory exists
            if(x.lastError() == ERROR_ACCESS_DENIED) {
                try {
                    if(WindowsFileAttributes.get(dir, false).isDirectory()) {
                        throw new FileAlreadyExistsException(dir.toString());
                    }
                } catch(WindowsException ignore) {
                }
            }
            x.rethrowAsIOException(dir);
        } finally {
            sd.release();
        }
    }
    
    // 创建文件/目录的符号链接；在windows上相当于"mklink linkPath targetPath"(文件的符号链接)或"mklink /D linkPath targetPath"(目录的符号链接)
    @Override
    public void createSymbolicLink(Path linkPath, Path targetPath, FileAttribute<?>... attrs) throws IOException {
        WindowsPath link = WindowsPath.toWindowsPath(linkPath);
        WindowsPath target = WindowsPath.toWindowsPath(targetPath);
        
        // 创建符号链接不允许设置attrs
        if(attrs.length>0) {
            // 通过指定的文件属性构造windows安全描述符，该方法仅在windows平台使用
            WindowsSecurityDescriptor.fromAttribute(attrs);  // may throw NPE or UOE
            throw new UnsupportedOperationException("Initial file attributes not supported when creating symbolic link");
        }
        
        // permission check
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new LinkPermission("symbolic"));
            link.checkWrite();
        }
        
        /*
         * Throw I/O exception for the drive-relative case because Windows creates a link with the resolved target for this case.
         */
        // 如果target是磁盘相对路径，如：C:foo，直接抛异常
        if(target.type() == WindowsPathType.DRIVE_RELATIVE) {
            throw new IOException("Cannot create symbolic link to working directory relative target");
        }
        
        /*
         * Windows treats symbolic links to directories differently than it does to other file types.
         * For that reason we need to check if the target is a directory (or a directory junction).
         */
        WindowsPath resolvedTarget;
        
        // 如果target是相对路径，如：foo，获取target相对于link的父路径的绝对路径
        if(target.type() == WindowsPathType.RELATIVE) {
            WindowsPath parent = link.getParent();
            resolvedTarget = (parent == null) ? target : parent.resolve(target);
        } else {
            resolvedTarget = link.resolve(target);
        }
        
        int flags = 0;
        try {
            // 获取resolvedTarget处文件/目录的属性信息(对于符号链接，不会链接到目标文件)
            WindowsFileAttributes wattrs = WindowsFileAttributes.get(resolvedTarget, false);
            
            // 如果resolvedTarget是目录(不管是不是符号链接)
            if(wattrs.isDirectory() || wattrs.isDirectoryLink()) {
                // 需要创建目录的符号链接
                flags |= SYMBOLIC_LINK_FLAG_DIRECTORY;
            }
        } catch(WindowsException x) {
            // unable to access target so assume target is not a directory
        }
        
        // create the link
        try {
            // 创建符号链接
            CreateSymbolicLink(link.getPathForWin32Calls(), WindowsPath.addPrefixIfNeeded(target.toString()), flags);
        } catch(WindowsException x) {
            if(x.lastError() == ERROR_INVALID_REPARSE_DATA) {
                x.rethrowAsIOException(link, target);
            } else {
                x.rethrowAsIOException(link);
            }
        }
    }
    
    // 创建文件的硬链接；在windows上相当于"mklink /H linkPath targetPath"
    @Override
    public void createLink(Path linkPath, Path targetPath) throws IOException {
        WindowsPath link = WindowsPath.toWindowsPath(linkPath);
        WindowsPath existing = WindowsPath.toWindowsPath(targetPath);
        
        // permission check
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new LinkPermission("hard"));
            link.checkWrite();
            existing.checkWrite();
        }
        
        // create hard link
        try {
            CreateHardLink(link.getPathForWin32Calls(), existing.getPathForWin32Calls());
        } catch(WindowsException x) {
            x.rethrowAsIOException(link, existing);
        }
    }
    
    /*▲ 创建 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 删除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 删除path处的文件/目录；failIfNotExists指示待删除目标不存在时，是否抛异常；如果待删除目标是非空的目录，则直接抛异常
    @Override
    boolean implDelete(Path path, boolean failIfNotExists) throws IOException {
        
        // 将Path强制转换为WindowsPath
        WindowsPath file = WindowsPath.toWindowsPath(path);
        // 如果存在安全管理器，则需要检查删除权限
        file.checkDelete();
        
        WindowsFileAttributes attrs = null;
        try {
            // 获取file的属性信息(对于符号链接，不会将其链接到目标文件)
            attrs = WindowsFileAttributes.get(file, false);
            
            // 如果file是目录(不管是不是符号链接)
            if(attrs.isDirectory() || attrs.isDirectoryLink()) {
                RemoveDirectory(file.getPathForWin32Calls());
                
                // 否则，删除文件
            } else {
                DeleteFile(file.getPathForWin32Calls());
            }
            
            return true;
        } catch(WindowsException x) {
            // no-op if file does not exist
            if(!failIfNotExists && (x.lastError() == ERROR_FILE_NOT_FOUND || x.lastError() == ERROR_PATH_NOT_FOUND)) {
                return false;
            }
            
            if(attrs != null && attrs.isDirectory()) {
                // ERROR_ALREADY_EXISTS is returned when attempting to delete non-empty directory on SAMBA servers
                if(x.lastError() == ERROR_DIR_NOT_EMPTY || x.lastError() == ERROR_ALREADY_EXISTS) {
                    throw new DirectoryNotEmptyException(file.getPathForExceptionMessage());
                }
            }
            
            x.rethrowAsIOException(file);
            
            return false;
        }
    }
    
    /*▲ 删除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 复制/剪切 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 将source处的文件/目录复制到target处，默认不允许覆盖，
     * 如果源目录不为空，则不会复制其子项，
     * 复制成功后，复制源依然保留。
     *
     * options是复制参数，可设置的值包括：
     * - StandardCopyOption.REPLACE_EXISTING
     * - StandardCopyOption.COPY_ATTRIBUTES
     * - LinkOption.NOFOLLOW_LINKS
     * - ExtendedCopyOption.INTERRUPTIBLE
     */
    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        WindowsFileCopy.copy(WindowsPath.toWindowsPath(source), WindowsPath.toWindowsPath(target), options);
    }
    
    /*
     * 将source处的文件/目录移动到target处，默认不允许覆盖，
     * 如果源目录不为空，且在不同的磁盘间复制，则不会复制其子项，
     * 复制成功后，复制源会被删除。
     *
     * options是移动参数，可设置的值包括：
     * - StandardCopyOption.REPLACE_EXISTING
     * - StandardCopyOption.ATOMIC_MOVE
     * - LinkOption.NOFOLLOW_LINKS(忽略)
     *
     * 注：如果移动发生在同一个目录下，则可以看做是重命名操作
     */
    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        WindowsFileCopy.move(WindowsPath.toWindowsPath(source), WindowsPath.toWindowsPath(target), options);
    }
    
    /*▲ 复制/剪切 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件存储 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回path处文件关联的文件存储
    @Override
    public FileStore getFileStore(Path path) throws IOException {
        
        // 将Path强制转换为WindowsPath
        WindowsPath file = WindowsPath.toWindowsPath(path);
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("getFileStoreAttributes"));
            file.checkRead();
        }
        
        // 创建file文件的windows文件存储对象
        return WindowsFileStore.create(file);
    }
    
    /*▲ 文件存储 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件属性(视图) ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 返回path处文件的文件属性视图，该视图的类型由name决定，
     * 此处name允许的取值为："basic"、"dos"、"user"、"owner"、"acl"，
     * options指示对于符号链接，是否将其链接到目标文件。
     */
    @Override
    public DynamicFileAttributeView getFileAttributeView(Path path, String name, LinkOption... options) {
        
        // 将Path转换为WindowsPath
        WindowsPath file = WindowsPath.toWindowsPath(path);
        
        boolean followLinks = Util.followLinks(options);
        
        if(name.equals("basic")) {
            return WindowsFileAttributeViews.createBasicView(file, followLinks);
        }
        
        if(name.equals("dos")) {
            return WindowsFileAttributeViews.createDosView(file, followLinks);
        }
        
        if(name.equals("user")) {
            return new WindowsUserDefinedFileAttributeView(file, followLinks);
        }
        
        if(name.equals("owner")) {
            return new FileOwnerAttributeViewImpl(new WindowsAclFileAttributeView(file, followLinks));
        }
        
        if(name.equals("acl")) {
            return new WindowsAclFileAttributeView(file, followLinks);
        }
        
        return null;
    }
    
    /*
     * 获取指定路径标识的文件的基础文件属性
     *
     * type   ：文件属性类型，此处仅支持基础文件属性(BasicFileAttributes)和DOS文件属性(DosFileAttributes)
     * options：对于符号链接，是否将其链接到目标文件
     */
    @Override
    @SuppressWarnings("unchecked")
    public <A extends BasicFileAttributes> A readAttributes(Path file, Class<A> type, LinkOption... options) throws IOException {
        Class<? extends BasicFileAttributeView> view;
        
        if(type == BasicFileAttributes.class) {
            view = BasicFileAttributeView.class;
        } else if(type == DosFileAttributes.class) {
            view = DosFileAttributeView.class;
        } else if(type == null) {
            throw new NullPointerException();
        } else {
            throw new UnsupportedOperationException();
        }
        
        // 获取file文件的文件属性视图
        BasicFileAttributeView attributeView = getFileAttributeView(file, view, options);
        
        // 读取基础的文件属性
        return (A) attributeView.readAttributes();
    }
    
    /*
     * 返回path处文件的文件属性视图
     *
     * type   : 文件属性类型，不同的平台上有不同的实现
     * options: 对于符号链接，是否将其链接到目标文件
     */
    @Override
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> view, LinkOption... options) {
        if(view == null) {
            throw new NullPointerException();
        }
        
        // 将Path转换为WindowsPath
        WindowsPath file = WindowsPath.toWindowsPath(path);
        
        // 对于符号链接，是否将其链接到目标文件
        boolean followLinks = Util.followLinks(options);
        
        if(view == UserDefinedFileAttributeView.class) {
            return (V) new WindowsUserDefinedFileAttributeView(file, followLinks);
        }
        
        if(view == BasicFileAttributeView.class) {
            return (V) WindowsFileAttributeViews.createBasicView(file, followLinks);
        }
        
        if(view == DosFileAttributeView.class) {
            return (V) WindowsFileAttributeViews.createDosView(file, followLinks);
        }
        
        if(view == FileOwnerAttributeView.class) {
            return (V) new FileOwnerAttributeViewImpl(new WindowsAclFileAttributeView(file, followLinks));
        }
        
        if(view == AclFileAttributeView.class) {
            return (V) new WindowsAclFileAttributeView(file, followLinks);
        }
        
        return null;
    }
    
    /*▲ 文件属性(视图) ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回当前文件系统支持的协议，此处支持"file"协议
    @Override
    public String getScheme() {
        return "file";
    }
    
    // 从指定的uri中解析出一个有效路径，此处要求改uri的协议为"file"
    @Override
    public Path getPath(URI uri) {
        return WindowsUriSupport.fromUri(theFileSystem, uri);
    }
    
    // 判断两个路径(文件)是否相同
    @Override
    public boolean isSameFile(Path path1, Path path2) throws IOException {
        WindowsPath file1 = WindowsPath.toWindowsPath(path1);
        
        if(file1.equals(path2)) {
            return true;
        }
        
        if(path2 == null) {
            throw new NullPointerException();
        }
        
        if(!(path2 instanceof WindowsPath)) {
            return false;
        }
        
        WindowsPath file2 = (WindowsPath) path2;
        
        // check security manager access to both files
        file1.checkRead();
        file2.checkRead();
        
        // open both files and see if they are the same
        long h1 = 0L;
        try {
            h1 = file1.openForReadAttributeAccess(true);
        } catch(WindowsException x) {
            x.rethrowAsIOException(file1);
        }
        try {
            WindowsFileAttributes attrs1 = null;
            try {
                // 返回h1文件的windows文件属性信息
                attrs1 = WindowsFileAttributes.readAttributes(h1);
            } catch(WindowsException x) {
                x.rethrowAsIOException(file1);
            }
            
            long h2 = 0L;
            try {
                h2 = file2.openForReadAttributeAccess(true);
            } catch(WindowsException x) {
                x.rethrowAsIOException(file2);
            }
            
            try {
                WindowsFileAttributes attrs2 = null;
                try {
                    // 返回h2文件的windows文件属性信息
                    attrs2 = WindowsFileAttributes.readAttributes(h2);
                } catch(WindowsException x) {
                    x.rethrowAsIOException(file2);
                }
                
                return WindowsFileAttributes.isSameFile(attrs1, attrs2);
            } finally {
                CloseHandle(h2);
            }
        } finally {
            CloseHandle(h1);
        }
    }
    
    // 判断obj处的文件是否为隐藏文件
    @Override
    public boolean isHidden(Path path) throws IOException {
        // 将Path转换为WindowsPath
        WindowsPath file = WindowsPath.toWindowsPath(path);
        
        file.checkRead();
        
        WindowsFileAttributes attrs = null;
        
        try {
            attrs = WindowsFileAttributes.get(file, true);
        } catch(WindowsException x) {
            x.rethrowAsIOException(file);
        }
        
        // DOS hidden attribute not meaningful when set on directories
        if(attrs.isDirectory()) {
            return false;
        }
        
        return attrs.isHidden();
    }
    
    // 判断是否可以对path处的文件(需存在)应用指定的访问模式；没有指定访问模式的话，默认检查文件是否可读
    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        WindowsPath file = WindowsPath.toWindowsPath(path);
        
        boolean r = false;
        boolean w = false;
        boolean x = false;
        for(AccessMode mode : modes) {
            switch(mode) {
                case READ:
                    r = true;
                    break;
                case WRITE:
                    w = true;
                    break;
                case EXECUTE:
                    x = true;
                    break;
                default:
                    throw new AssertionError("Should not get here");
            }
        }
        
        // special-case read access to avoid needing to determine effective access to file; default if modes not specified
        if(!w && !x) {
            // 判断给定的文件是否存在且可读
            checkReadAccess(file);
            return;
        }
        
        int mask = 0;
        
        if(r) {
            file.checkRead();
            mask |= FILE_READ_DATA;
        }
        
        if(w) {
            file.checkWrite();
            mask |= FILE_WRITE_DATA;
        }
        
        if(x) {
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                sm.checkExec(file.getPathForPermissionCheck());
            }
            mask |= FILE_EXECUTE;
        }
        
        if(!hasDesiredAccess(file, mask)) {
            throw new AccessDeniedException(file.getPathForExceptionMessage(), null, "Permissions does not allow requested access");
        }
        
        // for write access we need to check if the DOS readonly attribute and if the volume is read-only
        if(w) {
            try {
                WindowsFileAttributes attrs = WindowsFileAttributes.get(file, true);
                if(!attrs.isDirectory() && attrs.isReadOnly()) {
                    throw new AccessDeniedException(file.getPathForExceptionMessage(), null, "DOS readonly attribute is set");
                }
            } catch(WindowsException exc) {
                exc.rethrowAsIOException(file);
            }
            
            // 创建file文件的windows文件存储对象
            WindowsFileStore fileStore = WindowsFileStore.create(file);
            
            if(fileStore.isReadOnly()) {
                throw new AccessDeniedException(file.getPathForExceptionMessage(), null, "Read-only file system");
            }
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 检查给定的URI是否合规
    private void checkUri(URI uri) {
        if(!uri.getScheme().equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("URI does not match this provider");
        }
        
        if(uri.getRawAuthority() != null) {
            throw new IllegalArgumentException("Authority component present");
        }
        
        String path = uri.getPath();
        
        if(path == null) {
            throw new IllegalArgumentException("Path component is undefined");
        }
        
        if(!path.equals("/")) {
            throw new IllegalArgumentException("Path component should be '/'");
        }
        
        if(uri.getRawQuery() != null) {
            throw new IllegalArgumentException("Query component present");
        }
        
        if(uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Fragment component present");
        }
    }
    
    /**
     * Checks if the given file(or directory) exists and is readable.
     */
    // 判断给定的文件是否存在且可读
    private void checkReadAccess(WindowsPath file) throws IOException {
        try {
            Set<OpenOption> opts = Collections.emptySet();
            // 创建/打开一个文件，并返回其关联的非异步文件通道；这里没有设置操作参数，因而会使用"读"模式操作文件，相当于间接检测了可读性
            FileChannel fc = WindowsChannelFactory.newFileChannel(file.getPathForWin32Calls(), file.getPathForPermissionCheck(), opts, 0L);
            fc.close();
        } catch(WindowsException exc) {
            /*
             * Windows errors are very inconsistent when the file is a directory
             * (ERROR_PATH_NOT_FOUND returned for root directories for example)
             * so we retry by attempting to open it as a directory.
             */
            try {
                new WindowsDirectoryStream(file, null).close();
            } catch(IOException ioe) {
                // translate and throw original exception
                exc.rethrowAsIOException(file);
            }
        }
    }
    
    /**
     * Checks the file security against desired access.
     */
    private static boolean hasDesiredAccess(WindowsPath file, int rights) throws IOException {
        // read security descriptor containing ACL (symlinks are followed)
        boolean hasRights = false;
        String target = WindowsLinkSupport.getFinalPath(file, true);
        NativeBuffer aclBuffer = WindowsAclFileAttributeView.getFileSecurity(target, DACL_SECURITY_INFORMATION | OWNER_SECURITY_INFORMATION | GROUP_SECURITY_INFORMATION);
        
        try {
            hasRights = checkAccessMask(aclBuffer.address(), rights, FILE_GENERIC_READ, FILE_GENERIC_WRITE, FILE_GENERIC_EXECUTE, FILE_ALL_ACCESS);
        } catch(WindowsException exc) {
            exc.rethrowAsIOException(file);
        } finally {
            aclBuffer.release();
        }
        
        return hasRights;
    }
    
}
