/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.function.IntFunction;

/**
 * Factory for instances of a short-circuiting stateful intermediate operations that produce subsequences of their input stream.
 *
 * @since 1.8
 */
// 应用在有状态的中间阶段的辅助类，服务于limit(long)方法和skip(long)方法
final class SliceOps {
    
    private SliceOps() {
    }
    
    
    /**
     * Appends a "slice" operation to the provided stream.  The slice operation
     * may be may be skip-only, limit-only, or skip-and-limit.
     *
     * @param <T>      the type of both input and output elements
     * @param upstream a reference stream with element type T
     * @param skip     the number of elements to skip.  Must be >= 0.
     * @param limit    the maximum size of the resulting stream, or -1 if no limit is to be imposed
     */
    /*
     * 构造有状态的中间阶段的流，适用于limit(long)方法和skip(long)方法(引用类型版本)
     *
     * upstream: 上个阶段的流
     * skip    : 需要跳过的元素数量，必须>=0
     * limit   : 返回的流中可以包含的最大元素数量，-1表示没限制
     */
    public static <T> Stream<T> makeRef(AbstractPipeline<?, T, ?> upstream, long skip, long limit) {
        if(skip<0) {
            throw new IllegalArgumentException("Skip must be non-negative: " + skip);
        }
        
        // 返回一个有状态的流的中间阶段；该阶段的操作会对upstream流阶段发来的数据进行跳过或选择保存一段数据
        return new ReferencePipeline.StatefulOp<T, T>(upstream, StreamShape.REFERENCE, flags(limit)) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<T> opWrapSink(int flags, Sink<T> downSink) {
                
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedReference<T, T>(downSink) {
                    
                    // 需要跳过的元素数量
                    long n = skip;
                    // 发送给下游的元素数量上限
                    long m = limit >= 0 ? limit : Long.MAX_VALUE;
                    
                    @Override
                    public void begin(long size) {
                        // 计算需要发给下游的元素数量
                        size = calcSize(size, skip, m);
                        downstream.begin(size);
                    }
                    
                    @Override
                    public void accept(T t) {
                        // 如果该跳过的元素都跳过了
                        if(n == 0) {
                            // 如果待发送元素量大于0，则向下游发送一个元素
                            if(m>0) {
                                m--;
                                downstream.accept(t);
                            }
                            
                            // 如果还有未跳过的元素，则计数递减
                        } else {
                            n--;
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        // 如果已经没有待发送元素了，或者下游反馈了取消择取的信号，则此处也要向上游反馈"取消"信号
                        return m == 0 || downstream.cancellationRequested();
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
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<T> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                /*
                 * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
                 * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
                 *
                 * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
                 */
                long size = helper.exactOutputSizeIfKnown(spliterator);
                
                // 1.如果流中元素的数量以及切割后的子流中的元素数量均是有限的，则可以直接切片
                if(size>0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    // 计算切片的终止索引
                    long sliceFence = calcSliceFence(skip, limit);
                    
                    // 返回一个"分片"流迭代器
                    return new StreamSpliterators.SliceSpliterator.OfRef<>(wrapSpliterator, skip, sliceFence);
                }
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 2.如果helper流阶段的元素没有固定的遭遇顺序，则使用"无序"流迭代器
                if(!StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    // 返回一个"无序"流迭代器
                    return unorderedSkipLimitSpliterator(wrapSpliterator, skip, limit, size);
                }
                
                /*
                 * OOMEs will occur for LongStream.range(0, Long.MAX_VALUE).filter(i -> true).limit(n)
                 * when n * parallelismLevel is sufficiently large.
                 *
                 * Need to adjust the target size of splitting for the
                 * SliceTask from say (size / k) to say min(size / k, 1 << 14)
                 * This will limit the size of the buffers created at the leaf nodes
                 * cancellation will be more aggressive cancelling later tasks
                 * if the target slice size has been reached from a given task,
                 * cancellation should also clear local results if any
                 */
                
                /*
                 * 3.至此说明元素数量无限或未知，但是有固定的遭遇顺序。
                 *   此时需要使用特定的task来完成"分片"操作。
                 */
                
                // 返回用于创建T类型数组的函数表达式
                IntFunction<T[]> generator = Nodes.castingArray();
                
                // 构造"分片"任务，用来跳过某段元素，并保留指定数量的元素
                SliceTask<P_IN, T> sliceTask = new SliceTask<>(this, helper, spliterator, generator, skip, limit);
                
                // 执行任务，返回执行结果
                Node<T> node = sliceTask.invoke();
                
                // 返回可以代表node的流迭代器
                return node.spliterator();
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
                
                /*
                 * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
                 * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
                 *
                 * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
                 */
                long size = helper.exactOutputSizeIfKnown(spliterator);
                
                // 1.如果流中元素的数量以及切割后的子流中的元素数量均是有限的，则可以直接切片
                if(size>0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    /*
                     * Because the pipeline is SIZED the slice spliterator
                     * can be created from the source, this requires matching
                     * to shape of the source, and is potentially more efficient
                     * than creating the slice spliterator from the pipeline wrapping spliterator
                     */
                    
                    // 返回一个"分片"流迭代器
                    Spliterator<P_IN> sliceSpliterator = sliceSpliterator(helper.getSourceShape(), spliterator, skip, limit);
                    
                    /*
                     * 并行搜集元素，中间依然会经过sink的择取操作(引用类型版本)。
                     * 将sliceSpliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
                     */
                    return Nodes.collect(helper, sliceSpliterator, true, generator);
                }
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 2.如果helper流阶段的元素没有固定的遭遇顺序，则使用"无序"流迭代器
                if(!StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    /*
                     * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                     *
                     * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                     */
                    Spliterator<T> wrapSpliterator = helper.wrapSpliterator(spliterator);
                    
                    // 返回一个"无序"流迭代器
                    Spliterator<T> unorderedSpliterator = unorderedSkipLimitSpliterator(wrapSpliterator, skip, limit, size);
                    
                    /*
                     * Collect using this pipeline, which is empty and therefore
                     * can be used with the pipeline wrapping spliterator
                     * Note that we cannot create a slice spliterator from
                     * the source spliterator if the pipeline is not SIZED
                     */
                    /*
                     * 并行搜集元素，中间依然会经过sink的择取操作(引用类型版本)。
                     * 将sliceSpliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
                     */
                    return Nodes.collect(this, unorderedSpliterator, true, generator);
                }
                
                /*
                 * 3.至此说明元素数量无限或未知，但是有固定的遭遇顺序。
                 *   此时需要使用特定的task来完成"分片"操作。
                 */
                
                // 构造"分片"任务，用来跳过某段元素，并保留指定数量的元素
                SliceTask<P_IN, T> sliceTask = new SliceTask<>(this, helper, spliterator, generator, skip, limit);
                
                // 执行任务，返回执行结果
                return sliceTask.invoke();
            }
            
            // 返回一个"无序"流迭代器
            Spliterator<T> unorderedSkipLimitSpliterator(Spliterator<T> spliterator, long skip, long limit, long sizeIfKnown) {
                // 如果跳过的元素小于估计的元素数量，则需要调整skip和limit
                if(skip<=sizeIfKnown) {
                    // Use just the limit if the number of elements to skip is <= the known pipeline size
                    limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                    skip = 0;
                }
                
                return new StreamSpliterators.UnorderedSliceSpliterator.OfRef<>(spliterator, skip, limit);
            }
            
        };
    }
    
    /**
     * Appends a "slice" operation to the provided IntStream.  The slice
     * operation may be may be skip-only, limit-only, or skip-and-limit.
     *
     * @param upstream An IntStream
     * @param skip     The number of elements to skip.  Must be >= 0.
     * @param limit    The maximum size of the resulting stream, or -1 if no limit
     *                 is to be imposed
     */
    /*
     * 构造有状态的中间阶段的流，适用于limit(long)方法和skip(long)方法(int类型版本)
     *
     * upstream: 上个阶段的流
     * skip    : 需要跳过的元素数量，必须>=0
     * limit   : 返回的流中可以包含的最大元素数量，-1表示没限制
     */
    public static IntStream makeInt(AbstractPipeline<?, Integer, ?> upstream, long skip, long limit) {
        if(skip<0) {
            throw new IllegalArgumentException("Skip must be non-negative: " + skip);
        }
        
        // 返回一个有状态的流的中间阶段；该阶段的操作会对upstream流阶段发来的数据进行跳过或选择保存一段数据
        return new IntPipeline.StatefulOp<Integer>(upstream, StreamShape.INT_VALUE, flags(limit)) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Integer> opWrapSink(int flags, Sink<Integer> downSink) {
                
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedInt<Integer>(downSink) {
                    
                    // 需要跳过的元素数量
                    long n = skip;
                    // 发送给下游的元素数量上限
                    long m = limit >= 0 ? limit : Long.MAX_VALUE;
                    
                    @Override
                    public void begin(long size) {
                        // 计算需要发给下游的元素数量
                        size = calcSize(size, skip, m);
                        downstream.begin(size);
                    }
                    
                    @Override
                    public void accept(int t) {
                        // 如果该跳过的元素都跳过了
                        if(n == 0) {
                            // 如果待发送元素量大于0，则向下游发送一个元素
                            if(m>0) {
                                m--;
                                downstream.accept(t);
                            }
                            
                            // 如果还有未跳过的元素，则计数递减
                        } else {
                            n--;
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        // 如果已经没有待发送元素了，或者下游反馈了取消择取的信号，则此处也要向上游反馈"取消"信号
                        return m == 0 || downstream.cancellationRequested();
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
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<Integer> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                /*
                 * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
                 * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
                 *
                 * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
                 */
                long size = helper.exactOutputSizeIfKnown(spliterator);
                
                // 1.如果流中元素的数量以及切割后的子流中的元素数量均是有限的，则可以直接切片
                if(size>0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    // 计算切片的终止索引
                    long sliceFence = calcSliceFence(skip, limit);
                    
                    // 返回一个"分片"流迭代器
                    return new StreamSpliterators.SliceSpliterator.OfInt((Spliterator.OfInt) wrapSpliterator, skip, sliceFence);
                }
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 2.如果helper流阶段的元素没有固定的遭遇顺序，则使用"无序"流迭代器
                if(!StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    // 返回一个"无序"流迭代器
                    return unorderedSkipLimitSpliterator((Spliterator.OfInt) wrapSpliterator, skip, limit, size);
                }
                
                /*
                 * 3.至此说明元素数量无限或未知，但是有固定的遭遇顺序。
                 *   此时需要使用特定的task来完成"分片"操作。
                 */
                
                // 构造"分片"任务，用来跳过某段元素，并保留指定数量的元素
                SliceTask<P_IN, Integer> sliceTask = new SliceTask<>(this, helper, spliterator, Integer[]::new, skip, limit);
                
                // 执行任务，返回执行结果
                Node<Integer> node = sliceTask.invoke();
                
                // 返回可以代表node的流迭代器
                return node.spliterator();
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
                
                /*
                 * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
                 * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
                 *
                 * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
                 */
                long size = helper.exactOutputSizeIfKnown(spliterator);
                
                // 1.如果流中元素的数量以及切割后的子流中的元素数量均是有限的，则可以直接切片
                if(size>0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    /*
                     * Because the pipeline is SIZED the slice spliterator
                     * can be created from the source, this requires matching
                     * to shape of the source, and is potentially more efficient
                     * than creating the slice spliterator from the pipeline wrapping spliterator
                     */
                    
                    // 返回一个"分片"流迭代器
                    Spliterator<P_IN> sliceSpliterator = sliceSpliterator(helper.getSourceShape(), spliterator, skip, limit);
                    
                    /*
                     * 并行搜集元素，中间依然会经过sink的择取操作(int类型版本)。
                     * 将sliceSpliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
                     */
                    return Nodes.collectInt(helper, sliceSpliterator, true);
                }
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 2.如果helper流阶段的元素没有固定的遭遇顺序，则使用"无序"流迭代器
                if(!StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    /*
                     * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                     *
                     * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                     */
                    Spliterator<Integer> wrapSpliterator = helper.wrapSpliterator(spliterator);
                    
                    // 返回一个"无序"流迭代器
                    Spliterator.OfInt unorderedSpliterator = unorderedSkipLimitSpliterator((Spliterator.OfInt) wrapSpliterator, skip, limit, size);
                    
                    /*
                     * Collect using this pipeline, which is empty and therefore
                     * can be used with the pipeline wrapping spliterator
                     * Note that we cannot create a slice spliterator from
                     * the source spliterator if the pipeline is not SIZED
                     */
                    /*
                     * 并行搜集元素，中间依然会经过sink的择取操作(int类型版本)。
                     * 将sliceSpliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
                     */
                    return Nodes.collectInt(this, unorderedSpliterator, true);
                }
                
                /*
                 * 3.至此说明元素数量无限或未知，但是有固定的遭遇顺序。
                 *   此时需要使用特定的task来完成"分片"操作。
                 */
                
                // 构造"分片"任务，用来跳过某段元素，并保留指定数量的元素
                SliceTask<P_IN, Integer> sliceTask = new SliceTask<>(this, helper, spliterator, generator, skip, limit);
                
                // 执行任务，返回执行结果
                return sliceTask.invoke();
            }
            
            // 返回一个"无序"流迭代器
            Spliterator.OfInt unorderedSkipLimitSpliterator(Spliterator.OfInt s, long skip, long limit, long sizeIfKnown) {
                // 如果跳过的元素小于估计的元素数量，则需要调整skip和limit
                if(skip<=sizeIfKnown) {
                    // Use just the limit if the number of elements to skip is <= the known pipeline size
                    limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                    skip = 0;
                }
                
                return new StreamSpliterators.UnorderedSliceSpliterator.OfInt(s, skip, limit);
            }
            
        };
    }
    
    /**
     * Appends a "slice" operation to the provided LongStream.  The slice
     * operation may be may be skip-only, limit-only, or skip-and-limit.
     *
     * @param upstream A LongStream
     * @param skip     The number of elements to skip.  Must be >= 0.
     * @param limit    The maximum size of the resulting stream, or -1 if no limit
     *                 is to be imposed
     */
    /*
     * 构造有状态的中间阶段的流，适用于limit(long)方法和skip(long)方法(long类型版本)
     *
     * upstream: 上个阶段的流
     * skip    : 需要跳过的元素数量，必须>=0
     * limit   : 返回的流中可以包含的最大元素数量，-1表示没限制
     */
    public static LongStream makeLong(AbstractPipeline<?, Long, ?> upstream, long skip, long limit) {
        if(skip<0) {
            throw new IllegalArgumentException("Skip must be non-negative: " + skip);
        }
        
        return new LongPipeline.StatefulOp<Long>(upstream, StreamShape.LONG_VALUE, flags(limit)) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Long> opWrapSink(int flags, Sink<Long> downSink) {
                
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedLong<Long>(downSink) {
                    
                    // 需要跳过的元素数量
                    long n = skip;
                    // 发送给下游的元素数量上限
                    long m = limit >= 0 ? limit : Long.MAX_VALUE;
                    
                    @Override
                    public void begin(long size) {
                        // 计算需要发给下游的元素数量
                        size = calcSize(size, skip, m);
                        downstream.begin(size);
                    }
                    
                    @Override
                    public void accept(long t) {
                        // 如果该跳过的元素都跳过了
                        if(n == 0) {
                            // 如果待发送元素量大于0，则向下游发送一个元素
                            if(m>0) {
                                m--;
                                downstream.accept(t);
                            }
                            
                            // 如果还有未跳过的元素，则计数递减
                        } else {
                            n--;
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        // 如果已经没有待发送元素了，或者下游反馈了取消择取的信号，则此处也要向上游反馈"取消"信号
                        return m == 0 || downstream.cancellationRequested();
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
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<Long> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                /*
                 * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
                 * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
                 *
                 * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
                 */
                long size = helper.exactOutputSizeIfKnown(spliterator);
                
                // 1.如果流中元素的数量以及切割后的子流中的元素数量均是有限的，则可以直接切片
                if(size>0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    // 计算切片的终止索引
                    long sliceFence = calcSliceFence(skip, limit);
                    
                    // 返回一个"分片"流迭代器
                    return new StreamSpliterators.SliceSpliterator.OfLong((Spliterator.OfLong) wrapSpliterator, skip, sliceFence);
                }
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 2.如果helper流阶段的元素没有固定的遭遇顺序，则使用"无序"流迭代器
                if(!StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    return unorderedSkipLimitSpliterator((Spliterator.OfLong) wrapSpliterator, skip, limit, size);
                }
                
                /*
                 * 3.至此说明元素数量无限或未知，但是有固定的遭遇顺序。
                 *   此时需要使用特定的task来完成"分片"操作。
                 */
                
                // 返回用于创建T类型数组的函数表达式
                SliceTask<P_IN, Long> sliceTask = new SliceTask<>(this, helper, spliterator, Long[]::new, skip, limit);
                
                // 执行任务，返回执行结果
                Node<Long> node = sliceTask.invoke();
                
                // 返回可以代表node的流迭代器
                return node.spliterator();
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
                
                /*
                 * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
                 * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
                 *
                 * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
                 */
                long size = helper.exactOutputSizeIfKnown(spliterator);
                
                // 1.如果流中元素的数量以及切割后的子流中的元素数量均是有限的，则可以直接切片
                if(size>0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    /*
                     * Because the pipeline is SIZED the slice spliterator
                     * can be created from the source, this requires matching
                     * to shape of the source, and is potentially more efficient
                     * than creating the slice spliterator from the pipeline wrapping spliterator
                     */
                    
                    // 返回一个"分片"流迭代器
                    Spliterator<P_IN> sliceSpliterator = sliceSpliterator(helper.getSourceShape(), spliterator, skip, limit);
                    
                    /*
                     * 并行搜集元素，中间依然会经过sink的择取操作(引用类型版本)。
                     * 将sliceSpliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
                     */
                    return Nodes.collectLong(helper, sliceSpliterator, true);
                }
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 2.如果helper流阶段的元素没有固定的遭遇顺序，则使用"无序"流迭代器
                if(!StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    /*
                     * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                     *
                     * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                     */
                    Spliterator<Long> wrapSpliterator = helper.wrapSpliterator(spliterator);
                    
                    // 返回一个"无序"流迭代器
                    Spliterator.OfLong s = unorderedSkipLimitSpliterator((Spliterator.OfLong) wrapSpliterator, skip, limit, size);
                    
                    /*
                     * Collect using this pipeline, which is empty and therefore
                     * can be used with the pipeline wrapping spliterator
                     * Note that we cannot create a slice spliterator from
                     * the source spliterator if the pipeline is not SIZED
                     */
                    /*
                     * 并行搜集元素，中间依然会经过sink的择取操作(long类型版本)。
                     * 将sliceSpliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
                     */
                    return Nodes.collectLong(this, s, true);
                }
                
                // 构造"分片"任务，用来跳过某段元素，并保留指定数量的元素
                SliceTask<P_IN, Long> sliceTask = new SliceTask<>(this, helper, spliterator, generator, skip, limit);
                
                // 执行任务，返回执行结果
                return sliceTask.invoke();
            }
            
            // 返回一个"无序"流迭代器
            Spliterator.OfLong unorderedSkipLimitSpliterator(Spliterator.OfLong s, long skip, long limit, long sizeIfKnown) {
                // 如果跳过的元素小于估计的元素数量，则需要调整skip和limit
                if(skip<=sizeIfKnown) {
                    // Use just the limit if the number of elements
                    // to skip is <= the known pipeline size
                    limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                    skip = 0;
                }
                
                return new StreamSpliterators.UnorderedSliceSpliterator.OfLong(s, skip, limit);
            }
            
        };
    }
    
    /**
     * Appends a "slice" operation to the provided DoubleStream.  The slice
     * operation may be may be skip-only, limit-only, or skip-and-limit.
     *
     * @param upstream A DoubleStream
     * @param skip     The number of elements to skip.  Must be >= 0.
     * @param limit    The maximum size of the resulting stream, or -1 if no limit
     *                 is to be imposed
     */
    /*
     * 构造有状态的中间阶段的流，适用于limit(long)方法和skip(long)方法(double类型版本)
     *
     * upstream: 上个阶段的流
     * skip    : 需要跳过的元素数量，必须>=0
     * limit   : 返回的流中可以包含的最大元素数量，-1表示没限制
     */
    public static DoubleStream makeDouble(AbstractPipeline<?, Double, ?> upstream, long skip, long limit) {
        if(skip<0) {
            throw new IllegalArgumentException("Skip must be non-negative: " + skip);
        }
        
        return new DoublePipeline.StatefulOp<Double>(upstream, StreamShape.DOUBLE_VALUE, flags(limit)) {
            
            // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
            @Override
            Sink<Double> opWrapSink(int flags, Sink<Double> downSink) {
                
                // 返回一个链式Sink，其中downstream的值就是downSink，即下个流阶段的sink
                return new Sink.ChainedDouble<Double>(downSink) {
                    
                    // 需要跳过的元素数量
                    long n = skip;
                    // 发送给下游的元素数量上限
                    long m = limit >= 0 ? limit : Long.MAX_VALUE;
                    
                    @Override
                    public void begin(long size) {
                        // 计算需要发给下游的元素数量
                        size = calcSize(size, skip, m);
                        downstream.begin(size);
                    }
                    
                    @Override
                    public void accept(double t) {
                        // 如果该跳过的元素都跳过了
                        if(n == 0) {
                            // 如果待发送元素量大于0，则向下游发送一个元素
                            if(m>0) {
                                m--;
                                downstream.accept(t);
                            }
                            
                            // 如果还有未跳过的元素，则计数递减
                        } else {
                            n--;
                        }
                    }
                    
                    @Override
                    public boolean cancellationRequested() {
                        // 如果已经没有待发送元素了，或者下游反馈了取消择取的信号，则此处也要向上游反馈"取消"信号
                        return m == 0 || downstream.cancellationRequested();
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
                
                /*
                 * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                 *
                 * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                 */
                Spliterator<Double> wrapSpliterator = helper.wrapSpliterator(spliterator);
                
                /*
                 * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
                 * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
                 *
                 * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
                 */
                long size = helper.exactOutputSizeIfKnown(spliterator);
                
                // 1.如果流中元素的数量以及切割后的子流中的元素数量均是有限的，则可以直接切片
                if(size>0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    // 计算切片的终止索引
                    long sliceFence = calcSliceFence(skip, limit);
                    
                    // 返回一个"分片"流迭代器
                    return new StreamSpliterators.SliceSpliterator.OfDouble((Spliterator.OfDouble) wrapSpliterator, skip, sliceFence);
                }
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 2.如果helper流阶段的元素没有固定的遭遇顺序，则使用"无序"流迭代器
                if(!StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    return unorderedSkipLimitSpliterator((Spliterator.OfDouble) wrapSpliterator, skip, limit, size);
                }
                
                /*
                 * 3.至此说明元素数量无限或未知，但是有固定的遭遇顺序。
                 *   此时需要使用特定的task来完成"分片"操作。
                 */
                
                // 构造"分片"任务，用来跳过某段元素，并保留指定数量的元素
                SliceTask<P_IN, Double> sliceTask = new SliceTask<>(this, helper, spliterator, Double[]::new, skip, limit);
                
                // 执行任务，返回执行结果
                Node<Double> node = sliceTask.invoke();
                
                // 返回可以代表node的流迭代器
                return node.spliterator();
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
                
                /*
                 * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
                 * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
                 *
                 * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
                 */
                long size = helper.exactOutputSizeIfKnown(spliterator);
                
                // 1.如果流中元素的数量以及切割后的子流中的元素数量均是有限的，则可以直接切片
                if(size>0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                    /*
                     * Because the pipeline is SIZED the slice spliterator
                     * can be created from the source, this requires matching
                     * to shape of the source, and is potentially more efficient
                     * than creating the slice spliterator from the pipeline wrapping spliterator
                     */
                    
                    // 返回一个"分片"流迭代器
                    Spliterator<P_IN> sliceSpliterator = sliceSpliterator(helper.getSourceShape(), spliterator, skip, limit);
                    
                    /*
                     * 并行搜集元素，中间依然会经过sink的择取操作(double类型版本)。
                     * 将sliceSpliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
                     */
                    return Nodes.collectDouble(helper, sliceSpliterator, true);
                }
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 2.如果helper流阶段的元素没有固定的遭遇顺序，则使用"无序"流迭代器
                if(!StreamOpFlag.ORDERED.isKnown(streamAndOpFlags)) {
                    
                    /*
                     * 返回一个"包装"流迭代器，使用该流迭代器可以获取helper流流阶段的输出元素。
                     *
                     * spliterator: 相对于helper流阶段的上个(depth==0)的流阶段的流迭代器
                     */
                    Spliterator<Double> wrapSpliterator = helper.wrapSpliterator(spliterator);
                    
                    // 返回一个"无序"流迭代器
                    Spliterator.OfDouble unorderedSpliterator = unorderedSkipLimitSpliterator((Spliterator.OfDouble) wrapSpliterator, skip, limit, size);
                    
                    /*
                     * Collect using this pipeline, which is empty and therefore
                     * can be used with the pipeline wrapping spliterator
                     * Note that we cannot create a slice spliterator from
                     * the source spliterator if the pipeline is not SIZED
                     */
                    /*
                     * 并行搜集元素，中间依然会经过sink的择取操作(double类型版本)。
                     * 将sliceSpliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
                     */
                    return Nodes.collectDouble(this, unorderedSpliterator, true);
                }
                
                /*
                 * 3.至此说明元素数量无限或未知，但是有固定的遭遇顺序。
                 *   此时需要使用特定的task来完成"分片"操作。
                 */
                
                // 构造"分片"任务，用来跳过某段元素，并保留指定数量的元素
                SliceTask<P_IN, Double> task = new SliceTask<>(this, helper, spliterator, generator, skip, limit);
                
                // 执行任务，返回执行结果
                return task.invoke();
            }
            
            // 返回一个"无序"流迭代器
            Spliterator.OfDouble unorderedSkipLimitSpliterator(Spliterator.OfDouble s, long skip, long limit, long sizeIfKnown) {
                // 如果跳过的元素小于估计的元素数量，则需要调整skip和limit
                if(skip<=sizeIfKnown) {
                    // Use just the limit if the number of elements to skip is <= the known pipeline size
                    limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                    skip = 0;
                }
                
                return new StreamSpliterators.UnorderedSliceSpliterator.OfDouble(s, skip, limit);
            }
            
        };
    }
    
    
    /**
     * Creates a slice spliterator given a stream shape governing the
     * spliterator type.  Requires that the underlying Spliterator
     * be SUBSIZED.
     */
    // 为不同类型的元素构造"分片"流迭代器
    @SuppressWarnings("unchecked")
    private static <P_IN> Spliterator<P_IN> sliceSpliterator(StreamShape shape, Spliterator<P_IN> spliterator, long skip, long limit) {
        assert spliterator.hasCharacteristics(Spliterator.SUBSIZED);
        
        long sliceFence = calcSliceFence(skip, limit);
        
        switch(shape) {
            case REFERENCE:
                return new StreamSpliterators.SliceSpliterator.OfRef<>(spliterator, skip, sliceFence);
            case INT_VALUE:
                return (Spliterator<P_IN>) new StreamSpliterators.SliceSpliterator.OfInt((Spliterator.OfInt) spliterator, skip, sliceFence);
            case LONG_VALUE:
                return (Spliterator<P_IN>) new StreamSpliterators.SliceSpliterator.OfLong((Spliterator.OfLong) spliterator, skip, sliceFence);
            case DOUBLE_VALUE:
                return (Spliterator<P_IN>) new StreamSpliterators.SliceSpliterator.OfDouble((Spliterator.OfDouble) spliterator, skip, sliceFence);
            default:
                throw new IllegalStateException("Unknown shape " + shape);
        }
    }
    
    /**
     * Calculates the sliced size given the current size, number of elements
     * skip, and the number of elements to limit.
     *
     * @param size  the current size
     * @param skip  the number of elements to skip, assumed to be >= 0
     * @param limit the number of elements to limit, assumed to be >= 0, with
     *              a value of {@code Long.MAX_VALUE} if there is no limit
     *
     * @return the sliced size
     */
    /*
     * 计算需要发给下游的元素数量
     *
     * size : 上游发来的元素数量
     * skip : 本阶段需要跳过的元素数量
     * limit: 传递给下游的元素数量上限
     */
    private static long calcSize(long size, long skip, long limit) {
        if(size<0) {
            return -1;
        }
        
        return Math.max(-1, Math.min(size - skip, limit));
    }
    
    /**
     * Calculates the slice fence, which is one past the index of the slice
     * range
     *
     * @param skip  the number of elements to skip, assumed to be >= 0
     * @param limit the number of elements to limit, assumed to be >= 0, with
     *              a value of {@code Long.MAX_VALUE} if there is no limit
     *
     * @return the slice fence.
     */
    /*
     * 计算切片的终止索引
     *
     * skip : 需要跳过的元素数量
     * limit: 子spliterator中包含的最大元素数量，-1表示没限制
     */
    private static long calcSliceFence(long skip, long limit) {
        long sliceFence = limit >= 0 ? skip + limit : Long.MAX_VALUE;
        
        // Check for overflow
        return (sliceFence >= 0) ? sliceFence : Long.MAX_VALUE;
    }
    
    // 获取当前阶段的流应当使用的操作参数
    private static int flags(long limit) {
        return StreamOpFlag.NOT_SIZED | ((limit != -1) ? StreamOpFlag.IS_SHORT_CIRCUIT : 0);
    }
    
    
    /**
     * {@code ForkJoinTask} implementing slice computation.
     *
     * @param <P_IN>  Input element type to the stream pipeline
     * @param <P_OUT> Output element type from the stream pipeline
     */
    // "分片"任务，用来跳过某段元素，并保留指定数量的元素
    @SuppressWarnings("serial")
    private static final class SliceTask<P_IN, P_OUT> extends AbstractShortCircuitTask<P_IN, P_OUT, Node<P_OUT>, SliceTask<P_IN, P_OUT>> {
        private final AbstractPipeline<P_OUT, P_OUT, ?> pipe;
        private final IntFunction<P_OUT[]> generator;
        private final long targetOffset, targetSize;
        private volatile boolean completed; // 指示当前任务是否已完成
        private long thisNodeSize; // 记录当前任务中包含的元素数量
        
        SliceTask(AbstractPipeline<P_OUT, P_OUT, ?> pipe, PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, IntFunction<P_OUT[]> generator, long offset, long size) {
            super(helper, spliterator);
            this.pipe = pipe;
            this.generator = generator;
            this.targetOffset = offset;
            this.targetSize = size;
        }
        
        SliceTask(SliceTask<P_IN, P_OUT> parent, Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            this.pipe = parent.pipe;
            this.generator = parent.generator;
            this.targetOffset = parent.targetOffset;
            this.targetSize = parent.targetSize;
        }
        
        // 在当前(子)任务执行完成后，需要执行该回调方法
        @Override
        public final void onCompletion(CountedCompleter<?> caller) {
            
            // 如果不是叶子结点
            if(!isLeaf()) {
                Node<P_OUT> result;
                
                // 统计子结点中包含的元素数量
                thisNodeSize = leftChild.thisNodeSize + rightChild.thisNodeSize;
                
                // 如果当前任务已经取消，则需要设置一个空结果
                if(canceled) {
                    thisNodeSize = 0;
                    result = getEmptyResult();
                    
                    // 如果当前结点包含的元素数量为0，同样需要设置空结果
                } else if(thisNodeSize == 0) {
                    result = getEmptyResult();
                    
                    // 如果当前结点的左孩子结点包含的元素数量为0，则需要设置右孩子结点的执行结果
                } else if(leftChild.thisNodeSize == 0) {
                    result = rightChild.getLocalResult();
                    
                    // 对左右孩子的执行结果进行连接，生成一个"树状"Node
                } else {
                    result = Nodes.conc(pipe.getOutputShape(), leftChild.getLocalResult(), rightChild.getLocalResult());
                }
                
                /*
                 * 如果是根结点，则需要根据limit限制来确定最终需要保存的元素数量；
                 * 如果是中间结点，则直接设置执行结果
                 */
                setLocalResult(isRoot() ? doTruncate(result) : result);
                
                // 标记当前任务已经完成
                completed = true;
            }
            
            // 如果当前非根结点需要保留的元素数量>=0，且此结点及此结点左侧结点中已完成的任务中包含的元素数量已经大于目标值
            if(targetSize >= 0 && !isRoot() && isLeftCompleted(targetOffset + targetSize)) {
                // 取消当前任务的所有右兄弟任务及其所有父级的右兄弟任务
                cancelLaterNodes();
            }
            
            super.onCompletion(caller);
        }
        
        // 返回子任务的计算结果
        @Override
        protected final Node<P_OUT> doLeaf() {
            
            // 如果当前任务为根任务
            if(isRoot()) {
                
                /*
                 * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
                 * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
                 *
                 * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
                 */
                long sizeIfKnown = StreamOpFlag.SIZED.isPreserved(pipe.sourceOrOpFlags) ? pipe.exactOutputSizeIfKnown(spliterator) : -1;
                
                // 构造增强"数组"Node或"弹性缓冲区"Node(引用类型版本)
                final Node.Builder<P_OUT> nodeBuilderSink = pipe.makeNodeBuilder(sizeIfKnown, generator);
                
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
                helper.copyIntoWithCancel(wrappedSink, spliterator);
                
                /* There is no need to truncate since the op performs the skipping and limiting of elements */
                // 返回构造的node
                return nodeBuilderSink.build();
                
                // 如果当前任务不是根任务
            } else {
                
                // 构造增强"弹性缓冲区"Node(引用类型版本)
                final Node.Builder<P_OUT> nodeBuilderSink = pipe.makeNodeBuilder(-1, generator);
                
                // 如果不需要跳过元素
                if(targetOffset == 0) {
                    
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
                    helper.copyIntoWithCancel(wrappedSink, spliterator);
                    
                    // 如果不需要跳过元素，则只需要将nodeBuilderSink作为当前的阶段的sink即可
                } else {
                    /*
                     * 从nodeBuilderSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
                     * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
                     * 最终择取出的数据往往被存入了nodeBuilderSink代表的容器当中。
                     *
                     * nodeBuilderSink: (相对于helper的)下个流阶段的sink。如果nodeBuilderSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
                     * spliterator    : 流迭代器，作为数据源，包含了当前所有待访问的元素
                     */
                    helper.wrapAndCopyInto(nodeBuilderSink, spliterator);
                }
                
                // 记下执行结果
                Node<P_OUT> node = nodeBuilderSink.build();
                
                // 记录当前任务中包含的元素数量
                thisNodeSize = node.count();
                
                // 标记当前任务已完成
                completed = true;
                spliterator = null;
                
                return node;
            }
        }
        
        // 返回一个空结果
        @Override
        protected final Node<P_OUT> getEmptyResult() {
            return Nodes.emptyNode(pipe.getOutputShape());
        }
        
        // 构造新的子任务结点
        @Override
        protected SliceTask<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator) {
            return new SliceTask<>(this, spliterator);
        }
        
        // 取消当前任务
        @Override
        protected void cancel() {
            super.cancel();
            
            // 如果任务已完成，则设置一个空结果
            if(completed) {
                setLocalResult(getEmptyResult());
            }
        }
        
        // 根据limit限制来确定最终需要保存的元素数量
        private Node<P_OUT> doTruncate(Node<P_OUT> input) {
            long to = targetSize >= 0 ? Math.min(input.count(), targetOffset + targetSize) : thisNodeSize;
            
            /*
             * 将Node中[targetOffset, to)范围内的元素打包到由generator新建的子Node中返回
             * 注：打包过程可能伴随着进一步的择取
             */
            return input.truncate(targetOffset, to, generator);
        }
        
        /**
         * Determine if the number of completed elements in this node and nodes
         * to the left of this node is greater than or equal to the target size.
         *
         * @param target the target size
         *
         * @return true if the number of elements is greater than or equal to
         * the target size, otherwise false.
         */
        // 判断此结点及此结点左侧结点中已完成的任务中包含的元素数量是否大于目标值
        private boolean isLeftCompleted(long target) {
            long size = completed ? thisNodeSize : completedSize(target);
            if(size >= target) {
                return true;
            }
    
            for(SliceTask<P_IN, P_OUT> parent = getParent(), node = this; parent != null; node = parent, parent = parent.getParent()) {
                if(node == parent.rightChild) {
                    SliceTask<P_IN, P_OUT> left = parent.leftChild;
                    if(left != null) {
                        size += left.completedSize(target);
                        if(size >= target) {
                            return true;
                        }
                    }
                }
            }
    
            return size >= target;
        }
        
        /**
         * Compute the number of completed elements in this node.
         * <p>
         * Computation terminates if all nodes have been processed or the
         * number of completed elements is greater than or equal to the target
         * size.
         *
         * @param target the target size
         *
         * @return the number of completed elements
         */
        private long completedSize(long target) {
            if(completed) {
                return thisNodeSize;
            }
            
            SliceTask<P_IN, P_OUT> left = leftChild;
            SliceTask<P_IN, P_OUT> right = rightChild;
            if(left == null || right == null) {
                // must be completed
                return thisNodeSize;
            } else {
                long leftSize = left.completedSize(target);
                return (leftSize >= target) ? leftSize : leftSize + right.completedSize(target);
            }
        }
    }
    
}
