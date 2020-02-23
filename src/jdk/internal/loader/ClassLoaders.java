/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.loader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.jar.Manifest;
import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.VM;

/**
 * Creates and provides access to the built-in platform and application class loaders.
 * It also creates the class loader that is used to locate resources in modules defined to the boot class loader.
 */
// 类加载器集合，实现了Java内置的类加载器：app/platform/boot class loader
public class ClassLoaders {
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
    
    // 三个内置的类加载器
    private static final BootClassLoader BOOT_LOADER;
    private static final PlatformClassLoader PLATFORM_LOADER;
    private static final AppClassLoader APP_LOADER;
    
    
    // 初始化内置的类加载器
    static {
        /* -Xbootclasspath/a or -javaagent with Boot-Class-Path attribute */
        // 通过运行参数-Djdk.boot.class.path.append=...设置BootClassLoader的URLClassPath
        String append = VM.getSavedProperty("jdk.boot.class.path.append");
        
        BOOT_LOADER = new BootClassLoader((append != null && append.length()>0) ? new URLClassPath(append, true) : null);
    
        PLATFORM_LOADER = new PlatformClassLoader(BOOT_LOADER);
    
        /*
         * A class path is required when no initial module is specified.
         * In this case the class path defaults to "", meaning the current working directory.
         * When an initial module is specified, on the contrary,
         * we drop this historic interpretation of the empty string and instead treat it as unspecified.
         */
        // 查找类路径
        String cp = System.getProperty("java.class.path");
    
        // 如果未找到类路径
        if(cp == null || cp.length() == 0) {
            // 查找模块路径（存在模块时）
            String initialModuleName = System.getProperty("jdk.module.main");
            cp = (initialModuleName == null) ? "" : null;
        }
    
        URLClassPath ucp = new URLClassPath(cp, false);
    
        APP_LOADER = new AppClassLoader(PLATFORM_LOADER, ucp);
    }
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private ClassLoaders() {
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取类加载器实例 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the class loader that is used to find resources in modules
     * defined to the boot class loader.
     *
     * @apiNote This method is not public, it should instead be used via
     * the BootLoader class that provides a restricted API to this class
     * loader.
     */
    // 获取BootClassLoader
    static BuiltinClassLoader bootLoader() {
        return BOOT_LOADER;
    }
    
    /**
     * Returns the platform class loader.
     */
    // 获取PlatformClassLoader
    public static ClassLoader platformClassLoader() {
        return PLATFORM_LOADER;
    }
    
    /**
     * Returns the application class loader.
     */
    // 获取AppClassLoader
    public static ClassLoader appClassLoader() {
        return APP_LOADER;
    }
    
    /*▲ 获取类加载器实例 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * The class loader that is used to find resources in modules defined to the boot class loader.
     * It is not used for class loading.
     */
    /*
     * Bootstrap类加载器
     *
     * 该类加载器可以看做是boot class loader的半个替身
     *
     * BootClassLoader仅用于在boot class loader定义的模块中查找资源，它不用于类加载。
     * 设置此类的目的是简化资源获取操作，加载类的任务还得boot class loader完成。
     *
     * 设置/追加/前置搜索路径
     * -Xbootclasspath:
     * -Xbootclasspath/a:
     * -Xbootclasspath/p:
     */
    private static class BootClassLoader extends BuiltinClassLoader {
        BootClassLoader(URLClassPath bcp) {
            super(null, null, bcp);
        }
        
        /*
         * 该方法包含了查找类和定义类两方面的操作
         * 如果加载类失败，会返回null
         */
        @Override
        protected Class<?> loadClassOrNull(String className) {
            return JLA.findBootstrapClassOrNull(this, className);
        }
    }
    
    /**
     * The platform class loader, a unique type to make it easier to distinguish
     * from the application class loader.
     */
    // 平台类加载器
    private static class PlatformClassLoader extends BuiltinClassLoader {
        static {
            // 将当前类加载器注册为并行
            if(!ClassLoader.registerAsParallelCapable()) {
                throw new InternalError();
            }
        }
        
        PlatformClassLoader(BootClassLoader parent) {
            super("platform", parent, null);
        }
        
        /**
         * Called by the VM to support define package for AppCDS.
         *
         * Shared classes are returned in ClassLoader::findLoadedClass
         * that bypass the defineClass call.
         */
        // 根据包名与模块名定义Package对象
        private Package definePackage(String packageName, Module module) {
            return JLA.definePackage(this, packageName, module);
        }
    }
    
    /**
     * The application class loader that is a {@code BuiltinClassLoader} with customizations to be compatible with long standing behavior.
     */
    /*
     * 应用类加载器
     *
     * 设置搜索路径
     * -Djava.class.path
     * -cp
     * -classpath
     */
    private static class AppClassLoader extends BuiltinClassLoader {
        final URLClassPath ucp; // 自己的资源加载路径
        
        static {
            // 将当前类加载器注册为并行
            if(!ClassLoader.registerAsParallelCapable()) {
                throw new InternalError();
            }
        }
        
        AppClassLoader(PlatformClassLoader parent, URLClassPath ucp) {
            super("app", parent, ucp);
            this.ucp = ucp;
        }
        
        // 由给定的类名加载类。如果找不到该类则抛出异常
        @Override
        protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
            // for compatibility reasons, say where restricted package list has been updated to list API packages in the unnamed module.
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                int i = className.lastIndexOf('.');
                if(i != -1) {
                    sm.checkPackageAccess(className.substring(0, i));
                }
            }
            
            // 调用了BuiltinClassLoader中的加载逻辑
            return super.loadClass(className, resolve);
        }
        
        @Override
        protected PermissionCollection getPermissions(CodeSource cs) {
            PermissionCollection perms = super.getPermissions(cs);
            perms.add(new RuntimePermission("exitVM"));
            return perms;
        }
        
        /**
         * Called by the VM to support define package for AppCDS
         */
        // 尝试定义Package对象，如果该对象已经存在，则对其进行验证
        protected Package defineOrCheckPackage(String packageName, Manifest man, URL url) {
            return super.defineOrCheckPackage(packageName, man, url);
        }
        
        /**
         * Called by the VM to support dynamic additions to the class path
         *
         * @see java.lang.instrument.Instrumentation#appendToSystemClassLoaderSearch
         */
        // 将指定的文件路径添加加到搜索路径。
        void appendToClassPathForInstrumentation(String path) {
            ucp.addFile(path);
        }
        
        /**
         * Called by the VM to support define package for AppCDS
         *
         * Shared classes are returned in ClassLoader::findLoadedClass
         * that bypass the defineClass call.
         */
        // 根据包名与模块名定义Package对象
        private Package definePackage(String packageName, Module module) {
            return JLA.definePackage(this, packageName, module);
        }
    }
    
    
    /**
     * Attempts to convert the given string to a file URL.
     *
     * @apiNote This is called by the VM
     */
    // 尝试将给定字符串转换为文件URL
    @Deprecated
    private static URL toFileURL(String s) {
        try {
            // Use an intermediate File object to construct a URI/URL without authority component
            // as URLClassPath can't handle URLs with a UNC server name in the authority component.
            return Path.of(s).toRealPath().toFile().toURI().toURL();
        } catch(InvalidPathException | IOException ignore) {
            // malformed path string or class path element does not exist
            return null;
        }
    }
    
}
