/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.atomic;

import java.io.Serializable;
import java.lang.invoke.VarHandle;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * An {@code int} value that may be updated atomically.  See the
 * {@link VarHandle} specification for descriptions of the properties
 * of atomic accesses. An {@code AtomicInteger} is used in
 * applications such as atomically incremented counters, and cannot be
 * used as a replacement for an {@link java.lang.Integer}. However,
 * this class does extend {@code Number} to allow uniform access by
 * tools and utilities that deal with numerically-based classes.
 *
 * @since 1.5
 * @author Doug Lea
 */
// Integer类型（原子性）
public class AtomicInteger extends Number implements Serializable {
    
    private static final long serialVersionUID = 6214790243416807050L;
    
    // This class intended to be implemented using VarHandles, but there are unresolved cyclic startup dependencies.
    private static final jdk.internal.misc.Unsafe U = jdk.internal.misc.Unsafe.getUnsafe();
    
    private volatile int value;
    
    // 存储字段value在JVM中的偏移地址
    private static final long VALUE = U.objectFieldOffset(AtomicInteger.class, "value");
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new AtomicInteger with initial value {@code 0}.
     */
    // 构造原子变量，其值初始化为0
    public AtomicInteger() {
    }
    
    /**
     * Creates a new AtomicInteger with the given initial value.
     *
     * @param initialValue the initial value
     */
    // 构造原子变量，其值初始化为initialValue
    public AtomicInteger(int initialValue) {
        value = initialValue;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current value, with memory effects as specified by {@link VarHandle#getVolatile}.
     *
     * @return the current value
     */
    // 返回原子变量的值
    public final int get() {
        return value;
    }
    
    /**
     * Returns the current value of this {@code AtomicInteger} as an
     * {@code int},
     * with memory effects as specified by {@link VarHandle#getVolatile}.
     *
     * Equivalent to {@link #get()}.
     */
    // 以int形式返回原子变量的值
    public int intValue() {
        return get();
    }
    
    /**
     * Returns the current value of this {@code AtomicInteger} as a
     * {@code long} after a widening primitive conversion,
     * with memory effects as specified by {@link VarHandle#getVolatile}.
     * @jls 5.1.2 Widening Primitive Conversions
     */
    // 以long形式返回原子变量的值
    public long longValue() {
        return (long)get();
    }
    
    /**
     * Returns the current value of this {@code AtomicInteger} as a
     * {@code float} after a widening primitive conversion,
     * with memory effects as specified by {@link VarHandle#getVolatile}.
     * @jls 5.1.2 Widening Primitive Conversions
     */
    // 以float形式返回原子变量的值
    public float floatValue() {
        return (float)get();
    }
    
    /**
     * Returns the current value of this {@code AtomicInteger} as a
     * {@code double} after a widening primitive conversion,
     * with memory effects as specified by {@link VarHandle#getVolatile}.
     * @jls 5.1.2 Widening Primitive Conversions
     */
    // 以double形式返回原子变量的值
    public double doubleValue() {
        return (double)get();
    }
    
    /**
     * Returns the current value, with memory semantics of reading as if the variable was declared non-{@code volatile}.
     *
     * @return the value
     * @since 9
     */
    // 根据JVM地址偏移量返回原子变量的值
    public final int getPlain() {
        return U.getInt(this, VALUE);
    }
    
    /**
     * Returns the current value, with memory effects as specified by {@link VarHandle#getOpaque}.
     *
     * @return the value
     * @since 9
     */
    // 根据JVM地址偏移量返回原子变量的值
    public final int getOpaque() {
        return U.getIntOpaque(this, VALUE);
    }
    
    /**
     * Returns the current value, with memory effects as specified by {@link VarHandle#getAcquire}.
     *
     * @return the value
     * @since 9
     */
    // 根据JVM地址偏移量返回原子变量的值
    public final int getAcquire() {
        return U.getIntAcquire(this, VALUE);
    }
    
    /*▲ 获取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets the value to {@code newValue},
     * with memory effects as specified by {@link VarHandle#setVolatile}.
     *
     * @param newValue the new value
     */
    // 设置原子变量的值
    public final void set(int newValue) {
        value = newValue;
    }
    
    /**
     * Sets the value to {@code newValue}, with memory semantics
     * of setting as if the variable was declared non-{@code volatile}
     * and non-{@code final}.
     *
     * @param newValue the new value
     * @since 9
     */
    // 根据JVM地址偏移量设置原子变量的值
    public final void setPlain(int newValue) {
        U.putInt(this, VALUE, newValue);
    }
    
    /**
     * Sets the value to {@code newValue}, with memory effects as specified by {@link VarHandle#setOpaque}.
     *
     * @param newValue the new value
     * @since 9
     */
    // 根据JVM地址偏移量设置原子变量的值
    public final void setOpaque(int newValue) {
        U.putIntOpaque(this, VALUE, newValue);
    }
    
    /**
     * Sets the value to {@code newValue}, with memory effects as specified by {@link VarHandle#setRelease}.
     *
     * @param newValue the new value
     * @since 9
     */
    // 根据JVM地址偏移量设置原子变量的值
    public final void setRelease(int newValue) {
        U.putIntRelease(this, VALUE, newValue);
    }
    
    /**
     * Sets the value to {@code newValue}, with memory effects as specified by {@link VarHandle#setRelease}.
     *
     * @param newValue the new value
     * @since 1.6
     */
    // 根据JVM地址偏移量设置原子变量的值
    public final void lazySet(int newValue) {
        U.putIntRelease(this, VALUE, newValue);
    }
    
    /**
     * Atomically sets the value to {@code newValue} and returns the old value,
     * with memory effects as specified by {@link VarHandle#getAndSet}.
     *
     * @param newValue the new value
     * @return the previous value
     */
    // 将当前值原子地更新为newValue，并返回旧值
    public final int getAndSet(int newValue) {
        return U.getAndSetInt(this, VALUE, newValue);
    }
    
    /*▲ 设置值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 比较并更新-1 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#compareAndExchange}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the
     * expected value if successful
     * @since 9
     */
    // 如果当前值为expectedValue，则将其原子地更新为newValue，返回旧值
    public final int compareAndExchange(int expectedValue, int newValue) {
        return U.compareAndExchangeInt(this, VALUE, expectedValue, newValue);
    }
    
    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#compareAndExchangeAcquire}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the
     * expected value if successful
     * @since 9
     */
    // 如果当前值为expectedValue，则将其原子地更新为newValue，返回旧值
    public final int compareAndExchangeAcquire(int expectedValue, int newValue) {
        return U.compareAndExchangeIntAcquire(this, VALUE, expectedValue, newValue);
    }
    
    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#compareAndExchangeRelease}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the
     * expected value if successful
     * @since 9
     */
    // 如果当前值为expectedValue，则将其原子地更新为newValue，返回旧值
    public final int compareAndExchangeRelease(int expectedValue, int newValue) {
        return U.compareAndExchangeIntRelease(this, VALUE, expectedValue, newValue);
    }
    
    /*▲ 比较并更新-1 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 比较并更新-2 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by {@link VarHandle#compareAndSet}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    // 如果当前值为expectedValue，则将其原子地更新为newValue，返回值表示是否更新成功
    public final boolean compareAndSet(int expectedValue, int newValue) {
        return U.compareAndSetInt(this, VALUE, expectedValue, newValue);
    }
    
    /**
     * Possibly atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by {@link VarHandle#weakCompareAndSetPlain}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    // 如果当前值为expectedValue，则将其原子地更新为newValue，返回值表示是否更新成功
    public final boolean weakCompareAndSetPlain(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntPlain(this, VALUE, expectedValue, newValue);
    }
    
    /**
     * Possibly atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by {@link VarHandle#weakCompareAndSetPlain}.
     *
     * @deprecated This method has plain memory effects but the method
     * name implies volatile memory effects (see methods such as
     * {@link #compareAndExchange} and {@link #compareAndSet}).  To avoid
     * confusion over plain or volatile memory effects it is recommended that
     * the method {@link #weakCompareAndSetPlain} be used instead.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @see #weakCompareAndSetPlain
     */
    // 如果当前值为expectedValue，则将其原子地更新为newValue，返回值表示是否更新成功
    @Deprecated(since="9")
    public final boolean weakCompareAndSet(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntPlain(this, VALUE, expectedValue, newValue);
    }
    
    /**
     * Possibly atomically sets the value to {@code newValue} if
     * the current value {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#weakCompareAndSetAcquire}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    // 如果当前值为expectedValue，则将其原子地更新为newValue，返回值表示是否更新成功
    public final boolean weakCompareAndSetAcquire(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntAcquire(this, VALUE, expectedValue, newValue);
    }
    
    /**
     * Possibly atomically sets the value to {@code newValue} if
     * the current value {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#weakCompareAndSetRelease}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    // 如果当前值为expectedValue，则将其原子地更新为newValue，返回值表示是否更新成功
    public final boolean weakCompareAndSetRelease(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntRelease(this, VALUE, expectedValue, newValue);
    }
    
    /**
     * Possibly atomically sets the value to {@code newValue} if
     * the current value {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#weakCompareAndSet}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    // 如果当前值为expectedValue，则将其原子地更新为newValue，返回值表示是否更新成功
    public final boolean weakCompareAndSetVolatile(int expectedValue, int newValue) {
        return U.weakCompareAndSetInt(this, VALUE, expectedValue, newValue);
    }
    
    /*▲ 比较并更新-2 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 增减值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Atomically adds the given value to the current value,
     * with memory effects as specified by {@link VarHandle#getAndAdd}.
     *
     * @param delta the value to add
     * @return the previous value
     */
    // 原子地增加delta，并返回旧值
    public final int getAndAdd(int delta) {
        return U.getAndAddInt(this, VALUE, delta);
    }
    
    /**
     * Atomically adds the given value to the current value,
     * with memory effects as specified by {@link VarHandle#getAndAdd}.
     *
     * @param delta the value to add
     * @return the updated value
     */
    // 原子地增加delta，并返回新值
    public final int addAndGet(int delta) {
        return U.getAndAddInt(this, VALUE, delta) + delta;
    }
    
    /**
     * Atomically increments the current value,
     * with memory effects as specified by {@link VarHandle#getAndAdd}.
     *
     * <p>Equivalent to {@code getAndAdd(1)}.
     *
     * @return the previous value
     */
    // 原子地递增，并返回旧值
    public final int getAndIncrement() {
        return U.getAndAddInt(this, VALUE, 1);
    }
    
    /**
     * Atomically increments the current value,
     * with memory effects as specified by {@link VarHandle#getAndAdd}.
     *
     * <p>Equivalent to {@code addAndGet(1)}.
     *
     * @return the updated value
     */
    // 原子地递增，并返回新值
    public final int incrementAndGet() {
        return U.getAndAddInt(this, VALUE, 1) + 1;
    }
    
    /**
     * Atomically decrements the current value,
     * with memory effects as specified by {@link VarHandle#getAndAdd}.
     *
     * <p>Equivalent to {@code getAndAdd(-1)}.
     *
     * @return the previous value
     */
    // 原子地递减，并返回旧值
    public final int getAndDecrement() {
        return U.getAndAddInt(this, VALUE, -1);
    }
    
    /**
     * Atomically decrements the current value,
     * with memory effects as specified by {@link VarHandle#getAndAdd}.
     *
     * <p>Equivalent to {@code addAndGet(-1)}.
     *
     * @return the updated value
     */
    // 原子地递减，并返回新值
    public final int decrementAndGet() {
        return U.getAndAddInt(this, VALUE, -1) - 1;
    }
    
    /*▲ 增减值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ lambda操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the current value with the results of
     * applying the given function, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    // 对旧值执行给定的操作。如果操作成功，则返回旧值（旧值会动态变化）
    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev = get(), next = 0;
        
        boolean haveNext = false;
        
        while(true){
            // 如果待操作的值发生了变化，需要重新对其操作
            if (!haveNext) {
                next = updateFunction.applyAsInt(prev);
            }
            
            // 用操作后的新值更新旧值，如果更新成功则返回旧值
            if (weakCompareAndSetVolatile(prev, next)) {
                return prev;
            }
            
            // 待操作的值当前是否维持不变（因为可能被别的线程修改）
            haveNext = (prev == (prev = get()));
        }
    }
    
    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the current value with the results of
     * applying the given function, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     *
     * @return the updated value
     *
     * @since 1.8
     */
    // 对旧值执行给定的操作。如果操作成功，则返回操作后的值（旧值会动态变化）
    public final int updateAndGet(IntUnaryOperator updateFunction) {
        int prev = get(), next = 0;
        
        boolean haveNext = false;
        
        while(true){
            if(!haveNext) {
                next = updateFunction.applyAsInt(prev);
            }
            
            if(weakCompareAndSetVolatile(prev, next)) {
                return next;
            }
            
            haveNext = (prev == (prev = get()));
        }
    }
    
    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the current value with the results of
     * applying the given function to the current and given values,
     * returning the previous value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function is
     * applied with the current value as its first argument, and the
     * given update as the second argument.
     *
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     *
     * @return the previous value
     *
     * @since 1.8
     */
    // 对旧值执行给定的操作。如果操作成功，则返回旧值（旧值会动态变化）
    public final int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
        int prev = get(), next = 0;
        
        boolean haveNext = false;
        
        while(true){
            if(!haveNext) {
                next = accumulatorFunction.applyAsInt(prev, x);
            }
            
            if(weakCompareAndSetVolatile(prev, next)) {
                return prev;
            }
            
            haveNext = (prev == (prev = get()));
        }
    }
    
    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the current value with the results of
     * applying the given function to the current and given values,
     * returning the updated value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function is
     * applied with the current value as its first argument, and the
     * given update as the second argument.
     *
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     *
     * @return the updated value
     *
     * @since 1.8
     */
    // 对旧值执行给定的操作。如果操作成功，则返回操作后的值（旧值会动态变化）
    public final int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction) {
        int prev = get(), next = 0;
        
        boolean haveNext = false;
        
        while(true){
            if(!haveNext) {
                next = accumulatorFunction.applyAsInt(prev, x);
            }
            
            if(weakCompareAndSetVolatile(prev, next)) {
                return next;
            }
            
            haveNext = (prev == (prev = get()));
        }
    }
    
    /*▲ lambda操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString() {
        return Integer.toString(get());
    }
}
