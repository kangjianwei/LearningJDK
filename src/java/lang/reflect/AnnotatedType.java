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

package java.lang.reflect;

/**
 * {@code AnnotatedType} represents the potentially annotated use of a type in
 * the program currently running in this VM. The use may be of any type in the
 * Java programming language, including an array type, a parameterized type, a
 * type variable, or a wildcard type.
 *
 * @since 1.8
 */
/*
 * 【被注解类型】，代表"类型注解+类型"这个整体，要求注解的应用范围至少是@Target(ElementType.TYPE_USE)
 *
 * 有5种实现类（Impl）：
 *                                      AnnotatedType
 *                                            |
 *                                  AnnotatedTypeBaseImpl
 *                                            |
 *   +---------------------------+-------------------------------+--------------------------+
 *   |                           |                               |                          |
 *   |  AnnotatedTypeVariable    | AnnotatedParameterizedType    | AnnotatedWildcardType    | AnnotatedArrayType
 *   |           |               |             |                 |           |              |        |
 *   |           |               |             |                 |           |              |        |
 * AnnotatedTypeVariableImpl   AnnotatedParameterizedTypeImpl  AnnotatedWildcardTypeImpl  AnnotatedArrayTypeImpl
 */
public interface AnnotatedType extends AnnotatedElement {
    
    /**
     * Returns the potentially annotated type that this type is a member of, if
     * this type represents a nested type. For example, if this type is
     * {@code @TA O<T>.I<S>}, return a representation of {@code @TA O<T>}.
     *
     * <p>Returns {@code null} if this {@code AnnotatedType} represents a
     * top-level type, or a local or anonymous class, or a primitive type, or
     * void.
     *
     * <p>Returns {@code null} if this {@code AnnotatedType} is an instance of
     * {@code AnnotatedArrayType}, {@code AnnotatedTypeVariable}, or
     * {@code AnnotatedWildcardType}.
     *
     * @return an {@code AnnotatedType} object representing the potentially
     * annotated type that this type is a member of, or {@code null}
     *
     * @throws TypeNotPresentException             if the owner type
     *                                             refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if the owner type
     *                                             refers to a parameterized type that cannot be instantiated
     *                                             for any reason
     * @implSpec This default implementation returns {@code null} and performs no other
     * action.
     * @since 9
     */
    /*
     * 返回复合类型（外部类与内部类组成）的"外层类型+类型注解"
     * 只有在AnnotatedType的AnnotatedTypeBaseImpl实现和AnnotatedParameterizedTypeImpl实现种该方法才有可能返回非null值
     * 其它实现中一律返回null
     */
    default AnnotatedType getAnnotatedOwnerType() {
        return null;
    }
    
    /**
     * Returns the underlying type that this annotated type represents.
     *
     * @return the type this annotated type represents
     */
    // 返回【被注解类型】中的类型
    public Type getType();
}
