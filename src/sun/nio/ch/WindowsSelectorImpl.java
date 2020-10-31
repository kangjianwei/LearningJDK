/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * A multi-threaded implementation of Selector for Windows.
 *
 * @author Konstantin Kladko
 * @author Mark Reinhold
 */
// 与平台相关的通道选择器实现，此类对应于Windows平台
class WindowsSelectorImpl extends SelectorImpl {
    
    /** Maximum number of sockets for select(). Should be INIT_CAP times a power of 2 */
    /*
     * 每批(1024个)待监听通道的文件描述符的上限
     *
     * 在windows平台对NIO的实现中，底层调用了select()函数。
     *
     * select()函数所在的选择器默认允许同时打开的文件描述符数量上限为1024，
     * 但是如果待监听的通道(文件描述符)数量超过1024个怎么办？
     *
     * Java本地(native层)的解决方案是一个选择器不够就多弄几个选择器。
     * 在native层中，会同时创建多个选择器线程，且各个线程中都持有各自的选择器，这样就可以并行处理更多的文件描述符了。
     *
     * 然后在Java层呢，会开启多个守护线程，每凑够一批(1024个)通道(文件描述符)，就将其划归给某个子线程管理，
     * 而这个线程会与native层的选择器子线程进行交互，其实就是将各批文件描述符注册到native层对应子线程中的选择器上去监听。
     *
     * 参见：SelectThread
     */
    private static final int MAX_SELECTABLE_FDS = 1024;
    
    /** Initial capacity of the poll array */
    // "待监听键列表"(native层)的初始容量
    private final int INIT_CAP = 8;
    
    
    /*▼ 锁 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /** pending new registrations/updates, queued by implRegister and setEventOps */
    // 用在newKeys和updateKeys上的锁
    private final Object updateLock = new Object();
    
    /** Helper threads wait on this lock for the next poll */
    // 辅助线程使用的锁，控制辅助线程的阻塞与唤醒（辅助线程阻塞的原因是等待被startThreads()唤醒）
    private final StartLock startLock = new StartLock();
    
    /** Main thread waits on this lock, until all helper threads are done with poll() */
    // 主线程使用的锁，控制主线程的阻塞与唤醒（主线程阻塞的原因是等待辅助线程执行完成）
    private final FinishLock finishLock = new FinishLock();
    
    /** Lock for interrupt triggering and clearing */
    // 用在interruptTriggered上的锁
    private final Object interruptLock = new Object();
    
    /*▲ 锁 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    /*▼ 管道 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /** Pipe used as a wakeup object */
    // 管道对象
    private final Pipe wakeupPipe;
    
    /** File descriptors corresponding to source and sink */
    // 管道中的读通道在本地(native层)的文件描述符，用在"哨兵"元素中
    private final int wakeupSourceFd;
    
    // 管道中的写通道在本地(native层)的文件描述符
    private final int wakeupSinkFd;
    
    /*▲ 管道 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    /*▼ "选择键"队列 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /*
     * "新注册键临时队列"，用来存储新注册进来的"选择键"，这些选择键中的监听事件/参数可能会发生变化(如重复注册)。
     * "选择键"的新与旧是用通道(channel)和选择器(selector)两个属性来衡量的。
     *
     * "临时"的含义是在发起新一轮的select()后，这里的"选择键"会被移动到"待监听键列表"中。
     *
     * 注：该队列内的"选择键"不一定一直有效
     */
    private final Deque<SelectionKeyImpl> newKeys = new ArrayDeque<>();
    
    /*
     * "已更新键临时队列"，同样用来存储新注册进来的"选择键"，
     * 如果某个"选择键"的监听事件/参数在重复注册中被改变了，那么该"选择键"也会重复进入当前队列。
     *
     * "临时"的含义是在发起新一轮的select()后，这里的"选择键"会被移动到"待监听键列表"中。
     *
     * 注：该队列内的"选择键"不一定一直有效
     */
    private final Deque<SelectionKeyImpl> updateKeys = new ArrayDeque<>();
    
    /*▲ "选择键"队列 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    /*▼ "待监听键"  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    // "待监听键"目录，存储的是"待监听键"及其文件描述符
    private final FdMap fdMap = new FdMap();
    
    
    /**
     * The list of SelectableChannels serviced by this Selector.
     * Every mod MAX_SELECTABLE_FDS entry is bogus, to align this array with the poll array,
     * where the corresponding entry is occupied by the wakeupSocket
     */
    /*
     * "待监听键列表"(Java层)
     *
     * 此列表中的元素与pollWrapper中的元素是对应的，但是channelArray中的元素未设"哨兵"。
     */
    private SelectionKeyImpl[] channelArray = new SelectionKeyImpl[INIT_CAP];
    
    /** The global native poll array holds file decriptors and event masks */
    /*
     * "待监听键列表"(native层)
     *
     * 该列表中的"待监听键"会被分批次处理，每1024个为一批，交给一个线程去处理。
     * 而且，每批"待监听键"之首，都是一个"哨兵"元素。
     */
    private PollArrayWrapper pollWrapper;
    
    /** The number of valid entries in  poll array, including entries occupied by wakeup socket handle */
    // "待监听键列表"中存储的元素个数，此计数包含了"哨兵"元素在内
    private int totalChannels = 1;
    
    /*▲ "待监听键" ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    /** SubSelector for the main thread */
    /*
     * 主线程中的子选择器处理的是第一批"待监听键"，
     * 这批"待监听键"在"待监听键列表"(native层)中的起始索引是0，
     * 因此，使用无参构造器就可以。
     */
    private final SubSelector subSelector = new SubSelector();
    
    /** A list of helper threads for select */
    // 辅助线程列表
    private final List<SelectThread> threads = new ArrayList<SelectThread>();
    
    /** Number of helper threads needed for select. We need one thread per each additional set of MAX_SELECTABLE_FDS - 1 channels */
    // 统计辅助线程数量
    private int threadsCount = 0;
    
    /*
     * 中断标记，用于指示选择器当前是否可以唤醒辅助线程。
     *
     * 当关闭选择器，或对辅助线程进行了唤醒操作之后，会设置interruptTriggered为true，意思是目前不能再次唤醒辅助线程了；
     * 每次select()之后，都会将interruptTriggered恢复为false，表示可以再次适时唤醒辅助线程了。
     */
    private volatile boolean interruptTriggered;
    
    /** timeout for poll */
    // 选择器转入监听内核事件后用到的超时设置
    private long timeout;
    
    /**
     * We increment this counter on each call to updateSelectedKeys() each entry in SubSelector.fdsMap has a memorized value of updateCount.
     * When we increment numKeysUpdated we set updateCount for the corresponding entry to its current value.
     * This is used to avoid counting the same key more than once - the same key can appear in readfds and writefds.
     */
    // 记录当前是第几轮select操作(每轮select操作包含多个阶段)
    private long updateCount = 0;
    
    
    static {
        IOUtil.load();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    WindowsSelectorImpl(SelectorProvider sp) throws IOException {
        super(sp);
        
        // 构造一个Pipe对象，并打通内部管道的连接
        wakeupPipe = Pipe.open();
        
        // 获取管道中的读通道在本地(native层)的文件描述符
        wakeupSourceFd = ((SelChImpl) wakeupPipe.source()).getFDVal();
        
        /* Disable the Nagle algorithm so that the wakeup is more immediate */
        // 获取管道中的写通道，可以向这里写入数据
        SinkChannelImpl sink = (SinkChannelImpl) wakeupPipe.sink();
        
        // 对写通道上关联的socket禁用Nagle算法（即需要实时传输）
        (sink.socketChannel).socket().setTcpNoDelay(true);
        
        // 获取管道中的写通道在本地(native层)的文件描述符
        wakeupSinkFd = ((SelChImpl) sink).getFDVal();
        
        // 构造指定容量的"待监听键列表"(native层)
        pollWrapper = new PollArrayWrapper(INIT_CAP);
        
        /*
         * 向"待监听键列表"(native层)的0号索引处添加一个"哨兵"。
         * 与之呼应的是，totalChannels字段初始化为1，因为"哨兵"也是一个"待监听键"
         */
        pollWrapper.addWakeupSocket(wakeupSourceFd, 0);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注册/反注册 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 平台相关的一部分注册逻辑：主要是将指定的"选择键"注册(存储)到当前选择器内。
     *
     * windows上的实现方式为将该"选择键"添加到当前选择器的"新注册临时键队列"(newKeys)中。
     *
     * 参见：WindowsSelectorImpl#newKeys
     */
    @Override
    protected void implRegister(SelectionKeyImpl selectionKey) {
        ensureOpen();
        
        synchronized(updateLock) {
            // "选择键"入队
            newKeys.addLast(selectionKey);
        }
    }
    
    
    /*
     * 平台相关的一部分反注册逻辑：主要是将指定的"选择键"从当前选择器内移除。
     *
     * windows上的实现是将指定的"选择键"从"待监听键列表"中移除
     *
     * 参见：WindowsSelectorImpl中的channelArray和pollWrapper
     */
    @Override
    protected void implDereg(SelectionKeyImpl selectionKey) {
        assert !selectionKey.isValid();
        assert Thread.holdsLock(this);
        
        // 如果该"选择键"已经被处理过了，直接返回
        if(fdMap.remove(selectionKey) == null) {
            return;
        }
        
        // 获取当前"选择键"在"待监听键列表"(Java层)中的索引
        int index = selectionKey.getIndex();
        assert (index >= 0);
        
        /*
         * 如果待移除的"选择键"不是channelArray中最后一个元素，
         * 则用channelArray中最后一个元素覆盖index处的元素，
         * 这样删除更快，不用挪动其他元素。
         */
        if(index != totalChannels - 1) {
            // 获取channelArray中最后一个元素
            SelectionKeyImpl endChannel = channelArray[totalChannels - 1];
            // 用最后一个元素覆盖index处的元素
            channelArray[index] = endChannel;
            // 更新被移动的最后一个元素的位置信息
            endChannel.setIndex(index);
            
            // 使用相同的方式同步更新"待监听键列表"(native层)
            pollWrapper.replaceEntry(pollWrapper, totalChannels - 1, pollWrapper, index);
        }
        
        // 将待移除的"选择键"在"待监听键列表"(Java层)中的索引作废
        selectionKey.setIndex(-1);
        
        // 置空"待监听键列表"(Java层)中最后一个位置
        channelArray[totalChannels - 1] = null;
        
        // 计数减一
        totalChannels--;
        
        // 与growIfNeeded()中最后一步操作相反，这里需要减少操作批次
        if(totalChannels != 1 && totalChannels % MAX_SELECTABLE_FDS == 1) {
            totalChannels--;
            threadsCount--; // The last thread has become redundant.
        }
    }
    
    /*▲ 注册/反注册 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 选择就绪通道 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 选择可用的已就绪通道，返回本轮select()中找到的所有【可用的】"已就绪键"(已就绪通道)的数量
     *
     * 主要有三个步骤：
     *  1.搜集Java层注册的通道的文件描述符和注册监听的事件；
     *  2.把这些被监听的文件描述符交给内核，由内核监听它们的变动事件；
     *  3.内核收到被监听文件描述符的变动事件后，会向上交给Java层的选择器。
     *
     * action : 如果不为null，用来处理可用的"已就绪键"；
     *          如果为null，则会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)
     * timeout: 监听等待中的超时设置(参见SubSelector#poll())
     *          timeout=0表示可以立即返回
     *          timeout=-1表示一直阻塞，直到本地被新来的事件唤醒选择器线程，然后传导到Java层
     *          timeout为其他值表示阻塞timeout毫秒
     */
    @Override
    protected int doSelect(Consumer<SelectionKey> action, long timeout) throws IOException {
        assert Thread.holdsLock(this);
        
        this.timeout = timeout; // set selector timeout
        
        /*
         * 处理"新注册键临时队列"和"已更新键临时队列"，向"待监听键列表"中存储"选择键"和注册的监听事件
         *
         * 具体来说：
         * 向"待监听键列表"(Java层)(channelArray)中存储的是"选择键"，因为"选择键"中已经包含了"通道"和"事件"信息；
         * 向"待监听键列表"(native层)(pollWrapper)中存储的是通道的本地(native层)文件描述符和通道注册的监听事件。
         */
        processUpdateQueue();
        
        // 处理"已取消键临时集合"，参见AbstractSelector#cancelledKeys
        processDeregisterQueue();
        
        // 如果选择器已关闭，则重置"哨兵"元素，并结束select()操作
        if(interruptTriggered) {
            // 清空"哨兵"元素内的数据，并设置interruptTriggered = false，以准备下次select()操作
            resetWakeupSocket();
            return 0;
        }
        
        /*
         * Calculate number of helper threads needed for poll.
         * If necessary threads are created here and start waiting on startLock
         */
        // 调整SelectThread的数量，以适应待处理的SelectionKey的批次，如果有新增的SelectThread，则启动它（启动后陷入阻塞）
        adjustThreadsCount();
        
        /* reset finishLock */
        // 记录所有辅助线程启动前的总数
        finishLock.reset();
        
        /*
         * Wakeup helper threads, waiting on startLock, so they start polling.
         * Redundant threads will exit here after wakeup.
         */
        // 唤醒所有阻塞的(辅助)线程（使辅助线程开始工作）
        startLock.startThreads();
        
        /*
         * do polling in the main thread.
         * Main thread is responsible for first MAX_SELECTABLE_FDS entries in pollArray.
         */
        try {
            // 在一段可能阻塞的I/O操作开始之前，设置线程中断回调标记
            begin();
            
            try {
                // 阻塞主线程，并由底层内核侦听各通道上注册的感兴趣的参数（事件），直到有满足条件的事件达到时，唤醒主线程
                subSelector.poll();
            } catch(IOException e) {
                // 记录选择器线程在等待本地监听的事件传回消息的过程中出现的异常
                finishLock.setException(e); // Save this exception
            }
            
            /* Main thread is out of poll(). Wakeup others and wait for them */
            // 如果仍有正在阻塞的辅助线程
            if(threads.size()>0) {
                /*
                 * 必要时，阻塞主线程，以等待其他辅助线程全部醒来；
                 * 当主线程侦听到注册的事件时会调用此方法
                 */
                finishLock.waitForHelperThreads();
            }
        } finally {
            // 移除之前设置的线程中断回调标记
            end();
        }
        
        /* Done with poll(). Set wakeupSocket to nonsignaled  for the next run */
        // 如果选择器线程在等待本地监听的事件传回消息的过程中出现了异常，则抛出它
        finishLock.checkForException();
        
        // 处理"已取消键临时集合"，参见AbstractSelector#cancelledKeys
        processDeregisterQueue();
        
        /*
         * 在主线程和辅助线程上，找出本轮select()操作中所有可用的"已就绪键"；
         * 返回找到的可用的"已就绪键"的数量。
         *
         * action: 如果不为null，用来处理可用"已就绪键"；否则，会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)
         */
        int updated = updateSelectedKeys(action);
        
        /* Done with poll(). Set wakeupSocket to nonsignaled for the next run */
        // 清空"哨兵"元素内的数据，并设置interruptTriggered = false，以准备下次select()操作
        resetWakeupSocket();
        
        return updated;
    }
    
    /*▲ 选择就绪通道 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 打开/关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 确保选择器已开启，否则会抛出异常
    private void ensureOpen() {
        if(!isOpen()) {
            throw new ClosedSelectorException();
        }
    }
    
    // 平台相关的一部分关闭选择器的逻辑，主要是释放本地内存，且设置interruptTriggered = true
    @Override
    protected void implClose() throws IOException {
        assert !isOpen();
        assert Thread.holdsLock(this);
        
        /* prevent further wakeup */
        // 阻止辅助线程再次被唤醒
        synchronized(interruptLock) {
            interruptTriggered = true;
        }
        
        // 关闭管道
        wakeupPipe.sink().close();
        wakeupPipe.source().close();
        
        // 释本地内存
        pollWrapper.free();
        
        /* Make all remaining helper threads exit */
        // 标记所有辅助线程为作废状态
        for(SelectThread thread : threads) {
            thread.makeZombie();
        }
        
        // 唤醒所有辅助线程，由于上面标记辅助线程为作废，所以辅助线程被唤醒后，就会退出
        startLock.startThreads();
    }
    
    /*▲ 打开/关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 唤醒 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 通过"哨兵"元素唤醒所有阻塞的辅助线程，并设置interruptTriggered = true，后续这些辅助线程将会结束运行
    @Override
    public Selector wakeup() {
        synchronized(interruptLock) {
            if(!interruptTriggered) {
                // 通过"哨兵"元素唤醒所有阻塞的辅助线程
                setWakeupSocket();
                interruptTriggered = true;
            }
        }
        
        return this;
    }
    
    /** Sets Windows wakeup socket to a signaled state */
    // 通过"哨兵"元素唤醒所有阻塞的辅助线程
    private void setWakeupSocket() {
        // 向管道中的写通道写入一字节数据，这样可以唤醒阻塞的线程
        setWakeupSocket0(wakeupSinkFd);
    }
    
    /*
     * 向管道的写通道中写入一字节的数据，这样可以唤醒阻塞的辅助线程
     *
     * 原因是每批"待监听键"的首个元素都注册了管道中的读通道和Net.POLLIN参数，且所有"待监听键"共享一个读通道的文件描述符；
     * 当向写通道写入数据后，共享的那个读通道就可以响应到Net.POLLIN事件，表示有数据可读了，进而唤醒其所在的选择器线程。
     */
    private native void setWakeupSocket0(int wakeupSinkFd);
    
    
    /** Sets Windows wakeup socket to a non-signaled state */
    // 清空"哨兵"元素内的数据，并设置interruptTriggered = false，以准备下次select()操作
    private void resetWakeupSocket() {
        synchronized(interruptLock) {
            if(!interruptTriggered) {
                return;
            }
            
            // 清空管道的读通道中的数据，以便下次select()时再次写入
            resetWakeupSocket0(wakeupSourceFd);
            
            interruptTriggered = false;
        }
    }
    
    // 清空管道的读通道中的数据，以便下次select()时再次写入
    private native void resetWakeupSocket0(int wakeupSourceFd);
    
    /*▲ 唤醒 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 将指定的"选择键"加入到当前选择器的"已更新键队列"中(之前可能已经添加过了)
    @Override
    public void setEventOps(SelectionKeyImpl selectionKey) {
        // 确保通道已打开
        ensureOpen();
        
        synchronized(updateLock) {
            updateKeys.addLast(selectionKey);
        }
    }
    
    
    /**
     * Process new registrations and changes to the interest ops.
     */
    /*
     * 处理"新注册键临时队列"和"已更新键临时队列"，向"待监听键列表"中存储"选择键"和注册的监听事件
     *
     * 具体来说：
     * 向"待监听键列表"(Java层)(channelArray)中存储的是"选择键"，因为"选择键"中已经包含了"通道"和"事件"信息；
     * 向"待监听键列表"(native层)(pollWrapper)中存储的是通道的本地(native层)文件描述符和通道注册的监听事件。
     */
    private void processUpdateQueue() {
        assert Thread.holdsLock(this);
        
        synchronized(updateLock) {
            SelectionKeyImpl selectionKey;
            
            // 遍历"新注册键临时队列"，将所有元素出队
            while((selectionKey = newKeys.pollFirst()) != null) {
                // 忽略已经失效的"选择键"
                if(!selectionKey.isValid()) {
                    continue;
                }
                
                /*
                 * 如果"待监听键列表"已满，则需要扩容。
                 * 如果"待监听键"的数量又凑够了一个批次(1024个)，则需要新建辅助线程去处理。
                 * 当然，这里只是简单地记下实际需要的辅助线程数量，真正创建辅助线程的过程在adjustThreadsCount()中。
                 */
                growIfNeeded();
                
                // 将"新注册键临时队列"中的元素依次存入"待监听键列表"(Java层)
                channelArray[totalChannels] = selectionKey;
                // 记录指定的"选择键"在"待监听键列表"(Java层)中的索引
                selectionKey.setIndex(totalChannels);
                
                /*
                 * 将"新注册键临时队列"中的元素依次存入"待监听键列表"(native层)
                 * 实际存储的是"选择键"中那些通道在本地(native层)的文件描述符
                 *
                 * 注：此处还没存储有效的监听事件，只是让一个无效的事件去占位，对监听事件的真正配置发生的下面的循环中
                 */
                pollWrapper.putEntry(totalChannels, selectionKey);
                
                // 待"待监听键"数量增1
                totalChannels++;
                
                // 存储"待监听键"
                MapEntry previous = fdMap.put(selectionKey);
                assert previous == null;
            }
            
            // 遍历"已更新键临时队列"，将所有元素出队
            while((selectionKey = updateKeys.pollFirst()) != null) {
                // 忽略已经失效的"选择键"
                if(!selectionKey.isValid()) {
                    continue;
                }
                
                /*
                 * 翻译通道注册的监听事件，返回对interestOps的翻译结果
                 *
                 * 方向：Java层 --> native层
                 * 　　　SelectionKey.XXX --> Net.XXX
                 */
                int events = selectionKey.translateInterestOps();
                
                // 获取通道在本地(native层)的文件描述符
                int fd = selectionKey.getFDVal();
                
                /*
                 * 在确定该"选择键"存在的情形下，将其注册的监听事件存入"待监听键列表"(native层)
                 *
                 * 注：下面的操作对于同一个通道来说，会使得后注册进来的监听事件覆盖先前注册的监听事件
                 */
                if(fdMap.containsKey(fd)) {
                    // 获取"选择键"selectionKey在"待监听键列表"(Java层)中的索引
                    int index = selectionKey.getIndex();
                    assert index >= 0 && index<totalChannels;
                    
                    // 向"待监听键列表"(native层)中pollArray[index]处存储注册的监听事件
                    pollWrapper.putEventOps(index, events);
                }
            }
        }
    }
    
    /** After some channels registered/deregistered, the number of required helper threads may have changed. Adjust this number */
    // 调整SelectThread的数量，以适应待处理的SelectionKey的批次，如果有新增的SelectThread，则启动它
    private void adjustThreadsCount() {
        int size = threads.size();
        
        // 如果需要增加SelectThread的数量
        if(threadsCount>size) {
            // More threads needed. Start more threads.
            for(int i = size; i<threadsCount; i++) {
                SelectThread newThread = new SelectThread(i);
                threads.add(newThread);
                newThread.setDaemon(true);
                newThread.start();
            }
            
            // 如果需要减少SelectThread的数量
        } else if(threadsCount<size) {
            // Some threads become redundant. Remove them from the threads List.
            for(int i = size - 1; i >= threadsCount; i--) {
                // 标记移除的线程已经作废
                threads.remove(i).makeZombie();
            }
        }
    }
    
    /** Update ops of the corresponding Channels. Add the ready keys to the ready queue */
    /*
     * 在主线程和辅助线程上，找出本轮select()操作中所有可用的"已就绪键"；
     * 返回找到的可用的"已就绪键"的数量。
     *
     * action: 如果不为null，用来处理可用"已就绪键"；否则，会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)
     */
    private int updateSelectedKeys(Consumer<SelectionKey> action) {
        updateCount++;
        
        int numKeysUpdated = 0;
        
        // 在主线程上找出本轮select()操作中所有可用的"已就绪键"
        numKeysUpdated += subSelector.processSelectedKeys(updateCount, action);
        
        // 在辅助线程上找出本轮select()操作中所有可用的"已就绪键"
        for(SelectThread thread : threads) {
            numKeysUpdated += thread.subSelector.processSelectedKeys(updateCount, action);
        }
        
        return numKeysUpdated;
    }
    
    /*
     * 如果"待监听键列表"已满，则需要扩容。
     * 如果"待监听键"的数量又凑够了一个批次(1024个)，则需要新建辅助线程去处理。
     * 当然，这里只是简单地记下实际需要的辅助线程数量，真正创建辅助线程的过程在adjustThreadsCount()中。
     */
    private void growIfNeeded() {
        if(channelArray.length == totalChannels) {
            // 新容量翻倍
            int newSize = totalChannels * 2; // Make a larger array
            
            // 对"待监听键列表"(Java层)扩容
            SelectionKeyImpl[] temp = new SelectionKeyImpl[newSize];
            System.arraycopy(channelArray, 1, temp, 1, totalChannels - 1);
            channelArray = temp;
            
            // 对"待监听键列表"(native层)扩容
            pollWrapper.grow(newSize);
        }
        
        /*
         * 当totalChannels的值为0、1024、2048...时，标志的新的一批"待监听键"要出现了。
         * 在每批"待监听键列表"(native层)之首，都会存入一个"哨兵"元素。
         *
         * 同时，由于诞生了新一批的"待监听键"，所以自然也需要新的选择器线程来处理它(参见：MAX_SELECTABLE_FDS)。
         * 这个选择器线程不仅在Java层存在，而且在本地(native层)也存在，它们是一一对应的。
         *
         * 除第一批"待监听键"之外，其他每新增一批"待监听键"，在Java层都要同时新增一个选择器线程SelectThread去处理它。
         * 而那第一批"待监听键"呢，它是由主线程来处理的。
         *
         * 为了区别主线程这个选择器线程与后面那些新建的选择器线程，我们把后面那些新建的选择器线程统称为辅助线程。
         * 即当主线程不够用的时候，让辅助线程出来辅助处理其他批次的"待监听键"。
         *
         * 注：所有"哨兵"共享一个文件描述符，因此可被同时唤醒
         */
        if(totalChannels % MAX_SELECTABLE_FDS == 0) {
            // 向"待监听键列表"(native层)的totalChannels处添加一个"哨兵"
            pollWrapper.addWakeupSocket(wakeupSourceFd, totalChannels);
            // "待监听键"数量递增
            totalChannels++;
            // 辅助线程数量增一
            threadsCount++;
        }
    }
    
    // 丢弃fd处的socket中的紧急数据
    private native boolean discardUrgentData(int fd);
    
    
    // 作用在辅助线程上的锁，控制辅助线程的阻塞与唤醒（辅助线程阻塞的原因是等待被startThreads()唤醒）
    private final class StartLock {
        
        /**
         * A variable which distinguishes the current run of doSelect from the previous one.
         * Incrementing runsCounter and notifying threads will trigger another round of poll.
         */
        // 记录startThreads()调用次数
        private long runsCounter;
        
        /** Triggers threads, waiting on this lock to start polling */
        // 唤醒所有辅助线程，并自增runsCounter
        private synchronized void startThreads() {
            runsCounter++;  // next run
            notifyAll();    // wake up threads.
        }
        
        /**
         * This function is called by a helper thread to wait for the next round of poll().
         * It also checks, if this thread became redundant.
         * If yes, it returns true, notifying the thread that it should exit.
         */
        // 阻塞辅助线程thread，直到下次select()中调用startThreads()
        private synchronized boolean waitForStart(SelectThread thread) {
            while(true) {
                /*
                 * 如果lastRun依然与上次设置的runsCounter相同，
                 * 说明目前还没有遇到下一轮startThreads()，于是阻塞当前辅助线程。
                 */
                while(runsCounter == thread.lastRun) {
                    try {
                        startLock.wait();
                    } catch(InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // 如果辅助线程thread已经作废，则退出当前线程
                if(thread.isZombie()) { // redundant thread
                    return true; // will cause run() to exit.
                }
                
                // 每一轮startThreads()中都会更新lastRun参数
                thread.lastRun = runsCounter; // update lastRun
                
                return false; // will cause run() to poll.
            }
        }
    }
    
    /*
     * 用来确保所有辅助线程在主线程之前先完成一轮select。
     *
     * 如果当主线程完成了一轮select时还有其他辅助线程正在阻塞当中，则会将该主线程阻塞，
     * 直到所有辅助线程主动或被迫完成一轮select之后，再唤醒主线程。
     */
    private final class FinishLock {
        /** IOException which occurred during the last run */
        IOException exception = null;
        
        /** Number of helper threads, that did not finish yet */
        // 等待完成(正在阻塞)的辅助线程数量
        private int threadsToFinish;
        
        /** Called before polling */
        // 记录所有辅助线程启动前的总数
        private void reset() {
            threadsToFinish = threads.size(); // helper threads
        }
        
        /** Each helper thread invokes this function on finishLock, when the thread is done with poll() */
        /*
         * 必要时，唤醒所有辅助线程，每个辅助线程醒来都会将threadsToFinish的计数减一；
         * 如果所有辅助线程都醒来了，则唤醒阻塞的主线程。
         *
         * 每个辅助线程侦听到注册的事件时会调用此方法
         *
         * 如果辅助线程侦听到了注册的事件，它会自己醒来调用此方法；
         * 一旦有某个辅助线程或主线程自己醒来，那么它会通过向"哨兵"写入数据，
         * 来间接唤醒其它所有仍在阻塞的辅助线程，这对于其它辅助线程来说，是一个被迫醒来的过程。
         *
         * 如果所有辅助线程都醒来了，则唤醒正在阻塞的主线程
         */
        private synchronized void threadFinished() {
            /*
             * 如果只有当前的辅助线程醒来了，而其它辅助线程都还在阻塞当中，
             * 则通过"哨兵"元素唤醒所有阻塞的辅助线程，
             * 并且，会设置interruptTriggered = true
             */
            if(threadsToFinish == threads.size()) {
                wakeup();
            }
            
            // 每醒来一个辅助线程，则计数减1
            threadsToFinish--;
            
            /* all helper threads finished poll() */
            // 如果所有辅助线程都醒来了，则需要进一步唤醒主线程
            if(threadsToFinish == 0) {
                // 唤醒主线程
                notify();
            }
        }
        
        /** The main thread invokes this function on finishLock to wait for helper threads to finish poll() */
        /*
         * 必要时，阻塞主线程，以等待其他辅助线程全部醒来；
         * 当主线程侦听到注册的事件时会调用此方法。
         *
         * 如果主线程侦听到了注册的事件，它会自己醒来调用此方法；
         * 如果此时其它辅助线程还在阻塞，那么它会通过向"哨兵"写入数据，
         * 来间接唤醒其它所有仍在阻塞的辅助线程，这对于其它辅助线程来说，是一个被迫醒来的过程。
         *
         * 在其他所有辅助线程全部醒来之前，主线程会被阻塞，直到最后一个辅助线程也醒来之后，再唤醒正在阻塞的主线程。
         */
        private synchronized void waitForHelperThreads() {
            /*
             * 如果其它(所有)辅助线程都还在阻塞当中，则通过"哨兵"元素唤醒所有阻塞的辅助线程，
             * 并且，会设置interruptTriggered = true
             */
            if(threadsToFinish == threads.size()) {
                wakeup();
            }
            
            /*
             * 如果仍有阻塞的辅助线程，则把主线程也阻塞住，
             * 直到所有辅助线程都醒来之后，会唤醒阻塞在此处的主线程。
             */
            while(threadsToFinish != 0) {
                try {
                    finishLock.wait();
                } catch(InterruptedException e) {
                    // Interrupted - set interrupted state.
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        /** sets IOException for this run */
        // 记录选择器线程在等待本地监听的事件传回消息的过程中出现的异常
        private synchronized void setException(IOException e) {
            exception = e;
        }
        
        /** Checks if there was any exception during the last run. If yes, throws it */
        // 如果选择器线程在等待本地监听的事件传回消息的过程中出现了异常，则抛出它
        private void checkForException() throws IOException {
            if(exception == null) {
                return;
            }
            
            StringBuffer message = new StringBuffer("An exception occurred during the execution of select(): \n");
            message.append(exception);
            message.append('\n');
            exception = null;
            
            throw new IOException(message.toString());
        }
    }
    
    /*
     * 子选择器，用来分批处理注册在选择器上的通道(每1024个为一批)。
     * 子选择器存在于主线程与辅助线程中，干的活儿是一样的。
     *
     * 注：相对于Selector类，SubSelector才是进行核心操作的选择器。
     */
    private final class SubSelector {
        
        // 标记当前这批被待监听的"选择键"在"待监听键列表"(native层)(pollArray)上的起始索引
        private final int pollArrayIndex;
        
        /*
         * These arrays will hold result of native select().
         * The first element of each array is the number of selected sockets.
         * Other elements are file descriptors of selected sockets.
         */
        /*
         * 本地(native层)收到文件描述符的变动事件后，会将其填充到readFds/writeFds/exceptFds
         * 注：只要注册监听的文件描述符上有合规的变动事件就会返回，换句话说，该变动事件未必是Java层注册的监听事件，需要后续进一步筛选
         */
        private final int[] readFds = new int[MAX_SELECTABLE_FDS + 1];
        private final int[] writeFds = new int[MAX_SELECTABLE_FDS + 1];
        private final int[] exceptFds = new int[MAX_SELECTABLE_FDS + 1];
        
        // 构造一个子选择器以处理第一批"待监听键"，用在主线程中
        private SubSelector() {
            // 第一批"待监听键"在pollWrapper上的起始下标为0
            this.pollArrayIndex = 0;
        }
        
        /** helper threads */
        // 构造一个子选择器以处理第(threadIndex+1)批"待监听键"，用在辅助线程中
        private SubSelector(int threadIndex) {
            this.pollArrayIndex = (threadIndex + 1) * MAX_SELECTABLE_FDS;
        }
        
        /** poll for the main thread */
        /*
         * 阻塞主线程，并监听当前子选择器内注册的通道(的文件描述符)；
         * 当本地(native层)收到文件描述符的变动事件后，会将其向上通知到Java层，并唤醒主线程中的选择器。
         */
        private int poll() throws IOException {
            return poll0(pollWrapper.pollArrayAddress, Math.min(totalChannels, MAX_SELECTABLE_FDS), readFds, writeFds, exceptFds, timeout);
        }
        
        /** poll for helper threads */
        /*
         * 阻塞辅助线程，并监听当前子选择器内注册的通道(的文件描述符)；
         * 当本地(native层)收到文件描述符的变动事件后，会将其向上通知到Java层，并唤醒辅助线程中的选择器。
         */
        private int poll(int index) throws IOException {
            return poll0(pollWrapper.pollArrayAddress + (pollArrayIndex * PollArrayWrapper.SIZE_POLLFD), Math.min(MAX_SELECTABLE_FDS, totalChannels - (index + 1) * MAX_SELECTABLE_FDS), readFds, writeFds, exceptFds, timeout);
        }
        
        /*
         * 阻塞当前线程，并监听当前子选择器内注册的通道(的文件描述符)；
         * 当本地(native层)收到文件描述符的变动事件后，会将其向上通知到Java层，并唤醒当前线程中的选择器。
         */
        private native int poll0(long pollAddress, int numfds, int[] readFds, int[] writeFds, int[] exceptFds, long timeout);
        
        /*
         * 在当前线程上找出本轮select()操作中所有可用的"已就绪键"。
         *
         * 有些文件描述符对应的"选择键"虽然也已经就绪了，但是它的就绪事件与该"选择键"注册监听的事件不匹配，
         * 那么这类"已就绪键"会被视为不可用而将其忽略。
         *
         * 返回值表示本轮select()操作中总共找到了几个可用的"已就绪键"。
         *
         * updateCount: 当前是第几轮select()操作
         * action     : 如果不为null，用来处理可用"已就绪键"；否则，会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)
         */
        private int processSelectedKeys(long updateCount, Consumer<SelectionKey> action) {
            int numKeysUpdated = 0;
            numKeysUpdated += processFDSet(updateCount, action, readFds, Net.POLLIN, false);
            numKeysUpdated += processFDSet(updateCount, action, writeFds, Net.POLLCONN | Net.POLLOUT, false);
            numKeysUpdated += processFDSet(updateCount, action, exceptFds, Net.POLLIN | Net.POLLCONN | Net.POLLOUT, true);
            return numKeysUpdated;
        }
        
        /**
         * updateCount is used to tell if a key has been counted as updated in this select operation.
         *
         * me.updateCount <= updateCount
         */
        /*
         * 遍历由底层内核填充完毕的文件描述符数组fds，找出注册了rOps参数/事件，且当前已就绪的SelectionKey，将其存入集合selectedKeys中(参见SelectorImpl的selectedKeys属性)
         * 返回值指示本次遍历中发现了多少个注册了rOps事件且就绪的文件描述符（在一轮select中不会重复统计该文件描述符）
         *
         * 本地(native层)收到注册监听的文件描述符的变动事件后，会将其填充到fds。
         * 接下来，会将fds上可能发生的变动事件rOps与在fds上注册监听的事件进行比较，
         * 如果比较结果是匹配的，则视情形处理该fds对应的可用的"已就绪键"(处理过程参见processReadyEvents方法)，
         * 如果比较结果是不匹配的，则跳过这个"已就绪键"。
         *
         * 返回值表示对fds的此轮遍历中找到了几个可用的"已就绪键"
         *
         * updateCount: 当前是第几轮select()操作
         * action     : 如果不为空，用来处理可用"已就绪键"；否则，会将可用的"已就绪键"存储到"已就绪键集合"中(参见SelectorImpl#selectedKeys)
         * fds        : 本地(native层)反馈上来的已就绪的文件描述符
         * rOps       : 该类fds上所有可能发生的变动事件
         * isExceptFds: 是否正在处理exceptFds中的文件描述符
         */
        private int processFDSet(long updateCount, Consumer<SelectionKey> action, int[] fds, int rOps, boolean isExceptFds) {
            int numKeysUpdated = 0;
            
            // 0号单元记录了fds中的元素数量，所以这里从索引1处开始遍历就可以
            for(int i = 1; i<=fds[0]; i++) {
                int fdVal = fds[i];
                
                // 如果遍历途中遇到哨兵元素，则跳过该元素
                if(fdVal == wakeupSourceFd) {
                    synchronized(interruptLock) {
                        // 设置中断标记
                        interruptTriggered = true;
                    }
                    continue;
                }
                
                // 跳过那些已经失效的文件描述符(它们所在的"选择键"已被取消了)
                MapEntry mapEntry = fdMap.get(fdVal);
                if(mapEntry == null) {
                    /* If me is null, the key was deregistered in the previous processDeregisterQueue */
                    continue;
                }
                
                // 获取已经就绪的"选择键"(监听到了注册的事件)
                SelectionKeyImpl selectionKey = mapEntry.selectionKey;
                
                /*
                 * The descriptor may be in the exceptfds set because there is OOB data queued to the socket.
                 * If there is OOB data then it is discarded and the key is not added to the selected set.
                 */
                // 如果遇到exceptFds数组，则丢弃socket中的紧急数据
                if(isExceptFds && (selectionKey.channel() instanceof SocketChannelImpl)
                    // 丢弃fdVal处的socket中的紧急数据
                    && discardUrgentData(fdVal)) {
                    continue;
                }
                
                // 处理已就绪的通道(选择键)，即处理本地(native层)监听到变动事件的文件描述符，返回值指示该"选择键"是否可用(0-不可用，1-可用)
                int updated = processReadyEvents(rOps, selectionKey, action);
                // 如果该"选择键"不可用，则忽略后续操作
                if(updated<=0) {
                    continue;
                }
                
                // 统计可用的"已就绪键"，对于本轮已经统计过的键不会重复统计
                if(mapEntry.updateCount != updateCount) {
                    mapEntry.updateCount = updateCount;
                    numKeysUpdated++;
                }
            }
            
            return numKeysUpdated;
        }
    }
    
    /** Represents a helper thread used for select */
    /*
     * 辅助线程，用来分批处理大量注册监听的通道(文件描述符)
     *
     * 当注册监听的通道(文件描述符)数量超过1024个以后，就需要辅助线程的介入，进行分批并行处理。
     *
     * 注：辅助线程是作为守护线程存在的，且是在主线程中启动的
     *
     * 参见：MAX_SELECTABLE_FDS
     */
    private final class SelectThread extends Thread {
        /*
         * 辅助线程中的子选择器处理的是第一批之后的"待监听键"，
         * 这批"待监听键"在"待监听键列表"(native层)中的起始索引是(index+1)，
         * 因此，需要使用带参数的那个构造器。
         */
        final SubSelector subSelector;
        
        // 指示当前选择器线程中注册的这批文件描述符在"待监听键列表"(native层)(pollWrapper)中的索引是(index+1)
        private final int index;
        
        /*
         * 标记当前线程是否已经作废
         *
         * 当注册监听的文件描述符(的批次)减少时，会把空闲出来的辅助线程标记为作废，即设置zombie为true。
         * 当辅助线程进行下一轮循环时，检测到此标记会退出循环
         */
        private volatile boolean zombie;
        
        /** last run number */
        /*
         * 记录当前线程的循环次数，这是由startThreads()的调用次数决定的
         *
         */
        private long lastRun = 0;
        
        // Creates a new thread
        private SelectThread(int index) {
            super(null, null, "SelectorHelper", 0, false);
            
            this.index = index;
            
            this.subSelector = new SubSelector(index);
            
            // make sure we wait for next round of poll
            this.lastRun = startLock.runsCounter;
        }
        
        public void run() {
            // poll loop
            while(true) {
                /* wait for the start of poll. If this thread has become redundant, then exit */
                // 阻塞当前辅助线程，直到下次select()中调用startThreads()
                if(startLock.waitForStart(this)) {
                    // 如果当前线程已经作废，退出循环(线程结束运行)
                    return;
                }
                
                // call poll()
                try {
                    // 阻塞当前辅助线程，并由底层内核侦听各通道上注册的感兴趣的参数（事件），直到有满足条件的事件达到时，唤醒辅助线程
                    subSelector.poll(index);
                } catch(IOException e) {
                    /* Save this exception and let other threads finish */
                    // 记录选择器线程在等待本地监听的事件传回消息的过程中出现的异常
                    finishLock.setException(e);
                }
                
                /*
                 * 必要时，唤醒所有辅助线程，每个辅助线程醒来都会将threadsToFinish的计数减一；
                 * 如果所有辅助线程都醒来了，则唤醒阻塞的主线程。
                 *
                 * 每个辅助线程侦听到注册的事件时会调用此方法
                 */
                finishLock.threadFinished();
            }
        }
        
        // 标记当前线程已经作废
        void makeZombie() {
            zombie = true;
        }
        
        // 判断当前线程是否作废
        boolean isZombie() {
            return zombie;
        }
    }
    
    /** class for fdMap entries */
    // 对"待监听键"的封装
    private static final class MapEntry {
        final SelectionKeyImpl selectionKey;
        
        /*
         * 在统计可用的"已就绪键"时，记录该统计操作处于第几轮select()中。
         * 对于处于同一轮下的统计操作，不会重复统计本轮已统计过的键
         */ long updateCount = 0;
        
        MapEntry(SelectionKeyImpl selectionKey) {
            this.selectionKey = selectionKey;
        }
    }
    
    /** Maps file descriptors to their indices in  pollArray */
    // 存储"待监听键"及其文件描述符
    private static final class FdMap extends HashMap<Integer, MapEntry> {
        static final long serialVersionUID = 0L;
        
        // 根据文件描述符获取其所在的SelectionKey
        private MapEntry get(int fdVal) {
            return get(Integer.valueOf(fdVal));
        }
        
        // 将SelectionKey存到FdMap中
        private MapEntry put(SelectionKeyImpl selectionKey) {
            return put(selectionKey.getFDVal(), new MapEntry(selectionKey));
        }
        
        // 移除指定的selectionKey，并返回移除的元素
        private MapEntry remove(SelectionKeyImpl selectionKey) {
            Integer fdVal = selectionKey.getFDVal();
            
            MapEntry entry = get(fdVal);
            
            if((entry != null) && (entry.selectionKey.channel() == selectionKey.channel())) {
                return remove(fdVal);
            }
            
            return null;
        }
    }
    
}
