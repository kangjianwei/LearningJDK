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

package sun.nio.ch;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

/**
 * An implementation of SelectionKey.
 */
// "选择键"SelectionKey的本地实现
public final class SelectionKeyImpl extends AbstractSelectionKey {
    
    // 发起注册的通道
    private final SelChImpl channel;
    
    // 通道注册到的选择器
    private final SelectorImpl selector;
    
    /** index of key in pollfd array, used by some Selector implementations */
    // 当前"选择键"在"待监听键列表"(Java层)中的索引，参见WindowsSelectorImpl#channelArray
    private int index;
    
    // 通道注册/监听的事件：SelectionKey.XXX
    private volatile int interestOps;
    
    // 属性interestOps的地址
    private static final VarHandle INTERESTOPS = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "interestOps", VarHandle.class, SelectionKeyImpl.class, int.class);
    
    // 已就绪事件，来自本地(native)的反馈
    private volatile int readyOps;
    
    /** registered events in kernel, used by some Selector implementations */
    // 底层内核注册的参数/事件
    private int registeredEvents;
    
    /** used by Selector implementations to record when the key was selected */
    int lastPolled;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    SelectionKeyImpl(SelChImpl ch, SelectorImpl sel) {
        channel = ch;
        selector = sel;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回发起注册的通道
    @Override
    public SelectableChannel channel() {
        return (SelectableChannel) channel;
    }
    
    // 返回通道注册到的选择器
    @Override
    public Selector selector() {
        return selector;
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 监听事件/参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回通道注册的监听事件：SelectionKey.XXX(不会验证当前"选择键"是否有效)
    public int nioInterestOps() {
        return interestOps;
    }
    
    // 返回通道注册的监听事件：SelectionKey.XXX(需要验证当前"选择键"是否有效)
    @Override
    public int interestOps() {
        ensureValid();
        return interestOps;
    }
    
    /*
     * 翻译通道注册的监听事件，返回对interestOps的翻译结果
     *
     * 方向：Java层 --> native层
     * 　　　SelectionKey.XXX --> Net.XXX
     */
    int translateInterestOps() {
        return channel.translateInterestOps(interestOps);
    }
    
    
    /*
     *【覆盖更新】当前"选择键"内的监听事件，并且将当前"选择键"加入到选择器的"已更新键临时队列"中。
     * 事件注册完成后，返回当前"选择键"。
     *
     * 参见：WindowsSelectorImpl#updateKeys()
     */
    public SelectionKey nioInterestOps(int ops) {
        
        // 校验参数是否合规（不同的通道允许使用的参数类型不同）
        if((ops & ~channel().validOps()) != 0) {
            throw new IllegalArgumentException();
        }
        
        //【覆盖更新】当前选择键内的监听事件
        interestOps = ops;
        
        // 将当前"选择键"加入到选择器的"已更新键临时队列"中
        selector.setEventOps(this);
        
        return this;
    }
    
    /*
     *【覆盖更新】当前"选择键"内的监听事件，如果新旧事件不同，则将当前"选择键"加入到选择器的"已更新键临时队列"中。
     * 事件注册完成后，返回当前"选择键"。
     *
     * 参见：WindowsSelectorImpl#updateKeys()
     */
    @Override
    public SelectionKey interestOps(int ops) {
        // 确保当前选择键有效
        ensureValid();
        
        // 校验待监听事件是否合规
        if((ops & ~channel().validOps()) != 0) {
            throw new IllegalArgumentException();
        }
        
        //【覆盖更新】当前选择键内的监听事件，并返回旧的注册信息
        int oldOps = (int) INTERESTOPS.getAndSet(this, ops);
        
        // 如果新旧事件/参数不同，则将当前选择键存入"已更新键队列"
        if(ops != oldOps) {
            // 将当前"选择键"加入到选择器的"已更新键临时队列"中
            selector.setEventOps(this);
        }
        
        return this;
    }
    
    /*
     *【增量更新】当前"选择键"内的监听事件，如果新旧事件不同，则将当前"选择键"加入到选择器的"已更新键临时队列"中。
     * 事件注册完成后，返回旧的监听事件。
     *
     * 参见：WindowsSelectorImpl#updateKeys()
     */
    @Override
    public int interestOpsOr(int ops) {
        // 确保当前选择键有效
        ensureValid();
        
        // 校验待监听事件是否合规
        if((ops & ~channel().validOps()) != 0) {
            throw new IllegalArgumentException();
        }
        
        //【增量更新】当前选择键内的监听事件，并返回旧的注册信息
        int oldVal = (int) INTERESTOPS.getAndBitwiseOr(this, ops);
        
        // 如果新旧事件/参数不同，则将当前选择键存入"已更新键队列"
        if(oldVal != (oldVal | ops)) {
            // 将当前"选择键"加入到选择器的"已更新键临时队列"中
            selector.setEventOps(this);
        }
        
        return oldVal;
    }
    
    /*
     *【交集更新】当前"选择键"内的监听事件，如果新旧事件不同，则将当前"选择键"加入到选择器的"已更新键临时队列"中。
     * 事件注册完成后，返回旧的监听事件。
     *
     * 参见：WindowsSelectorImpl#updateKeys()
     */
    @Override
    public int interestOpsAnd(int ops) {
        // 确保当前选择键有效
        ensureValid();
        
        //【交集更新】当前选择键内的监听事件，并返回旧的注册信息
        int oldVal = (int) INTERESTOPS.getAndBitwiseAnd(this, ops);
        
        // 如果新旧事件/参数不同，则将当前选择键存入"已更新键队列"
        if(oldVal != (oldVal & ops)) {
            // 将当前"选择键"加入到选择器的"已更新键临时队列"中
            selector.setEventOps(this);
        }
        
        return oldVal;
    }
    
    /*▲ 监听事件/参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 就绪参数/事件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将ops设置为已就绪事件
    public void nioReadyOps(int ops) {
        readyOps = ops;
    }
    
    // 返回已就绪事件
    public int nioReadyOps() {
        return readyOps;
    }
    
    // 返回已就绪事件(会验证当前"选择键"是否有效)
    @Override
    public int readyOps() {
        ensureValid();
        return readyOps;
    }
    
    
    /*
     *【增量更新】已就绪事件
     *
     * 将本地(native)反馈的就绪信号ops翻译并存储到Java层的就绪事件readyOps中，
     * 返回值指示上一次反馈的事件与本次反馈的事件是否发生了改变。
     *
     * 通道收到有效的反馈事件后，会【增量更新】上次记录的已就绪事件，
     * 如果本地(native)反馈了错误或挂起信号，则将已就绪事件直接设置为通道注册的监听事件。
     *
     * 方向：native层 --> Java层
     * 　　　Net.XXX --> SelectionKey.XXX
     *
     * 参见：SelectionKeyImpl#readyOps
     */
    boolean translateAndUpdateReadyOps(int ops) {
        return channel.translateAndUpdateReadyOps(ops, this);
    }
    
    /*
     *【覆盖更新】已就绪事件
     *
     * 将本地(native)反馈的就绪信号ops翻译并存储到Java层的就绪事件readyOps中，
     * 返回值指示上一次反馈的事件与本次反馈的事件是否发生了改变。
     *
     * 通道收到有效的反馈事件后，会【覆盖】(selectionKey中)上次记录的已就绪事件，
     * 如果本地(native)反馈了错误或挂起信号，则将已就绪事件直接设置为通道注册的监听事件。
     *
     * 方向：native层 --> Java层
     * 　　　Net.XXX --> SelectionKey.XXX
     *
     * 参见：SelectionKeyImpl#readyOps
     */
    boolean translateAndSetReadyOps(int ops) {
        return channel.translateAndSetReadyOps(ops, this);
    }
    
    /*▲ 就绪参数/事件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回通道在本地(native层)的文件描述符
    int getFDVal() {
        return channel.getFDVal();
    }
    
    // 获取当前"选择键"在"待监听键列表"(Java层)中的索引
    int getIndex() {
        return index;
    }
    
    // 记录当前"选择键"在"待监听键列表"(Java层)中的索引
    void setIndex(int index) {
        this.index = index;
    }
    
    // 获取底层内核注册的参数/事件
    int registeredEvents() {
        // assert Thread.holdsLock(selector);
        return registeredEvents;
    }
    
    // 设置底层内核注册的参数/事件
    void registeredEvents(int events) {
        // assert Thread.holdsLock(selector);
        this.registeredEvents = events;
    }
    
    // 确保当前SelectionKey对象有效，否则抛异常
    private void ensureValid() {
        if(!isValid()) {
            throw new CancelledKeyException();
        }
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("channel=").append(channel).append(", selector=").append(selector);
        
        if(isValid()) {
            sb.append(", interestOps=").append(interestOps).append(", readyOps=").append(readyOps);
        } else {
            sb.append(", invalid");
        }
        
        return sb.toString();
    }
    
}
