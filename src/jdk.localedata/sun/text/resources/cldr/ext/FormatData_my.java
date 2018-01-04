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

public class FormatData_my extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u1007\u1014\u103a\u1014\u101d\u102b\u101b\u102e",
               "\u1016\u1031\u1016\u1031\u102c\u103a\u101d\u102b\u101b\u102e",
               "\u1019\u1010\u103a",
               "\u1027\u1015\u103c\u102e",
               "\u1019\u1031",
               "\u1007\u103d\u1014\u103a",
               "\u1007\u1030\u101c\u102d\u102f\u1004\u103a",
               "\u1029\u1002\u102f\u1010\u103a",
               "\u1005\u1000\u103a\u1010\u1004\u103a\u1018\u102c",
               "\u1021\u1031\u102c\u1000\u103a\u1010\u102d\u102f\u1018\u102c",
               "\u1014\u102d\u102f\u101d\u1004\u103a\u1018\u102c",
               "\u1012\u102e\u1007\u1004\u103a\u1018\u102c",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u1007\u1014\u103a",
               "\u1016\u1031",
               "\u1019\u1010\u103a",
               "\u1027",
               "\u1019\u1031",
               "\u1007\u103d\u1014\u103a",
               "\u1007\u1030",
               "\u1029",
               "\u1005\u1000\u103a",
               "\u1021\u1031\u102c\u1000\u103a",
               "\u1014\u102d\u102f",
               "\u1012\u102e",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u1007",
               "\u1016",
               "\u1019",
               "\u1027",
               "\u1019",
               "\u1007",
               "\u1007",
               "\u1029",
               "\u1005",
               "\u1021",
               "\u1014",
               "\u1012",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u1010\u1014\u1004\u103a\u1039\u1002\u1014\u103d\u1031",
               "\u1010\u1014\u1004\u103a\u1039\u101c\u102c",
               "\u1021\u1004\u103a\u1039\u1002\u102b",
               "\u1017\u102f\u1012\u1039\u1013\u101f\u1030\u1038",
               "\u1000\u103c\u102c\u101e\u1015\u1010\u1031\u1038",
               "\u101e\u1031\u102c\u1000\u103c\u102c",
               "\u1005\u1014\u1031",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u1010",
               "\u1010",
               "\u1021",
               "\u1017",
               "\u1000",
               "\u101e",
               "\u1005",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u1015\u1011\u1019 \u101e\u102f\u1036\u1038\u101c\u1015\u1010\u103a",
               "\u1012\u102f\u1010\u102d\u101a \u101e\u102f\u1036\u1038\u101c\u1015\u1010\u103a",
               "\u1010\u1010\u102d\u101a \u101e\u102f\u1036\u1038\u101c\u1015\u1010\u103a",
               "\u1005\u1010\u102f\u1010\u1039\u1011 \u101e\u102f\u1036\u1038\u101c\u1015\u1010\u103a",
            };
        final String[] metaValue_QuarterNarrows = new String[] {
               "\u1015",
               "\u1012\u102f",
               "\u1010",
               "\u1005",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u1014\u1036\u1014\u1000\u103a",
               "\u100a\u1014\u1031",
            };
        final String[] metaValue_Eras = new String[] {
               "\u1018\u102e\u1005\u102e",
               "\u1021\u1031\u1012\u102e",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "zzzz HH:mm:ss",
               "z HH:mm:ss",
               "HH:mm:ss",
               "H:mm",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE G dd MMMM y",
               "G dd MMMM y",
               "G d MMM y",
               "GGGGG dd-MM-yy",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE GGGG dd MMMM y",
               "GGGG dd MMMM y",
               "GGGG d MMM y",
               "G dd-MM-yy",
            };
        final String metaValue_calendarname_gregorian = "\u1014\u102d\u102f\u1004\u103a\u1004\u1036\u1010\u1000\u102c\u101e\u102f\u1036\u1038 \u1015\u103c\u1000\u1039\u1001\u1012\u102d\u1014\u103a";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u1014\u103e\u1005\u103a" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "mymr.NumberElements",
                new String[] {
                    ".",
                    ",",
                    "\u104a",
                    "%",
                    "\u1040",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "\u1002\u100f\u1014\u103a\u1038\u1019\u101f\u102f\u1010\u103a\u101e\u1031\u102c",
                }
            },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "{0} \u1005\u1036\u1010\u1031\u102c\u103a\u1001\u103b\u102d\u1014\u103a" },
            { "DefaultNumberingSystem", "mymr" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u1002\u103b\u1015\u1014\u103a \u1015\u103c\u1000\u1039\u1001\u1012\u102d\u1014\u103a" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayNames },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "long.Eras",
                new String[] {
                    "\u1001\u101b\u1005\u103a\u1010\u1031\u102c\u103a \u1019\u1015\u1031\u102b\u103a\u1019\u102e\u1014\u103e\u1005\u103a",
                    "\u1001\u101b\u1005\u103a\u1014\u103e\u1005\u103a",
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
                    "\u1002\u100f\u1014\u103a\u1038\u1019\u101f\u102f\u1010\u103a\u101e\u1031\u102c",
                }
            },
            { "MonthNarrows", metaValue_MonthNarrows },
            { "japanese.DatePatterns", metaValue_buddhist_DatePatterns },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u1019\u102d\u1014\u1005\u103a" },
            { "field.era", "\u1001\u1031\u1010\u103a" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u1014\u1036\u1014\u1000\u103a/\u100a\u1014\u1031" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "japanese.QuarterNarrows", metaValue_QuarterNarrows },
            { "calendarname.roc", "\u1019\u1004\u103a\u1002\u102f\u1021\u102d\u102f \u1015\u103c\u1000\u1039\u1001\u1012\u102d\u1014\u103a" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterNames },
            { "field.month", "\u101c" },
            { "field.second", "\u1005\u1000\u1039\u1000\u1014\u1037\u103a" },
            { "DayAbbreviations", metaValue_DayNames },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "#,##0.00\u00a0\u00a4",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "calendarname.islamic", "\u1021\u1005\u1039\u1005\u101c\u102c\u1019\u103a \u1015\u103c\u1000\u1039\u1001\u1012\u102d\u1014\u103a" },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "timezone.regionFormat", "{0} \u1021\u1001\u103b\u102d\u1014\u103a" },
            { "buddhist.QuarterNarrows", metaValue_QuarterNarrows },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.DayAbbreviations", metaValue_DayNames },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u1007\u102f\u1014\u103a" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterNames },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "QuarterNarrows", metaValue_QuarterNarrows },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "standalone.QuarterNarrows", metaValue_QuarterNarrows },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "\u1014\u1031\u1037" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayNames },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterNames },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayNames },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u1015\u1010\u103a" },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0} \u1014\u103d\u1031\u101b\u102c\u101e\u102e \u1005\u1036\u1010\u1031\u102c\u103a\u1001\u103b\u102d\u1014\u103a" },
            { "DatePatterns",
                new String[] {
                    "y\u104a MMMM d\u104a EEEE",
                    "y\u104a d MMMM",
                    "y\u104a MMM d",
                    "dd-MM-yy",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayNames },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u1014\u102c\u101b\u102e" },
            { "calendarname.buddhist", "\u1017\u102f\u1012\u1039\u1013 \u1015\u103c\u1000\u1039\u1001\u1012\u102d\u1014\u103a" },
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
