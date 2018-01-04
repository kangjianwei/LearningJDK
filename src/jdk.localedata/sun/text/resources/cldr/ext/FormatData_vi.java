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

public class FormatData_vi extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "th\u00e1ng 1",
               "th\u00e1ng 2",
               "th\u00e1ng 3",
               "th\u00e1ng 4",
               "th\u00e1ng 5",
               "th\u00e1ng 6",
               "th\u00e1ng 7",
               "th\u00e1ng 8",
               "th\u00e1ng 9",
               "th\u00e1ng 10",
               "th\u00e1ng 11",
               "th\u00e1ng 12",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "thg 1",
               "thg 2",
               "thg 3",
               "thg 4",
               "thg 5",
               "thg 6",
               "thg 7",
               "thg 8",
               "thg 9",
               "thg 10",
               "thg 11",
               "thg 12",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "Ch\u1ee7 Nh\u1eadt",
               "Th\u1ee9 Hai",
               "Th\u1ee9 Ba",
               "Th\u1ee9 T\u01b0",
               "Th\u1ee9 N\u0103m",
               "Th\u1ee9 S\u00e1u",
               "Th\u1ee9 B\u1ea3y",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "CN",
               "Th 2",
               "Th 3",
               "Th 4",
               "Th 5",
               "Th 6",
               "Th 7",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "CN",
               "T2",
               "T3",
               "T4",
               "T5",
               "T6",
               "T7",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "Qu\u00fd 1",
               "Qu\u00fd 2",
               "Qu\u00fd 3",
               "Qu\u00fd 4",
            };
        final String[] metaValue_standalone_QuarterAbbreviations = new String[] {
               "Q1",
               "Q2",
               "Q3",
               "Q4",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "SA",
               "CH",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "s",
               "c",
            };
        final String[] metaValue_long_Eras = new String[] {
               "Tr\u01b0\u1edbc CN",
               "sau CN",
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
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_buddhist_long_Eras = new String[] {
               "BC",
               "BE",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE, 'ng\u00e0y' dd MMMM 'n\u0103m' y G",
               "'Ng\u00e0y' dd 'th\u00e1ng' M 'n\u0103m' y G",
               "dd-MM-y G",
               "dd/MM/y GGGGG",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE, 'ng\u00e0y' dd MMMM 'n\u0103m' y GGGG",
               "'Ng\u00e0y' dd 'th\u00e1ng' M 'n\u0103m' y GGGG",
               "dd-MM-y GGGG",
               "dd/MM/y G",
            };
        final String[] metaValue_roc_long_Eras = new String[] {
               "Tr\u01b0\u1edbc R.O.C",
               "R.O.C.",
            };
        final String[] metaValue_islamic_long_Eras = new String[] {
               "",
               "AH",
            };
        final String metaValue_calendarname_gregorian = "L\u1ecbch Gregory";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "N\u0103m" },
            { "calendarname.islamic-umalqura", "L\u1ecbch H\u1ed3i Gi\u00e1o - Umm al-Qura" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.Eras", metaValue_buddhist_long_Eras },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns",
                new String[] {
                    "EEEE, 'ng\u00e0y' dd MMMM 'n\u0103m' y G",
                    "'Ng\u00e0y' dd 'th\u00e1ng' M 'n\u0103m' y G",
                    "dd-MM-y G",
                    "dd/MM/y G",
                }
            },
            { "standalone.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "calendarname.islamic-civil", "L\u1ecbch Islamic-Civil" },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "roc.long.Eras", metaValue_roc_long_Eras },
            { "narrow.Eras",
                new String[] {
                    "tr. CN",
                    "sau CN",
                }
            },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "Gi\u1edd chu\u1ea9n {0}" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "L\u1ecbch Nh\u1eadt B\u1ea3n" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "long.Eras", metaValue_long_Eras },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns",
                new String[] {
                    "{0} {1}",
                    "{0} {1}",
                    "{0}, {1}",
                    "{0}, {1}",
                }
            },
            { "narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
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
            { "japanese.DatePatterns",
                new String[] {
                    "EEEE, 'ng\u00e0y' dd MMMM 'n\u0103m' y GGGG",
                    "'Ng\u00e0y' dd 'th\u00e1ng' M 'n\u0103m' y GGGG",
                    "dd-MM-y GGGG",
                    "dd/MM/y GGGG",
                }
            },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "Ph\u00fat" },
            { "field.era", "th\u1eddi \u0111\u1ea1i" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "SA/CH" },
            { "calendarname.roc", "L\u1ecbch Trung Hoa D\u00e2n Qu\u1ed1c" },
            { "islamic.DatePatterns",
                new String[] {
                    "EEEE, 'ng\u00e0y' dd 'th\u00e1ng' MM 'n\u0103m' y GGGG",
                    "'Ng\u00e0y' dd 'th\u00e1ng' M 'n\u0103m' y GGGG",
                    "dd-MM-y GGGG",
                    "dd/MM/y G",
                }
            },
            { "roc.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "islamic.Eras", metaValue_islamic_long_Eras },
            { "field.month", "Th\u00e1ng" },
            { "roc.Eras", metaValue_roc_long_Eras },
            { "field.second", "Gi\u00e2y" },
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
            { "calendarname.islamic", "L\u1ecbch H\u1ed3i Gi\u00e1o" },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations",
                new String[] {
                    "Thg 1",
                    "Thg 2",
                    "Thg 3",
                    "Thg 4",
                    "Thg 5",
                    "Thg 6",
                    "Thg 7",
                    "Thg 8",
                    "Thg 9",
                    "Thg 10",
                    "Thg 11",
                    "Thg 12",
                    "",
                }
            },
            { "timezone.regionFormat", "Gi\u1edd {0}" },
            { "roc.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames",
                new String[] {
                    "qu\u00fd 1",
                    "qu\u00fd 2",
                    "qu\u00fd 3",
                    "qu\u00fd 4",
                }
            },
            { "japanese.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.long.Eras", metaValue_buddhist_long_Eras },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "M\u00fai gi\u1edd" },
            { "japanese.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "roc.narrow.Eras", metaValue_roc_long_Eras },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_long_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.islamic.DatePatterns",
                new String[] {
                    "EEEE, 'ng\u00e0y' dd 'th\u00e1ng' MM 'n\u0103m' y G",
                    "'Ng\u00e0y' dd 'th\u00e1ng' M 'n\u0103m' y G",
                    "dd-MM-y G",
                    "dd/MM/y GGGGG",
                }
            },
            { "field.weekday", "ng\u00e0y trong tu\u1ea7n" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.long.Eras", metaValue_islamic_long_Eras },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_standalone_QuarterAbbreviations },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "buddhist.Eras", metaValue_buddhist_long_Eras },
            { "field.week", "Tu\u1ea7n" },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "buddhist.MonthNarrows", metaValue_buddhist_MonthNarrows },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "Gi\u1edd m\u00f9a h\u00e8 {0}" },
            { "DatePatterns",
                new String[] {
                    "EEEE, d MMMM, y",
                    "d MMMM, y",
                    "d MMM, y",
                    "dd/MM/y",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "Gi\u1edd" },
            { "islamic.narrow.Eras", metaValue_islamic_long_Eras },
            { "calendarname.buddhist", "L\u1ecbch Ph\u1eadt Gi\u00e1o" },
            { "standalone.MonthNames",
                new String[] {
                    "Th\u00e1ng 1",
                    "Th\u00e1ng 2",
                    "Th\u00e1ng 3",
                    "Th\u00e1ng 4",
                    "Th\u00e1ng 5",
                    "Th\u00e1ng 6",
                    "Th\u00e1ng 7",
                    "Th\u00e1ng 8",
                    "Th\u00e1ng 9",
                    "Th\u00e1ng 10",
                    "Th\u00e1ng 11",
                    "Th\u00e1ng 12",
                    "",
                }
            },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
