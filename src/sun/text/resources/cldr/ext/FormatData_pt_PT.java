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

public class FormatData_pt_PT extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_DayAbbreviations = new String[] {
               "domingo",
               "segunda",
               "ter\u00e7a",
               "quarta",
               "quinta",
               "sexta",
               "s\u00e1bado",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "1.\u00ba trimestre",
               "2.\u00ba trimestre",
               "3.\u00ba trimestre",
               "4.\u00ba trimestre",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "da manh\u00e3",
               "da tarde",
            };
        final String[] metaValue_narrow_AmPmMarkers = new String[] {
               "a.m.",
               "p.m.",
            };
        final String metaValue_timezone_regionFormat_daylight = "Hora padr\u00e3o de {0}";
        final String[] metaValue_buddhist_long_Eras = new String[] {
               "BC",
               "BE",
            };
        final String[] metaValue_java_time_buddhist_DatePatterns = new String[] {
               "G y MMMM d, EEEE",
               "G y MMMM d",
               "G y MMM d",
               "d/M/y G",
            };
        final String[] metaValue_buddhist_DatePatterns = new String[] {
               "GGGG y MMMM d, EEEE",
               "GGGG y MMMM d",
               "GGGG y MMM d",
               "d/M/y GGGG",
            };
        final String metaValue_calendarname_gregorian = "Calend\u00e1rio gregoriano";
        final Object[][] data = new Object[][] {
            { "calendarname.islamic-umalqura", "Calend\u00e1rio isl\u00e2mico (Umm al-Qura)" },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.Eras", metaValue_buddhist_long_Eras },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.long.Eras", metaValue_buddhist_long_Eras },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.japanese.DatePatterns",
                new String[] {
                    "EEEE, d 'de' MMMM 'de' y G",
                    "d 'de' MMMM 'de' y G",
                    "dd/MM/y G",
                    "d/M/y G",
                }
            },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "calendarname.islamic-civil", "Calend\u00e1rio isl\u00e2mico (civil)" },
            { "islamic.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "abbreviated.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "timezone.regionFormat.standard", metaValue_timezone_regionFormat_daylight },
            { "japanese.abbreviated.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "calendarname.japanese", "Calend\u00e1rio japon\u00eas" },
            { "buddhist.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "java.time.islamic.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "DateTimePatterns",
                new String[] {
                    "{1} '\u00e0s' {0}",
                    "{1} '\u00e0s' {0}",
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
                    "NaN",
                }
            },
            { "japanese.DatePatterns",
                new String[] {
                    "EEEE, d 'de' MMMM 'de' y GGGG",
                    "d 'de' MMMM 'de' y GGGG",
                    "dd/MM/y GGGG",
                    "d/M/y GGGG",
                }
            },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "islamic.DatePatterns", metaValue_buddhist_DatePatterns },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "buddhist.DatePatterns", metaValue_buddhist_DatePatterns },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "#,##0.00\u00a0\u00a4",
                    "#,##0%",
                }
            },
            { "roc.DatePatterns", metaValue_buddhist_DatePatterns },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "calendarname.islamic", "Calend\u00e1rio isl\u00e2mico" },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "java.time.roc.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "java.time.buddhist.DatePatterns", metaValue_java_time_buddhist_DatePatterns },
            { "calendarname.gregorian", metaValue_calendarname_gregorian },
            { "timezone.regionFormat.daylight", metaValue_timezone_regionFormat_daylight },
            { "DatePatterns",
                new String[] {
                    "EEEE, d 'de' MMMM 'de' y",
                    "d 'de' MMMM 'de' y",
                    "dd/MM/y",
                    "dd/MM/yy",
                }
            },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "calendarname.buddhist", "Calend\u00e1rio budista" },
            { "timezone.regionFormat", "Hora de {0}" },
            { "roc.narrow.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "QuarterNames", metaValue_QuarterNames },
            { "QuarterAbbreviations", metaValue_QuarterNames },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "roc.abbreviated.AmPmMarkers", metaValue_narrow_AmPmMarkers },
            { "calendarname.gregory", metaValue_calendarname_gregorian },
        };
        return data;
    }
}
