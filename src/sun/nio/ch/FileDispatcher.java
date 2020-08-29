/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.SelectableChannel;

// File中IO操作分派器的抽象实现
abstract class FileDispatcher extends NativeDispatcher {
    
    public static final int NO_LOCK = -1;       // Failed to lock
    public static final int LOCKED = 0;         // Obtained requested lock
    public static final int RET_EX_LOCK = 1;    // Obtained exclusive lock
    public static final int INTERRUPTED = 2;    // Request interrupted
    
    /**
     * Sets or reports this file's position
     * If offset is -1, the current position is returned
     * otherwise the position is set to offset.
     */
    /*
     * 设置或报告文件的游标位置
     * offset==-1时，返回当前文件中的游标位置
     * offset!=-1时，为当前文件设置新游标offset
     */
    abstract long seek(FileDescriptor fd, long offset) throws IOException;
    
    /*
     * 设置实时同步
     * metaData为true 时，将文件内容或元数据的每个更新都实时同步到底层设备，类似于RandomAccessFile的rws模式
     * metaData为false时，将文件内容的每个更新都实时同步到底层设备，类似于RandomAccessFile的rwd模式
     */
    abstract int force(FileDescriptor fd, boolean metaData) throws IOException;
    
    // 用新尺寸size截短/扩展通道(文件)
    abstract int truncate(FileDescriptor fd, long size) throws IOException;
    
    // 返回fd处文件的大小(以字节为单位)
    abstract long size(FileDescriptor fd) throws IOException;
    
    // 对fd处的文件申请文件锁；加锁范围是pos处起的size个字节；shared指示是否申请共享锁；blocking指示是否阻塞式加锁
    abstract int lock(FileDescriptor fd, boolean blocking, long pos, long size, boolean shared) throws IOException;
    
    // 对fd处的文件释放文件锁；解锁范围是pos处起的size个字节
    abstract void release(FileDescriptor fd, long pos, long size) throws IOException;
    
    /**
     * Returns a dup of fd if a file descriptor is required for memory-mapping operations,
     * otherwise returns an invalid FileDescriptor (meaning a newly allocated FileDescriptor)
     */
    // 复制一份文件描述符
    abstract FileDescriptor duplicateForMapping(FileDescriptor fd) throws IOException;
    
    // 系统是否支持直接从文件通道向socket通道传输数据(windows上默认不支持，类unix上默认支持)
    abstract boolean canTransferToDirectly(SelectableChannel sc);
    
    // 系统直接从文件通道向socket通道传输数据时是否需要锁定游标敏感的操作(windows上默认需要，类unix上默认不需要)
    abstract boolean transferToDirectlyNeedsPositionLock();
    
    // 设置DirectIO的相关参数，返回DirectIO的对齐粒度
    abstract int setDirectIO(FileDescriptor fd, String path);
    
}
