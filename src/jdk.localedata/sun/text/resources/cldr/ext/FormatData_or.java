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

public class FormatData_or extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u0b1c\u0b3e\u0b28\u0b41\u0b06\u0b30\u0b40",
               "\u0b2b\u0b47\u0b2c\u0b43\u0b06\u0b30\u0b40",
               "\u0b2e\u0b3e\u0b30\u0b4d\u0b1a\u0b4d\u0b1a",
               "\u0b05\u0b2a\u0b4d\u0b30\u0b47\u0b32",
               "\u0b2e\u0b07",
               "\u0b1c\u0b41\u0b28",
               "\u0b1c\u0b41\u0b32\u0b3e\u0b07",
               "\u0b05\u0b17\u0b37\u0b4d\u0b1f",
               "\u0b38\u0b47\u0b2a\u0b4d\u0b1f\u0b47\u0b2e\u0b4d\u0b2c\u0b30",
               "\u0b05\u0b15\u0b4d\u0b1f\u0b4b\u0b2c\u0b30",
               "\u0b28\u0b2d\u0b47\u0b2e\u0b4d\u0b2c\u0b30",
               "\u0b21\u0b3f\u0b38\u0b47\u0b2e\u0b4d\u0b2c\u0b30",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u0b1c\u0b3e",
               "\u0b2b\u0b47",
               "\u0b2e\u0b3e",
               "\u0b05",
               "\u0b2e\u0b07",
               "\u0b1c\u0b41",
               "\u0b1c\u0b41",
               "\u0b05",
               "\u0b38\u0b47",
               "\u0b05",
               "\u0b28",
               "\u0b21\u0b3f",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u0b30\u0b2c\u0b3f\u0b2c\u0b3e\u0b30",
               "\u0b38\u0b4b\u0b2e\u0b2c\u0b3e\u0b30",
               "\u0b2e\u0b19\u0b4d\u0b17\u0b33\u0b2c\u0b3e\u0b30",
               "\u0b2c\u0b41\u0b27\u0b2c\u0b3e\u0b30",
               "\u0b17\u0b41\u0b30\u0b41\u0b2c\u0b3e\u0b30",
               "\u0b36\u0b41\u0b15\u0b4d\u0b30\u0b2c\u0b3e\u0b30",
               "\u0b36\u0b28\u0b3f\u0b2c\u0b3e\u0b30",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u0b30\u0b2c\u0b3f",
               "\u0b38\u0b4b\u0b2e",
               "\u0b2e\u0b19\u0b4d\u0b17\u0b33",
               "\u0b2c\u0b41\u0b27",
               "\u0b17\u0b41\u0b30\u0b41",
               "\u0b36\u0b41\u0b15\u0b4d\u0b30",
               "\u0b36\u0b28\u0b3f",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u0b30",
               "\u0b38\u0b4b",
               "\u0b2e",
               "\u0b2c\u0b41",
               "\u0b17\u0b41",
               "\u0b36\u0b41",
               "\u0b36",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "1\u0b2e \u0b24\u0b4d\u0b30\u0b5f\u0b2e\u0b3e\u0b38",
               "2\u0b5f \u0b24\u0b4d\u0b30\u0b5f\u0b2e\u0b3e\u0b38",
               "3\u0b5f \u0b24\u0b4d\u0b30\u0b5f\u0b2e\u0b3e\u0b38",
               "4\u0b30\u0b4d\u0b25 \u0b24\u0b4d\u0b30\u0b5f\u0b2e\u0b3e\u0b38",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "\u0b2a\u0b42",
               "\u0b05",
            };
        final String[] metaValue_Eras = new String[] {
               "BC",
               "AD",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "h:mm:ss a zzzz",
               "h:mm:ss a z",
               "h:mm:ss a",
               "h:mm a",
            };
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE, MMMM d, y G",
               "MMMM d, y G",
               "MMM d, y G",
               "M/d/y GGGGG",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE, MMMM d, y GGGG",
               "MMMM d, y GGGG",
               "MMM d, y GGGG",
               "M/d/y G",
            };
        final String[] metaValue_roc_AmPmMarkers = new String[] {
               "AM",
               "PM",
            };
        final String metaValue_calendarname_gregorian = "\u0b17\u0b4d\u0b30\u0b47\u0b17\u0b4b\u0b30\u0b3f\u0b5f \u0b15\u0b4d\u0b5f\u0b3e\u0b32\u0b47\u0b23\u0b4d\u0b21\u0b30\u0b4d";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u0b2c\u0b30\u0b4d\u0b37" },
            { "islamic.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_roc_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations",
                new String[] {
                    "Q1",
                    "Q2",
                    "Q3",
                    "Q4",
                }
            },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u0b38\u0b2e\u0b5f \u0b15\u0b4d\u0b37\u0b47\u0b24\u0b4d\u0b30" },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "japanese.QuarterAbbreviations", metaValue_QuarterNames },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "timezone.regionFormat.standard", "{0} \u0b2e\u0b3e\u0b28\u0b3e\u0b19\u0b4d\u0b15 \u0b38\u0b2e\u0b5f" },
            { "calendarname.japanese", "\u0b1c\u0b3e\u0b2a\u0b3e\u0b28\u0b3f\u0b1c\u0b4d\u200c \u0b15\u0b4d\u0b5f\u0b3e\u0b32\u0b47\u0b23\u0b4d\u0b21\u0b30\u0b4d\u200c" },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "roc.DayNames", metaValue_DayNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "long.Eras",
                new String[] {
                    "\u0b16\u0b4d\u0b30\u0b40\u0b37\u0b4d\u0b1f\u0b2a\u0b42\u0b30\u0b4d\u0b2c",
                    "\u0b16\u0b4d\u0b30\u0b40\u0b37\u0b4d\u0b1f\u0b3e\u0b2c\u0b4d\u0b26",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "buddhist.MonthAbbreviations", metaValue_MonthNames },
            { "field.weekday", "\u0b38. \u0b30 \u0b26\u0b3f\u0b28" },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns",
                new String[] {
                    "{0} \u0b20\u0b3e\u0b30\u0b47 {1}",
                    "{0} \u0b20\u0b3e\u0b30\u0b47 {1}",
                    "{1}, {0}",
                    "{1}, {0}",
                }
            },
            { "narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
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
                    "NaN",
                }
            },
            { "MonthNarrows", metaValue_MonthNarrows },
            { "japanese.DatePatterns", metaValue_buddhist_DatePatterns },
            { "japanese.MonthAbbreviations", metaValue_MonthNames },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u0b2e\u0b3f\u0b28\u0b3f\u0b1f\u0b4d" },
            { "field.era", "\u0b2f\u0b41\u0b17" },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "field.dayperiod", "\u0b2a\u0b42\u0b30\u0b4d\u0b2c\u0b3e\u0b39\u0b4d\u0b28/\u0b05\u0b2a\u0b30\u0b3e\u0b39\u0b4d\u0b28" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterNames },
            { "calendarname.roc", "\u0b2e\u0b3f\u0b19\u0b4d\u0b17\u0b4b\u0b13 \u0b15\u0b4d\u0b5f\u0b3e\u0b32\u0b47\u0b23\u0b4d\u0b21\u0b30\u0b4d\u200c" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterNames },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "field.month", "\u0b2e\u0b3e\u0b38" },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "field.second", "\u0b38\u0b47\u0b15\u0b47\u0b23\u0b4d\u0b21\u0b4d" },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u0b38\u0b2a\u0b4d\u0b24\u0b3e\u0b39" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##,##0.###",
                    "\u00a4#,##,##0.00",
                    "#,##,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "calendarname.islamic", "\u0b07\u0b38\u0b32\u0b3e\u0b2e\u0b3f\u0b15\u0b4d\u200c \u0b15\u0b4d\u0b5f\u0b3e\u0b32\u0b47\u0b23\u0b4d\u0b21\u0b30\u0b4d\u200c" },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "orya.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0b66",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "roc.AmPmMarkers", metaValue_roc_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0} \u0b26\u0b3f\u0b2c\u0b3e\u0b32\u0b4b\u0b15 \u0b38\u0b2e\u0b5f" },
            { "DatePatterns",
                new String[] {
                    "EEEE, MMMM d, y",
                    "MMMM d, y",
                    "MMM d, y",
                    "M/d/yy",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthNames },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u0b18\u0b23\u0b4d\u0b1f\u0b3e" },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "calendarname.buddhist", "\u0b2c\u0b4c\u0b26\u0b4d\u0b27\u0b27\u0b30\u0b4d\u0b2e\u0b3e\u0b32\u0b2e\u0b4d\u0b2c\u0b40\u0b19\u0b4d\u0b15 \u0b15\u0b4d\u0b5f\u0b3e\u0b32\u0b47\u0b23\u0b4d\u0b21\u0b30\u0b4d\u200c" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "standalone.MonthAbbreviations", metaValue_MonthNames },
            { "timezone.regionFormat", "{0} \u0b38\u0b2e\u0b5f" },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "roc.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterNames },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
