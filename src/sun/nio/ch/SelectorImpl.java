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

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Base Selector implementation class.
 */
// 通道选择器的基础实现
abstract class SelectorImpl extends AbstractSelector {
    
    /** The set of keys registered with this Selector */
    /*
     * "新注册键集合"，存储所有隶属于当前选择器的"选择键"。
     *
     * 每当有通道向当前选择器发起注册操作时，所有相关参数，包括通道(channel)、选择器(selector)、监听事件(ops)、附属对象(attachment)，
     * 都会被打包成一个"选择键"存储到该"选择键"集合中。
     */
    private final Set<SelectionKey> keys;
    
    /** Immutable */
    // "新注册键集合"的视图，与keys共享元素，但只读
    private final Set<SelectionKey> publicKeys;
    
    
    /** The set of keys with data ready for an operation */
    /*
     * "已就绪键集合"
     *
     * 注：这里存储的"已就绪键"都是可用的。
     * 　　可用的含义是发生在该"选择键"内的文件描述符上的变动事件与Java层向其注册监听的事件是匹配的。
     * ★★★当使用了带有action参数的select()方法时，是无法使用"已就绪键集合"的。
     *
     * 在某些情形下，处理完某个就绪的SelectionKey之后，需要将其从selectedKeys中移除
     */
    private final Set<SelectionKey> selectedKeys;
    
    /** Removal allowed, but not addition */
    // "已就绪键集合"的视图，与selectedKeys共享元素，允许删除，但不允许增加
    private final Set<SelectionKey> publicSelectedKeys;
    
    
    /** used to check for reentrancy */
    // 标记选择器是否正处于选择就绪通道的过程中
    private boolean inSelect;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    protected SelectorImpl(SelectorProvider sp) {
        super(sp);
        
        keys = ConcurrentHashMap.newKeySet();
        publicKeys = Collections.unmodifiableSet(keys);
        
        selectedKeys = new HashSet<>();
        publicSelectedKeys = Util.ungrowableSet(selectedKeys);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注册/反注册 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    @Override
    protected final SelectionKey register(AbstractSelectableChannel channel, int ops, Object attachment) {
        if(!(channel instanceof SelChImpl)) {
            throw new IllegalSelectorException();
        }
        
        // 构造"选择键"对象，并向其添加通道(channel)、选择器(selector)属性
        SelectionKeyImpl selectionKey = new SelectionKeyImpl((SelChImpl) channel, this);
        
        // 继续向"选择键"对象加入附属对象(attachment)属性
        selectionKey.attach(attachment);
        
        /*
         * 平台相关的一部分注册逻辑：主要是将指定的"选择键"注册(存储)到当前选择器内。
         *
         * windows上的实现方式为将该"选择键"添加到当前选择器的"新注册临时键队列"(newKeys)中。
         *
         * 参见：WindowsSelectorImpl#newKeys
         */
        implRegister(selectionKey);
        
        /*
         * add to the selector's key set, removing it immediately if the selector is closed.
         * The key is not in the channel's key set at this point
         * but it may be observed by a thread iterating over the selector's key set.
         */
        // 将装配好的"选择键"添加到当前选择器的"新注册键集合"(keys)中
        keys.add(selectionKey);
        
        try {
            /*
             *【覆盖更新】当前选择键内的监听事件，如果新旧事件不同，则将当前"选择键"加入到选择器的"已更新键临时队列"(updateKeys)中。
             * 事件注册完成后，返回当前"选择键"。
             *
             * 参见：WindowsSelectorImpl#updateKeys
             */
            selectionKey.interestOps(ops);
        } catch(ClosedSelectorException e) {
            assert channel.keyFor(this) == null;
            
            // 如果出现异常，将装配好的"选择键"从当前选择器的"新注册键集合"(keys)中移除
            keys.remove(selectionKey);
            
            // 如果出现异常，则取消该"注册键"：将其标记为无效，并将其添加到当前选择器的"已取消键"集合(cancelledKeys)中
            selectionKey.cancel();
            
            throw e;
        }
        
        return selectionKey;
    }
    
    /**
     * Register the key in the selector.
     *
     * The default implementation checks if the selector is open.
     * It should be overridden by selector implementations as needed.
     */
    /*
     * 平台相关的一部分注册逻辑：主要是将指定的"选择键"注册(存储)到当前选择器内。
     *
     * windows上的实现方式为将该"选择键"添加到当前选择器的"新注册临时键队列"(newKeys)中。
     *
     * 参见：WindowsSelectorImpl#newKeys
     */
    protected void implRegister(SelectionKeyImpl selectionKey) {
        ensureOpen();
    }
    
    
    /**
     * Removes the key from the selector
     */
    /*
     * 平台相关的一部分反注册逻辑：主要是将指定的"选择键"从当前选择器内移除。
     *
     * windows上的实现是将指定的"选择键"从"待监听键列表"中移除
     *
     * 参见：WindowsSelectorImpl中的channelArray和pollWrapper
     */
    protected abstract void implDereg(SelectionKeyImpl selectionKey) throws IOException;
    
    /*▲ 注册/反注册 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 选择就绪通道 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 选择可用的已就绪通道，返回本轮select()中找到的所有【可用的】"已就绪键"(已就绪通道)的数量
     *
     * 注：
     * 1.会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)
     * 2.本地(native层)没有相关的变动事件时，一直阻塞(参见SubSelector#poll())
     */
    @Override
    public final int select() throws IOException {
        // 对doSelect()方法加锁，以选择出可用的已就绪通道
        return lockAndDoSelect(null, -1);
    }
    
    /*
     * 选择可用的已就绪通道，返回本轮select()中找到的所有【可用的】"已就绪键"(已就绪通道)的数量
     *
     * 注：
     * 1.会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)
     * 2.本地(native层)没有相关的变动事件时，立即返回(参见SubSelector#poll())
     */
    @Override
    public final int selectNow() throws IOException {
        // 对doSelect()方法加锁，以选择出可用的已就绪通道
        return lockAndDoSelect(null, 0);
    }
    
    /*
     * 选择可用的已就绪通道，返回本轮select()中找到的所有【可用的】"已就绪键"(已就绪通道)的数量
     *
     * 注：会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)
     *
     * timeout: 监听等待中的超时设置(参见SubSelector#poll())：
     *          timeout<=0表示一直阻塞，直到本地被新来的事件唤醒选择器线程，然后传导到Java层；
     *          timeout为其他值表示阻塞timeout毫秒。
     */
    @Override
    public final int select(long timeout) throws IOException {
        if(timeout<0) {
            throw new IllegalArgumentException("Negative timeout");
        }
        
        // 对doSelect()方法加锁，以选择出可用的已就绪通道
        return lockAndDoSelect(null, (timeout == 0) ? -1 : timeout);
    }
    
    /*
     * 选择可用的已就绪通道，返回本轮select()中找到的所有【可用的】"已就绪键"(已就绪通道)的数量
     *
     * action : 如果为null，则会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)；
     *          如果不为null，则用来处理可用的"已就绪键"，即【不会】将其存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)。
     *          这就意味着使用selectedKeys()时就无法获取到"已就绪键"了。
     *
     * 注：本地(native层)没有相关的变动事件时，一直阻塞(参见SubSelector#poll())
     */
    @Override
    public final int select(Consumer<SelectionKey> action) throws IOException {
        Objects.requireNonNull(action);
        
        // 对doSelect()方法加锁，以选择出可用的已就绪通道
        return lockAndDoSelect(action, -1);
    }
    
    /*
     * 选择可用的已就绪通道，返回本轮select()中找到的所有【可用的】"已就绪键"(已就绪通道)的数量
     *
     * action : 如果为null，则会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)；
     *          如果不为null，则用来处理可用的"已就绪键"，即【不会】将其存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)。
     *          这就意味着使用selectedKeys()时就无法获取到"已就绪键"了。
     *
     * 注：本地(native层)没有相关的变动事件时，立即返回(参见SubSelector#poll())
     */
    @Override
    public final int selectNow(Consumer<SelectionKey> action) throws IOException {
        Objects.requireNonNull(action);
        
        // 对doSelect()方法加锁，以选择出可用的已就绪通道
        return lockAndDoSelect(action, 0);
    }
    
    /*
     * 选择可用的已就绪通道，返回本轮select()中找到的所有【可用的】"已就绪键"(已就绪通道)的数量
     *
     * action : 如果为null，则会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)；
     *          如果不为null，则用来处理可用的"已就绪键"，即【不会】将其存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)。
     *          这就意味着使用selectedKeys()时就无法获取到"已就绪键"了。
     *
     * timeout: 监听等待中的超时设置(参见SubSelector#poll())：
     *          timeout<=0表示一直阻塞，直到本地被新来的事件唤醒选择器线程，然后传导到Java层；
     *          timeout为其他值表示阻塞timeout毫秒。
     */
    @Override
    public final int select(Consumer<SelectionKey> action, long timeout) throws IOException {
        Objects.requireNonNull(action);
        
        if(timeout<0) {
            throw new IllegalArgumentException("Negative timeout");
        }
        
        // 对doSelect()方法加锁，以选择出可用的已就绪通道
        return lockAndDoSelect(action, (timeout == 0) ? -1 : timeout);
    }
    
    // 对doSelect()方法加锁，以选择出可用的已就绪通道
    private int lockAndDoSelect(Consumer<SelectionKey> action, long timeout) throws IOException {
        synchronized(this) {
            // 确保选择器处于开启状态（否则抛异常）
            ensureOpen();
            
            // 如果选择器已经正在选择就绪通道，则抛出异常
            if(inSelect) {
                throw new IllegalStateException("select in progress");
            }
            
            // 标记选择器进入选择就绪通道的过程
            inSelect = true;
            
            try {
                synchronized(publicSelectedKeys) {
                    // 选择可用的已就绪通道，返回本轮select()中找到的所有【可用的】"已就绪键"(已就绪通道)的数量
                    return doSelect(action, timeout);
                }
            } finally {
                // 清除标记
                inSelect = false;
            }
        }
    }
    
    /**
     * Selects the keys for channels that are ready for I/O operations.
     *
     * @param action  the action to perform, can be null
     * @param timeout timeout in milliseconds to wait, 0 to not wait, -1 to wait indefinitely
     */
    /*
     * 选择可用的已就绪通道，返回本轮select()中找到的所有【可用的】"已就绪键"(已就绪通道)的数量
     *
     * 主要有三个步骤：
     *  1.搜集Java层注册的通道的文件描述符和注册监听的事件；
     *  2.把这些被监听的文件描述符交给内核，由内核监听它们的变动事件；
     *  3.内核收到被监听文件描述符的变动事件后，会向上交给Java层的选择器。
     *
     * action : 如果为null，则会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)；
     *          如果不为null，则用来处理可用的"已就绪键"。
     * timeout: 监听等待中的超时设置(参见SubSelector#poll())：
     *          timeout=0表示可以立即返回；
     *          timeout=-1表示一直阻塞，直到本地被新来的事件唤醒选择器线程，然后传导到Java层；
     *          timeout为其他值表示阻塞timeout毫秒。
     */
    protected abstract int doSelect(Consumer<SelectionKey> action, long timeout) throws IOException;
    
    /*▲ 选择就绪通道 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 打开/关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 确保选择器处于开启状态(否则抛异常)
    private void ensureOpen() {
        if(!isOpen()) {
            throw new ClosedSelectorException();
        }
    }
    
    // 完成关闭选择器的后续逻辑，包括关闭通道、管道，结束辅助线程，释放分配的本地内存
    @Override
    public final void implCloseSelector() throws IOException {
        // 通过"哨兵"元素唤醒所有阻塞的辅助线程，并设置interruptTriggered = true，后续这些辅助线程将会结束运行
        wakeup();
        
        synchronized(this) {
            // 平台相关的一部分关闭选择器的逻辑，主要是释放本地内存，且设置interruptTriggered = true
            implClose();
            
            synchronized(publicSelectedKeys) {
                // 遍历当前选择器上的"新注册键集合"
                Iterator<SelectionKey> iterator = keys.iterator();
                while(iterator.hasNext()) {
                    // 获取到之前注册的"选择键"selectionKey
                    SelectionKeyImpl selectionKey = (SelectionKeyImpl) iterator.next();
                    
                    /*
                     * 从selectionKey所属通道的"选择键"集合keys中移除selectionKey，并将selectionKey标记为无效
                     *
                     * 参见AbstractSelectableChannel中的keys
                     */
                    deregister(selectionKey);
                    
                    // 销毁selectionKey所属的通道
                    SelectableChannel channel = selectionKey.channel();
                    if(!channel.isOpen() && !channel.isRegistered()) {
                        ((SelChImpl) channel).kill();
                    }
                    
                    // 同时从"已就绪键集合"中移除selectionKey
                    selectedKeys.remove(selectionKey);
                    
                    // 最后从"新注册键集合"keys中移除
                    iterator.remove();
                }
                
                assert selectedKeys.isEmpty() && keys.isEmpty();
            }
        }
    }
    
    /**
     * Invoked by implCloseSelector to close the selector.
     */
    // 平台相关的一部分关闭选择器的逻辑，主要是释放本地内存，且设置interruptTriggered = true
    protected abstract void implClose() throws IOException;
    
    /*▲ 打开/关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回"新注册键集合"的视图，与keys共享元素，但只读
    @Override
    public final Set<SelectionKey> keys() {
        ensureOpen();
        return publicKeys;
    }
    
    // 返回"已就绪键集合"的视图，与selectedKeys共享元素，允许删除，但不允许增加
    @Override
    public final Set<SelectionKey> selectedKeys() {
        ensureOpen();
        return publicSelectedKeys;
    }
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中断回调标记 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Marks the beginning of a select operation that might block
     */
    // 在一段可能阻塞的I/O操作开始之前，设置线程中断回调标记
    protected final void begin(boolean blocking) {
        if(blocking) {
            begin();
        }
    }
    
    /**
     * Marks the end of a select operation that may have blocked
     */
    // 移除之前设置的线程中断回调标记
    protected final void end(boolean blocking) {
        if(blocking) {
            end();
        }
    }
    
    /*▲ 中断回调标记 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Invoked by interestOps to ensure the interest ops are updated at the next selection operation.
     */
    // 将指定的"选择键"加入到当前选择器的"已更新键临时队列"中(之前可能已经添加过了)
    protected abstract void setEventOps(SelectionKeyImpl selectionKey);
    
    
    /**
     * Invoked by selection operations to process the cancelled-key set
     */
    /*
     * 处理"已取消键临时集合"
     *
     * 遍历已经标记为无效的"选择键"，将其从以下容器中移除：
     *
     * AbstractSelectableChannel -> keys         "选择键"集合
     * SelectorImpl              -> keys         "新注册键集合"
     * SelectorImpl              -> selectedKeys "已就绪键集合"
     * WindowsSelectorImpl       -> fdMap        "待监听键"目录
     * WindowsSelectorImpl       -> channelArray "待监听键列表"(Java层)
     * WindowsSelectorImpl       -> pollWrapper  "待监听键列表"(native层)
     *
     * 注：需要确保当前通道为非阻塞通道
     */
    protected final void processDeregisterQueue() throws IOException {
        assert Thread.holdsLock(this);
        assert Thread.holdsLock(publicSelectedKeys);
        
        // 返回"已取消键临时集合"
        Set<SelectionKey> set = cancelledKeys();
        
        synchronized(set) {
            // 如果集合为空，直接返回
            if(set.isEmpty()) {
                return;
            }
            
            // 遍历所有"已取消键"
            Iterator<SelectionKey> iterator = set.iterator();
            while(iterator.hasNext()) {
                // 一边处理一边移除，这个移除只能由迭代器来完成
                SelectionKeyImpl selectionKey = (SelectionKeyImpl) iterator.next();
                iterator.remove();
                
                /*
                 * 平台相关的一部分反注册逻辑：主要是将指定的"选择键"从当前选择器内移除。
                 *
                 * windows上的实现是将指定的"选择键"从"待监听键列表"中移除
                 *
                 * 参见：WindowsSelectorImpl中的channelArray和pollWrapper
                 */
                implDereg(selectionKey);
                
                // 从"已就绪键集合"中移除已取消的"选择键"
                selectedKeys.remove(selectionKey);
                
                // 从"新注册键集合"中移除已取消的"选择键"
                keys.remove(selectionKey);
                
                /*
                 * 从selectionKey所属通道的"选择键"集合keys中移除selectionKey，并将selectionKey标记为无效
                 *
                 * 参见AbstractSelectableChannel中的keys
                 */
                deregister(selectionKey);
                
                // 获取发起注册的通道
                SelectableChannel channel = selectionKey.channel();
                
                // 如果channel已经关闭，或者channel上的"选择键"集合已经为空，则销毁channel
                if(!channel.isOpen() && !channel.isRegistered()) {
                    ((SelChImpl) channel).kill();
                }
            }
        }
    }
    
    /**
     * Invoked by selection operations to handle ready events.
     * If an action is specified then it is invoked to handle the key,
     * otherwise the key is added to the selected-key set (or updated when it is already in the set).
     */
    /*
     * 处理已就绪的通道(选择键)，即处理本地(native层)监听到变动事件的文件描述符，返回值指示该"选择键"是否可用(0-不可用，1-可用)
     *
     * 具体处理方式为：
     * 1.如果action不为null：
     * 1.1. 使用rOps来【覆盖更新】selectionKey中的就绪事件
     * 1.1.1. 如果rOps和selectionKey匹配，则使用action处理selectionKey，并返回1，表示该"选择键"可用
     * 1.1.1. 如果rOps和selectionKey不匹配，则返回0，表示selectionKey不可用。
     * 2.如果action为null：
     * 2.1. 如果selectionKey不在"已就绪键集合"中
     * 2.1.1. 使用rOps来【覆盖更新】selectionKey中的就绪事件
     * 2.1.1.1. 如果rOps和selectionKey匹配，则将selectedKey添加到"已就绪键集合"，并返回1，表示该"选择键"可用
     * 2.1.1.1. 如果rOps和selectionKey不匹配，则返回0，表示selectionKey不可用。
     * 2.1. 如果selectionKey已经在"已就绪键集合"中
     * 2.1.1.【增量更新】selectionKey的已就绪事件
     *
     * 关于rOps和selectionKey，有两种匹配情形：
     * 1.本地(native)反馈的就绪信号rOps与已就绪的"选择键"selectionKey注册的监听事件不匹配，则返回0，表示该"选择键"不可用。
     * 2.本地(native)反馈的就绪信号rOps与已就绪的"选择键"selectionKey注册的监听事件匹配，则返回1，表示该"选择键"可用
     *
     * rOps        : 本地(native)反馈的就绪信号
     * selectionKey: 已就绪的"选择键"
     * action      : 如何处理已就绪的"选择键"
     */
    protected final int processReadyEvents(int rOps, SelectionKeyImpl selectionKey, Consumer<SelectionKey> action) {
        if(action != null) {
            // 【覆盖更新】已就绪事件
            selectionKey.translateAndSetReadyOps(rOps);
            
            // 如果"选择键"注册的监听事件与本地(native)反馈的就绪信号匹配
            if((selectionKey.nioReadyOps() & selectionKey.nioInterestOps()) != 0) {
                // 处理可用的"已就绪键"
                action.accept(selectionKey);
                
                // 确保选择器处于开启状态（否则抛异常）
                ensureOpen();
                
                return 1;
            }
            
            return 0;
        }
        
        assert Thread.holdsLock(publicSelectedKeys);
        
        // 如果selectionKey不在"已就绪键集合"中
        if(!selectedKeys.contains(selectionKey)) {
            //【覆盖更新】已就绪事件
            selectionKey.translateAndSetReadyOps(rOps);
            
            // 如果"选择键"注册的监听事件与本地(native)反馈的就绪信号匹配
            if((selectionKey.nioReadyOps() & selectionKey.nioInterestOps()) != 0) {
                // 将selectedKey添加到"已就绪键集合"
                selectedKeys.add(selectionKey);
                return 1;
            }
            
            // 如果selectionKey已经在"已就绪键集合"中
        } else {
            // 【增量更新】已就绪事件
            if(selectionKey.translateAndUpdateReadyOps(rOps)) {
                return 1;
            }
        }
        
        return 0;
    }
    
}
