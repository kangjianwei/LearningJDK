/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.lang.annotation.Native;

/**
 * Package-private abstract class for the local filesystem abstraction.
 */
// 本地文件系统，不同的平台有不同的实现(注意与java.nio.file.FileSystem区分)
abstract class FileSystem {
    
    /*
     * -- Normalization and construction --
     *
     * Constants for simple boolean attributes
     */
    @Native
    public static final int BA_EXISTS = 0x01; // 存在
    @Native
    public static final int BA_REGULAR = 0x02; // 常规文件
    @Native
    public static final int BA_DIRECTORY = 0x04; // 目录
    @Native
    public static final int BA_HIDDEN = 0x08; // 隐藏文件
    
    @Native
    public static final int ACCESS_READ = 0x04;  // 可读标记
    @Native
    public static final int ACCESS_WRITE = 0x02;  // 可写标记
    @Native
    public static final int ACCESS_EXECUTE = 0x01;  // 可执行标记
    
    /*
     * -- Path operations --
     *
     * -- Disk usage --
     */
    @Native
    public static final int SPACE_TOTAL = 0;
    @Native
    public static final int SPACE_FREE = 1;
    @Native
    public static final int SPACE_USABLE = 2;
    
    /*
     *  -- Attribute accessors --
     *
     * Flags for enabling/disabling performance optimizations for file name canonicalization
     */
    static boolean useCanonCaches = true;
    static boolean useCanonPrefixCache = true;
    
    
    static {
        useCanonCaches = getBooleanProperty("sun.io.useCanonCaches", useCanonCaches);
        useCanonPrefixCache = getBooleanProperty("sun.io.useCanonPrefixCache", useCanonPrefixCache);
    }
    
    
    /**
     * Return the local filesystem's name-separator character.
     */
    // 返回路径内部的分隔符：Windows系统上是'\'，类Unix系统上是'/'
    public abstract char getSeparator();
    
    /**
     * Return the local filesystem's path-separator character.
     */
    // 返回路径之间的分隔符：Windows系统上是';'，类Unix系统上是':'
    public abstract char getPathSeparator();
    
    /**
     * Convert the given pathname string to normal form.
     * If the string is already in normal form then it is simply returned.
     */
    // 返回本地化路径(忽略最后的'\'，除非是根目录)
    public abstract String normalize(String path);
    
    /**
     * Compute the length of this pathname string's prefix.
     * The pathname string must be in normal form.
     */
    // 返回本地化路径的前缀长度
    public abstract int prefixLength(String path);
    
    /**
     * Resolve the given abstract pathname into absolute form.
     * Invoked by the getAbsolutePath and getCanonicalPath methods in the File class.
     */
    // 将指定文件的本地化路径解析为绝对路径后返回
    public abstract String resolve(File file);
    
    /**
     * Resolve the child pathname string against the parent.
     * Both strings must be in normal form, and the result will be in normal form.
     */
    // 返回拼接后的本地化路径
    public abstract String resolve(String parent, String child);
    
    /**
     * Return the parent pathname string to be used when the parent-directory
     * argument in one of the two-argument File constructors is the empty
     * pathname.
     */
    // 返回默认的父级路径：：Windows系统上是'\'，类Unix系统上是'/'
    public abstract String getDefaultParent();
    
    /**
     * Create a new directory denoted by the given abstract pathname,
     * returning <code>true</code> if and only if the operation succeeds.
     */
    // 创建【目录】，返回值指示是否创建成功
    public abstract boolean createDirectory(File dir);
    
    /**
     * Create a new empty file with the given pathname.  Return
     * <code>true</code> if the file was created and <code>false</code> if a
     * file or directory with the given pathname already exists.  Throw an
     * IOException if an I/O error occurs.
     */
    // 创建【文件】，返回值指示是否创建成功
    public abstract boolean createFileExclusively(String pathname) throws IOException;
    
    /**
     * Delete the file or directory denoted by the given abstract pathname,
     * returning <code>true</code> if and only if the operation succeeds.
     */
    // 删除File，如果File是目录，则仅支持删除空目录，返回值指示是否删除成功
    public abstract boolean delete(File file);
    
    /**
     * Rename the file or directory denoted by the first abstract pathname to
     * the second abstract pathname, returning <code>true</code> if and only if
     * the operation succeeds.
     */
    // 将oldFile重命名为newFile(伴随"移动"的副作用)
    public abstract boolean rename(File oldFile, File newFile);
    
    /**
     * List the elements of the directory denoted by the given abstract pathname.
     * Return an array of strings naming the elements of the directory if successful; otherwise, return <code>null</code>.
     */
    // 返回子File的路径列表。如果file是文件，返回null。如果file是空目录，返回的数组元素数量也为0。
    public abstract String[] list(File file);
    
    /**
     * List the available filesystem roots.
     */
    // 返回根目录列表
    public abstract File[] listRoots();
    
    /**
     * Return the simple boolean attributes for the file or directory denoted
     * by the given abstract pathname, or zero if it does not exist or some
     * other I/O error occurs.
     */
    // 返回File的基础属性
    public abstract int getBooleanAttributes(File file);
    
    /**
     * Set the last-modified time of the file or directory denoted by the
     * given abstract pathname, returning <code>true</code> if and only if the
     * operation succeeds.
     */
    // 设置File的最后访问时间
    public abstract boolean setLastModifiedTime(File file, long time);
    
    /**
     * Return the time at which the file or directory denoted by the given
     * abstract pathname was last modified, or zero if it does not exist or
     * some other I/O error occurs.
     */
    // 返回File的最后修改时间
    public abstract long getLastModifiedTime(File file);
    
    /**
     * Mark the file or directory denoted by the given abstract pathname as
     * read-only, returning <code>true</code> if and only if the operation
     * succeeds.
     */
    // 设置File为只读
    public abstract boolean setReadOnly(File file);
    
    /**
     * Set on or off the access permission (to owner only or to all) to the file
     * or directory denoted by the given abstract pathname, based on the parameters
     * enable, access and oweronly.
     */
    // 设置File的访问权限(开启或关闭)
    public abstract boolean setPermission(File file, int access, boolean enable, boolean owneronly);
    
    /**
     * Check whether the file or directory denoted by the given abstract
     * pathname may be accessed by this process.  The second argument specifies
     * which access, ACCESS_READ, ACCESS_WRITE or ACCESS_EXECUTE, to check.
     * Return false if access is denied or an I/O error occurs
     */
    // 检查File是否具有某个访问属性
    public abstract boolean checkAccess(File file, int access);
    
    /**
     * Tell whether or not the given abstract pathname is absolute.
     */
    // 判断当前本地化路径是否为绝对路径
    public abstract boolean isAbsolute(File file);
    
    // 根据flag参数的不同，返回File所在磁盘的大小、磁盘未分配的存储大小、File可用的存储大小
    public abstract long getSpace(File file, int flag);
    
    /**
     * Post-process the given URI path string if necessary.
     * This is used on win32, e.g., to transform "/c:/foo" into "c:/foo".
     * The path string still has slash separators;
     * code in the File class will translate them after this method returns.
     */
    // 对uri中解析出的path进行后处理，将其转换为普通路径
    public abstract String fromURIPath(String path);
    
    // 返回对指定路径的规范化形式：盘符大写，解析"."和".."
    public abstract String canonicalize(String path) throws IOException;
    
    /**
     * Return the length in bytes of the file denoted by the given abstract
     * pathname, or zero if it does not exist, is a directory, or some other
     * I/O error occurs.
     */
    // 返回File的大小(以字节计)
    public abstract long getLength(File file);
    
    /**
     * Retrieve the maximum length of a component of a file path.
     *
     * @return The maximum length of a file path component.
     */
    // 返回最大路径名长度
    public abstract int getNameMax(String path);
    
    /**
     * Compare two abstract pathnames lexicographically.
     */
    /*
     * 比较两个File的本地化路径
     *
     * 在windows系统上通常忽略大小写，而在类unix系统上需要考虑大小写。
     */
    public abstract int compare(File file1, File file2);
    
    /**
     * Compute the hash code of an abstract pathname.
     */
    // 计算File的哈希码
    public abstract int hashCode(File file);
    
    // 获取boolean类型的属性
    private static boolean getBooleanProperty(String prop, boolean defaultVal) {
        return Boolean.parseBoolean(System.getProperty(prop, String.valueOf(defaultVal)));
    }
    
}
