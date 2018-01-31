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
 * COPYRIGHT AND PERMISSION NOTICE
 *
 * Copyright (C) 1991-2016 Unicode, Inc. All rights reserved.
 * Distributed under the Terms of Use in 
 * http://www.unicode.org/copyright.html.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of the Unicode data files and any associated documentation
 * (the "Data Files") or Unicode software and any associated documentation
 * (the "Software") to deal in the Data Files or Software
 * without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, and/or sell copies of
 * the Data Files or Software, and to permit persons to whom the Data Files
 * or Software are furnished to do so, provided that
 * (a) this copyright and permission notice appear with all copies 
 * of the Data Files or Software,
 * (b) this copyright and permission notice appear in associated 
 * documentation, and
 * (c) there is clear notice in each modified Data File or in the Software
 * as well as in the documentation associated with the Data File(s) or
 * Software that the data or software has been modified.
 *
 * THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF
 * ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT OF THIRD PARTY RIGHTS.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS
 * NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL
 * DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
 * DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE DATA FILES OR SOFTWARE.
 *
 * Except as contained in this notice, the name of a copyright holder
 * shall not be used in advertising or otherwise to promote the sale,
 * use or other dealings in these Data Files or Software without prior
 * written authorization of the copyright holder.
 */

package sun.text.resources.cldr.ext;

import java.util.ListResourceBundle;

public class FormatData_yue_Hans extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u4e00\u6708",
               "\u4e8c\u6708",
               "\u4e09\u6708",
               "\u56db\u6708",
               "\u4e94\u6708",
               "\u516d\u6708",
               "\u4e03\u6708",
               "\u516b\u6708",
               "\u4e5d\u6708",
               "\u5341\u6708",
               "\u5341\u4e00\u6708",
               "\u5341\u4e8c\u6708",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "1\u6708",
               "2\u6708",
               "3\u6708",
               "4\u6708",
               "5\u6708",
               "6\u6708",
               "7\u6708",
               "8\u6708",
               "9\u6708",
               "10\u6708",
               "11\u6708",
               "12\u6708",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u661f\u671f\u65e5",
               "\u661f\u671f\u4e00",
               "\u661f\u671f\u4e8c",
               "\u661f\u671f\u4e09",
               "\u661f\u671f\u56db",
               "\u661f\u671f\u4e94",
               "\u661f\u671f\u516d",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u5468\u65e5",
               "\u5468\u4e00",
               "\u5468\u4e8c",
               "\u5468\u4e09",
               "\u5468\u56db",
               "\u5468\u4e94",
               "\u5468\u516d",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u65e5",
               "\u4e00",
               "\u4e8c",
               "\u4e09",
               "\u56db",
               "\u4e94",
               "\u516d",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u7b2c1\u5b63",
               "\u7b2c2\u5b63",
               "\u7b2c3\u5b63",
               "\u7b2c4\u5b63",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u4e0a\u5348",
               "\u4e0b\u5348",
            };
        final String[] metaValue_long_Eras = new String[] {
               "\u897f\u5143\u524d",
               "\u897f\u5143",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "zzzz ah:mm:ss",
               "z ah:mm:ss",
               "ah:mm:ss",
               "ah:mm",
            };
        final String[] metaValue_DateTimePatterns = new String[] {
               "{1} {0}",
               "{1} {0}",
               "{1} {0}",
               "{1} {0}",
            };
        final String[] metaValue_buddhist_MonthNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
               "5",
               "6",
               "7",
               "8",
               "9",
               "10",
               "11",
               "12",
               "",
            };
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_buddhist_long_Eras = new String[] {
               "BC",
               "\u4f5b\u5386",
            };
        final String[] metaValue_japanese_long_Eras = new String[] {
               "\u897f\u5143",
               "\u660e\u6cbb",
               "\u5927\u6b63",
               "\u662d\u548c",
               "\u5e73\u6210",
            };
        final String[] metaValue_roc_long_Eras = new String[] {
               "\u6c11\u56fd\u524d",
               "\u6c11\u56fd",
            };
        final String[] metaValue_islamic_MonthNames = new String[] {
               "\u7a46\u54c8\u5170\u59c6\u6708",
               "\u8272\u6cd5\u5c14\u6708",
               "\u8d56\u6bd4\u6708 I",
               "\u8d56\u6bd4\u6708 II",
               "\u4e3b\u9a6c\u8fbe\u6708 I",
               "\u4e3b\u9a6c\u8fbe\u6708 II",
               "\u8d56\u54f2\u535c\u6708",
               "\u820d\u5c14\u90a6\u6708",
               "\u8d56\u4e70\u4e39\u6708",
               "\u95ea\u74e6\u9c81\u6708",
               "\u90fd\u5c14\u5580\u5c14\u5fb7\u6708",
               "\u90fd\u5c14\u9ed1\u54f2\u6708",
               "",
            };
        final String[] metaValue_islamic_long_Eras = new String[] {
               "",
               "\u4f0a\u65af\u5170\u5386",
            };
        final String metaValue_calendarname_gregorian = "\u516c\u5386";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u5e74" },
            { "calendarname.islamic-umalqura", "\u4e4c\u59c6\u5e93\u62c9\u5386" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.Eras", metaValue_buddhist_long_Eras },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns",
                new String[] {
                    "Gy\u5e74M\u6708d\u65e5EEEE",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gyy-MM-dd",
                }
            },
            { "standalone.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "calendarname.islamic-civil", "\u4f0a\u65af\u5170\u6c11\u7528\u5386" },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_long_Eras },
            { "roc.long.Eras", metaValue_roc_long_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u65e5\u672c\u5386" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "long.Eras", metaValue_long_Eras },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns", metaValue_DateTimePatterns },
            { "narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "latn.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "\u975e\u6570\u503c",
                }
            },
            { "japanese.DatePatterns",
                new String[] {
                    "GGGGy\u5e74M\u6708d\u65e5EEEE",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGyy-MM-dd",
                }
            },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u5206\u949f" },
            { "japanese.long.Eras", metaValue_japanese_long_Eras },
            { "field.era", "\u5e74\u4ee3" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u4e0a\u5348/\u4e0b\u5348" },
            { "islamic.MonthNames", metaValue_islamic_MonthNames },
            { "calendarname.roc", "\u6c11\u56fd\u5386" },
            { "islamic.DatePatterns",
                new String[] {
                    "GGGGy\u5e74M\u6708d\u65e5EEEE",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGy/M/d",
                }
            },
            { "roc.QuarterAbbreviations", metaValue_QuarterNames },
            { "islamic.Eras", metaValue_islamic_long_Eras },
            { "field.month", "\u6708" },
            { "roc.Eras", metaValue_roc_long_Eras },
            { "field.second", "\u79d2" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "\u00a4#,##0.00",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns",
                new String[] {
                    "GGGGy\u5e74M\u6708d\u65e5EEEE",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGyy/M/d",
                }
            },
            { "calendarname.islamic", "\u4f0a\u65af\u5170\u5386" },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "timezone.regionFormat", "{0}\u65f6\u95f4" },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.long.Eras", metaValue_buddhist_long_Eras },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u65f6\u533a" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.narrow.Eras", metaValue_roc_long_Eras },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_long_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.islamic.DatePatterns",
                new String[] {
                    "Gy\u5e74M\u6708d\u65e5EEEE",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gy/M/d",
                }
            },
            { "field.weekday", "\u5468\u5929" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.long.Eras", metaValue_islamic_long_Eras },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterNames },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns",
                new String[] {
                    "GGGGy\u5e74M\u6708d\u65e5EEEE",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGy-M-d",
                }
            },
            { "roc.MonthNames", metaValue_MonthNames },
            { "buddhist.Eras", metaValue_buddhist_long_Eras },
            { "field.week", "\u5468" },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "buddhist.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.buddhist.DatePatterns",
                new String[] {
                    "Gy\u5e74M\u6708d\u65e5EEEE",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gy-M-d",
                }
            },
            { "java.time.roc.DatePatterns",
                new String[] {
                    "Gy\u5e74M\u6708d\u65e5EEEE",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gyy/M/d",
                }
            },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "DatePatterns",
                new String[] {
                    "y\u5e74M\u6708d\u65e5EEEE",
                    "y\u5e74M\u6708d\u65e5",
                    "y\u5e74M\u6708d\u65e5",
                    "y/M/d",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "japanese.DateTimePatterns", metaValue_DateTimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u5c0f\u65f6" },
            { "islamic.MonthAbbreviations", metaValue_islamic_MonthNames },
            { "islamic.narrow.Eras", metaValue_islamic_long_Eras },
            { "calendarname.buddhist", "\u4f5b\u5386" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterNames },
            { "japanese.Eras", metaValue_japanese_long_Eras },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
