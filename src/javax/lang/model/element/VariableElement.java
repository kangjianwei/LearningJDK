/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.lang.model.element;

import javax.lang.model.util.Elements;

/**
 * Represents a field, {@code enum} constant, method or constructor
 * parameter, local variable, resource variable, or exception
 * parameter.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
// 变量元素，包括字段、枚举常量、方法或构造器的形参、局部变量、资源变量以及异常参数
public interface VariableElement extends Element {
    
    /**
     * Returns the value of this variable if this is a {@code final}
     * field initialized to a compile-time constant.  Returns {@code
     * null} otherwise.  The value will be of a primitive type or a
     * {@code String}.  If the value is of a primitive type, it is
     * wrapped in the appropriate wrapper class (such as {@link
     * Integer}).
     *
     * <p>Note that not all {@code final} fields will have
     * constant values.  In particular, {@code enum} constants are
     * <em>not</em> considered to be compile-time constants.  To have a
     * constant value, a field's type must be either a primitive type
     * or {@code String}.
     *
     * @return the value of this variable if this is a {@code final}
     * field initialized to a compile-time constant, or {@code null}
     * otherwise
     *
     * @see Elements#getConstantExpression(Object)
     * @jls 15.28 Constant Expression
     * @jls 4.12.4 final Variables
     */
    /*
     * 返回运行时常量元素的值（即final字段的值），对于其他类型的变量，返回Null
     * 返回值只能是原始类型（包装类型）或String
     * 注：枚举常量不是运行时常量，所以在这里返回null
     */
    Object getConstantValue();
    
    /**
     * Returns the simple name of this variable element.
     *
     * For method and constructor parameters, the name of each
     * parameter must be distinct from the names of all other
     * parameters of the same executable.  If the original source
     * names are not available, an implementation may synthesize names
     * subject to the distinctness requirement above.
     *
     * @return the simple name of this variable element
     */
    /*
     * 返回该元素的简单名称
     *
     * 对于在同一方法或者同一构造器的参数来说，其名称应该与其他的参数是不同的
     * 如果变量名称在源文件中不可视，则实现可以合成符合上述不同性要求的名称
     */
    @Override
    Name getSimpleName();
    
    /**
     * Returns the enclosing element of this variable.
     *
     * The enclosing element of a method or constructor parameter is
     * the executable declaring the parameter.
     *
     * @return the enclosing element of this variable
     */
    /*
     * 返回包围该元素的最内层的元素
     *
     * 对于方法或者构造器的参数来说，包围它的元素是其所对应的可执行元素
     */
    @Override
    Element getEnclosingElement();
}
