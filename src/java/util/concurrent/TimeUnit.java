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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

/**
 * A {@code TimeUnit} represents time durations at a given unit of
 * granularity and provides utility methods to convert across units,
 * and to perform timing and delay operations in these units.  A
 * {@code TimeUnit} does not maintain time information, but only
 * helps organize and use time representations that may be maintained
 * separately across various contexts.  A nanosecond is defined as one
 * thousandth of a microsecond, a microsecond as one thousandth of a
 * millisecond, a millisecond as one thousandth of a second, a minute
 * as sixty seconds, an hour as sixty minutes, and a day as twenty four
 * hours.
 *
 * <p>A {@code TimeUnit} is mainly used to inform time-based methods
 * how a given timing parameter should be interpreted. For example,
 * the following code will timeout in 50 milliseconds if the {@link
 * java.util.concurrent.locks.Lock lock} is not available:
 *
 * <pre> {@code
 * Lock lock = ...;
 * if (lock.tryLock(50L, TimeUnit.MILLISECONDS)) ...}</pre>
 *
 * while this code will timeout in 50 seconds:
 * <pre> {@code
 * Lock lock = ...;
 * if (lock.tryLock(50L, TimeUnit.SECONDS)) ...}</pre>
 *
 * Note however, that there is no guarantee that a particular timeout
 * implementation will be able to notice the passage of time at the
 * same granularity as the given {@code TimeUnit}.
 *
 * @since 1.5
 * @author Doug Lea
 */
// 时间单位转换工具
public enum TimeUnit {
    /**
     * Time unit representing one thousandth of a microsecond.
     */
    NANOSECONDS(TimeUnit.NANO_SCALE),   // 纳秒
    
    /**
     * Time unit representing one thousandth of a millisecond.
     */
    MICROSECONDS(TimeUnit.MICRO_SCALE), // 微秒
    
    /**
     * Time unit representing one thousandth of a second.
     */
    MILLISECONDS(TimeUnit.MILLI_SCALE), // 毫秒
    
    /**
     * Time unit representing one second.
     */
    SECONDS(TimeUnit.SECOND_SCALE),     // 秒
    
    /**
     * Time unit representing sixty seconds.
     * @since 1.6
     */
    MINUTES(TimeUnit.MINUTE_SCALE),     // 分
    
    /**
     * Time unit representing sixty minutes.
     * @since 1.6
     */
    HOURS(TimeUnit.HOUR_SCALE),         // 时
    
    /**
     * Time unit representing twenty four hours.
     * @since 1.6
     */
    DAYS(TimeUnit.DAY_SCALE);           // 天
    
    // 缩放因子
    private static final long NANO_SCALE   = 1L;
    private static final long MICRO_SCALE  = 1000L * NANO_SCALE;
    private static final long MILLI_SCALE  = 1000L * MICRO_SCALE;
    private static final long SECOND_SCALE = 1000L * MILLI_SCALE;
    private static final long MINUTE_SCALE = 60L * SECOND_SCALE;
    private static final long HOUR_SCALE   = 60L * MINUTE_SCALE;
    private static final long DAY_SCALE    = 24L * HOUR_SCALE;
    
    /*
     * Instances cache conversion ratios and saturation cutoffs for the units up through SECONDS.
     * Other cases compute them, in method cvt.
     */
    
    private final long scale;   // 缩放因子
    
    private final long maxNanos;    // 当前时间单位下可以表示的最大纳秒数
    private final long maxMicros;   // 当前时间单位下可以表示的最大微秒数
    private final long maxMillis;   // 当前时间单位下可以表示的最大毫秒数
    private final long maxSecs;     // 当前时间单位下可以表示的最大  秒数
    
    private final long microRatio;  // 微秒与当前时间单位的换算比例
    private final int milliRatio;   // 毫秒与当前时间单位的换算比例
    private final int secRatio;     //   秒与当前时间单位的换算比例
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private TimeUnit(long s) {
        this.scale = s;
        this.maxNanos = Long.MAX_VALUE / s;
        long ur = (s >= MICRO_SCALE) ? (s / MICRO_SCALE) : (MICRO_SCALE / s);
        this.microRatio = ur;
        this.maxMicros = Long.MAX_VALUE / ur;
        long mr = (s >= MILLI_SCALE) ? (s / MILLI_SCALE) : (MILLI_SCALE / s);
        this.milliRatio = (int)mr;
        this.maxMillis = Long.MAX_VALUE / mr;
        long sr = (s >= SECOND_SCALE) ? (s / SECOND_SCALE) : (SECOND_SCALE / s);
        this.secRatio = (int)sr;
        this.maxSecs = Long.MAX_VALUE / sr;
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间换算 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Equivalent to
     * {@link #convert(long, TimeUnit) NANOSECONDS.convert(duration, this)}.
     *
     * @param duration the duration
     *
     * @return the converted duration,
     * or {@code Long.MIN_VALUE} if conversion would negatively overflow,
     * or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    // 将当前时间单位下的duration换算为纳秒
    public long toNanos(long duration) {
        
        if(scale == NANO_SCALE) {
            return duration;
        }
        
        if(duration>maxNanos) {
            return Long.MAX_VALUE;
        }
        
        if(duration<-maxNanos) {
            return Long.MIN_VALUE;
        }
        
        return duration * scale;
    }
    
    /**
     * Equivalent to
     * {@link #convert(long, TimeUnit) MICROSECONDS.convert(duration, this)}.
     *
     * @param duration the duration
     *
     * @return the converted duration,
     * or {@code Long.MIN_VALUE} if conversion would negatively overflow,
     * or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    // 将当前时间单位下的duration换算为微秒
    public long toMicros(long duration) {
        
        if(scale<=MICRO_SCALE) {
            return (scale == MICRO_SCALE) ? duration : duration / microRatio;
        }
        
        if(duration>maxMicros) {
            return Long.MAX_VALUE;
        }
        
        if(duration<-maxMicros) {
            return Long.MIN_VALUE;
        }
        
        return duration * microRatio;
    }
    
    /**
     * Equivalent to
     * {@link #convert(long, TimeUnit) MILLISECONDS.convert(duration, this)}.
     *
     * @param duration the duration
     *
     * @return the converted duration,
     * or {@code Long.MIN_VALUE} if conversion would negatively overflow,
     * or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    // 将当前时间单位下的duration换算为毫秒
    public long toMillis(long duration) {
        
        if(scale<=MILLI_SCALE) {
            return (scale == MILLI_SCALE) ? duration : duration / milliRatio;
        }
        
        if(duration>maxMillis) {
            return Long.MAX_VALUE;
        }
        
        if(duration<-maxMillis) {
            return Long.MIN_VALUE;
        }
        
        return duration * milliRatio;
    }
    
    /**
     * Equivalent to
     * {@link #convert(long, TimeUnit) SECONDS.convert(duration, this)}.
     *
     * @param duration the duration
     *
     * @return the converted duration,
     * or {@code Long.MIN_VALUE} if conversion would negatively overflow,
     * or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    // 将当前时间单位下的duration换算为秒
    public long toSeconds(long duration) {
        
        if(scale<=SECOND_SCALE) {
            return (scale == SECOND_SCALE) ? duration : duration / secRatio;
        }
        
        if(duration>maxSecs) {
            return Long.MAX_VALUE;
        }
        
        if(duration<-maxSecs) {
            return Long.MIN_VALUE;
        }
        
        return duration * secRatio;
    }
    
    /**
     * Equivalent to
     * {@link #convert(long, TimeUnit) MINUTES.convert(duration, this)}.
     *
     * @param duration the duration
     *
     * @return the converted duration,
     * or {@code Long.MIN_VALUE} if conversion would negatively overflow,
     * or {@code Long.MAX_VALUE} if it would positively overflow.
     *
     * @since 1.6
     */
    // 将当前时间单位下的duration换算为分
    public long toMinutes(long duration) {
        return cvt(duration, MINUTE_SCALE, scale);
    }
    
    /**
     * Equivalent to
     * {@link #convert(long, TimeUnit) HOURS.convert(duration, this)}.
     *
     * @param duration the duration
     *
     * @return the converted duration,
     * or {@code Long.MIN_VALUE} if conversion would negatively overflow,
     * or {@code Long.MAX_VALUE} if it would positively overflow.
     *
     * @since 1.6
     */
    // 将当前时间单位下的duration换算为时
    public long toHours(long duration) {
        return cvt(duration, HOUR_SCALE, scale);
    }
    
    /**
     * Equivalent to
     * {@link #convert(long, TimeUnit) DAYS.convert(duration, this)}.
     *
     * @param duration the duration
     *
     * @return the converted duration
     *
     * @since 1.6
     */
    // 将当前时间单位下的duration换算为天
    public long toDays(long duration) {
        return cvt(duration, DAY_SCALE, scale);
    }
    
    /**
     * Converts the given time duration in the given unit to this unit.
     * Conversions from finer to coarser granularities truncate, so
     * lose precision. For example, converting {@code 999} milliseconds
     * to seconds results in {@code 0}. Conversions from coarser to
     * finer granularities with arguments that would numerically
     * overflow saturate to {@code Long.MIN_VALUE} if negative or
     * {@code Long.MAX_VALUE} if positive.
     *
     * <p>For example, to convert 10 minutes to milliseconds, use:
     * {@code TimeUnit.MILLISECONDS.convert(10L, TimeUnit.MINUTES)}
     *
     * @param sourceDuration the time duration in the given {@code sourceUnit}
     * @param sourceUnit the unit of the {@code sourceDuration} argument
     * @return the converted duration in this unit,
     * or {@code Long.MIN_VALUE} if conversion would negatively overflow,
     * or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    // 将sourceUnit时间单位下的sourceDuration时间转换为当前时间单位下的时间
    public long convert(long sourceDuration, TimeUnit sourceUnit) {
        switch (this) {
            case NANOSECONDS:  return sourceUnit.toNanos(sourceDuration);
            case MICROSECONDS: return sourceUnit.toMicros(sourceDuration);
            case MILLISECONDS: return sourceUnit.toMillis(sourceDuration);
            case SECONDS:      return sourceUnit.toSeconds(sourceDuration);
            default: return cvt(sourceDuration, scale, sourceUnit.scale);
        }
    }
    
    /**
     * General conversion utility.
     *
     * @param d duration
     * @param dst result unit scale
     * @param src source unit scale
     */
    // 将src时间单位下的d时间转换为dst时间单位下的时间
    private static long cvt(long d, long dst, long src) {
        
        if (src == dst) {
            return d;
        }
        
        long r = src / dst;
        
        if (src < dst) {
            return d * r;
        }
        
        long m = Long.MAX_VALUE / r;
        
        if (d > m) {
            return Long.MAX_VALUE;
        }
        
        if (d < -m) {
            return Long.MIN_VALUE;
        }
        
        return d * r;
    }
    
    /*▲ 时间换算 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 线程阻塞 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Performs a timed {@link Object#wait(long, int) Object.wait}
     * using this time unit.
     * This is a convenience method that converts timeout arguments
     * into the form required by the {@code Object.wait} method.
     *
     * <p>For example, you could implement a blocking {@code poll} method
     * (see {@link BlockingQueue#poll(long, TimeUnit) BlockingQueue.poll})
     * using:
     *
     * <pre> {@code
     * public E poll(long timeout, TimeUnit unit)
     *     throws InterruptedException {
     *   synchronized (lock) {
     *     while (isEmpty()) {
     *       unit.timedWait(lock, timeout);
     *       ...
     *     }
     *   }
     * }}</pre>
     *
     * @param obj     the object to wait on
     * @param timeout the maximum time to wait. If less than
     *                or equal to zero, do not wait at all.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    // wait()的替代者
    public void timedWait(Object obj, long timeout) throws InterruptedException {
        if(timeout>0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            obj.wait(ms, ns);
        }
    }
    
    /**
     * Performs a timed {@link Thread#join(long, int) Thread.join}
     * using this time unit.
     * This is a convenience method that converts time arguments into the
     * form required by the {@code Thread.join} method.
     *
     * @param thread  the thread to wait for
     * @param timeout the maximum time to wait. If less than
     *                or equal to zero, do not wait at all.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    // join()的替代者
    public void timedJoin(Thread thread, long timeout) throws InterruptedException {
        if(timeout>0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            thread.join(ms, ns);
        }
    }
    
    /**
     * Performs a {@link Thread#sleep(long, int) Thread.sleep} using
     * this time unit.
     * This is a convenience method that converts time arguments into the
     * form required by the {@code Thread.sleep} method.
     *
     * @param timeout the minimum time to sleep. If less than
     *                or equal to zero, do not sleep at all.
     *
     * @throws InterruptedException if interrupted while sleeping
     */
    // sleep()的替代者
    public void sleep(long timeout) throws InterruptedException {
        if(timeout>0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            Thread.sleep(ms, ns);
        }
    }
    
    /**
     * Utility to compute the excess-nanosecond argument to wait,
     * sleep, join.
     *
     * @param d the duration
     * @param m the number of milliseconds
     *
     * @return the number of nanoseconds
     */
    // 计算从纳秒/微秒换算到毫秒时损失的纳秒数
    private int excessNanos(long d, long m) {
        long s;
        if((s = scale) == NANO_SCALE)
            return (int) (d - (m * MILLI_SCALE));
        else if(s == MICRO_SCALE)
            return (int) ((d * 1000L) - (m * MILLI_SCALE));
        else
            return 0;
    }
    
    /*▲ 线程阻塞 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Converts the given time duration to this unit.
     *
     * <p>For any TimeUnit {@code unit},
     * {@code unit.convert(Duration.ofNanos(n))}
     * is equivalent to
     * {@code unit.convert(n, NANOSECONDS)}, and
     * {@code unit.convert(Duration.of(n, unit.toChronoUnit()))}
     * is equivalent to {@code n} (in the absence of overflow).
     *
     * @apiNote
     * This method differs from {@link Duration#toNanos()} in that it
     * does not throw {@link ArithmeticException} on numeric overflow.
     *
     * @param duration the time duration
     * @return the converted duration in this unit,
     * or {@code Long.MIN_VALUE} if conversion would negatively overflow,
     * or {@code Long.MAX_VALUE} if it would positively overflow.
     * @throws NullPointerException if {@code duration} is null
     * @see Duration#of(long, TemporalUnit)
     * @since 11
     */
    // 将Duration时间转换为当前时间单位下的时间
    public long convert(Duration duration) {
        
        long secs = duration.getSeconds();
        int nano = duration.getNano();
        
        if (secs < 0 && nano > 0) {
            // use representation compatible with integer division
            secs++;
            nano -= (int) SECOND_SCALE;
        }
        
        final long s, nanoVal;
        
        // Optimize for the common case - NANOSECONDS without overflow
        if (this == NANOSECONDS)
            nanoVal = nano;
        else if ((s = scale) < SECOND_SCALE)
            nanoVal = nano / s;
        else if (this == SECONDS)
            return secs;
        else
            return secs / secRatio;
        
        long val = secs * secRatio + nanoVal;
        
        return ((secs < maxSecs && secs > -maxSecs)
            || (secs == maxSecs && val > 0)
            || (secs == -maxSecs && val < 0))
            ? val
            : (secs > 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
    }
    
    /**
     * Converts this {@code TimeUnit} to the equivalent {@code ChronoUnit}.
     *
     * @return the converted equivalent ChronoUnit
     * @since 9
     */
    public ChronoUnit toChronoUnit() {
        switch (this) {
            case NANOSECONDS:  return ChronoUnit.NANOS;
            case MICROSECONDS: return ChronoUnit.MICROS;
            case MILLISECONDS: return ChronoUnit.MILLIS;
            case SECONDS:      return ChronoUnit.SECONDS;
            case MINUTES:      return ChronoUnit.MINUTES;
            case HOURS:        return ChronoUnit.HOURS;
            case DAYS:         return ChronoUnit.DAYS;
            default:
                throw new AssertionError();
        }
    }
    
    /**
     * Converts a {@code ChronoUnit} to the equivalent {@code TimeUnit}.
     *
     * @param chronoUnit the ChronoUnit to convert
     * @return the converted equivalent TimeUnit
     * @throws IllegalArgumentException if {@code chronoUnit} has no
     *         equivalent TimeUnit
     * @throws NullPointerException if {@code chronoUnit} is null
     * @since 9
     */
    public static TimeUnit of(ChronoUnit chronoUnit) {
        switch (Objects.requireNonNull(chronoUnit, "chronoUnit")) {
            case NANOS:   return TimeUnit.NANOSECONDS;
            case MICROS:  return TimeUnit.MICROSECONDS;
            case MILLIS:  return TimeUnit.MILLISECONDS;
            case SECONDS: return TimeUnit.SECONDS;
            case MINUTES: return TimeUnit.MINUTES;
            case HOURS:   return TimeUnit.HOURS;
            case DAYS:    return TimeUnit.DAYS;
            default:
                throw new IllegalArgumentException("No TimeUnit equivalent for " + chronoUnit);
        }
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
