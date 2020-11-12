/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * An object for traversing and partitioning elements of a source.  The source
 * of elements covered by a Spliterator could be, for example, an array, a
 * {@link Collection}, an IO channel, or a generator function.
 *
 * <p>A Spliterator may traverse elements individually ({@link
 * #tryAdvance tryAdvance()}) or sequentially in bulk
 * ({@link #forEachRemaining forEachRemaining()}).
 *
 * <p>A Spliterator may also partition off some of its elements (using
 * {@link #trySplit}) as another Spliterator, to be used in
 * possibly-parallel operations.  Operations using a Spliterator that
 * cannot split, or does so in a highly imbalanced or inefficient
 * manner, are unlikely to benefit from parallelism.  Traversal
 * and splitting exhaust elements; each Spliterator is useful for only a single
 * bulk computation.
 *
 * <p>A Spliterator also reports a set of {@link #characteristics()} of its
 * structure, source, and elements from among {@link #ORDERED},
 * {@link #DISTINCT}, {@link #SORTED}, {@link #SIZED}, {@link #NONNULL},
 * {@link #IMMUTABLE}, {@link #CONCURRENT}, and {@link #SUBSIZED}. These may
 * be employed by Spliterator clients to control, specialize or simplify
 * computation.  For example, a Spliterator for a {@link Collection} would
 * report {@code SIZED}, a Spliterator for a {@link Set} would report
 * {@code DISTINCT}, and a Spliterator for a {@link SortedSet} would also
 * report {@code SORTED}.  Characteristics are reported as a simple unioned bit
 * set.
 *
 * Some characteristics additionally constrain method behavior; for example if
 * {@code ORDERED}, traversal methods must conform to their documented ordering.
 * New characteristics may be defined in the future, so implementors should not
 * assign meanings to unlisted values.
 *
 * <p><a id="binding">A Spliterator that does not report {@code IMMUTABLE} or
 * {@code CONCURRENT} is expected to have a documented policy concerning:
 * when the spliterator <em>binds</em> to the element source; and detection of
 * structural interference of the element source detected after binding.</a>  A
 * <em>late-binding</em> Spliterator binds to the source of elements at the
 * point of first traversal, first split, or first query for estimated size,
 * rather than at the time the Spliterator is created.  A Spliterator that is
 * not <em>late-binding</em> binds to the source of elements at the point of
 * construction or first invocation of any method.  Modifications made to the
 * source prior to binding are reflected when the Spliterator is traversed.
 * After binding a Spliterator should, on a best-effort basis, throw
 * {@link ConcurrentModificationException} if structural interference is
 * detected.  Spliterators that do this are called <em>fail-fast</em>.  The
 * bulk traversal method ({@link #forEachRemaining forEachRemaining()}) of a
 * Spliterator may optimize traversal and check for structural interference
 * after all elements have been traversed, rather than checking per-element and
 * failing immediately.
 *
 * <p>Spliterators can provide an estimate of the number of remaining elements
 * via the {@link #estimateSize} method.  Ideally, as reflected in characteristic
 * {@link #SIZED}, this value corresponds exactly to the number of elements
 * that would be encountered in a successful traversal.  However, even when not
 * exactly known, an estimated value may still be useful to operations
 * being performed on the source, such as helping to determine whether it is
 * preferable to split further or traverse the remaining elements sequentially.
 *
 * <p>Despite their obvious utility in parallel algorithms, spliterators are not
 * expected to be thread-safe; instead, implementations of parallel algorithms
 * using spliterators should ensure that the spliterator is only used by one
 * thread at a time.  This is generally easy to attain via <em>serial
 * thread-confinement</em>, which often is a natural consequence of typical
 * parallel algorithms that work by recursive decomposition.  A thread calling
 * {@link #trySplit()} may hand over the returned Spliterator to another thread,
 * which in turn may traverse or further split that Spliterator.  The behaviour
 * of splitting and traversal is undefined if two or more threads operate
 * concurrently on the same spliterator.  If the original thread hands a
 * spliterator off to another thread for processing, it is best if that handoff
 * occurs before any elements are consumed with {@link #tryAdvance(Consumer)
 * tryAdvance()}, as certain guarantees (such as the accuracy of
 * {@link #estimateSize()} for {@code SIZED} spliterators) are only valid before
 * traversal has begun.
 *
 * <p>Primitive subtype specializations of {@code Spliterator} are provided for
 * {@link OfInt int}, {@link OfLong long}, and {@link OfDouble double} values.
 * The subtype default implementations of
 * {@link Spliterator#tryAdvance(java.util.function.Consumer)}
 * and {@link Spliterator#forEachRemaining(java.util.function.Consumer)} box
 * primitive values to instances of their corresponding wrapper class.  Such
 * boxing may undermine any performance advantages gained by using the primitive
 * specializations.  To avoid boxing, the corresponding primitive-based methods
 * should be used.  For example,
 * {@link Spliterator.OfInt#tryAdvance(java.util.function.IntConsumer)}
 * and {@link Spliterator.OfInt#forEachRemaining(java.util.function.IntConsumer)}
 * should be used in preference to
 * {@link Spliterator.OfInt#tryAdvance(java.util.function.Consumer)} and
 * {@link Spliterator.OfInt#forEachRemaining(java.util.function.Consumer)}.
 * Traversal of primitive values using boxing-based methods
 * {@link #tryAdvance tryAdvance()} and
 * {@link #forEachRemaining(java.util.function.Consumer) forEachRemaining()}
 * does not affect the order in which the values, transformed to boxed values,
 * are encountered.
 *
 * @param <T> the type of elements returned by this Spliterator
 *
 * @apiNote <p>Spliterators, like {@code Iterator}s, are for traversing the elements of
 * a source.  The {@code Spliterator} API was designed to support efficient
 * parallel traversal in addition to sequential traversal, by supporting
 * decomposition as well as single-element iteration.  In addition, the
 * protocol for accessing elements via a Spliterator is designed to impose
 * smaller per-element overhead than {@code Iterator}, and to avoid the inherent
 * race involved in having separate methods for {@code hasNext()} and
 * {@code next()}.
 *
 * <p>For mutable sources, arbitrary and non-deterministic behavior may occur if
 * the source is structurally interfered with (elements added, replaced, or
 * removed) between the time that the Spliterator binds to its data source and
 * the end of traversal.  For example, such interference will produce arbitrary,
 * non-deterministic results when using the {@code java.util.stream} framework.
 *
 * <p>Structural interference of a source can be managed in the following ways
 * (in approximate order of decreasing desirability):
 * <ul>
 * <li>The source cannot be structurally interfered with.
 * <br>For example, an instance of
 * {@link java.util.concurrent.CopyOnWriteArrayList} is an immutable source.
 * A Spliterator created from the source reports a characteristic of
 * {@code IMMUTABLE}.</li>
 * <li>The source manages concurrent modifications.
 * <br>For example, a key set of a {@link java.util.concurrent.ConcurrentHashMap}
 * is a concurrent source.  A Spliterator created from the source reports a
 * characteristic of {@code CONCURRENT}.</li>
 * <li>The mutable source provides a late-binding and fail-fast Spliterator.
 * <br>Late binding narrows the window during which interference can affect
 * the calculation; fail-fast detects, on a best-effort basis, that structural
 * interference has occurred after traversal has commenced and throws
 * {@link ConcurrentModificationException}.  For example, {@link ArrayList},
 * and many other non-concurrent {@code Collection} classes in the JDK, provide
 * a late-binding, fail-fast spliterator.</li>
 * <li>The mutable source provides a non-late-binding but fail-fast Spliterator.
 * <br>The source increases the likelihood of throwing
 * {@code ConcurrentModificationException} since the window of potential
 * interference is larger.</li>
 * <li>The mutable source provides a late-binding and non-fail-fast Spliterator.
 * <br>The source risks arbitrary, non-deterministic behavior after traversal
 * has commenced since interference is not detected.
 * </li>
 * <li>The mutable source provides a non-late-binding and non-fail-fast
 * Spliterator.
 * <br>The source increases the risk of arbitrary, non-deterministic behavior
 * since non-detected interference may occur after construction.
 * </li>
 * </ul>
 *
 * <p><b>Example.</b> Here is a class (not a very useful one, except
 * for illustration) that maintains an array in which the actual data
 * are held in even locations, and unrelated tag data are held in odd
 * locations. Its Spliterator ignores the tags.
 *
 * <pre> {@code
 * class TaggedArray<T> {
 *   private final Object[] elements; // immutable after construction
 *   TaggedArray(T[] data, Object[] tags) {
 *     int size = data.length;
 *     if (tags.length != size) throw new IllegalArgumentException();
 *     this.elements = new Object[2 * size];
 *     for (int i = 0, j = 0; i < size; ++i) {
 *       elements[j++] = data[i];
 *       elements[j++] = tags[i];
 *     }
 *   }
 *
 *   public Spliterator<T> spliterator() {
 *     return new TaggedArraySpliterator<>(elements, 0, elements.length);
 *   }
 *
 *   static class TaggedArraySpliterator<T> implements Spliterator<T> {
 *     private final Object[] array;
 *     private int origin; // current index, advanced on split or traversal
 *     private final int fence; // one past the greatest index
 *
 *     TaggedArraySpliterator(Object[] array, int origin, int fence) {
 *       this.array = array; this.origin = origin; this.fence = fence;
 *     }
 *
 *     public void forEachRemaining(Consumer<? super T> action) {
 *       for (; origin < fence; origin += 2)
 *         action.accept((T) array[origin]);
 *     }
 *
 *     public boolean tryAdvance(Consumer<? super T> action) {
 *       if (origin < fence) {
 *         action.accept((T) array[origin]);
 *         origin += 2;
 *         return true;
 *       }
 *       else // cannot advance
 *         return false;
 *     }
 *
 *     public Spliterator<T> trySplit() {
 *       int lo = origin; // divide range in half
 *       int mid = ((lo + fence) >>> 1) & ~1; // force midpoint to be even
 *       if (lo < mid) { // split out left half
 *         origin = mid; // reset this Spliterator's origin
 *         return new TaggedArraySpliterator<>(array, lo, mid);
 *       }
 *       else       // too small to split
 *         return null;
 *     }
 *
 *     public long estimateSize() {
 *       return (long)((fence - origin) / 2);
 *     }
 *
 *     public int characteristics() {
 *       return ORDERED | SIZED | IMMUTABLE | SUBSIZED;
 *     }
 *   }
 * }}</pre>
 *
 * <p>As an example how a parallel computation framework, such as the
 * {@code java.util.stream} package, would use Spliterator in a parallel
 * computation, here is one way to implement an associated parallel forEach,
 * that illustrates the primary usage idiom of splitting off subtasks until
 * the estimated amount of work is small enough to perform
 * sequentially. Here we assume that the order of processing across
 * subtasks doesn't matter; different (forked) tasks may further split
 * and process elements concurrently in undetermined order.  This
 * example uses a {@link java.util.concurrent.CountedCompleter};
 * similar usages apply to other parallel task constructions.
 *
 * <pre>{@code
 * static <T> void parEach(TaggedArray<T> a, Consumer<T> action) {
 *   Spliterator<T> s = a.spliterator();
 *   long targetBatchSize = s.estimateSize() / (ForkJoinPool.getCommonPoolParallelism() * 8);
 *   new ParEach(null, s, action, targetBatchSize).invoke();
 * }
 *
 * static class ParEach<T> extends CountedCompleter<Void> {
 *   final Spliterator<T> spliterator;
 *   final Consumer<T> action;
 *   final long targetBatchSize;
 *
 *   ParEach(ParEach<T> parent, Spliterator<T> spliterator,
 *           Consumer<T> action, long targetBatchSize) {
 *     super(parent);
 *     this.spliterator = spliterator; this.action = action;
 *     this.targetBatchSize = targetBatchSize;
 *   }
 *
 *   public void compute() {
 *     Spliterator<T> sub;
 *     while (spliterator.estimateSize() > targetBatchSize &&
 *            (sub = spliterator.trySplit()) != null) {
 *       addToPendingCount(1);
 *       new ParEach<>(this, sub, action, targetBatchSize).fork();
 *     }
 *     spliterator.forEachRemaining(action);
 *     propagateCompletion();
 *   }
 * }}</pre>
 * @implNote If the boolean system property {@code org.openjdk.java.util.stream.tripwire}
 * is set to {@code true} then diagnostic warnings are reported if boxing of
 * primitive values occur when operating on primitive subtype specializations.
 * @see Collection
 * @since 1.8
 */
/*
 * 流迭代器的引用类型版本
 *
 * Spliterator这个词来自Splitable Iterator，直译为"可分割的迭代器"。
 *
 * Spliterator与Iterator的主要区别是增加了一个拆分数据的方法trySplit()，以便支持并行运算。
 * 在遍历数据方面，Spliterator与Iterator的行为一致的。
 * Spliterator和Iterator可以互相做适配。
 *
 * 由于Spliterator通常用在流式操作中，因此，我们将其简称为"流迭代器"。
 *
 * 注：流式操作包含三个要素：Stream、Spliterator、Sink
 */
public interface Spliterator<T> {
    
    /*
     * Spliterator的参数
     *
     * 注：Spliterator的参数与Stream的参数并不是一一对应的
     *
     * 参见：StreamOpFlag
     */
    
    /**
     * Characteristic value signifying that, for each pair of
     * encountered elements {@code x, y}, {@code !x.equals(y)}. This
     * applies for example, to a Spliterator based on a {@link Set}.
     */
    // 表示每对元素各不相同，比如Set中的元素
    int DISTINCT = 0x00000001;
    
    /**
     * Characteristic value signifying that encounter order follows a defined
     * sort order. If so, method {@link #getComparator()} returns the associated
     * Comparator, or {@code null} if all elements are {@link Comparable} and
     * are sorted by their natural ordering.
     *
     * <p>A Spliterator that reports {@code SORTED} must also report
     * {@code ORDERED}.
     *
     * @apiNote The spliterators for {@code Collection} classes in the JDK that
     * implement {@link NavigableSet} or {@link SortedSet} report {@code SORTED}.
     */
    /*
     * 表示Spliterator中的元素序列遵循预设的排序顺序。
     *
     * 当Spliterator包含SORTED参数时，如果调用getComparator()，则会返回一个Comparator，或者返回null。
     * 返回null意味着所有元素都应当具备Comparable接口。
     *
     * 具有SORTED参数的Spliterator同时也应当具备ORDERED参数。
     *
     * 实现了NavigableSet或SortedSet接口的Collection子类元素，其Spliterator应具备SORTED参数。
     */
    int SORTED = 0x00000004;
    
    /**
     * Characteristic value signifying that an encounter order is defined for
     * elements. If so, this Spliterator guarantees that method
     * {@link #trySplit} splits a strict prefix of elements, that method
     * {@link #tryAdvance} steps by one element in prefix order, and that
     * {@link #forEachRemaining} performs actions in encounter order.
     *
     * <p>A {@link Collection} has an encounter order if the corresponding
     * {@link Collection#iterator} documents an order. If so, the encounter
     * order is the same as the documented order. Otherwise, a collection does
     * not have an encounter order.
     *
     * @apiNote Encounter order is guaranteed to be ascending index order for
     * any {@link List}. But no order is guaranteed for hash-based collections
     * such as {@link HashSet}. Clients of a Spliterator that reports
     * {@code ORDERED} are expected to preserve ordering constraints in
     * non-commutative parallel computations.
     */
    // 表示元素有固定的遭遇顺序(遍历顺序)
    int ORDERED = 0x00000010;
    
    /**
     * Characteristic value signifying that the value returned from
     * {@code estimateSize()} prior to traversal or splitting represents a
     * finite size that, in the absence of structural source modification,
     * represents an exact count of the number of elements that would be
     * encountered by a complete traversal.
     *
     * @apiNote Most Spliterators for Collections, that cover all elements of a
     * {@code Collection} report this characteristic. Sub-spliterators, such as
     * those for {@link HashSet}, that cover a sub-set of elements and
     * approximate their reported size do not.
     */
    /*
     * 数据量有限
     *
     * 表示可以调用estimateSize()后返回一个有限大小(往往是元素的真实数量)。
     * 大多数Collection的Spliterator都拥有此特征。
     */
    int SIZED = 0x00000040;
    
    /**
     * Characteristic value signifying that the source guarantees that
     * encountered elements will not be {@code null}. (This applies,
     * for example, to most concurrent collections, queues, and maps.)
     */
    // 表示元素非空，如大多数并行容器
    int NONNULL = 0x00000100;
    
    /**
     * Characteristic value signifying that the element source cannot be
     * structurally modified; that is, elements cannot be added, replaced, or
     * removed, so such changes cannot occur during traversal. A Spliterator
     * that does not report {@code IMMUTABLE} or {@code CONCURRENT} is expected
     * to have a documented policy (for example throwing
     * {@link ConcurrentModificationException}) concerning structural
     * interference detected during traversal.
     */
    // 表示元素结构不能被修改，即无法添加、删除、替换元素
    int IMMUTABLE = 0x00000400;
    
    /**
     * Characteristic value signifying that the element source may be safely
     * concurrently modified (allowing additions, replacements, and/or removals)
     * by multiple threads without external synchronization. If so, the
     * Spliterator is expected to have a documented policy concerning the impact
     * of modifications during traversal.
     *
     * <p>A top-level Spliterator should not report both {@code CONCURRENT} and
     * {@code SIZED}, since the finite size, if known, may change if the source
     * is concurrently modified during traversal. Such a Spliterator is
     * inconsistent and no guarantees can be made about any computation using
     * that Spliterator. Sub-spliterators may report {@code SIZED} if the
     * sub-split size is known and additions or removals to the source are not
     * reflected when traversing.
     *
     * <p>A top-level Spliterator should not report both {@code CONCURRENT} and
     * {@code IMMUTABLE}, since they are mutually exclusive. Such a Spliterator
     * is inconsistent and no guarantees can be made about any computation using
     * that Spliterator. Sub-spliterators may report {@code IMMUTABLE} if
     * additions or removals to the source are not reflected when traversing.
     *
     * @apiNote Most concurrent collections maintain a consistency policy
     * guaranteeing accuracy with respect to elements present at the point of
     * Spliterator construction, but possibly not reflecting subsequent
     * additions or removals.
     */
    // 表示可以在没有外部同步的情况下由多个线程安全地同时修改元素，这往往意味着这些元素来自于同步容器
    int CONCURRENT = 0x00001000;
    
    /**
     * Characteristic value signifying that all Spliterators resulting from
     * {@code trySplit()} will be both {@link #SIZED} and {@link #SUBSIZED}.
     * (This means that all child Spliterators, whether direct or indirect, will
     * be {@code SIZED}.)
     *
     * <p>A Spliterator that does not report {@code SIZED} as required by
     * {@code SUBSIZED} is inconsistent and no guarantees can be made about any
     * computation using that Spliterator.
     *
     * @apiNote Some spliterators, such as the top-level spliterator for an
     * approximately balanced binary tree, will report {@code SIZED} but not
     * {@code SUBSIZED}, since it is common to know the size of the entire tree
     * but not the exact sizes of subtrees.
     */
    /*
     * 表示调用trySplit()后返回的子Spliterator将同时具有SIZED和SUBSIZED参数。
     * 有些树形Node只具有SIZED特征而不具有SUBSIZED特征，因为整棵树的元素个数是已知的，但子树元素个数却未知。
     */
    int SUBSIZED = 0x00004000;
    
    
    
    /**
     * If this spliterator can be partitioned, returns a Spliterator
     * covering elements, that will, upon return from this method, not
     * be covered by this Spliterator.
     *
     * <p>If this Spliterator is {@link #ORDERED}, the returned Spliterator
     * must cover a strict prefix of the elements.
     *
     * <p>Unless this Spliterator covers an infinite number of elements,
     * repeated calls to {@code trySplit()} must eventually return {@code null}.
     * Upon non-null return:
     * <ul>
     * <li>the value reported for {@code estimateSize()} before splitting,
     * must, after splitting, be greater than or equal to {@code estimateSize()}
     * for this and the returned Spliterator; and</li>
     * <li>if this Spliterator is {@code SUBSIZED}, then {@code estimateSize()}
     * for this spliterator before splitting must be equal to the sum of
     * {@code estimateSize()} for this and the returned Spliterator after
     * splitting.</li>
     * </ul>
     *
     * <p>This method may return {@code null} for any reason,
     * including emptiness, inability to split after traversal has
     * commenced, data structure constraints, and efficiency
     * considerations.
     *
     * @return a {@code Spliterator} covering some portion of the
     * elements, or {@code null} if this spliterator cannot be split
     *
     * @apiNote An ideal {@code trySplit} method efficiently (without
     * traversal) divides its elements exactly in half, allowing
     * balanced parallel computation.  Many departures from this ideal
     * remain highly effective; for example, only approximately
     * splitting an approximately balanced tree, or for a tree in
     * which leaf nodes may contain either one or two elements,
     * failing to further split these nodes.  However, large
     * deviations in balance and/or overly inefficient {@code
     * trySplit} mechanics typically result in poor parallel
     * performance.
     */
    /*
     * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
     *
     * 注1：该操作可能会引起内部游标的变化
     * 注2：子Spliterator的参数可能发生改变
     */
    Spliterator<T> trySplit();
    
    /**
     * If a remaining element exists, performs the given action on it,
     * returning {@code true}; else returns {@code false}.  If this
     * Spliterator is {@link #ORDERED} the action is performed on the
     * next element in encounter order.  Exceptions thrown by the
     * action are relayed to the caller.
     *
     * @param action The action
     *
     * @return {@code false} if no remaining elements existed
     * upon entry to this method, else {@code true}.
     *
     * @throws NullPointerException if the specified action is null
     */
    /*
     * 尝试用action消费当前Spliterator中下一个元素。
     * 返回值指示是否找到了下一个元素。
     *
     * 注1：该操作可能会引起内部游标的变化
     * 注2：该操作可能会顺着sink链向下游传播
     */
    boolean tryAdvance(Consumer<? super T> action);
    
    /**
     * Performs the given action for each remaining element, sequentially in
     * the current thread, until all elements have been processed or the action
     * throws an exception.  If this Spliterator is {@link #ORDERED}, actions
     * are performed in encounter order.  Exceptions thrown by the action
     * are relayed to the caller.
     *
     * @param action The action
     *
     * @throws NullPointerException if the specified action is null
     * @implSpec The default implementation repeatedly invokes {@link #tryAdvance} until
     * it returns {@code false}.  It should be overridden whenever possible.
     */
    /*
     * 尝试用action逐个消费当前流迭代器中所有剩余元素。
     *
     * 注1：该操作可能会引起内部游标的变化
     * 注2：该操作可能会顺着sink链向下游传播
     */
    default void forEachRemaining(Consumer<? super T> action) {
        while(true) {
            boolean hasMoreElem = tryAdvance(action);
        
            // 如果已经没有更多元素了，则结束循环
            if(!hasMoreElem) {
                break;
            }
        }
    }
    
    /**
     * Returns an estimate of the number of elements that would be
     * encountered by a {@link #forEachRemaining} traversal, or returns {@link
     * Long#MAX_VALUE} if infinite, unknown, or too expensive to compute.
     *
     * <p>If this Spliterator is {@link #SIZED} and has not yet been partially
     * traversed or split, or this Spliterator is {@link #SUBSIZED} and has
     * not yet been partially traversed, this estimate must be an accurate
     * count of elements that would be encountered by a complete traversal.
     * Otherwise, this estimate may be arbitrarily inaccurate, but must decrease
     * as specified across invocations of {@link #trySplit}.
     *
     * @return the estimated size, or {@code Long.MAX_VALUE} if infinite,
     * unknown, or too expensive to compute.
     *
     * @apiNote Even an inexact estimate is often useful and inexpensive to compute.
     * For example, a sub-spliterator of an approximately balanced binary tree
     * may return a value that estimates the number of elements to be half of
     * that of its parent; if the root Spliterator does not maintain an
     * accurate count, it could estimate size to be the power of two
     * corresponding to its maximum depth.
     */
    /*
     * 初始时，返回流迭代器中的元素总量(可能不精确)。
     * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
     * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
     */
    long estimateSize();
    
    /**
     * Convenience method that returns {@link #estimateSize()} if this Spliterator is {@link #SIZED}, else {@code -1}.
     *
     * @return the exact size, if known, else {@code -1}.
     *
     * @implSpec The default implementation returns the result of {@code estimateSize()}
     * if the Spliterator reports a characteristic of {@code SIZED}, and {@code -1} otherwise.
     */
    /*
     * 初始时，尝试返回流迭代器中的元素总量。如果无法获取精确值，则返回-1。
     * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
     *
     * 注：通常在流迭代器拥有SIZED参数时可以获取到一个精确值。
     */
    default long getExactSizeIfKnown() {
        // 数据量无限
        if((characteristics() & SIZED) == 0) {
            return -1L;
        }
        
        // 数据量有限
        return estimateSize();
    }
    
    /**
     * Returns a set of characteristics of this Spliterator and its
     * elements. The result is represented as ORed values from {@link
     * #ORDERED}, {@link #DISTINCT}, {@link #SORTED}, {@link #SIZED},
     * {@link #NONNULL}, {@link #IMMUTABLE}, {@link #CONCURRENT},
     * {@link #SUBSIZED}.  Repeated calls to {@code characteristics()} on
     * a given spliterator, prior to or in-between calls to {@code trySplit},
     * should always return the same result.
     *
     * <p>If a Spliterator reports an inconsistent set of
     * characteristics (either those returned from a single invocation
     * or across multiple invocations), no guarantees can be made
     * about any computation using this Spliterator.
     *
     * @return a representation of characteristics
     *
     * @apiNote The characteristics of a given spliterator before splitting
     * may differ from the characteristics after splitting.  For specific
     * examples see the characteristic values {@link #SIZED}, {@link #SUBSIZED}
     * and {@link #CONCURRENT}.
     */
    // 返回流迭代器的参数
    int characteristics();
    
    /**
     * Returns {@code true} if this Spliterator's {@link
     * #characteristics} contain all of the given characteristics.
     *
     * @param characteristics the characteristics to check for
     *
     * @return {@code true} if all the specified characteristics are present,
     * else {@code false}
     *
     * @implSpec The default implementation returns true if the corresponding bits
     * of the given characteristics are set.
     */
    // 判断流迭代器是否包含指定的参数
    default boolean hasCharacteristics(int characteristics) {
        return (characteristics() & characteristics) == characteristics;
    }
    
    /**
     * If this Spliterator's source is {@link #SORTED} by a {@link Comparator}, returns that {@code Comparator}.
     * If the source is {@code SORTED} in {@linkplain Comparable natural order}, returns {@code null}.  Otherwise,
     * if the source is not {@code SORTED}, throws {@link IllegalStateException}.
     *
     * @return a Comparator, or {@code null} if the elements are sorted in the natural order.
     *
     * @throws IllegalStateException if the spliterator does not report a characteristic of {@code SORTED}.
     * @implSpec The default implementation always throws {@link IllegalStateException}.
     */
    /*
     * 对于具有SORTED参数的容器来说，
     * 如果该容器使用Comparator排序，则返回其Comparator；
     * 如果该容器使用Comparable实现自然排序，则返回null；
     *
     * 对于不具有SORTED参数的容器来说，抛出异常。
     */
    default Comparator<? super T> getComparator() {
        throw new IllegalStateException();
    }
    
    
    /**
     * A Spliterator specialized for primitive values.
     *
     * @param <T>        the type of elements returned by this Spliterator.  The
     *                   type must be a wrapper type for a primitive type, such as {@code Integer}
     *                   for the primitive {@code int} type.
     * @param <T_CONS>   the type of primitive consumer.  The type must be a
     *                   primitive specialization of {@link java.util.function.Consumer} for
     *                   {@code T}, such as {@link java.util.function.IntConsumer} for
     *                   {@code Integer}.
     * @param <T_SPLITR> the type of primitive Spliterator.  The type must be
     *                   a primitive specialization of Spliterator for {@code T}, such as
     *                   {@link Spliterator.OfInt} for {@code Integer}.
     *
     * @see Spliterator.OfInt
     * @see Spliterator.OfLong
     * @see Spliterator.OfDouble
     * @since 1.8
     */
    // 流迭代器的基本数值类型版本
    interface OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends Spliterator<T> {
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        T_SPLITR trySplit();
        
        /**
         * If a remaining element exists, performs the given action on it,
         * returning {@code true}; else returns {@code false}.  If this
         * Spliterator is {@link #ORDERED} the action is performed on the
         * next element in encounter order.  Exceptions thrown by the
         * action are relayed to the caller.
         *
         * @param action The action
         *
         * @return {@code false} if no remaining elements existed
         * upon entry to this method, else {@code true}.
         *
         * @throws NullPointerException if the specified action is null
         */
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @SuppressWarnings("overloads")
        boolean tryAdvance(T_CONS action);
        
        /**
         * Performs the given action for each remaining element, sequentially in
         * the current thread, until all elements have been processed or the
         * action throws an exception.  If this Spliterator is {@link #ORDERED},
         * actions are performed in encounter order.  Exceptions thrown by the
         * action are relayed to the caller.
         *
         * @param action The action
         *
         * @throws NullPointerException if the specified action is null
         * @implSpec The default implementation repeatedly invokes {@link #tryAdvance}
         * until it returns {@code false}.  It should be overridden whenever
         * possible.
         */
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @SuppressWarnings("overloads")
        default void forEachRemaining(T_CONS action) {
            while(tryAdvance(action)) {
            }
        }
    }
    
    /**
     * A Spliterator specialized for {@code int} values.
     *
     * @since 1.8
     */
    // 流迭代器的int类型版本
    interface OfInt extends OfPrimitive<Integer, IntConsumer, OfInt> {
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        OfInt trySplit();
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        boolean tryAdvance(IntConsumer action);
        
        /**
         * {@inheritDoc}
         *
         * @implSpec If the action is an instance of {@code IntConsumer} then it is cast
         * to {@code IntConsumer} and passed to
         * {@link #tryAdvance(java.util.function.IntConsumer)}; otherwise
         * the action is adapted to an instance of {@code IntConsumer}, by
         * boxing the argument of {@code IntConsumer}, and then passed to
         * {@link #tryAdvance(java.util.function.IntConsumer)}.
         */
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        default boolean tryAdvance(Consumer<? super Integer> action) {
            if(action instanceof IntConsumer) {
                return tryAdvance((IntConsumer) action);
            }
            
            if(Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfInt.tryAdvance((IntConsumer) action::accept)");
            }
            
            return tryAdvance((IntConsumer) action::accept);
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        default void forEachRemaining(IntConsumer action) {
            while(tryAdvance(action)) {
            }
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec If the action is an instance of {@code IntConsumer} then it is cast
         * to {@code IntConsumer} and passed to
         * {@link #forEachRemaining(java.util.function.IntConsumer)}; otherwise
         * the action is adapted to an instance of {@code IntConsumer}, by
         * boxing the argument of {@code IntConsumer}, and then passed to
         * {@link #forEachRemaining(java.util.function.IntConsumer)}.
         */
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        default void forEachRemaining(Consumer<? super Integer> action) {
            if(action instanceof IntConsumer) {
                forEachRemaining((IntConsumer) action);
                return;
            }
    
            if(Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfInt.forEachRemaining((IntConsumer) action::accept)");
            }
    
            forEachRemaining((IntConsumer) action::accept);
        }
    }
    
    /**
     * A Spliterator specialized for {@code long} values.
     *
     * @since 1.8
     */
    // 流迭代器的long类型版本
    interface OfLong extends OfPrimitive<Long, LongConsumer, OfLong> {
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        OfLong trySplit();
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        boolean tryAdvance(LongConsumer action);
        
        /**
         * {@inheritDoc}
         *
         * @implSpec If the action is an instance of {@code LongConsumer} then it is cast
         * to {@code LongConsumer} and passed to
         * {@link #tryAdvance(java.util.function.LongConsumer)}; otherwise
         * the action is adapted to an instance of {@code LongConsumer}, by
         * boxing the argument of {@code LongConsumer}, and then passed to
         * {@link #tryAdvance(java.util.function.LongConsumer)}.
         */
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        default boolean tryAdvance(Consumer<? super Long> action) {
            if(action instanceof LongConsumer) {
                return tryAdvance((LongConsumer) action);
            }
            
            if(Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfLong.tryAdvance((LongConsumer) action::accept)");
            }
            
            return tryAdvance((LongConsumer) action::accept);
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        default void forEachRemaining(LongConsumer action) {
            while(tryAdvance(action)) {
            }
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec If the action is an instance of {@code LongConsumer} then it is cast
         * to {@code LongConsumer} and passed to
         * {@link #forEachRemaining(java.util.function.LongConsumer)}; otherwise
         * the action is adapted to an instance of {@code LongConsumer}, by
         * boxing the argument of {@code LongConsumer}, and then passed to
         * {@link #forEachRemaining(java.util.function.LongConsumer)}.
         */
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        default void forEachRemaining(Consumer<? super Long> action) {
            if(action instanceof LongConsumer) {
                forEachRemaining((LongConsumer) action);
                return;
            }
    
            if(Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfLong.forEachRemaining((LongConsumer) action::accept)");
            }
    
            forEachRemaining((LongConsumer) action::accept);
        }
    }
    
    /**
     * A Spliterator specialized for {@code double} values.
     *
     * @since 1.8
     */
    // 流迭代器的double类型版本
    interface OfDouble extends OfPrimitive<Double, DoubleConsumer, OfDouble> {
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        OfDouble trySplit();
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        boolean tryAdvance(DoubleConsumer action);
        
        /**
         * {@inheritDoc}
         *
         * @implSpec If the action is an instance of {@code DoubleConsumer} then it is
         * cast to {@code DoubleConsumer} and passed to
         * {@link #tryAdvance(java.util.function.DoubleConsumer)}; otherwise
         * the action is adapted to an instance of {@code DoubleConsumer}, by
         * boxing the argument of {@code DoubleConsumer}, and then passed to
         * {@link #tryAdvance(java.util.function.DoubleConsumer)}.
         */
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        default boolean tryAdvance(Consumer<? super Double> action) {
            if(action instanceof DoubleConsumer) {
                return tryAdvance((DoubleConsumer) action);
            }
            
            if(Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfDouble.tryAdvance((DoubleConsumer) action::accept)");
            }
            
            return tryAdvance((DoubleConsumer) action::accept);
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        default void forEachRemaining(DoubleConsumer action) {
            while(tryAdvance(action)) {
            }
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec If the action is an instance of {@code DoubleConsumer} then it is
         * cast to {@code DoubleConsumer} and passed to
         * {@link #forEachRemaining(java.util.function.DoubleConsumer)};
         * otherwise the action is adapted to an instance of
         * {@code DoubleConsumer}, by boxing the argument of
         * {@code DoubleConsumer}, and then passed to
         * {@link #forEachRemaining(java.util.function.DoubleConsumer)}.
         */
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        default void forEachRemaining(Consumer<? super Double> action) {
            if(action instanceof DoubleConsumer) {
                forEachRemaining((DoubleConsumer) action);
                return;
            }
            
            if(Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Spliterator.OfDouble.forEachRemaining((DoubleConsumer) action::accept)");
            }
            
            forEachRemaining((DoubleConsumer) action::accept);
        }
    }
    
}
