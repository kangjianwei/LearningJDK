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
// 流的抽象基类，主要实现了BaseStream接口和PipelineHelper抽象类中的方法
abstract class AbstractPipeline<E_IN, E_OUT, S extends BaseStream<E_OUT, S>> extends PipelineHelper<E_OUT> implements BaseStream<E_OUT, S> {
    
    private static final String MSG_STREAM_LINKED = "stream has already been operated upon or closed";
    private static final String MSG_CONSUMED = "source already consumed or closed";
    
    /**
     * The source supplier. Only valid for the head pipeline.
     * Before the pipeline is consumed if non-null then {@code sourceSpliterator} must be null.
     * After the pipeline is consumed if non-null then is set to null.
     */
    /*
     * 源头(head)阶段的流迭代器工厂，被流的所有阶段共享
     *
     * 可以从sourceSupplier中获取到原始数据的流迭代器，进而通过流迭代器来访问原始数据
     *
     * 注：可以从sourceSupplier中获取到sourceSpliterator
     */
    private Supplier<? extends Spliterator<?>> sourceSupplier;
    
    /**
     * The source spliterator. Only valid for the head pipeline.
     * Before the pipeline is consumed if non-null then {@code sourceSupplier} must be null.
     * After the pipeline is consumed if non-null then is set to null.
     */
    /*
     * 源头(head)阶段的流迭代器，被流的所有阶段共享
     *
     * sourceSpliterator不是原始数据，而是原始数据的流迭代器，可以通过流迭代器来访问原始数据
     */
    private Spliterator<?> sourceSpliterator;
    
    /**
     * Backlink to the head of the pipeline chain (self if this is the source stage).
     */
    @SuppressWarnings("rawtypes")
    private final AbstractPipeline sourceStage;     // 流的源头(head)阶段的引用；源头(head)阶段和中间(intermediate)阶段都会持有该字段
    
    /**
     * The "upstream" pipeline, or null if this is the source stage.
     */
    @SuppressWarnings("rawtypes")
    private final AbstractPipeline previousStage;   // 链接到流的前一个阶段的引用；对于流的源头(head)阶段，该字段为null
    
    /**
     * The next stage in the pipeline, or null if this is the last stage.
     * Effectively final at the point of linking to the next pipeline.
     */
    @SuppressWarnings("rawtypes")
    private AbstractPipeline nextStage;             // 链接到流的下一个阶段的引用；对于流的最后一个中间(intermediate)阶段，该字段为null
    
    /**
     * True if this pipeline has been linked or consumed
     */
    private boolean linkedOrConsumed;               // 指示当前阶段是否与下一个阶段建立了链接
    
    /**
     * The operation flags for the intermediate operation represented by this pipeline object.
     */
    protected final int sourceOrOpFlags;            // 源头阶段的流参数，或者是中间阶段的操作参数
    
    /**
     * The combined source and operation flags for the source and all operations
     * up to and including the operation represented by this pipeline object.
     * Valid at the point of pipeline preparation for evaluation.
     */
    private int combinedFlags;                      // 当前流阶段的组合参数
    
    /**
     * True if pipeline is parallel, otherwise the pipeline is sequential; only
     * valid for the source stage.
     */
    private boolean parallel;                       // 是否并行执行
    
    /**
     * The number of intermediate operations between this pipeline object
     * and the stream source if sequential, or the previous stateful if parallel.
     * Valid at the point of pipeline preparation for evaluation.
     */
    /*
     * 在顺序流中，指示当前阶段属于流的第几个阶段，源头(head)阶段被视为第【0】个阶段。
     * 在并行流中，指示从上一个有状态的阶段算起，当前是第几个阶段。
     *
     * 举例：
     * H -> [A] -> B -> C -> [D] -> E -> F -> G -> T
     * 其中，H是源头阶段，T是终端阶段，其他是中间阶段。带中括号的阶段表示有状态的阶段。
     *
     * 在顺序流中，其depth编号为：
     * H -> [A] -> B -> C -> [D] -> E -> F -> G -> T
     * 0     1     2    3     4     5    6    7
     *
     * 在并行流中，初始时，其depth编号与顺序流一致，即：
     * H -> [A] -> B -> C -> [D] -> E -> F -> G -> T
     * 0     1     2    3     4     5    6    7
     * 在访问了C阶段的流后，其编号变为：
     * H -> [A] -> B -> C -> [D] -> E -> F -> G -> T
     * 0     0     1    2     4     5    6    7
     * 在访问了G阶段的流后，其编号变为：
     * H -> [A] -> B -> C -> [D] -> E -> F -> G -> T
     * 0     0     1    2     0     1    2    3
     */
    private int depth;
    
    /**
     * True if there are any stateful ops in the pipeline; only valid for the
     * source stage.
     */
    private boolean sourceAnyStateful;              // 指示当前流是否有状态：只要流的任一中间阶段有状态，则标记整个流为有状态流
    
    private Runnable sourceCloseAction;             // 关闭流时候的回调
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source      {@code Supplier<Spliterator>} describing the stream source
     * @param sourceFlags The source flags for the stream source, described in {@link StreamOpFlag}
     * @param parallel    True if the pipeline is parallel
     */
    /*
     * 构造流的源头(head)阶段
     *
     * source     : 数据源迭代器工厂；可以从source中获取到原始数据的流迭代器，进而通过流迭代器来访问原始数据
     * sourceFlags: 源头阶段的流参数
     * parallel   : 是否为并行流
     */
    AbstractPipeline(Supplier<? extends Spliterator<?>> source, int sourceFlags, boolean parallel) {
        
        // 数据源工厂
        this.sourceSupplier = source;
        
        // 记录流的源头(head)阶段的引用
        this.sourceStage = this;
        
        // 初始化流的前一个阶段的引用为null
        this.previousStage = null;
        
        // 记录源头阶段的流参数
        this.sourceOrOpFlags = sourceFlags & StreamOpFlag.STREAM_MASK;
        
        /* The following is an optimization of: StreamOpFlag.combineOpFlags(sourceOrOpFlags, StreamOpFlag.INITIAL_OPS_VALUE); */
        /*
         * 向sourceOrOpFlags中缺失数据的地方填充"11"(以每2个bit为一组去考察)
         * 注：这是StreamOpFlag.combineOpFlags(sourceOrOpFlags, StreamOpFlag.INITIAL_OPS_VALUE)的优化形式。
         */
        this.combinedFlags = (~(sourceOrOpFlags << 1)) & StreamOpFlag.INITIAL_OPS_VALUE;
        
        this.parallel = parallel;
        
        // 源头(head)阶段被视为流的第0个阶段
        this.depth = 0;
    }
    
    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source      {@code Spliterator} describing the stream source
     * @param sourceFlags the source flags for the stream source, described in {@link StreamOpFlag}
     * @param parallel    {@code true} if the pipeline is parallel
     */
    /*
     * 构造流的源头(head)阶段
     *
     * source     : 数据源迭代器；source不是原始数据，而是原始数据的流迭代器，可以通过流迭代器来访问原始数据
     * sourceFlags: 源头阶段的流参数
     * parallel   : 是否为并行流
     */
    AbstractPipeline(Spliterator<?> source, int sourceFlags, boolean parallel) {
        
        // 数据源
        this.sourceSpliterator = source;
        
        // 记录流的源头(head)阶段的引用
        this.sourceStage = this;
        
        // 初始化流的前一个阶段的引用为null
        this.previousStage = null;
        
        // 记录源头阶段的流参数
        this.sourceOrOpFlags = sourceFlags & StreamOpFlag.STREAM_MASK;
        
        /* The following is an optimization of: StreamOpFlag.combineOpFlags(sourceOrOpFlags, StreamOpFlag.INITIAL_OPS_VALUE); */
        /*
         * 向sourceOrOpFlags中缺失数据的地方填充"11"(以每2个bit为一组去考察)
         * 注：这是StreamOpFlag.combineOpFlags(sourceOrOpFlags, StreamOpFlag.INITIAL_OPS_VALUE)的优化形式。
         */
        this.combinedFlags = (~(sourceOrOpFlags << 1)) & StreamOpFlag.INITIAL_OPS_VALUE;
        
        this.parallel = parallel;
        
        // 源头(head)阶段被视为流的第0个阶段
        this.depth = 0;
    }
    
    /**
     * Constructor for appending an intermediate operation stage onto an existing pipeline.
     *
     * @param upstream the upstream pipeline stage
     * @param opFlags  the operation flags for the new stage, described in {@link StreamOpFlag}
     */
    /*
     * 构造流的中间(intermediate)阶段，该阶段持有流的前一个阶段的引用
     *
     * upstream: 链接到流的前一个阶段的引用
     * opFlags : 中间阶段的流的操作参数
     */
    AbstractPipeline(AbstractPipeline<?, E_IN, ?> upstream, int opFlags) {
        if(upstream.linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        
        // 标记upstream已经与下一个阶段建立了链接
        upstream.linkedOrConsumed = true;
        
        // 将当前阶段的引用记录到上一个阶段中
        upstream.nextStage = this;
        
        // 流中每个中间阶段都会记录流的源头(head)阶段的引用
        this.sourceStage = upstream.sourceStage;
        
        // 记录流的前一个阶段的引用
        this.previousStage = upstream;
        
        // 记录中间阶段的流的操作参数
        this.sourceOrOpFlags = opFlags & StreamOpFlag.OP_MASK;
        
        // 从upstream.combinedFlags中提取出在opFlags中缺失的数据位，并将其补充到opFlags上后返回
        this.combinedFlags = StreamOpFlag.combineOpFlags(opFlags, upstream.combinedFlags);
        
        // 如果当前阶段是有状态的，则标记整个流为有状态的流
        if(opIsStateful()) {
            sourceStage.sourceAnyStateful = true;
        }
        
        // 阶段数递增
        this.depth = upstream.depth + 1;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回当前阶段的流的流迭代器；如果遇到并行流的有状态的中间阶段，则需要特殊处理
    @Override
    @SuppressWarnings("unchecked")
    public Spliterator<E_OUT> spliterator() {
        if(linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        
        // 指示当前阶段已经与下一个阶段建立了链接
        linkedOrConsumed = true;
        
        // 如果当前阶段不是流的源头阶段，则需要间接获取当前阶段的流的流迭代器
        if(this != sourceStage) {
            
            // 获取上个(depth==0)的流阶段的流迭代器
            Spliterator<?> spliterator = sourceSpliterator(0);
            
            /*
             * 构造一个"包装"流迭代器，使用该流迭代器可以获取当前流阶段的输出元素。
             *
             * this       : 当前流阶段
             * spliterator: 相对于当前流阶段的上个(depth==0)的流阶段的流迭代器
             */
            return wrap(this, () -> spliterator, isParallel());
        }
        
        /*
         * 如果当前阶段是流的源头阶段，那么就可以就直接操作源头阶段的流迭代器了
         */
        
        // 源头阶段存在流迭代器，直接返回流迭代器
        if(sourceStage.sourceSpliterator != null) {
            @SuppressWarnings("unchecked")
            Spliterator<E_OUT> spliterator = (Spliterator<E_OUT>) sourceStage.sourceSpliterator;
            sourceStage.sourceSpliterator = null;
            return spliterator;
            
            // 源头阶段存在流迭代器工厂，则需要对该工厂进行包装，返回一个"惰性"流迭代器
        } else if(sourceStage.sourceSupplier != null) {
            @SuppressWarnings("unchecked")
            Supplier<Spliterator<E_OUT>> supplier = (Supplier<Spliterator<E_OUT>>) sourceStage.sourceSupplier;
            sourceStage.sourceSupplier = null;
            
            /*
             * 构造一个"惰性"流迭代器。
             * "惰性"的含义是使用流迭代器时，需要从流迭代器工厂中获取。
             */
            return lazySpliterator(supplier);
        } else {
            throw new IllegalStateException(MSG_CONSUMED);
        }
    }
    
    /**
     * Gets the source stage spliterator if this pipeline stage is the source stage.
     * The pipeline is consumed after this method is called and returns successfully.
     *
     * @return the source stage spliterator
     *
     * @throws IllegalStateException if this pipeline stage is not the source stage.
     */
    // 返回源头阶段的流迭代器；该方法只能由源头阶段的流调用
    @SuppressWarnings("unchecked")
    final Spliterator<E_OUT> sourceStageSpliterator() {
        // 如果当前阶段不是流的源头阶段，则抛出异常
        if(this != sourceStage) {
            throw new IllegalStateException();
        }
    
        if(linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
    
        // 指示当前阶段已经与下一个阶段建立了链接
        linkedOrConsumed = true;
    
        // 源头阶段存在流迭代器，直接返回流迭代器
        if(sourceStage.sourceSpliterator != null) {
            @SuppressWarnings("unchecked")
            Spliterator<E_OUT> spliterator = sourceStage.sourceSpliterator;
            sourceStage.sourceSpliterator = null;
            return spliterator;
        
            // 源头阶段存在流迭代器工厂，则需要从该工厂中提取流迭代器
        } else if(sourceStage.sourceSupplier != null) {
            @SuppressWarnings("unchecked")
            Spliterator<E_OUT> spliterator = (Spliterator<E_OUT>) sourceStage.sourceSupplier.get();
            sourceStage.sourceSupplier = null;
            return spliterator;
        } else {
            throw new IllegalStateException(MSG_CONSUMED);
        }
    }
    
    /**
     * Get the source spliterator for this pipeline stage.
     * For a sequential or stateless parallel pipeline, this is the source spliterator.
     * For a stateful parallel pipeline, this is a spliterator describing the results of all computations up to and including the most recent stateful operation.
     */
    /*
     * 返回上个(depth==0)的流阶段的流迭代器
     *
     * terminalFlags: 为终端阶段设置的组合参数。
     *                如果terminalFlags有效，还需要为其组合当前阶段的参数。
     */
    @SuppressWarnings("unchecked")
    private Spliterator<?> sourceSpliterator(int terminalFlags) {
        
        // (depth==0)的流阶段的流迭代器
        Spliterator<?> spliterator;
        
        // 如果源头阶段存在流迭代器，则直接记下该流迭代器
        if(sourceStage.sourceSpliterator != null) {
            spliterator = sourceStage.sourceSpliterator;
            sourceStage.sourceSpliterator = null;
            
            // 如果源头阶段存在流迭代器工厂，则需要从其中提取出流迭代器
        } else if(sourceStage.sourceSupplier != null) {
            spliterator = (Spliterator<?>) sourceStage.sourceSupplier.get();
            sourceStage.sourceSupplier = null;
        } else {
            throw new IllegalStateException(MSG_CONSUMED);
        }
        
        // 如果当前流需要并行执行，且当前流是有状态的，则需要更新某些阶段的depth属性
        if(isParallel() && sourceStage.sourceAnyStateful) {
            
            /*
             * Adapt the source spliterator, evaluating each stateful op
             * in the pipeline up to and including this pipeline stage.
             * The depth and flags of each pipeline stage are adjusted accordingly.
             */
            int depth = 1;
            
            @SuppressWarnings("rawtypes")
            AbstractPipeline pre = sourceStage;
            
            @SuppressWarnings("rawtypes")
            AbstractPipeline next = sourceStage.nextStage;
            
            @SuppressWarnings("rawtypes")
            AbstractPipeline cur = this;
            
            // 从源头阶段开始，从前到后遍历流的各个阶段，直到遇到当前阶段则退出循环
            while(pre != cur) {
                
                // 获取next阶段的操作参数
                int thisOpFlags = next.sourceOrOpFlags;
                
                // 如果next阶段是有状态的
                if(next.opIsStateful()) {
                    
                    // 重置深度为0
                    depth = 0;
                    
                    // next阶段是一个短路操作
                    if(StreamOpFlag.SHORT_CIRCUIT.isKnown(thisOpFlags)) {
                        /*
                         * Clear the short circuit flag for next pipeline stage.
                         * This stage encapsulates short-circuiting,
                         * the next stage may not have any short-circuit operations,
                         * and if so spliterator.forEachRemaining should be used for traversal
                         */
                        // 移除SHORT_CIRCUIT参数
                        thisOpFlags = thisOpFlags & ~StreamOpFlag.IS_SHORT_CIRCUIT;
                    }
                    
                    /*
                     * 返回next流阶段的流迭代器，其中存储了next流阶段的输出数据
                     *
                     * helper     : 上个流阶段
                     * spliterator: 上个(depth==0)的流阶段的流迭代器
                     *
                     * 注1：该方法仅用在并行流的中间阶段
                     * 注2：后续会将next阶段的depth设置为0，即spliterator永远存储的是(depth==0)的流阶段的数据
                     */
                    spliterator = next.opEvaluateParallelLazy(pre, spliterator);
                    
                    // 判断next阶段的元素数量是否为有限大小
                    boolean limited = spliterator.hasCharacteristics(Spliterator.SIZED);
                    
                    /* Inject or clear SIZED on the source pipeline stage based on the stage's spliterator */
                    if(limited) {
                        // 添加SIZED参数
                        thisOpFlags = (thisOpFlags & ~StreamOpFlag.NOT_SIZED) | StreamOpFlag.IS_SIZED;
                    } else {
                        // 移除SIZED参数
                        thisOpFlags = (thisOpFlags & ~StreamOpFlag.IS_SIZED) | StreamOpFlag.NOT_SIZED;
                    }
                }
                
                // 更新next流阶段的depth
                next.depth = depth++;
                
                // 从pre.combinedFlags中提取出在thisOpFlags中缺失的数据位，并将其补充到thisOpFlags上后返回
                next.combinedFlags = StreamOpFlag.combineOpFlags(thisOpFlags, pre.combinedFlags);
                
                // 更新到下个阶段
                pre = next;
                next = next.nextStage;
            }
        }
        
        /* Apply flags from the terminal operation to last pipeline stage */
        if(terminalFlags != 0) {
            // 从combinedFlags中提取出在terminalFlags中缺失的数据位，并将其补充到terminalFlags上后返回
            combinedFlags = StreamOpFlag.combineOpFlags(terminalFlags, combinedFlags);
        }
        
        return spliterator;
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
    @SuppressWarnings("unchecked")
    <P_IN> Spliterator<E_OUT> opEvaluateParallelLazy(PipelineHelper<E_OUT> helper, Spliterator<P_IN> spliterator) {
        Node<E_OUT> node = opEvaluateParallel(helper, spliterator, size -> (E_OUT[]) new Object[size]);
        
        return node.spliterator();
    }
    
    /*
     * 返回一个"包装"流迭代器，使用该流迭代器可以获取当前流阶段的输出元素。
     *
     * sourceSpliterator: 相对于当前流阶段的上个(depth==0)的流阶段的流迭代器
     */
    @Override
    @SuppressWarnings("unchecked")
    final <P_IN> Spliterator<E_OUT> wrapSpliterator(Spliterator<P_IN> sourceSpliterator) {
        
        // 如果当前流阶段已经满足depth==0，则直接返回sourceSpliterator
        if(depth == 0) {
            return (Spliterator<E_OUT>) sourceSpliterator;
        }
        
        /*
         * 构造一个"包装"流迭代器，使用该流迭代器可以获取当前流阶段的输出元素。
         *
         * this             : 当前流阶段
         * sourceSpliterator: 相对于当前流阶段的上个(depth==0)的流阶段的流迭代器
         */
        return wrap(this, () -> sourceSpliterator, isParallel());
    }
    
    /**
     * Create a lazy spliterator that wraps and obtains the supplied the
     * spliterator when a method is invoked on the lazy spliterator.
     *
     * @param supplier the supplier of a spliterator
     */
    /*
     * 构造一个"惰性"流迭代器
     *
     * "惰性"的含义是使用流迭代器时，需要从流迭代器工厂中获取
     */
    abstract Spliterator<E_OUT> lazySpliterator(Supplier<? extends Spliterator<E_OUT>> supplier);
    
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
    /*
     * 构造一个"包装"流迭代器，使用该流迭代器可以获取ph阶段的输出元素。
     *
     * ph        : 某个流阶段
     * supplier  : 相对于ph的上个(depth==0)的流阶段的流迭代器工厂
     * isParallel: ph所在流是否需要并行执行
     */
    abstract <P_IN> Spliterator<E_OUT> wrap(PipelineHelper<E_OUT> ph, Supplier<Spliterator<P_IN>> supplier, boolean isParallel);
    
    /*▲ 流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 中间操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 中间操作：将当前流设置为顺序流后返回
    @Override
    @SuppressWarnings("unchecked")
    public final S sequential() {
        sourceStage.parallel = false;
        return (S) this;
    }
    
    // 中间操作：将当前流设置为并行流后返回
    @Override
    @SuppressWarnings("unchecked")
    public final S parallel() {
        sourceStage.parallel = true;
        return (S) this;
    }
    
    /*▲ 中间操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终端操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Collect the elements output from the pipeline stage.
     *
     * @param generator the array generator to be used to create array instances
     *
     * @return a flat array-backed Node that holds the collected output elements
     */
    @SuppressWarnings("unchecked")
    final Node<E_OUT> evaluateToArrayNode(IntFunction<E_OUT[]> generator) {
        
        if(linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        
        linkedOrConsumed = true;
        
        /*
         * If the last intermediate operation is stateful
         * then evaluate directly to avoid an extra collection step
         */
        // 如果当前阶段是并行流的有状态的中间(操作)阶段
        if(isParallel() && previousStage != null && opIsStateful()) {
            /*
             * Set the depth of this, last, pipeline stage to zero to slice the
             * pipeline such that this operation will not be included in the
             * upstream slice and upstream operations will not be included in this slice
             */
            // 设置当前阶段的depth属性为0
            depth = 0;
            
            // 获取相对于previousStage的上个(depth==0)的流阶段的流迭代器
            Spliterator<E_IN> spliterator = previousStage.sourceSpliterator(0);
            
            
            return opEvaluateParallel(previousStage, spliterator, generator);
            
            
        } else {
            
            // 获取上个(depth==0)的流阶段的流迭代器
            Spliterator<?> spliterator = sourceSpliterator(0);
            
            /*
             * 将spliterator中的元素搜集到generator生成的数组中，然后将该数组封装到Node中返回。
             * 这个搜集过程会涉及到数据的择取过程，即数据流通常从上一个(depth==1)的流阶段的sink开始，经过整个sink链的筛选后，进入终端阶段的sink。
             *
             * 注：这里的flatten参数为true，即在并行操作中，如果中间生成的Node是树状Node，则需要将其转换为非树状Node后再返回。
             */
            return evaluate(spliterator, true, generator);
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
    // 执行一个终端操作，返回执行结果
    final <R> R evaluate(TerminalOp<E_OUT, R> terminalOp) {
        assert getOutputShape() == terminalOp.inputShape();
        
        if(linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        
        // 指示当前阶段已经与下一个阶段建立了链接
        linkedOrConsumed = true;
        
        // 获取当前终端操作上的组合参数
        int terminalFlags = terminalOp.getOpFlags();
        
        // 获取上个(depth==0)的流阶段的流迭代器
        Spliterator<?> spliterator = sourceSpliterator(terminalFlags);
        
        // 如果是并行流
        if(isParallel()) {
            /*
             * 使用terminalOp并行处理helper流阶段输出的元素，返回处理后的结果
             *
             * 为helper构造一个终端sink，并使用该终端sink对spliterator中的数据进行择取，返回最后的处理结果。
             * 返回值可能是收集到的元素，也可能只是对过滤后的元素的计数，还可能是其它定制化的结果。
             *
             * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
             * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
             */
            return terminalOp.evaluateParallel(this, spliterator);
        }
        
        /*
         * 同步处理helper流阶段输出的元素，返回处理后的结果
         *
         * 为helper构造一个终端sink，并使用该终端sink对spliterator中的数据进行择取，返回最后的处理结果。
         * 返回值可能是收集到的元素，也可能只是对过滤后的元素的计数，还可能是其它定制化的结果。
         *
         * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
         * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
         */
        return terminalOp.evaluateSequential(this, spliterator);
    }
    
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
    <P_IN> Node<E_OUT> opEvaluateParallel(PipelineHelper<E_OUT> helper, Spliterator<P_IN> spliterator, IntFunction<E_OUT[]> generator) {
        throw new UnsupportedOperationException("Parallel evaluation is not supported");
    }
    
    /*▲ 终端操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关闭 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 为当前流设置一个关闭回调，并返回当前流自身
    @Override
    @SuppressWarnings("unchecked")
    public S onClose(Runnable closeHandler) {
        Objects.requireNonNull(closeHandler);
        
        // 如果当前阶段与其他阶段之间有链接，则抛出异常
        if(linkedOrConsumed) {
            throw new IllegalStateException(MSG_STREAM_LINKED);
        }
        
        Runnable existingHandler = sourceStage.sourceCloseAction;
        
        // 如果关闭回调不存在，直接设置回调方法
        if(existingHandler == null) {
            sourceStage.sourceCloseAction = closeHandler;
            
            // 如果已经存在一个关闭回调，则组合两个Runnable以顺序执行
        } else {
            sourceStage.sourceCloseAction = Streams.composeWithExceptions(existingHandler, closeHandler);
        }
        
        return (S) this;
    }
    
    // 关闭此当前阶段的流，并执行关闭回调
    @Override
    public void close() {
        linkedOrConsumed = true;
        sourceSupplier = null;
        sourceSpliterator = null;
    
        Runnable closeAction = sourceStage.sourceCloseAction;
    
        // 如果存在注册的关闭回调，则将其执行一次，并清除该回调
        if(closeAction != null) {
            sourceStage.sourceCloseAction = null;
            closeAction.run();
        }
    }
    
    /*▲ 关闭 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断当前流是否有序
    final boolean isOrdered() {
        return StreamOpFlag.ORDERED.isKnown(combinedFlags);
    }
    
    // 判断当前流是否需要并行执行
    @Override
    public final boolean isParallel() {
        return sourceStage.parallel;
    }
    
    /**
     * Returns whether this operation is stateful or not.  If it is stateful,
     * then the method
     * {@link #opEvaluateParallel(PipelineHelper, java.util.Spliterator, java.util.function.IntFunction)}
     * must be overridden.
     *
     * @return {@code true} if this operation is stateful
     */
    // 判断流的当前阶段是否有状态
    abstract boolean opIsStateful();
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回(depth==0)阶段的流的形状
    @Override
    final StreamShape getSourceShape() {
        @SuppressWarnings("rawtypes")
        AbstractPipeline p = AbstractPipeline.this;
        while(p.depth>0) {
            p = p.previousStage;
        }
        
        // 返回流的形状
        return p.getOutputShape();
    }
    
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
    
    // 返回当前流阶段的组合参数
    @Override
    final int getStreamAndOpFlags() {
        return combinedFlags;
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
    // 返回流(STREAM)的参数(只返回包含"01"的位)
    final int getStreamFlags() {
        // 从组合参数中提取出属于流(STREAM)的参数，且只提取包含"01"的位
        return StreamOpFlag.toStreamFlags(combinedFlags);
    }
    
    /*▲ 参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 从downSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
     * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
     * 最终择取出的数据往往被存入了downSink代表的容器当中。
     *
     * downSink   : 下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
     * spliterator: 流迭代器，作为数据源，包含了当前所有待访问的元素
     *
     * 注：返回值仍是入参中的downSink，但经过上面的择取操作后，可以进一步从该downSink中获取想要的数据。
     */
    @Override
    final <P_IN, SINK extends Sink<E_OUT>> SINK wrapAndCopyInto(SINK downSink, Spliterator<P_IN> spliterator) {
        Objects.requireNonNull(downSink);
        
        /*
         * 从downSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink。
         *
         * 返回的sink与downSink组成一个完整的链条，以便处理属于上个(depth==0)的流阶段输出的数据。
         * 经过该sink链条处理过的数据，会被downSink所在的流阶段输出给downSink的下游阶段。
         */
        Sink<P_IN> wrappedSink = wrapSink(downSink);
        
        /*
         * 从wrappedSink开始顺着整个sink链条择取来自spliterator中的数据，
         * 该操作通常会依次执行每个sink上的begin()、accept()、end()方法。
         *
         * 如果当前流上存在短路操作，则可能会提前结束择取过程。
         */
        copyInto(wrappedSink, spliterator);
        
        // 返回未包装的sink，即入参中的属于终端阶段的sink
        return downSink;
    }
    
    /*
     * 从downSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink。
     *
     * 返回的sink与downSink组成一个完整的链条，以便处理属于上个(depth==0)的流阶段输出的数据。
     * 经过该sink链条处理过的数据，会被downSink所在的流阶段输出给downSink的下游阶段。
     *
     * downSink: 下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
     *
     * 注1：每次调用wrapSink()方法，就相当于进入了一个新的阶段，该阶段通常是模拟出来的终端阶段。
     *
     *
     * 例如存在流：
     * 　　H - A- B - T
     * 其中，H是源头阶段，T是终端阶段，I1和I2是两个中间阶段
     *
     * 首先，在T阶段中构造属于T阶段的downSink，并将其作为wrapSink()方法的入参；
     * 其次，回退到B阶段，调用其opWrapSink()方法，构造一个属于B阶段的sink，该sink往往持有T阶段sink的引用；
     * 最后，回退到A阶段，重复上述步骤，返回一个属于A阶段的sink，该sink往往持有B阶段的sink。
     *
     * 注2: 这里某个阶段的sink不一定总会持有下个阶段的sink。
     * 　   比如上游发来的数据已经符合当前阶段的要求时，那么当前阶段可以不做任何处理，直接把属于下游sink返回。
     */
    @Override
    @SuppressWarnings("unchecked")
    final <P_IN> Sink<P_IN> wrapSink(Sink<E_OUT> downSink) {
        Objects.requireNonNull(downSink);
        
        // 将pipe初始化为当前阶段
        @SuppressWarnings("rawtypes")
        AbstractPipeline pipe = AbstractPipeline.this;
        
        Sink<E_OUT> sink = downSink;
        
        // 从downSink开始，逆向遍历流中各个阶段的sink，并尝试将其"串联"起来，直到遇见上一个(depth==0)的流阶段时终止链接
        while(pipe.depth>0) {
            
            // 构造并返回属于pipe流阶段的sink，该sink通常与入参中的sink形成一个链条
            sink = pipe.opWrapSink(pipe.previousStage.combinedFlags, sink);
            
            // 将pipe更新为流的上个阶段的引用
            pipe = pipe.previousStage;
        }
        
        // 返回链接完成的sink
        return (Sink<P_IN>) sink;
    }
    
    /**
     * Accepts a {@code Sink} which will receive the results of this operation,
     * and return a {@code Sink} which accepts elements of the input type of
     * this operation and which performs the operation, passing the results to
     * the provided {@code Sink}.
     *
     * @param flags    The combined stream and operation flags up to, but not including, this operation
     * @param downSink sink to which elements should be sent after processing
     *
     * @return a sink which accepts elements, perform the operation upon each element,
     * and passes the results (if any) to the provided {@code Sink}.
     *
     * @apiNote The implementation may use the {@code flags} parameter to optimize the
     * sink wrapping.  For example, if the input is already {@code DISTINCT},
     * the implementation for the {@code Stream#distinct()} method could just
     * return the sink it was passed.
     */
    /*
     * 构造并返回属于当前流阶段的sink，该sink通常与downSink形成一个链条，以决定如何处理上个流阶段发来的数据。
     *
     * 1.如果上个流阶段发来的数据已经符合当前流阶段的操作要求，则直接返回属于下个流阶段的downSink，即可以将上个流的输出数据直接交给下个流阶段。
     * 2.如果上个流阶段发来的数据不符合预期，则需要构造一个属于当前流阶段的sink来处理这些数据。
     *   等当前流阶段的sink处理完这些数据后，再将其转交给下个流阶段的downSink。
     *   在这种情形下，返回的sink中往往会持有downSink的引用，以便将数据转交给下个流阶段。
     *
     * 注1：sink只是用于处理数据本身的，如果想改变流的某些属性，那么这不归sink管。
     *
     * flags   : 上个流阶段的组合参数。
     * downSink: 下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
     *
     * 注2：该方法仅应由中间阶段的流使用。
     *
     * 注3：对于物理上相邻的两个流阶段，比如如下有一个源头阶段H和两个中间阶段A和B：
     * 　　         H -> A -> B
     * 　　那么，A的"下个流阶段"就是B。
     * 　　可是如果想知道A阶段的输出结果时，就需要虚拟一个终端阶段出来，
     * 　　让该终端阶段作为A阶段的"下个流阶段"来收集A阶段输出的元素。
     * 　　之所以需要模拟终端阶段，是因为目前Stream接口的实现类只有源头阶段的流和中间阶段的流。
     */
    abstract Sink<E_IN> opWrapSink(int flags, Sink<E_OUT> downSink);
    
    /*
     * 从wrappedSink开始顺着整个sink链条择取来自spliterator中的数据，
     * 该操作通常会依次执行每个sink上的begin()、accept()、end()方法。
     *
     * 如果当前流上存在短路操作，则可能会提前结束择取过程。
     *
     * wrappedSink: sink链表上的第一个sink，这是择取数据的起点
     * spliterator: 流迭代器，作为数据源，包含了当前所有待访问的元素
     *
     * 注：wrappedSink所处的sink链条的终点往往是一个终端阶段的sink。
     */
    @Override
    final <P_IN> void copyInto(Sink<P_IN> wrappedSink, Spliterator<P_IN> spliterator) {
        Objects.requireNonNull(wrappedSink);
        
        // 获取当前流阶段的组合参数
        int streamAndOpFlags = getStreamAndOpFlags();
        
        // 存在短路操作
        if(StreamOpFlag.SHORT_CIRCUIT.isKnown(streamAndOpFlags)) {
            /*
             * 从wrappedSink开始顺着整个sink链条择取来自spliterator中的数据，
             * 该操作通常会依次执行每个sink上的begin()、accept()、end()方法。
             *
             * 值的关注的是，这里每次消费元素之前，都要先询问wrappedSink是否应当停止接收数据。
             * 如果wrappedSink无法做出决定，则它应当继续询问下游的sink。
             *
             * 如果当前流阶段收到了应当停止接收数据的信号，则会立即停止择取工作，并返回true。
             */
            copyIntoWithCancel(wrappedSink, spliterator);
            
            // 不存在短路操作
        } else {
            
            // 获取spliterator中的数据量
            long sizeIfKnown = spliterator.getExactSizeIfKnown();
            
            /*
             * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
             * 该行为会顺着sink链条向下游传播。
             */
            wrappedSink.begin(sizeIfKnown);
            
            /*
             * 尝试用wrappedSink逐个消费当前流迭代器中所有剩余元素。
             * 该行为会顺着sink链条向下游传播。
             */
            spliterator.forEachRemaining(wrappedSink);
            
            /*
             * 关闭sink链，结束本轮计算。
             * 该行为会顺着sink链条向下游传播。
             */
            wrappedSink.end();
        }
    }
    
    /*
     * 从wrappedSink开始顺着整个sink链条择取来自spliterator中的数据，
     * 该操作通常会依次执行每个sink上的begin()、accept()、end()方法。
     *
     * 值的关注的是，这里每次消费元素之前，都要先询问wrappedSink是否应当停止接收数据。
     * 如果wrappedSink无法做出决定，则它应当继续询问下游的sink。
     *
     * 如果当前流阶段收到了应当停止接收数据的信号，则会立即停止择取工作，并返回true。
     *
     * 返回值指示是否中途取消了择取操作。
     *
     * wrappedSink: sink链表上的第一个sink，这是择取数据的起点
     * spliterator: 流迭代器，做为数据源，包含了当前所有待访问的元素
     *
     * 注1：wrappedSink所处的sink链条的终点往往是一个终端阶段的sink。
     * 注2：该方法通常用在包含短路操作的流中。
     */
    @Override
    @SuppressWarnings("unchecked")
    final <P_IN> boolean copyIntoWithCancel(Sink<P_IN> wrappedSink, Spliterator<P_IN> spliterator) {
        @SuppressWarnings({"rawtypes"})
        AbstractPipeline p = AbstractPipeline.this;
        
        // 向前遍历，将p指向(depth==0)的流阶段
        while(p.depth>0) {
            p = p.previousStage;
        }
        
        // 获取spliterator中的数据量
        long sizeIfKnown = spliterator.getExactSizeIfKnown();
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         * 该行为会顺着sink链条向下游传播。
         */
        wrappedSink.begin(sizeIfKnown);
        
        /*
         * 尝试用wrappedSink逐个消费spliterator中所有剩余元素。
         * 该行为会顺着sink链条向下游传播。
         *
         * 值的关注的是，这里每次消费元素之前，都要先询问wrappedSink是否应当停止接收数据。
         * 如果wrappedSink无法做出决定，则它应当继续询问下游的sink。
         *
         * 如果当前流阶段收到了应当停止接收数据的信号，则会立即停止择取工作，并返回true。
         */
        boolean cancelled = p.forEachWithCancel(spliterator, wrappedSink);
        
        /*
         * 关闭sink链，结束本轮计算。
         * 该行为会顺着sink链条向下游传播。
         */
        wrappedSink.end();
        
        return cancelled;
    }
    
    /**
     * Traverse the elements of a spliterator compatible with this stream shape,
     * pushing those elements into a sink.   If the sink requests cancellation,
     * no further elements will be pulled or pushed.
     *
     * @param spliterator the spliterator to pull elements from
     * @param wrappedSink the sink to push elements to
     *
     * @return true if the cancellation was requested
     */
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
    abstract boolean forEachWithCancel(Spliterator<E_OUT> spliterator, Sink<E_OUT> wrappedSink);
    
    
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
    /*
     * 构造增强"数组"Node或"弹性缓冲区"Node(引用类型版本)
     *
     * exactSizeIfKnown: 元素数量
     * generator       : 生成内部数组的函数表达式
     *
     * 注：exactSizeIfKnown和generator都是被增强"数组"Node使用的
     */
    @Override
    abstract Node.Builder<E_OUT> makeNodeBuilder(long exactSizeIfKnown, IntFunction<E_OUT[]> generator);
    
    /*
     * 返回当前流阶段的输出元素
     *
     * 将spliterator中的元素搜集到generator生成的数组中，然后将该数组封装到Node中返回。
     * 这个搜集过程会涉及到数据的择取过程，即数据流通常从上一个(depth==1)的流阶段的sink开始，经过整个sink链的筛选后，进入终端阶段的sink。
     *
     * spliterator: 流迭代器，作为数据源，包含了当前所有待访问的元素。
     * flatten    : 指示在并行收集操作中，如果中间生成的Node是树状Node，则最后是否要将其转换为非树状Node后再返回。
     * generator  : 该函数表达式用于生成一个数组，以存储中途搜集到的元素。
     *              对于基本数值类型的元素来说，不会使用该参数，它们在内部会自己生成对应的基本数值类型数组。
     */
    @Override
    @SuppressWarnings("unchecked")
    final <P_IN> Node<E_OUT> evaluate(Spliterator<P_IN> spliterator, boolean flatten, IntFunction<E_OUT[]> generator) {
        
        // 如果需要并行执行
        if(isParallel()) {
            /*
             * 并行搜集元素，中间依然会经过sink的择取操作。
             * 将spliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
             *
             * 注：操作引用类型之外的元素时，不需要使用generator参数。
             */
            return evaluateToNode(this, spliterator, flatten, generator);
        }
        
        /*
         * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
         * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         *
         * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
         */
        long size = exactOutputSizeIfKnown(spliterator);
        
        /*
         * 构造增强"数组"Node或"弹性缓冲区"Node(引用类型版本)，用作临时终端的sink
         */
        Node.Builder<E_OUT> nodeBuilderSink = makeNodeBuilder(size, generator);
        
        /*
         * 从nodeBuilderSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
         * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
         * 最终择取出的数据往往被存入了nodeBuilderSink代表的容器当中。
         *
         * nodeBuilderSink: 下个流阶段的sink。如果nodeBuilderSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
         * spliterator    : 流迭代器，作为数据源，包含了当前所有待访问的元素
         */
        wrapAndCopyInto(nodeBuilderSink, spliterator);
        
        // 返回由Builder构建的Node（返回自身）
        return nodeBuilderSink.build();
    }
    
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
    /*
     * 并行搜集元素，中间依然会经过sink的择取操作。
     * 将spliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
     */
    abstract <P_IN> Node<E_OUT> evaluateToNode(PipelineHelper<E_OUT> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<E_OUT[]> generator);
    
    
    /*
     * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
     * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
     *
     * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
     */
    @Override
    final <P_IN> long exactOutputSizeIfKnown(Spliterator<P_IN> spliterator) {
        
        // 返回当前流的组合参数
        int streamAndOpFlags = getStreamAndOpFlags();
        
        return StreamOpFlag.SIZED.isKnown(streamAndOpFlags) ? spliterator.getExactSizeIfKnown() : -1;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
