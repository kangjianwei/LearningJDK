/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Native;

/**
 * Manipulates a native array of structs corresponding to (fd, events) pairs.
 *
 * typedef struct pollfd {
 *    SOCKET fd;            // 4 bytes
 *    short events;         // 2 bytes
 * } pollfd_t;
 *
 * @author Konstantin Kladko
 * @author Mike McCloskey
 */
/*
 * 选择器中用到的"待监听键列表"(native层)
 *
 * 本地(native层)注册的待监听的socket的数组，数组元素是对底层结构体pollfd的包装
 *
 * typedef struct {
 *    SOCKET fd;        // 4 bytes，待处理的socket文件描述符
 *    short  events;    // 2 bytes，该socket注册的监听事件，这些事件会交给底层内核侦听
 * } pollfd;
 *
 * 参见：libnio/ch/WindowsSelectorImpl.c文件
 */
class PollArrayWrapper {
    
    // pollfd结构体中变量的偏移量
    @Native
    private static final short FD_OFFSET = 0; // fd offset in pollfd
    @Native
    private static final short EVENT_OFFSET = 4; // events offset in pollfd
    
    // 为结构体pollfd分配的内存大小（考虑到字节对齐，这里分配了8个字节，大于实际需求的6个字节）
    static short SIZE_POLLFD = 8; // sizeof pollfd struct
    
    // 这里用一块本地内存映射一个数组，数组元素的类型是结构体pollfd
    private AllocatedNativeObject pollArray; // The fd array
    
    // 本地内存块pollArray使用的起始地址
    long pollArrayAddress; // pollArrayAddress
    
    // 数组pollArray的容量
    private int size; // Size of the pollArray
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 构造指定容量的"待监听键列表"(native层)
    PollArrayWrapper(int newSize) {
        // 计算pollArray需要分配的底层字节数
        int allocationSize = newSize * SIZE_POLLFD;
        // 分配本地内存（在底层创建一块内存，不是JVM内存）
        pollArray = new AllocatedNativeObject(allocationSize, true);
        // 获取本地内存块pollArray使用的起始地址
        pollArrayAddress = pollArray.address();
        // 记录数组pollArray的容量
        this.size = newSize;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 值操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回pollArray[i]中fd字段的值，即返回"待监听键列表"(native层)中pollArray[i]处存储的本地(native层)文件描述符
    int getDescriptor(int i) {
        return pollArray.getInt(SIZE_POLLFD * i + FD_OFFSET);
    }
    
    // 返回pollArray[i]中events字段的值，即返回"待监听键列表"(native层)中pollArray[i]处存储的监听事件
    int getEventOps(int i) {
        return pollArray.getShort(SIZE_POLLFD * i + EVENT_OFFSET);
    }
    
    // 为pollArray[i]中的fd字段设置指定的值，即向"待监听键列表"(native层)中pollArray[i]处存储本地(native层)文件描述符
    void putDescriptor(int i, int fd) {
        pollArray.putInt(SIZE_POLLFD * i + FD_OFFSET, fd);
    }
    
    // 为pollArray[i]中的events字段设置指定的值，即向"待监听键列表"(native层)中pollArray[i]处存储注册的监听事件
    void putEventOps(int i, int events) {
        pollArray.putShort(SIZE_POLLFD * i + EVENT_OFFSET, (short) events);
    }
    
    
    /* Prepare another pollfd struct for use */
    // 在pollArray[i]处存储一个"选择键"中通道在本地(native层)的文件描述符和一个无效的监听事件（无效事件用来占位）
    void putEntry(int i, SelectionKeyImpl selectionKey) {
        putDescriptor(i, selectionKey.getFDVal());
        putEventOps(i, 0);
    }
    
    /* Writes the pollfd entry from the source wrapper at the source index over the entry in the target wrapper at the target index */
    // 使用source中的pollArray[si]元素替换target中的pollArray[ti]元素
    void replaceEntry(PollArrayWrapper source, int si, PollArrayWrapper target, int ti) {
        target.putDescriptor(ti, source.getDescriptor(si));
        target.putEventOps(ti, source.getEventOps(si));
    }
    
    /* Adds Windows wakeup socket at a given index */
    /*
     * 向"待监听键列表"(native层)的index处添加一个"哨兵"
     *
     * 在"待监听键列表"(native层)中，每1024个待监听元素组成一个批次，然后将每个批次交给底层各自对应的选择器来处理。
     *
     * 每一批待监听元素的首位，都有一个"哨兵"元素(包含在1024个元素中)。
     * 这个"哨兵"元素由一个(管道中的)读通道在本地(native层)的文件描述符和一个Net.POLLIN监听事件构成。
     *
     * 设置该"哨兵"的目的是，当某个选择器线程陷入阻塞，但是我们又想关闭或者中断这个阻塞线程时，
     * 可以通过向"哨兵"元素写入数据来唤醒该阻塞的选择器线程。
     *
     * 通过该"哨兵"可以唤醒选择器的原因就是它监听了Net.POLLIN事件，所以"哨兵"发现有数据可读时，其所在的选择器线程就会醒来。
     *
     * 注：所有"哨兵"共享一个文件描述符，因此可被同时唤醒
     *
     * 参见：WindowsSelectorImpl#MAX_SELECTABLE_FDS
     */
    void addWakeupSocket(int fdVal, int index) {
        putDescriptor(index, fdVal);
        putEventOps(index, Net.POLLIN);
    }
    
    /*▲ 值操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /** Grows the pollfd array to new size */
    // 对"待监听键列表"(native层)扩容
    void grow(int newSize) {
        // 创建一块新内存
        PollArrayWrapper temp = new PollArrayWrapper(newSize);
        
        for(int i = 0; i<size; i++) {
            // 使用当前对象中的pollArray[i]替换temp中的pollArray[ti]
            replaceEntry(this, i, temp, i);
        }
        
        // 释放先前的内存
        pollArray.free();
        
        pollArray = temp.pollArray;
        size = temp.size;
        pollArrayAddress = pollArray.address();
    }
    
    // 释放本地内存
    void free() {
        pollArray.free();
    }
    
}
