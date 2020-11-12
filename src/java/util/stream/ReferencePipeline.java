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
import java.util.stream.MatchOps.MatchKind;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code U}.
 *
 * @param <P_IN>  type of elements in the upstream source
 * @param <P_OUT> type of elements in produced by this stage
 *
 * @since 1.8
 */
// 流的引用类型版本
abstract class ReferencePipeline<P_IN, P_OUT> extends AbstractPipeline<P_IN, P_OUT, Stream<P_OUT>> implements Stream<P_OUT> {
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source      {@code Supplier<Spliterator>} describing the stream source
     * @param sourceFlags the source flags for the stream source, described in
     *                    {@link StreamOpFlag}
     * @param parallel    {@code true} if the pipeline is parallel
     */
    /*
     * 构造流的源头(head)阶段
     *
     * source     : 数据源；source不是原始数据，而是原始数据的流迭代器，可以通过流迭代器来访问原始数据
     * sourceFlags: 源头阶段的流参数
     * parallel   : 是否为并行流
     */
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
    /*
     * 构造流的源头(head)阶段
     *
     * source     : 数据源；source不是原始数据，而是原始数据的流迭代器，可以通过流迭代器来访问原始数据
     * sourceFlags: 源头阶段的流参数
     * parallel   : 是否为并行流
     */
    ReferencePipeline(Spliterator<?> source, int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }
    
    /**
     * Constructor for appending an intermediate operation onto an existing
     * pipeline.
     *
     * @param upstream the upstream element source.
     */
    /*
     * 构造流的中间(intermediate)阶段，该阶段持有流的前一个阶段的引用
     *
     * upstream: 链接到流的前一个阶段的引用
     * opFlags : 中间阶段的流的操作参数
     */
    ReferencePipeline(AbstractPipeline<?, P_IN, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 构造一个"惰性"流迭代器
     *
     * "惰性"的含义是使用流迭代器时，需要从流迭代器工厂中获取
     */
    @Override
    final Spliterator<P_OUT> lazySpliterator(Supplier<? extends Spliterator<P_OUT>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator<>(supplier);
    }
    
    /*
     * 构造一个"包装"流迭代器，使用该流迭代器可以获取ph阶段的输出元素。
     *
     * ph        : 某个流阶段
     * supplier  : 相对于ph的上个(depth==0)的流阶段的流迭代器工厂
     * isParallel: ph所在流是否需要并行执行
     */
    @Override
    final <P_IN> Spliterator<P_OUT> wrap(PipelineHelper<P_OUT> ph, Supplier<Spliterator<P_IN>> supplier, boolean isParallel) {
        return new StreamSpliterators.WrappingSpliterator<>(ph, supplier, isParallel);
    }
    
    /*▲ 流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中间操作-无状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 筛选数据。参数举例：x->x%2==0，筛选出所有偶数
    @Override
    public final Stream<P_OUT> filter(Predicate<? super P_OUT> predicate) {
        Objects.requireNonNull(predicate);
    
        // 返回一个无状态的流
        return new StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SIZED) {
    
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> downSink) {
        
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<>(downSink) {
                    @Override
                    public void begin(long size) {
                        // 不清楚会过滤出多少个元素，所以这里重写begin方法，传入-1
                        downstream.begin(-1);
                    }
            
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
    
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<R> downSink) {
        
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<>(downSink) {
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
    
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Integer> downSink) {
        
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<>(downSink) {
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
    
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Long> downSink) {
        
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<>(downSink) {
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
    
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Double> downSink) {
        
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<>(downSink) {
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
     *
     * 比如int[][] a = new int[]{new int[]{1,2,3}, new int[]{4,5}. new int[]{6}};，降维成一维数组：[1,2,3,4,5,6]
     * 注：一次只能降低一个维度，比如遇到三维数组，那么如果想要得到一维的数据，需要连续调用两次。
     *
     * mapper的作用就是将上游的高维容器中的元素提取出来放到流中，并在后续将该元素依次传递到下游，
     * 这就相当于把原来的元素从高维容器中提取出来了。
     */
    @Override
    public final <R> Stream<R> flatMap(Function<? super P_OUT, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper);
        
        return new StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
    
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<R> downSink) {
        
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<>(downSink) {
                    // true if cancellationRequested() has been called
                    boolean cancellationRequestedCalled;
            
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
            
                    @Override
                    public void accept(P_OUT u) {
                        // 将高维度容器中的元素提取到一个流中
                        try(Stream<? extends R> result = mapper.apply(u)) {
                            // 如果没有元素，则直接返回
                            if(result == null) {
                                return;
                            }
                    
                            // 如果上游没有调用过cancellationRequested()方法，则直接将数据交给下游
                            if(!cancellationRequestedCalled) {
                                // 使用下游的sink对当前得到的元素进行遍历
                                result.sequential().forEach(downstream);
                            } else {
                                // 返回result流的流迭代器
                                Spliterator<? extends R> spliterator = result.sequential().spliterator();
                        
                                // 如果已经调用过cancellationRequested()方法，则之后每次传递一个元素之前都要询问下游是否已经停止接收数据
                                while(!downstream.cancellationRequested() && spliterator.tryAdvance(downstream)) {
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
                        // 调用此方法时说明上游发起一个询问，以查看下游是否已经停止了接收元素
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
    
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Integer> downSink) {
        
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<>(downSink) {
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
                                    Spliterator.OfInt spliterator = result.sequential().spliterator();
                                    while(!downstream.cancellationRequested() && spliterator.tryAdvance(downstreamAsInt)) {
                                    }
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
    
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Long> downSink) {
        
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<>(downSink) {
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
                                    Spliterator.OfLong spliterator = result.sequential().spliterator();
                                    while(!downstream.cancellationRequested() && spliterator.tryAdvance(downstreamAsLong)) {
                                    }
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
    
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Double> downSink) {
        
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<>(downSink) {
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
                                    Spliterator.OfDouble spliterator = result.sequential().spliterator();
                                    while(!downstream.cancellationRequested() && spliterator.tryAdvance(downstreamAsDouble)) {
                                    }
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
     * 用于查看流的内部结构，不会对流的结构产生影响。
     * peek常用来查看当前流的结构，比如输出其中的元素。
     */
    @Override
    public final Stream<P_OUT> peek(Consumer<? super P_OUT> action) {
        Objects.requireNonNull(action);
        
        return new StatelessOp<>(this, StreamShape.REFERENCE, 0) {
    
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> downSink) {
        
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<>(downSink) {
                    @Override
                    public void accept(P_OUT u) {
                        // 只在当前阶段消费，没有返回结果
                        action.accept(u);
                        // 向下游发送的依然是这个元素
                        downstream.accept(u);
                    }
                };
            }
        };
    }
    
    // 中间操作：将当前流设置为无序流后返回
    @Override
    public Stream<P_OUT> unordered() {
        // 如果已经是无序流，则直接返回
        if(!isOrdered()) {
            return this;
        }
    
        return new StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_ORDERED) {
        
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> downSink) {
                return downSink;
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
    
    // 排序（使用comparator制定排序规则）
    @Override
    public final Stream<P_OUT> sorted(Comparator<? super P_OUT> comparator) {
        return SortedOps.makeRef(this, comparator);
    }
    
    // 只显示前maxSize个元素
    @Override
    public final Stream<P_OUT> limit(long maxSize) {
        if(maxSize<0) {
            throw new IllegalArgumentException(Long.toString(maxSize));
        }
    
        return SliceOps.makeRef(this, 0, maxSize);
    }
    
    // 跳过前n个元素
    @Override
    public final Stream<P_OUT> skip(long n) {
        if(n<0) {
            throw new IllegalArgumentException(Long.toString(n));
        }
    
        if(n == 0) {
            return this;
        }
    
        return SliceOps.makeRef(this, n, -1);
    }
    
    // "保存前缀"：保存起初遇到的满足predicate条件的元素；只要遇到首个不满足条件的元素，就结束后续的保存动作
    @Override
    public final Stream<P_OUT> takeWhile(Predicate<? super P_OUT> predicate) {
        return WhileOps.makeTakeWhileRef(this, predicate);
    }
    
    // "丢弃前缀"：丢弃起初遇到的满足predicate条件的元素；只要遇到首个不满足条件的元素，就开始保存它后及其后面的元素
    @Override
    public final Stream<P_OUT> dropWhile(Predicate<? super P_OUT> predicate) {
        return WhileOps.makeDropWhileRef(this, predicate);
    }
    
    /*▲ 中间操作-有状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作-非短路操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将数据存入Object数组返回
    @Override
    public final Object[] toArray() {
        return toArray(size -> new Object[size]);
    }
    
    // 将数据存入指定类型的数组后返回
    @Override
    @SuppressWarnings("unchecked")
    public final <A> A[] toArray(IntFunction<A[]> generator) {
    
        /*
         * Since A has no relation to U (not possible to declare that A is an upper bound of U) there will be no static type checking.
         * Therefore use a raw type and assume A == U rather than propagating the separation of A and U throughout the code-base.
         * The runtime type of U is never checked for equality with the component type of the runtime type of A[].
         * Runtime checking will be performed when an element is stored in A[], thus if A is not a super type of U an ArrayStoreException will be thrown.
         */
        @SuppressWarnings("rawtypes")
        IntFunction rawGenerator = generator;   // 从泛型到原生类型的转换
    
        Node<A> node = evaluateToArrayNode(rawGenerator);
    
        /*
         * 尝试将树状Node转换为普通"数组"Node；如果node不是树状node，那直接就返回了。
         * 将树状Node中的元素并行地复制到generator生成的数组中，然后再将该数组封装成普通"数组"Node后返回(引用类型版本)。
         */
        Node<A> flatten = Nodes.flatten(node, rawGenerator);
    
        // 将Node中的元素复制到使用rawGenerator构造的数组中后返回
        return (A[]) flatten.asArray(rawGenerator);
    }
    
    // 遍历，并执行consumer操作
    @Override
    public void forEach(Consumer<? super P_OUT> consumer) {
        TerminalOp<P_OUT, Void> terminalOp = ForEachOps.makeRef(consumer, false);
        evaluate(terminalOp);
    }
    
    // 按遭遇顺序遍历，并执行consumer操作
    @Override
    public void forEachOrdered(Consumer<? super P_OUT> consumer) {
        TerminalOp<P_OUT, Void> terminalOp = ForEachOps.makeRef(consumer, true);
        evaluate(terminalOp);
    }
    
    // 求最小值，元素的排序规则取决于comparator
    @Override
    public final Optional<P_OUT> min(Comparator<? super P_OUT> comparator) {
        BinaryOperator<P_OUT> accumulator = BinaryOperator.minBy(comparator);
        return reduce(accumulator);
    }
    
    // 求最大值，元素的排序规则取决于comparator
    @Override
    public final Optional<P_OUT> max(Comparator<? super P_OUT> comparator) {
        BinaryOperator<P_OUT> accumulator = BinaryOperator.maxBy(comparator);
        return reduce(accumulator);
    }
    
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
    @Override
    public final Optional<P_OUT> reduce(BinaryOperator<P_OUT> accumulator) {
        TerminalOp<P_OUT, Optional<P_OUT>> terminalOp = ReduceOps.makeRef(accumulator);
        return evaluate(terminalOp);
    }
    
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
    @Override
    public final P_OUT reduce(final P_OUT identity, final BinaryOperator<P_OUT> accumulator) {
        TerminalOp<P_OUT, P_OUT> terminalOp = ReduceOps.makeRef(identity, accumulator, accumulator);
        return evaluate(terminalOp);
    }
    
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
    @Override
    public final <R> R reduce(R identity, BiFunction<R, ? super P_OUT, R> accumulator, BinaryOperator<R> combiner) {
        TerminalOp<P_OUT, R> terminalOp = ReduceOps.makeRef(identity, accumulator, combiner);
        return evaluate(terminalOp);
    }
    
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
    @Override
    public final <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super P_OUT> accumulator, BiConsumer<R, R> combiner) {
        TerminalOp<P_OUT, R> terminalOp = ReduceOps.makeRef(supplier, accumulator, combiner);
        return evaluate(terminalOp);
    }
    
    // 依赖于收集器的收集操作
    @Override
    @SuppressWarnings("unchecked")
    public final <R, A> R collect(Collector<? super P_OUT, A, R> collector) {
        A collection;
    
        // 1.并行流
        if(isParallel()
            // 2.收集器的容器支持并行存储
            && (collector.characteristics().contains(Collector.Characteristics.CONCURRENT))
            // 3.元素没有固定的遭遇顺序，或者收集操作不会保证确定的遭遇顺序
            && (!isOrdered() || collector.characteristics().contains(Collector.Characteristics.UNORDERED))) {
        
            /* 满足以上三个条件时， */
        
            // 获取收集器的容器
            collection = collector.supplier().get();
        
            // 获取择取元素的操作
            BiConsumer<A, ? super P_OUT> accumulator = collector.accumulator();
        
            // 直接使用并行遍历来处理元素
            forEach(e -> accumulator.accept(collection, e));
        
        } else {
            // 构造依赖收集器的汇总操作
            TerminalOp<P_OUT, A> terminalOp = ReduceOps.makeRef(collector);
        
            // 获取到执行完收集操作之后的收集器
            collection = evaluate(terminalOp);
        }
    
        // 如果不需要执行收尾操作，则直接返回
        if(collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return (R) collection;
        }
    
        // 获取收尾操作
        Function<A, R> finisher = collector.finisher();
    
        // 对收集器中的元素执行收尾操作
        return finisher.apply(collection);
    }
    
    // 计数
    @Override
    public final long count() {
        TerminalOp<P_OUT, Long> terminalOp = ReduceOps.makeRefCounting();
        return evaluate(terminalOp);
    }
    
    /*▲ 终端操作-非短路操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作-短路操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 找出第一个元素
    @Override
    public final Optional<P_OUT> findFirst() {
        TerminalOp<P_OUT, Optional<P_OUT>> terminalOp = FindOps.makeRef(true);
        return evaluate(terminalOp);
    }
    
    // 找到一个元素就返回，不管是不是第一个元素
    @Override
    public final Optional<P_OUT> findAny() {
        TerminalOp<P_OUT, Optional<P_OUT>> terminalOp = FindOps.makeRef(false);
        return evaluate(terminalOp);
    }
    
    // 判断是否存在元素满足predicate条件
    @Override
    public final boolean anyMatch(Predicate<? super P_OUT> predicate) {
        TerminalOp<P_OUT, Boolean> terminalOp = MatchOps.makeRef(predicate, MatchKind.ANY);
        return evaluate(terminalOp);
    }
    
    // 判断是否所有元素满足predicate条件
    @Override
    public final boolean allMatch(Predicate<? super P_OUT> predicate) {
        TerminalOp<P_OUT, Boolean> terminalOp = MatchOps.makeRef(predicate, MatchKind.ALL);
        return evaluate(terminalOp);
    }
    
    // 判断是否没有元素满足predicate条件
    @Override
    public final boolean noneMatch(Predicate<? super P_OUT> predicate) {
        TerminalOp<P_OUT, Boolean> terminalOp = MatchOps.makeRef(predicate, MatchKind.NONE);
        return evaluate(terminalOp);
    }
    
    /*▲ 终端操作-短路操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回流的形状
    @Override
    final StreamShape getOutputShape() {
        return StreamShape.REFERENCE;
    }
    
    /*▲ 参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 尝试用wrappedSink逐个消费spliterator中所有剩余元素。
     * 该行为会顺着sink链条向下游传播。
     *
     * 值的关注的是，这里每次消费元素之前，都要先询问wrappedSink是否应当停止接收数据。
     * 如果wrappedSink无法做出决定，则它应当继续询问下游的sink。
     *
     * 如果当前流阶段收到了应当停止接收数据的信号，则会立即停止择取工作，并返回true。
     *
     * 返回值指示是否中途取消了择取操作。
     *
     * 注1：该方法通常用在包含短路操作的流中。
     * 　 　比如需要查找一个大于0的元素，那么只要在流中发现了第一个大于0的元素，那么后续的择取操作就可以停止了。
     *
     * spliterator: 流迭代器，包含了当前所有待访问元素
     * wrappedSink: Sink链表上的第一个元素
     */
    @Override
    final boolean forEachWithCancel(Spliterator<P_OUT> spliterator, Sink<P_OUT> wrappedSink) {
        boolean cancelled;
        
        // 对spliterator中元素进行择取
        while(true) {
            
            // 判断是否应当停止接收数据
            cancelled = wrappedSink.cancellationRequested();
            
            // 如果遇到短路操作，可以取消后续操作
            if(cancelled) {
                return true;
            }
            
            // 使用wrappedSink消费spliterator中下一个元素，该行为会顺着sink链条向下游传播
            boolean hasMoreElem = spliterator.tryAdvance(wrappedSink);
            
            // 如果已经没有更多元素了，则结束择取
            if(!hasMoreElem) {
                break;
            }
        }
        
        return cancelled;
    }
    
    
    /*
     * 构造增强"数组"Node或"弹性缓冲区"Node(引用类型版本)
     *
     * exactSizeIfKnown: 元素数量
     * generator       : 生成内部数组的函数表达式
     *
     * 注：exactSizeIfKnown和generator都是被增强"数组"Node使用的
     */
    @Override
    final Node.Builder<P_OUT> makeNodeBuilder(long exactSizeIfKnown, IntFunction<P_OUT[]> generator) {
        return Nodes.builder(exactSizeIfKnown, generator);
    }
    
    /*
     * 并行搜集元素，中间依然会经过sink的择取操作(引用类型版本)。
     * 将spliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
     */
    @Override
    final <P_IN> Node<P_OUT> evaluateToNode(PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<P_OUT[]> generator) {
        return Nodes.collect(helper, spliterator, flattenTree, generator);
    }
    
    
    // 将当前阶段的流的Spliterator适配为Iterator(引用类型版本)
    @Override
    public final Iterator<P_OUT> iterator() {
        // 获取当前阶段的流的流迭代器
        Spliterator<P_OUT> spliterator = spliterator();
        return Spliterators.iterator(spliterator);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Source stage of a ReferencePipeline.
     *
     * @param <E_IN>  type of elements in the upstream source
     * @param <E_OUT> type of elements in produced by this stage
     *
     * @since 1.8
     */
    // 流的源头阶段(引用类型版本)，包含了数据源和可以对数据源执行的遍历、分割操作
    static class Head<E_IN, E_OUT> extends ReferencePipeline<E_IN, E_OUT> {
        
        /**
         * Constructor for the source stage of a Stream.
         *
         * @param source      {@code Supplier<Spliterator>} describing the stream
         *                    source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         */
        /*
         * 构造源头(head)阶段的流
         *
         * source     : 数据源迭代器工厂；可以从source中获取到原始数据的流迭代器，进而通过流迭代器来访问原始数据
         * sourceFlags: 源头阶段的流参数
         * parallel   : 是否为并行流
         */
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
        /*
         * 构造源头(head)阶段的流
         *
         * source     : 数据源迭代器；source不是原始数据，而是原始数据的流迭代器，可以通过流迭代器来访问原始数据
         * sourceFlags: 源头阶段的流参数
         * parallel   : 是否为并行流
         */
        Head(Spliterator<?> source, int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }
        
        // 源头阶段的流不允许调用此方法
        @Override
        final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }
        
        // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据；该方法仅由中间阶段的流使用
        @Override
        final Sink<E_IN> opWrapSink(int flags, Sink<E_OUT> downSink) {
            // 不是中间阶段，抛异常
            throw new UnsupportedOperationException();
        }
        
        // 遍历流中的元素，对其执行consumer操作
        @Override
        public void forEach(Consumer<? super E_OUT> consumer) {
            if(isParallel()) {
                super.forEach(consumer);
            } else {
                // 返回源头阶段的流迭代器；该方法只能由源头阶段的流调用
                Spliterator<E_OUT> spliterator = sourceStageSpliterator();
                
                spliterator.forEachRemaining(consumer);
            }
        }
        
        // 按遭遇顺序遍历流中的元素，对其执行consumer操作
        @Override
        public void forEachOrdered(Consumer<? super E_OUT> consumer) {
            if(isParallel()) {
                super.forEachOrdered(consumer);
            } else {
                // 返回源头阶段的流迭代器；该方法只能由源头阶段的流调用
                Spliterator<E_OUT> spliterator = sourceStageSpliterator();
                
                spliterator.forEachRemaining(consumer);
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
    // 流的[无]状态的中间阶段(引用类型版本)
    abstract static class StatelessOp<E_IN, E_OUT> extends ReferencePipeline<E_IN, E_OUT> {
    
        /**
         * Construct a new Stream by appending a stateless intermediate
         * operation to an existing stream.
         *
         * @param upstream   The upstream pipeline stage
         * @param inputShape The stream shape for the upstream pipeline stage
         * @param opFlags    Operation flags for the new stage
         */
        /*
         * 构造无状态的中间(intermediate)阶段的流，该阶段的流持有上一个阶段的流的引用
         *
         * upstream  : 链接到上一个流的引用
         * inputShape: 流的形状，即流中元素类型
         * opFlags   : 中间阶段的流的操作参数
         */
        StatelessOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
        }
    
        // 判断流的当前阶段是否有状态，这里总是返回false
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
    // 流的[有]状态的中间阶段(引用类型版本)
    abstract static class StatefulOp<E_IN, E_OUT> extends ReferencePipeline<E_IN, E_OUT> {
    
        /**
         * Construct a new Stream by appending a stateful intermediate operation
         * to an existing stream.
         *
         * @param upstream   The upstream pipeline stage
         * @param inputShape The stream shape for the upstream pipeline stage
         * @param opFlags    Operation flags for the new stage
         */
        /*
         * 构造有状态的中间(intermediate)阶段的流，该阶段的流持有上一个阶段的流的引用
         *
         * upstream  : 链接到上一个流的引用
         * inputShape: 流的形状，即流中元素类型
         * opFlags   : 中间阶段的流的操作参数
         */
        StatefulOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
        }
    
        // 判断流的当前阶段是否有状态，这里总是返回true
        @Override
        final boolean opIsStateful() {
            return true;
        }
    
        /*
         * 并行处理helper流阶段输出的元素，返回处理后的结果。
         *
         * 通常，如果helper流阶段的数据已经满足当前阶段的预期，则会直接返回helper流阶段的数据。
         * 否则，会视情形创建终端sink来处理helper流阶段的数据并返回。
         * 还可能不经过sink，而是直接处理helper流阶段的数据，并将处理后的数据返回。
         *
         * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
         * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
         * generator  : 必要的时候，创建存储处理结果的定长数组
         *
         * 不同的中间操作会重写该方法，以在该方法中使用不同的方式处理数据。
         * 返回值代表了当前(操作)阶段处理后的数据。
         *
         * 该方法仅在有状态的中间(操作)阶段上调用，如果某阶段的opIsStateful()返回true，则实现必须重写该方法。
         */
        @Override
        abstract <P_IN> Node<E_OUT> opEvaluateParallel(PipelineHelper<E_OUT> helper, Spliterator<P_IN> spliterator, IntFunction<E_OUT[]> generator);
    
    }
    
}
