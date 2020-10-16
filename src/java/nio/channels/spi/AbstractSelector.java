/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.nio.ch.Interruptible;

/**
 * Base implementation class for selectors.
 *
 * <p> This class encapsulates the low-level machinery required to implement
 * the interruption of selection operations.  A concrete selector class must
 * invoke the {@link #begin begin} and {@link #end end} methods before and
 * after, respectively, invoking an I/O operation that might block
 * indefinitely.  In order to ensure that the {@link #end end} method is always
 * invoked, these methods should be used within a
 * {@code try}&nbsp;...&nbsp;{@code finally} block:
 *
 * <blockquote><pre id="be">
 * try {
 *     begin();
 *     // Perform blocking I/O operation here
 *     ...
 * } finally {
 *     end();
 * }</pre></blockquote>
 *
 * <p> This class also defines methods for maintaining a selector's
 * cancelled-key set and for removing a key from its channel's key set, and
 * declares the abstract {@link #register register} method that is invoked by a
 * selectable channel's {@link AbstractSelectableChannel#register register}
 * method in order to perform the actual work of registering a channel.  </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */
// 通道选择器的抽象实现
public abstract class AbstractSelector extends Selector {
    
    /** The provider that created this selector */
    // 构造当前选择器对象的选择器工厂
    private final SelectorProvider provider;
    
    // 标记选择器处于开启还是关闭状态
    private final AtomicBoolean selectorOpen = new AtomicBoolean(true);
    
    // 线程中断回调标记，在中断选择器主线程时会被用到
    private Interruptible interruptor = null;
    
    /*
     * "已取消键临时集合"
     *
     * "临时"的含义是每一轮select()的前后，这里的"选择键"都会被移除
     */
    private final Set<SelectionKey> cancelledKeys = new HashSet<SelectionKey>();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a new instance of this class.
     *
     * @param provider The provider that created this selector
     */
    protected AbstractSelector(SelectorProvider provider) {
        this.provider = provider;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注册/反注册 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Registers the given channel with this selector.
     *
     * <p> This method is invoked by a channel's {@link
     * AbstractSelectableChannel#register register} method in order to perform
     * the actual work of registering the channel with this selector.  </p>
     *
     * @param ch  The channel to be registered
     * @param ops The initial interest set, which must be valid
     * @param att The initial attachment for the resulting key
     *
     * @return A new key representing the registration of the given channel
     * with this selector
     */
    /*
     * 通道channel向当前选择器发起注册操作，返回生成的"选择键"
     *
     * 具体的注册行为是：
     * 将通道(channel)、选择器(selector)、监听事件(ops)、附属对象(attachment)这四个属性打包成一个"选择键"对象，
     * 并将该对象分别存储到各个相关的"选择键"集合/队列中，涉及到的"选择键"集合/队列包括：
     *
     * AbstractSelectableChannel -> keys           "选择键"集合
     * SelectorImpl              -> keys           "新注册键集合"
     * WindowsSelectorImpl       -> newKeys        "新注册键临时队列"
     * WindowsSelectorImpl       -> updateKeys     "已更新键临时队列"
     * AbstractSelector          -> cancelledKeys  "已取消键临时集合"
     *
     * 注：需要确保当前通道为非阻塞通道
     */
    protected abstract SelectionKey register(AbstractSelectableChannel channel, int ops, Object att);
    
    
    /**
     * Removes the given key from its channel's key set.
     *
     * This method must be invoked by the selector for each channel that it deregisters.
     *
     * @param key The selection key to be removed
     */
    /*
     * 从key所属通道的"选择键"集合keys中移除key，并将key标记为无效
     *
     * 参见AbstractSelectableChannel中的keys
     */
    protected final void deregister(AbstractSelectionKey key) {
        ((AbstractSelectableChannel) key.channel()).removeKey(key);
    }
    
    /*▲ 注册/反注册 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 打开/关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断选择器是否处于开启状态
    public final boolean isOpen() {
        return selectorOpen.get();
    }
    
    
    /**
     * Closes this selector.
     *
     * <p> If the selector has already been closed then this method returns
     * immediately.  Otherwise it marks the selector as closed and then invokes
     * the {@link #implCloseSelector implCloseSelector} method in order to
     * complete the close operation.  </p>
     *
     * @throws IOException If an I/O error occurs
     */
    // 关闭选择器
    public final void close() throws IOException {
        // 先标记选择器为关闭
        boolean open = selectorOpen.getAndSet(false);
        if(!open) {
            return;
        }
    
        // 完成关闭选择器的后续逻辑，主要是释放资源
        implCloseSelector();
    }
    
    /**
     * Closes this selector.
     *
     * <p> This method is invoked by the {@link #close close} method in order
     * to perform the actual work of closing the selector.  This method is only
     * invoked if the selector has not yet been closed, and it is never invoked
     * more than once.
     *
     * <p> An implementation of this method must arrange for any other thread
     * that is blocked in a selection operation upon this selector to return
     * immediately as if by invoking the {@link
     * java.nio.channels.Selector#wakeup wakeup} method. </p>
     *
     * @throws IOException If an I/O error occurs while closing the selector
     */
    // 完成关闭选择器的后续逻辑，包括关闭通道、管道，结束辅助线程，释放分配的本地内存
    protected abstract void implCloseSelector() throws IOException;
    
    /*▲ 打开/关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取消 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 取消指定的"选择键"，即将其加入到"已取消键临时集合"(cancelledKeys)中
    void cancel(SelectionKey selectionKey) {
        synchronized(cancelledKeys) {
            cancelledKeys.add(selectionKey);
        }
    }
    
    /**
     * Retrieves this selector's cancelled-key set.
     *
     * <p> This set should only be used while synchronized upon it.  </p>
     *
     * @return The cancelled-key set
     */
    // 返回"已取消键临时集合"
    protected final Set<SelectionKey> cancelledKeys() {
        return cancelledKeys;
    }
    
    /*▲ 取消 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中断回调标记 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Marks the beginning of an I/O operation that might block indefinitely.
     *
     * <p> This method should be invoked in tandem with the {@link #end end}
     * method, using a {@code try}&nbsp;...&nbsp;{@code finally} block as
     * shown <a href="#be">above</a>, in order to implement interruption for
     * this selector.
     *
     * <p> Invoking this method arranges for the selector's {@link
     * Selector#wakeup wakeup} method to be invoked if a thread's {@link
     * Thread#interrupt interrupt} method is invoked while the thread is
     * blocked in an I/O operation upon the selector.  </p>
     */
    // 在一段可能阻塞的I/O操作开始之前，设置线程中断回调标记
    protected final void begin() {
        if(interruptor == null) {
            interruptor = new Interruptible() {
                public void interrupt(Thread ignore) {
                    // 通过哨兵元素唤醒阻塞的线程（选择器），并设置interruptTriggered = true
                    AbstractSelector.this.wakeup();
                }
            };
        }
    
        // 为当前线程设置线程中断回调标记
        AbstractInterruptibleChannel.blockedOn(interruptor);
    
        Thread me = Thread.currentThread();
        // 测试线程是否已经中断，线程的中断状态不受影响
        if(me.isInterrupted()) {
            // 如果线程已经中断了，那么立即执行interruptor中的逻辑
            interruptor.interrupt(me);
        }
    }
    
    /**
     * Marks the end of an I/O operation that might block indefinitely.
     *
     * <p> This method should be invoked in tandem with the {@link #begin begin}
     * method, using a {@code try}&nbsp;...&nbsp;{@code finally} block as
     * shown <a href="#be">above</a>, in order to implement interruption for
     * this selector.  </p>
     */
    // 移除之前设置的线程中断回调标记
    protected final void end() {
        AbstractInterruptibleChannel.blockedOn(null);
    }
    
    /*▲ 中断回调标记 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the provider that created this channel.
     *
     * @return The provider that created this channel
     */
    // 返回构造当前选择器的选择器工厂
    public final SelectorProvider provider() {
        return provider;
    }
    
}
