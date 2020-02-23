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

package jdk.internal.loader;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * root-ClassLoaderValue. Each instance defines a separate namespace for
 * associated values.
 * <p>
 * ClassLoaderValue allows associating a
 * {@link #computeIfAbsent(ClassLoader, BiFunction) computed} non-null value with
 * a {@code (ClassLoader, keys...)} tuple. The associated value, as well as the
 * keys are strongly reachable from the associated ClassLoader so care should be
 * taken to use such keys and values that only reference types resolvable from
 * the associated ClassLoader. Failing that, ClassLoader leaks are inevitable.
 * <p>
 * Example usage:
 * <pre>{@code
 * // create a root instance which represents a namespace and declares the type of
 * // associated values (Class instances in this example)
 * static final ClassLoaderValue<Class<?>> proxyClasses = new ClassLoaderValue<>();
 *
 * // create a compound key composed of a Module and a list of interfaces
 * Module module = ...;
 * List<Class<?>> interfaces = ...;
 * ClassLoaderValue<Class<?>>.Sub<Module>.Sub<List<Class<?>>> key =
 *     proxyClasses.sub(module).sub(interfaces);
 *
 * // use the compound key together with ClassLoader to lazily associate
 * // the value with tuple (loader, module, interfaces) and return it
 * ClassLoader loader = ...;
 * Class<?> proxyClass = key.computeIfAbsent(loader, (ld, ky) -> {
 *     List<Class<?>> intfcs = ky.key();
 *     Module m = ky.parent().key();
 *     Class<?> clazz = defineProxyClass(ld, m, intfcs);
 *     return clazz;
 * });
 * }</pre>
 * <p>
 * {@code classLoaderValue.<operation>(classLoader, ...)} represents an operation
 * to {@link #get}, {@link #putIfAbsent}, {@link #computeIfAbsent} or {@link #remove}
 * a value associated with a (classLoader, classLoaderValue) tuple. ClassLoader
 * instances and root-{@link ClassLoaderValue} instances are compared using
 * identity equality while {@link Sub sub}-ClassLoaderValue instances define
 * {@link #equals(Object) equality} in terms of equality of its
 * {@link Sub#parent() parent} ClassLoaderValue and its
 * {@link #key() key} component.
 *
 * @param <V> the type of value(s) associated with the root-ClassLoaderValue and
 *            all its {@link #sub(Object) descendants}.
 *
 * @author Peter Levart
 * @since 9
 */
/*
 * 根[类加载器局部值](root-clv)
 *
 * root-clv内部不缓存值，通常，它只是简单地作为类加载器局部缓存的一个key。
 *
 * 在多层级的clv中，root-clv作为根[类加载器局部值]存在，即通过root-clv对象，可以构造出一系列子级sub-clv，
 * 这使得多个sub-clv可以拥有一致的“外部环境”。
 */
public final class ClassLoaderValue<V> extends AbstractClassLoaderValue<ClassLoaderValue<V>, V> {
    
    /**
     * Constructs new root-ClassLoaderValue representing its own namespace.
     */
    public ClassLoaderValue() {
    }
    
    /**
     * @return the key component of this root-ClassLoaderValue (itself).
     */
    // 返回[类加载器局部值]中包装的key；对于root-clv，只是简单地返回自身
    @Override
    public ClassLoaderValue<V> key() {
        return this;
    }
    
    /**
     * root-ClassLoaderValue can only be equal to itself and has no predecessors.
     */
    // 判断给定的clv是否与当前clv相等，或者是当前clv的后代
    @Override
    public boolean isEqualOrDescendantOf(AbstractClassLoaderValue<?, V> clv) {
        // 只是简单地比较两个根[类加载器局部值]是否相等
        return equals(Objects.requireNonNull(clv));
    }
}
