/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.util;

/**
 * System Property access for internal use only.
 * Read-only access to System property values initialized during Phase 1 are cached.
 * Setting, clearing, or modifying the value using {@link System#setProperty) or {@link System#getProperties()} is ignored.
 * {@link SecurityManager#checkPropertyAccess} is NOT checked n these access methods.
 * The caller of these methods should take care to ensure that the returned property is not made accessible to untrusted code.
 */
// 一些只读的系统属性，一旦加载，禁止修改
public final class StaticProperty {
    
    // The class static initialization is triggered to initialize these final
    // fields during init Phase 1 and before a security manager is set.
    private static final String JAVA_HOME = initProperty("java.home");  // JDK根目录
    private static final String USER_DIR = initProperty("user.dir");   // 用户工作目录（如项目根目录）
    private static final String USER_NAME = initProperty("user.name");  // 用户主机名称，如kang
    private static final String USER_HOME = initProperty("user.home");  // 用户主机根目录，如C:\Users\kang
    
    private StaticProperty() {
    }
    
    /**
     * Return the {@code java.home} system property.
     *
     * <strong>{@link SecurityManager#checkPropertyAccess} is NOT checked
     * in this method. The caller of this method should take care to ensure
     * that the returned property is not made accessible to untrusted code.</strong>
     *
     * @return the {@code java.home} system property
     */
    // 获取JDK根目录
    public static String javaHome() {
        return JAVA_HOME;
    }
    
    /**
     * Return the {@code user.dir} system property.
     *
     * <strong>{@link SecurityManager#checkPropertyAccess} is NOT checked
     * in this method. The caller of this method should take care to ensure
     * that the returned property is not made accessible to untrusted code.</strong>
     *
     * @return the {@code user.dir} system property
     */
    // 获取用户工作目录（如项目根目录）
    public static String userDir() {
        return USER_DIR;
    }
    
    /**
     * Return the {@code user.home} system property.
     *
     * <strong>{@link SecurityManager#checkPropertyAccess} is NOT checked
     * in this method. The caller of this method should take care to ensure
     * that the returned property is not made accessible to untrusted code.</strong>
     *
     * @return the {@code user.home} system property
     */
    // 获取用户主机根目录，如C:\Users\kang
    public static String userHome() {
        return USER_HOME;
    }
    
    /**
     * Return the {@code user.name} system property.
     *
     * <strong>{@link SecurityManager#checkPropertyAccess} is NOT checked
     * in this method. The caller of this method should take care to ensure
     * that the returned property is not made accessible to untrusted code.</strong>
     *
     * @return the {@code user.name} system property
     */
    // 获取用户主机名称，如kang
    public static String userName() {
        return USER_NAME;
    }
    
    // 获取指定的属性初始值
    private static String initProperty(String key) {
        String v = System.getProperty(key);
        if(v == null) {
            throw new InternalError("null property: " + key);
        }
        return v;
    }
}
