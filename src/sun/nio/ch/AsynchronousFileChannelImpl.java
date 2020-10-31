/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base implementation of AsynchronousFileChannel.
 */
// 异步文件通道的抽象实现
abstract class AsynchronousFileChannelImpl extends AsynchronousFileChannel {
    
    /** close support */
    // 读/写锁，多个线程可以同时读，但不能同时写
    protected final ReadWriteLock closeLock = new ReentrantReadWriteLock();
    
    /** file descriptor */
    // 当前文件通道关联的文件描述符
    protected final FileDescriptor fdObj;
    
    /** associated Executor */
    // 为当前文件通道关联的线程池
    protected final ExecutorService executor;
    
    /** indicates if open for reading/writing */
    protected final boolean reading;    // 通道是否可读
    protected final boolean writing;    // 通道是否可写
    
    // 文件锁集合
    private volatile FileLockTable fileLockTable;
    
    // 通道是否已关闭
    protected volatile boolean closed;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    protected AsynchronousFileChannelImpl(FileDescriptor fdObj, boolean reading, boolean writing, ExecutorService executor) {
        this.fdObj = fdObj;
        this.reading = reading;
        this.writing = writing;
        this.executor = executor;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 从当前通道的position位置处读取数据以填充缓冲区dst（读取的字节数量最多填满缓冲区的剩余空间）
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否读取完成，以及获取实际读取到的字节数
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    @Override
    public final Future<Integer> read(ByteBuffer dst, long position) {
        return implRead(dst, position, null, null);
    }
    
    /*
     * 从当前通道的position位置处读取数据以填充缓冲区dst（读取的字节数量最多填满缓冲区的剩余空间）
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际读到的字节数
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    @Override
    public final <A> void read(ByteBuffer dst, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if(handler == null) {
            throw new NullPointerException("'handler' is null");
        }
        
        implRead(dst, position, attachment, handler);
    }
    
    // 实现异步IO中的读取操作
    abstract <A> Future<Integer> implRead(ByteBuffer dst, long position, A attachment, CompletionHandler<Integer, ? super A> handler);
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 从源缓冲区src中读取数据，并将读到的内容写入到当前通道中，position代表起始写入位置
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否写入完成，以及获取实际写入的字节数
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    @Override
    public final Future<Integer> write(ByteBuffer src, long position) {
        return implWrite(src, position, null, null);
    }
    
    /*
     * 从源缓冲区src中读取数据，并将读到的内容写入到当前通道中，position代表起始写入位置
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际写入的字节数
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    @Override
    public final <A> void write(ByteBuffer src, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if(handler == null) {
            throw new NullPointerException("'handler' is null");
        }
        
        implWrite(src, position, attachment, handler);
    }
    
    // 实现异步IO中的写入操作
    abstract <A> Future<Integer> implWrite(ByteBuffer src, long position, A attachment, CompletionHandler<Integer, ? super A> handler);
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 对当前通道文件进行加锁(文件锁)
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否加锁成功，以及获取到申请到的文件锁
     *
     * 注：此IO操作的结果是成功申请到的文件锁。如果IO操作没成效，则执行结果可以是异常。
     */
    @Override
    public final Future<FileLock> lock(long position, long size, boolean shared) {
        return implLock(position, size, shared, null, null);
    }
    
    /*
     * 对当前通道文件进行加锁(文件锁)
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际写入的字节数
     *
     * 注：此IO操作的结果是成功申请到的文件锁。如果IO操作没成效，则执行结果可以是异常。
     */
    @Override
    public final <A> void lock(long position, long size, boolean shared, A attachment, CompletionHandler<FileLock, ? super A> handler) {
        if(handler == null) {
            throw new NullPointerException("'handler' is null");
        }
        
        implLock(position, size, shared, attachment, handler);
    }
    
    // 实现异步IO中的加锁操作
    abstract <A> Future<FileLock> implLock(long position, long size, boolean shared, A attachment, CompletionHandler<FileLock, ? super A> handler);
    
    
    /**
     * Adds region to lock table
     */
    /*
     * 向文件锁集合中添加一个文件锁
     *
     * position: 锁定内容的起始游标
     * size    : 锁定内容的尺寸
     * shared  : 指示使用共享锁还是独占锁
     */
    protected final FileLockImpl addToFileLockTable(long position, long size, boolean shared) {
        final FileLockImpl fileLock;
        
        try {
            /* like begin() but returns null instead of exception */
            // 添加一个读锁
            closeLock.readLock().lock();
            
            // 如果通道已关闭，直接返回null
            if(closed) {
                return null;
            }
            
            try {
                // 确保文件锁集合已经初始化(懒加载)
                ensureFileLockTableInitialized();
            } catch(IOException x) {
                // should not happen
                throw new AssertionError(x);
            }
            
            // 构造作用于异步文件通道的文件锁
            fileLock = new FileLockImpl(this, position, size, shared);
            
            /* may throw OverlappedFileLockException */
            /*
             * 向当前文件锁集合添加一个文件锁
             *
             * 如果后续加锁操作失败了，那么需要移除这个文件锁。
             */
            fileLockTable.add(fileLock);
        } finally {
            // 移除一个读锁
            end();
        }
        
        return fileLock;
    }
    
    // 从文件锁集合中移除文件锁
    protected final void removeFromFileLockTable(FileLockImpl fileLock) {
        fileLockTable.remove(fileLock);
    }
    
    // 确保文件锁集合已经初始化(懒加载)
    final void ensureFileLockTableInitialized() throws IOException {
        // 双重检查锁
        if(fileLockTable == null) {
            synchronized(this) {
                if(fileLockTable == null) {
                    fileLockTable = new FileLockTable(this, fdObj);
                }
            }
        }
    }
    
    // 移除当前通道上所有文件锁，并释放其本地内存，最后将其标记为无效
    final void invalidateAllLocks() throws IOException {
        if(fileLockTable == null) {
            return;
        }
        
        // 移除当前通道上的所有文件锁
        List<FileLock> fileLockList = fileLockTable.removeAll();
        
        // 遍历这些移除掉的文件锁
        for(FileLock fileLock : fileLockList) {
            synchronized(fileLock) {
                // 如果当前锁是有效的，则考虑释放资源，并将其标记为无效
                if(fileLock.isValid()) {
                    FileLockImpl fileLockImpl = (FileLockImpl) fileLock;
                    
                    // 释放文件锁
                    implRelease(fileLockImpl);
                    
                    // 标记当前的文件锁失效
                    fileLockImpl.invalidate();
                }
            }
        }
    }
    
    
    /**
     * Invoked by FileLockImpl to release the given file lock and remove it from the lock table.
     */
    // 释放指定的文件锁
    final void release(FileLockImpl fileLock) throws IOException {
        try {
            // 添加一个读锁
            begin();
            
            // 释放文件锁
            implRelease(fileLock);
            
            // 从文件锁集合中移除文件锁
            removeFromFileLockTable(fileLock);
        } finally {
            // 移除一个读锁
            end();
        }
    }
    
    /**
     * Releases the given file lock.
     */
    // 释放文件锁
    protected abstract void implRelease(FileLockImpl fileLock) throws IOException;
    
    /*▲ 文件锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断通道是否处于开启状态
    @Override
    public final boolean isOpen() {
        return !closed;
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 开始/结束 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Marks the beginning of an I/O operation.
     *
     * @throws ClosedChannelException If channel is closed
     */
    // 添加一个读锁
    protected final void begin() throws IOException {
        closeLock.readLock().lock();
        if(closed) {
            throw new ClosedChannelException();
        }
    }
    
    /**
     * Marks the end of an I/O operation.
     */
    // 移除一个读锁
    protected final void end() {
        closeLock.readLock().unlock();
    }
    
    /**
     * Marks end of I/O operation
     */
    // 移除一个读锁；如果任务未完成，但是通道已经关闭了，则抛出异常
    protected final void end(boolean completed) throws IOException {
        // 移除一个读锁
        end();
        
        // 如果任务未完成，但是通道已经关闭了，则抛出异常
        if(!completed && !isOpen()) {
            throw new AsynchronousCloseException();
        }
    }
    
    /*▲ 开始/结束 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    final ExecutorService executor() {
        return executor;
    }
    
}
