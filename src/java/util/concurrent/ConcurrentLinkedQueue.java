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
 * Written by Doug Lea and Martin Buchholz with assistance from members of
 * JCP JSR-166 Expert Group and released to the public domain, as explained
 * at http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An unbounded thread-safe {@linkplain Queue queue} based on linked nodes.
 * This queue orders elements FIFO (first-in-first-out).
 * The <em>head</em> of the queue is that element that has been on the
 * queue the longest time.
 * The <em>tail</em> of the queue is that element that has been on the
 * queue the shortest time. New elements
 * are inserted at the tail of the queue, and the queue retrieval
 * operations obtain elements at the head of the queue.
 * A {@code ConcurrentLinkedQueue} is an appropriate choice when
 * many threads will share access to a common collection.
 * Like most other concurrent collection implementations, this class
 * does not permit the use of {@code null} elements.
 *
 * <p>This implementation employs an efficient <em>non-blocking</em>
 * algorithm based on one described in
 * <a href="http://www.cs.rochester.edu/~scott/papers/1996_PODC_queues.pdf">
 * Simple, Fast, and Practical Non-Blocking and Blocking Concurrent Queue
 * Algorithms</a> by Maged M. Michael and Michael L. Scott.
 *
 * <p>Iterators are <i>weakly consistent</i>, returning elements
 * reflecting the state of the queue at some point at or since the
 * creation of the iterator.  They do <em>not</em> throw {@link
 * java.util.ConcurrentModificationException}, and may proceed concurrently
 * with other operations.  Elements contained in the queue since the creation
 * of the iterator will be returned exactly once.
 *
 * <p>Beware that, unlike in most collections, the {@code size} method
 * is <em>NOT</em> a constant-time operation. Because of the
 * asynchronous nature of these queues, determining the current number
 * of elements requires a traversal of the elements, and so may report
 * inaccurate results if this collection is modified during traversal.
 *
 * <p>Bulk operations that add, remove, or examine multiple elements,
 * such as {@link #addAll}, {@link #removeIf} or {@link #forEach},
 * are <em>not</em> guaranteed to be performed atomically.
 * For example, a {@code forEach} traversal concurrent with an {@code
 * addAll} operation might observe only some of the added elements.
 *
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Queue} and {@link Iterator} interfaces.
 *
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code ConcurrentLinkedQueue}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code ConcurrentLinkedQueue} in another thread.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this queue
 */
/*
 * 链式无界单向队列，线程安全（CAS）
 *
 * 入队/出队时，对头尾结点的更新上采用了懒加载机制
 * （非常懒的懒加载，即不到影响操作结果的时刻，不会更新头尾结点）
 */
public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, Serializable {
    
    /*
     * This is a modification of the Michael & Scott algorithm,
     * adapted for a garbage-collected environment, with support for
     * interior node deletion (to support e.g. remove(Object)).  For
     * explanation, read the paper.
     *
     * Note that like most non-blocking algorithms in this package,
     * this implementation relies on the fact that in garbage
     * collected systems, there is no possibility of ABA problems due
     * to recycled nodes, so there is no need to use "counted
     * pointers" or related techniques seen in versions used in
     * non-GC'ed settings.
     *
     * The fundamental invariants are:
     * - There is exactly one (last) Node with a null next reference,
     *   which is CASed when enqueueing.  This last Node can be
     *   reached in O(1) time from tail, but tail is merely an
     *   optimization - it can always be reached in O(N) time from
     *   head as well.
     * - The elements contained in the queue are the non-null items in
     *   Nodes that are reachable from head.  CASing the item
     *   reference of a Node to null atomically removes it from the
     *   queue.  Reachability of all elements from head must remain
     *   true even in the case of concurrent modifications that cause
     *   head to advance.  A dequeued Node may remain in use
     *   indefinitely due to creation of an Iterator or simply a
     *   poll() that has lost its time slice.
     *
     * The above might appear to imply that all Nodes are GC-reachable
     * from a predecessor dequeued Node.  That would cause two problems:
     * - allow a rogue Iterator to cause unbounded memory retention
     * - cause cross-generational linking of old Nodes to new Nodes if
     *   a Node was tenured while live, which generational GCs have a
     *   hard time dealing with, causing repeated major collections.
     * However, only non-deleted Nodes need to be reachable from
     * dequeued Nodes, and reachability does not necessarily have to
     * be of the kind understood by the GC.  We use the trick of
     * linking a Node that has just been dequeued to itself.  Such a
     * self-link implicitly means to advance to head.
     *
     * Both head and tail are permitted to lag.  In fact, failing to
     * update them every time one could is a significant optimization
     * (fewer CASes). As with LinkedTransferQueue (see the internal
     * documentation for that class), we use a slack threshold of two;
     * that is, we update head/tail when the current pointer appears
     * to be two or more steps away from the first/last node.
     *
     * Since head and tail are updated concurrently and independently,
     * it is possible for tail to lag behind head (why not)?
     *
     * CASing a Node's item reference to null atomically removes the
     * element from the queue, leaving a "dead" node that should later
     * be unlinked (but unlinking is merely an optimization).
     * Interior element removal methods (other than Iterator.remove())
     * keep track of the predecessor node during traversal so that the
     * node can be CAS-unlinked.  Some traversal methods try to unlink
     * any deleted nodes encountered during traversal.  See comments
     * in bulkRemove.
     *
     * When constructing a Node (before enqueuing it) we avoid paying
     * for a volatile write to item.  This allows the cost of enqueue
     * to be "one-and-a-half" CASes.
     *
     * Both head and tail may or may not point to a Node with a
     * non-null item.  If the queue is empty, all items must of course
     * be null.  Upon creation, both head and tail refer to a dummy
     * Node with null item.  Both head and tail are only updated using
     * CAS, so they never regress, although again this is merely an
     * optimization.
     */
    
    /**
     * A node from which the first live (non-deleted) node (if any)
     * can be reached in O(1) time.
     * Invariants:
     * - all live nodes are reachable from head via succ()
     * - head != null
     * - (tmp = head).next != tmp || tmp != head
     * Non-invariants:
     * - head.item may or may not be null.
     * - it is permitted for tail to lag behind head, that is, for tail to not be reachable from head!
     */
    transient volatile Node<E> head;            // 队头
    
    /**
     * A node from which the last node on list (that is, the unique
     * node with node.next == null) can be reached in O(1) time.
     * Invariants:
     * - the last node is always reachable from tail via succ()
     * - tail != null
     * Non-invariants:
     * - tail.item may or may not be null.
     * - it is permitted for tail to lag behind head, that is, for tail to not be reachable from head!
     * - tail.next may or may not be self-linked.
     */
    private transient volatile Node<E> tail;    // 队尾
    
    /**
     * Tolerate this many consecutive dead nodes before CAS-collapsing.
     * Amortized cost of clear() is (1 + 1/MAX_HOPS) CASes per element.
     */
    private static final int MAX_HOPS = 8;
    
    
    // VarHandle mechanics
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    static final VarHandle ITEM;
    static final VarHandle NEXT;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(ConcurrentLinkedQueue.class, "head", Node.class);
            TAIL = l.findVarHandle(ConcurrentLinkedQueue.class, "tail", Node.class);
            ITEM = l.findVarHandle(Node.class, "item", Object.class);
            NEXT = l.findVarHandle(Node.class, "next", Node.class);
        } catch(ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code ConcurrentLinkedQueue} that is initially empty.
     */
    public ConcurrentLinkedQueue() {
        head = tail = new Node<E>();
    }
    
    /**
     * Creates a {@code ConcurrentLinkedQueue}
     * initially containing the elements of the given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param c the collection of elements to initially contain
     *
     * @throws NullPointerException if the specified collection or any
     *                              of its elements are null
     */
    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        Node<E> h = null, t = null;
        
        for(E e : c) {
            Node<E> newNode = new Node<E>(Objects.requireNonNull(e));
            if(h == null) {
                h = t = newNode;
            } else {
                t.appendRelaxed(t = newNode);
            }
        }
        
        if(h == null) {
            h = t = new Node<E>();
        }
        
        head = h;
        tail = t;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 入队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     *
     * @throws NullPointerException if the specified element is null
     */
    // 入队，不会队满，不会阻塞（线程安全）
    public boolean offer(E e) {
        // 创建结点
        final Node<E> newNode = new Node<E>(Objects.requireNonNull(e));
        
        for(Node<E> t = tail, p = t; ; ) {
            Node<E> q = p.next;
            
            if(q == null) {
                /* p是最后一个结点 */
                
                // 将新结点添加到队尾
                if(NEXT.compareAndSet(p, null, newNode)) {
                    /*
                     * Successful CAS is the linearization point for e to become an element of this queue,
                     * and for newNode to become "live".
                     */
                    
                    // 如果队尾滞后，更新队尾到新的末端
                    if(p != t) {
                        // hop two nodes at a time; failure is OK
                        TAIL.weakCompareAndSet(this, t, newNode);
                    }
                    
                    return true;
                }
                // Lost CAS race to another thread; re-read next
            } else if(p == q) {
                /*
                 * We have fallen off list.
                 * If tail is unchanged, it will also be off-list,
                 * in which case we need to jump to head,
                 * from which all live nodes are always reachable.
                 * Else the new tail is a better bet.
                 */
                p = (t != (t = tail)) ? t : head;
            } else {
                // Check for tail updates after two hops.
                p = (p != t && t != (t = tail)) ? t : q;
            }
        }
    }
    
    
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never throw
     * {@link IllegalStateException} or return {@code false}.
     *
     * @return {@code true} (as specified by {@link Collection#add})
     *
     * @throws NullPointerException if the specified element is null
     */
    // 入队/添加，不会队满，不会阻塞（线程安全）
    public boolean add(E e) {
        return offer(e);
    }
    
    /**
     * Appends all of the elements in the specified collection to the end of
     * this queue, in the order that they are returned by the specified
     * collection's iterator.  Attempts to {@code addAll} of a queue to
     * itself result in {@code IllegalArgumentException}.
     *
     * @param c the elements to be inserted into this queue
     *
     * @return {@code true} if this queue changed as a result of the call
     *
     * @throws NullPointerException     if the specified collection or any
     *                                  of its elements are null
     * @throws IllegalArgumentException if the collection is this queue
     */
    // 将容器中所有元素复制到队列当中
    public boolean addAll(Collection<? extends E> c) {
        if(c == this) {
            // As historically specified in AbstractQueue#addAll
            throw new IllegalArgumentException();
        }
        
        // Copy c into a private chain of Nodes
        Node<E> beginningOfTheEnd = null, last = null;
        for(E e : c) {
            Node<E> newNode = new Node<E>(Objects.requireNonNull(e));
            if(beginningOfTheEnd == null) {
                beginningOfTheEnd = last = newNode;
            } else {
                last.appendRelaxed(last = newNode);
            }
        }
        
        if(beginningOfTheEnd == null) {
            return false;
        }
        
        // Atomically append the chain at the tail of this collection
        for(Node<E> t = tail, p = t; ; ) {
            Node<E> q = p.next;
            if(q == null) {
                // p is last node
                if(NEXT.compareAndSet(p, null, beginningOfTheEnd)) {
                    // Successful CAS is the linearization point
                    // for all elements to be added to this queue.
                    if(!TAIL.weakCompareAndSet(this, t, last)) {
                        // Try a little harder to update tail,
                        // since we may be adding many elements.
                        t = tail;
                        if(last.next == null)
                            TAIL.weakCompareAndSet(this, t, last);
                    }
                    return true;
                }
                // Lost CAS race to another thread; re-read next
            } else if(p == q)
                // We have fallen off list.  If tail is unchanged, it
                // will also be off-list, in which case we need to
                // jump to head, from which all live nodes are always
                // reachable.  Else the new tail is a better bet.
                p = (t != (t = tail)) ? t : head;
            else {
                // Check for tail updates after two hops.
                p = (p != t && t != (t = tail)) ? t : q;
            }
        }
    }
    
    /*▲ 入队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 出队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 出队，队空时返回null，不会阻塞（线程安全）
    public E poll() {
restartFromHead:
        for(; ; ) {
            for(Node<E> h = head, p = h, q; ; p = q) {
                final E item = p.item;
                
                // 原子地更新item域为null
                if(item != null && p.casItem(item, null)) {
                    /* Successful CAS is the linearization point for item to be removed from this queue */
                    
                    // 如果p!=h，说明p初始指向的位置是个无效结点
                    if(p != h) {
                        // 更新head，一次跳两个结点
                        updateHead(h, ((q = p.next) != null) ? q : p);
                    }
                    
                    // 返回拿到的结点
                    return item;
                } else if((q = p.next) == null) {
                    /* 至此，说明p已经是最后一个结点，更新头结点，并返回null */
                    
                    updateHead(h, p);
                    
                    return null;
                } else {
                    /* 至此，说明p指向的结点中没有有效数据（被其他线程取走了） */
                    
                    if(p == q) {
                        /* 至此，说明p指向的结点成了无效结点（next域指向了自身），这时候需要重新定位头结点 */
                        
                        continue restartFromHead;
                    }
                }
            }
        }
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

restartFromHead:
        for(; ; ) {
            for(Node<E> p = head, pred = null; p != null; ) {
                Node<E> q = p.next;
                final E item = p.item;
                
                if(item != null) {
                    if(o.equals(item) && p.casItem(item, null)) {
                        skipDeadNodes(pred, p, p, q);
                        return true;
                    }
                    pred = p;
                    p = q;
                    continue;
                }
                
                for(Node<E> c = p; ; q = p.next) {
                    if(q == null || q.item != null) {
                        pred = skipDeadNodes(pred, c, p, q);
                        p = q;
                        break;
                    }
                    
                    if(p == (p = q)) {
                        continue restartFromHead;
                    }
                }
            }
            return false;
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
    
    
    // 清空，即移除所有元素，不阻塞（线程安全）
    public void clear() {
        bulkRemove(e -> true);
    }
    
    /*▲ 出队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取队头元素，线程安全
    public E peek() {
restartFromHead:
        for(; ; ) {
            for(Node<E> h = head, p = h, q; ; p = q) {
                final E item;
                if((item = p.item) != null || (q = p.next) == null) {
                    updateHead(h, p);
                    return item;
                } else if(p == q) {
                    continue restartFromHead;
                }
            }
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

restartFromHead:
        for(; ; ) {
            for(Node<E> p = head, pred = null; p != null; ) {
                Node<E> q = p.next;
                final E item;
                if((item = p.item) != null) {
                    if(o.equals(item))
                        return true;
                    pred = p;
                    p = q;
                    continue;
                }
                
                for(Node<E> c = p; ; q = p.next) {
                    if(q == null || q.item != null) {
                        pred = skipDeadNodes(pred, c, p, q);
                        p = q;
                        break;
                    }
                    
                    if(p == (p = q)) {
                        continue restartFromHead;
                    }
                }
            }
            return false;
        }
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 只读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
        return toArrayInternal(null);
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
        Objects.requireNonNull(a);
        return (T[]) toArrayInternal(a);
    }
    
    /*▲ 只读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 迭代 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 遍历所有元素，并执行相应的择取操作
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        forEachFrom(action, head);
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
    @Override
    public Spliterator<E> spliterator() {
        return new CLQSpliterator();
    }
    
    /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of elements in this queue.  If this queue
     * contains more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these queues, determining the current
     * number of elements requires an O(n) traversal.
     * Additionally, if elements are added or removed during execution
     * of this method, the returned result may be inaccurate.  Thus,
     * this method is typically not very useful in concurrent
     * applications.
     *
     * @return the number of elements in this queue
     */
    // 返回队列中元素数量
    public int size() {
restartFromHead:
        for(; ; ) {
            int count = 0;
            for(Node<E> p = first(); p != null; ) {
                if(p.item != null) {
                    if(++count == Integer.MAX_VALUE) {
                        break;  // @see Collection.size()
                    }
                }
                
                if(p == (p = p.next)) {
                    continue restartFromHead;
                }
            }
            return count;
        }
    }
    
    /**
     * Returns {@code true} if this queue contains no elements.
     *
     * @return {@code true} if this queue contains no elements
     */
    // 判断队列是否为null
    public boolean isEmpty() {
        return first() == null;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static final long serialVersionUID = 196745693267521676L;
    
    
    /**
     * Saves this queue to a stream (that is, serializes it).
     *
     * @param s the stream
     *
     * @throws java.io.IOException if an I/O error occurs
     * @serialData All of the elements (each an {@code E}) in
     * the proper order, followed by a null
     */
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        
        // Write out any hidden stuff
        s.defaultWriteObject();
        
        // Write out all elements in the proper order.
        for(Node<E> p = first(); p != null; p = succ(p)) {
            final E item;
            if((item = p.item) != null)
                s.writeObject(item);
        }
        
        // Use trailing null as sentinel
        s.writeObject(null);
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
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        
        // Read in elements until trailing null sentinel found
        Node<E> h = null, t = null;
        for(Object item; (item = s.readObject()) != null; ) {
            @SuppressWarnings("unchecked")
            Node<E> newNode = new Node<E>((E) item);
            if(h == null)
                h = t = newNode;
            else
                t.appendRelaxed(t = newNode);
        }
        if(h == null)
            h = t = new Node<E>();
        head = h;
        tail = t;
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 注：该方法会改变队列的结构，所以可能会导致调试过程中结果与预期不一致
    public String toString() {
        String[] a = null;
restartFromHead:
        for(; ; ) {
            int charLength = 0;
            int size = 0;
            
            // 让p指向头结点
            for(Node<E> p = first(); p != null; ) {
                final E item = p.item ;
                if(item != null) {
                    if(a == null) {
                        a = new String[4];
                    } else if(size == a.length) {
                        a = Arrays.copyOf(a, 2 * size);
                    }
                    String s = item.toString();
                    a[size++] = s;
                    charLength += s.length();
                }
                
                if(p == (p = p.next)) {
                    continue restartFromHead;
                }
            }
            
            if(size == 0) {
                return "[]";
            }
            
            return Helpers.toString(a, size, charLength);
        }
    }
    
    
    
    /**
     * Tries to CAS head to p.
     * If successful, repoint old head to itself as sentinel for succ(), below.
     */
    // 更新队头head到p，并丢弃旧的队头（丢弃结点的next域会指向自身，这个特性很重要，会影响后续的判断）
    final void updateHead(Node<E> h, Node<E> p) {
        // assert h != null && p != null && (h == p || h.item == null);
        if(h != p && HEAD.compareAndSet(this, h, p)) {
            NEXT.setRelease(h, h);
        }
    }
    
    /**
     * Returns the first live (non-deleted) node on list, or null if none.
     * This is yet another variant of poll/peek; here returning the
     * first node, not element.  We could make peek() a wrapper around
     * first(), but that would cost an extra volatile read of item,
     * and the need to add a retry loop to deal with the possibility
     * of losing a race to a concurrent poll().
     */
    // 获取头结点。如果头结点指向无效结点（存储了空值），则需要先更新头结点
    Node<E> first() {
restartFromHead:
        for(; ; ) {
            for(Node<E> h = head, p = h, q; ; p = q) {
                boolean hasItem = (p.item != null);
                
                // 如果遇到了有效的头结点，或者p已经是最后一个结点，则可以返回了
                if(hasItem || (q = p.next) == null) {
                    // 如果head==h，则令head=p，且丢弃h
                    updateHead(h, p);
                    return hasItem ? p : null;
                } else {
                    /* 至此，说明p指向的结点中没有有效数据（被其他线程取走了） */
                    
                    if(p == q) {
                        /* 至此，说明p指向的结点成了无效结点（next域指向了自身），这时候需要重新定位头结点 */
                        
                        continue restartFromHead;
                    }
                }
            }
        }
    }
    
    /**
     * Runs action on each element found during a traversal starting at p.
     * If p is null, the action is not run.
     */
    void forEachFrom(Consumer<? super E> action, Node<E> p) {
        for(Node<E> pred = null; p != null; ) {
            Node<E> q = p.next;
            final E item;
            if((item = p.item) != null) {
                action.accept(item);
                pred = p;
                p = q;
                continue;
            }
            for(Node<E> c = p; ; q = p.next) {
                if(q == null || q.item != null) {
                    pred = skipDeadNodes(pred, c, p, q);
                    p = q;
                    break;
                }
                if(p == (p = q)) {
                    pred = null;
                    p = head;
                    break;
                }
            }
        }
    }
    
    /**
     * Returns the successor of p, or the head node if p.next has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    final Node<E> succ(Node<E> p) {
        if(p == (p = p.next)) {
            p = head;
        }
        return p;
    }
    
    /**
     * Tries to CAS pred.next (or head, if pred is null) from c to p.
     * Caller must ensure that we're not unlinking the trailing node.
     */
    // 尝试将pred的后继从c更新为p（pred==null时，代表操作头结点）
    private boolean tryCasSuccessor(Node<E> pred, Node<E> c, Node<E> p) {
        // assert p != null;
        // assert c.item == null;
        // assert c != p;
        if(pred != null) {
            return NEXT.compareAndSet(pred, c, p);
        }
        
        if(HEAD.compareAndSet(this, c, p)) {
            // 将结点c作废（next域指向自身）
            NEXT.setRelease(c, c);
            return true;
        }
        
        return false;
    }
    
    /**
     * Collapse dead nodes between pred and q.
     *
     * @param pred the last known live node, or null if none
     * @param c    the first dead node
     * @param p    the last dead node
     * @param q    p.next: the next live node, or null if at end
     *
     * @return either old pred or p if pred dead or CAS failed
     */
    // 折叠无效结点
    private Node<E> skipDeadNodes(Node<E> pred, Node<E> c, Node<E> p, Node<E> q) {
        // assert pred != c;
        // assert p != q;
        // assert c.item == null;
        // assert p.item == null;
        if(q == null) {
            // Never unlink trailing node.
            if(c == p) {
                return pred;
            }
            q = p;
        }
        
        // 尝试将pred的后继从c更新为p（pred==null时，代表操作头结点）
        if(!tryCasSuccessor(pred, c, q)){
            return p;
        }
        
        if(pred==null){
            return pred;
        }
        
        return ITEM.get(pred) != null ? pred : p;
    }
    
    private Object[] toArrayInternal(Object[] a) {
        Object[] x = a;
restartFromHead:
        for(; ; ) {
            int size = 0;
            for(Node<E> p = first(); p != null; ) {
                final E item;
                if((item = p.item) != null) {
                    if(x == null)
                        x = new Object[4];
                    else if(size == x.length)
                        x = Arrays.copyOf(x, 2 * (size + 4));
                    x[size++] = item;
                }
                if(p == (p = p.next))
                    continue restartFromHead;
            }
            if(x == null)
                return new Object[0];
            else if(a != null && size<=a.length) {
                if(a != x)
                    System.arraycopy(x, 0, a, 0, size);
                if(size<a.length)
                    a[size] = null;
                return a;
            }
            return (size == x.length) ? x : Arrays.copyOf(x, size);
        }
    }
    
    /** Implementation of bulk remove methods. */
    private boolean bulkRemove(Predicate<? super E> filter) {
        boolean removed = false;
restartFromHead:
        for(; ; ) {
            int hops = MAX_HOPS;
            // c will be CASed to collapse intervening dead nodes between
            // pred (or head if null) and p.
            for(Node<E> p = head, c = p, pred = null, q; p != null; p = q) {
                q = p.next;
                final E item;
                boolean pAlive;
                if(pAlive = ((item = p.item) != null)) {
                    if(filter.test(item)) {
                        if(p.casItem(item, null)) {
                            removed = true;
                        }
                        pAlive = false;
                    }
                }
                if(pAlive || q == null || --hops == 0) {
                    // p might already be self-linked here, but if so:
                    // - CASing head will surely fail
                    // - CASing pred's next will be useless but harmless.
                    if((c != p && !tryCasSuccessor(pred, c, c = p)) || pAlive) {
                        // if CAS failed or alive, abandon old pred
                        hops = MAX_HOPS;
                        pred = p;
                        c = q;
                    }
                } else if(p == q) {
                    continue restartFromHead;
                }
            }
            return removed;
        }
    }
    
    
    
    
    
    
    // 队列结点
    static final class Node<E> {
        volatile E item;        // 当前元素
        volatile Node<E> next;  // 指向下一个结点
        
        /** Constructs a dead dummy node. */
        Node() {
        }
        
        /**
         * Constructs a node holding item.  Uses relaxed write because
         * item can only be seen after piggy-backing publication via CAS.
         */
        Node(E item) {
            ITEM.set(this, item);
        }
        
        void appendRelaxed(Node<E> next) {
            // assert next != null;
            // assert this.next == null;
            NEXT.set(this, next);
        }
        
        // 如果item==cmp，则更新item=val
        boolean casItem(E cmp, E val) {
            // assert item == cmp || item == null;
            // assert cmp != null;
            // assert val == null;
            return ITEM.compareAndSet(this, cmp, val);
        }
    }
    
    // 用于当前队列的外部迭代器
    private class Itr implements Iterator<E> {
        /**
         * Next node to return item for.
         */
        private Node<E> nextNode;
        
        /**
         * nextItem holds on to item fields because once we claim
         * that an element exists in hasNext(), we must return it in
         * the following next() call even if it was in the process of
         * being removed when hasNext() was called.
         */
        private E nextItem;
        
        /**
         * Node of the last returned item, to support remove.
         */
        private Node<E> lastRet;
        
        Itr() {
restartFromHead:
            for(; ; ) {
                Node<E> h, p, q;
                for(p = h = head; ; p = q) {
                    final E item;
                    if((item = p.item) != null) {
                        nextNode = p;
                        nextItem = item;
                        break;
                    } else if((q = p.next) == null) {
                        break;
                    } else if(p == q) {
                        continue restartFromHead;
                    }
                }
                updateHead(h, p);
                return;
            }
        }
        
        public boolean hasNext() {
            return nextItem != null;
        }
        
        public E next() {
            final Node<E> pred = nextNode;
            if(pred == null) {
                throw new NoSuchElementException();
            }
            // assert nextItem != null;
            lastRet = pred;
            E item = null;
            
            for(Node<E> p = succ(pred), q; ; p = q) {
                if(p == null || (item = p.item) != null) {
                    nextNode = p;
                    E x = nextItem;
                    nextItem = item;
                    return x;
                }
                // unlink deleted nodes
                if((q = succ(p)) != null) {
                    NEXT.compareAndSet(pred, p, q);
                }
            }
        }
        
        // Default implementation of forEachRemaining is "good enough".
        
        public void remove() {
            Node<E> l = lastRet;
            if(l == null) {
                throw new IllegalStateException();
            }
            // rely on a future traversal to relink.
            l.item = null;
            lastRet = null;
        }
    }
    
    /** A customized variant of Spliterators.IteratorSpliterator */
    // 描述此队列中元素的Spliterator
    final class CLQSpliterator implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        Node<E> current;    // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes
        
        public Spliterator<E> trySplit() {
            Node<E> p, q;
            
            if((p = current()) == null || (q = p.next) == null) {
                return null;
            }
            
            int i = 0, n = batch = Math.min(batch + 1, MAX_BATCH);
            Object[] a = null;
            
            do {
                final E e;
                if((e = p.item) != null) {
                    if(a == null)
                        a = new Object[n];
                    a[i++] = e;
                }
                if(p == (p = q)) {
                    p = first();
                }
            } while(p != null && (q = p.next) != null && i<n);
            
            setCurrent(p);
            
            return (i == 0)
                ? null
                : Spliterators.spliterator(a, 0, i, (Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT));
        }
        
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final Node<E> p;
            if((p = current()) != null) {
                current = null;
                exhausted = true;
                forEachFrom(action, p);
            }
        }
        
        public boolean tryAdvance(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Node<E> p;
            
            if((p = current()) != null) {
                E e;
                do {
                    e = p.item;
                    if(p == (p = p.next))
                        p = first();
                } while(e == null && p != null);
                
                setCurrent(p);
                
                if(e != null) {
                    action.accept(e);
                    return true;
                }
            }
            
            return false;
        }
        
        public long estimateSize() {
            return Long.MAX_VALUE;
        }
        
        public int characteristics() {
            return (Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
        }
        
        private void setCurrent(Node<E> p) {
            if((current = p) == null)
                exhausted = true;
        }
        
        private Node<E> current() {
            Node<E> p;
            if((p = current) == null && !exhausted) {
                setCurrent(p = first());
            }
            return p;
        }
    }
}
