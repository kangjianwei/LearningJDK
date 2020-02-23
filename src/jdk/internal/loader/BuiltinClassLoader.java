/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import jdk.internal.misc.VM;
import jdk.internal.module.ModulePatcher.PatchedModuleReader;
import jdk.internal.module.Resources;

/**
 * The platform or application class loader. Resources loaded from modules
 * defined to the boot class loader are also loaded via an instance of this
 * ClassLoader type.
 *
 * <p> This ClassLoader supports loading of classes and resources from modules.
 * Modules are defined to the ClassLoader by invoking the {@link #loadModule}
 * method. Defining a module to this ClassLoader has the effect of making the
 * types in the module visible. </p>
 *
 * <p> This ClassLoader also supports loading of classes and resources from a
 * class path of URLs that are specified to the ClassLoader at construction
 * time. The class path may expand at runtime (the Class-Path attribute in JAR
 * files or via instrumentation agents). </p>
 *
 * <p> The delegation model used by this ClassLoader differs to the regular
 * delegation model. When requested to load a class then this ClassLoader first
 * maps the class name to its package name. If there is a module defined to a
 * BuiltinClassLoader containing this package then the class loader delegates
 * directly to that class loader. If there isn't a module containing the
 * package then it delegates the search to the parent class loader and if not
 * found in the parent then it searches the class path. The main difference
 * between this and the usual delegation model is that it allows the platform
 * class loader to delegate to the application class loader, important with
 * upgraded modules defined to the platform class loader.
 */
// 三种内置类加载器的父类
public class BuiltinClassLoader extends SecureClassLoader {
    
    /** maps package name to loaded module for modules in the boot layer */
    // 内置类加载器加载的<包名, 模块>的映射，为所有类加载器共享
    private static final Map<String, LoadedModule> packageToModule = new ConcurrentHashMap<>(1024);
    
    /** maps a module name to a module reference */
    // 当前类加载器加载的<模块名, 模块引用>映射
    private final Map<String, ModuleReference> nameToModule;
    
    /** maps a module reference to a module reader */
    // 当前类加载器加载的<模块引用, 模块阅读器>映射，通常当被加载模块未编译时用到
    private final Map<ModuleReference, ModuleReader> moduleToReader;
    
    /** cache of resource name -> list of URLs. used only for resources that are not in module packages */
    // 缓存当前类加载器已搜索过的资源名称与其URL的映射
    private volatile SoftReference<Map<String, List<URL>>> resourceCache;
    
    /*
     * 父级类加载器[影子]
     *
     * 这与祖先类ClassLoader中的parent略有不同。
     * 在当前类加载器为PlatformClassLoader时，
     * 此处的parent显示为BootClassLoader，但祖先类ClassLoader中的parent却为null
     *
     * 为了与ClassLoader中的parent区分，故在其名称后面加了一个[影子]标记
     */
    private final BuiltinClassLoader parent;
    
    /** the URL class path, or null if there is no class path */
    // 当前类加载器关联的类路径
    private final URLClassPath ucp;
    
    
    static {
        // 将当前类加载器注册为并行
        if(!ClassLoader.registerAsParallelCapable()) {
            throw new InternalError("Unable to register as parallel capable");
        }
    }
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Create a new instance.
     */
    BuiltinClassLoader(String name, BuiltinClassLoader parent, URLClassPath ucp) {
        // 确保PlatformClassLoader的getParent()为null
        super(name, parent == null || parent == ClassLoaders.bootLoader() ? null : parent);
        
        this.parent = parent;
        this.ucp = ucp;
        
        this.nameToModule = new ConcurrentHashMap<>();
        this.moduleToReader = new ConcurrentHashMap<>();
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找资源(局部) ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Finds a resource with the given name in the modules defined to this class loader or its class path.
     */
    /*
     * 在当前类加载器可以访问到的模块路径/类路径下搜索首个匹配的资源（遍历所有可能的位置，一发现匹配资源就返回）
     *
     * 注：resName是相对于模块路径或类路径的根目录下的相对路径
     */
    @Override
    public URL findResource(String resName) {
        // 获取待查找资源所在的包
        String packageName = Resources.toPackageName(resName);
    
        // 获取待查找资源所在的模块
        LoadedModule module = packageToModule.get(packageName);
    
        // 如果找到了对应的模块，说明之前加载过该"包"内的类
        if(module != null) {
            /*
             * resource is in a package of a module defined to this loader
             *
             * 如果目标模块的类加载器就是当前类加载器，则可以直接使用当前类加载器查找资源；
             * 否则，该资源所在的包可能是由别的类加载器加载的，当前类加载器无权过问。
             */
            if(module.loader() == this) {
                URL url;
                try {
                    // 在指定的模块路径或当前类加载器的类路径下查找匹配的资源
                    url = findResource(module.name(), resName);
                } catch(IOException ioe) {
                    return null;
                }
        
                // 如果找到了该资源
                if(url != null) {
                    // 满足以下条件时才能返回资源的URL
                    if(resName.endsWith(".class")               // 待查找资源以".class"结尾
                        || url.toString().endsWith("/")         // 或者，待查找资源是目录
                        || isOpen(module.mref(), packageName)) { // 或者，目标模块将资源所在的包packageName开放(opens)给了所有其它模块
                        return url;
                    }
                }
            }
    
            // 如果资源位于未命名模块，或者资源所在的命名模块不是当前类加载器加载的
        } else {
            // not in a module package but may be in module defined to this loader
            try {
                // 尝试在当前类加载器下辖的所有模块内查找所有匹配的资源
                List<URL> urls = findMiscResource(resName);
    
                // 如果存在目标资源
                if(!urls.isEmpty()) {
                    URL url = urls.get(0);
                    // 一旦发现首个匹配的资源就立即返回
                    if(url != null) {
                        return checkURL(url); // check access before returning
                    }
                }
            } catch(IOException ioe) {
                return null;
            }
        }
    
        // 尝试在当前类加载器关联的类路径(的根目录)下搜索首个匹配的资源
        URL url = findResourceOnClassPath(resName);
    
        // 检查url的可访问性
        return checkURL(url);
    }
    
    /**
     * Returns a URL to a resource of the given name in a module defined to this class loader.
     */
    /*
     * 在指定的模块路径或当前类加载器的类路径下查找匹配的资源
     *
     * 如果moduleName为null，则在当前类加载器关联的类路径(根目录)下查找首个匹配的资源
     * 如果moduleName不为null，则在模块路径(根目录)下查找首个匹配的资源
     */
    @Override
    public URL findResource(String moduleName, String resName) throws IOException {
        URL url = null;
        
        // find in classpath
        if(moduleName == null) {
            // 尝试在当前类加载器关联的类路径(根目录)下搜索首个匹配的资源
            url = findResourceOnClassPath(resName);
        } else {
            // find in module
            ModuleReference mref = nameToModule.get(moduleName);
            if(mref != null) {
                // 尝试在指定模块的模块路径(根目录)中搜索匹配的资源
                url = findResource(mref, resName);
            }
        }
        
        return checkURL(url);  // check access before returning
    }
    
    /**
     * Returns an enumeration of URL objects to all the resources with the
     * given name in modules defined to this class loader or on the class
     * path of this loader.
     */
    /*
     * 在当前类加载器下辖的模块路径/类路径的根目录下搜索所有匹配的资源
     *
     * 注：resName是相对于模块路径或类路径的根目录下的相对路径
     */
    @Override
    public Enumeration<URL> findResources(String resName) throws IOException {
    
        // 缓存在模块路径下找到的资源
        List<URL> checked = new ArrayList<>();  // list of checked URLs
    
        // 获取待查找资源所在的包
        String packageName = Resources.toPackageName(resName);
    
        // 获取待加载资源的定位信息
        LoadedModule module = packageToModule.get(packageName);
    
        // 如果找到了对应的模块，说明之前加载过该"包"内的类
        if(module != null) {
            /*
             * resource is in a package of a module defined to this loader
             *
             * 如果目标模块的类加载器就是当前类加载器，则可以直接使用当前类加载器查找资源；
             * 否则，该资源所在的包可能是由别的类加载器加载的，当前类加载器无权过问。
             */
            if(module.loader() == this) {
                // 在指定的模块路径或当前类加载器的类路径下查找匹配的资源
                URL url = findResource(module.name(), resName); // checks URL
            
                // 如果找到了该资源
                if(url != null) {
                    // 满足以下条件时才能返回资源的URL
                    if(resName.endsWith(".class")               // 待查找资源以".class"结尾
                        || url.toString().endsWith("/")         // 或者，待查找资源是目录
                        || isOpen(module.mref(), packageName)) { // 或者，目标模块将资源所在的包packageName开放(opens)给了所有其它模块
                        checked.add(url);
                    }
                }
            }
        } else {
            /* not in a package of a module defined to this loader */
            // 尝试在当前类加载器下辖的所有模块内查找所有匹配的资源
            List<URL> urls = findMiscResource(resName);
        
            // 检查找到的资源路径
            for(URL url : urls) {
                url = checkURL(url);
                // 一旦发现匹配的资源就缓存起来
                if(url != null) {
                    checked.add(url);
                }
            }
        }
    
        /* class path (not checked) */
        // 尝试在类路径上搜索所有匹配的资源
        Enumeration<URL> enumeration = findResourcesOnClassPath(resName);
    
        /* concat the checked URLs and the (not checked) class path */
        // 连接模块路径和类路径下找到的所有资源
        return new Enumeration<>() {
            final Iterator<URL> iterator = checked.iterator();
            URL next;
        
            @Override
            public boolean hasMoreElements() {
                return hasNext();
            }
        
            @Override
            public URL nextElement() {
                if(hasNext()) {
                    URL result = next;
                    next = null;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }
            
            private boolean hasNext() {
                if(next != null) {
                    return true;
                } else if(iterator.hasNext()) {
                    next = iterator.next();
                    return true;
                } else {
                    // need to check each URL
                    while(enumeration.hasMoreElements() && next == null) {
                        next = checkURL(enumeration.nextElement());
                    }
                    return next != null;
                }
            }
        };
    }
    
    /**
     * Returns an input stream to a resource of the given name in a module defined to this class loader.
     */
    // 在指定的模块路径(根目录)或当前类加载器关联的类路径(根目录)下查找匹配的资源；如果成功找到资源，则返回其入流
    public InputStream findResourceAsStream(String moduleName, String resName) throws IOException {
    
        /* find in module defined to this loader, no security manager */
        // 如果不存在安全管理器，且模块名非空，进行快捷查找
        if(System.getSecurityManager() == null && moduleName != null) {
        
            // 查找模块引用
            ModuleReference mref = nameToModule.get(moduleName);
            if(mref == null) {
                return null;
            }
        
            // 获取模块阅读器，以便读取指定的模块
            ModuleReader moduleReader = moduleReaderFor(mref);
        
            // 打开模块中指定资源的输入流以便外界读取
            Optional<InputStream> streamOptional = moduleReader.open(resName);
        
            return streamOptional.orElse(null);
        } else {
            // 在指定的模块路径或当前类加载器的类路径下查找匹配的资源
            URL url = findResource(moduleName, resName);
        
            return (url != null) ? url.openStream() : null;
        }
    
    }
    
    
    /**
     * Returns a URL to a resource on the class path.
     */
    // 尝试在当前类加载器关联的类路径(根目录)下搜索首个匹配的资源
    private URL findResourceOnClassPath(String resName) {
        // 如果当前类加载器没有关联类路径，返回null
        if(!hasClassPath()) {
            return null;    // no class path
        }
        
        // 不存在安全管理器
        if(System.getSecurityManager() == null) {
            return ucp.findResource(resName, false);
        }
        
        PrivilegedAction<URL> pa = () -> ucp.findResource(resName, false);
        return AccessController.doPrivileged(pa);
    }
    
    /**
     * Returns the URL to a resource in a module or {@code null} if not found.
     */
    // 尝试在指定模块的模块路径(根目录)中搜索匹配的资源
    private URL findResource(ModuleReference mref, String resName) throws IOException {
        URI uri;
        
        // 不存在安全管理器
        if(System.getSecurityManager() == null) {
            // 获取模块阅读器，以便读取指定的模块
            ModuleReader moduleReader = moduleReaderFor(mref);
            // 返回指定资源在模块中的位置
            Optional<URI> uriOptional = moduleReader.find(resName);
            uri = uriOptional.orElse(null);
        } else {
            try {
                uri = AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                    @Override
                    public URI run() throws IOException {
                        ModuleReader moduleReader = moduleReaderFor(mref);
                        Optional<URI> uriOptional = moduleReader.find(resName);
                        return uriOptional.orElse(null);
                    }
                });
            } catch(PrivilegedActionException pae) {
                throw (IOException) pae.getCause();
            }
        }
        
        if(uri != null) {
            try {
                return uri.toURL();
            } catch(MalformedURLException | IllegalArgumentException ignored) {
            }
        }
        
        return null;
    }
    
    /**
     * Returns the URL to a resource in a module. Returns {@code null} if not found or an I/O error occurs.
     */
    // 尝试在指定模块的模块路径(根目录)中搜索匹配的资源；发生IOException时返回null
    private URL findResourceOrNull(ModuleReference mref, String resName) {
        try {
            // 尝试在指定模块的模块路径(根目录)中搜索匹配的资源
            return findResource(mref, resName);
        } catch(IOException ignore) {
            return null;
        }
    }
    
    /**
     * Returns the list of URLs to a "miscellaneous" resource in modules defined to this loader.
     * A miscellaneous resource is not in a module package, e.g. META-INF/services/p.S.
     *
     * The cache used by this method avoids repeated searching of all modules.
     */
    // 尝试在当前类加载器下辖的所有模块内查找所有匹配的资源
    private List<URL> findMiscResource(String resName) throws IOException {
        
        // 获取当前类加载器内已缓存的资源路径信息
        SoftReference<Map<String, List<URL>>> ref = this.resourceCache;
        Map<String, List<URL>> map = (ref != null) ? ref.get() : null;
        if(map == null) {
            map = new ConcurrentHashMap<>();
            this.resourceCache = new SoftReference<>(map);
        } else {
            List<URL> urls = map.get(resName);
            if(urls != null) {
                return urls;
            }
        }
    
        // 存储在当前类加载器下辖的模块中查找到的目标资源
        List<URL> urls;
        
        try {
            urls = AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                @Override
                public List<URL> run() throws IOException {
                    List<URL> result = null;
    
                    // 遍历当前类加载器加载过的所有模块，以便查找该资源的路径信息
                    for(ModuleReference mref : nameToModule.values()) {
                        // 获取模块阅读器，以便读取指定的模块
                        ModuleReader moduleReader = moduleReaderFor(mref);
    
                        // 返回指定资源在模块中的位置
                        URI uri = moduleReader.find(resName).orElse(null);
                        if(uri == null) {
                            continue;
                        }
    
                        try {
                            if(result == null) {
                                result = new ArrayList<>();
                            }
        
                            // 记录该资源的路径信息
                            result.add(uri.toURL());
                        } catch(MalformedURLException | IllegalArgumentException ignored) {
                        }
                    }
                    
                    return (result != null) ? result : Collections.emptyList();
                }
            });
        } catch(PrivilegedActionException pae) {
            throw (IOException) pae.getCause();
        }
        
        // only cache resources after VM is fully initialized
        if(VM.isModuleSystemInited()) {
            // 只有当模块系统初始化完成之后，才缓存搜索过的资源和对应的URL列表
            map.putIfAbsent(resName, urls);
        }
        
        return urls;
    }
    
    /**
     * Returns the URLs of all resources of the given name on the class path.
     */
    // 尝试在类路径下搜索所有匹配的资源
    private Enumeration<URL> findResourcesOnClassPath(String resName) {
        // 如果不存在类路径，返回一个空集
        if(!hasClassPath()) {
            // no class path
            return Collections.emptyEnumeration();
        }
    
        // 存在安全管理器
        if(System.getSecurityManager() != null) {
            PrivilegedAction<Enumeration<URL>> pa = () -> ucp.findResources(resName, false);
            return AccessController.doPrivileged(pa);
        }
    
        // 不存在安全管理器
        return ucp.findResources(resName, false);
    }
    
    /*▲ 查找资源(局部) ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 加载类 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Loads the class with the specified binary name.
     */
    // 由给定的类名加载类。如果找不到该类则抛出异常
    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        // 由给定的类名加载类。如果找不到该类则返回null
        Class<?> c = loadClassOrNull(className, resolve);
    
        if(c == null) {
            throw new ClassNotFoundException(className);
        }
    
        return c;
    }
    
    /**
     * A variation of {@code loadClass} to load a class with the specified
     * binary name. This method returns {@code null} when the class is not
     * found.
     */
    // 由给定的类名加载类，常用于切换待加载类的上下文（切换使用的类加载器）
    protected Class<?> loadClassOrNull(String className) {
        return loadClassOrNull(className, false);
    }
    
    /**
     * A variation of {@code loadClass} to load a class with the specified binary name.
     * This method returns {@code null} when the class is not found.
     */
    // 由给定的类名加载类。如果找不到该类则返回null
    protected Class<?> loadClassOrNull(String className, boolean resolve) {
        synchronized(getClassLoadingLock(className)) {
            
            // 首先检查该类是否已经加载
            Class<?> c = findLoadedClass(className);
            
            if(c == null) {
                
                // 返回指定类关联的定位信息，如果该类位于未命名的包中，则返回null
                LoadedModule loadedModule = findLoadedModule(className);
                
                // 如果该类位于模块中
                if(loadedModule != null) {
                    // 获取待加载类关联的类加载器
                    BuiltinClassLoader loader = loadedModule.loader();
                    
                    if(loader == this) {
                        // 确保模块系统是否已加载
                        if(VM.isModuleSystemInited()) {
                            // 查找类，从loadedModule中获取className类的位置信息，加载className类的字节码，待虚拟机定义类之后，将其返回
                            c = findClassInModuleOrNull(loadedModule, className);
                        }
                    } else {
                        /*
                         * 如果当前类加载器不是待加载类关联的类加载器
                         * 那么需要切换到待加载类关联的类加载器的上下文中去加载类
                         */
                        c = loader.loadClassOrNull(className);
                    }
                    
                    // 如果该类不在模块中
                } else {
                    // 查看父级类加载器[影子]是否有能力加载该类
                    if(parent != null) {
                        c = parent.loadClassOrNull(className);
                    }
                    
                    /*
                     * 如果父级加载器无法加载该类，那么检查当前类加载器是否关联了类路径
                     * 如果有关联了类路径，那么就尝试自己去加载这个类
                     */
                    if(c == null && hasClassPath() && VM.isModuleSystemInited()) {
                        // 查找类，从类路径下加载字节码，进而通知虚拟机定义className类，并将定义后的类返回
                        c = findClassOnClassPathOrNull(className);
                    }
                }
                
            }
            
            if(resolve && c != null) {
                resolveClass(c);
            }
            
            return c;
        }
    }
    
    /*▲ 加载类 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找(定义)类 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Finds the class with the specified binary name.
     */
    // 查找(定义)类，如果该类在模块中，则在模块中查找该类，否则在类路径下查找（如果待查找的类存在，则会加载器字节码，并交给虚拟机去定义）
    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        // no class loading until VM is fully initialized
        if(!VM.isModuleSystemInited()) {
            // 确保模块系统已经初始化，否则抛异常
            throw new ClassNotFoundException(className);
        }
        
        // 返回指定类关联的定位信息，如果该类位于未命名的包中，则返回null
        LoadedModule loadedModule = findLoadedModule(className);
        
        Class<?> c = null;
        
        if(loadedModule != null) {
            // attempt to load class in module defined to this loader
            if(loadedModule.loader() == this) {
                // 查找类，从loadedModule中获取className类的位置信息，加载className类的字节码，待虚拟机定义类之后，将其返回
                c = findClassInModuleOrNull(loadedModule, className);
            }
        } else {
            // search class path
            if(hasClassPath()) {
                // 查找类，从类路径下加载字节码，进而通知虚拟机定义className类，并将定义后的类返回
                c = findClassOnClassPathOrNull(className);
            }
        }
        
        // not found
        if(c == null) {
            throw new ClassNotFoundException(className);
        }
        
        return c;
    }
    
    /**
     * Finds the class with the specified binary name in a module.
     * This method returns {@code null} if the class cannot be found
     * or not defined in the specified module.
     */
    // 查找(定义)类，如果moduleName不为空，则在指定模块中查找该类，否则在类路径下查找（如果待查找的类存在，则会加载器字节码，并交给虚拟机去定义）
    @Override
    protected Class<?> findClass(String moduleName, String className) {
        if(moduleName != null) {
            // 返回指定类关联的定位信息，如果该类位于未命名的包中，或者其所在模块不是指定的模块，则返回null
            LoadedModule loadedModule = findLoadedModule(moduleName, className);
            if(loadedModule == null) {
                return null;
            }
            
            // attempt to load class in module defined to this loader
            assert loadedModule.loader() == this;
    
            // 查找类，从loadedModule中获取className类的位置信息，加载className类的字节码，待虚拟机定义类之后，将其返回
            return findClassInModuleOrNull(loadedModule, className);
        }
        
        // search class path
        if(hasClassPath()) {
            // 查找类，从类路径下加载字节码，进而通知虚拟机定义className类，并将定义后的类返回
            return findClassOnClassPathOrNull(className);
        }
        
        return null;
    }
    
    /**
     * Finds the class with the specified binary name if in a module
     * defined to this ClassLoader.
     *
     * @return the resulting Class or {@code null} if not found
     */
    // 查找(定义)类，从loadedModule中获取className类的位置信息，加载className类的字节码，待虚拟机定义类之后，将其返回
    private Class<?> findClassInModuleOrNull(LoadedModule loadedModule, String className) {
        if(System.getSecurityManager() == null) {
            return defineClass(className, loadedModule);
        } else {
            PrivilegedAction<Class<?>> pa = () -> defineClass(className, loadedModule);
            return AccessController.doPrivileged(pa);
        }
    }
    
    /**
     * Finds the class with the specified binary name on the class path.
     *
     * @return the resulting Class or {@code null} if not found
     */
    // 查找(定义)类，从类路径下加载字节码，进而通知虚拟机定义className类，并将定义后的类返回
    private Class<?> findClassOnClassPathOrNull(String className) {
        // 将类名的全限定名转换为路径形式
        String path = className.replace('.', '/').concat(".class");
        
        if(System.getSecurityManager() == null) {
            // 获取封装了待加载类信息的Resource对象
            Resource res = ucp.getResource(path, false);
            
            if(res != null) {
                try {
                    // 从res中加载字节码，进而通知虚拟机定义className类
                    return defineClass(className, res);
                } catch(IOException ioe) {
                    // TBD on how I/O errors should be propagated
                }
            }
            return null;
        } else {
            // avoid use of lambda here
            PrivilegedAction<Class<?>> pa = new PrivilegedAction<>() {
                public Class<?> run() {
                    // 获取封装了待加载类信息的Resource对象
                    Resource res = ucp.getResource(path, false);
                    if(res != null) {
                        try {
                            // 从res中加载字节码，进而通知虚拟机定义className类
                            return defineClass(className, res);
                        } catch(IOException ioe) {
                            // TBD on how I/O errors should be propagated
                        }
                    }
                    return null;
                }
            };
            
            return AccessController.doPrivileged(pa);
        }
    }
    
    /*▲ 查找(定义)类 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 定义类 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Defines the given binary class name to the VM, loading the class bytes from the given module.
     *
     * @return the resulting Class or {@code null} if an I/O error occurs
     */
    // 从loadedModule中获取className类的位置信息，加载className类的字节码，以便虚拟机定义类
    private Class<?> defineClass(String className, LoadedModule loadedModule) {
        // 从LoadedModule获取ModuleReference
        ModuleReference mref = loadedModule.mref();
        
        // 从指定的module获取ModuleReader用于读取module
        ModuleReader reader = moduleReaderFor(mref);
        
        try {
            ByteBuffer bb = null;
            URL csURL = null;
            
            /* locate class file, special handling for patched modules to avoid locating the resource twice */
            
            // 将类名的全限定名转换为路径形式
            String path = className.replace('.', '/').concat(".class");
    
            // 如果需要从补丁模块（--patch-module）读取
            if(reader instanceof PatchedModuleReader) {
                Resource r = ((PatchedModuleReader) reader).findResource(path);
                if(r != null) {
                    bb = r.getByteBuffer();
                    csURL = r.getCodeSourceURL();
                }
            } else {
                // 将class文件读取到缓冲区
                bb = reader.read(path).orElse(null);
                // 获取代码源信息
                csURL = loadedModule.codeSourceURL();
            }
            
            // 没有找到相应的类
            if(bb == null) {
                // class not found
                return null;
            }
            
            CodeSource cs = new CodeSource(csURL, (CodeSigner[]) null);
            try {
                // 利用存储在缓冲区bb中的字节码去定义类
                return defineClass(className, bb, cs);
                
            } finally {
                // 释放缓冲区（依实现而定）
                reader.release(bb);
            }
        } catch(IOException ioe) {
            // TBD on how I/O errors should be propagated
            return null;
        }
    }
    
    /**
     * Defines the given binary class name to the VM,
     * loading the class bytes via the given Resource object.
     *
     * @return the resulting Class
     *
     * @throws IOException       if reading the resource fails
     * @throws SecurityException if there is a sealing violation (JAR spec)
     */
    // 从res中加载字节码，进而通知虚拟机定义className类
    private Class<?> defineClass(String className, Resource res) throws IOException {
        // 获取代码源的URL
        URL url = res.getCodeSourceURL();
        
        // if class is in a named package then ensure that the package is defined
        int pos = className.lastIndexOf('.');
        if(pos != -1) {
            // 获取包名
            String packageName = className.substring(0, pos);
            // 获取MANIFEST.MF信息（一般存在于JAR包中）
            Manifest man = res.getManifest();
            // 尝试定义Package对象，如果该对象已经存在，则对其进行验证
            defineOrCheckPackage(packageName, man, url);
        }
        
        // 定义类
        ByteBuffer bb = res.getByteBuffer(); // 尝试将class文件加载到字节缓冲区
        if(bb != null) {
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            // 利用存储在缓冲区bb中的字节码去定义类
            return defineClass(className, bb, cs);
        } else {
            byte[] b = res.getBytes();  // 将class文件存储到字节流
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            // 使用JVM定义类（define class），该class文件的二进制流位于字节数组b中
            return defineClass(className, b, 0, b.length, cs);
        }
    }
    
    /*▲ 定义类 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Defines a package in this ClassLoader. If the package is already defined
     * then its sealing needs to be checked if sealed by the legacy sealing
     * mechanism.
     *
     * @throws SecurityException if there is a sealing violation (JAR spec)
     */
    // 尝试定义Package对象，如果该对象已经存在，则对其进行验证
    protected Package defineOrCheckPackage(String packageName, Manifest man, URL url) {
        // 获取指定包名的Package对象，如果该对象非空，则需要进一步验证它
        Package pkg = getAndVerifyPackage(packageName, man, url);
        
        // 定义一个新的Package对象，并与包名一起加入缓存
        if(pkg == null) {
            try {
                if(man != null) {
                    pkg = definePackage(packageName, man, url);
                } else {
                    pkg = definePackage(packageName, null, null, null, null, null, null, null);
                }
            } catch(IllegalArgumentException iae) {
                // defined by another thread so need to re-verify
                pkg = getAndVerifyPackage(packageName, man, url);
                if(pkg == null) {
                    throw new InternalError("Cannot find package: " + packageName);
                }
            }
        }
        
        return pkg;
    }
    
    /**
     * Gets the Package with the specified package name. If defined
     * then verifies it against the manifest and code source.
     *
     * @throws SecurityException if there is a sealing violation (JAR spec)
     */
    // 获取指定包名的Package对象，如果该对象非空，则需要进一步验证它
    private Package getAndVerifyPackage(String packageName, Manifest man, URL url) {
        // 获取指定包名的Package对象，如果该包还未被加载过，则返回null
        Package pkg = getDefinedPackage(packageName);
        
        if(pkg != null) {
            if(pkg.isSealed()) {
                if(!pkg.isSealed(url)) {
                    throw new SecurityException("sealing violation: package " + packageName + " is sealed");
                }
            } else {
                // can't seal package if already defined without sealing
                if((man != null) && isSealed(packageName, man)) {
                    throw new SecurityException("sealing violation: can't seal package " + packageName + ": already defined");
                }
            }
        }
        
        return pkg;
    }
    
    /**
     * Defines a new package in this ClassLoader. The attributes in the specified
     * Manifest are used to get the package version and sealing information.
     *
     * @throws IllegalArgumentException if the package name duplicates an
     *                                  existing package either in this class loader or one of its ancestors
     */
    // 定义一个新的包名，并加入缓存
    private Package definePackage(String packageName, Manifest man, URL url) {
        String specTitle = null;
        String specVersion = null;
        String specVendor = null;
        String implTitle = null;
        String implVersion = null;
        String implVendor = null;
        String sealed = null;
        URL sealBase = null;
        
        // 处理MANIFEST.MF中的信息
        if(man != null) {
            Attributes attr = man.getAttributes(packageName.replace('.', '/').concat("/"));
            if(attr != null) {
                specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
                specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
                specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                sealed = attr.getValue(Attributes.Name.SEALED);
            }
            
            attr = man.getMainAttributes();
            if(attr != null) {
                if(specTitle == null)
                    specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
                if(specVersion == null)
                    specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
                if(specVendor == null)
                    specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                if(implTitle == null)
                    implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                if(implVersion == null)
                    implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                if(implVendor == null)
                    implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                if(sealed == null)
                    sealed = attr.getValue(Attributes.Name.SEALED);
            }
            
            // package is sealed
            if("true".equalsIgnoreCase(sealed)) {
                sealBase = url;
            }
        }
        
        return definePackage(packageName, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }
    
    /**
     * Returns {@code true} if the specified package name is sealed according to
     * the given manifest.
     */
    // 判断该包是否是密封的
    private boolean isSealed(String packageName, Manifest man) {
        String path = packageName.replace('.', '/').concat("/");
        Attributes attr = man.getAttributes(path);
        String sealed = null;
        if(attr != null)
            sealed = attr.getValue(Attributes.Name.SEALED);
        if(sealed == null && (attr = man.getMainAttributes()) != null)
            sealed = attr.getValue(Attributes.Name.SEALED);
        return "true".equalsIgnoreCase(sealed);
    }
    
    /*▲ 包 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 模块 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Register a module this class loader. This has the effect of making the types in the module visible.
     */
    // 将加载的模块信息注册到当前的类加载器中
    public void loadModule(ModuleReference mref) {
        String moduleName = mref.descriptor().name();
        
        if(nameToModule.putIfAbsent(moduleName, mref) != null) {
            throw new InternalError(moduleName + " already defined to this loader");
        }
        
        LoadedModule loadedModule = new LoadedModule(this, mref);
        for(String packageName : mref.descriptor().packages()) {
            LoadedModule other = packageToModule.putIfAbsent(packageName, loadedModule);
            if(other != null) {
                throw new InternalError(packageName + " in modules " + moduleName + " and " + other.mref().descriptor().name());
            }
        }
        
        // clear resources cache if VM is already initialized
        if(VM.isModuleSystemInited() && resourceCache != null) {
            resourceCache = null;
        }
    }
    
    /**
     * Returns the {@code ModuleReference} for the named module defined to
     * this class loader; or {@code null} if not defined.
     *
     * @param moduleName The name of the module to find
     */
    // 返回指定模块关联的模块引用
    protected ModuleReference findModule(String moduleName) {
        return nameToModule.get(moduleName);
    }
    
    /**
     * Finds the candidate loaded module for the given class name.
     * Returns {@code null} if none of the modules defined to this class loader contain the API package for the class.
     */
    // 返回指定类关联的定位信息，如果该类位于未命名的包中，则返回null
    private LoadedModule findLoadedModule(String className) {
        /*
         * 判断是否有包名
         *
         * 如果没有包名，说明该类处于未命名的包，
         * 那么它也不能位于有名字的module（命名module或自动module）
         * 此时直接返回null
         */
        int pos = className.lastIndexOf('.');
        if(pos<0) {
            return null; // unnamed package
        }
        
        // 获取包名
        String packageName = className.substring(0, pos);
        
        return packageToModule.get(packageName);
    }
    
    /**
     * Finds the candidate loaded module for the given class name
     * in the named module.  Returns {@code null} if the named module
     * is not defined to this class loader or does not contain
     * the API package for the class.
     */
    // 返回指定类关联的定位信息，如果该类位于未命名的包中，或者其所在模块不是指定的模块，则返回null
    private LoadedModule findLoadedModule(String moduleName, String className) {
        // 返回指定类关联的定位信息，如果该类位于未命名的包中，则返回null
        LoadedModule loadedModule = findLoadedModule(className);
        
        // 如果不是位于指定的模块，也将返回null
        if(loadedModule != null && moduleName.equals(loadedModule.name())) {
            return loadedModule;
        } else {
            return null;
        }
    }
    
    /**
     * Returns the ModuleReader for the given module, creating it if needed.
     */
    // 获取模块阅读器，以便读取指定的模块
    private ModuleReader moduleReaderFor(ModuleReference mref) {
        ModuleReader reader = moduleToReader.get(mref);
        if(reader != null) {
            return reader;
        }
    
        // avoid method reference during startup
        Function<ModuleReference, ModuleReader> create = new Function<>() {
            public ModuleReader apply(ModuleReference moduleReference) {
                try {
                    return mref.open();
                } catch(IOException e) {
                    // Return a null module reader to avoid a future class load attempting to open the module again.
                    return new NullModuleReader();
                }
            }
        };
    
        reader = moduleToReader.computeIfAbsent(mref, create);
    
        return reader;
    }
    
    /**
     * Returns true if the given module opens the given package unconditionally.
     *
     * @implNote This method currently iterates over each of the open packages.
     * This will be replaced once the ModuleDescriptor.Opens API is updated.
     */
    // 判断给定的模块mref是否将指定的包packageName开放(opens)给了所有其它模块（从模块中读取资源时很重要）
    private boolean isOpen(ModuleReference mref, String packageName) {
        ModuleDescriptor descriptor = mref.descriptor();
        
        // 如果mref是open模块，或者是自动模块，可以直接返回true
        if(descriptor.isOpen() || descriptor.isAutomatic()) {
            return true;
        }
        
        // 遍历mref开放(opens)的元素
        for(ModuleDescriptor.Opens opens : descriptor.opens()) {
            // 开放的包名
            String source = opens.source();
            
            // 如果遇到了opens packageName（而不是opens...to...）
            if(!opens.isQualified() && source.equals(packageName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /*▲ 模块 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the permissions for the given CodeSource.
     */
    @Override
    protected PermissionCollection getPermissions(CodeSource cs) {
        PermissionCollection perms = super.getPermissions(cs);
        
        // add the permission to access the resource
        URL url = cs.getLocation();
        if(url == null)
            return perms;
        
        // avoid opening connection when URL is to resource in run-time image
        if(url.getProtocol().equals("jrt")) {
            perms.add(new RuntimePermission("accessSystemModules"));
            return perms;
        }
        
        // open connection to determine the permission needed
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
     * Returns {@code true} if there is a class path associated with this class loader.
     */
    // 当前类加载器是否关联了类路径
    boolean hasClassPath() {
        return ucp != null;
    }
    
    /**
     * Checks access to the given URL.
     * We use URLClassPath for consistent checking with java.net.URLClassLoader.
     */
    // 检查url的可访问性
    private static URL checkURL(URL url) {
        return URLClassPath.checkURL(url);
    }
    
    
    
    /**
     * A module defined/loaded by a built-in class loader.
     *
     * A LoadedModule encapsulates a ModuleReference along with its CodeSource
     * URL to avoid needing to create this URL when defining classes.
     */
    // 由内置类加载器加载的模块信息
    private static class LoadedModule {
        private final BuiltinClassLoader loader;    // 加载该模块使用的类加载器
        private final ModuleReference mref;         // 模块引用，包含模块描述符和模块的位置信息
        private final URL codeSourceURL;            // 代码源信息，用来声明从哪里加载类，属于安全策略的一部分，该对象可能为null
        
        LoadedModule(BuiltinClassLoader loader, ModuleReference mref) {
            URL url = null;
            if(mref.location().isPresent()) {
                try {
                    url = mref.location().get().toURL();
                } catch(MalformedURLException | IllegalArgumentException e) {
                }
            }
            this.loader = loader;
            this.mref = mref;
            this.codeSourceURL = url;
        }
        
        BuiltinClassLoader loader() {
            return loader;
        }
        
        ModuleReference mref() {
            return mref;
        }
        
        String name() {
            return mref.descriptor().name();
        }
        
        URL codeSourceURL() {
            return codeSourceURL;
        }
    }
    
    /**
     * A ModuleReader that doesn't read any resources.
     */
    private static class NullModuleReader implements ModuleReader {
        @Override
        public Optional<URI> find(String resName) {
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
    
}
