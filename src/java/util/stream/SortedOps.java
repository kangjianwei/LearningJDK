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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntFunction;

/**
 * Factory methods for transforming streams into sorted streams.
 *
 * @since 1.8
 */
// 应用在有状态的中间阶段的辅助类，服务于sorted()和sorted(Comparator)方法
final class SortedOps {
    
    private SortedOps() {
    }
    
    
    /**
     * Appends a "sorted" operation to the provided stream.
     *
     * @param <T>        the type of both input and output elements
     * @param upstream   a reference stream with element type T
     * @param comparator the comparator to order elements by
     */
    // 构造支持"排序"操作的流的中间阶段(引用类型版本)，排序规则由外部比较器comparator给出
    static <T> Stream<T> makeRef(AbstractPipeline<?, T, ?> upstream, Comparator<? super T> comparator) {
        return new OfRef<>(upstream, comparator);
    }
    
    
    /**
     * Appends a "sorted" operation to the provided stream.
     *
     * @param <T>      the type of both input and output elements
     * @param upstream a reference stream with element type T
     */
    // 构造支持"排序"操作的流的中间阶段(引用类型版本)
    static <T> Stream<T> makeRef(AbstractPipeline<?, T, ?> upstream) {
        return new OfRef<>(upstream);
    }
    
    /**
     * Appends a "sorted" operation to the provided stream.
     *
     * @param <T>      the type of both input and output elements
     * @param upstream a reference stream with element type T
     */
    // 构造支持"排序"操作的流的中间阶段(int类型版本)
    static <T> IntStream makeInt(AbstractPipeline<?, Integer, ?> upstream) {
        return new OfInt(upstream);
    }
    
    /**
     * Appends a "sorted" operation to the provided stream.
     *
     * @param <T>      the type of both input and output elements
     * @param upstream a reference stream with element type T
     */
    // 构造支持"排序"操作的流的中间阶段(long类型版本)
    static <T> LongStream makeLong(AbstractPipeline<?, Long, ?> upstream) {
        return new OfLong(upstream);
    }
    
    /**
     * Appends a "sorted" operation to the provided stream.
     *
     * @param <T>      the type of both input and output elements
     * @param upstream a reference stream with element type T
     */
    // 构造支持"排序"操作的流的中间阶段(double类型版本)
    static <T> DoubleStream makeDouble(AbstractPipeline<?, Double, ?> upstream) {
        return new OfDouble(upstream);
    }
    
    
    /**
     * Specialized subtype for sorting reference streams
     */
    // 支持"排序"操作的流的中间阶段(引用类型版本)
    private static final class OfRef<T> extends ReferencePipeline.StatefulOp<T, T> {
        
        /**
         * Comparator used for sorting
         */
        private final boolean isNaturalSort;            // 是否遵循自然排序，默认是true
        private final Comparator<? super T> comparator; // 外部比较器
        
        /**
         * Sort using natural order of {@literal <T>} which must be
         * {@code Comparable}.
         */
        // 默认遵循自然顺序进行比较
        OfRef(AbstractPipeline<?, T, ?> upstream) {
            super(upstream, StreamShape.REFERENCE, StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SORTED);
            
            this.isNaturalSort = true;
            
            /*
             * 获取“自然顺序”比较器，用于比较实现了Comparable的对象
             *
             * 注：所谓自然顺序是指顺应对象内部的Comparable排序规则
             */
            @SuppressWarnings("unchecked")
            Comparator<? super T> cmp = (Comparator<? super T>) Comparator.naturalOrder();
            
            this.comparator = cmp;
        }
        
        /**
         * Sort using the provided comparator.
         *
         * @param comparator The comparator to be used to evaluate ordering.
         */
        // 比较方式由comparator决定
        OfRef(AbstractPipeline<?, T, ?> upstream, Comparator<? super T> comparator) {
            super(upstream, StreamShape.REFERENCE, StreamOpFlag.IS_ORDERED | StreamOpFlag.NOT_SORTED);
            this.isNaturalSort = false;
            this.comparator = Objects.requireNonNull(comparator);
        }
        
        // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
        @Override
        public Sink<T> opWrapSink(int flags, Sink<T> downSink) {
            Objects.requireNonNull(downSink);
            
            /* If the input is already naturally sorted and this operation also naturally sorted then this is a no-op */
            // 1.如果上个阶段的流已经有序，且遵循自然顺序，则可以直接返回downSink
            if(StreamOpFlag.SORTED.isKnown(flags) && isNaturalSort) {
                return downSink;
            }
            
            // 2.如果上个阶段的流中元素数量有限，则可以使用支持"有限元素排序"的Sink
            if(StreamOpFlag.SIZED.isKnown(flags)) {
                return new SizedRefSortingSink<>(downSink, comparator);
            }
            
            // 3.如果上个阶段的流中元素数量无限或未知，则需要使用支持"无限元素排序"的Sink
            return new RefSortingSink<>(downSink, comparator);
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
        public <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> helper, Spliterator<P_IN> spliterator, IntFunction<T[]> generator) {
            
            // 获取helper流阶段的组合参数
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            // helper流阶段的元素是否已经按自然顺序排好序了
            boolean sorted = StreamOpFlag.SORTED.isKnown(streamAndOpFlags) && isNaturalSort;
            
            /*
             * 获取helper流阶段的输出元素
             *
             * 将spliterator中的元素搜集到generator生成的数组中，然后将该数组封装到Node中返回。
             * 这个搜集过程会涉及到数据的择取过程，即数据流通常从(相对于helper流阶段的)上一个(depth==1)的流阶段的sink开始，经过整个sink链的筛选后，进入终端阶段的sink。
             *
             * flatten: 指示在并行操作中，如果中间生成的Node是树状Node，则是否要将其转换为非树状Node后再返回。
             */
            Node<T> node = helper.evaluate(spliterator, !sorted, generator);
            
            // 如果helper流阶段的元素已经排好序了，则符合当前流阶段的预期，可以直接将其返回了
            if(sorted) {
                return node;
            }
            
            // 将Node中的元素复制到使用generator构造的数组中后返回
            T[] flattenedData = node.asArray(generator);
            
            // 上面得到的元素无序，则此处将数组元素按外部比较器的比较规则进行排序
            Arrays.parallelSort(flattenedData, comparator);
            
            // 构造普通"数组"Node(引用类型版本)
            return Nodes.node(flattenedData);
        }
    }
    
    /**
     * Specialized subtype for sorting int streams.
     */
    // 支持"排序"操作的流的中间阶段(int类型版本)
    private static final class OfInt extends IntPipeline.StatefulOp<Integer> {
        OfInt(AbstractPipeline<?, Integer, ?> upstream) {
            super(upstream, StreamShape.INT_VALUE, StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SORTED);
        }
        
        // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
        @Override
        public Sink<Integer> opWrapSink(int flags, Sink<Integer> downSink) {
            Objects.requireNonNull(downSink);
            
            // 1.如果上个阶段的流已经有序，则可以直接返回downSink
            if(StreamOpFlag.SORTED.isKnown(flags)) {
                return downSink;
            }
            
            // 2.如果上个阶段的流中元素数量有限，则可以使用支持"有限元素排序"的Sink
            if(StreamOpFlag.SIZED.isKnown(flags)) {
                return new SizedIntSortingSink(downSink);
            }
            
            // 3.如果上个阶段的流中元素数量无限或未知，则需要使用支持"无限元素排序"的Sink
            return new IntSortingSink(downSink);
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
        public <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator, IntFunction<Integer[]> generator) {
            
            // 获取helper流阶段的组合参数
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            // helper流阶段的元素是否已经按自然顺序排好序了
            boolean sorted = StreamOpFlag.SORTED.isKnown(streamAndOpFlags);
            
            /*
             * 获取helper流阶段的输出元素
             *
             * 将spliterator中的元素搜集到generator生成的数组中，然后将该数组封装到Node中返回。
             * 这个搜集过程会涉及到数据的择取过程，即数据流通常从(相对于helper流阶段的)上一个(depth==1)的流阶段的sink开始，经过整个sink链的筛选后，进入终端阶段的sink。
             *
             * flatten: 指示在并行操作中，如果中间生成的Node是树状Node，则是否要将其转换为非树状Node后再返回。
             */
            Node.OfInt node = (Node.OfInt) helper.evaluate(spliterator, !sorted, generator);
            
            // 如果helper流阶段的元素已经排好序了，则符合当前流阶段的预期，可以直接将其返回了
            if(sorted) {
                return node;
            }
            
            // 将Node中的元素存入基本类型数组后返回
            int[] flattenedData = node.asPrimitiveArray();
            
            // 上面得到的元素无序，则此处将数组元素按外部比较器的比较规则进行排序
            Arrays.parallelSort(flattenedData);
            
            // 构造普通"数组"Node(int类型版本)
            return Nodes.node(flattenedData);
        }
    }
    
    /**
     * Specialized subtype for sorting long streams.
     */
    // 支持"排序"操作的流的中间阶段(long类型版本)
    private static final class OfLong extends LongPipeline.StatefulOp<Long> {
        OfLong(AbstractPipeline<?, Long, ?> upstream) {
            super(upstream, StreamShape.LONG_VALUE, StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SORTED);
        }
        
        // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
        @Override
        public Sink<Long> opWrapSink(int flags, Sink<Long> downSink) {
            Objects.requireNonNull(downSink);
            
            // 1.如果上个阶段的流已经有序，且遵循自然顺序，则可以直接返回downSink
            if(StreamOpFlag.SORTED.isKnown(flags)) {
                return downSink;
            }
            
            // 2.如果上个阶段的流中元素数量有限，则可以使用支持"有限元素排序"的Sink
            if(StreamOpFlag.SIZED.isKnown(flags)) {
                return new SizedLongSortingSink(downSink);
            }
            
            // 3.如果上个阶段的流中元素数量无限或未知，则需要使用支持"无限元素排序"的Sink
            return new LongSortingSink(downSink);
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
        public <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator, IntFunction<Long[]> generator) {
            
            // 获取helper流阶段的组合参数
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            // helper流阶段的元素是否已经按自然顺序排好序了
            boolean sorted = StreamOpFlag.SORTED.isKnown(streamAndOpFlags);
            
            /*
             * 获取helper流阶段的输出元素
             *
             * 将spliterator中的元素搜集到generator生成的数组中，然后将该数组封装到Node中返回。
             * 这个搜集过程会涉及到数据的择取过程，即数据流通常从(相对于helper流阶段的)上一个(depth==1)的流阶段的sink开始，经过整个sink链的筛选后，进入终端阶段的sink。
             *
             * flatten: 指示在并行操作中，如果中间生成的Node是树状Node，则是否要将其转换为非树状Node后再返回。
             */
            Node.OfLong node = (Node.OfLong) helper.evaluate(spliterator, !sorted, generator);
            
            // 如果helper流阶段的元素已经排好序了，则符合当前流阶段的预期，可以直接将其返回了
            if(sorted) {
                return node;
            }
            
            // 将Node中的元素存入基本类型数组后返回
            long[] flattenedData = node.asPrimitiveArray();
            
            // 上面得到的元素无序，则此处将数组元素按外部比较器的比较规则进行排序
            Arrays.parallelSort(flattenedData);
            
            // 构造普通"数组"Node(int类型版本)
            return Nodes.node(flattenedData);
        }
    }
    
    /**
     * Specialized subtype for sorting double streams.
     */
    // 支持"排序"操作的流的中间阶段(double类型版本)
    private static final class OfDouble extends DoublePipeline.StatefulOp<Double> {
        OfDouble(AbstractPipeline<?, Double, ?> upstream) {
            super(upstream, StreamShape.DOUBLE_VALUE, StreamOpFlag.IS_ORDERED | StreamOpFlag.IS_SORTED);
        }
        
        // 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据
        @Override
        public Sink<Double> opWrapSink(int flags, Sink<Double> downSink) {
            Objects.requireNonNull(downSink);
            
            // 1.如果上个阶段的流已经有序，且遵循自然顺序，则可以直接返回downSink
            if(StreamOpFlag.SORTED.isKnown(flags)) {
                return downSink;
            }
            
            // 2.如果上个阶段的流中元素数量有限，则可以使用支持"有限元素排序"的Sink
            if(StreamOpFlag.SIZED.isKnown(flags)) {
                return new SizedDoubleSortingSink(downSink);
            }
            
            // 3.如果上个阶段的流中元素数量无限或未知，则需要使用支持"无限元素排序"的Sink
            return new DoubleSortingSink(downSink);
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
        public <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator, IntFunction<Double[]> generator) {
            
            // 获取helper流阶段的组合参数
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            // helper流阶段的元素是否已经按自然顺序排好序了
            boolean sorted = StreamOpFlag.SORTED.isKnown(streamAndOpFlags);
            
            /*
             * 获取helper流阶段的输出元素
             *
             * 将spliterator中的元素搜集到generator生成的数组中，然后将该数组封装到Node中返回。
             * 这个搜集过程会涉及到数据的择取过程，即数据流通常从(相对于helper流阶段的)上一个(depth==1)的流阶段的sink开始，经过整个sink链的筛选后，进入终端阶段的sink。
             *
             * flatten: 指示在并行操作中，如果中间生成的Node是树状Node，则是否要将其转换为非树状Node后再返回。
             */
            Node.OfDouble node = (Node.OfDouble) helper.evaluate(spliterator, !sorted, generator);
            
            // 如果helper流阶段的元素已经排好序了，则符合当前流阶段的预期，可以直接将其返回了
            if(sorted) {
                return node;
            }
            
            // 将Node中的元素存入基本类型数组后返回
            double[] flattenedData = node.asPrimitiveArray();
            
            // 上面得到的元素无序，则此处将数组元素按外部比较器的比较规则进行排序
            Arrays.parallelSort(flattenedData);
            
            // 构造普通"数组"Node(int类型版本)
            return Nodes.node(flattenedData);
        }
    }
    
    
    /**
     * Abstract {@link Sink} for implementing sort on reference streams.
     *
     * <p>
     * Note: documentation below applies to reference and all primitive sinks.
     * <p>
     * Sorting sinks first accept all elements, buffering then into an array
     * or a re-sizable data structure, if the size of the pipeline is known or
     * unknown respectively.  At the end of the sink protocol those elements are
     * sorted and then pushed downstream.
     * This class records if {@link #cancellationRequested} is called.  If so it
     * can be inferred that the source pushing source elements into the pipeline
     * knows that the pipeline is short-circuiting.  In such cases sub-classes
     * pushing elements downstream will preserve the short-circuiting protocol
     * by calling {@code downstream.cancellationRequested()} and checking the
     * result is {@code false} before an element is pushed.
     * <p>
     * Note that the above behaviour is an optimization for sorting with
     * sequential streams.  It is not an error that more elements, than strictly
     * required to produce a result, may flow through the pipeline.  This can
     * occur, in general (not restricted to just sorting), for short-circuiting
     * parallel pipelines.
     */
    // "排序"Sink的抽象实现(引用类型版本)
    private abstract static class AbstractRefSortingSink<T> extends Sink.ChainedReference<T, T> {
        
        // 外部比较器，只有引用类型版本支持
        protected final Comparator<? super T> comparator;
        
        /**
         * could be a lazy final value, if/when support is added
         * true if cancellationRequested() has been called
         */
        // 是否收到了取消请求
        protected boolean cancellationRequestedCalled;
        
        AbstractRefSortingSink(Sink<? super T> downstream, Comparator<? super T> comparator) {
            super(downstream);
            this.comparator = comparator;
        }
        
        /**
         * Records is cancellation is requested so short-circuiting behaviour
         * can be preserved when the sorted elements are pushed downstream.
         *
         * @return false, as this sink never short-circuits.
         */
        /*
         * 判断是否应当停止接收数据。
         * 在一些短路操作中，可以在达到某个目标后取消后续的操作。
         */
        @Override
        public final boolean cancellationRequested() {
            /*
             * If this method is called then an operation within the stream
             * pipeline is short-circuiting (see AbstractPipeline.copyInto).
             * Note that we cannot differentiate between an upstream or downstream operation
             */
            cancellationRequestedCalled = true;
            return false;
        }
    }
    
    /**
     * Abstract {@link Sink} for implementing sort on int streams.
     */
    // "排序"Sink的抽象实现(int类型版本)
    private abstract static class AbstractIntSortingSink extends Sink.ChainedInt<Integer> {
        
        /** true if cancellationRequested() has been called */
        // 是否收到了取消请求
        protected boolean cancellationRequestedCalled;
        
        AbstractIntSortingSink(Sink<? super Integer> downstream) {
            super(downstream);
        }
        
        /*
         * 判断是否应当停止接收数据。
         * 在一些短路操作中，可以在达到某个目标后取消后续的操作。
         */
        @Override
        public final boolean cancellationRequested() {
            cancellationRequestedCalled = true;
            return false;
        }
    }
    
    /**
     * Abstract {@link Sink} for implementing sort on long streams.
     */
    // "排序"Sink的抽象实现(long类型版本)
    private abstract static class AbstractLongSortingSink extends Sink.ChainedLong<Long> {
        
        /** true if cancellationRequested() has been called */
        // 是否收到了取消请求
        protected boolean cancellationRequestedCalled;
        
        AbstractLongSortingSink(Sink<? super Long> downstream) {
            super(downstream);
        }
        
        /*
         * 判断是否应当停止接收数据。
         * 在一些短路操作中，可以在达到某个目标后取消后续的操作。
         */
        @Override
        public final boolean cancellationRequested() {
            cancellationRequestedCalled = true;
            return false;
        }
    }
    
    /**
     * Abstract {@link Sink} for implementing sort on long streams.
     */
    // "排序"Sink的抽象实现(double类型版本)
    private abstract static class AbstractDoubleSortingSink extends Sink.ChainedDouble<Double> {
        
        /** true if cancellationRequested() has been called */
        // 是否收到了取消请求
        protected boolean cancellationRequestedCalled;
        
        AbstractDoubleSortingSink(Sink<? super Double> downstream) {
            super(downstream);
        }
        
        /*
         * 判断是否应当停止接收数据。
         * 在一些短路操作中，可以在达到某个目标后取消后续的操作。
         */
        @Override
        public final boolean cancellationRequested() {
            cancellationRequestedCalled = true;
            return false;
        }
    }
    
    
    /**
     * {@link Sink} for implementing sort on SIZED reference streams.
     */
    // 支持"有限元素排序"的Sink(引用类型版本)
    private static final class SizedRefSortingSink<T> extends AbstractRefSortingSink<T> {
        private T[] array;   // 存储上游发来的数据
        private int offset;  // 记录存储的元素数量
        
        SizedRefSortingSink(Sink<? super T> sink, Comparator<? super T> comparator) {
            super(sink, comparator);
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        @SuppressWarnings("unchecked")
        public void begin(long size) {
            if(size >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            
            array = (T[]) new Object[(int) size];
        }
        
        /*
         * 对上游发来的引用类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(T t) {
            array[offset++] = t;
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            
            // 在收尾阶段进行了排序
            Arrays.sort(array, 0, offset, comparator);
            
            // 将元素数量传递给了下游
            downstream.begin(offset);
            
            // 如果没有收到取消信号，则一切照旧，将当前阶段的元素传递到下游
            if(!cancellationRequestedCalled) {
                for(int i = 0; i<offset; i++) {
                    downstream.accept(array[i]);
                }
                
                // 如果收到了取消申请，则需要将该申请向下游传递，如果下游反馈说可以取消，则停止向下游传输数据
            } else {
                for(int i = 0; i<offset && !downstream.cancellationRequested(); i++) {
                    downstream.accept(array[i]);
                }
            }
            
            // 结束流
            downstream.end();
            
            // 释放数组资源
            array = null;
        }
        
    }
    
    /**
     * {@link Sink} for implementing sort on SIZED int streams.
     */
    // 支持"有限元素排序"的Sink(int类型版本)
    private static final class SizedIntSortingSink extends AbstractIntSortingSink {
        private int[] array;  // 存储上游发来的数据
        private int offset;   // 记录存储的元素数量
        
        SizedIntSortingSink(Sink<? super Integer> downstream) {
            super(downstream);
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            
            array = new int[(int) size];
        }
        
        /*
         * 对上游发来的int类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(int t) {
            array[offset++] = t;
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            
            // 在收尾阶段进行了排序
            Arrays.sort(array, 0, offset);
            
            // 将元素数量传递给了下游
            downstream.begin(offset);
            
            // 如果没有收到取消信号，则一切照旧，将当前阶段的元素传递到下游
            if(!cancellationRequestedCalled) {
                for(int i = 0; i<offset; i++) {
                    downstream.accept(array[i]);
                }
                
                // 如果收到了取消申请，则需要将该申请向下游传递，如果下游反馈说可以取消，则停止向下游传输数据
            } else {
                for(int i = 0; i<offset && !downstream.cancellationRequested(); i++) {
                    downstream.accept(array[i]);
                }
            }
            
            // 结束流
            downstream.end();
            
            // 释放数组资源
            array = null;
        }
        
    }
    
    /**
     * {@link Sink} for implementing sort on SIZED long streams.
     */
    // 支持"有限元素排序"的Sink(long类型版本)
    private static final class SizedLongSortingSink extends AbstractLongSortingSink {
        private long[] array;  // 存储上游发来的数据
        private int offset;    // 记录存储的元素数量
        
        SizedLongSortingSink(Sink<? super Long> downstream) {
            super(downstream);
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            array = new long[(int) size];
        }
        
        /*
         * 对上游发来的long类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(long t) {
            array[offset++] = t;
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            
            // 在收尾阶段进行了排序
            Arrays.sort(array, 0, offset);
            
            // 将元素数量传递给了下游
            downstream.begin(offset);
            
            // 如果没有收到取消信号，则一切照旧，将当前阶段的元素传递到下游
            if(!cancellationRequestedCalled) {
                for(int i = 0; i<offset; i++) {
                    downstream.accept(array[i]);
                }
                
                // 如果收到了取消申请，则需要将该申请向下游传递，如果下游反馈说可以取消，则停止向下游传输数据
            } else {
                for(int i = 0; i<offset && !downstream.cancellationRequested(); i++) {
                    downstream.accept(array[i]);
                }
            }
            
            // 结束流
            downstream.end();
            
            // 释放数组资源
            array = null;
        }
        
    }
    
    /**
     * {@link Sink} for implementing sort on SIZED double streams.
     */
    // 支持"有限元素排序"的Sink(double类型版本)
    private static final class SizedDoubleSortingSink extends AbstractDoubleSortingSink {
        private double[] array;  // 存储上游发来的数据
        private int offset;      // 记录存储的元素数量
        
        SizedDoubleSortingSink(Sink<? super Double> downstream) {
            super(downstream);
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            array = new double[(int) size];
        }
        
        /*
         * 对上游发来的double类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(double t) {
            array[offset++] = t;
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            
            // 在收尾阶段进行了排序
            Arrays.sort(array, 0, offset);
            
            // 将元素数量传递给了下游
            downstream.begin(offset);
            
            // 如果没有收到取消信号，则一切照旧，将当前阶段的元素传递到下游
            if(!cancellationRequestedCalled) {
                for(int i = 0; i<offset; i++) {
                    downstream.accept(array[i]);
                }
                
                // 如果收到了取消申请，则需要将该申请向下游传递，如果下游反馈说可以取消，则停止向下游传输数据
            } else {
                for(int i = 0; i<offset && !downstream.cancellationRequested(); i++) {
                    downstream.accept(array[i]);
                }
            }
            
            // 结束流
            downstream.end();
            
            // 释放数组资源
            array = null;
        }
        
    }
    
    
    /**
     * {@link Sink} for implementing sort on reference streams.
     */
    // 支持"无限元素排序"的Sink(引用类型版本)
    private static final class RefSortingSink<T> extends AbstractRefSortingSink<T> {
        private ArrayList<T> list; // 存储上游发来的数据
        
        RefSortingSink(Sink<? super T> sink, Comparator<? super T> comparator) {
            super(sink, comparator);
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            
            list = (size >= 0) ? new ArrayList<>((int) size) : new ArrayList<>();
        }
        
        /*
         * 对上游发来的引用类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(T t) {
            list.add(t);
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            
            // 在收尾阶段进行了排序
            list.sort(comparator);
            
            // 将元素数量传递给了下游
            downstream.begin(list.size());
            
            // 如果没有收到取消信号，则一切照旧，将当前阶段的元素传递到下游
            if(!cancellationRequestedCalled) {
                list.forEach(downstream::accept);
                
                // 如果收到了取消申请，则需要将该申请向下游传递，如果下游反馈说可以取消，则停止向下游传输数据
            } else {
                for(T t : list) {
                    if(downstream.cancellationRequested()) {
                        break;
                    }
                    
                    downstream.accept(t);
                }
            }
            
            // 结束流
            downstream.end();
            
            // 释放缓存
            list = null;
        }
        
    }
    
    /**
     * {@link Sink} for implementing sort on int streams.
     */
    // 支持"无限元素排序"的Sink(int类型版本)
    private static final class IntSortingSink extends AbstractIntSortingSink {
        private SpinedBuffer.OfInt buffer;  // 存储上游发来的数据
        
        IntSortingSink(Sink<? super Integer> sink) {
            super(sink);
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            
            buffer = (size>0) ? new SpinedBuffer.OfInt((int) size) : new SpinedBuffer.OfInt();
        }
        
        /*
         * 对上游发来的int类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(int t) {
            buffer.accept(t);
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            // 将buffer中的元素存入基本类型数组后返回
            int[] ints = buffer.asPrimitiveArray();
            
            // 在收尾阶段进行了排序
            Arrays.sort(ints);
            
            // 将元素数量传递给了下游
            downstream.begin(ints.length);
            
            // 如果没有收到取消信号，则一切照旧，将当前阶段的元素传递到下游
            if(!cancellationRequestedCalled) {
                for(int anInt : ints) {
                    downstream.accept(anInt);
                }
                
                // 如果收到了取消申请，则需要将该申请向下游传递，如果下游反馈说可以取消，则停止向下游传输数据
            } else {
                for(int anInt : ints) {
                    if(downstream.cancellationRequested()) {
                        break;
                    }
                    downstream.accept(anInt);
                }
            }
            
            // 结束流
            downstream.end();
        }
        
    }
    
    /**
     * {@link Sink} for implementing sort on long streams.
     */
    // 支持"无限元素排序"的Sink(long类型版本)
    private static final class LongSortingSink extends AbstractLongSortingSink {
        private SpinedBuffer.OfLong buffer;  // 存储上游发来的数据
        
        LongSortingSink(Sink<? super Long> sink) {
            super(sink);
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            
            buffer = (size>0) ? new SpinedBuffer.OfLong((int) size) : new SpinedBuffer.OfLong();
        }
        
        /*
         * 对上游发来的long类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(long t) {
            buffer.accept(t);
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            // 将buffer中的元素存入基本类型数组后返回
            long[] longs = buffer.asPrimitiveArray();
            
            // 在收尾阶段进行了排序
            Arrays.sort(longs);
            
            // 将元素数量传递给了下游
            downstream.begin(longs.length);
            
            // 如果没有收到取消信号，则一切照旧，将当前阶段的元素传递到下游
            if(!cancellationRequestedCalled) {
                for(long aLong : longs) {
                    downstream.accept(aLong);
                }
                
                // 如果收到了取消申请，则需要将该申请向下游传递，如果下游反馈说可以取消，则停止向下游传输数据
            } else {
                for(long aLong : longs) {
                    if(downstream.cancellationRequested()) {
                        break;
                    }
                    downstream.accept(aLong);
                }
            }
            
            // 结束流
            downstream.end();
        }
        
    }
    
    /**
     * {@link Sink} for implementing sort on double streams.
     */
    // 支持"无限元素排序"的Sink(double类型版本)
    private static final class DoubleSortingSink extends AbstractDoubleSortingSink {
        private SpinedBuffer.OfDouble buffer;  // 存储上游发来的数据
        
        DoubleSortingSink(Sink<? super Double> sink) {
            super(sink);
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            
            buffer = (size>0) ? new SpinedBuffer.OfDouble((int) size) : new SpinedBuffer.OfDouble();
        }
        
        /*
         * 对上游发来的double类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(double t) {
            buffer.accept(t);
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            // 将buffer中的元素存入基本类型数组后返回
            double[] doubles = buffer.asPrimitiveArray();
            
            // 在收尾阶段进行了排序
            Arrays.sort(doubles);
            
            // 将元素数量传递给了下游
            downstream.begin(doubles.length);
            
            // 如果没有收到取消信号，则一切照旧，将当前阶段的元素传递到下游
            if(!cancellationRequestedCalled) {
                for(double aDouble : doubles) {
                    downstream.accept(aDouble);
                }
                
                // 如果收到了取消申请，则需要将该申请向下游传递，如果下游反馈说可以取消，则停止向下游传输数据
            } else {
                for(double aDouble : doubles) {
                    if(downstream.cancellationRequested()) {
                        break;
                    }
                    downstream.accept(aDouble);
                }
            }
            
            // 结束流
            downstream.end();
        }
        
    }
    
}
