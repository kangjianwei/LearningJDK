/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.ref;

import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.VM;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Package-private; must be in same package as the Reference class
 */
/*
 * 主要用于清理操作，在对象回收前执行资源释放。存在性能问题，JDK 9之后废弃。
 *
 * 如果一个类实现了finalize()方法，那么每次创建该类对象的时候，都会多创建一个Finalizer对象(指向刚刚新建的对象)。
 *
 * 如果类没有实现finalize()方法，那么不会创建额外的Finalizer对象，进行垃圾回收的时候，可以直接从堆内存中释放该对象。
 *
 * 如果某个类实现了finalize方法，进行GC的时候，如果发现某个对象外部引用为null，即只被java.lang.ref.Finalizer对象引用时，
 * 那么追踪此对象的Finalizer引用对象会被加入到Finalizer类的引用队列（F-Queue）中，并唤醒FinalizerThread线程。
 * FinalizerThread会将F-Queue中的Finalizer引用取出，并将其从unfinalized链表中删除。
 * 随后，从取出的Finalizer引用中获取追踪的对象，执行其finalize()方法之后，取消对其的追踪，并等下次gc时释放被追踪对象的内存。
 * 这个过程是JVM在GC的时候自动完成的。
 *
 * 由以上描述可知，含有finalize()的对象从内存中释放，至少需要两次GC。
 *
 * 第一次GC, 检测到对象只有被Finalizer引用，将这个对象放入Finalizer内部的ReferenceQueue。此时，因为Finalizer的引用，对象还无法被GC。
 * 接下来，FinalizerThread会不停地取出队列中的对象，执行其清理操作（调用finalize方法）。
 * 清理操作结束后，取消对该对象的追踪，此时该对象没有任何引用，于是其所占内存会在下一次GC到来时被回收。
 *
 * 使用finalize容易导致OOM，因为如果创建对象的速度很快，那么Finalizer线程的回收速度赶不上创建速度，就会导致内存超载。
 */
final class Finalizer extends FinalReference<Object> {
    
    /** Lock guarding access to unfinalized list. */
    private static final Object lock = new Object();
    
    // 所有Finalizer共享的引用队列
    private static ReferenceQueue<Object> queue = new ReferenceQueue<>();
    
    /** Head of doubly linked list of Finalizers awaiting finalization. */
    private static Finalizer unfinalized = null;    // 存储所有注册的Finalizer，会不断剔除已报废的引用
    
    private Finalizer next, prev;
    
    
    // 在根线程组启动一个守护线程FinalizerThread
    static {
        // 获取当前线程所在的线程组
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        
        // 顺着当前线程组往上遍历，找到根线程组system
        ThreadGroup tgn = tg;
        while(tgn != null) {
            tg = tgn;
            tgn = tg.getParent();
        }
        
        // 构造FinalizerThread线程
        Thread finalizer = new FinalizerThread(tg);
        
        // 设置为较高优先级的守护线程
        finalizer.setPriority(Thread.MAX_PRIORITY - 2);
        finalizer.setDaemon(true);
        finalizer.start();
    }
    
    
    // Finalizer内部维护了一个unfinalized链表，每次新建的Finalizer对象都会插入(头插法)到该链表中
    private Finalizer(Object finalizee) {
        super(finalizee, queue);
        
        // push onto unfinalized
        synchronized(lock) {
            if(unfinalized != null) {
                this.next = unfinalized;
                unfinalized.prev = this;
            }
            
            unfinalized = this;
        }
    }
    
    
    /* Invoked by VM */
    // 由虚拟机调用，注册Finalizer的过程，就是添加一个新的Finalizer到内部的双向链表
    static void register(Object finalizee) {
        new Finalizer(finalizee);
    }
    
    /** Called by Runtime.runFinalization() */
    // 手动触发Finalizer的清理操作，不用等待FinalizerThread
    static void runFinalization() {
        if(VM.initLevel() == 0) {
            return;
        }
        
        forkSecondaryFinalizer(new Runnable() {
            private volatile boolean running;
            
            public void run() {
                // in case of recursive call to run()
                if(running) {
                    return;
                }
                
                final JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
                
                running = true;
                
                // 遍历所有已报废的Finalizer引用，并执行其清理操作
                for(Finalizer f; (f = (Finalizer) queue.poll()) != null; ) {
                    f.runFinalizer(jla);
                }
            }
        });
    }
    
    /**
     * Create a privileged secondary finalizer thread in the system thread
     * group for the given Runnable, and wait for it to complete.
     *
     * This method is used by runFinalization.
     *
     * It could have been implemented by offloading the work to the
     * regular finalizer thread and waiting for that thread to finish.
     * The advantage of creating a fresh thread, however, is that it insulates
     * invokers of that method from a stalled or deadlocked finalizer thread.
     */
    private static void forkSecondaryFinalizer(final Runnable proc) {
        AccessController.doPrivileged(new PrivilegedAction<>() {
            public Void run() {
                ThreadGroup tg = Thread.currentThread().getThreadGroup();
                ThreadGroup tgn = tg;
                while(tgn != null) {
                    tg = tgn;
                    tgn = tg.getParent();
                }
    
                Thread sft = new Thread(tg, proc, "Secondary finalizer", 0, false);
                sft.start();
    
                try {
                    sft.join();
                } catch(InterruptedException x) {
                    Thread.currentThread().interrupt();
                }
    
                return null;
            }
        });
    }
    
    /*
     * 清理操作，通常由FinalizerThread轮询执行，也可以通过runFinalization()手动触发
     *
     * 处理当前已经报废的Finalizer引用，执行清理操作：
     * 1.将当前Finalizer对象从unfinalized中移除
     * 2.调用（被追踪的对象的）finalize()方法
     * 3.移除对被追踪对象的引用
     *
     * 这样，在下一次GC时，就可以彻底释放掉无关的对象
     */
    private void runFinalizer(JavaLangAccess jla) {
    
        // 首先将待处理的Finalizer从unfinalized中摘下
        synchronized(lock) {
            if(this.next == this) {
                return; // already finalized
            }
        
            // unlink from unfinalized
            if(unfinalized == this) {
                unfinalized = this.next;
            } else {
                this.prev.next = this.next;
            }
        
            if(this.next != null) {
                this.next.prev = this.prev;
            }
        
            this.prev = null;
            this.next = this;           // mark as finalized
        }
    
        try {
            Object finalizee = this.get();
        
            if(finalizee != null && !(finalizee instanceof java.lang.Enum)) {
                // 执行该对象的finalize()操作
                jla.invokeFinalize(finalizee);
            
                // Clear stack slot containing this variable, to decrease the chances of false retention with a conservative GC
                finalizee = null;
            }
        } catch(Throwable ignored) {
        }
    
        // 取消对目标对象的追踪，以便下次GC时释放该对象所占内容
        super.clear();
    }
    
    
    // 返回引用队列
    static ReferenceQueue<Object> getQueue() {
        return queue;
    }
    
    
    /*
     * FinalizerThread是JVM内部的守护线程。
     * 这个线程会轮询Finalizer队列中的新增对象。
     * 一旦发现队列中出现了新的对象，它会移除该对象，并调用它的finalize()方法。
     * 等到下次GC再执行的时候，这个Finalizer实例以及它引用的那个对象就可以回垃圾回收掉了。
     */
    private static class FinalizerThread extends Thread {
        private volatile boolean running;
        
        FinalizerThread(ThreadGroup g) {
            super(g, null, "Finalizer", 0, false);
        }
        
        public void run() {
            // in case of recursive call to run()
            if(running) {
                return; // 避免递归调用
            }
            
            /*
             * Finalizer thread starts before System.initializeSystemClass is called.
             * Wait until JavaLangAccess is available
             */
            while(VM.initLevel() == 0) {
                // delay until VM completes initialization
                try {
                    // 等待虚拟机第一阶段的初始化完成
                    VM.awaitInitLevel(1);
                } catch(InterruptedException x) {
                    // ignore and continue
                }
            }
            
            final JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
            
            running = true;
            
            // 消费报废的Finalizer引用
            while(true) {
                try {
                    /*
                     * 不断从queue中去取Finalizer类型的reference，然后执行runFinalizer进行清理/释放操作。
                     * 如果queue里没有引用，就陷入阻塞，直到队列里有引用时才被唤醒
                     */
                    Finalizer f = (Finalizer) queue.remove();
                    f.runFinalizer(jla);
                } catch(InterruptedException x) {
                    // ignore and continue
                }
            }
        }
    }
    
}
