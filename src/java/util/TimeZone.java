/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.util;

import java.io.Serializable;
import java.time.ZoneId;

import jdk.internal.util.StaticProperty;
import sun.security.action.GetPropertyAction;
import sun.util.calendar.ZoneInfo;
import sun.util.calendar.ZoneInfoFile;
import sun.util.locale.provider.TimeZoneNameUtility;

/**
 * <code>TimeZone</code> represents a time zone offset, and also figures out daylight
 * savings.
 *
 * <p>
 * Typically, you get a <code>TimeZone</code> using <code>getDefault</code>
 * which creates a <code>TimeZone</code> based on the time zone where the program
 * is running. For example, for a program running in Japan, <code>getDefault</code>
 * creates a <code>TimeZone</code> object based on Japanese Standard Time.
 *
 * <p>
 * You can also get a <code>TimeZone</code> using <code>getTimeZone</code>
 * along with a time zone ID. For instance, the time zone ID for the
 * U.S. Pacific Time zone is "America/Los_Angeles". So, you can get a
 * U.S. Pacific Time <code>TimeZone</code> object with:
 * <blockquote><pre>
 * TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
 * </pre></blockquote>
 * You can use the <code>getAvailableIDs</code> method to iterate through
 * all the supported time zone IDs. You can then choose a
 * supported ID to get a <code>TimeZone</code>.
 * If the time zone you want is not represented by one of the
 * supported IDs, then a custom time zone ID can be specified to
 * produce a TimeZone. The syntax of a custom time zone ID is:
 *
 * <blockquote><pre>
 * <a id="CustomID"><i>CustomID:</i></a>
 *         <code>GMT</code> <i>Sign</i> <i>Hours</i> <code>:</code> <i>Minutes</i>
 *         <code>GMT</code> <i>Sign</i> <i>Hours</i> <i>Minutes</i>
 *         <code>GMT</code> <i>Sign</i> <i>Hours</i>
 * <i>Sign:</i> one of
 *         <code>+ -</code>
 * <i>Hours:</i>
 *         <i>Digit</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Minutes:</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Digit:</i> one of
 *         <code>0 1 2 3 4 5 6 7 8 9</code>
 * </pre></blockquote>
 *
 * <i>Hours</i> must be between 0 to 23 and <i>Minutes</i> must be
 * between 00 to 59.  For example, "GMT+10" and "GMT+0010" mean ten
 * hours and ten minutes ahead of GMT, respectively.
 * <p>
 * The format is locale independent and digits must be taken from the
 * Basic Latin block of the Unicode standard. No daylight saving time
 * transition schedule can be specified with a custom time zone ID. If
 * the specified string doesn't match the syntax, <code>"GMT"</code>
 * is used.
 * <p>
 * When creating a <code>TimeZone</code>, the specified custom time
 * zone ID is normalized in the following syntax:
 * <blockquote><pre>
 * <a id="NormalizedCustomID"><i>NormalizedCustomID:</i></a>
 *         <code>GMT</code> <i>Sign</i> <i>TwoDigitHours</i> <code>:</code> <i>Minutes</i>
 * <i>Sign:</i> one of
 *         <code>+ -</code>
 * <i>TwoDigitHours:</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Minutes:</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Digit:</i> one of
 *         <code>0 1 2 3 4 5 6 7 8 9</code>
 * </pre></blockquote>
 * For example, TimeZone.getTimeZone("GMT-8").getID() returns "GMT-08:00".
 *
 * <h3>Three-letter time zone IDs</h3>
 *
 * For compatibility with JDK 1.1.x, some other three-letter time zone IDs
 * (such as "PST", "CTT", "AST") are also supported. However, <strong>their
 * use is deprecated</strong> because the same abbreviation is often used
 * for multiple time zones (for example, "CST" could be U.S. "Central Standard
 * Time" and "China Standard Time"), and the Java platform can then only
 * recognize one of them.
 *
 *
 * @see          Calendar
 * @see          GregorianCalendar
 * @see          SimpleTimeZone
 * @author Mark Davis, David Goldsmith, Chen-Lieh Huang, Alan Liu
 * @since 1.1
 */
/*
 * 时区，应用于JDK8之前
 *
 * 注：JDK8之后，应当使用ZoneId替代TimeZone
 */
public abstract class TimeZone implements Serializable, Cloneable {
    // Proclaim serialization compatibility with JDK 1.1
    static final long serialVersionUID = 3581463369166924961L;
    
    /**
     * A style specifier for <code>getDisplayName()</code> indicating
     * a short name, such as "PST."
     *
     * @see #LONG
     * @since 1.2
     */
    // 时区名称显示用短格式
    public static final int SHORT = 0;
    
    /**
     * A style specifier for <code>getDisplayName()</code> indicating
     * a long name, such as "Pacific Standard Time."
     * @see #SHORT
     * @since 1.2
     */
    // 时区名称显示用长格式
    public static final int LONG = 1;
    
    // Constants used internally; unit is milliseconds
    private static final int ONE_MINUTE = 60 * 1000;
    private static final int ONE_HOUR = 60 * ONE_MINUTE;
    private static final int ONE_DAY = 24 * ONE_HOUR;
    
    static final String GMT_ID = "GMT";
    private static final int GMT_ID_LENGTH = 3;
    
    /**
     * The null constant as a TimeZone.
     */
    static final TimeZone NO_TIMEZONE = null;
    
    // 系统默认时区
    private static volatile TimeZone defaultTimeZone;
    
    /**
     * The string identifier of this <code>TimeZone</code>.  This is a
     * programmatic identifier used internally to look up <code>TimeZone</code>
     * objects from the system table and also to map them to their localized
     * display names.  <code>ID</code> values are unique in the system
     * table but may not be for dynamically created zones.
     *
     * @serial
     */
    // 时区ID的字符串表示
    private String ID;
    
    /**
     * Cached {@link ZoneId} for this TimeZone
     */
    // 时区ID，相当于TimeZone的另一种表示
    private transient ZoneId zoneId;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically implicit.)
     */
    public TimeZone() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the default {@code TimeZone} of the Java virtual machine. If the
     * cached default {@code TimeZone} is available, its clone is returned.
     * Otherwise, the method takes the following steps to determine the default
     * time zone.
     *
     * <ul>
     * <li>Use the {@code user.timezone} property value as the default
     * time zone ID if it's available.</li>
     * <li>Detect the platform time zone ID. The source of the
     * platform time zone and ID mapping may vary with implementation.</li>
     * <li>Use {@code GMT} as the last resort if the given or detected
     * time zone ID is unknown.</li>
     * </ul>
     *
     * <p>The default {@code TimeZone} created from the ID is cached,
     * and its clone is returned. The {@code user.timezone} property
     * value is set to the ID upon return.
     *
     * @return the default {@code TimeZone}
     *
     * @see #setDefault(TimeZone)
     */
    // 返回系统默认时区(的克隆)
    public static TimeZone getDefault() {
        return (TimeZone) getDefaultRef().clone();
    }
    
    /**
     * Gets the available IDs according to the given time zone offset in milliseconds.
     *
     * @param rawOffset the given time zone GMT offset in milliseconds.
     *
     * @return an array of IDs, where the time zone for that ID has
     * the specified GMT offset. For example, "America/Phoenix" and "America/Denver"
     * both have GMT-07:00, but differ in daylight saving behavior.
     *
     * @see #getRawOffset()
     */
    /*
     * 返回在指定的时区偏移(以毫秒为单位)下可用的时区ID。
     *
     * rawOffset: 代表一个与UTC/GMT时区的时间偏移。
     *
     * 例如，当rawOffset为28800000时，将返回以下时区ID：
     * Antarctica/Casey
     * Asia/Brunei
     * Asia/Choibalsan
     * Asia/Chongqing
     * Asia/Chungking
     * Asia/Harbin
     * Asia/Hong_Kong
     * Asia/Irkutsk
     * Asia/Kuala_Lumpur
     * Asia/Kuching
     * Asia/Macao
     * Asia/Macau
     * Asia/Makassar
     * Asia/Manila
     * Asia/Shanghai
     * Asia/Singapore
     * Asia/Taipei
     * Asia/Ujung_Pandang
     * Asia/Ulaanbaatar
     * Asia/Ulan_Bator
     * Australia/Perth
     * Australia/West
     * CTT
     * Etc/GMT-8
     * Hongkong
     * PRC
     * Singapore
     */
    public static synchronized String[] getAvailableIDs(int rawOffset) {
        return ZoneInfo.getAvailableIDs(rawOffset);
    }
    
    /**
     * Gets all the available IDs supported.
     *
     * @return an array of IDs.
     */
    // 返回所有可用的时区ID
    public static synchronized String[] getAvailableIDs() {
        return ZoneInfo.getAvailableIDs();
    }
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the <code>TimeZone</code> for the given ID.
     *
     * @param ID the ID for a <code>TimeZone</code>, either an abbreviation
     *           such as "PST", a full name such as "America/Los_Angeles", or a custom
     *           ID such as "GMT-8:00". Note that the support of abbreviations is
     *           for JDK 1.1.x compatibility only and full names should be used.
     *
     * @return the specified <code>TimeZone</code>, or the GMT zone if the given ID
     * cannot be understood.
     */
    // 返回给定的时区ID对应的时区；对于无法识别的ID，则回退为GMT时区
    public static synchronized TimeZone getTimeZone(String ID) {
        return getTimeZone(ID, true);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ 转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Converts this {@code TimeZone} object to a {@code ZoneId}.
     *
     * @return a {@code ZoneId} representing the same time zone as this
     * {@code TimeZone}
     *
     * @since 1.8
     */
    // 将TimeZone转换为ZoneId
    public ZoneId toZoneId() {
        ZoneId zId = zoneId;
        if(zId == null) {
            zoneId = zId = toZoneId0();
        }
        
        return zId;
    }
    
    /**
     * Gets the {@code TimeZone} for the given {@code zoneId}.
     *
     * @param zoneId a {@link ZoneId} from which the time zone ID is obtained
     *
     * @return the specified {@code TimeZone}, or the GMT zone if the given ID
     * cannot be understood.
     *
     * @throws NullPointerException if {@code zoneId} is {@code null}
     * @since 1.8
     */
    // 将ZoneId转换为TimeZone
    public static TimeZone getTimeZone(ZoneId zoneId) {
        // 获取时区ID的字符串形式，该字符串是唯一的
        String ID = zoneId.getId();
        
        char c = ID.charAt(0);
        if(c == '+' || c == '-') {
            ID = "GMT" + ID;
        } else if(c == 'Z' && ID.length() == 1) {
            ID = "UTC";
        }
        
        return getTimeZone(ID, true);
    }
    
    /*▲ 转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时区偏移 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the offset of this time zone from UTC at the specified
     * date. If Daylight Saving Time is in effect at the specified
     * date, the offset value is adjusted with the amount of daylight
     * saving.
     * <p>
     * This method returns a historically correct offset value if an
     * underlying TimeZone implementation subclass supports historical
     * Daylight Saving Time schedule and GMT offset changes.
     *
     * @param date the date represented in milliseconds since January 1, 1970 00:00:00 GMT
     *
     * @return the amount of time in milliseconds to add to UTC to get local time.
     *
     * @see Calendar#ZONE_OFFSET
     * @see Calendar#DST_OFFSET
     * @since 1.4
     */
    /*
     * 返回日期date在当前时区下的"实际偏移"毫秒数；这里可以返回一个准确的"实际偏移"。
     *
     * date: UTC/GMT时区的纪元毫秒
     *
     * 注：该方法与ZoneRules#getOffset(Instant)是等效的
     *
     *
     * 举例：
     *
     * // "America/Los_Angeles"的标准时区偏移是"-8"小时
     * TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
     *
     * // 初始化UTC/GMT时区的纪元毫秒
     * long epochMillisecond1 = LocalDateTime.of(2020, 3,  8,  9, 59).toEpochSecond(ZoneOffset.UTC)*1000;
     * long epochMillisecond2 = LocalDateTime.of(2020, 3,  8, 10,  0).toEpochSecond(ZoneOffset.UTC)*1000;
     * long epochMillisecond3 = LocalDateTime.of(2020, 11, 1,  8, 59).toEpochSecond(ZoneOffset.UTC)*1000;
     * long epochMillisecond4 = LocalDateTime.of(2020, 11, 1,  9,  0).toEpochSecond(ZoneOffset.UTC)*1000;
     * System.out.println("=========================================");
     * System.out.println(timeZone.getOffset(epochMillisecond1)); // -28800000毫秒，即-8小时
     * System.out.println(timeZone.getOffset(epochMillisecond2)); // -25200000毫秒，即-7小时
     * System.out.println(timeZone.getOffset(epochMillisecond3)); // -25200000毫秒，即-7小时
     * System.out.println(timeZone.getOffset(epochMillisecond4)); // -28800000毫秒，即-8小时
     */
    public int getOffset(long date) {
        if(inDaylightTime(new Date(date))) {
            return getRawOffset() + getDSTSavings();
        }
        
        return getRawOffset();
    }
    
    /**
     * Gets the time zone offset, for current date, modified in case of
     * daylight savings. This is the offset to add to UTC to get local time.
     * <p>
     * This method returns a historically correct offset if an
     * underlying <code>TimeZone</code> implementation subclass
     * supports historical Daylight Saving Time schedule and GMT
     * offset changes.
     *
     * @param era          the era of the given date.
     * @param year         the year in the given date.
     * @param month        the month in the given date.
     *                     Month is 0-based. e.g., 0 for January.
     * @param day          the day-in-month of the given date.
     * @param dayOfWeek    the day-of-week of the given date.
     * @param milliseconds the milliseconds in day in <em>standard</em> local time.
     *
     * @return the offset in milliseconds to add to GMT to get local time.
     *
     * @see Calendar#ZONE_OFFSET
     * @see Calendar#DST_OFFSET
     */
    /*
     * 由当前时区的"标准偏移日期"，计算此刻UTC/GMT时区的纪元毫秒date，
     * 最后再计算该date在当前时区下的"实际偏移"毫秒数。
     *
     * era         : ISO纪元；在公历中，0是公元前，1是公元(后)
     * year        : ISO年份
     * month       : 一年中的第几月(一月为0)
     * day         : 一月中的第几天
     * dayOfWeek   : 一周中的第几天，有效值为1到7，分别代表周日到周六
     *               目前的实现中，并没有校验这个值与日期是否匹配，
     *               因此，这相当于一个没有用到的参数。
     * milliseconds: 当前时间(用毫秒表示的时-分-秒)
     *
     * 注：这个方法本质上是对上面getOffset(int)方法的包装，但是这个方法设计的很别扭。
     *
     *
     * 举例：
     *
     * // "America/Los_Angeles"的标准时区偏移是"-8"小时
     * TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
     *
     * // 这里给出的是"-8"区的标准偏移毫秒，后续会结合日期将其转换为UTC/GMT时区的纪元毫秒
     * long millis1 = LocalTime.of(1,  59).toSecondOfDay()*1000;
     * long millis2 = LocalTime.of(2,   0).toSecondOfDay()*1000;
     * long millis3 = LocalTime.of(0,  59).toSecondOfDay()*1000;
     * long millis4 = LocalTime.of(1,   0).toSecondOfDay()*1000;
     * System.out.println("=========================================");
     * System.out.println(timeZone.getOffset(1, 2020, Calendar.MARCH,    8, 1, (int) millis1)); // -28800000毫秒，即-8小时
     * System.out.println(timeZone.getOffset(1, 2020, Calendar.MARCH,    8, 1, (int) millis2)); // -25200000毫秒，即-7小时
     * System.out.println(timeZone.getOffset(1, 2020, Calendar.NOVEMBER, 1, 1, (int) millis3)); // -25200000毫秒，即-7小时
     * System.out.println(timeZone.getOffset(1, 2020, Calendar.NOVEMBER, 1, 1, (int) millis4)); // -28800000毫秒，即-8小时
     */
    public abstract int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds);
    
    /**
     * Returns the amount of time in milliseconds to add to UTC to get
     * standard time in this time zone. Because this value is not
     * affected by daylight saving time, it is called <I>raw
     * offset</I>.
     * <p>
     * If an underlying <code>TimeZone</code> implementation subclass
     * supports historical GMT offset changes, the method returns the
     * raw offset value of the current date. In Honolulu, for example,
     * its raw offset changed from GMT-10:30 to GMT-10:00 in 1947, and
     * this method always returns -36000000 milliseconds (i.e., -10
     * hours).
     *
     * @return the amount of raw offset time in milliseconds to add to UTC.
     *
     * @see Calendar#ZONE_OFFSET
     */
    /*
     * 返回当前时区的"标准偏移"。
     *
     * 注："标准偏移"通常是固定的，该偏移与被测试地区所在时区的标准偏移是一致的。
     */
    public abstract int getRawOffset();
    
    /**
     * Sets the base time zone offset to GMT.
     * This is the offset to add to UTC to get local time.
     * <p>
     * If an underlying <code>TimeZone</code> implementation subclass
     * supports historical GMT offset changes, the specified GMT
     * offset is set as the latest GMT offset and the difference from
     * the known latest GMT offset value is used to adjust all
     * historical GMT offset values.
     *
     * @param offsetMillis the given base time zone offset to GMT.
     */
    // 更新当前时区的"标准偏移"
    public abstract void setRawOffset(int offsetMillis);
    
    /*▲ 时区偏移 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 夏令时 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Queries if this {@code TimeZone} uses Daylight Saving Time.
     *
     * <p>If an underlying {@code TimeZone} implementation subclass
     * supports historical and future Daylight Saving Time schedule
     * changes, this method refers to the last known Daylight Saving Time
     * rule that can be a future prediction and may not be the same as
     * the current rule. Consider calling {@link #observesDaylightTime()}
     * if the current rule should also be taken into account.
     *
     * @return {@code true} if this {@code TimeZone} uses Daylight Saving Time,
     * {@code false}, otherwise.
     *
     * @see #inDaylightTime(Date)
     * @see Calendar#DST_OFFSET
     */
    // 判断当前时区是否使用了夏令时
    public abstract boolean useDaylightTime();
    
    /**
     * Returns the amount of time to be added to local standard time
     * to get local wall clock time.
     *
     * <p>The default implementation returns 3600000 milliseconds
     * (i.e., one hour) if a call to {@link #useDaylightTime()}
     * returns {@code true}. Otherwise, 0 (zero) is returned.
     *
     * <p>If an underlying {@code TimeZone} implementation subclass
     * supports historical and future Daylight Saving Time schedule
     * changes, this method returns the amount of saving time of the
     * last known Daylight Saving Time rule that can be a future
     * prediction.
     *
     * <p>If the amount of saving time at any given time stamp is
     * required, construct a {@link Calendar} with this {@code
     * TimeZone} and the time stamp, and call {@link Calendar#get(int)
     * Calendar.get}{@code (}{@link Calendar#DST_OFFSET}{@code )}.
     *
     * @return the amount of saving time in milliseconds
     *
     * @see #inDaylightTime(Date)
     * @see #getOffset(long)
     * @see #getOffset(int, int, int, int, int, int)
     * @see Calendar#ZONE_OFFSET
     * @since 1.4
     */
    // 返回进入夏令时时间时，时钟需要拨快的毫秒数
    public int getDSTSavings() {
        if(useDaylightTime()) {
            return 3600000;
        }
        
        return 0;
    }
    
    /**
     * Queries if the given {@code date} is in Daylight Saving Time in
     * this time zone.
     *
     * @param date the given Date.
     *
     * @return {@code true} if the given date is in Daylight Saving Time,
     * {@code false}, otherwise.
     */
    /**
     * 判断在给定日期(属于系统本地时区，而不是属于当前时区)这个时刻，当前时区是否处于夏令时中。
     *
     * 首先，需要将给定的日期转换到UTC时区下的日期，
     * 然后再判断在某一刻的UTC时间点下，当前时区是否处于夏令时中。
     *
     * 举例：
     *
     * // "America/Los_Angeles"的标准时区偏移是"-8"小时
     * TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
     *
     * // 这里给出的是"+8"区的日期
     * Date date1 = new Date(2020 - 1900, Calendar.MARCH,    8, 17, 59); // false，UTC时间3/8/09:59
     * Date date2 = new Date(2020 - 1900, Calendar.MARCH,    8, 18,  0); // true ，UTC时间3/8/10:00
     * Date date3 = new Date(2020 - 1900, Calendar.NOVEMBER, 1, 16, 59); // true ，UTC时间3/8/08:59
     * Date date4 = new Date(2020 - 1900, Calendar.NOVEMBER, 1, 17,  0); // false，UTC时间3/8/09:00
     * System.out.println("=========================================");
     * System.out.println(timeZone.inDaylightTime(date1));
     * System.out.println(timeZone.inDaylightTime(date2));
     * System.out.println(timeZone.inDaylightTime(date3));
     * System.out.println(timeZone.inDaylightTime(date4));
     *
     * 注：这个方法的设计太糟糕，建议使用ZoneRules#isDaylightSavings(Instant)替代
     */
    public abstract boolean inDaylightTime(Date date);
    
    /**
     * Returns {@code true} if this {@code TimeZone} is currently in
     * Daylight Saving Time, or if a transition from Standard Time to
     * Daylight Saving Time occurs at any future time.
     *
     * <p>The default implementation returns {@code true} if
     * {@code useDaylightTime()} or {@code inDaylightTime(new Date())}
     * returns {@code true}.
     *
     * @return {@code true} if this {@code TimeZone} is currently in
     * Daylight Saving Time, or if a transition from Standard Time to
     * Daylight Saving Time occurs at any future time; {@code false}
     * otherwise.
     *
     * @see #useDaylightTime()
     * @see #inDaylightTime(Date)
     * @see Calendar#DST_OFFSET
     * @since 1.7
     */
    // 判断当前时区是否使用了夏令时，或者处于系统本地时区的当前时刻下，当前时区是否处于夏令时中
    public boolean observesDaylightTime() {
        return useDaylightTime() || inDaylightTime(new Date());
    }
    
    /*▲ 夏令时 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 时区名称 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a long standard time name of this {@code TimeZone} suitable for
     * presentation to the user in the default locale.
     *
     * <p>This method is equivalent to:
     * <blockquote><pre>
     * getDisplayName(false, {@link #LONG},
     *                Locale.getDefault({@link Locale.Category#DISPLAY}))
     * </pre></blockquote>
     *
     * @return the human-readable name of this time zone in the default locale.
     *
     * @see #getDisplayName(boolean, int, Locale)
     * @see Locale#getDefault(Locale.Category)
     * @see Locale.Category
     * @since 1.2
     */
    //
    public final String getDisplayName() {
        return getDisplayName(false, LONG, Locale.getDefault(Locale.Category.DISPLAY));
    }
    
    /**
     * Returns a long standard time name of this {@code TimeZone} suitable for
     * presentation to the user in the specified {@code locale}.
     *
     * <p>This method is equivalent to:
     * <blockquote><pre>
     * getDisplayName(false, {@link #LONG}, locale)
     * </pre></blockquote>
     *
     * @param locale the locale in which to supply the display name.
     *
     * @return the human-readable name of this time zone in the given locale.
     *
     * @throws NullPointerException if {@code locale} is {@code null}.
     * @see #getDisplayName(boolean, int, Locale)
     * @since 1.2
     */
    // 返回当前时区在指定区域的名称
    public final String getDisplayName(Locale locale) {
        return getDisplayName(false, LONG, locale);
    }
    
    /**
     * Returns a name in the specified {@code style} of this {@code TimeZone}
     * suitable for presentation to the user in the default locale. If the
     * specified {@code daylight} is {@code true}, a Daylight Saving Time name
     * is returned (even if this {@code TimeZone} doesn't observe Daylight Saving
     * Time). Otherwise, a Standard Time name is returned.
     *
     * <p>This method is equivalent to:
     * <blockquote><pre>
     * getDisplayName(daylight, style,
     *                Locale.getDefault({@link Locale.Category#DISPLAY}))
     * </pre></blockquote>
     *
     * @param daylight {@code true} specifying a Daylight Saving Time name, or
     *                 {@code false} specifying a Standard Time name
     * @param style    either {@link #LONG} or {@link #SHORT}
     *
     * @return the human-readable name of this time zone in the default locale.
     *
     * @throws IllegalArgumentException if {@code style} is invalid.
     * @see #getDisplayName(boolean, int, Locale)
     * @see Locale#getDefault(Locale.Category)
     * @see Locale.Category
     * @see java.text.DateFormatSymbols#getZoneStrings()
     * @since 1.2
     */
    /*
     * 返回当前时区的本地化名称
     *
     * daylight: 如果为true，则显示当前时区是否位于夏令时中，否则，只显示标准名称
     * style   : 时区名称的显示用长格式还是短格式
     */
    public final String getDisplayName(boolean daylight, int style) {
        return getDisplayName(daylight, style, Locale.getDefault(Locale.Category.DISPLAY));
    }
    
    /**
     * Returns a name in the specified {@code style} of this {@code TimeZone}
     * suitable for presentation to the user in the specified {@code
     * locale}. If the specified {@code daylight} is {@code true}, a Daylight
     * Saving Time name is returned (even if this {@code TimeZone} doesn't
     * observe Daylight Saving Time). Otherwise, a Standard Time name is
     * returned.
     *
     * <p>When looking up a time zone name, the {@linkplain
     * ResourceBundle.Control#getCandidateLocales(String, Locale) default
     * <code>Locale</code> search path of <code>ResourceBundle</code>} derived
     * from the specified {@code locale} is used. (No {@linkplain
     * ResourceBundle.Control#getFallbackLocale(String, Locale) fallback
     * <code>Locale</code>} search is performed.) If a time zone name in any
     * {@code Locale} of the search path, including {@link Locale#ROOT}, is
     * found, the name is returned. Otherwise, a string in the
     * <a href="#NormalizedCustomID">normalized custom ID format</a> is returned.
     *
     * @param daylight {@code true} specifying a Daylight Saving Time name, or
     *                 {@code false} specifying a Standard Time name
     * @param style    either {@link #LONG} or {@link #SHORT}
     * @param locale   the locale in which to supply the display name.
     *
     * @return the human-readable name of this time zone in the given locale.
     *
     * @throws IllegalArgumentException if {@code style} is invalid.
     * @throws NullPointerException     if {@code locale} is {@code null}.
     * @see java.text.DateFormatSymbols#getZoneStrings()
     * @since 1.2
     */
    /*
     * 返回当前时区在指定区域的名称
     *
     * daylight: 如果为true，则显示当前时区是否位于夏令时中，否则，只显示标准名称
     * style   : 时区名称的显示用长格式还是短格式
     * locale  : 显示区域名称的区域
     */
    public String getDisplayName(boolean daylight, int style, Locale locale) {
        if(style != SHORT && style != LONG) {
            throw new IllegalArgumentException("Illegal style: " + style);
        }
    
        String id = getID();
        String name = TimeZoneNameUtility.retrieveDisplayName(id, daylight, style, locale);
        if(name != null) {
            return name;
        }
    
        if(id.startsWith("GMT") && id.length()>3) {
            char sign = id.charAt(3);
            if(sign == '+' || sign == '-') {
                return id;
            }
        }
        int offset = getRawOffset();
        if(daylight) {
            offset += getDSTSavings();
        }
    
        return ZoneInfoFile.toCustomID(offset);
    }
    
    /*▲ 时区名称 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Gets the ID of this time zone.
     *
     * @return the ID of this time zone.
     */
    // 返回时区ID的字符串表示
    public String getID() {
        return ID;
    }
    
    /**
     * Sets the time zone ID. This does not change any other data in
     * the time zone object.
     *
     * @param ID the new time zone ID.
     */
    // 设置时区ID的字符串表示
    public void setID(String ID) {
        if(ID == null) {
            throw new NullPointerException();
        }
        this.ID = ID;
        this.zoneId = null;   // invalidate cache
    }
    
    /**
     * Sets the {@code TimeZone} that is returned by the {@code getDefault}
     * method. {@code zone} is cached. If {@code zone} is null, the cached
     * default {@code TimeZone} is cleared. This method doesn't change the value
     * of the {@code user.timezone} property.
     *
     * @param zone the new default {@code TimeZone}, or null
     *
     * @throws SecurityException if the security manager's {@code checkPermission}
     *                           denies {@code PropertyPermission("user.timezone",
     *                           "write")}
     * @see #getDefault
     * @see PropertyPermission
     */
    // 设置默认时区
    public static void setDefault(TimeZone zone) {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new PropertyPermission("user.timezone", "write"));
        }
        
        /*
         * by saving a defensive clone and returning a clone in getDefault() too,
         * the defaultTimeZone instance is isolated from user code which makes it
         * effectively immutable. This is important to avoid races when the
         * following is evaluated in ZoneId.systemDefault(): TimeZone.getDefault().toZoneId().
         */
        defaultTimeZone = (zone == null) ? null : (TimeZone) zone.clone();
    }
    
    /**
     * Returns true if this zone has the same rule and offset as another zone.
     * That is, if this zone differs only in ID, if at all.  Returns false
     * if the other zone is null.
     *
     * @param other the <code>TimeZone</code> object to be compared with
     *
     * @return true if the other zone is not null and is the same as this one,
     * with the possible exception of the ID
     *
     * @since 1.2
     */
    // 判断当前时区与指定的时区是否包含相同的"标准偏移"，且是否同样使用/不使用夏令时
    public boolean hasSameRules(TimeZone other) {
        return other != null && getRawOffset() == other.getRawOffset() && useDaylightTime() == other.useDaylightTime();
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the reference to the default TimeZone object. This
     * method doesn't create a clone.
     */
    // 返回系统默认时区
    static TimeZone getDefaultRef() {
        TimeZone defaultZone = defaultTimeZone;
        if(defaultZone == null) {
            // 加载时区信息，并对默认时区字段defaultTimeZone进行初始化
            defaultZone = setDefaultZone();
            assert defaultZone != null;
        }
        
        // Don't clone here.
        return defaultZone;
    }
    
    // 返回给定的时区ID对应的时区；fallback指示遇到无法识别的ID时，是否回退到GMT时区
    private static TimeZone getTimeZone(String ID, boolean fallback) {
        TimeZone tz = ZoneInfo.getTimeZone(ID);
        if(tz != null) {
            return tz;
        }
        
        tz = parseCustomTimeZone(ID);
        if(tz == null && fallback) {
            // 回退到GMT时区
            tz = new ZoneInfo(GMT_ID, 0);
        }
        
        return tz;
    }
    
    private ZoneId toZoneId0() {
        String id = getID();
        TimeZone defaultZone = defaultTimeZone;
        
        // are we not defaultTimeZone but our id is equal to default's?
        if(defaultZone != this && defaultZone != null && id.equals(defaultZone.getID())) {
            // delegate to default TZ which is effectively immutable
            return defaultZone.toZoneId();
        }
        
        // derive it ourselves
        if(ZoneInfoFile.useOldMapping() && id.length() == 3) {
            if("EST".equals(id)) {
                return ZoneId.of("America/New_York");
            }
            if("MST".equals(id)) {
                return ZoneId.of("America/Denver");
            }
            if("HST".equals(id)) {
                return ZoneId.of("America/Honolulu");
            }
        }
        
        return ZoneId.of(id, ZoneId.SHORT_IDS);
    }
    
    /**
     * Gets the raw GMT offset and the amount of daylight saving of this
     * time zone at the given time.
     *
     * @param date    the milliseconds (since January 1, 1970,
     *                00:00:00.000 GMT) at which the time zone offset and daylight
     *                saving amount are found
     * @param offsets an array of int where the raw GMT offset
     *                (offset[0]) and daylight saving amount (offset[1]) are stored,
     *                or null if those values are not needed. The method assumes that
     *                the length of the given array is two or larger.
     *
     * @return the total amount of the raw GMT offset and daylight
     * saving at the specified date.
     *
     * @see Calendar#ZONE_OFFSET
     * @see Calendar#DST_OFFSET
     */
    int getOffsets(long date, int[] offsets) {
        int rawoffset = getRawOffset();
        int dstoffset = 0;
        if(inDaylightTime(new Date(date))) {
            dstoffset = getDSTSavings();
        }
        if(offsets != null) {
            offsets[0] = rawoffset;
            offsets[1] = dstoffset;
        }
        return rawoffset + dstoffset;
    }
    
    private static String[] getDisplayNames(String id, Locale locale) {
        return TimeZoneNameUtility.retrieveDisplayNames(id, locale);
    }
    
    /**
     * Gets the platform defined TimeZone ID.
     */
    /*
     * 从JDK根目录下获取平台定义的时区ID
     *
     * JVM默认返回"Asia/Shanghai"，这代表中国时区。
     * 注：这跟GMT+8不一样
     */
    private static native String getSystemTimeZoneID(String javaHome);
    
    // 加载时区信息，并对默认时区字段defaultTimeZone进行初始化
    private static synchronized TimeZone setDefaultZone() {
        TimeZone tz;
        
        // 获取系统属性集
        Properties props = GetPropertyAction.privilegedGetProperties();
        
        // 查找"user.timezone"对应的属性值
        String zoneID = props.getProperty("user.timezone");
        
        // 如果没有相应的属性，则加载系统默认的属性
        if(zoneID == null || zoneID.isEmpty()) {
            // 获取JDK根目录
            String javaHome = StaticProperty.javaHome();
            try {
                // 获取平台定义的时区ID
                zoneID = getSystemTimeZoneID(javaHome);
                if(zoneID == null) {
                    zoneID = GMT_ID;
                }
            } catch(NullPointerException e) {
                zoneID = GMT_ID;
            }
        }
        
        // Get the time zone for zoneID. But not fall back to "GMT" here.
        tz = getTimeZone(zoneID, false);
        
        if(tz == null) {
            // If the given zone ID is unknown in Java,
            // try to get the GMT-offset-based time zone ID,
            // a.k.a. custom time zone ID (e.g., "GMT-08:00").
            String gmtOffsetID = getSystemGMTOffsetID();
            if(gmtOffsetID != null) {
                zoneID = gmtOffsetID;
            }
            tz = getTimeZone(zoneID, true);
        }
        
        assert tz != null;
        
        // 加载到有效的时区信息后，将其设置到"user.timezone"属性中
        props.setProperty("user.timezone", zoneID);
        
        defaultTimeZone = tz;
        
        return tz;
    }
    
    /**
     * Gets the custom time zone ID based on the GMT offset of the platform. (e.g., "GMT+08:00")
     */
    private static native String getSystemGMTOffsetID();
    
    /**
     * Parses a custom time zone identifier and returns a corresponding zone.
     * This method doesn't support the RFC 822 time zone format. (e.g., +hhmm)
     *
     * @param id a string of the <a href="#CustomID">custom ID form</a>.
     *
     * @return a newly created TimeZone with the given offset and
     * no daylight saving time, or null if the id cannot be parsed.
     */
    // 解析自定义的时区ID
    private static final TimeZone parseCustomTimeZone(String id) {
        int length;
        
        // Error if the length of id isn't long enough or id doesn't start with "GMT".
        if((length = id.length())<(GMT_ID_LENGTH + 2) || id.indexOf(GMT_ID) != 0) {
            return null;
        }
        
        ZoneInfo zi;
        
        // First, we try to find it in the cache with the given id.
        // Even the id is not normalized, the returned ZoneInfo should have its normalized id.
        zi = ZoneInfoFile.getZoneInfo(id);
        if(zi != null) {
            return zi;
        }
        
        int index = GMT_ID_LENGTH;
        boolean negative = false;
        char c = id.charAt(index++);
        if(c == '-') {
            negative = true;
        } else if (c != '+') {
            return null;
        }
        
        int hours = 0;
        int num = 0;
        int countDelim = 0;
        int len = 0;
        while (index < length) {
            c = id.charAt(index++);
            if (c == ':') {
                if (countDelim > 0) {
                    return null;
                }
                if (len > 2) {
                    return null;
                }
                hours = num;
                countDelim++;
                num = 0;
                len = 0;
                continue;
            }
            if (c < '0' || c > '9') {
                return null;
            }
            num = num * 10 + (c - '0');
            len++;
        }
        if (index != length) {
            return null;
        }
        if (countDelim == 0) {
            if (len <= 2) {
                hours = num;
                num = 0;
            } else {
                hours = num / 100;
                num %= 100;
            }
        } else {
            if (len != 2) {
                return null;
            }
        }
        if (hours > 23 || num > 59) {
            return null;
        }
        int gmtOffset =  (hours * 60 + num) * 60 * 1000;
        
        if (gmtOffset == 0) {
            zi = ZoneInfoFile.getZoneInfo(GMT_ID);
            if(negative) {
                zi.setID("GMT-00:00");
            } else {
                zi.setID("GMT+00:00");
            }
        } else {
            zi = ZoneInfoFile.getCustomTimeZone(id, negative ? -gmtOffset : gmtOffset);
        }
        
        return zi;
    }
    
    
    /**
     * Creates a copy of this <code>TimeZone</code>.
     *
     * @return a clone of this <code>TimeZone</code>
     */
    public Object clone() {
        try {
            return super.clone();
        } catch(CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
    
}
