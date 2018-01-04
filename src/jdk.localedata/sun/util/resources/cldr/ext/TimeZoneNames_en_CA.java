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

public class TimeZoneNames_en_CA extends TimeZoneNamesBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] Argentina = new String[] {
               "Argentina Standard Time",
               "ART",
               "Argentina Summer Time",
               "ARST",
               "Argentina Time",
               "ART",
            };
        final String[] America_Eastern = new String[] {
               "",
               "EST",
               "",
               "EDT",
               "",
               "ET",
            };
        final String[] Hawaii_Aleutian = new String[] {
               "",
               "HAST",
               "",
               "HADT",
               "",
               "HAT",
            };
        final String[] America_Central = new String[] {
               "",
               "CST",
               "",
               "CDT",
               "",
               "CT",
            };
        final String[] America_Mountain = new String[] {
               "",
               "MST",
               "",
               "MDT",
               "",
               "MT",
            };
        final String[] America_Pacific = new String[] {
               "",
               "PST",
               "",
               "PDT",
               "",
               "PT",
            };
        final String[] Alaska = new String[] {
               "",
               "AKST",
               "",
               "AKDT",
               "",
               "AKT",
            };
        final String[] Newfoundland = new String[] {
               "Newfoundland Standard Time",
               "NST",
               "Newfoundland Daylight Time",
               "NDT",
               "Newfoundland Time",
               "NT",
            };
        final String[] Atlantic = new String[] {
               "",
               "AST",
               "",
               "ADT",
               "",
               "AT",
            };
        final Object[][] data = new Object[][] {
            { "America/Los_Angeles", America_Pacific },
            { "America/Denver", America_Mountain },
            { "America/Phoenix", America_Mountain },
            { "America/Chicago", America_Central },
            { "America/New_York", America_Eastern },
            { "America/Indianapolis", America_Eastern },
            { "Pacific/Honolulu",
                new String[] {
                    "",
                    "HST",
                    "",
                    "HDT",
                    "",
                    "HST",
                }
            },
            { "America/Anchorage", Alaska },
            { "America/Halifax", Atlantic },
            { "America/Sitka", Alaska },
            { "America/St_Johns", Newfoundland },
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
            { "America/Thule", Atlantic },
            { "America/Curacao", Atlantic },
            { "America/Marigot", Atlantic },
            { "America/Martinique", Atlantic },
            { "America/El_Salvador", America_Central },
            { "America/Kentucky/Monticello", America_Eastern },
            { "America/Coral_Harbour", America_Eastern },
            { "America/Aruba", Atlantic },
            { "America/North_Dakota/Center", America_Central },
            { "America/Guatemala", America_Central },
            { "America/Argentina/La_Rioja", Argentina },
            { "America/Puerto_Rico", Atlantic },
            { "America/Rankin_Inlet", America_Central },
            { "America/Cayman", America_Eastern },
            { "America/Belize", America_Central },
            { "America/Panama", America_Eastern },
            { "SystemV/CST6CDT", America_Central },
            { "America/Tortola", Atlantic },
            { "America/Indiana/Tell_City", America_Central },
            { "America/Tijuana", America_Pacific },
            { "America/Port_of_Spain", Atlantic },
            { "America/Managua", America_Central },
            { "America/Indiana/Petersburg", America_Eastern },
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
            { "America/Nome", Alaska },
            { "America/Ojinaga", America_Mountain },
            { "America/Tegucigalpa", America_Central },
            { "America/Rainy_River", America_Central },
            { "SystemV/AST4", Atlantic },
            { "America/Argentina/Ushuaia", Argentina },
            { "America/Yellowknife", America_Mountain },
            { "America/Juneau", Alaska },
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
            { "America/Indiana/Vevay", America_Eastern },
            { "America/Jujuy", Argentina },
            { "America/Buenos_Aires", Argentina },
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
            { "America/Goose_Bay", Atlantic },
            { "America/Thunder_Bay", America_Eastern },
            { "America/Swift_Current", America_Central },
            { "America/Grand_Turk", America_Eastern },
            { "America/Metlakatla", Alaska },
            { "America/Pangnirtung", America_Eastern },
            { "EST", America_Eastern },
            { "America/Indiana/Marengo", America_Eastern },
            { "America/Creston", America_Mountain },
            { "MST", America_Mountain },
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
            { "America/Indiana/Vincennes", America_Eastern },
            { "America/Whitehorse", America_Pacific },
            { "America/Kralendijk", Atlantic },
            { "America/Mexico_City", America_Central },
            { "America/Argentina/Salta", Argentina },
            { "America/Antigua", Atlantic },
            { "America/Montreal", America_Eastern },
            { "America/Argentina/San_Juan", Argentina },
            { "America/Inuvik", America_Mountain },
            { "America/Iqaluit", America_Eastern },
            { "America/Matamoros", America_Central },
            { "America/Blanc-Sablon", Atlantic },
            { "America/Moncton", Atlantic },
            { "America/Indiana/Winamac", America_Eastern },
            { "SystemV/MST7MDT", America_Mountain },
            { "America/St_Vincent", Atlantic },
            { "PST8PDT", America_Pacific },
            { "America/Grenada", Atlantic },
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
            { "CST6CDT", America_Central },
            { "SystemV/HST10",
                new String[] {
                    "",
                    "HST",
                    "",
                    "HDT",
                    "",
                    "HST",
                }
            },
            { "America/Menominee", America_Central },
            { "America/Yakutat", Alaska },
            { "America/Adak", Hawaii_Aleutian },
            { "America/Argentina/Tucuman", Argentina },
            { "timezone.excity.Asia/Rangoon", "Rangoon" },
            { "HST",
                new String[] {
                    "",
                    "HST",
                    "",
                    "HDT",
                    "",
                    "HST",
                }
            },
            { "America/Resolute", America_Central },
            { "SystemV/YST9YDT", Alaska },
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
            { "America/Argentina/Rio_Gallegos", Argentina },
            { "SystemV/EST5", America_Eastern },
            { "America/Merida", America_Central },
            { "America/Edmonton", America_Mountain },
            { "America/Catamarca", Argentina },
            { "America/Santo_Domingo", Atlantic },
            { "SystemV/MST7", America_Mountain },
            { "America/St_Kitts", Atlantic },
            { "America/Fort_Nelson", America_Mountain },
            { "America/Glace_Bay", Atlantic },
            { "America/Dominica", Atlantic },
            { "America/Guadeloupe", Atlantic },
            { "America/Cordoba", Argentina },
            { "SystemV/PST8PDT", America_Pacific },
            { "America/Port-au-Prince", America_Eastern },
            { "America/Mendoza", Argentina },
            { "America/St_Barthelemy", Atlantic },
            { "America/Nipigon", America_Eastern },
            { "America/Regina", America_Central },
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
            { "America/Boise", America_Mountain },
            { "America/North_Dakota/New_Salem", America_Central },
            { "EST5EDT", America_Eastern },
            { "America/Dawson_Creek", America_Mountain },
            { "America/St_Thomas", Atlantic },
            { "Atlantic/Bermuda", Atlantic },
            { "America/Anguilla", Atlantic },
            { "America/Costa_Rica", America_Central },
            { "America/Dawson", America_Pacific },
            { "America/Shiprock", America_Mountain },
            { "America/Winnipeg", America_Central },
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
            { "America/Indiana/Knox", America_Central },
            { "America/Cancun", America_Eastern },
            { "America/North_Dakota/Beulah", America_Central },
            { "SystemV/AST4ADT", Atlantic },
            { "America/St_Lucia", Atlantic },
            { "America/Bahia_Banderas", America_Central },
            { "America/Montserrat", Atlantic },
            { "America/Cambridge_Bay", America_Mountain },
            { "America/Toronto", America_Eastern },
            { "MST7MDT", America_Mountain },
            { "America/Barbados", Atlantic },
            { "America/Monterrey", America_Central },
            { "SystemV/EST5EDT", America_Eastern },
            { "America/Nassau", America_Eastern },
            { "America/Jamaica", America_Eastern },
            { "America/Louisville", America_Eastern },
            { "America/Lower_Princes", Atlantic },
            { "America/Vancouver", America_Pacific },
            { "America/Detroit", America_Eastern },
            { "Pacific/Johnston", Hawaii_Aleutian },
            { "SystemV/CST6", America_Central },
        };
        return data;
    }
}
