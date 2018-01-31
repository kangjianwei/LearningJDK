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

public class TimeZoneNames_wo extends TimeZoneNamesBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] Europe_Central = new String[] {
               "CEST (waxtu est\u00e0ndaaru \u00ebroop s\u00e0ntaraal)",
               "",
               "CEST (waxtu ete wu \u00ebroop s\u00e0ntaraal)",
               "",
               "CTE (waxtu \u00ebroop s\u00e0ntaraal)",
               "",
            };
        final String[] America_Eastern = new String[] {
               "EST (waxtu est\u00e0ndaaru penku)",
               "",
               "EDT (waxtu b\u00ebcc\u00ebgu penku)",
               "",
               "ET waxtu penku",
               "",
            };
        final String[] America_Pacific = new String[] {
               "PST (waxtu est\u00e0ndaaru pasifik)",
               "",
               "PDT (waxtu b\u00ebcc\u00ebgu pasifik)",
               "",
               "PT (waxtu pasifik)",
               "",
            };
        final String[] Europe_Western = new String[] {
               "WEST (waxtu est\u00e0ndaaru \u00ebroop u sowwu-jant)",
               "",
               "WEST (waxtu ete wu \u00ebroop u sowwu-jant)",
               "",
               "WET (waxtu \u00ebroop u sowwu-jant",
               "",
            };
        final String[] Europe_Eastern = new String[] {
               "EEST (waxtu est\u00e0ndaaru \u00ebroop u penku)",
               "",
               "EEST (waxtu ete wu \u00ebroop u penku)",
               "",
               "EET (waxtu \u00ebroop u penku)",
               "",
            };
        final String[] Atlantic = new String[] {
               "AST (waxtu est\u00e0ndaaru penku)",
               "",
               "ADT (waxtu b\u00ebcc\u00ebgu atl\u00e0ntik)",
               "",
               "AT (waxtu atl\u00e0ntik)",
               "",
            };
        final String[] GMT = new String[] {
               "GMT (waxtu Greenwich)",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] America_Central = new String[] {
               "CST (waxtu est\u00e0ndaaru s\u00e0ntaraal)",
               "",
               "CDT (waxtu b\u00ebcc\u00ebgu s\u00e0ntaraal",
               "",
               "CT (waxtu s\u00e0ntaral)",
               "",
            };
        final String[] America_Mountain = new String[] {
               "MST (waxtu est\u00e0ndaaru tundu)",
               "",
               "MDT (waxtu b\u00ebcc\u00ebgu tundu)",
               "",
               "MT (waxtu tundu)",
               "",
            };
        final Object[][] data = new Object[][] {
            { "America/Los_Angeles", America_Pacific },
            { "America/Denver", America_Mountain },
            { "America/Phoenix", America_Mountain },
            { "America/Chicago", America_Central },
            { "America/New_York", America_Eastern },
            { "America/Indianapolis", America_Eastern },
            { "America/Halifax", Atlantic },
            { "Europe/Paris", Europe_Central },
            { "GMT", GMT },
            { "Africa/Casablanca", Europe_Western },
            { "Europe/Bucharest", Europe_Eastern },
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
            { "America/Marigot", Atlantic },
            { "America/El_Salvador", America_Central },
            { "America/Kentucky/Monticello", America_Eastern },
            { "Africa/El_Aaiun", Europe_Western },
            { "Africa/Ouagadougou", GMT },
            { "America/Coral_Harbour", America_Eastern },
            { "Africa/Cairo", Europe_Eastern },
            { "America/Aruba", Atlantic },
            { "America/North_Dakota/Center", America_Central },
            { "America/Guatemala", America_Central },
            { "Europe/London", GMT },
            { "America/Cayman", America_Eastern },
            { "America/Belize", America_Central },
            { "America/Panama", America_Eastern },
            { "Europe/San_Marino", Europe_Central },
            { "America/Indiana/Tell_City", America_Central },
            { "America/Tijuana", America_Pacific },
            { "America/Managua", America_Central },
            { "America/Indiana/Petersburg", America_Eastern },
            { "Europe/Brussels", Europe_Central },
            { "America/Ojinaga", America_Mountain },
            { "Europe/Warsaw", Europe_Central },
            { "Europe/Jersey", GMT },
            { "America/Tegucigalpa", America_Central },
            { "Asia/Damascus", Europe_Eastern },
            { "Europe/Luxembourg", Europe_Central },
            { "Atlantic/Reykjavik", GMT },
            { "Europe/Zaporozhye", Europe_Eastern },
            { "timezone.excity.Africa/Dakar", "Dakar" },
            { "Atlantic/St_Helena", GMT },
            { "Europe/Guernsey", GMT },
            { "Atlantic/Madeira", Europe_Western },
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
            { "America/Thunder_Bay", America_Eastern },
            { "America/Grand_Turk", America_Eastern },
            { "Europe/Uzhgorod", Europe_Eastern },
            { "America/Indiana/Marengo", America_Eastern },
            { "America/Creston", America_Mountain },
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
            { "Europe/Isle_of_Man", GMT },
            { "America/Mexico_City", America_Central },
            { "Africa/Tunis", Europe_Central },
            { "Europe/Andorra", Europe_Central },
            { "Africa/Tripoli", Europe_Eastern },
            { "Africa/Banjul", GMT },
            { "America/Matamoros", America_Central },
            { "America/Blanc-Sablon", Atlantic },
            { "Europe/Kaliningrad", Europe_Eastern },
            { "Europe/Lisbon", Europe_Western },
            { "Europe/Oslo", Europe_Central },
            { "Etc/GMT", GMT },
            { "CST6CDT", America_Central },
            { "Atlantic/Canary", Europe_Western },
            { "Africa/Lome", GMT },
            { "America/Menominee", America_Central },
            { "Africa/Freetown", GMT },
            { "Europe/Malta", Europe_Central },
            { "America/Resolute", America_Central },
            { "Europe/Busingen", Europe_Central },
            { "SystemV/EST5", America_Eastern },
            { "America/Edmonton", America_Mountain },
            { "Europe/Podgorica", Europe_Central },
            { "Europe/Skopje", Europe_Central },
            { "America/Santo_Domingo", Atlantic },
            { "Europe/Sarajevo", Europe_Central },
            { "America/Glace_Bay", Atlantic },
            { "Europe/Kiev", Europe_Eastern },
            { "Europe/Rome", Europe_Central },
            { "America/Port-au-Prince", America_Eastern },
            { "Europe/Belfast", GMT },
            { "America/St_Barthelemy", Atlantic },
            { "America/Nipigon", America_Eastern },
            { "America/Regina", America_Central },
            { "Atlantic/Jan_Mayen", Europe_Central },
            { "America/Dawson_Creek", America_Mountain },
            { "Africa/Algiers", Europe_Central },
            { "Europe/Mariehamn", Europe_Eastern },
            { "America/St_Thomas", Atlantic },
            { "Europe/Zurich", Europe_Central },
            { "America/Anguilla", Atlantic },
            { "Europe/Vilnius", Europe_Eastern },
            { "Africa/Bamako", GMT },
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
            { "America/Cancun", America_Eastern },
            { "Europe/Gibraltar", Europe_Central },
            { "Africa/Conakry", GMT },
            { "America/St_Lucia", Atlantic },
            { "Europe/Madrid", Europe_Central },
            { "America/Bahia_Banderas", America_Central },
            { "America/Montserrat", Atlantic },
            { "America/Cambridge_Bay", America_Mountain },
            { "Europe/Vaduz", Europe_Central },
            { "America/Barbados", Atlantic },
            { "America/Louisville", America_Eastern },
            { "America/Lower_Princes", Atlantic },
            { "America/Vancouver", America_Pacific },
            { "America/Danmarkshavn", GMT },
            { "America/Detroit", America_Eastern },
            { "Europe/Ljubljana", Europe_Central },
            { "America/Thule", Atlantic },
            { "America/Curacao", Atlantic },
            { "America/Martinique", Atlantic },
            { "Europe/Berlin", Europe_Central },
            { "Europe/Chisinau", Europe_Eastern },
            { "America/Puerto_Rico", Atlantic },
            { "America/Rankin_Inlet", America_Central },
            { "Africa/Dakar", GMT },
            { "Europe/Stockholm", Europe_Central },
            { "SystemV/CST6CDT", America_Central },
            { "America/Tortola", Atlantic },
            { "Europe/Budapest", Europe_Central },
            { "Europe/Zagreb", Europe_Central },
            { "America/Port_of_Spain", Atlantic },
            { "Europe/Helsinki", Europe_Eastern },
            { "Asia/Beirut", Europe_Eastern },
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
            { "Europe/Tallinn", Europe_Eastern },
            { "America/Rainy_River", America_Central },
            { "Europe/Belgrade", Europe_Central },
            { "Africa/Bissau", GMT },
            { "SystemV/AST4", Atlantic },
            { "America/Yellowknife", America_Mountain },
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
            { "Africa/Ceuta", Europe_Central },
            { "Africa/Timbuktu", GMT },
            { "America/Goose_Bay", Atlantic },
            { "America/Swift_Current", America_Central },
            { "America/Pangnirtung", America_Eastern },
            { "EST", America_Eastern },
            { "Europe/Sofia", Europe_Eastern },
            { "MST", America_Mountain },
            { "Africa/Nouakchott", GMT },
            { "Europe/Prague", Europe_Central },
            { "America/Indiana/Vincennes", America_Eastern },
            { "America/Whitehorse", America_Pacific },
            { "America/Kralendijk", Atlantic },
            { "Antarctica/Troll", GMT },
            { "America/Antigua", Atlantic },
            { "America/Montreal", America_Eastern },
            { "America/Inuvik", America_Mountain },
            { "America/Iqaluit", America_Eastern },
            { "Asia/Nicosia", Europe_Eastern },
            { "America/Moncton", Atlantic },
            { "America/Indiana/Winamac", America_Eastern },
            { "SystemV/MST7MDT", America_Mountain },
            { "America/St_Vincent", Atlantic },
            { "Asia/Gaza", Europe_Eastern },
            { "PST8PDT", America_Pacific },
            { "timezone.excity.Etc/Unknown", "D\u00ebkk bu\u00f1 xamul" },
            { "America/Grenada", Atlantic },
            { "Atlantic/Faeroe", Europe_Western },
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
            { "Europe/Bratislava", Europe_Central },
            { "Europe/Copenhagen", Europe_Central },
            { "Europe/Vienna", Europe_Central },
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
            { "America/Merida", America_Central },
            { "Europe/Tirane", Europe_Central },
            { "SystemV/MST7", America_Mountain },
            { "America/St_Kitts", Atlantic },
            { "Arctic/Longyearbyen", Europe_Central },
            { "America/Fort_Nelson", America_Mountain },
            { "Europe/Riga", Europe_Eastern },
            { "America/Dominica", Atlantic },
            { "America/Guadeloupe", Atlantic },
            { "Asia/Hebron", Europe_Eastern },
            { "SystemV/PST8PDT", America_Pacific },
            { "Africa/Abidjan", GMT },
            { "Africa/Monrovia", GMT },
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
            { "Atlantic/Bermuda", Atlantic },
            { "America/Costa_Rica", America_Central },
            { "America/Dawson", America_Pacific },
            { "America/Shiprock", America_Mountain },
            { "America/Winnipeg", America_Central },
            { "Europe/Amsterdam", Europe_Central },
            { "America/Indiana/Knox", America_Central },
            { "America/North_Dakota/Beulah", America_Central },
            { "Europe/Vatican", Europe_Central },
            { "Africa/Accra", GMT },
            { "Asia/Amman", Europe_Eastern },
            { "Etc/UTC",
                new String[] {
                    "CUT (waxtu iniwelsel yu\u00f1 boole)",
                    "UTC",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "SystemV/AST4ADT", Atlantic },
            { "Europe/Dublin", GMT },
            { "America/Toronto", America_Eastern },
            { "MST7MDT", America_Mountain },
            { "America/Monterrey", America_Central },
            { "SystemV/EST5EDT", America_Eastern },
            { "America/Nassau", America_Eastern },
            { "America/Jamaica", America_Eastern },
            { "Europe/Athens", Europe_Eastern },
            { "Europe/Monaco", Europe_Central },
            { "SystemV/CST6", America_Central },
        };
        return data;
    }
}
