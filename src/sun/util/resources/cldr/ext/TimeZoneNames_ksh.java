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

public class TimeZoneNames_ksh extends TimeZoneNamesBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] Africa_Central = new String[] {
               "Zentraal-Affrekaanesche Zigg",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Europe_Central = new String[] {
               "Meddel-Europpa sing jew\u00f6hnlijje Zick",
               "MEZ",
               "Meddel-Europpa sing Summerzick",
               "MESZ",
               "Meddel-Europpa sing Zick",
               "MEZ",
            };
        final String[] Indian_Ocean = new String[] {
               "dem Indische Ozejan sing Zick",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Mauritius = new String[] {
               "Jew\u00f6hnlijje Zigg vun Mauritius",
               "",
               "Summerzigg vun Mauritius",
               "",
               "Zigg vun Mauritius",
               "",
            };
        final String[] Africa_Eastern = new String[] {
               "O\u00df-Affrekaanesche Zigg",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Europe_Western = new String[] {
               "We\u00df-Europpa sing jew\u00f6hnlijje Zick",
               "WEZ",
               "We\u00df-Europpa sing Summerzick",
               "WESZ",
               "We\u00df-Europpa sing Zick",
               "WEZ",
            };
        final String[] Cape_Verde = new String[] {
               "Jew\u00f6hnlijje Kapv\u00e4rdejaansche Zigg",
               "",
               "Kapv\u00e4rdejaansche Sommerzigg",
               "",
               "Kapv\u00e4rdejaansche Zigg",
               "",
            };
        final String[] Europe_Eastern = new String[] {
               "O\u00df-Europpa sing jew\u00f6hnlijje Zick",
               "OEZ",
               "O\u00df-Europpa sing Summerzick",
               "OESZ",
               "O\u00df-Europpa sing Zick",
               "OEZ",
            };
        final String[] Africa_Western = new String[] {
               "Jew\u00f6hnlijje W\u00e4\u00df-Affrekaanesche Zigg",
               "",
               "W\u00e4\u00df-Affrekaanesche Sommerzigg",
               "",
               "W\u00e4\u00df-Affrekaanesche Zigg",
               "",
            };
        final String[] GMT = new String[] {
               "Greenwich sing Standat-Zick",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Africa_Southern = new String[] {
               "S\u00f6d-Affrekaanesche Zigg",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Seychelles = new String[] {
               "Zigg vun de Seisch\u00e4lle",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Azores = new String[] {
               "de Azore ier jew\u00f6hnlijje Zick",
               "",
               "de Azore ier Summerzick",
               "",
               "de Azore ier Zick",
               "",
            };
        final String[] Reunion = new String[] {
               "Zigg vun Reunion",
               "",
               "",
               "",
               "",
               "",
            };
        final Object[][] data = new Object[][] {
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
            { "Indian/Mahe", Seychelles },
            { "timezone.excity.Asia/Gaza", "Jaasa" },
            { "timezone.excity.Asia/Aqtobe", "Aqt\u00f6be" },
            { "Africa/Nairobi", Africa_Eastern },
            { "Africa/Libreville", Africa_Western },
            { "timezone.excity.Atlantic/St_Helena", "Zint Helena" },
            { "Africa/Maputo", Africa_Central },
            { "Africa/El_Aaiun", Europe_Western },
            { "Africa/Ouagadougou", GMT },
            { "Africa/Cairo", Europe_Eastern },
            { "Africa/Mbabane", Africa_Southern },
            { "timezone.excity.Europe/Luxembourg", "Luxembursch" },
            { "Europe/London",
                new String[] {
                    "Greenwich sing Standat-Zick",
                    "",
                    "Jru\u00dfbretannije sing Summerzick",
                    "",
                    "",
                    "",
                }
            },
            { "timezone.excity.Antarctica/Vostok", "Wostok" },
            { "timezone.excity.Asia/Hong_Kong", "Hongkong" },
            { "Europe/San_Marino", Europe_Central },
            { "timezone.excity.America/Indiana/Winamac", "Winamac en Indiana" },
            { "timezone.excity.America/Mexico_City", "Schtadt Mexiko" },
            { "timezone.excity.Europe/Isle_of_Man", "Ensel M\u00e4n" },
            { "timezone.excity.America/North_Dakota/Beulah", "Beulah en Nood Dakota" },
            { "Europe/Brussels", Europe_Central },
            { "Africa/Douala", Africa_Western },
            { "timezone.excity.Asia/Kamchatka", "Kamschattka" },
            { "timezone.excity.Europe/Gibraltar", "Jibraltaa" },
            { "timezone.excity.Europe/Lisbon", "Lissabon" },
            { "Europe/Warsaw", Europe_Central },
            { "timezone.excity.Asia/Yakutsk", "Jakutsk" },
            { "Europe/Jersey", GMT },
            { "Asia/Damascus", Europe_Eastern },
            { "Europe/Luxembourg", Europe_Central },
            { "timezone.excity.Europe/Zaporozhye", "Saporischschja" },
            { "timezone.excity.Asia/Tashkent", "Taschkent" },
            { "timezone.excity.Europe/Zagreb", "Sagreb" },
            { "Atlantic/Reykjavik", GMT },
            { "timezone.excity.America/Indiana/Vevay", "Vevay en Indiana" },
            { "Africa/Brazzaville", Africa_Western },
            { "Europe/Zaporozhye", Europe_Eastern },
            { "Africa/Porto-Novo", Africa_Western },
            { "Atlantic/St_Helena", GMT },
            { "timezone.excity.Asia/Shanghai", "Schanghai" },
            { "Africa/Dar_es_Salaam", Africa_Eastern },
            { "timezone.excity.Asia/Dushanbe", "Duschanbe" },
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
            { "Africa/Addis_Ababa", Africa_Eastern },
            { "Europe/Uzhgorod", Europe_Eastern },
            { "Africa/Kigali", Africa_Central },
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
            { "timezone.excity.America/Yakutat", "Jakutat" },
            { "timezone.excity.Pacific/Guam", "Juam" },
            { "Africa/Tunis", Europe_Central },
            { "Europe/Andorra", Europe_Central },
            { "Africa/Tripoli", Europe_Eastern },
            { "timezone.excity.Asia/Urumqi", "Urrumptschi" },
            { "Africa/Banjul", GMT },
            { "Indian/Comoro", Africa_Eastern },
            { "timezone.excity.America/Costa_Rica", "Ko\u00dftaricka" },
            { "timezone.excity.Africa/Lagos", "Laajos" },
            { "Indian/Reunion", Reunion },
            { "timezone.excity.Europe/Guernsey", "J\u00f6\u00f6nsei" },
            { "Europe/Kaliningrad", Europe_Eastern },
            { "timezone.excity.Europe/Riga", "Riija" },
            { "Africa/Windhoek", Africa_Central },
            { "Europe/Lisbon", Europe_Western },
            { "timezone.excity.Atlantic/Canary", "Kannaare" },
            { "Europe/Oslo", Europe_Central },
            { "Africa/Mogadishu", Africa_Eastern },
            { "timezone.excity.Pacific/Pitcairn", "Pitkern" },
            { "Etc/GMT", GMT },
            { "Atlantic/Canary", Europe_Western },
            { "timezone.excity.Asia/Krasnoyarsk", "Krasnojarsk" },
            { "Africa/Lome", GMT },
            { "Africa/Freetown", GMT },
            { "Europe/Malta", Europe_Central },
            { "timezone.excity.Asia/Yerevan", "Eriwan" },
            { "Africa/Asmera", Africa_Eastern },
            { "Africa/Kampala", Africa_Eastern },
            { "Europe/Busingen", Europe_Central },
            { "timezone.excity.Africa/Cairo", "Kaijro" },
            { "Africa/Malabo", Africa_Western },
            { "timezone.excity.Europe/Warsaw", "Warschau" },
            { "Europe/Podgorica", Europe_Central },
            { "Europe/Skopje", Europe_Central },
            { "timezone.excity.Europe/Moscow", "Moskau" },
            { "Africa/Bujumbura", Africa_Central },
            { "Europe/Sarajevo", Europe_Central },
            { "timezone.excity.America/St_Lucia", "Santa Lutschiija" },
            { "timezone.excity.Asia/Qyzylorda", "Qysylorda" },
            { "timezone.excity.America/Indiana/Tell_City", "Tell City en Indiana" },
            { "Africa/Lagos", Africa_Western },
            { "Europe/Kiev", Europe_Eastern },
            { "Europe/Rome", Europe_Central },
            { "Indian/Mauritius", Mauritius },
            { "Europe/Belfast",
                new String[] {
                    "Greenwich sing Standat-Zick",
                    "",
                    "Jru\u00dfbretannije sing Summerzick",
                    "",
                    "",
                    "",
                }
            },
            { "Africa/Luanda", Africa_Western },
            { "timezone.excity.America/St_Johns", "Zint John\u2019s" },
            { "Atlantic/Jan_Mayen", Europe_Central },
            { "timezone.excity.America/St_Barthelemy", "Zint Barth\u00e9lemy" },
            { "Africa/Algiers", Europe_Central },
            { "Europe/Mariehamn", Europe_Eastern },
            { "Europe/Zurich", Europe_Central },
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
            { "Africa/Maseru", Africa_Southern },
            { "Europe/Gibraltar", Europe_Central },
            { "timezone.excity.Africa/Windhoek", "Windhuk" },
            { "Africa/Conakry", GMT },
            { "Africa/Kinshasa", Africa_Western },
            { "Africa/Lubumbashi", Africa_Central },
            { "Europe/Madrid", Europe_Central },
            { "Indian/Antananarivo", Africa_Eastern },
            { "Europe/Vaduz", Europe_Central },
            { "Indian/Mayotte", Africa_Eastern },
            { "timezone.excity.America/St_Thomas", "Zint Thomas" },
            { "Atlantic/Cape_Verde", Cape_Verde },
            { "timezone.excity.Europe/Istanbul", "Istambul" },
            { "timezone.excity.Europe/Vatican", "der Vatikahn" },
            { "Africa/Blantyre", Africa_Central },
            { "timezone.excity.Asia/Tbilisi", "Tiblis" },
            { "timezone.excity.America/North_Dakota/Center", "Zenter en Nood Dakota" },
            { "America/Danmarkshavn", GMT },
            { "Europe/Ljubljana", Europe_Central },
            { "timezone.excity.America/Kentucky/Monticello", "Monticello en Kentucky" },
            { "timezone.excity.Asia/Vladivostok", "Wladiwostok" },
            { "Africa/Lusaka", Africa_Central },
            { "Europe/Berlin", Europe_Central },
            { "timezone.excity.Asia/Ulaanbaatar", "Ulan Bator" },
            { "Europe/Chisinau", Europe_Eastern },
            { "Africa/Dakar", GMT },
            { "Europe/Stockholm", Europe_Central },
            { "Europe/Budapest", Europe_Central },
            { "Europe/Zagreb", Europe_Central },
            { "Europe/Helsinki", Europe_Eastern },
            { "Asia/Beirut", Europe_Eastern },
            { "timezone.excity.Asia/Baghdad", "Bagdad" },
            { "timezone.excity.Antarctica/DumontDUrville", "Dumont-d\u2019Urville-Schtazjohn" },
            { "Africa/Harare", Africa_Central },
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
            { "Africa/Sao_Tome", Africa_Western },
            { "Indian/Chagos", Indian_Ocean },
            { "Europe/Tallinn", Europe_Eastern },
            { "timezone.excity.Europe/Jersey", "J\u00f6\u00f6sei" },
            { "timezone.excity.America/Indiana/Marengo", "Marengo en Indiana" },
            { "Africa/Khartoum", Africa_Central },
            { "Africa/Johannesburg", Africa_Southern },
            { "timezone.excity.America/Guayaquil", "Juayaquil" },
            { "Africa/Ndjamena", Africa_Western },
            { "timezone.excity.America/St_Vincent", "Zint Vintsch\u00e4nt" },
            { "Africa/Bangui", Africa_Western },
            { "timezone.excity.Asia/Singapore", "Singjapuur" },
            { "Europe/Belgrade", Europe_Central },
            { "timezone.excity.Europe/Vienna", "Wien" },
            { "Africa/Bissau", GMT },
            { "timezone.excity.Asia/Nicosia", "Nikosija" },
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
            { "timezone.excity.Europe/Volgograd", "Woljojrad" },
            { "timezone.excity.Asia/Phnom_Penh", "Pnom Penh" },
            { "timezone.excity.Europe/Rome", "Rom" },
            { "Africa/Juba", Africa_Eastern },
            { "Africa/Ceuta", Europe_Central },
            { "timezone.excity.Atlantic/Faeroe", "F\u00e4r\u00f6r" },
            { "Africa/Timbuktu", GMT },
            { "timezone.excity.America/North_Dakota/New_Salem", "Neu Salem en Nood Dakota" },
            { "timezone.excity.Europe/Bucharest", "Bukarest" },
            { "timezone.excity.Europe/Athens", "Athen" },
            { "Africa/Djibouti", Africa_Eastern },
            { "timezone.excity.Atlantic/Cape_Verde", "Kap Verde" },
            { "timezone.excity.America/Indiana/Knox", "Knox en Indiana" },
            { "timezone.excity.Asia/Bishkek", "Bischkek" },
            { "Europe/Sofia", Europe_Eastern },
            { "Africa/Niamey", Africa_Western },
            { "Africa/Nouakchott", GMT },
            { "Europe/Prague", Europe_Central },
            { "Antarctica/Troll", GMT },
            { "timezone.excity.Europe/Zurich", "Z\u00fcresch" },
            { "timezone.excity.Atlantic/Azores", "Azoore" },
            { "timezone.excity.Asia/Ashgabat", "Asshgabat" },
            { "timezone.excity.Pacific/Honolulu", "Honululu" },
            { "timezone.excity.Antarctica/Syowa", "Schoowa-Schtazjohn op d\u00e4 Ensel Onjul" },
            { "Asia/Nicosia", Europe_Eastern },
            { "timezone.excity.Europe/Copenhagen", "Kopenharen" },
            { "Africa/Gaborone", Africa_Central },
            { "Asia/Gaza", Europe_Eastern },
            { "timezone.excity.Etc/Unknown", "- we\u00dfe mer nit -" },
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
            { "timezone.excity.Asia/Riyadh", "Rijad" },
            { "Europe/Bratislava", Europe_Central },
            { "Europe/Copenhagen", Europe_Central },
            { "timezone.excity.America/Indiana/Petersburg", "Petersburg en Indiana" },
            { "timezone.excity.Asia/Rangoon", "Ranjun" },
            { "Atlantic/Azores", Azores },
            { "Europe/Vienna", Europe_Central },
            { "timezone.excity.Europe/Uzhgorod", "Uschjorod" },
            { "timezone.excity.Asia/Damascus", "Damaskus" },
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
            { "Europe/Tirane", Europe_Central },
            { "Arctic/Longyearbyen", Europe_Central },
            { "Europe/Riga", Europe_Eastern },
            { "Asia/Hebron", Europe_Eastern },
            { "Africa/Abidjan", GMT },
            { "Africa/Monrovia", GMT },
            { "timezone.excity.America/St_Kitts", "Zint Kitts" },
            { "timezone.excity.Europe/Kaliningrad", "Kalinninjraad" },
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
            { "timezone.excity.Pacific/Galapagos", "Jalappajos" },
            { "timezone.excity.Europe/Tirane", "Tiraana" },
            { "timezone.excity.Europe/Prague", "Prag" },
            { "Europe/Amsterdam", Europe_Central },
            { "timezone.excity.Asia/Saigon", "Sigong (Ho-Tschi-Minh-Schtadt)" },
            { "Europe/Vatican", Europe_Central },
            { "Africa/Accra", GMT },
            { "Asia/Amman", Europe_Eastern },
            { "timezone.excity.Asia/Yekaterinburg", "Jekaterinburg" },
            { "timezone.excity.America/Indiana/Vincennes", "Vincennes en Indiana" },
            { "Europe/Dublin",
                new String[] {
                    "Greenwich sing Standat-Zick",
                    "",
                    "Irland sing Summerzick",
                    "",
                    "",
                    "",
                }
            },
            { "timezone.excity.Europe/Brussels", "Br\u00fcssel" },
            { "Europe/Athens", Europe_Eastern },
            { "timezone.excity.Europe/Belgrade", "Beljrad" },
            { "Europe/Monaco", Europe_Central },
            { "timezone.excity.Indian/Maldives", "Malldive" },
        };
        return data;
    }
}
