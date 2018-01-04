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

public class FormatData_hi extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u091c\u0928\u0935\u0930\u0940",
               "\u092b\u093c\u0930\u0935\u0930\u0940",
               "\u092e\u093e\u0930\u094d\u091a",
               "\u0905\u092a\u094d\u0930\u0948\u0932",
               "\u092e\u0908",
               "\u091c\u0942\u0928",
               "\u091c\u0941\u0932\u093e\u0908",
               "\u0905\u0917\u0938\u094d\u0924",
               "\u0938\u093f\u0924\u0902\u092c\u0930",
               "\u0905\u0915\u094d\u0924\u0942\u092c\u0930",
               "\u0928\u0935\u0902\u092c\u0930",
               "\u0926\u093f\u0938\u0902\u092c\u0930",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u091c\u0928\u0970",
               "\u092b\u093c\u0930\u0970",
               "\u092e\u093e\u0930\u094d\u091a",
               "\u0905\u092a\u094d\u0930\u0948\u0932",
               "\u092e\u0908",
               "\u091c\u0942\u0928",
               "\u091c\u0941\u0932\u0970",
               "\u0905\u0917\u0970",
               "\u0938\u093f\u0924\u0970",
               "\u0905\u0915\u094d\u0924\u0942\u0970",
               "\u0928\u0935\u0970",
               "\u0926\u093f\u0938\u0970",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u091c",
               "\u092b\u093c",
               "\u092e\u093e",
               "\u0905",
               "\u092e",
               "\u091c\u0942",
               "\u091c\u0941",
               "\u0905",
               "\u0938\u093f",
               "\u0905",
               "\u0928",
               "\u0926\u093f",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u0930\u0935\u093f\u0935\u093e\u0930",
               "\u0938\u094b\u092e\u0935\u093e\u0930",
               "\u092e\u0902\u0917\u0932\u0935\u093e\u0930",
               "\u092c\u0941\u0927\u0935\u093e\u0930",
               "\u0917\u0941\u0930\u0941\u0935\u093e\u0930",
               "\u0936\u0941\u0915\u094d\u0930\u0935\u093e\u0930",
               "\u0936\u0928\u093f\u0935\u093e\u0930",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u0930\u0935\u093f",
               "\u0938\u094b\u092e",
               "\u092e\u0902\u0917\u0932",
               "\u092c\u0941\u0927",
               "\u0917\u0941\u0930\u0941",
               "\u0936\u0941\u0915\u094d\u0930",
               "\u0936\u0928\u093f",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u0930",
               "\u0938\u094b",
               "\u092e\u0902",
               "\u092c\u0941",
               "\u0917\u0941",
               "\u0936\u0941",
               "\u0936",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u092a\u0939\u0932\u0940 \u0924\u093f\u092e\u093e\u0939\u0940",
               "\u0926\u0942\u0938\u0930\u0940 \u0924\u093f\u092e\u093e\u0939\u0940",
               "\u0924\u0940\u0938\u0930\u0940 \u0924\u093f\u092e\u093e\u0939\u0940",
               "\u091a\u094c\u0925\u0940 \u0924\u093f\u092e\u093e\u0939\u0940",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "\u0924\u093f1",
               "\u0924\u093f2",
               "\u0924\u093f3",
               "\u0924\u093f4",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u092a\u0942\u0930\u094d\u0935\u093e\u0939\u094d\u0928",
               "\u0905\u092a\u0930\u093e\u0939\u094d\u0928",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "\u092a\u0942",
               "\u0905",
            };
        final String[] metaValue_Eras = new String[] {
               "\u0908\u0938\u093e-\u092a\u0942\u0930\u094d\u0935",
               "\u0908\u0938\u094d\u0935\u0940",
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
               "G EEEE, d MMMM y",
               "G d MMMM y",
               "G d MMM y",
               "G d/M/y",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "GGGG EEEE, d MMMM y",
               "GGGG d MMMM y",
               "GGGG d MMM y",
               "GGGG d/M/y",
            };
        final String metaValue_calendarname_gregorian = "\u0917\u094d\u0930\u0947\u0917\u094b\u0930\u093f\u092f\u0928 \u0915\u0948\u0932\u0947\u0902\u0921\u0930";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u0935\u0930\u094d\u0937" },
            { "calendarname.islamic-umalqura", "\u0907\u0938\u094d\u0932\u093e\u092e\u0940 \u0915\u0948\u0932\u0947\u0902\u0921\u0930 (\u0909\u092e\u094d\u092e \u0905\u0932-\u0915\u093c\u0941\u0930\u093e)" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.islamic-civil", "\u0907\u0938\u094d\u0932\u093e\u092e\u0940 \u0928\u093e\u0917\u0930\u093f\u0915 \u092a\u0902\u091a\u093e\u0902\u0917" },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "{0} \u092e\u093e\u0928\u0915 \u0938\u092e\u092f" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u091c\u093e\u092a\u093e\u0928\u0940 \u092a\u0902\u091a\u093e\u0902\u0917" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "long.Eras",
                new String[] {
                    "\u0908\u0938\u093e-\u092a\u0942\u0930\u094d\u0935",
                    "\u0908\u0938\u0935\u0940 \u0938\u0928",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns",
                new String[] {
                    "{1} \u0915\u094b {0}",
                    "{1} \u0915\u094b {0}",
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
            { "field.minute", "\u092e\u093f\u0928\u091f" },
            { "field.era", "\u092f\u0941\u0917" },
            { "japanese.long.Eras",
                new String[] {
                    "\u0908\u0938\u0935\u0940 \u0938\u0928",
                    "\u092e\u0947\u091c\u0940",
                    "\u0924\u093e\u0908\u0936\u094b",
                    "\u0936\u094b\u0935\u093e",
                    "\u0939\u0947\u0908\u0938\u0947\u0908",
                }
            },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u092a\u0942\u0930\u094d\u0935\u093e\u0939\u094d\u0928/\u0905\u092a\u0930\u093e\u0939\u094d\u0928" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.MonthNames",
                new String[] {
                    "\u092e\u0941\u0939\u0930\u094d\u0930\u092e",
                    "\u0938\u092b\u0930",
                    "\u0930\u093e\u092c\u0940 \u092a\u094d\u0930\u0925\u092e",
                    "\u0930\u093e\u092c\u0940 \u0926\u094d\u0935\u093f\u0924\u0940\u092f",
                    "\u091c\u0941\u092e\u094d\u0921\u093e \u092a\u094d\u0930\u0925\u092e",
                    "\u091c\u0941\u092e\u094d\u0921\u093e \u0926\u094d\u0935\u093f\u0924\u0940\u092f",
                    "\u0930\u091c\u092c",
                    "\u0936\u093e\u0935\u0928",
                    "\u0930\u092e\u091c\u093e\u0928",
                    "\u0936\u0935\u094d\u0935\u094d\u0932",
                    "\u091c\u093f\u0932-\u0915\u094d\u0926\u093e\u0939",
                    "\u091c\u093f\u0932\u094d-\u0939\u093f\u091c\u094d\u091c\u093e\u0939",
                    "",
                }
            },
            { "calendarname.roc", "\u091a\u0940\u0928\u0940 \u0917\u0923\u0924\u0902\u0924\u094d\u0930 \u092a\u0902\u091a\u093e\u0902\u0917" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "deva.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0966",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "field.month", "\u092e\u093e\u0939" },
            { "field.second", "\u0938\u0947\u0915\u0902\u0921" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##,##0.###",
                    "\u00a4#,##,##0.00",
                    "#,##,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "calendarname.islamic", "\u0907\u0938\u094d\u0932\u093e\u092e\u0940 \u092a\u0902\u091a\u093e\u0902\u0917" },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "timezone.regionFormat", "{0} \u0938\u092e\u092f" },
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
            { "field.zone", "\u0938\u092e\u092f \u0915\u094d\u0937\u0947\u0924\u094d\u0930" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "\u0938\u092a\u094d\u0924\u093e\u0939 \u0915\u093e \u0926\u093f\u0928" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u0938\u092a\u094d\u0924\u093e\u0939" },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0} \u0921\u0947\u0932\u093e\u0907\u091f \u0938\u092e\u092f" },
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
            { "field.hour", "\u0918\u0902\u091f\u093e" },
            { "calendarname.buddhist", "\u092c\u094c\u0926\u094d\u0927 \u092a\u0902\u091a\u093e\u0902\u0917" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "japanese.Eras",
                new String[] {
                    "\u0908\u0938\u094d\u0935\u0940",
                    "\u092e\u0947\u091c\u0940",
                    "\u0924\u093e\u0908\u0936\u094b",
                    "\u0936\u094b\u0935\u093e",
                    "\u0939\u0947\u0908\u0938\u0947\u0908",
                }
            },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
