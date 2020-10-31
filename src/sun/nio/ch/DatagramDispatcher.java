/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Allows different platforms to call different native methods for read and write operations.
 */
// UDP-Socket中IO操作分派器的抽象实现
class DatagramDispatcher extends NativeDispatcher {
    
    static {
        IOUtil.load();
    }
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 从文件描述符fd读取数据，并填充address指向的本地内存中的前len个字节
    int read(FileDescriptor fd, long address, int len) throws IOException {
        return read0(fd, address, len);
    }
    
    /*
     * 从文件描述符fd读取数据，并依次填充address指向的本地内存中的前len个缓冲区
     *
     * 注：address指向一个结构体数组，每个结构体数组中都引用了一块本地内存
     * 参见IOVecWrapper类
     *
     * 与read的区别是：read相当于填充一个buffer，而readv相当于填充一个buffer数组
     */
    long readv(FileDescriptor fd, long address, int len) throws IOException {
        return readv0(fd, address, len);
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向文件描述符fd写入数据，数据源是address指向的本地内存中的前len个字节
    int write(FileDescriptor fd, long address, int len) throws IOException {
        return write0(fd, address, len);
    }
    
    /*
     * 向文件描述符fd写入数据，数据源是address指向的本地内存中的前len个缓冲区
     *
     * 注：address指向一个结构体数组，每个结构体数组中都引用了一块本地内存
     * 参见IOVecWrapper类
     *
     * 与write的区别是：write的数据源相当于一个buffer，而writev的数据源相当于一个buffer数组
     */
    long writev(FileDescriptor fd, long address, int len) throws IOException {
        return writev0(fd, address, len);
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭文件描述符（通道）
    void close(FileDescriptor fd) throws IOException {
        SocketDispatcher.close0(fd);
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    static native int read0(FileDescriptor fd, long address, int len) throws IOException;
    
    static native long readv0(FileDescriptor fd, long address, int len) throws IOException;
    
    static native int write0(FileDescriptor fd, long address, int len) throws IOException;
    
    static native long writev0(FileDescriptor fd, long address, int len) throws IOException;
    
}
