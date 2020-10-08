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
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * "Portable" implementation of AsynchronousFileChannel for use on operating systems that don't support asynchronous file I/O.
 */
/*
 * 异步文件通道的可移植实现，用于兼容不支持异步文件IO的系统。
 *
 * 例如在linux和mac上，只有SimpleAsynchronousFileChannelImpl这一种实现类。
 */
public class SimpleAsynchronousFileChannelImpl extends AsynchronousFileChannelImpl {
    
    /** Used to make native read and write calls */
    // File中IO操作分派器的抽象实现
    private static final FileDispatcher nd = new FileDispatcherImpl();
    
    /** Thread-safe set of IDs of native threads, for signalling */
    private final NativeThreadSet threads = new NativeThreadSet(2);
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    SimpleAsynchronousFileChannelImpl(FileDescriptor fdObj, boolean reading, boolean writing, ExecutorService executor) {
        super(fdObj, reading, writing, executor);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回指定文件关联的异步文件通道
    public static AsynchronousFileChannel open(FileDescriptor fdo, boolean reading, boolean writing, ThreadPool pool) {
        // Executor is either default or based on pool parameters
        ExecutorService executor = (pool == null) ? DefaultExecutorHolder.defaultExecutor : pool.executor();
        return new SimpleAsynchronousFileChannelImpl(fdo, reading, writing, executor);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 实现异步IO中的读取操作
    @Override
    <A> Future<Integer> implRead(final ByteBuffer dst, final long position, final A attachment, final CompletionHandler<Integer, ? super A> handler) {
        if(position<0) {
            throw new IllegalArgumentException("Negative position");
        }
        
        if(!reading) {
            throw new NonReadableChannelException();
        }
        
        if(dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        
        /* complete immediately if channel closed or no space remaining */
        // 如果通道处于关闭状态，或者缓冲区容量为0，则需要给出异常提示
        if(!isOpen() || (dst.remaining() == 0)) {
            Throwable exc = (isOpen()) ? null : new ClosedChannelException();
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withResult(0, exc);
            }
            
            // 间接处理回调句柄，即将指定的回调句柄handler交给线程池executor处理，不会改变处理线程的递归调用深度
            Invoker.invokeIndirectly(handler, attachment, 0, exc, executor);
            
            return null;
        }
        
        /*
         * 创建一个挂起的任务，表示等待填充任务执行结果。
         * 如果handler不为null，说明显式设置了回调句柄，这种情形下就用不着PendingFuture了，因为可以直接将执行结果交给回调句柄。
         */
        final PendingFuture<Integer, A> future = (handler == null) ? new PendingFuture<Integer, A>(this) : null;
        
        // 构造一个"读取"任务
        Runnable task = new Runnable() {
            public void run() {
                int n = 0;
                Throwable exc = null;
                
                // 向本地线程集新增一个本地线程引用，返回新线程在本地线程集中的索引，这表示通道将阻塞该线程
                int ti = threads.add();
                
                try {
                    // 添加一个读锁
                    begin();
                    
                    do {
                        // 从文件描述符fd（关联的文件）中position位置处读取，读到的内容存入dst后，返回读到的字节数量
                        n = IOUtil.read(fdObj, dst, position, nd);
                        
                        // 不会理会中断标记，会继续读取
                    } while((n == IOStatus.INTERRUPTED) && isOpen());
                    
                    if(n<0 && !isOpen()) {
                        throw new AsynchronousCloseException();
                    }
                } catch(IOException x) {
                    if(!isOpen()) {
                        x = new AsynchronousCloseException();
                    }
                    exc = x;
                } finally {
                    // 移除一个读锁
                    end();
                    // 从本地线程集中移除指定索引处的本地线程引用，表示通道不再阻塞该线程了
                    threads.remove(ti);
                }
                
                // 未设置回调handler时，直接设置执行结果
                if(handler == null) {
                    future.setResult(n, exc);
                    
                    // 如果设置了回调handler，则需要处理回调句柄
                } else {
                    // 直接处理回调句柄，不会改变当前线程的递归调用深度
                    Invoker.invokeUnchecked(handler, attachment, n, exc);
                }
            }
        };
        
        // 让线程池去执行"读取"操作
        executor.execute(task);
        
        return future;
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 实现异步IO中的写入操作
    @Override
    <A> Future<Integer> implWrite(final ByteBuffer src, final long position, final A attachment, final CompletionHandler<Integer, ? super A> handler) {
        if(position<0) {
            throw new IllegalArgumentException("Negative position");
        }
        
        if(!writing) {
            throw new NonWritableChannelException();
        }
        
        // complete immediately if channel is closed or no bytes remaining */
        // 如果通道处于关闭状态，或者待写数据量为0，则需要给出异常提示
        if(!isOpen() || (src.remaining() == 0)) {
            Throwable exc = (isOpen()) ? null : new ClosedChannelException();
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withResult(0, exc);
            }
            
            // 间接处理回调句柄，即将指定的回调句柄handler交给线程池executor处理，不会改变处理线程的递归调用深度
            Invoker.invokeIndirectly(handler, attachment, 0, exc, executor);
            
            return null;
        }
        
        /*
         * 创建一个挂起的任务，表示等待填充任务执行结果。
         * 如果handler不为null，说明显式设置了回调句柄，这种情形下就用不着PendingFuture了，因为可以直接将执行结果交给回调句柄。
         */
        final PendingFuture<Integer, A> future = (handler == null) ? new PendingFuture<Integer, A>(this) : null;
        
        // 构造一个"写入"任务
        Runnable task = new Runnable() {
            public void run() {
                int n = 0;
                Throwable exc = null;
                
                // 向本地线程集新增一个本地线程引用，返回新线程在本地线程集中的索引，这表示通道将阻塞该线程
                int ti = threads.add();
                
                try {
                    // 添加一个读锁
                    begin();
                    
                    do {
                        // 从缓冲区src读取，读到的内容向文件描述符fdObj（关联的文件）中position位置处写入后，返回写入的字节数量
                        n = IOUtil.write(fdObj, src, position, nd);
                    } while((n == IOStatus.INTERRUPTED) && isOpen());
                    
                    if(n<0 && !isOpen()) {
                        throw new AsynchronousCloseException();
                    }
                } catch(IOException x) {
                    if(!isOpen()) {
                        x = new AsynchronousCloseException();
                    }
                    exc = x;
                } finally {
                    // 移除一个读锁
                    end();
                    // 从本地线程集中移除指定索引处的本地线程引用，表示通道不再阻塞该线程了
                    threads.remove(ti);
                }
                
                // 未设置回调handler时，直接设置执行结果
                if(handler == null) {
                    future.setResult(n, exc);
                    
                    // 如果设置了回调handler，则需要处理回调句柄
                } else {
                    // 直接处理回调句柄，不会改变当前线程的递归调用深度
                    Invoker.invokeUnchecked(handler, attachment, n, exc);
                }
            }
        };
        
        // 让线程池去执行"写入"操作
        executor.execute(task);
        
        return future;
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 文件锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 实现异步IO中的加锁操作
    @Override
    <A> Future<FileLock> implLock(final long position, final long size, final boolean shared, final A attachment, final CompletionHandler<FileLock, ? super A> handler) {
        // 共享锁要求通道(文件)可读
        if(shared && !reading) {
            throw new NonReadableChannelException();
        }
        
        // 独占锁要求通道(文件)可写
        if(!shared && !writing) {
            throw new NonWritableChannelException();
        }
        
        // 向文件锁集合中添加一个文件锁
        final FileLockImpl fli = addToFileLockTable(position, size, shared);
        
        // 如果通道已关闭，则无法获取到有效的文件锁，此时需要设置异常信息
        if(fli == null) {
            Throwable exc = new ClosedChannelException();
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withFailure(exc);
            }
            
            // 间接处理回调句柄，即将指定的回调句柄handler交给线程池executor处理，不会改变处理线程的递归调用深度
            Invoker.invokeIndirectly(handler, attachment, null, exc, executor);
            
            return null;
        }
        
        /*
         * 创建一个挂起的任务，表示等待填充任务执行结果。
         * 如果handler不为null，说明显式设置了回调句柄，这种情形下就用不着PendingFuture了，因为可以直接将执行结果交给回调句柄。
         */
        final PendingFuture<FileLock, A> future = (handler == null) ? new PendingFuture<FileLock, A>(this) : null;
        
        // 构造一个"加锁"任务
        Runnable task = new Runnable() {
            public void run() {
                Throwable exc = null;
                
                // 向本地线程集新增一个本地线程引用，返回新线程在本地线程集中的索引，这表示通道将阻塞该线程
                int ti = threads.add();
                
                try {
                    int n;
                    try {
                        // 添加一个读锁
                        begin();
                        
                        do {
                            // 申请文件锁
                            n = nd.lock(fdObj, true, position, size, shared);
                        } while((n == FileDispatcher.INTERRUPTED) && isOpen());
                        
                        if(n != FileDispatcher.LOCKED || !isOpen()) {
                            throw new AsynchronousCloseException();
                        }
                    } catch(IOException e) {
                        // 从文件锁集合中移除文件锁
                        removeFromFileLockTable(fli);
                        
                        if(!isOpen()) {
                            e = new AsynchronousCloseException();
                        }
                        
                        exc = e;
                    } finally {
                        // 移除一个读锁
                        end();
                    }
                } finally {
                    // 从本地线程集中移除指定索引处的本地线程引用，表示通道不再阻塞该线程了
                    threads.remove(ti);
                }
                
                // 未设置回调handler时，直接设置执行结果
                if(handler == null) {
                    future.setResult(fli, exc);
                    
                    // 如果设置了回调handler，则需要处理回调句柄
                } else {
                    // 直接处理回调句柄，不会改变当前线程的递归调用深度
                    Invoker.invokeUnchecked(handler, attachment, fli, exc);
                }
            }
        };
        
        boolean executed = false;
        try {
            // 让线程池去执行"加锁"操作
            executor.execute(task);
            executed = true;
        } finally {
            if(!executed) {
                // 从文件锁集合中移除文件锁
                removeFromFileLockTable(fli);
            }
        }
        
        return future;
    }
    
    // 尝试对指定区域的文件通道进行加锁；如果文件锁申请失败了，则返回null
    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        if(shared && !reading) {
            throw new NonReadableChannelException();
        }
        
        if(!shared && !writing) {
            throw new NonWritableChannelException();
        }
        
        // 向文件锁集合中添加一个文件锁
        FileLockImpl fli = addToFileLockTable(position, size, shared);
        if(fli == null) {
            throw new ClosedChannelException();
        }
        
        int ti = threads.add();
        
        boolean gotLock = false;
        
        try {
            // 添加一个读锁
            begin();
            
            int n;
            
            do {
                // 申请文件锁
                n = nd.lock(fdObj, false, position, size, shared);
            } while((n == FileDispatcher.INTERRUPTED) && isOpen());
            
            // 加锁成功
            if(n == FileDispatcher.LOCKED && isOpen()) {
                gotLock = true;
                return fli;    // lock acquired
            }
            
            // 加锁失败
            if(n == FileDispatcher.NO_LOCK) {
                return null;    // locked by someone else
            }
            
            // 出现了异常
            if(n == FileDispatcher.INTERRUPTED) {
                throw new AsynchronousCloseException();
            }
            
            // should not get here
            throw new AssertionError();
        } finally {
            if(!gotLock) {
                // 从文件锁集合中移除文件锁
                removeFromFileLockTable(fli);
            }
            
            // 移除一个读锁
            end();
            
            threads.remove(ti);
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
        int ti = threads.add();
        
        try {
            long n = 0L;
            try {
                // 添加一个读锁
                begin();
                do {
                    n = nd.size(fdObj);
                } while((n == IOStatus.INTERRUPTED) && isOpen());
                return n;
            } finally {
                // 任务是否已完成
                boolean completed = (n >= 0L);
                // 移除一个读锁；如果任务未完成，但是通道已经关闭了，则抛出异常
                end(completed);
            }
        } finally {
            threads.remove(ti);
        }
    }
    
    // 用新尺寸size截短通道(文件)；如果新尺寸比当前通道文件尺寸还大，则无操作
    @Override
    public AsynchronousFileChannel truncate(long size) throws IOException {
        if(size<0L) {
            throw new IllegalArgumentException("Negative size");
        }
        
        if(!writing) {
            throw new NonWritableChannelException();
        }
        
        int ti = threads.add();
        try {
            long n = 0L;
            try {
                // 添加一个读锁
                begin();
                do {
                    n = nd.size(fdObj);
                } while((n == IOStatus.INTERRUPTED) && isOpen());
                
                // truncate file if 'size' less than current size
                if(size<n && isOpen()) {
                    do {
                        n = nd.truncate(fdObj, size);
                    } while((n == IOStatus.INTERRUPTED) && isOpen());
                }
                return this;
            } finally {
                // 任务是否已完成
                boolean completed = (n>0);
                // 移除一个读锁；如果任务未完成，但是通道已经关闭了，则抛出异常
                end(completed);
            }
        } finally {
            threads.remove(ti);
        }
    }
    
    
    // 是否需要实时更新文件的元数据到本地
    @Override
    public void force(boolean metaData) throws IOException {
        int ti = threads.add();
        try {
            int n = 0;
            try {
                // 添加一个读锁
                begin();
                do {
                    n = nd.force(fdObj, metaData);
                } while((n == IOStatus.INTERRUPTED) && isOpen());
            } finally {
                // 任务是否已完成
                boolean completed = (n >= 0L);
                // 移除一个读锁；如果任务未完成，但是通道已经关闭了，则抛出异常
                end(completed);
            }
        } finally {
            threads.remove(ti);
        }
    }
    
    /*▲ 杂项  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 开启/关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭异步通道
    @Override
    public void close() throws IOException {
        // mark channel as closed
        synchronized(fdObj) {
            if(closed) {
                return;     // already closed
            }
            
            closed = true;
            
            // from this point on, if another thread invokes the begin() method then it will throw ClosedChannelException
        }
        
        /* Invalidate and release any locks that we still */
        // 移除当前通道上所有文件锁，并释放其本地内存，最后将其标记为无效
        invalidateAllLocks();
        
        /* signal any threads blocked on this channel*/
        // 唤醒当前所有被占用(阻塞)的本地线程以便关闭操作可以执行下去
        threads.signalAndWait();
        
        /* wait until all async I/O operations have completely gracefully */
        closeLock.writeLock().lock();
        
        try {
            // do nothing
        } finally {
            closeLock.writeLock().unlock();
        }
        
        /* close file */
        // 关闭通道
        nd.close(fdObj);
    }
    
    /*▲ 开启/关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /* lazy initialization of default thread pool for file I/O */
    // 线程池引用
    private static class DefaultExecutorHolder {
        // 异步IO线程池：容量非固定，初始容量默认与处理器数量一致(接受用户的自定义)
        static final ThreadPool pool = ThreadPool.createDefault();
        
        // 异步IO线程池内包装的【任务执行框架】
        static final ExecutorService defaultExecutor = pool.executor();
    }
    
}
