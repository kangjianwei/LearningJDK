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

public class FormatData_th extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u0e21\u0e01\u0e23\u0e32\u0e04\u0e21",
               "\u0e01\u0e38\u0e21\u0e20\u0e32\u0e1e\u0e31\u0e19\u0e18\u0e4c",
               "\u0e21\u0e35\u0e19\u0e32\u0e04\u0e21",
               "\u0e40\u0e21\u0e29\u0e32\u0e22\u0e19",
               "\u0e1e\u0e24\u0e29\u0e20\u0e32\u0e04\u0e21",
               "\u0e21\u0e34\u0e16\u0e38\u0e19\u0e32\u0e22\u0e19",
               "\u0e01\u0e23\u0e01\u0e0e\u0e32\u0e04\u0e21",
               "\u0e2a\u0e34\u0e07\u0e2b\u0e32\u0e04\u0e21",
               "\u0e01\u0e31\u0e19\u0e22\u0e32\u0e22\u0e19",
               "\u0e15\u0e38\u0e25\u0e32\u0e04\u0e21",
               "\u0e1e\u0e24\u0e28\u0e08\u0e34\u0e01\u0e32\u0e22\u0e19",
               "\u0e18\u0e31\u0e19\u0e27\u0e32\u0e04\u0e21",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "\u0e21.\u0e04.",
               "\u0e01.\u0e1e.",
               "\u0e21\u0e35.\u0e04.",
               "\u0e40\u0e21.\u0e22.",
               "\u0e1e.\u0e04.",
               "\u0e21\u0e34.\u0e22.",
               "\u0e01.\u0e04.",
               "\u0e2a.\u0e04.",
               "\u0e01.\u0e22.",
               "\u0e15.\u0e04.",
               "\u0e1e.\u0e22.",
               "\u0e18.\u0e04.",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u0e27\u0e31\u0e19\u0e2d\u0e32\u0e17\u0e34\u0e15\u0e22\u0e4c",
               "\u0e27\u0e31\u0e19\u0e08\u0e31\u0e19\u0e17\u0e23\u0e4c",
               "\u0e27\u0e31\u0e19\u0e2d\u0e31\u0e07\u0e04\u0e32\u0e23",
               "\u0e27\u0e31\u0e19\u0e1e\u0e38\u0e18",
               "\u0e27\u0e31\u0e19\u0e1e\u0e24\u0e2b\u0e31\u0e2a\u0e1a\u0e14\u0e35",
               "\u0e27\u0e31\u0e19\u0e28\u0e38\u0e01\u0e23\u0e4c",
               "\u0e27\u0e31\u0e19\u0e40\u0e2a\u0e32\u0e23\u0e4c",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u0e2d\u0e32.",
               "\u0e08.",
               "\u0e2d.",
               "\u0e1e.",
               "\u0e1e\u0e24.",
               "\u0e28.",
               "\u0e2a.",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "\u0e2d\u0e32",
               "\u0e08",
               "\u0e2d",
               "\u0e1e",
               "\u0e1e\u0e24",
               "\u0e28",
               "\u0e2a",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "\u0e44\u0e15\u0e23\u0e21\u0e32\u0e2a 1",
               "\u0e44\u0e15\u0e23\u0e21\u0e32\u0e2a 2",
               "\u0e44\u0e15\u0e23\u0e21\u0e32\u0e2a 3",
               "\u0e44\u0e15\u0e23\u0e21\u0e32\u0e2a 4",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u0e01\u0e48\u0e2d\u0e19\u0e40\u0e17\u0e35\u0e48\u0e22\u0e07",
               "\u0e2b\u0e25\u0e31\u0e07\u0e40\u0e17\u0e35\u0e48\u0e22\u0e07",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "a",
               "p",
            };
        final String[] metaValue_TimePatterns = new String[] {
               "H \u0e19\u0e32\u0e2c\u0e34\u0e01\u0e32 mm \u0e19\u0e32\u0e17\u0e35 ss \u0e27\u0e34\u0e19\u0e32\u0e17\u0e35 zzzz",
               "H \u0e19\u0e32\u0e2c\u0e34\u0e01\u0e32 mm \u0e19\u0e32\u0e17\u0e35 ss \u0e27\u0e34\u0e19\u0e32\u0e17\u0e35 z",
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
        final String[] metaValue_buddhist_Eras = new String[] {
               "BC",
               "\u0e1e.\u0e28.",
            };
        final String[] metaValue_roc_long_Eras = new String[] {
               "\u0e1b\u0e35\u0e01\u0e48\u0e2d\u0e19\u0e44\u0e15\u0e49\u0e2b\u0e27\u0e31\u0e19",
               "\u0e44\u0e15\u0e49\u0e2b\u0e27\u0e31\u0e19",
            };
        final String[] metaValue_islamic_Eras = new String[] {
               "",
               "\u0e2e.\u0e28.",
            };
        final String metaValue_calendarname_gregorian = "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e40\u0e01\u0e23\u0e01\u0e2d\u0e40\u0e23\u0e35\u0e22\u0e19";
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u0e1b\u0e35" },
            { "calendarname.islamic-umalqura", "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e2d\u0e34\u0e2a\u0e25\u0e32\u0e21 (\u0e2d\u0e38\u0e21\u0e21\u0e4c\u0e2d\u0e31\u0e25\u0e01\u0e38\u0e23\u0e32)" },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.Eras", metaValue_buddhist_Eras },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns",
                new String[] {
                    "EEEE\u0e17\u0e35\u0e48 d MMMM \u0e1b\u0e35G\u0e17\u0e35\u0e48 y",
                    "d MMMM \u0e1b\u0e35G y",
                    "d MMM G y",
                    "d/M/yy G",
                }
            },
            { "standalone.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "roc.MonthNarrows", metaValue_MonthAbbreviations },
            { "calendarname.islamic-civil", "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e2d\u0e34\u0e2a\u0e25\u0e32\u0e21\u0e0b\u0e35\u0e27\u0e34\u0e25" },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "japanese.TimePatterns", metaValue_TimePatterns },
            { "roc.long.Eras", metaValue_roc_long_Eras },
            { "narrow.Eras",
                new String[] {
                    "\u0e01\u0e48\u0e2d\u0e19 \u0e04.\u0e28.",
                    "\u0e04.\u0e28.",
                }
            },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "timezone.regionFormat.standard", "\u0e40\u0e27\u0e25\u0e32\u0e21\u0e32\u0e15\u0e23\u0e10\u0e32\u0e19{0}" },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.japanese", "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e0d\u0e35\u0e48\u0e1b\u0e38\u0e48\u0e19" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "long.Eras",
                new String[] {
                    "\u0e1b\u0e35\u0e01\u0e48\u0e2d\u0e19\u0e04\u0e23\u0e34\u0e2a\u0e15\u0e4c\u0e28\u0e31\u0e01\u0e23\u0e32\u0e0a",
                    "\u0e04\u0e23\u0e34\u0e2a\u0e15\u0e4c\u0e28\u0e31\u0e01\u0e23\u0e32\u0e0a",
                }
            },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "DateTimePatterns", metaValue_DateTimePatterns },
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
            { "MonthNarrows", metaValue_MonthAbbreviations },
            { "japanese.DatePatterns",
                new String[] {
                    "EEEE\u0e17\u0e35\u0e48 d MMMM \u0e1b\u0e35GGGG\u0e17\u0e35\u0e48 y",
                    "d MMMM \u0e1b\u0e35GGGG y",
                    "d MMM GGGG y",
                    "d/M/yy GGGG",
                }
            },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u0e19\u0e32\u0e17\u0e35" },
            { "field.era", "\u0e2a\u0e21\u0e31\u0e22" },
            { "japanese.long.Eras",
                new String[] {
                    "\u0e04\u0e23\u0e34\u0e2a\u0e15\u0e4c\u0e28\u0e31\u0e01\u0e23\u0e32\u0e0a",
                    "\u0e40\u0e21\u0e08\u0e34",
                    "\u0e17\u0e30\u0e2d\u0e34\u0e42\u0e0a",
                    "\u0e42\u0e0a\u0e27\u0e30",
                    "\u0e40\u0e2e\u0e40\u0e0b",
                }
            },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u0e0a\u0e48\u0e27\u0e07\u0e27\u0e31\u0e19" },
            { "standalone.MonthNarrows", metaValue_MonthAbbreviations },
            { "islamic.MonthNames",
                new String[] {
                    "\u0e21\u0e38\u0e2e\u0e30\u0e23\u0e4c\u0e23\u0e2d\u0e21",
                    "\u0e0b\u0e2d\u0e1f\u0e32\u0e23\u0e4c",
                    "\u0e23\u0e2d\u0e1a\u0e35 I",
                    "\u0e23\u0e2d\u0e1a\u0e35 II",
                    "\u0e08\u0e38\u0e21\u0e32\u0e14\u0e32 I",
                    "\u0e08\u0e38\u0e21\u0e32\u0e14\u0e32 II",
                    "\u0e23\u0e2d\u0e08\u0e31\u0e1a",
                    "\u0e0a\u0e30\u0e2d\u0e30\u0e1a\u0e32\u0e19",
                    "\u0e23\u0e2d\u0e21\u0e30\u0e14\u0e2d\u0e19",
                    "\u0e40\u0e0a\u0e32\u0e27\u0e31\u0e25",
                    "\u0e0b\u0e38\u0e25\u0e01\u0e34\u0e2d\u0e3a\u0e14\u0e30\u0e2e\u0e3a",
                    "\u0e0b\u0e38\u0e25\u0e2b\u0e34\u0e08\u0e0d\u0e30\u0e2e\u0e3a",
                    "",
                }
            },
            { "calendarname.roc", "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e44\u0e15\u0e49\u0e2b\u0e27\u0e31\u0e19" },
            { "islamic.DatePatterns",
                new String[] {
                    "EEEE\u0e17\u0e35\u0e48 d MMMM GGGG y",
                    "d MMMM GGGG y",
                    "d MMM GGGG y",
                    "d/M/y GGGG",
                }
            },
            { "roc.QuarterAbbreviations", metaValue_QuarterNames },
            { "islamic.Eras", metaValue_islamic_Eras },
            { "field.month", "\u0e40\u0e14\u0e37\u0e2d\u0e19" },
            { "roc.Eras", metaValue_roc_long_Eras },
            { "field.second", "\u0e27\u0e34\u0e19\u0e32\u0e17\u0e35" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNarrows", metaValue_DayNarrows },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "\u00a4#,##0.00",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns",
                new String[] {
                    "EEEE\u0e17\u0e35\u0e48 d MMMM \u0e1b\u0e35GGGG\u0e17\u0e35\u0e48 y",
                    "d MMMM \u0e1b\u0e35GGGG y",
                    "d MMM GGGG y",
                    "d/M/y GGGG",
                }
            },
            { "calendarname.islamic", "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e2d\u0e34\u0e2a\u0e25\u0e32\u0e21" },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.TimePatterns", metaValue_TimePatterns },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "timezone.regionFormat", "\u0e40\u0e27\u0e25\u0e32{0}" },
            { "roc.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "japanese.MonthNarrows", metaValue_MonthAbbreviations },
            { "islamic.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.long.Eras",
                new String[] {
                    "BC",
                    "\u0e1e\u0e38\u0e17\u0e18\u0e28\u0e31\u0e01\u0e23\u0e32\u0e0a",
                }
            },
            { "TimePatterns", metaValue_TimePatterns },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "field.zone", "\u0e40\u0e02\u0e15\u0e40\u0e27\u0e25\u0e32" },
            { "japanese.QuarterAbbreviations", metaValue_QuarterNames },
            { "roc.narrow.Eras", metaValue_roc_long_Eras },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "Eras",
                new String[] {
                    "\u0e1b\u0e35\u0e01\u0e48\u0e2d\u0e19 \u0e04.\u0e28.",
                    "\u0e04.\u0e28.",
                }
            },
            { "roc.DayNames", metaValue_DayNames },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.islamic.DatePatterns",
                new String[] {
                    "EEEE\u0e17\u0e35\u0e48 d MMMM G y",
                    "d MMMM G y",
                    "d MMM G y",
                    "d/M/y G",
                }
            },
            { "field.weekday", "\u0e27\u0e31\u0e19\u0e43\u0e19\u0e2a\u0e31\u0e1b\u0e14\u0e32\u0e2b\u0e4c" },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.long.Eras",
                new String[] {
                    "",
                    "\u0e2e\u0e34\u0e08\u0e40\u0e23\u0e32\u0e30\u0e2b\u0e4c\u0e28\u0e31\u0e01\u0e23\u0e32\u0e0a",
                }
            },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "buddhist.QuarterAbbreviations", metaValue_QuarterNames },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "buddhist.DatePatterns",
                new String[] {
                    "EEEE\u0e17\u0e35\u0e48 d MMMM GGGG y",
                    "d MMMM y",
                    "d MMM y",
                    "d/M/yy",
                }
            },
            { "roc.MonthNames", metaValue_MonthNames },
            { "buddhist.Eras", metaValue_buddhist_Eras },
            { "field.week", "\u0e2a\u0e31\u0e1b\u0e14\u0e32\u0e2b\u0e4c" },
            { "buddhist.MonthNarrows", metaValue_MonthAbbreviations },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "islamic.QuarterNarrows", metaValue_buddhist_QuarterNarrows },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.buddhist.DatePatterns",
                new String[] {
                    "EEEE\u0e17\u0e35\u0e48 d MMMM G y",
                    "d MMMM y",
                    "d MMM y",
                    "d/M/yy",
                }
            },
            { "java.time.roc.DatePatterns",
                new String[] {
                    "EEEE\u0e17\u0e35\u0e48 d MMMM \u0e1b\u0e35G\u0e17\u0e35\u0e48 y",
                    "d MMMM \u0e1b\u0e35G y",
                    "d MMM G y",
                    "d/M/y G",
                }
            },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", "\u0e40\u0e27\u0e25\u0e32\u0e2d\u0e2d\u0e21\u0e41\u0e2a\u0e07{0}" },
            { "DatePatterns",
                new String[] {
                    "EEEE\u0e17\u0e35\u0e48 d MMMM G y",
                    "d MMMM G y",
                    "d MMM y",
                    "d/M/yy",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.TimePatterns", metaValue_TimePatterns },
            { "japanese.DateTimePatterns", metaValue_DateTimePatterns },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "field.hour", "\u0e0a\u0e31\u0e48\u0e27\u0e42\u0e21\u0e07" },
            { "islamic.MonthAbbreviations",
                new String[] {
                    "\u0e21\u0e38\u0e2e\u0e31\u0e23.",
                    "\u0e40\u0e28\u0e32\u0e30.",
                    "\u0e23\u0e2d\u0e1a\u0e35 I",
                    "\u0e23\u0e2d\u0e1a\u0e35 II",
                    "\u0e08\u0e38\u0e21\u0e32\u0e14\u0e32 I",
                    "\u0e08\u0e38\u0e21\u0e32\u0e14\u0e32 II",
                    "\u0e40\u0e23\u0e32\u0e30.",
                    "\u0e0a\u0e30\u0e2d\u0e4c.",
                    "\u0e40\u0e23\u0e32\u0e30\u0e21\u0e30.",
                    "\u0e40\u0e0a\u0e32\u0e27.",
                    "\u0e0b\u0e38\u0e25\u0e01\u0e34\u0e2d\u0e3a.",
                    "\u0e0b\u0e38\u0e25\u0e2b\u0e34\u0e08.",
                    "",
                }
            },
            { "islamic.narrow.Eras", metaValue_islamic_Eras },
            { "calendarname.buddhist", "\u0e1b\u0e0f\u0e34\u0e17\u0e34\u0e19\u0e1e\u0e38\u0e17\u0e18" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "roc.TimePatterns", metaValue_TimePatterns },
            { "QuarterAbbreviations", metaValue_QuarterNames },
            { "japanese.Eras",
                new String[] {
                    "\u0e04.\u0e28.",
                    "\u0e40\u0e21\u0e08\u0e34",
                    "\u0e17\u0e30\u0e2d\u0e34\u0e42\u0e0a",
                    "\u0e42\u0e0a\u0e27\u0e30",
                    "\u0e40\u0e2e\u0e40\u0e0b",
                }
            },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
