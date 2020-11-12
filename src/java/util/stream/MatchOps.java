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

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Factory for instances of a short-circuiting {@code TerminalOp} that implement
 * quantified predicate matching on the elements of a stream. Supported variants
 * include match-all, match-any, and match-none.
 *
 * @since 1.8
 */
// 应用在终端阶段的辅助类，服务于匹配操作
final class MatchOps {
    
    private MatchOps() {
    }
    
    /**
     * Constructs a quantified predicate matcher for a Stream.
     *
     * @param <T>       the type of stream elements
     * @param predicate the {@code Predicate} to apply to stream elements
     * @param matchKind the kind of quantified match (all, any, none)
     *
     * @return a {@code TerminalOp} implementing the desired quantified match
     * criteria
     */
    // 返回用于"匹配"的终端操作(引用类型版本)
    public static <T> TerminalOp<T, Boolean> makeRef(Predicate<? super T> predicate, MatchKind matchKind) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(matchKind);
        
        class MatchSink extends BooleanTerminalSink<T> {
            MatchSink() {
                super(matchKind);
            }
            
            /*
             * 对上游发来的引用类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(T t) {
                // 这里是短路操作
                if(!stop && predicate.test(t) == matchKind.stopOnPredicateMatches) {
                    stop = true;
                    value = matchKind.shortCircuitResult;
                }
            }
        }
        
        return new MatchOp<>(StreamShape.REFERENCE, matchKind, MatchSink::new);
    }
    
    /**
     * Constructs a quantified predicate matcher for an {@code IntStream}.
     *
     * @param predicate the {@code Predicate} to apply to stream elements
     * @param matchKind the kind of quantified match (all, any, none)
     *
     * @return a {@code TerminalOp} implementing the desired quantified match
     * criteria
     */
    // 返回用于"匹配"的终端操作(int类型版本)
    public static TerminalOp<Integer, Boolean> makeInt(IntPredicate predicate, MatchKind matchKind) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(matchKind);
        
        class MatchSink extends BooleanTerminalSink<Integer> implements Sink.OfInt {
            MatchSink() {
                super(matchKind);
            }
            
            /*
             * 对上游发来的int类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(int t) {
                // 这里是短路操作
                if(!stop && predicate.test(t) == matchKind.stopOnPredicateMatches) {
                    stop = true;
                    value = matchKind.shortCircuitResult;
                }
            }
        }
        
        return new MatchOp<>(StreamShape.INT_VALUE, matchKind, MatchSink::new);
    }
    
    /**
     * Constructs a quantified predicate matcher for a {@code LongStream}.
     *
     * @param predicate the {@code Predicate} to apply to stream elements
     * @param matchKind the kind of quantified match (all, any, none)
     *
     * @return a {@code TerminalOp} implementing the desired quantified match
     * criteria
     */
    // 返回用于"匹配"的终端操作(long类型版本)
    public static TerminalOp<Long, Boolean> makeLong(LongPredicate predicate, MatchKind matchKind) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(matchKind);
        
        class MatchSink extends BooleanTerminalSink<Long> implements Sink.OfLong {
            
            MatchSink() {
                super(matchKind);
            }
            
            /*
             * 对上游发来的long类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(long t) {
                // 这里是短路操作
                if(!stop && predicate.test(t) == matchKind.stopOnPredicateMatches) {
                    stop = true;
                    value = matchKind.shortCircuitResult;
                }
            }
        }
        
        return new MatchOp<>(StreamShape.LONG_VALUE, matchKind, MatchSink::new);
    }
    
    /**
     * Constructs a quantified predicate matcher for a {@code DoubleStream}.
     *
     * @param predicate the {@code Predicate} to apply to stream elements
     * @param matchKind the kind of quantified match (all, any, none)
     *
     * @return a {@code TerminalOp} implementing the desired quantified match
     * criteria
     */
    // 返回用于"匹配"的终端操作(double类型版本)
    public static TerminalOp<Double, Boolean> makeDouble(DoublePredicate predicate, MatchKind matchKind) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(matchKind);
        
        class MatchSink extends BooleanTerminalSink<Double> implements Sink.OfDouble {
            
            MatchSink() {
                super(matchKind);
            }
            
            /*
             * 对上游发来的double类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(double t) {
                // 这里是短路操作
                if(!stop && predicate.test(t) == matchKind.stopOnPredicateMatches) {
                    stop = true;
                    value = matchKind.shortCircuitResult;
                }
            }
        }
        
        return new MatchOp<>(StreamShape.DOUBLE_VALUE, matchKind, MatchSink::new);
    }
    
    
    /**
     * Enum describing quantified match options -- all match, any match, none match.
     */
    // 匹配模式
    enum MatchKind {
        
        /** Do all elements match the predicate? */
        // 判断是否存在元素满足predicate条件，默认为不是
        ANY(true, true),
        
        /** Do any elements match the predicate? */
        // 判断是否所有元素满足predicate条件，默认为是
        ALL(false, false),
        
        /** Do no elements match the predicate? */
        // 判断是否没有元素满足predicate条件，默认为是
        NONE(true, false);
        
        /*
         * 决定何时该将后续操作短路。
         *
         * ANY : 找到首个满足条件的元素时进行短路
         * ALL : 找到首个不满足条件的元素时进行短路
         * NONE: 找到首个满足条件的元素时进行短路
         */
        private final boolean stopOnPredicateMatches;
        private final boolean shortCircuitResult;
        
        MatchKind(boolean stopOnPredicateMatches, boolean shortCircuitResult) {
            this.stopOnPredicateMatches = stopOnPredicateMatches;
            this.shortCircuitResult = shortCircuitResult;
        }
        
    }
    
    /**
     * Boolean specific terminal sink to avoid the boxing costs when returning
     * results.  Subclasses implement the shape-specific functionality.
     *
     * @param <T> The output type of the stream pipeline
     */
    // "匹配"sink
    private abstract static class BooleanTerminalSink<T> implements Sink<T> {
        boolean stop;
        boolean value;
        
        BooleanTerminalSink(MatchKind matchKind) {
            value = !matchKind.shortCircuitResult;
        }
        
        public boolean getAndClearState() {
            return value;
        }
        
        @Override
        public boolean cancellationRequested() {
            return stop;
        }
    }
    
    /**
     * A short-circuiting {@code TerminalOp} that evaluates a predicate on the
     * elements of a stream and determines whether all, any or none of those
     * elements match the predicate.
     *
     * @param <T> the output type of the stream pipeline
     */
    // 用于"匹配"的终端操作
    private static final class MatchOp<T> implements TerminalOp<T, Boolean> {
        final MatchKind matchKind;
        final Supplier<BooleanTerminalSink<T>> sinkSupplier;
        private final StreamShape inputShape;
        
        /**
         * Constructs a {@code MatchOp}.
         *
         * @param shape        the output shape of the stream pipeline
         * @param matchKind    the kind of quantified match (all, any, none)
         * @param sinkSupplier {@code Supplier} for a {@code Sink} of the
         *                     appropriate shape which implements the matching operation
         */
        MatchOp(StreamShape shape, MatchKind matchKind, Supplier<BooleanTerminalSink<T>> sinkSupplier) {
            this.inputShape = shape;
            this.matchKind = matchKind;
            this.sinkSupplier = sinkSupplier;
        }
        
        @Override
        public int getOpFlags() {
            return StreamOpFlag.IS_SHORT_CIRCUIT | StreamOpFlag.NOT_ORDERED;
        }
        
        @Override
        public StreamShape inputShape() {
            return inputShape;
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
        public <S> Boolean evaluateSequential(PipelineHelper<T> helper, Spliterator<S> spliterator) {
            BooleanTerminalSink<T> terminalSink = sinkSupplier.get();
            
            /*
             * 从terminalSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
             * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
             * 最终择取出的数据往往被存入了terminalSink代表的容器当中。
             *
             * terminalSink: (相对于helper的)下个流阶段的sink。如果terminalSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             * spliterator : 流迭代器，作为数据源，包含了当前所有待访问的元素
             */
            helper.wrapAndCopyInto(terminalSink, spliterator);
            
            // 返回操作结果
            return terminalSink.getAndClearState();
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
        public <S> Boolean evaluateParallel(PipelineHelper<T> helper, Spliterator<S> spliterator) {
            // Approach for parallel implementation:
            // - Decompose as per usual
            // - run match on leaf chunks, call result "b"
            // - if b == matchKind.shortCircuitOn, complete early and return b
            // - else if we complete normally, return !shortCircuitOn
            
            return new MatchTask<>(this, helper, spliterator).invoke();
        }
    }
    
    /**
     * ForkJoinTask implementation to implement a parallel short-circuiting
     * quantified match
     *
     * @param <P_IN>  the type of source elements for the pipeline
     * @param <P_OUT> the type of output elements for the pipeline
     */
    // "匹配"任务
    @SuppressWarnings("serial")
    private static final class MatchTask<P_IN, P_OUT> extends AbstractShortCircuitTask<P_IN, P_OUT, Boolean, MatchTask<P_IN, P_OUT>> {
        private final MatchOp<P_OUT> op;
        
        /**
         * Constructor for root node
         */
        MatchTask(MatchOp<P_OUT> op, PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator) {
            super(helper, spliterator);
            this.op = op;
        }
        
        /**
         * Constructor for non-root node
         */
        MatchTask(MatchTask<P_IN, P_OUT> parent, Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            this.op = parent.op;
        }
        
        @Override
        protected MatchTask<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator) {
            return new MatchTask<>(this, spliterator);
        }
        
        // 返回任务执行结果，后续会将完成消息向上传播通知父级
        @Override
        protected Boolean doLeaf() {
            BooleanTerminalSink<P_OUT> terminalSink = op.sinkSupplier.get();
            
            /*
             * 从terminalSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
             * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
             * 最终择取出的数据往往被存入了terminalSink代表的容器当中。
             *
             * terminalSink: (相对于helper的)下个流阶段的sink。如果terminalSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             * spliterator : 流迭代器，作为数据源，包含了当前所有待访问的元素
             */
            helper.wrapAndCopyInto(terminalSink, spliterator);
            
            boolean result = terminalSink.getAndClearState();
            
            /*
             * 如果存在有效的执行结果，则设置共享结果。
             *
             * 由于执行结果的默认值与shortCircuitResult相反，
             * 所以如果发现执行结果与shortCircuitResult相同了，说明是出现了有效的执行结果。
             */
            if(result == op.matchKind.shortCircuitResult) {
                shortCircuit(result);
            }
            
            return null;
        }
        
        // 返回一个空结果，其实返回的是一个默认结果
        @Override
        protected Boolean getEmptyResult() {
            return !op.matchKind.shortCircuitResult;
        }
    }
    
}

