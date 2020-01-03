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

package java.io;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jdk.internal.misc.JavaSecurityAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import jdk.internal.reflect.ReflectionFactory;
import sun.reflect.misc.ReflectUtil;

import static java.io.ObjectStreamField.appendClassSignature;
import static java.io.ObjectStreamField.getClassSignature;

/**
 * Serialization's descriptor for classes.
 * It contains the name and serialVersionUID of the class.
 * The ObjectStreamClass for a specific class loaded in this Java VM can be found/created using the lookup method.
 *
 * <p>The algorithm to compute the SerialVersionUID is described in
 * <a href="{@docRoot}/../specs/serialization/class.html#stream-unique-identifiers">
 * Object Serialization Specification, Section 4.6, Stream Unique Identifiers</a>.
 *
 * @author Mike Warres
 * @author Roger Riggs
 * @see ObjectStreamField
 * @see <a href="{@docRoot}/../specs/serialization/class.html">
 * Object Serialization Specification, Section 4, Class Descriptors</a>
 * @since 1.1
 */
// 待序列化/反序列化对象的序列化描述符
public class ObjectStreamClass implements Serializable {
    
    private static final long serialVersionUID = -6120832682080437368L;
    
    /** serialPersistentFields value indicating no serializable fields */
    public static final ObjectStreamField[] NO_FIELDS = new ObjectStreamField[0];   // 表示不存在待序列化的字段
    
    private static final ObjectStreamField[] serialPersistentFields = NO_FIELDS;    // 待序列化的字段集，默认指示不存在待序列化字段
    
    /** reflection factory for obtaining serialization constructors */
    // 反射对象工厂
    private static final ReflectionFactory reflFactory = AccessController.doPrivileged(new ReflectionFactory.GetReflectionFactoryAction());
    
    
    /** serialVersionUID of represented class (null if not computed yet) */
    private volatile Long suid; // cl的序列化编号
    
    /** data layout of serialized objects described by this class desc */
    private volatile ClassDataSlot[] dataLayout;    // 数据槽：包含从当前类到最上层实现了Serializable接口的父类的所有序列化描述符
    
    /** class associated with this descriptor (if any) */
    private Class<?> cl;    // 待序列化的类对象
    
    /** name of class represented by this descriptor */
    private String name;        // cl的类型名称(虚拟机中呈现的名称)
    
    /** true if represents dynamic proxy class */
    private boolean isProxy;    // cl是否为代理类
    
    /** true if represents enum type */
    private boolean isEnum;     // cl是否为枚举类
    
    /** true if represented class implements Serializable */
    private boolean serializable;   // cl是否为Serializable实现类
    
    /** true if represented class implements Externalizable */
    private boolean externalizable; // cl是否为Externalizable实现类
    
    /** serializable fields */
    private ObjectStreamField[] fields; // 待序列化字段的序列化描述符信息（要求原始类型排在前面，引用类型排在后面）
    
    /** aggregate marshalled size of primitive fields */
    private int primDataSize;   // 统计fields中基本类型字段所占字节数
    
    /** number of non-primitive fields */
    private int numObjFields;   // 统计fields中引用类型字段数量
    
    /** reflector for setting/getting serializable field values */
    private FieldReflector fieldRefl;   // 待序列化的字段的统计信息
    
    /** serialization-appropriate constructor, or null if none */
    private Constructor<?> cons;    // 生成反序列化对象的构造器
    
    /** protection domains that need to be checked when calling the constructor */
    private ProtectionDomain[] domains; // 保护域信息
    
    /** true if desc has data written by class-defined writeObject method */
    private boolean hasWriteObjectData; // 当前类中是否包含writeObject方法(Serializable实现类)
    
    /** class-defined writeObject method, or null if none */
    private Method writeObjectMethod;   // 当前类中的writeObject方法(Serializable实现类)
    
    /** class-defined readObject method, or null if none */
    private Method readObjectMethod;    // 当前类中的readObject方法(Serializable实现类)
    
    /** class-defined readObjectNoData method, or null if none */
    private Method readObjectNoDataMethod;  // 当前类中的readObjectNoData方法(Serializable实现类)
    
    /** class-defined writeReplace method, or null if none */
    private Method writeReplaceMethod;      // 当前类中的writeReplace方法
    
    /** class-defined readResolve method, or null if none */
    private Method readResolveMethod;       // 当前类中的readResolve方法
    
    
    /** local class descriptor for represented class (may point to self) */
    private ObjectStreamClass localDesc;    // cl的序列化描述符
    
    /** superclass descriptor appearing in stream */
    private ObjectStreamClass superDesc;    // cl的父类的序列化描述符
    
    /** true if, and only if, the object has been correctly initialized */
    private boolean initialized;    // 序列化描述符信息是否已经完成初始化
    
    /**
     * true if desc has externalizable data written in block data format; this
     * must be true by default to accommodate ObjectInputStream subclasses which
     * override readClassDescriptor() to return class descriptors obtained from
     * ObjectStreamClass.lookup() (see 4461737)
     */
    private boolean hasBlockExternalData = true;    // 是否包含块数据
    
    
    /** exception (if any) thrown while attempting to resolve class */
    private ClassNotFoundException resolveEx;
    
    /** exception (if any) to throw if non-enum serialization attempted */
    private ExceptionInfo serializeEx;
    
    /** exception (if any) to throw if non-enum deserialization attempted */
    private ExceptionInfo deserializeEx;
    
    /** exception (if any) to throw if default serialization attempted */
    private ExceptionInfo defaultSerializeEx;
    
    
    static {
        initNative();
    }
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates blank class descriptor which should be initialized via a subsequent call to initProxy(), initNonProxy() or readNonProxy().
     */
    ObjectStreamClass() {
    }
    
    /**
     * Creates local class descriptor representing given class.
     */
    // 为实现序列化接口的类对象创建一个序列化描述符
    private ObjectStreamClass(final Class<?> cl) {
        // 待序列化的类型
        this.cl = cl;
        
        // 待序列化的类型名称(虚拟机中呈现的名称)
        name = cl.getName();
        
        // 判断cl是否为代理类
        isProxy = Proxy.isProxyClass(cl);
        
        // 判断cl是否为枚举类
        isEnum = Enum.class.isAssignableFrom(cl);
        
        // 判断cl是否为Serializable类
        serializable = Serializable.class.isAssignableFrom(cl);
        
        // 判断cl是否为Externalizable类
        externalizable = Externalizable.class.isAssignableFrom(cl);
        
        // 获取cl的父类
        Class<?> superCl = cl.getSuperclass();
        
        // 获取cl的父类对象superCl的序列化描述符(只允许处理Serializable类型的实现类)
        superDesc = (superCl != null) ? lookup(superCl, false) : null;
        
        // cl的序列化描述符
        localDesc = this;
        
        // 如果cl为Serializable类
        if(serializable) {
            AccessController.doPrivileged(new PrivilegedAction<>() {
                public Void run() {
                    // 如果cl为枚举类
                    if(isEnum) {
                        suid = 0L;
                        fields = NO_FIELDS;
                        return null;
                    }
                    
                    // 如果cl为数组类
                    if(cl.isArray()) {
                        fields = NO_FIELDS;
                        return null;
                    }
                    
                    // 获取cl类型的对象的序列化编号
                    suid = getDeclaredSUID(cl);
                    try {
                        /*
                         * 返回cl类中待序列化字段的序列化描述符，要求cl类本身满足以下条件才能获取有效字段集：
                         * 1.是Serializable实现类
                         * 2.不是代理类
                         * 3.不是枚举
                         * 4.不是接口
                         */
                        fields = getSerialFields(cl);
                        
                        /*
                         * 统计fields中原始类型字段所占字节数与引用类型字段的数量，
                         * 并校验fields中的字段顺序（原始类型排在前面，引用类型排在后面）。
                         */
                        computeFieldOffsets();
                    } catch(InvalidClassException e) {
                        serializeEx = deserializeEx = new ExceptionInfo(e.classname, e.getMessage());
                        fields = NO_FIELDS;
                    }
                    
                    // 如果cl是Externalizable实现类
                    if(externalizable) {
                        // 获取cl类的public无参构造器，如果不存在则返回null
                        cons = getExternalizableConstructor(cl);
                        
                        // 如果cl是Serializable实现类
                    } else {
                        /*
                         * 返回cl的第一个不可序列化的父类的无参构造器，要求改无参构造器可被cl访问。
                         * 如果未找到该构造器，或该构造器子类无法访问，则返回null。
                         * 对返回的构造函数（如果有）禁用访问检查。
                         */
                        cons = getSerializableConstructor(cl);
                        // 获取"private void writeObject(ObjectOutputStream out)"方法
                        writeObjectMethod = getPrivateMethod(cl, "writeObject", new Class<?>[]{ObjectOutputStream.class}, Void.TYPE);
                        // 获取"private void readObject(ObjectOutputStream out)"方法
                        readObjectMethod = getPrivateMethod(cl, "readObject", new Class<?>[]{ObjectInputStream.class}, Void.TYPE);
                        // 获取"private void readObjectNoData()"方法
                        readObjectNoDataMethod = getPrivateMethod(cl, "readObjectNoData", null, Void.TYPE);
                        // 是否存在writeObject方法
                        hasWriteObjectData = (writeObjectMethod != null);
                    }
                    
                    // 保护域信息
                    domains = getProtectionDomains(cons, cl);
                    
                    // 获取"private Object writeReplace()"方法
                    writeReplaceMethod = getInheritableMethod(cl, "writeReplace", null, Object.class);
                    // 获取"private Object readResolve()"方法
                    readResolveMethod = getInheritableMethod(cl, "readResolve", null, Object.class);
                    return null;
                }
            });
        } else {
            suid = 0L;
            fields = NO_FIELDS;
        }
        
        try {
            // 获取待序列化的字段的统计信息
            fieldRefl = getReflector(fields, this);
        } catch(InvalidClassException ex) {
            // field mismatches impossible when matching local fields vs. self
            throw new InternalError(ex);
        }
        
        // 反序列化异常
        if(deserializeEx == null) {
            if(isEnum) {
                deserializeEx = new ExceptionInfo(name, "enum type");
            } else if(cons == null) {
                deserializeEx = new ExceptionInfo(name, "no valid constructor");
            }
        }
        
        // 遍历待序列化字段的序列化描述符信息
        for(ObjectStreamField field : fields) {
            // 如果该序列化描述符中存在无效字段
            if(field.getField() == null) {
                defaultSerializeEx = new ExceptionInfo(name, "unmatched serializable field(s) declared");
            }
        }
        
        initialized = true;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Find the descriptor for a class that can be serialized.  Creates an
     * ObjectStreamClass instance if one does not exist yet for class. Null is
     * returned if the specified class does not implement java.io.Serializable
     * or java.io.Externalizable.
     *
     * @param cl class for which to get the descriptor
     *
     * @return the class descriptor for the specified class
     */
    // 获取类对象cl(只允许处理Serializable类型)的序列化描述符，返回之前会先去缓存中查找
    public static ObjectStreamClass lookup(Class<?> cl) {
        return lookup(cl, false);
    }
    
    /**
     * Returns the descriptor for any class, regardless of whether it
     * implements {@link Serializable}.
     *
     * @param cl class for which to get the descriptor
     *
     * @return the class descriptor for the specified class
     *
     * @since 1.6
     */
    // 获取类对象cl的序列化描述符，返回之前会先去缓存中查找
    public static ObjectStreamClass lookupAny(Class<?> cl) {
        return lookup(cl, true);
    }
    
    /**
     * Looks up and returns class descriptor for given class, or null if class is non-serializable and "all" is set to false.
     *
     * @param cl  class to look up
     * @param all if true, return descriptors for all classes;
     *            if false, only return descriptors for serializable classes
     */
    /*
     * 返回类对象cl的序列化描述符，返回之前会先去缓存中查找。
     * all为true指示允许处理任意类型，而all为false则指示只允许处理Serializable类型(的实现类)。
     */
    static ObjectStreamClass lookup(Class<?> cl, boolean all) {
        if(!all && !Serializable.class.isAssignableFrom(cl)) {
            return null;
        }
        
        // 从localDescs中移除localDescsQueue中包含的元素
        processQueue(Caches.localDescsQueue, Caches.localDescs);
        
        // 将指定的类对象包装为弱引用键，并指定localDescsQueue为其引用队列
        WeakClassKey key = new WeakClassKey(cl, Caches.localDescsQueue);
        
        // 获取该弱引用键映射的引用
        Reference<?> ref = Caches.localDescs.get(key);
        
        Object entry = null;
        
        // 获取key关联的软引用追踪的对象
        if(ref != null) {
            // 返回此Reference包裹的自定义引用对象，如果该对象已被回收，则返回null
            entry = ref.get();
        }
        
        EntryFuture future = null;
        
        // 需要将其他获取value的线程阻塞住，等为key关联新value后再放行
        if(entry == null) {
            EntryFuture newEntry = new EntryFuture();
            
            // 包装了EntryFuture的软引用
            Reference<?> newRef = new SoftReference<>(newEntry);
            do {
                // 之前软引用追踪的对象被回收了
                if(ref != null) {
                    // 从map中移除拥有指定key和value的元素，返回值表示是否移除成功
                    Caches.localDescs.remove(key, ref);
                }
                
                // 重新关联：将指定的元素（key-value）存入Map，并返回旧值，不允许覆盖
                ref = Caches.localDescs.putIfAbsent(key, newRef);
                
                // 如果key已经关联值(来自别的线程干扰)
                if(ref != null) {
                    // 获取当前key关联的软引用追踪的对象
                    entry = ref.get();
                }
            } while(ref != null && entry == null);
            
            // newEntry被成功关联
            if(entry == null) {
                future = newEntry;
            }
        }
        
        // 直接获取到了目标value
        if(entry instanceof ObjectStreamClass) {    // check common case first
            return (ObjectStreamClass) entry;
        }
        
        if (entry instanceof EntryFuture) {
            future = (EntryFuture) entry;
            
            if(future.getOwner() == Thread.currentThread()) {
                /*
                 * Handle nested call situation described by 4803747: waiting
                 * for future value to be set by a lookup() call further up the
                 * stack will result in deadlock, so calculate and set the
                 * future value here instead.
                 */
                entry = null;
            } else {
                // 如果key没有关联到有效的value，会阻塞
                entry = future.get();
            }
        }
        
        if(entry == null) {
            try {
                // 生成cl类的序列化描述符信息
                entry = new ObjectStreamClass(cl);
            } catch(Throwable th) {
                entry = th;
            }
            
            /*
             * 此刻，key已经准备好关联有效的value(entry)，
             * 因此可以唤醒所有阻塞在future.get()上的线程了
             */
            if(future.set(entry)) {
                // 将指定的元素（key-value）存入Map，并返回旧值，允许覆盖
                Caches.localDescs.put(key, new SoftReference<>(entry));
            } else {
                // nested lookup call already set future
                entry = future.get();
            }
        }
        
        // 正常返回
        if(entry instanceof ObjectStreamClass) {
            return (ObjectStreamClass) entry;
        } else if(entry instanceof RuntimeException) {
            throw (RuntimeException) entry;
        } else if(entry instanceof Error) {
            throw (Error) entry;
        } else {
            throw new InternalError("unexpected entry: " + entry);
        }
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Invokes the writeObject method of the represented serializable class.
     * Throws UnsupportedOperationException if this class descriptor is not
     * associated with a class, or if the class is externalizable,
     * non-serializable or does not define writeObject.
     */
    // 调用obj对象的writeObject方法(Serializable实现类)
    void invokeWriteObject(Object obj, ObjectOutputStream out) throws IOException, UnsupportedOperationException {
        requireInitialized();
        
        if(writeObjectMethod != null) {
            try {
                writeObjectMethod.invoke(obj, out);
            } catch(InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if(th instanceof IOException) {
                    throw (IOException) th;
                } else {
                    throwMiscException(th);
                }
            } catch(IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Invokes the readObject method of the represented serializable class.
     * Throws UnsupportedOperationException if this class descriptor is not
     * associated with a class, or if the class is externalizable,
     * non-serializable or does not define readObject.
     */
    // 调用obj对象的readObject方法(Serializable实现类)
    void invokeReadObject(Object obj, ObjectInputStream in) throws ClassNotFoundException, IOException, UnsupportedOperationException {
        requireInitialized();
        
        if(readObjectMethod != null) {
            try {
                readObjectMethod.invoke(obj, in);
            } catch(InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if(th instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException) th;
                } else if(th instanceof IOException) {
                    throw (IOException) th;
                } else {
                    throwMiscException(th);
                }
            } catch(IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Invokes the readObjectNoData method of the represented serializable
     * class.  Throws UnsupportedOperationException if this class descriptor is
     * not associated with a class, or if the class is externalizable,
     * non-serializable or does not define readObjectNoData.
     */
    // 调用obj对象的readObjectNoDataMethod方法(Serializable实现类)
    void invokeReadObjectNoData(Object obj) throws IOException, UnsupportedOperationException {
        requireInitialized();
        
        if(readObjectNoDataMethod != null) {
            try {
                readObjectNoDataMethod.invoke(obj, (Object[]) null);
            } catch(InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if(th instanceof ObjectStreamException) {
                    throw (ObjectStreamException) th;
                } else {
                    throwMiscException(th);
                }
            } catch(IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Invokes the writeReplace method of the represented serializable class and returns the result.
     * Throws UnsupportedOperationException if this class descriptor is not associated with a class,
     * or if the class is non-serializable or does not define writeReplace.
     */
    // 调用obj对象的writeReplace方法
    Object invokeWriteReplace(Object obj) throws IOException, UnsupportedOperationException {
        requireInitialized();
        
        if(writeReplaceMethod != null) {
            try {
                // 调用writeReplace方法
                return writeReplaceMethod.invoke(obj, (Object[]) null);
            } catch(InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if(th instanceof ObjectStreamException) {
                    throw (ObjectStreamException) th;
                } else {
                    throwMiscException(th);
                    throw new InternalError(th);  // never reached
                }
            } catch(IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Invokes the readResolve method of the represented serializable class and
     * returns the result.  Throws UnsupportedOperationException if this class
     * descriptor is not associated with a class, or if the class is
     * non-serializable or does not define readResolve.
     */
    Object invokeReadResolve(Object obj) throws IOException, UnsupportedOperationException {
        requireInitialized();
        if(readResolveMethod != null) {
            try {
                return readResolveMethod.invoke(obj, (Object[]) null);
            } catch(InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if(th instanceof ObjectStreamException) {
                    throw (ObjectStreamException) th;
                } else {
                    throwMiscException(th);
                    throw new InternalError(th);  // never reached
                }
            } catch(IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns array of ClassDataSlot instances representing the data layout
     * (including superclass data) for serialized objects described by this class descriptor.
     * ClassDataSlots are ordered by inheritance with those containing "higher" superclasses appearing first.
     * The final ClassDataSlot contains a reference to this descriptor.
     */
    // 返回数据槽：包含从当前类到最上层实现了Serializable接口的父类的所有序列化描述符
    ClassDataSlot[] getClassDataLayout() throws InvalidClassException {
        // REMIND: synchronize instead of relying on volatile?
        if(dataLayout == null) {
            dataLayout = getClassDataLayout0();
        }
        
        return dataLayout;
    }
    
    // 返回数据槽：包含从当前类到最上层实现了Serializable接口的父类的所有序列化描述符
    private ClassDataSlot[] getClassDataLayout0() throws InvalidClassException {
        ArrayList<ClassDataSlot> slots = new ArrayList<>();
        Class<?> start = cl, end = cl;
        
        /* locate closest non-serializable superclass */
        // 查找cl首个非Serializable类型的父类
        while(end != null && Serializable.class.isAssignableFrom(end)) {
            end = end.getSuperclass();
        }
        
        HashSet<String> oscNames = new HashSet<>(3);
        
        for(ObjectStreamClass d = this; d != null; d = d.superDesc) {
            
            if(oscNames.contains(d.name)) {
                throw new InvalidClassException("Circular reference.");
            } else {
                oscNames.add(d.name);
            }
            
            /* search up inheritance hierarchy for class with matching name */
            String searchName = (d.cl != null) ? d.cl.getName() : d.name;
            Class<?> match = null;
            
            for(Class<?> c = start; c != end; c = c.getSuperclass()) {
                if(searchName.equals(c.getName())) {
                    match = c;
                    break;
                }
            }
            
            /* add "no data" slot for each unmatched class below match */
            if(match != null) {
                for(Class<?> c = start; c != match; c = c.getSuperclass()) {
                    // 获取类对象c的序列化描述符，返回之前会先去缓存中查找
                    ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(c, true);
                    ClassDataSlot slot = new ClassDataSlot(objectStreamClass, false);
                    slots.add(slot);
                }
                
                start = match.getSuperclass();
            }
            
            // 获取类对象cl的一个序列化描述符
            ObjectStreamClass objectStreamClass = d.getVariantFor(match);
            ClassDataSlot slot = new ClassDataSlot(objectStreamClass, true);
            // record descriptor/class pairing
            slots.add(slot);
        }
        
        /* add "no data" slot for any leftover unmatched classes */
        for(Class<?> c = start; c != end; c = c.getSuperclass()) {
            // 获取类对象c的序列化描述符，返回之前会先去缓存中查找
            ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(c, true);
            ClassDataSlot slot = new ClassDataSlot(objectStreamClass, false);
            slots.add(slot);
        }
        
        // order slots from superclass -> subclass
        Collections.reverse(slots);     // 逆转list中的元素：使得父类的序列化描述符排列到前面
        
        return slots.toArray(new ClassDataSlot[slots.size()]);
    }
    
    /**
     * If given class is the same as the class associated with this class descriptor, returns reference to this class descriptor.
     * Otherwise, returns variant of this class descriptor bound to given class.
     */
    // 获取类对象cl的一个序列化描述符
    private ObjectStreamClass getVariantFor(Class<?> cl) throws InvalidClassException {
        if(this.cl == cl) {
            return this;
        }
        
        ObjectStreamClass desc = new ObjectStreamClass();
        if(isProxy) {
            // 使用代理类对象cl来初始化desc
            desc.initProxy(cl, null, superDesc);
        } else {
            // 使用非代理类对象cl来初始化desc
            desc.initNonProxy(this, cl, null, superDesc);
        }
        
        return desc;
    }
    
    /**
     * Initializes class descriptor representing a proxy class.
     */
    // 使用代理类对象cl来初始化当前序列化描述符
    void initProxy(Class<?> cl, ClassNotFoundException resolveEx, ObjectStreamClass superDesc) throws InvalidClassException {
        ObjectStreamClass osc = null;
        if(cl != null) {
            // 获取类对象cl的序列化描述符，返回之前会先去缓存中查找
            osc = lookup(cl, true);
            if(!osc.isProxy) {
                throw new InvalidClassException("cannot bind proxy descriptor to a non-proxy class");
            }
        }
        
        this.cl = cl;
        this.resolveEx = resolveEx;
        this.superDesc = superDesc;
        isProxy = true;
        serializable = true;
        suid = 0L;
        fields = NO_FIELDS;
        if(osc != null) {
            localDesc = osc;
            name = localDesc.name;
            externalizable = localDesc.externalizable;
            writeReplaceMethod = localDesc.writeReplaceMethod;
            readResolveMethod = localDesc.readResolveMethod;
            deserializeEx = localDesc.deserializeEx;
            domains = localDesc.domains;
            cons = localDesc.cons;
        }
        
        // 返回待序列化的字段的统计信息
        fieldRefl = getReflector(fields, localDesc);
        initialized = true;
    }
    
    /**
     * Initializes class descriptor representing a non-proxy class.
     */
    // 使用非代理类对象cl来初始化当前序列化描述符
    void initNonProxy(ObjectStreamClass model, Class<?> cl, ClassNotFoundException resolveEx, ObjectStreamClass superDesc) throws InvalidClassException {
        // 获取待序列化对象的序列化编号
        long suid = model.getSerialVersionUID();
        
        ObjectStreamClass osc = null;
        if(cl != null) {
            // 获取类对象cl的序列化描述符，返回之前会先去缓存中查找
            osc = lookup(cl, true);
            if(osc.isProxy) {
                throw new InvalidClassException("cannot bind non-proxy descriptor to a proxy class");
            }
            
            if(model.isEnum != osc.isEnum) {
                throw new InvalidClassException(model.isEnum ? "cannot bind enum descriptor to a non-enum class" : "cannot bind non-enum descriptor to an enum class");
            }
            
            if(model.serializable == osc.serializable && !cl.isArray() && suid != osc.getSerialVersionUID()) {
                throw new InvalidClassException(osc.name, "local class incompatible: " + "stream classdesc serialVersionUID = " + suid + ", local class serialVersionUID = " + osc.getSerialVersionUID());
            }
            
            if(!classNamesEqual(model.name, osc.name)) {
                throw new InvalidClassException(osc.name, "local class name incompatible with stream class " + "name \"" + model.name + "\"");
            }
            
            if(!model.isEnum) {
                if((model.serializable == osc.serializable) && (model.externalizable != osc.externalizable)) {
                    throw new InvalidClassException(osc.name, "Serializable incompatible with Externalizable");
                }
                
                if((model.serializable != osc.serializable) || (model.externalizable != osc.externalizable) || !(model.serializable || model.externalizable)) {
                    deserializeEx = new ExceptionInfo(osc.name, "class invalid for deserialization");
                }
            }
        }
        
        this.cl = cl;
        this.resolveEx = resolveEx;
        this.superDesc = superDesc;
        name = model.name;
        this.suid = suid;
        isProxy = false;
        isEnum = model.isEnum;
        serializable = model.serializable;
        externalizable = model.externalizable;
        hasBlockExternalData = model.hasBlockExternalData;
        hasWriteObjectData = model.hasWriteObjectData;
        fields = model.fields;
        primDataSize = model.primDataSize;
        numObjFields = model.numObjFields;
        
        if(osc != null) {
            localDesc = osc;
            writeObjectMethod = localDesc.writeObjectMethod;
            readObjectMethod = localDesc.readObjectMethod;
            readObjectNoDataMethod = localDesc.readObjectNoDataMethod;
            writeReplaceMethod = localDesc.writeReplaceMethod;
            readResolveMethod = localDesc.readResolveMethod;
            if(deserializeEx == null) {
                deserializeEx = localDesc.deserializeEx;
            }
            domains = localDesc.domains;
            cons = localDesc.cons;
        }
        
        // 返回待序列化的字段的统计信息
        fieldRefl = getReflector(fields, localDesc);
        
        // reassign to matched fields so as to reflect local unshared settings
        fields = fieldRefl.getFields();
        
        initialized = true;
    }
    
    /**
     * Reads non-proxy class descriptor information from given input stream.
     * The resulting class descriptor is not fully functional; it can only be
     * used as input to the ObjectInputStream.resolveClass() and
     * ObjectStreamClass.initNonProxy() methods.
     */
    void readNonProxy(ObjectInputStream in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        suid = in.readLong();
        isProxy = false;
        
        byte flags = in.readByte();
        hasWriteObjectData = ((flags & ObjectStreamConstants.SC_WRITE_METHOD) != 0);
        hasBlockExternalData = ((flags & ObjectStreamConstants.SC_BLOCK_DATA) != 0);
        externalizable = ((flags & ObjectStreamConstants.SC_EXTERNALIZABLE) != 0);
        boolean sflag = ((flags & ObjectStreamConstants.SC_SERIALIZABLE) != 0);
        if(externalizable && sflag) {
            throw new InvalidClassException(name, "serializable and externalizable flags conflict");
        }
        
        serializable = externalizable || sflag;
        isEnum = ((flags & ObjectStreamConstants.SC_ENUM) != 0);
        if(isEnum && suid.longValue() != 0L) {
            throw new InvalidClassException(name, "enum descriptor has non-zero serialVersionUID: " + suid);
        }
        
        int numFields = in.readShort();
        if(isEnum && numFields != 0) {
            throw new InvalidClassException(name, "enum descriptor has non-zero field count: " + numFields);
        }
        
        fields = (numFields>0) ? new ObjectStreamField[numFields] : NO_FIELDS;
        
        for(int i = 0; i<numFields; i++) {
            char tcode = (char) in.readByte();
            String fname = in.readUTF();
            String signature = ((tcode == 'L') || (tcode == '[')) ? in.readTypeString() : new String(new char[]{tcode});
            try {
                fields[i] = new ObjectStreamField(fname, signature, false);
            } catch(RuntimeException e) {
                throw (IOException) new InvalidClassException(name, "invalid descriptor for field " + fname).initCause(e);
            }
        }
        
        /*
         * 统计fields中原始类型字段所占字节数与引用类型字段的数量，
         * 并校验fields中的字段顺序（原始类型排在前面，引用类型排在后面）。
         */
        computeFieldOffsets();
    }
    
    /**
     * Writes non-proxy class descriptor information to given output stream.
     */
    // 向输出流out写入当前(非代理对象的)序列化描述符
    void writeNonProxy(ObjectOutputStream out) throws IOException {
        out.writeUTF(name);     // 向最终输出流写入待序列化的类型名称(以UTF8的形式写入)
        
        // 获取待序列化对象的序列化编号
        long suid = getSerialVersionUID();
        out.writeLong(suid);
        
        byte flags = 0;
        if(externalizable) {
            flags |= ObjectStreamConstants.SC_EXTERNALIZABLE;
            
            int protocol = out.getProtocolVersion();
            if(protocol != ObjectStreamConstants.PROTOCOL_VERSION_1) {
                flags |= ObjectStreamConstants.SC_BLOCK_DATA;
            }
        } else if(serializable) {
            flags |= ObjectStreamConstants.SC_SERIALIZABLE;
        }
        if(hasWriteObjectData) {
            flags |= ObjectStreamConstants.SC_WRITE_METHOD;
        }
        if(isEnum) {
            flags |= ObjectStreamConstants.SC_ENUM;
        }
        
        out.writeByte(flags);
        
        out.writeShort(fields.length);
        
        for(ObjectStreamField f : fields) {
            out.writeByte(f.getTypeCode());
            out.writeUTF(f.getName());
            if(!f.isPrimitive()) {
                out.writeTypeString(f.getTypeString());
            }
        }
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Fetches the serializable primitive field values of object obj and
     * marshals them into byte array buf starting at offset 0.  It is the
     * responsibility of the caller to ensure that obj is of the proper type if
     * non-null.
     */
    // 返回obj中所有待序列化的基本类型字段的值
    void getPrimFieldValues(Object obj, byte[] buf) {
        fieldRefl.getPrimFieldValues(obj, buf);
    }
    
    /**
     * Sets the serializable primitive fields of object obj using values
     * unmarshalled from byte array buf starting at offset 0.  It is the
     * responsibility of the caller to ensure that obj is of the proper type if
     * non-null.
     */
    void setPrimFieldValues(Object obj, byte[] buf) {
        fieldRefl.setPrimFieldValues(obj, buf);
    }
    
    /**
     * Fetches the serializable object field values of object obj and stores
     * them in array vals starting at offset 0.  It is the responsibility of
     * the caller to ensure that obj is of the proper type if non-null.
     */
    // 获取obj中所有待序列化的引用类型字段的值
    void getObjFieldValues(Object obj, Object[] vals) {
        fieldRefl.getObjFieldValues(obj, vals);
    }
    
    /**
     * Sets the serializable object fields of object obj using values from
     * array vals starting at offset 0.  It is the responsibility of the caller
     * to ensure that obj is of the proper type if non-null.
     */
    void setObjFieldValues(Object obj, Object[] vals) {
        fieldRefl.setObjFieldValues(obj, vals);
    }
    
    /**
     * Creates a new instance of the represented class.  If the class is
     * externalizable, invokes its public no-arg constructor; otherwise, if the
     * class is serializable, invokes the no-arg constructor of the first
     * non-serializable superclass.  Throws UnsupportedOperationException if
     * this class descriptor is not associated with a class, if the associated
     * class is non-serializable or if the appropriate no-arg constructor is
     * inaccessible/unavailable.
     */
    // 返回反序列化对象的实例
    Object newInstance() throws InstantiationException, InvocationTargetException, UnsupportedOperationException {
        requireInitialized();
        
        if(cons != null) {
            try {
                if(domains == null || domains.length == 0) {
                    return cons.newInstance();
                } else {
                    JavaSecurityAccess jsa = SharedSecrets.getJavaSecurityAccess();
                    PrivilegedAction<?> pea = () -> {
                        try {
                            return cons.newInstance();
                        } catch(InstantiationException | InvocationTargetException | IllegalAccessException x) {
                            throw new UndeclaredThrowableException(x);
                        }
                    }; // Can't use PrivilegedExceptionAction with jsa
                    
                    try {
                        return jsa.doIntersectionPrivilege(pea, AccessController.getContext(), new AccessControlContext(domains));
                    } catch(UndeclaredThrowableException x) {
                        Throwable cause = x.getCause();
                        if(cause instanceof InstantiationException) {
                            throw (InstantiationException) cause;
                        }
                        if(cause instanceof InvocationTargetException) {
                            throw (InvocationTargetException) cause;
                        }
                        if(cause instanceof IllegalAccessException) {
                            throw (IllegalAccessException) cause;
                        }
                        // not supposed to happen
                        throw x;
                    }
                }
            } catch(IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Returns public no-arg constructor of given class, or null if none found.
     * Access checks are disabled on the returned constructor (if any),
     * since the defining class may still be non-public.
     */
    // 获取cl类的public无参构造器，如果不存在则返回null
    private static Constructor<?> getExternalizableConstructor(Class<?> cl) {
        try {
            // 返回cl类中的无参构造器，但不包括父类中的构造器
            Constructor<?> cons = cl.getDeclaredConstructor((Class<?>[]) null);
            cons.setAccessible(true);
            
            // 确保该构造器为public
            return ((cons.getModifiers() & Modifier.PUBLIC) != 0) ? cons : null;
        } catch(NoSuchMethodException ex) {
            return null;
        }
    }
    
    /**
     * Returns subclass-accessible no-arg constructor of first non-serializable superclass, or null if none found.
     * Access checks are disabled on the returned constructor (if any).
     */
    /*
     * 返回cl的第一个不可序列化的父类的无参构造器，要求改无参构造器可被cl访问。
     * 如果未找到该构造器，或该构造器子类无法访问，则返回null。
     * 对返回的构造函数（如果有）禁用访问检查。
     */
    private static Constructor<?> getSerializableConstructor(Class<?> cl) {
        return reflFactory.newConstructorForSerialization(cl);
    }
    
    /**
     * Returns non-static, non-abstract method with given signature provided it
     * is defined by or accessible (via inheritance) by the given class, or
     * null if no match found.  Access checks are disabled on the returned
     * method (if any).
     */
    // 从cl中获取拥有特定名称、形参类型、返回类型的非静态私有(实现)方法
    private static Method getInheritableMethod(Class<?> cl, String name, Class<?>[] argTypes, Class<?> returnType) {
        Method meth = null;
        Class<?> defCl = cl;
        
        while(defCl != null) {
            try {
                // 返回defCl类中指定名称和形参的方法，但不包括父类/父接口中的方法
                meth = defCl.getDeclaredMethod(name, argTypes);
                break;
            } catch(NoSuchMethodException ex) {
                // 获取defCl类的父类（只识别非泛型类型）
                defCl = defCl.getSuperclass();
            }
        }
        
        if((meth == null) || (meth.getReturnType() != returnType)) {
            return null;
        }
        
        meth.setAccessible(true);
        int mods = meth.getModifiers();
        
        if((mods & (Modifier.STATIC | Modifier.ABSTRACT)) != 0) {
            return null;
        } else if((mods & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0) {
            return meth;
        } else if((mods & Modifier.PRIVATE) != 0) {
            return (cl == defCl) ? meth : null;
        } else {
            return packageEquals(cl, defCl) ? meth : null;
        }
    }
    
    /**
     * Returns non-static private method with given signature defined by given
     * class, or null if none found.  Access checks are disabled on the
     * returned method (if any).
     */
    // 从cl中获取拥有特定名称、形参类型、返回类型的非静态私有方法
    private static Method getPrivateMethod(Class<?> cl, String name, Class<?>[] argTypes, Class<?> returnType) {
        try {
            // 返回cl类中指定名称和形参的方法，但不包括父类/父接口中的方法
            Method meth = cl.getDeclaredMethod(name, argTypes);
            meth.setAccessible(true);
            int mods = meth.getModifiers();
            return ((meth.getReturnType() == returnType) && ((mods & Modifier.STATIC) == 0) && ((mods & Modifier.PRIVATE) != 0)) ? meth : null;
        } catch(NoSuchMethodException ex) {
            return null;
        }
    }
    
    /**
     * Returns ObjectStreamField array describing the serializable fields of
     * the given class.  Serializable fields backed by an actual field of the
     * class are represented by ObjectStreamFields with corresponding non-null
     * Field objects.  Throws InvalidClassException if the (explicitly
     * declared) serializable fields are invalid.
     */
    /*
     * 返回cl类中待序列化字段的序列化描述符，要求cl类本身满足以下条件才能获取有效字段集：
     * 1.是Serializable实现类，不是Externalizable实现类
     * 2.不是代理类
     * 3.不是枚举
     * 4.不是接口
     */
    private static ObjectStreamField[] getSerialFields(Class<?> cl) throws InvalidClassException {
        ObjectStreamField[] fields;
        
        if(Serializable.class.isAssignableFrom(cl)          // cl是Serializable实现类
            && !Externalizable.class.isAssignableFrom(cl)   // cl不是Externalizable实现类
            && !Proxy.isProxyClass(cl)                      // cl不是代理类
            && !cl.isInterface()) {                         // cl不是接口
            
            /*
             * 从cl类的"serialPersistentFields"字段中解析出待序列化的字段集，
             * 这些待序列化的字段被包装为ObjectStreamField类型。
             *
             * 如果不存在"serialPersistentFields"字段
             */
            if((fields = getDeclaredSerialFields(cl)) == null) {
                /*
                 * 返回cl类中默认的待序列化字段，要求这些字段没有被static或transient修饰。
                 * 该方法在未找到ObjectStreamField[]类型的"serialPersistentFields"字段时才被执行。
                 */
                fields = getDefaultSerialFields(cl);
            }
            
            Arrays.sort(fields);
        } else {
            fields = NO_FIELDS;
        }
        
        return fields;
    }
    
    /**
     * Returns serializable fields of given class as defined explicitly by a "serialPersistentFields" field,
     * or null if no appropriate "serialPersistentFields" field is defined.
     * Serializable fields backed by an actual field of the class are represented by ObjectStreamFields
     * with corresponding non-null Field objects.
     * For compatibility with past releases, a "serialPersistentFields" field with a null value is
     * considered equivalent to not declaring "serialPersistentFields".
     * Throws InvalidClassException if the declared serializable fields are invalid--e.g.,
     * if multiple fields share the same name.
     */
    /*
     * 返回待序列化的字段集，这些待序列化的字段被包装为ObjectStreamField类型。
     *
     * 注：待序列化字段的信息解析自cl类的"serialPersistentFields"字段
     */
    private static ObjectStreamField[] getDeclaredSerialFields(Class<?> cl) throws InvalidClassException {
        ObjectStreamField[] serialPersistentFields = null;
        
        try {
            // 返回当前类中名称为"serialPersistentFields"的字段，但不包括父类/父接口中的字段
            Field f = cl.getDeclaredField("serialPersistentFields");
            int mask = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;
            // 校验该字段的修饰符
            if((f.getModifiers() & mask) == mask) {
                f.setAccessible(true);
                // 获取该字段的值
                serialPersistentFields = (ObjectStreamField[]) f.get(null);
            }
        } catch(Exception ex) {
        }
        
        // 如果没有获取到"serialPersistentFields"字段
        if(serialPersistentFields == null) {
            return null;
            
            // 如果存在"serialPersistentFields"字段，但没有实质的内容
        } else if(serialPersistentFields.length == 0) {
            return NO_FIELDS;
        }
        
        ObjectStreamField[] boundFields = new ObjectStreamField[serialPersistentFields.length];
        
        // 所有待序列化的字段的名称
        Set<String> fieldNames = new HashSet<>(serialPersistentFields.length);
        
        // 遍历"serialPersistentFields"数组
        for(int i = 0; i<serialPersistentFields.length; i++) {
            ObjectStreamField spf = serialPersistentFields[i];
            
            // 获取字段的名称
            String fname = spf.getName();
            if(fieldNames.contains(fname)) {
                throw new InvalidClassException("multiple serializable fields named " + fname);
            }
            fieldNames.add(fname);
            
            try {
                // 返回当前类中指定名称的字段，但不包括父类/父接口中的字段
                Field f = cl.getDeclaredField(fname);
                
                if((f.getType() == spf.getType())                       // 确保待序列化的字段类型正确
                    && ((f.getModifiers() & Modifier.STATIC) == 0)) {   // 确保待序列化的字段不是static类型
                    // 记录待序列化的字段
                    boundFields[i] = new ObjectStreamField(f, spf.isUnshared(), true);
                }
            } catch(NoSuchFieldException ex) {
            }
            
            if(boundFields[i] == null) {
                boundFields[i] = new ObjectStreamField(fname, spf.getType(), spf.isUnshared());
            }
        }
        
        return boundFields;
    }
    
    /**
     * Returns array of ObjectStreamFields corresponding to all non-static
     * non-transient fields declared by given class.  Each ObjectStreamField
     * contains a Field object for the field it represents.  If no default
     * serializable fields exist, NO_FIELDS is returned.
     */
    /*
     * 返回cl类中默认的待序列化字段，要求这些字段没有被static或transient修饰。
     *
     * 注：该方法在未找到ObjectStreamField[]类型的"serialPersistentFields"字段时才被执行。
     */
    private static ObjectStreamField[] getDefaultSerialFields(Class<?> cl) {
        
        int mask = Modifier.STATIC | Modifier.TRANSIENT;
        
        ArrayList<ObjectStreamField> list = new ArrayList<>();
        
        // 遍历cl类中所有字段，但不包括父类/父接口中的字段
        Field[] clFields = cl.getDeclaredFields();
        
        for(Field clField : clFields) {
            // 如果该字段没有被static或transient修饰，则将其包装为ObjectStreamField类型，并添加到集合中
            if((clField.getModifiers() & mask) == 0) {
                list.add(new ObjectStreamField(clField, false, true));
            }
        }
        
        int size = list.size();
        
        return (size == 0) ? NO_FIELDS : list.toArray(new ObjectStreamField[size]);
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 检查 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Throws an InvalidClassException if object instances referencing this
     * class descriptor should not be allowed to deserialize.  This method does
     * not apply to deserialization of enum constants.
     */
    void checkDeserialize() throws InvalidClassException {
        requireInitialized();
        
        if(deserializeEx != null) {
            throw deserializeEx.newInvalidClassException();
        }
    }
    
    /**
     * Throws an InvalidClassException if objects whose class is represented by
     * this descriptor should not be allowed to serialize.  This method does
     * not apply to serialization of enum constants.
     */
    void checkSerialize() throws InvalidClassException {
        requireInitialized();
        
        if(serializeEx != null) {
            throw serializeEx.newInvalidClassException();
        }
    }
    
    /**
     * Throws an InvalidClassException if objects whose class is represented by
     * this descriptor should not be permitted to use default serialization
     * (e.g., if the class declares serializable fields that do not correspond
     * to actual fields, and hence must use the GetField API).  This method
     * does not apply to deserialization of enum constants.
     */
    // 确保序列化描述符已初始化，且其中不包含无效字段
    void checkDefaultSerialize() throws InvalidClassException {
        requireInitialized();
        
        if(defaultSerializeEx != null) {
            throw defaultSerializeEx.newInvalidClassException();
        }
    }
    
    /**
     * Checks that the given values, from array vals starting at offset 0,
     * are assignable to the given serializable object fields.
     *
     * @throws ClassCastException if any value is not assignable
     */
    void checkObjFieldValueTypes(Object obj, Object[] vals) {
        fieldRefl.checkObjectFieldValueTypes(obj, vals);
    }
    
    /*▲ 检查 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the name of the class described by this descriptor.
     * This method returns the name of the class in the format that
     * is used by the {@link Class#getName} method.
     *
     * @return a string representing the name of the class
     */
    // 返回cl的类型名称(虚拟机中呈现的名称)
    public String getName() {
        return name;
    }
    
    /**
     * Return the serialVersionUID for this class.  The serialVersionUID
     * defines a set of classes all with the same name that have evolved from a
     * common root class and agree to be serialized and deserialized using a
     * common format.  NonSerializable classes have a serialVersionUID of 0L.
     *
     * @return the SUID of the class described by this descriptor
     */
    // 返回待序列化对象的序列化编号
    public long getSerialVersionUID() {
        // REMIND: synchronize instead of relying on volatile
        if(suid == null) {
            suid = AccessController.doPrivileged(new PrivilegedAction<Long>() {
                public Long run() {
                    // 返回一个默认的序列化编号
                    return computeDefaultSUID(cl);
                }
            });
        }
        
        return suid;
    }
    
    /**
     * Return the class in the local VM that this version is mapped to.  Null
     * is returned if there is no corresponding local class.
     *
     * @return the <code>Class</code> instance that this descriptor represents
     */
    // 返回待序列化对象的类型
    @CallerSensitive
    public Class<?> forClass() {
        if(cl == null) {
            return null;
        }
        
        // 确保序列化描述符已经初始化
        requireInitialized();
        
        if(System.getSecurityManager() != null) {
            Class<?> caller = Reflection.getCallerClass();
            if(ReflectUtil.needsPackageAccessCheck(caller.getClassLoader(), cl.getClassLoader())) {
                ReflectUtil.checkPackageAccess(cl);
            }
        }
        
        return cl;
    }
    
    /**
     * Return an array of the fields of this serializable class.
     *
     * @return an array containing an element for each persistent field of
     * this class. Returns an array of length zero if there are no
     * fields.
     *
     * @since 1.2
     */
    // 获取待序列化字段的拷贝
    public ObjectStreamField[] getFields() {
        return getFields(true);
    }
    
    /**
     * Get the field of this class by name.
     *
     * @param name the name of the data field to look for
     *
     * @return The ObjectStreamField object of the named field or null if
     * there is no such named field.
     */
    // 返回指定名称的待序列化字段
    public ObjectStreamField getField(String name) {
        return getField(name, null);
    }
    
    /**
     * Returns the "local" class descriptor for the class associated with this
     * class descriptor (i.e., the result of
     * ObjectStreamClass.lookup(this.forClass())) or null if there is no class
     * associated with this descriptor.
     */
    // 返回cl的序列化描述符
    ObjectStreamClass getLocalDesc() {
        requireInitialized();
        return localDesc;
    }
    
    /**
     * Returns superclass descriptor.  Note that on the receiving side, the
     * superclass descriptor may be bound to a class that is not a superclass
     * of the subclass descriptor's bound class.
     */
    // 返回cl的父类的序列化描述符
    ObjectStreamClass getSuperDesc() {
        requireInitialized();
        return superDesc;
    }
    
    /**
     * Returns arrays of ObjectStreamFields representing the serializable fields of the represented class.
     * If copy is true, a clone of this class descriptor's field array is returned,
     * otherwise the array itself is returned.
     */
    // 获取待序列化字段(的拷贝)
    ObjectStreamField[] getFields(boolean copy) {
        return copy ? fields.clone() : fields;
    }
    
    /**
     * Looks up a serializable field of the represented class by name and type.
     * A specified type of null matches all types, Object.class matches all
     * non-primitive types, and any other non-null type matches assignable
     * types only.  Returns matching field, or null if no match found.
     */
    // 返回指定名称与类型的待序列化字段
    ObjectStreamField getField(String name, Class<?> type) {
        for(ObjectStreamField f : fields) {
            if(f.getName().equals(name)) {
                if(type == null || (type == Object.class && !f.isPrimitive())) {
                    return f;
                }
                
                Class<?> ftype = f.getType();
                if(ftype != null && type.isAssignableFrom(ftype)) {
                    return f;
                }
            }
        }
        return null;
    }
    
    /**
     * Returns true if class descriptor represents a dynamic proxy class, false
     * otherwise.
     */
    // 判断cl是否为代理类
    boolean isProxy() {
        requireInitialized();
        return isProxy;
    }
    
    /**
     * Returns true if class descriptor represents an enum type, false
     * otherwise.
     */
    // 判断cl是否为枚举类
    boolean isEnum() {
        requireInitialized();
        return isEnum;
    }
    
    /**
     * Returns true if represented class implements Externalizable, false
     * otherwise.
     */
    // 判断cl是否为Externalizable实现类
    boolean isExternalizable() {
        requireInitialized();
        return externalizable;
    }
    
    /**
     * Returns true if represented class implements Serializable, false
     * otherwise.
     */
    // 判断cl是否为Serializable实现类
    boolean isSerializable() {
        requireInitialized();
        return serializable;
    }
    
    /**
     * Returns true if class descriptor represents externalizable class that
     * has written its data in 1.2 (block data) format, false otherwise.
     */
    // 是否包含块数据
    boolean hasBlockExternalData() {
        requireInitialized();
        return hasBlockExternalData;
    }
    
    /**
     * Returns true if class descriptor represents serializable (but not
     * externalizable) class which has written its data via a custom
     * writeObject() method, false otherwise.
     */
    // 判断当前类中是否包含writeObject方法(Serializable实现类)
    boolean hasWriteObjectData() {
        requireInitialized();
        return hasWriteObjectData;
    }
    
    /**
     * Returns true if represented class is serializable/externalizable and can
     * be instantiated by the serialization runtime--i.e., if it is
     * externalizable and defines a public no-arg constructor, or if it is
     * non-externalizable and its first non-serializable superclass defines an
     * accessible no-arg constructor.  Otherwise, returns false.
     */
    // 判断是否存在生成反序列化对象的构造器
    boolean isInstantiable() {
        requireInitialized();
        return (cons != null);
    }
    
    /**
     * Returns true if represented class is serializable (but not
     * externalizable) and defines a conformant writeObject method.  Otherwise,
     * returns false.
     */
    // 判断当前类中的writeObject方法(Serializable实现类)
    boolean hasWriteObjectMethod() {
        requireInitialized();
        return (writeObjectMethod != null);
    }
    
    /**
     * Returns true if represented class is serializable (but not
     * externalizable) and defines a conformant readObject method.  Otherwise,
     * returns false.
     */
    // 判断当前类中是否包含readObject方法(Serializable类)
    boolean hasReadObjectMethod() {
        requireInitialized();
        return (readObjectMethod != null);
    }
    
    /**
     * Returns true if represented class is serializable (but not
     * externalizable) and defines a conformant readObjectNoData method.
     * Otherwise, returns false.
     */
    // 判断当前类中是否包含readObjectNoData方法(Serializable类)
    boolean hasReadObjectNoDataMethod() {
        requireInitialized();
        return (readObjectNoDataMethod != null);
    }
    
    /**
     * Returns true if represented class is serializable or externalizable and
     * defines a conformant writeReplace method.  Otherwise, returns false.
     */
    // 判断当前类中是否包含writeReplace方法
    boolean hasWriteReplaceMethod() {
        requireInitialized();
        return (writeReplaceMethod != null);
    }
    
    /**
     * Returns true if represented class is serializable or externalizable and
     * defines a conformant readResolve method.  Otherwise, returns false.
     */
    // 判断当前类中是否包含readResolve方法方法
    boolean hasReadResolveMethod() {
        requireInitialized();
        return (readResolveMethod != null);
    }
    
    /**
     * Returns aggregate size (in bytes) of marshalled primitive field values for represented class.
     */
    // 返回fields中基本类型字段所占字节数
    int getPrimDataSize() {
        return primDataSize;
    }
    
    /**
     * Returns number of non-primitive serializable fields of represented class.
     */
    // 返回fields中引用类型字段数量
    int getNumObjFields() {
        return numObjFields;
    }
    
    /**
     * Returns ClassNotFoundException (if any) thrown while attempting to
     * resolve local class corresponding to this class descriptor.
     */
    ClassNotFoundException getResolveException() {
        return resolveEx;
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Matches given set of serializable fields with serializable fields described by the given local class descriptor,
     * and returns a FieldReflector instance capable of setting/getting values from the subset of fields
     * that match (non-matching fields are treated as filler,
     * for which get operations return default values and set operations discard given values).
     * Throws InvalidClassException if unresolvable type conflicts exist between the two sets of fields.
     */
    // 返回待序列化的字段的统计信息
    private static FieldReflector getReflector(ObjectStreamField[] fields, ObjectStreamClass localDesc) throws InvalidClassException {
        /* class irrelevant if no fields */
        // 待序列化的类对象
        Class<?> cl = (localDesc != null && fields.length>0) ? localDesc.cl : null;
        
        // 从reflectors中移除reflectorsQueue中包含的元素
        processQueue(Caches.reflectorsQueue, Caches.reflectors);
        
        // 缓存待序列化字段集的签名信息
        FieldReflectorKey key = new FieldReflectorKey(cl, fields, Caches.reflectorsQueue);
        
        // 获取key关联的软引用
        Reference<?> ref = Caches.reflectors.get(key);
        
        Object entry = null;
        
        // 获取key关联的软引用追踪的对象
        if(ref != null) {
            // 返回此Reference包裹的自定义引用对象，如果该对象已被回收，则返回null
            entry = ref.get();
        }
        
        EntryFuture future = null;
        
        // 需要将其他获取value的线程阻塞住，等为key关联新value后再放行
        if(entry == null) {
            // 为阻塞其他线程而设
            EntryFuture newEntry = new EntryFuture();
            
            // 包装了EntryFuture的软引用
            Reference<?> newRef = new SoftReference<>(newEntry);
            
            do {
                // 之前软引用追踪的对象被回收了
                if(ref != null) {
                    // 从map中移除拥有指定key和value的元素，返回值表示是否移除成功
                    Caches.reflectors.remove(key, ref);
                }
                
                // 重新关联：将指定的元素（key-value）存入Map，并返回旧值，不允许覆盖
                ref = Caches.reflectors.putIfAbsent(key, newRef);
                
                // 如果key已经关联值(来自别的线程干扰)
                if(ref != null) {
                    // 获取当前key关联的软引用追踪的对象
                    entry = ref.get();
                }
            } while(ref != null && entry == null);
            
            // newEntry被成功关联
            if(entry == null) {
                future = newEntry;
            }
        }
        
        // 直接获取到了目标value
        if(entry instanceof FieldReflector) {  // check common case first
            return (FieldReflector) entry;
        } else if(entry instanceof EntryFuture) {
            // 如果key没有关联到有效的value，会阻塞
            entry = ((EntryFuture) entry).get();
        } else if(entry == null) {
            try {
                // 将fields中的字段进行解析、包装(需要结合localDesc中的待序列化字段信息)
                ObjectStreamField[] matchFields = matchFields(fields, localDesc);
                // 即将为key关联的有效value
                entry = new FieldReflector(matchFields);
            } catch(Throwable th) {
                entry = th;
            }
            
            /*
             * 此刻，key已经准备好关联有效的value(entry)，
             * 因此可以唤醒所有阻塞在((EntryFuture) entry).get()上的线程了
             */
            future.set(entry);
            
            // 将指定的元素（key-value）存入Map，并返回旧值，允许覆盖
            Caches.reflectors.put(key, new SoftReference<>(entry));
        }
        
        // 正常返回
        if(entry instanceof FieldReflector) {
            return (FieldReflector) entry;
        } else if(entry instanceof InvalidClassException) {
            throw (InvalidClassException) entry;
        } else if(entry instanceof RuntimeException) {
            throw (RuntimeException) entry;
        } else if(entry instanceof Error) {
            throw (Error) entry;
        } else {
            throw new InternalError("unexpected entry: " + entry);
        }
    }
    
    /**
     * Matches given set of serializable fields with serializable fields
     * obtained from the given local class descriptor (which contain bindings
     * to reflective Field objects).  Returns list of ObjectStreamFields in
     * which each ObjectStreamField whose signature matches that of a local
     * field contains a Field object for that field; unmatched
     * ObjectStreamFields contain null Field objects.  Shared/unshared settings
     * of the returned ObjectStreamFields also reflect those of matched local
     * ObjectStreamFields.  Throws InvalidClassException if unresolvable type
     * conflicts exist between the two sets of fields.
     */
    // 将fields中的字段进行解析、包装(需要结合localDesc中的待序列化字段信息)
    private static ObjectStreamField[] matchFields(ObjectStreamField[] fields, ObjectStreamClass localDesc) throws InvalidClassException {
        // 获取序列化描述符localDesc中的待序列化字段
        ObjectStreamField[] localFields = (localDesc != null) ? localDesc.fields : NO_FIELDS;
        
        /*
         * Even if fields == localFields, we cannot simply return localFields
         * here.  In previous implementations of serialization,
         * ObjectStreamField.getType() returned Object.class if the
         * ObjectStreamField represented a non-primitive field and belonged to
         * a non-local class descriptor.  To preserve this (questionable)
         * behavior, the ObjectStreamField instances returned by matchFields
         * cannot report non-primitive types other than Object.class; hence
         * localFields cannot be returned directly.
         */
        
        ObjectStreamField[] matches = new ObjectStreamField[fields.length];
        
        // 遍历fields中的待序列化字段
        for(int i = 0; i<fields.length; i++) {
            ObjectStreamField f = fields[i];
            ObjectStreamField m = null;
            
            // 遍历序列化描述符localDesc中的待序列化字段
            for(ObjectStreamField lf : localFields) {
                // 遇到同名字段
                if(f.getName().equals(lf.getName())) {
                    if((f.isPrimitive() || lf.isPrimitive()) && f.getTypeCode() != lf.getTypeCode()) {
                        throw new InvalidClassException(localDesc.name, "incompatible types for field " + f.getName());
                    }
                    
                    if(lf.getField() != null) {
                        // 如果待序列化的字段不为null，则取出该字段，直接进行包装（不显示类型）
                        m = new ObjectStreamField(lf.getField(), lf.isUnshared(), false);
                    } else {
                        // 如果为null，使用另外一种包装方式
                        m = new ObjectStreamField(lf.getName(), lf.getSignature(), lf.isUnshared());
                    }
                }
            }
            
            // 如果在localDesc中未找到与fields中的字段同名的字段
            if(m == null) {
                // 对fields中的字段进行包装（非共享）
                m = new ObjectStreamField(f.getName(), f.getSignature(), false);
            }
            
            m.setOffset(f.getOffset());
            
            // 添加包装后的字段
            matches[i] = m;
        }
        
        return matches;
    }
    
    /**
     * Throws InternalError if not initialized.
     */
    // 确保序列化描述符已经初始化
    private final void requireInitialized() {
        if(!initialized) {
            throw new InternalError("Unexpected call when not initialized");
        }
    }
    
    /**
     * Calculates and sets serializable field offsets, as well as primitive
     * data size and object field count totals.  Throws InvalidClassException
     * if fields are illegally ordered.
     */
    /*
     * 统计fields中原始类型字段所占字节数与引用类型字段的数量，
     * 并校验fields中的字段顺序（原始类型排在前面，引用类型排在后面）。
     */
    private void computeFieldOffsets() throws InvalidClassException {
        primDataSize = 0;   // 统计原始类型字段所占字节数
        numObjFields = 0;   // 统计引用类型字段数量
        
        int firstObjIndex = -1; // fields中首个引用类型字段的索引
        
        // 遍历待序列化字段的序列化描述符信息
        for(int i = 0; i<fields.length; i++) {
            ObjectStreamField f = fields[i];
            
            // 获取字段签名
            switch(f.getTypeCode()) {
                case 'Z':   // boolean.class
                case 'B':   // byte.class
                    f.setOffset(primDataSize++);
                    break;
                
                case 'C':   // char.class
                case 'S':   // short.class
                    f.setOffset(primDataSize);
                    primDataSize += 2;
                    break;
                
                case 'I':   // int.class
                case 'F':   // float.class
                    f.setOffset(primDataSize);
                    primDataSize += 4;
                    break;
                
                case 'J':   // long.class
                case 'D':   // double.class
                    f.setOffset(primDataSize);
                    primDataSize += 8;
                    break;
                
                case '[':   // 数组
                case 'L':   // 其他引用类型
                    f.setOffset(numObjFields++);
                    if(firstObjIndex == -1) {
                        firstObjIndex = i;
                    }
                    break;
                
                default:
                    throw new InternalError();
            }
        }
        
        // 要求原始类型排在前面，引用类型排在后面
        if(firstObjIndex != -1 && firstObjIndex + numObjFields != fields.length) {
            throw new InvalidClassException(name, "illegal field order");
        }
    }
    
    /**
     * Creates a PermissionDomain that grants no permission.
     */
    private ProtectionDomain noPermissionsDomain() {
        PermissionCollection perms = new Permissions();
        perms.setReadOnly();
        return new ProtectionDomain(null, perms);
    }
    
    /**
     * Aggregate the ProtectionDomains of all the classes that separate
     * a concrete class {@code cl} from its ancestor's class declaring
     * a constructor {@code cons}.
     *
     * If {@code cl} is defined by the boot loader, or the constructor
     * {@code cons} is declared by {@code cl}, or if there is no security
     * manager, then this method does nothing and {@code null} is returned.
     *
     * @param cons A constructor declared by {@code cl} or one of its
     *             ancestors.
     * @param cl   A concrete class, which is either the class declaring
     *             the constructor {@code cons}, or a serializable subclass
     *             of that class.
     *
     * @return An array of ProtectionDomain representing the set of
     * ProtectionDomain that separate the concrete class {@code cl}
     * from its ancestor's declaring {@code cons}, or {@code null}.
     */
    private ProtectionDomain[] getProtectionDomains(Constructor<?> cons, Class<?> cl) {
        ProtectionDomain[] domains = null;
        if(cons != null && cl.getClassLoader() != null && System.getSecurityManager() != null) {
            Class<?> cls = cl;
            Class<?> fnscl = cons.getDeclaringClass();
            Set<ProtectionDomain> pds = null;
            while(cls != fnscl) {
                ProtectionDomain pd = cls.getProtectionDomain();
                if(pd != null) {
                    if(pds == null) {
                        pds = new HashSet<>();
                    }
                    pds.add(pd);
                }
                
                cls = cls.getSuperclass();
                if(cls == null) {
                    // that's not supposed to happen
                    // make a ProtectionDomain with no permission.
                    // should we throw instead?
                    if(pds == null) {
                        pds = new HashSet<>();
                    } else {
                        pds.clear();
                    }
                    
                    pds.add(noPermissionsDomain());
                    break;
                }
            }
            
            if(pds != null) {
                domains = pds.toArray(new ProtectionDomain[0]);
            }
        }
        
        return domains;
    }
    
    /**
     * Removes from the specified map any keys that have been enqueued
     * on the specified reference queue.
     */
    // 从map中移除queue中包含的元素
    static void processQueue(ReferenceQueue<Class<?>> queue, ConcurrentMap<? extends WeakReference<Class<?>>, ?> map) {
        Reference<? extends Class<?>> ref;
        
        // 从ReferenceQueue中删除一个Reference并将其返回
        while((ref = queue.poll()) != null) {
            // 移除拥有指定key的元素
            map.remove(ref);
        }
    }
    
    /**
     * Returns true if classes are defined in the same runtime package, false
     * otherwise.
     */
    private static boolean packageEquals(Class<?> cl1, Class<?> cl2) {
        return (cl1.getClassLoader() == cl2.getClassLoader() && cl1.getPackageName().equals(cl2.getPackageName()));
    }
    
    /**
     * Compares class names for equality, ignoring package names.  Returns true
     * if class names equal, false otherwise.
     */
    private static boolean classNamesEqual(String name1, String name2) {
        int idx1 = name1.lastIndexOf('.') + 1;
        int idx2 = name2.lastIndexOf('.') + 1;
        int len1 = name1.length() - idx1;
        int len2 = name2.length() - idx2;
        return len1 == len2 && name1.regionMatches(idx1, name2, idx2, len1);
    }
    
    /**
     * Convenience method for throwing an exception that is either a
     * RuntimeException, Error, or of some unexpected type (in which case it is
     * wrapped inside an IOException).
     */
    private static void throwMiscException(Throwable th) throws IOException {
        if(th instanceof RuntimeException) {
            throw (RuntimeException) th;
        } else if(th instanceof Error) {
            throw (Error) th;
        } else {
            throw new IOException("unexpected exception type", th);
        }
    }
    
    /**
     * Returns JVM type signature for given list of parameters and return type.
     */
    private static String getMethodSignature(Class<?>[] paramTypes, Class<?> retType) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for(int i = 0; i<paramTypes.length; i++) {
            appendClassSignature(sb, paramTypes[i]);
        }
        sb.append(')');
        appendClassSignature(sb, retType);
        return sb.toString();
    }
    
    /**
     * Returns explicit serial version UID value declared by given class, or
     * null if none.
     */
    // 获取类对象cl的序列化编号，即由static和final修饰的名为serialVersionUID字段的值
    private static Long getDeclaredSUID(Class<?> cl) {
        try {
            Field f = cl.getDeclaredField("serialVersionUID");
            
            int mask = Modifier.STATIC | Modifier.FINAL;
            
            // 要求serialVersionUID字段由static和final修饰
            if((f.getModifiers() & mask) == mask) {
                f.setAccessible(true);
                return f.getLong(null);
            }
        } catch(Exception ex) {
        }
        
        return null;
    }
    
    /**
     * Computes the default serial version UID value for the given class.
     */
    // 为给定的待序列化类计算一个默认的序列化编号
    private static long computeDefaultSUID(Class<?> cl) {
        // 对于非序列化类或者代理类，直接返回0
        if(!Serializable.class.isAssignableFrom(cl) || Proxy.isProxyClass(cl)) {
            return 0L;
        }
        
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            
            dout.writeUTF(cl.getName());
            
            int classMods = cl.getModifiers() & (Modifier.PUBLIC | Modifier.FINAL | Modifier.INTERFACE | Modifier.ABSTRACT);
            
            /*
             * compensate for javac bug in which ABSTRACT bit was set for an
             * interface only if the interface declared methods
             */
            Method[] methods = cl.getDeclaredMethods();
            if((classMods & Modifier.INTERFACE) != 0) {
                classMods = (methods.length>0) ? (classMods | Modifier.ABSTRACT) : (classMods & ~Modifier.ABSTRACT);
            }
            dout.writeInt(classMods);
            
            if(!cl.isArray()) {
                /*
                 * compensate for change in 1.2FCS in which
                 * Class.getInterfaces() was modified to return Cloneable and
                 * Serializable for array classes.
                 */
                Class<?>[] interfaces = cl.getInterfaces();
                String[] ifaceNames = new String[interfaces.length];
                for(int i = 0; i<interfaces.length; i++) {
                    ifaceNames[i] = interfaces[i].getName();
                }
                Arrays.sort(ifaceNames);
                for(String ifaceName : ifaceNames) {
                    dout.writeUTF(ifaceName);
                }
            }
            
            Field[] fields = cl.getDeclaredFields();
            MemberSignature[] fieldSigs = new MemberSignature[fields.length];
            for(int i = 0; i<fields.length; i++) {
                fieldSigs[i] = new MemberSignature(fields[i]);
            }
            Arrays.sort(fieldSigs, new Comparator<>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    return ms1.name.compareTo(ms2.name);
                }
            });
            for(MemberSignature sig : fieldSigs) {
                int mods = sig.member.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC | Modifier.FINAL | Modifier.VOLATILE | Modifier.TRANSIENT);
                if(((mods & Modifier.PRIVATE) == 0) || ((mods & (Modifier.STATIC | Modifier.TRANSIENT)) == 0)) {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature);
                }
            }
            
            if(hasStaticInitializer(cl)) {
                dout.writeUTF("<clinit>");
                dout.writeInt(Modifier.STATIC);
                dout.writeUTF("()V");
            }
            
            Constructor<?>[] cons = cl.getDeclaredConstructors();
            MemberSignature[] consSigs = new MemberSignature[cons.length];
            for(int i = 0; i<cons.length; i++) {
                consSigs[i] = new MemberSignature(cons[i]);
            }
            Arrays.sort(consSigs, new Comparator<>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    return ms1.signature.compareTo(ms2.signature);
                }
            });
            for(MemberSignature sig : consSigs) {
                int mods = sig.member.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.ABSTRACT | Modifier.STRICT);
                if((mods & Modifier.PRIVATE) == 0) {
                    dout.writeUTF("<init>");
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature.replace('/', '.'));
                }
            }
            
            MemberSignature[] methSigs = new MemberSignature[methods.length];
            for(int i = 0; i<methods.length; i++) {
                methSigs[i] = new MemberSignature(methods[i]);
            }
            Arrays.sort(methSigs, new Comparator<>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    int comp = ms1.name.compareTo(ms2.name);
                    if(comp == 0) {
                        comp = ms1.signature.compareTo(ms2.signature);
                    }
                    return comp;
                }
            });
            for(MemberSignature sig : methSigs) {
                int mods = sig.member.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.ABSTRACT | Modifier.STRICT);
                if((mods & Modifier.PRIVATE) == 0) {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature.replace('/', '.'));
                }
            }
            
            dout.flush();
            
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] hashBytes = md.digest(bout.toByteArray());
            long hash = 0;
            for(int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch(IOException ex) {
            throw new InternalError(ex);
        } catch(NoSuchAlgorithmException ex) {
            throw new SecurityException(ex.getMessage());
        }
    }
    
    /**
     * Returns true if the given class defines a static initializer method, false otherwise.
     */
    // 判断cl是否包含静态初始化块
    private static native boolean hasStaticInitializer(Class<?> cl);
    
    /**
     * Initializes native code.
     */
    private static native void initNative();
    
    
    
    /**
     * Return a string describing this ObjectStreamClass.
     */
    public String toString() {
        return name + ": static final long serialVersionUID = " + getSerialVersionUID() + "L;";
    }
    
    
    
    
    
    
    /**
     * Class representing the portion of an object's serialized form allotted to data described by a given class descriptor.
     * If "hasData" is false, the object's serialized form does not contain data associated with the class descriptor.
     */
    // 数据槽，包含了序列化描述符，hasData指示该处是否包含有效数据
    static class ClassDataSlot {
        /** class descriptor "occupying" this slot */
        final ObjectStreamClass desc;
        
        /** true if serialized form includes data for this slot's descriptor */
        final boolean hasData;
        
        ClassDataSlot(ObjectStreamClass desc, boolean hasData) {
            this.desc = desc;
            this.hasData = hasData;
        }
    }
    
    /**
     * Class for setting and retrieving serializable field values in batch.
     *
     * REMIND: dynamically generate these?
     */
    // 待序列化的字段的统计信息：用于批量设置和检索可序列化的字段值的类
    private static class FieldReflector {
        /** handle for performing unsafe operations */
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        
        /** fields to operate on */
        private final ObjectStreamField[] fields;   // 待序列化字段
        
        /** number of primitive fields */
        private final int numPrimFields;    // 基本类型字段数量
        /** unsafe field keys for reading fields - may contain dupes */
        private final long[] readKeys;      // 字段的JVM偏移地址（可能包含重复数据）
        /** unsafe fields keys for writing fields - no dupes */
        private final long[] writeKeys;     // 字段的JVM偏移地址（不包含重复数据）
        /** field data offsets */
        private final int[] offsets;        // 字段在对象中的偏移位置
        /** field type codes */
        private final char[] typeCodes;     // 字段签名
        /** field types */
        private final Class<?>[] types;     // 字段类型(只存储引用类型)
        
        /**
         * Constructs FieldReflector capable of setting/getting values from the
         * subset of fields whose ObjectStreamFields contain non-null
         * reflective Field objects.  ObjectStreamFields with null Fields are
         * treated as filler, for which get operations return default values
         * and set operations discard given values.
         */
        FieldReflector(ObjectStreamField[] fields) {
            this.fields = fields;
            int nfields = fields.length;
            readKeys = new long[nfields];
            writeKeys = new long[nfields];
            offsets = new int[nfields];
            typeCodes = new char[nfields];
            ArrayList<Class<?>> typeList = new ArrayList<>();
            Set<Long> usedKeys = new HashSet<>();
            
            // 遍历待序列化字段
            for(int i = 0; i<nfields; i++) {
                ObjectStreamField f = fields[i];
                Field rf = f.getField();
                // 获取非静态字段f的JVM偏移地址
                long key = (rf != null) ? unsafe.objectFieldOffset(rf) : Unsafe.INVALID_FIELD_OFFSET;
                readKeys[i] = key;
                writeKeys[i] = usedKeys.add(key) ? key : Unsafe.INVALID_FIELD_OFFSET;
                offsets[i] = f.getOffset();
                typeCodes[i] = f.getTypeCode();
                
                // 如果字段的类型不是基本类型
                if(!f.isPrimitive()) {
                    typeList.add((rf != null) ? rf.getType() : null);
                }
            }
            
            types = typeList.toArray(new Class<?>[typeList.size()]);
            numPrimFields = nfields - types.length;
        }
        
        /**
         * Returns list of ObjectStreamFields representing fields operated on
         * by this reflector.  The shared/unshared values and Field objects
         * contained by ObjectStreamFields in the list reflect their bindings
         * to locally defined serializable fields.
         */
        ObjectStreamField[] getFields() {
            return fields;
        }
        
        /**
         * Fetches the serializable primitive field values of object obj and
         * marshals them into byte array buf starting at offset 0.  The caller
         * is responsible for ensuring that obj is of the proper type.
         */
        // 获取待序列化的基本类型字段的值
        void getPrimFieldValues(Object obj, byte[] buf) {
            if(obj == null) {
                throw new NullPointerException();
            }
            
            /*
             * assuming checkDefaultSerialize() has been called on the class descriptor this FieldReflector was obtained from,
             * no field keys in array should be equal to Unsafe.INVALID_FIELD_OFFSET.
             */
            for(int i = 0; i<numPrimFields; i++) {
                long key = readKeys[i]; // 字段的JVM偏移地址（可能包含重复数据）
                int off = offsets[i];   // 字段在对象中的偏移位置
                switch(typeCodes[i]) {
                    case 'Z':
                        Bits.putBoolean(buf, off, unsafe.getBoolean(obj, key));
                        break;
                    
                    case 'B':
                        buf[off] = unsafe.getByte(obj, key);
                        break;
                    
                    case 'C':
                        Bits.putChar(buf, off, unsafe.getChar(obj, key));
                        break;
                    
                    case 'S':
                        Bits.putShort(buf, off, unsafe.getShort(obj, key));
                        break;
                    
                    case 'I':
                        Bits.putInt(buf, off, unsafe.getInt(obj, key));
                        break;
                    
                    case 'F':
                        Bits.putFloat(buf, off, unsafe.getFloat(obj, key));
                        break;
                    
                    case 'J':
                        Bits.putLong(buf, off, unsafe.getLong(obj, key));
                        break;
                    
                    case 'D':
                        Bits.putDouble(buf, off, unsafe.getDouble(obj, key));
                        break;
                    
                    default:
                        throw new InternalError();
                }
            }
        }
        
        /**
         * Sets the serializable primitive fields of object obj using values
         * unmarshalled from byte array buf starting at offset 0.  The caller
         * is responsible for ensuring that obj is of the proper type.
         */
        void setPrimFieldValues(Object obj, byte[] buf) {
            if(obj == null) {
                throw new NullPointerException();
            }
            
            for(int i = 0; i<numPrimFields; i++) {
                long key = writeKeys[i];
                if(key == Unsafe.INVALID_FIELD_OFFSET) {
                    continue;           // discard value
                }
                
                int off = offsets[i];
                switch(typeCodes[i]) {
                    case 'Z':
                        unsafe.putBoolean(obj, key, Bits.getBoolean(buf, off));
                        break;
                    
                    case 'B':
                        unsafe.putByte(obj, key, buf[off]);
                        break;
                    
                    case 'C':
                        unsafe.putChar(obj, key, Bits.getChar(buf, off));
                        break;
                    
                    case 'S':
                        unsafe.putShort(obj, key, Bits.getShort(buf, off));
                        break;
                    
                    case 'I':
                        unsafe.putInt(obj, key, Bits.getInt(buf, off));
                        break;
                    
                    case 'F':
                        unsafe.putFloat(obj, key, Bits.getFloat(buf, off));
                        break;
                    
                    case 'J':
                        unsafe.putLong(obj, key, Bits.getLong(buf, off));
                        break;
                    
                    case 'D':
                        unsafe.putDouble(obj, key, Bits.getDouble(buf, off));
                        break;
                    
                    default:
                        throw new InternalError();
                }
            }
        }
        
        /**
         * Fetches the serializable object field values of object obj and
         * stores them in array vals starting at offset 0.  The caller is
         * responsible for ensuring that obj is of the proper type.
         */
        // 获取待序列化的引用类型字段的值
        void getObjFieldValues(Object obj, Object[] vals) {
            if(obj == null) {
                throw new NullPointerException();
            }
            
            /*
             * assuming checkDefaultSerialize() has been called on the class descriptor this FieldReflector was obtained from,
             * no field keys in array should be equal to Unsafe.INVALID_FIELD_OFFSET.
             */
            for(int i = numPrimFields; i<fields.length; i++) {
                switch(typeCodes[i]) {
                    case 'L':
                    case '[':
                        vals[offsets[i]] = unsafe.getObject(obj, readKeys[i]);
                        break;
                    
                    default:
                        throw new InternalError();
                }
            }
        }
        
        /**
         * Sets the serializable object fields of object obj using values from
         * array vals starting at offset 0.  The caller is responsible for
         * ensuring that obj is of the proper type; however, attempts to set a
         * field with a value of the wrong type will trigger an appropriate
         * ClassCastException.
         */
        void setObjFieldValues(Object obj, Object[] vals) {
            setObjFieldValues(obj, vals, false);
        }
        
        /**
         * Checks that the given values, from array vals starting at offset 0,
         * are assignable to the given serializable object fields.
         *
         * @throws ClassCastException if any value is not assignable
         */
        void checkObjectFieldValueTypes(Object obj, Object[] vals) {
            setObjFieldValues(obj, vals, true);
        }
        
        private void setObjFieldValues(Object obj, Object[] vals, boolean dryRun) {
            if(obj == null) {
                throw new NullPointerException();
            }
            
            for(int i = numPrimFields; i<fields.length; i++) {
                long key = writeKeys[i];
                if(key == Unsafe.INVALID_FIELD_OFFSET) {
                    continue;           // discard value
                }
                
                switch(typeCodes[i]) {
                    case 'L':
                    case '[':
                        Object val = vals[offsets[i]];
                        if(val != null && !types[i - numPrimFields].isInstance(val)) {
                            Field f = fields[i].getField();
                            throw new ClassCastException("cannot assign instance of " + val.getClass().getName() + " to field " + f.getDeclaringClass().getName() + "." + f.getName() + " of type " + f.getType().getName() + " in instance of " + obj.getClass().getName());
                        }
                        if(!dryRun) {
                            unsafe.putObject(obj, key, val);
                        }
                        break;
                    
                    default:
                        throw new InternalError();
                }
            }
        }
    }
    
    /**
     * Contains information about InvalidClassException instances to be thrown
     * when attempting operations on an invalid class. Note that instances of
     * this class are immutable and are potentially shared among
     * ObjectStreamClass instances.
     */
    // 序列化异常信息
    private static class ExceptionInfo {
        private final String className;
        private final String message;
        
        ExceptionInfo(String cn, String msg) {
            className = cn;
            message = msg;
        }
        
        /**
         * Returns (does not throw) an InvalidClassException instance created
         * from the information in this object, suitable for being thrown by
         * the caller.
         */
        InvalidClassException newInvalidClassException() {
            return new InvalidClassException(className, message);
        }
    }
    
    
    
    /**
     * Placeholder used in class descriptor and field reflector lookup tables
     * for an entry in the process of being initialized.
     * (Internal) callers which receive an EntryFuture belonging to another thread as the result
     * of a lookup should call the get() method of the EntryFuture;
     * this will return the actual entry once it is ready for use and has been set().
     * To conserve objects, EntryFutures synchronize on themselves.
     */
    // 为key关联好value之前，阻塞其他获取value的线程
    private static class EntryFuture {
        private final Thread owner = Thread.currentThread();
        private static final Object unset = new Object();
        private Object entry = unset;
        
        
        /**
         * Attempts to set the value contained by this EntryFuture.  If the
         * EntryFuture's value has not been set already, then the value is
         * saved, any callers blocked in the get() method are notified, and
         * true is returned.  If the value has already been set, then no saving
         * or notification occurs, and false is returned.
         */
        synchronized boolean set(Object entry) {
            if(this.entry != unset) {
                return false;
            }
            this.entry = entry;
            notifyAll();
            return true;
        }
        
        /**
         * Returns the value contained by this EntryFuture, blocking if
         * necessary until a value is set.
         */
        synchronized Object get() {
            boolean interrupted = false;
            
            while(entry == unset) {
                try {
                    wait();
                } catch(InterruptedException ex) {
                    interrupted = true;
                }
            }
            
            if(interrupted) {
                AccessController.doPrivileged(new PrivilegedAction<>() {
                    public Void run() {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                });
            }
            
            return entry;
        }
        
        /**
         * Returns the thread that created this EntryFuture.
         */
        Thread getOwner() {
            return owner;
        }
    }
    
    private static class Caches {
        /** cache mapping local classes -> descriptors */
        static final ConcurrentMap<WeakClassKey, Reference<?>> localDescs = new ConcurrentHashMap<>();
        
        /** queue for WeakReferences to local classes */
        // 用于WeakClassKey的弱引用队列
        private static final ReferenceQueue<Class<?>> localDescsQueue = new ReferenceQueue<>();
        
        
        /** cache mapping field group/local desc pairs -> field reflectors */
        static final ConcurrentMap<FieldReflectorKey, Reference<?>> reflectors = new ConcurrentHashMap<>();
        
        /** queue for WeakReferences to field reflectors keys */
        // 用于FieldReflectorKey的弱引用队列
        private static final ReferenceQueue<Class<?>> reflectorsQueue = new ReferenceQueue<>();
    }
    
    /**
     * Weak key for Class objects.
     **/
    static class WeakClassKey extends WeakReference<Class<?>> {
        /**
         * saved value of the referent's identity hash code, to maintain
         * a consistent hash code after the referent has been cleared
         */
        private final int hash;
        
        /**
         * Create a new WeakClassKey to the given object, registered
         * with a queue.
         */
        WeakClassKey(Class<?> cl, ReferenceQueue<Class<?>> refQueue) {
            super(cl, refQueue);
            hash = System.identityHashCode(cl);
        }
        
        /**
         * Returns the identity hash code of the original referent.
         */
        public int hashCode() {
            return hash;
        }
        
        /**
         * Returns true if the given object is this identical
         * WeakClassKey instance, or, if this object's referent has not
         * been cleared, if the given object is another WeakClassKey
         * instance with the identical non-null referent as this one.
         */
        public boolean equals(Object obj) {
            if(obj == this) {
                return true;
            }
            
            if(obj instanceof WeakClassKey) {
                Object referent = get();
                return (referent != null) && (referent == ((WeakClassKey) obj).get());
            } else {
                return false;
            }
        }
    }
    
    /**
     * FieldReflector cache lookup key.
     * Keys are considered equal if they refer to the same class and equivalent field formats.
     */
    private static class FieldReflectorKey extends WeakReference<Class<?>> {
        private final boolean nullClass;    // 待序列化的类对象是否为null
        private final String sigs;          // (全部)待序列化字段名称+签名
        private final int hash;
        
        // 待序列化的类对象会被弱引用追踪
        FieldReflectorKey(Class<?> cl, ObjectStreamField[] fields, ReferenceQueue<Class<?>> queue) {
            super(cl, queue);
            
            nullClass = (cl == null);
            
            StringBuilder sbuf = new StringBuilder();
            for(ObjectStreamField field : fields) {
                sbuf.append(field.getName())        // 添加字段名称
                    .append(field.getSignature());  // 添加字段签名
            }
            sigs = sbuf.toString();
            
            hash = System.identityHashCode(cl) + sigs.hashCode();
        }
        
        public int hashCode() {
            return hash;
        }
        
        public boolean equals(Object obj) {
            if(obj == this) {
                return true;
            }
            
            if(obj instanceof FieldReflectorKey) {
                FieldReflectorKey other = (FieldReflectorKey) obj;
                Class<?> referent;
                return (nullClass ? other.nullClass : ((referent = get()) != null) && (referent == other.get())) && sigs.equals(other.sigs);
            } else {
                return false;
            }
        }
    }
    
    
    
    /**
     * Class for computing and caching field/constructor/method signatures
     * during serialVersionUID calculation.
     */
    private static class MemberSignature {
        
        public final Member member;
        public final String name;
        public final String signature;
        
        public MemberSignature(Field field) {
            member = field;
            name = field.getName();
            signature = getClassSignature(field.getType());
        }
        
        public MemberSignature(Constructor<?> cons) {
            member = cons;
            name = cons.getName();
            signature = getMethodSignature(cons.getParameterTypes(), Void.TYPE);
        }
        
        public MemberSignature(Method meth) {
            member = meth;
            name = meth.getName();
            signature = getMethodSignature(meth.getParameterTypes(), meth.getReturnType());
        }
    }
    
}
