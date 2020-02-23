/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.util.StringJoiner;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.ConstructorAccessor;
import jdk.internal.reflect.Reflection;
import jdk.internal.vm.annotation.ForceInline;
import sun.reflect.annotation.TypeAnnotation;
import sun.reflect.annotation.TypeAnnotationParser;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.ConstructorRepository;
import sun.reflect.generics.scope.ConstructorScope;

/**
 * {@code Constructor} provides information about, and access to, a single
 * constructor for a class.
 *
 * <p>{@code Constructor} permits widening conversions to occur when matching the
 * actual parameters to newInstance() with the underlying
 * constructor's formal parameters, but throws an
 * {@code IllegalArgumentException} if a narrowing conversion would occur.
 *
 * @param <T> the class in which the constructor is declared
 *
 * @author Kenneth Russell
 * @author Nakul Saraiya
 * @see Member
 * @see java.lang.Class
 * @see java.lang.Class#getConstructors()
 * @see java.lang.Class#getConstructor(Class[])
 * @see java.lang.Class#getDeclaredConstructors()
 * @since 1.1
 */
// 反射元素-构造器
public final class Constructor<T> extends Executable {
    
    private volatile ConstructorAccessor constructorAccessor;   // 构造器访问器，用于对构造器的反射调用
    
    private int slot;   // 当前构造器在宿主类构造器中的序号
    
    private Class<T> clazz; // 当前构造器所在的类
    
    private Class<?>[] parameterTypes;  // 构造器的形参列表
    private Class<?>[] exceptionTypes;  // 构造器抛出的异常列表
    private int modifiers;              // 构造器的修饰符
    
    // Generics and annotations support
    private transient String signature;
    
    // generic info repository; lazily initialized
    private transient ConstructorRepository genericInfo;
    
    private byte[] annotations;             // 作用在构造器上的注解(以字节形式表示，用在反射中)
    private byte[] parameterAnnotations;    // 作用在构造器形参上的注解(以字节形式表示，用在反射中)
    
    /**
     * For sharing of ConstructorAccessors.
     * This branching structure is currently only two levels deep (i.e., one root Constructor and potentially many Constructor objects pointing to it.)
     * If this branching structure would ever contain cycles, deadlocks can occur in annotation code.
     */
    // 如果当前Constructor是复制来的，此处保存它的复制源
    private Constructor<T> root;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Package-private constructor used by ReflectAccess to enable
     * instantiation of these objects in Java code from the java.lang
     * package via sun.reflect.LangReflectAccess.
     */
    Constructor(Class<T> declaringClass, Class<?>[] parameterTypes, Class<?>[] checkedExceptions,
                int modifiers, int slot, String signature, byte[] annotations, byte[] parameterAnnotations) {
        this.clazz = declaringClass;
        this.parameterTypes = parameterTypes;
        this.exceptionTypes = checkedExceptions;
        this.modifiers = modifiers;
        this.slot = slot;
        this.signature = signature;
        this.annotations = annotations;
        this.parameterAnnotations = parameterAnnotations;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置可访问性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * <p> A {@code SecurityException} is also thrown if this object is a
     * {@code Constructor} object for the class {@code Class} and {@code flag}
     * is true. </p>
     *
     * @param flag {@inheritDoc}
     *
     * @throws InaccessibleObjectException {@inheritDoc}
     * @throws SecurityException           if the request is denied by the security manager
     *                                     or this is a constructor for {@code java.lang.Class}
     * @spec JPMS
     */
    // 开启/关闭对当前元素的反射访问权限。访问private的Constructor时必须禁用安全检查，以开启访问权限，即setAccessible(true)
    @Override
    @CallerSensitive
    public void setAccessible(boolean flag) {
        AccessibleObject.checkPermission();
    
        // 如果需要开启访问权限
        if(flag) {
            // 获取setAccessible()的调用者所处的类
            Class<?> caller = Reflection.getCallerClass();
        
            // 判断caller是否可以访问当前元素(涉及到exports和opens的判断)
            checkCanSetAccessible(caller);
        }
    
        setAccessible0(flag);
    }
    
    /*▲ 设置可访问性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 使用构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Uses the constructor represented by this {@code Constructor} object to
     * create and initialize a new instance of the constructor's
     * declaring class, with the specified initialization parameters.
     * Individual parameters are automatically unwrapped to match
     * primitive formal parameters, and both primitive and reference
     * parameters are subject to method invocation conversions as necessary.
     *
     * <p>If the number of formal parameters required by the underlying constructor
     * is 0, the supplied {@code initargs} array may be of length 0 or null.
     *
     * <p>If the constructor's declaring class is an inner class in a
     * non-static context, the first argument to the constructor needs
     * to be the enclosing instance; see section 15.9.3 of
     * <cite>The Java&trade; Language Specification</cite>.
     *
     * <p>If the required access and argument checks succeed and the
     * instantiation will proceed, the constructor's declaring class
     * is initialized if it has not already been initialized.
     *
     * <p>If the constructor completes normally, returns the newly
     * created and initialized instance.
     *
     * @param initargs array of objects to be passed as arguments to
     *                 the constructor call; values of primitive types are wrapped in
     *                 a wrapper object of the appropriate type (e.g. a {@code float}
     *                 in a {@link java.lang.Float Float})
     *
     * @return a new object created by calling the constructor
     * this object represents
     *
     * @throws IllegalAccessException      if this {@code Constructor} object
     *                                     is enforcing Java language access control and the underlying
     *                                     constructor is inaccessible.
     * @throws IllegalArgumentException    if the number of actual
     *                                     and formal parameters differ; if an unwrapping
     *                                     conversion for primitive arguments fails; or if,
     *                                     after possible unwrapping, a parameter value
     *                                     cannot be converted to the corresponding formal
     *                                     parameter type by a method invocation conversion; if
     *                                     this constructor pertains to an enum type.
     * @throws InstantiationException      if the class that declares the
     *                                     underlying constructor represents an abstract class.
     * @throws InvocationTargetException   if the underlying constructor
     *                                     throws an exception.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     */
    // 通过反射创建对象，要求参数与构造器匹配
    @CallerSensitive
    @ForceInline
    public T newInstance(Object... initargs) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // 如果没有禁用安全检查，那么需要检查显式访问权限
        if(!override) {
            // 获取newInstance()调用者所处的类
            Class<?> caller = Reflection.getCallerClass();
        
            // 判断是否可以在caller中反射调用当前元素指示的构造器
            checkAccess(caller, clazz, clazz, modifiers);
        }
    
        // 不允许反射创建枚举对象
        if((clazz.getModifiers() & Modifier.ENUM) != 0) {
            throw new IllegalArgumentException("Cannot reflectively create enum objects");
        }
    
        ConstructorAccessor ca = constructorAccessor;   // read volatile
        if(ca == null) {
            // 获取当前构造器的访问器
            ca = acquireConstructorAccessor();
        }
    
        @SuppressWarnings("unchecked")
        T inst = (T) ca.newInstance(initargs);
    
        return inst;
    }
    
    /*▲ 使用构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 1. 修饰符 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     */
    // 获取构造器的修饰符
    @Override
    public int getModifiers() {
        return modifiers;
    }
    
    /*▲ 1. 修饰符 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 2. 构造器引入的TypeVariable ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * @throws GenericSignatureFormatError {@inheritDoc}
     * @since 1.5
     */
    // 返回构造器引入的TypeVariable
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public TypeVariable<Constructor<T>>[] getTypeParameters() {
        if(getSignature() != null) {
            return (TypeVariable<Constructor<T>>[]) getGenericInfo().getTypeParameters();
        } else
            return (TypeVariable<Constructor<T>>[]) new TypeVariable[0];
    }
    
    /*▲ 2. 构造器引入的TypeVariable ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 3. 返回值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 构造器没有返回值
    
    /*▲ 3. 返回值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 4. 构造器名称 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the name of this constructor, as a string.  This is
     * the binary name of the constructor's declaring class.
     */
    // 返回构造器名称，这与字符串化不同
    @Override
    public String getName() {
        return getDeclaringClass().getName();
    }
    
    /*▲ 4. 构造器名称 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字符串化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a string describing this {@code Constructor}.  The string is
     * formatted as the constructor access modifiers, if any,
     * followed by the fully-qualified name of the declaring class,
     * followed by a parenthesized, comma-separated list of the
     * constructor's formal parameter types.  For example:
     * <pre>{@code
     *    public java.util.Hashtable(int,float)
     * }</pre>
     *
     * <p>If the constructor is declared to throw exceptions, the
     * parameter list is followed by a space, followed by the word
     * "{@code throws}" followed by a comma-separated list of the
     * thrown exception types.
     *
     * <p>The only possible modifiers for constructors are the access
     * modifiers {@code public}, {@code protected} or
     * {@code private}.  Only one of these may appear, or none if the
     * constructor has default (package) access.
     *
     * @return a string describing this {@code Constructor}
     *
     * @jls 8.8.3 Constructor Modifiers
     * @jls 8.9.2 Enum Body Declarations
     */
    // 返回构造器的描述，不带泛型信息
    public String toString() {
        return sharedToString(Modifier.constructorModifiers(), false, parameterTypes, exceptionTypes);
    }
    
    /**
     * Returns a string describing this {@code Constructor},
     * including type parameters.  The string is formatted as the
     * constructor access modifiers, if any, followed by an
     * angle-bracketed comma separated list of the constructor's type
     * parameters, if any, followed by the fully-qualified name of the
     * declaring class, followed by a parenthesized, comma-separated
     * list of the constructor's generic formal parameter types.
     *
     * If this constructor was declared to take a variable number of
     * arguments, instead of denoting the last parameter as
     * "<code><i>Type</i>[]</code>", it is denoted as
     * "<code><i>Type</i>...</code>".
     *
     * A space is used to separate access modifiers from one another
     * and from the type parameters or class name.  If there are no
     * type parameters, the type parameter list is elided; if the type
     * parameter list is present, a space separates the list from the
     * class name.  If the constructor is declared to throw
     * exceptions, the parameter list is followed by a space, followed
     * by the word "{@code throws}" followed by a
     * comma-separated list of the generic thrown exception types.
     *
     * <p>The only possible modifiers for constructors are the access
     * modifiers {@code public}, {@code protected} or
     * {@code private}.  Only one of these may appear, or none if the
     * constructor has default (package) access.
     *
     * @return a string describing this {@code Constructor},
     * include type parameters
     *
     * @jls 8.8.3 Constructor Modifiers
     * @jls 8.9.2 Enum Body Declarations
     * @since 1.5
     */
    // 返回构造器的描述，带着泛型信息
    @Override
    public String toGenericString() {
        return sharedToGenericString(Modifier.constructorModifiers(), false);
    }
    
    /*▲ 字符串化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 5. 构造器形参 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * @since 1.8
     */
    // 获取形参数量
    public int getParameterCount() {
        return parameterTypes.length;
    }
    
    /**
     * {@inheritDoc}
     *
     * @since 1.5
     */
    // 是否为可变数量形参
    @Override
    public boolean isVarArgs() {
        return super.isVarArgs();
    }
    
    /**
     * {@inheritDoc}
     */
    // 获取所有形参的类型[类型擦除]
    @Override
    public Class<?>[] getParameterTypes() {
        return parameterTypes.clone();
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws GenericSignatureFormatError         {@inheritDoc}
     * @throws TypeNotPresentException             {@inheritDoc}
     * @throws MalformedParameterizedTypeException {@inheritDoc}
     * @since 1.5
     */
    // 获取所有形参的类型[支持泛型语义]
    @Override
    public Type[] getGenericParameterTypes() {
        return super.getGenericParameterTypes();
    }
    
    /*▲ 5. 构造器形参 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 6. 构造器抛出的异常 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     */
    // 获取该构造器抛出的异常类型[类型擦除]
    @Override
    public Class<?>[] getExceptionTypes() {
        return exceptionTypes.clone();
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws GenericSignatureFormatError         {@inheritDoc}
     * @throws TypeNotPresentException             {@inheritDoc}
     * @throws MalformedParameterizedTypeException {@inheritDoc}
     * @since 1.5
     */
    // 获取该构造器抛出的异常类型[支持泛型语义]
    @Override
    public Type[] getGenericExceptionTypes() {
        return super.getGenericExceptionTypes();
    }
    
    /*▲ 6. 构造器抛出的异常 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注解 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    // 1-2 返回该元素上指定类型的注解
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return super.getAnnotation(annotationClass);
    }
    
    /**
     * {@inheritDoc}
     *
     * @since 1.5
     */
    // 2-1 返回该元素上所有类型的注解
    public Annotation[] getDeclaredAnnotations() {
        return super.getDeclaredAnnotations();
    }
    
    /**
     * {@inheritDoc}
     *
     * @since 1.5
     */
    // 获取形参上的注解
    @Override
    public Annotation[][] getParameterAnnotations() {
        return sharedGetParameterAnnotations(parameterTypes, parameterAnnotations);
    }
    
    /**
     * {@inheritDoc}
     *
     * @since 1.8
     */
    // 获取返回类型上的【被注解类型】。由于构造器没有返回值，故将该类的声明当作其返回类型
    @Override
    public AnnotatedType getAnnotatedReturnType() {
        return getAnnotatedReturnType0(getDeclaringClass());
    }
    
    /**
     * {@inheritDoc}
     *
     * @since 1.8
     */
    // 获取Receiver Type上的【被注解类型】
    @Override
    public AnnotatedType getAnnotatedReceiverType() {
        Class<?> thisDeclClass = getDeclaringClass();
        Class<?> enclosingClass = thisDeclClass.getEnclosingClass();
        
        if(enclosingClass == null) {
            // A Constructor for a top-level class
            return null;
        }
        
        Class<?> outerDeclaringClass = thisDeclClass.getDeclaringClass();
        if(outerDeclaringClass == null) {
            // A constructor for a local or anonymous class
            return null;
        }
        
        // Either static nested or inner class
        if(Modifier.isStatic(thisDeclClass.getModifiers())) {
            // static nested
            return null;
        }
        
        // A Constructor for an inner class
        return TypeAnnotationParser.buildAnnotatedType(getTypeAnnotationBytes0(), SharedSecrets.getJavaLangAccess().
            getConstantPool(thisDeclClass), this, thisDeclClass, enclosingClass, TypeAnnotation.TypeAnnotationTarget.METHOD_RECEIVER);
    }
    
    /*▲ 注解 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 构造器特征 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the {@code Class} object representing the class that
     * declares the constructor represented by this object.
     */
    // 返回构造器所在的类
    @Override
    public Class<T> getDeclaringClass() {
        return clazz;
    }
    
    /**
     * {@inheritDoc}
     *
     * @jls 13.1 The Form of a Binary
     * @since 1.5
     */
    /*
     * 是否由编译器引入（非人为定义）
     *
     * 示例：
     *
     * package com.kang;
     *
     * public class Outer {
     *     public void accessInner(){
     *         Inner inner = new Inner();
     *     }
     *
     *     class Inner{
     *         private Inner() {
     *         }
     *     }
     * }
     *
     * 打印内部类Inner中的构造器，会发现一个特殊的构造器：
     *
     * com.kang.Outer$Inner(com.kang.Outer, com.kang.Outer$1)
     *
     * 这个构造器用来联结外部类和内部类，其isSynthetic()==true
     */
    @Override
    public boolean isSynthetic() {
        return super.isSynthetic();
    }
    
    /*▲ 构造器特征 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Compares this {@code Constructor} against the specified object.
     * Returns true if the objects are the same.  Two {@code Constructor} objects are
     * the same if they were declared by the same class and have the
     * same formal parameter types.
     */
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof Constructor) {
            Constructor<?> other = (Constructor<?>) obj;
            if(getDeclaringClass() == other.getDeclaringClass()) {
                return equalParamTypes(parameterTypes, other.parameterTypes);
            }
        }
        return false;
    }
    
    /**
     * Returns a hashcode for this {@code Constructor}. The hashcode is
     * the same as the hashcode for the underlying constructor's
     * declaring class name.
     */
    public int hashCode() {
        return getDeclaringClass().getName().hashCode();
    }
    
    
    
    
    
    
    @Override
    Constructor<T> getRoot() {
        return root;
    }
    
    /**
     * Package-private routine (exposed to java.lang.Class via
     * ReflectAccess) which returns a copy of this Constructor. The copy's
     * "root" field points to this Constructor.
     */
    Constructor<T> copy() {
        // This routine enables sharing of ConstructorAccessor objects
        // among Constructor objects which refer to the same underlying
        // method in the VM. (All of this contortion is only necessary
        // because of the "accessibility" bit in AccessibleObject,
        // which implicitly requires that new java.lang.reflect
        // objects be fabricated for each reflective call on Class
        // objects.)
        if(this.root != null)
            throw new IllegalArgumentException("Can not copy a non-root Constructor");
        
        Constructor<T> res = new Constructor<>(clazz, parameterTypes, exceptionTypes, modifiers, slot, signature, annotations, parameterAnnotations);
        res.root = this;
        // Might as well eagerly propagate this if already present
        res.constructorAccessor = constructorAccessor;
        return res;
    }
    
    @Override
    Class<?>[] getSharedParameterTypes() {
        return parameterTypes;
    }
    
    @Override
    Class<?>[] getSharedExceptionTypes() {
        return exceptionTypes;
    }
    
    @Override
    void specificToGenericStringHeader(StringBuilder sb) {
        specificToStringHeader(sb);
    }
    
    // 判断caller是否可以访问当前元素(涉及到exports和opens的判断)
    @Override
    void checkCanSetAccessible(Class<?> caller) {
        checkCanSetAccessible(caller, clazz);
        
        if(clazz == Class.class) {
            // can we change this to InaccessibleObjectException?
            throw new SecurityException("Cannot make a java.lang.Class constructor accessible");
        }
    }
    
    @Override
    boolean hasGenericInformation() {
        return (getSignature() != null);
    }
    
    // 返回当前构造器在其宿主类构造器中的序号
    int getSlot() {
        return slot;
    }
    
    String getSignature() {
        return signature;
    }
    
    @Override
    byte[] getAnnotationBytes() {
        return annotations;
    }
    
    byte[] getRawAnnotations() {
        return annotations;
    }
    
    byte[] getRawParameterAnnotations() {
        return parameterAnnotations;
    }
    
    /** Sets the ConstructorAccessor for this Constructor object and (recursively) its root */
    // 为当前构造器设置访问器
    void setConstructorAccessor(ConstructorAccessor accessor) {
        constructorAccessor = accessor;
        
        // Propagate up
        if(root != null) {
            root.setConstructorAccessor(accessor);
        }
    }
    
    @Override
    boolean handleParameterNumberMismatch(int resultLength, int numParameters) {
        Class<?> declaringClass = getDeclaringClass();
        if(declaringClass.isEnum() || declaringClass.isAnonymousClass() || declaringClass.isLocalClass())
            return false; // Can't do reliable parameter counting
        else {
            if(declaringClass.isMemberClass() && ((declaringClass.getModifiers() & Modifier.STATIC) == 0) && resultLength + 1 == numParameters) {
                return true;
            } else {
                throw new AnnotationFormatError("Parameter annotations don't match number of parameters");
            }
        }
    }
    
    @Override
    void specificToStringHeader(StringBuilder sb) {
        sb.append(getDeclaringClass().getTypeName());
    }
    
    @Override
    String toShortString() {
        StringBuilder sb = new StringBuilder("constructor ");
        sb.append(getDeclaringClass().getTypeName());
        sb.append('(');
        StringJoiner sj = new StringJoiner(",");
        for(Class<?> parameterType : getParameterTypes()) {
            sj.add(parameterType.getTypeName());
        }
        sb.append(sj);
        sb.append(')');
        return sb.toString();
    }
    
    // Returns ConstructorAccessor for this Constructor object, not looking up the chain to the root
    ConstructorAccessor getConstructorAccessor() {
        return constructorAccessor;
    }
    
    // Accessor for generic info repository
    @Override
    ConstructorRepository getGenericInfo() {
        // lazily initialize repository if necessary
        if(genericInfo == null) {
            // create and cache generic info repository
            genericInfo = ConstructorRepository.make(getSignature(), getFactory());
        }
        return genericInfo; //return cached repository
    }
    
    // Generics infrastructure. Accessor for factory
    private GenericsFactory getFactory() {
        // create scope and factory
        return CoreReflectionFactory.make(this, ConstructorScope.make(this));
    }
    
    /**
     * NOTE that there is no synchronization used here.
     * It is correct (though not efficient) to generate more than one ConstructorAccessor for a given Constructor.
     * However, avoiding synchronization will probably make the implementation more scalable.
     */
    // 返回当前构造器的访问器
    private ConstructorAccessor acquireConstructorAccessor() {
        // First check to see if one has been created yet, and take it if so.
        ConstructorAccessor tmp = null;
    
        if(root != null) {
            tmp = root.getConstructorAccessor();
        }
    
        if(tmp != null) {
            constructorAccessor = tmp;
        } else {
            /* Otherwise fabricate one and propagate it up to the root */
            tmp = reflectionFactory.newConstructorAccessor(this);
        
            setConstructorAccessor(tmp);
        }
    
        return tmp;
    }
}
