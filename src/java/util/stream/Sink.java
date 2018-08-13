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
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * An extension of {@link Consumer} used to conduct values through the stages of
 * a stream pipeline, with additional methods to manage size information,
 * control flow, etc.  Before calling the {@code accept()} method on a
 * {@code Sink} for the first time, you must first call the {@code begin()}
 * method to inform it that data is coming (optionally informing the sink how
 * much data is coming), and after all data has been sent, you must call the
 * {@code end()} method.  After calling {@code end()}, you should not call
 * {@code accept()} without again calling {@code begin()}.  {@code Sink} also
 * offers a mechanism by which the sink can cooperatively signal that it does
 * not wish to receive any more data (the {@code cancellationRequested()}
 * method), which a source can poll before sending more data to the
 * {@code Sink}.
 *
 * <p>A sink may be in one of two states: an initial state and an active state.
 * It starts out in the initial state; the {@code begin()} method transitions
 * it to the active state, and the {@code end()} method transitions it back into
 * the initial state, where it can be re-used.  Data-accepting methods (such as
 * {@code accept()} are only valid in the active state.
 *
 * @param <T> type of elements for value streams
 *
 * @apiNote A stream pipeline consists of a source, zero or more intermediate stages
 * (such as filtering or mapping), and a terminal stage, such as reduction or
 * for-each.  For concreteness, consider the pipeline:
 *
 * <pre>{@code
 *     int longestStringLengthStartingWithA
 *         = strings.stream()
 *                  .filter(s -> s.startsWith("A"))
 *                  .mapToInt(String::length)
 *                  .max();
 * }</pre>
 *
 * <p>Here, we have three stages, filtering, mapping, and reducing.  The
 * filtering stage consumes strings and emits a subset of those strings; the
 * mapping stage consumes strings and emits ints; the reduction stage consumes
 * those ints and computes the maximal value.
 *
 * <p>A {@code Sink} instance is used to represent each stage of this pipeline,
 * whether the stage accepts objects, ints, longs, or doubles.  Sink has entry
 * points for {@code accept(Object)}, {@code accept(int)}, etc, so that we do
 * not need a specialized interface for each primitive specialization.  (It
 * might be called a "kitchen sink" for this omnivorous tendency.)  The entry
 * point to the pipeline is the {@code Sink} for the filtering stage, which
 * sends some elements "downstream" -- into the {@code Sink} for the mapping
 * stage, which in turn sends integral values downstream into the {@code Sink}
 * for the reduction stage. The {@code Sink} implementations associated with a
 * given stage is expected to know the data type for the next stage, and call
 * the correct {@code accept} method on its downstream {@code Sink}.  Similarly,
 * each stage must implement the correct {@code accept} method corresponding to
 * the data type it accepts.
 *
 * <p>The specialized subtypes such as {@link Sink.OfInt} override
 * {@code accept(Object)} to call the appropriate primitive specialization of
 * {@code accept}, implement the appropriate primitive specialization of
 * {@code Consumer}, and re-abstract the appropriate primitive specialization of
 * {@code accept}.
 *
 * <p>The chaining subtypes such as {@link ChainedInt} not only implement
 * {@code Sink.OfInt}, but also maintain a {@code downstream} field which
 * represents the downstream {@code Sink}, and implement the methods
 * {@code begin()}, {@code end()}, and {@code cancellationRequested()} to
 * delegate to the downstream {@code Sink}.  Most implementations of
 * intermediate operations will use these chaining wrappers.  For example, the
 * mapping stage in the above example would look like:
 *
 * <pre>{@code
 *     IntSink is = new Sink.ChainedReference<U>(sink) {
 *         public void accept(U u) {
 *             downstream.accept(mapper.applyAsInt(u));
 *         }
 *     };
 * }</pre>
 *
 * <p>Here, we implement {@code Sink.ChainedReference<U>}, meaning that we expect
 * to receive elements of type {@code U} as input, and pass the downstream sink
 * to the constructor.  Because the next stage expects to receive integers, we
 * must call the {@code accept(int)} method when emitting values to the downstream.
 * The {@code accept()} method applies the mapping function from {@code U} to
 * {@code int} and passes the resulting value to the downstream {@code Sink}.
 * @since 1.8
 */

/*
 * Sink定义了开启(begin)、关闭(end)流水线的方式，并完成对数据的择取(accept)操作
 *
 * 在流水线的非源头阶段都包含各自的Sink，这些Sink往往形成一个单链表，以便从头到尾择取元素。
 *
 * Sink是Consumer的子类，可译作水槽（带滤网的水槽，择取数据）。
 * Sink可以看做一个特殊的函数，使用此函数来择取出自己想要的数据
 *
 * 在第一次调用Sink上的accept()方法之前，必须首先调用begin()方法以通知它数据即将到来（可选地通知Sink将要传输多少数据），以及发送完所有数据后，必须调用end()方法。
 * 在调用end()之后，如果不再调用begin()，则不应调用accept()。
 *
 * Sink还提供了一种机制，通过该机制，Sink可以协作地发信号通知它不希望再接收任何数据（cancellationRequested()方法），源可以在向Sink发送更多数据之前进行轮询。
 *
 * Sink可以处于两种状态之一：初始状态和活动状态。
 * begin()方法将其从初始状态转换为活动状态，end()方法将其转换回初始状态，可以重新使用它。
 * 数据择取方法（例如accept()）仅在活动状态下有效。
 *
 * 流水线由源头阶段，零个或多个中间阶段（例如filter或map）以及终端阶段（例如reduction或foreach）组成。
 */
interface Sink<T> extends Consumer<T> {
    
    /**
     * Resets the sink state to receive a fresh data set.  This must be called
     * before sending any data to the sink.  After calling {@link #end()},
     * you may call this method to reset the sink for another calculation.
     *
     * @param size The exact size of the data to be pushed downstream, if known or {@code -1} if unknown or infinite.
     *             Prior to this call, the sink must be in the initial state, and after this call it is in the active state.
     */
    /*
     * 重置Sink的状态以接收新数据集，必须在将任何数据发送到Sink之前调用。
     * 在调用end()之后，可以调用此方法重置Sink以进行另一次计算。
     * size代表向下游发送的数据量。如果数据量未知或无限，则设size==-1
     */
    default void begin(long size) {
    }
    
    /* 在父类Consumer中，有接收对象值的接口方法 */
    
    /**
     * Accepts an int value.
     *
     * @throws IllegalStateException if this sink does not accept int values
     * @implSpec The default implementation throws IllegalStateException.
     */
    // 接收int值，并进行相应的择取操作
    default void accept(int value) {
        throw new IllegalStateException("called wrong accept method");
    }
    
    /**
     * Accepts a long value.
     *
     * @throws IllegalStateException if this sink does not accept long values
     * @implSpec The default implementation throws IllegalStateException.
     */
    // 接收long值，并进行相应的择取操作
    default void accept(long value) {
        throw new IllegalStateException("called wrong accept method");
    }
    
    /**
     * Accepts a double value.
     *
     * @throws IllegalStateException if this sink does not accept double values
     * @implSpec The default implementation throws IllegalStateException.
     */
    // 接收double值，并进行相应的择取操作
    default void accept(double value) {
        throw new IllegalStateException("called wrong accept method");
    }
    
    /**
     * Indicates that all elements have been pushed.  If the {@code Sink} is
     * stateful, it should send any stored state downstream at this time, and
     * should clear any accumulated state (and associated resources).
     *
     * <p>Prior to this call, the sink must be in the active state, and after
     * this call it is returned to the initial state.
     */
    /*
     * 表示已推送所有元素。如果Sink是有状态的，它应该在此时向下游发送任何存储状态，并且应该清除任何累积状态（和相关资源）。
     * 在此调用之前，接收器必须处于活动状态，并且在此调用之后它将返回到初始状态。
     */
    default void end() {
    }
    
    /**
     * Indicates that this {@code Sink} does not wish to receive any more data.
     *
     * @return true if cancellation is requested
     *
     * @implSpec The default implementation always returns false.
     */
    // 返回true表示此Sink不希望再接收任何数据。默认实现始终返回false。
    default boolean cancellationRequested() {
        return false;
    }
    
    /**
     * {@code Sink} that implements {@code Sink<Integer>}, re-abstracts
     * {@code accept(int)}, and wires {@code accept(Integer)} to bridge to
     * {@code accept(int)}.
     */
    // 为基本类型int特化的Sink，可以处理int和Integer
    interface OfInt extends Sink<Integer>, IntConsumer {
        @Override
        void accept(int value);
        
        // 默认实现中经过了拆箱操作
        @Override
        default void accept(Integer i) {
            if(Tripwire.ENABLED)
                Tripwire.trip(getClass(), "{0} calling Sink.OfInt.accept(Integer)");
            accept(i.intValue());
        }
    }
    
    /**
     * {@code Sink} that implements {@code Sink<Long>}, re-abstracts
     * {@code accept(long)}, and wires {@code accept(Long)} to bridge to
     * {@code accept(long)}.
     */
    // 为基本类型long特化的Sink，可以处理long和Long
    interface OfLong extends Sink<Long>, LongConsumer {
        @Override
        void accept(long value);
        
        // 默认实现中经过了拆箱操作
        @Override
        default void accept(Long i) {
            if(Tripwire.ENABLED)
                Tripwire.trip(getClass(), "{0} calling Sink.OfLong.accept(Long)");
            accept(i.longValue());
        }
    }
    
    /**
     * {@code Sink} that implements {@code Sink<Double>}, re-abstracts
     * {@code accept(double)}, and wires {@code accept(Double)} to bridge to
     * {@code accept(double)}.
     */
    // 为基本类型double特化的Sink，可以处理double和Double
    interface OfDouble extends Sink<Double>, DoubleConsumer {
        @Override
        void accept(double value);
        
        // 默认实现中经过了拆箱操作
        @Override
        default void accept(Double i) {
            if(Tripwire.ENABLED)
                Tripwire.trip(getClass(), "{0} calling Sink.OfDouble.accept(Double)");
            accept(i.doubleValue());
        }
    }
    
    /**
     * Abstract {@code Sink} implementation for creating chains of
     * sinks.  The {@code begin}, {@code end}, and
     * {@code cancellationRequested} methods are wired to chain to the
     * downstream {@code Sink}.  This implementation takes a downstream
     * {@code Sink} of unknown input shape and produces a {@code Sink<T>}.  The
     * implementation of the {@code accept()} method must call the correct
     * {@code accept()} method on the downstream {@code Sink}.
     */
    // 为链式操作特化的Sink
    abstract class ChainedReference<T, E_OUT> implements Sink<T> {
        // 保存了下游的Sink，以方便链式调用
        protected final Sink<? super E_OUT> downstream;
        
        public ChainedReference(Sink<? super E_OUT> downstream) {
            this.downstream = Objects.requireNonNull(downstream);
        }
        
        // 逐个激活下游的Sink，参数代表传给下一个Sink的元素个数
        @Override
        public void begin(long size) {
            downstream.begin(size);
        }
        
        // 逐个关闭下游的Sink，这里可用容器接收最终的值
        @Override
        public void end() {
            downstream.end();
        }
        
        // 返回true表示此Sink不希望再接收任何数据。默认实现始终返回false。
        @Override
        public boolean cancellationRequested() {
            // 这里需要判断整个sink链条
            return downstream.cancellationRequested();
        }
    }
    
    /**
     * Abstract {@code Sink} implementation designed for creating chains of
     * sinks.  The {@code begin}, {@code end}, and
     * {@code cancellationRequested} methods are wired to chain to the
     * downstream {@code Sink}.  This implementation takes a downstream
     * {@code Sink} of unknown input shape and produces a {@code Sink.OfInt}.
     * The implementation of the {@code accept()} method must call the correct
     * {@code accept()} method on the downstream {@code Sink}.
     */
    // 为IntStream链式操作特化的Sink
    abstract class ChainedInt<E_OUT> implements Sink.OfInt {
        // 保存了下游的Sink，以方便链式调用
        protected final Sink<? super E_OUT> downstream;
        
        public ChainedInt(Sink<? super E_OUT> downstream) {
            this.downstream = Objects.requireNonNull(downstream);
        }
        
        // 逐个激活下游的Sink，这里可初始化容器
        @Override
        public void begin(long size) {
            downstream.begin(size);
        }
        
        // 逐个关闭下游的Sink，这里可用容器接收最终的值
        @Override
        public void end() {
            downstream.end();
        }
        
        // 逐个通知下游Sink取消接收数据
        @Override
        public boolean cancellationRequested() {
            return downstream.cancellationRequested();
        }
    }
    
    /**
     * Abstract {@code Sink} implementation designed for creating chains of
     * sinks.  The {@code begin}, {@code end}, and
     * {@code cancellationRequested} methods are wired to chain to the
     * downstream {@code Sink}.  This implementation takes a downstream
     * {@code Sink} of unknown input shape and produces a {@code Sink.OfLong}.
     * The implementation of the {@code accept()} method must call the correct
     * {@code accept()} method on the downstream {@code Sink}.
     */
    // 为LongStream链式操作特化的Sink
    abstract class ChainedLong<E_OUT> implements Sink.OfLong {
        // 保存了下游的Sink，以方便链式调用
        protected final Sink<? super E_OUT> downstream;
        
        public ChainedLong(Sink<? super E_OUT> downstream) {
            this.downstream = Objects.requireNonNull(downstream);
        }
        
        // 逐个激活下游的Sink，这里可初始化容器
        @Override
        public void begin(long size) {
            downstream.begin(size);
        }
        
        // 逐个关闭下游的Sink，这里可用容器接收最终的值
        @Override
        public void end() {
            downstream.end();
        }
        
        // 逐个通知下游Sink取消接收数据
        @Override
        public boolean cancellationRequested() {
            return downstream.cancellationRequested();
        }
    }
    
    /**
     * Abstract {@code Sink} implementation designed for creating chains of
     * sinks.  The {@code begin}, {@code end}, and
     * {@code cancellationRequested} methods are wired to chain to the
     * downstream {@code Sink}.  This implementation takes a downstream
     * {@code Sink} of unknown input shape and produces a {@code Sink.OfDouble}.
     * The implementation of the {@code accept()} method must call the correct
     * {@code accept()} method on the downstream {@code Sink}.
     */
    // 为DoubleStream链式操作特化的Sink
    abstract class ChainedDouble<E_OUT> implements Sink.OfDouble {
        // 保存了下游的Sink，以方便链式调用
        protected final Sink<? super E_OUT> downstream;
        
        public ChainedDouble(Sink<? super E_OUT> downstream) {
            this.downstream = Objects.requireNonNull(downstream);
        }
        
        // 逐个激活下游的Sink，这里可初始化容器
        @Override
        public void begin(long size) {
            downstream.begin(size);
        }
        
        // 逐个关闭下游的Sink，这里可用容器接收最终的值
        @Override
        public void end() {
            downstream.end();
        }
        
        // 逐个通知下游Sink取消接收数据
        @Override
        public boolean cancellationRequested() {
            return downstream.cancellationRequested();
        }
    }
}
