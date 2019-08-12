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

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.function.DoubleBinaryOperator;

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

/**
 * One or more variables that together maintain a running {@code double}
 * value updated using a supplied function.  When updates (method
 * {@link #accumulate}) are contended across threads, the set of variables
 * may grow dynamically to reduce contention.  Method {@link #get}
 * (or, equivalently, {@link #doubleValue}) returns the current value
 * across the variables maintaining updates.
 *
 * <p>This class is usually preferable to alternatives when multiple
 * threads update a common value that is used for purposes such as
 * summary statistics that are frequently updated but less frequently
 * read.
 *
 * <p>The supplied accumulator function should be side-effect-free,
 * since it may be re-applied when attempted updates fail due to
 * contention among threads.  For predictable results, the accumulator
 * function should be commutative and associative within the floating
 * point tolerance required in usage contexts. The function is applied
 * with an existing value (or identity) as one argument, and a given
 * update as the other argument. For example, to maintain a running
 * maximum value, you could supply {@code Double::max} along with
 * {@code Double.NEGATIVE_INFINITY} as the identity. The order of
 * accumulation within or across threads is not guaranteed. Thus, this
 * class may not be applicable if numerical stability is required,
 * especially when combining values of substantially different orders
 * of magnitude.
 *
 * <p>Class {@link DoubleAdder} provides analogs of the functionality
 * of this class for the common special case of maintaining sums.  The
 * call {@code new DoubleAdder()} is equivalent to {@code new
 * DoubleAccumulator((x, y) -> x + y, 0.0)}.
 *
 * <p>This class extends {@link Number}, but does <em>not</em> define
 * methods such as {@code equals}, {@code hashCode} and {@code
 * compareTo} because instances are expected to be mutated, and so are
 * not useful as collection keys.
 *
 * @author Doug Lea
 * @since 1.8
 */
// 对DoubleAdder的升级操作，通过内置的function属性，支持对目标值进行更多的操作
public class DoubleAccumulator extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;
    
    // 原始值
    private final long identity;
    
    // 支持对目标值进行更多操作
    private final DoubleBinaryOperator function;
    
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new instance using the given accumulator function
     * and identity element.
     *
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @param identity            identity (initial value) for the accumulator function
     */
    public DoubleAccumulator(DoubleBinaryOperator accumulatorFunction, double identity) {
        this.function = accumulatorFunction;
        base = this.identity = doubleToRawLongBits(identity);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 更新值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Updates with the given value.
     *
     * @param x the value
     */
    // 结合内部的function属性来更新当前值
    public void accumulate(double x) {
        Cell[] cs;
        long b, v, r;
        int m;
        Cell c;
        
        // 执行流程参考DoubleAdder中的add(double)
        if((cs = cells) != null
            || ((r = doubleToRawLongBits(function.applyAsDouble(longBitsToDouble(b = base), x))) != b && !casBase(b, r))) {
            boolean uncontended = true;
            if(cs == null
                || (m = cs.length - 1)<0
                || (c = cs[getProbe() & m]) == null
                || !(uncontended = ((r = doubleToRawLongBits(function.applyAsDouble(longBitsToDouble(v = c.value), x))) == v) || c.cas(v, r))) {
                doubleAccumulate(x, function, uncontended);
            }
        }
    }
    
    /*▲ 更新值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current value.  The returned value is <em>NOT</em>
     * an atomic snapshot; invocation in the absence of concurrent
     * updates returns an accurate result, but concurrent updates that
     * occur while the value is being calculated might not be
     * incorporated.
     *
     * @return the current value
     */
    public double get() {
        Cell[] cs = cells;
        double result = longBitsToDouble(base);
        if(cs != null) {
            for(Cell c : cs)
                if(c != null)
                    result = function.applyAsDouble(result, longBitsToDouble(c.value));
        }
        return result;
    }
    
    /**
     * Returns the {@linkplain #get current value} as an {@code int}
     * after a narrowing primitive conversion.
     */
    public int intValue() {
        return (int) get();
    }
    
    /**
     * Returns the {@linkplain #get current value} as a {@code long}
     * after a narrowing primitive conversion.
     */
    public long longValue() {
        return (long) get();
    }
    
    /**
     * Returns the {@linkplain #get current value} as a {@code float}
     * after a narrowing primitive conversion.
     */
    public float floatValue() {
        return (float) get();
    }
    
    /**
     * Equivalent to {@link #get}.
     *
     * @return the current value
     */
    public double doubleValue() {
        return get();
    }
    
    /*▲ 获取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 重置 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Resets variables maintaining updates to the identity value.
     * This method may be a useful alternative to creating a new
     * updater, but is only effective if there are no concurrent
     * updates.  Because this method is intrinsically racy, it should
     * only be used when it is known that no threads are concurrently
     * updating.
     */
    // 重置为原始值
    public void reset() {
        Cell[] cs = cells;
        base = identity;
        if(cs != null) {
            for(Cell c : cs)
                if(c != null)
                    c.reset(identity);
        }
    }
    
    /**
     * Equivalent in effect to {@link #get} followed by {@link
     * #reset}. This method may apply for example during quiescent
     * points between multithreaded computations.  If there are
     * updates concurrent with this method, the returned value is
     * <em>not</em> guaranteed to be the final value occurring before
     * the reset.
     *
     * @return the value before reset
     */
    // 获取当前值，并重置为原始值
    public double getThenReset() {
        Cell[] cs = cells;
        double result = longBitsToDouble(getAndSetBase(identity));
        if(cs != null) {
            for(Cell c : cs) {
                if(c != null) {
                    double v = longBitsToDouble(c.getAndSet(identity));
                    result = function.applyAsDouble(result, v);
                }
            }
        }
        return result;
    }
    
    /*▲ 重置 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString() {
        return Double.toString(get());
    }
    
    
    
    /**
     * Returns a
     * <a href="../../../../serialized-form.html#java.util.concurrent.atomic.DoubleAccumulator.SerializationProxy">
     * SerializationProxy</a>
     * representing the state of this instance.
     *
     * @return a {@link SerializationProxy}
     * representing the state of this instance
     */
    private Object writeReplace() {
        return new SerializationProxy(get(), function, identity);
    }
    
    /**
     * @param s the stream
     *
     * @throws java.io.InvalidObjectException always
     */
    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
    
    /**
     * Serialization proxy, used to avoid reference to the non-public
     * Striped64 superclass in serialized forms.
     *
     * @serial include
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;
        
        /**
         * The current value returned by get().
         *
         * @serial
         */
        private final double value;
        
        /**
         * The function used for updates.
         *
         * @serial
         */
        private final DoubleBinaryOperator function;
        
        /**
         * The identity value, represented as a long, as converted by
         * {@link Double#doubleToRawLongBits}.  The original identity
         * can be recovered using {@link Double#longBitsToDouble}.
         *
         * @serial
         */
        private final long identity;
        
        SerializationProxy(double value, DoubleBinaryOperator function, long identity) {
            this.value = value;
            this.function = function;
            this.identity = identity;
        }
        
        /**
         * Returns a {@code DoubleAccumulator} object with initial state
         * held by this proxy.
         *
         * @return a {@code DoubleAccumulator} object with initial state
         * held by this proxy
         */
        private Object readResolve() {
            double d = longBitsToDouble(identity);
            DoubleAccumulator a = new DoubleAccumulator(function, d);
            a.base = doubleToRawLongBits(value);
            return a;
        }
    }
    
}
