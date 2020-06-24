/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileTreeWalker.Event;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An {@code Iterator} to iterate over the nodes of a file tree.
 *
 * <pre>{@code
 *     try (FileTreeIterator iterator = new FileTreeIterator(start, maxDepth, options)) {
 *         while (iterator.hasNext()) {
 *             Event ev = iterator.next();
 *             Path path = ev.file();
 *             BasicFileAttributes attrs = ev.attributes();
 *         }
 *     }
 * }</pre>
 */
// 文件树迭代器
class FileTreeIterator implements Iterator<Event>, Closeable {
    private final FileTreeWalker walker;    // 文件树访问器
    private Event next;                     // 遍历事件
    
    /**
     * Creates a new iterator to walk the file tree starting at the given file.
     *
     * @throws IllegalArgumentException if {@code maxDepth} is negative
     * @throws IOException              if an I/O errors occurs opening the starting file
     * @throws SecurityException        if the security manager denies access to the starting file
     * @throws NullPointerException     if {@code start} or {@code options} is {@code null} or
     *                                  the options array contains a {@code null} element
     */
    /*
     * 构造针对start的文件树迭代器
     *
     * options：对于符号链接，是否将其链接到目标文件；如果显式设置了LinkOption.NOFOLLOW_LINKS，表示不链接
     * maxDepth：最大递归层次
     */
    FileTreeIterator(Path start, int maxDepth, FileVisitOption... options) throws IOException {
        
        // 构造文件树访问器
        this.walker = new FileTreeWalker(Arrays.asList(options), maxDepth);
        
        // 访问给定的实体(文件或目录)，返回一个遍历事件以指示下一步应该如何决策
        this.next = walker.walk(start);
        
        // 遍历事件的类型必须是ENTRY或START_DIRECTORY（即要求start为可访问的文件或目录）
        assert next.type() == FileTreeWalker.EventType.ENTRY || next.type() == FileTreeWalker.EventType.START_DIRECTORY;
        
        // IOException if there a problem accessing the starting file
        IOException ioe = next.ioeException();
        if(ioe != null) {
            // 如果遍历中发生了异常，则需要抛出
            throw ioe;
        }
    }
    
    // 是否存在下一个遍历事件
    @Override
    public boolean hasNext() {
        // 如果指定的文件访问器walker已经关闭
        if(!walker.isOpen()) {
            throw new IllegalStateException();
        }
        
        // 获取下一个遍历事件，将其存储在next字段
        fetchNextIfNeeded();
        
        return next != null;
    }
    
    // 获取下一个遍历事件
    @Override
    public Event next() {
        // 如果指定的文件访问器walker已经关闭
        if(!walker.isOpen()) {
            throw new IllegalStateException();
        }
        
        // 获取下一个遍历事件，将其存储在next字段
        fetchNextIfNeeded();
        
        if(next == null) {
            throw new NoSuchElementException();
        }
        
        Event result = next;
        
        next = null;
        
        return result;
    }
    
    @Override
    public void close() {
        walker.close();
    }
    
    // 获取下一个遍历事件，将其存储在next字段
    private void fetchNextIfNeeded() {
        // 如果next字段已有值，直接返回
        if(next != null) {
            return;
        }
        
        // 返回对下一个兄弟项或子项的遍历事件。如果子项都被遍历完了，则返回top目录遍历结束的事件
        FileTreeWalker.Event ev = walker.next();
        
        while(ev != null) {
            // 如果出现异常，则直接抛出
            IOException ioe = ev.ioeException();
            if(ioe != null) {
                throw new UncheckedIOException(ioe);
            }
            
            /* END_DIRECTORY events are ignored */
            if(ev.type() != FileTreeWalker.EventType.END_DIRECTORY) {
                // 记录遍历事件
                next = ev;
                return;
            }
            
            // 如果遇到了END_DIRECTORY事件，将被忽略，需要继续遍历
            ev = walker.next();
        }
    }
}
