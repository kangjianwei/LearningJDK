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
 * Sink水槽接口(引用类型版本)
 *
 * Sink直译作水槽，类如其名，Sink就是用接收上个Sink发来的数据的容器。
 *
 * 对于每个从上游发来的元素，都需要经过accept()方法的择取，以决定保留还是丢弃，或者做其他修改操作。
 *
 * 通常只有流的中间阶段和终端阶段需要使用Sink。
 *
 * 注：流式操作包含三个要素：Stream、Spliterator、Sink
 */
interface Sink<T> extends Consumer<T> {
    
    /**
     * Resets the sink state to receive a fresh data set.
     * This must be called before sending any data to the sink.
     * After calling {@link #end()}, you may call this method to reset the sink for another calculation.
     *
     * @param size The exact size of the data to be pushed downstream, if known or {@code -1} if unknown or infinite.
     *             Prior to this call, the sink must be in the initial state, and after this call it is in the active state.
     */
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
    default void begin(long size) {
    }
    
    /*
     * // 对上游发来的引用类型的值进行择取。
     * // 如果上游存在多个元素，该方法通常会被反复调用。
     * void accept(T value);
     *
     * 该方法存在于父接口Consumer中
     */
    
    /**
     * Accepts an int value.
     *
     * @throws IllegalStateException if this sink does not accept int values
     * @implSpec The default implementation throws IllegalStateException.
     */
    /*
     * 对上游发来的int类型的值进行择取。
     * 如果上游存在多个元素，该方法通常会被反复调用。
     */
    default void accept(int value) {
        throw new IllegalStateException("called wrong accept method");
    }
    
    /**
     * Accepts a long value.
     *
     * @throws IllegalStateException if this sink does not accept long values
     * @implSpec The default implementation throws IllegalStateException.
     */
    /*
     * 对上游发来的long类型的值进行择取。
     * 如果上游存在多个元素，该方法通常会被反复调用。
     */
    default void accept(long value) {
        throw new IllegalStateException("called wrong accept method");
    }
    
    /**
     * Accepts a double value.
     *
     * @throws IllegalStateException if this sink does not accept double values
     * @implSpec The default implementation throws IllegalStateException.
     */
    /*
     * 对上游发来的double类型的值进行择取。
     * 如果上游存在多个元素，该方法通常会被反复调用。
     */
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
     * 关闭sink链，结束本轮计算。
     *
     * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
     * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
     *
     * 该方法应当在已经得到目标数据之后被调用。
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
    /*
     * 判断是否应当停止接收数据。
     * 在一些短路操作中，可以在达到某个目标后取消后续的操作。
     */
    default boolean cancellationRequested() {
        return false;
    }
    
    
    /**
     * {@code Sink} that implements {@code Sink<Integer>}, re-abstracts
     * {@code accept(int)}, and wires {@code accept(Integer)} to bridge to
     * {@code accept(int)}.
     */
    // Sink水槽接口(int类型版本)
    interface OfInt extends Sink<Integer>, IntConsumer {
        
        @Override
        void accept(int value);
        
        // 默认实现中经过了拆箱操作
        @Override
        default void accept(Integer i) {
            if(Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Sink.OfInt.accept(Integer)");
            }
    
            accept(i.intValue());
        }
    }
    
    /**
     * {@code Sink} that implements {@code Sink<Long>}, re-abstracts
     * {@code accept(long)}, and wires {@code accept(Long)} to bridge to
     * {@code accept(long)}.
     */
    // Sink水槽接口(long类型版本)
    interface OfLong extends Sink<Long>, LongConsumer {
    
        @Override
        void accept(long value);
        
        // 默认实现中经过了拆箱操作
        @Override
        default void accept(Long i) {
            if(Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Sink.OfLong.accept(Long)");
            }
    
            accept(i.longValue());
        }
    }
    
    /**
     * {@code Sink} that implements {@code Sink<Double>}, re-abstracts
     * {@code accept(double)}, and wires {@code accept(Double)} to bridge to
     * {@code accept(double)}.
     */
    // Sink水槽接口(double类型版本)
    interface OfDouble extends Sink<Double>, DoubleConsumer {
    
        @Override
        void accept(double value);
        
        // 默认实现中经过了拆箱操作
        @Override
        default void accept(Double i) {
            if(Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling Sink.OfDouble.accept(Double)");
            }
    
            accept(i.doubleValue());
        }
    }
    
    
    
    
    
    
    /*▼ 链式Sink ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Abstract {@code Sink} implementation for creating chains of
     * sinks.  The {@code begin}, {@code end}, and
     * {@code cancellationRequested} methods are wired to chain to the
     * downstream {@code Sink}.  This implementation takes a downstream
     * {@code Sink} of unknown input shape and produces a {@code Sink<T>}.  The
     * implementation of the {@code accept()} method must call the correct
     * {@code accept()} method on the downstream {@code Sink}.
     */
    // 链式Sink的抽象实现(引用类型版本)
    abstract class ChainedReference<T, E_OUT> implements Sink<T> {
        
        // 保存了下游的Sink，以方便链式调用
        protected final Sink<? super E_OUT> downstream;
        
        public ChainedReference(Sink<? super E_OUT> downSink) {
            this.downstream = Objects.requireNonNull(downSink);
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
            downstream.begin(size);
        }
        
        /*
         * 关闭sink链，结束本轮计算
         *
         * 待处理完所有目标元素后(不一定要处理全部元素)，需要调用此方法。
         * 如果Sink是有状态的，则需要在此处清除之前存储的状态，并释放相关的资源。
         */
        @Override
        public void end() {
            downstream.end();
        }
        
        /*
         * 判断是否应当停止接收数据。
         * 在一些短路操作中，可以在达到某个目标后取消后续的操作。
         */
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
     * {@code Sink} of unknown input shape and produces a {@code Sink.OfInt}.
     * The implementation of the {@code accept()} method must call the correct
     * {@code accept()} method on the downstream {@code Sink}.
     */
    // 链式Sink的抽象实现(int类型版本)
    abstract class ChainedInt<E_OUT> implements Sink.OfInt {
    
        // 保存了下游的Sink，以方便链式调用
        protected final Sink<? super E_OUT> downstream;
    
        public ChainedInt(Sink<? super E_OUT> downSink) {
            this.downstream = Objects.requireNonNull(downSink);
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
            downstream.begin(size);
        }
    
        /*
         * 关闭sink链，结束本轮计算
         *
         * 待处理完所有目标元素后(不一定要处理全部元素)，需要调用此方法。
         * 如果Sink是有状态的，则需要在此处清除之前存储的状态，并释放相关的资源。
         */
        @Override
        public void end() {
            downstream.end();
        }
    
        /*
         * 判断是否应当停止接收数据。
         * 在一些短路操作中，可以在达到某个目标后取消后续的操作。
         */
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
    // 链式Sink的抽象实现(long类型版本)
    abstract class ChainedLong<E_OUT> implements Sink.OfLong {
    
        // 保存了下游的Sink，以方便链式调用
        protected final Sink<? super E_OUT> downstream;
    
        public ChainedLong(Sink<? super E_OUT> downSink) {
            this.downstream = Objects.requireNonNull(downSink);
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
            downstream.begin(size);
        }
    
        /*
         * 关闭sink链，结束本轮计算
         *
         * 待处理完所有目标元素后(不一定要处理全部元素)，需要调用此方法。
         * 如果Sink是有状态的，则需要在此处清除之前存储的状态，并释放相关的资源。
         */
        @Override
        public void end() {
            downstream.end();
        }
    
        /*
         * 判断是否应当停止接收数据。
         * 在一些短路操作中，可以在达到某个目标后取消后续的操作。
         */
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
    // 链式Sink的抽象实现(double类型版本)
    abstract class ChainedDouble<E_OUT> implements Sink.OfDouble {
    
        // 保存了下游的Sink，以方便链式调用
        protected final Sink<? super E_OUT> downstream;
    
        public ChainedDouble(Sink<? super E_OUT> downSink) {
            this.downstream = Objects.requireNonNull(downSink);
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
            downstream.begin(size);
        }
    
        /*
         * 关闭sink链，结束本轮计算
         *
         * 待处理完所有目标元素后(不一定要处理全部元素)，需要调用此方法。
         * 如果Sink是有状态的，则需要在此处清除之前存储的状态，并释放相关的资源。
         */
        @Override
        public void end() {
            downstream.end();
        }
    
        /*
         * 判断是否应当停止接收数据。
         * 在一些短路操作中，可以在达到某个目标后取消后续的操作。
         */
        @Override
        public boolean cancellationRequested() {
            return downstream.cancellationRequested();
        }
    }
    
    /*▲ 链式Sink ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
