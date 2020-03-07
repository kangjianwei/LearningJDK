/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Base implementation of background poller thread used in watch service implementations.
 * A poller thread waits on events from the file system and also services "requests" from clients to register
 * for new events or cancel existing registrations.
 */
/*
 * 监视服务后台使用的轮询器
 *
 * 注：下文中提到的"主线程"是指构造监视服务的线程，该线程不一定是传统意义上main方法所在的线程，其本身可能也是一个子线程。
 */
abstract class AbstractPoller implements Runnable {
    
    /** list of requests pending to the poller thread */
    // 保存待处理的Request(来自主线程的注册/取消/关闭操作)
    private final LinkedList<Request> requestList;
    
    /** set to true when shutdown */
    // 指示监视服务是否已关闭
    private boolean shutdown;
    
    protected AbstractPoller() {
        this.requestList = new LinkedList<>();
        this.shutdown = false;
    }
    
    /**
     * Starts the poller thread
     */
    // 在新建的守护工作线程中启动当前轮询器
    public void start() {
        final Runnable thisRunnable = this;
    
        AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Object run() {
                Thread thr = new Thread(null, thisRunnable, "FileSystemWatchService", 0, false);
                thr.setDaemon(true);
                thr.start();
                return null;
            }
        });
    }
    
    /**
     * Requests, and waits on, poller thread to register given file.
     */
    /*
     * 由主线程调用：向子线程(工作线程)请求"注册"服务(将dir注册给当前监视服务轮询器)，并阻塞主线程；
     * 直到"注册"完成后，唤醒主线程，返回监视键
     *
     * dir      : 待监视目录(树)
     * events   : 监视的事件类型；通常从StandardWatchEventKinds中获取
     * modifiers: 对被监视事件的修饰，这里使用ExtendedWatchEventModifier，指示是否监视子目录
     */
    final WatchKey register(Path dir, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        // validate arguments before request to poller
        if(dir == null) {
            throw new NullPointerException();
        }
        
        // 存储注册进来的【标准】监视事件
        Set<WatchEvent.Kind<?>> eventSet = new HashSet<>(events.length);
        
        // 遍历注册的监视事件
        for(WatchEvent.Kind<?> event : events) {
            // 记录标准监视事件
            if(event == StandardWatchEventKinds.ENTRY_CREATE || event == StandardWatchEventKinds.ENTRY_MODIFY || event == StandardWatchEventKinds.ENTRY_DELETE) {
                eventSet.add(event);
                continue;
            }
            
            // OVERFLOW 监视事件会被忽略
            if(event == StandardWatchEventKinds.OVERFLOW) {
                continue;
            }
            
            // null/unsupported
            if(event == null) {
                throw new NullPointerException("An element in event set is 'null'");
            }
            
            throw new UnsupportedOperationException(event.name());
        }
        
        // 如果不存在监视事件，则抛异常
        if(eventSet.isEmpty()) {
            throw new IllegalArgumentException("No events to register");
        }
        
        // 由主线程调用：向子线程(工作线程)请求"注册"服务，并阻塞主线程；直到"注册"完成后，唤醒主线程，返回监视键
        return (WatchKey) invoke(RequestType.REGISTER, dir, eventSet, modifiers);
    }
    
    /**
     * Cancels, and waits on, poller thread to cancel given key.
     */
    // 由主线程调用：向子线程(工作线程)请求"取消"服务，并阻塞主线程；直到"取消"完成后，唤醒主线程
    final void cancel(WatchKey key) {
        try {
            invoke(RequestType.CANCEL, key);
        } catch(IOException x) {
            // should not happen
            throw new AssertionError(x.getMessage());
        }
    }
    
    /**
     * Shutdown poller thread
     */
    // 由主线程调用：向子线程(工作线程)请求"关闭"服务，并阻塞主线程；直到"关闭"完成后，唤醒主线程
    final void close() throws IOException {
        invoke(RequestType.CLOSE);
    }
    
    /**
     * Enqueues request to poller thread and waits for result
     */
    /*
     * 由主线程调用，向子线程(工作线程)请求注册/取消/关闭服务，
     * 此操作会唤醒阻塞在GetQueuedCompletionStatus()上的子线程(工作线程)，参见：WindowsWatchService.Poller#run()。
     * 随后，将主线程阻塞，并由子线程处理该请求，参见AbstractPoller#processRequests(),
     * 直到子线程(工作线程)处理完注册/取消/关闭请求后，再唤醒阻塞在awaitResult()上的主线程，参见Request#release()
     * 并返回由工作线程生成的响应结果，该响应结果是一个就绪的监视键(注册请求)，或者为null(取消/关闭请求)，又或者是一个异常信息。
     */
    private Object invoke(RequestType type, Object... params) throws IOException {
        // 将入参包装一次Request
        Request req = new Request(type, params);
        
        synchronized(requestList) {
            if(shutdown) {
                throw new ClosedWatchServiceException();
            }
            
            // 保存待处理的Request(来自主线程的注册/取消/关闭操作)
            requestList.add(req);
            
            /*
             * 唤醒阻塞在GetQueuedCompletionStatus()上的工作线程
             *
             * 工作线程醒来后，会执行processRequests()方法处理Request
             *
             * Request被处理完后，会生成一个响应结果result，并唤醒被awaitResult()阻塞的主线程
             */
            wakeup();
        }
        
        // 由主线程调用：使主线程陷入阻塞，等待子线程(工作线程)处理主线程发送过去的注册/取消/关闭请求后，返回来自子线程的响应结果
        Object result = req.awaitResult();
        
        if(result instanceof RuntimeException) {
            throw (RuntimeException) result;
        }
        
        if(result instanceof IOException) {
            throw (IOException) result;
        }
        
        // 如果响应结果不是异常，那么如果主线程发出了注册请求，这里返回就绪的监视键；如果主线程发出了取消/关闭请求，这里返回null
        return result;
    }
    
    /**
     * Wakeup poller thread so that it can service pending requests
     */
    /*
     * 由主线程调用：主线程向子线程(工作线程)发出注册/取消/关闭请求之后，
     * 会立即调用此方法来唤醒阻塞在GetQueuedCompletionStatus()上的子线程(工作线程)处理请求
     */
    abstract void wakeup() throws IOException;
    
    /**
     * Invoked by poller thread to process all pending requests
     *
     * @return true if poller thread should shutdown
     */
    /*
     * 由子线程调用：处理来自主线程的注册/取消/关闭请求，参见WindowsWatchService.Poller#run()。
     * 执行完成后，会唤醒阻塞在Request#awaitResult()上的主线程，参见AbstractPoller#invoke()。
     * 返回值指示是否需要关闭子线程(工作线程)。
     */
    @SuppressWarnings("unchecked")
    boolean processRequests() {
        synchronized(requestList) {
            Request req;
            
            // 从requestList取出来自主线程的服务请求
            while((req = requestList.poll()) != null) {
                
                /* if in process of shutdown then reject request */
                // 如果监视服务已关闭，则生成异常信息
                if(shutdown) {
                    ClosedWatchServiceException exception = new ClosedWatchServiceException();
                    req.release(exception);
                    continue;
                }
                
                // 获取请求的类型：注册/取消/关闭
                switch(req.type()) {
                    
                    /* Register directory */
                    // 主线程请求"注册"服务
                    case REGISTER: {
                        // 请求参数
                        Object[] params = req.parameters();
                        
                        // 1.请求监视的目录
                        Path path = (Path) params[0];
                        
                        // 2.注册的监视事件
                        Set<? extends WatchEvent.Kind<?>> events = (Set<? extends WatchEvent.Kind<?>>) params[1];
                        
                        // 3.监视事件的修饰符
                        WatchEvent.Modifier[] modifiers = (WatchEvent.Modifier[]) params[2];
                        
                        /*
                         * 由子线程调用，执行注册操作。
                         * 将指定的目录(树)注册到监视服务中，返回被监视目录(树)关联的监视键。
                         */
                        Object result = implRegister(path, events, modifiers);
                        
                        // 将"注册请求"的响应结果设置为被监视文件关联的监视键
                        req.release(result);
                        
                        break;
                    }
                    
                    /* Cancel existing key */
                    // 主线程请求"取消"服务
                    case CANCEL: {
                        // 请求参数
                        Object[] params = req.parameters();
                        
                        // 被监视文件关联的监视键
                        WatchKey key = (WatchKey) params[0];
                        
                        /*
                         * 由子线程调用，执行"取消"操作。
                         * 取消/作废(已注册目录关联的)监视键，即表示不再监视其指示的目录
                         */
                        implCancelKey(key);
                        
                        // 将"取消请求"的响应结果设置为null
                        req.release(null);
                        
                        break;
                    }
                    
                    /* Close watch service */
                    // 主线程请求"关闭"服务
                    case CLOSE: {
                        /*
                         * 由子线程调用，执行"关闭"操作。
                         * 关闭监视服务，会取消所有(已注册目录关联的)监视键
                         */
                        implCloseAll();
                        
                        // 将"关闭请求"的响应结果设置为null
                        req.release(null);
                        
                        // 标记监视服务已关闭
                        shutdown = true;
                        
                        break;
                    }
                    
                    default: {
                        IOException exception = new IOException("request not recognized");
                        req.release(exception);
                    }
                }
            }
        }
        
        return shutdown;
    }
    
    /**
     * Executed by poller thread to register directory for changes
     */
    /*
     * 由子线程调用，执行注册操作。
     * 将指定的目录(树)注册到监视服务中，返回被监视目录(树)关联的监视键。
     */
    abstract Object implRegister(Path path, Set<? extends WatchEvent.Kind<?>> events, WatchEvent.Modifier... modifiers);
    
    /**
     * Executed by poller thread to cancel key
     */
    /*
     * 由子线程调用，执行"取消"操作。
     * 取消/作废(已注册目录关联的)监视键，即表示不再监视其指示的目录
     */
    abstract void implCancelKey(WatchKey key);
    
    /**
     * Executed by poller thread to shutdown and cancel all keys
     */
    /*
     * 由子线程调用，执行"关闭"操作。
     * 关闭监视服务，会取消所有(已注册目录关联的)监视键
     */
    abstract void implCloseAll();
    
    
    /**
     * Types of request that the poller thread must handle
     */
    // 请求类型
    private enum RequestType {
        REGISTER,   // 注册监视服务
        CANCEL,     // 取消监视键
        CLOSE       // 关闭监视服务
    }
    
    /**
     * Encapsulates a request (command) to the poller thread.
     */
    // 来自主线程的请求，主要包括注册/取消/关闭行为
    private static class Request {
        private final RequestType type; // 请求类型，包括注册、取消、关闭
        private final Object[] params;  // 请求参数，包括监视目录，监视的事件类型以及对被监视事件的修饰
        
        private boolean completed = false;  // 是否有了响应结果
        
        private Object result = null;       // 响应结果，可以是就绪的监视键，也可以为异常，还可以为null
        
        Request(RequestType type, Object... params) {
            this.type = type;
            this.params = params;
        }
        
        RequestType type() {
            return type;
        }
        
        Object[] parameters() {
            return params;
        }
        
        /**
         * Await completion of the request. The return value is the result of the request.
         */
        // 由主线程调用：使主线程陷入阻塞，等待子线程(工作线程)处理主线程发送过去的注册/取消/关闭请求后，返回来自子线程的响应结果
        Object awaitResult() {
            boolean interrupted = false;
            
            synchronized(this) {
                // 指示此阶段任务是否已完成
                while(!completed) {
                    try {
                        wait();
                    } catch(InterruptedException x) {
                        interrupted = true;
                    }
                }
                
                // 如果出现了中断异常
                if(interrupted) {
                    // 中断线程（只是给线程预设一个标记，不是立即让线程停下来）
                    Thread.currentThread().interrupt();
                }
                
                return result;
            }
        }
        
        // 子线程(工作线程)处理完主线程发送过去的注册/取消/关闭请求后，会标记此阶段任务已完成，随后会唤醒阻塞在awaitResult()上的主线程
        void release(Object result) {
            synchronized(this) {
                this.completed = true;
                this.result = result;
                notifyAll();
            }
        }
        
    }
}
