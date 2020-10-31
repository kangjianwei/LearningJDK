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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AcceptPendingException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ShutdownChannelGroupException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import jdk.internal.misc.Unsafe;

/**
 * Windows implementation of AsynchronousServerSocketChannel using overlapped I/O.
 */
// 异步ServerSocket通道的本地实现
class WindowsAsynchronousServerSocketChannelImpl extends AsynchronousServerSocketChannelImpl implements Iocp.OverlappedChannel {
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    /** 2 * (sizeof(SOCKET_ADDRESS) + 16) */
    private static final int DATA_BUFFER_SIZE = 88;
    
    private final long handle;          // [服务端Socket(监听)]在本地(native层)的文件描述符
    
    private final int completionKey;    // 完成键，用来关联通道
    private final Iocp iocp;            // 通道组
    
    /**
     * typically there will be zero, or one I/O operations pending.
     * In rare cases there may be more.
     * These rare cases arise when a sequence of accept operations complete immediately and handled by the initiating thread.
     * The corresponding OVERLAPPED cannot be reused/released until the completion event has been posted.
     */
    private final PendingIoCache ioCache;   // 重叠IO结构的缓存池
    
    /** the data buffer to receive the local/remote socket address */
    // 数据缓冲区，接收新连接发来的第一个数据块，以及存储连接两端的客户端地址和服务端地址
    private final long dataBuffer;
    
    /** flag to indicate that an accept operation is outstanding */
    // 指示通道"正在进行accept操作"(true)还是"已结束accept操作"(false)
    private AtomicBoolean accepting = new AtomicBoolean();
    
    
    static {
        IOUtil.load();
        initIDs();
    }
    
    
    WindowsAsynchronousServerSocketChannelImpl(Iocp iocp) throws IOException {
        super(iocp);
        
        /*
         * 获取Java层的文件描述符fd在本地(native层)的引用值。
         * fd是[服务端Socket(监听)]在Java层的文件描述符。
         */
        long handle = IOUtil.fdVal(fd);
        
        int key;
        try {
            /*
             * 将指定Socket的引用handle关联到"完成端口"上，并在keyToChannel中记录handle所在通道(支持重叠IO结构)的引用。
             * 返回值为与通道channel建立关联的完成键。
             */
            key = iocp.associate(this, handle);
        } catch(IOException x) {
            closesocket0(handle);   // prevent leak
            throw x;
        }
        
        this.handle = handle;
        this.completionKey = key;
        this.iocp = iocp;
        this.ioCache = new PendingIoCache();
        this.dataBuffer = unsafe.allocateMemory(DATA_BUFFER_SIZE);
    }
    
    
    // 实现异步IO中的"accept"操作
    @Override
    Future<AsynchronousSocketChannel> implAccept(Object attachment, final CompletionHandler<AsynchronousSocketChannel, Object> handler) {
        
        // 如果通道处于关闭状态，则需要给出异常提示
        if(!isOpen()) {
            Throwable exc = new ClosedChannelException();
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withFailure(exc);
            }
            
            /*
             * 间接处理回调句柄。
             * 间接的含义不在当前线程中处理回调句柄，
             * 而是将给定的回调句柄交给异步IO线程池去处理。
             */
            Invoker.invokeIndirectly(this, handler, attachment, null, exc);
            
            return null;
        }
        
        // 如果accept操作已被中止，则抛出异常
        if(isAcceptKilled()) {
            throw new RuntimeException("Accept not allowed due to cancellation");
        }
        
        /* ensure channel is bound to local address */
        // 确保已经完成了绑定
        if(localAddress == null) {
            throw new NotYetBoundException();
        }
        
        /*
         * create the socket that will be accepted.
         * The creation of the socket is enclosed by a begin/end for the listener socket to ensure that
         * we check that the listener is open and also to prevent the I/O port
         * from being closed as the new socket is registered.
         */
        WindowsAsynchronousSocketChannelImpl socketChannel = null;
        IOException ioe = null;
        
        try {
            // 添加一个读锁
            begin();
            
            // 构造一个[服务端Socket(通信)]对象，并在accept()成功后为其赋值
            socketChannel = new WindowsAsynchronousSocketChannelImpl(iocp, false);
        } catch(IOException e) {
            ioe = e;
        } finally {
            // 移除一个读锁
            end();
        }
        
        // 如果这里出现异常，则需要反馈异常信息，并退出accept操作
        if(ioe != null) {
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withFailure(ioe);
            }
            
            /*
             * 间接处理回调句柄。
             * 间接的含义不在当前线程中处理回调句柄，
             * 而是将给定的回调句柄交给异步IO线程池去处理。
             */
            Invoker.invokeIndirectly(this, handler, attachment, null, ioe);
            
            return null;
        }
        
        /*
         * need calling context when there is security manager as permission check may be done in a different thread
         * without any application call frames on the stack
         */
        AccessControlContext acc = (System.getSecurityManager() == null) ? null : AccessController.getContext();
        
        // 创建在Java层挂起任务，等待底层执行完之后通知Java层
        PendingFuture<AsynchronousSocketChannel, Object> future = new PendingFuture<>(this, handler, attachment);
        
        // 构造一个"accept"任务
        AcceptTask task = new AcceptTask(socketChannel, acc, future);
        
        // 为future设置上下文，即设置实际需要执行的操作
        future.setContext(task);
        
        /* check and set flag to prevent concurrent accepting */
        // 标记通道"正在进行accept操作"
        if(!accepting.compareAndSet(false, true)) {
            throw new AcceptPendingException();
        }
        
        // 执行"accept"操作，accept结束后，会通知阻塞的工作线程
        task.run();
        
        return future;
    }
    
    // 实现对异步IO通道的关闭操作
    @Override
    void implClose() throws IOException {
        /* close socket (which may cause outstanding accept to be aborted) */
        // 关闭[服务端Socket(监听)]
        closesocket0(handle);
        
        /* waits until the accept operations have completed */
        // 关闭重叠IO结构的缓存池
        ioCache.close();
        
        /* finally disassociate from the completion port */
        // 解除当前通道与完成键的关联
        iocp.disassociate(completionKey);
        
        /* release other resources */
        // 释放数据缓冲区在本地所占的内存
        unsafe.freeMemory(dataBuffer);
    }
    
    
    /*
     * 从任务结果映射集移除一条记录，并返回移除掉的重叠IO结构缓存池
     * 重叠IO结构被移除下来后，会先尝试将其缓存，缓存池已满时则直接释放重叠IO结构的本地内存
     */
    @Override
    public <V, A> PendingFuture<V, A> getByOverlapped(long overlapped) {
        // 从任务结果映射集移除一条记录；会尝试缓存重叠IO结构，缓存池已满时则直接释放重叠IO结构的本地内存
        return ioCache.remove(overlapped);
    }
    
    // 返回异步IO通道组，这是对完成端口的包装
    @Override
    public AsynchronousChannelGroupImpl group() {
        return iocp;
    }
    
    
    private static native void initIDs();
    
    private static native int accept0(long listenSocket, long acceptSocket, long overlapped, long dataBuffer) throws IOException;
    
    private static native void updateAcceptContext(long listenSocket, long acceptSocket) throws IOException;
    
    private static native void closesocket0(long socket) throws IOException;
    
    
    /**
     * Task to initiate accept operation and to handle result.
     */
    // 异步IO操作：进行accept操作，即服务端等待与客户端建立连接
    private class AcceptTask implements Runnable, Iocp.ResultHandler {
        private final WindowsAsynchronousSocketChannelImpl socketChannel;       // [服务端Socket(通信)]通道
        private final PendingFuture<AsynchronousSocketChannel, Object> future;  // 在Java层挂起的任务，等待填充执行结果
        private final AccessControlContext acc; // 权限检查
        
        AcceptTask(WindowsAsynchronousSocketChannelImpl socketChannel, AccessControlContext acc, PendingFuture<AsynchronousSocketChannel, Object> future) {
            this.socketChannel = socketChannel;
            this.acc = acc;
            this.future = future;
        }
        
        /**
         * Initiates the accept operation.
         */
        @Override
        public void run() {
            long overlapped = 0L;
            
            try {
                /* begin usage of listener socket */
                // 添加一个读锁
                begin();
                
                try {
                    /*
                     * begin usage of child socket
                     * (as it is registered with completion port and so may be closed in the event that the group is forcefully closed).
                     */
                    // 添加一个读锁
                    socketChannel.begin();
                    
                    synchronized(future) {
                        /*
                         * 将future与overlapped建立关联
                         *
                         * 向重叠IO结构缓存池ioCache中存储一个键值对<overlapped, future>，
                         * 即将一个OVERLAPPED结构(如果不存在则新建)与future进行绑定，并返回OVERLAPPED结构的本地引用。
                         */
                        overlapped = ioCache.add(future);
                        
                        // [服务端Socket(监听)]等待客户端的连接请求
                        int n = accept0(handle, socketChannel.handle(), overlapped, dataBuffer);
                        
                        /*
                         * 情形1.1：当前IO操作有成效
                         *
                         * 如果本地反馈IOStatus.UNAVAILABLE消息，
                         * 说明别的IO操作正在进行，当前IO操作已进入队列排队。
                         *
                         * 此种情形下需要挂起future以待后续处理。
                         */
                        if(n == IOStatus.UNAVAILABLE) {
                            return;
                        }
                        
                        /*
                         * 情形1.2：当前IO操作有成效：服务端成功等到了客户端的连接。
                         *
                         * 此种情形下需要进行一些关于accept的收尾处理。
                         */
                        
                        /* connection accepted immediately */
                        // 服务端与客户端accept成功后需要执行的一些收尾操作
                        finishAccept();
                        
                        /* allow another accept before the result is set */
                        // 标记通道"已结束accept操作"
                        enableAccept();
                        
                        // 设置执行结果为[服务端Socket(通信)]通道
                        future.setResult(socketChannel);
                    }
                } finally {
                    /* end usage on child socket */
                    // 移除一个读锁
                    socketChannel.end();
                }
            } catch(Throwable e) {
                /*
                 * 情形2：当前IO操作没成效，而且抛出了异常
                 *
                 * 此种情形下不需要挂起future，可以立即填充执行结果。
                 */
                
                /* failed to initiate accept so release resources */
                // 如果出状况之前设置了重叠IO结构
                if(overlapped != 0L) {
                    // 从任务结果映射集移除一条记录；会尝试缓存重叠IO结构，缓存池已满时则直接释放重叠IO结构的本地内存
                    ioCache.remove(overlapped);
                }
                
                // 关闭[服务端Socket(通信)]通道
                closeChildChannel();
                
                if(e instanceof ClosedChannelException) {
                    e = new AsynchronousCloseException();
                }
                
                if(!(e instanceof IOException) && !(e instanceof SecurityException)) {
                    e = new IOException(e);
                }
                
                // 标记通道"已结束accept操作"
                enableAccept();
                
                // 如果accept过程中出现了异常，则设置任务执行结果为异常
                future.setFailure(e);
                
            } finally {
                /* end of usage of listener socket */
                // 移除一个读锁
                end();
            }
            
            /*
             * accept completed immediately but may not have executed on initiating thread
             * in which case the operation may have been cancelled.
             */
            // 如果当前任务已经中止，则关闭[服务端Socket(通信)]通道
            if(future.isCancelled()) {
                closeChildChannel();
            }
            
            /*
             * 间接处理future中记录的回调句柄。
             * 间接的含义不在当前线程中处理回调句柄，
             * 而是将给定的回调句柄交给异步IO线程池去处理。
             */
            Invoker.invokeIndirectly(future);
        }
        
        /**
         * Executed when the I/O has completed
         */
        // 当IO线程执行完"accept"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行的结果
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            try {
                /* connection accept after group has shutdown */
                // 如果异步IO通道组已经准备关闭，则抛异常
                if(iocp.isShutdown()) {
                    throw new IOException(new ShutdownChannelGroupException());
                }
    
                /* finish the accept */
                try {
                    // 添加一个读锁
                    begin();
                    try {
                        // 添加一个读锁
                        socketChannel.begin();
            
                        // 服务端与客户端accept成功后需要执行的一些收尾操作
                        finishAccept();
                    } finally {
                        // 移除一个读锁
                        socketChannel.end();
                    }
                } finally {
                    // 移除一个读锁
                    end();
                }
    
                /* allow another accept before the result is set */
                // 标记通道"已结束accept操作"
                enableAccept();
    
                // 设置执行结果为[服务端Socket(通信)]通道
                future.setResult(socketChannel);
            } catch(Throwable e) {
                // 标记通道"已结束accept操作"
                enableAccept();
    
                // 关闭[服务端Socket(通信)]通道
                closeChildChannel();
    
                if(e instanceof ClosedChannelException) {
                    e = new AsynchronousCloseException();
                }
    
                if(!(e instanceof IOException) && !(e instanceof SecurityException)) {
                    e = new IOException(e);
                }
    
                // 如果出现了异常，则设置任务执行结果为相应的异常
                future.setFailure(e);
            }
    
            /* if an async cancel has already cancelled the operation then close the new channel so as to free resources */
            // 如果当前任务已经中止，则关闭[服务端Socket(通信)]通道
            if(future.isCancelled()) {
                closeChildChannel();
            }
    
            /*
             * 间接处理future中记录的回调句柄。
             * 间接的含义不在当前线程中处理回调句柄，
             * 而是将给定的回调句柄交给异步IO线程池去处理。
             */
            Invoker.invokeIndirectly(future);
        }
        
        // 当IO线程执行完"accept"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行中的异常信息
        @Override
        public void failed(int error, IOException e) {
            // 标记通道"已结束accept操作"
            enableAccept();
            
            // 关闭[服务端Socket(通信)]通道
            closeChildChannel();
            
            /* release waiters */
            if(isOpen()) {
                // 如果通道未关闭，则设置给定的异常信息
                future.setFailure(e);
            } else {
                // 如果通道已关闭，则设置一个通道已关闭的异常
                future.setFailure(new AsynchronousCloseException());
            }
            
            /*
             * 间接处理future中记录的回调句柄。
             * 间接的含义不在当前线程中处理回调句柄，
             * 而是将给定的回调句柄交给异步IO线程池去处理。
             */
            Invoker.invokeIndirectly(future);
        }
        
        // 标记通道"已结束accept操作"
        void enableAccept() {
            accepting.set(false);
        }
        
        // 关闭[服务端Socket(通信)]通道
        void closeChildChannel() {
            try {
                socketChannel.close();
            } catch(IOException ignore) {
            }
        }
        
        /** caller must have acquired read lock for the listener and child channel. */
        // 服务端与客户端accept成功后需要执行的一些收尾操作
        void finishAccept() throws IOException {
            
            /*
             * Set local/remote addresses. This is currently very inefficient
             * in that it requires 2 calls to getsockname and 2 calls to getpeername.
             * (should change this to use GetAcceptExSockaddrs)
             */
            /*
             * 更新[服务端Socket(通信)]的一些连接信息
             * 参见：https://stackoverflow.com/questions/9169086/tcp-shutdown-with-sockets-connected-through-acceptex
             */
            updateAcceptContext(handle, socketChannel.handle());
            
            // 从[服务端Socket(通信)]的文件描述符中解析出本地Socket地址，即服务端地址
            InetSocketAddress local = Net.localAddress(socketChannel.fd);
            
            // 从[服务端Socket(通信)]的文件描述符中解析出远程Socket地址，即客户端地址
            final InetSocketAddress remote = Net.remoteAddress(socketChannel.fd);
            
            // 设置[服务端Socket(通信)]的本地地址与远程地址
            socketChannel.setConnected(local, remote);
            
            // permission check (in context of initiating thread)
            if(acc != null) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        SecurityManager sm = System.getSecurityManager();
                        sm.checkAccept(remote.getAddress().getHostAddress(), remote.getPort());
                        return null;
                    }
                }, acc);
            }
        }
    }
    
}
