/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * A base type for primitive specializations of {@code Iterator}.
 * Specialized subtypes are provided for {@link OfInt int}, {@link OfLong long}, and {@link OfDouble double} values.
 *
 * <p>The specialized subtype default implementations of {@link Iterator#next}
 * and {@link Iterator#forEachRemaining(java.util.function.Consumer)} box primitive values to instances of their corresponding wrapper class.
 * Such boxing may offset any advantages gained when using the primitive specializations.
 * To avoid boxing, the corresponding primitive-based methods should be used.
 * For example, {@link PrimitiveIterator.OfInt#nextInt()} and  {@link PrimitiveIterator.OfInt#forEachRemaining(java.util.function.IntConsumer)}
 * should be used in preference to {@link PrimitiveIterator.OfInt#next()} and {@link PrimitiveIterator.OfInt#forEachRemaining(java.util.function.Consumer)}.
 *
 * <p>Iteration of primitive values using boxing-based methods {@link Iterator#next next()} and {@link Iterator#forEachRemaining(java.util.function.Consumer) forEachRemaining()},
 * does not affect the order in which the values, transformed to boxed values, are encountered.
 *
 * @param <T>      the type of elements returned by this PrimitiveIterator.
 *                 The type must be a wrapper type for a primitive type, such as {@code Integer} for the primitive {@code int} type.
 * @param <T_CONS> the type of primitive consumer.
 *                 The type must be a primitive specialization of {@link java.util.function.Consumer} for {@code T},
 *                 such as {@link java.util.function.IntConsumer} for {@code Integer}.
 *
 * @implNote If the boolean system property {@code org.openjdk.java.util.stream.tripwire} is set to {@code true}
 * then diagnostic warnings are reported if boxing of primitive values occur when operating on primitive subtype specializations.
 *
 * @since 1.8
 */

// 为基本类型特化的Iterator
public interface PrimitiveIterator<T, T_CONS> extends Iterator<T> {
    
    /**
     * Performs the given action for each remaining element, in the order
     * elements occur when iterating, until all elements have been processed
     * or the action throws an exception.  Errors or runtime exceptions
     * thrown by the action are relayed to the caller.
     *
     * @param action The action to be performed for each element
     *
     * @throws NullPointerException if the specified action is null
     */
    // 对每个剩余元素执行给定的择取操作，择取操作封装在action函数中。
    @SuppressWarnings("overloads")
    void forEachRemaining(T_CONS action);
    
    /**
     * An Iterator specialized for {@code int} values.
     *
     * @since 1.8
     */
    // 为int类型(包括byte,short,char)特化的Iterator
    interface OfInt extends PrimitiveIterator<Integer, IntConsumer> {
        
        /**
         * Returns the next {@code int} element in the iteration.
         *
         * @return the next {@code int} element in the iteration
         *
         * @throws NoSuchElementException if the iteration has no more elements
         */
        // 从迭代器返回下一个元素[基本类型版本]
        int nextInt();
        
        /**
         * {@inheritDoc}
         *
         * @implSpec The default implementation boxes the result of calling
         * {@link #nextInt()}, and returns that boxed result.
         */
        // 从迭代器返回下一个元素，默认的实现是将元素装箱后再返回[包装类型版本]
        @Override
        default Integer next() {
            if(Tripwire.ENABLED)
                Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfInt.nextInt()");
            return nextInt();
        }
        
        /**
         * Performs the given action for each remaining element until all elements
         * have been processed or the action throws an exception.  Actions are
         * performed in the order of iteration, if that order is specified.
         * Exceptions thrown by the action are relayed to the caller.
         *
         * @param action The action to be performed for each element
         *
         * @throws NullPointerException if the specified action is null
         * @implSpec <p>The default implementation behaves as if:
         * <pre>{@code
         *     while (hasNext())
         *         action.accept(nextInt());
         * }</pre>
         */
        // 对每个剩余元素执行给定的择取操作，择取操作封装在action函数中[基本类型版本]
        default void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action);
            while(hasNext()) {
                action.accept(nextInt());
            }
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec If the action is an instance of {@code IntConsumer} then it is cast
         * to {@code IntConsumer} and passed to {@link #forEachRemaining};
         * otherwise the action is adapted to an instance of
         * {@code IntConsumer}, by boxing the argument of {@code IntConsumer},
         * and then passed to {@link #forEachRemaining}.
         */
        /*
         * 对每个剩余元素执行给定的择取操作，择取操作封装在action函数中[包装类型版本]
         *
         * 如果操作是{IntConsumer}的实例，那么它将被强制转换为{IntConsumer}并传递给{#forEachRemaining};
         * 否则，通过装箱{IntConsumer}的参数，将其适配为{IntConsumer}的实例，然后传递给{#forEachRemaining}。
         */
        @Override
        default void forEachRemaining(Consumer<? super Integer> action) {
            // 适用于函数式接口
            if(action instanceof IntConsumer) {
                forEachRemaining((IntConsumer) action);
            } else {
                // The method reference action::accept is never null
                Objects.requireNonNull(action);
                if(Tripwire.ENABLED)
                    Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfInt.forEachRemainingInt(action::accept)");
                // 适用于非函数式接口（如子实现类），但功能一样的情形
                forEachRemaining((IntConsumer) action::accept);
            }
        }
        
    }
    
    /**
     * An Iterator specialized for {@code long} values.
     *
     * @since 1.8
     */
    // 为long类型特化的Iterator
    interface OfLong extends PrimitiveIterator<Long, LongConsumer> {
        
        /**
         * Returns the next {@code long} element in the iteration.
         *
         * @return the next {@code long} element in the iteration
         *
         * @throws NoSuchElementException if the iteration has no more elements
         */
        // 从迭代器返回下一个元素[基本类型版本]
        long nextLong();
        
        /**
         * {@inheritDoc}
         *
         * @implSpec The default implementation boxes the result of calling
         * {@link #nextLong()}, and returns that boxed result.
         */
        // 从迭代器返回下一个元素，默认的实现是将元素装箱后再返回[包装类型版本]
        @Override
        default Long next() {
            if(Tripwire.ENABLED)
                Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfLong.nextLong()");
            return nextLong();
        }
        
        /**
         * Performs the given action for each remaining element until all elements
         * have been processed or the action throws an exception.  Actions are
         * performed in the order of iteration, if that order is specified.
         * Exceptions thrown by the action are relayed to the caller.
         *
         * @param action The action to be performed for each element
         *
         * @throws NullPointerException if the specified action is null
         * @implSpec <p>The default implementation behaves as if:
         * <pre>{@code
         *     while (hasNext())
         *         action.accept(nextLong());
         * }</pre>
         */
        // 对每个剩余元素执行给定的择取操作，择取操作封装在action函数中[基本类型版本]
        default void forEachRemaining(LongConsumer action) {
            Objects.requireNonNull(action);
            while(hasNext()) {
                action.accept(nextLong());
            }
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec If the action is an instance of {@code LongConsumer} then it is cast
         * to {@code LongConsumer} and passed to {@link #forEachRemaining};
         * otherwise the action is adapted to an instance of
         * {@code LongConsumer}, by boxing the argument of {@code LongConsumer},
         * and then passed to {@link #forEachRemaining}.
         */
        /*
         * 对每个剩余元素执行给定的择取操作，择取操作封装在action函数中[包装类型版本]
         *
         * 如果操作是{LongConsumer}的实例，那么它将被强制转换为{LongConsumer}并传递给{#forEachRemaining};
         * 否则，通过装箱{LongConsumer}的参数，将其适配为{LongConsumer}的实例，然后传递给{#forEachRemaining}。
         */
        @Override
        default void forEachRemaining(Consumer<? super Long> action) {
            // 适用于函数式接口
            if(action instanceof LongConsumer) {
                forEachRemaining((LongConsumer) action);
            } else {
                // The method reference action::accept is never null
                Objects.requireNonNull(action);
                if(Tripwire.ENABLED)
                    Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfLong.forEachRemainingLong(action::accept)");
                // 适用于非函数式接口（如子实现类），但功能一样的情形
                forEachRemaining((LongConsumer) action::accept);
            }
        }
    }
    
    /**
     * An Iterator specialized for {@code double} values.
     *
     * @since 1.8
     */
    // 为double类型(包括float)特化的Iterator
    interface OfDouble extends PrimitiveIterator<Double, DoubleConsumer> {
        
        /**
         * Returns the next {@code double} element in the iteration.
         *
         * @return the next {@code double} element in the iteration
         *
         * @throws NoSuchElementException if the iteration has no more elements
         */
        // 从迭代器返回下一个元素[基本类型版本]
        double nextDouble();
        
        /**
         * {@inheritDoc}
         *
         * @implSpec The default implementation boxes the result of calling
         * {@link #nextDouble()}, and returns that boxed result.
         */
        // 从迭代器返回下一个元素，默认的实现是将元素装箱后再返回[包装类型版本]
        @Override
        default Double next() {
            if(Tripwire.ENABLED)
                Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfDouble.nextLong()");
            return nextDouble();
        }
        
        /**
         * Performs the given action for each remaining element until all elements
         * have been processed or the action throws an exception.  Actions are
         * performed in the order of iteration, if that order is specified.
         * Exceptions thrown by the action are relayed to the caller.
         *
         * @param action The action to be performed for each element
         *
         * @throws NullPointerException if the specified action is null
         * @implSpec <p>The default implementation behaves as if:
         * <pre>{@code
         *     while (hasNext())
         *         action.accept(nextDouble());
         * }</pre>
         */
        // 对每个剩余元素执行给定的择取操作，择取操作封装在action函数中[基本类型版本]
        default void forEachRemaining(DoubleConsumer action) {
            Objects.requireNonNull(action);
            while(hasNext()) {
                action.accept(nextDouble());
            }
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec If the action is an instance of {@code DoubleConsumer} then it is
         * cast to {@code DoubleConsumer} and passed to
         * {@link #forEachRemaining}; otherwise the action is adapted to
         * an instance of {@code DoubleConsumer}, by boxing the argument of
         * {@code DoubleConsumer}, and then passed to
         * {@link #forEachRemaining}.
         */
        /*
         * 对每个剩余元素执行给定的择取操作，择取操作封装在action函数中[包装类型版本]
         *
         * 如果操作是{DoubleConsumer}的实例，那么它将被强制转换为{DoubleConsumer}并传递给{#forEachRemaining};
         * 否则，通过装箱{DoubleConsumer}的参数，将其适配为{DoubleConsumer}的实例，然后传递给{#forEachRemaining}。
         */
        @Override
        default void forEachRemaining(Consumer<? super Double> action) {
            // 适用于函数式接口
            if(action instanceof DoubleConsumer) {
                forEachRemaining((DoubleConsumer) action);
            } else {
                // The method reference action::accept is never null
                Objects.requireNonNull(action);
                if(Tripwire.ENABLED)
                    Tripwire.trip(getClass(), "{0} calling PrimitiveIterator.OfDouble.forEachRemainingDouble(action::accept)");
                // 适用于非函数式接口（如子实现类），但功能一样的情形
                forEachRemaining((DoubleConsumer) action::accept);
            }
        }
    }
}
