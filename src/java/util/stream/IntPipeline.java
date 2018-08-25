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

import java.util.IntSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code int}.
 *
 * @param <E_IN> type of elements in the upstream source
 * @since 1.8
 */

// int类型的流水线
abstract class IntPipeline<E_IN>
    extends AbstractPipeline<E_IN, Integer, IntStream>
    implements IntStream {
    
    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source      {@code Supplier<Spliterator>} describing the stream source
     * @param sourceFlags The source flags for the stream source, described in
     *                    {@link StreamOpFlag}
     * @param parallel    {@code true} if the pipeline is parallel
     */
    // 构造Stream，常用于构建流的源头阶段（HEAD），需要从source中提取Spliterator
    IntPipeline(Supplier<? extends Spliterator<Integer>> source, int sourceFlags, boolean parallel) {
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
    IntPipeline(Spliterator<Integer> source, int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }
    
    /**
     * Constructor for appending an intermediate operation onto an existing
     * pipeline.
     *
     * @param upstream the upstream element source
     * @param opFlags  the operation flags for the new operation
     */
    // 构造Stream，常用于构建流的中间阶段，包括有状态流和无状态流。upstream是调用此构造方法的流
    IntPipeline(AbstractPipeline<?, E_IN, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }
    
    
    
    /*▼ 创建流的源头阶段 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    @Override
    public final IntStream takeWhile(IntPredicate predicate) {
        return WhileOps.makeTakeWhileInt(this, predicate);
    }
    
    @Override
    public final IntStream dropWhile(IntPredicate predicate) {
        return WhileOps.makeDropWhileInt(this, predicate);
    }
    
    /*▲ 创建流的源头阶段 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中间操作-无状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 筛选数据
    @Override
    public final IntStream filter(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedInt<Integer>(sink) {
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                    
                    @Override
                    public void accept(int t) {
                        if(predicate.test(t))
                            downstream.accept(t);
                    }
                };
            }
        };
    }
    
    // 映射数据
    @Override
    public final IntStream map(IntUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedInt<Integer>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept(mapper.applyAsInt(t));
                    }
                };
            }
        };
    }
    
    // 映射数据
    @Override
    public final LongStream mapToLong(IntToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedInt<Long>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept(mapper.applyAsLong(t));
                    }
                };
            }
        };
    }
    
    // 映射数据
    @Override
    public final DoubleStream mapToDouble(IntToDoubleFunction mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedInt<Double>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept(mapper.applyAsDouble(t));
                    }
                };
            }
        };
    }
    
    // 映射数据
    @Override
    public final <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return mapToObj(mapper, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT);
    }
    
    // 映射数据
    private <U> Stream<U> mapToObj(IntFunction<? extends U> mapper, int opFlags) {
        return new ReferencePipeline.StatelessOp<Integer, U>(this, StreamShape.INT_VALUE, opFlags) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<U> sink) {
                return new Sink.ChainedInt<U>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept(mapper.apply(t));
                    }
                };
            }
        };
    }
    
    // 数据降维
    @Override
    public final IntStream flatMap(IntFunction<? extends IntStream> mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedInt<Integer>(sink) {
                    // true if cancellationRequested() has been called
                    boolean cancellationRequestedCalled;
                    
                    // cache the consumer to avoid creation on every accepted element
                    IntConsumer downstreamAsInt = downstream::accept;
                    
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                    
                    @Override
                    public void accept(int t) {
                        try(IntStream result = mapper.apply(t)) {
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
                        // If this method is called then an operation within the stream
                        // pipeline is short-circuiting (see AbstractPipeline.copyInto).
                        // Note that we cannot differentiate between an upstream or
                        // downstream operation
                        cancellationRequestedCalled = true;
                        return downstream.cancellationRequested();
                    }
                };
            }
        };
    }
    
    // 用于查看流的内部结构，不会对流的结构产生影响
    @Override
    public final IntStream peek(IntConsumer action) {
        Objects.requireNonNull(action);
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, 0) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedInt<Integer>(sink) {
                    @Override
                    public void accept(int t) {
                        action.accept(t);
                        downstream.accept(t);
                    }
                };
            }
        };
    }
    
    // 装箱
    @Override
    public final Stream<Integer> boxed() {
        return mapToObj(Integer::valueOf, 0);
    }
    
    // 转换为LongStream
    @Override
    public final LongStream asLongStream() {
        return new LongPipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, 0) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedInt<Long>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept((long) t);
                    }
                };
            }
        };
    }
    
    // 转换为DoubleStream
    @Override
    public final DoubleStream asDoubleStream() {
        return new DoublePipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, 0) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedInt<Double>(sink) {
                    @Override
                    public void accept(int t) {
                        downstream.accept((double) t);
                    }
                };
            }
        };
    }
    
    // 中间操作，返回等效的无序流。
    @Override
    public IntStream unordered() {
        if(!isOrdered())
            return this;
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_ORDERED) {
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return sink;
            }
        };
    }
    
    /*▲ 中间操作-无状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中间操作-有状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 去重
    @Override
    public final IntStream distinct() {
        // While functional and quick to implement, this approach is not very efficient.
        // An efficient version requires an int-specific map/set implementation.
        return boxed().distinct().mapToInt(i -> i);
    }
    
    // 排序（默认升序）
    @Override
    public final IntStream sorted() {
        return SortedOps.makeInt(this);
    }
    
    // 只显示前maxSize个元素
    @Override
    public final IntStream limit(long maxSize) {
        if(maxSize < 0)
            throw new IllegalArgumentException(Long.toString(maxSize));
        return SliceOps.makeInt(this, 0, maxSize);
    }
    
    // 跳过前n个元素
    @Override
    public final IntStream skip(long n) {
        if(n < 0)
            throw new IllegalArgumentException(Long.toString(n));
        if(n == 0)
            return this;
        else
            return SliceOps.makeInt(this, n, -1);
    }
    
    /*▲ 中间操作-有状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作-短路操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 存在元素满足predicate条件
    @Override
    public final boolean anyMatch(IntPredicate predicate) {
        return evaluate(MatchOps.makeInt(predicate, MatchOps.MatchKind.ANY));
    }
    
    // 所有元素满足predicate条件
    @Override
    public final boolean allMatch(IntPredicate predicate) {
        return evaluate(MatchOps.makeInt(predicate, MatchOps.MatchKind.ALL));
    }
    
    // 没有元素满足predicate条件
    @Override
    public final boolean noneMatch(IntPredicate predicate) {
        return evaluate(MatchOps.makeInt(predicate, MatchOps.MatchKind.NONE));
    }
    
    // 找出第一个元素，返回一个可选的操作
    @Override
    public final OptionalInt findFirst() {
        return evaluate(FindOps.makeInt(true));
    }
    
    // 找到一个元素就返回，往往是第一个元素
    @Override
    public final OptionalInt findAny() {
        return evaluate(FindOps.makeInt(false));
    }
    
    /*▲ 终端操作-短路操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作-非短路操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将数据存入int数组返回
    @Override
    public final int[] toArray() {
        return Nodes.flattenInt((Node.OfInt) evaluateToArrayNode(Integer[]::new)).asPrimitiveArray();
    }
    
    // 遍历，并执行action操作
    @Override
    public void forEach(IntConsumer action) {
        evaluate(ForEachOps.makeInt(action, false));
    }
    
    // 按遭遇顺序遍历，并执行action操作
    @Override
    public void forEachOrdered(IntConsumer action) {
        evaluate(ForEachOps.makeInt(action, true));
    }
    
    // 收纳汇总，两两比对，完成指定动作
    @Override
    public final OptionalInt reduce(IntBinaryOperator op) {
        return evaluate(ReduceOps.makeInt(op));
    }
    
    // 收纳汇总，两两比对，完成op动作。identity是初值，op中的输入类型应当一致
    @Override
    public final int reduce(int identity, IntBinaryOperator op) {
        return evaluate(ReduceOps.makeInt(identity, op));
    }
    
    // 收集输出的元素到某个容器
    @Override
    public final <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(combiner);
        BinaryOperator<R> operator = (left, right) -> {
            combiner.accept(left, right);
            return left;
        };
        return evaluate(ReduceOps.makeInt(supplier, accumulator, operator));
    }
    
    // 计数
    @Override
    public final long count() {
        return evaluate(ReduceOps.makeIntCounting());
    }
    
    // 求最小值
    @Override
    public final OptionalInt min() {
        return reduce(Math::min);
    }
    
    // 求最大值
    @Override
    public final OptionalInt max() {
        return reduce(Math::max);
    }
    
    // 求和
    @Override
    public final int sum() {
        return reduce(0, Integer::sum);
    }
    
    // 求平均值
    @Override
    public final OptionalDouble average() {
        long[] avg = collect(() -> new long[2], (ll, i) -> {
            ll[0]++;
            ll[1] += i;
        }, (ll, rr) -> {
            ll[0] += rr[0];
            ll[1] += rr[1];
        });
        return avg[0] > 0 ? OptionalDouble.of((double) avg[1] / avg[0]) : OptionalDouble.empty();
    }
    
    // 信息统计
    @Override
    public final IntSummaryStatistics summaryStatistics() {
        return collect(IntSummaryStatistics::new, IntSummaryStatistics::accept, IntSummaryStatistics::combine);
    }
    
    /*▲ 终端操作-非短路操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 实现IntStream接口 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    @Override
    public final PrimitiveIterator.OfInt iterator() {
        return Spliterators.iterator(spliterator());
    }
    
    @Override
    public final Spliterator.OfInt spliterator() {
        return adapt(super.spliterator());
    }
    
    /*▲ 实现IntStream接口 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 返回流的形状
    @Override
    final StreamShape getOutputShape() {
        return StreamShape.INT_VALUE;
    }
    
    // 逐个择取元素，每次择取之前都要先判断是否应当停止接收数据
    @Override
    final boolean forEachWithCancel(Spliterator<Integer> spliterator, Sink<Integer> sink) {
        Spliterator.OfInt spl = adapt(spliterator);
        IntConsumer adaptedSink = adapt(sink);
        boolean cancelled;
        do {
        } while(!(cancelled = sink.cancellationRequested()) && spl.tryAdvance(adaptedSink));
        return cancelled;
    }
    
    // 返回第(3)、(4)类Node（固定长度Node和可变长度Node）
    @Override
    final Node.Builder<Integer> makeNodeBuilder(long exactSizeIfKnown, IntFunction<Integer[]> generator) {
        return Nodes.intBuilder(exactSizeIfKnown);
    }
    
    // 并行收集元素。如果流水线上存在有状态的中间操作，则优化计算过程
    @Override
    final <P_IN> Node<Integer> evaluateToNode(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<Integer[]> generator) {
        return Nodes.collectInt(helper, spliterator, flattenTree);
    }
    
    @Override
    final <P_IN> Spliterator<Integer> wrap(PipelineHelper<Integer> ph, Supplier<Spliterator<P_IN>> supplier, boolean isParallel) {
        return new StreamSpliterators.IntWrappingSpliterator<>(ph, supplier, isParallel);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    final Spliterator.OfInt lazySpliterator(Supplier<? extends Spliterator<Integer>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator.OfInt((Supplier<Spliterator.OfInt>) supplier);
    }
    
    /**
     * Adapt a {@code Sink<Integer> to an {@code IntConsumer}, ideally simply
     * by casting.
     */
    private static IntConsumer adapt(Sink<Integer> sink) {
        if(sink instanceof IntConsumer) {
            return (IntConsumer) sink;
        } else {
            if(Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class, "using IntStream.adapt(Sink<Integer> s)");
            return value -> sink.accept(value);
        }
    }
    
    /**
     * Adapt a {@code Spliterator<Integer>} to a {@code Spliterator.OfInt}.
     *
     * @implNote The implementation attempts to cast to a Spliterator.OfInt, and throws an
     * exception if this cast is not possible.
     */
    private static Spliterator.OfInt adapt(Spliterator<Integer> s) {
        if(s instanceof Spliterator.OfInt) {
            return (Spliterator.OfInt) s;
        } else {
            if(Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class, "using IntStream.adapt(Spliterator<Integer> s)");
            throw new UnsupportedOperationException("IntStream.adapt(Spliterator<Integer> s)");
        }
    }
    
    
    
    
    /**
     * Source stage of an IntStream.
     *
     * @param <E_IN> type of elements in the upstream source
     *
     * @since 1.8
     */
    // 流的源头阶段，包含了数据源和可以对数据源执行的遍历、分割操作
    static class Head<E_IN> extends IntPipeline<E_IN> {
        /**
         * Constructor for the source stage of an IntStream.
         *
         * @param source      {@code Supplier<Spliterator>} describing the stream
         *                    source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel    {@code true} if the pipeline is parallel
         */
        Head(Supplier<? extends Spliterator<Integer>> source, int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }
        
        /**
         * Constructor for the source stage of an IntStream.
         *
         * @param source      {@code Spliterator} describing the stream source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel    {@code true} if the pipeline is parallel
         */
        Head(Spliterator<Integer> source, int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }
        
        @Override
        final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        final Sink<E_IN> opWrapSink(int flags, Sink<Integer> sink) {
            throw new UnsupportedOperationException();
        }
        
        // Optimized sequential terminal operations for the head of the pipeline
        
        @Override
        public void forEach(IntConsumer action) {
            if(!isParallel()) {
                adapt(sourceStageSpliterator()).forEachRemaining(action);
            } else {
                super.forEach(action);
            }
        }
        
        @Override
        public void forEachOrdered(IntConsumer action) {
            if(!isParallel()) {
                adapt(sourceStageSpliterator()).forEachRemaining(action);
            } else {
                super.forEachOrdered(action);
            }
        }
    }
    
    /**
     * Base class for a stateless intermediate stage of an IntStream
     *
     * @param <E_IN> type of elements in the upstream source
     *
     * @since 1.8
     */
    // 流的中间阶段-无状态流
    abstract static class StatelessOp<E_IN> extends IntPipeline<E_IN> {
        /**
         * Construct a new IntStream by appending a stateless intermediate
         * operation to an existing stream.
         *
         * @param upstream   The upstream pipeline stage
         * @param inputShape The stream shape for the upstream pipeline stage
         * @param opFlags    Operation flags for the new stage
         */
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
     * Base class for a stateful intermediate stage of an IntStream.
     *
     * @param <E_IN> type of elements in the upstream source
     *
     * @since 1.8
     */
    // 流的中间阶段-有状态流
    abstract static class StatefulOp<E_IN> extends IntPipeline<E_IN> {
        /**
         * Construct a new IntStream by appending a stateful intermediate
         * operation to an existing stream.
         *
         * @param upstream   The upstream pipeline stage
         * @param inputShape The stream shape for the upstream pipeline stage
         * @param opFlags    Operation flags for the new stage
         */
        StatefulOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
        }
        
        @Override
        abstract <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator, IntFunction<Integer[]> generator);
        
        @Override
        final boolean opIsStateful() {
            return true;
        }
    }
}
