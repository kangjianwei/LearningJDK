/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.attribute.*;
import java.io.InputStream;
import java.io.IOException;

/**
 * Helper class to support copying or moving files when the source and target are associated with different providers.
 */
// 文件/目录的工具类，用来处理在不同文件系统下的数据复制
class CopyMoveHelper {
    private CopyMoveHelper() {
    }
    
    /**
     * Simple copy for use when source and target are associated with different providers
     */
    // 在不同的文件系统间复制数据，不支持中断操作
    static void copyToForeignTarget(Path source, Path target, CopyOption... options) throws IOException {
        
        // 解析复制参数，不支持中断操作
        CopyOptions opts = CopyOptions.parse(options);
        
        LinkOption[] linkOptions = (opts.followLinks) ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
        
        // attributes of source file
        BasicFileAttributes attrs = Files.readAttributes(source, BasicFileAttributes.class, linkOptions);
        // 如果attrs的宿主资源为符号链接，则抛异常
        if(attrs.isSymbolicLink()) {
            throw new IOException("Copying of symbolic links not supported");
        }
        
        // delete target if it exists and REPLACE_EXISTING is specified
        if(opts.replaceExisting) {
            // 删除target处的文件/目录；如果待删除目标是非空的目录，则直接抛异常；但如果待删除目标不存在，不会抛异常
            Files.deleteIfExists(target);
        } else if(Files.exists(target)) {
            throw new FileAlreadyExistsException(target.toString());
        }
        
        // 如果attrs的宿主资源为非符号链接的目录
        if(attrs.isDirectory()) {
            // 在指定的路径处创建目录，如果该目录已存在，则抛出异常
            Files.createDirectory(target);
        } else {
            try(InputStream in = Files.newInputStream(source)) {
                Files.copy(in, target);
            }
        }
        
        // 如果不需要复制属性，则直接返回
        if(!opts.copyAttributes) {
            return;
        }
        
        BasicFileAttributeView view = Files.getFileAttributeView(target, BasicFileAttributeView.class);
        try {
            view.setTimes(attrs.lastModifiedTime(), attrs.lastAccessTime(), attrs.creationTime());
        } catch(Throwable x) {
            // rollback
            try {
                // 删除target处的文件/目录；如果待删除目标不存在，或者待删除目标是非空的目录，则直接抛异常
                Files.delete(target);
            } catch(Throwable suppressed) {
                x.addSuppressed(suppressed);
            }
            throw x;
        }
    }
    
    /**
     * Simple move implements as copy+delete for use when source and target are
     * associated with different providers
     */
    // 在不同的文件系统间移动数据
    static void moveToForeignTarget(Path source, Path target, CopyOption... options) throws IOException {
        // 在不同的文件系统间复制数据
        copyToForeignTarget(source, target, convertMoveToCopyOptions(options));
        
        // 复制完成后，删除source处的文件/目录；如果待删除目标不存在，或者待删除目标是非空的目录，则直接抛异常
        Files.delete(source);
    }
    
    /**
     * Converts the given array of options for moving a file to options suitable
     * for copying the file when a move is implemented as copy + delete.
     */
    // 将移动参数转换为复制参数
    private static CopyOption[] convertMoveToCopyOptions(CopyOption... options) throws AtomicMoveNotSupportedException {
        int len = options.length;
        CopyOption[] newOptions = new CopyOption[len + 2];
        for(int i = 0; i<len; i++) {
            CopyOption option = options[i];
            if(option == StandardCopyOption.ATOMIC_MOVE) {
                throw new AtomicMoveNotSupportedException(null, null, "Atomic move between providers is not supported");
            }
            newOptions[i] = option;
        }
        newOptions[len] = LinkOption.NOFOLLOW_LINKS;
        newOptions[len + 1] = StandardCopyOption.COPY_ATTRIBUTES;
        return newOptions;
    }
    
    /**
     * Parses the arguments for a file copy operation.
     */
    // 复制参数
    private static class CopyOptions {
        boolean replaceExisting = false;
        boolean copyAttributes = false;
        boolean followLinks = true;
        
        private CopyOptions() {
        }
        
        // 解析复制参数，不支持中断操作
        static CopyOptions parse(CopyOption... options) {
            CopyOptions result = new CopyOptions();
            
            for(CopyOption option : options) {
                if(option == StandardCopyOption.REPLACE_EXISTING) {
                    result.replaceExisting = true;
                    continue;
                }
                
                if(option == LinkOption.NOFOLLOW_LINKS) {
                    result.followLinks = false;
                    continue;
                }
                
                if(option == StandardCopyOption.COPY_ATTRIBUTES) {
                    result.copyAttributes = true;
                    continue;
                }
                
                if(option == null) {
                    throw new NullPointerException();
                }
                
                throw new UnsupportedOperationException("'" + option + "' is not a recognized copy option");
            }
            
            return result;
        }
    }
}
