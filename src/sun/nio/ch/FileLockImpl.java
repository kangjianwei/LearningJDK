/*
 * Copyright (c) 2001, 2009, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

// 文件锁的实现类，实际起作用的是内核中的文件锁
public class FileLockImpl extends FileLock {
    
    // 判断当前文件锁是否失效
    private volatile boolean invalid;
    
    // 构造作用于同步文件通道的文件锁
    FileLockImpl(FileChannel channel, long position, long size, boolean shared) {
        super(channel, position, size, shared);
    }
    
    // 构造作用于异步文件通道的文件锁
    FileLockImpl(AsynchronousFileChannel channel, long position, long size, boolean shared) {
        super(channel, position, size, shared);
    }
    
    // 判断当前的文件锁是否有效
    public boolean isValid() {
        return !invalid;
    }
    
    // 标记当前的文件锁失效
    void invalidate() {
        assert Thread.holdsLock(this);
        invalid = true;
    }
    
    // 释放文件锁，并将其标记为失效
    public synchronized void release() throws IOException {
        // 获取被锁定文件的通道
        Channel ch = acquiredBy();
        // 如果通道已经关闭了，直接抛异常
        if(!ch.isOpen()) {
            throw new ClosedChannelException();
        }
        
        // 如果当前文件锁已经失效，直接返回
        if(!isValid()) {
            return;
        }
        
        if(ch instanceof FileChannelImpl) {
            ((FileChannelImpl) ch).release(this);
        } else if(ch instanceof AsynchronousFileChannelImpl) {
            ((AsynchronousFileChannelImpl) ch).release(this);
        } else {
            throw new AssertionError();
        }
        
        // 标记当前文件锁为失效
        invalidate();
    }
    
}
