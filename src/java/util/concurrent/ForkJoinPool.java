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

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinTask.AdaptedRunnable;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

/**
 * An {@link ExecutorService} for running {@link ForkJoinTask}s.
 * A {@code ForkJoinPool} provides the entry point for submissions
 * from non-{@code ForkJoinTask} clients, as well as management and
 * monitoring operations.
 *
 * <p>A {@code ForkJoinPool} differs from other kinds of {@link
 * ExecutorService} mainly by virtue of employing
 * <em>work-stealing</em>: all threads in the pool attempt to find and
 * execute tasks submitted to the pool and/or created by other active
 * tasks (eventually blocking waiting for work if none exist). This
 * enables efficient processing when most tasks spawn other subtasks
 * (as do most {@code ForkJoinTask}s), as well as when many small
 * tasks are submitted to the pool from external clients.  Especially
 * when setting <em>asyncMode</em> to true in constructors, {@code
 * ForkJoinPool}s may also be appropriate for use with event-style
 * tasks that are never joined. All worker threads are initialized
 * with {@link Thread#isDaemon} set {@code true}.
 *
 * <p>A static {@link #commonPool()} is available and appropriate for
 * most applications. The common pool is used by any ForkJoinTask that
 * is not explicitly submitted to a specified pool. Using the common
 * pool normally reduces resource usage (its threads are slowly
 * reclaimed during periods of non-use, and reinstated upon subsequent
 * use).
 *
 * <p>For applications that require separate or custom pools, a {@code
 * ForkJoinPool} may be constructed with a given target parallelism
 * level; by default, equal to the number of available processors.
 * The pool attempts to maintain enough active (or available) threads
 * by dynamically adding, suspending, or resuming internal worker
 * threads, even if some tasks are stalled waiting to join others.
 * However, no such adjustments are guaranteed in the face of blocked
 * I/O or other unmanaged synchronization. The nested {@link
 * ManagedBlocker} interface enables extension of the kinds of
 * synchronization accommodated. The default policies may be
 * overridden using a constructor with parameters corresponding to
 * those documented in class {@link ThreadPoolExecutor}.
 *
 * <p>In addition to execution and lifecycle control methods, this
 * class provides status check methods (for example
 * {@link #getStealCount}) that are intended to aid in developing,
 * tuning, and monitoring fork/join applications. Also, method
 * {@link #toString} returns indications of pool state in a
 * convenient form for informal monitoring.
 *
 * <p>As is the case with other ExecutorServices, there are three
 * main task execution methods summarized in the following table.
 * These are designed to be used primarily by clients not already
 * engaged in fork/join computations in the current pool.  The main
 * forms of these methods accept instances of {@code ForkJoinTask},
 * but overloaded forms also allow mixed execution of plain {@code
 * Runnable}- or {@code Callable}- based activities as well.  However,
 * tasks that are already executing in a pool should normally instead
 * use the within-computation forms listed in the table unless using
 * async event-style tasks that are not usually joined, in which case
 * there is little difference among choice of methods.
 *
 * <table class="plain">
 * <caption>Summary of task execution methods</caption>
 *  <tr>
 *    <td></td>
 *    <th scope="col"> Call from non-fork/join clients</th>
 *    <th scope="col"> Call from within fork/join computations</th>
 *  </tr>
 *  <tr>
 *    <th scope="row" style="text-align:left"> Arrange async execution</th>
 *    <td> {@link #execute(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#fork}</td>
 *  </tr>
 *  <tr>
 *    <th scope="row" style="text-align:left"> Await and obtain result</th>
 *    <td> {@link #invoke(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#invoke}</td>
 *  </tr>
 *  <tr>
 *    <th scope="row" style="text-align:left"> Arrange exec and obtain Future</th>
 *    <td> {@link #submit(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#fork} (ForkJoinTasks <em>are</em> Futures)</td>
 *  </tr>
 * </table>
 *
 * <p>The parameters used to construct the common pool may be controlled by
 * setting the following {@linkplain System#getProperty system properties}:
 * <ul>
 * <li>{@code java.util.concurrent.ForkJoinPool.common.parallelism}
 * - the parallelism level, a non-negative integer
 * <li>{@code java.util.concurrent.ForkJoinPool.common.threadFactory}
 * - the class name of a {@link ForkJoinWorkerThreadFactory}.
 * The {@linkplain ClassLoader#getSystemClassLoader() system class loader}
 * is used to load this class.
 * <li>{@code java.util.concurrent.ForkJoinPool.common.exceptionHandler}
 * - the class name of a {@link UncaughtExceptionHandler}.
 * The {@linkplain ClassLoader#getSystemClassLoader() system class loader}
 * is used to load this class.
 * <li>{@code java.util.concurrent.ForkJoinPool.common.maximumSpares}
 * - the maximum number of allowed extra threads to maintain target
 * parallelism (default 256).
 * </ul>
 * If no thread factory is supplied via a system property, then the
 * common pool uses a factory that uses the system class loader as the
 * {@linkplain Thread#getContextClassLoader() thread context class loader}.
 * In addition, if a {@link SecurityManager} is present, then
 * the common pool uses a factory supplying threads that have no
 * {@link Permissions} enabled.
 *
 * Upon any error in establishing these settings, default parameters
 * are used. It is possible to disable or limit the use of threads in
 * the common pool by setting the parallelism property to zero, and/or
 * using a factory that may return {@code null}. However doing so may
 * cause unjoined tasks to never be executed.
 *
 * <p><b>Implementation notes</b>: This implementation restricts the
 * maximum number of running threads to 32767. Attempts to create
 * pools with greater than the maximum number result in
 * {@code IllegalArgumentException}.
 *
 * <p>This implementation rejects submitted tasks (that is, by throwing
 * {@link RejectedExecutionException}) only when the pool is shut down
 * or internal resources have been exhausted.
 *
 * @since 1.7
 * @author Doug Lea
 */
/*
 * 工作池/任务池，负责并发任务的调度，以高效完成任务
 *
 * 【独立工作池】：由用户构造的ForkJoinPool
 * 【共享工作池】：ForkJoinPool类内置的一个对象：common，由所有ForkJoinPool共享
 *
 * 推荐直接使用【共享工作池】
 */
public class ForkJoinPool extends AbstractExecutorService {
    
    /*
     * Implementation Overview
     *
     * This class and its nested classes provide the main
     * functionality and control for a set of worker threads:
     * Submissions from non-FJ threads enter into submission queues.
     * Workers take these tasks and typically split them into subtasks
     * that may be stolen by other workers. Work-stealing based on
     * randomized scans generally leads to better throughput than
     * "work dealing" in which producers assign tasks to idle threads,
     * in part because threads that have finished other tasks before
     * the signalled thread wakes up (which can be a long time) can
     * take the task instead.  Preference rules give first priority to
     * processing tasks from their own queues (LIFO or FIFO, depending
     * on mode), then to randomized FIFO steals of tasks in other
     * queues.  This framework began as vehicle for supporting
     * tree-structured parallelism using work-stealing.  Over time,
     * its scalability advantages led to extensions and changes to
     * better support more diverse usage contexts.  Because most
     * internal methods and nested classes are interrelated, their
     * main rationale and descriptions are presented here; individual
     * methods and nested classes contain only brief comments about
     * details.
     *
     * WorkQueues
     * ==========
     *
     * Most operations occur within work-stealing queues (in nested
     * class WorkQueue).  These are special forms of Deques that
     * support only three of the four possible end-operations -- push,
     * pop, and poll (aka steal), under the further constraints that
     * push and pop are called only from the owning thread (or, as
     * extended here, under a lock), while poll may be called from
     * other threads.  (If you are unfamiliar with them, you probably
     * want to read Herlihy and Shavit's book "The Art of
     * Multiprocessor programming", chapter 16 describing these in
     * more detail before proceeding.)  The main work-stealing queue
     * design is roughly similar to those in the papers "Dynamic
     * Circular Work-Stealing Deque" by Chase and Lev, SPAA 2005
     * (http://research.sun.com/scalable/pubs/index.html) and
     * "Idempotent work stealing" by Michael, Saraswat, and Vechev,
     * PPoPP 2009 (http://portal.acm.org/citation.cfm?id=1504186).
     * The main differences ultimately stem from GC requirements that
     * we null out taken slots as soon as we can, to maintain as small
     * a footprint as possible even in programs generating huge
     * numbers of tasks. To accomplish this, we shift the CAS
     * arbitrating pop vs poll (steal) from being on the indices
     * ("base" and "top") to the slots themselves.
     *
     * Adding tasks then takes the form of a classic array push(task)
     * in a circular buffer:
     *    q.array[q.top++ % length] = task;
     *
     * (The actual code needs to null-check and size-check the array,
     * uses masking, not mod, for indexing a power-of-two-sized array,
     * adds a release fence for publication, and possibly signals
     * waiting workers to start scanning -- see below.)  Both a
     * successful pop and poll mainly entail a CAS of a slot from
     * non-null to null.
     *
     * The pop operation (always performed by owner) is:
     *   if ((the task at top slot is not null) and
     *        (CAS slot to null))
     *           decrement top and return task;
     *
     * And the poll operation (usually by a stealer) is
     *    if ((the task at base slot is not null) and
     *        (CAS slot to null))
     *           increment base and return task;
     *
     * There are several variants of each of these. Most uses occur
     * within operations that also interleave contention or emptiness
     * tracking or inspection of elements before extracting them, so
     * must interleave these with the above code. When performed by
     * owner, getAndSet is used instead of CAS (see for example method
     * nextLocalTask) which is usually more efficient, and possible
     * because the top index cannot independently change during the
     * operation.
     *
     * Memory ordering.  See "Correct and Efficient Work-Stealing for
     * Weak Memory Models" by Le, Pop, Cohen, and Nardelli, PPoPP 2013
     * (http://www.di.ens.fr/~zappa/readings/ppopp13.pdf) for an
     * analysis of memory ordering requirements in work-stealing
     * algorithms similar to (but different than) the one used here.
     * Extracting tasks in array slots via (fully fenced) CAS provides
     * primary synchronization. The base and top indices imprecisely
     * guide where to extract from. We do not usually require strict
     * orderings of array and index updates. Many index accesses use
     * plain mode, with ordering constrained by surrounding context
     * (usually with respect to element CASes or the two WorkQueue
     * volatile fields source and phase). When not otherwise already
     * constrained, reads of "base" by queue owners use acquire-mode,
     * and some externally callable methods preface accesses with
     * acquire fences.  Additionally, to ensure that index update
     * writes are not coalesced or postponed in loops etc, "opaque"
     * mode is used in a few cases where timely writes are not
     * otherwise ensured. The "locked" versions of push- and pop-
     * based methods for shared queues differ from owned versions
     * because locking already forces some of the ordering.
     *
     * Because indices and slot contents cannot always be consistent,
     * a check that base == top indicates (momentary) emptiness, but
     * otherwise may err on the side of possibly making the queue
     * appear nonempty when a push, pop, or poll have not fully
     * committed, or making it appear empty when an update of top has
     * not yet been visibly written.  (Method isEmpty() checks the
     * case of a partially completed removal of the last element.)
     * Because of this, the poll operation, considered individually,
     * is not wait-free. One thief cannot successfully continue until
     * another in-progress one (or, if previously empty, a push)
     * visibly completes.  This can stall threads when required to
     * consume from a given queue (see method poll()).  However, in
     * the aggregate, we ensure at least probabilistic
     * non-blockingness.  If an attempted steal fails, a scanning
     * thief chooses a different random victim target to try next. So,
     * in order for one thief to progress, it suffices for any
     * in-progress poll or new push on any empty queue to complete.
     *
     * This approach also enables support of a user mode in which
     * local task processing is in FIFO, not LIFO order, simply by
     * using poll rather than pop.  This can be useful in
     * message-passing frameworks in which tasks are never joined.
     *
     * WorkQueues are also used in a similar way for tasks submitted
     * to the pool. We cannot mix these tasks in the same queues used
     * by workers. Instead, we randomly associate submission queues
     * with submitting threads, using a form of hashing.  The
     * ThreadLocalRandom probe value serves as a hash code for
     * choosing existing queues, and may be randomly repositioned upon
     * contention with other submitters.  In essence, submitters act
     * like workers except that they are restricted to executing local
     * tasks that they submitted.  Insertion of tasks in shared mode
     * requires a lock but we use only a simple spinlock (using field
     * phase), because submitters encountering a busy queue move to a
     * different position to use or create other queues -- they block
     * only when creating and registering new queues. Because it is
     * used only as a spinlock, unlocking requires only a "releasing"
     * store (using setRelease) unless otherwise signalling.
     *
     * Management
     * ==========
     *
     * The main throughput advantages of work-stealing stem from
     * decentralized control -- workers mostly take tasks from
     * themselves or each other, at rates that can exceed a billion
     * per second.  The pool itself creates, activates (enables
     * scanning for and running tasks), deactivates, blocks, and
     * terminates threads, all with minimal central information.
     * There are only a few properties that we can globally track or
     * maintain, so we pack them into a small number of variables,
     * often maintaining atomicity without blocking or locking.
     * Nearly all essentially atomic control state is held in a few
     * volatile variables that are by far most often read (not
     * written) as status and consistency checks. We pack as much
     * information into them as we can.
     *
     * Field "ctl" contains 64 bits holding information needed to
     * atomically decide to add, enqueue (on an event queue), and
     * dequeue and release workers.  To enable this packing, we
     * restrict maximum parallelism to (1<<15)-1 (which is far in
     * excess of normal operating range) to allow ids, counts, and
     * their negations (used for thresholding) to fit into 16bit
     * subfields.
     *
     * Field "mode" holds configuration parameters as well as lifetime
     * status, atomically and monotonically setting SHUTDOWN, STOP,
     * and finally TERMINATED bits.
     *
     * Field "workQueues" holds references to WorkQueues.  It is
     * updated (only during worker creation and termination) under
     * lock (using field workerNamePrefix as lock), but is otherwise
     * concurrently readable, and accessed directly. We also ensure
     * that uses of the array reference itself never become too stale
     * in case of resizing, by arranging that (re-)reads are separated
     * by at least one acquiring read access.  To simplify index-based
     * operations, the array size is always a power of two, and all
     * readers must tolerate null slots. Worker queues are at odd
     * indices. Shared (submission) queues are at even indices, up to
     * a maximum of 64 slots, to limit growth even if the array needs
     * to expand to add more workers. Grouping them together in this
     * way simplifies and speeds up task scanning.
     *
     * All worker thread creation is on-demand, triggered by task
     * submissions, replacement of terminated workers, and/or
     * compensation for blocked workers. However, all other support
     * code is set up to work with other policies.  To ensure that we
     * do not hold on to worker references that would prevent GC, all
     * accesses to workQueues are via indices into the workQueues
     * array (which is one source of some of the messy code
     * constructions here). In essence, the workQueues array serves as
     * a weak reference mechanism. Thus for example the stack top
     * subfield of ctl stores indices, not references.
     *
     * Queuing Idle Workers. Unlike HPC work-stealing frameworks, we
     * cannot let workers spin indefinitely scanning for tasks when
     * none can be found immediately, and we cannot start/resume
     * workers unless there appear to be tasks available.  On the
     * other hand, we must quickly prod them into action when new
     * tasks are submitted or generated. In many usages, ramp-up time
     * is the main limiting factor in overall performance, which is
     * compounded at program start-up by JIT compilation and
     * allocation. So we streamline this as much as possible.
     *
     * The "ctl" field atomically maintains total worker and
     * "released" worker counts, plus the head of the available worker
     * queue (actually stack, represented by the lower 32bit subfield
     * of ctl).  Released workers are those known to be scanning for
     * and/or running tasks. Unreleased ("available") workers are
     * recorded in the ctl stack. These workers are made available for
     * signalling by enqueuing in ctl (see method runWorker).  The
     * "queue" is a form of Treiber stack. This is ideal for
     * activating threads in most-recently used order, and improves
     * performance and locality, outweighing the disadvantages of
     * being prone to contention and inability to release a worker
     * unless it is topmost on stack.  To avoid missed signal problems
     * inherent in any wait/signal design, available workers rescan
     * for (and if found run) tasks after enqueuing.  Normally their
     * release status will be updated while doing so, but the released
     * worker ctl count may underestimate the number of active
     * threads. (However, it is still possible to determine quiescence
     * via a validation traversal -- see isQuiescent).  After an
     * unsuccessful rescan, available workers are blocked until
     * signalled (see signalWork).  The top stack state holds the
     * value of the "phase" field of the worker: its index and status,
     * plus a version counter that, in addition to the count subfields
     * (also serving as version stamps) provide protection against
     * Treiber stack ABA effects.
     *
     * Creating workers. To create a worker, we pre-increment counts
     * (serving as a reservation), and attempt to construct a
     * ForkJoinWorkerThread via its factory. Upon construction, the
     * new thread invokes registerWorker, where it constructs a
     * WorkQueue and is assigned an index in the workQueues array
     * (expanding the array if necessary). The thread is then started.
     * Upon any exception across these steps, or null return from
     * factory, deregisterWorker adjusts counts and records
     * accordingly.  If a null return, the pool continues running with
     * fewer than the target number workers. If exceptional, the
     * exception is propagated, generally to some external caller.
     * Worker index assignment avoids the bias in scanning that would
     * occur if entries were sequentially packed starting at the front
     * of the workQueues array. We treat the array as a simple
     * power-of-two hash table, expanding as needed. The seedIndex
     * increment ensures no collisions until a resize is needed or a
     * worker is deregistered and replaced, and thereafter keeps
     * probability of collision low. We cannot use
     * ThreadLocalRandom.getProbe() for similar purposes here because
     * the thread has not started yet, but do so for creating
     * submission queues for existing external threads (see
     * externalPush).
     *
     * WorkQueue field "phase" is used by both workers and the pool to
     * manage and track whether a worker is UNSIGNALLED (possibly
     * blocked waiting for a signal).  When a worker is enqueued its
     * phase field is set. Note that phase field updates lag queue CAS
     * releases so usage requires care -- seeing a negative phase does
     * not guarantee that the worker is available. When queued, the
     * lower 16 bits of scanState must hold its pool index. So we
     * place the index there upon initialization and otherwise keep it
     * there or restore it when necessary.
     *
     * The ctl field also serves as the basis for memory
     * synchronization surrounding activation. This uses a more
     * efficient version of a Dekker-like rule that task producers and
     * consumers sync with each other by both writing/CASing ctl (even
     * if to its current value).  This would be extremely costly. So
     * we relax it in several ways: (1) Producers only signal when
     * their queue is possibly empty at some point during a push
     * operation (which requires conservatively checking size zero or
     * one to cover races). (2) Other workers propagate this signal
     * when they find tasks in a queue with size greater than one. (3)
     * Workers only enqueue after scanning (see below) and not finding
     * any tasks.  (4) Rather than CASing ctl to its current value in
     * the common case where no action is required, we reduce write
     * contention by equivalently prefacing signalWork when called by
     * an external task producer using a memory access with
     * full-volatile semantics or a "fullFence".
     *
     * Almost always, too many signals are issued, in part because a
     * task producer cannot tell if some existing worker is in the
     * midst of finishing one task (or already scanning) and ready to
     * take another without being signalled. So the producer might
     * instead activate a different worker that does not find any
     * work, and then inactivates. This scarcely matters in
     * steady-state computations involving all workers, but can create
     * contention and bookkeeping bottlenecks during ramp-up,
     * ramp-down, and small computations involving only a few workers.
     *
     * Scanning. Method scan (from runWorker) performs top-level
     * scanning for tasks. (Similar scans appear in helpQuiesce and
     * pollScan.)  Each scan traverses and tries to poll from each
     * queue starting at a random index. Scans are not performed in
     * ideal random permutation order, to reduce cacheline
     * contention. The pseudorandom generator need not have
     * high-quality statistical properties in the long term, but just
     * within computations; We use Marsaglia XorShifts (often via
     * ThreadLocalRandom.nextSecondarySeed), which are cheap and
     * suffice. Scanning also includes contention reduction: When
     * scanning workers fail to extract an apparently existing task,
     * they soon restart at a different pseudorandom index.  This form
     * of backoff improves throughput when many threads are trying to
     * take tasks from few queues, which can be common in some usages.
     * Scans do not otherwise explicitly take into account core
     * affinities, loads, cache localities, etc, However, they do
     * exploit temporal locality (which usually approximates these) by
     * preferring to re-poll from the same queue after a successful
     * poll before trying others (see method topLevelExec). However
     * this preference is bounded (see TOP_BOUND_SHIFT) as a safeguard
     * against infinitely unfair looping under unbounded user task
     * recursion, and also to reduce long-term contention when many
     * threads poll few queues holding many small tasks. The bound is
     * high enough to avoid much impact on locality and scheduling
     * overhead.
     *
     * Trimming workers. To release resources after periods of lack of
     * use, a worker starting to wait when the pool is quiescent will
     * time out and terminate (see method runWorker) if the pool has
     * remained quiescent for period given by field keepAlive.
     *
     * Shutdown and Termination. A call to shutdownNow invokes
     * tryTerminate to atomically set a runState bit. The calling
     * thread, as well as every other worker thereafter terminating,
     * helps terminate others by cancelling their unprocessed tasks,
     * and waking them up, doing so repeatedly until stable. Calls to
     * non-abrupt shutdown() preface this by checking whether
     * termination should commence by sweeping through queues (until
     * stable) to ensure lack of in-flight submissions and workers
     * about to process them before triggering the "STOP" phase of
     * termination.
     *
     * Joining Tasks
     * =============
     *
     * Any of several actions may be taken when one worker is waiting
     * to join a task stolen (or always held) by another.  Because we
     * are multiplexing many tasks on to a pool of workers, we can't
     * always just let them block (as in Thread.join).  We also cannot
     * just reassign the joiner's run-time stack with another and
     * replace it later, which would be a form of "continuation", that
     * even if possible is not necessarily a good idea since we may
     * need both an unblocked task and its continuation to progress.
     * Instead we combine two tactics:
     *
     *   Helping: Arranging for the joiner to execute some task that it
     *      would be running if the steal had not occurred.
     *
     *   Compensating: Unless there are already enough live threads,
     *      method tryCompensate() may create or re-activate a spare
     *      thread to compensate for blocked joiners until they unblock.
     *
     * A third form (implemented in tryRemoveAndExec) amounts to
     * helping a hypothetical compensator: If we can readily tell that
     * a possible action of a compensator is to steal and execute the
     * task being joined, the joining thread can do so directly,
     * without the need for a compensation thread.
     *
     * The ManagedBlocker extension API can't use helping so relies
     * only on compensation in method awaitBlocker.
     *
     * The algorithm in awaitJoin entails a form of "linear helping".
     * Each worker records (in field source) the id of the queue from
     * which it last stole a task.  The scan in method awaitJoin uses
     * these markers to try to find a worker to help (i.e., steal back
     * a task from and execute it) that could hasten completion of the
     * actively joined task.  Thus, the joiner executes a task that
     * would be on its own local deque if the to-be-joined task had
     * not been stolen. This is a conservative variant of the approach
     * described in Wagner & Calder "Leapfrogging: a portable
     * technique for implementing efficient futures" SIGPLAN Notices,
     * 1993 (http://portal.acm.org/citation.cfm?id=155354). It differs
     * mainly in that we only record queue ids, not full dependency
     * links.  This requires a linear scan of the workQueues array to
     * locate stealers, but isolates cost to when it is needed, rather
     * than adding to per-task overhead. Searches can fail to locate
     * stealers GC stalls and the like delay recording sources.
     * Further, even when accurately identified, stealers might not
     * ever produce a task that the joiner can in turn help with. So,
     * compensation is tried upon failure to find tasks to run.
     *
     * Compensation does not by default aim to keep exactly the target
     * parallelism number of unblocked threads running at any given
     * time. Some previous versions of this class employed immediate
     * compensations for any blocked join. However, in practice, the
     * vast majority of blockages are transient byproducts of GC and
     * other JVM or OS activities that are made worse by replacement
     * when they cause longer-term oversubscription.  Rather than
     * impose arbitrary policies, we allow users to override the
     * default of only adding threads upon apparent starvation.  The
     * compensation mechanism may also be bounded.  Bounds for the
     * commonPool (see COMMON_MAX_SPARES) better enable JVMs to cope
     * with programming errors and abuse before running out of
     * resources to do so.
     *
     * Common Pool
     * ===========
     *
     * The static common pool always exists after static
     * initialization.  Since it (or any other created pool) need
     * never be used, we minimize initial construction overhead and
     * footprint to the setup of about a dozen fields.
     *
     * When external threads submit to the common pool, they can
     * perform subtask processing (see externalHelpComplete and
     * related methods) upon joins.  This caller-helps policy makes it
     * sensible to set common pool parallelism level to one (or more)
     * less than the total number of available cores, or even zero for
     * pure caller-runs.  We do not need to record whether external
     * submissions are to the common pool -- if not, external help
     * methods return quickly. These submitters would otherwise be
     * blocked waiting for completion, so the extra effort (with
     * liberally sprinkled task status checks) in inapplicable cases
     * amounts to an odd form of limited spin-wait before blocking in
     * ForkJoinTask.join.
     *
     * As a more appropriate default in managed environments, unless
     * overridden by system properties, we use workers of subclass
     * InnocuousForkJoinWorkerThread when there is a SecurityManager
     * present. These workers have no permissions set, do not belong
     * to any user-defined ThreadGroup, and erase all ThreadLocals
     * after executing any top-level task (see
     * WorkQueue.afterTopLevelExec).  The associated mechanics (mainly
     * in ForkJoinWorkerThread) may be JVM-dependent and must access
     * particular Thread class fields to achieve this effect.
     *
     * Memory placement
     * ================
     *
     * Performance can be very sensitive to placement of instances of
     * ForkJoinPool and WorkQueues and their queue arrays. To reduce
     * false-sharing impact, the @Contended annotation isolates
     * adjacent WorkQueue instances, as well as the ForkJoinPool.ctl
     * field. WorkQueue arrays are allocated (by their threads) with
     * larger initial sizes than most ever need, mostly to reduce
     * false sharing with current garbage collectors that use cardmark
     * tables.
     *
     * Style notes
     * ===========
     *
     * Memory ordering relies mainly on VarHandles.  This can be
     * awkward and ugly, but also reflects the need to control
     * outcomes across the unusual cases that arise in very racy code
     * with very few invariants. All fields are read into locals
     * before use, and null-checked if they are references.  Array
     * accesses using masked indices include checks (that are always
     * true) that the array length is non-zero to avoid compilers
     * inserting more expensive traps.  This is usually done in a
     * "C"-like style of listing declarations at the heads of methods
     * or blocks, and using inline assignments on first encounter.
     * Nearly all explicit checks lead to bypass/return, not exception
     * throws, because they may legitimately arise due to
     * cancellation/revocation during shutdown.
     *
     * There is a lot of representation-level coupling among classes
     * ForkJoinPool, ForkJoinWorkerThread, and ForkJoinTask.  The
     * fields of WorkQueue maintain data structures managed by
     * ForkJoinPool, so are directly accessed.  There is little point
     * trying to reduce this, since any associated future changes in
     * representations will need to be accompanied by algorithmic
     * changes anyway. Several methods intrinsically sprawl because
     * they must accumulate sets of consistent reads of fields held in
     * local variables. Some others are artificially broken up to
     * reduce producer/consumer imbalances due to dynamic compilation.
     * There are also other coding oddities (including several
     * unnecessary-looking hoisted null checks) that help some methods
     * perform reasonably even when interpreted (not compiled).
     *
     * The order of declarations in this file is (with a few exceptions):
     * (1) Static utility functions
     * (2) Nested (static) classes
     * (3) Static fields
     * (4) Fields, along with constants used when unpacking some of them
     * (5) Internal control methods
     * (6) Callbacks and other support for ForkJoinTask methods
     * (7) Exported methods
     * (8) Static block initializing statics in minimally dependent order
     */
    
    // Bounds
    static final int SWIDTH       = 16;            // width of short
    static final int SMASK        = 0xffff;        // short bits == max index
    static final int MAX_CAP      = 0x7fff;        // max #workers - 1
    static final int SQMASK       = 0x007e;        // 最多使用64个偶数插槽（最多64个【共享队列】）
    
    // Masks and units for WorkQueue.phase and ctl sp subfield
    static final int QLOCK        = 1;             // must be 1
    static final int SS_SEQ       = 1 << 16;       // version count
    static final int QUIET        = 1 << 30;       // not scanning or working
    static final int UNSIGNALLED  = 1 << 31;       // must be negative
    static final int DORMANT      = QUIET | UNSIGNALLED;
    
    // Mode bits and sentinels, some also used in WorkQueue id and.source fields
    static final int OWNED        = 1;             // queue has owner thread
    
    // 【提交线程】的【共享队列】没有此标记，【工作线程】的【工作队列】是否有此标记取决于构造器的asyncMode参数
    static final int FIFO         = 1 << 16;       // fifo queue or access mode
    
    static final int SHUTDOWN     = 1 << 18;
    static final int TERMINATED   = 1 << 19;
    static final int STOP         = 1 << 31;       // must be negative
    
    /*
     * 工作池模式
     * 前16位记录运行状态，后16位记录并行度
     */
    volatile int mode;                   // parallelism, runstate, queue mode
    
    
    /**
     * Initial capacity of work-stealing queue array.
     * Must be a power of two, at least 2.
     */
    static final int INITIAL_QUEUE_CAPACITY = 1 << 13;
    
    /**
     * Maximum capacity for queue arrays.
     * Must be a power of two less than or equal to 1 << (31 - width of array entry) to ensure lack of wraparound of index calculations,
     * but defined to a value a bit less than this to help users trap runaway programs before saturating systems.
     */
    static final int MAXIMUM_QUEUE_CAPACITY = 1 << 26; // 64M
    
    /**
     * The maximum number of top-level polls per worker before checking other queues, expressed as a bit shift to, in effect,
     * multiply by pool size, and then use as random value mask, so average bound is about poolSize*(1<<TOP_BOUND_SHIFT).
     * See above for rationale.
     */
    static final int TOP_BOUND_SHIFT = 10;
    
    /**
     * Default idle timeout value (in milliseconds) for the thread triggering quiescence to park waiting for new work
     */
    // 单位：毫秒
    private static final long DEFAULT_KEEPALIVE = 60_000L;
    
    /**
     * Undershoot tolerance for idle timeouts
     */
    // 容差
    private static final long TIMEOUT_SLOP = 20L;
    
    /**
     * Increment for seed generators. See class ThreadLocal for explanation.
     */
    // 生成均匀哈希值的魔数，与ThreadLocal中的HASH_INCREMENT常量作用一致
    private static final int SEED_INCREMENT = 0x9e3779b9;
    
    /*
     * Bits and masks for field ctl, packed with 4 16-bit subfields:
     * RC: Number of released (unqueued) workers minus target parallelism
     * TC: Number of total workers minus target parallelism
     * SS: version count and status of top waiting thread
     * ID: poolIndex of top of Treiber stack of waiters
     *
     * When convenient, we can extract the lower 32 stack top bits (including version bits) as sp=(int)ctl.
     * The offsets of counts by the target parallelism and the positionings of fields makes it possible to perform the most common checks via sign tests of fields:
     * When ac is negative, there are not enough unqueued workers, when tc is negative, there are not enough total workers.
     * When sp is non-zero, there are waiting workers.
     * To deal with possibly negative fields, we use casts in and out of "short" and/or signed shifts to maintain signedness.
     *
     * Because it occupies uppermost bits, we can add one release count using getAndAddLong of RC_UNIT, rather than CAS, when returning from a blocked join.
     * Other updates entail multiple subfields and masking, requiring CAS.
     *
     * The limits packed in field "bounds" are also offset by the parallelism level to make them comparable to the ctl rc and tc fields.
     */
    
    // Lower and upper word masks
    private static final long SP_MASK    = 0xffffffffL; // 0x 0000 0000 FFFF FFFF
    private static final long UC_MASK    = ~SP_MASK;    // 0x FFFF FFFF 0000 0000
    
    // Release counts（用来计算【工作线程】活跃度）
    private static final int  RC_SHIFT   = 48;
    private static final long RC_UNIT    = 0x0001L << RC_SHIFT; // 0x 0001 0000 0000 0000
    private static final long RC_MASK    = 0xffffL << RC_SHIFT; // 0x FFFF 0000 0000 0000
    
    // Total counts（用来计算【工作线程】总数，>=并行度）
    private static final int  TC_SHIFT   = 32;
    private static final long TC_UNIT    = 0x0001L << TC_SHIFT; // 0x 0000 0001 0000 0000
    private static final long TC_MASK    = 0xffffL << TC_SHIFT; // 0x 0000 FFFF 0000 0000
    
    // sign
    private static final long ADD_WORKER = 0x0001L << (TC_SHIFT + 15);  // 0x 0000 8000 0000 0000
    
    /*
     *     ①   ②   ③   ④
     * 0x 0000 0000 0000 0000
     *
     * 第1个16位初始化为并行度的负数，
     * 可用来计算活跃的线程数量
     * （新建工作线程或工作线程从阻塞中恢复时增一，而工作线程转入阻塞时减一）
     *
     * 第2个16位初始化为工作池允许的【工作线程】的最大数量（>=并行度）的负数，
     * 可用来计算当前创建的【工作线程】数量（不受休眠数量的影响）
     *
     * 第3、4个16位记录了最近一个转入阻塞的【工作线程】管辖的【工作队列】的phase信息
     *
     * 第3个16位是随机数
     * 第4个16位记录工人数量
     */
    @jdk.internal.vm.annotation.Contended("fjpctl") // segregate
    volatile long ctl;                   // main pool control
    
    // Instance fields
    
    // 各线程累计的任务窃取数量
    volatile long stealCount;            // collects worker nsteals
    
    final long keepAlive;                // milliseconds before dropping if idle
    
    // 魔数，辅助生成【工作队列】的ID
    int indexSeed;                       // next worker index
    
    final int bounds;                    // min, max threads packed as shorts
    
    /*
     * 【工作组】，容量为2的冪
     *
     * 一个【工作池】(ForkJoinPool)上对应一个【工作组】(WorkQueue数组)
     * 每个【工作组】(WorkQueue数组)中包含多个【工作队列】(WorkQueue)
     * 每个【工作队列】(WorkQueue)中都包含一个任务组(ForkJoinTask数组)
     * 任务组(ForkJoinTask数组)中存放了正在排队的任务(ForkJoinTask)
     *
     * 这个模型可以简化为：【工作组】的每个插槽中带有一个任务队列（任务组的另一种称呼）
     */
    WorkQueue[] workQueues;              // 【工作组】
    
    // 【工作线程】名称前缀
    final String workerNamePrefix;       // for worker thread string; sync lock
    
    // 【工作池】上的【工作线程】工厂
    final ForkJoinWorkerThreadFactory factory;
    
    /**
     * Creates a new ForkJoinWorkerThread.
     * This factory is used unless overridden in ForkJoinPool constructors.
     */
    // 默认的【工作线程】工厂
    public static final ForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory;
    
    // 未捕获异常处理器
    final UncaughtExceptionHandler ueh;  // per-worker UEH
    
    final Predicate<? super ForkJoinPool> saturate;
    
    /**
     * Permission required for callers of methods that may start or kill threads.
     */
    // 用于线程池关闭时的权限
    static final RuntimePermission modifyThreadPermission;
    
    /**
     * Sequence number for creating workerNamePrefix.
     */
    // 【工作池】编号（累加）
    private static int poolNumberSequence;
    
    
    /*▼ 共享工作池 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /**
     * Common (static) pool.
     * Non-null for public use unless a static construction exception,
     * but internal usages null-check on use to paranoically avoid potential initialization circularities as well as to simplify generated code.
     */
    // 共享工作池：所有ForkJoinPool对象共享
    static final ForkJoinPool common;
    
    /**
     * Common pool parallelism.
     * To allow simpler use and management when common pool threads are disabled,
     * we allow the underlying common.parallelism field to be zero,
     * but in that case still report parallelism as 1 to reflect resulting caller-runs mechanics.
     */
    // 共享工作池的并行度
    static final int COMMON_PARALLELISM;
    
    /**
     * Limit on spare thread construction in tryCompensate.
     */
    // 共享工作池中【工作线程】数量最大值
    private static final int COMMON_MAX_SPARES;
    
    /**
     * The default value for COMMON_MAX_SPARES.
     * Overridable using the "java.util.concurrent.ForkJoinPool.common.maximumSpares" system property.
     * The default value is far in excess of normal requirements,
     * but also far short of MAX_CAP and typical OS thread limits,
     * so allows JVMs to catch misuse/abuse before running out of resources needed to do so.
     */
    // 共享工作池默认的最大【工作线程】数量
    private static final int DEFAULT_COMMON_MAX_SPARES = 256;
    
    /*▲ 共享工作池 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    // VarHandle mechanics
    private static final VarHandle CTL;
    private static final VarHandle MODE;
    static final VarHandle QA;
    
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            CTL = l.findVarHandle(ForkJoinPool.class, "ctl", long.class);
            MODE = l.findVarHandle(ForkJoinPool.class, "mode", int.class);
            QA = MethodHandles.arrayElementVarHandle(ForkJoinTask[].class);
        } catch(ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
        
        // 设置默认的F/J【工作线程】工厂
        defaultForkJoinWorkerThreadFactory = new DefaultForkJoinWorkerThreadFactory();
        
        // 设置权限
        modifyThreadPermission = new RuntimePermission("modifyThread");
        
        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
        
        // 设置共享工作池允许的最大【工作线程】数量
        int commonMaxSpares = DEFAULT_COMMON_MAX_SPARES;
        try {
            // 可通过运行参数修改共享工作池允许的最大【工作线程】数量：-Djava.util.concurrent.ForkJoinPool.common.maximumSpares=<size>
            String p = System.getProperty("java.util.concurrent.ForkJoinPool.common.maximumSpares");
            if(p != null) {
                commonMaxSpares = Integer.parseInt(p);
            }
        } catch(Exception ignore) {
        }
        
        COMMON_MAX_SPARES = commonMaxSpares;
        
        // 初始化共享工作池
        common = AccessController.doPrivileged(new PrivilegedAction<>() {
            public ForkJoinPool run() {
                return new ForkJoinPool((byte) 0);
            }
        });
        
        // 设置共享工作池的并行度
        COMMON_PARALLELISM = Math.max(common.mode & SMASK, 1);
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code ForkJoinPool} with parallelism equal to {@link
     * java.lang.Runtime#availableProcessors}, using defaults for all
     * other parameters (see {@link #ForkJoinPool(int,
     * ForkJoinWorkerThreadFactory, UncaughtExceptionHandler, boolean,
     * int, int, int, Predicate, long, TimeUnit)}).
     *
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public ForkJoinPool() {
        this(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()),
            defaultForkJoinWorkerThreadFactory, null, false, 0, MAX_CAP, 1, null, DEFAULT_KEEPALIVE, TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Creates a {@code ForkJoinPool} with the indicated parallelism
     * level, using defaults for all other parameters (see {@link
     * #ForkJoinPool(int, ForkJoinWorkerThreadFactory,
     * UncaughtExceptionHandler, boolean, int, int, int, Predicate,
     * long, TimeUnit)}).
     *
     * @param parallelism the parallelism level
     * @throws IllegalArgumentException if parallelism less than or
     *         equal to zero, or greater than implementation limit
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public ForkJoinPool(int parallelism) {
        this(parallelism, defaultForkJoinWorkerThreadFactory, null, false, 0, MAX_CAP, 1, null, DEFAULT_KEEPALIVE, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a {@code ForkJoinPool} with the given parameters (using
     * defaults for others -- see {@link #ForkJoinPool(int,
     * ForkJoinWorkerThreadFactory, UncaughtExceptionHandler, boolean,
     * int, int, int, Predicate, long, TimeUnit)}).
     *
     * @param parallelism the parallelism level. For default value,
     * use {@link java.lang.Runtime#availableProcessors}.
     * @param factory the factory for creating new threads. For default value,
     * use {@link #defaultForkJoinWorkerThreadFactory}.
     * @param handler the handler for internal worker threads that
     * terminate due to unrecoverable errors encountered while executing
     * tasks. For default value, use {@code null}.
     * @param asyncMode if true,
     * establishes local first-in-first-out scheduling mode for forked
     * tasks that are never joined. This mode may be more appropriate
     * than default locally stack-based mode in applications in which
     * worker threads only process event-style asynchronous tasks.
     * For default value, use {@code false}.
     * @throws IllegalArgumentException if parallelism less than or
     *         equal to zero, or greater than implementation limit
     * @throws NullPointerException if the factory is null
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode) {
        this(parallelism, factory, handler, asyncMode, 0, MAX_CAP, 1, null, DEFAULT_KEEPALIVE, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a {@code ForkJoinPool} with the given parameters.
     *
     * @param parallelism the parallelism level. For default value,
     * use {@link java.lang.Runtime#availableProcessors}.
     *
     * @param factory the factory for creating new threads. For
     * default value, use {@link #defaultForkJoinWorkerThreadFactory}.
     *
     * @param handler the handler for internal worker threads that
     * terminate due to unrecoverable errors encountered while
     * executing tasks. For default value, use {@code null}.
     *
     * @param asyncMode if true, establishes local first-in-first-out
     * scheduling mode for forked tasks that are never joined. This
     * mode may be more appropriate than default locally stack-based
     * mode in applications in which worker threads only process
     * event-style asynchronous tasks.  For default value, use {@code
     * false}.
     *
     * @param corePoolSize the number of threads to keep in the pool
     * (unless timed out after an elapsed keep-alive). Normally (and
     * by default) this is the same value as the parallelism level,
     * but may be set to a larger value to reduce dynamic overhead if
     * tasks regularly block. Using a smaller value (for example
     * {@code 0}) has the same effect as the default.
     *
     * @param maximumPoolSize the maximum number of threads allowed.
     * When the maximum is reached, attempts to replace blocked
     * threads fail.  (However, because creation and termination of
     * different threads may overlap, and may be managed by the given
     * thread factory, this value may be transiently exceeded.)  To
     * arrange the same value as is used by default for the common
     * pool, use {@code 256} plus the {@code parallelism} level. (By
     * default, the common pool allows a maximum of 256 spare
     * threads.)  Using a value (for example {@code
     * Integer.MAX_VALUE}) larger than the implementation's total
     * thread limit has the same effect as using this limit (which is
     * the default).
     *
     * @param minimumRunnable the minimum allowed number of core
     * threads not blocked by a join or {@link ManagedBlocker}.  To
     * ensure progress, when too few unblocked threads exist and
     * unexecuted tasks may exist, new threads are constructed, up to
     * the given maximumPoolSize.  For the default value, use {@code
     * 1}, that ensures liveness.  A larger value might improve
     * throughput in the presence of blocked activities, but might
     * not, due to increased overhead.  A value of zero may be
     * acceptable when submitted tasks cannot have dependencies
     * requiring additional threads.
     *
     * @param saturate if non-null, a predicate invoked upon attempts
     * to create more than the maximum total allowed threads.  By
     * default, when a thread is about to block on a join or {@link
     * ManagedBlocker}, but cannot be replaced because the
     * maximumPoolSize would be exceeded, a {@link
     * RejectedExecutionException} is thrown.  But if this predicate
     * returns {@code true}, then no exception is thrown, so the pool
     * continues to operate with fewer than the target number of
     * runnable threads, which might not ensure progress.
     *
     * @param keepAliveTime the elapsed time since last use before
     * a thread is terminated (and then later replaced if needed).
     * For the default value, use {@code 60, TimeUnit.SECONDS}.
     *
     * @param unit the time unit for the {@code keepAliveTime} argument
     *
     * @throws IllegalArgumentException if parallelism is less than or
     *         equal to zero, or is greater than implementation limit,
     *         or if maximumPoolSize is less than parallelism,
     *         of if the keepAliveTime is less than or equal to zero.
     * @throws NullPointerException if the factory is null
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     * @since 9
     */
    // 构造通用的工作池
    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode, int corePoolSize,
                        int maximumPoolSize, int minimumRunnable, Predicate<? super ForkJoinPool> saturate, long keepAliveTime, TimeUnit unit) {
        
        // check, encode, pack parameters
        if (parallelism <= 0 || parallelism > MAX_CAP   // 并行度默认在(0, MAX_CAP]范围内
            || maximumPoolSize < parallelism
            || keepAliveTime <= 0L) {
            throw new IllegalArgumentException();
        }
        
        if (factory == null) {
            throw new NullPointerException();
        }
        
        // 当前工作池允许的最大线程数量
        int maxSpares = Math.min(maximumPoolSize, MAX_CAP) - parallelism;
        
        // 当前工作池允许的最小线程数量
        int minAvail = Math.min(Math.max(minimumRunnable, 0), MAX_CAP);
        
        // 计算工作池允许的最大-最小【工作线程】数量
        int bounds = (maxSpares << SWIDTH) | ((minAvail - parallelism) & SMASK) ;
        
        // 默认的【工作线程】数量，最小值与并行度相同，最大值与MAX_CAP相同
        int corep = Math.min(Math.max(corePoolSize, parallelism), MAX_CAP);
        
        // 以负数形式保存了并行度（0x FFFF 0000 0000 0000）与维护的【工作线程】数量（0x 0000 FFFF 0000 0000）
        long ctl = (((long) (-parallelism) << RC_SHIFT) & RC_MASK) | (((long) (-corep) << TC_SHIFT) & TC_MASK);
        
        // 确定访问模式（默认是非异步）
        int accessMode = asyncMode ? FIFO : 0;
        
        // 初始化工作池的工作模式
        int mode = parallelism | accessMode;
        
        // 计算【工作线程】数组的容量（与并行度相关）
        int n = (parallelism>1) ? parallelism - 1 : 1; // at least 2 slots
        
        /*
         * 周期性地将n扩大2~4倍
         * 实际计算结果是：2^(ceil(log2(n+1))+1)
         */
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        n = (n + 1) << 1; // power of two, including space for submission queues
        
        this.workerNamePrefix = "ForkJoinPool-" + nextPoolId() + "-worker-";
        this.workQueues = new WorkQueue[n]; // 【工作组】的容量由并行度间接确定
        this.factory = factory;
        this.ueh = handler;
        this.saturate = saturate;
        this.keepAlive = Math.max(unit.toMillis(keepAliveTime), TIMEOUT_SLOP);
        this.bounds = bounds;
        this.mode = mode;
        this.ctl = ctl;
        
        checkPermission();
    }
    
    /**
     * Constructor for common pool using parameters possibly
     * overridden by system properties
     */
    // 共享工作池的构造方法
    private ForkJoinPool(byte forCommonPoolOnly) {
        int parallelism = -1;
        
        ForkJoinWorkerThreadFactory factory = null;
        UncaughtExceptionHandler handler = null;
        
        try {
            // 可以通过运行参数设置共享工作池的并行度
            String pp = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
            if(pp != null) {
                parallelism = Integer.parseInt(pp);
            }
            // 可以通过运行参数设置共享工作池的并行度
            factory = (ForkJoinWorkerThreadFactory) newInstanceFromSystemProperty("java.util.concurrent.ForkJoinPool.common.threadFactory");
            // 可以通过运行参数设置未捕获的异常处理器
            handler = (UncaughtExceptionHandler) newInstanceFromSystemProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler");
        } catch(Exception ignore) {
            // ignore exceptions in accessing/parsing properties
        }
        
        if(factory == null) {
            if(System.getSecurityManager() == null) {
                // 默认的【工作线程】工厂
                factory = defaultForkJoinWorkerThreadFactory;
            } else { // use security-managed default
                // 无害的【工作线程】工厂
                factory = new InnocuousForkJoinWorkerThreadFactory();
            }
        }
        
        // default 1 less than #cores
        if(parallelism<0 && (parallelism = Runtime.getRuntime().availableProcessors() - 1)<=0) {
            // 并行度最小为1
            parallelism = 1;
        }
        
        if(parallelism>MAX_CAP) {
            // 并行度最大为MAX_CAP
            parallelism = MAX_CAP;
        }
        
        long ctl = ((((long) (-parallelism) << TC_SHIFT) & TC_MASK) | (((long) (-parallelism) << RC_SHIFT) & RC_MASK));
        
        // 共享工作池的最大/最小【工作线程】数量
        int bounds = ((1 - parallelism) & SMASK) | (COMMON_MAX_SPARES << SWIDTH);
        
        // 计算【工作线程】数组的容量（与并行度相关）
        int n = (parallelism>1) ? parallelism - 1 : 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        n = (n + 1) << 1;
        
        this.workerNamePrefix = "ForkJoinPool.commonPool-worker-";
        this.workQueues = new WorkQueue[n];
        this.factory = factory;
        this.ueh = handler;
        this.saturate = null;
        this.keepAlive = DEFAULT_KEEPALIVE;
        this.bounds = bounds;
        this.mode = parallelism;
        this.ctl = ctl;
    }
    
    // 通过反射从指定属性中创建实例
    private static Object newInstanceFromSystemProperty(String property) throws ReflectiveOperationException {
        String className = System.getProperty(property);
        if(className == null) {
            return null;
        }
        
        return ClassLoader.getSystemClassLoader().loadClass(className).getConstructor().newInstance();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 共享工作池 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the targeted parallelism level of the common pool.
     *
     * @return the targeted parallelism level of the common pool
     * @since 1.8
     */
    // 返回共享工作池的并行度
    public static int getCommonPoolParallelism() {
        return COMMON_PARALLELISM;
    }
    
    /**
     * Returns the common pool instance. This pool is statically
     * constructed; its run state is unaffected by attempts to {@link
     * #shutdown} or {@link #shutdownNow}. However this pool and any
     * ongoing processing are automatically terminated upon program
     * {@link System#exit}.  Any program that relies on asynchronous
     * task processing to complete before program termination should
     * invoke {@code commonPool().}{@link #awaitQuiescence awaitQuiescence},
     * before exit.
     *
     * @return the common pool instance
     * @since 1.8
     */
    // 返回共享工作池
    public static ForkJoinPool commonPool() {
        // assert common != null : "static init error";
        return common;
    }
    
    /*▲ 共享工作池 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 分发/提交/执行/移除任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Submits a ForkJoinTask for execution.
     *
     * @param task the task to submit
     * @param <T> the type of the task's result
     * @return the task
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    // 提交/执行F/J任务，返回任务自身
    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        // 分发任务。将task分发到【队列】的top处排队，并创建/唤醒【工作线程】
        return externalSubmit(task);
    }
    
    /**
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    // 提交/执行实现了Runnable接口的任务，返回任务自身
    @SuppressWarnings("unchecked")
    public ForkJoinTask<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        
        ForkJoinTask<Void> fjTask = null;
        
        if(task instanceof ForkJoinTask<?>){
            fjTask = (ForkJoinTask<Void>) task; // 避免重复包装
        } else {
            fjTask = new ForkJoinTask.AdaptedRunnableAction(task);
        }
        
        // 分发任务。将task分发到【队列】的top处排队，并创建/唤醒【工作线程】
        return externalSubmit(fjTask);
    }
    
    /**
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     */
    // 提交/执行实现了Runnable接口的任务，返回任务自身
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        ForkJoinTask<T> fjTask = new AdaptedRunnable<>(task, result);
        
        // 分发任务。将task分发到【队列】的top处排队，并创建/唤醒【工作线程】
        return externalSubmit(fjTask);
    }
    
    /**
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    // 提交/执行实现了Callable接口的任务，返回任务自身
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        ForkJoinTask<T> fjTask = new ForkJoinTask.AdaptedCallable<T>(task);
        
        // 分发任务。将task分发到【队列】的top处排队，并创建/唤醒【工作线程】
        return externalSubmit(fjTask);
    }
    
    /**
     * Performs the given task, returning its result upon completion.
     * If the computation encounters an unchecked Exception or Error,
     * it is rethrown as the outcome of this invocation.  Rethrown
     * exceptions behave in the same way as regular exceptions, but,
     * when possible, contain stack traces (as displayed for example
     * using {@code ex.printStackTrace()}) of both the current thread
     * as well as the thread actually encountering the exception;
     * minimally only the latter.
     *
     * @param task the task
     * @param <T> the type of the task's result
     * @return the task's result
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    // 安排执行任务，并等待返回执行结果
    public <T> T invoke(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        
        // 分发任务。将task分发到【队列】的top处排队，并创建/唤醒【工作线程】
        externalSubmit(task);
        
        // 从【工作队列】的top处取出当前任务并执行，最后返回任务的执行结果
        return task.join();
    }
    
    /**
     * Arranges for (asynchronous) execution of the given task.
     *
     * @param task the task
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    // 分发任务，安排异步执行任务
    public void execute(ForkJoinTask<?> task) {
        // 分发任务。将task分发到【队列】的top处排队，并创建/唤醒【工作线程】
        externalSubmit(task);
    }
    
    /**
     * @throws NullPointerException if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     */
    // 分发、执行Runnable类型的任务
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        
        ForkJoinTask<?> fjTask;
        if (task instanceof ForkJoinTask<?>) { // avoid re-wrap
            fjTask = (ForkJoinTask<?>) task;
        } else {
            fjTask = new ForkJoinTask.RunnableExecuteAction(task);
        }
        
        // 分发任务。将task分发到【队列】的top处排队，并创建/唤醒【工作线程】
        externalSubmit(fjTask);
    }
    
    // 分发、执行容器中所有Callable类型的任务
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        // In previous versions of this class, this method constructed a task to run ForkJoinTask.invokeAll,
        // but now external invocation of multiple tasks is at least as efficient.
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        
        try {
            // 遍历容器，通过适配器包装任务
            for (Callable<T> task : tasks) {
                ForkJoinTask<T> fjTask = new ForkJoinTask.AdaptedCallable<T>(task);
                futures.add(fjTask);
                
                // 分发任务。将task分发到【队列】的top处排队，并创建/唤醒【工作线程】
                externalSubmit(fjTask);
            }
            
            for(Future<T> future : futures) {
                // 从【工作队列】的top处取出当前任务并执行，不返回任务状态。必要时，需要等待其他任务的完成
                ((ForkJoinTask<?>) future).quietlyJoin();
            }
            
            return futures;
        } catch (Throwable t) {
            for(Future<T> future : futures) {
                future.cancel(false);
            }
            
            throw t;
        }
    }
    
    /**
     * Pushes a possibly-external submission.
     */
    /*
     * 分发任务。将task分发到【队列】的top处排队，并创建/唤醒【工作线程】
     * 该分发动作可能由【提交线程】发起，也可能由【工作线程】发起
     */
    private <T> ForkJoinTask<T> externalSubmit(ForkJoinTask<T> task) {
        if(task == null) {
            throw new NullPointerException();
        }
        
        // 获取提交任务的线程
        Thread t = Thread.currentThread();
        
        // 如果提交动作发生在【工作线程】
        if(t instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
            
            // 如果【工作线程】的工作池就是当前的工作池
            if(wt.pool == this) {
                // 如果该【工作线程】中存在【工作队列】
                if(wt.workQueue != null) {
                    // 分发任务。【工作线程】将指定的task放入【工作队列】top处排队，并创建/唤醒【工作线程】
                    wt.workQueue.push(task);
                }
            }
            
            // 如果提交动作发生在【提交线程】
        } else {
            // 分发任务。【提交线程】将task放入【共享队列】top处排队，并创建/唤醒【工作线程】。如果【共享队列】不存在的话需要新建
            externalPush(task);
        }
        
        return task;
    }
    
    /**
     * Adds the given task to a submission queue at submitter's current queue, creating one if null or contended.
     *
     * @param task the task. Caller must ensure non-null.
     */
    // 分发任务。【提交线程】将task放入【共享队列】top处排队，并创建/唤醒【工作线程】。如果【共享队列】不存在的话需要新建
    final void externalPush(ForkJoinTask<?> task) {
        // 获取当前【提交线程】的探测值（可用来计算【共享队列】所在的插槽）
        int r= ThreadLocalRandom.getProbe();                                // initialize caller's probe
        
        // 确保ThreadLocalRandom已初始化，并设置了原始的种子
        if(r == 0) {
            ThreadLocalRandom.localInit();
            // 获取当前【提交线程】的探测值
            r = ThreadLocalRandom.getProbe();
        }
        
        while(true) {
            if((mode & SHUTDOWN) != 0 || workQueues == null || workQueues.length<=0) {
                throw new RejectedExecutionException();
            }
            
            // 相当于随机获取一个偶数索引[0,126]
            int index = r & (workQueues.length - 1) & SQMASK;
            
            // 指向【工作组】的偶数插槽（相当于为当前的【提交线程】在【工作组】上锁定一个插槽）
            WorkQueue wq = workQueues[index];
            
            // 如果该偶数插槽处还没有【共享队列】，就为该插槽新建一个【共享队列】
            if(wq==null) { // add queue
                // 为【提交线程】创建一个【共享队列】
                wq = new WorkQueue(this, null);
                
                // 初始化【工作组】
                wq.array = new ForkJoinTask<?>[INITIAL_QUEUE_CAPACITY];
                
                // 初始化id，添加QUIET标记，去除FIFO和OWNED标记
                wq.id = (r | QUIET) & ~(FIFO | OWNED);
                
                wq.source = QUIET;
                
                if(workerNamePrefix != null) {
                    // unless disabled, lock pool to install
                    synchronized(workerNamePrefix) {
                        // 确保【工作组】有效
                        if(workQueues!= null && workQueues.length>0) {
                            // 注：此处的i与上面的index等值，原因是初始化id时改变的那些位不影响索引值的计算
                            int i = wq.id & (workQueues.length - 1) & SQMASK;
                            if(workQueues[i] == null){
                                // 向【工作组】中放置【共享队列】
                                workQueues[i] = wq;  // else another thread already installed
                            }
                        }
                    }
                }
                
                // 如果该偶数插槽处存在【共享队列】
            } else {
                // 尝试锁定【共享队列】
                if(!wq.tryLockPhase()) {
                    // 如果锁定失败，说明该【共享队列】已被其它【提交线程】锁定，此时需要更新探测值，并进入新的循环，重新寻找一个偶数插槽
                    r = ThreadLocalRandom.advanceProbe(r);
                    
                    // 如果成功锁定【共享队列】
                } else {
                    // 【提交线程】将task放入【共享队列】top处
                    if(wq.lockedPush(task)) {
                        // 创建/唤醒【工作线程】
                        signalWork();
                    }
                    return;
                }
            }
            
        } // while(true)
    }
    
    /**
     * Performs tryUnpush for an external submitter.
     */
    // 移除任务。【提交线程】尝试将指定的task从【共享队列】top处移除，返回值代表是否成功移除
    final boolean tryExternalUnpush(ForkJoinTask<?> task) {
        // 获取当前【提交线程】的探测值（可用来计算【共享队列】所在的插槽）
        int r = ThreadLocalRandom.getProbe();
        
        // 【工作组】存在，且不为空
        if(workQueues != null && workQueues.length>0) {
            // 由探测值定位到当前【提交线程】管辖的【共享队列】
            WorkQueue wq = workQueues[(workQueues.length - 1) & r & SQMASK];
            if(wq != null) {
                // 移除任务。【提交线程】尝试将指定的task从【共享队列】top处移除，返回值代表是否成功移除
                return wq.tryLockedUnpush(task);
            }
        }
        
        return false;
    }
    
    /*▲ 分发/提交/执行/移除任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 加速完成任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Performs helpComplete for an external submitter.
     */
    // 【提交线程】尝试加速task的完成，并在最终返回任务的状态
    final int externalHelpComplete(CountedCompleter<?> task, int maxTasks) {
        // 获取当前【提交线程】的探测值（可用来计算【共享队列】所在的插槽）
        int r = ThreadLocalRandom.getProbe();
        
        if(workQueues != null && workQueues.length>0){
            WorkQueue w = workQueues[(workQueues.length - 1) & r & SQMASK];
            if(w!=null){
                // 在【共享队列】的top处查找task及task的父任务，如果找到，则执行它，以加速任务的完成
                return w.helpCC(task, maxTasks, true);
            }
        }
        
        return 0;
    }
    
    /**
     * Tries to steal and run tasks within the target's computation.
     * The maxTasks argument supports external usages; internal calls
     * use zero, allowing unbounded steps (external calls trap
     * non-positive values).
     *
     * @param w        caller
     * @param maxTasks if non-zero, the maximum number of other tasks to run
     *
     * @return task status on exit
     */
    // 【工作线程】尝试加速task的完成，并在最终返回任务的状态
    final int helpComplete(WorkQueue w, CountedCompleter<?> task, int maxTasks) {
        if(w!=null){
            // 在【共享队列】的top处查找task及task的父任务，如果找到，则执行它，以加速任务的完成
            return w.helpCC(task, maxTasks, false);
        }
        
        return 0;
    }
    
    /**
     * Helps and/or blocks until the given task is done or timeout.
     * First tries locally helping, then scans other queues for a task produced by one of w's stealers;
     * compensating and blocking if none are found (rescanning if tryCompensate fails).
     *
     * @param w        caller
     * @param task     the task
     * @param deadline for timed waits, if nonzero
     *
     * @return task status on exit
     */
    // 【工作线程】尝试加速task的完成，如果无法加速，则当前的【工作线程】考虑进入wait状态，直到task完成后被唤醒
    final int awaitJoin(WorkQueue w, ForkJoinTask<?> task, long deadline) {
        int s = 0;
        
        // 获取当前线程内的辅助种子
        int seed = ThreadLocalRandom.nextSecondarySeed();
        
        if(w != null && task != null) {
            /*
             * 如果task是CC型任务（总是返回未完成提示），
             * 则尝试加速task的完成，并在最终返回任务的状态
             * 如果任务已完成，返回任务的状态码
             */
            if(task instanceof CountedCompleter) {
                // 尝试将task从【队列】的top处移除，并执行它
                s = w.helpCC((CountedCompleter<?>) task, 0, false);
                if(s<0){
                    return s;
                }
            }
            
            // 如果task存在于【队列】当中，尝试将其从【队列】中移除，并执行它（应对乱序）
            w.tryRemoveAndExec(task);
            
            // 记下当前【队列】的source
            int src = w.source;
            
            // 当前【队列】的ID
            int id = w.id;
            
            // 奇数
            int r = (seed >>> 16) | 1;
            
            // 偶数
            int step = (seed & ~1) | 2;
            
            s = task.status;
            
            // 如果任务还未完成
            while(s >= 0) {
                // 初始化为工作组插槽数量
                int n = workQueues == null ? 0 : workQueues.length;
                
                int m = n - 1;
                
                // 互相窃取（扫描一遍）
                while(n>0) {
                    // 选择一个【工作队列】
                    WorkQueue q = workQueues[r & m];
                    
                    int b;
                    
                    // 如果选择的【队列】刚刚窃取了当前【队列】中任务
                    if(q != null && q.source==id && q.top != (b=q.base)) {
                        if(q.array != null && q.array.length>0) {
                            // 从base处窃取任务（属于原task的子任务，即加速完成）
                            int i = (q.array.length - 1) & b;
                            
                            ForkJoinTask<?> t = (ForkJoinTask<?>) QA.getAcquire(q.array, i);
                            
                            // 当前【列队】也尝试从选择的【队列】中的base处窃取任务
                            if(q.source == id && q.base == b++ && t != null && QA.compareAndSet(q.array, i, t, null)) {
                                q.base = b;
                                
                                // 记下被窃【队列】的ID
                                w.source = q.id;
                                
                                // 如果任务窃取成功，则执行任务
                                t.doExec();
                                
                                // 还原回当前【队列】的ID
                                w.source = src;
                            }
                        }
                        
                        break;
                    } else {
                        r += step;
                        --n;
                    }
                } // while(n>0)
                
                // 如果任务已完成，退出循环
                if((s = task.status)<0) {
                    break;
                }
                
                /*
                 * 至此，如果n==0，status>=0
                 * 说明上面执行了空的扫描，即没有发现是谁盗取了当前【队列】的任务，
                 * 这意味着待完成任务已被分解/执行了，且分解后的任务又被别的线程窃取了
                 * 而且窃取当前【队列】的任务的线程又窃取了别任务
                 */
                if(n == 0) { // empty scan
                    long ms, ns;
                    int block;
                    
                    if(deadline == 0L) {
                        ms = 0L;                       // untimed
                    } else if((ns = deadline - System.nanoTime())<=0L) {
                        break;                         // timeout
                    } else if((ms = TimeUnit.NANOSECONDS.toMillis(ns))<=0L) {
                        ms = 1L;                       // avoid 0 for timed wait
                    }
                    
                    /*
                     * 当前线程没有合适任务时，这里决定是否让其转入wait状态
                     * （不能为当前线程安排执行task子任务之外的任务，不然当task[已完成]时无法得到及时的响应）
                     */
                    block = tryCompensate(w);
                    
                    if(block != 0) {
                        // 如果任务还未完成，将任务状态标记为[等待]，并进入wait状态
                        task.internalWait(ms);
                        CTL.getAndAdd(this, (block>0) ? RC_UNIT : 0L);
                    }
                    
                    s = task.status;
                }
            } // while(s>=0)
        }
        
        return s;
    }
    
    /**
     * If called by a ForkJoinTask operating in this pool, equivalent
     * in effect to {@link ForkJoinTask#helpQuiesce}. Otherwise,
     * waits and/or attempts to assist performing tasks until this
     * pool {@link #isQuiescent} or the indicated timeout elapses.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if quiescent; {@code false} if the
     * timeout elapsed.
     */
    // 等待（促进）所有【工作队列】变为空闲
    public boolean awaitQuiescence(long timeout, TimeUnit unit) {
        // 将当前时间单位下的timeout换算为纳秒
        long nanos = unit.toNanos(timeout);
        
        Thread thread = Thread.currentThread();
        
        if (thread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread)thread;
            if(wt.pool == this){
                // 【工作线程】促进【工作队列】中任务的完成（使【工作线程】走向空闲）
                helpQuiescePool(wt.workQueue);
                return true;
            }
        }
        
        // 在未超时的情形下，随机轮询任务，如果任务存在，则执行它
        for (long startTime = System.nanoTime(); ; ) {
            // 通过轮询，随机从某个【队列】中获取一个任务
            ForkJoinTask<?> task = pollScan(false);
            
            if(task != null) {
                // 执行任务
                task.doExec();
            } else {
                // 判断是否所有【工作线程】当前都处于空闲状态
                if(isQuiescent()) {
                    return true;
                }
                
                // 判断是否超时
                if((System.nanoTime() - startTime)>nanos) {
                    return false;
                }
                
                // 当前线程让出CPU时间片，大家重新抢占执行权
                Thread.yield(); // cannot block
            }
        }
        
    }
    
    /**
     * Waits and/or attempts to assist performing tasks indefinitely
     * until the {@link #commonPool()} {@link #isQuiescent}.
     */
    // 等待（促进）【共享工作池】上的所有【工作队列】变为空闲
    static void quiesceCommonPool() {
        // 等待（促进）所有【工作队列】变为空闲
        common.awaitQuiescence(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }
    
    /**
     * Runs tasks until {@code isQuiescent()}. Rather than blocking
     * when tasks cannot be found, rescans until all others cannot
     * find tasks either.
     */
    // 【工作线程】促进【工作队列】中任务的完成（使【工作线程】走向空闲）
    final void helpQuiescePool(WorkQueue w) {
        int seed = ThreadLocalRandom.nextSecondarySeed();
        int r = seed >>> 16;
        int step = r | 1;   // 奇数步长
        
        int prevSrc = w.source;
        
        for(int source = prevSrc, released = -1; ; ) { // -1 until known
            ForkJoinTask<?> localTask;
            
            // 让【工作线程】尝试从自身的【工作队列】中取出[本地任务]
            while((localTask = w.nextLocalTask()) != null) {
                // 执行任务
                localTask.doExec();
            }
            
            if(w.phase >= 0 && released == -1) {
                released = 1;
            }
            
            boolean quiet = true;
            boolean empty = true;
            
            int n = workQueues == null ? 0 : workQueues.length;
            
            // 从后往前遍历【工作组】
            for(int m = n - 1; n>0; r += step, --n) {
                // 取出遇到的【工作队列】
                WorkQueue q = workQueues[r & m];
                if(q != null) {
                    int qs = q.source;
                    int b = q.base;
                    
                    // 如果该【工作队列】中有任务
                    if(q.top != b) {
                        quiet = empty = false;
                        ForkJoinTask<?>[] a = q.array;
                        int cap, k;
                        int qid = q.id;
                        if(a != null && (cap = a.length)>0) {
                            if(released == 0) {    // increment
                                released = 1;
                                CTL.getAndAdd(this, RC_UNIT);
                            }
                            
                            ForkJoinTask<?> t = (ForkJoinTask<?>) QA.getAcquire(a, k = (cap - 1) & b);
                            if(q.base == b++ && t != null && QA.compareAndSet(a, k, t, null)) {
                                q.base = b;
                                w.source = qid;
                                // 执行任务
                                t.doExec();
                                w.source = source = prevSrc;
                            }
                        }
                        break;
                    }
                    
                    // 如果该【工作队列】处于运行状态
                    if((qs & QUIET) == 0) {
                        quiet = false;
                    }
                }
            }// for
            
            // 如果所有【工作队列】都是空的，且所有工作队列处于park状态
            if(quiet) {
                if(released == 0) {
                    CTL.getAndAdd(this, RC_UNIT);
                }
                w.source = prevSrc;
                break;
            }
            
            // 如果所有【工作队列】都是空的
            if(empty) {
                if(source != QUIET) {
                    w.source = source = QUIET;
                }
                
                if(released == 1) {                 // decrement
                    released = 0;
                    CTL.getAndAdd(this, RC_MASK & -RC_UNIT);
                }
            }
        }// for
    }
    
    /*▲ 加速完成任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工作线程 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tries to add one worker, incrementing ctl counts before doing so, relying on createWorker to back out on failure.
     *
     * @param c incoming ctl value, with total count negative and no idle workers.
     *          On CAS failure, c is refreshed and retried if this holds (otherwise, a new worker is not needed).
     */
    // 添加一个【工作线程】
    private void tryAddWorker(long c) {
        do {
            // 【工作线程】总数+1，活跃数量+1
            long nc = ((RC_MASK & (c + RC_UNIT)) | (TC_MASK & (c + TC_UNIT)));
            
            // 更新ctl
            if(ctl == c && CTL.compareAndSet(this, c, nc)) {
                // 创建【工作线程】和【工作线程】，并与当前【工作池】完成三方绑定
                createWorker();
                break;
            }
            
            c = ctl;
            
        } while((c & ADD_WORKER) != 0L && (int) c == 0);
    }
    
    /**
     * Tries to construct and start one worker. Assumes that total
     * count has already been incremented as a reservation.  Invokes
     * deregisterWorker on any failure.
     *
     * @return true if successful
     */
    /*
     * 创建【工作线程】和【工作线程】，并与当前【工作池】完成三方绑定
     * 随后启动【工作线程】，窃取/执行任务
     * 如果上述过程发生异常，则注销任务
     */
    private boolean createWorker() {
        Throwable ex = null;
        ForkJoinWorkerThread wt = null;
        
        try {
            if(factory != null) {
                // 创建【工作线程】和【工作线程】，并与当前【工作池】完成三方绑定
                wt = factory.newThread(this);
                
                // 如果成功创建了【工作线程】
                if(wt != null){
                    // 启动【工作线程】（与此同时，当前线程也没闲着，要继续执行下去）
                    wt.start();
                    
                    return true;
                }
            }
        } catch(Throwable e) {
            ex = e;
        }
        
        deregisterWorker(wt, ex);
        
        return false;
    }
    
    /**
     * Callback from ForkJoinWorkerThread constructor to establish and record its WorkQueue.
     *
     * @param wt the worker thread
     *
     * @return the worker's queue
     */
    // 为指定的【工作线程】创建【工作队列】，并返回该【工作队列】
    final WorkQueue registerWorker(ForkJoinWorkerThread wt) {
        /*
         * 将【工作线程】设置为▉守护线程▉
         * 这意味着所有非守护线程结束后，【工作线程】也将结束，无论其处于什么状态
         */
        wt.setDaemon(true);  // configure thread
        
        if(ueh != null) {
            // 设置未捕获异常处理器
            wt.setUncaughtExceptionHandler(ueh);
        }
        
        // 为【工作线程】创建一个【工作队列】
        WorkQueue wq = new WorkQueue(this, wt);
        
        if(workerNamePrefix != null) {
            int tid = 0;
            
            synchronized(workerNamePrefix) {
                // 魔数递增
                indexSeed += SEED_INCREMENT;
                
                if(workQueues != null && workQueues.length>1) {
                    // 【工作组】容量是2的冪
                    int len = workQueues.length;
                    
                    // 生成[0, len)之间的奇数，作为【工作线程】在【工作组】中的索引
                    tid = ((indexSeed << 1) | 1) & (len-1);
                    
                    // 初始化遍历次数（容量的一半，因为只需要遍历奇数插槽）
                    int probes = len >>> 1;
                    
                    // 在【工作组】上查找空的插槽
                    while(true) {
                        // 插槽为空，或者这个插槽处的【工作队列】已报废，则直接在这里放置新的【工作队列】
                        if(workQueues[tid]==null || workQueues[tid].phase == QUIET) {
                            break;
                        }
                        
                        // 遍历结束
                        if(--probes == 0) {
                            tid = len | 1;                // resize below
                            break;
                        }
                        
                        // 前进到下一个奇数（越界后会轮转）
                        tid = (tid + 2) & (len-1);
                    }
                    
                    // 计算【工作队列】ID前16位
                    int idbits = (mode & FIFO) | (indexSeed & ~(SMASK | FIFO | DORMANT));
                    
                    // 生成【工作队列】的ID（后16位保存【工作队列】在【工作组】上的索引）
                    wq.id = idbits | tid;      // now publishable
                    
                    // 此处使得：phase>0
                    wq.phase = wq.id;
                    
                    // 如果无需扩容
                    if(tid<len) {
                        workQueues[tid] = wq;
                        
                        // 放不下了，需要给【工作组】扩容
                    } else {
                        // 容量加倍
                        int an = len << 1;
                        
                        WorkQueue[] wqs = new WorkQueue[an];
                        wqs[tid] = wq;
                        
                        // 遍历奇数位置，移动【工作队列】
                        for(int i=1; i<len; i+=2){
                            wqs[i] = workQueues[i];         // copy worker
                        }
                        
                        // 遍历偶数位置，移动【共享队列】
                        for(int i=0; i<len; i+=2){
                            WorkQueue w = workQueues[i];    // copy external queue
                            
                            //
                            if(w != null) {                 // position may change
                                wqs[w.id & (an-1) & SQMASK] = w;
                            }
                        }
                        
                        workQueues = wqs;
                    }
                }
            }
            
            // 为【工作线程】设置名称
            wt.setName(workerNamePrefix.concat(Integer.toString(tid)));
        }
        
        return wq;
    }
    
    /**
     * Final callback from terminating worker, as well as upon failure to construct or start a worker.
     * Removes record of worker from array, and adjusts counts.
     * If pool is shutting down, tries to complete termination.
     *
     * @param wt the worker thread, or null if construction failed
     * @param ex the exception causing failure, or null if none
     */
    // 移除【工作队列】，并标记【工作队列】中的任务为DONE|ABNORMAL状态，最后还要处理异常
    final void deregisterWorker(ForkJoinWorkerThread wt, Throwable ex) {
        WorkQueue wq = null;
        
        int phase = 0;
        
        // 如果给的的【工作线程】有效且包含【工作队列】
        if(wt != null && (wq = wt.workQueue) != null) {
            
            long ns = (long) wq.nsteals & 0xffffffffL;
            
            // 将该【工作线程】管理的【工作队列】从【工作组】中移除
            if(workerNamePrefix != null) {
                synchronized(workerNamePrefix) {
                    if(workQueues != null && workQueues.length>0) {
                        int i = wq.id & (workQueues.length-1);    // remove index from array
                        if(workQueues[i] == wq){
                            workQueues[i] = null;
                        }
                    }
                    
                    // 移除该【工作队列】时，需要统计其中包含的窃取来的任务数量
                    stealCount += ns;
                }
            }
            
            // 记录被移除的【工作队列】的版本号
            phase = wq.phase;
        }
        
        // else pre-adjusted
        if(phase != QUIET) {
            boolean boo;
            
            // decrement counts
            do {
                long c = ctl;
                boo = CTL.weakCompareAndSet(this, c, ((RC_MASK & (c - RC_UNIT)) | (TC_MASK & (c - TC_UNIT)) | (SP_MASK & c)));
            } while(!boo);
        }
        
        // 取消当前【工作队列】中的所有任务，即标记所有任务为DONE|ABNORMAL状态
        if(wq != null) {
            wq.cancelAll();                             // cancel remaining tasks
        }
        
        // 尝试关闭普通工作池
        if(!tryTerminate(false, false) && wq != null && wq.array != null) {
            // 创建/唤醒【工作线程】
            signalWork();
        }
        
        // help clean on way out
        if(ex == null) {
            // 清理失效的"异常记录"。即如果"任务"被GC回收，则将其所在的异常记录从异常记录哈希表中移除
            ForkJoinTask.helpExpungeStaleExceptions();
        } else {
            // 抛出异常
            ForkJoinTask.rethrow(ex);
        }
    }
    
    /**
     * Tries to create or release a worker if too few are running.
     */
    // 创建/唤醒【工作线程】
    final void signalWork() {
        // 进入死循环
        while(true) {
            /*
             * 如果【工作线程】已饱和，就退出循环。
             * 饱和的标志是：【工作线程】数量==并行度
             */
            if(ctl >= 0L) {
                break;
            }
            
            int sp = (int) ctl;
            
            /*
             * 如果没有空闲的【工作线程】，就需要尝试开启新的【工作线程】
             * 空闲的标志是：ctl的低32位为0，如果不为0，则会存储最近一个空闲的【工作线程】的信息
             */
            if(sp == 0) {
                // 如果【工作线程】数量没超过阙值，这个阙值与构造方法的corePoolSize参数有关
                if((ctl & ADD_WORKER) != 0L) {
                    // 添加一个【工作线程】
                    tryAddWorker(ctl);
                }
                
                break;
            }
            
            /* 至此，说明存在空闲的【工作线程】（之前进入了park状态） */
            
            // unstarted/terminated
            if(workQueues == null) {
                break;
            }
            
            // terminated
            if(workQueues.length <= (sp & SMASK)) {
                break;
            }
            
            // 获取最近转入park状态的【工作线程】
            WorkQueue wq = workQueues[sp & SMASK];
            
            if(wq == null) {
                break;
            }                               // terminating
            
            // 取消待唤醒标记
            int np = sp & ~UNSIGNALLED;
            
            /*
             * (ctl + RC_UNIT) & UC_MASK 代表恢复了一个阻塞线程，活跃的线程数增一
             * wq.stackPred & SP_MASK 代表上上个转入park状态（空闲）的【工作线程】的版本号
             */
            long nc = ((ctl + RC_UNIT) & UC_MASK) | (wq.stackPred & SP_MASK);
            
            if(sp == wq.phase && CTL.compareAndSet(this, ctl, nc)) {
                // 恢复版本号
                wq.phase = np;
                if(wq.owner != null && wq.source<0) {
                    // 唤醒刚刚转入park状态（空闲）的【工作线程】
                    LockSupport.unpark(wq.owner);
                }
                break;
            }
        } // while(true)
    }
    
    /**
     * Top-level runloop for workers, called by ForkJoinWorkerThread.run.
     * See above for explanation.
     */
    /*
     * 【工作线程】开始工作：窃取并执行任务
     * 如果无法窃取到任务，也无法从自身的【工作队列】中获取任务，
     * 则【工作线程】会转入park状态
     *
     * wq：当前【工作线程】管理的【工作队列】
     */
    final void runWorker(WorkQueue wq) {
        // 获取随机数（用来计算【工作组】的插槽位置）
        int r = (wq.id ^ ThreadLocalRandom.nextSecondarySeed()) | FIFO; // rng
        
        // 为【工作线程】的【工作队列】初始化【工作组】
        wq.array = new ForkJoinTask<?>[INITIAL_QUEUE_CAPACITY]; // initialize
        
        for(; ; ) {
            // 【工作线程】随机从某个插槽开始扫描【工作组】，执行窃取操作，直到无法再获取到排队任务才结束操作
            boolean meetTask = scan(wq, r);
            
            // 如果窃取过程中遇到过排队任务（有没有执行过不确定）
            if(meetTask) {
                // 更新随机数
                r ^= r << 13;
                r ^= r >>> 17;
                r ^= r << 5; // move (xorshift)
                
                // 如果窃取过程中没有发现排队任务
            } else {
                // enqueue, then rescan
                int pase = wq.phase;
                
                if(pase >= 0) {
                    // 添加了阻塞标记UNSIGNALLED，使得：phase<0
                    wq.phase = (pase + SS_SEQ) | UNSIGNALLED;
                    
                    long c, nc;
                    do {
                        // 记录此时的ctl（ctl随即可能被别的线程修改）
                        c = ctl;
                        
                        // 记录前一个陷入阻塞的【工作线程】的版本号
                        wq.stackPred = (int)c;
                        
                        // nc的前32位取自ctl（活跃线程数减一），后32位取自phase
                        nc = ((c - RC_UNIT) & UC_MASK) | (wq.phase & SP_MASK);
                        
                        // 更新ctl为nc
                    } while(!CTL.weakCompareAndSet(this, c, nc));
                    
                    // already queued
                } else {
                    // 清除线程的中断标记
                    Thread.interrupted();             // clear before park
                    
                    // 进入park前，会使得source<0
                    wq.source = DORMANT;               // enable signal
                    
                    long c = ctl;
                    
                    // 计算活跃的线程数量
                    int rc = (mode & SMASK) + (int) (c >> RC_SHIFT);
                    
                    // 如果工作池已停止
                    if(mode<0) {
                        break;
                    }
                    
                    // 如果没有活跃的【工作线程】，且该普通工作池已经经历了一次tryTerminate()
                    if(rc<=0 && (mode & SHUTDOWN) != 0 && tryTerminate(false, false)) {
                        break;                        // quiescent shutdown
                    }
                    
                    /*
                     * 如果没有活跃的【工作线程】，(经过上面的if流程后减没了)
                     * 且当前【工作线程】之前还有陷入park的【工作线程】，（当前【工作线程】不是第一个要陷入阻塞的）
                     * 且最近一个陷入park状态的线程是当前的【工作线程】
                     */
                    if(rc<=0 && wq.stackPred != 0 && pase == (int)c) {
                        // 尝试【工作线程】总量减一
                        long nc = ((c - TC_UNIT) & UC_MASK ) | (SP_MASK & wq.stackPred);
                        
                        long d = keepAlive + System.currentTimeMillis();
                        
                        // 休眠一段时间后自动醒来
                        LockSupport.parkUntil(this, d);
                        
                        /* drop on timeout if all idle */
                        // 如果这段时间内ctl没变化，说明没有新增的park线程，此时更新ctl，尝试缩减【工作线程总量】
                        if(ctl == c && d - System.currentTimeMillis()<=TIMEOUT_SLOP && CTL.compareAndSet(this, c, nc)) {
                            // 闲太久了，这个线程稍后就退出了，而【工作队列】也被标记为QUIET状态，即该队列已经报废
                            wq.phase = QUIET;
                            break;
                        }
                    } else if(wq.phase<0) {
                        LockSupport.park(this);       // OK if spuriously woken
                    }
                    
                    wq.source = 0;                    // disable signal
                }// if(pase < 0)
            } // if(meetTask)
        } // for(; ; )
    }
    
    /**
     * Scans for and if found executes one or more top-level tasks from a queue.
     *
     * @return true if found an apparently non-empty queue, and possibly ran task(s).
     */
    /*
     * 【工作线程】扫描【工作组】，执行窃取操作（也会从本地获取任务）
     *
     * 开始时，【工作线程】会带着一个空的【工作队列】wq来接收窃取到的task
     *
     * 在整个扫描过程中，【工作线程】可能会遇到[本地任务]和[外部任务]，
     * 直到无法再获取到排队任务才结束操作
     *
     * 返回true 代表本轮操作中发现了排队任务（但不一定抢到执行权）
     * 返回false代表本轮操作中未发现排队任务
     */
    private boolean scan(WorkQueue wq, int r) {
        int len;
        
        // 存在有效的【工作组】
        if(workQueues != null && (len = workQueues.length)>0 && wq != null) {
            // 由随机数r生成【工作组】的一个随机插槽
            int i = r & (len - 1);
            
            /*
             * 遍历整个【工作组】，直到找到一个非空的【队列】，并从中窃取任务
             * 如果当前不存在非空的【队列】，则退出循环
             */
            while(true) {
                // 随机定位到【工作组】的某个插槽（这个插槽可能是【工作队列】/【共享队列】），并准备窃取它的task
                WorkQueue w = workQueues[i];
                
                // 遇到非空的【队列】
                if(w != null && w.top != w.base) {
                    ForkJoinTask<?> task;
                    
                    // 确保【队列】的任务组有效
                    if(w.array != null && w.array.length>0) {
                        // 获取base处的索引
                        int index = w.base & (w.array.length - 1);
                        
                        // 获取base处的任务（窃取到了[外部任务]）
                        task = (ForkJoinTask<?>) QA.getAcquire(w.array, index);
                        
                        // 如果该任务有效，则将其从原来的任务组中移除
                        if(task != null && QA.compareAndSet(w.array, index, task, null)) {
                            // base游标递增，这个窃取过程类似出队，先来的先被窃取
                            w.base++;
                            
                            // 记录窃取的任务所在【队列】的id
                            wq.source = w.id;
                            
                            // 如果被窃取【队列】中还存在其他排队任务
                            if(w.top - w.base>0) {
                                // 创建/唤醒【工作线程】，继续窃取（但下次不一定窃取到这个【队列】）
                                signalWork();
                            }
                            
                            // 【工作线程】执行窃取操作
                            wq.topLevelExec(task, w, r & ((len << TOP_BOUND_SHIFT) - 1));
                        }
                    }
                    
                    return true;
                    
                    // 如果没有找到非空的【队列】，则前进到【工作组】的下一个插槽处
                } else if(--len>0) {
                    i = (i + 1) & (len-1);
                } else {
                    // 找遍了【工作组】也没有符合要求的【工作队列】
                    break;
                }
            }// while(true)
        }
        
        return false;
    }
    
    /**
     * Tries to decrement counts (sometimes implicitly) and possibly arrange for a compensating worker in preparation for blocking:
     * If not all core workers yet exist, creates one, else if any are unreleased (possibly including caller) releases one,
     * else if fewer than the minimum allowed number of workers running,
     * checks to see that they are all active, and if so creates an extra worker unless over maximum limit and policy is to saturate.
     * Most of these steps can fail due to interference, in which case 0 is returned so caller will retry.
     * A negative return value indicates that the caller doesn't need to re-adjust counts when later unblocked.
     *
     * @return 1: block then adjust, -1: block without adjust, 0 : retry
     */
    /*
     * 补偿
     *
     * w是当前【工作线程】中的【工作队列】
     *
     * 返回值：
     * -1 阻塞，不调整
     *  1 阻塞，调整
     *  0 重试
     */
    private int tryCompensate(WorkQueue w) {
        WorkQueue[] ws = workQueues;
        
        long c = ctl;
        
        int total = (short) (c >>> TC_SHIFT);
        
        // 【工作线程】总量已到阙值
        if(total >= 0) {
            int n, sp;
            
            // disabled
            if(ws == null || (n = ws.length)<=0 || w == null) {
                return 0;
            }
            
            /* replace or release */
            // 获取上一个转为park的【工作线程】的版本号
            if((sp = (int) c) != 0) {
                // 选择上一个转为park的【工作队列】
                WorkQueue v = ws[sp & (n - 1)];
                
                // 获取当前【工作队列】的版本号
                int wp = w.phase;
                
                long uc;
                
                // 当前的【工作线程】之前从park状态中自己醒来
                if(wp<0){
                    // 需要还原/补偿活跃度
                    uc = (c + RC_UNIT) & UC_MASK;
                } else {
                    uc = c & UC_MASK;
                }
                
                // 取消版本号上的park标记
                int np = sp & ~UNSIGNALLED;
                
                if(v != null) {
                    // 上一个转为park的【工作线程/队列】的版本号
                    int vp = v.phase;
                    
                    Thread vt = v.owner;
                    
                    // 上上个转为park的【工作线程】的戳记
                    long nc = ((long) v.stackPred & SP_MASK) | uc;
                    
                    // 使用nc更新ctl，这个过程就像链表删除一样
                    if(vp == sp && CTL.compareAndSet(this, c, nc)) {
                        // 还原park前的版本号
                        v.phase = np;
                        
                        // 唤醒上一个转为park的【工作线程】
                        if(vt != null && v.source<0) {
                            LockSupport.unpark(vt);
                        }
                        
                        /*
                         * wp<0时，说明上一个转为park的【工作线程】就是当前线程，
                         * 此时返回-1，代表后续不需要调整活跃度了，因为上面已经补偿过了
                         *
                         * wp>=0时，说明唤醒了一个正处于park状态的【工作线程】，
                         * 此时返回1，代表后续需要调整活跃度（增一）
                         */
                        return (wp<0) ? -1 : 1;
                    }
                }
                
                return 0;
            }
            
            /* reduce parallelism */
            // 如果目前没有【工作线程】陷入park，在还没到最小活跃度的情形下尝试减少活跃度
            if((int) (c >> RC_SHIFT) - (short) (bounds & SMASK)>0) {
                // 尝试减少一个活跃度
                long nc = (((c - RC_UNIT) & RC_MASK) | (~RC_MASK & c));
                
                // 返回1表示成功减少了一个活跃度，后续需要加回来
                return CTL.compareAndSet(this, c, nc) ? 1 : 0;
            }
            
            /* validate */
            int pc = mode & SMASK;  // 并行度
            int tc = pc + total;    // 总线程数
            int bc = 0;
            
            boolean unstable = false;
            
            // 遍历所有【工作线程】
            for(int i = 1; i<n; i += 2) {
                if(ws[i] != null) {
                    // 当前【工作线程】正在窃取任务
                    if(ws[i].source == 0) {
                        unstable = true;
                        break;
                    }
                    
                    --tc;
                    
                    if(ws[i].owner != null) {
                        Thread.State s = ws[i].owner.getState();
                        
                        if(s == Thread.State.BLOCKED || s == Thread.State.WAITING){
                            ++bc;            // worker is blocking
                        }
                    }
                }
            }
            
            // inconsistent
            if(unstable || tc != 0 || ctl != c) {
                return 0;
            }
            
            if(total + pc >= MAX_CAP || total >= (bounds >>> SWIDTH)) {
                if(saturate != null && saturate.test(this)) {
                    return -1;
                }
                
                if(bc<pc) {          // lagging
                    Thread.yield();  // for retry spins
                    return 0;
                }
                
                throw new RejectedExecutionException("Thread limit exceeded replacing blocked worker");
            }
        }
        
        // 增加【工作线程】总量上限
        long nc = ((c + TC_UNIT) & TC_MASK) | (c & ~TC_MASK); // expand pool
        
        return CTL.compareAndSet(this, c, nc) && createWorker() ? 1 : 0;
    }
    
    /*▲ 工作线程 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭工作池 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Possibly initiates and/or completes termination.
     *
     * @param now if true, unconditionally terminate, else only if no work and no active workers
     * @param enable if true, terminate when next possible
     *
     * @return true if terminating or terminated
     */
    /*
     * 尝试关闭【普通工作池】，会经历三个阶段SHUTDOWN、STOP、Terminate
     *
     * now   ：是否立即将普通工作池标记为STOP，即使仍有正在进行的任务
     * enable：是否将普通工作池标记为SHUTDOWN
     *
     * 返回true表示【普通工作池】正在Terminate或已经完成Terminate
     */
    private boolean tryTerminate(boolean now, boolean enable) {
        // 3 phases: try to set SHUTDOWN, then STOP, then TERMINATED
        int md;
        
        /* 阶段一：关闭 */
        while(((md = mode) & SHUTDOWN) == 0) {
            // 共享工作池无法关闭
            if(!enable || this == common) {       // cannot shutdown
                return false;
            } else {
                MODE.compareAndSet(this, md, md | SHUTDOWN);
            }
        }
        
        /* 阶段二：停止 */
        while(((md = mode) & STOP) == 0) {       // try to initiate termination
            /*
             * 如果不需要立即停止，
             * 那么检查线程是否处于空闲状态，或者任务已被执行完，
             * 两者至少满足其一时才能停止工作池
             */
            if(!now) {                           // check if quiescent & empty
                for(long oldSum = 0L; ; ) {      // repeat until stable
                    boolean running = false;
                    long checkSum = ctl;
                    
                    WorkQueue[] ws = workQueues;
                    
                    // 如果存在活跃线程
                    if((md & SMASK) + (int) (checkSum >> RC_SHIFT)>0) {
                        running = true;
                    } else if(ws != null) {
                        // 遍历【工作组】
                        for(WorkQueue w : ws) {
                            if(w != null) {
                                int s = w.source, p = w.phase;
                                int d = w.id, b = w.base;
                                if(b != w.top || ((d & 1) == 1 && (s >= 0 || p >= 0))) {
                                    running = true;
                                    break;     // working, scanning, or have work
                                }
                                checkSum += (((long) s << 48) + ((long) p << 32) + ((long) b << 16) + (long) d);
                            }
                        }// for
                    }
                    
                    if(((md = mode) & STOP) != 0) {
                        break;                 // already triggered
                    } else if(running) {
                        return false;
                    } else if(workQueues == ws && oldSum == (oldSum = checkSum)) {
                        break;
                    }
                } // for
            }
            
            if((md & STOP) == 0) {
                MODE.compareAndSet(this, md, md | STOP);
            }
        }
        
        /* 阶段三：终止 */
        while(((md = mode) & TERMINATED) == 0) { // help terminate others
            for(long oldSum = 0L; ; ) {          // repeat until stable
                WorkQueue[] ws;
                
                long checkSum = ctl;
                if((ws = workQueues) != null) {
                    for(WorkQueue w : ws) {
                        if(w != null) {
                            ForkJoinWorkerThread wt = w.owner;
                            // 取消当前【队列】中的所有任务，即标记所有任务为DONE|ABNORMAL状态
                            w.cancelAll();        // clear queues
                            if(wt != null) {
                                try {             // unblock join or park
                                    wt.interrupt();
                                } catch(Throwable ignore) {
                                }
                            }
                            
                            checkSum += ((long) w.phase << 32) + w.base;
                        }
                    }
                }
                
                if(((md = mode) & TERMINATED) != 0 || (workQueues == ws && oldSum == (oldSum = checkSum))) {
                    break;
                }
            }// for
            
            if((md & TERMINATED) != 0) {
                break;
            } else if((md & SMASK) + (short) (ctl >>> TC_SHIFT)>0) {
                break;
            } else if(MODE.compareAndSet(this, md, md | TERMINATED)) {
                synchronized(this) {
                    notifyAll();                  // for awaitTermination
                }
                break;
            }
        }
        
        return true;
    }
    
    /**
     * Possibly initiates an orderly shutdown in which previously
     * submitted tasks are executed, but no new tasks will be
     * accepted. Invocation has no effect on execution state if this
     * is the {@link #commonPool()}, and no additional effect if
     * already shut down.  Tasks that are in the process of being
     * submitted concurrently during the course of this method may or
     * may not be rejected.
     *
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    // 尝试关闭【工作池】（其mode必定会带有SHUTDOWN标记）
    public void shutdown() {
        checkPermission();
        tryTerminate(false, true);
    }
    
    /**
     * Possibly attempts to cancel and/or stop all tasks, and reject
     * all subsequently submitted tasks.  Invocation has no effect on
     * execution state if this is the {@link #commonPool()}, and no
     * additional effect if already shut down. Otherwise, tasks that
     * are in the process of being submitted or executed concurrently
     * during the course of this method may or may not be
     * rejected. This method cancels both existing and unexecuted
     * tasks, in order to permit termination in the presence of task
     * dependencies. So the method always returns an empty list
     * (unlike the case for some other Executors).
     *
     * @return an empty list
     * @throws SecurityException if a security manager exists and
     *         the caller is not permitted to modify threads
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    // 尝试关闭【工作池】（其mode必定会带有SHUTDOWN、STOP标记），返回一个空列表
    public List<Runnable> shutdownNow() {
        checkPermission();
        tryTerminate(true, true);
        return Collections.emptyList();
    }
    
    /**
     * Returns {@code true} if this pool has been shut down.
     *
     * @return {@code true} if this pool has been shut down
     */
    // 【工作池】是否经历完了关闭的第一阶段
    public boolean isShutdown() {
        return (mode & SHUTDOWN) != 0;
    }
    
    /**
     * Returns {@code true} if the process of termination has
     * commenced but not yet completed.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, or are waiting for I/O,
     * causing this executor not to properly terminate. (See the
     * advisory notes for class {@link ForkJoinTask} stating that
     * tasks should not normally entail blocking operations.  But if
     * they do, they must abort them on interrupt.)
     *
     * @return {@code true} if terminating but not yet terminated
     */
    // 【工作池】是否经历完了关闭的第二阶段
    public boolean isTerminating() {
        int md = mode;
        return (md & STOP) != 0 && (md & TERMINATED) == 0;
    }
    
    /**
     * Returns {@code true} if all tasks have completed following shut down.
     *
     * @return {@code true} if all tasks have completed following shut down
     */
    // 【工作池】是否经历完了关闭的第三阶段
    public boolean isTerminated() {
        return (mode & TERMINATED) != 0;
    }
    
    /**
     * Blocks until all tasks have completed execution after a
     * shutdown request, or the timeout occurs, or the current thread
     * is interrupted, whichever happens first. Because the {@link
     * #commonPool()} never terminates until program shutdown, when
     * applied to the common pool, this method is equivalent to {@link
     * #awaitQuiescence(long, TimeUnit)} but always returns {@code false}.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if this executor terminated and
     *         {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    // 等待（促进）【工作池】关闭
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        
        // 【共享工作池】无法关闭，所以这里总是返回false
        if(this == common) {
            // 等待（促进）所有【工作队列】变为空闲
            awaitQuiescence(timeout, unit);
            return false;
        }
        
        long nanos = unit.toNanos(timeout);
        // 工作池是否经历完了关闭的第三阶段
        if(isTerminated()) {
            return true;
        }
        
        // 已超时
        if(nanos<=0L) {
            return false;
        }
        
        long deadline = System.nanoTime() + nanos;
        synchronized(this) {
            for(; ; ) {
                if(isTerminated()) {
                    return true;
                }
                
                if(nanos<=0L) {
                    return false;
                }
                
                long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
                
                wait(millis>0L ? millis : 1L);
                
                nanos = deadline - System.nanoTime();
            }
        }
    }
    
    /*▲ 关闭工作池 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Scans for and returns a polled task, if available.
     * Used only for untracked polls.
     *
     * @param submissionsOnly if true, only scan submission queues
     */
    // 通过轮询，随机从某个【队列】中获取一个任务
    private ForkJoinTask<?> pollScan(boolean submissionsOnly) {
rescan:
        while((mode & STOP) == 0 && workQueues != null && workQueues.length>0) {
            int r = ThreadLocalRandom.nextSecondarySeed();
            int h = r >>> 16;
            
            int origin, step;
            
            if(submissionsOnly) {
                // 遍历偶数插槽
                origin = (r & ~1) & (workQueues.length - 1);         // even indices and steps
                step = (h & ~1) | 2;
            } else {
                // 遍历所有插槽
                origin = r & (workQueues.length - 1);
                step = h | 1;
            }
            
            boolean nonempty = false;
            for(int i = origin, oldSum = 0, checkSum = 0; ; ) {
                WorkQueue wq = workQueues[i];
                
                if(wq != null) {
                    int b;
                    
                    if(wq.top - (b = wq.base)>0) {
                        nonempty = true;
                        
                        // 窃取【队列】base处的task，如果取不到，就释放CPU时间片，各线程重新抢占执行权
                        ForkJoinTask<?> task = wq.poll();
                        if(task != null) {
                            return task;
                        }
                    } else {
                        checkSum += b + wq.id;
                    }
                }
                
                if((i = (i + step) & (workQueues.length - 1)) == origin) {
                    if(!nonempty && oldSum == (oldSum = checkSum)) {
                        break rescan;
                    }
                    
                    checkSum = 0;
                    nonempty = false;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Gets and removes a local or stolen task for the given worker.
     *
     * @return a task, if available
     */
    // 获取一个[本地任务]或窃取的[外部任务]
    final ForkJoinTask<?> nextTaskFor(WorkQueue w) {
        if(w!=null){
            // 让【工作线程】尝试从自身的【工作队列】中取出[本地任务]
            ForkJoinTask task = w.nextLocalTask();
            if(task!=null){
                return task;
            }
        }
        
        // 通过轮询，随机从某个【队列】中获取一个任务
        return pollScan(false);
    }
    
    /**
     * Removes and returns the next unexecuted submission if one is
     * available.  This method may be useful in extensions to this
     * class that re-assign work in systems with multiple pools.
     *
     * @return the next submission, or {@code null} if none
     */
    // 随机从某个【共享队列】中获取一个任务
    protected ForkJoinTask<?> pollSubmission() {
        // 通过轮询，随机从某个【共享队列】中获取一个任务
        return pollScan(true);
    }
    
    // 包装Runnable为AdaptedRunnable型任务
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ForkJoinTask.AdaptedRunnable<T>(runnable, value);
    }
    
    // 包装Callable为AdaptedCallable型任务
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ForkJoinTask.AdaptedCallable<T>(callable);
    }
    
    /*▲ 获取任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取队列 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns common pool queue for an external thread.
     */
    // 获取当前【提交线程】管辖的【共享队列】（从【共享工作池】中获取）
    static WorkQueue commonSubmitterQueue() {
        // 获取当前【提交线程】的探测值（可用来计算【共享队列】所在的插槽）
        int r = ThreadLocalRandom.getProbe();
        
        if(common != null && common.workQueues != null){
            int n = common.workQueues.length;
            if(n>0){
                return common.workQueues[(n - 1) & r & SQMASK];
            }
        }
        
        return null;
    }
    
    /*▲ 获取队列 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 统计 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of worker threads that have started but not
     * yet terminated.  The result returned by this method may differ
     * from {@link #getParallelism} when threads are created to
     * maintain parallelism when others are cooperatively blocked.
     *
     * @return the number of worker threads
     */
    // 返回当前【工作线程】的总数
    public int getPoolSize() {
        return ((mode & SMASK) + (short)(ctl >>> TC_SHIFT));
    }
    
    /**
     * Returns an estimate of the number of worker threads that are
     * not blocked waiting to join tasks or for other managed
     * synchronization. This method may overestimate the
     * number of running threads.
     *
     * @return the number of worker threads
     */
    // 获取当前运行着的【工作线程】的数量（即不包括陷入阻塞的线程）
    public int getRunningThreadCount() {
        WorkQueue w;
        
        VarHandle.acquireFence();
        
        int rc = 0;
        
        if (workQueues != null) {
            // 遍历奇数插槽
            for (int i = 1; i < workQueues.length; i += 2) {
                w = workQueues[i];
                
                // 判断【工作线程】是否在阻塞状态（包括park和wait以及争用态）
                if (w!=null && w.isApparentlyUnblocked()) {
                    ++rc;
                }
            }
        }
        
        return rc;
    }
    
    /**
     * Returns an estimate of the number of threads that are currently
     * stealing or executing tasks. This method may overestimate the
     * number of active threads.
     *
     * @return the number of active threads
     */
    // 获取当前活跃的线程数量（抑制了负数）
    public int getActiveThreadCount() {
        int r = (mode & SMASK) + (int)(ctl >> RC_SHIFT);
        return (r <= 0) ? 0 : r; // suppress momentarily negative values
    }
    
    /**
     * Returns an estimate of the total number of tasks stolen from one thread's work queue by another.
     * The reported value underestimates the actual total number of steals when the pool is not quiescent.
     * This value may be useful for monitoring and tuning fork/join programs:
     * in general, steal counts should be high enough to keep threads busy, but low enough to avoid overhead and contention across threads.
     *
     * @return the number of steals
     */
    /*
     * 统计所有【工作线程】累计窃取的任务数量，返回的是静态值，稍后可能会发生变化
     *
     * 这个值用来评估算法性能：
     * 窃取的任务数量高说明线程被充分利用
     * 窃取的任务数量低说明线程争用不明显
     */
    public long getStealCount() {
        long count = stealCount;
        
        if (workQueues != null) {
            // 遍历奇数插槽
            for (int i = 1; i < workQueues.length; i += 2) {
                
                if (workQueues[i] != null) {
                    count += (long)workQueues[i].nsteals & 0xffffffffL;
                }
            }
        }
        
        return count;
    }
    
    /**
     * Returns an estimate of the total number of tasks currently held
     * in queues by worker threads (but not including tasks submitted
     * to the pool that have not begun executing). This value is only
     * an approximation, obtained by iterating across all threads in
     * the pool. This method may be useful for tuning task
     * granularities.
     *
     * @return the number of queued tasks
     */
    // 统计所有【工作队列】中存放的任务数
    public long getQueuedTaskCount() {
        VarHandle.acquireFence();
        
        int count = 0;
        if (workQueues != null) {
            // 遍历奇数插槽
            for (int i = 1; i < workQueues.length; i += 2) {
                if (workQueues[i] != null) {
                    // 累加【队列】中的任务数（近似）
                    count += workQueues[i].queueSize();
                }
            }
        }
        
        return count;
    }
    
    /**
     * Returns an estimate of the number of tasks submitted to this
     * pool that have not yet begun executing.  This method may take
     * time proportional to the number of submissions.
     *
     * @return the number of queued submissions
     */
    // 统计所有【共享队列】中存放的任务数
    public int getQueuedSubmissionCount() {
        VarHandle.acquireFence();
        
        int count = 0;
        if (workQueues != null) {
            // 遍历偶数插槽
            for (int i = 0; i < workQueues.length; i += 2) {
                if (workQueues[i] != null) {
                    // 累加【队列】中的任务数（近似）
                    count += workQueues[i].queueSize();
                }
            }
        }
        return count;
    }
    
    /**
     * Returns a cheap heuristic guide for task partitioning when
     * programmers, frameworks, tools, or languages have little or no
     * idea about task granularity.  In essence, by offering this
     * method, we ask users only about tradeoffs in overhead vs
     * expected throughput and its variance, rather than how finely to
     * partition tasks.
     *
     * In a steady state strict (tree-structured) computation, each
     * thread makes available for stealing enough tasks for other
     * threads to remain active. Inductively, if all threads play by
     * the same rules, each thread should make available only a
     * constant number of tasks.
     *
     * The minimum useful constant is just 1. But using a value of 1
     * would require immediate replenishment upon each steal to
     * maintain enough tasks, which is infeasible.  Further,
     * partitionings/granularities of offered tasks should minimize
     * steal rates, which in general means that threads nearer the top
     * of computation tree should generate more than those nearer the
     * bottom. In perfect steady state, each thread is at
     * approximately the same level of computation tree. However,
     * producing extra tasks amortizes the uncertainty of progress and
     * diffusion assumptions.
     *
     * So, users will want to use values larger (but not much larger)
     * than 1 to both smooth over transient shortages and hedge
     * against uneven progress; as traded off against the cost of
     * extra task overhead. We leave the user to pick a threshold
     * value to compare with the results of this call to guide
     * decisions, but recommend values such as 3.
     *
     * When all threads are active, it is on average OK to estimate
     * surplus strictly locally. In steady-state, if one thread is
     * maintaining say 2 surplus tasks, then so are others. So we can
     * just use estimated queue length.  However, this strategy alone
     * leads to serious mis-estimates in some non-steady-state
     * conditions (ramp-up, ramp-down, other stalls). We can detect
     * many of these by further considering the number of "idle"
     * threads, that are known to have zero queued tasks, so
     * compensate by a factor of (#idle/#active) threads.
     */
    // 估算当前【工作线程】保留的本地排队任务数量
    static int getSurplusQueuedTaskCount() {
        Thread t = Thread.currentThread();
        
        if(t instanceof ForkJoinWorkerThread){
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
            
            if(wt.pool!=null && wt.workQueue!= null){
                int p = wt.pool.mode & SMASK;
                
                // 计算当前活跃的线程数
                int a = p + (int) (wt.pool.ctl >> RC_SHIFT);
                
                // 计算当前【工作线程】所辖的【工作队列】中排队的任务数
                int n = wt.workQueue.top - wt.workQueue.base;
                
                return n - (a>(p >>>= 1) ? 0 : a>(p >>>= 1) ? 1 : a>(p >>>= 1) ? 2 : a>(p >>>= 1) ? 4 : 8);
            }
        }
        
        return 0;
    }
    
    /*▲ 统计 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the targeted parallelism level of this pool.
     *
     * @return the targeted parallelism level of this pool
     */
    // 返回【工作池】的并行度
    public int getParallelism() {
        int par = mode & SMASK;
        return (par > 0) ? par : 1;
    }
    
    /**
     * Returns {@code true} if all worker threads are currently idle.
     * An idle worker is one that cannot obtain a task to execute
     * because none are available to steal from other threads, and
     * there are no pending submissions to the pool. This method is
     * conservative; it might not return {@code true} immediately upon
     * idleness of all threads, but will eventually become true if
     * threads remain inactive.
     *
     * @return {@code true} if all threads are currently idle
     */
    // 判断是否所有【工作线程】当前都处于空闲状态
    public boolean isQuiescent() {
        for (;;) {
            long c = ctl;
            int pc = mode & SMASK;
            int tc = pc + (short)(c >>> TC_SHIFT);  // 线程总数
            int rc = pc + (int)(c >> RC_SHIFT);     // 活跃线程数
            
            if ((mode & (STOP | TERMINATED)) != 0) {
                return true;
            } else if (rc > 0) {
                return false;
            } else {
                if (workQueues != null) {
                    // 遍历奇数插槽上的【工作队列】
                    for (int i = 1; i < workQueues.length; i += 2) {
                        if (workQueues[i] != null) {
                            // 如果该【工作队列】正在执行窃取的任务
                            if (workQueues[i].source > 0) {
                                return false;
                            }
                            
                            --tc;
                        }
                    }
                }
                
                if (tc == 0 && ctl == c) {
                    return true;
                }
            }
        }
    }
    
    /**
     * Returns {@code true} if there are any tasks submitted to this pool that have not yet begun executing.
     *
     * @return {@code true} if there are any queued submissions
     */
    // 如果所有【共享队列】中没有正在排队的任务，则返回true
    public boolean hasQueuedSubmissions() {
        WorkQueue[] ws;
        WorkQueue w;
        VarHandle.acquireFence();
        if ((ws = workQueues) != null) {
            // 遍历【共享队列】
            for (int i = 0; i < ws.length; i += 2) {
                if ((w = ws[i]) != null && !w.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns the factory used for constructing new workers.
     *
     * @return the factory used for constructing new workers
     */
    // 返回【工作线程】工厂
    public ForkJoinWorkerThreadFactory getFactory() {
        return factory;
    }
    
    /**
     * Returns the handler for internal worker threads that terminate
     * due to unrecoverable errors encountered while executing tasks.
     *
     * @return the handler, or {@code null} if none
     */
    // 返回未捕获异常处理器
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return ueh;
    }
    
    /**
     * Returns {@code true} if this pool uses local first-in-first-out scheduling mode for forked tasks that are never joined.
     *
     * @return {@code true} if this pool uses async mode
     */
    // 返回【队列】的访问模式
    public boolean getAsyncMode() {
        return (mode & FIFO) != 0;
    }
    
    /**
     * Returns the next sequence number. We don't expect this to ever contend, so use simple builtin sync.
     */
    // 获取下一个【工作池】的编号
    private static final synchronized int nextPoolId() {
        return ++poolNumberSequence;
    }
    
    /*▲ 信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ ManagedBlocker ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Runs the given possibly blocking task.  When {@linkplain
     * ForkJoinTask#inForkJoinPool() running in a ForkJoinPool}, this
     * method possibly arranges for a spare thread to be activated if
     * necessary to ensure sufficient parallelism while the current
     * thread is blocked in {@link ManagedBlocker#block blocker.block()}.
     *
     * <p>This method repeatedly calls {@code blocker.isReleasable()} and
     * {@code blocker.block()} until either method returns {@code true}.
     * Every call to {@code blocker.block()} is preceded by a call to
     * {@code blocker.isReleasable()} that returned {@code false}.
     *
     * <p>If not running in a ForkJoinPool, this method is
     * behaviorally equivalent to
     * <pre> {@code
     * while (!blocker.isReleasable())
     *   if (blocker.block())
     *     break;}</pre>
     *
     * If running in a ForkJoinPool, the pool may first be expanded to
     * ensure sufficient parallelism available during the call to
     * {@code blocker.block()}.
     *
     * @param blocker the blocker task
     * @throws InterruptedException if {@code blocker.block()} did so
     */
    // 管理阻塞块（决定blocker所在线程是否需要阻塞）
    public static void managedBlock(ManagedBlocker blocker) throws InterruptedException {
        if(blocker == null) {
            throw new NullPointerException();
        }
    
        Thread thread = Thread.currentThread();
        ForkJoinWorkerThread wt;
        ForkJoinPool p;
        WorkQueue w;
    
        if((thread instanceof ForkJoinWorkerThread)  // 当前线程是【工作线程】
            && (p = (wt = (ForkJoinWorkerThread) thread).pool) != null   // 当前【工作线程】的线程池存在
            && (w = wt.workQueue) != null) {                             // 上述线程池中存在工作队列
        
            int block;
        
            // 如果当前线程仍然需要阻塞
            while(!blocker.isReleasable()) {
                block = p.tryCompensate(w);
            
                if(block != 0) {
                    try {
                        while(!blocker.isReleasable() && !blocker.block())
                            ;
                    } finally {
                        CTL.getAndAdd(p, (block>0) ? RC_UNIT : 0L);
                    }
                    break;
                }
            }
        } else {
            while(!blocker.isReleasable() && !blocker.block())
                ;
        }
    }
    
    /**
     * If the given executor is a ForkJoinPool, poll and execute
     * AsynchronousCompletionTasks from worker's queue until none are
     * available or blocker is released.
     */
    // 尝试使用blocker当前所在线程加速指定的【工作池】中的任务完成
    static void helpAsyncBlocker(Executor e, ManagedBlocker blocker) {
        if(!(e instanceof ForkJoinPool)){
            return;
        }
        
        // 要求参数中指定的【任务执行器】是【工作池】类型
        ForkJoinPool p = (ForkJoinPool) e;
        
        Thread thread = Thread.currentThread();
        
        ForkJoinWorkerThread wt;
        WorkQueue w;
        WorkQueue[] ws;
        int r, n;
        
        if(thread instanceof ForkJoinWorkerThread                   // 当前线程是【工作线程】
            && (wt = (ForkJoinWorkerThread) thread).pool == p) {    // 当前【工作线程】的工作池与指定的工作池匹配
            // 获取当前【工作线程】管辖的【工作队列】
            w = wt.workQueue;
            
            // 【探测值】不为0，说明当前线程是【提交线程】
        } else if((r = ThreadLocalRandom.getProbe()) != 0           // 当前线程是【提交线程】
            && (ws = p.workQueues) != null                          // 指定的工作池中包含【工作组】
            && (n = ws.length)>0) {                                 // 上述【工作组】中包含【队列】
            // 获取当前【提交线程】管辖的【共享队列】
            w = ws[(n - 1) & r & SQMASK];
        } else {
            // 既不是【工作线程】也不是【提交线程】
            w = null;
        }
        
        // 拿到了【工作队列】或【共享队列】
        if(w != null) {
            // 利用blocker当前所在线程加速【队列】中的任务完成
            w.helpAsyncBlocker(blocker);
        }
        
    }
    
    /*▲ ManagedBlocker ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 权限管理 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * If there is a security manager, makes sure caller has permission to modify threads.
     */
    // 安全管理器引发的权限检查
    private static void checkPermission() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(modifyThreadPermission);
        }
    }
    
    static AccessControlContext contextWithPermissions(Permission... perms) {
        Permissions permissions = new Permissions();
        for(Permission perm : perms) {
            permissions.add(perm);
        }
        
        return new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, permissions)});
    }
    
    /*▲ 权限管理 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Removes all available unexecuted submitted and forked tasks
     * from scheduling queues and adds them to the given collection,
     * without altering their execution status. These may include
     * artificially generated or wrapped tasks. This method is
     * designed to be invoked only when the pool is known to be
     * quiescent. Invocations at other times may not remove all
     * tasks. A failure encountered while attempting to add elements
     * to collection {@code c} may result in elements being in
     * neither, either or both collections when the associated
     * exception is thrown.  The behavior of this operation is
     * undefined if the specified collection is modified while the
     * operation is in progress.
     *
     * @param c the collection to transfer elements into
     * @return the number of elements transferred
     */
    // 将【工作池】上现存的所有任务存入指定的容器中
    protected int drainTasksTo(Collection<? super ForkJoinTask<?>> c) {
        ForkJoinTask<?> t;
        VarHandle.acquireFence();
        int count = 0;
        if (workQueues != null) {
            // 遍历【工作组】
            for(WorkQueue w : workQueues) {
                if(w != null) {
                    // 取出【工作组】中的任务
                    while((t = w.poll()) != null) {
                        // 加到容器中
                        c.add(t);
                        ++count;
                    }
                }
            }
        }
        return count;
    }
    
    
    /**
     * Returns a string identifying this pool, as well as its state, including indications of run state, parallelism level, and worker and task counts.
     *
     * @return a string identifying this pool, as well as its state
     */
    // 字符串化
    public String toString() {
        // Use a single pass through workQueues to collect counts
        int md = mode; // read volatile fields first
        long c = ctl;
        long st = stealCount;
        long qt = 0L, qs = 0L; int rc = 0;
        WorkQueue[] ws; WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 0; i < ws.length; ++i) {
                if ((w = ws[i]) != null) {
                    int size = w.queueSize();
                    if ((i & 1) == 0) {
                        qs += size;
                    } else {
                        qt += size;
                        st += (long)w.nsteals & 0xffffffffL;
                        if (w.isApparentlyUnblocked()) {
                            ++rc;
                        }
                    }
                }
            }
        }
        
        int pc = (md & SMASK);
        int tc = pc + (short)(c >>> TC_SHIFT);
        int ac = pc + (int)(c >> RC_SHIFT);
        // ignore transient negative
        if (ac < 0) {
            ac = 0;
        }
        String level = ((md & TERMINATED) != 0
            ? "Terminated"
            : (md & STOP) != 0
            ? "Terminating"
            : (md & SHUTDOWN) != 0
            ? "Shutting down"
            : "Running");
        
        return super.toString() +
            "[" + level +
            ", parallelism = " + pc +
            ", size = " + tc +
            ", active = " + ac +
            ", running = " + rc +
            ", steals = " + st +
            ", tasks = " + qt +
            ", submissions = " + qs +
            "]";
    }
    
    
    
    
    
    
    /**
     * Queues supporting work-stealing as well as external task
     * submission. See above for descriptions and algorithms.
     */
    /*
     * 【队列】
     *
     * 【工作队列】：由【工作线程】创建的WorkQueue，位于【工作组】的奇数位置
     * 【共享队列】：由【提交线程】创建的WorkQueue，位于【工作组】的偶数位置
     *
     * 【工作队列】中的任务：
     * local  task：【本地任务】，取自[当前线程]的【工作组】中
     * stolen task：【窃取任务】，取自[其他线程]的【工作组】中
     */
    @jdk.internal.vm.annotation.Contended
    static final class WorkQueue {
        // 该WorkQueue所处的工作池
        final ForkJoinPool pool;   // the containing pool (may be null)
        
        // WorkQueue所在的【工作线程】，如果WorkQueue在【提交线程】中创建，则owner为null
        final ForkJoinWorkerThread owner; // owning thread or null if shared
        
        // 任务组，实际存放task的地方。在不至混淆的情形下，会混用【队列】与<任务组>的概念
        ForkJoinTask<?>[] array;   // the queued tasks; power of 2 size
        
        int nsteals;               // 记录【工作队列】从其它【队列】中窃取了多少任务
        
        /*
         * 【工作队列】的ID(>0)
         * 前16位是模式标记，后16位是【工作队列】在【工作组】上的索引
         */
        int id;                    // pool index, mode, tag
        
        /*
         * 对于【共享队列】来说：
         * phase==0，处于非锁定状态
         * phase==1，处于锁定状态
         *
         * 对于【工作队列】来说：phase用作版本号，具有特殊含义：
         * 1.phase==id(>0)，初始值
         * 2.phase==QUIET(>0)，则该【工作队列】已报废
         * 3.pase==UNSIGNALLED(<0)，该【工作队列】即将/已经进入park状态
         *
         * 每当【工作队列】所在的【工作线程】转入park时，phase的前16位会改变
         */
        volatile int phase;        // versioned, negative: queued, 1: locked
        
        /*
         * 对于【共享队列】来说，总是带有QUIET标记
         *
         * 对于【工作队列】来说：
         * 1.source==id(>0)，记录了被窃【队列】的ID
         * 2.source==QUIET|UNSIGNALLED(<0)，该【工作队列】即将/已经进入park状态
         * 3.source==QUIET(>0)，该【工作队列】处于空闲状态
         * 4.source==0，该【工作队列】处于工作状态
         */
        volatile int source;       // source queue id, or sentinel
        
        int base;                  // index of next slot for poll   // 先来的
        int top;                   // index of next slot for push   // 后到的
        
        int stackPred;             // pool stack (ctl) predecessor link
        
        
        // VarHandle mechanics.
        static final VarHandle PHASE;
        static final VarHandle BASE;
        static final VarHandle TOP;
        
        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                PHASE = l.findVarHandle(WorkQueue.class, "phase", int.class);
                BASE = l.findVarHandle(WorkQueue.class, "base", int.class);
                TOP = l.findVarHandle(WorkQueue.class, "top", int.class);
            } catch(ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        
        WorkQueue(ForkJoinPool pool, ForkJoinWorkerThread owner) {
            this.pool = pool;   // 关联工作池
            this.owner = owner; // 分配【工作线程】（如果在非【工作线程】中，这里为null）
            // Place indices in the center of array (that is not yet allocated)
            base = top = INITIAL_QUEUE_CAPACITY >>> 1;  // 设置首尾游标到中间位置
        }
        
        
        
        /*▼ 统计 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Returns true if owned and not known to be blocked.
         */
        // 判断【工作线程】是否在阻塞状态（包括park和wait以及争用态）
        final boolean isApparentlyUnblocked() {
            if(owner != null){
                // 获取【工作线程】的状态
                Thread.State s = owner.getState();
                
                // 判断【工作线程】是否在阻塞状态
                return s!=Thread.State.BLOCKED && s!=Thread.State.WAITING && s!=Thread.State.TIMED_WAITING;
            }
            
            // 如果调用者是【提交线程】，或者【工作线程】处于阻塞状态，返回false
            return false;
        }
        
        /**
         * Returns the approximate number of tasks in the queue.
         */
        // 返回【队列】中的任务数（近似）
        final int queueSize() {
            int n = (int) BASE.getAcquire(this) - top;
            return (n >= 0) ? 0 : -n;   // ignore transient negative
        }
        
        /*▲ 统计 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        /*▼ 分发 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Pushes a task. Call only by owner in unshared queues.
         *
         * @param task the task. Caller must ensure non-null.
         *
         * @throws RejectedExecutionException if array cannot be resized
         */
        // 分发任务。【工作线程】将指定的task放入【工作队列】top处排队，并创建/唤醒【工作线程】
        final void push(ForkJoinTask<?> task) {
            // 确保当前【工作队列】中存在有效的任务组
            if(array != null && array.length>0) {
                // 将task任务放入top处
                QA.setRelease(array, top & (array.length-1), task);
                
                // top游标+1
                top = top + 1;
                
                int count = top - (int) BASE.getAcquire(this);
                
                // 如果【工作队列】内只有1或2个任务，则唤醒【工作线程】
                if((count == 1 || count == 2) && pool != null) {
                    VarHandle.fullFence();
                    // 创建/唤醒【工作线程】
                    pool.signalWork();
                } else {
                    // 如果任务数组满了，需要扩容
                    if(count == array.length) {
                        // 对【工作队列】扩容
                        growArray(false);
                    }
                }
            }
        }
        
        /**
         * Version of push for shared queues. Call only with phase lock held.
         *
         * @return true if should signal work
         */
        /*
         * 分发任务
         *
         * 在【共享队列】被【提交线程】锁定的情形下，
         * 【提交线程】将task放入【共享队列】top处排队，
         * 最后解除对【共享队列】的锁定，
         *
         * 返回值含义：是否唤醒【工作线程】
         */
        final boolean lockedPush(ForkJoinTask<?> task) {
            boolean signal = false;
            
            if(array != null && array.length>0) {
                // 存入任务
                array[(array.length - 1) & top] = task;
                
                // top游标+1
                top = top + 1;
                
                /*
                 * 如果【共享队列】满了，则对【共享队列】扩容，
                 * 随后解除对【共享队列】的锁定
                 */
                if((top-base) == array.length) {
                    growArray(true);
                } else {
                    // 解除对【共享队列】的锁定
                    phase = 0; // full volatile unlock
                    
                    // 如果【共享队列】内只有1或2个任务，则唤醒
                    if(((top-base)==1)||((top-base)==2)) {
                        signal = true;
                    }
                }
            }
            
            return signal;
        }
        
        /*▲ 分发 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        /*▼ 移除 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Pops the given task only if it is at the current top.
         */
        // 移除任务。【工作线程】尝试将指定的task从【工作队列】top处移除，返回值代表是否成功移除
        final boolean tryUnpush(ForkJoinTask<?> task) {
            boolean popped = false;
            int s;
            
            // 如果任务组存在
            if(array != null && array.length>0) {
                s = top;
                
                // 如果任务组不为空
                if(s != base){
                    // 递减top，取出任务
                    popped = QA.compareAndSet(array, --s & (array.length-1), task, null);
                    
                    // 如果该任务确实在top处，则可以拿到任务
                    if(popped){
                        // 更新top
                        TOP.setOpaque(this, s);
                    }
                }
            }
            
            // 返回值表示是否成功取出了任务
            return popped;
        }
        
        /**
         * Shared version of tryUnpush.
         */
        // 移除任务。【提交线程】尝试将指定的task从【共享队列】top处移除，返回值代表是否成功移除
        final boolean tryLockedUnpush(ForkJoinTask<?> task) {
            boolean popped = false;
            
            int s = top - 1, k, cap;
            
            ForkJoinTask<?>[] a= array;
            
            /*
             * 如果task位于当前【共享队列】的top处，则：
             * 锁定【共享队列】；
             * 置空task在【共享队列】中的引用；
             * 修改top游标；
             * 解锁【共享队列】；
             */
            if(a != null && (cap = a.length)>0 && a[k = (cap - 1) & s] == task && tryLockPhase()) {
                if(top == s + 1 && array == a && (popped = QA.compareAndSet(a, k, task, null))) {
                    top = s;
                }
                
                // 解除了对【共享队列】的锁定
                releasePhaseLock();
            }
            
            return popped;
        }
        
        /*▲ 移除 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        /*▼ 获取 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        /**
         * Returns next task, if one exists, in order specified by mode.
         */
        // 如果【队列】带有FIFO标记，则从base处获取task，否则从top处获取任务
        final ForkJoinTask<?> peek() {
            if(array != null && array.length>0) {
                int n = (id & FIFO) != 0 ? base : top - 1;
                int index = n & (array.length - 1);
                return array[index];
            }
            
            return null;
        }
        
        /**
         * Takes next task, if one exists, in FIFO order.
         */
        // 窃取【队列】base处的task，如果取不到，就释放CPU时间片，各线程重新抢占执行权
        final ForkJoinTask<?> poll() {
            while(array != null && array.length>0) {
                int base = this.base;
                
                if(top - base>0){
                    int index = base & (array.length-1);
                    
                    // 获取base处的task
                    ForkJoinTask<?> task = (ForkJoinTask<?>) QA.getAcquire(array, index);
                    
                    if(this.base == base++) {
                        // 如果此处的task已被别的【工作线程】窃取了
                        if(task == null) {
                            // 则当前【工作线程】让出CPU时间片，大家重新抢占执行权
                            Thread.yield(); // await index advance
                        } else {
                            // 原子地清理task引用
                            if(QA.compareAndSet(array, index, task, null)) {
                                // 更新base
                                BASE.setOpaque(this, base);
                                return task;
                            }
                        }
                    }
                }
            }
            
            return null;
        }
        
        /**
         * Takes next task, if one exists, in order specified by mode.
         */
        /*
         * 让【工作线程】尝试从自身的【工作队列】中取出[本地任务]
         * 因为【工作线程】在执行上面的任务时，很大概率会向自身的【工作队列】中提交新任务
         * （取出任务的位置取决于访问模式是LIFO还是FIFO）
         */
        final ForkJoinTask<?> nextLocalTask() {
            ForkJoinTask<?> task = null;
            int b, s;
            
            if(array != null && array.length>0 ) {
                b = base;
                s = top;
                
                // 【工作队列】中有任务
                if(top-base>0){
                    // 如果【工作队列】的访问模式为FIFO，且存在不止一个任务排队
                    if((id & FIFO) != 0 && (top-base)>1){
                        // 取出当前任务队列的任务组中base处的task
                        task = (ForkJoinTask<?>) QA.getAndSet(array, (array.length - 1) & b++, null);
                        
                        // 存在多个【工作线程】竞争时，这里可能取不到任务
                        if(task != null) {
                            BASE.setOpaque(this, b);
                        } else { // on contention in FIFO mode, use regular poll
                            // 再尝试一次，取出任务组base处的task，如果还取不到，就释放CPU时间片，各线程重新抢占执行权
                            task = poll();
                        }
                        
                        // 如果【工作队列】的访问模式是LIFO，或者只有一个任务在排队
                    } else {
                        // 取出当前任务队列的任务组中top处的task
                        task = (ForkJoinTask<?>) QA.getAndSet(array, (array.length - 1) & --s, null);
                        
                        if(task != null) {
                            TOP.setOpaque(this, s);
                        }
                    }
                }
                
            }
            
            return task;
        }
        
        /*▲ 获取 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        
        
        /**
         * Tries to lock shared queue by CASing phase field.
         */
        // 尝试锁定【共享队列】
        final boolean tryLockPhase() {
            return PHASE.compareAndSet(this, 0, 1);
        }
        
        // 解除对【共享队列】的锁定
        final void releasePhaseLock() {
            PHASE.setRelease(this, 0);
        }
        
        /**
         * Returns an exportable index (used by ForkJoinWorkerThread).
         */
        // 返回【工作队列】的唯一编号
        final int getPoolIndex() {
            return (id & 0xffff) >>> 1; // ignore odd/even tag bit
        }
        
        /**
         * Provides a more accurate estimate of whether this queue has
         * any tasks than does queueSize, by checking whether a
         * near-empty queue has at least one unclaimed task.
         */
        // 判断当前【队列】的【任务组】是否为空
        final boolean isEmpty() {
            ForkJoinTask<?>[] a;
            int n, cap, b;
            VarHandle.acquireFence(); // needed by external callers
            return ((n = (b = base) - top) >= 0 // possibly one task
                || (n == -1 && ((a = array) == null || (cap = a.length) == 0 || a[(cap - 1) & b] == null)));
        }
        
        /**
         * Doubles the capacity of array.
         * Call either by owner or with lock held -- it is OK for base, but not top, to move while resizings are in progress.
         */
        // 对任务组（【队列】）扩容，如果【队列】被锁定(locked==true)，扩容后解除锁定
        final void growArray(boolean locked) {
            ForkJoinTask<?>[] newArray = null;
            
            try {
                if(array != null && array.length>0) {
                    // 容量翻倍
                    int newSize = array.length << 1;
                    
                    // 如果新数组容量不超标
                    if(newSize<=MAXIMUM_QUEUE_CAPACITY && newSize>0) {
                        try {
                            // 创建新数组
                            newArray = new ForkJoinTask<?>[newSize];
                        } catch(OutOfMemoryError ignored) {
                        }
                        
                        // 将位于旧任务组中的任务挪到新任务组中
                        for(int s =top-1, k = array.length-1; k >= 0; --k) {
                            ForkJoinTask<?> fjTask = (ForkJoinTask<?>) QA.getAndSet(array, s & (array.length-1), null);
                            
                            if(fjTask != null) {
                                newArray[s-- & (newSize - 1)] = fjTask;
                            } else {
                                break;
                            }
                        }
                        
                        array = newArray;
                        
                        VarHandle.releaseFence();
                    }
                    
                }
            } finally {
                if(locked) {
                    phase = 0;
                }
            }
            
            if(newArray == null) {
                throw new RejectedExecutionException("Queue capacity exceeded");
            }
        }
        
        // Specialized execution methods
        
        /**
         * Removes and cancels all known tasks, ignoring any exceptions.
         */
        // 取消当前【队列】中的所有任务，即标记所有任务为DONE|ABNORMAL状态
        final void cancelAll() {
            ForkJoinTask<?> t;
            
            // 不断取出任务组base处的task，并更新base，直到任务组中不再有task
            while((t = poll()) != null) {
                // 取消任务，即将当前任务标记为DONE|ABNORMAL状态
                ForkJoinTask.cancelIgnoringExceptions(t);
            }
        }
        
        /**
         * Runs the given (stolen) task if nonnull,
         * as well as remaining local tasks and others available from the given queue,
         * up to bound n (to avoid infinite unfairness).
         */
        /*
         * 【工作线程】执行窃取操作
         *
         * 1.先让当前【工作线程】执行从【队列】w中窃取的task
         * 2.随后循环从当前【工作线程】自身的【工作队列】中取出[本地任务]并执行
         * 3.如果找不到[本地任务]，就再次去w中窃取任务
         * 4.上述过程是重复进行的
         *   如果[本地任务]和[外部任务]均找不到了，则结束操作
         */
        final void topLevelExec(ForkJoinTask<?> task, WorkQueue w, int n) {
            if(task != null && w != null) { // hoist checks
                int nstolen = 1;
                
                // 执行窃取操作，循环往复
                for(; ; ) {
                    /*
                     * 首次到这时，执行的是【外部任务】
                     *
                     * 再次到这时，执行的可能是【外部任务】，也可能是【本地任务】
                     */
                    task.doExec();
                    
                    // 循环次数（随机），避免没有任务时陷入死循环
                    if(n--<0) {
                        break;
                    }
                    
                    /*
                     * 让【工作线程】尝试从自身的【工作队列】中取出[本地任务]，
                     * 因为【工作线程】在执行上面的任务时，会向自身的【工作队列】中提交新任务
                     */
                    task = nextLocalTask();
                    
                    // 如果没有取到[本地任务]（可能是本地没有多余任务了，也可能是被其他【工作线程】窃取了），则需要窃取别的队列中的任务
                    if(task == null) {
                        // 再次从w的base处窃取task，如果取不到，当前【工作线程】就释放CPU时间片，各线程重新抢占执行权
                        task = w.poll();
                        
                        // 如果w中已经没有task，则跳出循环
                        if(task == null) {
                            break;
                        }
                        
                        // 如果成功窃取了任务，窃取计数增一
                        ++nstolen;
                    }
                }
                
                // 累计窃取次数
                nsteals += nstolen;
                
                source = 0;
                
                // 【工作线程】完成窃取操作后，执行的回调
                if(owner != null) {
                    owner.afterTopLevelExec();
                }
            }
        }
        
        /**
         * If present, removes task from queue and executes it.
         */
        // 如果task存在于【队列】当中，尝试将其从【队列】中移除，并执行它
        final void tryRemoveAndExec(ForkJoinTask<?> task) {
            if(array != null && array.length>0 && top - base>0) { // traverse from top
                int m = array.length - 1;
                int ns = top - 1;   // 记下当前栈顶元素的位置
                int i = ns;
                
                while(true) {
                    // 计算索引
                    int index = i & m;
                    
                    // 查看当前【队列】top处的任务
                    ForkJoinTask<?> t = (ForkJoinTask<?>) QA.get(array, index);
                    if(t == null) {
                        break;
                    } else {
                        // 如果找到了参数中指定的任务
                        if(t == task) {
                            // 将该任务从【队列】中移除
                            if(QA.compareAndSet(array, index, t, null)) {
                                // 更新top
                                top = ns;   // safely shift down
                                
                                // 检查task后面是否还有其它任务
                                for(int j = i; j != ns; ++j) {
                                    /* 将task后面的任务依次前移 */
                                    
                                    int pindex = (j + 1) & m;
                                    ForkJoinTask<?> f = (ForkJoinTask<?>) QA.get(array, pindex);
                                    QA.setVolatile(array, pindex, null);
                                    int jindex = j & m;
                                    QA.setRelease(array, jindex, f);
                                }
                                
                                VarHandle.releaseFence();
                                
                                // 执行任务
                                t.doExec();
                            }
                            
                            break;
                        }
                    }
                    // 游标递减
                    --i;
                } // while(true)
            }
            
        }
        
        /**
         * Tries to pop and run tasks within the target's computation until done, not found, or limit exceeded.
         *
         * @param task   root of CountedCompleter computation
         * @param limit  max runs, or zero for no limit
         * @param shared true if must lock to extract task
         *
         * @return task status on exit
         */
        /*
         * 在【队列】的top处查找task及task的父任务，如果找到，则执行它，以加速任务的完成
         *
         * task是待完成的任务
         * limit是一个正数，代表最大重试次数，limit==0时代表可以一直查找任务，直到不再有匹配的任务
         * shared指示task位于【共享队列】(true)还是【工作队列】(false)
         *
         * 最后返回任务的状态
         */
        final int helpCC(CountedCompleter<?> task, int limit, boolean shared) {
            int status = 0;
            
            if(task != null && (status = task.status) >= 0) {
                int s, k, cap;
                
                ForkJoinTask<?>[] a;
                
                while((a = array) != null && (cap = a.length)>0 && (s = top) - base>0) {
                    /*
                     * v==null的话，说明当前【队列】的top处不存在给定的task
                     * v!=null的话，v的值就是给定的task，此时说明成功在【队列】的top处找到了指定的任务
                     */
                    CountedCompleter<?> v = null;
                    
                    // 获取top处的task
                    ForkJoinTask<?> o = a[k = (cap - 1) & (s - 1)];
                    
                    if(o instanceof CountedCompleter) {
                        CountedCompleter<?> t, f;
                        
                        f = t = (CountedCompleter<?>) o;
                        
                        for(; ; ) {
                            // 如果【队列】的top处的task与给定的task不一致
                            if(f != task) {
                                // 切换到父任务
                                if((f = f.completer) == null) {
                                    break;
                                }
                            } else {
                                // 如果是来自【共享队列】的任务
                                if(shared) {
                                    // 尝试锁定【共享队列】
                                    if(tryLockPhase()) {
                                        // 如果top处的task就是给定的task，则将其从top处移除
                                        if(top == s && array == a && QA.compareAndSet(a, k, t, null)) {
                                            top = s - 1;
                                            v = t;
                                        }
                                        
                                        // 解除对【共享队列】的锁定
                                        releasePhaseLock();
                                    }
                                    
                                    break;
                                }
                                
                                // 如果top处的task就是给定的task，则将其从top处移除
                                if(QA.compareAndSet(a, k, t, null)) {
                                    top = s - 1;
                                    v = t;
                                }
                                
                                break;
                            }
                        } // for(; ;)
                    } // if
                    
                    // 如果在【队列】的top处找到了匹配的task，则执行它
                    if(v != null) {
                        // 执行任务
                        v.doExec();
                    }
                    
                    // 如果任务[已完成]，或者在top处找不到匹配的任务，或者已经没有重试次数，则退出循环
                    if((status = task.status)<0 || v == null || (limit != 0 && --limit == 0)) {
                        break;
                    }
                } // while
            }
            
            return status;
        }
        
        /**
         * Tries to poll and run AsynchronousCompletionTasks until
         * none found or blocker is released
         *
         * @param blocker the blocker
         */
        /*
         * 理论上讲，如果blocker当前所在线程追踪的任务还没完成，
         * 则应当阻塞该线程，直到任务完成。
         * 这里为了最大化地利用资源，采用了同步非阻塞模式，
         * 通过轮询，不断判断任务是否已经完成（是否可以解除阻塞），
         * 如果任务还没完成，则blocker当前所在线程被用来加速【工作池】中的任务完成，
         * 如果任务已经完成，则退出循环，结束加速。
         */
        final void helpAsyncBlocker(ManagedBlocker blocker) {
            if(blocker != null) {
                int b, k, cap;
                ForkJoinTask<?>[] a;
                ForkJoinTask<?> t;
                
                while((a = array) != null && (cap = a.length)>0 && top - (b = base)>0) {
                    // 取出任务组/队列base处的任务
                    t = (ForkJoinTask<?>) QA.getAcquire(a, k = (cap - 1) & b);
                    
                    // 如果blocker当前所在线程需要解除阻塞（即blocker所在线程追踪的任务已经有结果了），直接退出
                    if(blocker.isReleasable()) {
                        break;
                    }
                    
                    // 成功取到了待加速任务
                    if(base == b++ && t != null) {
                        // 不加速AsynchronousCompletionTask类型的任务
                        if(!(t instanceof CompletableFuture.AsynchronousCompletionTask)) {
                            break;
                        }
                        
                        if(QA.compareAndSet(a, k, t, null)) {
                            BASE.setOpaque(this, b);
                            
                            // 执行任务
                            t.doExec();
                        }
                    }
                }
            }
        }
        
    }
    
    
    /**
     * Factory for creating new {@link ForkJoinWorkerThread}s.
     * A {@code ForkJoinWorkerThreadFactory} must be defined and used
     * for {@code ForkJoinWorkerThread} subclasses that extend base
     * functionality or initialize threads with different contexts.
     */
    // 【工作线程】工厂
    public static interface ForkJoinWorkerThreadFactory {
        /**
         * Returns a new worker thread operating in the given pool.
         * Returning null or throwing an exception may result in tasks
         * never being executed.  If this method throws an exception,
         * it is relayed to the caller of the method (for example
         * {@code execute}) causing attempted thread creation. If this
         * method returns null or throws an exception, it is not
         * retried until the next attempted creation (for example
         * another call to {@code execute}).
         *
         * @param pool the pool this thread works in
         * @return the new worker thread, or {@code null} if the request
         *         to create a thread is rejected
         * @throws NullPointerException if the pool is null
         */
        // 创建【工作线程】，并为其注册一个【工作队列】
        public ForkJoinWorkerThread newThread(ForkJoinPool pool);
    }
    
    /**
     * Default ForkJoinWorkerThreadFactory implementation; creates a
     * new ForkJoinWorkerThread using the system class loader as the
     * thread context class loader.
     */
    // 默认的【工作线程】工厂
    private static final class DefaultForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        private static final AccessControlContext ACC
            = contextWithPermissions(new RuntimePermission("getClassLoader"), new RuntimePermission("setContextClassLoader"));
        
        // 创建【工作线程】和【工作线程】，并与指定的【工作池】完成三方绑定
        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                public ForkJoinWorkerThread run() {
                    return new ForkJoinWorkerThread(pool, ClassLoader.getSystemClassLoader());
                }
            }, ACC);
        }
    }
    
    /**
     * Factory for innocuous worker threads.
     */
    // 无害的【工作线程】工厂，在存在SecurityManager时使用
    private static final class InnocuousForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        
        /**
         * An ACC to restrict permissions for the factory itself.
         * The constructed workers have no permissions set.
         */
        private static final AccessControlContext ACC = contextWithPermissions(modifyThreadPermission,
            new RuntimePermission("enableContextClassLoaderOverride"),
            new RuntimePermission("modifyThreadGroup"),
            new RuntimePermission("getClassLoader"),
            new RuntimePermission("setContextClassLoader")
        );
        
        // 创建【工作队列】和【工作线程】，并与指定的【工作池】完成三方绑定
        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                public ForkJoinWorkerThread run() {
                    return new ForkJoinWorkerThread.InnocuousForkJoinWorkerThread(pool);
                }
            }, ACC);
        }
    }
    
    
    /**
     * Interface for extending managed parallelism for tasks running in {@link ForkJoinPool}s.
     *
     * <p>A {@code ManagedBlocker} provides two methods.  Method
     * {@link #isReleasable} must return {@code true} if blocking is
     * not necessary. Method {@link #block} blocks the current thread
     * if necessary (perhaps internally invoking {@code isReleasable}
     * before actually blocking). These actions are performed by any
     * thread invoking {@link ForkJoinPool#managedBlock(ManagedBlocker)}.
     * The unusual methods in this API accommodate synchronizers that
     * may, but don't usually, block for long periods. Similarly, they
     * allow more efficient internal handling of cases in which
     * additional workers may be, but usually are not, needed to
     * ensure sufficient parallelism.  Toward this end,
     * implementations of method {@code isReleasable} must be amenable
     * to repeated invocation.
     *
     * <p>For example, here is a ManagedBlocker based on a
     * ReentrantLock:
     * <pre> {@code
     * class ManagedLocker implements ManagedBlocker {
     *   final ReentrantLock lock;
     *   boolean hasLock = false;
     *   ManagedLocker(ReentrantLock lock) { this.lock = lock; }
     *   public boolean block() {
     *     if (!hasLock)
     *       lock.lock();
     *     return true;
     *   }
     *   public boolean isReleasable() {
     *     return hasLock || (hasLock = lock.tryLock());
     *   }
     * }}</pre>
     *
     * <p>Here is a class that possibly blocks waiting for an
     * item on a given queue:
     * <pre> {@code
     * class QueueTaker<E> implements ManagedBlocker {
     *   final BlockingQueue<E> queue;
     *   volatile E item = null;
     *   QueueTaker(BlockingQueue<E> q) { this.queue = q; }
     *   public boolean block() throws InterruptedException {
     *     if (item == null)
     *       item = queue.take();
     *     return true;
     *   }
     *   public boolean isReleasable() {
     *     return item != null || (item = queue.poll()) != null;
     *   }
     *   public E getItem() { // call after pool.managedBlock completes
     *     return item;
     *   }
     * }}</pre>
     */
    // 阻塞块
    public interface ManagedBlocker {
        /**
         * Possibly blocks the current thread, for example waiting for a lock or condition.
         *
         * @return {@code true} if no additional blocking is necessary
         *         (i.e., if isReleasable would return true)
         *
         * @throws InterruptedException if interrupted while waiting
         *                              (the method is not required to do so, but is allowed to)
         */
        // 阻塞当前线程，返回true代表阻塞结束
        boolean block() throws InterruptedException;
        
        /**
         * Returns {@code true} if blocking is unnecessary.
         *
         * @return {@code true} if blocking is unnecessary
         */
        // 当前线程是否需要解除阻塞
        boolean isReleasable();
    }
    
}
