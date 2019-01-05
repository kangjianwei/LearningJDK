/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.lang.reflect.AnnotatedElement;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.internal.loader.BootLoader;
import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.loader.ClassLoaders;
import jdk.internal.module.IllegalAccessLogger;
import jdk.internal.module.ModuleLoaderMap;
import jdk.internal.module.Resources;
import jdk.internal.module.ServicesCatalog;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.ModuleVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.security.util.SecurityConstants;

/**
 * Represents a run-time module, either {@link #isNamed() named} or unnamed.
 *
 * <p> Named modules have a {@link #getName() name} and are constructed by the
 * Java Virtual Machine when a graph of modules is defined to the Java virtual
 * machine to create a {@linkplain ModuleLayer module layer}. </p>
 *
 * <p> An unnamed module does not have a name. There is an unnamed module for
 * each {@link ClassLoader ClassLoader}, obtained by invoking its {@link
 * ClassLoader#getUnnamedModule() getUnnamedModule} method. All types that are
 * not in a named module are members of their defining class loader's unnamed
 * module. </p>
 *
 * <p> The package names that are parameters or returned by methods defined in
 * this class are the fully-qualified names of the packages as defined in
 * section 6.5.3 of <cite>The Java&trade; Language Specification</cite>, for
 * example, {@code "java.lang"}. </p>
 *
 * <p> Unless otherwise specified, passing a {@code null} argument to a method
 * in this class causes a {@link NullPointerException NullPointerException} to
 * be thrown. </p>
 *
 * @since 9
 * @spec JPMS
 * @see Class#getModule()
 */
// 模块（表示一个运行时模块）
public final class Module implements AnnotatedElement {
    
    // the module descriptor
    private final ModuleDescriptor descriptor;
    
    // the layer that contains this module, can be null
    private final ModuleLayer layer;
    
    // module name and loader, these fields are read by VM
    private final String name;
    
    private final ClassLoader loader;
    
    // the modules that this module reads
    private volatile Set<Module> reads;
    
    // the packages are open to other modules, can be null if the value contains EVERYONE_MODULE then the package is open to all
    private volatile Map<String, Set<Module>> openPackages;
    
    // the packages that are exported, can be null if the value contains EVERYONE_MODULE then the package is exported to all
    private volatile Map<String, Set<Module>> exportedPackages;
    
    // special Module to mean "all unnamed modules"
    private static final Module ALL_UNNAMED_MODULE = new Module(null);
    private static final Set<Module> ALL_UNNAMED_MODULE_SET = Set.of(ALL_UNNAMED_MODULE);
    
    // special Module to mean "everyone"
    private static final Module EVERYONE_MODULE = new Module(null);
    private static final Set<Module> EVERYONE_SET = Set.of(EVERYONE_MODULE);
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new named Module. The resulting Module will be defined to the
     * VM but will not read any other modules, will not have any exports setup
     * and will not be registered in the service catalog.
     */
    Module(ModuleLayer layer, ClassLoader loader, ModuleDescriptor descriptor, URI uri) {
        this.layer = layer;
        this.name = descriptor.name();
        this.loader = loader;
        this.descriptor = descriptor;
        
        // define module to VM
        
        boolean isOpen = descriptor.isOpen() || descriptor.isAutomatic();
        Version version = descriptor.version().orElse(null);
        String vs = Objects.toString(version, null);
        String loc = Objects.toString(uri, null);
        String[] packages = descriptor.packages().toArray(new String[0]);
        defineModule0(this, isOpen, vs, loc, packages);
    }
    
    /**
     * Create the unnamed Module for the given ClassLoader.
     *
     * @see ClassLoader#getUnnamedModule
     */
    Module(ClassLoader loader) {
        this.layer = null;
        this.name = null;
        this.loader = loader;
        this.descriptor = null;
    }
    
    /**
     * Creates a named module but without defining the module to the VM.
     *
     * @apiNote This constructor is for VM white-box testing.
     */
    Module(ClassLoader loader, ModuleDescriptor descriptor) {
        this.layer = null;
        this.name = descriptor.name();
        this.loader = loader;
        this.descriptor = descriptor;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 关联信息 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if this module is a named module.
     *
     * @return {@code true} if this is a named module
     *
     * @see ClassLoader#getUnnamedModule()
     */
    // 该模块是否有名称
    public boolean isNamed() {
        return name != null;
    }
    
    /**
     * Returns the module name or {@code null} if this module is an unnamed
     * module.
     *
     * @return The module name
     */
    // 获取模块名称
    public String getName() {
        return name;
    }
    
    /**
     * Returns the {@code ClassLoader} for this module.
     *
     * <p> If there is a security manager then its {@code checkPermission}
     * method if first called with a {@code RuntimePermission("getClassLoader")}
     * permission to check that the caller is allowed to get access to the
     * class loader. </p>
     *
     * @return The class loader for this module
     *
     * @throws SecurityException If denied by the security manager
     */
    // 获取加载该模块的类加载器
    public ClassLoader getClassLoader() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
        }
        return loader;
    }
    
    /**
     * Returns the module descriptor for this module or {@code null} if this
     * module is an unnamed module.
     *
     * @return The module descriptor for this module
     */
    // 获取该模块的模块描述符
    public ModuleDescriptor getDescriptor() {
        return descriptor;
    }
    
    /**
     * Returns the module layer that contains this module or {@code null} if
     * this module is not in a module layer.
     *
     * A module layer contains named modules and therefore this method always
     * returns {@code null} when invoked on an unnamed module.
     *
     * <p> <a href="reflect/Proxy.html#dynamicmodule">Dynamic modules</a> are
     * named modules that are generated at runtime. A dynamic module may or may
     * not be in a module layer. </p>
     *
     * @return The module layer that contains this module
     *
     * @see java.lang.reflect.Proxy
     */
    // 获取该模块所在的模块层
    public ModuleLayer getLayer() {
        if(isNamed()) {
            ModuleLayer layer = this.layer;
            if(layer != null) {
                return layer;
            }
            
            // special-case java.base as it is created before the boot layer
            if(loader == null && name.equals("java.base")) {
                return ModuleLayer.boot();
            }
        }
        return null;
    }
    
    /**
     * Returns the set of package names for the packages in this module.
     *
     * <p> For named modules, the returned set contains an element for each
     * package in the module. </p>
     *
     * <p> For unnamed modules, this method is the equivalent to invoking the
     * {@link ClassLoader#getDefinedPackages() getDefinedPackages} method of
     * this module's class loader and returning the set of package names. </p>
     *
     * @return the set of the package names of the packages in this module
     */
    // 获取当前模块下辖的包
    public Set<String> getPackages() {
        if (isNamed()) {
            return descriptor.packages();
        } else {
            // unnamed module
            Stream<Package> packages;
            if (loader == null) {
                packages = BootLoader.packages();
            } else {
                packages = loader.packages();
            }
            
            return packages.map(Package::getName).collect(Collectors.toSet());
        }
    }
    
    /**
     * Returns an input stream for reading a resource in this module.
     * The {@code name} parameter is a {@code '/'}-separated path name that
     * identifies the resource. As with {@link Class#getResourceAsStream
     * Class.getResourceAsStream}, this method delegates to the module's class
     * loader {@link ClassLoader#findResource(String, String)
     * findResource(String,String)} method, invoking it with the module name
     * (or {@code null} when the module is unnamed) and the name of the
     * resource. If the resource name has a leading slash then it is dropped
     * before delegation.
     *
     * <p> A resource in a named module may be <em>encapsulated</em> so that
     * it cannot be located by code in other modules. Whether a resource can be
     * located or not is determined as follows: </p>
     *
     * <ul>
     * <li> If the resource name ends with  "{@code .class}" then it is not
     * encapsulated. </li>
     *
     * <li> A <em>package name</em> is derived from the resource name. If
     * the package name is a {@linkplain #getPackages() package} in the
     * module then the resource can only be located by the caller of this
     * method when the package is {@linkplain #isOpen(String, Module) open}
     * to at least the caller's module. If the resource is not in a
     * package in the module then the resource is not encapsulated. </li>
     * </ul>
     *
     * <p> In the above, the <em>package name</em> for a resource is derived
     * from the subsequence of characters that precedes the last {@code '/'} in
     * the name and then replacing each {@code '/'} character in the subsequence
     * with {@code '.'}. A leading slash is ignored when deriving the package
     * name. As an example, the package name derived for a resource named
     * "{@code a/b/c/foo.properties}" is "{@code a.b.c}". A resource name
     * with the name "{@code META-INF/MANIFEST.MF}" is never encapsulated
     * because "{@code META-INF}" is not a legal package name. </p>
     *
     * <p> This method returns {@code null} if the resource is not in this
     * module, the resource is encapsulated and cannot be located by the caller,
     * or access to the resource is denied by the security manager. </p>
     *
     * @param name The resource name
     *
     * @return An input stream for reading the resource or {@code null}
     *
     * @throws IOException If an I/O error occurs
     * @see Class#getResourceAsStream(String)
     */
    // 返回指定资源的输入流，以便调用者读取资源
    @CallerSensitive
    public InputStream getResourceAsStream(String name) throws IOException {
        if(name.startsWith("/")) {
            name = name.substring(1);
        }
        
        if(isNamed() && Resources.canEncapsulate(name)) {
            Module caller = getCallerModule(Reflection.getCallerClass());
            // 调用者不在当前模块内，也不再java.base模块内
            if(caller != this && caller != Object.class.getModule()) {
                String pn = Resources.toPackageName(name);
                if(getPackages().contains(pn)) {
                    if(caller == null && !isOpen(pn)) {
                        // no caller, package not open
                        return null;
                    }
                    
                    if(!isOpen(pn, caller)) {
                        // package not open to caller
                        return null;
                    }
                }
            }
        }
        
        String mn = this.name;
        
        // special-case built-in class loaders to avoid URL connection
        if(loader == null) {
            return BootLoader.findResourceAsStream(mn, name);
        } else if(loader instanceof BuiltinClassLoader) {
            return ((BuiltinClassLoader) loader).findResourceAsStream(mn, name);
        }
        
        // locate resource in module
        URL url = loader.findResource(mn, name);
        if(url != null) {
            try {
                return url.openStream();
            } catch(SecurityException e) {
            }
        }
        
        return null;
    }
    
    /**
     * Returns the module that a given caller class is a member of. Returns
     * {@code null} if the caller is {@code null}.
     */
    // 获取caller所在的模块
    private Module getCallerModule(Class<?> caller) {
        return (caller != null) ? caller.getModule() : null;
    }
    
    /*▲ 关联信息 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ reads ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Indicates if this module reads the given module. This method returns
     * {@code true} if invoked to test if this module reads itself. It also
     * returns {@code true} if invoked on an unnamed module (as unnamed
     * modules read all modules).
     *
     * @param other The other module
     *
     * @return {@code true} if this module reads {@code other}
     *
     * @see #addReads(Module)
     */
    // 当前模块是否可读取other模块
    public boolean canRead(Module other) {
        Objects.requireNonNull(other);
        
        // an unnamed module reads all modules
        if(!this.isNamed())
            return true;
        
        // all modules read themselves
        if(other == this)
            return true;
        
        // check if this module reads other
        if(other.isNamed()) {
            Set<Module> reads = this.reads; // volatile read
            if(reads != null && reads.contains(other))
                return true;
        }
        
        // check if this module reads the other module reflectively
        if(ReflectionData.reads.containsKeyPair(this, other))
            return true;
        
        // if other is an unnamed module then check if this module reads
        // all unnamed modules
        if(!other.isNamed() && ReflectionData.reads.containsKeyPair(this, ALL_UNNAMED_MODULE))
            return true;
        
        return false;
    }
    
    /**
     * If the caller's module is this module then update this module to read
     * the given module.
     *
     * This method is a no-op if {@code other} is this module (all modules read
     * themselves), this module is an unnamed module (as unnamed modules read
     * all modules), or this module already reads {@code other}.
     *
     * @param other The other module
     *
     * @return this module
     *
     * @throws IllegalCallerException If this is a named module and the caller's module is not this
     *                                module
     * @implNote <em>Read edges</em> added by this method are <em>weak</em> and
     * do not prevent {@code other} from being GC'ed when this module is
     * strongly reachable.
     * @see #canRead
     */
    // 使当前模块读取other模块（必须在当前模块内调用）
    @CallerSensitive
    public Module addReads(Module other) {
        Objects.requireNonNull(other);
        if(this.isNamed()) {
            Module caller = getCallerModule(Reflection.getCallerClass());
            if(caller != this) {
                throw new IllegalCallerException(caller + " != " + this);
            }
            implAddReads(other, true);
        }
        return this;
    }
    
    /**
     * Updates this module to read another module.
     *
     * @apiNote Used by the --add-reads command line option.
     */
    // 使当前模块读取other模块，相当于--add-reads命令行
    void implAddReads(Module other) {
        implAddReads(other, true);
    }
    
    /**
     * Updates this module to read all unnamed modules.
     *
     * @apiNote Used by the --add-reads command line option.
     */
    // 使当前模块读取所有未命名模块，相当于--add-reads命令行
    void implAddReadsAllUnnamed() {
        implAddReads(Module.ALL_UNNAMED_MODULE, true);
    }
    
    /**
     * Updates this module to read another module without notifying the VM.
     *
     * @apiNote This method is for VM white-box testing.
     */
    // 在不通知VM的情形下读取另一个模块
    void implAddReadsNoSync(Module other) {
        implAddReads(other, false);
    }
    
    /**
     * Makes the given {@code Module} readable to this module.
     *
     * If {@code syncVM} is {@code true} then the VM is notified.
     */
    // 使当前模块读取other模块，syncVM决定是否通知VM
    private void implAddReads(Module other, boolean syncVM) {
        Objects.requireNonNull(other);
        if(!canRead(other)) {
            // update VM first, just in case it fails
            if(syncVM) {
                if(other == ALL_UNNAMED_MODULE) {
                    addReads0(this, null);
                } else {
                    addReads0(this, other);
                }
            }
            
            // add reflective read
            ReflectionData.reads.putIfAbsent(this, other, Boolean.TRUE);
        }
    }
    
    /*▲ reads ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ exported 和 open ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * exported：显式使用公开元素，反射公开元素
     * open：反射非公开元素
     * 如果只打开exported权限，不影响open权限
     * 如果只打开open权限，则exported打开一半，即允许反射公开元素，但无法显式使用公开元素
     */
    
    
    /* ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ */
    
    /**
     * Returns {@code true} if this module exports the given package
     * unconditionally.
     *
     * <p> This method always returns {@code true} when invoked on an unnamed
     * module. A package that is {@link #isOpen(String) opened} unconditionally
     * is considered exported unconditionally at run-time and so this method
     * returns {@code true} if the package is opened unconditionally. </p>
     *
     * <p> This method does not check if the given module reads this module. </p>
     *
     * @param pn The package name
     *
     * @return {@code true} if this module exports the package unconditionally
     *
     * @see ModuleDescriptor#exports()
     */
    // 判断当前模块是否将pn包export/open给了所有模块
    public boolean isExported(String pn) {
        Objects.requireNonNull(pn);
        return implIsExportedOrOpen(pn, EVERYONE_MODULE, /*open*/false);
    }
    
    /**
     * Returns {@code true} if this module exports the given package to at
     * least the given module.
     *
     * <p> This method returns {@code true} if invoked to test if a package in
     * this module is exported to itself. It always returns {@code true} when
     * invoked on an unnamed module. A package that is {@link #isOpen open} to
     * the given module is considered exported to that module at run-time and
     * so this method returns {@code true} if the package is open to the given
     * module. </p>
     *
     * <p> This method does not check if the given module reads this module. </p>
     *
     * @param pn    The package name
     * @param other The other module
     *
     * @return {@code true} if this module exports the package to at least the
     * given module
     *
     * @see ModuleDescriptor#exports()
     * @see #addExports(String, Module)
     */
    // 判断当前模块是否将pn包export/open给了other模块
    public boolean isExported(String pn, Module other) {
        Objects.requireNonNull(pn);
        Objects.requireNonNull(other);
        return implIsExportedOrOpen(pn, other, /*open*/false);
    }
    
    /**
     * If the caller's module is this module then update this module to export
     * the given package to the given module.
     *
     * <p> This method has no effect if the package is already exported (or
     * <em>open</em>) to the given module. </p>
     *
     * @param pn    The package name
     * @param other The module
     *
     * @return this module
     *
     * @throws IllegalArgumentException If {@code pn} is {@code null}, or this is a named module and the
     *                                  package {@code pn} is not a package in this module
     * @throws IllegalCallerException   If this is a named module and the caller's module is not this
     *                                  module
     * @apiNote As specified in section 5.4.3 of the <cite>The Java&trade;
     * Virtual Machine Specification </cite>, if an attempt to resolve a
     * symbolic reference fails because of a linkage error, then subsequent
     * attempts to resolve the reference always fail with the same error that
     * was thrown as a result of the initial resolution attempt.
     * @jvms 5.4.3 Resolution
     * @see #isExported(String, Module)
     */
    // 将当前模块的pn包export给other模块
    @CallerSensitive
    public Module addExports(String pn, Module other) {
        if(pn == null) {
            throw new IllegalArgumentException("package is null");
        }
        Objects.requireNonNull(other);
        
        if(isNamed()) {
            Module caller = getCallerModule(Reflection.getCallerClass());
            if(caller != this) {
                throw new IllegalCallerException(caller + " != " + this);
            }
            
            // 将当前模块的pn包export给other模块
            implAddExportsOrOpens(pn, other, /*open*/false, /*syncVM*/true);
        }
        
        return this;
    }
    
    /**
     * Returns {@code true} if this module reflectively exports the given package to the given module.
     */
    // 判断当前模块是否将pn包动态export给了other模块
    boolean isReflectivelyExported(String pn, Module other) {
        return isReflectivelyExportedOrOpen(pn, other, false);
    }
    
    /**
     * Updates this module to export a package to another module.
     *
     * @apiNote Used by Instrumentation::redefineModule and --add-exports
     */
    // 将当前模块的pn包export给other模块
    void implAddExports(String pn, Module other) {
        implAddExportsOrOpens(pn, other, false, true);
    }
    
    /**
     * Updates this module to export a package unconditionally.
     *
     * @apiNote This method is for JDK tests only.
     */
    // 将当前模块的pn包export给所有模块
    void implAddExports(String pn) {
        implAddExportsOrOpens(pn, Module.EVERYONE_MODULE, false, true);
    }
    
    /**
     * Updates this module to export a package to all unnamed modules.
     *
     * @apiNote Used by the --add-exports command line option.
     */
    // 将当前模块的pn包export给未命名模块
    void implAddExportsToAllUnnamed(String pn) {
        implAddExportsOrOpens(pn, Module.ALL_UNNAMED_MODULE, false, true);
    }
    
    /**
     * Updates a module to export a package to another module without
     * notifying the VM.
     *
     * @apiNote This method is for VM white-box testing.
     */
    // 将当前模块的pn包export给other模块，不通知VM
    void implAddExportsNoSync(String pn, Module other) {
        implAddExportsOrOpens(pn.replace('/', '.'), other, false, false);
    }
    
    /**
     * Updates this export to export a package unconditionally without
     * notifying the VM.
     *
     * @apiNote This method is for VM white-box testing.
     */
    // 将当前模块的pn包export给所有模块，不通知VM
    void implAddExportsNoSync(String pn) {
        implAddExportsOrOpens(pn.replace('/', '.'), Module.EVERYONE_MODULE, false, false);
    }
    
    /* ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ */
    
    
    /* ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ */
    
    /**
     * Returns {@code true} if this module has <em>opened</em> a package
     * unconditionally.
     *
     * <p> This method always returns {@code true} when invoked on an unnamed
     * module. Additionally, it always returns {@code true} when invoked on an
     * {@link ModuleDescriptor#isOpen open} module with a package in the
     * module. </p>
     *
     * <p> This method does not check if the given module reads this module. </p>
     *
     * @param pn The package name
     *
     * @return {@code true} if this module has <em>opened</em> the package
     * unconditionally
     *
     * @see ModuleDescriptor#opens()
     */
    // 判断当前模块是否将pn包open给了所有模块
    public boolean isOpen(String pn) {
        Objects.requireNonNull(pn);
        return implIsExportedOrOpen(pn, EVERYONE_MODULE, /*open*/true);
    }
    
    /**
     * Returns {@code true} if this module has <em>opened</em> a package to at
     * least the given module.
     *
     * <p> This method returns {@code true} if invoked to test if a package in
     * this module is open to itself. It returns {@code true} when invoked on an
     * {@link ModuleDescriptor#isOpen open} module with a package in the module.
     * It always returns {@code true} when invoked on an unnamed module. </p>
     *
     * <p> This method does not check if the given module reads this module. </p>
     *
     * @param pn    The package name
     * @param other The other module
     *
     * @return {@code true} if this module has <em>opened</em> the package
     * to at least the given module
     *
     * @see ModuleDescriptor#opens()
     * @see #addOpens(String, Module)
     * @see java.lang.reflect.AccessibleObject#setAccessible(boolean)
     * @see java.lang.invoke.MethodHandles#privateLookupIn
     */
    // 判断当前模块是否将pn包open给了other模块
    public boolean isOpen(String pn, Module other) {
        Objects.requireNonNull(pn);
        Objects.requireNonNull(other);
        return implIsExportedOrOpen(pn, other, /*open*/true);
    }
    
    /**
     * If this module has <em>opened</em> a package to at least the caller
     * module then update this module to open the package to the given module.
     * Opening a package with this method allows all types in the package,
     * and all their members, not just public types and their public members,
     * to be reflected on by the given module when using APIs that support
     * private access or a way to bypass or suppress default Java language
     * access control checks.
     *
     * <p> This method has no effect if the package is already <em>open</em>
     * to the given module. </p>
     *
     * @param pn    The package name
     * @param other The module
     *
     * @return this module
     *
     * @throws IllegalArgumentException If {@code pn} is {@code null}, or this is a named module and the
     *                                  package {@code pn} is not a package in this module
     * @throws IllegalCallerException   If this is a named module and this module has not opened the
     *                                  package to at least the caller's module
     * @apiNote This method can be used for cases where a <em>consumer
     * module</em> uses a qualified opens to open a package to an <em>API
     * module</em> but where the reflective access to the members of classes in
     * the consumer module is delegated to code in another module. Code in the
     * API module can use this method to open the package in the consumer module
     * to the other module.
     * @see #isOpen(String, Module)
     * @see java.lang.reflect.AccessibleObject#setAccessible(boolean)
     * @see java.lang.invoke.MethodHandles#privateLookupIn
     */
    // 将当前模块的pn包open给other模块
    @CallerSensitive
    public Module addOpens(String pn, Module other) {
        if(pn == null) {
            throw new IllegalArgumentException("package is null");
        }
        Objects.requireNonNull(other);
        
        if(isNamed()) {
            Module caller = getCallerModule(Reflection.getCallerClass());
            if(caller != this && (caller == null || !isOpen(pn, caller))) {
                throw new IllegalCallerException(pn + " is not open to " + caller);
            }
            
            // 将当前模块的pn包open给other模块
            implAddExportsOrOpens(pn, other, /*open*/true, /*syncVM*/true);
        }
        
        return this;
    }
    
    /**
     * Returns {@code true} if this module reflectively opens the
     * given package to the given module.
     */
    // 判断当前模块是否将pn包动态open给了other模块
    boolean isReflectivelyOpened(String pn, Module other) {
        return isReflectivelyExportedOrOpen(pn, other, true);
    }
    
    /**
     * Updates this module to open a package to another module.
     *
     * @apiNote Used by Instrumentation::redefineModule and --add-opens
     */
    // 将当前模块的pn包open给other模块
    void implAddOpens(String pn, Module other) {
        implAddExportsOrOpens(pn, other, true, true);
    }
    
    /**
     * Updates this module to open a package unconditionally.
     *
     * @apiNote This method is for JDK tests only.
     */
    // 将当前模块的pn包open给所有模块
    void implAddOpens(String pn) {
        implAddExportsOrOpens(pn, Module.EVERYONE_MODULE, true, true);
    }
    
    /**
     * Updates this module to open a package to all unnamed modules.
     *
     * @apiNote Used by the --add-opens command line option.
     */
    // 将当前模块的pn包open给未命名模块
    void implAddOpensToAllUnnamed(String pn) {
        implAddExportsOrOpens(pn, Module.ALL_UNNAMED_MODULE, true, true);
    }
    
    /**
     * Updates a module to open all packages returned by the given iterator to
     * all unnamed modules.
     *
     * @apiNote Used during startup to open packages for illegal access.
     */
    // 将迭代器中所有的包open给未命名模块
    void implAddOpensToAllUnnamed(Iterator<String> iterator) {
        if(jdk.internal.misc.VM.isModuleSystemInited()) {
            throw new IllegalStateException("Module system already initialized");
        }
        
        // replace this module's openPackages map with a new map that opens
        // the packages to all unnamed modules.
        Map<String, Set<Module>> openPackages = this.openPackages;
        if(openPackages == null) {
            openPackages = new HashMap<>();
        } else {
            openPackages = new HashMap<>(openPackages);
        }
        while(iterator.hasNext()) {
            String pn = iterator.next();
            Set<Module> prev = openPackages.putIfAbsent(pn, ALL_UNNAMED_MODULE_SET);
            if(prev != null) {
                prev.add(ALL_UNNAMED_MODULE);
            }
            
            // update VM to export the package
            addExportsToAllUnnamed0(this, pn);
        }
        this.openPackages = openPackages;
    }
    
    /* ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ */
    
    
    /**
     * Returns {@code true} if this module exports or opens the given package to the given module.
     * If the other module is {@code EVERYONE_MODULE} then this method tests if the package is exported or opened unconditionally.
     */
    // 判断当前模块是否将pn包导出/开放给了other模块
    private boolean implIsExportedOrOpen(String pn, Module other, boolean open) {
        // all packages in unnamed modules are open
        if(!isNamed()) {
            // 未命名模块导出所有包
            return true;
        }
        
        // all packages are exported/open to self
        if(other == this && descriptor.packages().contains(pn)) {
            // 自身对自身导出所有包
            return true;
        }
        
        // all packages in open and automatic modules are open
        if(descriptor.isOpen() || descriptor.isAutomatic()) {
            // open模块或自动模块也可看做导出了所有包
            return descriptor.packages().contains(pn);
        }
        
        // exported/opened via module declaration/descriptor
        if(isStaticallyExportedOrOpen(pn, other, open)) {
            // 当前模块将pn包静态导出/开放给了other模块
            return true;
        }
        
        // exported via addExports/addOpens
        if(isReflectivelyExportedOrOpen(pn, other, open)) {
            // 当前模块将pn包动态导出/开放给了other模块
            return true;
        }
        
        // not exported or open to other
        return false;
    }
    
    /**
     * Returns {@code true} if this module exports or opens a package to
     * the given module via its module declaration or CLI options.
     */
    // 判断当前模块是否将pn包静态导出/开放给了other模块
    private boolean isStaticallyExportedOrOpen(String pn, Module other, boolean open) {
        // test if package is open to everyone or <other>
        Map<String, Set<Module>> openPackages = this.openPackages;
        if(openPackages != null && allows(openPackages.get(pn), other)) {
            // 判断当前模块是否将pn包静态开放给了other模块
            return true;
        }
        
        // 如果是在判断静态导出
        if(!open) {
            // test package is exported to everyone or <other>
            Map<String, Set<Module>> exportedPackages = this.exportedPackages;
            return exportedPackages != null && allows(exportedPackages.get(pn), other);
        }
        
        // 如果是在判断静态开放，这里可以返回false了
        return false;
    }
    
    /**
     * Returns {@code true} if this module reflectively exports or opens the
     * given package to the given module.
     */
    // 判断当前模块是否将pn包动态导出/开放给了other模块
    private boolean isReflectivelyExportedOrOpen(String pn, Module other, boolean open) {
        // exported or open to all modules
        Map<String, Boolean> exports = ReflectionData.exports.get(this, EVERYONE_MODULE);
        if(exports != null) {
            Boolean b = exports.get(pn);
            if(b != null) {
                boolean isOpen = b;
                if(!open || isOpen) {
                    return true;
                }
            }
        }
        
        if(other != EVERYONE_MODULE) {
            // exported or open to other
            exports = ReflectionData.exports.get(this, other);
            if(exports != null) {
                Boolean b = exports.get(pn);
                if(b != null) {
                    boolean isOpen = b;
                    if(!open || isOpen) {
                        return true;
                    }
                }
            }
            
            // other is an unnamed module && exported or open to all unnamed
            if(!other.isNamed()) {
                exports = ReflectionData.exports.get(this, ALL_UNNAMED_MODULE);
                if(exports != null) {
                    Boolean b = exports.get(pn);
                    if(b != null) {
                        boolean isOpen = b;
                        if(!open || isOpen) {
                            return true;
                        }
                    }
                }
            }
            
        }
        
        return false;
    }
    
    /**
     * Returns {@code true} if targets is non-null and contains EVERYONE_MODULE
     * or the given module. Also returns true if the given module is an unnamed
     * module and targets contains ALL_UNNAMED_MODULE.
     */
    // 判断module是否在targets集合表示的范畴内
    private boolean allows(Set<Module> targets, Module module) {
        if(targets != null) {
            if(targets.contains(EVERYONE_MODULE)) {
                return true;
            }
            
            if(module != EVERYONE_MODULE) {
                if(targets.contains(module)) {
                    return true;
                }
                
                if(!module.isNamed() && targets.contains(ALL_UNNAMED_MODULE)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Updates a module to export or open a module to another module.
     *
     * If {@code syncVM} is {@code true} then the VM is notified.
     */
    /*
     * 将当前模块的pn包export或open给other模块
     * open指示是export操作还是open操作
     * syncVM指示是否通知VM
     */
    private void implAddExportsOrOpens(String pn, Module other, boolean open, boolean syncVM) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(pn);
        
        // all packages are open in unnamed, open, and automatic modules
        if(!isNamed() || descriptor.isOpen() || descriptor.isAutomatic()) {
            return;
        }
        
        // check if the package is already exported/open to other
        if(implIsExportedOrOpen(pn, other, open)) {
            
            // if the package is exported/open for illegal access then we need
            // to record that it has also been exported/opened reflectively so
            // that the IllegalAccessLogger doesn't emit a warning.
            boolean needToAdd = false;
            if(!other.isNamed()) {
                IllegalAccessLogger l = IllegalAccessLogger.illegalAccessLogger();
                if(l != null) {
                    if(open) {
                        needToAdd = l.isOpenForIllegalAccess(this, pn);
                    } else {
                        needToAdd = l.isExportedForIllegalAccess(this, pn);
                    }
                }
            }
            if(!needToAdd) {
                // nothing to do
                return;
            }
        }
        
        // can only export a package in the module
        if(!descriptor.packages().contains(pn)) {
            throw new IllegalArgumentException("package " + pn + " not in contents");
        }
        
        // update VM first, just in case it fails
        if(syncVM) {
            if(other == EVERYONE_MODULE) {
                addExportsToAll0(this, pn);
            } else if(other == ALL_UNNAMED_MODULE) {
                addExportsToAllUnnamed0(this, pn);
            } else {
                addExports0(this, pn, other);
            }
        }
        
        // add package name to exports if absent
        Map<String, Boolean> map = ReflectionData.exports.computeIfAbsent(this, other, (m1, m2) -> new ConcurrentHashMap<>());
        if(open) {
            // 如果是open操作，也要打开部分export权限
            map.put(pn, Boolean.TRUE);  // may need to promote from FALSE to TRUE
        } else {
            map.putIfAbsent(pn, Boolean.FALSE);
        }
    }
    
    /*▲ exported 和 open ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * If the caller's module is this module then update this module to add a
     * service dependence on the given service type. This method is intended
     * for use by frameworks that invoke {@link java.util.ServiceLoader
     * ServiceLoader} on behalf of other modules or where the framework is
     * passed a reference to the service type by other code. This method is
     * a no-op when invoked on an unnamed module or an automatic module.
     *
     * <p> This method does not cause {@link Configuration#resolveAndBind
     * resolveAndBind} to be re-run. </p>
     *
     * @param service The service type
     *
     * @return this module
     *
     * @throws IllegalCallerException If this is a named module and the caller's module is not this
     *                                module
     * @see #canUse(Class)
     * @see ModuleDescriptor#uses()
     */
    // 声明当前模块使用指定的服务
    @CallerSensitive
    public Module addUses(Class<?> service) {
        Objects.requireNonNull(service);
        
        if(isNamed() && !descriptor.isAutomatic()) {
            Module caller = getCallerModule(Reflection.getCallerClass());
            if(caller != this) {
                throw new IllegalCallerException(caller + " != " + this);
            }
            implAddUses(service);
        }
        
        return this;
    }
    
    /**
     * Indicates if this module has a service dependence on the given service type.
     * This method always returns {@code true} when invoked on an unnamed module or an automatic module.
     *
     * @param service The service type
     *
     * @return {@code true} if this module uses service type {@code st}
     *
     * @see #addUses(Class)
     */
    // 判断当前模块是否声明使用指定的服务
    public boolean canUse(Class<?> service) {
        Objects.requireNonNull(service);
        
        if(!isNamed()) {
            // 未命名模块可使用所有服务
            return true;
        }
        
        if(descriptor.isAutomatic()) {
            // 自动模块可使用所有服务
            return true;
        }
        
        // uses was declared
        if(descriptor.uses().contains(service.getName())) {
            // 存在静态声明
            return true;
        }
        
        // uses added via addUses
        if(ReflectionData.uses.containsKeyPair(this, service)){
            // 存在动态声明
            return true;
        }
        
        return false;
    }
    
    /**
     * Update this module to add a service dependence on the given service
     * type.
     */
    // 动态声明当前模块使用指定的服务
    void implAddUses(Class<?> service) {
        if(!canUse(service)) {
            ReflectionData.uses.putIfAbsent(this, service, Boolean.TRUE);
        }
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建模块 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Defines all module in a configuration to the runtime.
     *
     * @return a map of module name to runtime {@code Module}
     *
     * @throws IllegalArgumentException If defining any of the modules to the VM fails
     */
    // 定义指定模块图中的所有模块，返回模块名到模块实例的映射
    static Map<String, Module> defineModules(Configuration cf, Function<String, ClassLoader> clf, ModuleLayer layer) {
        boolean isBootLayer = (ModuleLayer.boot() == null);
        
        int cap = (int) (cf.modules().size() / 0.75f + 1.0f);
        
        // 当前模块层中模块名称到模块实例的映射
        Map<String, Module> nameToModule = new HashMap<>(cap);
        
        // 模块名称到模块类加载器的映射
        Map<String, ClassLoader> nameToLoader = new HashMap<>(cap);
        
        Set<ClassLoader> loaders = new HashSet<>();
        boolean hasPlatformModules = false;
        
        /* map each module to a class loader */
        // 遍历指定模块图中的所有已解析模块，创建模块名称到模块类加载器的映射
        for(ResolvedModule resolvedModule : cf.modules()) {
            String name = resolvedModule.name();
            ClassLoader loader = clf.apply(name);
            nameToLoader.put(name, loader);
            if(loader == null || loader == ClassLoaders.platformClassLoader()) {
                if(!(clf instanceof ModuleLoaderMap.Mapper)) {
                    throw new IllegalArgumentException("loader can't be 'null' or the platform class loader");
                }
                hasPlatformModules = true;
            } else {
                loaders.add(loader);
            }
        }
        
        /* define each module in the configuration to the VM */
        // 遍历指定模块图中的所有已解析模块，创建模块名称到模块实例的映射
        for(ResolvedModule resolvedModule : cf.modules()) {
            ModuleReference mref = resolvedModule.reference();
            ModuleDescriptor descriptor = mref.descriptor();
            String name = descriptor.name();
            ClassLoader loader = nameToLoader.get(name);
            Module m;
            if(loader == null && name.equals("java.base")) {
                // java.base is already defined to the VM
                m = Object.class.getModule();
            } else {
                URI uri = mref.location().orElse(null);
                m = new Module(layer, loader, descriptor, uri);
            }
            nameToModule.put(name, m);
        }
        
        /* setup readability and exports/opens */
        // 遍历指定模块图中的所有已解析模块，解析依赖模块
        for(ResolvedModule resolvedModule : cf.modules()) {
            ModuleReference mref = resolvedModule.reference();
            ModuleDescriptor descriptor = mref.descriptor();
            
            String mn = descriptor.name();
            Module m = nameToModule.get(mn);
            assert m != null;
            
            // reads
            Set<Module> reads = new HashSet<>();
            
            // name -> source Module when in parent layer
            Map<String, Module> nameToSource = Collections.emptyMap();  // 位于父模块层中的依赖模块
            
            // 遍历每个模块的模块依赖
            for(ResolvedModule other : resolvedModule.reads()) {
                // 依赖模块的实例
                Module m2 = null;
                
                // 依赖模块与当前模块处于同一个模块图
                if(other.configuration() == cf) {
                    // this configuration
                    m2 = nameToModule.get(other.name());
                    assert m2 != null;
                } else {
                    // 遍历父模块层
                    for(ModuleLayer parent : layer.parents()) {
                        // 在模块层parent中查找与other对应的模块
                        m2 = findModule(parent, other);
                        if(m2 != null) {
                            break;
                        }
                    }
                    
                    assert m2 != null;
                    
                    if(nameToSource.isEmpty()) {
                        nameToSource = new HashMap<>();
                    }
                    
                    nameToSource.put(other.name(), m2);
                }
                
                reads.add(m2);
                
                // update VM view
                addReads0(m, m2);
            }
            m.reads = reads;
            
            // automatic modules read all unnamed modules
            if(descriptor.isAutomatic()) {
                m.implAddReads(ALL_UNNAMED_MODULE, true);
            }
            
            // 解析exports和opens的包，跳过open模块和自动模块
            if(!descriptor.isOpen() && !descriptor.isAutomatic()) {
                if(isBootLayer && descriptor.opens().isEmpty()) {
                    // no open packages, no qualified exports to modules in parent layers
                    initExports(m, nameToModule);
                } else {
                    // 在nameToSource、nameToModule、layer.parents()中依次查找模块m中标记为opens以及exports的包
                    initExportsAndOpens(m, nameToSource, nameToModule, layer.parents());
                }
            }
        }
        
        // if there are modules defined to the boot or platform class loaders
        // then register the modules in the class loader's services catalog
        if(hasPlatformModules) {
            ClassLoader pcl = ClassLoaders.platformClassLoader();
            ServicesCatalog bootCatalog = BootLoader.getServicesCatalog();
            ServicesCatalog pclCatalog = ServicesCatalog.getServicesCatalog(pcl);
            for(ResolvedModule resolvedModule : cf.modules()) {
                ModuleReference mref = resolvedModule.reference();
                ModuleDescriptor descriptor = mref.descriptor();
                if(!descriptor.provides().isEmpty()) {
                    String name = descriptor.name();
                    Module m = nameToModule.get(name);
                    ClassLoader loader = nameToLoader.get(name);
                    if(loader == null) {
                        bootCatalog.register(m);
                    } else if(loader == pcl) {
                        pclCatalog.register(m);
                    }
                }
            }
        }
        
        // record that there is a layer with modules defined to the class loader
        for(ClassLoader loader : loaders) {
            // 将layer缓存到loader内部的CLV
            layer.bindToLoader(loader);
        }
        
        return nameToModule;
    }
    
    /**
     * Find the runtime Module corresponding to the given ResolvedModule in the given parent layer (or its parents).
     */
    // 在模块层parent中查找与resolvedModule对应的模块
    private static Module findModule(ModuleLayer parent, ResolvedModule resolvedModule) {
        Configuration cf = resolvedModule.configuration();
        String dn = resolvedModule.name();
        return parent.layers().filter(l -> l.configuration() == cf).findAny().map(layer -> {
            // 在当前模块层以及父模块层中查找指定名称的模块
            Optional<Module> om = layer.findModule(dn);
            assert om.isPresent() : dn + " not found in layer";
            Module m = om.get();
            assert m.getLayer() == layer : m + " not in expected layer";
            return m;
        }).orElse(null);
    }
    
    /**
     * Initialize/setup a module's exports.
     *
     * @param m            the module
     * @param nameToModule map of module name to Module (for qualified exports)
     */
    // 初始化exports包
    private static void initExports(Module m, Map<String, Module> nameToModule) {
        Map<String, Set<Module>> exportedPackages = new HashMap<>();
        
        for(Exports exports : m.getDescriptor().exports()) {
            String source = exports.source();
            if(exports.isQualified()) {
                // qualified exports
                Set<Module> targets = new HashSet<>();
                for(String target : exports.targets()) {
                    Module m2 = nameToModule.get(target);
                    if(m2 != null) {
                        addExports0(m, source, m2);
                        targets.add(m2);
                    }
                }
                if(!targets.isEmpty()) {
                    exportedPackages.put(source, targets);
                }
            } else {
                // unqualified exports
                addExportsToAll0(m, source);
                exportedPackages.put(source, EVERYONE_SET);
            }
        }
        
        if(!exportedPackages.isEmpty())
            m.exportedPackages = exportedPackages;
    }
    
    /**
     * Initialize/setup a module's exports.
     *
     * @param m            the module
     * @param nameToSource map of module name to Module for modules that m reads
     * @param nameToModule map of module name to Module for modules in the layer
     *                     under construction
     * @param parents      the parent layers
     */
    // 在nameToSource、nameToModule、parents中依次查找模块m中标记为opens以及exports的包
    private static void initExportsAndOpens(Module m, Map<String, Module> nameToSource, Map<String, Module> nameToModule, List<ModuleLayer> parents) {
        ModuleDescriptor descriptor = m.getDescriptor();
        Map<String, Set<Module>> openPackages = new HashMap<>();    // 标记为opens的模块与其open的目标模块的映射
        Map<String, Set<Module>> exportedPackages = new HashMap<>();    // 标记为exports的模块与其export的目标模块的映射
        
        // 首先遍历标记为opens的包
        for(Opens opens : descriptor.opens()) {
            String source = opens.source();
            
            // 如果包含指定的目标模块
            if(opens.isQualified()) {
                // qualified opens
                Set<Module> targets = new HashSet<>();
                
                // 遍历指定的目标模块
                for(String target : opens.targets()) {
                    // 在nameToSource、nameToModule、parents中依次查找目标模块target
                    Module m2 = findModule(target, nameToSource, nameToModule, parents);
                    if(m2 != null) {
                        addExports0(m, source, m2);
                        targets.add(m2);
                    }
                }
                if(!targets.isEmpty()) {
                    openPackages.put(source, targets);
                }
            } else {
                // 如果没有指定的目标模块，那就是open给所有模块
                addExportsToAll0(m, source);
                openPackages.put(source, EVERYONE_SET);
            }
        }
        
        // 其次遍历标记为exports的包，跳过已被标记为opens的包
        for(Exports exports : descriptor.exports()) {
            String source = exports.source();
            
            // skip export if package is already open to everyone
            Set<Module> openToTargets = openPackages.get(source);
            if(openToTargets != null && openToTargets.contains(EVERYONE_MODULE)) {
                continue;
            }
            
            if(exports.isQualified()) {
                // qualified exports
                Set<Module> targets = new HashSet<>();
                
                // 遍历指定的目标模块
                for(String target : exports.targets()) {
                    // 在nameToSource、nameToModule、parents中依次查找目标模块target
                    Module m2 = findModule(target, nameToSource, nameToModule, parents);
                    if(m2 != null) {
                        // skip qualified export if already open to m2
                        if(openToTargets == null || !openToTargets.contains(m2)) {
                            addExports0(m, source, m2);
                            targets.add(m2);
                        }
                    }
                }
                if(!targets.isEmpty()) {
                    exportedPackages.put(source, targets);
                }
            } else {
                // 如果没有指定的目标模块，那就是export给所有模块
                addExportsToAll0(m, source);
                exportedPackages.put(source, EVERYONE_SET);
            }
        }
        
        if(!openPackages.isEmpty()) {
            m.openPackages = openPackages;
        }
        
        if(!exportedPackages.isEmpty()) {
            m.exportedPackages = exportedPackages;
        }
    }
    
    /**
     * Find the runtime Module with the given name. The module name is the
     * name of a target module in a qualified exports or opens directive.
     *
     * @param target       The target module to find
     * @param nameToSource The modules in parent layers that are read
     * @param nameToModule The modules in the layer under construction
     * @param parents      The parent layers
     */
    // 在nameToSource、nameToModule、parents中依次查找目标模块target
    private static Module findModule(String target, Map<String, Module> nameToSource, Map<String, Module> nameToModule, List<ModuleLayer> parents) {
        Module m = nameToSource.get(target);
        if(m == null) {
            m = nameToModule.get(target);
            if(m == null) {
                for(ModuleLayer parent : parents) {
                    // 在parent模块层及其祖先模块层中查找指定名称的模块
                    m = parent.findModule(target).orElse(null);
                    if(m != null) {
                        break;
                    }
                }
            }
        }
        return m;
    }
    
    /*▲ 创建模块 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 注解 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // cached class file with annotations
    private volatile Class<?> moduleInfoClass;
    
    /**
     * {@inheritDoc}
     * This method returns an empty array when invoked on an unnamed module.
     */
    // 1-1 返回当前模块上所有类型的注解
    @Override
    public Annotation[] getAnnotations() {
        return moduleInfoClass().getAnnotations();
    }
    
    /**
     * {@inheritDoc}
     * This method returns {@code null} when invoked on an unnamed module.
     */
    // 1-2 返回当前模块上指定类型的注解
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return moduleInfoClass().getDeclaredAnnotation(annotationClass);
    }
    
    /**
     * {@inheritDoc}
     * This method returns an empty array when invoked on an unnamed module.
     */
    // 2-1 返回当前模块上所有类型的注解
    @Override
    public Annotation[] getDeclaredAnnotations() {
        return moduleInfoClass().getDeclaredAnnotations();
    }
    
    // 加载module-info.class，并缓存到moduleInfoClass
    private Class<?> moduleInfoClass() {
        Class<?> clazz = this.moduleInfoClass;
        if(clazz != null) {
            return clazz;
        }
        
        synchronized(this) {
            clazz = this.moduleInfoClass;
            if(clazz == null) {
                if(isNamed()) {
                    PrivilegedAction<Class<?>> pa = this::loadModuleInfoClass;
                    clazz = AccessController.doPrivileged(pa);
                }
                
                if(clazz == null) {
                    // 代表不存在module-info
                    class DummyModuleInfo {
                    }
                    clazz = DummyModuleInfo.class;
                }
                this.moduleInfoClass = clazz;
            }
            return clazz;
        }
    }
    
    // 加载module-info.class
    private Class<?> loadModuleInfoClass() {
        Class<?> clazz = null;
        try(InputStream in = getResourceAsStream("module-info.class")) {
            if(in != null) {
                clazz = loadModuleInfoClass(in);
            }
        } catch(Exception ignore) {
        }
        return clazz;
    }
    
    /**
     * Loads module-info.class as a package-private interface in a class loader that is a child of this module's class loader.
     */
    // 从输入流中解析出module-info.class
    private Class<?> loadModuleInfoClass(InputStream in) throws IOException {
        final String MODULE_INFO = "module-info";
        
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM6, cw) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                cw.visit(version, Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT + Opcodes.ACC_SYNTHETIC, MODULE_INFO, null, "java/lang/Object", null);
            }
            
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                // keep annotations
                return super.visitAnnotation(desc, visible);
            }
            
            @Override
            public void visitAttribute(Attribute attr) {
                // drop non-annotation attributes
            }
            
            @Override
            public ModuleVisitor visitModule(String name, int flags, String version) {
                // drop Module attribute
                return null;
            }
        };
        
        ClassReader cr = new ClassReader(in);
        cr.accept(cv, 0);
        byte[] bytes = cw.toByteArray();
        
        ClassLoader cl = new ClassLoader(loader) {
            @Override
            protected Class<?> findClass(String cn) throws ClassNotFoundException {
                if(cn.equals(MODULE_INFO)) {
                    return super.defineClass(cn, bytes, 0, bytes.length);
                } else {
                    throw new ClassNotFoundException(cn);
                }
            }
        };
        
        try {
            return cl.loadClass(MODULE_INFO);
        } catch(ClassNotFoundException e) {
            throw new InternalError(e);
        }
    }
    
    /*▲ 注解 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns the string representation of this module. For a named module,
     * the representation is the string {@code "module"}, followed by a space,
     * and then the module name. For an unnamed module, the representation is
     * the string {@code "unnamed module"}, followed by a space, and then an
     * implementation specific string that identifies the unnamed module.
     *
     * @return The string representation of this module
     */
    @Override
    public String toString() {
        if(isNamed()) {
            return "module " + name;
        } else {
            String id = Integer.toHexString(System.identityHashCode(this));
            return "unnamed module @" + id;
        }
    }
    
    
    
    // -- native methods --
    
    // JVM_DefineModule
    private static native void defineModule0(Module module, boolean isOpen, String version, String location, String[] pns);
    
    // JVM_AddReadsModule
    private static native void addReads0(Module from, Module to);
    
    // JVM_AddModuleExports
    private static native void addExports0(Module from, String pn, Module to);
    
    // JVM_AddModuleExportsToAll
    private static native void addExportsToAll0(Module from, String pn);
    
    // JVM_AddModuleExportsToAllUnnamed
    private static native void addExportsToAllUnnamed0(Module from, String pn);
    
    
    /**
     * The holder of data structures to support readability, exports, and
     * service use added at runtime with the reflective APIs.
     */
    // 记录运行时添加的read、export、open、use描述符
    private static class ReflectionData {
        /**
         * A module (1st key) reads another module (2nd key)
         */
        static final WeakPairMap<Module, Module, Boolean> reads = new WeakPairMap<>();
        
        /**
         * A module (1st key) exports or opens a package to another module (2nd key).
         * The map value is a map of package name to a boolean that indicates if the package is opened.
         */
        // 如果是open操作，标记为true，如果是export操作，标记为false
        static final WeakPairMap<Module, Module, Map<String, Boolean>> exports = new WeakPairMap<>();
        
        /**
         * A module (1st key) uses a service (2nd key)
         */
        static final WeakPairMap<Module, Class<?>, Boolean> uses = new WeakPairMap<>();
    }
    
}
