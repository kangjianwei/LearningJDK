/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.IllegalChannelGroupException;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

// 异步通道组和异步Socket通道的工厂在windows平台上的实现
public class WindowsAsynchronousChannelProvider extends AsynchronousChannelProvider {
    
    private static volatile Iocp defaultIocp;
    
    public WindowsAsynchronousChannelProvider() {
        // nothing to do
    }
    
    // 返回一个带有固定容量线程池的异步通道组，线程池容量为nThreads
    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads, ThreadFactory factory) throws IOException {
        // 返回一个异步IO线程池：容量固定，固定容量为nThreads
        ThreadPool pool = ThreadPool.create(nThreads, factory);
        // 构造通道组，并启动工作线程
        return new Iocp(this, pool).start();
    }
    
    // 返回一个包含指定线程池的异步通道组，线程池初始容量为initialSize(具体值还需要进一步计算)
    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor, int initialSize) throws IOException {
        // 将指定的【任务执行框架】包装为异步IO线程池：容量非固定，初始容量为initialSize(具体值还需要进一步计算)
        ThreadPool pool = ThreadPool.wrap(executor, initialSize);
        // 构造通道组，并启动工作线程
        return new Iocp(this, pool).start();
    }
    
    // 打开一个异步Socket通道，group是该通道关联的异步通道组
    @Override
    public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group) throws IOException {
        // 向下类型转换：将group转换为当前平台上实现的通道组类型；如果group为null，则创建并返回一个系统默认的通道组(工作线程已启动)
        Iocp iocp = toIocp(group);
        // 构造异步Socket通道并返回
        return new WindowsAsynchronousSocketChannelImpl(iocp);
    }
    
    // 打开一个异步ServerSocket通道，group是该通道关联的异步通道组
    @Override
    public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(AsynchronousChannelGroup group) throws IOException {
        // 向下类型转换：将group转换为当前平台上实现的通道组类型；如果group为null，则创建并返回一个系统默认的通道组(工作线程已启动)
        Iocp iocp = toIocp(group);
        // 构造异步ServerSocket通道并返回
        return new WindowsAsynchronousServerSocketChannelImpl(iocp);
    }
    
    // 向下类型转换：将group转换为当前平台上实现的通道组类型；如果group为null，则创建并返回一个系统默认的通道组(工作线程已启动)
    private Iocp toIocp(AsynchronousChannelGroup group) throws IOException {
        if(group == null) {
            // 创建并返回一个系统默认的通道组(工作线程已启动)
            return defaultIocp();
        }
        
        if(!(group instanceof Iocp)) {
            throw new IllegalChannelGroupException();
        }
        
        return (Iocp) group;
    }
    
    // 创建并返回一个系统默认的通道组(工作线程已启动)
    private Iocp defaultIocp() throws IOException {
        // 双重检查锁
        if(defaultIocp == null) {
            synchronized(WindowsAsynchronousChannelProvider.class) {
                if(defaultIocp == null) {
                    // 获取一个异步IO线程池：容量非固定，初始容量默认与处理器数量一致(接受用户的自定义)
                    ThreadPool pool = ThreadPool.getDefault();
                    
                    /* default thread pool may be shared with AsynchronousFileChannels */
                    // 构造通道组，并启动工作线程
                    defaultIocp = new Iocp(this, pool).start();
                }
            }
        }
        
        return defaultIocp;
    }
    
}
