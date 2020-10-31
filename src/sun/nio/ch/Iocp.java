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

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ShutdownChannelGroupException;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jdk.internal.misc.Unsafe;

/**
 * Windows implementation of AsynchronousChannelGroup encapsulating an I/O completion port.
 */
// 通道组在windows上的实现，该实现与windows的完成端口机制息息相关
class Iocp extends AsynchronousChannelGroupImpl {
    
    private static final long INVALID_HANDLE_VALUE = -1L;
    
    /** handle to completion port */
    private final long port;        // "完成端口"对象(的本地引用)
    
    private int nextCompletionKey;  // 完成键，用来关联一个支持重叠IO结构的通道
    
    /*
     * 完成键到通道的映射。
     * 将完成键与通道一一对应起来，这样可以通过完成键找到其对应的通道。
     */
    private final Map<Integer, OverlappedChannel> keyToChannel = new HashMap<>();
    
    /** maps completion key to channel */
    // 操作keyToChannel时使用的锁
    private final ReadWriteLock keyToChannelLock = new ReentrantReadWriteLock();
    
    // 指示完成端口是否已经关闭
    private boolean closed;
    
    /**
     * the set of "stale" OVERLAPPED structures.
     * These OVERLAPPED structures relate to I/O operations
     * where the completion notification was not received in a timely manner after the channel is closed.
     */
    // 记录失效的重叠IO结构的本地引用
    private final Set<Long> staleIoSet = new HashSet<>();
    
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    
    
    static {
        IOUtil.load();
        initIDs();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 构造异步通道组，实际上是进行完成端口的初始化操作
     *
     * 涉及的完成端口操作：
     *【1】通知系统内核创建"完成端口"对象；
     *　 　限制本地(native层)允许新建的工作线程(先入后出)数量；
     *　 　初始化IO完成队列(先入先出)
     *
     * 参见：Iocp#createIoCompletionPort()
     */
    Iocp(AsynchronousChannelProvider provider, ThreadPool pool) throws IOException {
        super(provider, pool);
        
        /*
         * 获取待创建的(本地)工作线程数量。
         * 该参数用来告知IO完成端口在同一时间内最多能有多少个线程处于可运行状态。
         */
        int threadCount = fixedThreadCount();
        
        // 通知内核创建"完成端口"对象(此时只有最后一个参数有用)，并返回"完成端口"对象(的本地引用)
        this.port = createIoCompletionPort(INVALID_HANDLE_VALUE, 0, 0, threadCount);
        
        // 初始化完成键
        this.nextCompletionKey = 1;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 启动 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 在Java层和本地(native层)批量启动工作线程，以便后续处理已经完成的IO操作
     *
     * 涉及的完成端口操作：
     *【2】启动Java层的工作线程；
     *　 　启动本地(native层)的工作线程；
     *　 　Java层与本地(native层)的工作线程是一一对应的；
     */
    Iocp start() {
        /*
         * 构造一个轮询任务
         *
         * 该任务由工作线程执行，通过轮询从"完成端口"获取通知。
         * 当没有新的通知时，工作线程陷入阻塞。
         */
        EventHandlerTask task = new EventHandlerTask();
        
        // 在Java层和本地(native层)批量启动工作线程，以执行上述构造的轮询任务
        startThreads(task);
        
        // 返回启动后的通道组
        return this;
    }
    
    /*▲ 启动 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 执行任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 将指定的任务推送到任务队列中，并且向阻塞的工作线程发送模拟IO信号，
     * 以唤醒工作线程来处理任务队列中的task。
     */
    @Override
    void executeOnHandlerTask(Runnable task) {
        synchronized(this) {
            if(closed) {
                throw new RejectedExecutionException();
            }
            
            // 将指定的任务推送到任务队列中
            offerTask(task);
            
            // 向阻塞的工作线程发送模拟IO信号，以唤醒阻塞在getQueuedCompletionStatus()上的工作线程来处理任务队列中的task
            wakeup();
        }
    }
    
    /*▲ 执行任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 外部通道 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 添加一个外部通道(不会与完成端口绑定)，返回其对应的完成键
    @Override
    final Object attachForeignChannel(final Channel channel, FileDescriptor fdObj) throws IOException {
        OverlappedChannel ch = new OverlappedChannel() {
            public <V, A> PendingFuture<V, A> getByOverlapped(long overlapped) {
                return null;
            }
            
            public void close() throws IOException {
                channel.close();
            }
        };
        
        return associate(ch, 0L);
    }
    
    // 根据指定的完成键，移除其对应的外部通道
    @Override
    final void detachForeignChannel(Object key) {
        disassociate((Integer) key);
    }
    
    /*▲ 外部通道 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭通道组内关联的所有通道
    @Override
    void closeAllChannels() {
        
        /*
         * On Windows the close operation will close the socket/file handle
         * and then wait until all outstanding I/O operations have aborted.
         * This is necessary as each channel's cache of OVERLAPPED structures
         * can only be freed once all I/O operations have completed. As I/O
         * completion requires a lookup of the keyToChannel then we must close
         * the channels when not holding the write lock.
         */
        final int MAX_BATCH_SIZE = 32;
        
        OverlappedChannel[] channels = new OverlappedChannel[MAX_BATCH_SIZE];
        
        int count;
        
        do {
            // grab a batch of up to 32 channels
            keyToChannelLock.writeLock().lock();
            count = 0;
            try {
                // 遍历映射中的完成键
                for(Integer key : keyToChannel.keySet()) {
                    // 根据完成键获取对应的通道
                    channels[count++] = keyToChannel.get(key);
                    if(count >= MAX_BATCH_SIZE) {
                        break;
                    }
                }
            } finally {
                keyToChannelLock.writeLock().unlock();
            }
            
            // 关闭所有通道
            for(int i = 0; i<count; i++) {
                try {
                    // 关闭(释放)资源
                    channels[i].close();
                } catch(IOException ignore) {
                }
            }
        } while(count>0);
    }
    
    // 关闭所有工作线程(包括保底线程)
    @Override
    void shutdownHandlerTasks() {
        // 获取Java层工作线程的数量(包含了保底线程的数量)
        int nThreads = threadCount();
        
        // 唤醒所有阻塞在getQueuedCompletionStatus()上的工作线程，并指示其结束运行
        while(nThreads-->0) {
            wakeup();
        }
    }
    
    /*▲ 关闭  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 完成端口 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Associate the given handle with this group
     */
    /*
     * 将指定文件(包括Socket)的引用handle关联到"完成端口"上，并在keyToChannel中记录handle所在通道(支持重叠IO结构)的引用。
     * 返回值为与通道channel建立关联的完成键。
     *
     * 涉及的完成端口操作：
     *【4】将通道channel与完成端口handle关联起来。
     *　 　这样的话，当作用在channel上的IO操作结束后，才能通知到绑定的完成端口。
     */
    int associate(OverlappedChannel channel, long handle) throws IOException {
        
        // 加锁(写锁)
        keyToChannelLock.writeLock().lock();
        
        // generate a completion key (if not shutdown)
        int completionKey;
        try {
            // 如果通道组已关闭，则抛异常
            if(isShutdown()) {
                throw new ShutdownChannelGroupException();
            }
            
            // generate unique key
            do {
                // 生成一个完成键(有效的编号从1开始)
                completionKey = nextCompletionKey++;
                
                // 如果该完成键已经在keyToChannel中了，则需要重试；简单地说，这里需要确保完成键的唯一性
            } while((completionKey == 0) || keyToChannel.containsKey(completionKey));
            
            /* associate with I/O completion port */
            // 如果文件/Socket有效，则将其引用(handle)与完成端口进行关联
            if(handle != 0L) {
                createIoCompletionPort(handle, port, completionKey, 0);
            }
            
            // 对完成键与通道做映射
            keyToChannel.put(completionKey, channel);
        } finally {
            // 解锁(写锁)
            keyToChannelLock.writeLock().unlock();
        }
        
        return completionKey;
    }
    
    /**
     * Disassociate channel from the group.
     */
    // 解除与指定的完成键关联的异步IO通道
    void disassociate(int key) {
        boolean checkForShutdown = false;
    
        keyToChannelLock.writeLock().lock();
    
        try {
            keyToChannel.remove(key);
        
            /* last key to be removed so check if group is shutdown */
            // 如果keyToChannel已经为空了，则需要做标记，稍后会关闭通道组
            if(keyToChannel.isEmpty()) {
                checkForShutdown = true;
            }
        
        } finally {
            keyToChannelLock.writeLock().unlock();
        }
    
        // 如果keyToChannel已经为空了，而且异步IO通道组正在准备关闭
        if(checkForShutdown && isShutdown()) {
            try {
                // 立即关闭异步IO通道组，包括：关闭通道、关闭工作线程、关闭线程池
                shutdownNow();
            } catch(IOException ignore) {
            }
        }
    }
    
    /*
     * 该方法有两种用途：
     * 1. 通知内核创建"完成端口"对象(此时只有最后一个参数有用)，并返回"完成端口"对象(地址)
     *    concurrency: "完成端口"在并行中允许的最大线程数，一个经验值是可用CPU内核数量的两倍，以便提高资源利用率。
     *                 如果设置为0，则创建的工作线程数量由本地决定。
     * 2. 为handle(文件/Socket引用)关联"完成端口"(existingPort)
     *    handle       : 文件/Socket引用
     *    existingPort : 现有的"完成端口"对象
     *    completionKey: 完成键，用来关联一个支持重叠IO结构的通道(系统本身不关心这个值是多少)
     */
    private static native long createIoCompletionPort(long handle, long existingPort, int completionKey, int concurrency) throws IOException;
    
    /*
     * 无限阻塞，直到"完成端口"completionPort有新的通知就绪时，获取该通知的内容，并存入status中
     *
     * completionPort：指向"完成端口"内核对象的指针
     * status：存储IO结束后的返回的通知(数据)
     */
    private static native void getQueuedCompletionStatus(long completionPort, CompletionStatus status) throws IOException;
    
    // 唤醒阻塞在getQueuedCompletionStatus()上的工作线程
    private void wakeup() {
        try {
            // 向"完成端口"port处的"完成端口"对象发送消息，可以唤醒阻塞的getQueuedCompletionStatus()方法
            postQueuedCompletionStatus(port, 0);
        } catch(IOException e) {
            // should not happen
            throw new AssertionError(e);
        }
    }
    
    /*
     * 向"完成端口"completionPort处的"完成端口"对象发送消息，可以唤醒阻塞的getQueuedCompletionStatus()方法
     * 注：postQueuedCompletionStatus中传递的数据会到达getQueuedCompletionStatus()的CompletionStatus参数中
     */
    private static native void postQueuedCompletionStatus(long completionPort, int completionKey) throws IOException;
    
    // 关闭完成端口，并且释放已经失效的重叠IO结构在本地所占的内存
    void implClose() {
        synchronized(this) {
            if(closed) {
                return;
            }
            closed = true;
        }
        
        close0(port);
        
        synchronized(staleIoSet) {
            // 遍历所有失效的重叠IO结构
            for(Long ov : staleIoSet) {
                unsafe.freeMemory(ov);
            }
            staleIoSet.clear();
        }
    }
    
    // 关闭完成端口
    private static native void close0(long handle);
    
    // 将重叠IO结构中的错误消息包装为IOException后返回
    private static IOException translateErrorToIOException(int error) {
        String msg = getErrorMessage(error);
        if(msg == null) {
            msg = "Unknown error: 0x0" + Integer.toHexString(error);
        }
        
        return new IOException(msg);
    }
    
    // 解析重叠IO结构中的错误消息
    private static native String getErrorMessage(int error);
    
    /*▲ 完成端口  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 判断通道组是否为空，即该通道组内是否包含通道
    @Override
    boolean isEmpty() {
        keyToChannelLock.writeLock().lock();
        
        try {
            return keyToChannel.isEmpty();
        } finally {
            keyToChannelLock.writeLock().unlock();
        }
    }
    
    /**
     * Invoked when a channel associated with this port is closed before
     * notifications for all outstanding I/O operations have been received.
     */
    // 将指定的重叠IO结构标记为无效
    void makeStale(Long overlapped) {
        synchronized(staleIoSet) {
            staleIoSet.add(overlapped);
        }
    }
    
    /**
     * Checks if the given OVERLAPPED is stale and if so, releases it.
     */
    // 检查指定的重叠IO结构的本地引用，如果该重叠IO结构已经失效了，则释放其所占的本地内存
    private void checkIfStale(long ov) {
        synchronized(staleIoSet) {
            boolean removed = staleIoSet.remove(ov);
            if(removed) {
                unsafe.freeMemory(ov);
            }
        }
    }
    
    private static native void initIDs();
    
    
    /** Channels implements this interface support overlapped I/O and can be associated with a completion port. */
    // 支持重叠IO结构的通道
    interface OverlappedChannel extends Closeable {
        /**
         * Returns a reference to the pending I/O result.
         */
        /*
         * 从任务结果映射集移除一条记录，并返回移除掉的重叠IO结构缓存池
         * 重叠IO结构被移除下来后，会先尝试将其缓存，缓存池已满时则直接释放重叠IO结构的本地内存
         */
        <V, A> PendingFuture<V, A> getByOverlapped(long overlapped);
    }
    
    /** The handler for consuming the result of an asynchronous I/O operation. */
    /*
     * 显式设置异步任务的执行结果
     *
     * 首先会将结果设置给PendingFuture，如果用户显式设置了回调句柄CompletionHandler，
     * 则还会进一步讲执行结果设置给CompletionHandler。
     */
    interface ResultHandler {
        /**
         * Invoked if the I/O operation completes successfully.
         */
        // 设置任务执行的结果
        void completed(int bytesTransferred, boolean canInvokeDirect);
        
        /**
         * Invoked if the I/O operation fails.
         */
        // 设置任务执行中的异常信息
        void failed(int error, IOException ioe);
    }
    
    /** Container for data returned by GetQueuedCompletionStatus. */
    /*
     * IO完成记录，这是对本地(native层)IO完成队列中的元素的包装
     *
     * 参见：Iocp#getQueuedCompletionStatus()
     */
    private static class CompletionStatus {
        private int bytesTransferred;   // 本次IO操作处理的字节数
        private int completionKey;      // 完成键，用来关联一个支持重叠IO结构的通道
        private long overlapped;        // 重叠IO结构的本地引用
        private int error;              // 异步IO操作中出现的错误信息，如超时、断网等
        
        private CompletionStatus() {
        }
        
        int bytesTransferred() {
            return bytesTransferred;
        }
        
        int completionKey() {
            return completionKey;
        }
        
        long overlapped() {
            return overlapped;
        }
        
        int error() {
            return error;
        }
        
    }
    
    /** Long-running task servicing system-wide or per-file completion port. */
    /*
     * 轮询任务
     *
     * 该任务由工作线程执行，通过轮询从"完成端口"获取通知。
     * 当没有新的通知时，工作线程陷入阻塞。
     */
    private class EventHandlerTask implements Runnable {
        
        /*
         * 执行轮询任务，即处理已经完成的IO操作；如果IO操作还未完成，则陷入阻塞
         *
         * 涉及的完成端口操作：
         *【3】Java层与本地(native层)的工作线程已经启动；
         *　 　执行getQueuedCompletionStatus()，尝试从IO完成队列获取已执行完成的IO记录；
         *　 　当本地(native层)的IO完成队列为空时，Java层与本地(native层)的工作线程均陷入阻塞...
         *　 　当有IO操作完成，或者收到模拟IO操作完成的请求时，会向IO完成队列插入一条记录；
         *　 　接下来，唤醒本地(native层)的工作线程(后入先出，优先唤醒最后阻塞的线程)，将出队的记录填充到ioResult中；
         *　 　唤醒在Java层的工作线程，对本地(native层)已完成的IO操作做出响应(设置任务结果)。
         *
         * 注：getQueuedCompletionStatus()方法中的第一个参数是完成端口对象，它指定了工作线程对哪个完成端口感兴趣...
         * 　　只有目标完成端口上收到"IO完成队列有新记录出现"的消息后，才会唤醒阻塞在该完成端口上的工作线程。
         * 　　那么某个IO操作完成后，它如何知道通知哪个完成端口呢？
         * 　　答案是事先会用createIoCompletionPort()方法来完成文件/Socket引用与完成端口的绑定，这也是发挥了createIoCompletionPort()方法的第二个作用。
         * 　　这样的话，作用在某个文件/Socket上的IO操作结束后，其对应的完成端口就能收到通知了。
         *
         * 参见：AsynchronousChannelGroupImpl#startThreads(Runnable)
         */
        public void run() {
            
            // 获取当前线程内的线程局部缓存值：(递归)调用计数器
            Invoker.GroupAndInvokeCount myGroupAndInvokeCount = Invoker.getGroupAndInvokeCount();
            
            /*
             * 是否允许在当前线程内直接处理回调句柄
             *
             * 除保底线程外，每个工作线程内都持有的局部缓存。
             * 换句话说，myGroupAndInvokeCount不为null的话，说明当前线程不是保底线程。
             *
             * true : 允许在当前线程内直接处理回调句柄(除保底线程之外的工作线程) -固定 非固定
             * false: 不能在当前线程内直接处理回调句柄(保底线程) - 非固定
             */
            boolean canInvokeDirect = (myGroupAndInvokeCount != null);
            
            /*
             * 构造一条空的IO完成记录。
             * 等IO完成队列中有新的元素进来后，会将这些元素出队并填充到这条IO完成记录中。
             */
            CompletionStatus ioResult = new CompletionStatus();
            
            /*
             * 在当前线程退出时，释放需要重启一个线程来替换当前线程。
             * 如果是因为发生了错误/异常而导致当前线程退出，那么往往需要重启一个线程来替换当前线程，即设置replaceMe为true。
             * 如果是正常被唤醒后退出，那么不需要再重启别的线程。
             */
            boolean replaceMe = false;
            
            try {
                for(; ; ) {
                    /* reset invoke count */
                    if(myGroupAndInvokeCount != null) {
                        myGroupAndInvokeCount.resetInvokeCount();
                    }
                    
                    /* wait for I/O completion event. A error here is fatal (thread will not be replaced) */
                    replaceMe = false;
                    
                    
                    //========================本地(native层)的IO完成队列为空时，当前工作线程将进入阻塞========================//
                    
                    try {
                        // 无限阻塞，直到"完成端口"port有新的通知就绪时，获取该通知的内容，并存入ioResult中
                        getQueuedCompletionStatus(port, ioResult);
                    } catch(IOException x) {
                        // should not happen
                        x.printStackTrace();
                        return;
                    }
                    
                    
                    //========================工作线程醒来后，获取IO完成记录中的信息========================//
                    
                    // 从IO完成记录中获取到完成键
                    int completionKey = ioResult.completionKey();
                    
                    // 从IO完成记录中获取到重叠IO结构的本地引用
                    long overlapped = ioResult.overlapped();
                    
                    // 从IO完成记录中获取到传输(读取/写入)的字节数量
                    int bytesTransferred = ioResult.bytesTransferred();
                    
                    // 从IO完成记录中获取到异步IO操作中出现的错误信息
                    int error = ioResult.error();
                    
                    
                    //========================被模拟IO信号(请求)唤醒========================//
                    
                    /*
                     * 当非工作线程向完成端口发送了一个模拟的IO操作之后，这个模拟IO操作在执行结束后，也会进入IO完成队列。
                     * 这就会触发唤醒工作线程的流程...
                     * 这种发送模拟IO信号的机制，可以强制让工作线程醒来，并让工作线程执行预设的任务，或者直接让工作线程退出。
                     *
                     * 注：发送模拟IO信号的功能是由postQueuedCompletionStatus()方法来完成的。
                     *
                     * 参见：Iocp#wakeup()
                     */
                    
                    /* handle wakeup to execute task or shutdown */
                    if(completionKey == 0 && overlapped == 0L) {
                        Runnable task = pollTask();
                        
                        // 如果被模拟信号唤醒，但是没有待执行的任务，说明是收到了"关闭"的指示，此时应当结束当前工作线程
                        if(task == null) {
                            return;
                        }
                        
                        /* run task (if error/exception then replace thread) */
                        replaceMe = true;
                        
                        /*
                         * 处理任务队列中的task，而这个task通常就是处理回调句柄
                         *
                         * 参见：Iocp#executeOnHandlerTask
                         */
                        task.run();
                        
                        continue;
                    }
                    
                    
                    //========================被常规的异步IO操作唤醒========================//
                    
                    /*
                     * 每个异步IO操作完成后，会生成一条IO完成记录插入到IO完成队列中。
                     * 然后，唤醒本地(native层)的工作线程，并取出IO完成队列中的队头元素进行处理。
                     * 检查队头的IO完成记录，根据其作用的文件/Socket，找到其关联的完成端口。
                     * 唤醒阻塞在该完成端口上的工作线程(Java层)
                     *
                     * 注：工作线程被唤醒遵循后入先出，即最后阻塞的工作线程会被优先唤醒。
                     * 　　如果某个IO操作足够耗时的话，会发现总是同一个工作线程被频繁地唤醒，这也是后入先出的表现。
                     */
                    
                    /* map key to channel */
                    OverlappedChannel channel = null;
                    
                    // 加锁(读锁)
                    keyToChannelLock.readLock().lock();
                    
                    try {
                        
                        /*
                         * 通过完成键，找到上述IO完成记录对应的通道。
                         * 换句话说，就是了解这条IO完成记录中的IO操作是作用在哪个通道上的。
                         */
                        channel = keyToChannel.get(completionKey);
                        
                        /*
                         * 如果ch为null，说明keyToChannel中已经把completionKey对应的那条记录移除了。
                         * 参见：Iocp#disassociate()
                         */
                        if(channel == null) {
                            // 检查指定的重叠IO结构的本地引用，如果该重叠IO结构已经失效了，则释放其所占的本地内存
                            checkIfStale(overlapped);
                            
                            // 继续遍历，从IO完成队列中获取下一个队头元素
                            continue;
                        }
                    } finally {
                        // 解锁(读锁)
                        keyToChannelLock.readLock().unlock();
                    }
                    
                    /* lookup I/O request */
                    /*
                     * 根据重叠IO结构的本地引用，从重叠IO结构的缓存池中检索出与overlapped关联的挂起的任务。
                     *
                     * 只有在目标IO操作没成效的情形下，这里才会获取到非null的任务。
                     * 因为如果IO操作没成效，任务执行结果在当场就被设置完了，没必要等到这里再处理。
                     *
                     * 参见Iocp.ResultHandler的实现类
                     */
                    PendingFuture<?, ?> future = channel.getByOverlapped(overlapped);
                    
                    // 如果future为null，意味着该重叠IO结构关联的future已被移除了，此处需要考虑清理overlapped
                    if(future == null) {
                        /*
                         * we get here if the OVERLAPPED structure is associated with an I/O operation on a channel that was closed but the I/O operation event wasn't read in a timely manner.
                         * Alternatively, it may be related to a tryLock operation as the OVERLAPPED structures for these operations are not in the I/O cache.
                         */
                        // 检查指定的重叠IO结构的本地引用，如果该重叠IO结构已经失效了，则释放其所占的本地内存
                        checkIfStale(overlapped);
                        continue;
                    }
                    
                    /*
                     * 至此，说明之前的IO操作是有成效的，因此挂起了一个future等待填充执行结果。
                     * 现在获取到了这个挂起的任务，可以向其填充执行结果了。
                     */
                    
                    /* synchronize on result in case I/O completed immediately and was handled by initiator */
                    synchronized(future) {
                        /*
                         * 如果future已经被提前填充了执行结果，那么后续操作也没必要进行了。
                         * 例如在限定时间内等不到预想的执行结果时，可以自行填充一个执行结果。
                         */
                        if(future.isDone()) {
                            continue;
                        }
                        
                        // not handled by initiator
                    }
                    
                    
                    /* (if error/exception then replace thread) */
                    replaceMe = true;
                    
                    // 获取异步IO操作的引用(ReadTask/WriteTask/LockTask/ConnectTask/AcceptTask)
                    ResultHandler rh = (ResultHandler) future.getContext();
                    
                    // 如果一切正常，那么可以设置任务执行结果
                    if(error == 0) {
                        /*
                         * bytesTransferred: 本地操作传输的字节数
                         * canInvokeDirect : 是否允许在当前线程内直接处理回调句柄
                         */
                        rh.completed(bytesTransferred, canInvokeDirect);
                        
                        // 如果有内部错误发生，则设置任务执行中的异常信息
                    } else {
                        IOException ex = translateErrorToIOException(error);
                        rh.failed(error, ex);
                    }
                } // for(;;)
            } finally {
                // last thread to exit when shutdown releases resources
                int remaining = threadExit(this, replaceMe);
                
                // 如果Java层的工作线程全部退出了，且通道组也已关闭了
                if(remaining == 0 && isShutdown()) {
                    // 接下来要关闭完成端口，并释放那些已经失效的重叠IO结构占用的本地内存
                    implClose();
                }
            }
        }
    }
    
}
