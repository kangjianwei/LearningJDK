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

package sun.util.resources.cldr.ext;

import sun.util.resources.OpenListResourceBundle;

public class CurrencyNames_ji extends OpenListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final Object[][] data = new Object[][] {
            { "RUB", "RUB" },
            { "brl", "\u05d1\u05e8\u05d0\u05d6\u05d9\u05dc \u05e8\u05e2\u05d0\u05dc" },
            { "bzd", "\u05d1\u05e2\u05dc\u05d9\u05d6 \u05d3\u05d0\u05dc\u05d0\u05b7\u05e8" },
            { "chf", "\u05e9\u05d5\u05d5\u05d9\u05d9\u05e6\u05e2\u05e8 \u05e4\u05bf\u05e8\u05d0\u05b7\u05e0\u05e7" },
            { "cny", "\u05db\u05d9\u05e0\u05e2\u05d6\u05d9\u05e9\u05e2\u05e8 \u05d9\u05d5\u05d0\u05df" },
            { "eur", "\u05d0\u05d9\u05d9\u05e8\u05d0" },
            { "gbp", "\u05e4\u05bf\u05d5\u05e0\u05d8 \u05e9\u05d8\u05e2\u05e8\u05dc\u05d9\u05e0\u05d2" },
            { "inr", "\u05d0\u05d9\u05e0\u05d3\u05d9\u05e9\u05e2\u05e8 \u05e8\u05d5\u05e4\u05d9" },
            { "jpy", "\u05d9\u05d0\u05e4\u05d0\u05e0\u05e2\u05d6\u05d9\u05e9\u05e2\u05e8 \u05d9\u05e2\u05df" },
            { "rub", "\u05e8\u05d5\u05e1\u05d9\u05e9\u05e2\u05e8 \u05e8\u05d5\u05d1\u05dc" },
            { "sek", "\u05e9\u05d5\u05d5\u05e2\u05d3\u05d9\u05e9\u05e2 \u05e7\u05e8\u05d0\u05e0\u05e2" },
            { "usd", "\u05d0\u05de\u05e2\u05e8\u05d9\u05e7\u05d0\u05e0\u05e2\u05e8 \u05d3\u05d0\u05dc\u05d0\u05e8" },
            { "xag", "\u05d6\u05d9\u05dc\u05d1\u05e2\u05e8" },
            { "xau", "\u05d2\u05d0\u05dc\u05d3" },
            { "xxx", "\u05d0\u05d5\u05de\u05d1\u05d0\u05b7\u05d5\u05d5\u05d0\u05d5\u05e1\u05d8\u05e2 \u05d5\u05d5\u05d0\u05b7\u05dc\u05d5\u05d8\u05e2" },
        };
        return data;
    }
}
