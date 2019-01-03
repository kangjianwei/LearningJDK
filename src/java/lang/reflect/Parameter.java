/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import sun.reflect.annotation.AnnotationSupport;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Information about method parameters.
 *
 * A {@code Parameter} provides information about method parameters,
 * including its name and modifiers.  It also provides an alternate
 * means of obtaining attributes for the parameter.
 *
 * @since 1.8
 */
// 形参
public final class Parameter implements AnnotatedElement {
    private final String name;
    private final int modifiers;
    private final Executable executable;
    private final int index;
    private transient volatile Type parameterTypeCache;
    private transient volatile Class<?> parameterClassCache;
    private transient Map<Class<? extends Annotation>, Annotation> declaredAnnotations;
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Package-private constructor for {@code Parameter}.
     *
     * If method parameter data is present in the classfile, then the
     * JVM creates {@code Parameter} objects directly.  If it is
     * absent, however, then {@code Executable} uses this constructor
     * to synthesize them.
     *
     * @param name       The name of the parameter.
     * @param modifiers  The modifier flags for the parameter.
     * @param executable The executable which defines this parameter.
     * @param index      The index of the parameter.
     */
    Parameter(String name, int modifiers, Executable executable, int index) {
        this.name = name;
        this.modifiers = modifiers;
        this.executable = executable;
        this.index = index;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 修饰符 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Get the modifier flags for this the parameter represented by
     * this {@code Parameter} object.
     *
     * @return The modifier flags for this parameter.
     */
    // 获取形参的修饰符
    public int getModifiers() {
        return modifiers;
    }
    
    /*▲ 修饰符 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 形参类型 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a {@code Class} object that identifies the
     * declared type for the parameter represented by this
     * {@code Parameter} object.
     *
     * @return a {@code Class} object identifying the declared
     * type of the parameter represented by this object
     */
    // 获取形参类型[类型擦除]
    public Class<?> getType() {
        Class<?> tmp = parameterClassCache;
        if(null == tmp) {
            tmp = executable.getParameterTypes()[index];
            parameterClassCache = tmp;
        }
        return tmp;
    }
    
    /**
     * Returns a {@code Type} object that identifies the parameterized
     * type for the parameter represented by this {@code Parameter}
     * object.
     *
     * @return a {@code Type} object identifying the parameterized
     * type of the parameter represented by this object
     */
    // 获取形参类型[支持泛型语义]
    public Type getParameterizedType() {
        Type tmp = parameterTypeCache;
        if(null == tmp) {
            tmp = executable.getAllGenericParameterTypes()[index];
            parameterTypeCache = tmp;
        }
        
        return tmp;
    }
    
    /*▲ 形参类型 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 形参名称 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the name of the parameter.  If the parameter's name is
     * {@linkplain #isNamePresent() present}, then this method returns
     * the name provided by the class file. Otherwise, this method
     * synthesizes a name of the form argN, where N is the index of
     * the parameter in the descriptor of the method which declares
     * the parameter.
     *
     * @return The name of the parameter, either provided by the class
     * file or synthesized if the class file does not provide
     * a name.
     */
    // 返回形参名称，这与字符串化不同
    public String getName() {
        // Note: empty strings as parameter names are now outlawed.
        // The .equals("") is for compatibility with current JVM
        // behavior.  It may be removed at some point.
        if(name == null || name.equals(""))
            return "arg" + index;
        else
            return name;
    }
    
    /*▲ 形参名称 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字符串化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a string describing this parameter.  The format is the
     * modifiers for the parameter, if any, in canonical order as
     * recommended by <cite>The Java&trade; Language
     * Specification</cite>, followed by the fully- qualified type of
     * the parameter (excluding the last [] if the parameter is
     * variable arity), followed by "..." if the parameter is variable
     * arity, followed by a space, followed by the name of the
     * parameter.
     *
     * @return A string representation of the parameter and associated
     * information.
     */
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final Type type = getParameterizedType();
        final String typename = type.getTypeName();
        
        sb.append(Modifier.toString(getModifiers()));
        
        if(0 != modifiers)
            sb.append(' ');
        
        if(isVarArgs())
            sb.append(typename.replaceFirst("\\[\\]$", "..."));
        else
            sb.append(typename);
        
        sb.append(' ');
        sb.append(getName());
        
        return sb.toString();
    }
    
    /*▲ 字符串化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注解 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     */
    // 1-1 返回该元素上所有类型的注解
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    // 1-2 返回该元素上指定类型的注解
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        return annotationClass.cast(declaredAnnotations().get(annotationClass));
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    // 1-3 返回该元素上指定类型的注解[支持获取@Repeatable类型的注解]
    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        
        return AnnotationSupport.getDirectlyAndIndirectlyPresent(declaredAnnotations(), annotationClass);
    }
    
    /**
     * {@inheritDoc}
     */
    // 2-1 返回该元素上所有类型的注解
    public Annotation[] getDeclaredAnnotations() {
        return executable.getParameterAnnotations()[index];
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 2-2 返回该元素上指定类型的注解
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        // Only annotations on classes are inherited, for all other
        // objects getDeclaredAnnotation is the same as
        // getAnnotation.
        return getAnnotation(annotationClass);
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 2-3 返回该元素上指定类型的注解[支持获取@Repeatable类型的注解]
    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        // Only annotations on classes are inherited, for all other
        // objects getDeclaredAnnotations is the same as
        // getAnnotations.
        return getAnnotationsByType(annotationClass);
    }
    
    /**
     * Returns an AnnotatedType object that represents the use of a type to
     * specify the type of the formal parameter represented by this Parameter.
     *
     * @return an {@code AnnotatedType} object representing the use of a type
     * to specify the type of the formal parameter represented by this
     * Parameter
     */
    // 获取形参类型处的【被注解类型】
    public AnnotatedType getAnnotatedType() {
        // no caching for now
        return executable.getAnnotatedParameterTypes()[index];
    }
    
    /*▲ 注解 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 形参特征 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Return the {@code Executable} which declares this parameter.
     *
     * @return The {@code Executable} declaring this parameter.
     */
    // 返回形参所在的方法/构造器
    public Executable getDeclaringExecutable() {
        return executable;
    }
    
    /**
     * Returns true if the parameter has a name according to the class
     * file; returns false otherwise. Whether a parameter has a name
     * is determined by the {@literal MethodParameters} attribute of
     * the method which declares the parameter.
     *
     * @return true if and only if the parameter has a name according
     * to the class file.
     */
    // 是否保留了源码中的参数名称。默认不保留，如果需要保留，需要在编译时加入参数-parameters
    public boolean isNamePresent() {
        return executable.hasRealParameterData() && name != null;
    }
    
    /**
     * Returns {@code true} if this parameter is implicitly declared
     * in source code; returns {@code false} otherwise.
     *
     * @return true if and only if this parameter is implicitly
     * declared as defined by <cite>The Java&trade; Language
     * Specification</cite>.
     */
    // 是否为隐式参数，java编译器会为内部类的构造方法创建一个隐式参数
    public boolean isImplicit() {
        return Modifier.isMandated(getModifiers());
    }
    
    /**
     * Returns {@code true} if this parameter is neither implicitly
     * nor explicitly declared in source code; returns {@code false}
     * otherwise.
     *
     * @return true if and only if this parameter is a synthetic
     * construct as defined by
     * <cite>The Java&trade; Language Specification</cite>.
     *
     * @jls 13.1 The Form of a Binary
     */
    // 是否由编译器引入（非人为定义）
    public boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }
    
    /**
     * Returns {@code true} if this parameter represents a variable
     * argument list; returns {@code false} otherwise.
     *
     * @return {@code true} if an only if this parameter represents a
     * variable argument list.
     */
    // 是否为可变数量形参
    public boolean isVarArgs() {
        return executable.isVarArgs() && index == executable.getParameterCount() - 1;
    }
    
    /*▲ 形参特征 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Compares based on the executable and the index.
     *
     * @param obj The object to compare.
     *
     * @return Whether or not this is equal to the argument.
     */
    public boolean equals(Object obj) {
        if(obj instanceof Parameter) {
            Parameter other = (Parameter) obj;
            return (other.executable.equals(executable) && other.index == index);
        }
        return false;
    }
    
    /**
     * Returns a hash code based on the executable's hash code and the
     * index.
     *
     * @return A hash code based on the executable's hash code.
     */
    public int hashCode() {
        return executable.hashCode() ^ index;
    }
    
    
    
    // Package-private accessor to the real name field.
    String getRealName() {
        return name;
    }
    
    private synchronized Map<Class<? extends Annotation>, Annotation> declaredAnnotations() {
        if(null == declaredAnnotations) {
            declaredAnnotations = new HashMap<>();
            for(Annotation a : getDeclaredAnnotations())
                declaredAnnotations.put(a.annotationType(), a);
        }
        return declaredAnnotations;
    }
}
