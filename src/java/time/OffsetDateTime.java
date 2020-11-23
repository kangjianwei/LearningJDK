/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package java.time;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import java.time.zone.ZoneRules;
import java.util.Comparator;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.FOREVER;
import static java.time.temporal.ChronoUnit.NANOS;

/**
 * A date-time with an offset from UTC/Greenwich in the ISO-8601 calendar system,
 * such as {@code 2007-12-03T10:15:30+01:00}.
 * <p>
 * {@code OffsetDateTime} is an immutable representation of a date-time with an offset.
 * This class stores all date and time fields, to a precision of nanoseconds,
 * as well as the offset from UTC/Greenwich. For example, the value
 * "2nd October 2007 at 13:45:30.123456789 +02:00" can be stored in an {@code OffsetDateTime}.
 * <p>
 * {@code OffsetDateTime}, {@link java.time.ZonedDateTime} and {@link java.time.Instant} all store an instant
 * on the time-line to nanosecond precision.
 * {@code Instant} is the simplest, simply representing the instant.
 * {@code OffsetDateTime} adds to the instant the offset from UTC/Greenwich, which allows
 * the local date-time to be obtained.
 * {@code ZonedDateTime} adds full time-zone rules.
 * <p>
 * It is intended that {@code ZonedDateTime} or {@code Instant} is used to model data
 * in simpler applications. This class may be used when modeling date-time concepts in
 * more detail, or when communicating to a database or in a network protocol.
 *
 * <p>
 * This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; use of identity-sensitive operations (including reference equality
 * ({@code ==}), identity hash code, or synchronization) on instances of
 * {@code OffsetDateTime} may have unpredictable results and should be avoided.
 * The {@code equals} method should be used for comparisons.
 *
 * @implSpec This class is immutable and thread-safe.
 * @since 1.8
 */
/*
 * "本地日期-时间"，"时间"[关联]了所属的时区ID，"日期"基于[ISO]历法系统。
 *
 * 注：这里关联的时区ID是基于时间偏移的。
 */
public final class OffsetDateTime implements Temporal, TemporalAdjuster, Comparable<OffsetDateTime>, Serializable {
    
    /**
     * The minimum supported {@code OffsetDateTime}, '-999999999-01-01T00:00:00+18:00'.
     * This is the local date-time of midnight at the start of the minimum date
     * in the maximum offset (larger offsets are earlier on the time-line).
     * This combines {@link LocalDateTime#MIN} and {@link ZoneOffset#MAX}.
     * This could be used by an application as a "far past" date-time.
     */
    public static final OffsetDateTime MIN = LocalDateTime.MIN.atOffset(ZoneOffset.MAX);
    /**
     * The maximum supported {@code OffsetDateTime}, '+999999999-12-31T23:59:59.999999999-18:00'.
     * This is the local date-time just before midnight at the end of the maximum date
     * in the minimum offset (larger negative offsets are later on the time-line).
     * This combines {@link LocalDateTime#MAX} and {@link ZoneOffset#MIN}.
     * This could be used by an application as a "far future" date-time.
     */
    public static final OffsetDateTime MAX = LocalDateTime.MAX.atOffset(ZoneOffset.MIN);
    
    /**
     * The local date-time.
     */
    // 位于offset时区的"本地日期-时间"
    private final LocalDateTime dateTime;
    
    /**
     * The offset from UTC/Greenwich.
     */
    // 基于时间偏移的时区ID，用来指示当前"本地日期-时间"所处的时区
    private final ZoneOffset offset;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructor.
     *
     * @param dateTime the local date-time, not null
     * @param offset   the zone offset, not null
     */
    private OffsetDateTime(LocalDateTime dateTime, ZoneOffset offset) {
        this.dateTime = Objects.requireNonNull(dateTime, "dateTime");
        this.offset = Objects.requireNonNull(offset, "offset");
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains the current date-time from the system clock in the default time-zone.
     * <p>
     * This will query the {@link Clock#systemDefaultZone() system clock} in the default
     * time-zone to obtain the current date-time.
     * The offset will be calculated from the time-zone in the clock.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @return the current date-time using the system clock, not null
     */
    // 构造属于系统默认时区的"本地日期-时间"对象(时区偏移时间准确)
    public static OffsetDateTime now() {
        // 获取一个系统时钟，其预设的时区ID为系统默认的时区ID
        Clock clock = Clock.systemDefaultZone();
        return now(clock);
    }
    
    /**
     * Obtains the current date-time from the system clock in the specified time-zone.
     * <p>
     * This will query the {@link Clock#system(ZoneId) system clock} to obtain the current date-time.
     * Specifying the time-zone avoids dependence on the default time-zone.
     * The offset will be calculated from the specified time-zone.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @param zone the zone ID to use, not null
     *
     * @return the current date-time using the system clock, not null
     */
    // 构造属于zone时区的"本地日期-时间"对象(时区偏移时间准确)
    public static OffsetDateTime now(ZoneId zone) {
        // 获取一个系统时钟，其预设的时区ID为zone
        Clock clock = Clock.system(zone);
        return now(clock);
    }
    
    /**
     * Obtains the current date-time from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current date-time.
     * The offset will be calculated from the time-zone in the clock.
     * <p>
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@link Clock dependency injection}.
     *
     * @param clock the clock to use, not null
     *
     * @return the current date-time, not null
     */
    // 使用指定的时钟构造"本地日期-时间"对象(时区偏移时间准确)
    public static OffsetDateTime now(Clock clock) {
        Objects.requireNonNull(clock, "clock");
    
        // 获取clock时钟提供的时间戳
        final Instant instant = clock.instant();
        // 获取clock时钟提供的时区ID
        ZoneId zoneId = clock.getZone();
    
        // 获取与zoneId对应的"时区规则集"
        ZoneRules rules = zoneId.getRules();
        /*
         * 获取zoneId时区在instant时刻的"实际偏移"。
         * 这里可以返回一个准确的"实际偏移"。
         */
        ZoneOffset offset = rules.getOffset(instant);
    
        // 使用给定的时间戳构造属于offset时区的"本地日期-时间"对象(时区偏移时间准确)
        return ofInstant(instant, offset);
    }
    
    /**
     * Obtains an instance of {@code OffsetDateTime} from a date, time and offset.
     * <p>
     * This creates an offset date-time with the specified local date, time and offset.
     *
     * @param date   the local date, not null
     * @param time   the local time, not null
     * @param offset the zone offset, not null
     *
     * @return the offset date-time, not null
     */
    // 使用给定的"本地日期"部件和"本地时间"部件构造属于offset的"本地日期-时间"对象
    public static OffsetDateTime of(LocalDate date, LocalTime time, ZoneOffset offset) {
        LocalDateTime dt = LocalDateTime.of(date, time);
        return new OffsetDateTime(dt, offset);
    }
    
    /**
     * Obtains an instance of {@code OffsetDateTime} from a date-time and offset.
     * <p>
     * This creates an offset date-time with the specified local date-time and offset.
     *
     * @param localDateTime the local date-time, not null
     * @param offset        the zone offset, not null
     *
     * @return the offset date-time, not null
     */
    // 使用localDateTime构造属于zone的"本地日期-时间"对象
    public static OffsetDateTime of(LocalDateTime localDateTime, ZoneOffset offset) {
        return new OffsetDateTime(localDateTime, offset);
    }
    
    /**
     * Obtains an instance of {@code OffsetDateTime} from a year, month, day,
     * hour, minute, second, nanosecond and offset.
     * <p>
     * This creates an offset date-time with the seven specified fields.
     * <p>
     * This method exists primarily for writing test cases.
     * Non test-code will typically use other methods to create an offset time.
     * {@code LocalDateTime} has five additional convenience variants of the
     * equivalent factory method taking fewer arguments.
     * They are not provided here to reduce the footprint of the API.
     *
     * @param year         the year to represent, from MIN_YEAR to MAX_YEAR
     * @param month        the month-of-year to represent, from 1 (January) to 12 (December)
     * @param dayOfMonth   the day-of-month to represent, from 1 to 31
     * @param hour         the hour-of-day to represent, from 0 to 23
     * @param minute       the minute-of-hour to represent, from 0 to 59
     * @param second       the second-of-minute to represent, from 0 to 59
     * @param nanoOfSecond the nano-of-second to represent, from 0 to 999,999,999
     * @param offset       the zone offset, not null
     *
     * @return the offset date-time, not null
     *
     * @throws DateTimeException if the value of any field is out of range, or
     *                           if the day-of-month is invalid for the month-year
     */
    // 使用给定的日期部件和时间部件构造属于offset时区的"本地日期-时间"对象
    public static OffsetDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, ZoneOffset offset) {
        LocalDateTime dt = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
        return new OffsetDateTime(dt, offset);
    }
    
    /**
     * Obtains an instance of {@code OffsetDateTime} from an {@code Instant} and zone ID.
     * <p>
     * This creates an offset date-time with the same instant as that specified.
     * Finding the offset from UTC/Greenwich is simple as there is only one valid
     * offset for each instant.
     *
     * @param instant the instant to create the date-time from, not null
     * @param zone    the time-zone, which may be an offset, not null
     *
     * @return the offset date-time, not null
     *
     * @throws DateTimeException if the result exceeds the supported range
     */
    // 使用给定的时间戳构造属于zone时区的"本地日期-时间"对象(时区偏移时间准确)
    public static OffsetDateTime ofInstant(Instant instant, ZoneId zone) {
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(zone, "zone");
    
        // 获取与zone对应的"时区规则集"
        ZoneRules rules = zone.getRules();
        /*
         * 获取zone时区在instant时刻的"实际偏移"。
         * 这里可以返回一个准确的"实际偏移"。
         */
        ZoneOffset offset = rules.getOffset(instant);
    
        // 获取纪元秒部件
        long epochSecond = instant.getEpochSecond();
        // 获取纳秒偏移部件
        int nano = instant.getNano();
    
        // 使用UTC时区的纪元秒、纳秒偏移以及时区ID构造一个属于offset时区的"本地日期-时间"
        LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(epochSecond, nano, offset);
    
        return new OffsetDateTime(localDateTime, offset);
    }
    
    /**
     * Obtains an instance of {@code OffsetDateTime} from a temporal object.
     * <p>
     * This obtains an offset date-time based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code OffsetDateTime}.
     * <p>
     * The conversion will first obtain a {@code ZoneOffset} from the temporal object.
     * It will then try to obtain a {@code LocalDateTime}, falling back to an {@code Instant} if necessary.
     * The result will be the combination of {@code ZoneOffset} with either
     * with {@code LocalDateTime} or {@code Instant}.
     * Implementations are permitted to perform optimizations such as accessing
     * those fields that are equivalent to the relevant objects.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code OffsetDateTime::from}.
     *
     * @param temporal the temporal object to convert, not null
     *
     * @return the offset date-time, not null
     *
     * @throws DateTimeException if unable to convert to an {@code OffsetDateTime}
     */
    // 从temporal中获取/构造OffsetDateTime部件
    public static OffsetDateTime from(TemporalAccessor temporal) {
        if(temporal instanceof OffsetDateTime) {
            return (OffsetDateTime) temporal;
        }
    
        try {
            // 从temporal中查询出基于时间偏移的时区ID
            ZoneOffset offset = ZoneOffset.from(temporal);
            // 从temporal中查询LocalDate部件的信息
            LocalDate date = temporal.query(TemporalQueries.localDate());
            // 从temporal中查询LocalTime部件的信息
            LocalTime time = temporal.query(TemporalQueries.localTime());
        
            if(date != null && time != null) {
                return OffsetDateTime.of(date, time, offset);
            }
        
            // 从时间量temporal中获取秒部件和纳秒部件的信息，以构造一个时间戳
            Instant instant = Instant.from(temporal);
        
            // 使用给定的时间戳构造属于offset时区的"本地日期-时间"对象(时区偏移时间准确)
            return OffsetDateTime.ofInstant(instant, offset);
        } catch(DateTimeException ex) {
            throw new DateTimeException("Unable to obtain OffsetDateTime from TemporalAccessor: " + temporal + " of type " + temporal.getClass().getName(), ex);
        }
    }
    
    /**
     * Obtains an instance of {@code OffsetDateTime} from a text string
     * such as {@code 2007-12-03T10:15:30+01:00}.
     * <p>
     * The string must represent a valid date-time and is parsed using
     * {@link java.time.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME}.
     *
     * @param text the text to parse such as "2007-12-03T10:15:30+01:00", not null
     *
     * @return the parsed offset date-time, not null
     *
     * @throws DateTimeParseException if the text cannot be parsed
     */
    // 从指定的文本中解析出ZonedDateTime信息，要求该文本符合ISO规范，即类似：2020-01-15T08:20:53+08:00
    public static OffsetDateTime parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
    
    /**
     * Obtains an instance of {@code OffsetDateTime} from a text string using a specific formatter.
     * <p>
     * The text is parsed using the formatter, returning a date-time.
     *
     * @param text      the text to parse, not null
     * @param formatter the formatter to use, not null
     *
     * @return the parsed offset date-time, not null
     *
     * @throws DateTimeParseException if the text cannot be parsed
     */
    // 从指定的文本中解析出ZonedDateTime信息，要求该文本符合指定的格式规范
    public static OffsetDateTime parse(CharSequence text, DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        return formatter.parse(text, OffsetDateTime::from);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Combines this date-time with a time-zone to create a {@code ZonedDateTime}
     * ensuring that the result has the same instant.
     * <p>
     * This returns a {@code ZonedDateTime} formed from this date-time and the specified time-zone.
     * This conversion will ignore the visible local date-time and use the underlying instant instead.
     * This avoids any problems with local time-line gaps or overlaps.
     * The result might have different values for fields such as hour, minute an even day.
     * <p>
     * To attempt to retain the values of the fields, use {@link #atZoneSimilarLocal(ZoneId)}.
     * To use the offset as the zone ID, use {@link #toZonedDateTime()}.
     *
     * @param zone the time-zone to use, not null
     *
     * @return the zoned date-time formed from this date-time, not null
     */
    // 将当前时间量转换为属于zone的"本地日期-时间"对象(时区偏移时间准确)
    public ZonedDateTime atZoneSameInstant(ZoneId zone) {
        return ZonedDateTime.ofInstant(dateTime, offset, zone);
    }
    
    /**
     * Combines this date-time with a time-zone to create a {@code ZonedDateTime}
     * trying to keep the same local date and time.
     * <p>
     * This returns a {@code ZonedDateTime} formed from this date-time and the specified time-zone.
     * Where possible, the result will have the same local date-time as this object.
     * <p>
     * Time-zone rules, such as daylight savings, mean that not every time on the
     * local time-line exists. If the local date-time is in a gap or overlap according to
     * the rules then a resolver is used to determine the resultant local time and offset.
     * This method uses {@link ZonedDateTime#ofLocal(LocalDateTime, ZoneId, ZoneOffset)}
     * to retain the offset from this instance if possible.
     * <p>
     * Finer control over gaps and overlaps is available in two ways.
     * If you simply want to use the later offset at overlaps then call
     * {@link ZonedDateTime#withLaterOffsetAtOverlap()} immediately after this method.
     * <p>
     * To create a zoned date-time at the same instant irrespective of the local time-line,
     * use {@link #atZoneSameInstant(ZoneId)}.
     * To use the offset as the zone ID, use {@link #toZonedDateTime()}.
     *
     * @param zone the time-zone to use, not null
     *
     * @return the zoned date-time formed from this date and the earliest valid time for the zone, not null
     */
    /*
     * 将当前时间量转换为属于zone的"本地日期-时间"对象
     * 如果zone不是ZoneOffset类型，则时区偏移时间可能会不准确。
     */
    public ZonedDateTime atZoneSimilarLocal(ZoneId zone) {
        return ZonedDateTime.ofLocal(dateTime, zone, offset);
    }
    
    /**
     * Converts this date-time to a {@code ZonedDateTime} using the offset as the zone ID.
     * <p>
     * This creates the simplest possible {@code ZonedDateTime} using the offset
     * as the zone ID.
     * <p>
     * To control the time-zone used, see {@link #atZoneSameInstant(ZoneId)} and
     * {@link #atZoneSimilarLocal(ZoneId)}.
     *
     * @return a zoned date-time representing the same local date-time and offset, not null
     */
    // 将当前时间量转换为ZonedDateTime对象后返回(时区偏移时间准确)
    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.of(dateTime, offset);
    }
    
    /**
     * Converts this date-time to an {@code OffsetTime}.
     * <p>
     * This returns an offset time with the same local time and offset.
     *
     * @return an OffsetTime representing the time and offset, not null
     */
    // 将当前时间量转换为OffsetTime对象
    public OffsetTime toOffsetTime() {
        return OffsetTime.of(dateTime.toLocalTime(), offset);
    }
    
    /**
     * Converts this date-time to an {@code Instant}.
     * <p>
     * This returns an {@code Instant} representing the same point on the
     * time-line as this date-time.
     *
     * @return an {@code Instant} representing the same instant, not null
     */
    // 将当前时间量转换为时间戳，该时间戳反映的是UTC/GMT"零时区"的时间点
    public Instant toInstant() {
        return dateTime.toInstant(offset);
    }
    
    /**
     * Converts this date-time to the number of seconds from the epoch of 1970-01-01T00:00:00Z.
     * <p>
     * This allows this date-time to be converted to a value of the
     * {@link ChronoField#INSTANT_SECONDS epoch-seconds} field. This is primarily
     * intended for low-level conversions rather than general application usage.
     *
     * @return the number of seconds from the epoch of 1970-01-01T00:00:00Z
     */
    // 计算当前时间点下，UTC时区的纪元秒
    public long toEpochSecond() {
        return dateTime.toEpochSecond(offset);
    }
    
    /*▲ 转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 部件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the zone offset, such as '+01:00'.
     * <p>
     * This is the offset of the local date-time from UTC/Greenwich.
     *
     * @return the zone offset, not null
     */
    // 返回基于时间偏移的"时区ID"部件
    public ZoneOffset getOffset() {
        return offset;
    }
    
    /**
     * Gets the {@code LocalDateTime} part of this date-time.
     * <p>
     * This returns a {@code LocalDateTime} with the same year, month, day and time
     * as this date-time.
     *
     * @return the local date-time part of this date-time, not null
     */
    // 返回"本地日期-时间"组件
    public LocalDateTime toLocalDateTime() {
        return dateTime;
    }
    
    /**
     * Gets the {@code LocalDate} part of this date-time.
     * <p>
     * This returns a {@code LocalDate} with the same year, month and day
     * as this date-time.
     *
     * @return the date part of this date-time, not null
     */
    // 返回"本地日期"组件
    public LocalDate toLocalDate() {
        return dateTime.toLocalDate();
    }
    
    /**
     * Gets the {@code LocalTime} part of this date-time.
     * <p>
     * This returns a {@code LocalTime} with the same hour, minute, second and
     * nanosecond as this date-time.
     *
     * @return the time part of this date-time, not null
     */
    // 返回"本地时间"组件
    public LocalTime toLocalTime() {
        return dateTime.toLocalTime();
    }
    
    /**
     * Gets the year field.
     * <p>
     * This method returns the primitive {@code int} value for the year.
     * <p>
     * The year returned by this method is proleptic as per {@code get(YEAR)}.
     * To obtain the year-of-era, use {@code get(YEAR_OF_ERA)}.
     *
     * @return the year, from MIN_YEAR to MAX_YEAR
     */
    // (哪年)返回"年份"部件[-999999999, 999999999]，由"本地日期"部件计算而来
    public int getYear() {
        return dateTime.getYear();
    }
    
    /**
     * Gets the month-of-year field from 1 to 12.
     * <p>
     * This method returns the month as an {@code int} from 1 to 12.
     * Application code is frequently clearer if the enum {@link Month}
     * is used by calling {@link #getMonth()}.
     *
     * @return the month-of-year, from 1 to 12
     *
     * @see #getMonth()
     */
    // (哪月)返回"月份"部件[1, 12]，由"本地日期"部件计算而来
    public int getMonthValue() {
        return dateTime.getMonthValue();
    }
    
    /**
     * Gets the month-of-year field using the {@code Month} enum.
     * <p>
     * This method returns the enum {@link Month} for the month.
     * This avoids confusion as to what {@code int} values mean.
     * If you need access to the primitive {@code int} value then the enum
     * provides the {@link Month#getValue() int value}.
     *
     * @return the month-of-year, not null
     *
     * @see #getMonthValue()
     */
    // (哪月)以Month形式返回"月份"部件，由"本地日期"部件计算而来
    public Month getMonth() {
        return dateTime.getMonth();
    }
    
    /**
     * Gets the day-of-month field.
     * <p>
     * This method returns the primitive {@code int} value for the day-of-month.
     *
     * @return the day-of-month, from 1 to 31
     */
    // (哪日)返回"天"部件[1, 28/31]
    public int getDayOfMonth() {
        return dateTime.getDayOfMonth();
    }
    
    /**
     * Gets the day-of-week field, which is an enum {@code DayOfWeek}.
     * <p>
     * This method returns the enum {@link DayOfWeek} for the day-of-week.
     * This avoids confusion as to what {@code int} values mean.
     * If you need access to the primitive {@code int} value then the enum
     * provides the {@link DayOfWeek#getValue() int value}.
     * <p>
     * Additional information can be obtained from the {@code DayOfWeek}.
     * This includes textual names of the values.
     *
     * @return the day-of-week, not null
     */
    // (周几)返回当前"本地日期-时间"是所在周的第几天
    public DayOfWeek getDayOfWeek() {
        return dateTime.getDayOfWeek();
    }
    
    /**
     * Gets the day-of-year field.
     * <p>
     * This method returns the primitive {@code int} value for the day-of-year.
     *
     * @return the day-of-year, from 1 to 365, or 366 in a leap year
     */
    // 返回当前"本地日期-时间"是所在年份的第几天
    public int getDayOfYear() {
        return dateTime.getDayOfYear();
    }
    
    /**
     * Gets the hour-of-day field.
     *
     * @return the hour-of-day, from 0 to 23
     */
    // (几时)返回"小时"部件[0, 23]，由"本地时间"部件计算而来
    public int getHour() {
        return dateTime.getHour();
    }
    
    /**
     * Gets the minute-of-hour field.
     *
     * @return the minute-of-hour, from 0 to 59
     */
    // (几分)返回"分钟"部件[0, 59]，由"本地时间"部件计算而来
    public int getMinute() {
        return dateTime.getMinute();
    }
    
    /**
     * Gets the second-of-minute field.
     *
     * @return the second-of-minute, from 0 to 59
     */
    // (几秒)返回"秒"部件[0, 59]，由"本地时间"部件计算而来
    public int getSecond() {
        return dateTime.getSecond();
    }
    
    /**
     * Gets the nano-of-second field.
     *
     * @return the nano-of-second, from 0 to 999,999,999
     */
    // (几纳秒)返回"纳秒"部件[0, 999999999]，由"本地时间"部件计算而来
    public int getNano() {
        return dateTime.getNano();
    }
    
    /*▲ 部件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 增加 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a copy of this date-time with the specified amount added.
     * <p>
     * This returns an {@code OffsetDateTime}, based on this one, with the specified amount added.
     * The amount is typically {@link Period} or {@link Duration} but may be
     * any other type implementing the {@link TemporalAmount} interface.
     * <p>
     * The calculation is delegated to the amount object by calling
     * {@link TemporalAmount#addTo(Temporal)}. The amount implementation is free
     * to implement the addition in any way it wishes, however it typically
     * calls back to {@link #plus(long, TemporalUnit)}. Consult the documentation
     * of the amount implementation to determine if it can be successfully added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToAdd the amount to add, not null
     *
     * @return an {@code OffsetDateTime} based on this date-time with the addition made, not null
     *
     * @throws DateTimeException   if the addition cannot be made
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 对当前时间量的值与参数中的"时间段"求和
     *
     * 如果求和后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"求和"后的新对象再返回。
     */
    @Override
    public OffsetDateTime plus(TemporalAmount amountToAdd) {
        return (OffsetDateTime) amountToAdd.addTo(this);
    }
    
    /**
     * Returns a copy of this date-time with the specified amount added.
     * <p>
     * This returns an {@code OffsetDateTime}, based on this one, with the amount
     * in terms of the unit added. If it is not possible to add the amount, because the
     * unit is not supported or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoUnit} then the addition is implemented by
     * {@link LocalDateTime#plus(long, TemporalUnit)}.
     * The offset is not part of the calculation and will be unchanged in the result.
     * <p>
     * If the field is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.addTo(Temporal, long)}
     * passing {@code this} as the argument. In this case, the unit determines
     * whether and how to perform the addition.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToAdd the amount of the unit to add to the result, may be negative
     * @param unit        the unit of the amount to add, not null
     *
     * @return an {@code OffsetDateTime} based on this date-time with the specified amount added, not null
     *
     * @throws DateTimeException                if the addition cannot be made
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException              if numeric overflow occurs
     */
    /*
     * 对当前时间量的值累加amountToAdd个unit单位的时间量
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    @Override
    public OffsetDateTime plus(long amountToAdd, TemporalUnit unit) {
        if(unit instanceof ChronoUnit) {
            return with(dateTime.plus(amountToAdd, unit), offset);
        }
        
        return unit.addTo(this, amountToAdd);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of years added.
     * <p>
     * This method adds the specified amount to the years field in three steps:
     * <ol>
     * <li>Add the input years to the year field</li>
     * <li>Check if the resulting date would be invalid</li>
     * <li>Adjust the day-of-month to the last valid day if necessary</li>
     * </ol>
     * <p>
     * For example, 2008-02-29 (leap year) plus one year would result in the
     * invalid date 2009-02-29 (standard year). Instead of returning an invalid
     * result, the last valid day of the month, 2009-02-28, is selected instead.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param years the years to add, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the years added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加years年
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public OffsetDateTime plusYears(long years) {
        return with(dateTime.plusYears(years), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of months added.
     * <p>
     * This method adds the specified amount to the months field in three steps:
     * <ol>
     * <li>Add the input months to the month-of-year field</li>
     * <li>Check if the resulting date would be invalid</li>
     * <li>Adjust the day-of-month to the last valid day if necessary</li>
     * </ol>
     * <p>
     * For example, 2007-03-31 plus one month would result in the invalid date
     * 2007-04-31. Instead of returning an invalid result, the last valid day
     * of the month, 2007-04-30, is selected instead.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param months the months to add, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the months added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加months月
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public OffsetDateTime plusMonths(long months) {
        return with(dateTime.plusMonths(months), offset);
    }
    
    /**
     * Returns a copy of this OffsetDateTime with the specified number of weeks added.
     * <p>
     * This method adds the specified amount in weeks to the days field incrementing
     * the month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2008-12-31 plus one week would result in 2009-01-07.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param weeks the weeks to add, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the weeks added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加weeks周
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public OffsetDateTime plusWeeks(long weeks) {
        return with(dateTime.plusWeeks(weeks), offset);
    }
    
    /**
     * Returns a copy of this OffsetDateTime with the specified number of days added.
     * <p>
     * This method adds the specified amount to the days field incrementing the
     * month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2008-12-31 plus one day would result in 2009-01-01.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param days the days to add, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the days added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加days天
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public OffsetDateTime plusDays(long days) {
        return with(dateTime.plusDays(days), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of hours added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param hours the hours to add, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the hours added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加hours小时
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public OffsetDateTime plusHours(long hours) {
        return with(dateTime.plusHours(hours), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of minutes added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param minutes the minutes to add, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the minutes added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加minutes分钟
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public OffsetDateTime plusMinutes(long minutes) {
        return with(dateTime.plusMinutes(minutes), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of seconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param seconds the seconds to add, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the seconds added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加seconds秒
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public OffsetDateTime plusSeconds(long seconds) {
        return with(dateTime.plusSeconds(seconds), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of nanoseconds added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanos the nanos to add, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the nanoseconds added, not null
     *
     * @throws DateTimeException if the unit cannot be added to this type
     */
    /*
     * 在当前时间量的值上累加nanos纳秒
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public OffsetDateTime plusNanos(long nanos) {
        return with(dateTime.plusNanos(nanos), offset);
    }
    
    /*▲ 增加 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 减少 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a copy of this date-time with the specified amount subtracted.
     * <p>
     * This returns an {@code OffsetDateTime}, based on this one, with the specified amount subtracted.
     * The amount is typically {@link Period} or {@link Duration} but may be
     * any other type implementing the {@link TemporalAmount} interface.
     * <p>
     * The calculation is delegated to the amount object by calling
     * {@link TemporalAmount#subtractFrom(Temporal)}. The amount implementation is free
     * to implement the subtraction in any way it wishes, however it typically
     * calls back to {@link #minus(long, TemporalUnit)}. Consult the documentation
     * of the amount implementation to determine if it can be successfully subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToSubtract the amount to subtract, not null
     *
     * @return an {@code OffsetDateTime} based on this date-time with the subtraction made, not null
     *
     * @throws DateTimeException   if the subtraction cannot be made
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 对当前时间量的值与参数中的"时间段"求差
     *
     * 如果求差后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"求差"后的新对象再返回。
     */
    @Override
    public OffsetDateTime minus(TemporalAmount amountToSubtract) {
        return (OffsetDateTime) amountToSubtract.subtractFrom(this);
    }
    
    /**
     * Returns a copy of this date-time with the specified amount subtracted.
     * <p>
     * This returns an {@code OffsetDateTime}, based on this one, with the amount
     * in terms of the unit subtracted. If it is not possible to subtract the amount,
     * because the unit is not supported or for some other reason, an exception is thrown.
     * <p>
     * This method is equivalent to {@link #plus(long, TemporalUnit)} with the amount negated.
     * See that method for a full description of how addition, and thus subtraction, works.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToSubtract the amount of the unit to subtract from the result, may be negative
     * @param unit             the unit of the amount to subtract, not null
     *
     * @return an {@code OffsetDateTime} based on this date-time with the specified amount subtracted, not null
     *
     * @throws DateTimeException                if the subtraction cannot be made
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException              if numeric overflow occurs
     */
    /*
     * 对当前时间量的值减去amountToSubtract个unit单位的时间量
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    @Override
    public OffsetDateTime minus(long amountToSubtract, TemporalUnit unit) {
        if(amountToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, unit).plus(1, unit);
        }
        
        return plus(-amountToSubtract, unit);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of years subtracted.
     * <p>
     * This method subtracts the specified amount from the years field in three steps:
     * <ol>
     * <li>Subtract the input years from the year field</li>
     * <li>Check if the resulting date would be invalid</li>
     * <li>Adjust the day-of-month to the last valid day if necessary</li>
     * </ol>
     * <p>
     * For example, 2008-02-29 (leap year) minus one year would result in the
     * invalid date 2007-02-29 (standard year). Instead of returning an invalid
     * result, the last valid day of the month, 2007-02-28, is selected instead.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param years the years to subtract, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the years subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去years年
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public OffsetDateTime minusYears(long years) {
        if(years == Long.MIN_VALUE) {
            return plusYears(Long.MAX_VALUE).plusYears(1);
        }
        
        return plusYears(-years);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of months subtracted.
     * <p>
     * This method subtracts the specified amount from the months field in three steps:
     * <ol>
     * <li>Subtract the input months from the month-of-year field</li>
     * <li>Check if the resulting date would be invalid</li>
     * <li>Adjust the day-of-month to the last valid day if necessary</li>
     * </ol>
     * <p>
     * For example, 2007-03-31 minus one month would result in the invalid date
     * 2007-02-31. Instead of returning an invalid result, the last valid day
     * of the month, 2007-02-28, is selected instead.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param months the months to subtract, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the months subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去months月
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public OffsetDateTime minusMonths(long months) {
        if(months == Long.MIN_VALUE) {
            return plusMonths(Long.MAX_VALUE).plusMonths(1);
        }
        
        return plusMonths(-months);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of weeks subtracted.
     * <p>
     * This method subtracts the specified amount in weeks from the days field decrementing
     * the month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2009-01-07 minus one week would result in 2008-12-31.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param weeks the weeks to subtract, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the weeks subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去weeks周
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public OffsetDateTime minusWeeks(long weeks) {
        if(weeks == Long.MIN_VALUE) {
            return plusWeeks(Long.MAX_VALUE).plusWeeks(1);
        }
        
        return plusWeeks(-weeks);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of days subtracted.
     * <p>
     * This method subtracts the specified amount from the days field decrementing the
     * month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2009-01-01 minus one day would result in 2008-12-31.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param days the days to subtract, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the days subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去days天
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public OffsetDateTime minusDays(long days) {
        if(days == Long.MIN_VALUE) {
            return plusDays(Long.MAX_VALUE).plusDays(1);
        }
        
        return plusDays(-days);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of hours subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param hours the hours to subtract, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the hours subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去hours小时
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public OffsetDateTime minusHours(long hours) {
        if(hours == Long.MIN_VALUE) {
            return plusHours(Long.MAX_VALUE).plusHours(1);
        }
        
        return plusHours(-hours);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of minutes subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param minutes the minutes to subtract, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the minutes subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去minutes分钟
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public OffsetDateTime minusMinutes(long minutes) {
        if(minutes == Long.MIN_VALUE) {
            return plusMinutes(Long.MAX_VALUE).plusMinutes(1);
        }
        
        return plusMinutes(-minutes);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of seconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param seconds the seconds to subtract, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the seconds subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去seconds秒
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public OffsetDateTime minusSeconds(long seconds) {
        if(seconds == Long.MIN_VALUE) {
            return plusSeconds(Long.MAX_VALUE).plusSeconds(1);
        }
        
        return plusSeconds(-seconds);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified number of nanoseconds subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanos the nanos to subtract, may be negative
     *
     * @return an {@code OffsetDateTime} based on this date-time with the nanoseconds subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去nanos纳秒
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public OffsetDateTime minusNanos(long nanos) {
        if(nanos == Long.MIN_VALUE) {
            return plusNanos(Long.MAX_VALUE).plusNanos(1);
        }
        
        return plusNanos(-nanos);
    }
    
    /*▲ 减少 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量单位 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if the specified unit is supported.
     * <p>
     * This checks if the specified unit can be added to, or subtracted from, this date-time.
     * If false, then calling the {@link #plus(long, TemporalUnit)} and
     * {@link #minus(long, TemporalUnit) minus} methods will throw an exception.
     * <p>
     * If the unit is a {@link ChronoUnit} then the query is implemented here.
     * The supported units are:
     * <ul>
     * <li>{@code NANOS}
     * <li>{@code MICROS}
     * <li>{@code MILLIS}
     * <li>{@code SECONDS}
     * <li>{@code MINUTES}
     * <li>{@code HOURS}
     * <li>{@code HALF_DAYS}
     * <li>{@code DAYS}
     * <li>{@code WEEKS}
     * <li>{@code MONTHS}
     * <li>{@code YEARS}
     * <li>{@code DECADES}
     * <li>{@code CENTURIES}
     * <li>{@code MILLENNIA}
     * <li>{@code ERAS}
     * </ul>
     * All other {@code ChronoUnit} instances will return false.
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
    public boolean isSupported(TemporalUnit unit) {
        if(unit instanceof ChronoUnit) {
            return unit != FOREVER;
        }
        
        return unit != null && unit.isSupportedBy(this);
    }
    
    /*▲ 时间量单位 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量字段操作(TemporalAccessor) ███████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if the specified field is supported.
     * <p>
     * This checks if this date-time can be queried for the specified field.
     * If false, then calling the {@link #range(TemporalField) range},
     * {@link #get(TemporalField) get} and {@link #with(TemporalField, long)}
     * methods will throw an exception.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The supported fields are:
     * <ul>
     * <li>{@code NANO_OF_SECOND}
     * <li>{@code NANO_OF_DAY}
     * <li>{@code MICRO_OF_SECOND}
     * <li>{@code MICRO_OF_DAY}
     * <li>{@code MILLI_OF_SECOND}
     * <li>{@code MILLI_OF_DAY}
     * <li>{@code SECOND_OF_MINUTE}
     * <li>{@code SECOND_OF_DAY}
     * <li>{@code MINUTE_OF_HOUR}
     * <li>{@code MINUTE_OF_DAY}
     * <li>{@code HOUR_OF_AMPM}
     * <li>{@code CLOCK_HOUR_OF_AMPM}
     * <li>{@code HOUR_OF_DAY}
     * <li>{@code CLOCK_HOUR_OF_DAY}
     * <li>{@code AMPM_OF_DAY}
     * <li>{@code DAY_OF_WEEK}
     * <li>{@code ALIGNED_DAY_OF_WEEK_IN_MONTH}
     * <li>{@code ALIGNED_DAY_OF_WEEK_IN_YEAR}
     * <li>{@code DAY_OF_MONTH}
     * <li>{@code DAY_OF_YEAR}
     * <li>{@code EPOCH_DAY}
     * <li>{@code ALIGNED_WEEK_OF_MONTH}
     * <li>{@code ALIGNED_WEEK_OF_YEAR}
     * <li>{@code MONTH_OF_YEAR}
     * <li>{@code PROLEPTIC_MONTH}
     * <li>{@code YEAR_OF_ERA}
     * <li>{@code YEAR}
     * <li>{@code ERA}
     * <li>{@code INSTANT_SECONDS}
     * <li>{@code OFFSET_SECONDS}
     * </ul>
     * All other {@code ChronoField} instances will return false.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.isSupportedBy(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the field is supported is determined by the field.
     *
     * @param field the field to check, null returns false
     *
     * @return true if the field is supported on this date-time, false if not
     */
    // 判断当前时间量是否支持指定的时间量字段
    @Override
    public boolean isSupported(TemporalField field) {
        return field instanceof ChronoField || (field != null && field.isSupportedBy(this));
    }
    
    /**
     * Gets the range of valid values for the specified field.
     * <p>
     * The range object expresses the minimum and maximum valid values for a field.
     * This date-time is used to enhance the accuracy of the returned range.
     * If it is not possible to return the range, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return
     * appropriate range instances.
     * All other {@code ChronoField} instances will throw an {@code UnsupportedTemporalTypeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.rangeRefinedBy(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the range can be obtained is determined by the field.
     *
     * @param field the field to query the range for, not null
     *
     * @return the range of valid values for the field, not null
     *
     * @throws DateTimeException                if the range for the field cannot be obtained
     * @throws UnsupportedTemporalTypeException if the field is not supported
     */
    // 返回时间量字段field的取值区间，通常要求当前时间量支持该时间量字段
    @Override
    public ValueRange range(TemporalField field) {
        if(field instanceof ChronoField) {
            if(field == ChronoField.INSTANT_SECONDS || field == ChronoField.OFFSET_SECONDS) {
                return field.range();
            }
            
            return dateTime.range(field);
        }
        
        return field.rangeRefinedBy(this);
    }
    
    /**
     * Gets the value of the specified field from this date-time as an {@code int}.
     * <p>
     * This queries this date-time for the value of the specified field.
     * The returned value will always be within the valid range of values for the field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this date-time, except {@code NANO_OF_DAY}, {@code MICRO_OF_DAY},
     * {@code EPOCH_DAY}, {@code PROLEPTIC_MONTH} and {@code INSTANT_SECONDS} which are too
     * large to fit in an {@code int} and throw an {@code UnsupportedTemporalTypeException}.
     * All other {@code ChronoField} instances will throw an {@code UnsupportedTemporalTypeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.getFrom(TemporalAccessor)}
     * passing {@code this} as the argument. Whether the value can be obtained,
     * and what the value represents, is determined by the field.
     *
     * @param field the field to get, not null
     *
     * @return the value for the field
     *
     * @throws DateTimeException                if a value for the field cannot be obtained or
     *                                          the value is outside the range of valid values for the field
     * @throws UnsupportedTemporalTypeException if the field is not supported or
     *                                          the range of values exceeds an {@code int}
     * @throws ArithmeticException              if numeric overflow occurs
     */
    /*
     * 以int形式返回时间量字段field的值
     *
     * 目前支持的字段包括：
     *
     * ChronoField.OFFSET_SECONDS  - 返回"基于时间偏移的时区ID"部件，即当前时间点位于哪个时区偏移下
     * .........................................................................................
     * ChronoField.INSTANT_SECONDS ×
     * =========================================================================================
     * ChronoField.YEAR                         - 返回"Proleptic年"部件(哪年)
     * ChronoField.MONTH_OF_YEAR                - 返回"月份"部件(哪月)
     * ChronoField.DAY_OF_MONTH                 - 返回"天"部件(哪日)
     * .........................................................................................
     * ChronoField.ERA                          - 计算"Proleptic年"年所在的纪元；在公历系统中，0是公元前，1是公元(后)
     * ChronoField.YEAR_OF_ERA                  - 将"Proleptic年"转换为位于纪元中的[年]后返回
     * ChronoField.DAY_OF_WEEK                  - 计算当前"本地日期"是一周的第几天(周几)
     * ChronoField.DAY_OF_YEAR                  - 计算当前"本地日期"是所在年份的第几天
     * ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH - 如果当前"本地日期"的当月第一天是周一，依次推算当前"本地日期"当月其它天是周几
     * ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR  - 如果当前"本地日期"的当年第一天是周一，依次推算当前"本地日期"当年其它天是周几
     * ChronoField.ALIGNED_WEEK_OF_MONTH        - 如果当前"本地日期"的当月第一天是周一，依次推算当前"本地日期"位于第几周
     * ChronoField.ALIGNED_WEEK_OF_YEAR         - 如果当前"本地日期"的当年第一天是周一，依次推算当前"本地日期"位于第几周
     * ChronoField.PROLEPTIC_MONTH              ×
     * ChronoField.EPOCH_DAY                    ×
     * =========================================================================================
     * ChronoField.HOUR_OF_DAY        - 返回小时部件(几时)
     * ChronoField.MINUTE_OF_HOUR     - 返回分钟部件(几分)
     * ChronoField.SECOND_OF_MINUTE   - 返回秒部件(几秒)
     * ChronoField.NANO_OF_SECOND     - 返回纳秒部件(几纳秒)
     * ..........................................................................................
     * ChronoField.MICRO_OF_SECOND    - 从纳秒部件中计算出包含的微秒数（不要与纳秒部件同时展示）
     * ChronoField.MILLI_OF_SECOND    - 从纳秒部件中计算出包含的毫秒数（不要与纳秒部件同时展示）
     * ChronoField.NANO_OF_DAY        ×
     * ChronoField.MICRO_OF_DAY       ×
     * ChronoField.MILLI_OF_DAY       - 计算当前"本地时间"包含的毫秒数
     * ChronoField.SECOND_OF_DAY      - 计算当前"本地时间"包含的秒数
     * ChronoField.MINUTE_OF_DAY      - 计算当前"本地时间"包含的分钟数
     * ChronoField.HOUR_OF_AMPM       - 计算当前"本地时间"包含的小时是12小时制中的哪个[小时](计数从0~11)
     * ChronoField.CLOCK_HOUR_OF_AMPM - 计算当前"本地时间"包含的小时是12小时制中的哪个[钟点](计数从1~12)
     * ChronoField.CLOCK_HOUR_OF_DAY  - 计算当前"本地时间"包含的小时是24小时制中的哪个[钟点](计数从1~24)
     * ChronoField.AMPM_OF_DAY        - 计算当前"本地时间"位于上午(0)还是下午(1)
     */
    @Override
    public int get(TemporalField field) {
        if(field instanceof ChronoField) {
            switch((ChronoField) field) {
                case INSTANT_SECONDS:
                    throw new UnsupportedTemporalTypeException("Invalid field 'InstantSeconds' for get() method, use getLong() instead");
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
            }
            
            return dateTime.get(field);
        }
        
        return Temporal.super.get(field);
    }
    
    /**
     * Gets the value of the specified field from this date-time as a {@code long}.
     * <p>
     * This queries this date-time for the value of the specified field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this date-time.
     * All other {@code ChronoField} instances will throw an {@code UnsupportedTemporalTypeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.getFrom(TemporalAccessor)}
     * passing {@code this} as the argument. Whether the value can be obtained,
     * and what the value represents, is determined by the field.
     *
     * @param field the field to get, not null
     *
     * @return the value for the field
     *
     * @throws DateTimeException                if a value for the field cannot be obtained
     * @throws UnsupportedTemporalTypeException if the field is not supported
     * @throws ArithmeticException              if numeric overflow occurs
     */
    /*
     * 以long形式返回时间量字段field的值
     *
     * 目前支持的字段包括：
     *
     * ChronoField.OFFSET_SECONDS  - 返回"基于时间偏移的时区ID"部件，即当前时间点位于哪个时区偏移下
     * .....................................................................................................
     * ChronoField.INSTANT_SECONDS = 返回在当前时间点下，UTC时区的纪元秒
     * =====================================================================================================
     * ChronoField.YEAR                         - 返回"Proleptic年"部件(哪年)
     * ChronoField.MONTH_OF_YEAR                - 返回"月份"部件(哪月)
     * ChronoField.DAY_OF_MONTH                 - 返回"天"部件(哪日)
     * ....................................................................................................
     * ChronoField.ERA                          - 计算"Proleptic年"年所在的纪元；在公历系统中，0是公元前，1是公元(后)
     * ChronoField.YEAR_OF_ERA                  - 将"Proleptic年"转换为位于纪元中的[年]后返回
     * ChronoField.DAY_OF_WEEK                  - 计算当前"本地日期"是一周的第几天(周几)
     * ChronoField.DAY_OF_YEAR                  - 计算当前"本地日期"是所在年份的第几天
     * ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH - 如果当前"本地日期"的当月第一天是周一，依次推算当前"本地日期"当月其它天是周几
     * ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR  - 如果当前"本地日期"的当年第一天是周一，依次推算当前"本地日期"当年其它天是周几
     * ChronoField.ALIGNED_WEEK_OF_MONTH        - 如果当前"本地日期"的当月第一天是周一，依次推算当前"本地日期"位于第几周
     * ChronoField.ALIGNED_WEEK_OF_YEAR         - 如果当前"本地日期"的当年第一天是周一，依次推算当前"本地日期"位于第几周
     * ChronoField.PROLEPTIC_MONTH              = 计算当前时间量包含的"Proleptic月"
     * ChronoField.EPOCH_DAY                    = 计算当前时间量包含的"纪元天"
     * =====================================================================================================
     * ChronoField.HOUR_OF_DAY        - 返回小时部件(几时)
     * ChronoField.MINUTE_OF_HOUR     - 返回分钟部件(几分)
     * ChronoField.SECOND_OF_MINUTE   - 返回秒部件(几秒)
     * ChronoField.NANO_OF_SECOND     - 返回纳秒部件(几纳秒)
     * .....................................................................................................
     * ChronoField.MICRO_OF_SECOND    - 从纳秒部件中计算出包含的微秒数（不要与纳秒部件同时展示）
     * ChronoField.MILLI_OF_SECOND    - 从纳秒部件中计算出包含的毫秒数（不要与纳秒部件同时展示）
     * ChronoField.NANO_OF_DAY        = 计算当前"本地时间"包含的纳秒数
     * ChronoField.MICRO_OF_DAY       = 计算当前"本地时间"包含的微秒数
     * ChronoField.MILLI_OF_DAY       - 计算当前"本地时间"包含的毫秒数
     * ChronoField.SECOND_OF_DAY      - 计算当前"本地时间"包含的秒数
     * ChronoField.MINUTE_OF_DAY      - 计算当前"本地时间"包含的分钟数
     * ChronoField.HOUR_OF_AMPM       - 计算当前"本地时间"包含的小时是12小时制中的哪个[小时](计数从0~11)
     * ChronoField.CLOCK_HOUR_OF_AMPM - 计算当前"本地时间"包含的小时是12小时制中的哪个[钟点](计数从1~12)
     * ChronoField.CLOCK_HOUR_OF_DAY  - 计算当前"本地时间"包含的小时是24小时制中的哪个[钟点](计数从1~24)
     * ChronoField.AMPM_OF_DAY        - 计算当前"本地时间"位于上午(0)还是下午(1)
     */
    @Override
    public long getLong(TemporalField field) {
        if(field instanceof ChronoField) {
            switch((ChronoField) field) {
                case INSTANT_SECONDS:
                    return toEpochSecond();
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
            }
            
            return dateTime.getLong(field);
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
    public <R> R query(TemporalQuery<R> query) {
    
        if(query == TemporalQueries.precision()) {
            return (R) NANOS;
        }
    
        if(query == TemporalQueries.chronology()) {
            return (R) IsoChronology.INSTANCE;
        }
    
        if(query == TemporalQueries.zone() || query == TemporalQueries.offset()) {
            return (R) getOffset();
        }
    
        if(query == TemporalQueries.localDate()) {
            return (R) toLocalDate();
        }
    
        if(query == TemporalQueries.localTime()) {
            return (R) toLocalTime();
        }
    
        if(query == TemporalQueries.zoneId()) {
            return null;
        }
    
        /*
         * inline TemporalAccessor.super.query(query) as an optimization
         * non-JDK classes are not permitted to make this optimization
         */
        return query.queryFrom(this);
    }
    
    /*▲ 时间量字段操作(TemporalAccessor) ███████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 整合 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an adjusted copy of this date-time.
     * <p>
     * This returns an {@code OffsetDateTime}, based on this one, with the date-time adjusted.
     * The adjustment takes place using the specified adjuster strategy object.
     * Read the documentation of the adjuster to understand what adjustment will be made.
     * <p>
     * A simple adjuster might simply set the one of the fields, such as the year field.
     * A more complex adjuster might set the date to the last day of the month.
     * A selection of common adjustments is provided in
     * {@link java.time.temporal.TemporalAdjusters TemporalAdjusters}.
     * These include finding the "last day of the month" and "next Wednesday".
     * Key date-time classes also implement the {@code TemporalAdjuster} interface,
     * such as {@link Month} and {@link java.time.MonthDay MonthDay}.
     * The adjuster is responsible for handling special cases, such as the varying
     * lengths of month and leap years.
     * <p>
     * For example this code returns a date on the last day of July:
     * <pre>
     *  import static java.time.Month.*;
     *  import static java.time.temporal.TemporalAdjusters.*;
     *
     *  result = offsetDateTime.with(JULY).with(lastDayOfMonth());
     * </pre>
     * <p>
     * The classes {@link LocalDate}, {@link LocalTime} and {@link ZoneOffset} implement
     * {@code TemporalAdjuster}, thus this method can be used to change the date, time or offset:
     * <pre>
     *  result = offsetDateTime.with(date);
     *  result = offsetDateTime.with(time);
     *  result = offsetDateTime.with(offset);
     * </pre>
     * <p>
     * The result of this method is obtained by invoking the
     * {@link TemporalAdjuster#adjustInto(Temporal)} method on the
     * specified adjuster passing {@code this} as the argument.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param adjuster the adjuster to use, not null
     *
     * @return an {@code OffsetDateTime} based on {@code this} with the adjustment made, not null
     *
     * @throws DateTimeException   if the adjustment cannot be made
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 使用指定的时间量整合器adjuster来构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     */
    @Override
    public OffsetDateTime with(TemporalAdjuster adjuster) {
        if(adjuster instanceof LocalDate || adjuster instanceof LocalTime || adjuster instanceof LocalDateTime) {
            return with(dateTime.with(adjuster), offset);
        }
        
        if(adjuster instanceof Instant) {
            // 使用给定的时间戳构造属于offset时区的"本地日期-时间"对象(时区偏移时间准确)
            return ofInstant((Instant) adjuster, offset);
        }
        
        if(adjuster instanceof ZoneOffset) {
            return with(dateTime, (ZoneOffset) adjuster);
        }
        
        if(adjuster instanceof OffsetDateTime) {
            return (OffsetDateTime) adjuster;
        }
        
        return (OffsetDateTime) adjuster.adjustInto(this);
    }
    
    /**
     * Returns a copy of this date-time with the specified field set to a new value.
     * <p>
     * This returns an {@code OffsetDateTime}, based on this one, with the value
     * for the specified field changed.
     * This can be used to change any supported field, such as the year, month or day-of-month.
     * If it is not possible to set the value, because the field is not supported or for
     * some other reason, an exception is thrown.
     * <p>
     * In some cases, changing the specified field can cause the resulting date-time to become invalid,
     * such as changing the month from 31st January to February would make the day-of-month invalid.
     * In cases like this, the field is responsible for resolving the date. Typically it will choose
     * the previous valid date, which would be the last valid day of February in this example.
     * <p>
     * If the field is a {@link ChronoField} then the adjustment is implemented here.
     * <p>
     * The {@code INSTANT_SECONDS} field will return a date-time with the specified instant.
     * The offset and nano-of-second are unchanged.
     * If the new instant value is outside the valid range then a {@code DateTimeException} will be thrown.
     * <p>
     * The {@code OFFSET_SECONDS} field will return a date-time with the specified offset.
     * The local date-time is unaltered. If the new offset value is outside the valid range
     * then a {@code DateTimeException} will be thrown.
     * <p>
     * The other {@link #isSupported(TemporalField) supported fields} will behave as per
     * the matching method on {@link LocalDateTime#with(TemporalField, long) LocalDateTime}.
     * In this case, the offset is not part of the calculation and will be unchanged.
     * <p>
     * All other {@code ChronoField} instances will throw an {@code UnsupportedTemporalTypeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.adjustInto(Temporal, long)}
     * passing {@code this} as the argument. In this case, the field determines
     * whether and how to adjust the instant.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param field    the field to set in the result, not null
     * @param newValue the new value of the field in the result
     *
     * @return an {@code OffsetDateTime} based on {@code this} with the specified field set, not null
     *
     * @throws DateTimeException                if the field cannot be set
     * @throws UnsupportedTemporalTypeException if the field is not supported
     * @throws ArithmeticException              if numeric overflow occurs
     */
    /*
     * 通过整合指定类型的字段和当前时间量中的其他类型的字段来构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * field   : 待整合的字段(类型)
     * newValue: field的原始值，需要根据filed的类型进行放缩
     *
     * 目前支持传入的字段值包括：
     *
     * ChronoField.OFFSET_SECONDS  - 与[时区偏移秒]进行整合，只会覆盖当前时间量的"基于时区偏移的时区ID"部件
     * .............................................................................................................
     * ChronoField.INSTANT_SECONDS - 与[纪元秒]进行整合，这会影响到原时间量对象的"小时"、"分钟"、"秒"部件
     *
     * 遇到以下所有字段时，后面的描述是指如何在内部的"本地日期-时间"部件上生成新的LocalDateTime对象。
     * 当生成新的LocalDateTime对象后，还要继续与内部的时区部件进行整合。
     * =============================================================================================================
     * ChronoField.YEAR                         - 与[Proleptic-年]整合，只会覆盖当前时间量的"Proleptic年"部件
     * ChronoField.MONTH_OF_YEAR                - 与一年中的[月](1, 12)整合，只会覆盖当前时间量的"月份"组件
     * ChronoField.DAY_OF_MONTH                 - 与一月中的[天](1, 28/31)整合，只会覆盖当前时间量的"天"组件
     * .............................................................................................................
     * ChronoField.ERA                          - 与[纪元]整合，即切换公元前与公元(后)
     * ChronoField.YEAR_OF_ERA                  - 与位于纪元中的[年]整合，这会将该年份进行转换后覆盖"Proleptic年"部件
     * ChronoField.DAY_OF_WEEK                  - 与一周中的[天](1, 7)整合，这会在当前时间量的基础上增/减一定的天数，以达到给定的字段值表示的"周几"
     * ChronoField.DAY_OF_YEAR                  - 与一年中的[天](1, 365/366)整合，这会覆盖当前时间量的"月份"组件和"天"组件
     * ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH - 与位于月周对齐中的[天](1, 7)整合，这会在当前时间量的基础上增/减一定的天数，以达到给定的字段值表示的"周几"
     * ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR  - 与位于年周对齐中的[天](1, 7)整合，这会在当前时间量的基础上增/减一定的天数，以达到给定的字段值表示的"周几"
     * ChronoField.ALIGNED_WEEK_OF_MONTH        - 与位于月周对齐中的[第几周](1, 4/5)整合，这会在当前时间量的基础上增/减一定的周数，以达到给定的字段值表示的"第几周"
     * ChronoField.ALIGNED_WEEK_OF_YEAR         - 与位于年周对齐中的[第几周](1, 53)整合，这会在当前时间量的基础上增/减一定的周数，以达到给定的字段值表示的"第几周"
     * ChronoField.PROLEPTIC_MONTH              - 与[Proleptic-月]整合，这会在当前时间量的基础上增/减一定的月数，以达到给定的字段值表示的目标月
     * ChronoField.EPOCH_DAY                    - 与纪元中的[天](-365243219162L, 365241780471L)整合，这会完全地构造一个新的"本地日期"
     * =============================================================================================================
     * ChronoField.HOUR_OF_DAY        - 与24小时制中的[小时](0, 23)整合，只会覆盖当前时间量的"小时"组件
     * ChronoField.MINUTE_OF_HOUR     - 与一小时内的[分钟](0, 59)整合，只会覆盖当前时间量的"分钟"组件
     * ChronoField.SECOND_OF_MINUTE   - 与一分内的[秒](0, 59)整合，只会覆盖当前时间量的"秒"组件
     * ChronoField.NANO_OF_SECOND     - 与一秒内的[纳秒](0, 999_999_999)整合，只会覆盖当前时间量的"纳秒"组件
     * .............................................................................................................
     * ChronoField.MICRO_OF_SECOND    - 与一秒内的[微秒](0, 999_999)整合，会将其转换为纳秒，然后去覆盖当前时间量的"纳秒"组件
     * ChronoField.MILLI_OF_SECOND    - 与一秒内的[毫秒](0, 999)整合，会将其转换为纳秒，然后去覆盖当前时间量的"纳秒"组件
     * ChronoField.NANO_OF_DAY        - 与一天内的[纳秒](0, 86400L * 1000_000_000L - 1)整合，这会完全地构造一个新的"本地时间"
     * ChronoField.MICRO_OF_DAY       - 与一天内的[微秒](0, 86400L * 1000_000L - 1)整合，这会完全地构造一个新的"本地时间"
     * ChronoField.MILLI_OF_DAY       - 与一天内的[毫秒](0, 86400L * 1000L - 1)整合，这会完全地构造一个新的"本地时间"
     * ChronoField.SECOND_OF_DAY      - 与一天内的[秒](0, 86400L - 1)整合，这会在当前时间量的基础上增/减一定的秒数，以达到给定的字段值表示的时间
     * ChronoField.MINUTE_OF_DAY      - 与一天内的[分钟](0, (24 * 60) - 1)整合，这会在当前时间量的基础上增/减一定的分钟数，以达到给定的字段值表示的时间
     * ChronoField.HOUR_OF_AMPM       - 与12小时制中的[小时](0, 11)整合，这会在当前时间量的基础上增/减一定的小时数，以达到给定的字段值表示的时间
     * ChronoField.CLOCK_HOUR_OF_AMPM - 与12小时制中的[钟点](1, 12)整合，这会在当前时间量的基础上增/减一定的小时数，以达到给定的字段值表示的时间
     * ChronoField.CLOCK_HOUR_OF_DAY  - 与24小时制中的[钟点](1, 24)整合，会将其转换为24小时制的[小时]，然后覆盖当前时间量的"小时"组件
     * ChronoField.AMPM_OF_DAY        - 与一天中的[上午/下午](0, 1)整合，即在当前时间量的基础上增/减12个小时，进行上下午的切换
     */
    @Override
    public OffsetDateTime with(TemporalField field, long newValue) {
        if(field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            
            if(f == ChronoField.INSTANT_SECONDS) {
                // 根据给定的纪元秒与当前时间量的纳秒偏移构造一个时间戳
                Instant instant = Instant.ofEpochSecond(newValue, getNano());
                
                // 使用给定的时间戳构造属于offset时区的"本地日期-时间"对象(时区偏移时间准确)
                return ofInstant(instant, offset);
            }
            
            if(f == ChronoField.OFFSET_SECONDS) {
                // 确保字段field的取值区间在int的范围内，且给定的值newValue落在field的取值区间中，否则抛异常
                int seconds = f.checkValidIntValue(newValue);
                
                // 构造基于时间偏移的时区ID，其时间偏移为seconds秒
                ZoneOffset offset = ZoneOffset.ofTotalSeconds(seconds);
                
                // 使用dateTime构造属于offset的"本地日期-时间"
                return with(dateTime, offset);
            }
            
            // 通过整合指定类型的字段和dateTime中的其他类型的字段来构造时间量对象
            LocalDateTime newDateTime = dateTime.with(field, newValue);
            
            // 使用newDateTime构造属于offset的"本地日期-时间"
            return with(newDateTime, offset);
        }
        
        return field.adjustInto(this, newValue);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the year altered.
     * <p>
     * The time and offset do not affect the calculation and will be the same in the result.
     * If the day-of-month is invalid for the year, it will be changed to the last valid day of the month.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param year the year to set in the result, from MIN_YEAR to MAX_YEAR
     *
     * @return an {@code OffsetDateTime} based on this date-time with the requested year, not null
     *
     * @throws DateTimeException if the year value is invalid
     */
    /*
     * 将指定的"年"整合到当前时间量中以构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：年份
     */
    public OffsetDateTime withYear(int year) {
        return with(dateTime.withYear(year), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the month-of-year altered.
     * <p>
     * The time and offset do not affect the calculation and will be the same in the result.
     * If the day-of-month is invalid for the year, it will be changed to the last valid day of the month.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param month the month-of-year to set in the result, from 1 (January) to 12 (December)
     *
     * @return an {@code OffsetDateTime} based on this date-time with the requested month, not null
     *
     * @throws DateTimeException if the month-of-year value is invalid
     */
    /*
     * 将指定的"月"整合到当前时间量中以构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：月份
     */
    public OffsetDateTime withMonth(int month) {
        return with(dateTime.withMonth(month), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the day-of-month altered.
     * <p>
     * If the resulting {@code OffsetDateTime} is invalid, an exception is thrown.
     * The time and offset do not affect the calculation and will be the same in the result.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param dayOfMonth the day-of-month to set in the result, from 1 to 28-31
     *
     * @return an {@code OffsetDateTime} based on this date-time with the requested day, not null
     *
     * @throws DateTimeException if the day-of-month value is invalid,
     *                           or if the day-of-month is invalid for the month-year
     */
    /*
     * 将"一月中的天"整合到当前时间量中以构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：天
     */
    public OffsetDateTime withDayOfMonth(int dayOfMonth) {
        return with(dateTime.withDayOfMonth(dayOfMonth), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the day-of-year altered.
     * <p>
     * The time and offset do not affect the calculation and will be the same in the result.
     * If the resulting {@code OffsetDateTime} is invalid, an exception is thrown.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param dayOfYear the day-of-year to set in the result, from 1 to 365-366
     *
     * @return an {@code OffsetDateTime} based on this date with the requested day, not null
     *
     * @throws DateTimeException if the day-of-year value is invalid,
     *                           or if the day-of-year is invalid for the year
     */
    /*
     * 将"一年中的天"整合到当前时间量中以构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：月份、天
     */
    public OffsetDateTime withDayOfYear(int dayOfYear) {
        return with(dateTime.withDayOfYear(dayOfYear), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the hour-of-day altered.
     * <p>
     * The date and offset do not affect the calculation and will be the same in the result.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param hour the hour-of-day to set in the result, from 0 to 23
     *
     * @return an {@code OffsetDateTime} based on this date-time with the requested hour, not null
     *
     * @throws DateTimeException if the hour value is invalid
     */
    /*
     * 将指定的"小时"整合到当前时间量中以构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：小时
     */
    public OffsetDateTime withHour(int hour) {
        return with(dateTime.withHour(hour), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the minute-of-hour altered.
     * <p>
     * The date and offset do not affect the calculation and will be the same in the result.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param minute the minute-of-hour to set in the result, from 0 to 59
     *
     * @return an {@code OffsetDateTime} based on this date-time with the requested minute, not null
     *
     * @throws DateTimeException if the minute value is invalid
     */
    /*
     * 将指定的"分钟"整合到当前时间量中以构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：分钟
     */
    public OffsetDateTime withMinute(int minute) {
        return with(dateTime.withMinute(minute), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the second-of-minute altered.
     * <p>
     * The date and offset do not affect the calculation and will be the same in the result.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param second the second-of-minute to set in the result, from 0 to 59
     *
     * @return an {@code OffsetDateTime} based on this date-time with the requested second, not null
     *
     * @throws DateTimeException if the second value is invalid
     */
    /*
     * 将指定的"秒"整合到当前时间量中以构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：秒
     */
    public OffsetDateTime withSecond(int second) {
        return with(dateTime.withSecond(second), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the nano-of-second altered.
     * <p>
     * The date and offset do not affect the calculation and will be the same in the result.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param nanoOfSecond the nano-of-second to set in the result, from 0 to 999,999,999
     *
     * @return an {@code OffsetDateTime} based on this date-time with the requested nanosecond, not null
     *
     * @throws DateTimeException if the nano value is invalid
     */
    /*
     * 将指定的"纳秒"整合到当前时间量中以构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：纳秒
     */
    public OffsetDateTime withNano(int nanoOfSecond) {
        return with(dateTime.withNano(nanoOfSecond), offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified offset ensuring
     * that the result has the same local date-time.
     * <p>
     * This method returns an object with the same {@code LocalDateTime} and the specified {@code ZoneOffset}.
     * No calculation is needed or performed.
     * For example, if this time represents {@code 2007-12-03T10:30+02:00} and the offset specified is
     * {@code +03:00}, then this method will return {@code 2007-12-03T10:30+03:00}.
     * <p>
     * To take into account the difference between the offsets, and adjust the time fields,
     * use {@link #withOffsetSameInstant}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param offset the zone offset to change to, not null
     *
     * @return an {@code OffsetDateTime} based on this date-time with the requested offset, not null
     */
    /*
     * 将指定的"时区偏移"整合到当前时间量中以构造时间量对象。
     * 该操作只是简单地更新"时区偏移"部件。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件："时区偏移"部件
     */
    public OffsetDateTime withOffsetSameLocal(ZoneOffset offset) {
        return with(dateTime, offset);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the specified offset ensuring
     * that the result is at the same instant.
     * <p>
     * This method returns an object with the specified {@code ZoneOffset} and a {@code LocalDateTime}
     * adjusted by the difference between the two offsets.
     * This will result in the old and new objects representing the same instant.
     * This is useful for finding the local time in a different offset.
     * For example, if this time represents {@code 2007-12-03T10:30+02:00} and the offset specified is
     * {@code +03:00}, then this method will return {@code 2007-12-03T11:30+03:00}.
     * <p>
     * To change the offset without adjusting the local time use {@link #withOffsetSameLocal}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param offset the zone offset to change to, not null
     *
     * @return an {@code OffsetDateTime} based on this date-time with the requested offset, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 将指定的"时区偏移"整合到当前时间量中以构造时间量对象。
     * 该操作会先将"本地日期-时间"部件的值从原先的时区调整到offset时区，然后用offset更新"时区偏移"部件。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件："本地日期-时间"部件、"时区偏移"部件
     */
    public OffsetDateTime withOffsetSameInstant(ZoneOffset offset) {
        if(offset.equals(this.offset)) {
            return this;
        }
        
        int difference = offset.getTotalSeconds() - this.offset.getTotalSeconds();
        
        // 将"本地时间"更新到offset时区
        LocalDateTime adjusted = dateTime.plusSeconds(difference);
        
        return new OffsetDateTime(adjusted, offset);
    }
    
    /**
     * Adjusts the specified temporal object to have the same offset, date
     * and time as this object.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with the offset, date and time changed to be the same as this.
     * <p>
     * The adjustment is equivalent to using {@link Temporal#with(TemporalField, long)}
     * three times, passing {@link ChronoField#EPOCH_DAY},
     * {@link ChronoField#NANO_OF_DAY} and {@link ChronoField#OFFSET_SECONDS} as the fields.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#with(TemporalAdjuster)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisOffsetDateTime.adjustInto(temporal);
     *   temporal = temporal.with(thisOffsetDateTime);
     * </pre>
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param temporal the target object to be adjusted, not null
     *
     * @return the adjusted object, not null
     *
     * @throws DateTimeException   if unable to make the adjustment
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 拿当前时间量中的特定字段与时间量temporal中的其他字段进行整合。
     *
     * 如果整合后的值与temporal中原有的值相等，则可以直接使用temporal本身；否则，会返回新构造的时间量对象。
     *
     * 注：通常，这会用到当前时间量的所有部件信息
     *
     *
     * 当前时间量参与整合字段包括：
     * ChronoField.EPOCH_DAY      - 当前时间量的日期部件中包含的纪元天
     * ChronoField.NANO_OF_DAY    - 当前时间量的时间部件中包含的纳秒数
     * ChronoField.OFFSET_SECONDS - 当前时间量的时区偏移秒数
     *
     * 目标时间量temporal的取值可以是：
     * OffsetDateTime
     * ZonedDateTime
     * ChronoZonedDateTimeImpl
     */
    @Override
    public Temporal adjustInto(Temporal temporal) {
        /*
         * OffsetDateTime is treated as three separate fields, not an instant
         * this produces the most consistent set of results overall
         * the offset is set after the date and time, as it is typically a small
         * tweak to the result, with ZonedDateTime frequently ignoring the offset
         */
        
        // 获取当前时间量的日期部件中包含的纪元天
        long epochDay = toLocalDate().toEpochDay();
        
        // 获取当前时间量的时间部件中包含的纳秒数
        long nano = toLocalTime().toNanoOfDay();
        
        // 返回当前时间量的时区偏移秒数
        int offset = getOffset().getTotalSeconds();
        
        return temporal.with(ChronoField.EPOCH_DAY, epochDay).with(ChronoField.NANO_OF_DAY, nano).with(ChronoField.OFFSET_SECONDS, offset);
    }
    
    /*▲ 整合 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Calculates the amount of time until another date-time in terms of the specified unit.
     * <p>
     * This calculates the amount of time between two {@code OffsetDateTime}
     * objects in terms of a single {@code TemporalUnit}.
     * The start and end points are {@code this} and the specified date-time.
     * The result will be negative if the end is before the start.
     * For example, the amount in days between two date-times can be calculated
     * using {@code startDateTime.until(endDateTime, DAYS)}.
     * <p>
     * The {@code Temporal} passed to this method is converted to a
     * {@code OffsetDateTime} using {@link #from(TemporalAccessor)}.
     * If the offset differs between the two date-times, the specified
     * end date-time is normalized to have the same offset as this date-time.
     * <p>
     * The calculation returns a whole number, representing the number of
     * complete units between the two date-times.
     * For example, the amount in months between 2012-06-15T00:00Z and 2012-08-14T23:59Z
     * will only be one month as it is one minute short of two months.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method.
     * The second is to use {@link TemporalUnit#between(Temporal, Temporal)}:
     * <pre>
     *   // these two lines are equivalent
     *   amount = start.until(end, MONTHS);
     *   amount = MONTHS.between(start, end);
     * </pre>
     * The choice should be made based on which makes the code more readable.
     * <p>
     * The calculation is implemented in this method for {@link ChronoUnit}.
     * The units {@code NANOS}, {@code MICROS}, {@code MILLIS}, {@code SECONDS},
     * {@code MINUTES}, {@code HOURS} and {@code HALF_DAYS}, {@code DAYS},
     * {@code WEEKS}, {@code MONTHS}, {@code YEARS}, {@code DECADES},
     * {@code CENTURIES}, {@code MILLENNIA} and {@code ERAS} are supported.
     * Other {@code ChronoUnit} values will throw an exception.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.between(Temporal, Temporal)}
     * passing {@code this} as the first argument and the converted input temporal
     * as the second argument.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param endExclusive the end date, exclusive, which is converted to an {@code OffsetDateTime}, not null
     * @param unit         the unit to measure the amount in, not null
     *
     * @return the amount of time between this date-time and the end date-time
     *
     * @throws DateTimeException                if the amount cannot be calculated, or the end
     *                                          temporal cannot be converted to an {@code OffsetDateTime}
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException              if numeric overflow occurs
     */
    // 计算当前时间量到目标时间量endExclusive之间相差多少个unit单位的时间值
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        OffsetDateTime end = OffsetDateTime.from(endExclusive);
    
        if(unit instanceof ChronoUnit) {
            end = end.withOffsetSameInstant(offset);
            return dateTime.until(end.dateTime, unit);
        }
    
        return unit.between(this, end);
    }
    
    /**
     * Returns a copy of this {@code OffsetDateTime} with the time truncated.
     * <p>
     * Truncation returns a copy of the original date-time with fields
     * smaller than the specified unit set to zero.
     * For example, truncating with the {@link ChronoUnit#MINUTES minutes} unit
     * will set the second-of-minute and nano-of-second field to zero.
     * <p>
     * The unit must have a {@linkplain TemporalUnit#getDuration() duration}
     * that divides into the length of a standard day without remainder.
     * This includes all supplied time units on {@link ChronoUnit} and
     * {@link ChronoUnit#DAYS DAYS}. Other units throw an exception.
     * <p>
     * The offset does not affect the calculation and will be the same in the result.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param unit the unit to truncate to, not null
     *
     * @return an {@code OffsetDateTime} based on this date-time with the time truncated, not null
     *
     * @throws DateTimeException                if unable to truncate
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     */
    /*
     * 截断(对齐)
     *
     * 将当前时间量按照unit单位进行截断(对齐)，返回截断(对齐)后的新对象。
     *
     * 注：要求unit为"时间"单位
     */
    public OffsetDateTime truncatedTo(TemporalUnit unit) {
        return with(dateTime.truncatedTo(unit), offset);
    }
    
    /**
     * Checks if the instant of this date-time is after that of the specified date-time.
     * <p>
     * This method differs from the comparison in {@link #compareTo} and {@link #equals} in that it
     * only compares the instant of the date-time. This is equivalent to using
     * {@code dateTime1.toInstant().isAfter(dateTime2.toInstant());}.
     *
     * @param other the other date-time to compare to, not null
     *
     * @return true if this is after the instant of the specified date-time
     */
    // 判断当前日期-时间是否晚于参数中指定的日期-时间
    public boolean isAfter(OffsetDateTime other) {
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
     *
     * @param other the other date-time to compare to, not null
     *
     * @return true if this is before the instant of the specified date-time
     */
    // 判断当前日期-时间是否早于参数中指定的日期-时间
    public boolean isBefore(OffsetDateTime other) {
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
     *
     * @param other the other date-time to compare to, not null
     *
     * @return true if the instant equals the instant of the specified date-time
     */
    // 判断当前日期-时间与参数中指定的日期-时间是否相等
    public boolean isEqual(OffsetDateTime other) {
        return toEpochSecond() == other.toEpochSecond() && toLocalTime().getNano() == other.toLocalTime().getNano();
    }
    
    /**
     * Gets a comparator that compares two {@code OffsetDateTime} instances
     * based solely on the instant.
     * <p>
     * This method differs from the comparison in {@link #compareTo} in that it
     * only compares the underlying instant.
     *
     * @return a comparator that compares in time-line order
     *
     * @see #isAfter
     * @see #isBefore
     * @see #isEqual
     */
    // 返回一个外部比较器，以比较两个"本地日期-时间"的早晚
    public static Comparator<OffsetDateTime> timeLineOrder() {
        return new Comparator<OffsetDateTime>() {
            @Override
            public int compare(OffsetDateTime datetime1, OffsetDateTime datetime2) {
                return compareInstant(datetime1, datetime2);
            }
        };
    }
    
    /**
     * Formats this date-time using the specified formatter.
     * <p>
     * This date-time will be passed to the formatter to produce a string.
     *
     * @param formatter the formatter to use, not null
     *
     * @return the formatted date-time string, not null
     *
     * @throws DateTimeException if an error occurs during printing
     */
    // 将当前日期-时间转换为一个指定格式的字符串后返回
    public String format(DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns a new date-time based on this one, returning {@code this} where possible.
     *
     * @param dateTime the date-time to create with, not null
     * @param offset   the zone offset to create with, not null
     */
    // 使用dateTime构造属于offset的"本地日期-时间"
    private OffsetDateTime with(LocalDateTime dateTime, ZoneOffset offset) {
        if(this.dateTime == dateTime && this.offset.equals(offset)) {
            return this;
        }
        
        return new OffsetDateTime(dateTime, offset);
    }
    
    /**
     * Compares this {@code OffsetDateTime} to another date-time.
     * The comparison is based on the instant.
     *
     * @param datetime1 the first date-time to compare, not null
     * @param datetime2 the other date-time to compare to, not null
     *
     * @return the comparator value, negative if less, positive if greater
     */
    // 比较两个"本地日期-时间"的早晚
    private static int compareInstant(OffsetDateTime datetime1, OffsetDateTime datetime2) {
        // 如果时区偏移相等，则直接比较日期-时间部件
        if(datetime1.getOffset().equals(datetime2.getOffset())) {
            return datetime1.toLocalDateTime().compareTo(datetime2.toLocalDateTime());
        }
        
        // 统一转换到UTC时区的纪元秒下再比较
        int cmp = Long.compare(datetime1.toEpochSecond(), datetime2.toEpochSecond());
        if(cmp == 0) {
            // 比较纳秒偏移
            cmp = datetime1.toLocalTime().getNano() - datetime2.toLocalTime().getNano();
        }
        
        return cmp;
    }
    
    
    /**
     * Compares this date-time to another date-time.
     * <p>
     * The comparison is based on the instant then on the local date-time.
     * It is "consistent with equals", as defined by {@link Comparable}.
     * <p>
     * For example, the following is the comparator order:
     * <ol>
     * <li>{@code 2008-12-03T10:30+01:00}</li>
     * <li>{@code 2008-12-03T11:00+01:00}</li>
     * <li>{@code 2008-12-03T12:00+02:00}</li>
     * <li>{@code 2008-12-03T11:30+01:00}</li>
     * <li>{@code 2008-12-03T12:00+01:00}</li>
     * <li>{@code 2008-12-03T12:30+01:00}</li>
     * </ol>
     * Values #2 and #3 represent the same instant on the time-line.
     * When two values represent the same instant, the local date-time is compared
     * to distinguish them. This step is needed to make the ordering
     * consistent with {@code equals()}.
     *
     * @param other the other date-time to compare to, not null
     *
     * @return the comparator value, negative if less, positive if greater
     */
    @Override
    public int compareTo(OffsetDateTime other) {
        int cmp = compareInstant(this, other);
        if(cmp == 0) {
            cmp = toLocalDateTime().compareTo(other.toLocalDateTime());
        }
        return cmp;
    }
    
    /**
     * Outputs this date-time as a {@code String}, such as {@code 2007-12-03T10:15:30+01:00}.
     * <p>
     * The output will be one of the following ISO-8601 formats:
     * <ul>
     * <li>{@code uuuu-MM-dd'T'HH:mmXXXXX}</li>
     * <li>{@code uuuu-MM-dd'T'HH:mm:ssXXXXX}</li>
     * <li>{@code uuuu-MM-dd'T'HH:mm:ss.SSSXXXXX}</li>
     * <li>{@code uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXXXX}</li>
     * <li>{@code uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSXXXXX}</li>
     * </ul>
     * The format used will be the shortest that outputs the full value of
     * the time where the omitted parts are implied to be zero.
     *
     * @return a string representation of this date-time, not null
     */
    @Override
    public String toString() {
        return dateTime.toString() + offset.toString();
    }
    
    /**
     * Checks if this date-time is equal to another date-time.
     * <p>
     * The comparison is based on the local date-time and the offset.
     * To compare for the same instant on the time-line, use {@link #isEqual}.
     * Only objects of type {@code OffsetDateTime} are compared, other types return false.
     *
     * @param obj the object to check, null returns false
     *
     * @return true if this is equal to the other date-time
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj instanceof OffsetDateTime) {
            OffsetDateTime other = (OffsetDateTime) obj;
            return dateTime.equals(other.dateTime) && offset.equals(other.offset);
        }
        return false;
    }
    
    /**
     * A hash code for this date-time.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return dateTime.hashCode() ^ offset.hashCode();
    }
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 2287754244819255394L;
    
    /**
     * Defend against malicious streams.
     *
     * @param s the stream to read
     *
     * @throws InvalidObjectException always
     */
    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }
    
    static OffsetDateTime readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        LocalDateTime dateTime = LocalDateTime.readExternal(in);
        ZoneOffset offset = ZoneOffset.readExternal(in);
        return OffsetDateTime.of(dateTime, offset);
    }
    
    void writeExternal(ObjectOutput out) throws IOException {
        dateTime.writeExternal(out);
        offset.writeExternal(out);
    }
    
    /**
     * Writes the object using a
     * <a href="../../serialized-form.html#java.time.Ser">dedicated serialized form</a>.
     *
     * @return the instance of {@code Ser}, not null
     *
     * @serialData <pre>
     *  out.writeByte(10);  // identifies an OffsetDateTime
     *  // the <a href="../../serialized-form.html#java.time.LocalDateTime">datetime</a> excluding the one byte header
     *  // the <a href="../../serialized-form.html#java.time.ZoneOffset">offset</a> excluding the one byte header
     * </pre>
     */
    private Object writeReplace() {
        return new Ser(Ser.OFFSET_DATE_TIME_TYPE, this);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
