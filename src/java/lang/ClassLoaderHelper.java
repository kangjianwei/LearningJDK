/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.io.File;

// 加载本地库文件的辅助类
class ClassLoaderHelper {
    
    /**
     * Indicates, whether PATH env variable is allowed to contain quoted entries.
     */
    // 是否允许环境变量中的路径用双引号包围起来
    static final boolean allowsQuotedPathElements = true;
    
    private ClassLoaderHelper() {
    }
    
    /**
     * Returns an alternate path name for the given file such that if the original pathname did not exist,
     * then the file may be located at the alternate location.
     * For most platforms, this behavior is not supported and returns null.
     */
    /*
     * 返回给定文件的备用路径名。
     * 这样，如果原始路径名不存在，则该文件可能位于该备用位置。
     * 对于大多数平台，不支持此行为，并且返回null。
     */
    static File mapAlternativeName(File lib) {
        return null;
    }
}
