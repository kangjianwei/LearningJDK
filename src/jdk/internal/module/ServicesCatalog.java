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
// 服务提供者目录
public final class ServicesCatalog {
    
    /*
     * 服务目录CLV，由所有服务目录对象共享
     *
     * 该CLV映射的值是其所在的类加载器可以加载到的服务目录
     */
    private static final ClassLoaderValue<ServicesCatalog> CLV = new ClassLoaderValue<>();
    
    /*
     * 服务集：map代表所有服务目录的集合，存储的信息如下：
     *
     * <服务名称1，[服务提供者1]-->[服务提供者2]-->...>
     * <服务名称2，[服务提供者1]-->[服务提供者2]-->...>
     * <服务名称3，[服务提供者1]-->[服务提供者2]-->...>
     *
     * 术语约定：
     * map中的每个键值对称为一条服务目录；
     * 键值对中的key统称为服务
     * 键值对中的value统称为服务提供者列表，其中每个子元素都是一个服务提供者
     *
     * 注：服务名称就是服务接口名，服务提供者就是服务接口的实现类全名，模块是服务提供者所在的模块
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
    // 返回loder可以加载到的服务目录，如果不存在则返回null
    public static ServicesCatalog getServicesCatalogOrNull(ClassLoader loader) {
        return CLV.get(loader);
    }
    
    /**
     * Returns the ServicesCatalog for the given class loader, creating it if needed.
     */
    // 返回loder可以加载到的服务目录，如果不存在则返回一个空的服务目录
    public static ServicesCatalog getServicesCatalog(ClassLoader loader) {
        // 获取loder可以加载到的服务目录，如果不存在则返回null
        ServicesCatalog catalog = CLV.get(loader);
        // 如果存在相应的服务目录，直接返回
        if(catalog != null) {
            return catalog;
        }
    
        // 创建一个空的服务目录
        catalog = create();
    
        // 向loader的类加载器局部缓存中存入一个CLV对象到catalog的映射，并返回旧(目标)值，不允许覆盖
        ServicesCatalog previous = CLV.putIfAbsent(loader, catalog);
    
        // 如果之前已经存储过某个服务目录
        if(previous != null) {
            // 指向之前存储的服务目录
            catalog = previous;
        }
    
        return catalog;
    }
    
    
    /**
     * Registers the providers in the given module in this services catalog.
     */
    /*
     * 注册指定模块内的所有服务，即将指定模块内所有服务及其对应的服务提供者缓存到服务目录中
     *
     * 具体操作为：
     * 1.遍历该模块所有的服务接口
     * 2.遍历服务接口内的所有服务提供者
     * 3.将服务接口和服务提供者打包成一条目录存入目录集合，形如：<服务名称1，[服务提供者1]-->[服务提供者2]-->...>
     */
    public void register(Module module) {
        // 获取模块描述符
        ModuleDescriptor descriptor = module.getDescriptor();
    
        // 获取该模块中所有服务接口
        Set<Provides> provideSet = descriptor.provides();
        
        // 遍历每个服务接口
        for(Provides provides : provideSet) {
            // 服务接口名称
            String service = provides.service();
    
            // 该服务接口对应的服务提供者
            List<String> providerNames = provides.providers();
    
            // 服务提供者数量
            int count = providerNames.size();
            
            if(count == 1) {
                // 1. 服务提供者的全限定类名
                String providerName = providerNames.get(0);
    
                // 2. 创建一个服务提供者对象
                ServiceProvider serviceProvider = new ServiceProvider(module, providerName);
    
                // 3. 返回service在服务目录中对应的服务提供者列表
                List<ServiceProvider> list = providers(service);
    
                // 4. 将服务提供者对象追加到服务提供者列表中（即在服务目录中缓存了该服务提供者的信息）
                list.add(serviceProvider);
            } else {
                // 1. 创建一个空容器暂存服务提供者
                List<ServiceProvider> tmp = new ArrayList<>(count);
    
                // 遍历所有服务提供者
                for(String providerName : providerNames) {
                    // 2. 创建服务提供者对象
                    ServiceProvider serviceProvider = new ServiceProvider(module, providerName);
        
                    // 将服务提供者暂存到上述容器
                    tmp.add(serviceProvider);
                }
    
                // 3. 返回service在服务目录中对应的服务提供者列表
                List<ServiceProvider> list = providers(service);
    
                // 4. 将上述所有服务提供者追加到服务提供者列表当中（即在服务目录中缓存了这些服务提供者的信息）
                list.addAll(tmp);
            }
        }
    }
    
    /**
     * Add a provider in the given module to this services catalog
     *
     * @apiNote This method is for use by java.lang.instrument
     */
    // 向service的服务目录中插入一个服务提供者
    public void addProvider(Module module, Class<?> service, Class<?> providerImpl) {
        // 返回service在服务集中对应的服务提供者列表，如果不存在，则插入一个空集并返回
        List<ServiceProvider> list = providers(service.getName());
        
        // 构造一个关于service的服务提供者
        ServiceProvider provider = new ServiceProvider(module, providerImpl.getName());
        
        // 缓存到服务目录
        list.add(provider);
    }
    
    /**
     * Returns the (possibly empty) list of service providers that implement the given service type.
     */
    // 在服务集中查找service对应的服务提供者列表，如果还不存在，则简单地返回空集
    public List<ServiceProvider> findServices(String service) {
        return map.getOrDefault(service, Collections.emptyList());
    }
    
    /**
     * Returns the list of service provides for the given service type
     * name, creating it if needed.
     */
    // 返回service在服务集中对应的服务提供者列表，如果不存在，则插入一个空集并返回
    private List<ServiceProvider> providers(String service) {
        // 获取service对应的服务提供者列表
        List<ServiceProvider> list = map.get(service);
        
        /*
         * 因为ConcurrentHashMap不允许空key和空value
         * 所以list为空时说明该服务接口的服务目录还没创建<...，...>
         */
        if(list == null) {
            // 创建一个空的服务提供者列表
            list = new CopyOnWriteArrayList<>();
    
            // 尝试将这条服务目录存入map，并返回旧的服务提供者列表
            List<ServiceProvider> prev = map.putIfAbsent(service, list);
            
            // 在单线程中prev保持为空
            if(prev != null) {
                // 让服务提供者列表指向已有的服务提供者列表，稍后要在其上追加新的服务提供者
                list = prev;  // someone else got there
            }
        }
    
        /*
         * 返回service对应的服务提供者列表
         * 可能是一个全新的链条（为空），也可能是旧的链条（之前已经有服务提供者注册过）
         */
        return list;
    }
    
    
    /**
     * Represents a service provider in the services catalog.
     */
    /*
     * 服务提供(实现)者
     *
     * 记录了服务提供者的名称(类名)和其所在的模块，
     * 每个ServiceProvider都可以精确定位到某个服务的实现类
     */
    public final class ServiceProvider {
        private final String providerName;  // 服务提供者的名称(类名)
        private final Module module;        // 服务提供者所在的模块
        
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
            if(!(ob instanceof ServiceProvider)) {
                return false;
            }
            ServiceProvider that = (ServiceProvider) ob;
            return Objects.equals(this.module, that.module) && Objects.equals(this.providerName, that.providerName);
        }
    }
}
