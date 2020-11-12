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
 * Static classes and methods for operating on or creating instances of {@link Spliterator}
 * and its primitive specializations {@link Spliterator.OfInt}, {@link Spliterator.OfLong}, and {@link Spliterator.OfDouble}.
 *
 * @see Spliterator
 * @since 1.8
 */
/*
 * 流迭代器工厂，提供一些简单的流迭代器的实现
 *
 * 主要包含以下4类流迭代器：
 * [1] "空"Spliterator
 * [2] "数组"Spliterator
 * [3] "支持有限并行"的Spliterator
 * [4] "适配Iterator"的Spliterator
 *
 * 在方法层面，除了提供构造各类Spliterator的工厂方法，还提供了将Spliterator适配为Iterator的方法实现。
 */
public final class Spliterators {
    
    // Suppresses default constructor, ensuring non-instantiability.
    private Spliterators() {
    }
    
    
    
    /*▼ (1) 构造"空"Spliterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 空的Spliterator，不包含任何待处理元素
    private static final Spliterator<Object> EMPTY_SPLITERATOR = new EmptySpliterator.OfRef<>();
    private static final Spliterator.OfInt EMPTY_INT_SPLITERATOR = new EmptySpliterator.OfInt();
    private static final Spliterator.OfLong EMPTY_LONG_SPLITERATOR = new EmptySpliterator.OfLong();
    private static final Spliterator.OfDouble EMPTY_DOUBLE_SPLITERATOR = new EmptySpliterator.OfDouble();
    
    
    /**
     * Creates an empty {@code Spliterator}
     *
     * <p>The empty spliterator reports {@link Spliterator#SIZED} and
     * {@link Spliterator#SUBSIZED}.  Calls to
     * {@link java.util.Spliterator#trySplit()} always return {@code null}.
     *
     * @param <T> Type of elements
     *
     * @return An empty spliterator
     */
    // 构造"空"Spliterator(引用类型版本)
    @SuppressWarnings("unchecked")
    public static <T> Spliterator<T> emptySpliterator() {
        return (Spliterator<T>) EMPTY_SPLITERATOR;
    }
    
    /**
     * Creates an empty {@code Spliterator.OfInt}
     *
     * <p>The empty spliterator reports {@link Spliterator#SIZED} and
     * {@link Spliterator#SUBSIZED}.  Calls to
     * {@link java.util.Spliterator#trySplit()} always return {@code null}.
     *
     * @return An empty spliterator
     */
    // 构造"空"Spliterator(int类型版本)
    public static Spliterator.OfInt emptyIntSpliterator() {
        return EMPTY_INT_SPLITERATOR;
    }
    
    /**
     * Creates an empty {@code Spliterator.OfLong}
     *
     * <p>The empty spliterator reports {@link Spliterator#SIZED} and
     * {@link Spliterator#SUBSIZED}.  Calls to
     * {@link java.util.Spliterator#trySplit()} always return {@code null}.
     *
     * @return An empty spliterator
     */
    // 构造"空"Spliterator(long类型版本)
    public static Spliterator.OfLong emptyLongSpliterator() {
        return EMPTY_LONG_SPLITERATOR;
    }
    
    /**
     * Creates an empty {@code Spliterator.OfDouble}
     *
     * <p>The empty spliterator reports {@link Spliterator#SIZED} and
     * {@link Spliterator#SUBSIZED}.  Calls to
     * {@link java.util.Spliterator#trySplit()} always return {@code null}.
     *
     * @return An empty spliterator
     */
    // 构造"空"Spliterator(double类型版本)
    public static Spliterator.OfDouble emptyDoubleSpliterator() {
        return EMPTY_DOUBLE_SPLITERATOR;
    }
    
    /*▲ (1) 构造"空"Spliterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (2) 构造"数组"Spliterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code Spliterator} covering the elements of a given array,
     * using a customized set of spliterator characteristics.
     *
     * <p>This method is provided as an implementation convenience for
     * Spliterators which store portions of their elements in arrays, and need
     * fine control over Spliterator characteristics.  Most other situations in
     * which a Spliterator for an array is needed should use
     * {@link Arrays#spliterator(Object[])}.
     *
     * <p>The returned spliterator always reports the characteristics
     * {@code SIZED} and {@code SUBSIZED}.  The caller may provide additional
     * characteristics for the spliterator to report; it is common to
     * additionally specify {@code IMMUTABLE} and {@code ORDERED}.
     *
     * @param <T>                       Type of elements
     * @param array                     The array, assumed to be unmodified during use
     * @param additionalCharacteristics Additional spliterator characteristics
     *                                  of this spliterator's source or elements beyond {@code SIZED} and
     *                                  {@code SUBSIZED} which are always reported
     *
     * @return A spliterator for an array
     *
     * @throws NullPointerException if the given array is {@code null}
     * @see Arrays#spliterator(Object[])
     */
    // 构造"数组"Spliterator(引用类型版本)，数据源是array
    public static <T> Spliterator<T> spliterator(Object[] array, int additionalCharacteristics) {
        return new ArraySpliterator<>(Objects.requireNonNull(array), additionalCharacteristics);
    }
    
    /**
     * Creates a {@code Spliterator} covering a range of elements of a given
     * array, using a customized set of spliterator characteristics.
     *
     * <p>This method is provided as an implementation convenience for
     * Spliterators which store portions of their elements in arrays, and need
     * fine control over Spliterator characteristics.  Most other situations in
     * which a Spliterator for an array is needed should use
     * {@link Arrays#spliterator(Object[])}.
     *
     * <p>The returned spliterator always reports the characteristics
     * {@code SIZED} and {@code SUBSIZED}.  The caller may provide additional
     * characteristics for the spliterator to report; it is common to
     * additionally specify {@code IMMUTABLE} and {@code ORDERED}.
     *
     * @param <T>                       Type of elements
     * @param array                     The array, assumed to be unmodified during use
     * @param fromIndex                 The least index (inclusive) to cover
     * @param toIndex                   One past the greatest index to cover
     * @param additionalCharacteristics Additional spliterator characteristics
     *                                  of this spliterator's source or elements beyond {@code SIZED} and
     *                                  {@code SUBSIZED} which are always reported
     *
     * @return A spliterator for an array
     *
     * @throws NullPointerException           if the given array is {@code null}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                        {@code toIndex} is less than {@code fromIndex}, or
     *                                        {@code toIndex} is greater than the array size
     * @see Arrays#spliterator(Object[], int, int)
     */
    // 构造"数组"Spliterator(引用类型版本)，数据源是array[fromIndex, toIndex)
    public static <T> Spliterator<T> spliterator(Object[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new ArraySpliterator<>(array, fromIndex, toIndex, additionalCharacteristics);
    }
    
    
    /**
     * Creates a {@code Spliterator.OfInt} covering the elements of a given array,
     * using a customized set of spliterator characteristics.
     *
     * <p>This method is provided as an implementation convenience for
     * Spliterators which store portions of their elements in arrays, and need
     * fine control over Spliterator characteristics.  Most other situations in
     * which a Spliterator for an array is needed should use
     * {@link Arrays#spliterator(int[])}.
     *
     * <p>The returned spliterator always reports the characteristics
     * {@code SIZED} and {@code SUBSIZED}.  The caller may provide additional
     * characteristics for the spliterator to report; it is common to
     * additionally specify {@code IMMUTABLE} and {@code ORDERED}.
     *
     * @param array                     The array, assumed to be unmodified during use
     * @param additionalCharacteristics Additional spliterator characteristics
     *                                  of this spliterator's source or elements beyond {@code SIZED} and
     *                                  {@code SUBSIZED} which are always reported
     *
     * @return A spliterator for an array
     *
     * @throws NullPointerException if the given array is {@code null}
     * @see Arrays#spliterator(int[])
     */
    // 构造"数组"Spliterator(int类型版本)，数据源是array
    public static Spliterator.OfInt spliterator(int[] array, int additionalCharacteristics) {
        return new IntArraySpliterator(Objects.requireNonNull(array), additionalCharacteristics);
    }
    
    /**
     * Creates a {@code Spliterator.OfInt} covering a range of elements of a
     * given array, using a customized set of spliterator characteristics.
     *
     * <p>This method is provided as an implementation convenience for
     * Spliterators which store portions of their elements in arrays, and need
     * fine control over Spliterator characteristics.  Most other situations in
     * which a Spliterator for an array is needed should use
     * {@link Arrays#spliterator(int[], int, int)}.
     *
     * <p>The returned spliterator always reports the characteristics
     * {@code SIZED} and {@code SUBSIZED}.  The caller may provide additional
     * characteristics for the spliterator to report; it is common to
     * additionally specify {@code IMMUTABLE} and {@code ORDERED}.
     *
     * @param array                     The array, assumed to be unmodified during use
     * @param fromIndex                 The least index (inclusive) to cover
     * @param toIndex                   One past the greatest index to cover
     * @param additionalCharacteristics Additional spliterator characteristics
     *                                  of this spliterator's source or elements beyond {@code SIZED} and
     *                                  {@code SUBSIZED} which are always reported
     *
     * @return A spliterator for an array
     *
     * @throws NullPointerException           if the given array is {@code null}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                        {@code toIndex} is less than {@code fromIndex}, or
     *                                        {@code toIndex} is greater than the array size
     * @see Arrays#spliterator(int[], int, int)
     */
    // 构造"数组"Spliterator(int类型版本)，数据源是array[fromIndex, toIndex)
    public static Spliterator.OfInt spliterator(int[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new IntArraySpliterator(array, fromIndex, toIndex, additionalCharacteristics);
    }
    
    
    /**
     * Creates a {@code Spliterator.OfLong} covering the elements of a given array,
     * using a customized set of spliterator characteristics.
     *
     * <p>This method is provided as an implementation convenience for
     * Spliterators which store portions of their elements in arrays, and need
     * fine control over Spliterator characteristics.  Most other situations in
     * which a Spliterator for an array is needed should use
     * {@link Arrays#spliterator(long[])}.
     *
     * <p>The returned spliterator always reports the characteristics
     * {@code SIZED} and {@code SUBSIZED}.  The caller may provide additional
     * characteristics for the spliterator to report; it is common to
     * additionally specify {@code IMMUTABLE} and {@code ORDERED}.
     *
     * @param array                     The array, assumed to be unmodified during use
     * @param additionalCharacteristics Additional spliterator characteristics
     *                                  of this spliterator's source or elements beyond {@code SIZED} and
     *                                  {@code SUBSIZED} which are always reported
     *
     * @return A spliterator for an array
     *
     * @throws NullPointerException if the given array is {@code null}
     * @see Arrays#spliterator(long[])
     */
    // 构造"数组"Spliterator(long类型版本)，数据源是array
    public static Spliterator.OfLong spliterator(long[] array, int additionalCharacteristics) {
        return new LongArraySpliterator(Objects.requireNonNull(array), additionalCharacteristics);
    }
    
    /**
     * Creates a {@code Spliterator.OfLong} covering a range of elements of a
     * given array, using a customized set of spliterator characteristics.
     *
     * <p>This method is provided as an implementation convenience for
     * Spliterators which store portions of their elements in arrays, and need
     * fine control over Spliterator characteristics.  Most other situations in
     * which a Spliterator for an array is needed should use
     * {@link Arrays#spliterator(long[], int, int)}.
     *
     * <p>The returned spliterator always reports the characteristics
     * {@code SIZED} and {@code SUBSIZED}.  The caller may provide additional
     * characteristics for the spliterator to report.  (For example, if it is
     * known the array will not be further modified, specify {@code IMMUTABLE};
     * if the array data is considered to have an encounter order, specify
     * {@code ORDERED}).  The method {@link Arrays#spliterator(long[], int, int)} can
     * often be used instead, which returns a spliterator that reports
     * {@code SIZED}, {@code SUBSIZED}, {@code IMMUTABLE}, and {@code ORDERED}.
     *
     * @param array                     The array, assumed to be unmodified during use
     * @param fromIndex                 The least index (inclusive) to cover
     * @param toIndex                   One past the greatest index to cover
     * @param additionalCharacteristics Additional spliterator characteristics
     *                                  of this spliterator's source or elements beyond {@code SIZED} and
     *                                  {@code SUBSIZED} which are always reported
     *
     * @return A spliterator for an array
     *
     * @throws NullPointerException           if the given array is {@code null}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                        {@code toIndex} is less than {@code fromIndex}, or
     *                                        {@code toIndex} is greater than the array size
     * @see Arrays#spliterator(long[], int, int)
     */
    // 构造"数组"Spliterator(long类型版本)，数据源是array[fromIndex, toIndex)
    public static Spliterator.OfLong spliterator(long[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new LongArraySpliterator(array, fromIndex, toIndex, additionalCharacteristics);
    }
    
    
    /**
     * Creates a {@code Spliterator.OfDouble} covering the elements of a given array,
     * using a customized set of spliterator characteristics.
     *
     * <p>This method is provided as an implementation convenience for
     * Spliterators which store portions of their elements in arrays, and need
     * fine control over Spliterator characteristics.  Most other situations in
     * which a Spliterator for an array is needed should use
     * {@link Arrays#spliterator(double[])}.
     *
     * <p>The returned spliterator always reports the characteristics
     * {@code SIZED} and {@code SUBSIZED}.  The caller may provide additional
     * characteristics for the spliterator to report; it is common to
     * additionally specify {@code IMMUTABLE} and {@code ORDERED}.
     *
     * @param array                     The array, assumed to be unmodified during use
     * @param additionalCharacteristics Additional spliterator characteristics
     *                                  of this spliterator's source or elements beyond {@code SIZED} and
     *                                  {@code SUBSIZED} which are always reported
     *
     * @return A spliterator for an array
     *
     * @throws NullPointerException if the given array is {@code null}
     * @see Arrays#spliterator(double[])
     */
    // 构造"数组"Spliterator(double类型版本)，数据源是array
    public static Spliterator.OfDouble spliterator(double[] array, int additionalCharacteristics) {
        return new DoubleArraySpliterator(Objects.requireNonNull(array), additionalCharacteristics);
    }
    
    /**
     * Creates a {@code Spliterator.OfDouble} covering a range of elements of a
     * given array, using a customized set of spliterator characteristics.
     *
     * <p>This method is provided as an implementation convenience for
     * Spliterators which store portions of their elements in arrays, and need
     * fine control over Spliterator characteristics.  Most other situations in
     * which a Spliterator for an array is needed should use
     * {@link Arrays#spliterator(double[], int, int)}.
     *
     * <p>The returned spliterator always reports the characteristics
     * {@code SIZED} and {@code SUBSIZED}.  The caller may provide additional
     * characteristics for the spliterator to report.  (For example, if it is
     * known the array will not be further modified, specify {@code IMMUTABLE};
     * if the array data is considered to have an encounter order, specify
     * {@code ORDERED}).  The method {@link Arrays#spliterator(long[], int, int)} can
     * often be used instead, which returns a spliterator that reports
     * {@code SIZED}, {@code SUBSIZED}, {@code IMMUTABLE}, and {@code ORDERED}.
     *
     * @param array                     The array, assumed to be unmodified during use
     * @param fromIndex                 The least index (inclusive) to cover
     * @param toIndex                   One past the greatest index to cover
     * @param additionalCharacteristics Additional spliterator characteristics
     *                                  of this spliterator's source or elements beyond {@code SIZED} and
     *                                  {@code SUBSIZED} which are always reported
     *
     * @return A spliterator for an array
     *
     * @throws NullPointerException           if the given array is {@code null}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                        {@code toIndex} is less than {@code fromIndex}, or
     *                                        {@code toIndex} is greater than the array size
     * @see Arrays#spliterator(double[], int, int)
     */
    // 构造"数组"Spliterator(double类型版本)，数据源是array[fromIndex, toIndex)
    public static Spliterator.OfDouble spliterator(double[] array, int fromIndex, int toIndex, int additionalCharacteristics) {
        checkFromToBounds(Objects.requireNonNull(array).length, fromIndex, toIndex);
        return new DoubleArraySpliterator(array, fromIndex, toIndex, additionalCharacteristics);
    }
    
    /*▲ (2) 构造"数组"Spliterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (3) 构造"支持有限并行"的Spliterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 需要子类重写该抽象类，并实现tryAdvance()方法；必要时，还需要重写forEachRemaining()方法
    
    /*▲ (3) 构造"支持有限并行"的Spliterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (4) 构造"适配Iterator"的Spliterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code Spliterator} using the given collection's
     * {@link java.util.Collection#iterator()} as the source of elements, and
     * reporting its {@link java.util.Collection#size()} as its initial size.
     *
     * <p>The spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>, inherits
     * the <em>fail-fast</em> properties of the collection's iterator, and
     * implements {@code trySplit} to permit limited parallelism.
     *
     * @param <T>             Type of elements
     * @param c               The collection
     * @param characteristics Characteristics of this spliterator's source or
     *                        elements.  The characteristics {@code SIZED} and {@code SUBSIZED}
     *                        are additionally reported unless {@code CONCURRENT} is supplied.
     *
     * @return A spliterator from an iterator
     *
     * @throws NullPointerException if the given collection is {@code null}
     */
    // 构造"适配Iterator"的Spliterator(引用类型版本)
    public static <T> Spliterator<T> spliterator(Collection<? extends T> collection, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(collection), characteristics);
    }
    
    /**
     * Creates a {@code Spliterator} using a given {@code Iterator}
     * as the source of elements, with no initial size estimate.
     *
     * <p>The spliterator is not
     * <em><a href="Spliterator.html#binding">late-binding</a></em>, inherits
     * the <em>fail-fast</em> properties of the iterator, and implements
     * {@code trySplit} to permit limited parallelism.
     *
     * <p>Traversal of elements should be accomplished through the spliterator.
     * The behaviour of splitting and traversal is undefined if the iterator is
     * operated on after the spliterator is returned.
     *
     * @param <T>             Type of elements
     * @param iterator        The iterator for the source
     * @param characteristics Characteristics of this spliterator's source
     *                        or elements ({@code SIZED} and {@code SUBSIZED}, if supplied, are
     *                        ignored and are not reported.)
     *
     * @return A spliterator from an iterator
     *
     * @throws NullPointerException if the given iterator is {@code null}
     */
    // 构造"适配Iterator"的Spliterator(引用类型版本)
    public static <T> Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> iterator, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(iterator), characteristics);
    }
    
    /**
     * Creates a {@code Spliterator} using a given {@code Iterator}
     * as the source of elements, and with a given initially reported size.
     *
     * <p>The spliterator is not
     * <em><a href="Spliterator.html#binding">late-binding</a></em>, inherits
     * the <em>fail-fast</em> properties of the iterator, and implements
     * {@code trySplit} to permit limited parallelism.
     *
     * <p>Traversal of elements should be accomplished through the spliterator.
     * The behaviour of splitting and traversal is undefined if the iterator is
     * operated on after the spliterator is returned, or the initially reported
     * size is not equal to the actual number of elements in the source.
     *
     * @param <T>             Type of elements
     * @param iterator        The iterator for the source
     * @param size            The number of elements in the source, to be reported as
     *                        initial {@code estimateSize}
     * @param characteristics Characteristics of this spliterator's source or
     *                        elements.  The characteristics {@code SIZED} and {@code SUBSIZED}
     *                        are additionally reported unless {@code CONCURRENT} is supplied.
     *
     * @return A spliterator from an iterator
     *
     * @throws NullPointerException if the given iterator is {@code null}
     */
    // 构造"适配Iterator"的Spliterator(引用类型版本)
    public static <T> Spliterator<T> spliterator(Iterator<? extends T> iterator, long size, int characteristics) {
        return new IteratorSpliterator<>(Objects.requireNonNull(iterator), size, characteristics);
    }
    
    
    /**
     * Creates a {@code Spliterator.OfInt} using a given
     * {@code IntStream.IntIterator} as the source of elements, with no initial
     * size estimate.
     *
     * <p>The spliterator is not
     * <em><a href="Spliterator.html#binding">late-binding</a></em>, inherits
     * the <em>fail-fast</em> properties of the iterator, and implements
     * {@code trySplit} to permit limited parallelism.
     *
     * <p>Traversal of elements should be accomplished through the spliterator.
     * The behaviour of splitting and traversal is undefined if the iterator is
     * operated on after the spliterator is returned.
     *
     * @param iterator        The iterator for the source
     * @param characteristics Characteristics of this spliterator's source
     *                        or elements ({@code SIZED} and {@code SUBSIZED}, if supplied, are
     *                        ignored and are not reported.)
     *
     * @return A spliterator from an iterator
     *
     * @throws NullPointerException if the given iterator is {@code null}
     */
    // 构造"适配Iterator"的Spliterator(int类型版本)
    public static Spliterator.OfInt spliteratorUnknownSize(PrimitiveIterator.OfInt iterator, int characteristics) {
        return new IntIteratorSpliterator(Objects.requireNonNull(iterator), characteristics);
    }
    
    /**
     * Creates a {@code Spliterator.OfInt} using a given
     * {@code IntStream.IntIterator} as the source of elements, and with a given
     * initially reported size.
     *
     * <p>The spliterator is not
     * <em><a href="Spliterator.html#binding">late-binding</a></em>, inherits
     * the <em>fail-fast</em> properties of the iterator, and implements
     * {@code trySplit} to permit limited parallelism.
     *
     * <p>Traversal of elements should be accomplished through the spliterator.
     * The behaviour of splitting and traversal is undefined if the iterator is
     * operated on after the spliterator is returned, or the initially reported
     * size is not equal to the actual number of elements in the source.
     *
     * @param iterator        The iterator for the source
     * @param size            The number of elements in the source, to be reported as
     *                        initial {@code estimateSize}.
     * @param characteristics Characteristics of this spliterator's source or
     *                        elements.  The characteristics {@code SIZED} and {@code SUBSIZED}
     *                        are additionally reported unless {@code CONCURRENT} is supplied.
     *
     * @return A spliterator from an iterator
     *
     * @throws NullPointerException if the given iterator is {@code null}
     */
    // 构造"适配Iterator"的Spliterator(int类型版本)
    public static Spliterator.OfInt spliterator(PrimitiveIterator.OfInt iterator, long size, int characteristics) {
        return new IntIteratorSpliterator(Objects.requireNonNull(iterator), size, characteristics);
    }
    
    
    /**
     * Creates a {@code Spliterator.OfLong} using a given
     * {@code LongStream.LongIterator} as the source of elements, with no
     * initial size estimate.
     *
     * <p>The spliterator is not
     * <em><a href="Spliterator.html#binding">late-binding</a></em>, inherits
     * the <em>fail-fast</em> properties of the iterator, and implements
     * {@code trySplit} to permit limited parallelism.
     *
     * <p>Traversal of elements should be accomplished through the spliterator.
     * The behaviour of splitting and traversal is undefined if the iterator is
     * operated on after the spliterator is returned.
     *
     * @param iterator        The iterator for the source
     * @param characteristics Characteristics of this spliterator's source
     *                        or elements ({@code SIZED} and {@code SUBSIZED}, if supplied, are
     *                        ignored and are not reported.)
     *
     * @return A spliterator from an iterator
     *
     * @throws NullPointerException if the given iterator is {@code null}
     */
    // 构造"适配Iterator"的Spliterator(long类型版本)
    public static Spliterator.OfLong spliteratorUnknownSize(PrimitiveIterator.OfLong iterator, int characteristics) {
        return new LongIteratorSpliterator(Objects.requireNonNull(iterator), characteristics);
    }
    
    /**
     * Creates a {@code Spliterator.OfLong} using a given
     * {@code LongStream.LongIterator} as the source of elements, and with a
     * given initially reported size.
     *
     * <p>The spliterator is not
     * <em><a href="Spliterator.html#binding">late-binding</a></em>, inherits
     * the <em>fail-fast</em> properties of the iterator, and implements
     * {@code trySplit} to permit limited parallelism.
     *
     * <p>Traversal of elements should be accomplished through the spliterator.
     * The behaviour of splitting and traversal is undefined if the iterator is
     * operated on after the spliterator is returned, or the initially reported
     * size is not equal to the actual number of elements in the source.
     *
     * @param iterator        The iterator for the source
     * @param size            The number of elements in the source, to be reported as
     *                        initial {@code estimateSize}.
     * @param characteristics Characteristics of this spliterator's source or
     *                        elements.  The characteristics {@code SIZED} and {@code SUBSIZED}
     *                        are additionally reported unless {@code CONCURRENT} is supplied.
     *
     * @return A spliterator from an iterator
     *
     * @throws NullPointerException if the given iterator is {@code null}
     */
    // 构造"适配Iterator"的Spliterator(long类型版本)
    public static Spliterator.OfLong spliterator(PrimitiveIterator.OfLong iterator, long size, int characteristics) {
        return new LongIteratorSpliterator(Objects.requireNonNull(iterator), size, characteristics);
    }
    
    
    /**
     * Creates a {@code Spliterator.OfDouble} using a given
     * {@code DoubleStream.DoubleIterator} as the source of elements, with no
     * initial size estimate.
     *
     * <p>The spliterator is not
     * <em><a href="Spliterator.html#binding">late-binding</a></em>, inherits
     * the <em>fail-fast</em> properties of the iterator, and implements
     * {@code trySplit} to permit limited parallelism.
     *
     * <p>Traversal of elements should be accomplished through the spliterator.
     * The behaviour of splitting and traversal is undefined if the iterator is
     * operated on after the spliterator is returned.
     *
     * @param iterator        The iterator for the source
     * @param characteristics Characteristics of this spliterator's source
     *                        or elements ({@code SIZED} and {@code SUBSIZED}, if supplied, are
     *                        ignored and are not reported.)
     *
     * @return A spliterator from an iterator
     *
     * @throws NullPointerException if the given iterator is {@code null}
     */
    // 构造"适配Iterator"的Spliterator(double类型版本)
    public static Spliterator.OfDouble spliteratorUnknownSize(PrimitiveIterator.OfDouble iterator, int characteristics) {
        return new DoubleIteratorSpliterator(Objects.requireNonNull(iterator), characteristics);
    }
    
    /**
     * Creates a {@code Spliterator.OfDouble} using a given
     * {@code DoubleStream.DoubleIterator} as the source of elements, and with a
     * given initially reported size.
     *
     * <p>The spliterator is not
     * <em><a href="Spliterator.html#binding">late-binding</a></em>, inherits
     * the <em>fail-fast</em> properties of the iterator, and implements
     * {@code trySplit} to permit limited parallelism.
     *
     * <p>Traversal of elements should be accomplished through the spliterator.
     * The behaviour of splitting and traversal is undefined if the iterator is
     * operated on after the spliterator is returned, or the initially reported
     * size is not equal to the actual number of elements in the source.
     *
     * @param iterator        The iterator for the source
     * @param size            The number of elements in the source, to be reported as
     *                        initial {@code estimateSize}
     * @param characteristics Characteristics of this spliterator's source or
     *                        elements.  The characteristics {@code SIZED} and {@code SUBSIZED}
     *                        are additionally reported unless {@code CONCURRENT} is supplied.
     *
     * @return A spliterator from an iterator
     *
     * @throws NullPointerException if the given iterator is {@code null}
     */
    // 构造"适配Iterator"的Spliterator(double类型版本)
    public static Spliterator.OfDouble spliterator(PrimitiveIterator.OfDouble iterator, long size, int characteristics) {
        return new DoubleIteratorSpliterator(Objects.requireNonNull(iterator), size, characteristics);
    }
    
    /*▲ (4) 构造"适配Iterator"的Spliterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 将Spliterator适配为Iterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates an {@code Iterator} from a {@code Spliterator}.
     *
     * <p>Traversal of elements should be accomplished through the iterator.
     * The behaviour of traversal is undefined if the spliterator is operated
     * after the iterator is returned.
     *
     * @param <T>         Type of elements
     * @param spliterator The spliterator
     *
     * @return An iterator
     *
     * @throws NullPointerException if the given spliterator is {@code null}
     */
    // 将Spliterator适配为Iterator(引用类型版本)
    public static <T> Iterator<T> iterator(Spliterator<? extends T> spliterator) {
        Objects.requireNonNull(spliterator);
        
        // 适配器，包装Spliterator，使其发挥Iterator的功能
        class Adapter implements Iterator<T>, Consumer<T> {
            boolean valueReady = false; // 是否存在下一个元素
            T nextElement;              // 记录下一个元素
            
            // 重写了accept，被tryAdvance回调，用于记录下一个元素
            @Override
            public void accept(T e) {
                valueReady = true;
                nextElement = e;
            }
            
            // 是否存在下一个未遍历元素
            @Override
            public boolean hasNext() {
                if(!valueReady) {
                    // 尝试用accept()消费spliterator中下一个元素
                    spliterator.tryAdvance(this);
                }
                
                return valueReady;
            }
            
            // 返回下一个元素
            @Override
            public T next() {
                if(!valueReady && !hasNext()) {
                    throw new NoSuchElementException();
                }
                
                valueReady = false;
                
                return nextElement;
            }
        }
        
        return new Adapter();
    }
    
    /**
     * Creates an {@code PrimitiveIterator.OfInt} from a
     * {@code Spliterator.OfInt}.
     *
     * <p>Traversal of elements should be accomplished through the iterator.
     * The behaviour of traversal is undefined if the spliterator is operated
     * after the iterator is returned.
     *
     * @param spliterator The spliterator
     *
     * @return An iterator
     *
     * @throws NullPointerException if the given spliterator is {@code null}
     */
    // 将Spliterator适配为Iterator(int类型版本)
    public static PrimitiveIterator.OfInt iterator(Spliterator.OfInt spliterator) {
        Objects.requireNonNull(spliterator);
        
        // 适配器，包装Spliterator，使其发挥Iterator的功能
        class Adapter implements PrimitiveIterator.OfInt, IntConsumer {
            boolean valueReady = false; // 是否存在下一个元素
            int nextElement;            // 记录下一个元素
            
            // 重写了accept，被tryAdvance回调，用于记录下一个元素
            @Override
            public void accept(int e) {
                valueReady = true;
                nextElement = e;
            }
            
            // 是否存在下一个未遍历元素
            @Override
            public boolean hasNext() {
                if(!valueReady) {
                    // 尝试用accept()消费spliterator中下一个元素
                    spliterator.tryAdvance(this);
                }
                
                return valueReady;
            }
            
            // 返回下一个元素
            @Override
            public int nextInt() {
                if(!valueReady && !hasNext()) {
                    throw new NoSuchElementException();
                }
                
                valueReady = false;
                
                return nextElement;
            }
        }
        
        return new Adapter();
    }
    
    /**
     * Creates an {@code PrimitiveIterator.OfLong} from a
     * {@code Spliterator.OfLong}.
     *
     * <p>Traversal of elements should be accomplished through the iterator.
     * The behaviour of traversal is undefined if the spliterator is operated
     * after the iterator is returned.
     *
     * @param spliterator The spliterator
     *
     * @return An iterator
     *
     * @throws NullPointerException if the given spliterator is {@code null}
     */
    // 将Spliterator适配为Iterator(long类型版本)
    public static PrimitiveIterator.OfLong iterator(Spliterator.OfLong spliterator) {
        Objects.requireNonNull(spliterator);
        
        // 适配器，包装Spliterator，使其发挥Iterator的功能
        class Adapter implements PrimitiveIterator.OfLong, LongConsumer {
            boolean valueReady = false; // 是否存在下一个元素
            long nextElement;           // 记录下一个元素
            
            // 重写了accept，被tryAdvance回调，用于记录下一个元素
            @Override
            public void accept(long e) {
                valueReady = true;
                nextElement = e;
            }
            
            // 是否存在下一个未遍历元素
            @Override
            public boolean hasNext() {
                if(!valueReady) {
                    // 尝试用accept()消费spliterator中下一个元素
                    spliterator.tryAdvance(this);
                }
                
                return valueReady;
            }
            
            // 返回下一个元素
            @Override
            public long nextLong() {
                if(!valueReady && !hasNext()) {
                    throw new NoSuchElementException();
                }
                
                valueReady = false;
                
                return nextElement;
            }
        }
        
        return new Adapter();
    }
    
    /**
     * Creates an {@code PrimitiveIterator.OfDouble} from a
     * {@code Spliterator.OfDouble}.
     *
     * <p>Traversal of elements should be accomplished through the iterator.
     * The behaviour of traversal is undefined if the spliterator is operated
     * after the iterator is returned.
     *
     * @param spliterator The spliterator
     *
     * @return An iterator
     *
     * @throws NullPointerException if the given spliterator is {@code null}
     */
    // 将Spliterator适配为Iterator(double类型版本)
    public static PrimitiveIterator.OfDouble iterator(Spliterator.OfDouble spliterator) {
        Objects.requireNonNull(spliterator);
        
        // 适配器，包装Spliterator，使其发挥Iterator的功能
        class Adapter implements PrimitiveIterator.OfDouble, DoubleConsumer {
            boolean valueReady = false; // 是否存在下一个元素
            double nextElement;         // 记录下一个元素
            
            // 重写了accept，被tryAdvance回调，用于记录下一个元素
            @Override
            public void accept(double e) {
                valueReady = true;
                nextElement = e;
            }
            
            // 是否存在下一个未遍历元素
            @Override
            public boolean hasNext() {
                if(!valueReady) {
                    // 尝试用accept()消费spliterator中下一个元素
                    spliterator.tryAdvance(this);
                }
                
                return valueReady;
            }
            
            // 返回下一个元素
            @Override
            public double nextDouble() {
                if(!valueReady && !hasNext()) {
                    throw new NoSuchElementException();
                }
                
                valueReady = false;
                
                return nextElement;
            }
        }
        
        return new Adapter();
    }
    
    /*▲ 将Spliterator适配为Iterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    
    
    
    
    
    
    /*▼ [1] "空"Spliterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 空的Spliterator，不包含任何有效元素
    private abstract static class EmptySpliterator<T, S extends Spliterator<T>, C> {
        
        EmptySpliterator() {
        }
        
        public S trySplit() {
            return null;
        }
        
        public boolean tryAdvance(C consumer) {
            Objects.requireNonNull(consumer);
            return false;
        }
        
        public void forEachRemaining(C consumer) {
            Objects.requireNonNull(consumer);
        }
        
        public long estimateSize() {
            return 0;
        }
        
        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED;
        }
        
        
        // 空的Spliterator(引用类型版本)，不包含任何有效元素
        private static final class OfRef<T> extends EmptySpliterator<T, Spliterator<T>, Consumer<? super T>> implements Spliterator<T> {
            OfRef() {
            }
        }
        
        // 空的Spliterator(int类型版本)，不包含任何有效元素
        private static final class OfInt extends EmptySpliterator<Integer, Spliterator.OfInt, IntConsumer> implements Spliterator.OfInt {
            OfInt() {
            }
        }
        
        // 空的Spliterator(long类型版本)，不包含任何有效元素
        private static final class OfLong extends EmptySpliterator<Long, Spliterator.OfLong, LongConsumer> implements Spliterator.OfLong {
            OfLong() {
            }
        }
        
        // 空的Spliterator(double类型版本)，不包含任何有效元素
        private static final class OfDouble extends EmptySpliterator<Double, Spliterator.OfDouble, DoubleConsumer> implements Spliterator.OfDouble {
            OfDouble() {
            }
        }
    }
    
    /*▲ [1] "空"Spliterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ [2] "数组"Spliterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * A Spliterator designed for use by sources that traverse and split elements maintained in an unmodifiable {@code Object[]} array.
     */
    // "数组"Spliterator(引用类型版本)
    static final class ArraySpliterator<T> implements Spliterator<T> {
        /**
         * The array, explicitly typed as Object[].
         * Unlike in some other classes (see for example CR 6260652),
         * we do not need to screen arguments to ensure they are exactly of type Object[] so long as no methods write into the array or serialize it,
         * which we ensure here by defining this class as final.
         */
        private final Object[] array;       // 当前流迭代器中包含的数据
        private final int characteristics;  // 当前流迭代器的参数
        private int index;                  // 数据起始索引
        private final int fence;            // 数据终止索引(不包含)
    
        /**
         * Creates a spliterator covering all of the given array.
         *
         * @param array                     the array, assumed to be unmodified during use
         * @param additionalCharacteristics Additional spliterator characteristics
         *                                  of this spliterator's source or elements beyond {@code SIZED} and
         *                                  {@code SUBSIZED} which are always reported
         */
        // 构造"数组"Spliterator，其中包含array中全部数据
        public ArraySpliterator(Object[] array, int additionalCharacteristics) {
            this(array, 0, array.length, additionalCharacteristics);
        }
        
        /**
         * Creates a spliterator covering the given array and range
         *
         * @param array                     the array, assumed to be unmodified during use
         * @param origin                    the least index (inclusive) to cover
         * @param fence                     one past the greatest index to cover
         * @param additionalCharacteristics Additional spliterator characteristics
         *                                  of this spliterator's source or elements beyond {@code SIZED} and
         *                                  {@code SUBSIZED} which are always reported
         */
        // 构造"数组"Spliterator，其中包含array[index, fence)范围内的数据
        public ArraySpliterator(Object[] array, int origin, int fence, int additionalCharacteristics) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.characteristics = additionalCharacteristics | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator<T> trySplit() {
            int lo = index;
            int mid = (lo + fence) >>> 1;
        
            if(lo >= mid) {
                return null;
            }
        
            return new ArraySpliterator<>(array, lo, index = mid, characteristics);
        }
    
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if(action == null) {
                throw new NullPointerException();
            }
        
            if(index<0 || index >= fence) {
                return false;
            }
        
            @SuppressWarnings("unchecked")
            T e = (T) array[index++];
        
            action.accept(e);
        
            return true;
        }
    
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @SuppressWarnings("unchecked")
        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            Object[] a;
            int i, hi; // hoist accesses and checks from loop
        
            if(action == null) {
                throw new NullPointerException();
            }
        
            if((a = array).length >= (hi = fence) && (i = index) >= 0 && i<(index = hi)) {
                do {
                    action.accept((T) a[i]);
                } while(++i<hi);
            }
        }
    
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return (long) (fence - index);
        }
    
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
    
        /*
         * 对于具有SORTED参数的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED参数的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super T> getComparator() {
            if(hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
    
            throw new IllegalStateException();
        }
    }
    
    /**
     * A Spliterator.OfInt designed for use by sources that traverse and split elements maintained in an unmodifiable {@code int[]} array.
     */
    // "数组"Spliterator(int类型版本)
    static final class IntArraySpliterator implements Spliterator.OfInt {
        private final int[] array;          // 当前流迭代器中包含的数据
        private final int characteristics;  // 当前流迭代器的参数
        private int index;                  // 数据起始索引
        private final int fence;            // 数据终止索引(不包含)
        
        /**
         * Creates a spliterator covering all of the given array.
         *
         * @param array                     the array, assumed to be unmodified during use
         * @param additionalCharacteristics Additional spliterator characteristics
         *                                  of this spliterator's source or elements beyond {@code SIZED} and
         *                                  {@code SUBSIZED} which are always reported
         */
        // 构造"数组"Spliterator，其中包含array中全部数据
        public IntArraySpliterator(int[] array, int additionalCharacteristics) {
            this(array, 0, array.length, additionalCharacteristics);
        }
        
        /**
         * Creates a spliterator covering the given array and range
         *
         * @param array                     the array, assumed to be unmodified during use
         * @param origin                    the least index (inclusive) to cover
         * @param fence                     one past the greatest index to cover
         * @param additionalCharacteristics Additional spliterator characteristics
         *                                  of this spliterator's source or elements beyond {@code SIZED} and
         *                                  {@code SUBSIZED} which are always reported
         */
        // 构造"数组"Spliterator，其中包含array[index, fence)范围内的数据
        public IntArraySpliterator(int[] array, int origin, int fence, int additionalCharacteristics) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.characteristics = additionalCharacteristics | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public OfInt trySplit() {
            int lo = index;
            int mid = (lo + fence) >>> 1;
            
            if(lo >= mid) {
                return null;
            }
            
            return new IntArraySpliterator(array, lo, index = mid, characteristics);
        }
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(IntConsumer action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            if(index<0 || index >= fence) {
                return false;
            }
            
            action.accept(array[index++]);
            
            return true;
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public void forEachRemaining(IntConsumer action) {
            int[] a;
            int i, hi; // hoist accesses and checks from loop
            
            if(action == null) {
                throw new NullPointerException();
            }
            
            if((a = array).length >= (hi = fence) && (i = index) >= 0 && i<(index = hi)) {
                do {
                    action.accept(a[i]);
                } while(++i<hi);
            }
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return (long) (fence - index);
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
        
        /*
         * 对于具有SORTED参数的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED参数的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super Integer> getComparator() {
            if(hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            
            throw new IllegalStateException();
        }
    }
    
    /**
     * A Spliterator.OfLong designed for use by sources that traverse and split
     * elements maintained in an unmodifiable {@code int[]} array.
     */
    // "数组"Spliterator(long类型版本)
    static final class LongArraySpliterator implements Spliterator.OfLong {
        private final long[] array;         // 当前流迭代器中包含的数据
        private final int characteristics;  // 当前流迭代器的参数
        private int index;                  // 数据起始索引
        private final int fence;            // 数据终止索引(不包含)
        
        /**
         * Creates a spliterator covering all of the given array.
         *
         * @param array                     the array, assumed to be unmodified during use
         * @param additionalCharacteristics Additional spliterator characteristics
         *                                  of this spliterator's source or elements beyond {@code SIZED} and
         *                                  {@code SUBSIZED} which are always reported
         */
        // 构造"数组"Spliterator，其中包含array中全部数据
        public LongArraySpliterator(long[] array, int additionalCharacteristics) {
            this(array, 0, array.length, additionalCharacteristics);
        }
        
        /**
         * Creates a spliterator covering the given array and range
         *
         * @param array                     the array, assumed to be unmodified during use
         * @param origin                    the least index (inclusive) to cover
         * @param fence                     one past the greatest index to cover
         * @param additionalCharacteristics Additional spliterator characteristics
         *                                  of this spliterator's source or elements beyond {@code SIZED} and
         *                                  {@code SUBSIZED} which are always reported
         */
        // 构造"数组"Spliterator，其中包含array[index, fence)范围内的数据
        public LongArraySpliterator(long[] array, int origin, int fence, int additionalCharacteristics) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.characteristics = additionalCharacteristics | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public OfLong trySplit() {
            int lo = index;
            int mid = (lo + fence) >>> 1;
            
            if(lo >= mid) {
                return null;
            }
            
            return new LongArraySpliterator(array, lo, index = mid, characteristics);
        }
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(LongConsumer action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            if(index<0 || index >= fence) {
                return false;
            }
            
            action.accept(array[index++]);
            
            return true;
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public void forEachRemaining(LongConsumer action) {
            long[] a;
            int i, hi; // hoist accesses and checks from loop
            
            if(action == null) {
                throw new NullPointerException();
            }
            
            if((a = array).length >= (hi = fence) && (i = index) >= 0 && i<(index = hi)) {
                do {
                    action.accept(a[i]);
                } while(++i<hi);
            }
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return (long) (fence - index);
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
        
        /*
         * 对于具有SORTED参数的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED参数的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super Long> getComparator() {
            if(hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            throw new IllegalStateException();
        }
    }
    
    /**
     * A Spliterator.OfDouble designed for use by sources that traverse and split
     * elements maintained in an unmodifiable {@code int[]} array.
     */
    // "数组"Spliterator(double类型版本)
    static final class DoubleArraySpliterator implements Spliterator.OfDouble {
        private final double[] array;       // 当前流迭代器中包含的数据
        private final int characteristics;  // 当前流迭代器的参数
        private int index;                  // 数据起始索引
        private final int fence;            // 数据终止索引(不包含)
        
        /**
         * Creates a spliterator covering all of the given array.
         *
         * @param array                     the array, assumed to be unmodified during use
         * @param additionalCharacteristics Additional spliterator characteristics
         *                                  of this spliterator's source or elements beyond {@code SIZED} and
         *                                  {@code SUBSIZED} which are always reported
         */
        // 构造"数组"Spliterator，其中包含array中全部数据
        public DoubleArraySpliterator(double[] array, int additionalCharacteristics) {
            this(array, 0, array.length, additionalCharacteristics);
        }
        
        /**
         * Creates a spliterator covering the given array and range
         *
         * @param array                     the array, assumed to be unmodified during use
         * @param origin                    the least index (inclusive) to cover
         * @param fence                     one past the greatest index to cover
         * @param additionalCharacteristics Additional spliterator characteristics
         *                                  of this spliterator's source or elements beyond {@code SIZED} and
         *                                  {@code SUBSIZED} which are always reported
         */
        // 构造"数组"Spliterator，其中包含array[index, fence)范围内的数据
        public DoubleArraySpliterator(double[] array, int origin, int fence, int additionalCharacteristics) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.characteristics = additionalCharacteristics | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public OfDouble trySplit() {
            int lo = index;
            int mid = (lo + fence) >>> 1;
            
            if(lo >= mid) {
                return null;
            }
            
            return new DoubleArraySpliterator(array, lo, index = mid, characteristics);
        }
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            if(index<0 || index >= fence) {
                return false;
            }
            
            action.accept(array[index++]);
            
            return true;
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public void forEachRemaining(DoubleConsumer action) {
            double[] a;
            int i, hi;  // hoist accesses and checks from loop
            
            if(action == null) {
                throw new NullPointerException();
            }
            
            if((a = array).length >= (hi = fence) && (i = index) >= 0 && i<(index = hi)) {
                do {
                    action.accept(a[i]);
                } while(++i<hi);
            }
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return (long) (fence - index);
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
        
        /*
         * 对于具有SORTED参数的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED参数的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super Double> getComparator() {
            if(hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            
            throw new IllegalStateException();
        }
    }
    
    /*▲ [2] "数组"Spliterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ [3] "支持有限并行"的抽象Spliterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * An abstract {@code Spliterator} that implements {@code trySplit} to
     * permit limited parallelism.
     *
     * <p>An extending class need only
     * implement {@link #tryAdvance(java.util.function.Consumer) tryAdvance}.
     * The extending class should override
     * {@link #forEachRemaining(java.util.function.Consumer) forEachRemaining}
     * if it can provide a more performant implementation.
     *
     * @apiNote This class is a useful aid for creating a spliterator when it is not
     * possible or difficult to efficiently partition elements in a manner
     * allowing balanced parallel computation.
     *
     * <p>An alternative to using this class, that also permits limited
     * parallelism, is to create a spliterator from an iterator
     * (see {@link #spliterator(Iterator, long, int)}.  Depending on the
     * circumstances using an iterator may be easier or more convenient than
     * extending this class, such as when there is already an iterator
     * available to use.
     * @see #spliterator(Iterator, long, int)
     * @since 1.8
     */
    // "支持有限并行"的抽象Spliterator(引用类型版本)
    public abstract static class AbstractSpliterator<T> implements Spliterator<T> {
        static final int BATCH_UNIT = 1 << 10;  // 切割每批元素时所取的元素增量，比如第一批切割了n个元素，则第二批需要切割n+BATCH_UNIT个元素，但每批切割元素的上限为MAX_BATCH
        static final int MAX_BATCH = 1 << 25;  // 每批待切割元素的上限
        
        private final int characteristics; // 流迭代器参数
        
        private long est;   // 元素总数
        private int batch;  // 记录上一批切割的元素数量
        
        /**
         * Creates a spliterator reporting the given estimated size and
         * additionalCharacteristics.
         *
         * @param est                       the estimated size of this spliterator if known, otherwise
         *                                  {@code Long.MAX_VALUE}.
         * @param additionalCharacteristics properties of this spliterator's
         *                                  source or elements.  If {@code SIZED} is reported then this
         *                                  spliterator will additionally report {@code SUBSIZED}.
         */
        protected AbstractSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            this.characteristics = ((additionalCharacteristics & Spliterator.SIZED) != 0) ? additionalCharacteristics | Spliterator.SUBSIZED : additionalCharacteristics;
        }
        
        /**
         * {@inheritDoc}
         *
         * This implementation permits limited parallelism.
         */
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator<T> trySplit() {
            /*
             * Split into arrays of arithmetically increasing batch
             * sizes.  This will only improve parallel performance if
             * per-element Consumer actions are more costly than
             * transferring them into an array.  The use of an
             * arithmetic progression in split sizes provides overhead
             * vs parallelism bounds that do not particularly favor or
             * penalize cases of lightweight vs heavyweight element
             * operations, across combinations of #elements vs #cores,
             * whether or not either are known.  We generate
             * O(sqrt(#elements)) splits, allowing O(sqrt(#cores))
             * potential speedup.
             */
            
            HoldingConsumer<T> holder = new HoldingConsumer<>();
            
            // 仅有一个元素，或者无法获取到下一个元素时，返回null
            if(est<=1 || !tryAdvance(holder)) {
                return null;
            }
            
            // 初始化本次需要切割的元素数量
            int n = batch + BATCH_UNIT;
            
            // 不能超出原始数据量
            if(n>est) {
                n = (int) est;
            }
            
            // 不能超出切割阈值
            if(n>MAX_BATCH) {
                n = MAX_BATCH;
            }
            
            Object[] a = new Object[n];
            int j = 0;
            do {
                a[j] = holder.value;
            } while(++j<n && tryAdvance(holder));
            
            // 记录当前批次切割的元素总量，以作为下次切割的参考值
            batch = j;
            
            // 缩减原始数据量
            if(est != Long.MAX_VALUE) {
                est -= j;
            }
            
            return new ArraySpliterator<>(a, 0, j, characteristics());
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation returns the estimated size as reported when
         * created and, if the estimate size is known, decreases in size when
         * split.
         */
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return est;
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation returns the characteristics as reported when
         * created.
         */
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
        
        
        // 消费下一个元素
        static final class HoldingConsumer<T> implements Consumer<T> {
            Object value;
            
            @Override
            public void accept(T value) {
                this.value = value;
            }
        }
    }
    
    /**
     * An abstract {@code Spliterator.OfInt} that implements {@code trySplit} to
     * permit limited parallelism.
     *
     * <p>To implement a spliterator an extending class need only
     * implement {@link #tryAdvance(java.util.function.IntConsumer)
     * tryAdvance}.  The extending class should override
     * {@link #forEachRemaining(java.util.function.IntConsumer) forEachRemaining}
     * if it can provide a more performant implementation.
     *
     * @apiNote This class is a useful aid for creating a spliterator when it is not
     * possible or difficult to efficiently partition elements in a manner
     * allowing balanced parallel computation.
     *
     * <p>An alternative to using this class, that also permits limited
     * parallelism, is to create a spliterator from an iterator
     * (see {@link #spliterator(java.util.PrimitiveIterator.OfInt, long, int)}.
     * Depending on the circumstances using an iterator may be easier or more
     * convenient than extending this class. For example, if there is already an
     * iterator available to use then there is no need to extend this class.
     * @see #spliterator(java.util.PrimitiveIterator.OfInt, long, int)
     * @since 1.8
     */
    // "支持有限并行"的抽象Spliterator(int类型版本)
    public abstract static class AbstractIntSpliterator implements Spliterator.OfInt {
        static final int MAX_BATCH = AbstractSpliterator.MAX_BATCH;   // 切割每批元素时所取的元素增量，比如第一批切割了n个元素，则第二批需要切割n+BATCH_UNIT个元素，但每批切割元素的上限为MAX_BATCH
        static final int BATCH_UNIT = AbstractSpliterator.BATCH_UNIT;  // 每批待切割元素的上限
    
        private final int characteristics; // 流迭代器参数
    
        private long est;   // 元素总数
        private int batch;  // 记录上一批切割的元素数量
    
        /**
         * Creates a spliterator reporting the given estimated size and
         * characteristics.
         *
         * @param est                       the estimated size of this spliterator if known, otherwise
         *                                  {@code Long.MAX_VALUE}.
         * @param additionalCharacteristics properties of this spliterator's
         *                                  source or elements.  If {@code SIZED} is reported then this
         *                                  spliterator will additionally report {@code SUBSIZED}.
         */
        protected AbstractIntSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            this.characteristics = ((additionalCharacteristics & Spliterator.SIZED) != 0) ? additionalCharacteristics | Spliterator.SUBSIZED : additionalCharacteristics;
        }
    
        /**
         * {@inheritDoc}
         *
         * This implementation permits limited parallelism.
         */
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator.OfInt trySplit() {
            HoldingIntConsumer holder = new HoldingIntConsumer();
        
            // 仅有一个元素，或者无法获取到下一个元素时，返回null
            if(est<=1 || !tryAdvance(holder)) {
                return null;
            }
        
            // 初始化本次需要切割的元素数量
            int n = batch + BATCH_UNIT;
        
            // 不能超出原始数据量
            if(n>est) {
                n = (int) est;
            }
        
            // 不能超出切割阈值
            if(n>MAX_BATCH) {
                n = MAX_BATCH;
            }
        
            int[] a = new int[n];
            int j = 0;
        
            do {
                a[j] = holder.value;
            } while(++j<n && tryAdvance(holder));
        
            // 记录当前批次切割的元素总量，以作为下次切割的参考值
            batch = j;
        
            // 缩减原始数据量
            if(est != Long.MAX_VALUE) {
                est -= j;
            }
        
            return new IntArraySpliterator(a, 0, j, characteristics());
        }
    
        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation returns the estimated size as reported when
         * created and, if the estimate size is known, decreases in size when
         * split.
         */
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return est;
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation returns the characteristics as reported when
         * created.
         */
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
    
    
        // 消费下一个元素
        static final class HoldingIntConsumer implements IntConsumer {
            int value;
        
            @Override
            public void accept(int value) {
                this.value = value;
            }
        }
    }
    
    /**
     * An abstract {@code Spliterator.OfLong} that implements {@code trySplit}
     * to permit limited parallelism.
     *
     * <p>To implement a spliterator an extending class need only
     * implement {@link #tryAdvance(java.util.function.LongConsumer)
     * tryAdvance}.  The extending class should override
     * {@link #forEachRemaining(java.util.function.LongConsumer) forEachRemaining}
     * if it can provide a more performant implementation.
     *
     * @apiNote This class is a useful aid for creating a spliterator when it is not
     * possible or difficult to efficiently partition elements in a manner
     * allowing balanced parallel computation.
     *
     * <p>An alternative to using this class, that also permits limited
     * parallelism, is to create a spliterator from an iterator
     * (see {@link #spliterator(java.util.PrimitiveIterator.OfLong, long, int)}.
     * Depending on the circumstances using an iterator may be easier or more
     * convenient than extending this class. For example, if there is already an
     * iterator available to use then there is no need to extend this class.
     * @see #spliterator(java.util.PrimitiveIterator.OfLong, long, int)
     * @since 1.8
     */
    // "支持有限并行"的抽象Spliterator(long类型版本)
    public abstract static class AbstractLongSpliterator implements Spliterator.OfLong {
        static final int MAX_BATCH = AbstractSpliterator.MAX_BATCH;   // 切割每批元素时所取的元素增量，比如第一批切割了n个元素，则第二批需要切割n+BATCH_UNIT个元素，但每批切割元素的上限为MAX_BATCH
        static final int BATCH_UNIT = AbstractSpliterator.BATCH_UNIT;  // 每批待切割元素的上限
    
        private final int characteristics; // 流迭代器参数
    
        private long est;   // 元素总数
        private int batch;  // 记录上一批切割的元素数量
    
        /**
         * Creates a spliterator reporting the given estimated size and
         * characteristics.
         *
         * @param est                       the estimated size of this spliterator if known, otherwise
         *                                  {@code Long.MAX_VALUE}.
         * @param additionalCharacteristics properties of this spliterator's
         *                                  source or elements.  If {@code SIZED} is reported then this
         *                                  spliterator will additionally report {@code SUBSIZED}.
         */
        protected AbstractLongSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            this.characteristics = ((additionalCharacteristics & Spliterator.SIZED) != 0) ? additionalCharacteristics | Spliterator.SUBSIZED : additionalCharacteristics;
        }
    
        /**
         * {@inheritDoc}
         *
         * This implementation permits limited parallelism.
         */
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator.OfLong trySplit() {
            HoldingLongConsumer holder = new HoldingLongConsumer();
        
            // 仅有一个元素，或者无法获取到下一个元素时，返回null
            if(est<=1 || !tryAdvance(holder)) {
                return null;
            }
        
            // 初始化本次需要切割的元素数量
            int n = batch + BATCH_UNIT;
        
            // 不能超出原始数据量
            if(n>est) {
                n = (int) est;
            }
        
            // 不能超出切割阈值
            if(n>MAX_BATCH) {
                n = MAX_BATCH;
            }
        
            long[] a = new long[n];
            int j = 0;
        
            do {
                a[j] = holder.value;
            } while(++j<n && tryAdvance(holder));
        
            // 记录当前批次切割的元素总量，以作为下次切割的参考值
            batch = j;
        
            // 缩减原始数据量
            if(est != Long.MAX_VALUE) {
                est -= j;
            }
        
            return new LongArraySpliterator(a, 0, j, characteristics());
        }
    
        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation returns the estimated size as reported when
         * created and, if the estimate size is known, decreases in size when
         * split.
         */
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return est;
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation returns the characteristics as reported when
         * created.
         */
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
    
    
        // 消费下一个元素
        static final class HoldingLongConsumer implements LongConsumer {
            long value;
        
            @Override
            public void accept(long value) {
                this.value = value;
            }
        }
    }
    
    /**
     * An abstract {@code Spliterator.OfDouble} that implements
     * {@code trySplit} to permit limited parallelism.
     *
     * <p>To implement a spliterator an extending class need only
     * implement {@link #tryAdvance(java.util.function.DoubleConsumer)
     * tryAdvance}.  The extending class should override
     * {@link #forEachRemaining(java.util.function.DoubleConsumer) forEachRemaining}
     * if it can provide a more performant implementation.
     *
     * @apiNote This class is a useful aid for creating a spliterator when it is not
     * possible or difficult to efficiently partition elements in a manner
     * allowing balanced parallel computation.
     *
     * <p>An alternative to using this class, that also permits limited
     * parallelism, is to create a spliterator from an iterator
     * (see {@link #spliterator(java.util.PrimitiveIterator.OfDouble, long, int)}.
     * Depending on the circumstances using an iterator may be easier or more
     * convenient than extending this class. For example, if there is already an
     * iterator available to use then there is no need to extend this class.
     * @see #spliterator(java.util.PrimitiveIterator.OfDouble, long, int)
     * @since 1.8
     */
    // "支持有限并行"的抽象Spliterator(double类型版本)
    public abstract static class AbstractDoubleSpliterator implements Spliterator.OfDouble {
        static final int MAX_BATCH = AbstractSpliterator.MAX_BATCH;   // 切割每批元素时所取的元素增量，比如第一批切割了n个元素，则第二批需要切割n+BATCH_UNIT个元素，但每批切割元素的上限为MAX_BATCH
        static final int BATCH_UNIT = AbstractSpliterator.BATCH_UNIT;  // 每批待切割元素的上限
    
        private final int characteristics; // 流迭代器参数
    
        private long est;   // 元素总数
        private int batch;  // 记录上一批切割的元素数量
    
        /**
         * Creates a spliterator reporting the given estimated size and
         * characteristics.
         *
         * @param est                       the estimated size of this spliterator if known, otherwise
         *                                  {@code Long.MAX_VALUE}.
         * @param additionalCharacteristics properties of this spliterator's
         *                                  source or elements.  If {@code SIZED} is reported then this
         *                                  spliterator will additionally report {@code SUBSIZED}.
         */
        protected AbstractDoubleSpliterator(long est, int additionalCharacteristics) {
            this.est = est;
            this.characteristics = ((additionalCharacteristics & Spliterator.SIZED) != 0) ? additionalCharacteristics | Spliterator.SUBSIZED : additionalCharacteristics;
        }
    
        /**
         * {@inheritDoc}
         *
         * This implementation permits limited parallelism.
         */
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator.OfDouble trySplit() {
            HoldingDoubleConsumer holder = new HoldingDoubleConsumer();
        
            // 仅有一个元素，或者无法获取到下一个元素时，返回null
            if(est<=1 || !tryAdvance(holder)) {
                return null;
            }
        
            // 初始化本次需要切割的元素数量
            int n = batch + BATCH_UNIT;
        
            // 不能超出原始数据量
            if(n>est) {
                n = (int) est;
            }
        
            // 不能超出切割阈值
            if(n>MAX_BATCH) {
                n = MAX_BATCH;
            }
        
            double[] a = new double[n];
            int j = 0;
        
            do {
                a[j] = holder.value;
            } while(++j<n && tryAdvance(holder));
        
            // 记录当前批次切割的元素总量，以作为下次切割的参考值
            batch = j;
        
            // 缩减原始数据量
            if(est != Long.MAX_VALUE) {
                est -= j;
            }
        
            return new DoubleArraySpliterator(a, 0, j, characteristics());
        }
    
        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation returns the estimated size as reported when
         * created and, if the estimate size is known, decreases in size when
         * split.
         */
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return est;
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec This implementation returns the characteristics as reported when
         * created.
         */
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
    
    
        // 消费下一个元素
        static final class HoldingDoubleConsumer implements DoubleConsumer {
            double value;
        
            @Override
            public void accept(double value) {
                this.value = value;
            }
        }
    }
    
    /*▲ [3] "支持有限并行"的抽象Spliterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ [4] "适配Iterator"的Spliterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * A Spliterator using a given Iterator for element operations.
     * The spliterator implements {@code trySplit} to permit limited parallelism.
     */
    // "适配Iterator"的Spliterator(引用类型版本)
    static class IteratorSpliterator<T> implements Spliterator<T> {
        static final int BATCH_UNIT = 1 << 10;  // 切割每批元素时所取的元素增量，比如第一批切割了n个元素，则第二批需要切割n+BATCH_UNIT个元素，但每批切割元素的上限为MAX_BATCH
        static final int MAX_BATCH = 1 << 25;  // 每批待切割元素的上限
        
        private final int characteristics; // 流迭代器参数
        
        /*
         * collection和iterator通常只初始化一个。
         * 如果存在collection，则可以从中获取到iterator。
         */
        private final Collection<? extends T> collection; // 数据源
        private Iterator<? extends T> iterator;           // 数据源
        
        private long est;   // 元素总数
        private int batch;  // 记录上一批切割的元素数量
        
        /**
         * Creates a spliterator using the given collection's {@link java.util.Collection#iterator()) for traversal,
         * and reporting its {@link java.util.Collection#size()) as its initial size.
         *
         * @param c               the collection
         * @param characteristics properties of this spliterator's source or elements.
         */
        public IteratorSpliterator(Collection<? extends T> collection, int characteristics) {
            this.collection = collection;
            this.iterator = null;
            this.characteristics = (characteristics & Spliterator.CONCURRENT) == 0 ? characteristics | Spliterator.SIZED | Spliterator.SUBSIZED : characteristics;
        }
        
        /**
         * Creates a spliterator using the given iterator for traversal,
         * and reporting the given initial size and characteristics.
         *
         * @param iterator        the iterator for the source
         * @param size            the number of elements in the source
         * @param characteristics properties of this spliterator's source or elements.
         */
        public IteratorSpliterator(Iterator<? extends T> iterator, long size, int characteristics) {
            this.collection = null;
            this.iterator = iterator;
            this.est = size;
            this.characteristics = (characteristics & Spliterator.CONCURRENT) == 0 ? characteristics | Spliterator.SIZED | Spliterator.SUBSIZED : characteristics;
        }
        
        /**
         * Creates a spliterator using the given iterator for traversal,
         * and reporting the given initial size and characteristics.
         *
         * @param iterator        the iterator for the source
         * @param characteristics properties of this spliterator's source or elements.
         */
        public IteratorSpliterator(Iterator<? extends T> iterator, int characteristics) {
            this.collection = null;
            this.iterator = iterator;
            this.est = Long.MAX_VALUE;
            this.characteristics = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator<T> trySplit() {
            /*
             * Split into arrays of arithmetically increasing batch sizes.
             * This will only improve parallel performance if per-element Consumer actions are more costly than transferring them into an array.
             * The use of an arithmetic progression in split sizes provides overhead vs parallelism bounds
             * that do not particularly favor or penalize cases of lightweight vs heavyweight element operations,
             * across combinations of #elements vs #cores, whether or not either are known.
             * We generate O(sqrt(#elements)) splits, allowing O(sqrt(#cores)) potential speedup.
             */
            
            // 如果iterator为null，则需要从collection获取到iterator
            if(iterator == null) {
                iterator = collection.iterator();
                est = (long) collection.size();
            }
            
            // 仅有一个元素，或者迭代器无法获取到下一个元素时，返回null
            if(est<=1 || !iterator.hasNext()) {
                return null;
            }
            
            // 初始化本次需要切割的元素数量
            int n = batch + BATCH_UNIT;
            
            // 不能超出原始数据量
            if(n>est) {
                n = (int) est;
            }
            
            // 不能超出切割阈值
            if(n>MAX_BATCH) {
                n = MAX_BATCH;
            }
            
            Object[] a = new Object[n];
            int j = 0;
            
            do {
                a[j] = iterator.next();
            } while(++j<n && iterator.hasNext());
            
            // 记录当前批次切割的元素总量，以作为下次切割的参考值
            batch = j;
            
            // 缩减原始数据量
            if(est != Long.MAX_VALUE) {
                est -= j;
            }
            
            return new ArraySpliterator<>(a, 0, j, characteristics);
        }
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            // 如果iterator为null，则需要从collection获取到iterator
            if(iterator == null) {
                iterator = collection.iterator();
                est = (long) collection.size();
            }
            
            // 如果已经没有可访问元素，则直接返回false
            if(!iterator.hasNext()) {
                return false;
            }
            
            // 消费下一个元素
            action.accept(iterator.next());
            
            return true;
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            // 如果iterator为null，则需要从collection获取到iterator
            if(iterator == null) {
                iterator = collection.iterator();
                est = (long) collection.size();
            }
            
            iterator.forEachRemaining(action);
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            
            // 如果iterator为null，则需要从collection获取到iterator
            if(iterator == null) {
                iterator = collection.iterator();
                est = (long) collection.size();
            }
            
            return est;
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
        
        /*
         * 对于具有SORTED参数的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED参数的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super T> getComparator() {
            if(hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            
            throw new IllegalStateException();
        }
    }
    
    /**
     * A Spliterator.OfInt using a given IntStream.IntIterator for element
     * operations. The spliterator implements {@code trySplit} to
     * permit limited parallelism.
     */
    // "适配Iterator"的Spliterator(int类型版本)
    static final class IntIteratorSpliterator implements Spliterator.OfInt {
        static final int BATCH_UNIT = IteratorSpliterator.BATCH_UNIT;  // 切割每批元素时所取的元素增量，比如第一批切割了n个元素，则第二批需要切割n+BATCH_UNIT个元素，但每批切割元素的上限为MAX_BATCH
        static final int MAX_BATCH = IteratorSpliterator.MAX_BATCH;   // 每批待切割元素的上限
        
        private final int characteristics; // 流迭代器参数
        
        private PrimitiveIterator.OfInt iterator; // 数据源
        
        private long est;   // 元素总数
        private int batch;  // 记录上一批切割的元素数量
        
        /**
         * Creates a spliterator using the given iterator
         * for traversal, and reporting the given initial size
         * and characteristics.
         *
         * @param iterator        the iterator for the source
         * @param size            the number of elements in the source
         * @param characteristics properties of this spliterator's
         *                        source or elements.
         */
        public IntIteratorSpliterator(PrimitiveIterator.OfInt iterator, long size, int characteristics) {
            this.iterator = iterator;
            this.est = size;
            this.characteristics = (characteristics & Spliterator.CONCURRENT) == 0 ? characteristics | Spliterator.SIZED | Spliterator.SUBSIZED : characteristics;
        }
        
        /**
         * Creates a spliterator using the given iterator for a
         * source of unknown size, reporting the given
         * characteristics.
         *
         * @param iterator        the iterator for the source
         * @param characteristics properties of this spliterator's
         *                        source or elements.
         */
        public IntIteratorSpliterator(PrimitiveIterator.OfInt iterator, int characteristics) {
            this.iterator = iterator;
            this.est = Long.MAX_VALUE;
            this.characteristics = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public OfInt trySplit() {
            
            // 仅有一个元素，或者迭代器无法获取到下一个元素时，返回null
            if(est<=1 || !iterator.hasNext()) {
                return null;
            }
            
            // 初始化本次需要切割的元素数量
            int n = batch + BATCH_UNIT;
            
            // 不能超出原始数据量
            if(n>est) {
                n = (int) est;
            }
            
            // 不能超出切割阈值
            if(n>MAX_BATCH) {
                n = MAX_BATCH;
            }
            
            int[] a = new int[n];
            int j = 0;
            do {
                a[j] = iterator.nextInt();
            } while(++j<n && iterator.hasNext());
            
            // 记录当前批次切割的元素总量，以作为下次切割的参考值
            batch = j;
            
            // 缩减原始数据量
            if(est != Long.MAX_VALUE) {
                est -= j;
            }
            
            return new IntArraySpliterator(a, 0, j, characteristics);
        }
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(IntConsumer action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            // 如果已经没有可访问元素，则直接返回false
            if(!iterator.hasNext()) {
                return false;
            }
            
            // 消费下一个元素
            action.accept(iterator.nextInt());
            
            return true;
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public void forEachRemaining(IntConsumer action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            iterator.forEachRemaining(action);
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return est;
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
        
        /*
         * 对于具有SORTED参数的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED参数的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super Integer> getComparator() {
            if(hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            
            throw new IllegalStateException();
        }
    }
    
    // "适配Iterator"的Spliterator(long类型版本)
    static final class LongIteratorSpliterator implements Spliterator.OfLong {
        static final int BATCH_UNIT = IteratorSpliterator.BATCH_UNIT; // 切割每批元素时所取的元素增量，比如第一批切割了n个元素，则第二批需要切割n+BATCH_UNIT个元素，但每批切割元素的上限为MAX_BATCH
        static final int MAX_BATCH = IteratorSpliterator.MAX_BATCH;  // 每批待切割元素的上限
        
        private final int characteristics; // 流迭代器参数
        
        private PrimitiveIterator.OfLong iterator; // 数据源
        
        private long est;   // 元素总数
        private int batch;  // 记录上一批切割的元素数量
        
        /**
         * Creates a spliterator using the given iterator
         * for traversal, and reporting the given initial size
         * and characteristics.
         *
         * @param iterator        the iterator for the source
         * @param size            the number of elements in the source
         * @param characteristics properties of this spliterator's
         *                        source or elements.
         */
        public LongIteratorSpliterator(PrimitiveIterator.OfLong iterator, long size, int characteristics) {
            this.iterator = iterator;
            this.est = size;
            this.characteristics = (characteristics & Spliterator.CONCURRENT) == 0 ? characteristics | Spliterator.SIZED | Spliterator.SUBSIZED : characteristics;
        }
        
        /**
         * Creates a spliterator using the given iterator for a
         * source of unknown size, reporting the given
         * characteristics.
         *
         * @param iterator        the iterator for the source
         * @param characteristics properties of this spliterator's
         *                        source or elements.
         */
        public LongIteratorSpliterator(PrimitiveIterator.OfLong iterator, int characteristics) {
            this.iterator = iterator;
            this.est = Long.MAX_VALUE;
            this.characteristics = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public OfLong trySplit() {
            
            // 仅有一个元素，或者迭代器无法获取到下一个元素时，返回null
            if(est<=1 || !iterator.hasNext()) {
                return null;
            }
            
            // 初始化本次需要切割的元素数量
            int n = batch + BATCH_UNIT;
            
            // 不能超出原始数据量
            if(n>est) {
                n = (int) est;
            }
            
            // 不能超出切割阈值
            if(n>MAX_BATCH) {
                n = MAX_BATCH;
            }
            
            long[] a = new long[n];
            int j = 0;
            
            do {
                a[j] = iterator.nextLong();
            } while(++j<n && iterator.hasNext());
            
            // 记录当前批次切割的元素总量，以作为下次切割的参考值
            batch = j;
            
            // 缩减原始数据量
            if(est != Long.MAX_VALUE) {
                est -= j;
            }
            
            return new LongArraySpliterator(a, 0, j, characteristics);
        }
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(LongConsumer action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            // 如果已经没有可访问元素，则直接返回false
            if(!iterator.hasNext()) {
                return false;
            }
            
            // 消费下一个元素
            action.accept(iterator.nextLong());
            
            return true;
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public void forEachRemaining(LongConsumer action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            iterator.forEachRemaining(action);
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return est;
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
        
        /*
         * 对于具有SORTED参数的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED参数的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super Long> getComparator() {
            if(hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            
            throw new IllegalStateException();
        }
    }
    
    // "适配Iterator"的Spliterator(double类型版本)
    static final class DoubleIteratorSpliterator implements Spliterator.OfDouble {
        static final int BATCH_UNIT = IteratorSpliterator.BATCH_UNIT;  // 切割每批元素时所取的元素增量，比如第一批切割了n个元素，则第二批需要切割n+BATCH_UNIT个元素，但每批切割元素的上限为MAX_BATCH
        static final int MAX_BATCH = IteratorSpliterator.MAX_BATCH;   // 每批待切割元素的上限
        
        private final int characteristics; // 流迭代器参数
        
        private PrimitiveIterator.OfDouble iterator; // 数据源
        
        private long est;   // 元素总数
        private int batch;  // 记录上一批切割的元素数量
        
        /**
         * Creates a spliterator using the given iterator
         * for traversal, and reporting the given initial size
         * and characteristics.
         *
         * @param iterator        the iterator for the source
         * @param size            the number of elements in the source
         * @param characteristics properties of this spliterator's
         *                        source or elements.
         */
        public DoubleIteratorSpliterator(PrimitiveIterator.OfDouble iterator, long size, int characteristics) {
            this.iterator = iterator;
            this.est = size;
            this.characteristics = (characteristics & Spliterator.CONCURRENT) == 0 ? characteristics | Spliterator.SIZED | Spliterator.SUBSIZED : characteristics;
        }
        
        /**
         * Creates a spliterator using the given iterator for a
         * source of unknown size, reporting the given
         * characteristics.
         *
         * @param iterator        the iterator for the source
         * @param characteristics properties of this spliterator's
         *                        source or elements.
         */
        public DoubleIteratorSpliterator(PrimitiveIterator.OfDouble iterator, int characteristics) {
            this.iterator = iterator;
            this.est = Long.MAX_VALUE;
            this.characteristics = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public OfDouble trySplit() {
            
            // 仅有一个元素，或者迭代器无法获取到下一个元素时，返回null
            if(est<=1 || !iterator.hasNext()) {
                return null;
            }
            
            // 初始化本次需要切割的元素数量
            int n = batch + BATCH_UNIT;
            
            // 不能超出原始数据量
            if(n>est) {
                n = (int) est;
            }
            
            // 不能超出切割阈值
            if(n>MAX_BATCH) {
                n = MAX_BATCH;
            }
            
            double[] a = new double[n];
            int j = 0;
            
            do {
                a[j] = iterator.nextDouble();
            } while(++j<n && iterator.hasNext());
            
            // 记录当前批次切割的元素总量，以作为下次切割的参考值
            batch = j;
            
            // 缩减原始数据量
            if(est != Long.MAX_VALUE) {
                est -= j;
            }
            
            return new DoubleArraySpliterator(a, 0, j, characteristics);
        }
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            // 如果已经没有可访问元素，则直接返回false
            if(!iterator.hasNext()) {
                return false;
            }
            
            // 消费下一个元素
            action.accept(iterator.nextDouble());
            
            return true;
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public void forEachRemaining(DoubleConsumer action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            iterator.forEachRemaining(action);
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return est;
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return characteristics;
        }
        
        /*
         * 对于具有SORTED参数的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED参数的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super Double> getComparator() {
            if(hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            
            throw new IllegalStateException();
        }
    }
    
    /*▲ [4] "适配Iterator"的Spliterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Validate inclusive start index and exclusive end index against the length
     * of an array.
     *
     * @param arrayLength The length of the array
     * @param origin      The inclusive start index
     * @param fence       The exclusive end index
     *
     * @throws ArrayIndexOutOfBoundsException if the start index is greater than
     *                                        the end index, if the start index is negative, or the end index is
     *                                        greater than the array length
     */
    // 范围检查
    private static void checkFromToBounds(int arrayLength, int origin, int fence) {
        if(origin > fence) {
            throw new ArrayIndexOutOfBoundsException("origin(" + origin + ") > fence(" + fence + ")");
        }
    
        if(origin < 0) {
            throw new ArrayIndexOutOfBoundsException(origin);
        }
    
        if(fence > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(fence);
        }
    }
    
}
