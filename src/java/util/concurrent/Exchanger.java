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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;

/**
 * A synchronization point at which threads can pair and swap elements
 * within pairs.  Each thread presents some object on entry to the
 * {@link #exchange exchange} method, matches with a partner thread,
 * and receives its partner's object on return.  An Exchanger may be
 * viewed as a bidirectional form of a {@link SynchronousQueue}.
 * Exchangers may be useful in applications such as genetic algorithms
 * and pipeline designs.
 *
 * <p><b>Sample Usage:</b>
 * Here are the highlights of a class that uses an {@code Exchanger}
 * to swap buffers between threads so that the thread filling the
 * buffer gets a freshly emptied one when it needs it, handing off the
 * filled one to the thread emptying the buffer.
 * <pre> {@code
 * class FillAndEmpty {
 *   Exchanger<DataBuffer> exchanger = new Exchanger<>();
 *   DataBuffer initialEmptyBuffer = ... a made-up type
 *   DataBuffer initialFullBuffer = ...
 *
 *   class FillingLoop implements Runnable {
 *     public void run() {
 *       DataBuffer currentBuffer = initialEmptyBuffer;
 *       try {
 *         while (currentBuffer != null) {
 *           addToBuffer(currentBuffer);
 *           if (currentBuffer.isFull())
 *             currentBuffer = exchanger.exchange(currentBuffer);
 *         }
 *       } catch (InterruptedException ex) { ... handle ... }
 *     }
 *   }
 *
 *   class EmptyingLoop implements Runnable {
 *     public void run() {
 *       DataBuffer currentBuffer = initialFullBuffer;
 *       try {
 *         while (currentBuffer != null) {
 *           takeFromBuffer(currentBuffer);
 *           if (currentBuffer.isEmpty())
 *             currentBuffer = exchanger.exchange(currentBuffer);
 *         }
 *       } catch (InterruptedException ex) { ... handle ...}
 *     }
 *   }
 *
 *   void start() {
 *     new Thread(new FillingLoop()).start();
 *     new Thread(new EmptyingLoop()).start();
 *   }
 * }}</pre>
 *
 * <p>Memory consistency effects: For each pair of threads that
 * successfully exchange objects via an {@code Exchanger}, actions
 * prior to the {@code exchange()} in each thread
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * those subsequent to a return from the corresponding {@code exchange()}
 * in the other thread.
 *
 * @since 1.5
 * @author Doug Lea and Bill Scherer and Michael Scott
 * @param <V> The type of objects that may be exchanged
 */
/*
 * 交换器，用于多个线程之间交换数据
 *
 * 整体思路是，执行快的线程将待交换数据放入交换槽，
 * 然后等待执行慢的线程从交换槽拿走数据，并将自己的数据也放入交换槽，
 * 最后，那个执行快的线程取出交换槽中的数据，这样，成对的线程就完成了数据交换。
 */
public class Exchanger<V> {
    
    /*
     * Overview: The core algorithm is, for an exchange "slot",
     * and a participant (caller) with an item:
     *
     * for (;;) {
     *   if (slot is empty) {                       // offer
     *     place item in a Node;
     *     if (can CAS slot from empty to node) {
     *       wait for release;
     *       return matching item in node;
     *     }
     *   }
     *   else if (can CAS slot from node to empty) { // release
     *     get the item in node;
     *     set matching item in node;
     *     release waiting thread;
     *   }
     *   // else retry on CAS failure
     * }
     *
     * This is among the simplest forms of a "dual data structure" --
     * see Scott and Scherer's DISC 04 paper and
     * http://www.cs.rochester.edu/research/synchronization/pseudocode/duals.html
     *
     * This works great in principle. But in practice, like many
     * algorithms centered on atomic updates to a single location, it
     * scales horribly when there are more than a few participants
     * using the same Exchanger. So the implementation instead uses a
     * form of elimination arena, that spreads out this contention by
     * arranging that some threads typically use different slots,
     * while still ensuring that eventually, any two parties will be
     * able to exchange items. That is, we cannot completely partition
     * across threads, but instead give threads arena indices that
     * will on average grow under contention and shrink under lack of
     * contention. We approach this by defining the Nodes that we need
     * anyway as ThreadLocals, and include in them per-thread index
     * and related bookkeeping state. (We can safely reuse per-thread
     * nodes rather than creating them fresh each time because slots
     * alternate between pointing to a node vs null, so cannot
     * encounter ABA problems. However, we do need some care in
     * resetting them between uses.)
     *
     * Implementing an effective arena requires allocating a bunch of
     * space, so we only do so upon detecting contention (except on
     * uniprocessors, where they wouldn't help, so aren't used).
     * Otherwise, exchanges use the single-slot slotExchange method.
     * On contention, not only must the slots be in different
     * locations, but the locations must not encounter memory
     * contention due to being on the same cache line (or more
     * generally, the same coherence unit).  Because, as of this
     * writing, there is no way to determine cacheline size, we define
     * a value that is enough for common platforms.  Additionally,
     * extra care elsewhere is taken to avoid other false/unintended
     * sharing and to enhance locality, including adding padding (via
     * @Contended) to Nodes, embedding "bound" as an Exchanger field.
     *
     * The arena starts out with only one used slot. We expand the
     * effective arena size by tracking collisions; i.e., failed CASes
     * while trying to exchange. By nature of the above algorithm, the
     * only kinds of collision that reliably indicate contention are
     * when two attempted releases collide -- one of two attempted
     * offers can legitimately fail to CAS without indicating
     * contention by more than one other thread. (Note: it is possible
     * but not worthwhile to more precisely detect contention by
     * reading slot values after CAS failures.)  When a thread has
     * collided at each slot within the current arena bound, it tries
     * to expand the arena size by one. We track collisions within
     * bounds by using a version (sequence) number on the "bound"
     * field, and conservatively reset collision counts when a
     * participant notices that bound has been updated (in either
     * direction).
     *
     * The effective arena size is reduced (when there is more than
     * one slot) by giving up on waiting after a while and trying to
     * decrement the arena size on expiration. The value of "a while"
     * is an empirical matter.  We implement by piggybacking on the
     * use of spin->yield->block that is essential for reasonable
     * waiting performance anyway -- in a busy exchanger, offers are
     * usually almost immediately released, in which case context
     * switching on multiprocessors is extremely slow/wasteful.  Arena
     * waits just omit the blocking part, and instead cancel. The spin
     * count is empirically chosen to be a value that avoids blocking
     * 99% of the time under maximum sustained exchange rates on a
     * range of test machines. Spins and yields entail some limited
     * randomness (using a cheap xorshift) to avoid regular patterns
     * that can induce unproductive grow/shrink cycles. (Using a
     * pseudorandom also helps regularize spin cycle duration by
     * making branches unpredictable.)  Also, during an offer, a
     * waiter can "know" that it will be released when its slot has
     * changed, but cannot yet proceed until match is set.  In the
     * mean time it cannot cancel the offer, so instead spins/yields.
     * Note: It is possible to avoid this secondary check by changing
     * the linearization point to be a CAS of the match field (as done
     * in one case in the Scott & Scherer DISC paper), which also
     * increases asynchrony a bit, at the expense of poorer collision
     * detection and inability to always reuse per-thread nodes. So
     * the current scheme is typically a better tradeoff.
     *
     * On collisions, indices traverse the arena cyclically in reverse
     * order, restarting at the maximum index (which will tend to be
     * sparsest) when bounds change. (On expirations, indices instead
     * are halved until reaching 0.) It is possible (and has been
     * tried) to use randomized, prime-value-stepped, or double-hash
     * style traversal instead of simple cyclic traversal to reduce
     * bunching.  But empirically, whatever benefits these may have
     * don't overcome their added overhead: We are managing operations
     * that occur very quickly unless there is sustained contention,
     * so simpler/faster control policies work better than more
     * accurate but slower ones.
     *
     * Because we use expiration for arena size control, we cannot
     * throw TimeoutExceptions in the timed version of the public
     * exchange method until the arena size has shrunken to zero (or
     * the arena isn't enabled). This may delay response to timeout
     * but is still within spec.
     *
     * Essentially all of the implementation is in methods
     * slotExchange and arenaExchange. These have similar overall
     * structure, but differ in too many details to combine. The
     * slotExchange method uses the single Exchanger field "slot"
     * rather than arena array elements. However, it still needs
     * minimal collision detection to trigger arena construction.
     * (The messiest part is making sure interrupt status and
     * InterruptedExceptions come out right during transitions when
     * both methods may be called. This is done by using null return
     * as a sentinel to recheck interrupt status.)
     *
     * As is too common in this sort of code, methods are monolithic
     * because most of the logic relies on reads of fields that are
     * maintained as local variables so can't be nicely factored --
     * mainly, here, bulky spin->yield->block/cancel code.  Note that
     * field Node.item is not declared as volatile even though it is
     * read by releasing threads, because they only do so after CAS
     * operations that must precede access, and all uses by the owning
     * thread are otherwise acceptably ordered by other operations.
     * (Because the actual points of atomicity are slot CASes, it
     * would also be legal for the write to Node.match in a release to
     * be weaker than a full volatile write. However, this is not done
     * because it could allow further postponement of the write,
     * delaying progress.)
     */
    
    /**
     * Per-thread state.
     */
    // ThreadLocal的子类，用来获取交换结点（每个线程都有一个）
    private final Participant participant;
    
    /**
     * Slot used until contention detected.
     */
    // 多个线程对交换结点不存在争用时，使用该交换槽
    private volatile Node slot;
    
    /**
     * Elimination array; null until enabled (within slotExchange).
     * Element accesses use emulation of volatile gets and CAS.
     */
    // 竞技场，分为FULL+2个片区。当多个线程对交换结点存在争用时，使用该域
    private volatile Node[] arena;
    
    /** The number of CPUs, for sizing and spin control */
    // CPU核心数量
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    
    /**
     * The bound for spins while waiting for a match.
     * The actual number of iterations will on average be about twice this value due to randomization.
     * Note: Spinning is disabled when NCPU==1.
     */
    // 自旋次数
    private static final int SPINS = 1 << 10;
    
    /**
     * The index distance (as a shift value) between any two used slots
     * in the arena, spacing them out to avoid false sharing.
     */
    // 竞技场每个片区的容量（只使用最后一个插槽）
    private static final int ASHIFT = 5;
    
    /**
     * The maximum supported arena index.
     * The maximum allocatable arena size is MMASK + 1.
     * Must be a power of two minus one, less than (1<<(31-ASHIFT)).
     * The cap of 255 (0xff) more than suffices for the expected scaling limits of the main algorithms.
     */
    // 竞技场片区有效索引的最大值
    private static final int MMASK = 0xff;      // 0x ff
    
    /**
     * The maximum slot index of the arena:
     * The number of slots that can in principle hold all threads without contention,
     * or at most the maximum indexable value.
     */
    // 竞技场中当前片区的数量（0<=FULL<=MMASK）
    static final int FULL = (NCPU >= (MMASK << 1)) ? MMASK : NCPU >>> 1;
    
    /**
     * Unit for sequence/version bits of bound field. Each successful change to the bound also adds SEQ.
     */
    // 与bound配合使用，解决bound变化引发的ABA问题
    private static final int SEQ = MMASK + 1;   // 0b 100
    
    /**
     * The index of the largest valid arena position, OR'ed with SEQ
     * number in high bits, incremented on each update.  The initial
     * update from 0 to SEQ is used to ensure that the arena array is
     * constructed only once.
     */
    // bound & MMASK为竞技场上当前的片区索引的约束
    private volatile int bound;
    
    /**
     * Value representing null arguments/returns from public
     * methods. Needed because the API originally didn't disallow null
     * arguments, which it should have.
     */
    private static final Object NULL_ITEM = new Object();
    
    /**
     * Sentinel value returned by internal exchange methods upon
     * timeout, to avoid need for separate timed versions of these
     * methods.
     */
    private static final Object TIMED_OUT = new Object();
    
    // VarHandle mechanics
    private static final VarHandle BOUND;
    private static final VarHandle SLOT;
    private static final VarHandle MATCH;
    private static final VarHandle AA;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            BOUND = l.findVarHandle(Exchanger.class, "bound", int.class);
            SLOT = l.findVarHandle(Exchanger.class, "slot", Node.class);
            MATCH = l.findVarHandle(Node.class, "match", Object.class);
            AA = MethodHandles.arrayElementVarHandle(Node[].class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    /**
     * Creates a new Exchanger.
     */
    public Exchanger() {
        participant = new Participant();
    }
    
    /**
     * Waits for another thread to arrive at this exchange point (unless
     * the current thread is {@linkplain Thread#interrupt interrupted}),
     * and then transfers the given object to it, receiving its object
     * in return.
     *
     * <p>If another thread is already waiting at the exchange point then
     * it is resumed for thread scheduling purposes and receives the object
     * passed in by the current thread.  The current thread returns immediately,
     * receiving the object passed to the exchange by that other thread.
     *
     * <p>If no other thread is already waiting at the exchange then the
     * current thread is disabled for thread scheduling purposes and lies
     * dormant until one of two things happens:
     * <ul>
     * <li>Some other thread enters the exchange; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for the exchange,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @param x the object to exchange
     *
     * @return the object provided by the other thread
     *
     * @throws InterruptedException if the current thread was
     *                              interrupted while waiting
     */
    // 交换数据
    @SuppressWarnings("unchecked")
    public V exchange(V x) throws InterruptedException {
        Object v;
        Node[] a;
        Object item = (x == null) ? NULL_ITEM : x; // translate null args
        
        if((a=arena)!=null || (v=slotExchange(item, false, 0L))==null) {
            if((Thread.interrupted() || (v = arenaExchange(item, false, 0L)) == null)){
                throw new InterruptedException();
            }
        }
        
        return (v == NULL_ITEM) ? null : (V) v;
    }
    
    /**
     * Waits for another thread to arrive at this exchange point (unless
     * the current thread is {@linkplain Thread#interrupt interrupted} or
     * the specified waiting time elapses), and then transfers the given
     * object to it, receiving its object in return.
     *
     * <p>If another thread is already waiting at the exchange point then
     * it is resumed for thread scheduling purposes and receives the object
     * passed in by the current thread.  The current thread returns immediately,
     * receiving the object passed to the exchange by that other thread.
     *
     * <p>If no other thread is already waiting at the exchange then the
     * current thread is disabled for thread scheduling purposes and lies
     * dormant until one of three things happens:
     * <ul>
     * <li>Some other thread enters the exchange; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for the exchange,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then {@link
     * TimeoutException} is thrown.  If the time is less than or equal
     * to zero, the method will not wait at all.
     *
     * @param x       the object to exchange
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the {@code timeout} argument
     *
     * @return the object provided by the other thread
     *
     * @throws InterruptedException if the current thread was
     *                              interrupted while waiting
     * @throws TimeoutException     if the specified waiting time elapses
     *                              before another thread enters the exchange
     */
    // 交换数据，限制在指定时间内完成
    @SuppressWarnings("unchecked")
    public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        Object v;
        Object item = (x == null) ? NULL_ITEM : x;
        
        long ns = unit.toNanos(timeout);
        
        if((arena != null || (v = slotExchange(item, true, ns)) == null)) {
            if((Thread.interrupted() || (v = arenaExchange(item, true, ns)) == null)){
                throw new InterruptedException();
            }
        }
        
        if(v == TIMED_OUT) {
            throw new TimeoutException();
        }
        
        return (v == NULL_ITEM) ? null : (V) v;
    }
    
    /**
     * Exchange function used until arenas enabled. See above for explanation.
     *
     * @param item  the item to exchange
     * @param timed true if the wait is timed
     * @param ns    if timed, the maximum wait time, else 0L
     *
     * @return the other thread's item; or null if either the arena
     * was enabled or the thread was interrupted before completion; or
     * TIMED_OUT if timed and timed out
     */
    // 使用交换槽交换数据
    private final Object slotExchange(Object item, boolean timed, long ns) {
        // 获取当前线程的交换结点（如果还不存在的话调用initialValue()创建一个）
        Node p = participant.get();
        
        Thread t = Thread.currentThread();
        // 测试线程是否已经中断，线程的中断状态不受影响
        if(t.isInterrupted()) {
            // preserve interrupt status so caller can recheck
            return null;
        }
        
        Node q;
        
        for(; ; ) {
            /* 如果交换槽中存在交换结点（属于匹配线程） */
            if((q = slot) != null) {
                // 取出交换槽中的交换结点，置空交换槽（目的是锁定这个交换结点不被其他线程抢走）
                if(SLOT.compareAndSet(this, q, null)) {
                    // 从匹配线程的交换结点中取出匹配线程放入的数据
                    Object v = q.item;
                    // 向匹配线程的交换结点中放入当前线程的数据以便匹配线程取用
                    q.match = item;
                    // 唤醒匹配线程
                    Thread w = q.parked;
                    if(w != null) {
                        LockSupport.unpark(w);
                    }
                    // 返回从匹配线程中取到的数据
                    return v;
                }
                
                /* 至此，说明多个线程在争用一个交换结点，此时尝试初始化竞技场 */
                
                // create arena on contention, but continue until slot null
                if(NCPU>1 && bound == 0 && BOUND.compareAndSet(this, 0, SEQ)) {
                    arena = new Node[(FULL + 2) << ASHIFT];
                }
            } else if(arena != null) {
                // 当存在竞技场时，需要转到arenaExchange()方法中，在竞技场内交换数据
                return null; // caller must reroute to arenaExchange
            } else {
                /*
                 * 至此，说明未获取到交换结点
                 * 这同时也说明没有其他线程与当前线程争用交换结点
                 */
                
                // 向当前线程的交换结点内设置待交换数据
                p.item = item;
                // 将当前线程的交换结点发送到交换槽以待交换（如果arena[j]==null，令arena[j]=p）
                if(SLOT.compareAndSet(this, null, p)) {
                    break;
                }
                // 如果有别的线程抢先向交换槽发送了交换结点，那么重新循环，以获取这个结点，并交换数据
                p.item = null;
            }
        }
        
        // await release
        int h = p.hash;
        
        // 如果启用了超时设置，需要计算一个终止时间
        long end = timed ? System.nanoTime() + ns : 0L;
        
        // 规划自旋次数
        int spins = (NCPU>1) ? SPINS : 1;
        
        // 接收匹配线程中的数据
        Object v;
        
        // 如果没有获取到匹配线程中的数据
        while((v = p.match) == null) {
            // 尝试自旋
            if(spins>0) {
                h ^= h << 1;
                h ^= h >>> 3;
                h ^= h << 10;
                if(h == 0) {
                    h = SPINS | (int) t.getId();
                } else if(h<0 && (--spins & ((SPINS >>> 1) - 1)) == 0) {
                    Thread.yield();
                }
                
            } else if(slot != p) {
                /*
                 * 正常来说，如果匹配线程发现了当前线程送入交换槽的交换结点，
                 * 那么匹配线程会锁定该交换结点（使slot==null）
                 * 并且，匹配线程会取出交换结点item域中的数据，并将自己的数据存入match域等待当前线程取用，
                 *
                 * 这里结束自旋后，如果发现slot!=p，则说明有别线程发现了这个交换结点，并成功锁定，
                 * 此时，捕获这种情形，继续自旋，以等待匹配线程填充match域
                 */
                
                spins = SPINS;
            } else if(!t.isInterrupted() && arena == null && (!timed || (ns = end - System.nanoTime())>0L)) {
                /*
                 * 至此，说明slop==p，这表示仍然没有别的线程发现这个交换结点
                 * 那么此时，要计划进入阻塞...
                 * 进入阻塞要保证：
                 * 1.线程没有中断标记
                 * 2.不存在多个线程对交换结点的锁定竞争（即arena==null）
                 *   如果arena!=null，表明已有线程锁定了交换结点，那么交换结点的match域马上就能得到数据了，不需要阻塞
                 * 3.未启用超时，或者启用了超时，但时间未超过截止时间
                 */
                
                p.parked = t;
                
                // 准备阻塞
                if(slot == p) {
                    if(ns == 0L) {
                        LockSupport.park(this);
                    } else {
                        LockSupport.parkNanos(this, ns);
                    }
                }
                
                p.parked = null;
                
                // 如果slot==p，设置slot==null
            } else if(SLOT.compareAndSet(this, p, null)) {
                /*
                 * 至此，说明超时后仍然没有交换完数据
                 * 或者，在交换过程中线程被中断了
                 */
                
                v = timed && ns<=0L && !t.isInterrupted() ? TIMED_OUT : null;
                
                break;
            }
        }
        
        // 设置p.match==null
        MATCH.setRelease(p, null);
        p.item = null;
        p.hash = h;
        
        // 返回从匹配线程拿到的数据
        return v;
    }
    
    /**
     * Exchange function when arenas enabled. See above for explanation.
     *
     * @param item  the (non-null) item to exchange
     * @param timed true if the wait is timed
     * @param ns    if timed, the maximum wait time, else 0L
     *
     * @return the other thread's item; or null if interrupted; or
     * TIMED_OUT if timed and timed out
     */
    // 使用竞技场交换数据
    private final Object arenaExchange(Object item, boolean timed, long ns) {
        Node[] a = arena;
        int alen = a.length;
        
        // 获取当前线程的交换结点（如果还不存在的话调用initialValue()创建一个）
        Node p = participant.get();
        
        // 获取当前线程在竞技场上的片区索引（？奇怪为什么要设置片区，而不是直接设置交换槽）
        int i = p.index;
        
        for( ; ; ) { // access slot at i
            int b, m, c;
            
            // 获取当前线程所处的交换槽索引（总是在下标i的片区的最后一个位置）
            int j = (i << ASHIFT) + ((1 << ASHIFT) - 1);
            if(j<0 || j >= alen) {
                j = alen - 1;
            }
            
            // 获取交换槽arena[j]处的交换结点
            Node q = (Node) AA.getAcquire(a, j);
            
            // 如果交换结点存在，且成功锁定它（与slotExchange()方法中的锁定方式是一样的）
            if(q != null && AA.compareAndSet(a, j, q, null)) {
                // 从匹配线程的交换结点中取出匹配线程放入的数据
                Object v = q.item;                     // release
                // 向匹配线程的交换结点中放入当前线程的数据以便匹配线程取用
                q.match = item;
                // 唤醒匹配线程
                Thread w = q.parked;
                if(w != null) {
                    LockSupport.unpark(w);
                }
                // 返回从匹配线程中取到的数据
                return v;
                
                //当前线程在竞技场上的片区索引未超出约束范围，且交换结点不存在
            } else if(i<=(m = (b = bound) & MMASK) && q == null) {
                /* 至此，说明没有其他线程与当前线程争用交换结点 */
                
                // 向当前线程的交换结点内设置待交换数据
                p.item = item;                         // offer
                
                // 将当前线程的交换结点发送到交换槽以待交换（如果arena[j]==null，令arena[j]=p）
                if(AA.compareAndSet(a, j, null, p)) {
                    Thread t = Thread.currentThread(); // wait
                    
                    int h = p.hash;
                    
                    // 如果启用了超时设置，需要计算一个终止时间
                    long end = (timed && m == 0) ? System.nanoTime() + ns : 0L;
                    
                    // 规划自旋次数
                    int spins = SPINS;
                    
                    for( ; ; ) {
                        // 接收匹配线程中的数据
                        Object v = p.match;
                        
                        if(v != null) {
                            // 设置p.match==null
                            MATCH.setRelease(p, null);
                            p.item = null;             // clear for next use
                            p.hash = h;
                            
                            // 返回从匹配线程拿到的数据
                            return v;
                        }
                        
                        // 如果没有获取到匹配线程中的数据，尝试自旋
                        if(spins>0) {
                            h ^= h << 1;
                            h ^= h >>> 3;
                            h ^= h << 10; // xorshift
                            if(h == 0) {
                                // initialize hash
                                h = SPINS | (int) t.getId();
                            } else if(h<0          // approx 50% true
                                && (--spins & ((SPINS >>> 1) - 1)) == 0) {
                                Thread.yield();        // two yields per wait
                            }
                            
                        } else if(AA.getAcquire(a, j) != p) {
                            /*
                             * 正常来说，如果匹配线程发现了当前线程送入交换槽的交换结点，
                             * 那么匹配线程会锁定该交换结点（使slot==null）
                             * 并且，匹配线程会取出交换结点item域中的数据，并将自己的数据存入match域等待当前线程取用，
                             *
                             * 这里结束自旋后，如果发现slot!=p，则说明有别线程发现了这个交换结点，并成功锁定，
                             * 此时，捕获这种情形，继续自旋，以等待匹配线程填充match域
                             */
                            
                            spins = SPINS;       // releaser hasn't set match yet
                        } else if(!t.isInterrupted() && m==0 && (!timed || (ns = end - System.nanoTime())>0L)) {
                            /*
                             * 至此，说明slop==p，这表示仍然没有别的线程发现这个交换结点
                             * 那么此时，要计划进入阻塞...
                             * 进入阻塞要保证：
                             * 1.线程没有中断标记
                             * 2.不存在多个线程对交换结点的锁定竞争（即m==0）
                             *   如果m!=0，表明已有线程锁定了交换结点，那么交换结点的match域马上就能得到数据了，不需要阻塞
                             * 3.未启用超时，或者启用了超时，但时间未超过截止时间
                             */
                            
                            p.parked = t;              // minimize window
                            
                            // 如果arena[j]==p
                            if(AA.getAcquire(a, j) == p) {
                                if(ns == 0L) {
                                    LockSupport.park(this);
                                } else {
                                    LockSupport.parkNanos(this, ns);
                                }
                            }
                            
                            p.parked = null;
                            
                            // 如果arena[j]==p，则令arena[j]=null
                        } else if(AA.getAcquire(a, j)==p && AA.compareAndSet(a, j, p, null)) {
                            /*
                             * 至此，说明超时后仍然没有交换完数据
                             * 或者，在交换过程中线程被中断了
                             */
                            
                            // 结束了一个阻塞线程后（不管是否成功交换了数据），需要将竞技场中的片区索引约束也减一（收缩交换槽范围）
                            if(m != 0) {
                                // try to shrink
                                BOUND.compareAndSet(this, b, b + SEQ - 1);
                            }
                            
                            p.item = null;
                            p.hash = h;
                            i = p.index >>>= 1;        // descend
                            
                            if(Thread.interrupted()) {
                                return null;
                            }
                            
                            if(timed && m == 0 && ns<=0L) {
                                return TIMED_OUT;
                            }
                            
                            break;                     // expired; restart
                        }
                    } // for( ; ; )
                } else {
                    // 如果有别的线程抢先向交换槽发送了交换结点，那么重新循环，以获取这个结点，并交换数据
                    p.item = null;                     // clear offer
                }
                
            } else {
                /*
                 * 至此，说明有多个线程同时发现了交换结点，
                 * 那么，必然只会有一个线程成功锁定结点，
                 * 在锁定竞争中失败的线程，会执行到此
                 */
                
                // 更新交换结点的bound到竞技场最新的bound
                if(p.bound != b) {                    // stale; reset
                    p.bound = b;
                    p.collides = 0;
                    i = (i != m || m == 0) ? m : m - 1;
                } else if((c = p.collides)<m || m == FULL || !BOUND.compareAndSet(this, b, b + SEQ + 1)) {
                    p.collides = c + 1;
                    i = (i == 0) ? m : i - 1;          // cyclically traverse
                } else {
                    // 准备将当前线程的交换结点存入此片区
                    i = m + 1;                         // grow
                }
                
                p.index = i;
            }
        } // for( ; ; )
    }
    
    /**
     * Nodes hold partially exchanged data, plus other per-thread
     * bookkeeping. Padded via @Contended to reduce memory contention.
     */
    // 交换结点（每个线程都有一个）
    @jdk.internal.vm.annotation.Contended
    static final class Node {
        int index;              // Arena index
        int bound;              // Last recorded value of Exchanger.bound
        int collides;           // Number of CAS failures at current bound
        int hash;               // Pseudo-random for spins
        
        // 当前线程持有的待交换数据
        Object item;            // This thread's current item
        // 与当前线程进行数据交换的线程持有的待交换数据
        volatile Object match;  // Item provided by releasing thread
        
        volatile Thread parked; // Set to this thread when parked, else null
    }
    
    /** The corresponding thread local class */
    // 参与者，用来存储参与信息交换的线程
    static final class Participant extends ThreadLocal<Node> {
        public Node initialValue() {
            return new Node();
        }
    }
}
