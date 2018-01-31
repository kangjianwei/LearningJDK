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

public class FormatData_ne extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u091c\u0928\u0935\u0930\u0940",
               "\u092b\u0947\u092c\u094d\u0930\u0941\u0905\u0930\u0940",
               "\u092e\u093e\u0930\u094d\u091a",
               "\u0905\u092a\u094d\u0930\u093f\u0932",
               "\u092e\u0947",
               "\u091c\u0941\u0928",
               "\u091c\u0941\u0932\u093e\u0908",
               "\u0905\u0917\u0938\u094d\u091f",
               "\u0938\u0947\u092a\u094d\u091f\u0947\u092e\u094d\u092c\u0930",
               "\u0905\u0915\u094d\u091f\u094b\u092c\u0930",
               "\u0928\u094b\u092d\u0947\u092e\u094d\u092c\u0930",
               "\u0921\u093f\u0938\u0947\u092e\u094d\u092c\u0930",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u091c\u0928",
               "\u092b\u0947\u092c",
               "\u092e\u093e\u0930\u094d\u091a",
               "\u0905\u092a\u094d\u0930",
               "\u092e\u0947",
               "\u091c\u0941\u0928",
               "\u091c\u0941\u0932",
               "\u0905\u0917",
               "\u0938\u0947\u092a",
               "\u0905\u0915\u094d\u091f\u094b",
               "\u0928\u094b\u092d\u0947",
               "\u0921\u093f\u0938\u0947",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u0906\u0907\u0924\u092c\u093e\u0930",
               "\u0938\u094b\u092e\u092c\u093e\u0930",
               "\u092e\u0919\u094d\u0917\u0932\u092c\u093e\u0930",
               "\u092c\u0941\u0927\u092c\u093e\u0930",
               "\u092c\u093f\u0939\u093f\u092c\u093e\u0930",
               "\u0936\u0941\u0915\u094d\u0930\u092c\u093e\u0930",
               "\u0936\u0928\u093f\u092c\u093e\u0930",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u0906\u0907\u0924",
               "\u0938\u094b\u092e",
               "\u092e\u0919\u094d\u0917\u0932",
               "\u092c\u0941\u0927",
               "\u092c\u093f\u0939\u093f",
               "\u0936\u0941\u0915\u094d\u0930",
               "\u0936\u0928\u093f",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u0906",
               "\u0938\u094b",
               "\u092e",
               "\u092c\u0941",
               "\u092c\u093f",
               "\u0936\u0941",
               "\u0936",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u092a\u0939\u093f\u0932\u094b \u0938\u0924\u094d\u0930",
               "\u0926\u094b\u0938\u094d\u0930\u094b \u0938\u0924\u094d\u0930",
               "\u0924\u0947\u0938\u094d\u0930\u094b \u0938\u0924\u094d\u0930",
               "\u091a\u094c\u0925\u094b \u0938\u0924\u094d\u0930",
            };
        final String[] metaValue_QuarterNarrows = new String[] {
               "\u0967",
               "\u0968",
               "\u0969",
               "\u096a",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u092a\u0942\u0930\u094d\u0935\u093e\u0939\u094d\u0928",
               "\u0905\u092a\u0930\u093e\u0939\u094d\u0928",
            };
        final String[] metaValue_long_Eras = new String[] {
               "\u0908\u0938\u093e \u092a\u0942\u0930\u094d\u0935",
               "\u0938\u0928\u094d",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "HH:mm:ss zzzz",
               "HH:mm:ss z",
               "HH:mm:ss",
               "HH:mm",
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
        final String metaValue_calendarname_gregorian = "\u0917\u094d\u0930\u0947\u0917\u094b\u0930\u093f\u092f\u0928 \u092a\u093e\u0924\u094d\u0930\u094b";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u0935\u0930\u094d\u0937" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.islamic-civil", "\u0907\u0938\u094d\u0932\u093e\u092e\u0940 \u0928\u093e\u0917\u0930\u093f\u0915 \u092a\u093e\u0924\u094d\u0930\u094b" },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_long_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "{0} (+\u0966)" },
            { "DefaultNumberingSystem", "deva" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u091c\u093e\u092a\u093e\u0928\u0940 \u092a\u093e\u0924\u094d\u0930\u094b" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthNames },
            { "long.Eras", metaValue_long_Eras },
            { "roc.QuarterNarrows", metaValue_QuarterNarrows },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthNames },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}",
                    "{1} {0}",
                    "{1}, {0}",
                    "{1}, {0}",
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
            { "field.minute", "\u092e\u093f\u0928\u0947\u091f" },
            { "field.era", "\u0915\u093e\u0932" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u092a\u0942\u0930\u094d\u0935\u093e\u0939\u094d\u0928 / \u0905\u092a\u0930\u093e\u0939\u094d\u0928" },
            { "standalone.MonthNarrows",
                new String[] {
                    "\u091c\u0928",
                    "\u092b\u0947\u0947\u092c",
                    "\u092e\u093e\u0930\u094d\u091a",
                    "\u0905\u092a\u094d\u0930",
                    "\u092e\u0947",
                    "\u091c\u0941\u0928",
                    "\u091c\u0941\u0932",
                    "\u0905\u0917",
                    "\u0938\u0947\u092a",
                    "\u0905\u0915\u094d\u091f\u094b",
                    "\u0928\u094b\u092d\u0947",
                    "\u0921\u093f\u0938\u0947",
                    "",
                }
            },
            { "japanese.QuarterNarrows", metaValue_QuarterNarrows },
            { "calendarname.roc", "\u091a\u093f\u0928\u093f\u092f\u093e\u0901 \u0917\u0923\u0924\u0928\u094d\u0924\u094d\u0930\u0915\u094b \u092a\u093e\u0924\u094d\u0930\u094b" },
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
            { "roc.QuarterAbbreviations", metaValue_QuarterNames },
            { "field.month", "\u092e\u0939\u093f\u0928\u093e" },
            { "field.second", "\u0938\u0947\u0915\u0947\u0928\u094d\u0921" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "\u00a4\u00a0#,##0.00",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "calendarname.islamic", "\u0907\u0938\u094d\u0932\u093e\u092e\u0940 \u092a\u093e\u0924\u094d\u0930\u094b" },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthNames },
            { "timezone.regionFormat", "{0} \u0938\u092e\u092f" },
            { "buddhist.QuarterNarrows", metaValue_QuarterNarrows },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u0915\u094d\u0937\u0947\u0924\u094d\u0930" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterNames },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_long_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "QuarterNarrows", metaValue_QuarterNarrows },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "standalone.QuarterNarrows", metaValue_QuarterNarrows },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "\u0939\u092a\u094d\u0924\u093e\u0915\u094b \u092c\u093e\u0930" },
            { "japanese.MonthAbbreviations", metaValue_MonthNames },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterNames },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u0939\u092a\u094d\u0924\u093e" },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0} (+\u0967)" },
            { "DatePatterns",
                new String[] {
                    "y MMMM d, EEEE",
                    "y MMMM d",
                    "y MMM d",
                    "yy/M/d",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthNames },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u0918\u0923\u094d\u091f\u093e" },
            { "calendarname.buddhist", "\u092c\u0941\u0926\u094d\u0927\u093f\u0937\u094d\u091f \u092a\u093e\u0924\u094d\u0930\u094b" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
