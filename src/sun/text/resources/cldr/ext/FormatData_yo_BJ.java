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

public class FormatData_yo_BJ extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "Osh\u00f9 Sh\u025b\u0301r\u025b\u0301",
               "Osh\u00f9 \u00c8r\u00e8l\u00e8",
               "Osh\u00f9 \u0190r\u025b\u0300n\u00e0",
               "Osh\u00f9 \u00ccgb\u00e9",
               "Osh\u00f9 \u0190\u0300bibi",
               "Osh\u00f9 \u00d2k\u00fadu",
               "Osh\u00f9 Ag\u025bm\u0254",
               "Osh\u00f9 \u00d2g\u00fan",
               "Osh\u00f9 Owewe",
               "Osh\u00f9 \u0186\u0300w\u00e0r\u00e0",
               "Osh\u00f9 B\u00e9l\u00fa",
               "Osh\u00f9 \u0186\u0300p\u025b\u0300",
               "",
            };
        final String[] metaValue_MonthAbbreviations = new String[] {
               "Sh\u025b\u0301r\u025b\u0301",
               "\u00c8r\u00e8l\u00e8",
               "\u0190r\u025b\u0300n\u00e0",
               "\u00ccgb\u00e9",
               "\u0190\u0300bibi",
               "\u00d2k\u00fadu",
               "Ag\u025bm\u0254",
               "\u00d2g\u00fan",
               "Owewe",
               "\u0186\u0300w\u00e0r\u00e0",
               "B\u00e9l\u00fa",
               "\u0186\u0300p\u025b\u0300",
               "",
            };
        final String[] metaValue_DayNames = new String[] {
               "\u0186j\u0254\u0301 \u00c0\u00eck\u00fa",
               "\u0186j\u0254\u0301 Aj\u00e9",
               "\u0186j\u0254\u0301 \u00ccs\u025b\u0301gun",
               "\u0186j\u0254\u0301r\u00fa",
               "\u0186j\u0254\u0301b\u0254",
               "\u0186j\u0254\u0301 \u0190t\u00ec",
               "\u0186j\u0254\u0301 \u00c0b\u00e1m\u025b\u0301ta",
            };
        final String[] metaValue_DayAbbreviations = new String[] {
               "\u00c0\u00eck\u00fa",
               "Aj\u00e9",
               "\u00ccs\u025b\u0301gun",
               "\u0186j\u0254\u0301r\u00fa",
               "\u0186j\u0254\u0301b\u0254",
               "\u0190t\u00ec",
               "\u00c0b\u00e1m\u025b\u0301ta",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "K\u0254\u0301t\u00e0 K\u00ednn\u00ed",
               "K\u0254\u0301t\u00e0 Kej\u00ec",
               "K\u0254\u0301\u00e0 Keta",
               "K\u0254\u0301t\u00e0 K\u025brin",
            };
        final String[] metaValue_AmPmMarkers = new String[] {
               "\u00c0\u00e1r\u0254\u0300",
               "\u0186\u0300s\u00e1n",
            };
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u0186d\u00fan" },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.AmPmMarkers", metaValue_AmPmMarkers },
            { "islamic.AmPmMarkers", metaValue_AmPmMarkers },
            { "AmPmMarkers", metaValue_AmPmMarkers },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "islamic.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "roc.DayNames", metaValue_DayNames },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "roc.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "islamic.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
            { "islamic.DayNames", metaValue_DayNames },
            { "buddhist.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "field.weekday", "\u0186j\u0254\u0301 \u0186\u0300s\u025b\u0300" },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "japanese.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "buddhist.DayNames", metaValue_DayNames },
            { "field.minute", "\u00ccs\u025b\u0301j\u00fa" },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "buddhist.AmPmMarkers", metaValue_AmPmMarkers },
            { "field.dayperiod", "\u00c0\u00e1r\u0254\u0300/\u0254\u0300s\u00e1n" },
            { "japanese.QuarterNames", metaValue_QuarterNames },
            { "japanese.DayNames", metaValue_DayNames },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayNames", metaValue_DayNames },
            { "field.second", "\u00ccs\u025b\u0301j\u00fa \u00c0\u00e0y\u00e1" },
            { "roc.MonthNames", metaValue_MonthNames },
            { "field.week", "\u0186\u0300s\u00e8" },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "roc.AmPmMarkers", metaValue_AmPmMarkers },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "MonthAbbreviations", metaValue_MonthAbbreviations },
            { "standalone.DayNames", metaValue_DayNames },
            { "japanese.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "standalone.MonthAbbreviations", metaValue_MonthAbbreviations },
            { "roc.narrow.AmPmMarkers", metaValue_AmPmMarkers },
            { "QuarterNames", metaValue_QuarterNames },
            { "QuarterAbbreviations", metaValue_QuarterNames },
            { "standalone.QuarterNames", metaValue_QuarterNames },
            { "roc.abbreviated.AmPmMarkers", metaValue_AmPmMarkers },
        };
        return data;
    }
}
