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
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.time.zone.ZoneRules;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.time.LocalTime.MINUTES_PER_HOUR;
import static java.time.LocalTime.SECONDS_PER_HOUR;
import static java.time.LocalTime.SECONDS_PER_MINUTE;

/**
 * A time-zone offset from Greenwich/UTC, such as {@code +02:00}.
 * <p>
 * A time-zone offset is the amount of time that a time-zone differs from Greenwich/UTC.
 * This is usually a fixed number of hours and minutes.
 * <p>
 * Different parts of the world have different time-zone offsets.
 * The rules for how offsets vary by place and time of year are captured in the
 * {@link ZoneId} class.
 * <p>
 * For example, Paris is one hour ahead of Greenwich/UTC in winter and two hours
 * ahead in summer. The {@code ZoneId} instance for Paris will reference two
 * {@code ZoneOffset} instances - a {@code +01:00} instance for winter,
 * and a {@code +02:00} instance for summer.
 * <p>
 * In 2008, time-zone offsets around the world extended from -12:00 to +14:00.
 * To prevent any problems with that range being extended, yet still provide
 * validation, the range of offsets is restricted to -18:00 to 18:00 inclusive.
 * <p>
 * This class is designed for use with the ISO calendar system.
 * The fields of hours, minutes and seconds make assumptions that are valid for the
 * standard ISO definitions of those fields. This class may be used with other
 * calendar systems providing the definition of the time fields matches those
 * of the ISO calendar system.
 * <p>
 * Instances of {@code ZoneOffset} must be compared using {@link #equals}.
 * Implementations may choose to cache certain common offsets, however
 * applications must not rely on such caching.
 *
 * <p>
 * This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; use of identity-sensitive operations (including reference equality
 * ({@code ==}), identity hash code, or synchronization) on instances of
 * {@code ZoneOffset} may have unpredictable results and should be avoided.
 * The {@code equals} method should be used for comparisons.
 *
 * @implSpec This class is immutable and thread-safe.
 * @since 1.8
 */
// 基于时间偏移的时区ID，只反映与UTC时区的一个时差
public final class ZoneOffset extends ZoneId implements TemporalAccessor, TemporalAdjuster, Comparable<ZoneOffset>, Serializable {
    
    /** Cache of time-zone offset by offset in seconds. */
    private static final ConcurrentMap<Integer, ZoneOffset> SECONDS_CACHE = new ConcurrentHashMap<>(16, 0.75f, 4);
    /** Cache of time-zone offset by ID. */
    private static final ConcurrentMap<String, ZoneOffset> ID_CACHE = new ConcurrentHashMap<>(16, 0.75f, 4);
    
    /**
     * The abs maximum seconds.
     */
    private static final int MAX_SECONDS = 18 * SECONDS_PER_HOUR;
    
    /**
     * The time-zone offset for UTC, with an ID of 'Z'.
     */
    public static final ZoneOffset UTC = ZoneOffset.ofTotalSeconds(0);           // UTC时区，时间偏移秒数为0
    /**
     * Constant for the minimum supported offset.
     */
    public static final ZoneOffset MIN = ZoneOffset.ofTotalSeconds(-MAX_SECONDS); // 最小的时区偏移：-18
    /**
     * Constant for the maximum supported offset.
     */
    public static final ZoneOffset MAX = ZoneOffset.ofTotalSeconds(MAX_SECONDS);  // 最大的时区偏移：+18
    
    /**
     * The string form of the time-zone offset.
     */
    /*
     * 时区ID的字符串表示
     *
     * 此处的ID是基于一个偏移时间的，比如：
     * Z
     * +08:00
     * -07:05:12
     */
    private final transient String id;
    
    /**
     * The total offset in seconds.
     */
    /*
     * 时区偏移的秒数。
     *
     * 正数表示在东区，负数表示在西区，0表示在UTC/GMT时区。
     * 比如28800表示在东八区，比UTC/GMT时区快8个小时。
     */
    private final int totalSeconds;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructor.
     *
     * @param totalSeconds the total time-zone offset in seconds, from -64800 to +64800
     */
    private ZoneOffset(int totalSeconds) {
        super();
        this.totalSeconds = totalSeconds;
        id = buildId(totalSeconds);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains an instance of {@code ZoneOffset} using the ID.
     * <p>
     * This method parses the string ID of a {@code ZoneOffset} to
     * return an instance. The parsing accepts all the formats generated by
     * {@link #getId()}, plus some additional formats:
     * <ul>
     * <li>{@code Z} - for UTC
     * <li>{@code +h}
     * <li>{@code +hh}
     * <li>{@code +hh:mm}
     * <li>{@code -hh:mm}
     * <li>{@code +hhmm}
     * <li>{@code -hhmm}
     * <li>{@code +hh:mm:ss}
     * <li>{@code -hh:mm:ss}
     * <li>{@code +hhmmss}
     * <li>{@code -hhmmss}
     * </ul>
     * Note that &plusmn; means either the plus or minus symbol.
     * <p>
     * The ID of the returned offset will be normalized to one of the formats
     * described by {@link #getId()}.
     * <p>
     * The maximum supported range is from +18:00 to -18:00 inclusive.
     *
     * @param offsetId the offset ID, not null
     *
     * @return the zone-offset, not null
     *
     * @throws DateTimeException if the offset ID is invalid
     */
    /*
     * 根据给定的时间偏移构造基于时间偏移的时区ID
     *
     * offsetId: 代表时间偏移的字符串，包括以下形式：
     *
     * Z（表示在UTC时区）
     * +h
     * +hh
     * +hh:mm
     * -hh:mm
     * +hhmm
     * -hhmm
     * +hh:mm:ss
     * -hh:mm:ss
     * +hhmmss
     * -hhmmss
     *
     * 注：时间偏移的范围为[-18:00, +18:00]
     */
    @SuppressWarnings("fallthrough")
    public static ZoneOffset of(String offsetId) {
        Objects.requireNonNull(offsetId, "offsetId");
        
        // "Z" is always in the cache
        ZoneOffset offset = ID_CACHE.get(offsetId);
        if(offset != null) {
            return offset;
        }
        
        // parse - +h, +hh, +hhmm, +hh:mm, +hhmmss, +hh:mm:ss
        final int hours, minutes, seconds;
        switch(offsetId.length()) {
            case 2:
                offsetId = offsetId.charAt(0) + "0" + offsetId.charAt(1);  // fallthru
            case 3:
                hours = parseNumber(offsetId, 1, false);
                minutes = 0;
                seconds = 0;
                break;
            case 5:
                hours = parseNumber(offsetId, 1, false);
                minutes = parseNumber(offsetId, 3, false);
                seconds = 0;
                break;
            case 6:
                hours = parseNumber(offsetId, 1, false);
                minutes = parseNumber(offsetId, 4, true);
                seconds = 0;
                break;
            case 7:
                hours = parseNumber(offsetId, 1, false);
                minutes = parseNumber(offsetId, 3, false);
                seconds = parseNumber(offsetId, 5, false);
                break;
            case 9:
                hours = parseNumber(offsetId, 1, false);
                minutes = parseNumber(offsetId, 4, true);
                seconds = parseNumber(offsetId, 7, true);
                break;
            default:
                throw new DateTimeException("Invalid ID for ZoneOffset, invalid format: " + offsetId);
        }
        
        char first = offsetId.charAt(0);
        if(first != '+' && first != '-') {
            throw new DateTimeException("Invalid ID for ZoneOffset, plus/minus not found when expected: " + offsetId);
        }
        
        if(first == '-') {
            return ofHoursMinutesSeconds(-hours, -minutes, -seconds);
        } else {
            return ofHoursMinutesSeconds(hours, minutes, seconds);
        }
    }
    
    /**
     * Obtains an instance of {@code ZoneOffset} using an offset in hours.
     *
     * @param hours the time-zone offset in hours, from -18 to +18
     *
     * @return the zone-offset, not null
     *
     * @throws DateTimeException if the offset is not in the required range
     */
    // 构造基于时间偏移的时区ID，其时间偏移为hours小时
    public static ZoneOffset ofHours(int hours) {
        return ofHoursMinutesSeconds(hours, 0, 0);
    }
    
    /**
     * Obtains an instance of {@code ZoneOffset} using an offset in
     * hours and minutes.
     * <p>
     * The sign of the hours and minutes components must match.
     * Thus, if the hours is negative, the minutes must be negative or zero.
     * If the hours is zero, the minutes may be positive, negative or zero.
     *
     * @param hours   the time-zone offset in hours, from -18 to +18
     * @param minutes the time-zone offset in minutes, from 0 to &plusmn;59, sign matches hours
     *
     * @return the zone-offset, not null
     *
     * @throws DateTimeException if the offset is not in the required range
     */
    // 构造基于时间偏移的时区ID，其时间偏移为hours小时+minutes分
    public static ZoneOffset ofHoursMinutes(int hours, int minutes) {
        return ofHoursMinutesSeconds(hours, minutes, 0);
    }
    
    /**
     * Obtains an instance of {@code ZoneOffset} using an offset in
     * hours, minutes and seconds.
     * <p>
     * The sign of the hours, minutes and seconds components must match.
     * Thus, if the hours is negative, the minutes and seconds must be negative or zero.
     *
     * @param hours   the time-zone offset in hours, from -18 to +18
     * @param minutes the time-zone offset in minutes, from 0 to &plusmn;59, sign matches hours and seconds
     * @param seconds the time-zone offset in seconds, from 0 to &plusmn;59, sign matches hours and minutes
     *
     * @return the zone-offset, not null
     *
     * @throws DateTimeException if the offset is not in the required range
     */
    // 构造基于时间偏移的时区ID，其时间偏移为hours小时+minutes分+seconds秒
    public static ZoneOffset ofHoursMinutesSeconds(int hours, int minutes, int seconds) {
        validate(hours, minutes, seconds);
    
        int totalSeconds = totalSeconds(hours, minutes, seconds);
    
        return ofTotalSeconds(totalSeconds);
    }
    
    /**
     * Obtains an instance of {@code ZoneOffset} specifying the total offset in seconds
     * <p>
     * The offset must be in the range {@code -18:00} to {@code +18:00}, which corresponds to -64800 to +64800.
     *
     * @param totalSeconds the total time-zone offset in seconds, from -64800 to +64800
     *
     * @return the ZoneOffset, not null
     *
     * @throws DateTimeException if the offset is not in the required range
     */
    // 构造基于时间偏移的时区ID，其时间偏移为totalSeconds秒
    public static ZoneOffset ofTotalSeconds(int totalSeconds) {
        if(totalSeconds<-MAX_SECONDS || totalSeconds>MAX_SECONDS) {
            throw new DateTimeException("Zone offset not in valid range: -18:00 to +18:00");
        }
    
        // 如果totalSeconds是15分的倍数，则做缓存
        if(totalSeconds % (15 * SECONDS_PER_MINUTE) == 0) {
            Integer totalSecs = totalSeconds;
            ZoneOffset result = SECONDS_CACHE.get(totalSecs);
            if(result == null) {
                result = new ZoneOffset(totalSeconds);
                SECONDS_CACHE.putIfAbsent(totalSecs, result);
                result = SECONDS_CACHE.get(totalSecs);
                ID_CACHE.putIfAbsent(result.getId(), result);
            }
            return result;
        } else {
            return new ZoneOffset(totalSeconds);
        }
    }
    
    /**
     * Obtains an instance of {@code ZoneOffset} from a temporal object.
     * <p>
     * This obtains an offset based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code ZoneOffset}.
     * <p>
     * A {@code TemporalAccessor} represents some form of date and time information.
     * This factory converts the arbitrary temporal object to an instance of {@code ZoneOffset}.
     * <p>
     * The conversion uses the {@link TemporalQueries#offset()} query, which relies
     * on extracting the {@link ChronoField#OFFSET_SECONDS OFFSET_SECONDS} field.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code ZoneOffset::from}.
     *
     * @param temporal the temporal object to convert, not null
     *
     * @return the zone-offset, not null
     *
     * @throws DateTimeException if unable to convert to an {@code ZoneOffset}
     */
    /*
     * 从指定的时间量中查询出基于时间偏移的时区ID
     *
     * 注：要求该时间量支持ChronoField.OFFSET_SECONDS字段
     */
    public static ZoneOffset from(TemporalAccessor temporal) {
        Objects.requireNonNull(temporal, "temporal");
        
        // 获取特定的时间量查询器
        TemporalQuery<ZoneOffset> query = TemporalQueries.offset();
        
        // 使用指定的时间量查询器，从当前时间量中查询目标信息
        ZoneOffset offset = temporal.query(query);
        if(offset == null) {
            throw new DateTimeException("Unable to obtain ZoneOffset from TemporalAccessor: " + temporal + " of type " + temporal.getClass().getName());
        }
        
        return offset;
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 部件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the normalized zone offset ID.
     * <p>
     * The ID is minor variation to the standard ISO-8601 formatted string
     * for the offset. There are three formats:
     * <ul>
     * <li>{@code Z} - for UTC (ISO-8601)
     * <li>{@code +hh:mm} or {@code -hh:mm} - if the seconds are zero (ISO-8601)
     * <li>{@code +hh:mm:ss} or {@code -hh:mm:ss} - if the seconds are non-zero (not ISO-8601)
     * </ul>
     *
     * @return the zone offset ID, not null
     */
    // 返回时区ID的字符串表示
    @Override
    public String getId() {
        return id;
    }
    
    /**
     * Gets the total zone offset in seconds.
     * <p>
     * This is the primary way to access the offset amount.
     * It returns the total of the hours, minutes and seconds fields as a
     * single offset that can be added to a time.
     *
     * @return the total zone offset amount in seconds
     */
    // 返回当前时区的偏移秒数
    public int getTotalSeconds() {
        return totalSeconds;
    }
    
    /*▲ 部件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量字段操作(TemporalAccessor) ███████████████████████████████████████████████████████┓ */
    
    /**
     * Checks if the specified field is supported.
     * <p>
     * This checks if this offset can be queried for the specified field.
     * If false, then calling the {@link #range(TemporalField) range} and
     * {@link #get(TemporalField) get} methods will throw an exception.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@code OFFSET_SECONDS} field returns true.
     * All other {@code ChronoField} instances will return false.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.isSupportedBy(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the field is supported is determined by the field.
     *
     * @param field the field to check, null returns false
     *
     * @return true if the field is supported on this offset, false if not
     */
    // 判断当前时间量是否支持指定的时间量字段
    @Override
    public boolean isSupported(TemporalField field) {
        if(field instanceof ChronoField) {
            return field == ChronoField.OFFSET_SECONDS;
        }
        return field != null && field.isSupportedBy(this);
    }
    
    /**
     * Gets the range of valid values for the specified field.
     * <p>
     * The range object expresses the minimum and maximum valid values for a field.
     * This offset is used to enhance the accuracy of the returned range.
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
    // 返回时间量字段field的有效范围，通常要求当前时间量支持该时间量字段
    @Override
    public ValueRange range(TemporalField field) {
        return TemporalAccessor.super.range(field);
    }
    
    /**
     * Gets the value of the specified field from this offset as an {@code int}.
     * <p>
     * This queries this offset for the value of the specified field.
     * The returned value will always be within the valid range of values for the field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@code OFFSET_SECONDS} field returns the value of the offset.
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
     * ChronoField.OFFSET_SECONDS - 时区偏移的秒数
     */
    @Override
    public int get(TemporalField field) {
        if(field instanceof ChronoField) {
            if(field == ChronoField.OFFSET_SECONDS) {
                return totalSeconds;
            }
            
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        
        return range(field).checkValidIntValue(getLong(field), field);
    }
    
    /**
     * Gets the value of the specified field from this offset as a {@code long}.
     * <p>
     * This queries this offset for the value of the specified field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@code OFFSET_SECONDS} field returns the value of the offset.
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
     * ChronoField.OFFSET_SECONDS - 时区偏移的秒数
     */
    @Override
    public long getLong(TemporalField field) {
        if(field instanceof ChronoField) {
            if(field == ChronoField.OFFSET_SECONDS) {
                return totalSeconds;
            }
            
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        
        return field.getFrom(this);
    }
    
    /**
     * Queries this offset using the specified query.
     * <p>
     * This queries this offset using the specified query strategy object.
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
        if(query == TemporalQueries.offset() || query == TemporalQueries.zone()) {
            return (R) this;
        }
        
        return TemporalAccessor.super.query(query);
    }
    
    /*▲ 时间量字段操作(TemporalAccessor) ███████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 整合 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Adjusts the specified temporal object to have the same offset as this object.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with the offset changed to be the same as this.
     * <p>
     * The adjustment is equivalent to using {@link Temporal#with(TemporalField, long)}
     * passing {@link ChronoField#OFFSET_SECONDS} as the field.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#with(TemporalAdjuster)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisOffset.adjustInto(temporal);
     *   temporal = temporal.with(thisOffset);
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
     * ChronoField.OFFSET_SECONDS - 当前时间量的时区偏移秒数
     *
     * 目标时间量temporal的取值可以是：
     * OffsetTime
     * OffsetDateTime
     * ZonedDateTime
     * ChronoZonedDateTimeImpl
     */
    @Override
    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.OFFSET_SECONDS, totalSeconds);
    }
    
    /*▲ 整合 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the associated time-zone rules.
     * <p>
     * The rules will always return this offset when queried.
     * The implementation class is immutable, thread-safe and serializable.
     *
     * @return the rules, not null
     */
    // 返回与当前时区ID对应的"时区规则集"
    @Override
    public ZoneRules getRules() {
        return ZoneRules.of(this);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Parse a two digit zero-prefixed number.
     *
     * @param offsetId        the offset ID, not null
     * @param pos             the position to parse, valid
     * @param precededByColon should this number be prefixed by a precededByColon
     *
     * @return the parsed number, from 0 to 99
     */
    private static int parseNumber(CharSequence offsetId, int pos, boolean precededByColon) {
        if(precededByColon && offsetId.charAt(pos - 1) != ':') {
            throw new DateTimeException("Invalid ID for ZoneOffset, colon not found when expected: " + offsetId);
        }
        char ch1 = offsetId.charAt(pos);
        char ch2 = offsetId.charAt(pos + 1);
        if(ch1<'0' || ch1>'9' || ch2<'0' || ch2>'9') {
            throw new DateTimeException("Invalid ID for ZoneOffset, non numeric characters found: " + offsetId);
        }
        return (ch1 - 48) * 10 + (ch2 - 48);
    }
    
    /**
     * Validates the offset fields.
     *
     * @param hours   the time-zone offset in hours, from -18 to +18
     * @param minutes the time-zone offset in minutes, from 0 to &plusmn;59
     * @param seconds the time-zone offset in seconds, from 0 to &plusmn;59
     *
     * @throws DateTimeException if the offset is not in the required range
     */
    private static void validate(int hours, int minutes, int seconds) {
        if(hours<-18 || hours>18) {
            throw new DateTimeException("Zone offset hours not in valid range: value " + hours + " is not in the range -18 to 18");
        }
        if(hours>0) {
            if(minutes<0 || seconds<0) {
                throw new DateTimeException("Zone offset minutes and seconds must be positive because hours is positive");
            }
        } else if(hours<0) {
            if(minutes>0 || seconds>0) {
                throw new DateTimeException("Zone offset minutes and seconds must be negative because hours is negative");
            }
        } else if((minutes>0 && seconds<0) || (minutes<0 && seconds>0)) {
            throw new DateTimeException("Zone offset minutes and seconds must have the same sign");
        }
        if(minutes<-59 || minutes>59) {
            throw new DateTimeException("Zone offset minutes not in valid range: value " + minutes + " is not in the range -59 to 59");
        }
        if(seconds<-59 || seconds>59) {
            throw new DateTimeException("Zone offset seconds not in valid range: value " + seconds + " is not in the range -59 to 59");
        }
        if(Math.abs(hours) == 18 && (minutes | seconds) != 0) {
            throw new DateTimeException("Zone offset not in valid range: -18:00 to +18:00");
        }
    }
    
    /**
     * Calculates the total offset in seconds.
     *
     * @param hours   the time-zone offset in hours, from -18 to +18
     * @param minutes the time-zone offset in minutes, from 0 to &plusmn;59, sign matches hours and seconds
     * @param seconds the time-zone offset in seconds, from 0 to &plusmn;59, sign matches hours and minutes
     *
     * @return the total in seconds
     */
    private static int totalSeconds(int hours, int minutes, int seconds) {
        return hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds;
    }
    
    // 根据偏移秒数构造时区ID
    private static String buildId(int totalSeconds) {
        if(totalSeconds == 0) {
            return "Z";
        }
        
        int absTotalSeconds = Math.abs(totalSeconds);
        StringBuilder buf = new StringBuilder();
        int absHours = absTotalSeconds / SECONDS_PER_HOUR;
        int absMinutes = (absTotalSeconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR;
        buf.append(totalSeconds<0 ? "-" : "+").append(absHours<10 ? "0" : "").append(absHours).append(absMinutes<10 ? ":0" : ":").append(absMinutes);
        int absSeconds = absTotalSeconds % SECONDS_PER_MINUTE;
        if(absSeconds != 0) {
            buf.append(absSeconds<10 ? ":0" : ":").append(absSeconds);
        }
        
        return buf.toString();
    }
    
    
    /**
     * Compares this offset to another offset in descending order.
     * <p>
     * The offsets are compared in the order that they occur for the same time
     * of day around the world. Thus, an offset of {@code +10:00} comes before an
     * offset of {@code +09:00} and so on down to {@code -18:00}.
     * <p>
     * The comparison is "consistent with equals", as defined by {@link Comparable}.
     *
     * @param other the other date to compare to, not null
     *
     * @return the comparator value, negative if less, positive if greater
     *
     * @throws NullPointerException if {@code other} is null
     */
    @Override
    public int compareTo(ZoneOffset other) {
        // abs(totalSeconds) <= MAX_SECONDS, so no overflow can happen here
        return other.totalSeconds - totalSeconds;
    }
    
    /**
     * Outputs this offset as a {@code String}, using the normalized ID.
     *
     * @return a string representation of this offset, not null
     */
    @Override
    public String toString() {
        return id;
    }
    
    /**
     * Checks if this offset is equal to another offset.
     * <p>
     * The comparison is based on the amount of the offset in seconds.
     * This is equivalent to a comparison by ID.
     *
     * @param obj the object to check, null returns false
     *
     * @return true if this is equal to the other offset
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj instanceof ZoneOffset) {
            return totalSeconds == ((ZoneOffset) obj).totalSeconds;
        }
        return false;
    }
    
    /**
     * A hash code for this offset.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return totalSeconds;
    }
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 2357656521762053153L;
    
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
    
    @Override
    void write(DataOutput out) throws IOException {
        out.writeByte(Ser.ZONE_OFFSET_TYPE);
        writeExternal(out);
    }
    
    void writeExternal(DataOutput out) throws IOException {
        final int offsetSecs = totalSeconds;
        int offsetByte = offsetSecs % 900 == 0 ? offsetSecs / 900 : 127;  // compress to -72 to +72
        out.writeByte(offsetByte);
        if(offsetByte == 127) {
            out.writeInt(offsetSecs);
        }
    }
    
    static ZoneOffset readExternal(DataInput in) throws IOException {
        int offsetByte = in.readByte();
        return (offsetByte == 127 ? ZoneOffset.ofTotalSeconds(in.readInt()) : ZoneOffset.ofTotalSeconds(offsetByte * 900));
    }
    
    /**
     * Writes the object using a
     * <a href="../../serialized-form.html#java.time.Ser">dedicated serialized form</a>.
     *
     * @return the instance of {@code Ser}, not null
     *
     * @serialData <pre>
     *  out.writeByte(8);                  // identifies a ZoneOffset
     *  int offsetByte = totalSeconds % 900 == 0 ? totalSeconds / 900 : 127;
     *  out.writeByte(offsetByte);
     *  if (offsetByte == 127) {
     *      out.writeInt(totalSeconds);
     *  }
     * </pre>
     */
    private Object writeReplace() {
        return new Ser(Ser.ZONE_OFFSET_TYPE, this);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
