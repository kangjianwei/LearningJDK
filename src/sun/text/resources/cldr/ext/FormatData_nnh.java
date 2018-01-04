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

public class FormatData_nnh extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "sa\u014b tsets\u025b\u0300\u025b l\u00f9m",
               "sa\u014b k\u00e0g ngw\u00f3\u014b",
               "sa\u014b lepy\u00e8 sh\u00fam",
               "sa\u014b c\u00ff\u00f3",
               "sa\u014b ts\u025b\u0300\u025b c\u00ff\u00f3",
               "sa\u014b nj\u00ffol\u00e1\u02bc",
               "sa\u014b ty\u025b\u0300b ty\u025b\u0300b mb\u0289\u0300\u014b",
               "sa\u014b mb\u0289\u0300\u014b",
               "sa\u014b ngw\u0254\u0300\u02bc mb\u00ff\u025b",
               "sa\u014b t\u00e0\u014ba tsets\u00e1\u02bc",
               "sa\u014b mejwo\u014b\u00f3",
               "sa\u014b l\u00f9m",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "ly\u025b\u02bc\u025b\u0301 s\u1e85\u00ed\u014bt\u00e8",
               "mvf\u00f2 ly\u025b\u030c\u02bc",
               "mb\u0254\u0301\u0254nt\u00e8 mvf\u00f2 ly\u025b\u030c\u02bc",
               "ts\u00e8ts\u025b\u0300\u025b ly\u025b\u030c\u02bc",
               "mb\u0254\u0301\u0254nt\u00e8 tsets\u025b\u0300\u025b ly\u025b\u030c\u02bc",
               "mvf\u00f2 m\u00e0ga ly\u025b\u030c\u02bc",
               "m\u00e0ga ly\u025b\u030c\u02bc",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "mba\u02bc\u00e1mba\u02bc",
               "ncw\u00f2nz\u00e9m",
            };
        final String[] metaValue_Eras = new String[] {
               "m.z.Y.",
               "m.g.n.Y.",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE , 'ly\u025b'\u030c\u02bc d 'na' MMMM, y G",
               "'ly\u025b'\u030c\u02bc d 'na' MMMM, y G",
               "d MMM, y G",
               "dd/MM/yy GGGGG",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE , 'ly\u025b'\u030c\u02bc d 'na' MMMM, y GGGG",
               "'ly\u025b'\u030c\u02bc d 'na' MMMM, y GGGG",
               "d MMM, y GGGG",
               "dd/MM/yy G",
            };
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "ng\u00f9\u02bc" },
            { "roc.DayAbbreviations", metaValue_DayNames },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "roc.DayNames", metaValue_DayNames },
            { "standalone.DayAbbreviations", metaValue_DayNames },
            { "roc.MonthAbbreviations", metaValue_MonthNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "long.Eras",
                new String[] {
                    "m\u00e9 zy\u00e9 Y\u011bs\u00f4",
                    "m\u00e9 g\u00ffo \u0144zy\u00e9 Y\u011bs\u00f4",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "buddhist.MonthAbbreviations", metaValue_MonthNames },
            { "field.weekday", "ng\u00e0ba l\u00e1\u02bc" },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns",
                new String[] {
                    "{1},{0}",
                    "{1}, {0}",
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
            { "field.era", "ts\u0254\u0301 f\u0289\u0300\u02bc" },
            { "islamic.DayAbbreviations", metaValue_DayNames },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayNames },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "DayAbbreviations", metaValue_DayNames },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "\u00a4\u00a0#,##0.00",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "DatePatterns",
                new String[] {
                    "EEEE , 'ly\u025b'\u030c\u02bc d 'na' MMMM, y",
                    "'ly\u025b'\u030c\u02bc d 'na' MMMM, y",
                    "d MMM, y",
                    "dd/MM/yy",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayNames },
            { "MonthAbbreviations", metaValue_MonthNames },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "f\u0289\u0300\u02bc n\u00e8m" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "standalone.MonthAbbreviations", metaValue_MonthNames },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
        };
        return data;
    }
}
