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

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

/**
 * Factory for creating instances of {@code TerminalOp} that implement
 * reductions.
 *
 * @since 1.8
 */
// 应用在终端阶段的辅助类，服务于汇总操作
final class ReduceOps {
    
    private ReduceOps() {
    }
    
    
    /**
     * Constructs a {@code TerminalOp} that implements a functional reduce on
     * reference values producing an optional reference result.
     *
     * @param <T>      The type of the input elements, and the type of the result
     * @param operator The reducing function
     *
     * @return A {@code TerminalOp} implementing the reduction
     */
    /*
     * 无初始状态的汇总操作(引用类型版本)
     *
     * 尝试将遇到的每个数据与上一个状态做operator操作后，将汇总结果保存到上一次的状态值中。
     * 未设置初始状态，所以每个(子)任务只是专注处理它自身遇到的数据源。
     *
     * 例如：
     * Stream.of(1, 2, 3, 4, 5).reduce((a, b) -> a + b)
     * 这会将1、2、3、4、5累加起来。
     *
     * operator: 两种用途：
     *           1.用于择取操作，如果是并行流，则用在每个子任务中
     *           2.用于并行流的合并操作
     */
    public static <T> TerminalOp<T, Optional<T>> makeRef(BinaryOperator<T> operator) {
        Objects.requireNonNull(operator);
        
        class ReducingSink implements AccumulatingSink<T, Optional<T>, ReducingSink> {
            private boolean empty;
            private T state;
            
            public void begin(long size) {
                empty = true;
                state = null;
            }
            
            @Override
            public void accept(T t) {
                if(empty) {
                    empty = false;
                    state = t;
                } else {
                    state = operator.apply(state, t);
                }
            }
            
            @Override
            public void combine(ReducingSink other) {
                if(!other.empty) {
                    accept(other.state);
                }
            }
            
            @Override
            public Optional<T> get() {
                return empty ? Optional.empty() : Optional.of(state);
            }
            
        }
        
        return new ReduceOp<T, Optional<T>, ReducingSink>(StreamShape.REFERENCE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that implements a functional reduce on
     * {@code int} values, producing an optional integer result.
     *
     * @param operator the combining function
     *
     * @return a {@code TerminalOp} implementing the reduction
     */
    // 无初始状态的汇总操作(int类型版本)
    public static TerminalOp<Integer, OptionalInt> makeInt(IntBinaryOperator operator) {
        Objects.requireNonNull(operator);
        
        class ReducingSink implements AccumulatingSink<Integer, OptionalInt, ReducingSink>, Sink.OfInt {
            private boolean empty;
            private int state;
            
            public void begin(long size) {
                empty = true;
                state = 0;
            }
            
            @Override
            public void accept(int t) {
                if(empty) {
                    empty = false;
                    state = t;
                } else {
                    state = operator.applyAsInt(state, t);
                }
            }
            
            @Override
            public OptionalInt get() {
                return empty ? OptionalInt.empty() : OptionalInt.of(state);
            }
            
            @Override
            public void combine(ReducingSink other) {
                if(!other.empty)
                    accept(other.state);
            }
        }
        
        return new ReduceOp<Integer, OptionalInt, ReducingSink>(StreamShape.INT_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that implements a functional reduce on
     * {@code long} values, producing an optional long result.
     *
     * @param operator the combining function
     *
     * @return a {@code TerminalOp} implementing the reduction
     */
    // 无初始状态的汇总操作(long类型版本)
    public static TerminalOp<Long, OptionalLong> makeLong(LongBinaryOperator operator) {
        Objects.requireNonNull(operator);
        
        class ReducingSink implements AccumulatingSink<Long, OptionalLong, ReducingSink>, Sink.OfLong {
            private boolean empty;
            private long state;
            
            public void begin(long size) {
                empty = true;
                state = 0;
            }
            
            @Override
            public void accept(long t) {
                if(empty) {
                    empty = false;
                    state = t;
                } else {
                    state = operator.applyAsLong(state, t);
                }
            }
            
            @Override
            public OptionalLong get() {
                return empty ? OptionalLong.empty() : OptionalLong.of(state);
            }
            
            @Override
            public void combine(ReducingSink other) {
                if(!other.empty)
                    accept(other.state);
            }
        }
        
        return new ReduceOp<Long, OptionalLong, ReducingSink>(StreamShape.LONG_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that implements a functional reduce on
     * {@code double} values, producing an optional double result.
     *
     * @param operator the combining function
     *
     * @return a {@code TerminalOp} implementing the reduction
     */
    // 无初始状态的汇总操作(double类型版本)
    public static TerminalOp<Double, OptionalDouble> makeDouble(DoubleBinaryOperator operator) {
        Objects.requireNonNull(operator);
        
        class ReducingSink implements AccumulatingSink<Double, OptionalDouble, ReducingSink>, Sink.OfDouble {
            private boolean empty;
            private double state;
            
            public void begin(long size) {
                empty = true;
                state = 0;
            }
            
            @Override
            public void accept(double t) {
                if(empty) {
                    empty = false;
                    state = t;
                } else {
                    state = operator.applyAsDouble(state, t);
                }
            }
            
            @Override
            public OptionalDouble get() {
                return empty ? OptionalDouble.empty() : OptionalDouble.of(state);
            }
            
            @Override
            public void combine(ReducingSink other) {
                if(!other.empty)
                    accept(other.state);
            }
        }
        
        return new ReduceOp<Double, OptionalDouble, ReducingSink>(StreamShape.DOUBLE_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    
    /**
     * Constructs a {@code TerminalOp} that implements a functional reduce on
     * reference values.
     *
     * @param <T>      the type of the input elements
     * @param <U>      the type of the result
     * @param seed     the identity element for the reduction
     * @param reducer  the accumulating function that incorporates an additional
     *                 input element into the result
     * @param combiner the combining function that combines two intermediate
     *                 results
     *
     * @return a {@code TerminalOp} implementing the reduction
     */
    /*
     * 有初始状态的汇总操作(引用类型版本)
     *
     * 尝试将遇到的每个数据与上一个状态做汇总操作后，将汇总结果保存到上一次的状态值中。
     * 这里提供了两个操作：reducer用于在单个任务中择取数据，而combiner用于在并行流中合并多个子任务。
     * 这里需要设定一个初始状态seed，所以每个(子)任务在处理它自身遇到的数据源之前，首先要与该初始状态进行汇总。
     *
     * 例如：
     * Stream.of(1, 2, 3, 4, 5).reduce(-1, (a, b) -> a + b, (a, b) -> a + b)
     * 这是顺序流，操作结果是将-1、1、2、3、4、5累加起来，结果是14。
     *
     * Stream.of(1, 2, 3, 4, 5).parallel().reduce(-1, (a, b) -> a + b, (a, b) -> a + b)
     * 这是并行流，虽然使用的择取方法与顺序流相同，但不同的是这里需要先将数据源拆分到各个子任务中。
     * 根据默认的二分法拆分规则，上面的数据会被拆分为(1)、(2)、(3)、(4)、(5)这五组，
     * 由于这五组数据位于五个子任务中，那么每个子任务择取数据之时都会先与那个初始值-1去做汇总，
     * 即五个子任务的执行结果分别是：0、1、2、3、4，
     * 最后，将这5个子任务用combiner合并起来，那就是0+1+2+3+4 = 10
     *
     * seed    : 每个(子)任务需要使用的初始状态
     * reducer : 用于择取操作，如果是并行流，则用在每个叶子任务中
     * combiner: 用于并行流的合并子任务操作
     *
     * 注：通常来讲，要求reducer和combiner相呼应
     */
    public static <T, U> TerminalOp<T, U> makeRef(U seed, BiFunction<U, ? super T, U> reducer, BinaryOperator<U> combiner) {
        Objects.requireNonNull(reducer);
        Objects.requireNonNull(combiner);
        
        class ReducingSink extends Box<U> implements AccumulatingSink<T, U, ReducingSink> {
            @Override
            public void begin(long size) {
                state = seed;
            }
            
            @Override
            public void accept(T t) {
                state = reducer.apply(state, t);
            }
            
            @Override
            public void combine(ReducingSink other) {
                state = combiner.apply(state, other.state);
            }
        }
        
        return new ReduceOp<T, U, ReducingSink>(StreamShape.REFERENCE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that implements a functional reduce on
     * {@code int} values.
     *
     * @param identity the identity for the combining function
     * @param operator the combining function
     *
     * @return a {@code TerminalOp} implementing the reduction
     */
    // 有初始状态的汇总操作(int类型版本)
    public static TerminalOp<Integer, Integer> makeInt(int identity, IntBinaryOperator operator) {
        Objects.requireNonNull(operator);
        
        class ReducingSink implements AccumulatingSink<Integer, Integer, ReducingSink>, Sink.OfInt {
            private int state;
            
            @Override
            public void begin(long size) {
                state = identity;
            }
            
            @Override
            public void accept(int t) {
                state = operator.applyAsInt(state, t);
            }
            
            @Override
            public Integer get() {
                return state;
            }
            
            @Override
            public void combine(ReducingSink other) {
                accept(other.state);
            }
        }
        
        return new ReduceOp<Integer, Integer, ReducingSink>(StreamShape.INT_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that implements a functional reduce on
     * {@code long} values.
     *
     * @param identity the identity for the combining function
     * @param operator the combining function
     *
     * @return a {@code TerminalOp} implementing the reduction
     */
    // 有初始状态的汇总操作(long类型版本)
    public static TerminalOp<Long, Long> makeLong(long identity, LongBinaryOperator operator) {
        Objects.requireNonNull(operator);
        
        class ReducingSink implements AccumulatingSink<Long, Long, ReducingSink>, Sink.OfLong {
            private long state;
            
            @Override
            public void begin(long size) {
                state = identity;
            }
            
            @Override
            public void accept(long t) {
                state = operator.applyAsLong(state, t);
            }
            
            @Override
            public Long get() {
                return state;
            }
            
            @Override
            public void combine(ReducingSink other) {
                accept(other.state);
            }
        }
        
        return new ReduceOp<Long, Long, ReducingSink>(StreamShape.LONG_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that implements a functional reduce on
     * {@code double} values.
     *
     * @param identity the identity for the combining function
     * @param operator the combining function
     *
     * @return a {@code TerminalOp} implementing the reduction
     */
    // 有初始状态的汇总操作(double类型版本)
    public static TerminalOp<Double, Double> makeDouble(double identity, DoubleBinaryOperator operator) {
        Objects.requireNonNull(operator);
        
        class ReducingSink implements AccumulatingSink<Double, Double, ReducingSink>, Sink.OfDouble {
            private double state;
            
            @Override
            public void begin(long size) {
                state = identity;
            }
            
            @Override
            public void accept(double t) {
                state = operator.applyAsDouble(state, t);
            }
            
            @Override
            public Double get() {
                return state;
            }
            
            @Override
            public void combine(ReducingSink other) {
                accept(other.state);
            }
        }
        
        return new ReduceOp<Double, Double, ReducingSink>(StreamShape.DOUBLE_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    
    /**
     * Constructs a {@code TerminalOp} that implements a mutable reduce on reference values.
     *
     * @param <T>         the type of the input elements
     * @param <R>         the type of the result
     * @param seedFactory a factory to produce a new base accumulator
     * @param accumulator a function to incorporate an element into an accumulator
     * @param reducer     a function to combine an accumulator into another
     *
     * @return a {@code TerminalOp} implementing the reduction
     */
    /*
     * 有初始状态的消费操作(引用类型版本)
     *
     * 注：这里的消费通常是将遇到的元素存储到某个容器中。
     *
     * 尝试将遇到的每个数据与上一个状态做汇总操作后，汇总过程是一个消费过程，在消费中如何处理状态值，由该方法的入参决定。
     * 通常来说，我们会让seedFactory生成一个代表容器的"初始状态"，然后在消费过程中，把遇到的元素收纳到该容器当中。
     * 这里提供了两个操作：accumulator用于在单个任务中择取数据，而combiner用于在并行流中合并多个子任务。
     * 这里需要设定一个初始状态的工厂seedFactory，所以每个(子)任务在处理它自身遇到的数据源之前，首先要与该初始状态进行汇总。
     *
     * 例如：
     *
     * 假设有如下两个操作：
     * BiConsumer<ArrayList<Integer>, Integer> accumulator = (list, e) -> list.add(e);
     *
     * BiConsumer<ArrayList<Integer>, ArrayList<Integer>> combiner = (list1, list2) -> {
     *     for(Integer e : list2) {
     *         if(!list1.contains(e)) {
     *             list1.add(e);
     *         }
     *     }
     * };
     *
     *
     * Stream<Integer> stream = Stream.of(3, 2, 3, 1, 2);
     * ArrayList<Integer> list = stream.collect(() -> new ArrayList<Integer>(), accumulator, combiner);
     * 这是顺序流，操作结果是将3、2、3、1、2全部收集到list中。
     *
     * Stream<Integer> stream = Stream.of(3, 2, 3, 1, 2).parallel();
     * ArrayList<Integer> list = stream.collect(() -> new ArrayList<Integer>(), accumulator, combiner);
     * 这是并行流，虽然使用的择取方法与顺序流相同，但不同的是这里需要先将数据源拆分到各个子任务中
     * 根据默认的二分法拆分规则，上面的数据会被拆分为(1)、(2)、(3)、(4)、(5)这五组
     * 由于这五组数据位于五个子任务中，那么每个子任务择取数据之时都会先与那个初始状态做汇总。
     * 此处给出的初始状态就是一个list，操作目标就是将遇到的元素添加到该list中。
     * 因此在每个叶子任务完成后，其对应的元素就被添加到了list中。
     * 接下来，使用combiner对子任务汇总。这里的操作是遍历list2中的元素，找出那些不在list1中的元素，并将其添加到list1中。
     * 因此最终的汇总结果中只有3、2、1。
     *
     * seedFactory: 初始状态工厂
     * accumulator: 用于择取操作，如果是并行流，则用在每个叶子任务中
     * combiner   : 用于并行流的合并子任务操作
     *
     * 注：通常来讲，要求accumulator和combiner相呼应
     */
    public static <T, R> TerminalOp<T, R> makeRef(Supplier<R> seedFactory, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        Objects.requireNonNull(seedFactory);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        
        // 目标sink
        class ReducingSink extends Box<R> implements AccumulatingSink<T, R, ReducingSink> {
            @Override
            public void begin(long size) {
                state = seedFactory.get();
            }
            
            @Override
            public void accept(T t) {
                accumulator.accept(state, t);
            }
            
            @Override
            public void combine(ReducingSink other) {
                combiner.accept(state, other.state);
            }
        }
        
        return new ReduceOp<T, R, ReducingSink>(StreamShape.REFERENCE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that implements a mutable reduce on
     * {@code int} values.
     *
     * @param <R>         The type of the result
     * @param supplier    a factory to produce a new accumulator of the result type
     * @param accumulator a function to incorporate an int into an
     *                    accumulator
     * @param combiner    a function to combine an accumulator into another
     *
     * @return A {@code ReduceOp} implementing the reduction
     */
    // 有初始状态的消费操作(int类型版本)
    public static <R> TerminalOp<Integer, R> makeInt(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BinaryOperator<R> combiner) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        
        class ReducingSink extends Box<R> implements AccumulatingSink<Integer, R, ReducingSink>, Sink.OfInt {
            @Override
            public void begin(long size) {
                state = supplier.get();
            }
            
            @Override
            public void accept(int t) {
                accumulator.accept(state, t);
            }
            
            @Override
            public void combine(ReducingSink other) {
                state = combiner.apply(state, other.state);
            }
        }
        
        return new ReduceOp<Integer, R, ReducingSink>(StreamShape.INT_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that implements a mutable reduce on
     * {@code long} values.
     *
     * @param <R>         the type of the result
     * @param supplier    a factory to produce a new accumulator of the result type
     * @param accumulator a function to incorporate an int into an
     *                    accumulator
     * @param combiner    a function to combine an accumulator into another
     *
     * @return a {@code TerminalOp} implementing the reduction
     */
    // 有初始状态的消费操作(long类型版本)
    public static <R> TerminalOp<Long, R> makeLong(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BinaryOperator<R> combiner) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        
        class ReducingSink extends Box<R> implements AccumulatingSink<Long, R, ReducingSink>, Sink.OfLong {
            @Override
            public void begin(long size) {
                state = supplier.get();
            }
            
            @Override
            public void accept(long t) {
                accumulator.accept(state, t);
            }
            
            @Override
            public void combine(ReducingSink other) {
                state = combiner.apply(state, other.state);
            }
        }
        
        return new ReduceOp<Long, R, ReducingSink>(StreamShape.LONG_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that implements a mutable reduce on
     * {@code double} values.
     *
     * @param <R>         the type of the result
     * @param supplier    a factory to produce a new accumulator of the result type
     * @param accumulator a function to incorporate an int into an
     *                    accumulator
     * @param combiner    a function to combine an accumulator into another
     *
     * @return a {@code TerminalOp} implementing the reduction
     */
    // 有初始状态的消费操作(double类型版本)
    public static <R> TerminalOp<Double, R> makeDouble(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BinaryOperator<R> combiner) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        
        class ReducingSink extends Box<R> implements AccumulatingSink<Double, R, ReducingSink>, Sink.OfDouble {
            @Override
            public void begin(long size) {
                state = supplier.get();
            }
            
            @Override
            public void accept(double t) {
                accumulator.accept(state, t);
            }
            
            @Override
            public void combine(ReducingSink other) {
                state = combiner.apply(state, other.state);
            }
        }
        
        return new ReduceOp<Double, R, ReducingSink>(StreamShape.DOUBLE_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
    }
    
    
    /**
     * Constructs a {@code TerminalOp} that implements a mutable reduce on
     * reference values.
     *
     * @param <T>       the type of the input elements
     * @param <I>       the type of the intermediate reduction result
     * @param collector a {@code Collector} defining the reduction
     *
     * @return a {@code ReduceOp} implementing the reduction
     */
    // 依赖收集器的汇总操作(引用类型版本)
    public static <T, I> TerminalOp<T, I> makeRef(Collector<? super T, I, ?> collector) {
        
        // 收集器工厂
        Supplier<I> supplier = Objects.requireNonNull(collector).supplier();
        // 择取操作：(子)任务如何处理遇到的每个元素
        BiConsumer<I, ? super T> accumulator = collector.accumulator();
        // 合并操作：并行流中如何合并子任务
        BinaryOperator<I> combiner = collector.combiner();
        
        class ReducingSink extends Box<I> implements AccumulatingSink<T, I, ReducingSink> {
            @Override
            public void begin(long size) {
                state = supplier.get();
            }
            
            @Override
            public void accept(T t) {
                accumulator.accept(state, t);
            }
            
            @Override
            public void combine(ReducingSink other) {
                state = combiner.apply(state, other.state);
            }
        }
        
        return new ReduceOp<T, I, ReducingSink>(StreamShape.REFERENCE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
            
            @Override
            public int getOpFlags() {
                return collector.characteristics().contains(Collector.Characteristics.UNORDERED) ? StreamOpFlag.NOT_ORDERED : 0;
            }
        };
    }
    
    
    /**
     * Constructs a {@code TerminalOp} that counts the number of stream
     * elements.  If the size of the pipeline is known then count is the size
     * and there is no need to evaluate the pipeline.  If the size of the
     * pipeline is non known then count is produced, via reduction, using a
     * {@link CountingSink}.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code TerminalOp} implementing the counting
     */
    // 计数操作(引用类型版本)
    public static <T> TerminalOp<T, Long> makeRefCounting() {
        return new ReduceOp<T, Long, CountingSink<T>>(StreamShape.REFERENCE) {
            
            // 返回一个"计数"sink，用来统计经过当前sink过滤后输出的元素数量
            @Override
            public CountingSink<T> makeSink() {
                return new CountingSink.OfRef<>();
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
            public <P_IN> Long evaluateSequential(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                /*
                 * 如果helper流阶段的元素数量有限，则可以直接返回其元素数量，
                 * 因为当前终端操作的目的就是获取元素数量。
                 */
                if(StreamOpFlag.SIZED.isKnown(streamAndOpFlags)) {
                    return spliterator.getExactSizeIfKnown();
                }
                
                // 否则，执行上述makeSink()构造出的终端sink，来统计spliterator流阶段元素数量
                return super.evaluateSequential(helper, spliterator);
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
            public <P_IN> Long evaluateParallel(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有限，则此处可以直接获取到固定的元素数量
                if(StreamOpFlag.SIZED.isKnown(streamAndOpFlags)) {
                    return spliterator.getExactSizeIfKnown();
                }
                
                return super.evaluateParallel(helper, spliterator);
            }
            
            @Override
            public int getOpFlags() {
                return StreamOpFlag.NOT_ORDERED;
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that counts the number of stream
     * elements.  If the size of the pipeline is known then count is the size
     * and there is no need to evaluate the pipeline.  If the size of the
     * pipeline is non known then count is produced, via reduction, using a
     * {@link CountingSink}.
     *
     * @return a {@code TerminalOp} implementing the counting
     */
    // 计数操作(int类型版本)
    public static TerminalOp<Integer, Long> makeIntCounting() {
        return new ReduceOp<Integer, Long, CountingSink<Integer>>(StreamShape.INT_VALUE) {
            
            // 返回一个"计数"sink，用来统计经过当前sink过滤后输出的元素数量
            @Override
            public CountingSink<Integer> makeSink() {
                return new CountingSink.OfInt();
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
            public <P_IN> Long evaluateSequential(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                if(StreamOpFlag.SIZED.isKnown(streamAndOpFlags)) {
                    return spliterator.getExactSizeIfKnown();
                }
                
                return super.evaluateSequential(helper, spliterator);
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
            public <P_IN> Long evaluateParallel(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有限，则此处可以直接获取到固定的元素数量
                if(StreamOpFlag.SIZED.isKnown(streamAndOpFlags)) {
                    return spliterator.getExactSizeIfKnown();
                }
                
                return super.evaluateParallel(helper, spliterator);
            }
            
            @Override
            public int getOpFlags() {
                return StreamOpFlag.NOT_ORDERED;
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that counts the number of stream
     * elements.  If the size of the pipeline is known then count is the size
     * and there is no need to evaluate the pipeline.  If the size of the
     * pipeline is non known then count is produced, via reduction, using a
     * {@link CountingSink}.
     *
     * @return a {@code TerminalOp} implementing the counting
     */
    // 计数操作(long类型版本)
    public static TerminalOp<Long, Long> makeLongCounting() {
        return new ReduceOp<Long, Long, CountingSink<Long>>(StreamShape.LONG_VALUE) {
            
            // 返回一个"计数"sink，用来统计经过当前sink过滤后输出的元素数量
            @Override
            public CountingSink<Long> makeSink() {
                return new CountingSink.OfLong();
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
            public <P_IN> Long evaluateSequential(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有限，则此处可以直接获取到固定的元素数量
                if(StreamOpFlag.SIZED.isKnown(streamAndOpFlags)) {
                    return spliterator.getExactSizeIfKnown();
                }
                
                return super.evaluateSequential(helper, spliterator);
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
            public <P_IN> Long evaluateParallel(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有限，则此处可以直接获取到固定的元素数量
                if(StreamOpFlag.SIZED.isKnown(streamAndOpFlags)) {
                    return spliterator.getExactSizeIfKnown();
                }
                
                return super.evaluateParallel(helper, spliterator);
            }
            
            @Override
            public int getOpFlags() {
                return StreamOpFlag.NOT_ORDERED;
            }
        };
    }
    
    /**
     * Constructs a {@code TerminalOp} that counts the number of stream
     * elements.  If the size of the pipeline is known then count is the size
     * and there is no need to evaluate the pipeline.  If the size of the
     * pipeline is non known then count is produced, via reduction, using a
     * {@link CountingSink}.
     *
     * @return a {@code TerminalOp} implementing the counting
     */
    // 计数操作(double类型版本)
    public static TerminalOp<Double, Long> makeDoubleCounting() {
        return new ReduceOp<Double, Long, CountingSink<Double>>(StreamShape.DOUBLE_VALUE) {
            
            // 返回一个"计数"sink，用来统计经过当前sink过滤后输出的元素数量
            @Override
            public CountingSink<Double> makeSink() {
                return new CountingSink.OfDouble();
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
            public <P_IN> Long evaluateSequential(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有限，则此处可以直接获取到固定的元素数量
                if(StreamOpFlag.SIZED.isKnown(streamAndOpFlags)) {
                    return spliterator.getExactSizeIfKnown();
                }
                
                return super.evaluateSequential(helper, spliterator);
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
            public <P_IN> Long evaluateParallel(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator) {
                
                // 获取helper流阶段的组合参数
                int streamAndOpFlags = helper.getStreamAndOpFlags();
                
                // 如果helper流阶段的元素有限，则此处可以直接获取到固定的元素数量
                if(StreamOpFlag.SIZED.isKnown(streamAndOpFlags)) {
                    return spliterator.getExactSizeIfKnown();
                }
                
                return super.evaluateParallel(helper, spliterator);
            }
            
            @Override
            public int getOpFlags() {
                return StreamOpFlag.NOT_ORDERED;
            }
        };
    }
    
    
    /**
     * A type of {@code TerminalSink} that implements an associative reducing
     * operation on elements of type {@code T} and producing a result of type
     * {@code R}.
     *
     * @param <T> the type of input element to the combining operation
     * @param <R> the result type
     * @param <K> the type of the {@code AccumulatingSink}.
     */
    private interface AccumulatingSink<T, R, K extends AccumulatingSink<T, R, K>> extends TerminalSink<T, R> {
        // 用于在并行流中合并子任务
        void combine(K other);
    }
    
    /**
     * A sink that counts elements
     */
    // "计数"sink，用来统计经过当前sink过滤后输出的元素数量
    abstract static class CountingSink<T> extends Box<Long> implements AccumulatingSink<T, Long, CountingSink<T>> {
        
        long count;
        
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
            count = 0L;
        }
        
        @Override
        public Long get() {
            return count;
        }
        
        @Override
        public void combine(CountingSink<T> other) {
            count += other.count;
        }
        
        
        static final class OfRef<T> extends CountingSink<T> {
            @Override
            public void accept(T t) {
                count++;
            }
        }
        
        static final class OfInt extends CountingSink<Integer> implements Sink.OfInt {
            @Override
            public void accept(int t) {
                count++;
            }
        }
        
        static final class OfLong extends CountingSink<Long> implements Sink.OfLong {
            @Override
            public void accept(long t) {
                count++;
            }
        }
        
        static final class OfDouble extends CountingSink<Double> implements Sink.OfDouble {
            @Override
            public void accept(double t) {
                count++;
            }
        }
    }
    
    /**
     * State box for a single state element, used as a base class for
     * {@code AccumulatingSink} instances
     *
     * @param <U> The type of the state element
     */
    private abstract static class Box<U> {
        U state;
        
        // Avoid creation of special accessor
        Box() {
        }
        
        public U get() {
            return state;
        }
    }
    
    /**
     * A {@code TerminalOp} that evaluates a stream pipeline and sends the
     * output into an {@code AccumulatingSink}, which performs a reduce
     * operation. The {@code AccumulatingSink} must represent an associative
     * reducing operation.
     *
     * @param <T> the output type of the stream pipeline
     * @param <R> the result type of the reducing operation
     * @param <S> the type of the {@code AccumulatingSink}
     */
    // "汇总"操作
    private abstract static class ReduceOp<T, R, S extends AccumulatingSink<T, R, S>> implements TerminalOp<T, R> {
        private final StreamShape inputShape;
        
        /**
         * Create a {@code ReduceOp} of the specified stream shape which uses
         * the specified {@code Supplier} to create accumulating sinks.
         *
         * @param shape The shape of the stream pipeline
         */
        ReduceOp(StreamShape shape) {
            inputShape = shape;
        }
        
        // 构造一个目标sink作为终端sink，以便完成某些目标操作
        public abstract S makeSink();
        
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
        public <P_IN> R evaluateSequential(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
            
            // 构造终端阶段的sink，该sink可以用来收集数据、计数等
            S terminalSink = makeSink();
            
            /*
             * 从terminalSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
             * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
             * 最终择取出的数据往往被存入了terminalSink代表的容器当中。
             *
             * terminalSink: (相对于helper的)下个流阶段的sink。如果terminalSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             * spliterator : 流迭代器，作为数据源，包含了当前所有待访问的元素
             */
            helper.wrapAndCopyInto(terminalSink, spliterator);
            
            // 获取terminalSink的计算结果
            return terminalSink.get();
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
        public <P_IN> R evaluateParallel(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
            ReduceTask<P_IN, T, R, S> task = new ReduceTask<>(this, helper, spliterator);
            S terminalSink = task.invoke();
            return terminalSink.get();
        }
    }
    
    /**
     * A {@code ForkJoinTask} for performing a parallel reduce operation.
     */
    // "汇总"任务
    @SuppressWarnings("serial")
    private static final class ReduceTask<P_IN, P_OUT, R, S extends AccumulatingSink<P_OUT, R, S>> extends AbstractTask<P_IN, P_OUT, S, ReduceTask<P_IN, P_OUT, R, S>> {
        private final ReduceOp<P_OUT, R, S> op;
        
        ReduceTask(ReduceOp<P_OUT, R, S> op, PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator) {
            super(helper, spliterator);
            this.op = op;
        }
        
        ReduceTask(ReduceTask<P_IN, P_OUT, R, S> parent, Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            this.op = parent.op;
        }
        
        // 构造子任务
        @Override
        protected ReduceTask<P_IN, P_OUT, R, S> makeChild(Spliterator<P_IN> spliterator) {
            return new ReduceTask<>(this, spliterator);
        }
        
        @Override
        public void onCompletion(CountedCompleter<?> caller) {
            // 对于非叶子任务才需要进行合并
            if(!isLeaf()) {
                S leftResult = leftChild.getLocalResult();
                
                // 合并左右孩子结点上的任务
                leftResult.combine(rightChild.getLocalResult());
                
                // 设置执行结果
                setLocalResult(leftResult);
            }
            
            // GC spliterator, left and right child
            super.onCompletion(caller);
        }
        
        @Override
        protected S doLeaf() {
            S terminalSink = op.makeSink();
            
            /*
             * 从terminalSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
             * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
             * 最终择取出的数据往往被存入了terminalSink代表的容器当中。
             *
             * terminalSink: (相对于helper的)下个流阶段的sink。如果terminalSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             * spliterator : 流迭代器，作为数据源，包含了当前所有待访问的元素
             */
            helper.wrapAndCopyInto(terminalSink, spliterator);
            
            return terminalSink;
        }
    }
    
}
