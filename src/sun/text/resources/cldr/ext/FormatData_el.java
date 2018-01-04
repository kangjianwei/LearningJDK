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

public class FormatData_el extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u0399\u03b1\u03bd\u03bf\u03c5\u03b1\u03c1\u03af\u03bf\u03c5",
               "\u03a6\u03b5\u03b2\u03c1\u03bf\u03c5\u03b1\u03c1\u03af\u03bf\u03c5",
               "\u039c\u03b1\u03c1\u03c4\u03af\u03bf\u03c5",
               "\u0391\u03c0\u03c1\u03b9\u03bb\u03af\u03bf\u03c5",
               "\u039c\u03b1\u0390\u03bf\u03c5",
               "\u0399\u03bf\u03c5\u03bd\u03af\u03bf\u03c5",
               "\u0399\u03bf\u03c5\u03bb\u03af\u03bf\u03c5",
               "\u0391\u03c5\u03b3\u03bf\u03cd\u03c3\u03c4\u03bf\u03c5",
               "\u03a3\u03b5\u03c0\u03c4\u03b5\u03bc\u03b2\u03c1\u03af\u03bf\u03c5",
               "\u039f\u03ba\u03c4\u03c9\u03b2\u03c1\u03af\u03bf\u03c5",
               "\u039d\u03bf\u03b5\u03bc\u03b2\u03c1\u03af\u03bf\u03c5",
               "\u0394\u03b5\u03ba\u03b5\u03bc\u03b2\u03c1\u03af\u03bf\u03c5",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u0399\u03b1\u03bd",
               "\u03a6\u03b5\u03b2",
               "\u039c\u03b1\u03c1",
               "\u0391\u03c0\u03c1",
               "\u039c\u03b1\u0390",
               "\u0399\u03bf\u03c5\u03bd",
               "\u0399\u03bf\u03c5\u03bb",
               "\u0391\u03c5\u03b3",
               "\u03a3\u03b5\u03c0",
               "\u039f\u03ba\u03c4",
               "\u039d\u03bf\u03b5",
               "\u0394\u03b5\u03ba",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u0399",
               "\u03a6",
               "\u039c",
               "\u0391",
               "\u039c",
               "\u0399",
               "\u0399",
               "\u0391",
               "\u03a3",
               "\u039f",
               "\u039d",
               "\u0394",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u039a\u03c5\u03c1\u03b9\u03b1\u03ba\u03ae",
               "\u0394\u03b5\u03c5\u03c4\u03ad\u03c1\u03b1",
               "\u03a4\u03c1\u03af\u03c4\u03b7",
               "\u03a4\u03b5\u03c4\u03ac\u03c1\u03c4\u03b7",
               "\u03a0\u03ad\u03bc\u03c0\u03c4\u03b7",
               "\u03a0\u03b1\u03c1\u03b1\u03c3\u03ba\u03b5\u03c5\u03ae",
               "\u03a3\u03ac\u03b2\u03b2\u03b1\u03c4\u03bf",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u039a\u03c5\u03c1",
               "\u0394\u03b5\u03c5",
               "\u03a4\u03c1\u03af",
               "\u03a4\u03b5\u03c4",
               "\u03a0\u03ad\u03bc",
               "\u03a0\u03b1\u03c1",
               "\u03a3\u03ac\u03b2",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u039a",
               "\u0394",
               "\u03a4",
               "\u03a4",
               "\u03a0",
               "\u03a0",
               "\u03a3",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "1\u03bf \u03c4\u03c1\u03af\u03bc\u03b7\u03bd\u03bf",
               "2\u03bf \u03c4\u03c1\u03af\u03bc\u03b7\u03bd\u03bf",
               "3\u03bf \u03c4\u03c1\u03af\u03bc\u03b7\u03bd\u03bf",
               "4\u03bf \u03c4\u03c1\u03af\u03bc\u03b7\u03bd\u03bf",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "\u03a41",
               "\u03a42",
               "\u03a43",
               "\u03a44",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u03c0.\u03bc.",
               "\u03bc.\u03bc.",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "\u03c0\u03bc",
               "\u03bc\u03bc",
            };
        final String[] metaValue_Eras = new String[] {
               "\u03c0.\u03a7.",
               "\u03bc.\u03a7.",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "h:mm:ss a zzzz",
               "h:mm:ss a z",
               "h:mm:ss a",
               "h:mm a",
            };
        final String[] metaValue_DateTimePatterns = new String[] {
               "{1} - {0}",
               "{1} - {0}",
               "{1}, {0}",
               "{1}, {0}",
            };
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_buddhist_Eras = new String[] {
               "BC",
               "BE",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE, d MMMM y G",
               "d MMMM y G",
               "d MMM y G",
               "d/M/y GGGGG",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE, d MMMM y GGGG",
               "d MMMM y GGGG",
               "d MMM y GGGG",
               "d/M/y G",
            };
        final String[] metaValue_roc_long_Eras = new String[] {
               "\u03c0\u03c1\u03bf R.O.C.",
               "R.O.C.",
            };
        final String[] metaValue_islamic_long_Eras = new String[] {
               "",
               "\u0395.\u0395.",
            };
        final String metaValue_calendarname_gregorian = "\u0393\u03c1\u03b7\u03b3\u03bf\u03c1\u03b9\u03b1\u03bd\u03cc \u03b7\u03bc\u03b5\u03c1\u03bf\u03bb\u03cc\u03b3\u03b9\u03bf";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u03ad\u03c4\u03bf\u03c2" },
            { "calendarname.islamic-umalqura", "\u0399\u03c3\u03bb\u03b1\u03bc\u03b9\u03ba\u03cc \u03b7\u03bc\u03b5\u03c1\u03bf\u03bb\u03cc\u03b3\u03b9\u03bf (Umm al-Qura)" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.Eras", metaValue_buddhist_Eras },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns",
                new String[] {
                    "EEEE, d MMMM, y G",
                    "d MMMM, y G",
                    "d MMM, y G",
                    "d/M/yy",
                }
            },
            { "standalone.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.islamic-civil", "\u0399\u03c3\u03bb\u03b1\u03bc\u03b9\u03ba\u03cc \u03b7\u03bc\u03b5\u03c1\u03bf\u03bb\u03cc\u03b3\u03b9\u03bf (\u03c3\u03b5 \u03bc\u03bf\u03c1\u03c6\u03ae \u03c0\u03af\u03bd\u03b1\u03ba\u03b1, \u03b1\u03c3\u03c4\u03b9\u03ba\u03cc \u03b5\u03c0\u03bf\u03c7\u03ae\u03c2)" },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "roc.long.Eras", metaValue_roc_long_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "\u03a7\u03b5\u03b9\u03bc\u03b5\u03c1\u03b9\u03bd\u03ae \u03ce\u03c1\u03b1 ({0})" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u0399\u03b1\u03c0\u03c9\u03bd\u03b9\u03ba\u03cc \u03b7\u03bc\u03b5\u03c1\u03bf\u03bb\u03cc\u03b3\u03b9\u03bf" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "long.Eras",
                new String[] {
                    "\u03c0\u03c1\u03bf \u03a7\u03c1\u03b9\u03c3\u03c4\u03bf\u03cd",
                    "\u03bc\u03b5\u03c4\u03ac \u03a7\u03c1\u03b9\u03c3\u03c4\u03cc\u03bd",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns", metaValue_DateTimePatterns },
            { "narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "latn.NumberElements",
                new String[] {
                    ",",
                    ".",
                    ";",
                    "%",
                    "0",
                    "#",
                    "-",
                    "e",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "MonthNarrows", metaValue_MonthNarrows },
            { "japanese.DatePatterns",
                new String[] {
                    "EEEE, d MMMM, y GGGG",
                    "d MMMM, y GGGG",
                    "d MMM, y GGGG",
                    "d/M/yy",
                }
            },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u03bb\u03b5\u03c0\u03c4\u03cc" },
            { "field.era", "\u03c0\u03b5\u03c1\u03af\u03bf\u03b4\u03bf\u03c2" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u03c0.\u03bc./\u03bc.\u03bc." },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.roc", "\u0397\u03bc\u03b5\u03c1\u03bf\u03bb\u03cc\u03b3\u03b9\u03bf \u03c4\u03b7\u03c2 \u0394\u03b7\u03bc\u03bf\u03ba\u03c1\u03b1\u03c4\u03af\u03b1\u03c2 \u03c4\u03b7\u03c2 \u039a\u03af\u03bd\u03b1\u03c2" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "islamic.Eras", metaValue_islamic_long_Eras },
            { "field.month", "\u03bc\u03ae\u03bd\u03b1\u03c2" },
            { "roc.Eras", metaValue_roc_long_Eras },
            { "field.second", "\u03b4\u03b5\u03c5\u03c4\u03b5\u03c1\u03cc\u03bb\u03b5\u03c0\u03c4\u03bf" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "#,##0.00\u00a0\u00a4",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "calendarname.islamic", "\u0399\u03c3\u03bb\u03b1\u03bc\u03b9\u03ba\u03cc \u03b7\u03bc\u03b5\u03c1\u03bf\u03bb\u03cc\u03b3\u03b9\u03bf" },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations",
                new String[] {
                    "\u0399\u03b1\u03bd",
                    "\u03a6\u03b5\u03b2",
                    "\u039c\u03ac\u03c1",
                    "\u0391\u03c0\u03c1",
                    "\u039c\u03ac\u03b9",
                    "\u0399\u03bf\u03cd\u03bd",
                    "\u0399\u03bf\u03cd\u03bb",
                    "\u0391\u03cd\u03b3",
                    "\u03a3\u03b5\u03c0",
                    "\u039f\u03ba\u03c4",
                    "\u039d\u03bf\u03ad",
                    "\u0394\u03b5\u03ba",
                    "",
                }
            },
            { "timezone.regionFormat", "\u038f\u03c1\u03b1 ({0})" },
            { "roc.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.long.Eras",
                new String[] {
                    "BC",
                    "\u0392.\u0395.",
                }
            },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u03b6\u03ce\u03bd\u03b7 \u03ce\u03c1\u03b1\u03c2" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.narrow.Eras", metaValue_roc_long_Eras },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "\u03ba\u03b1\u03b8\u03b7\u03bc\u03b5\u03c1\u03b9\u03bd\u03ae" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.long.Eras", metaValue_islamic_long_Eras },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "buddhist.Eras", metaValue_buddhist_Eras },
            { "field.week", "\u03b5\u03b2\u03b4\u03bf\u03bc\u03ac\u03b4\u03b1" },
            { "buddhist.DateTimePatterns", metaValue_DateTimePatterns },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "\u0398\u03b5\u03c1\u03b9\u03bd\u03ae \u03ce\u03c1\u03b1 ({0})" },
            { "DatePatterns",
                new String[] {
                    "EEEE, d MMMM y",
                    "d MMMM y",
                    "d MMM y",
                    "d/M/yy",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u03ce\u03c1\u03b1" },
            { "islamic.narrow.Eras", metaValue_islamic_long_Eras },
            { "calendarname.buddhist", "\u0392\u03bf\u03c5\u03b4\u03b9\u03c3\u03c4\u03b9\u03ba\u03cc \u03b7\u03bc\u03b5\u03c1\u03bf\u03bb\u03cc\u03b3\u03b9\u03bf" },
            { "standalone.MonthNames",
                new String[] {
                    "\u0399\u03b1\u03bd\u03bf\u03c5\u03ac\u03c1\u03b9\u03bf\u03c2",
                    "\u03a6\u03b5\u03b2\u03c1\u03bf\u03c5\u03ac\u03c1\u03b9\u03bf\u03c2",
                    "\u039c\u03ac\u03c1\u03c4\u03b9\u03bf\u03c2",
                    "\u0391\u03c0\u03c1\u03af\u03bb\u03b9\u03bf\u03c2",
                    "\u039c\u03ac\u03b9\u03bf\u03c2",
                    "\u0399\u03bf\u03cd\u03bd\u03b9\u03bf\u03c2",
                    "\u0399\u03bf\u03cd\u03bb\u03b9\u03bf\u03c2",
                    "\u0391\u03cd\u03b3\u03bf\u03c5\u03c3\u03c4\u03bf\u03c2",
                    "\u03a3\u03b5\u03c0\u03c4\u03ad\u03bc\u03b2\u03c1\u03b9\u03bf\u03c2",
                    "\u039f\u03ba\u03c4\u03ce\u03b2\u03c1\u03b9\u03bf\u03c2",
                    "\u039d\u03bf\u03ad\u03bc\u03b2\u03c1\u03b9\u03bf\u03c2",
                    "\u0394\u03b5\u03ba\u03ad\u03bc\u03b2\u03c1\u03b9\u03bf\u03c2",
                    "",
                }
            },
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
