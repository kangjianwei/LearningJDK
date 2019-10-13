/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea, Bill Scherer, and Michael Scott with
 * assistance from members of JCP JSR-166 Expert Group and released to
 * the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@linkplain BlockingQueue blocking queue} in which each insert
 * operation must wait for a corresponding remove operation by another
 * thread, and vice versa.  A synchronous queue does not have any
 * internal capacity, not even a capacity of one.  You cannot
 * {@code peek} at a synchronous queue because an element is only
 * present when you try to remove it; you cannot insert an element
 * (using any method) unless another thread is trying to remove it;
 * you cannot iterate as there is nothing to iterate.  The
 * <em>head</em> of the queue is the element that the first queued
 * inserting thread is trying to add to the queue; if there is no such
 * queued thread then no element is available for removal and
 * {@code poll()} will return {@code null}.  For purposes of other
 * {@code Collection} methods (for example {@code contains}), a
 * {@code SynchronousQueue} acts as an empty collection.  This queue
 * does not permit {@code null} elements.
 *
 * <p>Synchronous queues are similar to rendezvous channels used in
 * CSP and Ada. They are well suited for handoff designs, in which an
 * object running in one thread must sync up with an object running
 * in another thread in order to hand it some information, event, or
 * task.
 *
 * <p>This class supports an optional fairness policy for ordering
 * waiting producer and consumer threads.  By default, this ordering
 * is not guaranteed. However, a queue constructed with fairness set
 * to {@code true} grants threads access in FIFO order.
 *
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Collection} and {@link Iterator} interfaces.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea and Bill Scherer and Michael Scott
 * @param <E> the type of elements held in this queue
 */
/*
 * 链式有界（不能无限存储）单向阻塞队列，线程安全（CAS）
 *
 * SynchronousQueue的侧重点是存储【操作】，换个角度讲是【交换】数据，而不是存储【数据】，
 * 【同类操作】到达时，它们会被存储在该队列中，
 * 【互补操作】到来时，它们之间先是“传递”/“交换”数据，随后，两个【操作】互相抵消。
 *
 * 使用SynchronousQueue的时候，如果一个线程的put()操作找不到互补的take()操作时，它将陷入阻塞，
 * 这一点上与LinkedTransferQueue不同。
 */
public class SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
    
    /*
     * This class implements extensions of the dual stack and dual
     * queue algorithms described in "Nonblocking Concurrent Objects
     * with Condition Synchronization", by W. N. Scherer III and
     * M. L. Scott.  18th Annual Conf. on Distributed Computing,
     * Oct. 2004 (see also
     * http://www.cs.rochester.edu/u/scott/synchronization/pseudocode/duals.html).
     * The (Lifo) stack is used for non-fair mode, and the (Fifo)
     * queue for fair mode. The performance of the two is generally
     * similar. Fifo usually supports higher throughput under
     * contention but Lifo maintains higher thread locality in common
     * applications.
     *
     * A dual queue (and similarly stack) is one that at any given
     * time either holds "data" -- items provided by put operations,
     * or "requests" -- slots representing take operations, or is
     * empty. A call to "fulfill" (i.e., a call requesting an item
     * from a queue holding data or vice versa) dequeues a
     * complementary node.  The most interesting feature of these
     * queues is that any operation can figure out which mode the
     * queue is in, and act accordingly without needing locks.
     *
     * Both the queue and stack extend abstract class Transferer
     * defining the single method transfer that does a put or a
     * take. These are unified into a single method because in dual
     * data structures, the put and take operations are symmetrical,
     * so nearly all code can be combined. The resulting transfer
     * methods are on the long side, but are easier to follow than
     * they would be if broken up into nearly-duplicated parts.
     *
     * The queue and stack data structures share many conceptual
     * similarities but very few concrete details. For simplicity,
     * they are kept distinct so that they can later evolve
     * separately.
     *
     * The algorithms here differ from the versions in the above paper
     * in extending them for use in synchronous queues, as well as
     * dealing with cancellation. The main differences include:
     *
     *  1. The original algorithms used bit-marked pointers, but
     *     the ones here use mode bits in nodes, leading to a number
     *     of further adaptations.
     *  2. SynchronousQueues must block threads waiting to become
     *     fulfilled.
     *  3. Support for cancellation via timeout and interrupts,
     *     including cleaning out cancelled nodes/threads
     *     from lists to avoid garbage retention and memory depletion.
     *
     * Blocking is mainly accomplished using LockSupport park/unpark,
     * except that nodes that appear to be the next ones to become
     * fulfilled first spin a bit (on multiprocessors only). On very
     * busy synchronous queues, spinning can dramatically improve
     * throughput. And on less busy ones, the amount of spinning is
     * small enough not to be noticeable.
     *
     * Cleaning is done in different ways in queues vs stacks.  For
     * queues, we can almost always remove a node immediately in O(1)
     * time (modulo retries for consistency checks) when it is
     * cancelled. But if it may be pinned as the current tail, it must
     * wait until some subsequent cancellation. For stacks, we need a
     * potentially O(n) traversal to be sure that we can remove the
     * node, but this can run concurrently with other threads
     * accessing the stack.
     *
     * While garbage collection takes care of most node reclamation
     * issues that otherwise complicate nonblocking algorithms, care
     * is taken to "forget" references to data, other nodes, and
     * threads that might be held on to long-term by blocked
     * threads. In cases where setting to null would otherwise
     * conflict with main algorithms, this is done by changing a
     * node's link to now point to the node itself. This doesn't arise
     * much for Stack nodes (because blocked threads do not hang on to
     * old head pointers), but references in Queue nodes must be
     * aggressively forgotten to avoid reachability of everything any
     * node has ever referred to since arrival.
     */
    
    /**
     * The number of times to spin before blocking in timed waits.
     * The value is empirically derived -- it works well across a
     * variety of processors and OSes. Empirically, the best value
     * seems not to vary with number of CPUs (beyond 2) so is just
     * a constant.
     */
    // 带有超时标记时的自旋次数
    static final int MAX_TIMED_SPINS = (Runtime.getRuntime().availableProcessors()<2) ? 0 : 32;
    
    /**
     * The number of times to spin before blocking in untimed waits.
     * This is greater than timed value because untimed waits spin
     * faster since they don't need to check times on each spin.
     */
    // 不带有超时标记时的自旋次数
    static final int MAX_UNTIMED_SPINS = MAX_TIMED_SPINS * 16;
    
    /**
     * The number of nanoseconds for which it is faster to spin rather than to use timed park. A rough estimate suffices.
     */
    // 进入阻塞的最小时间阙值，当剩余阻塞时间小于这个值时，不再进入阻塞，而是使用自旋
    static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000L;
    
    /**
     * The transferer. Set only in constructor, but cannot be declared
     * as final without further complicating serialization.  Since
     * this is accessed only at most once per public method, there
     * isn't a noticeable performance penalty for using volatile
     * instead of final here.
     */
    // 传递者
    private transient volatile Transferer<E> transferer;
    
    
    static {
        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code SynchronousQueue} with nonfair access policy.
     */
    // 默认构造"非公平模式"的同步阻塞队列
    public SynchronousQueue() {
        this(false);
    }
    
    /**
     * Creates a {@code SynchronousQueue} with the specified fairness policy.
     *
     * @param fair if true, waiting threads contend in FIFO order for
     *             access; otherwise the order is unspecified.
     */
    // 构造同步阻塞队列，可在参数中设置"公平模式"或"非公平模式"
    public SynchronousQueue(boolean fair) {
        transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 入队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Inserts the specified element into this queue, if another thread is
     * waiting to receive it.
     *
     * @param e the element to add
     *
     * @return {@code true} if the element was added to this queue, else
     * {@code false}
     *
     * @throws NullPointerException if the specified element is null
     */
    // 入队，线程安全，没有互补操作/结点时不阻塞，直接返回false
    public boolean offer(E e) {
        if(e == null) {
            throw new NullPointerException();
        }
        
        // 需要等待超时，但超时时间为0
        return transferer.transfer(e, true, 0) != null;
    }
    
    /**
     * Inserts the specified element into this queue, waiting if necessary
     * up to the specified wait time for another thread to receive it.
     *
     * @return {@code true} if successful, or {@code false} if the
     * specified waiting time elapses before a consumer appears
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    // 入队，线程安全，没有互补操作/结点时阻塞一段时间，如果在指定的时间内没有成功传递元素，则返回false
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if(e == null) {
            throw new NullPointerException();
        }
        
        // 需要等待超时
        if(transferer.transfer(e, true, unit.toNanos(timeout)) != null) {
            return true;
        }
        
        if(!Thread.interrupted()) {
            return false;
        }
        
        throw new InterruptedException();
    }
    
    
    /**
     * Adds the specified element to this queue,
     * waiting if necessary for another thread to receive it.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    // 入队，线程安全，没有互补操作/结点时，线程被阻塞
    public void put(E e) throws InterruptedException {
        if(e == null) {
            throw new NullPointerException();
        }
        
        // 无需等待超时
        if(transferer.transfer(e, false, 0) == null) {
            // 清除线程的中断状态
            Thread.interrupted();
            throw new InterruptedException();
        }
    }
    
    /*▲ 入队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 出队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves and removes the head of this queue, if another thread
     * is currently making an element available.
     *
     * @return the head of this queue, or {@code null} if no
     * element is available
     */
    // 出队，线程安全，没有互补操作/结点时不阻塞，直接返回null
    public E poll() {
        // 需要等待超时，但超时时间为0
        return transferer.transfer(null, true, 0);
    }
    
    /**
     * Retrieves and removes the head of this queue, waiting
     * if necessary up to the specified wait time, for another thread
     * to insert it.
     *
     * @return the head of this queue, or {@code null} if the
     * specified waiting time elapses before an element is present
     *
     * @throws InterruptedException {@inheritDoc}
     */
    // 出队，线程安全，没有互补操作/结点时阻塞一段时间，如果在指定的时间内没有成功传递元素，则返回null
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        // 需要等待超时
        E e = transferer.transfer(null, true, unit.toNanos(timeout));
        
        if(e != null || !Thread.interrupted()) {
            return e;
        }
        
        throw new InterruptedException();
    }
    
    
    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * for another thread to insert it.
     *
     * @return the head of this queue
     *
     * @throws InterruptedException {@inheritDoc}
     */
    // 出队，线程安全，没有互补操作/结点时，线程被阻塞
    public E take() throws InterruptedException {
        // 无需等待超时
        E e = transferer.transfer(null, false, 0);
        
        if(e != null) {
            return e;
        }
        
        Thread.interrupted();
        
        throw new InterruptedException();
    }
    
    
    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param o the element to remove
     *
     * @return {@code false}
     */
    // 总是返回false，该结构不用于存储数据，所以没有可被移除的数据
    public boolean remove(Object o) {
        return false;
    }
    
    
    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param c the collection
     *
     * @return {@code false}
     */
    // 总是返回false，该结构不用于存储数据，所以没有可被移除的数据
    public boolean removeAll(Collection<?> c) {
        return false;
    }
    
    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param c the collection
     *
     * @return {@code false}
     */
    // 总是返回false，该结构不用于存储数据，所以没有可保留的数据
    public boolean retainAll(Collection<?> c) {
        return false;
    }
    
    
    /**
     * Does nothing.
     * A {@code SynchronousQueue} has no internal capacity.
     */
    // 清空，这里是空实现，该结构不用于存储数据，所以无需清理
    public void clear() {
    }
    
    
    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    // 取出之前所有阻塞的"存"操作中的数据，存入给定的容器
    public int drainTo(Collection<? super E> c) {
        Objects.requireNonNull(c);
        if(c == this) {
            throw new IllegalArgumentException();
        }
        int n = 0;
        for(E e; (e = poll()) != null; n++) {
            c.add(e);
        }
        return n;
    }
    
    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    // 取出之前阻塞的"存"操作中的数据，存入给定的容器，最多取maxElements个数据
    public int drainTo(Collection<? super E> c, int maxElements) {
        Objects.requireNonNull(c);
        if(c == this) {
            throw new IllegalArgumentException();
        }
        int n = 0;
        for(E e; n<maxElements && (e = poll()) != null; n++) {
            c.add(e);
        }
        return n;
    }
    
    /*▲ 出队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Always returns {@code null}.
     * A {@code SynchronousQueue} does not return elements
     * unless actively waited on.
     *
     * @return {@code null}
     */
    // 总是返回null，代表该结构不用于保存数据
    public E peek() {
        return null;
    }
    
    /*▲ 取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包含查询 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param o the element
     *
     * @return {@code false}
     */
    // 总是返回false，代表该结构不用于保存数据
    public boolean contains(Object o) {
        return false;
    }
    
    /**
     * Returns {@code false} unless the given collection is empty.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param c the collection
     *
     * @return {@code false} unless given collection is empty
     */
    // 返回值表示给定的容器是否为null，与同步阻塞队列自身的结构无关
    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a zero-length array.
     *
     * @return a zero-length array
     */
    // 返回一个无容量的数组
    public Object[] toArray() {
        return new Object[0];
    }
    
    /**
     * Sets the zeroth element of the specified array to {@code null}
     * (if the array has non-zero length) and returns it.
     *
     * @param a the array
     *
     * @return the specified array
     *
     * @throws NullPointerException if the specified array is null
     */
    // 返回只包含一个null元素的数组
    public <T> T[] toArray(T[] a) {
        if(a.length>0) {
            a[0] = null;
        }
        
        return a;
    }
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 迭代 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an empty iterator in which {@code hasNext} always returns
     * {@code false}.
     *
     * @return an empty iterator
     */
    // 返回一个空的迭代器
    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }
    
    /**
     * Returns an empty spliterator in which calls to
     * {@link Spliterator#trySplit() trySplit} always return {@code null}.
     *
     * @return an empty spliterator
     *
     * @since 1.8
     */
    // 返回一个空的迭代器
    public Spliterator<E> spliterator() {
        return Spliterators.emptySpliterator();
    }
    
    /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Always returns zero.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @return zero
     */
    // 总是返回0，因为该结构不用于保存数据
    public int size() {
        return 0;
    }
    
    /**
     * Always returns zero.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @return zero
     */
    // 总是返回0，因为该结构不用于保存数据
    public int remainingCapacity() {
        return 0;
    }
    
    /**
     * Always returns {@code true}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @return {@code true}
     */
    // 总是返回true，因为该结构不用于保存数据
    public boolean isEmpty() {
        return true;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * To cope with serialization strategy in the 1.5 version of
     * SynchronousQueue, we declare some unused classes and fields
     * that exist solely to enable serializability across versions.
     * These fields are never used, so are initialized only if this
     * object is ever serialized or deserialized.
     */
    
    private static final long serialVersionUID = -3223113410248163686L;
    
    private ReentrantLock qlock;
    private WaitQueue waitingProducers;
    private WaitQueue waitingConsumers;
    
    /**
     * Saves this queue to a stream (that is, serializes it).
     *
     * @param s the stream
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    private void writeObject(ObjectOutputStream s) throws java.io.IOException {
        boolean fair = transferer instanceof TransferQueue;
        if(fair) {
            qlock = new ReentrantLock(true);
            waitingProducers = new FifoWaitQueue();
            waitingConsumers = new FifoWaitQueue();
        } else {
            qlock = new ReentrantLock();
            waitingProducers = new LifoWaitQueue();
            waitingConsumers = new LifoWaitQueue();
        }
        s.defaultWriteObject();
    }
    
    /**
     * Reconstitutes this queue from a stream (that is, deserializes it).
     *
     * @param s the stream
     *
     * @throws ClassNotFoundException if the class of a serialized object
     *                                could not be found
     * @throws java.io.IOException    if an I/O error occurs
     */
    private void readObject(ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        if(waitingProducers instanceof FifoWaitQueue)
            transferer = new TransferQueue<E>();
        else
            transferer = new TransferStack<E>();
    }
    
    @SuppressWarnings("serial")
    static class WaitQueue implements Serializable {
    }
    
    static class LifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3633113410248163686L;
    }
    
    static class FifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3623113410248163686L;
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Always returns {@code "[]"}.
     *
     * @return {@code "[]"}
     */
    public String toString() {
        return "[]";
    }
    
    
    
    /**
     * Shared internal API for dual stacks and queues.
     */
    // 数据"传递"接口，用于不同类型操作之间传递数据
    abstract static class Transferer<E> {
        /**
         * Performs a put or take.
         *
         * @param e if non-null, the item to be handed to a consumer;
         *          if null, requests that transfer return an item
         *          offered by producer.
         * @param timed if this operation should timeout
         * @param nanos the timeout, in nanoseconds
         * @return if non-null, the item provided or received; if null,
         *         the operation failed due to timeout or interrupt --
         *         the caller can distinguish which of these occurred
         *         by checking Thread.interrupted.
         */
        // "传递"数据，匹配/抵消操作
        abstract E transfer(E e, boolean timed, long nanos);
    }
    
    /** Dual Queue */
    /*
     * "公平模式"的同步阻塞队列的实现
     *
     * 使用链队来模拟，这意味着先来的操作先被执行
     * 注：该结构会在队头之前设置一个空结点，以标记队列为null
     */
    static final class TransferQueue<E> extends Transferer<E> {
        /*
         * This extends Scherer-Scott dual queue algorithm, differing,
         * among other ways, by using modes within nodes rather than
         * marked pointers. The algorithm is a little simpler than
         * that for stacks because fulfillers do not need explicit
         * nodes, and matching is done by CAS'ing QNode.item field
         * from non-null to null (for put) or vice versa (for take).
         */
        
        /** Head of queue */
        // 队头
        transient volatile QNode head;
        
        /** Tail of queue */
        // 队尾
        transient volatile QNode tail;
        
        /**
         * Reference to a cancelled node that might not yet have been
         * unlinked from queue because it was the last inserted node
         * when it was cancelled.
         */
        transient volatile QNode cleanMe;
        
        // VarHandle mechanics
        private static final VarHandle QHEAD;
        private static final VarHandle QTAIL;
        private static final VarHandle QCLEANME;
        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                QHEAD = l.findVarHandle(TransferQueue.class, "head", QNode.class);
                QTAIL = l.findVarHandle(TransferQueue.class, "tail", QNode.class);
                QCLEANME = l.findVarHandle(TransferQueue.class, "cleanMe", QNode.class);
            } catch(ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        
        TransferQueue() {
            QNode h = new QNode(null, false); // initialize to dummy node.
            head = h;
            tail = h;
        }
        
        /**
         * Puts or takes an item.
         */
        // "传递"数据，匹配/抵消操作
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {
            /* Basic algorithm is to loop trying to take either of two actions:
             *
             * 1. If queue apparently empty or holding same-mode nodes,
             *    try to add node to queue of waiters, wait to be
             *    fulfilled (or cancelled) and return matching item.
             *
             * 2. If queue apparently contains waiting items, and this
             *    call is of complementary mode, try to fulfill by CAS'ing
             *    item field of waiting node and dequeuing it, and then
             *    returning matching item.
             *
             * In each case, along the way, check for and try to help
             * advance head and tail on behalf of other stalled/slow
             * threads.
             *
             * The loop starts off with a null check guarding against
             * seeing uninitialized head or tail values. This never
             * happens in current SynchronousQueue, but could if
             * callers held non-volatile/final ref to the
             * transferer. The check is here anyway because it places
             * null checks at top of loop, which is usually faster
             * than having them implicitly interspersed.
             */
            
            QNode s = null; // constructed/reused as needed
            
            // 当前结点是否为数据结点。当遇到"存"操作时，该结点成为数据结点
            boolean isData = (e != null);
            
            for(; ; ) {
                // 每次进入循环都要重置h和t
                QNode t = tail;
                QNode h = head;
                
                // saw uninitialized value
                if(t == null || h == null) {
                    // spin
                    continue;
                }
                
                // 队列为空，或者队尾结点与当前结点的数据模式一致
                if(h == t || t.isData == isData) { // empty or same-mode
                    // 指向队尾之后的结点
                    QNode tn = t.next;
                    
                    // 队尾已被别的线程修改
                    if(t != tail) {                 // inconsistent read
                        continue;
                    }
                    
                    // 队尾处于滞后状态，则先更新队尾
                    if(tn != null) {               // lagging tail
                        // 原子地将队尾更新为tn
                        advanceTail(t, tn);
                        continue;
                    }
                    
                    // 需要等待，但超时了，不必再等
                    if(timed && nanos<=0L) {       // can't wait
                        return null;
                    }
                    
                    // 首次到达这里，构建新结点
                    if(s == null) {
                        s = new QNode(e, isData);
                    }
                    
                    // 队尾没有滞后时，原子地更新队尾的next域为s
                    if(!t.casNext(null, s)) {       // failed to link in
                        continue;
                    }
                    
                    // 原子地将队尾更新为s
                    advanceTail(t, s);              // swing tail and wait
                    
                    // 尝试阻塞当前线程（操作），直到匹配的操作到来后唤醒它，返回数据域
                    Object x = awaitFulfill(s, e, timed, nanos);
                    
                    // 如果结点已经被标记为取消
                    if(x == s) {                   // wait was cancelled
                        clean(t, s);
                        return null;
                    }
                    
                    // 如果s.next!=s，即s仍在队列中
                    if(!s.isOffList()) {           // not already unlinked
                        advanceHead(t, s);          // unlink if head
                        
                        // 遗忘数据域，跟取消的效果一样
                        if(x != null) {             // and forget fields
                            s.item = s;
                        }
                        
                        s.waiter = null;
                    }
                    
                    return (x != null) ? (E) x : e;
                    
                } else {                            // complementary-mode
                    
                    /*
                     * 至此：h!=t && t.isData!=isData
                     * 说明队列不为null，且队尾结点与当前结点的数据模式不一致
                     * 这时候该进行匹配操作了（进入互补模式）
                     */
                    
                    // 取出队头结点
                    QNode m = h.next;               // node to fulfill
                    // 确保队列不为null，且头尾结点未被修改
                    if(t != tail || m == null || h != head) {
                        continue;                   // inconsistent read
                    }
                    
                    // 取出队头数据（这里可能出现线程争用）
                    Object x = m.item;
                    if(isData == (x != null) ||    // m already fulfilled（该结点已被其它线程处理了）
                        x == m ||                  // m cancelled（该结点已经被取消了）
                        !m.casItem(x, e)) {        // lost CAS（更新数据域，以便awaitFulfill()方法被唤醒后退出）
                        
                        /* 线程争用失败的线程到这里 */
                        
                        /*
                         * 至此，说明队头结点已经被处理/取消，则可以跳过它了
                         * （也可能是线程争用失败了，此时起到加速作用）
                         */
                        advanceHead(h, m);         // dequeue and retry
                        continue;
                    }
                    
                    /* 线程争用成功的线程到这里 */
                    
                    // 原子地将队头更新为m
                    advanceHead(h, m);              // successfully fulfilled
                    
                    // 唤醒阻塞的操作/结点/线程
                    LockSupport.unpark(m.waiter);
                    
                    // 返回数据
                    return (x != null) ? (E) x : e;
                }
            }
        }
        
        /**
         * Spins/blocks until node s is fulfilled.
         *
         * @param s     the waiting node
         * @param e     the comparison value for checking match
         * @param timed true if timed wait
         * @param nanos timeout value
         *
         * @return matched item, or s if cancelled
         */
        // 尝试阻塞当前线程（操作），直到匹配的操作到来后唤醒它，返回数据域
        Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {
            /* Same idea as TransferStack.awaitFulfill */
            // 计算截止时间
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            
            Thread w = Thread.currentThread();
            
            // 设置自旋次数
            int spins = (head.next == s) ? (timed ? MAX_TIMED_SPINS : MAX_UNTIMED_SPINS) : 0;
            
            for(; ; ) {
                // 如果当前线程带有中断标记，则取消操作
                if(w.isInterrupted()) {
                    s.tryCancel(e);
                }
                
                // 操作已经被取消，或者该结点已被处理（数据域会发生变化）
                Object x = s.item;
                if(x != e) {
                    return x;
                }
                
                // 如果需要等待
                if(timed) {
                    // 计算等待时长
                    nanos = deadline - System.nanoTime();
                    
                    // 如果等待超时，则取消该操作
                    if(nanos<=0L) {
                        s.tryCancel(e);
                        continue;
                    }
                }
                
                // 如果需要自旋，自旋次数减一
                if(spins>0) {
                    --spins;
                    // 如果需要自旋，则进入"忙等待"状态
                    Thread.onSpinWait();
                } else if(s.waiter == null) {
                    // 记录将要被阻塞线程
                    s.waiter = w;
                } else if(!timed) {
                    // 阻塞当前线程，并用当前对象本身作为阻塞标记
                    LockSupport.park(this);
                } else if(nanos>SPIN_FOR_TIMEOUT_THRESHOLD) {
                    // 带超时的阻塞
                    LockSupport.parkNanos(this, nanos);
                }
            }
        }
        
        /**
         * Tries to cas nh as new head; if successful, unlink
         * old head's next node to avoid garbage retention.
         */
        // 原子地将队头更新为nh，并且旧的队头结点的next域将指向自身
        void advanceHead(QNode h, QNode nh) {
            if(h == head && QHEAD.compareAndSet(this, h, nh)) {
                // 使队头结点彻底脱离队列，辅助GC
                h.next = h; // forget old next
            }
        }
        
        /**
         * Tries to cas nt as new tail.
         */
        // 原子地将队尾更新为nt
        void advanceTail(QNode t, QNode nt) {
            if(tail == t) {
                QTAIL.compareAndSet(this, t, nt);
            }
        }
        
        /**
         * Tries to CAS cleanMe slot.
         */
        // 原子地更新cleanMe域为val
        boolean casCleanMe(QNode cmp, QNode val) {
            return cleanMe == cmp && QCLEANME.compareAndSet(this, cmp, val);
        }
        
        /**
         * Gets rid of cancelled node s with original predecessor pred.
         */
        // 借助原始前驱pred去除被取消的结点s
        void clean(QNode pred, QNode s) {
            s.waiter = null; // forget thread
            
            /*
             * At any given time, exactly one node on list cannot be
             * deleted -- the last inserted node. To accommodate this,
             * if we cannot delete s, we save its predecessor as
             * "cleanMe", deleting the previously saved version
             * first. At least one of node s or the node previously
             * saved can always be deleted, so this always terminates.
             */
            while(pred.next == s) { // Return early if already unlinked
                QNode h = head;
                QNode hn = h.next;   // Absorb cancelled first node as head
                if(hn != null && hn.isCancelled()) {
                    advanceHead(h, hn);
                    continue;
                }
                
                QNode t = tail;      // Ensure consistent read for tail
                if(t == h) {
                    return;
                }
                
                QNode tn = t.next;
                if(t != tail) {
                    continue;
                }
                
                if(tn != null) {
                    advanceTail(t, tn);
                    continue;
                }
                
                if(s != t) {        // If not tail, try to unsplice
                    QNode sn = s.next;
                    if(sn == s || pred.casNext(s, sn)) {
                        return;
                    }
                }
                
                QNode dp = cleanMe;
                if(dp != null) {    // Try unlinking previous cancelled node
                    QNode d = dp.next;
                    QNode dn;
                    if(d == null ||                     // d is gone or
                        d == dp ||                      // d is off list or
                        !d.isCancelled() ||             // d not cancelled or
                        (d != t &&                      // d not tail and
                            (dn = d.next) != null &&    //   has successor
                            dn != d &&                  //   that is on list
                            dp.casNext(d, dn))) {       // d unspliced
                        casCleanMe(dp, null);
                    }
                    
                    if(dp == pred) {
                        return;      // s is already saved node
                    }
                } else if(casCleanMe(null, pred)) {
                    return;          // Postpone cleaning s
                }
            }
        }
        
        /** Node class for TransferQueue. */
        // 链队结点，用于"公平模式"的同步阻塞队列
        static final class QNode {
            
            // 指向下一个结点（朝着队尾方向）
            volatile QNode next;          // next node in queue
            
            // 当前结点的数据模式，遇到"存"操作时，isData==null
            final boolean isData;
            
            // 数据域（生产者存数据，消费者存null。取消操作时指向自身）
            volatile Object item;         // CAS'ed to or from null
            
            // 被阻塞的操作/线程/结点
            volatile Thread waiter;       // to control park/unpark
            
            // VarHandle mechanics
            private static final VarHandle QITEM;
            private static final VarHandle QNEXT;
            static {
                try {
                    MethodHandles.Lookup l = MethodHandles.lookup();
                    QITEM = l.findVarHandle(QNode.class, "item", Object.class);
                    QNEXT = l.findVarHandle(QNode.class, "next", QNode.class);
                } catch(ReflectiveOperationException e) {
                    throw new ExceptionInInitializerError(e);
                }
            }
            
            QNode(Object item, boolean isData) {
                this.item = item;
                this.isData = isData;
            }
            
            // 原子地更新当前结点的next域为val
            boolean casNext(QNode cmp, QNode val) {
                return next == cmp && QNEXT.compareAndSet(this, cmp, val);
            }
            
            // 原子地更新当前结点的数据域为val
            boolean casItem(Object cmp, Object val) {
                return item == cmp && QITEM.compareAndSet(this, cmp, val);
            }
            
            /**
             * Tries to cancel by CAS'ing ref to this as item.
             */
            // 取消操作：原子地更新item为当前结点自身
            void tryCancel(Object cmp) {
                QITEM.compareAndSet(this, cmp, this);
            }
            
            // 当前操作/结点/线程是否已被取消
            boolean isCancelled() {
                return item == this;
            }
            
            /**
             * Returns true if this node is known to be off the queue
             * because its next pointer has been forgotten due to
             * an advanceHead operation.
             */
            // 判断s是否脱离了队列，由调用advanceHead()方法导致
            boolean isOffList() {
                return next == this;
            }
        }
    }
    
    /** Dual stack */
    /*
     * "非公平模式"的同步阻塞队列的实现
     * 使用链栈来模拟，这意味着先来的操作不一定先执行
     * 注意：这里的实现中，栈顶游标会指向一个有效的结点（操作）
     */
    static final class TransferStack<E> extends Transferer<E> {
        /*
         * This extends Scherer-Scott dual stack algorithm, differing,
         * among other ways, by using "covering" nodes rather than
         * bit-marked pointers: Fulfilling operations push on marker
         * nodes (with FULFILLING bit set in mode) to reserve a spot
         * to match a waiting node.
         */
        
        /* Modes for SNodes, ORed together in node fields */
        
        /** Node represents an unfulfilled consumer */
        // "取"，消费者标记，请求数据
        static final int REQUEST = 0;
        
        /** Node represents an unfulfilled producer */
        // "存"，生产者标记，提供数据
        static final int DATA = 1;
        
        /** Node is fulfilling another unfulfilled DATA or REQUEST */
        // 锁定栈顶操作的标记，接下来就要尝试匹配了
        static final int FULFILLING = 2;
        
        /** The head (top) of the stack */
        // 栈顶游标，指向最近一个被阻塞的结点（操作）
        volatile SNode head;
        
        // VarHandle mechanics
        private static final VarHandle SHEAD;
        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                SHEAD = l.findVarHandle(TransferStack.class, "head", SNode.class);
            } catch(ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        
        /**
         * Puts or takes an item.
         */
        // "传递"数据，匹配/抵消操作
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {
            /*
             * Basic algorithm is to loop trying one of three actions:
             *
             * 1. If apparently empty or already containing nodes of same
             *    mode, try to push node on stack and wait for a match,
             *    returning it, or null if cancelled.
             *
             * 2. If apparently containing node of complementary mode,
             *    try to push a fulfilling node on to stack, match
             *    with corresponding waiting node, pop both from
             *    stack, and return matched item. The matching or
             *    unlinking might not actually be necessary because of
             *    other threads performing action 3:
             *
             * 3. If top of stack already holds another fulfilling node,
             *    help it out by doing its match and/or pop
             *    operations, and then continue. The code for helping
             *    is essentially the same as for fulfilling, except
             *    that it doesn't return the item.
             */
            
            SNode s = null; // constructed/reused as needed
            
            // 设置操作标记，REQUEST代表"取"操作，DATA代表"存"操作
            int mode = (e==null) ? REQUEST : DATA;
            
            for(; ; ) {
                // 每次开始循环前都更新head
                SNode h = head;
                
                /*
                 * 如果栈顶没有阻塞的操作(h==null)，
                 * 或者当前操作与栈顶阻塞的操作一致(h.mode==mode)
                 * 此种情形下需要将当前操作阻塞，并等待匹配的操作出现
                 */
                if(h == null || h.mode == mode) {  // empty or same-mode
                    // 有超时标记，且已经超时
                    if(timed && nanos<=0L) {     // can't wait
                        // 如果h结点代表的操作已经被取消了，则弹出该结点
                        if(h != null && h.isCancelled()) {
                            casHead(h, h.next);     // pop cancelled node
                        } else {
                            // 这种情形参见offer()和poll()方法
                            return null;
                        }
                    } else {
                        // (创建s结点)，将s结点入栈（当前栈顶结点是h）
                        s = snode(s, e, h, mode);
                        
                        /*
                         * 如果h仍与head相等，原子地更新栈顶游标head，使其指向结点s
                         * 如果casHead()返回fasle，说明在此期间，head被别的线程修改了，
                         * 进一步说明有其他线程执行了存/取操作
                         */
                        if(casHead(h, s)) {
                            /*
                             * 尝试阻塞当前线程（操作），直到匹配的操作到来后唤醒它
                             * 被唤醒后，返回当前结点（操作）match域中存储的【待匹配结点】
                             */
                            SNode m = awaitFulfill(s, timed, nanos);
                            
                            // 如果【待匹配结点】就是自身，说明当前操作被取消了
                            if(m == s) {               // wait was cancelled
                                // 剔除被取消的操作（结点）（会剔除一段范围内的所有被取消结点）
                                clean(s);
                                return null;
                            }
                            
                            // 原子地更新head，这里相当于连续弹出两个结点，因为匹配是成对出现的
                            if((h = head) != null && h.next == s) {
                                // 更新栈顶游标【加速】
                                casHead(h, s.next);     // help s's fulfiller
                            }
                            
                            // 如果当前操作（之前被阻塞，现在被唤醒）需要数据
                            if(mode == REQUEST) {
                                // 返回【待匹配结点】提供的数据
                                return (E) m.item;
                                
                                // 如果当前操作（之前被阻塞，现在被唤醒）提供数据
                            } else {
                                // 返回当前结点中的数据
                                return (E) s.item;
                            }
                        }
                    }
                } else {
                    /*
                     * 至此：h!=null && h.mode!=mode，
                     * 说明栈顶存在已阻塞的操作，且与当前操作模式不一致（可以匹配）
                     */
                    
                    // 判断是否有别的线程在处理栈顶操作
                    if(!isFulfilling(h.mode)) { // try to fulfill
                        // 判断栈顶操作是否被取消（阻塞超时）
                        if(h.isCancelled()) {   // already cancelled
                            // 尝试出栈，且更新栈顶游标
                            casHead(h, h.next); // pop and retry
                        } else {
                            /*
                             * 调整s的位置，将其放在栈顶游标head之上
                             * 为结点s添加FULFILLING标记，标记s进入处理状态
                             */
                            s = snode(s, e, h, FULFILLING | mode);
                            
                            /*
                             * 原子地更新栈顶游标head到s的位置
                             * 更新完成后，栈顶的结点带有了FULFILLING标记（栈顶操作被锁定）
                             *
                             * 经过这一步，栈顶操作会被锁定，否则，栈顶操作还是可能会被其他线程抢走
                             */
                            if(casHead(h, s)) {
                                // 开始处理栈顶操作（尝试匹配）
                                for(; ; ) { // loop until matched or waiters disappear
                                    SNode m = s.next;       // m is s's match
                                    
                                    /*
                                     * m==null意味着已经没有排队的操作（结点）了
                                     * 出现这种情形原因可能是：
                                     * 另一个线程在帮助加速完成匹配时，
                                     * 发现先前next域的操作（结点）被取消了，
                                     * 那么紧接着这个next域就会被更新为next的next域，
                                     * 而这个next的next域可能为null，
                                     * 这就导致这里获取到的s.next为null
                                     */
                                    if(m == null) {        // all waiters are gone
                                        // 设置栈顶游标为null
                                        casHead(s, null);   // pop fulfill node
                                        s = null;           // use new node next time
                                        break;              // restart main loop
                                    }
                                    
                                    SNode mn = m.next;
                                    
                                    // 尝试为当前被阻塞的操作（结点m）设置一个【待匹配结点】s，并唤醒被阻塞的操作
                                    if(m.tryMatch(s)) {
                                        // 更新栈顶游标（连续弹出两个结点：m和s）
                                        casHead(s, mn);     // pop both s and m
                                        
                                        // 返回数据
                                        return (mode==REQUEST) ? (E) m.item : (E) s.item;
                                    } else {                // lost match
                                        /* 至此，说明阻塞操作(结点)被取消了 */
                                        
                                        // 更新next域，会导致里一个线程中获取到的m为null
                                        s.casNext(m, mn);   // help unlink
                                    }
                                }// for(; ; )
                            }
                        }
                    } else {                            // help a fulfiller
                        /*
                         * 至此，说明有别的线程正在处理栈顶操作（正在尝试匹配）
                         * 本着不浪费的原则，既然当前线程抢到执行权后，
                         * 发现栈顶操作被别的线程锁定了，但还没执行完，
                         * 那么这里就会加速完成栈顶操作的匹配
                         */
                        
                        // 获取栈顶操作下面的操作（结点）
                        SNode m = h.next;               // m is h's match
                        
                        /*
                         * m==null意味着已经没有排队的操作（结点）了
                         * 出现这种情形原因可能是：
                         * 当前线程在帮助加速完成匹配时，没有立即执行加速动作，
                         * 这个时候，原先执行匹配动作的线程照旧执行匹配，
                         * 但是在匹配过程中，发现先前next域的操作（结点）被取消了，
                         * 那么紧接着next域就会被更新为next的next域，
                         * 而这个next的next域可能为null，
                         * 这就导致这里获取到的s.next为null
                         */
                        if(m == null) { // waiter is gone
                            // 设置栈顶游标为null
                            casHead(h, null);           // pop fulfilling node
                        } else {
                            SNode mn = m.next;
                            
                            // 加速匹配过程
                            if(m.tryMatch(h)) {         // help match
                                casHead(h, mn);         // pop both h and m
                            } else {                    // lost match
                                /* 至此，说明阻塞操作(结点)被取消了 */
                                
                                // 更新next域，会导致里一个线程中获取到的m为null
                                h.casNext(m, mn);       // help unlink
                            }
                        }
                    }
                }
            } // for(; ; )
        }
        
        /**
         * Spins/blocks until node s is matched by a fulfill operation.
         *
         * @param s     the waiting node
         * @param timed true if timed wait
         * @param nanos timeout value
         *
         * @return matched node, or s if cancelled
         */
        // 尝试阻塞当前线程（操作），直到匹配的操作到来后唤醒它
        SNode awaitFulfill(SNode s, boolean timed, long nanos) {
            /*
             * When a node/thread is about to block, it sets its waiter
             * field and then rechecks state at least one more time
             * before actually parking, thus covering race vs
             * fulfiller noticing that waiter is non-null so should be
             * woken.
             *
             * When invoked by nodes that appear at the point of call
             * to be at the head of the stack, calls to park are
             * preceded by spins to avoid blocking when producers and
             * consumers are arriving very close in time.  This can
             * happen enough to bother only on multiprocessors.
             *
             * The order of checks for returning out of main loop
             * reflects fact that interrupts have precedence over
             * normal returns, which have precedence over
             * timeouts. (So, on timeout, one last check for match is
             * done before giving up.) Except that calls from untimed
             * SynchronousQueue.{poll/offer} don't check interrupts
             * and don't wait at all, so are trapped in transfer
             * method rather than calling awaitFulfill.
             */
            
            // 计算截止时间
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            
            Thread w = Thread.currentThread();
            
            // 设置自旋次数
            int spins = shouldSpin(s) ? (timed ? MAX_TIMED_SPINS : MAX_UNTIMED_SPINS) : 0;
            
            for(; ; ) {
                // 如果当前线程带有中断标记，则取消操作
                if(w.isInterrupted()) {
                    s.tryCancel();
                }
                
                // 【待匹配结点】，初始时未null，被唤醒后不为null
                SNode m = s.match;
                if(m != null) {
                    return m;
                }
                
                // 如果需要等待
                if(timed) {
                    // 计算等待时长
                    nanos = deadline - System.nanoTime();
                    
                    // 如果等待超时，则取消该操作
                    if(nanos<=0L) {
                        s.tryCancel();
                        continue;
                    }
                }
                
                // 如果需要自旋，则进入"忙等待"状态
                if(spins>0) {
                    Thread.onSpinWait();
                    // 如果需要自旋，自旋次数减一
                    spins = shouldSpin(s) ? (spins - 1) : 0;
                } else if(s.waiter == null) {
                    // 记录将要被阻塞线程
                    s.waiter = w; // establish waiter so can park next iter
                } else if(!timed) {
                    // 阻塞当前线程，并用当前对象本身作为阻塞标记
                    LockSupport.park(this);
                } else if(nanos>SPIN_FOR_TIMEOUT_THRESHOLD) {
                    // 带超时的阻塞
                    LockSupport.parkNanos(this, nanos);
                }
            }
        }
        
        /**
         * Creates or resets fields of a node. Called only from transfer
         * where the node to push on stack is lazily created and
         * reused when possible to help reduce intervals between reads
         * and CASes of head and to avoid surges of garbage when CASes
         * to push nodes fail due to contention.
         */
        // 创建结点s（结点代表当前的操作），并将s入栈（插入到next的上面）
        static SNode snode(SNode s, Object e, SNode next, int mode) {
            if(s == null) {
                s = new SNode(e);
            }
            s.mode = mode;
            s.next = next;
            return s;
        }
        
        /** Returns true if m has fulfilling bit set. */
        // 判断栈顶结点（操作）是否被锁定
        static boolean isFulfilling(int m) {
            return (m & FULFILLING) != 0;
        }
        
        /*
         * 原子地更新head，使其指向nh
         *
         * 如果在此期间，head被别的线程修改了，即有其他线程执行了存/取操作，
         * 那么该方法返回false（这里不考虑ABA问题，因为只要结点状态一致就可以）
         */
        boolean casHead(SNode h, SNode nh) {
            return h == head && SHEAD.compareAndSet(this, h, nh);
        }
        
        /**
         * Returns true if node s is at head or there is an active fulfiller.
         */
        // 判断是否需要通过自旋进入"忙等待"状态
        boolean shouldSpin(SNode s) {
            SNode h = head;
            return (h == s || h == null || isFulfilling(h.mode));
        }
        
        /**
         * Unlinks s from the stack.
         */
        // 清除s结点，以及清理head到s之前的一段距离上的结点
        void clean(SNode s) {
            s.item = null;   // forget item
            s.waiter = null; // forget thread
            
            /*
             * At worst we may need to traverse entire stack to unlink
             * s. If there are multiple concurrent calls to clean, we
             * might not see s if another thread has already removed
             * it. But we can stop when we see any node known to
             * follow s. We use s.next unless it too is cancelled, in
             * which case we try the node one past. We don't check any
             * further because we don't want to doubly traverse just to
             * find sentinel.
             */
            
            // past游标向栈底移动，直到遇到一个未取消的结点
            SNode past = s.next;
            if(past != null && past.isCancelled()) {
                past = past.next;
            }
            
            /* Absorb cancelled nodes at head */
            SNode p;
            // p游标从head向栈底移动，直到遇到一个未取消的结点（中途被取消的结点会从队列中剔除）
            while((p = head) != null && p != past && p.isCancelled()) {
                casHead(p, p.next);
            }
            
            /* Unsplice embedded nodes */
            // 继续剔除past到p之间所有被取消的结点
            while(p != null && p != past) {
                SNode n = p.next;
                if(n != null && n.isCancelled()) {
                    p.casNext(n, n.next);
                } else {
                    p = n;
                }
            }
        }
        
        /** Node class for TransferStacks. */
        // 链栈结点，用于"非公平模式"的同步阻塞队列
        static final class SNode {
            // 指向下一个阻塞的结点（操作），方向朝着栈底
            volatile SNode next;        // next node in stack
            
            /*
             * 指向与当前结点（操作）匹配的结点（操作），方向朝着栈顶
             * 当设置了match域后，意味着当前结点马上要被处理了
             */
            volatile SNode match;       // the node matched to this
            
            // 持有当前结点（操作）的线程
            volatile Thread waiter;     // to control park/unpark
            
            /* Note: item and mode fields don't need to be volatile since they are always written before, and read after, other volatile/atomic operations. */
            
            // 对于"存"操作来说，item存储数据；对于"取"(REQUEST)操作来说，item存储0
            Object item;                // data; or null for REQUESTs
            
            // 记录当前操作是"存"(DATA)还是"取"(REQUEST)
            int mode;
            
            // VarHandle mechanics
            private static final VarHandle SMATCH;
            private static final VarHandle SNEXT;
            static {
                try {
                    MethodHandles.Lookup l = MethodHandles.lookup();
                    SMATCH = l.findVarHandle(SNode.class, "match", SNode.class);
                    SNEXT = l.findVarHandle(SNode.class, "next", SNode.class);
                } catch(ReflectiveOperationException e) {
                    throw new ExceptionInInitializerError(e);
                }
            }
            
            SNode(Object item) {
                this.item = item;
            }
            
            // 原子地更新next域为val
            boolean casNext(SNode cmp, SNode val) {
                return cmp == next && SNEXT.compareAndSet(this, cmp, val);
            }
            
            /**
             * Tries to match node s to this node, if so, waking up thread.
             * Fulfillers call tryMatch to identify their waiters.
             * Waiters block until they have been matched.
             *
             * @param s the node to match
             *
             * @return true if successfully matched to s
             */
            /*
             * 将结点s作为【待匹配结点】，赋值给当前被阻塞结点的match域，尝试让当前结点与s匹配
             * 在这个过程中，当前被阻塞的线程（结点）会被唤醒
             * 如果当前被阻塞结点的match域不为空，则需要判断它与【待匹配结点】是否一致
             */
            boolean tryMatch(SNode s) {
                if(match == null && SMATCH.compareAndSet(this, null, s)) {
                    Thread w = waiter;
                    // 如果w==null，说明该操作（结点）被取消了
                    if(w != null) {    // waiters need at most one unpark
                        waiter = null;
                        LockSupport.unpark(w);
                    }
                    return true;
                }
                
                return match == s;
            }
            
            /**
             * Tries to cancel a wait by matching node to itself.
             */
            // 取消操作（将结点的match域设置为自身）
            void tryCancel() {
                SMATCH.compareAndSet(this, null, this);
            }
            
            // 判断当前操作是否被取消
            boolean isCancelled() {
                return match == this;
            }
        }
    }
}
