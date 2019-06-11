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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Represents a program element such as a module, package, class, or method.
 * Each element represents a static, language-level construct
 * (and not, for example, a runtime construct of the virtual machine).
 *
 * <p> Elements should be compared using the {@link #equals(Object)}
 * method.  There is no guarantee that any particular element will
 * always be represented by the same object.
 *
 * <p> To implement operations based on the class of an {@code
 * Element} object, either use a {@linkplain ElementVisitor visitor} or
 * use the result of the {@link #getKind} method.  Using {@code
 * instanceof} is <em>not</em> necessarily a reliable idiom for
 * determining the effective class of an object in this modeling
 * hierarchy since an implementation may choose to have a single object
 * implement multiple {@code Element} subinterfaces.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see Elements
 * @see TypeMirror
 * @since 1.6
 */

/*
 * 源码中的元素（结构），是一种静态结构，区别于运行时结构
 * 实现基于元素对象的操作时，应当配合使用ElementVisitor和getKind()
 */
public interface Element extends AnnotatedConstruct {
    
    /**
     * Returns the simple (unqualified) name of this element.  The
     * name of a generic type does not include any reference to its
     * formal type parameters.
     *
     * For example, the simple name of the type element {@code
     * java.util.Set<E>} is {@code "Set"}.
     *
     * If this element represents an unnamed {@linkplain
     * PackageElement#getSimpleName package} or unnamed {@linkplain
     * ModuleElement#getSimpleName module}, an empty name is returned.
     *
     * If it represents a {@linkplain ExecutableElement#getSimpleName
     * constructor}, the name "{@code <init>}" is returned.  If it
     * represents a {@linkplain ExecutableElement#getSimpleName static
     * initializer}, the name "{@code <clinit>}" is returned.
     *
     * If it represents an {@linkplain TypeElement#getSimpleName
     * anonymous class} or {@linkplain ExecutableElement#getSimpleName
     * instance initializer}, an empty name is returned.
     *
     * @return the simple name of this element
     * @see PackageElement#getSimpleName
     * @see ExecutableElement#getSimpleName
     * @see TypeElement#getSimpleName
     * @see VariableElement#getSimpleName
     * @see ModuleElement#getSimpleName
     * @revised 9
     * @spec JPMS
     */
    /*
     * 获取元素的简单名称（不含包名）
     *
     * 泛型会进行类型擦除
     * 未命名的包返回空的Name
     * 构造器返回<init>
     * 静态代码块返回<clinit>
     * 匿名类或普通初始化块，返回空的Name
     */
    Name getSimpleName();
    
    /**
     * Returns the {@code kind} of this element.
     *
     * @return the kind of this element
     */
    // 获取元素的种类标记
    ElementKind getKind();
    
    /**
     * Returns the type defined by this element.
     *
     * <p> A generic element defines a family of types, not just one.
     * If this is a generic element, a <i>prototypical</i> type is
     * returned.  This is the element's invocation on the
     * type variables corresponding to its own formal type parameters.
     * For example,
     * for the generic class element {@code C<N extends Number>},
     * the parameterized type {@code C<N>} is returned.
     * The {@link Types} utility interface has more general methods
     * for obtaining the full range of types defined by an element.
     *
     * @see Types
     *
     * @return the type defined by this element
     */
    // 获取元素类型
    TypeMirror asType();
    
    /**
     * Returns the modifiers of this element, excluding annotations.
     * Implicit modifiers, such as the {@code public} and {@code static}
     * modifiers of interface members, are included.
     *
     * @return the modifiers of this element, or an empty set if there are none
     */
    // 返回该元素的修饰符，隐式修饰符也包含,比如接口方法中的public和static
    Set<Modifier> getModifiers();
    
    /**
     * Returns the innermost element within which this element is, loosely speaking, enclosed.
     * <ul>
     * <li> If this element is one whose declaration is lexically enclosed
     * immediately within the declaration of another element, that other
     * element is returned.
     *
     * <li> If this is a {@linkplain TypeElement#getEnclosingElement
     * top-level type}, its package is returned.
     *
     * <li> If this is a {@linkplain
     * PackageElement#getEnclosingElement package}, its module is
     * returned if such a module exists. Otherwise, {@code null} is returned.
     *
     * <li> If this is a {@linkplain
     * TypeParameterElement#getEnclosingElement type parameter},
     * {@linkplain TypeParameterElement#getGenericElement the
     * generic element} of the type parameter is returned.
     *
     * <li> If this is a {@linkplain
     * VariableElement#getEnclosingElement method or constructor
     * parameter}, {@linkplain ExecutableElement the executable
     * element} which declares the parameter is returned.
     *
     * <li> If this is a {@linkplain ModuleElement#getEnclosingElement
     * module}, {@code null} is returned.
     *
     * </ul>
     *
     * @return the enclosing element, or {@code null} if there is none
     * @see Elements#getPackageOf
     * @revised 9
     * @spec JPMS
     */
    /*
     * 返回包围该元素（模块/包/类/接口/类型参数/变量）的最内层的元素
     *
     * 包含包的元素是模块
     * 包含类的元素是包
     * 包含内部类的元素是外部类
     * 包含方法、字段的元素是类
     * 包含形参的元素是方法、构造方法
     */
    Element getEnclosingElement();
    
    /**
     * Returns the elements that are, loosely speaking, directly
     * enclosed by this element.
     *
     * A {@linkplain TypeElement#getEnclosedElements class or
     * interface} is considered to enclose the fields, methods,
     * constructors, and member types that it directly declares.
     *
     * A {@linkplain PackageElement#getEnclosedElements package}
     * encloses the top-level classes and interfaces within it, but is
     * not considered to enclose subpackages.
     *
     * A {@linkplain ModuleElement#getEnclosedElements module}
     * encloses packages within it.
     *
     * Enclosed elements may include implicitly declared {@linkplain
     * Elements.Origin#MANDATED mandated} elements.
     *
     * Other kinds of elements are not currently considered to enclose any elements;
     * however, that may change as this API or the programming language evolves.
     *
     * @apiNote Elements of certain kinds can be isolated using
     * methods in {@link ElementFilter}.
     *
     * @return the enclosed elements, or an empty list if none
     * @see TypeElement#getEnclosedElements
     * @see PackageElement#getEnclosedElements
     * @see ModuleElement#getEnclosedElements
     * @see Elements#getAllMembers
     * @jls 8.8.9 Default Constructor
     * @jls 8.9 Enums
     * @revised 9
     * @spec JPMS
     */
    // 返回该元素（类/接口/包/模块）直接包含的元素
    List<? extends Element> getEnclosedElements();
    
    /**
     * {@inheritDoc}
     *
     * <p> To get inherited annotations as well, use {@link
     * Elements#getAllAnnotationMirrors(Element)
     * getAllAnnotationMirrors}.
     *
     * @since 1.6
     */
    // 获取直接声明在该元素上的注解镜像，不包括继承的注解
    @Override
    List<? extends AnnotationMirror> getAnnotationMirrors();
    
    /**
     * {@inheritDoc}
     * @since 1.6
     */
    // 返回指定类型的注解
    @Override
    <A extends Annotation> A getAnnotation(Class<A> annotationType);
    
    /**
     * Applies a visitor to this element.
     *
     * @param <R> the return type of the visitor's methods
     * @param <P> the type of the additional parameter to the visitor's methods
     * @param v   the visitor operating on this element
     * @param p   additional parameter to the visitor
     * @return a visitor-specified result
     */
    // 使用元素访问器访问元素
    <R, P> R accept(ElementVisitor<R, P> v, P p);
    
    /**
     * Returns {@code true} if the argument represents the same
     * element as {@code this}, or {@code false} otherwise.
     *
     * @apiNote The identity of an element involves implicit state
     * not directly accessible from the element's methods, including
     * state about the presence of unrelated types.  Element objects
     * created by different implementations of these interfaces should
     * <i>not</i> be expected to be equal even if &quot;the same&quot;
     * element is being modeled; this is analogous to the inequality
     * of {@code Class} objects for the same class file loaded through
     * different class loaders.
     *
     * @param obj  the object to be compared with this element
     * @return {@code true} if the specified object represents the same
     *          element as this
     */
    @Override
    boolean equals(Object obj);
    
    /**
     * Obeys the general contract of {@link Object#hashCode Object.hashCode}.
     *
     * @see #equals
     */
    @Override
    int hashCode();
}
