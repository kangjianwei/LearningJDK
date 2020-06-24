/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import sun.nio.fs.BasicFileAttributesHolder;

/**
 * Walks a file tree, generating a sequence of events corresponding to the files
 * in the tree.
 *
 * <pre>{@code
 *     Path top = ...
 *     Set<FileVisitOption> options = ...
 *     int maxDepth = ...
 *
 *     try (FileTreeWalker walker = new FileTreeWalker(options, maxDepth)) {
 *         FileTreeWalker.Event ev = walker.walk(top);
 *         do {
 *             process(ev);
 *             ev = walker.next();
 *         } while (ev != null);
 *     }
 * }</pre>
 *
 * @see Files#walkFileTree
 */
// 文件树访问器
class FileTreeWalker implements Closeable {
    private final boolean followLinks;      // 是否追踪(解析)符号链接
    private final LinkOption[] linkOptions; // 符号链接选项，与followLinks值是配套的
    
    // 最大递归深度
    private final int maxDepth;
    
    // 目录栈，栈顶目录是正在被遍历的目录
    private final ArrayDeque<DirectoryNode> stack = new ArrayDeque<>();
    
    // 指示文件访问器是否关闭
    private boolean closed;
    
    /**
     * Creates a {@code FileTreeWalker}.
     *
     * @throws IllegalArgumentException if {@code maxDepth} is negative
     * @throws ClassCastException       if {@code options} contains an element that is not a
     *                                  {@code FileVisitOption}
     * @throws NullPointerException     if {@code options} is {@code null} or the options
     *                                  array contains a {@code null} element
     */
    /*
     * 构造一个文件树访问器
     *
     * options：对于符号链接，是否将其链接到目标文件；如果显式设置了LinkOption.NOFOLLOW_LINKS，表示不链接
     * maxDepth：最大递归层次
     */
    FileTreeWalker(Collection<FileVisitOption> options, int maxDepth) {
        boolean fl = false;
        
        for(FileVisitOption option : options) {
            
            // will throw NPE if options contains null
            if(option != FileVisitOption.FOLLOW_LINKS) {
                // 如果存在遍历属性，则必须是FOLLOW_LINKS，否则抛异常
                throw new AssertionError("Should not get here");
            }
            
            fl = true;
        }
        
        if(maxDepth<0) {
            throw new IllegalArgumentException("'maxDepth' is negative");
        }
        
        this.followLinks = fl;
        
        // 从FileVisitOption转换为LinkOption
        this.linkOptions = fl ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
        
        this.maxDepth = maxDepth;
    }
    
    /**
     * Closes/pops all directories on the stack.
     */
    // 清空目录栈，关闭文件树访问器
    @Override
    public void close() {
        if(closed) {
            return;
        }
        
        // 清空目录栈
        while(!stack.isEmpty()) {
            // 将位于目录栈栈顶的目录结点出栈，并关闭其对应的目录流
            pop();
        }
        
        closed = true;
    }
    
    /**
     * Start walking from the given file.
     */
    // 访问给定的实体(文件或目录)，返回一个遍历事件以指示下一步应该如何决策
    Event walk(Path entry) {
        if(closed) {
            throw new IllegalStateException("Closed");
        }
        
        Event ev = visit(entry, false, false);
        assert ev != null;  // 如果返回null，说明遇到了安全异常，且忽略了它
        
        return ev;
    }
    
    /**
     * Returns the next Event or {@code null} if there are no more events or
     * the walker is closed.
     */
    // 返回对下一个兄弟项或子项的遍历事件。如果子项都被遍历完了，则返回top目录遍历结束的事件
    Event next() {
        // 查看栈顶的目录结点
        DirectoryNode top = stack.peek();
        // stack is empty, we are done
        if(top == null) {
            // 目录栈为空时，遍历结束
            return null;
        }
    
        // continue iteration of the directory at the top of the stack
        Event ev;
        do {
            Path entry = null;
            IOException ioe = null;
        
            /* get next entry in the directory */
            // 如果不能跳过top目录内的子项，则继续遍历它。否则，到下面关闭流
            if(!top.skipped()) {
                // 获取作用在top上的目录迭代器
                Iterator<Path> iterator = top.iterator();
            
                try {
                    // 是否存在下一个子项(忽略了"."和".."，且经过了目录流过滤器的筛选)
                    if(iterator.hasNext()) {
                        // 获取下一个子项
                        entry = iterator.next();
                    }
                } catch(DirectoryIteratorException x) {
                    ioe = x.getCause();
                }
            }
        
            /* no next entry so close and pop directory, creating corresponding event */
            // 如果已经没有合适的子项，说明top目录已经遍历完了，其对应的目录流也该关闭了
            if(entry == null) {
                try {
                    // 关闭top目录流
                    top.stream().close();
                } catch(IOException e) {
                    // 如果之前没有异常发生，则设置异常信息为当前异常
                    if(ioe == null) {
                        ioe = e;
                    
                        // 如果之前已经有了异常，则抑制当前发生的异常
                    } else {
                        ioe.addSuppressed(e);
                    }
                }
            
                // top目录已经遍历完，可以将其对应的目录结点从目录栈中移除了
                stack.pop();
            
                // 遍历事件：结束了对指定目录的遍历
                return new Event(EventType.END_DIRECTORY, top.directory(), ioe);
            }
        
            // 继续访问给定的实体(文件或目录)，返回一个遍历事件以指示下一步应该如何决策
            ev = visit(entry, true, true);
        
            // 如果ev==null，说明发生了安全异常，但该异常被忽略了，此时继续遍历目录中的其他子项(相当于放弃了当前遍历到的子项)
        } while(ev == null);
    
        return ev;
    }
    
    /**
     * Pops the directory node that is the current top of the stack so that
     * there are no more events for the directory (including no END_DIRECTORY)
     * event. This method is a no-op if the stack is empty or the walker is
     * closed.
     */
    // 将位于目录栈栈顶的目录结点出栈，并关闭其对应的目录流
    void pop() {
        // 如果目录栈为空，则直接返回
        if(stack.isEmpty()) {
            return;
        }
    
        // 目录结点出栈的同时，关闭其对应的目录流
        DirectoryNode node = stack.pop();
        try {
            node.stream().close();
        } catch(IOException ignore) {
        }
    }
    
    /**
     * Skips the remaining entries in the directory at the top of the stack.
     * This method is a no-op if the stack is empty or the walker is closed.
     */
    // 跳过其他兄弟项
    void skipRemainingSiblings() {
        // 如果目录栈为空，则直接返回
        if(stack.isEmpty()) {
            return;
        }
        
        // 将栈顶的目录结点标记为跳过，即以后遇到该目录中的其他子项均会忽略
        stack.peek().skip();
    }
    
    /**
     * Returns {@code true} if the walker is open.
     */
    // 判断当前文件访问器是否处于开启状态
    boolean isOpen() {
        return !closed;
    }
    
    /**
     * Visits the given file, returning the {@code Event} corresponding to that visit.
     *
     * The {@code ignoreSecurityException} parameter determines whether any SecurityException should be ignored or not.
     * If a SecurityException is thrown, and is ignored, then this method returns {@code null} to mean that
     * there is no event corresponding to a visit to the file.
     *
     * The {@code canUseCached} parameter determines whether cached attributes for the file can be used or not.
     */
    /*
     * 访问给定的实体(文件或目录)，返回一个遍历事件以指示下一步应该如何决策
     *
     * ignoreSecurityException：是否忽略安全异常。如果忽略，则遇到安全异常时，返回null，否则，安全异常会被抛出
     * canUseCached：是否允许使用文件属性缓存
     */
    private Event visit(Path entry, boolean ignoreSecurityException, boolean canUseCached) {
        
        // need the file attributes
        BasicFileAttributes attrs;
        try {
            // 返回指定路径标识的文件的基础文件属性
            attrs = getAttributes(entry, canUseCached);
        } catch(IOException ioe) {
            // 遍历事件：发生了异常
            return new Event(EventType.ENTRY, entry, ioe);
        } catch(SecurityException se) {
            if(ignoreSecurityException) {
                return null;
            }
            
            throw se;
        }
        
        /* at maximum depth or file is not a directory */
        // 获取当前目录栈的深度
        int depth = stack.size();
        
        // 如果递归层次达到上限，或者该实体不是目录(排除符号链接)，则停止继续递归遍历
        if(depth >= maxDepth || !attrs.isDirectory()) {
            // 遍历事件：遇到了不可遍历的实体
            return new Event(EventType.ENTRY, entry, attrs);
        }
        
        // 判断当前目录是否已访问过，如果遇到了已访问过的目录，说明此时出现了死循环
        boolean wouldLoop = wouldLoop(entry, attrs.fileKey());
        
        /* check for cycles when following links */
        /*
         * 对于符号链接，如果需要将其链接到目标文件，则需要考虑死循环的问题
         * 注：符号链接与快捷方式类似，但快捷方式不会导致遍历时的死循环
         */
        if(followLinks && wouldLoop) {
            // 遍历事件：发生了异常(死循环了)
            return new Event(EventType.ENTRY, entry, new FileSystemLoopException(entry.toString()));
        }
        
        /* file is a directory, attempt to open it */
        DirectoryStream<Path> stream = null;
        try {
            // 获取entry对应的目录流（不会过滤任何子项）
            stream = Files.newDirectoryStream(entry);
        } catch(IOException ioe) {
            // 遍历事件：发生了异常
            return new Event(EventType.ENTRY, entry, ioe);
        } catch(SecurityException se) {
            if(ignoreSecurityException) {
                return null;
            }
            throw se;
        }
        
        // 遇到了目录时，需要创建目录结点
        DirectoryNode directoryNode = new DirectoryNode(entry, attrs.fileKey(), stream);
        
        /* push a directory node to the stack and return an event */
        // 目录结点入栈
        stack.push(directoryNode);
        
        // 遍历事件：遇到了目录
        return new Event(EventType.START_DIRECTORY, entry, attrs);
    }
    
    /**
     * Returns the attributes of the given file, taking into account whether
     * the walk is following sym links is not. The {@code canUseCached}
     * argument determines whether this method can use cached attributes.
     */
    /*
     * 返回指定路径标识的文件的基础文件属性
     *
     * 当file为持有基础文件属性的holder时(属于BasicFileAttributesHolder的实例)，且不存在安全管理器时，
     * 则canUseCached指示是否从file的缓存(即持有的基础文件属性)中获取文件属性。
     */
    private BasicFileAttributes getAttributes(Path file, boolean canUseCached) throws IOException {
        
        /* if attributes are cached then use them if possible */
        if(canUseCached && (file instanceof BasicFileAttributesHolder) && (System.getSecurityManager() == null)) {
            // 获取缓存的"basic"文件属性
            BasicFileAttributes cached = ((BasicFileAttributesHolder) file).get();
            
            // 如果缓存不为空，且不需要链接到目标文件或者cached本身不是符号链接，则直接使用缓存
            if(cached != null && (!followLinks || !cached.isSymbolicLink())) {
                return cached;
            }
        }
        
        /*
         * attempt to get attributes of file.
         * If fails and we are following links then a link target might not exist so get attributes of link
         */
        BasicFileAttributes attrs;
        try {
            // 获取指定路径标识的文件的基础文件属性
            attrs = Files.readAttributes(file, BasicFileAttributes.class, linkOptions);
        } catch(IOException ioe) {
            // 如果不需要链接到目标文件，直接把异常抛出
            if(!followLinks) {
                throw ioe;
            }
            
            /* attempt to get attrmptes without following links */
            // 回退为不链接目标文件，重新获取文件属性
            attrs = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }
        
        return attrs;
    }
    
    /**
     * Returns true if walking into the given directory would result in a
     * file system loop/cycle.
     */
    // 判断当前目录是否已访问过，如果遇到了已访问过的目录，说明此时出现了死循环
    private boolean wouldLoop(Path dir, Object key) {
        /*
         * if this directory and ancestor has a file key then we compare them;
         * otherwise we use less efficient isSameFile test.
         */
        // 遍历目录栈
        for(DirectoryNode ancestor : stack) {
            
            // 获取文件标识
            Object ancestorKey = ancestor.key();
            
            // 如果存在文件标识，则用文件标识比对
            if(key != null && ancestorKey != null) {
                if(key.equals(ancestorKey)) {
                    // cycle detected
                    return true;
                }
            } else {
                try {
                    // 如果两个目录是同一目录
                    if(Files.isSameFile(dir, ancestor.directory())) {
                        // cycle detected
                        return true;
                    }
                } catch(IOException | SecurityException x) {
                    // ignore
                }
            }
        }
        
        return false;
    }
    
    /**
     * The event types.
     */
    // 遍历事件类型
    enum EventType {
        /**
         * Start of a directory
         */
        START_DIRECTORY,    // 遇到了目录
        
        /**
         * End of a directory
         */
        END_DIRECTORY,      // 结束了对指定目录的遍历
        
        /**
         * An entry in a directory
         */
        ENTRY               // 遇到了不可遍历的实体：遇到异常，递归层次达到上限，遇到文件而不是目录
    }
    
    /**
     * Events returned by the {@link #walk} and {@link #next} methods.
     */
    // 遍历事件
    static class Event {
        private final EventType type;               // 事件类型
        private final Path file;                    // 实体路径
        private final BasicFileAttributes attrs;    // 实体属性
        private final IOException ioe;              // 遍历过程中出现的异常
        
        Event(EventType type, Path file, BasicFileAttributes attrs) {
            this(type, file, attrs, null);
        }
        
        Event(EventType type, Path file, IOException ioe) {
            this(type, file, null, ioe);
        }
        
        private Event(EventType type, Path file, BasicFileAttributes attrs, IOException ioe) {
            this.type = type;
            this.file = file;
            this.attrs = attrs;
            this.ioe = ioe;
        }
        
        EventType type() {
            return type;
        }
        
        Path file() {
            return file;
        }
        
        BasicFileAttributes attributes() {
            return attrs;
        }
        
        IOException ioeException() {
            return ioe;
        }
    }
    
    /**
     * The element on the walking stack corresponding to a directory node.
     */
    // 目录结点，会存入目录栈
    private static class DirectoryNode {
        private final Path dir;                     // 目录路径
        private final Object key;                   // 唯一标识给定文件的对象。如果文件标识不可用(例如windows上)，则为null
        private final DirectoryStream<Path> stream; // 目录流
        private final Iterator<Path> iterator;      // 目录迭代器，用来遍历目录内的子项
        private boolean skipped;                    // 是否忽略该目录的子项（从设置为true的那刻生效）
        
        DirectoryNode(Path dir, Object key, DirectoryStream<Path> stream) {
            this.dir = dir;
            this.key = key;
            this.stream = stream;
            this.iterator = stream.iterator();
        }
        
        Path directory() {
            return dir;
        }
        
        Object key() {
            return key;
        }
        
        DirectoryStream<Path> stream() {
            return stream;
        }
        
        Iterator<Path> iterator() {
            return iterator;
        }
        
        void skip() {
            skipped = true;
        }
        
        boolean skipped() {
            return skipped;
        }
    }
}
