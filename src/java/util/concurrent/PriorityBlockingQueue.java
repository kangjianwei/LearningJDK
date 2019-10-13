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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import jdk.internal.misc.SharedSecrets;

/**
 * An unbounded {@linkplain BlockingQueue blocking queue} that uses
 * the same ordering rules as class {@link PriorityQueue} and supplies
 * blocking retrieval operations.  While this queue is logically
 * unbounded, attempted additions may fail due to resource exhaustion
 * (causing {@code OutOfMemoryError}). This class does not permit
 * {@code null} elements.  A priority queue relying on {@linkplain
 * Comparable natural ordering} also does not permit insertion of
 * non-comparable objects (doing so results in
 * {@code ClassCastException}).
 *
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Collection} and {@link Iterator} interfaces.
 * The Iterator provided in method {@link #iterator()} and the
 * Spliterator provided in method {@link #spliterator()} are <em>not</em>
 * guaranteed to traverse the elements of the PriorityBlockingQueue in
 * any particular order. If you need ordered traversal, consider using
 * {@code Arrays.sort(pq.toArray())}.  Also, method {@code drainTo} can
 * be used to <em>remove</em> some or all elements in priority order and
 * place them in another collection.
 *
 * <p>Operations on this class make no guarantees about the ordering
 * of elements with equal priority. If you need to enforce an
 * ordering, you can define custom classes or comparators that use a
 * secondary key to break ties in primary priority values.  For
 * example, here is a class that applies first-in-first-out
 * tie-breaking to comparable elements. To use it, you would insert a
 * {@code new FIFOEntry(anEntry)} instead of a plain entry object.
 *
 * <pre> {@code
 * class FIFOEntry<E extends Comparable<? super E>>
 *     implements Comparable<FIFOEntry<E>> {
 *   static final AtomicLong seq = new AtomicLong(0);
 *   final long seqNum;
 *   final E entry;
 *   public FIFOEntry(E entry) {
 *     seqNum = seq.getAndIncrement();
 *     this.entry = entry;
 *   }
 *   public E getEntry() { return entry; }
 *   public int compareTo(FIFOEntry<E> other) {
 *     int res = entry.compareTo(other.entry);
 *     if (res == 0 && other.entry != this.entry)
 *       res = (seqNum < other.seqNum ? -1 : 1);
 *     return res;
 *   }
 * }}</pre>
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
 * 顺序无界（队列容量支持扩容到Integer.MAX_VALUE）优先阻塞队列，线程安全（锁）
 *
 * 该容器可以看做是PriorityQueue的一个线程安全的版本
 *
 * 具体说明参见PriorityQueue
 */
@SuppressWarnings("unchecked")
public class PriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
    
    /*
     * The implementation uses an array-based binary heap, with public
     * operations protected with a single lock. However, allocation
     * during resizing uses a simple spinlock (used only while not
     * holding main lock) in order to allow takes to operate
     * concurrently with allocation.  This avoids repeated
     * postponement of waiting consumers and consequent element
     * build-up. The need to back away from lock during allocation
     * makes it impossible to simply wrap delegated
     * java.util.PriorityQueue operations within a lock, as was done
     * in a previous version of this class. To maintain
     * interoperability, a plain PriorityQueue is still used during
     * serialization, which maintains compatibility at the expense of
     * transiently doubling overhead.
     */
    
    /**
     * Default array capacity.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
    
    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    
    /**
     * Priority queue represented as a balanced binary heap: the two
     * children of queue[n] are queue[2*n+1] and queue[2*(n+1)].  The
     * priority queue is ordered by comparator, or by the elements'
     * natural ordering, if comparator is null: For each node n in the
     * heap and each descendant d of n, n <= d.  The element with the
     * lowest value is in queue[0], assuming the queue is nonempty.
     */
    // 存储队列元素
    private transient Object[] queue;
    
    /**
     * The number of elements in the priority queue.
     */
    // 队列长度
    private transient int size;
    
    /**
     * The comparator, or null if priority queue uses elements'
     * natural ordering.
     */
    // 外部比较器，支持以自定义的顺序来比较元素。如果没有设置，则使用元素自身实现的内部比较器
    private transient Comparator<? super E> comparator;
    
    /**
     * Spinlock for allocation, acquired via CAS.
     */
    // 标记是否正在扩容
    private transient volatile int allocationSpinLock;
    
    /**
     * Lock used for all public operations.
     */
    private final ReentrantLock lock = new ReentrantLock();
    
    /**
     * Condition for blocking when empty.
     */
    private final Condition notEmpty = lock.newCondition();
    
    
    // VarHandle mechanics
    private static final VarHandle ALLOCATIONSPINLOCK;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            ALLOCATIONSPINLOCK = l.findVarHandle(PriorityBlockingQueue.class, "allocationSpinLock", int.class);
        } catch(ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code PriorityBlockingQueue} with the default
     * initial capacity (11) that orders its elements according to
     * their {@linkplain Comparable natural ordering}.
     */
    public PriorityBlockingQueue() {
        this(DEFAULT_INITIAL_CAPACITY, null);
    }
    
    /**
     * Creates a {@code PriorityBlockingQueue} with the specified
     * initial capacity that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     *
     * @param initialCapacity the initial capacity for this priority queue
     *
     * @throws IllegalArgumentException if {@code initialCapacity} is less
     *                                  than 1
     */
    public PriorityBlockingQueue(int initialCapacity) {
        this(initialCapacity, null);
    }
    
    /**
     * Creates a {@code PriorityBlockingQueue} with the specified initial
     * capacity that orders its elements according to the specified
     * comparator.
     *
     * @param initialCapacity the initial capacity for this priority queue
     * @param comparator      the comparator that will be used to order this
     *                        priority queue.  If {@code null}, the {@linkplain Comparable
     *                        natural ordering} of the elements will be used.
     *
     * @throws IllegalArgumentException if {@code initialCapacity} is less
     *                                  than 1
     */
    public PriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
        if(initialCapacity<1) {
            throw new IllegalArgumentException();
        }
        this.comparator = comparator;
        this.queue = new Object[Math.max(1, initialCapacity)];
    }
    
    /**
     * Creates a {@code PriorityBlockingQueue} containing the elements
     * in the specified collection.  If the specified collection is a
     * {@link SortedSet} or a {@link PriorityQueue}, this
     * priority queue will be ordered according to the same ordering.
     * Otherwise, this priority queue will be ordered according to the
     * {@linkplain Comparable natural ordering} of its elements.
     *
     * @param c the collection whose elements are to be placed
     *          into this priority queue
     *
     * @throws ClassCastException   if elements of the specified collection
     *                              cannot be compared to one another according to the priority
     *                              queue's ordering
     * @throws NullPointerException if the specified collection or any
     *                              of its elements are null
     */
    public PriorityBlockingQueue(Collection<? extends E> c) {
        // 是否需要重建堆
        boolean heapify = true; // true if not known to be in heap order
        // 是否需要筛选空值
        boolean screen = true;  // true if must screen for nulls
        
        if(c instanceof SortedSet<?>) {
            SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
            this.comparator = (Comparator<? super E>) ss.comparator();
            // SortedSet已经有序了，不需要重建堆
            heapify = false;
        } else if(c instanceof PriorityBlockingQueue<?>) {
            PriorityBlockingQueue<? extends E> pq = (PriorityBlockingQueue<? extends E>) c;
            this.comparator = (Comparator<? super E>) pq.comparator();
            // 来自于PriorityBlockingQueue的元素已经排除了空值，所以不需要再筛选
            screen = false;
            // 如果是从PriorityBlockingQueue初始化的，也不需要重建堆
            if(pq.getClass() == PriorityBlockingQueue.class) { // exact match
                heapify = false;
            }
        }
        
        // 获取容器中的元素
        Object[] es = c.toArray();
        int n = es.length;
        
        // If c.toArray incorrectly doesn't return Object[], copy it.
        if(es.getClass() != Object[].class) {
            es = Arrays.copyOf(es, n, Object[].class);
        }
        
        // 必要时，筛选空值
        if(screen && (n == 1 || this.comparator != null)) {
            for(Object e : es) {
                if(e == null) {
                    throw new NullPointerException();
                }
            }
        }
        
        this.queue = ensureNonEmpty(es);
        this.size = n;
        
        // 必要时，重建小顶堆
        if(heapify) {
            heapify();
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 入队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Inserts the specified element into this priority queue.
     * As the queue is unbounded, this method will never return {@code false}.
     *
     * @param e the element to add
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     *
     * @throws ClassCastException   if the specified element cannot be compared
     *                              with elements currently in the priority queue according to the
     *                              priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    // 入队，线程安全，无法入队时扩容，不阻塞
    public boolean offer(E e) {
        if(e == null) {
            throw new NullPointerException();
        }
        
        final ReentrantLock lock = this.lock;
        lock.lock();
        
        int n, cap;
        Object[] es;
        
        // 扩容
        while((n = size) >= (cap = (es = queue).length)) {
            tryGrow(es, cap);
        }
        
        try {
            /* 插入。需要从小顶堆的结点n开始，向【上】查找一个合适的位置插入e */
            
            final Comparator<? super E> cmp;
            if((cmp = comparator) == null) {
                siftUpComparable(n, e, es);
            } else {
                siftUpUsingComparator(n, e, es, cmp);
            }
            
            // 队列长度增一
            size = n + 1;
            
            // 唤醒出队线程
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        
        return true;
    }
    
    /**
     * Inserts the specified element into this priority queue.
     * As the queue is unbounded, this method will never block or
     * return {@code false}.
     *
     * @param e       the element to add
     * @param timeout This parameter is ignored as the method never blocks
     * @param unit    This parameter is ignored as the method never blocks
     *
     * @return {@code true} (as specified by
     * {@link BlockingQueue#offer(Object, long, TimeUnit) BlockingQueue.offer})
     *
     * @throws ClassCastException   if the specified element cannot be compared
     *                              with elements currently in the priority queue according to the
     *                              priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    // 入队，线程安全，无法入队时扩容，不阻塞
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e); // never need to block
    }
    
    
    /**
     * Inserts the specified element into this priority queue.
     * As the queue is unbounded, this method will never block.
     *
     * @param e the element to add
     *
     * @throws ClassCastException   if the specified element cannot be compared
     *                              with elements currently in the priority queue according to the
     *                              priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    // 入队，线程安全，无法入队时扩容，不阻塞
    public void put(E e) {
        offer(e); // never need to block
    }
    
    
    /**
     * Inserts the specified element into this priority queue.
     *
     * @param e the element to add
     *
     * @return {@code true} (as specified by {@link Collection#add})
     *
     * @throws ClassCastException   if the specified element cannot be compared
     *                              with elements currently in the priority queue according to the
     *                              priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    // 入队/添加，线程安全，无法入队时扩容，不阻塞
    public boolean add(E e) {
        return offer(e);
    }
    
    /*▲ 入队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 出队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 出队，线程安全，无法出队时返回null，不阻塞
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return dequeue();
        } finally {
            lock.unlock();
        }
    }
    
    // 出队，线程安全，无法出队时阻塞一段时间，超时后无法出队则返回null
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        E result;
        try {
            while((result = dequeue()) == null && nanos>0) {
                nanos = notEmpty.awaitNanos(nanos);
            }
        } finally {
            lock.unlock();
        }
        return result;
    }
    
    
    // 出队，线程安全，无法出队时阻塞
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        E result;
        try {
            while((result = dequeue()) == null) {
                notEmpty.await();
            }
        } finally {
            lock.unlock();
        }
        return result;
    }
    
    
    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.  Returns {@code true} if and only if this queue contained
     * the specified element (or equivalently, if this queue changed as a
     * result of the call).
     *
     * @param o element to be removed from this queue, if present
     *
     * @return {@code true} if this queue changed as a result of the call
     */
    // 移除，非线程安全，移除成功则返回true
    public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 获取指定元素在队列中的索引，不在队列中时返回-1
            int i = indexOf(o);
            
            // 如果元素不在队列中，返回false
            if(i == -1) {
                return false;
            }
            
            // 移除队列索引i处的元素
            removeAt(i);
            
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Removes the ith element from queue.
     */
    // 移除队列索引i处的元素
    private void removeAt(int i) {
        final Object[] es = queue;
        final int n = size - 1;
        
        // 如果移除的是队尾元素
        if(n == i) {    // removed last element
            es[i] = null;
        } else {
            /* 如果不是移除队尾元素，则需要将队尾元素防止在新的小顶堆中的合适位置 */
            
            // 摘下队尾元素
            E moved = (E) es[n];
            
            es[n] = null;
            
            final Comparator<? super E> cmp;
            
            /* 插入。需要从小顶堆的结点i开始，向【下】查找一个合适的位置插入moved */
            
            if((cmp = comparator) == null) {
                siftDownComparable(i, moved, es, n);
            } else {
                siftDownUsingComparator(i, moved, es, n, cmp);
            }
            
            /*
             * 如果待moved是以i为根结点的小顶堆上的最小值，
             * 那么不能保证moved比结点i的父结点元素更大，
             * 此时需要向上搜寻moved的一个合适的插入位置
             */
            if(es[i] == moved) {
                /* 插入。需要从小顶堆的结点i开始，向【上】查找一个合适的位置插入x */
                
                if(cmp == null) {
                    siftUpComparable(i, moved, es);
                } else {
                    siftUpUsingComparator(i, moved, es, cmp);
                }
            }
        }
        size = n;
    }
    
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 移除所有满足过滤条件的元素（线程安全）
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        return bulkRemove(filter);
    }
    
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // (匹配则移除)移除队列中所有与给定容器中的元素匹配的元素（线程安全）
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> c.contains(e));
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // (不匹配则移除)移除队列中所有与给定容器中的元素不匹配的元素（线程安全）
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> !c.contains(e));
    }
    
    
    /**
     * Atomically removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     */
    // 清空，即移除所有元素（线程安全）
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Object[] es = queue;
            for(int i = 0, n = size; i<n; i++) {
                es[i] = null;
            }
            size = 0;
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    // 将队列中所有元素移除，并转移到给定的容器当中（线程安全）
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }
    
    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    // 将队列中前maxElements个元素移除，并转移到给定的容器当中（线程安全）
    public int drainTo(Collection<? super E> c, int maxElements) {
        Objects.requireNonNull(c);
        if(c == this) {
            throw new IllegalArgumentException();
        }
        
        if(maxElements<=0) {
            return 0;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(size, maxElements);
            for(int i = 0; i<n; i++) {
                // 任务添加到容器中
                c.add((E) queue[0]); // In this order, in case add() throws.
                // 移除队头任务
                dequeue();
            }
            return n;
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 出队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取队头元素
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (E) queue[0];
        } finally {
            lock.unlock();
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
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return indexOf(o) != -1;
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an array containing all of the elements in this queue.
     * The returned array elements are in no particular order.
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
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return Arrays.copyOf(queue, size);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Returns an array containing all of the elements in this queue; the
     * runtime type of the returned array is that of the specified array.
     * The returned array elements are in no particular order.
     * If the queue fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of this queue.
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
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = size;
            if(a.length<n)
                // Make a new array of a's runtime type, but my contents:
                return (T[]) Arrays.copyOf(queue, size, a.getClass());
            System.arraycopy(queue, 0, a, 0, n);
            if(a.length>n)
                a[n] = null;
            return a;
        } finally {
            lock.unlock();
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
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Object[] es = queue;
            for(int i = 0, n = size; i<n; i++) {
                action.accept((E) es[i]);
            }
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * Returns an iterator over the elements in this queue. The
     * iterator does not return the elements in any particular order.
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this queue
     */
    // 返回当前队列的迭代器
    public Iterator<E> iterator() {
        return new Itr(toArray());
    }
    
    /**
     * Returns a {@link Spliterator} over the elements in this queue.
     * The spliterator does not traverse elements in any particular order
     * (the {@link Spliterator#ORDERED ORDERED} characteristic is not reported).
     *
     * <p>The returned spliterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#NONNULL}.
     *
     * @return a {@code Spliterator} over the elements in this queue
     *
     * @implNote The {@code Spliterator} additionally reports {@link Spliterator#SUBSIZED}.
     * @since 1.8
     */
    // 返回描述此队列中元素的Spliterator
    public Spliterator<E> spliterator() {
        return new PBQSpliterator();
    }
    
    /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回队列中元素数量
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return size;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Always returns {@code Integer.MAX_VALUE} because
     * a {@code PriorityBlockingQueue} is not capacity constrained.
     *
     * @return {@code Integer.MAX_VALUE} always
     */
    // 计算队列剩余空间，返回Integer.MAX_VALUE代表无限
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }
    
    /**
     * Returns the comparator used to order the elements in this queue,
     * or {@code null} if this queue uses the {@linkplain Comparable
     * natural ordering} of its elements.
     *
     * @return the comparator used to order the elements in this queue,
     * or {@code null} if this queue uses the natural
     * ordering of its elements
     */
    // 返回该队列使用的外部比较器
    public Comparator<? super E> comparator() {
        return comparator;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static final long serialVersionUID = 5595510919245408276L;
    
    /**
     * A plain PriorityQueue used only for serialization,
     * to maintain compatibility with previous versions
     * of this class. Non-null only during serialization/deserialization.
     */
    // 用于序列化
    private PriorityQueue<E> q;
    
    
    /**
     * Saves this queue to a stream (that is, serializes it).
     *
     * For compatibility with previous version of this class, elements
     * are first copied to a java.util.PriorityQueue, which is then
     * serialized.
     *
     * @param s the stream
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    private void writeObject(ObjectOutputStream s) throws java.io.IOException {
        lock.lock();
        try {
            // avoid zero capacity argument
            q = new PriorityQueue<E>(Math.max(size, 1), comparator);
            q.addAll(this);
            s.defaultWriteObject();
        } finally {
            q = null;
            lock.unlock();
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
    private void readObject(ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        try {
            s.defaultReadObject();
            int sz = q.size();
            SharedSecrets.getJavaObjectInputStreamAccess().checkArray(s, Object[].class, sz);
            this.queue = new Object[Math.max(1, sz)];
            comparator = q.comparator();
            addAll(q);
        } finally {
            q = null;
        }
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 小顶堆 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Inserts item x at position k, maintaining heap invariant by
     * promoting x up the tree until it is greater than or equal to
     * its parent, or is the root.
     *
     * To simplify and speed up coercions and comparisons, the
     * Comparable and Comparator versions are separated into different
     * methods that are otherwise identical. (Similarly for siftDown.)
     *
     * @param i  the position to fill
     * @param x  the item to insert
     * @param es the heap array
     */
    // 插入。需要从小顶堆的结点i开始，向【上】查找一个合适的位置插入x
    private static <T> void siftUpComparable(int i, T x, Object[] es) {
        // 类型转换，要求待插入元素必须实现Comparable接口
        Comparable<? super T> key = (Comparable<? super T>) x;
        
        while(i>0) {
            // 获取父结点索引
            int parent = (i - 1) >>> 1;
            
            // 父结点
            Object e = es[parent];
            
            // 如果待插入元素大于父节点中的元素，则退出循环
            if(key.compareTo((T) e) >= 0) {
                break;
            }
            
            // 子结点保存父结点中的元素
            es[i] = e;
            
            // 向上搜寻合适的插入位置
            i = parent;
        }
        
        // 将元素key插入到合适的位置
        es[i] = key;
    }
    
    // 插入。需要从小顶堆的结点i开始，向【上】查找一个合适的位置插入x
    private static <T> void siftUpUsingComparator(int i, T x, Object[] es, Comparator<? super T> cmp) {
        while(i>0) {
            // 获取父结点索引
            int parent = (i - 1) >>> 1;
            
            // 父结点
            Object e = es[parent];
            
            // 如果待插入元素大于父节点中的元素，则退出循环
            if(cmp.compare(x, (T) e) >= 0) {
                break;
            }
            
            // 子结点保存父结点中的元素
            es[i] = e;
            
            // 向上搜寻合适的插入位置
            i = parent;
        }
        
        // 将元素x插入到合适的位置
        es[i] = x;
    }
    
    /**
     * Inserts item x at position k, maintaining heap invariant by
     * demoting x down the tree repeatedly until it is less than or
     * equal to its children or is a leaf.
     *
     * @param i  the position to fill
     * @param x  the item to insert
     * @param es the heap array
     * @param n  heap size
     */
    // 插入。需要从小顶堆的结点i开始，向【下】查找一个合适的位置插入x
    private static <T> void siftDownComparable(int i, T x, Object[] es, int n) {
        // 类型转换，要求待插入元素必须实现Comparable接口
        Comparable<? super T> key = (Comparable<? super T>) x;
        
        // 最多搜索一半元素
        int half = n >>> 1;           // loop while a non-leaf
        
        while(i<half) {
            int child = (i << 1) + 1;   // 左结点索引
            int right = child + 1;      // 右结点索引
            
            // 假设左结点为较小的结点
            Object c = es[child];
            
            // 如果右结点更小
            if(right<n && ((Comparable<? super T>) c).compareTo((T) es[right])>0) {
                // 更新min指向子结点中较小的结点
                c = es[child = right];
            }
            
            // 如果待插入元素小于子结点中较小的元素，则退出循环
            if(key.compareTo((T) c)<=0) {
                break;
            }
            
            // 父结点位置保存子结点中较小的元素
            es[i] = c;
            
            // 向下搜寻合适的插入位置
            i = child;
        }
        
        // 将元素key插入到合适的位置
        es[i] = key;
    }
    
    // 插入。需要从小顶堆的结点i开始，向【下】查找一个合适的位置插入x
    private static <T> void siftDownUsingComparator(int i, T x, Object[] es, int n, Comparator<? super T> cmp) {
        // 最多搜索一半元素
        int half = n >>> 1;
        
        while(i<half) {
            int child = (i << 1) + 1;   // 左结点索引
            int right = child + 1;      // 右结点索引
            
            // 假设左结点为较小的结点
            Object c = es[child];
            
            // 如果右结点更小
            if(right<n && cmp.compare((T) c, (T) es[right])>0) {
                // 更新min指向子结点中较小的结点
                c = es[child = right];
            }
            
            // 如果待插入元素小于子结点中较小的元素，则退出循环
            if(cmp.compare(x, (T) c)<=0) {
                break;
            }
            
            // 父结点位置保存子结点中较小的元素
            es[i] = c;
            
            // 向下搜寻合适的插入位置
            i = child;
        }
        
        // 将元素x插入到合适的位置
        es[i] = x;
    }
    
    /**
     * Establishes the heap invariant (described above) in the entire tree,
     * assuming nothing about the order of the elements prior to the call.
     * This classic algorithm due to Floyd (1964) is known to be O(size).
     */
    // 重建小顶堆
    private void heapify() {
        final Object[] es = queue;
        
        int n = size;
        
        // 从中间开始，倒着往回遍历
        int i = (n >>> 1) - 1;
        
        final Comparator<? super E> cmp;
        
        /* 插入。需要从小顶堆的结点i开始，向【下】查找一个合适的位置插入es[i] */
        
        if((cmp = comparator) == null) {
            for(; i >= 0; i--) {
                siftDownComparable(i, es[i], es, n);
            }
        } else {
            for(; i >= 0; i--) {
                siftDownUsingComparator(i, (E) es[i], es, n, cmp);
            }
        }
    }
    
    /*▲ 小顶堆 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    public String toString() {
        return Helpers.collectionToString(this);
    }
    
    
    
    /** Ensures that queue[0] exists, helping peek() and poll(). */
    // 确保返回的数组长度>=1
    private static Object[] ensureNonEmpty(Object[] es) {
        return (es.length>0) ? es : new Object[1];
    }
    
    /**
     * Tries to grow array to accommodate at least one more element
     * (but normally expand by about 50%), giving up (allowing retry)
     * on contention (which we expect to be rare). Call only while
     * holding lock.
     *
     * @param array  the heap array
     * @param oldCap the length of the array
     */
    // 扩容
    private void tryGrow(Object[] array, int oldCap) {
        lock.unlock(); // must release and then re-acquire main lock
        
        Object[] newArray = null;
        
        if(allocationSpinLock == 0 && ALLOCATIONSPINLOCK.compareAndSet(this, 0, 1)) {
            try {
                int newCap = oldCap + ((oldCap<64)
                    ? (oldCap + 2)  // grow faster if small
                    : (oldCap >> 1));
                
                if(newCap - MAX_ARRAY_SIZE>0) {    // possible overflow
                    int minCap = oldCap + 1;
                    if(minCap<0 || minCap>MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError();
                    }
                    newCap = MAX_ARRAY_SIZE;
                }
                
                if(newCap>oldCap && queue == array) {
                    newArray = new Object[newCap];
                }
            } finally {
                allocationSpinLock = 0;
            }
        }
        
        // back off if another thread is allocating
        if(newArray == null) {
            Thread.yield();
        }
        
        lock.lock();
        
        if(newArray != null && queue==array) {
            queue = newArray;
            System.arraycopy(array, 0, newArray, 0, oldCap);
        }
    }
    
    // 获取指定元素在队列中的索引，不在队列中时返回-1
    private int indexOf(Object o) {
        if(o != null) {
            final Object[] es = queue;
            for(int i = 0, n = size; i<n; i++) {
                if(o.equals(es[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * Mechanics for poll().  Call only while holding lock.
     */
    // 移除并返回队头元素，非线程安全，仅限内部使用
    private E dequeue() {
        // assert lock.isHeldByCurrentThread();
        final Object[] es = queue;
        
        // 获取队头元素
        final E result = (E)es[0];
        
        // 如果队头元素不为空
        if(result != null) {
            final int n = --size;
            
            // 摘下队尾元素
            final E x = (E) es[n];
            es[n] = null;
            
            // 插入。需要从小顶堆的根结点开始，向【下】查找一个合适的位置插入队尾元素
            if(n>0) {
                final Comparator<? super E> cmp;
                if((cmp = comparator) == null) {
                    siftDownComparable(0, x, es, n);
                } else {
                    siftDownUsingComparator(0, x, es, n, cmp);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Identity-based version for use in Itr.remove.
     *
     * @param o element to be removed from this queue, if present
     */
    // 移除指定元素
    void removeEq(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Object[] es = queue;
            for(int i = 0, n = size; i<n; i++) {
                if(o == es[i]) {
                    removeAt(i);
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    /** Implementation of bulk remove methods. */
    // 批量移除元素，即满足过滤条件的元素将被移除（线程安全）
    private boolean bulkRemove(Predicate<? super E> filter) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        
        try {
            final Object[] es = queue;
            final int end = size;
            int i;
            
            // 跳过开头可以保留的元素
            for(i = 0; i<end && !filter.test((E) es[i]); i++)
                ;
            
            // 所有元素均可保留
            if(i >= end) {
                return false;
            }
            
            // Tolerate predicates that reentrantly access the
            // collection for read, so traverse once to find elements
            // to delete, a second pass to physically expunge.
            final int beg = i;
            final long[] deathRow = nBits(end - beg);
            deathRow[0] = 1L;   // set bit 0
            for(i = beg + 1; i<end; i++) {
                if(filter.test((E) es[i])) {
                    setBit(deathRow, i - beg);
                }
            }
            
            int w = beg;
            for(i = beg; i<end; i++) {
                if(isClear(deathRow, i - beg)) {
                    es[w++] = es[i];
                }
            }
            
            for(i = size = w; i<end; i++) {
                es[i] = null;
            }
            
            // 重建小顶堆
            heapify();
            
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    private static long[] nBits(int n) {
        return new long[((n - 1) >> 6) + 1];
    }
    
    private static void setBit(long[] bits, int i) {
        bits[i >> 6] |= 1L << i;
    }
    
    private static boolean isClear(long[] bits, int i) {
        return (bits[i >> 6] & (1L << i)) == 0;
    }
    
    
    
    
    
    
    /**
     * Snapshot iterator that works off copy of underlying q array.
     */
    // 用于当前队列的外部迭代器
    final class Itr implements Iterator<E> {
        final Object[] array; // Array of all elements
        int cursor;           // index of next element to return
        int lastRet = -1;     // index of last element, or -1 if no such
        
        // 拷贝源数据
        Itr(Object[] array) {
            this.array = array;
        }
        
        public boolean hasNext() {
            return cursor<array.length;
        }
        
        public E next() {
            if(cursor >= array.length) {
                throw new NoSuchElementException();
            }
            return (E) array[lastRet = cursor++];
        }
        
        // 移除刚刚遍历的那个元素
        public void remove() {
            if(lastRet<0) {
                throw new IllegalStateException();
            }
            removeEq(array[lastRet]);
            lastRet = -1;
        }
        
        // 遍历剩余元素，并对其执行相应的择取操作
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final Object[] es = array;
            int i;
            if((i = cursor)<es.length) {
                lastRet = -1;
                cursor = es.length;
                for(; i<es.length; i++) {
                    action.accept((E) es[i]);
                }
                lastRet = es.length - 1;
            }
        }
    }
    
    /**
     * Immutable snapshot spliterator that binds to elements "late".
     */
    // 描述此队列中元素的Spliterator
    final class PBQSpliterator implements Spliterator<E> {
        Object[] array;        // null until late-bound-initialized
        int index;
        int fence;
        
        PBQSpliterator() {
        }
        
        PBQSpliterator(Object[] array, int index, int fence) {
            this.array = array;
            this.index = index;
            this.fence = fence;
        }
        
        public PBQSpliterator trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null : new PBQSpliterator(array, lo, index = mid);
        }
        
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final int hi = getFence(), lo = index;
            final Object[] es = array;
            index = hi;                 // ensure exhaustion
            for(int i = lo; i<hi; i++)
                action.accept((E) es[i]);
        }
        
        public boolean tryAdvance(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            if(getFence()>index && index >= 0) {
                action.accept((E) array[index++]);
                return true;
            }
            return false;
        }
        
        public long estimateSize() {
            return getFence() - index;
        }
        
        public int characteristics() {
            return (Spliterator.NONNULL | Spliterator.SIZED | Spliterator.SUBSIZED);
        }
        
        private int getFence() {
            if(array == null)
                fence = (array = toArray()).length;
            return fence;
        }
    }
}
