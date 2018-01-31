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

public class FormatData_jgo extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "Ndu\u014bmbi Sa\u014b",
               "P\u025bsa\u014b P\u025b\u0301p\u00e1",
               "P\u025bsa\u014b P\u025b\u0301t\u00e1t",
               "P\u025bsa\u014b P\u025b\u0301n\u025b\u0301kwa",
               "P\u025bsa\u014b Pataa",
               "P\u025bsa\u014b P\u025b\u0301n\u025b\u0301nt\u00fak\u00fa",
               "P\u025bsa\u014b Saamb\u00e1",
               "P\u025bsa\u014b P\u025b\u0301n\u025b\u0301f\u0254m",
               "P\u025bsa\u014b P\u025b\u0301n\u025b\u0301pf\u00fa\ua78b\u00fa",
               "P\u025bsa\u014b N\u025bg\u025b\u0301m",
               "P\u025bsa\u014b Nts\u0254\u030cpm\u0254\u0301",
               "P\u025bsa\u014b Nts\u0254\u030cpp\u00e1",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "S\u0254\u0301ndi",
               "M\u0254\u0301ndi",
               "\u00c1pta M\u0254\u0301ndi",
               "W\u025b\u0301n\u025bs\u025bd\u025b",
               "T\u0254\u0301s\u025bd\u025b",
               "F\u025bl\u00e2y\u025bd\u025b",
               "S\u00e1sid\u025b",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "S\u0254\u0301",
               "M\u0254\u0301",
               "\u00c1M",
               "W\u025b\u0301",
               "T\u0254\u0301",
               "F\u025b",
               "S\u00e1",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "mba\ua78cmba\ua78c",
               "\u014bka mb\u0254\u0301t nji",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "HH:mm:ss zzzz",
               "HH:mm:ss z",
               "HH:mm:ss",
               "HH:mm",
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
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE, G y MMMM dd",
               "G y MMMM d",
               "G y MMM d",
               "GGGGG y-MM-dd",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE, GGGG y MMMM dd",
               "GGGG y MMMM d",
               "GGGG y MMM d",
               "G y-MM-dd",
            };
        final String metaValue_calendarname_gregorian = "m\u025bl\u025b\u0301\ua78c-m\u025bk\u00e1t";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "roc.DayAbbreviations", metaValue_DayNames },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "roc.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "roc.DayNames", metaValue_DayNames },
            { "standalone.DayAbbreviations", metaValue_DayNames },
            { "roc.MonthAbbreviations", metaValue_MonthNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "long.Eras",
                new String[] {
                    "ts\u025btts\u025bt m\u025b\u014bgu\ua78c mi \u025b\u0301 l\u025b\u025bn\u025b K\u025bl\u00eds\u025bt\u0254 g\u0254 \u0144\u0254\u0301",
                    "ts\u025btts\u025bt m\u025b\u014bgu\ua78c mi \u025b\u0301 f\u00fan\u025b K\u025bl\u00eds\u025bt\u0254 t\u0254\u0301 m\u0254\u0301",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
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
            { "islamic.DayAbbreviations", metaValue_DayNames },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayNames },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
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
            { "buddhist.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "DatePatterns",
                new String[] {
                    "EEEE, y MMMM dd",
                    "y MMMM d",
                    "y MMM d",
                    "y-MM-dd",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayNames },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthNames },
            { "standalone.DayNames", metaValue_DayNames },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "standalone.MonthAbbreviations", metaValue_MonthNames },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "japanese.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
