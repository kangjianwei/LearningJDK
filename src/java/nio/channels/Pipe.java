/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * A pair of channels that implements a unidirectional pipe.
 *
 * <p> A pipe consists of a pair of channels: A writable {@link
 * Pipe.SinkChannel sink} channel and a readable {@link Pipe.SourceChannel source}
 * channel.  Once some bytes are written to the sink channel they can be read
 * from the source channel in exactly the order in which they were written.
 *
 * <p> Whether or not a thread writing bytes to a pipe will block until another
 * thread reads those bytes, or some previously-written bytes, from the pipe is
 * system-dependent and therefore unspecified.  Many pipe implementations will
 * buffer up to a certain number of bytes between the sink and source channels,
 * but such buffering should not be assumed.  </p>
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */
/*
 * 单向同步管道，用在通道选择器中
 *
 * 该管道由写通道SinkChannel和读通道SourceChannel组成，
 * 可以从写通道写入数据，从读通道读取数据，并且内部的读写通道是连通的
 */
public abstract class Pipe {
    
    /**
     * Initializes a new instance of this class.
     */
    protected Pipe() {
    }
    
    /**
     * Opens a pipe.
     *
     * <p> The new pipe is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openPipe openPipe} method of the
     * system-wide default {@link java.nio.channels.spi.SelectorProvider}
     * object.  </p>
     *
     * @return A new pipe
     *
     * @throws IOException If an I/O error occurs
     */
    // 构造一个Pipe对象
    public static Pipe open() throws IOException {
        return SelectorProvider.provider().openPipe();
    }
    
    /**
     * Returns this pipe's sink channel.
     *
     * @return This pipe's sink channel
     */
    // 返回管道中的写通道，可以向这里写入数据
    public abstract SinkChannel sink();
    
    /**
     * Returns this pipe's source channel.
     *
     * @return This pipe's source channel
     */
    // 返回管道中的读通道，可以从这里读取数据
    public abstract SourceChannel source();
    
    /**
     * A channel representing the writable end of a {@link Pipe}.
     *
     * @since 1.4
     */
    // 管道中的写通道，向这里写入数据
    public abstract static class SinkChannel extends AbstractSelectableChannel implements WritableByteChannel, GatheringByteChannel {
        
        /**
         * Initializes a new instance of this class.
         *
         * @param provider The selector provider
         */
        protected SinkChannel(SelectorProvider provider) {
            super(provider);
        }
        
        /**
         * Returns an operation set identifying this channel's supported
         * operations.
         *
         * <p> Pipe-sink channels only support writing, so this method returns
         * {@link SelectionKey#OP_WRITE}.  </p>
         *
         * @return The valid-operation set
         */
        // 返回当前通道允许的有效操作参数集
        public final int validOps() {
            return SelectionKey.OP_WRITE;
        }
        
    }
    
    /**
     * A channel representing the readable end of a {@link Pipe}.
     *
     * @since 1.4
     */
    // 管道中的读通道，从这里读取数据
    public abstract static class SourceChannel extends AbstractSelectableChannel implements ReadableByteChannel, ScatteringByteChannel {
        
        /**
         * Constructs a new instance of this class.
         *
         * @param provider The selector provider
         */
        protected SourceChannel(SelectorProvider provider) {
            super(provider);
        }
        
        /**
         * Returns an operation set identifying this channel's supported
         * operations.
         *
         * <p> Pipe-source channels only support reading, so this method
         * returns {@link SelectionKey#OP_READ}.  </p>
         *
         * @return The valid-operation set
         */
        // 返回当前通道允许的有效操作参数集
        public final int validOps() {
            return SelectionKey.OP_READ;
        }
        
    }
    
}
