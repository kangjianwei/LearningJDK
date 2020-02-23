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

import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.misc.JavaLangRefAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.ref.Cleaner;
import jdk.internal.vm.annotation.ForceInline;

/**
 * Abstract base class for reference objects.  This class defines the
 * operations common to all reference objects.  Because reference objects are
 * implemented in close cooperation with the garbage collector, this class may
 * not be subclassed directly.
 *
 * @author Mark Reinhold
 * @since 1.2
 *
 *
 * The state of a Reference object is characterized by two attributes.
 * It may be either "active", "pending", or "inactive".
 * It may also be either "registered", "enqueued", "dequeued", or "unregistered".
 *
 *   Active: Subject to special treatment by the garbage collector.
 *   Some time after the collector detects that the reachability of the referent has changed to the appropriate state,
 *   the collector "notifies" the reference, changing the state to either "pending" or "inactive".
 *   referent != null; discovered = null, or in GC discovered list.
 *
 *   Pending: An element of the pending-Reference list, waiting to be processed by the ReferenceHandler thread.
 *   The pending-Reference list is linked through the discovered fields of references in the list.
 *   referent = null; discovered = next element in pending-Reference list.
 *
 *   Inactive: Neither Active nor Pending.
 *   referent = null.
 *
 *   Registered: Associated with a queue when created, and not yet added to the queue.
 *   queue = the associated queue.
 *
 *   Enqueued: Added to the associated queue, and not yet removed.
 *   queue = ReferenceQueue.ENQUEUE; next = next entry in list, or this to indicate end of list.
 *
 *   Dequeued: Added to the associated queue and then removed.
 *   queue = ReferenceQueue.NULL; next = this.
 *
 *   Unregistered: Not associated with a queue when created.
 *   queue = ReferenceQueue.NULL.
 *
 * The collector only needs to examine the referent field and the discovered field to determine whether a (non-FinalReference) Reference object needs special treatment.
 * If the referent is non-null and not known to be live, then it may need to be discovered for possible later notification.
 * But if the discovered field is non-null, then it has already been discovered.
 *
 * FinalReference (which exists to support finalization) differs from other references, because a FinalReference is not cleared when notified.
 * The referent being null or not cannot be used to distinguish between the active state and pending or inactive states.
 * However, FinalReferences do not support enqueue().  Instead, the next field of a
 * FinalReference object is set to "this" when it is added to the pending-Reference list.  The use of "this" as the value of next in the
 * enqueued and dequeued states maintains the non-active state.
 * An additional check that the next field is null is required to determine that a FinalReference object is active.
 *
 * Initial states:
 *   [active/registered]
 *   [active/unregistered] [1]
 *
 * Transitions:
 *                            clear
 *   [active/registered]     ------->   [inactive/registered]
 *          |                                 |
 *          |                                 | enqueue [2]
 *          | GC              enqueue [2]     |
 *          |                -----------------|
 *          |                                 |
 *          v                                 |
 *   [pending/registered]    ---              v
 *          |                   | ReferenceHandler
 *          | enqueue [2]       |--->   [inactive/enqueued]
 *          v                   |             |
 *   [pending/enqueued]      ---              |
 *          |                                 | poll/remove
 *          | poll/remove                     |
 *          |                                 |
 *          v            ReferenceHandler     v
 *   [pending/dequeued]      ------>    [inactive/dequeued]
 *
 *
 *                           clear/enqueue/GC [3]
 *   [active/unregistered]   ------
 *          |                      |
 *          | GC                   |
 *          |                      |--> [inactive/unregistered]
 *          v                      |
 *   [pending/unregistered]  ------
 *                           ReferenceHandler
 *
 * Terminal states:
 *   [inactive/dequeued]
 *   [inactive/unregistered]
 *
 * Unreachable states (because enqueue also clears):
 *   [active/enqeued]
 *   [active/dequeued]
 *
 * [1] Unregistered is not permitted for FinalReferences.
 * [2] These transitions are not possible for FinalReferences, making [pending/enqueued] and [pending/dequeued] unreachable, and [inactive/registered] terminal.
 * [3] The garbage collector may directly transition a Reference from [active/unregistered] to [inactive/unregistered], bypassing the pending-Reference list.
 */
/*
 * 常见的Referenc子类是SoftReference、WeakReference、PhantomReference、FinalReference。
 * 这些子类相当于给了JVM一个信号，告诉JVM它们在内存中存留的时间。
 *
 * 引用简介：
 * Strong Reference：强引用，普通的的引用类型，new一个对象默认得到的引用就是强引用，只要对象存在强引用，就不会被GC。
 * SoftReference：软引用，当一个对象只剩软引用，且堆内存不足时，垃圾回收器才会回收对应引用
 * WeakReference：弱引用，当一个对象只剩弱引用，垃圾回收器每次运行都会回收其引用
 * PhantomReference：虚引用，对引用无影响，只用于获取对象被回收的通知
 * FinalReference：Java用于实现finalization的一个内部类
 *
 * 引用类型	取得目标对象方式	 垃圾回收条件	是否可能内存泄漏
 * 强引用	直接调用	         不回收	        可能
 * 软引用	通过get()方法	 视内存情况回收	不可能
 * 弱引用	通过get()方法	 永远回收	    不可能
 * 虚引用	无法取得	         不回收/回收	    可能
 * 注：虚引用在JDK9之前不回收，JDK9之后回收
 *
 * 值得注意的是，GC只对追踪的referent对象做特殊处理
 * 对于软/弱/虚引用本身，以及子类中的其他引用，按普通的垃圾回收机制处理
 *
 * 所以，如果自定义的引用继承了弱引用或虚引用，且自主增加了额外的引用变量，
 * 那么如果没有及时释放这些引用，还是可能发生内存泄露的
 */
public abstract class Reference<T> {
    
    private static final Object processPendingLock = new Object();
    
    // 判断“报废Reference”处理线程是否正在工作
    private static boolean processPendingActive = false;
    
    /* The queue this reference gets enqueued to by GC notification or by calling enqueue().
     *
     * When registered: the queue with which this reference is registered.
     *        enqueued: ReferenceQueue.ENQUEUE
     *        dequeued: ReferenceQueue.NULL
     *    unregistered: ReferenceQueue.NULL
     */
    volatile ReferenceQueue<? super T> queue;   // "报废引用"队列，当前引用被垃圾回收之后会存于此；多个引用可以共享一个引用队列
    
    /* The link in a ReferenceQueue's list of Reference objects.
     *
     * When registered: null
     *        enqueued: next element in queue (or this if last)
     *        dequeued: this (marking FinalReferences as inactive)
     *    unregistered: null
     */
    @SuppressWarnings("rawtypes")
    volatile Reference next;
    
    /*
     * 由Reference追踪的目标对象
     * 只有该对象被GC特别对待，子类中的其他对象不会被追踪
     */
    private T target;         /* Treated specially by GC */
    
    /*
     * Used by the garbage collector to accumulate Reference objects that need to be revisited in order to decide whether they should be notified.
     * Also used as the link in the pending-Reference list.
     * The discovered field and the next field are distinct to allow the enqueue() method to be applied to a Reference object
     * while it is either in the pending-Reference list or in the garbage collector's discovered set.
     *
     * When active: null or next element in a discovered reference list maintained by the GC (or this if last)
     *     pending: next element in the pending-Reference list (null if last)
     *    inactive: null
     */
    private transient Reference<T> discovered;  // 由虚拟机设置的"报废引用"列表，什么类型的引用都有
    
    
    // 在根线程组启动一个名为Reference Handler的守护线程来处理被回收掉的引用
    static {
        // 获取当前线程所在的线程组
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
    
        // 顺着当前线程组往上遍历，找到根线程组system
        ThreadGroup tgn = tg;
        while(tgn != null) {
            tg = tgn;
            tgn = tg.getParent();
        }
    
        // 构造名称为"Reference Handler"的线程
        Thread handler = new ReferenceHandler(tg, "Reference Handler");
    
        /* If there were a special system-only priority greater than MAX_PRIORITY, it would be used here */
        // 设置为最高优先级的守护线程
        handler.setPriority(Thread.MAX_PRIORITY);
        handler.setDaemon(true);
        handler.start();
        
        // provide access in SharedSecrets
        SharedSecrets.setJavaLangRefAccess(new JavaLangRefAccess() {
            @Override
            public boolean waitForReferenceProcessing() throws InterruptedException {
                return Reference.waitForReferenceProcessing();
            }
            
            @Override
            public void runFinalization() {
                Finalizer.runFinalization();
            }
        });
    }
    
    
    /**
     * High-priority thread to enqueue pending References
     */
    /*
     * “报废Reference”处理线程，用来监测被虚拟机清理的引用，并决定是否将其加入ReferenceQueue以便回收利用，在单独的后台线程中启动
     *
     * 注：站在报废引用队列的角度观察，该线程可以被视为是生产者，不断向报废引用队列填充已经报废的引用以待后续处理
     */
    private static class ReferenceHandler extends Thread {
        
        static {
            // pre-load and initialize Cleaner class so that we don't get into trouble later in the run loop if there's memory shortage while loading/initializing it lazily.
            ensureClassInitialized(Cleaner.class);  // 确保jdk.internal.ref.Cleaner已经初始化
        }
        
        ReferenceHandler(ThreadGroup g, String name) {
            super(g, null, name, 0, false);
        }
        
        // 确保指定的类已经初始化
        private static void ensureClassInitialized(Class<?> clazz) {
            try {
                Class.forName(clazz.getName(), true, clazz.getClassLoader());
            } catch(ClassNotFoundException e) {
                throw (Error) new NoClassDefFoundError(e.getMessage()).initCause(e);
            }
        }
        
        // 常驻后台运行
        public void run() {
            // 死循环
            while(true) {
                // 处理报废的引用
                processPendingReferences();
            }
        }
    }
    
    /**
     * Wait until the VM's pending-Reference list may be non-null.
     */
    // 等待，直到到VM的pending-Reference列表可能为非null。
    private static native void waitForReferencePendingList();
    
    /**
     * Atomically get and clear (set to null) the VM's pending-Reference list.
     */
    // 以原子方式获取并清除（设置为null）VM的pending-Reference列表。
    private static native Reference<Object> getAndClearReferencePendingList();
    
    // 被“报废Reference”处理线程不断执行，从JVM获取那些报废的Reference，并将其加入相应的ReferenceQueue
    private static void processPendingReferences() {
        /*
         * Only the singleton reference processing thread calls waitForReferencePendingList() and getAndClearReferencePendingList().
         * These are separate operations to avoid a race with other threads that are calling waitForReferenceProcessing().
         */
        // 陷入等待，直到“报废Reference”列表非null时被虚拟机唤醒
        waitForReferencePendingList();
        
        Reference<Object> pendingList;
        
        synchronized(processPendingLock) {
            // 获取一个清单，该清单里列出了刚刚被回收的引用（“报废Reference”）
            pendingList = getAndClearReferencePendingList();
            processPendingActive = true;
        }
        
        while(pendingList != null) {
            Reference<Object> ref = pendingList;
            pendingList = ref.discovered;
            ref.discovered = null;
    
            // 如果是特殊的虚引用：Cleaner，则需要执行该清理器的清理方法
            if(ref instanceof Cleaner) {
                // 对清理器追踪对象进行清理
                ((Cleaner) ref).clean();
    
                /*
                 * Notify any waiters that progress has been made.
                 * This improves latency for nio.Bits waiters, which are the only important ones.
                 */
                synchronized(processPendingLock) {
                    processPendingLock.notifyAll();
                }
            } else {
                ReferenceQueue<? super Object> q = ref.queue;
                if(q != ReferenceQueue.NULL) {
                    // 将“报废Reference”加入各自内部持有的引用队列中
                    q.enqueue(ref);
                }
            }
        }
    
        // Notify any waiters of completion of current round.
        synchronized(processPendingLock) {
            processPendingActive = false;
            processPendingLock.notifyAll();
        }
    }
    
    
    // 没有关联ReferenceQueue，意味着用户只需要特殊的引用类型，不关心对象何时被GC
    Reference(T target) {
        this(target, null);
    }
    
    // 传入自定义引用referent和ReferenceQueue，当reference被回收后，会添加到queue中
    Reference(T target, ReferenceQueue<? super T> queue) {
        this.target = target;
        this.queue = (queue == null) ? ReferenceQueue.NULL : queue;
    }
    
    /**
     * Returns this reference object's referent.
     * If this reference object has been cleared, either by the program or by the garbage collector, then this method returns <code>null</code>.
     *
     * @return The object to which this reference refers, or <code>null</code> if this reference object has been cleared
     */
    // 返回此Reference包裹的自定义引用对象，如果该对象已被回收，则返回null
    @HotSpotIntrinsicCandidate
    public T get() {
        return this.target;
    }
    
    /**
     * Clears this reference object.
     * Invoking this method will not cause this object to be enqueued.
     *
     * This method is invoked only by Java code; when the garbage collector clears references it does so directly, without invoking this method.
     */
    // 取消对目标对象的追踪
    public void clear() {
        this.target = null;
    }
    
    /**
     * Tells whether or not this reference object has been enqueued, either by the program or by the garbage collector.
     * If this reference object was not registered with a queue when it was created, then this method will always return <code>false</code>.
     *
     * @return <code>true</code> if and only if this reference object has been enqueued
     */
    // 判断当前Reference是否在ReferenceQueue中
    public boolean isEnqueued() {
        return (this.queue == ReferenceQueue.ENQUEUED);
    }
    
    /**
     * Clears this reference object and adds it to the queue with which it is registered, if any.
     *
     * <p> This method is invoked only by Java code; when the garbage collector enqueues references it does so directly, without invoking this method.
     *
     * @return <code>true</code> if this reference object was successfully enqueued;
     *         <code>false</code> if it was already enqueued or if it was not registered with a queue when it was created
     */
    // 取消对目标对象的追踪，并将当前报废的Reference入队，在这个过程中，会回收目标对象
    public boolean enqueue() {
        this.target = null;
        return this.queue.enqueue(this);
    }
    
    /**
     * Ensures that the object referenced by the given reference remains
     * <a href="package-summary.html#reachability"><em>strongly reachable</em></a>,
     * regardless of any prior actions of the program that might otherwise cause
     * the object to become unreachable; thus, the referenced object is not
     * reclaimable by garbage collection at least until after the invocation of
     * this method.  Invocation of this method does not itself initiate garbage
     * collection or finalization.
     *
     * <p> This method establishes an ordering for
     * <a href="package-summary.html#reachability"><em>strong reachability</em></a>
     * with respect to garbage collection.  It controls relations that are
     * otherwise only implicit in a program -- the reachability conditions
     * triggering garbage collection.  This method is designed for use in
     * uncommon situations of premature finalization where using
     * {@code synchronized} blocks or methods, or using other synchronization
     * facilities are not possible or do not provide the desired control.  This
     * method is applicable only when reclamation may have visible effects,
     * which is possible for objects with finalizers (See
     * <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-12.html#jls-12.6">
     * Section 12.6 17 of <cite>The Java&trade; Language Specification</cite></a>)
     * that are implemented in ways that rely on ordering control for correctness.
     *
     * @param ref the reference. If {@code null}, this method has no effect.
     *
     * @apiNote Finalization may occur whenever the virtual machine detects that no
     * reference to an object will ever be stored in the heap: The garbage
     * collector may reclaim an object even if the fields of that object are
     * still in use, so long as the object has otherwise become unreachable.
     * This may have surprising and undesirable effects in cases such as the
     * following example in which the bookkeeping associated with a class is
     * managed through array indices.  Here, method {@code action} uses a
     * {@code reachabilityFence} to ensure that the {@code Resource} object is
     * not reclaimed before bookkeeping on an associated
     * {@code ExternalResource} has been performed; in particular here, to
     * ensure that the array slot holding the {@code ExternalResource} is not
     * nulled out in method {@link Object#finalize}, which may otherwise run
     * concurrently.
     *
     * <pre> {@code
     * class Resource {
     *   private static ExternalResource[] externalResourceArray = ...
     *
     *   int myIndex;
     *   Resource(...) {
     *     myIndex = ...
     *     externalResourceArray[myIndex] = ...;
     *     ...
     *   }
     *   protected void finalize() {
     *     externalResourceArray[myIndex] = null;
     *     ...
     *   }
     *   public void action() {
     *     try {
     *       // ...
     *       int i = myIndex;
     *       Resource.update(externalResourceArray[i]);
     *     } finally {
     *       Reference.reachabilityFence(this);
     *     }
     *   }
     *   private static void update(ExternalResource ext) {
     *     ext.status = ...;
     *   }
     * }}</pre>
     *
     * Here, the invocation of {@code reachabilityFence} is nonintuitively
     * placed <em>after</em> the call to {@code update}, to ensure that the
     * array slot is not nulled out by {@link Object#finalize} before the
     * update, even if the call to {@code action} was the last use of this
     * object.  This might be the case if, for example a usage in a user program
     * had the form {@code new Resource().action();} which retains no other
     * reference to this {@code Resource}.  While probably overkill here,
     * {@code reachabilityFence} is placed in a {@code finally} block to ensure
     * that it is invoked across all paths in the method.  In a method with more
     * complex control paths, you might need further precautions to ensure that
     * {@code reachabilityFence} is encountered along all of them.
     *
     * <p> It is sometimes possible to better encapsulate use of
     * {@code reachabilityFence}.  Continuing the above example, if it were
     * acceptable for the call to method {@code update} to proceed even if the
     * finalizer had already executed (nulling out slot), then you could
     * localize use of {@code reachabilityFence}:
     *
     * <pre> {@code
     * public void action2() {
     *   // ...
     *   Resource.update(getExternalResource());
     * }
     * private ExternalResource getExternalResource() {
     *   ExternalResource ext = externalResourceArray[myIndex];
     *   Reference.reachabilityFence(this);
     *   return ext;
     * }}</pre>
     *
     * <p> Method {@code reachabilityFence} is not required in constructions
     * that themselves ensure reachability.  For example, because objects that
     * are locked cannot, in general, be reclaimed, it would suffice if all
     * accesses of the object, in all methods of class {@code Resource}
     * (including {@code finalize}) were enclosed in {@code synchronized (this)}
     * blocks.  (Further, such blocks must not include infinite loops, or
     * themselves be unreachable, which fall into the corner case exceptions to
     * the "in general" disclaimer.)  However, method {@code reachabilityFence}
     * remains a better option in cases where this approach is not as efficient,
     * desirable, or possible; for example because it would encounter deadlock.
     * @since 9
     */
    @ForceInline
    public static void reachabilityFence(Object ref) {
        // Does nothing. This method is annotated with @ForceInline to eliminate
        // most of the overhead that using @DontInline would cause with the
        // HotSpot JVM, when this fence is used in a wide variety of situations.
        // HotSpot JVM retains the ref and does not GC it before a call to
        // this method, because the JIT-compilers do not have GC-only safepoints.
    }
    
    /**
     * Test whether the VM's pending-Reference list contains any entries.
     */
    // 判断虚拟机的pending-Reference中是否包含更多"报废引用"
    private static native boolean hasReferencePendingList();
    
    /**
     * Wait for progress in reference processing.
     * Returns true after waiting
     * (for notification from the reference processing thread) if either
     * (1) the VM has any pending references, or
     * (2) the reference processing thread is processing references.
     * Otherwise, returns false immediately.
     */
    // 等待"报废引用"处理线程暂停工作，且虚拟机内没有待处理的"报废引用"时，返回false
    private static boolean waitForReferenceProcessing() throws InterruptedException {
        synchronized(processPendingLock) {
            /*
             * 如果"报废引用"处理线程正在工作，或者虚拟机中已积累了待处理的"报废引用"，
             * 则需要陷入阻塞，直到本次清理完成之后，或者下次清理结束之后，才会唤醒该方法的调用者，并返回true。
             * 如果将该方法放入一个死循环中，可以达到的效果是：
             * 直到虚拟机没有待处理的"报废引用"，且当前"报废引用"处理线程已经暂停工作，该方法才返回false
             */
            if(processPendingActive || hasReferencePendingList()) {
                // Wait for progress, not necessarily completion.
                processPendingLock.wait();
                return true;
            } else {
                return false;
            }
        }
    }
    
    /**
     * Throws {@link CloneNotSupportedException}. A {@code Reference} cannot be
     * meaningfully cloned. Construct a new {@code Reference} instead.
     *
     * @throws CloneNotSupportedException always
     * @returns never returns normally
     * @since 11
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
}
