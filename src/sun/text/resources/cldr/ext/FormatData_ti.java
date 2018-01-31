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

public class FormatData_ti extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u1325\u122a",
               "\u1208\u12ab\u1272\u1275",
               "\u1218\u130b\u1262\u1275",
               "\u121a\u12eb\u12dd\u12eb",
               "\u130d\u1295\u1266\u1275",
               "\u1230\u1290",
               "\u1213\u121d\u1208",
               "\u1290\u1213\u1230",
               "\u1218\u1235\u12a8\u1228\u121d",
               "\u1325\u1245\u121d\u1272",
               "\u1215\u12f3\u122d",
               "\u1273\u1215\u1233\u1235",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u1325\u122a",
               "\u1208\u12ab",
               "\u1218\u130b",
               "\u121a\u12eb",
               "\u130d\u1295",
               "\u1230\u1290",
               "\u1213\u121d",
               "\u1290\u1213",
               "\u1218\u1235",
               "\u1325\u1245",
               "\u1215\u12f3",
               "\u1273\u1215",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u1325",
               "\u1208",
               "\u1218",
               "\u121a",
               "\u130d",
               "\u1230",
               "\u1213",
               "\u1290",
               "\u1218",
               "\u1325",
               "\u1215",
               "\u1273",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u1230\u1295\u1260\u1275",
               "\u1230\u1291\u12ed",
               "\u1220\u1209\u1235",
               "\u1228\u1261\u12d5",
               "\u1283\u1219\u1235",
               "\u12d3\u122d\u1262",
               "\u1240\u12f3\u121d",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u1230\u1295",
               "\u1230\u1291",
               "\u1230\u1209",
               "\u1228\u1261",
               "\u1213\u1219",
               "\u12d3\u122d",
               "\u1240\u12f3",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u1230",
               "\u1230",
               "\u1230",
               "\u1228",
               "\u1213",
               "\u12d3",
               "\u1240",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u1240\u12f3\u121b\u12ed \u122d\u1265\u12d2",
               "\u12ab\u120d\u12a3\u12ed \u122d\u1265\u12d2",
               "\u1233\u120d\u1233\u12ed \u122d\u1265\u12d2",
               "\u122b\u1265\u12d3\u12ed \u122d\u1265\u12d2",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "\u122d1",
               "\u122d2",
               "\u122d3",
               "\u122d4",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u1295\u1309\u1206 \u1230\u12d3\u1270",
               "\u12f5\u1215\u122d \u1230\u12d3\u1275",
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
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "G y MMMM d, EEEE",
               "dd MMMM y G",
               "dd-MMM-y G",
               "dd/MM/yy GGGGG",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "GGGG y MMMM d, EEEE",
               "dd MMMM y GGGG",
               "dd-MMM-y GGGG",
               "dd/MM/yy G",
            };
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u12d3\u1218\u1275" },
            { "islamic.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows",
                new String[] {
                    "\u1230",
                    "\u1230",
                    "\u1220",
                    "\u1228",
                    "\u1213",
                    "\u12d3",
                    "\u1240",
                }
            },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u12ad\u120d\u120d" },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "roc.DayNames", metaValue_DayNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "long.Eras",
                new String[] {
                    "\u12d3/\u12d3",
                    "\u12d3\u1218\u1270 \u121d\u1205\u1228\u1275",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "field.weekday", "\u1218\u12d3\u120d\u1272 \u1293\u12ed \u1230\u1219\u1295" },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}",
                    "{1} {0}",
                    "{1} {0}",
                    "{1} {0}",
                }
            },
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
                    "NaN",
                }
            },
            { "MonthNarrows", metaValue_MonthNarrows },
            { "japanese.DatePatterns", metaValue_buddhist_DatePatterns },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u12f0\u1252\u1255" },
            { "field.era", "\u12d8\u1218\u1295" },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u12ad\u134d\u1208 \u1218\u12d3\u120d\u1272" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "field.month", "\u12c8\u122d\u1212" },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "field.second", "\u12ab\u120d\u12a2\u1275" },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u1230\u1219\u1295" },
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
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "DatePatterns",
                new String[] {
                    "EEEE\u1363 dd MMMM \u1218\u12d3\u120d\u1272 y G",
                    "dd MMMM y",
                    "dd-MMM-y",
                    "dd/MM/yy",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames",
                new String[] {
                    "\u1230\u1295\u1260\u1275",
                    "\u1230\u1291\u12ed",
                    "\u1230\u1209\u1235",
                    "\u1228\u1261\u12d5",
                    "\u1213\u1219\u1235",
                    "\u12d3\u122d\u1262",
                    "\u1240\u12f3\u121d",
                }
            },
            { "field.hour", "\u1230\u12d3\u1275" },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
        };
        return data;
    }
}
