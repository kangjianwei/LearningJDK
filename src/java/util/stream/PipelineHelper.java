/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.IntFunction;

/**
 * Helper class for executing <a href="package-summary.html#StreamOps">
 * stream pipelines</a>, capturing all of the information about a stream
 * pipeline (output shape, intermediate operations, stream flags, parallelism,
 * etc) in one place.
 *
 * <p>
 * A {@code PipelineHelper} describes the initial segment of a stream pipeline,
 * including its source, intermediate operations, and may additionally
 * incorporate information about the terminal (or stateful) operation which
 * follows the last intermediate operation described by this
 * {@code PipelineHelper}. The {@code PipelineHelper} is passed to the
 * {@link TerminalOp#evaluateParallel(PipelineHelper, java.util.Spliterator)},
 * {@link TerminalOp#evaluateSequential(PipelineHelper, java.util.Spliterator)},
 * and {@link AbstractPipeline#opEvaluateParallel(PipelineHelper, java.util.Spliterator,
 * java.util.function.IntFunction)}, methods, which can use the
 * {@code PipelineHelper} to access information about the pipeline such as
 * head shape, stream flags, and size, and use the helper methods
 * such as {@link #wrapAndCopyInto(Sink, Spliterator)},
 * {@link #copyInto(Sink, Spliterator)}, and {@link #wrapSink(Sink)} to execute
 * pipeline operations.
 *
 * @param <P_OUT> type of output elements from the pipeline
 * @since 1.8
 */
// 流的辅助类，源头阶段的流和中间阶段的流都可以使用其中的方法
abstract class PipelineHelper<P_OUT> {

    /**
     * Gets the stream shape for the source of the pipeline segment.
     *
     * @return the stream shape for the source of the pipeline segment.
     */
    // 返回(depth==0)阶段的流的形状
    abstract StreamShape getSourceShape();
    
    /**
     * Gets the combined stream and operation flags for the output of the described pipeline.
     * This will incorporate stream flags from the stream source,
     * all the intermediate operations and the terminal operation.
     *
     * @return the combined stream and operation flags
     *
     * @see StreamOpFlag
     */
    // 返回当前流阶段的组合参数
    abstract int getStreamAndOpFlags();

    /**
     * Returns the exact output size of the portion of the output resulting from
     * applying the pipeline stages described by this {@code PipelineHelper} to
     * the portion of the input described by the provided
     * {@code Spliterator}, if known.  If not known or known infinite, will
     * return {@code -1}.
     *
     * @apiNote
     * The exact output size is known if the {@code Spliterator} has the
     * {@code SIZED} characteristic, and the operation flags
     * {@link StreamOpFlag#SIZED} is known on the combined stream and operation
     * flags.
     *
     * @param spliterator the spliterator describing the relevant portion of the source data
     *
     * @return the exact size if known, or -1 if infinite or unknown
     */
    /*
     * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
     * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
     *
     * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
     */
    abstract <P_IN> long exactOutputSizeIfKnown(Spliterator<P_IN> spliterator);
    
    /**
     * Applies the pipeline stages described by this {@code PipelineHelper} to
     * the provided {@code Spliterator} and send the results to the provided
     * {@code Sink}.
     *
     * @implSpec
     * The implementation behaves as if:
     * <pre>{@code
     *     copyInto(wrapSink(sink), spliterator);
     * }</pre>
     *
     * @param sink the {@code Sink} to receive the results
     * @param spliterator the spliterator describing the source input to process
     */
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
    abstract <P_IN, S extends Sink<P_OUT>> S wrapAndCopyInto(S downSink, Spliterator<P_IN> spliterator);
    
    /**
     * Takes a {@code Sink} that accepts elements of the output type of the
     * {@code PipelineHelper}, and wrap it with a {@code Sink} that accepts
     * elements of the input type and implements all the intermediate operations
     * described by this {@code PipelineHelper}, delivering the result into the
     * provided {@code Sink}.
     *
     * @param sink the {@code Sink} to receive the results
     *
     * @return a {@code Sink} that implements the pipeline stages and sends
     * results to the provided {@code Sink}
     */
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
    abstract <P_IN> Sink<P_IN> wrapSink(Sink<P_OUT> downSink);
    
    /**
     * Pushes elements obtained from the {@code Spliterator} into the provided
     * {@code Sink}.  If the stream pipeline is known to have short-circuiting
     * stages in it (see {@link StreamOpFlag#SHORT_CIRCUIT}), the
     * {@link Sink#cancellationRequested()} is checked after each
     * element, stopping if cancellation is requested.
     *
     * @implSpec
     * This method conforms to the {@code Sink} protocol of calling
     * {@code Sink.begin} before pushing elements, via {@code Sink.accept}, and
     * calling {@code Sink.end} after all elements have been pushed.
     *
     * @param wrappedSink the destination {@code Sink}
     * @param spliterator the source {@code Spliterator}
     */
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
    abstract <P_IN> void copyInto(Sink<P_IN> wrappedSink, Spliterator<P_IN> spliterator);
    
    /**
     * Pushes elements obtained from the {@code Spliterator} into the provided
     * {@code Sink}, checking {@link Sink#cancellationRequested()} after each
     * element, and stopping if cancellation is requested.
     *
     * @implSpec
     * This method conforms to the {@code Sink} protocol of calling
     * {@code Sink.begin} before pushing elements, via {@code Sink.accept}, and
     * calling {@code Sink.end} after all elements have been pushed or if
     * cancellation is requested.
     *
     * @param wrappedSink the destination {@code Sink}
     * @param spliterator the source {@code Spliterator}
     * @return true if the cancellation was requested
     */
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
    abstract <P_IN> boolean copyIntoWithCancel(Sink<P_IN> wrappedSink, Spliterator<P_IN> spliterator);
    
    /**
     * Constructs a @{link Node.Builder} compatible with the output shape of
     * this {@code PipelineHelper}.
     *
     * @param exactSizeIfKnown if >=0 then a builder will be created that has a
     *                         fixed capacity of exactly sizeIfKnown elements; if < 0 then the
     *                         builder has variable capacity.  A fixed capacity builder will fail
     *                         if an element is added after the builder has reached capacity.
     * @param generator        a factory function for array instances
     *
     * @return a {@code Node.Builder} compatible with the output shape of this
     * {@code PipelineHelper}
     */
    /*
     * 构造增强"数组"Node或"弹性缓冲区"Node(引用类型版本)
     *
     * exactSizeIfKnown: 元素数量
     * generator       : 生成内部数组的函数表达式
     *
     * 注：exactSizeIfKnown和generator都是被增强"数组"Node使用的
     */
    abstract Node.Builder<P_OUT> makeNodeBuilder(long exactSizeIfKnown, IntFunction<P_OUT[]> generator);
    
    /**
     * Collects all output elements resulting from applying the pipeline stages
     * to the source {@code Spliterator} into a {@code Node}.
     *
     * @param spliterator the source {@code Spliterator}
     * @param flatten     if true and the pipeline is a parallel pipeline then the
     *                    {@code Node} returned will contain no children, otherwise the
     *                    {@code Node} may represent the root in a tree that reflects the
     *                    shape of the computation tree.
     * @param generator   a factory function for array instances
     *
     * @return the {@code Node} containing all output elements
     *
     * @implNote If the pipeline has no intermediate operations and the source is backed
     * by a {@code Node} then that {@code Node} will be returned (or flattened
     * and then returned). This reduces copying for a pipeline consisting of a
     * stateful operation followed by a terminal operation that returns an
     * array, such as:
     * <pre>{@code
     *     stream.sorted().toArray();
     * }</pre>
     */
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
    abstract <P_IN> Node<P_OUT> evaluate(Spliterator<P_IN> spliterator, boolean flatten, IntFunction<P_OUT[]> generator);
    
    /*
     * 返回一个"包装"流迭代器，使用该流迭代器可以获取当前流阶段的输出元素。
     *
     * sourceSpliterator: 相对于当前流阶段的上个(depth==0)的流阶段的流迭代器
     */
    abstract <P_IN> Spliterator<P_OUT> wrapSpliterator(Spliterator<P_IN> sourceSpliterator);
    
}
