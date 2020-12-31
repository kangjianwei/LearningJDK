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
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
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
import java.util.Objects;

import static java.time.temporal.ChronoUnit.CENTURIES;
import static java.time.temporal.ChronoUnit.DECADES;
import static java.time.temporal.ChronoUnit.ERAS;
import static java.time.temporal.ChronoUnit.MILLENNIA;
import static java.time.temporal.ChronoUnit.YEARS;

/**
 * A year in the ISO-8601 calendar system, such as {@code 2007}.
 * <p>
 * {@code Year} is an immutable date-time object that represents a year.
 * Any field that can be derived from a year can be obtained.
 * <p>
 * <b>Note that years in the ISO chronology only align with years in the
 * Gregorian-Julian system for modern years. Parts of Russia did not switch to the
 * modern Gregorian/ISO rules until 1920.
 * As such, historical years must be treated with caution.</b>
 * <p>
 * This class does not store or represent a month, day, time or time-zone.
 * For example, the value "2007" can be stored in a {@code Year}.
 * <p>
 * Years represented by this class follow the ISO-8601 standard and use
 * the proleptic numbering system. Year 1 is preceded by year 0, then by year -1.
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
 * {@code Year} may have unpredictable results and should be avoided.
 * The {@code equals} method should be used for comparisons.
 *
 * @implSpec This class is immutable and thread-safe.
 * @since 1.8
 */
// x年
public final class Year implements Temporal, TemporalAdjuster, Comparable<Year>, Serializable {
    
    /**
     * The minimum supported year, '-999,999,999'.
     */
    public static final int MIN_VALUE = -999_999_999;
    
    /**
     * The maximum supported year, '+999,999,999'.
     */
    public static final int MAX_VALUE = 999_999_999;
    
    /**
     * Parser.
     */
    private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD).toFormatter();
    
    /**
     * The year being represented.
     */
    private final int year; // "Proleptic年"部件[-999_999_999, 999_999_999]
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructor.
     *
     * @param year the year to represent
     */
    private Year(int year) {
        this.year = year;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains the current year from the system clock in the default time-zone.
     * <p>
     * This will query the {@link Clock#systemDefaultZone() system clock} in the default
     * time-zone to obtain the current year.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @return the current year using the system clock and default time-zone, not null
     */
    // 基于此刻的UTC时间，构造属于系统默认时区的Year对象
    public static Year now() {
        // 获取一个系统时钟，其预设的时区ID为系统默认的时区ID
        Clock clock = Clock.systemDefaultZone();
        return now(clock);
    }
    
    /**
     * Obtains the current year from the system clock in the specified time-zone.
     * <p>
     * This will query the {@link Clock#system(ZoneId) system clock} to obtain the current year.
     * Specifying the time-zone avoids dependence on the default time-zone.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @param zone the zone ID to use, not null
     *
     * @return the current year using the system clock, not null
     */
    // 基于此刻的UTC时间，构造属于zone时区的Year对象
    public static Year now(ZoneId zone) {
        // 获取一个系统时钟，其预设的时区ID为zone
        Clock clock = Clock.system(zone);
        return now(clock);
    }
    
    /**
     * Obtains the current year from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current year.
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@link Clock dependency injection}.
     *
     * @param clock the clock to use, not null
     *
     * @return the current year, not null
     */
    // 基于clock提供的时间戳和时区ID构造Year对象
    public static Year now(Clock clock) {
        // 基于clock提供的时间戳和时区ID构造"本地日期"对象
        final LocalDate now = LocalDate.now(clock);
        return Year.of(now.getYear());
    }
    
    /**
     * Obtains an instance of {@code Year}.
     * <p>
     * This method accepts a year value from the proleptic ISO calendar system.
     * <p>
     * The year 2AD/CE is represented by 2.<br>
     * The year 1AD/CE is represented by 1.<br>
     * The year 1BC/BCE is represented by 0.<br>
     * The year 2BC/BCE is represented by -1.<br>
     *
     * @param isoYear the ISO proleptic year to represent, from {@code MIN_VALUE} to {@code MAX_VALUE}
     *
     * @return the year, not null
     *
     * @throws DateTimeException if the field is invalid
     */
    // 根据指定的Proleptic年构造Year对象
    public static Year of(int isoYear) {
        ChronoField.YEAR.checkValidValue(isoYear);
        return new Year(isoYear);
    }
    
    /**
     * Obtains an instance of {@code Year} from a temporal object.
     * <p>
     * This obtains a year based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code Year}.
     * <p>
     * The conversion extracts the {@link ChronoField#YEAR year} field.
     * The extraction is only permitted if the temporal object has an ISO
     * chronology, or can be converted to a {@code LocalDate}.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code Year::from}.
     *
     * @param temporal the temporal object to convert, not null
     *
     * @return the year, not null
     *
     * @throws DateTimeException if unable to convert to a {@code Year}
     */
    // 从temporal中查询/构造Year对象
    public static Year from(TemporalAccessor temporal) {
        Objects.requireNonNull(temporal, "temporal");
    
        if(temporal instanceof Year) {
            return (Year) temporal;
        }
    
        try {
            if(!IsoChronology.INSTANCE.equals(Chronology.from(temporal))) {
                temporal = LocalDate.from(temporal);
            }
        
            return of(temporal.get(ChronoField.YEAR));
        } catch(DateTimeException ex) {
            throw new DateTimeException("Unable to obtain Year from TemporalAccessor: " + temporal + " of type " + temporal.getClass().getName(), ex);
        }
    }
    
    /**
     * Obtains an instance of {@code Year} from a text string such as {@code 2007}.
     * <p>
     * The string must represent a valid year.
     * Years outside the range 0000 to 9999 must be prefixed by the plus or minus symbol.
     *
     * @param text the text to parse such as "2007", not null
     *
     * @return the parsed year, not null
     *
     * @throws DateTimeParseException if the text cannot be parsed
     */
    // 从指定的文本中解析出Year信息，要求该文本符合ISO规范，即类似：2020
    public static Year parse(CharSequence text) {
        return parse(text, PARSER);
    }
    
    /**
     * Obtains an instance of {@code Year} from a text string using a specific formatter.
     * <p>
     * The text is parsed using the formatter, returning a year.
     *
     * @param text      the text to parse, not null
     * @param formatter the formatter to use, not null
     *
     * @return the parsed year, not null
     *
     * @throws DateTimeParseException if the text cannot be parsed
     */
    // 从指定的文本中解析出Year信息，要求该文本符合指定的格式规范
    public static Year parse(CharSequence text, DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        return formatter.parse(text, Year::from);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Combines this year with a day-of-year to create a {@code LocalDate}.
     * <p>
     * This returns a {@code LocalDate} formed from this year and the specified day-of-year.
     * <p>
     * The day-of-year value 366 is only valid in a leap year.
     *
     * @param dayOfYear the day-of-year to use, from 1 to 365-366
     *
     * @return the local date formed from this year and the specified date of year, not null
     *
     * @throws DateTimeException if the day of year is zero or less, 366 or greater or equal
     *                           to 366 and this is not a leap year
     */
    // 使用当前Proleptic年和年份中的天数dayOfYear构造"本地日期"对象
    public LocalDate atDay(int dayOfYear) {
        return LocalDate.ofYearDay(year, dayOfYear);
    }
    
    /**
     * Combines this year with a month to create a {@code YearMonth}.
     * <p>
     * This returns a {@code YearMonth} formed from this year and the specified month.
     * All possible combinations of year and month are valid.
     * <p>
     * This method can be used as part of a chain to produce a date:
     * <pre>
     *  LocalDate date = year.atMonth(month).atDay(day);
     * </pre>
     *
     * @param month the month-of-year to use, not null
     *
     * @return the year-month formed from this year and the specified month, not null
     */
    // 使用当前Proleptic年和月份month构造YearMonth对象
    public YearMonth atMonth(Month month) {
        return YearMonth.of(year, month);
    }
    
    /**
     * Combines this year with a month to create a {@code YearMonth}.
     * <p>
     * This returns a {@code YearMonth} formed from this year and the specified month.
     * All possible combinations of year and month are valid.
     * <p>
     * This method can be used as part of a chain to produce a date:
     * <pre>
     *  LocalDate date = year.atMonth(month).atDay(day);
     * </pre>
     *
     * @param month the month-of-year to use, from 1 (January) to 12 (December)
     *
     * @return the year-month formed from this year and the specified month, not null
     *
     * @throws DateTimeException if the month is invalid
     */
    // 使用当前Proleptic年和月份month构造YearMonth对象
    public YearMonth atMonth(int month) {
        return YearMonth.of(year, month);
    }
    
    /**
     * Combines this year with a month-day to create a {@code LocalDate}.
     * <p>
     * This returns a {@code LocalDate} formed from this year and the specified month-day.
     * <p>
     * A month-day of February 29th will be adjusted to February 28th in the resulting
     * date if the year is not a leap year.
     *
     * @param monthDay the month-day to use, not null
     *
     * @return the local date formed from this year and the specified month-day, not null
     */
    // 使用当前Proleptic年和一个日期信息monthDay构造"本地日期"对象
    public LocalDate atMonthDay(MonthDay monthDay) {
        return monthDay.atYear(year);
    }
    
    /*▲ 转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 部件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the year value.
     * <p>
     * The year returned by this method is proleptic as per {@code get(YEAR)}.
     *
     * @return the year, {@code MIN_VALUE} to {@code MAX_VALUE}
     */
    // 返回 "Proleptic年"部件
    public int getValue() {
        return year;
    }
    
    /*▲ 部件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 增加 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a copy of this year with the specified amount added.
     * <p>
     * This returns a {@code Year}, based on this one, with the specified amount added.
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
     * @return a {@code Year} based on this year with the addition made, not null
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
    public Year plus(TemporalAmount amountToAdd) {
        return (Year) amountToAdd.addTo(this);
    }
    
    /**
     * Returns a copy of this year with the specified amount added.
     * <p>
     * This returns a {@code Year}, based on this one, with the amount
     * in terms of the unit added. If it is not possible to add the amount, because the
     * unit is not supported or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoUnit} then the addition is implemented here.
     * The supported fields behave as follows:
     * <ul>
     * <li>{@code YEARS} -
     *  Returns a {@code Year} with the specified number of years added.
     *  This is equivalent to {@link #plusYears(long)}.
     * <li>{@code DECADES} -
     *  Returns a {@code Year} with the specified number of decades added.
     *  This is equivalent to calling {@link #plusYears(long)} with the amount
     *  multiplied by 10.
     * <li>{@code CENTURIES} -
     *  Returns a {@code Year} with the specified number of centuries added.
     *  This is equivalent to calling {@link #plusYears(long)} with the amount
     *  multiplied by 100.
     * <li>{@code MILLENNIA} -
     *  Returns a {@code Year} with the specified number of millennia added.
     *  This is equivalent to calling {@link #plusYears(long)} with the amount
     *  multiplied by 1,000.
     * <li>{@code ERAS} -
     *  Returns a {@code Year} with the specified number of eras added.
     *  Only two eras are supported so the amount must be one, zero or minus one.
     *  If the amount is non-zero then the year is changed such that the year-of-era
     *  is unchanged.
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
     * @return a {@code Year} based on this year with the specified amount added, not null
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
    public Year plus(long amountToAdd, TemporalUnit unit) {
        if(unit instanceof ChronoUnit) {
            switch((ChronoUnit) unit) {
                case YEARS:
                    return plusYears(amountToAdd);
                case DECADES:
                    return plusYears(Math.multiplyExact(amountToAdd, 10));
                case CENTURIES:
                    return plusYears(Math.multiplyExact(amountToAdd, 100));
                case MILLENNIA:
                    return plusYears(Math.multiplyExact(amountToAdd, 1000));
                case ERAS:
                    return with(ChronoField.ERA, Math.addExact(getLong(ChronoField.ERA), amountToAdd));
            }
            
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        
        return unit.addTo(this, amountToAdd);
    }
    
    /**
     * Returns a copy of this {@code Year} with the specified number of years added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param yearsToAdd the years to add, may be negative
     *
     * @return a {@code Year} based on this year with the years added, not null
     *
     * @throws DateTimeException if the result exceeds the supported range
     */
    /*
     * 在当前时间量的值上累加yearsToAdd年
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public Year plusYears(long yearsToAdd) {
        if(yearsToAdd == 0) {
            return this;
        }
        
        return of(ChronoField.YEAR.checkValidIntValue(year + yearsToAdd));  // overflow safe
    }
    
    /*▲ 增加 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 减少 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a copy of this year with the specified amount subtracted.
     * <p>
     * This returns a {@code Year}, based on this one, with the specified amount subtracted.
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
     * @return a {@code Year} based on this year with the subtraction made, not null
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
    public Year minus(TemporalAmount amountToSubtract) {
        return (Year) amountToSubtract.subtractFrom(this);
    }
    
    /**
     * Returns a copy of this year with the specified amount subtracted.
     * <p>
     * This returns a {@code Year}, based on this one, with the amount
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
     * @return a {@code Year} based on this year with the specified amount subtracted, not null
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
    public Year minus(long amountToSubtract, TemporalUnit unit) {
        if(amountToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, unit).plus(1, unit);
        }
        
        return plus(-amountToSubtract, unit);
    }
    
    /**
     * Returns a copy of this {@code Year} with the specified number of years subtracted.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param yearsToSubtract the years to subtract, may be negative
     *
     * @return a {@code Year} based on this year with the year subtracted, not null
     *
     * @throws DateTimeException if the result exceeds the supported range
     */
    /*
     * 在当前时间量的值上减去yearsToSubtract年
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public Year minusYears(long yearsToSubtract) {
        if(yearsToSubtract == Long.MIN_VALUE) {
            return plusYears(Long.MAX_VALUE).plusYears(1);
        }
        
        return plusYears(-yearsToSubtract);
    }
    
    /*▲ 减少 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量单位 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if the specified unit is supported.
     * <p>
     * This checks if the specified unit can be added to, or subtracted from, this year.
     * If false, then calling the {@link #plus(long, TemporalUnit)} and
     * {@link #minus(long, TemporalUnit) minus} methods will throw an exception.
     * <p>
     * If the unit is a {@link ChronoUnit} then the query is implemented here.
     * The supported units are:
     * <ul>
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
            return unit == YEARS || unit == DECADES || unit == CENTURIES || unit == MILLENNIA || unit == ERAS;
        }
        return unit != null && unit.isSupportedBy(this);
    }
    
    /*▲ 时间量单位 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量字段操作(TemporalAccessor) ███████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if the specified field is supported.
     * <p>
     * This checks if this year can be queried for the specified field.
     * If false, then calling the {@link #range(TemporalField) range},
     * {@link #get(TemporalField) get} and {@link #with(TemporalField, long)}
     * methods will throw an exception.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The supported fields are:
     * <ul>
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
     * @return true if the field is supported on this year, false if not
     */
    // 判断当前时间量是否支持指定的时间量字段
    @Override
    public boolean isSupported(TemporalField field) {
        if(field instanceof ChronoField) {
            return field == ChronoField.YEAR || field == ChronoField.YEAR_OF_ERA || field == ChronoField.ERA;
        }
        
        return field != null && field.isSupportedBy(this);
    }
    
    /**
     * Gets the range of valid values for the specified field.
     * <p>
     * The range object expresses the minimum and maximum valid values for a field.
     * This year is used to enhance the accuracy of the returned range.
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
        if(field == ChronoField.YEAR_OF_ERA) {
            if(year<=0) {
                return ValueRange.of(1, MAX_VALUE + 1);
            }
        
            return ValueRange.of(1, MAX_VALUE);
        }
    
        return Temporal.super.range(field);
    }
    
    /**
     * Gets the value of the specified field from this year as an {@code int}.
     * <p>
     * This queries this year for the value of the specified field.
     * The returned value will always be within the valid range of values for the field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this year.
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
     * ChronoField.YEAR        - 返回"Proleptic年"部件
     * ....................... ...........................................
     * ChronoField.YEAR_OF_ERA - 将"Proleptic年"转换为位于纪元中的[年]后返回
     * ChronoField.ERA         - 计算"Proleptic年"年所在的纪元；在公历系统中，0是公元前，1是公元(后)
     */
    @Override
    public int get(TemporalField field) {
        long longValue = getLong(field);
        return range(field).checkValidIntValue(longValue, field);
    }
    
    /**
     * Gets the value of the specified field from this year as a {@code long}.
     * <p>
     * This queries this year for the value of the specified field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this year.
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
     * ChronoField.YEAR        - 返回"Proleptic年"部件
     * ....................... ...........................................
     * ChronoField.YEAR_OF_ERA - 将"Proleptic年"转换为位于纪元中的[年]后返回
     * ChronoField.ERA         - 计算"Proleptic年"年所在的纪元；在公历系统中，0是公元前，1是公元(后)
     */
    @Override
    public long getLong(TemporalField field) {
        if(field instanceof ChronoField) {
            switch((ChronoField) field) {
                case YEAR_OF_ERA:
                    return (year<1 ? 1 - year : year);
                case YEAR:
                    return year;
                case ERA:
                    return (year<1 ? 0 : 1);
            }
            
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        
        return field.getFrom(this);
    }
    
    /**
     * Queries this year using the specified query.
     * <p>
     * This queries this year using the specified query strategy object.
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
            return (R) YEARS;
        }
        
        if(query == TemporalQueries.chronology()) {
            return (R) IsoChronology.INSTANCE;
        }
        
        return Temporal.super.query(query);
    }
    
    /*▲ 时间量字段操作(TemporalAccessor) ███████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 整合 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an adjusted copy of this year.
     * <p>
     * This returns a {@code Year}, based on this one, with the year adjusted.
     * The adjustment takes place using the specified adjuster strategy object.
     * Read the documentation of the adjuster to understand what adjustment will be made.
     * <p>
     * The result of this method is obtained by invoking the
     * {@link TemporalAdjuster#adjustInto(Temporal)} method on the
     * specified adjuster passing {@code this} as the argument.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param adjuster the adjuster to use, not null
     *
     * @return a {@code Year} based on {@code this} with the adjustment made, not null
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
    public Year with(TemporalAdjuster adjuster) {
        return (Year) adjuster.adjustInto(this);
    }
    
    /**
     * Returns a copy of this year with the specified field set to a new value.
     * <p>
     * This returns a {@code Year}, based on this one, with the value
     * for the specified field changed.
     * If it is not possible to set the value, because the field is not supported or for
     * some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the adjustment is implemented here.
     * The supported fields behave as follows:
     * <ul>
     * <li>{@code YEAR_OF_ERA} -
     *  Returns a {@code Year} with the specified year-of-era
     *  The era will be unchanged.
     * <li>{@code YEAR} -
     *  Returns a {@code Year} with the specified year.
     *  This completely replaces the date and is equivalent to {@link #of(int)}.
     * <li>{@code ERA} -
     *  Returns a {@code Year} with the specified era.
     *  The year-of-era will be unchanged.
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
     * @return a {@code Year} based on {@code this} with the specified field set, not null
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
     * ChronoField.YEAR        - 与[Proleptic-年]整合，只会覆盖当前时间量的"Proleptic年"部件
     * ....................................................................................
     * ChronoField.YEAR_OF_ERA - 与位于纪元中的[年]整合，这会将该年份进行转换后覆盖"Proleptic年"部件
     * ChronoField.ERA         - 与[纪元]整合，即切换公元前与公元(后)
     */
    @Override
    public Year with(TemporalField field, long newValue) {
        if(field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            f.checkValidValue(newValue);
            
            switch(f) {
                case YEAR_OF_ERA:
                    return Year.of((int) (year >= 1 ? newValue : 1 - newValue));
                case YEAR:
                    return Year.of((int) newValue);
                case ERA:
                    return (getLong(ChronoField.ERA) == newValue ? this : Year.of(1 - year));
            }
            
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        
        return field.adjustInto(this, newValue);
    }
    
    /**
     * Adjusts the specified temporal object to have this year.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with the year changed to be the same as this.
     * <p>
     * The adjustment is equivalent to using {@link Temporal#with(TemporalField, long)}
     * passing {@link ChronoField#YEAR} as the field.
     * If the specified temporal object does not use the ISO calendar system then
     * a {@code DateTimeException} is thrown.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#with(TemporalAdjuster)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisYear.adjustInto(temporal);
     *   temporal = temporal.with(thisYear);
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
     * ChronoField.YEAR - 当前时间量的"Proleptic年"部件
     *
     * 目标时间量temporal的取值可以是：
     * Year
     * YearMonth
     * LocalDate
     * LocalDateTime
     * OffsetDateTime
     * ZonedDateTime
     * ChronoLocalDateTimeImpl
     * ChronoZonedDateTimeImpl
     */
    @Override
    public Temporal adjustInto(Temporal temporal) {
        // 确保temporal是基于ISO历法系统的时间量
        if(!Chronology.from(temporal).equals(IsoChronology.INSTANCE)) {
            throw new DateTimeException("Adjustment only supported on ISO date-time");
        }
        
        return temporal.with(ChronoField.YEAR, year);
    }
    
    /*▲ 整合 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Calculates the amount of time until another year in terms of the specified unit.
     * <p>
     * This calculates the amount of time between two {@code Year}
     * objects in terms of a single {@code TemporalUnit}.
     * The start and end points are {@code this} and the specified year.
     * The result will be negative if the end is before the start.
     * The {@code Temporal} passed to this method is converted to a
     * {@code Year} using {@link #from(TemporalAccessor)}.
     * For example, the amount in decades between two year can be calculated
     * using {@code startYear.until(endYear, DECADES)}.
     * <p>
     * The calculation returns a whole number, representing the number of
     * complete units between the two years.
     * For example, the amount in decades between 2012 and 2031
     * will only be one decade as it is one year short of two decades.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method.
     * The second is to use {@link TemporalUnit#between(Temporal, Temporal)}:
     * <pre>
     *   // these two lines are equivalent
     *   amount = start.until(end, YEARS);
     *   amount = YEARS.between(start, end);
     * </pre>
     * The choice should be made based on which makes the code more readable.
     * <p>
     * The calculation is implemented in this method for {@link ChronoUnit}.
     * The units {@code YEARS}, {@code DECADES}, {@code CENTURIES},
     * {@code MILLENNIA} and {@code ERAS} are supported.
     * Other {@code ChronoUnit} values will throw an exception.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.between(Temporal, Temporal)}
     * passing {@code this} as the first argument and the converted input temporal
     * as the second argument.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param endExclusive the end date, exclusive, which is converted to a {@code Year}, not null
     * @param unit         the unit to measure the amount in, not null
     *
     * @return the amount of time between this year and the end year
     *
     * @throws DateTimeException                if the amount cannot be calculated, or the end
     *                                          temporal cannot be converted to a {@code Year}
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException              if numeric overflow occurs
     */
    // 计算当前时间量到目标时间量endExclusive之间相差多少个unit单位的时间值
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        Year end = Year.from(endExclusive);
    
        if(unit instanceof ChronoUnit) {
            long yearsUntil = ((long) end.year) - year;  // no overflow
        
            switch((ChronoUnit) unit) {
                case YEARS:
                    return yearsUntil;
                case DECADES:
                    return yearsUntil / 10;
                case CENTURIES:
                    return yearsUntil / 100;
                case MILLENNIA:
                    return yearsUntil / 1000;
                case ERAS:
                    return end.getLong(ChronoField.ERA) - getLong(ChronoField.ERA);
            }
        
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
    
        return unit.between(this, end);
    }
    
    /**
     * Checks if this year is after the specified year.
     *
     * @param other the other year to compare to, not null
     *
     * @return true if this is after the specified year
     */
    // 判断当前年份是否晚于参数中指定的年份
    public boolean isAfter(Year other) {
        return year>other.year;
    }
    
    /**
     * Checks if this year is before the specified year.
     *
     * @param other the other year to compare to, not null
     *
     * @return true if this point is before the specified year
     */
    // 判断当前年份是否早于参数中指定的年份
    public boolean isBefore(Year other) {
        return year<other.year;
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
     * @param year the year to check
     *
     * @return true if the year is leap, false otherwise
     */
    /*
     * 判断当前年份是否为闰年
     *
     * 太阳连续两次通过春分点的时间间隔被称为一个太阳年，又被称为一个回归年。
     * 根据公元1980年~公元2100年每回归年的时间长度计算得：
     *     1回归年 = 365.2422天，即约等于365天5小时48分46秒。
     * 这是根据121个回归年的平均值计算的结果，每个回归年的实际时间长短并不相等。
     * 在初始的公历系统中，每年被规定为365天，这就导致公历年每年比回归年慢0.2422天，
     * 每4年的话就是慢了4*0.2422=0.9688天，即将近慢了一天。
     * 如果每隔4年在公历年上人为增加一天，即2月从28天变为29天，这就是我们说的"闰年"。
     * 即(year%4==0)被认为是闰年。
     * 这样一来，"差不多"就可以弥补公历年慢的天数。
     * 但是这样的话，1-0.9688=0.0312，这又导致公历年每4年比回归年的4年快了0.0312天。
     * 0.0312*100=3.12，即公历年每400年就比回归年的400年快了3.12天。
     * 因此，为了使公历年与回归年更加匹配，每400年必须再减去3.12天。
     * 每个400年中，必定有3个百年是不能被400整除的，
     * 因此，我们可以取消这3个百年的"闰年"身份，使其成为平年。
     * 但是对于那个能被400整除的百年，我们依然保留它的"闰年'身份。
     * 这样一来，每400个公历年就只比400个回归年快3.12-3=0.12天了，
     * 进一步计算，每个公历年比回归年快0.0003天，即：
     *     1公历年 = 365.2422+0.0003 = 365.2425天
     * 即每过3333年，公历年大概会比回归年快1天，这个误差在目前是可以接受的。
     * 经过上述人为定义后，我们可以知道，
     * 对于非百年的年份，只要该年份能被4整除，它就是"闰年"。
     * 对于百年的年份来说，只有它被400整除，才被认为是"闰年"。
     * 用程序表示"闰年"就是：(year%100!=0 && year%4==0) || (year%100==0 && year%400==0) -- (1)的计算结果为true，
     * 由于year%4!=0时，(year%100==0 && year%400==0)必定为false，
     * 因此，(1)式可以简化为：(year%100!=0 && year%4==0) || (year%400==0) -- (2)。
     * 根据逻辑运算的分配律，(2)式等价于(year%100!=0 || year%400==0) && (year%4==0 || year%400==0) -- (3)。
     * 因为year%4!=0时，year%400==0必定为false，
     * 因此，(3)式可以简化为：(year%100!=0 || year%400==0) && (year%4==0) -- (4)，本方法就是用了这个判别式。
     *
     * 综上所述，判断闰年的表达式可以为：
     * (year%100!=0 && year%4==0) || (year%100==0 && year%400==0) -- (1)
     * year%400==0 || (year%4==0 && year%100!=0) -- (2)
     * year%4==0 && (year%100!=0 || year%400==0) -- (4)
     */
    public static boolean isLeap(long year) {
        // 能被4整除，且不能被100整除；或者，能被400整除
        return ((year & 3) == 0) && ((year % 100) != 0 || (year % 400) == 0);
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
    // 判断当前年份是否为闰年
    public boolean isLeap() {
        return Year.isLeap(year);
    }
    
    /**
     * Gets the length of this year in days.
     *
     * @return the length of this year in days, 365 or 366
     */
    // 返回当前年份包含的天数
    public int length() {
        return isLeap() ? 366 : 365;
    }
    
    /**
     * Checks if the month-day is valid for this year.
     * <p>
     * This method checks whether this year and the input month and day form
     * a valid date.
     *
     * @param monthDay the month-day to validate, null returns false
     *
     * @return true if the month and day are valid for this year
     */
    // 判断monthDay是否处于当前年份中
    public boolean isValidMonthDay(MonthDay monthDay) {
        return monthDay != null && monthDay.isValidYear(year);
    }
    
    /**
     * Formats this year using the specified formatter.
     * <p>
     * This year will be passed to the formatter to produce a string.
     *
     * @param formatter the formatter to use, not null
     *
     * @return the formatted year string, not null
     *
     * @throws DateTimeException if an error occurs during printing
     */
    // 将当前日期转换为一个指定格式的字符串后返回
    public String format(DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Compares this year to another year.
     * <p>
     * The comparison is based on the value of the year.
     * It is "consistent with equals", as defined by {@link Comparable}.
     *
     * @param other the other year to compare to, not null
     *
     * @return the comparator value, negative if less, positive if greater
     */
    @Override
    public int compareTo(Year other) {
        return year - other.year;
    }
    
    /**
     * Outputs this year as a {@code String}.
     *
     * @return a string representation of this year, not null
     */
    @Override
    public String toString() {
        return Integer.toString(year);
    }
    
    /**
     * Checks if this year is equal to another year.
     * <p>
     * The comparison is based on the time-line position of the years.
     *
     * @param obj the object to check, null returns false
     *
     * @return true if this is equal to the other year
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj instanceof Year) {
            return year == ((Year) obj).year;
        }
        return false;
    }
    
    /**
     * A hash code for this year.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return year;
    }
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = -23038383694477807L;
    
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
     *  out.writeByte(11);  // identifies a Year
     *  out.writeInt(year);
     * </pre>
     */
    private Object writeReplace() {
        return new Ser(Ser.YEAR_TYPE, this);
    }
    
    static Year readExternal(DataInput in) throws IOException {
        return Year.of(in.readInt());
    }
    
    void writeExternal(DataOutput out) throws IOException {
        out.writeInt(year);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
