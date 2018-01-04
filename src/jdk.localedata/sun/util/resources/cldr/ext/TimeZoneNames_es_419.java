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

public class TimeZoneNames_es_419 extends TimeZoneNamesBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] Easter = new String[] {
               "hora est\u00e1ndar de Isla de Pascua",
               "",
               "hora de verano de la Isla de Pascua",
               "",
               "hora de la Isla de Pascua",
               "",
            };
        final String[] Europe_Central = new String[] {
               "hora est\u00e1ndar de Europa central",
               "\u2205\u2205\u2205",
               "hora de verano de Europa central",
               "\u2205\u2205\u2205",
               "hora de Europa central",
               "\u2205\u2205\u2205",
            };
        final String[] Macquarie = new String[] {
               "hora de la Isla Macquarie",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Gilbert_Islands = new String[] {
               "hora de Islas Gilbert",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] America_Mountain = new String[] {
               "hora est\u00e1ndar de las Monta\u00f1as",
               "",
               "hora de verano de las Monta\u00f1as",
               "",
               "hora de las Monta\u00f1as",
               "",
            };
        final String[] Falkland = new String[] {
               "hora est\u00e1ndar de las Islas Malvinas",
               "",
               "hora de verano de las Islas Malvinas",
               "",
               "hora de las Islas Malvinas",
               "",
            };
        final String[] Europe_Western = new String[] {
               "hora est\u00e1ndar de Europa del Oeste",
               "\u2205\u2205\u2205",
               "hora de verano de Europa del Oeste",
               "\u2205\u2205\u2205",
               "hora de Europa del Oeste",
               "\u2205\u2205\u2205",
            };
        final String[] Norfolk = new String[] {
               "hora de la Isla Norfolk",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Europe_Eastern = new String[] {
               "hora est\u00e1ndar de Europa del Este",
               "\u2205\u2205\u2205",
               "hora de verano de Europa del Este",
               "\u2205\u2205\u2205",
               "hora de Europa del Este",
               "\u2205\u2205\u2205",
            };
        final String[] GMT = new String[] {
               "hora del meridiano de Greenwich",
               "\u2205\u2205\u2205",
               "",
               "",
               "",
               "",
            };
        final String[] Pyongyang = new String[] {
               "hora de Pionyang",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] India = new String[] {
               "hora de India",
               "",
               "",
               "",
               "",
               "",
            };
        final String[] Cook = new String[] {
               "hora est\u00e1ndar de las islas Cook",
               "",
               "hora de verano media de las islas Cook",
               "",
               "hora de las islas Cook",
               "",
            };
        final Object[][] data = new Object[][] {
            { "America/Denver", America_Mountain },
            { "America/Phoenix", America_Mountain },
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
            { "Europe/Ljubljana", Europe_Central },
            { "Europe/Berlin", Europe_Central },
            { "Africa/El_Aaiun", Europe_Western },
            { "Africa/Ouagadougou", GMT },
            { "Africa/Cairo", Europe_Eastern },
            { "Pacific/Rarotonga", Cook },
            { "Europe/Chisinau", Europe_Eastern },
            { "Europe/London", GMT },
            { "Africa/Dakar", GMT },
            { "Europe/Stockholm", Europe_Central },
            { "Europe/Budapest", Europe_Central },
            { "Europe/San_Marino", Europe_Central },
            { "Europe/Zagreb", Europe_Central },
            { "Europe/Helsinki", Europe_Eastern },
            { "Asia/Beirut", Europe_Eastern },
            { "Europe/Brussels", Europe_Central },
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
            { "America/Ojinaga", America_Mountain },
            { "Europe/Warsaw", Europe_Central },
            { "Europe/Tallinn", Europe_Eastern },
            { "Europe/Jersey", GMT },
            { "Asia/Damascus", Europe_Eastern },
            { "Europe/Luxembourg", Europe_Central },
            { "Europe/Belgrade", Europe_Central },
            { "Africa/Bissau", GMT },
            { "America/Yellowknife", America_Mountain },
            { "Atlantic/Reykjavik", GMT },
            { "Europe/Zaporozhye", Europe_Eastern },
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
            { "Atlantic/St_Helena", GMT },
            { "Africa/Ceuta", Europe_Central },
            { "timezone.excity.Asia/Dushanbe", "Duchanb\u00e9" },
            { "Europe/Guernsey", GMT },
            { "Africa/Timbuktu", GMT },
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
            { "Europe/Uzhgorod", Europe_Eastern },
            { "America/Creston", America_Mountain },
            { "Europe/Sofia", Europe_Eastern },
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
            { "Africa/Nouakchott", GMT },
            { "Europe/Isle_of_Man", GMT },
            { "Europe/Prague", Europe_Central },
            { "Antarctica/Troll", GMT },
            { "Africa/Tunis", Europe_Central },
            { "Europe/Andorra", Europe_Central },
            { "Africa/Tripoli", Europe_Eastern },
            { "Africa/Banjul", GMT },
            { "America/Inuvik", America_Mountain },
            { "Antarctica/Macquarie", Macquarie },
            { "Asia/Nicosia", Europe_Eastern },
            { "Europe/Kaliningrad", Europe_Eastern },
            { "SystemV/MST7MDT", America_Mountain },
            { "timezone.excity.Pacific/Wake", "Isla Wake" },
            { "Asia/Pyongyang", Pyongyang },
            { "Europe/Lisbon", Europe_Western },
            { "Europe/Oslo", Europe_Central },
            { "Asia/Gaza", Europe_Eastern },
            { "timezone.excity.Africa/Accra", "Accra" },
            { "Atlantic/Faeroe", Europe_Western },
            { "Etc/GMT", GMT },
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
            { "Pacific/Easter", Easter },
            { "Atlantic/Canary", Europe_Western },
            { "Europe/Bratislava", Europe_Central },
            { "Africa/Lome", GMT },
            { "Asia/Calcutta", India },
            { "Africa/Freetown", GMT },
            { "Europe/Copenhagen", Europe_Central },
            { "Pacific/Norfolk", Norfolk },
            { "Europe/Malta", Europe_Central },
            { "Europe/Vienna", Europe_Central },
            { "Pacific/Tarawa", Gilbert_Islands },
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
            { "Europe/Busingen", Europe_Central },
            { "America/Edmonton", America_Mountain },
            { "Europe/Podgorica", Europe_Central },
            { "Europe/Skopje", Europe_Central },
            { "Europe/Sarajevo", Europe_Central },
            { "Europe/Tirane", Europe_Central },
            { "SystemV/MST7", America_Mountain },
            { "Arctic/Longyearbyen", Europe_Central },
            { "America/Fort_Nelson", America_Mountain },
            { "Europe/Riga", Europe_Eastern },
            { "Europe/Kiev", Europe_Eastern },
            { "Asia/Hebron", Europe_Eastern },
            { "Europe/Rome", Europe_Central },
            { "Europe/Belfast", GMT },
            { "Africa/Abidjan", GMT },
            { "Africa/Monrovia", GMT },
            { "Atlantic/Jan_Mayen", Europe_Central },
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
            { "America/Dawson_Creek", America_Mountain },
            { "Africa/Algiers", Europe_Central },
            { "Europe/Mariehamn", Europe_Eastern },
            { "Europe/Zurich", Europe_Central },
            { "Europe/Vilnius", Europe_Eastern },
            { "America/Shiprock", America_Mountain },
            { "Africa/Bamako", GMT },
            { "Europe/Amsterdam", Europe_Central },
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
            { "Europe/Vatican", Europe_Central },
            { "Africa/Accra", GMT },
            { "timezone.excity.America/Fort_Nelson", "Fuerte Nelson" },
            { "Europe/Gibraltar", Europe_Central },
            { "Africa/Conakry", GMT },
            { "Asia/Amman", Europe_Eastern },
            { "Etc/UTC",
                new String[] {
                    "Hora Universal Coordinada",
                    "",
                    "",
                    "",
                    "",
                    "",
                }
            },
            { "Europe/Madrid", Europe_Central },
            { "Europe/Dublin", GMT },
            { "America/Cambridge_Bay", America_Mountain },
            { "Asia/Colombo", India },
            { "Europe/Vaduz", Europe_Central },
            { "MST7MDT", America_Mountain },
            { "timezone.excity.America/St_Thomas", "Santo Tom\u00e1s" },
            { "timezone.excity.Europe/Busingen", "B\u00fcsingen" },
            { "Europe/Athens", Europe_Eastern },
            { "Atlantic/Stanley", Falkland },
            { "America/Danmarkshavn", GMT },
            { "Europe/Monaco", Europe_Central },
        };
        return data;
    }
}
