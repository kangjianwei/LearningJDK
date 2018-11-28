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

package java.util.concurrent;

import java.io.ObjectStreamField;
import java.security.AccessControlContext;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;

/**
 * A random number generator isolated to the current thread.  Like the
 * global {@link java.util.Random} generator used by the {@link
 * java.lang.Math} class, a {@code ThreadLocalRandom} is initialized
 * with an internally generated seed that may not otherwise be
 * modified. When applicable, use of {@code ThreadLocalRandom} rather
 * than shared {@code Random} objects in concurrent programs will
 * typically encounter much less overhead and contention.  Use of
 * {@code ThreadLocalRandom} is particularly appropriate when multiple
 * tasks (for example, each a {@link ForkJoinTask}) use random numbers
 * in parallel in thread pools.
 *
 * <p>Usages of this class should typically be of the form:
 * {@code ThreadLocalRandom.current().nextX(...)} (where
 * {@code X} is {@code Int}, {@code Long}, etc).
 * When all usages are of this form, it is never possible to
 * accidentally share a {@code ThreadLocalRandom} across multiple threads.
 *
 * <p>This class also provides additional commonly used bounded random
 * generation methods.
 *
 * <p>Instances of {@code ThreadLocalRandom} are not cryptographically
 * secure.  Consider instead using {@link java.security.SecureRandom}
 * in security-sensitive applications. Additionally,
 * default-constructed instances do not use a cryptographically random
 * seed unless the {@linkplain System#getProperty system property}
 * {@code java.util.secureRandomSeed} is set to {@code true}.
 *
 * @author Doug Lea
 * @since 1.7
 */
/*
 * 伪随机数生成器，Random的子类
 *
 * 线程安全
 * 适用于多线程同步场景
 *
 * 在多线程中，相比Random，该伪随机数生成器的性能更好
 *
 *   支持使用内置种子计算的原始种子
 * 不支持自定义原始种子
 *   支持辅助种子
 *   支持使用安全种子（设置运行参数-Djava.util.secureRandomSeed=true）
 *
 * 注：虽然类名带有ThreadLocal字样，但跟ThreadLocal类几乎无关
 */
public class ThreadLocalRandom extends Random {
    /*
     * This class implements the java.util.Random API (and subclasses
     * Random) using a single static instance that accesses random
     * number state held in class Thread (primarily, field
     * threadLocalRandomSeed). In doing so, it also provides a home
     * for managing package-private utilities that rely on exactly the
     * same state as needed to maintain the ThreadLocalRandom
     * instances. We leverage the need for an initialization flag
     * field to also use it as a "probe" -- a self-adjusting thread
     * hash used for contention avoidance, as well as a secondary
     * simpler (xorShift) random seed that is conservatively used to
     * avoid otherwise surprising users by hijacking the
     * ThreadLocalRandom sequence.  The dual use is a marriage of
     * convenience, but is a simple and efficient way of reducing
     * application-level overhead and footprint of most concurrent
     * programs. Even more opportunistically, we also define here
     * other package-private utilities that access Thread class
     * fields.
     *
     * Even though this class subclasses java.util.Random, it uses the
     * same basic algorithm as java.util.SplittableRandom.  (See its
     * internal documentation for explanations, which are not repeated
     * here.)  Because ThreadLocalRandoms are not splittable
     * though, we use only a single 64bit gamma.
     *
     * Because this class is in a different package than class Thread,
     * field access methods use Unsafe to bypass access control rules.
     * To conform to the requirements of the Random superclass
     * constructor, the common static ThreadLocalRandom maintains an
     * "initialized" field for the sake of rejecting user calls to
     * setSeed while still allowing a call from constructor.  Note
     * that serialization is completely unnecessary because there is
     * only a static singleton.  But we generate a serial form
     * containing "rnd" and "initialized" fields to ensure
     * compatibility across versions.
     *
     * Implementations of non-core methods are mostly the same as in
     * SplittableRandom, that were in part derived from a previous
     * version of this class.
     *
     * The nextLocalGaussian ThreadLocal supports the very rarely used
     * nextGaussian method by providing a holder for the second of a
     * pair of them. As is true for the base class version of this
     * method, this time/space tradeoff is probably never worthwhile,
     * but we provide identical statistical properties.
     */
    
    private static final long serialVersionUID = -5851777807851030925L;
    /**
     * @serialField rnd long
     * seed for random computations
     * @serialField initialized boolean
     * always true
     */
    // 确定哪些字段参与序列化
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("rnd", long.class),
        new ObjectStreamField("initialized", boolean.class)
    };
    
    // IllegalArgumentException messages
    static final String BAD_BOUND = "bound must be positive";
    static final String BAD_RANGE = "bound must be greater than origin";
    static final String BAD_SIZE = "size must be non-negative";
    
    
    /*▼ 内置种子[共享] ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /*
     * 内置种子由多个线程共享
     * 每个线程都会以这个内置种子为蓝本，生成一个线程专有的原始种子值（参见SEED）
     * 而且在这个过程中，会将这里的内置种子进行原子地改变
     */
    
    /**
     * The increment of seeder per new instance.
     */
    // 哈希魔数，内置种子的增量
    private static final long SEEDER_INCREMENT = 0xbb67ae8584caa73bL;
    /**
     * The next seed for default constructors.
     */
    /*
     * 内置种子
     *
     * ThreadLocalRandom实例使用该种子生成一个线程专有的原始种子值和线程专有的辅助种子值
     *
     * 内置种子的初值只能由系统时间生成
     * 每个线程初次获取ThreadLocalRandom实例时，会将内置种子更新一次
     * 更新后的值会作为该线程专有的原始种子值
     */
    private static final AtomicLong seeder = new AtomicLong(mix64(System.currentTimeMillis()) ^ mix64(System.nanoTime()));
    
    /*▲ 内置种子[共享] ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    /*▼ 原始种子 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /*
     * 与Random不同的是，Random中原始种子是被各线程共享的
     * 但是在ThreadLocalRandom中，每个线程都有自己的原始种子
     * 且原始种子的初值不一样
     * 这个原始种子生成自哈希魔数，利用它可以产生均匀的哈希值作为随机数
     */
    
    /**
     * The seed increment.
     */
    // 哈希魔数，用作线程内原始种子的增量。每使用next()生成一个随机数，就将线程内的原始种子更新一次
    private static final long GAMMA = 0x9e3779b97f4a7c15L;
    
    /*▲ 原始种子 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    /*▼ 辅助种子 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /*▲ 辅助种子 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    /*▼ 探测值[共享] ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /*
     * 这里的探测值由多个线程共享
     * 每个线程都会以这个探测值为蓝本，生成一个线程专有的探测值（参见PROBE）
     * 并且在这个过程中，会将这里的探测值进行原子地改变
     */
    
    /** Generates per-thread initialization/probe field */
    // 探测值，初值为0
    private static final AtomicInteger probeGenerator = new AtomicInteger();
    /**
     * The increment for generating probe values.
     */
    // 哈希魔数，探测值增量
    private static final int PROBE_INCREMENT = 0x9e3779b9;
    
    /*▲ 探测值[共享] ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    /**
     * The least non-zero value returned by nextDouble().
     * This value is scaled by a random value of 53 bits to produce a result.
     */
    // double值的二进制精度
    private static final double DOUBLE_UNIT = 0x1.0p-53;  // 1.0  / (1L << 53)
    // float值的二进制精度
    private static final float FLOAT_UNIT   = 0x1.0p-24f; // 1.0f / (1 << 24)
    
    /** Rarely-used holder for the second of a pair of Gaussians */
    private static final ThreadLocal<Double> nextLocalGaussian = new ThreadLocal<>();
    
    
    /** The common ThreadLocalRandom */
    // 由各线程共享的伪随机数实例，但是为每个线程生成的随机数种子是不一样的
    static final ThreadLocalRandom instance = new ThreadLocalRandom();
    
    /**
     * Field used only during singleton initialization.
     * True when constructor completes.
     */
    // 标记ThreadLocalRandom实例instance是否已经创建
    boolean initialized;
    
    
    // Unsafe mechanics
    private static final Unsafe U = Unsafe.getUnsafe();
    
    // 当前线程内的【原始种子】，用来为当前线程生成均匀的随机数
    private static final long SEED = U.objectFieldOffset(Thread.class, "threadLocalRandomSeed");
    // 当前线程内的【辅助种子】
    private static final long SECONDARY = U.objectFieldOffset(Thread.class, "threadLocalRandomSecondarySeed");
    // 当前线程内的【探测值】
    private static final long PROBE = U.objectFieldOffset(Thread.class, "threadLocalRandomProbe");
    
    private static final long THREADLOCALS = U.objectFieldOffset(Thread.class, "threadLocals");
    private static final long INHERITABLETHREADLOCALS = U.objectFieldOffset(Thread.class, "inheritableThreadLocals");
    private static final long INHERITEDACCESSCONTROLCONTEXT = U.objectFieldOffset(Thread.class, "inheritedAccessControlContext");
    
    
    
    /* at end of <clinit> to survive static initialization circularity */
    // 在构造方法被调用前，设置一个安全的初始种子，即原始种子不再使用系统时间计算
    static {
        String sec = VM.getSavedProperty("java.util.secureRandomSeed");
        if(Boolean.parseBoolean(sec)) {
            byte[] seedBytes = SecureRandom.getSeed(8);
            long s = (long) seedBytes[0] & 0xffL;
            for(int i = 1; i<8; ++i) {
                s = (s << 8) | ((long) seedBytes[i] & 0xffL);
            }
            seeder.set(s);
        }
    }
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Constructor used only for static singleton */
    // 不支持主动调用构造方法
    private ThreadLocalRandom() {
        initialized = true; // false during super() call
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 种子 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current thread's {@code ThreadLocalRandom}.
     *
     * @return the current thread's {@code ThreadLocalRandom}
     */
    // 获取当前线程中的ThreadLocalRandom，如有必要，需要完成种子的初始化工作
    public static ThreadLocalRandom current() {
        // 如果当前线程内的探测值为0，则需要进行一些初始化工作
        if(U.getInt(Thread.currentThread(), PROBE) == 0) {
            // 为当前线程设置原始种子值和探测值
            localInit();
        }
        
        // 现在可以返回共享的伪随机数生成器了
        return instance;
    }
    
    /**
     * Initialize Thread fields for the current thread.  Called only
     * when Thread.threadLocalRandomProbe is zero, indicating that a
     * thread local seed value needs to be generated. Note that even
     * though the initialization is purely thread-local, we need to
     * rely on (static) atomic generators to initialize the values.
     */
    // 为当前线程设置原始种子值和探测值
    static final void localInit() {
        Thread thread = Thread.currentThread();
        
        // 原子地增加原始种子[共享]的值
        long seed = mix64(seeder.getAndAdd(SEEDER_INCREMENT));
        // 为当前线程初始化原始种子
        U.putLong(thread, SEED, seed);
        
        // 原子地增加探测值[共享]
        int p = probeGenerator.addAndGet(PROBE_INCREMENT);
        int probe = (p == 0) ? 1 : p; // skip 0
        
        // 为当前线程初始化探测值
        U.putInt(thread, PROBE, probe);
    }
    
    /**
     * Throws {@code UnsupportedOperationException}.  Setting seeds in
     * this generator is not supported.
     *
     * @throws UnsupportedOperationException always
     */
    // ThreadLocalRandom的实例一旦创建，就禁止自行设置种子
    public void setSeed(long seed) {
        // only allow call from super() constructor
        if(initialized) {
            throw new UnsupportedOperationException();
        }
    }
    
    // 更新当前线程内的原始种子，并返回更新后的值
    final long nextSeed() {
        Thread thread = Thread.currentThread();
        long r= U.getLong(thread, SEED) + GAMMA;
        U.putLong(thread, SEED, r);
        return r;
    }
    
    /**
     * Returns the pseudo-randomly initialized or updated secondary seed.
     */
    // 获取下一个辅助种子
    static final int nextSecondarySeed() {
        int r;
        
        Thread t = Thread.currentThread();
        
        // 获取辅助种子的值
        r = U.getInt(t, SECONDARY);
        
        // 如果已经设置过辅助种子，则更新它
        if(r != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        } else {
            // 如果辅助种子还未设置，使用内置种子初始化它
            r = mix32(seeder.getAndAdd(SEEDER_INCREMENT));
            if(r==0) {
                r = 1; // avoid zero
            }
        }
        
        U.putInt(t, SECONDARY, r);
        
        return r;
    }
    
    /*▲ 种子 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 生成伪随机数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a pseudorandom {@code int} value.
     *
     * @return a pseudorandom {@code int} value
     */
    // 随机生成一个int值，有正有负
    public int nextInt() {
        return mix32(nextSeed());
    }
    
    /**
     * Returns a pseudorandom {@code int} value between zero (inclusive) and the specified bound (exclusive).
     *
     * @param bound the upper bound (exclusive).  Must be positive.
     *
     * @return a pseudorandom {@code int} value between zero
     * (inclusive) and the bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    // 随机生成一个[0, bound)之内的int值
    public int nextInt(int bound) {
        if(bound<=0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        int r = mix32(nextSeed());
        int m = bound - 1;
        if((bound & m) == 0) { // power of two
            r &= m;
        } else { // reject over-represented candidates
            for(int u = r >>> 1; u + m - (r = u % bound)<0; u = mix32(nextSeed()) >>> 1)
                ;
        }
        return r;
    }
    
    /**
     * Returns a pseudorandom {@code int} value between the specified origin (inclusive) and the specified bound (exclusive).
     *
     * @param origin the least value returned
     * @param bound  the upper bound (exclusive)
     *
     * @return a pseudorandom {@code int} value between the origin
     * (inclusive) and the bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code origin} is greater than
     *                                  or equal to {@code bound}
     */
    // 随机生成一个[origin, bound)之内的int值，有正有负
    public int nextInt(int origin, int bound) {
        if(origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return internalNextInt(origin, bound);
    }
    
    /**
     * Returns a pseudorandom {@code long} value.
     *
     * @return a pseudorandom {@code long} value
     */
    // 随机生成一个long值，有正有负
    public long nextLong() {
        return mix64(nextSeed());
    }
    
    /**
     * Returns a pseudorandom {@code long} value between zero (inclusive)
     * and the specified bound (exclusive).
     *
     * @param bound the upper bound (exclusive).  Must be positive.
     *
     * @return a pseudorandom {@code long} value between zero
     * (inclusive) and the bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    // 随机生成一个[0, bound)之内的long值
    public long nextLong(long bound) {
        if(bound<=0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        long r = mix64(nextSeed());
        long m = bound - 1;
        if((bound & m) == 0L) { // power of two
            r &= m;
        } else { // reject over-represented candidates
            for(long u = r >>> 1; u + m - (r = u % bound)<0L; u = mix64(nextSeed()) >>> 1)
                ;
        }
        return r;
    }
    
    /**
     * Returns a pseudorandom {@code long} value between the specified
     * origin (inclusive) and the specified bound (exclusive).
     *
     * @param origin the least value returned
     * @param bound  the upper bound (exclusive)
     *
     * @return a pseudorandom {@code long} value between the origin
     * (inclusive) and the bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code origin} is greater than
     *                                  or equal to {@code bound}
     */
    // 随机生成一个[origin, bound)之内的long值，有正有负
    public long nextLong(long origin, long bound) {
        if(origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return internalNextLong(origin, bound);
    }
    
    /**
     * Returns a pseudorandom {@code float} value between zero
     * (inclusive) and one (exclusive).
     *
     * @return a pseudorandom {@code float} value between zero
     * (inclusive) and one (exclusive)
     */
    // 随机生成一个[0, 1)之内的float值
    public float nextFloat() {
        return (mix32(nextSeed()) >>> 8) * FLOAT_UNIT;
    }
    
    /**
     * Returns a pseudorandom {@code double} value between zero
     * (inclusive) and one (exclusive).
     *
     * @return a pseudorandom {@code double} value between zero
     * (inclusive) and one (exclusive)
     */
    // 随机生成一个[0, 1)之内的double值
    public double nextDouble() {
        return (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT;
    }
    
    /**
     * Returns a pseudorandom {@code double} value between 0.0
     * (inclusive) and the specified bound (exclusive).
     *
     * @param bound the upper bound (exclusive).  Must be positive.
     *
     * @return a pseudorandom {@code double} value between zero
     * (inclusive) and the bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    // 随机生成一个[0, bound)之内的double值
    public double nextDouble(double bound) {
        if(!(bound>0.0)) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
        double result = (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT * bound;
        return (result<bound)
            ? result  // correct for rounding
            : Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
    }
    
    /**
     * Returns a pseudorandom {@code double} value between the specified
     * origin (inclusive) and bound (exclusive).
     *
     * @param origin the least value returned
     * @param bound  the upper bound (exclusive)
     *
     * @return a pseudorandom {@code double} value between the origin
     * (inclusive) and the bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code origin} is greater than
     *                                  or equal to {@code bound}
     */
    // 随机生成一个[origin, bound)之内的double值，有正有负
    public double nextDouble(double origin, double bound) {
        if(!(origin<bound)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return internalNextDouble(origin, bound);
    }
    
    /**
     * Returns a pseudorandom {@code boolean} value.
     *
     * @return a pseudorandom {@code boolean} value
     */
    // 随机生成一个boolean值
    public boolean nextBoolean() {
        return mix32(nextSeed())<0;
    }
    
    // 随机生成一个double值，有正有负。所有生成的double值符合标准正态分布
    public double nextGaussian() {
        // Use nextLocalGaussian instead of nextGaussian field
        Double d = nextLocalGaussian.get();
        if(d != null) {
            nextLocalGaussian.set(null);
            return d;
        }
        double v1, v2, s;
        do {
            v1 = 2 * nextDouble() - 1; // between -1 and 1
            v2 = 2 * nextDouble() - 1; // between -1 and 1
            s = v1 * v1 + v2 * v2;
        } while(s >= 1 || s == 0);
        double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        nextLocalGaussian.set(v2 * multiplier);
        return v1 * multiplier;
    }
    
    /**
     * Generates a pseudorandom number with the indicated number of low-order bits.
     * Because this class has no subclasses, this method cannot be invoked or overridden.
     *
     * @param bits random bits
     *
     * @return the next pseudorandom value from this random number
     * generator's sequence
     */
    // 随机生成一个int值，该值范围是[0, 2^bits -1)
    protected int next(int bits) {
        return nextInt() >>> (32 - bits);
    }
    
    /*▲ 生成伪随机数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 流 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code int}
     * values.
     *
     * @return a stream of pseudorandom {@code int} values
     *
     * @implNote This method is implemented to be equivalent to {@code
     * ints(Long.MAX_VALUE)}.
     * @since 1.8
     */
    // 返回的流可以生成Long.MAX_VALUE个随机int值
    public IntStream ints() {
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, Long.MAX_VALUE, Integer.MAX_VALUE, 0), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code int} values.
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
            throw new IllegalArgumentException(BAD_SIZE);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, streamSize, Integer.MAX_VALUE, 0), false);
    }
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * int} values, each conforming to the given origin (inclusive) and bound
     * (exclusive).
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
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number
     * of pseudorandom {@code int} values, each conforming to the given
     * origin (inclusive) and bound (exclusive).
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
            throw new IllegalArgumentException(BAD_SIZE);
        }
        if(randomNumberOrigin >= randomNumberBound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.intStream(new RandomIntsSpliterator(0L, streamSize, randomNumberOrigin, randomNumberBound), false);
    }
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code long}
     * values.
     *
     * @return a stream of pseudorandom {@code long} values
     *
     * @implNote This method is implemented to be equivalent to {@code
     * longs(Long.MAX_VALUE)}.
     * @since 1.8
     */
    // 返回的流可以生成Long.MAX_VALUE个随机long值
    public LongStream longs() {
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, Long.MAX_VALUE, Long.MAX_VALUE, 0L), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code long} values.
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
            throw new IllegalArgumentException(BAD_SIZE);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, streamSize, Long.MAX_VALUE, 0L), false);
    }
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * long} values, each conforming to the given origin (inclusive) and bound
     * (exclusive).
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
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code long}, each conforming to the given origin
     * (inclusive) and bound (exclusive).
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
            throw new IllegalArgumentException(BAD_SIZE);
        }
        if(randomNumberOrigin >= randomNumberBound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.longStream(new RandomLongsSpliterator(0L, streamSize, randomNumberOrigin, randomNumberBound), false);
    }
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * double} values, each between zero (inclusive) and one
     * (exclusive).
     *
     * @return a stream of pseudorandom {@code double} values
     *
     * @implNote This method is implemented to be equivalent to {@code
     * doubles(Long.MAX_VALUE)}.
     * @since 1.8
     */
    // 返回的流可以生成Long.MAX_VALUE个随机double值，取值范围是[0, 1)
    public DoubleStream doubles() {
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, Long.MAX_VALUE, Double.MAX_VALUE, 0.0), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code double} values, each between zero
     * (inclusive) and one (exclusive).
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
            throw new IllegalArgumentException(BAD_SIZE);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, streamSize, Double.MAX_VALUE, 0.0), false);
    }
    
    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * double} values, each conforming to the given origin (inclusive) and bound
     * (exclusive).
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
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound), false);
    }
    
    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code double} values, each conforming to the given origin
     * (inclusive) and bound (exclusive).
     *
     * @param streamSize         the number of values to generate
     * @param randomNumberOrigin the origin (inclusive) of each random value
     * @param randomNumberBound  the bound (exclusive) of each random value
     *
     * @return a stream of pseudorandom {@code double} values,
     * each with the given origin (inclusive) and bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code streamSize} is
     *                                  less than zero, or {@code randomNumberOrigin}
     *                                  is greater than or equal to {@code randomNumberBound}
     * @since 1.8
     */
    // 返回的流可以生成streamSize个随机double值，取值范围是[randomNumberOrigin, randomNumberBound)
    public DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
        if(streamSize<0L) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        if(!(randomNumberOrigin<randomNumberBound)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
        return StreamSupport.doubleStream(new RandomDoublesSpliterator(0L, streamSize, randomNumberOrigin, randomNumberBound), false);
    }
    
    /*▲ 流 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the {@link #current() current} thread's {@code ThreadLocalRandom}.
     *
     * @return the {@link #current() current} thread's {@code ThreadLocalRandom}
     */
    private Object readResolve() {
        return current();
    }
    
    /**
     * Saves the {@code ThreadLocalRandom} to a stream (that is, serializes it).
     *
     * @param s the stream
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        
        java.io.ObjectOutputStream.PutField fields = s.putFields();
        fields.put("rnd", U.getLong(Thread.currentThread(), SEED));
        fields.put("initialized", true);
        s.writeFields();
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * The form of nextInt used by IntStream Spliterators.
     * Exactly the same as long version, except for types.
     *
     * @param origin the least value, unless greater than bound
     * @param bound  the upper bound (exclusive), must not equal origin
     *
     * @return a pseudorandom value
     */
    // 随机生成一个[origin, bound)之内的int值
    final int internalNextInt(int origin, int bound) {
        int r = mix32(nextSeed());
        if(origin<bound) {
            int n = bound - origin, m = n - 1;
            if((n & m) == 0)
                r = (r & m) + origin;
            else if(n>0) {
                for(int u = r >>> 1; u + m - (r = u % n)<0; u = mix32(nextSeed()) >>> 1)
                    ;
                r += origin;
            } else {
                while(r<origin || r >= bound)
                    r = mix32(nextSeed());
            }
        }
        return r;
    }
    
    /**
     * The form of nextLong used by LongStream Spliterators.  If
     * origin is greater than bound, acts as unbounded form of
     * nextLong, else as bounded form.
     *
     * @param origin the least value, unless greater than bound
     * @param bound  the upper bound (exclusive), must not equal origin
     *
     * @return a pseudorandom value
     */
    // 随机生成一个[origin, bound)之内的long值
    final long internalNextLong(long origin, long bound) {
        long r = mix64(nextSeed());
        if(origin<bound) {
            long n = bound - origin, m = n - 1;
            if((n & m) == 0L)  // power of two
                r = (r & m) + origin;
            else if(n>0L) {  // reject over-represented candidates
                for(long u = r >>> 1;            // ensure nonnegative
                    u + m - (r = u % n)<0L;    // rejection check
                    u = mix64(nextSeed()) >>> 1) // retry
                    ;
                r += origin;
            } else {              // range not representable as long
                while(r<origin || r >= bound)
                    r = mix64(nextSeed());
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
        double r = (nextLong() >>> 11) * DOUBLE_UNIT;
        if(origin<bound) {
            r = r * (bound - origin) + origin;
            if(r >= bound) // correct for rounding
                r = Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
        }
        return r;
    }
    
    
    /**
     * Erases ThreadLocals by nulling out Thread maps.
     */
    static final void eraseThreadLocals(Thread thread) {
        U.putObject(thread, THREADLOCALS, null);
        U.putObject(thread, INHERITABLETHREADLOCALS, null);
    }
    
    static final void setInheritedAccessControlContext(Thread thread, AccessControlContext acc) {
        U.putObjectRelease(thread, INHERITEDACCESSCONTROLCONTEXT, acc);
    }
    
    /*
     * Descriptions of the usages of the methods below can be found in the classes that use them.
     * Briefly, a thread's "probe" value is a non-zero hash code that (probably) does not collide with other existing threads
     * with respect to any power of two collision space.
     * When it does collide, it is pseudo-randomly adjusted (using a Marsaglia XorShift).
     * The nextSecondarySeed method is used in the same contexts as ThreadLocalRandom,
     * but only for transient usages such as random adaptive spin/block sequences for
     * which a cheap RNG suffices and for which it could in principle disrupt user-visible statistical properties
     * of the main ThreadLocalRandom if we were to use it.
     *
     * Note: Because of package-protection issues, versions of some these methods also appear in some subpackage classes.
     */
    
    /**
     * Returns the probe value for the current thread without forcing
     * initialization. Note that invoking ThreadLocalRandom.current()
     * can be used to force initialization on zero return.
     */
    // 获取当前线程内的【探测值】
    static final int getProbe() {
        return U.getInt(Thread.currentThread(), PROBE);
    }
    
    /**
     * Pseudo-randomly advances and records the given probe value for the given thread.
     */
    // 更新探测值，并返回更新后的值
    static final int advanceProbe(int probe) {
        // 由形参计算出一个哈希魔数
        probe ^= probe << 13;   // xorshift
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        U.putInt(Thread.currentThread(), PROBE, probe);
        return probe;
    }
    
    private static int mix32(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        return (int) (((z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L) >>> 32);
    }
    
    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
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
    private static final class RandomIntsSpliterator implements Spliterator.OfInt {
        // 随机数数量：fence-index
        long   index;
        final long   fence;
        
        // 随机数取值范围：[origin, bound)
        final int    origin;
        final int    bound;
        
        RandomIntsSpliterator(long index, long fence, int origin, int bound) {
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }
        
        public RandomIntsSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m<=i) ? null : new RandomIntsSpliterator(i, index = m, origin, bound);
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
                consumer.accept(ThreadLocalRandom.current().internalNextInt(origin, bound));
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
                int o = origin, b = bound;
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                do {
                    consumer.accept(rng.internalNextInt(o, b));
                } while(++i<f);
            }
        }
    }
    
    /**
     * Spliterator for long streams.
     */
    // 可以随机生成long元素的流
    private static final class RandomLongsSpliterator implements Spliterator.OfLong {
        // 随机数数量：fence-index
        long index;
        final long fence;
        
        // 随机数取值范围：[origin, bound)
        final long origin;
        final long bound;
        
        RandomLongsSpliterator(long index, long fence, long origin, long bound) {
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }
        
        public RandomLongsSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m<=i) ? null : new RandomLongsSpliterator(i, index = m, origin, bound);
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
                consumer.accept(ThreadLocalRandom.current().internalNextLong(origin, bound));
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
                long o = origin, b = bound;
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                do {
                    consumer.accept(rng.internalNextLong(o, b));
                } while(++i<f);
            }
        }
        
    }
    
    /**
     * Spliterator for double streams.
     */
    // 可以随机生成double元素的流
    private static final class RandomDoublesSpliterator implements Spliterator.OfDouble {
        // 随机数数量：fence-index
        long index;
        final long fence;
        
        // 随机数取值范围：[origin, bound)
        final double origin;
        final double bound;
        
        RandomDoublesSpliterator(long index, long fence, double origin, double bound) {
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }
        
        public RandomDoublesSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m<=i) ? null : new RandomDoublesSpliterator(i, index = m, origin, bound);
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
                consumer.accept(ThreadLocalRandom.current().internalNextDouble(origin, bound));
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
                double o = origin, b = bound;
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                do {
                    consumer.accept(rng.internalNextDouble(o, b));
                } while(++i<f);
            }
        }
    }
}
