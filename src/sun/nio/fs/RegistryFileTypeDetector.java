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

import java.nio.file.*;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * File type detector that does lookup of file extension using Windows Registry.
 */
// windows平台上对文件类型检测器的具体实现，其原理是通过查询注册表以获取资源类型(Content-Type)
public class RegistryFileTypeDetector extends AbstractFileTypeDetector {
    
    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                // nio.dll has dependency on net.dll
                System.loadLibrary("net");
                System.loadLibrary("nio");
                return null;
            }
        });
    }
    
    public RegistryFileTypeDetector() {
        super();
    }
    
    // 获取指定文件的类型(Content-Type)
    @Override
    public String implProbeContentType(Path file) throws IOException {
        if(file == null) {
            return null;
        }
        
        // 返回当前路径的名称(路径上最后一个组件)
        Path name = file.getFileName();
        if(name == null) {
            return null;
        }
        
        String filename = name.toString();
        
        int dot = filename.lastIndexOf('.');
        // 如果不存在文件后缀则直接返回
        if((dot<0) || (dot == (filename.length() - 1))) {
            return null;
        }
        
        // 获取文件后缀
        String key = filename.substring(dot);
        
        NativeBuffer keyBuffer = WindowsNativeDispatcher.asNativeBuffer(key);
        NativeBuffer nameBuffer = WindowsNativeDispatcher.asNativeBuffer("Content Type");
        try {
            // 在注册表(HKEY_CLASSES_ROOT\key:Content Type)中查找指定后缀对应的类型(Content-Type)
            return queryStringValue(keyBuffer.address(), nameBuffer.address());
        } finally {
            nameBuffer.release();
            keyBuffer.release();
        }
    }
    
    private static native String queryStringValue(long subKey, long name);
    
}
