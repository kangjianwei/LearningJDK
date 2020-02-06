/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.module.Resources;

/**
 * A class loader that loads classes and resources from a collection of
 * modules, or from a single module where the class loader is a member
 * of a pool of class loaders.
 *
 * <p> The delegation model used by this ClassLoader differs to the regular
 * delegation model. When requested to load a class then this ClassLoader first
 * maps the class name to its package name. If there a module defined to the
 * Loader containing the package then the class loader attempts to load from
 * that module. If the package is instead defined to a module in a "remote"
 * ClassLoader then this class loader delegates directly to that class loader.
 * The map of package name to remote class loader is created based on the
 * modules read by modules defined to this class loader. If the package is not
 * local or remote then this class loader will delegate to the parent class
 * loader. This allows automatic modules (for example) to link to types in the
 * unnamed module of the parent class loader.
 *
 * @see ModuleLayer#defineModulesWithOneLoader
 * @see ModuleLayer#defineModulesWithManyLoaders
 */
// 模块类加载器，用来加载指定的模块
public final class Loader extends SecureClassLoader {
    
    static {
        // 将当前类加载器注册为并行
        ClassLoader.registerAsParallelCapable();
    }
    
    // 模块类加载器池，可能为null
    private final LoaderPool pool;  // the pool this loader is a member of; can be null
    
    // 父级类加载器，可能为null
    private final ClassLoader parent;   // parent ClassLoader, can be null
    
    // 模块名到模块引用的映射，此处的模块均会被当前类加载器加载
    private final Map<String, ModuleReference> nameToModule; // maps a module name to a module reference
    
    // 包名到(已解析)模块的映射
    private final Map<String, LoadedModule> localPackageToModule; // maps package name to a module loaded by this class loader
    
    /*
     * key是包名，该包名是nameToModule中那些模块的依赖模块导出的包，且这些导出的包对nameToModule中那些模块可视。
     * value是类加载器，该类加载器是上述依赖模块的类加载器
     */
    private final Map<String, ClassLoader> remotePackageToLoader = new ConcurrentHashMap<>(); // maps package name to a remote class loader, populated post initialization
    
    // 模块引用到模块阅读器的映射，缓存加载过的模块引用
    private final Map<ModuleReference, ModuleReader> moduleToReader = new ConcurrentHashMap<>(); // maps a module reference to a module reader, populated lazily
    
    // ACC used when loading classes and resources
    private final AccessControlContext acc;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a {@code Loader} in a loader pool that loads classes/resources
     * from one module.
     */
    // 用parent作为父级类加载器，为指定的模块构造一个模块类加载器；pool是模块类加载器池，池中包含模块名到模块类加载器的映射
    public Loader(ResolvedModule resolvedModule, LoaderPool pool, ClassLoader parent) {
        super("Loader-" + resolvedModule.name(), parent);
        
        this.pool = pool;
        
        this.parent = parent;
        
        // 模块引用
        ModuleReference mref = resolvedModule.reference();
        
        // 模块描述符
        ModuleDescriptor descriptor = mref.descriptor();
        // 模块名称
        String mn = descriptor.name();
        
        // 加入模块名到模块引用的映射
        this.nameToModule = Map.of(mn, mref);
        
        // 包名到模块的映射
        Map<String, LoadedModule> localPackageToModule = new HashMap<>();
        
        LoadedModule lm = new LoadedModule(mref);
        
        // 遍历模块描述符中列出的所有包，将包名与该模块描述符对应起来
        descriptor.packages().forEach(pn -> localPackageToModule.put(pn, lm));
        
        // 包名到(已解析)模块的映射
        this.localPackageToModule = localPackageToModule;
        
        this.acc = AccessController.getContext();
    }
    
    
    /**
     * Creates a {@code Loader} that loads classes/resources from a collection
     * of modules.
     *
     * @throws IllegalArgumentException If two or more modules have the same package
     */
    // 用parent作为父级类加载器，为指定模块集合中的所有模块构造一个模块类加载器
    public Loader(Collection<ResolvedModule> modules, ClassLoader parent) {
        super(parent);
    
        this.pool = null;
        this.parent = parent;
    
        Map<String, ModuleReference> nameToModule = new HashMap<>();
        Map<String, LoadedModule> localPackageToModule = new HashMap<>();
    
        // 遍历模块集合
        for(ResolvedModule resolvedModule : modules) {
            // 模块引用
            ModuleReference mref = resolvedModule.reference();
        
            // 模块描述符
            ModuleDescriptor descriptor = mref.descriptor();
            // 模块名称
            String mn = descriptor.name();
        
            // 加入模块名到模块引用的映射
            nameToModule.put(mn, mref);
        
            // 遍历模块描述符中列出的所有包，将包名与该模块描述符对应起来
            descriptor.packages().forEach(pn -> {
                LoadedModule lm = new LoadedModule(mref);
            
                // 加入包名到(已解析)模块的映射
                if(localPackageToModule.put(pn, lm) != null) {
                    throw new IllegalArgumentException("Package " + pn + " in more than one module");
                }
            });
        }
    
        this.nameToModule = nameToModule;
        this.localPackageToModule = localPackageToModule;
    
        this.acc = AccessController.getContext();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 资源加载 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 自顶向下加载资源，截止到调用此方法的类加载器。返回【首个】匹配到的资源的URL
    @Override
    public URL getResource(String name) {
        Objects.requireNonNull(name);
        
        // 先通过当前类加载器内加载
        URL url = findResource(name);
        if(url != null) {
            return url;
        }
        
        // 再通过父级类加载器加载
        if(parent != null) {
            url = parent.getResource(name);
        } else {
            url = BootLoader.findResource(name);
        }
        
        return url;
    }
    
    // 自顶向下加载资源，截止到调用此方法的类加载器。返回【所有】匹配资源的URL
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Objects.requireNonNull(name);
        
        // 使用当前类加载器加载指定名称的资源，返回所有找到的资源位置
        List<URL> urls = findResourcesAsList(name);
        
        // 通过父级类加载器继续查找资源
        Enumeration<URL> enumeration;
        
        if(parent != null) {
            enumeration = parent.getResources(name);
        } else {
            enumeration = BootLoader.findResources(name);
        }
        
        // concat the URLs with the URLs returned by the parent
        return new Enumeration<>() {
            final Iterator<URL> iterator = urls.iterator();
            
            @Override
            public boolean hasMoreElements() {
                return (iterator.hasNext() || enumeration.hasMoreElements());
            }
            
            @Override
            public URL nextElement() {
                if(iterator.hasNext()) {
                    return iterator.next();
                } else {
                    return enumeration.nextElement();
                }
            }
        };
    }
    
    // 使用当前类加载器在其下辖的所有模块中查找匹配的资源，只要找到一个就立即返回
    @Override
    public URL findResource(String name) {
        // 获取指定名称的资源所在的包
        String pn = Resources.toPackageName(name);
        
        // 获取目标模块
        LoadedModule module = localPackageToModule.get(pn);
        
        if(module != null) {
            try {
                // 使用当前类加载器在指定的模块路径下查找匹配的资源
                URL url = findResource(module.name(), name);
                if(url != null) {
                    if(name.endsWith(".class")          // 如果是".class"文件
                        || url.toString().endsWith("/") // 或者，是目录
                        || isOpen(module.mref(), pn)) {  // 或者，目标模块是否将pn包开放给了其他所有模块
                        return url;
                    }
                }
            } catch(IOException ioe) {
                // ignore
            }
            
        } else {
            for(ModuleReference mref : nameToModule.values()) {
                try {
                    // 使用当前类加载器在指定的模块路径下查找匹配的资源
                    URL url = findResource(mref.descriptor().name(), name);
                    if(url != null) {
                        return url;
                    }
                } catch(IOException ioe) {
                    // ignore
                }
            }
        }
        
        return null;
    }
    
    /**
     * Returns a URL to a resource of the given name in a module defined to
     * this class loader.
     */
    // 使用当前类加载器在指定的模块路径下查找匹配的资源
    @Override
    protected URL findResource(String mn, String name) throws IOException {
        ModuleReference mref = (mn != null) ? nameToModule.get(mn) : null;
        if(mref == null) {
            return null;   // not defined to this class loader
        }
        
        // locate resource
        URL url = null;
        
        try {
            url = AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                public URL run() throws IOException {
                    Optional<URI> ouri = moduleReaderFor(mref).find(name);
                    if(ouri.isPresent()) {
                        try {
                            return ouri.get().toURL();
                        } catch(MalformedURLException | IllegalArgumentException e) {
                        }
                    }
                    return null;
                }
            });
        } catch(PrivilegedActionException pae) {
            throw (IOException) pae.getCause();
        }
        
        // check access with permissions restricted by ACC
        if(url != null && System.getSecurityManager() != null) {
            try {
                URL urlToCheck = url;
                url = AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                    @Override
                    public URL run() throws IOException {
                        return URLClassPath.checkURL(urlToCheck);
                    }
                }, acc);
            } catch(PrivilegedActionException pae) {
                url = null;
            }
        }
        
        return url;
    }
    
    // 使用当前类加载器加载指定名称的资源，返回所有找到的资源位置
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        List<URL> urls = findResourcesAsList(name);
        return Collections.enumeration(urls);
    }
    
    /**
     * Finds the resources with the given name in this class loader.
     */
    // 使用当前类加载器加载指定名称的资源，返回所有找到的资源位置
    private List<URL> findResourcesAsList(String name) throws IOException {
        // 获取返回指定名称的资源所在的包
        String pn = Resources.toPackageName(name);
    
        // 查找对应的模块
        LoadedModule module = localPackageToModule.get(pn);
    
        if(module != null) {
            // 在指定的模块路径下查找匹配的资源
            URL url = findResource(module.name(), name);
        
            if(url != null && (name.endsWith(".class") || url.toString().endsWith("/") || isOpen(module.mref(), pn))) {
                return List.of(url);
            } else {
                return Collections.emptyList();
            }
        } else {
            List<URL> urls = new ArrayList<>();
        
            // 遍历当前类加载器可以访问到的所有模块
            for(ModuleReference mref : nameToModule.values()) {
                // 在指定的模块路径下查找匹配的资源
                URL url = findResource(mref.descriptor().name(), name);
                if(url != null) {
                    urls.add(url);
                }
            }
        
            return urls;
        }
    }
    
    /*▲ 资源加载 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 类加载 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Loads the class with the specified binary name.
     */
    // 加载类
    @Override
    protected Class<?> loadClass(String cn, boolean resolve) throws ClassNotFoundException {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            // 从类名中获取包名
            String pn = packageName(cn);
            if(!pn.isEmpty()) {
                sm.checkPackageAccess(pn);
            }
        }
        
        synchronized(getClassLoadingLock(cn)) {
            // 检查该类是否已被加载过
            Class<?> c = findLoadedClass(cn);
            
            if(c == null) {
                // 获取指定的类所在的模块
                LoadedModule loadedModule = findLoadedModule(cn);
                
                /* class is in module defined to this class loader */
                if(loadedModule != null) {
                    c = findClassInModuleOrNull(loadedModule, cn);
                    
                    /* type in another module or visible via the parent loader */
                } else {
                    // 从类名中获取包名
                    String pn = packageName(cn);
                    
                    // 从依赖模块中查找其对应的类加载器
                    ClassLoader loader = remotePackageToLoader.get(pn);
                    if(loader == null) {
                        // type not in a module read by any of the modules defined to this loader, so delegate to parent class loader
                        loader = parent;
                    }
                    
                    if(loader == null) {
                        c = BootLoader.loadClassOrNull(cn);
                    } else {
                        c = loader.loadClass(cn);
                    }
                    
                }
            }
            
            if(c == null) {
                throw new ClassNotFoundException(cn);
            }
            
            if(resolve) {
                resolveClass(c);
            }
            
            return c;
        }
    }
    
    
    /**
     * Finds the class with the specified binary name.
     */
    // 查找(定义)类
    @Override
    protected Class<?> findClass(String cn) throws ClassNotFoundException {
        Class<?> c = null;
        
        // 获取指定的类所在的模块
        LoadedModule loadedModule = findLoadedModule(cn);
        if(loadedModule != null) {
            // 查找(定义)类
            c = findClassInModuleOrNull(loadedModule, cn);
        }
        
        if(c == null) {
            throw new ClassNotFoundException(cn);
        }
        
        return c;
    }
    
    /**
     * Finds the class with the specified binary name in the given module.
     * This method returns {@code null} if the class cannot be found.
     */
    // 查找(定义)类，需要指定模块名称
    @Override
    protected Class<?> findClass(String mn, String cn) {
        Class<?> c = null;
        
        // 获取指定的类所在的模块
        LoadedModule loadedModule = findLoadedModule(cn);
        
        if(loadedModule != null && loadedModule.name().equals(mn)) {
            // 查找(定义)类
            c = findClassInModuleOrNull(loadedModule, cn);
        }
        
        return c;
    }
    
    /**
     * Finds the class with the specified binary name if in a module
     * defined to this ClassLoader.
     *
     * @return the resulting Class or {@code null} if not found
     */
    // 查找(定义)类
    private Class<?> findClassInModuleOrNull(LoadedModule loadedModule, String cn) {
        PrivilegedAction<Class<?>> pa = () -> defineClass(cn, loadedModule);
        return AccessController.doPrivileged(pa, acc);
    }
    
    
    /**
     * Defines the given binary class name to the VM, loading the class
     * bytes from the given module.
     *
     * @return the resulting Class or {@code null} if an I/O error occurs
     */
    // 定义类
    private Class<?> defineClass(String cn, LoadedModule loadedModule) {
        // 在缓存中查找该模块对应的模块阅读器
        ModuleReader reader = moduleReaderFor(loadedModule.mref());
    
        try {
            // 类的全限定名替换为路径
            String rn = cn.replace('.', '/').concat(".class");
        
            // 读取该class文件
            ByteBuffer bb = reader.read(rn).orElse(null);
            if(bb == null) {
                return null;    // class not found
            }
        
            try {
                // 利用存储在缓冲区中的字节码去定义类
                return defineClass(cn, bb, loadedModule.codeSource());
            } finally {
                reader.release(bb);
            }
        
        } catch(IOException ioe) {
            // TBD on how I/O errors should be propagated
            return null;
        }
    }
    
    /*▲ 类加载 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Completes initialization of this Loader. This method populates
     * remotePackageToLoader with the packages of the remote modules, where
     * "remote modules" are the modules read by modules defined to this loader.
     *
     * @param cf                 the Configuration containing at least modules to be defined to
     *                           this class loader
     * @param parentModuleLayers the parent ModuleLayers
     */
    // 初始化依赖包到类加载器的映射，参见字段remotePackageToLoader的说明
    public Loader initRemotePackageMap(Configuration cf, List<ModuleLayer> parentModuleLayers) {
        
        // 遍历当前模块类加载器可以加载模块
        for(String name : nameToModule.keySet()) {
            
            // 查找该模块名对应的模块
            ResolvedModule resolvedModule = cf.findModule(name).get();
            assert resolvedModule.configuration() == cf;
            
            // 遍历resolvedModule中依赖(requires)的模块
            for(ResolvedModule other : resolvedModule.reads()) {
                // 获取依赖模块的名称
                String mn = other.name();
                
                // 依赖模块所在的模块图
                Configuration configuration = other.configuration();
                
                ClassLoader loader;
                
                // 如果依赖模块与当前模块位于同一个模块图
                if(configuration == cf) {
                    /*
                     * The module reads another module in the newly created layer.
                     * If all modules are defined to the same class loader then the packages are local.
                     */
                    // 如果模块类加载器池为空，说明该依赖模块与当前模块共用一个模块类加载器
                    if(pool == null) {
                        assert nameToModule.containsKey(mn);
                        continue;
                    }
                    
                    // 获取依赖模块可以使用的模块类加载器
                    loader = pool.loaderFor(mn);
                    
                    assert loader != null;
                    
                } else {
                    /* find the layer for the target module */
                    // 遍历父模块层，查找依赖模块所在的模块
                    ModuleLayer layer = parentModuleLayers.stream()
                        // 在模块层moduleLayer以及父模块层中查找包含模块图configuration的模块层
                        .map(moduleLayer -> findModuleLayer(moduleLayer, configuration)).flatMap(Optional::stream).findAny().orElseThrow(() -> new InternalError("Unable to find parent layer"));
                    
                    /*
                     * find the class loader for the module.
                     * For now we use the platform loader for modules defined to the boot loader
                     */
                    assert layer.findModule(mn).isPresent();
                    
                    // 获取依赖模块使用的类加载器，如果不存在，则使用platform类加载器
                    loader = layer.findLoader(mn);
                    if(loader == null) {
                        loader = ClassLoaders.platformClassLoader();
                    }
                }
                
                /* find the packages that are exported to the target module */
                // 当前模块的名称
                String target = resolvedModule.name();
                
                // 依赖模块的模块描述符
                ModuleDescriptor descriptor = other.reference().descriptor();
                
                // 遍历依赖模块导出的包
                for(ModuleDescriptor.Exports pkg : descriptor.exports()) {
                    
                    // 指示依赖模块是否将pkg包导出给了target模块
                    boolean delegate;
                    
                    // 判断是否存在exports...to...
                    if(pkg.isQualified()) {
                        // qualified export in same configuration
                        delegate = (other.configuration() == cf) && pkg.targets().contains(target);
                        
                        // 如果只是exports
                    } else {
                        // unqualified
                        delegate = true;
                    }
                    
                    if(delegate) {
                        // 获取导出的包名
                        String pn = pkg.source();
                        
                        ClassLoader l = remotePackageToLoader.putIfAbsent(pn, loader);
                        
                        if(l != null && l != loader) {
                            throw new IllegalArgumentException("Package " + pn + " cannot be imported from multiple loaders");
                        }
                    }
                }
            }
            
        }
        
        return this;
    }
    
    /**
     * Returns the loader pool that this loader is in or {@code null} if this
     * loader is not in a loader pool.
     */
    // 返回模块类加载器池
    public LoaderPool pool() {
        return pool;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Find the layer corresponding to the given configuration in the tree of layers rooted at the given parent.
     */
    // 在模块层moduleLayer以及父模块层中查找包含模块图cf的模块层
    private Optional<ModuleLayer> findModuleLayer(ModuleLayer moduleLayer, Configuration cf) {
        return SharedSecrets.getJavaLangAccess()
            // 深度遍历模块层moduleLayer以及父模块层，返回包含了遍历到的所有模块的流
            .layers(moduleLayer)
            // 选出模块图是cf的模块层
            .filter(layer -> layer.configuration() == cf).findAny();
    }
    
    /**
     * Find the candidate module for the given class name.
     * Returns {@code null} if none of the modules defined to this
     * class loader contain the API package for the class.
     */
    // 返回指定的类所在的模块
    private LoadedModule findLoadedModule(String cn) {
        // 从类名中获取包名
        String pn = packageName(cn);
        
        return pn.isEmpty() ? null : localPackageToModule.get(pn);
    }
    
    /**
     * Returns the package name for the given class name
     */
    // 从类名中获取包名
    private String packageName(String cn) {
        int pos = cn.lastIndexOf('.');
        return (pos<0) ? "" : cn.substring(0, pos);
    }
    
    /**
     * Returns the ModuleReader for the given module.
     */
    // 缓存/查找加载过的模块引用
    private ModuleReader moduleReaderFor(ModuleReference mref) {
        return moduleToReader.computeIfAbsent(mref, m -> createModuleReader(mref));
    }
    
    /**
     * Creates a ModuleReader for the given module.
     */
    // 获取指定模块的模块阅读器
    private ModuleReader createModuleReader(ModuleReference mref) {
        try {
            return mref.open();
        } catch(IOException e) {
            // Return a null module reader to avoid a future class load attempting to open the module again.
            return new NullModuleReader();
        }
    }
    
    /**
     * Returns true if the given module opens the given package unconditionally.
     *
     * @implNote This method currently iterates over each of the open
     * packages. This will be replaced once the ModuleDescriptor.Opens
     * API is updated.
     */
    // 判断指定的模块是否将pn包开放给了其他所有模块
    private boolean isOpen(ModuleReference mref, String pn) {
        ModuleDescriptor descriptor = mref.descriptor();
    
        // 如果当前模块是open模块或是自动模块，直接返回true
        if(descriptor.isOpen() || descriptor.isAutomatic()) {
            return true;
        }
    
        // 遍历当前模块开放的包
        for(ModuleDescriptor.Opens opens : descriptor.opens()) {
            // 开放的包名
            String source = opens.source();
        
            // 如果是opens...
            if(!opens.isQualified() && source.equals(pn)) {
                return true;
            }
        }
    
        return false;
    }
    
    /**
     * Returns the permissions for the given CodeSource.
     */
    @Override
    protected PermissionCollection getPermissions(CodeSource cs) {
        PermissionCollection perms = super.getPermissions(cs);
        
        URL url = cs.getLocation();
        if(url == null)
            return perms;
        
        // add the permission to access the resource
        try {
            Permission p = url.openConnection().getPermission();
            if(p != null) {
                // for directories then need recursive access
                if(p instanceof FilePermission) {
                    String path = p.getName();
                    if(path.endsWith(File.separator)) {
                        path += "-";
                        p = new FilePermission(path, "read");
                    }
                }
                perms.add(p);
            }
        } catch(IOException ioe) {
        }
        
        return perms;
    }
    
    
    /**
     * A ModuleReader that doesn't read any resources.
     */
    // 空的模块阅读器，不会读取到任何数据
    private static class NullModuleReader implements ModuleReader {
        @Override
        public Optional<URI> find(String name) {
            return Optional.empty();
        }
        
        @Override
        public Stream<String> list() {
            return Stream.empty();
        }
        
        @Override
        public void close() {
            throw new InternalError("Should not get here");
        }
    }
    
    /**
     * A module defined/loaded to a {@code Loader}.
     */
    // 由Loader加载的模块
    private static class LoadedModule {
        private final ModuleReference mref;
        private final URL url;              // may be null
        private final CodeSource cs;
        
        LoadedModule(ModuleReference mref) {
            URL url = null;
            if(mref.location().isPresent()) {
                try {
                    url = mref.location().get().toURL();
                } catch(MalformedURLException | IllegalArgumentException e) {
                }
            }
            this.mref = mref;
            this.url = url;
            this.cs = new CodeSource(url, (CodeSigner[]) null);
        }
        
        ModuleReference mref() {
            return mref;
        }
        
        String name() {
            return mref.descriptor().name();
        }
        
        URL location() {
            return url;
        }
        
        CodeSource codeSource() {
            return cs;
        }
    }
    
}
