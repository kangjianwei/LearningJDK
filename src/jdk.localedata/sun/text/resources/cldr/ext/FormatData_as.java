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

public class FormatData_as extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u099c\u09be\u09a8\u09c1\u09f1\u09be\u09f0\u09c0",
               "\u09ab\u09c7\u09ac\u09cd\u09f0\u09c1\u09f1\u09be\u09f0\u09c0",
               "\u09ae\u09be\u09f0\u09cd\u099a",
               "\u098f\u09aa\u09cd\u09f0\u09bf\u09b2",
               "\u09ae\u09c7\u2019",
               "\u099c\u09c1\u09a8",
               "\u099c\u09c1\u09b2\u09be\u0987",
               "\u0986\u0997\u09b7\u09cd\u099f",
               "\u099b\u09c7\u09aa\u09cd\u09a4\u09c7\u09ae\u09cd\u09ac\u09f0",
               "\u0985\u0995\u09cd\u099f\u09cb\u09ac\u09f0",
               "\u09a8\u09f1\u09c7\u09ae\u09cd\u09ac\u09f0",
               "\u09a1\u09bf\u099a\u09c7\u09ae\u09cd\u09ac\u09f0",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u099c\u09be\u09a8\u09c1",
               "\u09ab\u09c7\u09ac\u09cd\u09f0\u09c1",
               "\u09ae\u09be\u09f0\u09cd\u099a",
               "\u098f\u09aa\u09cd\u09f0\u09bf\u09b2",
               "\u09ae\u09c7\u2019",
               "\u099c\u09c1\u09a8",
               "\u099c\u09c1\u09b2\u09be\u0987",
               "\u0986\u0997",
               "\u099b\u09c7\u09aa\u09cd\u09a4\u09c7",
               "\u0985\u0995\u09cd\u099f\u09cb",
               "\u09a8\u09f1\u09c7",
               "\u09a1\u09bf\u099a\u09c7",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u099c",
               "\u09ab",
               "\u09ae",
               "\u098f",
               "\u09ae",
               "\u099c",
               "\u099c",
               "\u0986",
               "\u099b",
               "\u0985",
               "\u09a8",
               "\u09a1",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u09a6\u09c7\u0993\u09ac\u09be\u09f0",
               "\u09b8\u09cb\u09ae\u09ac\u09be\u09f0",
               "\u09ae\u0999\u09cd\u0997\u09b2\u09ac\u09be\u09f0",
               "\u09ac\u09c1\u09a7\u09ac\u09be\u09f0",
               "\u09ac\u09c3\u09b9\u09b8\u09cd\u09aa\u09a4\u09bf\u09ac\u09be\u09f0",
               "\u09b6\u09c1\u0995\u09cd\u09f0\u09ac\u09be\u09f0",
               "\u09b6\u09a8\u09bf\u09ac\u09be\u09f0",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u09a6\u09c7\u0993",
               "\u09b8\u09cb\u09ae",
               "\u09ae\u0999\u09cd\u0997\u09b2",
               "\u09ac\u09c1\u09a7",
               "\u09ac\u09c3\u09b9",
               "\u09b6\u09c1\u0995\u09cd\u09f0",
               "\u09b6\u09a8\u09bf",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u09a6",
               "\u09b8",
               "\u09ae",
               "\u09ac",
               "\u09ac",
               "\u09b6",
               "\u09b6",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u09aa\u09cd\u09f0\u09a5\u09ae \u09a4\u09bf\u09a8\u09bf\u09ae\u09be\u09b9",
               "\u09a6\u09cd\u09ac\u09bf\u09a4\u09c0\u09af\u09bc \u09a4\u09bf\u09a8\u09bf\u09ae\u09be\u09b9",
               "\u09a4\u09c3\u09a4\u09c0\u09af\u09bc \u09a4\u09bf\u09a8\u09bf\u09ae\u09be\u09b9",
               "\u099a\u09a4\u09c1\u09f0\u09cd\u09a5 \u09a4\u09bf\u09a8\u09bf\u09ae\u09be\u09b9",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "\u09e7\u09ae\u0983 \u09a4\u09bf\u0983",
               "\u09e8\u09af\u09bc\u0983 \u09a4\u09bf\u0983",
               "\u09e9\u09af\u09bc\u0983 \u09a4\u09bf\u0983",
               "\u09ea\u09f0\u09cd\u09a5\u0983 \u09a4\u09bf\u0983",
            };
        final String[] metaValue_QuarterNarrows = new String[] {
               "\u09e7",
               "\u09e8",
               "\u09e9",
               "\u09ea",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u09aa\u09c2\u09f0\u09cd\u09ac\u09be\u09b9\u09cd\u09a8",
               "\u0985\u09aa\u09f0\u09be\u09b9\u09cd\u09a8",
            };
        final String[] metaValue_Eras = new String[] {
               "\u0996\u09cd\u09f0\u09c0\u0983 \u09aa\u09c2\u0983",
               "\u0996\u09cd\u09f0\u09c0\u0983",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "a h.mm.ss zzzz",
               "a h.mm.ss z",
               "a h.mm.ss",
               "a h.mm",
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
        final String metaValue_calendarname_gregorian = "\u0997\u09cd\u09f0\u09c7\u0997\u09cb\u09f0\u09bf\u09af\u09bc\u09be\u09a8 \u0995\u09c7\u09b2\u09c7\u09a3\u09cd\u09a1\u09be\u09f0";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u09ac\u099b\u09f0" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.islamic-civil", "\u0987\u099a\u09b2\u09be\u09ae\u09c0-\u09a8\u09be\u0997\u09f0\u09bf\u0995\u09f0 \u09aa\u099e\u09cd\u099c\u09bf\u0995\u09be" },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "{0} (+0) \u09ae\u09be\u09a8 \u09b8\u09ae\u09af\u09bc" },
            { "DefaultNumberingSystem", "beng" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u099c\u09be\u09aa\u09be\u09a8\u09c0 \u0995\u09c7\u09b2\u09c7\u09a3\u09cd\u09a1\u09be\u09f0" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "beng.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u09e6",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "long.Eras",
                new String[] {
                    "\u0996\u09cd\u09f0\u09c0\u09b7\u09cd\u099f\u09aa\u09c2\u09f0\u09cd\u09ac",
                    "\u0996\u09cd\u09f0\u09c0\u09b7\u09cd\u099f\u09be\u09ac\u09cd\u09a6",
                }
            },
            { "roc.QuarterNarrows", metaValue_QuarterNarrows },
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
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u09ae\u09bf\u09a8\u09bf\u099f" },
            { "field.era", "\u09af\u09c1\u0997" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u09aa\u09c2\u09f0\u09cd\u09ac\u09be\u09b9\u09cd\u09a8/\u0985\u09aa\u09f0\u09be\u09b9\u09cd\u09a8" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "japanese.QuarterNarrows", metaValue_QuarterNarrows },
            { "calendarname.roc", "\u099a\u09c0\u09a8\u09be \u09aa\u09cd\u09f0\u099c\u09be\u09a4\u09a8\u09cd\u09a4\u09cd\u09f0\u09f0 \u0995\u09c7\u09b2\u09c7\u09a3\u09cd\u09a1\u09be\u09f0" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "field.month", "\u09ae\u09be\u09b9" },
            { "field.second", "\u099b\u09c7\u0995\u09c7\u09a3\u09cd\u09a1" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##,##0.###",
                    "\u00a4\u00a0#,##,##0.00",
                    "#,##,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "calendarname.islamic", "\u0987\u099b\u09b2\u09be\u09ae\u09c0 \u0995\u09c7\u09b2\u09c7\u09a3\u09cd\u09a1\u09be\u09f0" },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "timezone.regionFormat", "{0} \u09b8\u09ae\u09af\u09bc" },
            { "buddhist.QuarterNarrows", metaValue_QuarterNarrows },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u09b8\u09ae\u09af\u09bc \u0995\u09cd\u09b7\u09c7\u09a4\u09cd\u09f0" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "QuarterNarrows", metaValue_QuarterNarrows },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "standalone.QuarterNarrows", metaValue_QuarterNarrows },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "\u09b8\u09aa\u09cd\u09a4\u09be\u09b9\u09f0 \u09a6\u09bf\u09a8" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u09b8\u09aa\u09cd\u09a4\u09be\u09b9" },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0} (+1) \u09a1\u09c7\u09b2\u09be\u0987\u099f \u09b8\u09ae\u09af\u09bc" },
            { "DatePatterns",
                new String[] {
                    "EEEE, d MMMM, y",
                    "d MMMM, y",
                    "dd-MM-y",
                    "d-M-y",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u0998\u09a3\u09cd\u099f\u09be" },
            { "calendarname.buddhist", "\u09ac\u09cc\u09a6\u09cd\u09a7 \u0995\u09c7\u09b2\u09c7\u09a3\u09cd\u09a1\u09be\u09f0" },
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
