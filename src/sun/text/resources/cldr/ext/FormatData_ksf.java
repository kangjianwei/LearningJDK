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

public class FormatData_ksf extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u014bw\u00ed\u00ed a nt\u0254\u0301nt\u0254",
               "\u014bw\u00ed\u00ed ak\u01dd b\u025b\u0301\u025b",
               "\u014bw\u00ed\u00ed ak\u01dd r\u00e1\u00e1",
               "\u014bw\u00ed\u00ed ak\u01dd nin",
               "\u014bw\u00ed\u00ed ak\u01dd t\u00e1an",
               "\u014bw\u00ed\u00ed ak\u01dd t\u00e1af\u0254k",
               "\u014bw\u00ed\u00ed ak\u01dd t\u00e1ab\u025b\u025b",
               "\u014bw\u00ed\u00ed ak\u01dd t\u00e1araa",
               "\u014bw\u00ed\u00ed ak\u01dd t\u00e1anin",
               "\u014bw\u00ed\u00ed ak\u01dd nt\u025bk",
               "\u014bw\u00ed\u00ed ak\u01dd nt\u025bk di b\u0254\u0301k",
               "\u014bw\u00ed\u00ed ak\u01dd nt\u025bk di b\u025b\u0301\u025b",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u014b1",
               "\u014b2",
               "\u014b3",
               "\u014b4",
               "\u014b5",
               "\u014b6",
               "\u014b7",
               "\u014b8",
               "\u014b9",
               "\u014b10",
               "\u014b11",
               "\u014b12",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "s\u0254\u0301nd\u01dd",
               "l\u01ddnd\u00ed",
               "maad\u00ed",
               "m\u025bkr\u025bd\u00ed",
               "j\u01dd\u01ddd\u00ed",
               "j\u00famb\u00e1",
               "samd\u00ed",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "s\u0254\u0301n",
               "l\u01ddn",
               "maa",
               "m\u025bk",
               "j\u01dd\u01dd",
               "j\u00fam",
               "sam",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "s",
               "l",
               "m",
               "m",
               "j",
               "j",
               "s",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "id\u0301\u025b\u0301n k\u01ddb\u01ddk k\u01dd nt\u0254\u0301nt\u0254\u0301",
               "id\u025b\u0301n k\u01ddb\u01ddk k\u01dd k\u01ddb\u025b\u0301\u025b",
               "id\u025b\u0301n k\u01ddb\u01ddk k\u01dd k\u01ddr\u00e1\u00e1",
               "id\u025b\u0301n k\u01ddb\u01ddk k\u01dd k\u01ddnin",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "i1",
               "i2",
               "i3",
               "i4",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "s\u00e1r\u00faw\u00e1",
               "c\u025b\u025b\u0301nko",
            };
        final String[] metaValue_Eras = new String[] {
               "d.Y.",
               "k.Y.",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "HH:mm:ss zzzz",
               "HH:mm:ss z",
               "HH:mm:ss",
               "HH:mm",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE d MMMM y G",
               "d MMMM y G",
               "d MMM y G",
               "d/M/y GGGGG",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE d MMMM y GGGG",
               "d MMMM y GGGG",
               "d MMM y GGGG",
               "d/M/y G",
            };
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "B\u01ddk" },
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
            { "field.zone", "W\u00e1as" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
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
                    "di Y\u025b\u0301sus ak\u00e1 y\u00e1l\u025b",
                    "c\u00e1m\u025b\u025bn k\u01dd k\u01ddb\u0254pka Y",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "field.weekday", "M\u01ddr\u00fa m\u01dd s\u0254\u0301nd\u01dd" },
            { "buddhist.MonthNames", metaValue_MonthNames },
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
                    "NaN",
                }
            },
            { "japanese.DatePatterns", metaValue_buddhist_DatePatterns },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "M\u01ddn\u00edt" },
            { "field.era", "By\u00e1m\u025b\u025bn" },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "S\u00e1r\u00faw\u00e1 / C\u025b\u025b\u0301nko" },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "field.month", "\u014aw\u00ed\u00ed" },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "field.second", "H\u00e1u" },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "S\u0254\u0301nd\u01dd" },
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
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "DatePatterns",
                new String[] {
                    "EEEE d MMMM y",
                    "d MMMM y",
                    "d MMM y",
                    "d/M/y",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "C\u00e1m\u025b\u025bn" },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
        };
        return data;
    }
}
