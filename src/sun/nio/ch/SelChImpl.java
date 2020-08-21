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

import java.nio.channels.Channel;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * An interface that allows translation (and more!).
 *
 * @since 1.4
 */
// Select通道的扩展，主要增加了Java层监听事件与本地(native层)监听事件的翻译方法
public interface SelChImpl extends Channel {
    
    // 返回通道在Java层的文件描述符
    FileDescriptor getFD();
    
    // 返回通道在本地(native层)的文件描述符
    int getFDVal();
    
    // 销毁当前通道，即释放对Socket文件描述符的引用
    void kill() throws IOException;
    
    /**
     * Translates an interest operation set into a native event set
     */
    /*
     * 翻译通道注册的监听事件，返回对ops的翻译结果
     *
     * 方向：Java层 --> native层
     * 　　　SelectionKey.XXX --> Net.XXX
     */
    int translateInterestOps(int ops);
    
    /**
     * Adds the specified ops if present in interestOps.
     * The specified ops are turned on without affecting the other ops.
     *
     * @return true if the new value of sk.readyOps() set by this method contains at least one bit that the previous value did not contain
     */
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
    boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl selectionKey);
    
    /**
     * Sets the specified ops if present in interestOps.
     * The specified ops are turned on, and all other ops are turned off.
     *
     * @return true if the new value of sk.readyOps() set by this method contains at least one bit that the previous value did not contain
     */
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
    boolean translateAndSetReadyOps(int ops, SelectionKeyImpl selectionKey);
    
}
