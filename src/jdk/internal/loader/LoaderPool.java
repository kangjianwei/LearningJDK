/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A pool of class loaders.
 *
 * @see ModuleLayer#defineModulesWithManyLoaders
 */
// 模块类加载器池
public final class LoaderPool {
    
    // <模块名, 模块类加载器>映射，指示指定的模块需要用哪个模块类加载器去加载
    private final Map<String, Loader> loaders;  // maps module names to class loaders
    
    /**
     * Creates a pool of class loaders.
     * Each module in the given configuration will be loaded its own class loader in the pool.
     * The class loader is created with the given parent class loader as its parent.
     */
    // 使用同一个父级类加载器构造模块类加载器池，不同的模块使用各自的模块类加载器
    public LoaderPool(Configuration cf, List<ModuleLayer> parentLayers, ClassLoader parentLoader) {
        Map<String, Loader> loaders = new HashMap<>();
        
        // 遍历模块图中所有模块
        for(ResolvedModule resolvedModule : cf.modules()) {
            // 构造模块类加载器
            Loader loader = new Loader(resolvedModule, this, parentLoader);
            
            // 模块名
            String mn = resolvedModule.name();
            
            // 将<模块名, 模块类加载器>映射加入到池中
            loaders.put(mn, loader);
        }
        
        this.loaders = loaders;
        
        // 遍历池中所有模块类加载器
        loaders.values()
            // 初始化依赖包到类加载器的映射，参见字段remotePackageToLoader的说明
            .forEach(loader -> loader.initRemotePackageMap(cf, parentLayers));
    }
    
    /**
     * Returns the class loader for the named module
     */
    // 返回指定模块可以使用的模块类加载器
    public Loader loaderFor(String name) {
        Loader loader = loaders.get(name);
        assert loader != null;
        return loader;
    }
    
    /**
     * Returns a stream of the loaders in this pool.
     */
    // 返回池中所有模块类加载器
    public Stream<Loader> loaders() {
        return loaders.values().stream();
    }
    
}
