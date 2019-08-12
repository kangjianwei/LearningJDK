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
 * CompletableFuture代表一个任务阶段。各个任务（及执行结果）可以划分到不同的阶段，不同的阶段可以串联执行
 * 这里跟一般的思路不同：不是将任务封装到阶段，而是将阶段封装到任务中，阶段中又包含了任务的执行结果
 *
 * ---------------------------------------------------------------------------------------
 * AsyncSupply     （不可做枢纽，但其所在阶段包含枢纽栈）单阶段异步任务，有返回值，当前阶段的任务源是Supplier
 * AsyncRun        （不可做枢纽，但其所在阶段包含枢纽栈）单阶段异步任务，无返回值，当前阶段的任务源是Runnable
 * ---------------------------------------------------------------------------------------
 * UniApply        （可以做枢纽）依赖一个上游阶段的任务，当前阶段的任务源是Function，需要使用上游任务的执行结果作为入参
 * UniAccept       （可以做枢纽）依赖一个上游阶段的任务，当前阶段的任务源是Consumer，需要使用上游任务的执行结果作为入参
 * UniRun          （可以做枢纽）依赖一个上游阶段的任务，当前阶段的任务源是Runnable，无入参
 * ---------------------------------------------------------------------------------------
 * BiApply         （可以做枢纽）依赖两个上游阶段的任务，当前阶段的任务源是BiFunction，需要使用两个上游任务的执行结果作为入参
 * BiAccept        （可以做枢纽）依赖两个上游阶段的任务，当前阶段的任务源是BiConsumer，需要使用两个上游任务的执行结果作为入参
 * BiRun           （可以做枢纽）依赖两个上游阶段的任务，当前阶段的任务源是Runnable，无入参
 * ---------------------------------------------------------------------------------------
 * OrApply         （可以做枢纽）依赖两个上游阶段的任务，当前阶段的任务源是Function，需要使用其中一个（谁先执行完用谁）上游任务的执行结果作为入参
 * OrAccept        （可以做枢纽）依赖两个上游阶段的任务，当前阶段的任务源是Consumer，需要使用其中一个（谁先执行完用谁）上游任务的执行结果作为入参
 * OrRun           （可以做枢纽）依赖两个上游阶段的任务，当前阶段的任务源是Runnable，无入参
 * ---------------------------------------------------------------------------------------
 * UniCompose      （可以做枢纽）依赖一个上游阶段的任务，当前阶段的任务源是Function，需要使用上游任务的执行结果作为入参，经处理后生成新的CompletableFuture
 * UniHandle       （可以做枢纽）依赖一个上游阶段的任务，当前阶段的任务源是BiFunction，需要使用上游任务的执行结果和抛出的异常作为入参
 * UniWhenComplete （可以做枢纽）依赖一个上游阶段的任务，当前阶段的任务源是BiConsumer，需要使用上游任务的执行结果和抛出的异常作为入参
 * ---------------------------------------------------------------------------------------
 * BiRelay         （可以做枢纽）依赖多个上游阶段的任务，等待任一上游任务完成的过程中，会收集上游任务中抛出的异常信息（中途遇到异常不会停下来，也不会抛出来），而且谁在入参中排在前面收集谁的信息
 * AnyOf           （可以做枢纽）依赖多个上游阶段的任务，等待全部上游任务完成的过程中，会收集上游任务中抛出的异常信息和返回值（中途遇到异常不会停下来，也不会抛出来），而且谁先执行完收集谁的信息
 * ---------------------------------------------------------------------------------------
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
    static final int SYNC   =  0;   // 同步
    static final int ASYNC  =  1;   // 异步
    
    
    // 枢纽栈，存储待完成任务（联结下一个阶段的任务）
    volatile Completion stack;    // Top of Treiber stack of dependent actions
    
    
    /** The encoding of the null value. */
    // 对于返回值为null或者无返回值的任务，其执行结果是NIL
    static final AltResult NIL = new AltResult(null);
    
    // 任务执行结果
    volatile Object result;       // Either the result or boxed AltResult
    
    private static final boolean USE_COMMON_POOL = (ForkJoinPool.getCommonPoolParallelism()>1);
    
    /**
     * Default executor -- ForkJoinPool.commonPool() unless it cannot support parallelism.
     */
    // 默认使用【共享工作池】，除非不支持并行
    private static final Executor ASYNC_POOL = USE_COMMON_POOL ? ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();
    
    
    // VarHandle mechanics
    private static final VarHandle RESULT;
    private static final VarHandle STACK;
    private static final VarHandle NEXT;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            RESULT = l.findVarHandle(CompletableFuture.class, "result", Object.class);
            STACK = l.findVarHandle(CompletableFuture.class, "stack", Completion.class);
            
            NEXT = l.findVarHandle(Completion.class, "next", Completion.class);
        } catch (ReflectiveOperationException e) {
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
    CompletableFuture(Object r) {
        this.result = r;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ AsyncSupply ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （不可做枢纽，但其所在阶段包含枢纽栈）单阶段异步任务，有返回值，任务源是Supplier
    @SuppressWarnings("serial")
    static final class AsyncSupply<T> extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<T> dep;   // 当前任务所属阶段
        Supplier<? extends T> fn;   // 任务源
        
        AsyncSupply(CompletableFuture<T> dep, Supplier<? extends T> fn) {
            this.dep = dep;
            this.fn = fn;
        }
        
        // 执行任务
        public final boolean exec() {
            run();
            return false;
        }
        
        // 任务执行逻辑
        public void run() {
            CompletableFuture<T> d = dep;
            Supplier<? extends T> f = fn;
            
            if(d==null || f==null){
                return;
            }
            
            dep = null;
            fn = null;
            
            // 如果已有执行结果，直接返回
            if(d.result != null){
                return;
            }
            
            try {
                // 执行任务源
                T t = f.get();
                
                // 设置任务结果
                d.completeValue(t);
            } catch(Throwable ex) {
                // 设置异常结果
                d.completeThrowable(ex);
            }
            
            // 当前阶段的任务完成后，处理该阶段的枢纽栈
            d.postComplete();
        }
        
        public final Void getRawResult() {
            return null;
        }
        
        public final void setRawResult(Void v) {
        }
        
    }
    
    
    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} with
     * the value obtained by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     *                 to complete the returned CompletableFuture
     * @param <U>      the function's return type
     *
     * @return the new CompletableFuture
     */
    // 使用【默认工作池】执行AsyncSupply型任务
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return asyncSupplyStage(ASYNC_POOL, supplier);
    }
    
    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the given executor with the value obtained
     * by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     *                 to complete the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @param <U>      the function's return type
     *
     * @return the new CompletableFuture
     */
    // 使用指定的【任务执行器】执行AsyncSupply型任务
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        return asyncSupplyStage(screenExecutor(executor), supplier);
    }
    
    
    // 执行AsyncSupply型任务
    static <U> CompletableFuture<U> asyncSupplyStage(Executor e, Supplier<U> f) {
        if(f == null) {
            throw new NullPointerException();
        }
        
        CompletableFuture<U> d = new CompletableFuture<U>();
        // 异步执行
        e.execute(new AsyncSupply<U>(d, f));
        
        return d;
    }
    
    /*▲ AsyncSupply ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ AsyncRun ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （不可做枢纽，但其所在阶段包含枢纽栈）单阶段异步任务，无返回值，任务源是Runnable
    @SuppressWarnings("serial")
    static final class AsyncRun extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<Void> dep;    // 当前任务所属阶段
        Runnable fn;                    // 任务源
        
        AsyncRun(CompletableFuture<Void> dep, Runnable fn) {
            this.dep = dep;
            this.fn = fn;
        }
        
        // 执行任务
        public final boolean exec() {
            run();
            return false;
        }
        
        // 任务执行逻辑
        public void run() {
            CompletableFuture<Void> d = dep;
            Runnable f = fn;
            
            if(d==null || f==null){
                return;
            }
            
            dep = null;
            fn = null;
            
            // 如果已有执行结果，直接返回
            if(d.result != null){
                return;
            }
            
            try {
                // 执行任务源
                f.run();
                
                // 设置任务结果
                d.completeNull();
            } catch(Throwable ex) {
                // 设置异常结果
                d.completeThrowable(ex);
            }
            
            // 当前阶段的任务完成后，处理该阶段的枢纽栈
            d.postComplete();
        }
        
        public final Void getRawResult() {
            return null;
        }
        
        public final void setRawResult(Void v) {
        }
    }
    
    
    
    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} after
     * it runs the given action.
     *
     * @param runnable the action to run before completing the
     *                 returned CompletableFuture
     *
     * @return the new CompletableFuture
     */
    // 使用【默认工作池】执行AsyncRun型任务
    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return asyncRunStage(ASYNC_POOL, runnable);
    }
    
    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the given executor after it runs the given
     * action.
     *
     * @param runnable the action to run before completing the
     *                 returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     *
     * @return the new CompletableFuture
     */
    // 使用指定的【任务执行器】执行AsyncRun型任务
    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return asyncRunStage(screenExecutor(executor), runnable);
    }
    
    
    // 执行AsyncRun型任务
    static CompletableFuture<Void> asyncRunStage(Executor e, Runnable f) {
        if(f == null) {
            throw new NullPointerException();
        }
        
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        
        // 异步执行
        e.execute(new AsyncRun(d, f));
        
        return d;
    }
    
    /*▲ AsyncRun ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniApply ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （可以做枢纽）依赖一个上游阶段的任务，任务源是Function，需要使用上游任务的执行结果作为入参
    @SuppressWarnings("serial")
    static final class UniApply<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends V> fn; // 任务源
        
        /*
         * src - 上游阶段
         * dep - 当前阶段
         */
        UniApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, Function<? super T, ? extends V> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }
        
        // 执行任务
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d = dep;   // 当前任务所属阶段
            CompletableFuture<T> a = src;   // 上游阶段
            Object r;                       // 上游阶段的任务结果
            
            if(d==null || fn==null || a==null || (r=a.result)==null) {
                return null;
            }

tryComplete:
            // 如果当前阶段还没有执行结果，说明任务还没完成
            if(d.result == null) {
                // 如果上游阶段已有AltResult类型结果
                if(r instanceof AltResult) {
                    Throwable x = ((AltResult) r).ex;
                    
                    // 如果上游阶段产生的是异常
                    if(x != null) {
                        // 同样为当前阶段设置异常结果
                        d.completeThrowable(x, r);
                        // 跳出外层if
                        break tryComplete;
                    }
                    
                    // 至此，上游阶段的结果可能是null，也可能无返回值，但确定的是，其结果包装在了AltResult中
                    r = null;
                }
                
                try {
                    // 处理模式是NESTED或SYNC
                    if(mode<=0) {
                        /*
                         * 尝试异步执行当前任务，如果成功，则会辗转调用到tryFire(ASYNC)
                         * 返回true表示当前线程锁定该任务，但该任务需要同步执行
                         */
                        if(!claim()){
                            // 如果当前任务被别的线程抢走执行，或者成功了调用了异步执行逻辑，则直接返回
                            return null;
                        }
                    }
                    
                    // 触发当前任务源，入参是上游的执行结果
                    @SuppressWarnings("unchecked")
                    V t = fn.apply((T) r);
                    
                    // 设置任务结果
                    d.completeValue(t);
                } catch(Throwable ex) {
                    // 设置异常结果
                    d.completeThrowable(ex);
                }
            }
            
            dep = null;
            src = null;
            fn = null;
            
            // 下游阶段的任务执行完之后，先检查上游阶段的枢纽栈，再检查当前阶段的枢纽栈
            return d.postFire(a, mode);
        }
    }
    
    
    
    // 同步执行UniApply型任务
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> function) {
        return uniApplyStage(null, function);
    }
    
    // 使用【默认工作池】执行UniApply型任务
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> function) {
        return uniApplyStage(defaultExecutor(), function);
    }
    
    // 使用指定的【任务执行器】执行UniApply型任务
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> function, Executor executor) {
        return uniApplyStage(screenExecutor(executor), function);
    }
    
    
    // 执行UniApply型任务
    private <V> CompletableFuture<V> uniApplyStage(Executor executor, Function<? super T, ? extends V> function) {
        if(function == null) {
            throw new NullPointerException();
        }
        
        // 上游阶段的任务已有执行结果
        if(result != null) {
            // 立即执行当前阶段的任务
            return uniApplyNow(result, executor, function);
        }
        
        CompletableFuture<V> dep = newIncompleteFuture();
        // 尝试将（作为下游的）指定任务送入枢纽栈，如果（作为上游的）当前阶段任务已有执行结果，则同步执行指定任务
        unipush(new UniApply<T, V>(executor, dep, this, function));
        
        return dep;
    }
    
    // 在上游阶段的任务已有执行结果时，立即执行当前阶段的任务
    private <V> CompletableFuture<V> uniApplyNow(Object r, Executor executor, Function<? super T, ? extends V> function) {
        CompletableFuture<V> dep = newIncompleteFuture();
        
        if(r instanceof AltResult) {
            Throwable x = ((AltResult) r).ex;
            
            if(x != null) {
                dep.result = encodeThrowable(x, r);
                return dep;
            }
            
            r = null;
        }
        
        try {
            // 需要异步执行当前阶段的任务，会辗转调用到UniApply#tryFire(ASYNC)
            if(executor != null) {
                /*
                 * 创建UniApply时传入的executor参数为null
                 * 这意味着进入tryFire()方法后，不会再次发起异步请求（参见claim()）
                 */
                executor.execute(new UniApply<T, V>(null, dep, this, function));
            } else {
                // 需要同步执行当前阶段的任务，任务源的入参是上游任务的执行结果
                @SuppressWarnings("unchecked")
                V t = function.apply((T) r);
                dep.result = dep.encodeValue(t);
            }
        } catch(Throwable ex) {
            dep.result = encodeThrowable(ex);
        }
        
        return dep;
    }
    
    /**
     * Pushes the given completion unless it completes while trying.
     * Caller should first check that result is null.
     */
    // 尝试将（作为下游的）指定任务送入枢纽栈，如果（作为上游的）当前阶段任务已有执行结果，则同步执行指定任务
    final void unipush(Completion completion) {
        if(completion==null){
            return;
        }
        
        // 尝试将指定的任务加入枢纽栈
        while(!tryPushStack(completion)) {
            // 如果（作为上游的）当前阶段已有执行结果，则切段当前任务与枢纽栈的联系
            if(result != null) {
                NEXT.set(completion, null);
                break;
            }
        }
        
        // 如果（作为上游的）当前阶段已有执行结果
        if(result != null) {
            // 直接处理（作为下游的）指定任务（同步执行）
            completion.tryFire(SYNC);
        }
    }
    
    /*▲ UniApply ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniAccept ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * （可以做枢纽）依赖一个上游阶段的任务，任务源是Consumer，需要使用上游任务的执行结果作为入参
     * 执行完成后，为（作为下游的）当前阶段设置代表“空”的任务结果
     */
    @SuppressWarnings("serial")
    static final class UniAccept<T> extends UniCompletion<T, Void> {
        Consumer<? super T> fn;
        
        UniAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, Consumer<? super T> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }
        
        // 执行任务
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d = dep;    // 当前任务所属阶段
            CompletableFuture<T>    a = src;    // 上游阶段
            Object r;                           // 上游阶段的任务结果
            
            if(d == null || fn == null || a == null || (r = a.result) == null) {
                return null;
            }

tryComplete:
            // 如果当前阶段还没有执行结果，说明任务还没完成
            if(d.result == null) {
                // 如果上游阶段已有AltResult类型结果
                if(r instanceof AltResult) {
                    Throwable x = ((AltResult) r).ex;
                    
                    // 如果上游阶段产生的是异常
                    if(x != null) {
                        // 同样为当前阶段设置异常结果
                        d.completeThrowable(x, r);
                        // 跳出外层if
                        break tryComplete;
                    }
                    
                    // 至此，上游阶段的结果可能是null，也可能无返回值，但确定的是，其结果包装在了AltResult中
                    r = null;
                }
                
                try {
                    // 处理模式是NESTED或SYNC
                    if(mode<=0) {
                        /*
                         * 尝试异步执行当前任务，如果成功，则会辗转调用到tryFire(ASYNC)
                         * 返回true表示当前线程锁定该任务，但该任务需要同步执行
                         */
                        if(!claim()){
                            // 如果当前任务被别的线程抢走执行，或者成功了调用了异步执行逻辑，则直接返回
                            return null;
                        }
                    }
                    
                    // 触发当前任务源，入参是上游的执行结果
                    @SuppressWarnings("unchecked")
                    T t = (T) r;
                    fn.accept(t);
                    
                    // 设置任务结果
                    d.completeNull();
                } catch(Throwable ex) {
                    // 设置异常结果
                    d.completeThrowable(ex);
                }
            }
            
            dep = null;
            src = null;
            fn = null;
            
            // 下游阶段的任务执行完之后，先检查上游阶段的枢纽栈，再检查当前阶段的枢纽栈
            return d.postFire(a, mode);
        }
    }
    
    
    
    // 同步执行UniAccept型任务
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return uniAcceptStage(null, action);
    }
    
    // 使用【默认工作池】执行UniAccept型任务
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return uniAcceptStage(defaultExecutor(), action);
    }
    
    // 使用指定的【任务执行器】执行UniAccept型任务
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return uniAcceptStage(screenExecutor(executor), action);
    }
    
    
    // 执行UniAccept型任务
    private CompletableFuture<Void> uniAcceptStage(Executor e, Consumer<? super T> f) {
        if(f == null) {
            throw new NullPointerException();
        }
        
        // 上游阶段的任务已有执行结果
        if(result != null) {
            // 立即执行当前阶段的任务
            return uniAcceptNow(result, e, f);
        }
        
        CompletableFuture<Void> dep = newIncompleteFuture();
        // 尝试将（作为下游的）指定任务送入枢纽栈，如果（作为上游的）当前阶段任务已有执行结果，则同步执行指定任务
        unipush(new UniAccept<T>(e, dep, this, f));
        
        return dep;
    }
    
    // 在上游阶段的任务已有执行结果时，立即执行当前阶段的任务
    private CompletableFuture<Void> uniAcceptNow(Object r, Executor e, Consumer<? super T> f) {
        CompletableFuture<Void> dep = newIncompleteFuture();
        
        if(r instanceof AltResult) {
            Throwable x = ((AltResult) r).ex;
            
            if(x != null) {
                dep.result = encodeThrowable(x, r);
                return dep;
            }
            
            r = null;
        }
        
        try {
            // 需要异步执行当前阶段的任务，会辗转调用到UniAccept#tryFire(ASYNC)
            if(e != null) {
                /*
                 * 创建UniAccept时传入的executor参数为null
                 * 这意味着进入tryFire()方法后，不会再次发起异步请求（参见claim()）
                 */
                e.execute(new UniAccept<T>(null, dep, this, f));
            } else {
                // 需要同步执行当前阶段的任务，任务源的入参是上游任务的执行结果
                @SuppressWarnings("unchecked")
                T t = (T) r;
                f.accept(t);
                dep.result = NIL;
            }
        } catch(Throwable ex) {
            dep.result = encodeThrowable(ex);
        }
        
        return dep;
    }
    
    /*▲ UniAccept ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniRun ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * （可以做枢纽）依赖一个上游阶段的任务，任务源是Runnable，无入参
     * 执行完成后，为（作为下游的）当前阶段设置代表“空”的任务结果
     */
    @SuppressWarnings("serial")
    static final class UniRun<T> extends UniCompletion<T, Void> {
        Runnable fn;
        
        UniRun(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, Runnable fn) {
            super(executor, dep, src);
            this.fn = fn;
        }
        
        // 执行任务
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d = dep;    // 当前任务所属阶段
            CompletableFuture<T> a = src;       // 上游阶段
            Object r;                           // 上游阶段的任务结果
            
            if(d == null || fn == null || a == null || (r = a.result) == null) {
                return null;
            }

tryComplete:
            if(d.result == null) {
                if(r instanceof AltResult) {
                    Throwable x = ((AltResult) r).ex;
                    
                    if(x != null){
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                }
                
                try {
                    if(mode<=0 && !claim()) {
                        return null;
                    }
                    
                    fn.run();
                    d.completeNull();
                } catch(Throwable ex) {
                    d.completeThrowable(ex);
                }
                
            }
            
            dep = null;
            src = null;
            fn = null;
            
            return d.postFire(a, mode);
        }
    }
    
    
    
    // 同步执行UniRun型任务
    public CompletableFuture<Void> thenRun(Runnable action) {
        return uniRunStage(null, action);
    }
    
    // 使用【默认工作池】执行UniRun型任务
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return uniRunStage(defaultExecutor(), action);
    }
    
    // 使用指定的【任务执行器】执行UniRun型任务
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return uniRunStage(screenExecutor(executor), action);
    }
    
    
    // 执行UniRun型任务
    private CompletableFuture<Void> uniRunStage(Executor e, Runnable f) {
        if(f == null) {
            throw new NullPointerException();
        }
        
        if(result != null) {
            return uniRunNow(result, e, f);
        }
        
        CompletableFuture<Void> d = newIncompleteFuture();
        unipush(new UniRun<T>(e, d, this, f));
        
        return d;
    }
    
    // 在上游阶段的任务已有执行结果时，立即执行当前阶段的任务
    private CompletableFuture<Void> uniRunNow(Object r, Executor e, Runnable f) {
        CompletableFuture<Void> d = newIncompleteFuture();
        
        Throwable x;
        
        if(r instanceof AltResult && (x = ((AltResult) r).ex) != null) {
            d.result = encodeThrowable(x, r);
        } else {
            try {
                if(e != null) {
                    e.execute(new UniRun<T>(null, d, this, f));
                } else {
                    f.run();
                    d.result = NIL;
                }
            } catch(Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        
        return d;
    }
    
    /*▲ UniRun ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ BiApply ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （可以做枢纽）依赖两个上游阶段的任务，任务源是BiFunction，需要使用两个上游任务的执行结果作为入参
    @SuppressWarnings("serial")
    static final class BiApply<T, U, V> extends BiCompletion<T, U, V> {
        BiFunction<? super T, ? super U, ? extends V> fn;
        
        BiApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
            super(executor, dep, src, other);
            this.fn = fn;
        }
        
        // 执行任务
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d = dep;   // 当前任务所属阶段
            CompletableFuture<T> a = src;   // 上游阶段（其中之一）
            CompletableFuture<U> b = other; // 上游阶段（其中之一）
            
            if(fn == null
                || d == null
                || a == null || a.result == null
                || b == null || b.result == null
                || !d.biApply(a.result, b.result, fn, mode>0 ? null : this)) {
                return null;
            }
            
            dep = null;
            src = null;
            other = null;
            fn = null;
            
            return d.postFire(a, b, mode);
        }
    }
    
    
    
    // 同步执行BiApply型任务
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return biApplyStage(null, other, fn);
    }
    
    // 使用【默认工作池】执行BiApply型任务
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return biApplyStage(defaultExecutor(), other, fn);
    }
    
    // 使用指定的【任务执行器】执行BiApply型任务
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return biApplyStage(screenExecutor(executor), other, fn);
    }
    
    
    // 执行BiApply型任务
    private <U, V> CompletableFuture<V> biApplyStage(Executor e, CompletionStage<U> other, BiFunction<? super T, ? super U, ? extends V> f) {
        // 另一个源阶段的任务
        CompletableFuture<U> b = other.toCompletableFuture();
        
        if(f == null || b == null) {
            throw new NullPointerException();
        }
        
        CompletableFuture<V> d = newIncompleteFuture();
        
        // 两个上游阶段的任务之中，至少有一个未完成
        if(result == null || b.result == null) {
            bipush(b, new BiApply<T, U, V>(e, d, this, b, f));
            
            // 两个上游阶段均有了任务结果，且任务执行器为null，则这里同步计算当前阶段的任务结果
        } else if(e == null) {
            d.biApply(result, b.result, f, null);
            
            // 两个上游阶段均有了任务结果，且任务执行器不为null，则使用指定的执行器执行任务
        } else {
            try {
                e.execute(new BiApply<T, U, V>(null, d, this, b, f));
            } catch(Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        
        return d;
    }
    
    // 直接执行
    final <R, S> boolean biApply(Object r, Object s, BiFunction<? super R, ? super S, ? extends T> f, BiApply<R, S, T> c) {
tryComplete:
        if(result == null) {
            if(r instanceof AltResult) {
                Throwable x = ((AltResult) r).ex;
                if(x != null) {
                    completeThrowable(x, r);
                    break tryComplete;
                }
                
                r = null;
            }
            
            if(s instanceof AltResult) {
                Throwable x =((AltResult) s).ex;
                if(x != null) {
                    completeThrowable(x, s);
                    break tryComplete;
                }
                
                s = null;
            }
            
            try {
                if(c != null && !c.claim()) {
                    return false;
                }
                
                @SuppressWarnings("unchecked")
                R rr = (R) r;
                
                @SuppressWarnings("unchecked")
                S ss = (S) s;
                
                T t = f.apply(rr, ss);
                
                completeValue(t);
            } catch(Throwable ex) {
                completeThrowable(ex);
            }
        }
        
        return true;
    }
    
    /**
     * Pushes completion to this and b unless both done.
     * Caller should first check that either result or b.result is null.
     */
    final void bipush(CompletableFuture<?> b, BiCompletion<?, ?, ?> c) {
        if(c != null) {
            // 如果（作为上游的）当前源阶段还没有任务结果
            while(result == null) {
                // 尝试将指定的任务加入枢纽栈
                if(tryPushStack(c)) {
                    // 如果（作为上游的）另一个阶段还没有任务结果
                    if(b.result == null) {
                        b.unipush(new CoCompletion(c));
                        
                        // 如果（作为上游的）两个阶段都已有结果了
                    } else if(result != null) {
                        // 直接处理枢纽，以执行下游任务
                        c.tryFire(SYNC);
                    }
                    
                    return;
                }
            }
            
            // 如果源阶段二还没有任务结果
            b.unipush(c);
        }
    }
    
    /*▲ BiApply ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ BiAccept ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （可以做枢纽）依赖两个上游阶段的任务，任务源是BiConsumer，需要使用两个上游任务的执行结果作为入参
    @SuppressWarnings("serial")
    static final class BiAccept<T, U> extends BiCompletion<T, U, Void> {
        BiConsumer<? super T, ? super U> fn;
        
        BiAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> other, BiConsumer<? super T, ? super U> fn) {
            super(executor, dep, src, other);
            this.fn = fn;
        }
        
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d = dep;    // 当前任务所属阶段
            CompletableFuture<T>    a = src;    // 上游阶段（其中之一）
            CompletableFuture<U>    b = other;  // 上游阶段（其中之一）
            
            Object r, s;
            
            if(fn == null
                || d == null
                || a == null || a.result == null
                || b == null || b.result == null
                || !d.biAccept(a.result, b.result, fn, mode>0 ? null : this)) {
                return null;
            }
            
            dep = null;
            src = null;
            other = null;
            fn = null;
            
            return d.postFire(a, b, mode);
        }
    }
    
    
    
    // 同步执行BiAccept型任务
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(null, other, action);
    }
    
    // 使用【默认工作池】执行BiAccept型任务
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(defaultExecutor(), other, action);
    }
    
    // 使用指定的【任务执行器】执行BiAccept型任务
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        return biAcceptStage(screenExecutor(executor), other, action);
    }
    
    
    // 执行BiAccept型任务
    private <U> CompletableFuture<Void> biAcceptStage(Executor e, CompletionStage<U> other, BiConsumer<? super T, ? super U> f) {
        CompletableFuture<U> b = other.toCompletableFuture();
        
        if(f == null || b == null) {
            throw new NullPointerException();
        }
        
        CompletableFuture<Void> d = newIncompleteFuture();
        
        // 两个上游阶段的任务之中，至少有一个未完成
        if(result == null || b.result == null) {
            bipush(b, new BiAccept<T, U>(e, d, this, b, f));
            
            // 两个上游阶段均有了任务结果，且任务执行器为null，则这里同步计算当前阶段的任务结果
        } else if(e == null) {
            d.biAccept(result, b.result, f, null);
            
            // 两个上游阶段均有了任务结果，且任务执行器不为null，则使用指定的执行器执行任务
        } else {
            try {
                e.execute(new BiAccept<T, U>(null, d, this, b, f));
            } catch(Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        
        return d;
    }
    
    // 直接执行
    final <R, S> boolean biAccept(Object r, Object s, BiConsumer<? super R, ? super S> f, BiAccept<R, S> c) {
        Throwable x;

tryComplete:
        if(result == null) {
            if(r instanceof AltResult) {
                if((x = ((AltResult) r).ex) != null) {
                    completeThrowable(x, r);
                    break tryComplete;
                }
                r = null;
            }
            
            if(s instanceof AltResult) {
                if((x = ((AltResult) s).ex) != null) {
                    completeThrowable(x, s);
                    break tryComplete;
                }
                s = null;
            }
            try {
                if(c != null && !c.claim()) {
                    return false;
                }
                
                @SuppressWarnings("unchecked")
                R rr = (R) r;
                
                @SuppressWarnings("unchecked")
                S ss = (S) s;
                
                f.accept(rr, ss);
                completeNull();
            } catch(Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }
    
    /*▲ BiAccept ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ BiRun ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （可以做枢纽）依赖两个上游阶段的任务，任务源是Runnable，无入参
    @SuppressWarnings("serial")
    static final class BiRun<T, U> extends BiCompletion<T, U, Void> {
        Runnable fn;
        
        BiRun(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> other, Runnable fn) {
            super(executor, dep, src, other);
            this.fn = fn;
        }
        
        // 执行任务
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d = dep;
            CompletableFuture<T> a = src;
            CompletableFuture<U> b = other;
            
            if(fn == null
                || d == null
                || a == null || a.result == null
                || b == null || b.result == null
                || !d.biRun(a.result, b.result, fn, mode>0 ? null : this)) {
                return null;
            }
            
            dep = null;
            src = null;
            other = null;
            fn = null;
            
            return d.postFire(a, b, mode);
        }
    }
    
    
    
    // 同步执行BiRun型任务
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return biRunStage(null, other, action);
    }
    
    // 使用【默认工作池】执行BiRun型任务
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return biRunStage(defaultExecutor(), other, action);
    }
    
    // 使用指定的【任务执行器】执行BiRun型任务
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return biRunStage(screenExecutor(executor), other, action);
    }
    
    
    // 执行BiRun型任务
    private CompletableFuture<Void> biRunStage(Executor e, CompletionStage<?> other, Runnable f) {
        CompletableFuture<?> b = other.toCompletableFuture();
        
        if(f == null || b == null) {
            throw new NullPointerException();
        }
        
        CompletableFuture<Void> d = newIncompleteFuture();
        
        // 两个上游阶段的任务之中，至少有一个未完成
        if(result == null || b.result == null) {
            bipush(b, new BiRun<>(e, d, this, b, f));
            
            // 两个上游阶段均有了任务结果，且任务执行器为null，则这里同步计算当前阶段的任务结果
        } else if(e == null) {
            d.biRun(result, b.result, f, null);
            
            // 两个上游阶段均有了任务结果，且任务执行器不为null，则使用指定的执行器执行任务
        } else {
            try {
                e.execute(new BiRun<>(null, d, this, b, f));
            } catch(Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        
        return d;
    }
    
    // 直接执行
    final boolean biRun(Object r, Object s, Runnable f, BiRun<?, ?> c) {
        Throwable x;
        Object z;
        
        if(result == null) {
            if((r instanceof AltResult && (x = ((AltResult) (z = r)).ex) != null)
                || (s instanceof AltResult && (x = ((AltResult) (z = s)).ex) != null)) {
                completeThrowable(x, z);
            } else {
                try {
                    if(c != null && !c.claim()) {
                        return false;
                    }
                    f.run();
                    completeNull();
                } catch(Throwable ex) {
                    completeThrowable(ex);
                }
            }
        }
        return true;
    }
    
    /*▲ BiRun ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ OrApply ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （可以做枢纽）依赖两个上游阶段的任务，任务源是Function，需要使用其中一个（谁先执行完用谁）上游任务的执行结果作为入参
    @SuppressWarnings("serial")
    static final class OrApply<T, U extends T, V> extends BiCompletion<T, U, V> {
        Function<? super T, ? extends V> fn;
        
        OrApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> other, Function<? super T, ? extends V> fn) {
            super(executor, dep, src, other);
            this.fn = fn;
        }
        
        // 执行任务
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d = dep;     // 当前任务所属阶段
            CompletableFuture<T> a = src;     // 上游阶段（其中之一）
            CompletableFuture<U> b = other;   // 上游阶段（其中之一）
            
            Object r;
            Throwable x;
            
            if(fn == null
                || d == null || a == null || b == null
                || ((r = a.result) == null && (r = b.result) == null)) {
                return null;
            }

tryComplete:
            if(d.result == null) {
                try {
                    if(mode<=0 && !claim()) {
                        return null;
                    }
                    
                    if(r instanceof AltResult) {
                        if((x = ((AltResult) r).ex) != null) {
                            d.completeThrowable(x, r);
                            break tryComplete;
                        }
                        r = null;
                    }
                    
                    @SuppressWarnings("unchecked")
                    T t = (T) r;
                    
                    d.completeValue(fn.apply(t));
                } catch(Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            
            dep = null;
            src = null;
            other = null;
            fn = null;
            
            return d.postFire(a, b, mode);
        }
    }
    
    
    
    // 同步执行OrApply型任务
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return orApplyStage(null, other, fn);
    }
    
    // 使用【默认工作池】执行OrApply型任务
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return orApplyStage(defaultExecutor(), other, fn);
    }
    
    // 使用指定的【任务执行器】执行OrApply型任务
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        return orApplyStage(screenExecutor(executor), other, fn);
    }
    
    
    // 执行OrApply型任务
    private <U extends T, V> CompletableFuture<V> orApplyStage(Executor e, CompletionStage<U> other, Function<? super T, ? extends V> f) {
        CompletableFuture<U> b = other.toCompletableFuture();
        
        if(f == null || b == null) {
            throw new NullPointerException();
        }
        
        Object r;
        CompletableFuture<? extends T> z;
        
        if((r = (z = this).result) != null
            || (r = (z = b).result) != null) {
            return z.uniApplyNow(r, e, f);
        }
        
        CompletableFuture<V> d = newIncompleteFuture();
        orpush(b, new OrApply<T, U, V>(e, d, this, b, f));
        
        return d;
    }
    
    /**
     * Pushes completion to this and b unless either done.
     * Caller should first check that result and b.result are both null.
     */
    final void orpush(CompletableFuture<?> b, BiCompletion<?, ?, ?> c) {
        if(c != null) {
            while(!tryPushStack(c)) {
                if(result != null) {
                    NEXT.set(c, null);
                    break;
                }
            }
            
            if(result != null) {
                c.tryFire(SYNC);
            } else {
                CoCompletion completion = new CoCompletion(c);
                b.unipush(completion);
            }
        }
    }
    
    /*▲ OrApply ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ OrAccept ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （可以做枢纽）依赖两个上游阶段的任务，任务源是Consumer，需要使用其中一个（谁先执行完用谁）上游任务的执行结果作为入参
    @SuppressWarnings("serial")
    static final class OrAccept<T, U extends T> extends BiCompletion<T, U, Void> {
        Consumer<? super T> fn;
        
        OrAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> other, Consumer<? super T> fn) {
            super(executor, dep, src, other);
            this.fn = fn;
        }
        
        // 执行任务
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d = dep;
            CompletableFuture<T> a = src;
            CompletableFuture<U> b = other;
            Object r;
            Throwable x;
            Consumer<? super T> f;
            
            if(fn == null
                || d == null || a == null || b == null
                || ((r = a.result) == null && (r = b.result) == null)) {
                return null;
            }

tryComplete:
            if(d.result == null) {
                try {
                    if(mode<=0 && !claim()) {
                        return null;
                    }
                    
                    if(r instanceof AltResult) {
                        if((x = ((AltResult) r).ex) != null) {
                            d.completeThrowable(x, r);
                            break tryComplete;
                        }
                        r = null;
                    }
                    
                    @SuppressWarnings("unchecked")
                    T t = (T) r;
                    fn.accept(t);
                    d.completeNull();
                } catch(Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            
            dep = null;
            src = null;
            other = null;
            fn = null;
            
            return d.postFire(a, b, mode);
        }
    }
    
    
    
    // 同步执行OrAccept型任务
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return orAcceptStage(null, other, action);
    }
    
    // 使用【默认工作池】执行OrAccept型任务
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return orAcceptStage(defaultExecutor(), other, action);
    }
    
    // 使用指定的【任务执行器】执行OrAccept型任务
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        return orAcceptStage(screenExecutor(executor), other, action);
    }
    
    
    // 执行OrAccept型任务
    private <U extends T> CompletableFuture<Void> orAcceptStage(Executor e, CompletionStage<U> other, Consumer<? super T> f) {
        CompletableFuture<U> b = other.toCompletableFuture();
        
        if(f == null || b == null) {
            throw new NullPointerException();
        }
        
        Object r;
        CompletableFuture<? extends T> z;
        
        if((r = (z = this).result) != null
            || (r = (z = b).result) != null) {
            return z.uniAcceptNow(r, e, f);
        }
        
        CompletableFuture<Void> d = newIncompleteFuture();
        orpush(b, new OrAccept<T, U>(e, d, this, b, f));
        
        return d;
    }
    
    /*▲ OrAccept ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ OrRun ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （可以做枢纽）依赖两个上游阶段的任务，任务源是Runnable，无入参
    @SuppressWarnings("serial")
    static final class OrRun<T, U> extends BiCompletion<T, U, Void> {
        Runnable fn;
        
        OrRun(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> other, Runnable fn) {
            super(executor, dep, src, other);
            this.fn = fn;
        }
        
        // 执行任务
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d = dep;
            CompletableFuture<T> a = src;
            CompletableFuture<U> b = other;
            Object r;
            Throwable x;
            Runnable f;
            
            if(fn == null
                || d == null || a == null || b == null
                || ((r = a.result) == null && (r = b.result) == null)) {
                return null;
            }
            
            if(d.result == null) {
                try {
                    if(mode<=0 && !claim()) {
                        return null;
                    }
                    
                    if(r instanceof AltResult && (x = ((AltResult) r).ex) != null) {
                        d.completeThrowable(x, r);
                    } else {
                        fn.run();
                        d.completeNull();
                    }
                } catch(Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            
            dep = null;
            src = null;
            other = null;
            fn = null;
            
            return d.postFire(a, b, mode);
        }
    }
    
    
    
    // 同步执行OrRun型任务
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return orRunStage(null, other, action);
    }
    
    // 使用【默认工作池】执行OrRun型任务
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return orRunStage(defaultExecutor(), other, action);
    }
    
    // 使用指定的【任务执行器】执行OrRun型任务
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return orRunStage(screenExecutor(executor), other, action);
    }
    
    
    // 执行OrRun型任务
    private CompletableFuture<Void> orRunStage(Executor e, CompletionStage<?> other, Runnable f) {
        CompletableFuture<?> b = other.toCompletableFuture();
        
        if(f == null || b == null) {
            throw new NullPointerException();
        }
        
        Object r;
        CompletableFuture<?> z;
        
        if((r = (z = this).result) != null
            || (r = (z = b).result) != null) {
            return z.uniRunNow(r, e, f);
        }
        
        CompletableFuture<Void> d = newIncompleteFuture();
        orpush(b, new OrRun<>(e, d, this, b, f));
        
        return d;
    }
    
    /*▲ OrRun ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniCompose ████████████████████████████████████████████████████████████████████████████████┓ */
    
    @SuppressWarnings("serial")
    static final class UniCompose<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends CompletionStage<V>> fn;
        
        UniCompose(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, Function<? super T, ? extends CompletionStage<V>> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }
        
        // 执行任务
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d = dep;
            CompletableFuture<T> a = src;
            
            Object r;
            
            if(fn == null || d == null || a == null || (r = a.result) == null) {
                return null;
            }

tryComplete:
            if(d.result == null) {
                if(r instanceof AltResult) {
                    Throwable x = ((AltResult) r).ex;
                    
                    if(x != null) {
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                    
                    r = null;
                }
                
                try {
                    if(mode<=0 && !claim()) {
                        return null;
                    }
                    
                    @SuppressWarnings("unchecked")
                    T t = (T) r;
                    
                    CompletableFuture<V> g = fn.apply(t).toCompletableFuture();
                    
                    if((r = g.result) != null) {
                        d.completeRelay(r);
                    } else {
                        UniRelay<V, V> uniRelay = new UniRelay<>(d, g);
                        g.unipush(uniRelay);
                        if(d.result == null) {
                            return null;
                        }
                    }
                } catch(Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            
            dep = null;
            src = null;
            fn = null;
            
            return d.postFire(a, mode);
        }
    }
    
    
    @SuppressWarnings("serial")
    static final class UniRelay<U, T extends U> extends UniCompletion<T, U> {
        UniRelay(CompletableFuture<U> dep, CompletableFuture<T> src) {
            super(null, dep, src);
        }
        
        // 执行任务
        final CompletableFuture<U> tryFire(int mode) {
            CompletableFuture<U> d = dep;
            CompletableFuture<T> a = src;
            
            if(d == null || a == null || a.result == null) {
                return null;
            }
            
            if(d.result == null) {
                d.completeRelay(a.result);
            }
            
            src = null;
            dep = null;
            
            return d.postFire(a, mode);
        }
    }
    
    
    // 同步执行UniCompose型任务
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return uniComposeStage(null, fn);
    }
    
    // 使用【默认工作池】执行UniCompose型任务
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return uniComposeStage(defaultExecutor(), fn);
    }
    
    // 使用指定的【任务执行器】执行UniCompose型任务
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return uniComposeStage(screenExecutor(executor), fn);
    }
    
    
    // 执行UniCompose型任务
    private <V> CompletableFuture<V> uniComposeStage(Executor e, Function<? super T, ? extends CompletionStage<V>> f) {
        if(f == null) {
            throw new NullPointerException();
        }
        
        CompletableFuture<V> d = newIncompleteFuture();
        
        Object r;
        
        if((r = result) == null) {
            UniCompose<T, V> uniCompose = new UniCompose<>(e, d, this, f);
            unipush(uniCompose);
        } else if(e == null) {
            if(r instanceof AltResult) {
                Throwable x = ((AltResult) r).ex;
                if(x != null) {
                    d.result = encodeThrowable(x, r);
                    return d;
                }
                
                r = null;
            }
            
            try {
                @SuppressWarnings("unchecked")
                T t = (T) r;
                
                CompletableFuture<V> g = f.apply(t).toCompletableFuture();
                
                if(g.result != null) {
                    d.result = encodeRelay(g.result);
                } else {
                    g.unipush(new UniRelay<V, V>(d, g));
                }
            } catch(Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        } else {
            try {
                e.execute(new UniCompose<T, V>(null, d, this, f));
            } catch(Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        
        return d;
    }
    
    /*▲ UniCompose ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniHandle ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （可以做枢纽）依赖一个上游阶段的任务，当前阶段的任务源是BiFunction，需要使用上游任务的执行结果和抛出的异常作为入参
    @SuppressWarnings("serial")
    static final class UniHandle<T, V> extends UniCompletion<T, V> {
        BiFunction<? super T, Throwable, ? extends V> fn;
        
        UniHandle(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, BiFunction<? super T, Throwable, ? extends V> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }
        
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d = dep;
            CompletableFuture<T> a = src;
            
            if(fn == null
                || d == null
                || a == null || a.result == null
                || !d.uniHandle(a.result, fn, mode>0 ? null : this)) {
                return null;
            }
            
            dep = null;
            src = null;
            fn = null;
            
            return d.postFire(a, mode);
        }
    }
    
    
    
    // 同步执行UniHandle型任务
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return uniHandleStage(null, fn);
    }
    
    // 使用【默认工作池】执行UniHandle型任务
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return uniHandleStage(defaultExecutor(), fn);
    }
    
    // 使用指定的【任务执行器】执行UniHandle型任务
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return uniHandleStage(screenExecutor(executor), fn);
    }
    
    
    
    // 执行UniHandle型任务
    private <V> CompletableFuture<V> uniHandleStage(Executor e, BiFunction<? super T, Throwable, ? extends V> f) {
        if(f == null) {
            throw new NullPointerException();
        }
        
        CompletableFuture<V> d = newIncompleteFuture();
        
        if(result == null) {
            unipush(new UniHandle<T, V>(e, d, this, f));
        } else if(e == null) {
            d.uniHandle(result, f, null);
        } else {
            try {
                e.execute(new UniHandle<T, V>(null, d, this, f));
            } catch(Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        
        return d;
    }
    
    final <S> boolean uniHandle(Object r, BiFunction<? super S, Throwable, ? extends T> f, UniHandle<S, T> c) {
        S s;
        Throwable x;
        if(result == null) {
            try {
                if(c != null && !c.claim()) {
                    return false;
                }
                
                if(r instanceof AltResult) {
                    x = ((AltResult) r).ex;
                    s = null;
                } else {
                    x = null;
                    @SuppressWarnings("unchecked")
                    S ss = (S) r;
                    s = ss;
                }
                completeValue(f.apply(s, x));
            } catch(Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }
    
    /*▲ UniHandle ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ UniWhenComplete ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // （可以做枢纽）依赖一个上游阶段的任务，当前阶段的任务源是BiConsumer，需要使用上游任务的执行结果和抛出的异常作为入参
    @SuppressWarnings("serial")
    static final class UniWhenComplete<T> extends UniCompletion<T, T> {
        BiConsumer<? super T, ? super Throwable> fn;
        
        UniWhenComplete(Executor executor, CompletableFuture<T> dep, CompletableFuture<T> src, BiConsumer<? super T, ? super Throwable> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }
        
        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> d = dep;
            CompletableFuture<T> a = src;
            Object r;
            
            if(fn == null
                || d == null
                || a == null || a.result == null
                || !d.uniWhenComplete(a.result, fn, mode>0 ? null : this)) {
                return null;
            }
            
            dep = null;
            src = null;
            fn = null;
            
            return d.postFire(a, mode);
        }
    }
    
    
    
    // 同步执行UniWhenComplete型任务
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(null, action);
    }
    
    // 使用【默认工作池】执行UniWhenComplete型任务
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(defaultExecutor(), action);
    }
    
    // 使用指定的【任务执行器】执行UniWhenComplete型任务
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return uniWhenCompleteStage(screenExecutor(executor), action);
    }
    
    
    // 执行UniWhenComplete型任务
    private CompletableFuture<T> uniWhenCompleteStage(Executor e, BiConsumer<? super T, ? super Throwable> f) {
        if(f == null) {
            throw new NullPointerException();
        }
        
        CompletableFuture<T> d = newIncompleteFuture();
        Object r;
        
        if((r = result) == null) {
            unipush(new UniWhenComplete<T>(e, d, this, f));
        } else if(e == null) {
            d.uniWhenComplete(r, f, null);
        } else {
            try {
                e.execute(new UniWhenComplete<T>(null, d, this, f));
            } catch(Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        return d;
    }
    
    final boolean uniWhenComplete(Object r, BiConsumer<? super T, ? super Throwable> fn, UniWhenComplete<T> c) {
        T t;
        
        Throwable x = null;
        
        if(result == null) {
            try {
                if(c != null && !c.claim())
                    return false;
                if(r instanceof CompletableFuture.AltResult) {
                    x = ((AltResult) r).ex;
                    t = null;
                } else {
                    @SuppressWarnings("unchecked")
                    T tr = (T) r;
                    t = tr;
                }
                
                fn.accept(t, x);
                
                if(x == null) {
                    internalComplete(r);
                    return true;
                }
            } catch(Throwable ex) {
                if(x == null) {
                    x = ex;
                } else if(x != ex) {
                    x.addSuppressed(ex);
                }
            }
            
            completeThrowable(x, r);
        }
        return true;
    }
    
    /*▲ UniWhenComplete ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ BiRelay ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * （可以做枢纽）依赖多个上游阶段的任务，等待全部上游任务完成的过程中，会收集上游任务中异常信息（中途遇到异常不会停下来，也不会抛出来）
     * 在收集过程中，谁在入参中排在前面收集谁的信息
     */
    @SuppressWarnings("serial")
    static final class BiRelay<T, U> extends BiCompletion<T, U, Void> { // for And
        BiRelay(CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> other) {
            super(null, dep, src, other);
        }
        
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<T> a = src;
            CompletableFuture<U> b = other;
            CompletableFuture<Void> d = dep;
            Object r, s, z;
            Throwable x;
            
            if(d == null || a == null || a.result == null || b == null || b.result == null) {
                return null;
            }
            
            if(d.result == null) {
                if((a.result instanceof AltResult && (x = ((AltResult) (z = a.result)).ex) != null)
                    || (b.result instanceof AltResult && (x = ((AltResult) (z = b.result)).ex) != null)) {
                    d.completeThrowable(x, z);
                } else {
                    d.completeNull();
                }
            }
            
            src = null;
            other = null;
            dep = null;
            
            return d.postFire(a, b, mode);
        }
    }
    
    
    
    // 执行参数中指定的所有任务，直到全部任务执行完之后返回结果
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
    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        return andTree(cfs, 0, cfs.length - 1);
    }
    
    /** Recursively constructs a tree of completions. */
    static CompletableFuture<Void> andTree(CompletableFuture<?>[] cfs, int lo, int hi) {
        CompletableFuture<Void> d = new CompletableFuture<>();
        
        if(lo>hi) {
            // empty
            d.result = NIL;
        } else {
            CompletableFuture<?> a, b;
            Object z;
            Throwable x;
            int mid = (lo + hi) >>> 1;
            
            if((a = (lo == mid ? cfs[lo] : andTree(cfs, lo, mid))) == null
                || (b = (lo == hi ? a : (hi == mid + 1) ? cfs[hi] : andTree(cfs, mid + 1, hi))) == null) {
                throw new NullPointerException();
            }
            
            if(a.result == null || b.result == null) {
                a.bipush(b, new BiRelay<>(d, a, b));
            } else if((a.result instanceof AltResult && (x = ((AltResult) (z = a.result)).ex) != null)
                || (b.result instanceof AltResult && (x = ((AltResult) (z = b.result)).ex) != null)) {
                d.result = encodeThrowable(x, z);
            } else {
                d.result = NIL;
            }
        }
        return d;
    }
    
    /*▲ BiRelay ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ AnyOf ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Completion for an anyOf input future. */
    /*
     * （可以做枢纽）依赖多个上游阶段的任务，等待任一上游任务完成的过程中，会收集上游任务中抛出的异常信息和返回值（中途遇到异常不会停下来，也不会抛出来）
     * 在收集过程中，谁先执行完收集谁的信息
     */
    @SuppressWarnings("serial")
    static class AnyOf extends Completion {
        CompletableFuture<Object> dep;
        CompletableFuture<?> src;
        CompletableFuture<?>[] srcs;
        
        AnyOf(CompletableFuture<Object> dep, CompletableFuture<?> src, CompletableFuture<?>[] srcs) {
            this.dep = dep;
            this.src = src;
            this.srcs = srcs;
        }
        
        final CompletableFuture<Object> tryFire(int mode) {
            // assert mode != ASYNC;
            CompletableFuture<Object> d;
            CompletableFuture<?> a;
            CompletableFuture<?>[] as;
            Object r;
            
            if((d = dep) == null
                || (a = src) == null
                || (r = a.result) == null
                || (as = srcs) == null)
                return null;
            dep = null;
            src = null;
            srcs = null;
            
            if(d.completeRelay(r)) {
                for(CompletableFuture<?> b : as) {
                    if(b != a) {
                        b.cleanStack();
                    }
                }
                
                if(mode<0) {
                    return d;
                } else {
                    d.postComplete();
                }
            }
            return null;
        }
        
        final boolean isLive() {
            CompletableFuture<Object> d;
            return (d = dep) != null && d.result == null;
        }
    }
    
    
    
    // 执行参数中指定的所有任务，直到其中任一任务执行完之后返回结果
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
    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs) {
        if(cfs.length<=1) {
            return (cfs.length == 0) ? new CompletableFuture<Object>() : uniCopyStage(cfs[0]);
        }
        
        for(CompletableFuture<?> cf : cfs) {
            if(cf.result != null) {
                Object relay = encodeRelay(cf.result);
                return new CompletableFuture<Object>(relay);
            }
        }
        
        cfs = cfs.clone();
        
        CompletableFuture<Object> d = new CompletableFuture<>();
        for(CompletableFuture<?> cf : cfs) {
            cf.unipush(new AnyOf(d, cf, cfs));
        }
        
        // If d was completed while we were adding completions, we should
        // clean the stack of any sources that may have had completions
        // pushed on their stack after d was completed.
        if(d.result != null) {
            for(int i = 0, len = cfs.length; i<len; i++) {
                if(cfs[i].result != null) {
                    for(i++; i<len; i++) {
                        if(cfs[i].result == null) {
                            cfs[i].cleanStack();
                        }
                    }
                }
            }
        }
        
        return d;
    }
    
    /*▲ AnyOf ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置任务结果 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Completes with the null value, unless already completed. */
    // 设置任务结果（无返回值）
    final boolean completeNull() {
        return RESULT.compareAndSet(this, null, NIL);
    }
    
    /** Completes with a non-exceptional result, unless already completed. */
    // 设置任务结果（有返回值）
    final boolean completeValue(T t) {
        return RESULT.compareAndSet(this, null, encodeValue(t));
    }
    
    /** Completes with an exceptional result, unless already completed. */
    // 设置任务结果（抛异常）
    final boolean completeThrowable(Throwable x) {
        return RESULT.compareAndSet(this, null, encodeThrowable(x));
    }
    
    /**
     * Completes with the given (non-null) exceptional result as a
     * wrapped CompletionException unless it is one already, unless
     * already completed.  May complete with the given Object r
     * (which must have been the result of a source future) if it is
     * equivalent, i.e. if this is a simple propagation of an
     * existing CompletionException.
     */
    // 设置任务结果（抛异常），参数r是备用信息
    final boolean completeThrowable(Throwable x, Object r) {
        return RESULT.compareAndSet(this, null, encodeThrowable(x, r));
    }
    
    /**
     * Completes with r or a copy of r, unless already completed.
     * If exceptional, r is first coerced to a CompletionException.
     */
    // 设置任务结果（响应信息，可能是返回值，也可能是异常）
    final boolean completeRelay(Object r) {
        return RESULT.compareAndSet(this, null, encodeRelay(r));
    }
    
    
    /** Returns the encoding of the given non-exceptional value. */
    // 编码返回值
    final Object encodeValue(T t) {
        return (t == null) ? NIL : t;
    }
    
    /**
     * Returns the encoding of the given (non-null) exception as a wrapped CompletionException unless it is one already.
     */
    // 编码异常
    static AltResult encodeThrowable(Throwable x) {
        // 将给定的异常编码为CompletionException类型，然后存入AltResult
        return new AltResult((x instanceof CompletionException) ? x : new CompletionException(x));
    }
    
    /**
     * Returns the encoding of the given (non-null) exception as a
     * wrapped CompletionException unless it is one already.  May
     * return the given Object r (which must have been the result of a
     * source future) if it is equivalent, i.e. if this is a simple
     * relay of an existing CompletionException.
     */
    // 编码异常，参数r是备用信息
    static Object encodeThrowable(Throwable x, Object r) {
        /* 将给定的异常编码为CompletionException类型，然后存入AltResult。参数r是备用信息 */
        
        if(!(x instanceof CompletionException)) {
            x = new CompletionException(x);
        } else if(r instanceof AltResult && x == ((AltResult) r).ex) {
            return r;
        }
        
        return new AltResult(x);
    }
    
    /**
     * Returns the encoding of a copied outcome; if exceptional,
     * rewraps as a CompletionException, else returns argument.
     */
    // 编码响应信息（返回值/异常）
    static Object encodeRelay(Object r) {
        Throwable x;
        
        if(r instanceof AltResult && (x = ((AltResult) r).ex) != null && !(x instanceof CompletionException)) {
            r = new AltResult(new CompletionException(x));
        }
        
        return r;
    }
    
    /**
     * Returns the encoding of the given arguments: if the exception
     * is non-null, encodes as AltResult.  Otherwise uses the given
     * value, boxed as NIL if null.
     */
    // 编码返回值/异常
    Object encodeOutcome(T t, Throwable x) {
        return (x == null)
            ? (t == null) ? NIL : t
            : encodeThrowable(x);
    }
    
    
    /**
     * Reports result using Future.get conventions.
     */
    // 报告任务结果（r不可以为null）
    private static Object reportGet(Object r) throws InterruptedException, ExecutionException {
        if(r == null) {
            // by convention below, null means interrupted
            throw new InterruptedException();
        }
        
        if(r instanceof AltResult) {
            Throwable x = ((AltResult) r).ex;
            
            if(x == null) {
                return null;
            }
            
            if(x instanceof CancellationException) {
                throw (CancellationException) x;
            }
            
            if(x instanceof CompletionException) {
                Throwable cause = x.getCause();
                if(cause != null){
                    x = cause;
                }
            }
            
            throw new ExecutionException(x);
        }
        
        return r;
    }
    
    /**
     * Decodes outcome to return result or throw unchecked exception.
     */
    // 报告任务结果（r可以为null）
    private static Object reportJoin(Object r) {
        if(r instanceof AltResult) {
            Throwable x = ((AltResult) r).ex;
            
            if(x == null) {
                return null;
            }
            
            if(x instanceof CancellationException) {
                throw (CancellationException) x;
            }
            
            if(x instanceof CompletionException) {
                throw (CompletionException) x;
            }
            
            throw new CompletionException(x);
        }
        
        return r;
    }
    
    
    // 任务结果（用于包装void，null，异常这三种类型的结果）
    static final class AltResult {
        /**
         * 对于抛异常任务，此处存储异常信息
         * 对于无返回值或返回值为null的任务，此处存储null
         */
        final Throwable ex;        // null only for NIL
        
        AltResult(Throwable x) {
            this.ex = x;
        }
    }
    
    /*▲ 设置任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
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
    // 获取任务结果，如果还没有结果，则阻塞等待（考虑线程中断）
    @SuppressWarnings("unchecked")
    public T get() throws InterruptedException, ExecutionException {
        Object r = result;
        
        if(r==null) {
            r = waitingGet(true);
        }
        
        // 报告任务结果（r不可以为null）
        return (T) reportGet(r);
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
    // 获取任务结果，如果还没有结果，则阻塞等待（不考虑线程中断）
    @SuppressWarnings("unchecked")
    public T join() {
        Object r = result;
        
        if(r==null) {
            r = waitingGet(false);
        }
        
        // 报告任务结果（r可以为null）
        return (T) reportJoin(r);
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
    // 获取任务结果，如果还没有结果，则阻塞等待一段时间，等待时长由超时设置决定
    @SuppressWarnings("unchecked")
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long nanos = unit.toNanos(timeout);
        
        Object r = result;
        
        if(r == null) {
            r = timedGet(nanos);
        }
        
        // 报告任务结果（r不可以为null）
        return (T) reportGet(r);
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
    // 立即返回任务结果，参数valueIfAbsent是备用值
    @SuppressWarnings("unchecked")
    public T getNow(T valueIfAbsent) {
        Object r = result;
        
        // 如果当前任务还未执行完，则返回备用值
        if(r==null) {
            return valueIfAbsent;
        }
        
        // 报告任务结果（r可以为null）
        return (T) reportJoin(r);
    }
    
    /**
     * Returns raw result after waiting, or null if interruptible and interrupted.
     */
    // 获取任务结果，如果任务未完成，需要阻塞当前线程
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
                
                // 如果当前任务还未进入枢纽栈
            } else if(!queued) {
                // 将signaller加入到枢纽栈栈顶
                queued = tryPushStack(signaller);
            } else {
                try {
                    // 管理阻塞块（决定当前线程是否需要阻塞）
                    ForkJoinPool.managedBlock(signaller);
                } catch(InterruptedException ie) { // currently cannot happen
                    signaller.interrupted = true;
                }
                
                // 如果线程带有中断标记，且需要支持中断，则跳出循环，进行中断操作
                if(signaller.interrupted && interruptible) {
                    break;
                }
            }
        }
        
        // 如果当前任务在枢纽栈中排队
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
                // 清理枢纽栈
                cleanStack();
            }
        }
        
        if(r != null || (r = result) != null) {
            postComplete();
        }
        
        return r;
    }
    
    /**
     * Returns raw result after waiting, or null if interrupted, or
     * throws TimeoutException on timeout.
     */
    // 获取任务结果，如果任务未完成，需要阻塞当前线程一段时间
    private Object timedGet(long nanos) throws TimeoutException {
        if(Thread.interrupted()) {
            return null;
        }
        
        if(nanos<=0){
            throw new TimeoutException();
        }
        
        long d = System.nanoTime() + nanos;
        long deadline = (d == 0L) ? 1L : d; // avoid 0
        Signaller signaller = null;
        boolean queued = false;
        Object r;
        
        while((r = result) == null) { // similar to untimed
            if(signaller == null) {
                signaller = new Signaller(true, nanos, deadline);
                if(Thread.currentThread() instanceof ForkJoinWorkerThread) {
                    // 尝试使用signaller当前所在线程加速指定的【工作池】中的任务完成
                    ForkJoinPool.helpAsyncBlocker(defaultExecutor(), signaller);
                }
            } else if(!queued) {
                // 将signaller加入到枢纽栈栈顶
                queued = tryPushStack(signaller);
            } else if(signaller.nanos<=0L) {
                break;
            } else {
                try {
                    ForkJoinPool.managedBlock(signaller);
                } catch(InterruptedException ie) {
                    signaller.interrupted = true;
                }
                
                if(signaller.interrupted) {
                    break;
                }
            }
        }
        
        if(signaller != null && queued) {
            signaller.thread = null;
            if(r == null) {
                cleanStack();
            }
        }
        
        if(r != null || (r = result) != null) {
            postComplete();
        }
        
        if(r != null || (signaller != null && signaller.interrupted)) {
            return r;
        }
        
        throw new TimeoutException();
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
            if(interrupted && interruptible){
                return true;
            }
            
            // 如果启用了超时设置
            if(deadline != 0L){
                // 如果已经超时，表示可以解除阻塞了
                if(nanos<=0L){
                    return true;
                }
                
                // 计算剩余阻塞时间
                nanos = deadline - System.nanoTime();
                
                // 如果已经超时，表示可以解除阻塞了
                if(nanos<=0L){
                    return true;
                }
            }
            
            /* 至此，说明未启用超时设置（一直阻塞），或者还未超时 */
            
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
    
    /*▲ 获取任务结果 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 枢纽 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Returns true if successfully pushed c onto stack. */
    // 枢纽入栈（头插法），入栈失败则返回false
    final boolean tryPushStack(Completion completion) {
        Completion h = stack;
        NEXT.set(completion, h);         // CAS piggyback
        return STACK.compareAndSet(this, h, completion);
    }
    
    /** Unconditionally pushes c onto stack, retrying if necessary. */
    // 枢纽入栈，失败则重试
    final void pushStack(Completion c) {
        do {
        } while(!tryPushStack(c));
    }
    
    /** Traverses stack and unlinks one or more dead Completions, if found. */
    // 清理枢纽栈中首个已被处理过的枢纽
    final void cleanStack() {
        Completion p = stack;
        
        // 如果栈顶枢纽已被处理，则清理它
        for(boolean unlinked = false; ; ) {
            // 枢纽栈已经为空
            if(p == null) {
                return;
            }
            
            // 枢纽等待处理
            if(p.isLive()) {
                if(unlinked) {
                    return;
                }
                
                break;
                
                // 将已处理过的枢纽出栈
            } else if(STACK.weakCompareAndSet(this, p, (p = p.next))) {
                unlinked = true;
            } else {
                p = stack;
            }
        }
        
        // 清理栈中首个已处理过的枢纽
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
     * Pops and tries to trigger all reachable dependents.  Call only when known to be done.
     */
    /*
     * 当前阶段的任务完成后，处理该阶段的枢纽栈
     *
     * 当下游阶段依赖上游阶段时，如果上游阶段尚未包含执行结果，那么上游阶段的任务会进入枢纽栈等待执行
     * 当上游阶段的任务执行完成后，它会去检查枢纽栈，以处理待执行的下游任务
     *
     * 这里的处理过程很繁琐，但整体思想是采取了类似图中的深度遍历算法与类似ForkJoinPool中的工作窃取算法
     */
    final void postComplete() {
        /*
         * On each step, variable f holds current dependents to pop and run.
         * It is extended along only one path at a time, pushing others to avoid unbounded recursion.
         */
        
        CompletableFuture<?> f = this;
        
        while(true) {
            // 指向枢纽栈首个元素
            Completion h = f.stack;
            
            // 如果枢纽栈为空
            if(h==null){
                if(f!=this){
                    f = this;
                    h = f.stack;
                    // 当前阶段的枢纽栈已经为null
                    if(h==null){
                        return;
                    }
                } else {
                    // 当前枢纽栈为null
                    return;
                }
            }
            
            // 指向栈中下一个元素
            Completion t = h.next;
            
            // 更新栈顶游标指向下一个枢纽
            if(STACK.compareAndSet(f, h, t)) {
                // 如果栈中仍有元素
                if(t != null) {
                    if(f != this) {
                        // 将别处的枢纽“嫁接”到当前阶段的枢纽栈中
                        pushStack(h);
                        continue;
                    }
                    
                    // 将栈顶元素与枢纽栈分离
                    NEXT.compareAndSet(h, t, null); // try to detach
                }
                
                // 处理栈顶的枢纽元素（执行下游任务）
                CompletableFuture<?> d = h.tryFire(NESTED);
                
                // d不为null，意味着下游任务枢纽依然联结着下一个任务阶段
                f = (d==null) ? this : d;
            }
        }
    }
    
    /**
     * Post-processing by dependent after successful UniCompletion tryFire.
     * Tries to clean stack of source a, and then either runs postComplete
     * or returns this to caller, depending on mode.
     */
    // 下游阶段的任务执行完之后，先检查上游阶段的枢纽栈，再检查当前阶段的枢纽栈
    final CompletableFuture<T> postFire(CompletableFuture<?> a, int mode) {
        // 上游阶段的枢纽栈不为空
        if(a != null && a.stack != null) {
            Object r = a.result;
            
            if(r == null) {
                a.cleanStack();
            }
            
            // 处理模式是SYNC或ASYNC
            if(mode >= 0 && (r != null || a.result != null)) {
                a.postComplete();
            }
        }
        
        // 依赖阶段已有任务结果，且依赖阶段的枢纽栈不为空
        if(result != null && stack != null) {
            // NESTED模式
            if(mode<0) {
                return this;
            }
            
            // 处理依赖阶段的枢纽栈
            postComplete();
        }
        
        return null;
    }
    
    /** Post-processing after successful BiCompletion tryFire. */
    final CompletableFuture<T> postFire(CompletableFuture<?> a, CompletableFuture<?> b, int mode) {
        if(b != null && b.stack != null) { // clean second source
            Object r = b.result;
            
            if(r == null) {
                b.cleanStack();
            }
            
            if(mode >= 0 && (r != null || b.result != null)) {
                b.postComplete();
            }
        }
        
        return postFire(a, mode);
    }
    
    
    
    
    /**
     * A marker interface identifying asynchronous tasks produced by
     * {@code async} methods. This may be useful for monitoring,
     * debugging, and tracking asynchronous activities.
     *
     * @since 1.8
     */
    public interface AsynchronousCompletionTask {
    }
    
    @SuppressWarnings("serial")
    abstract static class Completion extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        // 链接下一个Completion
        volatile Completion next;      // Treiber stack link
        
        // 异步执行任务
        public final boolean exec() {
            tryFire(ASYNC);
            return false;
        }
        
        // 异步执行任务
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
        // 执行任务（枢纽类型）
        abstract CompletableFuture<?> tryFire(int mode);
        
        /** Returns true if possibly still triggerable. Used by cleanStack. */
        // 枢纽是否正在等待处理
        abstract boolean isLive();
    }
    
    /** A Completion with a source, dependent, and executor. */
    @SuppressWarnings("serial")
    abstract static class UniCompletion<T, V> extends Completion {
        Executor executor;                 // executor to use (null if none)
        CompletableFuture<V> dep;          // the dependent to complete
        CompletableFuture<T> src;          // source for action
        
        UniCompletion(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src) {
            this.executor = executor;
            this.dep = dep;
            this.src = src;
        }
        
        /**
         * Returns true if action can be run.
         * Call only when known to be triggerable.
         * Uses FJ tag bit to ensure that only one thread claims ownership.
         * If async, starts as task -- a later call to tryFire will run action.
         */
        /*
         * 尝试异步执行当前任务，如果成功，则会辗转调用到tryFire(ASYNC)
         * 返回true表示当前线程锁定该任务，且该任务需要同步执行
         */
        final boolean claim() {
            Executor e = executor;
            
            // 更新任务状态，确保只有一个任务拥有所有权
            if(compareAndSetForkJoinTaskTag((short) 0, (short) 1)) {
                if(e == null) {
                    return true;
                }
                
                executor = null; // disable
                
                // 用指定的【任务执行器】在异步模式下执行
                e.execute(this);
            }
            
            return false;
        }
        
        final boolean isLive() {
            return dep != null;
        }
    }
    
    /** A Completion for an action with two sources */
    @SuppressWarnings("serial")
    abstract static class BiCompletion<T, U, V> extends UniCompletion<T, V> {
        CompletableFuture<U> other; // second source for action
        
        BiCompletion(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> other) {
            super(executor, dep, src);
            this.other = other;
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
            CompletableFuture<?> d;
            
            if(base == null || (d = base.tryFire(mode)) == null) {
                return null;
            }
            
            base = null; // detach
            
            return d;
        }
        
        final boolean isLive() {
            BiCompletion<?, ?, ?> c;
            
            return (c = base) != null
                // && c.isLive()
                && c.dep != null;
        }
    }
    
    /*▲ 枢纽 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new CompletableFuture that is already completed with the given value.
     *
     * @param value the value
     * @param <U>   the type of the value
     *
     * @return the completed CompletableFuture
     */
    public static <U> CompletableFuture<U> completedFuture(U value) {
        return new CompletableFuture<U>((value == null) ? NIL : value);
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
    // 创建一个残缺的阶段，残缺的含义是当前阶段的执行需要依赖其他阶段
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CompletableFuture<U>();
    }
    
    /**
     * Returns this CompletableFuture.
     *
     * @return this CompletableFuture
     */
    public CompletableFuture<T> toCompletableFuture() {
        return this;
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
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return uniExceptionallyStage(fn);
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
    public CompletableFuture<T> copy() {
        return uniCopyStage(this);
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
    public static <U> CompletionStage<U> completedStage(U value) {
        return new MinimalStage<U>((value == null) ? NIL : value);
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
    public static <U> CompletionStage<U> failedStage(Throwable ex) {
        if(ex == null) {
            throw new NullPointerException();
        }
        
        return new MinimalStage<U>(new AltResult(ex));
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
    public CompletionStage<T> minimalCompletionStage() {
        return uniAsMinimalStage();
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
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        return completeAsync(supplier, defaultExecutor());
    }
    
    /**
     * Exceptionally completes this CompletableFuture with
     * a {@link TimeoutException} if not otherwise completed
     * before the given timeout.
     *
     * @param timeout how long to wait before completing exceptionally
     *                with a TimeoutException, in units of {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     *
     * @return this CompletableFuture
     *
     * @since 9
     */
    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        if(unit == null) {
            throw new NullPointerException();
        }
        
        if(result == null) {
            Canceller canceller = new Canceller(Delayer.delay(new Timeout(this), timeout, unit));
            whenComplete(canceller);
        }
        
        return this;
    }
    
    /**
     * Completes this CompletableFuture with the given value if not
     * otherwise completed before the given timeout.
     *
     * @param value   the value to use upon timeout
     * @param timeout how long to wait before completing normally
     *                with the given value, in units of {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     *
     * @return this CompletableFuture
     *
     * @since 9
     */
    public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
        if(unit == null) {
            throw new NullPointerException();
        }
        
        if(result == null) {
            DelayedCompleter<T> delayedCompleter = new DelayedCompleter<>(this, value);
            ScheduledFuture<?> scheduledFuture = Delayer.delay(delayedCompleter, timeout, unit);
            Canceller canceller = new Canceller(scheduledFuture);
            whenComplete(canceller);
        }
        
        return this;
    }
    
    private CompletableFuture<T> uniExceptionallyStage(Function<Throwable, ? extends T> f) {
        if(f == null) {
            throw new NullPointerException();
        }
        
        CompletableFuture<T> d = newIncompleteFuture();
        Object r;
        if((r = result) == null) {
            unipush(new UniExceptionally<T>(d, this, f));
        } else {
            d.uniExceptionally(r, f, null);
        }
        
        return d;
    }
    
    private static <U, T extends U> CompletableFuture<U> uniCopyStage(CompletableFuture<T> src) {
        CompletableFuture<U> d = src.newIncompleteFuture();
        
        if(src.result != null) {
            d.result = encodeRelay(src.result);
        } else {
            src.unipush(new UniRelay<U, T>(d, src));
        }
        return d;
    }
    
    private MinimalStage<T> uniAsMinimalStage() {
        Object r;
        
        if((r = result) != null) {
            return new MinimalStage<T>(encodeRelay(r));
        }
        
        MinimalStage<T> d = new MinimalStage<T>();
        unipush(new UniRelay<T, T>(d, this));
        return d;
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
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
        if(supplier == null || executor == null) {
            throw new NullPointerException();
        }
        
        executor.execute(new AsyncSupply<T>(this, supplier));
        
        return this;
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 阶段状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if completed in any fashion: normally,
     * exceptionally, or via cancellation.
     *
     * @return {@code true} if completed
     */
    // 判断当前阶段任务是否已完成（是否有了执行结果）
    public boolean isDone() {
        return result != null;
    }
    
    /**
     * If not already completed, sets the value returned by {@link
     * #get()} and related methods to the given value.
     *
     * @param value the result value
     *
     * @return {@code true} if this invocation caused this CompletableFuture
     * to transition to a completed state, else {@code false}
     */
    // 如果任务还未完成，将其结果设置为指定的值，并处理该阶段的枢纽栈
    public boolean complete(T value) {
        boolean triggered = completeValue(value);
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
    // 如果任务还未完成，将其结果设置为指定的异常，并处理该阶段的枢纽栈
    public boolean completeExceptionally(Throwable ex) {
        if(ex == null) {
            throw new NullPointerException();
        }
        boolean triggered = internalComplete(new AltResult(ex));
        postComplete();
        return triggered;
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
    // 取消任务
    public boolean cancel(boolean mayInterruptIfRunning) {
        CancellationException exception = new CancellationException();
        
        AltResult altResult = new AltResult(exception);
        
        boolean cancelled = (result == null) && internalComplete(altResult);
        
        postComplete();
        
        return cancelled || isCancelled();
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
     * Returns {@code true} if this CompletableFuture completed
     * exceptionally, in any way. Possible causes include
     * cancellation, explicit invocation of {@code
     * completeExceptionally}, and abrupt termination of a
     * CompletionStage action.
     *
     * @return {@code true} if this CompletableFuture completed exceptionally
     */
    // 判断当前任务是否在特殊状态下完成（出现了异常）
    public boolean isCompletedExceptionally() {
        return (result instanceof AltResult) && result != NIL;
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
    // 强制设置结果
    public void obtrudeValue(T value) {
        result = (value == null) ? NIL : value;
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
    // 强制设置异常结果
    public void obtrudeException(Throwable ex) {
        if(ex == null) {
            throw new NullPointerException();
        }
        
        result = new AltResult(ex);
        
        postComplete();
    }
    
    /**
     * Returns the estimated number of CompletableFutures whose
     * completions are awaiting completion of this CompletableFuture.
     * This method is designed for use in monitoring system state, not
     * for synchronization control.
     *
     * @return the number of dependent CompletableFutures
     */
    // 获取当前阶段枢纽栈中的枢纽数量
    public int getNumberOfDependents() {
        int count = 0;
        
        for(Completion p = stack; p != null; p = p.next) {
            ++count;
        }
        
        return count;
    }
    
    
    // 更新当前阶段的任务结果为r
    final boolean internalComplete(Object r) { // CAS from null to r
        return RESULT.compareAndSet(this, null, r);
    }
    
    /*▲ 阶段状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ （一次性的）定时任务 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    public static Executor delayedExecutor(long delay, TimeUnit unit) {
        if(unit == null) {
            throw new NullPointerException();
        }
        
        return new DelayedExecutor(delay, unit, ASYNC_POOL);
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
    public static Executor delayedExecutor(long delay, TimeUnit unit, Executor executor) {
        if(unit == null || executor == null) {
            throw new NullPointerException();
        }
        
        return new DelayedExecutor(delay, unit, executor);
    }
    
    /*▲ （一次性的）定时任务 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
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
    // 返回默认的工作池（【共享工作池】）
    public Executor defaultExecutor() {
        return ASYNC_POOL;
    }
    
    /**
     * Null-checks user executor argument, and translates uses of commonPool to ASYNC_POOL in case parallelism disabled.
     */
    // 非空检查
    static Executor screenExecutor(Executor e) {
        if(!USE_COMMON_POOL && e == ForkJoinPool.commonPool()) {
            // 纠正Executor
            return ASYNC_POOL;
        }
        
        if(e == null) {
            throw new NullPointerException();
        }
        
        return e;
    }
    
    final boolean uniExceptionally(Object r, Function<? super Throwable, ? extends T> f, UniExceptionally<T> c) {
        Throwable x;
        if(result == null) {
            try {
                if(r instanceof AltResult && (x = ((AltResult) r).ex) != null) {
                    if(c != null && !c.claim()) {
                        return false;
                    }
                    
                    T t = f.apply(x);
                    
                    completeValue(t);
                } else {
                    internalComplete(r);
                }
            } catch(Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }
    
    
    
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
            : "[Not completed, " + count + " dependents]")
            : (((result instanceof AltResult) && ((AltResult) result).ex != null)
            ? "[Completed exceptionally: " + ((AltResult) result).ex + "]"
            : "[Completed normally]"));
    }
    
    
    
    
    
    
    /** Fallback if ForkJoinPool.commonPool() cannot support parallelism */
    static final class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }
    
    @SuppressWarnings("serial")
    static final class UniExceptionally<T> extends UniCompletion<T, T> {
        Function<? super Throwable, ? extends T> fn;
        
        UniExceptionally(CompletableFuture<T> dep, CompletableFuture<T> src, Function<? super Throwable, ? extends T> fn) {
            super(null, dep, src);
            this.fn = fn;
        }
        
        final CompletableFuture<T> tryFire(int mode) {
            //            assert mode != ASYNC;    // never ASYNC
            
            CompletableFuture<T> d = dep;
            CompletableFuture<T> a = src;
            Object r;
            
            if(fn == null
                || d == null
                || a == null || a.result == null
                || !d.uniExceptionally(a.result, fn, this)) {
                return null;
            }
            
            dep = null;
            src = null;
            fn = null;
            
            return d.postFire(a, mode);
        }
    }
    
    static final class DelayedExecutor implements Executor {
        final long delay;
        final TimeUnit unit;
        final Executor executor;
        
        DelayedExecutor(long delay, TimeUnit unit, Executor executor) {
            this.delay = delay;
            this.unit = unit;
            this.executor = executor;
        }
        
        public void execute(Runnable r) {
            Delayer.delay(new TaskSubmitter(executor, r), delay, unit);
        }
    }
    
    /**
     * Singleton delay scheduler, used only for starting and
     * cancelling tasks.
     */
    static final class Delayer {
        static final ScheduledThreadPoolExecutor delayer;
        
        static {
            DaemonThreadFactory threadFactory = new DaemonThreadFactory();
            delayer = new ScheduledThreadPoolExecutor(1, threadFactory);
            delayer.setRemoveOnCancelPolicy(true);
        }
        
        // 执行一次性的定时任务(Runnable)
        static ScheduledFuture<?> delay(Runnable command, long delay, TimeUnit unit) {
            return delayer.schedule(command, delay, unit);
        }
        
        static final class DaemonThreadFactory implements ThreadFactory {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("CompletableFutureDelayScheduler");
                return t;
            }
        }
    }
    
    /** Action to submit user task */
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
    
    /** Action to completeExceptionally on timeout */
    static final class Timeout implements Runnable {
        final CompletableFuture<?> f;
        
        Timeout(CompletableFuture<?> f) {
            this.f = f;
        }
        
        public void run() {
            if(f != null && !f.isDone()) {
                f.completeExceptionally(new TimeoutException());
            }
        }
    }
    
    /** Action to complete on timeout */
    static final class DelayedCompleter<U> implements Runnable {
        final CompletableFuture<U> f;
        final U u;
        
        DelayedCompleter(CompletableFuture<U> f, U u) {
            this.f = f;
            this.u = u;
        }
        
        public void run() {
            if(f != null)
                f.complete(u);
        }
    }
    
    /** Action to cancel unneeded timeouts */
    static final class Canceller implements BiConsumer<Object, Throwable> {
        final Future<?> f;
        
        Canceller(Future<?> f) {
            this.f = f;
        }
        
        public void accept(Object ignore, Throwable ex) {
            if(ex == null && f != null && !f.isDone()) {
                f.cancel(false);
            }
        }
    }
    
    /**
     * A subclass that just throws UOE for most non-CompletionStage methods.
     */
    static final class MinimalStage<T> extends CompletableFuture<T> {
        MinimalStage() {
        }
        
        MinimalStage(Object r) {
            super(r);
        }
        
        @Override
        public <U> CompletableFuture<U> newIncompleteFuture() {
            return new MinimalStage<U>();
        }
        
        @Override
        public T get() {
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
        
        @Override
        public T join() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean complete(T value) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean completeExceptionally(Throwable ex) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
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
        
        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean isCancelled() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean isCompletedExceptionally() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public int getNumberOfDependents() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
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
        
        @Override
        public CompletableFuture<T> toCompletableFuture() {
            Object r = result;
            
            if(r != null) {
                Object obj = encodeRelay(r);
                return new CompletableFuture<T>(obj);
            } else {
                CompletableFuture<T> d = new CompletableFuture<>();
                UniRelay<T, T> uniRelay = new UniRelay<>(d, this);
                unipush(uniRelay);
                return d;
            }
        }
    }
    
}
