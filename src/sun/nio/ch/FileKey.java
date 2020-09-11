/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;

/*
 * Represents a key to a specific file on Windows
 */
/*
 * 文件键，用来记录文件在本地(native层)的引用信息
 *
 * 此处的实现对应于Windows平台，在Linux平台上会有别的实现
 */
public class FileKey {
    
    private long dwVolumeSerialNumber;  // 文件所在卷序列号
    
    /*
     * 文件ID的高位和低位
     *
     * 使用卷序列号和文件ID可以唯一定位一个文件
     * 但是该ID在系统重启或下次打开文件时可能会发生变化
     */
    private long nFileIndexHigh;
    private long nFileIndexLow;
    
    static {
        // 触发IOUtil完成静态初始化（包括加载本地类库）
        IOUtil.load();
        
        // 在底层获取文件定位信息
        initIDs();
    }
    
    private FileKey() {
    }
    
    // 工厂方法，根据传入的文件描述符构造一个文件定位信息对象
    public static FileKey create(FileDescriptor fd) throws IOException {
        FileKey fk = new FileKey();
        // 使用底层获取的文件定位信息初始化卷序列号与文件ID信息
        fk.init(fd);
        return fk;
    }
    
    public int hashCode() {
        return (int) (dwVolumeSerialNumber ^ (dwVolumeSerialNumber >>> 32)) + (int) (nFileIndexHigh ^ (nFileIndexHigh >>> 32)) + (int) (nFileIndexLow ^ (nFileIndexHigh >>> 32));
    }
    
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        
        if(!(obj instanceof FileKey)) {
            return false;
        }
        
        FileKey other = (FileKey) obj;
        
        return (this.dwVolumeSerialNumber == other.dwVolumeSerialNumber) && (this.nFileIndexHigh == other.nFileIndexHigh) && (this.nFileIndexLow == other.nFileIndexLow);
    }
    
    // 在底层获取文件定位信息
    private static native void initIDs();
    
    // 使用底层获取的文件定位信息初始化卷序列号与文件ID信息
    private native void init(FileDescriptor fd) throws IOException;
    
}
