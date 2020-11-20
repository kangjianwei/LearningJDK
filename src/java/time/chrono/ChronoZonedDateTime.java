/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2007-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package java.time.chrono;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Comparator;
import java.util.Objects;

import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoUnit.FOREVER;
import static java.time.temporal.ChronoUnit.NANOS;

/**
 * A date-time with a time-zone in an arbitrary chronology,
 * intended for advanced globalization use cases.
 * <p>
 * <b>Most applications should declare method signatures, fields and variables
 * as {@link ZonedDateTime}, not this interface.</b>
 * <p>
 * A {@code ChronoZonedDateTime} is the abstract representation of an offset date-time
 * where the {@code Chronology chronology}, or calendar system, is pluggable.
 * The date-time is defined in terms of fields expressed by {@link TemporalField},
 * where most common implementations are defined in {@link ChronoField}.
 * The chronology defines how the calendar system operates and the meaning of
 * the standard fields.
 *
 * <h3>When to use this interface</h3>
 * The design of the API encourages the use of {@code ZonedDateTime} rather than this
 * interface, even in the case where the application needs to deal with multiple
 * calendar systems. The rationale for this is explored in detail in {@link ChronoLocalDate}.
 * <p>
 * Ensure that the discussion in {@code ChronoLocalDate} has been read and understood
 * before using this interface.
 *
 * @param <D> the concrete type for the date of this date-time
 *
 * @implSpec This interface must be implemented with care to ensure other classes operate correctly.
 * All implementations that can be instantiated must be final, immutable and thread-safe.
 * Subclasses should be Serializable wherever possible.
 * @since 1.8
 */
// "本地日期-时间"接口，"时间"[关联]了所属的时区ID，允许在子类中将"日期"部件绑定到某种历法系统
public interface ChronoZonedDateTime<D extends ChronoLocalDate> extends Temporal, Comparable<ChronoZonedDateTime<?>> {
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains an instance of {@code ChronoZonedDateTime} from a temporal object.
     * <p>
     * This creates a zoned date-time based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code ChronoZonedDateTime}.
     * <p>
     * The conversion extracts and combines the chronology, date, time and zone
     * from the temporal object. The behavior is equivalent to using
     * {@link Chronology#zonedDateTime(TemporalAccessor)} with the extracted chronology.
     * Implementations are permitted to perform optimizations such as accessing
     * those fields that are equivalent to the relevant objects.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code ChronoZonedDateTime::from}.
     *
     * @param temporal the temporal object to convert, not null
     *
     * @return the date-time, not null
     *
     * @throws DateTimeException if unable to convert to a {@code ChronoZonedDateTime}
     * @see Chronology#zonedDateTime(TemporalAccessor)
     */
    // 从temporal中获取/构造ChronoZonedDateTime对象
    static ChronoZonedDateTime<?> from(TemporalAccessor temporal) {
        Objects.requireNonNull(temporal, "temporal");
    
        if(temporal instanceof ChronoZonedDateTime) {
            return (ChronoZonedDateTime<?>) temporal;
        }
    
        // 查询时间量的历法系统
        Chronology chrono = temporal.query(TemporalQueries.chronology());
        if(chrono == null) {
            throw new DateTimeException("Unable to obtain ChronoZonedDateTime from TemporalAccessor: " + temporal.getClass());
        }
    
        // 构造ChronoZonedDateTime对象
        return chrono.zonedDateTime(temporal);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Converts this date-time to an {@code Instant}.
     * <p>
     * This returns an {@code Instant} representing the same point on the
     * time-line as this date-time. The calculation combines the
     * {@linkplain #toLocalDateTime() local date-time} and
     * {@linkplain #getOffset() offset}.
     *
     * @return an {@code Instant} representing the same instant, not null
     */
    // 将当前"本地日期-时间"转换为时间戳，该时间戳反映的是UTC/GMT"零时区"的时间点
    default Instant toInstant() {
        // 计算当前时间点下，UTC时区的纪元秒
        long second = toEpochSecond();
        
        // 获取当前时间量中的纳秒部件
        int nano = toLocalTime().getNano();
        
        // 根据给定的纪元秒与纳秒偏移构造一个时间戳
        return Instant.ofEpochSecond(second, nano);
    }
    
    /**
     * Converts this date-time to the number of seconds from the epoch
     * of 1970-01-01T00:00:00Z.
     * <p>
     * This uses the {@linkplain #toLocalDateTime() local date-time} and
     * {@linkplain #getOffset() offset} to calculate the epoch-second value,
     * which is the number of elapsed seconds from 1970-01-01T00:00:00Z.
     * Instants on the time-line after the epoch are positive, earlier are negative.
     *
     * @return the number of seconds from the epoch of 1970-01-01T00:00:00Z
     */
    // 计算当前时间点下，UTC时区的纪元秒
    default long toEpochSecond() {
        // 计算当前时间量的纪元天
        long epochDay = toLocalDate().toEpochDay();
        // 计算当前时间量的纪元秒
        long secs = epochDay * 86400 + toLocalTime().toSecondOfDay();
        
        // 将"本地"的纪元秒转换为UTC/GMT"零时区"的纪元秒
        secs -= getOffset().getTotalSeconds();
        
        return secs;
    }
    
    /*▲ 转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 部件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the zone ID, such as 'Europe/Paris'.
     * <p>
     * This returns the stored time-zone id used to determine the time-zone rules.
     *
     * @return the zone ID, not null
     */
    // 返回"时区ID"部件
    ZoneId getZone();
    
    /**
     * Gets the zone offset, such as '+01:00'.
     * <p>
     * This is the offset of the local date-time from UTC/Greenwich.
     *
     * @return the zone offset, not null
     */
    // 返回基于时间偏移的"时区ID"部件
    ZoneOffset getOffset();
    
    /**
     * Gets the local date-time part of this date-time.
     * <p>
     * This returns a local date with the same year, month and day
     * as this date-time.
     *
     * @return the local date-time part of this date-time, not null
     */
    // 返回"本地日期-时间"组件
    ChronoLocalDateTime<D> toLocalDateTime();
    
    /**
     * Gets the local date part of this date-time.
     * <p>
     * This returns a local date with the same year, month and day
     * as this date-time.
     *
     * @return the date part of this date-time, not null
     */
    // 返回"本地日期"组件
    default D toLocalDate() {
        return toLocalDateTime().toLocalDate();
    }
    
    /**
     * Gets the local time part of this date-time.
     * <p>
     * This returns a local time with the same hour, minute, second and
     * nanosecond as this date-time.
     *
     * @return the time part of this date-time, not null
     */
    // 返回"本地时间"组件
    default LocalTime toLocalTime() {
        return toLocalDateTime().toLocalTime();
    }
    
    /*▲ 部件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 增加 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * @throws DateTimeException   {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    /*
     * 对当前时间量的值与参数中的"时间段"求和
     *
     * 如果求和后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"求和"后的新对象再返回。
     */
    @Override
    default ChronoZonedDateTime<D> plus(TemporalAmount amount) {
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), Temporal.super.plus(amount));
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws DateTimeException   {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    /*
     * 对当前时间量的值累加amountToAdd个unit单位的时间量
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    @Override
    ChronoZonedDateTime<D> plus(long amountToAdd, TemporalUnit unit);
    
    /*▲ 增加 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 减少 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * @throws DateTimeException   {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    /*
     * 对当前时间量的值与参数中的"时间段"求差
     *
     * 如果求差后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"求差"后的新对象再返回。
     */
    @Override
    default ChronoZonedDateTime<D> minus(TemporalAmount amount) {
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), Temporal.super.minus(amount));
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws DateTimeException   {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    /*
     * 对当前时间量的值减去amountToSubtract个unit单位的时间量
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    @Override
    default ChronoZonedDateTime<D> minus(long amountToSubtract, TemporalUnit unit) {
        Temporal temporal = Temporal.super.minus(amountToSubtract, unit);
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), temporal);
    }
    
    /*▲ 减少 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量单位 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if the specified unit is supported.
     * <p>
     * This checks if the specified unit can be added to or subtracted from this date-time.
     * If false, then calling the {@link #plus(long, TemporalUnit)} and
     * {@link #minus(long, TemporalUnit) minus} methods will throw an exception.
     * <p>
     * The set of supported units is defined by the chronology and normally includes
     * all {@code ChronoUnit} units except {@code FOREVER}.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.isSupportedBy(Temporal)}
     * passing {@code this} as the argument.
     * Whether the unit is supported is determined by the unit.
     *
     * @param unit the unit to check, null returns false
     *
     * @return true if the unit can be added/subtracted, false if not
     */
    // 判断当前时间量是否支持指定的时间量单位
    @Override
    default boolean isSupported(TemporalUnit unit) {
        if(unit instanceof ChronoUnit) {
            return unit != FOREVER;
        }
        
        return unit != null && unit.isSupportedBy(this);
    }
    
    /*▲ 时间量单位 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量字段操作(TemporalAccessor) ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if the specified field is supported.
     * <p>
     * This checks if the specified field can be queried on this date-time.
     * If false, then calling the {@link #range(TemporalField) range},
     * {@link #get(TemporalField) get} and {@link #with(TemporalField, long)}
     * methods will throw an exception.
     * <p>
     * The set of supported fields is defined by the chronology and normally includes
     * all {@code ChronoField} fields.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.isSupportedBy(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the field is supported is determined by the field.
     *
     * @param field the field to check, null returns false
     *
     * @return true if the field can be queried, false if not
     */
    // 判断当前时间量是否支持指定的时间量字段
    @Override
    boolean isSupported(TemporalField field);
    
    // 返回时间量字段field的取值区间，通常要求当前时间量支持该时间量字段
    @Override
    default ValueRange range(TemporalField field) {
        if(field instanceof ChronoField) {
            if(field == INSTANT_SECONDS || field == OFFSET_SECONDS) {
                return field.range();
            }
            
            return toLocalDateTime().range(field);
        }
        
        return field.rangeRefinedBy(this);
    }
    
    // 以int形式返回时间量字段field的值
    @Override
    default int get(TemporalField field) {
        if(field instanceof ChronoField) {
            switch((ChronoField) field) {
                case INSTANT_SECONDS:
                    throw new UnsupportedTemporalTypeException("Invalid field 'InstantSeconds' for get() method, use getLong() instead");
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
            }
            
            return toLocalDateTime().get(field);
        }
        
        return Temporal.super.get(field);
    }
    
    // 以long形式返回时间量字段field的值
    @Override
    default long getLong(TemporalField field) {
        if(field instanceof ChronoField) {
            switch((ChronoField) field) {
                case INSTANT_SECONDS:
                    return toEpochSecond();
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
            }
            
            return toLocalDateTime().getLong(field);
        }
        
        return field.getFrom(this);
    }
    
    /**
     * Queries this date-time using the specified query.
     * <p>
     * This queries this date-time using the specified query strategy object.
     * The {@code TemporalQuery} object defines the logic to be used to
     * obtain the result. Read the documentation of the query to understand
     * what the result of this method will be.
     * <p>
     * The result of this method is obtained by invoking the
     * {@link TemporalQuery#queryFrom(TemporalAccessor)} method on the
     * specified query passing {@code this} as the argument.
     *
     * @param <R>   the type of the result
     * @param query the query to invoke, not null
     *
     * @return the query result, null may be returned (defined by the query)
     *
     * @throws DateTimeException   if unable to query (defined by the query)
     * @throws ArithmeticException if numeric overflow occurs (defined by the query)
     */
    // 使用指定的时间量查询器，从当前时间量中查询目标信息
    @SuppressWarnings("unchecked")
    @Override
    default <R> R query(TemporalQuery<R> query) {
        // 查询时间量支持的最小时间量单位
        if(query == TemporalQueries.precision()) {
            return (R) NANOS;
        }
        
        // 查询时间量的历法系统
        if(query == TemporalQueries.chronology()) {
            return (R) getChronology();
        }
        
        if(query == TemporalQueries.zone() || query == TemporalQueries.zoneId()) {
            return (R) getZone();
        }
        
        if(query == TemporalQueries.offset()) {
            return (R) getOffset();
        }
        
        // 查询时间量的本地时间
        if(query == TemporalQueries.localTime()) {
            return (R) toLocalTime();
        }
        
        /*
         * inline TemporalAccessor.super.query(query) as an optimization
         * non-JDK classes are not permitted to make this optimization
         */
        return query.queryFrom(this);
    }
    
    /*▲ 时间量字段操作(TemporalAccessor) ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 整合 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * @throws DateTimeException   {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    /*
     * 使用指定的时间量整合器adjuster来构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     */
    @Override
    default ChronoZonedDateTime<D> with(TemporalAdjuster adjuster) {
        Temporal temporal = Temporal.super.with(adjuster);
        
        // 获取当前时间量的历法系统
        Chronology chrono = getChronology();
        
        // 判断当前时间量的历法系统chrono是否与时间量temporal的历法系统相同
        return ChronoZonedDateTimeImpl.ensureValid(chrono, temporal);
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws DateTimeException   {@inheritDoc}
     * @throws ArithmeticException {@inheritDoc}
     */
    /*
     * 通过整合指定类型的字段和当前时间量中的其他类型的字段来构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * field   : 待整合的字段(类型)
     * newValue: field的原始值，需要根据filed的类型进行放缩
     */
    @Override
    ChronoZonedDateTime<D> with(TemporalField field, long newValue);
    
    /**
     * Returns a copy of this date-time with a different time-zone,
     * retaining the local date-time if possible.
     * <p>
     * This method changes the time-zone and retains the local date-time.
     * The local date-time is only changed if it is invalid for the new zone.
     * <p>
     * To change the zone and adjust the local date-time,
     * use {@link #withZoneSameInstant(ZoneId)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param zone the time-zone to change to, not null
     *
     * @return a {@code ChronoZonedDateTime} based on this date-time with the requested zone, not null
     */
    /*
     * 将指定的"时区ID"整合到当前时间量中以构造时间量对象。
     * 该操作是尝试对ZoneId属性和ZoneOffset属性进行更新。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：(本地日期-时间)、时区ID、基于时间偏移的时区ID
     */
    ChronoZonedDateTime<D> withZoneSameLocal(ZoneId zone);
    
    /**
     * Returns a copy of this date-time with a different time-zone,
     * retaining the instant.
     * <p>
     * This method changes the time-zone and retains the instant.
     * This normally results in a change to the local date-time.
     * <p>
     * This method is based on retaining the same instant, thus gaps and overlaps
     * in the local time-line have no effect on the result.
     * <p>
     * To change the offset while keeping the local time,
     * use {@link #withZoneSameLocal(ZoneId)}.
     *
     * @param zone the time-zone to change to, not null
     *
     * @return a {@code ChronoZonedDateTime} based on this date-time with the requested zone, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 将指定的"时区ID"整合到当前时间量中以构造时间量对象。
     * 该操作会先获取在当前时刻时UTC时区的时间点，然后使用该时间点与zone构造一个ZonedDateTime对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：本地日期-时间、时区ID、基于时间偏移的时区ID
     */
    ChronoZonedDateTime<D> withZoneSameInstant(ZoneId zone);
    
    /**
     * Returns a copy of this date-time changing the zone offset to the
     * earlier of the two valid offsets at a local time-line overlap.
     * <p>
     * This method only has any effect when the local time-line overlaps, such as
     * at an autumn daylight savings cutover. In this scenario, there are two
     * valid offsets for the local date-time. Calling this method will return
     * a zoned date-time with the earlier of the two selected.
     * <p>
     * If this method is called when it is not an overlap, {@code this}
     * is returned.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return a {@code ChronoZonedDateTime} based on this date-time with the earlier offset, not null
     *
     * @throws DateTimeException if no rules can be found for the zone
     * @throws DateTimeException if no rules are valid for this date-time
     */
    /*
     * 如果当前本地日期-时间位于其所在时区的"重叠时间"中，
     * 则将当前时间量的时区偏移更新为"重叠时间"之前的时区偏移。
     */
    ChronoZonedDateTime<D> withEarlierOffsetAtOverlap();
    
    /**
     * Returns a copy of this date-time changing the zone offset to the
     * later of the two valid offsets at a local time-line overlap.
     * <p>
     * This method only has any effect when the local time-line overlaps, such as
     * at an autumn daylight savings cutover. In this scenario, there are two
     * valid offsets for the local date-time. Calling this method will return
     * a zoned date-time with the later of the two selected.
     * <p>
     * If this method is called when it is not an overlap, {@code this}
     * is returned.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return a {@code ChronoZonedDateTime} based on this date-time with the later offset, not null
     *
     * @throws DateTimeException if no rules can be found for the zone
     * @throws DateTimeException if no rules are valid for this date-time
     */
    /*
     * 如果当前本地日期-时间位于其所在时区的"重叠时间"中，
     * 则将当前时间量的时区偏移更新为"重叠时间"之后的时区偏移。
     */
    ChronoZonedDateTime<D> withLaterOffsetAtOverlap();
    
    /*▲ 整合 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the chronology of this date-time.
     * <p>
     * The {@code Chronology} represents the calendar system in use.
     * The era and other fields in {@link ChronoField} are defined by the chronology.
     *
     * @return the chronology, not null
     */
    // 返回当前时间量的历法系统
    default Chronology getChronology() {
        return toLocalDate().getChronology();
    }
    
    /**
     * Checks if the instant of this date-time is after that of the specified date-time.
     * <p>
     * This method differs from the comparison in {@link #compareTo} in that it
     * only compares the instant of the date-time. This is equivalent to using
     * {@code dateTime1.toInstant().isAfter(dateTime2.toInstant());}.
     * <p>
     * This default implementation performs the comparison based on the epoch-second
     * and nano-of-second.
     *
     * @param other the other date-time to compare to, not null
     *
     * @return true if this is after the specified date-time
     */
    // 判断当前日期-时间是否晚于参数中指定的日期-时间
    default boolean isAfter(ChronoZonedDateTime<?> other) {
        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        
        if(thisEpochSec>otherEpochSec) {
            return true;
        } else if(thisEpochSec<otherEpochSec) {
            return false;
        } else {
            return toLocalTime().getNano()>other.toLocalTime().getNano();
        }
    }
    
    /**
     * Checks if the instant of this date-time is before that of the specified date-time.
     * <p>
     * This method differs from the comparison in {@link #compareTo} in that it
     * only compares the instant of the date-time. This is equivalent to using
     * {@code dateTime1.toInstant().isBefore(dateTime2.toInstant());}.
     * <p>
     * This default implementation performs the comparison based on the epoch-second
     * and nano-of-second.
     *
     * @param other the other date-time to compare to, not null
     *
     * @return true if this point is before the specified date-time
     */
    // 判断当前日期-时间是否早于参数中指定的日期-时间
    default boolean isBefore(ChronoZonedDateTime<?> other) {
        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        
        if(thisEpochSec>otherEpochSec) {
            return false;
        } else if(thisEpochSec<otherEpochSec) {
            return true;
        } else {
            return toLocalTime().getNano()<other.toLocalTime().getNano();
        }
    }
    
    /**
     * Checks if the instant of this date-time is equal to that of the specified date-time.
     * <p>
     * This method differs from the comparison in {@link #compareTo} and {@link #equals}
     * in that it only compares the instant of the date-time. This is equivalent to using
     * {@code dateTime1.toInstant().equals(dateTime2.toInstant());}.
     * <p>
     * This default implementation performs the comparison based on the epoch-second
     * and nano-of-second.
     *
     * @param other the other date-time to compare to, not null
     *
     * @return true if the instant equals the instant of the specified date-time
     */
    // 判断当前日期-时间与参数中指定的日期-时间是否相等
    default boolean isEqual(ChronoZonedDateTime<?> other) {
        return toEpochSecond() == other.toEpochSecond() && toLocalTime().getNano() == other.toLocalTime().getNano();
    }
    
    /**
     * Gets a comparator that compares {@code ChronoZonedDateTime} in
     * time-line order ignoring the chronology.
     * <p>
     * This comparator differs from the comparison in {@link #compareTo} in that it
     * only compares the underlying instant and not the chronology.
     * This allows dates in different calendar systems to be compared based
     * on the position of the date-time on the instant time-line.
     * The underlying comparison is equivalent to comparing the epoch-second and nano-of-second.
     *
     * @return a comparator that compares in time-line order ignoring the chronology
     *
     * @see #isAfter
     * @see #isBefore
     * @see #isEqual
     */
    // 返回一个外部比较器，以比较两个"本地日期-时间"的早晚
    static Comparator<ChronoZonedDateTime<?>> timeLineOrder() {
        return new Comparator<ChronoZonedDateTime<?>>() {
            @Override
            public int compare(ChronoZonedDateTime<?> dateTime1, ChronoZonedDateTime<?> dateTime2) {
                int cmp = Long.compare(dateTime1.toEpochSecond(), dateTime2.toEpochSecond());
                if(cmp == 0) {
                    cmp = Long.compare(dateTime1.toLocalTime().getNano(), dateTime2.toLocalTime().getNano());
                }
                return cmp;
            }
        };
    }
    
    /**
     * Formats this date-time using the specified formatter.
     * <p>
     * This date-time will be passed to the formatter to produce a string.
     * <p>
     * The default implementation must behave as follows:
     * <pre>
     *  return formatter.format(this);
     * </pre>
     *
     * @param formatter the formatter to use, not null
     *
     * @return the formatted date-time string, not null
     *
     * @throws DateTimeException if an error occurs during printing
     */
    // 将当前日期-时间转换为一个指定格式的字符串后返回
    default String format(DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Compares this date-time to another date-time, including the chronology.
     * <p>
     * The comparison is based first on the instant, then on the local date-time,
     * then on the zone ID, then on the chronology.
     * It is "consistent with equals", as defined by {@link Comparable}.
     * <p>
     * If all the date-time objects being compared are in the same chronology, then the
     * additional chronology stage is not required.
     * <p>
     * This default implementation performs the comparison defined above.
     *
     * @param other the other date-time to compare to, not null
     *
     * @return the comparator value, negative if less, positive if greater
     */
    @Override
    default int compareTo(ChronoZonedDateTime<?> other) {
        int cmp = Long.compare(toEpochSecond(), other.toEpochSecond());
        if(cmp == 0) {
            cmp = toLocalTime().getNano() - other.toLocalTime().getNano();
            if(cmp == 0) {
                cmp = toLocalDateTime().compareTo(other.toLocalDateTime());
                if(cmp == 0) {
                    cmp = getZone().getId().compareTo(other.getZone().getId());
                    if(cmp == 0) {
                        cmp = getChronology().compareTo(other.getChronology());
                    }
                }
            }
        }
        return cmp;
    }
    
    /**
     * Outputs this date-time as a {@code String}.
     * <p>
     * The output will include the full zoned date-time.
     *
     * @return a string representation of this date-time, not null
     */
    @Override
    String toString();
    
    /**
     * Checks if this date-time is equal to another date-time.
     * <p>
     * The comparison is based on the offset date-time and the zone.
     * To compare for the same instant on the time-line, use {@link #compareTo}.
     * Only objects of type {@code ChronoZonedDateTime} are compared, other types return false.
     *
     * @param obj the object to check, null returns false
     *
     * @return true if this is equal to the other date-time
     */
    @Override
    boolean equals(Object obj);
    
    /**
     * A hash code for this date-time.
     *
     * @return a suitable hash code
     */
    @Override
    int hashCode();
    
}
