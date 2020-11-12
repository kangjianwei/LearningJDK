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

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Factory for instances of a short-circuiting {@code TerminalOp} that searches
 * for an element in a stream pipeline, and terminates when it finds one.
 * Supported variants include find-first (find the first element in the
 * encounter order) and find-any (find any element, may not be the first in
 * encounter order.)
 *
 * @since 1.8
 */
// 应用在终端阶段的辅助类，服务于查找操作
final class FindOps {
    
    private FindOps() {
    }
    
    /**
     * Constructs a {@code TerminalOp} for streams of objects.
     *
     * @param <T>           the type of elements of the stream
     * @param mustFindFirst whether the {@code TerminalOp} must produce the
     *                      first element in the encounter order
     *
     * @return a {@code TerminalOp} implementing the find operation
     */
    // 返回用于"查找"的终端操作(引用类型版本)
    @SuppressWarnings("unchecked")
    public static <T> TerminalOp<T, Optional<T>> makeRef(boolean mustFindFirst) {
        return (TerminalOp<T, Optional<T>>) (mustFindFirst ? FindSink.OfRef.OP_FIND_FIRST : FindSink.OfRef.OP_FIND_ANY);
    }
    
    /**
     * Constructs a {@code TerminalOp} for streams of ints.
     *
     * @param mustFindFirst whether the {@code TerminalOp} must produce the
     *                      first element in the encounter order
     *
     * @return a {@code TerminalOp} implementing the find operation
     */
    // 返回用于"查找"的终端操作(int类型版本)
    public static TerminalOp<Integer, OptionalInt> makeInt(boolean mustFindFirst) {
        return mustFindFirst ? FindSink.OfInt.OP_FIND_FIRST : FindSink.OfInt.OP_FIND_ANY;
    }
    
    /**
     * Constructs a {@code TerminalOp} for streams of longs.
     *
     * @param mustFindFirst whether the {@code TerminalOp} must produce the
     *                      first element in the encounter order
     *
     * @return a {@code TerminalOp} implementing the find operation
     */
    // 返回用于"查找"的终端操作(long类型版本)
    public static TerminalOp<Long, OptionalLong> makeLong(boolean mustFindFirst) {
        return mustFindFirst ? FindSink.OfLong.OP_FIND_FIRST : FindSink.OfLong.OP_FIND_ANY;
    }
    
    /**
     * Constructs a {@code FindOp} for streams of doubles.
     *
     * @param mustFindFirst whether the {@code TerminalOp} must produce the
     *                      first element in the encounter order
     *
     * @return a {@code TerminalOp} implementing the find operation
     */
    // 返回用于"查找"的终端操作(double类型版本)
    public static TerminalOp<Double, OptionalDouble> makeDouble(boolean mustFindFirst) {
        return mustFindFirst ? FindSink.OfDouble.OP_FIND_FIRST : FindSink.OfDouble.OP_FIND_ANY;
    }
    
    
    /**
     * Implementation of @{code TerminalSink} that implements the find
     * functionality, requesting cancellation when something has been found
     *
     * @param <T> The type of input element
     * @param <O> The result type, typically an optional type
     */
    // "查找"sink
    private abstract static class FindSink<T, O> implements TerminalSink<T, O> {
        boolean hasValue;
        T value;
        
        // Avoid creation of special accessor
        FindSink() {
        }
        
        /*
         * 对上游发来的引用类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(T value) {
            if(hasValue) {
                return;
            }
            
            hasValue = true;
            this.value = value;
        }
        
        @Override
        public boolean cancellationRequested() {
            return hasValue;
        }
        
        
        /** Specialization of {@code FindSink} for reference streams */
        // "查找"sink(引用类型版本)
        static final class OfRef<T> extends FindSink<T, Optional<T>> {
            static final TerminalOp<?, ?> OP_FIND_FIRST = new FindOp<>(true, StreamShape.REFERENCE, Optional.empty(), Optional::isPresent, FindSink.OfRef::new);
            static final TerminalOp<?, ?> OP_FIND_ANY = new FindOp<>(false, StreamShape.REFERENCE, Optional.empty(), Optional::isPresent, FindSink.OfRef::new);
            
            @Override
            public Optional<T> get() {
                return hasValue ? Optional.of(value) : null;
            }
        }
        
        /** Specialization of {@code FindSink} for int streams */
        // "查找"sink(int类型版本)
        static final class OfInt extends FindSink<Integer, OptionalInt> implements Sink.OfInt {
            static final TerminalOp<Integer, OptionalInt> OP_FIND_FIRST = new FindOp<>(true, StreamShape.INT_VALUE, OptionalInt.empty(), OptionalInt::isPresent, FindSink.OfInt::new);
            static final TerminalOp<Integer, OptionalInt> OP_FIND_ANY = new FindOp<>(false, StreamShape.INT_VALUE, OptionalInt.empty(), OptionalInt::isPresent, FindSink.OfInt::new);
            
            /*
             * 对上游发来的int类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(int value) {
                // Boxing is OK here, since few values will actually flow into the sink
                accept((Integer) value);
            }
            
            @Override
            public OptionalInt get() {
                return hasValue ? OptionalInt.of(value) : null;
            }
        }
        
        /** Specialization of {@code FindSink} for long streams */
        // "查找"sink(long类型版本)
        static final class OfLong extends FindSink<Long, OptionalLong> implements Sink.OfLong {
            static final TerminalOp<Long, OptionalLong> OP_FIND_FIRST = new FindOp<>(true, StreamShape.LONG_VALUE, OptionalLong.empty(), OptionalLong::isPresent, FindSink.OfLong::new);
            static final TerminalOp<Long, OptionalLong> OP_FIND_ANY = new FindOp<>(false, StreamShape.LONG_VALUE, OptionalLong.empty(), OptionalLong::isPresent, FindSink.OfLong::new);
            
            /*
             * 对上游发来的long类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(long value) {
                // Boxing is OK here, since few values will actually flow into the sink
                accept((Long) value);
            }
            
            @Override
            public OptionalLong get() {
                return hasValue ? OptionalLong.of(value) : null;
            }
        }
        
        /** Specialization of {@code FindSink} for double streams */
        // "查找"sink(double类型版本)
        static final class OfDouble extends FindSink<Double, OptionalDouble> implements Sink.OfDouble {
            static final TerminalOp<Double, OptionalDouble> OP_FIND_FIRST = new FindOp<>(true, StreamShape.DOUBLE_VALUE, OptionalDouble.empty(), OptionalDouble::isPresent, FindSink.OfDouble::new);
            static final TerminalOp<Double, OptionalDouble> OP_FIND_ANY = new FindOp<>(false, StreamShape.DOUBLE_VALUE, OptionalDouble.empty(), OptionalDouble::isPresent, FindSink.OfDouble::new);
            
            /*
             * 对上游发来的double类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(double value) {
                // Boxing is OK here, since few values will actually flow into the sink
                accept((Double) value);
            }
            
            @Override
            public OptionalDouble get() {
                return hasValue ? OptionalDouble.of(value) : null;
            }
        }
    }
    
    /**
     * A short-circuiting {@code TerminalOp} that searches for an element in a
     * stream pipeline, and terminates when it finds one.  Implements both
     * find-first (find the first element in the encounter order) and find-any
     * (find any element, may not be the first in encounter order.)
     *
     * @param <T> the output type of the stream pipeline
     * @param <O> the result type of the find operation, typically an optional
     *            type
     */
    // 用于"查找"的终端操作
    private static final class FindOp<T, O> implements TerminalOp<T, O> {
        final int opFlags;
        final O emptyValue;
        final Predicate<O> presentPredicate;
        final Supplier<TerminalSink<T, O>> sinkSupplier;
        private final StreamShape shape;
        
        /**
         * Constructs a {@code FindOp}.
         *
         * @param mustFindFirst    if true, must find the first element in
         *                         encounter order, otherwise can find any element
         * @param shape            stream shape of elements to search
         * @param emptyValue       result value corresponding to "found nothing"
         * @param presentPredicate {@code Predicate} on result value
         *                         corresponding to "found something"
         * @param sinkSupplier     supplier for a {@code TerminalSink} implementing
         *                         the matching functionality
         */
        FindOp(boolean mustFindFirst, StreamShape shape, O emptyValue, Predicate<O> presentPredicate, Supplier<TerminalSink<T, O>> sinkSupplier) {
            // 如果不是必须查找物理上的首个元素，则设置一个NOT_ORDERED标志
            this.opFlags = StreamOpFlag.IS_SHORT_CIRCUIT | (mustFindFirst ? 0 : StreamOpFlag.NOT_ORDERED);
            this.shape = shape;
            this.emptyValue = emptyValue;
            this.presentPredicate = presentPredicate;
            this.sinkSupplier = sinkSupplier;
        }
        
        @Override
        public int getOpFlags() {
            return opFlags;
        }
        
        @Override
        public StreamShape inputShape() {
            return shape;
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
        public <S> O evaluateSequential(PipelineHelper<T> helper, Spliterator<S> spliterator) {
            
            TerminalSink<T, O> terminalSink = sinkSupplier.get();
            
            /*
             * 从terminalSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
             * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
             * 最终择取出的数据往往被存入了terminalSink代表的容器当中。
             *
             * terminalSink: (相对于helper的)下个流阶段的sink。如果terminalSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             * spliterator : 流迭代器，作为数据源，包含了当前所有待访问的元素
             */
            helper.wrapAndCopyInto(terminalSink, spliterator);
            
            // 获取执行结果
            O result = terminalSink.get();
            
            return result != null ? result : emptyValue;
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
        public <P_IN> O evaluateParallel(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
            /* This takes into account the upstream ops flags and the terminal op flags and therefore takes into account findFirst or findAny */
            
            // 获取helper流阶段的组合参数
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            // 判断helper流阶段的元素是否有固定的遭遇顺序，如果是的话，后续要查找物理上的首个元素
            boolean mustFindFirst = StreamOpFlag.ORDERED.isKnown(streamAndOpFlags);
            
            // 构造"查找"任务
            FindTask<P_IN, T, O> task = new FindTask<>(this, mustFindFirst, helper, spliterator);
            
            // 执行任务
            return task.invoke();
        }
    }
    
    /**
     * {@code ForkJoinTask} implementing parallel short-circuiting search
     *
     * @param <P_IN>  Input element type to the stream pipeline
     * @param <P_OUT> Output element type from the stream pipeline
     * @param <O>     Result type from the find operation
     */
    // "查找"任务
    @SuppressWarnings("serial")
    private static final class FindTask<P_IN, P_OUT, O> extends AbstractShortCircuitTask<P_IN, P_OUT, O, FindTask<P_IN, P_OUT, O>> {
        private final FindOp<P_OUT, O> op;
        private final boolean mustFindFirst;    // 是否必须查找物理上的首个元素
        
        FindTask(FindOp<P_OUT, O> op, boolean mustFindFirst, PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator) {
            super(helper, spliterator);
            this.mustFindFirst = mustFindFirst;
            this.op = op;
        }
        
        FindTask(FindTask<P_IN, P_OUT, O> parent, Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            this.mustFindFirst = parent.mustFindFirst;
            this.op = parent.op;
        }
        
        @Override
        protected FindTask<P_IN, P_OUT, O> makeChild(Spliterator<P_IN> spliterator) {
            return new FindTask<>(this, spliterator);
        }
        
        @Override
        protected O getEmptyResult() {
            return op.emptyValue;
        }
        
        @Override
        protected O doLeaf() {
            
            // 终端sink，收集找到的元素
            TerminalSink<P_OUT, O> terminalSink = op.sinkSupplier.get();
            
            /*
             * 从terminalSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
             * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
             * 最终择取出的数据往往被存入了terminalSink代表的容器当中。
             *
             * terminalSink: (相对于helper的)下个流阶段的sink。如果terminalSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             * spliterator : 流迭代器，作为数据源，包含了当前所有待访问的元素
             */
            helper.wrapAndCopyInto(terminalSink, spliterator);
            
            // 获取任务执行结果
            O result = terminalSink.get();
            
            // 如果必须查找物理上的首个元素
            if(mustFindFirst) {
                // 如果已有任务结果，则需要进一步判断
                if(result != null) {
                    // 确定该保存结果还是取消后续任务
                    foundResult(result);
                    
                    /*
                     * 返回的result会被设置为当前任务的执行结果：
                     * 如果是根结点，设置共享结果，否则，设置普通结果
                     */
                    return result;
                }
                // 如果不是必须查找物理上的首个元素，则可以直接设置共享结果
            } else {
                if(result != null) {
                    // 设置共享任务结果
                    shortCircuit(result);
                }
            }
            
            return null;
        }
        
        @Override
        public void onCompletion(CountedCompleter<?> caller) {
            // 如果必须查找物理上的首个元素
            if(mustFindFirst) {
                
                // 遍历子任务
                for(FindTask<P_IN, P_OUT, O> child = leftChild, p = null; child != p; p = child, child = rightChild) {
                    // 获取子任务结果(普通结果)
                    O result = child.getLocalResult();
                    
                    // 如果存在有效的结果，则需要进一步判断
                    if(result != null && op.presentPredicate.test(result)) {
                        
                        // 为当前任务设置执行结果：如果是根结点，设置共享结果，否则，设置普通结果
                        setLocalResult(result);
                        
                        // 确定该保存结果还是取消后续任务
                        foundResult(result);
                        
                        break;
                    }
                }
            }
            
            super.onCompletion(caller);
        }
        
        // 确定该保存结果还是取消后续任务
        private void foundResult(O answer) {
            // 如果当前任务是最左侧的任务，则可以设置共享任务结果
            if(isLeftmostNode()) {
                // 设置共享任务结果
                shortCircuit(answer);
                
                // 如果当前任务不是最左侧的任务，那么取消该任务之后的任务
            } else {
                cancelLaterNodes();
            }
        }
    }
    
    
}

