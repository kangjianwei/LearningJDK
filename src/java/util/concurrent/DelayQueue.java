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

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * An unbounded {@linkplain BlockingQueue blocking queue} of
 * {@code Delayed} elements, in which an element can only be taken
 * when its delay has expired.  The <em>head</em> of the queue is that
 * {@code Delayed} element whose delay expired furthest in the
 * past.  If no delay has expired there is no head and {@code poll}
 * will return {@code null}. Expiration occurs when an element's
 * {@code getDelay(TimeUnit.NANOSECONDS)} method returns a value less
 * than or equal to zero.  Even though unexpired elements cannot be
 * removed using {@code take} or {@code poll}, they are otherwise
 * treated as normal elements. For example, the {@code size} method
 * returns the count of both expired and unexpired elements.
 * This queue does not permit null elements.
 *
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Collection} and {@link Iterator} interfaces.
 * The Iterator provided in method {@link #iterator()} is <em>not</em>
 * guaranteed to traverse the elements of the DelayQueue in any
 * particular order.
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
 * 顺序无界（队列容量支持扩容到Integer.MAX_VALUE）延时阻塞队列，线程安全（锁）
 *
 * DelayQueue内的元素必须实现Delayed接口，即带有延时特性。
 *
 * 该容器的内部实现借助了一个不支持外部比较器的优先队列PriorityQueue，
 * 但由于优先队列必须通过比较器来确定元素的"优先级"，
 * 因而DelayQueue中的元素必须自己实现内部比较器接口。
 */
public class DelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {
    
    // 不支持外部比较器的优先队列
    private final PriorityQueue<E> queue = new PriorityQueue<E>();
    
    /**
     * Thread designated to wait for the element at the head of
     * the queue.  This variant of the Leader-Follower pattern
     * (http://www.cs.wustl.edu/~schmidt/POSA/POSA2/) serves to
     * minimize unnecessary timed waiting.  When a thread becomes
     * the leader, it waits only for the next delay to elapse, but
     * other threads await indefinitely.  The leader thread must
     * signal some other thread before returning from take() or
     * poll(...), unless some other thread becomes leader in the
     * interim.  Whenever the head of the queue is replaced with
     * an element with an earlier expiration time, the leader
     * field is invalidated by being reset to null, and some
     * waiting thread, but not necessarily the current leader, is
     * signalled.  So waiting threads must be prepared to acquire
     * and lose leadership while waiting.
     */
    // 记录首个被阻塞的线程
    private Thread leader;
    
    private final transient ReentrantLock lock = new ReentrantLock();
    
    /**
     * Condition signalled when a newer element becomes available
     * at the head of the queue or a new thread may need to
     * become leader.
     */
    private final Condition available = lock.newCondition();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new {@code DelayQueue} that is initially empty.
     */
    public DelayQueue() {
    }
    
    /**
     * Creates a {@code DelayQueue} initially containing the elements of the
     * given collection of {@link Delayed} instances.
     *
     * @param c the collection of elements to initially contain
     *
     * @throws NullPointerException if the specified collection or any
     *                              of its elements are null
     */
    public DelayQueue(Collection<? extends E> c) {
        this.addAll(c);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 入队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Inserts the specified element into this delay queue.
     *
     * @param e the element to add
     *
     * @return {@code true}
     *
     * @throws NullPointerException if the specified element is null
     */
    // 入队，线程安全。无法入队时扩容，不阻塞
    public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 入队，无法入队时扩容
            queue.offer(e);
            
            // 如果入队任务排在了队首
            if(queue.peek() == e) {
                leader = null;
                // 队头有新任务达到，唤醒阻塞线程
                available.signal();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Inserts the specified element into this delay queue. As the queue is
     * unbounded this method will never block.
     *
     * @param e       the element to add
     * @param timeout This parameter is ignored as the method never blocks
     * @param unit    This parameter is ignored as the method never blocks
     *
     * @return {@code true}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    // 入队，线程安全。无法入队时扩容，不阻塞
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }
    
    
    /**
     * Inserts the specified element into this delay queue. As the queue is
     * unbounded this method will never block.
     *
     * @param e the element to add
     *
     * @throws NullPointerException {@inheritDoc}
     */
    // 入队，线程安全。无法入队时扩容，不阻塞
    public void put(E e) {
        offer(e);
    }
    
    
    /**
     * Inserts the specified element into this delay queue.
     *
     * @param e the element to add
     *
     * @return {@code true} (as specified by {@link Collection#add})
     *
     * @throws NullPointerException if the specified element is null
     */
    // 入队/添加，线程安全。无法入队时扩容，不阻塞
    public boolean add(E e) {
        return offer(e);
    }
    
    /*▲ 入队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 出队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves and removes the head of this queue, or returns {@code null}
     * if this queue has no elements with an expired delay.
     *
     * @return the head of this queue, or {@code null} if this
     * queue has no elements with an expired delay
     */
    // 出队，线程安全。队空时返回null
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 获取队头任务first
            E first = queue.peek();
            
            // 如果队列为空，则返回null
            if(first==null){
                return null;
            }
            
            return first.getDelay(NANOSECONDS)>0
                ? null          // 队头任务还未到触发时间
                : queue.poll(); // 队头任务已经可以开始了，移除并返回队头任务first
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element with an expired delay is available on this queue,
     * or the specified wait time expires.
     *
     * @return the head of this queue, or {@code null} if the
     * specified waiting time elapses before an element with
     * an expired delay becomes available
     *
     * @throws InterruptedException {@inheritDoc}
     */
    // 出队，线程安全。队空时阻塞一段时间，超时后无法出队返回null
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for(; ; ) {
                // 获取队头任务
                E first = queue.peek();
                
                // 如果队列为空
                if(first == null) {
                    // 如果已经超时，则返回null
                    if(nanos<=0L) {
                        return null;
                    } else {
                        // 如果还未超时，阻塞一段时间
                        nanos = available.awaitNanos(nanos);
                    }
                } else {
                    // 队头任务获取距被触发还剩余的时间
                    long delay = first.getDelay(NANOSECONDS);
                    // 如果已经可以执行了
                    if(delay<=0L) {
                        // 队头任务已经可以开始了，移除并返回队头任务first
                        return queue.poll();
                    }
                    
                    // 如果已经超时，则返回null
                    if(nanos<=0L) {
                        return null;
                    }
                    
                    // 如果队头任务还未到触发时间，则不需要保持其引用
                    first = null; // don't retain ref while waiting
                    
                    // 如果已有阻塞线程，且未超时，当前线程需要阻塞一段时间，或者中途被唤醒
                    if(nanos<delay || leader != null) {
                        nanos = available.awaitNanos(nanos);
                    } else {
                        Thread thisThread = Thread.currentThread();
                        // 记录首个被阻塞的线程
                        leader = thisThread;
                        try {
                            // 在任务触发之前阻塞
                            long timeLeft = available.awaitNanos(delay);
                            nanos -= delay - timeLeft;
                        } finally {
                            // 醒来后，置空leader，重新获取队头任务
                            if(leader == thisThread) {
                                leader = null;
                            }
                        }
                    }
                }
            }
        } finally {
            // 执行完之后，需要唤醒后续排队的阻塞线程
            if(leader == null && queue.peek() != null) {
                available.signal();
            }
            lock.unlock();
        }
    }
    
    
    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element with an expired delay is available on this queue.
     *
     * @return the head of this queue
     *
     * @throws InterruptedException {@inheritDoc}
     */
    // 出队，线程安全。无法出队时阻塞
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for(; ; ) {
                // 获取队头任务first
                E first = queue.peek();
                
                // 如果队列为空，则需要阻塞
                if(first == null) {
                    available.await();
                } else {
                    // 队头任务获取距被触发还剩余的时间
                    long delay = first.getDelay(NANOSECONDS);
                    // 如果已经可以执行了
                    if(delay<=0L) {
                        // 队头任务已经可以开始了，移除并返回队头任务first
                        return queue.poll();
                    }
                    
                    // 如果队头任务还未到触发时间，则不需要保持其引用
                    first = null; // don't retain ref while waiting
                    
                    // 如果已有阻塞线程，当前线程需要无限期阻塞，直到被唤醒
                    if(leader != null) {
                        available.await();
                    } else {
                        Thread thisThread = Thread.currentThread();
                        // 记录首个被阻塞的线程
                        leader = thisThread;
                        try {
                            // 在任务触发之前阻塞
                            available.awaitNanos(delay);
                        } finally {
                            // 醒来后，置空leader，重新参与循环，以获取队头任务
                            if(leader == thisThread) {
                                leader = null;
                            }
                        }
                    }
                }
            }
        } finally {
            // 执行完之后，需要唤醒后续等待的阻塞线程
            if(leader == null && queue.peek() != null) {
                available.signal();
            }
            lock.unlock();
        }
    }
    
    
    /**
     * Removes a single instance of the specified element from this
     * queue, if it is present, whether or not it has expired.
     */
    // 移除，线程安全。移除成功则返回true
    public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return queue.remove(o);
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * Atomically removes all of the elements from this delay queue.
     * The queue will be empty after this call returns.
     * Elements with an unexpired delay are not waited for; they are
     * simply discarded from the queue.
     */
    // 清空。将所有元素移除，不阻塞，不抛异常
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            queue.clear();
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
            int n = 0;
            for(E first;
                n<maxElements && (first = queue.peek()) != null && first.getDelay(NANOSECONDS)<=0;
            ) {
                // 任务添加到容器中
                c.add(first);   // In this order, in case add() throws.
                // 移除队头元素first
                queue.poll();
                ++n;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 出队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves, but does not remove, the head of this queue, or
     * returns {@code null} if this queue is empty.  Unlike
     * {@code poll}, if no expired elements are available in the queue,
     * this method returns the element that will expire next,
     * if one exists.
     *
     * @return the head of this queue, or {@code null} if this
     * queue is empty
     */
    // 查看队头任务，如果队列为空，返回null
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return queue.peek();
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
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
            return queue.toArray();
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
     * <p>The following code can be used to dump a delay queue into a newly
     * allocated array of {@code Delayed}:
     *
     * <pre> {@code Delayed[] a = q.toArray(new Delayed[0]);}</pre>
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
            return queue.toArray(a);
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 迭代 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an iterator over all the elements (both expired and
     * unexpired) in this queue. The iterator does not return the
     * elements in any particular order.
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
    
    /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回队列中的任务数
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Always returns {@code Integer.MAX_VALUE} because
     * a {@code DelayQueue} is not capacity constrained.
     *
     * @return {@code Integer.MAX_VALUE}
     */
    // 计算队列剩余空间，返回Integer.MAX_VALUE代表无限
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Identity-based version for use in Itr.remove.
     */
    // 移除元素o
    void removeEQ(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for(Iterator<E> it = queue.iterator(); it.hasNext(); ) {
                if(o == it.next()) {
                    it.remove();
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    
    
    
    
    
    /**
     * Snapshot iterator that works off copy of underlying q array.
     */
    // 用于当前队列的外部迭代器
    private class Itr implements Iterator<E> {
        final Object[] array; // Array of all elements
        int cursor;           // index of next element to return
        int lastRet;          // index of last element, or -1 if no such
        
        // 拷贝源数据
        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }
        
        public boolean hasNext() {
            return cursor<array.length;
        }
        
        @SuppressWarnings("unchecked")
        public E next() {
            if(cursor >= array.length) {
                throw new NoSuchElementException();
            }
            return (E) array[lastRet = cursor++];
        }
        
        public void remove() {
            if(lastRet<0) {
                throw new IllegalStateException();
            }
            removeEQ(array[lastRet]);
            lastRet = -1;
        }
    }
}
