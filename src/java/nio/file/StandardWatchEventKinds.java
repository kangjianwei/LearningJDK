/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;

/**
 * Defines the <em>standard</em> event kinds.
 *
 * @since 1.7
 */
// 被监视事件类型的标准实现
public final class StandardWatchEventKinds {
    
    /**
     * A special event to indicate that events may have been lost or discarded.
     *
     * The {@link WatchEvent#context context} for this event is implementation specific and may be {@code null}.
     * The event {@link WatchEvent#count count} may be greater than {@code 1}.
     *
     * @see WatchService
     */
    /*
     * 一个特殊事件，指示事件可能已丢失或丢弃；
     *
     * 当工作线程(服务线程)准备向主线程反馈该类型的事件时，往往会忽略该事件之后反馈的一系列事件，直到该事件本身被处理。
     */
    public static final WatchEvent.Kind<Object> OVERFLOW = new StdWatchEventKind<>("OVERFLOW", Object.class);
    
    /**
     * Directory entry created.
     *
     * When a directory is registered for this event then the {@link WatchKey} is queued
     * when it is observed that an entry is created in the directory or renamed into the directory.
     * The event {@link WatchEvent#count count} for this event is always {@code 1}.
     */
    // 被监听目录中发生了新建/重命名事件，事件计数为1
    public static final WatchEvent.Kind<Path> ENTRY_CREATE = new StdWatchEventKind<>("ENTRY_CREATE", Path.class);
    
    /**
     * Directory entry deleted.
     *
     * When a directory is registered for this event then the {@link WatchKey} is queued
     * when it is observed that an entry is deleted or renamed out of the directory.
     * The event {@link WatchEvent#count count} for this event is always {@code 1}.
     */
    // 被监听目录中发生了移除/重命名事件，事件计数为1
    public static final WatchEvent.Kind<Path> ENTRY_DELETE = new StdWatchEventKind<>("ENTRY_DELETE", Path.class);
    
    /**
     * Directory entry modified.
     *
     * When a directory is registered for this event then the {@link WatchKey} is queued
     * when it is observed that an entry in the directory has been modified.
     * The event {@link WatchEvent#count count} for this event is {@code 1} or greater.
     */
    // 被监听目录中发生了更改事件，事件计数可能大于1
    public static final WatchEvent.Kind<Path> ENTRY_MODIFY = new StdWatchEventKind<>("ENTRY_MODIFY", Path.class);
    
    
    private StandardWatchEventKinds() {
    }
    
    
    // 被监视事件类型的标准实现，通常包括创建/移除/更改事件
    private static class StdWatchEventKind<T> implements WatchEvent.Kind<T> {
        private final String name;      // 被监视事件名称
        private final Class<T> type;    // 被监视事件的上下文类型
        
        StdWatchEventKind(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }
        
        @Override
        public String name() {
            return name;
        }
        
        @Override
        public Class<T> type() {
            return type;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
}
