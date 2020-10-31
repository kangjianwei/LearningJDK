/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Future for a pending I/O operation.
 * A PendingFuture allows for the attachment of an additional arbitrary context object and a timer task.
 */
/*
 * "已挂起的结果"，用来包装异步IO的操作结果。
 *
 * 当异步IO操作还未结束时，该操作会生产一个PendingFuture，
 * 并等到IO操作结束之后，再向PendingFuture中填充执行结果。
 *
 * 当前类通常用在那些IO操作有成效的场合，比如顺利读取或写入了一批字节。
 *
 * 参见：CompletedFuture
 */
final class PendingFuture<V, A> implements Future<V> {
    
    private volatile V result;      // 异步IO操作的返回值
    private volatile Throwable exc; // 异步IO操作的异常
    
    private final A attachment;     // 附件
    
    /* true if result (or exception) is available */
    private volatile boolean haveResult;    // 是否已向当前对象填充了任务执行结果（正常返回值或异常）
    
    private final AsynchronousChannel channel;  // 异步IO操作所属的Channel（即在哪个Channel上发起了异步IO操作）
    
    private final CompletionHandler<V, ? super A> handler; // 回调句柄，可能为null
    
    /** optional context object */
    private volatile Object context;    // 当前挂起的异步IO操作(等待完成)
    
    /** optional timer task that is cancelled when result becomes available */
    private Future<?> timeoutTask;  // 定时任务，在异步IO操作超时的时候给出提醒
    
    /** latch for waiting (created lazily if needed) */
    private CountDownLatch latch;   // 闭锁
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    PendingFuture(AsynchronousChannel channel) {
        this(channel, null, null);
    }
    
    PendingFuture(AsynchronousChannel channel, CompletionHandler<V, ? super A> handler, A attachment) {
        this.channel = channel;
        this.handler = handler;
        this.attachment = attachment;
    }
    
    PendingFuture(AsynchronousChannel channel, Object context) {
        this(channel, null, null, context);
    }
    
    PendingFuture(AsynchronousChannel channel, CompletionHandler<V, ? super A> handler, A attachment, Object context) {
        this.channel = channel;
        this.handler = handler;
        this.attachment = attachment;
        this.context = context;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取任务结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取IO操作的执行结果(该结果可以是异常)；如果任务未完成，则陷入阻塞以等待任务完成
    @Override
    public V get() throws ExecutionException, InterruptedException {
        // 如果还没有结果
        if(!haveResult) {
            // 当前线程是否需要进入阻塞
            boolean needToWait = prepareForWait();
            if(needToWait) {
                // 阻塞IO操作，等待任务执行完成
                latch.await();
            }
        }
        
        // 如果任务执行中抛出了异常，这里再次将其抛出
        if(exc != null) {
            if(exc instanceof CancellationException) {
                throw new CancellationException();
            }
            
            throw new ExecutionException(exc);
        }
        
        return result;
    }
    
    // 获取IO操作的执行结果(该结果可以是异常)；如果任务未完成，则阻塞一段时间以等待任务完成；如果超时后还没等到任务完成，会抛出超时异常
    @Override
    public V get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        // 如果还没有结果
        if(!haveResult) {
            // 当前线程是否需要进入阻塞
            boolean needToWait = prepareForWait();
            if(needToWait) {
                // 阻塞IO操作，等待任务执行完成
                if(!latch.await(timeout, unit)) {
                    throw new TimeoutException();
                }
            }
        }
        
        // 如果任务执行中抛出了异常，这里再次将其抛出
        if(exc != null) {
            if(exc instanceof CancellationException) {
                throw new CancellationException();
            }
            
            throw new ExecutionException(exc);
        }
        
        return result;
    }
    
    /*▲ 获取任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置任务结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets the result
     */
    // 设置正常结果或异常信息
    void setResult(V res, Throwable e) {
        if(e == null) {
            setResult(res);
        } else {
            setFailure(e);
        }
    }
    
    /**
     * Sets the result, or a no-op if the result or exception is already set.
     */
    // 设置任务执行结果(正常)
    void setResult(V res) {
        synchronized(this) {
            if(haveResult) {
                return;
            }
            
            // 记录正常结果
            result = res;
            
            // 标记为已有结果/异常
            haveResult = true;
            
            // 如果之前设置了定时任务来监控相关IO操作，则此处可以取消它了，因为已经有结果了
            if(timeoutTask != null) {
                // 取消定时任务
                timeoutTask.cancel(false);
            }
            
            if(latch != null) {
                // 撤去一道闸门，待所有阀门都撤去后，所有被阻塞的获取数据的线程将全部被唤醒
                latch.countDown();
            }
        }
    }
    
    /**
     * Sets the result, or a no-op if the result or exception is already set.
     */
    // 设置任务执行异常信息(异常)
    void setFailure(Throwable x) {
        if(!(x instanceof IOException) && !(x instanceof SecurityException)) {
            x = new IOException(x);
        }
    
        synchronized(this) {
            if(haveResult) {
                return;
            }
        
            // 记录异常结果
            exc = x;
        
            // 标记为已有结果/异常
            haveResult = true;
        
            // 如果之前设置了定时任务来监控相关IO操作，则此处可以取消它了，因为已经有结果了
            if(timeoutTask != null) {
                // 取消定时任务
                timeoutTask.cancel(false);
            }
        
            if(latch != null) {
                // 撤去一道闸门，待所有阀门都撤去后，所有被阻塞的获取数据的线程将全部被唤醒
                latch.countDown();
            }
        }
    }
    
    /*▲ 设置任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取任务执行结果（不会感知到异常）
    V value() {
        return result;
    }
    
    // 如果任务执行结果是CancellationException，则返回它
    Throwable exception() {
        return (exc instanceof CancellationException) ? null : exc;
    }
    
    // 返回附件
    A attachment() {
        return attachment;
    }
    
    // 返回任务所属的Channel（即在哪个Channel上发起了任务）
    AsynchronousChannel channel() {
        return channel;
    }
    
    // 返回回调handler，用于"回调式"场景
    CompletionHandler<V, ? super A> handler() {
        return handler;
    }
    
    // 返回当前挂起的异步IO操作(等待完成)
    Object getContext() {
        return context;
    }
    
    // 设置当前挂起的异步IO操作(等待完成)
    void setContext(Object context) {
        this.context = context;
    }
    
    /*
     * 设置一个已经安排好的定时任务task。
     * 当某个异步IO操作在限定时间内还没完成时，需要通过此task给出提醒。
     * 当然，如果目标操作提前完成了，或者提前失败了，那么就需要取消该task了。
     */
    void setTimeoutTask(Future<?> task) {
        synchronized(this) {
            // 如果目标操作已经完成了，那么也就不需要此定时任务做监督了，直接取消该定时任务即可
            if(haveResult) {
                // 取消定时任务
                task.cancel(false);
            } else {
                // 依计划设置定时任务
                this.timeoutTask = task;
            }
        }
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中止 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 中止异步任务，包括取消或中断，参数表示是否需要关闭异步通道
    @Override
    public boolean cancel(boolean closeChannel) {
        synchronized(this) {
            // 如果任务已完成，则直接返回
            if(haveResult) {
                return false;    // already completed
            }
            
            // 执行回调
            if(channel() instanceof Cancellable) {
                ((Cancellable) channel()).onCancel(this);
            }
            
            // set result and cancel timer
            exc = new CancellationException();
            
            // 标记任务已完成
            haveResult = true;
            
            // 如果设置了监视IO操作的定时任务，则取消它
            if(timeoutTask != null) {
                timeoutTask.cancel(false);
            }
        }
        
        /* close channel if forceful cancel */
        // 如有必要，则关闭异步通道
        if(closeChannel) {
            try {
                channel().close();
            } catch(IOException ignore) {
            }
        }
        
        // 撤去闸门，唤醒等待获取任务执行结果的线程
        if(latch != null) {
            latch.countDown();
        }
        
        return true;
    }
    
    /*▲ 中止 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 是否已向当前对象填充了任务执行结果(异步IO操作是否已完成)
    @Override
    public boolean isDone() {
        return haveResult;
    }
    
    // 判断当前任务是否已中止
    @Override
    public boolean isCancelled() {
        return (exc instanceof CancellationException);
    }
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /** creates latch if required; return true if caller needs to wait */
    // 判断当前线程是否需要等待任务执行完成
    private boolean prepareForWait() {
        synchronized(this) {
            // 如果已经有结果，则不需要等待
            if(haveResult) {
                return false;
            }
            
            // 初始化闭锁（这里只设置了一道阀门）
            if(latch == null) {
                latch = new CountDownLatch(1);
            }
            
            return true;
        }
    }
    
}
