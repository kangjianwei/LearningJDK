/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2012, Stephen Colebourne & Michael Nascimento Santos
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

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

/**
 * A calendar system, used to organize and identify dates.
 * <p>
 * The main date and time API is built on the ISO calendar system.
 * The chronology operates behind the scenes to represent the general concept of a calendar system.
 * For example, the Japanese, Minguo, Thai Buddhist and others.
 * <p>
 * Most other calendar systems also operate on the shared concepts of year, month and day,
 * linked to the cycles of the Earth around the Sun, and the Moon around the Earth.
 * These shared concepts are defined by {@link ChronoField} and are available
 * for use by any {@code Chronology} implementation:
 * <pre>
 *   LocalDate isoDate = ...
 *   ThaiBuddhistDate thaiDate = ...
 *   int isoYear = isoDate.get(ChronoField.YEAR);
 *   int thaiYear = thaiDate.get(ChronoField.YEAR);
 * </pre>
 * As shown, although the date objects are in different calendar systems, represented by different
 * {@code Chronology} instances, both can be queried using the same constant on {@code ChronoField}.
 * For a full discussion of the implications of this, see {@link ChronoLocalDate}.
 * In general, the advice is to use the known ISO-based {@code LocalDate}, rather than
 * {@code ChronoLocalDate}.
 * <p>
 * While a {@code Chronology} object typically uses {@code ChronoField} and is based on
 * an era, year-of-era, month-of-year, day-of-month model of a date, this is not required.
 * A {@code Chronology} instance may represent a totally different kind of calendar system,
 * such as the Mayan.
 * <p>
 * In practical terms, the {@code Chronology} instance also acts as a factory.
 * The {@link #of(String)} method allows an instance to be looked up by identifier,
 * while the {@link #ofLocale(Locale)} method allows lookup by locale.
 * <p>
 * The {@code Chronology} instance provides a set of methods to create {@code ChronoLocalDate} instances.
 * The date classes are used to manipulate specific dates.
 * <ul>
 * <li> {@link #dateNow() dateNow()}
 * <li> {@link #dateNow(Clock) dateNow(clock)}
 * <li> {@link #dateNow(ZoneId) dateNow(zone)}
 * <li> {@link #date(int, int, int) date(yearProleptic, month, day)}
 * <li> {@link #date(Era, int, int, int) date(era, yearOfEra, month, day)}
 * <li> {@link #dateYearDay(int, int) dateYearDay(yearProleptic, dayOfYear)}
 * <li> {@link #dateYearDay(Era, int, int) dateYearDay(era, yearOfEra, dayOfYear)}
 * <li> {@link #date(TemporalAccessor) date(TemporalAccessor)}
 * </ul>
 *
 * <h3 id="addcalendars">Adding New Calendars</h3>
 * The set of available chronologies can be extended by applications.
 * Adding a new calendar system requires the writing of an implementation of
 * {@code Chronology}, {@code ChronoLocalDate} and {@code Era}.
 * The majority of the logic specific to the calendar system will be in the
 * {@code ChronoLocalDate} implementation.
 * The {@code Chronology} implementation acts as a factory.
 * <p>
 * To permit the discovery of additional chronologies, the {@link java.util.ServiceLoader ServiceLoader}
 * is used. A file must be added to the {@code META-INF/services} directory with the
 * name 'java.time.chrono.Chronology' listing the implementation classes.
 * See the ServiceLoader for more details on service loading.
 * For lookup by id or calendarType, the system provided calendars are found
 * first followed by application provided calendars.
 * <p>
 * Each chronology must define a chronology ID that is unique within the system.
 * If the chronology represents a calendar system defined by the
 * CLDR specification then the calendar type is the concatenation of the
 * CLDR type and, if applicable, the CLDR variant.
 *
 * @implSpec This interface must be implemented with care to ensure other classes operate correctly.
 * All implementations that can be instantiated must be final, immutable and thread-safe.
 * Subclasses should be Serializable wherever possible.
 * @since 1.8
 */
/*
 * 历法系统
 *
 * Chronology支持的历法系统包括：
 * IsoChronology          - ISO历法系统，一定程度上与公历系统等同
 * HijrahChronology       - 伊斯兰历
 * ThaiBuddhistChronology - 泰国佛教历
 * JapaneseChronology     - 日本历
 * MinguoChronology       - 中华民国历
 */
public interface Chronology extends Comparable<Chronology> {
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains an instance of {@code Chronology} from a chronology ID or
     * calendar system type.
     * <p>
     * This returns a chronology based on either the ID or the type.
     * The {@link #getId() chronology ID} uniquely identifies the chronology.
     * The {@link #getCalendarType() calendar system type} is defined by the
     * CLDR specification.
     * <p>
     * The chronology may be a system chronology or a chronology
     * provided by the application via ServiceLoader configuration.
     * <p>
     * Since some calendars can be customized, the ID or type typically refers
     * to the default customization. For example, the Gregorian calendar can have multiple
     * cutover dates from the Julian, but the lookup only provides the default cutover date.
     *
     * @param idOrType the chronology ID or calendar system type, not null
     *
     * @return the chronology with the identifier requested, not null
     *
     * @throws DateTimeException if the chronology cannot be found
     */
    // 根据指定的历法ID或历法类型，(构造并)返回对应的历法系统
    static Chronology of(String idOrType) {
        return AbstractChronology.of(idOrType);
    }
    
    /**
     * Obtains an instance of {@code Chronology} from a locale.
     * <p>
     * This returns a {@code Chronology} based on the specified locale,
     * typically returning {@code IsoChronology}. Other calendar systems
     * are only returned if they are explicitly selected within the locale.
     * <p>
     * The {@link Locale} class provide access to a range of information useful
     * for localizing an application. This includes the language and region,
     * such as "en-GB" for English as used in Great Britain.
     * <p>
     * The {@code Locale} class also supports an extension mechanism that
     * can be used to identify a calendar system. The mechanism is a form
     * of key-value pairs, where the calendar system has the key "ca".
     * For example, the locale "en-JP-u-ca-japanese" represents the English
     * language as used in Japan with the Japanese calendar system.
     * <p>
     * This method finds the desired calendar system in a manner equivalent
     * to passing "ca" to {@link Locale#getUnicodeLocaleType(String)}.
     * If the "ca" key is not present, then {@code IsoChronology} is returned.
     * <p>
     * Note that the behavior of this method differs from the older
     * {@link java.util.Calendar#getInstance(Locale)} method.
     * If that method receives a locale of "th_TH" it will return {@code BuddhistCalendar}.
     * By contrast, this method will return {@code IsoChronology}.
     * Passing the locale "th-TH-u-ca-buddhist" into either method will
     * result in the Thai Buddhist calendar system and is therefore the
     * recommended approach going forward for Thai calendar system localization.
     * <p>
     * A similar, but simpler, situation occurs for the Japanese calendar system.
     * The locale "jp_JP_JP" has previously been used to access the calendar.
     * However, unlike the Thai locale, "ja_JP_JP" is automatically converted by
     * {@code Locale} to the modern and recommended form of "ja-JP-u-ca-japanese".
     * Thus, there is no difference in behavior between this method and
     * {@code Calendar#getInstance(Locale)}.
     *
     * @param locale the locale to use to obtain the calendar system, not null
     *
     * @return the calendar system associated with the locale, not null
     *
     * @throws DateTimeException if the locale-specified calendar cannot be found
     */
    // 返回指定区域可用的历法系统
    static Chronology ofLocale(Locale locale) {
        return AbstractChronology.ofLocale(locale);
    }
    
    /**
     * Obtains an instance of {@code Chronology} from a temporal object.
     * <p>
     * This obtains a chronology based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code Chronology}.
     * <p>
     * The conversion will obtain the chronology using {@link TemporalQueries#chronology()}.
     * If the specified temporal object does not have a chronology, {@link IsoChronology} is returned.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code Chronology::from}.
     *
     * @param temporal the temporal to convert, not null
     *
     * @return the chronology, not null
     *
     * @throws DateTimeException if unable to convert to a {@code Chronology}
     */
    // 返回时间量temporal使用的历法系统
    static Chronology from(TemporalAccessor temporal) {
        Objects.requireNonNull(temporal, "temporal");
        
        // 获取时间量temporal使用的历法系统
        Chronology obj = temporal.query(TemporalQueries.chronology());
        
        return Objects.requireNonNullElse(obj, IsoChronology.INSTANCE);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 历法系统 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the ID of the chronology.
     * <p>
     * The ID uniquely identifies the {@code Chronology}.
     * It can be used to lookup the {@code Chronology} using {@link #of(String)}.
     *
     * @return the chronology ID, not null
     *
     * @see #getCalendarType()
     */
    // 返回历法的ID
    String getId();
    
    /**
     * Gets the calendar type of the calendar system.
     * <p>
     * The calendar type is an identifier defined by the CLDR and
     * <em>Unicode Locale Data Markup Language (LDML)</em> specifications
     * to uniquely identify a calendar.
     * The {@code getCalendarType} is the concatenation of the CLDR calendar type
     * and the variant, if applicable, is appended separated by "-".
     * The calendar type is used to lookup the {@code Chronology} using {@link #of(String)}.
     *
     * @return the calendar system type, null if the calendar is not defined by CLDR/LDML
     *
     * @see #getId()
     */
    // 返回历法的类型
    String getCalendarType();
    
    /*▲ 历法系统 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 本地日期 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains the current local date in this chronology from the system clock in the default time-zone.
     * <p>
     * This will query the {@link Clock#systemDefaultZone() system clock} in the default
     * time-zone to obtain the current date.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @return the current local date using the system clock and default time-zone, not null
     *
     * @throws DateTimeException if unable to create the date
     * @implSpec The default implementation invokes {@link #dateNow(Clock)}.
     */
    // 基于此刻的UTC时间，构造属于系统默认时区的"本地日期"对象
    default ChronoLocalDate dateNow() {
        return dateNow(Clock.systemDefaultZone());
    }
    
    /**
     * Obtains the current local date in this chronology from the system clock in the specified time-zone.
     * <p>
     * This will query the {@link Clock#system(ZoneId) system clock} to obtain the current date.
     * Specifying the time-zone avoids dependence on the default time-zone.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @param zone the zone ID to use, not null
     *
     * @return the current local date using the system clock, not null
     *
     * @throws DateTimeException if unable to create the date
     * @implSpec The default implementation invokes {@link #dateNow(Clock)}.
     */
    // 基于此刻的UTC时间，构造属于zone时区的"本地日期"对象
    default ChronoLocalDate dateNow(ZoneId zone) {
        return dateNow(Clock.system(zone));
    }
    
    /**
     * Obtains the current local date in this chronology from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current date - today.
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@link Clock dependency injection}.
     *
     * @param clock the clock to use, not null
     *
     * @return the current local date, not null
     *
     * @throws DateTimeException if unable to create the date
     * @implSpec The default implementation invokes {@link #date(TemporalAccessor)}.
     */
    // 基于clock提供的时间戳和时区ID构造"本地日期"对象
    default ChronoLocalDate dateNow(Clock clock) {
        Objects.requireNonNull(clock, "clock");
    
        // 基于clock提供的时间戳和时区ID构造"本地日期"对象
        LocalDate localDate = LocalDate.now(clock);
    
        return date(localDate);
    }
    
    /**
     * Obtains a local date in this chronology from another temporal object.
     * <p>
     * This obtains a date in this chronology based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code ChronoLocalDate}.
     * <p>
     * The conversion typically uses the {@link ChronoField#EPOCH_DAY EPOCH_DAY}
     * field, which is standardized across calendar systems.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code aChronology::date}.
     *
     * @param temporal the temporal object to convert, not null
     *
     * @return the local date in this chronology, not null
     *
     * @throws DateTimeException if unable to create the date
     * @see ChronoLocalDate#from(TemporalAccessor)
     */
    /*
     * 从temporal中查询LocalDate部件。
     *
     * 如果没有现成的部件，通常需要从temporal中解析出纪元天，
     * 然后使用纪元天构造LocalDate后返回。
     */
    ChronoLocalDate date(TemporalAccessor temporal);
    
    /**
     * Obtains a local date in this chronology from the era, year-of-era,
     * month-of-year and day-of-month fields.
     *
     * @param era        the era of the correct type for the chronology, not null
     * @param yearOfEra  the chronology year-of-era
     * @param month      the chronology month-of-year
     * @param dayOfMonth the chronology day-of-month
     *
     * @return the local date in this chronology, not null
     *
     * @throws DateTimeException  if unable to create the date
     * @throws ClassCastException if the {@code era} is not of the correct type for the chronology
     * @implSpec The default implementation combines the era and year-of-era into a proleptic
     * year before calling {@link #date(int, int, int)}.
     */
    /*
     * 根据纪元、ISO年份、月份和月份中的天数构造"本地日期"对象
     *
     * era       : 纪元
     * year      : 年份，这里应当传入ISO年份(公历年)
     * month     : 月份
     * dayOfMonth: 一月中的第几天
     */
    default ChronoLocalDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        // 将era处的ISO年份(公历年)转换为"Proleptic年"
        int prolepticYear = prolepticYear(era, yearOfEra);
        return date(prolepticYear, month, dayOfMonth);
    }
    
    /**
     * Obtains a local date in this chronology from the era, year-of-era and
     * day-of-year fields.
     *
     * @param era       the era of the correct type for the chronology, not null
     * @param yearOfEra the chronology year-of-era
     * @param dayOfYear the chronology day-of-year
     *
     * @return the local date in this chronology, not null
     *
     * @throws DateTimeException  if unable to create the date
     * @throws ClassCastException if the {@code era} is not of the correct type for the chronology
     * @implSpec The default implementation combines the era and year-of-era into a proleptic
     * year before calling {@link #dateYearDay(int, int)}.
     */
    // 根据纪元、ISO年份和年份中的天数构造"本地日期"对象
    default ChronoLocalDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        // 将era处的ISO年份(公历年)转换为"Proleptic年"
        int prolepticYear = prolepticYear(era, yearOfEra);
        return dateYearDay(prolepticYear, dayOfYear);
    }
    
    /**
     * Obtains a local date in this chronology from the proleptic-year,
     * month-of-year and day-of-month fields.
     *
     * @param prolepticYear the chronology proleptic-year
     * @param month         the chronology month-of-year
     * @param dayOfMonth    the chronology day-of-month
     *
     * @return the local date in this chronology, not null
     *
     * @throws DateTimeException if unable to create the date
     */
    /*
     * 根据Proleptic年份、月份和月份中的天数构造"本地日期"对象
     *
     * year      : 年份，这里应当传入"Proleptic年"
     * month     : 月份
     * dayOfMonth: 一月中的第几天
     */
    ChronoLocalDate date(int prolepticYear, int month, int dayOfMonth);
    
    /**
     * Obtains a local date in this chronology from the proleptic-year and
     * day-of-year fields.
     *
     * @param prolepticYear the chronology proleptic-year
     * @param dayOfYear     the chronology day-of-year
     *
     * @return the local date in this chronology, not null
     *
     * @throws DateTimeException if unable to create the date
     */
    // 根据Proleptic年份和年份中的天数构造"本地日期"对象
    ChronoLocalDate dateYearDay(int prolepticYear, int dayOfYear);
    
    /**
     * Obtains a local date in this chronology from the epoch-day.
     * <p>
     * The definition of {@link ChronoField#EPOCH_DAY EPOCH_DAY} is the same
     * for all calendar systems, thus it can be used for conversion.
     *
     * @param epochDay the epoch day
     *
     * @return the local date in this chronology, not null
     *
     * @throws DateTimeException if unable to create the date
     */
    // 根据给定的纪元天构造"本地日期"对象
    ChronoLocalDate dateEpochDay(long epochDay);
    
    /**
     * Resolves parsed {@code ChronoField} values into a date during parsing.
     * <p>
     * Most {@code TemporalField} implementations are resolved using the
     * resolve method on the field. By contrast, the {@code ChronoField} class
     * defines fields that only have meaning relative to the chronology.
     * As such, {@code ChronoField} date fields are resolved here in the
     * context of a specific chronology.
     * <p>
     * The default implementation, which explains typical resolve behaviour,
     * is provided in {@link AbstractChronology}.
     *
     * @param fieldValues   the map of fields to values, which can be updated, not null
     * @param resolverStyle the requested type of resolve, not null
     *
     * @return the resolved date, null if insufficient information to create a date
     *
     * @throws DateTimeException if the date cannot be resolved, typically
     *                           because of a conflict in the input data
     */
    /*
     * 从fieldValues中解析出时间量字段的值，然后用这些值来构造本地日期。
     *
     * resolverStyle: 指示解析时间量字段的策略
     */
    ChronoLocalDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle);
    
    /*▲ 本地日期 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 本地日期-时间 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains a local date-time in this chronology from another temporal object.
     * <p>
     * This obtains a date-time in this chronology based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code ChronoLocalDateTime}.
     * <p>
     * The conversion extracts and combines the {@code ChronoLocalDate} and the
     * {@code LocalTime} from the temporal object.
     * Implementations are permitted to perform optimizations such as accessing
     * those fields that are equivalent to the relevant objects.
     * The result uses this chronology.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code aChronology::localDateTime}.
     *
     * @param temporal the temporal object to convert, not null
     *
     * @return the local date-time in this chronology, not null
     *
     * @throws DateTimeException if unable to create the date-time
     * @see ChronoLocalDateTime#from(TemporalAccessor)
     */
    // 从temporal中获取/构造ChronoLocalDateTime对象
    default ChronoLocalDateTime<? extends ChronoLocalDate> localDateTime(TemporalAccessor temporal) {
        try {
            // 从temporal中查询ChronoLocalDate部件的信息
            ChronoLocalDate localDate = date(temporal);
            // 从temporal中查询LocalTime部件
            LocalTime localTime = LocalTime.from(temporal);
        
            // 将当前"本地日期"和指定的"本地时间"整合成一个"本地日期-时间"对象后返回
            return localDate.atTime(localTime);
        } catch(DateTimeException ex) {
            throw new DateTimeException("Unable to obtain ChronoLocalDateTime from TemporalAccessor: " + temporal.getClass(), ex);
        }
    }
    
    /**
     * Obtains a {@code ChronoZonedDateTime} in this chronology from another temporal object.
     * <p>
     * This obtains a zoned date-time in this chronology based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code ChronoZonedDateTime}.
     * <p>
     * The conversion will first obtain a {@code ZoneId} from the temporal object,
     * falling back to a {@code ZoneOffset} if necessary. It will then try to obtain
     * an {@code Instant}, falling back to a {@code ChronoLocalDateTime} if necessary.
     * The result will be either the combination of {@code ZoneId} or {@code ZoneOffset}
     * with {@code Instant} or {@code ChronoLocalDateTime}.
     * Implementations are permitted to perform optimizations such as accessing
     * those fields that are equivalent to the relevant objects.
     * The result uses this chronology.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code aChronology::zonedDateTime}.
     *
     * @param temporal the temporal object to convert, not null
     *
     * @return the zoned date-time in this chronology, not null
     *
     * @throws DateTimeException if unable to create the date-time
     * @see ChronoZonedDateTime#from(TemporalAccessor)
     */
    // 从temporal中解析出时间戳和时区ID，然后用它们构造一个属于zone时区的"本地日期-时间"
    default ChronoZonedDateTime<? extends ChronoLocalDate> zonedDateTime(TemporalAccessor temporal) {
        try {
            // 从temporal中查询ZoneId部件的信息(宽松模式，如果在时间量中无法直接查找到ZoneId属性，则回退为查找ZoneOffset属性)
            ZoneId zone = ZoneId.from(temporal);
    
            try {
                // 从时间量temporal中获取秒部件和纳秒部件的信息，以构造一个时间戳
                Instant instant = Instant.from(temporal);
                // 使用时间戳和时区ID构造一个属于zone时区的"本地日期-时间"
                return zonedDateTime(instant, zone);
            } catch(DateTimeException e) {
                ChronoLocalDateTimeImpl<?> localDateTime = ChronoLocalDateTimeImpl.ensureValid(this, localDateTime(temporal));
                return ChronoZonedDateTimeImpl.ofBest(localDateTime, zone, null);
            }
        } catch(DateTimeException e) {
            throw new DateTimeException("Unable to obtain ChronoZonedDateTime from TemporalAccessor: " + temporal.getClass(), e);
        }
    }
    
    /**
     * Obtains a {@code ChronoZonedDateTime} in this chronology from an {@code Instant}.
     * <p>
     * This obtains a zoned date-time with the same instant as that specified.
     *
     * @param instant the instant to create the date-time from, not null
     * @param zone    the time-zone, not null
     *
     * @return the zoned date-time, not null
     *
     * @throws DateTimeException if the result exceeds the supported range
     */
    // 使用时间戳和时区ID构造一个属于zone时区的"本地日期-时间"(时区偏移时间准确)
    default ChronoZonedDateTime<? extends ChronoLocalDate> zonedDateTime(Instant instant, ZoneId zone) {
        return ChronoZonedDateTimeImpl.ofInstant(this, instant, zone);
    }
    
    /*▲ 本地日期-时间 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间段 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains a period for this chronology based on years, months and days.
     * <p>
     * This returns a period tied to this chronology using the specified
     * years, months and days.  All supplied chronologies use periods
     * based on years, months and days, however the {@code ChronoPeriod} API
     * allows the period to be represented using other units.
     *
     * @param years  the number of years, may be negative
     * @param months the number of years, may be negative
     * @param days   the number of years, may be negative
     *
     * @return the period in terms of this chronology, not null
     *
     * @implSpec The default implementation returns an implementation class suitable
     * for most calendar systems. It is based solely on the three units.
     * Normalization, addition and subtraction derive the number of months
     * in a year from the {@link #range(ChronoField)}. If the number of
     * months within a year is fixed, then the calculation approach for
     * addition, subtraction and normalization is slightly different.
     * <p>
     * If implementing an unusual calendar system that is not based on
     * years, months and days, or where you want direct control, then
     * the {@code ChronoPeriod} interface must be directly implemented.
     * <p>
     * The returned period is immutable and thread-safe.
     */
    // 返回由"年/月/天"构造的时间段
    default ChronoPeriod period(int years, int months, int days) {
        return new ChronoPeriodImpl(this, years, months, days);
    }
    
    /*▲ 时间段 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the number of seconds from the epoch of 1970-01-01T00:00:00Z.
     * <p>
     * The number of seconds is calculated using the proleptic-year,
     * month, day-of-month, hour, minute, second, and zoneOffset.
     *
     * @param prolepticYear the chronology proleptic-year
     * @param month         the chronology month-of-year
     * @param dayOfMonth    the chronology day-of-month
     * @param hour          the hour-of-day, from 0 to 23
     * @param minute        the minute-of-hour, from 0 to 59
     * @param second        the second-of-minute, from 0 to 59
     * @param zoneOffset    the zone offset, not null
     *
     * @return the number of seconds relative to 1970-01-01T00:00:00Z, may be negative
     *
     * @throws DateTimeException if any of the values are out of range
     * @since 9
     */
    /*
     * 将给定的日期部件和时间部件视为在zoneOffset时区的一个时间点，然后求该时间点下，UTC时区的纪元秒
     *
     * 注：这里传入的年份信息是"Proleptic年"
     */
    default long epochSecond(int prolepticYear, int month, int dayOfMonth, int hour, int minute, int second, ZoneOffset zoneOffset) {
        Objects.requireNonNull(zoneOffset, "zoneOffset");
        
        HOUR_OF_DAY.checkValidValue(hour);
        MINUTE_OF_HOUR.checkValidValue(minute);
        SECOND_OF_MINUTE.checkValidValue(second);
        
        long daysInSec = Math.multiplyExact(date(prolepticYear, month, dayOfMonth).toEpochDay(), 86400);
        long timeinSec = (hour * 60 + minute) * 60 + second;
        return Math.addExact(daysInSec, timeinSec - zoneOffset.getTotalSeconds());
    }
    
    /**
     * Gets the number of seconds from the epoch of 1970-01-01T00:00:00Z.
     * <p>
     * The number of seconds is calculated using the era, year-of-era,
     * month, day-of-month, hour, minute, second, and zoneOffset.
     *
     * @param era        the era of the correct type for the chronology, not null
     * @param yearOfEra  the chronology year-of-era
     * @param month      the chronology month-of-year
     * @param dayOfMonth the chronology day-of-month
     * @param hour       the hour-of-day, from 0 to 23
     * @param minute     the minute-of-hour, from 0 to 59
     * @param second     the second-of-minute, from 0 to 59
     * @param zoneOffset the zone offset, not null
     *
     * @return the number of seconds relative to 1970-01-01T00:00:00Z, may be negative
     *
     * @throws DateTimeException if any of the values are out of range
     * @since 9
     */
    /*
     * 将给定的日期部件和时间部件视为在zoneOffset时区的一个时间点，然后求该时间点下，UTC时区的纪元秒
     *
     * 注：这里传入的年份信息是"纪元"和"ISO年份"
     */
    default long epochSecond(Era era, int yearOfEra, int month, int dayOfMonth, int hour, int minute, int second, ZoneOffset zoneOffset) {
        Objects.requireNonNull(era, "era");
        
        // 将era处的ISO年份(公历年)转换为"Proleptic年"
        int prolepticYear = prolepticYear(era, yearOfEra);
        return epochSecond(prolepticYear, month, dayOfMonth, hour, minute, second, zoneOffset);
    }
    
    /**
     * Calculates the proleptic-year given the era and year-of-era.
     * <p>
     * This combines the era and year-of-era into the single proleptic-year field.
     * <p>
     * If the chronology makes active use of eras, such as {@code JapaneseChronology}
     * then the year-of-era will be validated against the era.
     * For other chronologies, validation is optional.
     *
     * @param era       the era of the correct type for the chronology, not null
     * @param yearOfEra the chronology year-of-era
     *
     * @return the proleptic-year
     *
     * @throws DateTimeException  if unable to convert to a proleptic-year,
     *                            such as if the year is invalid for the era
     * @throws ClassCastException if the {@code era} is not of the correct type for the chronology
     */
    // 将era处的ISO年份(公历年)转换为"Proleptic年"
    int prolepticYear(Era era, int yearOfEra);
    
    /**
     * Creates the chronology era object from the numeric value.
     * <p>
     * The era is, conceptually, the largest division of the time-line.
     * Most calendar systems have a single epoch dividing the time-line into two eras.
     * However, some have multiple eras, such as one for the reign of each leader.
     * The exact meaning is determined by the chronology according to the following constraints.
     * <p>
     * The era in use at 1970-01-01 must have the value 1.
     * Later eras must have sequentially higher values.
     * Earlier eras must have sequentially lower values.
     * Each chronology must refer to an enum or similar singleton to provide the era values.
     * <p>
     * This method returns the singleton era of the correct type for the specified era value.
     *
     * @param eraValue the era value
     *
     * @return the calendar system era, not null
     *
     * @throws DateTimeException if unable to create the era
     */
    // 将指定的整数转换为纪元枚举
    Era eraOf(int eraValue);
    
    /*▲ 转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if the specified year is a leap year.
     * <p>
     * A leap-year is a year of a longer length than normal.
     * The exact meaning is determined by the chronology according to the following constraints.
     * <ul>
     * <li>a leap-year must imply a year-length longer than a non leap-year.
     * <li>a chronology that does not support the concept of a year must return false.
     * <li>the correct result must be returned for all years within the
     *     valid range of years for the chronology.
     * </ul>
     * <p>
     * Outside the range of valid years an implementation is free to return
     * either a best guess or false.
     * An implementation must not throw an exception, even if the year is
     * outside the range of valid years.
     *
     * @param prolepticYear the proleptic-year to check, not validated for range
     *
     * @return true if the year is a leap year
     */
    // 判断当前年份是否为闰年
    boolean isLeapYear(long prolepticYear);
    
    /**
     * Gets the list of eras for the chronology.
     * <p>
     * Most calendar systems have an era, within which the year has meaning.
     * If the calendar system does not support the concept of eras, an empty
     * list must be returned.
     *
     * @return the list of eras for the chronology, may be immutable, not null
     */
    // 返回当前历法系统下可用的纪元
    List<Era> eras();
    
    /**
     * Gets the range of valid values for the specified field.
     * <p>
     * All fields can be expressed as a {@code long} integer.
     * This method returns an object that describes the valid range for that value.
     * <p>
     * Note that the result only describes the minimum and maximum valid values
     * and it is important not to read too much into them. For example, there
     * could be values within the range that are invalid for the field.
     * <p>
     * This method will return a result whether or not the chronology supports the field.
     *
     * @param field the field to get the range for, not null
     *
     * @return the range of valid values for the field, not null
     *
     * @throws DateTimeException if the range for the field cannot be obtained
     */
    // 返回时间量字段field的取值区间
    ValueRange range(ChronoField field);
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the available chronologies.
     * <p>
     * Each returned {@code Chronology} is available for use in the system.
     * The set of chronologies includes the system chronologies and
     * any chronologies provided by the application via ServiceLoader
     * configuration.
     *
     * @return the independent, modifiable set of the available chronology IDs, not null
     */
    // 返回当前所有可用的历法系统，包括自定义的历法系统
    static Set<Chronology> getAvailableChronologies() {
        return AbstractChronology.getAvailableChronologies();
    }
    
    /**
     * Gets the textual representation of this chronology.
     * <p>
     * This returns the textual name used to identify the chronology,
     * suitable for presentation to the user.
     * The parameters control the style of the returned text and the locale.
     *
     * @param style  the style of the text required, not null
     * @param locale the locale to use, not null
     *
     * @return the text value of the chronology, not null
     *
     * @implSpec The default implementation behaves as though the formatter was used to
     * format the chronology textual name.
     */
    // 返回当前历法系统的文本显示
    default String getDisplayName(TextStyle style, Locale locale) {
        TemporalAccessor temporal = new TemporalAccessor() {
    
            // 判断当前时间量是否支持指定的时间量字段
            @Override
            public boolean isSupported(TemporalField field) {
                return false;
            }
    
            // 以long形式返回时间量字段field的值
            @Override
            public long getLong(TemporalField field) {
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            }
    
            // 使用指定的时间量查询器，从当前时间量中查询目标信息
            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TemporalQuery<R> query) {
                // 查询时间量的历法系统
                if(query == TemporalQueries.chronology()) {
                    return (R) Chronology.this;
                }
        
                return TemporalAccessor.super.query(query);
            }
        };
    
        return new DateTimeFormatterBuilder().appendChronologyText(style).toFormatter(locale).format(temporal);
    }
    
    
    /**
     * Compares this chronology to another chronology.
     * <p>
     * The comparison order first by the chronology ID string, then by any
     * additional information specific to the subclass.
     * It is "consistent with equals", as defined by {@link Comparable}.
     *
     * @param other the other chronology to compare to, not null
     *
     * @return the comparator value, negative if less, positive if greater
     */
    @Override
    int compareTo(Chronology other);
    
    /**
     * Outputs this chronology as a {@code String}.
     * <p>
     * The format should include the entire state of the object.
     *
     * @return a string representation of this chronology, not null
     */
    @Override
    String toString();
    
    /**
     * Checks if this chronology is equal to another chronology.
     * <p>
     * The comparison is based on the entire state of the object.
     *
     * @param obj the object to check, null returns false
     *
     * @return true if this is equal to the other chronology
     */
    @Override
    boolean equals(Object obj);
    
    /**
     * A hash code for this chronology.
     * <p>
     * The hash code should be based on the entire state of the object.
     *
     * @return a suitable hash code
     */
    @Override
    int hashCode();
    
}
