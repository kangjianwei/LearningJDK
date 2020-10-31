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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.channels.ShutdownChannelGroupException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jdk.internal.misc.Unsafe;
import sun.net.util.SocketExceptions;

/**
 * Windows implementation of AsynchronousSocketChannel using overlapped I/O.
 */
// 异步Socket通道的本地实现
class WindowsAsynchronousSocketChannelImpl extends AsynchronousSocketChannelImpl implements Iocp.OverlappedChannel {
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    /**
     * typedef struct _WSABUF {
     * u_long      len;
     * char FAR *  buf;
     * } WSABUF;
     */
    private static final int SIZEOF_WSABUF = dependsArch(8, 16); // 目标/源头缓冲区基础信息，用在读/写操作中
    private static final int OFFSETOF_LEN = 0;
    private static final int OFFSETOF_BUF = dependsArch(4, 8);
    
    
    /** maximum vector size for scatter/gather I/O */
    private static final int MAX_WSABUF = 16;
    
    private static final int SIZEOF_WSABUFARRAY = MAX_WSABUF * SIZEOF_WSABUF;
    
    /** socket handle. Use begin()/end() around each usage of this handle. */
    final long handle;  // [客户端Socket]/[服务端Socket(通信)]在本地(native层)的文件描述符
    
    /** completion key to identify channel when I/O completes */
    private final int completionKey;    // 完成键，用来关联通道
    /** I/O completion port that the socket is associated with */
    private final Iocp iocp;            // 通道组
    
    /**
     * Pending I/O operations are tied to an OVERLAPPED structure that can only
     * be released when the I/O completion event is posted to the completion port.
     * Where I/O operations complete immediately then it is possible
     * there may be more than two OVERLAPPED structures in use.
     */
    private final PendingIoCache ioCache; // 重叠IO结构的缓存池
    
    /** per-channel arrays of WSABUF structures */
    private final long readBufferArray;  // 目标缓冲区基础信息
    private final long writeBufferArray; // 源头缓冲区基础信息
    
    // 本地指针长度，通常为4字节或8字节
    private static int addressSize = unsafe.addressSize();
    
    
    static {
        IOUtil.load();
        initIDs();
    }
    
    
    WindowsAsynchronousSocketChannelImpl(Iocp iocp, boolean failIfGroupShutdown) throws IOException {
        super(iocp);
        
        /*
         * 获取Java层的文件描述符fd在本地(native层)的引用值。
         * fd是[客户端Socket]/[服务端Socket(通信)]在Java层的文件描述符。
         */
        long handle = IOUtil.fdVal(fd);
        
        int key = 0;
        try {
            /*
             * 将指定Socket的引用handle关联到"完成端口"上，并在keyToChannel中记录handle所在通道(支持重叠IO结构)的引用。
             * 返回值为与通道channel建立关联的完成键。
             */
            key = iocp.associate(this, handle);
        } catch(ShutdownChannelGroupException x) {
            if(failIfGroupShutdown) {
                closesocket0(handle);
                throw x;
            }
        } catch(IOException x) {
            closesocket0(handle);
            throw x;
        }
        
        this.handle = handle;
        this.iocp = iocp;
        this.completionKey = key;
        this.ioCache = new PendingIoCache();
        
        // allocate WSABUF arrays
        this.readBufferArray = unsafe.allocateMemory(SIZEOF_WSABUFARRAY);
        this.writeBufferArray = unsafe.allocateMemory(SIZEOF_WSABUFARRAY);
    }
    
    WindowsAsynchronousSocketChannelImpl(Iocp iocp) throws IOException {
        this(iocp, true);
    }
    
    
    // 存在安全管理器的情形下，需要使用此方法执行【bind】操作
    private void doPrivilegedBind(final SocketAddress sa) throws IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    bind(sa);
                    return null;
                }
            });
        } catch(PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }
    
    // 实现异步IO中的"connect"操作
    @Override
    <A> Future<Void> implConnect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        
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
        
        // 检查指定的Socket地址是否合规
        InetSocketAddress isa = Net.checkAddress(remote);
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());
        }
        
        /*
         * check and update state.
         * ConnectEx requires the socket to be bound to a local address.
         */
        IOException bindException = null;
        
        // 如果还未绑定，这里需要进行绑定操作
        synchronized(stateLock) {
            // 如果客户端与服务端已建立连接，则抛异常
            if(state == ST_CONNECTED) {
                throw new AlreadyConnectedException();
            }
            
            // 如果客户端已经准备与服务端建立连接，则抛异常
            if(state == ST_PENDING) {
                throw new ConnectionPendingException();
            }
            
            if(localAddress == null) {
                try {
                    // 使用通配IP和随机端口号初始化Socket地址
                    SocketAddress any = new InetSocketAddress(0);
                    
                    // 如果不存在安全管理器，则直接进行绑定
                    if(sm == null) {
                        bind(any);
                        // 如果存在安全管理器，则需要经过权限校验
                    } else {
                        doPrivilegedBind(any);
                    }
                } catch(IOException x) {
                    bindException = x;
                }
            }
            
            // 如果绑定成功，则进入下一步：指示客户端准备与服务端建立连接
            if(bindException == null) {
                state = ST_PENDING;
            }
        }
        
        // 如果这里出现异常，则需要反馈异常信息，并退出connect操作
        if(bindException != null) {
            try {
                // 由于绑定失败，所以关闭通道
                close();
            } catch(IOException ignore) {
            }
            
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withFailure(bindException);
            }
            
            // 处理回调句柄，会视情形进行直接处理或间接处理
            Invoker.invoke(this, handler, attachment, null, bindException);
            
            return null;
        }
        
        // 创建在Java层挂起任务，等待底层执行完之后通知Java层
        PendingFuture<Void, A> future = new PendingFuture<>(this, handler, attachment);
        
        // 构造一个"connect"任务
        ConnectTask<A> task = new ConnectTask<A>(isa, future);
        
        // 为future设置上下文，即设置实际需要执行的操作
        future.setContext(task);
        
        // 执行"connect"操作，connect结束后，会通知阻塞的工作线程
        task.run();
        
        return future;
    }
    
    // 实现异步IO中的读取操作
    @Override
    <V extends Number, A> Future<V> implRead(boolean isScatteringRead, ByteBuffer dst, ByteBuffer[] dsts, long timeout, TimeUnit unit, A attachment, CompletionHandler<V, ? super A> handler) {
        
        // 目标缓冲区组：存储从当前通道读取到的数据
        ByteBuffer[] bufs;
        
        // 如果需要散射读(即将单个通道中的内容读取到多个目标缓冲区)
        if(isScatteringRead) {
            bufs = dsts;
        } else {
            bufs = new ByteBuffer[1];
            bufs[0] = dst;
        }
        
        // 创建在Java层挂起任务，等待底层执行完之后通知Java层
        PendingFuture<V, A> future = new PendingFuture<>(this, handler, attachment);
        
        // 构造一个"读取"任务
        final ReadTask<V, A> readTask = new ReadTask<>(bufs, isScatteringRead, future);
        
        // 为future设置上下文，即设置实际需要执行的操作
        future.setContext(readTask);
        
        /*
         * 如果设置了超时时间，则需要安排一个定时任务做楔子；
         * 等到了截止时间时，会通过该楔子判断当前异步任务是否完成。
         * 如果超时了还没完成，那就得抛出超时异常了。
         */
        if(timeout>0L) {
            // 待执行的一次性定时任务
            Runnable task = new Runnable() {
                public void run() {
                    readTask.timeout();
                }
            };
            
            // 执行一次性的定时任务task，并返回任务本身：在任务启动后的timeout时长后开始执行
            Future<?> timeoutTask = iocp.schedule(task, timeout, unit);
            
            // 设置timeoutTask作为楔子，在当前异步IO操作超时的时候给出提醒
            future.setTimeoutTask(timeoutTask);
        }
        
        // 执行"读取"操作，读取结束后，会通知阻塞的工作线程
        readTask.run();
        
        return future;
    }
    
    // 实现异步IO中的写入操作
    @Override
    <V extends Number, A> Future<V> implWrite(boolean isGatheringWrite, ByteBuffer src, ByteBuffer[] srcs, long timeout, TimeUnit unit, A attachment, CompletionHandler<V, ? super A> handler) {
        
        // 源头缓冲区组：存储向当前通道写入的数据
        ByteBuffer[] bufs;
        
        // 如果需要聚集写(即将多个源头缓存区的内容发送到单个通道)
        if(isGatheringWrite) {
            bufs = srcs;
        } else {
            bufs = new ByteBuffer[1];
            bufs[0] = src;
        }
        
        // 在Java层挂起任务，等待底层执行完之后通知Java层
        PendingFuture<V, A> future = new PendingFuture<>(this, handler, attachment);
        
        // 构造"写入"操作
        final WriteTask<V, A> writeTask = new WriteTask<>(bufs, isGatheringWrite, future);
        
        // 为future设置上下文，即设置实际需要执行的操作
        future.setContext(writeTask);
        
        /*
         * 如果设置了超时时间，则需要安排一个定时任务做楔子；
         * 等到了截止时间时，会通过该楔子判断当前异步任务是否完成。
         * 如果超时了还没完成，那就得抛出超时异常了。
         */
        if(timeout>0L) {
            // 待执行的一次性定时任务
            Runnable task = new Runnable() {
                public void run() {
                    writeTask.timeout();
                }
            };
            
            // 执行一次性的定时任务task，并返回任务本身：在任务启动后的timeout时长后开始执行
            Future<?> timeoutTask = iocp.schedule(task, timeout, unit);
            
            // 设置timeoutTask作为楔子，在当前异步IO操作超时的时候给出提醒
            future.setTimeoutTask(timeoutTask);
        }
        
        // 执行"写入"操作，写入结束后，会通知阻塞的工作线程
        writeTask.run();
        
        return future;
    }
    
    // 实现对异步IO通道的关闭操作
    @Override
    void implClose() throws IOException {
        /* close socket (may cause outstanding async I/O operations to fail). */
        // 关闭[客户端Socket]
        closesocket0(handle);
        
        /* waits until all I/O operations have completed */
        // 关闭重叠IO结构的缓存池
        ioCache.close();
        
        /* release arrays of WSABUF structures */
        // 释放一些占用的本地内存
        unsafe.freeMemory(readBufferArray);
        unsafe.freeMemory(writeBufferArray);
        
        /*
         * finally disassociate from the completion port
         * (key can be 0 if channel created when group is shutdown)
         */
        if(completionKey != 0) {
            // 解除当前通道与完成键的关联
            iocp.disassociate(completionKey);
        }
    }
    
    // 取消异步IO操作时的回调
    @Override
    public void onCancel(PendingFuture<?, ?> task) {
        if(task.getContext() instanceof ConnectTask) {
            // 中止读取与写入
            killConnect();
        }
        
        if(task.getContext() instanceof ReadTask) {
            // 中止读取
            killReading();
        }
        
        if(task.getContext() instanceof WriteTask) {
            // 中止写入
            killWriting();
        }
    }
    
    
    /**
     * Invoked by Iocp when an I/O operation competes.
     */
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
    
    /** invoked by WindowsAsynchronousServerSocketChannelImpl */
    // 返回当前通道的句柄
    long handle() {
        return handle;
    }
    
    /** invoked by WindowsAsynchronousServerSocketChannelImpl when new connection accept */
    // 设置[服务端Socket(通信)]的本地地址与远程地址
    void setConnected(InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
        synchronized(stateLock) {
            state = ST_CONNECTED;
            this.localAddress = localAddress;
            this.remoteAddress = remoteAddress;
        }
    }
    
    private static int dependsArch(int value32, int value64) {
        return (addressSize == 4) ? value32 : value64;
    }
    
    
    private static native void initIDs();
    
    private static native int connect0(long socket, boolean preferIPv6, InetAddress remote, int remotePort, long overlapped) throws IOException;
    
    private static native void updateConnectContext(long socket) throws IOException;
    
    private static native int read0(long socket, int count, long addres, long overlapped) throws IOException;
    
    private static native int write0(long socket, int count, long address, long overlapped) throws IOException;
    
    private static native void shutdown0(long socket, int how) throws IOException;
    
    private static native void closesocket0(long socket) throws IOException;
    
    
    /**
     * Implements the task to initiate a connection and the handler to
     * consume the result when the connection is established (or fails).
     */
    // 异步IO操作：进行connect操作，即客户端与服务端建立连接
    private class ConnectTask<A> implements Runnable, Iocp.ResultHandler {
        private final InetSocketAddress remote;         // 需要连接到的远端Socket的地址
        private final PendingFuture<Void, A> future;    // 在Java层挂起的任务，等待填充执行结果
        
        ConnectTask(InetSocketAddress remote, PendingFuture<Void, A> future) {
            this.remote = remote;
            this.future = future;
        }
        
        /**
         * Task to initiate a connection.
         */
        @Override
        public void run() {
            long overlapped = 0L;
            Throwable exc = null;
            
            try {
                // 添加一个读锁
                begin();
                
                /*
                 * synchronize on future to allow this thread handle the case
                 * where the connection is established immediately.
                 */
                synchronized(future) {
                    /*
                     * 将future与overlapped建立关联
                     *
                     * 向重叠IO结构缓存池ioCache中存储一个键值对<overlapped, future>，
                     * 即将一个OVERLAPPED结构(如果不存在则新建)与future进行绑定，并返回OVERLAPPED结构的本地引用。
                     */
                    overlapped = ioCache.add(future);
                    
                    /* initiate the connection */
                    // 当前Socket向指定地址处的ServerSocket发起连接
                    int n = connect0(handle, Net.isIPv6Available(), remote.getAddress(), remote.getPort(), overlapped);
                    
                    /*
                     * 情形1.1：当前IO操作有成效
                     *
                     * 如果本地反馈IOStatus.UNAVAILABLE消息，
                     * 说明别的IO操作正在进行，当前IO操作已进入队列排队。
                     *
                     * 此种情形下需要挂起future以待后续处理。
                     */
                    if(n == IOStatus.UNAVAILABLE) {
                        // connection is pending
                        return;
                    }
                    
                    /*
                     * 情形1.2：当前IO操作有成效：客户端成功与服务端建立了连接。
                     *
                     * 此种情形下需要进行一些关于connect的收尾处理。
                     */
                    
                    /* connection established immediately */
                    // 客户端与服务端connect成功后需要执行的一些收尾操作
                    afterConnect();
                    
                    // 设置执行结果为null，因为connect无返回值
                    future.setResult(null);
                }
            } catch(Throwable e) {
                /*
                 * 情形2：当前IO操作没成效，而且抛出了异常
                 *
                 * 此种情形下不需要挂起future，可以立即填充执行结果。
                 */
                
                // 如果出状况之前设置了重叠IO结构
                if(overlapped != 0L) {
                    // 从任务结果映射集移除一条记录；会尝试缓存重叠IO结构，缓存池已满时则直接释放重叠IO结构的本地内存
                    ioCache.remove(overlapped);
                }
                
                exc = e;
            } finally {
                // 移除一个读锁
                end();
            }
            
            // connect过程中出现了异常
            if(exc != null) {
                // 关闭异步通道
                closeChannel();
                
                // 构造一条异常信息
                exc = SocketExceptions.of(toIOException(exc), remote);
                
                // 如果connect过程中出现了异常，则设置任务执行结果为异常
                future.setFailure(exc);
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
        
        /**
         * Invoked by handler thread when connection established.
         */
        // 当IO线程执行完"connect"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行的结果
        @Override
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            Throwable exc = null;
            try {
                // 添加一个读锁
                begin();
    
                // 客户端与服务端connect成功后需要执行的一些收尾操作
                afterConnect();
    
                // 设置任务执行的结果
                future.setResult(null);
            } catch(Throwable e) {
                /* channel is closed or unable to finish connect */
                exc = e;
            } finally {
                // 移除一个读锁
                end();
            }
            
            /* can't close channel while in begin/end block */
            if(exc != null) {
                // 关闭异步通道
                closeChannel();
                
                // 生成一条异常信息
                IOException e = SocketExceptions.of(toIOException(exc), remote);
                
                // 设置异常信息
                future.setFailure(e);
            }
            
            if(canInvokeDirect) {
                // 直接处理future中记录的回调句柄，不会改变当前线程的递归调用深度
                Invoker.invokeUnchecked(future);
            } else {
                // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
                Invoker.invoke(future);
            }
        }
        
        /**
         * Invoked by handler thread when failed to establish connection.
         */
        // 当IO线程执行完"connect"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行中的异常信息
        @Override
        public void failed(int error, IOException e) {
            e = SocketExceptions.of(e, remote);
            
            if(isOpen()) {
                // 关闭异步通道
                closeChannel();
                
                // 如果通道未关闭，则设置给定的异常信息
                future.setFailure(e);
            } else {
                // 生成一条异常信息
                e = SocketExceptions.of(new AsynchronousCloseException(), remote);
                
                // 如果通道已关闭，则设置一个通道已关闭的异常
                future.setFailure(e);
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
        
        /**
         * Invoke after a connection is successfully established.
         */
        // 客户端与服务端connect成功后需要执行的一些收尾操作
        private void afterConnect() throws IOException {
            /*
             * 更新[客户Socket]的一些连接信息
             * 参见：https://stackoverflow.com/questions/9169086/tcp-shutdown-with-sockets-connected-through-acceptex
             */
            updateConnectContext(handle);
            
            synchronized(stateLock) {
                // 指示客户端与服务端已建立连接
                state = ST_CONNECTED;
                // 记录已连接的远端Socket地址
                remoteAddress = remote;
            }
        }
        
        // 关闭异步通道
        private void closeChannel() {
            try {
                close();
            } catch(IOException ignore) {
            }
        }
        
        // 异常类型转换
        private IOException toIOException(Throwable x) {
            if(x instanceof IOException) {
                if(x instanceof ClosedChannelException) {
                    x = new AsynchronousCloseException();
                }
                return (IOException) x;
            }
            
            return new IOException(x);
        }
    }
    
    /**
     * Implements the task to initiate a read and the handler to consume the
     * result when the read completes.
     */
    // 异步IO操作：进行读取操作，读取当前通道中的数据，并将其存储到指定的缓冲区中
    private class ReadTask<V, A> implements Runnable, Iocp.ResultHandler {
        private final boolean scatteringRead; // 是否为散射读(即将单个通道中的内容读取到多个目标缓冲区)
        
        private final ByteBuffer[] bufs; // 目标缓冲区组：存储从当前通道读取到的数据
        private final int numBufs;       // 目标缓冲区的数量
        
        private ByteBuffer[] shadow;  // 影子缓冲区组，暂时存储将要读到的数据
        
        private final PendingFuture<V, A> future;   // 在Java层挂起的任务，等待填充执行结果
        
        ReadTask(ByteBuffer[] bufs, boolean scatteringRead, PendingFuture<V, A> future) {
            this.bufs = bufs;
            this.numBufs = Math.min(bufs.length, MAX_WSABUF);
            this.scatteringRead = scatteringRead;
            this.future = future;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            long overlapped = 0L;      // OVERLAPPED结构的本地引用
            boolean prepared = false;  // 指示目标缓冲区是否已经准备好
            boolean pending = false;   // 指示读取操作是否已经挂起
            
            try {
                // 添加一个读锁
                begin();
                
                /* substitute non-direct buffers */
                // 准备目标缓冲区，以接收即将读取到的数据
                prepareBuffers();
                
                // 已准备好目标缓冲区
                prepared = true;
                
                /* get an OVERLAPPED structure (from the cache or allocate) */
                /*
                 * 将future与overlapped建立关联
                 *
                 * 向重叠IO结构缓存池ioCache中存储一个键值对<overlapped, future>，
                 * 即将一个OVERLAPPED结构(如果不存在则新建)与future进行绑定，并返回OVERLAPPED结构的本地引用。
                 */
                overlapped = ioCache.add(future);
                
                // 从handle处的通道中读取数据，读取到的数据将暂时存储到shadow中
                int n = read0(handle, numBufs, readBufferArray, overlapped);
                
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
                 * 情形2.1：当前IO操作没成效，原因是Socket通道已关闭
                 *
                 * 此时无法读取内容，则将执行结果设置为-1。
                 */
                if(n == IOStatus.EOF) {
                    // 结束读取，并非因为超时
                    enableReading();
                    
                    // 直接设置执行结果为EOF
                    if(scatteringRead) {
                        future.setResult((V) Long.valueOf(-1L));
                    } else {
                        future.setResult((V) Integer.valueOf(-1));
                    }
                    
                    /*
                     * 情形2.2：当前IO操作没成效，而且抛出了异常
                     *
                     * 本地产生了其他错误消息的话，直接抛异常。
                     *
                     * 此种情形下不需要挂起future，可以立即填充执行结果。
                     */
                } else {
                    throw new InternalError("Read completed immediately");
                }
            } catch(Throwable e) {
                /*
                 * failed to initiate read.
                 * reset read flag before releasing waiters.
                 */
                // 结束读取，并非因为超时
                enableReading();
                
                if(e instanceof ClosedChannelException) {
                    e = new AsynchronousCloseException();
                }
                
                if(!(e instanceof IOException)) {
                    e = new IOException(e);
                }
                
                // 如果读取过程中出现了异常，则设置任务执行结果为异常
                future.setFailure(e);
            } finally {
                /* release resources if I/O not pending */
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
                    
                    // 如果目标缓冲区已经准备好，此处需要释放其空间
                    if(prepared) {
                        // 如果目标缓冲区是直接缓冲区，则释放其本地内存
                        releaseBuffers();
                    }
                }
                
                // 移除一个读锁
                end();
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
        
        /**
         * Executed when the I/O has completed
         */
        // 当IO线程执行完"读取"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行的结果
        @Override
        @SuppressWarnings("unchecked")
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            // 如果没读到有效字节，说明遇到了EOF
            if(bytesTransferred == 0) {
                bytesTransferred = -1;  // EOF
            } else {
                // 将读到的字节存入目标缓冲区
                updateBuffers(bytesTransferred);
            }
            
            // 如果目标缓冲区是直接缓冲区，则释放其本地内存
            releaseBuffers();
            
            // release waiters if not already released by timeout
            synchronized(future) {
                // 如果任务已经执行完成(之前设置过了执行结果)，则直接返回
                if(future.isDone()) {
                    return;
                }
                
                // 结束读取，并非因为超时
                enableReading();
                
                // 设置执行结果为读到的字节数
                if(scatteringRead) {
                    future.setResult((V) Long.valueOf(bytesTransferred));
                } else {
                    future.setResult((V) Integer.valueOf(bytesTransferred));
                }
            }
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
            // 如果目标缓冲区是直接缓冲区，则释放其本地内存
            releaseBuffers();
            
            /* release waiters if not already released by timeout */
            // 如果通道已经关闭，则需要更新异常为"关闭异常"
            if(!isOpen()) {
                e = new AsynchronousCloseException();
            }
            
            synchronized(future) {
                // 如果任务已经执行完成(之前设置过了执行结果)，则直接返回
                if(future.isDone()) {
                    return;
                }
                
                // 结束读取，并非因为超时
                enableReading();
                
                // 如果出现了异常，则设置任务执行结果为相应的异常
                future.setFailure(e);
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
        
        /**
         * Invoked prior to read to prepare the WSABUF array. Where necessary,
         * it substitutes non-direct buffers with direct buffers.
         */
        // 准备目标缓冲区，以接收即将读取到的数据
        void prepareBuffers() {
            
            // 影子缓冲区组，暂时存储将要读取到的数据
            shadow = new ByteBuffer[numBufs];
            
            // 目标缓冲区基础信息，包含目标缓冲区地址与目标缓冲区的剩余空间信息
            long address = readBufferArray;
            
            // 遍历每个目标缓冲区
            for(int i = 0; i<numBufs; i++) {
                ByteBuffer dst = bufs[i];
                
                int pos = dst.position();
                int lim = dst.limit();
                assert (pos<=lim);
                int rem = (pos<=lim ? lim - pos : 0);
                
                long addr;
                
                // 如果目标缓冲区不是直接缓冲区，则需要创建一个影子缓冲区暂时存储将要读到的数据
                if(!(dst instanceof DirectBuffer)) {
                    // 获取一块容量至少为rem个字节的直接缓冲区
                    ByteBuffer bb = Util.getTemporaryDirectBuffer(rem);
                    // 记下目标缓冲区
                    shadow[i] = bb;
                    // 目标缓冲区地址
                    addr = ((DirectBuffer) bb).address();
                    
                    // 如果目标缓冲区已经是直接缓冲区，则直接使用当前目标缓冲区
                } else {
                    // 记下目标缓冲区
                    shadow[i] = dst;
                    // 目标缓冲区地址
                    addr = ((DirectBuffer) dst).address() + pos;
                }
                
                unsafe.putAddress(address + OFFSETOF_BUF, addr); // 记录目标缓冲区地址
                unsafe.putInt(address + OFFSETOF_LEN, rem);      // 记录目标缓冲区的剩余空间
                
                address += SIZEOF_WSABUF;
            }
        }
        
        /**
         * Invoked after a read has completed to update the buffer positions
         * and release any substituted buffers.
         */
        // 将读到的字节存入目标缓冲区
        void updateBuffers(int bytesRead) {
            
            // 遍历目标缓冲区，设置好游标
            for(int i = 0; i<numBufs; i++) {
                ByteBuffer nextBuffer = shadow[i];
                
                int pos = nextBuffer.position();
                int len = nextBuffer.remaining();
                
                // 如果剩余的字节数大于目标缓冲区剩余的空间，则可以直接填满当前目标缓冲区
                if(bytesRead >= len) {
                    bytesRead -= len;
                    
                    int newPosition = pos + len;
                    try {
                        nextBuffer.position(newPosition);
                    } catch(IllegalArgumentException x) {
                        // position changed by another
                    }
                    
                    // 剩余字节数已经小于当前目标缓冲区的剩余空间，则把剩余的字节填充到目标缓冲区
                } else {
                    // Buffers not completely filled
                    if(bytesRead>0) {
                        assert (pos + bytesRead<(long) Integer.MAX_VALUE);
                        
                        int newPosition = pos + bytesRead;
                        try {
                            nextBuffer.position(newPosition);
                        } catch(IllegalArgumentException x) {
                            // position changed by another
                        }
                    }
                    break;
                }
            }
            
            // Put results from shadow into the slow buffers
            for(int i = 0; i<numBufs; i++) {
                // 如果原先的目标缓冲区已经是直接缓冲区，则忽略此目标缓冲区
                if(bufs[i] instanceof DirectBuffer) {
                    continue;
                }
                
                // 将影子缓冲区从写模式切换到读模式
                shadow[i].flip();
                
                try {
                    // 将影子缓冲区中的数据转入原先给出的目标缓冲区中
                    bufs[i].put(shadow[i]);
                } catch(BufferOverflowException x) {
                    // position changed by another
                }
            }
        }
        
        // 如果目标缓冲区是直接缓冲区，则释放其本地内存
        void releaseBuffers() {
            for(int i = 0; i<numBufs; i++) {
                if(!(bufs[i] instanceof DirectBuffer)) {
                    // 采用FILO的形式(入栈模式)将shadow[i]放入Buffer缓存池以待复用
                    Util.releaseTemporaryDirectBuffer(shadow[i]);
                }
            }
        }
        
        /**
         * Invoked if timeout expires before it is cancelled
         */
        // 到了截止时间了，需要判断当前异步IO操作是否已经完成了
        void timeout() {
            // synchronize on result as the I/O could complete/fail
            synchronized(future) {
                if(future.isDone()) {
                    return;
                }
                
                // kill further reading before releasing waiters
                enableReading(true);
                
                // 设置超时异常
                future.setFailure(new InterruptedByTimeoutException());
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
    }
    
    /**
     * Implements the task to initiate a write and the handler to consume the
     * result when the write completes.
     */
    // 异步IO操作：进行写入操作，读取指定缓冲区中的数据，并将其写入到当前通道中
    private class WriteTask<V, A> implements Runnable, Iocp.ResultHandler {
        private final boolean gatheringWrite;   // 是否需要聚集写(即将多个源头缓存区的内容发送到单个通道)
        
        private final ByteBuffer[] bufs; // 源头缓冲区组：存储向当前通道写入的数据
        private final int numBufs;       // 源头缓冲区的数量
        
        private ByteBuffer[] shadow;  // 影子缓冲区组，暂时存储即将写入的数据
        
        private final PendingFuture<V, A> future;   // 在Java层挂起的任务，等待填充执行结果
        
        WriteTask(ByteBuffer[] bufs, boolean gatheringWrite, PendingFuture<V, A> future) {
            this.bufs = bufs;
            this.numBufs = Math.min(bufs.length, MAX_WSABUF);
            this.gatheringWrite = gatheringWrite;
            this.future = future;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            long overlapped = 0L;      // OVERLAPPED结构的本地引用
            boolean prepared = false;  // 指示源头缓冲区是否已经准备好
            boolean pending = false;   // 指示写入操作是否已经挂起
            boolean shutdown = false;
            
            try {
                // 添加一个读锁
                begin();
                
                /* substitute non-direct buffers */
                // 准备源头缓冲区，以存储即将写入的数据
                prepareBuffers();
                
                // 已准备好源头缓冲区
                prepared = true;
                
                /* get an OVERLAPPED structure (from the cache or allocate) */
                /*
                 * 将future与overlapped建立关联
                 *
                 * 向重叠IO结构缓存池ioCache中存储一个键值对<overlapped, future>，
                 * 即将一个OVERLAPPED结构(如果不存在则新建)与future进行绑定，并返回OVERLAPPED结构的本地引用。
                 */
                overlapped = ioCache.add(future);
                
                // 向handle处的通道中写入数据，shadow暂时存储了待写入的数据
                int n = write0(handle, numBufs, writeBufferArray, overlapped);
                
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
                    // 标记future任务已挂起，需要后续填充IO操作的结果
                    pending = true;
                    return;
                }
                
                /*
                 * 情形2.1：当前IO操作没成效，原因是Socket通道已关闭
                 *
                 * 此时无法写入内容，则将执行结果设置为-1，并抛出异常。
                 */
                if(n == IOStatus.EOF) {
                    /* special case for shutdown output */
                    shutdown = true;
                    throw new ClosedChannelException();
                }
                
                /*
                 * 情形2.2：当前IO操作没成效，而且抛出了异常
                 *
                 * 本地产生了其他错误消息的话，直接抛异常。
                 *
                 * 此种情形下不需要挂起future，可以立即填充执行结果。
                 */
                throw new InternalError("Write completed immediately");
            } catch(Throwable e) {
                /* write failed. Enable writing before releasing waiters. */
                // 结束写入，并非因为超时
                enableWriting();
                
                if(!shutdown && (e instanceof ClosedChannelException)) {
                    e = new AsynchronousCloseException();
                }
                
                if(!(e instanceof IOException)) {
                    e = new IOException(e);
                }
                
                // 如果读取过程中出现了异常，则设置任务执行结果为异常
                future.setFailure(e);
            } finally {
                /* release resources if I/O not pending */
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
                    
                    // 如果源头缓冲区已经准备好，此处需要释放其空间
                    if(prepared) {
                        // 如果源头缓冲区是直接缓冲区，则释放其本地内存
                        releaseBuffers();
                    }
                }
                
                // 移除一个读锁
                end();
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
        
        /**
         * Executed when the I/O has completed
         */
        // 当IO线程执行完"写入"操作后，唤醒工作线程，在工作线程中调用此方法设置任务执行的结果
        @Override
        @SuppressWarnings("unchecked")
        public void completed(int bytesTransferred, boolean canInvokeDirect) {
            // 更新源头缓冲区的游标
            updateBuffers(bytesTransferred);
            
            /* return direct buffer to cache if substituted */
            // 如果源头缓冲区是直接缓冲区，则释放其本地内存
            releaseBuffers();
            
            /* release waiters if not already released by timeout */
            synchronized(future) {
                // 如果任务已经执行完成(之前设置过了执行结果)，则直接返回
                if(future.isDone()) {
                    return;
                }
                
                // 结束写入，并非因为超时
                enableWriting();
                
                // 设置执行结果为写入的字节数
                if(gatheringWrite) {
                    future.setResult((V) Long.valueOf(bytesTransferred));
                } else {
                    future.setResult((V) Integer.valueOf(bytesTransferred));
                }
            }
            
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
            // 如果源头缓冲区是直接缓冲区，则释放其本地内存
            releaseBuffers();
            
            /* release waiters if not already released by timeout */
            // 如果通道已经关闭，则需要更新异常为"关闭异常"
            if(!isOpen()) {
                e = new AsynchronousCloseException();
            }
            
            synchronized(future) {
                // 如果任务已经执行完成(之前设置过了执行结果)，则直接返回
                if(future.isDone()) {
                    return;
                }
                
                // 结束写入，并非因为超时
                enableWriting();
                
                // 如果出现了异常，则设置任务执行结果为相应的异常
                future.setFailure(e);
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
        
        /**
         * Invoked prior to write to prepare the WSABUF array. Where necessary,
         * it substitutes non-direct buffers with direct buffers.
         */
        // 准备源头缓冲区，以存储即将写入的数据
        void prepareBuffers() {
            
            // 影子缓冲区组，暂时存储即将写入的数据
            shadow = new ByteBuffer[numBufs];
            
            // 源头缓冲区基础信息，包含源头缓冲区地址与源头缓冲区的剩余数据信息
            long address = writeBufferArray;
            
            // 遍历每个源头缓冲区
            for(int i = 0; i<numBufs; i++) {
                ByteBuffer src = bufs[i];
                
                int pos = src.position();
                int lim = src.limit();
                assert (pos<=lim);
                int rem = (pos<=lim ? lim - pos : 0);
                
                long addr;
                
                // 如果源头缓冲区不是直接缓冲区，则需要创建一个影子缓冲区暂时存储即将写入的数据
                if(!(src instanceof DirectBuffer)) {
                    // 获取一块容量至少为rem个字节的直接缓冲区
                    ByteBuffer bb = Util.getTemporaryDirectBuffer(rem);
                    // 向bb中存入源头缓冲区的数据
                    bb.put(src);
                    // 将bb从写模式切换到读模式
                    bb.flip();
                    
                    // 重新将src的游标设置到pos处
                    src.position(pos);
                    
                    // 记下源头缓冲区
                    shadow[i] = bb;
                    // 源头缓冲区地址
                    addr = ((DirectBuffer) bb).address();
                    
                    // 如果源头缓冲区已经是直接缓冲区，则直接使用当前源头缓冲区
                } else {
                    // 记下源头缓冲区
                    shadow[i] = src;
                    // 源头缓冲区地址
                    addr = ((DirectBuffer) src).address() + pos;
                }
                
                unsafe.putAddress(address + OFFSETOF_BUF, addr); // 记录源头缓冲区地址
                unsafe.putInt(address + OFFSETOF_LEN, rem);      // 记录源头缓冲区的剩余数据量
                
                address += SIZEOF_WSABUF;
            }
        }
        
        /**
         * Invoked after a write has completed to update the buffer positions
         * and release any substituted buffers.
         */
        // 更新源头缓冲区的游标
        void updateBuffers(int bytesWritten) {
            
            // Notify the buffers how many bytes were taken
            for(int i = 0; i<numBufs; i++) {
                ByteBuffer nextBuffer = bufs[i];
    
                int pos = nextBuffer.position();
                int lim = nextBuffer.limit();
                int len = (pos<=lim ? lim - pos : lim);
    
                if(bytesWritten >= len) {
                    bytesWritten -= len;
                    int newPosition = pos + len;
                    try {
                        nextBuffer.position(newPosition);
                    } catch(IllegalArgumentException x) {
                        // position changed by someone else
                    }
                } else {
                    // Buffers not completely filled
                    if(bytesWritten>0) {
                        assert (pos + bytesWritten<(long) Integer.MAX_VALUE);
                        int newPosition = pos + bytesWritten;
                        try {
                            nextBuffer.position(newPosition);
                        } catch(IllegalArgumentException x) {
                            // position changed by someone else
                        }
                    }
                    break;
                }
            }
        }
        
        // 如果源头缓冲区是直接缓冲区，则释放其本地内存
        void releaseBuffers() {
            for(int i = 0; i<numBufs; i++) {
                if(!(bufs[i] instanceof DirectBuffer)) {
                    // 采用FILO的形式(入栈模式)将shadow[i]放入Buffer缓存池以待复用
                    Util.releaseTemporaryDirectBuffer(shadow[i]);
                }
            }
        }
        
        /**
         * Invoked if timeout expires before it is cancelled
         */
        // 到了截止时间了，需要判断当前异步IO操作是否已经完成了
        void timeout() {
            // synchronize on result as the I/O could complete/fail
            synchronized(future) {
                if(future.isDone()) {
                    return;
                }
                
                // kill further writing before releasing waiters
                enableWriting(true);
                
                // 设置超时异常
                future.setFailure(new InterruptedByTimeoutException());
            }
            
            // 当异步IO操作已有执行结果时，接下来处理future中记录的回调句柄
            Invoker.invoke(future);
        }
    }
    
}
