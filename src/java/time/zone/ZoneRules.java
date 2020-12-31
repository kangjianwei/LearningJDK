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
 * Copyright (c) 2009-2012, Stephen Colebourne & Michael Nascimento Santos
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
package java.time.zone;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The rules defining how the zone offset varies for a single time-zone.
 * <p>
 * The rules model all the historic and future transitions for a time-zone.
 * {@link ZoneOffsetTransition} is used for known transitions, typically historic.
 * {@link ZoneOffsetTransitionRule} is used for future transitions that are based
 * on the result of an algorithm.
 * <p>
 * The rules are loaded via {@link ZoneRulesProvider} using a {@link ZoneId}.
 * The same rules may be shared internally between multiple zone IDs.
 * <p>
 * Serializing an instance of {@code ZoneRules} will store the entire set of rules.
 * It does not store the zone ID as it is not part of the state of this object.
 * <p>
 * A rule implementation may or may not store full information about historic
 * and future transitions, and the information stored is only as accurate as
 * that supplied to the implementation by the rules provider.
 * Applications should treat the data provided as representing the best information
 * available to the implementation of this rule.
 *
 * @implSpec This class is immutable and thread-safe.
 * @since 1.8
 */
/*
 * 时区规则集，隶属于某个预设的时区
 *
 * 时区规则集可以反映全球各地区采用的历史计时规则以及通用计时规则。
 * 比如有的地区在某些年份使用了夏令时，这会在时区规则集中有记录。
 *
 * 通常，时区规则集是从预设的本地文件中加载的，这个本地文件的位置不一，
 * 譬如在JDK-11中，其位置是{JAVA_HOME}/lib/tzdb.dat，
 * 该文件支持更新，参见：https://www.oracle.com/technetwork/cn/java/javase/tzupdater-readme-136440-zhs.html。
 *
 * 关于时间规则数据库，参见：https://www.iana.org/time-zones
 */
public final class ZoneRules implements Serializable {
    
    /**
     * The last year to have its transitions cached.
     */
    private static final int LAST_CACHED_YEAR = 2100;
    
    /**
     * The zero-length long array.
     */
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    
    /**
     * The zero-length lastrules array.
     */
    private static final ZoneOffsetTransitionRule[] EMPTY_LASTRULES = new ZoneOffsetTransitionRule[0];
    
    /**
     * The zero-length ldt array.
     */
    private static final LocalDateTime[] EMPTY_LDT_ARRAY = new LocalDateTime[0];
    
    /**
     * The transitions between standard offsets (epoch seconds), sorted.
     */
    private final long[] standardTransitions;
    
    /**
     * The standard offsets.
     */
    private final ZoneOffset[] standardOffsets;
    
    /**
     * The transitions between instants (epoch seconds), sorted.
     */
    private final long[] savingsInstantTransitions;
    
    /**
     * The transitions between local date-times, sorted.
     * This is a paired array, where the first entry is the start of the transition
     * and the second entry is the end of the transition.
     */
    private final LocalDateTime[] savingsLocalTransitions;
    
    /**
     * The wall offsets.
     */
    private final ZoneOffset[] wallOffsets;
    
    /**
     * The last rule.
     */
    private final ZoneOffsetTransitionRule[] lastRules;
    
    /**
     * The map of recent transitions.
     */
    private final transient ConcurrentMap<Integer, ZoneOffsetTransition[]> lastRulesCache = new ConcurrentHashMap<Integer, ZoneOffsetTransition[]>();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates an instance.
     *
     * @param baseStandardOffset           the standard offset to use before legal rules were set, not null
     * @param baseWallOffset               the wall offset to use before legal rules were set, not null
     * @param standardOffsetTransitionList the list of changes to the standard offset, not null
     * @param transitionList               the list of transitions, not null
     * @param lastRules                    the recurring last rules, size 16 or less, not null
     */
    ZoneRules(ZoneOffset baseStandardOffset, ZoneOffset baseWallOffset, List<ZoneOffsetTransition> standardOffsetTransitionList, List<ZoneOffsetTransition> transitionList, List<ZoneOffsetTransitionRule> lastRules) {
        super();
        
        // convert standard transitions
        
        this.standardTransitions = new long[standardOffsetTransitionList.size()];
        
        this.standardOffsets = new ZoneOffset[standardOffsetTransitionList.size() + 1];
        this.standardOffsets[0] = baseStandardOffset;
        for(int i = 0; i<standardOffsetTransitionList.size(); i++) {
            this.standardTransitions[i] = standardOffsetTransitionList.get(i).toEpochSecond();
            this.standardOffsets[i + 1] = standardOffsetTransitionList.get(i).getOffsetAfter();
        }
        
        // convert savings transitions to locals
        List<LocalDateTime> localTransitionList = new ArrayList<>();
        List<ZoneOffset> localTransitionOffsetList = new ArrayList<>();
        localTransitionOffsetList.add(baseWallOffset);
        for(ZoneOffsetTransition trans : transitionList) {
            if(trans.isGap()) {
                localTransitionList.add(trans.getDateTimeBefore());
                localTransitionList.add(trans.getDateTimeAfter());
            } else {
                localTransitionList.add(trans.getDateTimeAfter());
                localTransitionList.add(trans.getDateTimeBefore());
            }
            localTransitionOffsetList.add(trans.getOffsetAfter());
        }
        this.savingsLocalTransitions = localTransitionList.toArray(new LocalDateTime[localTransitionList.size()]);
        this.wallOffsets = localTransitionOffsetList.toArray(new ZoneOffset[localTransitionOffsetList.size()]);
        
        // convert savings transitions to instants
        this.savingsInstantTransitions = new long[transitionList.size()];
        for(int i = 0; i<transitionList.size(); i++) {
            this.savingsInstantTransitions[i] = transitionList.get(i).toEpochSecond();
        }
        
        // last rules
        if(lastRules.size()>16) {
            throw new IllegalArgumentException("Too many transition rules");
        }
        this.lastRules = lastRules.toArray(new ZoneOffsetTransitionRule[lastRules.size()]);
    }
    
    /**
     * Constructor.
     *
     * @param standardTransitions       the standard transitions, not null
     * @param standardOffsets           the standard offsets, not null
     * @param savingsInstantTransitions the standard transitions, not null
     * @param wallOffsets               the wall offsets, not null
     * @param lastRules                 the recurring last rules, size 15 or less, not null
     */
    private ZoneRules(long[] standardTransitions, ZoneOffset[] standardOffsets, long[] savingsInstantTransitions, ZoneOffset[] wallOffsets, ZoneOffsetTransitionRule[] lastRules) {
        super();
        
        this.standardTransitions = standardTransitions;
        this.standardOffsets = standardOffsets;
        this.savingsInstantTransitions = savingsInstantTransitions;
        this.wallOffsets = wallOffsets;
        this.lastRules = lastRules;
        
        if(savingsInstantTransitions.length == 0) {
            this.savingsLocalTransitions = EMPTY_LDT_ARRAY;
        } else {
            // convert savings transitions to locals
            List<LocalDateTime> localTransitionList = new ArrayList<>();
            for(int i = 0; i<savingsInstantTransitions.length; i++) {
                ZoneOffset before = wallOffsets[i];
                ZoneOffset after = wallOffsets[i + 1];
                ZoneOffsetTransition trans = new ZoneOffsetTransition(savingsInstantTransitions[i], before, after);
                if(trans.isGap()) {
                    localTransitionList.add(trans.getDateTimeBefore());
                    localTransitionList.add(trans.getDateTimeAfter());
                } else {
                    localTransitionList.add(trans.getDateTimeAfter());
                    localTransitionList.add(trans.getDateTimeBefore());
                }
            }
            
            this.savingsLocalTransitions = localTransitionList.toArray(new LocalDateTime[0]);
        }
    }
    
    /**
     * Creates an instance of ZoneRules that has fixed zone rules.
     *
     * @param offset the offset this fixed zone rules is based on, not null
     *
     * @see #isFixedOffset()
     */
    private ZoneRules(ZoneOffset offset) {
        this.standardOffsets = new ZoneOffset[1];
        this.standardOffsets[0] = offset;
        this.standardTransitions = EMPTY_LONG_ARRAY;
        this.savingsInstantTransitions = EMPTY_LONG_ARRAY;
        this.savingsLocalTransitions = EMPTY_LDT_ARRAY;
        this.wallOffsets = standardOffsets;
        this.lastRules = EMPTY_LASTRULES;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Obtains an instance of ZoneRules that has fixed zone rules.
     *
     * @param offset the offset this fixed zone rules is based on, not null
     *
     * @return the zone rules, not null
     *
     * @see #isFixedOffset()
     */
    // 由给定的时区偏移构造时区规则集
    public static ZoneRules of(ZoneOffset offset) {
        Objects.requireNonNull(offset, "offset");
        return new ZoneRules(offset);
    }
    
    /**
     * Obtains an instance of a ZoneRules.
     *
     * @param baseStandardOffset           the standard offset to use before legal rules were set, not null
     * @param baseWallOffset               the wall offset to use before legal rules were set, not null
     * @param standardOffsetTransitionList the list of changes to the standard offset, not null
     * @param transitionList               the list of transitions, not null
     * @param lastRules                    the recurring last rules, size 16 or less, not null
     *
     * @return the zone rules, not null
     */
    // 构造地理时区的时区规则集
    public static ZoneRules of(ZoneOffset baseStandardOffset, ZoneOffset baseWallOffset, List<ZoneOffsetTransition> standardOffsetTransitionList, List<ZoneOffsetTransition> transitionList, List<ZoneOffsetTransitionRule> lastRules) {
        Objects.requireNonNull(baseStandardOffset, "baseStandardOffset");
        Objects.requireNonNull(baseWallOffset, "baseWallOffset");
        Objects.requireNonNull(standardOffsetTransitionList, "standardOffsetTransitionList");
        Objects.requireNonNull(transitionList, "transitionList");
        Objects.requireNonNull(lastRules, "lastRules");
        
        return new ZoneRules(baseStandardOffset, baseWallOffset, standardOffsetTransitionList, transitionList, lastRules);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 实际偏移 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 示例
     *
     * 现今，美国(United States)洛杉矶(Los Angeles)仍执行夏令时。
     * 以2020年为例，从当地标准时间2020/3/8/02:00开始，该地区进入夏令时，时钟拨快一个小时。
     * 从当地标准时间2020/11/1/02:00结束，该地区结束夏令时，时钟拨回与当前地区所在的UTC时区相同的时间。
     *
     * UTC   | 3/8/09:59 [3/8/10:00 ... 11/1/08:59 11/1/09:00)
     * UTC-8 | 3/8/01:59 [3/8/02:00 ... 11/1/00:59 11/1/01:00)
     * local | 3/8/01:59 [3/8/03:00 ... 11/1/01:59 11/1/01:00)  实际
     *  ???               3/8/02:00                11/1/02:00   预期
     *
     *
     * // 美国洛杉矶的地理时区ID，其位于UTC时区的西八区(-8)
     * ZoneId zoneId = ZoneId.of("America/Los_Angeles");
     * // 获取当前时区对应的时区规则集
     * ZoneRules rules = zoneId.getRules();
     *
     *
     * // 注：下面选取的时间点只精确到秒
     * LocalDateTime local1 = LocalDateTime.of(2020, 3,  8, 1, 59); // ★ 即将进入夏令时
     * LocalDateTime local2 = LocalDateTime.of(2020, 3,  8, 2, 00); //   "间隙时间"的起始
     * LocalDateTime local3 = LocalDateTime.of(2020, 3,  8, 2, 59); //   "间隙时间"的终止
     * LocalDateTime local4 = LocalDateTime.of(2020, 3,  8, 3, 00); // ★ 夏令时开始
     * LocalDateTime local5 = LocalDateTime.of(2020, 11, 1, 0, 59); // ★ 即将进入"重叠时间"
     * LocalDateTime local6 = LocalDateTime.of(2020, 11, 1, 1, 00); //   "重叠时间"的起始，第二次执行时，表示夏令时结束
     * LocalDateTime local7 = LocalDateTime.of(2020, 11, 1, 1, 59); //   "重叠时间"的终止
     * LocalDateTime local8 = LocalDateTime.of(2020, 11, 1, 2, 00); // ★ 夏令时早已结束
     * System.out.println("=========================================");
     * System.out.println(rules.getOffset(local1)); // (1) -08:00 ★
     * System.out.println(rules.getOffset(local2)); // (2) -08:00
     * System.out.println(rules.getOffset(local3)); // (3) -08:00
     * System.out.println(rules.getOffset(local4)); // (4) -07:00 ★
     * System.out.println(rules.getOffset(local5)); // (5) -07:00 ★
     * System.out.println(rules.getOffset(local6)); // (6) -07:00
     * System.out.println(rules.getOffset(local7)); // (7) -07:00
     * System.out.println(rules.getOffset(local8)); // (8) -08:00 ★
     *
     *
     * // 注：下面选取的时间点只精确到秒
     * Instant instant1 = LocalDateTime.of(2020, 3,  8,  9, 59).toInstant(ZoneOffset.UTC);
     * Instant instant2 = LocalDateTime.of(2020, 3,  8, 10,  0).toInstant(ZoneOffset.UTC);
     * Instant instant3 = LocalDateTime.of(2020, 11, 1,  8, 59).toInstant(ZoneOffset.UTC);
     * Instant instant4 = LocalDateTime.of(2020, 11, 1,  9,  0).toInstant(ZoneOffset.UTC);
     * System.out.println("=========================================");
     * System.out.println(rules.getOffset(instant1)); // ( 9) -08:00
     * System.out.println(rules.getOffset(instant2)); // (10) -07:00
     * System.out.println(rules.getOffset(instant3)); // (11) -07:00
     * System.out.println(rules.getOffset(instant4)); // (12) -08:00
     *
     * 解释上面的输出：
     * (1)~(8)使用本地时间测试实际偏移，(9)~(12)使用UTC时间测试实际偏移。
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * >> 当使用本地时间时，从"本地日期-时间"到实际偏移的映射分为三种情况：
     * ---------------------------------------------------------
     * Gap    : 间隙，理论上不存在有效偏移
     *          规定从当地标准时间3/8/02:00开始，该地区进入夏令时，时钟拨快一个小时，
     *          因此，原本时间应当从3/8/01:59跳到3/8/02:00，
     *          实际在该地区就变成从3/8/01:59跳到3/8/03:00。
     *          这就导致3月8日的时间段[2:00, 2:59)在该地区是缺失的，这个缺失的时间段被称为"间隙时间"。
     * ---------------------------------------------------------
     * Overlap: 重叠，理论上存在两个有效偏移
     *          规定从当地标准时间11/1/02:00开始，该地区退出夏令时，时钟拨慢一个小时，即恢复到该地区不使用夏令时的正常时间。
     *          因此，原本时间应当从11/1/01:59跳到11/1/02:00，
     *          实际在该地区就变成从11/1/01:59跳到11/1/01:00。
     *          由于拨慢的这一个小时，导致11月1日的时间段[1:00, 2:00)会被执行两次，第一次在夏令时中，第二次在正常时间中。
     *          这个被重复执行的时间段，我们称之为"重叠时间"。
     * ---------------------------------------------------------
     * Normal : 正常，理论上存在一个有效偏移
     *          除去夏令时时间段、"间隙时间"、"重叠时间"的剩余时间，我们称之为"正常时间"。
     * ---------------------------------------------------------
     * 在"间隙时间"和"重叠时间"时，系统并不能准确地反应时区偏移，返回的偏移量是一个"最佳"值而不是"正确"值，
     * 这个"最佳"值往往会惯性地与上个阶段的值相同。
     * 在夏令时时间段，得到的实际偏移是-7，因为时钟拨快了一个小时，
     * 在正常时间段，得到的实际偏移是-8，因为恢复到了该时区的标准时间。
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * >> 当使用UTC时间测试时，这就很"正常"了：
     * 当UTC时间进入 3/8/10:00时，该地区所在时区的UTC-8时间进入 3/8/02:00，此时开始夏令时。
     * 当UTC时间进入11/1/09:00时，该地区所在时区的UTC-8时间进入11/1/01:00，此时结束夏令时。
     *
     * 上面没有写测试"标准偏移"的代码，因为标准偏移往往是固定的。
     * 标准偏移与该地区所在的UTC时区相关，由于"America/Los_Angeles"地区位于UTC-8时区，所以其"标准偏移"总是返回-8.
     */
    
    /**
     * Gets the offset applicable at the specified instant in these rules.
     * <p>
     * The mapping from an instant to an offset is simple, there is only
     * one valid offset for each instant.
     * This method returns that offset.
     *
     * @param instant the instant to find the offset for, not null, but null
     *                may be ignored if the rules have a single offset for all instants
     *
     * @return the offset, not null
     */
    /*
     * 获取当前时区规则集在instant时刻的"实际偏移"；这里可以返回一个准确的"实际偏移"。
     *
     * instant: 时间戳，用来表示UTC/GMT"零时区"的一个时间点。
     *
     * 注："实际偏移"受夏令时的影响，不保证与"标准偏移"保持一致。
     */
    public ZoneOffset getOffset(Instant instant) {
        if(savingsInstantTransitions.length == 0) {
            return standardOffsets[0];
        }
        
        long epochSec = instant.getEpochSecond();
        
        // check if using last rules
        if(lastRules.length>0 && epochSec>savingsInstantTransitions[savingsInstantTransitions.length - 1]) {
            int year = findYear(epochSec, wallOffsets[wallOffsets.length - 1]);
            
            ZoneOffsetTransition trans = null;
            ZoneOffsetTransition[] transArray = findTransitionArray(year);
            for(ZoneOffsetTransition zoneOffsetTransition : transArray) {
                trans = zoneOffsetTransition;
                if(epochSec<trans.toEpochSecond()) {
                    return trans.getOffsetBefore();
                }
            }
            
            return trans.getOffsetAfter();
        }
        
        // using historic rules
        int index = Arrays.binarySearch(savingsInstantTransitions, epochSec);
        if(index<0) {
            // switch negative insert position to start of matched range
            index = -index - 2;
        }
        
        return wallOffsets[index + 1];
    }
    
    /**
     * Gets a suitable offset for the specified local date-time in these rules.
     * <p>
     * The mapping from a local date-time to an offset is not straightforward.
     * There are three cases:
     * <ul>
     * <li>Normal, with one valid offset. For the vast majority of the year, the normal
     *  case applies, where there is a single valid offset for the local date-time.</li>
     * <li>Gap, with zero valid offsets. This is when clocks jump forward typically
     *  due to the spring daylight savings change from "winter" to "summer".
     *  In a gap there are local date-time values with no valid offset.</li>
     * <li>Overlap, with two valid offsets. This is when clocks are set back typically
     *  due to the autumn daylight savings change from "summer" to "winter".
     *  In an overlap there are local date-time values with two valid offsets.</li>
     * </ul>
     * Thus, for any given local date-time there can be zero, one or two valid offsets.
     * This method returns the single offset in the Normal case, and in the Gap or Overlap
     * case it returns the offset before the transition.
     * <p>
     * Since, in the case of Gap and Overlap, the offset returned is a "best" value, rather
     * than the "correct" value, it should be treated with care. Applications that care
     * about the correct offset should use a combination of this method,
     * {@link #getValidOffsets(LocalDateTime)} and {@link #getTransition(LocalDateTime)}.
     *
     * @param localDateTime the local date-time to query, not null, but null
     *                      may be ignored if the rules have a single offset for all instants
     *
     * @return the best available offset for the local date-time, not null
     */
    /*
     * 获取当前时区规则集在localDateTime时刻的"实际偏移"。
     * 当处于"间隙时间"或"重叠时间"时，这里返回的"实际偏移"并不保证准确。
     *
     * localDateTime: 当前时区规则集所属时区的一个本地时间。
     *
     * 注："实际偏移"受夏令时的影响，不保证与"标准偏移"保持一致。
     */
    public ZoneOffset getOffset(LocalDateTime localDateTime) {
        Object info = getOffsetInfo(localDateTime);
        
        if(info instanceof ZoneOffsetTransition) {
            return ((ZoneOffsetTransition) info).getOffsetBefore();
        }
        
        return (ZoneOffset) info;
    }
    
    /**
     * Checks of the zone rules are fixed, such that the offset never varies.
     *
     * @return true if the time-zone is fixed and the offset never changes
     */
    /*
     * 判断当前时区的"实际偏移"规则是否固定
     *
     * 通常，[基于地理时区的时区ID]中那些包含地名的时区对应的"实际偏移"规则是不固定的(往往受夏令时影响)；
     * 而那些标准的UTC/GMT时区，比如"Etc/GMT-8"这样的时区，以及[基于时间偏移的时区ID]，比如"+8"这样的时区，
     * 其对应的"实际偏移"规则是固定的(不会考虑夏令时)。
     */
    public boolean isFixedOffset() {
        return savingsInstantTransitions.length == 0;
    }
    
    /*▲ 实际偏移 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 标准偏移 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the standard offset for the specified instant in this zone.
     * <p>
     * This provides access to historic information on how the standard offset
     * has changed over time.
     * The standard offset is the offset before any daylight saving time is applied.
     * This is typically the offset applicable during winter.
     *
     * @param instant the instant to find the offset information for, not null, but null
     *                may be ignored if the rules have a single offset for all instants
     *
     * @return the standard offset, not null
     */
    /*
     * 获取当前时区规则集在instant时刻的"标准偏移"。
     *
     * instant: 时间戳，用来表示UTC/GMT"零时区"的一个时间点。
     *
     * 注："标准偏移"通常是固定的，该偏移与被测试地区所在时区的标准偏移是一致的。
     */
    public ZoneOffset getStandardOffset(Instant instant) {
        if(savingsInstantTransitions.length == 0) {
            return standardOffsets[0];
        }
        
        // 获取instant的纪元秒部件
        long epochSec = instant.getEpochSecond();
        
        int index = Arrays.binarySearch(standardTransitions, epochSec);
        if(index<0) {
            // switch negative insert position to start of matched range
            index = -index - 2;
        }
        
        return standardOffsets[index + 1];
    }
    
    /*▲ 标准偏移 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 有效偏移 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * // 美国洛杉矶的地理时区ID，其位于UTC时区的西八区(-8)
     * ZoneId zoneId = ZoneId.of("America/Los_Angeles");
     * // 获取当前时区对应的时区规则集
     * ZoneRules rules = zoneId.getRules();
     *
     * // 注：下面选取的时间点只精确到秒
     * LocalDateTime local1 = LocalDateTime.of(2020, 3,  8, 1, 59); // ★ 即将进入夏令时
     * LocalDateTime local2 = LocalDateTime.of(2020, 3,  8, 2, 00); //   "间隙时间"的起始
     * LocalDateTime local3 = LocalDateTime.of(2020, 3,  8, 2, 59); //   "间隙时间"的终止
     * LocalDateTime local4 = LocalDateTime.of(2020, 3,  8, 3, 00); // ★ 夏令时开始
     * LocalDateTime local5 = LocalDateTime.of(2020, 11, 1, 0, 59); //   即将进入"重叠时间"
     * LocalDateTime local6 = LocalDateTime.of(2020, 11, 1, 1, 00); //   "重叠时间"的起始
     * LocalDateTime local7 = LocalDateTime.of(2020, 11, 1, 1, 59); //   "重叠时间"的终止
     * LocalDateTime local8 = LocalDateTime.of(2020, 11, 1, 2, 00); // ★ 夏令时结束
     *
     * System.out.println("=========================================");
     * System.out.println(rules.getValidOffsets(local1)); // [-08:00] ★
     * System.out.println(rules.getValidOffsets(local2)); // []
     * System.out.println(rules.getValidOffsets(local3)); // []
     * System.out.println(rules.getValidOffsets(local4)); // [-07:00] ★
     * System.out.println(rules.getValidOffsets(local5)); // [-07:00]
     * System.out.println(rules.getValidOffsets(local6)); // [-07:00, -08:00]
     * System.out.println(rules.getValidOffsets(local7)); // [-07:00, -08:00]
     * System.out.println(rules.getValidOffsets(local8)); // [-08:00] ★
     *
     * System.out.println("=========================================");
     * // local1位于"正常时间"段，只有"-8"一个偏移
     * System.out.println("正常时间　 (-7) " + rules.isValidOffset(local1, ZoneOffset.of("-7"))); // 正常时间　 (-7) false
     * System.out.println("正常时间　 (-8) " + rules.isValidOffset(local1, ZoneOffset.of("-8"))); // 正常时间　 (-8) true
     * // local2位于"间隙时间"段，没有有效偏移
     * System.out.println("间隙时间　 (-7) " + rules.isValidOffset(local2, ZoneOffset.of("-7"))); // 间隙时间　 (-7) false
     * System.out.println("间隙时间　 (-8) " + rules.isValidOffset(local2, ZoneOffset.of("-8"))); // 间隙时间　 (-8) false
     * // local4位于"夏令时时间"段，只有"-7"一个偏移
     * System.out.println("夏令时时间 (-7) " + rules.isValidOffset(local4, ZoneOffset.of("-7"))); // 夏令时时间 (-7) true
     * System.out.println("夏令时时间 (-8) " + rules.isValidOffset(local4, ZoneOffset.of("-8"))); // 夏令时时间 (-8) false
     * // local6位于"重叠时间"段，包含"-7"进而"-8"两个偏移
     * System.out.println("重叠时间　 (-7) " + rules.isValidOffset(local6, ZoneOffset.of("-7"))); // 重叠时间　 (-7) true
     * System.out.println("重叠时间　 (-8) " + rules.isValidOffset(local6, ZoneOffset.of("-8"))); // 重叠时间　 (-8) true
     *
     * 输出结果参见"实际偏移"中的描述。
     */
    
    /**
     * Gets the offset applicable at the specified local date-time in these rules.
     * <p>
     * The mapping from a local date-time to an offset is not straightforward.
     * There are three cases:
     * <ul>
     * <li>Normal, with one valid offset. For the vast majority of the year, the normal
     *  case applies, where there is a single valid offset for the local date-time.</li>
     * <li>Gap, with zero valid offsets. This is when clocks jump forward typically
     *  due to the spring daylight savings change from "winter" to "summer".
     *  In a gap there are local date-time values with no valid offset.</li>
     * <li>Overlap, with two valid offsets. This is when clocks are set back typically
     *  due to the autumn daylight savings change from "summer" to "winter".
     *  In an overlap there are local date-time values with two valid offsets.</li>
     * </ul>
     * Thus, for any given local date-time there can be zero, one or two valid offsets.
     * This method returns that list of valid offsets, which is a list of size 0, 1 or 2.
     * In the case where there are two offsets, the earlier offset is returned at index 0
     * and the later offset at index 1.
     * <p>
     * There are various ways to handle the conversion from a {@code LocalDateTime}.
     * One technique, using this method, would be:
     * <pre>
     *  List&lt;ZoneOffset&gt; validOffsets = rules.getOffset(localDT);
     *  if (validOffsets.size() == 1) {
     *    // Normal case: only one valid offset
     *    zoneOffset = validOffsets.get(0);
     *  } else {
     *    // Gap or Overlap: determine what to do from transition (which will be non-null)
     *    ZoneOffsetTransition trans = rules.getTransition(localDT);
     *  }
     * </pre>
     * <p>
     * In theory, it is possible for there to be more than two valid offsets.
     * This would happen if clocks to be put back more than once in quick succession.
     * This has never happened in the history of time-zones and thus has no special handling.
     * However, if it were to happen, then the list would return more than 2 entries.
     *
     * @param localDateTime the local date-time to query for valid offsets, not null, but null
     *                      may be ignored if the rules have a single offset for all instants
     *
     * @return the list of valid offsets, may be immutable, not null
     */
    /*
     * 获取当前时区规则集在localDateTime时刻的"有效偏移"。
     *
     * 在"间隙时间"中，没有有效的偏移；
     * 在"重叠时间"中，存在两个有效偏移；
     * 在"正常时间"或"夏令时时间"中，存在一个有效偏移。
     *
     * localDateTime: 当前时区规则集所属时区的一个本地时间。
     */
    public List<ZoneOffset> getValidOffsets(LocalDateTime localDateTime) {
        // should probably be optimized
        Object info = getOffsetInfo(localDateTime);
        
        if(info instanceof ZoneOffsetTransition) {
            return ((ZoneOffsetTransition) info).getValidOffsets();
        }
        
        return Collections.singletonList((ZoneOffset) info);
    }
    
    /**
     * Checks if the offset date-time is valid for these rules.
     * <p>
     * To be valid, the local date-time must not be in a gap and the offset
     * must match one of the valid offsets.
     * <p>
     * This default implementation checks if {@link #getValidOffsets(java.time.LocalDateTime)}
     * contains the specified offset.
     *
     * @param localDateTime the date-time to check, not null, but null
     *                      may be ignored if the rules have a single offset for all instants
     * @param offset        the offset to check, null returns false
     *
     * @return true if the offset date-time is valid for these rules
     */
    // 判断当前时区规则集在localDateTime时刻的"有效偏移"中是否包含offset偏移
    public boolean isValidOffset(LocalDateTime localDateTime, ZoneOffset offset) {
        return getValidOffsets(localDateTime).contains(offset);
    }
    
    /*▲ 有效偏移 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 偏移转换规则 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * // 美国洛杉矶的地理时区ID，其位于UTC时区的西八区(-8)
     * ZoneId zoneId = ZoneId.of("America/Los_Angeles");
     * // 获取当前时区对应的时区规则集
     * ZoneRules rules = zoneId.getRules();
     *
     * // 注：下面选取的时间点只精确到秒
     * LocalDateTime local1 = LocalDateTime.of(2020, 3,  8, 1, 59); // ★ 即将进入夏令时
     * LocalDateTime local2 = LocalDateTime.of(2020, 3,  8, 2, 00); //   "间隙时间"的起始
     * LocalDateTime local3 = LocalDateTime.of(2020, 3,  8, 2, 59); //   "间隙时间"的终止
     * LocalDateTime local4 = LocalDateTime.of(2020, 3,  8, 3, 00); // ★ 夏令时开始
     * LocalDateTime local5 = LocalDateTime.of(2020, 11, 1, 0, 59); //   即将进入"重叠时间"
     * LocalDateTime local6 = LocalDateTime.of(2020, 11, 1, 1, 00); //   "重叠时间"的起始
     * LocalDateTime local7 = LocalDateTime.of(2020, 11, 1, 1, 59); //   "重叠时间"的终止
     * LocalDateTime local8 = LocalDateTime.of(2020, 11, 1, 2, 00); // ★ 夏令时结束
     *
     * System.out.println("=========================================");
     * System.out.println(rules.getTransition(local1)); // null
     * System.out.println(rules.getTransition(local2)); // Transition[Gap at 2020-03-08T02:00-08:00 to -07:00]
     * System.out.println(rules.getTransition(local3)); // Transition[Gap at 2020-03-08T02:00-08:00 to -07:00]
     * System.out.println(rules.getTransition(local4)); // null
     * System.out.println(rules.getTransition(local5)); // null
     * System.out.println(rules.getTransition(local6)); // Transition[Overlap at 2020-11-01T02:00-07:00 to -08:00]
     * System.out.println(rules.getTransition(local7)); // Transition[Overlap at 2020-11-01T02:00-07:00 to -08:00]
     * System.out.println(rules.getTransition(local8)); // null
     */
    
    
    /**
     * Gets the offset transition applicable at the specified local date-time in these rules.
     * <p>
     * The mapping from a local date-time to an offset is not straightforward.
     * There are three cases:
     * <ul>
     * <li>Normal, with one valid offset. For the vast majority of the year, the normal
     *  case applies, where there is a single valid offset for the local date-time.</li>
     * <li>Gap, with zero valid offsets. This is when clocks jump forward typically
     *  due to the spring daylight savings change from "winter" to "summer".
     *  In a gap there are local date-time values with no valid offset.</li>
     * <li>Overlap, with two valid offsets. This is when clocks are set back typically
     *  due to the autumn daylight savings change from "summer" to "winter".
     *  In an overlap there are local date-time values with two valid offsets.</li>
     * </ul>
     * A transition is used to model the cases of a Gap or Overlap.
     * The Normal case will return null.
     * <p>
     * There are various ways to handle the conversion from a {@code LocalDateTime}.
     * One technique, using this method, would be:
     * <pre>
     *  ZoneOffsetTransition trans = rules.getTransition(localDT);
     *  if (trans != null) {
     *    // Gap or Overlap: determine what to do from transition
     *  } else {
     *    // Normal case: only one valid offset
     *    zoneOffset = rule.getOffset(localDT);
     *  }
     * </pre>
     *
     * @param localDateTime the local date-time to query for offset transition, not null, but null
     *                      may be ignored if the rules have a single offset for all instants
     *
     * @return the offset transition, null if the local date-time is not in transition
     */
    /*
     * 获取当前时区规则集在localDateTime时刻的偏移转换规则，该规则用来指示如何切换时区的时间偏移。
     *
     * 只有在"间隙时间"和"重叠时间"中，需要用到偏移转换规则。
     * 在正常时间和夏令时时间内，无需用到偏移转换规则。
     *
     * localDateTime: 当前时区规则集所属时区的一个本地时间。
     */
    public ZoneOffsetTransition getTransition(LocalDateTime localDateTime) {
        Object info = getOffsetInfo(localDateTime);
        return (info instanceof ZoneOffsetTransition ? (ZoneOffsetTransition) info : null);
    }
    
    /**
     * Gets the list of transition rules for years beyond those defined in the transition list.
     * <p>
     * The complete set of transitions for this rules instance is defined by this method
     * and {@link #getTransitions()}. This method returns instances of {@link ZoneOffsetTransitionRule}
     * that define an algorithm for when transitions will occur.
     * <p>
     * For any given {@code ZoneRules}, this list contains the transition rules for years
     * beyond those years that have been fully defined. These rules typically refer to future
     * daylight saving time rule changes.
     * <p>
     * If the zone defines daylight savings into the future, then the list will normally
     * be of size two and hold information about entering and exiting daylight savings.
     * If the zone does not have daylight savings, or information about future changes
     * is uncertain, then the list will be empty.
     * <p>
     * The list will be empty for fixed offset rules and for any time-zone where there is no
     * daylight saving time. The list will also be empty if the transition rules are unknown.
     *
     * @return an immutable list of transition rules, not null
     */
    // 返回当前时区规则集中的[通用]偏移转换规则
    public List<ZoneOffsetTransitionRule> getTransitionRules() {
        return List.of(lastRules);
    }
    
    /**
     * Gets the complete list of fully defined transitions.
     * <p>
     * The complete set of transitions for this rules instance is defined by this method
     * and {@link #getTransitionRules()}. This method returns those transitions that have
     * been fully defined. These are typically historical, but may be in the future.
     * <p>
     * The list will be empty for fixed offset rules and for any time-zone where there has
     * only ever been a single offset. The list will also be empty if the transition rules are unknown.
     *
     * @return an immutable list of fully defined transitions, not null
     */
    // 返回当前时区规则集中的[历史]偏移转换规则
    public List<ZoneOffsetTransition> getTransitions() {
        List<ZoneOffsetTransition> list = new ArrayList<>();
        for(int i = 0; i<savingsInstantTransitions.length; i++) {
            list.add(new ZoneOffsetTransition(savingsInstantTransitions[i], wallOffsets[i], wallOffsets[i + 1]));
        }
        return Collections.unmodifiableList(list);
    }
    
    /**
     * Gets the next transition after the specified instant.
     * <p>
     * This returns details of the next transition after the specified instant.
     * For example, if the instant represents a point where "Summer" daylight savings time
     * applies, then the method will return the transition to the next "Winter" time.
     *
     * @param instant the instant to get the next transition after, not null, but null
     *                may be ignored if the rules have a single offset for all instants
     *
     * @return the next transition after the specified instant, null if this is after the last transition
     */
    /*
     * 返回当前时区规则集在紧邻instant时刻之后的偏移转换规则。
     * 该偏移转换规则可能是[历史]偏移转换规则，也可能是[通用]偏移转换规则。
     *
     * instant: 时间戳，用来表示UTC/GMT"零时区"的一个时间点。
     */
    public ZoneOffsetTransition nextTransition(Instant instant) {
        if(savingsInstantTransitions.length == 0) {
            return null;
        }
        
        long epochSec = instant.getEpochSecond();
        
        // check if using last rules
        if(epochSec >= savingsInstantTransitions[savingsInstantTransitions.length - 1]) {
            if(lastRules.length == 0) {
                return null;
            }
            
            // search year the instant is in
            int year = findYear(epochSec, wallOffsets[wallOffsets.length - 1]);
            ZoneOffsetTransition[] transArray = findTransitionArray(year);
            for(ZoneOffsetTransition trans : transArray) {
                if(epochSec<trans.toEpochSecond()) {
                    return trans;
                }
            }
            
            // use first from following year
            if(year<Year.MAX_VALUE) {
                transArray = findTransitionArray(year + 1);
                return transArray[0];
            }
            return null;
        }
        
        // using historic rules
        int index = Arrays.binarySearch(savingsInstantTransitions, epochSec);
        if(index<0) {
            index = -index - 1;  // switched value is the next transition
        } else {
            index += 1;  // exact match, so need to add one to get the next
        }
        
        return new ZoneOffsetTransition(savingsInstantTransitions[index], wallOffsets[index], wallOffsets[index + 1]);
    }
    
    /**
     * Gets the previous transition before the specified instant.
     * <p>
     * This returns details of the previous transition before the specified instant.
     * For example, if the instant represents a point where "summer" daylight saving time
     * applies, then the method will return the transition from the previous "winter" time.
     *
     * @param instant the instant to get the previous transition after, not null, but null
     *                may be ignored if the rules have a single offset for all instants
     *
     * @return the previous transition before the specified instant, null if this is before the first transition
     */
    /*
     * 返回当前时区规则集在紧邻instant时刻之前的偏移转换规则。
     * 该偏移转换规则可能是[历史]偏移转换规则，也可能是[通用]偏移转换规则。
     *
     * instant: 时间戳，用来表示UTC/GMT"零时区"的一个时间点。
     */
    public ZoneOffsetTransition previousTransition(Instant instant) {
        if(savingsInstantTransitions.length == 0) {
            return null;
        }
        long epochSec = instant.getEpochSecond();
        if(instant.getNano()>0 && epochSec<Long.MAX_VALUE) {
            epochSec += 1;  // allow rest of method to only use seconds
        }
        
        // check if using last rules
        long lastHistoric = savingsInstantTransitions[savingsInstantTransitions.length - 1];
        if(lastRules.length>0 && epochSec>lastHistoric) {
            // search year the instant is in
            ZoneOffset lastHistoricOffset = wallOffsets[wallOffsets.length - 1];
            int year = findYear(epochSec, lastHistoricOffset);
            ZoneOffsetTransition[] transArray = findTransitionArray(year);
            for(int i = transArray.length - 1; i >= 0; i--) {
                if(epochSec>transArray[i].toEpochSecond()) {
                    return transArray[i];
                }
            }
            
            // use last from preceding year
            int lastHistoricYear = findYear(lastHistoric, lastHistoricOffset);
            if(--year>lastHistoricYear) {
                transArray = findTransitionArray(year);
                return transArray[transArray.length - 1];
            }
            // drop through
        }
        
        // using historic rules
        int index = Arrays.binarySearch(savingsInstantTransitions, epochSec);
        if(index<0) {
            index = -index - 1;
        }
        
        if(index<=0) {
            return null;
        }
        
        return new ZoneOffsetTransition(savingsInstantTransitions[index - 1], wallOffsets[index - 1], wallOffsets[index]);
    }
    
    /*▲ 偏移转换规则 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 夏令时 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * // 美国洛杉矶的地理时区ID，其位于UTC时区的西八区(-8)
     * ZoneId zoneId = ZoneId.of("America/Los_Angeles");
     * // 获取当前时区对应的时区规则集
     * ZoneRules rules = zoneId.getRules();
     *
     * // 注：下面选取的时间点只精确到秒
     * Instant instant1 = LocalDateTime.of(2020, 3,  8, 9,  59).toInstant(ZoneOffset.UTC);
     * Instant instant2 = LocalDateTime.of(2020, 3,  8, 10,  0).toInstant(ZoneOffset.UTC);
     * Instant instant3 = LocalDateTime.of(2020, 11, 1, 8,  59).toInstant(ZoneOffset.UTC);
     * Instant instant4 = LocalDateTime.of(2020, 11, 1, 9,   0).toInstant(ZoneOffset.UTC);
     *
     * System.out.println("=========================================");
     * System.out.println(rules.getDaylightSavings(instant1)); // PT0S，当前时刻的时间无偏移
     * System.out.println(rules.getDaylightSavings(instant2)); // PT1H，当前时刻的时间相对于标准时区时间偏移了+1小时
     * System.out.println(rules.getDaylightSavings(instant3)); // PT1H，当前时刻的时间相对于标准时区时间偏移了+1小时
     * System.out.println(rules.getDaylightSavings(instant4)); // PT0S，当前时刻的时间无偏移
     *
     * System.out.println("=========================================");
     * System.out.println(rules.isDaylightSavings(instant1)); // false
     * System.out.println(rules.isDaylightSavings(instant2)); // true
     * System.out.println(rules.isDaylightSavings(instant3)); // true
     * System.out.println(rules.isDaylightSavings(instant4)); // false
     */
    
    /**
     * Gets the amount of daylight savings in use for the specified instant in this zone.
     * <p>
     * This provides access to historic information on how the amount of daylight
     * savings has changed over time.
     * This is the difference between the standard offset and the actual offset.
     * Typically the amount is zero during winter and one hour during summer.
     * Time-zones are second-based, so the nanosecond part of the duration will be zero.
     * <p>
     * This default implementation calculates the duration from the
     * {@link #getOffset(java.time.Instant) actual} and
     * {@link #getStandardOffset(java.time.Instant) standard} offsets.
     *
     * @param instant the instant to find the daylight savings for, not null, but null
     *                may be ignored if the rules have a single offset for all instants
     *
     * @return the difference between the standard and actual offset, not null
     */
    /*
     * 获取instant时刻在当前时区规则集中的时间偏移(即进入夏令时时间时，时钟需要拨快的时间量)。
     *
     * instant: 时间戳，用来表示UTC/GMT"零时区"的一个时间点。
     */
    public Duration getDaylightSavings(Instant instant) {
        if(savingsInstantTransitions.length == 0) {
            return Duration.ZERO;
        }
        
        ZoneOffset standardOffset = getStandardOffset(instant);
        ZoneOffset actualOffset = getOffset(instant);
        return Duration.ofSeconds(actualOffset.getTotalSeconds() - standardOffset.getTotalSeconds());
    }
    
    /**
     * Checks if the specified instant is in daylight savings.
     * <p>
     * This checks if the standard offset and the actual offset are the same
     * for the specified instant.
     * If they are not, it is assumed that daylight savings is in operation.
     * <p>
     * This default implementation compares the {@link #getOffset(java.time.Instant) actual}
     * and {@link #getStandardOffset(java.time.Instant) standard} offsets.
     *
     * @param instant the instant to find the offset information for, not null, but null
     *                may be ignored if the rules have a single offset for all instants
     *
     * @return the standard offset, not null
     */
    /*
     * 判断instant时刻是否位于当前时区规则集的"夏令时时间"中。
     *
     * instant: 时间戳，用来表示UTC/GMT"零时区"的一个时间点。
     */
    public boolean isDaylightSavings(Instant instant) {
        return !getStandardOffset(instant).equals(getOffset(instant));
    }
    
    /*▲ 夏令时 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    private Object getOffsetInfo(LocalDateTime dt) {
        if(savingsInstantTransitions.length == 0) {
            return standardOffsets[0];
        }
        
        // check if using last rules
        if(lastRules.length>0 && dt.isAfter(savingsLocalTransitions[savingsLocalTransitions.length - 1])) {
            ZoneOffsetTransition[] transArray = findTransitionArray(dt.getYear());
            Object info = null;
            for(ZoneOffsetTransition trans : transArray) {
                info = findOffsetInfo(dt, trans);
                if(info instanceof ZoneOffsetTransition || info.equals(trans.getOffsetBefore())) {
                    return info;
                }
            }
            return info;
        }
        
        // using historic rules
        int index = Arrays.binarySearch(savingsLocalTransitions, dt);
        if(index == -1) {
            // before first transition
            return wallOffsets[0];
        }
        
        if(index<0) {
            // switch negative insert position to start of matched range
            index = -index - 2;
        } else if(index<savingsLocalTransitions.length - 1 && savingsLocalTransitions[index].equals(savingsLocalTransitions[index + 1])) {
            // handle overlap immediately following gap
            index++;
        }
        
        if((index & 1) == 0) {
            // gap or overlap
            LocalDateTime dtBefore = savingsLocalTransitions[index];
            LocalDateTime dtAfter = savingsLocalTransitions[index + 1];
            ZoneOffset offsetBefore = wallOffsets[index / 2];
            ZoneOffset offsetAfter = wallOffsets[index / 2 + 1];
            if(offsetAfter.getTotalSeconds()>offsetBefore.getTotalSeconds()) {
                // gap
                return new ZoneOffsetTransition(dtBefore, offsetBefore, offsetAfter);
            } else {
                // overlap
                return new ZoneOffsetTransition(dtAfter, offsetBefore, offsetAfter);
            }
        } else {
            // normal (neither gap or overlap)
            return wallOffsets[index / 2 + 1];
        }
    }
    
    /**
     * Finds the offset info for a local date-time and transition.
     *
     * @param dt    the date-time, not null
     * @param trans the transition, not null
     *
     * @return the offset info, not null
     */
    private Object findOffsetInfo(LocalDateTime dt, ZoneOffsetTransition trans) {
        LocalDateTime localTransition = trans.getDateTimeBefore();
        
        if(trans.isGap()) {
            if(dt.isBefore(localTransition)) {
                return trans.getOffsetBefore();
            }
            if(dt.isBefore(trans.getDateTimeAfter())) {
                return trans;
            } else {
                return trans.getOffsetAfter();
            }
        } else {
            if(!dt.isBefore(localTransition)) {
                return trans.getOffsetAfter();
            }
            if(dt.isBefore(trans.getDateTimeAfter())) {
                return trans.getOffsetBefore();
            } else {
                return trans;
            }
        }
    }
    
    /**
     * Finds the appropriate transition array for the given year.
     *
     * @param year the year, not null
     *
     * @return the transition array, not null
     */
    private ZoneOffsetTransition[] findTransitionArray(int year) {
        Integer yearObj = year;  // should use Year class, but this saves a class load
        ZoneOffsetTransition[] transArray = lastRulesCache.get(yearObj);
        if(transArray != null) {
            return transArray;
        }
        
        ZoneOffsetTransitionRule[] ruleArray = lastRules;
        transArray = new ZoneOffsetTransition[ruleArray.length];
        for(int i = 0; i<ruleArray.length; i++) {
            transArray[i] = ruleArray[i].createTransition(year);
        }
        
        if(year<LAST_CACHED_YEAR) {
            lastRulesCache.putIfAbsent(yearObj, transArray);
        }
        
        return transArray;
    }
    
    private int findYear(long epochSecond, ZoneOffset offset) {
        // inline for performance
        long localSecond = epochSecond + offset.getTotalSeconds();
        long localEpochDay = Math.floorDiv(localSecond, 86400);
        return LocalDate.ofEpochDay(localEpochDay).getYear();
    }
    
    
    /**
     * Returns a string describing this object.
     *
     * @return a string for debugging, not null
     */
    @Override
    public String toString() {
        return "ZoneRules[currentStandardOffset=" + standardOffsets[standardOffsets.length - 1] + "]";
    }
    
    /**
     * Checks if this set of rules equals another.
     * <p>
     * Two rule sets are equal if they will always result in the same output
     * for any given input instant or local date-time.
     * Rules from two different groups may return false even if they are in fact the same.
     * <p>
     * This definition should result in implementations comparing their entire state.
     *
     * @param otherRules the other rules, null returns false
     *
     * @return true if this rules is the same as that specified
     */
    @Override
    public boolean equals(Object otherRules) {
        if(this == otherRules) {
            return true;
        }
        if(otherRules instanceof ZoneRules) {
            ZoneRules other = (ZoneRules) otherRules;
            return Arrays.equals(standardTransitions, other.standardTransitions) && Arrays.equals(standardOffsets, other.standardOffsets) && Arrays.equals(savingsInstantTransitions, other.savingsInstantTransitions) && Arrays.equals(wallOffsets, other.wallOffsets) && Arrays.equals(lastRules, other.lastRules);
        }
        return false;
    }
    
    /**
     * Returns a suitable hash code given the definition of {@code #equals}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(standardTransitions) ^ Arrays.hashCode(standardOffsets) ^ Arrays.hashCode(savingsInstantTransitions) ^ Arrays.hashCode(wallOffsets) ^ Arrays.hashCode(lastRules);
    }
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 3044319355680032515L;
    
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
     * Writes the state to the stream.
     *
     * @param out the output stream, not null
     *
     * @throws IOException if an error occurs
     */
    void writeExternal(DataOutput out) throws IOException {
        out.writeInt(standardTransitions.length);
        for(long trans : standardTransitions) {
            Ser.writeEpochSec(trans, out);
        }
        for(ZoneOffset offset : standardOffsets) {
            Ser.writeOffset(offset, out);
        }
        out.writeInt(savingsInstantTransitions.length);
        for(long trans : savingsInstantTransitions) {
            Ser.writeEpochSec(trans, out);
        }
        for(ZoneOffset offset : wallOffsets) {
            Ser.writeOffset(offset, out);
        }
        out.writeByte(lastRules.length);
        for(ZoneOffsetTransitionRule rule : lastRules) {
            rule.writeExternal(out);
        }
    }
    
    /**
     * Reads the state from the stream.
     *
     * @param in the input stream, not null
     *
     * @return the created object, not null
     *
     * @throws IOException if an error occurs
     */
    static ZoneRules readExternal(DataInput in) throws IOException, ClassNotFoundException {
        int stdSize = in.readInt();
        long[] stdTrans = (stdSize == 0) ? EMPTY_LONG_ARRAY : new long[stdSize];
        for(int i = 0; i<stdSize; i++) {
            stdTrans[i] = Ser.readEpochSec(in);
        }
        ZoneOffset[] stdOffsets = new ZoneOffset[stdSize + 1];
        for(int i = 0; i<stdOffsets.length; i++) {
            stdOffsets[i] = Ser.readOffset(in);
        }
        int savSize = in.readInt();
        long[] savTrans = (savSize == 0) ? EMPTY_LONG_ARRAY : new long[savSize];
        for(int i = 0; i<savSize; i++) {
            savTrans[i] = Ser.readEpochSec(in);
        }
        ZoneOffset[] savOffsets = new ZoneOffset[savSize + 1];
        for(int i = 0; i<savOffsets.length; i++) {
            savOffsets[i] = Ser.readOffset(in);
        }
        int ruleSize = in.readByte();
        ZoneOffsetTransitionRule[] rules = (ruleSize == 0) ? EMPTY_LASTRULES : new ZoneOffsetTransitionRule[ruleSize];
        for(int i = 0; i<ruleSize; i++) {
            rules[i] = ZoneOffsetTransitionRule.readExternal(in);
        }
        return new ZoneRules(stdTrans, stdOffsets, savTrans, savOffsets, rules);
    }
    
    /**
     * Writes the object using a
     * <a href="../../../serialized-form.html#java.time.zone.Ser">dedicated serialized form</a>.
     *
     * @return the replacing object, not null
     *
     * @serialData <pre style="font-size:1.0em">{@code
     *
     *   out.writeByte(1);  // identifies a ZoneRules
     *   out.writeInt(standardTransitions.length);
     *   for (long trans : standardTransitions) {
     *       Ser.writeEpochSec(trans, out);
     *   }
     *   for (ZoneOffset offset : standardOffsets) {
     *       Ser.writeOffset(offset, out);
     *   }
     *   out.writeInt(savingsInstantTransitions.length);
     *   for (long trans : savingsInstantTransitions) {
     *       Ser.writeEpochSec(trans, out);
     *   }
     *   for (ZoneOffset offset : wallOffsets) {
     *       Ser.writeOffset(offset, out);
     *   }
     *   out.writeByte(lastRules.length);
     *   for (ZoneOffsetTransitionRule rule : lastRules) {
     *       rule.writeExternal(out);
     *   }
     * }
     * </pre>
     * <p>
     * Epoch second values used for offsets are encoded in a variable
     * length form to make the common cases put fewer bytes in the stream.
     * <pre style="font-size:1.0em">{@code
     *
     *  static void writeEpochSec(long epochSec, DataOutput out) throws IOException {
     *     if (epochSec >= -4575744000L && epochSec < 10413792000L && epochSec % 900 == 0) {  // quarter hours between 1825 and 2300
     *         int store = (int) ((epochSec + 4575744000L) / 900);
     *         out.writeByte((store >>> 16) & 255);
     *         out.writeByte((store >>> 8) & 255);
     *         out.writeByte(store & 255);
     *      } else {
     *          out.writeByte(255);
     *          out.writeLong(epochSec);
     *      }
     *  }
     * }
     * </pre>
     * <p>
     * ZoneOffset values are encoded in a variable length form so the
     * common cases put fewer bytes in the stream.
     * <pre style="font-size:1.0em">{@code
     *
     *  static void writeOffset(ZoneOffset offset, DataOutput out) throws IOException {
     *     final int offsetSecs = offset.getTotalSeconds();
     *     int offsetByte = offsetSecs % 900 == 0 ? offsetSecs / 900 : 127;  // compress to -72 to +72
     *     out.writeByte(offsetByte);
     *     if (offsetByte == 127) {
     *         out.writeInt(offsetSecs);
     *     }
     * }
     * }
     * </pre>
     */
    private Object writeReplace() {
        return new Ser(Ser.ZRULES, this);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
