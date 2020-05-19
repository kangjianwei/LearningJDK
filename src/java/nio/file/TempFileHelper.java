/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.Set;
import sun.security.action.GetPropertyAction;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;


/**
 * Helper class to support creation of temporary files and directories with initial attributes.
 */
// 临时文件/目录工具类
class TempFileHelper {
    /** temporary directory location */
    // 临时文件/目录生成的位置
    private static final Path tmpdir = Path.of(GetPropertyAction.privilegedGetProperty("java.io.tmpdir"));
    
    // 判断当前平台是否属于"posix"家族，在windows平台上为false，在linux/mac平台上为true
    private static final boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    
    // file name generation, same as java.io.File for now
    private static final SecureRandom random = new SecureRandom();
    
    private TempFileHelper() {
    }
    
    /**
     * Creates a temporary file in the given directory, or in the temporary directory if dir is {@code null}.
     */
    // 创建临时文件："dir+prefix+随机数+suffix"
    static Path createTempFile(Path dir, String prefix, String suffix, FileAttribute<?>[] attrs) throws IOException {
        return create(dir, prefix, suffix, false, attrs);
    }
    
    /**
     * Creates a temporary directory in the given directory, or in the temporary directory if dir is {@code null}.
     */
    // 创建临时目录："dir+prefix+随机数"
    static Path createTempDirectory(Path dir, String prefix, FileAttribute<?>[] attrs) throws IOException {
        return create(dir, prefix, null, true, attrs);
    }
    
    /**
     * Creates a file or directory in the given directory (or in the temporary directory if dir is {@code null}).
     */
    // 创建临时文件/目录
    private static Path create(Path dir, String prefix, String suffix, boolean createDirectory, FileAttribute<?>[] attrs) throws IOException {
        if(prefix == null) {
            prefix = "";
        }
        
        if(suffix == null) {
            suffix = (createDirectory) ? "" : ".tmp";
        }
        
        if(dir == null) {
            dir = tmpdir;
        }
        
        /* in POSIX environments use default file and directory permissions if initial permissions not given by caller */
        // 在POSIX环境下需要添加特定的权限
        if(isPosix && (dir.getFileSystem() == FileSystems.getDefault())) {
            if(attrs.length == 0) {
                // no attributes so use default permissions
                attrs = new FileAttribute<?>[1];
                attrs[0] = (createDirectory) ? PosixPermissions.dirPermissions : PosixPermissions.filePermissions;
            } else {
                // check if posix permissions given; if not use default
                boolean hasPermissions = false;
                for(FileAttribute<?> attr : attrs) {
                    if(attr.name().equals("posix:permissions")) {
                        hasPermissions = true;
                        break;
                    }
                }
                
                if(!hasPermissions) {
                    FileAttribute<?>[] copy = new FileAttribute<?>[attrs.length + 1];
                    System.arraycopy(attrs, 0, copy, 0, attrs.length);
                    attrs = copy;
                    attrs[attrs.length - 1] = (createDirectory) ? PosixPermissions.dirPermissions : PosixPermissions.filePermissions;
                }
            }
        }
        
        // loop generating random names until file or directory can be created
        SecurityManager sm = System.getSecurityManager();
        
        for(; ; ) {
            Path path;
            try {
                // 构造临时文件/目录的路径:"dir+prefix+随机数+suffix"
                path = generatePath(prefix, suffix, dir);
            } catch(InvalidPathException e) {
                // don't reveal temporary directory location
                if(sm != null) {
                    throw new IllegalArgumentException("Invalid prefix or suffix");
                }
                throw e;
            }
            
            try {
                if(createDirectory) {
                    // 在指定的路径处创建目录，如果该目录已存在，则抛出异常
                    return Files.createDirectory(path, attrs);
                } else {
                    // 在指定的路径处创建文件，如果该文件已存在，则抛出异常
                    return Files.createFile(path, attrs);
                }
            } catch(SecurityException e) {
                // don't reveal temporary directory location
                if(dir == tmpdir && sm != null) {
                    throw new SecurityException("Unable to create temporary file or directory");
                }
                throw e;
            } catch(FileAlreadyExistsException e) {
                // ignore
            }
        }
    }
    
    // 构造临时文件/目录的路径:"dir+prefix+随机数+suffix"
    private static Path generatePath(String prefix, String suffix, Path dir) {
        
        // 按10进制返回一个随机long的无符号值
        String n = Long.toUnsignedString(random.nextLong());
        
        String name = prefix + n + suffix;
        
        // 获取dir所属的文件系统
        FileSystem fileSystem = dir.getFileSystem();
        
        // 构造与fileSystem匹配的路径对象，返回的路径已经本地化
        Path path = fileSystem.getPath(name);
        
        // the generated name should be a simple file name
        if(path.getParent() != null) {
            throw new IllegalArgumentException("Invalid prefix or suffix");
        }
        
        // 绝对化：基于dir解析path；如果pathr是相对路径，则返回"dir+path"，如果path是绝对路径，原样返回
        return dir.resolve(path);
    }
    
    // default file and directory permissions (lazily initialized)
    private static class PosixPermissions {
        static final FileAttribute<Set<PosixFilePermission>> filePermissions = PosixFilePermissions.asFileAttribute(EnumSet.of(OWNER_READ, OWNER_WRITE));
        static final FileAttribute<Set<PosixFilePermission>> dirPermissions = PosixFilePermissions.asFileAttribute(EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));
    }
    
}
