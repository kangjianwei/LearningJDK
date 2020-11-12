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
import java.util.stream.Collector.Characteristics;

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
/*
 * 收集器(Collector)工厂
 *
 * 该工厂中允许定制各种收集器，以完成不同的收集操作。
 */
public final class Collectors {
    
    // 未设置任何参数
    static final Set<Collector.Characteristics> CH_NOID = Collections.emptySet();
    
    // 并发容器/无序收集/无需收尾
    static final Set<Collector.Characteristics> CH_CONCURRENT_ID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.CONCURRENT, Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH));
    
    // 并发容器/无序收集
    static final Set<Collector.Characteristics> CH_CONCURRENT_NOID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.CONCURRENT, Collector.Characteristics.UNORDERED));
    
    // 无序收集/无需收尾
    static final Set<Collector.Characteristics> CH_UNORDERED_ID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH));
    
    // 无序收集
    static final Set<Collector.Characteristics> CH_UNORDERED_NOID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.UNORDERED));
    
    // 无需收尾
    static final Set<Collector.Characteristics> CH_ID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private Collectors() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 简单收集 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 收集元素到自定义容器
    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<C> collectionFactory) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   由入参提供。
         */
        Supplier<C> supplier = collectionFactory;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素添加到容器中。
         */
        BiConsumer<C, T> accumulator = Collection::add;
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的元素添加到前一个容器中，并返回前一个容器。
         */
        BinaryOperator<C> combiner = (collection1, collection2) -> {
            collection1.addAll(collection2);
            return collection1;
        };
    
        /*
         * 5.容器参数，指示容器的特征。
         *   无需收尾。
         */
        Set<Characteristics> characteristics = CH_ID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
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
    // 收集元素到ArrayList容器
    public static <T> Collector<T, ?, List<T>> toList() {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   构造一个ArrayList。
         */
        Supplier<List<T>> supplier = (Supplier<List<T>>) ArrayList::new;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素添加到容器中。
         */
        BiConsumer<List<T>, T> accumulator = List::add;
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的元素添加到前一个容器中，并返回前一个容器。
         */
        BinaryOperator<List<T>> combiner = (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        };
    
        /*
         * 5.容器参数，指示容器的特征。
         *   无需收尾。
         */
        Set<Characteristics> characteristics = CH_ID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
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
    // 收集元素到只读的List容器
    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   构造一个ArrayList。
         */
        Supplier<List<T>> supplier = (Supplier<List<T>>) ArrayList::new;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素添加到容器中。
         */
        BiConsumer<List<T>, T> accumulator = List::add;
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的元素添加到前一个容器中，并返回前一个容器。
         */
        BinaryOperator<List<T>> combiner = (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   将之前收集到的元素存入一个只读容器后返回。
         */
        Function<List<T>, List<T>> finisher = list -> (List<T>) List.of(list.toArray());
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
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
    // 收集元素到HashSet容器
    public static <T> Collector<T, ?, Set<T>> toSet() {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   构造一个HashSet。
         */
        Supplier<Set<T>> supplier = (Supplier<Set<T>>) HashSet::new;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素添加到容器中。
         */
        BiConsumer<Set<T>, T> accumulator = Set::add;
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将小容量容器中的元素添加到大容量容器中，这样做可以节省一定的时间。
         */
        BinaryOperator<Set<T>> combiner = (left, right) -> {
            if(left.size()<right.size()) {
                right.addAll(left);
                return right;
            } else {
                left.addAll(right);
                return left;
            }
        };
    
        /*
         * 5.容器参数，指示容器的特征。
         *   无序收集/无需收尾。
         */
        Set<Characteristics> characteristics = CH_UNORDERED_ID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
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
    // 收集元素到只读的Set容器
    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   构造一个HashSet。
         */
        Supplier<Set<T>> supplier = (Supplier<Set<T>>) HashSet::new;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素添加到容器中。
         */
        BiConsumer<Set<T>, T> accumulator = Set::add;
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将小容量容器中的元素添加到大容量容器中，这样做可以节省一定的时间。
         */
        BinaryOperator<Set<T>> combiner = (left, right) -> {
            if(left.size()<right.size()) {
                right.addAll(left);
                return right;
            } else {
                left.addAll(right);
                return left;
            }
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   将之前收集到的元素存入一个只读容器后返回。
         */
        Function<Set<T>, Set<T>> finisher = set -> (Set<T>) Set.of(set.toArray());
    
        /*
         * 5.容器参数，指示容器的特征。
         *   无序收集
         */
        Set<Characteristics> characteristics = CH_UNORDERED_NOID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
    }
    
    /**
     * Returns a {@code Collector} that concatenates the input elements into a
     * {@code String}, in encounter order.
     *
     * @return a {@code Collector} that concatenates the input elements into a
     * {@code String}, in encounter order
     */
    // 收集字符串到StringBuilder容器，并转换为String后返回
    public static Collector<CharSequence, ?, String> joining() {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   构造一个StringBuilder。
         */
        Supplier<StringBuilder> supplier = StringBuilder::new;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素添加到容器中。
         */
        BiConsumer<StringBuilder, CharSequence> accumulator = StringBuilder::append;
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的元素添加到前一个容器中，并返回前一个容器。
         */
        BinaryOperator<StringBuilder> combiner = (builder, charSequence) -> {
            builder.append(charSequence);
            return builder;
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   将之前收集到的字符序列转换为String后返回。
         */
        Function<StringBuilder, String> finisher = StringBuilder::toString;
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
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
    // 收集字符串到StringJoiner容器，并转换为String后返回；字符串之间使用delimiter分割
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
    // 收集字符串到StringJoiner容器，并转换为String后返回；字符串之间使用delimiter分割，并且带有前缀prefix和后缀suffix
    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   构造一个StringJoiner。
         */
        Supplier<StringJoiner> supplier = () -> new StringJoiner(delimiter, prefix, suffix);
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素添加到容器中。
         */
        BiConsumer<StringJoiner, CharSequence> accumulator = StringJoiner::add;
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的元素添加到前一个容器中，并返回前一个容器。
         */
        BinaryOperator<StringJoiner> combiner = StringJoiner::merge;
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   将之前收集到的字符序列转换为String后返回。
         */
        Function<StringJoiner, String> finisher = StringJoiner::toString;
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
    }
    
    /*▲ 简单收集 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 过滤 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Adapts a {@code Collector} to one accepting elements of the same type
     * {@code T} by applying the predicate to each input element and only
     * accumulating if the predicate returns {@code true}.
     *
     * @param <T>        the type of the input elements
     * @param <A>        intermediate accumulation type of the downstream collector
     * @param <R>        result type of collector
     * @param predicate  a predicate to be applied to the input elements
     * @param downstream a collector which will accept values that match the predicate
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
    // 对上游的数据进行过滤：只保存符合predicate条件的元素，过滤后存储到downstream中
    public static <T, A, R> Collector<T, ?, R> filtering(Predicate<? super T> predicate, Collector<? super T, A, R> downstream) {
        
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   使用downstream中的容器工厂。
         */
        Supplier<A> supplier = downstream.supplier();
        
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   使用downstream中的择取操作，但是择取每个元素之前，必须先使用predicate对元素过滤，保留满足条件的元素。
         */
        BiConsumer<A, T> accumulator = (r, t) -> {
            if(predicate.test(t)) {
                downstream.accumulator().accept(r, t);
            }
        };
        
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   使用downstream中的合并操作。
         */
        BinaryOperator<A> combiner = downstream.combiner();
        
        /*
         * 4.收尾操作（可选，最后执行）。
         *   使用downstream中的收尾操作。
         */
        Function<A, R> finisher = downstream.finisher();
        
        /*
         * 5.容器参数，指示容器的特征。
         *   使用downstream中的容器参数。
         */
        Set<Characteristics> characteristics = downstream.characteristics();
        
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
    }
    
    /*▲ 过滤 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 映射 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 对上游的数据进行映射：将上游的元素使用mapper映射后再保存，映射后存储到downstream中
    public static <T, U, A, R> Collector<T, ?, R> mapping(Function<? super T, ? extends U> mapper, Collector<? super U, A, R> downstream) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   使用downstream中的容器工厂。
         */
        Supplier<A> supplier = downstream.supplier();
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   使用downstream中的择取操作，但是择取每个元素之前，必须先使用mapper对元素映射，保留转换之后的元素。
         */
        BiConsumer<A, T> accumulator = (r, t) -> downstream.accumulator().accept(r, mapper.apply(t));
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   使用downstream中的合并操作。
         */
        BinaryOperator<A> combiner = downstream.combiner();
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   使用downstream中的收尾操作。
         */
        Function<A, R> finisher = downstream.finisher();
    
        /*
         * 5.容器参数，指示容器的特征。
         *   使用downstream中的容器参数。
         */
        Set<Characteristics> characteristics = downstream.characteristics();
    
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
    }
    
    /*▲ 映射 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 降维 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 对上游的数据进行降维：如果上游的元素本身是一个容器，则将该容器中的元素逐一选出来，再存入downstream中；mapper的作用是将上游的元素流化
    public static <T, U, A, R> Collector<T, ?, R> flatMapping(Function<? super T, ? extends Stream<? extends U>> mapper, Collector<? super U, A, R> downstream) {
        
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   使用downstream中的容器工厂。
         */
        Supplier<A> supplier = downstream.supplier();
        
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   使用downstream中的择取操作，但是择取每个元素之前，必须先将该元素内的子元素分别取出来，再对其进行择取，则相当于"降维"。
         */
        BiConsumer<A, T> accumulator = (r, t) -> {
            try(Stream<? extends U> result = mapper.apply(t)) {
                if(result != null) {
                    result.sequential().forEach(u -> downstream.accumulator().accept(r, u));
                }
            }
        };
        
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   使用downstream中的合并操作。
         */
        BinaryOperator<A> combiner = downstream.combiner();
        
        /*
         * 4.收尾操作（可选，最后执行）。
         *   使用downstream中的收尾操作。
         */
        Function<A, R> finisher = downstream.finisher();
        
        /*
         * 5.容器参数，指示容器的特征。
         *   使用downstream中的容器参数。
         */
        Set<Characteristics> characteristics = downstream.characteristics();
        
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
    }
    
    /*▲ 降维 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 后处理 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 对上游的数据进行后处理：对上游的元素先使用downstream中的收尾操作处理，再使用thenFinisher这个收尾操作处理；后处理的结果存储到downstream中
    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream, Function<R, RR> thenFinisher) {
        
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   使用downstream中的容器工厂。
         */
        Supplier<A> supplier = downstream.supplier();
        
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   使用downstream中的择取操作。
         */
        BiConsumer<A, T> accumulator = downstream.accumulator();
        
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   使用downstream中的合并操作。
         */
        BinaryOperator<A> combiner = downstream.combiner();
        
        /*
         * 4.收尾操作（可选，最后执行）。
         *   使用downstream中的收尾操作之后，还要再使用另一个收尾操作。
         */
        Function<A, RR> finisher = downstream.finisher().andThen(thenFinisher);
        
        /*
         * 5.容器参数，指示容器的特征。
         *   使用downstream中的容器参数；如果容器参数中包含IDENTITY_FINISH，则需要移除IDENTITY_FINISH的限制。
         */
        Set<Characteristics> characteristics = downstream.characteristics();
        
        // 如果downstream包含了IDENTITY_FINISH参数，则移除它
        if(characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            if(characteristics.size() == 1) {
                characteristics = Collectors.CH_NOID;
            } else {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }
        
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
    }
    
    /*▲ 后处理 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 计数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 对一系列int元素求和，待求和元素需要先经过mapper的处理
    public static <T> Collector<T, ?, Integer> summingInt(ToIntFunction<? super T> mapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   使用int数组。
         */
        Supplier<int[]> supplier = () -> new int[1];
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素转换为int类型的值，然后累加起来。
         */
        BiConsumer<int[], T> accumulator = (array, e) -> { array[0] += mapper.applyAsInt(e); };
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的和值累加到前一个容器的和值上，并返回前一个容器。
         */
        BinaryOperator<int[]> combiner = (a, array2) -> {
            a[0] += array2[0];
            return a;
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   返回获取到的和。
         */
        Function<int[], Integer> finisher = array -> array[0];
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
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
    // 对一系列long元素求和，待求和元素需要先经过mapper的处理
    public static <T> Collector<T, ?, Long> summingLong(ToLongFunction<? super T> mapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   使用long数组。
         */
        Supplier<long[]> supplier = () -> new long[1];
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素转换为long类型的值，然后累加起来。
         */
        BiConsumer<long[], T> accumulator = (array, e) -> { array[0] += mapper.applyAsLong(e); };
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的和值累加到前一个容器的和值上，并返回前一个容器。
         */
        BinaryOperator<long[]> combiner = (a, b) -> {
            a[0] += b[0];
            return a;
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   返回获取到的和。
         */
        Function<long[], Long> finisher = array -> array[0];
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
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
    // 对一系列double元素求和，待求和元素需要先经过mapper的处理
    public static <T> Collector<T, ?, Double> summingDouble(ToDoubleFunction<? super T> mapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   使用double数组。
         */
        Supplier<double[]> supplier = () -> new double[3];
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素转换为double类型的值，然后累加起来。
         */
        BiConsumer<double[], T> accumulator = (array, e) -> {
            double val = mapper.applyAsDouble(e);
            sumWithCompensation(array, val);
            array[2] += val;
        };
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的和值累加到前一个容器的和值上，并返回前一个容器。
         */
        BinaryOperator<double[]> combiner = (a, b) -> {
            sumWithCompensation(a, b[0]);
            a[2] += b[2];
            return sumWithCompensation(a, b[1]);
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   返回获取到的和。
         */
        Function<double[], Double> finisher = array -> computeFinalSum(array);
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        /*
         * In the arrays allocated for the collect operation, index 0
         * holds the high-order bits of the running sum, index 1 holds
         * the low-order bits of the sum computed via compensated
         * summation, and index 2 holds the simple sum used to compute
         * the proper result if the stream contains infinite values of
         * the same sign.
         */
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
    }
    
    /*▲ 计数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 平均值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 求一系列int元素的平均值，待求均值的元素需要先经过mapper的处理
    public static <T> Collector<T, ?, Double> averagingInt(ToIntFunction<? super T> mapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   使用long数组。
         */
        Supplier<long[]> supplier = () -> new long[2];
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素转换为int类型的值，然后累加起来。
         *   array[0]中存储了和值，array[1]中存储了元素的数量以便后续求均值。
         */
        BiConsumer<long[], T> accumulator = (array, t) -> {
            array[0] += mapper.applyAsInt(t);
            array[1]++;
        };
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的和值与元素数量信息累加到前一个容器中，并返回前一个容器。
         */
        BinaryOperator<long[]> combiner = (a, b) -> {
            a[0] += b[0];
            a[1] += b[1];
            return a;
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   返回获取到的均值。
         */
        Function<long[], Double> finisher = a -> (a[1] == 0) ? 0.0d : (double) a[0] / a[1];
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
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
    // 求一系列long元素的平均值，待求均值的元素需要先经过mapper的处理
    public static <T> Collector<T, ?, Double> averagingLong(ToLongFunction<? super T> mapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   使用long数组。
         */
        Supplier<long[]> supplier = () -> new long[2];
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素转换为long类型的值，然后累加起来。
         *   array[0]中存储了和值，array[1]中存储了元素的数量以便后续求均值。
         */
        BiConsumer<long[], T> accumulator = (a, t) -> {
            a[0] += mapper.applyAsLong(t);
            a[1]++;
        };
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的和值与元素数量信息累加到前一个容器中，并返回前一个容器。
         */
        BinaryOperator<long[]> combiner = (a, b) -> {
            a[0] += b[0];
            a[1] += b[1];
            return a;
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   返回获取到的均值。
         */
        Function<long[], Double> finisher = a -> (a[1] == 0) ? 0.0d : (double) a[0] / a[1];
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
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
    // 求一系列double元素的平均值，待求均值的元素需要先经过mapper的处理
    public static <T> Collector<T, ?, Double> averagingDouble(ToDoubleFunction<? super T> mapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   使用double数组。
         */
        Supplier<double[]> supplier = () -> new double[4];
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的元素转换为double类型的值，然后累加起来。
         *   array[0]中存储了和值，array[1]中存储了元素的数量以便后续求均值。
         */
        BiConsumer<double[], T> accumulator = (a, t) -> {
            double val = mapper.applyAsDouble(t);
            sumWithCompensation(a, val);
            a[2]++;
            a[3] += val;
        };
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   将后一个容器中的和值与元素数量信息累加到前一个容器中，并返回前一个容器。
         */
        BinaryOperator<double[]> combiner = (a, b) -> {
            sumWithCompensation(a, b[0]);
            sumWithCompensation(a, b[1]);
            a[2] += b[2];
            a[3] += b[3];
            return a;
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   返回获取到的均值。
         */
        Function<double[], Double> finisher = a -> (a[2] == 0) ? 0.0d : (computeFinalSum(a) / a[2]);
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        /*
         * In the arrays allocated for the collect operation, index 0
         * holds the high-order bits of the running sum, index 1 holds
         * the low-order bits of the sum computed via compensated
         * summation, and index 2 holds the number of values seen.
         */
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
    }
    
    /*▲ 平均值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 最值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 求最小值，元素的比较规则由comparator给出
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
    // 求最大值，元素的比较规则由comparator给出
    public static <T> Collector<T, ?, Optional<T>> maxBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.maxBy(comparator));
    }
    
    /*▲ 最值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 归约 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 使用operator将遇到的所有元素归约成一个元素后返回
    public static <T> Collector<T, ?, Optional<T>> reducing(BinaryOperator<T> operator) {
        
        // 状态盒
        class OptionalBox implements Consumer<T> {
            T value = null;
            boolean present = false;
            
            @Override
            public void accept(T t) {
                // 处理后续元素
                if(present) {
                    value = operator.apply(value, t);
                    
                    // 保存首个元素
                } else {
                    value = t;
                    present = true;
                }
            }
        }
        
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   构造一个状态盒。
         */
        Supplier<OptionalBox> supplier = OptionalBox::new;
        
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   使用operator对上个元素和新元素进行归约，并且用归约后的结果更新上个元素。
         */
        BiConsumer<OptionalBox, T> accumulator = OptionalBox::accept;
        
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   对两个容器中的元素进行归约，并用归约结果更新第一个容器的后将其返回。
         */
        BinaryOperator<OptionalBox> combiner = (a, b) -> {
            if(b.present) {
                a.accept(b.value);
            }
            return a;
        };
        
        /*
         * 4.收尾操作（可选，最后执行）。
         *   返回对归约结果的包装值。
         */
        Function<OptionalBox, Optional<T>> finisher = a -> Optional.ofNullable(a.value);
        
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
        
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
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
    // 使用operator将遇到的所有元素归约成一个元素后返回，identity将作为首个待归约元素
    public static <T> Collector<T, ?, T> reducing(T identity, BinaryOperator<T> operator) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   构造一个引用类型的数组。
         */
        Supplier<T[]> supplier = boxSupplier(identity);
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   使用operator对上个元素和新元素进行归约，并且用归约后的结果更新上个元素。
         */
        BiConsumer<T[], T> accumulator = (a, t) -> { a[0] = operator.apply(a[0], t); };
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   对两个容器中的元素进行归约，并用归约结果更新第一个容器的后将其返回。
         */
        BinaryOperator<T[]> combiner = (a, b) -> {
            a[0] = operator.apply(a[0], b[0]);
            return a;
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   返回对归约结果。
         */
        Function<T[], T> finisher = a -> a[0];
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
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
    // 使用operator将遇到的所有元素归约成一个元素后返回，identity将作为首个待归约元素；除identity外，其他待归约元素要先经过mapper的处理后再参加归约
    public static <T, U> Collector<T, ?, U> reducing(U identity, Function<? super T, ? extends U> mapper, BinaryOperator<U> operator) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   构造一个引用类型的数组。
         */
        Supplier<U[]> supplier = boxSupplier(identity);
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   使用operator对上个元素和新元素进行归约，并且用归约后的结果更新上个元素。
         *   除identity外，其他待归约元素要先经过mapper的处理后再参加归约。
         */
        BiConsumer<U[], T> accumulator = (a, t) -> { a[0] = operator.apply(a[0], mapper.apply(t)); };
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   对两个容器中的元素进行归约，并用归约结果更新第一个容器的后将其返回。
         */
        BinaryOperator<U[]> combiner = (a, b) -> {
            a[0] = operator.apply(a[0], b[0]);
            return a;
        };
    
        /*
         * 4.收尾操作（可选，最后执行）。
         *   返回对归约结果。
         */
        Function<U[], U> finisher = a -> a[0];
    
        /*
         * 5.容器参数，指示容器的特征。
         *   未设置任何参数。
         */
        Set<Characteristics> characteristics = CH_NOID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
    }
    
    /*▲ 归约 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 分类 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    /*
     * 对上游的数据进行分类，分类后的数据存储一个收集器中并将其返回，该收集器的容器默认是HashMap。
     *
     * classifier: 对key进行分类，入参是非空的key，返回值是分类后的特征值。
     *
     * 注：
     * 1.上面提到的HashMap，其key是分类特征值，而value是ArrayList，存储着分类后的数据。
     * 2.分类后的数据就是原始的key，这里没有做进一步处理。
     */
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
    /*
     * 对上游的数据进行分类，分类后的数据存储一个收集器中并将其返回，该收集器的容器默认是HashMap。
     *
     * classifier: 对key进行分类，入参是非空的key，返回值是分类后的特征值(分类后的元素存放容器需要由downstream给出)。
     * downstream: 制定分类数据的操作流程以及提供存储分类后数据的容器。
     *
     * 注：
     * 1.上面提到的HashMap，其key是分类特征值，而value也是一个容器，存储着分类后的数据。
     * 2.分类后的数据是什么，由downstream的择取逻辑来决定，而且，downstream还提供了存储分类后的数据的容器。
     */
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
    /*
     * 对上游的数据进行分类，分类后的数据存储一个收集器中并将其返回，该收集器的容器是mapFactory，它应当被设置为Map类型。
     *
     * classifier: 对key进行分类，入参是非空的key，返回值是分类后的特征值。
     * mapFactory: 收集数据的容器工厂，其生产的容器必须为Map。
     * downstream: 制定分类数据的操作流程以及提供存储分类后数据的容器。
     *
     * 注：
     * 1.downstream生产的Map容器，其key是分类特征值，而value也是一个容器，存储着分类后的数据。
     * 2.分类后的数据是什么，由downstream的择取逻辑来决定，而且，downstream还提供了存储分类后的数据的容器。
     */
    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   容器工厂由入参给出，应该使用一个Map容器。
         */
        @SuppressWarnings("unchecked")
        Supplier<Map<K, A>> supplier = (Supplier<Map<K, A>>) mapFactory;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的key进行分类，并决定后续如何处理分类后的key和该key对应的值。
         */
        BiConsumer<Map<K, A>, T> accumulator = (map, k) -> {
            // 使用classifier对key进行分类；如果key为null，则抛异常
            K key = Objects.requireNonNull(classifier.apply(k), "element cannot be mapped to a null key");
        
            // 如果map中已经存在这个key了，则返回它的分类容器；否则，为其创建一个新的分类容器后返回(这个所谓的新容器获取自downstream中)
            A container = map.computeIfAbsent(key, thisKey -> downstream.supplier().get());
        
            // 对指定的key做择取操作，通常是将该key对应的value存储到分类容器中
            downstream.accumulator().accept(container, k);
        };
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   合并相同类别的元素到同一个容器。
         */
        BinaryOperator<Map<K, A>> combiner = Collectors.<K, A, Map<K, A>>mapMerger(downstream.combiner());
    
        // 如果downstream指示不需要执行收尾操作，则这里可以直接构造收集器了
        if(downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
        
            /*
             * 5.容器参数，指示容器的特征。
             *   无需收尾。
             */
            Set<Characteristics> characteristics = CH_ID;
        
            return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
        } else {
        
            /*
             * 4.收尾操作（可选，最后执行）。
             *   使用downstream中的收尾操作处理容器中的元素。
             */
            Function<Map<K, A>, M> finisher = intermediate -> {
                // 获取downstream中的收尾操作
                @SuppressWarnings("unchecked")
                Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
            
                // 使用downstream中的收尾操作处理容器中的元素
                intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
            
                @SuppressWarnings("unchecked")
                M castResult = (M) intermediate;
            
                // 返回处理结果
                return castResult;
            };
        
            /*
             * 5.容器参数，指示容器的特征。
             *   未设置任何参数。
             */
            Set<Characteristics> characteristics = CH_NOID;
        
            return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
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
    /*
     * 对上游的数据进行分类，分类后的数据存储一个收集器中并将其返回，该收集器的容器默认是ConcurrentHashMap。
     *
     * classifier: 对key进行分类，入参是非空的key，返回值是分类后的特征值。
     *
     * 注：
     * 1.上面提到的ConcurrentHashMap，其key是分类特征值，而value是ArrayList，存储着分类后的数据。
     * 2.分类后的数据就是原始的key，这里没有做进一步处理。
     * 3.这是groupingBy(Function)的同步容器版本。
     */
    public static <T, K> Collector<T, ?, ConcurrentMap<K, List<T>>> groupingByConcurrent(Function<? super T, ? extends K> classifier) {
        return groupingByConcurrent(classifier, toList());
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
    /*
     * 对上游的数据进行分类，分类后的数据存储一个收集器中并将其返回，该收集器的容器默认是ConcurrentHashMap。
     *
     * classifier: 对key进行分类，入参是非空的key，返回值是分类后的特征值(分类后的元素存放容器需要由downstream给出)。
     * downstream: 制定分类数据的操作流程以及提供存储分类后数据的容器。
     *
     * 注：
     * 1.上面提到的ConcurrentHashMap，其key是分类特征值，而value也是一个容器，存储着分类后的数据。
     * 2.分类后的数据是什么，由downstream的择取逻辑来决定，而且，downstream还提供了存储分类后的数据的容器。
     * 3.这是groupingBy(Function, Collector)的同步容器版本。
     */
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
    /*
     * 对上游的数据进行分类，分类后的数据存储一个收集器中并将其返回，该收集器的容器是mapFactory，它应当被设置为ConcurrentMap类型。
     *
     * classifier: 对key进行分类，入参是非空的key，返回值是分类后的特征值。
     * mapFactory: 收集数据的容器工厂，其生产的容器必须为Map。
     * downstream: 制定分类数据的操作流程以及提供存储分类后数据的容器。
     *
     * 注：
     * 1.downstream生产的Map容器，其key是分类特征值，而value也是一个容器，存储着分类后的数据。
     * 2.分类后的数据是什么，由downstream的择取逻辑来决定，而且，downstream还提供了存储分类后的数据的容器。
     * 3.这是groupingBy(Function, Supplier, Collector)的同步容器版本。
     */
    public static <T, K, A, D, M extends ConcurrentMap<K, D>> Collector<T, ?, M> groupingByConcurrent(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   容器工厂由入参给出，应该使用一个ConcurrentMap容器。
         */
        @SuppressWarnings("unchecked")
        Supplier<ConcurrentMap<K, A>> supplier = (Supplier<ConcurrentMap<K, A>>) mapFactory;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将遇到的key进行分类，并决定后续如何处理分类后的key和该key对应的值。
         */
        BiConsumer<ConcurrentMap<K, A>, T> accumulator;
    
        // downstream中给出的容器是支持并发的，则可以直接存储元素；否则，需要存储分类后的数据时，需要进行同步操作
        if(downstream.characteristics().contains(Collector.Characteristics.CONCURRENT)) {
            accumulator = (map, t) -> {
                // 使用classifier对key进行分类；如果key为null，则抛异常
                K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
                // 如果map中已经存在这个key了，则返回它的分类容器；否则，为其创建一个新的分类容器后返回(这个所谓的新容器获取自downstream中)
                A resultContainer = map.computeIfAbsent(key, k -> downstream.supplier().get());
                // 对指定的key做择取操作，通常是将该key对应的value存储到分类容器中
                downstream.accumulator().accept(resultContainer, t);
            };
        } else {
            accumulator = (map, t) -> {
                // 使用classifier对key进行分类；如果key为null，则抛异常
                K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
                // 如果map中已经存在这个key了，则返回它的分类容器；否则，为其创建一个新的分类容器后返回(这个所谓的新容器获取自downstream中)
                A resultContainer = map.computeIfAbsent(key, k -> downstream.supplier().get());
            
                // 进行同步操作
                synchronized(resultContainer) {
                    // 对指定的key做择取操作，通常是将该key对应的value存储到分类容器中
                    downstream.accumulator().accept(resultContainer, t);
                }
            };
        }
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   合并相同类别的元素到同一个容器。
         */
        BinaryOperator<ConcurrentMap<K, A>> combiner = Collectors.<K, A, ConcurrentMap<K, A>>mapMerger(downstream.combiner());
    
        // 如果downstream指示不需要执行收尾操作，则这里可以直接构造收集器了
        if(downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
        
            /*
             * 5.容器参数，指示容器的特征。
             *   并发容器/无序收集/无需收尾。
             */
            Set<Characteristics> characteristics = CH_CONCURRENT_ID;
        
            return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
        } else {
        
            /*
             * 4.收尾操作（可选，最后执行）。
             *   使用downstream中的收尾操作处理容器中的元素。
             */
            Function<ConcurrentMap<K, A>, M> finisher = intermediate -> {
            
                // 获取downstream中的收尾操作
                @SuppressWarnings("unchecked")
                Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
            
                // 使用downstream中的收尾操作处理容器中的元素
                intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
            
                @SuppressWarnings("unchecked")
                M castResult = (M) intermediate;
            
                // 返回处理结果
                return castResult;
            };
        
            /*
             * 5.容器参数，指示容器的特征。
             *   并发容器/无序收集。
             */
            Set<Characteristics> characteristics = CH_CONCURRENT_NOID;
        
            return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }
    }
    
    /*▲ 分类 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 二元分组 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 将上游元素根据给定的校验条件分组到两个ArrayList容器中
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
    // 将上游元素根据给定的校验条件分组到两个自定义的容器中
    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(Predicate<? super T> predicate, Collector<? super T, A, D> downstream) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   生产一个Partition容器，该容器内部包含两个由downstream提供的小容器，一个用来存储与predicate匹配的元素，另一个用来存储与predicate不匹配的元素。
         */
        Supplier<Partition<A>> supplier = () -> new Partition<>(downstream.supplier().get(), downstream.supplier().get());
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   先对上游元素进行predicate校验，如果返回true，则将该元素交给forTrue容器处理，否则，将其交给forFalse容器。
         *   具体说交给容器后怎么处理，由downstream给出的择取逻辑决定。
         */
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BiConsumer<Partition<A>, T> accumulator = (result, t) -> downstreamAccumulator.accept(predicate.test(t) ? result.forTrue : result.forFalse, t);
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   分别对两个Partition容器中的forTrue与forFalse子容器进行合并。
         */
        BinaryOperator<A> downstreamCombiner = downstream.combiner();
        BinaryOperator<Partition<A>> combiner = (left, right) -> new Partition<>(downstreamCombiner.apply(left.forTrue, right.forTrue), downstreamCombiner.apply(left.forFalse, right.forFalse));
    
        if(downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            /*
             * 5.容器参数，指示容器的特征。
             *   无需收尾。
             */
            Set<Characteristics> characteristics = CH_ID;
        
            return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
        } else {
            /*
             * 4.收尾操作（可选，最后执行）。
             *   由downstream给出的收尾操作决定如何进一步处理Partition容器中的元素。
             */
            Function<Partition<A>, Map<Boolean, D>> finisher = par -> new Partition<>(downstream.finisher().apply(par.forTrue), downstream.finisher().apply(par.forFalse));
        
            /*
             * 5.容器参数，指示容器的特征。
             *   未设置任何参数。
             */
            Set<Characteristics> characteristics = CH_NOID;
        
            return new CollectorImpl<>(supplier, accumulator, combiner, finisher, characteristics);
        }
    }
    
    /*▲ 二元分组 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Map化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 将每个上游元素解析成一个键值对，并将其存入HashMap；其中，keyMapper和valueMapper分别用来映射键和值，且要确保键不重复
    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   此处生产的HashMap用于存储择取后的元素。
         */
        Supplier<Map<K, U>> supplier = HashMap::new;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   从指定的元素中解析出键值对，然后存入map中(此过程需要确保key不重复，否则抛异常)
         */
        BiConsumer<Map<K, U>, T> accumulator = uniqKeysMapAccumulator(keyMapper, valueMapper);
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   合并给定的两个Map，需要确保key不重复，否则抛异常。
         */
        BinaryOperator<Map<K, U>> combiner = uniqKeysMapMerger();
    
        /*
         * 5.容器参数，指示容器的特征。
         *   无需收尾。
         */
        Set<Characteristics> characteristics = CH_ID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
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
    // 将每个上游元素解析成一个键值对，并将其存入HashMap；其中，keyMapper和valueMapper分别用来映射键和值，且如果key重复时，需要使用mergeFunction对旧值与备用值进行归并
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
    // 将每个上游元素解析成一个键值对，并将其存入mapFactory提供的Map容器中；其中，keyMapper和valueMapper分别用来映射键和值，且如果key重复时，需要使用mergeFunction对旧值与备用值进行归并
    public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction, Supplier<M> mapFactory) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   此处生产的Map用于存储择取后的元素。
         */
        Supplier<M> supplier = mapFactory;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   从指定的元素中解析出键值对，然后存入map中。
         *   如果该key已经重复了，则需要借助mergeFunction将旧值和入参中的备用值重新构造为一个新值后存入map。
         */
        BiConsumer<M, T> accumulator = (map, element) -> map.merge(keyMapper.apply(element), valueMapper.apply(element), mergeFunction);
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   合并两个Map；如果key出现重复，则对旧value和入参中的备用value进行归并，归并后的新的value存入map。
         */
        BinaryOperator<M> combiner = mapMerger(mergeFunction);
    
        /*
         * 5.容器参数，指示容器的特征。
         *   无需收尾。
         */
        Set<Characteristics> characteristics = CH_ID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
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
    // 将每个上游元素解析成一个键值对，并将其存入一个只读Map；其中，keyMapper和valueMapper分别用来映射键和值，且要确保键不重复
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper");
        Objects.requireNonNull(valueMapper, "valueMapper");
    
        // 从上游元素中解析出键值对，并将其存入HashMap；其中，keyMapper和valueMapper分别用来映射键和值，且要确保键不重复
        Collector<T, ?, Map<K, U>> downstream = toMap(keyMapper, valueMapper);
    
        // 将上述收集到的元素存入一个只读Map中后返回
        Function<Map<K, U>, Map<K, U>> thenFinisher = map -> (Map<K, U>) Map.ofEntries(map.entrySet().toArray(new Map.Entry[0]));
    
        // 对上游的数据进行后处理：对上游的元素先使用downstream中的收尾操作处理，再使用thenFinisher这个收尾操作处理；后处理的结果存储到downstream中
        return collectingAndThen(downstream, thenFinisher);
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
    // 将每个上游元素解析成一个键值对，并将其存入一个只读Map；其中，keyMapper和valueMapper分别用来映射键和值，且如果key重复时，需要使用mergeFunction对旧值与备用值进行归并
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        Objects.requireNonNull(keyMapper, "keyMapper");
        Objects.requireNonNull(valueMapper, "valueMapper");
        Objects.requireNonNull(mergeFunction, "mergeFunction");
    
        // 从上游元素中解析出键值对，并将其存入HashMap；其中，keyMapper和valueMapper分别用来映射键和值，且如果key重复时，需要使用mergeFunction对旧值与备用值进行归并
        Collector<T, ?, Map<K, U>> downstream = toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
    
        // 将上述收集到的元素存入一个只读Map中后返回
        Function<Map<K, U>, Map<K, U>> thenFinisher = map -> (Map<K, U>) Map.ofEntries(map.entrySet().toArray(new Map.Entry[0]));
    
        // 对上游的数据进行后处理：对上游的元素先使用downstream中的收尾操作处理，再使用thenFinisher这个收尾操作处理；后处理的结果存储到downstream中
        return collectingAndThen(downstream, thenFinisher);
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
    // 将每个上游元素解析成一个键值对，并将其存入ConcurrentHashMap；其中，keyMapper和valueMapper分别用来映射键和值，且要确保键不重复
    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   此处生产的ConcurrentHashMap用于存储择取后的元素。
         */
        Supplier<Map<K, U>> supplier = ConcurrentHashMap::new;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   从指定的元素中解析出键值对，然后存入map中(此过程需要确保key不重复，否则抛异常)
         */
        BiConsumer<Map<K, U>, T> accumulator = uniqKeysMapAccumulator(keyMapper, valueMapper);
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   合并给定的两个Map，需要确保key不重复，否则抛异常
         */
        BinaryOperator<Map<K, U>> combiner = uniqKeysMapMerger();
    
        /*
         * 5.容器参数，指示容器的特征。
         *   并发容器/无序收集/无需收尾。
         */
        Set<Characteristics> characteristics = CH_CONCURRENT_ID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
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
    // 将每个上游元素解析成一个键值对，并将其存入ConcurrentMap；其中，keyMapper和valueMapper分别用来映射键和值，且如果key重复时，需要使用mergeFunction对旧值与备用值进行归并
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
    // 将每个上游元素解析成一个键值对，并将其存入mapFactory提供的ConcurrentMap容器中；其中，keyMapper和valueMapper分别用来映射键和值，且如果key重复时，需要使用mergeFunction对旧值与备用值进行归并
    public static <T, K, U, M extends ConcurrentMap<K, U>> Collector<T, ?, M> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction, Supplier<M> mapFactory) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   此处生产的ConcurrentMap用于存储择取后的元素。
         */
        Supplier<M> supplier = mapFactory;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   从指定的元素中解析出键值对，然后存入map中。
         *   如果该key已经重复了，则需要借助mergeFunction将旧值和入参中的备用值重新构造为一个新值后存入map。
         */
        BiConsumer<M, T> accumulator = (map, element) -> map.merge(keyMapper.apply(element), valueMapper.apply(element), mergeFunction);
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   合并两个Map；如果key出现重复，则对旧value和入参中的备用value进行归并，归并后的新的value存入map。
         */
        BinaryOperator<M> combiner = mapMerger(mergeFunction);
    
        /*
         * 5.容器参数，指示容器的特征。
         *   并发容器/无序收集/无需收尾。
         */
        Set<Characteristics> characteristics = CH_CONCURRENT_ID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
    }
    
    /*▲ Map化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 信息统计 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 信息统计，即对int类型的元素统计如下相关信息：计数、求和、均值、最小值、最大值；被统计的元素需要先经过mapper的处理
    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(ToIntFunction<? super T> mapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   此处生产的IntSummaryStatistics容器存储统计信息。
         */
        Supplier<IntSummaryStatistics> supplier = IntSummaryStatistics::new;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将上游元素用mapper映射后，再将其记录到IntSummaryStatistics中。
         */
        BiConsumer<IntSummaryStatistics, T> accumulator = (r, t) -> r.accept(mapper.applyAsInt(t));
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   合并两个IntSummaryStatistics中的统计信息。
         */
        BinaryOperator<IntSummaryStatistics> combiner = (l, r) -> {
            l.combine(r);
            return l;
        };
    
        /*
         * 5.容器参数，指示容器的特征。
         *   无需收尾。
         */
        Set<Characteristics> characteristics = CH_ID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
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
    // 信息统计，即对long类型的元素统计如下相关信息：计数、求和、均值、最小值、最大值；被统计的元素需要先经过mapper的处理
    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(ToLongFunction<? super T> mapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   此处生产的LongSummaryStatistics容器存储统计信息。
         */
        Supplier<LongSummaryStatistics> supplier = LongSummaryStatistics::new;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将上游元素用mapper映射后，再将其记录到LongSummaryStatistics中。
         */
        BiConsumer<LongSummaryStatistics, T> accumulator = (r, t) -> r.accept(mapper.applyAsLong(t));
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   合并两个LongSummaryStatistics中的统计信息。
         */
        BinaryOperator<LongSummaryStatistics> combiner = (l, r) -> {
            l.combine(r);
            return l;
        };
    
        /*
         * 5.容器参数，指示容器的特征。
         *   无需收尾。
         */
        Set<Characteristics> characteristics = CH_ID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
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
    // 信息统计，即对double类型的元素统计如下相关信息：计数、求和、均值、最小值、最大值；被统计的元素需要先经过mapper的处理
    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(ToDoubleFunction<? super T> mapper) {
    
        /*
         * 1.容器工厂（该工厂用来构造收纳元素的容器）。
         *   此处生产的DoubleSummaryStatistics容器存储统计信息。
         */
        Supplier<DoubleSummaryStatistics> supplier = DoubleSummaryStatistics::new;
    
        /*
         * 2.择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）。
         *   将上游元素用mapper映射后，再将其记录到DoubleSummaryStatistics中。
         */
        BiConsumer<DoubleSummaryStatistics, T> accumulator = (r, t) -> r.accept(mapper.applyAsDouble(t));
    
        /*
         * 3.合并容器（这是合并操作，通常用于在并行流中合并子任务）。
         *   合并两个DoubleSummaryStatistics中的统计信息。
         */
        BinaryOperator<DoubleSummaryStatistics> combiner = (l, r) -> {
            l.combine(r);
            return l;
        };
    
        /*
         * 5.容器参数，指示容器的特征。
         *   无需收尾。
         */
        Set<Characteristics> characteristics = CH_ID;
    
        return new CollectorImpl<>(supplier, accumulator, combiner, characteristics);
    }
    
    /*▲ 信息统计 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
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
    // 合并给定的两个Map，需要确保key不重复，否则抛异常
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
    // 从指定的元素中解析出键值对，然后存入map中(此过程需要确保key不重复，否则抛异常)
    private static <T, K, V> BiConsumer<Map<K, V>, T> uniqKeysMapAccumulator(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return (map, element) -> {
            // 从给定的元素中解析出一个key
            K k = keyMapper.apply(element);
    
            // 从给定的元素中解析出一个value
            V v = Objects.requireNonNull(valueMapper.apply(element));
    
            // 将指定的键值对加入到map中，返回旧值
            V u = map.putIfAbsent(k, v);
    
            // 如果返回值不为null，说明之前就存在该key了，会抛异常
            if(u != null) {
                throw duplicateKeyException(k, u, v);
            }
        };
    }
    
    // 强制类型转换(标识转换)
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
    // 合并两个Map；如果key出现重复，则对旧value和入参中的备用value进行归并，归并后的新的value存入map
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
        private final Supplier<A> supplier;                 // 1.构造容器
        private final BiConsumer<A, T> accumulator;         // 2.择取元素
        private final BinaryOperator<A> combiner;           // 3.合并容器
        private final Function<A, R> finisher;              // 4.收尾操作
        private final Set<Characteristics> characteristics; // 5.容器参数
        
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
        
        // 1. 容器工厂（该工厂用来构造收纳元素的容器）
        @Override
        public Supplier<A> supplier() {
            return supplier;
        }
        
        // 2. 择取元素（这是(子)任务中的择取操作，通常用于将元素添加到目标容器）
        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }
        
        // 3. 合并容器（这是合并操作，通常用于在并行流中合并子任务）
        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }
        
        // 4. 收尾操作（可选，最后执行）
        @Override
        public Function<A, R> finisher() {
            return finisher;
        }
        
        // 5. 返回容器的参数，指示容器的特征
        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }
    
    /**
     * Implementation class used by partitioningBy.
     */
    // partitioningBy中使用的容器，里面包含两个小容器
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
