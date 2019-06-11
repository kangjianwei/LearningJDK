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

import java.util.List;
import javax.lang.model.type.*;

/**
 * Represents a method, constructor, or initializer (static or
 * instance) of a class or interface, including annotation type
 * elements.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see ExecutableType
 * @since 1.6
 */
// 可执行元素，如方法/构造器/初始化块
public interface ExecutableElement extends Element, Parameterizable {
    /**
     * Returns the formal type parameters of this executable
     * in declaration order.
     *
     * @return the formal type parameters, or an empty list
     * if there are none
     */
    // 获取类型参数
    List<? extends TypeParameterElement> getTypeParameters();
    
    /**
     * Returns the return type of this executable.
     * Returns a {@link NoType} with kind {@link TypeKind#VOID VOID}
     * if this executable is not a method, or is a method that does not
     * return a value.
     *
     * @return the return type of this executable
     */
    // 获取返回类型
    TypeMirror getReturnType();
    
    /**
     * Returns the formal parameters of this executable.
     * They are returned in declaration order.
     *
     * @return the formal parameters,
     * or an empty list if there are none
     */
    // 获取形参
    List<? extends VariableElement> getParameters();
    
    /**
     * Returns the receiver type of this executable,
     * or {@link javax.lang.model.type.NoType NoType} with
     * kind {@link javax.lang.model.type.TypeKind#NONE NONE}
     * if the executable has no receiver type.
     *
     * An executable which is an instance method, or a constructor of an
     * inner class, has a receiver type derived from the {@linkplain
     * #getEnclosingElement declaring type}.
     *
     * An executable which is a static method, or a constructor of a
     * non-inner class, or an initializer (static or instance), has no
     * receiver type.
     *
     * @return the receiver type of this executable
     * @since 1.8
     */
    /*
     * 获取接收器类型
     *
     * 如果没有对应的接收器,则返回由NoType和TypeKind#NONE组成的TypeMirror
     * 如果当前是一个实例方法、构造器或者是一个内部类，则其接收器类型可以由Element.getEnclosingElement()方法获得
     * 如果当前是一个静态方法，或者是非内部类的构造器，或者是初始块(静态的或者是实例的)，则没有接收器类型
     */
    TypeMirror getReceiverType();
    
    /**
     * Returns {@code true} if this method or constructor accepts a variable
     * number of arguments and returns {@code false} otherwise.
     *
     * @return {@code true} if this method or constructor accepts a variable
     * number of arguments and {@code false} otherwise
     */
    // 是否为可变形参
    boolean isVarArgs();
    
    /**
     * Returns {@code true} if this method is a default method and
     * returns {@code false} otherwise.
     *
     * @return {@code true} if this method is a default method and
     * {@code false} otherwise
     *
     * @since 1.8
     */
    // 是否为（接口中的）默认方法
    boolean isDefault();
    
    /**
     * Returns the exceptions and other throwables listed in this
     * method or constructor's {@code throws} clause in declaration
     * order.
     *
     * @return the exceptions and other throwables listed in the
     * {@code throws} clause, or an empty list if there are none
     */
    // 获取抛出的异常列表
    List<? extends TypeMirror> getThrownTypes();
    
    /**
     * Returns the default value if this executable is an annotation
     * type element.  Returns {@code null} if this method is not an
     * annotation type element, or if it is an annotation type element
     * with no default value.
     *
     * @return the default value, or {@code null} if none
     */
    // 获取默认值（注解方法上的默认值）
    AnnotationValue getDefaultValue();
    
    /**
     * Returns the simple name of a constructor, method, or
     * initializer.  For a constructor, the name {@code "<init>"} is
     * returned, for a static initializer, the name {@code "<clinit>"}
     * is returned, and for an anonymous class or instance
     * initializer, an empty name is returned.
     *
     * @return the simple name of a constructor, method, or
     * initializer
     */
    /*
     * 获取元素的简单名称
     *
     * 对于构造器，方法或者初始化块，返回其相应的Name
     * 对于构造器,返回<init>,对于静态代码块,返回<clinit>，对于匿名类或者实例代码块,返回空的Name
     */
    @Override
    Name getSimpleName();
}
