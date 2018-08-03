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
package java.util;

import java.io.Serializable;

/**
 * Package private supporting class for {@link Comparator}.
 */
// 工具类。定义了自然顺序比较器与null优先比较器，以补充Comparator的操作。
class Comparators {
    private Comparators() {
        throw new AssertionError("no instances");
    }
    
    /**
     * Compares {@link Comparable} objects in natural order.
     *
     * @see Comparable
     */
    // 自然顺序比较器，用于比较实现了Comparable的对象
    enum NaturalOrderComparator implements Comparator<Comparable<Object>> {
        INSTANCE;
        
        @Override
        public int compare(Comparable<Object> c1, Comparable<Object> c2) {
            return c1.compareTo(c2);
        }
        
        @Override
        public Comparator<Comparable<Object>> reversed() {
            return Comparator.reverseOrder();
        }
    }
    
    /**
     * Null-friendly comparators
     */
    // nullFirst比较器
    static final class NullComparator<T> implements Comparator<T>, Serializable {
        private static final long serialVersionUID = -7569533591570686392L;
        /**
         * nullFirst==true ：null被认为是序列中第一个元素
         * nullFirst==false：null被认为是序列中最后一个元素
         */
        private final boolean nullFirst;
        // if null, non-null Ts are considered equal
        private final Comparator<T> real;   // 预置比较器
        
        @SuppressWarnings("unchecked")
        NullComparator(boolean nullFirst, Comparator<? super T> real) {
            this.nullFirst = nullFirst;
            this.real = (Comparator<T>) real;
        }
        
        /*
         * 如果待比较元素非空，且预置的Comparator也为空，则这两个元素被认为是相同的
         *
         * nullFirst==true：
         * null<--> obj  ===> -1
         * obj <--> null ===>  1
         *
         * nullFirst==false：
         * null<--> obj  ===>  1
         * obj <--> null ===> -1
         *
         * a和b都为null时，返回0。
         * a和b都不为null时：
         *   1.比较器real==null，返回0。
         *   2.比较器real!=null，使用real比较两个元素。
         */
        @Override
        public int compare(T a, T b) {
            if(a == null) {
                return (b == null) ? 0 : (nullFirst ? -1 : 1);
            } else if(b == null) {
                return nullFirst ? 1 : -1;
            } else {
                return (real == null) ? 0 : real.compare(a, b);
            }
        }
        
        // 返回null优先的比较器，且带有备用比较器other
        @Override
        public Comparator<T> thenComparing(Comparator<? super T> other) {
            Objects.requireNonNull(other);
            return new NullComparator<>(
                nullFirst,      // null优先
                real == null
                    ? other                     // 如果预置的比较器real为null，则直接启用备用比较器other
                    : real.thenComparing(other) // 如果预置的比较器real不为null，则同时使用预置比较器和备用比较器
            );
        }
        
        // 返回null不优先的与预置比较器real顺序相反的比较器，
        @Override
        public Comparator<T> reversed() {
            return new NullComparator<>(
                !nullFirst,     // null不优先
                real == null
                    ? null
                    : real.reversed()
            );
        }
    }
}
