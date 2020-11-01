/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.MatchOps.MatchKind;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code long}.
 *
 * @param <E_IN> type of elements in the upstream source
 *
 * @since 1.8
 */
/*
 * 流的long类型版本
 *
 * 参见：ReferencePipeline
 */
abstract class LongPipeline<E_IN> extends AbstractPipeline<E_IN, Long, LongStream> implements LongStream {
    
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
    LongPipeline(Supplier<? extends Spliterator<Long>> source, int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }
    
    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source      {@code Spliterator} describing the stream source
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
    LongPipeline(Spliterator<Long> source, int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }
    
    /**
     * Constructor for appending an intermediate operation onto an existing pipeline.
     *
     * @param upstream the upstream element source.
     * @param opFlags  the operation flags
     */
    /*
     * 构造流的中间(intermediate)阶段，该阶段持有流的前一个阶段的引用
     *
     * upstream: 链接到流的前一个阶段的引用
     * opFlags : 中间阶段的流的操作参数
     */
    LongPipeline(AbstractPipeline<?, E_IN, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回当前阶段的流的流迭代器；如果遇到并行流的有状态的中间阶段，则需要特殊处理
    @Override
    public final Spliterator.OfLong spliterator() {
        return adapt(super.spliterator());
    }
    
    /*
     * 构造一个"惰性"流迭代器
     *
     * "惰性"的含义是使用流迭代器时，需要从流迭代器工厂中获取
     */
    @Override
    @SuppressWarnings("unchecked")
    final Spliterator.OfLong lazySpliterator(Supplier<? extends Spliterator<Long>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator.OfLong((Supplier<Spliterator.OfLong>) supplier);
    }
    
    /*
     * 构造一个"包装"流迭代器，使用该流迭代器可以获取ph阶段的输出元素。
     *
     * ph        : 某个流阶段
     * supplier  : 相对于ph的上个(depth==0)的流阶段的流迭代器工厂
     * isParallel: ph所在流是否需要并行执行
     */
    @Override
    final <P_IN> Spliterator<Long> wrap(PipelineHelper<Long> ph, Supplier<Spliterator<P_IN>> supplier, boolean isParallel) {
        return new StreamSpliterators.LongWrappingSpliterator<>(ph, supplier, isParallel);
    }
    
    /*▲ 流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中间操作-无状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 筛选数据
    @Override
    public final LongStream filter(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
    
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SIZED) {
        
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> downSink) {
            
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedLong<Long>(downSink) {
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                
                    @Override
                    public void accept(long t) {
                        if(predicate.test(t)) {
                            downstream.accept(t);
                        }
                    }
                };
            }
        };
    }
    
    // 映射数据
    @Override
    public final LongStream map(LongUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
    
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
        
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> downSink) {
            
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedLong<Long>(downSink) {
                    @Override
                    public void accept(long t) {
                        downstream.accept(mapper.applyAsLong(t));
                    }
                };
            }
        };
    }
    
    // 映射数据
    @Override
    public final IntStream mapToInt(LongToIntFunction mapper) {
        Objects.requireNonNull(mapper);
    
        return new IntPipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
        
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Integer> downSink) {
            
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedLong<Integer>(downSink) {
                    @Override
                    public void accept(long t) {
                        downstream.accept(mapper.applyAsInt(t));
                    }
                };
            }
        };
    }
    
    // 映射数据
    @Override
    public final DoubleStream mapToDouble(LongToDoubleFunction mapper) {
        Objects.requireNonNull(mapper);
    
        return new DoublePipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
        
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Double> downSink) {
            
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedLong<Double>(downSink) {
                    @Override
                    public void accept(long t) {
                        downstream.accept(mapper.applyAsDouble(t));
                    }
                };
            }
        };
    }
    
    // 映射数据
    @Override
    public final <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return mapToObj(mapper, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT);
    }
    
    // 数据降维
    @Override
    public final LongStream flatMap(LongFunction<? extends LongStream> mapper) {
        Objects.requireNonNull(mapper);
    
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
        
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> downSink) {
            
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedLong<Long>(downSink) {
                    // true if cancellationRequested() has been called
                    boolean cancellationRequestedCalled;
                
                    // cache the consumer to avoid creation on every accepted element
                    LongConsumer downstreamAsLong = downstream::accept;
                
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                
                    @Override
                    public void accept(long t) {
                        try(LongStream result = mapper.apply(t)) {
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
                        /*
                         * If this method is called then an operation within the stream
                         * pipeline is short-circuiting (see AbstractPipeline.copyInto).
                         * Note that we cannot differentiate between an upstream or downstream operation
                         */
                        cancellationRequestedCalled = true;
                        return downstream.cancellationRequested();
                    }
                };
            }
        };
    }
    
    // 用于查看流的内部结构，不会对流的结构产生影响
    @Override
    public final LongStream peek(LongConsumer action) {
        Objects.requireNonNull(action);
    
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, 0) {
        
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> downSink) {
            
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedLong<Long>(downSink) {
                    @Override
                    public void accept(long t) {
                        action.accept(t);
                        downstream.accept(t);
                    }
                };
            }
        };
    }
    
    // 装箱
    @Override
    public final Stream<Long> boxed() {
        return mapToObj(Long::valueOf, 0);
    }
    
    // 中间操作，返回等效的无序流
    @Override
    public LongStream unordered() {
        if(!isOrdered()) {
            return this;
        }
        
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_ORDERED) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> downSink) {
                return downSink;
            }
        };
    }
    
    // 转换为DoubleStream
    @Override
    public final DoubleStream asDoubleStream() {
        return new DoublePipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_DISTINCT) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Double> downSink) {
                
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedLong<Double>(downSink) {
                    @Override
                    public void accept(long t) {
                        downstream.accept((double) t);
                    }
                };
            }
        };
    }
    
    /*▲ 中间操作-无状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中间操作-有状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 去重
    @Override
    public final LongStream distinct() {
        /*
         * While functional and quick to implement, this approach is not very efficient.
         * An efficient version requires a long-specific map/set implementation.
         */
        Stream<Long> longStream = boxed();
    
        Stream<Long> distinct = longStream.distinct();
    
        return distinct.mapToLong(i -> (long) i);
    }
    
    // 排序（默认升序）
    @Override
    public final LongStream sorted() {
        return SortedOps.makeLong(this);
    }
    
    // 只显示前maxSize个元素
    @Override
    public final LongStream limit(long maxSize) {
        if(maxSize<0)
            throw new IllegalArgumentException(Long.toString(maxSize));
        return SliceOps.makeLong(this, 0, maxSize);
    }
    
    // 跳过前n个元素
    @Override
    public final LongStream skip(long n) {
        if(n<0) {
            throw new IllegalArgumentException(Long.toString(n));
        }
    
        if(n == 0) {
            return this;
        }
    
        return SliceOps.makeLong(this, n, -1);
    }
    
    // "保存前缀"：保存起初遇到的满足predicate条件的元素；只要遇到首个不满足条件的元素，就结束后续的保存动作
    @Override
    public final LongStream takeWhile(LongPredicate predicate) {
        return WhileOps.makeTakeWhileLong(this, predicate);
    }
    
    // "丢弃前缀"：丢弃起初遇到的满足predicate条件的元素；只要遇到首个不满足条件的元素，就开始保存它后及其后面的元素
    @Override
    public final LongStream dropWhile(LongPredicate predicate) {
        return WhileOps.makeDropWhileLong(this, predicate);
    }
    
    /*▲ 中间操作-有状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作-非短路操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将数据存入long数组返回
    @Override
    public final long[] toArray() {
    
        Node.OfLong node = (Node.OfLong) evaluateToArrayNode(Long[]::new);
    
        /*
         * 尝试将树状Node转换为普通"数组"Node；如果node不是树状node，那直接就返回了。
         * 将树状Node中的元素并行地复制到long数组中，然后再将该数组封装成普通"数组"Node后返回(long类型版本)。
         */
        Node.OfLong flatten = Nodes.flattenLong(node);
    
        // 将Node中的元素存入基本类型数组后返回
        return flatten.asPrimitiveArray();
    }
    
    // 遍历，并执行consumer操作
    @Override
    public void forEach(LongConsumer consumer) {
        TerminalOp<Long, Void> terminalOp = ForEachOps.makeLong(consumer, false);
        evaluate(terminalOp);
    }
    
    // 按遭遇顺序遍历，并执行consumer操作
    @Override
    public void forEachOrdered(LongConsumer consumer) {
        TerminalOp<Long, Void> terminalOp = ForEachOps.makeLong(consumer, true);
        evaluate(terminalOp);
    }
    
    // 求最小值
    @Override
    public final OptionalLong min() {
        return reduce(Math::min);
    }
    
    // 求最大值
    @Override
    public final OptionalLong max() {
        return reduce(Math::max);
    }
    
    // 无初始状态的汇总操作(引用类型版本)
    @Override
    public final OptionalLong reduce(LongBinaryOperator op) {
        TerminalOp<Long, OptionalLong> terminalOp = ReduceOps.makeLong(op);
        return evaluate(terminalOp);
    }
    
    // 有初始状态的汇总操作(引用类型版本)
    @Override
    public final long reduce(long identity, LongBinaryOperator op) {
        TerminalOp<Long, Long> terminalOp = ReduceOps.makeLong(identity, op);
        return evaluate(terminalOp);
    }
    
    // 有初始状态的消费操作(引用类型版本)
    @Override
    public final <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(combiner);
    
        BinaryOperator<R> operator = (left, right) -> {
            combiner.accept(left, right);
            return left;
        };
    
        TerminalOp<Long, R> terminalOp = ReduceOps.makeLong(supplier, accumulator, operator);
        return evaluate(terminalOp);
    }
    
    // 计数
    @Override
    public final long count() {
        TerminalOp<Long, Long> terminalOp = ReduceOps.makeLongCounting();
        return evaluate(terminalOp);
    }
    
    // 求和
    @Override
    public final long sum() {
        // use better algorithm to compensate for intermediate overflow?
        return reduce(0, Long::sum);
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
        return avg[0]>0 ? OptionalDouble.of((double) avg[1] / avg[0]) : OptionalDouble.empty();
    }
    
    // 信息统计
    @Override
    public final LongSummaryStatistics summaryStatistics() {
        return collect(LongSummaryStatistics::new, LongSummaryStatistics::accept, LongSummaryStatistics::combine);
    }
    
    /*▲ 终端操作-非短路操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作-短路操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 找出第一个元素
    @Override
    public final OptionalLong findFirst() {
        TerminalOp<Long, OptionalLong> terminalOp = FindOps.makeLong(true);
        return evaluate(terminalOp);
    }
    
    // 找到一个元素就返回，不管是不是第一个元素
    @Override
    public final OptionalLong findAny() {
        TerminalOp<Long, OptionalLong> terminalOp = FindOps.makeLong(false);
        return evaluate(terminalOp);
    }
    
    // 判断是否存在元素满足predicate条件
    @Override
    public final boolean anyMatch(LongPredicate predicate) {
        TerminalOp<Long, Boolean> terminalOp = MatchOps.makeLong(predicate, MatchKind.ANY);
        return evaluate(terminalOp);
    }
    
    // 判断是否所有元素满足predicate条件
    @Override
    public final boolean allMatch(LongPredicate predicate) {
        TerminalOp<Long, Boolean> terminalOp = MatchOps.makeLong(predicate, MatchKind.ALL);
        return evaluate(terminalOp);
    }
    
    // 判断是否所有元素满足predicate条件
    @Override
    public final boolean noneMatch(LongPredicate predicate) {
        TerminalOp<Long, Boolean> terminalOp = MatchOps.makeLong(predicate, MatchKind.NONE);
        return evaluate(terminalOp);
    }
    
    /*▲ 终端操作-短路操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回流的形状
    @Override
    final StreamShape getOutputShape() {
        return StreamShape.LONG_VALUE;
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
    final boolean forEachWithCancel(Spliterator<Long> spliterator, Sink<Long> wrappedSink) {
        Spliterator.OfLong adaptSpliterator = adapt(spliterator);
        LongConsumer adaptSink = adapt(wrappedSink);
        
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
            boolean hasMoreElem = adaptSpliterator.tryAdvance(adaptSink);
            
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
    final Node.Builder<Long> makeNodeBuilder(long exactSizeIfKnown, IntFunction<Long[]> generator) {
        return Nodes.longBuilder(exactSizeIfKnown);
    }
    
    /*
     * 并行搜集元素，中间依然会经过sink的择取操作(long类型版本)。
     * 将spliterator中的元素并行地收集到long数组中，然后将该数组封装到Node中返回。
     *
     * 注：这里不需要使用generator入参，创建long数组的方式由内部实现。
     */
    @Override
    final <P_IN> Node<Long> evaluateToNode(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<Long[]> generator) {
        return Nodes.collectLong(helper, spliterator, flattenTree);
    }
    
    
    // 将当前阶段的流的Spliterator适配为Iterator(引用类型版本)
    @Override
    public final PrimitiveIterator.OfLong iterator() {
        return Spliterators.iterator(spliterator());
    }
    
    // 映射数据
    private <U> Stream<U> mapToObj(LongFunction<? extends U> mapper, int opFlags) {
        return new ReferencePipeline.StatelessOp<Long, U>(this, StreamShape.LONG_VALUE, opFlags) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<U> downSink) {
                
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedLong<U>(downSink) {
                    @Override
                    public void accept(long t) {
                        downstream.accept(mapper.apply(t));
                    }
                };
            }
        };
    }
    
    /**
     * Adapt a {@code Sink<Long> to an {@code LongConsumer}, ideally simply
     * by casting.
     */
    // 将Sink适配为LongConsumer
    private static LongConsumer adapt(Sink<Long> sink) {
        if(sink instanceof LongConsumer) {
            return (LongConsumer) sink;
        } else {
            if(Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class, "using LongStream.adapt(Sink<Long> s)");
            return sink::accept;
        }
    }
    
    /**
     * Adapt a {@code Spliterator<Long>} to a {@code Spliterator.OfLong}.
     *
     * @implNote The implementation attempts to cast to a Spliterator.OfLong, and throws
     * an exception if this cast is not possible.
     */
    // 适配流迭代器
    private static Spliterator.OfLong adapt(Spliterator<Long> s) {
        if(s instanceof Spliterator.OfLong) {
            return (Spliterator.OfLong) s;
        } else {
            if(Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class, "using LongStream.adapt(Spliterator<Long> s)");
            throw new UnsupportedOperationException("LongStream.adapt(Spliterator<Long> s)");
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Source stage of a LongPipeline.
     *
     * @param <E_IN> type of elements in the upstream source
     *
     * @since 1.8
     */
    // 流的源头阶段(long类型版本)，包含了数据源和可以对数据源执行的遍历、分割操作
    static class Head<E_IN> extends LongPipeline<E_IN> {
        
        /**
         * Constructor for the source stage of a LongStream.
         *
         * @param source      {@code Supplier<Spliterator>} describing the stream
         *                    source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel    {@code true} if the pipeline is parallel
         */
        /*
         * 构造源头(head)阶段的流
         *
         * source     : 数据源迭代器工厂；可以从source中获取到原始数据的流迭代器，进而通过流迭代器来访问原始数据
         * sourceFlags: 源头阶段的流参数
         * parallel   : 是否为并行流
         */
        Head(Supplier<? extends Spliterator<Long>> source, int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }
        
        /**
         * Constructor for the source stage of a LongStream.
         *
         * @param source      {@code Spliterator} describing the stream source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel    {@code true} if the pipeline is parallel
         */
        /*
         * 构造源头(head)阶段的流
         *
         * source     : 数据源迭代器；source不是原始数据，而是原始数据的流迭代器，可以通过流迭代器来访问原始数据
         * sourceFlags: 源头阶段的流参数
         * parallel   : 是否为并行流
         */
        Head(Spliterator<Long> source, int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }
        
        // 源头阶段的流不允许调用此方法
        @Override
        final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }
        
        // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据；该方法仅由中间阶段的流使用
        @Override
        final Sink<E_IN> opWrapSink(int flags, Sink<Long> downSink) {
            // 不是中间阶段，抛异常
            throw new UnsupportedOperationException();
        }
        
        // 遍历流中的元素，对其执行consumer操作
        @Override
        public void forEach(LongConsumer consumer) {
            if(isParallel()) {
                super.forEach(consumer);
            } else {
                // 返回源头阶段的流迭代器；该方法只能由源头阶段的流调用
                Spliterator<Long> spliterator = sourceStageSpliterator();
                
                adapt(spliterator).forEachRemaining(consumer);
            }
        }
        
        // 按遭遇顺序遍历流中的元素，对其执行consumer操作
        @Override
        public void forEachOrdered(LongConsumer consumer) {
            if(isParallel()) {
                super.forEachOrdered(consumer);
            } else {
                // 返回源头阶段的流迭代器；该方法只能由源头阶段的流调用
                Spliterator<Long> spliterator = sourceStageSpliterator();
                
                adapt(spliterator).forEachRemaining(consumer);
            }
        }
    }
    
    /**
     * Base class for a stateless intermediate stage of a LongStream.
     *
     * @param <E_IN> type of elements in the upstream source
     *
     * @since 1.8
     */
    // 流的[无]状态的中间阶段(long类型版本)
    abstract static class StatelessOp<E_IN> extends LongPipeline<E_IN> {
    
        /**
         * Construct a new LongStream by appending a stateless intermediate
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
     * Base class for a stateful intermediate stage of a LongStream.
     *
     * @param <E_IN> type of elements in the upstream source
     *
     * @since 1.8
     */
    // 流的[有]状态的中间阶段(long类型版本)
    abstract static class StatefulOp<E_IN> extends LongPipeline<E_IN> {
    
        /**
         * Construct a new LongStream by appending a stateful intermediate
         * operation to an existing stream.
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
        abstract <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator, IntFunction<Long[]> generator);
    
    }
    
}
