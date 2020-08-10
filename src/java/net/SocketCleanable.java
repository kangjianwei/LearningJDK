/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner;
import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.ref.CleanerFactory;
import jdk.internal.ref.PhantomCleanable;

/**
 * Cleanable for a socket/datagramsocket FileDescriptor when it becomes phantom reachable.
 * Create a cleanup if the raw fd != -1. Windows closes sockets using the fd.
 * Subclassed from {@code PhantomCleanable} so that {@code clear} can be called to disable the cleanup
 * when the socket fd is closed by any means other than calling {FileDescriptor#close()}.
 * Otherwise, it might incorrectly close the handle or fd after it has been reused.
 */
/*
 *    Reference
 *        │
 * PhantomReference   Cleanable
 *        └──────┬────────┘
 *        PhantomCleanable
 *               │
 *        SocketCleanable
 *
 * Socket（虚引用）清理器，此类的属性既是虚引用，又是清理器
 */
final class SocketCleanable extends PhantomCleanable<FileDescriptor> {
    // Access to FileDescriptor private fields
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();
    
    // Socket文件的文件描述符编号
    private final int fd;
    
    /**
     * Constructor for a phantom cleanable reference.
     *
     * @param obj     the object to monitor
     * @param cleaner the cleaner
     * @param fd      file descriptor to close
     */
    private SocketCleanable(FileDescriptor obj, Cleaner cleaner, int fd) {
        super(obj, cleaner);
        this.fd = fd;
    }
    
    /**
     * Register a socket specific Cleanable with the FileDescriptor
     * if the FileDescriptor is non-null and the raw fd is != -1.
     *
     * @param fdo the FileDescriptor; may be null
     */
    // 注册Socket的文件描述符到清理器
    static void register(FileDescriptor fdo) {
        if(fdo != null && fdo.valid()) {
            int fd = fdAccess.get(fdo);
            fdAccess.registerCleanup(fdo, new SocketCleanable(fdo, CleanerFactory.cleaner(), fd));
        }
    }
    
    /**
     * Unregister a Cleanable from the FileDescriptor.
     *
     * @param fdo the FileDescriptor; may be null
     */
    // 取消注册
    static void unregister(FileDescriptor fdo) {
        if(fdo != null) {
            fdAccess.unregisterCleanup(fdo);
        }
    }
    
    /**
     * Close the native handle or fd.
     */
    // 关闭本地的fd。CleanerImpl.run()==>清理器clean()-->清理器performCleanup()
    @Override
    protected void performCleanup() {
        try {
            cleanupClose0(fd);
        } catch(IOException ioe) {
            throw new UncheckedIOException("close", ioe);
        }
    }
    
    /** Native function to call NET_SocketClose(fd) Used only for last chance cleanup */
    private static native void cleanupClose0(int fd) throws IOException;
    
}
