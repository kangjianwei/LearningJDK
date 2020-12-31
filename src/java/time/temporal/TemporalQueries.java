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
 *
 *
 *
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
package java.time.temporal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;

/**
 * Common implementations of {@code TemporalQuery}.
 * <p>
 * This class provides common implementations of {@link TemporalQuery}.
 * These are defined here as they must be constants, and the definition
 * of lambdas does not guarantee that. By assigning them once here,
 * they become 'normal' Java constants.
 * <p>
 * Queries are a key tool for extracting information from temporal objects.
 * They exist to externalize the process of querying, permitting different
 * approaches, as per the strategy design pattern.
 * Examples might be a query that checks if the date is the day before February 29th
 * in a leap year, or calculates the number of days to your next birthday.
 * <p>
 * The {@link TemporalField} interface provides another mechanism for querying
 * temporal objects. That interface is limited to returning a {@code long}.
 * By contrast, queries can return any type.
 * <p>
 * There are two equivalent ways of using a {@code TemporalQuery}.
 * The first is to invoke the method on this interface directly.
 * The second is to use {@link TemporalAccessor#query(TemporalQuery)}:
 * <pre>
 *   // these two lines are equivalent, but the second approach is recommended
 *   temporal = thisQuery.queryFrom(temporal);
 *   temporal = temporal.query(thisQuery);
 * </pre>
 * It is recommended to use the second approach, {@code query(TemporalQuery)},
 * as it is a lot clearer to read in code.
 * <p>
 * The most common implementations are method references, such as
 * {@code LocalDate::from} and {@code ZoneId::from}.
 * Additional common queries are provided to return:
 * <ul>
 * <li> a Chronology,
 * <li> a LocalDate,
 * <li> a LocalTime,
 * <li> a ZoneOffset,
 * <li> a precision,
 * <li> a zone, or
 * <li> a zoneId.
 * </ul>
 *
 * @since 1.8
 */
// 时间量查询器工厂，内部预设了多种实用的查询器
public final class TemporalQueries {
    /*
     * note that it is vital that each method supplies a constant, not a
     * calculated value, as they will be checked for using ==
     * it is also vital that each constant is different (due to the == checking)
     * as such, alterations to this code must be done with care
     */
    
    /**
     * Private constructor since this is a utility class.
     */
    private TemporalQueries() {
    }
    
    
    /*
     * special constants should be used to extract information from a TemporalAccessor that cannot be derived in other ways
     * Javadoc added here, so as to pretend they are more normal than they really are
     */
    
    /**
     * A query for the smallest supported unit.
     * <p>
     * This queries a {@code TemporalAccessor} for the time precision.
     * If the target {@code TemporalAccessor} represents a consistent or complete date-time,
     * date or time then this must return the smallest precision actually supported.
     * Note that fields such as {@code NANO_OF_DAY} and {@code NANO_OF_SECOND}
     * are defined to always return ignoring the precision, thus this is the only
     * way to find the actual smallest supported unit.
     * For example, were {@code GregorianCalendar} to implement {@code TemporalAccessor}
     * it would return a precision of {@code MILLIS}.
     * <p>
     * The result from JDK classes implementing {@code TemporalAccessor} is as follows:<br>
     * {@code LocalDate} returns {@code DAYS}<br>
     * {@code LocalTime} returns {@code NANOS}<br>
     * {@code LocalDateTime} returns {@code NANOS}<br>
     * {@code ZonedDateTime} returns {@code NANOS}<br>
     * {@code OffsetTime} returns {@code NANOS}<br>
     * {@code OffsetDateTime} returns {@code NANOS}<br>
     * {@code ChronoLocalDate} returns {@code DAYS}<br>
     * {@code ChronoLocalDateTime} returns {@code NANOS}<br>
     * {@code ChronoZonedDateTime} returns {@code NANOS}<br>
     * {@code Era} returns {@code ERAS}<br>
     * {@code DayOfWeek} returns {@code DAYS}<br>
     * {@code Month} returns {@code MONTHS}<br>
     * {@code Year} returns {@code YEARS}<br>
     * {@code YearMonth} returns {@code MONTHS}<br>
     * {@code MonthDay} returns null (does not represent a complete date or time)<br>
     * {@code ZoneOffset} returns null (does not represent a date or time)<br>
     * {@code Instant} returns {@code NANOS}<br>
     *
     * @return a query that can obtain the precision of a temporal, not null
     */
    // 查询时间量支持的最小时间量单位
    public static TemporalQuery<TemporalUnit> precision() {
        return TemporalQueries.PRECISION;
    }
    
    /**
     * A query for the {@code Chronology}.
     * <p>
     * This queries a {@code TemporalAccessor} for the chronology.
     * If the target {@code TemporalAccessor} represents a date, or part of a date,
     * then it should return the chronology that the date is expressed in.
     * As a result of this definition, objects only representing time, such as
     * {@code LocalTime}, will return null.
     * <p>
     * The result from JDK classes implementing {@code TemporalAccessor} is as follows:<br>
     * {@code LocalDate} returns {@code IsoChronology.INSTANCE}<br>
     * {@code LocalTime} returns null (does not represent a date)<br>
     * {@code LocalDateTime} returns {@code IsoChronology.INSTANCE}<br>
     * {@code ZonedDateTime} returns {@code IsoChronology.INSTANCE}<br>
     * {@code OffsetTime} returns null (does not represent a date)<br>
     * {@code OffsetDateTime} returns {@code IsoChronology.INSTANCE}<br>
     * {@code ChronoLocalDate} returns the associated chronology<br>
     * {@code ChronoLocalDateTime} returns the associated chronology<br>
     * {@code ChronoZonedDateTime} returns the associated chronology<br>
     * {@code Era} returns the associated chronology<br>
     * {@code DayOfWeek} returns null (shared across chronologies)<br>
     * {@code Month} returns {@code IsoChronology.INSTANCE}<br>
     * {@code Year} returns {@code IsoChronology.INSTANCE}<br>
     * {@code YearMonth} returns {@code IsoChronology.INSTANCE}<br>
     * {@code MonthDay} returns null {@code IsoChronology.INSTANCE}<br>
     * {@code ZoneOffset} returns null (does not represent a date)<br>
     * {@code Instant} returns null (does not represent a date)<br>
     * <p>
     * The method {@link java.time.chrono.Chronology#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code Chronology::from}.
     * That method is equivalent to this query, except that it throws an
     * exception if a chronology cannot be obtained.
     *
     * @return a query that can obtain the chronology of a temporal, not null
     */
    // 查询时间量的历法系统
    public static TemporalQuery<Chronology> chronology() {
        return TemporalQueries.CHRONO;
    }
    
    /**
     * A strict query for the {@code ZoneId}.
     * <p>
     * This queries a {@code TemporalAccessor} for the zone.
     * The zone is only returned if the date-time conceptually contains a {@code ZoneId}.
     * It will not be returned if the date-time only conceptually has an {@code ZoneOffset}.
     * Thus a {@link java.time.ZonedDateTime} will return the result of {@code getZone()},
     * but an {@link java.time.OffsetDateTime} will return null.
     * <p>
     * In most cases, applications should use {@link #zone()} as this query is too strict.
     * <p>
     * The result from JDK classes implementing {@code TemporalAccessor} is as follows:<br>
     * {@code LocalDate} returns null<br>
     * {@code LocalTime} returns null<br>
     * {@code LocalDateTime} returns null<br>
     * {@code ZonedDateTime} returns the associated zone<br>
     * {@code OffsetTime} returns null<br>
     * {@code OffsetDateTime} returns null<br>
     * {@code ChronoLocalDate} returns null<br>
     * {@code ChronoLocalDateTime} returns null<br>
     * {@code ChronoZonedDateTime} returns the associated zone<br>
     * {@code Era} returns null<br>
     * {@code DayOfWeek} returns null<br>
     * {@code Month} returns null<br>
     * {@code Year} returns null<br>
     * {@code YearMonth} returns null<br>
     * {@code MonthDay} returns null<br>
     * {@code ZoneOffset} returns null<br>
     * {@code Instant} returns null<br>
     *
     * @return a query that can obtain the zone ID of a temporal, not null
     */
    // 查询ZoneId部件的信息(严格模式，通常需要在时间量中直接查找到ZoneId属性)
    public static TemporalQuery<ZoneId> zoneId() {
        return TemporalQueries.ZONE_ID;
    }
    
    /**
     * A lenient query for the {@code ZoneId}, falling back to the {@code ZoneOffset}.
     * <p>
     * This queries a {@code TemporalAccessor} for the zone.
     * It first tries to obtain the zone, using {@link #zoneId()}.
     * If that is not found it tries to obtain the {@link #offset()}.
     * Thus a {@link java.time.ZonedDateTime} will return the result of {@code getZone()},
     * while an {@link java.time.OffsetDateTime} will return the result of {@code getOffset()}.
     * <p>
     * In most cases, applications should use this query rather than {@code #zoneId()}.
     * <p>
     * The method {@link ZoneId#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code ZoneId::from}.
     * That method is equivalent to this query, except that it throws an
     * exception if a zone cannot be obtained.
     *
     * @return a query that can obtain the zone ID or offset of a temporal, not null
     */
    // 查询ZoneId部件的信息(宽松模式，如果在时间量中无法直接查找到ZoneId属性，则回退为查找ZoneOffset属性)
    public static TemporalQuery<ZoneId> zone() {
        return TemporalQueries.ZONE;
    }
    
    /**
     * A query for {@code ZoneOffset} returning null if not found.
     * <p>
     * This returns a {@code TemporalQuery} that can be used to query a temporal
     * object for the offset. The query will return null if the temporal
     * object cannot supply an offset.
     * <p>
     * The query implementation examines the {@link ChronoField#OFFSET_SECONDS OFFSET_SECONDS}
     * field and uses it to create a {@code ZoneOffset}.
     * <p>
     * The method {@link java.time.ZoneOffset#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code ZoneOffset::from}.
     * This query and {@code ZoneOffset::from} will return the same result if the
     * temporal object contains an offset. If the temporal object does not contain
     * an offset, then the method reference will throw an exception, whereas this
     * query will return null.
     *
     * @return a query that can obtain the offset of a temporal, not null
     */
    /*
     * 查询ZoneOffset部件的信息
     *
     * 如果没有现成的部件，通常需要从指定的时间量中解析出时区偏移的秒数，
     * 然后使用时区偏移的秒数构造ZoneOffset后返回。
     */
    public static TemporalQuery<ZoneOffset> offset() {
        return TemporalQueries.OFFSET;
    }

    /**
     * A query for {@code LocalDate} returning null if not found.
     * <p>
     * This returns a {@code TemporalQuery} that can be used to query a temporal
     * object for the local date. The query will return null if the temporal
     * object cannot supply a local date.
     * <p>
     * The query implementation examines the {@link ChronoField#EPOCH_DAY EPOCH_DAY}
     * field and uses it to create a {@code LocalDate}.
     * <p>
     * The method {@link ZoneOffset#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code LocalDate::from}.
     * This query and {@code LocalDate::from} will return the same result if the
     * temporal object contains a date. If the temporal object does not contain
     * a date, then the method reference will throw an exception, whereas this
     * query will return null.
     *
     * @return a query that can obtain the date of a temporal, not null
     */
    /*
     * 查询LocalDate部件的信息
     *
     * 如果没有现成的部件，通常需要从指定的时间量中解析出纪元天，
     * 然后使用纪元天构造LocalDate后返回。
     */
    public static TemporalQuery<LocalDate> localDate() {
        return TemporalQueries.LOCAL_DATE;
    }

    /**
     * A query for {@code LocalTime} returning null if not found.
     * <p>
     * This returns a {@code TemporalQuery} that can be used to query a temporal
     * object for the local time. The query will return null if the temporal
     * object cannot supply a local time.
     * <p>
     * The query implementation examines the {@link ChronoField#NANO_OF_DAY NANO_OF_DAY}
     * field and uses it to create a {@code LocalTime}.
     * <p>
     * The method {@link ZoneOffset#from(TemporalAccessor)} can be used as a
     * {@code TemporalQuery} via a method reference, {@code LocalTime::from}.
     * This query and {@code LocalTime::from} will return the same result if the
     * temporal object contains a time. If the temporal object does not contain
     * a time, then the method reference will throw an exception, whereas this
     * query will return null.
     *
     * @return a query that can obtain the time of a temporal, not null
     */
    /*
     * 查询LocalTime部件的信息
     *
     * 如果没有现成的部件，通常需要从指定的时间量中解析出包含的纳秒数，
     * 然后使用该时间量包含的纳秒数构造LocalTime后返回。
     */
    public static TemporalQuery<LocalTime> localTime() {
        return TemporalQueries.LOCAL_TIME;
    }
    
    
    /**
     * A query for the smallest supported unit.
     */
    // 查询时间量支持的最小时间量单位
    static final TemporalQuery<TemporalUnit> PRECISION = new TemporalQuery<>() {
        @Override
        public TemporalUnit queryFrom(TemporalAccessor temporal) {
            return temporal.query(TemporalQueries.PRECISION);
        }
        
        @Override
        public String toString() {
            return "Precision";
        }
    };
    
    /**
     * A query for the {@code Chronology}.
     */
    // 查询时间量的历法系统
    static final TemporalQuery<Chronology> CHRONO = new TemporalQuery<>() {
        @Override
        public Chronology queryFrom(TemporalAccessor temporal) {
            return temporal.query(TemporalQueries.CHRONO);
        }
        
        @Override
        public String toString() {
            return "Chronology";
        }
    };
    
    /**
     * A strict query for the {@code ZoneId}.
     */
    // 查询ZoneId部件的信息(严格模式，通常需要在时间量中直接查找到ZoneId属性)
    static final TemporalQuery<ZoneId> ZONE_ID = new TemporalQuery<>() {
        @Override
        public ZoneId queryFrom(TemporalAccessor temporal) {
            return temporal.query(TemporalQueries.ZONE_ID);
        }
        
        @Override
        public String toString() {
            return "ZoneId";
        }
    };
    
    /**
     * A lenient query for the {@code ZoneId}, falling back to the {@code ZoneOffset}.
     */
    /*
     * 查询ZoneId部件的信息(宽松模式，如果在时间量中无法直接查找到ZoneId属性，则回退为查找ZoneOffset属性)
     *
     * 首先尝试查找ZoneId部件的信息，如果找到，则直接返回。
     * 如果找不到ZoneId部件的信息，则尝试查找ZoneOffset部件的信息。
     * 如果没有现成的ZoneOffset部件，通常需要从指定的时间量中解析出时区偏移的秒数，
     * 然后使用时区偏移的秒数构造ZoneOffset后返回。
     */
    static final TemporalQuery<ZoneId> ZONE = new TemporalQuery<>() {
        @Override
        public ZoneId queryFrom(TemporalAccessor temporal) {
            // 查询ZoneId部件的信息(严格模式，通常需要在时间量中直接查找到ZoneId属性)
            ZoneId zone = temporal.query(TemporalQueries.ZONE_ID);
            // 如果已经找到ZoneId部件，则直接返回。
            if(zone != null) {
                return zone;
            }
            
            /*
             * 查询ZoneOffset部件的信息
             *
             * 如果没有现成的ZoneOffset部件，通常需要从指定的时间量中解析出时区偏移的秒数，
             * 然后使用时区偏移的秒数构造ZoneOffset后返回。
             */
            return temporal.query(TemporalQueries.OFFSET);
        }
        
        @Override
        public String toString() {
            return "Zone";
        }
    };
    
    /**
     * A query for {@code ZoneOffset} returning null if not found.
     */
    /*
     * 查询ZoneOffset部件的信息
     *
     * 如果没有现成的部件，通常需要从指定的时间量中解析出时区偏移的秒数，
     * 然后使用时区偏移的秒数构造ZoneOffset后返回。
     */
    static final TemporalQuery<ZoneOffset> OFFSET = new TemporalQuery<>() {
        @Override
        public ZoneOffset queryFrom(TemporalAccessor temporal) {
            // 支持ChronoField.OFFSET_SECONDS的时间量通常包含ZoneOffset属性
            if(temporal.isSupported(ChronoField.OFFSET_SECONDS)) {
                // 获取temporal的时区偏移秒数
                int offset = temporal.get(ChronoField.OFFSET_SECONDS);
                // 构造基于时间偏移的时区ID，其时间偏移为offset秒
                return ZoneOffset.ofTotalSeconds(offset);
            }
            
            return null;
        }

        @Override
        public String toString() {
            return "ZoneOffset";
        }
    };
    
    /**
     * A query for {@code LocalDate} returning null if not found.
     */
    /*
     * 查询LocalDate部件的信息
     *
     * 如果没有现成的部件，通常需要从指定的时间量中解析出纪元天，
     * 然后使用纪元天构造LocalDate后返回。
     */
    static final TemporalQuery<LocalDate> LOCAL_DATE = new TemporalQuery<>() {
        @Override
        public LocalDate queryFrom(TemporalAccessor temporal) {
            // 支持ChronoField.EPOCH_DAY的时间量通常含有日期组件
            if(temporal.isSupported(ChronoField.EPOCH_DAY)) {
                // 计算temporal包含的"纪元天"
                long epochDay = temporal.getLong(ChronoField.EPOCH_DAY);
                // 根据给定的纪元天构造"本地日期"
                return LocalDate.ofEpochDay(epochDay);
            }
            
            return null;
        }

        @Override
        public String toString() {
            return "LocalDate";
        }
    };
    
    /**
     * A query for {@code LocalTime} returning null if not found.
     */
    /*
     * 查询LocalTime部件的信息
     *
     * 如果没有现成的部件，通常需要从指定的时间量中解析出包含的纳秒数，
     * 然后使用该时间量包含的纳秒数构造LocalTime后返回。
     */
    static final TemporalQuery<LocalTime> LOCAL_TIME = new TemporalQuery<>() {
        @Override
        public LocalTime queryFrom(TemporalAccessor temporal) {
            // 支持ChronoField.EPOCH_DAY的时间量通常含有时间组件
            if(temporal.isSupported(ChronoField.NANO_OF_DAY)) {
                // 计算当前"本地时间"包含的纳秒数
                long nanos = temporal.getLong(ChronoField.NANO_OF_DAY);
                // 使用指定的纳秒数(不超过一天)构造"本地时间"
                return LocalTime.ofNanoOfDay(nanos);
            }
            
            return null;
        }

        @Override
        public String toString() {
            return "LocalTime";
        }
    };
    
}
