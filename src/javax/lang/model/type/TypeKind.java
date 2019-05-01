/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javax.lang.model.type;

/**
 * The kind of a type mirror.
 *
 * <p>Note that it is possible additional type kinds will be added to
 * accommodate new, currently unknown, language structures added to
 * future versions of the Java&trade; programming language.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see TypeMirror
 * @since 1.6
 */
// 元素的类型标记
public enum TypeKind {
    /**
     * The primitive type {@code boolean}.
     */
    BOOLEAN,    // boolean基本类型
    
    /**
     * The primitive type {@code byte}.
     */
    BYTE,       // byte基本类型
    
    /**
     * The primitive type {@code short}.
     */
    SHORT,      // short基本类型
    
    /**
     * The primitive type {@code int}.
     */
    INT,        // int基本类型
    
    /**
     * The primitive type {@code long}.
     */
    LONG,       // long基本类型
    
    /**
     * The primitive type {@code char}.
     */
    CHAR,       // char基本类型
    
    /**
     * The primitive type {@code float}.
     */
    FLOAT,      // float基本类型
    
    /**
     * The primitive type {@code double}.
     */
    DOUBLE,     // double基本类型
    
    /**
     * The pseudo-type corresponding to the keyword {@code void}.
     *
     * @see NoType
     */
    VOID,       // void伪类型
    
    /**
     * A pseudo-type used where no actual type is appropriate.
     *
     * @see NoType
     */
    NONE,       // 代表没有合适的类型与之对应的伪类型
    
    /**
     * The null type.
     */
    NULL,       // null类型
    
    /**
     * An array type.
     */
    ARRAY,      // array类型
    
    /**
     * A class or interface type.
     */
    DECLARED,   // 声明类型，如类或接口
    
    /**
     * A class or interface type that could not be resolved.
     */
    ERROR,      // 错误的类型，表示无法正确建模的类或接口类型
    
    /**
     * A type variable.
     */
    TYPEVAR,    // 类型变量
    
    /**
     * A wildcard type argument.
     */
    WILDCARD,   // 通配符
    
    /**
     * A pseudo-type corresponding to a package element.
     *
     * @see NoType
     */
    PACKAGE,    // 代表包的伪类型
    
    /**
     * A method, constructor, or initializer.
     */
    EXECUTABLE, // 可执行类型，如方法、构造器、初始化块
    
    /**
     * An implementation-reserved type.
     * This is not the type you are looking for.
     */
    OTHER,      // 保留类型
    
    /**
     * A union type.
     *
     * @since 1.7
     */
    UNION,      // 联合类型，如多个异常参数联合使用 ExceptionA | ExceptionB e
    
    /**
     * An intersection type.
     *
     * @since 1.8
     */
    INTERSECTION,   // 交集类型，如 Number & Runnable
    
    /**
     * A pseudo-type corresponding to a module element.
     *
     * @spec JPMS
     * @see NoType
     * @since 9
     */
    MODULE;         // 模块类型
    
    /**
     * Returns {@code true} if this kind corresponds to a primitive
     * type and {@code false} otherwise.
     *
     * @return {@code true} if this kind corresponds to a primitive type
     */
    // 判断是否是为基本类型
    public boolean isPrimitive() {
        switch(this) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case CHAR:
            case FLOAT:
            case DOUBLE:
                return true;
            
            default:
                return false;
        }
    }
}
