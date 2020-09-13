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

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.internal.misc.JavaNioAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.ref.Cleaner;
import jdk.internal.ref.CleanerFactory;

// 文件通道的本地实现
public class FileChannelImpl extends FileChannel {
    
    /** Memory allocation size for mapping buffers */
    // 系统分页大小
    private static final long allocationGranularity;
    
    /** Access to FileDescriptor internals */
    // FileDescriptor类的后门
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();
    
    /** Thread-safe set of IDs of native threads, for signalling */
    private final NativeThreadSet threads = new NativeThreadSet(2);
    
    /** Lock for operations involving position and size */
    // 对涉及更改通道游标这种敏感操作加锁
    private final Object positionLock = new Object();
    
    /** keeps track of locks on this file */
    // 文件锁集合(单例)
    private volatile FileLockTable fileLockTable;
    
    /** Used to make native read and write calls */
    private final FileDispatcher nd;    // 文件操作分派器
    
    /** File descriptor */
    private final FileDescriptor fd;    // 当前通道(文件)的文件描述符(一对一)
    
    // File access mode (immutable)
    private final boolean writable; // 通道是否可写
    private final boolean readable; // 通道是否可读
    
    /** Required to prevent finalization of creating stream (immutable) */
    private final Object parent;    // 打开此通道的流/文件
    
    /** The path of the referenced file (null if the parent stream is created with a file descriptor) */
    private final String path;      // 此通道代表的文件的路径
    
    /*
     * 是否使用DirectIO
     * 在一次性传输大量数据时，使用DirectIO可以减少CPU的中断开销，以及消除缓冲区的分配和复制对IO性能的影响
     */
    private final boolean direct;
    
    /** IO alignment value for DirectIO */
    /*
     * DirectIO的对齐粒度
     * 使用DirectIO时，数据要求按块对齐，alignment就是该块的大小
     */
    private final int alignment;
    
    /** Cleanable with an action which closes this channel's file descriptor */
    private final Cleanable closer; // 该通道关联的清理器
    
    /** blocking operations are not interruptible */
    private volatile boolean uninterruptible;   // 设置该通道为忽略中断
    
    /** Maximum size to map when using a mapped buffer */
    private static final long MAPPED_TRANSFER_SIZE = 8L * 1024L * 1024L;    // 8M
    
    private static final int TRANSFER_SIZE = 8192;  // 8KB
    
    /**
     * Assume at first that the underlying kernel supports sendfile();
     * set this to false if we find out later that it doesn't
     */
    // 指示系统内核是否支持通道间直接传输数据(预先假设它是支持的)，如果后续发现不支持，则设置为false
    private static volatile boolean transferSupported = true;
    
    /**
     * Assume that the underlying kernel sendfile() will work if the target fd is a file;
     * set this to false if we find out later that it doesn't
     */
    // 指示系统内核是否支持向文件通道直接写入数据(预先假设它是支持的)，如果后续发现不支持，则设置为false
    private static volatile boolean fileSupported = true;
    
    /**
     * Assume that the underlying kernel sendfile() will work if the target fd is a pipe;
     * set this to false if we find out later that it doesn't
     */
    // 指示系统内核是否支持向管道(本质还是socket通道)直接写入数据(预先假设它是支持的)，如果后续发现不支持，则设置为false
    private static volatile boolean pipeSupported = true;
    
    private static final int MAP_RO = 0;    // 只读映射
    private static final int MAP_RW = 1;    // 读写映射
    private static final int MAP_PV = 2;    // 写时拷贝映射
    
    
    static {
        // 触发IOUtil完成静态初始化(包括加载本地类库)
        IOUtil.load();
        // 获取系统分页大小
        allocationGranularity = initIDs();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private FileChannelImpl(FileDescriptor fd, String path, boolean readable, boolean writable, boolean direct, Object parent) {
        this.fd = fd;
        this.readable = readable;
        this.writable = writable;
        this.parent = parent;
        this.path = path;
        this.direct = direct;
        
        this.nd = new FileDispatcherImpl();
        
        // 如果使用了DirectIO，则需要获取其对齐粒度
        if(direct) {
            assert path != null;
            // 设置DirectIO的相关参数，返回DirectIO的对齐粒度
            this.alignment = nd.setDirectIO(fd, path);
        } else {
            this.alignment = -1;
        }
        
        /*
         * Register a cleaning action if and only if there is no parent as the parent will take care of closing the file descriptor.
         * FileChannel is used by the LambdaMetaFactory so a lambda cannot be used here hence we use a nested class instead.
         */
        // 如果当前通道没有关联流/文件，则需要为该通道关联一个清理器，以清理通道内的文件描述符
        this.closer = parent != null ? null : CleanerFactory.cleaner().register(this, new Closer(fd));
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Used by FileInputStream.getChannel(), FileOutputStream.getChannel and RandomAccessFile.getChannel() */
    // 返回一个文件通道对象
    public static FileChannel open(FileDescriptor fd, String path, boolean readable, boolean writable, boolean direct, Object parent) {
        return new FileChannelImpl(fd, path, readable, writable, direct, parent);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 从当前文件通道(关联的文件)中起始处读取，读到的内容存入dst后，返回读到的字节数量
     * 该方法是一次性地，即已经读完的流不可以重复读取
     */
    public int read(ByteBuffer dst) throws IOException {
        ensureOpen();
        
        if(!readable) {
            throw new NonReadableChannelException();
        }
        
        synchronized(positionLock) {
            // 如果需要使用DirectIO
            if(direct) {
                // 确保position是alignment的整数倍，否则抛异常
                Util.checkChannelPositionAligned(position(), alignment);
            }
            
            int n = 0;
            int ti = -1;
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
                beginBlocking();
                
                ti = threads.add();
                
                // 如果通道已关闭，直接返回
                if(!isOpen()) {
                    return 0;
                }
                
                do {
                    // 从文件描述符fd(关联的文件)中起始处读取，读到的内容存入dst后，返回读到的字节数量
                    n = IOUtil.read(fd, dst, -1, direct, alignment, nd);
                    
                    // 不会理会中断标记，会继续读取
                } while((n == IOStatus.INTERRUPTED) && isOpen());
                
                return IOStatus.normalize(n);
            } finally {
                threads.remove(ti);
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endBlocking(n>0);
                assert IOStatus.check(n);
            }
        }
    }
    
    /*
     * 从当前文件通道(关联的文件)中读取，读到的内容存入dst后，返回读到的字节数量
     * 该方法可重复调用(因为position>=0)，读取的位置是position指定的位置(支持随机读取)
     */
    public int read(ByteBuffer dst, long position) throws IOException {
        if(dst == null) {
            throw new NullPointerException();
        }
        
        // 确保position不是负数
        if(position<0) {
            throw new IllegalArgumentException("Negative position");
        }
        
        if(!readable) {
            throw new NonReadableChannelException();
        }
        
        // 如果需要使用DirectIO
        if(direct) {
            // 确保position是alignment的整数倍，否则抛异常
            Util.checkChannelPositionAligned(position, alignment);
        }
        
        ensureOpen();
        
        if(nd.needsPositionLock()) {
            synchronized(positionLock) {
                return readInternal(dst, position);
            }
        } else {
            return readInternal(dst, position);
        }
    }
    
    /*
     * 从当前文件通道(关联的文件)中position位置处读取，读到的内容存入dst后，返回读到的字节数量
     * 当position==-1时，该方法是一次性地，即已经读完的流不可以重复读取(不支持随机读取)
     * 当position>=0时，该方法可重复调用，读取的位置是position指定的位置(支持随机读取)
     */
    private int readInternal(ByteBuffer dst, long position) throws IOException {
        assert !nd.needsPositionLock() || Thread.holdsLock(positionLock);
        
        int n = 0;
        int ti = -1;
        
        try {
            // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
            beginBlocking();
            
            ti = threads.add();
            
            if(!isOpen()) {
                return -1;
            }
            
            do {
                // 从文件描述符fd(关联的文件)中position位置处读取，读到的内容存入dst后，返回读到的字节数量
                n = IOUtil.read(fd, dst, position, direct, alignment, nd);
                
                // 不会理会中断标记，会继续读取
            } while((n == IOStatus.INTERRUPTED) && isOpen());
            
            return IOStatus.normalize(n);
        } finally {
            threads.remove(ti);
            
            // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
            endBlocking(n>0);
            assert IOStatus.check(n);
        }
    }
    
    
    /*
     * 【散射】从当前文件通道(关联的文件)中读取，读到的内容依次存入dsts中offset处起的length个缓冲区
     * 该方法是一次性地，即已经读完的流不可以重复读取(不支持随机读取)
     */
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        if((offset<0) || (length<0) || (offset>dsts.length - length)) {
            throw new IndexOutOfBoundsException();
        }
        
        ensureOpen();
        
        if(!readable) {
            throw new NonReadableChannelException();
        }
        
        synchronized(positionLock) {
            // 确保从字节对齐的地方开始读
            if(direct) {
                // 确保position是alignment的整数倍，否则抛异常
                Util.checkChannelPositionAligned(position(), alignment);
            }
            
            long n = 0;
            int ti = -1;
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
                beginBlocking();
                
                ti = threads.add();
                
                if(!isOpen()) {
                    return 0;
                }
                
                do {
                    // 从文件描述符fd(关联的文件)中读取，读到的内容依次存入dsts中offset处起的length个缓冲区后，返回读到的字节数量
                    n = IOUtil.read(fd, dsts, offset, length, direct, alignment, nd);
                    
                    // 不会理会中断标记，会继续读取
                } while((n == IOStatus.INTERRUPTED) && isOpen());
                
                return IOStatus.normalize(n);
            } finally {
                threads.remove(ti);
                
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endBlocking(n>0);
                assert IOStatus.check(n);
            }
        }
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 从缓冲区src读取，读到的内容向当前文件通道(关联的文件)中追加写入后，返回写入的字节数量
     * 待写入内容从fd中上次position==-1时写完的末尾追加内容(不支持随机写入)
     */
    public int write(ByteBuffer src) throws IOException {
        ensureOpen();
        
        if(!writable) {
            throw new NonWritableChannelException();
        }
        
        synchronized(positionLock) {
            // 确保向字节对齐的地方开始写
            if(direct) {
                // 确保position是alignment的整数倍，否则抛异常
                Util.checkChannelPositionAligned(position(), alignment);
            }
            
            int n = 0;
            int ti = -1;
            
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
                beginBlocking();
                
                ti = threads.add();
                
                if(!isOpen()) {
                    return 0;
                }
                
                do {
                    // 从缓冲区src读取，读到的内容向文件描述符fd(关联的文件)中追加写入后，返回写入的字节数量
                    n = IOUtil.write(fd, src, -1, direct, alignment, nd);
                } while((n == IOStatus.INTERRUPTED) && isOpen());
                
                return IOStatus.normalize(n);
            } finally {
                threads.remove(ti);
                
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endBlocking(n>0);
                assert IOStatus.check(n);
            }
        }
    }
    
    /*
     * 【聚集】从缓冲区src读取，读到的内容向当前文件通道(关联的文件)中position位置处写入后，返回写入的字节数量
     * 待写入内容从fd的position位置处开始写(支持随机写入)
     */
    public int write(ByteBuffer src, long position) throws IOException {
        if(src == null) {
            throw new NullPointerException();
        }
        
        if(position<0) {
            throw new IllegalArgumentException("Negative position");
        }
        
        if(!writable) {
            throw new NonWritableChannelException();
        }
        
        // 确保向字节对齐的地方开始写
        if(direct) {
            // 确保position是alignment的整数倍，否则抛异常
            Util.checkChannelPositionAligned(position, alignment);
        }
        
        ensureOpen();
        
        // 以下完成写入操作
        if(nd.needsPositionLock()) {
            synchronized(positionLock) {
                return writeInternal(src, position);
            }
        } else {
            return writeInternal(src, position);
        }
    }
    
    /*
     * 从缓冲区src读取，读到的内容向当前文件通道(关联的文件)中position位置处写入后，返回写入的字节数量
     * 当position==-1时，待写入内容从fd中上次position==-1时写完的末尾追加内容(不支持随机写入)
     * 当position>=0时，待写入内容从fd的position位置处开始写(支持随机写入)
     */
    private int writeInternal(ByteBuffer src, long position) throws IOException {
        assert !nd.needsPositionLock() || Thread.holdsLock(positionLock);
        
        int n = 0;
        int ti = -1;
        
        try {
            // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
            beginBlocking();
            
            ti = threads.add();
            
            if(!isOpen()) {
                return -1;
            }
            
            do {
                // 从缓冲区src读取，读到的内容向文件描述符fd(关联的文件)中position位置处写入后，返回写入的字节数量
                n = IOUtil.write(fd, src, position, direct, alignment, nd);
            } while((n == IOStatus.INTERRUPTED) && isOpen());
            
            return IOStatus.normalize(n);
        } finally {
            threads.remove(ti);
            
            // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
            endBlocking(n>0);
            assert IOStatus.check(n);
        }
    }
    
    
    // 从srcs[offset, offset+length-1]中各个缓冲区读取，读到内容向当前文件通道(关联的文件)中起始位置处写入，返回写入的字节数量
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if((offset<0) || (length<0) || (offset>srcs.length - length)) {
            throw new IndexOutOfBoundsException();
        }
        
        ensureOpen();
        
        if(!writable) {
            throw new NonWritableChannelException();
        }
        
        synchronized(positionLock) {
            // 确保向字节对齐的地方开始写
            if(direct) {
                // 确保position是alignment的整数倍，否则抛异常
                Util.checkChannelPositionAligned(position(), alignment);
            }
            
            long n = 0;
            int ti = -1;
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
                beginBlocking();
                
                ti = threads.add();
                
                if(!isOpen()) {
                    return 0;
                }
                
                do {
                    // 从srcs[offset, offset+length-1]中各个缓冲区读取数据，读到的内容向文件描述符fd(关联的文件)中起始位置处写入后，返回写入的字节数量
                    n = IOUtil.write(fd, srcs, offset, length, direct, alignment, nd);
                } while((n == IOStatus.INTERRUPTED) && isOpen());
                
                return IOStatus.normalize(n);
            } finally {
                threads.remove(ti);
                
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endBlocking(n>0);
                assert IOStatus.check(n);
            }
        }
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 发送数据 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 从当前文件通道的position位置起读取count个字节的数据(数量会经过修正)，读到的数据会写入target通道，要求源通道可读，目标通道可写；返回实际传输的字节数量。
     * 注：由于目标通道类型未知，因此将分别尝试直接传输、文件映射、普通NIO这三种方式传输数据
     */
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        ensureOpen();
        
        if(!target.isOpen()) {
            throw new ClosedChannelException();
        }
        
        // 要求源通道可读
        if(!readable) {
            throw new NonReadableChannelException();
        }
        
        // 要求目标通道可写
        if(target instanceof FileChannelImpl && !((FileChannelImpl) target).writable) {
            throw new NonWritableChannelException();
        }
        
        if((position<0) || (count<0)) {
            throw new IllegalArgumentException();
        }
        
        // 获取当前文件通道的字节数量
        long sz = size();
        
        // 游标已经越界
        if(position>sz) {
            return 0;
        }
        
        // 待传输字节数量不能超过Integer.MAX_VALUE
        int icount = (int) Math.min(count, Integer.MAX_VALUE);
        
        // 如果已传输字节数量+待传输字节数量超出了通道字节总量，缩减待传输字节数量为一个合适的值
        if((sz - position)<icount) {
            icount = (int) (sz - position);
        }
        
        long n;
        
        /* Attempt a direct transfer, if the kernel supports it */
        // 1. 尝试直接从当前文件通道传输数据到目标通道，需要系统内核的支持
        if((n = transferToDirectly(position, icount, target)) >= 0) {
            return n;
        }
        
        /* Attempt a mapped transfer, but only to trusted channel types */
        // 2. 尝试通过文件内存映射从当前文件通道向可信的目标通道传输数据
        if((n = transferToTrustedChannel(position, icount, target)) >= 0) {
            return n;
        }
        
        /* Slow path for untrusted targets */
        // 3. 尝试通过NIO的方式从当前文件通道向目标通道传输数据
        return transferToArbitraryChannel(position, icount, target);
    }
    
    // 1. 尝试直接从当前文件通道传输数据到目标通道，需要系统内核的支持
    private long transferToDirectly(long position, int icount, WritableByteChannel target) throws IOException {
        // 如果系统内核不支持通道间直接传输数据(预先假设它是支持的)，直接返回
        if(!transferSupported) {
            return IOStatus.UNSUPPORTED;
        }
        
        // 目标通道的文件描述符
        FileDescriptor targetFD = null;
        
        // 如果目标通道是文件通道
        if(target instanceof FileChannelImpl) {
            // 如果系统内核不支持向文件通道直接写入数据(预先假设它是支持的)，直接返回
            if(!fileSupported) {
                return IOStatus.UNSUPPORTED_CASE;
            }
            
            // 获取目标通道的文件描述符
            targetFD = ((FileChannelImpl) target).fd;
            
            // 如果目标通道是多路复用通道，主要指socket通道
        } else if(target instanceof SelChImpl) {
            /* Direct transfer to pipe causes EINVAL on some configurations */
            // 如果目标通道是管道中的写通道，但系统内核不支持向管道(本质还是socket通道)直接写入数据，直接返回
            if((target instanceof SinkChannelImpl) && !pipeSupported) {
                return IOStatus.UNSUPPORTED_CASE;
            }
            
            /*
             * Platform-specific restrictions.
             * Now there is only one: Direct transfer to non-blocking channel could be forbidden
             */
            SelectableChannel sc = (SelectableChannel) target;
            
            // 如果系统不支持直接从文件通道向socket通道传输数据，直接返回(windows上默认不支持，类unix上默认支持)
            if(!nd.canTransferToDirectly(sc)) {
                return IOStatus.UNSUPPORTED_CASE;
            }
            
            // 获取目标通道的文件描述符
            targetFD = ((SelChImpl) target).getFD();
        }
        
        // 如果目标通道类型未知，直接返回
        if(targetFD == null) {
            return IOStatus.UNSUPPORTED;
        }
        
        // 获取当前通道的文件描述符fd在本地(native层)的引用值
        int thisFDVal = IOUtil.fdVal(fd);
        
        // 获取目标通道的文件描述符targetFD在本地(native层)的引用值
        int targetFDVal = IOUtil.fdVal(targetFD);
        
        // 两值相等，说明是同一个(文件)通道，无需传输，直接返回
        if(thisFDVal == targetFDVal) {
            return IOStatus.UNSUPPORTED;
        }
        
        // 如果系统直接从文件通道向socket通道传输数据时需要锁定游标敏感的操作(windows上默认需要，类unix上默认不需要)
        if(nd.transferToDirectlyNeedsPositionLock()) {
            synchronized(positionLock) {
                // 获取当前文件通道的游标
                long pos = position();
                try {
                    // 直接从当前文件通道传输数据到目标通道
                    return transferToDirectlyInternal(position, icount, target, targetFD);
                } finally {
                    // 数据传输完毕后，恢复当前文件通道的游标
                    position(pos);
                }
            }
        }
        
        // 直接从当前文件通道传输数据到目标通道
        return transferToDirectlyInternal(position, icount, target, targetFD);
    }
    
    // 2. 尝试通过文件内存映射从当前文件通道向可信的目标通道传输数据
    private long transferToTrustedChannel(long position, long count, WritableByteChannel target) throws IOException {
        boolean isSelChImpl = (target instanceof SelChImpl);
        
        if(!((target instanceof FileChannelImpl) || isSelChImpl)) {
            return IOStatus.UNSUPPORTED;
        }
        
        // Trusted target: Use a mapped buffer
        long remaining = count;
        
        // 分批传输数据
        while(remaining>0L) {
            // 本次传输的数据量
            long size = Math.min(remaining, MAPPED_TRANSFER_SIZE);
            
            try {
                // 返回当前文件(通道)的只读映射
                MappedByteBuffer dbb = map(MapMode.READ_ONLY, position, size);
                
                try {
                    /* ## Bug: Closing this channel will not terminate the write */
                    
                    // 向目标通道target中写入当前文件内存映射区dbb中包含的内容
                    int n = target.write(dbb);
                    assert n >= 0;
                    
                    remaining -= n;
                    if(isSelChImpl) {
                        // one attempt to write to selectable channel
                        break;
                    }
                    
                    assert n>0;
                    position += n;
                } finally {
                    // 释放内存映射区
                    unmap(dbb);
                }
            } catch(ClosedByInterruptException e) {
                // target closed by interrupt as ClosedByInterruptException needs to be thrown after closing this channel.
                assert !target.isOpen();
                
                try {
                    close();
                } catch(Throwable suppressed) {
                    e.addSuppressed(suppressed);
                }
                throw e;
            } catch(IOException ioe) {
                // Only throw exception if no bytes have been written
                if(remaining == count) {
                    throw ioe;
                }
                break;
            }
        } // while
        
        return count - remaining;
    }
    
    // 3. 尝试通过NIO的方式从当前文件通道向目标通道传输数据
    private long transferToArbitraryChannel(long position, int icount, WritableByteChannel target) throws IOException {
        // Untrusted target: Use a newly-erased buffer
        int size = Math.min(icount, TRANSFER_SIZE);
        
        // 获取一块容量至少为size个字节的直接缓冲区(每次最多传输8KB)
        ByteBuffer bb = Util.getTemporaryDirectBuffer(size);
        
        long tw = 0;                    // Total bytes written
        long pos = position;
        try {
            // 擦除Buffer中的数据(全部填充为0)
            Util.erase(bb);
            
            // 分批传输数据
            while(tw<icount) {
                // 设置新的上界limit
                bb.limit(Math.min((int) (icount - tw), TRANSFER_SIZE));
                
                // 从当前通道中pos位置起，将数据读到直接缓存区bb中
                int nr = read(bb, pos);
                if(nr<=0) {
                    break;
                }
                
                // 从写模式转入读模式
                bb.flip();
                
                /* ## Bug: Will block writing target if this channel is asynchronously closed */
                // 向目标通道target中写入直接缓冲区bb包含的内容
                int nw = target.write(bb);
                tw += nw;
                if(nw != nr) {
                    break;
                }
                pos += nw;
                bb.clear();
            }
            
            return tw;
        } catch(IOException x) {
            if(tw>0) {
                return tw;
            }
            throw x;
        } finally {
            // 采用FILO的形式(入栈模式)将bb放入Buffer缓存池以待复用
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }
    
    // 直接从当前文件通道传输数据到目标通道，使用条件参见transferToDirectly()中的判断
    private long transferToDirectlyInternal(long position, int icount, WritableByteChannel target, FileDescriptor targetFD) throws IOException {
        assert !nd.transferToDirectlyNeedsPositionLock() || Thread.holdsLock(positionLock);
        
        long n = -1;
        int ti = -1;
        
        try {
            // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
            beginBlocking();
            
            ti = threads.add();
            
            if(!isOpen()) {
                return -1;
            }
            
            do {
                // 直接从文件描述符fd所在的文件通道传输数据到目标通道
                n = transferTo0(fd, position, icount, targetFD);
            } while((n == IOStatus.INTERRUPTED) && isOpen());
            
            // 如果平台不支持文件通道向socket通道传输数据(不一定不支持其他通道)
            if(n == IOStatus.UNSUPPORTED_CASE) {
                if(target instanceof SinkChannelImpl) {
                    pipeSupported = false;
                }
                
                if(target instanceof FileChannelImpl) {
                    fileSupported = false;
                }
                
                return IOStatus.UNSUPPORTED_CASE;
            }
            
            // 如果操作系统不支持通道间直接传输数据(不支持任何通道)
            if(n == IOStatus.UNSUPPORTED) {
                // Don't bother trying again
                transferSupported = false;
                return IOStatus.UNSUPPORTED;
            }
            
            return IOStatus.normalize(n);
        } finally {
            threads.remove(ti);
            
            // 清除之前设置的线程中断回调
            end(n>-1);
        }
    }
    
    /*▲ 发送数据 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 接收数据 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 从源通道src中起始位置处读取count个字节，读到的数据写入当前文件通道的position位置，要求源通道可读，当前文件通道可写；返回实际接收的字节数量。
     * 注：由于本次写入的目标通道就是当前文件通道，所以不需要尝试直接传输技术(不支持直接向文件通道直接传输数据)，这里只需要尝试文件映射和普通NIO这两种方式传输数据
     */
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        ensureOpen();
        
        if(!src.isOpen()) {
            throw new ClosedChannelException();
        }
        
        // 要求目标通道可写
        if(!writable) {
            throw new NonWritableChannelException();
        }
        
        if((position<0) || (count<0)) {
            throw new IllegalArgumentException();
        }
        
        // 写入的位置超出了当前文件通道现有的字节数量，直接返回
        if(position>size()) {
            return 0;
        }
        
        // 如果源通道也是文件通道
        if(src instanceof FileChannelImpl) {
            // 1. 尝试通过文件内存映射从源通道向当前文件通道传输数据
            return transferFromFileChannel((FileChannelImpl) src, position, count);
        }
        
        // 2. 尝试通过NIO的方式从源通道向当前文件通道传输数据
        return transferFromArbitraryChannel(src, position, count);
    }
    
    // 1. 尝试通过文件内存映射从源通道向当前文件通道传输数据
    private long transferFromFileChannel(FileChannelImpl src, long position, long count) throws IOException {
        if(!src.readable) {
            throw new NonReadableChannelException();
        }
        
        synchronized(src.positionLock) {
            // 获取源通道的游标
            long pos = src.position();
            // 修正需要从源通道读取的字节数
            long max = Math.min(count, src.size() - pos);
            
            long remaining = max;
            long p = pos;
            
            while(remaining>0L) {
                // 本次传输的数据量
                long size = Math.min(remaining, MAPPED_TRANSFER_SIZE);
                
                /* ## Bug: Closing this channel will not terminate the write */
                // 返回源通道的只读文件映射内存
                MappedByteBuffer bb = src.map(MapMode.READ_ONLY, p, size);
                
                try {
                    // 向当前通道的position处写入源文件内存映射区bb中包含的数据
                    long n = write(bb, position);
                    assert n>0;
                    p += n;
                    position += n;
                    remaining -= n;
                } catch(IOException ioe) {
                    // Only throw exception if no bytes have been written
                    if(remaining == max) {
                        throw ioe;
                    }
                    break;
                } finally {
                    // 释放内存映射区
                    unmap(bb);
                }
            }
            
            long nwritten = max - remaining;
            
            src.position(pos + nwritten);
            
            return nwritten;
        }
    }
    
    // 2. 尝试通过NIO的方式从源通道向当前文件通道传输数据
    private long transferFromArbitraryChannel(ReadableByteChannel src, long position, long count) throws IOException {
        // Untrusted target: Use a newly-erased buffer
        int size = (int) Math.min(count, TRANSFER_SIZE);
        
        // 获取一块容量至少为size个字节的直接缓冲区(每次最多传输8KB)
        ByteBuffer bb = Util.getTemporaryDirectBuffer(size);
        
        long tw = 0;    // Total bytes written
        long pos = position;
        
        try {
            // 擦除Buffer中的数据(全部填充为0)
            Util.erase(bb);
            
            // 分批传输数据
            while(tw<count) {
                // 设置新的上界limit
                bb.limit((int) Math.min((count - tw), (long) TRANSFER_SIZE));
                
                /* ## Bug: Will block reading src if this channel is asynchronously closed */
                // 从源通道src中读取，读到的内容存入直接缓冲区bb
                int nr = src.read(bb);
                if(nr<=0) {
                    break;
                }
                
                // 从写模式转入读模式
                bb.flip();
                
                // 向当前通道的pos处写入，写入的内容包含在直接缓冲区bb中
                int nw = write(bb, pos);
                tw += nw;
                if(nw != nr) {
                    break;
                }
                pos += nw;
                bb.clear();
            }
            
            return tw;
        } catch(IOException x) {
            if(tw>0) {
                return tw;
            }
            throw x;
        } finally {
            // 采用FILO的形式(入栈模式)将bb放入Buffer缓存池以待复用
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }
    
    /*▲ 接收数据 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 内存映射 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 返回一块文件映射内存(经过了包装，加入了内存清理操作)
     *
     * 例如映射整个通道的内容，可以调用：
     * fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
     *
     * 与文件锁的范围机制不一样，映射文件的范围不应超过文件的实际大小。
     * 如果申请一个超出文件大小的映射，文件会被增大以匹配映射的大小
     */
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        ensureOpen();
        
        if(mode == null) {
            throw new NullPointerException("Mode is null");
        }
        
        if(position<0L) {
            throw new IllegalArgumentException("Negative position");
        }
        
        if(size<0L) {
            throw new IllegalArgumentException("Negative size");
        }
        
        if(position + size<0) {
            throw new IllegalArgumentException("Position + size overflow");
        }
        
        if(size>Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Size exceeds Integer.MAX_VALUE");
        }
        
        int imode = -1;
        if(mode == MapMode.READ_ONLY) {
            imode = MAP_RO;
        } else if(mode == MapMode.READ_WRITE) {
            imode = MAP_RW;
        } else if(mode == MapMode.PRIVATE) {
            imode = MAP_PV;
        }
        
        assert (imode >= 0);
        
        if((mode != MapMode.READ_ONLY) && !writable) {
            throw new NonWritableChannelException();
        }
        
        if(!readable) {
            throw new NonReadableChannelException();
        }
        
        long addr = -1;
        int ti = -1;
        try {
            // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
            beginBlocking();
            
            ti = threads.add();
            
            if(!isOpen()) {
                return null;
            }
            
            long mapSize;
            int pagePosition;
            
            synchronized(positionLock) {
                long filesize;
                
                do {
                    // 返回此文件通道的字节数量
                    filesize = nd.size(fd);
                } while((filesize == IOStatus.INTERRUPTED) && isOpen());
                
                if(!isOpen()) {
                    return null;
                }
                
                // 请求的数据量超出了当前文件的表示范围
                if(filesize<position + size) { // Extend file size
                    if(!writable) {
                        throw new IOException("Channel not open for writing " + "- cannot extend file to required size");
                    }
                    
                    int rv;
                    do {
                        // 扩展当前文件到新尺寸
                        rv = nd.truncate(fd, position + size);
                    } while((rv == IOStatus.INTERRUPTED) && isOpen());
                    
                    if(!isOpen()) {
                        return null;
                    }
                }
                
                // 待传输数据为0
                if(size == 0) {
                    addr = 0;
                    
                    // a valid file descriptor is not required
                    FileDescriptor dummy = new FileDescriptor();
                    
                    if((!writable) || (imode == MAP_RO)) {
                        // 创建一块只读的基于内存的直接字节缓冲区
                        return Util.newMappedByteBufferR(0, 0, dummy, null);
                    } else {
                        // 创建一块可读写的基于内存的直接字节缓冲区
                        return Util.newMappedByteBuffer(0, 0, dummy, null);
                    }
                }
                
                // 用待映射文件通道的游标位置对系统分页大小取余，以便后续的字节对齐操作
                pagePosition = (int) (position % allocationGranularity);
                
                // 修正映射区域的起始游标
                long mapPosition = position - pagePosition;
                
                // 计算映射区域需要的真实容量
                mapSize = size + pagePosition;
                
                try {
                    /* If map0 did not throw an exception, the address is valid */
                    // 创建一块容量为mapSize的内存映射区域
                    addr = map0(imode, mapPosition, mapSize);
                } catch(OutOfMemoryError x) {
                    /* An OutOfMemoryError may indicate that we've exhausted memory so force gc and re-attempt map */
                    // 内存溢出时，gc一下，并再稍后重试映射
                    System.gc();
                    
                    try {
                        Thread.sleep(100);
                    } catch(InterruptedException y) {
                        Thread.currentThread().interrupt();
                    }
                    
                    try {
                        // gc后再次尝试
                        addr = map0(imode, mapPosition, mapSize);
                    } catch(OutOfMemoryError y) {
                        // After a second OOME, fail
                        throw new IOException("Map failed", y);
                    }
                }
            } // synchronized
            
            // On Windows, and potentially other platforms,
            // we need an open file descriptor for some mapping operations.
            FileDescriptor mfd;
            try {
                // 在一些平台上，需要使用打开的文件描述符完成一些映射操作
                mfd = nd.duplicateForMapping(fd);
            } catch(IOException ioe) {
                // 如果出现异常，则销毁指定的内存映射区域
                unmap0(addr, mapSize);
                throw ioe;
            }
            
            assert (IOStatus.checkAll(addr));
            assert (addr % allocationGranularity == 0);
            
            int isize = (int) size;
            
            // 对已创建好的内存映射区域做简单的包装
            Unmapper um = new Unmapper(addr, mapSize, isize, mfd);
            if((!writable) || (imode == MAP_RO)) {
                // 创建一块只读的基于内存的直接字节缓冲区
                return Util.newMappedByteBufferR(isize, addr + pagePosition, mfd, um);
            } else {
                // 创建一块可读写的基于内存的直接字节缓冲区
                return Util.newMappedByteBuffer(isize, addr + pagePosition, mfd, um);
            }
        } finally {
            threads.remove(ti);
            
            // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
            endBlocking(IOStatus.checkAll(addr));
        }
    }
    
    // 释放内存映射区
    private static void unmap(MappedByteBuffer bb) {
        Cleaner cl = ((DirectBuffer) bb).cleaner();
        if(cl != null) {
            cl.clean();
        }
    }
    
    /*▲ 内存映射 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 通道操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回此通道(文件)的游标位置
    public long position() throws IOException {
        ensureOpen();
        
        synchronized(positionLock) {
            long p = -1;
            int ti = -1;
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
                beginBlocking();
                
                ti = threads.add();
                
                if(!isOpen()) {
                    return 0;
                }
                
                // 判断是否处于追加模式(向通道写入内容时有用)
                boolean append = fdAccess.getAppend(fd);
                
                do {
                    /* in append-mode then position is advanced to end before writing */
                    // 确定文件游标位置(处于追加模式时，定位到文件末尾以便写入)
                    p = (append) ? nd.size(fd) : nd.seek(fd, -1);
                } while((p == IOStatus.INTERRUPTED) && isOpen());
                
                return IOStatus.normalize(p);
            } finally {
                threads.remove(ti);
                
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endBlocking(p>-1);
                assert IOStatus.check(p);
            }
        }
    }
    
    // 设置此通道(文件)的游标位置
    public FileChannel position(long newPosition) throws IOException {
        ensureOpen();
        
        if(newPosition<0) {
            throw new IllegalArgumentException();
        }
        
        synchronized(positionLock) {
            long p = -1;
            int ti = -1;
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
                beginBlocking();
                
                ti = threads.add();
                
                if(!isOpen()) {
                    return null;
                }
                
                do {
                    // 设置新游标
                    p = nd.seek(fd, newPosition);
                } while((p == IOStatus.INTERRUPTED) && isOpen());
                
                return this;
            } finally {
                threads.remove(ti);
                
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endBlocking(p>-1);
                assert IOStatus.check(p);
            }
        }
    }
    
    // 返回当前文件通道的字节数量
    public long size() throws IOException {
        ensureOpen();
        
        synchronized(positionLock) {
            long s = -1;
            int ti = -1;
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
                beginBlocking();
                
                ti = threads.add();
                
                if(!isOpen()) {
                    return -1;
                }
                
                do {
                    s = nd.size(fd);
                } while((s == IOStatus.INTERRUPTED) && isOpen());
                
                return IOStatus.normalize(s);
            } finally {
                threads.remove(ti);
                
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endBlocking(s>-1);
                assert IOStatus.check(s);
            }
        }
    }
    
    // 用指定的新尺寸newSize截断通道(文件)
    public FileChannel truncate(long newSize) throws IOException {
        ensureOpen();
        
        if(newSize<0) {
            throw new IllegalArgumentException("Negative size");
        }
        
        if(!writable) {
            throw new NonWritableChannelException();
        }
        
        synchronized(positionLock) {
            int rv = -1;
            long p = -1;
            int ti = -1;
            long rp = -1;
            try {
                // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
                beginBlocking();
                
                ti = threads.add();
                
                if(!isOpen()) {
                    return null;
                }
                
                // get current size
                long size;
                do {
                    // 返回此通道(文件)的字节数量
                    size = nd.size(fd);
                } while((size == IOStatus.INTERRUPTED) && isOpen());
                
                if(!isOpen()) {
                    return null;
                }
                
                // get current position
                do {
                    // 获取此通道(文件)当前的游标
                    p = nd.seek(fd, -1);
                } while((p == IOStatus.INTERRUPTED) && isOpen());
                
                if(!isOpen()) {
                    return null;
                }
                
                assert p >= 0;
                
                // 如果给定的大小小于文件当前大小，则截断文件
                if(newSize<size) {
                    do {
                        rv = nd.truncate(fd, newSize);
                    } while((rv == IOStatus.INTERRUPTED) && isOpen());
                    
                    if(!isOpen()) {
                        return null;
                    }
                }
                
                /* if position is beyond new size then adjust it */
                // 如果因为截断文件导致游标越界，则更新游标
                if(p>newSize) {
                    p = newSize;
                }
                
                do {
                    // 设置新游标
                    rp = nd.seek(fd, p);
                } while((rp == IOStatus.INTERRUPTED) && isOpen());
                
                return this;
            } finally {
                threads.remove(ti);
                
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endBlocking(rv>-1);
                assert IOStatus.check(rv);
            }
        }
    }
    
    /*
     * 设置实时同步
     * metaData为true 时，将文件内容或元数据的每个更新都实时同步到底层设备，类似于RandomAccessFile的rws模式
     * metaData为false时，将文件内容的每个更新都实时同步到底层设备，类似于RandomAccessFile的rwd模式
     */
    // 设置更新文件内容时，是否实时同步其元数据
    public void force(boolean metaData) throws IOException {
        ensureOpen();
        
        int rv = -1;
        int ti = -1;
        
        try {
            // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
            beginBlocking();
            
            ti = threads.add();
            
            if(!isOpen()) {
                return;
            }
            
            do {
                rv = nd.force(fd, metaData);
            } while((rv == IOStatus.INTERRUPTED) && isOpen());
        } finally {
            threads.remove(ti);
            
            // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
            endBlocking(rv>-1);
            assert IOStatus.check(rv);
        }
    }
    
    // 设置该通道为忽略中断
    public void setUninterruptible() {
        uninterruptible = true;
    }
    
    // 确保通道处于开启状态
    private void ensureOpen() throws IOException {
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
    }
    
    /*▲ 通道操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // FileLockTable单例工厂，用来初始化一个FileLockTable对象
    private FileLockTable fileLockTable() throws IOException {
        if(fileLockTable != null) {
            return fileLockTable;
        }
        
        synchronized(this) {
            if(fileLockTable == null) {
                int ti = threads.add();
                try {
                    ensureOpen();
                    fileLockTable = new FileLockTable(this, fd);
                } finally {
                    threads.remove(ti);
                }
            }
        }
        
        return fileLockTable;
    }
    
    /*
     * 对文件的某段区域加锁，shared决定使用共享锁还是独占锁
     * 如果申请锁定范围是有效的，那么lock()方法会阻塞，直到前面的锁被释放
     * 某些系统不支持共享锁，那么对共享锁的申请会转为独占锁
     * 【阻塞式】申请锁，一直到申请成功
     */
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        ensureOpen();
        
        // 申请共享锁时通道需要可读
        if(shared && !readable) {
            throw new NonReadableChannelException();
        }
        
        // 申请独占锁时通道需要可写
        if(!shared && !writable) {
            throw new NonWritableChannelException();
        }
        
        // 获取文件锁集合(单例)
        FileLockTable fileLockTable = fileLockTable();
        
        // 初始化文件锁信息，并将其加入到文件锁集合当中
        FileLockImpl fl1 = new FileLockImpl(this, position, size, shared);
        fileLockTable.add(fl1);
        
        boolean completed = false;
        int ti = -1;
        
        try {
            // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
            beginBlocking();
            
            ti = threads.add();
            
            // 如果文件通道尚未打开，则无法锁定，直接返回null
            if(!isOpen()) {
                return null;
            }
            
            int n;
            do {
                // 申请文件锁
                n = nd.lock(fd, true, position, size, shared);
            } while((n == FileDispatcher.INTERRUPTED) && isOpen());
            
            if(isOpen()) {
                // 如果得到的是独占锁
                if(n == FileDispatcher.RET_EX_LOCK) {
                    assert shared;
                    
                    // 创建独占锁
                    FileLockImpl fl2 = new FileLockImpl(this, position, size, false);
                    
                    // 使用独占锁fl2替代fl1
                    fileLockTable.replace(fl1, fl2);
                    
                    fl1 = fl2;
                }
                
                // 完成加锁
                completed = true;
            }
        } finally {
            if(!completed) {
                fileLockTable.remove(fl1);
            }
            
            threads.remove(ti);
            
            try {
                // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
                endBlocking(completed);
            } catch(ClosedByInterruptException e) {
                throw new FileLockInterruptionException();
            }
        }
        
        return fl1;
    }
    
    /*
     * 对文件的某段区域加锁，shared决定使用共享锁还是独占锁
     * 某些系统不支持共享锁，那么对共享锁的申请会转为独占锁
     * 【非阻塞式】申请锁，申请失败后返回null
     */
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        ensureOpen();
        
        if(shared && !readable) {
            throw new NonReadableChannelException();
        }
        
        if(!shared && !writable) {
            throw new NonWritableChannelException();
        }
        
        // 获取文件锁集合(单例)
        FileLockTable fileLockTable = fileLockTable();
        
        // 初始化文件锁信息，并将该文件锁加入到文件锁集合当中
        FileLockImpl fli = new FileLockImpl(this, position, size, shared);
        fileLockTable.add(fli);
        
        int result;
        
        int ti = threads.add();
        
        try {
            try {
                ensureOpen();
                // 申请文件锁
                result = nd.lock(fd, false, position, size, shared);
            } catch(IOException e) {
                fileLockTable.remove(fli);
                throw e;
            }
            
            // 没有申请到锁
            if(result == FileDispatcher.NO_LOCK) {
                fileLockTable.remove(fli);
                return null;
            }
            
            // 如果得到的是独占锁
            if(result == FileDispatcher.RET_EX_LOCK) {
                // 原本申请的是共享锁
                assert shared;
                
                FileLockImpl fli2 = new FileLockImpl(this, position, size, false);
                fileLockTable.replace(fli, fli2);
                return fli2;
            }
            
            return fli;
        } finally {
            threads.remove(ti);
        }
    }
    
    // 释放文件锁
    void release(FileLockImpl fli) throws IOException {
        int ti = threads.add();
        
        try {
            ensureOpen();
            nd.release(fd, fli.position(), fli.size());
        } finally {
            threads.remove(ti);
        }
        
        assert fileLockTable != null;
        
        fileLockTable.remove(fli);
    }
    
    /*▲ 文件锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 阻塞 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
    private void beginBlocking() {
        // 如果该通道忽略中断，则直接返回
        if(uninterruptible) {
            return;
        }
        
        // 设置一个线程中断回调，以便可以利用"中断"操作来退出阻塞
        begin();
    }
    
    // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
    private void endBlocking(boolean completed) throws AsynchronousCloseException {
        // 如果该通道忽略中断，则直接返回
        if(uninterruptible) {
            return;
        }
        
        // 清除之前设置的线程中断回调
        end(completed);
    }
    
    /*▲ 阻塞 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将通道标记为关闭状态后，再调用此方法执行资源清理操作
    protected void implCloseChannel() throws IOException {
        // 跳过无效的文件描述符
        if(!fd.valid()) {
            return;
        }
        
        // Release and invalidate any locks that we still hold
        if(fileLockTable != null) {
            for(FileLock fl : fileLockTable.removeAll()) {
                synchronized(fl) {
                    if(fl.isValid()) {
                        nd.release(fd, fl.position(), fl.size());
                        ((FileLockImpl) fl).invalidate();
                    }
                }
            }
        }
        
        /* signal any threads blocked on this channel */
        // 唤醒当前所有被占用(阻塞)的本地线程以便关闭操作可以执行下去
        threads.signalAndWait();
        
        if(parent != null) {
            /*
             * Close the fd via the parent stream's close method.  The parent
             * will reinvoke our close method, which is defined in the
             * superclass AbstractInterruptibleChannel, but the isOpen logic in
             * that method will prevent this method from being reinvoked.
             */
            ((Closeable) parent).close();
        } else if(closer != null) {
            // Perform the cleaning action so it is not redone when
            // this channel becomes phantom reachable.
            try {
                closer.clean();
            } catch(UncheckedIOException uioe) {
                throw uioe.getCause();
            }
        } else {
            fdAccess.close(fd);
        }
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /** Caches fieldIDs */
    // 获取系统分页大小
    private static native long initIDs();
    
    /** Transfers from src to dst, or returns -2 if kernel can't do that */
    /*
     * 直接从文件描述符fd所在的文件通道传输数据到目标通道
     * Windows上使用TransmitFile()，linux使用sendFile()
     */
    private native long transferTo0(FileDescriptor fd, long position, long count, FileDescriptor dst);
    
    /** Creates a new mapping */
    // 创建一块内存映射区域
    private native long map0(int prot, long position, long length) throws IOException;
    
    /** Removes an existing mapping */
    // 销毁指定的内存映射区域
    private static native int unmap0(long address, long length);
    
    
    /**
     * Invoked by sun.management.ManagementFactoryHelper to create the management
     * interface for mapped buffers.
     */
    // 一些钩子方法
    public static JavaNioAccess.BufferPool getMappedBufferPool() {
        return new JavaNioAccess.BufferPool() {
            @Override
            public String getName() {
                return "mapped";
            }
            
            @Override
            public long getCount() {
                return Unmapper.count;
            }
            
            @Override
            public long getTotalCapacity() {
                return Unmapper.totalCapacity;
            }
            
            @Override
            public long getMemoryUsed() {
                return Unmapper.totalSize;
            }
        };
    }
    
    
    // 用于内存映射区域的释放
    private static class Unmapper implements Runnable {
        // keep track of mapped buffer usage
        static volatile int count;
        static volatile long totalSize;
        static volatile long totalCapacity;
        
        // may be required to close file
        private static final NativeDispatcher nd = new FileDispatcherImpl();
        private final long size;
        private final int cap;
        private final FileDescriptor fd;
        private volatile long address;
        
        private Unmapper(long address, long size, int cap, FileDescriptor fd) {
            assert (address != 0);
            this.address = address;
            this.size = size;
            this.cap = cap;
            this.fd = fd;
            
            synchronized(Unmapper.class) {
                count++;
                totalSize += size;
                totalCapacity += cap;
            }
        }
        
        public void run() {
            if(address == 0) {
                return;
            }
            
            // 销毁指定的内存映射区域
            unmap0(address, size);
            address = 0;
            
            // if this mapping has a valid file descriptor then we close it
            if(fd.valid()) {
                try {
                    nd.close(fd);
                } catch(IOException ignore) {
                    // nothing we can do
                }
            }
            
            synchronized(Unmapper.class) {
                count--;
                totalSize -= size;
                totalCapacity -= cap;
            }
        }
    }
    
    // 用于在清理器中关闭文件描述符
    private static class Closer implements Runnable {
        private final FileDescriptor fd;
        
        Closer(FileDescriptor fd) {
            this.fd = fd;
        }
        
        public void run() {
            try {
                // 关闭文件描述符
                fdAccess.close(fd);
            } catch(IOException ioe) {
                // Rethrow as unchecked so the exception can be propagated as needed
                throw new UncheckedIOException("close", ioe);
            }
        }
    }
    
}
