/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

/**
 * Represents a class or interface program element.
 * Provides access to information about the type and its members.
 * Note that an enum type is a kind of class and an annotation type is a kind of interface.
 *
 * <p> While a {@code TypeElement} represents a class or interface
 * <i>element</i>, a {@link DeclaredType} represents a class
 * or interface <i>type</i>, the latter being a use
 * (or <i>invocation</i>) of the former.
 * The distinction is most apparent with generic types,
 * for which a single element can define a whole
 * family of types.  For example, the element
 * {@code java.util.Set} corresponds to the parameterized types
 * {@code java.util.Set<String>} and {@code java.util.Set<Number>}
 * (and many others), and to the raw type {@code java.util.Set}.
 *
 * <p> Each method of this interface that returns a list of elements
 * will return them in the order that is natural for the underlying
 * source of program information.  For example, if the underlying
 * source of information is Java source code, then the elements will be
 * returned in source code order.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see DeclaredType
 * @since 1.6
 */
// 类型元素，包括类、接口、枚举、注解
public interface TypeElement extends Element, Parameterizable, QualifiedNameable {
    
    /**
     * Returns the fully qualified name of this type element.
     * More precisely, it returns the <i>canonical</i> name.
     * For local and anonymous classes, which do not have canonical names,
     * an empty name is returned.
     *
     * <p>The name of a generic type does not include any reference
     * to its formal type parameters.
     * For example, the fully qualified name of the interface
     * {@code java.util.Set<E>} is "{@code java.util.Set}".
     * Nested types use "{@code .}" as a separator, as in
     * "{@code java.util.Map.Entry}".
     *
     * @return the fully qualified name of this class or interface, or
     * an empty name if none
     *
     * @see Elements#getBinaryName
     * @jls 6.7 Fully Qualified Names and Canonical Names
     */
    /*
     * 返回该类型的全限定名.更确切地说，它返回规范名称。对于没有标准名称的局部和匿名类，返回空名称
     *
     * 泛型类型的名称不包括对其正式类型参数的任何引用
     * 例如，接口java.util.Set<E>的完全限定名是java.util.Set
     * 嵌套类型使用"."作为分隔符，如java.util.Map.Entry
     */
    Name getQualifiedName();
    
    /**
     * Returns the simple name of this type element.
     *
     * For an anonymous class, an empty name is returned.
     *
     * @return the simple name of this class or interface,
     * an empty name for an anonymous class
     *
     */
    // 返回该类型的简单名称，对于匿名类，返回空Name
    @Override
    Name getSimpleName();
    
    /**
     * Returns the direct superclass of this type element.
     * If this type element represents an interface or the class
     * {@code java.lang.Object}, then a {@link NoType}
     * with kind {@link TypeKind#NONE NONE} is returned.
     *
     * @return the direct superclass, or a {@code NoType} if there is none
     */
    /*
     * 返回该元素直接父类的类型
     * 如果该类是一个接口或者该类是Object，则返回由NoType和TypeKind#NONE组成的TypeMirror
     */
    TypeMirror getSuperclass();
    
    /**
     * Returns the interface types directly implemented by this class
     * or extended by this interface.
     *
     * @return the interface types directly implemented by this class
     * or extended by this interface, or an empty list if there are none
     */
    /*
     * 如果当前元素是类，则返回实现的接口
     * 如果当前元素是接口，则返回继承的接口
     */
    List<? extends TypeMirror> getInterfaces();
    
    /**
     * Returns the formal type parameters of this type element
     * in declaration order.
     *
     * @return the formal type parameters, or an empty list
     * if there are none
     */
    // 按照声明顺序返回该类型的泛型类型参数
    List<? extends TypeParameterElement> getTypeParameters();
    
    /**
     * Returns the <i>nesting kind</i> of this type element.
     *
     * @return the nesting kind of this type element
     */
    // 获取元素的嵌套结构类别
    NestingKind getNestingKind();
    
    /**
     * Returns the fields, methods, constructors, and member types
     * that are directly declared in this class or interface.
     *
     * This includes any {@linkplain Elements.Origin#MANDATED
     * mandated} elements such as the (implicit) default constructor
     * and the implicit {@code values} and {@code valueOf} methods of
     * an enum type.
     *
     * @apiNote As a particular instance of the {@linkplain
     * javax.lang.model.element general accuracy requirements} and the
     * ordering behavior required of this interface, the list of
     * enclosed elements will be returned in the natural order for the
     * originating source of information about the type.  For example,
     * if the information about the type is originating from a source
     * file, the elements will be returned in source code order.
     * (However, in that case the the ordering of {@linkplain
     * Elements.Origin#MANDATED implicitly declared} elements, such as
     * default constructors, is not specified.)
     *
     * @return the enclosed elements in proper order, or an empty list if none
     *
     * @jls 8.8.9 Default Constructor
     * @jls 8.9.3 Enum Members
     */
    /*
     * 返回在该类或者接口中直接声明的字段，方法，构造器和成员类型
     * 包括(隐式的)默认构造器和在枚举类型中隐式的values和valueOf方法
     */
    @Override
    List<? extends Element> getEnclosedElements();
    
    /**
     * Returns the package of a top-level type and returns the
     * immediately lexically enclosing element for a {@linkplain
     * NestingKind#isNested nested} type.
     *
     * @return the package of a top-level type, the immediately
     * lexically enclosing element for a nested type
     */
    // 返回包围该元素的最内层的元素
    @Override
    Element getEnclosingElement();
}
