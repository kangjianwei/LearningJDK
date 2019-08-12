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

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import jdk.internal.vm.annotation.ReservedStackAccess;

/**
 * A capability-based lock with three modes for controlling read/write
 * access.  The state of a StampedLock consists of a version and mode.
 * Lock acquisition methods return a stamp that represents and
 * controls access with respect to a lock state; "try" versions of
 * these methods may instead return the special value zero to
 * represent failure to acquire access. Lock release and conversion
 * methods require stamps as arguments, and fail if they do not match
 * the state of the lock. The three modes are:
 *
 * <ul>
 *
 *  <li><b>Writing.</b> Method {@link #writeLock} possibly blocks
 *   waiting for exclusive access, returning a stamp that can be used
 *   in method {@link #unlockWrite} to release the lock. Untimed and
 *   timed versions of {@code tryWriteLock} are also provided. When
 *   the lock is held in write mode, no read locks may be obtained,
 *   and all optimistic read validations will fail.
 *
 *  <li><b>Reading.</b> Method {@link #readLock} possibly blocks
 *   waiting for non-exclusive access, returning a stamp that can be
 *   used in method {@link #unlockRead} to release the lock. Untimed
 *   and timed versions of {@code tryReadLock} are also provided.
 *
 *  <li><b>Optimistic Reading.</b> Method {@link #tryOptimisticRead}
 *   returns a non-zero stamp only if the lock is not currently held
 *   in write mode. Method {@link #validate} returns true if the lock
 *   has not been acquired in write mode since obtaining a given
 *   stamp.  This mode can be thought of as an extremely weak version
 *   of a read-lock, that can be broken by a writer at any time.  The
 *   use of optimistic mode for short read-only code segments often
 *   reduces contention and improves throughput.  However, its use is
 *   inherently fragile.  Optimistic read sections should only read
 *   fields and hold them in local variables for later use after
 *   validation. Fields read while in optimistic mode may be wildly
 *   inconsistent, so usage applies only when you are familiar enough
 *   with data representations to check consistency and/or repeatedly
 *   invoke method {@code validate()}.  For example, such steps are
 *   typically required when first reading an object or array
 *   reference, and then accessing one of its fields, elements or
 *   methods.
 *
 * </ul>
 *
 * <p>This class also supports methods that conditionally provide
 * conversions across the three modes. For example, method {@link
 * #tryConvertToWriteLock} attempts to "upgrade" a mode, returning
 * a valid write stamp if (1) already in writing mode (2) in reading
 * mode and there are no other readers or (3) in optimistic mode and
 * the lock is available. The forms of these methods are designed to
 * help reduce some of the code bloat that otherwise occurs in
 * retry-based designs.
 *
 * <p>StampedLocks are designed for use as internal utilities in the
 * development of thread-safe components. Their use relies on
 * knowledge of the internal properties of the data, objects, and
 * methods they are protecting.  They are not reentrant, so locked
 * bodies should not call other unknown methods that may try to
 * re-acquire locks (although you may pass a stamp to other methods
 * that can use or convert it).  The use of read lock modes relies on
 * the associated code sections being side-effect-free.  Unvalidated
 * optimistic read sections cannot call methods that are not known to
 * tolerate potential inconsistencies.  Stamps use finite
 * representations, and are not cryptographically secure (i.e., a
 * valid stamp may be guessable). Stamp values may recycle after (no
 * sooner than) one year of continuous operation. A stamp held without
 * use or validation for longer than this period may fail to validate
 * correctly.  StampedLocks are serializable, but always deserialize
 * into initial unlocked state, so they are not useful for remote
 * locking.
 *
 * <p>Like {@link java.util.concurrent.Semaphore Semaphore}, but unlike most
 * {@link Lock} implementations, StampedLocks have no notion of ownership.
 * Locks acquired in one thread can be released or converted in another.
 *
 * <p>The scheduling policy of StampedLock does not consistently
 * prefer readers over writers or vice versa.  All "try" methods are
 * best-effort and do not necessarily conform to any scheduling or
 * fairness policy. A zero return from any "try" method for acquiring
 * or converting locks does not carry any information about the state
 * of the lock; a subsequent invocation may succeed.
 *
 * <p>Because it supports coordinated usage across multiple lock
 * modes, this class does not directly implement the {@link Lock} or
 * {@link ReadWriteLock} interfaces. However, a StampedLock may be
 * viewed {@link #asReadLock()}, {@link #asWriteLock()}, or {@link
 * #asReadWriteLock()} in applications requiring only the associated
 * set of functionality.
 *
 * <p><b>Sample Usage.</b> The following illustrates some usage idioms
 * in a class that maintains simple two-dimensional points. The sample
 * code illustrates some try/catch conventions even though they are
 * not strictly needed here because no exceptions can occur in their
 * bodies.
 *
 * <pre> {@code
 * class Point {
 *   private double x, y;
 *   private final StampedLock sl = new StampedLock();
 *
 *   // an exclusively locked method
 *   void move(double deltaX, double deltaY) {
 *     long stamp = sl.writeLock();
 *     try {
 *       x += deltaX;
 *       y += deltaY;
 *     } finally {
 *       sl.unlockWrite(stamp);
 *     }
 *   }
 *
 *   // a read-only method
 *   // upgrade from optimistic read to read lock
 *   double distanceFromOrigin() {
 *     long stamp = sl.tryOptimisticRead();
 *     try {
 *       retryHoldingLock: for (;; stamp = sl.readLock()) {
 *         if (stamp == 0L)
 *           continue retryHoldingLock;
 *         // possibly racy reads
 *         double currentX = x;
 *         double currentY = y;
 *         if (!sl.validate(stamp))
 *           continue retryHoldingLock;
 *         return Math.hypot(currentX, currentY);
 *       }
 *     } finally {
 *       if (StampedLock.isReadLockStamp(stamp))
 *         sl.unlockRead(stamp);
 *     }
 *   }
 *
 *   // upgrade from optimistic read to write lock
 *   void moveIfAtOrigin(double newX, double newY) {
 *     long stamp = sl.tryOptimisticRead();
 *     try {
 *       retryHoldingLock: for (;; stamp = sl.writeLock()) {
 *         if (stamp == 0L)
 *           continue retryHoldingLock;
 *         // possibly racy reads
 *         double currentX = x;
 *         double currentY = y;
 *         if (!sl.validate(stamp))
 *           continue retryHoldingLock;
 *         if (currentX != 0.0 || currentY != 0.0)
 *           break;
 *         stamp = sl.tryConvertToWriteLock(stamp);
 *         if (stamp == 0L)
 *           continue retryHoldingLock;
 *         // exclusive access
 *         x = newX;
 *         y = newY;
 *         return;
 *       }
 *     } finally {
 *       if (StampedLock.isWriteLockStamp(stamp))
 *         sl.unlockWrite(stamp);
 *     }
 *   }
 *
 *   // Upgrade read lock to write lock
 *   void moveIfAtOrigin(double newX, double newY) {
 *     long stamp = sl.readLock();
 *     try {
 *       while (x == 0.0 && y == 0.0) {
 *         long ws = sl.tryConvertToWriteLock(stamp);
 *         if (ws != 0L) {
 *           stamp = ws;
 *           x = newX;
 *           y = newY;
 *           break;
 *         }
 *         else {
 *           sl.unlockRead(stamp);
 *           stamp = sl.writeLock();
 *         }
 *       }
 *     } finally {
 *       sl.unlock(stamp);
 *     }
 *   }
 * }}</pre>
 *
 * @since 1.8
 * @author Doug Lea
 */
/*
 * 改进的读写锁
 *
 * 读锁与读锁共存，写锁与写锁互斥，读锁与写锁也互斥
 * 性能较ReentrantReadWriteLock有所提升，
 * 原理是加大了CAS的力度，避免了不断地切换线程上下文
 */
public class StampedLock implements Serializable {
    /*
     * Algorithmic notes:
     *
     * The design employs elements of Sequence locks
     * (as used in linux kernels; see Lameter's
     * http://www.lameter.com/gelato2005.pdf
     * and elsewhere; see
     * Boehm's http://www.hpl.hp.com/techreports/2012/HPL-2012-68.html)
     * and Ordered RW locks (see Shirako et al
     * http://dl.acm.org/citation.cfm?id=2312015)
     *
     * Conceptually, the primary state of the lock includes a sequence
     * number that is odd when write-locked and even otherwise.
     * However, this is offset by a reader count that is non-zero when
     * read-locked.  The read count is ignored when validating
     * "optimistic" seqlock-reader-style stamps.  Because we must use
     * a small finite number of bits (currently 7) for readers, a
     * supplementary reader overflow word is used when the number of
     * readers exceeds the count field. We do this by treating the max
     * reader count value (RBITS) as a spinlock protecting overflow
     * updates.
     *
     * Waiters use a modified form of CLH lock used in
     * AbstractQueuedSynchronizer (see its internal documentation for
     * a fuller account), where each node is tagged (field mode) as
     * either a reader or writer. Sets of waiting readers are grouped
     * (linked) under a common node (field cowait) so act as a single
     * node with respect to most CLH mechanics.  By virtue of the
     * queue structure, wait nodes need not actually carry sequence
     * numbers; we know each is greater than its predecessor.  This
     * simplifies the scheduling policy to a mainly-FIFO scheme that
     * incorporates elements of Phase-Fair locks (see Brandenburg &
     * Anderson, especially http://www.cs.unc.edu/~bbb/diss/).  In
     * particular, we use the phase-fair anti-barging rule: If an
     * incoming reader arrives while read lock is held but there is a
     * queued writer, this incoming reader is queued.  (This rule is
     * responsible for some of the complexity of method acquireRead,
     * but without it, the lock becomes highly unfair.) Method release
     * does not (and sometimes cannot) itself wake up cowaiters. This
     * is done by the primary thread, but helped by any other threads
     * with nothing better to do in methods acquireRead and
     * acquireWrite.
     *
     * These rules apply to threads actually queued. All tryLock forms
     * opportunistically try to acquire locks regardless of preference
     * rules, and so may "barge" their way in.  Randomized spinning is
     * used in the acquire methods to reduce (increasingly expensive)
     * context switching while also avoiding sustained memory
     * thrashing among many threads.  We limit spins to the head of
     * queue. If, upon wakening, a thread fails to obtain lock, and is
     * still (or becomes) the first waiting thread (which indicates
     * that some other thread barged and obtained lock), it escalates
     * spins (up to MAX_HEAD_SPINS) to reduce the likelihood of
     * continually losing to barging threads.
     *
     * Nearly all of these mechanics are carried out in methods
     * acquireWrite and acquireRead, that, as typical of such code,
     * sprawl out because actions and retries rely on consistent sets
     * of locally cached reads.
     *
     * As noted in Boehm's paper (above), sequence validation (mainly
     * method validate()) requires stricter ordering rules than apply
     * to normal volatile reads (of "state").  To force orderings of
     * reads before a validation and the validation itself in those
     * cases where this is not already forced, we use acquireFence.
     * Unlike in that paper, we allow writers to use plain writes.
     * One would not expect reorderings of such writes with the lock
     * acquisition CAS because there is a "control dependency", but it
     * is theoretically possible, so we additionally add a
     * storeStoreFence after lock acquisition CAS.
     *
     * ----------------------------------------------------------------
     * Here's an informal proof that plain reads by _successful_
     * readers see plain writes from preceding but not following
     * writers (following Boehm and the C++ standard [atomics.fences]):
     *
     * Because of the total synchronization order of accesses to
     * volatile long state containing the sequence number, writers and
     * _successful_ readers can be globally sequenced.
     *
     * int x, y;
     *
     * Writer 1:
     * inc sequence (odd - "locked")
     * storeStoreFence();
     * x = 1; y = 2;
     * inc sequence (even - "unlocked")
     *
     * Successful Reader:
     * read sequence (even)
     * // must see writes from Writer 1 but not Writer 2
     * r1 = x; r2 = y;
     * acquireFence();
     * read sequence (even - validated unchanged)
     * // use r1 and r2
     *
     * Writer 2:
     * inc sequence (odd - "locked")
     * storeStoreFence();
     * x = 3; y = 4;
     * inc sequence (even - "unlocked")
     *
     * Visibility of writer 1's stores is normal - reader's initial
     * read of state synchronizes with writer 1's final write to state.
     * Lack of visibility of writer 2's plain writes is less obvious.
     * If reader's read of x or y saw writer 2's write, then (assuming
     * semantics of C++ fences) the storeStoreFence would "synchronize"
     * with reader's acquireFence and reader's validation read must see
     * writer 2's initial write to state and so validation must fail.
     * But making this "proof" formal and rigorous is an open problem!
     * ----------------------------------------------------------------
     *
     * The memory layout keeps lock state and queue pointers together
     * (normally on the same cache line). This usually works well for
     * read-mostly loads. In most other cases, the natural tendency of
     * adaptive-spin CLH locks to reduce memory contention lessens
     * motivation to further spread out contended locations, but might
     * be subject to future improvements.
     */
    
    private static final long serialVersionUID = -6001602636862214147L;
    
    /** Number of processors, for spin control */
    // 虚拟机可用的CPU(核心)个数，现代处理器一般都大于一个核
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    
    /** Maximum number of retries before enqueuing on acquisition; at least 1 */
    private static final int SPINS = (NCPU > 1) ? 1 << 6 : 1;           // 64
    
    /** Maximum number of tries before blocking at head on acquisition */
    private static final int HEAD_SPINS = (NCPU > 1) ? 1 << 10 : 1;     // 1024
    
    /** Maximum number of retries before re-blocking */
    private static final int MAX_HEAD_SPINS = (NCPU > 1) ? 1 << 16 : 1; // 65536
    
    /** The period for yielding when waiting for overflow spinlock */
    private static final int OVERFLOW_YIELD_RATE = 7; // must be power 2 - 1
    
    /** The number of bits to use for reader count before overflowing */
    private static final int LG_READERS = 7;
    
    /*
     * 3 stamp modes can be distinguished by examining (m = stamp & ABITS):
     * write mode: m == WBIT
     * optimistic read mode: m == 0L (even when read lock is held)
     * read mode: m > 0L && m <= RFULL (the stamp is a copy of state, but the
     * read hold count in the stamp is unused other than to determine mode)
     *
     * This differs slightly from the encoding of state:
     * (state & ABITS) == 0L indicates the lock is currently unlocked.
     * (state & ABITS) == RBITS is a special transient value
     * indicating spin-locked to manipulate reader bits overflow.
     */
    
    /*
     * 读锁标记：1 0000 0001 ~ 1 0111 1110
     * 写锁标记：1 1000 0000
     * 乐观读锁：1 0000 0000
     */
    private static final long RUNIT = 1L;               // 0 0000 0001 读锁计数
    private static final long WBIT  = 1L << LG_READERS; // 0 1000 0000 写锁掩码 (m&ABITS == WBIT)
    private static final long RBITS = WBIT - 1L;        // 0 0111 1111 读锁掩码（读锁溢出）
    private static final long RFULL = RBITS - 1L;       // 0 0111 1110 读锁掩码 (0 < m&ABITS <= RFULL)
    private static final long ABITS = RBITS | WBIT;     // 0 1111 1111 读/写锁状态掩码
    // note overlap with ABITS
    private static final long SBITS = ~RBITS;           // 1 1000 0000（高位全为1，屏蔽读锁）
    /** Initial value for lock state; avoids failure value zero. */
    private static final long ORIGIN = WBIT << 1;       // 1 0000 0000 锁状态的初始值（可以认为是乐观锁标记）
    
    // Special value from cancelled acquire methods so caller can throw IE
    private static final long INTERRUPTED = 1L;
    
    // Values for node status; order matters
    private static final int WAITING   = -1;    // 标记后继结点需要阻塞
    private static final int CANCELLED =  1;    // 标记后继结点需要取消
    
    // Modes for nodes (int not boolean to allow arithmetic)
    private static final int RMODE = 0; // 标记后继结点中的线程正在申请读锁
    private static final int WMODE = 1; // 标记后继结点中的线程正在申请写锁（头结点也为WMODE）
    
    /** Lock sequence/state */
    // 锁的状态标记，要么保存多个读锁，要么保存一个写锁，要么保存一个乐观锁
    private transient volatile long state;
    
    /** Head/Tail of CLH queue */
    // 等待队列的主线中的首尾结点（主线上没有连续的读锁线程）
    private transient volatile WNode whead, wtail;
    
    /** extra reader count when state read count saturated */
    // 记录读锁溢出的数量
    private transient int readerOverflow;
    
    
    // views
    transient ReadLockView readLockView;
    transient WriteLockView writeLockView;
    transient ReadWriteLockView readWriteLockView;
    
    // VarHandle mechanics
    private static final VarHandle STATE;
    private static final VarHandle WHEAD;
    private static final VarHandle WTAIL;
    
    private static final VarHandle WNEXT;
    private static final VarHandle WSTATUS;
    private static final VarHandle WCOWAIT;
    
    
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE = l.findVarHandle(StampedLock.class, "state", long.class);
            WHEAD = l.findVarHandle(StampedLock.class, "whead", WNode.class);
            WTAIL = l.findVarHandle(StampedLock.class, "wtail", WNode.class);
            
            WNEXT = l.findVarHandle(WNode.class, "next", WNode.class);
            WCOWAIT = l.findVarHandle(WNode.class, "cowait", WNode.class);
            WSTATUS = l.findVarHandle(WNode.class, "status", int.class);
        } catch(ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new lock, initially in unlocked state.
     */
    public StampedLock() {
        // 初始化锁的状态：0001 0000 0000
        state = ORIGIN;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 锁视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a plain {@link Lock} view of this StampedLock in which
     * the {@link Lock#lock} method is mapped to {@link #readLock},
     * and similarly for other methods. The returned Lock does not
     * support a {@link Condition}; method {@link Lock#newCondition()}
     * throws {@code UnsupportedOperationException}.
     *
     * @return the lock
     */
    // 获取读锁实例
    public Lock asReadLock() {
        ReadLockView v;
        if((v = readLockView) != null) {
            return v;
        }
        return readLockView = new ReadLockView();
    }
    
    /**
     * Returns a plain {@link Lock} view of this StampedLock in which
     * the {@link Lock#lock} method is mapped to {@link #writeLock},
     * and similarly for other methods. The returned Lock does not
     * support a {@link Condition}; method {@link Lock#newCondition()}
     * throws {@code UnsupportedOperationException}.
     *
     * @return the lock
     */
    // 获取写锁实例
    public Lock asWriteLock() {
        WriteLockView v;
        if((v = writeLockView) != null) {
            return v;
        }
        return writeLockView = new WriteLockView();
    }
    
    /**
     * Returns a {@link ReadWriteLock} view of this StampedLock in
     * which the {@link ReadWriteLock#readLock()} method is mapped to
     * {@link #asReadLock()}, and {@link ReadWriteLock#writeLock()} to
     * {@link #asWriteLock()}.
     *
     * @return the lock
     */
    // 获取读/写锁实例，通过它可以进一步获取读锁和写锁的实例
    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v;
        if((v = readWriteLockView) != null) {
            return v;
        }
        return readWriteLockView = new ReadWriteLockView();
    }
    
    /*▲ 锁视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 申请读锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Non-exclusively acquires the lock, blocking if necessary
     * until available.
     *
     * @return a read stamp that can be used to unlock or convert mode
     */
    // 申请读锁，失败后去排队
    @ReservedStackAccess
    public long readLock() {
        long s, next;
        
        // 如果没有排队线程
        if(whead == wtail){
            // 获取当前的锁状态
            long m = (s = state) & ABITS;
            
            // 不存在写锁，且读锁未到溢出边界
            if(m<RFULL){
                // 更新state
                if(casState(s, next = s + RUNIT)){
                    return next;
                }
            }
        }
        
        // 存在排队线程，或者存在写锁，或者读锁处于溢出边界
        return acquireRead(false, 0L);
    }
    
    /**
     * Non-exclusively acquires the lock if it is immediately available.
     *
     * @return a read stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    // 申请读锁，如果存在写锁则申请失败，失败后不阻塞，也不再尝试
    @ReservedStackAccess
    public long tryReadLock() {
        long s, m, next;
        
        // 如果当前不存在写锁
        while((m = (s = state) & ABITS) != WBIT) {
            // 如果读锁未溢出
            if(m<RFULL) {
                if(casState(s, next = s + RUNIT)) {
                    return next;
                }
                
                // 如果读锁处于溢出边界
            } else {
                // 尝试增加溢出标记readerOverflow
                if((next = tryIncReaderOverflow(s)) != 0L) {
                    return next;
                }
            }
        }
        
        return 0L;
    }
    
    /**
     * Non-exclusively acquires the lock if it is available within the
     * given time and the current thread has not been interrupted.
     * Behavior under timeout and interruption matches that specified
     * for method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     *
     * @return a read stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     *
     * @throws InterruptedException if the current thread is interrupted
     *                              before acquiring the lock
     */
    // 申请读锁，超时后失败，未超时则排队（不可阻塞带有中断标记的线程）
    @ReservedStackAccess
    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        long s, m, next, deadline;
        
        // 将当前时间单位下的time换算为纳秒
        long nanos = unit.toNanos(time);
        
        // 如果当前线程没有中断标记
        if(!Thread.interrupted()) {
            // 获取当前的锁状态
            m = (s = state) & ABITS;
            
            // 如果当前不存在写锁
            if(m != WBIT) {
                // 如果锁的数量还没到溢出边界
                if(m<RFULL) {
                    // 更新锁状态
                    if(casState(s, next = s + RUNIT)) {
                        return next;
                    }
                    
                    // 如果当前锁的数量已经在溢出边界了
                } else {
                    // 尝试增加溢出标记readerOverflow
                    if((next = tryIncReaderOverflow(s)) != 0L) {
                        return next;
                    }
                }
            }
            
            // 如果已经超时，本次申请失败
            if(nanos<=0L) {
                return 0L;
            }
            
            // 计算截止时间
            if((deadline = System.nanoTime() + nanos) == 0L) {
                deadline = 1L;
            }
            
            // 申请读锁，超时后失败，未超时则排队
            if((next = acquireRead(true, deadline)) != INTERRUPTED) {
                return next;
            }
        }
        
        throw new InterruptedException();
    }
    
    /**
     * Non-exclusively acquires the lock, blocking if necessary
     * until available or the current thread is interrupted.
     * Behavior under interruption matches that specified
     * for method {@link Lock#lockInterruptibly()}.
     *
     * @return a read stamp that can be used to unlock or convert mode
     *
     * @throws InterruptedException if the current thread is interrupted
     *                              before acquiring the lock
     */
    // 申请读锁，失败后去排队（不可阻塞带有中断标记的线程）
    @ReservedStackAccess
    public long readLockInterruptibly() throws InterruptedException {
        long s, next;
        if(!Thread.interrupted()
            // bypass acquireRead on common uncontended case
            && ((whead == wtail && ((s = state) & ABITS)<RFULL && casState(s, next = s + RUNIT)) || (next = acquireRead(true, 0L)) != INTERRUPTED))
            return next;
        
        throw new InterruptedException();
    }
    
    /**
     * See above for explanation.
     *
     * @param interruptible true if should check interrupts and if so return INTERRUPTED
     * @param deadline      if nonzero, the System.nanoTime value to timeout at (and return zero)
     *
     * @return next state, or INTERRUPTED
     */
    // 申请读锁
    private long acquireRead(boolean interruptible, long deadline) {
        boolean wasInterrupted = false;
        WNode node = null, p;
        
        // 死循环（初始化等待队列，并将当前结点分发到主线/支线）
        for(int spins = -1; ; ) {
            WNode h;
            
            // 每次自旋进来都获取最新的队头和队尾
            h = whead;
            p = wtail;
            
            // 如果当前主线没有排队的线程
            if(h==p) {
                // 小自旋
                for(long m, s, ns; ; ) {
                    // 获取锁的状态
                    m = (s = state) & ABITS;
                    ns = 0;
                    
                    boolean boo = false;
                    
                    // 如果当前不存在写锁，且读锁未溢出
                    if(m<RFULL){
                        // 允许申请锁，读锁计数增一
                        ns = s + RUNIT;
                        boo = casState(s, ns);
                    } else {
                        // 如果当前不存在写锁，但是读锁处于溢出边缘
                        if(m<WBIT){
                            // 尝试增加溢出标记readerOverflow
                            ns = tryIncReaderOverflow(s);
                            boo = ns!=0L;
                        }// if(m<WBIT)
                    } // if(m<RFULL)
                    
                    // 即使的溢出，这里依然可以申请到锁
                    if(boo) {
                        if(wasInterrupted) {
                            Thread.currentThread().interrupt();
                        }
                        return ns;
                    } else {
                        // 如果存在写锁，则进入小自旋，以较低的开销等待锁空闲
                        if(m>= WBIT) {
                            // 如果设定了自旋次数，则递减自旋计数
                            if(spins>0) {
                                --spins;
                                Thread.onSpinWait();
                            } else {
                                // 自旋结束
                                if(spins == 0) {
                                    // 获取最新的队头和队尾信息
                                    WNode nh = whead, np = wtail;
                                    
                                    // 等待队列与自旋之前的状态一样
                                    if(nh == h && np == p) {
                                        // 跳出自旋去排队
                                        break;
                                    }
                                    
                                    // 如果队头或队尾发生了变化，更新h和p的指向
                                    h = nh;
                                    p = np;
                                    
                                    // 如果队头不等于队尾，说明此时存在其他排队线程，直接跳出自旋准备去排队
                                    if(h != p) {
                                        break;
                                    }
                                }
                                
                                // 设置小自旋次数为64
                                spins = SPINS;
                            } // if(spins>0)
                        } // if(m>=WBIT)
                    } // if(boo)
                } // 小自旋
            } // if(h==p)
            
            /*
             * 至此，第一阶段的抢锁过程失败了，失败原因可能是：
             * 1.存在其他排队线程
             * 2.没有排队线程，但存在迟迟不被释放的写锁
             */
            
            // 如果当前还没有等待队列
            if(p == null) {
                // 初始化等待队列队头
                WNode hd = new WNode(WMODE, null);
                if(WHEAD.weakCompareAndSet(this, null, hd)) {
                    // 队尾和队头指向同一个结点
                    wtail = hd;
                }
                
                // 如果存在等待队列，但是还没有属于当前线程的排队结点，则新建一个
            } else if(node == null) {
                node = new WNode(RMODE, p);
                
                // 如果当前主线没有排队的结点，或者队尾是【写锁线程】在排队
            } else if(h == p || p.mode != RMODE) {
                // 如果队尾发生了变化，则需要更新队尾
                if(node.prev != p) {
                    node.prev = p;
                    
                    // 原子地更新队尾为node
                } else if(WTAIL.weakCompareAndSet(this, p, node)) {
                    p.next = node;
                    
                    // 如果当前结点成功地进入等待队列，则跳出小自旋
                    break;
                }
                
                // 如果当前已经存在排队的节点，且队尾是【读锁线程】在排队，此时要初始化支线了（头插法）
            } else if(!WCOWAIT.compareAndSet(p, node.cowait = p.cowait, node)) {
                node.cowait = null;
            } else {
                // 上面支线初始化成功后，直接到了这里，进入支线死循环
                for(; ; ) {
                    WNode pp, c;
                    Thread w;
                    
                    // 如果头结点上有支线线程，这里帮忙唤醒一下
                    if((h = whead) != null
                        && (c = h.cowait) != null
                        && WCOWAIT.compareAndSet(h, c, c.cowait)
                        && (w = c.thread) != null) {
                        LockSupport.unpark(w);
                    }
                    
                    if(Thread.interrupted()) {
                        if(interruptible) {
                            return cancelWaiter(node, p, true);
                        }
                        wasInterrupted = true;
                    }
                    
                    // 如果支线上首个读锁线程已经排在主线队首，或者已经在执行
                    if(h == (pp = p.prev) || h == p || pp == null) {
                        long m, s, ns;
                        do {
                            if((m = (s = state) & ABITS)<RFULL  // 恰好当前的锁状态为读锁，则允许申请读锁
                                ? casState(s, ns = s + RUNIT)
                                : (m<WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) {
                                if(wasInterrupted) {
                                    Thread.currentThread().interrupt();
                                }
                                // 读锁申请成功，不需要去阻塞了
                                return ns;
                            }
                        } while(m<WBIT);
                    }
                    
                    if(whead == h && p.prev == pp) {
                        long time;
                        
                        // 支线上首个读锁线程正在执行
                        if(pp == null || h == p || p.status>0) {
                            node = null; // throw away
                            break;
                        }
                        
                        if(deadline == 0L) {
                            time = 0L;
                        } else if((time = deadline - System.nanoTime())<=0L) {
                            if(wasInterrupted) {
                                Thread.currentThread().interrupt();
                            }
                            return cancelWaiter(node, p, false);
                        }
                        
                        node.thread = Thread.currentThread();
                        // 还没执行到支线上，或者当前运行的是写锁
                        if((h != pp || (state & ABITS) == WBIT) && whead == h && p.prev == pp) {
                            if(time == 0L) {
                                // 陷入阻塞，醒来后在小自旋中继续活动
                                LockSupport.park(this);
                            } else {
                                LockSupport.parkNanos(this, time);
                            }
                        }
                        node.thread = null;
                    }
                } // 支线死循环
            }
        } // 死循环
        
        // 死循环（处理等待队列的主线）
        for(int spins = -1; ; ) {
            WNode h, np, pp;
            int ps;
            
            // 如果当前主线没有排队的结点
            if((h = whead) == p) {
                if(spins<0) {
                    // 预设自旋1024次
                    spins = HEAD_SPINS;
                } else if(spins<MAX_HEAD_SPINS) {
                    // 前一次大自旋没成功的话，这里自旋次数翻倍
                    spins <<= 1;
                }
                
                // 进入大自旋，大自旋时，k在递减，而spins不变
                for(int k = spins; ; ) { // spin at head
                    long m, s, ns;
                    if((m = (s = state) & ABITS)<RFULL  // 如果当前锁状态变为读锁，则更新读锁计数
                        ? casState(s, ns = s + RUNIT)
                        : (m<WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) {
                        WNode c;
                        Thread w;
                        // 读锁申请成功，更新头结点（很重要的一步）
                        whead = node;
                        node.prev = null;
                        // 唤醒该结点支线上所有申请读锁的线程
                        while((c = node.cowait) != null) {
                            if(WCOWAIT.compareAndSet(node, c, c.cowait) && (w = c.thread) != null) {
                                LockSupport.unpark(w);
                            }
                        }
                        if(wasInterrupted) {
                            Thread.currentThread().interrupt();
                        }
                        return ns;
                    } else if(m >= WBIT && --k<=0) {
                        // 如果当前锁状态依旧是写锁，则继续自旋，直到自旋条件不成立时才退出
                        break;
                    } else {
                        Thread.onSpinWait();
                    }
                }// 大自旋
            } else if(h != null) {
                WNode c;
                Thread w;
                while((c = h.cowait) != null) {
                    if(WCOWAIT.compareAndSet(h, c, c.cowait) && (w = c.thread) != null) {
                        LockSupport.unpark(w);
                    }
                }
            }
            
            // 主线上仍有排队线程，或当前锁状态是写锁
            if(whead == h) {
                if((np = node.prev) != p) {
                    if(np != null) {
                        (p = np).next = node;   // stale
                    }
                } else if((ps = p.status) == 0) {
                    WSTATUS.compareAndSet(p, 0, WAITING);
                } else if(ps == CANCELLED) {
                    if((pp = p.prev) != null) {
                        node.prev = pp;
                        pp.next = node;
                    }
                } else {
                    long time;
                    if(deadline == 0L) {
                        time = 0L;
                    } else if((time = deadline - System.nanoTime())<=0L) {
                        return cancelWaiter(node, node, false);
                    }
                    
                    node.thread = Thread.currentThread();
                    
                    if(p.status<0 && (p != h || (state & ABITS) == WBIT) && whead == h && node.prev == p) {
                        if(time == 0L) {
                            // 陷入阻塞，醒来后在大自旋中继续活动
                            LockSupport.park(this);
                        } else {
                            LockSupport.parkNanos(this, time);
                        }
                    }
                    
                    node.thread = null;
                    
                    if(Thread.interrupted()) {
                        if(interruptible) {
                            return cancelWaiter(node, node, true);
                        }
                        wasInterrupted = true;
                    }
                }
            } // if(whead == h)
        } // 死循环
    }
    
    /**
     * Tries to increment readerOverflow by first setting state access bits value to RBITS,
     * indicating hold of spinlock, then updating, then releasing.
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     *
     * @return new stamp on success, else zero
     */
    // 尝试增加溢出标记readerOverflow
    private long tryIncReaderOverflow(long s) {
        assert (s & ABITS) >= RFULL;
        
        if((s & ABITS) == RFULL) {
            // 将state从0111 1110更新为0111 1111
            if(casState(s, s | RBITS)) {
                // 记录读锁溢出的数量
                ++readerOverflow;
                // 将state从0111 1111更新回0111 1110
                STATE.setVolatile(this, s);
                // 返回s
                return s;
            }
        } else if((LockSupport.nextSecondarySeed() & OVERFLOW_YIELD_RATE) == 0) {
            Thread.yield();
        } else {
            Thread.onSpinWait();
        }
        
        return 0L;
    }
    
    /**
     * Tries to decrement readerOverflow.
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     *
     * @return new stamp on success, else zero
     */
    // 尝试减少溢出标记readerOverflow
    private long tryDecReaderOverflow(long s) {
        assert (s & ABITS) >= RFULL;
        
        // 判断给定的读锁标记是否处于溢出边界上
        if((s & ABITS) == RFULL) {
            // 增加一道读锁，表示当前正在处理移除
            if(casState(s, s | RBITS)) {
                int r;
                long next;
                
                // 已经溢出了，递减计数器
                if((r = readerOverflow)>0) {
                    readerOverflow = r - 1;
                    next = s;
                } else {
                    // 刚好没溢出
                    next = s - RUNIT;
                }
                
                // 把上面增加的读锁标记抹掉，表示溢出处理完了
                STATE.setVolatile(this, next);
                
                return next;
            }
        } else if((LockSupport.nextSecondarySeed() & OVERFLOW_YIELD_RATE) == 0) {
            Thread.yield();
        } else {
            Thread.onSpinWait();
        }
        
        return 0L;
    }
    
    /*▲ 申请读锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 申请写锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Exclusively acquires the lock, blocking if necessary
     * until available.
     *
     * @return a write stamp that can be used to unlock or convert mode
     */
    // 申请写锁，失败后去排队
    @ReservedStackAccess
    public long writeLock() {
        // 快速申请写锁，如果此时已经存在其他锁，则申请失败（只尝试一次，失败了也不阻塞）
        long next= tryWriteLock();
        
        // 如果快速通道失败
        if(next == 0L) {
            // 已存在锁（可能是读锁，也可能是写锁）的情形下申请写锁
            return acquireWrite(false, 0L);
        }
        
        return next;
    }
    
    /**
     * Exclusively acquires the lock if it is immediately available.
     *
     * @return a write stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    // 快速申请写锁，如果此时锁被占用，则申请失败（只尝试一次，失败了也不阻塞）
    @ReservedStackAccess
    public long tryWriteLock() {
        long s = state;
        
        // 如果没有读/写锁
        if((s & ABITS) == 0L) {
            // 走此快速通道申请写锁
            return tryWriteLock(s);
        }
        
        return 0L;
    }
    
    /**
     * Exclusively acquires the lock if it is available within the
     * given time and the current thread has not been interrupted.
     * Behavior under timeout and interruption matches that specified
     * for method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     *
     * @return a write stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     *
     * @throws InterruptedException if the current thread is interrupted
     *                              before acquiring the lock
     */
    // 申请写锁，超时后失败，未超时则排队（不可阻塞带有中断标记的线程）
    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(time);
        if(!Thread.interrupted()) {
            long next, deadline;
            if((next = tryWriteLock()) != 0L) {
                return next;
            }
            if(nanos<=0L) {
                return 0L;
            }
            if((deadline = System.nanoTime() + nanos) == 0L) {
                deadline = 1L;
            }
            if((next = acquireWrite(true, deadline)) != INTERRUPTED) {
                return next;
            }
        }
        throw new InterruptedException();
    }
    
    /**
     * Exclusively acquires the lock, blocking if necessary
     * until available or the current thread is interrupted.
     * Behavior under interruption matches that specified
     * for method {@link Lock#lockInterruptibly()}.
     *
     * @return a write stamp that can be used to unlock or convert mode
     *
     * @throws InterruptedException if the current thread is interrupted
     *                              before acquiring the lock
     */
    // 申请写锁，失败后去排队（不可阻塞带有中断标记的线程）
    @ReservedStackAccess
    public long writeLockInterruptibly() throws InterruptedException {
        long next;
        if(!Thread.interrupted() && (next = acquireWrite(true, 0L)) != INTERRUPTED) {
            return next;
        }
        throw new InterruptedException();
    }
    
    // 在没有读/写锁的时候，通过此快速通道申请写锁
    private long tryWriteLock(long s) {
        assert (s & ABITS) == 0L;
        
        // 设置写锁标记
        long next = s | WBIT;
        
        // 更新state
        if(casState(s, next)) {
            VarHandle.storeStoreFence();
            return next;
        }
        
        return 0L;
    }
    
    /**
     * See above for explanation.
     *
     * @param interruptible true if should check interrupts and if so
     *                      return INTERRUPTED
     * @param deadline      if nonzero, the System.nanoTime value to timeout
     *                      at (and return zero)
     *
     * @return next state, or INTERRUPTED
     */
    // 已存在锁的情形下，申请写锁
    private long acquireWrite(boolean interruptible, long deadline) {
        WNode node = null, p;
        
        // 小自旋，主要用于创建等待队列
        for(int spins=-1; ; ) {
            long m, s, ns;
            
            // 查看锁的状态
            m = (s = state) & ABITS;
            
            // 如果当前没有读/写锁
            if(m==0L) {
                // 尝试走快速通道
                if((ns = tryWriteLock(s)) != 0L) {
                    return ns;
                }
                
                // 有读/写锁的情形下，初始化自旋次数
            } else if(spins<0) {
                // 如果存在写锁，且没有排队线程
                if(m==WBIT && wtail==whead){
                    spins = SPINS;  // 自旋64次，如果还不行，就去排队
                } else {
                    // 如果存在读锁，或者有排队的线程，不自旋，直接去排队
                    spins = 0;
                }
                
                // 开始自旋，自旋计数器每次减一
            } else if(spins>0) {
                --spins;    // 计数器递减
                Thread.onSpinWait();
                
                // 初始化等待队列（p每次进来都要指向新的wtail，这很重要，因为wtail可能被别的线程修改）
            } else if((p = wtail) == null) {
                // 初始化队头
                WNode hd = new WNode(WMODE, null);
                if(WHEAD.weakCompareAndSet(this, null, hd)) {
                    // 队尾和队头指向同一个结点
                    wtail = hd;
                }
                
                // 如果当前线程还没有参与排队的结点，就新建一个
            } else if(node == null) {
                node = new WNode(WMODE, p);
                
                // 如果wtail发生改变，意味着p也会跟着改变，此时需要更新node.prev
            } else if(node.prev != p) {
                node.prev = p;
                
                // 原子地更新队尾为node
            } else if(WTAIL.weakCompareAndSet(this, p, node)) {
                p.next = node;
                break;
            }
        }
        
        boolean wasInterrupted = false;
        
        // 等待队列已就绪，接下来该设置其参数了
        for(int spins = -1; ; ) {
            WNode h, np, pp;
            int ps;
            
            h = whead;
            
            // 如果当前线程排在队首，则继续尝试自旋
            if(h==p) {
                if(spins<0) {
                    // 预设自旋1024次
                    spins = HEAD_SPINS;
                } else if(spins<MAX_HEAD_SPINS) {
                    // 前一次大自旋没成功的话，这里自旋次数翻倍
                    spins <<= 1;
                }
                
                // 大自旋，与小自旋不同，这里是k在递减，spins在递减过程中不变
                for(int k = spins; k>0; --k) { // spin at head
                    long m, s, ns;
                    
                    // 获取锁的状态
                    m = (s = state) & ABITS;
                    
                    // 如果当前没有读/写锁
                    if(m==0L) {
                        // 走快速通道申请写锁
                        if((ns = tryWriteLock(s)) != 0L) {
                            // 更新头结点（很重要的一步）
                            whead = node;
                            node.prev = null;
                            if(wasInterrupted) {
                                Thread.currentThread().interrupt();
                            }
                            return ns;
                        }
                    } else {
                        Thread.onSpinWait();
                    }
                } // 大自旋
            } else {
                // help release stale waiters
                if(h != null) {
                    WNode c;
                    Thread w;
                    while((c = h.cowait) != null) {
                        if(WCOWAIT.weakCompareAndSet(h, c, c.cowait) && (w = c.thread) != null) {
                            LockSupport.unpark(w);
                        }
                    }
                }
            } // if(h==p)
            
            // 如果当前线程没有排在队首，或者大自旋失败了
            if(whead == h) {
                if((np = node.prev) != p) {
                    if(np != null) {
                        (p = np).next = node;   // stale
                    }
                } else if((ps = p.status) == 0) {
                    // 更新当前线程的前驱为等待状态（当前线程的状态需要保存到前驱中）
                    WSTATUS.compareAndSet(p, 0, WAITING);
                } else if(ps == CANCELLED) {
                    if((pp = p.prev) != null) {
                        node.prev = pp;
                        pp.next = node;
                    }
                } else {
                    long time; // 0 argument to park means no timeout
                    
                    // 未设置超时
                    if(deadline == 0L) {
                        time = 0L;
                    } else {
                        // 发生了超时，需要取消该线程的等待
                        if((time = deadline - System.nanoTime())<=0L) {
                            return cancelWaiter(node, node, false);
                        }
                    }
                    
                    // 当前线程需要等待，将其线程引用存入当前的结点
                    node.thread = Thread.currentThread();
                    
                    // 前驱处于等待状态
                    if(p.status<0) {
                        // 当前线程没有排在队首，或者当前有其他锁存在
                        if(p != h || (state & ABITS) != 0L){
                            // 队头没被改变
                            if(whead == h){
                                if(node.prev == p){
                                    if(time == 0L) {
                                        // 阻塞
                                        LockSupport.park(this);
                                    } else {
                                        // 带着超时去阻塞
                                        LockSupport.parkNanos(this, time);
                                    }
                                }
                            }
                        }
                    }
                    
                    // 清除node内的线程引用
                    node.thread = null;
                    
                    if(Thread.interrupted()) {
                        if(interruptible) {
                            return cancelWaiter(node, node, true);
                        }
                        wasInterrupted = true;
                    }
                }
            } // if(whead == h)
        } // for(int spins = -1; ; )
    }
    
    /*▲ 申请写锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 释放锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * If the lock state matches the given stamp, releases the
     * non-exclusive lock.
     *
     * @param stamp a stamp returned by a read-lock operation
     *
     * @throws IllegalMonitorStateException if the stamp does
     *                                      not match the current state of this lock
     */
    // 释放读锁，如果读锁都释放完了，唤醒排队的线程
    @ReservedStackAccess
    public void unlockRead(long stamp) {
        long s, m;
        WNode h;
        while(((s = state) & SBITS) == (stamp & SBITS) && (stamp & RBITS)>0L && ((m = s & RBITS)>0L)) {
            if(m<RFULL) {
                // 锁未溢出，递减锁状态的计数
                if(casState(s, s - RUNIT)) {
                    // 读锁释放完的时候，同步队列中存在排队的线程
                    if(m == RUNIT && (h = whead) != null && h.status != 0) {
                        release(h);
                    }
                    return;
                }
                
                // 尝试减少溢出标记readerOverflow
            } else if(tryDecReaderOverflow(s) != 0L) {
                return;
            }
        }
        throw new IllegalMonitorStateException();
    }
    
    /**
     * If the lock state matches the given stamp, releases the
     * exclusive lock.
     *
     * @param stamp a stamp returned by a write-lock operation
     *
     * @throws IllegalMonitorStateException if the stamp does
     *                                      not match the current state of this lock
     */
    // 释放写锁，并唤醒排队的线程
    @ReservedStackAccess
    public void unlockWrite(long stamp) {
        if(state != stamp || (stamp & WBIT) == 0L) {
            throw new IllegalMonitorStateException();
        }
        unlockWriteInternal(stamp);
    }
    
    /**
     * If the lock state matches the given stamp, releases the
     * corresponding mode of the lock.
     *
     * @param stamp a stamp returned by a lock operation
     *
     * @throws IllegalMonitorStateException if the stamp does
     *                                      not match the current state of this lock
     */
    // 释放锁，根据stamp来区分释放读锁还是释放写锁
    @ReservedStackAccess
    public void unlock(long stamp) {
        if((stamp & WBIT) != 0L) {
            unlockWrite(stamp);
        } else {
            unlockRead(stamp);
        }
    }
    
    /**
     * Releases one hold of the read lock if it is held, without
     * requiring a stamp value. This method may be useful for recovery
     * after errors.
     *
     * @return {@code true} if the read lock was held, else false
     */
    // 释放读锁，不需要传入stamp标记
    @ReservedStackAccess
    public boolean tryUnlockRead() {
        long s, m;
        WNode h;
        while((m = (s = state) & ABITS) != 0L && m<WBIT) {
            if(m<RFULL) {
                if(casState(s, s - RUNIT)) {
                    if(m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return true;
                }
            } else if(tryDecReaderOverflow(s) != 0L)
                return true;
        }
        return false;
    }
    
    // 释放读锁，不需要传入stamp标记
    final void unstampedUnlockRead() {
        long s, m;
        WNode h;
        while((m = (s = state) & RBITS)>0L) {
            if(m<RFULL) {
                if(casState(s, s - RUNIT)) {
                    if(m == RUNIT && (h = whead) != null && h.status != 0) {
                        release(h);
                    }
                    return;
                }
            } else if(tryDecReaderOverflow(s) != 0L) {
                return;
            }
        }
        throw new IllegalMonitorStateException();
    }
    
    /**
     * Releases the write lock if it is held, without requiring a
     * stamp value. This method may be useful for recovery after
     * errors.
     *
     * @return {@code true} if the lock was held, else false
     */
    // 释放写锁，不需要传入stamp标记
    @ReservedStackAccess
    public boolean tryUnlockWrite() {
        long s;
        if(((s = state) & WBIT) != 0L) {
            unlockWriteInternal(s);
            return true;
        }
        return false;
    }
    
    // 释放写锁，不需要传入stamp标记
    final void unstampedUnlockWrite() {
        long s;
        if(((s = state) & WBIT) == 0L) {
            throw new IllegalMonitorStateException();
        }
        unlockWriteInternal(s);
    }
    
    /**
     * Returns an unlocked state, incrementing the version and avoiding special failure value 0L.
     *
     * @param s a write-locked state (or stamp)
     */
    // 如果s是写锁，返回ORIGIN，否则返回s
    private static long unlockWriteState(long s) {
        
        // 如果s是写锁，返回ORIGIN
        if((s += WBIT) == 0L) {
            return ORIGIN;
        }
        
        return s;
    }
    
    // 清除当前锁状态中保存的写锁标记（如果s是写锁的话），唤醒排队的线程
    private long unlockWriteInternal(long s) {
        long next;
        WNode h;
        
        // 如果s是写锁，就将当前锁的状态设置为ORIGIN，否则，维持原样
        STATE.setVolatile(this, next = unlockWriteState(s));
        
        // 如果同步队列中存在排队的线程
        if((h = whead) != null && h.status != 0) {
            // 更新h的status为0，并唤醒h后面阻塞的线程
            release(h);
        }
        
        return next;
    }
    
    /**
     * Wakes up the successor of h (normally whead). This is normally
     * just h.next, but may require traversal from wtail if next
     * pointers are lagging. This may fail to wake up an acquiring
     * thread when one or more have been cancelled, but the cancel
     * methods themselves provide extra safeguards to ensure liveness.
     */
    // 更新h的status为0，并唤醒h后面阻塞的线程
    private void release(WNode h) {
        if(h != null) {
            WNode q;
            Thread w;
            
            // 更新h中的status为0
            WSTATUS.compareAndSet(h, WAITING, 0);
            
            // 查找h后面离h最近的等待唤醒的线程（排除标记为取消的线程）
            if((q = h.next) == null || q.status == CANCELLED) {
                for(WNode t = wtail; t != null && t != h; t = t.prev) {
                    if(t.status<=0) {
                        q = t;
                    }
                }
            }
            
            // 唤醒h后面阻塞的线程
            if(q != null && (w = q.thread) != null) {
                LockSupport.unpark(w);
            }
        }
    }
    
    /*▲ 释放锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 锁转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * If the lock state matches the given stamp, atomically performs one of the following actions.
     * If the stamp represents holding a write lock, releases it and obtains a read lock.
     * Or, if a read lock, returns it.
     * Or, if an optimistic read, acquires a read lock and returns a read stamp only if immediately available.
     * This method returns zero in all other cases.
     *
     * @param stamp a stamp
     *
     * @return a valid read stamp, or zero on failure
     */
    /*
     * 转换当前锁为读锁，参数stamp是某个锁创建时的状态
     *
     * 写锁 --> 读锁：写锁转为一个读锁，并唤醒首个排队线程
     * 读锁 --> 读锁：无效果，维持当前锁状态
     * 乐观读锁-->读锁：读锁计数增一
     */
    public long tryConvertToReadLock(long stamp) {
        long a, s, next;
        WNode h;
        
        // state与stamp的模式要匹配（类型一致）
        while(((s = state) & SBITS) == (stamp & SBITS)) {
            // stamp是写锁标记
            if((a = stamp & ABITS) >= WBIT) {
                // 写锁标记是唯一的，所以不可能出现两个写锁标记不一致的情形
                if(s != stamp) {
                    break;
                }
                
                // 将当前锁的状态设置为读锁：1 0000 0001（代表一个读锁）
                STATE.setVolatile(this, next = unlockWriteState(s) + RUNIT);
                
                // 如果存在阻塞的线程
                if((h = whead) != null && h.status != 0) {
                    // 更新h的status为0，并唤醒h后面阻塞的线程
                    release(h);
                }
                return next;
                
                // stamp是乐观读锁标记
            } else if(a == 0L) {
                // 当前读锁未溢出
                if((s & ABITS)<RFULL) {
                    if(casState(s, next = s + RUNIT)) {
                        return next;
                    }
                    
                    // 当前读锁是溢出的
                } else if((next = tryIncReaderOverflow(s)) != 0L) {
                    return next;
                }
                
                // stamp是读锁标记
            } else {
                if((s & ABITS) == 0L) {
                    break;
                }
                return stamp;
            }
        }
        
        return 0L;
    }
    
    /**
     * If the lock state matches the given stamp then, atomically,
     * if the stamp represents holding a lock,
     * releases it and returns an observation stamp.
     * Or, if an optimistic read, returns it if validated.
     * This method returns zero in all other cases,
     * and so may be useful as a form of "tryUnlock".
     *
     * @param stamp a stamp
     *
     * @return a valid optimistic read stamp, or zero on failure
     */
    /*
     * 转换当前锁为乐观读锁，参数stamp是某个锁创建时的状态
     *
     * 写锁 --> 乐观读锁：写锁转为一个乐观读锁，并唤醒首个排队线程
     * 读锁 --> 乐观读锁：读锁计数减一，如果读锁已经不存在了，则唤醒首个排队线程
     * 乐观读锁-->乐观读锁：无效果
     */
    public long tryConvertToOptimisticRead(long stamp) {
        long a, m, s, next;
        WNode h;
        VarHandle.acquireFence();
        
        // state与stamp的模式要匹配（类型一致）
        while(((s = state) & SBITS) == (stamp & SBITS)) {
            // stamp是写锁标记
            if((a = stamp & ABITS) >= WBIT) {
                // 写锁标记是唯一的，所以不可能出现两个写锁标记不一致的情形
                if(s != stamp) {
                    break;
                }
                
                // 清除当前锁状态中保存的写锁标记（如果s是写锁的话），唤醒排队的线程
                return unlockWriteInternal(s);
                
                // stamp是乐观读锁
            } else if(a == 0L) {
                return stamp;
                
                // 当前的锁标记state无效
            } else if((m = s & ABITS) == 0L) {
                break;
                
                // stamp是读锁，state也是有效的读锁（未溢出）
            } else if(m<RFULL) {
                // 读锁计数减一
                if(casState(s, next = s - RUNIT)) {
                    // m == RUNIT说明之前只剩一个读锁了，刚才递减后，现在没有任何读锁了，此时需要唤醒首个排队的线程
                    if(m == RUNIT && (h = whead) != null && h.status != 0) {
                        // 更新h的status为0，并唤醒h后面阻塞的线程
                        release(h);
                    }
                    
                    // 返回乐观锁标记
                    return next & SBITS;
                }
                
                // stamp是读锁，state也是有效的读锁（溢出了）
            } else if((next = tryDecReaderOverflow(s)) != 0L) {
                // 尝试减少溢出标记readerOverflow后，返回乐观锁标记
                return next & SBITS;
            }
        }
        
        return 0L;
    }
    
    /**
     * If the lock state matches the given stamp,
     * atomically performs one of the following actions.
     * If the stamp represents holding a write lock, returns it.
     * Or, if a read lock, if the write lock is available, releases the read lock and returns a write stamp.
     * Or, if an optimistic read, returns a write stamp only if immediately available.
     * This method returns zero in all other cases.
     *
     * @param stamp a stamp
     *
     * @return a valid write stamp, or zero on failure
     */
    /*
     * 转换当前锁为写锁，参数stamp是某个锁创建时的状态（读锁被多个线程持有时禁止转换为写锁）
     *
     * 写锁 --> 写锁：无效果
     * 读锁 --> 写锁：如果当前锁标记仅存储了一个读锁，则直接转为写锁，如果当前存在多个读锁，则转换失败
     * 乐观读锁-->写锁：直接转换
     */
    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS, m, s, next;
        
        // state与stamp的模式要匹配（类型一致）
        while(((s = state) & SBITS) == (stamp & SBITS)) {
            // state是乐观读锁
            if((m = s & ABITS) == 0L) {
                // 要求stamp是乐观读锁（不能为0）
                if(a != 0L) {
                    break;
                }
                
                // 当前没有读/写锁，直接走快速通道，申请写锁
                if((next = tryWriteLock(s)) != 0L) {
                    return next;
                }
                
                // state是写锁
            } else if(m == WBIT) {
                // 写锁标记是唯一的，所以不可能出现两个写锁标记不一致的情形
                if(a != m) {
                    break;
                }
                
                return stamp;
                
                // state是读锁，且只被一个线程持有，而且stamp也为读锁（或乐观读锁）
            } else if(m == RUNIT && a != 0L) {
                // 更新state为写锁标记
                if(casState(s, next = s - RUNIT + WBIT)) {
                    VarHandle.storeStoreFence();
                    return next;
                }
            } else {
                break;
            }
        }
        
        return 0L;
    }
    
    /*▲ 锁转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 锁状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether a stamp represents a successful optimistic read.
     *
     * @param stamp a stamp returned by a previous StampedLock operation
     *
     * @return {@code true} if the stamp was returned by a successful
     * optimistic read operation, that is, a non-zero return from
     * {@link #tryOptimisticRead()} or
     * {@link #tryConvertToOptimisticRead(long)}
     *
     * @since 10
     */
    // 判断stamp是否为一个成功的乐观读锁标记（例如ORIGIN）
    public static boolean isOptimisticReadStamp(long stamp) {
        return (stamp & ABITS) == 0L && stamp != 0L;
    }
    
    /**
     * Returns {@code true} if the lock is currently held non-exclusively.
     *
     * @return {@code true} if the lock is currently held non-exclusively
     */
    // 判断当前的锁状态state是否为读锁
    public boolean isReadLocked() {
        return (state & RBITS) != 0L;
    }
    
    /**
     * Returns {@code true} if the lock is currently held exclusively.
     *
     * @return {@code true} if the lock is currently held exclusively
     */
    // 判断当前的锁状态state是否为写锁
    public boolean isWriteLocked() {
        return (state & WBIT) != 0L;
    }
    
    /**
     * Queries the number of read locks held for this lock. This
     * method is designed for use in monitoring system state, not for
     * synchronization control.
     *
     * @return the number of read locks held
     */
    // 返回当前的锁状态state中保存的读锁数量
    public int getReadLockCount() {
        return getReadLockCount(state);
    }
    
    /**
     * Tells whether a stamp represents holding a lock non-exclusively.
     * This method may be useful in conjunction with
     * {@link #tryConvertToReadLock}, for example: <pre> {@code
     * long stamp = sl.tryOptimisticRead();
     * try {
     *   ...
     *   stamp = sl.tryConvertToReadLock(stamp);
     *   ...
     * } finally {
     *   if (StampedLock.isReadLockStamp(stamp))
     *     sl.unlockRead(stamp);
     * }}</pre>
     *
     * @param stamp a stamp returned by a previous StampedLock operation
     *
     * @return {@code true} if the stamp was returned by a successful
     * read-lock operation
     *
     * @since 10
     */
    // 判断stamp是否为读锁
    public static boolean isReadLockStamp(long stamp) {
        return (stamp & RBITS) != 0L;
    }
    
    /**
     * Tells whether a stamp represents holding a lock exclusively.
     * This method may be useful in conjunction with
     * {@link #tryConvertToWriteLock}, for example: <pre> {@code
     * long stamp = sl.tryOptimisticRead();
     * try {
     *   ...
     *   stamp = sl.tryConvertToWriteLock(stamp);
     *   ...
     * } finally {
     *   if (StampedLock.isWriteLockStamp(stamp))
     *     sl.unlockWrite(stamp);
     * }}</pre>
     *
     * @param stamp a stamp returned by a previous StampedLock operation
     *
     * @return {@code true} if the stamp was returned by a successful
     * write-lock operation
     *
     * @since 10
     */
    // 判断stamp是否为写锁
    public static boolean isWriteLockStamp(long stamp) {
        return (stamp & ABITS) == WBIT;
    }
    
    /**
     * Tells whether a stamp represents holding a lock.
     * This method may be useful in conjunction with
     * {@link #tryConvertToReadLock} and {@link #tryConvertToWriteLock},
     * for example: <pre> {@code
     * long stamp = sl.tryOptimisticRead();
     * try {
     *   ...
     *   stamp = sl.tryConvertToReadLock(stamp);
     *   ...
     *   stamp = sl.tryConvertToWriteLock(stamp);
     *   ...
     * } finally {
     *   if (StampedLock.isLockStamp(stamp))
     *     sl.unlock(stamp);
     * }}</pre>
     *
     * @param stamp a stamp returned by a previous StampedLock operation
     *
     * @return {@code true} if the stamp was returned by a successful
     * read-lock or write-lock operation
     *
     * @since 10
     */
    // 判断stamp是否保存了读锁或写锁
    public static boolean isLockStamp(long stamp) {
        return (stamp & ABITS) != 0L;
    }
    
    /**
     * Returns combined state-held and overflow read count for given
     * state s.
     */
    // 返回stamp中保存的读锁数量
    private int getReadLockCount(long stamp) {
        long readers;
        if((readers = stamp & RBITS) >= RFULL)
            readers = RFULL + readerOverflow;
        return (int) readers;
    }
    
    /*▲ 锁状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * If node non-null, forces cancel status and unsplices it from
     * queue if possible and wakes up any cowaiters (of the node, or
     * group, as applicable), and in any case helps release current
     * first waiter if lock is free. (Calling with null arguments
     * serves as a conditional form of release, which is not currently
     * needed but may be needed under possible future cancellation
     * policies). This is a variant of cancellation methods in
     * AbstractQueuedSynchronizer (see its detailed explanation in AQS
     * internal documentation).
     *
     * @param node        if non-null, the waiter
     * @param group       either node or the group node is cowaiting with
     * @param interrupted if already interrupted
     *
     * @return INTERRUPTED if interrupted or Thread.interrupted, else zero
     */
    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        if(node != null && group != null) {
            Thread w;
            node.status = CANCELLED;
            // unsplice cancelled nodes from group
            for(WNode p = group, q; (q = p.cowait) != null; ) {
                if(q.status == CANCELLED) {
                    WCOWAIT.compareAndSet(p, q, q.cowait);
                    p = group; // restart
                } else {
                    p = q;
                }
            }
            if(group == node) {
                for(WNode r = group.cowait; r != null; r = r.cowait) {
                    if((w = r.thread) != null) {
                        LockSupport.unpark(w); // wake up uncancelled co-waiters
                    }
                }
                for(WNode pred = node.prev; pred != null; ) { // unsplice
                    WNode succ, pp;        // find valid successor
                    while((succ = node.next) == null || succ.status == CANCELLED) {
                        WNode q = null;    // find successor the slow way
                        for(WNode t = wtail; t != null && t != node; t = t.prev) {
                            if(t.status != CANCELLED) {
                                q = t;     // don't link if succ cancelled
                            }
                        }
                        if(succ == q ||   // ensure accurate successor
                            WNEXT.compareAndSet(node, succ, succ = q)) {
                            if(succ == null && node == wtail) {
                                WTAIL.compareAndSet(this, node, pred);
                            }
                            break;
                        }
                    }
                    if(pred.next == node) { // unsplice pred link
                        WNEXT.compareAndSet(pred, node, succ);
                    }
                    if(succ != null && (w = succ.thread) != null) {
                        // wake up succ to observe new pred
                        succ.thread = null;
                        LockSupport.unpark(w);
                    }
                    if(pred.status != CANCELLED || (pp = pred.prev) == null) {
                        break;
                    }
                    node.prev = pp;        // repeat if new pred wrong/cancelled
                    WNEXT.compareAndSet(pp, pred, succ);
                    pred = pp;
                }
            }
        }
        WNode h; // Possibly release first waiter
        while((h = whead) != null) {
            long s;
            WNode q; // similar to release() but check eligibility
            if((q = h.next) == null || q.status == CANCELLED) {
                for(WNode t = wtail; t != null && t != h; t = t.prev) {
                    if(t.status<=0) {
                        q = t;
                    }
                }
            }
            if(h == whead) {
                if(q != null && h.status == 0 && ((s = state) & ABITS) != WBIT && // waiter is eligible
                    (s == 0L || q.mode == RMODE)) {
                    release(h);
                }
                break;
            }
        }
        return (interrupted || Thread.interrupted()) ? INTERRUPTED : 0L;
    }
    
    /**
     * Returns a stamp that can later be validated, or zero if exclusively locked.
     *
     * @return a valid optimistic read stamp, or zero if exclusively locked
     */
    // 如果当前锁不是读/写锁，则可以返回一个非零的乐观读锁标记
    public long tryOptimisticRead() {
        long s;
        
        // 如果当前没有写锁
        if(((s=state) & WBIT) == 0L) {
            // 返回ORIGIN值或0
            return s & SBITS;
        }
        
        // 如果存在写锁，直接返回0
        return 0L;
    }
    
    /**
     * Returns true if the lock has not been exclusively acquired
     * since issuance of the given stamp. Always returns false if the
     * stamp is zero. Always returns true if the stamp represents a
     * currently held lock. Invoking this method with a value not
     * obtained from {@link #tryOptimisticRead} or a locking method
     * for this lock has no defined effect or result.
     *
     * @param stamp a stamp
     *
     * @return {@code true} if the lock has not been exclusively acquired
     * since issuance of the given stamp; else false
     */
    // 判断stamp与当前锁状态state中保存的锁类型是否一致
    public boolean validate(long stamp) {
        VarHandle.acquireFence();
        return (stamp & SBITS) == (state & SBITS);
    }
    
    // 原子地更新state为newValue
    private boolean casState(long expectedValue, long newValue) {
        return STATE.compareAndSet(this, expectedValue, newValue);
    }
    
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        STATE.setVolatile(this, ORIGIN); // reset to unlocked state
    }
    
    /**
     * Returns a string identifying this lock, as well as its lock
     * state.  The state, in brackets, includes the String {@code
     * "Unlocked"} or the String {@code "Write-locked"} or the String
     * {@code "Read-locks:"} followed by the current number of
     * read-locks held.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        long s = state;
        return super.toString() + ((s & ABITS) == 0L ? "[Unlocked]" : (s & WBIT) != 0L ? "[Write-locked]" : "[Read-locks:" + getReadLockCount(s) + "]");
    }
    
    
    
    // 读锁
    final class ReadLockView implements Lock {
        // 申请读锁，失败后去排队（可阻塞带有中断标记的线程）
        public void lock() {
            readLock();
        }
        
        // 申请读锁，失败后去排队（不可阻塞带有中断标记的线程）
        public void lockInterruptibly() throws InterruptedException {
            readLockInterruptibly();
        }
        
        // 申请读锁，如果存在写锁则申请失败，失败后不阻塞，也不再尝试
        public boolean tryLock() {
            return tryReadLock() != 0L;
        }
        
        // 申请读锁，超时后失败，未超时则排队（不可阻塞带有中断标记的线程）
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return tryReadLock(time, unit) != 0L;
        }
        
        // 释放读锁，不需要传入stamp标记
        public void unlock() {
            unstampedUnlockRead();
        }
        
        // 不支持条件对象
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
    
    // 写锁
    final class WriteLockView implements Lock {
        // 申请写锁，失败后去排队（可阻塞带有中断标记的线程）
        public void lock() {
            writeLock();
        }
        
        // 申请写锁，失败后去排队（不可阻塞带有中断标记的线程）
        public void lockInterruptibly() throws InterruptedException {
            writeLockInterruptibly();
        }
        
        // 申请写锁，如果存在锁被占用则申请失败，失败后不阻塞，也不再尝试
        public boolean tryLock() {
            return tryWriteLock() != 0L;
        }
        
        // 申请写锁，超时后失败，未超时则排队（不可阻塞带有中断标记的线程）
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return tryWriteLock(time, unit) != 0L;
        }
        
        // 释放写锁，不需要传入stamp标记
        public void unlock() {
            unstampedUnlockWrite();
        }
        
        // 不支持条件对象
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
    
    // 读/写锁
    final class ReadWriteLockView implements ReadWriteLock {
        // 获取"读"锁
        public Lock readLock() {
            return asReadLock();
        }
        
        // 获取"写"锁
        public Lock writeLock() {
            return asWriteLock();
        }
    }
    
    /** Wait nodes */
    // 等待队列结点，存储被阻塞的线程
    static final class WNode {
        // 标记被阻塞的线程是在申请读锁还是申请写锁，每个结点的mode存储在它的前驱中
        final int mode;           // RMODE or WMODE
        
        // 标记被阻塞的线程是等待还是取消，每个结点的status存储在它的前驱中
        volatile int status;      // 0, WAITING, or CANCELLED
        
        volatile WNode prev;    // 等待队列的主线中指向前一个结点
        volatile WNode next;    // 等待队列的主线中指向下一个结点
        
        // 等待队列的支线中的结点，链接等待的读锁线程
        volatile WNode cowait;    // list of linked readers
        
        // 当前被阻塞的线程
        volatile Thread thread;   // non-null while possibly parked
        
        WNode(int m, WNode p) {
            mode = m;
            prev = p;
        }
    }
}
