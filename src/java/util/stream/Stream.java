/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.util.stream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.StreamSpliterators.InfiniteSupplyingSpliterator.OfRef;
import java.util.stream.Streams.StreamBuilderImpl;
import java.util.stream.WhileOps.UnorderedWhileSpliterator.OfRef.Dropping;
import java.util.stream.WhileOps.UnorderedWhileSpliterator.OfRef.Taking;

/**
 * A sequence of elements supporting sequential and parallel aggregate
 * operations.  The following example illustrates an aggregate operation using
 * {@link Stream} and {@link IntStream}:
 *
 * <pre>{@code
 *     int sum = widgets.stream()
 *                      .filter(w -> w.getColor() == RED)
 *                      .mapToInt(w -> w.getWeight())
 *                      .sum();
 * }</pre>
 *
 * In this example, {@code widgets} is a {@code Collection<Widget>}.  We create
 * a stream of {@code Widget} objects via {@link Collection#stream Collection.stream()},
 * filter it to produce a stream containing only the red widgets, and then
 * transform it into a stream of {@code int} values representing the weight of
 * each red widget. Then this stream is summed to produce a total weight.
 *
 * <p>In addition to {@code Stream}, which is a stream of object references,
 * there are primitive specializations for {@link IntStream}, {@link LongStream},
 * and {@link DoubleStream}, all of which are referred to as "streams" and
 * conform to the characteristics and restrictions described here.
 *
 * <p>To perform a computation, stream
 * <a href="package-summary.html#StreamOps">operations</a> are composed into a
 * <em>stream pipeline</em>.  A stream pipeline consists of a source (which
 * might be an array, a collection, a generator function, an I/O channel,
 * etc), zero or more <em>intermediate operations</em> (which transform a
 * stream into another stream, such as {@link Stream#filter(Predicate)}), and a
 * <em>terminal operation</em> (which produces a result or side-effect, such
 * as {@link Stream#count()} or {@link Stream#forEach(Consumer)}).
 * Streams are lazy; computation on the source data is only performed when the
 * terminal operation is initiated, and source elements are consumed only
 * as needed.
 *
 * <p>A stream implementation is permitted significant latitude in optimizing
 * the computation of the result.  For example, a stream implementation is free
 * to elide operations (or entire stages) from a stream pipeline -- and
 * therefore elide invocation of behavioral parameters -- if it can prove that
 * it would not affect the result of the computation.  This means that
 * side-effects of behavioral parameters may not always be executed and should
 * not be relied upon, unless otherwise specified (such as by the terminal
 * operations {@code forEach} and {@code forEachOrdered}). (For a specific
 * example of such an optimization, see the API note documented on the
 * {@link #count} operation.  For more detail, see the
 * <a href="package-summary.html#SideEffects">side-effects</a> section of the
 * stream package documentation.)
 *
 * <p>Collections and streams, while bearing some superficial similarities,
 * have different goals.  Collections are primarily concerned with the efficient
 * management of, and access to, their elements.  By contrast, streams do not
 * provide a means to directly access or manipulate their elements, and are
 * instead concerned with declaratively describing their source and the
 * computational operations which will be performed in aggregate on that source.
 * However, if the provided stream operations do not offer the desired
 * functionality, the {@link #iterator()} and {@link #spliterator()} operations
 * can be used to perform a controlled traversal.
 *
 * <p>A stream pipeline, like the "widgets" example above, can be viewed as
 * a <em>query</em> on the stream source.  Unless the source was explicitly
 * designed for concurrent modification (such as a {@link ConcurrentHashMap}),
 * unpredictable or erroneous behavior may result from modifying the stream
 * source while it is being queried.
 *
 * <p>Most stream operations accept parameters that describe user-specified
 * behavior, such as the lambda expression {@code w -> w.getWeight()} passed to
 * {@code mapToInt} in the example above.  To preserve correct behavior,
 * these <em>behavioral parameters</em>:
 * <ul>
 * <li>must be <a href="package-summary.html#NonInterference">non-interfering</a>
 * (they do not modify the stream source); and</li>
 * <li>in most cases must be <a href="package-summary.html#Statelessness">stateless</a>
 * (their result should not depend on any state that might change during execution
 * of the stream pipeline).</li>
 * </ul>
 *
 * <p>Such parameters are always instances of a
 * <a href="../function/package-summary.html">functional interface</a> such
 * as {@link java.util.function.Function}, and are often lambda expressions or
 * method references.  Unless otherwise specified these parameters must be
 * <em>non-null</em>.
 *
 * <p>A stream should be operated on (invoking an intermediate or terminal stream
 * operation) only once.  This rules out, for example, "forked" streams, where
 * the same source feeds two or more pipelines, or multiple traversals of the
 * same stream.  A stream implementation may throw {@link IllegalStateException}
 * if it detects that the stream is being reused. However, since some stream
 * operations may return their receiver rather than a new stream object, it may
 * not be possible to detect reuse in all cases.
 *
 * <p>Streams have a {@link #close()} method and implement {@link AutoCloseable}.
 * Operating on a stream after it has been closed will throw {@link IllegalStateException}.
 * Most stream instances do not actually need to be closed after use, as they
 * are backed by collections, arrays, or generating functions, which require no
 * special resource management. Generally, only streams whose source is an IO channel,
 * such as those returned by {@link Files#lines(Path)}, will require closing. If a
 * stream does require closing, it must be opened as a resource within a try-with-resources
 * statement or similar control structure to ensure that it is closed promptly after its
 * operations have completed.
 *
 * <p>Stream pipelines may execute either sequentially or in
 * <a href="package-summary.html#Parallelism">parallel</a>.  This
 * execution mode is a property of the stream.  Streams are created
 * with an initial choice of sequential or parallel execution.  (For example,
 * {@link Collection#stream() Collection.stream()} creates a sequential stream,
 * and {@link Collection#parallelStream() Collection.parallelStream()} creates
 * a parallel one.)  This choice of execution mode may be modified by the
 * {@link #sequential()} or {@link #parallel()} methods, and may be queried with
 * the {@link #isParallel()} method.
 *
 * @param <T> the type of the stream elements
 *
 * @see IntStream
 * @see LongStream
 * @see DoubleStream
 * @see <a href="package-summary.html">java.util.stream</a>
 * @since 1.8
 */
/*
 * 流接口，这是完成数据流式操作的基本骨架。
 *
 * Stream可直译作流，即数据像流一样从Stream上经过，并经过sink的择取。
 *
 * 一个完整的流通常包含三个阶段：源头阶段、中间阶段、终端阶段。
 * 源头阶段的流和中间阶段的流都需要实现Stream接口，而终端阶段不需要实现Stream接口。
 * 因此，我们可以认为源头阶段的流和中间阶段的流都是物理上存在的，而终端阶段的流只是一个模拟概念。
 *
 * Stream封装了三大类操作：创建流、中间操作、终端操作。
 * 创建源头阶段的流的操作是显式定义的，而创建中间阶段的流的操作通常在匿名类中完成。
 * 对于源头阶段和中间阶段的流，它们都可以调用中间操作和终端操作。
 * 但是对于终端阶段的流，由于其实一个概念上的存在，并没有真实地去创建一个Stream，所以不能显式调用中间操作和终端操作。
 *
 * 在每个流的源头阶段，必须包含一个Spliterator作为数据源，
 * 而在非源头阶段，必须包含Sink来决定如何择取元素。
 *
 * 注：流式操作包含三个要素：Stream、Spliterator、Sink
 */
public interface Stream<T> extends BaseStream<T, Stream<T>> {
    
    /*▼ 创建流的源头阶段 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an empty sequential {@code Stream}.
     *
     * @param <T> the type of stream elements
     *
     * @return an empty sequential stream
     */
    // 构造处于源头阶段的流，该流不包含任何待处理元素
    static <T> Stream<T> empty() {
        // 构造"空"Spliterator(引用类型版本)
        Spliterator<T> spliterator = Spliterators.emptySpliterator();
        return StreamSupport.stream(spliterator, false);
    }
    
    /**
     * Returns a sequential {@code Stream} containing a single element.
     *
     * @param e   the single element
     * @param <T> the type of stream elements
     *
     * @return a singleton sequential stream
     */
    // 构造处于源头阶段的流，该流仅包含一个元素。当然，该元素可能是多维度的，比如数组或其他容器
    static <T> Stream<T> of(T e) {
        // 构造单元素流迭代器，流构建器处于[已完成]状态
        Spliterator<T> spliterator = new StreamBuilderImpl<>(e);
        return StreamSupport.stream(spliterator, false);
    }
    
    /**
     * Returns a sequential {@code Stream} containing a single element, if
     * non-null, otherwise returns an empty {@code Stream}.
     *
     * @param e   the single element
     * @param <T> the type of stream elements
     *
     * @return a stream with a single element if the specified element
     * is non-null, otherwise an empty stream
     *
     * @since 9
     */
    /*
     * 构造处于源头阶段的流，该流最多仅包含一个元素。
     *
     * 如果e为null，则执行empty()操作。
     * 如果e不为null，则执行of(T)操作。
     */
    static <T> Stream<T> ofNullable(T e) {
        if(e == null) {
            return Stream.empty();
        }
        
        Spliterator<T> spliterator = new Streams.StreamBuilderImpl<>(e);
        return StreamSupport.stream(spliterator, false);
    }
    
    /**
     * Creating a stream from an array is safe
     *
     * Returns a sequential ordered stream whose elements are the specified values.
     *
     * @param <T>    the type of stream elements
     * @param values the elements of the new stream
     *
     * @return the new stream
     */
    // 构造处于源头阶段的流，该流包含了指定数组(或类似数组的序列)中的元素
    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> Stream<T> of(T... values) {
        // 内部使用了"数组"Spliterator(引用类型版本)
        return Arrays.stream(values);
    }
    
    /**
     * Returns an infinite sequential ordered {@code Stream} produced by iterative
     * application of a function {@code f} to an initial element {@code seed},
     * producing a {@code Stream} consisting of {@code seed}, {@code f(seed)},
     * {@code f(f(seed))}, etc.
     *
     * <p>The first element (position {@code 0}) in the {@code Stream} will be
     * the provided {@code seed}.  For {@code n > 0}, the element at position
     * {@code n}, will be the result of applying the function {@code f} to the
     * element at position {@code n - 1}.
     *
     * <p>The action of applying {@code f} for one element
     * <a href="../concurrent/package-summary.html#MemoryVisibility"><i>happens-before</i></a>
     * the action of applying {@code f} for subsequent elements.  For any given
     * element the action may be performed in whatever thread the library
     * chooses.
     *
     * @param <T>  the type of stream elements
     * @param seed the initial element
     * @param next a function to be applied to the previous element to produce a new element
     *
     * @return a new sequential {@code Stream}
     */
    /*
     * 构造一个包含无限元素的流，仅支持单元素访问(如果遍历，则停不下来)
     *
     * 该流中的元素特征为：
     * 第一个元素是seed，
     * 第二个元素是由next处理第一个元素后返回的新元素，
     * 第三个元素是由next处理第二个元素后返回的新元素，
     * 以此类推...
     */
    static <T> Stream<T> iterate(final T seed, final UnaryOperator<T> next) {
        Objects.requireNonNull(next);
        
        /*
         * 构造Spliterator供Stream使用
         *
         * 注：没有重写forEachRemaining，tryAdvance也不会返回false，
         * 　　因此，在调用forEachRemaining时将陷入死循环。
         */
        Spliterator<T> spliterator = new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE) {
            boolean started; // 是否已经开启了访问
            T prev;          // 记录上一次访问过的元素
            
            /*
             * 尝试用action消费当前流迭代器中下一个元素。
             * 此处总是返回true，表示总是"存在"下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                
                T t;
                
                // 如果是首次访问，则需要访问seed
                if(!started) {
                    t = seed;
                    started = true; // 标记访问已开启
                    
                    // 如果不是首次访问，则使用fun处理上一次访问过的元素，并返回一个新元素
                } else {
                    t = next.apply(prev);
                }
                
                // 记录新元素
                prev = t;
                
                // 消费新元素
                action.accept(t);
                
                return true;
            }
        };
        
        // 构造处于源头(head)阶段的流(引用类型版本)
        return StreamSupport.stream(spliterator, false);
    }
    
    /**
     * Returns a sequential ordered {@code Stream} produced by iterative
     * application of the given {@code next} function to an initial element,
     * conditioned on satisfying the given {@code hasNext} predicate.  The
     * stream terminates as soon as the {@code hasNext} predicate returns false.
     *
     * <p>{@code Stream.iterate} should produce the same sequence of elements as
     * produced by the corresponding for-loop:
     * <pre>{@code
     *     for (T index=seed; hasNext.test(index); index = next.apply(index)) {
     *         ...
     *     }
     * }</pre>
     *
     * <p>The resulting sequence may be empty if the {@code hasNext} predicate
     * does not hold on the seed value.  Otherwise the first element will be the
     * supplied {@code seed} value, the next element (if present) will be the
     * result of applying the {@code next} function to the {@code seed} value,
     * and so on iteratively until the {@code hasNext} predicate indicates that
     * the stream should terminate.
     *
     * <p>The action of applying the {@code hasNext} predicate to an element
     * <a href="../concurrent/package-summary.html#MemoryVisibility"><i>happens-before</i></a>
     * the action of applying the {@code next} function to that element.  The
     * action of applying the {@code next} function for one element
     * <i>happens-before</i> the action of applying the {@code hasNext}
     * predicate for subsequent elements.  For any given element an action may
     * be performed in whatever thread the library chooses.
     *
     * @param <T>     the type of stream elements
     * @param seed    the initial element
     * @param hasNext a predicate to apply to elements to determine when the
     *                stream must terminate.
     * @param next    a function to be applied to the previous element to produce
     *                a new element
     *
     * @return a new sequential {@code Stream}
     *
     * @since 9
     */
    /*
     * 构造一个包含有限元素的流，既支持单元素访问，也支持批量访问(可以遍历)
     *
     * 该流中的元素特征为：
     * 第一个元素是seed，
     * 第二个元素是由next处理第一个元素后返回的新元素，
     * 第三个元素是由next处理第二个元素后返回的新元素，
     * 以此类推...
     *
     * 如果由next处理生成的新元素被hasNext识别为终止元素，则需要关闭访问
     */
    static <T> Stream<T> iterate(T seed, Predicate<? super T> hasNext, UnaryOperator<T> next) {
        Objects.requireNonNull(next);
        Objects.requireNonNull(hasNext);
    
        Spliterator<T> spliterator = new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE) {
            boolean started;   // 是否已经开启了访问
            boolean finished;  // 是否已经关闭了访问
            T prev;            // 记录上一次访问过的元素
        
            /*
             * 尝试用action消费当前流迭代器中下一个元素。
             * 返回值指示是否找到了下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);
            
                // 如果已经关闭了访问，直接返回false
                if(finished) {
                    return false;
                }
            
                T t;
            
                // 如果是首次访问，则需要访问seed
                if(!started) {
                    t = seed;
                    started = true; // 标记访问已开启
                
                    // 如果不是首次访问，则使用fun处理上一次访问过的元素，并返回一个新元素
                } else {
                    t = next.apply(prev);
                }
            
                // 如果遇到了终止元素，则关闭访问
                if(!hasNext.test(t)) {
                    prev = null;
                    finished = true; // 标记访问已关闭
                    return false;
                }
            
                // 记录新元素
                prev = t;
            
                // 消费新元素
                action.accept(t);
            
                return true;
            }
        
            /*
             * 尝试用action逐个消费当前流迭代器中所有剩余元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                Objects.requireNonNull(action);
            
                // 如果已经关闭了访问，直接返回false
                if(finished) {
                    return;
                }
            
                finished = true;
            
                T t;
            
                // 如果是首次访问，则需要访问seed
                if(!started) {
                    t = seed;
                
                    // 如果不是首次访问，则使用fun处理上一次访问过的元素，并返回一个新元素
                } else {
                    t = next.apply(prev);
                }
            
                prev = null;
            
                // 遍历剩余的所有元素
                while(hasNext.test(t)) {
                    // 消费新元素
                    action.accept(t);
                
                    // 生成新元素
                    t = next.apply(t);
                }
            }
        };
    
        // 构造处于源头(head)阶段的流(引用类型版本)
        return StreamSupport.stream(spliterator, false);
    }
    
    /**
     * Returns an infinite sequential unordered stream where each element is
     * generated by the provided {@code Supplier}.  This is suitable for
     * generating constant streams, streams of random elements, etc.
     *
     * @param <T>      the type of stream elements
     * @param supplier the {@code Supplier} of generated elements
     *
     * @return a new infinite sequential unordered {@code Stream}
     */
    // 构造一个包含无限元素的流，元素由supplier提供
    static <T> Stream<T> generate(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        
        // "无限"流迭代器(引用类型版本)
        Spliterator<T> spliterator = new OfRef<>(Long.MAX_VALUE, supplier);
        
        // 构造处于源头(head)阶段的流(引用类型版本)
        return StreamSupport.stream(spliterator, false);
    }
    
    /**
     * Creates a lazily concatenated stream whose elements are all the
     * elements of the first stream followed by all the elements of the
     * second stream.  The resulting stream is ordered if both
     * of the input streams are ordered, and parallel if either of the input
     * streams is parallel.  When the resulting stream is closed, the close
     * handlers for both input streams are invoked.
     *
     * <p>This method operates on the two input streams and binds each stream
     * to its source.  As a result subsequent modifications to an input stream
     * source may not be reflected in the concatenated stream result.
     *
     * @param <T> The type of stream elements
     * @param s1  the first stream
     * @param s2  the second stream
     *
     * @return the concatenation of the two input streams
     *
     * @implNote Use caution when constructing streams from repeated concatenation.
     * Accessing an element of a deeply concatenated stream can result in deep
     * call chains, or even {@code StackOverflowError}.
     *
     * <p>Subsequent changes to the sequential/parallel execution mode of the
     * returned stream are not guaranteed to be propagated to the input streams.
     * @apiNote To preserve optimization opportunities this method binds each stream to
     * its source and accepts only two streams as parameters.  For example, the
     * exact size of the concatenated stream source can be computed if the exact
     * size of each input stream source is known.
     * To concatenate more streams without binding, or without nested calls to
     * this method, try creating a stream of streams and flat-mapping with the
     * identity function, for example:
     * <pre>{@code
     *     Stream<T> concat = Stream.of(s1, s2, s3, s4).flatMap(s -> s);
     * }</pre>
     */
    // 构造一个由s1和s2拼接而成的流
    static <T> Stream<T> concat(Stream<? extends T> s1, Stream<? extends T> s2) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
    
        // "拼接"流迭代器(引用类型版本)
        @SuppressWarnings("unchecked")
        Spliterator<T> spliterator = new Streams.ConcatSpliterator.OfRef<>((Spliterator<T>) s1.spliterator(), (Spliterator<T>) s2.spliterator());
    
        // 组合流
        Stream<T> stream = StreamSupport.stream(spliterator, s1.isParallel() || s2.isParallel());
    
        // 将两个流的关闭动作串联
        Runnable s = Streams.composedClose(s1, s2);
    
        // 为stream注册关闭回调：当stream关闭时，同时将s1和s2也关闭
        return stream.onClose(s);
    }
    
    /*▲ 创建流的源头阶段 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中间操作-无状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a stream consisting of the elements of this stream that match
     * the given predicate.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to each element to determine if it
     *                  should be included
     *
     * @return the new stream
     */
    // 筛选数据。参数举例：x->x%2==0，筛选出所有偶数
    Stream<T> filter(Predicate<? super T> predicate);
    
    /**
     * Returns a stream consisting of the results of applying the given
     * function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param <R>    The element type of the new stream
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     *
     * @return the new stream
     */
    // 映射数据。参数举例：x->x*x，求平方后返回
    <R> Stream<R> map(Function<? super T, ? extends R> mapper);
    
    /**
     * Returns an {@code IntStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">
     * intermediate operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     *
     * @return the new stream
     */
    // 映射数据，返回一个IntStream。参数举例：s->s.length()（要求返回值为int），计算字符串长度
    IntStream mapToInt(ToIntFunction<? super T> mapper);
    
    /**
     * Returns a {@code LongStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     *
     * @return the new stream
     */
    // 映射数据，返回一个LongStream。参考#mapToInt
    LongStream mapToLong(ToLongFunction<? super T> mapper);
    
    /**
     * Returns a {@code DoubleStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     *
     * @return the new stream
     */
    // 映射数据，返回一个DoubleStream。参考#mapToInt
    DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);
    
    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.  Each mapped stream is
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed into this stream.  (If a mapped stream is {@code null}
     * an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param <R>    The element type of the new stream
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     *
     * @return the new stream
     *
     * @apiNote The {@code flatMap()} operation has the effect of applying a one-to-many
     * transformation to the elements of the stream, and then flattening the
     * resulting elements into a new stream.
     *
     * <p><b>Examples.</b>
     *
     * <p>If {@code orders} is a stream of purchase orders, and each purchase
     * order contains a collection of line items, then the following produces a
     * stream containing all the line items in all the orders:
     * <pre>{@code
     *     orders.flatMap(order -> order.getLineItems().stream())...
     * }</pre>
     *
     * <p>If {@code path} is the path to a file, then the following produces a
     * stream of the {@code words} contained in that file:
     * <pre>{@code
     *     Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8);
     *     Stream<String> words = lines.flatMap(line -> Stream.of(line.split(" +")));
     * }</pre>
     * The {@code mapper} function passed to {@code flatMap} splits a line,
     * using a simple regular expression, into an array of words, and then
     * creates a stream of words from that array.
     */
    /*
     * 数据降维。参数举例：l->l.stream()。
     *
     * 比如int[][] a = new int[]{new int[]{1,2,3}, new int[]{4,5}. new int[]{6}};，降维成一维数组：[1,2,3,4,5,6]
     * 注：一次只能降低一个维度，比如遇到三维数组，那么如果想要得到一维的数据，需要连续调用两次。
     *
     * mapper的作用就是将上游的高维容器中的元素提取出来放到流中，并在后续将该元素依次传递到下游，
     * 这就相当于把原来的元素从高维容器中提取出来了。
     */
    <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);
    
    /**
     * Returns an {@code IntStream} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element.  Each mapped
     * stream is {@link java.util.stream.BaseStream#close() closed} after its
     * contents have been placed into this stream.  (If a mapped stream is
     * {@code null} an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     *
     * @return the new stream
     *
     * @see #flatMap(Function)
     */
    // 数据降维。参考#flatMap。
    IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper);
    
    /**
     * Returns an {@code LongStream} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element.  Each mapped
     * stream is {@link java.util.stream.BaseStream#close() closed} after its
     * contents have been placed into this stream.  (If a mapped stream is
     * {@code null} an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     *
     * @return the new stream
     *
     * @see #flatMap(Function)
     */
    // 数据降维。参考#flatMap。
    LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper);
    
    /**
     * Returns an {@code DoubleStream} consisting of the results of replacing
     * each element of this stream with the contents of a mapped stream produced
     * by applying the provided mapping function to each element.  Each mapped
     * stream is {@link java.util.stream.BaseStream#close() closed} after its
     * contents have placed been into this stream.  (If a mapped stream is
     * {@code null} an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     *
     * @return the new stream
     *
     * @see #flatMap(Function)
     */
    // 数据降维。参考#flatMap。
    DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper);
    
    /**
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on each element as elements are consumed
     * from the resulting stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * <p>For parallel stream pipelines, the action may be called at
     * whatever time and in whatever thread the element is made available by the
     * upstream operation.  If the action modifies shared state,
     * it is responsible for providing the required synchronization.
     *
     * @param action a <a href="package-summary.html#NonInterference">
     *               non-interfering</a> action to perform on the elements as
     *               they are consumed from the stream
     *
     * @return the new stream
     *
     * @apiNote This method exists mainly to support debugging, where you want
     * to see the elements as they flow past a certain point in a pipeline:
     * <pre>{@code
     *     Stream.of("one", "two", "three", "four")
     *         .filter(e -> e.length() > 3)
     *         .peek(e -> System.out.println("Filtered value: " + e))
     *         .map(String::toUpperCase)
     *         .peek(e -> System.out.println("Mapped value: " + e))
     *         .collect(Collectors.toList());
     * }</pre>
     *
     * <p>In cases where the stream implementation is able to optimize away the
     * production of some or all the elements (such as with short-circuiting
     * operations like {@code findFirst}, or in the example described in
     * {@link #count}), the action will not be invoked for those elements.
     */
    /*
     * 用于查看流的内部结构，不会对流的结构产生影响。
     * peek常用来查看当前流的结构，比如输出其中的元素。
     */
    Stream<T> peek(Consumer<? super T> action);
    
    /*▲ 中间操作-无状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中间操作-有状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a stream consisting of the distinct elements (according to
     * {@link Object#equals(Object)}) of this stream.
     *
     * <p>For ordered streams, the selection of distinct elements is stable
     * (for duplicated elements, the element appearing first in the encounter
     * order is preserved.)  For unordered streams, no stability guarantees
     * are made.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">stateful
     * intermediate operation</a>.
     *
     * @return the new stream
     *
     * @apiNote Preserving stability for {@code distinct()} in parallel pipelines is
     * relatively expensive (requires that the operation act as a full barrier,
     * with substantial buffering overhead), and stability is often not needed.
     * Using an unordered stream source (such as {@link #generate(Supplier)})
     * or removing the ordering constraint with {@link #unordered()} may result
     * in significantly more efficient execution for {@code distinct()} in parallel
     * pipelines, if the semantics of your situation permit.  If consistency
     * with encounter order is required, and you are experiencing poor performance
     * or memory utilization with {@code distinct()} in parallel pipelines,
     * switching to sequential execution with {@link #sequential()} may improve
     * performance.
     */
    // 去重
    Stream<T> distinct();
    
    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to natural order.  If the elements of this stream are not
     * {@code Comparable}, a {@code java.lang.ClassCastException} may be thrown
     * when the terminal operation is executed.
     *
     * <p>For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">stateful
     * intermediate operation</a>.
     *
     * @return the new stream
     */
    // 排序（默认升序）
    Stream<T> sorted();
    
    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the provided {@code Comparator}.
     *
     * <p>For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">stateful
     * intermediate operation</a>.
     *
     * @param comparator a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                   <a href="package-summary.html#Statelessness">stateless</a>
     *                   {@code Comparator} to be used to compare stream elements
     *
     * @return the new stream
     */
    // 排序（使用comparator制定排序规则）
    Stream<T> sorted(Comparator<? super T> comparator);
    
    /**
     * Returns a stream consisting of the elements of this stream, truncated
     * to be no longer than {@code maxSize} in length.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * stateful intermediate operation</a>.
     *
     * @param maxSize the number of elements the stream should be limited to
     *
     * @return the new stream
     *
     * @throws IllegalArgumentException if {@code maxSize} is negative
     * @apiNote While {@code limit()} is generally a cheap operation on sequential
     * stream pipelines, it can be quite expensive on ordered parallel pipelines,
     * especially for large values of {@code maxSize}, since {@code limit(n)}
     * is constrained to return not just any <em>n</em> elements, but the
     * <em>first n</em> elements in the encounter order.  Using an unordered
     * stream source (such as {@link #generate(Supplier)}) or removing the
     * ordering constraint with {@link #unordered()} may result in significant
     * speedups of {@code limit()} in parallel pipelines, if the semantics of
     * your situation permit.  If consistency with encounter order is required,
     * and you are experiencing poor performance or memory utilization with
     * {@code limit()} in parallel pipelines, switching to sequential execution
     * with {@link #sequential()} may improve performance.
     */
    // 只显示前maxSize个元素
    Stream<T> limit(long maxSize);
    
    /**
     * Returns a stream consisting of the remaining elements of this stream
     * after discarding the first {@code n} elements of the stream.
     * If this stream contains fewer than {@code n} elements then an
     * empty stream will be returned.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">stateful
     * intermediate operation</a>.
     *
     * @param n the number of leading elements to skip
     *
     * @return the new stream
     *
     * @throws IllegalArgumentException if {@code n} is negative
     * @apiNote While {@code skip()} is generally a cheap operation on sequential
     * stream pipelines, it can be quite expensive on ordered parallel pipelines,
     * especially for large values of {@code n}, since {@code skip(n)}
     * is constrained to skip not just any <em>n</em> elements, but the
     * <em>first n</em> elements in the encounter order.  Using an unordered
     * stream source (such as {@link #generate(Supplier)}) or removing the
     * ordering constraint with {@link #unordered()} may result in significant
     * speedups of {@code skip()} in parallel pipelines, if the semantics of
     * your situation permit.  If consistency with encounter order is required,
     * and you are experiencing poor performance or memory utilization with
     * {@code skip()} in parallel pipelines, switching to sequential execution
     * with {@link #sequential()} may improve performance.
     */
    // 跳过前n个元素
    Stream<T> skip(long n);
    
    /**
     * Returns, if this stream is ordered, a stream consisting of the longest
     * prefix of elements taken from this stream that match the given predicate.
     * Otherwise returns, if this stream is unordered, a stream consisting of a
     * subset of elements taken from this stream that match the given predicate.
     *
     * <p>If this stream is ordered then the longest prefix is a contiguous
     * sequence of elements of this stream that match the given predicate.  The
     * first element of the sequence is the first element of this stream, and
     * the element immediately following the last element of the sequence does
     * not match the given predicate.
     *
     * <p>If this stream is unordered, and some (but not all) elements of this
     * stream match the given predicate, then the behavior of this operation is
     * nondeterministic; it is free to take any subset of matching elements
     * (which includes the empty set).
     *
     * <p>Independent of whether this stream is ordered or unordered if all
     * elements of this stream match the given predicate then this operation
     * takes all elements (the result is the same as the input), or if no
     * elements of the stream match the given predicate then no elements are
     * taken (the result is an empty stream).
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * stateful intermediate operation</a>.
     *
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to elements to determine the longest
     *                  prefix of elements.
     *
     * @return the new stream
     *
     * @implSpec The default implementation obtains the {@link #spliterator() spliterator}
     * of this stream, wraps that spliterator so as to support the semantics
     * of this operation on traversal, and returns a new stream associated with
     * the wrapped spliterator.  The returned stream preserves the execution
     * characteristics of this stream (namely parallel or sequential execution
     * as per {@link #isParallel()}) but the wrapped spliterator may choose to
     * not support splitting.  When the returned stream is closed, the close
     * handlers for both the returned and this stream are invoked.
     * @apiNote While {@code takeWhile()} is generally a cheap operation on sequential
     * stream pipelines, it can be quite expensive on ordered parallel
     * pipelines, since the operation is constrained to return not just any
     * valid prefix, but the longest prefix of elements in the encounter order.
     * Using an unordered stream source (such as {@link #generate(Supplier)}) or
     * removing the ordering constraint with {@link #unordered()} may result in
     * significant speedups of {@code takeWhile()} in parallel pipelines, if the
     * semantics of your situation permit.  If consistency with encounter order
     * is required, and you are experiencing poor performance or memory
     * utilization with {@code takeWhile()} in parallel pipelines, switching to
     * sequential execution with {@link #sequential()} may improve performance.
     * @since 9
     */
    // "保存前缀"：保存起初遇到的满足predicate条件的元素；只要遇到首个不满足条件的元素，就结束后续的保存动作
    default Stream<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        
        // 返回当前阶段的流的流迭代器
        Spliterator<T> spliterator = spliterator();
        
        // 返回"保存前缀"流迭代器，这是"无序前缀"流迭代器的一种
        Spliterator<T> takeSpliterator = new Taking<>(spliterator, true, predicate);
        
        // 构造处于源头(head)阶段的流(引用类型版本)
        Stream<T> stream = StreamSupport.stream(takeSpliterator, isParallel());
        
        /* Reuses the unordered spliterator, which, when encounter is present, is safe to use as long as it configured not to split */
        // 为stream注册关闭回调：如果stream关闭了，也顺便将当前流关闭
        return stream.onClose(this::close);
    }
    
    /**
     * Returns, if this stream is ordered, a stream consisting of the remaining
     * elements of this stream after dropping the longest prefix of elements
     * that match the given predicate.  Otherwise returns, if this stream is
     * unordered, a stream consisting of the remaining elements of this stream
     * after dropping a subset of elements that match the given predicate.
     *
     * <p>If this stream is ordered then the longest prefix is a contiguous
     * sequence of elements of this stream that match the given predicate.  The
     * first element of the sequence is the first element of this stream, and
     * the element immediately following the last element of the sequence does
     * not match the given predicate.
     *
     * <p>If this stream is unordered, and some (but not all) elements of this
     * stream match the given predicate, then the behavior of this operation is
     * nondeterministic; it is free to drop any subset of matching elements
     * (which includes the empty set).
     *
     * <p>Independent of whether this stream is ordered or unordered if all
     * elements of this stream match the given predicate then this operation
     * drops all elements (the result is an empty stream), or if no elements of
     * the stream match the given predicate then no elements are dropped (the
     * result is the same as the input).
     *
     * <p>This is a <a href="package-summary.html#StreamOps">stateful
     * intermediate operation</a>.
     *
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to elements to determine the longest
     *                  prefix of elements.
     *
     * @return the new stream
     *
     * @implSpec The default implementation obtains the {@link #spliterator() spliterator}
     * of this stream, wraps that spliterator so as to support the semantics
     * of this operation on traversal, and returns a new stream associated with
     * the wrapped spliterator.  The returned stream preserves the execution
     * characteristics of this stream (namely parallel or sequential execution
     * as per {@link #isParallel()}) but the wrapped spliterator may choose to
     * not support splitting.  When the returned stream is closed, the close
     * handlers for both the returned and this stream are invoked.
     * @apiNote While {@code dropWhile()} is generally a cheap operation on sequential
     * stream pipelines, it can be quite expensive on ordered parallel
     * pipelines, since the operation is constrained to return not just any
     * valid prefix, but the longest prefix of elements in the encounter order.
     * Using an unordered stream source (such as {@link #generate(Supplier)}) or
     * removing the ordering constraint with {@link #unordered()} may result in
     * significant speedups of {@code dropWhile()} in parallel pipelines, if the
     * semantics of your situation permit.  If consistency with encounter order
     * is required, and you are experiencing poor performance or memory
     * utilization with {@code dropWhile()} in parallel pipelines, switching to
     * sequential execution with {@link #sequential()} may improve performance.
     * @since 9
     */
    // "丢弃前缀"：丢弃起初遇到的满足predicate条件的元素；只要遇到首个不满足条件的元素，就开始保存它后及其后面的元素
    default Stream<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        
        // 返回当前阶段的流的流迭代器
        Spliterator<T> spliterator = spliterator();
        
        // 返回"丢弃前缀"流迭代器，这是"无序前缀"流迭代器的一种
        Spliterator<T> dropSpliterator = new Dropping<>(spliterator, true, predicate);
        
        // 构造处于源头(head)阶段的流(引用类型版本)
        Stream<T> stream = StreamSupport.stream(dropSpliterator, isParallel());
        
        /* Reuses the unordered spliterator, which, when encounter is present, is safe to use as long as it configured not to split */
        // 为stream注册关闭回调：如果stream关闭了，也顺便将当前流关闭
        return stream.onClose(this::close);
    }
    
    /*▲ 中间操作-有状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作-非短路操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an array containing the elements of this stream.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @return an array, whose {@linkplain Class#getComponentType runtime component
     * type} is {@code Object}, containing the elements of this stream
     */
    // 将数据存入Object数组返回
    Object[] toArray();
    
    /**
     * Returns an array containing the elements of this stream, using the
     * provided {@code generator} function to allocate the returned array, as
     * well as any additional arrays that might be required for a partitioned
     * execution or for resizing.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param <A>       the component type of the resulting array
     * @param generator a function which produces a new array of the desired
     *                  type and the provided length
     *
     * @return an array containing the elements in this stream
     *
     * @throws ArrayStoreException if the runtime type of any element of this
     *                             stream is not assignable to the {@linkplain Class#getComponentType
     *                             runtime component type} of the generated array
     * @apiNote The generator function takes an integer, which is the size of the
     * desired array, and produces an array of the desired size.  This can be
     * concisely expressed with an array constructor reference:
     * <pre>{@code
     *     Person[] men = people.stream()
     *                          .filter(p -> p.getGender() == MALE)
     *                          .toArray(Person[]::new);
     * }</pre>
     */
    // 将数据存入指定类型的数组后返回
    <A> A[] toArray(IntFunction<A[]> generator);
    
    /**
     * Performs an action for each element of this stream.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * <p>The behavior of this operation is explicitly nondeterministic.
     * For parallel stream pipelines, this operation does <em>not</em>
     * guarantee to respect the encounter order of the stream, as doing so
     * would sacrifice the benefit of parallelism.  For any given element, the
     * action may be performed at whatever time and in whatever thread the
     * library chooses.  If the action accesses shared state, it is
     * responsible for providing the required synchronization.
     *
     * @param action a <a href="package-summary.html#NonInterference">
     *               non-interfering</a> action to perform on the elements
     */
    // 遍历，并执行action操作
    void forEach(Consumer<? super T> action);
    
    /**
     * Performs an action for each element of this stream, in the encounter
     * order of the stream if the stream has a defined encounter order.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * <p>This operation processes the elements one at a time, in encounter
     * order if one exists.  Performing the action for one element
     * <a href="../concurrent/package-summary.html#MemoryVisibility"><i>happens-before</i></a>
     * performing the action for subsequent elements, but for any given element,
     * the action may be performed in whatever thread the library chooses.
     *
     * @param action a <a href="package-summary.html#NonInterference">
     *               non-interfering</a> action to perform on the elements
     *
     * @see #forEach(Consumer)
     */
    // 按遭遇顺序遍历，并执行action操作
    void forEachOrdered(Consumer<? super T> action);
    
    /**
     * Returns the minimum element of this stream according to the provided
     * {@code Comparator}.  This is a special case of a
     * <a href="package-summary.html#Reduction">reduction</a>.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal operation</a>.
     *
     * @param comparator a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                   <a href="package-summary.html#Statelessness">stateless</a>
     *                   {@code Comparator} to compare elements of this stream
     *
     * @return an {@code Optional} describing the minimum element of this stream,
     * or an empty {@code Optional} if the stream is empty
     *
     * @throws NullPointerException if the minimum element is null
     */
    // 求最小值，元素的排序规则取决于comparator
    Optional<T> min(Comparator<? super T> comparator);
    
    /**
     * Returns the maximum element of this stream according to the provided
     * {@code Comparator}.  This is a special case of a
     * <a href="package-summary.html#Reduction">reduction</a>.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param comparator a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                   <a href="package-summary.html#Statelessness">stateless</a>
     *                   {@code Comparator} to compare elements of this stream
     *
     * @return an {@code Optional} describing the maximum element of this stream,
     * or an empty {@code Optional} if the stream is empty
     *
     * @throws NullPointerException if the maximum element is null
     */
    // 求最大值，元素的排序规则取决于comparator
    Optional<T> max(Comparator<? super T> comparator);
    
    /**
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using an
     * <a href="package-summary.html#Associativity">associative</a> accumulation
     * function, and returns an {@code Optional} describing the reduced value,
     * if any. This is equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     T result = null;
     *     for (T element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.apply(result, element);
     *     }
     *     return foundAny ? Optional.of(result) : Optional.empty();
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code accumulator} function must be an
     * <a href="package-summary.html#Associativity">associative</a> function.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values
     *
     * @return an {@link Optional} describing the result of the reduction
     *
     * @throws NullPointerException if the result of the reduction is null
     * @see #reduce(Object, BinaryOperator)
     * @see #min(Comparator)
     * @see #max(Comparator)
     */
    /*
     * 无初始状态的汇总操作(引用类型版本)
     *
     * 尝试将遇到的每个数据与上一个状态做accumulator操作后，将汇总结果保存到上一次的状态值中。
     * 未设置初始状态，所以每个(子)任务只是专注处理它自身遇到的数据源。
     *
     * 例如：
     * Stream.of(1, 2, 3, 4, 5).reduce((a, b) -> a + b)
     * 这会将1、2、3、4、5累加起来。
     *
     * accumulator: 两种用途：
     *              1.用于择取操作，如果是并行流，则用在每个子任务中
     *              2.用于并行流的合并操作
     */
    Optional<T> reduce(BinaryOperator<T> accumulator);
    
    /**
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using the provided identity value and an
     * <a href="package-summary.html#Associativity">associative</a>
     * accumulation function, and returns the reduced value.  This is equivalent
     * to:
     * <pre>{@code
     *     T result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code identity} value must be an identity for the accumulator
     * function. This means that for all {@code t},
     * {@code accumulator.apply(identity, t)} is equal to {@code t}.
     * The {@code accumulator} function must be an
     * <a href="package-summary.html#Associativity">associative</a> function.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param identity    the identity value for the accumulating function
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values
     *
     * @return the result of the reduction
     *
     * @apiNote Sum, min, max, average, and string concatenation are all special
     * cases of reduction. Summing a stream of numbers can be expressed as:
     *
     * <pre>{@code
     *     Integer sum = integers.reduce(0, (a, b) -> a+b);
     * }</pre>
     *
     * or:
     *
     * <pre>{@code
     *     Integer sum = integers.reduce(0, Integer::sum);
     * }</pre>
     *
     * <p>While this may seem a more roundabout way to perform an aggregation
     * compared to simply mutating a running total in a loop, reduction
     * operations parallelize more gracefully, without needing additional
     * synchronization and with greatly reduced risk of data races.
     */
    /*
     * 有初始状态的汇总操作(引用类型版本)
     *
     * 尝试将遇到的每个数据与上一个状态做汇总操作后，将汇总结果保存到上一次的状态值中。
     * 这里提供了两个操作：reducer用于在单个任务中择取数据，而combiner用于在并行流中合并多个子任务。
     * 这里需要设定一个初始状态seed，所以每个(子)任务在处理它自身遇到的数据源之前，首先要与该初始状态进行汇总。
     *
     * 例如：
     * Stream.of(1, 2, 3, 4, 5).reduce(-1, (a, b) -> a + b)
     * 这是顺序流，操作结果是将-1、1、2、3、4、5累加起来，结果是14。
     *
     * Stream.of(1, 2, 3, 4, 5).parallel().reduce(-1, (a, b) -> a + b)
     * 这是并行流，虽然使用的择取方法与顺序流相同，但不同的是这里需要先将数据源拆分到各个子任务中。
     * 根据默认的二分法拆分规则，上面的数据会被拆分为(1)、(2)、(3)、(4)、(5)这五组，
     * 由于这五组数据位于五个子任务中，那么每个子任务择取数据之时都会先与那个初始值-1去做汇总，
     * 即五个子任务的执行结果分别是：0、1、2、3、4，
     * 最后，将这5个子任务用accumulator再合并起来，那就是0+1+2+3+4 = 10
     *
     * identity   : 每个(子)任务需要使用的初始状态
     * accumulator: 既用于择取操作，如果是并行流，则用在每个叶子任务中，也用于并行流的合并子任务操作
     */
    T reduce(T identity, BinaryOperator<T> accumulator);
    
    /**
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using the provided identity, accumulation and
     * combining functions.  This is equivalent to:
     * <pre>{@code
     *     U result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code identity} value must be an identity for the combiner
     * function.  This means that for all {@code u}, {@code combiner(identity, u)}
     * is equal to {@code u}.  Additionally, the {@code combiner} function
     * must be compatible with the {@code accumulator} function; for all
     * {@code u} and {@code t}, the following must hold:
     * <pre>{@code
     *     combiner.apply(u, accumulator.apply(identity, t)) == accumulator.apply(u, t)
     * }</pre>
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param <U>         The type of the result
     * @param identity    the identity value for the combiner function
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for incorporating an additional element into a result
     * @param combiner    an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values, which must be
     *                    compatible with the accumulator function
     *
     * @return the result of the reduction
     *
     * @apiNote Many reductions using this form can be represented more simply
     * by an explicit combination of {@code map} and {@code reduce} operations.
     * The {@code accumulator} function acts as a fused mapper and accumulator,
     * which can sometimes be more efficient than separate mapping and reduction,
     * such as when knowing the previously reduced value allows you to avoid
     * some computation.
     * @see #reduce(BinaryOperator)
     * @see #reduce(Object, BinaryOperator)
     */
    /*
     * 有初始状态的汇总操作(引用类型版本)
     *
     * 尝试将遇到的每个数据与上一个状态做汇总操作后，将汇总结果保存到上一次的状态值中。
     * 这里提供了两个操作：accumulator用于在单个任务中择取数据，而combiner用于在并行流中合并多个子任务。
     * 这里需要设定一个初始状态identity，所以每个(子)任务在处理它自身遇到的数据源之前，首先要与该初始状态进行汇总。
     *
     * 例如：
     * Stream.of(1, 2, 3, 4, 5).reduce(-1, (a, b) -> a + b, (a, b) -> a + b)
     * 这是顺序流，操作结果是将-1、1、2、3、4、5累加起来，结果是14。
     *
     * Stream.of(1, 2, 3, 4, 5).parallel().reduce(-1, (a, b) -> a + b, (a, b) -> a + b)
     * 这是并行流，虽然使用的择取方法与顺序流相同，但不同的是这里需要先将数据源拆分到各个子任务中。
     * 根据默认的二分法拆分规则，上面的数据会被拆分为(1)、(2)、(3)、(4)、(5)这五组，
     * 由于这五组数据位于五个子任务中，那么每个子任务择取数据之时都会先与那个初始值-1去做汇总，
     * 即五个子任务的执行结果分别是：0、1、2、3、4，
     * 最后，将这5个子任务用combiner合并起来，那就是0+1+2+3+4 = 10
     *
     * identity   : 每个(子)任务需要使用的初始状态
     * accumulator: 用于择取操作，如果是并行流，则用在每个叶子任务中
     * combiner   : 用于并行流的合并子任务操作
     *
     * 注：通常来讲，要求accumulator和combiner相呼应
     */
    <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);
    
    /**
     * Performs a <a href="package-summary.html#MutableReduction">mutable
     * reduction</a> operation on the elements of this stream.  A mutable
     * reduction is one in which the reduced value is a mutable result container,
     * such as an {@code ArrayList}, and elements are incorporated by updating
     * the state of the result rather than by replacing the result.  This
     * produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (T element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     *
     * <p>Like {@link #reduce(Object, BinaryOperator)}, {@code collect} operations
     * can be parallelized without requiring additional synchronization.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param <R>         the type of the mutable result container
     * @param supplier    a function that creates a new mutable result container.
     *                    For a parallel execution, this function may be called
     *                    multiple times and must return a fresh value each time.
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function that must fold an element into a result
     *                    container.
     * @param combiner    an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function that accepts two partial result containers
     *                    and merges them, which must be compatible with the
     *                    accumulator function.  The combiner function must fold
     *                    the elements from the second result container into the
     *                    first result container.
     *
     * @return the result of the reduction
     *
     * @apiNote There are many existing classes in the JDK whose signatures are
     * well-suited for use with method references as arguments to {@code collect()}.
     * For example, the following will accumulate strings into an {@code ArrayList}:
     * <pre>{@code
     *     List<String> asList = stringStream.collect(ArrayList::new, ArrayList::add,
     *                                                ArrayList::addAll);
     * }</pre>
     *
     * <p>The following will take a stream of strings and concatenates them into a
     * single string:
     * <pre>{@code
     *     String concat = stringStream.collect(StringBuilder::new, StringBuilder::append,
     *                                          StringBuilder::append)
     *                                 .toString();
     * }</pre>
     */
    /*
     * 有初始状态的消费操作(引用类型版本)
     *
     * 注：这里的消费通常是将遇到的元素存储到某个容器中。
     *
     * 尝试将遇到的每个数据与上一个状态做汇总操作后，汇总过程是一个消费过程，在消费中如何处理状态值，由该方法的入参决定。
     * 通常来说，我们会让supplier生成一个代表容器的"初始状态"，然后在消费过程中，把遇到的元素收纳到该容器当中。
     * 这里提供了两个操作：accumulator用于在单个任务中择取数据，而combiner用于在并行流中合并多个子任务。
     * 这里需要设定一个初始状态的工厂supplier，所以每个(子)任务在处理它自身遇到的数据源之前，首先要与该初始状态进行汇总。
     *
     * 例如：
     *
     * 假设有如下两个操作：
     * BiConsumer<ArrayList<Integer>, Integer> accumulator = (list, e) -> list.add(e);
     *
     * BiConsumer<ArrayList<Integer>, ArrayList<Integer>> combiner = (list1, list2) -> {
     *     for(Integer e : list2) {
     *         if(!list1.contains(e)) {
     *             list1.add(e);
     *         }
     *     }
     * };
     *
     *
     * Stream<Integer> stream = Stream.of(3, 2, 3, 1, 2);
     * ArrayList<Integer> list = stream.collect(() -> new ArrayList<Integer>(), accumulator, combiner);
     * 这是顺序流，操作结果是将3、2、3、1、2全部收集到list中。
     *
     * Stream<Integer> stream = Stream.of(3, 2, 3, 1, 2).parallel();
     * ArrayList<Integer> list = stream.collect(() -> new ArrayList<Integer>(), accumulator, combiner);
     * 这是并行流，虽然使用的择取方法与顺序流相同，但不同的是这里需要先将数据源拆分到各个子任务中
     * 根据默认的二分法拆分规则，上面的数据会被拆分为(1)、(2)、(3)、(4)、(5)这五组
     * 由于这五组数据位于五个子任务中，那么每个子任务择取数据之时都会先与那个初始状态做汇总。
     * 此处给出的初始状态就是一个list，操作目标就是将遇到的元素添加到该list中。
     * 因此在每个叶子任务完成后，其对应的元素就被添加到了list中。
     * 接下来，使用combiner对子任务汇总。这里的操作是遍历list2中的元素，找出那些不在list1中的元素，并将其添加到list1中。
     * 因此最终的汇总结果中只有3、2、1。
     *
     * supplier   : 初始状态工厂
     * accumulator: 用于择取操作，如果是并行流，则用在每个叶子任务中
     * combiner   : 用于并行流的合并子任务操作
     *
     * 注：通常来讲，要求accumulator和combiner相呼应
     */
    <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);
    
    /**
     * Performs a <a href="package-summary.html#MutableReduction">mutable
     * reduction</a> operation on the elements of this stream using a
     * {@code Collector}.  A {@code Collector}
     * encapsulates the functions used as arguments to
     * {@link #collect(Supplier, BiConsumer, BiConsumer)}, allowing for reuse of
     * collection strategies and composition of collect operations such as
     * multiple-level grouping or partitioning.
     *
     * <p>If the stream is parallel, and the {@code Collector}
     * is {@link Collector.Characteristics#CONCURRENT concurrent}, and
     * either the stream is unordered or the collector is
     * {@link Collector.Characteristics#UNORDERED unordered},
     * then a concurrent reduction will be performed (see {@link Collector} for
     * details on concurrent reduction.)
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * <p>When executed in parallel, multiple intermediate results may be
     * instantiated, populated, and merged so as to maintain isolation of
     * mutable data structures.  Therefore, even when executed in parallel
     * with non-thread-safe data structures (such as {@code ArrayList}), no
     * additional synchronization is needed for a parallel reduction.
     *
     * @param <R>       the type of the result
     * @param <A>       the intermediate accumulation type of the {@code Collector}
     * @param collector the {@code Collector} describing the reduction
     *
     * @return the result of the reduction
     *
     * @apiNote The following will accumulate strings into an ArrayList:
     * <pre>{@code
     *     List<String> asList = stringStream.collect(Collectors.toList());
     * }</pre>
     *
     * <p>The following will classify {@code Person} objects by city:
     * <pre>{@code
     *     Map<String, List<Person>> peopleByCity
     *         = personStream.collect(Collectors.groupingBy(Person::getCity));
     * }</pre>
     *
     * <p>The following will classify {@code Person} objects by state and city,
     * cascading two {@code Collector}s together:
     * <pre>{@code
     *     Map<String, Map<String, List<Person>>> peopleByStateAndCity
     *         = personStream.collect(Collectors.groupingBy(Person::getState,
     *                                                      Collectors.groupingBy(Person::getCity)));
     * }</pre>
     * @see #collect(Supplier, BiConsumer, BiConsumer)
     * @see Collectors
     */
    // 依赖于收集器的收集操作
    <R, A> R collect(Collector<? super T, A, R> collector);
    
    /**
     * Returns the count of elements in this stream.  This is a special case of
     * a <a href="package-summary.html#Reduction">reduction</a> and is
     * equivalent to:
     * <pre>{@code
     *     return mapToLong(e -> 1L).sum();
     * }</pre>
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal operation</a>.
     *
     * @return the count of elements in this stream
     *
     * @apiNote An implementation may choose to not execute the stream pipeline (either
     * sequentially or in parallel) if it is capable of computing the count
     * directly from the stream source.  In such cases no source elements will
     * be traversed and no intermediate operations will be evaluated.
     * Behavioral parameters with side-effects, which are strongly discouraged
     * except for harmless cases such as debugging, may be affected.  For
     * example, consider the following stream:
     * <pre>{@code
     *     List<String> l = Arrays.asList("A", "B", "C", "D");
     *     long count = l.stream().peek(System.out::println).count();
     * }</pre>
     * The number of elements covered by the stream source, a {@code List}, is
     * known and the intermediate operation, {@code peek}, does not inject into
     * or remove elements from the stream (as may be the case for
     * {@code flatMap} or {@code filter} operations).  Thus the count is the
     * size of the {@code List} and there is no need to execute the pipeline
     * and, as a side-effect, print out the list elements.
     */
    // 计数
    long count();
    
    /*▲ 终端操作-非短路操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作-短路操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an {@link Optional} describing the first element of this stream,
     * or an empty {@code Optional} if the stream is empty.  If the stream has
     * no encounter order, then any element may be returned.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * terminal operation</a>.
     *
     * @return an {@code Optional} describing the first element of this stream,
     * or an empty {@code Optional} if the stream is empty
     *
     * @throws NullPointerException if the element selected is null
     */
    // 找出第一个元素
    Optional<T> findFirst();
    
    /**
     * Returns an {@link Optional} describing some element of the stream, or an
     * empty {@code Optional} if the stream is empty.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * terminal operation</a>.
     *
     * <p>The behavior of this operation is explicitly nondeterministic; it is
     * free to select any element in the stream.  This is to allow for maximal
     * performance in parallel operations; the cost is that multiple invocations
     * on the same source may not return the same result.  (If a stable result
     * is desired, use {@link #findFirst()} instead.)
     *
     * @return an {@code Optional} describing some element of this stream, or an
     * empty {@code Optional} if the stream is empty
     *
     * @throws NullPointerException if the element selected is null
     * @see #findFirst()
     */
    // 找到一个元素就返回，不管是不是第一个元素
    Optional<T> findAny();
    
    /**
     * Returns whether any elements of this stream match the provided
     * predicate.  May not evaluate the predicate on all elements if not
     * necessary for determining the result.  If the stream is empty then
     * {@code false} is returned and the predicate is not evaluated.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * terminal operation</a>.
     *
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to elements of this stream
     *
     * @return {@code true} if any elements of the stream match the provided
     * predicate, otherwise {@code false}
     *
     * @apiNote This method evaluates the <em>existential quantification</em> of the
     * predicate over the elements of the stream (for some x P(x)).
     */
    // 判断是否存在元素满足predicate条件
    boolean anyMatch(Predicate<? super T> predicate);
    
    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.  If the stream is empty then {@code true} is
     * returned and the predicate is not evaluated.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * terminal operation</a>.
     *
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to elements of this stream
     *
     * @return {@code true} if either all elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     *
     * @apiNote This method evaluates the <em>universal quantification</em> of the
     * predicate over the elements of the stream (for all x P(x)).  If the
     * stream is empty, the quantification is said to be <em>vacuously
     * satisfied</em> and is always {@code true} (regardless of P(x)).
     */
    // 判断是否所有元素满足predicate条件
    boolean allMatch(Predicate<? super T> predicate);
    
    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.  If the stream is empty then {@code true} is
     * returned and the predicate is not evaluated.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * terminal operation</a>.
     *
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to elements of this stream
     *
     * @return {@code true} if either no elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     *
     * @apiNote This method evaluates the <em>universal quantification</em> of the
     * negated predicate over the elements of the stream (for all x ~P(x)).  If
     * the stream is empty, the quantification is said to be vacuously satisfied
     * and is always {@code true}, regardless of P(x).
     */
    // 判断是否没有元素满足predicate条件
    boolean noneMatch(Predicate<? super T> predicate);
    
    /*▲ 终端操作-短路操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns a builder for a {@code Stream}.
     *
     * @param <T> type of elements
     *
     * @return a stream builder
     */
    // 返回一个Stream构建器
    static <T> Builder<T> builder() {
        return new Streams.StreamBuilderImpl<>();
    }
    
    
    /**
     * A mutable builder for a {@code Stream}.  This allows the creation of a
     * {@code Stream} by generating elements individually and adding them to the
     * {@code Builder} (without the copying overhead that comes from using
     * an {@code ArrayList} as a temporary buffer.)
     *
     * <p>A stream builder has a lifecycle, which starts in a building
     * phase, during which elements can be added, and then transitions to a built
     * phase, after which elements may not be added.  The built phase begins
     * when the {@link #build()} method is called, which creates an ordered
     * {@code Stream} whose elements are the elements that were added to the stream
     * builder, in the order they were added.
     *
     * @param <T> the type of stream elements
     *
     * @see Stream#builder()
     * @since 1.8
     */
    // Stream构建器接口，允许创建单元素流或多元素流
    interface Builder<T> extends Consumer<T> {
        
        /**
         * Builds the stream, transitioning this builder to the built state.
         * An {@code IllegalStateException} is thrown if there are further attempts
         * to operate on the builder after it has entered the built state.
         *
         * @return the built stream
         *
         * @throws IllegalStateException if the builder has already transitioned to
         *                               the built state
         */
        // 构建单元素流或多元素流
        Stream<T> build();
        
        /**
         * Adds an element to the stream being built.
         *
         * @param t the element to add
         *
         * @return {@code this} builder
         *
         * @throws IllegalStateException if the builder has already transitioned to
         *                               the built state
         * @implSpec The default implementation behaves as if:
         * <pre>{@code
         *     accept(t)
         *     return this;
         * }</pre>
         */
        // 如果当前流构建器处于[待完成]状态，则可以向其中添加元素
        default Builder<T> add(T t) {
            accept(t);
            return this;
        }
        
        /**
         * Adds an element to the stream being built.
         *
         * @throws IllegalStateException if the builder has already transitioned to
         *                               the built state
         */
        // 添加元素
        @Override
        void accept(T t);
        
    }
    
}
