/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.LinkPermission;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import sun.nio.ch.FileChannelImpl;

/**
 * Service-provider class for file systems. The methods defined by the {@link
 * java.nio.file.Files} class will typically delegate to an instance of this
 * class.
 *
 * <p> A file system provider is a concrete implementation of this class that
 * implements the abstract methods defined by this class. A provider is
 * identified by a {@code URI} {@link #getScheme() scheme}. The default provider
 * is identified by the URI scheme "file". It creates the {@link FileSystem} that
 * provides access to the file systems accessible to the Java virtual machine.
 * The {@link FileSystems} class defines how file system providers are located
 * and loaded. The default provider is typically a system-default provider but
 * may be overridden if the system property {@code
 * java.nio.file.spi.DefaultFileSystemProvider} is set. In that case, the
 * provider has a one argument constructor whose formal parameter type is {@code
 * FileSystemProvider}. All other providers have a zero argument constructor
 * that initializes the provider.
 *
 * <p> A provider is a factory for one or more {@link FileSystem} instances. Each
 * file system is identified by a {@code URI} where the URI's scheme matches
 * the provider's {@link #getScheme scheme}. The default file system, for example,
 * is identified by the URI {@code "file:///"}. A memory-based file system,
 * for example, may be identified by a URI such as {@code "memory:///?name=logfs"}.
 * The {@link #newFileSystem newFileSystem} method may be used to create a file
 * system, and the {@link #getFileSystem getFileSystem} method may be used to
 * obtain a reference to an existing file system created by the provider. Where
 * a provider is the factory for a single file system then it is provider dependent
 * if the file system is created when the provider is initialized, or later when
 * the {@code newFileSystem} method is invoked. In the case of the default
 * provider, the {@code FileSystem} is created when the provider is initialized.
 *
 * <p> All of the methods in this class are safe for use by multiple concurrent
 * threads.
 *
 * @since 1.7
 */
// 文件系统工厂(服务)，系统默认支持"file"/"jar"/"jrt"这三类文件系统工厂
public abstract class FileSystemProvider {
    
    /** lock using when loading providers */
    private static final Object lock = new Object();
    
    // 文件的默认打开选项
    private static final Set<OpenOption> DEFAULT_OPEN_OPTIONS = Set.of(StandardOpenOption.CREATE,            // 如果目标文件存在，则打开文件；否则新建文件
        StandardOpenOption.TRUNCATE_EXISTING, // 如果目标文件存在，将其长度截断为0；否则操作失败
        StandardOpenOption.WRITE              // 写文件，指示向文件中写入数据 , 并且移动文件指针
    );
    
    // 缓存已注册的文件系统工厂
    private static volatile List<FileSystemProvider> installedProviders;
    
    /** used to avoid recursive loading of installed providers */
    // 是否已经加载过文件系统工厂
    private static boolean loadingProviders = false;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class.
     *
     * <p> During construction a provider may safely access files associated
     * with the default provider but care needs to be taken to avoid circular
     * loading of other installed providers. If circular loading of installed
     * providers is detected then an unspecified error is thrown.
     *
     * @throws SecurityException If a security manager has been installed and it denies
     *                           {@link RuntimePermission}{@code ("fileSystemProvider")}
     */
    protected FileSystemProvider() {
        this(checkPermission());
    }
    
    private FileSystemProvider(Void ignore) {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 目录流遍历(非递归) ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens a directory, returning a {@code DirectoryStream} to iterate over
     * the entries in the directory. This method works in exactly the manner
     * specified by the {@link
     * Files#newDirectoryStream(java.nio.file.Path, java.nio.file.DirectoryStream.Filter)}
     * method.
     *
     * @param dir    the path to the directory
     * @param filter the directory stream filter
     *
     * @return a new and open {@code DirectoryStream} object
     *
     * @throws NotDirectoryException if the file could not otherwise be opened because it is not
     *                               a directory <i>(optional specific exception)</i>
     * @throws IOException           if an I/O error occurs
     * @throws SecurityException     In the case of the default provider, and a security manager is
     *                               installed, the {@link SecurityManager#checkRead(String) checkRead}
     *                               method is invoked to check read access to the directory.
     */
    // 返回指定实体的目录流，用来搜寻目录内的直接子项（需要自定义过滤器）
    public abstract DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException;
    
    /*▲ 目录流遍历(非递归) ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节流 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens a file, returning an input stream to read from the file. This
     * method works in exactly the manner specified by the {@link
     * Files#newInputStream} method.
     *
     * <p> The default implementation of this method opens a channel to the file
     * as if by invoking the {@link #newByteChannel} method and constructs a
     * stream that reads bytes from the channel. This method should be overridden
     * where appropriate.
     *
     * @param path    the path to the file to open
     * @param options options specifying how the file is opened
     *
     * @return a new input stream
     *
     * @throws IllegalArgumentException      if an invalid combination of options is specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager is
     *                                       installed, the {@link SecurityManager#checkRead(String) checkRead}
     *                                       method is invoked to check read access to the file.
     */
    // 返回path处文件的输入流，以便从中读取数据
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        // 存在预设参数时，应当确保该参数不是APPEND和WRITE
        if(options.length>0) {
            for(OpenOption opt : options) {
                // All OpenOption values except for APPEND and WRITE are allowed
                if(opt == StandardOpenOption.APPEND || opt == StandardOpenOption.WRITE) {
                    throw new UnsupportedOperationException("'" + opt + "' not allowed");
                }
            }
        }
        
        // 创建/打开一个文件，并返回其关联的非异步文件通道
        ReadableByteChannel rbc = Files.newByteChannel(path, options);
        // 如果是文件通道，设置该通道为忽略中断
        if(rbc instanceof FileChannelImpl) {
            ((FileChannelImpl) rbc).setUninterruptible();
        }
        
        // 返回一个可读通道的输入流，允许从指定的通道中读取数据
        return Channels.newInputStream(rbc);
    }
    
    /**
     * Opens or creates a file, returning an output stream that may be used to
     * write bytes to the file. This method works in exactly the manner
     * specified by the {@link Files#newOutputStream} method.
     *
     * <p> The default implementation of this method opens a channel to the file
     * as if by invoking the {@link #newByteChannel} method and constructs a
     * stream that writes bytes to the channel. This method should be overridden
     * where appropriate.
     *
     * @param path    the path to the file to open or create
     * @param options options specifying how the file is opened
     *
     * @return a new output stream
     *
     * @throws IllegalArgumentException      if {@code options} contains an invalid combination of options
     * @throws UnsupportedOperationException if an unsupported option is specified
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager is
     *                                       installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *                                       method is invoked to check write access to the file. The {@link
     *                                       SecurityManager#checkDelete(String) checkDelete} method is
     *                                       invoked to check delete access if the file is opened with the
     *                                       {@code DELETE_ON_CLOSE} option.
     */
    // 返回path处文件的输出流，以便向其写入数据
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        int len = options.length;
        
        Set<OpenOption> opts;
        
        if(len == 0) {
            opts = DEFAULT_OPEN_OPTIONS;
        } else {
            opts = new HashSet<>();
            
            // 存在预设参数时，应当确保该参数不是READ
            for(OpenOption opt : options) {
                if(opt == StandardOpenOption.READ) {
                    throw new IllegalArgumentException("READ not allowed");
                }
                opts.add(opt);
            }
            opts.add(StandardOpenOption.WRITE);
        }
        
        // 创建/打开一个文件，并返回其关联的非异步文件通道
        WritableByteChannel wbc = newByteChannel(path, opts);
        // 如果是文件通道，设置该通道为忽略中断
        if(wbc instanceof FileChannelImpl) {
            ((FileChannelImpl) wbc).setUninterruptible();
        }
        
        // 返回一个可写通道的输出流，允许向指定的通道中写入数据
        return Channels.newOutputStream(wbc);
    }
    
    /*▲ 字节流 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件通道 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Opens or creates a file, returning a seekable byte channel to access the
     * file. This method works in exactly the manner specified by the {@link
     * Files#newByteChannel(Path, Set, FileAttribute[])} method.
     *
     * @param path    the path to the file to open or create
     * @param options options specifying how the file is opened
     * @param attrs   an optional list of file attributes to set atomically when creating the file
     *
     * @return a new seekable byte channel
     *
     * @throws IllegalArgumentException      if the set contains an invalid combination of options
     * @throws UnsupportedOperationException if an unsupported open option is specified or the array contains
     *                                       attributes that cannot be set atomically when creating the file
     * @throws FileAlreadyExistsException    if a file of that name already exists and the {@link
     *                                       StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *                                       <i>(optional specific exception)</i>
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager is
     *                                       installed, the {@link SecurityManager#checkRead(String) checkRead}
     *                                       method is invoked to check read access to the path if the file is
     *                                       opened for reading. The {@link SecurityManager#checkWrite(String)
     *                                       checkWrite} method is invoked to check write access to the path
     *                                       if the file is opened for writing. The {@link
     *                                       SecurityManager#checkDelete(String) checkDelete} method is
     *                                       invoked to check delete access if the file is opened with the
     *                                       {@code DELETE_ON_CLOSE} option.
     */
    /*
     * 创建/打开一个文件，并返回其关联的非异步文件通道；该方法目前的实现与newFileChannel()一致
     *
     * 注：attrs是文件权限属性，允许为空数组，但不能为null，其实现依平台实现而定：
     * windows  : 要求attrs的name()方法返回"acl:acl"，且value()方法返回List<AclEntry>类型的对象
     * linux/mac: 要求attrs的name()方法返回"posix:permissions"或"unix:permissions"，且value()方法返回Set<PosixFilePermission>类型的对象
     */
    public abstract SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException;
    
    /**
     * Opens or creates a file for reading and/or writing, returning a file
     * channel to access the file. This method works in exactly the manner
     * specified by the {@link FileChannel#open(Path, Set, FileAttribute[])
     * FileChannel.open} method. A provider that does not support all the
     * features required to construct a file channel throws {@code
     * UnsupportedOperationException}. The default provider is required to
     * support the creation of file channels. When not overridden, the default
     * implementation throws {@code UnsupportedOperationException}.
     *
     * @param path    the path of the file to open or create
     * @param options options specifying how the file is opened
     * @param attrs   an optional list of file attributes to set atomically when
     *                creating the file
     *
     * @return a new file channel
     *
     * @throws IllegalArgumentException      If the set contains an invalid combination of options
     * @throws UnsupportedOperationException If this provider that does not support creating file channels,
     *                                       or an unsupported open option or file attribute is specified
     * @throws IOException                   If an I/O error occurs
     * @throws SecurityException             In the case of the default file system, the {@link
     *                                       SecurityManager#checkRead(String)} method is invoked to check
     *                                       read access if the file is opened for reading. The {@link
     *                                       SecurityManager#checkWrite(String)} method is invoked to check
     *                                       write access if the file is opened for writing
     */
    /*
     * 创建/打开一个文件，并返回其关联的非异步文件通道；该方法目前的实现与newByteChannel()一致
     *
     * 注：attrs是文件权限属性，允许为空数组，但不能为null，其实现依平台实现而定：
     * windows  : 要求attrs的name()方法返回"acl:acl"，且value()方法返回List<AclEntry>类型的对象
     * linux/mac: 要求attrs的name()方法返回"posix:permissions"或"unix:permissions"，且value()方法返回Set<PosixFilePermission>类型的对象
     */
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Opens or creates a file for reading and/or writing, returning an
     * asynchronous file channel to access the file. This method works in
     * exactly the manner specified by the {@link
     * AsynchronousFileChannel#open(Path, Set, ExecutorService, FileAttribute[])
     * AsynchronousFileChannel.open} method.
     * A provider that does not support all the features required to construct
     * an asynchronous file channel throws {@code UnsupportedOperationException}.
     * The default provider is required to support the creation of asynchronous
     * file channels. When not overridden, the default implementation of this
     * method throws {@code UnsupportedOperationException}.
     *
     * @param path     the path of the file to open or create
     * @param options  options specifying how the file is opened
     * @param executor the thread pool or {@code null} to associate the channel with
     *                 the default thread pool
     * @param attrs    an optional list of file attributes to set atomically when
     *                 creating the file
     *
     * @return a new asynchronous file channel
     *
     * @throws IllegalArgumentException      If the set contains an invalid combination of options
     * @throws UnsupportedOperationException If this provider that does not support creating asynchronous file
     *                                       channels, or an unsupported open option or file attribute is
     *                                       specified
     * @throws IOException                   If an I/O error occurs
     * @throws SecurityException             In the case of the default file system, the {@link
     *                                       SecurityManager#checkRead(String)} method is invoked to check
     *                                       read access if the file is opened for reading. The {@link
     *                                       SecurityManager#checkWrite(String)} method is invoked to check
     *                                       write access if the file is opened for writing
     */
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
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    /*▲ 文件通道 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new directory. This method works in exactly the manner
     * specified by the {@link Files#createDirectory} method.
     *
     * @param dir   the directory to create
     * @param attrs an optional list of file attributes to set atomically when
     *              creating the directory
     *
     * @throws UnsupportedOperationException if the array contains an attribute that cannot be set atomically
     *                                       when creating the directory
     * @throws FileAlreadyExistsException    if a directory could not otherwise be created because a file of
     *                                       that name already exists <i>(optional specific exception)</i>
     * @throws IOException                   if an I/O error occurs or the parent directory does not exist
     * @throws SecurityException             In the case of the default provider, and a security manager is
     *                                       installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *                                       method is invoked to check write access to the new directory.
     */
    /*
     * 在指定的路径处创建目录，如果该目录已存在，则抛出异常
     * attrs是创建目录中用到的一些附加参数，不同的文件系统需要不同的参数集
     */
    public abstract void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException;
    
    /**
     * Creates a symbolic link to a target. This method works in exactly the
     * manner specified by the {@link Files#createSymbolicLink} method.
     *
     * <p> The default implementation of this method throws {@code
     * UnsupportedOperationException}.
     *
     * @param link   the path of the symbolic link to create
     * @param target the target of the symbolic link
     * @param attrs  the array of attributes to set atomically when creating the symbolic link
     *
     * @throws UnsupportedOperationException if the implementation does not support symbolic links or the
     *                                       array contains an attribute that cannot be set atomically when
     *                                       creating the symbolic link
     * @throws FileAlreadyExistsException    if a file with the name already exists <i>(optional specific
     *                                       exception)</i>
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager
     *                                       is installed, it denies {@link LinkPermission}{@code ("symbolic")}
     *                                       or its {@link SecurityManager#checkWrite(String) checkWrite}
     *                                       method denies write access to the path of the symbolic link.
     */
    // 创建文件/目录的符号链接；在windows上相当于"mklink link target"(文件的符号链接)或"mklink /D link target"(目录的符号链接)
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Creates a new link (directory entry) for an existing file. This method
     * works in exactly the manner specified by the {@link Files#createLink}
     * method.
     *
     * <p> The default implementation of this method throws {@code
     * UnsupportedOperationException}.
     *
     * @param link   the link (directory entry) to create
     * @param target a path to an existing file
     *
     * @throws UnsupportedOperationException if the implementation does not support adding an existing file
     *                                       to a directory
     * @throws FileAlreadyExistsException    if the entry could not otherwise be created because a file of
     *                                       that name already exists <i>(optional specific exception)</i>
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager
     *                                       is installed, it denies {@link LinkPermission}{@code ("hard")}
     *                                       or its {@link SecurityManager#checkWrite(String) checkWrite}
     *                                       method denies write access to either the  link or the
     *                                       existing file.
     */
    // 创建文件的硬链接；在windows上相当于"mklink /H link target"
    public void createLink(Path link, Path target) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    /*▲ 创建 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 删除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Deletes a file. This method works in exactly the  manner specified by the
     * {@link Files#delete} method.
     *
     * @param path the path to the file to delete
     *
     * @throws NoSuchFileException        if the file does not exist <i>(optional specific exception)</i>
     * @throws DirectoryNotEmptyException if the file is a directory and could not otherwise be deleted
     *                                    because the directory is not empty <i>(optional specific
     *                                    exception)</i>
     * @throws IOException                if an I/O error occurs
     * @throws SecurityException          In the case of the default provider, and a security manager is
     *                                    installed, the {@link SecurityManager#checkDelete(String)} method
     *                                    is invoked to check delete access to the file
     */
    // 删除path处的文件/目录；如果待删除目标不存在，或者待删除目标是非空的目录，则直接抛异常
    public abstract void delete(Path path) throws IOException;
    
    /**
     * Deletes a file if it exists. This method works in exactly the manner
     * specified by the {@link Files#deleteIfExists} method.
     *
     * <p> The default implementation of this method simply invokes {@link
     * #delete} ignoring the {@code NoSuchFileException} when the file does not
     * exist. It may be overridden where appropriate.
     *
     * @param path the path to the file to delete
     *
     * @return {@code true} if the file was deleted by this method; {@code
     * false} if the file could not be deleted because it did not
     * exist
     *
     * @throws DirectoryNotEmptyException if the file is a directory and could not otherwise be deleted
     *                                    because the directory is not empty <i>(optional specific
     *                                    exception)</i>
     * @throws IOException                if an I/O error occurs
     * @throws SecurityException          In the case of the default provider, and a security manager is
     *                                    installed, the {@link SecurityManager#checkDelete(String)} method
     *                                    is invoked to check delete access to the file
     */
    // 删除path处的文件/目录；如果待删除目标是非空的目录，则直接抛异常；但如果待删除目标不存在，不会抛异常
    public boolean deleteIfExists(Path path) throws IOException {
        try {
            delete(path);
            return true;
        } catch(NoSuchFileException ignore) {
            return false;
        }
    }
    
    /*▲ 删除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 复制/剪切 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Copy a file to a target file. This method works in exactly the manner
     * specified by the {@link Files#copy(Path, Path, CopyOption[])} method
     * except that both the source and target paths must be associated with
     * this provider.
     *
     * @param source  the path to the file to copy
     * @param target  the path to the target file
     * @param options options specifying how the copy should be done
     *
     * @throws UnsupportedOperationException if the array contains a copy option that is not supported
     * @throws FileAlreadyExistsException    if the target file exists but cannot be replaced because the
     *                                       {@code REPLACE_EXISTING} option is not specified <i>(optional
     *                                       specific exception)</i>
     * @throws DirectoryNotEmptyException    the {@code REPLACE_EXISTING} option is specified but the file
     *                                       cannot be replaced because it is a non-empty directory
     *                                       <i>(optional specific exception)</i>
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager is
     *                                       installed, the {@link SecurityManager#checkRead(String) checkRead}
     *                                       method is invoked to check read access to the source file, the
     *                                       {@link SecurityManager#checkWrite(String) checkWrite} is invoked
     *                                       to check write access to the target file. If a symbolic link is
     *                                       copied the security manager is invoked to check {@link
     *                                       LinkPermission}{@code ("symbolic")}.
     */
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
    public abstract void copy(Path source, Path target, CopyOption... options) throws IOException;
    
    /**
     * Move or rename a file to a target file. This method works in exactly the
     * manner specified by the {@link Files#move} method except that both the
     * source and target paths must be associated with this provider.
     *
     * @param source  the path to the file to move
     * @param target  the path to the target file
     * @param options options specifying how the move should be done
     *
     * @throws UnsupportedOperationException   if the array contains a copy option that is not supported
     * @throws FileAlreadyExistsException      if the target file exists but cannot be replaced because the
     *                                         {@code REPLACE_EXISTING} option is not specified <i>(optional
     *                                         specific exception)</i>
     * @throws DirectoryNotEmptyException      the {@code REPLACE_EXISTING} option is specified but the file
     *                                         cannot be replaced because it is a non-empty directory
     *                                         <i>(optional specific exception)</i>
     * @throws AtomicMoveNotSupportedException if the options array contains the {@code ATOMIC_MOVE} option but
     *                                         the file cannot be moved as an atomic file system operation.
     * @throws IOException                     if an I/O error occurs
     * @throws SecurityException               In the case of the default provider, and a security manager is
     *                                         installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *                                         method is invoked to check write access to both the source and
     *                                         target file.
     */
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
    public abstract void move(Path source, Path target, CopyOption... options) throws IOException;
    
    /*▲ 复制/剪切 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件存储 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the {@link FileStore} representing the file store where a file is located.
     * This method works in exactly the manner specified by the {@link Files#getFileStore} method.
     *
     * @param path the path to the file
     *
     * @return the file store where the file is stored
     *
     * @throws IOException       if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is
     *                           installed, the {@link SecurityManager#checkRead(String) checkRead}
     *                           method is invoked to check read access to the file, and in
     *                           addition it checks
     *                           {@link RuntimePermission}{@code ("getFileStoreAttributes")}
     */
    // 返回path处文件关联的文件存储
    public abstract FileStore getFileStore(Path path) throws IOException;
    
    /*▲ 文件存储 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件属性(视图) ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets the value of a file attribute.
     * This method works in exactly the manner specified by the {@link Files#setAttribute} method.
     *
     * @param path      the path to the file
     * @param attribute the attribute to set
     * @param value     the attribute value
     * @param options   options indicating how symbolic links are handled
     *
     * @throws UnsupportedOperationException if the attribute view is not available
     * @throws IllegalArgumentException      if the attribute name is not specified, or is not recognized, or
     *                                       the attribute value is of the correct type but has an
     *                                       inappropriate value
     * @throws ClassCastException            If the attribute value is not of the expected type or is a
     *                                       collection containing elements that are not of the expected
     *                                       type
     * @throws IOException                   If an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager is
     *                                       installed, its {@link SecurityManager#checkWrite(String) checkWrite}
     *                                       method denies write access to the file. If this method is invoked
     *                                       to set security sensitive attributes then the security manager
     *                                       may be invoked to check for additional permissions.
     */
    /*
     * 为path处文件设置指定的属性(一条)
     *
     * path     : 等待设置属性的文件的路径
     * attribute: 设置的属性名，其格式为"属性类型:属性名称"，如果未指定属性类型，默认使用"basic"类型；
     *            属性类型的常用取值参见AbstractFileSystemProvider#getFileAttributeView()方法中的name参数；
     *            属性名称的取值参见不同属性视图的实现类。
     * value    : 设置的属性值，不同类型的属性视图拥有不同的可选值，使用时需要参见各属性视图中setAttribute()方法的value参数
     * options  : 指示对于符号链接，是否将其链接到目标文件；如果显式设置了LinkOption.NOFOLLOW_LINKS，表示不链接
     */
    public abstract void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException;
    
    /**
     * Reads a set of file attributes as a bulk operation. This method works in
     * exactly the manner specified by the {@link
     * Files#readAttributes(Path, String, LinkOption[])} method.
     *
     * @param path       the path to the file
     * @param attributes the attributes to read
     * @param options    options indicating how symbolic links are handled
     *
     * @return a map of the attributes returned; may be empty. The map's keys
     * are the attribute names, its values are the attribute values
     *
     * @throws UnsupportedOperationException if the attribute view is not available
     * @throws IllegalArgumentException      if no attributes are specified or an unrecognized attributes is
     *                                       specified
     * @throws IOException                   If an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager is
     *                                       installed, its {@link SecurityManager#checkRead(String) checkRead}
     *                                       method denies read access to the file. If this method is invoked
     *                                       to read security sensitive attributes then the security manager
     *                                       may be invoke to check for additional permissions.
     */
    /*
     * 从path处的文件中获取指定的属性(多条)
     *
     * path     : 等待获取属性的文件的路径
     * attribute: 获取的属性名，其格式为"属性类型:属性名称1,属性名称2,属性名称3..."，如果未指定属性类型，默认使用"basic"类型；
     *            属性类型的常用取值参见AbstractFileSystemProvider#getFileAttributeView()方法中的name参数；
     *            属性名称的取值参见不同属性视图的实现类，使用"*"表示获取指定视图下所有属性。
     * options  : 指示对于符号链接，是否将其链接到目标文件；如果显式设置了LinkOption.NOFOLLOW_LINKS，表示不链接
     */
    public abstract Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException;
    
    /**
     * Reads a file's attributes as a bulk operation. This method works in
     * exactly the manner specified by the {@link
     * Files#readAttributes(Path, Class, LinkOption[])} method.
     *
     * @param <A>     The {@code BasicFileAttributes} type
     * @param path    the path to the file
     * @param type    the {@code Class} of the file attributes required
     *                to read
     * @param options options indicating how symbolic links are handled
     *
     * @return the file attributes
     *
     * @throws UnsupportedOperationException if an attributes of the given type are not supported
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, a security manager is
     *                                       installed, its {@link SecurityManager#checkRead(String) checkRead}
     *                                       method is invoked to check read access to the file
     */
    /*
     * 返回path处文件的文件属性
     *
     * type   : 文件属性类型，不同的平台上有不同的实现。
     *                                     windows  linux  mac
     *          BasicFileAttributes.class     √       √     √
     *          DosFileAttributes.class       √       √
     *          PosixFileAttributes.class             √     √
     *          JrtFileAttributes.class       √       √     √
     *          ZipFileAttributes.class       √       √     √
     * options: 对于符号链接，是否将其链接到目标文件；如果显式设置了LinkOption.NOFOLLOW_LINKS，表示不链接
     */
    public abstract <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException;
    
    /**
     * Returns a file attribute view of a given type. This method works in
     * exactly the manner specified by the {@link Files#getFileAttributeView}
     * method.
     *
     * @param <V>     The {@code FileAttributeView} type
     * @param path    the path to the file
     * @param type    the {@code Class} object corresponding to the file attribute view
     * @param options options indicating how symbolic links are handled
     *
     * @return a file attribute view of the specified type, or {@code null} if
     * the attribute view type is not available
     */
    /*
     * 返回path处文件的文件属性视图
     *
     * type   : 文件属性类型，不同的平台上有不同的实现。
     *                                              windows  linux  mac
     *          UserDefinedFileAttributeView.class     √       √
     *          BasicFileAttributeView.class           √       √     √
     *          DosFileAttributeView.class             √       √
     *          FileOwnerAttributeView.class           √       √     √
     *          AclFileAttributeView.class             √
     *          PosixFileAttributeView.class                   √     √
     *          JrtFileAttributeView.class             √       √     √
     *          ZipFileAttributeView.class             √       √     √
     * options: 对于符号链接，是否将其链接到目标文件；如果显式设置了LinkOption.NOFOLLOW_LINKS，表示不链接
     */
    public abstract <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options);
    
    /*▲ 文件属性(视图) ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件系统 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs a new {@code FileSystem} to access the contents of a file as a
     * file system.
     *
     * <p> This method is intended for specialized providers of pseudo file
     * systems where the contents of one or more files is treated as a file
     * system. The {@code env} parameter is a map of provider specific properties
     * to configure the file system.
     *
     * <p> If this provider does not support the creation of such file systems
     * or if the provider does not recognize the file type of the given file then
     * it throws {@code UnsupportedOperationException}. The default implementation
     * of this method throws {@code UnsupportedOperationException}.
     *
     * @param path The path to the file
     * @param env  A map of provider specific properties to configure the file system; may be empty
     *
     * @return A new file system
     *
     * @throws UnsupportedOperationException If this provider does not support access to the contents as a
     *                                       file system or it does not recognize the file type of the
     *                                       given file
     * @throws IllegalArgumentException      If the {@code env} parameter does not contain properties required
     *                                       by the provider, or a property value is invalid
     * @throws IOException                   If an I/O error occurs
     * @throws SecurityException             If a security manager is installed and it denies an unspecified
     *                                       permission.
     */
    /*
     * 返回与指定的Path匹配的文件系统，env是当前文件系统工厂用到的属性，可以不设置
     * 注：目前仅有"jar"文件系统工厂实现了该方法
     */
    public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns an existing {@code FileSystem} created by this provider.
     *
     * <p> This method returns a reference to a {@code FileSystem} that was
     * created by invoking the {@link #newFileSystem(URI, Map) newFileSystem(URI,Map)}
     * method. File systems created the {@link #newFileSystem(Path, Map)
     * newFileSystem(Path,Map)} method are not returned by this method.
     * The file system is identified by its {@code URI}. Its exact form
     * is highly provider dependent. In the case of the default provider the URI's
     * path component is {@code "/"} and the authority, query and fragment components
     * are undefined (Undefined components are represented by {@code null}).
     *
     * <p> Once a file system created by this provider is {@link
     * java.nio.file.FileSystem#close closed} it is provider-dependent if this
     * method returns a reference to the closed file system or throws {@link
     * FileSystemNotFoundException}. If the provider allows a new file system to
     * be created with the same URI as a file system it previously created then
     * this method throws the exception if invoked after the file system is
     * closed (and before a new instance is created by the {@link #newFileSystem
     * newFileSystem} method).
     *
     * <p> If a security manager is installed then a provider implementation
     * may require to check a permission before returning a reference to an
     * existing file system. In the case of the {@link FileSystems#getDefault
     * default} file system, no permission check is required.
     *
     * @param uri URI reference
     *
     * @return The file system
     *
     * @throws IllegalArgumentException    If the pre-conditions for the {@code uri} parameter aren't met
     * @throws FileSystemNotFoundException If the file system does not exist
     * @throws SecurityException           If a security manager is installed and it denies an unspecified
     *                                     permission.
     */
    /*
     * 返回与指定的URI匹配的文件系统(仅适用系统内部可用的文件系统)
     * 注：系统内部默认仅支持"file"/"jar"/"jrt"协议
     */
    public abstract FileSystem getFileSystem(URI uri);
    
    /**
     * Constructs a new {@code FileSystem} object identified by a URI. This
     * method is invoked by the {@link FileSystems#newFileSystem(URI, Map)}
     * method to open a new file system identified by a URI.
     *
     * <p> The {@code uri} parameter is an absolute, hierarchical URI, with a
     * scheme equal (without regard to case) to the scheme supported by this
     * provider. The exact form of the URI is highly provider dependent. The
     * {@code env} parameter is a map of provider specific properties to configure
     * the file system.
     *
     * <p> This method throws {@link FileSystemAlreadyExistsException} if the
     * file system already exists because it was previously created by an
     * invocation of this method. Once a file system is {@link
     * java.nio.file.FileSystem#close closed} it is provider-dependent if the
     * provider allows a new file system to be created with the same URI as a
     * file system it previously created.
     *
     * @param uri URI reference
     * @param env A map of provider specific properties to configure the file system; may be empty
     *
     * @return A new file system
     *
     * @throws IllegalArgumentException         If the pre-conditions for the {@code uri} parameter aren't met,
     *                                          or the {@code env} parameter does not contain properties required
     *                                          by the provider, or a property value is invalid
     * @throws IOException                      An I/O error occurs creating the file system
     * @throws SecurityException                If a security manager is installed and it denies an unspecified
     *                                          permission required by the file system provider implementation
     * @throws FileSystemAlreadyExistsException If the file system has already been created
     */
    /*
     * 返回与指定的URI匹配的文件系统，env是目标文件系统工厂用到的属性
     * 注1：系统内部默认仅支持"file"/"jar"/"jrt"协议，如果需要使用其它协议，则应当自定义匹配的文件系统工厂
     * 注2：目前，默认的"file"文件系统工厂【未实现】该方法
     */
    public abstract FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException;
    
    /*▲ 文件系统 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 服务发现 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a list of the installed file system providers.
     *
     * <p> The first invocation of this method causes the default provider to be
     * initialized (if not already initialized) and loads any other installed
     * providers as described by the {@link FileSystems} class.
     *
     * @return An unmodifiable list of the installed file system providers. The
     * list contains at least one element, that is the default file
     * system provider
     *
     * @throws ServiceConfigurationError When an error occurs while loading a service provider
     */
    // 返回当前所有可用的文件系统工厂，系统已实现的包括"file"/"jar"/"jrt"文件系统工厂
    public static List<FileSystemProvider> installedProviders() {
        if(installedProviders != null) {
            return installedProviders;
        }
    
        /* ensure default provider is initialized */
        // 获取默认的文件系统工厂："file"文件系统工厂
        FileSystemProvider defaultProvider = FileSystems.getDefault().provider();
    
        // 双重检查
        synchronized(lock) {
            if(installedProviders == null) {
                if(loadingProviders) {
                    throw new Error("Circular loading of installed providers detected");
                }
            
                // 标记为已加载过文件系统工厂
                loadingProviders = true;
            
                // 加载所有已注册的FileSystemProvider服务，返回除"file"之外的文件系统工厂
                List<FileSystemProvider> list = AccessController.doPrivileged(new PrivilegedAction<>() {
                    @Override
                    public List<FileSystemProvider> run() {
                        return loadInstalledProviders();
                    }
                });
                
                /* insert the default provider at the start of the list */
                // 添加"file"文件系统工厂
                list.add(0, defaultProvider);
                
                installedProviders = Collections.unmodifiableList(list);
            }
        }
        
        return installedProviders;
    }
    
    /** loads all installed providers */
    // 加载所有已注册的FileSystemProvider服务，返回除"file"之外的文件系统工厂
    private static List<FileSystemProvider> loadInstalledProviders() {
        List<FileSystemProvider> list = new ArrayList<>();
        
        // 使用指定的类加载器加载注册的FileSystemProvider服务
        ServiceLoader<FileSystemProvider> providers = ServiceLoader.load(FileSystemProvider.class, ClassLoader.getSystemClassLoader());
        
        // 遍历注册的FileSystemProvider
        for(FileSystemProvider provider : providers) {
            // 获取文件系统工厂provider支持的协议
            String scheme = provider.getScheme();
            
            // add to list if the provider is not "file" and isn't a duplicate
            if(scheme.equalsIgnoreCase("file")) {
                continue;
            }
            
            boolean found = false;
            
            for(FileSystemProvider p : list) {
                // 获取文件系统工厂provider支持的协议
                String sch = p.getScheme();
                
                if(sch.equalsIgnoreCase(scheme)) {
                    found = true;
                    break;
                }
            }
            
            if(!found) {
                list.add(provider);
            }
        }
        
        return list;
    }
    
    /*▲ 服务发现 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads the target of a symbolic link. This method works in exactly the
     * manner specified by the {@link Files#readSymbolicLink} method.
     *
     * <p> The default implementation of this method throws {@code
     * UnsupportedOperationException}.
     *
     * @param link the path to the symbolic link
     *
     * @return The target of the symbolic link
     *
     * @throws UnsupportedOperationException if the implementation does not support symbolic links
     * @throws NotLinkException              if the target could otherwise not be read because the file
     *                                       is not a symbolic link <i>(optional specific exception)</i>
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager
     *                                       is installed, it checks that {@code FilePermission} has been
     *                                       granted with the "{@code readlink}" action to read the link.
     */
    // 返回path处符号链接的路径
    public Path readSymbolicLink(Path link) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    
    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme
     */
    // 返回当前文件系统工厂支持的协议，目前支持file/jar/jrt协议，也可以自定义
    public abstract String getScheme();
    
    /**
     * Return a {@code Path} object by converting the given {@link URI}. The
     * resulting {@code Path} is associated with a {@link FileSystem} that
     * already exists or is constructed automatically.
     *
     * <p> The exact form of the URI is file system provider dependent. In the
     * case of the default provider, the URI scheme is {@code "file"} and the
     * given URI has a non-empty path component, and undefined query, and
     * fragment components. The resulting {@code Path} is associated with the
     * default {@link FileSystems#getDefault default} {@code FileSystem}.
     *
     * <p> If a security manager is installed then a provider implementation
     * may require to check a permission. In the case of the {@link
     * FileSystems#getDefault default} file system, no permission check is
     * required.
     *
     * @param uri The URI to convert
     *
     * @return The resulting {@code Path}
     *
     * @throws IllegalArgumentException    If the URI scheme does not identify this provider or other
     *                                     preconditions on the uri parameter do not hold
     * @throws FileSystemNotFoundException The file system, identified by the URI, does not exist and
     *                                     cannot be created automatically
     * @throws SecurityException           If a security manager is installed and it denies an unspecified
     *                                     permission.
     */
    // 从指定的uri中解析出一个有效路径，目前支持file/jar/jrt协议，也可以自定义
    public abstract Path getPath(URI uri);
    
    
    /**
     * Tests if two paths locate the same file. This method works in exactly the
     * manner specified by the {@link Files#isSameFile} method.
     *
     * @param path  one path to the file
     * @param path2 the other path
     *
     * @return {@code true} if, and only if, the two paths locate the same file
     *
     * @throws IOException       if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is
     *                           installed, the {@link SecurityManager#checkRead(String) checkRead}
     *                           method is invoked to check read access to both files.
     */
    // 判断两个路径(文件)是否相同
    public abstract boolean isSameFile(Path path1, Path path2) throws IOException;
    
    /**
     * Tells whether or not a file is considered <em>hidden</em>. This method
     * works in exactly the manner specified by the {@link Files#isHidden}
     * method.
     *
     * <p> This method is invoked by the {@link Files#isHidden isHidden} method.
     *
     * @param path the path to the file to test
     *
     * @return {@code true} if the file is considered hidden
     *
     * @throws IOException       if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is
     *                           installed, the {@link SecurityManager#checkRead(String) checkRead}
     *                           method is invoked to check read access to the file.
     */
    // 判断path处的文件是否为隐藏文件
    public abstract boolean isHidden(Path path) throws IOException;
    
    
    /**
     * Checks the existence, and optionally the accessibility, of a file.
     *
     * <p> This method may be used by the {@link Files#isReadable isReadable},
     * {@link Files#isWritable isWritable} and {@link Files#isExecutable
     * isExecutable} methods to check the accessibility of a file.
     *
     * <p> This method checks the existence of a file and that this Java virtual
     * machine has appropriate privileges that would allow it access the file
     * according to all of access modes specified in the {@code modes} parameter
     * as follows:
     *
     * <table class="striped">
     * <caption style="display:none">Access Modes</caption>
     * <thead>
     * <tr> <th scope="col">Value</th> <th scope="col">Description</th> </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <th scope="row"> {@link AccessMode#READ READ} </th>
     *   <td> Checks that the file exists and that the Java virtual machine has
     *     permission to read the file. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link AccessMode#WRITE WRITE} </th>
     *   <td> Checks that the file exists and that the Java virtual machine has
     *     permission to write to the file, </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link AccessMode#EXECUTE EXECUTE} </th>
     *   <td> Checks that the file exists and that the Java virtual machine has
     *     permission to {@link Runtime#exec execute} the file. The semantics
     *     may differ when checking access to a directory. For example, on UNIX
     *     systems, checking for {@code EXECUTE} access checks that the Java
     *     virtual machine has permission to search the directory in order to
     *     access file or subdirectories. </td>
     * </tr>
     * </tbody>
     * </table>
     *
     * <p> If the {@code modes} parameter is of length zero, then the existence
     * of the file is checked.
     *
     * <p> This method follows symbolic links if the file referenced by this
     * object is a symbolic link. Depending on the implementation, this method
     * may require to read file permissions, access control lists, or other
     * file attributes in order to check the effective access to the file. To
     * determine the effective access to a file may require access to several
     * attributes and so in some implementations this method may not be atomic
     * with respect to other file system operations.
     *
     * @param path  the path to the file to check
     * @param modes The access modes to check; may have zero elements
     *
     * @throws UnsupportedOperationException an implementation is required to support checking for
     *                                       {@code READ}, {@code WRITE}, and {@code EXECUTE} access. This
     *                                       exception is specified to allow for the {@code Access} enum to
     *                                       be extended in future releases.
     * @throws NoSuchFileException           if a file does not exist <i>(optional specific exception)</i>
     * @throws AccessDeniedException         the requested access would be denied or the access cannot be
     *                                       determined because the Java virtual machine has insufficient
     *                                       privileges or other reasons. <i>(optional specific exception)</i>
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager is
     *                                       installed, the {@link SecurityManager#checkRead(String) checkRead}
     *                                       is invoked when checking read access to the file or only the
     *                                       existence of the file, the {@link SecurityManager#checkWrite(String)
     *                                       checkWrite} is invoked when checking write access to the file,
     *                                       and {@link SecurityManager#checkExec(String) checkExec} is invoked
     *                                       when checking execute access.
     */
    // 判断是否可以对path处的文件(需存在)应用指定的访问模式；没有指定访问模式的话，默认检查文件是否可读
    public abstract void checkAccess(Path path, AccessMode... modes) throws IOException;
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    private static Void checkPermission() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("fileSystemProvider"));
        }
        
        return null;
    }
    
}
