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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Implementations of {@link Collector} that implement various useful reduction
 * operations, such as accumulating elements into collections, summarizing
 * elements according to various criteria, etc.
 *
 * <p>The following are examples of using the predefined collectors to perform
 * common mutable reduction tasks:
 *
 * <pre>{@code
 * // Accumulate names into a List
 * List<String> list = people.stream()
 *   .map(Person::getName)
 *   .collect(Collectors.toList());
 *
 * // Accumulate names into a TreeSet
 * Set<String> set = people.stream()
 *   .map(Person::getName)
 *   .collect(Collectors.toCollection(TreeSet::new));
 *
 * // Convert elements to strings and concatenate them, separated by commas
 * String joined = things.stream()
 *   .map(Object::toString)
 *   .collect(Collectors.joining(", "));
 *
 * // Compute sum of salaries of employee
 * int total = employees.stream()
 *   .collect(Collectors.summingInt(Employee::getSalary));
 *
 * // Group employees by department
 * Map<Department, List<Employee>> byDept = employees.stream()
 *   .collect(Collectors.groupingBy(Employee::getDepartment));
 *
 * // Compute sum of salaries by department
 * Map<Department, Integer> totalByDept = employees.stream()
 *   .collect(Collectors.groupingBy(Employee::getDepartment,
 *                                  Collectors.summingInt(Employee::getSalary)));
 *
 * // Partition students into passing and failing
 * Map<Boolean, List<Student>> passingFailing = students.stream()
 *   .collect(Collectors.partitioningBy(s -> s.getGrade() >= PASS_THRESHOLD));
 *
 * }</pre>
 *
 * @since 1.8
 */

// 收集器(Collector)工厂，定义了各种收集器，以完成各种收纳操作
public final class Collectors {
    
    static final Set<Collector.Characteristics> CH_NOID
        = Collections.emptySet();
    static final Set<Collector.Characteristics> CH_CONCURRENT_ID
        = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.CONCURRENT, Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH));
    static final Set<Collector.Characteristics> CH_CONCURRENT_NOID
        = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.CONCURRENT, Collector.Characteristics.UNORDERED));
    static final Set<Collector.Characteristics> CH_UNORDERED_ID
        = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH));
    static final Set<Collector.Characteristics> CH_UNORDERED_NOID
        = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.UNORDERED));
    static final Set<Collector.Characteristics> CH_ID
        = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
    
    private Collectors() {
    }
    
    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code Collection}, in encounter order.  The {@code Collection} is
     * created by the provided factory.
     *
     * @param <T>               the type of the input elements
     * @param <C>               the type of the resulting {@code Collection}
     * @param collectionFactory a supplier providing a new empty {@code Collection}
     *                          into which the results will be inserted
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code Collection}, in encounter order
     */
    // 自定义容器，在参数中指定，该容器需要实现add方法
    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<C> collectionFactory) {
        return new CollectorImpl<>(
            collectionFactory,  // 1.构造容器
            Collection<T>::add, // 2.收纳元素
            (r1, r2) -> { r1.addAll(r2); return r1; }, // 3.合并容器
            CH_ID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code List}. There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code List} returned; if more
     * control over the returned {@code List} is required, use {@link #toCollection(Supplier)}.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code List}, in encounter order
     */
    // ArrayList容器，内部定义
    public static <T> Collector<T, ?, List<T>> toList() {
        return new CollectorImpl<>(
            (Supplier<List<T>>) ArrayList::new, // 1.构造容器
            List::add, // 2.收纳元素
            (left, right) -> { left.addAll(right); return left; }, // 3.合并容器
            CH_ID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that accumulates the input elements into an
     * <a href="../List.html#unmodifiable">unmodifiable List</a> in encounter
     * order. The returned Collector disallows null values and will throw
     * {@code NullPointerException} if it is presented with a null value.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code Collector} that accumulates the input elements into an
     * <a href="../List.html#unmodifiable">unmodifiable List</a> in encounter order
     *
     * @since 10
     */
    // 不可变的ArrayList容器，内部定义。按遭遇顺序收纳元素。
    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return new CollectorImpl<>(
            (Supplier<List<T>>) ArrayList::new, // 1.构造容器
            List::add, // 2.收纳元素
            (left, right) -> { left.addAll(right); return left; }, // 3.合并容器
            list -> (List<T>) List.of(list.toArray()), // 4.整理操作
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code Set}. There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code Set} returned; if more
     * control over the returned {@code Set} is required, use
     * {@link #toCollection(Supplier)}.
     *
     * <p>This is an {@link Collector.Characteristics#UNORDERED unordered}
     * Collector.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code Set}
     */
    // HashSet容器，内部定义
    public static <T> Collector<T, ?, Set<T>> toSet() {
        return new CollectorImpl<>(
            (Supplier<Set<T>>) HashSet::new, // 1.构造容器
            Set::add, // 2.收纳元素
            (left, right) -> {
                if(left.size() < right.size()) {
                    right.addAll(left);
                    return right;
                } else {
                    left.addAll(right);
                    return left;
                } }, // 3.合并容器
            CH_UNORDERED_ID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that accumulates the input elements into an
     * <a href="../Set.html#unmodifiable">unmodifiable Set</a>. The returned
     * Collector disallows null values and will throw {@code NullPointerException}
     * if it is presented with a null value. If the input contains duplicate elements,
     * an arbitrary element of the duplicates is preserved.
     *
     * <p>This is an {@link Collector.Characteristics#UNORDERED unordered}
     * Collector.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code Collector} that accumulates the input elements into an
     * <a href="../Set.html#unmodifiable">unmodifiable Set</a>
     *
     * @since 10
     */
    // 不可变的HashSet容器，内部定义。
    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return new CollectorImpl<>(
            (Supplier<Set<T>>) HashSet::new, // 1.构造容器
            Set::add, (left, right) -> {
            if(left.size()<right.size()) {
                right.addAll(left);
                return right;
            } else {
                left.addAll(right);
                return left;
            } }, // 2.收纳元素
            set -> (Set<T>) Set.of(set.toArray()), // 3.合并容器
            CH_UNORDERED_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that concatenates the input elements into a
     * {@code String}, in encounter order.
     *
     * @return a {@code Collector} that concatenates the input elements into a
     * {@code String}, in encounter order
     */
    // StringBuilder容器，拼接String。
    public static Collector<CharSequence, ?, String> joining() {
        return new CollectorImpl<CharSequence, StringBuilder, String>(
            StringBuilder::new, // 1.构造容器
            StringBuilder::append, (r1, r2) -> { r1.append(r2); return r1; }, // 2.收纳元素
            StringBuilder::toString, // 3.合并容器
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that concatenates the input elements,
     * separated by the specified delimiter, in encounter order.
     *
     * @param delimiter the delimiter to be used between each element
     *
     * @return A {@code Collector} which concatenates CharSequence elements,
     * separated by the specified delimiter, in encounter order
     */
    // StringJoiner容器，使用指定的分隔符拼接String。
    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }
    
    /**
     * Returns a {@code Collector} that concatenates the input elements,
     * separated by the specified delimiter, with the specified prefix and
     * suffix, in encounter order.
     *
     * @param delimiter the delimiter to be used between each element
     * @param prefix    the sequence of characters to be used at the beginning
     *                  of the joined result
     * @param suffix    the sequence of characters to be used at the end
     *                  of the joined result
     *
     * @return A {@code Collector} which concatenates CharSequence elements,
     * separated by the specified delimiter, in encounter order
     */
    // StringJoiner容器，使用指定的分隔符、前缀、后缀拼接String。
    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return new CollectorImpl<>(
            () -> new StringJoiner(delimiter, prefix, suffix), // 1.构造容器
            StringJoiner::add, // 2.收纳元素
            StringJoiner::merge, // 3.合并容器
            StringJoiner::toString, // 4.整理操作
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Adapts a {@code Collector} to one accepting elements of the same type
     * {@code T} by applying the predicate to each input element and only
     * accumulating if the predicate returns {@code true}.
     *
     * @param <T>        the type of the input elements
     * @param <A>        intermediate accumulation type of the downstream collector
     * @param <R>        result type of collector
     * @param predicate  a predicate to be applied to the input elements
     * @param downstream a collector which will accept values that match the
     *                   predicate
     *
     * @return a collector which applies the predicate to the input elements
     * and provides matching elements to the downstream collector
     *
     * @apiNote The {@code filtering()} collectors are most useful when used in a
     * multi-level reduction, such as downstream of a {@code groupingBy} or
     * {@code partitioningBy}.  For example, given a stream of
     * {@code Employee}, to accumulate the employees in each department that have a
     * salary above a certain threshold:
     * <pre>{@code
     * Map<Department, Set<Employee>> wellPaidEmployeesByDepartment
     *   = employees.stream().collect(
     *     groupingBy(Employee::getDepartment,
     *                filtering(e -> e.getSalary() > 2000,
     *                          toSet())));
     * }</pre>
     * A filtering collector differs from a stream's {@code filter()} operation.
     * In this example, suppose there are no employees whose salary is above the
     * threshold in some department.  Using a filtering collector as shown above
     * would result in a mapping from that department to an empty {@code Set}.
     * If a stream {@code filter()} operation were done instead, there would be
     * no mapping for that department at all.
     * @since 9
     */
    // 自定义容器，收纳之前先过滤，在groupingBy中很实用
    public static <T, A, R> Collector<T, ?, R> filtering(Predicate<? super T> predicate, Collector<? super T, A, R> downstream) {
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        return new CollectorImpl<>(
            downstream.supplier(), // 1.构造容器
            (r, t) -> {
                if(predicate.test(t)) {
                    downstreamAccumulator.accept(r, t);
                } }, // 2.收纳元素
            downstream.combiner(), // 3.合并容器
            downstream.finisher(), // 4.整理操作
            downstream.characteristics() // 5. 容器的特征值
        );
    }
    
    /**
     * Adapts a {@code Collector} accepting elements of type {@code U} to one
     * accepting elements of type {@code T} by applying a mapping function to
     * each input element before accumulation.
     *
     * @param <T>        the type of the input elements
     * @param <U>        type of elements accepted by downstream collector
     * @param <A>        intermediate accumulation type of the downstream collector
     * @param <R>        result type of collector
     * @param mapper     a function to be applied to the input elements
     * @param downstream a collector which will accept mapped values
     *
     * @return a collector which applies the mapping function to the input
     * elements and provides the mapped results to the downstream collector
     *
     * @apiNote The {@code mapping()} collectors are most useful when used in a
     * multi-level reduction, such as downstream of a {@code groupingBy} or
     * {@code partitioningBy}.  For example, given a stream of
     * {@code Person}, to accumulate the set of last names in each city:
     * <pre>{@code
     * Map<City, Set<String>> lastNamesByCity
     *   = people.stream().collect(
     *     groupingBy(Person::getCity,
     *                mapping(Person::getLastName,
     *                        toSet())));
     * }</pre>
     */
    // 自定义容器，收纳之前先映射，在groupingBy中很实用
    public static <T, U, A, R> Collector<T, ?, R> mapping(Function<? super T, ? extends U> mapper, Collector<? super U, A, R> downstream) {
        BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
        return new CollectorImpl<>(
            downstream.supplier(), // 1.构造容器
            (r, t) -> downstreamAccumulator.accept(r, mapper.apply(t)), // 2.收纳元素
            downstream.combiner(), // 3.合并容器
            downstream.finisher(), // 4.整理操作
            downstream.characteristics() // 5. 容器的特征值
        );
    }
    
    /**
     * Adapts a {@code Collector} accepting elements of type {@code U} to one
     * accepting elements of type {@code T} by applying a flat mapping function
     * to each input element before accumulation.  The flat mapping function
     * maps an input element to a {@link Stream stream} covering zero or more
     * output elements that are then accumulated downstream.  Each mapped stream
     * is {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed downstream.  (If a mapped stream is {@code null}
     * an empty stream is used, instead.)
     *
     * @param <T>        the type of the input elements
     * @param <U>        type of elements accepted by downstream collector
     * @param <A>        intermediate accumulation type of the downstream collector
     * @param <R>        result type of collector
     * @param mapper     a function to be applied to the input elements, which
     *                   returns a stream of results
     * @param downstream a collector which will receive the elements of the
     *                   stream returned by mapper
     *
     * @return a collector which applies the mapping function to the input
     * elements and provides the flat mapped results to the downstream collector
     *
     * @apiNote The {@code flatMapping()} collectors are most useful when used in a
     * multi-level reduction, such as downstream of a {@code groupingBy} or
     * {@code partitioningBy}.  For example, given a stream of
     * {@code Order}, to accumulate the set of line items for each customer:
     * <pre>{@code
     * Map<String, Set<LineItem>> itemsByCustomerName
     *   = orders.stream().collect(
     *     groupingBy(Order::getCustomerName,
     *                flatMapping(order -> order.getLineItems().stream(),
     *                            toSet())));
     * }</pre>
     * @since 9
     */
    // 自定义容器，收纳之前先降维，在groupingBy中很实用
    public static <T, U, A, R> Collector<T, ?, R> flatMapping(Function<? super T, ? extends Stream<? extends U>> mapper, Collector<? super U, A, R> downstream) {
        BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
        return new CollectorImpl<>(
            downstream.supplier(),  // 1.构造容器
            (r, t) -> {
                try(Stream<? extends U> result = mapper.apply(t)) {
                    if(result != null)
                        result.sequential().forEach(u -> downstreamAccumulator.accept(r, u));
                } }, // 2.收纳元素
            downstream.combiner(), // 3.合并容器
            downstream.finisher(), // 4.整理操作
            downstream.characteristics() // 5. 容器的特征值
        );
    }
    
    /**
     * Adapts a {@code Collector} to perform an additional finishing
     * transformation.  For example, one could adapt the {@link #toList()}
     * collector to always produce an immutable list with:
     * <pre>{@code
     * List<String> list = people.stream().collect(
     *   collectingAndThen(toList(),
     *                     Collections::unmodifiableList));
     * }</pre>
     *
     * @param <T>        the type of the input elements
     * @param <A>        intermediate accumulation type of the downstream collector
     * @param <R>        result type of the downstream collector
     * @param <RR>       result type of the resulting collector
     * @param downstream a collector
     * @param finisher   a function to be applied to the final result of the downstream collector
     *
     * @return a collector which performs the action of the downstream collector,
     * followed by an additional finishing step
     */
    // 自定义容器，收纳之后对容器进行操作
    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream, Function<R, RR> finisher) {
        Set<Collector.Characteristics> characteristics = downstream.characteristics();
        if(characteristics.contains(Collector.Characteristics.IDENTITY_FINISH)) {
            if(characteristics.size() == 1)
                characteristics = Collectors.CH_NOID;
            else {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Collector.Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }
        return new CollectorImpl<>(
            downstream.supplier(), // 1.构造容器
            downstream.accumulator(), // 2.收纳元素
            downstream.combiner(), // 3.合并容器
            downstream.finisher().andThen(finisher), // 4.整理操作
            characteristics // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} accepting elements of type {@code T} that
     * counts the number of input elements.  If no elements are present, the
     * result is 0.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code Collector} that counts the input elements
     *
     * @implSpec This produces a result equivalent to:
     * <pre>{@code
     *     reducing(0L, e -> 1L, Long::sum)
     * }</pre>
     */
    // 计数
    public static <T> Collector<T, ?, Long> counting() {
        return summingLong(e -> 1L);
    }
    
    /**
     * Returns a {@code Collector} that produces the sum of a integer-valued
     * function applied to the input elements.  If no elements are present,
     * the result is 0.
     *
     * @param <T>    the type of the input elements
     * @param mapper a function extracting the property to be summed
     *
     * @return a {@code Collector} that produces the sum of a derived property
     */
    // int求和，参数是求和时对每个元素所做的动作
    public static <T> Collector<T, ?, Integer> summingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl<>(
            () -> new int[1], // 1.构造容器
            (a, t) -> { a[0] += mapper.applyAsInt(t); }, // 2.收纳元素
            (a, b) -> { a[0] += b[0]; return a; }, a -> a[0], // 3.合并容器
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that produces the sum of a long-valued
     * function applied to the input elements.  If no elements are present,
     * the result is 0.
     *
     * @param <T>    the type of the input elements
     * @param mapper a function extracting the property to be summed
     *
     * @return a {@code Collector} that produces the sum of a derived property
     */
    // long求和，参数是求和时对每个元素所做的动作
    public static <T> Collector<T, ?, Long> summingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl<>(
            () -> new long[1], // 1.构造容器
            (a, t) -> { a[0] += mapper.applyAsLong(t); }, // 2.收纳元素
            (a, b) -> { a[0] += b[0]; return a; }, // 3.合并容器
            a -> a[0], // 4.整理操作
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that produces the sum of a double-valued
     * function applied to the input elements.  If no elements are present,
     * the result is 0.
     *
     * <p>The sum returned can vary depending upon the order in which
     * values are recorded, due to accumulated rounding error in
     * addition of values of differing magnitudes. Values sorted by increasing
     * absolute magnitude tend to yield more accurate results.  If any recorded
     * value is a {@code NaN} or the sum is at any point a {@code NaN} then the
     * sum will be {@code NaN}.
     *
     * @param <T>    the type of the input elements
     * @param mapper a function extracting the property to be summed
     *
     * @return a {@code Collector} that produces the sum of a derived property
     */
    // double求和，参数是求和时对每个元素所做的动作
    public static <T> Collector<T, ?, Double> summingDouble(ToDoubleFunction<? super T> mapper) {
        /*
         * In the arrays allocated for the collect operation, index 0
         * holds the high-order bits of the running sum, index 1 holds
         * the low-order bits of the sum computed via compensated
         * summation, and index 2 holds the simple sum used to compute
         * the proper result if the stream contains infinite values of
         * the same sign.
         */
        return new CollectorImpl<>(
            () -> new double[3], // 1.构造容器
            (a, t) -> {
                double val = mapper.applyAsDouble(t);
                sumWithCompensation(a, val);
                a[2] += val;
            }, // 2.收纳元素
            (a, b) -> {
                sumWithCompensation(a, b[0]);
                a[2] += b[2];
                return sumWithCompensation(a, b[1]);
            }, // 3.合并容器
            a -> computeFinalSum(a), // 4.整理操作
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that produces the arithmetic mean of an integer-valued
     * function applied to the input elements.  If no elements are present,
     * the result is 0.
     *
     * @param <T>    the type of the input elements
     * @param mapper a function extracting the property to be averaged
     *
     * @return a {@code Collector} that produces the arithmetic mean of a
     * derived property
     */
    // 求int的平均值
    public static <T> Collector<T, ?, Double> averagingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl<>(
            () -> new long[2], // 1.构造容器
            (a, t) -> {
                a[0] += mapper.applyAsInt(t);
                a[1]++;
            }, // 2.收纳元素
            (a, b) -> {
                a[0] += b[0];
                a[1] += b[1];
                return a;
            }, // 3.合并容器
            a -> (a[1] == 0) ? 0.0d : (double) a[0] / a[1], // 4.整理操作
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that produces the arithmetic mean of a long-valued
     * function applied to the input elements.  If no elements are present,
     * the result is 0.
     *
     * @param <T>    the type of the input elements
     * @param mapper a function extracting the property to be averaged
     *
     * @return a {@code Collector} that produces the arithmetic mean of a
     * derived property
     */
    // 求long的平均值
    public static <T> Collector<T, ?, Double> averagingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl<>(
            () -> new long[2],  // 1.构造容器
            (a, t) -> {
                a[0] += mapper.applyAsLong(t);
                a[1]++;
            }, // 2.收纳元素
            (a, b) -> {
                a[0] += b[0];
                a[1] += b[1];
                return a;
            }, // 3.合并容器
            a -> (a[1] == 0) ? 0.0d : (double) a[0] / a[1], // 4.整理操作
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that produces the arithmetic mean of a double-valued
     * function applied to the input elements.  If no elements are present,
     * the result is 0.
     *
     * <p>The average returned can vary depending upon the order in which
     * values are recorded, due to accumulated rounding error in
     * addition of values of differing magnitudes. Values sorted by increasing
     * absolute magnitude tend to yield more accurate results.  If any recorded
     * value is a {@code NaN} or the sum is at any point a {@code NaN} then the
     * average will be {@code NaN}.
     *
     * @param <T>    the type of the input elements
     * @param mapper a function extracting the property to be averaged
     *
     * @return a {@code Collector} that produces the arithmetic mean of a
     * derived property
     *
     * @implNote The {@code double} format can represent all
     * consecutive integers in the range -2<sup>53</sup> to
     * 2<sup>53</sup>. If the pipeline has more than 2<sup>53</sup>
     * values, the divisor in the average computation will saturate at
     * 2<sup>53</sup>, leading to additional numerical errors.
     */
    // 求double的平均值
    public static <T> Collector<T, ?, Double> averagingDouble(ToDoubleFunction<? super T> mapper) {
        /*
         * In the arrays allocated for the collect operation, index 0
         * holds the high-order bits of the running sum, index 1 holds
         * the low-order bits of the sum computed via compensated
         * summation, and index 2 holds the number of values seen.
         */
        return new CollectorImpl<>(
            () -> new double[4], // 1.构造容器
            (a, t) -> {
                double val = mapper.applyAsDouble(t);
                sumWithCompensation(a, val);
                a[2]++;
                a[3] += val;
            }, // 2.收纳元素
            (a, b) -> {
                sumWithCompensation(a, b[0]);
                sumWithCompensation(a, b[1]);
                a[2] += b[2];
                a[3] += b[3];
                return a;
            }, // 3.合并容器
            a -> (a[2] == 0) ? 0.0d : (computeFinalSum(a) / a[2]), // 4.整理操作
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that produces the minimal element according
     * to a given {@code Comparator}, described as an {@code Optional<T>}.
     *
     * @param <T>        the type of the input elements
     * @param comparator a {@code Comparator} for comparing elements
     *
     * @return a {@code Collector} that produces the minimal value
     *
     * @implSpec This produces a result equivalent to:
     * <pre>{@code
     *     reducing(BinaryOperator.minBy(comparator))
     * }</pre>
     */
    // 求最小值（返回值是Optional）
    public static <T> Collector<T, ?, Optional<T>> minBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.minBy(comparator));
    }
    
    /**
     * Returns a {@code Collector} that produces the maximal element according
     * to a given {@code Comparator}, described as an {@code Optional<T>}.
     *
     * @param <T>        the type of the input elements
     * @param comparator a {@code Comparator} for comparing elements
     *
     * @return a {@code Collector} that produces the maximal value
     *
     * @implSpec This produces a result equivalent to:
     * <pre>{@code
     *     reducing(BinaryOperator.maxBy(comparator))
     * }</pre>
     */
    // 求最大值（返回值是Optional）
    public static <T> Collector<T, ?, Optional<T>> maxBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.maxBy(comparator));
    }
    
    /**
     * Returns a {@code Collector} which performs a reduction of its
     * input elements under a specified {@code BinaryOperator}.  The result
     * is described as an {@code Optional<T>}.
     *
     * @param <T> element type for the input and output of the reduction
     * @param op  a {@code BinaryOperator<T>} used to reduce the input elements
     *
     * @return a {@code Collector} which implements the reduction operation
     *
     * @apiNote The {@code reducing()} collectors are most useful when used in a
     * multi-level reduction, downstream of {@code groupingBy} or
     * {@code partitioningBy}.  To perform a simple reduction on a stream,
     * use {@link Stream#reduce(BinaryOperator)} instead.
     *
     * <p>For example, given a stream of {@code Person}, to calculate tallest
     * person in each city:
     * <pre>{@code
     * Comparator<Person> byHeight = Comparator.comparing(Person::getHeight);
     * Map<City, Optional<Person>> tallestByCity
     *   = people.stream().collect(
     *     groupingBy(Person::getCity,
     *                reducing(BinaryOperator.maxBy(byHeight))));
     * }</pre>
     * @see #reducing(Object, BinaryOperator)
     * @see #reducing(Object, Function, BinaryOperator)
     */
    // 使用指定的动作归约输出元素
    public static <T> Collector<T, ?, Optional<T>> reducing(BinaryOperator<T> op) {
        class OptionalBox implements Consumer<T> {
            T value = null;
            boolean present = false;
            
            @Override
            public void accept(T t) {
                if(present) {
                    value = op.apply(value, t);
                } else {
                    value = t;
                    present = true;
                }
            }
        }
        
        return new CollectorImpl<>(
            OptionalBox::new, // 1.构造容器
            OptionalBox::accept, // 2.收纳元素
            (a, b) -> {
                if(b.present) {
                    a.accept(b.value);
                }
                return a;
            }, // 3.合并容器
            a -> Optional.ofNullable(a.value), // 4.整理操作
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} which performs a reduction of its
     * input elements under a specified {@code BinaryOperator} using the
     * provided identity.
     *
     * @param <T>      element type for the input and output of the reduction
     * @param identity the identity value for the reduction (also, the value
     *                 that is returned when there are no input elements)
     * @param op       a {@code BinaryOperator<T>} used to reduce the input elements
     *
     * @return a {@code Collector} which implements the reduction operation
     *
     * @apiNote The {@code reducing()} collectors are most useful when used in a
     * multi-level reduction, downstream of {@code groupingBy} or
     * {@code partitioningBy}.  To perform a simple reduction on a stream,
     * use {@link Stream#reduce(Object, BinaryOperator)}} instead.
     * @see #reducing(BinaryOperator)
     * @see #reducing(Object, Function, BinaryOperator)
     */
    // 使用指定的动作归约输出元素，identity可以看做是初始值
    public static <T> Collector<T, ?, T> reducing(T identity, BinaryOperator<T> op) {
        return new CollectorImpl<>(
            boxSupplier(identity),  // 1.构造容器
            (a, t) -> { a[0] = op.apply(a[0], t); }, // 2.收纳元素
            (a, b) -> {
                a[0] = op.apply(a[0], b[0]);
                return a;
            }, // 3.合并容器
            a -> a[0], // 4.整理操作
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} which performs a reduction of its
     * input elements under a specified mapping function and
     * {@code BinaryOperator}. This is a generalization of
     * {@link #reducing(Object, BinaryOperator)} which allows a transformation
     * of the elements before reduction.
     *
     * @param <T>      the type of the input elements
     * @param <U>      the type of the mapped values
     * @param identity the identity value for the reduction (also, the value
     *                 that is returned when there are no input elements)
     * @param mapper   a mapping function to apply to each input value
     * @param op       a {@code BinaryOperator<U>} used to reduce the mapped values
     *
     * @return a {@code Collector} implementing the map-reduce operation
     *
     * @apiNote The {@code reducing()} collectors are most useful when used in a
     * multi-level reduction, downstream of {@code groupingBy} or
     * {@code partitioningBy}.  To perform a simple map-reduce on a stream,
     * use {@link Stream#map(Function)} and {@link Stream#reduce(Object, BinaryOperator)}
     * instead.
     *
     * <p>For example, given a stream of {@code Person}, to calculate the longest
     * last name of residents in each city:
     * <pre>{@code
     * Comparator<String> byLength = Comparator.comparing(String::length);
     * Map<City, String> longestLastNameByCity
     *   = people.stream().collect(
     *     groupingBy(Person::getCity,
     *                reducing("",
     *                         Person::getLastName,
     *                         BinaryOperator.maxBy(byLength))));
     * }</pre>
     * @see #reducing(Object, BinaryOperator)
     * @see #reducing(BinaryOperator)
     */
    // 使用指定的动作归约输出元素，归约之前，可先对元素执行mapper操作
    public static <T, U> Collector<T, ?, U> reducing(U identity, Function<? super T, ? extends U> mapper, BinaryOperator<U> op) {
        return new CollectorImpl<>(
            boxSupplier(identity), // 1.构造容器
            (a, t) -> { a[0] = op.apply(a[0], mapper.apply(t)); }, // 2.收纳元素
            (a, b) -> { a[0] = op.apply(a[0], b[0]); return a; }, // 3.合并容器
            a -> a[0], // 4.整理操作
            CH_NOID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} implementing a "group by" operation on
     * input elements of type {@code T}, grouping elements according to a
     * classification function, and returning the results in a {@code Map}.
     *
     * <p>The classification function maps elements to some key type {@code K}.
     * The collector produces a {@code Map<K, List<T>>} whose keys are the
     * values resulting from applying the classification function to the input
     * elements, and whose corresponding values are {@code List}s containing the
     * input elements which map to the associated key under the classification
     * function.
     *
     * <p>There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} or {@code List} objects returned.
     *
     * @param <T>        the type of the input elements
     * @param <K>        the type of the keys
     * @param classifier the classifier function mapping input elements to keys
     *
     * @return a {@code Collector} implementing the group-by operation
     *
     * @implSpec This produces a result similar to:
     * <pre>{@code
     *     groupingBy(classifier, toList());
     * }</pre>
     * @implNote The returned {@code Collector} is not concurrent.  For parallel stream
     * pipelines, the {@code combiner} function operates by merging the keys
     * from one map into another, which can be an expensive operation.  If
     * preservation of the order in which elements appear in the resulting {@code Map}
     * collector is not required, using {@link #groupingByConcurrent(Function)}
     * may offer better parallel performance.
     * @see #groupingBy(Function, Collector)
     * @see #groupingBy(Function, Supplier, Collector)
     * @see #groupingByConcurrent(Function)
     */
    // 分组，HashMap容器，classifier用来制定分类依据生成key值，下游收集器默认使用ArrayList
    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> classifier) {
        return groupingBy(classifier, toList());
    }
    
    /**
     * Returns a {@code Collector} implementing a cascaded "group by" operation
     * on input elements of type {@code T}, grouping elements according to a
     * classification function, and then performing a reduction operation on
     * the values associated with a given key using the specified downstream
     * {@code Collector}.
     *
     * <p>The classification function maps elements to some key type {@code K}.
     * The downstream collector operates on elements of type {@code T} and
     * produces a result of type {@code D}. The resulting collector produces a
     * {@code Map<K, D>}.
     *
     * <p>There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code Map} returned.
     *
     * <p>For example, to compute the set of last names of people in each city:
     * <pre>{@code
     * Map<City, Set<String>> namesByCity
     *   = people.stream().collect(
     *     groupingBy(Person::getCity,
     *                mapping(Person::getLastName,
     *                        toSet())));
     * }</pre>
     *
     * @param <T>        the type of the input elements
     * @param <K>        the type of the keys
     * @param <A>        the intermediate accumulation type of the downstream collector
     * @param <D>        the result type of the downstream reduction
     * @param classifier a classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream reduction
     *
     * @return a {@code Collector} implementing the cascaded group-by operation
     *
     * @implNote The returned {@code Collector} is not concurrent.  For parallel stream
     * pipelines, the {@code combiner} function operates by merging the keys
     * from one map into another, which can be an expensive operation.  If
     * preservation of the order in which elements are presented to the downstream
     * collector is not required, using {@link #groupingByConcurrent(Function, Collector)}
     * may offer better parallel performance.
     * @see #groupingBy(Function)
     * @see #groupingBy(Function, Supplier, Collector)
     * @see #groupingByConcurrent(Function, Collector)
     */
    // 分组，HashMap容器，classifier用来制定分类依据生成key值，downstream代表下游的收集器，可以进一步分解上游收集到的元素以作为上游容器的value
    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }
    
    /**
     * Returns a {@code Collector} implementing a cascaded "group by" operation
     * on input elements of type {@code T}, grouping elements according to a
     * classification function, and then performing a reduction operation on
     * the values associated with a given key using the specified downstream
     * {@code Collector}.  The {@code Map} produced by the Collector is created
     * with the supplied factory function.
     *
     * <p>The classification function maps elements to some key type {@code K}.
     * The downstream collector operates on elements of type {@code T} and
     * produces a result of type {@code D}. The resulting collector produces a
     * {@code Map<K, D>}.
     *
     * <p>For example, to compute the set of last names of people in each city,
     * where the city names are sorted:
     * <pre>{@code
     * Map<City, Set<String>> namesByCity
     *   = people.stream().collect(
     *     groupingBy(Person::getCity,
     *                TreeMap::new,
     *                mapping(Person::getLastName,
     *                        toSet())));
     * }</pre>
     *
     * @param <T>        the type of the input elements
     * @param <K>        the type of the keys
     * @param <A>        the intermediate accumulation type of the downstream collector
     * @param <D>        the result type of the downstream reduction
     * @param <M>        the type of the resulting {@code Map}
     * @param classifier a classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @param mapFactory a supplier providing a new empty {@code Map}
     *                   into which the results will be inserted
     *
     * @return a {@code Collector} implementing the cascaded group-by operation
     *
     * @implNote The returned {@code Collector} is not concurrent.  For parallel stream
     * pipelines, the {@code combiner} function operates by merging the keys
     * from one map into another, which can be an expensive operation.  If
     * preservation of the order in which elements are presented to the downstream
     * collector is not required, using {@link #groupingByConcurrent(Function, Supplier, Collector)}
     * may offer better parallel performance.
     * @see #groupingBy(Function, Collector)
     * @see #groupingBy(Function)
     * @see #groupingByConcurrent(Function, Supplier, Collector)
     */
    // 分组，自定义Map类的容器，classifier用来制定分类依据生成key值，downstream代表下游的收集器，可以进一步分解上游收集到的元素以作为上游容器的value
    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BiConsumer<Map<K, A>, T> accumulator = (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
            A container = m.computeIfAbsent(key, k -> downstreamSupplier.get());
            downstreamAccumulator.accept(container, t);
        };
        BinaryOperator<Map<K, A>> merger = Collectors.<K, A, Map<K, A>>mapMerger(downstream.combiner());
        @SuppressWarnings("unchecked")
        Supplier<Map<K, A>> mangledFactory = (Supplier<Map<K, A>>) mapFactory;
        
        if(downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(
                mangledFactory, // 1.构造容器
                accumulator, // 2.收纳元素
                merger, // 3.合并容器
                CH_ID // 5. 容器的特征值
            );
        } else {
            @SuppressWarnings("unchecked")
            Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
            Function<Map<K, A>, M> finisher = intermediate -> {
                intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
                @SuppressWarnings("unchecked")
                M castResult = (M) intermediate;
                return castResult;
            };
            return new CollectorImpl<>(
                mangledFactory, // 1.构造容器
                accumulator, // 2.收纳元素
                merger, // 3.合并容器
                finisher, // 4.整理操作
                CH_NOID // 5. 容器的特征值
            );
        }
    }
    
    /**
     * Returns a concurrent {@code Collector} implementing a "group by"
     * operation on input elements of type {@code T}, grouping elements
     * according to a classification function.
     *
     * <p>This is a {@link Collector.Characteristics#CONCURRENT concurrent} and
     * {@link Collector.Characteristics#UNORDERED unordered} Collector.
     *
     * <p>The classification function maps elements to some key type {@code K}.
     * The collector produces a {@code ConcurrentMap<K, List<T>>} whose keys are the
     * values resulting from applying the classification function to the input
     * elements, and whose corresponding values are {@code List}s containing the
     * input elements which map to the associated key under the classification
     * function.
     *
     * <p>There are no guarantees on the type, mutability, or serializability
     * of the {@code ConcurrentMap} or {@code List} objects returned, or of the
     * thread-safety of the {@code List} objects returned.
     *
     * @param <T>        the type of the input elements
     * @param <K>        the type of the keys
     * @param classifier a classifier function mapping input elements to keys
     *
     * @return a concurrent, unordered {@code Collector} implementing the group-by operation
     *
     * @implSpec This produces a result similar to:
     * <pre>{@code
     *     groupingByConcurrent(classifier, toList());
     * }</pre>
     * @see #groupingBy(Function)
     * @see #groupingByConcurrent(Function, Collector)
     * @see #groupingByConcurrent(Function, Supplier, Collector)
     */
    // 分组，ConcurrentHashMap容器
    public static <T, K> Collector<T, ?, ConcurrentMap<K, List<T>>> groupingByConcurrent(Function<? super T, ? extends K> classifier) {
        return groupingByConcurrent(classifier, ConcurrentHashMap::new, toList());
    }
    
    /**
     * Returns a concurrent {@code Collector} implementing a cascaded "group by"
     * operation on input elements of type {@code T}, grouping elements
     * according to a classification function, and then performing a reduction
     * operation on the values associated with a given key using the specified
     * downstream {@code Collector}.
     *
     * <p>This is a {@link Collector.Characteristics#CONCURRENT concurrent} and
     * {@link Collector.Characteristics#UNORDERED unordered} Collector.
     *
     * <p>The classification function maps elements to some key type {@code K}.
     * The downstream collector operates on elements of type {@code T} and
     * produces a result of type {@code D}. The resulting collector produces a
     * {@code ConcurrentMap<K, D>}.
     *
     * <p>There are no guarantees on the type, mutability, or serializability
     * of the {@code ConcurrentMap} returned.
     *
     * <p>For example, to compute the set of last names of people in each city,
     * where the city names are sorted:
     * <pre>{@code
     * ConcurrentMap<City, Set<String>> namesByCity
     *   = people.stream().collect(
     *     groupingByConcurrent(Person::getCity,
     *                          mapping(Person::getLastName,
     *                                  toSet())));
     * }</pre>
     *
     * @param <T>        the type of the input elements
     * @param <K>        the type of the keys
     * @param <A>        the intermediate accumulation type of the downstream collector
     * @param <D>        the result type of the downstream reduction
     * @param classifier a classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream reduction
     *
     * @return a concurrent, unordered {@code Collector} implementing the cascaded group-by operation
     *
     * @see #groupingBy(Function, Collector)
     * @see #groupingByConcurrent(Function)
     * @see #groupingByConcurrent(Function, Supplier, Collector)
     */
    // 分组，ConcurrentHashMap容器
    public static <T, K, A, D> Collector<T, ?, ConcurrentMap<K, D>> groupingByConcurrent(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return groupingByConcurrent(classifier, ConcurrentHashMap::new, downstream);
    }
    
    /**
     * Returns a concurrent {@code Collector} implementing a cascaded "group by"
     * operation on input elements of type {@code T}, grouping elements
     * according to a classification function, and then performing a reduction
     * operation on the values associated with a given key using the specified
     * downstream {@code Collector}.  The {@code ConcurrentMap} produced by the
     * Collector is created with the supplied factory function.
     *
     * <p>This is a {@link Collector.Characteristics#CONCURRENT concurrent} and
     * {@link Collector.Characteristics#UNORDERED unordered} Collector.
     *
     * <p>The classification function maps elements to some key type {@code K}.
     * The downstream collector operates on elements of type {@code T} and
     * produces a result of type {@code D}. The resulting collector produces a
     * {@code ConcurrentMap<K, D>}.
     *
     * <p>For example, to compute the set of last names of people in each city,
     * where the city names are sorted:
     * <pre>{@code
     * ConcurrentMap<City, Set<String>> namesByCity
     *   = people.stream().collect(
     *     groupingByConcurrent(Person::getCity,
     *                          ConcurrentSkipListMap::new,
     *                          mapping(Person::getLastName,
     *                                  toSet())));
     * }</pre>
     *
     * @param <T>        the type of the input elements
     * @param <K>        the type of the keys
     * @param <A>        the intermediate accumulation type of the downstream collector
     * @param <D>        the result type of the downstream reduction
     * @param <M>        the type of the resulting {@code ConcurrentMap}
     * @param classifier a classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @param mapFactory a supplier providing a new empty {@code ConcurrentMap}
     *                   into which the results will be inserted
     *
     * @return a concurrent, unordered {@code Collector} implementing the cascaded group-by operation
     *
     * @see #groupingByConcurrent(Function)
     * @see #groupingByConcurrent(Function, Collector)
     * @see #groupingBy(Function, Supplier, Collector)
     */
    // 分组，自定义ConcurrentMap类的容器
    public static <T, K, A, D, M extends ConcurrentMap<K, D>> Collector<T, ?, M> groupingByConcurrent(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BinaryOperator<ConcurrentMap<K, A>> merger = Collectors.<K, A, ConcurrentMap<K, A>>mapMerger(downstream.combiner());
        @SuppressWarnings("unchecked")
        Supplier<ConcurrentMap<K, A>> mangledFactory = (Supplier<ConcurrentMap<K, A>>) mapFactory;
        BiConsumer<ConcurrentMap<K, A>, T> accumulator;
        
        if(downstream.characteristics().contains(Collector.Characteristics.CONCURRENT)) {
            accumulator = (m, t) -> {
                K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
                A resultContainer = m.computeIfAbsent(key, k -> downstreamSupplier.get());
                downstreamAccumulator.accept(resultContainer, t);
            };
        } else {
            accumulator = (m, t) -> {
                K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
                A resultContainer = m.computeIfAbsent(key, k -> downstreamSupplier.get());
                synchronized(resultContainer) {
                    downstreamAccumulator.accept(resultContainer, t);
                }
            };
        }
        
        if(downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(
                mangledFactory, // 1.构造容器
                accumulator, // 2.收纳元素
                merger, // 3.合并容器
                CH_CONCURRENT_ID // 5. 容器的特征值
            );
        } else {
            @SuppressWarnings("unchecked")
            Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
            Function<ConcurrentMap<K, A>, M> finisher = intermediate -> {
                intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
                @SuppressWarnings("unchecked")
                M castResult = (M) intermediate;
                return castResult;
            };
            return new CollectorImpl<>(
                mangledFactory, // 1.构造容器
                accumulator, // 2.收纳元素
                merger, // 3.合并容器
                finisher, // 4.整理操作
                CH_CONCURRENT_NOID // 5. 容器的特征值
            );
        }
    }
    
    /**
     * Returns a {@code Collector} which partitions the input elements according
     * to a {@code Predicate}, and organizes them into a
     * {@code Map<Boolean, List<T>>}.
     *
     * The returned {@code Map} always contains mappings for both
     * {@code false} and {@code true} keys.
     * There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code Map} or {@code List}
     * returned.
     *
     * @param <T>       the type of the input elements
     * @param predicate a predicate used for classifying input elements
     *
     * @return a {@code Collector} implementing the partitioning operation
     *
     * @apiNote If a partition has no elements, its value in the result Map will be
     * an empty List.
     * @see #partitioningBy(Predicate, Collector)
     */
    // 分组，下游容器是ArrayList
    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<? super T> predicate) {
        return partitioningBy(predicate, toList());
    }
    
    /**
     * Returns a {@code Collector} which partitions the input elements according
     * to a {@code Predicate}, reduces the values in each partition according to
     * another {@code Collector}, and organizes them into a
     * {@code Map<Boolean, D>} whose values are the result of the downstream
     * reduction.
     *
     * <p>
     * The returned {@code Map} always contains mappings for both
     * {@code false} and {@code true} keys.
     * There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code Map} returned.
     *
     * @param <T>        the type of the input elements
     * @param <A>        the intermediate accumulation type of the downstream collector
     * @param <D>        the result type of the downstream reduction
     * @param predicate  a predicate used for classifying input elements
     * @param downstream a {@code Collector} implementing the downstream
     *                   reduction
     *
     * @return a {@code Collector} implementing the cascaded partitioning
     * operation
     *
     * @apiNote If a partition has no elements, its value in the result Map will be
     * obtained by calling the downstream collector's supplier function and then
     * applying the finisher function.
     * @see #partitioningBy(Predicate)
     */
    // 分组，自定义下游容器
    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(Predicate<? super T> predicate, Collector<? super T, A, D> downstream) {
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BiConsumer<Partition<A>, T> accumulator = (result, t) -> downstreamAccumulator.accept(predicate.test(t) ? result.forTrue : result.forFalse, t);
        BinaryOperator<A> op = downstream.combiner();
        BinaryOperator<Partition<A>> merger = (left, right) -> new Partition<>(op.apply(left.forTrue, right.forTrue), op.apply(left.forFalse, right.forFalse));
        Supplier<Partition<A>> supplier = () -> new Partition<>(downstream.supplier().get(), downstream.supplier().get());
        
        if(downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(
                supplier, // 1.构造容器
                accumulator, // 2.收纳元素
                merger, // 3.合并容器
                CH_ID // 5. 容器的特征值
            );
        } else {
            Function<Partition<A>, Map<Boolean, D>> finisher = par -> new Partition<>(downstream.finisher().apply(par.forTrue), downstream.finisher().apply(par.forFalse));
            return new CollectorImpl<>(
                supplier, // 1.构造容器
                accumulator, // 2.收纳元素
                merger, // 3.合并容器
                finisher, // 4.整理操作
                CH_NOID // 5. 容器的特征值
            );
        }
    }
    
    /**
     * Returns a {@code Collector} that accumulates elements into a
     * {@code Map} whose keys and values are the result of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.  If the mapped keys
     * might have duplicates, use {@link #toMap(Function, Function, BinaryOperator)}
     * instead.
     *
     * <p>There are no guarantees on the type, mutability, serializability,
     * or thread-safety of the {@code Map} returned.
     *
     * @param <T>         the type of the input elements
     * @param <K>         the output type of the key mapping function
     * @param <U>         the output type of the value mapping function
     * @param keyMapper   a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     *
     * @return a {@code Collector} which collects elements into a {@code Map}
     * whose keys and values are the result of applying mapping functions to
     * the input elements
     *
     * @apiNote It is common for either the key or the value to be the input elements.
     * In this case, the utility method
     * {@link java.util.function.Function#identity()} may be helpful.
     * For example, the following produces a {@code Map} mapping
     * students to their grade point average:
     * <pre>{@code
     * Map<Student, Double> studentToGPA
     *   = students.stream().collect(
     *     toMap(Function.identity(),
     *           student -> computeGPA(student)));
     * }</pre>
     * And the following produces a {@code Map} mapping a unique identifier to
     * students:
     * <pre>{@code
     * Map<String, Student> studentIdToStudent
     *   = students.stream().collect(
     *     toMap(Student::getId,
     *           Function.identity()));
     * }</pre>
     * @implNote The returned {@code Collector} is not concurrent.  For parallel stream
     * pipelines, the {@code combiner} function operates by merging the keys
     * from one map into another, which can be an expensive operation.  If it is
     * not required that results are inserted into the {@code Map} in encounter
     * order, using {@link #toConcurrentMap(Function, Function)}
     * may offer better parallel performance.
     * @see #toMap(Function, Function, BinaryOperator)
     * @see #toMap(Function, Function, BinaryOperator, Supplier)
     * @see #toConcurrentMap(Function, Function)
     */
    // 键值对，使用HashMap容器，key不能重复
    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return new CollectorImpl<>(
            HashMap::new, // 1.构造容器
            uniqKeysMapAccumulator(keyMapper, valueMapper), // 2.收纳元素
            uniqKeysMapMerger(), // 3.合并容器
            CH_ID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that accumulates elements into a
     * {@code Map} whose keys and values are the result of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped
     * keys contain duplicates (according to {@link Object#equals(Object)}),
     * the value mapping function is applied to each equal element, and the
     * results are merged using the provided merging function.
     *
     * <p>There are no guarantees on the type, mutability, serializability,
     * or thread-safety of the {@code Map} returned.
     *
     * @param <T>           the type of the input elements
     * @param <K>           the output type of the key mapping function
     * @param <U>           the output type of the value mapping function
     * @param keyMapper     a mapping function to produce keys
     * @param valueMapper   a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key, as supplied
     *                      to {@link Map#merge(Object, Object, BiFunction)}
     *
     * @return a {@code Collector} which collects elements into a {@code Map}
     * whose keys are the result of applying a key mapping function to the input
     * elements, and whose values are the result of applying a value mapping
     * function to all input elements equal to the key and combining them
     * using the merge function
     *
     * @apiNote There are multiple ways to deal with collisions between multiple elements
     * mapping to the same key.  The other forms of {@code toMap} simply use
     * a merge function that throws unconditionally, but you can easily write
     * more flexible merge policies.  For example, if you have a stream
     * of {@code Person}, and you want to produce a "phone book" mapping name to
     * address, but it is possible that two persons have the same name, you can
     * do as follows to gracefully deal with these collisions, and produce a
     * {@code Map} mapping names to a concatenated list of addresses:
     * <pre>{@code
     * Map<String, String> phoneBook
     *   = people.stream().collect(
     *     toMap(Person::getName,
     *           Person::getAddress,
     *           (s, a) -> s + ", " + a));
     * }</pre>
     * @implNote The returned {@code Collector} is not concurrent.  For parallel stream
     * pipelines, the {@code combiner} function operates by merging the keys
     * from one map into another, which can be an expensive operation.  If it is
     * not required that results are merged into the {@code Map} in encounter
     * order, using {@link #toConcurrentMap(Function, Function, BinaryOperator)}
     * may offer better parallel performance.
     * @see #toMap(Function, Function)
     * @see #toMap(Function, Function, BinaryOperator, Supplier)
     * @see #toConcurrentMap(Function, Function, BinaryOperator)
     */
    // 键值对，使用HashMap容器，当key重复时，需要借助合并函数来合并value（所以本质上来说，key还是不重复）
    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
    }
    
    /**
     * Returns a {@code Collector} that accumulates elements into a
     * {@code Map} whose keys and values are the result of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped
     * keys contain duplicates (according to {@link Object#equals(Object)}),
     * the value mapping function is applied to each equal element, and the
     * results are merged using the provided merging function.  The {@code Map}
     * is created by a provided supplier function.
     *
     * @param <T>           the type of the input elements
     * @param <K>           the output type of the key mapping function
     * @param <U>           the output type of the value mapping function
     * @param <M>           the type of the resulting {@code Map}
     * @param keyMapper     a mapping function to produce keys
     * @param valueMapper   a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key, as supplied
     *                      to {@link Map#merge(Object, Object, BiFunction)}
     * @param mapFactory    a supplier providing a new empty {@code Map}
     *                      into which the results will be inserted
     *
     * @return a {@code Collector} which collects elements into a {@code Map}
     * whose keys are the result of applying a key mapping function to the input
     * elements, and whose values are the result of applying a value mapping
     * function to all input elements equal to the key and combining them
     * using the merge function
     *
     * @implNote The returned {@code Collector} is not concurrent.  For parallel stream
     * pipelines, the {@code combiner} function operates by merging the keys
     * from one map into another, which can be an expensive operation.  If it is
     * not required that results are merged into the {@code Map} in encounter
     * order, using {@link #toConcurrentMap(Function, Function, BinaryOperator, Supplier)}
     * may offer better parallel performance.
     * @see #toMap(Function, Function)
     * @see #toMap(Function, Function, BinaryOperator)
     * @see #toConcurrentMap(Function, Function, BinaryOperator, Supplier)
     */
    // 键值对，自定义Map类容器接收元素，当key重复时，需要合并value
    public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction, Supplier<M> mapFactory) {
        BiConsumer<M, T> accumulator = (map, element) -> map.merge(keyMapper.apply(element), valueMapper.apply(element), mergeFunction);
        return new CollectorImpl<>(
            mapFactory, // 1.构造容器
            accumulator, // 2.收纳元素
            mapMerger(mergeFunction), // 3.合并容器
            CH_ID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} that accumulates the input elements into an
     * <a href="../Map.html#unmodifiable">unmodifiable Map</a>,
     * whose keys and values are the result of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.  If the mapped keys
     * might have duplicates, use {@link #toUnmodifiableMap(Function, Function, BinaryOperator)}
     * to handle merging of the values.
     *
     * <p>The returned Collector disallows null keys and values. If either mapping function
     * returns null, {@code NullPointerException} will be thrown.
     *
     * @param <T>         the type of the input elements
     * @param <K>         the output type of the key mapping function
     * @param <U>         the output type of the value mapping function
     * @param keyMapper   a mapping function to produce keys, must be non-null
     * @param valueMapper a mapping function to produce values, must be non-null
     *
     * @return a {@code Collector} that accumulates the input elements into an
     * <a href="../Map.html#unmodifiable">unmodifiable Map</a>, whose keys and values
     * are the result of applying the provided mapping functions to the input elements
     *
     * @throws NullPointerException if either keyMapper or valueMapper is null
     * @see #toUnmodifiableMap(Function, Function, BinaryOperator)
     * @since 10
     */
    // 键值对，使用HashMap容器，key不能重复，元素放到容器后不能被修改
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper");
        Objects.requireNonNull(valueMapper, "valueMapper");
        return collectingAndThen(toMap(keyMapper, valueMapper), map -> (Map<K, U>) Map.ofEntries(map.entrySet().toArray(new Map.Entry[0])));
    }
    
    /**
     * Returns a {@code Collector} that accumulates the input elements into an
     * <a href="../Map.html#unmodifiable">unmodifiable Map</a>,
     * whose keys and values are the result of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped
     * keys contain duplicates (according to {@link Object#equals(Object)}),
     * the value mapping function is applied to each equal element, and the
     * results are merged using the provided merging function.
     *
     * <p>The returned Collector disallows null keys and values. If either mapping function
     * returns null, {@code NullPointerException} will be thrown.
     *
     * @param <T>           the type of the input elements
     * @param <K>           the output type of the key mapping function
     * @param <U>           the output type of the value mapping function
     * @param keyMapper     a mapping function to produce keys, must be non-null
     * @param valueMapper   a mapping function to produce values, must be non-null
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key, as supplied
     *                      to {@link Map#merge(Object, Object, BiFunction)},
     *                      must be non-null
     *
     * @return a {@code Collector} that accumulates the input elements into an
     * <a href="../Map.html#unmodifiable">unmodifiable Map</a>, whose keys and values
     * are the result of applying the provided mapping functions to the input elements
     *
     * @throws NullPointerException if the keyMapper, valueMapper, or mergeFunction is null
     * @see #toUnmodifiableMap(Function, Function)
     * @since 10
     */
    // 键值对，使用HashMap容器，当key重复时，需要合并value，元素放到容器后不能被修改
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        Objects.requireNonNull(keyMapper, "keyMapper");
        Objects.requireNonNull(valueMapper, "valueMapper");
        Objects.requireNonNull(mergeFunction, "mergeFunction");
        return collectingAndThen(toMap(keyMapper, valueMapper, mergeFunction, HashMap::new), map -> (Map<K, U>) Map.ofEntries(map.entrySet().toArray(new Map.Entry[0])));
    }
    
    /**
     * Returns a concurrent {@code Collector} that accumulates elements into a
     * {@code ConcurrentMap} whose keys and values are the result of applying
     * the provided mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to
     * {@link Object#equals(Object)}), an {@code IllegalStateException} is
     * thrown when the collection operation is performed.  If the mapped keys
     * may have duplicates, use
     * {@link #toConcurrentMap(Function, Function, BinaryOperator)} instead.
     *
     * <p>There are no guarantees on the type, mutability, or serializability
     * of the {@code ConcurrentMap} returned.
     *
     * @param <T>         the type of the input elements
     * @param <K>         the output type of the key mapping function
     * @param <U>         the output type of the value mapping function
     * @param keyMapper   the mapping function to produce keys
     * @param valueMapper the mapping function to produce values
     *
     * @return a concurrent, unordered {@code Collector} which collects elements into a
     * {@code ConcurrentMap} whose keys are the result of applying a key mapping
     * function to the input elements, and whose values are the result of
     * applying a value mapping function to the input elements
     *
     * @apiNote It is common for either the key or the value to be the input elements.
     * In this case, the utility method
     * {@link java.util.function.Function#identity()} may be helpful.
     * For example, the following produces a {@code ConcurrentMap} mapping
     * students to their grade point average:
     * <pre>{@code
     * ConcurrentMap<Student, Double> studentToGPA
     *   = students.stream().collect(
     *     toConcurrentMap(Function.identity(),
     *                     student -> computeGPA(student)));
     * }</pre>
     * And the following produces a {@code ConcurrentMap} mapping a
     * unique identifier to students:
     * <pre>{@code
     * ConcurrentMap<String, Student> studentIdToStudent
     *   = students.stream().collect(
     *     toConcurrentMap(Student::getId,
     *                     Function.identity()));
     * }</pre>
     *
     * <p>This is a {@link Collector.Characteristics#CONCURRENT concurrent} and
     * {@link Collector.Characteristics#UNORDERED unordered} Collector.
     * @see #toMap(Function, Function)
     * @see #toConcurrentMap(Function, Function, BinaryOperator)
     * @see #toConcurrentMap(Function, Function, BinaryOperator, Supplier)
     */
    // 键值对，使用ConcurrentHashMap容器，key不能重复
    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return new CollectorImpl<>(
            ConcurrentHashMap::new, // 1.构造容器
            uniqKeysMapAccumulator(keyMapper, valueMapper), // 2.收纳元素
            uniqKeysMapMerger(), // 3.合并容器
            CH_CONCURRENT_ID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a concurrent {@code Collector} that accumulates elements into a
     * {@code ConcurrentMap} whose keys and values are the result of applying
     * the provided mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to {@link Object#equals(Object)}),
     * the value mapping function is applied to each equal element, and the
     * results are merged using the provided merging function.
     *
     * <p>There are no guarantees on the type, mutability, or serializability
     * of the {@code ConcurrentMap} returned.
     *
     * @param <T>           the type of the input elements
     * @param <K>           the output type of the key mapping function
     * @param <U>           the output type of the value mapping function
     * @param keyMapper     a mapping function to produce keys
     * @param valueMapper   a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key, as supplied
     *                      to {@link Map#merge(Object, Object, BiFunction)}
     *
     * @return a concurrent, unordered {@code Collector} which collects elements into a
     * {@code ConcurrentMap} whose keys are the result of applying a key mapping
     * function to the input elements, and whose values are the result of
     * applying a value mapping function to all input elements equal to the key
     * and combining them using the merge function
     *
     * @apiNote There are multiple ways to deal with collisions between multiple elements
     * mapping to the same key.  The other forms of {@code toConcurrentMap} simply use
     * a merge function that throws unconditionally, but you can easily write
     * more flexible merge policies.  For example, if you have a stream
     * of {@code Person}, and you want to produce a "phone book" mapping name to
     * address, but it is possible that two persons have the same name, you can
     * do as follows to gracefully deal with these collisions, and produce a
     * {@code ConcurrentMap} mapping names to a concatenated list of addresses:
     * <pre>{@code
     * ConcurrentMap<String, String> phoneBook
     *   = people.stream().collect(
     *     toConcurrentMap(Person::getName,
     *                     Person::getAddress,
     *                     (s, a) -> s + ", " + a));
     * }</pre>
     *
     * <p>This is a {@link Collector.Characteristics#CONCURRENT concurrent} and
     * {@link Collector.Characteristics#UNORDERED unordered} Collector.
     * @see #toConcurrentMap(Function, Function)
     * @see #toConcurrentMap(Function, Function, BinaryOperator, Supplier)
     * @see #toMap(Function, Function, BinaryOperator)
     */
    // 键值对，使用ConcurrentHashMap容器，当key重复时，需要合并value
    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, ConcurrentHashMap::new);
    }
    
    /**
     * Returns a concurrent {@code Collector} that accumulates elements into a
     * {@code ConcurrentMap} whose keys and values are the result of applying
     * the provided mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to {@link Object#equals(Object)}),
     * the value mapping function is applied to each equal element, and the
     * results are merged using the provided merging function.  The
     * {@code ConcurrentMap} is created by a provided supplier function.
     *
     * <p>This is a {@link Collector.Characteristics#CONCURRENT concurrent} and
     * {@link Collector.Characteristics#UNORDERED unordered} Collector.
     *
     * @param <T>           the type of the input elements
     * @param <K>           the output type of the key mapping function
     * @param <U>           the output type of the value mapping function
     * @param <M>           the type of the resulting {@code ConcurrentMap}
     * @param keyMapper     a mapping function to produce keys
     * @param valueMapper   a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key, as supplied
     *                      to {@link Map#merge(Object, Object, BiFunction)}
     * @param mapFactory    a supplier providing a new empty {@code ConcurrentMap}
     *                      into which the results will be inserted
     *
     * @return a concurrent, unordered {@code Collector} which collects elements into a
     * {@code ConcurrentMap} whose keys are the result of applying a key mapping
     * function to the input elements, and whose values are the result of
     * applying a value mapping function to all input elements equal to the key
     * and combining them using the merge function
     *
     * @see #toConcurrentMap(Function, Function)
     * @see #toConcurrentMap(Function, Function, BinaryOperator)
     * @see #toMap(Function, Function, BinaryOperator, Supplier)
     */
    // 键值对，自定义ConcurrentMap类容器，当key重复时，需要合并value
    public static <T, K, U, M extends ConcurrentMap<K, U>> Collector<T, ?, M> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction, Supplier<M> mapFactory) {
        BiConsumer<M, T> accumulator = (map, element) -> map.merge(keyMapper.apply(element), valueMapper.apply(element), mergeFunction);
        return new CollectorImpl<>(
            mapFactory, // 1.构造容器
            accumulator, // 2.收纳元素
            mapMerger(mergeFunction), // 3.合并容器
            CH_CONCURRENT_ID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} which applies an {@code int}-producing
     * mapping function to each input element, and returns summary statistics
     * for the resulting values.
     *
     * @param <T>    the type of the input elements
     * @param mapper a mapping function to apply to each element
     *
     * @return a {@code Collector} implementing the summary-statistics reduction
     *
     * @see #summarizingDouble(ToDoubleFunction)
     * @see #summarizingLong(ToLongFunction)
     */
    // 信息统计，对int类型的元素统计相关信息：计数、求和、均值、最小值、最大值
    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl<T, IntSummaryStatistics, IntSummaryStatistics>(
            IntSummaryStatistics::new,  // 1.构造容器
            (r, t) -> r.accept(mapper.applyAsInt(t)), // 2.收纳元素
            (l, r) -> { l.combine(r); return l; }, // 3.合并容器
            CH_ID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} which applies an {@code long}-producing
     * mapping function to each input element, and returns summary statistics
     * for the resulting values.
     *
     * @param <T>    the type of the input elements
     * @param mapper the mapping function to apply to each element
     *
     * @return a {@code Collector} implementing the summary-statistics reduction
     *
     * @see #summarizingDouble(ToDoubleFunction)
     * @see #summarizingInt(ToIntFunction)
     */
    // 信息统计，对long类型的元素统计相关信息：计数、求和、均值、最小值、最大值
    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl<T, LongSummaryStatistics, LongSummaryStatistics>(
            LongSummaryStatistics::new, // 1.构造容器
            (r, t) -> r.accept(mapper.applyAsLong(t)), // 2.收纳元素
            (l, r) -> { l.combine(r); return l; }, // 3.合并容器
            CH_ID // 5. 容器的特征值
        );
    }
    
    /**
     * Returns a {@code Collector} which applies an {@code double}-producing
     * mapping function to each input element, and returns summary statistics
     * for the resulting values.
     *
     * @param <T>    the type of the input elements
     * @param mapper a mapping function to apply to each element
     *
     * @return a {@code Collector} implementing the summary-statistics reduction
     *
     * @see #summarizingLong(ToLongFunction)
     * @see #summarizingInt(ToIntFunction)
     */
    // 信息统计，对double类型的元素统计相关信息：计数、求和、均值、最小值、最大值
    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(ToDoubleFunction<? super T> mapper) {
        return new CollectorImpl<T, DoubleSummaryStatistics, DoubleSummaryStatistics>(
            DoubleSummaryStatistics::new, // 1.构造容器
            (r, t) -> r.accept(mapper.applyAsDouble(t)), // 2.收纳元素
            (l, r) -> { l.combine(r); return l; }, // 3.合并容器
            CH_ID // 5. 容器的特征值
        );
    }
    
    
    
    /**
     * Incorporate a new double value using Kahan summation / compensation summation.
     *
     * High-order bits of the sum are in intermediateSum[0],
     * low-order bits of the sum are in intermediateSum[1],
     * any additional elements are application-specific.
     *
     * @param intermediateSum the high-order and low-order words of the intermediate sum
     * @param value           the name value to be included in the running sum
     */
    /*
     * Kahan求和精度补偿算法，弥补浮点运算中的精度损失
     * intermediateSum[0]：存储的是上次求和的结果（有精度损失）
     * intermediateSum[1]：存储的是上次运算产生的误差（精度损失）
     */
    static double[] sumWithCompensation(double[] intermediateSum, double value) {
        double tmp = value - intermediateSum[1];    // 当前的值补上上一次的误差
        double sum = intermediateSum[0];            // 前一次的和
        // Little wolf of rounding error
        double velvel = sum + tmp;                  // 本次的求和结果，已补上上次的误差
        intermediateSum[1] = (velvel - sum) - tmp;  // 本次运算产生的新误差
        intermediateSum[0] = velvel;                // 记下本次的求和结果
        
        return intermediateSum;
    }
    
    /**
     * If the compensated sum is spuriously NaN from accumulating one or more same-signed infinite values,
     * return the correctly-signed infinity stored in the simple sum.
     */
    // 返回求和的结果，要考虑精度损失的问题
    static double computeFinalSum(double[] summands) {
        // Better error bounds to add both terms as the final sum
        double tmp = summands[0] + summands[1];
        double simpleSum = summands[summands.length - 1];
        if(Double.isNaN(tmp) && Double.isInfinite(simpleSum)) {
            return simpleSum;
        } else {
            return tmp;
        }
    }
    
    /**
     * {@code BinaryOperator<Map>} that merges the contents of its right
     * argument into its left argument, throwing {@code IllegalStateException}
     * if duplicate keys are encountered.
     *
     * @param <K> type of the map keys
     * @param <V> type of the map values
     * @param <M> type of the map
     *
     * @return a merge function for two maps
     */
    // 确保key不重复，否则抛异常
    private static <K, V, M extends Map<K, V>> BinaryOperator<M> uniqKeysMapMerger() {
        return (m1, m2) -> {
            for(Map.Entry<K, V> e : m2.entrySet()) {
                K k = e.getKey();
                V v = Objects.requireNonNull(e.getValue());
                V u = m1.putIfAbsent(k, v);
                if(u != null) {
                    throw duplicateKeyException(k, u, v);
                }
            }
            return m1;
        };
    }
    
    /**
     * {@code BiConsumer<Map, T>} that accumulates (key, value) pairs
     * extracted from elements into the map, throwing {@code IllegalStateException}
     * if duplicate keys are encountered.
     *
     * @param keyMapper   a function that maps an element into a key
     * @param valueMapper a function that maps an element into a value
     * @param <T>         type of elements
     * @param <K>         type of map keys
     * @param <V>         type of map values
     *
     * @return an accumulating consumer
     */
    // 确保key不重复，否则抛异常
    private static <T, K, V> BiConsumer<Map<K, V>, T> uniqKeysMapAccumulator(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return (map, element) -> {
            K k = keyMapper.apply(element);
            V v = Objects.requireNonNull(valueMapper.apply(element));
            V u = map.putIfAbsent(k, v);
            if(u != null) {
                throw duplicateKeyException(k, u, v);
            }
        };
    }
    
    // 强制类型转换
    @SuppressWarnings("unchecked")
    private static <I, R> Function<I, R> castingIdentity() {
        return i -> (R) i;
    }
    
    /**
     * {@code BinaryOperator<Map>} that merges the contents of its right
     * argument into its left argument, using the provided merge function to
     * handle duplicate keys.
     *
     * @param <K>           type of the map keys
     * @param <V>           type of the map values
     * @param <M>           type of the map
     * @param mergeFunction A merge function suitable for
     *                      {@link Map#merge(Object, Object, BiFunction) Map.merge()}
     *
     * @return a merge function for two maps
     */
    // key出现重复时，合并对应的value
    private static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(BinaryOperator<V> mergeFunction) {
        return (m1, m2) -> {
            for(Map.Entry<K, V> e : m2.entrySet()) {
                m1.merge(e.getKey(), e.getValue(), mergeFunction);
            }
            return m1;
        };
    }
    
    // 将identity包装到数组后返回
    @SuppressWarnings("unchecked")
    private static <T> Supplier<T[]> boxSupplier(T identity) {
        return () -> (T[]) new Object[]{identity};
    }
    
    /**
     * Construct an {@code IllegalStateException} with appropriate message.
     *
     * @param k the duplicate key
     * @param u 1st value to be accumulated/merged
     * @param v 2nd value to be accumulated/merged
     */
    // key重复的异常
    private static IllegalStateException duplicateKeyException(Object k, Object u, Object v) {
        return new IllegalStateException(String.format("Duplicate key %s (attempted merging values %s and %s)", k, u, v));
    }
    
    
    
    /**
     * Simple implementation class for {@code Collector}.
     *
     * @param <T> the type of elements to be collected
     * @param <R> the type of the result
     */
    // 收集器Collector的实现类
    static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
        private final Supplier<A> supplier;         // 1.构造容器
        private final BiConsumer<A, T> accumulator; // 2.收纳元素
        private final BinaryOperator<A> combiner;   // 3.合并容器
        private final Function<A, R> finisher;      // 4.整理操作
        private final Set<Characteristics> characteristics; // 5. 容器的特征值
        
        CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Function<A, R> finisher, Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }
        
        CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Set<Characteristics> characteristics) {
            this(supplier, accumulator, combiner, castingIdentity(), characteristics);
        }
        
        // 1.构造容器
        @Override
        public Supplier<A> supplier() {
            return supplier;
        }
        
        // 2.收纳元素
        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }
        
        // 3.合并容器
        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }
        
        // 4.整理操作
        @Override
        public Function<A, R> finisher() {
            return finisher;
        }
        
        // 5. 容器的特征值
        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }
    
    /**
     * Implementation class used by partitioningBy.
     */
    // partitioningBy中使用的容器
    private static final class Partition<T> extends AbstractMap<Boolean, T> implements Map<Boolean, T> {
        final T forTrue;
        final T forFalse;
        
        Partition(T forTrue, T forFalse) {
            this.forTrue = forTrue;
            this.forFalse = forFalse;
        }
        
        @Override
        public Set<Map.Entry<Boolean, T>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public Iterator<Map.Entry<Boolean, T>> iterator() {
                    Map.Entry<Boolean, T> falseEntry = new SimpleImmutableEntry<>(false, forFalse);
                    Map.Entry<Boolean, T> trueEntry = new SimpleImmutableEntry<>(true, forTrue);
                    return List.of(falseEntry, trueEntry).iterator();
                }
                
                @Override
                public int size() {
                    return 2;
                }
            };
        }
    }
}
