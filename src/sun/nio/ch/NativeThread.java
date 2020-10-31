/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Socket通道本地线程操作
 *
 * 在一些系统平台上(如linux或mac)上，当某个可阻塞的Socket通道被阻塞时，其所在的native线程无法被另一个线程关闭。
 * 比如A线程阻塞在Socket通道的读操作上，但是B线程却要关闭该Socket通道，此时必须先等A线程结束该阻塞操作。
 * 等待A线程结束其阻塞操作后，会唤醒休眠的B线程，以完成后续的关闭过程。
 */
class NativeThread {
    
    /*
     * 返回当前通道所在的native线程引用
     *
     * 在windows平台上总是返回0，表示可以随意关闭此线程
     */
    static long current() {
        // return 0 to ensure that async close of blocking sockets will close the underlying socket.
        return 0;
    }
    
    /*
     * 唤醒阻塞的nt线程
     *
     * 在windows平台上此方法无任何效果
     */
    static void signal(long nt) {
    }
    
}
