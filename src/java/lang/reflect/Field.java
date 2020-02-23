/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Map;
import java.util.Objects;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.FieldAccessor;
import jdk.internal.reflect.Reflection;
import jdk.internal.vm.annotation.ForceInline;
import sun.reflect.annotation.AnnotationParser;
import sun.reflect.annotation.AnnotationSupport;
import sun.reflect.annotation.TypeAnnotation;
import sun.reflect.annotation.TypeAnnotationParser;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.FieldRepository;
import sun.reflect.generics.scope.ClassScope;

/**
 * A {@code Field} provides information about, and dynamic access to, a
 * single field of a class or an interface.  The reflected field may
 * be a class (static) field or an instance field.
 *
 * <p>A {@code Field} permits widening conversions to occur during a get or
 * set access operation, but throws an {@code IllegalArgumentException} if a
 * narrowing conversion would occur.
 *
 * @author Kenneth Russell
 * @author Nakul Saraiya
 * @see Member
 * @see java.lang.Class
 * @see java.lang.Class#getFields()
 * @see java.lang.Class#getField(String)
 * @see java.lang.Class#getDeclaredFields()
 * @see java.lang.Class#getDeclaredField(String)
 * @since 1.1
 */
// 反射元素-字段
public final class Field extends AccessibleObject implements Member {
    
    // Cached field accessor created without override
    private FieldAccessor fieldAccessor;            // 字段访问器缓存，未禁用访问安全检查
    
    // Cached field accessor created with override
    private FieldAccessor overrideFieldAccessor;    // 字段访问器缓存，禁用了访问安全检查
    
    private int slot;       // 当前字段在宿主类字段中的序号
    
    private Class<?> clazz; // 当前字段所在的类
    
    // This is guaranteed to be interned by the VM in the 1.4 reflection implementation
    private String name;    // 字段名称
    private Class<?> type;  // 字段类型
    private int modifiers;  // 字段描述符
    
    // Generics and annotations support
    private transient String signature;
    
    // generic info repository; lazily initialized
    private transient FieldRepository genericInfo;
    
    private byte[] annotations; // 作用在字段上的注解(以字节形式表示，用在反射中)
    
    private transient volatile Map<Class<? extends Annotation>, Annotation> declaredAnnotations;
    
    /**
     * Generics infrastructure
     * For sharing of FieldAccessors.
     * This branching structure is currently only two levels deep (i.e., one root Field and potentially many Field objects pointing to it.)
     * If this branching structure would ever contain cycles, deadlocks can occur in annotation code.
     */
    // 如果当前Field是复制来的，此处保存它的复制源
    private Field root;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Package-private constructor used by ReflectAccess to enable
     * instantiation of these objects in Java code from the java.lang
     * package via sun.reflect.LangReflectAccess.
     */
    Field(Class<?> declaringClass, String name, Class<?> type, int modifiers, int slot, String signature, byte[] annotations) {
        this.clazz = declaringClass;
        this.name = name;
        this.type = type;
        this.modifiers = modifiers;
        this.slot = slot;
        this.signature = signature;
        this.annotations = annotations;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置可访问性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws InaccessibleObjectException {@inheritDoc}
     * @throws SecurityException           {@inheritDoc}
     */
    // 开启/关闭对当前元素的反射访问权限。访问private的Field时必须禁用安全检查，以开启访问权限，即setAccessible(true)
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
    
    
    
    /*▼ 使用字段 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the value of a static or instance {@code boolean} field.
     *
     * @param obj the object to extract the {@code boolean} value
     *            from
     *
     * @return the value of the {@code boolean} field
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is inaccessible.
     * @throws IllegalArgumentException    if the specified object is not
     *                                     an instance of the class or interface declaring the
     *                                     underlying field (or a subclass or implementor
     *                                     thereof), or if the field value cannot be
     *                                     converted to the type {@code boolean} by a
     *                                     widening conversion.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#get
     */
    // 返回obj中当前字段的值，要求当前字段为boolean类型
    @CallerSensitive
    @ForceInline
    public boolean getBoolean(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
        
        return getFieldAccessor(obj).getBoolean(obj);
    }
    
    /**
     * Gets the value of a static or instance field of type
     * {@code char} or of another primitive type convertible to
     * type {@code char} via a widening conversion.
     *
     * @param obj the object to extract the {@code char} value
     *            from
     *
     * @return the value of the field converted to type {@code char}
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is inaccessible.
     * @throws IllegalArgumentException    if the specified object is not
     *                                     an instance of the class or interface declaring the
     *                                     underlying field (or a subclass or implementor
     *                                     thereof), or if the field value cannot be
     *                                     converted to the type {@code char} by a
     *                                     widening conversion.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#get
     */
    // 返回obj中当前字段的值，要求当前字段为char类型
    @CallerSensitive
    @ForceInline
    public char getChar(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
        
        return getFieldAccessor(obj).getChar(obj);
    }
    
    /**
     * Gets the value of a static or instance {@code byte} field.
     *
     * @param obj the object to extract the {@code byte} value
     *            from
     *
     * @return the value of the {@code byte} field
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is inaccessible.
     * @throws IllegalArgumentException    if the specified object is not
     *                                     an instance of the class or interface declaring the
     *                                     underlying field (or a subclass or implementor
     *                                     thereof), or if the field value cannot be
     *                                     converted to the type {@code byte} by a
     *                                     widening conversion.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#get
     */
    // 返回obj中当前字段的值，要求当前字段为byte类型
    @CallerSensitive
    @ForceInline
    public byte getByte(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
    
        return getFieldAccessor(obj).getByte(obj);
    }
    
    /**
     * Gets the value of a static or instance field of type
     * {@code short} or of another primitive type convertible to
     * type {@code short} via a widening conversion.
     *
     * @param obj the object to extract the {@code short} value
     *            from
     *
     * @return the value of the field converted to type {@code short}
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is inaccessible.
     * @throws IllegalArgumentException    if the specified object is not
     *                                     an instance of the class or interface declaring the
     *                                     underlying field (or a subclass or implementor
     *                                     thereof), or if the field value cannot be
     *                                     converted to the type {@code short} by a
     *                                     widening conversion.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#get
     */
    // 返回obj中当前字段的值，要求当前字段为short类型
    @CallerSensitive
    @ForceInline
    public short getShort(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
    
        return getFieldAccessor(obj).getShort(obj);
    }
    
    /**
     * Gets the value of a static or instance field of type
     * {@code int} or of another primitive type convertible to
     * type {@code int} via a widening conversion.
     *
     * @param obj the object to extract the {@code int} value
     *            from
     *
     * @return the value of the field converted to type {@code int}
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is inaccessible.
     * @throws IllegalArgumentException    if the specified object is not
     *                                     an instance of the class or interface declaring the
     *                                     underlying field (or a subclass or implementor
     *                                     thereof), or if the field value cannot be
     *                                     converted to the type {@code int} by a
     *                                     widening conversion.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#get
     */
    // 返回obj中当前字段的值，要求当前字段为int类型
    @CallerSensitive
    @ForceInline
    public int getInt(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
    
        return getFieldAccessor(obj).getInt(obj);
    }
    
    /**
     * Gets the value of a static or instance field of type
     * {@code long} or of another primitive type convertible to
     * type {@code long} via a widening conversion.
     *
     * @param obj the object to extract the {@code long} value
     *            from
     *
     * @return the value of the field converted to type {@code long}
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is inaccessible.
     * @throws IllegalArgumentException    if the specified object is not
     *                                     an instance of the class or interface declaring the
     *                                     underlying field (or a subclass or implementor
     *                                     thereof), or if the field value cannot be
     *                                     converted to the type {@code long} by a
     *                                     widening conversion.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#get
     */
    // 返回obj中当前字段的值，要求当前字段为long类型
    @CallerSensitive
    @ForceInline
    public long getLong(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
    
        return getFieldAccessor(obj).getLong(obj);
    }
    
    /**
     * Gets the value of a static or instance field of type
     * {@code float} or of another primitive type convertible to
     * type {@code float} via a widening conversion.
     *
     * @param obj the object to extract the {@code float} value
     *            from
     *
     * @return the value of the field converted to type {@code float}
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is inaccessible.
     * @throws IllegalArgumentException    if the specified object is not
     *                                     an instance of the class or interface declaring the
     *                                     underlying field (or a subclass or implementor
     *                                     thereof), or if the field value cannot be
     *                                     converted to the type {@code float} by a
     *                                     widening conversion.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#get
     */
    // 返回obj中当前字段的值，要求当前字段为float类型
    @CallerSensitive
    @ForceInline
    public float getFloat(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
    
        return getFieldAccessor(obj).getFloat(obj);
    }
    
    /**
     * Gets the value of a static or instance field of type
     * {@code double} or of another primitive type convertible to
     * type {@code double} via a widening conversion.
     *
     * @param obj the object to extract the {@code double} value
     *            from
     *
     * @return the value of the field converted to type {@code double}
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is inaccessible.
     * @throws IllegalArgumentException    if the specified object is not
     *                                     an instance of the class or interface declaring the
     *                                     underlying field (or a subclass or implementor
     *                                     thereof), or if the field value cannot be
     *                                     converted to the type {@code double} by a
     *                                     widening conversion.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#get
     */
    // 返回obj中当前字段的值，要求当前字段为double类型
    @CallerSensitive
    @ForceInline
    public double getDouble(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
    
        return getFieldAccessor(obj).getDouble(obj);
    }
    
    /**
     * Returns the value of the field represented by this {@code Field}, on
     * the specified object. The value is automatically wrapped in an
     * object if it has a primitive type.
     *
     * <p>The underlying field's value is obtained as follows:
     *
     * <p>If the underlying field is a static field, the {@code obj} argument
     * is ignored; it may be null.
     *
     * <p>Otherwise, the underlying field is an instance field.  If the
     * specified {@code obj} argument is null, the method throws a
     * {@code NullPointerException}. If the specified object is not an
     * instance of the class or interface declaring the underlying
     * field, the method throws an {@code IllegalArgumentException}.
     *
     * <p>If this {@code Field} object is enforcing Java language access control, and
     * the underlying field is inaccessible, the method throws an
     * {@code IllegalAccessException}.
     * If the underlying field is static, the class that declared the
     * field is initialized if it has not already been initialized.
     *
     * <p>Otherwise, the value is retrieved from the underlying instance
     * or static field.  If the field has a primitive type, the value
     * is wrapped in an object before being returned, otherwise it is
     * returned as is.
     *
     * <p>If the field is hidden in the type of {@code obj},
     * the field's value is obtained according to the preceding rules.
     *
     * @param obj object from which the represented field's value is
     *            to be extracted
     *
     * @return the value of the represented field in object
     * {@code obj}; primitive values are wrapped in an appropriate
     * object before being returned
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is inaccessible.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying
     *                                     field (or a subclass or implementor thereof).
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     */
    // 返回obj中当前字段的值，要求当前字段为对象类型
    @CallerSensitive
    @ForceInline
    public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
    
        return getFieldAccessor(obj).get(obj);
    }
    
    
    /**
     * Sets the value of a field as a {@code boolean} on the specified object.
     * This method is equivalent to
     * {@code set(obj, zObj)},
     * where {@code zObj} is a {@code Boolean} object and
     * {@code zObj.booleanValue() == value}.
     *
     * @param obj   the object whose field should be modified
     * @param value the new value for the field of {@code obj} being modified
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is either inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying
     *                                     field (or a subclass or implementor thereof),
     *                                     or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#set
     */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为boolean类型
    @CallerSensitive
    @ForceInline
    public void setBoolean(Object obj, boolean value) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
        
        getFieldAccessor(obj).setBoolean(obj, value);
    }
    
    /**
     * Sets the value of a field as a {@code char} on the specified object.
     * This method is equivalent to
     * {@code set(obj, cObj)},
     * where {@code cObj} is a {@code Character} object and
     * {@code cObj.charValue() == value}.
     *
     * @param obj   the object whose field should be modified
     * @param value the new value for the field of {@code obj} being modified
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is either inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying
     *                                     field (or a subclass or implementor thereof),
     *                                     or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#set
     */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为char类型
    @CallerSensitive
    @ForceInline
    public void setChar(Object obj, char value) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
        
        getFieldAccessor(obj).setChar(obj, value);
    }
    
    /**
     * Sets the value of a field as a {@code byte} on the specified object.
     * This method is equivalent to
     * {@code set(obj, bObj)},
     * where {@code bObj} is a {@code Byte} object and
     * {@code bObj.byteValue() == value}.
     *
     * @param obj   the object whose field should be modified
     * @param value the new value for the field of {@code obj} being modified
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is either inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying
     *                                     field (or a subclass or implementor thereof),
     *                                     or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#set
     */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为byte类型
    @CallerSensitive
    @ForceInline
    public void setByte(Object obj, byte value) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
        
        getFieldAccessor(obj).setByte(obj, value);
    }
    
    /**
     * Sets the value of a field as a {@code short} on the specified object.
     * This method is equivalent to
     * {@code set(obj, sObj)},
     * where {@code sObj} is a {@code Short} object and
     * {@code sObj.shortValue() == value}.
     *
     * @param obj   the object whose field should be modified
     * @param value the new value for the field of {@code obj} being modified
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is either inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying
     *                                     field (or a subclass or implementor thereof),
     *                                     or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#set
     */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为short类型
    @CallerSensitive
    @ForceInline
    public void setShort(Object obj, short value) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
        
        getFieldAccessor(obj).setShort(obj, value);
    }
    
    /**
     * Sets the value of a field as an {@code int} on the specified object.
     * This method is equivalent to
     * {@code set(obj, iObj)},
     * where {@code iObj} is an {@code Integer} object and
     * {@code iObj.intValue() == value}.
     *
     * @param obj   the object whose field should be modified
     * @param value the new value for the field of {@code obj} being modified
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is either inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying
     *                                     field (or a subclass or implementor thereof),
     *                                     or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#set
     */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为int类型
    @CallerSensitive
    @ForceInline
    public void setInt(Object obj, int value) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
        
        getFieldAccessor(obj).setInt(obj, value);
    }
    
    /**
     * Sets the value of a field as a {@code long} on the specified object.
     * This method is equivalent to
     * {@code set(obj, lObj)},
     * where {@code lObj} is a {@code Long} object and
     * {@code lObj.longValue() == value}.
     *
     * @param obj   the object whose field should be modified
     * @param value the new value for the field of {@code obj} being modified
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is either inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying
     *                                     field (or a subclass or implementor thereof),
     *                                     or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#set
     */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为long类型
    @CallerSensitive
    @ForceInline
    public void setLong(Object obj, long value) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
        
        getFieldAccessor(obj).setLong(obj, value);
    }
    
    /**
     * Sets the value of a field as a {@code float} on the specified object.
     * This method is equivalent to
     * {@code set(obj, fObj)},
     * where {@code fObj} is a {@code Float} object and
     * {@code fObj.floatValue() == value}.
     *
     * @param obj   the object whose field should be modified
     * @param value the new value for the field of {@code obj} being modified
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is either inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying
     *                                     field (or a subclass or implementor thereof),
     *                                     or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#set
     */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为float类型
    @CallerSensitive
    @ForceInline
    public void setFloat(Object obj, float value) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
        
        getFieldAccessor(obj).setFloat(obj, value);
    }
    
    /**
     * Sets the value of a field as a {@code double} on the specified object.
     * This method is equivalent to
     * {@code set(obj, dObj)},
     * where {@code dObj} is a {@code Double} object and
     * {@code dObj.doubleValue() == value}.
     *
     * @param obj   the object whose field should be modified
     * @param value the new value for the field of {@code obj} being modified
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is either inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying
     *                                     field (or a subclass or implementor thereof),
     *                                     or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     * @see Field#set
     */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为double类型
    @CallerSensitive
    @ForceInline
    public void setDouble(Object obj, double value) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
        
        getFieldAccessor(obj).setDouble(obj, value);
    }
    
    /**
     * Sets the field represented by this {@code Field} object on the
     * specified object argument to the specified new value. The new
     * value is automatically unwrapped if the underlying field has a
     * primitive type.
     *
     * <p>The operation proceeds as follows:
     *
     * <p>If the underlying field is static, the {@code obj} argument is
     * ignored; it may be null.
     *
     * <p>Otherwise the underlying field is an instance field.  If the
     * specified object argument is null, the method throws a
     * {@code NullPointerException}.  If the specified object argument is not
     * an instance of the class or interface declaring the underlying
     * field, the method throws an {@code IllegalArgumentException}.
     *
     * <p>If this {@code Field} object is enforcing Java language access control, and
     * the underlying field is inaccessible, the method throws an
     * {@code IllegalAccessException}.
     *
     * <p>If the underlying field is final, the method throws an
     * {@code IllegalAccessException} unless {@code setAccessible(true)}
     * has succeeded for this {@code Field} object
     * and the field is non-static. Setting a final field in this way
     * is meaningful only during deserialization or reconstruction of
     * instances of classes with blank final fields, before they are
     * made available for access by other parts of a program. Use in
     * any other context may have unpredictable effects, including cases
     * in which other parts of a program continue to use the original
     * value of this field.
     *
     * <p>If the underlying field is of a primitive type, an unwrapping
     * conversion is attempted to convert the new value to a value of
     * a primitive type.  If this attempt fails, the method throws an
     * {@code IllegalArgumentException}.
     *
     * <p>If, after possible unwrapping, the new value cannot be
     * converted to the type of the underlying field by an identity or
     * widening conversion, the method throws an
     * {@code IllegalArgumentException}.
     *
     * <p>If the underlying field is static, the class that declared the
     * field is initialized if it has not already been initialized.
     *
     * <p>The field is set to the possibly unwrapped and widened new value.
     *
     * <p>If the field is hidden in the type of {@code obj},
     * the field's value is set according to the preceding rules.
     *
     * @param obj   the object whose field should be modified
     * @param value the new value for the field of {@code obj}
     *              being modified
     *
     * @throws IllegalAccessException      if this {@code Field} object
     *                                     is enforcing Java language access control and the underlying
     *                                     field is either inaccessible or final.
     * @throws IllegalArgumentException    if the specified object is not an
     *                                     instance of the class or interface declaring the underlying
     *                                     field (or a subclass or implementor thereof),
     *                                     or if an unwrapping conversion fails.
     * @throws NullPointerException        if the specified object is null
     *                                     and the field is an instance field.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为对象类型
    @CallerSensitive
    @ForceInline
    public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
        if(!override) {
            Class<?> caller = Reflection.getCallerClass();
            checkAccess(caller, obj);
        }
    
        getFieldAccessor(obj).set(obj, value);
    }
    
    /*▲ 使用字段 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 修饰符 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the Java language modifiers for the field represented
     * by this {@code Field} object, as an integer. The {@code Modifier} class should
     * be used to decode the modifiers.
     *
     * @see Modifier
     */
    // 获取方法的修饰符
    public int getModifiers() {
        return modifiers;
    }
    
    /*▲ 修饰符 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字段类型 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a {@code Class} object that identifies the
     * declared type for the field represented by this
     * {@code Field} object.
     *
     * @return a {@code Class} object identifying the declared
     * type of the field represented by this object
     */
    // 获取字段类型[类型擦除]
    public Class<?> getType() {
        return type;
    }
    
    /**
     * Returns a {@code Type} object that represents the declared type for
     * the field represented by this {@code Field} object.
     *
     * <p>If the {@code Type} is a parameterized type, the
     * {@code Type} object returned must accurately reflect the
     * actual type parameters used in the source code.
     *
     * <p>If the type of the underlying field is a type variable or a
     * parameterized type, it is created. Otherwise, it is resolved.
     *
     * @return a {@code Type} object that represents the declared type for
     * the field represented by this {@code Field} object
     *
     * @throws GenericSignatureFormatError         if the generic field
     *                                             signature does not conform to the format specified in
     *                                             <cite>The Java&trade; Virtual Machine Specification</cite>
     * @throws TypeNotPresentException             if the generic type
     *                                             signature of the underlying field refers to a non-existent
     *                                             type declaration
     * @throws MalformedParameterizedTypeException if the generic
     *                                             signature of the underlying field refers to a parameterized type
     *                                             that cannot be instantiated for any reason
     * @since 1.5
     */
    // 获取字段类型[支持泛型语义]
    public Type getGenericType() {
        if(getGenericSignature() != null) {
            return getGenericInfo().getGenericType();
        } else {
            return getType();
        }
    }
    
    /*▲ 字段类型 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字段名称 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the name of the field represented by this {@code Field} object.
     */
    // 返回字段名称，这与字符串化不同
    public String getName() {
        return name;
    }
    
    /*▲ 字段名称 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字符串化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a string describing this {@code Field}.  The format is
     * the access modifiers for the field, if any, followed
     * by the field type, followed by a space, followed by
     * the fully-qualified name of the class declaring the field,
     * followed by a period, followed by the name of the field.
     * For example:
     * <pre>
     *    public static final int java.lang.Thread.MIN_PRIORITY
     *    private int java.io.FileDescriptor.fd
     * </pre>
     *
     * <p>The modifiers are placed in canonical order as specified by
     * "The Java Language Specification".  This is {@code public},
     * {@code protected} or {@code private} first, and then other
     * modifiers in the following order: {@code static}, {@code final},
     * {@code transient}, {@code volatile}.
     *
     * @return a string describing this {@code Field}
     *
     * @jls 8.3.1 Field Modifiers
     */
    // 返回方法的描述，不带泛型信息
    public String toString() {
        int mod = getModifiers();
        return (((mod == 0) ? "" : (Modifier.toString(mod) + " ")) + getType().getTypeName() + " " + getDeclaringClass().getTypeName() + "." + getName());
    }
    
    /**
     * Returns a string describing this {@code Field}, including
     * its generic type.  The format is the access modifiers for the
     * field, if any, followed by the generic field type, followed by
     * a space, followed by the fully-qualified name of the class
     * declaring the field, followed by a period, followed by the name
     * of the field.
     *
     * <p>The modifiers are placed in canonical order as specified by
     * "The Java Language Specification".  This is {@code public},
     * {@code protected} or {@code private} first, and then other
     * modifiers in the following order: {@code static}, {@code final},
     * {@code transient}, {@code volatile}.
     *
     * @return a string describing this {@code Field}, including
     * its generic type
     *
     * @jls 8.3.1 Field Modifiers
     * @since 1.5
     */
    // 返回方法的描述，带着泛型信息
    public String toGenericString() {
        int mod = getModifiers();
        Type fieldType = getGenericType();
        return (((mod == 0) ? "" : (Modifier.toString(mod) + " ")) + fieldType.getTypeName() + " " + getDeclaringClass().getTypeName() + "." + getName());
    }
    
    /*▲ 字符串化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注解 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
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
     * @since 1.8
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
        return AnnotationParser.toArray(declaredAnnotations());
    }
    
    /**
     * Returns an AnnotatedType object that represents the use of a type to specify
     * the declared type of the field represented by this Field.
     *
     * @return an object representing the declared type of the field
     * represented by this Field
     *
     * @since 1.8
     */
    // 获取字段类型处的【被注解类型】
    public AnnotatedType getAnnotatedType() {
        return TypeAnnotationParser.buildAnnotatedType(getTypeAnnotationBytes0(), SharedSecrets.getJavaLangAccess().
            getConstantPool(getDeclaringClass()), this, getDeclaringClass(), getGenericType(), TypeAnnotation.TypeAnnotationTarget.FIELD);
    }
    
    /*▲ 注解 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字段特征 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the {@code Class} object representing the class or interface
     * that declares the field represented by this {@code Field} object.
     */
    // 返回字段所在的类
    @Override
    public Class<?> getDeclaringClass() {
        return clazz;
    }
    
    /**
     * Returns {@code true} if this field is a synthetic
     * field; returns {@code false} otherwise.
     *
     * @return true if and only if this field is a synthetic
     * field as defined by the Java Language Specification.
     *
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
     *     class Inner{
     *     }
     * }
     *
     * 打印内部类Inner的字段时，会发现一个特殊的字段：
     *
     * final com.kang.Outer com.kang.Outer$Inner.this$0
     *
     * 这个名为this$0的字段持有对外部类对象的一个引用，其isSynthetic()==true
     */
    public boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }
    
    /**
     * Returns {@code true} if this field represents an element of
     * an enumerated type; returns {@code false} otherwise.
     *
     * @return {@code true} if and only if this field represents an element of
     * an enumerated type.
     *
     * @since 1.5
     */
    // 是否为枚举常量
    public boolean isEnumConstant() {
        return (getModifiers() & Modifier.ENUM) != 0;
    }
    
    /*▲ 字段特征 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Compares this {@code Field} against the specified object.  Returns
     * true if the objects are the same.  Two {@code Field} objects are the same if
     * they were declared by the same class and have the same name
     * and type.
     */
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof Field) {
            Field other = (Field) obj;
            return (getDeclaringClass() == other.getDeclaringClass()) && (getName() == other.getName()) && (getType() == other.getType());
        }
        return false;
    }
    
    /**
     * Returns a hashcode for this {@code Field}.  This is computed as the
     * exclusive-or of the hashcodes for the underlying field's
     * declaring class name and its name.
     */
    public int hashCode() {
        return getDeclaringClass().getName().hashCode() ^ getName().hashCode();
    }
    
    
    
    
    
    
    /**
     * Package-private routine (exposed to java.lang.Class via
     * ReflectAccess) which returns a copy of this Field. The copy's
     * "root" field points to this Field.
     */
    Field copy() {
        // This routine enables sharing of FieldAccessor objects
        // among Field objects which refer to the same underlying
        // method in the VM. (All of this contortion is only necessary
        // because of the "accessibility" bit in AccessibleObject,
        // which implicitly requires that new java.lang.reflect
        // objects be fabricated for each reflective call on Class
        // objects.)
        if(this.root != null) {
            throw new IllegalArgumentException("Can not copy a non-root Field");
        }
        
        Field res = new Field(clazz, name, type, modifiers, slot, signature, annotations);
    
        res.root = this;
    
        // Might as well eagerly propagate this if already present
        res.fieldAccessor = fieldAccessor;
        res.overrideFieldAccessor = overrideFieldAccessor;
        
        return res;
    }
    
    // 判断caller是否可以访问当前元素(涉及到exports和opens的判断)
    @Override
    void checkCanSetAccessible(Class<?> caller) {
        checkCanSetAccessible(caller, clazz);
    }
    
    @Override
    String toShortString() {
        return "field " + getDeclaringClass().getTypeName() + "." + getName();
    }
    
    @Override
    Field getRoot() {
        return root;
    }
    
    private String getGenericSignature() {
        return signature;
    }
    
    /** Accessor for factory */
    private GenericsFactory getFactory() {
        Class<?> c = getDeclaringClass();
        // create scope and factory
        return CoreReflectionFactory.make(c, ClassScope.make(c));
    }
    
    /** Accessor for generic info repository */
    private FieldRepository getGenericInfo() {
        // lazily initialize repository if necessary
        if(genericInfo == null) {
            // create and cache generic info repository
            genericInfo = FieldRepository.make(getGenericSignature(), getFactory());
        }
        
        return genericInfo; //return cached repository
    }
    
    /** check access to field */
    // 检查当前字段的可访问性
    private void checkAccess(Class<?> caller, Object obj) throws IllegalAccessException {
        checkAccess(caller, clazz, Modifier.isStatic(modifiers) ? null : obj.getClass(), modifiers);
    }
    
    /** security check is done before calling this method */
    // 返回当前字段的访问器
    private FieldAccessor getFieldAccessor(Object obj) throws IllegalAccessException {
        FieldAccessor accessor = (override) ? overrideFieldAccessor : fieldAccessor;
        return (accessor != null) ? accessor : acquireFieldAccessor(override);
    }
    
    /**
     * NOTE that there is no synchronization used here.
     * It is correct (though not efficient) to generate more than one FieldAccessor for a given Field.
     * However, avoiding synchronization will probably make the implementation more scalable.
     */
    // 返回当前字段的访问器
    private FieldAccessor acquireFieldAccessor(boolean overrideFinalCheck) {
        // First check to see if one has been created yet, and take it if so
        FieldAccessor accessor = null;
    
        // 尝试从复制源中获取字段访问器
        if(root != null) {
            // 从缓存中获取当前字段的访问器
            accessor = root.getFieldAccessor(overrideFinalCheck);
        }
    
        // 对字段访问器进行缓存
        if(accessor != null) {
            if(overrideFinalCheck) {
                overrideFieldAccessor = accessor;    // 设置字段访问器缓存，禁用了访问安全检查
            } else {
                fieldAccessor = accessor;            // 设置字段访问器缓存，未禁用访问安全检查
            }
        } else {
            /* Otherwise fabricate one and propagate it up to the root*/
            // 构造并返回字段field的访问器，overrideFinalCheck指示字段field的访问安全检查是否被禁用
            accessor = reflectionFactory.newFieldAccessor(this, overrideFinalCheck);
        
            // 设置(缓存)当前字段的访问器
            setFieldAccessor(accessor, overrideFinalCheck);
        }
    
        return accessor;
    }
    
    /** Returns FieldAccessor for this Field object, not looking up the chain to the root */
    // 从缓存中获取当前字段的访问器
    private FieldAccessor getFieldAccessor(boolean overrideFinalCheck) {
        return (overrideFinalCheck) ? overrideFieldAccessor : fieldAccessor;
    }
    
    /** Sets the FieldAccessor for this Field object and (recursively) its root */
    // 设置(缓存)当前字段的访问器
    private void setFieldAccessor(FieldAccessor accessor, boolean overrideFinalCheck) {
        if(overrideFinalCheck) {
            overrideFieldAccessor = accessor;
        } else {
            fieldAccessor = accessor;
        }
        
        // Propagate up
        if(root != null) {
            root.setFieldAccessor(accessor, overrideFinalCheck);
        }
    }
    
    private Map<Class<? extends Annotation>, Annotation> declaredAnnotations() {
        Map<Class<? extends Annotation>, Annotation> declAnnos;
        if((declAnnos = declaredAnnotations) == null) {
            synchronized(this) {
                if((declAnnos = declaredAnnotations) == null) {
                    Field root = this.root;
                    if(root != null) {
                        declAnnos = root.declaredAnnotations();
                    } else {
                        declAnnos = AnnotationParser.parseAnnotations(annotations, SharedSecrets.getJavaLangAccess().getConstantPool(getDeclaringClass()), getDeclaringClass());
                    }
                    declaredAnnotations = declAnnos;
                }
            }
        }
    
        return declAnnos;
    }
    
    private native byte[] getTypeAnnotationBytes0();
    
}
