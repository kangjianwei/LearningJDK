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

public class FormatData_ro_MD extends ListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] metaValue_DayAbbreviations = new String[] {
               "Dum",
               "Lun",
               "Mar",
               "Mie",
               "Joi",
               "Vin",
               "S\u00e2m",
            };
        final String[] metaValue_DayNarrows = new String[] {
               "D",
               "L",
               "Ma",
               "Mi",
               "J",
               "V",
               "S",
            };
        final String[] metaValue_QuarterNames = new String[] {
               "trimestrul 1",
               "trimestrul 2",
               "trimestrul 3",
               "trimestrul 4",
            };
        final String[] metaValue_QuarterAbbreviations = new String[] {
               "trim. 1",
               "trim. 2",
               "trim. 3",
               "trim. 4",
            };
        final Object[][] data = new Object[][] {
            { "buddhist.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DayNarrows", metaValue_DayNarrows },
            { "islamic.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "roc.DayAbbreviations", metaValue_DayAbbreviations },
            { "standalone.DayNarrows", metaValue_DayNarrows },
            { "standalone.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.QuarterNames", metaValue_QuarterNames },
            { "standalone.QuarterAbbreviations",
                new String[] {
                    "Trim. 1",
                    "Trim. 2",
                    "Trim. 3",
                    "Trim. 4",
                }
            },
            { "roc.QuarterNames", metaValue_QuarterNames },
            { "buddhist.DayAbbreviations", metaValue_DayAbbreviations },
            { "islamic.DayNarrows", metaValue_DayNarrows },
            { "roc.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "japanese.DayAbbreviations", metaValue_DayAbbreviations },
            { "japanese.QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "islamic.DayAbbreviations", metaValue_DayAbbreviations },
            { "DayAbbreviations", metaValue_DayAbbreviations },
            { "buddhist.DayNarrows", metaValue_DayNarrows },
            { "DayNarrows", metaValue_DayNarrows },
            { "japanese.DayNarrows", metaValue_DayNarrows },
            { "QuarterNames", metaValue_QuarterNames },
            { "QuarterAbbreviations", metaValue_QuarterAbbreviations },
            { "buddhist.QuarterNames", metaValue_QuarterNames },
            { "standalone.QuarterNames",
                new String[] {
                    "Trimestrul 1",
                    "Trimestrul 2",
                    "Trimestrul 3",
                    "Trimestrul 4",
                }
            },
            { "japanese.QuarterNames", metaValue_QuarterNames },
        };
        return data;
    }
}
