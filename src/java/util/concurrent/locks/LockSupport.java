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

package java.util.concurrent.locks;

import java.util.concurrent.ThreadLocalRandom;
import jdk.internal.misc.Unsafe;

/**
 * Basic thread blocking primitives for creating locks and other
 * synchronization classes.
 *
 * <p>This class associates, with each thread that uses it, a permit
 * (in the sense of the {@link java.util.concurrent.Semaphore
 * Semaphore} class). A call to {@code park} will return immediately
 * if the permit is available, consuming it in the process; otherwise
 * it <em>may</em> block.  A call to {@code unpark} makes the permit
 * available, if it was not already available. (Unlike with Semaphores
 * though, permits do not accumulate. There is at most one.)
 * Reliable usage requires the use of volatile (or atomic) variables
 * to control when to park or unpark.  Orderings of calls to these
 * methods are maintained with respect to volatile variable accesses,
 * but not necessarily non-volatile variable accesses.
 *
 * <p>Methods {@code park} and {@code unpark} provide efficient
 * means of blocking and unblocking threads that do not encounter the
 * problems that cause the deprecated methods {@code Thread.suspend}
 * and {@code Thread.resume} to be unusable for such purposes: Races
 * between one thread invoking {@code park} and another thread trying
 * to {@code unpark} it will preserve liveness, due to the
 * permit. Additionally, {@code park} will return if the caller's
 * thread was interrupted, and timeout versions are supported. The
 * {@code park} method may also return at any other time, for "no
 * reason", so in general must be invoked within a loop that rechecks
 * conditions upon return. In this sense {@code park} serves as an
 * optimization of a "busy wait" that does not waste as much time
 * spinning, but must be paired with an {@code unpark} to be
 * effective.
 *
 * <p>The three forms of {@code park} each also support a
 * {@code blocker} object parameter. This object is recorded while
 * the thread is blocked to permit monitoring and diagnostic tools to
 * identify the reasons that threads are blocked. (Such tools may
 * access blockers using method {@link #getBlocker(Thread)}.)
 * The use of these forms rather than the original forms without this
 * parameter is strongly encouraged. The normal argument to supply as
 * a {@code blocker} within a lock implementation is {@code this}.
 *
 * <p>These methods are designed to be used as tools for creating
 * higher-level synchronization utilities, and are not in themselves
 * useful for most concurrency control applications.  The {@code park}
 * method is designed for use only in constructions of the form:
 *
 * <pre> {@code
 * while (!canProceed()) {
 *   // ensure request to unpark is visible to other threads
 *   ...
 *   LockSupport.park(this);
 * }}</pre>
 *
 * where no actions by the thread publishing a request to unpark,
 * prior to the call to {@code park}, entail locking or blocking.
 * Because only one permit is associated with each thread, any
 * intermediary uses of {@code park}, including implicitly via class
 * loading, could lead to an unresponsive thread (a "lost unpark").
 *
 * <p><b>Sample Usage.</b> Here is a sketch of a first-in-first-out
 * non-reentrant lock class:
 * <pre> {@code
 * class FIFOMutex {
 *   private final AtomicBoolean locked = new AtomicBoolean(false);
 *   private final Queue<Thread> waiters
 *     = new ConcurrentLinkedQueue<>();
 *
 *   public void lock() {
 *     boolean wasInterrupted = false;
 *     // publish current thread for unparkers
 *     waiters.add(Thread.currentThread());
 *
 *     // Block while not first in queue or cannot acquire lock
 *     while (waiters.peek() != Thread.currentThread() ||
 *            !locked.compareAndSet(false, true)) {
 *       LockSupport.park(this);
 *       // ignore interrupts while waiting
 *       if (Thread.interrupted())
 *         wasInterrupted = true;
 *     }
 *
 *     waiters.remove();
 *     // ensure correct interrupt status on return
 *     if (wasInterrupted)
 *       Thread.currentThread().interrupt();
 *   }
 *
 *   public void unlock() {
 *     locked.set(false);
 *     LockSupport.unpark(waiters.peek());
 *   }
 *
 *   static {
 *     // Reduce the risk of "lost unpark" due to classloading
 *     Class<?> ensureLoaded = LockSupport.class;
 *   }
 * }}</pre>
 *
 * @since 1.5
 */
/*
 * 管理线程状态（阻塞/唤醒线程）
 *
 * 阻塞/唤醒的语义在不同的场景下可以理解为生产/消费许可证，也可以理解为借出/归还许可证
 *
 * 标记为中断的线程无法阻塞（阻塞不起作用），但线程阻塞期间，可以对其设置中断状态
 *
 * 与wait、sleep、join这些阻塞不同的是，park期间设置中断标记不会触发中断异常
 */
public class LockSupport {
    // Hotspot implementation via intrinsics API
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long PARKBLOCKER = U.objectFieldOffset(Thread.class, "parkBlocker");
    private static final long SECONDARY   = U.objectFieldOffset(Thread.class, "threadLocalRandomSecondarySeed");
    private static final long TID         = U.objectFieldOffset(Thread.class, "tid");
    
    private LockSupport() {
    } // Cannot be instantiated.
    
    /**
     * Makes available the permit for the given thread, if it
     * was not already available.  If the thread was blocked on
     * {@code park} then it will unblock.  Otherwise, its next call
     * to {@code park} is guaranteed not to block. This operation
     * is not guaranteed to have any effect at all if the given
     * thread has not been started.
     *
     * @param thread the thread to unpark, or {@code null}, in which case
     *               this operation has no effect
     */
    /*
     * 发给目标线程一个许可证，该许可证被park消费，用于唤醒被park阻塞的线程
     *
     * 该许可证可以提前发给线程备用，也可以等线程陷入阻塞后，在等待许可证时再（由另一个线程）给它，进而唤醒线程。
     * 连续重复发给线程的许可证只被视为一个许可证。
     */
    public static void unpark(Thread thread) {
        if(thread != null) {
            U.unpark(thread);
        }
    }
    
    /**
     * Disables the current thread for thread scheduling purposes unless the
     * permit is available.
     *
     * <p>If the permit is available then it is consumed and the call returns
     * immediately; otherwise
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     *
     * @param blocker the synchronization object responsible for this
     *                thread parking
     *
     * @since 1.6
     */
    /*
     * 等待消费一个许可证，这会使线程陷入阻塞。blocker参数仅作为线程阻塞标记。
     *
     * 如果提前给过许可，则线程继续执行。
     * 如果陷入阻塞后等待许可，则可由别的线程发给它许可（在别的线程中调用unpark）。
     * 使用线程中断也可以唤醒陷入阻塞的线程。
     *
     * 注：对标记为中断的线程使用阻塞无效
     */
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        
        // 标记线程陷入了阻塞
        setBlocker(t, blocker);
        
        // 使线程一直陷入阻塞，直到被唤醒
        U.park(false, 0L);
        
        // 标记线程脱离了阻塞
        setBlocker(t, null);
    }
    
    /**
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param blocker the synchronization object responsible for this
     *                thread parking
     * @param nanos   the maximum number of nanoseconds to wait
     *
     * @since 1.6
     */
    /*
     * 使线程阻塞nanos（单位：纳秒）时长后自动醒来（中途可被唤醒）
     *
     * 注：对标记为中断的线程使用阻塞无效
     */
    public static void parkNanos(Object blocker, long nanos) {
        if(nanos>0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            U.park(false, nanos);
            setBlocker(t, null);
        }
    }
    
    /**
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * @param blocker  the synchronization object responsible for this
     *                 thread parking
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *                 to wait until
     *
     * @since 1.6
     */
    /*
     * 使线程陷入阻塞，直到deadline（以毫秒为单位的Unix时间戳）时间点时才醒来（中途可被唤醒）
     *
     * 注：对标记为中断的线程使用阻塞无效
     */
    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        U.park(true, deadline);
        setBlocker(t, null);
    }
    
    /**
     * Disables the current thread for thread scheduling purposes unless the
     * permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of three
     * things happens:
     *
     * <ul>
     *
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     */
    // 作用同park(blocker)方法，只是不设置阻塞标记
    public static void park() {
        U.park(false, 0L);
    }
    
    /**
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param nanos the maximum number of nanoseconds to wait
     */
    // 作用同parkNanos(blocker, nanos)方法，只是不设置阻塞标记
    public static void parkNanos(long nanos) {
        if(nanos>0)
            U.park(false, nanos);
    }
    
    /**
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *                 to wait until
     */
    // 作用同parkUntil(blocker, nanos)方法，只是不设置阻塞标记
    public static void parkUntil(long deadline) {
        U.park(true, deadline);
    }
    
    /**
     * Returns the pseudo-randomly initialized or updated secondary seed.
     * Copied from ThreadLocalRandom due to package access restrictions.
     */
    // 获取当前线程内的随机数生成器，并返回生成的种子
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if((r = U.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        } else if((r = ThreadLocalRandom.current().nextInt()) == 0) {
            r = 1; // avoid zero
        }
        U.putInt(t, SECONDARY, r);
        return r;
    }
    
    // 获取线程id
    /**
     * Returns the thread id for the given thread.  We must access
     * this directly rather than via method Thread.getId() because
     * getId() has been known to be overridden in ways that do not
     * preserve unique mappings.
     */
    static final long getThreadId(Thread thread) {
        return U.getLong(thread, TID);
    }
    
    /**
     * Returns the blocker object supplied to the most recent
     * invocation of a park method that has not yet unblocked, or null
     * if not blocked.  The value returned is just a momentary
     * snapshot -- the thread may have since unblocked or blocked on a
     * different blocker object.
     *
     * @param t the thread
     *
     * @return the blocker
     *
     * @throws NullPointerException if argument is null
     * @since 1.6
     */
    // 获取线程的阻塞标记
    public static Object getBlocker(Thread t) {
        if(t == null) {
            throw new NullPointerException();
        }
        return U.getObjectVolatile(t, PARKBLOCKER);
    }
    
    // 设置线程t的parkBlocker字段为arg，用来标记该线程陷入了阻塞
    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        U.putObject(t, PARKBLOCKER, arg);
    }
    
}
