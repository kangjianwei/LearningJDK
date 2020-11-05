/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Factory methods for transforming streams into duplicate-free streams, using
 * {@link Object#equals(Object)} to determine equality.
 *
 * @since 1.8
 */
// 应用在有状态的中间阶段的辅助类，服务于distinct()方法
final class DistinctOps {
    
    private DistinctOps() {
    }
    
    
    /**
     * Appends a "distinct" operation to the provided stream, and returns the
     * new stream.
     *
     * @param <T>      the type of both input and output elements
     * @param upstream a reference stream with element type T
     *
     * @return the new stream
     */
    // 构造可以执行"去重"任务的流的中间阶段
    static <T> ReferencePipeline<T, T> makeRef(AbstractPipeline<?, T, ?> upstream) {
    
        // 返回一个有状态的流的中间阶段；该阶段的操作会对upstream流阶段发来的数据进行去重
        return new ReferencePipeline.StatefulOp<T, T>(upstream, StreamShape.REFERENCE, StreamOpFlag.IS_DISTINCT | StreamOpFlag.NOT_SIZED) {
        
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<T> opWrapSink(int flags, Sink<T> downSink) {
                Objects.requireNonNull(downSink);
            
                // 1.如果上个流阶段的元素是无重复的，则其符合此阶段的预期，可以直接使用下游的downSink
                if(StreamOpFlag.DISTINCT.isKnown(flags)) {
                    return downSink;
                }
            
                // 2.如果上个流阶段的元素是排好序的，则可以用简单的办法去重
                if(StreamOpFlag.SORTED.isKnown(flags)) {
                
                    // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                    return new Sink.ChainedReference<T, T>(downSink) {
                        boolean seenNull;
                        T lastSeen;
                    
                        @Override
                        public void begin(long size) {
                            seenNull = false;
                            lastSeen = null;
                            downstream.begin(-1);
                        }
                    
                        @Override
                        public void accept(T t) {
                            if(t == null) {
                                if(!seenNull) {
                                    seenNull = true;
                                    downstream.accept(lastSeen = null);
                                }
                                // 只处理未访问过的值
                            } else if(!t.equals(lastSeen)) {
                                downstream.accept(lastSeen = t);
                            }
                        }
                    
                        @Override
                        public void end() {
                            seenNull = false;
                            lastSeen = null;
                            downstream.end();
                        }
                    
                    };
                }
            
                // 3.返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<T, T>(downSink) {
                
                    // 存储不重复的元素
                    Set<T> seen;
                
                    @Override
                    public void begin(long size) {
                        seen = new HashSet<>();
                        downstream.begin(-1);
                    }
                
                    // 在此处对元素进行去重
                    @Override
                    public void accept(T t) {
                        if(!seen.contains(t)) {
                            seen.add(t);
                            downstream.accept(t);
                        }
                    }
                
                    @Override
                    public void end() {
                        seen = null;
                        downstream.end();
                    }
                
                };
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
            
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
            
                /*
                 * 1.如果helper流阶段的元素有固定的遭遇顺序，则需要对其去重
                 *   这里去重完成后，依然保留了元素之前的遭遇顺序。
                 */
                if(StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    // 将helper阶段输出的元素去重后存储到node中返回；spliterator中存储了数据源
                    return reduce(helper, spliterator);
                }
            
                // 2.如果helper流阶段的元素是无重复的，则其符合此阶段的预期，可以直接收集
                if(StreamOpFlag.DISTINCT.isKnown(streamAndOpFlags)) {
                    /*
                     * 获取helper流阶段的输出元素
                     *
                     * 将spliterator中的元素搜集到generator生成的数组中，然后将该数组封装到Node中返回。
                     * 这个搜集过程会涉及到数据的择取过程，即数据流通常从(相对于helper流阶段的)上一个(depth==1)的流阶段的sink开始，经过整个sink链的筛选后，进入终端阶段的sink。
                     *
                     * 注：这里的flatten参数为false，即在并行操作中，如果中间生成的Node是树状Node，依然可以将其直接返回。
                     */
                    Node<T> node = helper.evaluate(spliterator, false, generator);
                
                    // 封装了无重复的元素后返回
                    return node;
                }
            
                /*
                 * 3.构造一个异步任务，以完成去重任务。
                 *   这里不保证去重后元素的遭遇顺序。
                 */
            
                /* Holder of null state since ConcurrentHashMap does not support null values. */
                AtomicBoolean seenNull = new AtomicBoolean(false);
            
                // 接收异步操作的处理结果
                ConcurrentHashMap<T, Boolean> map = new ConcurrentHashMap<>();
            
                // 构造一个终端操作，用来遍历目标元素，并将其存入map中
                TerminalOp<T, Void> forEachOp = ForEachOps.makeRef(t -> {
                    if(t == null) {
                        seenNull.set(true);
                    } else {
                        map.putIfAbsent(t, Boolean.TRUE);
                    }
                }, false);
            
                /*
                 * 使用forEachOp并行处理helper流阶段输出的元素，返回处理后的结果
                 *
                 * 为helper构造一个终端sink，并使用该终端sink对spliterator中的数据进行择取，返回最后的处理结果。
                 * 返回值可能是收集到的元素，也可能只是对过滤后的元素的计数，还可能是其它定制化的结果。
                 *
                 * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
                 * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
                 *
                 * 注：这里忽略了返回结果，因为返回结果都被存储到map中了
                 */
                forEachOp.evaluateParallel(helper, spliterator);
            
                /* If null has been seen then copy the key set into a HashSet that supports null values and add null. */
                // 对可能存在的null元素进行单独处理
                Set<T> keys = map.keySet();
                if(seenNull.get()) {
                    // TODO Implement a more efficient set-union view, rather than copying
                    keys = new HashSet<>(keys);
                    keys.add(null);
                }
            
                // 返回处理结果
                return Nodes.node(keys);
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
            
                /*
                 * 1.如果helper流阶段的元素有固定的遭遇顺序，则需要对其去重
                 *   这里去重完成后，依然保留了元素之前的遭遇顺序。
                 */
                if(StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                
                    // 将helper阶段输出的元素去重后存储到node中返回；spliterator中存储了数据源
                    Node<T> node = reduce(helper, spliterator);
                
                    // 将去重后的数据封装到流迭代器中返回
                    return node.spliterator();
                }
            
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<T> wrapSpliterator = helper.wrapSpliterator(spliterator);
            
                // 2.如果helper流阶段的元素是无重复的，则其符合此阶段的预期，可以直接返回wrapSpliterator
                if(StreamOpFlag.DISTINCT.isKnown(streamAndOpFlags)) {
                    // 封装了无重复的元素后返回
                    return wrapSpliterator;
                }
            
                /*
                 * 3.构造并返回一个"去重"流迭代器，使用该流迭代器也可以间接对wrapSpliterator中的元素进行去重。
                 *   这里不保证去重后元素的遭遇顺序。
                 */
                return new StreamSpliterators.DistinctSpliterator<>(wrapSpliterator);
            }
        
            // 将helper阶段输出的元素去重后存储到node中返回；spliterator中存储了数据源
            <P_IN> Node<T> reduce(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
            
                // LinkedHashSet工厂
                Supplier<LinkedHashSet<T>> seedFactory = () -> new LinkedHashSet<T>();
            
                // 函数表达式：将e添加到set中
                BiConsumer<LinkedHashSet<T>, ? super T> accumulator = (set, e) -> set.add(e);
            
                // 函数表达式：将collection中的元素添加到set中
                BiConsumer<LinkedHashSet<T>, LinkedHashSet<T>> reducer = (set, collection) -> set.addAll(collection);
            
                /* If the stream is SORTED then it should also be ORDERED so the following will also preserve the sort order */
                // 为helper构造的终端sink
                TerminalOp<T, LinkedHashSet<T>> terminalOp = ReduceOps.makeRef(seedFactory, accumulator, reducer);
            
                /*
                 * 使用terminalOp并行处理helper流阶段输出的元素，返回处理后的结果
                 *
                 * 为helper构造一个终端sink，并使用该终端sink对spliterator中的数据进行择取，返回最后的处理结果。
                 * 返回值可能是收集到的元素，也可能只是对过滤后的元素的计数，还可能是其它定制化的结果。
                 *
                 * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
                 * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
                 *
                 * 此处的处理结果就是：将helper阶段输出的元素存储到一个LinkedHashSet中。
                 */
                LinkedHashSet<T> collection = terminalOp.evaluateParallel(helper, spliterator);
            
                // 返回一个Collection-Node，其中包含了collection中的元素
                return Nodes.node(collection);
            }
        
        };
    }
}
