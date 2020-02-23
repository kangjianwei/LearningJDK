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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;

/**
 * AbstractClassLoaderValue is a superclass of root-{@link ClassLoaderValue}
 * and {@link Sub sub}-ClassLoaderValue.
 *
 * @param <CLV> the type of concrete ClassLoaderValue (this type)
 * @param <V>   the type of values associated with ClassLoaderValue
 */
/*
 * [类加载器局部值](clv)的抽象实现，有root-clv和sub-clv两种类型。
 *
 * 在每个类加载器中均存在一个类加载器局部缓存，该缓存中经常存储一些与该类加载器密切相关的值。
 * clv就是上述类加载器局部缓存中的key，需要再为其映射一个value，以便一起存到类加载器局部缓存中。
 *
 * 注：这可以类比ThreadLocal
 * 每个线程内有一个线程局部缓存，该线程局部缓存的key是某个线程，而其value是与线程密切相关的值。
 */
public abstract class AbstractClassLoaderValue<CLV extends AbstractClassLoaderValue<CLV, V>, V> {
    
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
    
    /**
     * Sole constructor.
     */
    AbstractClassLoaderValue() {
    }
    
    /**
     * Constructs new sub-ClassLoaderValue of this ClassLoaderValue with given
     * key component.
     *
     * @param key the key component of the sub-ClassLoaderValue.
     * @param <K> the type of the key component.
     *
     * @return a sub-ClassLoaderValue of this ClassLoaderValue for given key
     */
    // 返回一个包含指定key的子级[类加载器局部值]
    public <K> Sub<K> sub(K key) {
        return new Sub<K>(key);
    }
    
    /**
     * Returns the key component of this ClassLoaderValue.
     * The key component of the root-{@link ClassLoaderValue} is the ClassLoaderValue itself,
     * while the key component of a {@link #sub(Object) sub}-ClassLoaderValue is what was given to construct it.
     *
     * @return the key component of this ClassLoaderValue.
     */
    // 返回[类加载器局部值]中包装的key；对于root-clv，只是简单地返回自身
    public abstract Object key();
    
    /**
     * Returns {@code true} if this ClassLoaderValue is equal to given {@code clv}
     * or if this ClassLoaderValue was derived from given {@code clv} by a chain
     * of {@link #sub(Object)} invocations.
     *
     * @param clv the ClassLoaderValue to test this against
     *
     * @return if this ClassLoaderValue is equal to given {@code clv} or
     * its descendant
     */
    // 判断给定的clv是否与当前clv相等，或者是当前clv的后代
    public abstract boolean isEqualOrDescendantOf(AbstractClassLoaderValue<?, V> clv);
    
    /**
     * @return a ConcurrentHashMap for given ClassLoader
     */
    // 返回cl内的类加载器局部缓存
    @SuppressWarnings("unchecked")
    private static <CLV extends AbstractClassLoaderValue<CLV, ?>> ConcurrentHashMap<CLV, Object> map(ClassLoader cl) {
        return (ConcurrentHashMap<CLV, Object>) (
            cl == null
                ? BootLoader.getClassLoaderValueMap()   // 对于boot class loader需要特殊处理
                : JLA.createOrGetClassLoaderValueMap(cl)
        );
    }
    
    /**
     * Returns the value associated with this ClassLoaderValue and given ClassLoader
     * or {@code null} if there is none.
     *
     * @param cl the ClassLoader for the associated value
     *
     * @return the value associated with this ClassLoaderValue and given ClassLoader
     * or {@code null} if there is none.
     */
    // 从cl的类加载器局部缓存中提取当前对象映射的值
    public V get(ClassLoader cl) {
        // 获取cl内的类加载器局部缓存
        ConcurrentHashMap<CLV, Object> map = AbstractClassLoaderValue.<CLV>map(cl);
        
        // 获取当前对象(作为key)在类加载器局部缓存中关联的值
        Object memoizerOrValue = map.get(this);
        
        try {
            // 返回目标值：如果memoizerOrValue是Memoizer，则提取Memoizer中的目标值；否则，原样返回
            return extractValue(memoizerOrValue);
        } catch(Memoizer.RecursiveInvocationException e) {
            // propagate recursive get() for the same key that is just being calculated in computeIfAbsent()
            throw e;
        } catch(Throwable t) {
            // don't propagate exceptions thrown from Memoizer - pretend that there was no entry
            // (computeIfAbsent invocation will try to remove it anyway)
            return null;
        }
    }
    
    /**
     * Associates given value {@code v} with this ClassLoaderValue and given
     * ClassLoader and returns {@code null} if there was no previously associated
     * value or does nothing and returns previously associated value if there
     * was one.
     *
     * @param cl the ClassLoader for the associated value
     * @param v  the value to associate
     *
     * @return previously associated value or null if there was none
     */
    // 向cl的类加载器局部缓存中存入一个当前对象到value的映射，并返回旧(目标)值，不允许覆盖
    public V putIfAbsent(ClassLoader loader, V value) {
        // 获取loader内的类加载器局部缓存
        ConcurrentHashMap<CLV, Object> map = map(loader);
        
        // 使用当前[类加载器局部值]对象作为类加载器局部缓存的key
        @SuppressWarnings("unchecked")
        CLV clv = (CLV) this;
        
        while(true) {
            try {
                // 将当前对象到value的映射存入map，并返回旧值，不允许覆盖
                Object memoizerOrValue = map.putIfAbsent(clv, value);
                
                // 返回目标值：如果memoizerOrValue是Memoizer，则提取Memoizer中的目标值；否则，原样返回
                return extractValue(memoizerOrValue);
            } catch(Memoizer.RecursiveInvocationException e) {
                // propagate RecursiveInvocationException for the same key that is just being calculated in computeIfAbsent
                throw e;
            } catch(Throwable t) {
                // don't propagate exceptions thrown from foreign Memoizer - pretend that there was no entry and retry
                // (foreign computeIfAbsent invocation will try to remove it anyway)
            }
            
            // TODO:
            // Thread.onSpinLoop(); // when available
        }
    }
    
    /**
     * Returns the value associated with this ClassLoaderValue and given
     * ClassLoader if there is one or computes the value by invoking given
     * {@code mappingFunction}, associates it and returns it.
     * <p>
     * Computation and association of the computed value is performed atomically
     * by the 1st thread that requests a particular association while holding a
     * lock associated with this ClassLoaderValue and given ClassLoader.
     * Nested calls from the {@code mappingFunction} to {@link #get},
     * {@link #putIfAbsent} or {@link #computeIfAbsent} for the same association
     * are not allowed and throw {@link IllegalStateException}. Nested call to
     * {@link #remove} for the same association is allowed but will always return
     * {@code false} regardless of passed-in comparison value. Nested calls for
     * other association(s) are allowed, but care should be taken to avoid
     * deadlocks. When two threads perform nested computations of the overlapping
     * set of associations they should always request them in the same order.
     *
     * @param cl              the ClassLoader for the associated value
     * @param mappingFunction the function to compute the value
     *
     * @return the value associated with this ClassLoaderValue and given
     * ClassLoader.
     *
     * @throws IllegalStateException if a direct or indirect invocation from
     *                               within given {@code mappingFunction} that
     *                               computes the value of a particular association
     *                               to {@link #get}, {@link #putIfAbsent} or
     *                               {@link #computeIfAbsent}
     *                               for the same association is attempted.
     */
    // 返回当前clv对象在cl中的类加载器局部缓存中映射的目标值，如果不存在，则计算出新的目标值并缓存起来
    public V computeIfAbsent(ClassLoader cl, BiFunction<? super ClassLoader, ? super CLV, ? extends V> mappingFunction) throws IllegalStateException {
    
        // 获取cl内的类加载器局部缓存
        ConcurrentHashMap<CLV, Object> map = map(cl);
    
        @SuppressWarnings("unchecked")
        CLV clv = (CLV) this;   // 记下当前的[类加载器局部值]对象
    
        Memoizer<CLV, V> memoizer = null;
        
        while(true) {
            Object memoizerOrValue;
    
            // 首次进入循环时，memoizer为null
            if(memoizer == null) {
                // 在特定的类加载器局部缓存中查找clv映射的值
                memoizerOrValue = map.get(clv);
            } else {
                // 再次进入循环时，尝试将clv到memoizer的映射存入特定的类加载器局部缓存中，并返回旧值，不允许覆盖
                memoizerOrValue = map.putIfAbsent(clv, memoizer);
            }
    
            // 如果存在有效的memoizerOrValue，则返回目标值
            if(memoizerOrValue != null) {
                try {
                    // 返回目标值：如果memoizerOrValue是Memoizer，则提取Memoizer中的目标值；否则，原样返回
                    return extractValue(memoizerOrValue);
                } catch(Memoizer.RecursiveInvocationException e) {
                    // propagate recursive attempts to calculate the same value as being calculated at the moment
                    throw e;
                } catch(Throwable t) {
                    // don't propagate exceptions thrown from foreign Memoizer - pretend that there was no entry and retry
                    // (foreign computeIfAbsent invocation will try to remove it anyway)
                }
            } else {
        
                // 如果这是首次进入循环，memoizer必定为null，此时需要创建Memoizer
                if(memoizer == null) {
                    memoizer = new Memoizer<>(cl, clv, mappingFunction);
                    continue;
                }
        
                // mv != null, therefore sv == null was a result of successful putIfAbsent
                try {
                    /* 如果这不是首次进入循环，则memoizer必定不为null，因为之前创建过了 */
            
                    // 获取目标值，会触发mappingFunction中的计算
                    V targetValue = memoizer.get();
            
                    /* attempt to replace our Memoizer with the value */
                    // 将clv映射的memoizer更新为targetValue
                    map.replace(clv, memoizer, targetValue);
            
                    // 返回目标值
                    return targetValue;
                } catch(Throwable t) {
                    // our Memoizer has thrown, attempt to remove it
                    map.remove(clv, memoizer);
                    // propagate exception because it's from our Memoizer
                    throw t;
                }
            }
    
            // TODO:
            // Thread.onSpinLoop(); // when available
        }// while(true)
    }
    
    /**
     * Removes the value associated with this ClassLoaderValue and given
     * ClassLoader if the associated value is equal to given value {@code v} and
     * returns {@code true} or does nothing and returns {@code false} if there is
     * no currently associated value or it is not equal to given value {@code v}.
     *
     * @param cl the ClassLoader for the associated value
     * @param v  the value to compare with currently associated value
     *
     * @return {@code true} if the association was removed or {@code false} if not
     */
    // 从loader的类加载器局部缓存中移除一个元素，该元素的键是当前对象，值是参数中指定的value
    public boolean remove(ClassLoader loader, Object value) {
        // 获取loader内的类加载器局部缓存
        ConcurrentHashMap<CLV, Object> map = AbstractClassLoaderValue.<CLV>map(loader);
    
        // 移除拥有指定键值对的元素
        return map.remove(this, value);
    }
    
    /**
     * Removes all values associated with given ClassLoader {@code cl} and
     * {@link #isEqualOrDescendantOf(AbstractClassLoaderValue) this or descendants}
     * of this ClassLoaderValue.
     * This is not an atomic operation. Other threads may see some associations
     * be already removed and others still present while this method is executing.
     * <p>
     * The sole intention of this method is to cleanup after a unit test that
     * tests ClassLoaderValue directly. It is not intended for use in
     * actual algorithms.
     *
     * @param cl the associated ClassLoader of the values to be removed
     */
    // 从loader的类加载器局部缓存中批量移除元素：这些元素的键是当前对象自身，或是当前对象的后代
    public void removeAll(ClassLoader loader) {
        // 获取loader内的类加载器局部缓存
        ConcurrentHashMap<CLV, Object> map = map(loader);
    
        // 遍历map中的实体，如果该实体的key就是当前对象自身，或是当前对象的后代，则该实体移除
        map.keySet().removeIf(key -> key.isEqualOrDescendantOf(this));
    }
    
    /**
     * @return value extracted from the {@link Memoizer} if given
     * {@code memoizerOrValue} parameter is a {@code Memoizer} or
     * just return given parameter.
     */
    // 返回目标值：如果memoizerOrValue是Memoizer，则提取Memoizer中的目标值；否则，原样返回
    @SuppressWarnings("unchecked")
    private V extractValue(Object memoizerOrValue) {
        if(memoizerOrValue instanceof Memoizer) {
            return ((Memoizer<?, V>) memoizerOrValue).get();
        } else {
            return (V) memoizerOrValue;
        }
    }
    
    /**
     * sub-ClassLoaderValue is an inner class of {@link AbstractClassLoaderValue}
     * and also a subclass of it. It can therefore be instantiated as an inner
     * class of either an instance of root-{@link ClassLoaderValue} or another
     * instance of itself. This enables composing type-safe compound keys of
     * arbitrary length:
     * <pre>{@code
     * ClassLoaderValue<V> clv = new ClassLoaderValue<>();
     * ClassLoaderValue<V>.Sub<K1>.Sub<K2>.Sub<K3> clv_k123 =
     *     clv.sub(k1).sub(k2).sub(k3);
     * }</pre>
     * From which individual components are accessible in a type-safe way:
     * <pre>{@code
     * K1 k1 = clv_k123.parent().parent().key();
     * K2 k2 = clv_k123.parent().key();
     * K3 k3 = clv_k123.key();
     * }</pre>
     * This allows specifying non-capturing lambdas for the mapping function of
     * {@link #computeIfAbsent(ClassLoader, BiFunction)} operation that can
     * access individual key components from passed-in
     * sub-[sub-...]ClassLoaderValue instance in a type-safe way.
     *
     * @param <K> the type of {@link #key()} component contained in the
     *            sub-ClassLoaderValue.
     */
    /*
     * 子级[类加载器局部值](sub-clv)，它也可以做其他sub-clv的父级。
     *
     * sub-clv除了可以作为类加载器局部缓存的一个key，还可以在sub-clv内部缓存一个值。
     */
    public final class Sub<K> extends AbstractClassLoaderValue<Sub<K>, V> {
    
        // 当前sub-clv内缓存的值
        private final K key;
    
        Sub(K key) {
            this.key = key;
        }
    
        /**
         * @return the parent ClassLoaderValue this sub-ClassLoaderValue
         * has been {@link #sub(Object) derived} from.
         */
        // 返回父级[类加载器局部值]
        public AbstractClassLoaderValue<CLV, V> parent() {
            // 此处返回了外部类的引用
            return AbstractClassLoaderValue.this;
        }
        
        /**
         * @return the key component of this sub-ClassLoaderValue.
         */
        // 返回[类加载器局部值]中包装的key；对于root-clv，只是简单地返回自身
        @Override
        public K key() {
            return key;
        }
        
        /**
         * sub-ClassLoaderValue is a descendant of given {@code clv} if it is
         * either equal to it or if its {@link #parent() parent} is a
         * descendant of given {@code clv}.
         */
        // 判断给定的clv是否与当前clv相等，或者是当前clv的后代
        @Override
        public boolean isEqualOrDescendantOf(AbstractClassLoaderValue<?, V> clv) {
            return equals(Objects.requireNonNull(clv)) || parent().isEqualOrDescendantOf(clv);
        }
    
        // 判断等个子级[类加载器局部值]是否相等
        @Override
        public boolean equals(Object o) {
            // 同一个对象，肯定相同
            if(this == o) {
                return true;
            }
    
            // 待比较对象不是Sub，肯定不相同
            if(!(o instanceof Sub)) {
                return false;
            }
    
            @SuppressWarnings("unchecked")
            Sub<?> that = (Sub<?>) o;
    
            return this.parent().equals(that.parent()) // 是否拥有相同的父级[类加载器局部值]
                && Objects.equals(this.key, that.key);  // 包装的key是否一致
    
            /*
             * 对于Proxy#proxyCache创建的sub-ClassLoaderValue来说，需要判断缓存的代理接口类型是否一致
             * 对于Proxy。ProxyBuilder#reverseProxyCache创建的sub-ClassLoaderValue来说，需要判断缓存的代理对象是否一致
             */
        }
        
        @Override
        public int hashCode() {
            return 31 * parent().hashCode() + Objects.hashCode(key);
        }
    }
    
    /**
     * A memoized supplier that invokes given {@code mappingFunction} just once
     * and remembers the result or thrown exception for subsequent calls.
     * If given mappingFunction returns null, it is converted to NullPointerException,
     * thrown from the Memoizer's {@link #get()} method and remembered.
     * If the Memoizer is invoked recursively from the given {@code mappingFunction},
     * {@link RecursiveInvocationException} is thrown, but it is not remembered.
     * The in-flight call to the {@link #get()} can still complete successfully if
     * such exception is handled by the mappingFunction.
     */
    // 记忆棒，用作占位，后续会被其内部的目标值替代
    private static final class Memoizer<CLV extends AbstractClassLoaderValue<CLV, V>, V> implements Supplier<V> {
    
        private final ClassLoader cl;
        private final CLV clv;
    
        // 如果目标值不存在，会使用该函数计算出一个新的目标值
        private final BiFunction<? super ClassLoader, ? super CLV, ? extends V> mappingFunction;
    
        private volatile V targetValue; // 目标值
    
        private volatile Throwable ex;  // 异常信息
    
        private boolean inCall; // 标记get()方法是否正在调用中，防止递归调用
    
        Memoizer(ClassLoader cl, CLV clv, BiFunction<? super ClassLoader, ? super CLV, ? extends V> mappingFunction) {
            this.cl = cl;
            this.clv = clv;
            this.mappingFunction = mappingFunction;
        }
    
        // 返回目标值；如果目标值不存在，会触发mappingFunction中的计算
        @Override
        public V get() throws RecursiveInvocationException {
            V value = this.targetValue;
            if(value != null) {
                return value;
            }
        
            Throwable ex = this.ex;
        
            // 如果当前无异常
            if(ex == null) {
                synchronized(this) {
                    // 如果当前既无目标值，也无异常
                    if((value = this.targetValue) == null && (ex = this.ex) == null) {
                        // 不允许递归调用
                        if(inCall) {
                            throw new RecursiveInvocationException();
                        }
                    
                        inCall = true;  // 标记get()方法正在调用中
                    
                        try {
                            // 根据cl和clv计算出一个新值
                            this.targetValue = value = Objects.requireNonNull(mappingFunction.apply(cl, clv));
                        } catch(Throwable e) {
                            this.ex = ex = e;   // 如果出现异常，则记录异常信息
                        } finally {
                            inCall = false;
                        }
                    }
                }
            }
        
            if(value != null) {
                return value;
            }
        
            if(ex instanceof Error) {
                throw (Error) ex;
            } else if(ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new UndeclaredThrowableException(ex);
            }
        }
        
        static class RecursiveInvocationException extends IllegalStateException {
            private static final long serialVersionUID = 1L;
            
            RecursiveInvocationException() {
                super("Recursive call");
            }
        }
    }
}
