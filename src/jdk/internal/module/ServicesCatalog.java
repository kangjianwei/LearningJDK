/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import jdk.internal.loader.ClassLoaderValue;

/**
 * A <em>services catalog</em>. Each {@code ClassLoader} and {@code Layer} has
 * an optional {@code ServicesCatalog} for modules that provide services.
 *
 * @apiNote This class will be replaced once the ServiceLoader is further
 * specified
 */
// 服务及服务实现者的目录
public final class ServicesCatalog {
    
    // 关联了ServicesCatalog值的ClassLoaderValue，用来缓存服务目录
    private static final ClassLoaderValue<ServicesCatalog> CLV = new ClassLoaderValue<>();
    
    /*
     * map代表所有服务目录的集合，存储的信息如下：
     *
     * <服务名称1，[模块1，服务实现者1]-[模块2，服务实现者2]-[模块3，服务实现者3]>
     * <服务名称2，[模块1，服务实现者1]-[模块2，服务实现者2]-[模块3，服务实现者3]>
     * <服务名称3，[模块1，服务实现者1]-[模块2，服务实现者2]-[模块3，服务实现者3]>
     *
     * 术语约定：
     * map中的每个键值对称为一条服务目录；
     * 键值对中的key统称为服务
     * 键值对中的value统称为服务实现者链，其中每个子元素都是一个服务实现者
     *
     * 其中服务名称就是服务接口名，服务实现者就是服务接口的实现类全名，模块是服务实现者所在的模块
     */
    private final Map<String, List<ServiceProvider>> map = new ConcurrentHashMap<>();
    
    private ServicesCatalog() {
    }
    
    /**
     * Creates a ServicesCatalog that supports concurrent registration and and lookup
     */
    // 创建一个空的服务目录
    public static ServicesCatalog create() {
        return new ServicesCatalog();
    }
    
    /**
     * Returns the ServicesCatalog for the given class loader or {@code null}
     * if there is none.
     */
    // 在loader内部的CLV大本营中获取缓存的服务目录，如果不存在则返回null
    public static ServicesCatalog getServicesCatalogOrNull(ClassLoader loader) {
        return CLV.get(loader);
    }
    
    /**
     * Returns the ServicesCatalog for the given class loader, creating it if needed.
     */
    // 在loader内部的CLV大本营中获取缓存的服务目录，如果不存在则新建一个空的服务目录
    public static ServicesCatalog getServicesCatalog(ClassLoader loader) {
        // 在loader内部的CLV大本营中获取缓存的服务目录
        ServicesCatalog catalog = CLV.get(loader);
        
        // 如果catalog为空，说明之前从未存储过服务目录
        if(catalog == null) {
            // 创建一个空的服务目录
            catalog = create();
            
            /*
             * 如果当前CLV不在loader内部的CLV大本营，则将CLV与其关联的服务目录一起存入大本营
             * 返回值为CLV之前关联的服务目录
             */
            ServicesCatalog previous = CLV.putIfAbsent(loader, catalog);
            
            // 如果之前已经存储过某个服务目录
            if(previous != null) {
                // 指向之前存储的服务目录
                catalog = previous;
            }
        }
        
        return catalog;
    }
    
    /**
     * Registers the providers in the given module in this services catalog.
     */
    /*
     * 注册当前模块内的所有服务，即将所有服务实现者缓存到服务目录中
     *
     * 具体操作为：
     * 1.遍历该模块所有的服务接口
     * 2.遍历服务接口内的所有服务实现者
     * 3.将服务接口和服务实现者打包成一条目录存入目录集合，形如：
     *   <服务名称，[模块1，服务实现者1]-[模块2，服务实现者2]-[模块3，服务实现者3]>
     */
    public void register(Module module) {
        // 返回模块描述符
        ModuleDescriptor descriptor = module.getDescriptor();
        
        // 获取该模块中所有的服务接口
        Set<Provides> provideSet = descriptor.provides();
        
        // 遍历每个服务接口
        for(Provides provides : provideSet) {
            // 服务接口名称
            String service = provides.service();
            
            // 该服务接口对应的服务实现者
            List<String> providerNames = provides.providers();
            
            // 服务实现者数量
            int count = providerNames.size();
            
            if(count == 1) {
                // 1. 服务实现者的类名全名
                String pn = providerNames.get(0);
                
                // 2. 创建一个服务实现者对象
                ServiceProvider serviceProvider = new ServiceProvider(module, pn);
                
                // 3. 返回service在服务目录中对应的服务实现者链
                List<ServiceProvider> list = providers(service);
                
                // 4. 将服务实现者对象追加到服务实现者链中（即在服务目录中缓存了该服务实现者的信息）
                list.add(serviceProvider);
            } else {
                // 1. 创建一个空容器暂存服务实现者
                List<ServiceProvider> tmp = new ArrayList<>(count);
                
                for(String pn : providerNames) {
                    // 2. 创建服务实现者对象
                    ServiceProvider serviceProvider = new ServiceProvider(module, pn);
                    
                    // 将服务实现者暂存到上述容器
                    tmp.add(serviceProvider);
                }
                
                // 3. 返回service在服务目录中对应的服务实现者链
                List<ServiceProvider> list = providers(service);
                
                // 4. 将上述所有服务实现者追加到服务实现者链当中（即在服务目录中缓存了这些服务实现者的信息）
                list.addAll(tmp);
            }
        }
    }
    
    /**
     * Add a provider in the given module to this services catalog
     *
     * @apiNote This method is for use by java.lang.instrument
     */
    // 向服务目录的集合中添加一条服务目录
    public void addProvider(Module module, Class<?> service, Class<?> impl) {
        // 返回service在服务目录中对应的服务实现者链，如果还不存在，则会新建
        List<ServiceProvider> list = providers(service.getName());
        list.add(new ServiceProvider(module, impl.getName()));
    }
    
    /**
     * Returns the (possibly empty) list of service providers that implement the given service type.
     */
    // 在服务目录中查找service对应的服务实现者链，如果还不存在，则返回空的列表
    public List<ServiceProvider> findServices(String service) {
        return map.getOrDefault(service, Collections.emptyList());
    }
    
    /**
     * Returns the list of service provides for the given service type
     * name, creating it if needed.
     */
    // 返回service在服务目录中对应的服务实现者链，如果还不存在，则会新建
    private List<ServiceProvider> providers(String service) {
        // 获取service对应的服务实现者链
        List<ServiceProvider> list = map.get(service);
        
        /*
         * 因为ConcurrentHashMap不允许空key和空value
         * 所以list为空时说明该服务接口的服务目录还没创建<...，...>
         */
        if(list == null) {
            // 创建一个空的服务实现者链
            list = new CopyOnWriteArrayList<>();
            
            // 尝试将这条服务目录存入map，并返回旧的服务实现者链
            List<ServiceProvider> prev = map.putIfAbsent(service, list);
            
            // 在单线程中prev保持为空
            if(prev != null) {
                // 让服务实现者链指向已有的服务实现者链，稍后要在其上追加新的服务实现者
                list = prev;  // someone else got there
            }
        }
        
        /*
         * 返回service对应的服务实现者链
         * 可能是一个全新的链条（为空），也可能是旧的链条（之前已经有服务实现者注册过）
         */
        return list;
    }
    
    /**
     * Represents a service provider in the services catalog.
     */
    /*
     * 记录服务实现者的类名和其所在的模块信息：[模块，服务实现者]
     * 换句话说，每个ServiceProvider都可以精确定位到一个服务实现者
     */
    public final class ServiceProvider {
        private final Module module;
        private final String providerName;
        
        public ServiceProvider(Module module, String providerName) {
            this.module = module;
            this.providerName = providerName;
        }
        
        public Module module() {
            return module;
        }
        
        public String providerName() {
            return providerName;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(module, providerName);
        }
        
        @Override
        public boolean equals(Object ob) {
            if(!(ob instanceof ServiceProvider))
                return false;
            ServiceProvider that = (ServiceProvider) ob;
            return Objects.equals(this.module, that.module) && Objects.equals(this.providerName, that.providerName);
        }
    }
}
