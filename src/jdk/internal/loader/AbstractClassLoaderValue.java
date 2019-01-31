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
import java.util.Iterator;
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
 * 缓存代理类对象、代理类接口-代理类构造器
 *
 * 将自身作为key，再关联一个value存储到ClassLoader中的一个map对象中（有点类似ThreadLocal的原理）
 * AbstractClassLoaderValue有两种类型：sub-clv和root-clv
 * 其中sub-clv由root-clv孕育，且sub-clv本身可以缓存一个对象
 */
public abstract class AbstractClassLoaderValue<CLV extends AbstractClassLoaderValue<CLV, V>, V> {
    
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
    
    /**
     * Sole constructor.
     */
    AbstractClassLoaderValue() {
    }
    
    /**
     * Returns the key component of this ClassLoaderValue. The key component of
     * the root-{@link ClassLoaderValue} is the ClassLoaderValue itself,
     * while the key component of a {@link #sub(Object) sub}-ClassLoaderValue
     * is what was given to construct it.
     *
     * @return the key component of this ClassLoaderValue.
     */
    // 对于sub-clv，返回其缓存的对象；对于clv，返回自身
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
    /// 判断当前clv与指定clv是否相等，或者当前clv是指定clv的后裔（即由指定clv创建了当前clv）
    public abstract boolean isEqualOrDescendantOf(AbstractClassLoaderValue<?, V> clv);
    
    /**
     * @return a ConcurrentHashMap for given ClassLoader
     */
    // 返回当前ClassLoader内部的classLoaderValueMap
    @SuppressWarnings("unchecked")
    private static <CLV extends AbstractClassLoaderValue<CLV, ?>> ConcurrentHashMap<CLV, Object> map(ClassLoader cl) {
        return (ConcurrentHashMap<CLV, Object>) (
            cl == null
                ? BootLoader.getClassLoaderValueMap()   // 对于boot class loader需要特殊处理
                : JLA.createOrGetClassLoaderValueMap(cl)
        );
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
    // 孕育一个sun-clv
    public <K> Sub<K> sub(K key) {
        return new Sub<K>(key);
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
    // 返回满足条件的动态module或代理接口对应的构造器
    public V computeIfAbsent(ClassLoader cl, BiFunction<? super ClassLoader, ? super CLV, ? extends V> mappingFunction) throws IllegalStateException {
        
        /*
         * 获取ClassLoader内部的map，map中依次缓存的键值对如下：
         *
         * <一> 如果不需要生成动态module
         * 1. <sub-clv[代理接口], Memoizer>         // sub-clv由Proxy#proxyCache孕育
         * 2. <sub-clv[代理类的对象], true>         // sub-clv由ProxyBuilder#reverseProxyCache孕育
         * 3. <sub-clv[代理接口], 代理类的构造方法>  // sub-clv由Proxy#proxyCache孕育，会替换1
         * 最终剩2、3两个键值对
         *
         * <二> 如果需要生成动态module
         * 1. <sub-clv[代理接口], Memoizer>     // sub-clv由Proxy#proxyCache孕育
         * 2. <root-clv, Memoizer>             // root-clv指Proxy.ProxyBuilder#dynProxyModules
         * 3. <root-clv, 动态module>           // root-clv指Proxy.ProxyBuilder#dynProxyModules，会替换掉2
         * 4. <sub-clv[代理类的对象], true>     // sub-clv由ProxyBuilder#reverseProxyCache孕育
         * 5. <sub-clv[代理接口], 代理类的构造方法>  // sub-clv由Proxy#proxyCache孕育，会替换1
         * 最终剩3、4、5三个键值对
         */
        ConcurrentHashMap<CLV, Object> map = map(cl);
        
        @SuppressWarnings("unchecked")
        CLV clv = (CLV) this;   // 记下当前的ClassLoaderValue
        
        Memoizer<CLV, V> mv = null;
        
        while(true) {
            Object val;
            
            // 首次进入循环时，mv为null
            if(mv == null) {
                /*
                 * 本轮循环刚开始时，使用clv作为key在map查找clv关联的值value。
                 *
                 * 对于从Proxy#proxyCache孕育的sub-clv来说，需要比对sub-clv缓存的代理接口类型是否一致。
                 * 如果查找成功则返回关联的值（即代理接口对应的构造器）
                 *
                 * 对于从Proxy.ProxyBuilder#dynProxyModules这个clv来说，需要比对clv引用是否一致。
                 * 如果查找成功则返回关联的module
                 */
                val = map.get(clv);
            } else {
                /*
                 * 非首次进入循环，则Memoizer已创建完毕，将其缓存起来
                 *
                 * 如果该方法由Proxy#proxyCache孕育的sub-clv对象调用，则缓存形式为<sub-clv[代理接口], Memoizer>
                 * 如果该方法由Proxy.ProxyBuilder#dynProxyModules对象调用，则缓存形式为<root-clv, Memoizer>
                 */
                val = map.putIfAbsent(clv, mv);
            }
            
            // 无法找到clv关联的值value
            if(val == null) {
                
                if(mv == null) {
                    // 首次进入循环时，需要创建Memoizer
                    mv = new Memoizer<>(cl, clv, mappingFunction);
                    continue;
                }
                
                // mv != null, therefore sv == null was a result of successful putIfAbsent
                try {
                    /*
                     * 触发Memoizer去完成计算
                     * 在get()过程中，会调用ProxyBuilder#reverseProxyCache孕育sub-clv对象
                     * 该sub-clv对象会生成代理类对象，并以<sub-clv[代理类的对象], true>形式存入map
                     *
                     * 如果当前方法由Proxy#proxyCache对象调用，
                     * 则在get结束时，会返回代理类对象的专用构造器
                     *
                     * 如果当前方法由Proxy.ProxyBuilder#dynProxyModules对象调用，
                     * 则第一次进入get时目的是为了创建动态module，
                     * 并以<clv, 动态module>形式存入map，之后将其返回
                     * 随后，才会创建出代理类对象，并返回其构造器
                     */
                    V v = mv.get();
                    
                    // attempt to replace our Memoizer with the value
                    /*
                     * (key, oldValue, newValue)，更新value
                     * 清除记忆，将其替换成动态module或构造器
                     */
                    map.replace(clv, mv, v);
                    
                    // 返回计算值
                    return v;
                } catch(Throwable t) {
                    // our Memoizer has thrown, attempt to remove it
                    map.remove(clv, mv);
                    // propagate exception because it's from our Memoizer
                    throw t;
                }
            } else {
                try {
                    // 如果val已经被缓存，则返回它
                    return extractValue(val);
                } catch(Memoizer.RecursiveInvocationException e) {
                    // propagate recursive attempts to calculate the same
                    // value as being calculated at the moment
                    throw e;
                } catch(Throwable t) {
                    // don't propagate exceptions thrown from foreign Memoizer -
                    // pretend that there was no entry and retry
                    // (foreign computeIfAbsent invocation will try to remove it anyway)
                }
            }
            
            // TODO:
            // Thread.onSpinLoop(); // when available
        }
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
    // 在cl内部的CLV大本营中获取CLV关联的值
    public V get(ClassLoader cl) {
        // 从ClassLoader中的map中取出以当前对象为key的键值对关联的value
        
        // 获取CLV的大本营
        ConcurrentHashMap<CLV, Object> map = AbstractClassLoaderValue.<CLV>map(cl);
        
        // 返回当前CLV在大本营中关联的值
        Object val = map.get(this);
        
        try {
            // 如果val是Memoizer对象，需要从其中提取目标值，否则直接返回
            return extractValue(val);
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
    /*
     * 如果当前CLV不在cl内部的CLV大本营，则将CLV与其关联的值一起存入大本营
     * 返回值为CLV之前关联的值
     */
    public V putIfAbsent(ClassLoader cl, V v) {
        // 获取ClassLoader内的CLV大本营
        ConcurrentHashMap<CLV, Object> map = map(cl);
        
        @SuppressWarnings("unchecked")
        CLV clv = (CLV) this;
        
        while(true) {
            try {
                /*
                 * 如果当前CLV不在CLV大本营，则将CLV与其关联的值一起存入大本营，并返回null
                 * 否则，返回CLV关联的值
                 */
                Object val = map.putIfAbsent(clv, v);
                
                // 如果CLV关联的值是Memoizer对象，需要从其中提取目标值，否则直接返回
                return extractValue(val);
            } catch(Memoizer.RecursiveInvocationException e) {
                // propagate RecursiveInvocationException for the same key that
                // is just being calculated in computeIfAbsent
                throw e;
            } catch(Throwable t) {
                // don't propagate exceptions thrown from foreign Memoizer -
                // pretend that there was no entry and retry
                // (foreign computeIfAbsent invocation will try to remove it anyway)
            }
            // TODO:
            // Thread.onSpinLoop(); // when available
        }
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
    // 移除指定的键值对，键是当前clv对象，值是v
    public boolean remove(ClassLoader cl, Object v) {
        return AbstractClassLoaderValue.<CLV>map(cl).remove(this, v);
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
    // 移除当前clv所在的键值对
    public void removeAll(ClassLoader cl) {
        ConcurrentHashMap<CLV, Object> map = map(cl);
        for(Iterator<CLV> i = map.keySet().iterator(); i.hasNext(); ) {
            if(i.next().isEqualOrDescendantOf(this)) {
                i.remove();
            }
        }
    }
    
    /**
     * @return value extracted from the {@link Memoizer} if given
     * {@code memoizerOrValue} parameter is a {@code Memoizer} or
     * just return given parameter.
     */
    // 如果memoizerOrValue是Memoizer对象，需要从其中提取目标值，否则直接返回
    @SuppressWarnings("unchecked")
    private V extractValue(Object memoizerOrValue) {
        if(memoizerOrValue instanceof Memoizer) {
            /*
             * 涉及到多线程时，此处缓存的值仍然可能是某个“记忆”
             * 则需要从此记忆中提取（计算）目标值
             */
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
    // sub-ClassLoaderValue，自身作为map的键，而且还可以缓存一个对象
    public final class Sub<K> extends AbstractClassLoaderValue<Sub<K>, V> {
        /*
         * 可以用来缓存某些值，比如：
         * 1. 缓存某个代理接口对应的构造器，如root-ClassLoaderValue为Proxy-proxyCache时
         * 2. 缓存某个代理接口对应的代理对象，如root-ClassLoaderValue为Proxy.ProxyBuilder#reverseProxyCache时
         */
        private final K key;
        
        Sub(K key) {
            this.key = key;
        }
        
        /**
         * @return the parent ClassLoaderValue this sub-ClassLoaderValue
         * has been {@link #sub(Object) derived} from.
         */
        public AbstractClassLoaderValue<CLV, V> parent() {
            return AbstractClassLoaderValue.this;
        }
        
        /**
         * @return the key component of this sub-ClassLoaderValue.
         */
        // 返回sub-clv缓存的对象
        @Override
        public K key() {
            return key;
        }
        
        /**
         * sub-ClassLoaderValue is a descendant of given {@code clv} if it is
         * either equal to it or if its {@link #parent() parent} is a
         * descendant of given {@code clv}.
         */
        // 判断this.clv与clv是否相等，或者this.clv是clv的后裔（即由clv创建了this.clv）
        @Override
        public boolean isEqualOrDescendantOf(AbstractClassLoaderValue<?, V> clv) {
            return equals(Objects.requireNonNull(clv)) || parent().isEqualOrDescendantOf(clv);
        }
        
        // 判等
        @Override
        public boolean equals(Object o) {
            if(this == o)
                return true;
            
            if(!(o instanceof Sub))
                return false;
            
            @SuppressWarnings("unchecked")
            Sub<?> that = (Sub<?>) o;
            
            return this.parent().equals(that.parent()) // 是否孕育自同一个root-ClassLoaderValue
                /*
                 * 对于Proxy#proxyCache创建的sub-ClassLoaderValue来说，需要判断缓存的代理接口类型是否一致
                 * 对于Proxy。ProxyBuilder#reverseProxyCache创建的sub-ClassLoaderValue来说，需要判断缓存的代理对象是否一致
                 */
                && Objects.equals(this.key, that.key);  // 缓存的对象是否一致
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
    // 记忆器
    private static final class Memoizer<CLV extends AbstractClassLoaderValue<CLV, V>, V> implements Supplier<V> {
        
        private final ClassLoader cl;
        private final CLV clv;
        private final BiFunction<? super ClassLoader, ? super CLV, ? extends V> mappingFunction;
        
        private volatile V v;
        private volatile Throwable t;
        private boolean inCall; // 标记是否已经调用过
        
        Memoizer(ClassLoader cl, CLV clv, BiFunction<? super ClassLoader, ? super CLV, ? extends V> mappingFunction) {
            this.cl = cl;
            this.clv = clv;
            this.mappingFunction = mappingFunction;
        }
        
        // 触发mappingFunction中的计算
        @Override
        public V get() throws RecursiveInvocationException {
            V v = this.v;
            if(v != null) {
                return v;
            }
            
            Throwable t = this.t;
            if(t == null) {
                synchronized(this) {
                    if((v = this.v) == null && (t = this.t) == null) {
                        if(inCall) {
                            throw new RecursiveInvocationException();
                        }
                        inCall = true;  // 标记为已调用
                        
                        try {
                            // 计算出动态module或代理对象的构造器
                            this.v = v = Objects.requireNonNull(mappingFunction.apply(cl, clv));
                        } catch(Throwable x) {
                            this.t = t = x;
                        } finally {
                            inCall = false;
                        }
                    }
                }
            }
            if(v != null)
                return v;
            if(t instanceof Error) {
                throw (Error) t;
            } else if(t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new UndeclaredThrowableException(t);
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
