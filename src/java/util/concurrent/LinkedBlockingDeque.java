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
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An optionally-bounded {@linkplain BlockingDeque blocking deque} based on linked nodes.
 *
 * <p>The optional capacity bound constructor argument serves as a
 * way to prevent excessive expansion. The capacity, if unspecified,
 * is equal to {@link Integer#MAX_VALUE}.  Linked nodes are
 * dynamically created upon each insertion unless this would bring the
 * deque above capacity.
 *
 * <p>Most operations run in constant time (ignoring time spent
 * blocking).  Exceptions include {@link #remove(Object) remove},
 * {@link #removeFirstOccurrence removeFirstOccurrence}, {@link
 * #removeLastOccurrence removeLastOccurrence}, {@link #contains
 * contains}, {@link #iterator iterator.remove()}, and the bulk
 * operations, all of which run in linear time.
 *
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Collection} and {@link Iterator} interfaces.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @since 1.6
 * @author  Doug Lea
 * @param <E> the type of elements held in this deque
 */
/*
 * 链式有界/无界（初始化时决定使用有界还是无界）双向阻塞队列，线程安全（锁）
 *
 * "双向"的特征意味着"入队"/"出队"操作均可以在队头或队尾完成
 */
public class LinkedBlockingDeque<E> extends AbstractQueue<E> implements BlockingDeque<E>, Serializable {
    
    /*
     * Implemented as a simple doubly-linked list protected by a
     * single lock and using conditions to manage blocking.
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
     * self-link implicitly means to jump to "first" (for next links)
     * or "last" (for prev links).
     */
    
    /*
     * We have "diamond" multiple interface/abstract class inheritance
     * here, and that introduces ambiguities. Often we want the
     * BlockingDeque javadoc combined with the AbstractQueue
     * implementation, so a lot of method specs are duplicated here.
     */
    
    /**
     * Pointer to first node.
     * Invariant: (first == null && last == null) ||
     * (first.prev == null && first.item != null)
     */
    transient Node<E> first;    // 队头
    
    /**
     * Pointer to last node.
     * Invariant: (first == null && last == null) ||
     * (last.next == null && last.item != null)
     */
    transient Node<E> last;     // 队尾
    
    /** Maximum number of items in the deque */
    private final int capacity;     // 队列容量。决定队列是有界还是无界
    
    /** Number of items in the deque */
    private transient int count;    // 长度（元素个数）
    
    
    /** Main lock guarding all access */
    // 队列锁
    final ReentrantLock lock = new ReentrantLock();
    
    /** Condition for waiting takes */
    // 出队条件
    private final Condition notEmpty = lock.newCondition();
    
    /** Condition for waiting puts */
    // 入队条件
    private final Condition notFull = lock.newCondition();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code LinkedBlockingDeque} with a capacity of
     * {@link Integer#MAX_VALUE}.
     */
    public LinkedBlockingDeque() {
        this(Integer.MAX_VALUE);
    }
    
    /**
     * Creates a {@code LinkedBlockingDeque} with the given (fixed) capacity.
     *
     * @param capacity the capacity of this deque
     *
     * @throws IllegalArgumentException if {@code capacity} is less than 1
     */
    public LinkedBlockingDeque(int capacity) {
        if(capacity<=0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
    }
    
    /**
     * Creates a {@code LinkedBlockingDeque} with a capacity of
     * {@link Integer#MAX_VALUE}, initially containing the elements of
     * the given collection, added in traversal order of the
     * collection's iterator.
     *
     * @param c the collection of elements to initially contain
     *
     * @throws NullPointerException if the specified collection or any
     *                              of its elements are null
     */
    // 用指定容器中的元素初始化队列
    public LinkedBlockingDeque(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        addAll(c);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 入队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 从队头入队，线程安全。队满时返回false
    public boolean offerFirst(E e) {
        if(e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return linkFirst(node);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 从队尾入队，线程安全。队满时返回false
    public boolean offerLast(E e) {
        if(e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return linkLast(node);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * @throws NullPointerException if the specified element is null
     */
    // 从队尾入队，线程安全。队满时返回false
    public boolean offer(E e) {
        return offerLast(e);
    }
    
    
    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    // 从队头入队，线程安全。队满时阻塞一段时间，超时后无法入队则返回false
    public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if(e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while(!linkFirst(node)) {
                if(nanos<=0L) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    // 从队尾入队，线程安全。队满时阻塞一段时间，超时后无法入队则返回false
    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if(e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while(!linkLast(node)) {
                if(nanos<=0L) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    // 从队尾入队，线程安全。队满时阻塞一段时间，超时后无法入队则返回false
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return offerLast(e, timeout, unit);
    }
    
    
    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    // 从队头入队，线程安全。队满时阻塞
    public void putFirst(E e) throws InterruptedException {
        if(e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while(!linkFirst(node)) {
                notFull.await();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    // 从队尾入队，线程安全。队满时阻塞
    public void putLast(E e) throws InterruptedException {
        if(e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while(!linkLast(node)) {
                notFull.await();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    // 从队尾入队，线程安全。队满时阻塞
    public void put(E e) throws InterruptedException {
        putLast(e);
    }
    
    
    /**
     * @throws IllegalStateException if this deque is full
     * @throws NullPointerException  {@inheritDoc}
     */
    // 从队头入队，线程安全。队满时抛异常
    public void addFirst(E e) {
        if(!offerFirst(e)) {
            throw new IllegalStateException("Deque full");
        }
    }
    
    /**
     * @throws IllegalStateException if this deque is full
     * @throws NullPointerException  {@inheritDoc}
     */
    // 从队尾入队，线程安全。队满时抛异常
    public void addLast(E e) {
        if(!offerLast(e)) {
            throw new IllegalStateException("Deque full");
        }
    }
    
    /**
     * Inserts the specified element at the end of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * it is generally preferable to use method {@link #offer(Object) offer}.
     *
     * <p>This method is equivalent to {@link #addLast}.
     *
     * @throws IllegalStateException if this deque is full
     * @throws NullPointerException  if the specified element is null
     */
    // 从队尾入队，线程安全。队满时抛异常
    public boolean add(E e) {
        addLast(e);
        return true;
    }
    
    
    /**
     * Appends all of the elements in the specified collection to the end of
     * this deque, in the order that they are returned by the specified
     * collection's iterator.  Attempts to {@code addAll} of a deque to
     * itself result in {@code IllegalArgumentException}.
     *
     * @param c the elements to be inserted into this deque
     *
     * @return {@code true} if this deque changed as a result of the call
     *
     * @throws NullPointerException     if the specified collection or any
     *                                  of its elements are null
     * @throws IllegalArgumentException if the collection is this deque
     * @throws IllegalStateException    if this deque is full
     * @see #add(Object)
     */
    // 将容器中所有元素复制到队列当中
    public boolean addAll(Collection<? extends E> c) {
        if(c == this) {
            // As historically specified in AbstractQueue#addAll
            throw new IllegalArgumentException();
        }
        
        // Copy c into a private chain of Nodes
        Node<E> beg = null, end = null;
        int n = 0;
        for(E e : c) {
            Objects.requireNonNull(e);
            n++;
            Node<E> newNode = new Node<E>(e);
            if(beg == null) {
                beg = end = newNode;
            } else {
                end.next = newNode;
                newNode.prev = end;
                end = newNode;
            }
        }
        
        if(beg == null) {
            return false;
        }
        
        // Atomically append the chain at the end
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if(count + n<=capacity) {
                beg.prev = last;
                if(first == null) {
                    first = beg;
                } else {
                    last.next = beg;
                }
                last = end;
                count += n;
                notEmpty.signalAll();
                return true;
            }
        } finally {
            lock.unlock();
        }
        
        // Fall back to historic non-atomic implementation,
        // failing with IllegalStateException when the capacity is exceeded.
        return super.addAll(c);
    }
    
    /*▲ 入队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 出队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 从队头出队，线程安全。队空时返回null
    public E pollFirst() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return unlinkFirst();
        } finally {
            lock.unlock();
        }
    }
    
    // 从队尾出队，线程安全。队空时返回null
    public E pollLast() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return unlinkLast();
        } finally {
            lock.unlock();
        }
    }
    
    // 从队头出队，线程安全。队空时返回null
    public E poll() {
        return pollFirst();
    }
    
    
    // 从队头出队，线程安全。队空时阻塞一段时间，超时后无法出队则返回null
    public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            E x;
            while((x = unlinkFirst()) == null) {
                if(nanos<=0L) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return x;
        } finally {
            lock.unlock();
        }
    }
    
    // 从队尾出队，线程安全。队空时阻塞一段时间，超时后无法出队则返回null
    public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            E x;
            while((x = unlinkLast()) == null) {
                if(nanos<=0L) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return x;
        } finally {
            lock.unlock();
        }
    }
    
    // 从队头出队，线程安全。队空时阻塞一段时间，超时后无法出队则返回null
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return pollFirst(timeout, unit);
    }
    
    
    // 从队头出队，线程安全。队空时阻塞
    public E takeFirst() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E x;
            while((x = unlinkFirst()) == null) {
                notEmpty.await();
            }
            return x;
        } finally {
            lock.unlock();
        }
    }
    
    // 从队尾出队，线程安全。队空时阻塞
    public E takeLast() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E x;
            // 删除队尾结点。如果队尾不为null，返回其中的数据
            while((x = unlinkLast()) == null) {
                notEmpty.await();
            }
            return x;
        } finally {
            lock.unlock();
        }
    }
    
    // 从队头出队，线程安全。队空时阻塞
    public E take() throws InterruptedException {
        return takeFirst();
    }
    
    
    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    // 从队头出队，线程安全。队空时抛异常
    public E removeFirst() {
        E x = pollFirst();
        if(x == null) {
            throw new NoSuchElementException();
        }
        return x;
    }
    
    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    // 从队尾出队，线程安全。队空时抛异常
    public E removeLast() {
        E x = pollLast();
        if(x == null) {
            throw new NoSuchElementException();
        }
        return x;
    }
    
    /**
     * Retrieves and removes the head of the queue represented by this deque.
     * This method differs from {@link #poll() poll()} only in that it throws an
     * exception if this deque is empty.
     *
     * <p>This method is equivalent to {@link #removeFirst() removeFirst}.
     *
     * @return the head of the queue represented by this deque
     *
     * @throws NoSuchElementException if this deque is empty
     */
    // 从队头出队，线程安全。队空时抛异常
    public E remove() {
        return removeFirst();
    }
    
    
    // 从前往后遍历队列，移除首个包含指定元素的结点
    public boolean removeFirstOccurrence(Object o) {
        if(o == null) {
            return false;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for(Node<E> p = first; p != null; p = p.next) {
                if(o.equals(p.item)) {
                    unlink(p);
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    // 从后往前遍历队列，移除首个包含指定元素的结点
    public boolean removeLastOccurrence(Object o) {
        if(o == null)
            return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for(Node<E> p = last; p != null; p = p.prev) {
                if(o.equals(p.item)) {
                    unlink(p);
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * <p>This method is equivalent to
     * {@link #removeFirstOccurrence(Object) removeFirstOccurrence}.
     *
     * @param o element to be removed from this deque, if present
     *
     * @return {@code true} if this deque changed as a result of the call
     */
    // 从前往后遍历队列，移除首个包含指定元素的结点
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
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
     * Atomically removes all of the elements from this deque.
     * The deque will be empty after this call returns.
     */
    // 清空，即移除所有元素，不阻塞（线程安全）
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for(Node<E> f = first; f != null; ) {
                f.item = null;
                Node<E> n = f.next;
                f.prev = null;
                f.next = null;
                f = n;
            }
            first = last = null;
            count = 0;
            notFull.signalAll();
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
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(maxElements, count);
            for(int i = 0; i<n; i++) {
                c.add(first.item);   // In this order, in case add() throws.
                unlinkFirst();
            }
            return n;
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 出队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 栈式操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws IllegalStateException if this deque is full
     * @throws NullPointerException  {@inheritDoc}
     */
    // 从队头入队，线程安全。队满时抛异常（栈式操作）
    public void push(E e) {
        addFirst(e);
    }
    
    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    // 从队头出队，线程安全。队空时抛异常（栈式操作）
    public E pop() {
        return removeFirst();
    }
    
    /*▲ 栈式操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque.  This method differs from {@link #peek() peek()} only in that
     * it throws an exception if this deque is empty.
     *
     * <p>This method is equivalent to {@link #getFirst() getFirst}.
     *
     * @return the head of the queue represented by this deque
     *
     * @throws NoSuchElementException if this deque is empty
     */
    // 获取队头元素，线程安全。队空时抛出异常
    public E element() {
        return getFirst();
    }
    
    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    // 获取队头元素，线程安全。队空时抛出异常
    public E getFirst() {
        E x = peekFirst();
        if(x == null) {
            throw new NoSuchElementException();
        }
        return x;
    }
    
    // 获取队头元素，线程安全。队空时返回null
    public E peek() {
        return peekFirst();
    }
    
    // 获取队头元素，线程安全。队空时返回null
    public E peekFirst() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (first == null) ? null : first.item;
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    // 获取队尾元素，线程安全。队空时抛出异常
    public E getLast() {
        E x = peekLast();
        if(x == null) {
            throw new NoSuchElementException();
        }
        return x;
    }
    
    // 获取队尾元素，线程安全。队空时返回null
    public E peekLast() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (last == null) ? null : last.item;
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包含查询 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if this deque contains the specified element.
     * More formally, returns {@code true} if and only if this deque contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this deque
     *
     * @return {@code true} if this deque contains the specified element
     */
    // 判断队列中是否包含元素o
    public boolean contains(Object o) {
        if(o == null) {
            return false;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for(Node<E> p = first; p != null; p = p.next) {
                if(o.equals(p.item)) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an array containing all of the elements in this deque, in
     * proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this deque.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this deque
     */
    @SuppressWarnings("unchecked")
    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] a = new Object[count];
            int k = 0;
            for(Node<E> p = first; p != null; p = p.next) {
                a[k++] = p.item;
            }
            return a;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Returns an array containing all of the elements in this deque, in
     * proper sequence; the runtime type of the returned array is that of
     * the specified array.  If the deque fits in the specified array, it
     * is returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array and the size of this deque.
     *
     * <p>If this deque fits in the specified array with room to spare
     * (i.e., the array has more elements than this deque), the element in
     * the array immediately following the end of the deque is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a deque known to contain only strings.
     * The following code can be used to dump the deque into a newly
     * allocated array of {@code String}:
     *
     * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the deque are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     *
     * @return an array containing all of the elements in this deque
     *
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this deque
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if(a.length<count) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), count);
            }
            
            int k = 0;
            for(Node<E> p = first; p != null; p = p.next) {
                a[k++] = (T) p.item;
            }
            if(a.length>k) {
                a[k] = null;
            }
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
        forEachFrom(action, null);
    }
    
    
    /**
     * Returns an iterator over the elements in this deque in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    // 返回当前队列的顺序迭代器
    public Iterator<E> iterator() {
        return new Itr();
    }
    
    /**
     * Returns an iterator over the elements in this deque in reverse
     * sequential order.  The elements will be returned in order from
     * last (tail) to first (head).
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this deque in reverse order
     */
    // 返回当前队列的逆序迭代器
    public Iterator<E> descendingIterator() {
        return new DescendingItr();
    }
    
    /**
     * Returns a {@link Spliterator} over the elements in this deque.
     *
     * <p>The returned spliterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#ORDERED}, and {@link Spliterator#NONNULL}.
     *
     * @return a {@code Spliterator} over the elements in this deque
     *
     * @implNote The {@code Spliterator} implements {@code trySplit} to permit limited
     * parallelism.
     * @since 1.8
     */
    // 返回描述此队列中元素的Spliterator
    public Spliterator<E> spliterator() {
        return new LBDSpliterator();
    }
    
    /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    // 返回队列中元素数量
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Returns the number of additional elements that this deque can ideally
     * (in the absence of memory or resource constraints) accept without
     * blocking. This is always equal to the initial capacity of this deque
     * less the current {@code size} of this deque.
     *
     * <p>Note that you <em>cannot</em> always tell if an attempt to insert
     * an element will succeed by inspecting {@code remainingCapacity}
     * because it may be the case that another thread is about to
     * insert or remove an element.
     */
    // 返回队列的剩余容量
    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return capacity - count;
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static final long serialVersionUID = -387911632671998426L;
    
    
    /**
     * Saves this deque to a stream (that is, serializes it).
     *
     * @param s the stream
     *
     * @throws java.io.IOException if an I/O error occurs
     * @serialData The capacity (int), followed by elements (each an
     * {@code Object}) in the proper order, followed by a null
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // Write out capacity and any hidden stuff
            s.defaultWriteObject();
            // Write out all elements in the proper order.
            for(Node<E> p = first; p != null; p = p.next)
                s.writeObject(p.item);
            // Use trailing null as sentinel
            s.writeObject(null);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Reconstitutes this deque from a stream (that is, deserializes it).
     *
     * @param s the stream
     *
     * @throws ClassNotFoundException if the class of a serialized object
     *                                could not be found
     * @throws java.io.IOException    if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        count = 0;
        first = null;
        last = null;
        // Read in all elements and place in queue
        for(; ; ) {
            @SuppressWarnings("unchecked")
            E item = (E) s.readObject();
            if(item == null)
                break;
            add(item);
        }
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    public String toString() {
        return Helpers.collectionToString(this);
    }
    
    
    
    /*▼ 插入/删除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Links node as first element, or returns false if full.
     */
    // 将结点插入到队列头部，非线程安全。队满时返回false
    private boolean linkFirst(Node<E> node) {
        // assert lock.isHeldByCurrentThread();
        if(count >= capacity) {
            return false;
        }
        Node<E> f = first;
        node.next = f;
        first = node;
        if(last == null) {
            last = node;
        } else {
            f.prev = node;
        }
        ++count;
        notEmpty.signal();
        return true;
    }
    
    /**
     * Links node as last element, or returns false if full.
     */
    // 将结点插入到队列尾部，非线程安全。队满时返回false
    private boolean linkLast(Node<E> node) {
        // assert lock.isHeldByCurrentThread();
        if(count >= capacity) {
            return false;
        }
        Node<E> l = last;
        node.prev = l;
        last = node;
        if(first == null) {
            first = node;
        } else {
            l.next = node;
        }
        ++count;
        notEmpty.signal();
        return true;
    }
    
    /**
     * Removes and returns first element, or null if empty.
     */
    // 删除队头结点，并返回其数据，非线程安全。队空时返回null
    private E unlinkFirst() {
        // assert lock.isHeldByCurrentThread();
        Node<E> f = first;
        if(f == null) {
            return null;
        }
        Node<E> n = f.next;
        E item = f.item;
        f.item = null;
        f.next = f; // help GC
        first = n;
        if(n == null) {
            last = null;
        } else {
            n.prev = null;
        }
        --count;
        notFull.signal();
        return item;
    }
    
    /**
     * Removes and returns last element, or null if empty.
     */
    // 删除队尾结点，并返回其数据，非线程安全。队空时返回null
    private E unlinkLast() {
        // assert lock.isHeldByCurrentThread();
        Node<E> l = last;
        if(l == null) {
            return null;
        }
        Node<E> p = l.prev;
        E item = l.item;
        l.item = null;
        l.prev = l; // help GC
        last = p;
        if(p == null) {
            first = null;
        } else {
            p.next = null;
        }
        --count;
        notFull.signal();
        return item;
    }
    
    /**
     * Unlinks x.
     */
    // 从队列中删除指定的结点，非线程安全
    void unlink(Node<E> x) {
        // assert lock.isHeldByCurrentThread();
        // assert x.item != null;
        Node<E> p = x.prev;
        Node<E> n = x.next;
        if(p == null) {
            // 待删除结点是队头，则删除队头结点
            unlinkFirst();
        } else if(n == null) {
            // 待删除结点是队尾，则删除队尾结点
            unlinkLast();
        } else {
            p.next = n;
            n.prev = p;
            x.item = null;
            // Don't mess with x's links.  They may still be in use by an iterator.
            --count;
            notFull.signal();
        }
    }
    
    /*▲ 插入/删除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Used for any element traversal that is not entirely under lock.
     * Such traversals must handle both:
     * - dequeued nodes (p.next == p)
     * - (possibly multiple) interior removed nodes (p.item == null)
     */
    // 返回p的后继
    Node<E> succ(Node<E> p) {
        if(p == (p = p.next)) {
            p = first;
        }
        return p;
    }
    
    /** Implementation of bulk remove methods. */
    // 批量移除元素，即满足过滤条件的元素将被移除，不阻塞（线程安全）
    @SuppressWarnings("unchecked")
    private boolean bulkRemove(Predicate<? super E> filter) {
        boolean removed = false;
        final ReentrantLock lock = this.lock;
        Node<E> p = null;
        Node<E>[] nodes = null;
        int n, len = 0;
        do {
            // 1. Extract batch of up to 64 elements while holding the lock.
            lock.lock();
            try {
                if(nodes == null) {  // first batch; initialize
                    p = first;
                    for(Node<E> q = p; q != null; q = succ(q)) {
                        if(q.item != null && ++len == 64) {
                            break;
                        }
                    }
                    nodes = (Node<E>[]) new Node<?>[len];
                }
                for(n = 0; p != null && n<len; p = succ(p)) {
                    nodes[n++] = p;
                }
            } finally {
                lock.unlock();
            }
            
            // 2. Run the filter on the elements while lock is free.
            long deathRow = 0L;       // "bitset" of size 64
            for(int i = 0; i<n; i++) {
                final E e;
                if((e = nodes[i].item) != null && filter.test(e)) {
                    deathRow |= 1L << i;
                }
            }
            
            // 3. Remove any filtered elements while holding the lock.
            if(deathRow != 0) {
                lock.lock();
                try {
                    for(int i = 0; i<n; i++) {
                        final Node<E> q;
                        if((deathRow & (1L << i)) != 0L && (q = nodes[i]).item != null) {
                            unlink(q);
                            removed = true;
                        }
                        nodes[i] = null; // help GC
                    }
                } finally {
                    lock.unlock();
                }
            }
        } while(n>0 && p != null);
        
        return removed;
    }
    
    /**
     * Runs action on each element found during a traversal starting at p.
     * If p is null, traversal starts at head.
     */
    // 从p开始遍历所有剩余元素，并执行相应的择取操作
    void forEachFrom(Consumer<? super E> action, Node<E> p) {
        // Extract batches of elements while holding the lock; then
        // run the action on the elements while not
        final ReentrantLock lock = this.lock;
        final int batchSize = 64;       // max number of elements per batch
        Object[] es = null;             // container for batch of elements
        int n, len = 0;
        do {
            lock.lock();
            try {
                if(es == null) {
                    if(p == null)
                        p = first;
                    for(Node<E> q = p; q != null; q = succ(q))
                        if(q.item != null && ++len == batchSize)
                            break;
                    es = new Object[len];
                }
                for(n = 0; p != null && n<len; p = succ(p))
                    if((es[n] = p.item) != null)
                        n++;
            } finally {
                lock.unlock();
            }
            for(int i = 0; i<n; i++) {
                @SuppressWarnings("unchecked")
                E e = (E) es[i];
                action.accept(e);
            }
        } while(n>0 && p != null);
    }
    
    void checkInvariants() {
        // assert lock.isHeldByCurrentThread();
        // Nodes may get self-linked or lose their item, but only
        // after being unlinked and becoming unreachable from first.
        for(Node<E> p = first; p != null; p = p.next) {
            // assert p.next != p;
            // assert p.item != null;
        }
    }
    
    
    
    
    
    
    /** Doubly-linked list node class */
    // 队列结点
    static final class Node<E> {
        /**
         * The item, or null if this node has been removed.
         */
        E item; // 结点中存储的数据
        
        /**
         * One of:
         * - the real predecessor Node
         * - this Node, meaning the predecessor is tail
         * - null, meaning there is no predecessor
         */
        Node<E> prev;   // 前驱
        
        /**
         * One of:
         * - the real successor Node
         * - this Node, meaning the successor is head
         * - null, meaning there is no successor
         */
        Node<E> next;   // 后继
        
        Node(E x) {
            item = x;
        }
    }
    
    /**
     * Base class for LinkedBlockingDeque iterators.
     */
    // 外部迭代器，获取下一个元素的逻辑由子类实现
    private abstract class AbstractItr implements Iterator<E> {
        /**
         * The next node to return in next().
         */
        Node<E> next;
        
        /**
         * nextItem holds on to item fields because once we claim that
         * an element exists in hasNext(), we must return item read
         * under lock even if it was in the process of being removed
         * when hasNext() was called.
         */
        E nextItem;
        
        /**
         * Node returned by most recent call to next. Needed by remove.
         * Reset to null if this element is deleted by a call to remove.
         */
        private Node<E> lastRet;
        
        AbstractItr() {
            // set to initial position
            final ReentrantLock lock = LinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                if((next = firstNode()) != null)
                    nextItem = next.item;
            } finally {
                lock.unlock();
            }
        }
        
        public boolean hasNext() {
            return next != null;
        }
        
        public E next() {
            Node<E> p;
            if((p = next) == null)
                throw new NoSuchElementException();
            lastRet = p;
            E x = nextItem;
            final ReentrantLock lock = LinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                E e = null;
                for(p = nextNode(p); p != null && (e = p.item) == null; )
                    p = succ(p);
                next = p;
                nextItem = e;
            } finally {
                lock.unlock();
            }
            return x;
        }
        
        public void forEachRemaining(Consumer<? super E> action) {
            // A variant of forEachFrom
            Objects.requireNonNull(action);
            Node<E> p;
            if((p = next) == null)
                return;
            lastRet = p;
            next = null;
            final ReentrantLock lock = LinkedBlockingDeque.this.lock;
            final int batchSize = 64;
            Object[] es = null;
            int n, len = 1;
            do {
                lock.lock();
                try {
                    if(es == null) {
                        p = nextNode(p);
                        for(Node<E> q = p; q != null; q = succ(q))
                            if(q.item != null && ++len == batchSize)
                                break;
                        es = new Object[len];
                        es[0] = nextItem;
                        nextItem = null;
                        n = 1;
                    } else
                        n = 0;
                    for(; p != null && n<len; p = succ(p))
                        if((es[n] = p.item) != null) {
                            lastRet = p;
                            n++;
                        }
                } finally {
                    lock.unlock();
                }
                for(int i = 0; i<n; i++) {
                    @SuppressWarnings("unchecked")
                    E e = (E) es[i];
                    action.accept(e);
                }
            } while(n>0 && p != null);
        }
        
        public void remove() {
            Node<E> n = lastRet;
            if(n == null)
                throw new IllegalStateException();
            lastRet = null;
            final ReentrantLock lock = LinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                if(n.item != null)
                    unlink(n);
            } finally {
                lock.unlock();
            }
        }
        
        abstract Node<E> firstNode();
        
        abstract Node<E> nextNode(Node<E> n);
        
        private Node<E> succ(Node<E> p) {
            if(p == (p = nextNode(p)))
                p = firstNode();
            return p;
        }
    }
    
    /** Forward iterator */
    // 顺序迭代器
    private class Itr extends AbstractItr {
        Itr() {
        }                        // prevent access constructor creation
        
        Node<E> firstNode() {
            return first;
        }
        
        Node<E> nextNode(Node<E> n) {
            return n.next;
        }
    }
    
    /** Descending iterator */
    // 逆序迭代器
    private class DescendingItr extends AbstractItr {
        DescendingItr() {
        }              // prevent access constructor creation
        
        Node<E> firstNode() {
            return last;
        }
        
        Node<E> nextNode(Node<E> n) {
            return n.prev;
        }
    }
    
    /**
     * A customized variant of Spliterators.IteratorSpliterator.
     * Keep this class in sync with (very similar) LBQSpliterator.
     */
    // 描述此队列中元素的Spliterator
    private final class LBDSpliterator implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        
        // 指向当前这批待遍历元素中的首个元素
        Node<E> current;    // current node; null until initialized
        
        // 当前这批待遍历元素的数量
        int batch;          // batch size for splits
        
        // true代表没有待遍历元素了
        boolean exhausted;  // true when no more nodes
        
        // 迭代器当前的容量（会变化）
        long est = size();  // size estimate
        
        LBDSpliterator() {
        }
        
        public long estimateSize() {
            return est;
        }
        
        // 从容器的指定范围切割一段元素，将其打包到Spliterator后返回
        public Spliterator<E> trySplit() {
            Node<E> h;
            if(!exhausted
                && ((h = current) != null || (h = first) != null)
                && h.next != null) {
                
                int n = batch = Math.min(batch + 1, MAX_BATCH);
                Object[] a = new Object[n];
                final ReentrantLock lock = LinkedBlockingDeque.this.lock;
                int i = 0;
                Node<E> p = current;
                lock.lock();
                try {
                    if(p != null || (p = first) != null) {
                        for(; p != null && i<n; p = succ(p)) {
                            if((a[i] = p.item) != null) {
                                i++;
                            }
                        }
                    }
                } finally {
                    lock.unlock();
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
                final ReentrantLock lock = LinkedBlockingDeque.this.lock;
                lock.lock();
                try {
                    Node<E> p;
                    if((p = current) != null || (p = first) != null) {
                        do {
                            e = p.item;
                            p = succ(p);
                        } while(e == null && p != null);
                    }
                    
                    if((current = p) == null) {
                        exhausted = true;
                    }
                } finally {
                    lock.unlock();
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
