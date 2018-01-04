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

public class FormatData_ja extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "1\u6708",
               "2\u6708",
               "3\u6708",
               "4\u6708",
               "5\u6708",
               "6\u6708",
               "7\u6708",
               "8\u6708",
               "9\u6708",
               "10\u6708",
               "11\u6708",
               "12\u6708",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u65e5\u66dc\u65e5",
               "\u6708\u66dc\u65e5",
               "\u706b\u66dc\u65e5",
               "\u6c34\u66dc\u65e5",
               "\u6728\u66dc\u65e5",
               "\u91d1\u66dc\u65e5",
               "\u571f\u66dc\u65e5",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u65e5",
               "\u6708",
               "\u706b",
               "\u6c34",
               "\u6728",
               "\u91d1",
               "\u571f",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u7b2c1\u56db\u534a\u671f",
               "\u7b2c2\u56db\u534a\u671f",
               "\u7b2c3\u56db\u534a\u671f",
               "\u7b2c4\u56db\u534a\u671f",
            };
        final String[] metaValue_standalone_QuarterAbbreviations = new String[] {
               "Q1",
               "Q2",
               "Q3",
               "Q4",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u5348\u524d",
               "\u5348\u5f8c",
            };
        final String[] metaValue_long_Eras = new String[] {
               "\u7d00\u5143\u524d",
               "\u897f\u66a6",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "H\u6642mm\u5206ss\u79d2 zzzz",
               "H:mm:ss z",
               "H:mm:ss",
               "H:mm",
            };
        final String[] metaValue_DateTimePatterns = new String[] {
               "{1} {0}",
               "{1} {0}",
               "{1} {0}",
               "{1} {0}",
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
        final String[] metaValue_buddhist_Eras = new String[] {
               "BC",
               "BE",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "GGGGy\u5e74M\u6708d\u65e5EEEE",
               "GGGGy\u5e74M\u6708d\u65e5",
               "GGGGy/MM/dd",
               "GGGGy/MM/dd",
            };
        final String[] metaValue_japanese_long_Eras = new String[] {
               "\u897f\u66a6",
               "\u660e\u6cbb",
               "\u5927\u6b63",
               "\u662d\u548c",
               "\u5e73\u6210",
            };
        final String[] metaValue_roc_Eras = new String[] {
               "\u6c11\u56fd\u524d",
               "\u6c11\u56fd",
            };
        final String[] metaValue_java_time_roc_DatePatterns = new String[] {
               "Gy\u5e74M\u6708d\u65e5EEEE",
               "Gy\u5e74M\u6708d\u65e5",
               "Gy/MM/dd",
               "Gy/MM/dd",
            };
        final String[] metaValue_islamic_MonthNames = new String[] {
               "\u30e0\u30cf\u30c3\u30e9\u30e0",
               "\u30b5\u30d5\u30a2\u30eb",
               "\u30e9\u30d3\u30fc\u30fb\u30a6\u30eb\u30fb\u30a2\u30a6\u30ef\u30eb",
               "\u30e9\u30d3\u30fc\u30fb\u30a6\u30c3\u30fb\u30b5\u30fc\u30cb\u30fc",
               "\u30b8\u30e5\u30de\u30fc\u30c0\u30eb\u30fb\u30a2\u30a6\u30ef\u30eb",
               "\u30b8\u30e5\u30de\u30fc\u30c0\u30c3\u30b5\u30fc\u30cb\u30fc",
               "\u30e9\u30b8\u30e3\u30d6",
               "\u30b7\u30e3\u30a2\u30d0\u30fc\u30f3",
               "\u30e9\u30de\u30c0\u30fc\u30f3",
               "\u30b7\u30e3\u30a6\u30ef\u30fc\u30eb",
               "\u30ba\u30eb\u30fb\u30ab\u30a4\u30c0",
               "\u30ba\u30eb\u30fb\u30d2\u30c3\u30b8\u30e3",
               "",
            };
        final String[] metaValue_islamic_long_Eras = new String[] {
               "",
               "AH",
            };
        final String metaValue_calendarname_gregorian = "\u897f\u66a6(\u30b0\u30ec\u30b4\u30ea\u30aa\u66a6)";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u5e74" },
            { "calendarname.islamic-umalqura", "\u30a4\u30b9\u30e9\u30e0\u66a6(\u30a6\u30f3\u30e0\u30fb\u30a2\u30eb\u30af\u30e9\u30fc)" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.Eras", metaValue_buddhist_Eras },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns",
                new String[] {
                    "Gy\u5e74M\u6708d\u65e5EEEE",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gy\u5e74M\u6708d\u65e5",
                    "GGGGGy/M/d",
                }
            },
            { "standalone.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "japanese.narrow.Eras",
                new String[] {
                    "AD",
                    "M",
                    "T",
                    "S",
                    "H",
                }
            },
            { "roc.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "calendarname.islamic-civil", "\u30a4\u30b9\u30e9\u30e0\u6b74(\u5b9a\u5468\u671f\u3001\u516c\u6c11\u7d00\u5143)" },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras",
                new String[] {
                    "BC",
                    "AD",
                }
            },
            { "roc.long.Eras",
                new String[] {
                    "\u6c11\u56fd\u524d",
                    "",
                }
            },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "{0}\u6a19\u6e96\u6642" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u548c\u66a6" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthNames },
            { "long.Eras", metaValue_long_Eras },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthNames },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns", metaValue_DateTimePatterns },
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
            { "japanese.DatePatterns",
                new String[] {
                    "GGGGy\u5e74M\u6708d\u65e5EEEE",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "Gy/M/d",
                }
            },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u5206" },
            { "japanese.long.Eras", metaValue_japanese_long_Eras },
            { "field.era", "\u6642\u4ee3" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u5348\u524d/\u5348\u5f8c" },
            { "islamic.MonthNames", metaValue_islamic_MonthNames },
            { "calendarname.roc", "\u4e2d\u83ef\u6c11\u56fd\u66a6" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "islamic.Eras", metaValue_islamic_long_Eras },
            { "field.month", "\u6708" },
            { "roc.Eras", metaValue_roc_Eras },
            { "field.second", "\u79d2" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNarrows", metaValue_DayAbbreviations },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "\u00a4#,##0.00",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "calendarname.islamic", "\u30a4\u30b9\u30e9\u30e0\u66a6" },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthNames },
            { "timezone.regionFormat", "{0}\u6642\u9593" },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "standalone.DayNarrows", metaValue_DayAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.long.Eras",
                new String[] {
                    "BC",
                    "\u4ecf\u66a6",
                }
            },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayAbbreviations },
            { "field.zone", "\u30bf\u30a4\u30e0\u30be\u30fc\u30f3" },
            { "japanese.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "roc.narrow.Eras", metaValue_roc_Eras },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_long_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.islamic.DatePatterns", metaValue_java_time_roc_DatePatterns },
            { "field.weekday", "\u66dc\u65e5" },
            { "japanese.MonthAbbreviations", metaValue_MonthNames },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.long.Eras", metaValue_islamic_long_Eras },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "buddhist.Eras", metaValue_buddhist_Eras },
            { "field.week", "\u9031" },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "buddhist.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayAbbreviations },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_roc_DatePatterns },
            { "java.time.buddhist.DatePatterns",
                new String[] {
                    "GGGGy\u5e74M\u6708d\u65e5EEEE",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "Gy/MM/dd",
                    "Gy/MM/dd",
                }
            },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "{0}\u590f\u6642\u9593" },
            { "DatePatterns",
                new String[] {
                    "y\u5e74M\u6708d\u65e5EEEE",
                    "y\u5e74M\u6708d\u65e5",
                    "y/MM/dd",
                    "y/MM/dd",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "japanese.DateTimePatterns", metaValue_DateTimePatterns },
            { "MonthAbbreviations", metaValue_MonthNames },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u6642" },
            { "islamic.MonthAbbreviations", metaValue_islamic_MonthNames },
            { "islamic.narrow.Eras", metaValue_islamic_long_Eras },
            { "calendarname.buddhist", "\u4ecf\u66a6" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "buddhist.DayNarrows", metaValue_DayAbbreviations },
            { "japanese.DayNarrows", metaValue_DayAbbreviations },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "japanese.Eras", metaValue_japanese_long_Eras },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
