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

package java.util.concurrent.atomic;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * A package-local class holding common representation and mechanics
 * for classes supporting dynamic striping on 64bit values. The class
 * extends Number so that concrete subclasses must publicly do so.
 */
// 对普通原子类型的升级，采用分段锁缓解了线程争用的开销
@SuppressWarnings("serial")
abstract class Striped64 extends Number {
    /*
     * This class maintains a lazily-initialized table of atomically
     * updated variables, plus an extra "base" field. The table size
     * is a power of two. Indexing uses masked per-thread hash codes.
     * Nearly all declarations in this class are package-private,
     * accessed directly by subclasses.
     *
     * Table entries are of class Cell; a variant of AtomicLong padded
     * (via @Contended) to reduce cache contention. Padding is
     * overkill for most Atomics because they are usually irregularly
     * scattered in memory and thus don't interfere much with each
     * other. But Atomic objects residing in arrays will tend to be
     * placed adjacent to each other, and so will most often share
     * cache lines (with a huge negative performance impact) without
     * this precaution.
     *
     * In part because Cells are relatively large, we avoid creating
     * them until they are needed.  When there is no contention, all
     * updates are made to the base field.  Upon first contention (a
     * failed CAS on base update), the table is initialized to size 2.
     * The table size is doubled upon further contention until
     * reaching the nearest power of two greater than or equal to the
     * number of CPUS. Table slots remain empty (null) until they are
     * needed.
     *
     * A single spinlock ("cellsBusy") is used for initializing and
     * resizing the table, as well as populating slots with new Cells.
     * There is no need for a blocking lock; when the lock is not
     * available, threads try other slots (or the base).  During these
     * retries, there is increased contention and reduced locality,
     * which is still better than alternatives.
     *
     * The Thread probe fields maintained via ThreadLocalRandom serve
     * as per-thread hash codes. We let them remain uninitialized as
     * zero (if they come in this way) until they contend at slot
     * 0. They are then initialized to values that typically do not
     * often conflict with others.  Contention and/or table collisions
     * are indicated by failed CASes when performing an update
     * operation. Upon a collision, if the table size is less than
     * the capacity, it is doubled in size unless some other thread
     * holds the lock. If a hashed slot is empty, and lock is
     * available, a new Cell is created. Otherwise, if the slot
     * exists, a CAS is tried.  Retries proceed by "double hashing",
     * using a secondary hash (Marsaglia XorShift) to try to find a
     * free slot.
     *
     * The table size is capped because, when there are more threads
     * than CPUs, supposing that each thread were bound to a CPU,
     * there would exist a perfect hash function mapping threads to
     * slots that eliminates collisions. When we reach capacity, we
     * search for this mapping by randomly varying the hash codes of
     * colliding threads.  Because search is random, and collisions
     * only become known via CAS failures, convergence can be slow,
     * and because threads are typically not bound to CPUS forever,
     * may not occur at all. However, despite these limitations,
     * observed contention rates are typically low in these cases.
     *
     * It is possible for a Cell to become unused when threads that
     * once hashed to it terminate, as well as in the case where
     * doubling the table causes no thread to hash to it under
     * expanded mask.  We do not try to detect or remove such cells,
     * under the assumption that for long-running instances, observed
     * contention levels will recur, so the cells will eventually be
     * needed again; and for short-lived ones, it does not matter.
     */
    
    /** Number of CPUS, to place bound on table size */
    // 虚拟机可用的处理器数量
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    
    /**
     * Table of cells. When non-null, size is a power of 2.
     */
    /*
     * 分段记录当前对象的【操作系数】(包括放大/缩小/增减等操作)
     * 最后获取总值时，要考虑此处的系数
     */
    transient volatile Cell[] cells;
    
    /**
     * Base value, used mainly when there is no contention, but also as
     * a fallback during table initialization races. Updated via CAS.
     */
    // 当前对象的基值（获取总值时，还要考虑cells中的【操作系数】）
    transient volatile long base;
    
    /**
     * Spinlock (locked via CAS) used when resizing and/or creating Cells.
     */
    // 自旋锁，标记cells是否处于繁忙（被操作）状态，繁忙是1，不忙是0
    transient volatile int cellsBusy;
    
    // VarHandle mechanics
    private static final VarHandle BASE;
    private static final VarHandle CELLSBUSY;
    private static final VarHandle THREAD_PROBE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            BASE = l.findVarHandle(Striped64.class, "base", long.class);
            CELLSBUSY = l.findVarHandle(Striped64.class, "cellsBusy", int.class);
            l = AccessController.doPrivileged(new PrivilegedAction<>() {
                public MethodHandles.Lookup run() {
                    try {
                        return MethodHandles.privateLookupIn(Thread.class, MethodHandles.lookup());
                    } catch(ReflectiveOperationException e) {
                        throw new ExceptionInInitializerError(e);
                    }
                }
            });
            THREAD_PROBE = l.findVarHandle(Thread.class, "threadLocalRandomProbe", int.class);
        } catch(ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    /**
     * Package-private default constructor.
     */
    Striped64() {
    }
    
    /**
     * Returns the probe value for the current thread.
     * Duplicated from ThreadLocalRandom because of packaging restrictions.
     */
    // 获取当前线程内的探测值
    static final int getProbe() {
        return (int) THREAD_PROBE.get(Thread.currentThread());
    }
    
    /**
     * Pseudo-randomly advances and records the given probe value for the given thread.
     * Duplicated from ThreadLocalRandom because of packaging restrictions.
     */
    // 更新当前线程内的探测值，并返回更新后的值
    static final int advanceProbe(int probe) {
        probe ^= probe << 13;   // xorshift
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        THREAD_PROBE.set(Thread.currentThread(), probe);
        return probe;
    }
    
    /**
     * CASes the base field.
     */
    // 原子地更新基值，返回值指示是否更新成功
    final boolean casBase(long cmp, long val) {
        return BASE.compareAndSet(this, cmp, val);
    }
    
    // 原子地设置基值，并返回旧值
    final long getAndSetBase(long val) {
        return (long)BASE.getAndSet(this, val);
    }
    
    /**
     * CASes the cellsBusy field from 0 to 1 to acquire lock.
     */
    // 获取cells的操作权（从不忙状态0更新到繁忙状态1）
    final boolean casCellsBusy() {
        return CELLSBUSY.compareAndSet(this, 0, 1);
    }
    
    /**
     * Handles cases of updates involving initialization, resizing,
     * creating new Cells, and/or contention. See above for
     * explanation. This method suffers the usual non-modularity
     * problems of optimistic retry code, relying on rechecked sets of
     * reads.
     *
     * @param x the value
     * @param fn the update function, or null for add (this convention
     * avoids the need for an extra field or function in LongAdder).
     * @param wasUncontended false if CAS failed before call
     */
    // 对long值进行一些目标操作，包括扩大/缩小/增减等
    final void longAccumulate(long x, LongBinaryOperator fn, boolean wasUncontended) {
        int h;
        
        // 获取当前线程的探测值（必须先确保当前线程的探测值有效）
        if((h = getProbe()) == 0) {
            ThreadLocalRandom.current(); // force initialization
            h = getProbe();
            wasUncontended = true;
        }
        
        boolean collide = false;                // True if last slot nonempty
        
        for(; ; ) {
            Cell[] cs;
            Cell c;
            int n;
            long v;
            
            // 如果cells已经存在
            if((cs = cells) != null && (n = cs.length)>0) {
                // 如果当前线程关联的cell为null，尝试为其创造一个新的cell
                if((c = cs[(n - 1) & h]) == null) {
                    // Try to attach new Cell
                    if(cellsBusy == 0) {
                        // Optimistically create
                        Cell r = new Cell(x);
                        // 获取cells的控制权
                        if(cellsBusy == 0 && casCellsBusy()) {
                            try {               // Recheck under lock
                                Cell[] rs;
                                int m, j;
                                // 存在有效的cells，且当前线程关联的cell为null，则为该cell赋值
                                if((rs = cells) != null
                                    && (m = rs.length)>0
                                    && rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    break;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            continue;           // Slot is now non-empty
                        }
                    }
                    collide = false;
                    
                    // 如果当前线程关联的cell不为null，但是上次更新【操作系数】时失败了，说明此该cell被争用，需要更新一下探测值重新找一个cell
                } else if(!wasUncontended) {    // CAS already known to fail
                    wasUncontended = true;      // Continue after rehash
                    
                    // 如果当前线程关联的cell不为null，尝试更新其【操作系数】（wasUncontended已经为true）
                } else if(c.cas(v = c.value, (fn == null) ? v + x : fn.applyAsLong(v, x))) {
                    break;
                    
                    // 如果cell的数量已经超过了虚拟机可用的处理器数量，或者，cells被别的线程扩容了，则不需要纠结碰撞的问题
                } else if(n >= NCPU || cells != cs) {
                    // 重置collide为false，表示当前没有发生碰撞
                    collide = false;            // At max size or stale
                    
                    // 此时发生了碰撞，需要更新collide为true，下一轮进来可能要扩容
                } else if(!collide) {
                    collide = true;
                    
                    // 获取cells的控制权，并着手扩容
                } else if(cellsBusy == 0 && casCellsBusy()) {
                    try {
                        // Expand table unless stale
                        if(cells == cs) {
                            // 发生碰撞后，又进行了一次赋值尝试，但是失败了，说明cells争用严重，需要扩容
                            cells = Arrays.copyOf(cs, n << 1);
                        }
                    } finally {
                        // 释放cells的控制权
                        cellsBusy = 0;
                    }
                    
                    // 重置collide状态
                    collide = false;
                    
                    // 扩容后不更新探测值，而是在当前的cell位置重试
                    continue;                   // Retry with expanded table
                }
                
                // 更新当前线程内的探测值，并返回更新后的值
                h = advanceProbe(h);
                
                // 如果cells不存在，且获取到cells的操作权
            } else if(cellsBusy == 0 && cells == cs && casCellsBusy()) {
                // Initialize table
                try {
                    // 初始化cells，并向某个cell中存入【操作系数】
                    if(cells == cs) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(x);
                        cells = rs;
                        break;
                    }
                } finally {
                    // 释放cells的控制权
                    cellsBusy = 0;
                }
                
                /*
                 * 如果cells不存在，且无法获取到cells的操作权（被别的线程抢走了控制权）
                 * 则尝试在base上做更新，如果尝试成功，则结束目标操作，否则，重新执行上面的for循环
                 */
            } else if(casBase(v = base, (fn == null) ? v + x : fn.applyAsLong(v, x))) { // Fall back on using base
                break;
            }
        }
    }
    
    /**
     * Same as longAccumulate, but injecting long/double conversions
     * in too many places to sensibly merge with long version, given
     * the low-overhead requirements of this class. So must instead be
     * maintained by copy/paste/adapt.
     */
    // 对double值进行一些目标操作，包括扩大/缩小/增减等
    final void doubleAccumulate(double x, DoubleBinaryOperator fn, boolean wasUncontended) {
        int h;
        
        // 获取当前线程的探测值（必须先确保当前线程的探测值有效）
        if((h = getProbe()) == 0) {
            ThreadLocalRandom.current(); // force initialization
            h = getProbe();
            wasUncontended = true;
        }
        
        boolean collide = false;                // True if last slot nonempty
        
        for(; ; ) {
            Cell[] cs;
            Cell c;
            int n;
            long v;
            
            // 如果cells已经存在
            if((cs = cells) != null && (n = cs.length)>0) {
                // 如果当前线程关联的cell为null，尝试为其创造一个新的cell
                if((c = cs[(n - 1) & h]) == null) {
                    // Try to attach new Cell
                    if(cellsBusy == 0) {
                        // 先计算x的二进制格式，然后返回该二进制格式表示的long
                        long bits = Double.doubleToRawLongBits(x);
                        Cell r = new Cell(bits);
                        
                        // 获取cells的控制权
                        if(cellsBusy == 0 && casCellsBusy()) {
                            try {               // Recheck under lock
                                Cell[] rs;
                                int m, j;
                                // 存在有效的cells，且当前线程关联的cell为null，则为该cell赋值
                                if((rs = cells) != null
                                    && (m = rs.length)>0
                                    && rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    break;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            
                            continue;           // Slot is now non-empty
                        }
                    }
                    
                    collide = false;
                    
                    // 如果当前线程关联的cell不为null，但是上次更新【操作系数】时失败了，说明此该cell被争用，需要更新一下探测值重新找一个cell
                } else if(!wasUncontended) {    // CAS already known to fail
                    wasUncontended = true;      // Continue after rehash
                    
                    // 如果当前线程关联的cell不为null，尝试更新其【操作系数】（wasUncontended已经为true）
                } else if(c.cas(v = c.value, apply(fn, v, x))) {
                    break;
                    
                    // 如果cell的数量已经超过了虚拟机可用的处理器数量，或者，cells被别的线程扩容了，则不需要纠结碰撞的问题
                } else if(n >= NCPU || cells != cs) {
                    // 重置collide为false，表示当前没有发生碰撞
                    collide = false;            // At max size or stale
                    
                    // 此时发生了碰撞，需要更新collide为true，下一轮进来可能要扩容
                } else if(!collide) {
                    collide = true;
                    
                    // 获取cells的控制权，并着手扩容
                } else if(cellsBusy == 0 && casCellsBusy()) {
                    try {
                        // Expand table unless stale
                        if(cells == cs) {
                            // 发生碰撞后，又进行了一次赋值尝试，但是失败了，说明cells争用严重，需要扩容
                            cells = Arrays.copyOf(cs, n << 1);
                        }
                    } finally {
                        // 释放cells的控制权
                        cellsBusy = 0;
                    }
                    
                    // 重置collide状态
                    collide = false;
                    
                    // 扩容后不更新探测值，而是在当前的cell位置重试
                    continue;                   // Retry with expanded table
                }
                
                // 更新当前线程内的探测值，并返回更新后的值
                h = advanceProbe(h);
                
                // 如果cells不存在，且获取到cells的操作权
            } else if(cellsBusy == 0 && cells == cs && casCellsBusy()) {
                // Initialize table
                try {
                    // 初始化cells，并向某个cell中存入【操作系数】
                    if(cells == cs) {
                        Cell[] rs = new Cell[2];
                        long bits = Double.doubleToRawLongBits(x);
                        rs[h & 1] = new Cell(bits);
                        cells = rs;
                        break;
                    }
                } finally {
                    // 释放cells的控制权
                    cellsBusy = 0;
                }
                
                /*
                 * 如果cells不存在，且无法获取到cells的操作权（被别的线程抢走了控制权）
                 * 则尝试在base上做更新，如果尝试成功，则结束目标操作，否则，重新执行上面的for循环
                 */
            } else if(casBase(v = base, apply(fn, v, x))) { // Fall back on using base
                break;
            }
        }
    }
    
    // 使用fn处理v和x（v要先转换为double，返回值也代表double）
    private static long apply(DoubleBinaryOperator fn, long v, double x) {
        // 先计算v的二进制格式，然后返回该二进制格式表示的double
        double d = Double.longBitsToDouble(v);
        // 处理v跟x（默认是相加）
        d = (fn == null) ? d + x : fn.applyAsDouble(d, x);
        // 以long形式返回处理结果
        return Double.doubleToRawLongBits(d);
    }
    
    /**
     * Padded variant of AtomicLong supporting only raw accesses plus CAS.
     *
     * JVM intrinsics note: It would be possible to use a release-only
     * form of CAS here, if it were provided.
     */
    // 分段存储当前对象的【操作系数】，缓解线程争用
    @jdk.internal.vm.annotation.Contended
    static final class Cell {
        // 【操作系数】，参见cells参数
        volatile long value;
        
        // VarHandle mechanics
        private static final VarHandle VALUE;
        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                VALUE = l.findVarHandle(Cell.class, "value", long.class);
            } catch(ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        
        Cell(long x) {
            value = x;
        }
        
        // 原子地更新cell
        final boolean cas(long cmp, long val) {
            return VALUE.compareAndSet(this, cmp, val);
        }
        
        // 原子地设置cell为val，并返回旧值
        final long getAndSet(long val) {
            return (long) VALUE.getAndSet(this, val);
        }
        
        // 重置cell为0
        final void reset() {
            VALUE.setVolatile(this, 0L);
        }
        
        // 重置cell为identity
        final void reset(long identity) {
            VALUE.setVolatile(this, identity);
        }
    }
    
}
