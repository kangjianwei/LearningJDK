/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * The {@code kind} of an element.
 *
 * <p>Note that it is possible additional element kinds will be added
 * to accommodate new, currently unknown, language structures added to
 * future versions of the Java&trade; programming language.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see Element
 * @since 1.6
 */
// 元素的种类标记
public enum ElementKind {
    
    /**
     * A module.
     *
     * @spec JPMS
     * @since 9
     */
    MODULE,             // 模块
    
    /** A package. */
    PACKAGE,            // 包
    
    
    /* Declared types */
    
    /** An enum type. */
    ENUM,               // 枚举
    
    /** A class not described by a more specific kind (like {@code ENUM}). */
    CLASS,              // 类
    
    /** An annotation type. */
    ANNOTATION_TYPE,    // 注解
    
    /**
     * An interface not described by a more specific kind (like
     * {@code ANNOTATION_TYPE}).
     */
    INTERFACE,          // 接口
    
    
    /* Variables */
    
    /** An enum constant. */
    ENUM_CONSTANT,      // 枚举常量
    
    /**
     * A field not described by a more specific kind (like
     * {@code ENUM_CONSTANT}).
     */
    FIELD,              // 字段
    
    /** A parameter of a method or constructor. */
    PARAMETER,          // 方法或者构造器中的形参
    
    /** A local variable. */
    LOCAL_VARIABLE,     // 局部变量
    
    /** A parameter of an exception handler. */
    EXCEPTION_PARAMETER,    // 异常参数
    
    
    /* Executables */
    
    /** A method. */
    METHOD,         // 方法
    
    /** A constructor. */
    CONSTRUCTOR,    // 构造器
    
    /** A static initializer. */
    STATIC_INIT,    // 静态初始块
    
    /** An instance initializer. */
    INSTANCE_INIT,  // 初始块
    
    
    /** A type parameter. */
    TYPE_PARAMETER,     // 类型参数
    
    /**
     * A resource variable.
     *
     * @since 1.7
     */
    RESOURCE_VARIABLE,  // 资源变量（try-with-resources结构中的参数）
    
    
    /**
     * An implementation-reserved element.
     * This is not the element you are looking for.
     */
    OTHER;
    
    
    /**
     * Returns {@code true} if this is a kind of class:
     * either {@code CLASS} or {@code ENUM}.
     *
     * @return {@code true} if this is a kind of class
     */
    public boolean isClass() {
        return this == CLASS || this == ENUM;
    }
    
    /**
     * Returns {@code true} if this is a kind of interface:
     * either {@code INTERFACE} or {@code ANNOTATION_TYPE}.
     *
     * @return {@code true} if this is a kind of interface
     */
    public boolean isInterface() {
        return this == INTERFACE || this == ANNOTATION_TYPE;
    }
    
    /**
     * Returns {@code true} if this is a kind of field:
     * either {@code FIELD} or {@code ENUM_CONSTANT}.
     *
     * @return {@code true} if this is a kind of field
     */
    public boolean isField() {
        return this == FIELD || this == ENUM_CONSTANT;
    }}
