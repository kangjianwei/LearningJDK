/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * An event or a repeated event for an object that is registered with a {@link
 * WatchService}.
 *
 * <p> An event is classified by its {@link #kind() kind} and has a {@link
 * #count() count} to indicate the number of times that the event has been
 * observed. This allows for efficient representation of repeated events. The
 * {@link #context() context} method returns any context associated with
 * the event. In the case of a repeated event then the context is the same for
 * all events.
 *
 * <p> Watch events are immutable and safe for use by multiple concurrent
 * threads.
 *
 * @param <T> The type of the context object associated with the event
 *
 * @since 1.7
 */
// 被监视的事件，通常由系统底层触发，然后转交给子线程(工作线程)，或者直接由子线程(工作线程)生成
public interface WatchEvent<T> {
    
    /**
     * Returns the event kind.
     *
     * @return the event kind
     */
    // 返回被监视事件的类型；参见StandardWatchEventKinds
    Kind<T> kind();
    
    /**
     * Returns the context for the event.
     *
     * In the case of {@link StandardWatchEventKinds#ENTRY_CREATE ENTRY_CREATE}, {@link StandardWatchEventKinds#ENTRY_DELETE ENTRY_DELETE}, and {@link StandardWatchEventKinds#ENTRY_MODIFY ENTRY_MODIFY} events
     * the context is a {@code Path} that is the {@link Path#relativize relative} path between the directory registered with the watch service,
     * and the entry that is created, deleted, or modified.
     *
     * @return the event context; may be {@code null}
     */
    /*
     * 返回被监视事件的上下文，可能为空
     *
     * 对于ENTRY_CREATE/ENTRY_DELETE/ENTRY_MODIFY事件，
     * 其创建/移除/修改事件的上下文是一个相对路径，该路径是相对于被监视目录的
     */
    T context();
    
    /**
     * Returns the event count. If the event count is greater than {@code 1} then this is a repeated event.
     *
     * @return the event count
     */
    /*
     * 返回事件计数
     *
     * 对于针对同一文件的ENTRY_MODIFY(更改)事件，即使没有连续发生，也会将其合并。
     * 对于其他事件，连续发生时才会被合并。
     */
    int count();
    
    /**
     * An event kind, for the purposes of identification.
     *
     * @see StandardWatchEventKinds
     * @since 1.7
     */
    // 被监视事件的类型，通常包括ENTRY_CREATE/ENTRY_DELETE/ENTRY_MODIFY事件
    interface Kind<T> {
        
        /**
         * Returns the name of the event kind.
         *
         * @return the name of the event kind
         */
        // 被监视事件名称
        String name();
        
        /**
         * Returns the type of the {@link WatchEvent#context context} value.
         *
         * @return the type of the context value
         */
        // 被监视事件的上下文类型
        Class<T> type();
    }
    
    /**
     * An event modifier that qualifies how a {@link Watchable} is registered with a {@link WatchService}.
     *
     * <p> This release does not define any <em>standard</em> modifiers.
     *
     * @see Watchable#register
     * @since 1.7
     */
    // 监视服务的修饰符
    interface Modifier {
        /**
         * Returns the name of the modifier.
         *
         * @return the name of the modifier
         */
        // 监视事件修饰符的名称
        String name();
    }
}
