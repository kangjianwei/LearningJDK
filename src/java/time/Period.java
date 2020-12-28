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
 * Copyright (c) 2008-2012, Stephen Colebourne & Michael Nascimento Santos
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
import java.time.chrono.ChronoPeriod;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.YEARS;

/**
 * A date-based amount of time in the ISO-8601 calendar system,
 * such as '2 years, 3 months and 4 days'.
 * <p>
 * This class models a quantity or amount of time in terms of years, months and days.
 * See {@link Duration} for the time-based equivalent to this class.
 * <p>
 * Durations and periods differ in their treatment of daylight savings time
 * when added to {@link ZonedDateTime}. A {@code Duration} will add an exact
 * number of seconds, thus a duration of one day is always exactly 24 hours.
 * By contrast, a {@code Period} will add a conceptual day, trying to maintain
 * the local time.
 * <p>
 * For example, consider adding a period of one day and a duration of one day to
 * 18:00 on the evening before a daylight savings gap. The {@code Period} will add
 * the conceptual day and result in a {@code ZonedDateTime} at 18:00 the following day.
 * By contrast, the {@code Duration} will add exactly 24 hours, resulting in a
 * {@code ZonedDateTime} at 19:00 the following day (assuming a one hour DST gap).
 * <p>
 * The supported units of a period are {@link ChronoUnit#YEARS YEARS},
 * {@link ChronoUnit#MONTHS MONTHS} and {@link ChronoUnit#DAYS DAYS}.
 * All three fields are always present, but may be set to zero.
 * <p>
 * The ISO-8601 calendar system is the modern civil calendar system used today
 * in most of the world. It is equivalent to the proleptic Gregorian calendar
 * system, in which today's rules for leap years are applied for all time.
 * <p>
 * The period is modeled as a directed amount of time, meaning that individual parts of the
 * period may be negative.
 *
 * <p>
 * This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; use of identity-sensitive operations (including reference equality
 * ({@code ==}), identity hash code, or synchronization) on instances of
 * {@code Period} may have unpredictable results and should be avoided.
 * The {@code equals} method should be used for comparisons.
 *
 * @implSpec This class is immutable and thread-safe.
 * @since 1.8
 */
// 时间段，包含年/月/日部件，精确到天；"日期"部件基于[ISO]历法系统
public final class Period implements ChronoPeriod, Serializable {
    
    /**
     * The pattern for parsing.
     */
    private static final Pattern PATTERN = Pattern.compile("([-+]?)P(?:([-+]?[0-9]+)Y)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)W)?(?:([-+]?[0-9]+)D)?", Pattern.CASE_INSENSITIVE);
    
    /**
     * The set of supported units.
     */
    // 当前"时间段"内包含的时间量单位
    private static final List<TemporalUnit> SUPPORTED_UNITS = List.of(YEARS, MONTHS, DAYS);
    
    /**
     * A constant for a period of zero.
     */
    public static final Period ZERO = new Period(0, 0, 0);  // 各项时间量为0的时间段
    
    /**
     * The number of years.
     */
    private final int years;    // "年"的数量
    /**
     * The number of months.
     */
    private final int months;   // "月"的数量
    /**
     * The number of days.
     */
    private final int days;     // "天"的数量
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructor.
     *
     * @param years  the amount
     * @param months the amount
     * @param days   the amount
     */
    private Period(int years, int months, int days) {
        this.years = years;
        this.months = months;
        this.days = days;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains a {@code Period} representing a number of years.
     * <p>
     * The resulting period will have the specified years.
     * The months and days units will be zero.
     *
     * @param years the number of years, positive or negative
     *
     * @return the period of years, not null
     */
    // 返回由"年"构造的时间段
    public static Period ofYears(int years) {
        return create(years, 0, 0);
    }
    
    /**
     * Obtains a {@code Period} representing a number of months.
     * <p>
     * The resulting period will have the specified months.
     * The years and days units will be zero.
     *
     * @param months the number of months, positive or negative
     *
     * @return the period of months, not null
     */
    // 返回由"月"构造的时间段
    public static Period ofMonths(int months) {
        return create(0, months, 0);
    }
    
    /**
     * Obtains a {@code Period} representing a number of weeks.
     * <p>
     * The resulting period will be day-based, with the amount of days
     * equal to the number of weeks multiplied by 7.
     * The years and months units will be zero.
     *
     * @param weeks the number of weeks, positive or negative
     *
     * @return the period, with the input weeks converted to days, not null
     */
    // 返回由"周"构造的时间段
    public static Period ofWeeks(int weeks) {
        // 获取weeks周包含的天数
        int days = Math.multiplyExact(weeks, 7);
        return create(0, 0, days);
    }
    
    /**
     * Obtains a {@code Period} representing a number of days.
     * <p>
     * The resulting period will have the specified days.
     * The years and months units will be zero.
     *
     * @param days the number of days, positive or negative
     *
     * @return the period of days, not null
     */
    // 返回由"天"构造的时间段
    public static Period ofDays(int days) {
        return create(0, 0, days);
    }
    
    /**
     * Obtains a {@code Period} representing a number of years, months and days.
     * <p>
     * This creates an instance based on years, months and days.
     *
     * @param years  the amount of years, may be negative
     * @param months the amount of months, may be negative
     * @param days   the amount of days, may be negative
     *
     * @return the period of years, months and days, not null
     */
    // 返回由"年/月/天"构造的时间段
    public static Period of(int years, int months, int days) {
        return create(years, months, days);
    }
    
    /**
     * Obtains an instance of {@code Period} from a temporal amount.
     * <p>
     * This obtains a period based on the specified amount.
     * A {@code TemporalAmount} represents an  amount of time, which may be
     * date-based or time-based, which this factory extracts to a {@code Period}.
     * <p>
     * The conversion loops around the set of units from the amount and uses
     * the {@link ChronoUnit#YEARS YEARS}, {@link ChronoUnit#MONTHS MONTHS}
     * and {@link ChronoUnit#DAYS DAYS} units to create a period.
     * If any other units are found then an exception is thrown.
     * <p>
     * If the amount is a {@code ChronoPeriod} then it must use the ISO chronology.
     *
     * @param amount the temporal amount to convert, not null
     *
     * @return the equivalent period, not null
     *
     * @throws DateTimeException   if unable to convert to a {@code Period}
     * @throws ArithmeticException if the amount of years, months or days exceeds an int
     */
    // 将指定的"时间段"转换为Period；要求amount包含YEARS/MONTHS/DAYS单位
    public static Period from(TemporalAmount amount) {
        Objects.requireNonNull(amount, "amount");
    
        if(amount instanceof Period) {
            return (Period) amount;
        }
    
        if(amount instanceof ChronoPeriod) {
            // 确保所用的历法系统一致
            if(!IsoChronology.INSTANCE.equals(((ChronoPeriod) amount).getChronology())) {
                throw new DateTimeException("Period requires ISO chronology: " + amount);
            }
        }
    
        int years = 0;
        int months = 0;
        int days = 0;
    
    
        // 获取"时间段"amount内包含的时间量单位
        List<TemporalUnit> temporalUnits = amount.getUnits();
    
        // 遍历所有时间量单位
        for(TemporalUnit unit : temporalUnits) {
            // 获取"时间段"amount中指定的时间量单位unit对应的时间量数值
            long unitAmount = amount.get(unit);
        
            if(unit == ChronoUnit.YEARS) {
                years = Math.toIntExact(unitAmount);
            } else if(unit == ChronoUnit.MONTHS) {
                months = Math.toIntExact(unitAmount);
            } else if(unit == ChronoUnit.DAYS) {
                days = Math.toIntExact(unitAmount);
            } else {
                throw new DateTimeException("Unit must be Years, Months or Days, but was " + unit);
            }
        }
    
        return create(years, months, days);
    }
    
    /**
     * Obtains a {@code Period} from a text string such as {@code PnYnMnD}.
     * <p>
     * This will parse the string produced by {@code toString()} which is
     * based on the ISO-8601 period formats {@code PnYnMnD} and {@code PnW}.
     * <p>
     * The string starts with an optional sign, denoted by the ASCII negative
     * or positive symbol. If negative, the whole period is negated.
     * The ASCII letter "P" is next in upper or lower case.
     * There are then four sections, each consisting of a number and a suffix.
     * At least one of the four sections must be present.
     * The sections have suffixes in ASCII of "Y", "M", "W" and "D" for
     * years, months, weeks and days, accepted in upper or lower case.
     * The suffixes must occur in order.
     * The number part of each section must consist of ASCII digits.
     * The number may be prefixed by the ASCII negative or positive symbol.
     * The number must parse to an {@code int}.
     * <p>
     * The leading plus/minus sign, and negative values for other units are
     * not part of the ISO-8601 standard. In addition, ISO-8601 does not
     * permit mixing between the {@code PnYnMnD} and {@code PnW} formats.
     * Any week-based input is multiplied by 7 and treated as a number of days.
     * <p>
     * For example, the following are valid inputs:
     * <pre>
     *   "P2Y"             -- Period.ofYears(2)
     *   "P3M"             -- Period.ofMonths(3)
     *   "P4W"             -- Period.ofWeeks(4)
     *   "P5D"             -- Period.ofDays(5)
     *   "P1Y2M3D"         -- Period.of(1, 2, 3)
     *   "P1Y2M3W4D"       -- Period.of(1, 2, 25)
     *   "P-1Y2M"          -- Period.of(-1, 2, 0)
     *   "-P1Y2M"          -- Period.of(-1, -2, 0)
     * </pre>
     *
     * @param text the text to parse, not null
     *
     * @return the parsed period, not null
     *
     * @throws DateTimeParseException if the text cannot be parsed to a period
     */
    /*
     * 从给定的文本中解析出一段时间信息，文本格式为"PnYnMnD"。
     *
     * "P2Y"        ->  2年
     * "P3M"        ->  3个月
     * "P4W"        ->  4周
     * "P5D"        ->  5天
     * "P1Y2M3D"    ->  1年2个月3天
     * "P1Y2M3W4D"  ->  1年2个月3周4天
     * "P-1Y2M"     -> -1年+2个月
     * "-P1Y2M"     -> -1年-2个月
     */
    public static Period parse(CharSequence text) {
        Objects.requireNonNull(text, "text");
        Matcher matcher = PATTERN.matcher(text);
        if(matcher.matches()) {
            int negate = (charMatch(text, matcher.start(1), matcher.end(1), '-') ? -1 : 1);
            int yearStart = matcher.start(2), yearEnd = matcher.end(2);
            int monthStart = matcher.start(3), monthEnd = matcher.end(3);
            int weekStart = matcher.start(4), weekEnd = matcher.end(4);
            int dayStart = matcher.start(5), dayEnd = matcher.end(5);
            if(yearStart >= 0 || monthStart >= 0 || weekStart >= 0 || dayStart >= 0) {
                try {
                    int years = parseNumber(text, yearStart, yearEnd, negate);
                    int months = parseNumber(text, monthStart, monthEnd, negate);
                    int weeks = parseNumber(text, weekStart, weekEnd, negate);
                    int days = parseNumber(text, dayStart, dayEnd, negate);
                    days = Math.addExact(days, Math.multiplyExact(weeks, 7));
                    return create(years, months, days);
                } catch(NumberFormatException ex) {
                    throw new DateTimeParseException("Text cannot be parsed to a Period", text, 0, ex);
                }
            }
        }
        throw new DateTimeParseException("Text cannot be parsed to a Period", text, 0);
    }
    
    /**
     * Obtains a {@code Period} consisting of the number of years, months,
     * and days between two dates.
     * <p>
     * The start date is included, but the end date is not.
     * The period is calculated by removing complete months, then calculating
     * the remaining number of days, adjusting to ensure that both have the same sign.
     * The number of months is then split into years and months based on a 12 month year.
     * A month is considered if the end day-of-month is greater than or equal to the start day-of-month.
     * For example, from {@code 2010-01-15} to {@code 2011-03-18} is one year, two months and three days.
     * <p>
     * The result of this method can be a negative period if the end is before the start.
     * The negative sign will be the same in each of year, month and day.
     *
     * @param startDateInclusive the start date, inclusive, not null
     * @param endDateExclusive   the end date, exclusive, not null
     *
     * @return the period between this date and the end date, not null
     *
     * @see ChronoLocalDate#until(ChronoLocalDate)
     */
    // 计算两个"日期"时间点之间的时间段
    public static Period between(LocalDate startDateInclusive, LocalDate endDateExclusive) {
        return startDateInclusive.until(endDateExclusive);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the total number of months in this period.
     * <p>
     * This returns the total number of months in the period by multiplying the
     * number of years by 12 and adding the number of months.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return the total number of months in the period, may be negative
     */
    // 返回当前"时间段"包含的月数
    public long toTotalMonths() {
        return years * 12L + months;
    }
    
    /*▲ 转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 部件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the amount of years of this period.
     * <p>
     * This returns the years unit.
     * <p>
     * The months unit is not automatically normalized with the years unit.
     * This means that a period of "15 months" is different to a period
     * of "1 year and 3 months".
     *
     * @return the amount of years of this period, may be negative
     */
    // 返回"年"部件
    public int getYears() {
        return years;
    }
    
    /**
     * Gets the amount of months of this period.
     * <p>
     * This returns the months unit.
     * <p>
     * The months unit is not automatically normalized with the years unit.
     * This means that a period of "15 months" is different to a period
     * of "1 year and 3 months".
     *
     * @return the amount of months of this period, may be negative
     */
    // 返回"月"部件
    public int getMonths() {
        return months;
    }
    
    /**
     * Gets the amount of days of this period.
     * <p>
     * This returns the days unit.
     *
     * @return the amount of days of this period, may be negative
     */
    // 返回"日"部件
    public int getDays() {
        return days;
    }
    
    /*▲ 部件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 加法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a copy of this period with the specified period added.
     * <p>
     * This operates separately on the years, months and days.
     * No normalization is performed.
     * <p>
     * For example, "1 year, 6 months and 3 days" plus "2 years, 2 months and 2 days"
     * returns "3 years, 8 months and 5 days".
     * <p>
     * The specified amount is typically an instance of {@code Period}.
     * Other types are interpreted using {@link Period#from(TemporalAmount)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToAdd the amount to add, not null
     *
     * @return a {@code Period} based on this period with the requested period added, not null
     *
     * @throws DateTimeException   if the specified amount has a non-ISO chronology or
     *                             contains an invalid unit
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 对当前"时间段"的值与参数中的"时间段"求和
     *
     * 如果求和后的值与当前"时间段"的值相等，则直接返回当前"时间段"对象。
     * 否则，需要构造"求和"后的新对象再返回。
     */
    public Period plus(TemporalAmount amountToAdd) {
        Period isoAmount = Period.from(amountToAdd);
        
        return create(Math.addExact(years, isoAmount.years), Math.addExact(months, isoAmount.months), Math.addExact(days, isoAmount.days));
    }
    
    /**
     * Returns a copy of this period with the specified years added.
     * <p>
     * This adds the amount to the years unit in a copy of this period.
     * The months and days units are unaffected.
     * For example, "1 year, 6 months and 3 days" plus 2 years returns "3 years, 6 months and 3 days".
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param yearsToAdd the years to add, positive or negative
     *
     * @return a {@code Period} based on this period with the specified years added, not null
     *
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 在当前"时间段"的值上累加yearsToAdd年
     *
     * 如果累加后的值与当前"时间段"的值相等，则直接返回当前"时间段"对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public Period plusYears(long yearsToAdd) {
        if(yearsToAdd == 0) {
            return this;
        }
        
        return create(Math.toIntExact(Math.addExact(years, yearsToAdd)), months, days);
    }
    
    /**
     * Returns a copy of this period with the specified months added.
     * <p>
     * This adds the amount to the months unit in a copy of this period.
     * The years and days units are unaffected.
     * For example, "1 year, 6 months and 3 days" plus 2 months returns "1 year, 8 months and 3 days".
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param monthsToAdd the months to add, positive or negative
     *
     * @return a {@code Period} based on this period with the specified months added, not null
     *
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 在当前"时间段"的值上累加monthsToAdd月
     *
     * 如果累加后的值与当前"时间段"的值相等，则直接返回当前"时间段"对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public Period plusMonths(long monthsToAdd) {
        if(monthsToAdd == 0) {
            return this;
        }
        
        return create(years, Math.toIntExact(Math.addExact(months, monthsToAdd)), days);
    }
    
    /**
     * Returns a copy of this period with the specified days added.
     * <p>
     * This adds the amount to the days unit in a copy of this period.
     * The years and months units are unaffected.
     * For example, "1 year, 6 months and 3 days" plus 2 days returns "1 year, 6 months and 5 days".
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToAdd the days to add, positive or negative
     *
     * @return a {@code Period} based on this period with the specified days added, not null
     *
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 在当前"时间段"的值上累加daysToAdd天
     *
     * 如果累加后的值与当前"时间段"的值相等，则直接返回当前"时间段"对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    public Period plusDays(long daysToAdd) {
        if(daysToAdd == 0) {
            return this;
        }
        
        return create(years, months, Math.toIntExact(Math.addExact(days, daysToAdd)));
    }
    
    /*▲ 加法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 减法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a copy of this period with the specified period subtracted.
     * <p>
     * This operates separately on the years, months and days.
     * No normalization is performed.
     * <p>
     * For example, "1 year, 6 months and 3 days" minus "2 years, 2 months and 2 days"
     * returns "-1 years, 4 months and 1 day".
     * <p>
     * The specified amount is typically an instance of {@code Period}.
     * Other types are interpreted using {@link Period#from(TemporalAmount)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToSubtract the amount to subtract, not null
     *
     * @return a {@code Period} based on this period with the requested period subtracted, not null
     *
     * @throws DateTimeException   if the specified amount has a non-ISO chronology or
     *                             contains an invalid unit
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 对当前"时间段"的值与参数中的"时间段"求差
     *
     * 如果求差后的值与当前"时间段"的值相等，则直接返回当前"时间段"对象。
     * 否则，需要构造"求差"后的新对象再返回。
     */
    public Period minus(TemporalAmount amountToSubtract) {
        Period isoAmount = Period.from(amountToSubtract);
        
        return create(Math.subtractExact(years, isoAmount.years), Math.subtractExact(months, isoAmount.months), Math.subtractExact(days, isoAmount.days));
    }
    
    /**
     * Returns a copy of this period with the specified years subtracted.
     * <p>
     * This subtracts the amount from the years unit in a copy of this period.
     * The months and days units are unaffected.
     * For example, "1 year, 6 months and 3 days" minus 2 years returns "-1 years, 6 months and 3 days".
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param yearsToSubtract the years to subtract, positive or negative
     *
     * @return a {@code Period} based on this period with the specified years subtracted, not null
     *
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 在当前"时间段"的值上减去yearsToSubtract年
     *
     * 如果减去后的值与当前"时间段"的值相等，则直接返回当前"时间段"对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public Period minusYears(long yearsToSubtract) {
        if(yearsToSubtract == Long.MIN_VALUE) {
            return plusYears(Long.MAX_VALUE).plusYears(1);
        }
        
        return plusYears(-yearsToSubtract);
    }
    
    /**
     * Returns a copy of this period with the specified months subtracted.
     * <p>
     * This subtracts the amount from the months unit in a copy of this period.
     * The years and days units are unaffected.
     * For example, "1 year, 6 months and 3 days" minus 2 months returns "1 year, 4 months and 3 days".
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param monthsToSubtract the years to subtract, positive or negative
     *
     * @return a {@code Period} based on this period with the specified months subtracted, not null
     *
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 在当前"时间段"的值上减去monthsToSubtract月
     *
     * 如果减去后的值与当前"时间段"的值相等，则直接返回当前"时间段"对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public Period minusMonths(long monthsToSubtract) {
        if(monthsToSubtract == Long.MIN_VALUE) {
            return plusMonths(Long.MAX_VALUE).plusMonths(1);
        }
        
        return plusMonths(-monthsToSubtract);
    }
    
    /**
     * Returns a copy of this period with the specified days subtracted.
     * <p>
     * This subtracts the amount from the days unit in a copy of this period.
     * The years and months units are unaffected.
     * For example, "1 year, 6 months and 3 days" minus 2 days returns "1 year, 6 months and 1 day".
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToSubtract the months to subtract, positive or negative
     *
     * @return a {@code Period} based on this period with the specified days subtracted, not null
     *
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 在当前"时间段"的值上减去daysToSubtract天
     *
     * 如果减去后的值与当前时间量的值相等，则直接返回当前"时间段"对象。
     * 否则，需要构造"减去"操作后的新对象再返回。
     */
    public Period minusDays(long daysToSubtract) {
        if(daysToSubtract == Long.MIN_VALUE) {
            return plusDays(Long.MAX_VALUE).plusDays(1);
        }
        
        return plusDays(-daysToSubtract);
    }
    
    /*▲ 减法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 乘法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new instance with each element in this period multiplied
     * by the specified scalar.
     * <p>
     * This returns a period with each of the years, months and days units
     * individually multiplied.
     * For example, a period of "2 years, -3 months and 4 days" multiplied by
     * 3 will return "6 years, -9 months and 12 days".
     * No normalization is performed.
     *
     * @param scalar the scalar to multiply by, not null
     *
     * @return a {@code Period} based on this period with the amounts multiplied by the scalar, not null
     *
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 在当前"时间段"的值上乘以scalar(即放大scalar倍)
     *
     * 如果乘以后的值与当前"时间段"的值相等，则直接返回当前"时间段"对象。
     * 否则，需要构造"乘以"操作后的新对象再返回。
     */
    public Period multipliedBy(int scalar) {
        if(this == ZERO || scalar == 1) {
            return this;
        }
        
        return create(Math.multiplyExact(years, scalar), Math.multiplyExact(months, scalar), Math.multiplyExact(days, scalar));
    }
    
    /*▲ 乘法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 其他运算 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new instance with each amount in this period negated.
     * <p>
     * This returns a period with each of the years, months and days units
     * individually negated.
     * For example, a period of "2 years, -3 months and 4 days" will be
     * negated to "-2 years, 3 months and -4 days".
     * No normalization is performed.
     *
     * @return a {@code Period} based on this period with the amounts negated, not null
     *
     * @throws ArithmeticException if numeric overflow occurs, which only happens if
     *                             one of the units has the value {@code Long.MIN_VALUE}
     */
    // 取相反数
    public Period negated() {
        return multipliedBy(-1);
    }
    
    /**
     * Checks if any of the three units of this period are negative.
     * <p>
     * This checks whether the years, months or days units are less than zero.
     *
     * @return true if any unit of this period is negative
     */
    // 判断当前"时间段"是否为负
    public boolean isNegative() {
        return years<0 || months<0 || days<0;
    }
    
    /*▲ 其他运算 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 增加/减少 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Adds this period to the specified temporal object.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with this period added.
     * If the temporal has a chronology, it must be the ISO chronology.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#plus(TemporalAmount)}.
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   dateTime = thisPeriod.addTo(dateTime);
     *   dateTime = dateTime.plus(thisPeriod);
     * </pre>
     * <p>
     * The calculation operates as follows.
     * First, the chronology of the temporal is checked to ensure it is ISO chronology or null.
     * Second, if the months are zero, the years are added if non-zero, otherwise
     * the combination of years and months is added if non-zero.
     * Finally, any days are added.
     * <p>
     * This approach ensures that a partial period can be added to a partial date.
     * For example, a period of years and/or months can be added to a {@code YearMonth},
     * but a period including days cannot.
     * The approach also adds years and months together when necessary, which ensures
     * correct behaviour at the end of the month.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param temporal the temporal object to adjust, not null
     *
     * @return an object of the same type with the adjustment made, not null
     *
     * @throws DateTimeException   if unable to add
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 增加目标时间量temporal
     *
     * 尝试将当前"时间段"累加到指定的时间量temporal上，
     * 如果累加后的值与原值相同，则返回temporal自身；否则，会构造一个新对象再返回。
     */
    @Override
    public Temporal addTo(Temporal temporal) {
        validateChrono(temporal);
        
        if(months == 0) {
            if(years != 0) {
                temporal = temporal.plus(years, YEARS);
            }
        } else {
            long totalMonths = toTotalMonths();
            if(totalMonths != 0) {
                temporal = temporal.plus(totalMonths, MONTHS);
            }
        }
        
        if(days != 0) {
            temporal = temporal.plus(days, DAYS);
        }
        
        return temporal;
    }
    
    /**
     * Subtracts this period from the specified temporal object.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with this period subtracted.
     * If the temporal has a chronology, it must be the ISO chronology.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#minus(TemporalAmount)}.
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   dateTime = thisPeriod.subtractFrom(dateTime);
     *   dateTime = dateTime.minus(thisPeriod);
     * </pre>
     * <p>
     * The calculation operates as follows.
     * First, the chronology of the temporal is checked to ensure it is ISO chronology or null.
     * Second, if the months are zero, the years are subtracted if non-zero, otherwise
     * the combination of years and months is subtracted if non-zero.
     * Finally, any days are subtracted.
     * <p>
     * This approach ensures that a partial period can be subtracted from a partial date.
     * For example, a period of years and/or months can be subtracted from a {@code YearMonth},
     * but a period including days cannot.
     * The approach also subtracts years and months together when necessary, which ensures
     * correct behaviour at the end of the month.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param temporal the temporal object to adjust, not null
     *
     * @return an object of the same type with the adjustment made, not null
     *
     * @throws DateTimeException   if unable to subtract
     * @throws ArithmeticException if numeric overflow occurs
     */
    /*
     * 减少目标时间量temporal
     *
     * 尝试从指定的时间量temporal上减去当前"时间段"，
     * 如果减少后的值与原值相同，则返回temporal自身；否则，会构造一个新对象再返回。
     */
    @Override
    public Temporal subtractFrom(Temporal temporal) {
        validateChrono(temporal);
        
        if(months == 0) {
            if(years != 0) {
                temporal = temporal.minus(years, YEARS);
            }
        } else {
            long totalMonths = toTotalMonths();
            if(totalMonths != 0) {
                temporal = temporal.minus(totalMonths, MONTHS);
            }
        }
        
        if(days != 0) {
            temporal = temporal.minus(days, DAYS);
        }
        
        return temporal;
    }
    
    /*▲ 增加/减少 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量单位 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the set of units supported by this period.
     * <p>
     * The supported units are {@link ChronoUnit#YEARS YEARS},
     * {@link ChronoUnit#MONTHS MONTHS} and {@link ChronoUnit#DAYS DAYS}.
     * They are returned in the order years, months, days.
     * <p>
     * This set can be used in conjunction with {@link #get(TemporalUnit)}
     * to access the entire state of the period.
     *
     * @return a list containing the years, months and days units, not null
     */
    // 返回当前"时间段"上可用的时间量单位，这其实是该"时间段"的组成部件
    @Override
    public List<TemporalUnit> getUnits() {
        return SUPPORTED_UNITS;
    }
    
    /**
     * Gets the value of the requested unit.
     * <p>
     * This returns a value for each of the three supported units,
     * {@link ChronoUnit#YEARS YEARS}, {@link ChronoUnit#MONTHS MONTHS} and
     * {@link ChronoUnit#DAYS DAYS}.
     * All other units throw an exception.
     *
     * @param unit the {@code TemporalUnit} for which to return the value
     *
     * @return the long value of the unit
     *
     * @throws DateTimeException                if the unit is not supported
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     */
    // 返回当前"时间段"中指定的时间量单位unit对应的时间量数值
    @Override
    public long get(TemporalUnit unit) {
        if(unit == ChronoUnit.YEARS) {
            return getYears();
        }
        
        if(unit == ChronoUnit.MONTHS) {
            return getMonths();
        }
        
        if(unit == ChronoUnit.DAYS) {
            return getDays();
        }
        
        throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
    
    /*▲ 时间量单位 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 整合 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a copy of this period with the specified amount of years.
     * <p>
     * This sets the amount of the years unit in a copy of this period.
     * The months and days units are unaffected.
     * <p>
     * The months unit is not automatically normalized with the years unit.
     * This means that a period of "15 months" is different to a period
     * of "1 year and 3 months".
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param years the years to represent, may be negative
     *
     * @return a {@code Period} based on this period with the requested years, not null
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
     * 影响部件：年
     */
    public Period withYears(int years) {
        if(years == this.years) {
            return this;
        }
        return create(years, months, days);
    }
    
    /**
     * Returns a copy of this period with the specified amount of months.
     * <p>
     * This sets the amount of the months unit in a copy of this period.
     * The years and days units are unaffected.
     * <p>
     * The months unit is not automatically normalized with the years unit.
     * This means that a period of "15 months" is different to a period
     * of "1 year and 3 months".
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param months the months to represent, may be negative
     *
     * @return a {@code Period} based on this period with the requested months, not null
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
     * 影响部件：月
     */
    public Period withMonths(int months) {
        if(months == this.months) {
            return this;
        }
        return create(years, months, days);
    }
    
    /**
     * Returns a copy of this period with the specified amount of days.
     * <p>
     * This sets the amount of the days unit in a copy of this period.
     * The years and months units are unaffected.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param days the days to represent, may be negative
     *
     * @return a {@code Period} based on this period with the requested days, not null
     */
    /*
     * 将指定的"天"整合到当前时间量中以构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：天
     */
    public Period withDays(int days) {
        if(days == this.days) {
            return this;
        }
        return create(years, months, days);
    }
    
    /*▲ 整合 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if all three units of this period are zero.
     * <p>
     * A zero period has the value zero for the years, months and days units.
     *
     * @return true if this period is zero-length
     */
    // 判断当前"时间段"的值是否为0，即该"时间段"内所有计时部件的值为0
    public boolean isZero() {
        return (this == ZERO);
    }
    
    /**
     * Gets the chronology of this period, which is the ISO calendar system.
     * <p>
     * The {@code Chronology} represents the calendar system in use.
     * The ISO-8601 calendar system is the modern civil calendar system used today
     * in most of the world. It is equivalent to the proleptic Gregorian calendar
     * system, in which today's rules for leap years are applied for all time.
     *
     * @return the ISO chronology, not null
     */
    // 返回当前"时间段"使用的历法系统
    @Override
    public IsoChronology getChronology() {
        return IsoChronology.INSTANCE;
    }
    
    /**
     * Returns a copy of this period with the years and months normalized.
     * <p>
     * This normalizes the years and months units, leaving the days unit unchanged.
     * The months unit is adjusted to have an absolute value less than 12,
     * with the years unit being adjusted to compensate. For example, a period of
     * "1 Year and 15 months" will be normalized to "2 years and 3 months".
     * <p>
     * The sign of the years and months units will be the same after normalization.
     * For example, a period of "1 year and -25 months" will be normalized to
     * "-1 year and -1 month".
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return a {@code Period} based on this period with excess months normalized to years, not null
     *
     * @throws ArithmeticException if numeric overflow occurs
     */
    // 返回当前"时间段"的一个规范表示：即先从比较大的单位开始填充数据
    public Period normalized() {
        // 获取当前"时间段"包含的月数
        long totalMonths = toTotalMonths();
        
        // 年部件
        long splitYears = totalMonths / 12;
        // 月部件
        int splitMonths = (int) (totalMonths % 12);
        
        // 已经规范，原样返回
        if(splitYears == years && splitMonths == months) {
            return this;
        }
        
        return create(Math.toIntExact(splitYears), splitMonths, days);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    private static boolean charMatch(CharSequence text, int start, int end, char c) {
        return (start >= 0 && end == start + 1 && text.charAt(start) == c);
    }
    
    private static int parseNumber(CharSequence text, int start, int end, int negate) {
        if(start<0 || end<0) {
            return 0;
        }
        int val = Integer.parseInt(text, start, end, 10);
        try {
            return Math.multiplyExact(val, negate);
        } catch(ArithmeticException ex) {
            throw new DateTimeParseException("Text cannot be parsed to a Period", text, 0, ex);
        }
    }
    
    /**
     * Creates an instance.
     *
     * @param years  the amount
     * @param months the amount
     * @param days   the amount
     */
    private static Period create(int years, int months, int days) {
        if((years | months | days) == 0) {
            return ZERO;
        }
        
        return new Period(years, months, days);
    }
    
    /**
     * Validates that the temporal has the correct chronology.
     */
    private void validateChrono(TemporalAccessor temporal) {
        Objects.requireNonNull(temporal, "temporal");
        
        Chronology temporalChrono = temporal.query(TemporalQueries.chronology());
        if(temporalChrono != null && !IsoChronology.INSTANCE.equals(temporalChrono)) {
            throw new DateTimeException("Chronology mismatch, expected: ISO, actual: " + temporalChrono.getId());
        }
    }
    
    
    /**
     * Outputs this period as a {@code String}, such as {@code P6Y3M1D}.
     * <p>
     * The output will be in the ISO-8601 period format.
     * A zero period will be represented as zero days, 'P0D'.
     *
     * @return a string representation of this period, not null
     */
    @Override
    public String toString() {
        if(this == ZERO) {
            return "P0D";
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append('P');
            if(years != 0) {
                buf.append(years).append('Y');
            }
            if(months != 0) {
                buf.append(months).append('M');
            }
            if(days != 0) {
                buf.append(days).append('D');
            }
            return buf.toString();
        }
    }
    
    /**
     * Checks if this period is equal to another period.
     * <p>
     * The comparison is based on the type {@code Period} and each of the three amounts.
     * To be equal, the years, months and days units must be individually equal.
     * Note that this means that a period of "15 Months" is not equal to a period
     * of "1 Year and 3 Months".
     *
     * @param obj the object to check, null returns false
     *
     * @return true if this is equal to the other period
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj instanceof Period) {
            Period other = (Period) obj;
            return years == other.years && months == other.months && days == other.days;
        }
        return false;
    }
    
    /**
     * A hash code for this period.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return years + Integer.rotateLeft(months, 8) + Integer.rotateLeft(days, 16);
    }
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = -3587258372562876L;
    
    /**
     * Writes the object using a
     * <a href="../../serialized-form.html#java.time.Ser">dedicated serialized form</a>.
     *
     * @return the instance of {@code Ser}, not null
     *
     * @serialData <pre>
     *  out.writeByte(14);  // identifies a Period
     *  out.writeInt(years);
     *  out.writeInt(months);
     *  out.writeInt(days);
     * </pre>
     */
    private Object writeReplace() {
        return new Ser(Ser.PERIOD_TYPE, this);
    }
    
    /**
     * Defend against malicious streams.
     *
     * @param s the stream to read
     *
     * @throws java.io.InvalidObjectException always
     */
    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }
    
    void writeExternal(DataOutput out) throws IOException {
        out.writeInt(years);
        out.writeInt(months);
        out.writeInt(days);
    }
    
    static Period readExternal(DataInput in) throws IOException {
        int years = in.readInt();
        int months = in.readInt();
        int days = in.readInt();
        return Period.of(years, months, days);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
