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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * A date-time with a time-zone in the calendar neutral API.
 * <p>
 * {@code ZoneChronoDateTime} is an immutable representation of a date-time with a time-zone.
 * This class stores all date and time fields, to a precision of nanoseconds,
 * as well as a time-zone and zone offset.
 * <p>
 * The purpose of storing the time-zone is to distinguish the ambiguous case where
 * the local time-line overlaps, typically as a result of the end of daylight time.
 * Information about the local-time can be obtained using methods on the time-zone.
 *
 * @param <D> the concrete type for the date of this date-time
 *
 * @implSpec This class is immutable and thread-safe.
 * @serial Document the delegation of this class in the serialized-form specification.
 * @since 1.8
 */
/*
 * "本地日期-时间"，"时间"[关联]了所属的时区ID，"日期"基于[非ISO]历法系统。
 *
 * 这里的非ISO历法系统可以是：
 * HijrahDate       - 伊斯兰历
 * ThaiBuddhistDate - 泰国佛教历
 * JapaneseDate     - 日本历
 * MinguoDate       - 中华民国历
 *
 * 注：该类其实也可以使用基于ISO历法系统的LocalDate，但是并不推荐这么用。
 * 　　这里如果需要使用基于[ISO]历法系统的"本地日期-时间"，推荐直接使用ZonedDateTime。
 */
final class ChronoZonedDateTimeImpl<D extends ChronoLocalDate> implements ChronoZonedDateTime<D>, Serializable {
    
    /**
     * The local date-time.
     */
    /*
     * 本地日期-时间
     *
     * 注：这个时间点反映的是zone/offset时区的时间点。
     */
    private final transient ChronoLocalDateTimeImpl<D> dateTime;
    
    /**
     * The zone ID.
     */
    /*
     * 时区ID，用来指示当前"本地日期-时间"所处的时区
     *
     * 这可能是[基于时间偏移的时区ID]，也可能是[基于地理时区的时区ID]
     */
    private final transient ZoneId zone;
    
    /**
     * The zone offset.
     */
    /*
     * 基于时间偏移的时区ID，用来指示当前"本地日期-时间"所处的时区
     *
     * 如果zone是[基于时间偏移的时区ID]，则offset的值与zone的值相等；
     * 如果zone是[基于地理时区的时区ID]，则需要根据其对应的时区规则集，计算出当前时刻该时区的时间偏移。
     */
    private final transient ZoneOffset offset;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructor.
     *
     * @param dateTime the date-time, not null
     * @param offset   the zone offset, not null
     * @param zone     the zone ID, not null
     */
    private ChronoZonedDateTimeImpl(ChronoLocalDateTimeImpl<D> dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = Objects.requireNonNull(dateTime, "dateTime");
        this.offset = Objects.requireNonNull(offset, "offset");
        this.zone = Objects.requireNonNull(zone, "zone");
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains an instance from an instant using the specified time-zone.
     *
     * @param chrono  the chronology, not null
     * @param instant the instant, not null
     * @param zone    the zone identifier, not null
     *
     * @return the zoned date-time, not null
     */
    // 使用指定的历法系统、时间戳和时区ID构造一个属于zone时区的"本地日期-时间"(时区偏移时间准确)
    static ChronoZonedDateTimeImpl<?> ofInstant(Chronology chrono, Instant instant, ZoneId zone) {
        
        // 获取与zone对应的"时区规则集"
        ZoneRules rules = zone.getRules();
        /*
         * 获取zone时区在instant时刻的"实际偏移"。
         * 这里可以返回一个准确的"实际偏移"。
         */
        ZoneOffset offset = rules.getOffset(instant);
        Objects.requireNonNull(offset, "offset");
        
        // 使用UTC时区的纪元秒、纳秒偏移以及时区ID构造一个属于offset时区的"本地日期-时间"对象
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset);
        
        // 从dateTime中获取/构造ChronoLocalDateTimeImpl对象
        ChronoLocalDateTimeImpl<?> localDateTime = (ChronoLocalDateTimeImpl<?>) chrono.localDateTime(dateTime);
        
        return new ChronoZonedDateTimeImpl<>(localDateTime, offset, zone);
    }
    
    /**
     * Obtains an instance from a local date-time using the preferred offset if possible.
     *
     * @param localDateTime   the local date-time, not null
     * @param zone            the zone identifier, not null
     * @param preferredOffset the zone offset, null if no preference
     *
     * @return the zoned date-time, not null
     */
    /*
     * 使用基于zone的localDateTime来构造ChronoZonedDateTime。
     * 如果zone不是ZoneOffset类型，则时区偏移时间可能会不准确。
     *
     * 如果localDateTime处于"正常时间"或"夏令时时间"中，则offset字段可以使用zone时区中唯一的有效偏移。
     * 如果localDateTime处于"间隙时间"中，则将该时间调到间隙后。
     * 如果localDateTime处于"重叠时间"中，则如果preferredOffset有效，则offset字段可以直接使用它，否则，默认使用首个时区偏移。
     *
     * localDateTime  : 基于zone的"本地日期-时间"
     * zone           : 新生成的"本地日期-时间"的时区ID
     * preferredOffset: 当localDateTime位于zone的"重叠时间"中时，使用该参数指定localDateTime应当使用的时区偏移。
     */
    static <R extends ChronoLocalDate> ChronoZonedDateTime<R> ofBest(ChronoLocalDateTimeImpl<R> localDateTime, ZoneId zone, ZoneOffset preferredOffset) {
        Objects.requireNonNull(localDateTime, "localDateTime");
        Objects.requireNonNull(zone, "zone");
        
        if(zone instanceof ZoneOffset) {
            return new ChronoZonedDateTimeImpl<>(localDateTime, (ZoneOffset) zone, zone);
        }
        
        // 由localDateTime中的日期时间信息，构造出一个LocalDateTime类型的对象
        LocalDateTime dateTime = LocalDateTime.from(localDateTime);
        
        // 获取与zone对应的"时区规则集"
        ZoneRules rules = zone.getRules();
        
        /*
         * 获取zone时区在dateTime时刻的"有效偏移"。
         *
         * 在"间隙时间"中，没有有效的偏移；
         * 在"重叠时间"中，存在两个有效偏移；
         * 在"正常时间"或"夏令时时间"中，存在一个有效偏移。
         *
         * dateTime: 视为zone时区的一个本地时间。
         */
        List<ZoneOffset> validOffsets = rules.getValidOffsets(dateTime);
        
        ZoneOffset offset;
        
        // 处于"正常时间"或"夏令时时间"中
        if(validOffsets.size() == 1) {
            // 直接使用此时获取到的那个有效偏移
            offset = validOffsets.get(0);
            
            // 处于"间隙时间"中，则将该时间调到间隙后
        } else if(validOffsets.size() == 0) {
            /*
             * 获取zone时区在localDateTime时刻的偏移转换规则，该规则用来指示如何切换时区的时间偏移。
             *
             * 只有在"间隙时间"和"重叠时间"中，需要用到偏移转换规则。
             * 在正常时间和夏令时时间内，无需用到偏移转换规则。
             *
             * dateTime: 视为zone时区的一个本地时间。
             */
            ZoneOffsetTransition trans = rules.getTransition(dateTime);
            
            // 间隙后的偏移 - 间隙前的偏移，通常指进入夏令时前后相差的时间
            long duration = trans.getDuration().getSeconds();
            
            // 在localDateTime的值上累加seconds秒
            localDateTime = localDateTime.plusSeconds(duration);
            
            // 获取间隙后的偏移
            offset = trans.getOffsetAfter();
            
            // 处于"重叠时间"中
        } else {
            /*
             * 如果预设了一个可以使用的有效偏移，则直接使用该偏移量.
             * 这里预设了偏移，相当于指定了该本地时间是作为夏令时时间还是非夏令时时间。
             */
            if(preferredOffset != null && validOffsets.contains(preferredOffset)) {
                offset = preferredOffset;
                
                /*
                 * 如果没有预设有效偏移，或者预设的有效偏移无效（不在"重叠时间"的有效偏移中），
                 * 此时，我们默认选择"重叠时间"的有效偏移列表中的第一个有效偏移。
                 * 通常来说，这个偏移是仍处于夏令时的偏移。
                 */
            } else {
                offset = validOffsets.get(0);
            }
        }
        
        Objects.requireNonNull(offset, "offset");  // protect against bad ZoneRules
        return new ChronoZonedDateTimeImpl<>(localDateTime, offset, zone);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 部件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回"时区ID"部件
    @Override
    public ZoneId getZone() {
        return zone;
    }
    
    // 返回基于时间偏移的"时区ID"部件
    @Override
    public ZoneOffset getOffset() {
        return offset;
    }
    
    // 返回"本地日期-时间"组件
    @Override
    public ChronoLocalDateTime<D> toLocalDateTime() {
        return dateTime;
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
    public ChronoZonedDateTime<D> plus(long amountToAdd, TemporalUnit unit) {
        if(unit instanceof ChronoUnit) {
            return with(dateTime.plus(amountToAdd, unit));
        }
        
        // 对当前时间量的值累加amountToAdd个unit单位的时间量
        ChronoZonedDateTimeImpl<D> dateTime = unit.addTo(this, amountToAdd);
        
        // 确保历法系统匹配
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), dateTime);   // TODO: Generics replacement Risk!
    }
    
    /*▲ 增加 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时间量字段操作(TemporalAccessor) ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 判断当前时间量是否支持指定的时间量字段
    @Override
    public boolean isSupported(TemporalField field) {
        return field instanceof ChronoField || (field != null && field.isSupportedBy(this));
    }
    
    /*▲ 时间量字段操作(TemporalAccessor) ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 整合 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
     * ChronoField.INSTANT_SECONDS - 与[纪元秒]进行整合，这会影响到原时间量对象的"小时"、"分钟"、"秒"部件
     * ===========================
     * LocalTime中支持的字段
     * ===========================
     * ChronoLocalDate的子类中支持的字段
     */
    @Override
    public ChronoZonedDateTime<D> with(TemporalField field, long newValue) {
        if(field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            
            if(f == ChronoField.INSTANT_SECONDS) {
                return plus(newValue - toEpochSecond(), SECONDS);
            }
            
            if(f == ChronoField.OFFSET_SECONDS) {
                ZoneOffset offset = ZoneOffset.ofTotalSeconds(f.checkValidIntValue(newValue));
                return create(dateTime.toInstant(offset), zone);
            }
            
            ChronoLocalDateTimeImpl<D> newDateTime = dateTime.with(field, newValue);
            
            /*
             * 使用基于zone的newDateTime来构造ChronoZonedDateTime。
             * 如果zone不是ZoneOffset类型，则时区偏移时间可能会不准确。
             */
            return ofBest(newDateTime, zone, offset);
        }
        
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), field.adjustInto(this, newValue));
    }
    
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
    @Override
    public ChronoZonedDateTime<D> withZoneSameLocal(ZoneId zone) {
        /*
         * 使用基于zone的dateTime来构造ChronoZonedDateTime。
         * 如果zone不是ZoneOffset类型，则时区偏移时间可能会不准确。
         */
        return ofBest(dateTime, zone, offset);
    }
    
    /*
     * 将指定的"时区ID"整合到当前时间量中以构造时间量对象。
     * 该操作会先获取在当前时刻时UTC时区的时间点，然后使用该时间点与zone构造一个ChronoZonedDateTime对象。
     *
     * 如果整合后的值与当前时间量中的值相等，则直接返回当前时间量对象。
     * 否则，需要构造"整合"后的新对象再返回。
     *
     * 注：整合过程，通常是时间量部件的替换/覆盖过程。
     * 　　至于是替换/覆盖一个部件还是多个部件，则需要根据参数的意义而定。
     *
     * 影响部件：本地日期-时间、时区ID、基于时间偏移的时区ID
     */
    @Override
    public ChronoZonedDateTime<D> withZoneSameInstant(ZoneId zone) {
        Objects.requireNonNull(zone, "zone");
        
        if(this.zone.equals(zone)) {
            return this;
        }
        
        /*
         * 将当前"本地日期-时间"转换为时间戳，该时间戳反映的是UTC/GMT"零时区"的时间点。
         *
         * offset: 指示当前"本地日期-时间"所处的时区偏移。
         */
        Instant instant = dateTime.toInstant(offset);
        
        return create(instant, zone);
    }
    
    /*
     * 如果当前本地日期-时间位于其所在时区的"重叠时间"中，
     * 则将当前时间量的时区偏移更新为"重叠时间"之前的时区偏移。
     */
    @Override
    public ChronoZonedDateTime<D> withEarlierOffsetAtOverlap() {
        
        // 获取当前"本地日期-时间"所在时区的时区ID
        ZoneId zone = getZone();
        
        // 获取与zone对应的"时区规则集"
        ZoneRules rules = zone.getRules();
        
        // 从当前时间量中中获取/构造LocalDateTime对象
        LocalDateTime dateTime = LocalDateTime.from(this);
        
        /*
         * 获取zone时区在dateTime时刻的偏移转换规则，该规则用来指示如何切换时区的时间偏移。
         *
         * 只有在"间隙时间"和"重叠时间"中，需要用到偏移转换规则。
         * 在正常时间和夏令时时间内，无需用到偏移转换规则。
         *
         * dateTime: 视为zone时区的一个本地时间。
         */
        ZoneOffsetTransition trans = rules.getTransition(dateTime);
        
        // 如果dateTime时刻位于"重叠时间"
        if(trans != null && trans.isOverlap()) {
            // 获取"重叠"前的时间偏移
            ZoneOffset earlierOffset = trans.getOffsetBefore();
            if(!earlierOffset.equals(offset)) {
                // 使用"重叠"前的时间偏移更新ZoneOffset属性
                return new ChronoZonedDateTimeImpl<>(this.dateTime, earlierOffset, this.zone);
            }
        }
        
        return this;
    }
    
    /*
     * 如果当前本地日期-时间位于其所在时区的"重叠时间"中，
     * 则将当前时间量的时区偏移更新为"重叠时间"之后的时区偏移。
     */
    @Override
    public ChronoZonedDateTime<D> withLaterOffsetAtOverlap() {
        
        // 获取当前"本地日期-时间"所在时区的时区ID
        ZoneId zone = getZone();
        
        // 获取与zone对应的"时区规则集"
        ZoneRules rules = zone.getRules();
        
        // 从当前时间量中中获取/构造LocalDateTime对象
        LocalDateTime dateTime = LocalDateTime.from(this);
        
        /*
         * 获取zone时区在dateTime时刻的偏移转换规则，该规则用来指示如何切换时区的时间偏移。
         *
         * 只有在"间隙时间"和"重叠时间"中，需要用到偏移转换规则。
         * 在正常时间和夏令时时间内，无需用到偏移转换规则。
         *
         * dateTime: 视为zone时区的一个本地时间。
         */
        ZoneOffsetTransition trans = rules.getTransition(dateTime);
        
        // 注：这里没有进行trans.isOverlap()判断依然可行，原因是前面构造ChronoZonedDateTime时，对于"间隙"中的时间会将其前进到夏令时中
        if(trans != null) {
            // 获取"重叠"后的时间偏移
            ZoneOffset laterOffset = trans.getOffsetAfter();
            if(!laterOffset.equals(getOffset())) {
                // 使用"重叠"后的时间偏移更新ZoneOffset属性
                return new ChronoZonedDateTimeImpl<>(this.dateTime, laterOffset, zone);
            }
        }
        
        return this;
    }
    
    /*▲ 整合 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 计算当前时间量到目标时间量endExclusive之间相差多少个unit单位的时间值
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        Objects.requireNonNull(endExclusive, "endExclusive");
        Objects.requireNonNull(unit, "unit");
        
        @SuppressWarnings("unchecked")
        ChronoZonedDateTime<D> end = (ChronoZonedDateTime<D>) getChronology().zonedDateTime(endExclusive);
        if(unit instanceof ChronoUnit) {
            end = end.withZoneSameInstant(offset);
            return dateTime.until(end.toLocalDateTime(), unit);
        }
        
        return unit.between(this, end);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Casts the {@code Temporal} to {@code ChronoZonedDateTimeImpl} ensuring it bas the specified chronology.
     *
     * @param chrono   the chronology to check for, not null
     * @param temporal a date-time to cast, not null
     *
     * @return the date-time checked and cast to {@code ChronoZonedDateTimeImpl}, not null
     *
     * @throws ClassCastException if the date-time cannot be cast to ChronoZonedDateTimeImpl
     *                            or the chronology is not equal this Chronology
     */
    // 确保temporal的历法系统为chrono
    static <R extends ChronoLocalDate> ChronoZonedDateTimeImpl<R> ensureValid(Chronology chrono, Temporal temporal) {
        @SuppressWarnings("unchecked")
        ChronoZonedDateTimeImpl<R> other = (ChronoZonedDateTimeImpl<R>) temporal;
        if(!chrono.equals(other.getChronology())) {
            throw new ClassCastException("Chronology mismatch, required: " + chrono.getId() + ", actual: " + other.getChronology().getId());
        }
        return other;
    }
    
    /**
     * Obtains an instance from an {@code Instant}.
     *
     * @param instant the instant to create the date-time from, not null
     * @param zone    the time-zone to use, validated not null
     *
     * @return the zoned date-time, validated not null
     */
    // 使用当前时间量绑定的历法系统、时间戳和时区ID构造一个属于zone时区的"本地日期-时间"(时区偏移时间准确)
    @SuppressWarnings("unchecked")
    private ChronoZonedDateTimeImpl<D> create(Instant instant, ZoneId zone) {
        return (ChronoZonedDateTimeImpl<D>) ofInstant(getChronology(), instant, zone);
    }
    
    
    @Override
    public String toString() {
        String str = toLocalDateTime().toString() + getOffset().toString();
        if(getOffset() != getZone()) {
            str += '[' + getZone().toString() + ']';
        }
        return str;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj instanceof ChronoZonedDateTime) {
            return compareTo((ChronoZonedDateTime<?>) obj) == 0;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return toLocalDateTime().hashCode() ^ getOffset().hashCode() ^ Integer.rotateLeft(getZone().hashCode(), 3);
    }
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = -5261813987200935591L;
    
    /**
     * Writes the ChronoZonedDateTime using a
     * <a href="../../../serialized-form.html#java.time.chrono.Ser">dedicated serialized form</a>.
     *
     * @return the instance of {@code Ser}, not null
     *
     * @serialData <pre>
     *  out.writeByte(3);                  // identifies a ChronoZonedDateTime
     *  out.writeObject(toLocalDateTime());
     *  out.writeObject(getOffset());
     *  out.writeObject(getZone());
     * </pre>
     */
    private Object writeReplace() {
        return new Ser(Ser.CHRONO_ZONE_DATE_TIME_TYPE, this);
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
    
    static ChronoZonedDateTime<?> readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ChronoLocalDateTime<?> dateTime = (ChronoLocalDateTime<?>) in.readObject();
        ZoneOffset offset = (ZoneOffset) in.readObject();
        ZoneId zone = (ZoneId) in.readObject();
        return dateTime.atZone(offset).withZoneSameLocal(zone);
        // TODO: ZDT uses ofLenient()
    }
    
    void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(dateTime);
        out.writeObject(offset);
        out.writeObject(zone);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
