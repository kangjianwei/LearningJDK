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
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An optionally-bounded {@linkplain BlockingQueue blocking queue} based on
 * linked nodes.
 * This queue orders elements FIFO (first-in-first-out).
 * The <em>head</em> of the queue is that element that has been on the
 * queue the longest time.
 * The <em>tail</em> of the queue is that element that has been on the
 * queue the shortest time. New elements
 * are inserted at the tail of the queue, and the queue retrieval
 * operations obtain elements at the head of the queue.
 * Linked queues typically have higher throughput than array-based queues but
 * less predictable performance in most concurrent applications.
 *
 * <p>The optional capacity bound constructor argument serves as a
 * way to prevent excessive queue expansion. The capacity, if unspecified,
 * is equal to {@link Integer#MAX_VALUE}.  Linked nodes are
 * dynamically created upon each insertion unless this would bring the
 * queue above capacity.
 *
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Collection} and {@link Iterator} interfaces.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @param <E> the type of elements held in this queue
 *
 * @author Doug Lea
 * @since 1.5
 */
/*
 * 链式有界/无界（初始化时决定使用有界还是无界）单向阻塞队列，线程安全（锁）
 *
 * LinkedBlockingQueue的侧重点是存储【数据】，可以将大量【数据】缓存到该队列中以便取用
 *
 * 该队列使用的锁是ReentrantLock，配套Condition，在"入队"/"出队"时，会锁住整个队列。
 * 这样的锁粒度较大，所以线程争用高时，性能较差。
 */
public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
    
    /*
     * A variant of the "two lock queue" algorithm.  The putLock gates
     * entry to put (and offer), and has an associated condition for
     * waiting puts.  Similarly for the takeLock.  The "count" field
     * that they both rely on is maintained as an atomic to avoid
     * needing to get both locks in most cases. Also, to minimize need
     * for puts to get takeLock and vice-versa, cascading notifies are
     * used. When a put notices that it has enabled at least one take,
     * it signals taker. That taker in turn signals others if more
     * items have been entered since the signal. And symmetrically for
     * takes signalling puts. Operations such as remove(Object) and
     * iterators acquire both locks.
     *
     * Visibility between writers and readers is provided as follows:
     *
     * Whenever an element is enqueued, the putLock is acquired and
     * count updated.  A subsequent reader guarantees visibility to the
     * enqueued Node by either acquiring the putLock (via fullyLock)
     * or by acquiring the takeLock, and then reading n = count.get();
     * this gives visibility to the first n items.
     *
     * To implement weakly consistent iterators, it appears we need to
     * keep all Nodes GC-reachable from a predecessor dequeued Node.
     * That would cause two problems:
     * - allow a rogue Iterator to cause unbounded memory retention
     * - cause cross-generational linking of old Nodes to new Nodes if
     *   a Node was tenured while live, which generational GCs have a
     *   hard time dealing with, causing repeated major collections.
     * However, only non-deleted Nodes need to be reachable from
     * dequeued Nodes, and reachability does not necessarily have to
     * be of the kind understood by the GC.  We use the trick of
     * linking a Node that has just been dequeued to itself.  Such a
     * self-link implicitly means to advance to head.next.
     */
    
    /** The capacity bound, or Integer.MAX_VALUE if none */
    // 队列容量，决定队列是有界还是无界
    private final int capacity;
    
    /** Current number of elements */
    // 当前队列中的元素数量
    private final AtomicInteger count = new AtomicInteger();
    
    /** Lock held by put, offer, etc */
    // "入队"锁
    private final ReentrantLock putLock = new ReentrantLock();
    /** Wait queue for waiting puts */
    // "入队"条件
    private final Condition notFull = putLock.newCondition();
    
    /** Lock held by take, poll, etc */
    // "出队"锁
    private final ReentrantLock takeLock = new ReentrantLock();
    /** Wait queue for waiting takes */
    // "出队"条件
    private final Condition notEmpty = takeLock.newCondition();
    
    /**
     * Head of linked list.
     * Invariant: head.item == null
     */
    // 队头
    transient Node<E> head;
    
    /**
     * Tail of linked list.
     * Invariant: last.next == null
     */
    // 队尾
    private transient Node<E> last;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code LinkedBlockingQueue} with a capacity of
     * {@link Integer#MAX_VALUE}.
     */
    // 构造阻塞式单向链队，最大容量为Integer.MAX_VALUE
    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }
    
    /**
     * Creates a {@code LinkedBlockingQueue} with the given (fixed) capacity.
     *
     * @param capacity the capacity of this queue
     *
     * @throws IllegalArgumentException if {@code capacity} is not greater
     *                                  than zero
     */
    // 构造指定容量的阻塞式单向链队
    public LinkedBlockingQueue(int capacity) {
        if(capacity<=0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        last = head = new Node<E>(null);
    }
    
    /**
     * Creates a {@code LinkedBlockingQueue} with a capacity of
     * {@link Integer#MAX_VALUE}, initially containing the elements of the
     * given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param c the collection of elements to initially contain
     *
     * @throws NullPointerException if the specified collection or any
     *                              of its elements are null
     */
    // 用给定容器中的元素初始化队列
    public LinkedBlockingQueue(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        final ReentrantLock putLock = this.putLock;
        putLock.lock(); // Never contended, but necessary for visibility
        try {
            int n = 0;
            for(E e : c) {
                if(e == null) {
                    throw new NullPointerException();
                }
                
                if(n == capacity) {
                    throw new IllegalStateException("Queue full");
                }
                // 元素入队
                enqueue(new Node<E>(e));
                ++n;
            }
            count.set(n);
        } finally {
            putLock.unlock();
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 入队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Inserts the specified element at the tail of this queue if it is
     * possible to do so immediately without exceeding the queue's capacity,
     * returning {@code true} upon success and {@code false} if this queue
     * is full.
     * When using a capacity-restricted queue, this method is generally
     * preferable to method {@link BlockingQueue#add add}, which can fail to
     * insert an element only by throwing an exception.
     *
     * @throws NullPointerException if the specified element is null
     */
    // 入队，线程安全，队满时不阻塞，直接返回false
    public boolean offer(E e) {
        if(e == null) {
            throw new NullPointerException();
        }
        
        final AtomicInteger count = this.count;
        
        if(count.get() == capacity) {
            return false;
        }
        
        final int c;
        final Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            // 队满时直接返回
            if(count.get() == capacity) {
                return false;
            }
            
            // 入队
            enqueue(node);
            
            // 元素个数增一
            c = count.getAndIncrement();
            
            // 如果现在至少还剩一个空槽（c是入队前容量）
            if(c + 1<capacity) {
                // 唤醒可能阻塞的"入队"线程
                notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        
        // 如果队列之前为空（现在不空了）
        if(c == 0) {
            // 唤醒可能阻塞的"出队"线程
            signalNotEmpty();
        }
        
        return true;
    }
    
    /**
     * Inserts the specified element at the tail of this queue, waiting if
     * necessary up to the specified wait time for space to become available.
     *
     * @return {@code true} if successful, or {@code false} if
     * the specified waiting time elapses before space is available
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    // 入队，线程安全，队满时阻塞一段时间，如果在指定的时间内没有机会插入元素，则返回false
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if(e == null) {
            throw new NullPointerException();
        }
        
        long nanos = unit.toNanos(timeout);
        final int c;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            // 如果队列已满，且未超时，则需要阻塞一段时间
            while(count.get() == capacity) {
                if(nanos<=0L) {
                    // 如果一直没机会插入元素，则返回false
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            
            // 执行入队操作
            enqueue(new Node<E>(e));
            c = count.getAndIncrement();
            
            // 如果现在至少还剩一个空槽（c是入队前容量）
            if(c + 1<capacity) {
                // 唤醒可能阻塞的"入队"线程
                notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        
        // 如果队列之前为空（现在不空了）
        if(c == 0) {
            // 唤醒可能阻塞的"出队"线程
            signalNotEmpty();
        }
        
        return true;
    }
    
    
    /**
     * Inserts the specified element at the tail of this queue, waiting if
     * necessary for space to become available.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    // 入队，线程安全，队满时线程被阻塞
    public void put(E e) throws InterruptedException {
        if(e == null) {
            throw new NullPointerException();
        }
        final int c;
        final Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            /*
             * Note that count is used in wait guard even though it is
             * not protected by lock. This works because count can
             * only decrease at this point (all other puts are shut
             * out by lock), and we (or some other waiting put) are
             * signalled if it ever changes from capacity. Similarly
             * for all other uses of count in other wait guards.
             */
            // 如果队列已满，则当前线程陷入阻塞，并唤醒其他等待者
            while(count.get() == capacity) {
                // "未满"条件不成立
                notFull.await();
            }
            
            // 入队
            enqueue(node);
            
            // 元素个数增一
            c = count.getAndIncrement();
            
            // 如果现在至少还剩一个空槽（c是入队前容量）
            if(c + 1<capacity) {
                // 唤醒可能阻塞的"入队"线程
                notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        
        // 如果队列之前为空（现在不空了）
        if(c == 0) {
            // 唤醒可能阻塞的"出队"线程
            signalNotEmpty();
        }
    }
    
    /*▲ 入队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 出队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 出队，线程安全，队空时不阻塞，直接返回null
    public E poll() {
        final AtomicInteger count = this.count;
        if(count.get() == 0) {
            return null;
        }
        
        final E x;
        final int c;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            // 队空时直接返回
            if(count.get() == 0) {
                return null;
            }
            
            // 出队
            x = dequeue();
            
            // 元素个数减一
            c = count.getAndDecrement();
            
            // 如果现在至少还剩一个元素（c是入队前容量）
            if(c>1) {
                // 唤醒可能阻塞的"出队"线程
                notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        
        // 如果队列之前为满（现在不满了）
        if(c == capacity) {
            // 唤醒可能阻塞的"入队"线程
            signalNotFull();
        }
        
        return x;
    }
    
    // 出队，线程安全，队空时阻塞一段时间，如果在指定的时间内没有机会取出元素，则返回null
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        final E x;
        final int c;
        long nanos = unit.toNanos(timeout);
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            // 如果队列已空，且未超时，则需要阻塞一段时间
            while(count.get() == 0) {
                if(nanos<=0L) {
                    // 如果一直没机会取出元素，则返回null
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            
            // 执行出队操作
            x = dequeue();
            c = count.getAndDecrement();
            
            // 如果现在至少还剩一个元素（c是入队前容量）
            if(c>1) {
                // 唤醒可能阻塞的"出队"线程
                notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        
        // 如果队列之前为满（现在不满了）
        if(c == capacity) {
            // 唤醒可能阻塞的"入队"线程
            signalNotFull();
        }
        
        return x;
    }
    
    
    // 出队，线程安全，队空时线程被阻塞
    public E take() throws InterruptedException {
        final E x;
        final int c;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            // 如果队列已空，则当前线程陷入阻塞，并唤醒其他等待者
            while(count.get() == 0) {
                // "未空"条件不成立
                notEmpty.await();
            }
            
            // 出队
            x = dequeue();
            
            // 元素个数减一
            c = count.getAndDecrement();
            
            // 如果现在至少还剩一个元素（c是入队前容量）
            if(c>1) {
                // 唤醒可能阻塞的"出队"线程
                notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        
        // 如果队列之前为满（现在不满了）
        if(c == capacity) {
            // 唤醒可能阻塞的"入队"线程
            signalNotFull();
        }
        
        return x;
    }
    
    
    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.
     * Returns {@code true} if this queue contained the specified element
     * (or equivalently, if this queue changed as a result of the call).
     *
     * @param o element to be removed from this queue, if present
     *
     * @return {@code true} if this queue changed as a result of the call
     */
    // 移除元素，不阻塞（线程安全）
    public boolean remove(Object o) {
        if(o == null) {
            return false;
        }
        
        // 对入队和出队动作加锁
        fullyLock();
        try {
            // 用p指向待移除结点
            for(Node<E> pred = head, p = pred.next; p != null; pred = p, p = p.next) {
                if(o.equals(p.item)) {
                    // 移除pred后面链接的结点p
                    unlink(p, pred);
                    return true;
                }
            }
            return false;
        } finally {
            // 对入队和出队解锁
            fullyUnlock();
        }
    }
    
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 移除所有满足过滤条件的元素，不阻塞（线程安全）
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        return bulkRemove(filter);
    }
    
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // (匹配则移除)移除队列中所有与给定容器中的元素匹配的元素，不阻塞（线程安全）
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> c.contains(e));
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // (不匹配则移除)移除队列中所有与给定容器中的元素不匹配的元素，不阻塞（线程安全）
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> !c.contains(e));
    }
    
    
    /**
     * Atomically removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     */
    // 清空，即移除所有元素，不阻塞（线程安全）
    public void clear() {
        // 对入队和出队动作加锁
        fullyLock();
        try {
            for(Node<E> p, h = head; (p = h.next) != null; h = p) {
                h.next = h;
                p.item = null;
            }
            head = last;
            // assert head.item == null && head.next == null;
            if(count.getAndSet(0) == capacity) {
                // 唤醒入队线程
                notFull.signal();
            }
        } finally {
            // 对入队和出队解锁
            fullyUnlock();
        }
    }
    
    
    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    // 将队列中所有元素移除，并转移到给定的容器当中
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }
    
    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    // 将队列中前maxElements个元素移除，并转移到给定的容器当中
    public int drainTo(Collection<? super E> c, int maxElements) {
        Objects.requireNonNull(c);
        
        if(c == this) {
            throw new IllegalArgumentException();
        }
        
        if(maxElements<=0) {
            return 0;
        }
        
        boolean signalNotFull = false;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            int n = Math.min(maxElements, count.get());
            // count.get provides visibility to first n Nodes
            Node<E> h = head;
            int i = 0;
            try {
                // 将队列中的元素转移到给定的容器当中
                while(i<n) {
                    Node<E> p = h.next;
                    c.add(p.item);
                    p.item = null;
                    h.next = h;
                    h = p;
                    ++i;
                }
                return n;
            } finally {
                // Restore invariants even if c.add() threw
                if(i>0) {
                    // assert h.item == null;
                    head = h;   // 更新head
                    
                    // 判断是否有必要唤醒"入队"线程
                    signalNotFull = (count.getAndAdd(-i) == capacity);
                }
            }
        } finally {
            takeLock.unlock();
            
            // 如果转移元素前队列是满的（现在不满了），则唤醒"入队"线程
            if(signalNotFull) {
                signalNotFull();
            }
        }
    }
    
    /*▲ 出队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取队头元素，线程安全
    public E peek() {
        final AtomicInteger count = this.count;
        if(count.get() == 0) {
            return null;
        }
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            return (count.get()>0) ? head.next.item : null;
        } finally {
            takeLock.unlock();
        }
    }
    
    /*▲ 取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包含查询 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this queue
     *
     * @return {@code true} if this queue contains the specified element
     */
    // 判断队列中是否包含元素o
    public boolean contains(Object o) {
        if(o == null) {
            return false;
        }
        
        // 对入队和出队动作加锁
        fullyLock();
        
        try {
            for(Node<E> p = head.next; p != null; p = p.next) {
                if(o.equals(p.item)) {
                    return true;
                }
            }
            return false;
        } finally {
            // 对入队和出队解锁
            fullyUnlock();
        }
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        fullyLock();
        try {
            int size = count.get();
            Object[] a = new Object[size];
            int k = 0;
            for(Node<E> p = head.next; p != null; p = p.next) {
                a[k++] = p.item;
            }
            return a;
        } finally {
            fullyUnlock();
        }
    }
    
    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence; the runtime type of the returned array is that of
     * the specified array.  If the queue fits in the specified array, it
     * is returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array and the size of this queue.
     *
     * <p>If this queue fits in the specified array with room to spare
     * (i.e., the array has more elements than this queue), the element in
     * the array immediately following the end of the queue is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of {@code String}:
     *
     * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     *
     * @return an array containing all of the elements in this queue
     *
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this queue
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        fullyLock();
        try {
            int size = count.get();
            if(a.length<size) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }
            
            int k = 0;
            for(Node<E> p = head.next; p != null; p = p.next) {
                a[k++] = (T) p.item;
            }
            if(a.length>k) {
                a[k] = null;
            }
            return a;
        } finally {
            fullyUnlock();
        }
    }
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 迭代 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 遍历所有元素，并执行相应的择取操作
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        forEachFrom(action, null);
    }
    
    
    /**
     * Returns an iterator over the elements in this queue in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this queue in proper sequence
     */
    // 返回当前队列的迭代器
    public Iterator<E> iterator() {
        return new Itr();
    }
    
    /**
     * Returns a {@link Spliterator} over the elements in this queue.
     *
     * <p>The returned spliterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#ORDERED}, and {@link Spliterator#NONNULL}.
     *
     * @return a {@code Spliterator} over the elements in this queue
     *
     * @implNote The {@code Spliterator} implements {@code trySplit} to permit limited
     * parallelism.
     * @since 1.8
     */
    // 返回描述此队列中元素的Spliterator
    public Spliterator<E> spliterator() {
        return new LBQSpliterator();
    }
    
    /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of elements in this queue.
     *
     * @return the number of elements in this queue
     *
     * this doc comment is overridden to remove the reference to collections greater in size than Integer.MAX_VALUE
     */
    // 返回队列中元素数量
    public int size() {
        return count.get();
    }
    
    /**
     * Returns the number of additional elements that this queue can ideally
     * (in the absence of memory or resource constraints) accept without
     * blocking. This is always equal to the initial capacity of this queue
     * less the current {@code size} of this queue.
     *
     * <p>Note that you <em>cannot</em> always tell if an attempt to insert
     * an element will succeed by inspecting {@code remainingCapacity}
     * because it may be the case that another thread is about to
     * insert or remove an element.
     *
     * this doc comment is a modified copy of the inherited doc comment, without the reference to unlimited queues.
     */
    // 返回队列的剩余容量
    public int remainingCapacity() {
        return capacity - count.get();
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Locks to prevent both puts and takes.
     */
    // 对入队和出队动作加锁
    void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }
    
    /**
     * Unlocks to allow both puts and takes.
     */
    // 对入队和出队动作解锁
    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }
    
    /**
     * Signals a waiting put. Called only from take/poll.
     */
    // 唤醒"入队"线程
    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }
    
    /**
     * Signals a waiting take. Called only from put/offer (which do not
     * otherwise ordinarily lock takeLock.)
     */
    // 唤醒"出队"线程
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }
    
    /*▲ 锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static final long serialVersionUID = -6903933977591709194L;
    
    
    /**
     * Saves this queue to a stream (that is, serializes it).
     *
     * @param s the stream
     *
     * @throws java.io.IOException if an I/O error occurs
     * @serialData The capacity is emitted (int), followed by all of
     * its elements (each an {@code Object}) in the proper order,
     * followed by a null
     */
    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
        
        fullyLock();
        
        try {
            // Write out any hidden stuff, plus capacity
            s.defaultWriteObject();
            
            // Write out all elements in the proper order.
            for(Node<E> p = head.next; p != null; p = p.next) {
                s.writeObject(p.item);
            }
            
            // Use trailing null as sentinel
            s.writeObject(null);
        } finally {
            fullyUnlock();
        }
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
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        // Read in capacity, and any hidden stuff
        s.defaultReadObject();
        
        count.set(0);
        last = head = new Node<E>(null);
        
        // Read in all elements and place in queue
        for(; ; ) {
            @SuppressWarnings("unchecked")
            E item = (E) s.readObject();
            if(item == null) {
                break;
            }
            add(item);
        }
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    public String toString() {
        return Helpers.collectionToString(this);
    }
    
    
    
    /**
     * Links node at end of queue.
     *
     * @param node the node
     */
    // 入队，非线程安全，仅内部使用
    private void enqueue(Node<E> node) {
        // assert putLock.isHeldByCurrentThread();
        // assert last.next == null;
        last = last.next = node;
    }
    
    /**
     * Removes a node from head of queue.
     *
     * @return the node
     */
    // 出队，非线程安全，仅内部使用
    private E dequeue() {
        // assert takeLock.isHeldByCurrentThread();
        // assert head.item == null;
        Node<E> h = head;
        Node<E> first = h.next;
        // 辅助GC
        h.next = h; // help GC
        // 队头前进
        head = first;
        E x = first.item;
        first.item = null;
        return x;
    }
    
    /**
     * Unlinks interior Node p with predecessor pred.
     */
    // 移除pred后面链接的结点p
    void unlink(Node<E> p, Node<E> pred) {
        // assert putLock.isHeldByCurrentThread();
        // assert takeLock.isHeldByCurrentThread();
        // p.next is not changed, to allow iterators that are
        // traversing p to maintain their weak-consistency guarantee.
        p.item = null;
        pred.next = p.next;
        
        // 如果移除的是最后一个结点，则更新last
        if(last == p) {
            last = pred;
        }
        
        // 如果移除元素前队列是满的（现在不满了），唤醒"入队"线程
        if(count.getAndDecrement() == capacity) {
            notFull.signal();
        }
    }
    
    /** Implementation of bulk remove methods. */
    // 批量移除元素，即满足过滤条件的元素将被移除，不阻塞（线程安全）
    @SuppressWarnings("unchecked")
    private boolean bulkRemove(Predicate<? super E> filter) {
        boolean removed = false;
        Node<E> p = null, ancestor = head;
        Node<E>[] nodes = null;
        int n, len = 0;
        do {
            // 1. Extract batch of up to 64 elements while holding the lock.
            fullyLock();
            try {
                // 初始化容器，用来存储待筛选元素
                if(nodes == null) {  // first batch; initialize
                    p = head.next;
                    for(Node<E> q = p; q != null; q = succ(q)) {
                        if(q.item != null && ++len == 64) {
                            break;
                        }
                    }
                    nodes = (Node<E>[]) new Node<?>[len];
                }
                
                // 取出一批元素
                for(n = 0; p != null && n<len; p = succ(p)) {
                    nodes[n++] = p;
                }
            } finally {
                fullyUnlock();
            }
            
            // 2. Run the filter on the elements while lock is free.
            long deathRow = 0L;       // "bitset" of size 64
            for(int i = 0; i<n; i++) {
                final E e;
                // 标记应当移除的元素
                if((e = nodes[i].item) != null && filter.test(e)) {
                    deathRow |= 1L << i;
                }
            }
            
            // 3. Remove any filtered elements while holding the lock.
            if(deathRow != 0) {
                fullyLock();
                try {
                    for(int i = 0; i<n; i++) {
                        final Node<E> q;
                        // 移除满足筛选条件的元素
                        if((deathRow & (1L << i)) != 0L && (q = nodes[i]).item != null) {
                            ancestor = findPred(q, ancestor);
                            unlink(q, ancestor);
                            removed = true;
                        }
                        nodes[i] = null; // help GC
                    }
                } finally {
                    fullyUnlock();
                }
            }
        } while(n>0 && p != null);
        
        return removed;
    }
    
    /**
     * Returns the predecessor of live node p, given a node that was
     * once a live ancestor of p (or head); allows unlinking of p.
     */
    // 从ancestor开始，查找结点p的前驱
    Node<E> findPred(Node<E> p, Node<E> ancestor) {
        // assert p.item != null;
        if(ancestor.item == null) {
            ancestor = head;
        }
        
        // Fails with NPE if precondition not satisfied
        for(Node<E> q; (q = ancestor.next) != p; ) {
            ancestor = q;
        }
        
        return ancestor;
    }
    
    /**
     * Used for any element traversal that is not entirely under lock.
     * Such traversals must handle both:
     * - dequeued nodes (p.next == p)
     * - (possibly multiple) interior removed nodes (p.item == null)
     */
    // 返回p的后继
    Node<E> succ(Node<E> p) {
        // 如果p结点已经被移除
        if(p == (p = p.next)) {
            // 让p指向首个结点
            p = head.next;
        }
        return p;
    }
    
    /**
     * Runs action on each element found during a traversal starting at p.
     * If p is null, traversal starts at head.
     */
    // 从p开始遍历所有剩余元素，并执行相应的择取操作
    void forEachFrom(Consumer<? super E> action, Node<E> p) {
        // Extract batches of elements while holding the lock; then run the action on the elements while not
        final int batchSize = 64;       // max number of elements per batch
        Object[] es = null;             // container for batch of elements
        int n, len = 0;
        
        // 分批遍历元素，参考迭代器Itr中的遍历，原理是一致的
        do {
            fullyLock();
            try {
                // 第一批
                if(es == null) {
                    // 从队头开始遍历
                    if(p == null) {
                        p = head.next;
                    }
                    
                    for(Node<E> q = p; q != null; q = succ(q)) {
                        if(q.item != null && ++len == batchSize) {
                            break;
                        }
                    }
                    es = new Object[len];
                }
                
                for(n = 0; p != null && n<len; p = succ(p)) {
                    if((es[n] = p.item) != null) {
                        n++;
                    }
                }
            } finally {
                fullyUnlock();
            }
            
            for(int i = 0; i<n; i++) {
                @SuppressWarnings("unchecked")
                E e = (E) es[i];
                action.accept(e);
            }
        } while(n>0 && p != null);
    }
    
    
    
    
    
    
    /**
     * Linked list node class.
     */
    // 队列结点
    static class Node<E> {
        E item;
        
        /**
         * One of:
         * - the real successor Node
         * - this Node, meaning the successor is head.next
         * - null, meaning there is no successor (this is the last node)
         */
        // 后继
        Node<E> next;
        
        Node(E x) {
            item = x;
        }
    }
    
    /**
     * Weakly-consistent iterator.
     *
     * Lazily updated ancestor field provides expected O(1) remove(),
     * but still O(n) in the worst case, whenever the saved ancestor
     * is concurrently deleted.
     */
    // 用于当前队列的外部迭代器
    private class Itr implements Iterator<E> {
        // 指向下一个结点
        private Node<E> next;           // Node holding nextItem
        
        // 持有下一个元素
        private E nextItem;             // next item to hand out
        
        private Node<E> lastRet;
        private Node<E> ancestor;       // Helps unlink lastRet on remove()
        
        // 初始化迭代器
        Itr() {
            fullyLock();
            try {
                if((next = head.next) != null) {
                    nextItem = next.item;
                }
            } finally {
                fullyUnlock();
            }
        }
        
        // 判断是否存在下一个元素
        public boolean hasNext() {
            return next != null;
        }
        
        // 返回下一个元素，并更新next与nextItem
        public E next() {
            Node<E> p;
            if((p = next) == null) {
                throw new NoSuchElementException();
            }
            lastRet = p;
            E x = nextItem;
            fullyLock();
            try {
                E e = null;
                for(p = p.next; p != null && (e = p.item) == null; ) {
                    p = succ(p);
                }
                next = p;
                nextItem = e;
            } finally {
                fullyUnlock();
            }
            return x;
        }
        
        // 移除刚刚遍历过的那个元素
        public void remove() {
            // p指向上一个遍历过的元素
            Node<E> p = lastRet;
            
            if(p == null) {
                throw new IllegalStateException();
            }
            
            lastRet = null;
            
            fullyLock();
            try {
                if(p.item != null) {
                    if(ancestor == null) {
                        ancestor = head;
                    }
                    
                    // 从ancestor开始，查找结点p的前驱
                    ancestor = findPred(p, ancestor);
                    
                    // 移除ancestor后面链接的结点p
                    unlink(p, ancestor);
                }
            } finally {
                fullyUnlock();
            }
        }
        
        // 遍历剩余元素，并对其执行相应的择取操作
        public void forEachRemaining(Consumer<? super E> action) {
            // A variant of forEachFrom
            Objects.requireNonNull(action);
            
            Node<E> p;
            
            if((p = next) == null) {
                return;
            }
            
            lastRet = p;
            
            next = null;
            
            final int batchSize = 64;
            
            Object[] es = null;
            
            int n, len = 1;
            
            do {
                fullyLock();
                try {
                    if(es == null) {
                        p = p.next;
                        
                        // 统计剩余元素（如果超过了64个，则分批统计）
                        for(Node<E> q = p; q != null; q = succ(q)) {
                            if(q.item != null && ++len == batchSize) {
                                break;
                            }
                        }
                        es = new Object[len];
                        es[0] = nextItem;
                        nextItem = null;
                        n = 1;
                    } else {
                        // 统计下一批元素
                        n = 0;
                    }
                    
                    for(; p != null && n<len; p = succ(p)) {
                        if((es[n] = p.item) != null) {
                            lastRet = p;
                            n++;
                        }
                    }
                } finally {
                    fullyUnlock();
                }
                
                // 应用择取操作
                for(int i = 0; i<n; i++) {
                    @SuppressWarnings("unchecked")
                    E e = (E) es[i];
                    action.accept(e);
                }
                
            } while(n>0 && p != null);
        }
    }
    
    /**
     * A customized variant of Spliterators.IteratorSpliterator.
     * Keep this class in sync with (very similar) LBDSpliterator.
     */
    // 描述此队列中元素的Spliterator
    private final class LBQSpliterator implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        
        // 指向当前这批待遍历元素中的首个元素
        Node<E> current;    // current node; null until initialized
        
        // 当前这批待遍历元素的数量
        int batch;          // batch size for splits
        
        // true代表没有待遍历元素了
        boolean exhausted;  // true when no more nodes
        
        // 迭代器当前的容量（会变化）
        long est = size();  // size estimate
        
        LBQSpliterator() {
        }
        
        public long estimateSize() {
            return est;
        }
        
        // 从容器的指定范围切割一段元素，将其打包到Spliterator后返回
        public Spliterator<E> trySplit() {
            Node<E> h;
            
            if(!exhausted
                && ((h = current) != null || (h = head.next) != null)
                && h.next != null) {
                
                // 当前这批元素的数量
                int n = batch = Math.min(batch + 1, MAX_BATCH);
                Object[] a = new Object[n];
                int i = 0;
                Node<E> p = current;
                fullyLock();
                try {
                    if(p != null || (p = head.next) != null) {
                        // 选出一批元素
                        for(; p != null && i<n; p = succ(p)) {
                            if((a[i] = p.item) != null) {
                                i++;
                            }
                        }
                    }
                } finally {
                    fullyUnlock();
                }
                
                if((current = p) == null) {
                    est = 0L;
                    exhausted = true;
                } else if((est -= i)<0L) {
                    est = 0L;
                }
                
                if(i>0) {
                    return Spliterators.spliterator(a, 0, i, (Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT));
                }
            }
            
            return null;
        }
        
        // 对容器中的单个当前元素执行择取操作
        public boolean tryAdvance(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            if(!exhausted) {
                E e = null;
                fullyLock();
                try {
                    Node<E> p;
                    if((p = current) != null || (p = head.next) != null)
                        do {
                            e = p.item;
                            p = succ(p);
                        } while(e == null && p != null);
                    if((current = p) == null) {
                        exhausted = true;
                    }
                } finally {
                    fullyUnlock();
                }
                
                if(e != null) {
                    action.accept(e);
                    return true;
                }
            }
            return false;
        }
        
        // 遍历所有剩余元素，并执行相应的择取操作
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            
            // 如果仍有待遍历元素
            if(!exhausted) {
                exhausted = true;
                Node<E> p = current;
                current = null;
                forEachFrom(action, p);
            }
        }
        
        public int characteristics() {
            return (Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
        }
    }
    
}
