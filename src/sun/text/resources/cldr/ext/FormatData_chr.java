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

public class FormatData_chr extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u13a4\u13c3\u13b8\u13d4\u13c5",
               "\u13a7\u13a6\u13b5",
               "\u13a0\u13c5\u13f1",
               "\u13a7\u13ec\u13c2",
               "\u13a0\u13c2\u13cd\u13ac\u13d8",
               "\u13d5\u13ad\u13b7\u13f1",
               "\u13ab\u13f0\u13c9\u13c2",
               "\u13a6\u13b6\u13c2",
               "\u13da\u13b5\u13cd\u13d7",
               "\u13da\u13c2\u13c5\u13d7",
               "\u13c5\u13d3\u13d5\u13c6",
               "\u13a5\u13cd\u13a9\u13f1",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u13a4\u13c3",
               "\u13a7\u13a6",
               "\u13a0\u13c5",
               "\u13a7\u13ec",
               "\u13a0\u13c2",
               "\u13d5\u13ad",
               "\u13ab\u13f0",
               "\u13a6\u13b6",
               "\u13da\u13b5",
               "\u13da\u13c2",
               "\u13c5\u13d3",
               "\u13a5\u13cd",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u13a4",
               "\u13a7",
               "\u13a0",
               "\u13a7",
               "\u13a0",
               "\u13d5",
               "\u13ab",
               "\u13a6",
               "\u13da",
               "\u13da",
               "\u13c5",
               "\u13a5",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u13a4\u13be\u13d9\u13d3\u13c6\u13cd\u13ac",
               "\u13a4\u13be\u13d9\u13d3\u13c9\u13c5\u13af",
               "\u13d4\u13b5\u13c1\u13a2\u13a6",
               "\u13e6\u13a2\u13c1\u13a2\u13a6",
               "\u13c5\u13a9\u13c1\u13a2\u13a6",
               "\u13e7\u13be\u13a9\u13b6\u13cd\u13d7",
               "\u13a4\u13be\u13d9\u13d3\u13c8\u13d5\u13be",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u13c6\u13cd\u13ac",
               "\u13c9\u13c5\u13af",
               "\u13d4\u13b5\u13c1",
               "\u13e6\u13a2\u13c1",
               "\u13c5\u13a9\u13c1",
               "\u13e7\u13be\u13a9",
               "\u13c8\u13d5\u13be",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u13c6",
               "\u13c9",
               "\u13d4",
               "\u13e6",
               "\u13c5",
               "\u13e7",
               "\u13a4",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "1st \u13a9\u13c4\u13d9\u13d7",
               "2nd \u13a9\u13c4\u13d9\u13d7",
               "3rd \u13a9\u13c4\u13d9\u13d7",
               "4th \u13a9\u13c4\u13d9\u13d7",
            };
        final String[] metaValue_standalone_QuarterAbbreviations = new String[] {
               "Q1",
               "Q2",
               "Q3",
               "Q4",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u13cc\u13be\u13b4",
               "\u13d2\u13af\u13f1\u13a2\u13d7\u13e2",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "\u13cc",
               "\u13d2",
            };
        final String[] metaValue_abbreviated_AmPmMarkers = new String[] {
               "\u13cc\u13be\u13b4",
               "\u13d2\u13af\u13f1\u13a2",
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
        final String metaValue_calendarname_gregorian = "\u13a9\u13b4\u13aa\u13b5\u13a0\u13c2 \u13c5\u13d9 \u13d7\u13ce\u13cd\u13d7";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u13a4\u13d5\u13d8\u13f4\u13cc\u13d7\u13d2\u13a2" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "timezone.regionFormat.standard", "{0} \u13a0\u13df\u13b6\u13cd\u13d7 \u13a0\u13df\u13a2\u13b5\u13d2" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "calendarname.japanese", "\u13e3\u13c6\u13c2\u13cf \u13c5\u13d9 \u13d7\u13ce\u13cd\u13d7" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "long.Eras",
                new String[] {
                    "\u13e7\u13d3\u13b7\u13b8 \u13a4\u13b7\u13af\u13cd\u13d7 \u13a6\u13b6\u13c1\u13db",
                    "\u13a0\u13c3 \u13d9\u13bb\u13c2",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns",
                new String[] {
                    "{1} \u13a4\u13be\u13a2 {0}",
                    "{1} \u13a4\u13be\u13a2 {0}",
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
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u13a2\u13ef\u13d4\u13ec\u13cd\u13d4\u13c5" },
            { "field.era", "\u13d7\u13d3\u13b4\u13c2\u13cd\u13ac" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u13cc\u13be\u13b4/\u13d2\u13af\u13f1" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.roc", "\u13cd\u13a6\u13da\u13a9 \u13be\u13bf \u13d3\u13b6\u13c2\u13a8\u13cd\u13db \u13c5\u13d9 \u13d7\u13ce\u13cd\u13d7" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "field.month", "\u13a7\u13b8\u13a2" },
            { "field.second", "\u13a0\u13ce\u13e2" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "\u00a4#,##0.00",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "calendarname.islamic", "\u13a2\u13cd\u13b3\u13bb\u13a9 \u13c5\u13d9 \u13d7\u13ce\u13cd\u13d7" },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "timezone.regionFormat", "{0} \u13a0\u13df\u13a2\u13b5\u13d2" },
            { "roc.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u13c2\u13ac\u13be\u13db \u13e7\u13d3\u13b4\u13c5\u13d3 \u13d3\u13df\u13a2\u13b5\u13cd\u13d2\u13a2" },
            { "japanese.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "\u13a2\u13a6 \u13d5\u13a8\u13cc\u13d7\u13d2" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u13d2\u13be\u13d9\u13d3\u13c6\u13cd\u13d7" },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0} \u13aa\u13af \u13a2\u13a6 \u13a0\u13df\u13a2\u13b5\u13cd\u13d2\u13a9" },
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
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u13d1\u13df\u13b6\u13d3" },
            { "calendarname.buddhist", "\u13ca\u13d7\u13cd\u13d8 \u13c5\u13d9 \u13d7\u13ce\u13cd\u13d7" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "roc.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
