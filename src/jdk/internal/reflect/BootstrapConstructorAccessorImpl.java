/*
 * Copyright (c) 2001, 2011, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import jdk.internal.misc.Unsafe;

/**
 * Uses Unsafe.allocateObject() to instantiate classes; only used for bootstrapping.
 */
/*
 * 如果需要创建ConstructorAccessorImpl类的构造器访问器，那么需要使用当前类。
 * 因为当前类的newInstance()可以不调用构造器就生成对象，
 * 这就避免了在创建ConstructorAccessorImpl类的构造器访问器时触发无限递归。
 */
class BootstrapConstructorAccessorImpl extends ConstructorAccessorImpl {
    private final Constructor<?> constructor;
    
    BootstrapConstructorAccessorImpl(Constructor<?> constructor) {
        this.constructor = constructor;
    }
    
    public Object newInstance(Object[] args) throws IllegalArgumentException, InvocationTargetException {
        try {
            Unsafe unsafe = UnsafeFieldAccessorImpl.unsafe;
            
            // 不调用构造器就生成对象，但是该对象的字段会被赋为对应类型的"零值"，为该对象赋过的默认值也无效
            return unsafe.allocateInstance(constructor.getDeclaringClass());
        } catch(InstantiationException e) {
            throw new InvocationTargetException(e);
        }
    }
}
