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

public class TimeZoneNames_gsw extends TimeZoneNamesBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] Africa_Central = new String[] {
               "Zentralafrikanischi Ziit",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Moscow = new String[] {
               "Moskauer Schtandardziit",
               "",
               "Moskauer Summerziit",
               "",
               "Moskauer Ziit",
               "",
            };
        final String[] Europe_Central = new String[] {
               "Mitteleurop\u00e4ischi Schtandardziit",
               "MEZ",
               "Mitteleurop\u00e4ischi Summerziit",
               "MESZ",
               "Mitteleurop\u00e4ischi Ziit",
               "MEZ",
            };
        final String[] Acre = new String[] {
               "Acre-Schtandardziit",
               "",
               "Acre-Summerziit",
               "",
               "Acre-Ziit",
               "",
            };
        final String[] Africa_Eastern = new String[] {
               "Oschtafrikanischi Ziit",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Europe_Western = new String[] {
               "Weschteurop\u00e4ischi Schtandardziit",
               "WEZ",
               "Weschteurop\u00e4ischi Summerziit",
               "WESZ",
               "Weschteurop\u00e4ischi Ziit",
               "WEZ",
            };
        final String[] Europe_Eastern = new String[] {
               "Oschteurop\u00e4ischi Schtandardziit",
               "OEZ",
               "Oschteurop\u00e4ischi Summerziit",
               "OESZ",
               "Oschteurop\u00e4ischi Ziit",
               "OEZ",
            };
        final String[] Afghanistan = new String[] {
               "Afghanischtan-Ziit",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Africa_Western = new String[] {
               "Weschtafrikanischi Schtandardziit",
               "",
               "Weschtafrikanischi Summerziit",
               "",
               "Weschtafrikanischi Ziit",
               "",
            };
        final String[] Africa_Southern = new String[] {
               "S\u00fc\u00fcdafrikanischi ziit",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] America_Central = new String[] {
               "Amerika-Zentraal Schtandardziit",
               "",
               "Amerika-Zentraal Summerziit",
               "",
               "Amerika-Zentraal Ziit",
               "",
            };
        final String[] Alaska = new String[] {
               "Alaska-Schtandardziit",
               "",
               "Alaska-Summerziit",
               "",
               "Alaska-Ziit",
               "",
            };
        final String[] Amazon = new String[] {
               "Amazonas-Schtandardziit",
               "",
               "Amazonas-Summerziit",
               "",
               "Amazonas-Ziit",
               "",
            };
        final Object[][] data = new Object[][] {
            { "America/Chicago", America_Central },
            { "America/Anchorage", Alaska },
            { "America/Sitka", Alaska },
            { "Europe/Paris", Europe_Central },
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
            { "America/Cuiaba", Amazon },
            { "timezone.excity.Asia/Aqtobe", "Aktobe" },
            { "Africa/Nairobi", Africa_Eastern },
            { "Africa/Libreville", Africa_Western },
            { "Africa/Maputo", Africa_Central },
            { "America/El_Salvador", America_Central },
            { "Africa/El_Aaiun", Europe_Western },
            { "Africa/Cairo", Europe_Eastern },
            { "Africa/Mbabane", Africa_Southern },
            { "timezone.excity.Europe/Luxembourg", "Luxemburg" },
            { "America/North_Dakota/Center", America_Central },
            { "America/Guatemala", America_Central },
            { "timezone.excity.Antarctica/Vostok", "Woschtok" },
            { "America/Belize", America_Central },
            { "timezone.excity.Asia/Hong_Kong", "Hongkong" },
            { "Europe/San_Marino", Europe_Central },
            { "America/Indiana/Tell_City", America_Central },
            { "timezone.excity.America/Mexico_City", "Mexiko-Schtadt" },
            { "America/Managua", America_Central },
            { "Europe/Brussels", Europe_Central },
            { "timezone.excity.America/Curacao", "Cura\u00e7ao" },
            { "Africa/Douala", Africa_Western },
            { "timezone.excity.Pacific/Fiji", "Fidschi" },
            { "timezone.excity.Asia/Kamchatka", "Kamtschatka" },
            { "timezone.excity.Europe/Lisbon", "Lissabon" },
            { "Europe/Warsaw", Europe_Central },
            { "timezone.excity.Asia/Yakutsk", "Jakutsk" },
            { "timezone.excity.Africa/Djibouti", "Dschibuti" },
            { "America/Tegucigalpa", America_Central },
            { "Asia/Damascus", Europe_Eastern },
            { "America/Eirunepe", Acre },
            { "Europe/Luxembourg", Europe_Central },
            { "timezone.excity.Europe/Zaporozhye", "Saporischja" },
            { "timezone.excity.Asia/Tashkent", "Taschkent" },
            { "Africa/Brazzaville", Africa_Western },
            { "Europe/Zaporozhye", Europe_Eastern },
            { "Africa/Porto-Novo", Africa_Western },
            { "timezone.excity.Africa/Addis_Ababa", "Addis Abeba" },
            { "Africa/Dar_es_Salaam", Africa_Eastern },
            { "timezone.excity.Asia/Dushanbe", "Duschanbe" },
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
            { "timezone.excity.America/Port_of_Spain", "Port-of-Spain" },
            { "Africa/Addis_Ababa", Africa_Eastern },
            { "Europe/Uzhgorod", Europe_Eastern },
            { "Africa/Kigali", Africa_Central },
            { "timezone.excity.America/El_Salvador", "Salvador" },
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
            { "timezone.excity.Pacific/Easter", "Oschterinsle" },
            { "America/Mexico_City", America_Central },
            { "Africa/Tunis", Europe_Central },
            { "Europe/Andorra", Europe_Central },
            { "Africa/Tripoli", Europe_Eastern },
            { "Indian/Comoro", Africa_Eastern },
            { "timezone.excity.Asia/Sakhalin", "Sachalin" },
            { "America/Matamoros", America_Central },
            { "Europe/Kaliningrad", Europe_Eastern },
            { "Africa/Windhoek", Africa_Central },
            { "Europe/Lisbon", Europe_Western },
            { "timezone.excity.Atlantic/Canary", "Kanare" },
            { "Europe/Oslo", Europe_Central },
            { "Africa/Mogadishu", Africa_Eastern },
            { "timezone.excity.Africa/Mogadishu", "Mogadischu" },
            { "CST6CDT", America_Central },
            { "Atlantic/Canary", Europe_Western },
            { "timezone.excity.Asia/Krasnoyarsk", "Krasnojarsk" },
            { "America/Manaus", Amazon },
            { "America/Menominee", America_Central },
            { "Europe/Malta", Europe_Central },
            { "timezone.excity.Asia/Yerevan", "Erivan" },
            { "America/Resolute", America_Central },
            { "timezone.excity.Asia/Qatar", "Katar" },
            { "Africa/Asmera", Africa_Eastern },
            { "Africa/Kampala", Africa_Eastern },
            { "Europe/Busingen", Europe_Central },
            { "timezone.excity.Africa/Cairo", "Kairo" },
            { "Africa/Malabo", Africa_Western },
            { "timezone.excity.Europe/Warsaw", "Warschau" },
            { "Europe/Podgorica", Europe_Central },
            { "Europe/Skopje", Europe_Central },
            { "timezone.excity.Europe/Moscow", "Moskau" },
            { "Africa/Bujumbura", Africa_Central },
            { "Europe/Sarajevo", Europe_Central },
            { "Europe/Minsk", Moscow },
            { "Africa/Lagos", Africa_Western },
            { "timezone.excity.Asia/Taipei", "Taipeh" },
            { "Europe/Kiev", Europe_Eastern },
            { "Europe/Rome", Europe_Central },
            { "Africa/Luanda", Africa_Western },
            { "America/Regina", America_Central },
            { "Atlantic/Jan_Mayen", Europe_Central },
            { "Africa/Algiers", Europe_Central },
            { "Europe/Mariehamn", Europe_Eastern },
            { "Europe/Zurich", Europe_Central },
            { "Europe/Vilnius", Europe_Eastern },
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
            { "Africa/Kinshasa", Africa_Western },
            { "Africa/Lubumbashi", Africa_Central },
            { "Europe/Madrid", Europe_Central },
            { "America/Bahia_Banderas", America_Central },
            { "timezone.excity.Africa/Sao_Tome", "S\u00e3o Tom\u00e9" },
            { "Indian/Antananarivo", Africa_Eastern },
            { "Europe/Vaduz", Europe_Central },
            { "Indian/Mayotte", Africa_Eastern },
            { "Europe/Volgograd", Moscow },
            { "Africa/Blantyre", Africa_Central },
            { "America/Rio_Branco", Acre },
            { "timezone.excity.Asia/Tbilisi", "Tiflis" },
            { "Europe/Ljubljana", Europe_Central },
            { "timezone.excity.Asia/Vladivostok", "Wladiwostok" },
            { "Africa/Lusaka", Africa_Central },
            { "Europe/Berlin", Europe_Central },
            { "timezone.excity.Asia/Ulaanbaatar", "Ulan-Baator" },
            { "timezone.excity.Asia/Macau", "Macao" },
            { "Europe/Moscow", Moscow },
            { "Europe/Chisinau", Europe_Eastern },
            { "America/Rankin_Inlet", America_Central },
            { "Europe/Stockholm", Europe_Central },
            { "SystemV/CST6CDT", America_Central },
            { "Europe/Budapest", Europe_Central },
            { "America/Porto_Velho", Amazon },
            { "Europe/Zagreb", Europe_Central },
            { "timezone.excity.Asia/Novosibirsk", "Nowosibirsk" },
            { "Europe/Helsinki", Europe_Eastern },
            { "Asia/Beirut", Europe_Eastern },
            { "timezone.excity.Asia/Baghdad", "Bagdad" },
            { "timezone.excity.Antarctica/DumontDUrville", "Dumont D\u2019Urville" },
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
            { "America/Nome", Alaska },
            { "Africa/Sao_Tome", Africa_Western },
            { "Europe/Tallinn", Europe_Eastern },
            { "Africa/Khartoum", Africa_Central },
            { "Africa/Johannesburg", Africa_Southern },
            { "Africa/Ndjamena", Africa_Western },
            { "timezone.excity.Atlantic/South_Georgia", "S\u00fc\u00fcd-Georgie" },
            { "Africa/Bangui", Africa_Western },
            { "America/Rainy_River", America_Central },
            { "timezone.excity.Asia/Singapore", "Singapur" },
            { "Europe/Belgrade", Europe_Central },
            { "timezone.excity.Europe/Vienna", "Wien" },
            { "timezone.excity.America/Cayman", "Kaimaninsle" },
            { "timezone.excity.America/Havana", "Havanna" },
            { "America/Juneau", Alaska },
            { "timezone.excity.Asia/Nicosia", "Nikosia" },
            { "timezone.excity.Europe/Kiev", "Kiew" },
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
            { "timezone.excity.Europe/Rome", "Rom" },
            { "Africa/Juba", Africa_Eastern },
            { "America/Campo_Grande", Amazon },
            { "timezone.excity.Asia/Tokyo", "Tokio" },
            { "Africa/Ceuta", Europe_Central },
            { "timezone.excity.Atlantic/Faeroe", "F\u00e4r\u00f6er" },
            { "timezone.excity.Asia/Muscat", "Muschkat" },
            { "America/Swift_Current", America_Central },
            { "timezone.excity.Asia/Pyongyang", "Pj\u00f6ngjang" },
            { "timezone.excity.Africa/El_Aaiun", "El Aai\u00fan" },
            { "timezone.excity.Europe/Bucharest", "Bukarescht" },
            { "America/Metlakatla", Alaska },
            { "timezone.excity.Europe/Athens", "Athen" },
            { "Africa/Djibouti", Africa_Eastern },
            { "timezone.excity.Atlantic/Cape_Verde", "Kap Verde" },
            { "timezone.excity.America/Indiana/Knox", "Knox" },
            { "Europe/Simferopol", Moscow },
            { "timezone.excity.Asia/Bishkek", "Bischkek" },
            { "Europe/Sofia", Europe_Eastern },
            { "Africa/Niamey", Africa_Western },
            { "Europe/Prague", Europe_Central },
            { "timezone.excity.Europe/Zurich", "Z\u00fcri" },
            { "timezone.excity.Atlantic/Azores", "Azore" },
            { "timezone.excity.America/Jamaica", "Jamaika" },
            { "timezone.excity.Indian/Reunion", "R\u00e9union" },
            { "Asia/Nicosia", Europe_Eastern },
            { "timezone.excity.Europe/Copenhagen", "Kopehage" },
            { "Africa/Gaborone", Africa_Central },
            { "America/Boa_Vista", Amazon },
            { "Asia/Gaza", Europe_Eastern },
            { "timezone.excity.Africa/Accra", "Akkra" },
            { "timezone.excity.Etc/Unknown", "Unbekannt" },
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
            { "timezone.excity.Asia/Riyadh", "Riad" },
            { "Europe/Bratislava", Europe_Central },
            { "timezone.excity.Indian/Christmas", "Wienachts-Insle" },
            { "America/Yakutat", Alaska },
            { "Asia/Kabul", Afghanistan },
            { "Europe/Copenhagen", Europe_Central },
            { "Europe/Vienna", Europe_Central },
            { "SystemV/YST9YDT", Alaska },
            { "timezone.excity.Europe/Uzhgorod", "Uschgorod" },
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
            { "America/Merida", America_Central },
            { "Europe/Tirane", Europe_Central },
            { "timezone.excity.Africa/Algiers", "Algier" },
            { "Arctic/Longyearbyen", Europe_Central },
            { "Europe/Riga", Europe_Eastern },
            { "Asia/Hebron", Europe_Eastern },
            { "timezone.excity.Africa/Khartoum", "Khartum" },
            { "timezone.excity.Africa/Ouagadougou", "Wagadugu" },
            { "timezone.excity.Asia/Tehran", "Teheran" },
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
            { "America/North_Dakota/New_Salem", America_Central },
            { "timezone.excity.Africa/Dar_es_Salaam", "Daressalam" },
            { "America/Costa_Rica", America_Central },
            { "timezone.excity.Europe/Tirane", "Tirana" },
            { "America/Winnipeg", America_Central },
            { "Europe/Amsterdam", Europe_Central },
            { "America/Indiana/Knox", America_Central },
            { "timezone.excity.Atlantic/Bermuda", "Bermudas" },
            { "America/North_Dakota/Beulah", America_Central },
            { "Europe/Vatican", Europe_Central },
            { "timezone.excity.America/Asuncion", "Asunci\u00f3n" },
            { "Asia/Amman", Europe_Eastern },
            { "timezone.excity.Europe/Chisinau", "Kischinau" },
            { "timezone.excity.Asia/Yekaterinburg", "Jekaterinburg" },
            { "timezone.excity.Europe/Brussels", "Br\u00fcssel" },
            { "timezone.excity.Indian/Comoro", "Komore" },
            { "America/Monterrey", America_Central },
            { "Europe/Athens", Europe_Eastern },
            { "timezone.excity.Europe/Vilnius", "Wilna" },
            { "Europe/Monaco", Europe_Central },
            { "SystemV/CST6", America_Central },
            { "timezone.excity.Indian/Maldives", "Maledive" },
        };
        return data;
    }
}
