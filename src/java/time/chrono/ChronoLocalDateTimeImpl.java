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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;
import java.util.Objects;

import static java.time.temporal.ChronoField.EPOCH_DAY;

/**
 * A date-time without a time-zone for the calendar neutral API.
 * <p>
 * {@code ChronoLocalDateTime} is an immutable date-time object that represents a date-time, often
 * viewed as year-month-day-hour-minute-second. This object can also access other
 * fields such as day-of-year, day-of-week and week-of-year.
 * <p>
 * This class stores all date and time fields, to a precision of nanoseconds.
 * It does not store or represent a time-zone. For example, the value
 * "2nd October 2007 at 13:45.30.123456789" can be stored in an {@code ChronoLocalDateTime}.
 *
 * @param <D> the concrete type for the date of this date-time
 *
 * @implSpec This class is immutable and thread-safe.
 * @serial
 * @since 1.8
 */
/*
 * "本地日期-时间"，"时间"[未关联]所属时区ID，"日期"基于[非ISO]历法系统。
 *
 * 这里的非ISO历法系统可以是：
 * HijrahDate       - 伊斯兰历
 * ThaiBuddhistDate - 泰国佛教历
 * JapaneseDate     - 日本历
 * MinguoDate       - 中华民国历
 *
 * 注：该类其实也可以使用基于ISO历法系统的LocalDate，但是并不推荐这么用。
 * 　　这里如果需要使用基于[ISO]历法系统的"本地日期-时间"，推荐直接使用LocalDateTime。
 */
final class ChronoLocalDateTimeImpl<D extends ChronoLocalDate> implements ChronoLocalDateTime<D>, Temporal, TemporalAdjuster, Serializable {
    
    /**
     * Hours per day.
     */
    static final int HOURS_PER_DAY = 24; // 每天24小时
    
    /**
     * Minutes per hour.
     */
    static final int MINUTES_PER_HOUR = 60;                              // 每小时60分
    /**
     * Minutes per day.
     */
    static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY; // 每天24*60分
    
    /**
     * Seconds per minute.
     */
    static final int SECONDS_PER_MINUTE = 60;                                  // 每分60秒
    /**
     * Seconds per hour.
     */
    static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR; // 每小时60*60秒
    /**
     * Seconds per day.
     */
    static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;       // 每天60*60*24秒
    
    /**
     * Milliseconds per day.
     */
    static final long MILLIS_PER_DAY = SECONDS_PER_DAY * 1000L;     // 每天60*60*24*1000毫秒
    /**
     * Microseconds per day.
     */
    static final long MICROS_PER_DAY = SECONDS_PER_DAY * 1000_000L; // 每天60*60*24*1000微秒
    
    /**
     * Nanos per second.
     */
    static final long NANOS_PER_SECOND = 1000_000_000L;                         // 每秒是1000_000_000纳秒
    /**
     * Nanos per minute.
     */
    static final long NANOS_PER_MINUTE = NANOS_PER_SECOND * SECONDS_PER_MINUTE; // 每分钟是60*1000_000_000纳秒
    /**
     * Nanos per hour.
     */
    static final long NANOS_PER_HOUR = NANOS_PER_MINUTE * MINUTES_PER_HOUR;     // 每小时是60*60*1000_000_000纳秒
    /**
     * Nanos per day.
     */
    static final long NANOS_PER_DAY = NANOS_PER_HOUR * HOURS_PER_DAY;           // 每天是24*60*60*1000_000_000纳秒
    
    /**
     * The date part.
     */
    private final transient D date;         // "本地日期"部件
    
    /**
     * The time part.
     */
    private final transient LocalTime time; // "本地时间"部件
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructor.
     *
     * @param date the date part of the date-time, not null
     * @param time the time part of the date-time, not null
     */
    private ChronoLocalDateTimeImpl(D date, LocalTime time) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(time, "time");
        
        this.date = date;
        this.time = time;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains an instance of {@code ChronoLocalDateTime} from a date and time.
     *
     * @param date the local date, not null
     * @param time the local time, not null
     *
     * @return the local date-time, not null
     */
    // 使用指定的"本地日期"部件和"本地时间"部件构造一个"本地日期-时间"对象
    static <R extends ChronoLocalDate> ChronoLocalDateTimeImpl<R> of(R date, LocalTime time) {
        return new ChronoLocalDateTimeImpl<>(date, time);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 拿当前时间量与指定的时区ID构造一个属于zone时区的"本地日期-时间"对象
     * 如果zone不是ZoneOffset类型，则时区偏移时间可能会不准确。
     */
    @Override
    public ChronoZonedDateTime<D> atZone(ZoneId zone) {
        return ChronoZonedDateTimeImpl.ofBest(this, zone, null);
    }
    
    /*▲ 转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 部件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回"本地日期"部件
    @Override
    public D toLocalDate() {
        return date;
    }
    
    // 返回"本地时间"部件
    @Override
    public LocalTime toLocalTime() {
        return time;
    }
    
    /*▲ 部件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 增加 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 对当前时间量的值累加amountToAdd个unit单位的时间量
     *
     * 如果累加后的值与当前时间量的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"累加"操作后的新对象再返回。
     */
    @Override
    public ChronoLocalDateTimeImpl<D> plus(long amountToAdd, TemporalUnit unit) {
        if(unit instanceof ChronoUnit) {
            ChronoUnit f = (ChronoUnit) unit;
            switch(f) {
                case NANOS:
                    return plusNanos(amountToAdd);
                case MICROS:
                    return plusDays(amountToAdd / MICROS_PER_DAY).plusNanos((amountToAdd % MICROS_PER_DAY) * 1000);
                case MILLIS:
                    return plusDays(amountToAdd / MILLIS_PER_DAY).plusNanos((amountToAdd % MILLIS_PER_DAY) * 1000000);
                case SECONDS:
                    return plusSeconds(amountToAdd);
                case MINUTES:
                    return plusMinutes(amountToAdd);
                case HOURS:
                    return plusHours(amountToAdd);
                case HALF_DAYS:
                    return plusDays(amountToAdd / 256).plusHours((amountToAdd % 256) * 12);  // no overflow (256 is multiple of 2)
            }
            
            return with(date.plus(amountToAdd, unit), time);
        }
        
        return ChronoLocalDateTimeImpl.ensureValid(date.getChronology(), unit.addTo(this, amountToAdd));
    }
    
    /*▲ 增加 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量字段操作(TemporalAccessor) ███████████████████████████████████████████████████████┓ */
    
    // 判断当前时间量是否支持指定的时间量字段
    @Override
    public boolean isSupported(TemporalField field) {
        if(field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            return f.isDateBased() || f.isTimeBased();
        }
        
        return field != null && field.isSupportedBy(this);
    }
    
    // 返回时间量字段field的取值区间，通常要求当前时间量支持该时间量字段
    @Override
    public ValueRange range(TemporalField field) {
        if(field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            return (f.isTimeBased() ? time.range(field) : date.range(field));
        }
        
        return field.rangeRefinedBy(this);
    }
    
    /*
     * 以int形式返回时间量字段field的值
     *
     * 目前支持的字段包括：
     *
     * LocalTime中支持的字段
     * ===========================
     * ChronoLocalDate中支持的字段
     */
    @Override
    public int get(TemporalField field) {
        if(field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            return (f.isTimeBased() ? time.get(field) : date.get(field));
        }
        
        return range(field).checkValidIntValue(getLong(field), field);
    }
    
    /*
     * 以long形式返回时间量字段field的值
     *
     * 目前支持的字段包括：
     *
     * LocalTime中支持的字段
     * ===========================
     * ChronoLocalDate中支持的字段
     */
    @Override
    public long getLong(TemporalField field) {
        if(field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            
            return (f.isTimeBased() ? time.getLong(field) : date.getLong(field));
        }
        
        return field.getFrom(this);
    }
    
    /*▲ 时间量字段操作(TemporalAccessor) ███████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 整合 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 使用指定的时间量整合器adjuster来构造时间量对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     */
    @SuppressWarnings("unchecked")
    @Override
    public ChronoLocalDateTimeImpl<D> with(TemporalAdjuster adjuster) {
        if(adjuster instanceof ChronoLocalDate) {
            // The Chronology is checked in with(date,time)
            return with((ChronoLocalDate) adjuster, time);
        }
        
        if(adjuster instanceof LocalTime) {
            return with(date, (LocalTime) adjuster);
        }
        
        if(adjuster instanceof ChronoLocalDateTimeImpl) {
            return ChronoLocalDateTimeImpl.ensureValid(date.getChronology(), (ChronoLocalDateTimeImpl<?>) adjuster);
        }
        
        return ChronoLocalDateTimeImpl.ensureValid(date.getChronology(), adjuster.adjustInto(this));
    }
    
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
     * LocalTime中支持的字段
     * ===========================
     * ChronoLocalDate的子类中支持的字段
     */
    @Override
    public ChronoLocalDateTimeImpl<D> with(TemporalField field, long newValue) {
        if(field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            
            // 更新"本地时间"部件
            if(f.isTimeBased()) {
                LocalTime newTime = time.with(field, newValue);
                return with(date, newTime);
                
                // 更新"本地日期"部件
            } else {
                ChronoLocalDate newDate = date.with(field, newValue);
                return with(newDate, time);
            }
        }
        
        return ChronoLocalDateTimeImpl.ensureValid(date.getChronology(), field.adjustInto(this, newValue));
    }
    
    /*▲ 整合 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 计算当前时间量到目标时间量endExclusive之间相差多少个unit单位的时间值
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        Objects.requireNonNull(endExclusive, "endExclusive");
        Objects.requireNonNull(unit, "unit");
        
        @SuppressWarnings("unchecked")
        ChronoLocalDateTime<D> end = (ChronoLocalDateTime<D>) getChronology().localDateTime(endExclusive);
        
        if(unit instanceof ChronoUnit) {
            if(unit.isTimeBased()) {
                long amount = end.getLong(EPOCH_DAY) - date.getLong(EPOCH_DAY);
                
                switch((ChronoUnit) unit) {
                    case NANOS:
                        amount = Math.multiplyExact(amount, NANOS_PER_DAY);
                        break;
                    case MICROS:
                        amount = Math.multiplyExact(amount, MICROS_PER_DAY);
                        break;
                    case MILLIS:
                        amount = Math.multiplyExact(amount, MILLIS_PER_DAY);
                        break;
                    case SECONDS:
                        amount = Math.multiplyExact(amount, SECONDS_PER_DAY);
                        break;
                    case MINUTES:
                        amount = Math.multiplyExact(amount, MINUTES_PER_DAY);
                        break;
                    case HOURS:
                        amount = Math.multiplyExact(amount, HOURS_PER_DAY);
                        break;
                    case HALF_DAYS:
                        amount = Math.multiplyExact(amount, 2);
                        break;
                }
                
                return Math.addExact(amount, time.until(end.toLocalTime(), unit));
            }
            
            ChronoLocalDate endDate = end.toLocalDate();
            if(end.toLocalTime().isBefore(time)) {
                endDate = endDate.minus(1, ChronoUnit.DAYS);
            }
            
            return date.until(endDate, unit);
        }
        
        return unit.between(this, end);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns a copy of this date-time with the new date and time, checking
     * to see if a new object is in fact required.
     *
     * @param newDate the date of the new date-time, not null
     * @param newTime the time of the new date-time, not null
     *
     * @return the date-time, not null
     */
    // 整合newDate和newTime
    private ChronoLocalDateTimeImpl<D> with(Temporal newDate, LocalTime newTime) {
        if(date == newDate && time == newTime) {
            return this;
        }
        
        // Validate that the new Temporal is a ChronoLocalDate (and not something else)
        D cd = ChronoLocalDateImpl.ensureValid(date.getChronology(), newDate);
        
        return new ChronoLocalDateTimeImpl<>(cd, newTime);
    }
    
    // 加上days天
    private ChronoLocalDateTimeImpl<D> plusDays(long days) {
        return with(date.plus(days, ChronoUnit.DAYS), time);
    }
    
    // 加上hours小时
    private ChronoLocalDateTimeImpl<D> plusHours(long hours) {
        return plusWithOverflow(date, hours, 0, 0, 0);
    }
    
    // 加上minutes分钟
    private ChronoLocalDateTimeImpl<D> plusMinutes(long minutes) {
        return plusWithOverflow(date, 0, minutes, 0, 0);
    }
    
    // 加上seconds秒
    ChronoLocalDateTimeImpl<D> plusSeconds(long seconds) {
        return plusWithOverflow(date, 0, 0, seconds, 0);
    }
    
    // 加上nanos纳秒
    private ChronoLocalDateTimeImpl<D> plusNanos(long nanos) {
        return plusWithOverflow(date, 0, 0, 0, nanos);
    }
    
    /*
     * 将newDate和当前时间量的"本地时间"部件捆绑为一个时间点，
     * 然后在该时间点上累加hours小时minutes分钟seconds秒nanos纳秒，以便构造新的时间点。
     * 这里的hours/minutes/seconds/nanos理论上可以使用任意数值。
     */
    private ChronoLocalDateTimeImpl<D> plusWithOverflow(D newDate, long hours, long minutes, long seconds, long nanos) {
        // 9223372036854775808 long, 2147483648 int
        if((hours | minutes | seconds | nanos) == 0) {
            return with(newDate, time);
        }
        
        // 获取"时间"单位中包含的天数
        long totDays = nanos / NANOS_PER_DAY +   // max/24*60*60*1000_000_000
            seconds / SECONDS_PER_DAY + // max/24*60*60
            minutes / MINUTES_PER_DAY + // max/24*60
            hours / HOURS_PER_DAY;      // max/24
        
        // "时间"单位中剩余的纳秒
        long totNanos = nanos % NANOS_PER_DAY +              // max 86400000000000
            (seconds % SECONDS_PER_DAY) * NANOS_PER_SECOND + // max 86400000000000
            (minutes % MINUTES_PER_DAY) * NANOS_PER_MINUTE + // max 86400000000000
            (hours % HOURS_PER_DAY) * NANOS_PER_HOUR;        // max 86400000000000
        
        // 计算"本地时间"time包含的纳秒数
        long curNoD = time.toNanoOfDay();                          // max 86400000000000
        
        // 计算所有纳秒的和
        totNanos = totNanos + curNoD;                              // total 432000000000000
        
        // 计算所有的天数
        totDays += Math.floorDiv(totNanos, NANOS_PER_DAY);
        
        // 计算残留的纳秒数
        long newNoD = Math.floorMod(totNanos, NANOS_PER_DAY);
        
        // 构造新的"本地时间"
        LocalTime newTime = newNoD == curNoD ? time // 重用time部件
            : LocalTime.ofNanoOfDay(newNoD); // 使用指定的纳秒数(不超过一天)构造"本地时间"
        
        // 在newDate的值上累加daysToAdd天，构造新的"本地日期"
        ChronoLocalDate plusDays = newDate.plus(totDays, ChronoUnit.DAYS);
        
        return with(plusDays, newTime);
    }
    
    /**
     * Casts the {@code Temporal} to {@code ChronoLocalDateTime} ensuring it bas the specified chronology.
     *
     * @param chrono   the chronology to check for, not null
     * @param temporal a date-time to cast, not null
     *
     * @return the date-time checked and cast to {@code ChronoLocalDateTime}, not null
     *
     * @throws ClassCastException if the date-time cannot be cast to ChronoLocalDateTimeImpl
     *                            or the chronology is not equal this Chronology
     */
    static <R extends ChronoLocalDate> ChronoLocalDateTimeImpl<R> ensureValid(Chronology chrono, Temporal temporal) {
        @SuppressWarnings("unchecked")
        ChronoLocalDateTimeImpl<R> other = (ChronoLocalDateTimeImpl<R>) temporal;
        if(!chrono.equals(other.getChronology())) {
            throw new ClassCastException("Chronology mismatch, required: " + chrono.getId() + ", actual: " + other.getChronology().getId());
        }
        
        return other;
    }
    
    
    @Override
    public String toString() {
        return toLocalDate().toString() + 'T' + toLocalTime().toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj instanceof ChronoLocalDateTime) {
            return compareTo((ChronoLocalDateTime<?>) obj) == 0;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return toLocalDate().hashCode() ^ toLocalTime().hashCode();
    }
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 4556003607393004514L;
    
    /**
     * Writes the ChronoLocalDateTime using a
     * <a href="../../../serialized-form.html#java.time.chrono.Ser">dedicated serialized form</a>.
     *
     * @return the instance of {@code Ser}, not null
     *
     * @serialData <pre>
     *  out.writeByte(2);              // identifies a ChronoLocalDateTime
     *  out.writeObject(toLocalDate());
     *  out.witeObject(toLocalTime());
     * </pre>
     */
    private Object writeReplace() {
        return new Ser(Ser.CHRONO_LOCAL_DATE_TIME_TYPE, this);
    }
    
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
    
    static ChronoLocalDateTime<?> readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ChronoLocalDate date = (ChronoLocalDate) in.readObject();
        LocalTime time = (LocalTime) in.readObject();
        return date.atTime(time);
    }
    
    void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(date);
        out.writeObject(time);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
