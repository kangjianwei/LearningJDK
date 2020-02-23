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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static java.util.concurrent.Flow.Publisher;
import static java.util.concurrent.Flow.Subscriber;
import static java.util.concurrent.Flow.Subscription;

/**
 * A {@link Flow.Publisher} that asynchronously issues submitted
 * (non-null) items to current subscribers until it is closed.  Each
 * current subscriber receives newly submitted items in the same order
 * unless drops or exceptions are encountered.  Using a
 * SubmissionPublisher allows item generators to act as compliant <a
 * href="http://www.reactive-streams.org/"> reactive-streams</a>
 * Publishers relying on drop handling and/or blocking for flow
 * control.
 *
 * <p>A SubmissionPublisher uses the {@link Executor} supplied in its
 * constructor for delivery to subscribers. The best choice of
 * Executor depends on expected usage. If the generator(s) of
 * submitted items run in separate threads, and the number of
 * subscribers can be estimated, consider using a {@link
 * Executors#newFixedThreadPool}. Otherwise consider using the
 * default, normally the {@link ForkJoinPool#commonPool}.
 *
 * <p>Buffering allows producers and consumers to transiently operate
 * at different rates.  Each subscriber uses an independent buffer.
 * Buffers are created upon first use and expanded as needed up to the
 * given maximum. (The enforced capacity may be rounded up to the
 * nearest power of two and/or bounded by the largest value supported
 * by this implementation.)  Invocations of {@link
 * Flow.Subscription#request(long) request} do not directly result in
 * buffer expansion, but risk saturation if unfilled requests exceed
 * the maximum capacity.  The default value of {@link
 * Flow#defaultBufferSize()} may provide a useful starting point for
 * choosing a capacity based on expected rates, resources, and usages.
 *
 * <p>A single SubmissionPublisher may be shared among multiple
 * sources. Actions in a source thread prior to publishing an item or
 * issuing a signal <a href="package-summary.html#MemoryVisibility">
 * <i>happen-before</i></a> actions subsequent to the corresponding
 * access by each subscriber. But reported estimates of lag and demand
 * are designed for use in monitoring, not for synchronization
 * control, and may reflect stale or inaccurate views of progress.
 *
 * <p>Publication methods support different policies about what to do
 * when buffers are saturated. Method {@link #submit(Object) submit}
 * blocks until resources are available. This is simplest, but least
 * responsive.  The {@code offer} methods may drop items (either
 * immediately or with bounded timeout), but provide an opportunity to
 * interpose a handler and then retry.
 *
 * <p>If any Subscriber method throws an exception, its subscription
 * is cancelled.  If a handler is supplied as a constructor argument,
 * it is invoked before cancellation upon an exception in method
 * {@link Flow.Subscriber#onNext onNext}, but exceptions in methods
 * {@link Flow.Subscriber#onSubscribe onSubscribe},
 * {@link Flow.Subscriber#onError(Throwable) onError} and
 * {@link Flow.Subscriber#onComplete() onComplete} are not recorded or
 * handled before cancellation.  If the supplied Executor throws
 * {@link RejectedExecutionException} (or any other RuntimeException
 * or Error) when attempting to execute a task, or a drop handler
 * throws an exception when processing a dropped item, then the
 * exception is rethrown. In these cases, not all subscribers will
 * have been issued the published item. It is usually good practice to
 * {@link #closeExceptionally closeExceptionally} in these cases.
 *
 * <p>Method {@link #consume(Consumer)} simplifies support for a
 * common case in which the only action of a subscriber is to request
 * and process all items using a supplied function.
 *
 * <p>This class may also serve as a convenient base for subclasses
 * that generate items, and use the methods in this class to publish
 * them.  For example here is a class that periodically publishes the
 * items generated from a supplier. (In practice you might add methods
 * to independently start and stop generation, to share Executors
 * among publishers, and so on, or use a SubmissionPublisher as a
 * component rather than a superclass.)
 *
 * <pre> {@code
 * class PeriodicPublisher<T> extends SubmissionPublisher<T> {
 *   final ScheduledFuture<?> periodicTask;
 *   final ScheduledExecutorService scheduler;
 *   PeriodicPublisher(Executor executor, int maxBufferCapacity,
 *                     Supplier<? extends T> supplier,
 *                     long period, TimeUnit unit) {
 *     super(executor, maxBufferCapacity);
 *     scheduler = new ScheduledThreadPoolExecutor(1);
 *     periodicTask = scheduler.scheduleAtFixedRate(
 *       () -> submit(supplier.get()), 0, period, unit);
 *   }
 *   public void close() {
 *     periodicTask.cancel(false);
 *     scheduler.shutdown();
 *     super.close();
 *   }
 * }}</pre>
 *
 * <p>Here is an example of a {@link Flow.Processor} implementation.
 * It uses single-step requests to its publisher for simplicity of
 * illustration. A more adaptive version could monitor flow using the
 * lag estimate returned from {@code submit}, along with other utility
 * methods.
 *
 * <pre> {@code
 * class TransformProcessor<S,T> extends SubmissionPublisher<T>
 *   implements Flow.Processor<S,T> {
 *   final Function<? super S, ? extends T> function;
 *   Flow.Subscription subscription;
 *   TransformProcessor(Executor executor, int maxBufferCapacity,
 *                      Function<? super S, ? extends T> function) {
 *     super(executor, maxBufferCapacity);
 *     this.function = function;
 *   }
 *   public void onSubscribe(Flow.Subscription subscription) {
 *     (this.subscription = subscription).request(1);
 *   }
 *   public void onNext(S item) {
 *     subscription.request(1);
 *     submit(function.apply(item));
 *   }
 *   public void onError(Throwable ex) { closeExceptionally(ex); }
 *   public void onComplete() { close(); }
 * }}</pre>
 *
 * @param <T> the published item type
 *
 * @author Doug Lea
 * @since 9
 */
/*
 * 生产/发布/推送者，是Flow.Publisher的一个实现
 */
public class SubmissionPublisher<T> implements Publisher<T>, AutoCloseable {
    
    /*
     * Most mechanics are handled by BufferedSubscription. This class
     * mainly tracks subscribers and ensures sequentiality, by using
     * built-in synchronization locks across public methods. Using
     * built-in locks works well in the most typical case in which
     * only one thread submits items. We extend this idea in
     * submission methods by detecting single-ownership to reduce
     * producer-consumer synchronization strength.
     */
    
    /** The largest possible power of two array size. */
    static final int BUFFER_CAPACITY_LIMIT = 1 << 30;
    
    /**
     * Initial buffer capacity used when maxBufferCapacity is
     * greater. Must be a power of two.
     */
    static final int INITIAL_CAPACITY = 32;
    
    // default Executor setup; nearly the same as CompletableFuture
    
    /**
     * Default executor -- ForkJoinPool.commonPool() unless it cannot support parallelism.
     */
    // 默认的任务执行器
    private static final Executor ASYNC_POOL = (ForkJoinPool.getCommonPoolParallelism()>1) ? ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();
    
    /**
     * Clients (BufferedSubscriptions) are maintained in a linked list
     * (via their "next" fields). This works well for publish loops.
     * It requires O(n) traversal to check for duplicate subscribers,
     * but we expect that subscribing is much less common than
     * publishing. Unsubscribing occurs only during traversal loops,
     * when BufferedSubscription methods return negative values
     * signifying that they have been closed.  To reduce
     * head-of-line blocking, submit and offer methods first call
     * BufferedSubscription.offer on each subscriber, and place
     * saturated ones in retries list (using nextRetry field), and
     * retry, possibly blocking or dropping.
     */
    // 注册的中介链表，先注册的排在开头
    BufferedSubscription<T> clients;
    
    /** Run status, updated only within locks */
    // 标记中介是否已经关闭
    volatile boolean closed;
    
    /** Set true on first call to subscribe, to initialize possible owner */
    // 当首次注册消费者之后置为true
    boolean subscribed;
    
    /** The first caller thread to subscribe, or null if thread ever changed */
    // 首次注册消费者时生产者所在线程
    Thread owner;
    
    /** If non-null, the exception in closeExceptionally */
    // 调用closeExceptionally(Throwable)异常关闭中介时传入的异常信息
    volatile Throwable closedException;
    
    // 执行消费任务的任务执行器
    final Executor executor;
    
    // 如果消息在消费过程中出现异常，设置handler可以让消费者决定下一步该采取什么操作，包括如何处理异常
    final BiConsumer<? super Subscriber<? super T>, ? super Throwable> onNextHandler;
    
    // 消息队列最大容量
    final int maxBufferCapacity;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new SubmissionPublisher using the {@link
     * ForkJoinPool#commonPool()} for async delivery to subscribers
     * (unless it does not support a parallelism level of at least two,
     * in which case, a new Thread is created to run each task), with
     * maximum buffer capacity of {@link Flow#defaultBufferSize}, and no
     * handler for Subscriber exceptions in method {@link
     * Flow.Subscriber#onNext(Object) onNext}.
     */
    public SubmissionPublisher() {
        this(ASYNC_POOL, Flow.defaultBufferSize(), null);
    }
    
    /**
     * Creates a new SubmissionPublisher using the given Executor for
     * async delivery to subscribers, with the given maximum buffer size
     * for each subscriber, and no handler for Subscriber exceptions in
     * method {@link Flow.Subscriber#onNext(Object) onNext}.
     *
     * @param executor          the executor to use for async delivery,
     *                          supporting creation of at least one independent thread
     * @param maxBufferCapacity the maximum capacity for each
     *                          subscriber's buffer (the enforced capacity may be rounded up to
     *                          the nearest power of two and/or bounded by the largest value
     *                          supported by this implementation; method {@link #getMaxBufferCapacity}
     *                          returns the actual value)
     *
     * @throws NullPointerException     if executor is null
     * @throws IllegalArgumentException if maxBufferCapacity not
     *                                  positive
     */
    public SubmissionPublisher(Executor executor, int maxBufferCapacity) {
        this(executor, maxBufferCapacity, null);
    }
    
    /**
     * Creates a new SubmissionPublisher using the given Executor for
     * async delivery to subscribers, with the given maximum buffer size
     * for each subscriber, and, if non-null, the given handler invoked
     * when any Subscriber throws an exception in method {@link
     * Flow.Subscriber#onNext(Object) onNext}.
     *
     * @param executor          the executor to use for async delivery,
     *                          supporting creation of at least one independent thread
     * @param maxBufferCapacity the maximum capacity for each
     *                          subscriber's buffer (the enforced capacity may be rounded up to
     *                          the nearest power of two and/or bounded by the largest value
     *                          supported by this implementation; method {@link #getMaxBufferCapacity}
     *                          returns the actual value)
     * @param handler           if non-null, procedure to invoke upon exception
     *                          thrown in method {@code onNext}
     *
     * @throws NullPointerException     if executor is null
     * @throws IllegalArgumentException if maxBufferCapacity not
     *                                  positive
     */
    /**
     * executor          - 该任务执行器用在执行消费动作中
     * maxBufferCapacity - 消息队列最大容量，会被预处理之后再存到maxBufferCapacity域
     * handler           - 如果消息在消费过程中出现异常，设置handler可以让消费者决定下一步该采取什么操作，包括如何处理异常
     */
    public SubmissionPublisher(Executor executor, int maxBufferCapacity, BiConsumer<? super Subscriber<? super T>, ? super Throwable> handler) {
        if(executor == null) {
            throw new NullPointerException();
        }
        
        if(maxBufferCapacity<=0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        
        this.executor = executor;
        
        this.onNextHandler = handler;
        
        // 适当扩大maxBufferCapacity（扩大倍数不超过2）
        this.maxBufferCapacity = roundCapacity(maxBufferCapacity);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注册 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Adds the given Subscriber unless already subscribed.
     * If already subscribed, the Subscriber's {@link
     * Flow.Subscriber#onError(Throwable) onError} method is invoked on
     * the existing subscription with an {@link IllegalStateException}.
     * Otherwise, upon success, the Subscriber's {@link
     * Flow.Subscriber#onSubscribe onSubscribe} method is invoked
     * asynchronously with a new {@link Flow.Subscription}.  If {@link
     * Flow.Subscriber#onSubscribe onSubscribe} throws an exception, the
     * subscription is cancelled. Otherwise, if this SubmissionPublisher
     * was closed exceptionally, then the subscriber's {@link
     * Flow.Subscriber#onError onError} method is invoked with the
     * corresponding exception, or if closed without exception, the
     * subscriber's {@link Flow.Subscriber#onComplete() onComplete}
     * method is invoked.  Subscribers may enable receiving items by
     * invoking the {@link Flow.Subscription#request(long) request}
     * method of the new Subscription, and may unsubscribe by invoking
     * its {@link Flow.Subscription#cancel() cancel} method.
     *
     * @param subscriber the subscriber
     *
     * @throws NullPointerException if subscriber is null
     */
    // 注册消费者。其执行过程是先向生产者注册中介，再向中介注册消费者，是一个间接注册过程
    public void subscribe(Subscriber<? super T> subscriber) {
        if(subscriber == null) {
            throw new NullPointerException();
        }
        
        int max = maxBufferCapacity; // allocate initial array
        
        // 中介内部的消息队列（按最大容量分配）
        Object[] array = new Object[max<INITIAL_CAPACITY ? max : INITIAL_CAPACITY];
        
        // 这里将消息队列的初始容量设置的跟最大容量一样大
        BufferedSubscription<T> subscription = new BufferedSubscription<T>(subscriber, executor, onNextHandler, array, max);
        
        synchronized(this) {
            // 如果是首次注册消费者
            if(!subscribed) {
                subscribed = true;
                // 首次注册消费者时生产者所在线程
                owner = Thread.currentThread();
            }
            
            // 遍历注册的所有中介（消费者）
            for(BufferedSubscription<T> b = clients, pred = null; ; ) {
                // 如果遍历到了结尾
                if(b == null) {
                    Throwable ex;
                    
                    // 将中介注册到生产者时的回调
                    subscription.onSubscribe();
                    
                    // 如果中介已被异常关闭
                    if((ex = closedException) != null) {
                        // 使用已经异常关闭的中介
                        subscription.onError(ex);
                        
                        // 如果中介已被正常关闭
                    } else if(closed) {
                        // 使用已经正常关闭的中介
                        subscription.onComplete();
                        
                        // 如果是添加首个注册的中介
                    } else if(pred == null) {
                        clients = subscription;
                        
                        // 追加中介
                    } else {
                        pred.next = subscription;
                    }
                    
                    break;
                }
                
                BufferedSubscription<T> next = b.next;
                
                // 如果中介已被关闭，
                if(b.isClosed()) {   // remove
                    b.next = null;    // detach
                    if(pred == null) {
                        clients = next;
                    } else {
                        pred.next = next;
                    }
                    
                    // 不允许重复注册同一个消费者
                } else if(subscriber.equals(b.subscriber)) {
                    // 使用中介时发生异常
                    b.onError(new IllegalStateException("Duplicate subscribe"));
                    break;
                } else {
                    pred = b;
                }
                
                b = next;
            }// for
        }
    }
    
    /*▲ 注册 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 生产/推送/发布消息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Publishes the given item to each current subscriber by
     * asynchronously invoking its {@link Flow.Subscriber#onNext(Object)
     * onNext} method, blocking uninterruptibly while resources for any
     * subscriber are unavailable. This method returns an estimate of
     * the maximum lag (number of items submitted but not yet consumed)
     * among all current subscribers. This value is at least one
     * (accounting for this submitted item) if there are any
     * subscribers, else zero.
     *
     * <p>If the Executor for this publisher throws a
     * RejectedExecutionException (or any other RuntimeException or
     * Error) when attempting to asynchronously notify subscribers,
     * then this exception is rethrown, in which case not all
     * subscribers will have been issued this item.
     *
     * @param item the (non-null) item to publish
     *
     * @return the estimated maximum lag among subscribers
     *
     * @throws IllegalStateException      if closed
     * @throws NullPointerException       if item is null
     * @throws RejectedExecutionException if thrown by Executor
     */
    /*
     * 生产者向中介推送消息（存储到消息队列中以便消费者去消费），推送失败后中介/生产者进入无限期阻塞，直到被主动唤醒，醒来后只重试一次
     *
     * item    待发布的消息
     *
     * 返回值：
     * 如果为负数，其绝对值代表本次发布错过的中介数量（因为满了无法接受）
     * 如果为正数，代表各中介内消息队列的最大长度，即衡量估计需要最长等待的时间
     */
    public int submit(T item) {
        return doOffer(item, Long.MAX_VALUE, null);
    }
    
    /**
     * Publishes the given item, if possible, to each current subscriber
     * by asynchronously invoking its {@link
     * Flow.Subscriber#onNext(Object) onNext} method. The item may be
     * dropped by one or more subscribers if resource limits are
     * exceeded, in which case the given handler (if non-null) is
     * invoked, and if it returns true, retried once.  Other calls to
     * methods in this class by other threads are blocked while the
     * handler is invoked.  Unless recovery is assured, options are
     * usually limited to logging the error and/or issuing an {@link
     * Flow.Subscriber#onError(Throwable) onError} signal to the
     * subscriber.
     *
     * <p>This method returns a status indicator: If negative, it
     * represents the (negative) number of drops (failed attempts to
     * issue the item to a subscriber). Otherwise it is an estimate of
     * the maximum lag (number of items submitted but not yet
     * consumed) among all current subscribers. This value is at least
     * one (accounting for this submitted item) if there are any
     * subscribers, else zero.
     *
     * <p>If the Executor for this publisher throws a
     * RejectedExecutionException (or any other RuntimeException or
     * Error) when attempting to asynchronously notify subscribers, or
     * the drop handler throws an exception when processing a dropped
     * item, then this exception is rethrown.
     *
     * @param item   the (non-null) item to publish
     * @param onDrop if non-null, the handler invoked upon a drop to a
     *               subscriber, with arguments of the subscriber and item; if it
     *               returns true, an offer is re-attempted (once)
     *
     * @return if negative, the (negative) number of drops; otherwise an estimate of maximum lag
     *
     * @throws IllegalStateException      if closed
     * @throws NullPointerException       if item is null
     * @throws RejectedExecutionException if thrown by Executor
     */
    /*
     * 生产者向中介推送消息（存储到消息队列中以便消费者去消费），推送失败后立即重试
     *
     * item    待发布的消息
     * onDrop  第一次重试失败后调用，onDrop的返回值指示是否需要进行第二次尝试
     *
     * 返回值：
     * 如果为负数，其绝对值代表本次发布错过的中介数量（因为满了无法接受）
     * 如果为正数，代表各中介内消息队列的最大长度，即衡量估计需要最长等待的时间
     */
    public int offer(T item, BiPredicate<Subscriber<? super T>, ? super T> onDrop) {
        return doOffer(item, 0L, onDrop);
    }
    
    /**
     * Publishes the given item, if possible, to each current subscriber
     * by asynchronously invoking its {@link
     * Flow.Subscriber#onNext(Object) onNext} method, blocking while
     * resources for any subscription are unavailable, up to the
     * specified timeout or until the caller thread is interrupted, at
     * which point the given handler (if non-null) is invoked, and if it
     * returns true, retried once. (The drop handler may distinguish
     * timeouts from interrupts by checking whether the current thread
     * is interrupted.)  Other calls to methods in this class by other
     * threads are blocked while the handler is invoked.  Unless
     * recovery is assured, options are usually limited to logging the
     * error and/or issuing an {@link Flow.Subscriber#onError(Throwable)
     * onError} signal to the subscriber.
     *
     * <p>This method returns a status indicator: If negative, it
     * represents the (negative) number of drops (failed attempts to
     * issue the item to a subscriber). Otherwise it is an estimate of
     * the maximum lag (number of items submitted but not yet
     * consumed) among all current subscribers. This value is at least
     * one (accounting for this submitted item) if there are any
     * subscribers, else zero.
     *
     * <p>If the Executor for this publisher throws a
     * RejectedExecutionException (or any other RuntimeException or
     * Error) when attempting to asynchronously notify subscribers, or
     * the drop handler throws an exception when processing a dropped
     * item, then this exception is rethrown.
     *
     * @param item    the (non-null) item to publish
     * @param timeout how long to wait for resources for any subscriber
     *                before giving up, in units of {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     * @param onDrop  if non-null, the handler invoked upon a drop to a
     *                subscriber, with arguments of the subscriber and item; if it
     *                returns true, an offer is re-attempted (once)
     *
     * @return if negative, the (negative) number of drops; otherwise an estimate of maximum lag
     *
     * @throws IllegalStateException      if closed
     * @throws NullPointerException       if item is null
     * @throws RejectedExecutionException if thrown by Executor
     */
    /*
     * 生产者向中介推送消息（存储到消息队列中以便消费者去消费），推送失败后阻塞timeout时长后再重试
     *
     * item    待发布的消息
     * timeout 发布失败并重试前需要等待的时长
     * unit    timeout的时间单位
     * onDrop  第一次重试失败后调用，onDrop的返回值指示是否需要进行第二次尝试
     *
     * 返回值：
     * 如果为负数，其绝对值代表本次发布错过的中介数量（因为满了无法接受）
     * 如果为正数，代表各中介内消息队列的最大长度，即衡量估计需要最长等待的时间
     */
    public int offer(T item, long timeout, TimeUnit unit, BiPredicate<Subscriber<? super T>, ? super T> onDrop) {
        // 将unit时间单位下的timeout换算为纳秒
        long nanos = unit.toNanos(timeout);
        
        // distinguishes from untimed (only wrt interrupt policy)
        if(nanos == Long.MAX_VALUE) {
            --nanos;
        }
        
        return doOffer(item, nanos, onDrop);
    }
    
    /**
     * Common implementation for all three forms of submit and offer.
     * Acts as submit if nanos == Long.MAX_VALUE, else offer.
     */
    /*
     * 生产者向中介推送消息（存储到消息队列中以便消费者去消费），推送失败后阻塞nanos时长后再重试
     *
     * item    待发布的消息
     * timeout 发布失败并重试前需要等待的时长，单位纳秒
     * onDrop  第一次重试失败后调用，onDrop的返回值指示是否需要进行第二次尝试
     *
     * 返回值：
     * 如果为负数，其绝对值代表本次发布错过的中介数量（因为满了无法接受）
     * 如果为正数，代表各中介内消息队列的最大长度，即衡量估计需要最长等待的时间
     */
    private int doOffer(T item, long nanos, BiPredicate<Subscriber<? super T>, ? super T> onDrop) {
        if(item == null) {
            throw new NullPointerException();
        }
        
        // 记录各中介内消息队列的最大长度
        int lag = 0;
        
        // true：生产行为没有发生在首次注册消费者时生产者所在线程中
        boolean unowned;
        
        boolean complete;
        
        synchronized(this) {
            Thread t = Thread.currentThread();
            Thread o;
            
            // 中介链
            BufferedSubscription<T> b = clients;
            
            // 如果生产者当前所在线程不是首次注册消费者时所在的线程
            if((unowned = ((o = owner) != t)) && o != null) {
                owner = null;                     // disable bias
            }
            
            // 如果当前没有注册的中介（消费者）
            if(b == null) {
                complete = closed;
            } else {
                complete = false;
                
                boolean cleanMe = false;
                
                // 指向需要重试接收消息的中介
                BufferedSubscription<T> retries = null;
                BufferedSubscription<T> rtail = null;
                BufferedSubscription<T> next;
                
                // 遍历中介链，将消息推送到每个中介的消息队列，以便中介关联的消费者去消费
                do {
                    // 下一个中介
                    next = b.next;
                    
                    /*
                     * 将消息item添加到中介的消息队列中以待消费
                     * 返回值表示消息队列长度，返回0表示中介的消息队列已满，返回负数表示中介已关闭
                     */
                    int stat = b.offer(item, unowned);
                    
                    // 如果消息队列已满
                    if(stat == 0) {               // saturated; add to retry list
                        b.nextRetry = null;       // avoid garbage on exceptions
                        
                        // 记录该中介，以待后续重试接收消息
                        if(rtail == null) {
                            retries = b;
                        } else {
                            rtail.nextRetry = b;
                        }
                        
                        rtail = b;
                        
                        // 如果中介已关闭
                    } else if(stat<0) {         // closed
                        // 标记该中介为允许清理
                        cleanMe = true;         // remove later
                    } else if(stat>lag) {
                        // 记录积压的更长的消息队列长度
                        lag = stat;
                    }
                    
                    // 如果存在下一个中介
                } while((b = next) != null);
                
                // 每完成一次生产/推送，都需要把推送失败的重试一下
                if(retries != null || cleanMe) {
                    // 处理推送失败的中介（阻塞并重试，或者清理）
                    lag = retryOffer(item, nanos, onDrop, retries, lag, cleanMe);
                }
            }
        }
        
        // 如果待接收消息的中介已关闭，则抛异常
        if(complete) {
            throw new IllegalStateException("Closed");
        }
        
        return lag;
    }
    
    /**
     * Helps, (timed) waits for, and/or drops buffers on list;
     * returns lag or negative drops (for use in offer).
     */
    // 处理推送失败的中介（阻塞并重试）
    private int retryOffer(T item, long nanos, BiPredicate<Subscriber<? super T>, ? super T> onDrop, BufferedSubscription<T> retries, int lag, boolean cleanMe) {
        // 如果存在需要重试的中介
        for(BufferedSubscription<T> r = retries; r != null; ) {
            BufferedSubscription<T> nextRetry = r.nextRetry;
            
            r.nextRetry = null;
            
            /*
             * 推送失败并重试之前需要阻塞/等待
             * 如果nanos>0，说明需要阻塞一会儿（等待消费者消费）之后再去重试
             */
            if(nanos>0L) {
                r.awaitSpace(nanos);
            }
            
            // 尝试重新推送/生产消息，返回值代表消息队列长度，返回0表示推送/生产失败（比如满了），返回负数说明中介已关闭
            int stat = r.retryOffer(item);
            
            // 如果消息队列为null（比如满了），且需要重试（由onDrop的实现觉得），则再次尝试
            if(stat == 0 && onDrop != null && onDrop.test(r.subscriber, item)) {
                // 再次尝试重新推送/生产消息，返回值代表消息队列长度，返回0表示推送/生产失败（比如满了），返回负数说明中介已关闭
                stat = r.retryOffer(item);
            }
            
            // 消息队列满了
            if(stat == 0) {
                lag = (lag >= 0) ? -1 : lag - 1;
                
                // 中介已关闭，稍后需要清理
            } else if(stat<0) {
                cleanMe = true;
            } else if(lag >= 0 && stat>lag) {
                // 记录积压的更长的消息队列长度
                lag = stat;
            }
            
            r = nextRetry;
        }
        
        // 如果有需要清理的中介（已经关闭的中介）
        if(cleanMe) {
            // 清理已经关闭的中介，并返回未关闭的中介数量
            cleanAndCount();
        }
        
        return lag;
    }
    
    /**
     * Returns current list count after removing closed subscribers.
     * Call only while holding lock.  Used mainly by retryOffer for cleanup.
     */
    // 清理已经关闭的中介。返回未关闭的中介数量
    private int cleanAndCount() {
        int count = 0;
        BufferedSubscription<T> pred = null, next;
        for(BufferedSubscription<T> b = clients; b != null; b = next) {
            next = b.next;
            if(b.isClosed()) {
                b.next = null;
                if(pred == null) {
                    clients = next;
                } else {
                    pred.next = next;
                }
            } else {
                pred = b;
                ++count;
            }
        }
        return count;
    }
    
    /*▲ 生产/推送/发布消息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭中介 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Unless already closed, issues {@link Flow.Subscriber#onComplete() onComplete} signals to current subscribers,
     * and disallows subsequent attempts to publish.
     * Upon return, this method does NOT guarantee that all subscribers have yet completed.
     */
    // 正常关闭中介，阻断生产者向其推送消息（关闭后，不保证消息队列中的消息已被全部处理）
    public void close() {
        if(!closed) {
            BufferedSubscription<T> b;
            
            synchronized(this) {
                // no need to re-check closed here
                b = clients;
                clients = null;
                owner = null;
                closed = true;
            }
            
            while(b != null) {
                BufferedSubscription<T> next = b.next;
                b.next = null;
                // 正常关闭中介
                b.onComplete();
                b = next;
            }
        }
    }
    
    /**
     * Unless already closed, issues {@link Flow.Subscriber#onError(Throwable) onError} signals to current subscribers with the given error,
     * and disallows subsequent attempts to publish.
     * Future subscribers also receive the given error.
     * Upon return, this method does <em>NOT</em> guarantee that all subscribers have yet completed.
     *
     * @param error the {@code onError} argument sent to subscribers
     *
     * @throws NullPointerException if error is null
     */
    // 异常关闭中介（传入一个异常信息），阻断生产者向其推送消息（关闭后，不保证消息队列中的消息已被全部处理）
    public void closeExceptionally(Throwable error) {
        if(error == null) {
            throw new NullPointerException();
        }
        
        if(!closed) {
            BufferedSubscription<T> b;
            synchronized(this) {
                b = clients;
                if(!closed) {  // don't clobber racing close
                    closedException = error;
                    clients = null;
                    owner = null;
                    closed = true;
                }
            }
            
            while(b != null) {
                BufferedSubscription<T> next = b.next;
                b.next = null;
                // 异常关闭中介
                b.onError(error);
                b = next;
            }
        }
    }
    
    /**
     * Returns true if this publisher is not accepting submissions.
     *
     * @return true if closed
     */
    // 判断中介是否已经关闭
    public boolean isClosed() {
        return closed;
    }
    
    /**
     * Returns the exception associated with {@link
     * #closeExceptionally(Throwable) closeExceptionally}, or null if
     * not closed or if closed normally.
     *
     * @return the exception, or null if none
     */
    // 获取引起异常关闭中介的异常信息
    public Throwable getClosedException() {
        return closedException;
    }
    
    /*▲ 关闭中介 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns true if this publisher has any subscribers.
     *
     * @return true if this publisher has any subscribers
     */
    // 判断是否存在未关闭的中介（顺便将查过过程中遇到的已关闭的中介从中介链上移除）
    public boolean hasSubscribers() {
        boolean nonEmpty = false;
        synchronized(this) {
            for(BufferedSubscription<T> b = clients; b != null; ) {
                BufferedSubscription<T> next = b.next;
                if(b.isClosed()) {
                    b.next = null;
                    b = clients = next;
                } else {
                    nonEmpty = true;
                    break;
                }
            }
        }
        
        return nonEmpty;
    }
    
    /**
     * Returns a list of current subscribers for monitoring and
     * tracking purposes, not for invoking {@link Flow.Subscriber}
     * methods on the subscribers.
     *
     * @return list of current subscribers
     */
    // 获取未关闭的中介列表
    public List<Subscriber<? super T>> getSubscribers() {
        ArrayList<Subscriber<? super T>> subs = new ArrayList<>();
        synchronized(this) {
            BufferedSubscription<T> pred = null, next;
            for(BufferedSubscription<T> b = clients; b != null; b = next) {
                next = b.next;
                if(b.isClosed()) {
                    b.next = null;
                    if(pred == null) {
                        clients = next;
                    } else {
                        pred.next = next;
                    }
                } else {
                    subs.add(b.subscriber);
                    pred = b;
                }
            }
        }
        return subs;
    }
    
    /**
     * Returns true if the given Subscriber is currently subscribed.
     *
     * @param subscriber the subscriber
     *
     * @return true if currently subscribed
     *
     * @throws NullPointerException if subscriber is null
     */
    // 判断是否存在未关闭的中介subscriber
    public boolean isSubscribed(Subscriber<? super T> subscriber) {
        if(subscriber == null) {
            throw new NullPointerException();
        }
        
        if(!closed) {
            synchronized(this) {
                BufferedSubscription<T> pred = null, next;
                for(BufferedSubscription<T> b = clients; b != null; b = next) {
                    next = b.next;
                    if(b.isClosed()) {
                        b.next = null;
                        if(pred == null) {
                            clients = next;
                        } else {
                            pred.next = next;
                        }
                    } else if(subscriber.equals(b.subscriber)) {
                        return true;
                    } else {
                        pred = b;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Returns the number of current subscribers.
     *
     * @return the number of current subscribers
     */
    // 清理已经关闭的中介，并返回未关闭的中介数量
    public int getNumberOfSubscribers() {
        synchronized(this) {
            return cleanAndCount();
        }
    }
    
    /**
     * Returns the Executor used for asynchronous delivery.
     *
     * @return the Executor used for asynchronous delivery
     */
    // 获取执行消费任务的任务执行器
    public Executor getExecutor() {
        return executor;
    }
    
    /**
     * Returns the maximum per-subscriber buffer capacity.
     *
     * @return the maximum per-subscriber buffer capacity
     */
    // 获取消息队列的最大长度
    public int getMaxBufferCapacity() {
        return maxBufferCapacity;
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns an estimate of the minimum number of items requested
     * (via {@link Flow.Subscription#request(long) request}) but not
     * yet produced, among all current subscribers.
     *
     * @return the estimate, or zero if no subscribers
     */
    public long estimateMinimumDemand() {
        long min = Long.MAX_VALUE;
        boolean nonEmpty = false;
        synchronized(this) {
            BufferedSubscription<T> pred = null, next;
            for(BufferedSubscription<T> b = clients; b != null; b = next) {
                int n;
                long d;
                next = b.next;
                
                // 返回缓存的消息数量(估计值)，如果中介通道已关闭，则返回负数
                if((n = b.estimateLag())<0) {
                    b.next = null;
                    if(pred == null)
                        clients = next;
                    else
                        pred.next = next;
                } else {
                    if((d = b.demand - n)<min)
                        min = d;
                    nonEmpty = true;
                    pred = b;
                }
            }
        }
        return nonEmpty ? min : 0;
    }
    
    /**
     * Returns an estimate of the maximum number of items produced but
     * not yet consumed among all current subscribers.
     *
     * @return the estimate
     */
    public int estimateMaximumLag() {
        int max = 0;
        synchronized(this) {
            BufferedSubscription<T> pred = null, next;
            for(BufferedSubscription<T> b = clients; b != null; b = next) {
                int n;
                next = b.next;
                if((n = b.estimateLag())<0) {
                    b.next = null;
                    if(pred == null)
                        clients = next;
                    else
                        pred.next = next;
                } else {
                    if(n>max)
                        max = n;
                    pred = b;
                }
            }
        }
        return max;
    }
    
    /**
     * Processes all published items using the given Consumer function.
     * Returns a CompletableFuture that is completed normally when this
     * publisher signals {@link Flow.Subscriber#onComplete()
     * onComplete}, or completed exceptionally upon any error, or an
     * exception is thrown by the Consumer, or the returned
     * CompletableFuture is cancelled, in which case no further items
     * are processed.
     *
     * @param consumer the function applied to each onNext item
     *
     * @return a CompletableFuture that is completed normally
     * when the publisher signals onComplete, and exceptionally
     * upon any error or cancellation
     *
     * @throws NullPointerException if consumer is null
     */
    public CompletableFuture<Void> consume(Consumer<? super T> consumer) {
        if(consumer == null) {
            throw new NullPointerException();
        }
    
        CompletableFuture<Void> status = new CompletableFuture<>();
    
        // 消费者，【消费】消息和信号
        ConsumerSubscriber<T> subscriber = new ConsumerSubscriber<>(status, consumer);
    
        // 注册消费者
        subscribe(subscriber);
    
        return status;
    }
    
    /** Round capacity to power of 2, at most limit. */
    // 适当扩大cap（扩大倍数不超过2）
    static final int roundCapacity(int cap) {
        int n = cap - 1;
        
        /*
         * 周期性地将n扩大1~2倍
         * 实际计算结果是：2^ceil(log2(n+1))-1
         */
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        
        // n至少为1
        return (n<=0) ? 1 : (n >= BUFFER_CAPACITY_LIMIT) ? BUFFER_CAPACITY_LIMIT : n + 1;
    }
    
    
    
    
    
    
    /**
     * A resizable array-based ring buffer with integrated control to
     * start a consumer task whenever items are available.  The buffer
     * algorithm is specialized for the case of at most one concurrent
     * producer and consumer, and power of two buffer sizes. It relies
     * primarily on atomic operations (CAS or getAndSet) at the next
     * array slot to put or take an element, at the "tail" and "head"
     * indices written only by the producer and consumer respectively.
     *
     * We ensure internally that there is at most one active consumer
     * task at any given time. The publisher guarantees a single
     * producer via its lock. Sync among producers and consumers
     * relies on volatile fields "ctl", "demand", and "waiting" (along
     * with element access). Other variables are accessed in plain
     * mode, relying on outer ordering and exclusion, and/or enclosing
     * them within other volatile accesses. Some atomic operations are
     * avoided by tracking single threaded ownership by producers (in
     * the style of biased locking).
     *
     * Execution control and protocol state are managed using field
     * "ctl".  Methods to subscribe, close, request, and cancel set
     * ctl bits (mostly using atomic boolean method getAndBitwiseOr),
     * and ensure that a task is running. (The corresponding consumer
     * side actions are in method consume.)  To avoid starting a new
     * task on each action, ctl also includes a keep-alive bit
     * (ACTIVE) that is refreshed if needed on producer actions.
     * (Maintaining agreement about keep-alives requires most atomic
     * updates to be full SC/Volatile strength, which is still much
     * cheaper than using one task per item.)  Error signals
     * additionally null out items and/or fields to reduce termination
     * latency.  The cancel() method is supported by treating as ERROR
     * but suppressing onError signal.
     *
     * Support for blocking also exploits the fact that there is only
     * one possible waiter. ManagedBlocker-compatible control fields
     * are placed in this class itself rather than in wait-nodes.
     * Blocking control relies on the "waiting" and "waiter"
     * fields. Producers set them before trying to block. Signalling
     * unparks and clears fields. If the producer and/or consumer are
     * using a ForkJoinPool, the producer attempts to help run
     * consumer tasks via ForkJoinPool.helpAsyncBlocker before
     * blocking.
     *
     * Usages of this class may encounter any of several forms of
     * memory contention. We try to ameliorate across them without
     * unduly impacting footprints in low-contention usages where it
     * isn't needed. Buffer arrays start out small and grow only as
     * needed.  The class uses @Contended and heuristic field
     * declaration ordering to reduce false-sharing memory contention
     * across instances of BufferedSubscription (as in, multiple
     * subscribers per publisher).  We additionally segregate some
     * fields that would otherwise nearly always encounter cache line
     * contention among producers and consumers. To reduce contention
     * across time (vs space), consumers only periodically update
     * other fields (see method takeItems), at the expense of possibly
     * staler reporting of lags and demand (bounded at 12.5% == 1/8
     * capacity) and possibly more atomic operations.
     *
     * Other forms of imbalance and slowdowns can occur during startup
     * when producer and consumer methods are compiled and/or memory
     * is allocated at different rates.  This is ameliorated by
     * artificially subdividing some consumer methods, including
     * isolation of all subscriber callbacks.  This code also includes
     * typical power-of-two array screening idioms to avoid compilers
     * generating traps, along with the usual SSA-based inline
     * assignment coding style. Also, all methods and fields have
     * default visibility to simplify usage by callers.
     */
    // 中介，内部包含注册的消费者，用于打通生产者和消费者的联系
    @SuppressWarnings("serial")
    @jdk.internal.vm.annotation.Contended
    static final class BufferedSubscription<T> implements Subscription, ForkJoinPool.ManagedBlocker {
        // 中介的状态位信息
        volatile int ctl;                  // atomic run state flags
        
        // ctl bit values
        static final int  CLOSED   = 0x01;  // 中介已关闭，停止消费，发生在COMPLETE或ERROR之后 | if set, other bits ignored
        static final int  ACTIVE   = 0x02;  // 消费者处于活动状态 | keep-alive for consumer task
        static final int  REQS     = 0x04;  // 消费者发出消费请求 | (possibly) nonzero demand
        static final int  ERROR    = 0x08;  // 中介异常关闭，停止推送 | issues onError when noticed
        static final int  COMPLETE = 0x10;  // 中介正常关闭，停止推送 | issues onComplete when done
        static final int  RUN      = 0x20;  // 消费者即将或正在运行 | task is or will be running
        static final int  OPEN     = 0x40;  // 消费者已经完成注册 | true after subscribe
        
        static final long INTERRUPTED = -1L; // timeout vs interrupt sentinel
        
        
        // 外部注册的消费者（自定义的实现）
        final Subscriber<? super T> subscriber;
        
        // 消息队列最大容量
        final int maxCapacity;             // max buffer size
        
        // 消息队列，存放生产者生产的消息，等待被消费
        Object[] array;                    // buffer
        
        // 消息队列头尾游标
        int head;                          // next position to take
        int tail;                          // next position to put
        
        // 如果消息在消费过程中出现异常，设置handler可以让消费者决定下一步该采取什么操作，包括如何处理异常
        final BiConsumer<? super Subscriber<? super T>, ? super Throwable> onNextHandler;
        
        // 消费者申请消费的消息数量
        @jdk.internal.vm.annotation.Contended("c") // segregate
        volatile long demand;              // # unfilled requests
        
        // 非零表示中介被阻塞
        @jdk.internal.vm.annotation.Contended("c")
        volatile int waiting;              // nonzero if producer blocked
        
        // 中介的阻塞超时
        long timeout;                      // Long.MAX_VALUE if untimed wait
        
        // 任务执行器，执行消费动作
        Executor executor;                 // null on error
        
        // 被阻塞的中介所在的线程
        Thread waiter;                     // blocked producer thread
        
        // 挂起来自生产者的错误，在异常关闭消费者时传递给消费者
        Throwable pendingError;            // holds until onError issued
        
        // 所有向生产者注册的中介按注册的先后顺序串联起来，next指向队头
        BufferedSubscription<T> next;      // used only by publisher
        
        // 生产者向中介推送消息时，如果中介的消息队列满了或因为其他意外而无法存储消息，则将这些中介统一记录，并串联起来，nextRetry指向链条上的下一个中介
        BufferedSubscription<T> nextRetry; // used only by publisher
        
        
        // VarHandle mechanics
        static final VarHandle CTL;
        static final VarHandle DEMAND;
        static final VarHandle QA;
        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                CTL = l.findVarHandle(BufferedSubscription.class, "ctl", int.class);
                DEMAND = l.findVarHandle(BufferedSubscription.class, "demand", long.class);
                QA = MethodHandles.arrayElementVarHandle(Object[].class);
            } catch(ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
            
            // Reduce the risk of rare disastrous classloading in first call to
            // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
            Class<?> ensureLoaded = LockSupport.class;
        }
        
        
        
        /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * maxBufferCapacity 消息队列最大容量
         */
        BufferedSubscription(Subscriber<? super T> subscriber, Executor executor, BiConsumer<? super Subscriber<? super T>, ? super Throwable> onNextHandler, Object[] array, int maxBufferCapacity) {
            this.subscriber = subscriber;
            this.executor = executor;
            this.onNextHandler = onNextHandler;
            this.array = array;
            this.maxCapacity = maxBufferCapacity;
        }
        
        /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 注册 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        // 将中介注册到生产者时的回调
        final void onSubscribe() {
            startOnSignal(RUN | ACTIVE);
        }
        
        /*▲ 注册 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 启动 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Tries to start consumer task. Sets error state on failure.
         */
        // 尝试启动中介
        final void tryStart() {
            try {
                // 将中介包装到ConsumerTask中
                ConsumerTask<T> task = new ConsumerTask<T>(this);
                
                Executor e = executor;
                if(e != null) {   // skip if disabled on error
                    /*
                     * 使用Executor执行Runnable类型的任务
                     * 如果是ForkJoinPool，会辗转调用到ConsumerTask的exec()方法
                     * 如果是ThreadPoolExecutor，会辗转调用到ConsumerTask的run()方法
                     * 无论是exec()还是run()，都会调用中介的consume()方法
                     */
                    e.execute(task);
                }
            } catch(RuntimeException | Error ex) {
                getAndBitwiseOrCtl(ERROR | CLOSED);
                throw ex;
            }
        }
        
        /*▲ 启动 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 生产/推送/发布 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Tries to add item and start consumer task if necessary.
         *
         * @return negative if closed, 0 if saturated, else estimated lag
         */
        /*
         * 将消息item添加到中介的消息队列中以待消费
         * 返回值表示消息队列长度，返回0表示中介的消息队列已满，返回负数表示中介已关闭
         */
        final int offer(T item, boolean unowned) {
            Object[] a;
            int stat = 0;
            int cap = ((a = array) == null) ? 0 : a.length;
            int t = tail;
            int i = t & (cap - 1);  // 消息入队下标
            int n = t + 1 - head;   // 假如消息成功入队，队列的新长度
            
            if(cap>0) {
                boolean added;
                // 如果消息队列已满，且消息队列容量未达上限，则需要扩容
                if(n >= cap && cap<maxCapacity) {
                    // 对消息队列扩容，返回值表示扩容是否成功
                    added = growAndOffer(item, a, t);
                    
                    /*
                     * 1.消息队列已满，且容量达到了上限
                     * 2.消息队列未满
                     */
                } else if(n >= cap || unowned) { // need volatile CAS
                    /*
                     * 1.消息队列已满，且容量达到了上限
                     * 2.消息队列未满，但生产行为没有发生在首次注册消费者时生产者所在线程中
                     *
                     * 如果i处的消息为null，则向其填充新消息item，否则的话，item会丢失
                     */
                    added = QA.compareAndSet(a, i, null, item);
                } else {                             // can use release mode
                    /*
                     * 消息队列未满，且生产行为发生在首次注册消费者时生产者所在线程中
                     * 直接更新下标i处的消息
                     */
                    QA.setRelease(a, i, item);
                    added = true;
                }
                
                // 如果消息入队成功，则更新队尾游标，并记录消息数量
                if(added) {
                    tail = t + 1;
                    stat = n;
                }
            }
            
            // 生产消息后，确保中介是活动的（必要的时候再次启动中介）
            return startOnOffer(stat);
        }
        
        /**
         * Tries to expand buffer and add item, returning true on
         * success. Currently fails only if out of memory.
         */
        // 对消息队列扩容，返回值表示扩容是否成功
        final boolean growAndOffer(T item, Object[] a, int t) {
            int cap = 0, newCap = 0;
            
            Object[] newArray = null;
            
            if(a != null && (cap = a.length)>0 && (newCap = cap << 1)>0) {
                try {
                    newArray = new Object[newCap];
                } catch(OutOfMemoryError ex) {
                }
            }
            
            // 扩容失败
            if(newArray == null) {
                return false;
            }
            
            // take and move items
            int newMask = newCap - 1;
            newArray[t-- & newMask] = item;
            // 从后往前将消息从旧队列复制到新队列，从后往前的目的是排除已经消费掉的消息
            for(int mask = cap - 1, k = mask; k >= 0; --k) {
                Object x = QA.getAndSet(a, t & mask, null);
                
                // already consumed
                if(x == null) {
                    break;
                }
                
                newArray[t-- & newMask] = x;
            }
            
            array = newArray;
            
            VarHandle.releaseFence();         // release array and slots
            
            return true;
        }
        
        /**
         * Version of offer for retries (no resize or bias)
         */
        // 尝试重新推送/生产消息，返回值代表消息队列长度，返回0表示推送/生产失败（比如满了），返回负数说明中介已关闭
        final int retryOffer(T item) {
            Object[] a;
            
            int stat = 0, t = tail, h = head, cap;
            
            if((a = array) != null && (cap = a.length)>0 && QA.compareAndSet(a, (cap - 1) & t, null, item)) {
                stat = (tail = t + 1) - h;
            }
            
            // 生产消息后，确保中介是活动的（必要的时候再次启动中介）
            return startOnOffer(stat);
        }
        
        // Consumer task actions
        
        /**
         * Tries to start consumer task after offer.
         *
         * @return negative if now closed, else argument
         */
        // 生产消息后，确保中介是活动的（必要的时候再次启动中介），返回-1表示中介已关闭
        final int startOnOffer(int stat) {
            int c; // start or keep alive if requests exist and not active
            
            // 如果在生产消息时发现存在REQS标记但不存在ACTIVE标记，说明消费者可能已经不再RUN状态了，此时需要重新添加RUN和ACTIVE标记
            if(((c = ctl) & (REQS | ACTIVE)) == REQS && ((c = getAndBitwiseOrCtl(RUN | ACTIVE)) & (RUN | CLOSED)) == 0) {
                // 如果中介确实不在RUN或CLOSED状态，则尝试再次启动中介
                tryStart();
                
                // 如果中介已关闭，返回一个负值
            } else if((c & CLOSED) != 0) {
                stat = -1;
            }
            
            return stat;
        }
        
        /*▲ 生产/推送/发布 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 消费/订阅/接收 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Consumer loop, called from ConsumerTask, or indirectly when helping during submit.
         */
        // 中介已经启动，接下来连通生产者和消费者
        final void consume() {
            // 获取消费者
            Subscriber<? super T> s = subscriber;
            if(s==null){    // hoist checks
                return;
            }
            
            // 中介打开连接通道，允许生产者和消费者进行通信
            subscribeOnOpen(s);
            
            // 消费者申请消费的消息数量
            long d = demand;
            
            // 遍历消息队列
            for(int h = head, t = tail; ; ) {
                int c = ctl;
                
                // 如果中介已被异常关闭
                if((c & ERROR) != 0) {
                    closeOnError(s, null);
                    break;
                }
                
                // 根据消费者的请求，取出消息以待消费
                int taken = takeItems(s, d, h);
                
                // 如果成功消费了消息
                if(taken>0) {
                    // 队头游标前进
                    head = h += taken;
                    // 从消费者申请消费的消息数量中减去已消费的消息数量
                    d = subtractDemand(taken);
                    
                    /*
                     * 如果已经满足了消费者之前的需求，
                     * 则再次从demand中获取消费者申请消费的消息数量
                     * 如果消费者不再申请消费，则移除请求标记REQS
                     */
                } else if((d = demand) == 0L && (c & REQS) != 0) {
                    weakCasCtl(c, c & ~REQS);    // exhausted demand
                    
                    // 如果消费者仍在请求消费，则确保请求标记REQS存在
                } else if(d != 0L && (c & REQS) == 0) {
                    weakCasCtl(c, c | REQS);     // new demand
                    
                    // 如果队尾没有发生变化
                } else if(t == (t = tail)) {      // stability check
                    boolean empty;
                    
                    // 如果消息队列已经为null，且中介已被关闭时
                    if((empty = (t == h)) && (c & COMPLETE) != 0) {
                        // 正常关闭中介
                        closeOnComplete(s);      // end of stream
                        break;
                    }
                    
                    /*
                     * 1.消息队列已经为null，但中介没有COMPLETE标记
                     * 2.消息队列不为null，但消费者没有再申请消费消息
                     */
                    if(empty || d == 0L) {
                        int bit = ((c & ACTIVE) != 0) ? ACTIVE : RUN;
                        
                        /*
                         * 中介处于ACTIVE状态，则清除ACTIVE标记，
                         * 中介没有处于ACTIVE状态，则清除RUN标记，并结束循环
                         */
                        if(weakCasCtl(c, c & ~bit) && bit == RUN) {
                            break;               // un-keep-alive or exit
                        }
                    }
                }
            }
        }
        
        /**
         * Issues subscriber.onSubscribe if this is first signal.
         */
        // 中介打开通道，以便生产者和消费者进行通信
        final void subscribeOnOpen(Subscriber<? super T> s) {
            // 如果消费者已经开启了消费行为，直接返回
            if((ctl & OPEN) != 0){
                return;
            }
            
            // 设置消费开启标记
            if((getAndBitwiseOrCtl(OPEN) & OPEN) == 0) {
                // 通知消费者中介已准备就绪
                consumeSubscribe(s);
            }
        }
        
        // ★★★ 调用消费者的onSubscribe()，通知消费者中介已准备就绪
        final void consumeSubscribe(Subscriber<? super T> s) {
            try {
                // ignore if disabled
                if(s != null) {
                    /*
                     * 中介已准备就绪时的回调（中介刚刚设置了OPEN标记）
                     * 一般在这里需要拿到中介的引用，进而向生产者发出消费请求（参见request()）
                     */
                    s.onSubscribe(this);
                }
            } catch(Throwable ex) {
                closeOnError(s, ex);
            }
        }
        
        /**
         * Consumes some items until unavailable or bound or error.
         *
         * @param s subscriber
         * @param d current demand
         * @param h current head
         *
         * @return number taken
         */
        /*
         * 根据消费者的请求，取出消息队列中的消息以便后续消费
         *
         * s 消费者
         * d 当前需要消费的消息数量
         * h 消息队列队头
         *
         * 返回本次消费的消息数量
         */
        final int takeItems(Subscriber<? super T> s, long d, int h) {
            Object[] a;
            
            int k = 0, cap;
            
            if((a = array) != null && (cap = a.length)>0) {
                int m = cap - 1, b = (m >>> 3) + 1; // min(1, cap/8)
                
                int n = (d<(long) b) ? (int) d : b;
                
                for(; k<n; ++h, ++k) {
                    // 从head处获取消息
                    Object x = QA.getAndSet(a, h & m, null);
                    
                    // 如果中介处于阻塞状态
                    if(waiting != 0) {
                        // 解除对中介的阻塞（随后生产者可以继续推送消息）
                        signalWaiter();
                    }
                    
                    // 如果还没有待消费消息，退出循环
                    if(x == null) {
                        break;
                    }
                    
                    // 让消费者消费消息，如果消费失败，退出循环
                    if(!consumeNext(s, x)) {
                        break;
                    }
                }
            }
            
            return k;
        }
        
        // ★★★ 调用消费者的onNext()，由消费者s消费消息x
        final boolean consumeNext(Subscriber<? super T> s, Object x) {
            try {
                @SuppressWarnings("unchecked")
                T y = (T) x;
                
                if(s != null) {
                    /*
                     * 消费者开始消费指定的消息(item)
                     * 一般在这里消费完成后，需要继续向生产者发出消费请求（参见request()）
                     */
                    s.onNext(y);
                }
                return true;
            } catch(Throwable ex) {
                // 消费者在消费消息的过程中发生了异常，则需要将控制权移交给消费者以决定如何处理异常
                handleOnNext(s, ex);
                return false;
            }
        }
        
        /**
         * Processes exception in Subscriber.onNext.
         */
        // 消费者在消费消息的过程中发生了异常，则需要将控制权移交给消费者以决定如何处理异常
        final void handleOnNext(Subscriber<? super T> s, Throwable ex) {
            BiConsumer<? super Subscriber<? super T>, ? super Throwable> h;
            
            try {
                // 如果该handler存在
                if((h = onNextHandler) != null) {
                    // 将控制权移交给消费者
                    h.accept(s, ex);
                }
            } catch(Throwable ignore) {
            }
            
            // 异常关闭中介
            closeOnError(s, ex);
        }
        
        /*▲ 消费/订阅/接收 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        // （从生产者内部调用）在正常关闭中介，或使用已经正常关闭的中介时的回调
        final void onComplete() {
            startOnSignal(RUN | ACTIVE | COMPLETE);
        }
        
        // （从生产者内部调用）在异常关闭中介，或使用已经异常关闭的中介，或使用中介时发生异常时的回调
        final void onError(Throwable ex) {
            int c;
            Object[] a;      // to null out buffer on async error
            
            if(ex != null) {
                pendingError = ex;  // races are OK
            }
            
            c = getAndBitwiseOrCtl(RUN | ACTIVE | ERROR);
            
            // 如果中介还未关闭
            if((c & CLOSED) == 0) {
                // 重新启动
                if((c & RUN) == 0) {
                    tryStart();
                } else if((a = array) != null) {
                    // 清空消息队列
                    Arrays.fill(a, null);
                }
            }
        }
        
        /**
         * Issues subscriber.onComplete unless already closed.
         */
        // （从中介内部调用）如果消费者在消费的时候，消息队列已经消费完了，且中介已被关闭时，调用此方法通知消费者
        final void closeOnComplete(Subscriber<? super T> s) {
            // 如果中介尚未CLOSED，则为其设置CLOSED标记
            if((getAndBitwiseOrCtl(CLOSED) & CLOSED) == 0) {
                consumeComplete(s);
            }
        }
        
        // ★★★ 调用消费者的onComplete()
        final void consumeComplete(Subscriber<? super T> s) {
            try {
                if(s != null) {
                    s.onComplete();
                }
            } catch(Throwable ignore) {
            }
        }
        
        /**
         * Issues subscriber.onError, and unblocks producer if needed.
         */
        // （从中介内部调用）如果消费者在注册时（onSubscribe()内部）发生了异常，或者在消费前，发现中介已被异常关闭，或者在消费中（onNext()内部）发生了异常，调用此方法通知消费者
        final void closeOnError(Subscriber<? super T> s, Throwable ex) {
            if((getAndBitwiseOrCtl(ERROR | CLOSED) & CLOSED) == 0) {
                if(ex == null) {
                    ex = pendingError;
                }
                
                pendingError = null;  // detach
                executor = null;      // suppress racing start calls
                
                // 解除对中介的阻塞
                signalWaiter();
                
                consumeError(s, ex);
            }
        }
        
        // ★★★ 调用消费者的onError()
        final void consumeError(Subscriber<? super T> s, Throwable ex) {
            try {
                if(ex != null && s != null) {
                    // 如果是消费前就发现中介已被异常关闭，则不会回调此方法
                    s.onError(ex);
                }
            } catch(Throwable ignore) {
            }
        }
        
        /**
         * Returns true if closed (consumer task may still be running).
         */
        // 判断中介是否关闭了连接通道（消费者可能仍在运行）
        final boolean isClosed() {
            return (ctl & CLOSED) != 0;
        }
        
        /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ 中介状态 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Sets the given control bits, starting task if not running or closed.
         *
         * @param bits state bits, assumed to include RUN but not CLOSED
         */
        // 更新中介状态，如果中介尚未RUN或者CLOSED，则尝试启动中介
        final void startOnSignal(int bits) {
            // 如果bits已存在，不需要更改
            if((ctl & bits) == bits){
                return;
            }
            
            // 为ctl添加状态bits，并返回添加前ctl的值
            int oldCtl = getAndBitwiseOrCtl(bits);
            
            // 如果ctl更新前既不包含RUN标记，也不包含CLOSED标记
            if((oldCtl & (RUN | CLOSED)) == 0) {
                tryStart();
            }
        }
        
        // 原子地将ctl从cmp更新到val
        final boolean weakCasCtl(int cmp, int val) {
            return CTL.weakCompareAndSet(this, cmp, val);
        }
        
        // 更新ctl到(ctl | bits)，且返回更新前的ctl
        final int getAndBitwiseOrCtl(int bits) {
            return (int) CTL.getAndBitwiseOr(this, bits);
        }
        
        /*▲ 中介状态 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ Subscription ████████████████████████████████████████████████████████████████████████████████┓ */
        
        // 消费者通过中介发出消费请求
        public final void request(long n) {
            if(n>0L) {
                for(; ; ) {
                    long p = demand;
                    long d = p + n;  // saturate
                    
                    // 原子地更新demand
                    if(casDemand(p, d<p ? Long.MAX_VALUE : d)) {
                        break;
                    }
                }
                
                startOnSignal(RUN | ACTIVE | REQS);
            } else {
                onError(new IllegalArgumentException("non-positive subscription request"));
            }
        }
        
        // （尽力）使消费者停止接收消息
        public final void cancel() {
            onError(null);
        }
        
        /*▲ Subscription ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        /*▼ ManagedBlocker ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Returns true if closed or space available.
         * For ManagedBlocker.
         */
        /*
         * 当前中介（所在线程）是否需要解除阻塞
         *
         * 如果中介已关闭，或中介内的消息队列未满，则返回true
         */
        public final boolean isReleasable() {
            Object[] a;
            int cap;
            return (ctl & CLOSED) != 0 // 中介已关闭
                || ((a = array) != null && (cap = a.length)>0 && QA.getAcquire(a, (cap - 1) & tail) == null);   // 中介内的消息队列未满
        }
        
        /**
         * Blocks until closed, space available or timeout.
         * For ManagedBlocker.
         */
        // 阻塞中介（所在线程），阻塞的目的是让生产者停下来等待消费者完成消费动作
        public final boolean block() {
            long nanos = timeout;
            
            // 是否使用超时（当timeout为Long.MAX_VALUE时被认为需要无限等待）
            boolean timed = (nanos<Long.MAX_VALUE);
            
            long deadline = timed ? System.nanoTime() + nanos : 0L;
            
            // 如果还不能解除阻塞
            while(!isReleasable()) {
                // 测试当前线程是否已经中断，线程的中断状态会被清除
                if(Thread.interrupted()) {
                    // 如果线程已经设置为中断，则结束阻塞
                    timeout = INTERRUPTED;
                    if(timed) {
                        break;
                    }
                    
                    // 如果阻塞已经超时，则退出阻塞
                } else if(timed && (nanos = deadline - System.nanoTime())<=0L) {
                    break;
                    
                    // 初始化被阻塞的中介所在的线程
                } else if(waiter == null) {
                    waiter = Thread.currentThread();
                    
                    // 更新waiting为非零，表示中介被阻塞
                } else if(waiting == 0) {
                    waiting = 1;
                    
                    // 如果设置了超时限制
                } else if(timed) {
                    LockSupport.parkNanos(this, nanos);
                } else {
                    // 否则一直阻塞，直到被主动唤醒
                    LockSupport.park(this);
                }
            }
            
            // 还原标记
            waiter = null;
            waiting = 0;
            
            return true;
        }
        
        /*▲ ManagedBlocker ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        // 从消费者申请消费的消息数量中减去已消费的消息数量
        final long subtractDemand(int k) {
            long n = (long) (-k);
            return n + (long) DEMAND.getAndAdd(this, n);
        }
        
        // 原子地将demand从cmp更新到val
        final boolean casDemand(long cmp, long val) {
            return DEMAND.compareAndSet(this, cmp, val);
        }
        
        /**
         * Returns estimated number of buffered items, or negative if closed.
         */
        // 返回缓存的消息数量(估计值)，如果中介通道已关闭，则返回负数
        final int estimateLag() {
            int n = tail - head;
            return isClosed() ? -1 : (n<0) ? 0 : n;
        }
        
        /**
         * Helps or blocks until timeout, closed, or space available.
         */
        /*
         * 推送失败并重试之前需要阻塞/等待
         * 如果nanos>0，说明需要阻塞一会儿（等待消费者消费）之后再去重试
         */
        final void awaitSpace(long nanos) {
            // 如果中介未关闭，但是消息队列满了，则需要考虑阻塞
            if(!isReleasable()) {
                
                // 阻塞之前，尝试加速消费者的消费行为
                ForkJoinPool.helpAsyncBlocker(executor, this);
                
                // 再次判断是否需要阻塞
                if(!isReleasable()) {
                    timeout = nanos;
                    
                    try {
                        // 管理阻塞块（决定中介所在线程是否需要阻塞）
                        ForkJoinPool.managedBlock(this);
                    } catch(InterruptedException ie) {
                        timeout = INTERRUPTED;
                    }
                    
                    // 如果发生了中断异常，为当前线程设置中断标记
                    if(timeout == INTERRUPTED) {
                        // 中断线程（只是给线程预设一个标记，不是立即让线程停下来）
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        /**
         * Unblocks waiting producer.
         */
        // 解除对中介的阻塞（随后生产者可以继续推送消息，但具体能不能推送，还得看中介有没有被关闭）
        final void signalWaiter() {
            Thread w;
            waiting = 0;
            if((w = waiter) != null) {
                LockSupport.unpark(w);
            }
        }
    }
    
    /**
     * A task for consuming buffer items and signals, created and
     * executed whenever they become available. A task consumes as
     * many items/signals as possible before terminating, at which
     * point another task is created when needed. The dual Runnable
     * and ForkJoinTask declaration saves overhead when executed by
     * ForkJoinPools, without impacting other kinds of Executors.
     */
    // ForkJoinTask的实现类，用于包装中介，执行中介的消费动作
    @SuppressWarnings("serial")
    static final class ConsumerTask<T> extends ForkJoinTask<Void> implements Runnable, CompletableFuture.AsynchronousCompletionTask {
        final BufferedSubscription<T> consumer;
        
        ConsumerTask(BufferedSubscription<T> consumer) {
            this.consumer = consumer;
        }
        
        public final Void getRawResult() {
            return null;
        }
        
        public final void setRawResult(Void v) {
        }
        
        // 执行任务，如任务容器是ForkJoinPool
        public final boolean exec() {
            consumer.consume();
            return false;
        }
        
        // 执行任务，如任务容器是ThreadPoolExecutor
        public final void run() {
            consumer.consume();
        }
    }
    
    /** Subscriber for method consume */
    // 消费者，【消费】消息和信号
    static final class ConsumerSubscriber<T> implements Subscriber<T> {
        final CompletableFuture<Void> status;
        final Consumer<? super T> consumer;
        Subscription subscription;
    
        ConsumerSubscriber(CompletableFuture<Void> status, Consumer<? super T> consumer) {
            this.status = status;
            this.consumer = consumer;
        }
    
        /*
         * 中介已准备就绪时的回调（中介刚刚设置了OPEN标记）
         * 一般在这里需要拿到中介的引用，进而向生产者发出消费请求
         */
        public final void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
        
            status.whenComplete((v, e) -> subscription.cancel());
        
            if(!status.isDone()) {
                subscription.request(Long.MAX_VALUE);
            }
        }
    
        /*
         * 消费者开始消费指定的消息(item)
         * 一般在这里消费完成后，需要继续向生产者发出消费请求
         */
        public final void onNext(T item) {
            try {
                consumer.accept(item);
            } catch(Throwable ex) {
                subscription.cancel();
                status.completeExceptionally(ex);
            }
        }
    
        // （从中介内部调用）如果消费者在消费的时候，消息队列已经消费完了，且中介已被关闭时，回调此方法
        public final void onComplete() {
            status.complete(null);
        }
    
        // （从中介内部调用）如果消费者在注册时（onSubscribe()内部）发生了异常，或者在消费中（onNext()内部）发生了异常，回调此方法
        public final void onError(Throwable ex) {
            status.completeExceptionally(ex);
        }
    
    }
    
    /** Fallback if ForkJoinPool.commonPool() cannot support parallelism */
    // 如果ForkJoinPool中的共享工作池不支持并行，则使用该线程池
    private static final class ThreadPerTaskExecutor implements Executor {
        ThreadPerTaskExecutor() {
        }      // prevent access constructor creation
        
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }
    
}
