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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.misc.VM;

/**
 * Common utility routines used by both java.lang and java.lang.reflect
 */
// 用于反射过程的工具类
public class Reflection {
    
    /**
     * Used to filter out fields and methods from certain classes from public
     * view, where they are sensitive or they may contain VM-internal objects.
     * These Maps are updated very rarely. Rather than synchronize on
     * each access, we use copy-on-write
     */
    private static volatile Map<Class<?>, String[]> fieldFilterMap;  // 待过滤字段的注册表，内含的字段将被过滤掉
    private static volatile Map<Class<?>, String[]> methodFilterMap; // 待过滤方法的注册表，内含的方法将被过滤掉
    
    
    
    static {
        Map<Class<?>, String[]> map = new HashMap<Class<?>, String[]>();
        map.put(Reflection.class, new String[]{"fieldFilterMap", "methodFilterMap"});
        map.put(System.class, new String[]{"security"});
        map.put(Class.class, new String[]{"classLoader"});
        fieldFilterMap = map;
        
        methodFilterMap = new HashMap<>();
    }
    
    
    
    /**
     * Returns the class of the caller of the method calling this method,
     * ignoring frames associated with java.lang.reflect.Method.invoke() and its implementation.
     */
    /*
     * 返回调用getCallerClass()的方法的调用者所处的类(忽略反射栈帧)
     *
     * public class B {
     *     public static void fun(){
     *         Class clazz = Reflection.getCallerClass();
     *     }
     * }
     *
     * public class A {
     *     public static void main(String[] args) {
     *         B.fun(); // 调用此方法后，fun()中的clazz值显示为A
     *     }
     * }
     *
     */
    @CallerSensitive
    @HotSpotIntrinsicCandidate
    public static native Class<?> getCallerClass();
    
    /**
     * Retrieves the access flags written to the class file.
     * For inner classes these flags may differ from those returned by Class.getModifiers(),
     * which searches the InnerClasses attribute to find the source-level access flags.
     * This is used instead of Class.getModifiers() for run-time access checks due to compatibility reasons; see 4471811.
     * Only the values of the low 13 bits (i.e., a mask of 0x1FFF) are guaranteed to be valid.
     */
    // 返回指定类的访问标记；对于内部类，该标记与Class.getModifiers()返回的标记可能不同
    @HotSpotIntrinsicCandidate
    public static native int getClassAccessFlags(Class<?> clazz);
    
    /**
     * Ensures that access to a member is granted and throws IllegalAccessException if not.
     *
     * @param currentClass the class performing the access
     * @param memberClass  the declaring class of the member being accessed
     * @param targetClass  the class of target object if accessing instance field or method;
     *                     or the declaring class if accessing constructor;
     *                     or null if accessing static field or method
     * @param modifiers    the member's access modifiers
     *
     * @throws IllegalAccessException if access to member is denied
     */
    // 确保可以在currentClass中通过targetClass类型的对象来显式访问memberClass中具有modifiers修饰符的成员(如果访问的是静态成员，则targetClass为null)；否则，会抛异常。
    public static void ensureMemberAccess(Class<?> currentClass, Class<?> memberClass, Class<?> targetClass, int modifiers) throws IllegalAccessException {
        if(!verifyMemberAccess(currentClass, memberClass, targetClass, modifiers)) {
            // 如果无法访问，则抛出一个非法访问异常
            throw newIllegalAccessException(currentClass, memberClass, targetClass, modifiers);
        }
    }
    
    /**
     * Verify access to a member and return {@code true} if it is granted.
     *
     * @param currentClass the class performing the access
     * @param memberClass  the declaring class of the member being accessed
     * @param targetClass  the class of target object if accessing instance field or method;
     *                     or the declaring class if accessing constructor;
     *                     or null if accessing static field or method
     * @param modifiers    the member's access modifiers
     *
     * @return {@code true} if access to member is granted
     */
    /*
     * 判断是否可以在currentClass中通过targetClass类型的对象来显式访问memberClass中具有modifiers修饰符的成员(如果访问的是静态成员，则targetClass为null)。
     *
     * 以显式调用方法fun()为例：
     * currentClass：使用fun()方法的类
     * memberClass ：fun()方法所属的类
     * targetClass ：调用fun()方法的对象所属的类，通常与memberClass相同，或为memberClass的子类；
     *               如果fun()是静态方法，targetClass为null
     * modifiers   ：fun()方法的修饰符
     *
     * 简单示例如下：
     *
     * class memberClass {
     *     public void fun() {}
     * }
     *
     * class targetClass extends memberClass {
     * }
     *
     * class currentClass {
     *      memberClass member = new targetClass();
     *
     *      member.fun();
     * }
     *
     */
    public static boolean verifyMemberAccess(Class<?> currentClass, Class<?> memberClass, Class<?> targetClass, int modifiers) {
        // 访问自身方法，永远成立
        if(currentClass == memberClass) {
            // Always succeeds
            return true;
        }
        
        /*
         * 【大前提】：currentClass与memberClass不是同一个类
         */
        
        // 如果无法在currentClass类中访问memberClass类，直接返回false
        if(!verifyModuleAccess(currentClass.getModule(), memberClass)) {
            return false;
        }
        
        // 是否进行过isSameClassPackage的判断
        boolean gotIsSameClassPackage = false;
        
        // true：currentClass和memberClass处于同一个包(前提是拥有相同的类加载器)
        boolean isSameClassPackage = false;
        
        // 获取memberClass类的访问标记
        int accessFlags = getClassAccessFlags(memberClass);
        
        // 如果memberClass类是包访问权限，且currentClass不在同一个包下，则currentClass无法访问到memberClass
        if(!Modifier.isPublic(accessFlags)) {
            // 判断currentClass和memberClass是否处于同一个包(前提是拥有相同的类加载器)
            isSameClassPackage = isSameClassPackage(currentClass, memberClass);
            gotIsSameClassPackage = true;
            if(!isSameClassPackage) {
                return false;
            }
        }
        
        /*
         * 【大前提】：拿到了类的访问权限
         *
         * 至此，说明currentClass类可以访问到memberClass类，至于能不能访问到memberClass类内的成员，则需要进一步判断
         */
        
        // 如果待访问成员是public的，则currentClass可以访问到该成员
        if(Modifier.isPublic(modifiers)) {
            return true;
        }
        
        /*
         * 【大前提】：待访问成员不是public
         */
        
        /* Check for nestmate access if member is private */
        // 如果待访问成员是私有的，且currentClass和memberClass互为嵌套关系（不分谁嵌套谁），则可以互相访问
        if(Modifier.isPrivate(modifiers)) {
            /* Note: targetClass may be outside the nest, but that is okay as long as memberClass is in the nest */
            if(areNestMates(currentClass, memberClass)) {
                return true;
            }
        }
        
        /*
         * 【大前提】：
         * 待访问成员是private，且currentClass和memberClass不是嵌套关系；
         * 或者，待访问成员是default或protected访问权限；
         */
        
        // 标记是否接近访问成功
        boolean successSoFar = false;
        
        // 如果待访问成员是protected，且currentClass类是memberClass类的子类(上面已经排除了相同的可能)，那么currentClass也可以访问父类memberClass中的protected成员
        if(Modifier.isProtected(modifiers)) {
            if(isSubclassOf(currentClass, memberClass)) {
                successSoFar = true;
            }
        }
        
        if(!successSoFar && !Modifier.isPrivate(modifiers)) {
            /*
             * 【大前提】：
             * 待访问成员为protected，且currentClass不是memberClass的子类
             * 或者，待访问成员为default
             */
            
            // 如果还未进行isSameClassPackage判断（说明memberClass类是public）
            if(!gotIsSameClassPackage) {
                // 判断currentClass和memberClass是否处于同一个包(前提是拥有相同的类加载器)
                isSameClassPackage = isSameClassPackage(currentClass, memberClass);
                gotIsSameClassPackage = true;
            }
            
            // currentClass和memberClass处于同一个包(前提是拥有相同的类加载器)
            if(isSameClassPackage) {
                // 对于同一个包下的类，它们可以互相访问protected成员或default成员
                successSoFar = true;
            }
        }
        
        if(!successSoFar) {
            return false;
        }
        
        /*
         * 【大前提】：currentClass对memberClass有包访问权限
         */
        
        /* Additional test for protected instance members and protected constructors: JLS 6.6.2 */
        // 对可能出现的一连串继承关系进行判断
        if(targetClass != null && Modifier.isProtected(modifiers) && targetClass != currentClass) {
            
            if(!gotIsSameClassPackage) {
                /*
                 * 至此，说明memberClass类是public，且待访问成员是protected，currentClass是memberClass的子类
                 */
                
                // 判断currentClass和memberClass是否处于同一个包(前提是拥有相同的类加载器)
                isSameClassPackage = isSameClassPackage(currentClass, memberClass);
                gotIsSameClassPackage = true;
            }
            
            if(!isSameClassPackage) {
                // 判断targetClass类是否与currentClass类相同，或为currentClass类的子类
                return isSubclassOf(targetClass, currentClass);
            }
        }
        
        return true;
    }
    
    /**
     * Returns {@code true} if memberClass's module exports memberClass's package to currentModule.
     */
    // 判断是否可以在currentModule模块内访问memberClass类
    public static boolean verifyModuleAccess(Module currentModule, Class<?> memberClass) {
        // 获取memberClass所在的模块
        Module memberModule = memberClass.getModule();
        
        // 如果位于相同的模块，允许互相访问
        if(currentModule == memberModule) {
            /*
             * same module (named or unnamed) or both null if called before module system is initialized,
             * which means we are dealing with java.base only.
             */
            return true;
        }
        
        // 获取memberClass所在的包名
        String pkg = memberClass.getPackageName();
        
        // 判断memberModule模块是否将pkg包导出(exports)给了currentModule模块
        return memberModule.isExported(pkg, currentModule);
    }
    
    /**
     * Tests if the given method is caller-sensitive and the declaring class
     * is defined by either the bootstrap class loader or platform class loader.
     */
    // 判断指定的方法是否为"调用者敏感"的(带有CallerSensitive注解)
    public static boolean isCallerSensitive(Method m) {
        final ClassLoader loader = m.getDeclaringClass().getClassLoader();
        
        // 如果给定的类加载器是否为bootstrap类加载器或为platform类加载器
        if(VM.isSystemDomainLoader(loader)) {
            // 判断方法m上是否存在注解CallerSensitive
            return m.isAnnotationPresent(CallerSensitive.class);
        }
        
        return false;
    }
    
    /**
     * Returns an IllegalAccessException with an exception message based on the access that is denied.
     */
    // 返回一个非法访问异常
    public static IllegalAccessException newIllegalAccessException(Class<?> currentClass, Class<?> memberClass, Class<?> targetClass, int modifiers) throws IllegalAccessException {
        String currentSuffix = "";
        String memberSuffix = "";
        
        Module m1 = currentClass.getModule();
        if(m1.isNamed()) {
            currentSuffix = " (in " + m1 + ")";
        }
        
        Module m2 = memberClass.getModule();
        if(m2.isNamed()) {
            memberSuffix = " (in " + m2 + ")";
        }
        
        String memberPackageName = memberClass.getPackageName();
        
        String msg = currentClass + currentSuffix + " cannot access ";
        if(m2.isExported(memberPackageName, m1)) {
            // module access okay so include the modifiers in the message
            msg += "a member of " + memberClass + memberSuffix + " with modifiers \"" + Modifier.toString(modifiers) + "\"";
        } else {
            // module access failed
            msg += memberClass + memberSuffix + " because " + m2 + " does not export " + memberPackageName;
            if(m2.isNamed()) {
                msg += " to " + m1;
            }
        }
        
        return new IllegalAccessException(msg);
    }
    
    /**
     * Returns true if {@code currentClass} and {@code memberClass} are nestmates
     * - that is, if they have the same nesthost as determined by the VM.
     */
    // 判断两个类是否相同或互为嵌套(内部类)关系（不分谁嵌套谁）
    public static native boolean areNestMates(Class<?> currentClass, Class<?> memberClass);
    
    // 判断queryClass类是否与ofClass类相同，或为ofClass类的子类
    static boolean isSubclassOf(Class<?> queryClass, Class<?> ofClass) {
        while(queryClass != null) {
            if(queryClass == ofClass) {
                return true;
            }
            
            queryClass = queryClass.getSuperclass();
        }
        
        return false;
    }
    
    /**
     * Returns true if two classes in the same package.
     */
    // 判断c1和c2是否处于同一个包(前提是拥有相同的类加载器)
    private static boolean isSameClassPackage(Class<?> c1, Class<?> c2) {
        // 首先确保类加载器一致
        if(c1.getClassLoader() != c2.getClassLoader()) {
            return false;
        }
        
        return Objects.equals(c1.getPackageName(), c2.getPackageName());
    }
    
    /** fieldNames must contain only interned Strings */
    // 将containingClass到fieldNames的映射注册到待过滤字段的注册表中
    public static synchronized void registerFieldsToFilter(Class<?> containingClass, String... fieldNames) {
        fieldFilterMap = registerFilter(fieldFilterMap, containingClass, fieldNames);
    }
    
    /** methodNames must contain only interned Strings */
    // 将containingClass到methodNames的映射注册到待过滤方法的注册表中
    public static synchronized void registerMethodsToFilter(Class<?> containingClass, String... methodNames) {
        methodFilterMap = registerFilter(methodFilterMap, containingClass, methodNames);
    }
    
    // 将containingClass到names的映射注册到相应的注册表map中
    private static Map<Class<?>, String[]> registerFilter(Map<Class<?>, String[]> map, Class<?> containingClass, String... names) {
        if(map.get(containingClass) != null) {
            throw new IllegalArgumentException("Filter already registered: " + containingClass);
        }
        
        map = new HashMap<Class<?>, String[]>(map);
        
        map.put(containingClass, names);
        
        return map;
    }
    
    // 返回对fields的过滤结果：如果某字段出现在containingClass类注册的字段过滤表中，则会将其过滤掉
    public static Field[] filterFields(Class<?> containingClass, Field[] fields) {
        if(fieldFilterMap == null) {
            return fields;  // Bootstrapping
        }
        
        return (Field[]) filter(fields, fieldFilterMap.get(containingClass));
    }
    
    // 返回对methods的过滤结果：如果某方法出现在containingClass类注册的方法过滤表中，则会将其过滤掉
    public static Method[] filterMethods(Class<?> containingClass, Method[] methods) {
        if(methodFilterMap == null) {
            return methods;
        }
        
        return (Method[]) filter(methods, methodFilterMap.get(containingClass));
    }
    
    // 对members进行过滤，返回过滤后的成员：如果某成员名称出现在过滤表filteredNames中，则它会被过滤掉
    private static Member[] filter(Member[] members, String[] filteredNames) {
        if((filteredNames == null) || (members.length == 0)) {
            return members;
        }
        
        int numNewMembers = 0;
        for(Member member : members) {
            boolean shouldSkip = false;
            
            // 判断member是否应当被过滤掉
            for(String filteredName : filteredNames) {
                if(member.getName() == filteredName) {
                    shouldSkip = true;
                    break;
                }
            }
            
            // 对于需要保留的member，统计其数量
            if(!shouldSkip) {
                ++numNewMembers;
            }
        }
        
        // 创建新数组，存放要保留的member
        Member[] newMembers = (Member[]) Array.newInstance(members[0].getClass(), numNewMembers);
        
        int destIdx = 0;
        for(Member member : members) {
            boolean shouldSkip = false;
            for(String filteredName : filteredNames) {
                if(member.getName() == filteredName) {
                    shouldSkip = true;
                    break;
                }
            }
            
            // 对于需要保留的member，将其存储到newMembers中
            if(!shouldSkip) {
                newMembers[destIdx++] = member;
            }
        }
        
        return newMembers;
    }
    
}
