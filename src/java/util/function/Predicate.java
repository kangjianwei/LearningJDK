/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.function;

import java.util.Objects;

/**
 * Represents a predicate (boolean-valued function) of one argument.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #test(Object)}.
 *
 * @param <T> the type of the input to the predicate
 *
 * @since 1.8
 */
/*
 * 函数式接口：Predicate<T>
 *
 * 参数：T
 * 返回：boolean
 * 示例：x是否为偶数
 *       Predicate<Integer> f = x -> x%2==0;
 */
@FunctionalInterface
public interface Predicate<T> {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param t the input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(T t);

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this
     *              predicate
     * @return a composed predicate that represents the short-circuiting logical
     * AND of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    /*
     * f1.and(f2)，返回一个“逻辑与”表达式，执行f1&&f2的判断
     *
     * Predicate<Integer> p1 = x->x>10;
     * Predicate<Integer> p2 = x->x%2==1;
     * Predicate<Integer> p = p1.and(p2);
     * // 判断15是否是大于10的奇数
     * System.out.println(p.test(15));
     */
    default java.util.function.Predicate<T> and(java.util.function.Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }
    
    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ORed with this
     *              predicate
     * @return a composed predicate that represents the short-circuiting logical
     * OR of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    /*
     * f1.or(f2)，返回一个“逻辑或”表达式，执行f1||f2的判断
     *
     * Predicate<Integer> p1 = x->x>5;
     * Predicate<Integer> p2 = x->x%2==1;
     * Predicate<Integer> p = p1.or(p2);
     * // 判断3是否是大于5或者是一个奇数
     * System.out.println(p.test(3));
     */
    default java.util.function.Predicate<T> or(java.util.function.Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    /**
     * Returns a predicate that represents the logical negation of this
     * predicate.
     *
     * @return a predicate that represents the logical negation of this
     * predicate
     */
    /*
     * f.negate()，返回一个“逻辑非”表达式，执行!f的判断
     *
     * Predicate<Integer> p = x->x%2==1;
     * // 判断2是不是非奇数，即偶数
     * System.out.println(p.negate().test(2));
     */
    default java.util.function.Predicate<T> negate() {
        return (t) -> !test(t);
    }

    /**
     * Returns a predicate that tests if two arguments are equal according
     * to {@link Objects#equals(Object, Object)}.
     *
     * @param <T> the type of arguments to the predicate
     * @param targetRef the object reference with which to compare for equality,
     *               which may be {@code null}
     * @return a predicate that tests if two arguments are equal according
     * to {@link Objects#equals(Object, Object)}
     */
    /*
     * Predicate.isEqual(obj)，返回一个“判等”表达式
     * // 判断两个字符串是否相等
     * System.out.println(Predicate.isEqual("abc").test("abc"));
     */
    static <T> java.util.function.Predicate<T> isEqual(Object targetRef) {
        return (null == targetRef)
                ? Objects::isNull
                : object -> targetRef.equals(object);
    }

    /**
     * Returns a predicate that is the negation of the supplied predicate.
     * This is accomplished by returning result of the calling
     * {@code target.negate()}.
     *
     * @param <T>     the type of arguments to the specified predicate
     * @param target  predicate to negate
     *
     * @return a predicate that negates the results of the supplied
     *         predicate
     *
     * @throws NullPointerException if target is null
     *
     * @since 11
     */
    /*
     * Predicate.not()，返回一个“逻辑非”表达式，与negate()方法几乎一样
     * // 判断3是不是“不是”偶数
     * System.out.println(Predicate.<Integer>not(x->x%2==0).test(3));
     */
    @SuppressWarnings("unchecked")
    static <T> java.util.function.Predicate<T> not(java.util.function.Predicate<? super T> target) {
        Objects.requireNonNull(target);
        return (java.util.function.Predicate<T>)target.negate();
    }
}
