/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.module;

import java.io.PrintStream;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Requires.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jdk.internal.module.ModuleHashes;
import jdk.internal.module.ModuleReferenceImpl;
import jdk.internal.module.ModuleTarget;

/**
 * The resolver used by {@link Configuration#resolve} and {@link Configuration#resolveAndBind}.
 *
 * @implNote The resolver is used at VM startup and so deliberately avoids using lambda and stream usages in code paths used during startup.
 */
// 模块依赖解析器，包括对requires依赖和服务依赖的解析
final class Resolver {
    
    private final ModuleFinder beforeFinder;    // 前置查找器
    private final List<Configuration> parents;  // 父模块图集合
    private final ModuleFinder afterFinder;     // 后置查找器
    private final PrintStream traceOutput;
    
    // (依赖图)记录已解析的模块名称到模块引用的映射
    private final Map<String, ModuleReference> nameToReference = new HashMap<>();   // maps module name to module reference
    
    // 是否解析过所有自动模块
    private boolean haveAllAutomaticModules;    // true if all automatic modules have been found
    
    private String targetPlatform;  // constraint on target platform
    
    private Set<ModuleDescriptor> visited;      // the modules that were visited
    private Set<ModuleDescriptor> visitPath;    // the modules in the current visit path
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws IllegalArgumentException if there are more than one parent and
     *                                  the constraints on the target platform conflict
     */
    Resolver(ModuleFinder beforeFinder, List<Configuration> parents, ModuleFinder afterFinder, PrintStream traceOutput) {
        this.beforeFinder = beforeFinder;
        this.parents = parents;
        this.afterFinder = afterFinder;
        this.traceOutput = traceOutput;
        
        // record constraint on target platform, checking for conflicts
        for(Configuration parent : parents) {
            String value = parent.targetPlatform();
            if(value != null) {
                if(targetPlatform == null) {
                    targetPlatform = value;
                } else {
                    if(!value.equals(targetPlatform)) {
                        String msg = "Parents have conflicting constraints on target" + "  platform: " + targetPlatform + ", " + value;
                        throw new IllegalArgumentException(msg);
                    }
                }
            }
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 依赖解析 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Resolves the given named modules.
     *
     * @throws ResolutionException
     */
    // 对roots中还未解析的模块描述符进行依赖解析，解析到的模块信息会存储到nameToReference中
    Resolver resolve(Collection<String> roots) {
        
        // create the visit stack to get us started
        Deque<ModuleDescriptor> queue = new ArrayDeque<>();
        
        /*
         * 遍历roots，在beforeFinder、parents、afterFinder中查找对应的模块描述符，
         * 对于未被解析的模块描述符，会将其添加到queue中以待后续解析
         */
        for(String root : roots) {
            // 使用beforeFinder查找指定名称的模块，返回模块引用
            ModuleReference mref = findWithBeforeFinder(root);
            if(mref == null) {
                // 在模块图parents及其父模块图中搜索指定名称的模块
                if(findInParent(root) == null) {
                    // 如果没有在父模块图中找到root模块，则使用afterFinder查找指定名称的模块，返回模块引用
                    mref = findWithAfterFinder(root);
                    if(mref == null) {
                        findFail("Module %s not found", root);
                    }
                } else {
                    // 如果root模块位于父模块图中，则什么也不做(已经解析过了)
                    continue;
                }
            }
            
            if(isTracing()) {
                trace("root %s", nameAndInfo(mref));
            }
            
            // 将指定的模块记录/映射到缓存nameToReference中
            addFoundModule(mref);
            
            queue.push(mref.descriptor());
        }
        
        // 对queue中给出的模块描述符进行依赖解析，返回解析到的依赖图
        resolve(queue);
        
        return this;
    }
    
    /**
     * Augments the set of resolved modules with modules induced by the service-use relation.
     */
    // 对已解析的模块，需要继续解析其依赖的服务，进一步完善模块依赖图
    Resolver bind() {
    
        /*
         * Scan the finders for all available service provider modules.
         * As java.base uses services then the module finders will be scanned anyway.
         */
        // key是服务名称，value是提供该服务的模块，这些模块来自beforeFinder和afterFinder
        Map<String, Set<ModuleReference>> availableProviders = new HashMap<>();
    
        // 遍历存在于beforeFinder、afterFinder中，但不存在于parents中的模块
        for(ModuleReference mref : findAll()) {
            ModuleDescriptor descriptor = mref.descriptor();
            // 如果当前模块没有provides的服务，直接略过
            if(descriptor.provides().isEmpty()) {
                continue;
            }
        
            // 遍历当前模块provides的服务
            for(Provides provides : descriptor.provides()) {
                String serviceName = provides.service();
            
                Set<ModuleReference> providers = availableProviders.computeIfAbsent(serviceName, key -> new HashSet<>());
            
                providers.add(mref);
            }
        }
    
        // create the visit stack
        Deque<ModuleDescriptor> queue = new ArrayDeque<>();
    
        // 潜在的服务消费者，先从父模块图中查找，再从已解析的模块中查找
        Set<ModuleDescriptor> initialConsumers; // the initial set of modules that may use services
    
        // 还未创建Boot Layer
        if(ModuleLayer.boot() == null) {
            initialConsumers = new HashSet<>();
        } else {
            initialConsumers = parents.stream().flatMap(Configuration::configurations).distinct().flatMap(c -> c.descriptors().stream()).collect(Collectors.toSet());
        }
    
        for(ModuleReference mref : nameToReference.values()) {
            initialConsumers.add(mref.descriptor());
        }
    
        // Where there is a consumer of a service then resolve all modules that provide an implementation of that service
        Set<ModuleDescriptor> candidateConsumers = initialConsumers;
    
        do {
            // 遍历潜在的服务消费者，将为其提供服务的模块添加到模块图
            for(ModuleDescriptor consumer : candidateConsumers) {
                // 如果当前模块没有消费服务，则直接略过
                if(consumer.uses().isEmpty()) {
                    continue;
                }
            
                // the modules that provide at least one service
                Set<ModuleDescriptor> modulesToBind = null;
                if(isTracing()) {
                    modulesToBind = new HashSet<>();
                }
            
                // 遍历当前模块消费的服务
                for(String service : consumer.uses()) {
                    // 获取该服务的提供者
                    Set<ModuleReference> providers = availableProviders.get(service);
                    if(providers == null) {
                        continue;
                    }
                
                    // 遍历服务提供者
                    for(ModuleReference providerRef : providers) {
                        // 提供服务的模块
                        ModuleDescriptor provider = providerRef.descriptor();
                    
                        // 服务消费者与提供者一致，则略过
                        if(provider.equals(consumer)) {
                            continue;
                        }
                    
                        if(isTracing() && modulesToBind.add(provider)) {
                            trace("%s binds %s", consumer.name(), nameAndInfo(providerRef));
                        }
                    
                        // 由于模块provider为模块consumer提供了服务，则将其保存到nameToReference中
                        String pn = provider.name();
                        if(!nameToReference.containsKey(pn)) {
                            addFoundModule(providerRef);
                            queue.push(provider);
                        }
                    }
                }
            }
        
            // 对queue中给出的模块描述符再次进行依赖解析，返回解析到的依赖图
            candidateConsumers = resolve(queue);
        } while(!candidateConsumers.isEmpty());
    
        return this;
    }
    
    /**
     * Resolve all modules in the given queue.
     * On completion the queue will be empty and any resolved modules will be added to {@code nameToReference}.
     *
     * @return The set of module resolved by this invocation of resolve
     */
    // 对queue中给出的模块描述符进行依赖解析，返回解析到的依赖图
    private Set<ModuleDescriptor> resolve(Deque<ModuleDescriptor> queue) {
        // 存储已解析的模块描述符
        Set<ModuleDescriptor> resolved = new HashSet<>();
        
        // 如果队列不为空
        while(!queue.isEmpty()) {
            // 从双向容器头部移除，容器空时返回null
            ModuleDescriptor descriptor = queue.poll();
            assert nameToReference.containsKey(descriptor.name());
            
            /* if the module is an automatic module then all automatic modules need to be resolved */
            // 如果是自动模块，且未解析过所有自动模块，则对所有自动模块进行解析
            if(descriptor.isAutomatic() && !haveAllAutomaticModules) {
                // 遍历存在于beforeFinder、afterFinder中，但不存在于parents中的自动模块，将其添加到queue中
                addFoundAutomaticModules().forEach(mref -> {
                    ModuleDescriptor other = mref.descriptor();
                    queue.offer(other); // 从双向容器尾部加入，容器满时返回false
                    if(isTracing()) {
                        trace("%s requires %s", descriptor.name(), nameAndInfo(mref));
                    }
                });
                
                // 标记已经解析过所有自动模块
                haveAllAutomaticModules = true;
            }
            
            // 遍历descriptor依赖(requires)的元素
            for(ModuleDescriptor.Requires requires : descriptor.requires()) {
                
                /* only required at compile-time */
                // 遇到了静态依赖(requires static)，则忽略它
                if(requires.modifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                
                String dn = requires.name();
                
                // 使用beforeFinder查找指定名称的模块，返回模块引用
                ModuleReference mref = findWithBeforeFinder(dn);
                if(mref == null) {
                    // 在模块图parents及其父模块图中搜索指定名称的模块
                    if(findInParent(dn) == null) {
                        mref = findWithAfterFinder(dn);
                        if(mref == null) {
                            findFail("Module %s not found, required by %s", dn, descriptor.name());
                        }
                    } else {
                        // dependence is in parent
                        continue;
                    }
                }
                
                if(isTracing() && !dn.equals("java.base")) {
                    trace("%s requires %s", descriptor.name(), nameAndInfo(mref));
                }
                
                // 该依赖不在nameToReference中
                if(!nameToReference.containsKey(dn)) {
                    addFoundModule(mref);           // 将指定的模块记录/映射到缓存nameToReference中
                    queue.offer(mref.descriptor()); // 从双向容器尾部加入，容器满时返回false
                }
            }
            
            resolved.add(descriptor);
        }
        
        return resolved;
    }
    
    /**
     * Add all automatic modules that have not already been found to the nameToReference map.
     */
    // 返回存在于beforeFinder、afterFinder中，但不存在于parents中的自动模块
    private Set<ModuleReference> addFoundAutomaticModules() {
        Set<ModuleReference> result = new HashSet<>();
        
        /*
         * 遍历存在于beforeFinder、afterFinder中，但不存在于parents中的模块，
         * 如果遇到了自动模块，且该模块未被解析
         */
        findAll().forEach(mref -> {
            String mn = mref.descriptor().name();
            if(mref.descriptor().isAutomatic() && !nameToReference.containsKey(mn)) {
                addFoundModule(mref);   // 将指定的模块记录/映射到缓存nameToReference中
                result.add(mref);
            }
        });
        
        return result;
    }
    
    /**
     * Add the module to the nameToReference map. Also check any constraints on
     * the target platform with the constraints of other modules.
     */
    // 将指定的模块记录/映射到缓存nameToReference中
    private void addFoundModule(ModuleReference mref) {
        String name = mref.descriptor().name();
        
        if(mref instanceof ModuleReferenceImpl) {
            ModuleTarget target = ((ModuleReferenceImpl) mref).moduleTarget();
            if(target != null) {
                checkTargetPlatform(name, target);
            }
        }
        
        nameToReference.put(name, mref);
    }
    
    /**
     * Find a module of the given name in the parent configurations
     */
    // 在模块图parents及其父模块图中搜索指定名称的模块
    private ResolvedModule findInParent(String name) {
        // 遍历父模块图
        for(Configuration parent : parents) {
            // 在模块图parent及其父模块图中搜索指定名称的模块
            Optional<ResolvedModule> om = parent.findModule(name);
            if(om.isPresent()) {
                return om.get();
            }
        }
        return null;
    }
    
    /**
     * Invokes the beforeFinder to find method to find the given module.
     */
    // 使用beforeFinder查找指定名称的模块，返回模块引用
    private ModuleReference findWithBeforeFinder(String name) {
        return beforeFinder.find(name).orElse(null);
    }
    
    /**
     * Invokes the afterFinder to find method to find the given module.
     */
    // 使用afterFinder查找指定名称的模块，返回模块引用
    private ModuleReference findWithAfterFinder(String name) {
        return afterFinder.find(name).orElse(null);
    }
    
    /**
     * Returns the set of all modules that are observable with the before and after ModuleFinders.
     */
    // 返回存在于beforeFinder、afterFinder中，但不存在于parents中的模块
    private Set<ModuleReference> findAll() {
        Set<ModuleReference> beforeModules = beforeFinder.findAll();
        Set<ModuleReference> afterModules = afterFinder.findAll();
        
        if(afterModules.isEmpty()) {
            return beforeModules;
        }
        
        if(beforeModules.isEmpty() && parents.size() == 1 && parents.get(0) == Configuration.empty()) {
            return afterModules;
        }
        
        // 首先把beforeFinder中找到的模块描述符添加进来
        Set<ModuleReference> result = new HashSet<>(beforeModules);
        
        // 在beforeFinder和parents中查找afterFinder中包含的模块描述符
        for(ModuleReference mref : afterModules) {
            String name = mref.descriptor().name();
            
            if(beforeFinder.find(name).isEmpty() && findInParent(name) == null) {
                // 记录没有找到的元素
                result.add(mref);
            }
        }
        
        return result;
    }
    
    /**
     * Check that the module's constraints on the target platform does
     * conflict with the constraint of other modules resolved so far.
     */
    // 目标平台信息监测
    private void checkTargetPlatform(String mn, ModuleTarget target) {
        String value = target.targetPlatform();
        if(value == null) {
            return;
        }
        
        if(targetPlatform == null) {
            targetPlatform = value;
        } else {
            if(!value.equals(targetPlatform)) {
                findFail("Module %s has constraints on target platform (%s)" + " that conflict with other modules: %s", mn, value, targetPlatform);
            }
        }
    }
    
    /*▲ 依赖解析 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 完成解析 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Execute post-resolution checks and returns the module graph of resolved
     * modules as a map.
     */
    // 对指定的模块依赖图进行循环依赖、可读性等检查
    Map<ResolvedModule, Set<ResolvedModule>> finish(Configuration configuration) {
        // 检测已解析模块中是否存在循环依赖
        detectCycles();
        
        // 检测模块描述符的哈希值
        checkHashes();
        
        Map<ResolvedModule, Set<ResolvedModule>> graph = makeGraph(configuration);
        
        checkExportSuppliers(graph);
        
        return graph;
    }
    
    /**
     * Checks the given module graph for cycles.
     *
     * For now the implementation is a simple depth first search on the
     * dependency graph. We'll replace this later, maybe with Tarjan.
     */
    // 检测已解析模块中是否存在循环依赖
    private void detectCycles() {
        visited = new HashSet<>();
        visitPath = new LinkedHashSet<>(); // preserve insertion order
    
        // 遍历所有解析到的模块
        for(ModuleReference mref : nameToReference.values()) {
            // 深度优先遍历指定的模块与其依赖，检测是否存在循环依赖
            visit(mref.descriptor());
        }
    
        visited.clear();
    }
    
    // 深度优先遍历指定的模块与其依赖，检测是否存在循环依赖
    private void visit(ModuleDescriptor descriptor) {
        if(visited.contains(descriptor)) {
            return;
        }
        
        // 将该模块描述符添加到访问路径
        boolean added = visitPath.add(descriptor);
        if(!added) {
            // 出现了循环遍历
            resolveFail("Cycle detected: %s", cycleAsString(descriptor));
        }
        
        // 遍历descriptor中的依赖
        for(ModuleDescriptor.Requires requires : descriptor.requires()) {
            // 获取模块名称
            String dn = requires.name();
            
            // 在依赖图中查找该模块
            ModuleReference mref = nameToReference.get(dn);
            if(mref == null) {
                continue;
            }
            
            // 获取依赖模块的模块描述符
            ModuleDescriptor other = mref.descriptor();
            // 忽略对自身的依赖
            if(other != descriptor) {
                // dependency is in this configuration
                visit(other);   // 继续访问依赖
            }
        }
        
        // 将该模块描述符从访问路径移除
        visitPath.remove(descriptor);
        
        // 标记该模块的依赖已被遍历完
        visited.add(descriptor);
    }
    
    /**
     * Checks the hashes in the module descriptor to ensure that they match any recorded hashes.
     */
    // 检测模块描述符的哈希值
    private void checkHashes() {
        for(ModuleReference mref : nameToReference.values()) {
            
            // get the recorded hashes, if any
            if(!(mref instanceof ModuleReferenceImpl)) {
                continue;
            }
            
            ModuleHashes hashes = ((ModuleReferenceImpl) mref).recordedHashes();
            if(hashes == null) {
                continue;
            }
            
            ModuleDescriptor descriptor = mref.descriptor();
            String algorithm = hashes.algorithm();
            
            for(String dn : hashes.names()) {
                ModuleReference mref2 = nameToReference.get(dn);
                
                if(mref2 == null) {
                    ResolvedModule resolvedModule = findInParent(dn);
                    if(resolvedModule != null) {
                        mref2 = resolvedModule.reference();
                    }
                }
                
                if(mref2 == null) {
                    continue;
                }
                
                if(!(mref2 instanceof ModuleReferenceImpl)) {
                    findFail("Unable to compute the hash of module %s", dn);
                }
                
                ModuleReferenceImpl other = (ModuleReferenceImpl) mref2;
                byte[] recordedHash = hashes.hashFor(dn);
                byte[] actualHash = other.computeHash(algorithm);
                
                if(actualHash == null) {
                    findFail("Unable to compute the hash of module %s", dn);
                }
                
                if(!Arrays.equals(recordedHash, actualHash)) {
                    findFail("Hash of %s (%s) differs to expected hash (%s)" + " recorded in %s", dn, toHexString(actualHash), toHexString(recordedHash), descriptor.name());
                }
            }
            
        }
    }
    
    /**
     * Computes the readability graph for the modules in the given Configuration.
     *
     * The readability graph is created by propagating "requires" through the
     * "requires transitive" edges of the module dependence graph. So if the
     * module dependence graph has m1 requires m2 && m2 requires transitive m3
     * then the resulting readability graph will contain m1 reads m2, m1 reads m3,
     * and m2 reads m3.
     */
    /*
     * requires static <module>;      // <module>在编译时必须出现，在运行时可选
     * requires transitive <module>;  // 依赖的<module>具有传递性
     */
    private Map<ResolvedModule, Set<ResolvedModule>> makeGraph(Configuration cf) {
        
        // initial capacity of maps to avoid resizing
        int capacity = 1 + (4 * nameToReference.size()) / 3;
        
        // the "reads" graph starts as a module dependence graph and is iteratively updated to be the readability graph
        Map<ResolvedModule, Set<ResolvedModule>> g1 = new HashMap<>(capacity);
        
        // the "requires transitive" graph, contains requires transitive edges only
        Map<ResolvedModule, Set<ResolvedModule>> g2;
        
        /*
         * need "requires transitive" from the modules in parent configurations
         * as there may be selected modules that have a dependency on modules in
         * the parent configuration.
         */
        // 如果boot模块层还未创建
        if(ModuleLayer.boot() == null) {
            g2 = new HashMap<>(capacity);
        } else {
            g2 = parents.stream().flatMap(Configuration::configurations).distinct().flatMap(c -> c.modules().stream().flatMap(m1 -> m1.descriptor().requires().stream().filter(r -> r.modifiers().contains(Modifier.TRANSITIVE)).flatMap(r -> {
                Optional<ResolvedModule> m2 = c.findModule(r.name());
                assert m2.isPresent() || r.modifiers().contains(Modifier.STATIC);
                return m2.stream();
            }).map(m2 -> Map.entry(m1, m2)))) // stream of m1->m2
                .collect(Collectors.groupingBy(Map.Entry::getKey, HashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
        }
        
        // populate g1 and g2 with the dependences from the selected modules
        
        Map<String, ResolvedModule> nameToResolved = new HashMap<>(capacity);
        
        for(ModuleReference mref : nameToReference.values()) {
            ModuleDescriptor descriptor = mref.descriptor();
            String name = descriptor.name();
            
            ResolvedModule m1 = computeIfAbsent(nameToResolved, name, cf, mref);
            
            Set<ResolvedModule> reads = new HashSet<>();
            Set<ResolvedModule> requiresTransitive = new HashSet<>();
            
            for(ModuleDescriptor.Requires requires : descriptor.requires()) {
                String dn = requires.name();
                
                ResolvedModule m2 = null;
                ModuleReference mref2 = nameToReference.get(dn);
                if(mref2 != null) {
                    // same configuration
                    m2 = computeIfAbsent(nameToResolved, dn, cf, mref2);
                } else {
                    // parent configuration
                    m2 = findInParent(dn);
                    if(m2 == null) {
                        assert requires.modifiers().contains(Modifier.STATIC);
                        continue;
                    }
                }
                
                // m1 requires m2 => m1 reads m2
                reads.add(m2);
                
                // m1 requires transitive m2
                if(requires.modifiers().contains(Modifier.TRANSITIVE)) {
                    requiresTransitive.add(m2);
                }
                
            }
            
            // automatic modules read all selected modules and all modules in parent configurations
            if(descriptor.isAutomatic()) {
                /*
                 * reads all selected modules
                 * `requires transitive` all selected automatic modules
                 */
                for(ModuleReference mref2 : nameToReference.values()) {
                    ModuleDescriptor descriptor2 = mref2.descriptor();
                    String name2 = descriptor2.name();
                    
                    if(!name.equals(name2)) {
                        ResolvedModule m2 = computeIfAbsent(nameToResolved, name2, cf, mref2);
                        reads.add(m2);
                        if(descriptor2.isAutomatic()) {
                            requiresTransitive.add(m2);
                        }
                    }
                }
                
                // reads all modules in parent configurations `requires transitive` all automatic modules in parent configurations
                for(Configuration parent : parents) {
                    parent.configurations().map(Configuration::modules).flatMap(Set::stream).forEach(m -> {
                        reads.add(m);
                        if(m.reference().descriptor().isAutomatic()) {
                            requiresTransitive.add(m);
                        }
                    });
                }
            }
            
            g1.put(m1, reads);
            g2.put(m1, requiresTransitive);
        }
        
        // Iteratively update g1 until there are no more requires transitive to propagate
        boolean changed;
        List<ResolvedModule> toAdd = new ArrayList<>();
        do {
            changed = false;
            for(Set<ResolvedModule> m1Reads : g1.values()) {
                for(ResolvedModule m2 : m1Reads) {
                    Set<ResolvedModule> m2RequiresTransitive = g2.get(m2);
                    if(m2RequiresTransitive != null) {
                        for(ResolvedModule m3 : m2RequiresTransitive) {
                            if(!m1Reads.contains(m3)) {
                                // m1 reads m2, m2 requires transitive m3
                                // => need to add m1 reads m3
                                toAdd.add(m3);
                            }
                        }
                    }
                }
                if(!toAdd.isEmpty()) {
                    m1Reads.addAll(toAdd);
                    toAdd.clear();
                    changed = true;
                }
            }
        } while(changed);
        
        return g1;
    }
    
    /**
     * Equivalent to
     * <pre>{@code
     *     map.computeIfAbsent(name, key -> new ResolvedModule(cf, mref))
     * </pre>}
     */
    // 模拟map的computeIfAbsent()方法
    private ResolvedModule computeIfAbsent(Map<String, ResolvedModule> map, String name, Configuration cf, ModuleReference mref) {
        ResolvedModule m = map.get(name);
        if(m == null) {
            m = new ResolvedModule(cf, mref);
            map.put(name, m);
        }
        return m;
    }
    
    /**
     * Checks the readability graph to ensure that
     * <ol>
     *   <li><p> A module does not read two or more modules with the same name.
     *   This includes the case where a module reads another another with the
     *   same name as itself. </p></li>
     *   <li><p> Two or more modules in the configuration don't export the same
     *   package to a module that reads both. This includes the case where a
     *   module {@code M} containing package {@code p} reads another module
     *   that exports {@code p} to {@code M}. </p></li>
     *   <li><p> A module {@code M} doesn't declare that it "{@code uses p.S}"
     *   or "{@code provides p.S with ...}" but package {@code p} is neither
     *   in module {@code M} nor exported to {@code M} by any module that
     *   {@code M} reads. </p></li>
     * </ol>
     */
    private void checkExportSuppliers(Map<ResolvedModule, Set<ResolvedModule>> graph) {
        for(Map.Entry<ResolvedModule, Set<ResolvedModule>> e : graph.entrySet()) {
            ModuleDescriptor descriptor1 = e.getKey().descriptor();
            String name1 = descriptor1.name();
        
            // the names of the modules that are read (including self)
            Set<String> names = new HashSet<>();
            names.add(name1);
        
            // the map of packages that are local or exported to descriptor1
            Map<String, ModuleDescriptor> packageToExporter = new HashMap<>();
        
            // local packages
            Set<String> packages = descriptor1.packages();
            for(String pn : packages) {
                packageToExporter.put(pn, descriptor1);
            }
        
            // descriptor1 reads descriptor2
            Set<ResolvedModule> reads = e.getValue();
            for(ResolvedModule endpoint : reads) {
                ModuleDescriptor descriptor2 = endpoint.descriptor();
            
                String name2 = descriptor2.name();
                if(descriptor2 != descriptor1 && !names.add(name2)) {
                    if(name2.equals(name1)) {
                        resolveFail("Module %s reads another module named %s", name1, name1);
                    } else {
                        resolveFail("Module %s reads more than one module named %s", name1, name2);
                    }
                }
            
                if(descriptor2.isAutomatic()) {
                    // automatic modules read self and export all packages
                    if(descriptor2 != descriptor1) {
                        for(String source : descriptor2.packages()) {
                            ModuleDescriptor supplier = packageToExporter.putIfAbsent(source, descriptor2);
                        
                            // descriptor2 and 'supplier' export source to descriptor1
                            if(supplier != null) {
                                failTwoSuppliers(descriptor1, source, descriptor2, supplier);
                            }
                        }
                    
                    }
                } else {
                    for(ModuleDescriptor.Exports export : descriptor2.exports()) {
                        if(export.isQualified()) {
                            if(!export.targets().contains(descriptor1.name())) {
                                continue;
                            }
                        }
                    
                        // source is exported by descriptor2
                        String source = export.source();
                        ModuleDescriptor supplier = packageToExporter.putIfAbsent(source, descriptor2);
                    
                        // descriptor2 and 'supplier' export source to descriptor1
                        if(supplier != null) {
                            failTwoSuppliers(descriptor1, source, descriptor2, supplier);
                        }
                    }
                
                }
            }
        
            // uses/provides checks not applicable to automatic modules
            if(!descriptor1.isAutomatic()) {
                // uses S
                for(String service : descriptor1.uses()) {
                    String pn = packageName(service);
                    if(!packageToExporter.containsKey(pn)) {
                        resolveFail("Module %s does not read a module that exports %s", descriptor1.name(), pn);
                    }
                }
            
                // provides S
                for(ModuleDescriptor.Provides provides : descriptor1.provides()) {
                    String pn = packageName(provides.service());
                    if(!packageToExporter.containsKey(pn)) {
                        resolveFail("Module %s does not read a module that exports %s", descriptor1.name(), pn);
                    }
                }
            
            }
        }
    }
    
    /**
     * Returns the package name
     */
    // 获取包名
    private static String packageName(String cn) {
        int index = cn.lastIndexOf(".");
        return (index == -1) ? "" : cn.substring(0, index);
    }
    
    /*▲ 完成解析 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 是否追踪输出
    private boolean isTracing() {
        return traceOutput != null;
    }
    
    // 打印追踪信息
    private void trace(String fmt, Object... args) {
        if(traceOutput != null) {
            traceOutput.format(fmt, args);
            traceOutput.println();
        }
    }
    
    // 返回模块描述符的信息以便打印
    private String nameAndInfo(ModuleReference mref) {
        ModuleDescriptor descriptor = mref.descriptor();
        StringBuilder sb = new StringBuilder(descriptor.name());
        mref.location().ifPresent(uri -> sb.append(" " + uri));
        if(descriptor.isAutomatic()) {
            sb.append(" automatic");
        }
        return sb.toString();
    }
    
    /**
     * Throw FindException with the given format string and arguments
     */
    // 模块查找失败时生成错误信息
    private static void findFail(String fmt, Object... args) {
        String msg = String.format(fmt, args);
        throw new FindException(msg);
    }
    
    /**
     * Returns a String with a list of the modules in a detected cycle.
     */
    // 出现循环依赖时，生成错误信息
    private String cycleAsString(ModuleDescriptor descriptor) {
        List<ModuleDescriptor> list = new ArrayList<>(visitPath);
        list.add(descriptor);
        int index = list.indexOf(descriptor);
        return list.stream().skip(index).map(ModuleDescriptor::name).collect(Collectors.joining(" -> "));
    }
    
    /**
     * Throw ResolutionException with the given format string and arguments
     */
    // 解析失败时生成错误信息
    private static void resolveFail(String fmt, Object... args) {
        String msg = String.format(fmt, args);
        throw new ResolutionException(msg);
    }
    
    private static String toHexString(byte[] ba) {
        StringBuilder sb = new StringBuilder(ba.length * 2);
        for(byte b : ba) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
    
    /**
     * Fail because a module in the configuration exports the same package to
     * a module that reads both. This includes the case where a module M
     * containing a package p reads another module that exports p to at least
     * module M.
     */
    private void failTwoSuppliers(ModuleDescriptor descriptor, String source, ModuleDescriptor supplier1, ModuleDescriptor supplier2) {
        
        if(supplier2 == descriptor) {
            ModuleDescriptor tmp = supplier1;
            supplier1 = supplier2;
            supplier2 = tmp;
        }
        
        if(supplier1 == descriptor) {
            resolveFail("Module %s contains package %s" + ", module %s exports package %s to %s", descriptor.name(), source, supplier2.name(), source, descriptor.name());
        } else {
            resolveFail("Modules %s and %s export package %s to module %s", supplier1.name(), supplier2.name(), source, descriptor.name());
        }
        
    }
    
    
    String targetPlatform() {
        return targetPlatform;
    }
    
}
