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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An unbounded {@link TransferQueue} based on linked nodes.
 * This queue orders elements FIFO (first-in-first-out) with respect
 * to any given producer.  The <em>head</em> of the queue is that
 * element that has been on the queue the longest time for some
 * producer.  The <em>tail</em> of the queue is that element that has
 * been on the queue the shortest time for some producer.
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
 * methods of the {@link Collection} and {@link Iterator} interfaces.
 *
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code LinkedTransferQueue}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code LinkedTransferQueue} in another thread.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @since 1.7
 * @author Doug Lea
 * @param <E> the type of elements held in this queue
 */
/*
 * 链式无界单向阻塞队列，线程安全（CAS），可以看做是SynchronousQueue的加强版
 *
 * 使用LinkedTransferQueue的时候，如果一个线程的put()操作找不到互补的take()操作时，
 * 它将数据放入阻塞队列，并立即返回，这一点上与SynchronousQueue不同。
 */
public class LinkedTransferQueue<E> extends AbstractQueue<E> implements TransferQueue<E>, Serializable {
    
    /*
     * *** Overview of Dual Queues with Slack ***
     *
     * Dual Queues, introduced by Scherer and Scott
     * (http://www.cs.rochester.edu/~scott/papers/2004_DISC_dual_DS.pdf)
     * are (linked) queues in which nodes may represent either data or
     * requests.  When a thread tries to enqueue a data node, but
     * encounters a request node, it instead "matches" and removes it;
     * and vice versa for enqueuing requests. Blocking Dual Queues
     * arrange that threads enqueuing unmatched requests block until
     * other threads provide the match. Dual Synchronous Queues (see
     * Scherer, Lea, & Scott
     * http://www.cs.rochester.edu/u/scott/papers/2009_Scherer_CACM_SSQ.pdf)
     * additionally arrange that threads enqueuing unmatched data also
     * block.  Dual Transfer Queues support all of these modes, as
     * dictated by callers.
     *
     * A FIFO dual queue may be implemented using a variation of the
     * Michael & Scott (M&S) lock-free queue algorithm
     * (http://www.cs.rochester.edu/~scott/papers/1996_PODC_queues.pdf).
     * It maintains two pointer fields, "head", pointing to a
     * (matched) node that in turn points to the first actual
     * (unmatched) queue node (or null if empty); and "tail" that
     * points to the last node on the queue (or again null if
     * empty). For example, here is a possible queue with four data
     * elements:
     *
     *  head                tail
     *    |                   |
     *    v                   v
     *    M -> U -> U -> U -> U
     *
     * The M&S queue algorithm is known to be prone to scalability and
     * overhead limitations when maintaining (via CAS) these head and
     * tail pointers. This has led to the development of
     * contention-reducing variants such as elimination arrays (see
     * Moir et al http://portal.acm.org/citation.cfm?id=1074013) and
     * optimistic back pointers (see Ladan-Mozes & Shavit
     * http://people.csail.mit.edu/edya/publications/OptimisticFIFOQueue-journal.pdf).
     * However, the nature of dual queues enables a simpler tactic for
     * improving M&S-style implementations when dual-ness is needed.
     *
     * In a dual queue, each node must atomically maintain its match
     * status. While there are other possible variants, we implement
     * this here as: for a data-mode node, matching entails CASing an
     * "item" field from a non-null data value to null upon match, and
     * vice-versa for request nodes, CASing from null to a data
     * value. (Note that the linearization properties of this style of
     * queue are easy to verify -- elements are made available by
     * linking, and unavailable by matching.) Compared to plain M&S
     * queues, this property of dual queues requires one additional
     * successful atomic operation per enq/deq pair. But it also
     * enables lower cost variants of queue maintenance mechanics. (A
     * variation of this idea applies even for non-dual queues that
     * support deletion of interior elements, such as
     * j.u.c.ConcurrentLinkedQueue.)
     *
     * Once a node is matched, its match status can never again
     * change.  We may thus arrange that the linked list of them
     * contain a prefix of zero or more matched nodes, followed by a
     * suffix of zero or more unmatched nodes. (Note that we allow
     * both the prefix and suffix to be zero length, which in turn
     * means that we do not use a dummy header.)  If we were not
     * concerned with either time or space efficiency, we could
     * correctly perform enqueue and dequeue operations by traversing
     * from a pointer to the initial node; CASing the item of the
     * first unmatched node on match and CASing the next field of the
     * trailing node on appends.  While this would be a terrible idea
     * in itself, it does have the benefit of not requiring ANY atomic
     * updates on head/tail fields.
     *
     * We introduce here an approach that lies between the extremes of
     * never versus always updating queue (head and tail) pointers.
     * This offers a tradeoff between sometimes requiring extra
     * traversal steps to locate the first and/or last unmatched
     * nodes, versus the reduced overhead and contention of fewer
     * updates to queue pointers. For example, a possible snapshot of
     * a queue is:
     *
     *  head           tail
     *    |              |
     *    v              v
     *    M -> M -> U -> U -> U -> U
     *
     * The best value for this "slack" (the targeted maximum distance
     * between the value of "head" and the first unmatched node, and
     * similarly for "tail") is an empirical matter. We have found
     * that using very small constants in the range of 1-3 work best
     * over a range of platforms. Larger values introduce increasing
     * costs of cache misses and risks of long traversal chains, while
     * smaller values increase CAS contention and overhead.
     *
     * Dual queues with slack differ from plain M&S dual queues by
     * virtue of only sometimes updating head or tail pointers when
     * matching, appending, or even traversing nodes; in order to
     * maintain a targeted slack.  The idea of "sometimes" may be
     * operationalized in several ways. The simplest is to use a
     * per-operation counter incremented on each traversal step, and
     * to try (via CAS) to update the associated queue pointer
     * whenever the count exceeds a threshold. Another, that requires
     * more overhead, is to use random number generators to update
     * with a given probability per traversal step.
     *
     * In any strategy along these lines, because CASes updating
     * fields may fail, the actual slack may exceed targeted slack.
     * However, they may be retried at any time to maintain targets.
     * Even when using very small slack values, this approach works
     * well for dual queues because it allows all operations up to the
     * point of matching or appending an item (hence potentially
     * allowing progress by another thread) to be read-only, thus not
     * introducing any further contention.  As described below, we
     * implement this by performing slack maintenance retries only
     * after these points.
     *
     * As an accompaniment to such techniques, traversal overhead can
     * be further reduced without increasing contention of head
     * pointer updates: Threads may sometimes shortcut the "next" link
     * path from the current "head" node to be closer to the currently
     * known first unmatched node, and similarly for tail. Again, this
     * may be triggered with using thresholds or randomization.
     *
     * These ideas must be further extended to avoid unbounded amounts
     * of costly-to-reclaim garbage caused by the sequential "next"
     * links of nodes starting at old forgotten head nodes: As first
     * described in detail by Boehm
     * (http://portal.acm.org/citation.cfm?doid=503272.503282), if a GC
     * delays noticing that any arbitrarily old node has become
     * garbage, all newer dead nodes will also be unreclaimed.
     * (Similar issues arise in non-GC environments.)  To cope with
     * this in our implementation, upon CASing to advance the head
     * pointer, we set the "next" link of the previous head to point
     * only to itself; thus limiting the length of chains of dead nodes.
     * (We also take similar care to wipe out possibly garbage
     * retaining values held in other Node fields.)  However, doing so
     * adds some further complexity to traversal: If any "next"
     * pointer links to itself, it indicates that the current thread
     * has lagged behind a head-update, and so the traversal must
     * continue from the "head".  Traversals trying to find the
     * current tail starting from "tail" may also encounter
     * self-links, in which case they also continue at "head".
     *
     * It is tempting in slack-based scheme to not even use CAS for
     * updates (similarly to Ladan-Mozes & Shavit). However, this
     * cannot be done for head updates under the above link-forgetting
     * mechanics because an update may leave head at a detached node.
     * And while direct writes are possible for tail updates, they
     * increase the risk of long retraversals, and hence long garbage
     * chains, which can be much more costly than is worthwhile
     * considering that the cost difference of performing a CAS vs
     * write is smaller when they are not triggered on each operation
     * (especially considering that writes and CASes equally require
     * additional GC bookkeeping ("write barriers") that are sometimes
     * more costly than the writes themselves because of contention).
     *
     * *** Overview of implementation ***
     *
     * We use a threshold-based approach to updates, with a slack
     * threshold of two -- that is, we update head/tail when the
     * current pointer appears to be two or more steps away from the
     * first/last node. The slack value is hard-wired: a path greater
     * than one is naturally implemented by checking equality of
     * traversal pointers except when the list has only one element,
     * in which case we keep slack threshold at one. Avoiding tracking
     * explicit counts across method calls slightly simplifies an
     * already-messy implementation. Using randomization would
     * probably work better if there were a low-quality dirt-cheap
     * per-thread one available, but even ThreadLocalRandom is too
     * heavy for these purposes.
     *
     * With such a small slack threshold value, it is not worthwhile
     * to augment this with path short-circuiting (i.e., unsplicing
     * interior nodes) except in the case of cancellation/removal (see
     * below).
     *
     * All enqueue/dequeue operations are handled by the single method
     * "xfer" with parameters indicating whether to act as some form
     * of offer, put, poll, take, or transfer (each possibly with
     * timeout). The relative complexity of using one monolithic
     * method outweighs the code bulk and maintenance problems of
     * using separate methods for each case.
     *
     * Operation consists of up to two phases. The first is implemented
     * in method xfer, the second in method awaitMatch.
     *
     * 1. Traverse until matching or appending (method xfer)
     *
     *    Conceptually, we simply traverse all nodes starting from head.
     *    If we encounter an unmatched node of opposite mode, we match
     *    it and return, also updating head (by at least 2 hops) to
     *    one past the matched node (or the node itself if it's the
     *    pinned trailing node).  Traversals also check for the
     *    possibility of falling off-list, in which case they restart.
     *
     *    If the trailing node of the list is reached, a match is not
     *    possible.  If this call was untimed poll or tryTransfer
     *    (argument "how" is NOW), return empty-handed immediately.
     *    Else a new node is CAS-appended.  On successful append, if
     *    this call was ASYNC (e.g. offer), an element was
     *    successfully added to the end of the queue and we return.
     *
     *    Of course, this naive traversal is O(n) when no match is
     *    possible.  We optimize the traversal by maintaining a tail
     *    pointer, which is expected to be "near" the end of the list.
     *    It is only safe to fast-forward to tail (in the presence of
     *    arbitrary concurrent changes) if it is pointing to a node of
     *    the same mode, even if it is dead (in this case no preceding
     *    node could still be matchable by this traversal).  If we
     *    need to restart due to falling off-list, we can again
     *    fast-forward to tail, but only if it has changed since the
     *    last traversal (else we might loop forever).  If tail cannot
     *    be used, traversal starts at head (but in this case we
     *    expect to be able to match near head).  As with head, we
     *    CAS-advance the tail pointer by at least two hops.
     *
     * 2. Await match or cancellation (method awaitMatch)
     *
     *    Wait for another thread to match node; instead cancelling if
     *    the current thread was interrupted or the wait timed out. On
     *    multiprocessors, we use front-of-queue spinning: If a node
     *    appears to be the first unmatched node in the queue, it
     *    spins a bit before blocking. In either case, before blocking
     *    it tries to unsplice any nodes between the current "head"
     *    and the first unmatched node.
     *
     *    Front-of-queue spinning vastly improves performance of
     *    heavily contended queues. And so long as it is relatively
     *    brief and "quiet", spinning does not much impact performance
     *    of less-contended queues.  During spins threads check their
     *    interrupt status and generate a thread-local random number
     *    to decide to occasionally perform a Thread.yield. While
     *    yield has underdefined specs, we assume that it might help,
     *    and will not hurt, in limiting impact of spinning on busy
     *    systems.  We also use smaller (1/2) spins for nodes that are
     *    not known to be front but whose predecessors have not
     *    blocked -- these "chained" spins avoid artifacts of
     *    front-of-queue rules which otherwise lead to alternating
     *    nodes spinning vs blocking. Further, front threads that
     *    represent phase changes (from data to request node or vice
     *    versa) compared to their predecessors receive additional
     *    chained spins, reflecting longer paths typically required to
     *    unblock threads during phase changes.
     *
     *
     * ** Unlinking removed interior nodes **
     *
     * In addition to minimizing garbage retention via self-linking
     * described above, we also unlink removed interior nodes. These
     * may arise due to timed out or interrupted waits, or calls to
     * remove(x) or Iterator.remove.  Normally, given a node that was
     * at one time known to be the predecessor of some node s that is
     * to be removed, we can unsplice s by CASing the next field of
     * its predecessor if it still points to s (otherwise s must
     * already have been removed or is now offlist). But there are two
     * situations in which we cannot guarantee to make node s
     * unreachable in this way: (1) If s is the trailing node of list
     * (i.e., with null next), then it is pinned as the target node
     * for appends, so can only be removed later after other nodes are
     * appended. (2) We cannot necessarily unlink s given a
     * predecessor node that is matched (including the case of being
     * cancelled): the predecessor may already be unspliced, in which
     * case some previous reachable node may still point to s.
     * (For further explanation see Herlihy & Shavit "The Art of
     * Multiprocessor Programming" chapter 9).  Although, in both
     * cases, we can rule out the need for further action if either s
     * or its predecessor are (or can be made to be) at, or fall off
     * from, the head of list.
     *
     * Without taking these into account, it would be possible for an
     * unbounded number of supposedly removed nodes to remain reachable.
     * Situations leading to such buildup are uncommon but can occur
     * in practice; for example when a series of short timed calls to
     * poll repeatedly time out at the trailing node but otherwise
     * never fall off the list because of an untimed call to take() at
     * the front of the queue.
     *
     * When these cases arise, rather than always retraversing the
     * entire list to find an actual predecessor to unlink (which
     * won't help for case (1) anyway), we record a conservative
     * estimate of possible unsplice failures (in "sweepVotes").
     * We trigger a full sweep when the estimate exceeds a threshold
     * ("SWEEP_THRESHOLD") indicating the maximum number of estimated
     * removal failures to tolerate before sweeping through, unlinking
     * cancelled nodes that were not unlinked upon initial removal.
     * We perform sweeps by the thread hitting threshold (rather than
     * background threads or by spreading work to other threads)
     * because in the main contexts in which removal occurs, the
     * caller is timed-out or cancelled, which are not time-critical
     * enough to warrant the overhead that alternatives would impose
     * on other threads.
     *
     * Because the sweepVotes estimate is conservative, and because
     * nodes become unlinked "naturally" as they fall off the head of
     * the queue, and because we allow votes to accumulate even while
     * sweeps are in progress, there are typically significantly fewer
     * such nodes than estimated.  Choice of a threshold value
     * balances the likelihood of wasted effort and contention, versus
     * providing a worst-case bound on retention of interior nodes in
     * quiescent queues. The value defined below was chosen
     * empirically to balance these under various timeout scenarios.
     *
     * Because traversal operations on the linked list of nodes are a
     * natural opportunity to sweep dead nodes, we generally do so,
     * including all the operations that might remove elements as they
     * traverse, such as removeIf and Iterator.remove.  This largely
     * eliminates long chains of dead interior nodes, except from
     * cancelled or timed out blocking operations.
     *
     * Note that we cannot self-link unlinked interior nodes during
     * sweeps. However, the associated garbage chains terminate when
     * some successor ultimately falls off the head of the list and is
     * self-linked.
     */
    
    
    /* Possible values for "how" argument in xfer method. */
    
    private static final int NOW = 0;   // for untimed poll, tryTransfer
    private static final int ASYNC = 1; // for offer, put, add
    private static final int SYNC = 2;  // for transfer, take
    private static final int TIMED = 3; // for timed poll, tryTransfer
    
    
    /**
     * Tolerate this many consecutive dead nodes before CAS-collapsing.
     * Amortized cost of clear() is (1 + 1/MAX_HOPS) CASes per element.
     */
    private static final int MAX_HOPS = 8;
    
    /**
     * The maximum number of estimated removal failures (sweepVotes)
     * to tolerate before sweeping through the queue unlinking
     * cancelled nodes that were not unlinked upon initial
     * removal. See above for explanation. The value must be at least
     * two to avoid useless sweeps when removing trailing nodes.
     */
    static final int SWEEP_THRESHOLD = 32;
    
    /**
     * The number of times to spin (with randomly interspersed calls
     * to Thread.yield) on multiprocessor before blocking when a node
     * is apparently the first waiter in the queue.  See above for
     * explanation. Must be a power of two. The value is empirically
     * derived -- it works pretty well across a variety of processors,
     * numbers of CPUs, and OSes.
     */
    private static final int FRONT_SPINS = 1 << 7;
    
    /**
     * The number of times to spin before blocking when a node is
     * preceded by another node that is apparently spinning.  Also
     * serves as an increment to FRONT_SPINS on phase changes, and as
     * base average frequency for yielding during spins. Must be a
     * power of two.
     */
    private static final int CHAINED_SPINS = FRONT_SPINS >>> 1;
    
    /** True if on multiprocessor */
    private static final boolean MP = Runtime.getRuntime().availableProcessors()>1;
    
    /**
     * A node from which the first live (non-matched) node (if any)
     * can be reached in O(1) time.
     * Invariants:
     * - all live nodes are reachable from head via .next
     * - head != null
     * - (tmp = head).next != tmp || tmp != head
     * Non-invariants:
     * - head may or may not be live
     * - it is permitted for tail to lag behind head, that is, for tail
     * to not be reachable from head!
     */
    transient volatile Node head;   // 队头
    
    /**
     * A node from which the last node on list (that is, the unique
     * node with node.next == null) can be reached in O(1) time.
     * Invariants:
     * - the last node is always reachable from tail via .next
     * - tail != null
     * Non-invariants:
     * - tail may or may not be live
     * - it is permitted for tail to lag behind head, that is, for tail
     * to not be reachable from head!
     * - tail.next may or may not be self-linked.
     */
    private transient volatile Node tail;   // 队尾
    
    /** The number of apparent failures to unsplice cancelled nodes */
    private transient volatile int sweepVotes;
    
    
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle SWEEPVOTES;
    static final VarHandle ITEM;
    static final VarHandle NEXT;
    static final VarHandle WAITER;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(LinkedTransferQueue.class, "head", Node.class);
            TAIL = l.findVarHandle(LinkedTransferQueue.class, "tail", Node.class);
            SWEEPVOTES = l.findVarHandle(LinkedTransferQueue.class, "sweepVotes", int.class);
            ITEM = l.findVarHandle(Node.class, "item", Object.class);
            NEXT = l.findVarHandle(Node.class, "next", Node.class);
            WAITER = l.findVarHandle(Node.class, "waiter", Thread.class);
        } catch(ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
        
        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates an initially empty {@code LinkedTransferQueue}.
     */
    public LinkedTransferQueue() {
        // 初始化队头/队尾指向一个已匹配结点（isData域与item域不匹配）
        head = tail = new Node();
    }
    
    /**
     * Creates a {@code LinkedTransferQueue}
     * initially containing the elements of the given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param c the collection of elements to initially contain
     *
     * @throws NullPointerException if the specified collection or any
     *                              of its elements are null
     */
    // 使用指定容器中的元素初始化队列
    public LinkedTransferQueue(Collection<? extends E> c) {
        Node h = null;
        Node t = null;
        
        for(E e : c) {
            Node newNode = new Node(Objects.requireNonNull(e));
            
            if(h == null) {
                h = t = newNode;
            } else {
                // 尝试将结点t的next域更新为newNode，并将t指向newNode
                t.appendRelaxed(t = newNode);
            }
        }
        
        if(h == null) {
            h = t = new Node();
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
     * @throws NullPointerException if the specified element is null
     */
    // 入队，不会队满，不会阻塞（线程安全）
    public boolean offer(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }
    
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never block or
     * return {@code false}.
     *
     * @return {@code true} (as specified by
     *  {@link BlockingQueue#offer(Object,long,TimeUnit) BlockingQueue.offer})
     * @throws NullPointerException if the specified element is null
     */
    // 入队，不会队满，不会阻塞（线程安全）
    public boolean offer(E e, long timeout, TimeUnit unit) {
        xfer(e, true, ASYNC, 0);
        return true;
    }
    
    
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never block.
     *
     * @throws NullPointerException if the specified element is null
     */
    // 入队，不会队满，不会阻塞（线程安全）
    public void put(E e) {
        xfer(e, true, ASYNC, 0);
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
        xfer(e, true, ASYNC, 0);
        return true;
    }
    
    /*▲ 入队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 出队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 出队，没有互补结点时，返回null（线程安全）
    public E poll() {
        return xfer(null, false, NOW, 0);
    }
    
    // 出队，没有互补结点时，队空时阻塞一段时间，超时后无法出队则返回null（线程安全）
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = xfer(null, false, TIMED, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted()) {
            return e;
        }
        throw new InterruptedException();
    }
    
    
    // 出队，没有互补结点时，线程被阻塞（线程安全）
    public E take() throws InterruptedException {
        E e = xfer(null, false, SYNC, 0);
        if (e != null) {
            return e;
        }
        Thread.interrupted();
        throw new InterruptedException();
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
            for(Node p = head, pred = null; p != null; ) {
                Node q = p.next;
                final Object item;
                if((item = p.item) != null) {
                    if(p.isData) {
                        if(o.equals(item) && p.tryMatch(item, null)) {
                            skipDeadNodes(pred, p, p, q);
                            return true;
                        }
                        pred = p;
                        p = q;
                        continue;
                    }
                } else if(!p.isData) {
                    break;
                }
                
                for(Node c = p; ; q = p.next) {
                    if(q == null || !q.isMatched()) {
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
    
    
    /**
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    // 将队列中所有元素移除，并转移到给定的容器当中
    public int drainTo(Collection<? super E> c) {
        Objects.requireNonNull(c);
        if(c == this) {
            throw new IllegalArgumentException();
        }
        
        int n = 0;
        for(E e; (e = poll()) != null; n++) {
            c.add(e);
        }
        
        return n;
    }
    
    /**
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    // 将队列中前maxElements个元素移除，并转移到给定的容器当中
    public int drainTo(Collection<? super E> c, int maxElements) {
        Objects.requireNonNull(c);
        if(c == this) {
            throw new IllegalArgumentException();
        }
        
        int n = 0;
        for(E e; n<maxElements && (e = poll()) != null; n++) {
            c.add(e);
        }
        
        return n;
    }
    
    /*▲ 出队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取队头元素，线程安全
    public E peek() {
restartFromHead:
        for(; ; ) {
            for(Node p = head; p != null; ) {
                Object item = p.item;
                if(p.isData) {
                    if(item != null) {
                        @SuppressWarnings("unchecked")
                        E e = (E) item;
                        return e;
                    }
                } else if(item == null) {
                    break;
                }
                
                if(p == (p = p.next)) {
                    continue restartFromHead;
                }
            }
            return null;
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
            for(Node p = head, pred = null; p != null; ) {
                Node q = p.next;
                final Object item;
                if((item = p.item) != null) {
                    if(p.isData) {
                        if(o.equals(item)) {
                            return true;
                        }
                        pred = p;
                        p = q;
                        continue;
                    }
                } else if(!p.isData) {
                    break;
                }
                
                for(Node c = p; ; q = p.next) {
                    if(q == null || !q.isMatched()) {
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
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
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
    public Spliterator<E> spliterator() {
        return new LTQSpliterator();
    }
    
    /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断队列中是否包含消费者（阻塞的"取"操作）
    public boolean hasWaitingConsumer() {
restartFromHead:
        for(; ; ) {
            for(Node p = head; p != null; ) {
                Object item = p.item;
                if(p.isData) {
                    if(item != null) {
                        break;
                    }
                } else if(item == null) {
                    return true;
                }
                
                if(p == (p = p.next)) {
                    continue restartFromHead;
                }
            }
            return false;
        }
    }
    
    // 返回队列中包含的消费者数量（阻塞的"取"操作）
    public int getWaitingConsumerCount() {
        return countOfMode(false);
    }
    
    
    /**
     * Returns the number of elements in this queue.  If this queue
     * contains more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these queues, determining the current
     * number of elements requires an O(n) traversal.
     *
     * @return the number of elements in this queue
     */
    // 返回队列中元素数量
    public int size() {
        return countOfMode(true);
    }
    
    /**
     * Always returns {@code Integer.MAX_VALUE} because a
     * {@code LinkedTransferQueue} is not capacity constrained.
     *
     * @return {@code Integer.MAX_VALUE} (as specified by
     * {@link BlockingQueue#remainingCapacity()})
     */
    // 总是返回Integer.MAX_VALUE，因为该结构容量无限
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }
    
    /**
     * Returns {@code true} if this queue contains no elements.
     *
     * @return {@code true} if this queue contains no elements
     */
    // 判断队列是否为空
    public boolean isEmpty() {
        return firstDataNode() == null;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 传递数据 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Transfers the element to a consumer, waiting if necessary to do so.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long, TimeUnit) poll}),
     * else inserts the specified element at the tail of this queue
     * and waits until the element is received by a consumer.
     *
     * @throws NullPointerException if the specified element is null
     */
    // 传递元素，没有消费者时阻塞，直到消费者到达时解除阻塞
    public void transfer(E e) throws InterruptedException {
        if(xfer(e, true, SYNC, 0) != null) {
            Thread.interrupted(); // failure possible only due to interrupt
            throw new InterruptedException();
        }
    }
    
    /**
     * Transfers the element to a waiting consumer immediately, if possible.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long, TimeUnit) poll}),
     * otherwise returning {@code false} without enqueuing the element.
     *
     * @throws NullPointerException if the specified element is null
     */
    /*
     * 尝试传递元素，没有消费者时返回false
     *
     * 如果已有"取"操作在阻塞，则将元素传递给取操作，返回true
     * 否则，直接返回false（不会将数据放在队列中）
     */
    public boolean tryTransfer(E e) {
        return xfer(e, true, NOW, 0) == null;
    }
    
    /**
     * Transfers the element to a consumer if it is possible to do so
     * before the timeout elapses.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long, TimeUnit) poll}),
     * else inserts the specified element at the tail of this queue
     * and waits until the element is received by a consumer,
     * returning {@code false} if the specified wait time elapses
     * before the element can be transferred.
     *
     * @throws NullPointerException if the specified element is null
     */
    /*
     * 尝试传递元素，没有消费者时则阻塞一段时间，超时后无法传递则返回false
     *
     * 如果已有"取"操作在阻塞，则将元素传递给取操作，返回true
     * 否则，将数据结点放在队列中，并阻塞一段时间
     * 阻塞超时后，将数据结点标记为已匹配，并返回false
     */
    public boolean tryTransfer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if(xfer(e, true, TIMED, unit.toNanos(timeout)) == null) {
            return true;
        }
        
        if(!Thread.interrupted()) {
            return false;
        }
        
        throw new InterruptedException();
    }
    
    /**
     * Implements all queuing methods. See above for explanation.
     *
     * @param e        the item or null for take
     * @param haveData true if this is a put, else a take
     * @param how      NOW, ASYNC, SYNC, or TIMED
     * @param nanos    timeout in nanosecs, used only if mode is TIMED
     *
     * @return an item if matched, else e
     *
     * @throws NullPointerException if haveData mode but e is null
     */
    // 队列操作的核心方法
    @SuppressWarnings("unchecked")
    private E xfer(E e, boolean haveData, int how, long nanos) {
        if(haveData && (e == null)) {
            throw new NullPointerException();
        }

restart:
        for(Node s = null, h = null, t = null; ; ) {
            //
            Node p = (t != (t = tail) && t.isData == haveData)
                ? t             // 入队（存入数据）时，从队尾开始
                : (h = head);   // 出队（进行匹配）时，从队头开始
            
            for(; ; ) {
                final Node q;
                final Object item;
                
                // 如果结点类型不匹配，说明可以开始匹配了
                if(p.isData != haveData && haveData == ((item = p.item) == null)) {
                    if(h == null) {
                        h = head;
                    }
                    
                    // 尝试将结点p的item域从item更新到e，且唤醒阻塞线程（可能为null）
                    if(p.tryMatch(item, e)) {
                        /* 至此，结点p从未匹配结点转为匹配结点 */
                        
                        if(h != p) {
                            // 折叠h到p之间的已匹配结点
                            skipDeadNodesNearHead(h, p);
                        }
                        
                        return (E) item;
                    }
                }
                
                /*
                 * 至此，有两种可能：
                 * 1.入队/出队结点与队尾/队头结点类型一致，
                 *   此时，需要将数据存入队列，或者，需要阻塞"取"操作
                 * 2.结点类型不一致，但是尝试更新结点p的item域时失败了，
                 *   这可能是因为别的线程抢先执行了匹配过程
                 */
                
                // 如果p已经是最后一个结点
                if((q = p.next) == null) {
                    // 如果是poll()或tryTransfer()操作，立即返回
                    if(how == NOW) {
                        return e;
                    }
                    
                    // 如果需要存数据，或者需要阻塞"取"操作，则创建排队结点
                    if(s == null) {
                        s = new Node(e);
                    }
                    
                    // 尝试将结点p的next域从null更新到s
                    if(!p.casNext(null, s)) {
                        continue;
                    }
                    
                    // 队尾滞后了，则需要更新tail
                    if(p != t) {
                        casTail(t, s);
                    }
                    
                    // 如果是put/offer/add操作，直接返回
                    if(how == ASYNC) {
                        return e;
                    }
                    
                    /*
                     * 如果是：
                     * SYNC  - take()/transfer()
                     * TIMED - 超时的poll()/tryTransfer()
                     * 将当前线程转入阻塞
                     */
                    return awaitMatch(s, p, e, (how == TIMED), nanos);
                }
                
                /* 如果p不是最后一个结点，继续向后查找 */
                
                // 如果p结点已经被移除，则需要重新执行xfer()方法
                if(p == (p = q)) {
                    continue restart;
                }
            } // for(; ; )
        } // for
    }
    
    /**
     * Spins/yields/blocks until node s is matched or caller gives up.
     *
     * @param s     the waiting node
     * @param pred  the predecessor of s, or null if unknown (the null
     *              case does not occur in any current calls but may in possible
     *              future extensions)
     * @param e     the comparison value for checking match
     * @param timed if true, wait only until timeout elapses
     * @param nanos timeout in nanosecs, used only if timed is true
     *
     * @return matched item, or e if unmatched on interrupt or timeout
     */
    // 使用take()/transfer()或超时的poll()/tryTransfer()时，将当前线程转入阻塞
    private E awaitMatch(Node s, Node pred, E e, boolean timed, long nanos) {
        int spins = -1; // initialized after first item and cancel checks
        
        // 使用超时的poll()，则timed==true
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        
        Thread w = Thread.currentThread();
        
        ThreadLocalRandom randomYields = null; // bound if needed
        
        for(; ; ) {
            final Object item = s.item;
            
            // 如果数据域发生变化，说明该结点已被匹配
            if(item != e) {       // matched
                // 将结点作废（使item域指向自身，且置空waiter域）
                s.forgetContents();           // avoid garbage
                
                @SuppressWarnings("unchecked")
                E itemE = (E) item;
                
                return itemE;
                
                // 如果线程已中断，或已超时
            } else if(w.isInterrupted() || (timed && nanos<=0L)) {
                /* try to cancel and unlink */
                
                // 尝试更新结点s的item域（将未匹配结点设置为已匹配结点）
                if(s.casItem(e, s.isData ? null : s)) {
                    // 取消已匹配结点的链接
                    unsplice(pred, s);
                    return e;
                }
                
                // 初始化自旋次数
            } else if(spins<0) {            // establish spins at/near front
                spins = spinsFor(pred, s.isData);
                
                if(spins>0) {
                    randomYields = ThreadLocalRandom.current();
                }
                
                // 进入自旋
            } else if(spins>0) {             // spin
                --spins;
                
                if(randomYields.nextInt(CHAINED_SPINS) == 0) {
                    Thread.yield();           // occasionally yield
                }
                
                // 设置阻塞线程
            } else if(s.waiter == null) {
                s.waiter = w;                 // request unpark then recheck
                
                // 超时的poll()/tryTransfer()
            } else if(timed) {
                nanos = deadline - System.nanoTime();
                if(nanos>0L) {
                    LockSupport.parkNanos(this, nanos);
                }
                
                // take()
            } else {
                LockSupport.park(this);
            }
        } // for(; ; )
    }
    
    /**
     * Unsplices (now or later) the given deleted/cancelled node with the given predecessor.
     *
     * @param pred a node that was at one time known to be the predecessor of s
     * @param s the node to be unspliced
     */
    // 取消已匹配结点的链接
    final void unsplice(Node pred, Node s) {
        // assert pred != null;
        // assert pred != s;
        // assert s != null;
        // assert s.isMatched();
        // assert (SWEEP_THRESHOLD & (SWEEP_THRESHOLD - 1)) == 0;
        s.waiter = null; // disable signals
        
        /*
         * See above for rationale. Briefly: if pred still points to
         * s, try to unlink s.  If s cannot be unlinked, because it is
         * trailing node or pred might be unlinked, and neither pred
         * nor s are head or offlist, add to sweepVotes, and if enough
         * votes have accumulated, sweep.
         */
        if (pred != null && pred.next == s) {
            Node n = s.next;
            
            if (n == null
                || (n != s && pred.casNext(s, n) && pred.isMatched())) {
                
                for (;;) {               // check if at, or could be, head
                    Node h = head;
                    
                    if (h == pred || h == s) {
                        return;          // at head or list empty
                    }
                    
                    if (!h.isMatched()) {
                        break;
                    }
                    
                    Node hn = h.next;
                    if (hn == null) {
                        return;          // now empty
                    }
                    
                    if (hn != h && casHead(h, hn)) {
                        // 更新结点h的next域为自身
                        h.selfLink();  // advance head
                    }
                }
                
                // sweep every SWEEP_THRESHOLD votes
                if (pred.next != pred && s.next != s // recheck if offlist
                    && (incSweepVotes() & (SWEEP_THRESHOLD - 1)) == 0) {
                    // 从头结点开始遍历，取消已匹配结点的链接
                    sweep();
                }
            }
        }
    }
    
    /**
     * Unlinks matched (typically cancelled) nodes encountered in a traversal from head.
     */
    // 从头结点开始遍历，取消已匹配结点的链接
    private void sweep() {
        for (Node p = head, s, n; p != null && (s = p.next) != null; ) {
            if (!s.isMatched()) {
                // Unmatched nodes are never self-linked
                p = s;
            } else if ((n = s.next) == null) {
                // trailing node is pinned
                break;
            } else if (s == n) {    // stale
                // No need to also check for p == s, since that implies s == n
                p = head;
            } else {
                p.casNext(s, n);
            }
        }
    }
    
    /*▲ 传递数据 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static final long serialVersionUID = -3223113410248163686L;
    
    
    /**
     * Saves this queue to a stream (that is, serializes it).
     *
     * @param s the stream
     *
     * @throws java.io.IOException if an I/O error occurs
     * @serialData All of the elements (each an {@code E}) in
     * the proper order, followed by a null
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        for(E e : this)
            s.writeObject(e);
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
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        
        // Read in elements until trailing null sentinel found
        Node h = null, t = null;
        for(Object item; (item = s.readObject()) != null; ) {
            Node newNode = new Node(item);
            if(h == null) {
                h = t = newNode;
            } else {
                t = newNode;
                t.appendRelaxed(t);
            }
        }
        if(h == null)
            h = t = new Node();
        head = h;
        tail = t;
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    public String toString() {
        String[] a = null;

restartFromHead:
        for(; ; ) {
            int charLength = 0;
            int size = 0;
            for(Node p = head; p != null; ) {
                Object item = p.item;
                if(p.isData) {
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
                } else if(item == null) {
                    break;
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
    
    
    
    // 尝试将队列的tail域从cmp更新到val
    private boolean casTail(Node cmp, Node val) {
        // assert cmp != null;
        // assert val != null;
        return TAIL.compareAndSet(this, cmp, val);
    }
    
    // 尝试将队列的head域从cmp更新到val
    private boolean casHead(Node cmp, Node val) {
        return HEAD.compareAndSet(this, cmp, val);
    }
    
    /** Atomic version of ++sweepVotes. */
    private int incSweepVotes() {
        return (int) SWEEPVOTES.getAndAdd(this, 1) + 1;
    }
    
    /**
     * Tries to CAS pred.next (or head, if pred is null) from c to p.
     * Caller must ensure that we're not unlinking the trailing node.
     */
    private boolean tryCasSuccessor(Node pred, Node c, Node p) {
        // assert p != null;
        // assert c.isData != (c.item != null);
        // assert c != p;
        if(pred != null) {
            return pred.casNext(c, p);
        }
        
        if(casHead(c, p)) {
            // 更新结点c的next域为自身
            c.selfLink();
            return true;
        }
        
        return false;
    }
    
    /**
     * Collapses dead (matched) nodes between pred and q.
     *
     * @param pred the last known live node, or null if none
     * @param c    the first dead node
     * @param p    the last dead node
     * @param q    p.next: the next live node, or null if at end
     *
     * @return pred if pred still alive and CAS succeeded; else p
     */
    private Node skipDeadNodes(Node pred, Node c, Node p, Node q) {
        // assert pred != c;
        // assert p != q;
        // assert c.isMatched();
        // assert p.isMatched();
        
        if(q == null) {
            // Never unlink trailing node.
            if(c == p) {
                return pred;
            }
            
            q = p;
        }
        
        if(!tryCasSuccessor(pred, c, q)){
            return p;
        }
        
        if(pred == null){
            return pred;
        }
        
        // 判断当前结点是否为已匹配结点（isData域与item域不匹配）
        return !pred.isMatched() ? pred : p;
    }
    
    /**
     * Collapses dead (matched) nodes from h (which was once head) to p.
     * Caller ensures all nodes from h up to and including p are dead.
     */
    // 折叠h到p之间的已匹配结点
    private void skipDeadNodesNearHead(Node h, Node p) {
        // assert h != null;
        // assert h != p;
        // assert p.isMatched();
        
        for(; ; ) {
            final Node q;
            
            // 如果p已经是最后一个结点
            if((q = p.next) == null) {
                break;
            }
            
            // 如果结点q还未匹配（isData域与item域匹配）
            if(!q.isMatched()) {
                p = q;
                break;
            }
            
            // 如果p的next域指向了自身
            if(p == (p = q)) {
                return;
            }
        }
        
        // 尝试将队列的head域从h更新到p
        if(casHead(h, p)) {
            // 更新结点h的next域为自身
            h.selfLink();
        }
    }
    
    /**
     * Returns spin/yield value for a node with given predecessor and data mode. See above for explanation.
     */
    private static int spinsFor(Node pred, boolean haveData) {
        if(MP && pred != null) {
            if(pred.isData != haveData) {
                // phase change
                return FRONT_SPINS + CHAINED_SPINS;
            }
            
            if(pred.isMatched()) {
                // probably at front
                return FRONT_SPINS;
            }
            
            if(pred.waiter == null) {
                // pred apparently spinning
                return CHAINED_SPINS;
            }
        }
        return 0;
    }
    
    /**
     * Returns the first unmatched data node, or null if none.
     * Callers must recheck if the returned node is unmatched
     * before using.
     */
    final Node firstDataNode() {
        Node first = null;
restartFromHead:
        for(; ; ) {
            Node h = head, p = h;
            while(p != null) {
                if(p.item != null) {
                    if(p.isData) {
                        first = p;
                        break;
                    }
                } else if(!p.isData) {
                    break;
                }
                
                final Node q;
                if((q = p.next) == null) {
                    break;
                }
                
                if(p == (p = q)) {
                    continue restartFromHead;
                }
            }
            if(p != h && casHead(h, p)) {
                // 尝试将结点h的next域更新为自身
                h.selfLink();
            }
            
            return first;
        }
    }
    
    /**
     * Traverses and counts unmatched nodes of the given mode.
     * Used by methods size and getWaitingConsumerCount.
     */
    private int countOfMode(boolean data) {
restartFromHead:
        for(; ; ) {
            int count = 0;
            for(Node p = head; p != null; ) {
                // 该结点还未匹配
                if(!p.isMatched()) {
                    if(p.isData != data) {
                        return 0;
                    }
                    
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
     * Runs action on each element found during a traversal starting at p.
     * If p is null, the action is not run.
     */
    @SuppressWarnings("unchecked")
    void forEachFrom(Consumer<? super E> action, Node p) {
        for (Node pred = null; p != null; ) {
            Node q = p.next;
            final Object item;
            if ((item = p.item) != null) {
                if (p.isData) {
                    action.accept((E) item);
                    pred = p; p = q; continue;
                }
            }
            else if (!p.isData)
                break;
            for (Node c = p;; q = p.next) {
                if (q == null || !q.isMatched()) {
                    pred = skipDeadNodes(pred, c, p, q); p = q; break;
                }
                if (p == (p = q)) { pred = null; p = head; break; }
            }
        }
    }
    
    /** Implementation of bulk remove methods. */
    @SuppressWarnings("unchecked")
    private boolean bulkRemove(Predicate<? super E> filter) {
        boolean removed = false;
restartFromHead: for (;;) {
            int hops = MAX_HOPS;
            // c will be CASed to collapse intervening dead nodes between
            // pred (or head if null) and p.
            for (Node p = head, c = p, pred = null, q; p != null; p = q) {
                q = p.next;
                final Object item; boolean pAlive;
                if (pAlive = ((item = p.item) != null && p.isData)) {
                    if (filter.test((E) item)) {
                        if (p.tryMatch(item, null))
                            removed = true;
                        pAlive = false;
                    }
                }
                else if (!p.isData && item == null)
                    break;
                if (pAlive || q == null || --hops == 0) {
                    // p might already be self-linked here, but if so:
                    // - CASing head will surely fail
                    // - CASing pred's next will be useless but harmless.
                    if ((c != p && !tryCasSuccessor(pred, c, c = p))
                        || pAlive) {
                        // if CAS failed or alive, abandon old pred
                        hops = MAX_HOPS;
                        pred = p;
                        c = q;
                    }
                } else if (p == q)
                    continue restartFromHead;
            }
            return removed;
        }
    }
    
    private Object[] toArrayInternal(Object[] a) {
        Object[] x = a;
restartFromHead:
        for(; ; ) {
            int size = 0;
            for(Node p = head; p != null; ) {
                Object item = p.item;
                if(p.isData) {
                    if(item != null) {
                        if(x == null)
                            x = new Object[4];
                        else if(size == x.length)
                            x = Arrays.copyOf(x, 2 * (size + 4));
                        x[size++] = item;
                    }
                } else if(item == null)
                    break;
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
    
    
    
    
    
    
    /**
     * Queue nodes. Uses Object, not E, for items to allow forgetting
     * them after use.  Writes that are intrinsically ordered wrt
     * other accesses or CASes use simple relaxed forms.
     */
    // 队列结点
    static final class Node {
        private static final long serialVersionUID = -3375979862319811754L;
        
        // 指向下一个结点
        volatile Node next;
        
        // 判断当前结点是否为数据结点
        final boolean isData;   // false if this is a request node
        
        // 数据域（生产者存数据，消费者存null。取消操作时指向自身）
        volatile Object item;   // initially non-null if isData; CASed to match
        
        // 被阻塞的操作/线程/结点
        volatile Thread waiter; // null when not waiting for a match
        
        /** Constructs a (matched data) dummy node. */
        // 初始化一个"已匹配"结点
        Node() {
            isData = true;
        }
        
        /**
         * Constructs a data node holding item if item is non-null,
         * else a request node.  Uses relaxed write because item can
         * only be seen after piggy-backing publication via CAS.
         */
        // 初始化一个生产者结点或消费者结点
        Node(Object item) {
            ITEM.set(this, item);
            isData = (item != null);
        }
        
        // 尝试将当前结点的next域从cmp更新到val
        final boolean casNext(Node cmp, Node val) {
            // assert val != null;
            return NEXT.compareAndSet(this, cmp, val);
        }
        
        /**
         * Links node to itself to avoid garbage retention.  Called
         * only after CASing head field, so uses relaxed write.
         */
        // 尝试将当前结点的next域更新为自身
        final void selfLink() {
            // assert isMatched();
            NEXT.setRelease(this, this);
        }
        
        // 尝试将结点next的next域更新为自身
        final void appendRelaxed(Node next) {
            // assert next != null;
            // assert this.next == null;
            NEXT.set(this, next);
        }
        
        // 尝试将当前结点的item域从cmp更新到val
        final boolean casItem(Object cmp, Object val) {
            // assert isData == (cmp != null);
            // assert isData == (val == null);
            // assert !(cmp instanceof Node);
            return ITEM.compareAndSet(this, cmp, val);
        }
        
        /**
         * Sets item (of a request node) to self and waiter to null,
         * to avoid garbage retention after matching or cancelling.
         * Uses relaxed writes because order is already constrained in
         * the only calling contexts: item is forgotten only after
         * volatile/atomic mechanics that extract items, and visitors
         * of request nodes only ever check whether item is null.
         * Similarly, clearing waiter follows either CAS or return
         * from park (if ever parked; else we don't care).
         */
        // 将结点作废（使item域指向自身，且置空waiter域）
        final void forgetContents() {
            // assert isMatched();
            if(!isData) {
                ITEM.set(this, this);
            }
            WAITER.set(this, null);
        }
        
        /**
         * Returns true if this node has been matched, including the
         * case of artificial matches due to cancellation.
         */
        // 判断当前结点是否为已匹配结点（isData域与item域不匹配）
        final boolean isMatched() {
            return isData == (item == null);
        }
        
        /** Tries to CAS-match this node; if successful, wakes waiter. */
        // 尝试将当前结点的item域从cmp更新到val，且唤醒阻塞线程（可能为null）
        final boolean tryMatch(Object cmp, Object val) {
            // 尝试将当前结点的item域从cmp更新到val
            if(casItem(cmp, val)) {
                LockSupport.unpark(waiter);
                return true;
            }
            return false;
        }
        
        /**
         * Returns true if a node with the given mode cannot be
         * appended to this node because this node is unmatched and
         * has opposite data mode.
         */
        final boolean cannotPrecede(boolean haveData) {
            boolean d = isData;
            return d != haveData && d != (item == null);
        }
    }
    
    /**
     * Weakly-consistent iterator.
     *
     * Lazily updated ancestor is expected to be amortized O(1) remove(),
     * but O(n) in the worst case, when lastRet is concurrently deleted.
     */
    // 用于当前队列的外部迭代器
    final class Itr implements Iterator<E> {
        private Node nextNode;   // next node to return item for
        private E nextItem;      // the corresponding item
        private Node lastRet;    // last returned node, to support remove
        private Node ancestor;   // Helps unlink lastRet on remove()
        
        Itr() {
            advance(null);
        }
        
        public final boolean hasNext() {
            return nextNode != null;
        }
        
        public final E next() {
            final Node p;
            if((p = nextNode) == null) {
                throw new NoSuchElementException();
            }
            E e = nextItem;
            advance(lastRet = p);
            return e;
        }
        
        public final void remove() {
            final Node lastRet = this.lastRet;
            if(lastRet == null) {
                throw new IllegalStateException();
            }
            this.lastRet = null;
            if(lastRet.item == null) {
                // already deleted?
                return;
            }
            // Advance ancestor, collapsing intervening dead nodes
            Node pred = ancestor;
            for(Node p = (pred == null) ? head : pred.next, c = p, q; p != null; ) {
                if(p == lastRet) {
                    final Object item;
                    if((item = p.item) != null) {
                        p.tryMatch(item, null);
                    }
                    if((q = p.next) == null) {
                        q = p;
                    }
                    if(c != q) {
                        tryCasSuccessor(pred, c, q);
                    }
                    ancestor = pred;
                    return;
                }
                final Object item;
                final boolean pAlive;
                if(pAlive = ((item = p.item) != null && p.isData)) {
                    // exceptionally, nothing to do
                } else if(!p.isData && item == null)
                    break;
                if((c != p && !tryCasSuccessor(pred, c, c = p)) || pAlive) {
                    pred = p;
                    c = p = p.next;
                } else if(p == (p = p.next)) {
                    pred = null;
                    c = p = head;
                }
            }
            // traversal failed to find lastRet; must have been deleted;
            // leave ancestor at original location to avoid overshoot;
            // better luck next time!
            
            // assert lastRet.isMatched();
        }
        
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Node q = null;
            for(Node p; (p = nextNode) != null; advance(q = p)) {
                action.accept(nextItem);
            }
            if(q != null) {
                lastRet = q;
            }
        }
        
        /**
         * Moves to next node after pred, or first node if pred null.
         */
        @SuppressWarnings("unchecked")
        private void advance(Node pred) {
            for(Node p = (pred == null) ? head : pred.next, c = p; p != null; ) {
                final Object item;
                if((item = p.item) != null && p.isData) {
                    nextNode = p;
                    nextItem = (E) item;
                    if(c != p) {
                        tryCasSuccessor(pred, c, p);
                    }
                    return;
                } else if(!p.isData && item == null)
                    break;
                if(c != p && !tryCasSuccessor(pred, c, c = p)) {
                    pred = p;
                    c = p = p.next;
                } else if(p == (p = p.next)) {
                    pred = null;
                    c = p = head;
                }
            }
            nextItem = null;
            nextNode = null;
        }
    }
    
    /** A customized variant of Spliterators.IteratorSpliterator */
    // 描述此队列中元素的Spliterator
    final class LTQSpliterator implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        Node current;       // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes
        
        LTQSpliterator() {
        }
        
        public Spliterator<E> trySplit() {
            Node p, q;
            
            if((p = current()) == null || (q = p.next) == null) {
                return null;
            }
            
            int i = 0, n = batch = Math.min(batch + 1, MAX_BATCH);
            
            Object[] a = null;
            
            do {
                final Object item = p.item;
                if(p.isData) {
                    if(item != null) {
                        if(a == null) {
                            a = new Object[n];
                        }
                        a[i++] = item;
                    }
                } else if(item == null) {
                    p = null;
                    break;
                }
                if(p == (p = q)) {
                    p = firstDataNode();
                }
            } while(p != null && (q = p.next) != null && i<n);
            
            setCurrent(p);
            
            return (i == 0)
                ? null
                : Spliterators.spliterator(a, 0, i, (Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT));
        }
        
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final Node p;
            if((p = current()) != null) {
                current = null;
                exhausted = true;
                forEachFrom(action, p);
            }
        }
        
        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Node p;
            if((p = current()) != null) {
                E e = null;
                do {
                    final Object item = p.item;
                    final boolean isData = p.isData;
                    if(p == (p = p.next)) {
                        p = head;
                    }
                    
                    if(isData) {
                        if(item != null) {
                            e = (E) item;
                            break;
                        }
                    } else if(item == null) {
                        p = null;
                    }
                    
                } while(p != null);
                
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
        
        private void setCurrent(Node p) {
            if((current = p) == null) {
                exhausted = true;
            }
        }
        
        private Node current() {
            Node p;
            if((p = current) == null && !exhausted) {
                setCurrent(p = firstDataNode());
            }
            return p;
        }
    }
    
}
