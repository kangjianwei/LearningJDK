/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.reflect;

import java.io.Externalizable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Properties;
import jdk.internal.misc.VM;
import sun.reflect.misc.ReflectUtil;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;

/**
 * The master factory for all reflective objects, both those in java.lang.reflect (Fields, Methods, Constructors)
 * as well as their delegates (FieldAccessors, MethodAccessors, ConstructorAccessors).
 *
 * The methods in this class are extremely unsafe and can cause subversion of both the language and the verifier.
 * For this reason, they are all instance methods, and access to the constructor of
 * this factory is guarded by a security check, in similar style to {@link jdk.internal.misc.Unsafe}.
 */
// 反射对象工厂
public class ReflectionFactory {
    
    private static final ReflectionFactory soleInstance = new ReflectionFactory();
    
    /** Provides access to package-private mechanisms in java.lang.reflect */
    private static volatile LangReflectAccess langReflectAccess;    // 反射工具类
    
    private static boolean initted = false; // 是否检查过初始化
    
    /** Method for static class initializer <clinit>, or null */
    private static volatile Method hasStaticInitializerMethod;
    
    /**
     * "Inflation" mechanism.
     * Loading bytecodes to implement Method.invoke() and Constructor.newInstance() currently costs 3-4x
     * more than an invocation via native code for the first invocation
     * (though subsequent invocations have been benchmarked to be over 20x faster).
     * Unfortunately this cost increases startup time for certain applications that use reflection
     * intensively (but only once per class) to bootstrap themselves.
     * To avoid this penalty we reuse the existing JVM entry points for the first few invocations
     * of Methods and Constructors and then switch to the bytecode-based implementations.
     * Package-private to be accessible to NativeMethodAccessorImpl and NativeConstructorAccessorImpl
     */
    /*
     * 来自：https://blogs.oracle.com/buck/inflation-system-properties
     *
     * Java反射有两种方法来调用类的方法或构造器：JNI或纯Java。
     * JNI的执行速度很慢(主要是因为从Java到JNI以及从JNI到Java的过渡开销)，但是它的初始化成本为零，因为我们不需要生成任何东西，通用访问器的实现中已经内置。
     * 纯Java的解决方案执行速度更快(没有JNI开销)，但是初始化成本很高，因为我们需要在运行前为每个需要调用的方法生成自定义字节码。
     * (简单讲：JNI方案执行慢，初始化快；纯Java方案执行快，初始化慢)
     * 因此，理想情况下，我们只希望为将被调用次数多的方法生成纯Java实现(因为这样可以分摊初始化成本)。
     * 而"Inflation"技术就是Java运行时尝试达到此目标的技术。
     *
     * 默认情况下，"Inflation"技术是开启的。
     * 这样一来，反射操作会在前期先使用JNI调用，但后续会为调用次数超过某个阈值的访问器生成纯Java版本。
     *
     * 如果关闭了"Inflation"技术，则跳过前面的JNI调用，在首次反射调用时即生成纯Java版本的访问器。
     */
    private static boolean noInflation = false; // 是否关闭了"Inflation"技术。
    private static int inflationThreshold = 15; // JNI调用阈值，当某个方法反射调用超过这个阈值时，会为其生成纯Java的版本访问器。
    
    // true if deserialization constructor checking is disabled
    private static boolean disableSerialConstructorChecks = false;  // 是否禁用序列化构造器的检查
    
    
    private ReflectionFactory() {
    }
    
    
    /**
     * Provides the caller with the capability to instantiate reflective
     * objects.
     *
     * <p> First, if there is a security manager, its
     * <code>checkPermission</code> method is called with a {@link
     * java.lang.RuntimePermission} with target
     * <code>"reflectionFactoryAccess"</code>.  This may result in a
     * security exception.
     *
     * <p> The returned <code>ReflectionFactory</code> object should be
     * carefully guarded by the caller, since it can be used to read and
     * write private data and invoke private methods, as well as to load
     * unverified bytecodes.  It must never be passed to untrusted code.
     *
     * @exception SecurityException if a security manager exists and its
     *             <code>checkPermission</code> method doesn't allow
     *             access to the RuntimePermission "reflectionFactoryAccess".  */
    // 返回ReflectionFactory实例
    public static ReflectionFactory getReflectionFactory() {
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkPermission(SecurityConstants.REFLECTION_FACTORY_ACCESS_PERMISSION);
        }
    
        return soleInstance;
    }
    
    /**
     * Returns an alternate reflective Method instance for the given method intended for reflection to invoke, if present.
     *
     * A trusted method can define an alternate implementation for a method `foo`
     * by defining a method named "reflected$foo" that will be invoked reflectively.
     */
    // 对于"调用者敏感"的方法，查找其一个替代方法(以reflected$开头)，如果找不到，返回null
    private static Method findMethodForReflection(Method method) {
        String altName = "reflected$" + method.getName();
    
        try {
            // 获取方法所在的类
            Class<?> declaringClass = method.getDeclaringClass();
        
            // 返回declaringClass类中指定名称和形参的方法，但不包括父类/父接口中的方法
            return declaringClass.getDeclaredMethod(altName, method.getParameterTypes());
        } catch(NoSuchMethodException ex) {
            return null;
        }
    }
    
    /** Called only by java.lang.reflect.Modifier's static initializer */
    // 设置指定的反射工具类，仅在Modifier静态初始化块中被初始化
    public void setLangReflectAccess(LangReflectAccess access) {
        langReflectAccess = access;
    }
    
    /**
     * Note: this routine can cause the declaring class for the field
     * be initialized and therefore must not be called until the
     * first get/set of this field.
     *
     * @param field    the field
     * @param override true if caller has overridden accessibility
     */
    // 构造并返回字段field的访问器，override指示字段field的访问安全检查是否被禁用
    public FieldAccessor newFieldAccessor(Field field, boolean override) {
        // 检查初始化，只检查一次
        checkInitted();
    
        Field root = langReflectAccess.getRoot(field);
        if(root != null) {
            // FieldAccessor will use the root unless the modifiers have been overrridden
            if(root.getModifiers() == field.getModifiers() || !override) {
                field = root;
            }
        }
    
        return UnsafeFieldAccessorFactory.newFieldAccessor(field, override);
    }
    
    // 返回指定方法的访问器
    public MethodAccessor newMethodAccessor(Method method) {
        // 检查初始化，只检查一次
        checkInitted();
        
        // 如果指定的方法是"调用者敏感"的(带有CallerSensitive注解)
        if(Reflection.isCallerSensitive(method)) {
            // 对于"调用者敏感"的方法，查找其一个替代方法(以reflected$开头)，如果找不到，返回null
            Method altMethod = findMethodForReflection(method);
            if(altMethod != null) {
                method = altMethod;
            }
        }
        
        /* use the root Method that will not cache caller class */
        // 如果指定的method是复制来的，则获取它的复制源
        Method root = langReflectAccess.getRoot(method);
        if(root != null) {
            method = root;
        }
        
        // 获取method所在的类
        Class<?> declaringClass = method.getDeclaringClass();
        
        // 如果关闭了"Inflation"技术，且declaringClass不是虚拟机匿名类
        if(noInflation && !ReflectUtil.isVMAnonymousClass(declaringClass)) {
            // 构造基于纯Java的方法访问器，以便直接使用纯Java的方式进行反射操作
            MethodAccessorGenerator accessor = new MethodAccessorGenerator();
            return accessor.generateMethod(declaringClass, method.getName(), method.getParameterTypes(), method.getReturnType(), method.getExceptionTypes(), method.getModifiers());
            
            // 如果开启"Inflation"技术
        } else {
            // 构造基于JNI的方法调用器，先尝试用基于JNI的方式进行反射操作
            NativeMethodAccessorImpl acc = new NativeMethodAccessorImpl(method);
            DelegatingMethodAccessorImpl res = new DelegatingMethodAccessorImpl(acc);
            acc.setParent(res);
            return res;
        }
    }
    
    // 返回指定构造器的访问器
    public ConstructorAccessor newConstructorAccessor(Constructor<?> constructor) {
        // 检查初始化，只检查一次
        checkInitted();
        
        // 返回构造器所在的类
        Class<?> declaringClass = constructor.getDeclaringClass();
        
        // 如果需要创建的是抽象类，则禁止通过反射实例化对象
        if(Modifier.isAbstract(declaringClass.getModifiers())) {
            // 适用于抽象类的构造器访问器：当尝试调用newInstance()方法构造对象时，会抛出异常
            return new InstantiationExceptionConstructorAccessorImpl(null);
        }
        
        // 如果需要创建的是Class类，则禁止通过反射实例化对象
        if(declaringClass == Class.class) {
            return new InstantiationExceptionConstructorAccessorImpl("Can not instantiate java.lang.Class");
        }
        
        /* use the root Constructor that will not cache caller class */
        // 如果指定的constructor是复制来的，则获取它的复制源
        Constructor<?> root = langReflectAccess.getRoot(constructor);
        if(root != null) {
            constructor = root;
        }
        
        /*
         * Bootstrapping issue:
         * since we use Class.newInstance() in the ConstructorAccessor generation process,
         * we have to break the cycle here.
         */
        // 如果declaringClass类是否与ConstructorAccessorImpl类相同，或为ConstructorAccessorImpl类的子类，则需要防止构造器产生无限递归调用
        if(Reflection.isSubclassOf(declaringClass, ConstructorAccessorImpl.class)) {
            return new BootstrapConstructorAccessorImpl(constructor);
        }
        
        // 构造器可能已经发生了改变
        declaringClass = constructor.getDeclaringClass();
        
        // 如果关闭了"Inflation"技术，且declaringClass不是虚拟机匿名类
        if(noInflation && !ReflectUtil.isVMAnonymousClass(declaringClass)) {
            // 构造基于纯Java的构造器访问器，以便直接使用纯Java的方式进行反射操作
            MethodAccessorGenerator accessor = new MethodAccessorGenerator();
            return accessor.generateConstructor(declaringClass, constructor.getParameterTypes(), constructor.getExceptionTypes(), constructor.getModifiers());
            
            // 如果开启"Inflation"技术
        } else {
            // 构造基于JNI的构造器调用器，先尝试用基于JNI的方式进行反射操作
            NativeConstructorAccessorImpl acc = new NativeConstructorAccessorImpl(constructor);
            DelegatingConstructorAccessorImpl res = new DelegatingConstructorAccessorImpl(acc);
            acc.setParent(res);
            return res;
        }
    }
    
    
    
    /*▼ 字段 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new java.lang.reflect.Field.
     * Access checks as per java.lang.reflect.AccessibleObject are not overridden.
     */
    // 构造并返回一个"字段"对象
    public Field newField(Class<?> declaringClass, String name, Class<?> type, int modifiers, int slot, String signature, byte[] annotations) {
        return langReflectAccess().newField(declaringClass, name, type, modifiers, slot, signature, annotations);
    }
    
    /**
     * Makes a copy of the passed field.
     * The returned field is a "child" of the passed one; see the comments in Field.java for details.
     */
    // 字段对象拷贝
    public Field copyField(Field field) {
        return langReflectAccess().copyField(field);
    }
    
    /*▲ 字段 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new java.lang.reflect.Constructor.
     * Access checks as per java.lang.reflect.AccessibleObject are not overridden.
     */
    // 构造并返回一个"构造器"对象
    public Constructor<?> newConstructor(Class<?> declaringClass, Class<?>[] parameterTypes, Class<?>[] checkedExceptions, int modifiers, int slot, String signature, byte[] annotations, byte[] parameterAnnotations) {
        return langReflectAccess().newConstructor(declaringClass, parameterTypes, checkedExceptions, modifiers, slot, signature, annotations, parameterAnnotations);
    }
    
    /**
     * Gets the ConstructorAccessor object for a java.lang.reflect.Constructor
     */
    // 返回指定构造器的访问器
    public ConstructorAccessor getConstructorAccessor(Constructor<?> constructor) {
        return langReflectAccess().getConstructorAccessor(constructor);
    }
    
    /**
     * Sets the ConstructorAccessor object for a java.lang.reflect.Constructor
     */
    // 为指定的构造器设置访问器
    public void setConstructorAccessor(Constructor<?> constructor, ConstructorAccessor accessor) {
        langReflectAccess().setConstructorAccessor(constructor, accessor);
    }
    
    /**
     * Makes a copy of the passed constructor. The returned
     * constructor is a "child" of the passed one; see the comments
     * in Constructor.java for details.
     */
    // 构造器拷贝
    public <T> Constructor<T> copyConstructor(Constructor<T> arg) {
        return langReflectAccess().copyConstructor(arg);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new java.lang.reflect.Method.
     * Access checks as per java.lang.reflect.AccessibleObject are not overridden.
     */
    // 构造并返回一个"方法"对象
    public Method newMethod(Class<?> declaringClass, String name, Class<?>[] parameterTypes, Class<?> returnType, Class<?>[] checkedExceptions, int modifiers, int slot, String signature, byte[] annotations, byte[] parameterAnnotations, byte[] annotationDefault) {
        return langReflectAccess().newMethod(declaringClass, name, parameterTypes, returnType, checkedExceptions, modifiers, slot, signature, annotations, parameterAnnotations, annotationDefault);
    }
    
    /** Gets the MethodAccessor object for a java.lang.reflect.Method */
    // 返回指定方法的访问器
    public MethodAccessor getMethodAccessor(Method method) {
        return langReflectAccess().getMethodAccessor(method);
    }
    
    /** Sets the MethodAccessor object for a java.lang.reflect.Method */
    // 为指定的方法设置访问器
    public void setMethodAccessor(Method method, MethodAccessor accessor) {
        langReflectAccess().setMethodAccessor(method, accessor);
    }
    
    /**
     * Makes a copy of the passed method. The returned method is a
     * "child" of the passed one; see the comments in Method.java for
     * details.
     */
    // 方法拷贝，参数中的方法是返回值的复制源
    public Method copyMethod(Method method) {
        return langReflectAccess().copyMethod(method);
    }
    
    /**
     * Makes a copy of the passed method. The returned method is NOT
     * a "child" but a "sibling" of the Method in arg. Should only be
     * used on non-root methods.
     */
    // 方法拷贝
    public Method leafCopyMethod(Method method) {
        return langReflectAccess().leafCopyMethod(method);
    }
    
    /*▲ 方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 可执行元素 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回可执行元素的形参列表
    public Class<?>[] getExecutableSharedParameterTypes(Executable ex) {
        return langReflectAccess().getExecutableSharedParameterTypes(ex);
    }
    
    /**
     * Gets the byte[] that encodes TypeAnnotations on an executable.
     */
    public byte[] getExecutableTypeAnnotationBytes(Executable ex) {
        return langReflectAccess().getExecutableTypeAnnotationBytes(ex);
    }
    
    /*▲ 可执行元素 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 用在序列化中 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public final MethodHandle readObjectForSerialization(Class<?> cl) {
        return findReadWriteObjectForSerialization(cl, "readObject", ObjectInputStream.class);
    }
    
    public final MethodHandle readObjectNoDataForSerialization(Class<?> cl) {
        return findReadWriteObjectForSerialization(cl, "readObjectNoData", ObjectInputStream.class);
    }
    
    public final MethodHandle writeObjectForSerialization(Class<?> cl) {
        return findReadWriteObjectForSerialization(cl, "writeObject", ObjectOutputStream.class);
    }
    
    /**
     * Returns a MethodHandle for {@code writeReplace} on the serializable class
     * or null if no match found.
     *
     * @param cl a serializable class
     *
     * @return the {@code writeReplace} MethodHandle or {@code null} if not found
     */
    public final MethodHandle writeReplaceForSerialization(Class<?> cl) {
        return getReplaceResolveForSerialization(cl, "writeReplace");
    }
    
    /**
     * Returns a MethodHandle for {@code readResolve} on the serializable class
     * or null if no match found.
     *
     * @param cl a serializable class
     *
     * @return the {@code writeReplace} MethodHandle or {@code null} if not found
     */
    public final MethodHandle readResolveForSerialization(Class<?> cl) {
        return getReplaceResolveForSerialization(cl, "readResolve");
    }
    
    /**
     * Returns true if the given class defines a static initializer method,
     * false otherwise.
     */
    public final boolean hasStaticInitializerForSerialization(Class<?> cl) {
        Method m = hasStaticInitializerMethod;
        if(m == null) {
            try {
                m = ObjectStreamClass.class.getDeclaredMethod("hasStaticInitializer", Class.class);
                m.setAccessible(true);
                hasStaticInitializerMethod = m;
            } catch(NoSuchMethodException ex) {
                throw new InternalError("No such method hasStaticInitializer on " + ObjectStreamClass.class, ex);
            }
        }
        try {
            return (Boolean) m.invoke(null, cl);
        } catch(InvocationTargetException | IllegalAccessException ex) {
            throw new InternalError("Exception invoking hasStaticInitializer", ex);
        }
    }
    
    /**
     * Return the accessible constructor for OptionalDataException signaling eof.
     *
     * @return the eof constructor for OptionalDataException
     */
    public final Constructor<OptionalDataException> newOptionalDataExceptionForSerialization() {
        try {
            // 返回OptionalDataException类中指定形参的构造器，但不包括父类中的构造器
            Constructor<OptionalDataException> boolCtor = OptionalDataException.class.getDeclaredConstructor(Boolean.TYPE);
            boolCtor.setAccessible(true);
            return boolCtor;
        } catch(NoSuchMethodException ex) {
            throw new InternalError("Constructor not found", ex);
        }
    }
    
    // 如果clazz是Externalizable类型，返回其构造器
    public final Constructor<?> newConstructorForExternalization(Class<?> clazz) {
        if(!Externalizable.class.isAssignableFrom(clazz)) {
            return null;
        }
        
        try {
            Constructor<?> cons = clazz.getConstructor();
            cons.setAccessible(true);
            return cons;
        } catch(NoSuchMethodException ex) {
            return null;
        }
    }
    
    // 返回可供clazz使用的构造器
    public final Constructor<?> newConstructorForSerialization(Class<?> clazz, Constructor<?> constructorToCall) {
        // 如果constructorToCall所在的类就是clazz，则可以直接返回
        if(constructorToCall.getDeclaringClass() == clazz) {
            constructorToCall.setAccessible(true);
            return constructorToCall;
        }
        
        // 基于constructorToCall，生成一个可供clazz使用的构造器后返回
        return generateConstructor(clazz, constructorToCall);
    }
    
    /**
     * Returns a constructor that allocates an instance of cl and that then initializes
     * the instance by calling the no-arg constructor of its first non-serializable
     * superclass.
     * This is specified in the Serialization Specification, section 3.1,
     * in step 11 of the deserialization process.
     * If cl is not serializable, returns cl's no-arg constructor.
     * If no accessible constructor is found, or if the class hierarchy is somehow malformed
     * (e.g., a serializable class has no superclass), null is returned.
     *
     * @param clazz the class for which a constructor is to be found
     *
     * @return the generated constructor, or null if none is available
     */
    /*
     * 返回clazz的第一个不可序列化的父类的无参构造器，要求该无参构造器可被clazz访问。
     * 如果未找到该构造器，或该构造器子类无法访问，则返回null。
     */
    public final Constructor<?> newConstructorForSerialization(Class<?> clazz) {
        Class<?> initCl = clazz;
        
        // 如果initCl是Serializable的实现类，则向上查找其首个不是Serializable实现类的父类
        while(Serializable.class.isAssignableFrom(initCl)) {
            Class<?> prev = initCl;
            
            if((initCl = initCl.getSuperclass()) == null    // 如果不存在父类(如接口)，直接返回null
                // 如果没有禁用构造器检查，则应当判断cl是否可以访问其父类的某个构造器
                || (!disableSerialConstructorChecks && !superHasAccessibleConstructor(prev))) {
                return null;
            }
        }
        
        Constructor<?> constructorToCall;
        try {
            // 获取initCl中的无参构造器
            constructorToCall = initCl.getDeclaredConstructor();
            // 获取该构造器的修饰符
            int mods = constructorToCall.getModifiers();
            // 如果构造器私有，或者构造器为包访问权限，但cl和initCl不在同一个包，此时返回null，即无法获取到可用构造器
            if((mods & Modifier.PRIVATE) != 0 || ((mods & (Modifier.PUBLIC | Modifier.PROTECTED)) == 0 && !packageEquals(clazz, initCl))) {
                return null;
            }
        } catch(NoSuchMethodException ex) {
            return null;
        }
        
        // 基于constructorToCall，生成一个可供clazz使用的构造器后返回
        return generateConstructor(clazz, constructorToCall);
    }
    
    /*▲ 用在序列化中 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Given a class, determines whether its superclass has
     * any constructors that are accessible from the class.
     * This is a special purpose method intended to do access
     * checking for a serializable class and its superclasses
     * up to, but not including, the first non-serializable
     * superclass. This also implies that the superclass is
     * always non-null, because a serializable class must be a
     * class (not an interface) and Object is not serializable.
     *
     * @param cl the class from which access is checked
     *
     * @return whether the superclass has a constructor accessible from cl
     */
    // 判断cl是否可以访问其父类的某个构造器
    private boolean superHasAccessibleConstructor(Class<?> cl) {
        // 获取cl的父类（只识别非泛型类型）
        Class<?> superCl = cl.getSuperclass();
    
        assert Serializable.class.isAssignableFrom(cl);
        assert superCl != null;
    
        // 如果两个类位于同一个包
        if(packageEquals(cl, superCl)) {
            /* accessible if any non-private constructor is found */
            // 遍历superCl中所有构造器
            for(Constructor<?> ctor : superCl.getDeclaredConstructors()) {
                // 如果存在任一非私有的构造器，返回true
                if((ctor.getModifiers() & Modifier.PRIVATE) == 0) {
                    return true;
                }
            }
        
            // 判断两个类是否相同或互为嵌套(内部类)关系（不分谁嵌套谁）
            return Reflection.areNestMates(cl, superCl);
        
            // 如果两个类不再同一个包下
        } else {
            // sanity check to ensure the parent is protected or public */
            // 确保父类superCl为public或protected
            if((superCl.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC)) == 0) {
                return false;
            }
        
            /* accessible if any constructor is protected or public */
            // 遍历superCl中所有构造器
            for(Constructor<?> ctor : superCl.getDeclaredConstructors()) {
                // 如果存在任一非protected或public的构造器，返回true
                if((ctor.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC)) != 0) {
                    return true;
                }
            }
        
            return false;
        }
    }
    
    // 返回某个方法被基于JNI的反射调用的次数
    static int inflationThreshold() {
        return inflationThreshold;
    }
    
    /**
     * We have to defer full initialization of this class until after the static initializer is run since java.lang.reflect.Method's static initializer
     * (more properly, that for java.lang.reflect.AccessibleObject) causes this class's to be run, before the system properties are set up.
     */
    // 检查初始化，只检查一次
    private static void checkInitted() {
        // 如果已经检查过初始化，直接返回
        if(initted) {
            return;
        }
        
        /*
         * Defer initialization until module system is initialized
         * so as to avoid inflation and spinning bytecode in unnamed modules during early startup.
         */
        // 如果模块系统还未初始化(VM初始化第二阶段还未完成)
        if(!VM.isModuleSystemInited()) {
            return;
        }
        
        Properties props = GetPropertyAction.privilegedGetProperties();
        
        // 判断是否关闭"Inflation"技术，默认是开启的，反射操作会有一个从JNI调用过渡到纯Java调用的过程
        String val = props.getProperty("sun.reflect.noInflation");
        if(val != null && val.equals("true")) {
            noInflation = true; // 关闭"Inflation"技术
        }
        
        // 尝试更新JNI调用阈值
        val = props.getProperty("sun.reflect.inflationThreshold");
        if(val != null) {
            try {
                inflationThreshold = Integer.parseInt(val);
            } catch(NumberFormatException e) {
                throw new RuntimeException("Unable to parse property sun.reflect.inflationThreshold", e);
            }
        }
        
        // 是否禁用序列化构造器的检查
        disableSerialConstructorChecks = "true".equals(props.getProperty("jdk.disableSerialConstructorChecks"));
        
        // 已进行初始化检查
        initted = true;
    }
    
    // 返回反射对象访问工具
    private static LangReflectAccess langReflectAccess() {
        if(langReflectAccess == null) {
            /*
             * Call a static method to get class java.lang.reflect.Modifier initialized.
             * Its static initializer will cause setLangReflectAccess() to be called from the context of the java.lang.reflect package.
             */
            // 加载Modifier方法内的静态初始化块，使langReflectAccess对象被初始化
            Modifier.isPublic(Modifier.PUBLIC);
        }
        
        return langReflectAccess;
    }
    
    /**
     * Returns true if classes are defined in the classloader and same package, false
     * otherwise.
     *
     * @param cl1 a class
     * @param cl2 another class
     *
     * @return true if the two classes are in the same classloader and package
     */
    // 判断两个类是否位于同一个包
    private static boolean packageEquals(Class<?> cl1, Class<?> cl2) {
        assert !cl1.isArray() && !cl2.isArray();
        
        if(cl1 == cl2) {
            return true;
        }
        
        // 类加载器一致，包名一致
        return cl1.getClassLoader() == cl2.getClassLoader() && Objects.equals(cl1.getPackageName(), cl2.getPackageName());
    }
    
    // 基于constructorToCall，生成一个可供clazz使用的构造器后返回
    private final Constructor<?> generateConstructor(Class<?> clazz, Constructor<?> constructorToCall) {
        
        MethodAccessorGenerator methodAccessorGenerator = new MethodAccessorGenerator();
        
        // 返回构造器访问器，该构造器所在的类需要支持序列化
        ConstructorAccessor acc = methodAccessorGenerator.generateSerializationConstructor(clazz, constructorToCall.getParameterTypes(), constructorToCall.getExceptionTypes(), constructorToCall.getModifiers(), constructorToCall.getDeclaringClass());
        
        // 构造并返回一个"构造器"对象
        Constructor<?> c = newConstructor(constructorToCall.getDeclaringClass(), constructorToCall.getParameterTypes(), constructorToCall.getExceptionTypes(), constructorToCall.getModifiers(), langReflectAccess().getConstructorSlot(constructorToCall), langReflectAccess().getConstructorSignature(constructorToCall), langReflectAccess().getConstructorAnnotations(constructorToCall), langReflectAccess().getConstructorParameterAnnotations(constructorToCall));
        
        setConstructorAccessor(c, acc);
        
        c.setAccessible(true);
        
        return c;
    }
    
    private final MethodHandle findReadWriteObjectForSerialization(Class<?> cl, String methodName, Class<?> streamClass) {
        if(!Serializable.class.isAssignableFrom(cl)) {
            return null;
        }
        
        try {
            Method meth = cl.getDeclaredMethod(methodName, streamClass);
            int mods = meth.getModifiers();
            if(meth.getReturnType() != Void.TYPE || Modifier.isStatic(mods) || !Modifier.isPrivate(mods)) {
                return null;
            }
            meth.setAccessible(true);
            return MethodHandles.lookup().unreflect(meth);
        } catch(NoSuchMethodException ex) {
            return null;
        } catch(IllegalAccessException ex1) {
            throw new InternalError("Error", ex1);
        }
    }
    
    /**
     * Lookup readResolve or writeReplace on a class with specified
     * signature constraints.
     *
     * @param cl         a serializable class
     * @param methodName the method name to find
     *
     * @return a MethodHandle for the method or {@code null} if not found or has the wrong signature.
     */
    private MethodHandle getReplaceResolveForSerialization(Class<?> cl, String methodName) {
        if(!Serializable.class.isAssignableFrom(cl)) {
            return null;
        }
        
        Class<?> defCl = cl;
        while(defCl != null) {
            try {
                Method m = defCl.getDeclaredMethod(methodName);
                if(m.getReturnType() != Object.class) {
                    return null;
                }
                int mods = m.getModifiers();
                if(Modifier.isStatic(mods) | Modifier.isAbstract(mods)) {
                    return null;
                } else if(Modifier.isPublic(mods) | Modifier.isProtected(mods)) {
                    // fall through
                } else if(Modifier.isPrivate(mods) && (cl != defCl)) {
                    return null;
                } else if(!packageEquals(cl, defCl)) {
                    return null;
                }
                try {
                    // Normal return
                    m.setAccessible(true);
                    return MethodHandles.lookup().unreflect(m);
                } catch(IllegalAccessException ex0) {
                    // setAccessible should prevent IAE
                    throw new InternalError("Error", ex0);
                }
            } catch(NoSuchMethodException ex) {
                defCl = defCl.getSuperclass();
            }
        }
        return null;
    }
    
    
    /**
     * A convenience class for acquiring the capability to instantiate
     * reflective objects.  Use this instead of a raw call to {@link
     * #getReflectionFactory} in order to avoid being limited by the
     * permissions of your callers.
     *
     * <p>An instance of this class can be used as the argument of
     * <code>AccessController.doPrivileged</code>.
     */
    // 获取ReflectionFactory实例
    public static final class GetReflectionFactoryAction implements PrivilegedAction<ReflectionFactory> {
        public ReflectionFactory run() {
            return getReflectionFactory();
        }
    }
    
}
