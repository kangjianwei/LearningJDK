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

public class FormatData_hy extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u0570\u0578\u0582\u0576\u057e\u0561\u0580\u056b",
               "\u0583\u0565\u057f\u0580\u057e\u0561\u0580\u056b",
               "\u0574\u0561\u0580\u057f\u056b",
               "\u0561\u057a\u0580\u056b\u056c\u056b",
               "\u0574\u0561\u0575\u056b\u057d\u056b",
               "\u0570\u0578\u0582\u0576\u056b\u057d\u056b",
               "\u0570\u0578\u0582\u056c\u056b\u057d\u056b",
               "\u0585\u0563\u0578\u057d\u057f\u0578\u057d\u056b",
               "\u057d\u0565\u057a\u057f\u0565\u0574\u0562\u0565\u0580\u056b",
               "\u0570\u0578\u056f\u057f\u0565\u0574\u0562\u0565\u0580\u056b",
               "\u0576\u0578\u0575\u0565\u0574\u0562\u0565\u0580\u056b",
               "\u0564\u0565\u056f\u057f\u0565\u0574\u0562\u0565\u0580\u056b",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u0570\u0576\u057e",
               "\u0583\u057f\u057e",
               "\u0574\u0580\u057f",
               "\u0561\u057a\u0580",
               "\u0574\u0575\u057d",
               "\u0570\u0576\u057d",
               "\u0570\u056c\u057d",
               "\u0585\u0563\u057d",
               "\u057d\u0565\u057a",
               "\u0570\u0578\u056f",
               "\u0576\u0578\u0575",
               "\u0564\u0565\u056f",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "\u0540",
               "\u0553",
               "\u0544",
               "\u0531",
               "\u0544",
               "\u0540",
               "\u0540",
               "\u0555",
               "\u054d",
               "\u0540",
               "\u0546",
               "\u0534",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u056f\u056b\u0580\u0561\u056f\u056b",
               "\u0565\u0580\u056f\u0578\u0582\u0577\u0561\u0562\u0569\u056b",
               "\u0565\u0580\u0565\u0584\u0577\u0561\u0562\u0569\u056b",
               "\u0579\u0578\u0580\u0565\u0584\u0577\u0561\u0562\u0569\u056b",
               "\u0570\u056b\u0576\u0563\u0577\u0561\u0562\u0569\u056b",
               "\u0578\u0582\u0580\u0562\u0561\u0569",
               "\u0577\u0561\u0562\u0561\u0569",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u056f\u056b\u0580",
               "\u0565\u0580\u056f",
               "\u0565\u0580\u0584",
               "\u0579\u0580\u0584",
               "\u0570\u0576\u0563",
               "\u0578\u0582\u0580",
               "\u0577\u0562\u0569",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u053f",
               "\u0535",
               "\u0535",
               "\u0549",
               "\u0540",
               "\u0548",
               "\u0547",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "1-\u056b\u0576 \u0565\u057c\u0561\u0574\u057d\u0575\u0561\u056f",
               "2-\u0580\u0564 \u0565\u057c\u0561\u0574\u057d\u0575\u0561\u056f",
               "3-\u0580\u0564 \u0565\u057c\u0561\u0574\u057d\u0575\u0561\u056f",
               "4-\u0580\u0564 \u0565\u057c\u0561\u0574\u057d\u0575\u0561\u056f",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "1-\u056b\u0576 \u0565\u057c\u0574\u057d.",
               "2-\u0580\u0564 \u0565\u057c\u0574\u057d.",
               "3-\u0580\u0564 \u0565\u057c\u0574\u057d.",
               "4-\u0580\u0564 \u0565\u057c\u0574\u057d.",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u053f\u0531",
               "\u053f\u0540",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "\u0561",
               "\u0570",
            };
        final String[] metaValue_Eras = new String[] {
               "\u0574.\u0569.\u0561.",
               "\u0574.\u0569.",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "HH:mm:ss zzzz",
               "HH:mm:ss z",
               "HH:mm:ss",
               "HH:mm",
            };
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "d MMMM, y \u0569. G, EEEE",
               "dd MMMM, y \u0569. G",
               "dd MMM, y \u0569. G",
               "dd.MM.y GGGGG",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "d MMMM, y \u0569. GGGG, EEEE",
               "dd MMMM, y \u0569. GGGG",
               "dd MMM, y \u0569. GGGG",
               "dd.MM.y G",
            };
        final String metaValue_calendarname_gregorian = "\u0563\u0580\u056b\u0563\u0578\u0580\u0575\u0561\u0576 \u0585\u0580\u0561\u0581\u0578\u0582\u0575\u0581";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u057f\u0561\u0580\u056b" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras", metaValue_Eras },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u0573\u0561\u057a\u0578\u0576\u0561\u056f\u0561\u0576 \u0585\u0580\u0561\u0581\u0578\u0582\u0575\u0581" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "long.Eras",
                new String[] {
                    "\u0554\u0580\u056b\u057d\u057f\u0578\u057d\u056b\u0581 \u0561\u057c\u0561\u057b",
                    "\u0554\u0580\u056b\u057d\u057f\u0578\u057d\u056b\u0581 \u0570\u0565\u057f\u0578",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns",
                new String[] {
                    "{1}, {0}",
                    "{1}, {0}",
                    "{1}, {0}",
                    "{1}, {0}",
                }
            },
            { "narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
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
                    "\u0548\u0579\u0539",
                }
            },
            { "MonthNarrows", metaValue_MonthNarrows },
            { "japanese.DatePatterns", metaValue_buddhist_DatePatterns },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u0580\u0578\u057a\u0565" },
            { "field.era", "\u0569\u057e\u0561\u0580\u056f\u0578\u0582\u0569\u0575\u0578\u0582\u0576" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u053f\u0531/\u053f\u0540" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.roc", "\u0574\u056b\u0576\u0563\u0578\u0582\u0578 \u0585\u0580\u0561\u0581\u0578\u0582\u0575\u0581" },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "field.month", "\u0561\u0574\u056b\u057d" },
            { "field.second", "\u057e\u0561\u0575\u0580\u056f\u0575\u0561\u0576" },
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
            { "calendarname.islamic", "\u056b\u057d\u056c\u0561\u0574\u0561\u056f\u0561\u0576 \u0585\u0580\u0561\u0581\u0578\u0582\u0575\u0581" },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
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
            { "field.zone", "\u056a\u0561\u0574\u0561\u0575\u056b\u0576 \u0563\u0578\u057f\u056b" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras", metaValue_Eras },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "\u0577\u0561\u0562\u0561\u0569\u057e\u0561 \u0585\u0580" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u0577\u0561\u0562\u0561\u0569" },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "DatePatterns",
                new String[] {
                    "y \u0569. MMMM d, EEEE",
                    "dd MMMM, y \u0569.",
                    "dd MMM, y \u0569.",
                    "dd.MM.yy",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u056a\u0561\u0574" },
            { "calendarname.buddhist", "\u0562\u0578\u0582\u0564\u0564\u0561\u0575\u0561\u056f\u0561\u0576 \u0585\u0580\u0561\u0581\u0578\u0582\u0575\u0581" },
            { "standalone.MonthNames",
                new String[] {
                    "\u0570\u0578\u0582\u0576\u057e\u0561\u0580",
                    "\u0583\u0565\u057f\u0580\u057e\u0561\u0580",
                    "\u0574\u0561\u0580\u057f",
                    "\u0561\u057a\u0580\u056b\u056c",
                    "\u0574\u0561\u0575\u056b\u057d",
                    "\u0570\u0578\u0582\u0576\u056b\u057d",
                    "\u0570\u0578\u0582\u056c\u056b\u057d",
                    "\u0585\u0563\u0578\u057d\u057f\u0578\u057d",
                    "\u057d\u0565\u057a\u057f\u0565\u0574\u0562\u0565\u0580",
                    "\u0570\u0578\u056f\u057f\u0565\u0574\u0562\u0565\u0580",
                    "\u0576\u0578\u0575\u0565\u0574\u0562\u0565\u0580",
                    "\u0564\u0565\u056f\u057f\u0565\u0574\u0562\u0565\u0580",
                    "",
                }
            },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
