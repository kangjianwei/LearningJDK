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

public class FormatData_si extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u0da2\u0db1\u0dc0\u0dcf\u0dbb\u0dd2",
               "\u0db4\u0dd9\u0db6\u0dbb\u0dc0\u0dcf\u0dbb\u0dd2",
               "\u0db8\u0dcf\u0dbb\u0dca\u0dad\u0dd4",
               "\u0d85\u0db4\u0dca\u200d\u0dbb\u0dda\u0dbd\u0dca",
               "\u0db8\u0dd0\u0dba\u0dd2",
               "\u0da2\u0dd6\u0db1\u0dd2",
               "\u0da2\u0dd6\u0dbd\u0dd2",
               "\u0d85\u0d9c\u0ddd\u0dc3\u0dca\u0dad\u0dd4",
               "\u0dc3\u0dd0\u0db4\u0dca\u0dad\u0dd0\u0db8\u0dca\u0db6\u0dbb\u0dca",
               "\u0d94\u0d9a\u0dca\u0dad\u0ddd\u0db6\u0dbb\u0dca",
               "\u0db1\u0ddc\u0dc0\u0dd0\u0db8\u0dca\u0db6\u0dbb\u0dca",
               "\u0daf\u0dd9\u0dc3\u0dd0\u0db8\u0dca\u0db6\u0dbb\u0dca",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u0da2\u0db1",
               "\u0db4\u0dd9\u0db6",
               "\u0db8\u0dcf\u0dbb\u0dca\u0dad\u0dd4",
               "\u0d85\u0db4\u0dca\u200d\u0dbb\u0dda\u0dbd\u0dca",
               "\u0db8\u0dd0\u0dba\u0dd2",
               "\u0da2\u0dd6\u0db1\u0dd2",
               "\u0da2\u0dd6\u0dbd\u0dd2",
               "\u0d85\u0d9c\u0ddd",
               "\u0dc3\u0dd0\u0db4\u0dca",
               "\u0d94\u0d9a\u0dca",
               "\u0db1\u0ddc\u0dc0\u0dd0",
               "\u0daf\u0dd9\u0dc3\u0dd0",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u0da2",
               "\u0db4\u0dd9",
               "\u0db8\u0dcf",
               "\u0d85",
               "\u0db8\u0dd0",
               "\u0da2\u0dd6",
               "\u0da2\u0dd6",
               "\u0d85",
               "\u0dc3\u0dd0",
               "\u0d94",
               "\u0db1\u0dd9",
               "\u0daf\u0dd9",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u0d89\u0dbb\u0dd2\u0daf\u0dcf",
               "\u0dc3\u0db3\u0dd4\u0daf\u0dcf",
               "\u0d85\u0d9f\u0dc4\u0dbb\u0dd4\u0dc0\u0dcf\u0daf\u0dcf",
               "\u0db6\u0daf\u0dcf\u0daf\u0dcf",
               "\u0db6\u0dca\u200d\u0dbb\u0dc4\u0dc3\u0dca\u0db4\u0dad\u0dd2\u0db1\u0dca\u0daf\u0dcf",
               "\u0dc3\u0dd2\u0d9a\u0dd4\u0dbb\u0dcf\u0daf\u0dcf",
               "\u0dc3\u0dd9\u0db1\u0dc3\u0dd4\u0dbb\u0dcf\u0daf\u0dcf",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u0d89\u0dbb\u0dd2\u0daf\u0dcf",
               "\u0dc3\u0db3\u0dd4\u0daf\u0dcf",
               "\u0d85\u0d9f\u0dc4",
               "\u0db6\u0daf\u0dcf\u0daf\u0dcf",
               "\u0db6\u0dca\u200d\u0dbb\u0dc4\u0dc3\u0dca",
               "\u0dc3\u0dd2\u0d9a\u0dd4",
               "\u0dc3\u0dd9\u0db1",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u0d89",
               "\u0dc3",
               "\u0d85",
               "\u0db6",
               "\u0db6\u0dca\u200d\u0dbb",
               "\u0dc3\u0dd2",
               "\u0dc3\u0dd9",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "1 \u0dc0\u0db1 \u0d9a\u0dcf\u0dbb\u0dca\u0dad\u0dd4\u0dc0",
               "2 \u0dc0\u0db1 \u0d9a\u0dcf\u0dbb\u0dca\u0dad\u0dd4\u0dc0",
               "3 \u0dc0\u0db1 \u0d9a\u0dcf\u0dbb\u0dca\u0dad\u0dd4\u0dc0",
               "4 \u0dc0\u0db1 \u0d9a\u0dcf\u0dbb\u0dca\u0dad\u0dd4\u0dc0",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "\u0d9a\u0dcf\u0dbb\u0dca:1",
               "\u0d9a\u0dcf\u0dbb\u0dca:2",
               "\u0d9a\u0dcf\u0dbb\u0dca:3",
               "\u0d9a\u0dcf\u0dbb\u0dca:4",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u0db4\u0dd9.\u0dc0.",
               "\u0db4.\u0dc0.",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "\u0db4\u0dd9",
               "\u0db4",
            };
        final String[] metaValue_Eras = new String[] {
               "\u0d9a\u0dca\u200d\u0dbb\u0dd2.\u0db4\u0dd6.",
               "\u0d9a\u0dca\u200d\u0dbb\u0dd2.\u0dc0.",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "HH.mm.ss zzzz",
               "HH.mm.ss z",
               "HH.mm.ss",
               "HH.mm",
            };
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "G y MMMM d, EEEE",
               "G y MMMM d",
               "G y MMM d",
               "GGGGG y-MM-dd",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "GGGG y MMMM d, EEEE",
               "GGGG y MMMM d",
               "GGGG y MMM d",
               "G y-MM-dd",
            };
        final String metaValue_calendarname_gregorian = "\u0d9c\u0dca\u200d\u0dbb\u0dd9\u0d9c\u0dbb\u0dd2\u0dba\u0dcf\u0db1\u0dd4 \u0daf\u0dd2\u0db1 \u0daf\u0dbb\u0dca\u0dc1\u0db1\u0dba";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u0dc0\u0dbb\u0dca\u0dc2\u0dba" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.gmtFormat", "\u0d9c\u0dca\u200d\u0dbb\u0dd2\u0db8\u0dc0\u0dda{0}" },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "{0} \u0dc3\u0db8\u0dca\u0db8\u0dad \u0dc0\u0dda\u0dbd\u0dcf\u0dc0" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u0da2\u0db4\u0db1\u0dca \u0daf\u0dd2\u0db1 \u0daf\u0dbb\u0dca\u0dc1\u0db1\u0dba" },
            { "timezone.gmtZeroFormat", "\u0d9c\u0dca\u200d\u0dbb\u0dd2\u0db8\u0dc0\u0dda" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "long.Eras",
                new String[] {
                    "\u0d9a\u0dca\u200d\u0dbb\u0dd2\u0dc3\u0dca\u0dad\u0dd4 \u0db4\u0dd6\u0dbb\u0dca\u0dc0",
                    "\u0d9a\u0dca\u200d\u0dbb\u0dd2\u0dc3\u0dca\u0dad\u0dd4 \u0dc0\u0dbb\u0dca\u0dc2",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}",
                    "{1} {0}",
                    "{1} {0}",
                    "{1} {0}",
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
            { "field.minute", "\u0db8\u0dd2\u0db1\u0dd2\u0dad\u0dca\u0dad\u0dd4\u0dc0" },
            { "field.era", "\u0dba\u0dd4\u0d9c\u0dba" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u0db4\u0dd9.\u0dc0/\u0db4.\u0dc0" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.roc", "\u0db8\u0dd2\u0db1\u0dca\u0d9c\u0dcf \u0daf\u0dd2\u0db1 \u0daf\u0dbb\u0dca\u0dc1\u0db1\u0dba" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "field.month", "\u0db8\u0dcf\u0dc3\u0dba" },
            { "field.second", "\u0dad\u0dad\u0dca\u0db4\u0dbb\u0dba" },
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
            { "calendarname.islamic", "\u0d89\u0dc3\u0dca\u0dbd\u0dcf\u0db8\u0dd3\u0dba \u0daf\u0dd2\u0db1 \u0daf\u0dbb\u0dca\u0dc1\u0db1\u0dba" },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations",
                new String[] {
                    "\u0da2\u0db1",
                    "\u0db4\u0dd9\u0db6",
                    "\u0db8\u0dcf\u0dbb\u0dca",
                    "\u0d85\u0db4\u0dca\u200d\u0dbb\u0dda\u0dbd\u0dca",
                    "\u0db8\u0dd0\u0dba\u0dd2",
                    "\u0da2\u0dd6\u0db1\u0dd2",
                    "\u0da2\u0dd6\u0dbd\u0dd2",
                    "\u0d85\u0d9c\u0ddd",
                    "\u0dc3\u0dd0\u0db4\u0dca",
                    "\u0d94\u0d9a\u0dca",
                    "\u0db1\u0ddc\u0dc0\u0dd0",
                    "\u0daf\u0dd9\u0dc3\u0dd0",
                    "",
                }
            },
            { "timezone.regionFormat", "{0} \u0dc0\u0dda\u0dbd\u0dcf\u0dc0" },
            { "roc.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u0dc0\u0dda\u0dbd\u0dcf \u0d9a\u0dbd\u0dcf\u0db4\u0dba" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "\u0dc3\u0dad\u0dd2\u0dba\u0dda \u0daf\u0dd2\u0db1\u0dba" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "timezone.hourFormat", "+HH.mm;-HH.mm" },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u0dc3\u0dad\u0dd2\u0dba" },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0} \u0daf\u0dd2\u0dc0\u0dcf\u0d86\u0dbd\u0ddd\u0d9a \u0dc0\u0dda\u0dbd\u0dcf\u0dc0" },
            { "DatePatterns",
                new String[] {
                    "y MMMM d, EEEE",
                    "y MMMM d",
                    "y MMM d",
                    "y-MM-dd",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u0db4\u0dd0\u0dba" },
            { "calendarname.buddhist", "\u0db6\u0ddc\u0daf\u0dd4 \u0daf\u0dd2\u0db1 \u0daf\u0dbb\u0dca\u0dc1\u0db1\u0dba" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
