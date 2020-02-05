/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.PrintStream;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jdk.internal.loader.BootLoader;
import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.JavaLangModuleAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.perf.PerfCounter;

/**
 * Initializes/boots the module system.
 *
 * The {@link #boot() boot} method is called early in the startup to initialize the module system.
 * In summary, the boot method creates a Configuration by resolving a set of module names specified via the launcher (or equivalent) -m and --add-modules options.
 * The modules are located on a module path that is constructed from the upgrade module path, system modules, and application module path.
 * The Configuration is instantiated as the boot layer with each module in the configuration defined to a class loader.
 */
// 模块系统启动器
public final class ModuleBootstrap {
    
    // the token for "all default modules"
    private static final String ALL_DEFAULT = "ALL-DEFAULT";
    
    // the token for "all system modules"
    private static final String ALL_SYSTEM = "ALL-SYSTEM";
    
    // the token for "all modules on the module path"
    private static final String ALL_MODULE_PATH = "ALL-MODULE-PATH";
    
    
    private static final String JAVA_BASE = "java.base";    // 基础模块
    
    
    // the token for "all unnamed modules"
    private static final String ALL_UNNAMED = "ALL-UNNAMED";
    
    private static final String ADD_MODULES = "--add-modules";
    private static final String ADD_EXPORTS = "--add-exports";
    private static final String ADD_OPENS = "--add-opens";
    private static final String ADD_READS = "--add-reads";
    private static final String PATCH_MODULE = "--patch-module";
    
    // access to java.lang/module
    private static final JavaLangModuleAccess JLMA = SharedSecrets.getJavaLangModuleAccess();
    
    /** The ModulePatcher for the initial configuration */
    /*
     * --patch-module信息，"--patch-module"名称在系统内部被转换为"jdk.module.patch.{序号}"
     *
     * --patch-module用来将目录或jar包中的class文件添加/覆盖到指定module，通常在测试环节使用。
     */
    private static final ModulePatcher patcher = initModulePatcher();
    
    // ModuleFinders for the initial configuration
    private static volatile ModuleFinder unlimitedFinder;   // 应用"--limit-modules"前的模块查找器
    private static volatile ModuleFinder limitedFinder;     // 应用"--limit-modules"后的模块查找器
    
    private ModuleBootstrap() {
    }
    
    /**
     * Initialize the module system, returning the boot layer.
     *
     * @see java.lang.System#initPhase2(boolean, boolean)
     */
    /*
     * 初始化模块系统，返回模块层
     *
     * 注：关于命令行参数名称与系统内部使用的参数名称之间的转换，参见openjdk\src\hotspot\share\runtime\arguments.cpp文件
     */
    public static ModuleLayer boot() throws Exception {
        
        // Step 0: Command line options
        
        long t0 = System.nanoTime();
        
        /*
         * module-path可以分为三类
         *
         * compilation module path，由--module-source-path指定，表示模块源码，仅在javac中使用
         * application module path，由--module-path指定，表示已编译的应用模块
         * upgrade module path，由--upgrade-module-path指定，表示用来替换运行环境中可升级模块的已编译模块
         *
         * --module-path将不包含模块声明的jar识别为automatic module。
         */
        
        // 基于"--upgrade-module-path"的模块查找器("--upgrade-module-path"名称在系统内部被转换为"jdk.module.upgrade.path")
        ModuleFinder upgradeModulePath = finderFor("jdk.module.upgrade.path");
        
        // 基于"--module-path"的模块查找器("--module-path"名称在系统内部被转换为"jdk.module.path")
        ModuleFinder appModulePath = finderFor("jdk.module.path");
        
        // 是否包含--patch-module信息
        boolean isPatched = patcher.hasPatches();
        
        // 获取"jdk.module.main"信息，即主模块信息，由-m指定
        String mainModule = System.getProperty("jdk.module.main");
        
        /*
         * 获取"--add-module"信息。
         *
         * --add-module用来添加额外的模块到根模块集合中。
         */
        Set<String> addModules = addModules();
        
        /*
         * 获取"--limit-modules"信息。
         *
         * 用来限定编译及运行时可以使用的模块，限定的范围是main module，--add-modules添加的modules，
         * 以及该参数指定的modules及其transitive依赖的modules。
         *
         * 当包含main方法的module是unnamed modules的时候，经常用这个参数来减少需要被解析的模块数量。
         */
        Set<String> limitModules = limitModules();
        
        PrintStream traceOutput = null;
        
        /*
         * 获取key为"jdk.module.showModuleResolution"的系统属性的value，并将该条属性从系统属性集中移除。
         * 注：被移除的属性可以去VM#savedProps中查找
         *
         * "jdk.module.showModuleResolution"属性由"--show-module-resolution"映射而来
         */
        String trace = getAndRemoveProperty("jdk.module.showModuleResolution");
        if(Boolean.parseBoolean(trace)) {
            traceOutput = System.out;   // 到达此处时，System.out已有值
        }
        
        
        /*
         * Step 1:
         *
         * The observable system modules, either all system modules or the system modules pre-generated for the initial module
         * (the initial module may be the unnamed module).
         * If the system modules are pre-generated for the initial module then resolution can be skipped.
         */
        
        long t1 = System.nanoTime();
        
        // 系统模块，包含运行环境锁提供的已编译模块
        SystemModules systemModules = null;
        ModuleFinder systemModuleFinder;
        
        // 是否存在模块查找器("--module-path"、"--upgrade-module-path")
        boolean haveModulePath = (appModulePath != null || upgradeModulePath != null);
        
        // 是否需要后续处理，如果没有通过命令行显式引入其他模块，则不需要后续处理
        boolean needResolution = true;
        
        
        if(!haveModulePath                  // 不包含"--module-path"或"--upgrade-module-path"信息
            && addModules.isEmpty()         // 不包含"--add-module"信息
            && limitModules.isEmpty()) {    // 不包含"--limit-modules"信息
            
            // 获取初始模块关联的系统模块信息
            systemModules = SystemModuleFinders.systemModules(mainModule);
            
            // noinspection ConstantConditions
            if(systemModules != null        // 包含系统模块
                && !isPatched               // 不包含--patch-module信息
                && traceOutput == null) {   // 不需要追踪输出
                
                // 后续不再需要解析
                needResolution = false;
            }
        }
        
        // 获取所有系统模块信息
        if(systemModules == null) {
            // all system modules are observable
            systemModules = SystemModuleFinders.allSystemModules();
        }
        
        // 获取外部模块信息
        if(systemModules == null) {
            // exploded build or testing
            systemModules = new ExplodedSystemModules();
            systemModuleFinder = SystemModuleFinders.ofSystem();
            
            // 构造系统模块集的模块查找器
        } else {
            // images build
            systemModuleFinder = SystemModuleFinders.of(systemModules);
        }
        
        Counters.add("jdk.module.boot.1.systemModulesTime", t1);
        
        
        /*
         * Step 2:
         *
         * Define and load java.base.
         * This patches all classes loaded to date so that they are members of java.base.
         * Once java.base is loaded then resources in java.base are available for error messages needed from here on.
         */
        
        long t2 = System.nanoTime();
        
        // 获取"java.base"模块的引用
        ModuleReference base = systemModuleFinder.find(JAVA_BASE).orElse(null);
        if(base == null) {
            throw new InternalError(JAVA_BASE + " not found");
        }
        
        // 获取"java.base"模块位置
        URI baseUri = base.location().orElse(null);
        if(baseUri == null) {
            throw new InternalError(JAVA_BASE + " does not have a location");
        }
        
        // 将加载的"java.base"模块信息注册到BootClassLoader中
        BootLoader.loadModule(base);
        
        // 构造"java.base"模块(会通知虚拟机加载)
        Modules.defineModule(null, base.descriptor(), baseUri);
        
        Counters.add("jdk.module.boot.2.defineBaseTime", t2);
        
        
        /*
         * Step 2a:
         *
         * Scan all modules when --validate-modules specified
         */
        
        /*
         * 获取key为"jdk.module.validation"的系统属性的value，并将该条属性从系统属性集中移除。
         * 注：被移除的属性可以去VM#savedProps中查找
         *
         * "jdk.module.validation"属性由"--validate-modules"映射而来
         *
         * --validate-modules用来指示对所有模块进行验证
         */
        if(getAndRemoveProperty("jdk.module.validation") != null) {
            int errors = ModulePathValidator.scanAllModules(System.out);
            if(errors>0) {
                fail("Validation of module path failed");
            }
        }
        
        
        /*
         * Step 3:
         *
         * If resolution is needed then create the module finder and the set of root modules to resolve.
         */
        
        long t3 = System.nanoTime();
        
        ModuleFinder savedModuleFinder = null;
        ModuleFinder finder;
        
        /*
         * 模块系统解析的根模块，需要从此处解析模块依赖信息，最终形成完整的依赖图。
         *
         * 可以通过--add-modules将默认根模块之外的模块添加到模块依赖解析中。
         * JDK9默认的root module是java.se模块。
         *
         * 由于java.se.ee不在默认的root modules中，
         * 因此--add-modules的最常见的用途是用来添加ee中的模块，比如java.xml.bind
         */
        Set<String> roots;
        
        // 进行后续处理
        if(needResolution) {
            
            /*
             * upgraded modules override the modules in the run-time image
             *
             * 存在"--upgrade-module-path"模块
             */
            if(upgradeModulePath != null) {
                // 组合upgradeModulePath和systemModuleFinder两个模块查找器
                systemModuleFinder = ModuleFinder.compose(upgradeModulePath, systemModuleFinder);
            }
            
            /*
             * The module finder: [--upgrade-module-path] system [--module-path]
             *
             * 存在"--module-path"模块
             */
            if(appModulePath != null) {
                // 组合systemModuleFinder和appModulePath两个模块查找器
                finder = ModuleFinder.compose(systemModuleFinder, appModulePath);
                
                // 如果不存在"--module-path"模块，直接使用上面的systemModuleFinde
            } else {
                finder = systemModuleFinder;
            }
            
            // The root modules to resolve
            roots = new HashSet<>();
            
            // launcher -m option to specify the main/initial module
            if(mainModule != null) {
                // 将主模块信息加入到根模块集合
                roots.add(mainModule);
            }
            
            // additional module(s) specified by --add-modules
            boolean addAllDefaultModules = false;   // 是否添加未命名模块对应的根模块集合
            boolean addAllSystemModules = false;   // 是否添加所有的系统模块
            boolean addAllApplicationModules = false;   // 是否添加模块路径中搜索到的全部可见模块
            
            for(String mod : addModules) {
                switch(mod) {
                    case ALL_DEFAULT:
                        addAllDefaultModules = true;
                        break;
                    case ALL_SYSTEM:
                        addAllSystemModules = true;
                        break;
                    case ALL_MODULE_PATH:
                        addAllApplicationModules = true;
                        break;
                    default:
                        // 直接添加
                        roots.add(mod);
                }
            }
            
            // 记录应用"--limit-modules"前的模块查找器
            savedModuleFinder = finder;
            
            // 如果存在"--limit-modules"信息
            if(!limitModules.isEmpty()) {
                finder = limitFinder(finder, limitModules, roots);
            }
            
            /*
             * If there is no initial module specified then assume that the initial module is the unnamed module of the application class loader.
             * This is implemented by resolving all observable modules that export an API.
             * Modules that have the DO_NOT_RESOLVE_BY_DEFAULT bit set in their ModuleResolution attribute flags are excluded from the default set of roots
             */
            if(mainModule == null || addAllDefaultModules) {
                Set<String> finderSet = DefaultRoots.compute(systemModuleFinder, finder);
                roots.addAll(finderSet);
            }
            
            // If `--add-modules ALL-SYSTEM` is specified then all observable system modules will be resolved.
            if(addAllSystemModules) {
                ModuleFinder f = finder;  // observable modules
                systemModuleFinder.findAll().stream().map(ModuleReference::descriptor).map(ModuleDescriptor::name).filter(mn -> f.find(mn).isPresent())  // observable
                    .forEach(mn -> roots.add(mn));
            }
            
            // If `--add-modules ALL-MODULE-PATH` is specified then all observable modules on the application module path will be resolved.
            if(appModulePath != null && addAllApplicationModules) {
                ModuleFinder f = finder;  // observable modules
                appModulePath.findAll().stream().map(ModuleReference::descriptor).map(ModuleDescriptor::name).filter(mn -> f.find(mn).isPresent())  // observable
                    .forEach(mn -> roots.add(mn));
            }
        } else {
            // no resolution case
            finder = systemModuleFinder;
            roots = null;
        }
        
        Counters.add("jdk.module.boot.3.optionsAndRootsTime", t3);
        
        /*
         * Step 4:
         *
         * Resolve the root modules, with service binding, to create the configuration for the boot layer.
         * If resolution is not needed then create the configuration for the boot layer from the readability graph created at link time.
         */
        
        long t4 = System.nanoTime();
        
        // 构造Boot Layer层的模块图
        Configuration configuration;
        if(needResolution) {
            // 基于给定的待解析模块roots来构造模块依赖图(需要解析服务依赖)，父模块依赖图为空集
            configuration = JLMA.resolveAndBind(finder, roots, traceOutput);
        } else {
            // 获取系统模块依赖(requires)映射
            Map<String, Set<String>> map = systemModules.moduleReads();
            // 由指定的模块依赖集构造模块依赖图，父模块图为空集
            configuration = JLMA.newConfiguration(systemModuleFinder, map);
        }
        
        /* check that modules specified to --patch-module are resolved */
        // 打印patch module信息
        if(isPatched) {
            patcher.patchedModules().stream().filter(mn -> !configuration.findModule(mn).isPresent()).forEach(mn -> warnUnknownModule(PATCH_MODULE, mn));
        }
        
        Counters.add("jdk.module.boot.4.resolveTime", t4);
        
        /*
         * Step 5:
         *
         * Map the modules in the configuration to class loaders.
         * The static configuration provides the mapping of standard and JDK modules to the boot and platform loaders.
         * All other modules (JDK tool modules, and both explicit and automatic modules on the application module path)
         * are defined to the application class loader.
         */
        
        long t5 = System.nanoTime();
        
        /*
         * mapping of modules to class loaders
         *
         * 将模块图中的module映射到PlatformClassLoader和AppClassLoader（过滤了BootClassLoader）
         */
        Function<String, ClassLoader> mappingFunction = ModuleLoaderMap.mappingFunction(configuration);
        
        // check that all modules to be mapped to the boot loader will be loaded from the runtime image
        if(haveModulePath) {
            for(ResolvedModule resolvedModule : configuration.modules()) {
                ModuleReference mref = resolvedModule.reference();
                String name = mref.descriptor().name();
                ClassLoader cl = mappingFunction.apply(name);
                if(cl == null) {
                    if(upgradeModulePath != null && upgradeModulePath.find(name).isPresent()) {
                        fail(name + ": cannot be loaded from upgrade module path");
                    }
                    
                    if(!systemModuleFinder.find(name).isPresent()) {
                        fail(name + ": cannot be loaded from application module path");
                    }
                }
            }
        }
        
        // check for split packages in the modules mapped to the built-in loaders
        if(systemModules.hasSplitPackages() || isPatched || haveModulePath) {
            checkSplitPackages(configuration, mappingFunction);
        }
        
        // 将加载的模块信息注册到内置的类加载器中
        loadModules(configuration, mappingFunction);    // load/register the modules with the built-in class loaders
        
        Counters.add("jdk.module.boot.5.loadModulesTime", t5);
        
        
        /*
         * Step 6:
         *
         * Define all modules to the VM
         */
        
        long t6 = System.nanoTime();
        
        // 构造一个空的模块层
        ModuleLayer emptyLayer = ModuleLayer.empty();
        
        // 用空模块层作为Boot Layer的父模块层，并基于上面创建的模块图来构造Boot Layer
        ModuleLayer bootLayer = emptyLayer.defineModules(configuration, mappingFunction);
        
        Counters.add("jdk.module.boot.6.layerCreateTime", t6);
        
        
        /*
         * Step 7:
         *
         * Miscellaneous
         */
        
        // check incubating status
        if(systemModules.hasIncubatorModules() || haveModulePath) {
            checkIncubatingStatus(configuration);
        }
        
        // --add-reads, --add-exports/--add-opens, and --illegal-access
        long t7 = System.nanoTime();
        
        // 解析--add-reads选项
        addExtraReads(bootLayer);
        
        // 解析--add-exports和--add-opens选项
        boolean extraExportsOrOpens = addExtraExportsAndOpens(bootLayer);
        
        // 解析--illegal-access选项
        addIllegalAccess(upgradeModulePath, systemModules, bootLayer, extraExportsOrOpens);
        
        Counters.add("jdk.module.boot.7.adjustModulesTime", t7);
        
        // save module finders for later use
        if(savedModuleFinder != null) {
            unlimitedFinder = new SafeModuleFinder(savedModuleFinder);
            if(savedModuleFinder != finder) {
                limitedFinder = new SafeModuleFinder(finder);
            }
        }
        
        // total time to initialize
        Counters.add("jdk.module.boot.totalTime", t0);
        Counters.publish();
        
        return bootLayer;
    }
    
    /**
     * Initialize the module patcher for the initial configuration passed on the value of the --patch-module options.
     */
    // 解析--patch-module信息，"--patch-module"名称在系统内部被转换为"jdk.module.patch.{序号}"
    private static ModulePatcher initModulePatcher() {
        Map<String, List<String>> map = decode("jdk.module.patch.", File.pathSeparator, false);
        return new ModulePatcher(map);
    }
    
    /**
     * Creates a finder from the module path that is the value of the given system property and optionally patched by --patch-module
     */
    // 返回指定的属性对应的模块查找器
    private static ModuleFinder finderFor(String prop) {
        // 获取prop对应的系统属性的值。如果prop不存在，返回null
        String s = System.getProperty(prop);
        if(s == null) {
            return null;
        }
        
        String[] dirs = s.split(File.pathSeparator);
        Path[] paths = new Path[dirs.length];
        int i = 0;
        for(String dir : dirs) {
            paths[i++] = Path.of(dir);
        }
        
        return ModulePath.of(patcher, paths);
    }
    
    /**
     * Returns the set of module names specified by --add-module options.
     */
    // 获取"--add-module"信息
    private static Set<String> addModules() {
        String prefix = "jdk.module.addmods.";
        
        int index = 0;
        
        /*
         * 获取key为"jdk.module.addmods.{序号}"的系统属性的value，并将该条属性从系统属性集中移除。
         * 注：被移除的属性可以去VM#savedProps中查找
         *
         * "jdk.module.addmods.{序号}"属性由"--add-module"映射而来
         */
        String value = getAndRemoveProperty(prefix + index);
        if(value == null) {
            return Collections.emptySet();
        }
        
        Set<String> modules = new HashSet<>();
        while(value != null) {
            for(String s : value.split(",")) {
                if(s.length()>0) {
                    modules.add(s);
                }
            }
            index++;
            value = getAndRemoveProperty(prefix + index);
        }
        
        return modules;
    }
    
    /**
     * Returns the set of module names specified by --limit-modules.
     */
    // 获取"--limit-modules"信息
    private static Set<String> limitModules() {
        /*
         * 获取key为"jdk.module.limitmods"的系统属性的value，并将该条属性从系统属性集中移除。
         * 注：被移除的属性可以去VM#savedProps中查找
         *
         * "jdk.module.limitmods"属性由"--limit-modules"映射而来
         */
        String value = getAndRemoveProperty("jdk.module.limitmods");
        if(value == null) {
            return Collections.emptySet();
        }
        
        Set<String> names = new HashSet<>();
        for(String name : value.split(",")) {
            if(name.length()>0) {
                names.add(name);
            }
        }
        
        return names;
    }
    
    /**
     * Returns a ModuleFinder that limits observability to the given root modules,
     * their transitive dependences, plus a set of other modules.
     */
    // 返回一组由"--limit-modules"限定的模块查找器
    private static ModuleFinder limitFinder(ModuleFinder finder, Set<String> roots, Set<String> otherMods) {
        // resolve all root modules
        Configuration cf = Configuration.empty().resolve(finder, ModuleFinder.of(), roots);
        
        // module name -> reference
        Map<String, ModuleReference> map = new HashMap<>();
        
        // root modules and their transitive dependences
        cf.modules().stream().map(ResolvedModule::reference).forEach(mref -> map.put(mref.descriptor().name(), mref));
        
        // additional modules
        otherMods.stream().map(finder::find).flatMap(Optional::stream).forEach(mref -> map.putIfAbsent(mref.descriptor().name(), mref));
        
        // set of modules that are observable
        Set<ModuleReference> mrefs = new HashSet<>(map.values());
        
        return new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                return Optional.ofNullable(map.get(name));
            }
            
            @Override
            public Set<ModuleReference> findAll() {
                return mrefs;
            }
        };
    }
    
    /**
     * Checks for split packages between modules defined to the built-in class
     * loaders.
     */
    private static void checkSplitPackages(Configuration cf, Function<String, ClassLoader> clf) {
        Map<String, String> packageToModule = new HashMap<>();
        for(ResolvedModule resolvedModule : cf.modules()) {
            ModuleDescriptor descriptor = resolvedModule.reference().descriptor();
            String name = descriptor.name();
            ClassLoader loader = clf.apply(name);
            if(loader == null || loader instanceof BuiltinClassLoader) {
                for(String p : descriptor.packages()) {
                    String other = packageToModule.putIfAbsent(p, name);
                    if(other != null) {
                        String msg = "Package " + p + " in both module " + name + " and module " + other;
                        throw new LayerInstantiationException(msg);
                    }
                }
            }
        }
    }
    
    /**
     * Load/register the modules to the built-in class loaders.
     */
    // 将加载的模块信息注册到内置的类加载器中
    private static void loadModules(Configuration cf, Function<String, ClassLoader> clf) {
        for(ResolvedModule resolvedModule : cf.modules()) {
            ModuleReference mref = resolvedModule.reference();
            String name = resolvedModule.name();
            ClassLoader loader = clf.apply(name);
            
            if(loader == null) {
                // skip java.base as it is already loaded
                if(!name.equals(JAVA_BASE)) {
                    BootLoader.loadModule(mref);
                }
            } else if(loader instanceof BuiltinClassLoader) {
                ((BuiltinClassLoader) loader).loadModule(mref);
            }
        }
    }
    
    /**
     * Checks incubating status of modules in the configuration
     */
    private static void checkIncubatingStatus(Configuration cf) {
        String incubating = null;
        for(ResolvedModule resolvedModule : cf.modules()) {
            ModuleReference mref = resolvedModule.reference();
            
            // emit warning if the WARN_INCUBATING module resolution bit set
            if(ModuleResolution.hasIncubatingWarning(mref)) {
                String mn = mref.descriptor().name();
                if(incubating == null) {
                    incubating = mn;
                } else {
                    incubating += ", " + mn;
                }
            }
        }
        if(incubating != null) {
            warn("Using incubator modules: " + incubating);
        }
    }
    
    /**
     * Process the --add-reads options to add any additional read edges that are specified on the command-line.
     */
    // 解析--add-reads选项
    private static void addExtraReads(ModuleLayer bootLayer) {
        
        // 获取--add-opens信息，"--add-opens"名称在系统内部被转换为"jdk.module.addreads.{序号}"
        Map<String, List<String>> map = decode("jdk.module.addreads.");
        if(map.isEmpty()) {
            return;
        }
        
        for(Map.Entry<String, List<String>> e : map.entrySet()) {
            // the key is $MODULE
            String mn = e.getKey();
            
            // 在模块层bootLayer以及父模块层中查找指定名称的模块
            Optional<Module> module = bootLayer.findModule(mn);
            // 如果指定的模块不存在
            if(module.isEmpty()) {
                // 打印日志，提示无法识别当前模块
                warnUnknownModule(ADD_READS, mn);
                continue;
            }
            
            Module sourceModule = module.get();
            
            // the value is the set of other modules (by name)
            for(String name : e.getValue()) {
                if(ALL_UNNAMED.equals(name)) {
                    // 使模块sourceModule依赖(requires)所有未命名模块，需要通知VM
                    Modules.addReadsAllUnnamed(sourceModule);
                } else {
                    // 在模块层bootLayer以及父模块层中查找指定名称的模块
                    module = bootLayer.findModule(name);
                    
                    if(module.isPresent()) {
                        // 使sourceModule模块依赖(requires)module模块
                        Modules.addReads(sourceModule, module.get());
                    } else {
                        warnUnknownModule(ADD_READS, name);
                    }
                }
            }
        }
    }
    
    /**
     * Process the --add-exports and --add-opens options to export/open
     * additional packages specified on the command-line.
     */
    // 解析--add-exports和--add-opens选项
    private static boolean addExtraExportsAndOpens(ModuleLayer bootLayer) {
        boolean extraExportsOrOpens = false;
    
        // --add-exports，"--add-exports"名称在系统内部被转换为"jdk.module.addexports.{序号}"
        String prefix = "jdk.module.addexports.";
    
        // 获取--add-exports信息
        Map<String, List<String>> extraExports = decode(prefix);
        if(!extraExports.isEmpty()) {
            addExtraExportsOrOpens(bootLayer, extraExports, false);
            extraExportsOrOpens = true;
        }
    
    
        // --add-opens，"--add-opens"名称在系统内部被转换为"jdk.module.addopens.{序号}"
        prefix = "jdk.module.addopens.";
    
        // 获取--add-opens信息
        Map<String, List<String>> extraOpens = decode(prefix);
        if(!extraOpens.isEmpty()) {
            addExtraExportsOrOpens(bootLayer, extraOpens, true);
            extraExportsOrOpens = true;
        }
    
        return extraExportsOrOpens;
    }
    
    // 解析--add-exports和--add-opens选项
    private static void addExtraExportsOrOpens(ModuleLayer bootLayer, Map<String, List<String>> map, boolean opens) {
        String option = opens ? ADD_OPENS : ADD_EXPORTS;
        
        for(Map.Entry<String, List<String>> e : map.entrySet()) {
            
            // the key is $MODULE/$PACKAGE
            String key = e.getKey();
            String[] s = key.split("/");
            if(s.length != 2) {
                fail(unableToParse(option, "<module>/<package>", key));
            }
            
            String mn = s[0];
            String pn = s[1];
            if(mn.isEmpty() || pn.isEmpty()) {
                fail(unableToParse(option, "<module>/<package>", key));
            }
            
            // The exporting module is in the boot layer
            Module m;
            Optional<Module> om = bootLayer.findModule(mn);
            if(!om.isPresent()) {
                warnUnknownModule(option, mn);
                continue;
            }
            
            m = om.get();
            
            if(!m.getDescriptor().packages().contains(pn)) {
                warn("package " + pn + " not in " + mn);
                continue;
            }
            
            // the value is the set of modules to export to (by name)
            for(String name : e.getValue()) {
                boolean allUnnamed = false;
                Module other = null;
                if(ALL_UNNAMED.equals(name)) {
                    allUnnamed = true;
                } else {
                    om = bootLayer.findModule(name);
                    if(om.isPresent()) {
                        other = om.get();
                    } else {
                        warnUnknownModule(option, name);
                        continue;
                    }
                }
                if(allUnnamed) {
                    if(opens) {
                        Modules.addOpensToAllUnnamed(m, pn);
                    } else {
                        Modules.addExportsToAllUnnamed(m, pn);
                    }
                } else {
                    if(opens) {
                        Modules.addOpens(m, pn, other);
                    } else {
                        Modules.addExports(m, pn, other);
                    }
                }
                
            }
        }
    }
    
    /**
     * Decodes the values of --add-reads, -add-exports or --add-opens which use the "," to separate the RHS of the option value.
     */
    // 从系统属性中解析出--add-reads, -add-exports, --add-opens这类选项的映射信息
    private static Map<String, List<String>> decode(String prefix) {
        return decode(prefix, ",", true);
    }
    
    /**
     * Decodes the values of --add-reads, -add-exports, --add-opens or --patch-modules options that are encoded in system properties.
     *
     * @param prefix the system property prefix
     * @param regex  the regex for splitting the RHS of the option value
     */
    // 从系统属性中解析出--patch-modules, --add-reads, -add-exports, --add-opens这类选项的映射信息
    private static Map<String, List<String>> decode(String prefix, String regex, boolean allowDuplicates) {
        int index = 0;
        
        /* the system property is removed after decoding */
        /*
         * 获取key为prefix+index的系统属性的value，并将该条属性从系统属性集中移除。
         * 注：被移除的属性可以去VM#savedProps中查找
         */
        String value = getAndRemoveProperty(prefix + index);
        if(value == null) {
            return Collections.emptyMap();
        }
        
        Map<String, List<String>> map = new HashMap<>();
        
        while(value != null) {
            
            int pos = value.indexOf('=');
            
            // 不存在"="
            if(pos == -1) {
                fail(unableToParse(option(prefix), "<module>=<value>", value));
            }
            
            // 不存在<module>
            if(pos == 0) {
                fail(unableToParse(option(prefix), "<module>=<value>", value));
            }
            
            // key is <module> or <module>/<package>
            String key = value.substring(0, pos);   // "="之前的内容
            String rhs = value.substring(pos + 1);  // "="之后的内容，使用指定的分隔符来分割
            
            if(rhs.isEmpty()) {
                fail(unableToParse(option(prefix), "<module>=<value>", value));
            }
            
            // value is <module>(,<module>)* or <file>(<pathsep><file>)*
            if(!allowDuplicates && map.containsKey(key)) {
                fail(key + " specified more than once to " + option(prefix));
            }
            
            List<String> values = map.computeIfAbsent(key, k -> new ArrayList<>());
            int ntargets = 0;
            for(String s : rhs.split(regex)) {
                if(s.length()>0) {
                    values.add(s);
                    ntargets++;
                }
            }
            
            if(ntargets == 0) {
                fail("Target must be specified: " + option(prefix) + " " + value);
            }
            
            index++;
            
            value = getAndRemoveProperty(prefix + index);
        }
        
        return map;
    }
    
    /**
     * Returns the command-line option name corresponds to the specified system property prefix.
     */
    // 命令行参数与内部标识之间的转换
    static String option(String prefix) {
        switch(prefix) {
            case "jdk.module.addexports.":
                return ADD_EXPORTS;
            case "jdk.module.addopens.":
                return ADD_OPENS;
            case "jdk.module.addreads.":
                return ADD_READS;
            case "jdk.module.patch.":
                return PATCH_MODULE;
            case "jdk.module.addmods.":
                return ADD_MODULES;
            default:
                throw new IllegalArgumentException(prefix);
        }
    }
    
    /**
     * Process the --illegal-access option (and its default) to open packages
     * of system modules in the boot layer to code in unnamed modules.
     */
    // 解析--illegal-access选项(是否允许unnamed modules反射使用所有named modules中的类，为了方便迁移设置，将来可能会被移除)
    private static void addIllegalAccess(ModuleFinder upgradeModulePath, SystemModules systemModules, ModuleLayer bootLayer, boolean extraExportsOrOpens) {
        /*
         * 获取key为"jdk.module.illegalAccess"的系统属性的value，并将该条属性从系统属性集中移除。
         * 注：被移除的属性可以去VM#savedProps中查找
         *
         * "jdk.module.illegalAccess"属性由"--illegal-access"映射而来
         */
        String value = getAndRemoveProperty("jdk.module.illegalAccess");
        
        IllegalAccessLogger.Mode mode = IllegalAccessLogger.Mode.ONESHOT;
        
        if(value != null) {
            switch(value) {
                case "deny":
                    return;
                case "permit":
                    break;
                case "warn":
                    mode = IllegalAccessLogger.Mode.WARN;
                    break;
                case "debug":
                    mode = IllegalAccessLogger.Mode.DEBUG;
                    break;
                default:
                    fail("Value specified to --illegal-access not recognized:" + " '" + value + "'");
                    return;
            }
        }
        
        IllegalAccessLogger.Builder builder = new IllegalAccessLogger.Builder(mode, System.err);
        
        Map<String, Set<String>> map1 = systemModules.concealedPackagesToOpen();
        Map<String, Set<String>> map2 = systemModules.exportedPackagesToOpen();
        if(map1.isEmpty() && map2.isEmpty()) {
            // need to generate (exploded build)
            IllegalAccessMaps maps = IllegalAccessMaps.generate(limitedFinder());
            map1 = maps.concealedPackagesToOpen();
            map2 = maps.exportedPackagesToOpen();
        }
        
        // open specific packages in the system modules
        for(Module m : bootLayer.modules()) {
            ModuleDescriptor descriptor = m.getDescriptor();
            String name = m.getName();
            
            // skip open modules
            if(descriptor.isOpen()) {
                continue;
            }
            
            // skip modules loaded from the upgrade module path
            if(upgradeModulePath != null && upgradeModulePath.find(name).isPresent()) {
                continue;
            }
            
            Set<String> concealedPackages = map1.getOrDefault(name, Set.of());
            Set<String> exportedPackages = map2.getOrDefault(name, Set.of());
            
            // refresh the set of concealed and exported packages if needed
            if(extraExportsOrOpens) {
                concealedPackages = new HashSet<>(concealedPackages);
                exportedPackages = new HashSet<>(exportedPackages);
                Iterator<String> iterator = concealedPackages.iterator();
                while(iterator.hasNext()) {
                    String pn = iterator.next();
                    if(m.isExported(pn, BootLoader.getUnnamedModule())) {
                        // concealed package is exported to ALL-UNNAMED
                        iterator.remove();
                        exportedPackages.add(pn);
                    }
                }
                iterator = exportedPackages.iterator();
                while(iterator.hasNext()) {
                    String pn = iterator.next();
                    if(m.isOpen(pn, BootLoader.getUnnamedModule())) {
                        // exported package is opened to ALL-UNNAMED
                        iterator.remove();
                    }
                }
            }
            
            // log reflective access to all types in concealed packages
            builder.logAccessToConcealedPackages(m, concealedPackages);
            
            // log reflective access to non-public members/types in exported packages
            builder.logAccessToExportedPackages(m, exportedPackages);
            
            // open the packages to unnamed modules
            JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
            // 连接两个迭代器
            Iterator<String> iterator = concat(concealedPackages.iterator(), exportedPackages.iterator());
            jla.addOpensToAllUnnamed(m, iterator);
        }
        
        builder.complete();
    }
    
    /**
     * Returns an iterator that yields all elements of the first iterator
     * followed by all the elements of the second iterator.
     */
    // 连接两个迭代器
    static <T> Iterator<T> concat(Iterator<T> iterator1, Iterator<T> iterator2) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator1.hasNext() || iterator2.hasNext();
            }
            
            @Override
            public T next() {
                if(iterator1.hasNext()) {
                    return iterator1.next();
                }
                if(iterator2.hasNext()) {
                    return iterator2.next();
                }
                throw new NoSuchElementException();
            }
        };
    }
    
    /**
     * Gets and remove the named system property
     */
    /*
     * 获取拥有指定key的系统属性的value，并将该条属性从系统属性集中移除。
     * 注：被移除的属性可以去VM#savedProps中查找
     */
    private static String getAndRemoveProperty(String key) {
        return (String) System.getProperties().remove(key);
    }
    
    
    /**
     * Returns the ModulePatcher for the initial configuration.
     */
    // 获取基于--patch-module信息的ModulePatcher
    public static ModulePatcher patcher() {
        return patcher;
    }
    
    /**
     * Returns the ModuleFinder for the initial configuration before
     * observability is limited by the --limit-modules command line option.
     *
     * @apiNote Used to support locating modules {@code java.instrument} and
     * {@code jdk.management.agent} modules when they are loaded dynamically.
     */
    /*
     * 返回应用"--limit-modules"前的模块查找器；
     * 如果为null，回退为系统模块查找器。
     */
    public static ModuleFinder unlimitedFinder() {
        if(unlimitedFinder == null) {
            // 返回一个系统模块查找器
            return ModuleFinder.ofSystem();
        }
        
        return unlimitedFinder;
    }
    
    /**
     * Returns the ModuleFinder for the initial configuration.
     *
     * @apiNote Used to support "{@code java --list-modules}".
     */
    /*
     * 返回应用"--limit-modules"后的模块查找器；
     * 如果为null，回退为应用"--limit-modules"前的模块查找器；
     * 如果仍为null，回退为系统模块查找器。
     */
    public static ModuleFinder limitedFinder() {
        if(limitedFinder == null) {
            return unlimitedFinder();
        }
        
        return limitedFinder;
    }
    
    
    /**
     * Throws a RuntimeException with the given message
     */
    static void fail(String m) {
        throw new RuntimeException(m);
    }
    
    static void warn(String m) {
        System.err.println("WARNING: " + m);
    }
    
    static void warnUnknownModule(String option, String mn) {
        warn("Unknown module: " + mn + " specified to " + option);
    }
    
    static String unableToParse(String option, String text, String value) {
        return "Unable to parse " + option + " " + text + ": " + value;
    }
    
    
    /**
     * Wraps a (potentially not thread safe) ModuleFinder created during startup for use after startup.
     */
    // 包装在虚拟机启动时创建的模块查找器
    static class SafeModuleFinder implements ModuleFinder {
        private final Set<ModuleReference> mrefs;
        private volatile Map<String, ModuleReference> nameToModule;
        
        SafeModuleFinder(ModuleFinder finder) {
            this.mrefs = Collections.unmodifiableSet(finder.findAll());
        }
        
        @Override
        public Optional<ModuleReference> find(String name) {
            Objects.requireNonNull(name);
            Map<String, ModuleReference> nameToModule = this.nameToModule;
            if(nameToModule == null) {
                this.nameToModule = nameToModule = mrefs.stream().collect(Collectors.toMap(m -> m.descriptor().name(), Function.identity()));
            }
            return Optional.ofNullable(nameToModule.get(name));
        }
        
        @Override
        public Set<ModuleReference> findAll() {
            return mrefs;
        }
    }
    
    /**
     * Counters for startup performance analysis.
     */
    // 性能统计
    static class Counters {
        private static final boolean PUBLISH_COUNTERS;
        private static final boolean PRINT_COUNTERS;
        private static Map<String, Long> counters;
        
        static {
            String s = System.getProperty("jdk.module.boot.usePerfData");
            if(s == null) {
                PUBLISH_COUNTERS = false;
                PRINT_COUNTERS = false;
            } else {
                PUBLISH_COUNTERS = true;
                PRINT_COUNTERS = s.equals("debug");
                counters = new LinkedHashMap<>();  // preserve insert order
            }
        }
        
        /**
         * Add a counter
         */
        static void add(String name, long start) {
            if(PUBLISH_COUNTERS || PRINT_COUNTERS) {
                counters.put(name, (System.nanoTime() - start));
            }
        }
        
        /**
         * Publish the counters to the instrumentation buffer or stdout.
         */
        static void publish() {
            if(PUBLISH_COUNTERS || PRINT_COUNTERS) {
                for(Map.Entry<String, Long> e : counters.entrySet()) {
                    String name = e.getKey();
                    long value = e.getValue();
                    if(PUBLISH_COUNTERS) {
                        PerfCounter.newPerfCounter(name).set(value);
                    }
            
                    if(PRINT_COUNTERS) {
                        System.out.println(name + " = " + value);
                    }
                }
            }
        }
    }
    
}
