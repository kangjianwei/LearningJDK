/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Delegates its invocation to another ConstructorAccessorImpl and can
 * change its delegate at run time.
 */
/*
 * 构造器访问器的代理，配合"Inflation"技术的实现。
 *
 * 该代理会包装一个被代理的构造器访问器。
 * 如果开启了"Inflation"技术，则会用到当前代理类。
 *
 * 当某个构造器被反射调用的次数小于某个阈值时(参考ReflectionFactory#inflationThreshold字段)，被代理的构造器访问器是基于JNI的构造器访问器，
 * 当某个构造器被反射调用的次数超过某个阈值时，被代理的构造器访问器是基于纯Java实现的构造器访问器。
 */
class DelegatingConstructorAccessorImpl extends ConstructorAccessorImpl {
    private ConstructorAccessorImpl delegate;   // 被代理的构造器访问器
    
    DelegatingConstructorAccessorImpl(ConstructorAccessorImpl delegate) {
        setDelegate(delegate);
    }
    
    /*
     * delegate可能的值：
     * 基于JNI的构造器访问器：NativeConstructorAccessorImpl
     * 基于纯Java实现的构造器访问器：jdk/internal/reflect/GeneratedConstructorAccessor
     *                         或：jdk/internal/reflect/GeneratedSerializationConstructorAccessor
     *
     * delegate的切换参见"Inflation"技术的描述
     */
    public Object newInstance(Object[] args) throws InstantiationException, IllegalArgumentException, InvocationTargetException {
        return delegate.newInstance(args);
    }
    
    void setDelegate(ConstructorAccessorImpl delegate) {
        this.delegate = delegate;
    }
}
