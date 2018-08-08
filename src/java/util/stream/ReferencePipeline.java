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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code U}.
 *
 * @param <P_IN>  type of elements in the upstream source
 * @param <P_OUT> type of elements in produced by this stage
 *
 * @since 1.8
 */
// 实现了绝大部分流水线操作，并定义了流的三个阶段，用于处理引用类型的元素
abstract class ReferencePipeline<P_IN, P_OUT>
    extends AbstractPipeline<P_IN, P_OUT, Stream<P_OUT>>
    implements Stream<P_OUT> {
    
    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source      {@code Supplier<Spliterator>} describing the stream source
     * @param sourceFlags the source flags for the stream source, described in
     *                    {@link StreamOpFlag}
     * @param parallel    {@code true} if the pipeline is parallel
     */
    // 构造Stream，常用于构建流的源头阶段（HEAD），需要从source中提取Spliterator
    ReferencePipeline(Supplier<? extends Spliterator<?>> source, int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }
    
    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source      {@code Spliterator} describing the stream source
     * @param sourceFlags The source flags for the stream source, described in
     *                    {@link StreamOpFlag}
     * @param parallel    {@code true} if the pipeline is parallel
     */
    // 构造Stream，常用于构建流的源头阶段（HEAD）
    ReferencePipeline(Spliterator<?> source, int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }
    
    /**
     * Constructor for appending an intermediate operation onto an existing
     * pipeline.
     *
     * @param upstream the upstream element source.
     */
    // 构造Stream，常用于构建流的中间阶段，包括有状态流和无状态流。upstream是调用此构造方法的流
    ReferencePipeline(AbstractPipeline<?, P_IN, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }
    
    
    
    /*▼ 创建流的源头阶段 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    @Override
    public final Stream<P_OUT> takeWhile(Predicate<? super P_OUT> predicate) {
        return WhileOps.makeTakeWhileRef(this, predicate);
    }
    
    @Override
    public final Stream<P_OUT> dropWhile(Predicate<? super P_OUT> predicate) {
        return WhileOps.makeDropWhileRef(this, predicate);
    }
    
    /*▲ 创建流的源头阶段 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中间操作-无状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 筛选数据。参数举例：x->x%2==0，筛选出所有偶数
    @Override
    public final Stream<P_OUT> filter(Predicate<? super P_OUT> predicate) {
        Objects.requireNonNull(predicate);
        
        // 返回一个无状态的流
        return new StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SIZED) {
            // 从后向前包装SInk。传入下一个阶段的Sink，连同当前阶段的Sink包装到一起再返回
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> sink) {
                // downstream保存了流水线下一个阶段的Sink
                return new Sink.ChainedReference<>(sink) {
                    // 从前向后激活流水线上所有阶段的Sink
                    @Override
                    public void begin(long size) {
                        // 不清楚会过滤出多少个元素，所以这里重写begin方法，传入-1
                        downstream.begin(-1);
                    }
                    
                    // 将数据用当前阶段的Sink择取，然后再传给下一个阶段的Sink（从前向后传输）
                    @Override
                    public void accept(P_OUT u) {
                        // 如果数据满足filter条件，则继续传下去
                        if(predicate.test(u)) {
                            downstream.accept(u);
                        }
                    }
                };
            }
        };
    }
    
    // 映射数据。参数举例：x->x*x，求平方后返回
    @Override
    @SuppressWarnings("unchecked")
    public final <R> Stream<R> map(Function<? super P_OUT, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        
        // 返回一个无状态的流
        return new StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            // 从后向前包装SInk。传入下一个阶段的Sink，连同当前阶段的Sink包装到一起再返回
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<R> sink) {
                // downstream保存了流水线下一个阶段的Sink
                return new Sink.ChainedReference<>(sink) {
                    /* 可以使用默认的begin方法，因为这里要将所有数据传给下一个阶段 */
                    
                    // 将数据用当前阶段的Sink择取，然后再传给下一个阶段的Sink（从前向后传输）
                    @Override
                    public void accept(P_OUT u) {
                        downstream.accept(mapper.apply(u));
                    }
                };
            }
        };
    }
    
    // 映射数据，返回一个IntStream。参数举例：s->s.length()（要求返回值为int），计算字符串长度
    @Override
    public final IntStream mapToInt(ToIntFunction<? super P_OUT> mapper) {
        Objects.requireNonNull(mapper);
        
        // 返回一个无状态的流
        return new IntPipeline.StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            // 从后向前包装SInk。传入下一个阶段的Sink，连同当前阶段的Sink包装到一起再返回
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedReference<>(sink) {
                    /* 可以使用默认的begin方法，因为这里要将所有数据传给下一个阶段 */
                    
                    // 将数据用当前阶段的Sink择取，然后再传给下一个阶段的Sink（从前向后传输）
                    @Override
                    public void accept(P_OUT u) {
                        downstream.accept(mapper.applyAsInt(u));
                    }
                };
            }
        };
    }
    
    // 映射数据，返回一个LongStream。参考#mapToInt
    @Override
    public final LongStream mapToLong(ToLongFunction<? super P_OUT> mapper) {
        Objects.requireNonNull(mapper);
        
        return new LongPipeline.StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedReference<>(sink) {
                    @Override
                    public void accept(P_OUT u) {
                        downstream.accept(mapper.applyAsLong(u));
                    }
                };
            }
        };
    }
    
    // 映射数据，返回一个DoubleStream。参考#mapToInt
    @Override
    public final DoubleStream mapToDouble(ToDoubleFunction<? super P_OUT> mapper) {
        Objects.requireNonNull(mapper);
        
        return new DoublePipeline.StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedReference<>(sink) {
                    @Override
                    public void accept(P_OUT u) {
                        downstream.accept(mapper.applyAsDouble(u));
                    }
                };
            }
        };
    }
    
    /*
     * 数据降维。参数举例：l->l.stream()。
     * 比如int[][] a = new int[]{new int[]{1,2,3}, new int[]{4,5}. new int[]{6}};，降维成一维数组：[1,2,3,4,5,6]
     * 注：一次只能降低一个维度，比如遇到三维数组，那么如果想要得到一维的数据，需要连续调用两次。
     */
    @Override
    public final <R> Stream<R> flatMap(Function<? super P_OUT, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper);
        
        return new StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<R> sink) {
                return new Sink.ChainedReference<>(sink) {
                    // true if cancellationRequested() has been called
                    boolean cancellationRequestedCalled;
                    
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                    
                    @Override
                    public void accept(P_OUT u) {
                        // 将嵌套的维度中的每个元素转成一个流
                        try(Stream<? extends R> result = mapper.apply(u)) {
                            if(result != null) {
                                // 下游Sink不包含短路操作，直接对所有数据执行下游的操作
                                if(!cancellationRequestedCalled) {
                                    // 得到嵌套维度中的某个元素后，再对该元素内部的元素进行遍历，并应用下个阶段的sink
                                    result.sequential().forEach(downstream);
                                } else {
                                    // 如果下游存在短路操作，需要每处理一个元素就查看一下是否存在短路条件
                                    var s = result.sequential().spliterator();
                                    do {
                                    } while(
                                        !downstream.cancellationRequested()     // 在整个链条上判断是否应当停止接收数据（返回true表示停止接收）
                                            && s.tryAdvance(downstream)             // 对单个元素进行择取，如果已经没有元素，则返回flase
                                    );
                                }
                            }
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        /*
                         * If this method is called then an operation within the stream pipeline is short-circuiting (see AbstractPipeline.copyInto).
                         * Note that we cannot differentiate between an upstream or downstream operation
                         */
                        // 调用此方法时说明存在短路操作
                        cancellationRequestedCalled = true;
                        return downstream.cancellationRequested();
                    }
                };
            }
        };
    }
    
    // 数据降维。参考#flatMap。
    @Override
    public final IntStream flatMapToInt(Function<? super P_OUT, ? extends IntStream> mapper) {
        Objects.requireNonNull(mapper);
        
        return new IntPipeline.StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedReference<>(sink) {
                    // true if cancellationRequested() has been called
                    boolean cancellationRequestedCalled;
                    
                    // cache the consumer to avoid creation on every accepted element
                    IntConsumer downstreamAsInt = downstream::accept;
                    
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                    
                    @Override
                    public void accept(P_OUT u) {
                        try(IntStream result = mapper.apply(u)) {
                            if(result != null) {
                                if(!cancellationRequestedCalled) {
                                    result.sequential().forEach(downstreamAsInt);
                                } else {
                                    var s = result.sequential().spliterator();
                                    do {
                                    } while(!downstream.cancellationRequested() && s.tryAdvance(downstreamAsInt));
                                }
                            }
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        cancellationRequestedCalled = true;
                        return downstream.cancellationRequested();
                    }
                };
            }
        };
    }
    
    // 数据降维。参考#flatMap。
    @Override
    public final LongStream flatMapToLong(Function<? super P_OUT, ? extends LongStream> mapper) {
        Objects.requireNonNull(mapper);
        
        // We can do better than this, by polling cancellationRequested when stream is infinite
        return new LongPipeline.StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedReference<>(sink) {
                    // true if cancellationRequested() has been called
                    boolean cancellationRequestedCalled;
                    
                    // cache the consumer to avoid creation on every accepted element
                    LongConsumer downstreamAsLong = downstream::accept;
                    
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                    
                    @Override
                    public void accept(P_OUT u) {
                        try(LongStream result = mapper.apply(u)) {
                            if(result != null) {
                                if(!cancellationRequestedCalled) {
                                    result.sequential().forEach(downstreamAsLong);
                                } else {
                                    var s = result.sequential().spliterator();
                                    do {
                                    } while(!downstream.cancellationRequested() && s.tryAdvance(downstreamAsLong));
                                }
                            }
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        cancellationRequestedCalled = true;
                        return downstream.cancellationRequested();
                    }
                };
            }
        };
    }
    
    // 数据降维。参考#flatMap。
    @Override
    public final DoubleStream flatMapToDouble(Function<? super P_OUT, ? extends DoubleStream> mapper) {
        Objects.requireNonNull(mapper);
        
        return new DoublePipeline.StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedReference<>(sink) {
                    // true if cancellationRequested() has been called
                    boolean cancellationRequestedCalled;
                    
                    // cache the consumer to avoid creation on every accepted element
                    DoubleConsumer downstreamAsDouble = downstream::accept;
                    
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                    
                    @Override
                    public void accept(P_OUT u) {
                        try(DoubleStream result = mapper.apply(u)) {
                            if(result != null) {
                                if(!cancellationRequestedCalled) {
                                    result.sequential().forEach(downstreamAsDouble);
                                } else {
                                    var s = result.sequential().spliterator();
                                    do {
                                    } while(!downstream.cancellationRequested() && s.tryAdvance(downstreamAsDouble));
                                }
                            }
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        cancellationRequestedCalled = true;
                        return downstream.cancellationRequested();
                    }
                };
            }
        };
    }
    
    /*
     * 用于查看流的内部结构，不会对流的结构产生影响
     *
     * peek的实现跟map类似，但区别在于map会把当前处理结果传给下一个Sink，但peek不会传递处理结果。
     * peek常用来查看当前流的结构，比如输出其中的元素。
     */
    @Override
    public final Stream<P_OUT> peek(Consumer<? super P_OUT> action) {
        Objects.requireNonNull(action);
        
        return new StatelessOp<>(this, StreamShape.REFERENCE, 0) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> sink) {
                return new Sink.ChainedReference<>(sink) {
                    @Override
                    public void accept(P_OUT u) {
                        // 不会把处理结果传递给下一个阶段
                        action.accept(u);
                        downstream.accept(u);
                    }
                };
            }
        };
    }
    
    /*
     * 中间操作，返回等效的无序流。
     * 可能会返回自身，因为该流已经无序，或流的状态被修改为无序。
     */
    @Override
    public Stream<P_OUT> unordered() {
        if(!isOrdered())
            return this;
        return new StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_ORDERED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> sink) {
                return sink;
            }
        };
    }
    
    /*▲ 中间操作-无状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中间操作-有状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 去重
    @Override
    public final Stream<P_OUT> distinct() {
        return DistinctOps.makeRef(this);
    }
    
    // 排序（默认升序）
    @Override
    public final Stream<P_OUT> sorted() {
        return SortedOps.makeRef(this);
    }
    
    // 排序（使用Comparator改变排序规则）
    @Override
    public final Stream<P_OUT> sorted(Comparator<? super P_OUT> comparator) {
        return SortedOps.makeRef(this, comparator);
    }
    
    // 只显示前maxSize个元素
    @Override
    public final Stream<P_OUT> limit(long maxSize) {
        if(maxSize < 0)
            throw new IllegalArgumentException(Long.toString(maxSize));
        return SliceOps.makeRef(this, 0, maxSize);
    }
    
    // 跳过前n个元素
    @Override
    public final Stream<P_OUT> skip(long n) {
        if(n < 0)
            throw new IllegalArgumentException(Long.toString(n));
        if(n == 0)
            return this;
        else
            return SliceOps.makeRef(this, n, -1);
    }
    
    /*▲ 中间操作-有状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作-短路操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 存在元素满足predicate条件
    @Override
    public final boolean anyMatch(Predicate<? super P_OUT> predicate) {
        return evaluate(MatchOps.makeRef(predicate, MatchOps.MatchKind.ANY));
    }
    
    // 所有元素满足predicate条件
    @Override
    public final boolean allMatch(Predicate<? super P_OUT> predicate) {
        return evaluate(MatchOps.makeRef(predicate, MatchOps.MatchKind.ALL));
    }
    
    // 没有元素满足predicate条件
    @Override
    public final boolean noneMatch(Predicate<? super P_OUT> predicate) {
        return evaluate(MatchOps.makeRef(predicate, MatchOps.MatchKind.NONE));
    }
    
    // 找出第一个元素，返回一个可选的操作
    @Override
    public final Optional<P_OUT> findFirst() {
        return evaluate(FindOps.makeRef(true));
    }
    
    // 找到一个元素就返回，往往是第一个元素
    @Override
    public final Optional<P_OUT> findAny() {
        return evaluate(FindOps.makeRef(false));
    }
    
    /*▲ 终端操作-短路操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作-非短路操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将数据存入Object数组返回
    @Override
    public final Object[] toArray() {
        return toArray(value -> new Object[value]);
    }
    
    // 将数据存入指定类型的数组后返回
    @Override
    @SuppressWarnings("unchecked")
    public final <A> A[] toArray(IntFunction<A[]> generator) {
        // Since A has no relation to U (not possible to declare that A is an upper bound of U) there will be no static type checking.
        // Therefore use a raw type and assume A == U rather than propagating the separation of A and U throughout the code-base.
        // The runtime type of U is never checked for equality with the component type of the runtime type of A[].
        // Runtime checking will be performed when an element is stored in A[], thus if A is not a super type of U an ArrayStoreException will be thrown.
        @SuppressWarnings("rawtypes")
        IntFunction rawGenerator = generator;   // 从泛型到原生类型的转换
        
        Node node = evaluateToArrayNode(rawGenerator);
        
        Node flatten = Nodes.flatten(node, rawGenerator);
        
        return (A[]) flatten.asArray(rawGenerator);
    }
    
    // 遍历，并执行action操作
    @Override
    public void forEach(Consumer<? super P_OUT> action) {
        evaluate(ForEachOps.makeRef(action, false));
    }
    
    // 按遭遇顺序遍历，并执行action操作
    @Override
    public void forEachOrdered(Consumer<? super P_OUT> action) {
        evaluate(ForEachOps.makeRef(action, true));
    }
    
    // 收纳汇总，两两比对，完成指定动作
    @Override
    public final Optional<P_OUT> reduce(BinaryOperator<P_OUT> accumulator) {
        return evaluate(ReduceOps.makeRef(accumulator));
    }
    
    // 收纳汇总，两两比对，完成accumulator动作。identity是初值，accumulator中的输入类型应当一致。
    @Override
    public final P_OUT reduce(final P_OUT identity, final BinaryOperator<P_OUT> accumulator) {
        return evaluate(ReduceOps.makeRef(identity, accumulator, accumulator));
    }
    
    // 收纳汇总，两两比对，完成combiner动作。identity是初值，accumulator中的输入类型可以不一致。combiner用在并行操作。
    @Override
    public final <R> R reduce(R identity, BiFunction<R, ? super P_OUT, R> accumulator, BinaryOperator<R> combiner) {
        return evaluate(ReduceOps.makeRef(identity, accumulator, combiner));
    }
    
    /*
     * 收集输出的元素到某个容器
     * supplier用于构造容器
     * accumulator用于向容器添加元素
     * combiner用于拼接容器（用在并行处理中）
     */
    @Override
    public final <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super P_OUT> accumulator, BiConsumer<R, R> combiner) {
        return evaluate(ReduceOps.makeRef(supplier, accumulator, combiner));
    }
    
    // 收纳汇总，具体行为取决于Collector
    @Override
    @SuppressWarnings("unchecked")
    public final <R, A> R collect(Collector<? super P_OUT, A, R> collector) {
        A container;
        if(isParallel()
            && (collector.characteristics().contains(Collector.Characteristics.CONCURRENT))
            && (!isOrdered() || collector.characteristics().contains(Collector.Characteristics.UNORDERED))) {
            container = collector.supplier().get();
            BiConsumer<A, ? super P_OUT> accumulator = collector.accumulator();
            forEach(u -> accumulator.accept(container, u));
        } else {
            container = evaluate(ReduceOps.makeRef(collector));
        }
        return collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)
            ? (R) container
            : collector.finisher().apply(container);
    }
    
    // 计数
    @Override
    public final long count() {
        return evaluate(ReduceOps.makeRefCounting());
    }
    
    // 求最小值，元素的“大小”判断取决于comparator
    @Override
    public final Optional<P_OUT> min(Comparator<? super P_OUT> comparator) {
        return reduce(BinaryOperator.minBy(comparator));
    }
    
    // 求最大值，元素的“大小”判断取决于comparator
    @Override
    public final Optional<P_OUT> max(Comparator<? super P_OUT> comparator) {
        return reduce(BinaryOperator.maxBy(comparator));
    }
    
    /*▲ 终端操作-非短路操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    // 返回流的形状
    @Override
    final StreamShape getOutputShape() {
        return StreamShape.REFERENCE;
    }
    
    // 逐个择取元素，每次择取之前都要先判断是否应当停止接收数据
    @Override
    final boolean forEachWithCancel(Spliterator<P_OUT> spliterator, Sink<P_OUT> sink) {
        boolean cancelled;
        do {
        } while(
            !(cancelled = sink.cancellationRequested()) // 在整个链条上判断是否应当停止接收数据（返回true表示停止接收）
                && spliterator.tryAdvance(sink) // 对单个元素进行择取，如果已经没有元素，则返回flase
        );
        return cancelled;
    }
    
    // 返回第(3)、(4)类Node（固定长度Node和可变长度Node）
    @Override
    final Node.Builder<P_OUT> makeNodeBuilder(long exactSizeIfKnown, IntFunction<P_OUT[]> generator) {
        return Nodes.builder(exactSizeIfKnown, generator);
    }
    
    // 并行收集元素。如果流水线上存在有状态的中间操作，则优化计算过程
    @Override
    final <P_IN> Node<P_OUT> evaluateToNode(PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<P_OUT[]> generator) {
        return Nodes.collect(helper, spliterator, flattenTree, generator);
    }
    
    @Override
    final <P_IN> Spliterator<P_OUT> wrap(PipelineHelper<P_OUT> ph, Supplier<Spliterator<P_IN>> supplier, boolean isParallel) {
        return new StreamSpliterators.WrappingSpliterator<>(ph, supplier, isParallel);
    }
    
    @Override
    final Spliterator<P_OUT> lazySpliterator(Supplier<? extends Spliterator<P_OUT>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator<>(supplier);
    }
    
    // 返回(4)类Spliterator：将Spliterator适配到Iterator来使用
    @Override
    public final Iterator<P_OUT> iterator() {
        return Spliterators.iterator(spliterator());
    }
    
    
    
    
    /**
     * Source stage of a ReferencePipeline.
     *
     * @param <E_IN>  type of elements in the upstream source
     * @param <E_OUT> type of elements in produced by this stage
     *
     * @since 1.8
     */
    // 流的源头阶段，包含了数据源和可以对数据源执行的遍历、分割操作
    static class Head<E_IN, E_OUT> extends ReferencePipeline<E_IN, E_OUT> {
        /**
         * Constructor for the source stage of a Stream.
         *
         * @param source      {@code Supplier<Spliterator>} describing the stream
         *                    source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         */
        // 构造Stream的HEAD阶段，需要从source中提取Spliterator
        Head(Supplier<? extends Spliterator<?>> source, int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }
        
        /**
         * Constructor for the source stage of a Stream.
         *
         * @param source      {@code Spliterator} describing the stream source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         */
        // 构造Stream的HEAD阶段
        Head(Spliterator<?> source, int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }
        
        @Override
        final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        final Sink<E_IN> opWrapSink(int flags, Sink<E_OUT> sink) {
            throw new UnsupportedOperationException();
        }
        
        // Optimized sequential terminal operations for the head of the pipeline
        
        @Override
        public void forEach(Consumer<? super E_OUT> action) {
            if(!isParallel()) {
                sourceStageSpliterator().forEachRemaining(action);
            } else {
                super.forEach(action);
            }
        }
        
        @Override
        public void forEachOrdered(Consumer<? super E_OUT> action) {
            if(!isParallel()) {
                sourceStageSpliterator().forEachRemaining(action);
            } else {
                super.forEachOrdered(action);
            }
        }
    }
    
    /**
     * Base class for a stateless intermediate stage of a Stream.
     *
     * @param <E_IN>  type of elements in the upstream source
     * @param <E_OUT> type of elements in produced by this stage
     *
     * @since 1.8
     */
    // 流的中间阶段-无状态流
    abstract static class StatelessOp<E_IN, E_OUT> extends ReferencePipeline<E_IN, E_OUT> {
        /**
         * Construct a new Stream by appending a stateless intermediate
         * operation to an existing stream.
         *
         * @param upstream   The upstream pipeline stage
         * @param inputShape The stream shape for the upstream pipeline stage
         * @param opFlags    Operation flags for the new stage
         */
        // 构造无状态的流的中间阶段，upstream是调用此构造方法的流
        StatelessOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
        }
        
        @Override
        final boolean opIsStateful() {
            return false;
        }
    }
    
    /**
     * Base class for a stateful intermediate stage of a Stream.
     *
     * @param <E_IN>  type of elements in the upstream source
     * @param <E_OUT> type of elements in produced by this stage
     *
     * @since 1.8
     */
    // 流的中间阶段-有状态流
    abstract static class StatefulOp<E_IN, E_OUT> extends ReferencePipeline<E_IN, E_OUT> {
        /**
         * Construct a new Stream by appending a stateful intermediate operation
         * to an existing stream.
         *
         * @param upstream   The upstream pipeline stage
         * @param inputShape The stream shape for the upstream pipeline stage
         * @param opFlags    Operation flags for the new stage
         */
        // 构造有状态的流的中间阶段，upstream是调用此构造方法的流
        StatefulOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
        }
        
        @Override
        abstract <P_IN> Node<E_OUT> opEvaluateParallel(PipelineHelper<E_OUT> helper, Spliterator<P_IN> spliterator, IntFunction<E_OUT[]> generator);
        
        @Override
        final boolean opIsStateful() {
            return true;
        }
    }
}
