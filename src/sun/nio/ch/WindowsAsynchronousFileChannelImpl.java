/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.util.concurrent.Future;
import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.internal.misc.SharedSecrets;

/**
 * Windows implementation of AsynchronousFileChannel using overlapped I/O.
 */
// 异步文件通道的本地实现(在Windows上的实现)，包含了异步IO操作
public class WindowsAsynchronousFileChannelImpl extends AsynchronousFileChannelImpl implements Iocp.OverlappedChannel, Groupable {
    
    /** Failed to lock */
    static final int NO_LOCK = -1;
    /** Obtained requested lock */
    static final int LOCKED = 0;
    
    /** error when EOF is detected asynchronously */
    private static final int ERROR_HANDLE_EOF = 38;
    
    // FileDescriptor后门
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();
    
    /** Used for force/truncate/size methods */
    // File中IO操作分派器的抽象实现
    private static final FileDispatcher nd = new FileDispatcherImpl();
    
    /** The handle is extracted for use in native methods invoked from this class */
    private final long handle;  // 本地文件引用(指针)
    
    /** I/O completion port (group) */
    private final Iocp iocp;                // 包装对完成端口的使用，用来支持windows上异步IO的实现
    
    private final boolean isDefaultIocp;    // 是否使用了默认的Iocp(参见DefaultIocpHolder)
    
    /** Caches OVERLAPPED structure for each outstanding I/O operation */
    private final PendingIoCache ioCache;   // 重叠IO结构的缓存池：用来缓存重叠IO结构，以便复用
    
    /** The key that identifies the channel's association with the I/O port */
    private final int completionKey;        // 完成键
    
    
    static {
        IOUtil.load();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 构造一个异步文件通道对象，主要是将指定文件(包括Socket)的引用handle关联到"完成端口"上，
     * 并在keyToChannel中记录handle所在通道(支持重叠IO结构)的引用。
     *
     * 涉及的完成端口操作：
     *【4】将通道channel与完成端口handle关联起来。
     *　 　这样的话，当作用在channel上的IO操作结束后，才能通知到绑定的完成端口。
     */
    private WindowsAsynchronousFileChannelImpl(FileDescriptor fdObj, boolean reading, boolean writing, Iocp iocp, boolean isDefaultIocp) throws IOException {
        super(fdObj, reading, writing, iocp.executor());
        
        // 本地文件句柄(引用)
        this.handle = fdAccess.getHandle(fdObj);
        
        // windows上异步IO的实现
        this.iocp = iocp;
        
        // 是否使用了默认的iocp
        this.isDefaultIocp = isDefaultIocp;
        
        // 重叠IO结构的缓存池
        this.ioCache = new PendingIoCache();
        
        /*
         * 将指定文件(包括Socket)的引用handle关联到"完成端口"上，并在keyToChannel中记录handle所在通道(支持重叠IO结构)的引用。
         * 返回值为与通道channel建立关联的完成键。
         */
        this.completionKey = iocp.associate(this, handle);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 打开一个异步IO通道，此过程中会将工作线程就绪并阻塞，等待新的操作到达并执行完之后，再唤醒工作线程
     *
     * 主要经历了三步：
     * 1.构造异步通道组，实际上是进行完成端口的初始化操作
     * 2.在Java层和本地(native层)批量启动工作线程，以便后续处理已经完成的IO操作
     * 3.将指定文件(包括Socket)的引用handle关联到"完成端口"上
     */
    public static AsynchronousFileChannel open(FileDescriptor fdo, boolean reading, boolean writing, ThreadPool pool) throws IOException {
        Iocp iocp;
        boolean isDefaultIocp;
        
        // 如果未指定线程池
        if(pool == null) {
            // 获取默认的Iocp(已启动其关联的工作线程)
            iocp = DefaultIocpHolder.defaultIocp;
            isDefaultIocp = true;
        } else {
            // 用指定的线程池创建Iocp，并启动其关联的工作线程
            iocp = new Iocp(null, pool).start();
            isDefaultIocp = false;
        }
        
        try {
            // 创建一个异步IO通道
            return new WindowsAsynchronousFileChannelImpl(fdo, reading, writing, iocp, isDefaultIocp);
        } catch(IOException x) {
            // error binding to port so need to close it (if created for this channel)
            if(!isDefaultIocp) {
                iocp.implClose();
            }
            throw x;
        }
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 实现异步IO中的读取操作
    @Override
    <A> Future<Integer> implRead(ByteBuffer dst, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if(!reading) {
            throw new NonReadableChannelException();
        }
        
        if(position<0) {
            throw new IllegalArgumentException("Negative position");
        }
        
        if(dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        
        // 如果通道处于关闭状态，则需要给出异常提示
        if(!isOpen()) {
            Throwable exc = new ClosedChannelException();
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withFailure(exc);
            }
            
            // 处理回调句柄，会视情形进行直接处理或间接处理
            Invoker.invoke(this, handler, attachment, null, exc);
            
            return null;
        }
        
        int pos = dst.position();
        int lim = dst.limit();
        // 计算缓冲区中的剩余容量
        int rem = (pos<=lim ? lim - pos : 0);
        
        // 如果缓冲区已经没有可用容量，则立刻给出运行结果：实际读到了0个字节
        if(rem == 0) {
            // 未设置回调handler时，直接包装运行结果
            if(handler == null) {
                return CompletedFuture.withResult(0);
            }
            
            // 处理回调句柄，会视情形进行直接处理或间接处理
            Invoker.invoke(this, handler, attachment, 0, null);
            
            return null;
        }
        
        /* create Future and task that initiates read */
        // 创建在Java层挂起任务，等待底层执行完之后通知Java层
        PendingFuture<Integer, A> future = new PendingFuture<>(this, handler, attachment);
        
        // 构造一个"读取"任务
        ReadTask<A> readTask = new ReadTask<>(dst, pos, rem, position, future);
        
        // 为future设置上下文，即设置实际需要执行的操作
        future.setContext(readTask);
        
        // 执行"读取"操作，读取结束后，会通知阻塞的工作线程
        readTask.run();
        
        return future;
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 实现异步IO中的写入操作
    <A> Future<Integer> implWrite(ByteBuffer src, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if(!writing) {
            throw new NonWritableChannelException();
        }
        
        if(position<0) {
            throw new IllegalArgumentException("Negative position");
        }
        
        // 如果通道处于关闭状态，则需要给出异常提示
        if(!isOpen()) {
            Throwable exc = new ClosedChannelException();
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withFailure(exc);
            }
            
            // 处理回调句柄，会视情形进行直接处理或间接处理
            Invoker.invoke(this, handler, attachment, null, exc);
            
            return null;
        }
        
        int pos = src.position();
        int lim = src.limit();
        assert (pos<=lim);
        // 计算缓冲区中的剩余数据的数量
        int rem = (pos<=lim ? lim - pos : 0);
        
        // 如果缓冲区已经没有可读数据，则立刻给出运行结果：实际写入了0个字节
        if(rem == 0) {
            // 未设置回调handler时，直接包装运行结果
            if(handler == null) {
                return CompletedFuture.withResult(0);
            }
            
            // 处理回调句柄，会视情形进行直接处理或间接处理
            Invoker.invoke(this, handler, attachment, 0, null);
            
            return null;
        }
        
        /* create Future and task to initiate write */
        // 在Java层挂起任务，等待底层执行完之后通知Java层
        PendingFuture<Integer, A> future = new PendingFuture<>(this, handler, attachment);
        
        // 构造"写入"操作
        WriteTask<A> writeTask = new WriteTask<>(src, pos, rem, position, future);
        
        // 为future设置上下文，即设置实际需要执行的操作
        future.setContext(writeTask);
        
        // 执行"写入"操作，写入结束后，会通知阻塞的工作线程
        writeTask.run();
        
        return future;
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 实现异步IO中的加锁操作
    @Override
    <A> Future<FileLock> implLock(final long position, final long size, final boolean shared, A attachment, final CompletionHandler<FileLock, ? super A> handler) {
        // 共享锁要求通道(文件)可读
        if(shared && !reading) {
            throw new NonReadableChannelException();
        }
        
        // 独占锁要求通道(文件)可写
        if(!shared && !writing) {
            throw new NonWritableChannelException();
        }
        
        // 向文件锁集合中添加一个文件锁
        FileLockImpl fileLock = addToFileLockTable(position, size, shared);
        
        // 如果通道已关闭，则无法获取到有效的文件锁，此时需要设置异常信息
        if(fileLock == null) {
            Throwable exc = new ClosedChannelException();
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withFailure(exc);
            }
            
            // 处理回调句柄，会视情形进行直接处理或间接处理
            Invoker.invoke(this, handler, attachment, null, exc);
            return null;
        }
        
        /* create Future and task that will be invoked to acquire lock */
        // 创建在Java层挂起任务，等待底层执行完之后通知Java层
        PendingFuture<FileLock, A> future = new PendingFuture<>(this, handler, attachment);
        
        // 构造一个"加锁"任务
        LockTask<A> lockTask = new LockTask<>(position, fileLock, future);
        
        // 为future设置上下文，即设置实际需要执行的操作
        future.setContext(lockTask);
        
        // 执行"加锁"操作，读取结束后，会通知阻塞的工作线程
        lockTask.run();
        
        return future;
    }
    
    // 尝试对指定区域的文件通道进行加锁；如果文件锁申请失败了，则返回null
    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        // 共享锁要求通道(文件)可读
        if(shared && !reading) {
            throw new NonReadableChannelException();
        }
        
        // 独占锁要求通道(文件)可写
        if(!shared && !writing) {
            throw new NonWritableChannelException();
        }
        
        // 向文件锁集合中添加一个文件锁
        final FileLockImpl fileLock = addToFileLockTable(position, size, shared);
        if(fileLock == null) {
            throw new ClosedChannelException();
        }
        
        boolean gotLock = false;
        try {
            // 添加一个读锁
            begin();
            
            // 申请文件锁
            int res = nd.lock(fdObj, false, position, size, shared);
            // 申请失败返回null
            if(res == NO_LOCK) {
                return null;
            }
            
            // 申请成功
            gotLock = true;
            
            return fileLock;
        } finally {
            // 如果申请文件锁失败了，则需要从文件锁集合中移除文件锁
            if(!gotLock) {
                removeFromFileLockTable(fileLock);
            }
            
            // 移除一个读锁
            end();
        }
    }
    
    
    // 释放文件锁
    @Override
    protected void implRelease(FileLockImpl fli) throws IOException {
        nd.release(fdObj, fli.position(), fli.size());
    }
    
    /*▲ 文件锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回当前通道文件的尺寸
    @Override
    public long size() throws IOException {
        try {
            // 添加一个读锁
            begin();
            return nd.size(fdObj);
        } finally {
            // 移除一个读锁
            end();
        }
    }
    
    // 用新尺寸size截短通道(文件)；如果新尺寸比当前通道文件尺寸还大，则无操作
    @Override
    public AsynchronousFileChannel truncate(long size) throws IOException {
        
        if(size<0) {
            throw new IllegalArgumentException("Negative size");
        }
        
        if(!writing) {
            throw new NonWritableChannelException();
        }
        
        try {
            // 添加一个读锁
            begin();
            
            if(size>nd.size(fdObj)) {
                return this;
            }
            
            nd.truncate(fdObj, size);
        } finally {
            // 移除一个读锁
            end();
        }
        
        return this;
    }
    
    
    // 是否需要实时更新文件的元数据到本地
    @Override
    public void force(boolean metaData) throws IOException {
        try {
            // 添加一个读锁
            begin();
            // 是否需要实时更新文件的元数据到本地
            nd.force(fdObj, metaData);
        } finally {
            // 移除一个读锁
            end();
        }
    }
    
    /*▲ 杂项  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 开启/关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭异步通道
    @Override
    public void close() throws IOException {
        closeLock.writeLock().lock();
        
        try {
            if(closed) {
                return;     // already closed
            }
            closed = true;
        } finally {
            closeLock.writeLock().unlock();
        }
        
        /* invalidate all locks held for this channel */
        // 移除当前通道上所有文件锁，并释放其本地内存，最后将其标记为无效
        invalidateAllLocks();
        
        // close the file */
        // 释放通道文件在本地的引用
        nd.close(fdObj);
        
        /* waits until all I/O operations have completed */
        // 关闭重叠IO结构的缓存池
        ioCache.close();
        
        /* disassociate from port */
        // 解除当前通道与完成键的关联
        iocp.disassociate(completionKey);
        
        // for the non-default group close the port
        if(!isDefaultIocp) {
            // 在异步IO通道组已经为空时，关闭所有工作线程(包括保底线程)
            iocp.detachFromThreadPool();
        }
    }
    
    /*▲ 开启/关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*
     * 从任务结果映射集移除一条记录，并返回移除掉的重叠IO结构缓存池
     * 重叠IO结构被移除下来后，会先尝试将其缓存，缓存池已满时则直接释放重叠IO结构的本地内存
     */
    @Override
    public <V, A> PendingFuture<V, A> getByOverlapped(long overlapped) {
        return ioCache.remove(overlapped);
    }
    
    // 返回异步IO通道组，这是对完成端口的包装
    @Override
    public AsynchronousChannelGroupImpl group() {
        return iocp;
    }
    
    /**
     * Translates Throwable to IOException
     */
    // 转换异常
    private static IOException toIOException(Throwable e) {
        if(e instanceof IOException) {
            if(e instanceof ClosedChannelException) {
                e = new AsynchronousCloseException();
            }
            return (IOException) e;
        }
        
        return new IOException(e);
    }
    
    
    private static native int readFile(long handle, long address, int len, long offset, long overlapped) throws IOException;
    
    private static native int writeFile(long handle, long address, int len, long offset, long overlapped) throws IOException;
    
    private static native int lockFile(long handle, long position, long size, boolean shared, long overlapped) throws IOException;
    
    
    /** Lazy initialization of default I/O completion port */
    // 默认的Iocp引用
    private static class DefaultIocpHolder {
        static final Iocp defaultIocp = defaultIocp();
        
        // 使用默认的线程池构造一个Iocp，并启动其关联的工作线程
        private static Iocp defaultIocp() {
            try {
                // 获取一个异步IO线程池：容量非固定，初始容量默认与处理器数量一致(接受用户的自定义)
                ThreadPool pool = ThreadPool.createDefault();
                
                return new Iocp(null, pool).start();
            } catch(IOException ioe) {
                throw new InternalError(ioe);
            }
        }
    }
    
    /**
     * Task that initiates read operation and handles completion result.
     */
    // 异步IO操作：从通道中读取数据
    private class ReadTask<A> implements Runnable, Iocp.ResultHandler {
        private final ByteBuffer dst;   // 目标缓冲区，用来存储从通道中读取到的数据
        private final int pos;          // 目标缓冲区的当前游标
        private final int rem;          // 目标缓冲区的剩余空间
        private final long position;    // 文件通道的读取起点
        
        /** set to dst if direct; otherwise set to substituted direct buffer */
        private volatile ByteBuffer buf;    // (真正的)目标缓冲区，参见run()中的设置逻辑
        
        private final PendingFuture<Integer, A> future; // 在Java层挂起的任务，等待填充执行结果
        
        ReadTask(ByteBuffer dst, int pos, int rem, long position, PendingFuture<Integer, A> future) {
            this.dst = dst;
            this.pos = pos;
            this.rem = rem;
            this.position = position;
            this.future = future;
        }
        
        // 执行"读取"任务；读取结束后，会通知阻塞的工作线程
        @Override
        public void run() {
            int n = -1;
            long overlapped = 0L;   // OVERLAPPED结构的本地引用
            long address;           // 目标缓冲区的起始可写位置，即向目标缓冲区写入数据时的起始写入位置
            
            /* Substitute a native buffer if not direct */
            // 如果dst已经是直接缓冲区，则直接使用它
            if(dst instanceof DirectBuffer) {
                buf = dst;
                address = ((DirectBuffer) dst).address() + pos;
                
                // 如果dst不是直接缓冲区，则创建一块与dst剩余容量一样大的直接缓冲区来作为目标缓冲区
            } else {
                // 获取一块容量至少为rem个字节的直接缓冲区
                buf = Util.getTemporaryDirectBuffer(rem);
                address = ((DirectBuffer) buf).address();
            }
            
            /*
             * 指示future是否需要挂起
             *
             * 在当前IO操作有成效的时候(情形1)，需要挂起，等待后续填充执行结果。
             * 在当前IO操作没有成效(情形2)，比如收到了EOF消息，或者收到了异常消息，
             * 那么此时就不需要挂起future了，因为当场就可以设置执行结果了。
             */
            boolean pending = false;
            
            try {
                // 添加一个读锁
                begin();
                
                /*
                 * 将future与overlapped建立关联
                 *
                 * 向重叠IO结构缓存池ioCache中存储一个键值对<overlapped, future>，
                 * 即将一个OVERLAPPED结构(如果不存在则新建)与future进行绑定，并返回OVERLAPPED结构的本地引用。
                 */
                overlapped = ioCache.add(future);
                
                /*
                 * 进行本地读操作：从handle指向的文件中读取数据。
                 *
                 * 如果该IO操作正常结束，则会唤醒与handle关联的完成端口所在的工作线程
                 */
                n = readFile(handle, address, rem, position, overlapped);
                
                /* if there are channels in the group then shutdown will continue when the last channel is closed */
                /*
                 * 情形1：当前IO操作有成效
                 *
                 * 本地反馈IOStatus.UNAVAILABLE消息有两种原因：
                 * 1.读取成功
                 * 2.读取失败，失败原因是别的IO操作正在进行，当前IO操作已进入队列排队
                 *
                 * 此种情形下需要挂起future以待后续处理。
                 */
                if(n == IOStatus.UNAVAILABLE) {
                    // 标记future任务已挂起，需要后续填充IO操作的结果
                    pending = true;
                    return;
                }
                
                /*
                 * 情形2.1：当前IO操作没成效，但没有出异常
                 *
                 * 读取失败，失败原因是已经没有内容可读了。
                 * 此时，本地会立即反馈一个EOF(-1)标记到Java层。
                 *
                 * 此种情形下不需要挂起future，可以执行填充执行结果。
                 */
                if(n == IOStatus.EOF) {
                    // 直接将任务执行结果设置为EOF(-1)，且标记本次IO操作(任务)已完成
                    future.setResult(n);
                    
                    /*
                     * 情形2.2：当前IO操作没成效，而且抛出了异常
                     *
                     * 本地产生了其他错误消息的话，直接抛异常。
                     *
                     * 此种情形下不需要挂起future，可以立即填充执行结果。
                     */
                } else {
                    throw new InternalError("Unexpected result: " + n);
                }
                
            } catch(Throwable e) {
                // 解析异常
                IOException exception = toIOException(e);
                // 如果读取过程中出现了异常，则设置任务执行结果为异常
                future.setFailure(exception);
            } finally {
                /*
                 * 如果当前IO操作没成效，那么当场就会被设置任务执行结果。
                 * 在此种情形下，也就没必要挂起future了。
                 * 因此，此处的工作就是将overlapped与相应的future取消关联。
                 */
                if(!pending) {
                    // 如果出状况之前设置了重叠IO结构
                    if(overlapped != 0L) {
                        // 从任务结果映射集移除一条记录；会尝试缓存重叠IO结构，缓存池已满时则直接释放重叠IO结构的本地内存
                        ioCache.remove(overlapped);
                    }
                    
                    // 如果buf是新建的直接缓冲区，此处回收它
                    releaseBufferIfSubstituted();
                }
                
                // 移除一个读锁
                end();
            }
            
            /*
             * 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
             *
             * 对应情形2，只有当IO操作没成效时，此处可以在当前线程(非工作线程)中直接处理回调句柄。
             * 原因是在情形2下，上面已经完成了任务结果的设置(EOF或异常)，
             * 而对回调句柄的处理，必须位于对任务结果的设置之后。
             */
            Invoker.invoke(future);
        }
        
        /**
         * Executed when the I/O has completed
         */
        // 当IO线程执行完"读取"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行的结果
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            // 有效数据存入dst，并更新目标缓冲区的游标(如果dst不是直接缓冲区，则伴随着存储目标数据的动作)
            updatePosition(bytesTransferred);
    
            /* return direct buffer to cache if substituted */
            // 如果buf是新建的直接缓冲区，此处回收它
            releaseBufferIfSubstituted();
    
            /* release waiters and invoke completion handler */
            // 设置任务执行的结果(读取的字节数)
            future.setResult(bytesTransferred);
    
            // 如果允许直接在当前线程中处理回调句柄
            if(canInvokeDirect) {
                // 直接处理future中记录的回调句柄，不会改变当前线程的递归调用深度
                Invoker.invokeUnchecked(future);
            } else {
                // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
                Invoker.invoke(future);
            }
        }
        
        // 当IO线程执行完"读取"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行中的异常信息
        @Override
        public void failed(int error, IOException e) {
            /* if EOF detected asynchronously then it is reported as error */
            // 如果读到了文件结尾
            if(error == ERROR_HANDLE_EOF) {
                completed(-1, false);
                
                // 如果出现了其他异常
            } else {
                /* return direct buffer to cache if substituted */
                // 如果buf是新建的直接缓冲区，此处回收它
                releaseBufferIfSubstituted();
                
                /* release waiters */
                if(isOpen()) {
                    // 如果通道未关闭，则设置给定的异常信息
                    future.setFailure(e);
                } else {
                    // 如果通道已关闭，则设置一个通道已关闭的异常
                    future.setFailure(new AsynchronousCloseException());
                }
                
                // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
                Invoker.invoke(future);
            }
        }
        
        // 如果buf是新建的直接缓冲区，此处回收它
        void releaseBufferIfSubstituted() {
            if(buf != dst) {
                // 采用FILO的形式(入栈模式)将buf放入Buffer缓存池以待复用
                Util.releaseTemporaryDirectBuffer(buf);
            }
        }
        
        // 有效数据存入dst，并更新目标缓冲区的游标(如果dst不是直接缓冲区，则伴随着存储目标数据的动作)
        void updatePosition(int bytesTransferred) {
            /* if the I/O succeeded then adjust buffer position */
            if(bytesTransferred<=0) {
                return;
            }
            
            // 如果dst已经是直接缓冲区，向dst中写入数据后，需要更新dst的游标
            if(buf == dst) {
                try {
                    dst.position(pos + bytesTransferred);
                } catch(IllegalArgumentException x) {
                    // someone has changed the position; ignore
                }
                
                // 如果dst不是直接缓冲区，则将buf中的数据存入dst后，并更新其游标
            } else {
                // had to substitute direct buffer
                buf.position(bytesTransferred).flip();
                try {
                    dst.put(buf);
                } catch(BufferOverflowException x) {
                    // someone has changed the position; ignore
                }
            }
        }
    }
    
    /**
     * Task that initiates write operation and handles completion result.
     */
    // 异步IO操作：向通道中写入数据
    private class WriteTask<A> implements Runnable, Iocp.ResultHandler {
        private final ByteBuffer src;   // 源缓冲区，从这里读取数据写入当前通道
        private final int pos, rem;     // 源缓冲区的当前游标和剩余可读字节数
        private final long position;    // 文件通道的写入起点
        
        // set to src if direct; otherwise set to substituted direct buffer
        private volatile ByteBuffer buf;    // (真正的)源缓冲区，参见run()中的设置逻辑
        
        private final PendingFuture<Integer, A> future; // 在Java层挂起的任务，等待填充执行结果
        
        WriteTask(ByteBuffer src, int pos, int rem, long position, PendingFuture<Integer, A> future) {
            this.src = src;
            this.pos = pos;
            this.rem = rem;
            this.position = position;
            this.future = future;
        }
        
        // 执行"写入"任务，写入结束后，会通知阻塞的工作线程
        @Override
        public void run() {
            int n = -1;
            long overlapped = 0L;   // 为OVERLAPPED结构分配的内存地址
            long address;           // 从源缓冲区读取数据时的起始读取位置
            
            /* Substitute a native buffer if not direct */
            // 如果src已经是直接缓冲区，则直接使用它
            if(src instanceof DirectBuffer) {
                buf = src;
                address = ((DirectBuffer) src).address() + pos;
                
                // 如果src不是直接缓冲区，则创建一块与src剩余可读字节数量一样大的直接缓冲区来作为源缓冲区
            } else {
                // 获取一块容量至少为size个字节的直接缓冲区
                buf = Util.getTemporaryDirectBuffer(rem);
                // 将源缓冲区src的内容全部写入到buf缓冲区
                buf.put(src);
                // 将buf缓冲区切换到读模式
                buf.flip();
                // temporarily restore position as we don't know how many bytes will be written
                src.position(pos);
                address = ((DirectBuffer) buf).address();
            }
            
            try {
                // 添加一个读锁
                begin();
                
                /*
                 * 将future与overlapped建立关联
                 *
                 * 向重叠IO结构缓存池ioCache中存储一个键值对<overlapped, future>，
                 * 即将一个OVERLAPPED结构(如果不存在则新建)与future进行绑定，并返回OVERLAPPED结构的本地引用。
                 */
                overlapped = ioCache.add(future);
                
                /*
                 * 进行本地读操作：向handle指向的文件中写入数据。
                 *
                 * 如果该IO操作正常结束，则会唤醒与handle关联的完成端口所在的工作线程
                 */
                n = writeFile(handle, address, rem, position, overlapped);
                
                /*
                 * 情形1：当前IO操作有成效
                 *
                 * 本地反馈IOStatus.UNAVAILABLE消息有两种原因：
                 * 1.写入成功
                 * 2.写入失败，失败原因是别的IO操作正在进行，当前IO操作已进入队列排队
                 *
                 * 此种情形下需要挂起future以待后续处理。
                 */
                if(n == IOStatus.UNAVAILABLE) {
                    return;
                }
                
                /*
                 * 情形2：当前IO操作没成效，而且抛出了异常
                 *
                 * 此种情形下不需要挂起future，可以立即填充执行结果。
                 */
                throw new InternalError("Unexpected result: " + n);
                
            } catch(Throwable e) {
                // 解析异常
                IOException exception = toIOException(e);
                // 如果写入过程中出现了异常，则设置异常信息
                future.setFailure(exception);
                
                // 如果出状况之前设置了重叠IO结构
                if(overlapped != 0L) {
                    // 从任务结果映射集移除一条记录；会尝试缓存重叠IO结构，缓存池已满时则直接释放重叠IO结构的本地内存
                    ioCache.remove(overlapped);
                }
                
                // 如果buf是新建的直接缓冲区，此处回收它
                releaseBufferIfSubstituted();
            } finally {
                // 移除一个读锁
                end();
            }
            
            /*
             * 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
             * 对应情形2，只有当IO操作没成效时，此处可以在当前线程(非工作线程)中直接处理回调句柄。
             */
            Invoker.invoke(future);
        }
        
        /**
         * Executed when the I/O has completed
         */
        // 当IO线程执行完"写入"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行的结果
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            // (待写入数据位于src中)更新源缓冲区的游标
            updatePosition(bytesTransferred);
    
            /* return direct buffer to cache if substituted */
            // 如果buf是新建的直接缓冲区，此处回收它
            releaseBufferIfSubstituted();
    
            /* release waiters and invoke completion handler */
            // 设置任务执行的结果(写入的字节数)
            future.setResult(bytesTransferred);
    
            // 如果允许直接在当前线程中处理回调句柄
            if(canInvokeDirect) {
                // 直接处理future中记录的回调句柄，不会改变当前线程的递归调用深度
                Invoker.invokeUnchecked(future);
            } else {
                // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
                Invoker.invoke(future);
            }
        }
        
        // 当IO线程执行完"写入"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行中的异常信息
        @Override
        public void failed(int error, IOException e) {
            /* return direct buffer to cache if substituted */
            // 如果buf是新建的直接缓冲区，此处回收它
            releaseBufferIfSubstituted();
            
            /* release waiters and invoker completion handler */
            if(isOpen()) {
                // 如果通道未关闭，则设置给定的异常信息
                future.setFailure(e);
            } else {
                // 如果通道已关闭，则设置一个通道已关闭的异常
                future.setFailure(new AsynchronousCloseException());
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
        
        // 如果buf是新建的直接缓冲区，此处回收它
        void releaseBufferIfSubstituted() {
            if(buf != src) {
                // 采用FILO的形式(入栈模式)将buf放入Buffer缓存池以待复用
                Util.releaseTemporaryDirectBuffer(buf);
            }
        }
        
        // (待写入数据位于src中)更新源缓冲区的游标
        void updatePosition(int bytesTransferred) {
            // if the I/O succeeded then adjust buffer position
            if(bytesTransferred>0) {
                try {
                    src.position(pos + bytesTransferred);
                } catch(IllegalArgumentException x) {
                    // someone has changed the position
                }
            }
        }
    }
    
    /**
     * Task that initiates locking operation and handles completion result.
     */
    // 异步IO操作：对当前通道(文件)加锁
    private class LockTask<A> implements Runnable, Iocp.ResultHandler {
        private final long position;                        // 加锁内容的起始游标
        private final FileLockImpl fileLock;                // 文件锁
        private final PendingFuture<FileLock, A> future;    // 在Java层挂起的任务，等待填充执行结果
        
        LockTask(long position, FileLockImpl fileLock, PendingFuture<FileLock, A> future) {
            this.position = position;
            this.fileLock = fileLock;
            this.future = future;
        }
        
        // 执行"加锁"任务；加锁结束后，会通知阻塞的工作线程
        @Override
        public void run() {
            long overlapped = 0L;
            boolean pending = false;
            
            try {
                // 添加一个读锁
                begin();
                
                /*
                 * 将future与overlapped建立关联
                 *
                 * 向重叠IO结构缓存池ioCache中存储一个键值对<overlapped, future>，
                 * 即将一个OVERLAPPED结构(如果不存在则新建)与future进行绑定，并返回OVERLAPPED结构的本地引用。
                 */
                overlapped = ioCache.add(future);
                
                /* synchronize on result to avoid race with handler thread when lock is acquired immediately */
                synchronized(future) {
                    /*
                     * 进行本地读操作：对handle指向的文件进行加锁操作。
                     *
                     * 如果该IO操作正常结束，则会唤醒与handle关联的完成端口所在的工作线程
                     */
                    int n = lockFile(handle, position, fileLock.size(), fileLock.isShared(), overlapped);
                    
                    /*
                     * 情形1：当前IO操作有成效
                     *
                     * 本地反馈IOStatus.UNAVAILABLE消息有两种原因：
                     * 1.加锁成功
                     * 2.加锁失败，失败原因是别的IO操作正在进行，当前IO操作已进入队列排队
                     *
                     * 此种情形下需要挂起future以待后续处理。
                     */
                    if(n == IOStatus.UNAVAILABLE) {
                        pending = true;
                        return;
                    }
                    
                    /*
                     * 情形2：当前IO操作没成效，而且抛出了异常
                     *
                     * 此种情形下不需要挂起future，可以立即填充执行结果。
                     */
                    future.setResult(fileLock);
                }
                
            } catch(Throwable e) {
                // 如果加锁失败，或者通道关闭，则从文件锁集合中移除文件锁
                removeFromFileLockTable(fileLock);
                
                // 解析异常
                IOException exception = toIOException(e);
                // 如果写入过程中出现了异常，则设置异常信息
                future.setFailure(exception);
            } finally {
                /*
                 * 如果当前IO操作没成效，那么当场就会被设置任务执行结果。
                 * 在此种情形下，也就没必要挂起future了。
                 * 因此，此处的工作就是将overlapped与相应的future取消关联。
                 */
                if(!pending && overlapped != 0L) {
                    // 从任务结果映射集移除一条记录；会尝试缓存重叠IO结构，缓存池已满时则直接释放重叠IO结构的本地内存
                    ioCache.remove(overlapped);
                }
                
                // 移除一个读锁
                end();
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
        
        // 当IO线程执行完"加锁"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行的结果
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            /* release waiters and invoke completion handler */
            // 设置任务执行的结果(已经生效的文件锁)
            future.setResult(fileLock);
            
            // 如果允许直接在当前线程中处理回调句柄
            if(canInvokeDirect) {
                // 直接处理future中记录的回调句柄，不会改变当前线程的递归调用深度
                Invoker.invokeUnchecked(future);
            } else {
                // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
                Invoker.invoke(future);
            }
        }
        
        // 当IO线程执行完"加取"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行中的异常信息
        @Override
        public void failed(int error, IOException e) {
            /* lock not acquired so remove from lock table */
            // 从文件锁集合中移除文件锁
            removeFromFileLockTable(fileLock);
            
            /* release waiters */
            if(isOpen()) {
                // 如果通道未关闭，则设置给定的异常信息
                future.setFailure(e);
            } else {
                // 如果通道已关闭，则设置一个通道已关闭的异常
                future.setFailure(new AsynchronousCloseException());
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
    }
    
}
