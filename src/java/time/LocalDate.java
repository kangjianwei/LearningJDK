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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.IsoChronology;
import java.time.chrono.IsoEra;
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
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.Objects;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.time.LocalTime.SECONDS_PER_DAY;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.YEAR;

/**
 * A date without a time-zone in the ISO-8601 calendar system,
 * such as {@code 2007-12-03}.
 * <p>
 * {@code LocalDate} is an immutable date-time object that represents a date,
 * often viewed as year-month-day. Other date fields, such as day-of-year,
 * day-of-week and week-of-year, can also be accessed.
 * For example, the value "2nd October 2007" can be stored in a {@code LocalDate}.
 * <p>
 * This class does not store or represent a time or time-zone.
 * Instead, it is a description of the date, as used for birthdays.
 * It cannot represent an instant on the time-line without additional information
 * such as an offset or time-zone.
 * <p>
 * The ISO-8601 calendar system is the modern civil calendar system used today
 * in most of the world. It is equivalent to the proleptic Gregorian calendar
 * system, in which today's rules for leap years are applied for all time.
 * For most applications written today, the ISO-8601 rules are entirely suitable.
 * However, any application that makes use of historical dates, and requires them
 * to be accurate will find the ISO-8601 approach unsuitable.
 *
 * <p>
 * This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; use of identity-sensitive operations (including reference equality
 * ({@code ==}), identity hash code, or synchronization) on instances of
 * {@code LocalDate} may have unpredictable results and should be avoided.
 * The {@code equals} method should be used for comparisons.
 *
 * @implSpec This class is immutable and thread-safe.
 * @since 1.8
 */
// "本地日期"，"日期"基于[ISO]历法系统
public final class LocalDate implements Temporal, TemporalAdjuster, ChronoLocalDate, Serializable {
    
    /**
     * The number of days in a 400 year cycle.
     */
    /*
     * 每400个公历年包含的天数
     *
     * 参见：Year#isLeap(year)中关于闰年的描述。
     */
    private static final int DAYS_PER_CYCLE = 146097;
    
    /**
     * The number of days from year zero to year 1970.
     * There are five 400 year cycles from year zero to 2000.
     * There are 7 leap years from 1970 to 2000.
     */
    /*
     * 公历0年到公历1970年包含的天数
     *
     * 从0年到2000年之间有5个400年周期。
     * 从1970年到2000年有7个闰年。
     */
    static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);
    
    /**
     * The epoch year {@code LocalDate}, '1970-01-01'.
     */
    public static final LocalDate EPOCH = LocalDate.of(1970, 1, 1); // 新纪元时间
    
    /**
     * The minimum supported {@code LocalDate}, '-999999999-01-01'.
     * This could be used by an application as a "far past" date.
     */
    public static final LocalDate MIN = LocalDate.of(Year.MIN_VALUE, 1, 1);
    /**
     * The maximum supported {@code LocalDate}, '+999999999-12-31'.
     * This could be used by an application as a "far future" date.
     */
    public static final LocalDate MAX = LocalDate.of(Year.MAX_VALUE, 12, 31);
    
    /**
     * The year.
     */
    private final int year;    // "Proleptic年"部件[-999999999, 999999999]
    
    /**
     * The month-of-year.
     */
    private final short month; // "月份"部件[1, 12]
    
    /**
     * The day-of-month.
     */
    private final short day;   // "天"部件[1, 28/31]
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructor, previously validated.
     *
     * @param year       the year to represent, from MIN_YEAR to MAX_YEAR
     * @param month      the month-of-year to represent, not null
     * @param dayOfMonth the day-of-month to represent, valid for year-month, from 1 to 31
     */
    private LocalDate(int year, int month, int dayOfMonth) {
        this.year = year;
        this.month = (short) month;
        this.day = (short) dayOfMonth;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains the current date from the system clock in the default time-zone.
     * <p>
     * This will query the {@link Clock#systemDefaultZone() system clock} in the default
     * time-zone to obtain the current date.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @return the current date using the system clock and default time-zone, not null
     */
    // 基于此刻的UTC时间，构造属于系统默认时区的"本地日期"对象
    public static LocalDate now() {
        // 获取一个系统时钟，其预设的时区ID为系统默认的时区ID
        Clock clock = Clock.systemDefaultZone();
        return now(clock);
    }
    
    /**
     * Obtains the current date from the system clock in the specified time-zone.
     * <p>
     * This will query the {@link Clock#system(ZoneId) system clock} to obtain the current date.
     * Specifying the time-zone avoids dependence on the default time-zone.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @param zone the zone ID to use, not null
     *
     * @return the current date using the system clock, not null
     */
    // 基于此刻的UTC时间，构造属于zone时区的"本地日期"对象
    public static LocalDate now(ZoneId zone) {
        // 获取一个系统时钟，其预设的时区ID为zone
        Clock clock = Clock.system(zone);
        return now(clock);
    }
    
    /**
     * Obtains the current date from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current date - today.
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@link Clock dependency injection}.
     *
     * @param clock the clock to use, not null
     *
     * @return the current date, not null
     */
    // 基于clock提供的时间戳和时区ID构造"本地日期"对象
    public static LocalDate now(Clock clock) {
        Objects.requireNonNull(clock, "clock");
    
        // 获取clock时钟提供的时间戳
        final Instant instant = clock.instant();
        // 获取clock时钟提供的时区ID
        ZoneId zoneId = clock.getZone();
    
        // 使用指定的时间戳和时区ID构造属于zone时区的"本地日期"
        return ofInstant(instant, zoneId);
    }
    
    /**
     * Obtains an instance of {@code LocalDate} from a year, month and day.
     * <p>
     * This returns a {@code LocalDate} with the specified year, month and day-of-month.
     * The day must be valid for the year and month, otherwise an exception will be thrown.
     *
     * @param year       the year to represent, from MIN_YEAR to MAX_YEAR
     * @param month      the month-of-year to represent, not null
     * @param dayOfMonth the day-of-month to represent, from 1 to 31
     *
     * @return the local date, not null
     *
     * @throws DateTimeException if the value of any field is out of range,
     *                           or if the day-of-month is invalid for the month-year
     */
    /*
     * 根据Proleptic年份、月份和月份中的天数构造"本地日期"对象
     *
     * year      : 年份，这里应当传入"Proleptic年"
     * month     : 月份
     * dayOfMonth: 一月中的第几天
     */
    public static LocalDate of(int year, Month month, int dayOfMonth) {
        Objects.requireNonNull(month, "month");
        
        YEAR.checkValidValue(year);
        DAY_OF_MONTH.checkValidValue(dayOfMonth);
        
        return create(year, month.getValue(), dayOfMonth);
    }
    
    /**
     * Obtains an instance of {@code LocalDate} from a year, month and day.
     * <p>
     * This returns a {@code LocalDate} with the specified year, month and day-of-month.
     * The day must be valid for the year and month, otherwise an exception will be thrown.
     *
     * @param year       the year to represent, from MIN_YEAR to MAX_YEAR
     * @param month      the month-of-year to represent, from 1 (January) to 12 (December)
     * @param dayOfMonth the day-of-month to represent, from 1 to 31
     *
     * @return the local date, not null
     *
     * @throws DateTimeException if the value of any field is out of range,
     *                           or if the day-of-month is invalid for the month-year
     */
    /*
     * 根据Proleptic年份、月份和月份中的天数构造"本地日期"对象
     *
     * year      : 年份，这里应当传入"Proleptic年"
     * month     : 月份
     * dayOfMonth: 一月中的第几天
     */
    public static LocalDate of(int year, int month, int dayOfMonth) {
        YEAR.checkValidValue(year);
        MONTH_OF_YEAR.checkValidValue(month);
        DAY_OF_MONTH.checkValidValue(dayOfMonth);
        
        return create(year, month, dayOfMonth);
    }
    
    /**
     * Obtains an instance of {@code LocalDate} from a year and day-of-year.
     * <p>
     * This returns a {@code LocalDate} with the specified year and day-of-year.
     * The day-of-year must be valid for the year, otherwise an exception will be thrown.
     *
     * @param year      the year to represent, from MIN_YEAR to MAX_YEAR
     * @param dayOfYear the day-of-year to represent, from 1 to 366
     *
     * @return the local date, not null
     *
     * @throws DateTimeException if the value of any field is out of range,
     *                           or if the day-of-year is invalid for the year
     */
    /*
     * 根据Proleptic年份和年份中的天数构造"本地日期"对象
     *
     * year     : 年份，这里应当传入"Proleptic年"
     * dayOfYear: 一年中的第几天
     */
    public static LocalDate ofYearDay(int year, int dayOfYear) {
        YEAR.checkValidValue(year);
        DAY_OF_YEAR.checkValidValue(dayOfYear);
        
        // 当前年份是否为"闰年"
        boolean leap = IsoChronology.INSTANCE.isLeapYear(year);
        if(dayOfYear == 366 && !leap) {
            throw new DateTimeException("Invalid date 'DayOfYear 366' as '" + year + "' is not a leap year");
        }
        
        // 计算月份
        Month moy = Month.of((dayOfYear - 1) / 31 + 1);
        
        // 获取当前月份的最后一天是所在年份的第几天
        int monthEnd = moy.firstDayOfYear(leap) + moy.length(leap) - 1;
        
        if(dayOfYear>monthEnd) {
            moy = moy.plus(1);
        }
        
        // 计算出剩余的天数
        int dom = dayOfYear - moy.firstDayOfYear(leap) + 1;
        
        return new LocalDate(year, moy.getValue(), dom);
    }
    
    /**
     * Obtains an instance of {@code LocalDate} from the epoch day count.
     * <p>
     * This returns a {@code LocalDate} with the specified epoch-day.
     * The {@link ChronoField#EPOCH_DAY EPOCH_DAY} is a simple incrementing count
     * of days where day 0 is 1970-01-01. Negative numbers represent earlier days.
     *
     * @param epochDay the Epoch Day to convert, based on the epoch 1970-01-01
     *
     * @return the local date, not null
     *
     * @throws DateTimeException if the epoch day exceeds the supported date range
     */
    // 根据给定的纪元天构造"本地日期"对象
    public static LocalDate ofEpochDay(long epochDay) {
        EPOCH_DAY.checkValidValue(epochDay);
    
        long zeroDay = epochDay + DAYS_0000_TO_1970;
    
        // find the march-based year
        zeroDay -= 60;  // adjust to 0000-03-01 so leap day is at end of four year cycle
        long adjust = 0;
        if(zeroDay<0) {
            // adjust negative years to positive for calculation
            long adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1;
            adjust = adjustCycles * 400;
            zeroDay += -adjustCycles * DAYS_PER_CYCLE;
        }
    
        long yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if(doyEst<0) {
            // fix estimate
            yearEst--;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
    
        yearEst += adjust;  // reset any negative year
        int marchDoy0 = (int) doyEst;
    
        // convert march-based values back to january-based
        int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        int month = (marchMonth0 + 2) % 12 + 1;
        int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst += marchMonth0 / 10;
    
        // check year now we are certain it is correct
        int year = YEAR.checkValidIntValue(yearEst);
        return new LocalDate(year, month, dom);
    }
    
    /**
     * Obtains an instance of {@code LocalDate} from an {@code Instant} and zone ID.
     * <p>
     * This creates a local date based on the specified instant.
     * First, the offset from UTC/Greenwich is obtained using the zone ID and instant,
     * which is simple as there is only one valid offset for each instant.
     * Then, the instant and offset are used to calculate the local date.
     *
     * @param instant the instant to create the date from, not null
     * @param zone    the time-zone, which may be an offset, not null
     *
     * @return the local date, not null
     *
     * @throws DateTimeException if the result exceeds the supported range
     * @since 9
     */
    // 使用指定的时间戳和时区ID构造属于zone时区的"本地日期"对象
    public static LocalDate ofInstant(Instant instant, ZoneId zone) {
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(zone, "zone");
        
        // 获取与zone对应的"时区规则集"
        ZoneRules rules = zone.getRules();
        /*
         * 获取zone时区在instant时刻的"实际偏移"。
         * 这里可以返回一个准确的"实际偏移"。
         */
        ZoneOffset offset = rules.getOffset(instant);
        
        // 计算instant在zone时区的纪元秒
        long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
        
        // 根据纪元秒计算纪元天
        long localEpochDay = Math.floorDiv(localSecond, SECONDS_PER_DAY);
        
        // 根据给定的纪元天构造"本地日期"
        return ofEpochDay(localEpochDay);
    }
    
    /**
     * Obtains an instance of {@code LocalDate} from a temporal object.
     * <p>
     * This obtains a local date based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code LocalDate}.
     * <p>
     * The conversion uses the {@link TemporalQueries#localDate()} query, which relies
     * on extracting the {@link ChronoField#EPOCH_DAY EPOCH_DAY} field.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code LocalDate::from}.
     *
     * @param temporal the temporal object to convert, not null
     *
     * @return the local date, not null
     *
     * @throws DateTimeException if unable to convert to a {@code LocalDate}
     */
    /*
     * 从temporal中查询LocalDate部件。
     *
     * 如果没有现成的部件，通常需要从temporal中解析出纪元天，
     * 然后使用纪元天构造LocalDate后返回。
     */
    public static LocalDate from(TemporalAccessor temporal) {
        Objects.requireNonNull(temporal, "temporal");
        
        // 从temporal中查询LocalDate部件的信息
        LocalDate date = temporal.query(TemporalQueries.localDate());
        if(date == null) {
            throw new DateTimeException("Unable to obtain LocalDate from TemporalAccessor: " + temporal + " of type " + temporal.getClass().getName());
        }
        
        return date;
    }
    
    /**
     * Obtains an instance of {@code LocalDate} from a text string such as {@code 2007-12-03}.
     * <p>
     * The string must represent a valid date and is parsed using
     * {@link java.time.format.DateTimeFormatter#ISO_LOCAL_DATE}.
     *
     * @param text the text to parse such as "2007-12-03", not null
     *
     * @return the parsed local date, not null
     *
     * @throws DateTimeParseException if the text cannot be parsed
     */
    // 从指定的文本中解析出LocalDate信息，要求该文本符合ISO规范，即类似：2020-01-15
    public static LocalDate parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    /**
     * Obtains an instance of {@code LocalDate} from a text string using a specific formatter.
     * <p>
     * The text is parsed using the formatter, returning a date.
     *
     * @param text      the text to parse, not null
     * @param formatter the formatter to use, not null
     *
     * @return the parsed local date, not null
     *
     * @throws DateTimeParseException if the text cannot be parsed
     */
    // 从指定的文本中解析出LocalDate信息，要求该文本符合指定的格式规范
    public static LocalDate parse(CharSequence text, DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        
        return formatter.parse(text, new TemporalQuery<LocalDate>() {
            @Override
            public LocalDate queryFrom(TemporalAccessor temporal) {
                return from(temporal);
            }
        });
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Combines this date with a time to create a {@code LocalDateTime}.
     * <p>
     * This returns a {@code LocalDateTime} formed from this date at the specified time.
     * All possible combinations of date and time are valid.
     *
     * @param time the time to combine with, not null
     *
     * @return the local date-time formed from this date and the specified time, not null
     */
    // 将当前"本地日期"和指定的"本地时间"整合成一个"本地日期-时间"对象后返回
    @Override
    public LocalDateTime atTime(LocalTime time) {
        return LocalDateTime.of(this, time);
    }
    
    /**
     * Combines this date with a time to create a {@code LocalDateTime}.
     * <p>
     * This returns a {@code LocalDateTime} formed from this date at the
     * specified hour and minute.
     * The seconds and nanosecond fields will be set to zero.
     * The individual time fields must be within their valid range.
     * All possible combinations of date and time are valid.
     *
     * @param hour   the hour-of-day to use, from 0 to 23
     * @param minute the minute-of-hour to use, from 0 to 59
     *
     * @return the local date-time formed from this date and the specified time, not null
     *
     * @throws DateTimeException if the value of any field is out of range
     */
    // 将当前"本地日期"和指定的时间部件整合成一个"本地日期-时间"对象后返回
    public LocalDateTime atTime(int hour, int minute) {
        // 根据指定的时间部件构造"本地时间"对象
        LocalTime localTime = LocalTime.of(hour, minute);
        return atTime(localTime);
    }
    
    /**
     * Combines this date with a time to create a {@code LocalDateTime}.
     * <p>
     * This returns a {@code LocalDateTime} formed from this date at the
     * specified hour, minute and second.
     * The nanosecond field will be set to zero.
     * The individual time fields must be within their valid range.
     * All possible combinations of date and time are valid.
     *
     * @param hour   the hour-of-day to use, from 0 to 23
     * @param minute the minute-of-hour to use, from 0 to 59
     * @param second the second-of-minute to represent, from 0 to 59
     *
     * @return the local date-time formed from this date and the specified time, not null
     *
     * @throws DateTimeException if the value of any field is out of range
     */
    // 将当前"本地日期"和指定的时间部件整合成一个"本地日期-时间"对象后返回
    public LocalDateTime atTime(int hour, int minute, int second) {
        // 根据指定的时间部件构造"本地时间"对象
        LocalTime localTime = LocalTime.of(hour, minute, second);
        return atTime(localTime);
    }
    
    /**
     * Combines this date with a time to create a {@code LocalDateTime}.
     * <p>
     * This returns a {@code LocalDateTime} formed from this date at the
     * specified hour, minute, second and nanosecond.
     * The individual time fields must be within their valid range.
     * All possible combinations of date and time are valid.
     *
     * @param hour         the hour-of-day to use, from 0 to 23
     * @param minute       the minute-of-hour to use, from 0 to 59
     * @param second       the second-of-minute to represent, from 0 to 59
     * @param nanoOfSecond the nano-of-second to represent, from 0 to 999,999,999
     *
     * @return the local date-time formed from this date and the specified time, not null
     *
     * @throws DateTimeException if the value of any field is out of range
     */
    // 将当前"本地日期"和指定的时间部件整合成一个"本地日期-时间"对象后返回
    public LocalDateTime atTime(int hour, int minute, int second, int nanoOfSecond) {
        // 根据指定的时间部件构造"本地时间"对象
        LocalTime localTime = LocalTime.of(hour, minute, second, nanoOfSecond);
        return atTime(localTime);
    }
    
    /**
     * Combines this date with an offset time to create an {@code OffsetDateTime}.
     * <p>
     * This returns an {@code OffsetDateTime} formed from this date at the specified time.
     * All possible combinations of date and time are valid.
     *
     * @param time the time to combine with, not null
     *
     * @return the offset date-time formed from this date and the specified time, not null
     */
    /*
     * 将当前"本地日期"和指定的"本地时间"整合成一个"本地日期-时间"对象后返回。
     * 返回的OffsetDateTime中使用了参数time中的时区偏移信息。
     */
    public OffsetDateTime atTime(OffsetTime time) {
        // 从time中获取"本地时间"部件
        LocalTime localTime = time.toLocalTime();
        // 从time中获取"时区偏移"部件
        ZoneOffset zoneOffset = time.getOffset();
        
        // 使用当前"本地日期"和localTime构造一个"本地日期-时间"对象
        LocalDateTime localDateTime = LocalDateTime.of(this, localTime);
        
        // 使用localDateTime构造属于zoneOffset的"本地日期-时间"对象
        return OffsetDateTime.of(localDateTime, zoneOffset);
    }
    
    /**
     * Combines this date with the time of midnight to create a {@code LocalDateTime}
     * at the start of this date.
     * <p>
     * This returns a {@code LocalDateTime} formed from this date at the time of
     * midnight, 00:00, at the start of this date.
     *
     * @return the local date-time of midnight at the start of this date, not null
     */
    // 将当前"本地日期"和一个代表一天中起始的"本地时间"整合成一个"本地日期-时间"对象后返回
    public LocalDateTime atStartOfDay() {
        return LocalDateTime.of(this, LocalTime.MIDNIGHT);
    }
    
    /**
     * Returns a zoned date-time from this date at the earliest valid time according
     * to the rules in the time-zone.
     * <p>
     * Time-zone rules, such as daylight savings, mean that not every local date-time
     * is valid for the specified zone, thus the local date-time may not be midnight.
     * <p>
     * In most cases, there is only one valid offset for a local date-time.
     * In the case of an overlap, there are two valid offsets, and the earlier one is used,
     * corresponding to the first occurrence of midnight on the date.
     * In the case of a gap, the zoned date-time will represent the instant just after the gap.
     * <p>
     * If the zone ID is a {@link ZoneOffset}, then the result always has a time of midnight.
     * <p>
     * To convert to a specific time in a given time-zone call {@link #atTime(LocalTime)}
     * followed by {@link LocalDateTime#atZone(ZoneId)}.
     *
     * @param zone the zone ID to use, not null
     *
     * @return the zoned date-time formed from this date and the earliest valid time for the zone, not null
     */
    /*
     * 将当前"本地日期"和一个代表一天中起始的"本地时间"整合成一个"本地日期-时间"对象后返回。
     * 返回的ZonedDateTime中使用了参数zone中的时区信息。
     */
    public ZonedDateTime atStartOfDay(ZoneId zone) {
        Objects.requireNonNull(zone, "zone");
        
        /*
         * need to handle case where there is a gap from 11:30 to 00:30
         * standard ZDT factory would result in 01:00 rather than 00:30
         */
        LocalDateTime localDateTime = atTime(LocalTime.MIDNIGHT);
        
        // 如果zone是地理时区
        if(!(zone instanceof ZoneOffset)) {
            // 获取与zone对应的"时区规则集"
            ZoneRules rules = zone.getRules();
            // 获取zone时区在localDateTime时刻的偏移转换规则，该规则用来指示如何切换时区的时间偏移
            ZoneOffsetTransition trans = rules.getTransition(localDateTime);
            // 如果localDateTime位于zone时区的"间隙时间"中，则需要将其调整到"间隙时间"之后
            if(trans != null && trans.isGap()) {
                localDateTime = trans.getDateTimeAfter();
            }
        }
        
        return ZonedDateTime.of(localDateTime, zone);
    }
    
    // 返回当前时间量的纪元天
    @Override
    public long toEpochDay() {
        long y = year;
        long m = month;
        
        long total = 0;
        
        total += 365 * y;
        
        if(y >= 0) {
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
        } else {
            total -= y / -4 - y / -100 + y / -400;
        }
        
        total += ((367 * m - 362) / 12);
        total += day - 1;
        if(m>2) {
            total--;
            if(!isLeapYear()) {
                total--;
            }
        }
        
        return total - DAYS_0000_TO_1970;
    }
    
    /**
     * Converts this {@code LocalDate} to the number of seconds since the epoch
     * of 1970-01-01T00:00:00Z.
     * <p>
     * This combines this local date with the specified time and
     * offset to calculate the epoch-second value, which is the
     * number of elapsed seconds from 1970-01-01T00:00:00Z.
     * Instants on the time-line after the epoch are positive, earlier
     * are negative.
     *
     * @param time   the local time, not null
     * @param offset the zone offset, not null
     *
     * @return the number of seconds since the epoch of 1970-01-01T00:00:00Z, may be negative
     *
     * @since 9
     */
    // 将位于offset时区的当前"本地日期"与指定的"本地时间"time捆绑为一个"时间点"，然后计算该本地时间点下，UTC时区的纪元秒
    public long toEpochSecond(LocalTime time, ZoneOffset offset) {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(offset, "offset");
        
        // 返回当前本地日期的纪元天
        long epochDay = toEpochDay();
        // 将"本地时间"time转换为一天中的秒数
        int seconds = time.toSecondOfDay();
        
        // 计算出(本地日期+time)代表的纪元秒
        long epochSec = epochDay * SECONDS_PER_DAY + seconds;
        
        // 减去时区偏移秒数
        epochSec -= offset.getTotalSeconds();
        
        return epochSec;
    }
    
    /*▲ 转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 部件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // (哪年)返回"年份"部件[-999999999, 999999999]
    public int getYear() {
        return year;
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
    // (哪月)返回"月份"部件[1, 12]
    public int getMonthValue() {
        return month;
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
    // (哪月)以Month形式返回"月份"部件
    public Month getMonth() {
        return Month.of(month);
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
        return day;
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
    // (周几)返回当前"本地日期"是所在周的第几天
    public DayOfWeek getDayOfWeek() {
        int dow0 = Math.floorMod(toEpochDay() + 3, 7);
        return DayOfWeek.of(dow0 + 1);
    }
    
    /**
     * Gets the day-of-year field.
     * <p>
     * This method returns the primitive {@code int} value for the day-of-year.
     *
     * @return the day-of-year, from 1 to 365, or 366 in a leap year
     */
    // 返回当前"本地日期"是所在年份的第几天
    public int getDayOfYear() {
        return getMonth().firstDayOfYear(isLeapYear()) + day - 1;
    }
    
    /*▲ 部件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 增加 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a copy of this date with the specified amount added.
     * <p>
     * This returns a {@code LocalDate}, based on this one, with the specified amount added.
     * The amount is typically {@link Period} but may be any other type implementing
     * the {@link TemporalAmount} interface.
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
     * @return a {@code LocalDate} based on this date with the addition made, not null
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
    public LocalDate plus(TemporalAmount amountToAdd) {
        Objects.requireNonNull(amountToAdd, "amountToAdd");
        
        if(amountToAdd instanceof Period) {
            Period periodToAdd = (Period) amountToAdd;
            return plusMonths(periodToAdd.toTotalMonths()).plusDays(periodToAdd.getDays());
        }
        
        return (LocalDate) amountToAdd.addTo(this);
    }
    
    /**
     * Returns a copy of this date with the specified amount added.
     * <p>
     * This returns a {@code LocalDate}, based on this one, with the amount
     * in terms of the unit added. If it is not possible to add the amount, because the
     * unit is not supported or for some other reason, an exception is thrown.
     * <p>
     * In some cases, adding the amount can cause the resulting date to become invalid.
     * For example, adding one month to 31st January would result in 31st February.
     * In cases like this, the unit is responsible for resolving the date.
     * Typically it will choose the previous valid date, which would be the last valid
     * day of February in this example.
     * <p>
     * If the field is a {@link ChronoUnit} then the addition is implemented here.
     * The supported fields behave as follows:
     * <ul>
     * <li>{@code DAYS} -
     *  Returns a {@code LocalDate} with the specified number of days added.
     *  This is equivalent to {@link #plusDays(long)}.
     * <li>{@code WEEKS} -
     *  Returns a {@code LocalDate} with the specified number of weeks added.
     *  This is equivalent to {@link #plusWeeks(long)} and uses a 7 day week.
     * <li>{@code MONTHS} -
     *  Returns a {@code LocalDate} with the specified number of months added.
     *  This is equivalent to {@link #plusMonths(long)}.
     *  The day-of-month will be unchanged unless it would be invalid for the new
     *  month and year. In that case, the day-of-month is adjusted to the maximum
     *  valid value for the new month and year.
     * <li>{@code YEARS} -
     *  Returns a {@code LocalDate} with the specified number of years added.
     *  This is equivalent to {@link #plusYears(long)}.
     *  The day-of-month will be unchanged unless it would be invalid for the new
     *  month and year. In that case, the day-of-month is adjusted to the maximum
     *  valid value for the new month and year.
     * <li>{@code DECADES} -
     *  Returns a {@code LocalDate} with the specified number of decades added.
     *  This is equivalent to calling {@link #plusYears(long)} with the amount
     *  multiplied by 10.
     *  The day-of-month will be unchanged unless it would be invalid for the new
     *  month and year. In that case, the day-of-month is adjusted to the maximum
     *  valid value for the new month and year.
     * <li>{@code CENTURIES} -
     *  Returns a {@code LocalDate} with the specified number of centuries added.
     *  This is equivalent to calling {@link #plusYears(long)} with the amount
     *  multiplied by 100.
     *  The day-of-month will be unchanged unless it would be invalid for the new
     *  month and year. In that case, the day-of-month is adjusted to the maximum
     *  valid value for the new month and year.
     * <li>{@code MILLENNIA} -
     *  Returns a {@code LocalDate} with the specified number of millennia added.
     *  This is equivalent to calling {@link #plusYears(long)} with the amount
     *  multiplied by 1,000.
     *  The day-of-month will be unchanged unless it would be invalid for the new
     *  month and year. In that case, the day-of-month is adjusted to the maximum
     *  valid value for the new month and year.
     * <li>{@code ERAS} -
     *  Returns a {@code LocalDate} with the specified number of eras added.
     *  Only two eras are supported so the amount must be one, zero or minus one.
     *  If the amount is non-zero then the year is changed such that the year-of-era
     *  is unchanged.
     *  The day-of-month will be unchanged unless it would be invalid for the new
     *  month and year. In that case, the day-of-month is adjusted to the maximum
     *  valid value for the new month and year.
     * </ul>
     * <p>
     * All other {@code ChronoUnit} instances will throw an {@code UnsupportedTemporalTypeException}.
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
     * @return a {@code LocalDate} based on this date with the specified amount added, not null
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
    public LocalDate plus(long amountToAdd, TemporalUnit unit) {
        if(unit instanceof ChronoUnit) {
            ChronoUnit f = (ChronoUnit) unit;
            switch(f) {
                case DAYS:
                    return plusDays(amountToAdd);
                case WEEKS:
                    return plusWeeks(amountToAdd);
                case MONTHS:
                    return plusMonths(amountToAdd);
                case YEARS:
                    return plusYears(amountToAdd);
                case DECADES:
                    return plusYears(Math.multiplyExact(amountToAdd, 10));
                case CENTURIES:
                    return plusYears(Math.multiplyExact(amountToAdd, 100));
                case MILLENNIA:
                    return plusYears(Math.multiplyExact(amountToAdd, 1000));
                case ERAS:
                    return with(ERA, Math.addExact(getLong(ERA), amountToAdd));
            }
            
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        
        return unit.addTo(this, amountToAdd);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the specified number of years added.
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
     * @param yearsToAdd the years to add, may be negative
     *
     * @return a {@code LocalDate} based on this date with the years added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加yearsToAdd年
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public LocalDate plusYears(long yearsToAdd) {
        if(yearsToAdd == 0) {
            return this;
        }
        
        int newYear = YEAR.checkValidIntValue(year + yearsToAdd);  // safe overflow
        
        return resolvePreviousValid(newYear, month, day);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the specified number of months added.
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
     * @param monthsToAdd the months to add, may be negative
     *
     * @return a {@code LocalDate} based on this date with the months added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加monthsToAdd月
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public LocalDate plusMonths(long monthsToAdd) {
        if(monthsToAdd == 0) {
            return this;
        }
        
        long monthCount = year * 12L + (month - 1);
        long calcMonths = monthCount + monthsToAdd;  // safe overflow
        int newYear = YEAR.checkValidIntValue(Math.floorDiv(calcMonths, 12));
        int newMonth = Math.floorMod(calcMonths, 12) + 1;
        
        return resolvePreviousValid(newYear, newMonth, day);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the specified number of weeks added.
     * <p>
     * This method adds the specified amount in weeks to the days field incrementing
     * the month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2008-12-31 plus one week would result in 2009-01-07.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param weeksToAdd the weeks to add, may be negative
     *
     * @return a {@code LocalDate} based on this date with the weeks added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加weeksToAdd周
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public LocalDate plusWeeks(long weeksToAdd) {
        return plusDays(Math.multiplyExact(weeksToAdd, 7));
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the specified number of days added.
     * <p>
     * This method adds the specified amount to the days field incrementing the
     * month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2008-12-31 plus one day would result in 2009-01-01.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToAdd the days to add, may be negative
     *
     * @return a {@code LocalDate} based on this date with the days added, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上累加daysToAdd天
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public LocalDate plusDays(long daysToAdd) {
        if(daysToAdd == 0) {
            return this;
        }
        
        // 先进行便捷处理
        long dom = day + daysToAdd;
        
        if(dom>0) {
            // 绝对安全的天数
            if(dom<=28) {
                return new LocalDate(year, month, (int) dom);
            }
            
            // 59th Jan is 28th Feb, 59th Feb is 31st Mar
            if(dom<=59) {
                // 返回当前"本地日期"的月份包含的天数
                long monthLen = lengthOfMonth();
                
                // 相对安全的天数
                if(dom<=monthLen) {
                    return new LocalDate(year, month, (int) dom);
                }
                
                // 进入下一个月
                if(month<12) {
                    return new LocalDate(year, month + 1, (int) (dom - monthLen));
                }
                
                YEAR.checkValidValue(year + 1);
                
                // 进入下一年
                return new LocalDate(year + 1, 1, (int) (dom - monthLen));
            }
        }
        
        // 返回当前时间量的纪元天
        long epochDay = toEpochDay();
        
        // 加上daysToAdd
        long mjDay = Math.addExact(epochDay, daysToAdd);
        
        // 根据给定的纪元天构造"本地日期"
        return LocalDate.ofEpochDay(mjDay);
    }
    
    /*▲ 增加 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 减少 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a copy of this date with the specified amount subtracted.
     * <p>
     * This returns a {@code LocalDate}, based on this one, with the specified amount subtracted.
     * The amount is typically {@link Period} but may be any other type implementing
     * the {@link TemporalAmount} interface.
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
     * @return a {@code LocalDate} based on this date with the subtraction made, not null
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
    public LocalDate minus(TemporalAmount amountToSubtract) {
        Objects.requireNonNull(amountToSubtract, "amountToSubtract");
        
        if(amountToSubtract instanceof Period) {
            Period periodToSubtract = (Period) amountToSubtract;
            return minusMonths(periodToSubtract.toTotalMonths()).minusDays(periodToSubtract.getDays());
        }
        
        return (LocalDate) amountToSubtract.subtractFrom(this);
    }
    
    /**
     * Returns a copy of this date with the specified amount subtracted.
     * <p>
     * This returns a {@code LocalDate}, based on this one, with the amount
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
     * @return a {@code LocalDate} based on this date with the specified amount subtracted, not null
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
    public LocalDate minus(long amountToSubtract, TemporalUnit unit) {
        if(amountToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, unit).plus(1, unit);
        }
        
        return plus(-amountToSubtract, unit);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the specified number of years subtracted.
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
     * @param yearsToSubtract the years to subtract, may be negative
     *
     * @return a {@code LocalDate} based on this date with the years subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去yearsToSubtract年
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public LocalDate minusYears(long yearsToSubtract) {
        if(yearsToSubtract == Long.MIN_VALUE) {
            return plusYears(Long.MAX_VALUE).plusYears(1);
        }
        
        return plusYears(-yearsToSubtract);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the specified number of months subtracted.
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
     * @param monthsToSubtract the months to subtract, may be negative
     *
     * @return a {@code LocalDate} based on this date with the months subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去monthsToSubtract月
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public LocalDate minusMonths(long monthsToSubtract) {
        if(monthsToSubtract == Long.MIN_VALUE) {
            return plusMonths(Long.MAX_VALUE).plusMonths(1);
        }
        
        return plusMonths(-monthsToSubtract);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the specified number of weeks subtracted.
     * <p>
     * This method subtracts the specified amount in weeks from the days field decrementing
     * the month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2009-01-07 minus one week would result in 2008-12-31.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param weeksToSubtract the weeks to subtract, may be negative
     *
     * @return a {@code LocalDate} based on this date with the weeks subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去weeksToSubtract周
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public LocalDate minusWeeks(long weeksToSubtract) {
        if(weeksToSubtract == Long.MIN_VALUE) {
            return plusWeeks(Long.MAX_VALUE).plusWeeks(1);
        }
        
        return plusWeeks(-weeksToSubtract);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the specified number of days subtracted.
     * <p>
     * This method subtracts the specified amount from the days field decrementing the
     * month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2009-01-01 minus one day would result in 2008-12-31.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToSubtract the days to subtract, may be negative
     *
     * @return a {@code LocalDate} based on this date with the days subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported date range
     */
    /*
     * 在当前时间量的值上减去daysToSubtract天
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public LocalDate minusDays(long daysToSubtract) {
        if(daysToSubtract == Long.MIN_VALUE) {
            return plusDays(Long.MAX_VALUE).plusDays(1);
        }
        
        return plusDays(-daysToSubtract);
    }
    
    /*▲ 减少 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量单位 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if the specified unit is supported.
     * <p>
     * This checks if the specified unit can be added to, or subtracted from, this date.
     * If false, then calling the {@link #plus(long, TemporalUnit)} and
     * {@link #minus(long, TemporalUnit) minus} methods will throw an exception.
     * <p>
     * If the unit is a {@link ChronoUnit} then the query is implemented here.
     * The supported units are:
     * <ul>
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
        return ChronoLocalDate.super.isSupported(unit);
    }
    
    /*▲ 时间量单位 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量字段操作(TemporalAccessor) ███████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if the specified field is supported.
     * <p>
     * This checks if this date can be queried for the specified field.
     * If false, then calling the {@link #range(TemporalField) range},
     * {@link #get(TemporalField) get} and {@link #with(TemporalField, long)}
     * methods will throw an exception.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The supported fields are:
     * <ul>
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
     * @return true if the field is supported on this date, false if not
     */
    // 判断当前时间量是否支持指定的时间量字段
    @Override
    public boolean isSupported(TemporalField field) {
        return ChronoLocalDate.super.isSupported(field);
    }
    
    /**
     * Gets the range of valid values for the specified field.
     * <p>
     * The range object expresses the minimum and maximum valid values for a field.
     * This date is used to enhance the accuracy of the returned range.
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
            ChronoField f = (ChronoField) field;
            
            if(f.isDateBased()) {
                switch(f) {
                    case DAY_OF_MONTH:
                        return ValueRange.of(1, lengthOfMonth());
                    case DAY_OF_YEAR:
                        return ValueRange.of(1, lengthOfYear());
                    case ALIGNED_WEEK_OF_MONTH:
                        return ValueRange.of(1, getMonth() == Month.FEBRUARY && !isLeapYear() ? 4 : 5);
                    case YEAR_OF_ERA:
                        return (getYear()<=0 ? ValueRange.of(1, Year.MAX_VALUE + 1) : ValueRange.of(1, Year.MAX_VALUE));
                }
                
                return field.range();
            }
            
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        
        return field.rangeRefinedBy(this);
    }
    
    /**
     * Gets the value of the specified field from this date as an {@code int}.
     * <p>
     * This queries this date for the value of the specified field.
     * The returned value will always be within the valid range of values for the field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this date, except {@code EPOCH_DAY} and {@code PROLEPTIC_MONTH}
     * which are too large to fit in an {@code int} and throw an {@code UnsupportedTemporalTypeException}.
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
     * ChronoField.PROLEPTIC_MONTH              ×
     * ChronoField.EPOCH_DAY                    ×
     */
    @Override
    public int get(TemporalField field) {
        if(field instanceof ChronoField) {
            return get0(field);
        }
        
        return ChronoLocalDate.super.get(field);
    }
    
    /**
     * Gets the value of the specified field from this date as a {@code long}.
     * <p>
     * This queries this date for the value of the specified field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this date.
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
     */
    @Override
    public long getLong(TemporalField field) {
        if(field instanceof ChronoField) {
            if(field == EPOCH_DAY) {
                // 返回当前时间量的纪元天
                return toEpochDay();
            }
            
            if(field == PROLEPTIC_MONTH) {
                // 返回当前时间量的[Proleptic-月]
                return getProlepticMonth();
            }
            
            return get0(field);
        }
        
        return field.getFrom(this);
    }
    
    /**
     * Queries this date using the specified query.
     * <p>
     * This queries this date using the specified query strategy object.
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
        if(query == TemporalQueries.localDate()) {
            return (R) this;
        }
        
        return ChronoLocalDate.super.query(query);
    }
    
    /*▲ 时间量字段操作(TemporalAccessor) ███████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 整合 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an adjusted copy of this date.
     * <p>
     * This returns a {@code LocalDate}, based on this one, with the date adjusted.
     * The adjustment takes place using the specified adjuster strategy object.
     * Read the documentation of the adjuster to understand what adjustment will be made.
     * <p>
     * A simple adjuster might simply set the one of the fields, such as the year field.
     * A more complex adjuster might set the date to the last day of the month.
     * <p>
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
     *  result = localDate.with(JULY).with(lastDayOfMonth());
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
     * @return a {@code LocalDate} based on {@code this} with the adjustment made, not null
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
    public LocalDate with(TemporalAdjuster adjuster) {
        if(adjuster instanceof LocalDate) {
            return (LocalDate) adjuster;
        }
        
        return (LocalDate) adjuster.adjustInto(this);
    }
    
    /**
     * Returns a copy of this date with the specified field set to a new value.
     * <p>
     * This returns a {@code LocalDate}, based on this one, with the value
     * for the specified field changed.
     * This can be used to change any supported field, such as the year, month or day-of-month.
     * If it is not possible to set the value, because the field is not supported or for
     * some other reason, an exception is thrown.
     * <p>
     * In some cases, changing the specified field can cause the resulting date to become invalid,
     * such as changing the month from 31st January to February would make the day-of-month invalid.
     * In cases like this, the field is responsible for resolving the date. Typically it will choose
     * the previous valid date, which would be the last valid day of February in this example.
     * <p>
     * If the field is a {@link ChronoField} then the adjustment is implemented here.
     * The supported fields behave as follows:
     * <ul>
     * <li>{@code DAY_OF_WEEK} -
     *  Returns a {@code LocalDate} with the specified day-of-week.
     *  The date is adjusted up to 6 days forward or backward within the boundary
     *  of a Monday to Sunday week.
     * <li>{@code ALIGNED_DAY_OF_WEEK_IN_MONTH} -
     *  Returns a {@code LocalDate} with the specified aligned-day-of-week.
     *  The date is adjusted to the specified month-based aligned-day-of-week.
     *  Aligned weeks are counted such that the first week of a given month starts
     *  on the first day of that month.
     *  This may cause the date to be moved up to 6 days into the following month.
     * <li>{@code ALIGNED_DAY_OF_WEEK_IN_YEAR} -
     *  Returns a {@code LocalDate} with the specified aligned-day-of-week.
     *  The date is adjusted to the specified year-based aligned-day-of-week.
     *  Aligned weeks are counted such that the first week of a given year starts
     *  on the first day of that year.
     *  This may cause the date to be moved up to 6 days into the following year.
     * <li>{@code DAY_OF_MONTH} -
     *  Returns a {@code LocalDate} with the specified day-of-month.
     *  The month and year will be unchanged. If the day-of-month is invalid for the
     *  year and month, then a {@code DateTimeException} is thrown.
     * <li>{@code DAY_OF_YEAR} -
     *  Returns a {@code LocalDate} with the specified day-of-year.
     *  The year will be unchanged. If the day-of-year is invalid for the
     *  year, then a {@code DateTimeException} is thrown.
     * <li>{@code EPOCH_DAY} -
     *  Returns a {@code LocalDate} with the specified epoch-day.
     *  This completely replaces the date and is equivalent to {@link #ofEpochDay(long)}.
     * <li>{@code ALIGNED_WEEK_OF_MONTH} -
     *  Returns a {@code LocalDate} with the specified aligned-week-of-month.
     *  Aligned weeks are counted such that the first week of a given month starts
     *  on the first day of that month.
     *  This adjustment moves the date in whole week chunks to match the specified week.
     *  The result will have the same day-of-week as this date.
     *  This may cause the date to be moved into the following month.
     * <li>{@code ALIGNED_WEEK_OF_YEAR} -
     *  Returns a {@code LocalDate} with the specified aligned-week-of-year.
     *  Aligned weeks are counted such that the first week of a given year starts
     *  on the first day of that year.
     *  This adjustment moves the date in whole week chunks to match the specified week.
     *  The result will have the same day-of-week as this date.
     *  This may cause the date to be moved into the following year.
     * <li>{@code MONTH_OF_YEAR} -
     *  Returns a {@code LocalDate} with the specified month-of-year.
     *  The year will be unchanged. The day-of-month will also be unchanged,
     *  unless it would be invalid for the new month and year. In that case, the
     *  day-of-month is adjusted to the maximum valid value for the new month and year.
     * <li>{@code PROLEPTIC_MONTH} -
     *  Returns a {@code LocalDate} with the specified proleptic-month.
     *  The day-of-month will be unchanged, unless it would be invalid for the new month
     *  and year. In that case, the day-of-month is adjusted to the maximum valid value
     *  for the new month and year.
     * <li>{@code YEAR_OF_ERA} -
     *  Returns a {@code LocalDate} with the specified year-of-era.
     *  The era and month will be unchanged. The day-of-month will also be unchanged,
     *  unless it would be invalid for the new month and year. In that case, the
     *  day-of-month is adjusted to the maximum valid value for the new month and year.
     * <li>{@code YEAR} -
     *  Returns a {@code LocalDate} with the specified year.
     *  The month will be unchanged. The day-of-month will also be unchanged,
     *  unless it would be invalid for the new month and year. In that case, the
     *  day-of-month is adjusted to the maximum valid value for the new month and year.
     * <li>{@code ERA} -
     *  Returns a {@code LocalDate} with the specified era.
     *  The year-of-era and month will be unchanged. The day-of-month will also be unchanged,
     *  unless it would be invalid for the new month and year. In that case, the
     *  day-of-month is adjusted to the maximum valid value for the new month and year.
     * </ul>
     * <p>
     * In all cases, if the new value is outside the valid range of values for the field
     * then a {@code DateTimeException} will be thrown.
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
     * @return a {@code LocalDate} based on {@code this} with the specified field set, not null
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
     * ChronoField.YEAR                         - 与[Proleptic-年]整合，只会覆盖当前时间量的"Proleptic年"部件
     * ChronoField.MONTH_OF_YEAR                - 与一年中的[月](1, 12)整合，只会覆盖当前时间量的"月份"组件
     * ChronoField.DAY_OF_MONTH                 - 与一月中的[天](1, 28/31)整合，只会覆盖当前时间量的"天"组件
     * ..............................................................................................................................
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
     */
    @Override
    public LocalDate with(TemporalField field, long newValue) {
        if(field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            f.checkValidValue(newValue);
            
            switch(f) {
                case DAY_OF_WEEK:
                    return plusDays(newValue - getDayOfWeek().getValue());
                case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                    return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                    return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                case DAY_OF_MONTH:
                    return withDayOfMonth((int) newValue);
                case DAY_OF_YEAR:
                    return withDayOfYear((int) newValue);
                case EPOCH_DAY:
                    return LocalDate.ofEpochDay(newValue);
                case ALIGNED_WEEK_OF_MONTH:
                    return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_MONTH));
                case ALIGNED_WEEK_OF_YEAR:
                    return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_YEAR));
                case MONTH_OF_YEAR:
                    return withMonth((int) newValue);
                case PROLEPTIC_MONTH:
                    return plusMonths(newValue - getProlepticMonth());
                case YEAR_OF_ERA:
                    return withYear((int) (year >= 1 ? newValue : 1 - newValue));
                case YEAR:
                    return withYear((int) newValue);
                case ERA:
                    return (getLong(ERA) == newValue ? this : withYear(1 - year));
            }
            
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        
        return field.adjustInto(this, newValue);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the year altered.
     * <p>
     * If the day-of-month is invalid for the year, it will be changed to the last valid day of the month.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param year the year to set in the result, from MIN_YEAR to MAX_YEAR
     *
     * @return a {@code LocalDate} based on this date with the requested year, not null
     *
     * @throws DateTimeException if the year value is invalid
     */
    /*
     * 将指定的"Proleptic年"整合到当前时间量中以构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：年份，这里应当传入"Proleptic年"
     */
    public LocalDate withYear(int year) {
        if(this.year == year) {
            return this;
        }
        
        YEAR.checkValidValue(year);
        
        return resolvePreviousValid(year, month, day);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the month-of-year altered.
     * <p>
     * If the day-of-month is invalid for the year, it will be changed to the last valid day of the month.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param month the month-of-year to set in the result, from 1 (January) to 12 (December)
     *
     * @return a {@code LocalDate} based on this date with the requested month, not null
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
    public LocalDate withMonth(int month) {
        if(this.month == month) {
            return this;
        }
        
        MONTH_OF_YEAR.checkValidValue(month);
        
        return resolvePreviousValid(year, month, day);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the day-of-month altered.
     * <p>
     * If the resulting date is invalid, an exception is thrown.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param dayOfMonth the day-of-month to set in the result, from 1 to 28-31
     *
     * @return a {@code LocalDate} based on this date with the requested day, not null
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
    public LocalDate withDayOfMonth(int dayOfMonth) {
        if(this.day == dayOfMonth) {
            return this;
        }
        
        return of(year, month, dayOfMonth);
    }
    
    /**
     * Returns a copy of this {@code LocalDate} with the day-of-year altered.
     * <p>
     * If the resulting date is invalid, an exception is thrown.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param dayOfYear the day-of-year to set in the result, from 1 to 365-366
     *
     * @return a {@code LocalDate} based on this date with the requested day, not null
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
    public LocalDate withDayOfYear(int dayOfYear) {
        if(this.getDayOfYear() == dayOfYear) {
            return this;
        }
        
        return ofYearDay(year, dayOfYear);
    }
    
    /**
     * Adjusts the specified temporal object to have the same date as this object.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with the date changed to be the same as this.
     * <p>
     * The adjustment is equivalent to using {@link Temporal#with(TemporalField, long)}
     * passing {@link ChronoField#EPOCH_DAY} as the field.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#with(TemporalAdjuster)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisLocalDate.adjustInto(temporal);
     *   temporal = temporal.with(thisLocalDate);
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
     * ChronoField.EPOCH_DAY - 当前时间量中包含的纪元天
     *
     * 目标时间量temporal的取值可以是：
     * LocalDate
     * LocalDateTime
     * OffsetDateTime
     * ZonedDateTime
     * ChronoLocalDateTimeImpl
     * ChronoZonedDateTimeImpl
     */
    @Override
    public Temporal adjustInto(Temporal temporal) {
        return ChronoLocalDate.super.adjustInto(temporal);
    }
    
    /*▲ 整合 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Calculates the amount of time until another date in terms of the specified unit.
     * <p>
     * This calculates the amount of time between two {@code LocalDate}
     * objects in terms of a single {@code TemporalUnit}.
     * The start and end points are {@code this} and the specified date.
     * The result will be negative if the end is before the start.
     * The {@code Temporal} passed to this method is converted to a
     * {@code LocalDate} using {@link #from(TemporalAccessor)}.
     * For example, the amount in days between two dates can be calculated
     * using {@code startDate.until(endDate, DAYS)}.
     * <p>
     * The calculation returns a whole number, representing the number of
     * complete units between the two dates.
     * For example, the amount in months between 2012-06-15 and 2012-08-14
     * will only be one month as it is one day short of two months.
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
     * The units {@code DAYS}, {@code WEEKS}, {@code MONTHS}, {@code YEARS},
     * {@code DECADES}, {@code CENTURIES}, {@code MILLENNIA} and {@code ERAS}
     * are supported. Other {@code ChronoUnit} values will throw an exception.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.between(Temporal, Temporal)}
     * passing {@code this} as the first argument and the converted input temporal
     * as the second argument.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param endExclusive the end date, exclusive, which is converted to a {@code LocalDate}, not null
     * @param unit         the unit to measure the amount in, not null
     *
     * @return the amount of time between this date and the end date
     *
     * @throws DateTimeException                if the amount cannot be calculated, or the end
     *                                          temporal cannot be converted to a {@code LocalDate}
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException              if numeric overflow occurs
     */
    // 计算当前时间量到目标时间量endExclusive之间相差多少个unit单位的时间值
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        /*
         * 从temporal中查询LocalDate部件。
         *
         * 如果没有现成的部件，通常需要从temporal中解析出纪元天，
         * 然后使用纪元天构造LocalDate后返回。
         */
        LocalDate end = LocalDate.from(endExclusive);
    
        if(unit instanceof ChronoUnit) {
            switch((ChronoUnit) unit) {
                case DAYS:
                    return daysUntil(end);
                case WEEKS:
                    return daysUntil(end) / 7;
                case MONTHS:
                    return monthsUntil(end);
                case YEARS:
                    return monthsUntil(end) / 12;
                case DECADES:
                    return monthsUntil(end) / 120;
                case CENTURIES:
                    return monthsUntil(end) / 1200;
                case MILLENNIA:
                    return monthsUntil(end) / 12000;
                case ERAS:
                    return end.getLong(ERA) - getLong(ERA);
            }
        
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
    
        return unit.between(this, end);
    }
    
    /**
     * Calculates the period between this date and another date as a {@code Period}.
     * <p>
     * This calculates the period between two dates in terms of years, months and days.
     * The start and end points are {@code this} and the specified date.
     * The result will be negative if the end is before the start.
     * The negative sign will be the same in each of year, month and day.
     * <p>
     * The calculation is performed using the ISO calendar system.
     * If necessary, the input date will be converted to ISO.
     * <p>
     * The start date is included, but the end date is not.
     * The period is calculated by removing complete months, then calculating
     * the remaining number of days, adjusting to ensure that both have the same sign.
     * The number of months is then normalized into years and months based on a 12 month year.
     * A month is considered to be complete if the end day-of-month is greater
     * than or equal to the start day-of-month.
     * For example, from {@code 2010-01-15} to {@code 2011-03-18} is "1 year, 2 months and 3 days".
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method.
     * The second is to use {@link Period#between(LocalDate, LocalDate)}:
     * <pre>
     *   // these two lines are equivalent
     *   period = start.until(end);
     *   period = Period.between(start, end);
     * </pre>
     * The choice should be made based on which makes the code more readable.
     *
     * @param endDateExclusive the end date, exclusive, which may be in any chronology, not null
     *
     * @return the period between this date and the end date, not null
     */
    // 计算当前时间量到目标时间量之间相差的"时间段"
    @Override
    public Period until(ChronoLocalDate endDateExclusive) {
        LocalDate end = LocalDate.from(endDateExclusive);
    
        long totalMonths = end.getProlepticMonth() - this.getProlepticMonth();  // safe
        int days = end.day - this.day;
        if(totalMonths>0 && days<0) {
            totalMonths--;
            LocalDate calcDate = this.plusMonths(totalMonths);
            days = (int) (end.toEpochDay() - calcDate.toEpochDay());  // safe
        } else if(totalMonths<0 && days>0) {
            totalMonths++;
            days -= end.lengthOfMonth();
        }
    
        long years = totalMonths / 12;  // safe
        int months = (int) (totalMonths % 12);  // safe
    
        return Period.of(Math.toIntExact(years), months, days);
    }
    
    /**
     * Returns a sequential ordered stream of dates. The returned stream starts from this date
     * (inclusive) and goes to {@code endExclusive} (exclusive) by an incremental step of 1 day.
     * <p>
     * This method is equivalent to {@code datesUntil(endExclusive, Period.ofDays(1))}.
     *
     * @param endExclusive the end date, exclusive, not null
     *
     * @return a sequential {@code Stream} for the range of {@code LocalDate} values
     *
     * @throws IllegalArgumentException if end date is before this date
     * @since 9
     */
    // 将当前时间量到目标时间量之间相差的每一天都转换为LocalDate对象并全部存入流中
    public Stream<LocalDate> datesUntil(LocalDate endExclusive) {
        long start = toEpochDay();
        long end = endExclusive.toEpochDay();
        if(end<start) {
            throw new IllegalArgumentException(endExclusive + " < " + this);
        }
    
        return LongStream.range(start, end).mapToObj(LocalDate::ofEpochDay);
    }
    
    /**
     * Returns a sequential ordered stream of dates by given incremental step. The returned stream
     * starts from this date (inclusive) and goes to {@code endExclusive} (exclusive).
     * <p>
     * The n-th date which appears in the stream is equal to {@code this.plus(step.multipliedBy(n))}
     * (but the result of step multiplication never overflows). For example, if this date is
     * {@code 2015-01-31}, the end date is {@code 2015-05-01} and the step is 1 month, then the
     * stream contains {@code 2015-01-31}, {@code 2015-02-28}, {@code 2015-03-31}, and
     * {@code 2015-04-30}.
     *
     * @param endExclusive the end date, exclusive, not null
     * @param step         the non-zero, non-negative {@code Period} which represents the step.
     *
     * @return a sequential {@code Stream} for the range of {@code LocalDate} values
     *
     * @throws IllegalArgumentException if step is zero, or {@code step.getDays()} and
     *                                  {@code step.toTotalMonths()} have opposite sign, or end date is before this date
     *                                  and step is positive, or end date is after this date and step is negative
     * @since 9
     */
    // 将当前时间量到目标时间量之间相差的每个"时间段"都转换为LocalDate对象并全部存入流中；step参数指定了作为步长的"时间段"
    public Stream<LocalDate> datesUntil(LocalDate endExclusive, Period step) {
        if(step.isZero()) {
            throw new IllegalArgumentException("step is zero");
        }
    
        long end = endExclusive.toEpochDay();
        long start = toEpochDay();
        long until = end - start;
        long months = step.toTotalMonths();
        long days = step.getDays();
        if((months<0 && days>0) || (months>0 && days<0)) {
            throw new IllegalArgumentException("period months and days are of opposite sign");
        }
        if(until == 0) {
            return Stream.empty();
        }
        int sign = months>0 || days>0 ? 1 : -1;
        if(sign<0 ^ until<0) {
            throw new IllegalArgumentException(endExclusive + (sign<0 ? " > " : " < ") + this);
        }
        if(months == 0) {
            long steps = (until - sign) / days; // non-negative
            return LongStream.rangeClosed(0, steps).mapToObj(n -> LocalDate.ofEpochDay(start + n * days));
        }
        // 48699/1600 = 365.2425/12, no overflow, non-negative result
        long steps = until * 1600 / (months * 48699 + days * 1600) + 1;
        long addMonths = months * steps;
        long addDays = days * steps;
        long maxAddMonths = months>0 ? MAX.getProlepticMonth() - getProlepticMonth() : getProlepticMonth() - MIN.getProlepticMonth();
        // adjust steps estimation
        if(addMonths * sign>maxAddMonths || (plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign) {
            steps--;
            addMonths -= months;
            addDays -= days;
            if(addMonths * sign>maxAddMonths || (plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign) {
                steps--;
            }
        }
    
        return LongStream.rangeClosed(0, steps).mapToObj(n -> this.plusMonths(months * n).plusDays(days * n));
    }
    
    /**
     * Returns the length of the year represented by this date.
     * <p>
     * This returns the length of the year in days, either 365 or 366.
     *
     * @return 366 if the year is leap, 365 otherwise
     */
    // 返回当前"本地日期"的"年份"部件包含的天数
    @Override
    public int lengthOfYear() {
        return (isLeapYear() ? 366 : 365);
    }
    
    /**
     * Returns the length of the month represented by this date.
     * <p>
     * This returns the length of the month in days.
     * For example, a date in January would return 31.
     *
     * @return the length of the month in days
     */
    // 返回当前"本地日期"的"月份"部件包含的天数
    @Override
    public int lengthOfMonth() {
        switch(month) {
            case 2:
                return (isLeapYear() ? 29 : 28);
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }
    
    /**
     * Checks if the year is a leap year, according to the ISO proleptic
     * calendar system rules.
     * <p>
     * This method applies the current rules for leap years across the whole time-line.
     * In general, a year is a leap year if it is divisible by four without
     * remainder. However, years divisible by 100, are not leap years, with
     * the exception of years divisible by 400 which are.
     * <p>
     * For example, 1904 is a leap year it is divisible by 4.
     * 1900 was not a leap year as it is divisible by 100, however 2000 was a
     * leap year as it is divisible by 400.
     * <p>
     * The calculation is proleptic - applying the same rules into the far future and far past.
     * This is historically inaccurate, but is correct for the ISO-8601 standard.
     *
     * @return true if the year is leap, false otherwise
     */
    // 判断当前"本地日期"的年份是否为"闰年"
    @Override
    public boolean isLeapYear() {
        return IsoChronology.INSTANCE.isLeapYear(year);
    }
    
    /**
     * Gets the chronology of this date, which is the ISO calendar system.
     * <p>
     * The {@code Chronology} represents the calendar system in use.
     * The ISO-8601 calendar system is the modern civil calendar system used today
     * in most of the world. It is equivalent to the proleptic Gregorian calendar
     * system, in which today's rules for leap years are applied for all time.
     *
     * @return the ISO chronology, not null
     */
    // 返回当前"本地日期"所用的历法系统，默认是ISO历法系统，可以在一定程度上看做与公历等同
    @Override
    public IsoChronology getChronology() {
        return IsoChronology.INSTANCE;
    }
    
    /**
     * Gets the era applicable at this date.
     * <p>
     * The official ISO-8601 standard does not define eras, however {@code IsoChronology} does.
     * It defines two eras, 'CE' from year one onwards and 'BCE' from year zero backwards.
     * Since dates before the Julian-Gregorian cutover are not in line with history,
     * the cutover between 'BCE' and 'CE' is also not aligned with the commonly used
     * eras, often referred to using 'BC' and 'AD'.
     * <p>
     * Users of this class should typically ignore this method as it exists primarily
     * to fulfill the {@link ChronoLocalDate} contract where it is necessary to support
     * the Japanese calendar system.
     *
     * @return the IsoEra applicable at this date, not null
     */
    // 返回当前"本地日期"所属的纪元：小于1的年份在公元前，其他年份在公元后(基于ISO历法系统)
    @Override
    public IsoEra getEra() {
        return (getYear()<1 ? IsoEra.BCE : IsoEra.CE);
    }
    
    /**
     * Checks if this date is after the specified date.
     * <p>
     * This checks to see if this date represents a point on the
     * local time-line after the other date.
     * <pre>
     *   LocalDate a = LocalDate.of(2012, 6, 30);
     *   LocalDate b = LocalDate.of(2012, 7, 1);
     *   a.isAfter(b) == false
     *   a.isAfter(a) == false
     *   b.isAfter(a) == true
     * </pre>
     * <p>
     * This method only considers the position of the two dates on the local time-line.
     * It does not take into account the chronology, or calendar system.
     * This is different from the comparison in {@link #compareTo(ChronoLocalDate)},
     * but is the same approach as {@link ChronoLocalDate#timeLineOrder()}.
     *
     * @param other the other date to compare to, not null
     *
     * @return true if this date is after the specified date
     */
    // 判断当前日期是否晚于参数中指定的日期
    @Override
    public boolean isAfter(ChronoLocalDate other) {
        if(other instanceof LocalDate) {
            return compareTo0((LocalDate) other)>0;
        }
        
        return ChronoLocalDate.super.isAfter(other);
    }
    
    /**
     * Checks if this date is before the specified date.
     * <p>
     * This checks to see if this date represents a point on the
     * local time-line before the other date.
     * <pre>
     *   LocalDate a = LocalDate.of(2012, 6, 30);
     *   LocalDate b = LocalDate.of(2012, 7, 1);
     *   a.isBefore(b) == true
     *   a.isBefore(a) == false
     *   b.isBefore(a) == false
     * </pre>
     * <p>
     * This method only considers the position of the two dates on the local time-line.
     * It does not take into account the chronology, or calendar system.
     * This is different from the comparison in {@link #compareTo(ChronoLocalDate)},
     * but is the same approach as {@link ChronoLocalDate#timeLineOrder()}.
     *
     * @param other the other date to compare to, not null
     *
     * @return true if this date is before the specified date
     */
    // 判断当前日期是否早于参数中指定的日期
    @Override
    public boolean isBefore(ChronoLocalDate other) {
        if(other instanceof LocalDate) {
            return compareTo0((LocalDate) other)<0;
        }
        
        return ChronoLocalDate.super.isBefore(other);
    }
    
    /**
     * Checks if this date is equal to the specified date.
     * <p>
     * This checks to see if this date represents the same point on the
     * local time-line as the other date.
     * <pre>
     *   LocalDate a = LocalDate.of(2012, 6, 30);
     *   LocalDate b = LocalDate.of(2012, 7, 1);
     *   a.isEqual(b) == false
     *   a.isEqual(a) == true
     *   b.isEqual(a) == false
     * </pre>
     * <p>
     * This method only considers the position of the two dates on the local time-line.
     * It does not take into account the chronology, or calendar system.
     * This is different from the comparison in {@link #compareTo(ChronoLocalDate)}
     * but is the same approach as {@link ChronoLocalDate#timeLineOrder()}.
     *
     * @param other the other date to compare to, not null
     *
     * @return true if this date is equal to the specified date
     */
    // 判断当前日期与参数中指定的日期是否相等
    @Override
    public boolean isEqual(ChronoLocalDate other) {
        if(other instanceof LocalDate) {
            return compareTo0((LocalDate) other) == 0;
        }
        
        return ChronoLocalDate.super.isEqual(other);
    }
    
    /**
     * Formats this date using the specified formatter.
     * <p>
     * This date will be passed to the formatter to produce a string.
     *
     * @param formatter the formatter to use, not null
     *
     * @return the formatted date string, not null
     *
     * @throws DateTimeException if an error occurs during printing
     */
    // 将当前日期转换为一个指定格式的字符串后返回
    @Override
    public String format(DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 计算当前日期到参数中日期相差的天数
    long daysUntil(LocalDate end) {
        return end.toEpochDay() - toEpochDay();
    }
    
    // 计算当前日期到参数中日期相差的月数
    private long monthsUntil(LocalDate end) {
        long packed1 = getProlepticMonth() * 32L + getDayOfMonth();  // no overflow
        long packed2 = end.getProlepticMonth() * 32L + end.getDayOfMonth();  // no overflow
        return (packed2 - packed1) / 32;
    }
    
    /**
     * Creates a local date from the year, month and day fields.
     *
     * @param year       the year to represent, validated from MIN_YEAR to MAX_YEAR
     * @param month      the month-of-year to represent, from 1 to 12, validated
     * @param dayOfMonth the day-of-month to represent, validated from 1 to 31
     *
     * @return the local date, not null
     *
     * @throws DateTimeException if the day-of-month is invalid for the month-year
     */
    /*
     * 根据年份、月份和月份中的天数构造本地日期
     *
     * year      : 年份
     * month     : 月份
     * dayOfMonth: 一月中的第几天
     */
    private static LocalDate create(int year, int month, int dayOfMonth) {
        
        if(dayOfMonth>28) {
            int dom = 31;
            
            switch(month) {
                case 2:
                    dom = (IsoChronology.INSTANCE.isLeapYear(year) ? 29 : 28);
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    dom = 30;
                    break;
            }
            
            if(dayOfMonth>dom) {
                if(dayOfMonth == 29) {
                    throw new DateTimeException("Invalid date 'February 29' as '" + year + "' is not a leap year");
                } else {
                    throw new DateTimeException("Invalid date '" + Month.of(month).name() + " " + dayOfMonth + "'");
                }
            }
        }
        
        return new LocalDate(year, month, dayOfMonth);
    }
    
    /**
     * Resolves the date, resolving days past the end of month.
     *
     * @param year  the year to represent, validated from MIN_YEAR to MAX_YEAR
     * @param month the month-of-year to represent, validated from 1 to 12
     * @param day   the day-of-month to represent, validated from 1 to 31
     *
     * @return the resolved date, not null
     */
    // 根据给定的日期单位构造LocalDate对象；必要时，需要对day参数进行截断
    private static LocalDate resolvePreviousValid(int year, int month, int day) {
        // 根据月份，截断天的范围
        switch(month) {
            case 2:
                day = Math.min(day, IsoChronology.INSTANCE.isLeapYear(year) ? 29 : 28);
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                day = Math.min(day, 30);
                break;
        }
        
        return new LocalDate(year, month, day);
    }
    
    /*
     * 处理ChronoField中的"日期"字段(只处理可以返回int值的)
     *
     * 目前支持的字段包括：
     *
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
     * ChronoField.PROLEPTIC_MONTH              ×
     * ChronoField.EPOCH_DAY                    ×
     */
    private int get0(TemporalField field) {
        switch((ChronoField) field) {
            case DAY_OF_WEEK:
                return getDayOfWeek().getValue();
            case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                return ((day - 1) % 7) + 1;
            case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                return ((getDayOfYear() - 1) % 7) + 1;
            case DAY_OF_MONTH:
                return day;
            case DAY_OF_YEAR:
                return getDayOfYear();
            case EPOCH_DAY:
                throw new UnsupportedTemporalTypeException("Invalid field 'EpochDay' for get() method, use getLong() instead");
            case ALIGNED_WEEK_OF_MONTH:
                return ((day - 1) / 7) + 1;
            case ALIGNED_WEEK_OF_YEAR:
                return ((getDayOfYear() - 1) / 7) + 1;
            case MONTH_OF_YEAR:
                return month;
            case PROLEPTIC_MONTH:
                throw new UnsupportedTemporalTypeException("Invalid field 'ProlepticMonth' for get() method, use getLong() instead");
            case YEAR_OF_ERA:
                return (year >= 1 ? year : 1 - year);
            case YEAR:
                return year;
            case ERA:
                return (year<1 ? 0 : 1);
        }
        
        throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    
    // 返回当前时间量的[Proleptic-月]
    private long getProlepticMonth() {
        return (year * 12L + month - 1);
    }
    
    int compareTo0(LocalDate otherDate) {
        int cmp = (year - otherDate.year);
        if(cmp == 0) {
            cmp = (month - otherDate.month);
            if(cmp == 0) {
                cmp = (day - otherDate.day);
            }
        }
        return cmp;
    }
    
    
    /**
     * Compares this date to another date.
     * <p>
     * The comparison is primarily based on the date, from earliest to latest.
     * It is "consistent with equals", as defined by {@link Comparable}.
     * <p>
     * If all the dates being compared are instances of {@code LocalDate},
     * then the comparison will be entirely based on the date.
     * If some dates being compared are in different chronologies, then the
     * chronology is also considered, see {@link java.time.chrono.ChronoLocalDate#compareTo}.
     *
     * @param other the other date to compare to, not null
     *
     * @return the comparator value, negative if less, positive if greater
     */
    @Override
    public int compareTo(ChronoLocalDate other) {
        if(other instanceof LocalDate) {
            return compareTo0((LocalDate) other);
        }
        return ChronoLocalDate.super.compareTo(other);
    }
    
    /**
     * Outputs this date as a {@code String}, such as {@code 2007-12-03}.
     * <p>
     * The output will be in the ISO-8601 format {@code uuuu-MM-dd}.
     *
     * @return a string representation of this date, not null
     */
    @Override
    public String toString() {
        int yearValue = year;
        int monthValue = month;
        int dayValue = day;
        int absYear = Math.abs(yearValue);
        StringBuilder buf = new StringBuilder(10);
        if(absYear<1000) {
            if(yearValue<0) {
                buf.append(yearValue - 10000).deleteCharAt(1);
            } else {
                buf.append(yearValue + 10000).deleteCharAt(0);
            }
        } else {
            if(yearValue>9999) {
                buf.append('+');
            }
            buf.append(yearValue);
        }
        return buf.append(monthValue<10 ? "-0" : "-").append(monthValue).append(dayValue<10 ? "-0" : "-").append(dayValue).toString();
    }
    
    /**
     * Checks if this date is equal to another date.
     * <p>
     * Compares this {@code LocalDate} with another ensuring that the date is the same.
     * <p>
     * Only objects of type {@code LocalDate} are compared, other types return false.
     * To compare the dates of two {@code TemporalAccessor} instances, including dates
     * in two different chronologies, use {@link ChronoField#EPOCH_DAY} as a comparator.
     *
     * @param obj the object to check, null returns false
     *
     * @return true if this is equal to the other date
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj instanceof LocalDate) {
            return compareTo0((LocalDate) obj) == 0;
        }
        return false;
    }
    
    /**
     * A hash code for this date.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        int yearValue = year;
        int monthValue = month;
        int dayValue = day;
        return (yearValue & 0xFFFFF800) ^ ((yearValue << 11) + (monthValue << 6) + (dayValue));
    }
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 2942565459149668126L;
    
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
    
    /**
     * Writes the object using a
     * <a href="../../serialized-form.html#java.time.Ser">dedicated serialized form</a>.
     *
     * @return the instance of {@code Ser}, not null
     *
     * @serialData <pre>
     *  out.writeByte(3);  // identifies a LocalDate
     *  out.writeInt(year);
     *  out.writeByte(month);
     *  out.writeByte(day);
     * </pre>
     */
    private Object writeReplace() {
        return new Ser(Ser.LOCAL_DATE_TYPE, this);
    }
    
    static LocalDate readExternal(DataInput in) throws IOException {
        int year = in.readInt();
        int month = in.readByte();
        int dayOfMonth = in.readByte();
        return LocalDate.of(year, month, dayOfMonth);
    }
    
    void writeExternal(DataOutput out) throws IOException {
        out.writeInt(year);
        out.writeByte(month);
        out.writeByte(day);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
