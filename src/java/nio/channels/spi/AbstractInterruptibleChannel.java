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

/*
 */

package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.InterruptibleChannel;
import jdk.internal.misc.SharedSecrets;
import sun.nio.ch.Interruptible;

/**
 * Base implementation class for interruptible channels.
 *
 * <p> This class encapsulates the low-level machinery required to implement
 * the asynchronous closing and interruption of channels.  A concrete channel
 * class must invoke the {@link #begin begin} and {@link #end end} methods
 * before and after, respectively, invoking an I/O operation that might block
 * indefinitely.  In order to ensure that the {@link #end end} method is always
 * invoked, these methods should be used within a
 * {@code try}&nbsp;...&nbsp;{@code finally} block:
 *
 * <blockquote><pre id="be">
 * boolean completed = false;
 * try {
 *     begin();
 *     completed = ...;    // Perform blocking I/O operation
 *     return ...;         // Return result
 * } finally {
 *     end(completed);
 * }</pre></blockquote>
 *
 * <p> The {@code completed} argument to the {@link #end end} method tells
 * whether or not the I/O operation actually completed, that is, whether it had
 * any effect that would be visible to the invoker.  In the case of an
 * operation that reads bytes, for example, this argument should be
 * {@code true} if, and only if, some bytes were actually transferred into the
 * invoker's target buffer.
 *
 * <p> A concrete channel class must also implement the {@link
 * #implCloseChannel implCloseChannel} method in such a way that if it is
 * invoked while another thread is blocked in a native I/O operation upon the
 * channel then that operation will immediately return, either by throwing an
 * exception or by returning normally.  If a thread is interrupted or the
 * channel upon which it is blocked is asynchronously closed then the channel's
 * {@link #end end} method will throw the appropriate exception.
 *
 * <p> This class performs the synchronization required to implement the {@link
 * java.nio.channels.Channel} specification.  Implementations of the {@link
 * #implCloseChannel implCloseChannel} method need not synchronize against
 * other threads that might be attempting to close the channel.  </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */
// 可中断通道的抽象实现
public abstract class AbstractInterruptibleChannel implements Channel, InterruptibleChannel {
    
    private final Object closeLock = new Object();  // 执行关闭操作使用的锁
    
    private volatile boolean closed;        // 标记通道是否已关闭
    
    private volatile Thread interrupted;    // 记录被中断的线程
    private Interruptible interruptor;      // 线程中断回调标记
    
    /**
     * Initializes a new instance of this class.
     */
    protected AbstractInterruptibleChannel() {
    }
    
    // 判断通道是否处于开启状态
    public final boolean isOpen() {
        return !closed;
    }
    
    /**
     * Closes this channel.
     *
     * <p> If the channel has already been closed then this method returns
     * immediately.  Otherwise it marks the channel as closed and then invokes
     * the {@link #implCloseChannel implCloseChannel} method in order to
     * complete the close operation.  </p>
     *
     * @throws IOException If an I/O error occurs
     */
    // 关闭当前通道
    public final void close() throws IOException {
        synchronized(closeLock) {
            if(closed) {
                return;
            }
        
            // 将通道标记为关闭状态
            closed = true;
        
            // 关闭"可中断"通道
            implCloseChannel();
        }
    }
    
    /**
     * Closes this channel.
     *
     * <p> This method is invoked by the {@link #close close} method in order
     * to perform the actual work of closing the channel.  This method is only
     * invoked if the channel has not yet been closed, and it is never invoked
     * more than once.
     *
     * <p> An implementation of this method must arrange for any other thread
     * that is blocked in an I/O operation upon this channel to return
     * immediately, either by throwing an exception or by returning normally.
     * </p>
     *
     * @throws IOException If an I/O error occurs while closing the channel
     */
    // 将通道标记为关闭状态后，再调用此方法执行资源清理操作
    protected abstract void implCloseChannel() throws IOException;
    
    /**
     * Marks the beginning of an I/O operation that might block indefinitely.
     *
     * <p> This method should be invoked in tandem with the {@link #end end}
     * method, using a {@code try}&nbsp;...&nbsp;{@code finally} block as
     * shown <a href="#be">above</a>, in order to implement asynchronous
     * closing and interruption for this channel.  </p>
     */
    // 标记可能阻塞的IO操作的开始：需要为阻塞通道所在的线程设置中断回调，该回调在遇到线程中断时会关闭通道
    protected final void begin() {
        // 初始化线程中断回调标记
        if(interruptor == null) {
            interruptor = new Interruptible() {
                // 线程已被中断，关闭通道
                public void interrupt(Thread target) {
                    synchronized(closeLock) {
                        if(closed) {
                            return;
                        }
                    
                        // 标记通道关闭
                        closed = true;
                    
                        // 记下被中断的线程
                        interrupted = target;
                    
                        try {
                            AbstractInterruptibleChannel.this.implCloseChannel();
                        } catch(IOException x) {
                        }
                    }
                }
            };
        }
    
        // 在线程可能进入阻塞前，为当前线程设置一个线程中断回调标记interruptor，以便在线程被中断时调用该标记的回调方法
        blockedOn(interruptor);
    
        // 获取当前通道所在线程
        Thread me = Thread.currentThread();
    
        // 如果当前线程已经带有中断标记，则执行线程中断回调标记中的逻辑
        if(me.isInterrupted()) {
            interruptor.interrupt(me);
        }
    }
    
    /**
     * Marks the end of an I/O operation that might block indefinitely.
     *
     * <p> This method should be invoked in tandem with the {@link #begin
     * begin} method, using a {@code try}&nbsp;...&nbsp;{@code finally} block
     * as shown <a href="#be">above</a>, in order to implement asynchronous
     * closing and interruption for this channel.  </p>
     *
     * @param completed {@code true} if, and only if, the I/O operation completed
     *                  successfully, that is, had some effect that would be visible to
     *                  the operation's invoker
     *
     * @throws AsynchronousCloseException If the channel was asynchronously closed
     * @throws ClosedByInterruptException If the thread blocked in the I/O operation was interrupted
     */
    // 标记可能阻塞的IO操作的结束：需要移除为阻塞通道所在的线程设置的中断回调
    protected final void end(boolean completed) throws AsynchronousCloseException {
        // 清除之前设置的线程中断回调
        blockedOn(null);
        
        // 获取被中断的线程
        Thread interrupted = this.interrupted;
        
        // 如果是由于线程中断而关闭了通道，这里抛出异常
        if(interrupted != null && interrupted == Thread.currentThread()) {
            this.interrupted = null;
            throw new ClosedByInterruptException();
        }
        
        // 如果IO任务还未完成，但通道已关闭，这里抛出异常
        if(!completed && closed) {
            throw new AsynchronousCloseException();
        }
    }
    
    // 为当前线程设置/清除线程中断回调标记
    static void blockedOn(Interruptible interruptor) {
        SharedSecrets.getJavaLangAccess().blockedOn(interruptor);
    }
    
}
