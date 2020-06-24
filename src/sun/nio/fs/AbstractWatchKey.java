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

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base implementation class for watch keys.
 */
/*
 * 监视键的抽象实现
 *
 * 监视键是主线程与子线程(工作线程)的沟通桥梁，其生成方式是：
 * 主线程向子线程(工作线程)发出"注册"请求，子线程(工作线程)处理完该请求后，返回继续的监视键。
 *
 * 监视键记录了被监视目录，以及包含了从子线程(工作线程)反馈的目标事件。
 *
 * 子线程(工作线程)发现(命中)监视事件时，会视情形将其封装为WatchEvent；
 * 随后，将WatchEvent存入监视键的待处理事件集合，并且再次将监视键加入到监视服务中为其准备的队列容器中；
 * 最后，主线程会从上述队列容器中取出监视键，并进一步从监视键中获取监视到的事件。
 */
abstract class AbstractWatchKey implements WatchKey {
    
    /**
     * Special event to signal overflow
     */
    static final Event<Object> OVERFLOW_EVENT = new Event<>(StandardWatchEventKinds.OVERFLOW, null);
    
    // reference to the original directory
    private final Path dir;                     // 当前监视键关联的被监视的目录(树)
    
    // reference to watcher
    private final AbstractWatchService watcher; // 当前监视键使用的监视服务(多个目录可以共用一个监视服务)
    
    // key state
    private State state;                        // 监视键状态
    
    /**
     * Maximum size of event list (in the future this may be tunable)
     */
    static final int MAX_EVENT_LIST_SIZE = 512; // 待处理事件的集合的最大容量
    
    // 挂起的待处理事件，这些事件由系统触发的，且事件类型是由用户注册的；这里的事件可以是各种类型(对于连续发生的同类型事件，会将其合并，只增加计数)
    private List<WatchEvent<?>> events;
    
    /*
     * 挂起的待处理的ENTRY_MODIFY(更改)事件映射，key是事件上下文(路径)，value是待处理的ENTRY_MODIFY(更改)事件。
     *
     * 设置该字段的目的是对ENTRY_MODIFY(更改)事件特殊处理：对于同一个文件的多个ENTRY_MODIFY(更改)事件，即使没有连续发生，也会将其合并。
     */
    private Map<Object, WatchEvent<?>> lastModifyEvents;    // maps a context to the last event for the context (if the last queued event for the context is an ENTRY_MODIFY event)
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    protected AbstractWatchKey(Path dir, AbstractWatchService watcher) {
        this.dir = dir;
        this.watcher = watcher;
        this.state = State.READY;
        this.events = new ArrayList<>();
        this.lastModifyEvents = new HashMap<>();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取出/添加事件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 从监视键内取出当前可视的待处理事件，这些事件都是系统触发的，且事件类型是之前被注册的；处理完这些事件后，通常应当调用reset()重置/复用监视键
    @Override
    public final List<WatchEvent<?>> pollEvents() {
        synchronized(this) {
            // 取走已经封装好的事件
            List<WatchEvent<?>> result = events;
            
            // 构造新容器来存放待处理事件（这些事件很可能与上面取走的事件是同一批）
            events = new ArrayList<>();
            lastModifyEvents.clear();
            
            return result;
        }
    }
    
    /**
     * Adds the event to this key and signals it.
     */
    /*
     * 将底层响应的kind类型的事件封装到监视键watchKey，如果该事件"有效"，则context指示触发该事件的子目录。
     * 随后，将封装好的监视键标记为激活，并将其加入到所属监视服务的阻塞队列中，
     * 最后，唤醒阻塞在出队操作上的主线程，参见WatchService中的take()和poll()。
     */
    @SuppressWarnings("unchecked")
    final void signalEvent(WatchEvent.Kind<?> kind, Object context) {
        // 判断待处理事件是否ENTRY_MODIFY(更改)事件
        boolean isModify = (kind == StandardWatchEventKinds.ENTRY_MODIFY);
        
        synchronized(this) {
            // 获取待处理事件的数量
            int size = events.size();
            
            // 如果当前已有待处理事件
            if(size>0) {
                
                // 获取最近一个挂起的待处理事件
                WatchEvent<?> prev = events.get(size - 1);
                
                /* if the previous event is an OVERFLOW event or this is a repeated event then we simply increment the counter */
                // 如果prev是OVERFLOW事件，或者prev与正在处理的事件是相同事件(事件类型一致，上下文也一致)，则只需要增加计数（认为出现了重复事件）
                if((prev.kind() == StandardWatchEventKinds.OVERFLOW) || ((kind == prev.kind() && Objects.equals(context, prev.context())))) {
                    // 增加事件计数
                    ((Event<?>) prev).increment();
                    return;
                }
                
                /* 至此，说明当前正在处理的事件有效，且与最近挂起的待处理事件类型不同 */
                
                /* if this is a modify event and the last entry for the context is a modify event then we simply increment the count */
                // 如果存在挂起的ENTRY_MODIFY(更改)事件
                if(!lastModifyEvents.isEmpty()) {
                    // 如果正在处理的事件是ENTRY_MODIFY(更改)事件(但prev不是ENTRY_MODIFY事件)
                    if(isModify) {
                        // 获取context(路径)关联的监视事件(如果存在，肯定是ENTRY_MODIFY)
                        WatchEvent<?> ev = lastModifyEvents.get(context);
                        
                        // 如果可以获取到，说明context(路径)关联的ENTRY_MODIFY事件已经在挂起列表中，此时只需要简单地递增其计数
                        if(ev != null) {
                            assert ev.kind() == StandardWatchEventKinds.ENTRY_MODIFY;
                            // 递增计数，即遇到了重复事件
                            ((Event<?>) ev).increment();
                            return;
                        }
                        
                        // 如果正则处理的事件不是ENTRY_MODIFY(更改)事件
                    } else {
                        /* not a modify event so remove from the map as the last event will no longer be a modify event */
                        // 移除context(路径)关联的ENTRY_MODIFY事件，可以看做是对之前待处理的ENTRY_MODIFY事件的覆盖
                        lastModifyEvents.remove(context);
                    }
                }
                
                /* if the list has reached the limit then drop pending events and queue an OVERFLOW event */
                // 如果待处理事件的集合events已经满了，需要标记当前事件为无效（相当于丢弃）
                if(size >= MAX_EVENT_LIST_SIZE) {
                    kind = StandardWatchEventKinds.OVERFLOW;
                    isModify = false;
                    context = null;
                }
            }
            
            /* non-repeated event */
            // 构造一个全新的事件
            Event<Object> ev = new Event<>((WatchEvent.Kind<Object>) kind, context);
            
            // 如果是ENTRY_MODIFY(更改)事件
            if(isModify) {
                // 将当前事件存入挂起的待处理的ENTRY_MODIFY(更改)事件映射
                lastModifyEvents.put(context, ev);
                
                // 如果是"无效"事件
            } else if(kind == StandardWatchEventKinds.OVERFLOW) {
                /* drop all pending events */
                // 清空所有待处理事件
                events.clear();
                
                // 清空最近一次的待处理的ENTRY_MODIFY(更改)事件
                lastModifyEvents.clear();
            }
            
            // 将当前收到的事件(通知)加入到待处理事件的集合
            events.add(ev);
            
            // 将当前监视键加入到所属监视服务的阻塞队列中，并标记其状态为激活(此过程中会唤醒阻塞在出队操作上的线程)
            signal();
        }
    }
    
    /**
     * Enqueues this key to the watch service
     */
    // 将当前监视键加入到所属监视服务的阻塞队列中，并标记其状态为激活(此过程中会唤醒阻塞在出队操作上的线程)
    final void signal() {
        synchronized(this) {
            // 如果当前监视键处于就绪状态
            if(state == State.READY) {
                // 监视键转入到激活状态
                state = State.SIGNALLED;
                
                // 将指定的监视键加入到监视服务watcher的阻塞队列中，并唤醒等待从监视服务获取监视键的线程
                watcher.enqueueKey(this);
            }
        }
    }
    
    /*▲ 取出/添加事件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 重置/复用 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 当处理完一批事件后，需要重置(复用)监视键，以便子线程(工作线程)向其中填充监视到的事件，并且让主线程获取到这些事件
    @Override
    public final boolean reset() {
        synchronized(this) {
            // 如果当前监视键为激活状态，且仍然有效
            if(state == State.SIGNALLED && isValid()) {
                // 如果当前没有待处理事件，则将监视键重置为就绪状态，以便其进行复用
                if(events.isEmpty()) {
                    state = State.READY;
                    
                    // 如果当前存在待处理事件，则直接将监视键入队，并唤醒等待从监视服务获取监视键的(主)线程
                } else {
                    // pending events so re-queue key
                    watcher.enqueueKey(this);
                }
            }
            
            return isValid();
        }
    }
    
    /*▲ 重置/复用 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Return the original watchable (Path)
     */
    // 返回当前监视键关联的被监视的目录(树)
    @Override
    public Path watchable() {
        return dir;
    }
    
    // 返回当前监视键使用的监视服务(多个目录可以共用一个监视服务)
    final AbstractWatchService watcher() {
        return watcher;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Possible key states
     */
    // 监视键状态
    private enum State {
        READY,      // 就绪状态，表示当前监视键等待加入到所属监视服务的阻塞队列中
        SIGNALLED   // 激活状态，表示当前监视键已经加入到所属监视服务的阻塞队列中
    }
    
    /**
     * WatchEvent implementation
     */
    // 被监视的事件的基础实现，通常由系统底层触发，然后转交给子线程(工作线程)，或者直接由子线程(工作线程)生成
    private static class Event<T> implements WatchEvent<T> {
        private final WatchEvent.Kind<T> kind;  // 事件类型；参见StandardWatchEventKinds
        private final T context;                // 事件上下文
        
        /*
         * 事件计数
         *
         * 对于针对同一文件的ENTRY_MODIFY(更改)事件，即使没有连续发生，也会将其合并。
         * 对于其他事件，连续发生时才会被合并。
         */
        private int count;                      // synchronize on watch key to access/increment count
        
        Event(WatchEvent.Kind<T> type, T context) {
            this.kind = type;
            this.context = context;
            this.count = 1;
        }
        
        // 返回被监视事件的类型
        @Override
        public WatchEvent.Kind<T> kind() {
            return kind;
        }
        
        // 返回被监视事件的上下文(通常是触发监视事件的子文件/目录)，可能为空
        @Override
        public T context() {
            return context;
        }
        
        // 返回事件计数
        @Override
        public int count() {
            return count;
        }
        
        /** for repeated events */
        // 增加事件计数
        void increment() {
            count++;
        }
    }
    
}
