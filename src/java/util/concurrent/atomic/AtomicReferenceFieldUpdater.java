/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.atomic;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import jdk.internal.misc.Unsafe;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.reflect.misc.ReflectUtil;

/**
 * A reflection-based utility that enables atomic updates to
 * designated {@code volatile} reference fields of designated
 * classes.  This class is designed for use in atomic data structures
 * in which several reference fields of the same node are
 * independently subject to atomic updates. For example, a tree node
 * might be declared as
 *
 * <pre> {@code
 * class Node {
 *   private volatile Node left, right;
 *
 *   private static final AtomicReferenceFieldUpdater<Node, Node> leftUpdater =
 *     AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "left");
 *   private static AtomicReferenceFieldUpdater<Node, Node> rightUpdater =
 *     AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "right");
 *
 *   Node getLeft() { return left; }
 *   boolean compareAndSetLeft(Node expect, Node update) {
 *     return leftUpdater.compareAndSet(this, expect, update);
 *   }
 *   // ... and so on
 * }}</pre>
 *
 * <p>Note that the guarantees of the {@code compareAndSet}
 * method in this class are weaker than in other atomic classes.
 * Because this class cannot ensure that all uses of the field
 * are appropriate for purposes of atomic access, it can
 * guarantee atomicity only with respect to other invocations of
 * {@code compareAndSet} and {@code set} on the same updater.
 *
 * <p>Object arguments for parameters of type {@code T} that are not
 * instances of the class passed to {@link #newUpdater} will result in
 * a {@link ClassCastException} being thrown.
 *
 * @param <T> The type of the object holding the updatable field
 * @param <V> The type of the field
 *
 * @author Doug Lea
 * @since 1.5
 */
// 对目标类中volatile修饰的名为fieldName的vclass类型的引用字段进行原子操作
public abstract class AtomicReferenceFieldUpdater<T, V> {
    
    /*▼ 构造器/工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Protected do-nothing constructor for use by subclasses.
     */
    protected AtomicReferenceFieldUpdater() {
    }
    
    /**
     * Creates and returns an updater for objects with the given field.
     * The Class arguments are needed to check that reflective types and
     * generic types match.
     *
     * @param tclass    the class of the objects holding the field
     * @param vclass    the class of the field
     * @param fieldName the name of the field to be updated
     * @param <U>       the type of instances of tclass
     * @param <W>       the type of instances of vclass
     *
     * @return the updater
     *
     * @throws ClassCastException       if the field is of the wrong type
     * @throws IllegalArgumentException if the field is not volatile
     * @throws RuntimeException         with a nested reflection-based
     *                                  exception if the class does not hold field or is the wrong type,
     *                                  or the field is inaccessible to the caller according to Java language
     *                                  access control
     */
    @CallerSensitive
    public static <U, W> AtomicReferenceFieldUpdater<U, W> newUpdater(Class<U> tclass, Class<W> vclass, String fieldName) {
        return new AtomicReferenceFieldUpdaterImpl<U, W>(tclass, vclass, fieldName, Reflection.getCallerClass());
    }
    
    /*▲ 构造器/工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current value held in the field of the given object
     * managed by this updater.
     *
     * @param obj An object whose field to get
     *
     * @return the current value
     */
    // 获取对象obj中的目标字段值
    public abstract V get(T obj);
    
    /*▲ 获取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets the field of the given object managed by this updater to the
     * given updated value. This operation is guaranteed to act as a volatile
     * store with respect to subsequent invocations of {@code compareAndSet}.
     *
     * @param obj      An object whose field to set
     * @param newValue the new value
     */
    // 设置对象obj中的目标字段的值为newValue
    public abstract void set(T obj, V newValue);
    
    /**
     * Eventually sets the field of the given object managed by this
     * updater to the given updated value.
     *
     * @param obj      An object whose field to set
     * @param newValue the new value
     *
     * @since 1.6
     */
    // 设置对象obj中的目标字段的值为newValue
    public abstract void lazySet(T obj, V newValue);
    
    /**
     * Atomically sets the field of the given object managed by this updater
     * to the given value and returns the old value.
     *
     * @param obj      An object whose field to get and set
     * @param newValue the new value
     *
     * @return the previous value
     */
    // 设置对象obj中的目标字段的值为newValue，并返回旧值
    public V getAndSet(T obj, V newValue) {
        V prev;
        do {
            prev = get(obj);
        } while(!compareAndSet(obj, prev, newValue));
        return prev;
    }
    
    /*▲ 设置值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 比较并更新 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Atomically sets the field of the given object managed by this updater
     * to the given updated value if the current value {@code ==} the
     * expected value. This method is guaranteed to be atomic with respect to
     * other calls to {@code compareAndSet} and {@code set}, but not
     * necessarily with respect to other changes in the field.
     *
     * @param obj    An object whose field to conditionally set
     * @param expect the expected value
     * @param update the new value
     *
     * @return {@code true} if successful
     */
    // 如果obj中目标字段的值为expect，则将其更新为update
    public abstract boolean compareAndSet(T obj, V expect, V update);
    
    /**
     * Atomically sets the field of the given object managed by this updater
     * to the given updated value if the current value {@code ==} the
     * expected value. This method is guaranteed to be atomic with respect to
     * other calls to {@code compareAndSet} and {@code set}, but not
     * necessarily with respect to other changes in the field.
     *
     * <p><a href="package-summary.html#weakCompareAndSet">May fail
     * spuriously and does not provide ordering guarantees</a>, so is
     * only rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param obj    An object whose field to conditionally set
     * @param expect the expected value
     * @param update the new value
     *
     * @return {@code true} if successful
     */
    // 如果obj中目标字段的值为expect，则将其更新为update
    public abstract boolean weakCompareAndSet(T obj, V expect, V update);
    
    /*▲ 比较并更新 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ lambda操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the field of the given object managed
     * by this updater with the results of applying the given
     * function, returning the previous value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.
     *
     * @param obj            An object whose field to get and set
     * @param updateFunction a side-effect-free function
     *
     * @return the previous value
     *
     * @since 1.8
     */
    // 对对象obj内的目标字段执行给定的操作。如果操作成功，则返回旧值
    public final V getAndUpdate(T obj, UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = updateFunction.apply(prev);
        } while(!compareAndSet(obj, prev, next));
        return prev;
    }
    
    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the field of the given object managed
     * by this updater with the results of applying the given
     * function, returning the updated value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.
     *
     * @param obj            An object whose field to get and set
     * @param updateFunction a side-effect-free function
     *
     * @return the updated value
     *
     * @since 1.8
     */
    // 对对象obj内的目标字段执行给定的操作。如果操作成功，则返回新值
    public final V updateAndGet(T obj, UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = updateFunction.apply(prev);
        } while(!compareAndSet(obj, prev, next));
        return next;
    }
    
    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the field of the given object managed
     * by this updater with the results of applying the given function
     * to the current and given values, returning the previous value.
     * The function should be side-effect-free, since it may be
     * re-applied when attempted updates fail due to contention among
     * threads.  The function is applied with the current value as its
     * first argument, and the given update as the second argument.
     *
     * @param obj                 An object whose field to get and set
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     *
     * @return the previous value
     *
     * @since 1.8
     */
    // 对对象obj内的目标字段执行给定的操作。如果操作成功，则返回旧值
    public final V getAndAccumulate(T obj, V x, BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.apply(prev, x);
        } while(!compareAndSet(obj, prev, next));
        return prev;
    }
    
    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the field of the given object managed
     * by this updater with the results of applying the given function
     * to the current and given values, returning the updated value.
     * The function should be side-effect-free, since it may be
     * re-applied when attempted updates fail due to contention among
     * threads.  The function is applied with the current value as its
     * first argument, and the given update as the second argument.
     *
     * @param obj                 An object whose field to get and set
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     *
     * @return the updated value
     *
     * @since 1.8
     */
    // 对对象obj内的目标字段执行给定的操作。如果操作成功，则返回新值
    public final V accumulateAndGet(T obj, V x, BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.apply(prev, x);
        } while(!compareAndSet(obj, prev, next));
        return next;
    }
    
    /*▲ lambda操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    private static final class AtomicReferenceFieldUpdaterImpl<T, V> extends AtomicReferenceFieldUpdater<T, V> {
        private static final Unsafe U = Unsafe.getUnsafe();
        
        private final long offset;
        
        /** class holding the field */
        // 待访问字段所在的类
        private final Class<T> tclass;
        
        /**
         * if field is protected, the subclass constructing updater, else
         * the same as tclass
         */
        // 如果从不在同一个包中的子类caller中访问tclass，则cclass==caller，否则，cclass==tclass
        private final Class<?> cclass;
        
        /** field value type */
        // 目标字段类型
        private final Class<V> vclass;
        
        
        
        /*▼ 构造器 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        /*
         * Internal type checks within all update methods contain
         * internal inlined optimizations checking for the common
         * cases where the class is final (in which case a simple
         * getClass comparison suffices) or is of type Object (in
         * which case no check is needed because all objects are
         * instances of Object). The Object case is handled simply by
         * setting vclass to null in constructor.  The targetCheck and
         * updateCheck methods are invoked when these faster
         * screenings fail.
         */
        AtomicReferenceFieldUpdaterImpl(final Class<T> tclass, final Class<V> vclass, final String fieldName, final Class<?> caller) {
            final Field field;
            final Class<?> fieldClass;
            final int modifiers;
            
            try {
                // 返回tclass中指定名称的字段，但不包括父类/父接口中的字段
                field = AccessController.doPrivileged(new PrivilegedExceptionAction<Field>() {
                    public Field run() throws NoSuchFieldException {
                        return tclass.getDeclaredField(fieldName);
                    }
                });
                
                // 获取字段修饰符
                modifiers = field.getModifiers();
                
                // 确保caller可以访问tclass中具有modifiers修饰符的成员，否则会抛异常
                ReflectUtil.ensureMemberAccess(caller, tclass, null, modifiers);
                
                ClassLoader ccl = caller.getClassLoader();
                ClassLoader cl = tclass.getClassLoader();
                
                if((ccl != null) && (ccl != cl) && ((cl == null) || !isAncestor(cl, ccl))) {
                    // 使用系统安全管理器检查当前类对tclass所在的包的访问权限
                    ReflectUtil.checkPackageAccess(tclass);
                }
                
                // 获取字段类型[类型擦除]
                fieldClass = field.getType();
            } catch(PrivilegedActionException pae) {
                throw new RuntimeException(pae.getException());
            } catch(Exception ex) {
                throw new RuntimeException(ex);
            }
            
            if(vclass != fieldClass) {
                throw new ClassCastException();
            }
            
            // 目标字段必须为引用类型
            if(vclass.isPrimitive()) {
                throw new IllegalArgumentException("Must be reference type");
            }
            
            // 目标字段修饰符必须带有volatile
            if(!Modifier.isVolatile(modifiers)) {
                throw new IllegalArgumentException("Must be volatile type");
            }
            
            // Access to protected field members is restricted to receivers only
            // of the accessing class, or one of its subclasses, and the
            // accessing class must in turn be a subclass (or package sibling)
            // of the protected member's defining class.
            // If the updater refers to a protected field of a declaring class
            // outside the current package, the receiver argument will be
            // narrowed to the type of the accessing class.
            this.cclass = (Modifier.isProtected(modifiers) && tclass.isAssignableFrom(caller) && !isSamePackage(tclass, caller)) ? caller : tclass;
            this.tclass = tclass;
            this.vclass = vclass;
            this.offset = U.objectFieldOffset(field);
        }
        
        /*▲ 构造器 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 获取值 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 获取对象obj中的目标字段值
        @SuppressWarnings("unchecked")
        public final V get(T obj) {
            accessCheck(obj);
            return (V) U.getObjectVolatile(obj, offset);
        }
        
        /*▲ 获取值 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 设置值 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 设置对象obj中的目标字段的值为newValue
        public final void set(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            U.putObjectVolatile(obj, offset, newValue);
        }
        
        // 设置对象obj中的目标字段的值为newValue
        public final void lazySet(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            U.putObjectRelease(obj, offset, newValue);
        }
        
        // 设置对象obj中的目标字段的值为newValue，并返回旧值
        @SuppressWarnings("unchecked")
        public final V getAndSet(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            return (V) U.getAndSetObject(obj, offset, newValue);
        }
        
        /*▲ 设置值 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 比较并更新 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 如果obj中目标字段的值为expect，则将其更新为update
        public final boolean compareAndSet(T obj, V expect, V update) {
            accessCheck(obj);
            valueCheck(update);
            return U.compareAndSetObject(obj, offset, expect, update);
        }
        
        // 如果obj中目标字段的值为expect，则将其更新为update
        public final boolean weakCompareAndSet(T obj, V expect, V update) {
            // same implementation as strong form for now
            accessCheck(obj);
            valueCheck(update);
            return U.compareAndSetObject(obj, offset, expect, update);
        }
        
        /*▲ 比较并更新 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /**
         * Returns true if the second classloader can be found in the first
         * classloader's delegation chain.
         * Equivalent to the inaccessible: first.isAncestor(second).
         */
        // 判断类加载器second是否为first的祖先（second在first的委托链上）
        private static boolean isAncestor(ClassLoader first, ClassLoader second) {
            ClassLoader acl = first;
            do {
                acl = acl.getParent();
                if(second == acl) {
                    return true;
                }
            } while(acl != null);
            return false;
        }
        
        /**
         * Returns true if the two classes have the same class loader and
         * package qualifier
         */
        // 判断class1与class2是否拥有相同的类加载器且位于同一个包
        private static boolean isSamePackage(Class<?> class1, Class<?> class2) {
            return class1.getClassLoader() == class2.getClassLoader()
                && Objects.equals(class1.getPackageName(), class2.getPackageName());
        }
        
        /**
         * Checks that target argument is instance of cclass.  On
         * failure, throws cause.
         */
        // 检查[对象]obj是否为cclass类的实例，不是的话会抛异常
        private final void accessCheck(T obj) {
            if(!cclass.isInstance(obj)) {
                throwAccessCheckException(obj);
            }
        }
        
        /**
         * Throws access exception if accessCheck failed due to
         * protected access, else ClassCastException.
         */
        // 负责抛异常
        private final void throwAccessCheckException(T obj) {
            if(cclass == tclass) {
                throw new ClassCastException();
            } else {
                throw new RuntimeException(new IllegalAccessException("Class " + cclass.getName() + " can not access a protected member of class " + tclass.getName() + " using an instance of " + obj.getClass().getName()));
            }
        }
        
        // 检查[对象]v是否为vclass类的实例，不是的话会抛异常
        private final void valueCheck(V v) {
            if(v != null && !(vclass.isInstance(v))) {
                throwCCE();
            }
        }
        
        // 负责抛异常
        static void throwCCE() {
            throw new ClassCastException();
        }
        
    }
}
