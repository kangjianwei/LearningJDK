/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * Factory for instances of a takeWhile and dropWhile operations
 * that produce subsequences of their input stream.
 *
 * @since 9
 */
// 应用在有状态的中间阶段的辅助类，服务于takeWhile()和dropWhile()方法
final class WhileOps {
    
    static final int TAKE_FLAGS = StreamOpFlag.NOT_SIZED | StreamOpFlag.IS_SHORT_CIRCUIT;
    static final int DROP_FLAGS = StreamOpFlag.NOT_SIZED;
    
    
    /**
     * Appends a "takeWhile" operation to the provided Stream.
     *
     * @param <T>       the type of both input and output elements
     * @param upstream  a reference stream with element type T
     * @param predicate the predicate that returns false to halt taking.
     */
    // 返回"保存前缀"的流(引用类型版本)
    static <T> Stream<T> makeTakeWhileRef(AbstractPipeline<?, T, ?> upstream, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        
        return new ReferencePipeline.StatefulOp<T, T>(upstream, StreamShape.REFERENCE, TAKE_FLAGS) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<T> opWrapSink(int flags, Sink<T> downSink) {
                
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<T, T>(downSink) {
                    boolean take = true;
                    
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                    
                    // 保存前缀
                    @Override
                    public void accept(T t) {
                        // 只要遇到首个不合要求的元素，就结束择取过程
                        if(take && (take = predicate.test(t))) {
                            downstream.accept(t);
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        return !take || downstream.cancellationRequested();
                    }
                };
            }
            
            /*
             * 并行处理helper流阶段输出的元素，然后将处理结果封装到流迭代器中返回。
             *
             * 通常，如果helper流阶段的数据已经满足当前阶段的预期，则会直接包装helper流阶段的数据到流迭代器中。
             * 否则，会视情形创建终端sink来处理helper流阶段的数据，并同样将其封装到流迭代器中返回。
             * 还可能会对spliterator做进一步的包装，返回一个与当前操作匹配的流迭代器。
             *
             * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
             * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
             *
             * 不同的中间操作会重写该方法，以在该方法中使用不同的方式处理数据。
             * 返回值代表了当前阶段的流处理器。
             *
             * 该方法仅在有状态的中间(操作)阶段上调用，如果某阶段的opIsStateful()返回true，则实现必须重写该方法。
             */
            @Override
            <P_IN> Spliterator<T> opEvaluateParallelLazy(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有固定的遭遇顺序
                if(StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    // 返回用于创建T类型数组的函数表达式
                    IntFunction<T[]> generator = Nodes.castingArray();
                    
                    // 并行处理helper流阶段输出的元素，返回处理后的结果
                    Node<T> node = opEvaluateParallel(helper, spliterator, generator);
                    
                    // 返回node的流迭代器
                    return node.spliterator();
                }
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<T> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                // 返回"保存前缀"流迭代器，这是"无序前缀"流迭代器的一种
                return new UnorderedWhileSpliterator.OfRef.Taking<>(wrapSpliterator, false, predicate);
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
            <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> helper, Spliterator<P_IN> spliterator, IntFunction<T[]> generator) {
                
                // 构造用于"保存前缀"的任务
                TakeWhileTask<P_IN, T> task = new TakeWhileTask<>(this, helper, spliterator, generator);
                
                // 执行任务
                return task.invoke();
            }
        };
    }
    
    /**
     * Appends a "takeWhile" operation to the provided IntStream.
     *
     * @param upstream  a reference stream with element type T
     * @param predicate the predicate that returns false to halt taking.
     */
    // 返回"保存前缀"的流(int类型版本)
    static IntStream makeTakeWhileInt(AbstractPipeline<?, Integer, ?> upstream, IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        
        return new IntPipeline.StatefulOp<Integer>(upstream, StreamShape.INT_VALUE, TAKE_FLAGS) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> downSink) {
                
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedInt<Integer>(downSink) {
                    boolean take = true;
                    
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                    
                    // 保存前缀
                    @Override
                    public void accept(int t) {
                        // 只要遇到首个不合要求的元素，就结束择取过程
                        if(take && (take = predicate.test(t))) {
                            downstream.accept(t);
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        return !take || downstream.cancellationRequested();
                    }
                };
            }
            
            /*
             * 并行处理helper流阶段输出的元素，然后将处理结果封装到流迭代器中返回。
             *
             * 通常，如果helper流阶段的数据已经满足当前阶段的预期，则会直接包装helper流阶段的数据到流迭代器中。
             * 否则，会视情形创建终端sink来处理helper流阶段的数据，并同样将其封装到流迭代器中返回。
             * 还可能会对spliterator做进一步的包装，返回一个与当前操作匹配的流迭代器。
             *
             * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
             * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
             *
             * 不同的中间操作会重写该方法，以在该方法中使用不同的方式处理数据。
             * 返回值代表了当前阶段的流处理器。
             *
             * 该方法仅在有状态的中间(操作)阶段上调用，如果某阶段的opIsStateful()返回true，则实现必须重写该方法。
             */
            @Override
            <P_IN> Spliterator<Integer> opEvaluateParallelLazy(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有固定的遭遇顺序
                if(StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    // 返回用于创建int类型数组的函数表达式
                    IntFunction<Integer[]> generator = Integer[]::new;
                    
                    // 并行处理helper流阶段输出的元素，返回处理后的结果
                    Node<Integer> node = opEvaluateParallel(helper, spliterator, generator);
                    
                    // 返回node的流迭代器
                    return node.spliterator();
                }
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<Integer> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                // 返回"保存前缀"流迭代器，这是"无序前缀"流迭代器的一种
                return new UnorderedWhileSpliterator.OfInt.Taking((Spliterator.OfInt) wrapSpliterator, false, predicate);
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
            <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator, IntFunction<Integer[]> generator) {
                
                // 构造用于"保存前缀"的任务
                TakeWhileTask<P_IN, Integer> task = new TakeWhileTask<>(this, helper, spliterator, generator);
                
                // 执行任务
                return task.invoke();
            }
        };
    }
    
    /**
     * Appends a "takeWhile" operation to the provided LongStream.
     *
     * @param upstream  a reference stream with element type T
     * @param predicate the predicate that returns false to halt taking.
     */
    // 返回"保存前缀"的流(long类型版本)
    static LongStream makeTakeWhileLong(AbstractPipeline<?, Long, ?> upstream, LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        
        return new LongPipeline.StatefulOp<Long>(upstream, StreamShape.LONG_VALUE, TAKE_FLAGS) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> downSink) {
                
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedLong<Long>(downSink) {
                    boolean take = true;
                    
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                    
                    // 保存前缀
                    @Override
                    public void accept(long t) {
                        // 只要遇到首个不合要求的元素，就结束择取过程
                        if(take && (take = predicate.test(t))) {
                            downstream.accept(t);
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        return !take || downstream.cancellationRequested();
                    }
                };
            }
            
            /*
             * 并行处理helper流阶段输出的元素，然后将处理结果封装到流迭代器中返回。
             *
             * 通常，如果helper流阶段的数据已经满足当前阶段的预期，则会直接包装helper流阶段的数据到流迭代器中。
             * 否则，会视情形创建终端sink来处理helper流阶段的数据，并同样将其封装到流迭代器中返回。
             * 还可能会对spliterator做进一步的包装，返回一个与当前操作匹配的流迭代器。
             *
             * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
             * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
             *
             * 不同的中间操作会重写该方法，以在该方法中使用不同的方式处理数据。
             * 返回值代表了当前阶段的流处理器。
             *
             * 该方法仅在有状态的中间(操作)阶段上调用，如果某阶段的opIsStateful()返回true，则实现必须重写该方法。
             */
            @Override
            <P_IN> Spliterator<Long> opEvaluateParallelLazy(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有固定的遭遇顺序
                if(StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    // 返回用于创建long类型数组的函数表达式
                    IntFunction<Long[]> generator = Long[]::new;
                    
                    // 并行处理helper流阶段输出的元素，返回处理后的结果
                    Node<Long> node = opEvaluateParallel(helper, spliterator, generator);
                    
                    // 返回node的流迭代器
                    return node.spliterator();
                }
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<Long> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                // 返回"保存前缀"流迭代器，这是"无序前缀"流迭代器的一种
                return new UnorderedWhileSpliterator.OfLong.Taking((Spliterator.OfLong) wrapSpliterator, false, predicate);
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
            <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator, IntFunction<Long[]> generator) {
                
                // 构造用于"保存前缀"的任务
                TakeWhileTask<P_IN, Long> task = new TakeWhileTask<>(this, helper, spliterator, generator);
                
                // 执行任务
                return task.invoke();
            }
        };
    }
    
    /**
     * Appends a "takeWhile" operation to the provided DoubleStream.
     *
     * @param upstream  a reference stream with element type T
     * @param predicate the predicate that returns false to halt taking.
     */
    // 返回"保存前缀"的流(double类型版本)
    static DoubleStream makeTakeWhileDouble(AbstractPipeline<?, Double, ?> upstream, DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        
        return new DoublePipeline.StatefulOp<Double>(upstream, StreamShape.DOUBLE_VALUE, TAKE_FLAGS) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> downSink) {
                
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedDouble<Double>(downSink) {
                    boolean take = true;
                    
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }
                    
                    // 保存前缀
                    @Override
                    public void accept(double t) {
                        // 只要遇到首个不合要求的元素，就结束择取过程
                        if(take && (take = predicate.test(t))) {
                            downstream.accept(t);
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        return !take || downstream.cancellationRequested();
                    }
                };
            }
            
            /*
             * 并行处理helper流阶段输出的元素，然后将处理结果封装到流迭代器中返回。
             *
             * 通常，如果helper流阶段的数据已经满足当前阶段的预期，则会直接包装helper流阶段的数据到流迭代器中。
             * 否则，会视情形创建终端sink来处理helper流阶段的数据，并同样将其封装到流迭代器中返回。
             * 还可能会对spliterator做进一步的包装，返回一个与当前操作匹配的流迭代器。
             *
             * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
             * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
             *
             * 不同的中间操作会重写该方法，以在该方法中使用不同的方式处理数据。
             * 返回值代表了当前阶段的流处理器。
             *
             * 该方法仅在有状态的中间(操作)阶段上调用，如果某阶段的opIsStateful()返回true，则实现必须重写该方法。
             */
            @Override
            <P_IN> Spliterator<Double> opEvaluateParallelLazy(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有固定的遭遇顺序
                if(StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    // 返回用于创建double类型数组的函数表达式
                    IntFunction<Double[]> generator = Double[]::new;
                    
                    // 并行处理helper流阶段输出的元素，返回处理后的结果
                    Node<Double> node = opEvaluateParallel(helper, spliterator, generator);
                    
                    // 返回node的流迭代器
                    return node.spliterator();
                }
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<Double> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                // 返回"保存前缀"流迭代器，这是"无序前缀"流迭代器的一种
                return new UnorderedWhileSpliterator.OfDouble.Taking((Spliterator.OfDouble) wrapSpliterator, false, predicate);
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
            <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator, IntFunction<Double[]> generator) {
                
                // 构造用于"保存前缀"的任务
                TakeWhileTask<P_IN, Double> task = new TakeWhileTask<>(this, helper, spliterator, generator);
                
                // 执行任务
                return task.invoke();
            }
        };
    }
    
    
    /**
     * Appends a "dropWhile" operation to the provided Stream.
     *
     * @param <T>       the type of both input and output elements
     * @param upstream  a reference stream with element type T
     * @param predicate the predicate that returns false to halt dropping.
     */
    // 返回"丢弃前缀"的流(引用类型版本)
    static <T> Stream<T> makeDropWhileRef(AbstractPipeline<?, T, ?> upstream, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        
        // 中间阶段的流
        class Op extends ReferencePipeline.StatefulOp<T, T> implements DropWhileOp<T> {
            
            public Op(AbstractPipeline<?, T, ?> upstream, StreamShape inputShape, int opFlags) {
                super(upstream, inputShape, opFlags);
            }
            
            public DropWhileSink<T> opWrapSink(Sink<T> downSink, boolean retainAndCountDroppedElements) {
                
                class OpSink extends Sink.ChainedReference<T, T> implements DropWhileSink<T> {
                    long dropCount; // 记录满足条件的元素数量
                    boolean take;
                    
                    OpSink() {
                        super(downSink);
                    }
                    
                    // 丢弃前缀
                    @Override
                    public void accept(T t) {
                        boolean takeElement = take || (take = !predicate.test(t));
                        
                        /*
                         * 遇到满足条件的元素时，要对其进行计数；直到遇见首个不满足条件的元素，则此后不再统计元素数量。
                         * 注：此分支仅用于元素有固定遭遇顺序的并行流。
                         */
                        if(retainAndCountDroppedElements) {
                            if(!takeElement) {
                                dropCount++;
                            }
                            
                            // 不管满不满足条件，先暂时把元素放行，但是dropCount中已经记录了需要跳过的元素数量
                            downstream.accept(t);
                            
                            /*
                             * 遇到满足条件的元素时，则丢弃元素；直到遇见首个不满足条件的元素，则此后开始保存元素。
                             * 注：此分支应用于顺序流。
                             */
                        } else {
                            if(takeElement) {
                                downstream.accept(t);
                            }
                        }
                    }
                    
                    @Override
                    public long getDropCount() {
                        return dropCount;
                    }
                }
                
                return new OpSink();
            }
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<T> opWrapSink(int flags, Sink<T> downSink) {
                return opWrapSink(downSink, false);
            }
            
            /*
             * 并行处理helper流阶段输出的元素，然后将处理结果封装到流迭代器中返回。
             *
             * 通常，如果helper流阶段的数据已经满足当前阶段的预期，则会直接包装helper流阶段的数据到流迭代器中。
             * 否则，会视情形创建终端sink来处理helper流阶段的数据，并同样将其封装到流迭代器中返回。
             * 还可能会对spliterator做进一步的包装，返回一个与当前操作匹配的流迭代器。
             *
             * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
             * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
             *
             * 不同的中间操作会重写该方法，以在该方法中使用不同的方式处理数据。
             * 返回值代表了当前阶段的流处理器。
             *
             * 该方法仅在有状态的中间(操作)阶段上调用，如果某阶段的opIsStateful()返回true，则实现必须重写该方法。
             */
            @Override
            <P_IN> Spliterator<T> opEvaluateParallelLazy(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有固定的遭遇顺序
                if(StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    // 返回用于创建T类型数组的函数表达式
                    IntFunction<T[]> generator = Nodes.castingArray();
                    
                    // 并行处理helper流阶段输出的元素，返回处理后的结果
                    Node<T> node = opEvaluateParallel(helper, spliterator, generator);
                    
                    // 返回node的流迭代器
                    return node.spliterator();
                }
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<T> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                // 返回"丢弃前缀"流迭代器，这是"无序前缀"流迭代器的一种
                return new UnorderedWhileSpliterator.OfRef.Dropping<>(wrapSpliterator, false, predicate);
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
            <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> helper, Spliterator<P_IN> spliterator, IntFunction<T[]> generator) {
                
                // 构造用于"丢弃前缀"的任务
                DropWhileTask<P_IN, T> task = new DropWhileTask<>(this, helper, spliterator, generator);
                
                // 执行任务
                return task.invoke();
            }
        }
        
        return new Op(upstream, StreamShape.REFERENCE, DROP_FLAGS);
    }
    
    /**
     * Appends a "dropWhile" operation to the provided IntStream.
     *
     * @param upstream  a reference stream with element type T
     * @param predicate the predicate that returns false to halt dropping.
     */
    // 返回"丢弃前缀"的流(int类型版本)
    static IntStream makeDropWhileInt(AbstractPipeline<?, Integer, ?> upstream, IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        
        // 中间阶段的流
        class Op extends IntPipeline.StatefulOp<Integer> implements DropWhileOp<Integer> {
            public Op(AbstractPipeline<?, Integer, ?> upstream, StreamShape inputShape, int opFlags) {
                super(upstream, inputShape, opFlags);
            }
            
            public DropWhileSink<Integer> opWrapSink(Sink<Integer> downSink, boolean retainAndCountDroppedElements) {
                
                class OpSink extends Sink.ChainedInt<Integer> implements DropWhileSink<Integer> {
                    long dropCount; // 记录满足条件的元素数量
                    boolean take;
                    
                    OpSink() {
                        super(downSink);
                    }
                    
                    // 丢弃前缀
                    @Override
                    public void accept(int t) {
                        boolean takeElement = take || (take = !predicate.test(t));
                        
                        /*
                         * 遇到满足条件的元素时，要对其进行计数；直到遇见首个不满足条件的元素，则此后不再统计元素数量。
                         * 注：此分支仅用于元素有固定遭遇顺序的并行流。
                         */
                        if(retainAndCountDroppedElements) {
                            if(!takeElement) {
                                dropCount++;
                            }
                            
                            // 不管满不满足条件，先暂时把元素放行，但是dropCount中已经记录了需要跳过的元素数量
                            downstream.accept(t);
                            
                            /*
                             * 遇到满足条件的元素时，则丢弃元素；直到遇见首个不满足条件的元素，则此后开始保存元素。
                             * 注：此分支应用于顺序流。
                             */
                        } else {
                            if(takeElement) {
                                downstream.accept(t);
                            }
                        }
                    }
                    
                    @Override
                    public long getDropCount() {
                        return dropCount;
                    }
                }
                
                return new OpSink();
            }
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> downSink) {
                return opWrapSink(downSink, false);
            }
            
            /*
             * 并行处理helper流阶段输出的元素，然后将处理结果封装到流迭代器中返回。
             *
             * 通常，如果helper流阶段的数据已经满足当前阶段的预期，则会直接包装helper流阶段的数据到流迭代器中。
             * 否则，会视情形创建终端sink来处理helper流阶段的数据，并同样将其封装到流迭代器中返回。
             * 还可能会对spliterator做进一步的包装，返回一个与当前操作匹配的流迭代器。
             *
             * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
             * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
             *
             * 不同的中间操作会重写该方法，以在该方法中使用不同的方式处理数据。
             * 返回值代表了当前阶段的流处理器。
             *
             * 该方法仅在有状态的中间(操作)阶段上调用，如果某阶段的opIsStateful()返回true，则实现必须重写该方法。
             */
            @Override
            <P_IN> Spliterator<Integer> opEvaluateParallelLazy(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有固定的遭遇顺序
                if(StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    // 返回用于创建int类型数组的函数表达式
                    IntFunction<Integer[]> generator = Integer[]::new;
                    
                    // 并行处理helper流阶段输出的元素，返回处理后的结果
                    Node<Integer> node = opEvaluateParallel(helper, spliterator, generator);
                    
                    // 返回node的流迭代器
                    return node.spliterator();
                }
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<Integer> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                // 返回"丢弃前缀"流迭代器，这是"无序前缀"流迭代器的一种
                return new UnorderedWhileSpliterator.OfInt.Dropping((Spliterator.OfInt) wrapSpliterator, false, predicate);
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
            <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator, IntFunction<Integer[]> generator) {
                
                // 构造用于"丢弃前缀"的任务
                DropWhileTask<P_IN, Integer> task = new DropWhileTask<>(this, helper, spliterator, generator);
                
                // 执行任务
                return task.invoke();
            }
        }
        
        return new Op(upstream, StreamShape.INT_VALUE, DROP_FLAGS);
    }
    
    /**
     * Appends a "dropWhile" operation to the provided LongStream.
     *
     * @param upstream  a reference stream with element type T
     * @param predicate the predicate that returns false to halt dropping.
     */
    // 返回"丢弃前缀"的流(long类型版本)
    static LongStream makeDropWhileLong(AbstractPipeline<?, Long, ?> upstream, LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        
        // 中间阶段的流
        class Op extends LongPipeline.StatefulOp<Long> implements DropWhileOp<Long> {
            public Op(AbstractPipeline<?, Long, ?> upstream, StreamShape inputShape, int opFlags) {
                super(upstream, inputShape, opFlags);
            }
            
            public DropWhileSink<Long> opWrapSink(Sink<Long> downSink, boolean retainAndCountDroppedElements) {
                
                class OpSink extends Sink.ChainedLong<Long> implements DropWhileSink<Long> {
                    long dropCount; // 记录满足条件的元素数量
                    boolean take;
                    
                    OpSink() {
                        super(downSink);
                    }
                    
                    // 丢弃前缀
                    @Override
                    public void accept(long t) {
                        boolean takeElement = take || (take = !predicate.test(t));
                        
                        /*
                         * 遇到满足条件的元素时，要对其进行计数；直到遇见首个不满足条件的元素，则此后不再统计元素数量。
                         * 注：此分支仅用于元素有固定遭遇顺序的并行流。
                         */
                        if(retainAndCountDroppedElements) {
                            if(!takeElement) {
                                dropCount++;
                            }
                            
                            // 不管满不满足条件，先暂时把元素放行，但是dropCount中已经记录了需要跳过的元素数量
                            downstream.accept(t);
                            
                            /*
                             * 遇到满足条件的元素时，则丢弃元素；直到遇见首个不满足条件的元素，则此后开始保存元素。
                             * 注：此分支应用于顺序流。
                             */
                        } else {
                            if(takeElement) {
                                downstream.accept(t);
                            }
                        }
                    }
                    
                    @Override
                    public long getDropCount() {
                        return dropCount;
                    }
                }
                
                return new OpSink();
            }
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> downSink) {
                return opWrapSink(downSink, false);
            }
            
            /*
             * 并行处理helper流阶段输出的元素，然后将处理结果封装到流迭代器中返回。
             *
             * 通常，如果helper流阶段的数据已经满足当前阶段的预期，则会直接包装helper流阶段的数据到流迭代器中。
             * 否则，会视情形创建终端sink来处理helper流阶段的数据，并同样将其封装到流迭代器中返回。
             * 还可能会对spliterator做进一步的包装，返回一个与当前操作匹配的流迭代器。
             *
             * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
             * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
             *
             * 不同的中间操作会重写该方法，以在该方法中使用不同的方式处理数据。
             * 返回值代表了当前阶段的流处理器。
             *
             * 该方法仅在有状态的中间(操作)阶段上调用，如果某阶段的opIsStateful()返回true，则实现必须重写该方法。
             */
            @Override
            <P_IN> Spliterator<Long> opEvaluateParallelLazy(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有固定的遭遇顺序
                if(StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    // 返回用于创建long类型数组的函数表达式
                    IntFunction<Long[]> generator = Long[]::new;
                    
                    // 并行处理helper流阶段输出的元素，返回处理后的结果
                    Node<Long> node = opEvaluateParallel(helper, spliterator, generator);
                    
                    // 返回node的流迭代器
                    return node.spliterator();
                }
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<Long> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                // 返回"丢弃前缀"流迭代器，这是"无序前缀"流迭代器的一种
                return new UnorderedWhileSpliterator.OfLong.Dropping((Spliterator.OfLong) wrapSpliterator, false, predicate);
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
            <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator, IntFunction<Long[]> generator) {
                
                // 构造用于"丢弃前缀"的任务
                DropWhileTask<P_IN, Long> task = new DropWhileTask<>(this, helper, spliterator, generator);
                
                // 执行任务
                return task.invoke();
            }
        }
        
        return new Op(upstream, StreamShape.LONG_VALUE, DROP_FLAGS);
    }
    
    /**
     * Appends a "dropWhile" operation to the provided DoubleStream.
     *
     * @param upstream  a reference stream with element type T
     * @param predicate the predicate that returns false to halt dropping.
     */
    // 返回"丢弃前缀"的流(double类型版本)
    static DoubleStream makeDropWhileDouble(AbstractPipeline<?, Double, ?> upstream, DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        
        // 中间阶段的流
        class Op extends DoublePipeline.StatefulOp<Double> implements DropWhileOp<Double> {
            public Op(AbstractPipeline<?, Double, ?> upstream, StreamShape inputShape, int opFlags) {
                super(upstream, inputShape, opFlags);
            }
            
            public DropWhileSink<Double> opWrapSink(Sink<Double> downSink, boolean retainAndCountDroppedElements) {
                
                class OpSink extends Sink.ChainedDouble<Double> implements DropWhileSink<Double> {
                    long dropCount; // 记录满足条件的元素数量
                    boolean take;
                    
                    OpSink() {
                        super(downSink);
                    }
                    
                    // 丢弃前缀
                    @Override
                    public void accept(double t) {
                        boolean takeElement = take || (take = !predicate.test(t));
                        
                        /*
                         * 遇到满足条件的元素时，要对其进行计数；直到遇见首个不满足条件的元素，则此后不再统计元素数量。
                         * 注：此分支仅用于元素有固定遭遇顺序的并行流。
                         */
                        if(retainAndCountDroppedElements) {
                            if(!takeElement) {
                                dropCount++;
                            }
                            
                            // 不管满不满足条件，先暂时把元素放行，但是dropCount中已经记录了需要跳过的元素数量
                            downstream.accept(t);
                            
                            /*
                             * 遇到满足条件的元素时，则丢弃元素；直到遇见首个不满足条件的元素，则此后开始保存元素。
                             * 注：此分支应用于顺序流。
                             */
                        } else {
                            if(takeElement) {
                                downstream.accept(t);
                            }
                        }
                    }
                    
                    @Override
                    public long getDropCount() {
                        return dropCount;
                    }
                }
                
                return new OpSink();
            }
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> downSink) {
                return opWrapSink(downSink, false);
            }
            
            /*
             * 并行处理helper流阶段输出的元素，然后将处理结果封装到流迭代器中返回。
             *
             * 通常，如果helper流阶段的数据已经满足当前阶段的预期，则会直接包装helper流阶段的数据到流迭代器中。
             * 否则，会视情形创建终端sink来处理helper流阶段的数据，并同样将其封装到流迭代器中返回。
             * 还可能会对spliterator做进一步的包装，返回一个与当前操作匹配的流迭代器。
             *
             * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
             * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
             *
             * 不同的中间操作会重写该方法，以在该方法中使用不同的方式处理数据。
             * 返回值代表了当前阶段的流处理器。
             *
             * 该方法仅在有状态的中间(操作)阶段上调用，如果某阶段的opIsStateful()返回true，则实现必须重写该方法。
             */
            @Override
            <P_IN> Spliterator<Double> opEvaluateParallelLazy(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有固定的遭遇顺序
                if(StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    // 返回用于创建double类型数组的函数表达式
                    IntFunction<Double[]> generator = Double[]::new;
                    
                    // 并行处理helper流阶段输出的元素，返回处理后的结果
                    Node<Double> node = opEvaluateParallel(helper, spliterator, generator);
                    
                    // 返回node的流迭代器
                    return node.spliterator();
                }
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<Double> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                // 返回"丢弃前缀"流迭代器，这是"无序前缀"流迭代器的一种
                return new UnorderedWhileSpliterator.OfDouble.Dropping((Spliterator.OfDouble) wrapSpliterator, false, predicate);
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
            <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator, IntFunction<Double[]> generator) {
                
                // 构造用于"丢弃前缀"的任务
                DropWhileTask<P_IN, Double> task = new DropWhileTask<>(this, helper, spliterator, generator);
                
                // 执行任务
                return task.invoke();
            }
        }
        
        return new Op(upstream, StreamShape.DOUBLE_VALUE, DROP_FLAGS);
    }
    
    
    /**
     * A specialization for the dropWhile operation that controls if
     * elements to be dropped are counted and passed downstream.
     * <p>
     * This specialization is utilized by the {@link TakeWhileTask} for
     * pipelines that are ordered.  In such cases elements cannot be dropped
     * until all elements have been collected.
     *
     * @param <T> the type of both input and output elements
     */
    interface DropWhileOp<T> {
        /**
         * Accepts a {@code Sink} which will receive the results of this
         * dropWhile operation, and return a {@code DropWhileSink} which
         * accepts
         * elements and which performs the dropWhile operation passing the
         * results to the provided {@code Sink}.
         *
         * @param sink                          sink to which elements should be sent after processing
         * @param retainAndCountDroppedElements true if elements to be dropped
         *                                      are counted and passed to the sink, otherwise such elements
         *                                      are actually dropped and not passed to the sink.
         *
         * @return a dropWhile sink
         */
        DropWhileSink<T> opWrapSink(Sink<T> sink, boolean retainAndCountDroppedElements);
    }
    
    /**
     * A specialization for a dropWhile sink.
     *
     * @param <T> the type of both input and output elements
     */
    interface DropWhileSink<T> extends Sink<T> {
        /**
         * @return the could of elements that would have been dropped and
         * instead were passed downstream.
         */
        long getDropCount();
    }
    
    /**
     * A spliterator supporting takeWhile and dropWhile operations over an
     * underlying spliterator whose covered elements have no encounter order.
     * <p>
     * Concrete subclasses of this spliterator support reference and primitive
     * types for takeWhile and dropWhile.
     * <p>
     * For the takeWhile operation if during traversal taking completes then
     * taking is cancelled globally for the splitting and traversal of all
     * related spliterators.
     * Cancellation is governed by a shared {@link AtomicBoolean} instance.  A
     * spliterator in the process of taking when cancellation occurs will also
     * be cancelled but not necessarily immediately.  To reduce contention on
     * the {@link AtomicBoolean} instance, cancellation make be acted on after
     * a small number of additional elements have been traversed.
     * <p>
     * For the dropWhile operation if during traversal dropping completes for
     * some, but not all elements, then it is cancelled globally for the
     * traversal of all related spliterators (splitting is not cancelled).
     * Cancellation is governed in the same manner as for the takeWhile
     * operation.
     *
     * @param <T>        the type of elements returned by this spliterator
     * @param <T_SPLITR> the type of the spliterator
     */
    // "无序前缀"流迭代器的抽象实现，用在元素没有固定遭遇顺序的并行流的前缀运算中。
    abstract static class UnorderedWhileSpliterator<T, T_SPLITR extends Spliterator<T>> implements Spliterator<T> {
        
        /** Power of two constant minus one used for modulus of count */
        static final int CANCEL_CHECK_COUNT = (1 << 6) - 1;
        
        /** The underlying spliterator */
        final T_SPLITR spliterator; // 存储了数据源的流迭代器
        
        /**
         * True if no splitting should be performed, if true then
         * this spliterator may be used for an underlying spliterator whose
         * covered elements have an encounter order
         * See use in stream take/dropWhile default default methods
         */
        final boolean noSplitting;  // 当前流迭代器是否可分割
        
        /**
         * True when operations are cancelled for all related spliterators
         * For taking, spliterators cannot split or traversed
         * For dropping, spliterators cannot be traversed
         */
        final AtomicBoolean cancel; // 是否收到了"取消接收数据"的请求
        
        /** True while taking or dropping should be performed when traversing */
        boolean takeOrDrop = true;  // "保存前缀"还是"丢弃前缀"
        
        /** The count of elements traversed */
        int count;
        
        UnorderedWhileSpliterator(T_SPLITR spliterator, boolean noSplitting) {
            this.spliterator = spliterator;
            this.noSplitting = noSplitting;
            this.cancel = new AtomicBoolean();
        }
        
        UnorderedWhileSpliterator(T_SPLITR spliterator, UnorderedWhileSpliterator<T, T_SPLITR> parent) {
            this.spliterator = spliterator;
            this.noSplitting = parent.noSplitting;
            this.cancel = parent.cancel;
        }
        
        @Override
        public T_SPLITR trySplit() {
            @SuppressWarnings("unchecked")
            T_SPLITR ls = noSplitting ? null : (T_SPLITR) spliterator.trySplit();
            return ls != null ? makeSpliterator(ls) : null;
        }
        
        @Override
        public long estimateSize() {
            return spliterator.estimateSize();
        }
        
        @Override
        public long getExactSizeIfKnown() {
            return -1L;
        }
        
        @Override
        public int characteristics() {
            // Size is not known
            return spliterator.characteristics() & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }
        
        /*
         * 对于具有SORTED特征值的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED特征值的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super T> getComparator() {
            return spliterator.getComparator();
        }
        
        // 构造子Spliterator
        abstract T_SPLITR makeSpliterator(T_SPLITR s);
        
        // 检查是否应当继续接收数据
        boolean checkCancelOnCount() {
            return count != 0 || !cancel.get();
        }
        
        
        // "无序前缀"流迭代器的抽象实现(引用类型版本)
        abstract static class OfRef<T> extends UnorderedWhileSpliterator<T, Spliterator<T>> implements Consumer<T> {
            final Predicate<? super T> predicate;
            T t;
            
            OfRef(Spliterator<T> spliterator, boolean noSplitting, Predicate<? super T> predicate) {
                super(spliterator, noSplitting);
                this.predicate = predicate;
            }
            
            OfRef(Spliterator<T> spliterator, OfRef<T> parent) {
                super(spliterator, parent);
                this.predicate = parent.predicate;
            }
            
            @Override
            public void accept(T t) {
                count = (count + 1) & CANCEL_CHECK_COUNT;
                this.t = t;
            }
            
            
            // "保存前缀"流迭代器，这是"无序前缀"流迭代器的一种
            static final class Taking<T> extends OfRef<T> {
                Taking(Spliterator<T> spliterator, boolean noSplitting, Predicate<? super T> predicate) {
                    super(spliterator, noSplitting, predicate);
                }
                
                Taking(Spliterator<T> spliterator, Taking<T> parent) {
                    super(spliterator, parent);
                }
                
                @Override
                public Spliterator<T> trySplit() {
                    // Do not split if all operations are cancelled
                    return cancel.get() ? null : super.trySplit();
                }
                
                /*
                 * 尝试用action消费当前流迭代器中下一个元素。
                 * 返回值指示是否找到了下一个元素。
                 *
                 * 注1：该操作可能会引起内部游标的变化
                 * 注2：该操作可能会顺着sink链向下游传播
                 */
                @Override
                public boolean tryAdvance(Consumer<? super T> action) {
                    boolean test = true;
                    
                    // 保存前缀
                    if(takeOrDrop                        // If can take
                        && checkCancelOnCount()          // and if not cancelled
                        && spliterator.tryAdvance(this)  // and if advanced one element
                        && (test = predicate.test(t))) { // and test on element passes
                        action.accept(t);
                        return true;
                    } else {
                        // Taking is finished
                        takeOrDrop = false;
                        
                        // Cancel all further traversal and splitting operations only if test of element failed (short-circuited)
                        if(!test) {
                            cancel.set(true);
                        }
                        return false;
                    }
                }
                
                @Override
                Spliterator<T> makeSpliterator(Spliterator<T> s) {
                    return new Taking<>(s, this);
                }
            }
            
            // "丢弃前缀"流迭代器，这是"无序前缀"流迭代器的一种
            static final class Dropping<T> extends OfRef<T> {
                Dropping(Spliterator<T> spliterator, boolean noSplitting, Predicate<? super T> predicate) {
                    super(spliterator, noSplitting, predicate);
                }
                
                Dropping(Spliterator<T> spliterator, Dropping<T> parent) {
                    super(spliterator, parent);
                }
                
                /*
                 * 尝试用action消费当前流迭代器中下一个元素。
                 * 返回值指示是否找到了下一个元素。
                 *
                 * 注1：该操作可能会引起内部游标的变化
                 * 注2：该操作可能会顺着sink链向下游传播
                 */
                @Override
                public boolean tryAdvance(Consumer<? super T> action) {
                    
                    // "丢弃前缀"
                    if(takeOrDrop) {
                        takeOrDrop = false;
                        
                        boolean adv;
                        boolean dropped = false;
                        
                        // 跳过满足条件的元素
                        while((adv = spliterator.tryAdvance(this)) // If advanced one element
                            && checkCancelOnCount()                // and if not cancelled
                            && predicate.test(t)) {                // and test on element passes
                            dropped = true;
                        }
                        
                        // Report advanced element, if any
                        if(adv) {
                            // Cancel all further dropping if one or more elements were previously dropped
                            if(dropped) {
                                cancel.set(true);
                            }
                            action.accept(t);
                        }
                        
                        return adv;
                    } else {
                        return spliterator.tryAdvance(action);
                    }
                }
                
                @Override
                Spliterator<T> makeSpliterator(Spliterator<T> spliterator) {
                    return new Dropping<>(spliterator, this);
                }
            }
        }
        
        // "无序前缀"流迭代器的抽象实现(int类型版本)
        abstract static class OfInt extends UnorderedWhileSpliterator<Integer, Spliterator.OfInt> implements IntConsumer, Spliterator.OfInt {
            final IntPredicate predicate;
            int t;
            
            OfInt(Spliterator.OfInt spliterator, boolean noSplitting, IntPredicate predicate) {
                super(spliterator, noSplitting);
                this.predicate = predicate;
            }
            
            OfInt(Spliterator.OfInt spliterator, UnorderedWhileSpliterator.OfInt parent) {
                super(spliterator, parent);
                this.predicate = parent.predicate;
            }
            
            @Override
            public void accept(int t) {
                count = (count + 1) & CANCEL_CHECK_COUNT;
                this.t = t;
            }
            
            
            // "保存前缀"流迭代器，这是"无序前缀"流迭代器的一种
            static final class Taking extends UnorderedWhileSpliterator.OfInt {
                Taking(Spliterator.OfInt spliterator, boolean noSplitting, IntPredicate predicate) {
                    super(spliterator, noSplitting, predicate);
                }
                
                Taking(Spliterator.OfInt spliterator, UnorderedWhileSpliterator.OfInt parent) {
                    super(spliterator, parent);
                }
                
                @Override
                public Spliterator.OfInt trySplit() {
                    // Do not split if all operations are cancelled
                    return cancel.get() ? null : super.trySplit();
                }
                
                /*
                 * 尝试用action消费当前流迭代器中下一个元素。
                 * 返回值指示是否找到了下一个元素。
                 *
                 * 注1：该操作可能会引起内部游标的变化
                 * 注2：该操作可能会顺着sink链向下游传播
                 */
                @Override
                public boolean tryAdvance(IntConsumer action) {
                    boolean test = true;
                    
                    // 保存前缀
                    if(takeOrDrop                        // If can take
                        && checkCancelOnCount()          // and if not cancelled
                        && spliterator.tryAdvance(this)  // and if advanced one element
                        && (test = predicate.test(t))) { // and test on element passes
                        action.accept(t);
                        return true;
                    } else {
                        // Taking is finished
                        takeOrDrop = false;
                        // Cancel all further traversal and splitting operations only if test of element failed (short-circuited)
                        if(!test) {
                            cancel.set(true);
                        }
                        return false;
                    }
                }
                
                @Override
                Spliterator.OfInt makeSpliterator(Spliterator.OfInt spliterator) {
                    return new Taking(spliterator, this);
                }
            }
            
            // "丢弃前缀"流迭代器，这是"无序前缀"流迭代器的一种
            static final class Dropping extends UnorderedWhileSpliterator.OfInt {
                Dropping(Spliterator.OfInt spliterator, boolean noSplitting, IntPredicate predicate) {
                    super(spliterator, noSplitting, predicate);
                }
                
                Dropping(Spliterator.OfInt spliterator, UnorderedWhileSpliterator.OfInt parent) {
                    super(spliterator, parent);
                }
                
                /*
                 * 尝试用action消费当前流迭代器中下一个元素。
                 * 返回值指示是否找到了下一个元素。
                 *
                 * 注1：该操作可能会引起内部游标的变化
                 * 注2：该操作可能会顺着sink链向下游传播
                 */
                @Override
                public boolean tryAdvance(IntConsumer action) {
                    
                    // "丢弃前缀"
                    if(takeOrDrop) {
                        takeOrDrop = false;
                        
                        boolean adv;
                        boolean dropped = false;
                        
                        // 跳过满足条件的元素
                        while((adv = spliterator.tryAdvance(this)) // If advanced one element
                            && checkCancelOnCount()                // and if not cancelled
                            && predicate.test(t)) {                // and test on element passes
                            dropped = true;
                        }
                        
                        // Report advanced element, if any
                        if(adv) {
                            // Cancel all further dropping if one or more elements were previously dropped
                            if(dropped) {
                                cancel.set(true);
                            }
                            action.accept(t);
                        }
                        return adv;
                    } else {
                        return spliterator.tryAdvance(action);
                    }
                }
                
                @Override
                Spliterator.OfInt makeSpliterator(Spliterator.OfInt spliterator) {
                    return new Dropping(spliterator, this);
                }
            }
        }
        
        // "无序前缀"流迭代器的抽象实现(long类型版本)
        abstract static class OfLong extends UnorderedWhileSpliterator<Long, Spliterator.OfLong> implements LongConsumer, Spliterator.OfLong {
            final LongPredicate predicate;
            long t;
            
            OfLong(Spliterator.OfLong spliterator, boolean noSplitting, LongPredicate predicate) {
                super(spliterator, noSplitting);
                this.predicate = predicate;
            }
            
            OfLong(Spliterator.OfLong spliterator, UnorderedWhileSpliterator.OfLong parent) {
                super(spliterator, parent);
                this.predicate = parent.predicate;
            }
            
            @Override
            public void accept(long t) {
                count = (count + 1) & CANCEL_CHECK_COUNT;
                this.t = t;
            }
            
            
            // "保存前缀"流迭代器，这是"无序前缀"流迭代器的一种
            static final class Taking extends UnorderedWhileSpliterator.OfLong {
                Taking(Spliterator.OfLong spliterator, boolean noSplitting, LongPredicate predicate) {
                    super(spliterator, noSplitting, predicate);
                }
                
                Taking(Spliterator.OfLong spliterator, UnorderedWhileSpliterator.OfLong parent) {
                    super(spliterator, parent);
                }
                
                @Override
                public Spliterator.OfLong trySplit() {
                    // Do not split if all operations are cancelled
                    return cancel.get() ? null : super.trySplit();
                }
                
                /*
                 * 尝试用action消费当前流迭代器中下一个元素。
                 * 返回值指示是否找到了下一个元素。
                 *
                 * 注1：该操作可能会引起内部游标的变化
                 * 注2：该操作可能会顺着sink链向下游传播
                 */
                @Override
                public boolean tryAdvance(LongConsumer action) {
                    boolean test = true;
                    
                    // 保存前缀
                    if(takeOrDrop                        // If can take
                        && checkCancelOnCount()          // and if not cancelled
                        && spliterator.tryAdvance(this)  // and if advanced one element
                        && (test = predicate.test(t))) { // and test on element passes
                        action.accept(t);
                        return true;
                    } else {
                        // Taking is finished
                        takeOrDrop = false;
                        
                        // Cancel all further traversal and splitting operations only if test of element failed (short-circuited)
                        if(!test) {
                            cancel.set(true);
                        }
                        
                        return false;
                    }
                }
                
                @Override
                Spliterator.OfLong makeSpliterator(Spliterator.OfLong spliterator) {
                    return new Taking(spliterator, this);
                }
            }
            
            // "丢弃前缀"流迭代器，这是"无序前缀"流迭代器的一种
            static final class Dropping extends UnorderedWhileSpliterator.OfLong {
                Dropping(Spliterator.OfLong spliterator, boolean noSplitting, LongPredicate predicate) {
                    super(spliterator, noSplitting, predicate);
                }
                
                Dropping(Spliterator.OfLong spliterator, UnorderedWhileSpliterator.OfLong parent) {
                    super(spliterator, parent);
                }
                
                /*
                 * 尝试用action消费当前流迭代器中下一个元素。
                 * 返回值指示是否找到了下一个元素。
                 *
                 * 注1：该操作可能会引起内部游标的变化
                 * 注2：该操作可能会顺着sink链向下游传播
                 */
                @Override
                public boolean tryAdvance(LongConsumer action) {
                    
                    // "丢弃前缀"
                    if(takeOrDrop) {
                        takeOrDrop = false;
                        
                        boolean adv;
                        boolean dropped = false;
                        
                        // 跳过满足条件的元素
                        while((adv = spliterator.tryAdvance(this)) // If advanced one element
                            && checkCancelOnCount()                // and if not cancelled
                            && predicate.test(t)) {                // and test on element passes
                            dropped = true;
                        }
                        
                        // Report advanced element, if any
                        if(adv) {
                            // Cancel all further dropping if one or more elements were previously dropped
                            if(dropped) {
                                cancel.set(true);
                            }
                            action.accept(t);
                        }
                        return adv;
                    } else {
                        return spliterator.tryAdvance(action);
                    }
                }
                
                @Override
                Spliterator.OfLong makeSpliterator(Spliterator.OfLong s) {
                    return new Dropping(s, this);
                }
            }
        }
        
        // "无序前缀"流迭代器的抽象实现(double类型版本)
        abstract static class OfDouble extends UnorderedWhileSpliterator<Double, Spliterator.OfDouble> implements DoubleConsumer, Spliterator.OfDouble {
            final DoublePredicate predicate;
            double t;
            
            OfDouble(Spliterator.OfDouble spliterator, boolean noSplitting, DoublePredicate predicate) {
                super(spliterator, noSplitting);
                this.predicate = predicate;
            }
            
            OfDouble(Spliterator.OfDouble spliterator, UnorderedWhileSpliterator.OfDouble parent) {
                super(spliterator, parent);
                this.predicate = parent.predicate;
            }
            
            @Override
            public void accept(double t) {
                count = (count + 1) & CANCEL_CHECK_COUNT;
                this.t = t;
            }
            
            
            // "保存前缀"流迭代器，这是"无序前缀"流迭代器的一种
            static final class Taking extends UnorderedWhileSpliterator.OfDouble {
                Taking(Spliterator.OfDouble spliterator, boolean noSplitting, DoublePredicate predicate) {
                    super(spliterator, noSplitting, predicate);
                }
                
                Taking(Spliterator.OfDouble spliterator, UnorderedWhileSpliterator.OfDouble parent) {
                    super(spliterator, parent);
                }
                
                @Override
                public Spliterator.OfDouble trySplit() {
                    // Do not split if all operations are cancelled
                    return cancel.get() ? null : super.trySplit();
                }
                
                /*
                 * 尝试用action消费当前流迭代器中下一个元素。
                 * 返回值指示是否找到了下一个元素。
                 *
                 * 注1：该操作可能会引起内部游标的变化
                 * 注2：该操作可能会顺着sink链向下游传播
                 */
                @Override
                public boolean tryAdvance(DoubleConsumer action) {
                    boolean test = true;
                    
                    // 保存前缀
                    if(takeOrDrop                        // If can take
                        && checkCancelOnCount()          // and if not cancelled
                        && spliterator.tryAdvance(this)  // and if advanced one element
                        && (test = predicate.test(t))) { // and test on element passes
                        action.accept(t);
                        return true;
                    } else {
                        // Taking is finished
                        takeOrDrop = false;
                        
                        // Cancel all further traversal and splitting operations only if test of element failed (short-circuited)
                        if(!test) {
                            cancel.set(true);
                        }
                        
                        return false;
                    }
                }
                
                @Override
                Spliterator.OfDouble makeSpliterator(Spliterator.OfDouble spliterator) {
                    return new Taking(spliterator, this);
                }
            }
            
            // "丢弃前缀"流迭代器，这是"无序前缀"流迭代器的一种
            static final class Dropping extends UnorderedWhileSpliterator.OfDouble {
                Dropping(Spliterator.OfDouble spliterator, boolean noSplitting, DoublePredicate predicate) {
                    super(spliterator, noSplitting, predicate);
                }
                
                Dropping(Spliterator.OfDouble spliterator, UnorderedWhileSpliterator.OfDouble parent) {
                    super(spliterator, parent);
                }
                
                /*
                 * 尝试用action消费当前流迭代器中下一个元素。
                 * 返回值指示是否找到了下一个元素。
                 *
                 * 注1：该操作可能会引起内部游标的变化
                 * 注2：该操作可能会顺着sink链向下游传播
                 */
                @Override
                public boolean tryAdvance(DoubleConsumer action) {
                    
                    // "丢弃前缀"
                    if(takeOrDrop) {
                        takeOrDrop = false;
                        
                        boolean adv;
                        boolean dropped = false;
                        
                        // 跳过满足条件的元素
                        while((adv = spliterator.tryAdvance(this)) // If advanced one element
                            && checkCancelOnCount()                // and if not cancelled
                            && predicate.test(t)) {                // and test on element passes
                            dropped = true;
                        }
                        
                        // Report advanced element, if any
                        if(adv) {
                            // Cancel all further dropping if one or more elements were previously dropped
                            if(dropped) {
                                cancel.set(true);
                            }
                            action.accept(t);
                        }
                        return adv;
                    } else {
                        return spliterator.tryAdvance(action);
                    }
                }
                
                @Override
                Spliterator.OfDouble makeSpliterator(Spliterator.OfDouble spliterator) {
                    return new Dropping(spliterator, this);
                }
            }
        }
        
    }
    
    /**
     * {@code ForkJoinTask} implementing takeWhile computation.
     * <p>
     * If the pipeline has encounter order then all tasks to the right of
     * a task where traversal was short-circuited are cancelled.
     * The results of completed (and cancelled) tasks are discarded.
     * The result of merging a short-circuited left task and right task (which
     * may or may not be short-circuited) is that left task.
     * <p>
     * If the pipeline has no encounter order then all tasks to the right of
     * a task where traversal was short-circuited are cancelled.
     * The results of completed (and possibly cancelled) tasks are not
     * discarded, as there is no need to throw away computed results.
     * The result of merging does not change if a left task was
     * short-circuited.
     * No attempt is made, once a leaf task stopped taking, for it to cancel
     * all other tasks, and further more, short-circuit the computation with its
     * result.
     *
     * @param <P_IN>  Input element type to the stream pipeline
     * @param <P_OUT> Output element type from the stream pipeline
     */
    // 用于"保存前缀"的任务，用于元素有固定遭遇顺序的并行流
    @SuppressWarnings("serial")
    private static final class TakeWhileTask<P_IN, P_OUT> extends AbstractShortCircuitTask<P_IN, P_OUT, Node<P_OUT>, TakeWhileTask<P_IN, P_OUT>> {
        private final AbstractPipeline<P_OUT, P_OUT, ?> pipe;
        private final IntFunction<P_OUT[]> generator;
        private final boolean isOrdered;
        
        // True if completed, must be set after the local result
        private volatile boolean completed;
        
        private long thisNodeSize;
        
        // True if a short-circuited
        private boolean shortCircuited;
        
        TakeWhileTask(AbstractPipeline<P_OUT, P_OUT, ?> pipe, PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, IntFunction<P_OUT[]> generator) {
            super(helper, spliterator);
            this.pipe = pipe;
            this.generator = generator;
            
            // 获取helper流阶段的组合参数
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            this.isOrdered = StreamOpFlag.ORDERED.isKnown(streamAndOpFlags);
        }
        
        TakeWhileTask(TakeWhileTask<P_IN, P_OUT> parent, Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            this.pipe = parent.pipe;
            this.generator = parent.generator;
            this.isOrdered = parent.isOrdered;
        }
        
        // 在当前(子)任务执行完成后，需要执行该回调方法
        @Override
        public final void onCompletion(CountedCompleter<?> caller) {
            
            // 如果不是叶子结点
            if(!isLeaf()) {
                Node<P_OUT> result;
                
                // 判断是否应当取消后续操作：如果子任务已经取消，那么父任务也没必要进行下去，因为父任务就是由子任务组成的
                shortCircuited = leftChild.shortCircuited | rightChild.shortCircuited;
                
                // 如果已经取消，设置一个空的执行结果
                if(isOrdered && canceled) {
                    thisNodeSize = 0;
                    result = getEmptyResult();
                    
                    /*
                     * 如果只是左孩子结点取消，则此处设置左孩子结点的执行结果，而忽略右孩子结点的执行结果
                     *
                     * 注：这里仍需要设置左孩子结点结果的原因是可能在取消任务之前，已经处理了一部分数据
                     */
                } else if(isOrdered && leftChild.shortCircuited) {
                    // If taking finished on the left node then use the left node result
                    thisNodeSize = leftChild.thisNodeSize;
                    result = leftChild.getLocalResult();
                    
                    // 归并左右孩子结点的元素
                } else {
                    thisNodeSize = leftChild.thisNodeSize + rightChild.thisNodeSize;
                    result = merge();
                }
                
                // 设置任务结果
                setLocalResult(result);
            }
            
            // 标记已完成
            completed = true;
            
            super.onCompletion(caller);
        }
        
        // 返回子任务的计算结果
        @Override
        protected final Node<P_OUT> doLeaf() {
            
            // 构造增强"数组"Node或"弹性缓冲区"Node(引用类型版本)
            Node.Builder<P_OUT> nodeBuilderSink = helper.makeNodeBuilder(-1, generator);
            
            // 获取helper流阶段的组合参数；helper是pipe的上个流阶段
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            // 构造并返回属于pipe流阶段的sink，该sink通常与nodeBuilderSink形成一个链条，以决定如何处理pipe的上个流阶段发来的数据
            Sink<P_OUT> downSink = pipe.opWrapSink(streamAndOpFlags, nodeBuilderSink);
            
            /*
             * 从downSink开始，逆向遍历流，构造并返回(相对于helper阶段的)属于上个(depth==1)的流阶段的sink。
             *
             * 返回的sink与downSink组成一个完整的链条，以便处理属于上个(depth==0)的流阶段输出的数据。
             * 经过该sink链条处理过的数据，会被downSink所在的流阶段输出给downSink的下游阶段。
             *
             * downSink: (相对于helper阶段的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             */
            Sink<P_IN> wrappedSink = helper.wrapSink(downSink);
            
            /*
             * 从wrappedSink开始顺着整个sink链条择取来自spliterator中的数据，
             * 该操作通常会依次执行每个sink上的begin()、accept()、end()方法。
             *
             * 值的关注的是，这里每次消费元素之前，都要先询问wrappedSink是否应当停止接收数据。
             * 如果wrappedSink无法做出决定，则它应当继续询问下游的sink。
             *
             * 如果当前流阶段收到了应当停止接收数据的信号，则会立即停止择取工作，并返回true。
             */
            shortCircuited = helper.copyIntoWithCancel(wrappedSink, spliterator);
            
            // 如果收到了取消请求，则取消后续的任务
            if(shortCircuited) {
                // Cancel later nodes if the predicate returned false during traversal
                cancelLaterNodes();
            }
            
            // 将数据元素封装到node中
            Node<P_OUT> node = nodeBuilderSink.build();
            
            // 记录当前node中保存的元素数量
            thisNodeSize = node.count();
            
            return node;
        }
        
        // 构造子任务
        @Override
        protected TakeWhileTask<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator) {
            return new TakeWhileTask<>(this, spliterator);
        }
        
        // 归并子结点的执行结果
        Node<P_OUT> merge() {
            if(leftChild.thisNodeSize == 0) {
                // If the left node size is 0 then use the right node result
                return rightChild.getLocalResult();
            } else if(rightChild.thisNodeSize == 0) {
                // If the right node size is 0 then use the left node result
                return leftChild.getLocalResult();
            } else {
                // Combine the left and right nodes
                return Nodes.conc(pipe.getOutputShape(), leftChild.getLocalResult(), rightChild.getLocalResult());
            }
        }
        
        // 获取一个空的执行结果
        @Override
        protected final Node<P_OUT> getEmptyResult() {
            return Nodes.emptyNode(pipe.getOutputShape());
        }
        
        @Override
        protected void cancel() {
            super.cancel();
            
            if(isOrdered && completed) {
                // If the task is completed then clear the result, if any to aid GC
                setLocalResult(getEmptyResult());
            }
        }
    }
    
    /**
     * {@code ForkJoinTask} implementing dropWhile computation.
     * <p>
     * If the pipeline has encounter order then each leaf task will not
     * drop elements but will obtain a count of the elements that would have
     * been otherwise dropped. That count is used as an index to track
     * elements to be dropped. Merging will update the index so it corresponds
     * to the index that is the end of the global prefix of elements to be
     * dropped. The root is truncated according to that index.
     * <p>
     * If the pipeline has no encounter order then each leaf task will drop
     * elements. Leaf tasks are ordinarily merged. No truncation of the root
     * node is required.
     * No attempt is made, once a leaf task stopped dropping, for it to cancel
     * all other tasks, and further more, short-circuit the computation with
     * its result.
     *
     * @param <P_IN>  Input element type to the stream pipeline
     * @param <P_OUT> Output element type from the stream pipeline
     */
    // 用于"丢弃前缀"的任务，用于元素有固定遭遇顺序的并行流
    @SuppressWarnings("serial")
    private static final class DropWhileTask<P_IN, P_OUT> extends AbstractTask<P_IN, P_OUT, Node<P_OUT>, DropWhileTask<P_IN, P_OUT>> {
        private final AbstractPipeline<P_OUT, P_OUT, ?> pipe;
        private final IntFunction<P_OUT[]> generator;
        private final boolean isOrdered;
        private long thisNodeSize;
        
        /**
         * The index from which elements of the node should be taken
         * i.e. the node should be truncated from [takeIndex, thisNodeSize)
         * Equivalent to the count of dropped elements
         */
        // 记录当前task中满足条件(需要被丢弃)的元素数量
        private long index;
        
        DropWhileTask(AbstractPipeline<P_OUT, P_OUT, ?> pipe, PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, IntFunction<P_OUT[]> generator) {
            super(helper, spliterator);
            
            assert pipe instanceof DropWhileOp;
            
            this.pipe = pipe;
            this.generator = generator;
            
            // 获取helper流阶段的组合参数
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            // 元素是否有固定的遭遇顺序
            this.isOrdered = StreamOpFlag.ORDERED.isKnown(streamAndOpFlags);
        }
        
        DropWhileTask(DropWhileTask<P_IN, P_OUT> parent, Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            
            this.pipe = parent.pipe;
            this.generator = parent.generator;
            this.isOrdered = parent.isOrdered;
        }
        
        // 在当前(子)任务执行完成后，需要执行该回调方法
        @Override
        public final void onCompletion(CountedCompleter<?> caller) {
            
            // 如果不是叶子结点
            if(!isLeaf()) {
                // 如果当前流有固定的遭遇顺序
                if(isOrdered) {
                    index = leftChild.index;
                    
                    /* If a contiguous sequence of dropped elements include those of the right node, if any */
                    // 如果左孩子结点的元素应当全部被丢弃，则这里需要再加上右孩子结点中需要被丢弃的元素数量
                    if(index == leftChild.thisNodeSize) {
                        index += rightChild.index;
                    }
                }
                
                // 记录非叶子结点上保存的元素数量(稍后可能会被丢弃一部分)
                thisNodeSize = leftChild.thisNodeSize + rightChild.thisNodeSize;
                
                // 合并左右子结点
                Node<P_OUT> result = merge();
                
                // 如果已经到了根结点，则需要丢弃那些应当被丢弃的元素(即丢弃index之前那些满足条件的元素)
                if(isRoot()) {
                    result = doTruncate(result);
                }
                
                // 设置执行结果
                setLocalResult(result);
            }
            
            super.onCompletion(caller);
        }
        
        // 返回子任务的计算结果
        @Override
        protected final Node<P_OUT> doLeaf() {
            // 是否为孩子结点
            boolean isChild = !isRoot();
            
            // If this not the root and pipeline is ordered and size is known then pre-size the builder
            long sizeIfKnown = isChild && isOrdered && StreamOpFlag.SIZED.isPreserved(pipe.sourceOrOpFlags) ? pipe.exactOutputSizeIfKnown(spliterator) : -1;
            
            // 构造增强"数组"Node或"弹性缓冲区"Node(引用类型版本)
            Node.Builder<P_OUT> nodeBuilderSink = helper.makeNodeBuilder(sizeIfKnown, generator);
            
            @SuppressWarnings("unchecked")
            DropWhileOp<P_OUT> dropOp = (DropWhileOp<P_OUT>) pipe;
            
            // If this leaf is the root then there is no merging on completion and there is no need to retain dropped elements
            DropWhileSink<P_OUT> downSink = dropOp.opWrapSink(nodeBuilderSink, isOrdered && isChild);
            
            /*
             * 从downSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
             * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
             * 最终择取出的数据往往被存入了downSink代表的容器当中。
             *
             * downSink   : (相对于helper的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             * spliterator: 流迭代器，作为数据源，包含了当前所有待访问的元素
             */
            helper.wrapAndCopyInto(downSink, spliterator);
            
            // 将数据元素封装到node中
            Node<P_OUT> node = nodeBuilderSink.build();
            
            // 记录当前node中保存的元素数量
            thisNodeSize = node.count();
            
            // 记录当前任务中丢弃的元素数量
            index = downSink.getDropCount();
            
            return node;
        }
        
        // 构造子任务
        @Override
        protected DropWhileTask<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator) {
            return new DropWhileTask<>(this, spliterator);
        }
        
        // 归并子结点的执行结果
        private Node<P_OUT> merge() {
            if(leftChild.thisNodeSize == 0) {
                // If the left node size is 0 then use the right node result
                return rightChild.getLocalResult();
            } else if(rightChild.thisNodeSize == 0) {
                // If the right node size is 0 then use the left node result
                return leftChild.getLocalResult();
            } else {
                // Combine the left and right nodes
                return Nodes.conc(pipe.getOutputShape(), leftChild.getLocalResult(), rightChild.getLocalResult());
            }
        }
        
        // 丢弃前缀
        private Node<P_OUT> doTruncate(Node<P_OUT> input) {
            if(isOrdered) {
                // 将index中[index, input.count())范围内的元素打包到新建的子Node中返回
                return input.truncate(index, input.count(), generator);
            }
            
            return input;
        }
    }
    
}
