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
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link Future} that may be explicitly completed (setting its
 * value and status), and may be used as a {@link CompletionStage},
 * supporting dependent functions and actions that trigger upon its
 * completion.
 *
 * <p>When two or more threads attempt to
 * {@link #complete complete},
 * {@link #completeExceptionally completeExceptionally}, or
 * {@link #cancel cancel}
 * a CompletableFuture, only one of them succeeds.
 *
 * <p>In addition to these and related methods for directly
 * manipulating status and results, CompletableFuture implements
 * interface {@link CompletionStage} with the following policies: <ul>
 *
 * <li>Actions supplied for dependent completions of
 * <em>non-async</em> methods may be performed by the thread that
 * completes the current CompletableFuture, or by any other caller of
 * a completion method.
 *
 * <li>All <em>async</em> methods without an explicit Executor
 * argument are performed using the {@link ForkJoinPool#commonPool()}
 * (unless it does not support a parallelism level of at least two, in
 * which case, a new Thread is created to run each task).  This may be
 * overridden for non-static methods in subclasses by defining method
 * {@link #defaultExecutor()}. To simplify monitoring, debugging,
 * and tracking, all generated asynchronous tasks are instances of the
 * marker interface {@link AsynchronousCompletionTask}.  Operations
 * with time-delays can use adapter methods defined in this class, for
 * example: {@code supplyAsync(supplier, delayedExecutor(timeout,
 * timeUnit))}.  To support methods with delays and timeouts, this
 * class maintains at most one daemon thread for triggering and
 * cancelling actions, not for running them.
 *
 * <li>All CompletionStage methods are implemented independently of
 * other public methods, so the behavior of one method is not impacted
 * by overrides of others in subclasses.
 *
 * <li>All CompletionStage methods return CompletableFutures.  To
 * restrict usages to only those methods defined in interface
 * CompletionStage, use method {@link #minimalCompletionStage}. Or to
 * ensure only that clients do not themselves modify a future, use
 * method {@link #copy}.
 * </ul>
 *
 * <p>CompletableFuture also implements {@link Future} with the following
 * policies: <ul>
 *
 * <li>Since (unlike {@link FutureTask}) this class has no direct
 * control over the computation that causes it to be completed,
 * cancellation is treated as just another form of exceptional
 * completion.  Method {@link #cancel cancel} has the same effect as
 * {@code completeExceptionally(new CancellationException())}. Method
 * {@link #isCompletedExceptionally} can be used to determine if a
 * CompletableFuture completed in any exceptional fashion.
 *
 * <li>In case of exceptional completion with a CompletionException,
 * methods {@link #get()} and {@link #get(long, TimeUnit)} throw an
 * {@link ExecutionException} with the same cause as held in the
 * corresponding CompletionException.  To simplify usage in most
 * contexts, this class also defines methods {@link #join()} and
 * {@link #getNow} that instead throw the CompletionException directly
 * in these cases.
 * </ul>
 *
 * <p>Arguments used to pass a completion result (that is, for
 * parameters of type {@code T}) for methods accepting them may be
 * null, but passing a null value for any other parameter will result
 * in a {@link NullPointerException} being thrown.
 *
 * <p>Subclasses of this class should normally override the "virtual
 * constructor" method {@link #newIncompleteFuture}, which establishes
 * the concrete type returned by CompletionStage methods. For example,
 * here is a class that substitutes a different default Executor and
 * disables the {@code obtrude} methods:
 *
 * <pre> {@code
 * class MyCompletableFuture<T> extends CompletableFuture<T> {
 *   static final Executor myExecutor = ...;
 *   public MyCompletableFuture() { }
 *   public <U> CompletableFuture<U> newIncompleteFuture() {
 *     return new MyCompletableFuture<U>(); }
 *   public Executor defaultExecutor() {
 *     return myExecutor; }
 *   public void obtrudeValue(T value) {
 *     throw new UnsupportedOperationException(); }
 *   public void obtrudeException(Throwable ex) {
 *     throw new UnsupportedOperationException(); }
 * }}</pre>
 *
 * @author Doug Lea
 * @param <T> The result type returned by this future's {@code join}
 * and {@code get} methods
 * @since 1.8
 */
/*
 * 等待完成的阶段：一个大任务可以拆分为多个阶段任务，这些阶段任务可以串联或并联执行。
 *
 * 待完成阶段会持有当前阶段期望得到的执行结果result，还会在阶段任务栈stack中存储下个阶段中需要执行的阶段任务。
 *
 * ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 * AsyncSupply     (不可做下游任务)异步执行【Supplier】任务：该任务【有】返回值，【无需】上游的执行结果做入参，且该任务【有】返回值
 * AsyncRun        (不可做下游任务)异步执行【Runnable】任务：该任务【无】返回值，【无需】上游的执行结果做入参，且该任务【无】返回值
 * ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 * UniApply        (可以做下游任务)同步/异步执行【Function】任务：该任务【有】返回值，且【需要】等待【一个】上游任务执行完，并使用该上游的执行结果做该任务的入参；如果上游发生异常，则提前返回
 * UniAccept       (可以做下游任务)同步/异步执行【Consumer】任务：该任务【无】返回值，且【需要】等待【一个】上游任务执行完，并使用该上游的执行结果做该任务的入参；如果上游发生异常，则提前返回
 * UniRun          (可以做下游任务)同步/异步执行【Runnable】任务：该任务【无】返回值，且【需要】等待【一个】上游任务执行完，但该任务无需入参；如果上游发生异常，则提前返回
 * ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 * BiApply         (可以做下游任务)同步/异步执行【BiFunction】任务：该任务【有】返回值，且【需要】等待【两个】上游任务执行完，并使用该上游的[那两个]执行结果做该任务的入参；如果上游发生异常，则提前返回
 * BiAccept        (可以做下游任务)同步/异步执行【BiConsumer】任务：该任务【无】返回值，且【需要】等待【两个】上游任务执行完，并使用该上游的[那两个]执行结果做该任务的入参；如果上游发生异常，则提前返回
 * BiRun           (可以做下游任务)同步/异步执行【Runnable】任务：该任务【无】返回值，且【需要】等待【两个】上游任务执行完，但该任务无需入参；如果上游发生异常，则提前返回
 * ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 * OrApply         (可以做下游任务)同步/异步执行【Function】任务：该任务【有】返回值，且【需要】等待【两个】上游任务中的【任意一个】执行完，并使用该上游的[那个]执行结果做该任务的入参；如果上游发生异常，则提前返回
 * OrAccept        (可以做下游任务)同步/异步执行【Consumer】任务：该任务【无】返回值，且【需要】等待【两个】上游任务中的【任意一个】执行完，并使用该上游的[那个]执行结果做该任务的入参；如果上游发生异常，则提前返回
 * OrRun           (可以做下游任务)同步/异步执行【Runnable】任务：该任务【无】返回值，且【需要】等待【两个】上游任务中的【任意一个】执行完，但该任务无需入参；如果上游发生异常，则提前返回
 * ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 * UniCompose      (可以做下游任务)同步/异步执行【Function】任务，该任务【需要】等待【一个】上游任务执行完，并使用该上游的执行结果或抛出的异常作为入参
 * UniHandle       (可以做下游任务)同步/异步执行【BiFunction】任务，该任务【需要】等待【一个】上游任务执行完，并使用该上游的执行结果和抛出的异常作为入参
 * UniWhenComplete (可以做下游任务)同步/异步执行【BiConsumer】任务，该任务【需要】等待【一个】上游任务执行完，并使用该上游的执行结果和抛出的异常作为入参
 * -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 * anyOf           (不可做下游任务)依赖多个上游阶段的任务，该任务【需要】等待【多个】上游任务中的【任意一个】执行完，并存储[那个]任务的执行结果或异常信息
 * allOf           (不可做下游任务)依赖多个上游阶段的任务，该任务【需要】等待【多个】上游任务【全部】执行完，该等待过程中不会搜集任务结果，但会记录排在靠前的任务抛出的异常(跟执行时长无关，跟在参数中的次序有关)
 * -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 */
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    
    /*
     * Overview:
     *
     * A CompletableFuture may have dependent completion actions,
     * collected in a linked stack. It atomically completes by CASing
     * a result field, and then pops off and runs those actions. This
     * applies across normal vs exceptional outcomes, sync vs async
     * actions, binary triggers, and various forms of completions.
     *
     * Non-nullness of volatile field "result" indicates done.  It may
     * be set directly if known to be thread-confined, else via CAS.
     * An AltResult is used to box null as a result, as well as to
     * hold exceptions.  Using a single field makes completion simple
     * to detect and trigger.  Result encoding and decoding is
     * straightforward but tedious and adds to the sprawl of trapping
     * and associating exceptions with targets.  Minor simplifications
     * rely on (static) NIL (to box null results) being the only
     * AltResult with a null exception field, so we don't usually need
     * explicit comparisons.  Even though some of the generics casts
     * are unchecked (see SuppressWarnings annotations), they are
     * placed to be appropriate even if checked.
     *
     * Dependent actions are represented by Completion objects linked
     * as Treiber stacks headed by field "stack". There are Completion
     * classes for each kind of action, grouped into:
     * - single-input (UniCompletion),
     * - two-input (BiCompletion),
     * - projected (BiCompletions using exactly one of two inputs),
     * - shared (CoCompletion, used by the second of two sources),
     * - zero-input source actions,
     * - Signallers that unblock waiters.
     * Class Completion extends ForkJoinTask to enable async execution
     * (adding no space overhead because we exploit its "tag" methods
     * to maintain claims). It is also declared as Runnable to allow
     * usage with arbitrary executors.
     *
     * Support for each kind of CompletionStage relies on a separate
     * class, along with two CompletableFuture methods:
     *
     * * A Completion class with name X corresponding to function,
     *   prefaced with "Uni", "Bi", or "Or". Each class contains
     *   fields for source(s), actions, and dependent. They are
     *   boringly similar, differing from others only with respect to
     *   underlying functional forms. We do this so that users don't
     *   encounter layers of adapters in common usages.
     *
     * * Boolean CompletableFuture method x(...) (for example
     *   biApply) takes all of the arguments needed to check that an
     *   action is triggerable, and then either runs the action or
     *   arranges its async execution by executing its Completion
     *   argument, if present. The method returns true if known to be
     *   complete.
     *
     * * Completion method tryFire(int mode) invokes the associated x
     *   method with its held arguments, and on success cleans up.
     *   The mode argument allows tryFire to be called twice (SYNC,
     *   then ASYNC); the first to screen and trap exceptions while
     *   arranging to execute, and the second when called from a task.
     *   (A few classes are not used async so take slightly different
     *   forms.)  The claim() callback suppresses function invocation
     *   if already claimed by another thread.
     *
     * * Some classes (for example UniApply) have separate handling
     *   code for when known to be thread-confined ("now" methods) and
     *   for when shared (in tryFire), for efficiency.
     *
     * * CompletableFuture method xStage(...) is called from a public
     *   stage method of CompletableFuture f. It screens user
     *   arguments and invokes and/or creates the stage object.  If
     *   not async and already triggerable, the action is run
     *   immediately.  Otherwise a Completion c is created, and
     *   submitted to the executor if triggerable, or pushed onto f's
     *   stack if not.  Completion actions are started via c.tryFire.
     *   We recheck after pushing to a source future's stack to cover
     *   possible races if the source completes while pushing.
     *   Classes with two inputs (for example BiApply) deal with races
     *   across both while pushing actions.  The second completion is
     *   a CoCompletion pointing to the first, shared so that at most
     *   one performs the action.  The multiple-arity methods allOf
     *   does this pairwise to form trees of completions.  Method
     *   anyOf is handled differently from allOf because completion of
     *   any source should trigger a cleanStack of other sources.
     *   Each AnyOf completion can reach others via a shared array.
     *
     * Note that the generic type parameters of methods vary according
     * to whether "this" is a source, dependent, or completion.
     *
     * Method postComplete is called upon completion unless the target
     * is guaranteed not to be observable (i.e., not yet returned or
     * linked). Multiple threads can call postComplete, which
     * atomically pops each dependent action, and tries to trigger it
     * via method tryFire, in NESTED mode.  Triggering can propagate
     * recursively, so NESTED mode returns its completed dependent (if
     * one exists) for further processing by its caller (see method
     * postFire).
     *
     * Blocking methods get() and join() rely on Signaller Completions
     * that wake up waiting threads.  The mechanics are similar to
     * Treiber stack wait-nodes used in FutureTask, Phaser, and
     * SynchronousQueue. See their internal documentation for
     * algorithmic details.
     *
     * Without precautions, CompletableFutures would be prone to
     * garbage accumulation as chains of Completions build up, each
     * pointing back to its sources. So we null out fields as soon as
     * possible.  The screening checks needed anyway harmlessly ignore
     * null arguments that may have been obtained during races with
     * threads nulling out fields.  We also try to unlink non-isLive
     * (fired or cancelled) Completions from stacks that might
     * otherwise never be popped: Method cleanStack always unlinks non
     * isLive completions from the head of stack; others may
     * occasionally remain if racing with other cancellations or
     * removals.
     *
     * Completion fields need not be declared as final or volatile
     * because they are only visible to other threads upon safe
     * publication.
     */
    
    // Modes for Completion.tryFire. Signedness matters.
    static final int NESTED = -1;   // 嵌套
    static final int SYNC = 0;   // 同步
    static final int ASYNC = 1;   // 异步
    
    
    // 是否可以使用【共享工作池】(当共享工作池的并行度大于1，即支持并行时可以使用它)
    private static final boolean USE_COMMON_POOL = (ForkJoinPool.getCommonPoolParallelism()>1);
    
    /**
     * Default executor -- ForkJoinPool.commonPool() unless it cannot support parallelism.
     */
    // 默认使用【共享工作池】作为任务执行器，除非它不支持并行
    private static final Executor ASYNC_POOL = USE_COMMON_POOL ? ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();
    
    
    // 下游任务栈，存储直属的下游待完成任务
    volatile Completion stack;    // Top of Treiber stack of dependent actions
    
    
    /** The encoding of the null value. */
    // 特殊的任务执行结果：对于返回值为null或者无返回值的任务，其执行结果是NIL
    static final AltResult NIL = new AltResult(null);
    
    // 当前阶段的执行结果
    volatile Object result;       // Either the result or boxed AltResult
    
    
    // VarHandle mechanics
    private static final VarHandle RESULT;  // CompletableFuture中的result域
    private static final VarHandle STACK;   // CompletableFuture中的stack域
    private static final VarHandle NEXT;    // Completion中的next域
    
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            RESULT = l.findVarHandle(CompletableFuture.class, "result", Object.class);
            STACK = l.findVarHandle(CompletableFuture.class, "stack", Completion.class);
            
            NEXT = l.findVarHandle(Completion.class, "next", Completion.class);
        } catch(ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
        
        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new incomplete CompletableFuture.
     */
    public CompletableFuture() {
    }
    
    /**
     * Creates a new complete CompletableFuture with given encoded result.
     */
    CompletableFuture(Object result) {
        this.result = result;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ AsyncSupply ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} with
     * the value obtained by calling the given Supplier.
     *
     * @param action a function returning the value to be used to complete the returned CompletableFuture
     * @param <U>    the function's return type
     *
     * @return the new CompletableFuture
     */
    // 异步执行Supplier任务，返回该任务所属阶段(可从中获取执行结果)
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> action) {
        return asyncSupplyStage(ASYNC_POOL, action); // 【任务执行器】是【共享工作池】
    }
    
    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the given executor with the value obtained
     * by calling the given Supplier.
     *
     * @param action   a function returning the value to be used to complete the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @param <U>      the function's return type
     *
     * @return the new CompletableFuture
     */
    // 异步执行Supplier任务，返回该任务所属阶段(可从中获取执行结果)
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> action, Executor executor) {
        return asyncSupplyStage(screenExecutor(executor), action); // 【任务执行器】需要自行指定
    }
    
    /*▲ AsyncSupply ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ AsyncRun ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} after
     * it runs the given action.
     *
     * @param action the action to run before completing the returned CompletableFuture
     *
     * @return the new CompletableFuture
     */
    // 异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public static CompletableFuture<Void> runAsync(Runnable action) {
        return asyncRunStage(ASYNC_POOL, action); // 【任务执行器】是【共享工作池】
    }
    
    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the given executor after it runs the given
     * action.
     *
     * @param action   the action to run before completing the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     *
     * @return the new CompletableFuture
     */
    // 异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public static CompletableFuture<Void> runAsync(Runnable action, Executor executor) {
        return asyncRunStage(screenExecutor(executor), action); // 【任务执行器】需要自行指定
    }
    
    /*▲ AsyncRun ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniApply ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> action) {
        return uniApplyStage(null, action);
    }
    
    // 异步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> action) {
        return uniApplyStage(defaultExecutor(), action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> action, Executor executor) {
        return uniApplyStage(screenExecutor(executor), action); // 【任务执行器】需要自行指定
    }
    
    /*▲ UniApply ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniAccept ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行Consumer任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return uniAcceptStage(null, action);
    }
    
    // 异步执行Consumer任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return uniAcceptStage(defaultExecutor(), action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行Consumer任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return uniAcceptStage(screenExecutor(executor), action); // 【任务执行器】需要自行指定
    }
    
    /*▲ UniAccept ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniRun ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> thenRun(Runnable action) {
        return uniRunStage(null, action);
    }
    
    // 异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return uniRunStage(defaultExecutor(), action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return uniRunStage(screenExecutor(executor), action); // 【任务执行器】需要自行指定
    }
    
    /*▲ UniRun ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ BiApply ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行BiFunction任务，返回该任务所属阶段(可从中获取执行结果)
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> upFuture2, BiFunction<? super T, ? super U, ? extends V> action) {
        return biApplyStage(null, upFuture2, action);
    }
    
    // 异步执行BiFunction任务，返回该任务所属阶段(可从中获取执行结果)
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> upFuture2, BiFunction<? super T, ? super U, ? extends V> action) {
        return biApplyStage(defaultExecutor(), upFuture2, action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行BiFunction任务，返回该任务所属阶段(可从中获取执行结果)
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> upFuture2, BiFunction<? super T, ? super U, ? extends V> action, Executor executor) {
        return biApplyStage(screenExecutor(executor), upFuture2, action); // 【任务执行器】需要自行指定
    }
    
    /*▲ BiApply ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ BiAccept ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行BiConsumer任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> upFuture2, BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(null, upFuture2, action);
    }
    
    // 异步执行BiConsumer任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> upFuture2, BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(defaultExecutor(), upFuture2, action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行BiConsumer任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> upFuture2, BiConsumer<? super T, ? super U> action, Executor executor) {
        return biAcceptStage(screenExecutor(executor), upFuture2, action); // 【任务执行器】需要自行指定
    }
    
    /*▲ BiAccept ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ BiRun ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> upFuture2, Runnable action) {
        return biRunStage(null, upFuture2, action);
    }
    
    // 异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> upFuture2, Runnable action) {
        return biRunStage(defaultExecutor(), upFuture2, action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> upFuture2, Runnable action, Executor executor) {
        return biRunStage(screenExecutor(executor), upFuture2, action); // 【任务执行器】需要自行指定
    }
    
    /*▲ BiRun ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ OrApply ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> upFuture2, Function<? super T, U> action) {
        return orApplyStage(null, upFuture2, action);
    }
    
    // 异步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> upFuture2, Function<? super T, U> action) {
        return orApplyStage(defaultExecutor(), upFuture2, action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> upFuture2, Function<? super T, U> action, Executor executor) {
        return orApplyStage(screenExecutor(executor), upFuture2, action); // 【任务执行器】需要自行指定
    }
    
    /*▲ OrApply ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ OrAccept ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行Consumer任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> upFuture2, Consumer<? super T> action) {
        return orAcceptStage(null, upFuture2, action);
    }
    
    // 异步执行Consumer任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> upFuture2, Consumer<? super T> action) {
        return orAcceptStage(defaultExecutor(), upFuture2, action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行Consumer任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> upFuture2, Consumer<? super T> action, Executor executor) {
        return orAcceptStage(screenExecutor(executor), upFuture2, action); // 【任务执行器】需要自行指定
    }
    
    /*▲ OrAccept ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ OrRun ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> upFuture2, Runnable action) {
        return orRunStage(null, upFuture2, action);
    }
    
    // 异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> upFuture2, Runnable action) {
        return orRunStage(defaultExecutor(), upFuture2, action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> upFuture2, Runnable action, Executor executor) {
        return orRunStage(screenExecutor(executor), upFuture2, action); // 【任务执行器】需要自行指定
    }
    
    /*▲ OrRun ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniCompose ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> action) {
        return uniComposeStage(null, action);
    }
    
    // 异步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> action) {
        return uniComposeStage(defaultExecutor(), action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> action, Executor executor) {
        return uniComposeStage(screenExecutor(executor), action); // 【任务执行器】需要自行指定
    }
    
    /*▲ UniCompose ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniHandle ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行BiFunction任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> action) {
        return uniHandleStage(null, action);
    }
    
    // 异步执行BiFunction任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> action) {
        return uniHandleStage(defaultExecutor(), action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行BiFunction任务，返回该任务所属阶段(可从中获取执行结果)
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> action, Executor executor) {
        return uniHandleStage(screenExecutor(executor), action); // 【任务执行器】需要自行指定
    }
    
    /*▲ UniHandle ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniWhenComplete ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 同步执行BiConsumer任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(null, action);
    }
    
    // 异步执行BiConsumer任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(defaultExecutor(), action); // 【任务执行器】是【共享工作池】
    }
    
    // 异步执行BiConsumer任务，返回该任务所属阶段(可从中获取执行结果)
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return uniWhenCompleteStage(screenExecutor(executor), action); // 【任务执行器】需要自行指定
    }
    
    /*▲ UniWhenComplete ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ anyOf ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new CompletableFuture that is completed when any of
     * the given CompletableFutures complete, with the same result.
     * Otherwise, if it completed exceptionally, the returned
     * CompletableFuture also does so, with a CompletionException
     * holding this exception as its cause.  If no CompletableFutures
     * are provided, returns an incomplete CompletableFuture.
     *
     * @param cfs the CompletableFutures
     *
     * @return a new CompletableFuture that is completed with the
     * result or exception of any of the given CompletableFutures when
     * one completes
     *
     * @throws NullPointerException if the array or any of its elements are
     *                              {@code null}
     */
    // 执行参数中指定的所有任务，直到其中任一任务执行完之后返回结果
    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... futures) {
        if(futures.length<=1) {
            return (futures.length == 0) ? new CompletableFuture<Object>() : uniCopyStage(futures[0]);
        }
        
        for(CompletableFuture<?> future : futures) {
            if(future.result != null) {
                Object relay = encodeRelay(future.result);
                return new CompletableFuture<Object>(relay);
            }
        }
        
        futures = futures.clone();
        
        CompletableFuture<Object> downFuture = new CompletableFuture<>();
        
        for(CompletableFuture<?> future : futures) {
            AnyOf downTask = new AnyOf(downFuture, future, futures);
            future.unipush(downTask);
        }
        
        /*
         * If d was completed while we were adding completions,
         * we should clean the stack of any sources that may have had completions
         * pushed on their stack after d was completed.
         */
        if(downFuture.result == null) {
            return downFuture;
        }
        
        for(int i = 0, len = futures.length; i<len; i++) {
            if(futures[i].result != null) {
                for(i++; i<len; i++) {
                    if(futures[i].result == null) {
                        futures[i].cleanStack();
                    }
                }
            }
        }
        
        return downFuture;
    }
    
    /*▲ anyOf ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ allOf ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new CompletableFuture that is completed when all of
     * the given CompletableFutures complete.  If any of the given
     * CompletableFutures complete exceptionally, then the returned
     * CompletableFuture also does so, with a CompletionException
     * holding this exception as its cause.  Otherwise, the results,
     * if any, of the given CompletableFutures are not reflected in
     * the returned CompletableFuture, but may be obtained by
     * inspecting them individually. If no CompletableFutures are
     * provided, returns a CompletableFuture completed with the value
     * {@code null}.
     *
     * <p>Among the applications of this method is to await completion
     * of a set of independent CompletableFutures before continuing a
     * program, as in: {@code CompletableFuture.allOf(c1, c2,
     * c3).join();}.
     *
     * @param cfs the CompletableFutures
     *
     * @return a new CompletableFuture that is completed when all of the
     * given CompletableFutures complete
     *
     * @throws NullPointerException if the array or any of its elements are
     *                              {@code null}
     */
    // 执行参数中指定的所有任务，直到全部任务执行完之后返回
    public static CompletableFuture<Void> allOf(CompletableFuture<?>... futures) {
        return andTree(futures, 0, futures.length - 1);
    }
    
    /*▲ allOf ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取任务结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Waits if necessary for this future to complete, and then
     * returns its result.
     *
     * @return the result value
     *
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException    if this future completed exceptionally
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     */
    // 获取任务结果，如果还没有结果，则阻塞等待(考虑线程中断)
    @SuppressWarnings("unchecked")
    public T get() throws InterruptedException, ExecutionException {
        Object result = this.result;
        
        if(result == null) {
            // 获取任务结果，如果任务未完成，需要阻塞等待一段时间(遇到线程中断时会退出)
            result = waitingGet(true);
        }
        
        return (T) reportGet(result);
    }
    
    /**
     * Returns the result value when complete, or throws an
     * (unchecked) exception if completed exceptionally. To better
     * conform with the use of common functional forms, if a
     * computation involved in the completion of this
     * CompletableFuture threw an exception, this method throws an
     * (unchecked) {@link CompletionException} with the underlying
     * exception as its cause.
     *
     * @return the result value
     *
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException   if this future completed
     *                               exceptionally or a completion computation threw an exception
     */
    // 获取任务结果，如果还没有结果，则阻塞等待(不考虑线程中断)
    @SuppressWarnings("unchecked")
    public T join() {
        Object result = this.result;
        
        if(result == null) {
            // 获取任务结果，如果任务未完成，需要阻塞等待一段时间(遇到线程中断时不会退出)
            result = waitingGet(false);
        }
        
        return (T) reportJoin(result);
    }
    
    /**
     * Waits if necessary for at most the given time for this future
     * to complete, and then returns its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     *
     * @return the result value
     *
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException    if this future completed exceptionally
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     * @throws TimeoutException      if the wait timed out
     */
    // 获取任务结果，如果还没有结果，则阻塞等待一段时间，等待时长由超时设置决定，超时后抛出异常
    @SuppressWarnings("unchecked")
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long waitTime = unit.toNanos(timeout);
        
        Object result = this.result;
        
        if(result == null) {
            // 获取任务结果，如果任务未完成，需要阻塞等待一段时间，超时后抛出异常
            result = timedGet(waitTime);
        }
        
        return (T) reportGet(result);
    }
    
    /**
     * Returns the result value (or throws any encountered exception)
     * if completed, else returns the given valueIfAbsent.
     *
     * @param valueIfAbsent the value to return if not completed
     *
     * @return the result value, if completed, else the given valueIfAbsent
     *
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException   if this future completed
     *                               exceptionally or a completion computation threw an exception
     */
    // 立即返回任务结果，如果还没有结果，还有valueIfAbsent作为备用值
    @SuppressWarnings("unchecked")
    public T getNow(T valueIfAbsent) {
        // 如果当前任务还未执行完，则返回备用值
        if(result == null) {
            return valueIfAbsent;
        }
        
        // 报告任务结果(result可以为null)
        return (T) reportJoin(result);
    }
    
    
    /**
     * Returns raw result after waiting, or null if interruptible and interrupted.
     */
    // 获取任务结果，如果任务未完成，需要阻塞等待一段时间(interruptible指示遇到线程中断时是否会退出)
    private Object waitingGet(boolean interruptible) {
        Signaller signaller = null;
        
        boolean queued = false;
        
        Object r;
        
        // 如果任务未完成
        while((r = result) == null) {
            if(signaller == null) {
                // 初始化阻塞块，关联到当前线程
                signaller = new Signaller(interruptible, 0L, 0L);
        
                // 如果当前线程是【工作线程】
                if(Thread.currentThread() instanceof ForkJoinWorkerThread) {
                    // 尝试使用signaller当前所在线程加速指定的【工作池】中的任务完成
                    ForkJoinPool.helpAsyncBlocker(defaultExecutor(), signaller);
                }
        
                continue;
                // 如果当前任务还未进入下游任务栈
            }
    
            if(!queued) {
                // 将signaller加入到下游任务栈栈顶
                queued = tryPushStack(signaller);
                continue;
            }
    
            try {
                // 管理阻塞块(决定当前线程是否需要阻塞)
                ForkJoinPool.managedBlock(signaller);
            } catch(InterruptedException ie) { // currently cannot happen
                signaller.interrupted = true;
            }
    
            // 如果线程带有中断标记，且需要支持中断，则跳出循环，进行中断操作
            if(signaller.interrupted && interruptible) {
                break;
            }
        }
        
        // 如果当前任务在下游任务栈中排队
        if(signaller != null && queued) {
            // 置空阻塞块的线程域
            signaller.thread = null;
            
            // 如果线程带有中断标记，但不需要支持中断
            if(!interruptible && signaller.interrupted) {
                // 为线程设置中断标记
                Thread.currentThread().interrupt();
            }
            
            // 还没有返回结果，说明该任务已不再存活
            if(r == null) {
                // 清理下游任务栈
                cleanStack();
            }
        }
        
        if(r != null || (r = result) != null) {
            // (递归)处理当前阶段的下游任务栈(会触发tryFire(NESTED))
            postComplete();
        }
        
        return r;
    }
    
    /**
     * Returns raw result after waiting, or null if interrupted, or throws TimeoutException on timeout.
     */
    // 获取任务结果，如果任务未完成，需要阻塞等待一段时间，超时后抛出异常
    private Object timedGet(long waitTime) throws TimeoutException {
        if(Thread.interrupted()) {
            return null;
        }
    
        if(waitTime<=0) {
            throw new TimeoutException();
        }
    
        long deadline = System.nanoTime() + waitTime;
        if(deadline == 0L) {
            deadline = 1L;  // avoid 0
        }
    
        Signaller signaller = null;
        boolean queued = false;
        Object r;
    
        while((r = result) == null) { // similar to untimed
            if(signaller == null) {
                signaller = new Signaller(true, waitTime, deadline);
            
                if(Thread.currentThread() instanceof ForkJoinWorkerThread) {
                    // 尝试使用signaller当前所在线程加速指定的【工作池】中的任务完成
                    ForkJoinPool.helpAsyncBlocker(defaultExecutor(), signaller);
                }
            
                continue;
            }
        
            if(!queued) {
                // 将signaller加入到下游任务栈栈顶
                queued = tryPushStack(signaller);
                continue;
            }
        
            if(signaller.nanos<=0L) {
                break;
            }
        
            try {
                ForkJoinPool.managedBlock(signaller);
            } catch(InterruptedException ie) {
                signaller.interrupted = true;
            }
        
            if(signaller.interrupted) {
                break;
            }
        }
    
        if(signaller != null && queued) {
            signaller.thread = null;
            if(r == null) {
                cleanStack();
            }
        }
        
        if(r != null || (r = result) != null) {
            // (递归)处理当前阶段的下游任务栈(会触发tryFire(NESTED))
            postComplete();
        }
        
        if(r != null || (signaller != null && signaller.interrupted)) {
            return r;
        }
    
        throw new TimeoutException();
    }
    
    /*▲ 获取任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 促进任务完成 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * If not already completed, sets the value returned by {@link
     * #get()} and related methods to the given value.
     *
     * @param value the result value
     *
     * @return {@code true} if this invocation caused this CompletableFuture
     * to transition to a completed state, else {@code false}
     */
    // 促使任务完成(如果任务未完成)：设置当前阶段的执行结果为指定的值，并处理该阶段的下游任务栈
    public boolean complete(T result) {
        // 设置任务结果为result(有返回值)
        boolean triggered = completeValue(result);
        
        // (递归)处理当前阶段的下游任务栈(会触发tryFire(NESTED))
        postComplete();
        
        return triggered;
    }
    
    /**
     * If not already completed, causes invocations of {@link #get()}
     * and related methods to throw the given exception.
     *
     * @param ex the exception
     *
     * @return {@code true} if this invocation caused this CompletableFuture
     * to transition to a completed state, else {@code false}
     */
    // 促使任务完成(如果任务未完成)：设置当前阶段的执行结果为指定的异常，并处理该阶段的下游任务栈
    public boolean completeExceptionally(Throwable ex) {
        if(ex == null) {
            throw new NullPointerException();
        }
        
        AltResult result = new AltResult(ex);
        
        // 更新当前阶段的任务结果为result
        boolean triggered = internalComplete(result);
        
        // (递归)处理当前阶段的下游任务栈(会触发tryFire(NESTED))
        postComplete();
        
        return triggered;
    }
    
    /**
     * Forcibly sets or resets the value subsequently returned by
     * method {@link #get()} and related methods, whether or not
     * already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param value the completion value
     */
    // 强制促使任务完成：设置当前阶段的执行结果为result；如果result为null，会使用NIL替换
    public void obtrudeValue(T result) {
        this.result = (result == null) ? NIL : result;
        
        // (递归)处理当前阶段的下游任务栈(会触发tryFire(NESTED))
        postComplete();
    }
    
    /**
     * Forcibly causes subsequent invocations of method {@link #get()}
     * and related methods to throw the given exception, whether or
     * not already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param ex the exception
     *
     * @throws NullPointerException if the exception is null
     */
    // 强制促使任务完成：设置当前阶段的执行结果为指定的异常，并处理该阶段的下游任务栈
    public void obtrudeException(Throwable ex) {
        if(ex == null) {
            throw new NullPointerException();
        }
        
        result = new AltResult(ex);
        
        // (递归)处理当前阶段的下游任务栈(会触发tryFire(NESTED))
        postComplete();
    }
    
    /*▲ 促进任务完成 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ 工具箱 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new CompletableFuture that is already completed with the given value.
     *
     * @param value the value
     * @param <U>   the type of the value
     *
     * @return the completed CompletableFuture
     */
    // 构造一个执行结果为result的阶段；如果result为null，则会将其替换为NIL
    public static <T> CompletableFuture<T> completedFuture(T result) {
        return new CompletableFuture<T>((result == null) ? NIL : result);
    }
    
    /**
     * Returns a new CompletableFuture that is already completed
     * exceptionally with the given exception.
     *
     * @param ex  the exception
     * @param <U> the type of the value
     *
     * @return the exceptionally completed CompletableFuture
     *
     * @since 9
     */
    // 构造一个执行结果为指定异常ex的阶段(异常ex会被包装到AltResul中)
    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        if(ex == null) {
            throw new NullPointerException();
        }
        
        return new CompletableFuture<U>(new AltResult(ex));
    }
    
    /**
     * Returns a new CompletionStage that is already completed with
     * the given value and supports only those methods in
     * interface {@link CompletionStage}.
     *
     * @param value the value
     * @param <U>   the type of the value
     *
     * @return the completed CompletionStage
     *
     * @since 9
     */
    // 构造一个执行结果为result的"退化"阶段；如果result为null，则会将其替换为NIL
    public static <T> CompletionStage<T> completedStage(T result) {
        return new MinimalStage<T>((result == null) ? NIL : result);
    }
    
    /**
     * Returns a new CompletionStage that is already completed
     * exceptionally with the given exception and supports only those
     * methods in interface {@link CompletionStage}.
     *
     * @param ex  the exception
     * @param <U> the type of the value
     *
     * @return the exceptionally completed CompletionStage
     *
     * @since 9
     */
    // 构造一个执行结果为指定异常ex的"退化"阶段(异常ex会被包装到AltResul中)
    public static <T> CompletionStage<T> failedStage(Throwable ex) {
        if(ex == null) {
            throw new NullPointerException();
        }
        
        return new MinimalStage<T>(new AltResult(ex));
    }
    
    
    /**
     * Returns this CompletableFuture.
     *
     * @return this CompletableFuture
     */
    // 对当前阶段进行转换，默认是原样返回
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }
    
    /**
     * Returns a new incomplete CompletableFuture of the type to be
     * returned by a CompletionStage method. Subclasses should
     * normally override this method to return an instance of the same
     * class as this CompletableFuture. The default implementation
     * returns an instance of class CompletableFuture.
     *
     * @param <U> the type of the value
     *
     * @return a new CompletableFuture
     *
     * @since 9
     */
    // 返回一个残缺的阶段，残缺的含义是该阶段未设置执行结果，需要依赖其他阶段
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CompletableFuture<U>();
    }
    
    /**
     * Returns a new CompletionStage that is completed normally with
     * the same value as this CompletableFuture when it completes
     * normally, and cannot be independently completed or otherwise
     * used in ways not defined by the methods of interface {@link
     * CompletionStage}.  If this CompletableFuture completes
     * exceptionally, then the returned CompletionStage completes
     * exceptionally with a CompletionException with this exception as
     * cause.
     *
     * <p>Unless overridden by a subclass, a new non-minimal
     * CompletableFuture with all methods available can be obtained from
     * a minimal CompletionStage via {@link #toCompletableFuture()}.
     * For example, completion of a minimal stage can be awaited by
     *
     * <pre> {@code minimalStage.toCompletableFuture().join(); }</pre>
     *
     * @return the new CompletionStage
     *
     * @since 9
     */
    // 将当前阶段包装为一个"退化"阶段后返回
    public CompletionStage<T> minimalCompletionStage() {
        return uniAsMinimalStage();
    }
    
    /**
     * Returns a new CompletableFuture that is completed when this
     * CompletableFuture completes, with the result of the given
     * function of the exception triggering this CompletableFuture's
     * completion when it completes exceptionally; otherwise, if this
     * CompletableFuture completes normally, then the returned
     * CompletableFuture also completes normally with the same value.
     * Note: More flexible versions of this functionality are
     * available using methods {@code whenComplete} and {@code handle}.
     *
     * @param fn the function to use to compute the value of the
     *           returned CompletableFuture if this CompletableFuture completed
     *           exceptionally
     *
     * @return the new CompletableFuture
     */
    // 返回一个新阶段：该阶段会使用当前阶段的执行结果；如果当前阶段产生的是"异常"结果，则会先通过action来处理/转换它
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> action) {
        return uniExceptionallyStage(action);
    }
    
    /**
     * Completes this CompletableFuture with the result of the given
     * Supplier function invoked from an asynchronous task using the
     * default executor.
     *
     * @param supplier a function returning the value to be used
     *                 to complete this CompletableFuture
     *
     * @return this CompletableFuture
     *
     * @since 9
     */
    // 用action为当前阶段设置执行结果，设置完成后返回当前阶段；设置过程通过【共享工作池】完成
    public CompletableFuture<T> completeAsync(Supplier<? extends T> action) {
        return completeAsync(action, defaultExecutor());
    }
    
    /**
     * Completes this CompletableFuture with the result of
     * the given Supplier function invoked from an asynchronous
     * task using the given executor.
     *
     * @param supplier a function returning the value to be used
     *                 to complete this CompletableFuture
     * @param executor the executor to use for asynchronous execution
     *
     * @return this CompletableFuture
     *
     * @since 9
     */
    // 用action为当前阶段设置执行结果，设置完成后返回当前阶段；设置过程通过executor完成
    public CompletableFuture<T> completeAsync(Supplier<? extends T> action, Executor executor) {
        if(action == null || executor == null) {
            throw new NullPointerException();
        }
        
        AsyncSupply<T> task = new AsyncSupply<>(this, action);
        
        executor.execute(task);
        
        return this;
    }
    
    /**
     * Exceptionally completes this CompletableFuture with
     * a {@link TimeoutException} if not otherwise completed
     * before the given timeout.
     *
     * @param delay how long to wait before completing exceptionally
     *              with a TimeoutException, in units of {@code unit}
     * @param unit  a {@code TimeUnit} determining how to interpret the
     *              {@code timeout} parameter
     *
     * @return this CompletableFuture
     *
     * @since 9
     */
    // 限制当前任务在initialDelay时长内执行完成，如果超时后当前任务还未完成，则会为当前任务设置一个异常结果
    public CompletableFuture<T> orTimeout(long initialDelay, TimeUnit unit) {
        if(unit == null) {
            throw new NullPointerException();
        }
        
        // 如果当前任务已有结果，直接返回
        if(result != null) {
            return this;
        }
        
        // 特殊任务：如果当前任务未在指定时间内完成时，需要为当前任务设置一个异常结果
        Timeout task = new Timeout(this);
        
        // 执行一次性的定时任务，并返回任务本身：在任务启动后的initialDelay时长后开始执行(在一个守护线程中执行)
        ScheduledFuture<?> future = Delayer.delay(task, initialDelay, unit);
        
        
        // 定时任务清理器，如果当前任务执行很快，在future到期之前就执行完了，那么该清理器就派上用场了
        Canceller action = new Canceller(future);
        
        /*
         * 等待当前任务(作为上游)执行完成后，会将当前任务的执行结果和抛出的异常作为入参传递给action的accept()方法并执行它；
         * accept()方法主要用来取消之前注册的定时任务future，当然，前提是future中没有发生异常。
         */
        whenComplete(action);
        
        return this;
    }
    
    /**
     * Completes this CompletableFuture with the given value if not
     * otherwise completed before the given timeout.
     *
     * @param value   the value to use upon timeout
     * @param delay   how long to wait before completing normally
     *                with the given value, in units of {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     *
     * @return this CompletableFuture
     *
     * @since 9
     */
    // 限制当前任务在initialDelay时长内执行完成，如果超时后当前任务还未完成，则会为当前任务设置一个备用的执行结果
    public CompletableFuture<T> completeOnTimeout(T bakResult, long initialDelay, TimeUnit unit) {
        if(unit == null) {
            throw new NullPointerException();
        }
        
        // 如果当前任务已有结果，直接返回
        if(result != null) {
            return this;
        }
        
        // 特殊任务：如果当前任务未在指定时间内完成时，需要为当前任务设置一个备用的执行结果
        DelayedCompleter<T> task = new DelayedCompleter<>(this, bakResult);
        
        // 执行一次性的定时任务，并返回任务本身：在任务启动后的initialDelay时长后开始执行(在一个守护线程中执行)
        ScheduledFuture<?> future = Delayer.delay(task, initialDelay, unit);
        
        // 定时任务清理器，如果当前任务执行很快，在future到期之前就执行完了，那么该清理器就派上用场了
        Canceller action = new Canceller(future);
        
        /*
         * 等待当前任务(作为上游)执行完成后，会将当前任务的执行结果和抛出的异常作为入参传递给action的accept()方法并执行它；
         * accept()方法主要用来取消之前注册的定时任务future，当然，前提是future中没有发生异常。
         */
        whenComplete(action);
        
        return this;
    }
    
    /**
     * Returns a new CompletableFuture that is completed normally with
     * the same value as this CompletableFuture when it completes
     * normally. If this CompletableFuture completes exceptionally,
     * then the returned CompletableFuture completes exceptionally
     * with a CompletionException with this exception as cause. The
     * behavior is equivalent to {@code thenApply(x -> x)}. This
     * method may be useful as a form of "defensive copying", to
     * prevent clients from completing, while still being able to
     * arrange dependent actions.
     *
     * @return the new CompletableFuture
     *
     * @since 9
     */
    // 返回对指定阶段的一个拷贝
    public CompletableFuture<T> copy() {
        return uniCopyStage(this);
    }
    
    
    // 返回一个新阶段：该阶段会使用当前阶段的执行结果；如果当前阶段产生的是"异常"结果，则会先通过action来处理/转换它
    private CompletableFuture<T> uniExceptionallyStage(Function<Throwable, ? extends T> action) {
        if(action == null) {
            throw new NullPointerException();
        }
        
        CompletableFuture<T> downFuture = newIncompleteFuture();
        
        // 如果当前阶段已有执行结果
        if(result != null) {
            /*
             * 如果当前阶段产生的是"异常"结果，则尝试用action处理该"异常"，并将处理后的结果重新设置为downFuture阶段执行结果；
             * 否则，downFuture阶段会直接使用当前阶段的结果。
             */
            downFuture.uniExceptionally(result, action, null);
        } else {
            UniExceptionally<T> downTask = new UniExceptionally<>(downFuture, this, action);
            unipush(downTask);
        }
        
        return downFuture;
    }
    
    // 返回对指定阶段的一个拷贝
    private static <U, T extends U> CompletableFuture<U> uniCopyStage(CompletableFuture<T> future) {
        CompletableFuture<U> newFuture = future.newIncompleteFuture();
        
        // 如果待拷贝阶段已有执行结果，直接为新的任务复制
        if(future.result != null) {
            newFuture.result = encodeRelay(future.result);
            
            // 如果待拷贝阶段还没执行完，则需要挂起
        } else {
            UniRelay<U, T> downTask = new UniRelay<>(newFuture, future);
            future.unipush(downTask);
        }
        
        return newFuture;
    }
    
    // 将当前阶段包装为一个"退化"阶段后返回
    private MinimalStage<T> uniAsMinimalStage() {
        
        if(result != null) {
            return new MinimalStage<T>(encodeRelay(result));
        }
        
        MinimalStage<T> downFuture = new MinimalStage<T>();
        
        UniRelay<T, T> downTask = new UniRelay<>(downFuture, this);
        
        unipush(downTask);
        
        return downFuture;
    }
    
    /*
     * 如果上游阶段产生的是"异常"结果，则尝试用action处理该"异常"，并将处理后的结果重新设置为当前阶段执行结果；
     * 否则，当前阶段会直接使用上游阶段的结果。
     */
    final boolean uniExceptionally(Object upResult, Function<? super Throwable, ? extends T> action, UniExceptionally<T> task) {
        
        // 当前阶段任务已有执行结果，直接返回
        if(result != null) {
            return true;
        }
        
        try {
            Throwable ex;
            
            // 如果upResult是"异常"结果
            if(upResult instanceof AltResult && (ex = ((AltResult) upResult).ex) != null) {
                if(task != null && !task.claim()) {
                    return false;
                }
                
                // 根据给定的异常，计算出一个执行结果，然后再将其设置为当前阶段的执行结果
                completeValue(action.apply(ex));
                
                // 如果upResult是普通结果
            } else {
                // 更新当前阶段的任务结果为result
                internalComplete(upResult);
            }
        } catch(Throwable e) {
            completeThrowable(e);
        }
        
        return true;
    }
    
    /*▲ 工具箱 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 任务执行器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /* 注：此处出现的定时器是在守护线程中运行的 */
    
    /**
     * Returns a new Executor that submits a task to the default
     * executor after the given delay (or no delay if non-positive).
     * Each delay commences upon invocation of the returned executor's
     * {@code execute} method.
     *
     * @param delay how long to delay, in units of {@code unit}
     * @param unit  a {@code TimeUnit} determining how to interpret the
     *              {@code delay} parameter
     *
     * @return the new delayed executor
     *
     * @since 9
     */
    // 构造一次性定时任务执行器：该执行器可以在启动后的initialDelay时长后，用【共享工作池】执行后续给定的任务
    public static Executor delayedExecutor(long initialDelay, TimeUnit unit) {
        if(unit == null) {
            throw new NullPointerException();
        }
        
        return new DelayedExecutor(initialDelay, unit, ASYNC_POOL);
    }
    
    /**
     * Returns a new Executor that submits a task to the given base
     * executor after the given delay (or no delay if non-positive).
     * Each delay commences upon invocation of the returned executor's
     * {@code execute} method.
     *
     * @param delay    how long to delay, in units of {@code unit}
     * @param unit     a {@code TimeUnit} determining how to interpret the
     *                 {@code delay} parameter
     * @param executor the base executor
     *
     * @return the new delayed executor
     *
     * @since 9
     */
    // 构造一次性定时任务执行器：该执行器可以在启动后的initialDelay时长后，用给定的executor执行后续给定的任务
    public static Executor delayedExecutor(long initialDelay, TimeUnit unit, Executor executor) {
        if(unit == null || executor == null) {
            throw new NullPointerException();
        }
        
        return new DelayedExecutor(initialDelay, unit, executor);
    }
    
    /**
     * Returns the default Executor used for async methods that do not
     * specify an Executor. This class uses the {@link
     * ForkJoinPool#commonPool()} if it supports more than one
     * parallel thread, or else an Executor using one thread per async
     * task.  This method may be overridden in subclasses to return
     * an Executor that provides at least one independent thread.
     *
     * @return the executor
     *
     * @since 9
     */
    // 返回默认的任务执行器：【共享工作池】
    public Executor defaultExecutor() {
        return ASYNC_POOL;
    }
    
    
    /**
     * Null-checks user executor argument, and translates uses of commonPool to ASYNC_POOL in case parallelism disabled.
     */
    // 并行性检查与非空检查
    static Executor screenExecutor(Executor executor) {
        // 如果不可以使用共享工作池，但给定的executor就是共享工作池
        if(!USE_COMMON_POOL && executor == ForkJoinPool.commonPool()) {
            // 将executor回退为单线程任务执行器
            return ASYNC_POOL;
        }
        
        if(executor == null) {
            throw new NullPointerException();
        }
        
        return executor;
    }
    
    /*▲ 任务执行器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 阶段状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if completed in any fashion: normally,
     * exceptionally, or via cancellation.
     *
     * @return {@code true} if completed
     */
    // 判断当前阶段任务是否已完成(是否有了执行结果)
    public boolean isDone() {
        return this.result != null;
    }
    
    /**
     * Returns {@code true} if this CompletableFuture completed
     * exceptionally, in any way. Possible causes include
     * cancellation, explicit invocation of {@code
     * completeExceptionally}, and abrupt termination of a
     * CompletionStage action.
     *
     * @return {@code true} if this CompletableFuture completed exceptionally
     */
    // 判断当前任务的执行结果是否为异常
    public boolean isCompletedExceptionally() {
        return (this.result instanceof AltResult) && this.result != NIL;
    }
    
    /**
     * Returns {@code true} if this CompletableFuture was cancelled
     * before it completed normally.
     *
     * @return {@code true} if this CompletableFuture was cancelled
     * before it completed normally
     */
    // 判断当前阶段任务是否已取消
    public boolean isCancelled() {
        return (result instanceof AltResult) && (((AltResult) result).ex instanceof CancellationException);
    }
    
    /**
     * If not already completed, completes this CompletableFuture with
     * a {@link CancellationException}. Dependent CompletableFutures
     * that have not already completed will also complete
     * exceptionally, with a {@link CompletionException} caused by
     * this {@code CancellationException}.
     *
     * @param mayInterruptIfRunning this value has no effect in this
     *                              implementation because interrupts are not used to control
     *                              processing.
     *
     * @return {@code true} if this task is now cancelled
     */
    // 取消任务，任务执行结果会被设置为一个异常
    public boolean cancel(boolean mayInterruptIfRunning) {
        CancellationException exception = new CancellationException();
        
        AltResult altResult = new AltResult(exception);
        
        // 更新当前阶段的任务结果为result
        boolean cancelled = (result == null) && internalComplete(altResult);
        
        // (递归)处理当前阶段的下游任务栈(会触发tryFire(NESTED))
        postComplete();
        
        return cancelled || isCancelled();
    }
    
    /**
     * Returns the estimated number of CompletableFutures whose
     * completions are awaiting completion of this CompletableFuture.
     * This method is designed for use in monitoring system state, not
     * for synchronization control.
     *
     * @return the number of dependent CompletableFutures
     */
    // 获取当前阶段下游任务栈中的下游任务数量
    public int getNumberOfDependents() {
        int count = 0;
        
        for(Completion p = stack; p != null; p = p.next) {
            ++count;
        }
        
        return count;
    }
    
    
    // 更新当前阶段的任务结果为result
    final boolean internalComplete(Object result) { // CAS from null to r
        return RESULT.compareAndSet(this, null, result);
    }
    
    /*▲ 阶段状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 执行任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 异步执行Supplier任务，返回该任务所属阶段(可从中获取执行结果)
    static <U> CompletableFuture<U> asyncSupplyStage(Executor executor, Supplier<U> action) {
        if(action == null) {
            throw new NullPointerException();
        }
        
        // 待填充执行结果的阶段
        CompletableFuture<U> future = new CompletableFuture<U>();
        
        // 构造需要异步执行的阶段任务
        AsyncSupply<U> task = new AsyncSupply<>(future, action);
        
        // 异步执行
        executor.execute(task);
        
        return future;
    }
    
    // 异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    static CompletableFuture<Void> asyncRunStage(Executor executor, Runnable action) {
        if(action == null) {
            throw new NullPointerException();
        }
        
        // 待填充执行结果的阶段
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // 构造需要异步执行的阶段任务
        AsyncRun task = new AsyncRun(future, action);
        
        // 异步执行
        executor.execute(task);
        
        return future;
    }
    
    // 同步/异步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    private <V> CompletableFuture<V> uniApplyStage(Executor executor, Function<? super T, ? extends V> action) {
        if(action == null) {
            throw new NullPointerException();
        }
        
        // 如果当前阶段(作为上游)已有执行结果
        if(result != null) {
            // 立即执行下游阶段的任务
            return uniApplyNow(result, executor, action);
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<V> downFuture = newIncompleteFuture();
        
        // 构造需要同步/异步执行的下游阶段任务
        UniApply<T, V> downTask = new UniApply<>(executor, downFuture, this, action);
        
        // 尝试将下游任务downTask送入下游任务栈，如果(作为上游的)当前阶段任务已有执行结果，则改为同步执行指定任务
        unipush(downTask);
        
        return downFuture;
    }
    
    // 同步/异步执行Consumer任务，返回该任务所属阶段(可从中获取执行结果)
    private CompletableFuture<Void> uniAcceptStage(Executor executor, Consumer<? super T> action) {
        if(action == null) {
            throw new NullPointerException();
        }
        
        // 如果当前阶段(作为上游)已有执行结果
        if(result != null) {
            // 立即执行下游阶段的任务
            return uniAcceptNow(result, executor, action);
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<Void> downFuture = newIncompleteFuture();
        
        // 构造需要同步/异步执行的下游阶段任务
        UniAccept<T> downTask = new UniAccept<>(executor, downFuture, this, action);
        
        // 尝试将下游任务downTask送入下游任务栈，如果(作为上游的)当前阶段任务已有执行结果，则改为同步执行指定任务
        unipush(downTask);
        
        return downFuture;
    }
    
    // 同步/异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    private CompletableFuture<Void> uniRunStage(Executor executor, Runnable action) {
        if(action == null) {
            throw new NullPointerException();
        }
        
        // 如果当前阶段(作为上游)已有执行结果
        if(result != null) {
            // 立即执行下游阶段的任务
            return uniRunNow(result, executor, action);
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<Void> downFuture = newIncompleteFuture();
        
        // 构造需要同步/异步执行的下游阶段任务
        UniRun<T> downTask = new UniRun<>(executor, downFuture, this, action);
        
        // 尝试将下游任务downTask送入下游任务栈，如果(作为上游的)当前阶段任务已有执行结果，则改为同步执行指定任务
        unipush(downTask);
        
        return downFuture;
    }
    
    // 同步/异步执行BiFunction任务，返回该任务所属阶段(可从中获取执行结果)
    private <U, V> CompletableFuture<V> biApplyStage(Executor executor, CompletionStage<U> upStage2, BiFunction<? super T, ? super U, ? extends V> action) {
        // 另一个上游阶段
        CompletableFuture<U> upFuture2 = upStage2.toCompletableFuture();
        
        if(action == null || upFuture2 == null) {
            throw new NullPointerException();
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<V> downFuture = newIncompleteFuture();
        
        // 两个上游阶段的任务之中，至少有一个未完成
        if(result == null || upFuture2.result == null) {
            
            BiApply<T, U, V> downTask = new BiApply<>(executor, downFuture, this, upFuture2, action);
            
            bipush(upFuture2, downTask);
            
            return downFuture;
        }
        
        // 两个上游阶段均有了任务结果，但任务执行器为null，则这里同步计算下游阶段的任务结果
        if(executor == null) {
            downFuture.biApply(result, upFuture2.result, action, null);
            return downFuture;
        }
        
        // 两个上游阶段均有了任务结果，且任务执行器不为null，则使用executor执行下游任务
        try {
            // 构造需要异步执行的下游阶段任务
            BiApply<T, U, V> downTask = new BiApply<>(null, downFuture, this, upFuture2, action);
            executor.execute(downTask);
        } catch(Throwable ex) {
            downFuture.result = encodeThrowable(ex);
        }
        
        return downFuture;
    }
    
    // 同步/异步执行BiConsumer任务，返回该任务所属阶段(可从中获取执行结果)
    private <U> CompletableFuture<Void> biAcceptStage(Executor executor, CompletionStage<U> upStage2, BiConsumer<? super T, ? super U> action) {
        // 另一个源阶段的任务
        CompletableFuture<U> upFuture2 = upStage2.toCompletableFuture();
        
        if(action == null || upFuture2 == null) {
            throw new NullPointerException();
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<Void> downFuture = newIncompleteFuture();
        
        // 两个上游阶段的任务之中，至少有一个未完成
        if(result == null || upFuture2.result == null) {
            BiAccept<T, U> task = new BiAccept<>(executor, downFuture, this, upFuture2, action);
            
            bipush(upFuture2, task);
            
            return downFuture;
        }
        
        // 两个上游阶段均有了任务结果，且任务执行器为null，则这里同步计算当前阶段的任务结果
        if(executor == null) {
            downFuture.biAccept(result, upFuture2.result, action, null);
            return downFuture;
        }
        
        // 两个上游阶段均有了任务结果，且任务执行器不为null，则使用executor执行下游任务
        try {
            // 构造需要异步执行的下游阶段任务
            BiAccept<T, U> downTask = new BiAccept<>(null, downFuture, this, upFuture2, action);
            executor.execute(downTask);
        } catch(Throwable ex) {
            downFuture.result = encodeThrowable(ex);
        }
        
        return downFuture;
    }
    
    // 同步/异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    private CompletableFuture<Void> biRunStage(Executor executor, CompletionStage<?> upStage2, Runnable action) {
        // 另一个源阶段的任务
        CompletableFuture<?> upFuture2 = upStage2.toCompletableFuture();
        
        if(action == null || upFuture2 == null) {
            throw new NullPointerException();
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<Void> downFuture = newIncompleteFuture();
        
        // 两个上游阶段的任务之中，至少有一个未完成
        if(result == null || upFuture2.result == null) {
            BiRun<T, ?> task = new BiRun<>(executor, downFuture, this, upFuture2, action);
            
            bipush(upFuture2, task);
            
            return downFuture;
        }
        
        // 两个上游阶段均有了任务结果，且任务执行器为null，则这里同步计算当前阶段的任务结果
        if(executor == null) {
            downFuture.biRun(result, upFuture2.result, action, null);
            return downFuture;
        }
        
        // 两个上游阶段均有了任务结果，且任务执行器不为null，则使用executor执行下游任务
        try {
            // 构造需要异步执行的下游阶段任务
            BiRun<T, ?> downTask = new BiRun<>(null, downFuture, this, upFuture2, action);
            executor.execute(downTask);
        } catch(Throwable ex) {
            downFuture.result = encodeThrowable(ex);
        }
        
        return downFuture;
    }
    
    // 同步/异步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    private <U extends T, V> CompletableFuture<V> orApplyStage(Executor executor, CompletionStage<U> upStage2, Function<? super T, ? extends V> action) {
        // 另一个源阶段的任务
        CompletableFuture<U> upFuture2 = upStage2.toCompletableFuture();
        
        if(action == null || upFuture2 == null) {
            throw new NullPointerException();
        }
        
        // 如果当前阶段(作为上游)已有执行结果，则可以返回
        if(result != null) {
            return uniApplyNow(result, executor, action);
        }
        
        // 如果另一个上游阶段已有执行结果，也可以返回
        if(upFuture2.result != null) {
            return upFuture2.uniApplyNow(upFuture2.result, executor, action);
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<V> downFuture = newIncompleteFuture();
        
        // 构造需要同步/异步执行的下游阶段任务
        OrApply<T, U, V> downTask = new OrApply<>(executor, downFuture, this, upFuture2, action);
        
        orpush(upFuture2, downTask);
        
        return downFuture;
    }
    
    // 同步/异步执行Consumer任务，返回该任务所属阶段(可从中获取执行结果)
    private <U extends T> CompletableFuture<Void> orAcceptStage(Executor executor, CompletionStage<U> upStage2, Consumer<? super T> action) {
        // 另一个源阶段的任务
        CompletableFuture<U> upFuture2 = upStage2.toCompletableFuture();
        
        if(action == null || upFuture2 == null) {
            throw new NullPointerException();
        }
        
        // 如果当前阶段(作为上游)已有执行结果，则可以返回
        if(result != null) {
            return uniAcceptNow(result, executor, action);
        }
        
        // 如果另一个上游阶段已有执行结果，也可以返回
        if(upFuture2.result != null) {
            return upFuture2.uniAcceptNow(upFuture2.result, executor, action);
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<Void> downFuture = newIncompleteFuture();
        
        // 构造需要同步/异步执行的下游阶段任务
        OrAccept<T, U> downTask = new OrAccept<>(executor, downFuture, this, upFuture2, action);
        
        orpush(upFuture2, downTask);
        
        return downFuture;
    }
    
    // 同步/异步执行Runnable任务，返回该任务所属阶段(可从中获取执行结果)
    private CompletableFuture<Void> orRunStage(Executor executor, CompletionStage<?> upStage2, Runnable action) {
        // 另一个源阶段的任务
        CompletableFuture<?> upFuture2 = upStage2.toCompletableFuture();
        
        if(action == null || upFuture2 == null) {
            throw new NullPointerException();
        }
        
        // 如果当前阶段(作为上游)已有执行结果，则可以返回
        if(result != null) {
            return uniRunNow(result, executor, action);
        }
        
        // 如果另一个上游阶段已有执行结果，也可以返回
        if(upFuture2.result != null) {
            return upFuture2.uniRunNow(upFuture2.result, executor, action);
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<Void> downFuture = newIncompleteFuture();
        
        // 构造需要同步/异步执行的下游阶段任务
        OrRun<T, ?> downTask = new OrRun<>(executor, downFuture, this, upFuture2, action);
        
        orpush(upFuture2, downTask);
        
        return downFuture;
    }
    
    // 同步/异步执行Function任务，返回该任务所属阶段(可从中获取执行结果)
    private <V> CompletableFuture<V> uniComposeStage(Executor executor, Function<? super T, ? extends CompletionStage<V>> action) {
        if(action == null) {
            throw new NullPointerException();
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<V> downFuture = newIncompleteFuture();
        
        Object upResult = this.result;
        
        // 如果当前阶段(作为上游)还没执行结果，则需要构造下游任务，并加入下游任务栈等待执行完
        if(upResult == null) {
            UniCompose<T, V> downTask = new UniCompose<>(executor, downFuture, this, action);
            unipush(downTask);
            return downFuture;
        }
        
        // 如果当前阶段(作为上游)已有执行结果，且executor为null，则可以同步执行任务
        if(executor == null) {
            // 如果当前阶段(作为上游)的任务执行结果是null或异常(被包装到了AltResult中)
            if(upResult instanceof AltResult) {
                Throwable ex = ((AltResult) upResult).ex;
                
                // 如果当前阶段(作为上游)产生的是异常
                if(ex != null) {
                    // 将upResult设置为当前阶段的执行结果，并结束当前任务
                    downFuture.result = encodeThrowable(ex, upResult);
                    return downFuture;
                }
                
                // 至此，说明当前阶段(作为上游)的结果是null(NIL)
                upResult = null;
            }
            
            try {
                CompletionStage<V> stage = action.apply((T) upResult);
                
                CompletableFuture<V> tmpFuture = stage.toCompletableFuture();
                
                if(tmpFuture.result != null) {
                    downFuture.result = encodeRelay(tmpFuture.result);
                    
                    // 注：如果tmpFuture中没有设置执行结果，后续会陷入阻塞
                } else {
                    UniRelay<V, V> downTask = new UniRelay<>(downFuture, tmpFuture);
                    tmpFuture.unipush(downTask);
                }
            } catch(Throwable ex) {
                downFuture.result = encodeThrowable(ex);
            }
            
            return downFuture;
        }
        
        // 如果当前阶段(作为上游)已有执行结果，但是executor不为null，则需要异步执行任务
        try {
            UniCompose<T, V> downTask = new UniCompose<>(null, downFuture, this, action);
            executor.execute(downTask);
        } catch(Throwable ex) {
            downFuture.result = encodeThrowable(ex);
        }
        
        return downFuture;
    }
    
    // 同步/异步执行BiFunction任务，返回该任务所属阶段(可从中获取执行结果)
    private <V> CompletableFuture<V> uniHandleStage(Executor executor, BiFunction<? super T, Throwable, ? extends V> action) {
        if(action == null) {
            throw new NullPointerException();
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<V> downFuture = newIncompleteFuture();
        
        // 如果当前阶段(作为上游)还没执行结果，则需要构造下游任务，并加入下游任务栈等待执行完
        if(result == null) {
            UniHandle<T, V> downTask = new UniHandle<>(executor, downFuture, this, action);
            unipush(downTask);
            return downFuture;
        }
        
        // 如果当前阶段(作为上游)已有执行结果，且executor为null，则可以同步执行任务
        if(executor == null) {
            downFuture.uniHandle(result, action, null);
            return downFuture;
        }
        
        // 如果当前阶段(作为上游)已有执行结果，但是executor不为null，则需要异步执行任务
        try {
            UniHandle<T, V> downTask = new UniHandle<>(null, downFuture, this, action);
            executor.execute(downTask);
        } catch(Throwable ex) {
            downFuture.result = encodeThrowable(ex);
        }
        
        return downFuture;
    }
    
    // 同步/异步执行BiConsumer任务，返回该任务所属阶段(可从中获取执行结果)
    private CompletableFuture<T> uniWhenCompleteStage(Executor executor, BiConsumer<? super T, ? super Throwable> action) {
        if(action == null) {
            throw new NullPointerException();
        }
        
        // 待填充执行结果的下游阶段
        CompletableFuture<T> downFuture = newIncompleteFuture();
        
        // 如果当前阶段(作为上游)还没执行结果，则需要构造下游任务，并加入下游任务栈等待执行完
        if(result == null) {
            UniWhenComplete<T> downTask = new UniWhenComplete<>(executor, downFuture, this, action);
            unipush(downTask);
            return downFuture;
        }
        
        // 如果当前阶段(作为上游)已有执行结果，且executor为null，则可以同步执行任务
        if(executor == null) {
            downFuture.uniWhenComplete(result, action, null);
            return downFuture;
        }
        
        // 如果当前阶段(作为上游)已有执行结果，但是executor不为null，则需要异步执行任务
        try {
            UniWhenComplete<T> downTask = new UniWhenComplete<>(null, downFuture, this, action);
            executor.execute(downTask);
        } catch(Throwable ex) {
            downFuture.result = encodeThrowable(ex);
        }
        
        return downFuture;
    }
    
    /** Recursively constructs a tree of completions. */
    // 等待futures内所有任务执行完之后返回
    static CompletableFuture<Void> andTree(CompletableFuture<?>[] futures, int low, int high) {
        CompletableFuture<Void> downFuture = new CompletableFuture<>();
        
        // empty
        if(low>high) {
            downFuture.result = NIL;
            return downFuture;
        }
        
        CompletableFuture<?> left, right;
        
        int mid = (low + high) >>> 1;
        
        if(low == mid) {
            left = futures[low];
        } else {
            left = andTree(futures, low, mid);
        }
        
        if(left == null) {
            throw new NullPointerException();
        }
        
        if(low == high) {
            right = left;
        } else {
            if(high == mid + 1) {
                right = futures[high];
            } else {
                right = andTree(futures, mid + 1, high);
            }
        }
        
        if(right == null) {
            throw new NullPointerException();
        }
        
        // 两端的任务至少有一端没有执行完，加入下游任务栈
        if(left.result == null || right.result == null) {
            BiRelay<?, ?> downTask = new BiRelay<>(downFuture, left, right);
            left.bipush(right, downTask);
            return downFuture;
        }
        
        // 左边的任务出现了异常，返回"异常"结果
        if((left.result instanceof AltResult && ((AltResult) left.result).ex != null)) {
            downFuture.result = encodeThrowable(((AltResult) left.result).ex, left.result);
            return downFuture;
        }
        
        // 右边的任务出现了异常，返回"异常"结果
        if((right.result instanceof AltResult && ((AltResult) right.result).ex != null)) {
            downFuture.result = encodeThrowable(((AltResult) right.result).ex, right.result);
            return downFuture;
        }
        
        // 没有出现异常，返回"空"结果
        downFuture.result = NIL;
        
        return downFuture;
    }
    
    
    // 在上游阶段的任务已有执行结果时，立即执行下游阶段的UniApply型任务
    private <V> CompletableFuture<V> uniApplyNow(Object upResult, Executor executor, Function<? super T, ? extends V> action) {
        CompletableFuture<V> downFuture = newIncompleteFuture();
        
        // 如果上游阶段的任务执行结果是null或异常(被包装到了AltResult中)
        if(upResult instanceof AltResult) {
            Throwable ex = ((AltResult) upResult).ex;
            
            // 如果上游阶段产生的是异常
            if(ex != null) {
                // 将upResult设置为当前阶段的执行结果，并结束当前任务
                downFuture.result = encodeThrowable(ex, upResult);
                return downFuture;
            }
            
            // 至此，说明上游阶段的结果是null(NIL)
            upResult = null;
        }
        
        try {
            // 需要异步执行当前阶段的任务，会辗转调用到UniApply#tryFire(ASYNC)
            if(executor != null) {
                UniApply<T, V> downTask = new UniApply<>(null, downFuture, this, action);
                
                // 执行下游任务
                executor.execute(downTask);
                
                // 需要同步执行当前阶段的任务，任务源的入参是上游任务的执行结果
            } else {
                V result = action.apply((T) upResult);
                
                downFuture.result = downFuture.encodeValue(result);
            }
        } catch(Throwable ex) {
            downFuture.result = encodeThrowable(ex);
        }
        
        return downFuture;
    }
    
    // 在上游阶段的任务已有执行结果时，立即执行下游阶段的UniAccept型任务
    private CompletableFuture<Void> uniAcceptNow(Object upResult, Executor executor, Consumer<? super T> action) {
        CompletableFuture<Void> downFuture = newIncompleteFuture();
        
        // 如果上游阶段的任务执行结果是null或异常(被包装到了AltResult中)
        if(upResult instanceof AltResult) {
            Throwable ex = ((AltResult) upResult).ex;
            
            // 如果上游阶段产生的是异常
            if(ex != null) {
                // 将upResult设置为当前阶段的执行结果，并结束当前任务
                downFuture.result = encodeThrowable(ex, upResult);
                return downFuture;
            }
            
            // 至此，说明上游阶段的结果是null(NIL)
            upResult = null;
        }
        
        try {
            // 需要异步执行当前阶段的任务，会辗转调用到UniApply#tryFire(ASYNC)
            if(executor != null) {
                UniAccept<T> downTask = new UniAccept<>(null, downFuture, this, action);
                
                // 执行下游任务
                executor.execute(downTask);
                
                // 需要同步执行当前阶段的任务，任务源的入参是上游任务的执行结果
            } else {
                action.accept((T) upResult);
                
                downFuture.result = NIL;
            }
        } catch(Throwable ex) {
            downFuture.result = encodeThrowable(ex);
        }
        
        return downFuture;
    }
    
    // 在上游阶段的任务已有执行结果时，立即执行下游阶段的UniRun型任务
    private CompletableFuture<Void> uniRunNow(Object upResult, Executor executor, Runnable action) {
        CompletableFuture<Void> downFuture = newIncompleteFuture();
        
        // 如果上游阶段的任务执行结果是null或异常(被包装到了AltResult中)
        if(upResult instanceof AltResult) {
            Throwable ex = ((AltResult) upResult).ex;
            
            // 如果上游阶段产生的是异常
            if(ex != null) {
                // 将upResult设置为当前阶段的执行结果，并结束当前任务
                downFuture.result = encodeThrowable(ex, upResult);
            }
            
            return downFuture;
        }
        
        try {
            // 需要异步执行当前阶段的任务，会辗转调用到UniApply#tryFire(ASYNC)
            if(executor != null) {
                UniRun<T> downTask = new UniRun<>(null, downFuture, this, action);
                
                // 执行下游任务
                executor.execute(downTask);
                
                // 需要同步执行当前阶段的任务
            } else {
                action.run();
                
                downFuture.result = NIL;
            }
        } catch(Throwable ex) {
            downFuture.result = encodeThrowable(ex);
        }
        
        return downFuture;
    }
    
    // 在上游阶段的任务已有执行结果时，立即执行下游阶段的BiApply型任务
    final <R, S> boolean biApply(Object upResult1, Object upResult2, BiFunction<? super R, ? super S, ? extends T> action, BiApply<R, S, T> task) {
        // 如果当前阶段的任务已有执行结果
        if(result != null) {
            return true;
        }
        
        // 如果上游阶段一的任务执行结果是null或异常(被包装到了AltResult中)
        if(upResult1 instanceof AltResult) {
            Throwable ex = ((AltResult) upResult1).ex;
            
            // 如果上游阶段一产生的是异常
            if(ex != null) {
                // 将upResult1设置为当前阶段的执行结果，并结束当前任务
                completeThrowable(ex, upResult1);
                return true;
            }
            
            // 至此，说明上游阶段一的结果是null(NIL)
            upResult1 = null;
        }
        
        // 如果上游阶段二的任务执行结果是null或异常(被包装到了AltResult中)
        if(upResult2 instanceof AltResult) {
            Throwable ex = ((AltResult) upResult2).ex;
            
            // 如果上游阶段二产生的是异常
            if(ex != null) {
                // 将upResult2设置为当前阶段的执行结果，并结束当前任务
                completeThrowable(ex, upResult2);
                return true;
            }
            
            // 至此，说明上游阶段二的结果是null(NIL)
            upResult2 = null;
        }
        
        try {
            if(task != null && !task.claim()) {
                return false;
            }
            
            T result = action.apply((R) upResult1, (S) upResult2);
            
            completeValue(result);
        } catch(Throwable ex) {
            completeThrowable(ex);
        }
        
        return true;
    }
    
    // 在上游阶段的任务已有执行结果时，立即执行下游阶段的BiAccept型任务
    final <R, S> boolean biAccept(Object upResult1, Object upResult2, BiConsumer<? super R, ? super S> action, BiAccept<R, S> task) {
        // 当前阶段已有执行结果
        if(this.result != null) {
            return true;
        }
        
        // 如果上游阶段一的任务执行结果是null或异常(被包装到了AltResult中)
        if(upResult1 instanceof AltResult) {
            Throwable ex = ((AltResult) upResult1).ex;
            
            // 如果上游阶段一产生的是异常
            if(ex != null) {
                // 将upResult1设置为当前阶段的执行结果，并结束当前任务
                completeThrowable(ex, upResult1);
                return true;
            }
            
            // 至此，说明上游阶段一的结果是null(NIL)
            upResult1 = null;
        }
        
        // 如果上游阶段二的任务执行结果是null或异常(被包装到了AltResult中)
        if(upResult2 instanceof AltResult) {
            Throwable ex = ((AltResult) upResult2).ex;
            
            // 如果上游阶段二产生的是异常
            if(ex != null) {
                // 将upResult2设置为当前阶段的执行结果，并结束当前任务
                completeThrowable(ex, upResult2);
                return true;
            }
            
            // 至此，说明上游阶段二的结果是null(NIL)
            upResult2 = null;
        }
        
        try {
            if(task != null && !task.claim()) {
                return false;
            }
            
            action.accept((R) upResult1, (S) upResult2);
            
            // 设置任务结果为NIL(代表无返回值)
            completeNull();
        } catch(Throwable ex) {
            completeThrowable(ex);
        }
        
        return true;
    }
    
    // 在上游阶段的任务已有执行结果时，立即执行下游阶段的BiRun型任务
    final boolean biRun(Object upResult1, Object upResult2, Runnable action, BiRun<?, ?> task) {
        if(this.result != null) {
            return true;
        }
        
        // 如果上游阶段一的任务执行结果是null或异常(被包装到了AltResult中)
        if(upResult1 instanceof AltResult) {
            Throwable ex = ((AltResult) upResult1).ex;
            
            // 如果上游阶段一产生的是异常
            if(ex != null) {
                // 将upResult1设置为当前阶段的执行结果，并结束当前任务
                completeThrowable(ex, upResult1);
            }
            
            return true;
        }
        
        // 如果上游阶段二的任务执行结果是null或异常(被包装到了AltResult中)
        if(upResult2 instanceof AltResult) {
            Throwable ex = ((AltResult) upResult2).ex;
            
            // 如果上游阶段一产生的是异常
            if(ex != null) {
                completeThrowable(ex, upResult2);
            }
            
            // 将upResult1设置为当前阶段的执行结果，并结束当前任务
            return true;
        }
        
        try {
            if(task != null && !task.claim()) {
                return false;
            }
            
            action.run();
            
            completeNull();
        } catch(Throwable ex) {
            completeThrowable(ex);
        }
        
        return true;
    }
    
    // 在上游阶段的任务已有执行结果，且executor为null，可以同步执行UniHandle型任务
    final <R> boolean uniHandle(Object upResult, BiFunction<? super R, Throwable, ? extends T> action, UniHandle<R, T> task) {
        R r;
        
        if(this.result != null) {
            return true;
        }
        
        try {
            if(task != null && !task.claim()) {
                return false;
            }
            
            Throwable ex;
            
            // 如果上游阶段的任务执行结果是null或异常(被包装到了AltResult中)
            if(upResult instanceof AltResult) {
                ex = ((AltResult) upResult).ex;
                r = null;
            } else {
                ex = null;
                r = (R) upResult;
            }
            
            completeValue(action.apply(r, ex));
        } catch(Throwable ex) {
            completeThrowable(ex);
        }
        
        return true;
    }
    
    // 在上游阶段的任务已有执行结果，且executor为null，可以同步执行UniWhenComplete型任务
    final boolean uniWhenComplete(Object upResult, BiConsumer<? super T, ? super Throwable> action, UniWhenComplete<T> task) {
        
        if(this.result != null) {
            return true;
        }
        
        Throwable ex = null;
        
        try {
            if(task != null && !task.claim()) {
                return false;
            }
            
            T r;
            
            if(upResult instanceof AltResult) {
                ex = ((AltResult) upResult).ex;
                r = null;
            } else {
                r = (T) upResult;
            }
            
            action.accept(r, ex);
            
            if(ex == null) {
                internalComplete(upResult);
                return true;
            }
        } catch(Throwable e) {
            if(ex == null) {
                ex = e;
            } else if(ex != e) {
                ex.addSuppressed(e);
            }
        }
        
        completeThrowable(ex, upResult);
        
        return true;
    }
    
    /**
     * Pushes the given completion unless it completes while trying.
     * Caller should first check that result is null.
     */
    // 尝试将下游任务task送入下游任务栈，如果(作为上游的)当前阶段任务已有执行结果，则同步执行指定任务
    final void unipush(Completion task) {
        if(task == null) {
            return;
        }
        
        // 尝试将task加入下游任务栈，如果入栈失败，则需要重试
        while(!tryPushStack(task)) {
            /*
             * 如果当前阶段(作为上游)的任务已经执行完毕，
             * 则不必再努力将下游的task入栈，而是在后续直接处理它
             */
            if(result != null) {
                NEXT.set(task, null);
                break;
            }
        }
        
        /*
         * 如果当前阶段(作为上游)的任务已经执行完毕，
         * 则同步处理该下游任务(直接触发)。
         */
        if(result != null) {
            // 直接处理(作为下游的)指定任务(同步执行)
            task.tryFire(SYNC);
        }
    }
    
    /**
     * Pushes completion to this and b unless both done.
     * Caller should first check that either result or b.result is null.
     */
    // 如果两个上游阶段(当前阶段与upFuture2阶段)至少有一个阶段未完成，则尝试将task加入相应的下游任务栈
    final void bipush(CompletableFuture<?> upFuture2, BiCompletion<?, ?, ?> task) {
        if(task == null) {
            return;
        }
        
        // 如果当前阶段(上游阶段一)任务还没有执行结果
        while(result == null) {
            
            // 尝试将task加入下游任务栈(头插法)，入栈失败则返回false
            if(!tryPushStack(task)) {
                continue;
            }
            
            // 如果上游阶段二还没有执行结果
            if(upFuture2.result == null) {
                upFuture2.unipush(new CoCompletion(task));
                return;
            }
            
            // 如果当前阶段已经有执行结果
            if(result != null) {
                // 直接处理下游任务，以执行下游任务
                task.tryFire(SYNC);
            }
            
            return;
        }
        
        /*
         * 如果当前阶段(上游阶段一)任务已经有执行结果了
         * 尝试将下游任务task送入upFuture2的下游任务栈，
         * 如果(作为上游的)upFuture2阶段任务已有执行结果，则转为同步执行指定任务
         */
        upFuture2.unipush(task);
    }
    
    /**
     * Pushes completion to this and b unless either done.
     * Caller should first check that result and b.result are both null.
     */
    // 如果两个上游阶段(当前阶段与upFuture2阶段)都没有完成时，则尝试将task加入相应的下游任务栈
    final void orpush(CompletableFuture<?> upFuture2, BiCompletion<?, ?, ?> task) {
        if(task == null) {
            return;
        }
        
        while(!tryPushStack(task)) {
            if(result != null) {
                NEXT.set(task, null);
                break;
            }
        }
        
        if(result != null) {
            task.tryFire(SYNC);
        } else {
            upFuture2.unipush(new CoCompletion(task));
        }
    }
    
    /*▲ 执行任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置任务结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Completes with the null value, unless already completed. */
    // 设置任务结果为NIL(代表无返回值)
    final boolean completeNull() {
        return RESULT.compareAndSet(this, null, NIL);
    }
    
    /** Completes with a non-exceptional result, unless already completed. */
    // 设置任务结果为result(有返回值)
    final boolean completeValue(T result) {
        return RESULT.compareAndSet(this, null, encodeValue(result));
    }
    
    /** Completes with an exceptional result, unless already completed. */
    // 设置任务结果为异常(抛异常)
    final boolean completeThrowable(Throwable ex) {
        return RESULT.compareAndSet(this, null, encodeThrowable(ex));
    }
    
    /**
     * Completes with the given (non-null) exceptional result as a
     * wrapped CompletionException unless it is one already, unless
     * already completed.  May complete with the given Object r
     * (which must have been the result of a source future) if it is
     * equivalent, i.e. if this is a simple propagation of an
     * existing CompletionException.
     */
    // 设置任务结果为异常(抛异常)，参数result是备用信息
    final boolean completeThrowable(Throwable ex, Object result) {
        return RESULT.compareAndSet(this, null, encodeThrowable(ex, result));
    }
    
    /**
     * Completes with r or a copy of r, unless already completed.
     * If exceptional, r is first coerced to a CompletionException.
     */
    // 设置任务结果(响应信息，可能是返回值，也可能是异常)
    final boolean completeRelay(Object result) {
        return RESULT.compareAndSet(this, null, encodeRelay(result));
    }
    
    
    /** Returns the encoding of the given non-exceptional value. */
    // 编码返回值：如果result为null，则返回特殊值NIL；否则，原样返回
    final Object encodeValue(T result) {
        return (result == null) ? NIL : result;
    }
    
    /**
     * Returns the encoding of the given (non-null) exception as a wrapped CompletionException unless it is one already.
     */
    /*
     * 编码异常
     * 尝试将ex包装到CompletionException，再包装到AltResult后返回。
     * 如果ex已经是CompletionException类型，则直接将ex包装到AltResult中返回。
     */
    static AltResult encodeThrowable(Throwable ex) {
        Throwable throwable = (ex instanceof CompletionException) ? ex : new CompletionException(ex);
        
        // 将给定的异常编码为CompletionException类型，然后存入AltResult
        return new AltResult(throwable);
    }
    
    /**
     * Returns the encoding of the given (non-null) exception as a
     * wrapped CompletionException unless it is one already.  May
     * return the given Object r (which must have been the result of a
     * source future) if it is equivalent, i.e. if this is a simple
     * relay of an existing CompletionException.
     */
    /*
     * 编码异常
     * 尝试将ex包装到CompletionException，再包装到AltResult后返回。
     * 如果result已经是包装了ex的AltResult对象，则直接返回result。
     */
    static Object encodeThrowable(Throwable ex, Object result) {
        /* 将给定的异常编码为CompletionException类型，然后存入AltResult。参数r是备用信息 */
        
        if(!(ex instanceof CompletionException)) {
            ex = new CompletionException(ex);
            return new AltResult(ex);
        }
        
        if(result instanceof AltResult && ex == ((AltResult) result).ex) {
            return result;
        }
        
        return new AltResult(ex);
    }
    
    /**
     * Returns the encoding of a copied outcome; if exceptional,
     * rewraps as a CompletionException, else returns argument.
     */
    /*
     * 编码响应信息(返回值/异常)
     *
     * 如果result是AltResult类型的对象，但result中的ex域不是CompletionException类型，则尝试将result的ex域包装为CompletionException类型。
     * 否则，直接返回result对象。
     */
    static Object encodeRelay(Object result) {
        Throwable ex;
        
        if(result instanceof AltResult && (ex = ((AltResult) result).ex) != null && !(ex instanceof CompletionException)) {
            result = new AltResult(new CompletionException(ex));
        }
        
        return result;
    }
    
    /**
     * Returns the encoding of the given arguments: if the exception
     * is non-null, encodes as AltResult.  Otherwise uses the given
     * value, boxed as NIL if null.
     */
    // 编码返回值/异常
    Object encodeOutcome(T result, Throwable ex) {
        return (ex != null) ? encodeThrowable(ex) : encodeValue(result);
    }
    
    
    /**
     * Reports result using Future.get conventions.
     */
    // 解码任务结果
    private static Object reportGet(Object result) throws InterruptedException, ExecutionException {
        if(result == null) {
            // by convention below, null means interrupted
            throw new InterruptedException();
        }
        
        if(!(result instanceof AltResult)) {
            return result;
        }
        
        Throwable ex = ((AltResult) result).ex;
        
        if(ex == null) {
            return null;
        }
        
        if(ex instanceof CancellationException) {
            throw (CancellationException) ex;
        }
        
        if(ex instanceof CompletionException) {
            Throwable cause = ex.getCause();
            if(cause != null) {
                ex = cause;
            }
        }
        
        throw new ExecutionException(ex);
    }
    
    /**
     * Decodes outcome to return result or throw unchecked exception.
     */
    // 解码任务结果
    private static Object reportJoin(Object result) {
        if(!(result instanceof AltResult)) {
            return result;
        }
        
        Throwable ex = ((AltResult) result).ex;
        
        if(ex == null) {
            return null;
        }
        
        if(ex instanceof CancellationException) {
            throw (CancellationException) ex;
        }
        
        if(ex instanceof CompletionException) {
            throw (CompletionException) ex;
        }
        
        throw new CompletionException(ex);
    }
    
    /*▲ 设置任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 下游任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Returns true if successfully pushed c onto stack. */
    // 尝试将task加入下游任务栈(头插法)，入栈失败则返回false
    final boolean tryPushStack(Completion task) {
        Completion top = stack;
        NEXT.set(task, top);         // CAS piggyback
        return STACK.compareAndSet(this, top, task);
    }
    
    /** Unconditionally pushes c onto stack, retrying if necessary. */
    // 尝试将task加入下游任务栈(头插法)，不成功不返回
    final void pushStack(Completion task) {
        do {
        } while(!tryPushStack(task));
    }
    
    /** Traverses stack and unlinks one or more dead Completions, if found. */
    // 清理下游任务栈中首个已被处理过的下游任务
    final void cleanStack() {
        Completion p = stack;
        
        boolean unlinked = false;
        
        // 如果栈顶下游任务已被处理，则清理它
        while(true) {
            // 如果下游任务栈已经为空
            if(p == null) {
                return;
            }
            
            // 如果下游任务等待处理
            if(p.isLive()) {
                if(unlinked) {
                    return;
                }
                
                break;
                
                // 将已处理过的下游任务出栈
            } else if(STACK.weakCompareAndSet(this, p, (p = p.next))) {
                unlinked = true;
            } else {
                p = stack;
            }
        }
        
        // 清理栈中首个已处理过的下游任务
        for(Completion q = p.next; q != null; ) {
            Completion s = q.next;
            if(q.isLive()) {
                p = q;
                q = s;
            } else if(NEXT.weakCompareAndSet(p, q, s)) {
                break;
            } else {
                q = p.next;
            }
        }
    }
    
    /**
     * Pops and tries to trigger all reachable dependents.
     * Call only when known to be done.
     */
    /*
     * (递归)处理当前阶段的下游任务栈(会触发tryFire(NESTED))
     *
     * 该处理过程是递归进行的，其处理顺序类似于图的深度优先遍历，
     * 而且，在深优遍历中，如果遇到多个拥有相同上游的任务，
     * 则会先遍历那些较早加入下游任务栈的任务。
     */
    final void postComplete() {
        /*
         * On each step, variable f holds current dependents to pop and run.
         * It is extended along only one path at a time, pushing others to avoid unbounded recursion.
         */
        
        CompletableFuture<?> future = this;
        
        while(true) {
            // 获取下游任务栈中首个元素
            Completion head = future.stack;
            
            // 如果下游任务栈为空
            if(head == null) {
                // 如果stage未指向当前阶段，则进行回退
                if(future != this) {
                    future = this;
                    head = future.stack;
                    
                    /*
                     * 如果当前阶段的下游任务栈已经为null，
                     * 说明所有下游阶段都被遍历完了，可以返回了
                     */
                    if(head == null) {
                        return;
                    }
                } else {
                    // 当前下游任务栈为null，所有下游阶段都被遍历完
                    return;
                }
            }
            
            // 获取下游任务栈中下一个元素
            Completion next = head.next;
            
            // 更新栈顶游标指向下一个下游任务
            if(STACK.compareAndSet(future, head, next)) {
                
                // 如果栈中仍有元素
                if(next != null) {
                    /*
                     * 如果stage未指向当前阶段，
                     * 将下游中较晚加入的任务“嫁接”到当前阶段的下游任务栈中，
                     * 以便后续回退时进行访问。
                     */
                    if(future != this) {
                        // 尝试将head任务加入当前下游任务栈(头插法)，不成功不返回
                        pushStack(head);
                        continue;
                    }
                    
                    // 将栈顶元素与下游任务栈分离
                    NEXT.compareAndSet(head, next, null); // try to detach
                }
                
                // 处理head元素，返回该任务所处阶段
                CompletableFuture<?> downFuture = head.tryFire(NESTED);
                
                /*
                 * downFuture不为null，意味着head中仍有待遍历的下游任务，
                 * 则切换stage为downFuture，以便继续递归遍历
                 */
                future = (downFuture == null) ? this : downFuture;
            }
        }
    }
    
    /**
     * Post-processing by dependent after successful UniCompletion tryFire.
     * Tries to clean stack of source a, and then either runs postComplete
     * or returns this to caller, depending on mode.
     */
    // 下游阶段的任务执行完之后，先检查上游阶段的下游任务栈，再检查当前阶段的下游任务栈
    final CompletableFuture<T> postFire(CompletableFuture<?> upFuture, int mode) {
        // 如果上游阶段包含下游任务
        if(upFuture != null && upFuture.stack != null) {
            // 获取上游阶段任务的执行结果
            Object upResult = upFuture.result;
            
            // 如果上游阶段任务还没有执行结果
            if(upResult == null) {
                upFuture.cleanStack();
            }
            
            // 处理模式是SYNC或ASYNC
            if(mode >= 0 && (upResult != null || upFuture.result != null)) {
                // (递归)处理上游阶段的下游任务栈(会触发tryFire(NESTED))
                upFuture.postComplete();
            }
        }
        
        // 如果当前(下游)阶段已有任务结果，且当前(下游)阶段的下游任务栈不为空
        if(result != null && stack != null) {
            // NESTED模式
            if(mode<0) {
                return this;
            }
            
            // (递归)处理当前(下游)阶段的下游任务栈(会触发tryFire(NESTED))
            postComplete();
        }
        
        return null;
    }
    
    /** Post-processing after successful BiCompletion tryFire. */
    final CompletableFuture<T> postFire(CompletableFuture<?> upFuture1, CompletableFuture<?> upFuture2, int mode) {
        // 如果upFuture2阶段包含下游任务
        if(upFuture2 != null && upFuture2.stack != null) { // clean second source
            // 获取upFuture2阶段任务的执行结果
            Object upResult2 = upFuture2.result;
            
            // 如果upFuture2阶段任务还没有执行结果
            if(upResult2 == null) {
                upFuture2.cleanStack();
            }
            
            if(mode >= 0 && (upResult2 != null || upFuture2.result != null)) {
                // (递归)处理upFuture2阶段的下游任务栈(会触发tryFire(NESTED))
                upFuture2.postComplete();
            }
        }
        
        return postFire(upFuture1, mode);
    }
    
    /*▲ 下游任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns a string identifying this CompletableFuture, as well as
     * its completion state.  The state, in brackets, contains the
     * String {@code "Completed Normally"} or the String {@code
     * "Completed Exceptionally"}, or the String {@code "Not
     * completed"} followed by the number of CompletableFutures
     * dependent upon its completion, if any.
     *
     * @return a string identifying this CompletableFuture, as well as its state
     */
    public String toString() {
        int count = 0; // avoid call to getNumberOfDependents in case disabled
        
        for(Completion p = stack; p != null; p = p.next) {
            ++count;
        }
        
        return super.toString()
            + ((result == null)
            ? ((count == 0)
            ? "[Not completed]"
            : "[Not completed, " + count + " dependents]") : (((result instanceof AltResult) && ((AltResult) result).ex != null) ? "[Completed exceptionally: " + ((AltResult) result).ex + "]" : "[Completed normally]"));
    }
    
    
    /**
     * A marker interface identifying asynchronous tasks produced by
     * {@code async} methods. This may be useful for monitoring,
     * debugging, and tracking asynchronous activities.
     *
     * @since 1.8
     */
    // 异步任务接口
    public interface AsynchronousCompletionTask {
    }
    
    // 支持异步执行的任务
    @SuppressWarnings("serial")
    abstract static class Completion extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        // 链接到下一个拥有相同上游的待执行任务
        volatile Completion next;
        
        // 异步执行当前任务
        public final boolean exec() {
            tryFire(ASYNC);
            return false;
        }
        
        // 异步执行当前任务
        public final void run() {
            tryFire(ASYNC);
        }
        
        public final Void getRawResult() {
            return null;
        }
        
        public final void setRawResult(Void v) {
        }
        
        /**
         * Performs completion action if triggered, returning a dependent that may need propagation, if one exists.
         *
         * @param mode SYNC, ASYNC, or NESTED
         */
        // 执行任务(下游任务类型)
        abstract CompletableFuture<?> tryFire(int mode);
        
        /** Returns true if possibly still triggerable. Used by cleanStack. */
        // 下游任务是否正在等待处理
        abstract boolean isLive();
    }
    
    /** A Completion with a source, dependent, and executor. */
    // 包含单个依赖的任务
    @SuppressWarnings("serial")
    abstract static class UniCompletion<T, V> extends Completion {
        Executor executor;               // 用于执行当前任务的【任务执行器】
        CompletableFuture<V> future;     // 当前阶段，待填充任务执行结果
        CompletableFuture<T> upFuture1;  // 一个上游阶段
        
        UniCompletion(Executor executor, CompletableFuture<V> future, CompletableFuture<T> upFuture1) {
            this.executor = executor;
            this.future = future;
            this.upFuture1 = upFuture1;
        }
        
        /**
         * Returns true if action can be run.
         * Call only when known to be triggerable.
         * Uses FJ tag bit to ensure that only one thread claims ownership.
         * If async, starts as task -- a later call to tryFire will run action.
         */
        /*
         * 尝试异步执行当前任务，如果成功，则会辗转调用到tryFire(ASYNC)。
         * 返回值指示当前(上游)线程是否需要继续处理当前(下游)任务。
         *
         * 如果当前线程成功锁定任务，但该任务未指定【任务执行器】，则返回true，表示后续需要同步执行该任务。
         *
         * 如果当前线程未成功锁定任务，或者，该任务指定了【任务执行器】，则返回false，
         * 此时，表示该任务已被异步执行(可能是其它的上游线程抢到执行权，也可能是被当前任务自己的【任务执行器】执行)，
         * 那么当前遍历会被中断，此分支后续不再需要处理该任务。
         */
        final boolean claim() {
            Executor executor = this.executor;
            
            // 锁定当前任务，一个任务只能有一个Executor拥有
            if(compareAndSetForkJoinTaskTag((short) 0, (short) 1)) {
                if(executor == null) {
                    return true;
                }
                
                this.executor = null; // disable
                
                // 用指定的【任务执行器】在异步模式下执行
                executor.execute(this);
            }
            
            return false;
        }
        
        final boolean isLive() {
            return this.future != null;
        }
    }
    
    /** A Completion for an action with two sources */
    // 包含两个依赖的任务
    @SuppressWarnings("serial")
    abstract static class BiCompletion<T, U, V> extends UniCompletion<T, V> {
        CompletableFuture<U> upFuture2;  // 另一个上游阶段
        
        BiCompletion(Executor executor, CompletableFuture<V> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2) {
            super(executor, future, upFuture1);
            this.upFuture2 = upFuture2;
        }
    }
    
    //【AsyncSupply】任务：(不可做下游任务)异步执行【Supplier】任务：该任务【有】返回值，【无需】上游的执行结果做入参，且该任务【有】返回值
    @SuppressWarnings("serial")
    static final class AsyncSupply<T> extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<T> future;  // 当前任务所属阶段
        Supplier<? extends T> action;  // 待执行动作
        
        AsyncSupply(CompletableFuture<T> future, Supplier<? extends T> action) {
            this.future = future;
            this.action = action;
        }
        
        // 执行任务。返回值指示任务是否正常完成
        public final boolean exec() {
            run();
            return false;
        }
        
        // 任务执行逻辑
        public void run() {
            CompletableFuture<T> future = this.future;
            Supplier<? extends T> action = this.action;
            
            if(future == null || action == null) {
                return;
            }
            
            this.future = null;
            this.action = null;
            
            // 如果当前阶段已有执行结果，直接返回
            if(future.result != null) {
                return;
            }
            
            try {
                // 执行任务
                T result = action.get();
                
                // 设置任务结果为result(有返回值)
                future.completeValue(result);
            } catch(Throwable ex) {
                // 设置异常结果
                future.completeThrowable(ex);
            }
            
            // (递归)处理stage阶段的下游任务栈(会触发tryFire(NESTED))
            future.postComplete();
        }
        
        public final Void getRawResult() {
            return null;
        }
        
        public final void setRawResult(Void v) {
        }
    }
    
    //【AsyncRun】任务：(不可做下游任务)异步执行【Runnable】任务：该任务【无】返回值，【无需】上游的执行结果做入参，且该任务【无】返回值
    @SuppressWarnings("serial")
    static final class AsyncRun extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<Void> future;   // 当前任务所属阶段
        Runnable action;                  // 任务源
        
        AsyncRun(CompletableFuture<Void> future, Runnable action) {
            this.future = future;
            this.action = action;
        }
        
        // 执行任务
        public final boolean exec() {
            run();
            return false;
        }
        
        // 任务执行逻辑
        public void run() {
            CompletableFuture<Void> future = this.future;
            Runnable action = this.action;
            
            if(future == null || action == null) {
                return;
            }
            
            this.future = null;
            this.action = null;
            
            // 如果已有执行结果，直接返回
            if(future.result != null) {
                return;
            }
            
            try {
                // 执行任务源
                action.run();
                
                // 设置任务结果为NIL(代表无返回值)
                future.completeNull();
            } catch(Throwable ex) {
                // 设置异常结果
                future.completeThrowable(ex);
            }
            
            // (递归)处理stage阶段的下游任务栈(会触发tryFire(NESTED))
            future.postComplete();
        }
        
        public final Void getRawResult() {
            return null;
        }
        
        public final void setRawResult(Void v) {
        }
    }
    
    //【UniApply】任务：(可以做下游任务)同步/异步执行【Function】任务：该任务【有】返回值，且【需要】等待【一个】上游任务执行完，并使用该上游的执行结果做该任务的入参；如果上游发生异常，则提前返回
    @SuppressWarnings("serial")
    static final class UniApply<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends V> action;    // 任务源
        
        UniApply(Executor executor, CompletableFuture<V> future, CompletableFuture<T> upFuture1, Function<? super T, ? extends V> action) {
            super(executor, future, upFuture1);
            this.action = action;
        }
        
        // 执行当前任务，如果上游的执行结果是异常，则结束当前任务
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> future = this.future;     // 当前任务所属阶段
            CompletableFuture<T> upFuture1 = this.upFuture1;  // 上游阶段
            Object upResult1;                                // 上游阶段的任务执行结果
            
            if(future == null || this.action == null) {
                return null;
            }
            
            // 如果上游阶段还没有执行结果
            if(upFuture1 == null || (upResult1 = upFuture1.result) == null) {
                return null;
            }
            
            // 如果当前阶段还没有执行结果，说明任务还没完成
            if(future.result != null) {
                return tryComplete(future, upFuture1, mode);
            }
            
            // 如果上游阶段的任务执行结果是null或异常(被包装到了AltResult中)
            if(upResult1 instanceof AltResult) {
                Throwable ex = ((AltResult) upResult1).ex;
                
                // 如果上游阶段产生的是异常
                if(ex != null) {
                    // 将upResult设置为当前阶段的执行结果，并结束当前任务
                    future.completeThrowable(ex, upResult1);
                    
                    return tryComplete(future, upFuture1, mode);
                }
                
                // 至此，说明上游阶段的结果是null(NIL)
                upResult1 = null;
            }
            
            try {
                // 处理模式是NESTED或SYNC：当前(下游)任务位于上游线程中
                if(mode<=0) {
                    /*
                     * 尝试异步执行当前任务，如果成功，则会辗转调用到tryFire(ASYNC)。
                     * 返回值指示当前(上游)线程是否需要继续处理当前(下游)任务
                     */
                    if(!claim()) {
                        // 如果当前任务被别的线程抢走执行，或者成功了调用了异步执行逻辑，则直接返回
                        return null;
                    }
                }
                
                // 触发当前任务源，入参是上游的执行结果
                @SuppressWarnings("unchecked")
                V result = this.action.apply((T) upResult1);
                
                // 为当前阶段任务设置执行结果
                future.completeValue(result);
            } catch(Throwable ex) {
                // 设置异常结果
                future.completeThrowable(ex);
            }
            
            return tryComplete(future, upFuture1, mode);
        }
        
        private CompletableFuture<V> tryComplete(CompletableFuture<V> future, CompletableFuture<T> upFuture1, int mode) {
            this.future = null;
            this.upFuture1 = null;
            this.action = null;
            
            // 下游阶段的任务执行完之后，先检查上游阶段的下游任务栈，再检查当前阶段的下游任务栈
            return future.postFire(upFuture1, mode);
        }
    }
    
    //【UniAccept】任务：(可以做下游任务)同步/异步执行【Consumer】任务：该任务【无】返回值，且【需要】等待【一个】上游任务执行完，并使用该上游的执行结果做该任务的入参；如果上游发生异常，则提前返回
    @SuppressWarnings("serial")
    static final class UniAccept<T> extends UniCompletion<T, Void> {
        Consumer<? super T> action;    // 任务源
        
        UniAccept(Executor executor, CompletableFuture<Void> future, CompletableFuture<T> upFuture1, Consumer<? super T> action) {
            super(executor, future, upFuture1);
            this.action = action;
        }
        
        // 执行任务
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> future = this.future;        // 当前任务所属阶段
            CompletableFuture<T> upFuture1 = this.upFuture1;  // 上游阶段
            Object upResult1;                                   // 上游阶段的任务执行结果
            
            if(future == null || this.action == null) {
                return null;
            }
            
            // 如果上游阶段还没有执行结果
            if(upFuture1 == null || (upResult1 = upFuture1.result) == null) {
                return null;
            }
            
            // 如果当前阶段还没有执行结果，说明任务还没完成
            if(future.result != null) {
                return tryComplete(future, upFuture1, mode);
            }
            
            // 如果上游阶段已有AltResult类型结果
            if(upResult1 instanceof AltResult) {
                Throwable ex = ((AltResult) upResult1).ex;
                
                // 如果上游阶段产生的是异常
                if(ex != null) {
                    // 同样为当前阶段设置异常结果
                    future.completeThrowable(ex, upResult1);
                    return tryComplete(future, upFuture1, mode);
                }
                
                // 至此，上游阶段的结果可能是null，也可能无返回值，但确定的是，其结果包装在了AltResult中
                upResult1 = null;
            }
            
            try {
                // 处理模式是NESTED或SYNC
                if(mode<=0) {
                    /*
                     * 尝试异步执行当前任务，如果成功，则会辗转调用到tryFire(ASYNC)
                     * 返回true表示当前线程锁定该任务，但该任务需要同步执行
                     */
                    if(!claim()) {
                        // 如果当前任务被别的线程抢走执行，或者成功了调用了异步执行逻辑，则直接返回
                        return null;
                    }
                }
                
                // 触发当前任务源，入参是上游的执行结果
                action.accept((T) upResult1);
                
                // 设置任务结果为NIL(代表无返回值)
                future.completeNull();
            } catch(Throwable ex) {
                // 设置异常结果
                future.completeThrowable(ex);
            }
            
            return tryComplete(future, upFuture1, mode);
        }
        
        private CompletableFuture<Void> tryComplete(CompletableFuture<Void> future, CompletableFuture<T> upFuture1, int mode) {
            this.future = null;
            this.upFuture1 = null;
            this.action = null;
            
            // 下游阶段的任务执行完之后，先检查上游阶段的下游任务栈，再检查当前阶段的下游任务栈
            return future.postFire(upFuture1, mode);
        }
    }
    
    //【UniRun】任务：(可以做下游任务)同步/异步执行【Runnable】任务：该任务【无】返回值，且【需要】等待【一个】上游任务执行完，但该任务无需入参；如果上游发生异常，则提前返回
    @SuppressWarnings("serial")
    static final class UniRun<T> extends UniCompletion<T, Void> {
        Runnable action;    // 任务源
        
        UniRun(Executor executor, CompletableFuture<Void> future, CompletableFuture<T> upFuture1, Runnable action) {
            super(executor, future, upFuture1);
            this.action = action;
        }
        
        // 执行任务
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> future = this.future;     // 当前任务所属阶段
            CompletableFuture<T> upFuture1 = this.upFuture1;  // 上游阶段
            Object upResult;                                // 上游阶段的任务结果
            
            if(future == null || action == null) {
                return null;
            }
            
            if(upFuture1 == null || (upResult = upFuture1.result) == null) {
                return null;
            }
            
            if(future.result != null) {
                return tryComplete(future, upFuture1, mode);
            }
            
            if(upResult instanceof AltResult) {
                Throwable ex = ((AltResult) upResult).ex;
                
                if(ex != null) {
                    future.completeThrowable(ex, upResult);
                    return tryComplete(future, upFuture1, mode);
                }
            }
            
            try {
                // 处理模式是NESTED或SYNC
                if(mode<=0) {
                    if(!claim()) {
                        return null;
                    }
                }
                
                action.run();
                
                future.completeNull();
            } catch(Throwable ex) {
                future.completeThrowable(ex);
            }
            
            return tryComplete(future, upFuture1, mode);
        }
        
        private CompletableFuture<Void> tryComplete(CompletableFuture<Void> future, CompletableFuture<T> upFuture1, int mode) {
            this.future = null;
            this.upFuture1 = null;
            this.action = null;
            
            // 下游阶段的任务执行完之后，先检查上游阶段的下游任务栈，再检查当前阶段的下游任务栈
            return future.postFire(upFuture1, mode);
        }
    }
    
    //【BiApply】任务：(可以做下游任务)同步/异步执行【BiFunction】任务：该任务【有】返回值，且【需要】等待【两个】上游任务执行完，并使用该上游的[那两个]执行结果做该任务的入参；如果上游发生异常，则提前返回
    @SuppressWarnings("serial")
    static final class BiApply<T, U, V> extends BiCompletion<T, U, V> {
        BiFunction<? super T, ? super U, ? extends V> action;    // 任务源
        
        BiApply(Executor executor, CompletableFuture<V> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2, BiFunction<? super T, ? super U, ? extends V> action) {
            super(executor, future, upFuture1, upFuture2);
            this.action = action;
        }
        
        // 执行任务
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> future = this.future;        // 当前任务所属阶段
            CompletableFuture<T> upFuture1 = this.upFuture1;  // 上游阶段(其中之一)
            CompletableFuture<U> upFuture2 = this.upFuture2;  // 上游阶段(其中之一)
            
            if(action == null || future == null) {
                return null;
            }
            
            if(upFuture1 == null || upFuture1.result == null) {
                return null;
            }
            
            if(upFuture2 == null || upFuture2.result == null) {
                return null;
            }
            
            /*
             * 如果mode是NESTED或SYNC，则就在当前(上游)线程中执行当前(下游)任务，
             * 如果mode是ASYNC，则异步(使用当前任务的Executor)执行当前(下游)任务。
             */
            if(!future.biApply(upFuture1.result, upFuture2.result, action, mode>0 ? null : this)) {
                return null;
            }
            
            this.future = null;
            this.upFuture1 = null;
            this.upFuture2 = null;
            this.action = null;
            
            return future.postFire(upFuture1, upFuture2, mode);
        }
    }
    
    //【BiAccept】任务：(可以做下游任务)同步/异步执行【BiConsumer】任务：该任务【无】返回值，且【需要】等待【两个】上游任务执行完，并使用该上游的[那两个]执行结果做该任务的入参；如果上游发生异常，则提前返回
    @SuppressWarnings("serial")
    static final class BiAccept<T, U> extends BiCompletion<T, U, Void> {
        BiConsumer<? super T, ? super U> action;
        
        BiAccept(Executor executor, CompletableFuture<Void> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2, BiConsumer<? super T, ? super U> action) {
            super(executor, future, upFuture1, upFuture2);
            this.action = action;
        }
        
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> future = this.future;         // 当前任务所属阶段
            CompletableFuture<T> upFuture1 = this.upFuture1;   // 上游阶段(其中之一)
            CompletableFuture<U> upFuture2 = this.upFuture2;   // 上游阶段(其中之一)
            
            if(this.action == null || future == null) {
                return null;
            }
            
            if(upFuture1 == null || upFuture1.result == null) {
                return null;
            }
            
            if(upFuture2 == null || upFuture2.result == null) {
                return null;
            }
            
            if(!future.biAccept(upFuture1.result, upFuture2.result, this.action, mode>0 ? null : this)) {
                return null;
            }
            
            this.future = null;
            this.upFuture1 = null;
            this.upFuture2 = null;
            this.action = null;
            
            return future.postFire(upFuture1, upFuture2, mode);
        }
    }
    
    //【BiRun】任务：(可以做下游任务)同步/异步执行【Runnable】任务：该任务【无】返回值，且【需要】等待【两个】上游任务执行完，但该任务无需入参；如果上游发生异常，则提前返回
    @SuppressWarnings("serial")
    static final class BiRun<T, U> extends BiCompletion<T, U, Void> {
        Runnable action;
        
        BiRun(Executor executor, CompletableFuture<Void> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2, Runnable action) {
            super(executor, future, upFuture1, upFuture2);
            this.action = action;
        }
        
        // 执行任务
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> future = this.future;
            CompletableFuture<T> upFuture1 = this.upFuture1;
            CompletableFuture<U> upFuture2 = this.upFuture2;
            
            if(this.action == null || future == null) {
                return null;
            }
            
            if(upFuture1 == null || upFuture1.result == null) {
                return null;
            }
            
            if(upFuture2 == null || upFuture2.result == null) {
                return null;
            }
            
            if(!future.biRun(upFuture1.result, upFuture2.result, this.action, mode>0 ? null : this)) {
                return null;
            }
            
            this.future = null;
            this.upFuture1 = null;
            this.upFuture2 = null;
            this.action = null;
            
            return future.postFire(upFuture1, upFuture2, mode);
        }
    }
    
    // 【OrApply】任务：(可以做下游任务)同步/异步执行【Function】任务：该任务【有】返回值，且【需要】等待【两个】上游任务中的【任意一个】执行完，并使用该上游的[那个]执行结果做该任务的入参；如果上游发生异常，则提前返回
    @SuppressWarnings("serial")
    static final class OrApply<T, U extends T, V> extends BiCompletion<T, U, V> {
        Function<? super T, ? extends V> action;    // 任务源
        
        OrApply(Executor executor, CompletableFuture<V> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2, Function<? super T, ? extends V> action) {
            super(executor, future, upFuture1, upFuture2);
            this.action = action;
        }
        
        // 执行任务
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> future = this.future;        // 当前任务所属阶段
            CompletableFuture<T> upFuture1 = this.upFuture1;  // 上游阶段(其中之一)
            CompletableFuture<U> upFuture2 = this.upFuture2;  // 上游阶段(其中之一)
            
            Object upResult;
            
            if(action == null || future == null) {
                return null;
            }
            
            if(upFuture1 == null || upFuture2 == null) {
                return null;
            }
            
            if(((upResult = upFuture1.result) == null && (upResult = upFuture2.result) == null)) {
                return null;
            }
            
            if(future.result != null) {
                return tryComplete(future, upFuture1, upFuture2, mode);
            }
            
            try {
                if(mode<=0 && !claim()) {
                    return null;
                }
                
                if(upResult instanceof AltResult) {
                    Throwable ex = ((AltResult) upResult).ex;
                    
                    if(ex != null) {
                        future.completeThrowable(ex, upResult);
                        return tryComplete(future, upFuture1, upFuture2, mode);
                    }
                    
                    upResult = null;
                }
                
                V r = action.apply((T) upResult);
                
                future.completeValue(r);
            } catch(Throwable ex) {
                future.completeThrowable(ex);
            }
            
            return tryComplete(future, upFuture1, upFuture2, mode);
        }
        
        private CompletableFuture<V> tryComplete(CompletableFuture<V> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2, int mode) {
            this.future = null;
            this.upFuture1 = null;
            this.upFuture2 = null;
            this.action = null;
            
            return future.postFire(upFuture1, upFuture2, mode);
        }
    }
    
    // 【OrAccept】任务：(可以做下游任务)同步/异步执行【Consumer】任务：该任务【无】返回值，且【需要】等待【两个】上游任务中的【任意一个】执行完，并使用该上游的[那个]执行结果做该任务的入参；如果上游发生异常，则提前返回
    @SuppressWarnings("serial")
    static final class OrAccept<T, U extends T> extends BiCompletion<T, U, Void> {
        Consumer<? super T> action;
        
        OrAccept(Executor executor, CompletableFuture<Void> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2, Consumer<? super T> action) {
            super(executor, future, upFuture1, upFuture2);
            this.action = action;
        }
        
        // 执行任务
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> future = this.future;     // 当前任务所属阶段
            CompletableFuture<T> upFuture1 = this.upFuture1;  // 上游阶段(其中之一)
            CompletableFuture<U> upFuture2 = this.upFuture2;  // 上游阶段(其中之一)
            
            Object upResult;
            
            if(this.action == null || future == null) {
                return null;
            }
            
            if(upFuture1 == null || upFuture2 == null) {
                return null;
            }
            
            if((upResult = upFuture1.result) == null && (upResult = upFuture2.result) == null) {
                return null;
            }
            
            if(future.result != null) {
                return future.postFire(upFuture1, upFuture2, mode);
            }
            
            try {
                if(mode<=0 && !claim()) {
                    return null;
                }
                
                if(upResult instanceof AltResult) {
                    Throwable ex = ((AltResult) upResult).ex;
                    
                    if(ex != null) {
                        future.completeThrowable(ex, upResult);
                        return future.postFire(upFuture1, upFuture2, mode);
                    }
                    
                    upResult = null;
                }
                
                action.accept((T) upResult);
                
                future.completeNull();
            } catch(Throwable ex) {
                future.completeThrowable(ex);
            }
            
            return future.postFire(upFuture1, upFuture2, mode);
        }
        
        private CompletableFuture<Void> tryComplete(CompletableFuture<Void> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2, int mode) {
            this.future = null;
            this.upFuture1 = null;
            this.upFuture2 = null;
            this.action = null;
            
            return future.postFire(upFuture1, upFuture2, mode);
        }
    }
    
    // 【OrRun】任务：(可以做下游任务)同步/异步执行【Runnable】任务：该任务【无】返回值，且【需要】等待【两个】上游任务中的【任意一个】执行完，但该任务无需入参；如果上游发生异常，则提前返回
    @SuppressWarnings("serial")
    static final class OrRun<T, U> extends BiCompletion<T, U, Void> {
        Runnable action;
        
        OrRun(Executor executor, CompletableFuture<Void> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2, Runnable action) {
            super(executor, future, upFuture1, upFuture2);
            this.action = action;
        }
        
        // 执行任务
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> future = this.future;     // 当前任务所属阶段
            CompletableFuture<T> upFuture1 = this.upFuture1;  // 上游阶段(其中之一)
            CompletableFuture<U> upFuture2 = this.upFuture2;  // 上游阶段(其中之一)
            
            Object upResult;
            
            if(this.action == null || future == null) {
                return null;
            }
            
            if(upFuture1 == null || upFuture2 == null) {
                return null;
            }
            
            if((upResult = upFuture1.result) == null && (upResult = upFuture2.result) == null) {
                return null;
            }
            
            if(future.result != null) {
                return future.postFire(upFuture1, upFuture2, mode);
            }
            
            try {
                if(mode<=0 && !claim()) {
                    return null;
                }
                
                Throwable ex;
                
                if(upResult instanceof AltResult && (ex = ((AltResult) upResult).ex) != null) {
                    future.completeThrowable(ex, upResult);
                } else {
                    action.run();
                    future.completeNull();
                }
            } catch(Throwable ex) {
                future.completeThrowable(ex);
            }
            
            return future.postFire(upFuture1, upFuture2, mode);
        }
        
        private CompletableFuture<Void> tryComplete(CompletableFuture<Void> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2, int mode) {
            this.future = null;
            this.upFuture1 = null;
            this.upFuture2 = null;
            this.action = null;
            
            return future.postFire(upFuture1, upFuture2, mode);
        }
    }
    
    // 【UniCompose】任务：(可以做下游任务)同步/异步执行【Function】任务，该任务【需要】等待【一个】上游任务执行完，并使用该上游的执行结果或抛出的异常作为入参
    @SuppressWarnings("serial")
    static final class UniCompose<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends CompletionStage<V>> action;
        
        UniCompose(Executor executor, CompletableFuture<V> future, CompletableFuture<T> upFuture1, Function<? super T, ? extends CompletionStage<V>> action) {
            super(executor, future, upFuture1);
            this.action = action;
        }
        
        // 执行任务
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> future = this.future;
            CompletableFuture<T> upFuture1 = this.upFuture1;
            
            if(this.action == null || future == null) {
                return null;
            }
            
            Object upResult1;
            
            // 如果上游阶段还没有执行结果，则需要等待上游任务完成后再触发到此
            if(upFuture1 == null || (upResult1 = upFuture1.result) == null) {
                return null;
            }
            
            // 如果当前阶段已经有执行结果
            if(future.result != null) {
                return tryComplete(future, upFuture1, mode);
            }
            
            // 如果上游阶段的任务执行结果是null或异常(被包装到了AltResult中)
            if(upResult1 instanceof AltResult) {
                Throwable ex = ((AltResult) upResult1).ex;
                
                // 如果上游阶段产生的是异常
                if(ex != null) {
                    // 将upResult设置为当前阶段的执行结果，并结束当前任务
                    future.completeThrowable(ex, upResult1);
                    
                    return tryComplete(future, upFuture1, mode);
                }
                
                // 至此，说明上游阶段的结果是null(NIL)
                upResult1 = null;
            }
            
            try {
                if(mode<=0) {
                    if(!claim()) {
                        return null;
                    }
                }
                
                CompletionStage<V> stage = this.action.apply((T) upResult1);
                
                CompletableFuture<V> tmpFuture = stage.toCompletableFuture();
                
                if(tmpFuture.result != null) {
                    future.completeRelay(tmpFuture.result);
                    
                    // 注：如果tmpFuture中没有设置执行结果，后续会陷入阻塞
                } else {
                    UniRelay<V, V> downTask = new UniRelay<>(future, tmpFuture);
                    tmpFuture.unipush(downTask);
                    
                    if(future.result == null) {
                        return null;
                    }
                }
            } catch(Throwable ex) {
                future.completeThrowable(ex);
            }
            
            return tryComplete(future, upFuture1, mode);
        }
        
        private CompletableFuture<V> tryComplete(CompletableFuture<V> future, CompletableFuture<T> upFuture1, int mode) {
            this.future = null;
            this.upFuture1 = null;
            this.action = null;
            
            // 下游阶段的任务执行完之后，先检查上游阶段的下游任务栈，再检查当前阶段的下游任务栈
            return future.postFire(upFuture1, mode);
        }
    }
    
    // 【UniRelay】任务：(可以做下游任务)同步/异步执行【BiFunction】任务，该任务【需要】等待【一个】上游任务执行完，并使用该上游的执行结果和抛出的异常作为入参
    @SuppressWarnings("serial")
    static final class UniHandle<T, V> extends UniCompletion<T, V> {
        BiFunction<? super T, Throwable, ? extends V> action;
        
        UniHandle(Executor executor, CompletableFuture<V> future, CompletableFuture<T> upFuture1, BiFunction<? super T, Throwable, ? extends V> action) {
            super(executor, future, upFuture1);
            this.action = action;
        }
        
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> future = this.future;
            CompletableFuture<T> upFuture1 = this.upFuture1;
            
            if(action == null || future == null) {
                return null;
            }
            
            // 如果上游阶段还没有执行结果，则需要等待上游任务完成后再触发到此
            if(upFuture1 == null || upFuture1.result == null) {
                return null;
            }
            
            if(!future.uniHandle(upFuture1.result, action, mode>0 ? null : this)) {
                return null;
            }
            
            this.future = null;
            this.upFuture1 = null;
            this.action = null;
            
            return future.postFire(upFuture1, mode);
        }
    }
    
    // 【UniWhenComplete】任务：(可以做下游任务)同步/异步执行【BiConsumer】任务，该任务【需要】等待【一个】上游任务执行完，并使用该上游的执行结果和抛出的异常作为入参
    @SuppressWarnings("serial")
    static final class UniWhenComplete<T> extends UniCompletion<T, T> {
        BiConsumer<? super T, ? super Throwable> action;
        
        UniWhenComplete(Executor executor, CompletableFuture<T> future, CompletableFuture<T> upFuture1, BiConsumer<? super T, ? super Throwable> action) {
            super(executor, future, upFuture1);
            this.action = action;
        }
        
        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> stage = this.future;
            CompletableFuture<T> upFuture1 = this.upFuture1;
            
            if(action == null || stage == null) {
                return null;
            }
            
            // 如果上游阶段还没有执行结果，则需要等待上游任务完成后再触发到此
            if(upFuture1 == null || upFuture1.result == null) {
                return null;
            }
            
            if(!stage.uniWhenComplete(upFuture1.result, action, mode>0 ? null : this)) {
                return null;
            }
            
            this.future = null;
            this.upFuture1 = null;
            this.action = null;
            
            return stage.postFire(upFuture1, mode);
        }
    }
    
    /** Completion for an anyOf input future. */
    // 【AnyOf】任务：(不可做下游任务)依赖多个上游阶段的任务，该任务【需要】等待【多个】上游任务中的【任意一个】执行完，并存储[那个]任务的执行结果或异常信息
    @SuppressWarnings("serial")
    static class AnyOf extends Completion {
        CompletableFuture<Object> downFuture;
        CompletableFuture<?> upFuture;
        CompletableFuture<?>[] upFutures;
        
        AnyOf(CompletableFuture<Object> downFuture, CompletableFuture<?> upFuture, CompletableFuture<?>[] upFutures) {
            this.downFuture = downFuture;
            this.upFuture = upFuture;
            this.upFutures = upFutures;
        }
        
        final CompletableFuture<Object> tryFire(int mode) {
            // assert mode != ASYNC;
            CompletableFuture<Object> downFuture = this.downFuture;
            CompletableFuture<?> upFuture = this.upFuture;
            CompletableFuture<?>[] upFutures = this.upFutures;
            Object upResult;
            
            // 如果上游阶段还没有执行结果，则需要等待上游任务完成后再触发到此
            if(downFuture == null || upFuture == null || (upResult = upFuture.result) == null || upFutures == null) {
                return null;
            }
            
            this.downFuture = null;
            this.upFuture = null;
            this.upFutures = null;
            
            // 设置任务结果(响应信息，可能是返回值，也可能是异常)
            if(!downFuture.completeRelay(upResult)) {
                return null;
            }
            
            for(CompletableFuture<?> future : upFutures) {
                if(future != upFuture) {
                    future.cleanStack();
                }
            }
            
            if(mode<0) {
                return downFuture;
            }
            
            
            // (递归)处理d阶段的下游任务栈(会触发tryFire(NESTED))
            downFuture.postComplete();
            
            return null;
        }
        
        final boolean isLive() {
            return downFuture != null && downFuture.result == null;
        }
    }
    
    @SuppressWarnings("serial")
    static final class UniRelay<U, T extends U> extends UniCompletion<T, U> {
        UniRelay(CompletableFuture<U> future, CompletableFuture<T> upFuture1) {
            super(null, future, upFuture1);
        }
        
        // 执行任务
        final CompletableFuture<U> tryFire(int mode) {
            CompletableFuture<U> future = this.future;
            CompletableFuture<T> upFuture1 = this.upFuture1;
            
            if(future == null) {
                return null;
            }
            
            // 如果上游阶段还没有执行结果，则需要等待上游任务完成后再触发到此
            if(upFuture1 == null || upFuture1.result == null) {
                return null;
            }
            
            if(future.result == null) {
                future.completeRelay(upFuture1.result);
            }
            
            this.future = null;
            this.upFuture1 = null;
            
            return future.postFire(upFuture1, mode);
        }
    }
    
    @SuppressWarnings("serial")
    static final class BiRelay<T, U> extends BiCompletion<T, U, Void> { // for And
        BiRelay(CompletableFuture<Void> future, CompletableFuture<T> upFuture1, CompletableFuture<U> upFuture2) {
            super(null, future, upFuture1, upFuture2);
        }
        
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> future = this.future;
            CompletableFuture<T> upFuture1 = this.upFuture1;
            CompletableFuture<U> upFuture2 = this.upFuture2;
            
            Throwable ex;
            
            if(future == null) {
                return null;
            }
            
            // 如果上游阶段还没有执行结果，则需要等待上游任务完成后再触发到此
            if(upFuture1 == null || upFuture1.result == null || upFuture2 == null || upFuture2.result == null) {
                return null;
            }
            
            if(future.result == null) {
                if((upFuture1.result instanceof AltResult && (ex = ((AltResult) upFuture1.result).ex) != null)) {
                    future.completeThrowable(ex, upFuture1.result);
                } else if(upFuture2.result instanceof AltResult && (ex = ((AltResult) upFuture2.result).ex) != null) {
                    future.completeThrowable(ex, upFuture2.result);
                } else {
                    future.completeNull();
                }
            }
            
            this.future = null;
            this.upFuture1 = null;
            this.upFuture2 = null;
            
            return future.postFire(upFuture1, upFuture2, mode);
        }
    }
    
    /** A Completion delegating to a BiCompletion */
    @SuppressWarnings("serial")
    static final class CoCompletion extends Completion {
        BiCompletion<?, ?, ?> base;
        
        CoCompletion(BiCompletion<?, ?, ?> base) {
            this.base = base;
        }
        
        final CompletableFuture<?> tryFire(int mode) {
            CompletableFuture<?> downFuture;
            
            if(base == null) {
                return null;
            }
            
            downFuture = base.tryFire(mode);
            
            if(downFuture == null) {
                return null;
            }
            
            base = null;    // detach
            
            return downFuture;
        }
        
        final boolean isLive() {
            return base != null && base.future != null;
        }
    }
    
    
    // 任务结果(用于包装null或异常)
    static final class AltResult {
        /**
         * 对于抛异常任务，此处存储异常信息
         * 对于无返回值或返回值为null的任务，此处存储null
         * 当ex为null时，外在显示为NIL
         */
        final Throwable ex;        // null only for NIL
        
        AltResult(Throwable ex) {
            this.ex = ex;
        }
    }
    
    
    /**
     * Completion for recording and releasing a waiting thread.
     * This class implements ManagedBlocker to avoid starvation when blocking actions pile up in ForkJoinPools.
     */
    // 阻塞块
    @SuppressWarnings("serial")
    static final class Signaller extends Completion implements ForkJoinPool.ManagedBlocker {
        
        volatile Thread thread;         // 阻塞块当前所在的线程
        final boolean interruptible;    // 阻塞块是否支持中断
        boolean interrupted;            // 当前线程是否已有中断标记
        final long deadline;            // non-zero if timed
        long nanos;                     // remaining wait time if timed
        
        Signaller(boolean interruptible, long nanos, long deadline) {
            this.thread = Thread.currentThread();
            this.interruptible = interruptible;
            this.nanos = nanos;
            this.deadline = deadline;
        }
        
        // 当前线程是否需要解除阻塞
        public boolean isReleasable() {
            // 测试当前线程是否已经中断，线程的中断状态会被清除
            if(Thread.interrupted()) {
                // 线程带有中断标记
                interrupted = true;
            }
            
            // 如果线程带有中断标记，且阻塞块支持中断
            if(interrupted && interruptible) {
                return true;
            }
            
            // 如果启用了超时设置
            if(deadline != 0L) {
                // 如果已经超时，表示可以解除阻塞了
                if(nanos<=0L) {
                    return true;
                }
                
                // 计算剩余阻塞时间
                nanos = deadline - System.nanoTime();
                
                // 如果已经超时，表示可以解除阻塞了
                if(nanos<=0L) {
                    return true;
                }
            }
            
            /* 至此，说明未启用超时设置(一直阻塞)，或者还未超时 */
            
            // 阻塞线程是否还在，如果阻塞线程已经置空，说明该线程不需要阻塞了
            return thread == null;
        }
        
        // 阻塞当前线程，返回true代表阻塞结束
        public boolean block() {
            // 如果仍然需要阻塞
            while(!isReleasable()) {
                if(deadline == 0L) {
                    LockSupport.park(this);
                } else {
                    LockSupport.parkNanos(this, nanos);
                }
            }
            
            return true;
        }
        
        // 解除已完成任务所在线程的阻塞
        final CompletableFuture<?> tryFire(int ignore) {
            Thread w; // no need to atomically claim
            
            if((w = thread) != null) {
                thread = null;
                LockSupport.unpark(w);
            }
            
            return null;
        }
        
        // 当前任务是否"存活"
        final boolean isLive() {
            // 包含阻塞线程的任务才算存活
            return thread != null;
        }
    }
    
    
    /** Fallback if ForkJoinPool.commonPool() cannot support parallelism */
    // 单线程任务执行器
    static final class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }
    
    @SuppressWarnings("serial")
    static final class UniExceptionally<T> extends UniCompletion<T, T> {
        Function<? super Throwable, ? extends T> action;
        
        UniExceptionally(CompletableFuture<T> future, CompletableFuture<T> upFuture1, Function<? super Throwable, ? extends T> action) {
            super(null, future, upFuture1);
            this.action = action;
        }
        
        final CompletableFuture<T> tryFire(int mode) {
            //            assert mode != ASYNC;    // never ASYNC
            
            CompletableFuture<T> future = this.future;
            CompletableFuture<T> upFuture1 = this.upFuture1;
            Object upResult1;
            
            if(action == null || future == null) {
                return null;
            }
            
            // 如果上游阶段还没有执行结果，则需要等待上游任务完成后再触发到此
            if(upFuture1 == null || (upResult1 = upFuture1.result) == null) {
                return null;
            }
            
            if(!future.uniExceptionally(upResult1, action, this)) {
                return null;
            }
            
            this.future = null;
            this.upFuture1 = null;
            this.action = null;
            
            return future.postFire(upFuture1, mode);
        }
    }
    
    /**
     * Singleton delay scheduler, used only for starting and cancelling tasks.
     */
    // 一次性的定时任务执行器
    static final class Delayer {
        // 定时任务线程池
        static final ScheduledThreadPoolExecutor delayer;
        
        static {
            // 守护线程工厂
            DaemonThreadFactory threadFactory = new DaemonThreadFactory();
    
            // 【核心阈值】为1的定时任务线程池
            delayer = new ScheduledThreadPoolExecutor(1, threadFactory);
    
            // 设置允许移除被中止的任务
            delayer.setRemoveOnCancelPolicy(true);
        }
        
        // 执行一次性的定时任务(Runnable)，并返回任务本身：在任务启动后的initialDelay时长后开始执行(在一个守护线程中执行)
        static ScheduledFuture<?> delay(Runnable command, long initialDelay, TimeUnit unit) {
            return delayer.schedule(command, initialDelay, unit);
        }
        
        // 守护线程工厂
        static final class DaemonThreadFactory implements ThreadFactory {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                thread.setName("CompletableFutureDelayScheduler");
                return thread;
            }
        }
    }
    
    /** Action to cancel unneeded timeouts */
    // 定时任务清理器
    static final class Canceller implements BiConsumer<Object, Throwable> {
        final Future<?> future;
        
        Canceller(Future<?> future) {
            this.future = future;
        }
        
        // 清理之前注册的定时任务
        public void accept(Object ignore, Throwable ex) {
            if(ex == null && future != null && !future.isDone()) {
                future.cancel(false);
            }
        }
    }
    
    /** Action to completeExceptionally on timeout */
    // 特殊任务，由Delayer执行：当注册的future未在指定时间内完成时，需要为future设置一个异常结果
    static final class Timeout implements Runnable {
        final CompletableFuture<?> future;
        
        Timeout(CompletableFuture<?> future) {
            this.future = future;
        }
        
        public void run() {
            if(future != null && !future.isDone()) {
                // 如果任务还未完成，将其结果设置为指定的异常，并处理该阶段的下游任务栈
                future.completeExceptionally(new TimeoutException());
            }
        }
    }
    
    /** Action to complete on timeout */
    // 特殊任务，由Delayer执行：当注册的future未在指定时间内完成时，需要为future设置一个备用的执行结果
    static final class DelayedCompleter<R> implements Runnable {
        final CompletableFuture<R> future;
        final R bakResult; // 备用的执行结果
        
        DelayedCompleter(CompletableFuture<R> future, R bakResult) {
            this.future = future;
            this.bakResult = bakResult;
        }
        
        public void run() {
            if(future != null) {
                future.complete(bakResult);
            }
        }
    }
    
    /** Action to submit user task */
    // 特殊任务，由DelayedExecutor执行：由给定的【任务执行器】执行给定的任务
    static final class TaskSubmitter implements Runnable {
        final Executor executor;
        final Runnable action;
        
        TaskSubmitter(Executor executor, Runnable action) {
            this.executor = executor;
            this.action = action;
        }
        
        public void run() {
            executor.execute(action);
        }
    }
    
    // 一次性的定时任务执行器
    static final class DelayedExecutor implements Executor {
        final long initialDelay;
        final TimeUnit unit;
        final Executor executor;
        
        DelayedExecutor(long initialDelay, TimeUnit unit, Executor executor) {
            this.initialDelay = initialDelay;
            this.unit = unit;
            this.executor = executor;
        }
        
        public void execute(Runnable runnable) {
            // 构造一个待执行任务
            TaskSubmitter submitter = new TaskSubmitter(executor, runnable);
            
            // 执行一次性的定时任务，并返回任务本身：在任务启动后的initialDelay时长后开始执行(在一个守护线程中执行)
            Delayer.delay(submitter, initialDelay, unit);
        }
    }
    
    /**
     * A subclass that just throws UOE for most non-CompletionStage methods.
     */
    /*
     * "退化"阶段，限制了大部分对阶段的"查询"操作。
     * 几乎支持全部CompletionStage中的操作，以及支持一部分CompletableFuture中的操作。
     */
    static final class MinimalStage<T> extends CompletableFuture<T> {
        
        MinimalStage() {
        }
        
        MinimalStage(Object result) {
            super(result);
        }
        
        
        /*▼ 工具箱 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        @Override
        public CompletableFuture<T> toCompletableFuture() {
            if(result != null) {
                Object obj = encodeRelay(result);
                return new CompletableFuture<T>(obj);
            } else {
                CompletableFuture<T> downFuture = new CompletableFuture<>();
                UniRelay<T, T> downTask = new UniRelay<>(downFuture, this);
                unipush(downTask);
                return downFuture;
            }
        }
        
        @Override
        public <U> CompletableFuture<U> newIncompleteFuture() {
            return new MinimalStage<U>();
        }
        
        @Override
        public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
        
        /*▲ 工具箱 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        /*▼ 获取任务结果 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        @Override
        public T get() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public T join() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public T get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public T getNow(T valueIfAbsent) {
            throw new UnsupportedOperationException();
        }
        
        /*▲ 获取任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        /*▼ 促进任务完成 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        @Override
        public boolean complete(T value) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean completeExceptionally(Throwable ex) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void obtrudeValue(T value) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void obtrudeException(Throwable ex) {
            throw new UnsupportedOperationException();
        }
        
        /*▲ 促进任务完成 ████████████████████████████████████████████████████████████████████████████████┛ */
        
        
        /*▼ 阶段状态 ████████████████████████████████████████████████████████████████████████████████┓ */
        
        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean isCompletedExceptionally() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean isCancelled() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public int getNumberOfDependents() {
            throw new UnsupportedOperationException();
        }
        
        /*▲ 阶段状态 ████████████████████████████████████████████████████████████████████████████████┛ */
        
    }
    
}
