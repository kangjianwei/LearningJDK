/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

/**
 * File attributes associated with a file in a file system that supports legacy "DOS" attributes.
 *
 * <p> <b>Usage Example:</b>
 * <pre>
 *    Path file = ...
 *    DosFileAttributes attrs = Files.readAttributes(file, DosFileAttributes.class);
 * </pre>
 *
 * @since 1.7
 */
/*
 * "dos"文件属性
 *
 * 注：
 * windows平台上直接实现了该接口；
 * linux平台上先实现PosixFileAttributes接口，然后构造了一个内部类间接兼容DosFileAttributes；
 * mac平台上未实现该接口。
 */
public interface DosFileAttributes extends BasicFileAttributes {
    
    /**
     * Returns the value of the read-only attribute.
     *
     * This attribute is often used as a simple access control mechanism to prevent files from being deleted or updated.
     * Whether the file system or platform does any enforcement to prevent <em>read-only</em> files
     * from being updated is implementation specific.
     *
     * @return the value of the read-only attribute
     */
    // 判断是否为只读文件
    boolean isReadOnly();
    
    /**
     * Returns the value of the hidden attribute.
     *
     * This attribute is often used to indicate if the file is visible to users.
     *
     * @return the value of the hidden attribute
     */
    // 判断是否为隐藏文件
    boolean isHidden();
    
    /**
     * Returns the value of the system attribute.
     *
     * This attribute is often used to indicate that the file is a component of the operating system.
     *
     * @return the value of the system attribute
     */
    // 判断是否为系统文件
    boolean isSystem();
    
    /**
     * Returns the value of the archive attribute.
     *
     * This attribute is typically used by backup programs.
     *
     * @return the value of the archive attribute
     */
    // 判断是否为已存档文件，用在备份操作中
    boolean isArchive();
    
}
