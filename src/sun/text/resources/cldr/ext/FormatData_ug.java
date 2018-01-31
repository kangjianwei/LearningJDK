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

public class FormatData_ug extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u064a\u0627\u0646\u06cb\u0627\u0631",
               "\u0641\u06d0\u06cb\u0631\u0627\u0644",
               "\u0645\u0627\u0631\u062a",
               "\u0626\u0627\u067e\u0631\u06d0\u0644",
               "\u0645\u0627\u064a",
               "\u0626\u0649\u064a\u06c7\u0646",
               "\u0626\u0649\u064a\u06c7\u0644",
               "\u0626\u0627\u06cb\u063a\u06c7\u0633\u062a",
               "\u0633\u06d0\u0646\u062a\u06d5\u0628\u0649\u0631",
               "\u0626\u06c6\u0643\u062a\u06d5\u0628\u0649\u0631",
               "\u0646\u0648\u064a\u0627\u0628\u0649\u0631",
               "\u062f\u06d0\u0643\u0627\u0628\u0649\u0631",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u064a\u06d5\u0643\u0634\u06d5\u0646\u0628\u06d5",
               "\u062f\u06c8\u0634\u06d5\u0646\u0628\u06d5",
               "\u0633\u06d5\u064a\u0634\u06d5\u0646\u0628\u06d5",
               "\u0686\u0627\u0631\u0634\u06d5\u0646\u0628\u06d5",
               "\u067e\u06d5\u064a\u0634\u06d5\u0646\u0628\u06d5",
               "\u062c\u06c8\u0645\u06d5",
               "\u0634\u06d5\u0646\u0628\u06d5",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u064a\u06d5",
               "\u062f\u06c8",
               "\u0633\u06d5",
               "\u0686\u0627",
               "\u067e\u06d5",
               "\u062c\u06c8",
               "\u0634\u06d5",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u064a",
               "\u062f",
               "\u0633",
               "\u0686",
               "\u067e",
               "\u062c",
               "\u0634",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u0628\u0649\u0631\u0649\u0646\u0686\u0649 \u067e\u06d5\u0633\u0649\u0644",
               "\u0626\u0649\u0643\u0643\u0649\u0646\u0686\u0649 \u067e\u06d5\u0633\u0649\u0644",
               "\u0626\u06c8\u0686\u0649\u0646\u0686\u0649 \u067e\u06d5\u0633\u0649\u0644",
               "\u062a\u06c6\u062a\u0649\u0646\u0686\u0649 \u067e\u06d5\u0633\u0649\u0644",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "1-\u067e\u06d5\u0633\u0649\u0644",
               "2-\u067e\u06d5\u0633\u0649\u0644",
               "3-\u067e\u06d5\u0633\u0649\u0644",
               "4-\u067e\u06d5\u0633\u0649\u0644",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u0686\u06c8\u0634\u062a\u0649\u0646 \u0628\u06c7\u0631\u06c7\u0646",
               "\u0686\u06c8\u0634\u062a\u0649\u0646 \u0643\u06d0\u064a\u0649\u0646",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "\u0628",
               "\u0643",
            };
        final String[] metaValue_abbreviated_AmPmMarkers = new String[] {
               "\u0686.\u0628",
               "\u0686.\u0643",
            };
        final String[] metaValue_Eras = new String[] {
               "BCE",
               "\u0645\u0649\u0644\u0627\u062f\u0649\u064a\u06d5",
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
        final String[] metaValue_buddhist_Eras = new String[] {
               "BC",
               "BE",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE\u060c MMMM d\u060c y G",
               "MMMM d\u060c y G",
               "MMM d\u060c y G",
               "M/d/y GGGGG",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE\u060c MMMM d\u060c y GGGG",
               "MMMM d\u060c y GGGG",
               "MMM d\u060c y GGGG",
               "M/d/y G",
            };
        final String[] metaValue_roc_Eras = new String[] {
               "Before R.O.C.",
               "\u0645\u0649\u0646\u06af\u0648",
            };
        final String[] metaValue_islamic_MonthNames = new String[] {
               "\u0645\u06c7\u06be\u06d5\u0631\u0631\u06d5\u0645",
               "\u0633\u06d5\u067e\u06d5\u0631",
               "\u0631\u06d5\u0628\u0649\u0626\u06c7\u0644\u0626\u06d5\u06cb\u06cb\u06d5\u0644",
               "\u0631\u06d5\u0628\u0649\u0626\u06c7\u0644\u0626\u0627\u062e\u0649\u0631",
               "\u062c\u06d5\u0645\u0627\u062f\u0649\u064a\u06d5\u0644\u0626\u06d5\u06cb\u06cb\u06d5\u0644",
               "\u062c\u06d5\u0645\u0627\u062f\u0649\u064a\u06d5\u0644\u0626\u0627\u062e\u0649\u0631",
               "\u0631\u06d5\u062c\u06d5\u0628",
               "\u0634\u06d5\u0626\u0628\u0627\u0646",
               "\u0631\u0627\u0645\u0649\u0632\u0627\u0646",
               "\u0634\u06d5\u06cb\u06cb\u0627\u0644",
               "\u0632\u06c7\u0644\u0642\u06d5\u0626\u062f\u06d5",
               "\u0632\u06c7\u0644\u06be\u06d5\u062c\u062c\u06d5",
               "",
            };
        final String[] metaValue_islamic_long_Eras = new String[] {
               "",
               "\u06be\u0649\u062c\u0631\u0649\u064a\u06d5",
            };
        final String metaValue_calendarname_gregorian = "\u0645\u0649\u0644\u0627\u062f\u0649\u064a\u06d5 \u064a\u0649\u0644\u0646\u0627\u0645\u06d5\u0633\u0649";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u064a\u0649\u0644" },
            { "calendarname.islamic-umalqura", "\u0626\u0649\u0633\u0644\u0627\u0645 \u06be\u0649\u062c\u0631\u0649\u064a\u06d5 \u064a\u0649\u0644\u0646\u0627\u0645\u06d5\u0633\u0649 (\u0626\u06c7\u0645 \u0626\u06d5\u0644\u0642\u06c7\u0631\u0627)" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.Eras", metaValue_buddhist_Eras },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns",
                new String[] {
                    "EEEE, MMMM d\u060c y G",
                    "MMMM d\u060c y G",
                    "MMM d\u060c y G",
                    "M/d/y GGGGG",
                }
            },
            { "standalone.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "calendarname.islamic-civil", "\u0626\u0649\u0633\u0644\u0627\u0645 \u06be\u0649\u062c\u0631\u0649\u064a\u06d5 \u064a\u0649\u0644\u0646\u0627\u0645\u06d5\u0633\u0649 (\u0645\u06d5\u062f\u0649\u0646\u06d5)" },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "roc.long.Eras",
                new String[] {
                    "\u062c\u06c7\u06ad\u062e\u06c7\u0627 \u0645\u0649\u0646\u06af\u0648\u062f\u0649\u0646 \u0628\u06c7\u0631\u06c7\u0646",
                    "\u0645\u0649\u0646\u06af\u0648",
                }
            },
            { "abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "timezone.regionFormat.standard", "{0} \u0626\u06c6\u0644\u0686\u06d5\u0645\u0644\u0649\u0643 \u06cb\u0627\u0642\u062a\u0649" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "calendarname.japanese", "\u064a\u0627\u067e\u0648\u0646\u0649\u064a\u06d5 \u064a\u0649\u0644\u0646\u0627\u0645\u06d5\u0633\u0649" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthNames },
            { "long.Eras",
                new String[] {
                    "\u0645\u0649\u0644\u0627\u062f\u0649\u064a\u06d5\u062f\u0649\u0646 \u0628\u06c7\u0631\u06c7\u0646",
                    "\u0645\u0649\u0644\u0627\u062f\u0649\u064a\u06d5",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthNames },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}",
                    "{1} {0}",
                    "{1}\u060c {0}",
                    "{1}\u060c {0}",
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
            { "japanese.DatePatterns",
                new String[] {
                    "EEEE, MMMM d\u060c y GGGG",
                    "MMMM d\u060c y GGGG",
                    "MMM d\u060c y GGGG",
                    "M/d/y G",
                }
            },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u0645\u0649\u0646\u06c7\u062a" },
            { "field.era", "\u0645\u0649\u0644\u0627\u062f\u0649\u064a\u06d5" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u0686\u06c8\u0634\u062a\u0649\u0646 \u0628\u06c7\u0631\u06c7\u0646/\u0686\u06c8\u0634\u062a\u0649\u0646 \u0643\u06d0\u064a\u0649\u0646" },
            { "islamic.MonthNames", metaValue_islamic_MonthNames },
            { "calendarname.roc", "\u0645\u0649\u0646\u06af\u0648 \u064a\u0649\u0644\u0646\u0627\u0645\u06d5\u0633\u0649" },
            { "islamic.DatePatterns",
                new String[] {
                    "EEEE\u060c d MMMM\u060c y GGGG",
                    "d MMMM\u060c y GGGG",
                    "d MMM\u060c y GGGG",
                    "d\u200f/M\u200f/y G",
                }
            },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "islamic.Eras", metaValue_islamic_long_Eras },
            { "field.month", "\u0626\u0627\u064a" },
            { "roc.Eras", metaValue_roc_Eras },
            { "field.second", "\u0633\u06d0\u0643\u06c7\u0646\u062a" },
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
            { "calendarname.islamic", "\u0626\u0649\u0633\u0644\u0627\u0645 \u06be\u0649\u062c\u0631\u0649\u064a\u06d5 \u064a\u0649\u0644\u0646\u0627\u0645\u06d5\u0633\u0649" },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthNames },
            { "timezone.regionFormat", "{0} \u06cb\u0627\u0642\u062a\u0649" },
            { "roc.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.long.Eras",
                new String[] {
                    "BC",
                    "\u0628\u06c7\u062f\u062f\u0627 \u064a\u0649\u0644\u0646\u0627\u0645\u06d5\u0633\u0649",
                }
            },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u06cb\u0627\u0642\u0649\u062a \u0631\u0627\u064a\u0648\u0646\u0649" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.narrow.Eras", metaValue_roc_Eras },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "java.time.islamic.DatePatterns",
                new String[] {
                    "EEEE\u060c d MMMM\u060c y G",
                    "d MMMM\u060c y G",
                    "d MMM\u060c y G",
                    "d\u200f/M\u200f/y GGGGG",
                }
            },
            { "field.weekday", "\u06be\u06d5\u067e\u062a\u06d5 \u0643\u06c8\u0646\u0644\u0649\u0631\u0649" },
            { "japanese.MonthAbbreviations", metaValue_MonthNames },
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
            { "field.week", "\u06be\u06d5\u067e\u062a\u06d5" },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0} \u064a\u0627\u0632\u0644\u0649\u0642 \u06cb\u0627\u0642\u062a\u0649" },
            { "DatePatterns",
                new String[] {
                    "y d-MMMM\u060c EEEE",
                    "d-MMMM\u060c y",
                    "d-MMM\u060c y",
                    "y-MM-dd",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthNames },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u0633\u0627\u0626\u06d5\u062a" },
            { "islamic.MonthAbbreviations", metaValue_islamic_MonthNames },
            { "islamic.narrow.Eras", metaValue_islamic_long_Eras },
            { "calendarname.buddhist", "\u0628\u06c7\u062f\u062f\u0627 \u064a\u0649\u0644\u0646\u0627\u0645\u06d5\u0633\u0649" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.abbreviated.AmPmMarkers", metaValue_abbreviated_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
