/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.Configuration;
import java.lang.module.ModuleReference;
import java.net.URI;

/**
 * A NamedPackage represents a package by name in a specific module.
 *
 * A class loader will automatically create NamedPackage for each
 * package when a class is defined.  Package object is lazily
 * defined until Class::getPackage, Package::getPackage(s), or
 * ClassLoader::getDefinedPackage(s) method is called.
 *
 * NamedPackage allows ClassLoader to keep track of the runtime
 * packages with minimal footprint and avoid constructing Package
 * object.
 */
// 存储package名称和对应的module信息
class NamedPackage {
    private final String name;
    private final Module module;
    
    NamedPackage(String pn, Module module) {
        // 有名字的module中不能放置没名字的包
        if(pn.isEmpty() && module.isNamed()) {
            throw new InternalError("unnamed package in  " + module);
        }
        
        this.name = pn.intern();
        this.module = module;
    }
    
    /**
     * Creates a Package object of the given name and module.
     */
    // 使用给定的包名和模块名创建Package对象
    static Package toPackage(String name, Module module) {
        return new Package(name, module);
    }
    
    /**
     * Returns the name of this package.
     */
    // 返回包名
    String packageName() {
        return name;
    }
    
    /**
     * Returns the module of this named package.
     */
    // 返回模块名
    Module module() {
        return module;
    }
    
    /**
     * Returns the location of the module if this named package is in a named module; otherwise, returns null.
     */
    // 如果此命名包位于命名模块中，则返回模块的位置信息。否则，返回null。
    URI location() {
        if(module.isNamed() && module.getLayer() != null) {
            Configuration cf = module.getLayer().configuration();
            ModuleReference mref = cf.findModule(module.getName()).get().reference();
            return mref.location().orElse(null);
        }
        return null;
    }
}
