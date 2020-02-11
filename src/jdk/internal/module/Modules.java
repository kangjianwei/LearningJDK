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

package jdk.internal.module;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jdk.internal.loader.BootLoader;
import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.loader.ClassLoaders;
import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;

/**
 * A helper class for creating and updating modules. This class is intended to
 * support command-line options, tests, and the instrumentation API. It is also
 * used by the VM to load modules or add read edges when agents are instrumenting
 * code that need to link to supporting classes.
 *
 * The parameters that are package names in this API are the fully-qualified
 * names of the packages as defined in section 6.5.3 of <cite>The Java&trade;
 * Language Specification </cite>, for example, {@code "java.lang"}.
 */
// 用于模块定义过程的工具类
public class Modules {
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
    
    // 最顶端的模块层，最下面一层是Boot Layer，且Boot Layer的父模块层是一个特殊的空层
    private static volatile ModuleLayer topLayer;
    
    private Modules() {
    }
    
    /**
     * Creates a new Module. The module has the given ModuleDescriptor and
     * is defined to the given class loader.
     *
     * The resulting Module is in a larval state in that it does not read
     * any other module and does not have any exports.
     *
     * The URI is for information purposes only.
     */
    // 构造一个命名模块，会通知虚拟机
    public static Module defineModule(ClassLoader loader, ModuleDescriptor descriptor, URI uri) {
        return JLA.defineModule(loader, descriptor, uri);
    }
    
    /**
     * Updates m1 to read m2.
     * Same as m1.addReads(m2) but without a caller check.
     */
    // 使模块module依赖(requires)other模块，需要通知VM
    public static void addReads(Module module, Module other) {
        JLA.addReads(module, other);
    }
    
    /**
     * Update module m to read all unnamed modules.
     */
    // 使模块module依赖(requires)所有未命名模块，需要通知VM
    public static void addReadsAllUnnamed(Module module) {
        JLA.addReadsAllUnnamed(module);
    }
    
    /**
     * Updates module m1 to export a package to module m2.
     * Same as m1.addExports(pn, m2) but without a caller check
     */
    // 将模块module的pn包导出(exports)给other模块，需要通知VM
    public static void addExports(Module module, String pn, Module other) {
        JLA.addExports(module, pn, other);
    }
    
    /**
     * Updates module m to export a package to all unnamed modules.
     */
    // 将模块module的pn包导出(exports)给所有未命名模块，需要通知VM
    public static void addExportsToAllUnnamed(Module module, String pn) {
        JLA.addExportsToAllUnnamed(module, pn);
    }
    
    /**
     * Updates module m1 to open a package to module m2.
     * Same as m1.addOpens(pn, m2) but without a caller check.
     */
    // 将模块module的pn包(开放)opens给other模块，需要通知VM
    public static void addOpens(Module module, String pn, Module other) {
        JLA.addOpens(module, pn, other);
    }
    
    /**
     * Updates module m to open a package to all unnamed modules.
     */
    // 将模块module的pn包(开放)opens给所有未命名模块，需要通知VM
    public static void addOpensToAllUnnamed(Module module, String pn) {
        JLA.addOpensToAllUnnamed(module, pn);
    }
    
    /**
     * Updates module m to use a service.
     * Same as m2.addUses(service) but without a caller check.
     */
    // 声明模块module将使用(uses)指定的服务
    public static void addUses(Module module, Class<?> service) {
        JLA.addUses(module, service);
    }
    
    /**
     * Updates module m to provide a service
     */
    // 分别向模块所在模块层中的服务目录和加载模块的类加载器中的服务目录中添加一条服务信息
    public static void addProvides(Module module, Class<?> service, Class<?> providerImpl) {
        // 获取模块module所在的模块层
        ModuleLayer layer = module.getLayer();
        
        // 获取加载模块module的类加载器
        PrivilegedAction<ClassLoader> pa = module::getClassLoader;
        ClassLoader loader = AccessController.doPrivileged(pa);
        
        ClassLoader platformClassLoader = ClassLoaders.platformClassLoader();
        
        // 更新类加载器内缓存的服务目录
        if(layer == null || loader == null || loader == platformClassLoader) {
            // update ClassLoader catalog
            ServicesCatalog catalog;
            
            // 获取loder可以加载到的服务目录，如果不存在则返回一个空的服务目录
            if(loader == null) {
                catalog = BootLoader.getServicesCatalog();
            } else {
                catalog = ServicesCatalog.getServicesCatalog(loader);
            }
            
            // 向service的服务目录中插入一个服务提供者
            catalog.addProvider(module, service, providerImpl);
        }
        
        // update Layer catalog
        if(layer != null) {
            // 返回当前模块层的服务目录（包含所有模块内的所有提供的服务）
            ServicesCatalog servicesCatalog = JLA.getServicesCatalog(layer);
            
            // 向service的服务目录中插入一个服务提供者
            servicesCatalog.addProvider(module, service, providerImpl);
        }
    }
    
    /**
     * Called by the VM when code in the given Module has been transformed by an agent
     * and so may have been instrumented to call into supporting classes on the boot class path or application class path.
     */
    // 由虚拟机调用，使指定的模块依赖BootClassLoader和AppClassLoader中的未命名模块
    public static void transformedByAgent(Module module) {
        // 使module模块依赖(requires)BootClassLoader的未命名模块，需要通知VM
        addReads(module, BootLoader.getUnnamedModule());
        // 使module模块依赖(requires)AppClassLoader的未命名模块，需要通知VM
        addReads(module, ClassLoaders.appClassLoader().getUnnamedModule());
    }
    
    /**
     * Called by the VM to load a system module, typically "java.instrument" or "jdk.management.agent".
     * If the module is not loaded then it is resolved and loaded (along with any dependences that weren't previously loaded) into a child layer.
     */
    // 加载指定的模块，由虚拟机调用；如果该模块是个新模块，则需要创建的模块层
    public static synchronized Module loadModule(String name) {
        ModuleLayer top = topLayer;
        if(top == null) {
            top = ModuleLayer.boot();
        }
    
        // 在top模块层以及父模块层中查找指定名称的模块，如果已找到，则直接返回
        Module module = top.findModule(name).orElse(null);
        if(module != null) {
            // module already loaded
            return module;
        }
    
        // 获取topLayer中的模块依赖图，如果topLayer为null，则取Boot Layer
        Configuration parentGraph = top.configuration();
    
        // resolve the module with the top-most layer as the parent
        ModuleFinder before = ModuleFinder.of();    // 空集
    
        /*
         * 获取应用"--limit-modules"前的模块查找器；
         * 如果为null，回退为系统模块查找器。
         */
        ModuleFinder after = ModuleBootstrap.unlimitedFinder();
    
        // 模块名
        Set<String> roots = Set.of(name);
    
        // 基于给定的待解析模块roots来构造模块依赖图(需要解析服务依赖)，父模块依赖图为parentGraph
        Configuration cf = parentGraph.resolveAndBind(before, after, roots);
    
        // 创建子模块层
        Function<String, ClassLoader> clf = ModuleLoaderMap.mappingFunction(cf);
        ModuleLayer newLayer = top.defineModules(cf, clf);
    
        // add qualified exports/opens to give access to modules in child layer
        Map<String, Module> map = newLayer.modules().stream().collect(Collectors.toMap(Module::getName, Function.identity()));
        ModuleLayer layer = top;
        while(layer != null) {
            for(Module m : layer.modules()) {
                // qualified exports
                m.getDescriptor().exports().stream().filter(ModuleDescriptor.Exports::isQualified).forEach(e -> e.targets().forEach(target -> {
                    Module other = map.get(target);
                    if(other != null) {
                        addExports(m, e.source(), other);
                    }
                }));
            
                // qualified opens
                m.getDescriptor().opens().stream().filter(ModuleDescriptor.Opens::isQualified).forEach(o -> o.targets().forEach(target -> {
                    Module other = map.get(target);
                    if(other != null) {
                        addOpens(m, o.source(), other);
                    }
                }));
            }
        
            List<ModuleLayer> parents = layer.parents();
            assert parents.size()<=1;
            layer = parents.isEmpty() ? null : parents.get(0);
        }
    
        // update security manager before making types visible
        JLA.addNonExportedPackages(newLayer);
    
        // update the built-in class loaders to make the types visible
        for(ResolvedModule resolvedModule : cf.modules()) {
            ModuleReference mref = resolvedModule.reference();
            String mn = mref.descriptor().name();
            ClassLoader cl = clf.apply(mn);
            if(cl == null) {
                BootLoader.loadModule(mref);
            } else {
                ((BuiltinClassLoader) cl).loadModule(mref);
            }
        }
    
        // new top layer
        topLayer = newLayer;
    
        // return module
        return newLayer.findModule(name).orElseThrow(() -> new InternalError("module not loaded"));
    }
    
    /**
     * Finds the module with the given name in the boot layer or any child
     * layers created to load the "java.instrument" or "jdk.management.agent"
     * modules into a running VM.
     */
    // 在所有模块层上查找指定的模块
    public static Optional<Module> findLoadedModule(String name) {
        ModuleLayer top = topLayer;
        if(top == null) {
            top = ModuleLayer.boot();
        }
    
        return top.findModule(name);
    }
    
}
