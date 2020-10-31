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

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * File-descriptor based I/O utilities that are shared by NIO classes.
 */
// 用于各类通道操作中工具类，主要负责IO，需要与本地（系统内核）中的方法进行交互
public class IOUtil {
    
    /**
     * Max number of iovec structures that readv/writev supports
     */
    static final int IOV_MAX;
    
    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            // 加载本地库
            public Void run() {
                System.loadLibrary("net");
                System.loadLibrary("nio");
                return null;
            }
        });
        
        initIDs();
        
        IOV_MAX = iovMax();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private IOUtil() {
    }                // No instantiation
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 从文件描述符fd（关联的文件/socket）中position位置处开始读取，读到的内容写入dst后，返回读到的字节数量
     * 当position==-1时，该方法是一次性地，即已经读完的流不可以重复读取（不支持随机读取）
     * 当position>=0时，该方法可重复调用，读取的位置是position指定的位置（支持随机读取）
     */
    static int read(FileDescriptor fd, ByteBuffer dst, long position, boolean directIO, int alignment, NativeDispatcher nd) throws IOException {
        if(dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        
        // 如果待写缓冲区已经是直接缓冲区
        if(dst instanceof DirectBuffer) {
            // 直接向目标缓冲区写入从fd中读到的数据
            return readIntoNativeBuffer(fd, dst, position, directIO, alignment, nd);
        }
        
        // 如果目标缓冲区不是直接缓冲区，则需要准备一个直接缓冲区作为中转
        ByteBuffer bb;
        
        // 获取dst中剩余可写空间
        int rem = dst.remaining();
        
        // 如果需要使用DirectIO，则应保证待写数据量按DirectIO的对齐粒度对齐
        if(directIO) {
            // 确保rem是alignment的整数倍，否则抛异常
            Util.checkRemainingBufferSizeAligned(rem, alignment);
            // 获取一块容量至少为size个字节的直接缓冲区（限定了对齐单元的尺寸为alignment）
            bb = Util.getTemporaryAlignedDirectBuffer(rem, alignment);
        } else {
            // 获取一块容量至少为size个字节的直接缓冲区
            bb = Util.getTemporaryDirectBuffer(rem);
        }
        
        try {
            // 从fd读取，向临时创建的直接缓冲bb区写入
            int n = readIntoNativeBuffer(fd, bb, position, directIO, alignment, nd);
            
            // 从写模式切换为读模式
            bb.flip();
            
            if(n>0) {
                // 从临时创建的直接缓冲区bb中读取，向dst缓冲区写入
                dst.put(bb);
            }
            
            return n;
            
        } finally {
            // 采用FILO的形式(入栈模式)将bb放入Buffer缓存池以待复用
            Util.offerFirstTemporaryDirectBuffer(bb);
        }
    }
    
    /*
     * 从文件描述符fd（关联的文件/socket）中position位置处读取，读到的内容写入直接缓冲区bb后，返回读到的字节数量
     * 当position==-1时，该方法是一次性地，即已经读完的流不可以重复读取（不支持随机读取）
     * 当position>=0时，该方法可重复调用，读取的位置是position指定的位置（支持随机读取）
     */
    private static int readIntoNativeBuffer(FileDescriptor fd, ByteBuffer bb, long position, boolean directIO, int alignment, NativeDispatcher nd) throws IOException {
        int pos = bb.position();    // 待写缓冲区游标
        int lim = bb.limit();       // 待写缓冲区上界
        assert (pos<=lim);
        
        // 获取bb中剩余可写空间
        int rem = (pos<=lim ? lim - pos : 0);
        
        // 如果需要使用DirectIO，则应保证待写数据量按DirectIO的对齐粒度对齐
        if(directIO) {
            // 确保(address + pos)是alignment的整数倍，否则抛异常
            Util.checkBufferPositionAligned(bb, pos, alignment);
            
            // 确保rem是alignment的整数倍，否则抛异常
            Util.checkRemainingBufferSizeAligned(rem, alignment);
        }
        
        if(rem == 0) {
            return 0;
        }
        
        int n = 0;
        
        if(position == -1) {
            // 从文件描述符fd读取数据，并填充address指向的本地内存中的前rem个字节
            n = nd.read(fd, ((DirectBuffer) bb).address() + pos, rem);
        } else {
            // 从文件描述符fd读取数据，并从address指向的本地内存中的position位置开始，填充前rem个字节
            n = nd.pread(fd, ((DirectBuffer) bb).address() + pos, rem, position);
        }
        
        // 设置新的游标position
        if(n>0) {
            bb.position(pos + n);
        }
        
        return n;
    }
    
    /*
     * 从文件描述符fd（关联的文件/socket）中position位置处读取，读到的内容写入dst后，返回读到的字节数量
     * 该方法是一次性地(position==-1)，即已经读完的流不可以重复读取（不支持随机读取）
     * 该方法不使用内存分页对齐(alignment==-1)，也不使用DirectIO(directIO==-1)
     */
    static int read(FileDescriptor fd, ByteBuffer dst, long position, NativeDispatcher nd) throws IOException {
        return read(fd, dst, position, false, -1, nd);
    }
    
    
    /*
     * 从文件描述符fd（关联的文件/socket）中读取，读到的内容依次写入dsts中各个缓冲区后，返回读到的字节数量
     * 该方法是一次性地，即已经读完的流不可以重复读取（不支持随机读取）
     * 该方法不使用内存分页对齐(alignment==-1)，也不使用DirectIO(directIO==-1)
     */
    static long read(FileDescriptor fd, ByteBuffer[] dsts, NativeDispatcher nd) throws IOException {
        return read(fd, dsts, 0, dsts.length, false, -1, nd);
    }
    
    /*
     * 从文件描述符fd（关联的文件/socket）中读取，读到的内容依次写入dsts中offset处起的length个缓冲区后，返回读到的字节数量
     * 该方法是一次性地，即已经读完的流不可以重复读取（不支持随机读取）
     * 该方法不使用内存分页对齐(alignment==-1)，也不使用DirectIO
     */
    static long read(FileDescriptor fd, ByteBuffer[] dsts, int offset, int length, NativeDispatcher nd) throws IOException {
        return read(fd, dsts, offset, length, false, -1, nd);
    }
    
    /*
     * 从文件描述符fd（关联的文件/socket）中读取，读到的内容依次写入dsts中offset处起的length个缓冲区后，返回读到的字节数量
     * 该方法是一次性地，即已经读完的流不可以重复读取（不支持随机读取）
     * 是否使用内存分页对齐与DirectIO，取决于alignment和directIO参数
     */
    static long read(FileDescriptor fd, ByteBuffer[] dsts, int offset, int length, boolean directIO, int alignment, NativeDispatcher nd) throws IOException {
        // 创建长度为size的结构体length的数组
        IOVecWrapper vec = IOVecWrapper.get(length);
        
        boolean completed = false;
        int iov_len = 0;
        
        try {
            // Iterate over buffers to populate native iovec array.
            int count = offset + length;
            int i = offset;
            
            // 遍历缓冲区数组，创建底层结构体iovec的数组，以便向其中写入数据
            while(i<count && iov_len<IOV_MAX) {
                ByteBuffer buf = dsts[i];
                
                // 无法向只读Buffer写入数据
                if(buf.isReadOnly()) {
                    throw new IllegalArgumentException("Read-only buffer");
                }
                
                int pos = buf.position();
                int lim = buf.limit();
                
                assert (pos<=lim);
                
                // 获取buf中剩余可写空间
                int rem = pos<=lim ? lim - pos : 0;
                
                // 如果需要使用DirectIO，则应保证待写数据量按照DirectIO的对齐粒度对齐
                if(directIO) {
                    // 确保rem是alignment的整数倍，否则抛异常
                    Util.checkRemainingBufferSizeAligned(rem, alignment);
                }
                
                if(rem>0) {
                    // 在IOVecWrapper中记录buf的存储信息<buf, pos, rem>
                    vec.setBuffer(iov_len, buf, pos, rem);
                    
                    /* allocate shadow buffer to ensure I/O is done with direct buffer */
                    // 如果待写的buf不是直接缓冲区，则创建一块直接缓冲区来加快写入速度
                    if(!(buf instanceof DirectBuffer)) {
                        // 准备影子缓冲区（直接缓冲区）
                        ByteBuffer shadow;
                        
                        if(directIO) {
                            // 获取一块容量至少为rem个字节的直接缓冲区（限定了对齐单元的尺寸为alignment）
                            shadow = Util.getTemporaryAlignedDirectBuffer(rem, alignment);
                        } else {
                            // 获取一块容量至少为rem个字节的直接缓冲区
                            shadow = Util.getTemporaryDirectBuffer(rem);
                        }
                        
                        // 在IOVecWrapper中记录影子缓冲区buf的信息
                        vec.setShadow(iov_len, shadow);
                        
                        // 指向影子缓冲区，以待写入
                        buf = shadow;
                        
                        // 定位到影子缓冲区的起始可写位置
                        pos = shadow.position();
                    }
                    
                    // 记录本地堆内存的基址（指向一块本地内存）
                    vec.putBase(iov_len, ((DirectBuffer) buf).address() + pos);
                    
                    // 记录本地内存容量
                    vec.putLen(iov_len, rem);
                    
                    iov_len++;
                } // if(rem>0)
                
                i++;
            } // while
            
            if(iov_len == 0) {
                return 0L;
            }
            
            /* 至此，结构体iovec的数组（其实就是上面创建的影子缓冲区）已准备好，可以向其中写入数据了 */
            
            /*
             * 从文件描述符fd读取数据，并依次填充vec.address指向的本地内存中的前iov_len个缓冲区
             * 注：vec.address指向一个结构体数组，每个结构体数组中都引用了一块本地内存
             * 参见IOVecWrapper类
             */
            long bytesRead = nd.readv(fd, vec.address, iov_len);
            
            /*
             * 至此，从文件描述符读取的内容已写入了影子缓冲区
             * 接下来，需要将影子缓冲区中的内容读出来，再存到目标缓冲区dsts中
             */
            
            /* Notify the buffers how many bytes were read */
            long left = bytesRead;
            for(int j = 0; j<iov_len; j++) {
                ByteBuffer shadow = vec.getShadow(j);
                
                if(left>0) {
                    ByteBuffer buf = vec.getBuffer(j);
                    int rem = vec.getRemaining(j);
                    int n = (left>rem) ? rem : (int) left;
                    
                    // 如果原先的目标存储本来就是直接缓冲区，这里的shadow将为null
                    if(shadow == null) {
                        int pos = vec.getPosition(j);
                        buf.position(pos + n);
                        
                        // 如果原先的目标存储不是直接缓冲区，则会新建影子缓冲区缓存数据
                    } else {
                        shadow.limit(shadow.position() + n);
                        buf.put(shadow);
                    }
                    
                    left -= n;
                }
                
                if(shadow != null) {
                    // 采用FIFO的形式(入队模式)将shadow写入Buffer缓存池以待复用
                    Util.offerLastTemporaryDirectBuffer(shadow);
                }
                
                vec.clearRefs(j);
            } // for
            
            completed = true;
            
            return bytesRead;
            
            // 读取结束后，尝试缓存新建的影子缓冲区以便复用
        } finally {
            /* if an error occurred then clear refs to buffers and return any shadow buffers to cache */
            // 如果completed = true，说明是正常完成，那么影子缓冲区已经加入到了Buffer缓冲池，没必要重复操作了
            if(!completed) {
                for(int j = 0; j<iov_len; j++) {
                    ByteBuffer shadow = vec.getShadow(j);
                    if(shadow != null) {
                        // 采用FIFO的形式(入队模式)将shadow写入Buffer缓存池以待复用
                        Util.offerLastTemporaryDirectBuffer(shadow);
                    }
                    vec.clearRefs(j);
                }
            }
        }
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 从缓冲区src读取，读到的内容向文件描述符fd（关联的文件/socket）中position位置处写入后，返回写入的字节数量
     * 当position==-1时，待写入内容从fd中上次position==-1时写完的末尾追加内容（不支持随机写入）
     * 当position>=0时，待写入内容从fd的position位置处开始写（支持随机写入）
     */
    static int write(FileDescriptor fd, ByteBuffer src, long position, boolean directIO, int alignment, NativeDispatcher nd) throws IOException {
        
        // 如果源缓冲区已经是直接缓冲区
        if(src instanceof DirectBuffer) {
            // 直接从目标缓冲区读取数据并写入fd
            return writeFromNativeBuffer(fd, src, position, directIO, alignment, nd);
        }
        
        // 如果目标缓冲区不是直接缓冲区，则需要准备一个直接缓冲区作为中转
        ByteBuffer bb;
        
        int pos = src.position();   // 游标
        int lim = src.limit();      // 上界
        
        assert (pos<=lim);
        
        // 获取src中剩余可读内容字节数
        int rem = (pos<=lim ? lim - pos : 0);
        
        // 如果需要使用DirectIO，则应保证待读数据量按DirectIO的对齐粒度对齐
        if(directIO) {
            // 确保rem是alignment的整数倍，否则抛异常
            Util.checkRemainingBufferSizeAligned(rem, alignment);
            // 获取一块容量至少为rem个字节的直接缓冲区（限定了对齐单元的尺寸为alignment）
            bb = Util.getTemporaryAlignedDirectBuffer(rem, alignment);
        } else {
            // 获取一块容量至少为rem个字节的直接缓冲区
            bb = Util.getTemporaryDirectBuffer(rem);
        }
        
        try {
            // 从src缓冲区读取，向临时创建的直接缓冲区bb写入
            bb.put(src);
            
            // 从写模式切换为读模式
            bb.flip();
            
            /* Do not update src until we see how many bytes were written */
            // 在获悉实际写入到fd中多少个字节之前，先别着急更新源缓冲区的游标，而是让其维持原状
            src.position(pos);
            
            // 从临时创建的直接缓冲区bb中读取，向文件描述符fd（关联的文件/socket）中写入
            int n = writeFromNativeBuffer(fd, bb, position, directIO, alignment, nd);
            
            if(n>0) {
                // 更新源缓冲区的游标
                src.position(pos + n);
            }
            
            return n;
        } finally {
            // 采用FILO的形式(入栈模式)将bb放入Buffer缓存池以待复用
            Util.offerFirstTemporaryDirectBuffer(bb);
        }
    }
    
    /*
     * 从缓冲区bb读取，读到的内容向文件描述符fd（关联的文件/socket）中position位置处写入后，返回写入的字节数量
     * 当position==-1时，待写入内容从fd中上次position==-1时写完的末尾追加内容（不支持随机写入）
     * 当position>=0时，待写入内容从fd的position位置处开始写（支持随机写入）
     */
    private static int writeFromNativeBuffer(FileDescriptor fd, ByteBuffer bb, long position, boolean directIO, int alignment, NativeDispatcher nd) throws IOException {
        int pos = bb.position();    // 待读缓冲区游标
        int lim = bb.limit();       // 待读缓冲区上界
        
        assert (pos<=lim);
        
        // 获取bb中剩余可读内容字节数
        int rem = (pos<=lim ? lim - pos : 0);
        
        // 如果需要使用DirectIO，则应保证待读数据量按DirectIO的对齐粒度对齐
        if(directIO) {
            // 确保(address + pos)是alignment的整数倍，否则抛异常
            Util.checkBufferPositionAligned(bb, pos, alignment);
            
            // 确保rem是alignment的整数倍，否则抛异常
            Util.checkRemainingBufferSizeAligned(rem, alignment);
        }
        
        if(rem == 0) {
            return 0;
        }
        
        int n = 0;
        
        if(position == -1) {
            // 向文件描述符fd写入数据，数据源是address指向的本地内存中的前rem个字节
            n = nd.write(fd, ((DirectBuffer) bb).address() + pos, rem);
        } else {
            // 向文件描述符fd写入数据，数据源是address指向的本地内存中的position位置开始的前rem个字节
            n = nd.pwrite(fd, ((DirectBuffer) bb).address() + pos, rem, position);
        }
        
        // 设置新的游标position
        if(n>0) {
            bb.position(pos + n);
        }
        
        return n;
    }
    
    /*
     * 从缓冲区src读取，读到的内容向文件描述符fd（关联的文件/socket）中position位置处写入后，返回写入的字节数量
     * 当position==-1时，待写入内容从fd中上次position==-1时写完的末尾追加内容（不支持随机写入）
     * 当position>=0时，待写入内容从fd的position位置处开始写（支持随机写入）
     * 该方法不使用内存分页对齐(alignment==-1)，也不使用DirectIO(directIO==-1)
     */
    static int write(FileDescriptor fd, ByteBuffer src, long position, NativeDispatcher nd) throws IOException {
        return write(fd, src, position, false, -1, nd);
    }
    
    
    /*
     * 从srcs中各个缓冲区读取数据，读到的内容向文件描述符fd（关联的文件/socket）中起始位置处写入后，返回写入的字节数量
     * 该方法不使用内存分页对齐(alignment==-1)，也不使用DirectIO(directIO==-1)
     */
    static long write(FileDescriptor fd, ByteBuffer[] srcs, NativeDispatcher nd) throws IOException {
        return write(fd, srcs, 0, srcs.length, false, -1, nd);
    }
    
    /*
     * 从srcs[offset, offset+length-1]中各个缓冲区读取数据，读到的内容向文件描述符fd（关联的文件/socket）中起始位置处写入后，返回写入的字节数量
     * 该方法不使用内存分页对齐(alignment==-1)，也不使用DirectIO(directIO==-1)
     */
    static long write(FileDescriptor fd, ByteBuffer[] srcs, int offset, int length, NativeDispatcher nd) throws IOException {
        return write(fd, srcs, offset, length, false, -1, nd);
    }
    
    // 从srcs[offset, offset+length-1]中各个缓冲区读取数据，读到的内容向文件描述符fd（关联的文件/socket）中起始位置处写入后，返回写入的字节数量
    static long write(FileDescriptor fd, ByteBuffer[] srcs, int offset, int length, boolean directIO, int alignment, NativeDispatcher nd) throws IOException {
        // 创建长度为size的结构体length的数组
        IOVecWrapper vec = IOVecWrapper.get(length);
        
        boolean completed = false;
        int iov_len = 0;
        
        try {
            // Iterate over buffers to populate native iovec array.
            int count = offset + length;
            int i = offset;
            
            // 遍历缓冲区数组，创建底层结构体iovec的数组，以便向其中写入数据
            while(i<count && iov_len<IOV_MAX) {
                ByteBuffer buf = srcs[i];
                
                int pos = buf.position();
                int lim = buf.limit();
                
                assert (pos<=lim);
                
                int rem = (pos<=lim ? lim - pos : 0);
                
                // 如果需要使用DirectIO，则应保证待读缓冲区按DirectIO的对齐粒度对齐
                if(directIO) {
                    // 确保rem是alignment的整数倍，否则抛异常
                    Util.checkRemainingBufferSizeAligned(rem, alignment);
                }
                
                // 获取buf中剩余可读数据量
                if(rem>0) {
                    // 在IOVecWrapper中记录buf的存储信息<buf, pos, rem>
                    vec.setBuffer(iov_len, buf, pos, rem);
                    
                    /* allocate shadow buffer to ensure I/O is done with direct buffer */
                    // 如果待读buf不是直接缓冲区，则创建一块直接缓冲区来加快读取速度
                    if(!(buf instanceof DirectBuffer)) {
                        // 准备影子缓冲区（直接缓冲区）
                        ByteBuffer shadow;
                        
                        if(directIO) {
                            // 获取一块容量至少为rem个字节的直接缓冲区（限定了对齐单元的尺寸为alignment）
                            shadow = Util.getTemporaryAlignedDirectBuffer(rem, alignment);
                        } else {
                            // 获取一块容量至少为rem个字节的直接缓冲区
                            shadow = Util.getTemporaryDirectBuffer(rem);
                        }
                        
                        /* 至此，结构体iovec的数组（其实就是上面创建的影子缓冲区）已准备好，可以向其中写入数据了 */
                        
                        // 将源缓冲区buf的内容全部写入到影子缓冲区
                        shadow.put(buf);
                        
                        // 更改写模式到读模式
                        shadow.flip();
                        
                        // 在IOVecWrapper中记录影子缓冲区buf的信息
                        vec.setShadow(iov_len, shadow);
                        
                        /* temporarily restore position in user buffer */
                        // 临时恢复源缓冲区buf的游标，后面知道向文件描述符fd中实际写入多少字节的数据后，再更新源缓冲区buf的游标
                        buf.position(pos);
                        
                        // 指向影子缓冲区，以待读取
                        buf = shadow;
                        
                        // 定位到影子缓冲区的起始可读位置
                        pos = shadow.position();
                    } // while
                    
                    // 记录本地堆内存的基址（指向一块本地内存）
                    vec.putBase(iov_len, ((DirectBuffer) buf).address() + pos);
                    
                    // 记录本地内存容量
                    vec.putLen(iov_len, rem);
                    
                    iov_len++;
                } // if(rem>0)
                
                i++;
            } // while
            
            if(iov_len == 0) {
                return 0L;
            }
            
            /*
             * 至此，从源缓冲区srcs读取的内容已写入了影子缓冲区
             * 接下来，需要将影子缓冲区中的内容读出来，再存到文件描述符fd中
             */
            
            /*
             * 向文件描述符fd写入数据，数据源是address指向的本地内存中的前iov_len个影子缓冲区
             * 注：address指向一个结构体数组，每个结构体数组中都引用了一块本地内存
             * 参见IOVecWrapper类
             */
            long bytesWritten = nd.writev(fd, vec.address, iov_len);
            
            /* Notify the buffers how many bytes were taken */
            long left = bytesWritten;
            
            // 至此，已经可以确定向文件描述符fd中实际写入了多少数据，因此，可以计算源缓冲区srcs的游标了
            for(int j = 0; j<iov_len; j++) {
                if(left>0) {
                    ByteBuffer buf = vec.getBuffer(j);
                    int pos = vec.getPosition(j);
                    int rem = vec.getRemaining(j);
                    int n = (left>rem) ? rem : (int) left;
                    buf.position(pos + n);
                    left -= n;
                }
                
                /* return shadow buffers to buffer pool */
                ByteBuffer shadow = vec.getShadow(j);
                if(shadow != null) {
                    // 采用FIFO的形式(入队模式)将shadow写入Buffer缓存池以待复用
                    Util.offerLastTemporaryDirectBuffer(shadow);
                }
                
                vec.clearRefs(j);
            } // for
            
            completed = true;
            
            return bytesWritten;
            
        } finally {
            /* if an error occurred then clear refs to buffers and return any shadow buffers to cache */
            // 如果completed = true，说明是正常完成，那么影子缓冲区已经加入到了Buffer缓冲池，没必要重复操作了
            if(!completed) {
                for(int j = 0; j<iov_len; j++) {
                    ByteBuffer shadow = vec.getShadow(j);
                    if(shadow != null) {
                        // 采用FIFO的形式(入队模式)将shadow写入Buffer缓存池以待复用
                        Util.offerLastTemporaryDirectBuffer(shadow);
                    }
                    vec.clearRefs(j);
                }
            }
        }
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件描述符 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将本地(native层)的文件描述符fdv包装为一个Java层的文件描述符对象后返回
    public static FileDescriptor newFD(int fdv) {
        FileDescriptor fd = new FileDescriptor();
        setfdVal(fd, fdv);
        return fd;
    }
    
    // 将本地(native层)的文件描述符fdv设置到Java层的文件描述符fd中
    static native void setfdVal(FileDescriptor fd, int fdv);
    
    // 获取Java层的文件描述符fd在本地(native层)的引用值
    public static native int fdVal(FileDescriptor fd);
    
    /*▲ 文件描述符 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Used to trigger loading of native libraries
     */
    // 触发IOUtil完成静态初始化（包括加载本地类库）
    public static void load() {
    }
    
    
    static native void initIDs();
    
    /**
     * Returns two file descriptors for a pipe encoded in a long.
     * The read end of the pipe is returned in the high 32 bits,
     * while the write end is returned in the low 32 bits.
     */
    static native long makePipe(boolean blocking) throws IOException;
    
    // 是否阻塞(blocking)文件描述符fd关联的通道
    public static native void configureBlocking(FileDescriptor fd, boolean blocking) throws IOException;
    
    static native boolean randomBytes(byte[] someBytes);
    
    static native int iovMax();
    
    static native int write1(int fd, byte b) throws IOException;
    
    /**
     * Read and discard all bytes.
     */
    static native boolean drain(int fd) throws IOException;
    
    /**
     * Read and discard at most one byte
     *
     * @return the number of bytes read or IOS_INTERRUPTED
     */
    static native int drain1(int fd) throws IOException;
    
    static native int fdLimit();
    
}
