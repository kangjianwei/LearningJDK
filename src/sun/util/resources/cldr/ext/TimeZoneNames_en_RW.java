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

import sun.util.resources.TimeZoneNamesBundle;

public class TimeZoneNames_en_RW extends TimeZoneNamesBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] Africa_Central = new String[] {
               "Central Africa Time",
               "CAT",
               "Central African Summer Time",
               "CAST",
               "Central Africa Time",
               "CAT",
            };
        final String[] Africa_Eastern = new String[] {
               "East Africa Time",
               "EAT",
               "Eastern African Summer Time",
               "EAST",
               "Eastern Africa Time",
               "EAT",
            };
        final String[] Africa_Southern = new String[] {
               "South Africa Standard Time",
               "SAST",
               "South Africa Summer Time",
               "SAST",
               "South Africa Time",
               "SAT",
            };
        final String[] Africa_Western = new String[] {
               "West Africa Standard Time",
               "WAT",
               "West Africa Summer Time",
               "WAST",
               "West Africa Time",
               "WAT",
            };
        final Object[][] data = new Object[][] {
            { "UTC",
                new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "Africa/Nairobi", Africa_Eastern },
            { "Africa/Libreville", Africa_Western },
            { "Africa/Lusaka", Africa_Central },
            { "Africa/Gaborone", Africa_Central },
            { "Africa/Maputo", Africa_Central },
            { "Africa/Windhoek", Africa_Central },
            { "Africa/Mbabane", Africa_Southern },
            { "Africa/Mogadishu", Africa_Eastern },
            { "Asia/Famagusta",
                new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "Africa/Harare", Africa_Central },
            { "Africa/Douala", Africa_Western },
            { "Pacific/Bougainville",
                new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "Africa/Asmera", Africa_Eastern },
            { "Europe/Ulyanovsk",
                new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "Africa/Kampala", Africa_Eastern },
            { "Africa/Sao_Tome", Africa_Western },
            { "Africa/Khartoum", Africa_Central },
            { "Africa/Malabo", Africa_Western },
            { "Africa/Johannesburg", Africa_Southern },
            { "Africa/Ndjamena", Africa_Western },
            { "Africa/Bujumbura", Africa_Central },
            { "Africa/Bangui", Africa_Western },
            { "Africa/Lagos", Africa_Western },
            { "Africa/Brazzaville", Africa_Western },
            { "Africa/Luanda", Africa_Western },
            { "Africa/Porto-Novo", Africa_Western },
            { "Europe/Astrakhan",
                new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "Africa/Juba", Africa_Eastern },
            { "Asia/Srednekolymsk",
                new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "Europe/Saratov",
                new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "Africa/Dar_es_Salaam", Africa_Eastern },
            { "Antarctica/Palmer",
                new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "Africa/Maseru", Africa_Southern },
            { "Africa/Kinshasa", Africa_Western },
            { "Africa/Addis_Ababa", Africa_Eastern },
            { "Africa/Lubumbashi", Africa_Central },
            { "Africa/Djibouti", Africa_Eastern },
            { "Africa/Kigali", Africa_Central },
            { "Indian/Antananarivo", Africa_Eastern },
            { "America/Punta_Arenas",
                new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "Africa/Niamey", Africa_Western },
            { "Indian/Mayotte", Africa_Eastern },
            { "Africa/Blantyre", Africa_Central },
            { "Indian/Comoro", Africa_Eastern },
        };
        return data;
    }
}
