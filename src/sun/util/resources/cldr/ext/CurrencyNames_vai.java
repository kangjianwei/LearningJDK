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

public class CurrencyNames_vai extends OpenListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final Object[][] data = new Object[][] {
            { "LRD", "$" },
            { "aed", "\ua5b3\ua56f\ua524\ua5f3 \ua549\ua55f\ua52c \ua5e1\ua546\ua513\ua53b \ua535\ua54c\ua546" },
            { "aoa", "\ua549\ua590\ua55e \ua5b4\ua54e\ua60b\ua564" },
            { "aud", "\ua5ba\ua53b\ua5a4\ua503\ua537\ua569 \ua55c\ua55e\ua54c" },
            { "bhd", "\ua551\ua5f8\ua60b" },
            { "bif", "\ua59c\ua5a9\ua53a \ua5a2\ua55f\ua60b\ua543" },
            { "bwp", "\ua577\ua5ac\ua54e\ua56f \ua59b\ua55e" },
            { "cad", "\ua56a\ua56f\ua55c \ua55c\ua55e\ua54c" },
            { "cdf", "\ua58f\ua590\ua571 \ua5a2\ua55f\ua60b\ua543" },
            { "chf", "\ua5ac\ua503\ua564 \ua5a8\ua56e\ua54a \ua5a2\ua55f\ua60b\ua543" },
            { "cny", "\ua566\ua547\ua527 \ua5b3\ua54e\ua60b \ua513\ua546\ua60b\ua52c" },
            { "cve", "\ua5e1\ua53b\ua5b4\ua581 \ua56a\ua577\ua5f2\ua5e1\ua535\ua569\ua586" },
            { "djf", "\ua540\ua59c\ua533 \ua5a2\ua55f\ua60b\ua543" },
            { "dzd", "\ua549\ua537\ua540\ua538\ua569 \ua535\ua56f" },
            { "egp", "\ua546\ua53b\ua55e \ua5c1\ua5bb\ua60b" },
            { "ern", "\ua500\ua538\ua533\ua55f \ua5c1\ua5bb\ua60b" },
            { "etb", "\ua524\ua57f\ua58e\ua52a\ua569 \ua52b\ua524" },
            { "eur", "\ua5b3\ua584" },
            { "gbp", "\ua51b\ua51f\ua53b \ua5c1\ua5bb\ua60b \ua53b\ua5f3\ua537\ua60b" },
            { "ghc", "\ua56d\ua54c\ua56f \ua53b\ua535" },
            { "gmd", "\ua56d\ua52d\ua569 \ua55c\ua55e\ua53b" },
            { "gns", "\ua545\ua524\ua547 \ua5a2\ua55f\ua60b\ua543" },
            { "inr", "\ua524\ua53a\ua569 \ua5a9\ua52a" },
            { "jpy", "\ua567\ua550\ua547\ua527 \ua602\ua60b" },
            { "kes", "\ua51e\ua570 \ua53b\ua51d\ua60b" },
            { "kmf", "\ua58f\ua592\ua584 \ua5a2\ua55f\ua60b\ua543" },
            { "lrd", "\ua55e\ua524\ua52b\ua569 \ua55c\ua55e\ua54c" },
            { "lsl", "\ua537\ua587\ua57f \ua583\ua533" },
            { "lyd", "\ua537\ua52b\ua569 \ua535\ua56f" },
            { "mad", "\ua5de\ua55f\ua58f \ua535\ua54c\ua546" },
            { "mga", "\ua56e\ua55e\ua56d\ua54c\ua53b \ua549\ua538\ua569\ua538" },
            { "mro", "\ua5de\ua538\ua55a\ua547\ua570 \ua5b3\ua545\ua569 (1973\u20132017)" },
            { "mru", "\ua5de\ua538\ua55a\ua547\ua570 \ua5b3\ua545\ua569" },
            { "mur", "\ua5de\ua513\ua5d4 \ua5a9\ua52a" },
            { "mwk", "\ua56e\ua55e\ua54c\ua528 \ua5b4\ua54e\ua566" },
            { "mzm", "\ua5de\ua564\ua52d\ua543 \ua546\ua533\ua56a" },
            { "nad", "\ua56f\ua546\ua52b\ua569 \ua55c\ua55e\ua54c" },
            { "ngn", "\ua56f\ua524\ua540\ua538\ua569 \ua56f\ua524\ua55f" },
            { "rwf", "\ua55f\ua599\ua561 \ua5a2\ua55f\ua60b\ua543" },
            { "sar", "\ua562\ua599\ua535 \ua538\ua569\ua537" },
            { "scr", "\ua516\ua5fc\ua537 \ua5a9\ua52a" },
            { "sdg", "\ua5ac\ua5f5\ua60b \ua5c1\ua5bb\ua60b" },
            { "shp", "\ua53b\ua60b \ua5e5\ua537\ua56f \ua5c1\ua5bb\ua60b" },
            { "sll", "\ua537\ua5da\ua60b" },
            { "sos", "\ua587\ua56e\ua537 \ua53b\ua51d\ua60b" },
            { "std", "\ua562\ua574 \ua57f\ua508 \ua5ea \ua549 \ua557\ua574 \ua581\ua59c\ua55f (1977\u20132017)" },
            { "stn", "\ua562\ua574 \ua57f\ua508 \ua5ea \ua549 \ua557\ua574 \ua581\ua59c\ua55f" },
            { "szl", "\ua537\ua55e\ua51f\ua547" },
            { "tnd", "\ua5a4\ua547\ua53b\ua569 \ua535\ua56f" },
            { "tzs", "\ua55a\ua60b\ua564\ua547\ua570 \ua53b\ua51d\ua60b" },
            { "ugx", "\ua5b3\ua56d\ua561 \ua53b\ua51d\ua60b" },
            { "usd", "\ua576\ua571 \ua55c\ua55e" },
            { "zar", "\ua549\ua531\ua538\ua56a \ua5db\ua524 \ua512\ua60b\ua5e3 \ua5cf \ua55f\ua60b\ua535" },
            { "zmk", "\ua564\ua52d\ua569 \ua5b4\ua54e\ua566 (1968\u20132012)" },
            { "zmw", "\ua564\ua52d\ua569 \ua5b4\ua54e\ua566" },
            { "zwd", "\ua53d\ua553\ua59c\ua503 \ua55c\ua55e" },
        };
        return data;
    }
}
