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

public class FormatData_ps extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u062c\u0646\u0648\u0631\u064a",
               "\u0641\u0628\u0631\u0648\u0631\u064a",
               "\u0645\u0627\u0631\u0686",
               "\u0627\u067e\u0631\u06cc\u0644",
               "\u0645\u06cd",
               "\u062c\u0648\u0646",
               "\u062c\u0648\u0644\u0627\u06cc",
               "\u0627\u06af\u0633\u062a",
               "\u0633\u06d0\u067e\u062a\u0645\u0628\u0631",
               "\u0627\u06a9\u062a\u0648\u0628\u0631",
               "\u0646\u0648\u0645\u0628\u0631",
               "\u062f\u0633\u0645\u0628\u0631",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u062c",
               "\u0641",
               "\u0645",
               "\u0627",
               "\u0645",
               "\u062c",
               "\u062c",
               "\u0627",
               "\u0633",
               "\u0627",
               "\u0646",
               "\u062f",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u064a\u0648\u0646\u06cd",
               "\u062f\u0648\u0646\u06cd",
               "\u062f\u0631\u06d0\u0646\u06cd",
               "\u0685\u0644\u0631\u0646\u06cd",
               "\u067e\u064a\u0646\u0681\u0646\u06cd",
               "\u062c\u0645\u0639\u0647",
               "\u0627\u0648\u0646\u06cd",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u0644\u0648\u0645\u0693\u06cd \u0631\u0628\u0639\u0647",
               "\u06f2\u0645\u0647 \u0631\u0628\u0639\u0647",
               "\u06f3\u0645\u0647 \u0631\u0628\u0639\u0647",
               "\u06f4\u0645\u0647 \u0631\u0628\u0639\u0647",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u063a.\u0645.",
               "\u063a.\u0648.",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "H:mm:ss (zzzz)",
               "H:mm:ss (z)",
               "H:mm:ss",
               "H:mm",
            };
        final String[] metaValue_buddhist_DayNarrows = new String[] {
               "S",
               "M",
               "T",
               "W",
               "T",
               "F",
               "S",
            };
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE \u062f G y \u062f MMMM d",
               "\u062f G y \u062f MMMM d",
               "G y MMM d",
               "GGGGG y/M/d",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE \u062f GGGG y \u062f MMMM d",
               "\u062f GGGG y \u062f MMMM d",
               "GGGG y MMM d",
               "G y/M/d",
            };
        final String[] metaValue_islamic_MonthNames = new String[] {
               "\u0645\u062d\u0631\u0645",
               "\u062f \u0635\u0641\u0631\u06d0 \u062f",
               "\u0631\u0628\u064a\u0639",
               "\u0631\u0628\u064a\u0639 II",
               "\u062c\u0645\u0627\u0639\u0647",
               "\u062c\u0645\u0648\u0645\u0627 II",
               "\u0631\u0627\u062c\u0627\u0628",
               "\u0634\u0639\u0628\u0627\u0646",
               "\u0631\u0645\u0636\u0627\u0646",
               "\u0634\u0648\u0627\u0644",
               "\u062f\u0627\u0644\u0642\u0627\u0639\u062f\u0647",
               "\u062d\u0644\u0627\u0644 \u062d\u062c",
               "",
            };
        final String[] metaValue_islamic_long_Eras = new String[] {
               "",
               "AH",
            };
        final String metaValue_calendarname_gregorian = "\u062f \u06ab\u0631\u06cc\u06ab\u0648\u0631\u06cc\u0627 \u06a9\u069a\u062a\u0647";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u06a9\u0627\u0644" },
            { "arabext.NumberElements",
                new String[] {
                    "\u066b",
                    "\u066c",
                    "\u061b",
                    "\u066a",
                    "\u06f0",
                    "#",
                    "\u200e-\u200e",
                    "\u00d7\u06f1\u06f0^",
                    "\u0609",
                    "\u221e",
                    "NaN",
                }
            },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.islamic-civil", "\u062f \u0627\u0633\u0644\u0627\u0645\u064a \u062c\u0646\u062a\u0631\u064a (\u062c\u062f\u0648\u0644\u064a\u060c \u062f \u0645\u062f\u0646\u064a \u0639\u0635\u0631)" },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras",
                new String[] {
                    "BCE",
                    "\u0645.",
                }
            },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "{0} \u0645\u0639\u06cc\u0627\u0631\u06cc \u0648\u062e\u062a" },
            { "DefaultNumberingSystem", "arabext" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u062f \u062c\u0627\u067e\u0627\u0646\u064a \u062c\u0646\u062a\u0631\u064a" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayNames },
            { "roc.MonthAbbreviations", metaValue_MonthNames },
            { "long.Eras",
                new String[] {
                    "\u0644\u0647 \u0645\u06cc\u0644\u0627\u062f \u0685\u062e\u0647 \u0648\u0693\u0627\u0646\u062f\u06d0",
                    "\u0644\u0647 \u0645\u06cc\u0644\u0627\u062f \u0685\u062e\u0647 \u0648\u0631\u0648\u0633\u062a\u0647",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthNames },
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
                    ".",
                    ";",
                    "%",
                    "0",
                    "#",
                    "\u200e\u2212",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "MonthNarrows", metaValue_MonthNarrows },
            { "japanese.DatePatterns", metaValue_buddhist_DatePatterns },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u062f\u0642\u064a\u0642\u0647" },
            { "field.era", "\u067e\u06d0\u0631" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u0648\u0631\u0681 \u0634\u06d0\u0628\u0647" },
            { "islamic.MonthNames", metaValue_islamic_MonthNames },
            { "calendarname.roc", "\u0645\u0646\u06af\u0648\u0648 \u062c\u0646\u062a\u0631\u064a" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterNames },
            { "islamic.Eras", metaValue_islamic_long_Eras },
            { "field.month", "\u0645\u064a\u0627\u0634\u062a" },
            { "field.second", "\u062b\u0627\u0646\u064a\u0647" },
            { "DayAbbreviations", metaValue_DayNames },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "#,##0.00\u00a0\u00a4",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "calendarname.islamic", "\u062f \u0627\u0633\u0644\u0627\u0645\u064a \u062c\u0646\u062a\u0631\u064a" },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations",
                new String[] {
                    "\u062c\u0646\u0648\u0631\u064a",
                    "\u0641\u0628\u0631\u0648\u0631\u064a",
                    "\u0645\u0627\u0631\u0686",
                    "\u0627\u067e\u0631\u06cc\u0644",
                    "\u0645\u06cd",
                    "\u062c\u0648\u0646",
                    "\u062c\u0648\u0644\u0627\u06cc",
                    "\u0627\u06af\u0633\u062a",
                    "\u0633\u067e\u062a\u0645\u0628\u0631",
                    "\u0627\u06a9\u062a\u0648\u0628\u0631",
                    "\u0646\u0648\u0645\u0628\u0631",
                    "\u062f\u0633\u0645\u0628\u0631",
                    "",
                }
            },
            { "timezone.regionFormat", "\u062f {0} \u067e\u0647 \u0648\u062e\u062a" },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.DayAbbreviations", metaValue_DayNames },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_buddhist_DayNarrows },
            { "field.zone", "\u0648\u062e\u062a \u0633\u064a\u0645\u0647" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterNames },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras",
                new String[] {
                    "\u0644\u0647 \u0645\u06cc\u0644\u0627\u062f \u0648\u0693\u0627\u0646\u062f\u06d0",
                    "\u0645.",
                }
            },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "\u062f \u0627\u0648\u0646\u06cd \u0648\u0631\u0681" },
            { "japanese.MonthAbbreviations", metaValue_MonthNames },
            { "islamic.DayAbbreviations", metaValue_DayNames },
            { "islamic.long.Eras", metaValue_islamic_long_Eras },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterNames },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayNames },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u0627\u0648\u0646\u06cd" },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0} \u0631\u06bc\u0627 \u0648\u0631\u0681 \u0648\u062e\u062a" },
            { "DatePatterns",
                new String[] {
                    "EEEE \u062f y \u062f MMMM d",
                    "\u062f y \u062f MMMM d",
                    "y MMM d",
                    "y/M/d",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayNames },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthNames },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u0633\u0627\u0639\u062a" },
            { "islamic.MonthAbbreviations", metaValue_islamic_MonthNames },
            { "islamic.narrow.Eras", metaValue_islamic_long_Eras },
            { "calendarname.buddhist", "\u0628\u0648\u062f\u0627\u064a\u064a \u062c\u0646\u062a\u0631\u064a" },
            { "standalone.MonthNames",
                new String[] {
                    "\u062c\u0646\u0648\u0631\u064a",
                    "\u0641\u06d0\u0628\u0631\u0648\u0631\u064a",
                    "\u0645\u0627\u0631\u0686",
                    "\u0627\u067e\u0631\u06cc\u0644",
                    "\u0645\u06cd",
                    "\u062c\u0648\u0646",
                    "\u062c\u0648\u0644\u0627\u06cc",
                    "\u0627\u06af\u0633\u062a",
                    "\u0633\u067e\u062a\u0645\u0628\u0631",
                    "\u0627\u06a9\u062a\u0648\u0628\u0631",
                    "\u0646\u0648\u0645\u0628\u0631",
                    "\u062f\u0633\u0645\u0628\u0631",
                    "",
                }
            },
            { "buddhist.DayNarrows", metaValue_buddhist_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "japanese.DayNarrows", metaValue_buddhist_DayNarrows },
            { "QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
