/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.misc;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import jdk.internal.reflect.Reflection;
import sun.security.util.SecurityConstants;

// 访问权限检查工具
public final class ReflectUtil {
    
    // Note that bytecode instrumentation tools may exclude 'sun.*'
    // classes but not generated proxy classes and so keep it in com.sun.*
    public static final String PROXY_PACKAGE = "com.sun.proxy"; // 代理接口为public时，代理类使用的包名
    
    private ReflectUtil() {
    }
    
    // 加载指定的类
    public static Class<?> forName(String name) throws ClassNotFoundException {
        checkPackageAccess(name);
        return Class.forName(name);
    }
    
    /**
     * Ensures that access to a method or field is granted and throws
     * IllegalAccessException if not. This method is not suitable for checking
     * access to constructors.
     *
     * @param currentClass the class performing the access
     * @param memberClass  the declaring class of the member being accessed
     * @param target       the target object if accessing instance field or method;
     *                     or null if accessing static field or method or if target
     *                     object access rights will be checked later
     * @param modifiers    the member's access modifiers
     *
     * @throws IllegalAccessException if access to member is denied
     * @implNote Delegates directly to
     * {@link Reflection#ensureMemberAccess(Class, Class, Class, int)}
     * which should be used instead.
     */
    // 确保可以在currentClass中通过target对象来显式访问memberClass中具有modifiers修饰符的成员(如果访问的是静态成员，则target为null)；否则，会抛异常
    public static void ensureMemberAccess(Class<?> currentClass, Class<?> memberClass, Object target, int modifiers) throws IllegalAccessException {
        Reflection.ensureMemberAccess(currentClass, memberClass, target == null ? null : target.getClass(), modifiers);
    }
    
    /**
     * Does a conservative approximation of member access check. Use this if
     * you don't have an actual 'userland' caller Class/ClassLoader available.
     * This might be more restrictive than a precise member access check where
     * you have a caller, but should never allow a member access that is
     * forbidden.
     *
     * @param m the {@code Member} about to be accessed
     */
    // 检查当前线程对member的访问权限
    public static void conservativeCheckMemberAccess(Member member) throws SecurityException {
        final SecurityManager sm = System.getSecurityManager();
        if(sm == null) {
            return;
        }
        
        // Check for package access on the declaring class.
        //
        // In addition, unless the member and the declaring class are both
        // public check for access declared member permissions.
        //
        // This is done regardless of ClassLoader relations between the {@code
        // Member m} and any potential caller.
        
        // 获取member所在的类
        final Class<?> declaringClass = member.getDeclaringClass();
        
        // 通过安全管理器检查当前线程对declaringClass所在包的访问权限；如果declaringClass是代理类，还需要检查其代理接口
        privateCheckPackageAccess(sm, declaringClass);
        
        // 确保member及其所在的类均由public修饰
        if(Modifier.isPublic(member.getModifiers()) && Modifier.isPublic(declaringClass.getModifiers())) {
            return;
        }
        
        // Check for declared member access.
        sm.checkPermission(SecurityConstants.CHECK_MEMBER_ACCESS_PERMISSION);
    }
    
    /**
     * Checks package access on the given class.
     *
     * If it is a {@link Proxy#isProxyClass(java.lang.Class)} that implements
     * a non-public interface (i.e. may be in a non-restricted package),
     * also check the package access on the proxy interfaces.
     */
    // 检查当前线程对clazz所在的包的访问权限；如果clazz是代理类，还需要检查其代理接口
    public static void checkPackageAccess(Class<?> clazz) {
        SecurityManager s = System.getSecurityManager();
        if(s == null) {
            return;
        }
        
        // 通过安全管理器检查当前线程对clazz所在包的访问权限；如果clazz是代理类，还需要检查其代理接口
        privateCheckPackageAccess(s, clazz);
    }
    
    // 检查当前线程对clazz所在的包的访问权限；如果clazz是代理类，还需要检查其代理接口
    public static boolean isPackageAccessible(Class<?> clazz) {
        try {
            checkPackageAccess(clazz);
        } catch(SecurityException e) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks package access on the given classname.
     * This method is typically called when the Class instance is not available and the caller attempts to load a class on behalf the true caller (application).
     */
    // 检查当前线程对指定类的访问权限
    public static void checkPackageAccess(String name) {
        SecurityManager s = System.getSecurityManager();
        if(s == null) {
            return;
        }
        
        // 将'/'替换为.
        String cname = name.replace('/', '.');
        
        // 如果是数组类，提取组件名称
        if(cname.startsWith("[")) {
            int b = cname.lastIndexOf('[') + 2;
            if(b>1 && b<cname.length()) {
                cname = cname.substring(b);
            }
        }
        
        // 检查包名
        int i = cname.lastIndexOf('.');
        
        if(i != -1) {
            // 提取包名
            String pkg = cname.substring(0, i);
            
            // 通过安全管理器检查当前线程对pkg包的访问权限
            s.checkPackageAccess(pkg);
        }
    }
    
    /**
     * Returns true if package access check is needed for reflective
     * access from a class loader 'from' to classes or members in
     * a class defined by class loader 'to'.  This method returns true
     * if 'from' is not the same as or an ancestor of 'to'.  All code
     * in a system domain are granted with all permission and so this
     * method returns false if 'from' class loader is a class loader
     * loading system classes.  On the other hand, if a class loader
     * attempts to access system domain classes, it requires package
     * access check and this method will return true.
     */
    // 判断from（加载的类）访问to（加载的类）时，是否需要包访问权限的检查
    public static boolean needsPackageAccessCheck(ClassLoader from, ClassLoader to) {
        if(from == null || from == to) {
            return false;
        }
        
        if(to == null) {
            return true;
        }
        
        // 判断类加载器from是否为to的祖先（from位于to的委托链上）
        return !isAncestor(from, to);
    }
    
    /**
     * Access check on the interfaces that a proxy class implements and throw
     * {@code SecurityException} if it accesses a restricted package from
     * the caller's class loader.
     *
     * @param ccl        the caller's class loader
     * @param interfaces the list of interfaces that a proxy class implements
     */
    // 检查ccl（加载的类）对代理接口interfaces的访问权限
    public static void checkProxyPackageAccess(ClassLoader ccl, Class<?>... interfaces) {
        SecurityManager sm = System.getSecurityManager();
        if(sm == null) {
            return;
        }
        
        // 遍历代理接口
        for(Class<?> intf : interfaces) {
            // 获取代理接口的类加载器
            ClassLoader cl = intf.getClassLoader();
            
            // 判断ccl（加载的类）访问cl（加载的类）时，是否需要包访问权限的检查
            if(needsPackageAccessCheck(ccl, cl)) {
                // 通过安全管理器检查当前线程对intf所在包的访问权限；如果intf是代理类，还需要检查其代理接口
                privateCheckPackageAccess(sm, intf);
            }
        }
    }
    
    /**
     * Test if the given class is a proxy class that implements non-public interface.
     * Such proxy class may be in a non-restricted package that bypasses checkPackageAccess.
     */
    // 判断cls是否为实现了非public代理接口的代理类
    public static boolean isNonPublicProxyClass(Class<?> cls) {
        // 判断cls是否为代理类
        if(!Proxy.isProxyClass(cls)) {
            return false;
        }
        
        String pkg = cls.getPackageName();
        return pkg == null || !pkg.startsWith(PROXY_PACKAGE);
    }
    
    /**
     * Check if the given method is a method declared in the proxy interface mplemented by the given proxy instance.
     *
     * @param proxy  a proxy instance
     * @param method an interface method dispatched to a InvocationHandler
     *
     * @throws IllegalArgumentException if the given proxy or method is invalid.
     */
    // 检查method是否为代理接口或Object中的方法
    public static void checkProxyMethod(Object proxy, Method method) {
        // check if it is a valid proxy instance
        if(proxy == null || !Proxy.isProxyClass(proxy.getClass())) {
            throw new IllegalArgumentException("Not a Proxy instance");
        }
        
        if(Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("Can't handle static method");
        }
        
        Class<?> c = method.getDeclaringClass();
        if(c == Object.class) {
            String name = method.getName();
            if(name.equals("hashCode") || name.equals("equals") || name.equals("toString")) {
                return;
            }
        }
        
        Class<?> proxyClass = proxy.getClass();
        
        // 判断c是否为proxyClass的某个父接口
        if(isSuperInterface(proxyClass, c)) {
            return;
        }
        
        // disallow any method not declared in one of the proxy interfaces
        throw new IllegalArgumentException("Can't handle: " + method);
    }
    
    /**
     * Checks if {@code Class cls} is a VM-anonymous class as defined by {@link jdk.internal.misc.Unsafe#defineAnonymousClass}
     * (not to be confused with a Java Language anonymous inner class).
     */
    // 检查cls是否为虚拟机匿名类(这与Java语言中的匿名内部类不同)
    public static boolean isVMAnonymousClass(Class<?> cls) {
        // 类名中包含'/'
        return cls.getName().indexOf('/')>-1;
    }
    
    /**
     * Check package access on the proxy interfaces that the given proxy class implements.
     *
     * @param clazz Proxy class object
     */
    // 通过安全管理器检查当前线程对clazz的代理接口所在包的访问权限
    public static void checkProxyPackageAccess(Class<?> clazz) {
        SecurityManager s = System.getSecurityManager();
        if(s != null) {
            privateCheckProxyPackageAccess(s, clazz);
        }
    }
    
    /**
     * NOTE: should only be called if a SecurityManager is installed
     */
    // 通过安全管理器检查当前线程对clazz所在包的访问权限；如果clazz是代理类，还需要检查其代理接口
    private static void privateCheckPackageAccess(SecurityManager s, Class<?> clazz) {
        // 如果clazz是数组类型，获取数组的组件类型，如int[][]返回int；如果不是数组，则返回空
        while(clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        
        // 获取clazz所在的包
        String pkg = clazz.getPackageName();
        
        if(pkg != null && !pkg.isEmpty()) {
            // 通过安全管理器检查当前线程对pkg包的访问权限
            s.checkPackageAccess(pkg);
        }
        
        // 如果clazz是实现了非public代理接口的代理类
        if(isNonPublicProxyClass(clazz)) {
            // 通过安全管理器检查当前线程对clazz的代理接口所在包的访问权限
            privateCheckProxyPackageAccess(s, clazz);
        }
    }
    
    /**
     * NOTE: should only be called if a SecurityManager is installed
     */
    // 通过安全管理器检查当前线程对clazz的代理接口所在包的访问权限
    private static void privateCheckProxyPackageAccess(SecurityManager s, Class<?> clazz) {
        // check proxy interfaces if the given class is a proxy class
        if(Proxy.isProxyClass(clazz)) {
            // 遍历代理接口
            for(Class<?> intf : clazz.getInterfaces()) {
                privateCheckPackageAccess(s, intf);
            }
        }
    }
    
    /** Returns true if p is an ancestor of cl i.e. class loader 'p' can be found in the cl's delegation chain */
    // 判断类加载器p是否为cl的祖先（p位于cl的委托链上）
    private static boolean isAncestor(ClassLoader p, ClassLoader cl) {
        ClassLoader acl = cl;
        
        do {
            acl = acl.getParent();
            if(p == acl) {
                return true;
            }
        } while(acl != null);
        
        return false;
    }
    
    // 判断intf是否为c的某个父接口
    private static boolean isSuperInterface(Class<?> c, Class<?> intf) {
        for(Class<?> i : c.getInterfaces()) {
            if(i == intf) {
                return true;
            }
            
            if(isSuperInterface(i, intf)) {
                return true;
            }
        }
        
        return false;
    }
}
