/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.SelectableChannel;
import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.internal.misc.SharedSecrets;
import sun.security.action.GetPropertyAction;

// File中IO操作分派器的本地实现
class FileDispatcherImpl extends FileDispatcher {
    
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();
    
    /*
     * set to true if fast file transmission (TransmitFile) is enabled
     *
     * 是否开启了文件快速传输机制(Windows上对应TransmitFile()，linux对应sendFile())
     * 默认是关闭的，可以通过设置系统属性：jdk.nio.enableFastFileTransfer来开启
     */
    private static final boolean fastFileTransfer;
    
    
    static {
        // 加载IOUtil类，完成静态初始化
        IOUtil.load();
        fastFileTransfer = isFastFileTransferRequested();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    FileDispatcherImpl() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
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
    
    /*
     * 从文件描述符fd读取数据，并从address指向的本地内存中的position位置开始，填充前len个字节
     *
     * 与read的区别是：
     * read会引起文件指针的移动，且必须从缓冲区首个位置开始填充
     * pread不会引起文件指针的移动，且支持随机填充(从缓冲区position位置处开始填充)
     *
     * 注：由于是随机填充缓冲区，所以该方法通常仅支持File通道，而不支持Socket通道
     */
    int pread(FileDescriptor fd, long address, int len, long position) throws IOException {
        return pread0(fd, address, len, position);
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向文件描述符fd写入数据，数据源是address指向的本地内存中的前len个字节
    int write(FileDescriptor fd, long address, int len) throws IOException {
        return write0(fd, address, len, fdAccess.getAppend(fd));
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
        return writev0(fd, address, len, fdAccess.getAppend(fd));
    }
    
    /*
     * 向文件描述符fd写入数据，数据源是address指向的本地内存中的position位置开始的前len个字节
     *
     * 与write的区别是：
     * write会引起文件指针的移动，且必须从数据源首个位置开始读取
     * pwrite不会引起文件指针的移动，且支持随机读取(从数据源position位置处开始读取)
     *
     * 注：由于是随机读取数据源，所以该方法通常仅支持File通道，而不支持Socket通道
     */
    int pwrite(FileDescriptor fd, long address, int len, long position) throws IOException {
        return pwrite0(fd, address, len, position);
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 传输数据 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 系统是否支持直接从文件通道向socket通道传输数据(windows上默认不支持，类unix上默认支持)
    boolean canTransferToDirectly(SelectableChannel sc) {
        // 当开启了文件快速传输机制，且目标socket通道处于阻塞模式时，该方法返回true
        return fastFileTransfer && sc.isBlocking();
    }
    
    // 系统直接从文件通道向socket通道传输数据时是否需要锁定游标敏感的操作(windows上默认需要，类unix上默认不需要)
    boolean transferToDirectlyNeedsPositionLock() {
        return true;
    }
    
    // 判断是否开启了文件快速传输机制
    static boolean isFastFileTransferRequested() {
        String fileTransferProp = GetPropertyAction.privilegedGetProperty("jdk.nio.enableFastFileTransfer");
        
        boolean enable;
        if("".equals(fileTransferProp)) {
            enable = true;
        } else {
            enable = Boolean.parseBoolean(fileTransferProp);
        }
        
        return enable;
    }
    
    // 是否需要对涉及更改通道游标这种敏感操作加锁，文件通道默认需要加锁
    @Override
    boolean needsPositionLock() {
        return true;
    }
    
    /*▲ 传输数据 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回fd处文件的大小(以字节为单位)
    long size(FileDescriptor fd) throws IOException {
        return size0(fd);
    }
    
    /*
     * 设置或报告文件的游标位置
     * offset==-1时，返回当前文件中的游标位置
     * offset!=-1时，为当前文件设置新游标offset
     */
    long seek(FileDescriptor fd, long offset) throws IOException {
        return seek0(fd, offset);
    }
    
    /*
     * 设置实时同步，即强制将全部待修改数据都应用到磁盘的文件上
     * 所有的现代文件系统都会缓存数据和延迟磁盘文件更新以提高性能。调用force()方法要求文件的所有待定修改立即同步到磁盘
     *
     * metaData为true 时，将文件内容或元数据的每个更新都实时同步到底层设备，类似于RandomAccessFile的rws模式
     * metaData为false时，将文件内容的每个更新都实时同步到底层设备，类似于RandomAccessFile的rwd模式
     *
     * 注：元数据指文件所有者、访问权限、最后一次修改时间等信息。大多数情形下，该信息对数据恢复而言是不重要的。
     */
    int force(FileDescriptor fd, boolean metaData) throws IOException {
        return force0(fd, metaData);
    }
    
    // 用新尺寸size截短/扩展通道(文件)
    int truncate(FileDescriptor fd, long size) throws IOException {
        return truncate0(fd, size);
    }
    
    /*▲ 文件操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 对fd处的文件申请文件锁；加锁范围是pos处起的size个字节；shared指示是否申请共享锁；blocking指示是否阻塞式加锁
    int lock(FileDescriptor fd, boolean blocking, long pos, long size, boolean shared) throws IOException {
        return lock0(fd, blocking, pos, size, shared);
    }
    
    // 对fd处的文件释放文件锁；解锁范围是pos处起的size个字节
    void release(FileDescriptor fd, long pos, long size) throws IOException {
        release0(fd, pos, size);
    }
    
    // 关闭文件描述符(通道)
    void close(FileDescriptor fd) throws IOException {
        fdAccess.close(fd);
    }
    
    /*▲ 文件锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 复制一份文件描述符
    FileDescriptor duplicateForMapping(FileDescriptor fd) throws IOException {
        // on Windows we need to keep a handle to the file
        FileDescriptor result = new FileDescriptor();
        long handle = duplicateHandle(fdAccess.getHandle(fd));
        fdAccess.setHandle(result, handle);
        fdAccess.registerCleanup(result);
        return result;
    }
    
    // 设置DirectIO的相关参数，返回DirectIO的对齐粒度
    int setDirectIO(FileDescriptor fd, String path) {
        int result = -1;
        
        // 目录/文件的路径
        String filePath = path.substring(0, path.lastIndexOf(File.separator));
        
        // 分配非直接缓冲区HeapCharBuffer：将缓冲区建立在JVM的内存中
        CharBuffer buffer = CharBuffer.allocate(filePath.length());
        // 将字符串filePath的写入此缓冲区
        buffer.put(filePath);
        
        try {
            result = setDirect0(fd, buffer);
        } catch(IOException e) {
            throw new UnsupportedOperationException("Error setting up DirectIO", e);
        }
        
        return result;
    }
    
    
    static native int read0(FileDescriptor fd, long address, int len) throws IOException;
    
    static native long readv0(FileDescriptor fd, long address, int len) throws IOException;
    
    static native int pread0(FileDescriptor fd, long address, int len, long position) throws IOException;
    
    static native int write0(FileDescriptor fd, long address, int len, boolean append) throws IOException;
    
    static native long writev0(FileDescriptor fd, long address, int len, boolean append) throws IOException;
    
    static native int pwrite0(FileDescriptor fd, long address, int len, long position) throws IOException;
    
    static native long size0(FileDescriptor fd) throws IOException;
    
    static native long seek0(FileDescriptor fd, long offset) throws IOException;
    
    static native int force0(FileDescriptor fd, boolean metaData) throws IOException;
    
    static native int truncate0(FileDescriptor fd, long size) throws IOException;
    
    static native int lock0(FileDescriptor fd, boolean blocking, long pos, long size, boolean shared) throws IOException;
    
    static native void release0(FileDescriptor fd, long pos, long size) throws IOException;
    
    static native void close0(FileDescriptor fd) throws IOException;
    
    static native long duplicateHandle(long fd) throws IOException;
    
    static native int setDirect0(FileDescriptor fd, CharBuffer buffer) throws IOException;
    
}
