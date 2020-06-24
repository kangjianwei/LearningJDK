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
import java.util.Map;

/**
 * Implemented by FileAttributeView implementations to support access to attributes by names.
 */
// 动态的文件属性视图，根据指定的名称确定对应的视图
interface DynamicFileAttributeView {
    
    /*
     * JDK中实现的文件属性视图类型：
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
     *
     *
     *
     * 各类型文件属性视图中可用的属性名称attName如下：
     * ---------------------------------------------------------------------------------------------
     * 属性类型 |                            属性名称
     * "basic" | [creationTime, lastAccessTime, lastModifiedTime]
     * "dos"   | [creationTime, lastAccessTime, lastModifiedTime, readonly, archive, system, hidden]
     * "user"  | [自定义]
     * "owner" | [owner]
     * "acl"   | [owner, acl]
     * "posix" | [creationTime, lastAccessTime, lastModifiedTime, permissions, owner, group]
     * "unix"  | [creationTime, lastAccessTime, lastModifiedTime, permissions, owner, group, mode, ino, dev, rdev, nlink, uid, gid, ctime]
     */
    
    /**
     * Sets/updates the value of an attribute.
     */
    // 向当前文件属性视图中设置attName属性，设置的属性值为value
    void setAttribute(String attName, Object value) throws IOException;
    
    /**
     * Reads a set of file attributes as a bulk operation.
     */
    // 从当前文件属性视图中获取一批属性的值；这批属性的名称由attNames给出，获取到的属性以<属性名, 属性值>的形式返回
    Map<String, Object> readAttributes(String[] attNames) throws IOException;
    
}
