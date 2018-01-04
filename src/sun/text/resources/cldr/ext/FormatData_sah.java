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

public class FormatData_sah extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u0422\u043e\u0445\u0441\u0443\u043d\u043d\u044c\u0443",
               "\u041e\u043b\u0443\u043d\u043d\u044c\u0443",
               "\u041a\u0443\u043b\u0443\u043d \u0442\u0443\u0442\u0430\u0440",
               "\u041c\u0443\u0443\u0441 \u0443\u0441\u0442\u0430\u0440",
               "\u042b\u0430\u043c \u044b\u0439\u044b\u043d",
               "\u0411\u044d\u0441 \u044b\u0439\u044b\u043d",
               "\u041e\u0442 \u044b\u0439\u044b\u043d",
               "\u0410\u0442\u044b\u0440\u0434\u044c\u044b\u0445 \u044b\u0439\u044b\u043d",
               "\u0411\u0430\u043b\u0430\u0495\u0430\u043d \u044b\u0439\u044b\u043d",
               "\u0410\u043b\u0442\u044b\u043d\u043d\u044c\u044b",
               "\u0421\u044d\u0442\u0438\u043d\u043d\u044c\u0438",
               "\u0430\u0445\u0441\u044b\u043d\u043d\u044c\u044b",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u0422\u043e\u0445\u0441",
               "\u041e\u043b\u0443\u043d",
               "\u041a\u043b\u043d",
               "\u041c\u0441\u0443",
               "\u042b\u0430\u043c",
               "\u0411\u044d\u0441",
               "\u041e\u0442\u0439",
               "\u0410\u0442\u0440",
               "\u0411\u043b\u0495",
               "\u0410\u043b\u0442",
               "\u0421\u044d\u0442",
               "\u0410\u0445\u0441",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u0422",
               "\u041e",
               "\u041a",
               "\u041c",
               "\u042b",
               "\u0411",
               "\u041e",
               "\u0410",
               "\u0411",
               "\u0410",
               "\u0421",
               "\u0410",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u0431\u0430\u0441\u043a\u044b\u04bb\u044b\u0430\u043d\u043d\u044c\u0430",
               "\u0431\u044d\u043d\u0438\u0434\u0438\u044d\u043d\u043d\u044c\u0438\u043a",
               "\u043e\u043f\u0442\u0443\u043e\u0440\u0443\u043d\u043d\u044c\u0443\u043a",
               "\u0441\u044d\u0440\u044d\u0434\u044d",
               "\u0447\u044d\u043f\u043f\u0438\u044d\u0440",
               "\u0411\u044d\u044d\u0442\u0438\u04a5\u0441\u044d",
               "\u0441\u0443\u0431\u0443\u043e\u0442\u0430",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u0431\u0441",
               "\u0431\u043d",
               "\u043e\u043f",
               "\u0441\u044d",
               "\u0447\u043f",
               "\u0431\u044d",
               "\u0441\u0431",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u0411",
               "\u0411",
               "\u041e",
               "\u0421",
               "\u0427",
               "\u0411",
               "\u0421",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "1-\u043a\u044b \u043a\u044b\u0431\u0430\u0430\u0440\u0442\u0430\u043b",
               "2-\u0441 \u043a\u044b\u0431\u0430\u0430\u0440\u0442\u0430\u043b",
               "3-\u0441 \u043a\u044b\u0431\u0430\u0430\u0440\u0442\u0430\u043b",
               "4-\u0441 \u043a\u044b\u0431\u0430\u0430\u0440\u0442\u0430\u043b",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "1-\u043a\u044b \u043a\u0431",
               "2-\u0441 \u043a\u0431",
               "3-\u0441 \u043a\u0431",
               "4-\u0441 \u043a\u0431",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u042d\u0418",
               "\u042d\u041a",
            };
        final String[] metaValue_long_Eras = new String[] {
               "\u0431. \u044d. \u0438.",
               "\u0431. \u044d",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "HH:mm:ss zzzz",
               "HH:mm:ss z",
               "HH:mm:ss",
               "HH:mm",
            };
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "G y '\u0441\u044b\u043b' MMMM d '\u043a\u04af\u043d\u044d', EEEE",
               "G y, MMMM d",
               "G y, MMM d",
               "GGGGG yy/M/d",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "GGGG y '\u0441\u044b\u043b' MMMM d '\u043a\u04af\u043d\u044d', EEEE",
               "GGGG y, MMMM d",
               "GGGG y, MMM d",
               "G yy/M/d",
            };
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u0421\u044b\u043b" },
            { "islamic.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u041a\u044d\u043c \u0431\u0430\u043b\u0430\u04bb\u0430\u0442\u0430" },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_long_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u0414\u044c\u043e\u043f\u043f\u0443\u043e\u043d \u0445\u0430\u043b\u0430\u043d\u0434\u0430\u0430\u0440\u0430" },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_long_Eras },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "roc.DayNames", metaValue_DayNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "long.Eras", metaValue_long_Eras },
            { "islamic.DayNames", metaValue_DayNames },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "field.weekday", "\u041d\u044d\u0434\u0438\u044d\u043b\u044d \u043a\u04af\u043d\u044d" },
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
                    ",",
                    "\u00a0",
                    ";",
                    "%",
                    "0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "\u0447\u044b\u044b\u04bb\u044b\u043b\u0430\u00a0\u0431\u0443\u043e\u0442\u0430\u0445",
                }
            },
            { "MonthNarrows", metaValue_MonthNarrows },
            { "japanese.DatePatterns", metaValue_buddhist_DatePatterns },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u041c\u04af\u043d\u04af\u04af\u0442\u044d" },
            { "field.era", "\u042d\u044d\u0440\u044d" },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u042d\u0418/\u042d\u041a" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "field.month", "\u042b\u0439" },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "field.second", "\u0421\u04e9\u043a\u04af\u04af\u043d\u0434\u044d" },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u041d\u044d\u0434\u0438\u044d\u043b\u044d" },
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
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "calendarname.islamic", "\u0418\u0441\u043b\u0430\u0430\u043c \u0445\u0430\u043b\u0430\u043d\u0434\u0430\u0430\u0440\u0430" },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "DatePatterns",
                new String[] {
                    "y '\u0441\u044b\u043b' MMMM d '\u043a\u04af\u043d\u044d', EEEE",
                    "y, MMMM d",
                    "y, MMM d",
                    "yy/M/d",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u0427\u0430\u0430\u0441" },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "calendarname.buddhist", "\u0411\u0443\u0443\u0434\u0434\u0430 \u0445\u0430\u043b\u0430\u043d\u0434\u0430\u0430\u0440\u0430" },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.MonthNames",
                new String[] {
                    "\u0442\u043e\u0445\u0441\u0443\u043d\u043d\u044c\u0443",
                    "\u043e\u043b\u0443\u043d\u043d\u044c\u0443",
                    "\u043a\u0443\u043b\u0443\u043d \u0442\u0443\u0442\u0430\u0440",
                    "\u043c\u0443\u0443\u0441 \u0443\u0441\u0442\u0430\u0440",
                    "\u044b\u0430\u043c \u044b\u0439\u0430",
                    "\u0431\u044d\u0441 \u044b\u0439\u0430",
                    "\u043e\u0442 \u044b\u0439\u0430",
                    "\u0430\u0442\u044b\u0440\u0434\u044c\u044b\u0445 \u044b\u0439\u0430",
                    "\u0431\u0430\u043b\u0430\u0495\u0430\u043d \u044b\u0439\u0430",
                    "\u0430\u043b\u0442\u044b\u043d\u043d\u044c\u044b",
                    "\u0441\u044d\u0442\u0438\u043d\u043d\u044c\u0438",
                    "\u0430\u0445\u0441\u044b\u043d\u043d\u044c\u044b",
                    "",
                }
            },
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
