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

public class FormatData_mzn extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_MonthNames = new String[] {
               "\u0698\u0627\u0646\u0648\u06cc\u0647",
               "\u0641\u0648\u0631\u06cc\u0647",
               "\u0645\u0627\u0631\u0633",
               "\u0622\u0648\u0631\u06cc\u0644",
               "\u0645\u0647",
               "\u0698\u0648\u0626\u0646",
               "\u0698\u0648\u0626\u06cc\u0647",
               "\u0627\u0648\u062a",
               "\u0633\u067e\u062a\u0627\u0645\u0628\u0631",
               "\u0627\u06a9\u062a\u0628\u0631",
               "\u0646\u0648\u0627\u0645\u0628\u0631",
               "\u062f\u0633\u0627\u0645\u0628\u0631",
               "",
            };
        final String[] metaValue_Eras = new String[] {
               "\u067e.\u0645",
               "\u0645.",
            };
        final Object[][] data = new Object[][] {
            { "MonthNames", metaValue_MonthNames },
            { "field.year", "\u0633\u0627\u0644" },
            { "japanese.MonthNames", metaValue_MonthNames },
            { "roc.MonthAbbreviations", metaValue_MonthNames },
            { "long.Eras",
                new String[] {
                    "\u0642\u0628\u0644 \u0645\u06cc\u0644\u0627\u062f",
                    "\u0628\u0639\u062f \u0645\u06cc\u0644\u0627\u062f",
                }
            },
            { "buddhist.MonthAbbreviations", metaValue_MonthNames },
            { "field.weekday", "\u0647\u0641\u062a\u0647\u200c\u06cc \u0650\u0631\u0648\u0632" },
            { "MonthAbbreviations", metaValue_MonthNames },
            { "buddhist.MonthNames", metaValue_MonthNames },
            { "field.hour", "\u0633\u0627\u0639\u0650\u062a" },
            { "field.zone", "\u0632\u0645\u0648\u0646\u06cc \u0645\u0646\u0642\u0637\u0647" },
            { "field.month", "\u0645\u0627\u0647" },
            { "japanese.MonthAbbreviations", metaValue_MonthNames },
            { "field.minute", "\u062f\u0642\u06cc\u0642\u0647" },
            { "field.second", "\u062b\u0627\u0646\u06cc\u0647" },
            { "roc.MonthNames", metaValue_MonthNames },
            { "narrow.Eras", metaValue_Eras },
            { "field.era", "\u062a\u0642\u0648\u06cc\u0645" },
            { "field.week", "\u0647\u0641\u062a\u0647" },
            { "standalone.MonthNames", metaValue_MonthNames },
            { "standalone.MonthAbbreviations", metaValue_MonthNames },
            { "DefaultNumberingSystem", "arabext" },
            { "field.dayperiod", "\u0635\u0648\u0627\u062d\u06cc/\u0638\u064f\u0631" },
            { "Eras", metaValue_Eras },
        };
        return data;
    }
}
