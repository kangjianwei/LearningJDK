/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
import jdk.internal.misc.Unsafe;

/**
 * An instance of this class is used to generate a stream of
 * pseudorandom numbers. The class uses a 48-bit seed, which is
 * modified using a linear congruential formula. (See Donald Knuth,
 * <i>The Art of Computer Programming, Volume 2</i>, Section 3.2.1.)
 * <p>
 * If two instances of {@code Random} are created with the same
 * seed, and the same sequence of method calls is made for each, they
 * will generate and return identical sequences of numbers. In order to
 * guarantee this property, particular algorithms are specified for the
 * class {@code Random}. Java implementations must use all the algorithms
 * shown here for the class {@code Random}, for the sake of absolute
 * portability of Java code. However, subclasses of class {@code Random}
 * are permitted to use other algorithms, so long as they adhere to the
 * general contracts for all the methods.
 * <p>
 * The algorithms implemented by class {@code Random} use a
 * {@code protected} utility method that on each invocation can supply
 * up to 32 pseudorandomly generated bits.
 * <p>
 * Many applications will find the method {@link Math#random} simpler to use.
 *
 * <p>Instances of {@code java.util.Random} are threadsafe.
 * However, the concurrent use of the same {@code java.util.Random}
 * instance across threads may encounter contention and consequent
 * poor performance. Consider instead using
 * {@link java.util.concurrent.ThreadLocalRandom} in multithreaded
 * designs.
 *
 * <p>Instances of {@code java.util.Random} are not cryptographically
 * secure.  Consider instead using {@link java.security.SecureRandom} to
 * get a cryptographically secure pseudo-random number generator for use
 * by security-sensitive applications.
 *
 * @author  Frank Yellin
 * @since   1.0
 */
/*
 * 伪随机数生成器
 *
 * 线程安全
 * 适用于大多数单线程场景
 *
 * 在多线程中，生成随机数的性能欠佳（存在线程争用）
 * 该类更适用于单线程环境，在多线程中可以使用ThreadLocalRandom
 *
 *   支持使用内置种子计算的原始种子
 *   支持自定义原始种子
 */
public class Random implements Serializable {
    
    /** use serialVersionUID from JDK 1.1 for interoperability */
    static final long serialVersionUID = 3905348978240129619L;
    /**
     * Serializable fields for Random.
     *
     * @serialField seed long
     * seed for random computations
     * @serialField nextNextGaussian double
     * next Gaussian to be returned
     * @serialField haveNextNextGaussian boolean
     * nextNextGaussian is valid
     */
    // 确定哪些字段参与序列化
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("seed", Long.TYPE),
        new ObjectStreamField("nextNextGaussian", Double.TYPE),
        new ObjectStreamField("haveNextNextGaussian", Boolean.TYPE)
    };
    
    // IllegalArgumentException messages
    static final String BadBound = "bound must be positive";
    static final String BadRange = "bound must be greater than origin";
    static final String BadSize = "size must be non-negative";
    
    
    /*▼ 内置种子 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    // 哈希魔数[偶]，作为内置种子的初始值
    private static final long m0 = 0x5DEECE66DL;
    // 哈希魔数[偶]，用来更新内置种子
    private static final long M = 1181783497276652981L;
    /*
     * 内置种子，用于为默认的Random实例生成原始种子
     *
     * 当用户没有显式指定随机数种子时，使用内置种子来推导原始种子的值
     * 每创建一个默认的Random实例，内置种子的值就改变一次
     */
    private static final AtomicLong seedUniquifier = new AtomicLong(m0); // 初始的种子标记
    
    /*▲ 内置种子 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    /*▼ 原始种子 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    // 哈希魔数，用来更新原始种子
    private static final long multiplier = 0x5DEECE66DL;
    private static final long addend = 0xBL;    // 偏移量
    // 更新原始种子时使用的掩码
    private static final long mask = (1L << 48) - 1;
    /**
     * The internal state associated with this pseudorandom number generator.
     * (The specs for the methods in this class describe the ongoing computation of this value.)
     */
    /*
     * 原始种子，Random实例使用该种子生成伪随机数
     *
     * 原始种子的初值可由系统的内置种子配合系统时间生成，也可由用户指定
     * 每生成一个随机数，原始种子的值就改变一次
     *
     * 如果原始种子被单个线程持有，那么接下来生成的一系列随机数是均匀的
     * 如果原始种子被多个线程持有，那么从单个线程的角度观察，其生成的随机数是不均匀的
     */
    private final AtomicLong seed;
    
    /*▲ 原始种子 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    // float值的二进制精度
    private static final float FLOAT_UNIT   = 0x1.0p-24f; // 1.0f/(1 << 24)
    // double值的二进制精度
    private static final double DOUBLE_UNIT = 0x1.0p-53;  // 1.0/(1L << 53)
    
    private double nextNextGaussian;
    private boolean haveNextNextGaussian = false;
    
    
    // Support for resetting seed while deserializing
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long seedOffset;   // 记录seed属性在JVM内存中的的偏移地址
    static {
        try {
            seedOffset = unsafe.objectFieldOffset(Random.class.getDeclaredField("seed"));
        } catch(Exception ex) {
            throw new Error(ex);
        }
    }
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new random number generator. This constructor sets
     * the seed of the random number generator to a value very likely
     * to be distinct from any other invocation of this constructor.
     */
    // 构造默认的伪随机数生成器
    public Random() {
        // 配合当前的系统时间，生成一个内置种子，并进一步计算出原始种子
        this(seedUniquifier() ^ System.nanoTime());
    }
    
    /**
     * Creates a new random number generator using a single {@code long} seed.
     * The seed is the initial value of the internal state of the pseudorandom
     * number generator which is maintained by method {@link #next}.
     *
     * <p>The invocation {@code new Random(seed)} is equivalent to:
     * <pre> {@code
     * Random rnd = new Random();
     * rnd.setSeed(seed);}</pre>
     *
     * @param seed the initial seed
     *
     * @see #setSeed(long)
     */
    // 构造指定种子的伪随机数生成器
    public Random(long seed) {
        if(getClass() == Random.class) {
            // 对指定的种子加工后作为当前Random实例的种子的初始值
            this.seed = new AtomicLong(initialScramble(seed));
        } else {
            // subclass might have overriden setSeed
            this.seed = new AtomicLong();
            setSeed(seed);
        }
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 种子 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 更新内置种子，每初始化一个默认的Random实例就调用一次
    private static long seedUniquifier() {
        // L'Ecuyer, "Tables of Linear Congruential Generators of Different Sizes and Good Lattice Structure", 1999
        for(; ; ) {
            long current = seedUniquifier.get();
            long next = current * M;
            // 更新seedUniquifier为新值next，更新时参考的期望值是current
            if(seedUniquifier.compareAndSet(current, next)) {
                return next;
            }
        }
    }
    
    /**
     * Sets the seed of this random number generator using a single
     * {@code long} seed. The general contract of {@code setSeed} is
     * that it alters the state of this random number generator object
     * so as to be in exactly the same state as if it had just been
     * created with the argument {@code seed} as a seed. The method
     * {@code setSeed} is implemented by class {@code Random} by
     * atomically updating the seed to
     * <pre>{@code (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1)}</pre>
     * and clearing the {@code haveNextNextGaussian} flag used by {@link
     * #nextGaussian}.
     *
     * <p>The implementation of {@code setSeed} by class {@code Random}
     * happens to use only 48 bits of the given seed. In general, however,
     * an overriding method may use all 64 bits of the {@code long}
     * argument as a seed value.
     *
     * @param seed the initial seed
     */
    // 设置原始种子，该方法可能由子类重写
    public synchronized void setSeed(long seed) {
        this.seed.set(initialScramble(seed));
        haveNextNextGaussian = false;
    }
    
    // 加工原始种子
    private static long initialScramble(long seed) {
        return (seed ^ multiplier) & mask;
    }
    
    // 重置原始种子为seedVal
    private void resetSeed(long seedVal) {
        unsafe.putObjectVolatile(this, seedOffset, new AtomicLong(seedVal));
    }
    
    /*▲ 种子 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 生成伪随机数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Generates random bytes and places them into a user-supplied
     * byte array.  The number of random bytes produced is equal to
     * the length of the byte array.
     *
     * <p>The method {@code nextBytes} is implemented by class {@code Random}
     * as if by:
     * <pre> {@code
     * public void nextBytes(byte[] bytes) {
     *   for (int i = 0; i < bytes.length; )
     *     for (int rnd = nextInt(), n = Math.min(bytes.length - i, 4);
     *          n-- > 0; rnd >>= 8)
     *       bytes[i++] = (byte)rnd;
     * }}</pre>
     *
     * @param bytes the byte array to fill with random bytes
     *
     * @throws NullPointerException if the byte array is null
     * @since 1.1
     */
    // 随机填充一个byte数组，有正有负
    public void nextBytes(byte[] bytes) {
        for(int i = 0, len = bytes.length; i<len; ) {
            for(int rnd = nextInt(), n = Math.min(len - i, Integer.SIZE / Byte.SIZE); n-->0; rnd >>= Byte.SIZE) {
                bytes[i++] = (byte) rnd;
            }
        }
    }
    
    /**
     * Returns the next pseudorandom, uniformly distributed {@code int}
     * value from this random number generator's sequence. The general
     * contract of {@code nextInt} is that one {@code int} value is
     * pseudorandomly generated and returned. All 2<sup>32</sup> possible
     * {@code int} values are produced with (approximately) equal probability.
     *
     * <p>The method {@code nextInt} is implemented by class {@code Random}
     * as if by:
     * <pre> {@code
     * public int nextInt() {
     *   return next(32);
     * }}</pre>
     *
     * @return the next pseudorandom, uniformly distributed {@code int}
     * value from this random number generator's sequence
     */
    // 随机生成一个int值，有正有负
    public int nextInt() {
        return next(32);
    }
    
    /**
     * Returns a pseudorandom, uniformly distributed {@code int} value
     * between 0 (inclusive) and the specified value (exclusive), drawn from
     * this random number generator's sequence.  The general contract of
     * {@code nextInt} is that one {@code int} value in the specified range
     * is pseudorandomly generated and returned.  All {@code bound} possible
     * {@code int} values are produced with (approximately) equal
     * probability.  The method {@code nextInt(int bound)} is implemented by
     * class {@code Random} as if by:
     * <pre> {@code
     * public int nextInt(int bound) {
     *   if (bound <= 0)
     *     throw new IllegalArgumentException("bound must be positive");
     *
     *   if ((bound & -bound) == bound)  // i.e., bound is a power of 2
     *     return (int)((bound * (long)next(31)) >> 31);
     *
     *   int bits, val;
     *   do {
     *       bits = next(31);
     *       val = bits % bound;
     *   } while (bits - val + (bound-1) < 0);
     *   return val;
     * }}</pre>
     *
     * <p>The hedge "approximately" is used in the foregoing description only
     * because the next method is only approximately an unbiased source of
     * independently chosen bits.  If it were a perfect source of randomly
     * chosen bits, then the algorithm shown would choose {@code int}
     * values from the stated range with perfect uniformity.
     * <p>
     * The algorithm is slightly tricky.  It rejects values that would result
     * in an uneven distribution (due to the fact that 2^31 is not divisible
     * by n). The probability of a value being rejected depends on n.  The
     * worst case is n=2^30+1, for which the probability of a reject is 1/2,
     * and the expected number of iterations before the loop terminates is 2.
     * <p>
     * The algorithm treats the case where n is a power of two specially: it
     * returns the correct number of high-order bits from the underlying
     * pseudo-random number generator.  In the absence of special treatment,
     * the correct number of <i>low-order</i> bits would be returned.  Linear
     * congruential pseudo-random number generators such as the one
     * implemented by this class are known to have short periods in the
     * sequence of values of their low-order bits.  Thus, this special case
     * greatly increases the length of the sequence of values returned by
     * successive calls to this method if n is a small power of two.
     *
     * @param bound the upper bound (exclusive).  Must be positive.
     *
     * @return the next pseudorandom, uniformly distributed {@code int}
     * value between zero (inclusive) and {@code bound} (exclusive)
     * from this random number generator's sequence
     *
     * @throws IllegalArgumentException if bound is not positive
     * @since 1.2
     */
    // 随机生成一个[0, bound)之内的int值
    public int nextInt(int bound) {
        if(bound<=0) {
            throw new IllegalArgumentException(BadBound);
        }
        
        int r = next(31);
        int m = bound - 1;
        if((bound & m) == 0)  // i.e., bound is a power of 2
            r = (int) ((bound * (long) r) >> 31);
        else {
            for(int u = r; u - (r = u % bound) + m<0; u = next(31))
                ;
        }
        return r;
    }
    
    /**
     * Returns the next pseudorandom, uniformly distributed {@code long}
     * value from this random number generator's sequence. The general
     * contract of {@code nextLong} is that one {@code long} value is
     * pseudorandomly generated and returned.
     *
     * <p>The method {@code nextLong} is implemented by class {@code Random}
     * as if by:
     * <pre> {@code
     * public long nextLong() {
     *   return ((long)next(32) << 32) + next(32);
     * }}</pre>
     *
     * Because class {@code Random} uses a seed with only 48 bits,
     * this algorithm will not return all possible {@code long} values.
     *
     * @return the next pseudorandom, uniformly distributed {@code long}
     * value from this random number generator's sequence
     */
    // 随机生成一个long值，有正有负
    public long nextLong() {
        // it's okay that the bottom word remains signed.
        return ((long) (next(32)) << 32) + next(32);
    }
    
    /**
     * Returns the next pseudorandom, uniformly distributed {@code float}
     * value between {@code 0.0} and {@code 1.0} from this random
     * number generator's sequence.
     *
     * <p>The general contract of {@code nextFloat} is that one
     * {@code float} value, chosen (approximately) uniformly from the
     * range {@code 0.0f} (inclusive) to {@code 1.0f} (exclusive), is
     * pseudorandomly generated and returned. All 2<sup>24</sup> possible
     * {@code float} values of the form <i>m&nbsp;x&nbsp;</i>2<sup>-24</sup>,
     * where <i>m</i> is a positive integer less than 2<sup>24</sup>, are
     * produced with (approximately) equal probability.
     *
     * <p>The method {@code nextFloat} is implemented by class {@code Random}
     * as if by:
     * <pre> {@code
     * public float nextFloat() {
     *   return next(24) / ((float)(1 << 24));
     * }}</pre>
     *
     * <p>The hedge "approximately" is used in the foregoing description only
     * because the next method is only approximately an unbiased source of
     * independently chosen bits. If it were a perfect source of randomly
     * chosen bits, then the algorithm shown would choose {@code float}
     * values from the stated range with perfect uniformity.<p>
     * [In early versions of Java, the result was incorrectly calculated as:
     * <pre> {@code
     *   return next(30) / ((float)(1 << 30));}</pre>
     * This might seem to be equivalent, if not better, but in fact it
     * introduced a slight nonuniformity because of the bias in the rounding
     * of floating-point numbers: it was slightly more likely that the
     * low-order bit of the significand would be 0 than that it would be 1.]
     *
     * @return the next pseudorandom, uniformly distributed {@code float}
     * value between {@code 0.0} and {@code 1.0} from this
     * random number generator's sequence
     */
    // 随机生成一个[0, 1)之内的double值
    public float nextFloat() {
        return next(24) * FLOAT_UNIT;
    }
    
    /**
     * Returns the next pseudorandom, uniformly distributed
     * {@code double} value between {@code 0.0} and
     * {@code 1.0} from this random number generator's sequence.
     *
     * <p>The general contract of {@code nextDouble} is that one
     * {@code double} value, chosen (approximately) uniformly from the
     * range {@code 0.0d} (inclusive) to {@code 1.0d} (exclusive), is
     * pseudorandomly generated and returned.
     *
     * <p>The method {@code nextDouble} is implemented by class {@code Random}
     * as if by:
     * <pre> {@code
     * public double nextDouble() {
     *   return (((long)next(26) << 27) + next(27))
     *     / (double)(1L << 53);
     * }}</pre>
     *
     * <p>The hedge "approximately" is used in the foregoing description only
     * because the {@code next} method is only approximately an unbiased
     * source of independently chosen bits. If it were a perfect source of
     * randomly chosen bits, then the algorithm shown would choose
     * {@code double} values from the stated range with perfect uniformity.
     * <p>[In early versions of Java, the result was incorrectly calculated as:
     * <pre> {@code
     *   return (((long)next(27) << 27) + next(27))
     *     / (double)(1L << 54);}</pre>
     * This might seem to be equivalent, if not better, but in fact it
     * introduced a large nonuniformity because of the bias in the rounding
     * of floating-point numbers: it was three times as likely that the
     * low-order bit of the significand would be 0 than that it would be 1!
     * This nonuniformity probably doesn't matter much in practice, but we
     * strive for perfection.]
     *
     * @return the next pseudorandom, uniformly distributed {@code double}
     * value between {@code 0.0} and {@code 1.0} from this
     * random number generator's sequence
     *
     * @see Math#random
     */
    // 随机生成一个[0, bound)之内的double值
    public double nextDouble() {
        return (((long) (next(26)) << 27) + next(27)) * DOUBLE_UNIT;
    }
    
    /**
     * Returns the next pseudorandom, uniformly distributed
     * {@code boolean} value from this random number generator's
     * sequence. The general contract of {@code nextBoolean} is that one
     * {@code boolean} value is pseudorandomly generated and returned.  The
     * values {@code true} and {@code false} are produced with
     * (approximately) equal probability.
     *
     * <p>The method {@code nextBoolean} is implemented by class {@code Random}
     * as if by:
     * <pre> {@code
     * public boolean nextBoolean() {
     *   return next(1) != 0;
     * }}</pre>
     *
     * @return the next pseudorandom, uniformly distributed
     * {@code boolean} value from this random number generator's
     * sequence
     *
     * @since 1.2
     */
    // 随机生成一个boolean值
    public boolean nextBoolean() {
        return next(1) != 0;
    }
    
    /**
     * Returns the next pseudorandom, Gaussian ("normally") distributed
     * {@code double} value with mean {@code 0.0} and standard
     * deviation {@code 1.0} from this random number generator's sequence.
     * <p>
     * The general contract of {@code nextGaussian} is that one
     * {@code double} value, chosen from (approximately) the usual
     * normal distribution with mean {@code 0.0} and standard deviation
     * {@code 1.0}, is pseudorandomly generated and returned.
     *
     * <p>The method {@code nextGaussian} is implemented by class
     * {@code Random} as if by a threadsafe version of the following:
     * <pre> {@code
     * private double nextNextGaussian;
     * private boolean haveNextNextGaussian = false;
     *
     * public double nextGaussian() {
     *   if (haveNextNextGaussian) {
     *     haveNextNextGaussian = false;
     *     return nextNextGaussian;
     *   } else {
     *     double v1, v2, s;
     *     do {
     *       v1 = 2 * nextDouble() - 1;   // between -1.0 and 1.0
     *       v2 = 2 * nextDouble() - 1;   // between -1.0 and 1.0
     *       s = v1 * v1 + v2 * v2;
     *     } while (s >= 1 || s == 0);
     *     double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s)/s);
     *     nextNextGaussian = v2 * multiplier;
     *     haveNextNextGaussian = true;
     *     return v1 * multiplier;
     *   }
     * }}</pre>
     * This uses the <i>polar method</i> of G. E. P. Box, M. E. Muller, and
     * G. Marsaglia, as described by Donald E. Knuth in <i>The Art of
     * Computer Programming</i>, Volume 2: <i>Seminumerical Algorithms</i>,
     * section 3.4.1, subsection C, algorithm P. Note that it generates two
     * independent values at the cost of only one call to {@code StrictMath.log}
     * and one call to {@code StrictMath.sqrt}.
     *
     * @return the next pseudorandom, Gaussian ("normally") distributed
     * {@code double} value with mean {@code 0.0} and
     * standard deviation {@code 1.0} from this random number
     * generator's sequence
     */
    // 随机生成一个double值，有正有负。所有生成的double值符合标准正态分布
    public synchronized double nextGaussian() {
        // See Knuth, ACP, Section 3.4.1 Algorithm C.
        if(haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        } else {
            double v1, v2, s;
            do {
                v1 = 2 * nextDouble() - 1; // between -1 and 1
                v2 = 2 * nextDouble() - 1; // between -1 and 1
                s = v1 * v1 + v2 * v2;
            } while(s >= 1 || s == 0);
            double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
            nextNextGaussian = v2 * multiplier;
            haveNextNextGaussian = true;
            return v1 * multiplier;
        }
    }
    
    /**
     * Generates the next pseudorandom number. Subclasses should
     * override this, as this is used by all other methods.
     *
     * <p>The general contract of {@code next} is that it returns an
     * {@code int} value and if the argument {@code bits} is between
     * {@code 1} and {@code 32} (inclusive), then that many low-order
     * bits of the returned value will be (approximately) independently
     * chosen bit values, each of which is (approximately) equally
     * likely to be {@code 0} or {@code 1}. The method {@code next} is
     * implemented by class {@code Random} by atomically updating the seed to
     * <pre>{@code (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)}</pre>
     * and returning
     * <pre>{@code (int)(seed >>> (48 - bits))}.</pre>
     *
     * This is a linear congruential pseudorandom number generator, as
     * defined by D. H. Lehmer and described by Donald E. Knuth in
     * <i>The Art of Computer Programming,</i> Volume 2:
     * <i>Seminumerical Algorithms</i>, section 3.2.1.
     *
     * @param bits random bits
     *
     * @return the next pseudorandom value from this random number
     * generator's sequence
     *
     * @since 1.1
     */
    // 随机生成一个int值，该值范围是[0, 2^bits -1)
    protected int next(int bits) {
        long oldseed, nextseed;
        AtomicLong seed = this.seed;
        
        // 原子地更新原始种子，该种子取值范围是[0, mask]
        do {
            oldseed = seed.get();
            nextseed = (oldseed * multiplier + addend) & mask;
        } while(!seed.compareAndSet(oldseed, nextseed));
        
        // 由原始种子计算出哈希值，此时的哈希值与之前的哈希值可能重复
        return (int) (nextseed >>> (48 - bits));
    }
    
    /*▲ 生成伪随机数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 流 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code int}
     * values.
     *
     * <p>A pseudorandom {@code int} value is generated as if it's the result of
     * calling the method {@link #nextInt()}.
     *
     * @return a stream of pseudorandom {@code int} values
     *
     * @implNote This method is implemented to be equivalent to {@code
     * ints(Long.MAX_VALUE)}.
     * @since 1.8
     */
    // 返回的流可以生成Long.MAX_VALUE个随机int值
    public IntStream ints() {
        return StreamSupport.intStream(new RandomIntsSpliterator(this, 0L, Long.MAX_VALUE, Integer.MAX_VALUE, 0), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code int} values.
     *
     * <p>A pseudorandom {@code int} value is generated as if it's the result of
     * calling the method {@link #nextInt()}.
     *
     * @param streamSize the number of values to generate
     *
     * @return a stream of pseudorandom {@code int} values
     *
     * @throws IllegalArgumentException if {@code streamSize} is
     *                                  less than zero
     * @since 1.8
     */
    // 返回的流可以生成streamSize个随机int值
    public IntStream ints(long streamSize) {
        if(streamSize<0L) {
            throw new IllegalArgumentException(BadSize);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(this, 0L, streamSize, Integer.MAX_VALUE, 0), false);
    }
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * int} values, each conforming to the given origin (inclusive) and bound
     * (exclusive).
     *
     * <p>A pseudorandom {@code int} value is generated as if it's the result of
     * calling the following method with the origin and bound:
     * <pre> {@code
     * int nextInt(int origin, int bound) {
     *   int n = bound - origin;
     *   if (n > 0) {
     *     return nextInt(n) + origin;
     *   }
     *   else {  // range not representable as int
     *     int r;
     *     do {
     *       r = nextInt();
     *     } while (r < origin || r >= bound);
     *     return r;
     *   }
     * }}</pre>
     *
     * @param randomNumberOrigin the origin (inclusive) of each random value
     * @param randomNumberBound  the bound (exclusive) of each random value
     *
     * @return a stream of pseudorandom {@code int} values,
     * each with the given origin (inclusive) and bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code randomNumberOrigin}
     *                                  is greater than or equal to {@code randomNumberBound}
     * @implNote This method is implemented to be equivalent to {@code
     * ints(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound)}.
     * @since 1.8
     */
    // 返回的流可以生成Long.MAX_VALUE个随机int值，取值范围是[randomNumberOrigin, randomNumberBound)
    public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
        if(randomNumberOrigin >= randomNumberBound) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(this, 0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number
     * of pseudorandom {@code int} values, each conforming to the given
     * origin (inclusive) and bound (exclusive).
     *
     * <p>A pseudorandom {@code int} value is generated as if it's the result of
     * calling the following method with the origin and bound:
     * <pre> {@code
     * int nextInt(int origin, int bound) {
     *   int n = bound - origin;
     *   if (n > 0) {
     *     return nextInt(n) + origin;
     *   }
     *   else {  // range not representable as int
     *     int r;
     *     do {
     *       r = nextInt();
     *     } while (r < origin || r >= bound);
     *     return r;
     *   }
     * }}</pre>
     *
     * @param streamSize         the number of values to generate
     * @param randomNumberOrigin the origin (inclusive) of each random value
     * @param randomNumberBound  the bound (exclusive) of each random value
     *
     * @return a stream of pseudorandom {@code int} values,
     * each with the given origin (inclusive) and bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code streamSize} is
     *                                  less than zero, or {@code randomNumberOrigin}
     *                                  is greater than or equal to {@code randomNumberBound}
     * @since 1.8
     */
    // 返回的流可以生成streamSize个随机int值，取值范围是[randomNumberOrigin, randomNumberBound)
    public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
        if(streamSize<0L) {
            throw new IllegalArgumentException(BadSize);
        }
        if(randomNumberOrigin >= randomNumberBound) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(this, 0L, streamSize, randomNumberOrigin, randomNumberBound), false);
    }
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code long}
     * values.
     *
     * <p>A pseudorandom {@code long} value is generated as if it's the result
     * of calling the method {@link #nextLong()}.
     *
     * @return a stream of pseudorandom {@code long} values
     *
     * @implNote This method is implemented to be equivalent to {@code
     * longs(Long.MAX_VALUE)}.
     * @since 1.8
     */
    // 返回的流可以生成Long.MAX_VALUE个随机long值
    public LongStream longs() {
        return StreamSupport.longStream(new RandomLongsSpliterator(this, 0L, Long.MAX_VALUE, Long.MAX_VALUE, 0L), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code long} values.
     *
     * <p>A pseudorandom {@code long} value is generated as if it's the result
     * of calling the method {@link #nextLong()}.
     *
     * @param streamSize the number of values to generate
     *
     * @return a stream of pseudorandom {@code long} values
     *
     * @throws IllegalArgumentException if {@code streamSize} is
     *                                  less than zero
     * @since 1.8
     */
    // 返回的流可以生成streamSize个随机long值
    public LongStream longs(long streamSize) {
        if(streamSize<0L) {
            throw new IllegalArgumentException(BadSize);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(this, 0L, streamSize, Long.MAX_VALUE, 0L), false);
    }
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * long} values, each conforming to the given origin (inclusive) and bound
     * (exclusive).
     *
     * <p>A pseudorandom {@code long} value is generated as if it's the result
     * of calling the following method with the origin and bound:
     * <pre> {@code
     * long nextLong(long origin, long bound) {
     *   long r = nextLong();
     *   long n = bound - origin, m = n - 1;
     *   if ((n & m) == 0L)  // power of two
     *     r = (r & m) + origin;
     *   else if (n > 0L) {  // reject over-represented candidates
     *     for (long u = r >>> 1;            // ensure nonnegative
     *          u + m - (r = u % n) < 0L;    // rejection check
     *          u = nextLong() >>> 1) // retry
     *         ;
     *     r += origin;
     *   }
     *   else {              // range not representable as long
     *     while (r < origin || r >= bound)
     *       r = nextLong();
     *   }
     *   return r;
     * }}</pre>
     *
     * @param randomNumberOrigin the origin (inclusive) of each random value
     * @param randomNumberBound  the bound (exclusive) of each random value
     *
     * @return a stream of pseudorandom {@code long} values,
     * each with the given origin (inclusive) and bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code randomNumberOrigin}
     *                                  is greater than or equal to {@code randomNumberBound}
     * @implNote This method is implemented to be equivalent to {@code
     * longs(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound)}.
     * @since 1.8
     */
    // 返回的流可以生成Long.MAX_VALUE个随机long值，取值范围是[randomNumberOrigin, randomNumberBound)
    public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
        if(randomNumberOrigin >= randomNumberBound) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(this, 0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code long}, each conforming to the given origin
     * (inclusive) and bound (exclusive).
     *
     * <p>A pseudorandom {@code long} value is generated as if it's the result
     * of calling the following method with the origin and bound:
     * <pre> {@code
     * long nextLong(long origin, long bound) {
     *   long r = nextLong();
     *   long n = bound - origin, m = n - 1;
     *   if ((n & m) == 0L)  // power of two
     *     r = (r & m) + origin;
     *   else if (n > 0L) {  // reject over-represented candidates
     *     for (long u = r >>> 1;            // ensure nonnegative
     *          u + m - (r = u % n) < 0L;    // rejection check
     *          u = nextLong() >>> 1) // retry
     *         ;
     *     r += origin;
     *   }
     *   else {              // range not representable as long
     *     while (r < origin || r >= bound)
     *       r = nextLong();
     *   }
     *   return r;
     * }}</pre>
     *
     * @param streamSize         the number of values to generate
     * @param randomNumberOrigin the origin (inclusive) of each random value
     * @param randomNumberBound  the bound (exclusive) of each random value
     *
     * @return a stream of pseudorandom {@code long} values,
     * each with the given origin (inclusive) and bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code streamSize} is
     *                                  less than zero, or {@code randomNumberOrigin}
     *                                  is greater than or equal to {@code randomNumberBound}
     * @since 1.8
     */
    // 返回的流可以生成streamSize个随机long值，取值范围是[randomNumberOrigin, randomNumberBound)
    public LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
        if(streamSize<0L) {
            throw new IllegalArgumentException(BadSize);
        }
        if(randomNumberOrigin >= randomNumberBound) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(this, 0L, streamSize, randomNumberOrigin, randomNumberBound), false);
    }
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * double} values, each between zero (inclusive) and one
     * (exclusive).
     *
     * <p>A pseudorandom {@code double} value is generated as if it's the result
     * of calling the method {@link #nextDouble()}.
     *
     * @return a stream of pseudorandom {@code double} values
     *
     * @implNote This method is implemented to be equivalent to {@code
     * doubles(Long.MAX_VALUE)}.
     * @since 1.8
     */
    // 返回的流可以生成Long.MAX_VALUE个随机double值，取值范围是[0, 1)
    public DoubleStream doubles() {
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(this, 0L, Long.MAX_VALUE, Double.MAX_VALUE, 0.0), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code double} values, each between zero
     * (inclusive) and one (exclusive).
     *
     * <p>A pseudorandom {@code double} value is generated as if it's the result
     * of calling the method {@link #nextDouble()}.
     *
     * @param streamSize the number of values to generate
     *
     * @return a stream of {@code double} values
     *
     * @throws IllegalArgumentException if {@code streamSize} is
     *                                  less than zero
     * @since 1.8
     */
    // 返回的流可以生成streamSize个随机double值，取值范围是[0, 1)
    public DoubleStream doubles(long streamSize) {
        if(streamSize<0L) {
            throw new IllegalArgumentException(BadSize);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(this, 0L, streamSize, Double.MAX_VALUE, 0.0), false);
    }
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * double} values, each conforming to the given origin (inclusive) and bound
     * (exclusive).
     *
     * <p>A pseudorandom {@code double} value is generated as if it's the result
     * of calling the following method with the origin and bound:
     * <pre> {@code
     * double nextDouble(double origin, double bound) {
     *   double r = nextDouble();
     *   r = r * (bound - origin) + origin;
     *   if (r >= bound) // correct for rounding
     *     r = Math.nextDown(bound);
     *   return r;
     * }}</pre>
     *
     * @param randomNumberOrigin the origin (inclusive) of each random value
     * @param randomNumberBound  the bound (exclusive) of each random value
     *
     * @return a stream of pseudorandom {@code double} values,
     * each with the given origin (inclusive) and bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code randomNumberOrigin}
     *                                  is greater than or equal to {@code randomNumberBound}
     * @implNote This method is implemented to be equivalent to {@code
     * doubles(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound)}.
     * @since 1.8
     */
    // 返回的流可以生成Long.MAX_VALUE个随机double值，取值范围是[randomNumberOrigin, randomNumberBound)
    public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        if(!(randomNumberOrigin<randomNumberBound)) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(this, 0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code double} values, each conforming to the given origin
     * (inclusive) and bound (exclusive).
     *
     * <p>A pseudorandom {@code double} value is generated as if it's the result
     * of calling the following method with the origin and bound:
     * <pre> {@code
     * double nextDouble(double origin, double bound) {
     *   double r = nextDouble();
     *   r = r * (bound - origin) + origin;
     *   if (r >= bound) // correct for rounding
     *     r = Math.nextDown(bound);
     *   return r;
     * }}</pre>
     *
     * @param streamSize         the number of values to generate
     * @param randomNumberOrigin the origin (inclusive) of each random value
     * @param randomNumberBound  the bound (exclusive) of each random value
     *
     * @return a stream of pseudorandom {@code double} values,
     * each with the given origin (inclusive) and bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code streamSize} is
     *                                  less than zero
     * @throws IllegalArgumentException if {@code randomNumberOrigin}
     *                                  is greater than or equal to {@code randomNumberBound}
     * @since 1.8
     */
    // 返回的流可以生成streamSize个随机double值，取值范围是[randomNumberOrigin, randomNumberBound)
    public DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
        if(streamSize<0L) {
            throw new IllegalArgumentException(BadSize);
        }
        if(!(randomNumberOrigin<randomNumberBound)) {
            throw new IllegalArgumentException(BadRange);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(this, 0L, streamSize, randomNumberOrigin, randomNumberBound), false);
    }
    
    /*▲ 流 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reconstitute the {@code Random} instance from a stream (that is,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        
        ObjectInputStream.GetField fields = s.readFields();
        
        // The seed is read in as {@code long} for historical reasons, but it is converted to an AtomicLong.
        long seedVal = fields.get("seed", -1L);
        if(seedVal<0)
            throw new java.io.StreamCorruptedException("Random: invalid seed");
        resetSeed(seedVal);
        nextNextGaussian = fields.get("nextNextGaussian", 0.0);
        haveNextNextGaussian = fields.get("haveNextNextGaussian", false);
    }
    
    /**
     * Save the {@code Random} instance to a stream.
     */
    private synchronized void writeObject(ObjectOutputStream s) throws IOException {
        
        // set the values of the Serializable fields
        ObjectOutputStream.PutField fields = s.putFields();
        
        // The seed is serialized as a long for historical reasons.
        fields.put("seed", seed.get());
        fields.put("nextNextGaussian", nextNextGaussian);
        fields.put("haveNextNextGaussian", haveNextNextGaussian);
        
        // save them
        s.writeFields();
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * The form of nextInt used by IntStream Spliterators.
     * For the unbounded case: uses nextInt().
     * For the bounded case with representable range: uses nextInt(int bound)
     * For the bounded case with unrepresentable range: uses nextInt()
     *
     * @param origin the least value, unless greater than bound
     * @param bound  the upper bound (exclusive), must not equal origin
     *
     * @return a pseudorandom value
     */
    // 随机生成一个[origin, bound)之内的int值
    final int internalNextInt(int origin, int bound) {
        if(origin<bound) {
            int n = bound - origin;
            if(n>0) {
                return nextInt(n) + origin;
            } else {  // range not representable as int
                int r;
                do {
                    r = nextInt();
                } while(r<origin || r >= bound);
                return r;
            }
        } else {
            return nextInt();
        }
    }
    
    /**
     * The form of nextLong used by LongStream Spliterators.
     * If origin is greater than bound, acts as unbounded form of nextLong, else as bounded form.
     *
     * @param origin the least value, unless greater than bound
     * @param bound  the upper bound (exclusive), must not equal origin
     *
     * @return a pseudorandom value
     */
    // 随机生成一个[origin, bound)之内的long值
    final long internalNextLong(long origin, long bound) {
        long r = nextLong();
        if(origin<bound) {
            long n = bound - origin, m = n - 1;
            if((n & m) == 0L)  // power of two
                r = (r & m) + origin;
            else if(n>0L) {  // reject over-represented candidates
                for(long u = r >>> 1;            // ensure nonnegative
                    u + m - (r = u % n)<0L;    // rejection check
                    u = nextLong() >>> 1) // retry
                    ;
                r += origin;
            } else {              // range not representable as long
                while(r<origin || r >= bound)
                    r = nextLong();
            }
        }
        return r;
    }
    
    /**
     * The form of nextDouble used by DoubleStream Spliterators.
     *
     * @param origin the least value, unless greater than bound
     * @param bound  the upper bound (exclusive), must not equal origin
     *
     * @return a pseudorandom value
     */
    // 随机生成一个[origin, bound)之内的double值
    final double internalNextDouble(double origin, double bound) {
        double r = nextDouble();
        if(origin<bound) {
            r = r * (bound - origin) + origin;
            if(r >= bound) { // correct for rounding
                r = Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
            }
        }
        return r;
    }
    
    
    
    /**
     * Spliterator for int streams.  We multiplex the four int
     * versions into one class by treating a bound less than origin as
     * unbounded, and also by treating "infinite" as equivalent to
     * Long.MAX_VALUE. For splits, it uses the standard divide-by-two
     * approach. The long and double versions of this class are
     * identical except for types.
     */
    // 可以随机生成int元素的流
    static final class RandomIntsSpliterator implements Spliterator.OfInt {
        final Random rng;   // 随机数生成器
        
        // 随机数数量：fence-index
        long   index;
        final long   fence;
        
        // 随机数取值范围：[origin, bound)
        final int    origin;
        final int    bound;
        
        
        RandomIntsSpliterator(Random rng, long index, long fence, int origin, int bound) {
            this.rng = rng;
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }
        
        public RandomIntsSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m<=i) ? null : new RandomIntsSpliterator(rng, i, index = m, origin, bound);
        }
        
        public long estimateSize() {
            return fence - index;
        }
        
        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }
        
        public boolean tryAdvance(IntConsumer consumer) {
            if(consumer == null)
                throw new NullPointerException();
            long i = index, f = fence;
            if(i<f) {
                consumer.accept(rng.internalNextInt(origin, bound));
                index = i + 1;
                return true;
            }
            return false;
        }
        
        public void forEachRemaining(IntConsumer consumer) {
            if(consumer == null)
                throw new NullPointerException();
            long i = index, f = fence;
            if(i<f) {
                index = f;
                Random r = rng;
                int o = origin, b = bound;
                do {
                    consumer.accept(r.internalNextInt(o, b));
                } while(++i<f);
            }
        }
    }
    
    /**
     * Spliterator for long streams.
     */
    // 可以随机生成long元素的流
    static final class RandomLongsSpliterator implements Spliterator.OfLong {
        final Random rng;
        
        // 随机数数量：fence-index
        long index;
        final long fence;
        
        // 随机数取值范围：[origin, bound)
        final long origin;
        final long bound;
        
        
        RandomLongsSpliterator(Random rng, long index, long fence, long origin, long bound) {
            this.rng = rng;
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }
        
        public RandomLongsSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m<=i) ? null : new RandomLongsSpliterator(rng, i, index = m, origin, bound);
        }
        
        public long estimateSize() {
            return fence - index;
        }
        
        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }
        
        public boolean tryAdvance(LongConsumer consumer) {
            if(consumer == null)
                throw new NullPointerException();
            long i = index, f = fence;
            if(i<f) {
                consumer.accept(rng.internalNextLong(origin, bound));
                index = i + 1;
                return true;
            }
            return false;
        }
        
        public void forEachRemaining(LongConsumer consumer) {
            if(consumer == null)
                throw new NullPointerException();
            long i = index, f = fence;
            if(i<f) {
                index = f;
                Random r = rng;
                long o = origin, b = bound;
                do {
                    consumer.accept(r.internalNextLong(o, b));
                } while(++i<f);
            }
        }
        
    }
    
    /**
     * Spliterator for double streams.
     */
    // 可以随机生成double元素的流
    static final class RandomDoublesSpliterator implements Spliterator.OfDouble {
        final Random rng;
        
        // 随机数数量：fence-index
        long index;
        final long fence;
        
        // 随机数取值范围：[origin, bound)
        final double origin;
        final double bound;
        
        
        RandomDoublesSpliterator(Random rng, long index, long fence, double origin, double bound) {
            this.rng = rng;
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }
        
        public RandomDoublesSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m<=i) ? null : new RandomDoublesSpliterator(rng, i, index = m, origin, bound);
        }
        
        public long estimateSize() {
            return fence - index;
        }
        
        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }
        
        public boolean tryAdvance(DoubleConsumer consumer) {
            if(consumer == null)
                throw new NullPointerException();
            long i = index, f = fence;
            if(i<f) {
                consumer.accept(rng.internalNextDouble(origin, bound));
                index = i + 1;
                return true;
            }
            return false;
        }
        
        public void forEachRemaining(DoubleConsumer consumer) {
            if(consumer == null)
                throw new NullPointerException();
            long i = index, f = fence;
            if(i<f) {
                index = f;
                Random r = rng;
                double o = origin, b = bound;
                do {
                    consumer.accept(r.internalNextDouble(o, b));
                } while(++i<f);
            }
        }
    }
}
