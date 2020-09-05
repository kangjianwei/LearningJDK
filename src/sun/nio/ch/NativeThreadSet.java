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

/** Special-purpose data structure for sets of native threads */
// 本地线程集，通常用于文件通道
class NativeThreadSet {
    
    private long[] elts;    // 记录(通道)正在使用(占用/阻塞)的本地线程引用(本地线程集)
    private int used = 0;   // elts中包含的元素个数
    
    // 是否正在等待其他阻塞的线程醒来
    private boolean waitingToEmpty;
    
    NativeThreadSet(int n) {
        elts = new long[n];
    }
    
    /** Adds the current native thread to this set, returning its index so that it can efficiently be removed later */
    // 向本地线程集新增一个本地线程引用，返回新线程在本地线程集中的索引，这表示通道将阻塞该线程
    int add() {
        // 返回当前通道所在的native线程引用
        long th = NativeThread.current();
        
        /* 0 and -1 are treated as placeholders, not real thread handles */
        // 0或-1被视为占位，不是实际的线程引用(对于无效的线程引用，通常在windows上返回0，在类unix平台上返回-1)
        if(th == 0) {
            th = -1;
        }
        
        synchronized(this) {
            int start = 0;
            
            // 视情形扩容
            if(used >= elts.length) {
                int on = elts.length;
                int nn = on * 2;
                long[] nelts = new long[nn];
                System.arraycopy(elts, 0, nelts, 0, on);
                elts = nelts;
                start = on;
            }
            
            for(int i = start; i<elts.length; i++) {
                // 跳过已经存在线程引用的插槽
                if(elts[i] != 0) {
                    continue;
                }
                
                // 找到空槽后，记下一个新的本地线程引用
                elts[i] = th;
                
                // 计数增一
                used++;
                
                // 返回新线程的索引
                return i;
            }
            
            assert false;
            
            return -1;
        }
    }
    
    /** Removes the thread at the given index */
    // 从本地线程集中移除指定索引处的本地线程引用，表示通道不再阻塞该线程了
    void remove(int index) {
        synchronized(this) {
            elts[index] = 0;    // 将此处的线程引用标记为0
            used--;         // 计数减一
            
            // 如果所有占用的线程已经释放，且waitingToEmpty为true，则唤醒所有阻塞的线程
            if(used == 0 && waitingToEmpty) {
                notifyAll();
            }
        }
    }
    
    /** Signals all threads in this set */
    // 唤醒当前所有被占用(阻塞)的本地线程以便关闭操作可以执行下去
    synchronized void signalAndWait() {
        boolean interrupted = false;
        
        // 如果存在正在占用(占用/阻塞)的本地线程
        while(used>0) {
            int u = used;
            int n = elts.length;
            
            for(long th : elts) {
                // 跳过空槽
                if(th == 0) {
                    continue;
                }
                
                // 唤醒阻塞的线程th
                if(th != -1) {
                    NativeThread.signal(th);
                }
                
                if(--u == 0) {
                    break;
                }
            }
            
            // 等待阻塞的线程被唤醒
            waitingToEmpty = true;
            
            try {
                // 等待50毫秒
                wait(50);
            } catch(InterruptedException e) {
                interrupted = true;
            } finally {
                // 恢复标记
                waitingToEmpty = false;
            }
        }
        
        // 如果出现了中断异常，则中断线程（只是给线程预设一个标记，不是立即让线程停下来）
        if(interrupted) {
            Thread.currentThread().interrupt();
        }
    }
    
}
