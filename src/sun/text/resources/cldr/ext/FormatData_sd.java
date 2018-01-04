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

public class FormatData_sd extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u062c\u0646\u0648\u0631\u064a",
               "\u0641\u064a\u0628\u0631\u0648\u0631\u064a",
               "\u0645\u0627\u0631\u0686",
               "\u0627\u067e\u0631\u064a\u0644",
               "\u0645\u0626\u064a",
               "\u062c\u0648\u0646",
               "\u062c\u0648\u0644\u0627\u0621\u0650",
               "\u0622\u06af\u0633\u067d",
               "\u0633\u064a\u067e\u067d\u0645\u0628\u0631",
               "\u0622\u06aa\u067d\u0648\u0628\u0631",
               "\u0646\u0648\u0645\u0628\u0631",
               "\u068a\u0633\u0645\u0628\u0631",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u0622\u0686\u0631",
               "\u0633\u0648\u0645\u0631",
               "\u0627\u06b1\u0627\u0631\u0648",
               "\u0627\u0631\u0628\u0639",
               "\u062e\u0645\u064a\u0633",
               "\u062c\u0645\u0639\u0648",
               "\u0687\u0646\u0687\u0631",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u0622\u0686\u0631",
               "\u0633\u0648",
               "\u0627\u06b1\u0627\u0631\u0648",
               "\u0627\u0631\u0628\u0639",
               "\u062e\u0645",
               "\u062c\u0645\u0639\u0648",
               "\u0687\u0646\u0687\u0631",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u067e\u0647\u0631\u064a\u0646 \u067d\u064a \u0645\u0627\u0647\u064a",
               "\u067b\u064a\u0646 \u067d\u064a \u0645\u0627\u0647\u064a",
               "\u067d\u064a\u0646 \u067d\u064a \u0645\u0627\u0647\u064a",
               "\u0686\u0648\u067f\u064a\u0646 \u067d\u064a \u0645\u0627\u0647\u064a",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u0635\u0628\u062d\u060c \u0645\u0646\u062c\u0647\u0646\u062f",
               "\u0645\u0646\u062c\u0647\u0646\u062f\u060c \u0634\u0627\u0645",
            };
        final String[] metaValue_abbreviated_AmPmMarkers = new String[] {
               "\u0635\u0628\u062d\u060c \u0645\u0646\u062c\u0647\u0646\u062f",
               "\u0634\u0627\u0645\u060c \u0645\u0646\u062c\u0647\u0646\u062f",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "h:mm:ss a zzzz",
               "h:mm:ss a z",
               "h:mm:ss a",
               "h:mm a",
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
        final String metaValue_calendarname_gregorian = "\u062c\u0627\u0631\u062c\u064a\u0627\u0626\u064a \u06aa\u0626\u0644\u064a\u0646\u068a\u0631";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u0633\u0627\u0644" },
            { "islamic.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.DayAbbreviations", metaValue_DayNames },
            { "standalone.DayNarrows",
                new String[] {
                    "\u0622\u0686",
                    "\u0633\u0648",
                    "\u0627\u06b1",
                    "\u0627\u0631",
                    "\u062e\u0645",
                    "\u062c\u0645",
                    "\u0687\u0646",
                }
            },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u067d\u0627\u0626\u064a\u0645 \u0632\u0648\u0646" },
            { "roc.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "japanese.QuarterAbbreviations", metaValue_QuarterNames },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "DefaultNumberingSystem", "arab" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "calendarname.japanese", "\u062c\u0627\u067e\u0627\u0646\u064a \u06aa\u0626\u0644\u064a\u0646\u068a\u0631" },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "roc.DayNames", metaValue_DayNames },
            { "standalone.DayAbbreviations", metaValue_DayNames },
            { "roc.MonthAbbreviations", metaValue_MonthNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "long.Eras",
                new String[] {
                    "\u0645\u0633\u064a\u062d \u06a9\u0627\u0646 \u0627\u06b3",
                    "\u0639\u064a\u0633\u0648\u064a \u06a9\u0627\u0646 \u067e\u0647\u0631\u064a\u0646",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "buddhist.MonthAbbreviations", metaValue_MonthNames },
            { "field.weekday", "\u0647\u0641\u062a\u064a \u062c\u0648 \u068f\u064a\u0646\u0647\u0646" },
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
            { "japanese.DatePatterns", metaValue_buddhist_DatePatterns },
            { "japanese.MonthAbbreviations", metaValue_MonthNames },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u0645\u0646\u067d" },
            { "field.era", "\u062f\u0648\u0631" },
            { "islamic.DayAbbreviations", metaValue_DayNames },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u0635\u0628\u062d\u060c \u0645\u0646\u062c\u0647\u0646\u062f/\u0645\u0646\u062c\u0647\u0646\u062f\u060c \u0634\u0627\u0645" },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterNames },
            { "calendarname.roc", "\u0645\u0646\u06af\u0648\u0648 \u06aa\u0626\u0644\u064a\u0646\u068a\u0631" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterNames },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayNames },
            { "DayNames", metaValue_DayNames },
            { "field.month", "\u0645\u0647\u064a\u0646\u0648" },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "field.second", "\u0633\u064a\u06aa\u0646\u068a" },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u0647\u0641\u062a\u0648" },
            { "DayAbbreviations", metaValue_DayNames },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "\u00a4\u00a0#,##0.00",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "buddhist.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "calendarname.islamic", "\u0627\u0633\u0644\u0627\u0645\u064a \u06aa\u0626\u0644\u064a\u0646\u068a\u0631" },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "DatePatterns",
                new String[] {
                    "y MMMM d, EEEE",
                    "y MMMM d",
                    "y MMM d",
                    "y-MM-dd",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayNames },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthNames },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u06aa\u0644\u0627\u06aa" },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "calendarname.buddhist", "\u067b\u068c \u068c\u0631\u0645 \u062c\u0648 \u06aa\u0626\u0644\u064a\u0646\u068a\u0631" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "standalone.MonthAbbreviations", metaValue_MonthNames },
            { "timezone.regionFormat", "{0} \u0648\u0642\u062a" },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterNames },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "roc.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
