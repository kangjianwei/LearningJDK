/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation class for watch services.
 */
/*
 * 监视服务的抽象实现
 *
 * 注：下文中提到的"主线程"是指构造监视服务的线程，该线程不一定是传统意义上main方法所在的线程，其本身可能也是一个子线程。
 */
abstract class AbstractWatchService implements WatchService {
    
    /** special key to indicate that watch service is closed */
    // 用来指示监视服务已经关闭的监视键
    private final WatchKey CLOSE_KEY = new AbstractWatchKey(null, null) {
        @Override
        public boolean isValid() {
            return true;
        }
        
        @Override
        public void cancel() {
        }
    };
    
    // "关闭"服务时用到的锁
    private final Object closeLock = new Object();
    
    /** signaled keys waiting to be dequeued */
    // 使用阻塞队列存储监视键
    private final LinkedBlockingDeque<WatchKey> pendingKeys = new LinkedBlockingDeque<>();
    
    /** used when closing watch service */
    // 标记监视服务是否已经关闭
    private volatile boolean closed;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    protected AbstractWatchService() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注册 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Register the given object with this watch service
     */
    /*
     * 将路径path指示的目录(树)注册给监视服务watcher。
     *
     * path     : 待监视目录
     * events   : 监视的事件类型；通常从StandardWatchEventKinds中获取
     * modifiers: 对被监视事件的修饰，参见ExtendedWatchEventModifier(通常用这个，可以决定是否监视子目录)和SensitivityWatchEventModifier
     */
    abstract WatchKey register(Path path, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifers) throws IOException;
    
    /*▲ 注册 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 由主线程调用：向子线程(工作线程)请求"关闭"服务，并阻塞主线程；直到"关闭"完成后，唤醒主线程。
     * 具体的关闭行为包括移除所有未处理监视键，且加入一个特殊的监视键CLOSE_KEY。
     */
    @Override
    public final void close() throws IOException {
        synchronized(closeLock) {
            // nothing to do if already closed
            if(closed) {
                return;
            }
            
            closed = true;
            
            // 由主线程调用：向子线程(工作线程)请求"关闭"服务，并阻塞主线程；直到"关闭"完成后，唤醒主线程
            implClose();
            
            // clear pending keys and queue special key to ensure that any threads blocked in take/poll wakeup
            pendingKeys.clear();            // 移除所有未处理的监视键
            pendingKeys.offer(CLOSE_KEY);   // 加入一个监视服务关闭的标记
        }
    }
    
    /**
     * Closes this watch service. This method is invoked by the close
     * method to perform the actual work of closing the watch service.
     */
    // 由主线程调用：向子线程(工作线程)请求"关闭"服务，并阻塞主线程；直到"关闭"完成后，唤醒主线程
    abstract void implClose() throws IOException;
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取出监视键 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 由主线程调用：取出下一个监视键，如果不存在，则阻塞
    @Override
    public final WatchKey take() throws InterruptedException {
        checkOpen();
        WatchKey key = pendingKeys.take();
        checkKey(key);
        return key;
    }
    
    // 由主线程调用：取出下一个监视键，如果不存在，则返回null
    @Override
    public final WatchKey poll() {
        checkOpen();
        WatchKey key = pendingKeys.poll();
        checkKey(key);
        return key;
    }
    
    // 由主线程调用：取出下一个监视键，如果不存在，则阻塞，如果阻塞超时后仍然没有监视键，则返回null
    @Override
    public final WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
        checkOpen();
        WatchKey key = pendingKeys.poll(timeout, unit);
        checkKey(key);
        return key;
    }
    
    /*▲ 取出监视键 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 添加监视键 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** used by AbstractWatchKey to enqueue key */
    // 通常由子线程(工作线程)调用：将指定的监视键加入到当前监视服务的阻塞队列中，并唤醒等待从监视服务获取监视键的(主)线程
    final void enqueueKey(WatchKey key) {
        pendingKeys.offer(key);
    }
    
    /*▲ 添加监视键 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Tells whether or not this watch service is open.
     */
    // 判断当前监视服务是否正在运行
    final boolean isOpen() {
        return !closed;
    }
    
    /**
     * Retrieves the object upon which the close method synchronizes.
     */
    // 返回关闭监视服务时用到的同步锁
    final Object closeLock() {
        return closeLock;
    }
    
    /**
     * Throws ClosedWatchServiceException if watch service is closed
     */
    // 检查监视服务是否正在运行
    private void checkOpen() {
        if(closed) {
            throw new ClosedWatchServiceException();
        }
    }
    
    /**
     * Checks the key isn't the special CLOSE_KEY used to unblock threads when the watch service is closed.
     */
    // 检查监视键
    private void checkKey(WatchKey key) {
        if(key == CLOSE_KEY) {
            /* re-queue in case there are other threads blocked in take/poll */
            // 如果取到的监视键是关闭标记，则将其再次入队
            enqueueKey(key);
        }
        
        checkOpen();
    }
    
}
