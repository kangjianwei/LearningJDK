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

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Abstract base class for "pipeline" classes, which are the core
 * implementations of the Stream interface and its primitive specializations.
 * Manages construction and evaluation of stream pipelines.
 *
 * <p>An {@code AbstractPipeline} represents an initial portion of a stream
 * pipeline, encapsulating a stream source and zero or more intermediate
 * operations.  The individual {@code AbstractPipeline} objects are often
 * referred to as <em>stages</em>, where each stage describes either the stream
 * source or an intermediate operation.
 *
 * <p>A concrete intermediate stage is generally built from an
 * {@code AbstractPipeline}, a shape-specific pipeline class which extends it
 * (e.g., {@code IntPipeline}) which is also abstract, and an operation-specific
 * concrete class which extends that.  {@code AbstractPipeline} contains most of
 * the mechanics of evaluating the pipeline, and implements methods that will be
 * used by the operation; the shape-specific classes add helper methods for
 * dealing with collection of results into the appropriate shape-specific
 * containers.
 *
 * <p>After chaining a new intermediate operation, or executing a terminal
 * operation, the stream is considered to be consumed, and no more intermediate
 * or terminal operations are permitted on this stream instance.
 *
 * @implNote
 * <p>For sequential streams, and parallel streams without
 * <a href="package-summary.html#StreamOps">stateful intermediate
 * operations</a>, parallel streams, pipeline evaluation is done in a single
 * pass that "jams" all the operations together.  For parallel streams with
 * stateful operations, execution is divided into segments, where each
 * stateful operations marks the end of a segment, and each segment is
 * evaluated separately and the result used as the input to the next
 * segment.  In all cases, the source data is not consumed until a terminal
 * operation begins.
 *
 * @param <E_IN>  type of input elements
 * @param <E_OUT> type of output elements
 * @param <S> type of the subclass implementing {@code BaseStream}
 * @since 1.8
 */

// 流水线的抽象基类，主要实现了BaseStream接口和PipelineHelper抽象类中的方法
abstract class AbstractPipeline<E_IN, E_OUT, S extends BaseStream<E_OUT, S>>
    extends PipelineHelper<E_OUT>
    implements BaseStream<E_OUT, S> {
    
    private static final String MSG_STREAM_LINKED = "stream has already been operated upon or closed";
    private static final String MSG_CONSUMED = "source already consumed or closed";
    
    /**
     * Backlink to the head of the pipeline chain (self if this is the source stage).
     */
    @SuppressWarnings("rawtypes")
    private final AbstractPipeline sourceStage;     // 流水线的源头阶段（常见的是HEAD）
    
    /**
     * The "upstream" pipeline, or null if this is the source stage.
     */
    @SuppressWarnings("rawtypes")
    private final AbstractPipeline previousStage;   // 链接到流水线的上一个阶段
    
    /**
     * The next stage in the pipeline, or null if this is the last stage.
     * Effectively final at the point of linking to the next pipeline.
     */
    @SuppressWarnings("rawtypes")
    private AbstractPipeline nextStage;             // 链接到流水线的下一个阶段
    
    /**
     * The number of intermediate operations between this pipeline object
     * and the stream source if sequential, or the previous stateful if parallel.
     * Valid at the point of pipeline preparation for evaluation.
     */
    private int depth;                              // 当前的流是第几个阶段（HEAD是第0个阶段）
    
    /**
     * The operation flags for the intermediate operation represented by this pipeline object.
     */
    protected final int sourceOrOpFlags;            // 流的中间阶段的操作标志
    
    /**
     * The combined source and operation flags for the source and all operations
     * up to and including the operation represented by this pipeline object.
     * Valid at the point of pipeline preparation for evaluation.
     */
    private int combinedFlags;                      // 流的组合操作标志
    
    /**
     * The source spliterator. Only valid for the head pipeline.
     * Before the pipeline is consumed if non-null then {@code sourceSupplier} must be null.
     * After the pipeline is consumed if non-null then is set to null.
     */
    // 源头阶段的Spliterator，仅在流的源头阶段生效，消费后置空
    private Spliterator<?> sourceSpliterator;
    
    /**
     * The source supplier. Only valid for the head pipeline.
     * Before the pipeline is consumed if non-null then {@code sourceSpliterator} must be null.
     * After the pipeline is consumed if non-null then is set to null.
     */
    // 源头阶段的Spliterator的提供商，仅在流的源头阶段生效，消费后置空
    private Supplier<? extends Spliterator<?>> sourceSupplier;  // 比较少见
    
    /**
     * True if this pipeline has been linked or consumed
     */
    private boolean linkedOrConsumed;
    
    /**
     * True if there are any stateful ops in the pipeline; only valid for the
     * source stage.
     */
    private boolean sourceAnyStateful;
    
    private Runnable sourceCloseAction;
    
    /**
     * True if pipeline is parallel, otherwise the pipeline is sequential; only
     * valid for the source stage.
     */
    private boolean parallel;   // 是否并行执行
    
    
    
    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source      {@code Supplier<Spliterator>} describing the stream source
     * @param sourceFlags The source flags for the stream source, described in
     *                    {@link StreamOpFlag}
     * @param parallel    True if the pipeline is parallel
     */
    // 构造Stream，常用于构建流的源头阶段（HEAD），需要从source中提取Spliterator
    AbstractPipeline(Supplier<? extends Spliterator<?>> source, int sourceFlags, boolean parallel) {
        this.previousStage = null;
        this.sourceSupplier = source;
        this.sourceStage = this;
        this.sourceOrOpFlags = sourceFlags & StreamOpFlag.STREAM_MASK;
        // The following is an optimization of: StreamOpFlag.combineOpFlags(sourceOrOpFlags, StreamOpFlag.INITIAL_OPS_VALUE);
        this.combinedFlags = (~(sourceOrOpFlags << 1)) & StreamOpFlag.INITIAL_OPS_VALUE;
        this.depth = 0;
        this.parallel = parallel;
    }
    
    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source      {@code Spliterator} describing the stream source
     * @param sourceFlags the source flags for the stream source, described in
     *                    {@link StreamOpFlag}
     * @param parallel    {@code true} if the pipeline is parallel
     */
    // 构造Stream，常用于构建流的源头阶段（HEAD）
    AbstractPipeline(Spliterator<?> source, int sourceFlags, boolean parallel) {
        this.previousStage = null;
        this.sourceSpliterator = source;
        this.sourceStage = this;
        this.sourceOrOpFlags = sourceFlags & StreamOpFlag.STREAM_MASK;
        // The following is an optimization of: StreamOpFlag.combineOpFlags(sourceOrOpFlags, StreamOpFlag.INITIAL_OPS_VALUE);
        this.combinedFlags = (~(sourceOrOpFlags << 1)) & StreamOpFlag.INITIAL_OPS_VALUE;
        this.depth = 0;
        this.parallel = parallel;
    }
    
    /**
     * Constructor for appending an intermediate operation stage onto an
     * existing pipeline.
     *
     * @param previousStage the upstream pipeline stage
     * @param opFlags       the operation flags for the new stage, described in
     *                      {@link StreamOpFlag}
     */
    /*
     * 构造Stream，常用于构建流的中间阶段，包括有状态流和无状态流。
     * upstream是调用此构造方法的流
     */
    AbstractPipeline(AbstractPipeline<?, E_IN, ?> previousStage, int opFlags) {
        if(previousStage.linkedOrConsumed)
            throw new IllegalStateException(MSG_STREAM_LINKED);
        previousStage.linkedOrConsumed = true;
        previousStage.nextStage = this;
        
        this.previousStage = previousStage;
        this.sourceOrOpFlags = opFlags & StreamOpFlag.OP_MASK;
        this.combinedFlags = StreamOpFlag.combineOpFlags(opFlags, previousStage.combinedFlags);
        this.sourceStage = previousStage.sourceStage;
        if(opIsStateful())
            sourceStage.sourceAnyStateful = true;
        this.depth = previousStage.depth + 1;
    }
    
    
    
    /*▼ 用于流的终端阶段 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Collect the elements output from the pipeline stage.
     *
     * @param generator the array generator to be used to create array instances
     *
     * @return a flat array-backed Node that holds the collected output elements
     */
    @SuppressWarnings("unchecked")
    final Node<E_OUT> evaluateToArrayNode(IntFunction<E_OUT[]> generator) {
        if(linkedOrConsumed)
            throw new IllegalStateException(MSG_STREAM_LINKED);
        linkedOrConsumed = true;
        
        // If the last intermediate operation is stateful then
        // evaluate directly to avoid an extra collection step
        if(isParallel() && previousStage != null && opIsStateful()) {
            // Set the depth of this, last, pipeline stage to zero to slice the
            // pipeline such that this operation will not be included in the
            // upstream slice and upstream operations will not be included
            // in this slice
            depth = 0;
            return opEvaluateParallel(previousStage, previousStage.sourceSpliterator(0), generator);
        } else {
            return evaluate(sourceSpliterator(0), true, generator);
        }
    }
    
    /**
     * Evaluate the pipeline with a terminal operation to produce a result.
     *
     * @param <R>        the type of result
     * @param terminalOp the terminal operation to be applied to the pipeline.
     *
     * @return the result
     */
    //
    final <R> R evaluate(TerminalOp<E_OUT, R> terminalOp) {
        assert getOutputShape() == terminalOp.inputShape();
        
        if(linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        
        linkedOrConsumed = true;
        
        return isParallel()
            ? terminalOp.evaluateParallel(this, sourceSpliterator(terminalOp.getOpFlags()))
            : terminalOp.evaluateSequential(this, sourceSpliterator(terminalOp.getOpFlags()));
    }
    
    /**
     * Gets the source stage spliterator if this pipeline stage is the source
     * stage.  The pipeline is consumed after this method is called and
     * returns successfully.
     *
     * @return the source stage spliterator
     *
     * @throws IllegalStateException if this pipeline stage is not the source
     *                               stage.
     */
    @SuppressWarnings("unchecked")
    final Spliterator<E_OUT> sourceStageSpliterator() {
        if(this != sourceStage)
            throw new IllegalStateException();
        
        if(linkedOrConsumed)
            throw new IllegalStateException(MSG_STREAM_LINKED);
        linkedOrConsumed = true;
        
        if(sourceStage.sourceSpliterator != null) {
            @SuppressWarnings("unchecked")
            Spliterator<E_OUT> s = sourceStage.sourceSpliterator;
            sourceStage.sourceSpliterator = null;
            return s;
        } else if(sourceStage.sourceSupplier != null) {
            @SuppressWarnings("unchecked")
            Spliterator<E_OUT> s = (Spliterator<E_OUT>) sourceStage.sourceSupplier.get();
            sourceStage.sourceSupplier = null;
            return s;
        } else {
            throw new IllegalStateException(MSG_CONSUMED);
        }
    }
    
    /*▲ 用于流的终端阶段 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 实现BaseStream接口 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回流中元素的Spliterator（可分割的迭代器）
    @Override
    @SuppressWarnings("unchecked")
    public Spliterator<E_OUT> spliterator() {
        if(linkedOrConsumed)
            throw new IllegalStateException(MSG_STREAM_LINKED);
        linkedOrConsumed = true;
        
        if(this == sourceStage) {
            if(sourceStage.sourceSpliterator != null) {
                @SuppressWarnings("unchecked")
                Spliterator<E_OUT> s = (Spliterator<E_OUT>) sourceStage.sourceSpliterator;
                sourceStage.sourceSpliterator = null;
                return s;
            } else if(sourceStage.sourceSupplier != null) {
                @SuppressWarnings("unchecked")
                Supplier<Spliterator<E_OUT>> s = (Supplier<Spliterator<E_OUT>>) sourceStage.sourceSupplier;
                sourceStage.sourceSupplier = null;
                return lazySpliterator(s);
            } else {
                throw new IllegalStateException(MSG_CONSUMED);
            }
        } else {
            return wrap(this, () -> sourceSpliterator(0), isParallel());
        }
    }
    
    // 是否需要并行处理
    @Override
    public final boolean isParallel() {
        return sourceStage.parallel;
    }
    
    // 中间操作，返回顺序的等效流。
    @Override
    @SuppressWarnings("unchecked")
    public final S sequential() {
        sourceStage.parallel = false;
        return (S) this;
    }
    
    // 中间操作，返回并行的等效流。
    @Override
    @SuppressWarnings("unchecked")
    public final S parallel() {
        sourceStage.parallel = true;
        return (S) this;
    }
    
    // 中间操作，返回附加close操作的等效流，可能会返回自身。
    @Override
    @SuppressWarnings("unchecked")
    public S onClose(Runnable closeHandler) {
        if(linkedOrConsumed)
            throw new IllegalStateException(MSG_STREAM_LINKED);
        Objects.requireNonNull(closeHandler);
        Runnable existingHandler = sourceStage.sourceCloseAction;
        sourceStage.sourceCloseAction = (existingHandler == null)
            ? closeHandler
            : Streams.composeWithExceptions(existingHandler, closeHandler);
        return (S) this;
    }
    
    // 关闭此流，这将导致调用此流水线的所有关闭处理程序。
    @Override
    public void close() {
        linkedOrConsumed = true;
        sourceSupplier = null;
        sourceSpliterator = null;
        if(sourceStage.sourceCloseAction != null) {
            Runnable closeAction = sourceStage.sourceCloseAction;
            sourceStage.sourceCloseAction = null;
            closeAction.run();
        }
    }
    
    /*▲ 实现BaseStream接口 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 实现PipelineHelper抽象类 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取流的源头阶段的形状
    @Override
    final StreamShape getSourceShape() {
        @SuppressWarnings("rawtypes")
        AbstractPipeline p = AbstractPipeline.this;
        while(p.depth > 0) {
            p = p.previousStage;
        }
        
        // 返回流的形状
        return p.getOutputShape();
    }
    
    // 返回流的组合操作标志
    @Override
    final int getStreamAndOpFlags() {
        return combinedFlags;
    }
    
    // 返回输出的元素数量，如果未知或无穷，则返回-1
    @Override
    final <P_IN> long exactOutputSizeIfKnown(Spliterator<P_IN> spliterator) {
        return StreamOpFlag.SIZED.isKnown(getStreamAndOpFlags()) ? spliterator.getExactSizeIfKnown() : -1;
    }
    
    // 从后往前包装sink的同时，从前到后择取数据
    @Override
    final <P_IN, SINK extends Sink<E_OUT>> SINK wrapAndCopyInto(SINK sink, Spliterator<P_IN> spliterator) {
        Objects.requireNonNull(sink);
        
        // 从终端的Sink开始，逐段向前包装Sink形成一个单链表，然后将最靠前的sink返回
        Sink<P_IN> wrappedSink = wrapSink(sink);
        
        // 从第一个中间阶段开始择取数据
        copyInto(wrappedSink, spliterator);
        
        // 这里返回的还是形参中未经过包装的sink，即终端的sink
        return sink;
    }
    
    // 从终端的Sink开始，逐段向前包装Sink形成一个单链表，然后将最靠前的sink返回
    @Override
    @SuppressWarnings("unchecked")
    final <P_IN> Sink<P_IN> wrapSink(Sink<E_OUT> sink) {
        Objects.requireNonNull(sink);
        
        /*
         * 举例：筛选出所有偶数，求平方，最后存入数组
         * Stream.of(1, 2, 3, 4).filter(i -> i % 2 == 0).map(x->x*x).toArray()
         * 这里有一个源头阶段：of，两个中间阶段filter和map，一个终端阶段toArray
         * 一开始，形参中的sink代表toArray阶段的sink，称作toArray-sink，而p代表终端阶段前一个阶段的流，这里是map流。
         * 通过不断调用opWrapSink，将在p阶段生成一个新的sink，该sink持有了指向下一个阶段的引用
         * 最终，该循环生成了两个新的sink：filter-sink和map-sink，且返回的是最靠前的sink。
         */
        for(@SuppressWarnings("rawtypes") AbstractPipeline p = AbstractPipeline.this; p.depth > 0; p = p.previousStage) {
            sink = p.opWrapSink(p.previousStage.combinedFlags, sink);
        }
        
        // 返回包装后的sink，该sink持有流的下一个阶段的sink，所有sink形成了一个单链表
        return (Sink<P_IN>) sink;
    }
    
    /*
     * 从HEAD阶段之后开始择取数据
     *
     * 会调用源头阶段的Spliterator的forEachRemaining方法（如果重写不当可能会陷入死循环）
     * 还会依次调用sink链条上的begin、accept、end方法
     *
     * wrappedSink 第一个中间阶段的Sink
     * spliterator 源头阶段的Spliterator
     */
    @Override
    final <P_IN> void copyInto(Sink<P_IN> wrappedSink, Spliterator<P_IN> spliterator) {
        Objects.requireNonNull(wrappedSink);
        
        // 非短路操作
        if(!StreamOpFlag.SHORT_CIRCUIT.isKnown(getStreamAndOpFlags())) {
            long size = spliterator.getExactSizeIfKnown();
            
            wrappedSink.begin(size);    // 从第一个中间阶段开始，依次调用整个Sink链条上begin，激活流
            spliterator.forEachRemaining(wrappedSink);  // 遍历容器内每个元素，在其上执行相应的择取操作
            wrappedSink.end();          // 从第一个中间阶段开始，依次调用整个Sink链条上end，关闭流
        } else {
            // 短路操作
            copyIntoWithCancel(wrappedSink, spliterator);
        }
    }
    
    /*
     * 从HEAD阶段之后开始择取数据，存在短路操作（即满足某种条件就终止择取）
     *
     * wrappedSink 第一个中间阶段的Sink
     * spliterator 源头阶段的Spliterator
     */
    @Override
    @SuppressWarnings("unchecked")
    final <P_IN> boolean copyIntoWithCancel(Sink<P_IN> wrappedSink, Spliterator<P_IN> spliterator) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        AbstractPipeline p = AbstractPipeline.this;
        while(p.depth > 0) {
            p = p.previousStage;    // 找到流的源头阶段HEAD
        }
        
        // 通过源头阶段的Spliterator中获取当前情境中的元素数量（精确值）
        long size = spliterator.getExactSizeIfKnown();
        
        // 从第一个中间阶段开始，依次调用整个Sink链条上begin，激活流
        wrappedSink.begin(size);
        
        /*
         * 逐个择取元素，每次择取之前都要先判断是否应当停止接收数据
         * 当不需要再择取数据，或者已择取完所有数据时，退出择取，返回false
         *
         * 比如判断一个序列中是否存在大于0的元素，那么只要发现有一个元素大于0，那么就立即停止择取
         */
        boolean cancelled = p.forEachWithCancel(spliterator, wrappedSink);
        
        // 从第一个中间阶段开始，依次调用整个Sink链条上end，关闭流
        wrappedSink.end();
        
        return cancelled;
    }
    
    /**
     * Make a node builder compatible with this stream shape.
     *
     * @param exactSizeIfKnown if {@literal >=0}, then a node builder will be
     *                         created that has a fixed capacity of at most sizeIfKnown elements. If
     *                         {@literal < 0}, then the node builder has an unfixed capacity. A fixed
     *                         capacity node builder will throw exceptions if an element is added after
     *                         builder has reached capacity, or is built before the builder has reached
     *                         capacity.
     * @param generator        the array generator to be used to create instances of a
     *                         T[] array. For implementations supporting primitive nodes, this parameter
     *                         may be ignored.
     *
     * @return a node builder
     */
    // 返回第(3)、(4)类Node（固定长度Node和可变长度Node）
    @Override
    abstract Node.Builder<E_OUT> makeNodeBuilder(long exactSizeIfKnown, IntFunction<E_OUT[]> generator);
    
    // 利用Sink链中定义的操作择取数据
    @Override
    @SuppressWarnings("unchecked")
    final <P_IN> Node<E_OUT> evaluate(Spliterator<P_IN> spliterator, boolean flatten, IntFunction<E_OUT[]> generator) {
        if(isParallel()) {
            // 并行收集元素。如果流水线上存在有状态的中间操作，则优化计算过程
            return evaluateToNode(this, spliterator, flatten, generator);
        } else {
            /*
             * 返回第(3)、(4)类Node（固定长度Node和可变长度Node）
             *
             * ★ 这些类型的Node兼具Node和Node.Builder属性（实现了Sink接口）
             */
            Node.Builder<E_OUT> nb = makeNodeBuilder(exactOutputSizeIfKnown(spliterator), generator);
            
            /*
             * 这里的sink与上面的nd指向相同的对象。
             * 但是，经过wrapAndCopyInto调用后，会完成sink的包装和数据的择取
             */
            Node.Builder<E_OUT> sink = wrapAndCopyInto(nb, spliterator);
            
            // 返回由Builder构建的Node（返回自身）
            return sink.build();
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    final <P_IN> Spliterator<E_OUT> wrapSpliterator(Spliterator<P_IN> sourceSpliterator) {
        if(depth == 0) {
            return (Spliterator<E_OUT>) sourceSpliterator;
        } else {
            return wrap(this, () -> sourceSpliterator, isParallel());
        }
    }
    
    /*▲ 实现PipelineHelper抽象类 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Get the output shape of the pipeline.  If the pipeline is the head,
     * then it's output shape corresponds to the shape of the source.
     * Otherwise, it's output shape corresponds to the output shape of the
     * associated operation.
     *
     * @return the output shape
     */
    // 返回流的形状
    abstract StreamShape getOutputShape();
    
    /**
     * Traverse the elements of a spliterator compatible with this stream shape,
     * pushing those elements into a sink.   If the sink requests cancellation,
     * no further elements will be pulled or pushed.
     *
     * @param spliterator the spliterator to pull elements from
     * @param sink        the sink to push elements to
     *
     * @return true if the cancellation was requested
     */
    // 逐个择取元素，每次择取之前都要先判断是否应当停止接收数据
    abstract boolean forEachWithCancel(Spliterator<E_OUT> spliterator, Sink<E_OUT> sink);
    
    /**
     * Collect elements output from a pipeline into a Node that holds elements of this shape.
     *
     * @param helper      the pipeline helper describing the pipeline stages
     * @param spliterator the source spliterator
     * @param flattenTree true if the returned node should be flattened
     * @param generator   the array generator
     *
     * @return a Node holding the output of the pipeline
     */
    // 并行收集元素。如果流水线上存在有状态的中间操作，则优化计算过程
    abstract <P_IN> Node<E_OUT> evaluateToNode(PipelineHelper<E_OUT> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<E_OUT[]> generator);
    
    /**
     * Create a spliterator that wraps a source spliterator, compatible with
     * this stream shape, and operations associated with a {@link
     * PipelineHelper}.
     *
     * @param ph       the pipeline helper describing the pipeline stages
     * @param supplier the supplier of a spliterator
     *
     * @return a wrapping spliterator compatible with this shape
     */
    abstract <P_IN> Spliterator<E_OUT> wrap(PipelineHelper<E_OUT> ph, Supplier<Spliterator<P_IN>> supplier, boolean isParallel);
    
    /**
     * Create a lazy spliterator that wraps and obtains the supplied the
     * spliterator when a method is invoked on the lazy spliterator.
     *
     * @param supplier the supplier of a spliterator
     */
    abstract Spliterator<E_OUT> lazySpliterator(Supplier<? extends Spliterator<E_OUT>> supplier);
    
    
    
    /**
     * Get the source spliterator for this pipeline stage.
     * For a sequential or stateless parallel pipeline, this is the source spliterator.
     * For a stateful parallel pipeline, this is a spliterator describing the results of all computations up to and including the most recent stateful operation.
     */
    // 获取源头阶段的Spliterator
    @SuppressWarnings("unchecked")
    private Spliterator<?> sourceSpliterator(int terminalFlags) {
        // Get the source spliterator of the pipeline
        Spliterator<?> spliterator;
        
        if(sourceStage.sourceSpliterator != null) { // 直接查找
            spliterator = sourceStage.sourceSpliterator;
            sourceStage.sourceSpliterator = null;
        } else if(sourceStage.sourceSupplier != null) { // 间接查找
            spliterator = (Spliterator<?>) sourceStage.sourceSupplier.get();
            sourceStage.sourceSupplier = null;
        } else {
            throw new IllegalStateException(MSG_CONSUMED);
        }
        
        if(isParallel() && sourceStage.sourceAnyStateful) {
            // Adapt the source spliterator, evaluating each stateful op
            // in the pipeline up to and including this pipeline stage.
            // The depth and flags of each pipeline stage are adjusted accordingly.
            int depth = 1;
            for(
                @SuppressWarnings("rawtypes")
                AbstractPipeline u = sourceStage, p = sourceStage.nextStage, e = this; u != e; u = p, p = p.nextStage) {
                
                int thisOpFlags = p.sourceOrOpFlags;
                if(p.opIsStateful()) {
                    depth = 0;
                    
                    if(StreamOpFlag.SHORT_CIRCUIT.isKnown(thisOpFlags)) {
                        // Clear the short circuit flag for next pipeline stage
                        // This stage encapsulates short-circuiting, the next
                        // stage may not have any short-circuit operations, and
                        // if so spliterator.forEachRemaining should be used
                        // for traversal
                        thisOpFlags = thisOpFlags & ~StreamOpFlag.IS_SHORT_CIRCUIT;
                    }
                    
                    spliterator = p.opEvaluateParallelLazy(u, spliterator);
                    
                    // Inject or clear SIZED on the source pipeline stage
                    // based on the stage's spliterator
                    thisOpFlags = spliterator.hasCharacteristics(Spliterator.SIZED) ? (thisOpFlags & ~StreamOpFlag.NOT_SIZED) | StreamOpFlag.IS_SIZED : (thisOpFlags & ~StreamOpFlag.IS_SIZED) | StreamOpFlag.NOT_SIZED;
                }
                p.depth = depth++;
                p.combinedFlags = StreamOpFlag.combineOpFlags(thisOpFlags, u.combinedFlags);
            }
        }
        
        if(terminalFlags != 0) {
            // Apply flags from the terminal operation to last pipeline stage
            combinedFlags = StreamOpFlag.combineOpFlags(terminalFlags, combinedFlags);
        }
        
        return spliterator;
    }
    
    /**
     * Returns the composition of stream flags of the stream source and all
     * intermediate operations.
     *
     * @return the composition of stream flags of the stream source and all
     * intermediate operations
     *
     * @see StreamOpFlag
     */
    // 返回流的组合操作标志
    final int getStreamFlags() {
        return StreamOpFlag.toStreamFlags(combinedFlags);
    }
    
    final boolean isOrdered() {
        return StreamOpFlag.ORDERED.isKnown(combinedFlags);
    }
    
    /**
     * Returns whether this operation is stateful or not.  If it is stateful,
     * then the method
     * {@link #opEvaluateParallel(PipelineHelper, java.util.Spliterator, java.util.function.IntFunction)}
     * must be overridden.
     *
     * @return {@code true} if this operation is stateful
     */
    // 是否为有状态流
    abstract boolean opIsStateful();
    
    /**
     * Accepts a {@code Sink} which will receive the results of this operation,
     * and return a {@code Sink} which accepts elements of the input type of
     * this operation and which performs the operation, passing the results to
     * the provided {@code Sink}.
     *
     * @param flags The combined stream and operation flags up to, but not
     *              including, this operation
     * @param sink  sink to which elements should be sent after processing
     *
     * @return a sink which accepts elements, perform the operation upon
     * each element, and passes the results (if any) to the provided
     * {@code Sink}.
     *
     * @apiNote The implementation may use the {@code flags} parameter to optimize the
     * sink wrapping.  For example, if the input is already {@code DISTINCT},
     * the implementation for the {@code Stream#distinct()} method could just
     * return the sink it was passed.
     */
    // 完成Sink的链接。从后向前包装SInk，传入下一个阶段的Sink，连同当前阶段的Sink包装到一起再返回
    abstract Sink<E_IN> opWrapSink(int flags, Sink<E_OUT> sink);
    
    /**
     * Performs a parallel evaluation of the operation using the specified
     * {@code PipelineHelper} which describes the upstream intermediate
     * operations.  Only called on stateful operations.  If {@link
     * #opIsStateful()} returns true then implementations must override the
     * default implementation.
     *
     * @param helper      the pipeline helper describing the pipeline stages
     * @param spliterator the source {@code Spliterator}
     * @param generator   the array generator
     *
     * @return a {@code Node} describing the result of the evaluation
     *
     * @implSpec The default implementation always throw
     * {@code UnsupportedOperationException}.
     */
    <P_IN> Node<E_OUT> opEvaluateParallel(PipelineHelper<E_OUT> helper, Spliterator<P_IN> spliterator, IntFunction<E_OUT[]> generator) {
        throw new UnsupportedOperationException("Parallel evaluation is not supported");
    }
    
    /**
     * Returns a {@code Spliterator} describing a parallel evaluation of the
     * operation, using the specified {@code PipelineHelper} which describes the
     * upstream intermediate operations.  Only called on stateful operations.
     * It is not necessary (though acceptable) to do a full computation of the
     * result here; it is preferable, if possible, to describe the result via a
     * lazily evaluated spliterator.
     *
     * @param helper      the pipeline helper
     * @param spliterator the source {@code Spliterator}
     *
     * @return a {@code Spliterator} describing the result of the evaluation
     *
     * @implSpec The default implementation behaves as if:
     * <pre>{@code
     *     return evaluateParallel(helper, i -> (E_OUT[]) new
     * Object[i]).spliterator();
     * }</pre>
     * and is suitable for implementations that cannot do better than a full
     * synchronous evaluation.
     */
    @SuppressWarnings("unchecked")
    <P_IN> Spliterator<E_OUT> opEvaluateParallelLazy(PipelineHelper<E_OUT> helper, Spliterator<P_IN> spliterator) {
        return opEvaluateParallel(helper, spliterator, i -> (E_OUT[]) new Object[i]).spliterator();
    }
}
