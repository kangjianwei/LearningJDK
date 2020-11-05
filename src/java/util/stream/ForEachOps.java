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

import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountedCompleter;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;

/**
 * Factory for creating instances of {@code TerminalOp} that perform an
 * action for every element of a stream.  Supported variants include unordered
 * traversal (elements are provided to the {@code Consumer} as soon as they are
 * available), and ordered traversal (elements are provided to the
 * {@code Consumer} in encounter order.)
 *
 * <p>Elements are provided to the {@code Consumer} on whatever thread and
 * whatever order they become available.  For ordered traversals, it is
 * guaranteed that processing an element <em>happens-before</em> processing
 * subsequent elements in the encounter order.
 *
 * <p>Exceptions occurring as a result of sending an element to the
 * {@code Consumer} will be relayed to the caller and traversal will be
 * prematurely terminated.
 *
 * @since 1.8
 */
// 应用在终端阶段的辅助类，服务于遍历操作
final class ForEachOps {
    
    private ForEachOps() {
    }
    
    
    /**
     * Constructs a {@code TerminalOp} that perform an action for every element
     * of a stream.
     *
     * @param action  the {@code Consumer} that receives all elements of a
     *                stream
     * @param ordered whether an ordered traversal is requested
     * @param <T>     the type of the stream elements
     *
     * @return the {@code TerminalOp} instance
     */
    // 返回用于"遍历"的终端操作(引用类型版本)
    public static <T> TerminalOp<T, Void> makeRef(Consumer<? super T> action, boolean ordered) {
        Objects.requireNonNull(action);
        return new ForEachOp.OfRef<>(action, ordered);
    }
    
    /**
     * Constructs a {@code TerminalOp} that perform an action for every element
     * of an {@code IntStream}.
     *
     * @param action  the {@code IntConsumer} that receives all elements of a
     *                stream
     * @param ordered whether an ordered traversal is requested
     *
     * @return the {@code TerminalOp} instance
     */
    // 返回用于"遍历"的终端操作(int类型版本)
    public static TerminalOp<Integer, Void> makeInt(IntConsumer action, boolean ordered) {
        Objects.requireNonNull(action);
        return new ForEachOp.OfInt(action, ordered);
    }
    
    /**
     * Constructs a {@code TerminalOp} that perform an action for every element
     * of a {@code LongStream}.
     *
     * @param action  the {@code LongConsumer} that receives all elements of a
     *                stream
     * @param ordered whether an ordered traversal is requested
     *
     * @return the {@code TerminalOp} instance
     */
    // 返回用于"遍历"的终端操作(long类型版本)
    public static TerminalOp<Long, Void> makeLong(LongConsumer action, boolean ordered) {
        Objects.requireNonNull(action);
        return new ForEachOp.OfLong(action, ordered);
    }
    
    /**
     * Constructs a {@code TerminalOp} that perform an action for every element
     * of a {@code DoubleStream}.
     *
     * @param action  the {@code DoubleConsumer} that receives all elements of
     *                a stream
     * @param ordered whether an ordered traversal is requested
     *
     * @return the {@code TerminalOp} instance
     */
    // 返回用于"遍历"的终端操作(double类型版本)
    public static TerminalOp<Double, Void> makeDouble(DoubleConsumer action, boolean ordered) {
        Objects.requireNonNull(action);
        return new ForEachOp.OfDouble(action, ordered);
    }
    
    
    /**
     * A {@code TerminalOp} that evaluates a stream pipeline and sends the
     * output to itself as a {@code TerminalSink}.  Elements will be sent in
     * whatever thread they become available.  If the traversal is unordered,
     * they will be sent independent of the stream's encounter order.
     *
     * <p>This terminal operation is stateless.  For parallel evaluation, each
     * leaf instance of a {@code ForEachTask} will send elements to the same
     * {@code TerminalSink} reference that is an instance of this class.
     *
     * @param <T> the output type of the stream pipeline
     */
    // 用于"遍历"的终端操作的抽象实现(这种操作的实现比较简单)
    abstract static class ForEachOp<T> implements TerminalOp<T, Void>, TerminalSink<T, Void> {
        
        private final boolean ordered;
        
        protected ForEachOp(boolean ordered) {
            this.ordered = ordered;
        }
        
        @Override
        public int getOpFlags() {
            return ordered ? 0 : StreamOpFlag.NOT_ORDERED;
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
        @Override
        public <S> Void evaluateSequential(PipelineHelper<T> helper, Spliterator<S> spliterator) {
            
            /*
             * 从当前sink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
             * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
             * 最终择取出的数据往往被存入了当前sink代表的容器当中。
             *
             * this       : (相对于helper的)下个流阶段的sink。如果当前sink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             * spliterator: 流迭代器，作为数据源，包含了当前所有待访问的元素
             */
            helper.wrapAndCopyInto(this, spliterator);
            
            // 返回任务执行结果，这里没有返回值，返回Void
            return this.get();
        }
        
        /*
         * 并行处理helper流阶段输出的元素，返回处理后的结果
         *
         * 为helper构造一个终端sink，并使用该终端sink对spliterator中的数据进行择取，返回最后的处理结果。
         * 返回值可能是收集到的元素，也可能只是对过滤后的元素的计数，还可能是其它定制化的结果。
         *
         * helper     : 某个流阶段，通常需要在当前终端操作中处理从helper阶段输出的数据
         * spliterator: 待处理的数据的源头，该流迭代器属于helper之前的(depth==0)的流阶段(包含helper阶段)
         */
        @Override
        public <S> Void evaluateParallel(PipelineHelper<T> helper, Spliterator<S> spliterator) {
            
            if(ordered) {
                ForEachOrderedTask<S, T> task = new ForEachOrderedTask<>(helper, spliterator, this);
                task.invoke();
            } else {
                /*
                 * 从当前sink开始，逆向遍历流，构造并返回(相对于helper阶段的)属于上个(depth==1)的流阶段的sink。
                 *
                 * 返回的sink与downSink组成一个完整的链条，以便处理属于上个(depth==0)的流阶段输出的数据。
                 * 经过该sink链条处理过的数据，会被downSink所在的流阶段输出给downSink的下游阶段。
                 *
                 * this: (相对于helper阶段的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
                 */
                Sink<S> wrappedSink = helper.wrapSink(this);
                
                ForEachTask<S, T> task = new ForEachTask<>(helper, spliterator, wrappedSink);
                
                task.invoke();
            }
            
            return null;
        }
        
        // 返回任务执行结果，这里没有返回值，返回Void
        @Override
        public Void get() {
            return null;
        }
        
        
        /** Implementation class for reference streams */
        // 用于"遍历"的终端操作(引用类型版本)
        static final class OfRef<T> extends ForEachOp<T> {
            final Consumer<? super T> consumer;
            
            OfRef(Consumer<? super T> consumer, boolean ordered) {
                super(ordered);
                this.consumer = consumer;
            }
            
            /*
             * 对上游发来的引用类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(T t) {
                consumer.accept(t);
            }
        }
        
        /** Implementation class for {@code IntStream} */
        // 用于"遍历"的终端操作(int类型版本)
        static final class OfInt extends ForEachOp<Integer> implements Sink.OfInt {
            final IntConsumer consumer;
            
            OfInt(IntConsumer consumer, boolean ordered) {
                super(ordered);
                this.consumer = consumer;
            }
            
            @Override
            public StreamShape inputShape() {
                return StreamShape.INT_VALUE;
            }
            
            /*
             * 对上游发来的int类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(int t) {
                consumer.accept(t);
            }
        }
        
        /** Implementation class for {@code LongStream} */
        // 用于"遍历"的终端操作(long类型版本)
        static final class OfLong extends ForEachOp<Long> implements Sink.OfLong {
            final LongConsumer consumer;
            
            OfLong(LongConsumer consumer, boolean ordered) {
                super(ordered);
                this.consumer = consumer;
            }
            
            @Override
            public StreamShape inputShape() {
                return StreamShape.LONG_VALUE;
            }
            
            /*
             * 对上游发来的long类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(long t) {
                consumer.accept(t);
            }
        }
        
        /** Implementation class for {@code DoubleStream} */
        // 用于"遍历"的终端操作(double类型版本)
        static final class OfDouble extends ForEachOp<Double> implements Sink.OfDouble {
            final DoubleConsumer consumer;
            
            OfDouble(DoubleConsumer consumer, boolean ordered) {
                super(ordered);
                this.consumer = consumer;
            }
            
            @Override
            public StreamShape inputShape() {
                return StreamShape.DOUBLE_VALUE;
            }
            
            /*
             * 对上游发来的double类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(double t) {
                consumer.accept(t);
            }
        }
        
    }
    
    /** A {@code ForkJoinTask} for performing a parallel for-each operation */
    // "遍历"任务，可能不遵循遭遇顺序
    @SuppressWarnings("serial")
    static final class ForEachTask<S, T> extends CountedCompleter<Void> {
        private final Sink<S> sink;
        private final PipelineHelper<T> helper;
        private Spliterator<S> spliterator;
        private long targetSize;
        
        ForEachTask(PipelineHelper<T> helper, Spliterator<S> spliterator, Sink<S> sink) {
            super(null);
            this.sink = sink;
            this.helper = helper;
            this.spliterator = spliterator;
            this.targetSize = 0L;
        }
        
        ForEachTask(ForEachTask<S, T> parent, Spliterator<S> spliterator) {
            super(parent);
            this.spliterator = spliterator;
            this.sink = parent.sink;
            this.targetSize = parent.targetSize;
            this.helper = parent.helper;
        }
        
        /** Similar to AbstractTask but doesn't need to track child tasks */
        public void compute() {
            Spliterator<S> rightSplit = spliterator, leftSplit;
            
            long sizeEstimate = rightSplit.estimateSize(), sizeThreshold;
            
            if((sizeThreshold = targetSize) == 0L) {
                // 根据传入的元素总量，返回每个子任务(建议)包含的元素数量
                targetSize = sizeThreshold = AbstractTask.suggestTargetSize(sizeEstimate);
            }
            
            // 获取helper流阶段的组合参数
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            boolean isShortCircuit = StreamOpFlag.SHORT_CIRCUIT.isKnown(streamAndOpFlags);
            
            boolean forkRight = false;
            Sink<S> taskSink = sink;
            ForEachTask<S, T> task = this;
            
            while(!isShortCircuit || !taskSink.cancellationRequested()) {
                
                // 如果任务已经没必要拆分，则可以直接执行
                if(sizeEstimate<=sizeThreshold || (leftSplit = rightSplit.trySplit()) == null) {
                    
                    /*
                     * 从taskSink开始顺着整个sink链条择取来自rightSplit中的数据，
                     * 该操作通常会依次执行每个sink上的begin()、accept()、end()方法。
                     *
                     * 如果当前流上存在短路操作，则可能会提前结束择取过程。
                     */
                    task.helper.copyInto(taskSink, rightSplit);
                    
                    break;
                }
                
                // 封装子任务
                ForEachTask<S, T> leftTask = new ForEachTask<>(task, leftSplit);
                
                // 设置挂起计数
                task.addToPendingCount(1);
                
                ForEachTask<S, T> taskToFork;
                
                // 轮流拆分左右子任务
                if(forkRight) {
                    forkRight = false;
                    rightSplit = leftSplit;
                    taskToFork = task;
                    task = leftTask;
                } else {
                    forkRight = true;
                    taskToFork = leftTask;
                }
                
                // 将taskToFork交给其他线程去执行
                taskToFork.fork();
                
                // 更新right任务中包含的元素数量
                sizeEstimate = rightSplit.estimateSize();
            }
            
            task.spliterator = null;
            
            // 尝试完成任务，并将完成行为向上传播(不会触发onCompletion()回调)
            task.propagateCompletion();
        }
        
    }
    
    /**
     * A {@code ForkJoinTask} for performing a parallel for-each operation which visits the elements in encounter order
     */
    // "遍历"任务，遵循遭遇顺序
    @SuppressWarnings("serial")
    static final class ForEachOrderedTask<S, T> extends CountedCompleter<Void> {
        
        /*
         * Our goal is to ensure that the elements associated with a task are
         * processed according to an in-order traversal of the computation tree.
         * We use completion counts for representing these dependencies, so that
         * a task does not complete until all the tasks preceding it in this
         * order complete.  We use the "completion map" to associate the next
         * task in this order for any left child.  We increase the pending count
         * of any node on the right side of such a mapping by one to indicate
         * its dependency, and when a node on the left side of such a mapping
         * completes, it decrements the pending count of its corresponding right
         * side.  As the computation tree is expanded by splitting, we must
         * atomically update the mappings to maintain the invariant that the
         * completion map maps left children to the next node in the in-order
         * traversal.
         *
         * Take, for example, the following computation tree of tasks:
         *
         *       a
         *      / \
         *     b   c
         *    / \ / \
         *   d  e f  g
         *
         * The complete map will contain (not necessarily all at the same time)
         * the following associations:
         *
         *   d -> e
         *   b -> f
         *   f -> g
         *
         * Tasks e, f, g will have their pending counts increased by 1.
         *
         * The following relationships hold:
         *
         *   - completion of d "happens-before" e;
         *   - completion of d and e "happens-before b;
         *   - completion of b "happens-before" f; and
         *   - completion of f "happens-before" g
         *
         * Thus overall the "happens-before" relationship holds for the
         * reporting of elements, covered by tasks d, e, f and g, as specified
         * by the forEachOrdered operation.
         */
        
        private final PipelineHelper<T> helper;
        private final long targetSize;
        private final ConcurrentHashMap<ForEachOrderedTask<S, T>, ForEachOrderedTask<S, T>> completionMap;
        private final Sink<T> action;
        private final ForEachOrderedTask<S, T> leftPredecessor;
        private Spliterator<S> spliterator;
        private Node<T> node;
        
        protected ForEachOrderedTask(PipelineHelper<T> helper, Spliterator<S> spliterator, Sink<T> action) {
            super(null);
            
            this.helper = helper;
            this.spliterator = spliterator;
            
            /*
             * 初始时，返回流迭代器中的元素总量(可能不精确)。
             * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
             * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
             */
            long size = spliterator.estimateSize();
            
            // 根据传入的元素总量，返回每个子任务(建议)包含的元素数量
            this.targetSize = AbstractTask.suggestTargetSize(size);
            
            // 如果需要拆分当前任务，则返回子任务数量的一个建议值
            int leafCount = AbstractTask.getLeafTarget();
            
            // Size map to avoid concurrent re-sizes
            this.completionMap = new ConcurrentHashMap<>(Math.max(16, leafCount << 1));
            
            this.action = action;
            this.leftPredecessor = null;
        }
        
        ForEachOrderedTask(ForEachOrderedTask<S, T> parent, Spliterator<S> spliterator, ForEachOrderedTask<S, T> leftPredecessor) {
            super(parent);
            this.helper = parent.helper;
            this.spliterator = spliterator;
            this.targetSize = parent.targetSize;
            this.completionMap = parent.completionMap;
            this.action = parent.action;
            this.leftPredecessor = leftPredecessor;
        }
        
        @Override
        public final void compute() {
            doCompute(this);
        }
        
        private static <S, T> void doCompute(ForEachOrderedTask<S, T> task) {
            Spliterator<S> rightSplit = task.spliterator;
            Spliterator<S> leftSplit;
            
            long sizeThreshold = task.targetSize;
            boolean forkRight = false;
            
            while(true) {
                
                // 如果right任务的数据量已经满足要求，则无需再拆分
                if(rightSplit.estimateSize()<=sizeThreshold) {
                    break;
                }
                
                // 如果right任务数据量过大，则需要拆分其流迭代器
                leftSplit = rightSplit.trySplit();
                if(leftSplit == null) {
                    break;
                }
                
                // 封装左右孩子任务
                ForEachOrderedTask<S, T> leftChild = new ForEachOrderedTask<>(task, leftSplit, task.leftPredecessor);
                ForEachOrderedTask<S, T> rightChild = new ForEachOrderedTask<>(task, rightSplit, leftChild);
                
                /*
                 * Fork the parent task.
                 * Completion of the left and right children "happens-before" completion of the parent.
                 */
                // 父任务设置挂起计数
                task.addToPendingCount(1);
                
                /* Completion of the left child "happens-before" completion of the right child */
                // 右孩子任务也挂起，等待左孩子任务的完成
                rightChild.addToPendingCount(1);
                
                task.completionMap.put(leftChild, rightChild);
                
                // If task is not on the left spine
                if(task.leftPredecessor != null) {
                    /*
                     * Completion of left-predecessor, or left subtree,
                     * "happens-before" completion of left-most leaf node of right subtree.
                     * The left child's pending count needs to be updated before it is associated in the completion map,
                     * otherwise the left child can complete prematurely and violate the "happens-before" constraint.
                     */
                    leftChild.addToPendingCount(1);
                    
                    // Update association of left-predecessor to left-most leaf node of right subtree
                    if(task.completionMap.replace(task.leftPredecessor, task, leftChild)) {
                        // If replaced, adjust the pending count of the parent to complete when its children complete
                        task.addToPendingCount(-1);
                    } else {
                        /*
                         * Left-predecessor has already completed, parent's pending count is adjusted by left-predecessor;
                         * left child is ready to complete.
                         */
                        leftChild.addToPendingCount(-1);
                    }
                }
                
                ForEachOrderedTask<S, T> taskToFork;
                
                // 轮流拆分左右子任务
                if(forkRight) {
                    forkRight = false;
                    rightSplit = leftSplit;
                    task = leftChild;
                    taskToFork = rightChild;
                } else {
                    forkRight = true;
                    task = rightChild;
                    taskToFork = leftChild;
                }
                
                taskToFork.fork();
                
            } // while(true)
            
            /*
             * Task's pending count is either 0 or 1.
             * If 1 then the completion map will contain a value that is task,
             * and two calls to tryComplete are required for completion,
             * one below and one triggered by the completion of task's left-predecessor in onCompletion.
             * Therefore there is no data race within the if block.
             */
            if(task.getPendingCount()>0) {
                // Cannot complete just yet so buffer elements into a Node for use when completion occurs
                @SuppressWarnings("unchecked")
                IntFunction<T[]> generator = size -> (T[]) new Object[size];
                
                /*
                 * 初始时，尝试返回rightSplit中的元素总量。如果无法获取精确值，则返回-1。
                 * 当访问过rightSplit中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
                 *
                 * 注：通常在流拥有SIZED参数(相当于rightSplit有SIZED参数)时可以获取到一个精确值。
                 */
                long sizeIfKnown = task.helper.exactOutputSizeIfKnown(rightSplit);
                
                // 构造增强"数组"Node或"弹性缓冲区"Node(引用类型版本)
                Node.Builder<T> nodeBuilderSink = task.helper.makeNodeBuilder(sizeIfKnown, generator);
                
                /*
                 * 从nodeBuilderSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
                 * 然后从返回的sink开始，顺着整个sink链条择取来自rightSplit中的数据，
                 * 最终择取出的数据往往被存入了nodeBuilderSink代表的容器当中。
                 *
                 * nodeBuilderSink: (相对于task.helper的)下个流阶段的sink。如果nodeBuilderSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
                 * rightSplit     : 流迭代器，作为数据源，包含了当前所有待访问的元素
                 */
                task.helper.wrapAndCopyInto(nodeBuilderSink, rightSplit);
                
                // 获取构造的node
                task.node = nodeBuilderSink.build();
                
                task.spliterator = null;
            }
            
            // 尝试从task开始，向上传播"完成"消息
            task.tryComplete();
        }
        
        @Override
        public void onCompletion(CountedCompleter<?> caller) {
            if(node != null) {
                // Dump buffered elements from this leaf into the sink
                node.forEach(action);
                node = null;
            } else if(spliterator != null) {
                /* Dump elements output from this leaf's pipeline into the sink */
                
                /*
                 * 从action开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
                 * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
                 * 最终择取出的数据往往被存入了action代表的容器当中。
                 *
                 * action     : (相对于helper的)下个流阶段的sink。如果terminalSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
                 * spliterator: 流迭代器，作为数据源，包含了当前所有待访问的元素
                 */
                helper.wrapAndCopyInto(action, spliterator);
                
                spliterator = null;
            }
            
            /*
             * The completion of this task *and* the dumping of elements
             * "happens-before" completion of the associated left-most leaf task of right subtree
             * (if any, which can be this task's right sibling)
             */
            ForEachOrderedTask<S, T> leftDescendant = completionMap.remove(this);
            
            if(leftDescendant != null) {
                // 尝试从leftDescendant任务开始，向上传播"完成"消息
                leftDescendant.tryComplete();
            }
        }
        
    }
    
}
