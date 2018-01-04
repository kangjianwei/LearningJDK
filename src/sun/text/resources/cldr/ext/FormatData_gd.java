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

public class FormatData_gd extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "dhen Fhaoilleach",
               "dhen Ghearran",
               "dhen Mh\u00e0rt",
               "dhen Ghiblean",
               "dhen Ch\u00e8itean",
               "dhen \u00d2gmhios",
               "dhen Iuchar",
               "dhen L\u00f9nastal",
               "dhen t-Sultain",
               "dhen D\u00e0mhair",
               "dhen t-Samhain",
               "dhen D\u00f9bhlachd",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "Faoi",
               "Gearr",
               "M\u00e0rt",
               "Gibl",
               "C\u00e8it",
               "\u00d2gmh",
               "Iuch",
               "L\u00f9na",
               "Sult",
               "D\u00e0mh",
               "Samh",
               "D\u00f9bh",
               "",
            };
        final String[] metaValue_MonthNarrows = new String[] {
               "F",
               "G",
               "M",
               "G",
               "C",
               "\u00d2",
               "I",
               "L",
               "S",
               "D",
               "S",
               "D",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "DiD\u00f2mhnaich",
               "DiLuain",
               "DiM\u00e0irt",
               "DiCiadain",
               "DiarDaoin",
               "DihAoine",
               "DiSathairne",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "DiD",
               "DiL",
               "DiM",
               "DiC",
               "Dia",
               "Dih",
               "DiS",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "D",
               "L",
               "M",
               "C",
               "A",
               "H",
               "S",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "1d chairteal",
               "2na cairteal",
               "3s cairteal",
               "4mh cairteal",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "C1",
               "C2",
               "C3",
               "C4",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "m",
               "f",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "HH:mm:ss zzzz",
               "HH:mm:ss z",
               "HH:mm:ss",
               "HH:mm",
            };
        final String[] metaValue_DateTimePatterns = new String[] {
               "{1} {0}",
               "{1} {0}",
               "{1} {0}",
               "{1} {0}",
            };
        final String[] metaValue_buddhist_QuarterNarrows = new String[] {
               "1",
               "2",
               "3",
               "4",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "EEEE, d MMMM y G",
               "d MMMM y G",
               "d MMM y G",
               "d/M/y GGGGG",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "EEEE, d MMMM y GGGG",
               "d MMMM y GGGG",
               "d MMM y GGGG",
               "d/M/y G",
            };
        final String[] metaValue_roc_Eras = new String[] {
               "Ro PnS",
               "M\u00edngu\u00f3",
            };
        final String metaValue_calendarname_gregorian = "Am M\u00ecosachan Griogarach";
        final String[] metaValue_brah_NumberElements = new String[] {
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
            };
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "bliadhna" },
            { "calendarname.islamic-umalqura", "Am M\u00ecosachan Ioslamach (Umm al-Qura)" },
            { "arabext.NumberElements",
                new String[] {
                    "\u066b",
                    "\u066c",
                    "\u061b",
                    "\u066a",
                    "\u06f0",
                    "#",
                    "-",
                    "\u00d7\u06f1\u06f0^",
                    "\u0609",
                    "\u221e",
                    "NaN",
                }
            },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "standalone.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "lana.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u1a80",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "roc.MonthNarrows", metaValue_MonthNarrows },
            { "calendarname.islamic-civil", "Am M\u00ecosachan Ioslamach (cl\u00e0rach, linn s\u00ecobhalta)" },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "mymr.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u1040",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "narrow.Eras",
                new String[] {
                    "R",
                    "A",
                }
            },
            { "roc.long.Eras",
                new String[] {
                    "Ro Ph. na S\u00ecne",
                    "M\u00edngu\u00f3",
                }
            },
            { "talu.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u19d0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "Bun-\u00e0m: {0}" },
            { "tamldec.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0be6",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "Am M\u00ecosachan Seapanach" },
            { "mtei.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\uabf0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "beng.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u09e6",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "long.Eras",
                new String[] {
                    "Ro Chr\u00ecosta",
                    "An d\u00e8idh Chr\u00ecosta",
                }
            },
            { "saur.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\ua8d0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns", metaValue_DateTimePatterns },
            { "narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "latn.NumberElements", metaValue_brah_NumberElements },
            { "MonthNarrows", metaValue_MonthNarrows },
            { "japanese.DatePatterns", metaValue_buddhist_DatePatterns },
            { "guru.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0a66",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "mionaid" },
            { "field.era", "linn" },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "m/f" },
            { "standalone.MonthNarrows", metaValue_MonthNarrows },
            { "cham.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\uaa50",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "arab.NumberElements",
                new String[] {
                    "\u066b",
                    "\u066c",
                    "\u061b",
                    "\u066a",
                    "\u0660",
                    "#",
                    "\u061c-",
                    "\u0627\u0633",
                    "\u0609",
                    "\u221e",
                    "NaN",
                }
            },
            { "shrd.NumberElements", metaValue_brah_NumberElements },
            { "takr.NumberElements", metaValue_brah_NumberElements },
            { "calendarname.roc", "M\u00ecosachan Poblachd na S\u00ecne" },
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
            { "field.month", "m\u00ecos" },
            { "osma.NumberElements", metaValue_brah_NumberElements },
            { "roc.Eras", metaValue_roc_Eras },
            { "field.second", "diog" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "\u00a4#,##0.00",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "calendarname.islamic", "Am M\u00ecosachan Ioslamach" },
            { "knda.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0ce6",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "bali.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u1b50",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "brah.NumberElements", metaValue_brah_NumberElements },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "olck.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u1c50",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "japanese.MonthNarrows", metaValue_MonthNarrows },
            { "islamic.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "mymrshan.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u1090",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "mlym.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0d66",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "java.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\ua9d0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "TimePatterns", metaValue_TimePatterns },
            { "lepc.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u1c40",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "fullwide.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\uff10",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "khmr.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u17e0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "field.zone", "roinn-t\u00ecde" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DateTimePatterns", metaValue_DateTimePatterns },
            { "roc.narrow.Eras", metaValue_roc_Eras },
            { "laoo.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0ed0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "sund.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u1bb0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "thai.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0e50",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras",
                new String[] {
                    "RC",
                    "AD",
                }
            },
            { "kali.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\ua900",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "mong.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u1810",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "field.weekday", "latha na seachdaine" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "telu.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0c66",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "nkoo.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u07c0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "cakm.NumberElements", metaValue_brah_NumberElements },
            { "tibt.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0f20",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "lanatham.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u1a90",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "roc.MonthNames", metaValue_MonthNames },
            { "sora.NumberElements", metaValue_brah_NumberElements },
            { "field.week", "seachdain" },
            { "gujr.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0ae6",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "buddhist.MonthNarrows", metaValue_MonthNarrows },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "limb.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u1946",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "orya.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\u0b66",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "T\u00ecde samhraidh: {0}" },
            { "DatePatterns",
                new String[] {
                    "EEEE, d'mh' MMMM y",
                    "d'mh' MMMM y",
                    "d MMM y",
                    "dd/MM/y",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "uair a th\u00ecde" },
            { "vaii.NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "\ua620",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "calendarname.buddhist", "Am M\u00ecosachan Budach" },
            { "standalone.MonthNames",
                new String[] {
                    "Am Faoilleach",
                    "An Gearran",
                    "Am M\u00e0rt",
                    "An Giblean",
                    "An C\u00e8itean",
                    "An t-\u00d2gmhios",
                    "An t-Iuchar",
                    "An L\u00f9nastal",
                    "An t-Sultain",
                    "An D\u00e0mhair",
                    "An t-Samhain",
                    "An D\u00f9bhlachd",
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
