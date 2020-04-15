/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base implementation of BasicFileAttributeView
 */
/*
 * "basic"文件属性视图的抽象实现，该视图支持一些通用的文件属性，包括：
 * - size             // 文件尺寸
 * - creationTime     // 创建时间
 * - lastAccessTime   // 最后访问时间
 * - lastModifiedTime // 最后修改时间
 * - fileKey          // 文件标识
 * - isDirectory      // 是否为目录
 * - isRegularFile    // 是否为包含不透明内容的常规文件
 * - isSymbolicLink   // 是否为符号链接
 * - isOther          // 是否为常规文件、目录或符号链接之外的其他文件
 *
 * 注：三大操作系统平台均支持该视图
 */
abstract class AbstractBasicFileAttributeView implements BasicFileAttributeView, DynamicFileAttributeView {
    
    private static final String SIZE_NAME = "size";             // 文件尺寸
    private static final String CREATION_TIME_NAME = "creationTime";     // 创建时间
    private static final String LAST_ACCESS_TIME_NAME = "lastAccessTime";   // 最后访问时间
    private static final String LAST_MODIFIED_TIME_NAME = "lastModifiedTime"; // 最后修改时间
    private static final String FILE_KEY_NAME = "fileKey";          // 文件标识
    private static final String IS_DIRECTORY_NAME = "isDirectory";      // 是否为目录
    private static final String IS_REGULAR_FILE_NAME = "isRegularFile";    // 是否为包含不透明内容的常规文件
    private static final String IS_SYMBOLIC_LINK_NAME = "isSymbolicLink";   // 是否为符号链接
    private static final String IS_OTHER_NAME = "isOther";          // 是否为常规文件、目录或符号链接之外的其他文件
    
    /** the names of the basic attributes */
    // 当前"basic"文件属性视图中所有允许出现的属性名
    static final Set<String> basicAttributeNames = Util.newSet(SIZE_NAME, CREATION_TIME_NAME, LAST_ACCESS_TIME_NAME, LAST_MODIFIED_TIME_NAME, FILE_KEY_NAME, IS_DIRECTORY_NAME, IS_REGULAR_FILE_NAME, IS_SYMBOLIC_LINK_NAME, IS_OTHER_NAME);
    
    protected AbstractBasicFileAttributeView() {
    }
    
    // 返回当前属性视图的名称："basic"
    @Override
    public String name() {
        return "basic";
    }
    
    /*
     * 向当前"basic"文件属性视图中设置attName属性，设置的属性值为value，可设置的属性包括：
     * - creationTime    : 创建时间，其值是FileTime类型的对象
     * - lastAccessTime  : 最后访问时间，其值是FileTime类型的对象
     * - lastModifiedTime: 最后修改时间，其值是FileTime类型的对象
     */
    @Override
    public void setAttribute(String attName, Object value) throws IOException {
        
        // 设置创建时间
        if(attName.equals(CREATION_TIME_NAME)) {
            setTimes(null, null, (FileTime) value);
            return;
        }
        
        // 设置最后访问时间
        if(attName.equals(LAST_ACCESS_TIME_NAME)) {
            setTimes(null, (FileTime) value, null);
            return;
        }
        
        // 设置最后修改时间
        if(attName.equals(LAST_MODIFIED_TIME_NAME)) {
            setTimes((FileTime) value, null, null);
            return;
        }
        
        throw new IllegalArgumentException("'" + name() + ":" + attName + "' not recognized");
    }
    
    /**
     * 从当前"basic"文件属性视图中获取一批属性的值；这批属性的名称由attNames给出，获取到的属性以<属性名, 属性值>的形式返回，可获取的属性包括：
     * - creationTime    : 创建时间，其值是FileTime类型的对象
     * - lastAccessTime  : 最后访问时间，其值是FileTime类型的对象
     * - lastModifiedTime: 最后修改时间，其值是FileTime类型的对象
     */
    @Override
    public Map<String, Object> readAttributes(String[] attNames) throws IOException {
        // 创建一个属性构建器，basicAttributeNames提供了所有允许出现的属性名
        AttributesBuilder builder = AttributesBuilder.create(basicAttributeNames, attNames);
        
        // 获取"basic"文件属性视图
        BasicFileAttributes basicFileAttributes = readAttributes();
        
        // 构造"basic"文件属性映射
        addRequestedBasicAttributes(basicFileAttributes, builder);
        
        // 以只读Map的形式返回"basic"文件属性映射
        return builder.unmodifiableMap();
    }
    
    /**
     * Invoked by readAttributes or sub-classes to add all matching basic
     * attributes to the builder
     */
    // 构造"basic"文件属性映射
    final void addRequestedBasicAttributes(BasicFileAttributes attrs, AttributesBuilder builder) {
        if(builder.match(SIZE_NAME)) {
            builder.add(SIZE_NAME, attrs.size());
        }
        
        if(builder.match(CREATION_TIME_NAME)) {
            builder.add(CREATION_TIME_NAME, attrs.creationTime());
        }
        
        if(builder.match(LAST_ACCESS_TIME_NAME)) {
            builder.add(LAST_ACCESS_TIME_NAME, attrs.lastAccessTime());
        }
        
        if(builder.match(LAST_MODIFIED_TIME_NAME)) {
            builder.add(LAST_MODIFIED_TIME_NAME, attrs.lastModifiedTime());
        }
        
        if(builder.match(FILE_KEY_NAME)) {
            builder.add(FILE_KEY_NAME, attrs.fileKey());
        }
        
        if(builder.match(IS_DIRECTORY_NAME)) {
            builder.add(IS_DIRECTORY_NAME, attrs.isDirectory());
        }
        
        if(builder.match(IS_REGULAR_FILE_NAME)) {
            builder.add(IS_REGULAR_FILE_NAME, attrs.isRegularFile());
        }
        
        if(builder.match(IS_SYMBOLIC_LINK_NAME)) {
            builder.add(IS_SYMBOLIC_LINK_NAME, attrs.isSymbolicLink());
        }
        
        if(builder.match(IS_OTHER_NAME)) {
            builder.add(IS_OTHER_NAME, attrs.isOther());
        }
    }
    
    
    /**
     * Used to build a map of attribute name/values.
     */
    // 属性构建器
    static class AttributesBuilder {
        private Set<String> names = new HashSet<>();        // 存储向属性构建器注册的属性
        private boolean copyAll;                            // 如果为true，说明接受所有注册的属性
        private Map<String, Object> map = new HashMap<>();  // 属性集，为注册的属性名关联属性值
        
        // 构造属性构建器，allowed指示允许注册的属性，requested指示当前申请注册的属性
        private AttributesBuilder(Set<String> allowed, String[] requested) {
            for(String name : requested) {
                if(name.equals("*")) {
                    copyAll = true;
                } else {
                    if(!allowed.contains(name)) {
                        throw new IllegalArgumentException("'" + name + "' not recognized");
                    }
                    
                    names.add(name);
                }
            }
        }
        
        /**
         * Creates builder to build up a map of the matching attributes
         */
        // 创建一个属性构建器
        static AttributesBuilder create(Set<String> allowed, String[] requested) {
            return new AttributesBuilder(allowed, requested);
        }
        
        /**
         * Returns true if the attribute should be returned in the map
         */
        // 判断指定的属性是否为注册过的属性
        boolean match(String attName) {
            return copyAll || names.contains(attName);
        }
        
        // 添加属性名与属性值到内部映射中
        void add(String attName, Object value) {
            map.put(attName, value);
        }
        
        /**
         * Returns the map. Discard all references to the AttributesBuilder
         * after invoking this method.
         */
        // 以只读Map的形式返回内部构造的属性集
        Map<String, Object> unmodifiableMap() {
            return Collections.unmodifiableMap(map);
        }
    }
    
}
