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

package sun.nio.fs;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;

/**
 * Base implementation class of FileSystemProvider
 */
// 文件系统工厂(服务)的抽象实现
public abstract class AbstractFileSystemProvider extends FileSystemProvider {
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    protected AbstractFileSystemProvider() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 删除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 删除path处的文件/目录；如果待删除目标不存在，或者待删除目标是非空的目录，则直接抛异常
    @Override
    public final void delete(Path path) throws IOException {
        implDelete(path, true);
    }
    
    // 删除path处的文件/目录；如果待删除目标是非空的目录，则直接抛异常；但如果待删除目标不存在，不会抛异常
    @Override
    public final boolean deleteIfExists(Path path) throws IOException {
        return implDelete(path, false);
    }
    
    /**
     * Deletes a file. The {@code failIfNotExists} parameters determines if an
     * {@code IOException} is thrown when the file does not exist.
     */
    // 删除path处的文件/目录；failIfNotExists指示待删除目标不存在时，是否抛异常；如果待删除目标是非空的目录，则直接抛异常
    abstract boolean implDelete(Path path, boolean failIfNotExists) throws IOException;
    
    /*▲ 删除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件属性(视图) ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    @Override
    public final void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        // 分割以":"分割的属性信息
        String[] attr = split(attribute);
        if(attr[0].length() == 0) {
            throw new IllegalArgumentException(attribute);
        }
        
        // 返回file处文件的文件属性视图，该视图的类型由attr[0]决定
        DynamicFileAttributeView view = getFileAttributeView(path, attr[0], options);
        
        if(view == null) {
            throw new UnsupportedOperationException("View '" + attr[0] + "' not available");
        }
        
        // 向文件属性视图view中设置attr[1]属性，设置的属性值为value
        view.setAttribute(attr[1], value);
    }
    
    /*
     * 从path处的文件中获取指定的属性(多条)
     *
     * path     : 等待获取属性的文件的路径
     * attribute: 获取的属性名，其格式为"属性类型:属性名称1,属性名称2,属性名称3..."，如果未指定属性类型，默认使用"basic"类型；
     *            属性类型的常用取值参见AbstractFileSystemProvider#getFileAttributeView()方法中的name参数；
     *            属性名称的取值参见不同属性视图的实现类，使用"*"表示获取指定视图下所有属性。
     * options  : 指示对于符号链接，是否将其链接到目标文件；如果显式设置了LinkOption.NOFOLLOW_LINKS，表示不链接
     */
    @Override
    public final Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        // 分割以":"分割的属性信息
        String[] attr = split(attributes);
        
        if(attr[0].length() == 0) {
            throw new IllegalArgumentException(attributes);
        }
        
        // 返回path处文件的文件属性视图，该视图的类型由attr[0]决定
        DynamicFileAttributeView view = getFileAttributeView(path, attr[0], options);
        
        if(view == null) {
            throw new UnsupportedOperationException("View '" + attr[0] + "' not available");
        }
        
        // 从文件属性视图view中获取一批属性的值；这批属性的名称由attr[1]给出，获取到的属性以<属性名, 属性值>的形式返回
        return view.readAttributes(attr[1].split(","));
    }
    
    /**
     * Gets a DynamicFileAttributeView by name. Returns {@code null} if the view is not available.
     */
    /*
     * 返回path处文件的文件属性视图，该视图的类型由name决定，options指示对于符号链接，是否将其链接到目标文件；如果显式设置了LinkOption.NOFOLLOW_LINKS，表示不链接
     *
     * name参数允许的取值为：
     *          windows   linux   mac
     * "basic"     √        √      √
     * "dos"       √        √
     * "user"      √        √
     * "owner"     √        √      √
     * "acl"       √
     * "posix"              √      √
     * "unix"               √      √
     *
     *              视图"继承体系"
     *                    |
     *   +--------+-------+--------------+
     *   |        |       |              |
     *   |        |       |           "basic"
     *   |        |       |       +-----+-----+
     * "user"  "owner"  "acl"  "posix"      "dos"
     *                            |
     *                          "unix"
     *
     * 注："owner"是被"acl"(windows)或"posix"(linux/mac)代理的
     */
    abstract DynamicFileAttributeView getFileAttributeView(Path path, String name, LinkOption... options);
    
    /*▲ 文件属性(视图) ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ "basic"属性视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tests whether a file is a directory.
     *
     * @return {@code true} if the file is a directory; {@code false} if
     * the file does not exist, is not a directory, or it cannot
     * be determined if the file is a directory or not.
     */
    // 判断file文件是否为非符号链接的目录
    public boolean isDirectory(Path file) {
        try {
            // 获取file文件的"basic"文件属性
            BasicFileAttributes attributes = readAttributes(file, BasicFileAttributes.class);
            // 判断attributes的宿主资源是否为非符号链接的目录
            return attributes.isDirectory();
        } catch(IOException ioe) {
            return false;
        }
    }
    
    /**
     * Tests whether a file is a regular file with opaque content.
     *
     * @return {@code true} if the file is a regular file; {@code false} if
     * the file does not exist, is not a regular file, or it
     * cannot be determined if the file is a regular file or not.
     */
    /*
     * 判断file文件是否为"不透明的"常规文件
     *
     * 在类unix系统中，该类文件是永久存储在文件系统中的字节序列；
     * 在windows上，比如普通文件、文件硬链接，均属于"不透明的"常规文件；
     * 对于符号链接，如果需要将其链接到目标文件，那么文件的符号链接也属于"不透明的"常规文件。
     */
    public boolean isRegularFile(Path file) {
        try {
            // 获取file文件的文件属性
            BasicFileAttributes attributes = readAttributes(file, BasicFileAttributes.class);
            
            // 判断是否为包含不透明内容的常规文件
            return attributes.isRegularFile();
        } catch(IOException ioe) {
            return false;
        }
    }
    
    /*▲ "basic"属性视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Checks the existence of a file.
     *
     * @return {@code true} if the file exists; {@code false} if the file does
     * not exist or its existence cannot be determined.
     */
    // 判断file文件是否存在
    public boolean exists(Path file) {
        try {
            checkAccess(file);
            return true;
        } catch(IOException ioe) {
            return false;
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Splits the given attribute name into the name of an attribute view and the attribute.
     * If the attribute view is not identified then it assumed to be "basic".
     */
    // 分割以":"分割的属性信息，attribute的格式为"属性类型:属性名称"，如果未指定属性类型，默认使用"basic"类型
    private static String[] split(String attribute) {
        String[] s = new String[2];
        int pos = attribute.indexOf(':');
        if(pos == -1) {
            s[0] = "basic";
            s[1] = attribute;
        } else {
            s[0] = attribute.substring(0, pos++);
            s[1] = (pos == attribute.length()) ? "" : attribute.substring(pos);
        }
        return s;
    }
    
}
