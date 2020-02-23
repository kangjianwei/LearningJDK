/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.MethodAccessor;
import jdk.internal.reflect.Reflection;
import jdk.internal.vm.annotation.ForceInline;
import sun.reflect.annotation.AnnotationParser;
import sun.reflect.annotation.AnnotationType;
import sun.reflect.annotation.ExceptionProxy;
import sun.reflect.annotation.TypeNotPresentExceptionProxy;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.MethodRepository;
import sun.reflect.generics.scope.MethodScope;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.nio.ByteBuffer;
import java.util.StringJoiner;

/**
 * A {@code Method} provides information about, and access to, a single method
 * on a class or interface.  The reflected method may be a class method
 * or an instance method (including an abstract method).
 *
 * <p>A {@code Method} permits widening conversions to occur when matching the
 * actual parameters to invoke with the underlying method's formal
 * parameters, but it throws an {@code IllegalArgumentException} if a
 * narrowing conversion would occur.
 *
 * @author Kenneth Russell
 * @author Nakul Saraiya
 * @see Member
 * @see java.lang.Class
 * @see java.lang.Class#getMethods()
 * @see java.lang.Class#getMethod(String, Class[])
 * @see java.lang.Class#getDeclaredMethods()
 * @see java.lang.Class#getDeclaredMethod(String, Class[])
 * @since 1.1
 */
// 反射元素-方法（在注解接口中的方法也被称为属性）
public final class Method extends Executable {
    
    private volatile MethodAccessor methodAccessor; // 方法访问器，用于对方法的反射调用
    
    private int slot;   // 当前方法在宿主类方法中的序号
    
    private Class<?> clazz; // 当前方法所在的类
    
    // This is guaranteed to be interned by the VM in the 1.4 reflection implementation
    private String name;                // 方法名称
    private Class<?> returnType;        // 方法的返回类型
    private Class<?>[] parameterTypes;  // 方法的形参列表
    private Class<?>[] exceptionTypes;  // 方法抛出的异常列表
    private int modifiers;              // 方法的修饰符
    
    // Generics and annotations support
    private transient String signature;
    
    // generic info repository; lazily initialized
    private transient MethodRepository genericInfo;
    
    private byte[] annotations;             // 作用在方法上的注解(以字节形式表示，用在反射中)
    private byte[] parameterAnnotations;    // 作用在方法形参上的注解(以字节形式表示，用在反射中)
    private byte[] annotationDefault;       // 注解接口中方法(属性)的默认值(以字节形式表示，用在反射中)
    
    /**
     * For sharing of MethodAccessors.
     * This branching structure is currently only two levels deep (i.e., one root Method and potentially many Method objects pointing to it.)
     * If this branching structure would ever contain cycles, deadlocks can occur in annotation code.
     */
    // 如果当前Method是复制来的，此处保存它的复制源
    private Method root;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Package-private constructor used by ReflectAccess to enable
     * instantiation of these objects in Java code from the java.lang
     * package via sun.reflect.LangReflectAccess.
     */
    Method(Class<?> declaringClass, String name, Class<?>[] parameterTypes, Class<?> returnType, Class<?>[] checkedExceptions,
           int modifiers, int slot, String signature, byte[] annotations, byte[] parameterAnnotations, byte[] annotationDefault) {
        this.clazz = declaringClass;
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.exceptionTypes = checkedExceptions;
        this.modifiers = modifiers;
        this.slot = slot;
        this.signature = signature;
        this.annotations = annotations;
        this.parameterAnnotations = parameterAnnotations;
        this.annotationDefault = annotationDefault;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置可访问性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws InaccessibleObjectException {@inheritDoc}
     * @throws SecurityException           {@inheritDoc}
     */
    // 开启/关闭对当前元素的反射访问权限。访问private的Method时必须禁用安全检查，以开启访问权限，即setAccessible(true)
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
    
    
    
    /*▼ 使用方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     * Individual parameters are automatically unwrapped to match
     * primitive formal parameters, and both primitive and reference
     * parameters are subject to method invocation conversions as
     * necessary.
     *
     * <p>If the underlying method is static, then the specified {@code obj}
     * argument is ignored. It may be null.
     *
     * <p>If the number of formal parameters required by the underlying method is
     * 0, the supplied {@code args} array may be of length 0 or null.
     *
     * <p>If the underlying method is an instance method, it is invoked
     * using dynamic method lookup as documented in The Java Language
     * Specification, section 15.12.4.4; in particular,
     * overriding based on the runtime type of the target object may occur.
     *
     * <p>If the underlying method is static, the class that declared
     * the method is initialized if it has not already been initialized.
     *
     * <p>If the method completes normally, the value it returns is
     * returned to the caller of invoke; if the value has a primitive
     * type, it is first appropriately wrapped in an object. However,
     * if the value has the type of an array of a primitive type, the
     * elements of the array are <i>not</i> wrapped in objects; in
     * other words, an array of primitive type is returned.  If the
     * underlying method return type is void, the invocation returns
     * null.
     *
     * @param obj  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @return the result of dispatching the method represented by
     * this object on {@code obj} with parameters
     * {@code args}
     *
     * @throws IllegalAccessException      if this {@code Method} object
     *                                     is enforcing Java language access control and the underlying
     *                                     method is inaccessible.
     * @throws IllegalArgumentException    if the method is an
     *                                     instance method and the specified object argument
     *                                     is not an instance of the class or interface
     *                                     declaring the underlying method (or of a subclass
     *                                     or implementor thereof); if the number of actual
     *                                     and formal parameters differ; if an unwrapping
     *                                     conversion for primitive arguments fails; or if,
     *                                     after possible unwrapping, a parameter value
     *                                     cannot be converted to the corresponding formal
     *                                     parameter type by a method invocation conversion.
     * @throws InvocationTargetException   if the underlying method
     *                                     throws an exception.
     * @throws NullPointerException        if the specified object is null
     *                                     and the method is an instance method.
     * @throws ExceptionInInitializerError if the initialization
     *                                     provoked by this method fails.
     */
    // 使用obj对象调用当前方法，args为传入的参数。如果是静态方法调用，obj为null
    @CallerSensitive
    @ForceInline
    @HotSpotIntrinsicCandidate
    public Object invoke(Object obj, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // 如果没有禁用安全检查，那么需要检查显式访问权限
        if(!override) {
            // 获取invoke()方法的调用者所处的类
            Class<?> caller = Reflection.getCallerClass();
        
            // 判断是否可以在caller中反射调用当前元素指示的方法
            checkAccess(caller, clazz, Modifier.isStatic(modifiers) ? null : obj.getClass(), modifiers);
        }
    
        MethodAccessor ma = methodAccessor;     // read volatile
        if(ma == null) {
            // 获取方法访问器
            ma = acquireMethodAccessor();
        }
    
        return ma.invoke(obj, args);
    }
    
    /*▲ 使用方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 1. 修饰符 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     */
    // 获取方法的修饰符
    @Override
    public int getModifiers() {
        return modifiers;
    }
    
    /*▲ 1. 修饰符 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 2. 方法引入的TypeVariable ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * @throws GenericSignatureFormatError {@inheritDoc}
     * @since 1.5
     */
    // 方法引入的TypeVariable
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public TypeVariable<Method>[] getTypeParameters() {
        if(getGenericSignature() != null) {
            return (TypeVariable<Method>[]) getGenericInfo().getTypeParameters();
        } else {
            return (TypeVariable<Method>[]) new TypeVariable[0];
        }
    }
    
    /*▲ 2. 方法引入的TypeVariable ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 3. 返回值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a {@code Class} object that represents the formal return type
     * of the method represented by this {@code Method} object.
     *
     * @return the return type for the method this object represents
     */
    // 获取返回值类型[类型擦除]
    public Class<?> getReturnType() {
        return returnType;
    }
    
    /**
     * Returns a {@code Type} object that represents the formal return
     * type of the method represented by this {@code Method} object.
     *
     * <p>If the return type is a parameterized type,
     * the {@code Type} object returned must accurately reflect
     * the actual type parameters used in the source code.
     *
     * <p>If the return type is a type variable or a parameterized type, it
     * is created. Otherwise, it is resolved.
     *
     * @return a {@code Type} object that represents the formal return
     * type of the underlying  method
     *
     * @throws GenericSignatureFormatError         if the generic method signature does not conform to the format
     *                                             specified in
     *                                             <cite>The Java&trade; Virtual Machine Specification</cite>
     * @throws TypeNotPresentException             if the underlying method's
     *                                             return type refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if the
     *                                             underlying method's return typed refers to a parameterized
     *                                             type that cannot be instantiated for any reason
     * @since 1.5
     */
    // 获取返回值类型[支持泛型语义]
    public Type getGenericReturnType() {
        if(getGenericSignature() != null) {
            return getGenericInfo().getReturnType();
        } else {
            return getReturnType();
        }
    }
    
    /*▲ 3. 返回值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 4. 方法名称 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the name of the method represented by this {@code Method}
     * object, as a {@code String}.
     */
    // 返回方法名称，这与字符串化不同
    @Override
    public String getName() {
        return name;
    }
    
    /*▲ 4. 方法名称 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字符串化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a string describing this {@code Method}.  The string is
     * formatted as the method access modifiers, if any, followed by
     * the method return type, followed by a space, followed by the
     * class declaring the method, followed by a period, followed by
     * the method name, followed by a parenthesized, comma-separated
     * list of the method's formal parameter types. If the method
     * throws checked exceptions, the parameter list is followed by a
     * space, followed by the word "{@code throws}" followed by a
     * comma-separated list of the thrown exception types.
     * For example:
     * <pre>
     *    public boolean java.lang.Object.equals(java.lang.Object)
     * </pre>
     *
     * <p>The access modifiers are placed in canonical order as
     * specified by "The Java Language Specification".  This is
     * {@code public}, {@code protected} or {@code private} first,
     * and then other modifiers in the following order:
     * {@code abstract}, {@code default}, {@code static}, {@code final},
     * {@code synchronized}, {@code native}, {@code strictfp}.
     *
     * @return a string describing this {@code Method}
     *
     * @jls 8.4.3 Method Modifiers
     * @jls 9.4   Method Declarations
     * @jls 9.6.1 Annotation Type Elements
     */
    // 返回方法的描述，不带泛型信息
    public String toString() {
        return sharedToString(Modifier.methodModifiers(), isDefault(), parameterTypes, exceptionTypes);
    }
    
    /**
     * Returns a string describing this {@code Method}, including
     * type parameters.  The string is formatted as the method access
     * modifiers, if any, followed by an angle-bracketed
     * comma-separated list of the method's type parameters, if any,
     * followed by the method's generic return type, followed by a
     * space, followed by the class declaring the method, followed by
     * a period, followed by the method name, followed by a
     * parenthesized, comma-separated list of the method's generic
     * formal parameter types.
     *
     * If this method was declared to take a variable number of
     * arguments, instead of denoting the last parameter as
     * "<code><i>Type</i>[]</code>", it is denoted as
     * "<code><i>Type</i>...</code>".
     *
     * A space is used to separate access modifiers from one another
     * and from the type parameters or return type.  If there are no
     * type parameters, the type parameter list is elided; if the type
     * parameter list is present, a space separates the list from the
     * class name.  If the method is declared to throw exceptions, the
     * parameter list is followed by a space, followed by the word
     * "{@code throws}" followed by a comma-separated list of the generic
     * thrown exception types.
     *
     * <p>The access modifiers are placed in canonical order as
     * specified by "The Java Language Specification".  This is
     * {@code public}, {@code protected} or {@code private} first,
     * and then other modifiers in the following order:
     * {@code abstract}, {@code default}, {@code static}, {@code final},
     * {@code synchronized}, {@code native}, {@code strictfp}.
     *
     * @return a string describing this {@code Method},
     * include type parameters
     *
     * @jls 8.4.3 Method Modifiers
     * @jls 9.4   Method Declarations
     * @jls 9.6.1 Annotation Type Elements
     * @since 1.5
     */
    // 返回方法的描述，带着泛型信息
    @Override
    public String toGenericString() {
        return sharedToGenericString(Modifier.methodModifiers(), isDefault());
    }
    
    /*▲ 字符串化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 5. 方法形参 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    
    /*▲ 5. 方法形参 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 6. 方法抛出的异常 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     */
    // 获取该方法抛出的异常类型[类型擦除]
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
    // 获取该方法抛出的异常类型[支持泛型语义]
    @Override
    public Type[] getGenericExceptionTypes() {
        return super.getGenericExceptionTypes();
    }
    
    /*▲ 6. 方法抛出的异常 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
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
    // 获取返回类型上的【被注解类型】
    @Override
    public AnnotatedType getAnnotatedReturnType() {
        return getAnnotatedReturnType0(getGenericReturnType());
    }
    
    /*▲ 注解 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 方法特征 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the {@code Class} object representing the class or interface
     * that declares the method represented by this object.
     */
    // 返回方法所在的类
    @Override
    public Class<?> getDeclaringClass() {
        return clazz;
    }
    
    /**
     * {@inheritDoc}
     *
     * @jls 13.1 The Form of a Binary
     * @since 1.5
     */
    /*
     * 是否为合成方法，即由编译器引入（非人为定义）
     *
     * 示例一：泛型兼容中引入的方法，参见isBridge()
     *
     *
     * 示例二：外部类和内部类之间的访问
     *
     * package com.kang;
     *
     * public class Outer {
     *     private String outer = "outer";
     *
     *     public void accessInner(){
     *         System.out.println(new Inner().inner);
     *     }
     *
     *     class Inner{
     *         private String inner = "inner";
     *
     *         public void accessOuter(){
     *             System.out.println(outer);
     *         }
     *     }
     * }
     *
     * 分别打印Outer和Inner中的方法：
     *
     * ==============Outer==============
     * public void com.kang.Outer.accessInner()
     * static java.lang.String com.kang.Outer.access$100(com.kang.Outer)
     *
     * ==============Inner==============
     * public void com.kang.Outer$Inner.accessOuter()
     * static java.lang.String com.kang.Outer$Inner.access$000(com.kang.Outer$Inner)
     *
     * 如上，Outer中的access$100和Innter中的access$000就是由编译器引入的方法
     */
    @Override
    public boolean isSynthetic() {
        return super.isSynthetic();
    }
    
    /**
     * Returns {@code true} if this method is a bridge
     * method; returns {@code false} otherwise.
     *
     * @return true if and only if this method is a bridge
     * method as defined by the Java Language Specification.
     *
     * @since 1.5
     */
    /*
     * 是否为桥接方法
     *
     * 示例：
     *
     * package com.kang;
     *
     * public interface Super<T> {
     *     void print(T t);
     * }
     *
     * public class Sub implements Super<String>{
     *     public void print(String s) {
     *         System.out.println(s);
     *     }
     * }
     *
     * 为了兼容JDK 1.5之前的代码，泛型在编译时会被抹去，这样的话就变为：
     * public interface Super {
     *     void print(Object t);
     * }
     *
     * public class Sub implements Super {
     *     public void print(String s) {
     *         System.out.println(s);
     *     }
     * }
     *
     * 但是这样的话，Sub就没有重写到Super中的方法，为了解决这个问题，虚拟机会引入一个桥接方法，即Sub在编译后实际生成的代码为：
     * public class Sub implements Super{
     *     public void print(String s) {
     *         System.out.println(s);
     *     }
     *
     *     // 这就是桥接方法，满足isBridge()==true，也满足isSynthetic()==true
     *     public void print(Object s) {
     *         this.print((String)s);
     *     }
     * }
     */
    public boolean isBridge() {
        return (getModifiers() & Modifier.BRIDGE) != 0;
    }
    
    /**
     * Returns {@code true} if this method is a default
     * method; returns {@code false} otherwise.
     *
     * A default method is a public non-abstract instance method, that
     * is, a non-static method with a body, declared in an interface
     * type.
     *
     * @return true if and only if this method is a default
     * method as defined by the Java Language Specification.
     *
     * @since 1.8
     */
    // 是否为接口中的default方法
    public boolean isDefault() {
        // Default methods are public non-abstract instance methods declared in an interface.
        return ((getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC) && getDeclaringClass().isInterface();
    }
    
    /**
     * Returns the default value for the annotation member represented by
     * this {@code Method} instance.  If the member is of a primitive type,
     * an instance of the corresponding wrapper type is returned. Returns
     * null if no default is associated with the member, or if the method
     * instance does not represent a declared member of an annotation type.
     *
     * @return the default value for the annotation member represented
     * by this {@code Method} instance.
     *
     * @throws TypeNotPresentException if the annotation is of type
     *                                 {@link Class} and no definition can be found for the
     *                                 default class value.
     * @since 1.5
     */
    // 返回当前方法(属性)在注解接口中的默认值(非注解接口情形下返回null)
    public Object getDefaultValue() {
        if(annotationDefault == null) {
            return null;
        }
    
        Class<?> memberType = AnnotationType.invocationHandlerReturnType(getReturnType());
    
        Object result = AnnotationParser.parseMemberValue(memberType, ByteBuffer.wrap(annotationDefault), SharedSecrets.getJavaLangAccess().getConstantPool(getDeclaringClass()), getDeclaringClass());
        if(result instanceof ExceptionProxy) {
            if(result instanceof TypeNotPresentExceptionProxy) {
                TypeNotPresentExceptionProxy proxy = (TypeNotPresentExceptionProxy) result;
                throw new TypeNotPresentException(proxy.typeName(), proxy.getCause());
            }
        
            throw new AnnotationFormatError("Invalid default: " + this);
        }
    
        return result;
    }
    
    /*▲ 方法特征 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Compares this {@code Method} against the specified object.  Returns
     * true if the objects are the same.  Two {@code Methods} are the same if
     * they were declared by the same class and have the same name
     * and formal parameter types and return type.
     */
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof Method) {
            Method other = (Method) obj;
            if((getDeclaringClass() == other.getDeclaringClass()) && (getName() == other.getName())) {
                if(!returnType.equals(other.getReturnType()))
                    return false;
                return equalParamTypes(parameterTypes, other.parameterTypes);
            }
        }
    
        return false;
    }
    
    /**
     * Returns a hashcode for this {@code Method}.  The hashcode is computed
     * as the exclusive-or of the hashcodes for the underlying
     * method's declaring class name and the method's name.
     */
    public int hashCode() {
        return getDeclaringClass().getName().hashCode() ^ getName().hashCode();
    }
    
    
    /** Accessor for generic info repository */
    @Override
    MethodRepository getGenericInfo() {
        // lazily initialize repository if necessary
        if(genericInfo == null) {
            // create and cache generic info repository
            genericInfo = MethodRepository.make(getGenericSignature(), getFactory());
        }
    
        return genericInfo; //return cached repository
    }
    
    /**
     * Package-private routine (exposed to java.lang.Class via ReflectAccess) which returns a copy of this Method.
     * The copy's "root" field points to this Method.
     */
    Method copy() {
        /*
         * This routine enables sharing of MethodAccessor objects among Method objects which refer to the same underlying method in the VM.
         * (All of this contortion is only necessary because of the "accessibility" bit in AccessibleObject,
         * which implicitly requires that new java.lang.reflect objects be fabricated for each reflective call on Class objects.)
         */
        if(this.root != null) {
            throw new IllegalArgumentException("Can not copy a non-root Method");
        }
    
        Method res = new Method(clazz, name, parameterTypes, returnType, exceptionTypes, modifiers, slot, signature, annotations, parameterAnnotations, annotationDefault);
        res.root = this;
        res.methodAccessor = methodAccessor;    // Might as well eagerly propagate this if already present
    
        return res;
    }
    
    /**
     * Make a copy of a leaf method.
     */
    Method leafCopy() {
        if(this.root == null) {
            throw new IllegalArgumentException("Can only leafCopy a non-root Method");
        }
        
        Method res = new Method(clazz, name, parameterTypes, returnType, exceptionTypes, modifiers, slot, signature, annotations, parameterAnnotations, annotationDefault);
        res.root = root;
        res.methodAccessor = methodAccessor;
    
        return res;
    }
    
    @Override
    void checkCanSetAccessible(Class<?> caller) {
        checkCanSetAccessible(caller, clazz);
    }
    
    @Override
    Method getRoot() {
        return root;
    }
    
    @Override
    boolean hasGenericInformation() {
        return (getGenericSignature() != null);
    }
    
    @Override
    byte[] getAnnotationBytes() {
        return annotations;
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
    void specificToStringHeader(StringBuilder sb) {
        sb.append(getReturnType().getTypeName()).append(' ');
        sb.append(getDeclaringClass().getTypeName()).append('.');
        sb.append(getName());
    }
    
    @Override
    String toShortString() {
        StringBuilder sb = new StringBuilder("method ");
        sb.append(getDeclaringClass().getTypeName()).append('.');
        sb.append(getName());
        sb.append('(');
        StringJoiner sj = new StringJoiner(",");
        for(Class<?> parameterType : getParameterTypes()) {
            sj.add(parameterType.getTypeName());
        }
        sb.append(sj);
        sb.append(')');
        return sb.toString();
    }
    
    @Override
    void specificToGenericStringHeader(StringBuilder sb) {
        Type genRetType = getGenericReturnType();
        sb.append(genRetType.getTypeName()).append(' ');
        sb.append(getDeclaringClass().getTypeName()).append('.');
        sb.append(getName());
    }
    
    /** Returns MethodAccessor for this Method object, not looking up the chain to the root */
    MethodAccessor getMethodAccessor() {
        return methodAccessor;
    }
    
    /** Sets the MethodAccessor for this Method object and (recursively) its root */
    void setMethodAccessor(MethodAccessor accessor) {
        methodAccessor = accessor;
    
        // Propagate up
        if(root != null) {
            root.setMethodAccessor(accessor);
        }
    }
    
    @Override
    boolean handleParameterNumberMismatch(int resultLength, int numParameters) {
        throw new AnnotationFormatError("Parameter annotations don't match number of parameters");
    }
    
    // Generics infrastructure
    private String getGenericSignature() {
        return signature;
    }
    
    // Accessor for factory
    private GenericsFactory getFactory() {
        // create scope and factory
        return CoreReflectionFactory.make(this, MethodScope.make(this));
    }
    
    /**
     * NOTE that there is no synchronization used here.
     * It is correct (though not efficient) to generate more than one MethodAccessor for a given Method.
     * However, avoiding synchronization will probably make the implementation more scalable.
     */
    // 返回当前方法的访问器
    private MethodAccessor acquireMethodAccessor() {
        // First check to see if one has been created yet, and take it if so
        MethodAccessor tmp = null;
        
        if(root != null) {
            tmp = root.getMethodAccessor();
        }
        
        if(tmp != null) {
            methodAccessor = tmp;
        } else {
            /* Otherwise fabricate one and propagate it up to the root */
            tmp = reflectionFactory.newMethodAccessor(this);
            
            setMethodAccessor(tmp);
        }
        
        return tmp;
    }
}
