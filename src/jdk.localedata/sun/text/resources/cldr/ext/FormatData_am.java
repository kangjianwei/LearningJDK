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

public class FormatData_am extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u1303\u1295\u12e9\u12c8\u122a",
               "\u134c\u1265\u1229\u12c8\u122a",
               "\u121b\u122d\u127d",
               "\u12a4\u1355\u122a\u120d",
               "\u121c\u12ed",
               "\u1301\u1295",
               "\u1301\u120b\u12ed",
               "\u12a6\u1308\u1235\u1275",
               "\u1234\u1355\u1274\u121d\u1260\u122d",
               "\u12a6\u12ad\u1276\u1260\u122d",
               "\u1296\u126c\u121d\u1260\u122d",
               "\u12f2\u1234\u121d\u1260\u122d",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u1303\u1295\u12e9",
               "\u134c\u1265\u1229",
               "\u121b\u122d\u127d",
               "\u12a4\u1355\u122a",
               "\u121c\u12ed",
               "\u1301\u1295",
               "\u1301\u120b\u12ed",
               "\u12a6\u1308\u1235",
               "\u1234\u1355\u1274",
               "\u12a6\u12ad\u1276",
               "\u1296\u126c\u121d",
               "\u12f2\u1234\u121d",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u1303",
               "\u134c",
               "\u121b",
               "\u12a4",
               "\u121c",
               "\u1301",
               "\u1301",
               "\u12a6",
               "\u1234",
               "\u12a6",
               "\u1296",
               "\u12f2",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u12a5\u1211\u12f5",
               "\u1230\u129e",
               "\u121b\u12ad\u1230\u129e",
               "\u1228\u1261\u12d5",
               "\u1210\u1219\u1235",
               "\u12d3\u122d\u1265",
               "\u1245\u12f3\u121c",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u12a5\u1211\u12f5",
               "\u1230\u129e",
               "\u121b\u12ad\u1230",
               "\u1228\u1261\u12d5",
               "\u1210\u1219\u1235",
               "\u12d3\u122d\u1265",
               "\u1245\u12f3\u121c",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u12a5",
               "\u1230",
               "\u121b",
               "\u1228",
               "\u1210",
               "\u12d3",
               "\u1245",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "1\u129b\u12cd \u1229\u1265",
               "2\u129b\u12cd \u1229\u1265",
               "3\u129b\u12cd \u1229\u1265",
               "4\u129b\u12cd \u1229\u1265",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "\u1229\u12651",
               "\u1229\u12652",
               "\u1229\u12653",
               "\u1229\u12654",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u1325\u12cb\u1275",
               "\u12a8\u1230\u12d3\u1275",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "\u1320",
               "\u12a8",
            };
        final String[] metaValue_Eras = new String[] {
               "\u12d3/\u12d3",
               "\u12d3/\u121d",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "h:mm:ss a zzzz",
               "h:mm:ss a z",
               "h:mm:ss a",
               "h:mm a",
            };
        final String[] metaValue_DateTimePatterns = new String[] {
               "{1} {0}",
               "{1} {0}",
               "{1} {0}",
               "{1} {0}",
            };
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_buddhist_long_Eras = new String[] {
               "BC",
               "BE",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE\u1363 d MMMM y G",
               "d MMMM y G",
               "d MMM y G",
               "dd/MM/y GGGGG",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE\u1363 d MMMM y GGGG",
               "d MMMM y GGGG",
               "d MMM y GGGG",
               "dd/MM/y G",
            };
        final String metaValue_calendarname_gregorian = "\u12e8\u130d\u122a\u130e\u122a\u12eb\u1295 \u12e8\u1240\u1295 \u12a0\u1246\u1323\u1320\u122d";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u12d3\u1218\u1275" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.Eras", metaValue_buddhist_long_Eras },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.gmtFormat", "\u1302 \u12a4\u121d \u1272{0}" },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.islamic-civil", "\u12e8\u12a5\u1235\u120b\u121d \u1205\u12dd\u1263\u12ca \u12e8\u1240\u1295 \u12a0\u1246\u1323\u1320\u122d" },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "{0} \u1218\u12f0\u1260\u129b \u1230\u12d3\u1275" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u12e8\u1303\u1353\u1295 \u12e8\u1240\u1295 \u12a0\u1246\u1323\u1320\u122d" },
            { "timezone.gmtZeroFormat", "\u1302 \u12a4\u121d \u1272" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "long.Eras",
                new String[] {
                    "\u12d3\u1218\u1270 \u12d3\u1208\u121d",
                    "\u12d3\u1218\u1270 \u121d\u1215\u1228\u1275",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns", metaValue_DateTimePatterns },
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
            { "field.minute", "\u12f0\u1242\u1243" },
            { "field.era", "\u12d8\u1218\u1295" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u1325\u12cb\u1275/\u12a8\u1230\u12d3\u1275" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.roc", "\u12e8\u121a\u1295\u1309 \u12e8\u1240\u1295 \u12a0\u1246\u1323\u1320\u122d" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "field.month", "\u12c8\u122d" },
            { "field.second", "\u1230\u12a8\u1295\u12f5" },
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
            { "calendarname.islamic", "\u12e8\u12a5\u1235\u120b\u121b\u12ca \u12e8\u1230\u12d3\u1275 \u12a0\u1246\u1323\u1320\u122d" },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "timezone.regionFormat", "{0} \u130a\u12dc" },
            { "roc.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.long.Eras", metaValue_buddhist_long_Eras },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u12e8\u1230\u12d3\u1275 \u1230\u1245" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "\u12a0\u12d8\u1266\u1275" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "timezone.hourFormat", "+HHmm;-HHmm" },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "buddhist.Eras", metaValue_buddhist_long_Eras },
            { "field.week", "\u1233\u121d\u1295\u1275" },
            { "buddhist.DateTimePatterns", metaValue_DateTimePatterns },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0} \u12e8\u1240\u1295 \u1265\u122d\u1203\u1295 \u1230\u12d3\u1275" },
            { "DatePatterns",
                new String[] {
                    "EEEE \u1363d MMMM y",
                    "d MMMM y",
                    "d MMM y",
                    "dd/MM/y",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u1230\u12d3\u1275" },
            { "calendarname.buddhist", "\u12e8\u1261\u12f2\u1235\u1275 \u1240\u1295 \u12a0\u1246\u1323\u1320\u122d" },
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
