/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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
/*
 * IO操作的分派器，允许不同的平台调用各自的原生方法进行读写操作。
 * 这些IO主要针对TCP/UDP Socket或File的文件描述符。
 */
abstract class NativeDispatcher {
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 从文件描述符fd读取数据，并填充address指向的本地内存中的前len个字节
    abstract int read(FileDescriptor fd, long address, int len) throws IOException;
    
    /*
     * 从文件描述符fd读取数据，并依次填充address指向的本地内存中的前len个缓冲区
     *
     * 注：address指向一个结构体数组，每个结构体数组中都引用了一块本地内存
     * 参见IOVecWrapper类
     *
     * 与read的区别是：read相当于填充一个buffer，而readv相当于填充一个buffer数组
     */
    abstract long readv(FileDescriptor fd, long address, int len) throws IOException;
    
    /*
     * 从文件描述符fd读取数据，并从address指向的本地内存中的position位置开始，填充前len个字节
     *
     * 与read的区别是：
     * read会引起文件指针的移动，且必须从缓冲区首个位置开始填充
     * pread不会引起文件指针的移动，且支持随机填充（从缓冲区position位置处开始填充）
     *
     * 注：由于是随机填充缓冲区，所以该方法通常仅支持File通道，而不支持Socket通道
     */
    int pread(FileDescriptor fd, long address, int len, long position) throws IOException {
        throw new IOException("Operation Unsupported");
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向文件描述符fd写入数据，数据源是address指向的本地内存中的前len个字节
    abstract int write(FileDescriptor fd, long address, int len) throws IOException;
    
    /*
     * 向文件描述符fd写入数据，数据源是address指向的本地内存中的前len个缓冲区
     *
     * 注：address指向一个结构体数组，每个结构体数组中都引用了一块本地内存
     * 参见IOVecWrapper类
     *
     * 与write的区别是：write的数据源相当于一个buffer，而writev的数据源相当于一个buffer数组
     */
    abstract long writev(FileDescriptor fd, long address, int len) throws IOException;
    
    /*
     * 向文件描述符fd写入数据，数据源是address指向的本地内存中的position位置开始的前len个字节
     *
     * 与write的区别是：
     * write会引起文件指针的移动，且必须从数据源首个位置开始读取
     * pwrite不会引起文件指针的移动，且支持随机读取（从数据源position位置处开始读取）
     *
     * 注：由于是随机读取数据源，所以该方法通常仅支持File通道，而不支持Socket通道
     */
    int pwrite(FileDescriptor fd, long address, int len, long position) throws IOException {
        throw new IOException("Operation Unsupported");
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭文件描述符（通道），即释放对文件描述符的引用
    abstract void close(FileDescriptor fd) throws IOException;
    
    /**
     * Prepare the given fd for closing by duping it to a known internal fd that's already closed.
     * This is necessary on some operating systems (Solaris and Linux) to prevent fd recycling.
     */
    // 关闭fd处的Socket通道前的一些准备
    void preClose(FileDescriptor fd) throws IOException {
        // Do nothing by default; this is only needed on Unix
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns {@code true} if pread/pwrite needs to be synchronized with position sensitive methods.
     */
    // 是否需要对涉及更改通道游标这种敏感操作加锁
    boolean needsPositionLock() {
        return false;
    }
    
}
