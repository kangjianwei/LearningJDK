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

/**
 * One or more variables that together maintain an initially zero
 * {@code long} sum.  When updates (method {@link #add}) are contended
 * across threads, the set of variables may grow dynamically to reduce
 * contention. Method {@link #sum} (or, equivalently, {@link
 * #longValue}) returns the current total combined across the
 * variables maintaining the sum.
 *
 * <p>This class is usually preferable to {@link AtomicLong} when
 * multiple threads update a common sum that is used for purposes such
 * as collecting statistics, not for fine-grained synchronization
 * control.  Under low update contention, the two classes have similar
 * characteristics. But under high contention, expected throughput of
 * this class is significantly higher, at the expense of higher space
 * consumption.
 *
 * <p>LongAdders can be used with a {@link
 * java.util.concurrent.ConcurrentHashMap} to maintain a scalable
 * frequency map (a form of histogram or multiset). For example, to
 * add a count to a {@code ConcurrentHashMap<String,LongAdder> freqs},
 * initializing if not already present, you can use {@code
 * freqs.computeIfAbsent(key, k -> new LongAdder()).increment();}
 *
 * <p>This class extends {@link Number}, but does <em>not</em> define
 * methods such as {@code equals}, {@code hashCode} and {@code
 * compareTo} because instances are expected to be mutated, and so are
 * not useful as collection keys.
 *
 * @author Doug Lea
 * @since 1.8
 */
// 整数加法器，可以看做是对整数的普通原子操作的升级，利用"分段锁"，缓解了线程争用带来的开销，默认只支持增减操作
public class LongAdder extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new adder with initial sum of zero.
     */
    // 初始化一个总和为0的Long加法器
    public LongAdder() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 增/减 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Adds the given value.
     *
     * @param x the value to add
     */
    // 在当前值的基础上增加x（x可以为负数）
    public void add(long x) {
        Cell[] cs;
        long b, v;
        int m;
        Cell c;
        
        if((cs = cells) != null // 如果cells已经存在，则尝试更新cell
            || !casBase(b = base, b + x)) { // 如果cells不存在，则尝试更新基值
            
            /* 至此，说明cells存在，或者cells不存在，且基值更新失败 */
            
            boolean uncontended = true;
            if(cs == null   // cells不存在，且基值更新失败
                || (m = cs.length - 1)<0    // cells存在但无效（长度为0）
                || (c = cs[getProbe() & m]) == null // cells存在且有效，且当前线程关联的cell为null（很可能是探测值无效引起的）
                || !(uncontended = c.cas(v = c.value, v + x))) { // cells存在且有效，且当前线程关联着非空cell，则尝试更新该cell内的【操作系数】
                longAccumulate(x, null, uncontended);
            }
        }
    }
    
    /**
     * Equivalent to {@code add(1)}.
     */
    // 增一
    public void increment() {
        add(1L);
    }
    
    /**
     * Equivalent to {@code add(-1)}.
     */
    // 减一
    public void decrement() {
        add(-1L);
    }
    
    /*▲ 增/减 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current sum.  The returned value is <em>NOT</em> an
     * atomic snapshot; invocation in the absence of concurrent
     * updates returns an accurate result, but concurrent updates that
     * occur while the sum is being calculated might not be
     * incorporated.
     *
     * @return the sum
     */
    // 遍历cells，处理【操作系数】，返回总值
    public long sum() {
        Cell[] cs = cells;
        long sum = base;
        if(cs != null) {
            for(Cell c : cs) {
                if(c != null) {
                    sum += c.value;
                }
            }
        }
        return sum;
    }
    
    /**
     * Returns the {@link #sum} as an {@code int} after a narrowing
     * primitive conversion.
     */
    // 遍历cells，处理【操作系数】，以int形式返回总值
    public int intValue() {
        return (int) sum();
    }
    
    /**
     * Equivalent to {@link #sum}.
     *
     * @return the sum
     */
    // 遍历cells，处理【操作系数】，以long形式返回总值
    public long longValue() {
        return sum();
    }
    
    /**
     * Returns the {@link #sum} as a {@code float}
     * after a widening primitive conversion.
     */
    // 遍历cells，处理【操作系数】，以float形式返回总值
    public float floatValue() {
        return (float) sum();
    }
    
    /**
     * Returns the {@link #sum} as a {@code double} after a widening
     * primitive conversion.
     */
    // 遍历cells，处理【操作系数】，以double形式返回总值
    public double doubleValue() {
        return (double) sum();
    }
    
    /*▲ 获取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 重置 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Resets variables maintaining the sum to zero.  This method may
     * be a useful alternative to creating a new adder, but is only
     * effective if there are no concurrent updates.  Because this
     * method is intrinsically racy, it should only be used when it is
     * known that no threads are concurrently updating.
     */
    // 重置当前的整数加法器
    public void reset() {
        Cell[] cs = cells;
        base = 0L;
        if(cs != null) {
            for(Cell c : cs) {
                if(c != null) {
                    c.reset();
                }
            }
        }
    }
    
    /**
     * Equivalent in effect to {@link #sum} followed by {@link
     * #reset}. This method may apply for example during quiescent
     * points between multithreaded computations.  If there are
     * updates concurrent with this method, the returned value is
     * <em>not</em> guaranteed to be the final value occurring before
     * the reset.
     *
     * @return the sum
     */
    // 以long形式返回总值，并重置当前的整数加法器
    public long sumThenReset() {
        Cell[] cs = cells;
        // 设置基值为0，且用sum接收旧值
        long sum = getAndSetBase(0L);
        if(cs != null) {
            for(Cell c : cs) {
                if(c != null) {
                    // 处理cell中的【操作系数】，处理之后将cell的值置为0
                    sum += c.getAndSet(0L);
                }
            }
        }
        return sum;
    }
    
    /*▲ 重置 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns the String representation of the {@link #sum}.
     *
     * @return the String representation of the {@link #sum}
     */
    public String toString() {
        return Long.toString(sum());
    }
    
    
    
    /**
     * Returns a
     * <a href="../../../../serialized-form.html#java.util.concurrent.atomic.LongAdder.SerializationProxy">
     * SerializationProxy</a>
     * representing the state of this instance.
     *
     * @return a {@link SerializationProxy}
     * representing the state of this instance
     */
    private Object writeReplace() {
        return new SerializationProxy(this);
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
         * The current value returned by sum().
         *
         * @serial
         */
        private final long value;
        
        SerializationProxy(LongAdder a) {
            value = a.sum();
        }
        
        /**
         * Returns a {@code LongAdder} object with initial state
         * held by this proxy.
         *
         * @return a {@code LongAdder} object with initial state
         * held by this proxy
         */
        private Object readResolve() {
            LongAdder a = new LongAdder();
            a.base = value;
            return a;
        }
    }
    
}
