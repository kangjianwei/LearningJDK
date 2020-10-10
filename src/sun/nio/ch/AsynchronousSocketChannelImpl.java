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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ReadPendingException;
import java.nio.channels.WritePendingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import sun.net.NetHooks;
import sun.net.ext.ExtendedSocketOptions;

import static sun.net.ext.ExtendedSocketOptions.SOCK_STREAM;

/**
 * Base implementation of AsynchronousSocketChannel
 */
// 异步Socket通道的抽象实现
abstract class AsynchronousSocketChannelImpl extends AsynchronousSocketChannel implements Cancellable, Groupable {
    
    /** State, increases monotonically */
    // Socket状态常量
    static final int ST_UNINITIALIZED = -1;
    static final int ST_UNCONNECTED = 0;
    static final int ST_PENDING = 1; // 客户端准备与服务端建立连接
    static final int ST_CONNECTED = 2; // 客户端与服务端已建立连接
    
    // Socket状态
    protected volatile int state = ST_UNINITIALIZED;
    
    protected final FileDescriptor fd;  // [客户端Socket]/[服务端Socket(通信)]在Java层的文件描述符
    
    /*
     * 本地地址
     *
     * [客户端Socket] 　　 : 客户端地址
     * [服务端Socket(通信)]: 服务端地址
     */
    protected volatile InetSocketAddress localAddress;
    
    /*
     * 远程地址
     *
     * [客户端Socket] 　　 : 服务端的地址
     * [服务端Socket(通信)]: 客户端的地址
     */
    protected volatile InetSocketAddress remoteAddress;
    
    // 指示当前通道是否已关闭
    private volatile boolean closed;
    
    private boolean reading;        // 是否正在读取
    private boolean readShutdown;   // 通道的读取功能是否被关闭
    private boolean readKilled;     // 是否因超时/中止而不能再读取
    
    private boolean writing;        // 是否正在写入
    private boolean writeShutdown;  // 通道的写入功能是否被关闭
    private boolean writeKilled;    // 是否因超时/中止而不能再写入
    
    /** set true when exclusive binding is on and SO_REUSEADDR is emulated */
    // 是否允许立刻重用已关闭的socket端口
    private boolean isReuseAddress;
    
    /** protects state, localAddress, and remoteAddress */
    protected final Object stateLock = new Object();
    
    /** reading state */
    private final Object readLock = new Object();
    
    /** writing state */
    private final Object writeLock = new Object();
    
    /** close support */
    private final ReadWriteLock closeLock = new ReentrantReadWriteLock();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    AsynchronousSocketChannelImpl(AsynchronousChannelGroupImpl group) throws IOException {
        super(group.provider());
        // 创建客户端TCP socket，并返回其文件描述符
        this.fd = Net.socket(true);
        this.state = ST_UNCONNECTED;
    }
    
    /** Constructor for sockets obtained from AsynchronousServerSocketChannelImpl */
    AsynchronousSocketChannelImpl(AsynchronousChannelGroupImpl group, FileDescriptor fd, InetSocketAddress remote) throws IOException {
        super(group.provider());
        this.fd = fd;
        this.state = ST_CONNECTED;
        this.localAddress = Net.localAddress(fd);
        this.remoteAddress = remote;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ socket操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 对[客户端Socket]执行【bind】操作；local为null时，使用通配IP和随机端口
    @Override
    public final AsynchronousSocketChannel bind(SocketAddress local) throws IOException {
        try {
            // 添加一个读锁
            begin();
            
            synchronized(stateLock) {
                if(state == ST_PENDING) {
                    throw new ConnectionPendingException();
                }
                
                // 如果已经绑定则抛异常
                if(localAddress != null) {
                    throw new AlreadyBoundException();
                }
                
                // 确定绑定到哪个地址
                InetSocketAddress isa = (local == null) ? new InetSocketAddress(0) : Net.checkAddress(local);
                
                // 权限校验
                SecurityManager sm = System.getSecurityManager();
                if(sm != null) {
                    sm.checkListen(isa.getPort());
                }
                
                // Socket进行bind操作之前的回调
                NetHooks.beforeTcpBind(fd, isa.getAddress(), isa.getPort());
                
                // 为[客户端Socket]绑定IP地址与端口号
                Net.bind(fd, isa.getAddress(), isa.getPort());
                
                // 记录绑定的本地地址
                localAddress = Net.localAddress(fd);
            }
        } finally {
            // 移除一个读锁
            end();
        }
        
        return this;
    }
    
    /*
     * 对[客户端Socket]执行【connect】操作，以便连接到远端Socket；
     * 返回值是一个包含Void的Future，主线程轮询此Future以判断是否accept完成。
     *
     * 注：这里的返回值包装Void的原因是connect操作本来就没有返回值，Void在这里只是用来占位。
     * 　　又由于需要一个判断异步IO操作是否完成的机制，所以引入了Future。
     */
    @Override
    public final Future<Void> connect(SocketAddress remote) {
        return implConnect(remote, null, null);
    }
    
    /*
     * 对[客户端Socket]执行【connect】操作，以便连接到远端Socket；
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程。
     *
     * 注：这里的回调句柄包装Void的原因是connect操作本来就没有返回值，Void在这里只是用来占位。
     * 　　又由于需要一个回调机制来向主线程反馈任务执行结果，所以引入了CompletionHandler。
     */
    @Override
    public final <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        if(handler == null) {
            throw new NullPointerException("'handler' is null");
        }
        
        implConnect(remote, attachment, handler);
    }
    
    /**
     * Invoked by connect to initiate the connect operation.
     */
    // 实现异步IO中的"connect"操作
    abstract <A> Future<Void> implConnect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler);
    
    /*▲ socket操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取消/关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭从当前通道读取数据的功能
    @Override
    public final AsynchronousSocketChannel shutdownInput() throws IOException {
        try {
            // 添加一个读锁
            begin();
            
            // 如果远程地址为null(还未连接)，则抛出异常
            if(remoteAddress == null) {
                throw new NotYetConnectedException();
            }
            
            synchronized(readLock) {
                // 如果通道的读取功能还未关闭，则在此处关闭通道的"读取"
                if(!readShutdown) {
                    Net.shutdown(fd, Net.SHUT_RD);
                    readShutdown = true;
                }
            }
        } finally {
            // 移除一个读锁
            end();
        }
        
        return this;
    }
    
    // 关闭向当前通道写入数据的功能
    @Override
    public final AsynchronousSocketChannel shutdownOutput() throws IOException {
        try {
            // 添加一个读锁
            begin();
            
            // 如果远程地址为null(还未连接)，则抛出异常
            if(remoteAddress == null) {
                throw new NotYetConnectedException();
            }
            
            synchronized(writeLock) {
                // 如果通道的写入功能还未关闭，则在此处关闭通道的"写入"
                if(!writeShutdown) {
                    Net.shutdown(fd, Net.SHUT_WR);
                    writeShutdown = true;
                }
            }
        } finally {
            // 移除一个读锁
            end();
        }
        
        return this;
    }
    
    // 关闭异步通道
    @Override
    public final void close() throws IOException {
        /* synchronize with any threads initiating asynchronous operations */
        closeLock.writeLock().lock();
        
        try {
            if(closed) {
                return;     // already closed
            }
            closed = true;
        } finally {
            closeLock.writeLock().unlock();
        }
        
        // 实现对异步IO通道的关闭操作
        implClose();
    }
    
    /**
     * Invoked to close socket and release other resources.
     */
    // 实现对异步IO通道的关闭操作
    abstract void implClose() throws IOException;
    
    
    // 结束读取，并非因为超时
    final void enableReading() {
        enableReading(false);
    }
    
    // 指示结束读取；killed指示是否需要因为超时而关闭读取功能
    final void enableReading(boolean killed) {
        synchronized(readLock) {
            // 指示结束读取
            reading = false;
            
            if(killed) {
                // 指示因超时而不能再读取
                readKilled = true;
            }
        }
    }
    
    // 结束写入，并非因为超时
    final void enableWriting() {
        enableWriting(false);
    }
    
    // 指示结束写入；killed指示是否需要因为超时而关闭写入功能
    final void enableWriting(boolean killed) {
        synchronized(writeLock) {
            // 指示结束写入
            writing = false;
            
            if(killed) {
                // 指示因超时而不能再写入
                writeKilled = true;
            }
        }
    }
    
    // 中止读取
    final void killReading() {
        synchronized(readLock) {
            readKilled = true;
        }
    }
    
    // 中止写入
    final void killWriting() {
        synchronized(writeLock) {
            writeKilled = true;
        }
    }
    
    /** when a connect is cancelled then the connection may have been established so prevent reading or writing. */
    // 中止读取与写入
    final void killConnect() {
        killReading();
        killWriting();
    }
    
    /*▲ 取消/关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 从当前通道读取数据并填充到缓冲区dst中（读取的字节数量最多填满缓冲区的剩余空间）
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否读取完成，以及获取实际读取到的字节数
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    @Override
    public final Future<Integer> read(ByteBuffer dst) {
        if(dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        
        return read(false, dst, null, 0L, TimeUnit.MILLISECONDS, null, null);
    }
    
    /*
     * 从当前通道读取数据并填充到缓冲区dst中
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际读到的字节数
     * 允许设置超时时间，即在指定时间内没有完成读取操作的话，抛出异常
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    @Override
    public final <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if(handler == null) {
            throw new NullPointerException("'handler' is null");
        }
        
        if(dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        
        read(false, dst, null, timeout, unit, attachment, handler);
    }
    
    /*
     * 从当前通道读取数据并填充到缓冲区组dsts中(填充到dsts中offset处起的length个缓冲区中)
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际读到的字节数
     * 允许设置超时时间，即在指定时间内没有完成读取操作的话，抛出异常
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    @Override
    public final <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        if(handler == null) {
            throw new NullPointerException("'handler' is null");
        }
        
        if((offset<0) || (length<0) || (offset>dsts.length - length)) {
            throw new IndexOutOfBoundsException();
        }
        
        // 获取bufs[offset, offset+length-1]范围内的子序列
        ByteBuffer[] bufs = Util.subsequence(dsts, offset, length);
        for(ByteBuffer buf : bufs) {
            if(buf.isReadOnly()) {
                throw new IllegalArgumentException("Read-only buffer");
            }
        }
        
        read(true, null, bufs, timeout, unit, attachment, handler);
    }
    
    /*
     * 从当前通道读取数据并填充到缓冲区中
     *
     * 如果isScatteringRead为false，则只需要将读到的数据填充到dst中；
     * 如果isScatteringRead为true，则说明是散射读，需要将读到的数据填充到dsts中offset处起的length个缓冲区中。
     *
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否读取完成，以及获取实际读取到的字节数
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际读到的字节数
     *
     * 返回值与回调句柄是两种指示任务完成的机制，但通常来讲，只需要使用其中一种即可。
     *
     * 允许设置超时时间，即在指定时间内没有完成读取操作的话，抛出异常
     *
     * 注：此IO操作的结果是读取到的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    @SuppressWarnings("unchecked")
    private <V extends Number, A> Future<V> read(boolean isScatteringRead, ByteBuffer dst, ByteBuffer[] dsts, long timeout, TimeUnit unit, A attachment, CompletionHandler<V, ? super A> handler) {
        
        // 如果通道处于关闭状态，则需要给出异常提示
        if(!isOpen()) {
            Throwable e = new ClosedChannelException();
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withFailure(e);
            }
            
            // 处理回调句柄，会视情形进行直接处理或间接处理
            Invoker.invoke(this, handler, attachment, null, e);
            
            return null;
        }
        
        // 如果远程地址为null(还未连接)，则抛出异常
        if(remoteAddress == null) {
            throw new NotYetConnectedException();
        }
        
        // 指示是否有剩余空间来存储即将读取到的数据
        boolean hasSpaceToRead = isScatteringRead || dst.hasRemaining();
        
        // 指示通道的读取功能是否关闭
        boolean shutdown = false;
        
        // check and update state
        synchronized(readLock) {
            // 如果因超时而不能再读取，则抛出异常
            if(readKilled) {
                throw new IllegalStateException("Reading not allowed due to timeout or cancellation");
            }
            
            // 如果已经正在读取，则抛出异常
            if(reading) {
                throw new ReadPendingException();
            }
            
            // 如果通道的读取功能已经关闭，则做标记
            if(readShutdown) {
                shutdown = true;
            } else {
                if(hasSpaceToRead) {
                    // 指示进入读取状态
                    reading = true;
                }
            }
        }
        
        /*
         * immediately complete with -1 if shutdown for read
         * immediately complete with 0 if no space remaining
         */
        // 如果通道已关闭，或者缓冲区空间已经不足了，那么就没必要执行读取了
        if(shutdown || !hasSpaceToRead) {
            Number result;
            
            if(isScatteringRead) {
                result = (shutdown) ? Long.valueOf(-1L) : Long.valueOf(0L);
            } else {
                result = (shutdown) ? -1 : 0;
            }
            
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withResult((V) result);
            }
            
            // 处理回调句柄，会视情形进行直接处理或间接处理
            Invoker.invoke(this, handler, attachment, (V) result, null);
            
            return null;
        }
        
        // 实现异步IO中的读取操作
        return implRead(isScatteringRead, dst, dsts, timeout, unit, attachment, handler);
    }
    
    /**
     * Invoked by read to initiate the I/O operation.
     */
    // 实现异步IO中的读取操作
    abstract <V extends Number, A> Future<V> implRead(boolean isScatteringRead, ByteBuffer dst, ByteBuffer[] dsts, long timeout, TimeUnit unit, A attachment, CompletionHandler<V, ? super A> handler);
    
    
    /*
     * 从源缓冲区src中读取数据，并将读到的内容写入到当前通道中
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否写入完成，以及获取实际写入的字节数
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    @Override
    public final Future<Integer> write(ByteBuffer src) {
        return write(false, src, null, 0L, TimeUnit.MILLISECONDS, null, null);
    }
    
    /*
     * 从源缓冲区src中读取数据，并将读到的内容写入到当前通道中
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际写入的字节数
     * 允许设置超时时间，即在指定时间内没有完成写入操作的话，抛出异常
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    @Override
    public final <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if(handler == null) {
            throw new NullPointerException("'handler' is null");
        }
        
        write(false, src, null, timeout, unit, attachment, handler);
    }
    
    /*
     * 从源缓冲区组srcs中offset处起的length个缓冲区中读取数据，并将读到的内容写入到当前通道中
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际写入的字节数
     * 允许设置超时时间，即在指定时间内没有完成写入操作的话，抛出异常
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是异常。
     */
    @Override
    public final <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        if(handler == null) {
            throw new NullPointerException("'handler' is null");
        }
        
        if((offset<0) || (length<0) || (offset>srcs.length - length)) {
            throw new IndexOutOfBoundsException();
        }
        
        srcs = Util.subsequence(srcs, offset, length);
        
        write(true, null, srcs, timeout, unit, attachment, handler);
    }
    
    /*
     * 将指定缓冲区中的数据写入到当前通道
     *
     * 如果isGatheringWrite为false，则待写的数据暂存在src中；
     * 如果isGatheringWrite为true，则说明是聚集写，需要将srcs中offset处起的length个缓冲区中暂存的数据写入当前通道。
     *
     * 返回值是一个包含IO操作结果的Future，主线程轮询此Future以判断是否读取完成，以及获取实际读取到的字节数
     * 最后一个参数是异步IO回调句柄，由工作线程执行完任务之后通过handler中的回调方法通知主线程，以便主线程获取实际读到的字节数
     *
     * 返回值与回调句柄是两种指示任务完成的机制，但通常来讲，只需要使用其中一种即可。
     *
     * 允许设置超时时间，即在指定时间内没有完成写入操作的话，抛出异常
     *
     * 注：此IO操作的结果是写入的字节数。如果IO操作没成效，则执行结果可以是EOF或异常。
     */
    @SuppressWarnings("unchecked")
    private <V extends Number, A> Future<V> write(boolean isGatheringWrite, ByteBuffer src, ByteBuffer[] srcs, long timeout, TimeUnit unit, A attachment, CompletionHandler<V, ? super A> handler) {
        
        // 指示是否有剩余数据需要写入通道
        boolean hasDataToWrite = isGatheringWrite || src.hasRemaining();
        
        // 指示通道的写入功能是否关闭
        boolean shutdown = false;
        
        // 如果通道处于关闭状态，则需要将通道的写入功能标记为关闭
        if(!isOpen()) {
            shutdown = true;
            
            // 通道未关闭时，进行后续操作
        } else {
            // 如果远程地址为null(还未连接)，则抛出异常
            if(remoteAddress == null) {
                throw new NotYetConnectedException();
            }
            
            /* check and update state */
            synchronized(writeLock) {
                // 如果因超时而不能再写入，则抛出异常
                if(writeKilled) {
                    throw new IllegalStateException("Writing not allowed due to timeout or cancellation");
                }
                
                // 如果已经正在写入，则抛出异常
                if(writing) {
                    throw new WritePendingException();
                }
                
                // 如果通道的写入功能已经关闭，则做标记
                if(writeShutdown) {
                    shutdown = true;
                } else {
                    if(hasDataToWrite) {
                        // 指示进入写入状态
                        writing = true;
                    }
                }
            }
        }
        
        /* channel is closed or shutdown for write */
        // 如果通道的写入功能已经关闭，则需要给出异常提示
        if(shutdown) {
            Throwable e = new ClosedChannelException();
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withFailure(e);
            }
            
            // 处理回调句柄，会视情形进行直接处理或间接处理
            Invoker.invoke(this, handler, attachment, null, e);
            
            return null;
        }
        
        /* nothing to write so complete immediately */
        // 如果缓冲区中已经没有待写数据了，那么就没必要执行写入了
        if(!hasDataToWrite) {
            Number result = (isGatheringWrite) ? (Number) 0L : (Number) 0;
            
            // 未设置回调handler时，直接包装异常信息
            if(handler == null) {
                return CompletedFuture.withResult((V) result);
            }
            
            // 处理回调句柄，会视情形进行直接处理或间接处理
            Invoker.invoke(this, handler, attachment, (V) result, null);
            
            return null;
        }
        
        // 实现异步IO中的写入操作
        return implWrite(isGatheringWrite, src, srcs, timeout, unit, attachment, handler);
    }
    
    /**
     * Invoked by write to initiate the I/O operation.
     */
    // 实现异步IO中的写入操作
    abstract <V extends Number, A> Future<V> implWrite(boolean isGatheringWrite, ByteBuffer src, ByteBuffer[] srcs, long timeout, TimeUnit unit, A attachment, CompletionHandler<V, ? super A> handler);
    
    /*▲ 读/写操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 地址信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取绑定的本地地址
    @Override
    public final SocketAddress getLocalAddress() throws IOException {
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
        
        // 对指定的地址进行安全校验
        return Net.getRevealedLocalAddress(localAddress);
    }
    
    // 获取连接到的远程地址
    @Override
    public final SocketAddress getRemoteAddress() throws IOException {
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
        
        return remoteAddress;
    }
    
    /*▲ 地址信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断当前通道是否处于开启(运行)状态
    @Override
    public final boolean isOpen() {
        return !closed;
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取当前通道支持的Socket配置参数集合
    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }
    
    // 获取指定名称的Socket配置参数
    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getOption(SocketOption<T> name) throws IOException {
        if(name == null) {
            throw new NullPointerException();
        }
        
        if(!supportedOptions().contains(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        
        try {
            // 添加一个读锁
            begin();
            
            if(name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                // SO_REUSEADDR emulated when using exclusive bind
                return (T) Boolean.valueOf(isReuseAddress);
            }
            
            return (T) Net.getSocketOption(fd, Net.UNSPEC, name);
        } finally {
            // 移除一个读锁
            end();
        }
    }
    
    // 设置指定名称的Socket配置参数
    @Override
    public final <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        if(name == null) {
            throw new NullPointerException();
        }
        
        if(!supportedOptions().contains(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        
        try {
            // 添加一个读锁
            begin();
            
            // 如果通道的写入功能已经关闭，则抛出异常
            if(writeShutdown) {
                throw new IOException("Connection has been shutdown for writing");
            }
            
            if(name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
                // SO_REUSEADDR emulated when using exclusive bind
                isReuseAddress = (Boolean) value;
            } else {
                Net.setSocketOption(fd, Net.UNSPEC, name, value);
            }
            
            return this;
        } finally {
            // 移除一个读锁
            end();
        }
    }
    
    /*▲ Socket配置参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 加锁/解锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Marks beginning of access to file descriptor/handle
     */
    // 添加一个读锁
    final void begin() throws IOException {
        closeLock.readLock().lock();
        if(!isOpen()) {
            throw new ClosedChannelException();
        }
    }
    
    /**
     * Marks end of access to file descriptor/handle
     */
    // 移除一个读锁
    final void end() {
        closeLock.readLock().unlock();
    }
    
    /*▲ 加锁/解锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());
        sb.append('[');
        synchronized(stateLock) {
            if(!isOpen()) {
                sb.append("closed");
            } else {
                switch(state) {
                    case ST_UNCONNECTED:
                        sb.append("unconnected");
                        break;
                    case ST_PENDING:
                        sb.append("connection-pending");
                        break;
                    case ST_CONNECTED:
                        sb.append("connected");
                        if(readShutdown) {
                            sb.append(" ishut");
                        }
                        if(writeShutdown) {
                            sb.append(" oshut");
                        }
                        break;
                }
                
                if(localAddress != null) {
                    sb.append(" local=");
                    sb.append(Net.getRevealedLocalAddressAsString(localAddress));
                }
                
                if(remoteAddress != null) {
                    sb.append(" remote=");
                    sb.append(remoteAddress.toString());
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }
    
    
    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();
        
        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>(5);
            set.add(StandardSocketOptions.SO_SNDBUF);
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_KEEPALIVE);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            if(Net.isReusePortAvailable()) {
                set.add(StandardSocketOptions.SO_REUSEPORT);
            }
            set.add(StandardSocketOptions.TCP_NODELAY);
            set.addAll(ExtendedSocketOptions.options(SOCK_STREAM));
            return Collections.unmodifiableSet(set);
        }
    }
    
}
