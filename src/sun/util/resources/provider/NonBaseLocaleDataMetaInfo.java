/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

// -- This file was mechanically generated: Do not edit! -- //

/*
 * This class contains a map which records the locale list string for
 * each resource in sun.util.resources & sun.text.resources.
 * It is used to avoid loading non-existent localized resources so that
 * jar files won't be opened unnecessary to look up them.
 */
package sun.util.resources.provider;

import java.util.HashMap;
import java.util.Map;
import sun.util.locale.provider.LocaleDataMetaInfo;
import static sun.util.locale.provider.LocaleProviderAdapter.Type;

public class NonBaseLocaleDataMetaInfo implements LocaleDataMetaInfo {

    private static final Map<String, String> resourceNameToLocales = new HashMap<>(9);

    static {
        /* During JDK build time, #XXX_YYY# will be replaced by a string contain all the locales
           supported by the resource.

           Don't remove the space character between " and #. That is put there purposely so that
           look up locale string such as "en" could be based on if it contains " en ".
        */
        resourceNameToLocales.put("FormatData",
                                  "  ar ar-JO ar-LB ar-SY be be-BY bg bg-BG ca ca-ES cs cs-CZ da da-DK de de-AT de-CH de-DE de-LU el el-CY el-GR en-AU en-CA en-GB en-IE en-IN en-MT en-NZ en-PH en-SG en-ZA es es-AR es-BO es-CL es-CO es-CR es-DO es-EC es-ES es-GT es-HN es-MX es-NI es-PA es-PE es-PR es-PY es-SV es-US es-UY es-VE et et-EE fi fi-FI fr fr-BE fr-CA fr-CH fr-FR ga ga-IE hi-IN hr hr-HR hu hu-HU in in-ID is is-IS it it-CH it-IT iw iw-IL ja ja-JP ko ko-KR lt lt-LT lv lv-LV mk mk-MK ms ms-MY mt mt-MT nb nb-NO nl nl-BE nl-NL nn-NO no no-NO no-NO-NY pl pl-PL pt pt-BR pt-PT ro ro-RO ru ru-RU sk sk-SK sl sl-SI sq sq-AL sr sr-BA sr-CS sr-Latn sr-Latn-ME sr-ME sr-RS sv sv-SE th th-TH tr tr-TR uk uk-UA vi vi-VN zh zh-CN zh-HK zh-Hans-CN zh-Hans-SG zh-Hant-HK zh-Hant-TW zh-SG zh-TW ");

        resourceNameToLocales.put("CollationData",
                                  "  ar be bg ca cs da el es et fi fr hi hr hu is iw ja ko lt lv mk nb nb-NO nn-NO no pl ro ru sk sl sq sr sr-Latn sv th tr uk vi zh zh-HK zh-Hant-HK zh-Hant-TW zh-TW ");

        resourceNameToLocales.put("BreakIteratorInfo",
                                  "  nb nb-NO nn-NO th ");

        resourceNameToLocales.put("BreakIteratorRules",
                                  "  nb nb-NO nn-NO th ");

        resourceNameToLocales.put("TimeZoneNames",
                                  "  de en-CA en-GB en-IE es fr hi it ja ko nb nb-NO nn-NO pt-BR sv zh-CN zh-HK zh-Hans-CN zh-Hant-HK zh-Hant-TW zh-TW ");

        resourceNameToLocales.put("LocaleNames",
                                  "  ar be bg ca cs da de el el-CY en-MT en-PH en-SG es es-US et fi fr ga hi hr hu in is it iw ja ko lt lv mk ms mt nb nb-NO nl nn-NO no no-NO-NY pl pt pt-BR pt-PT ro ru sk sl sq sr sr-Latn sv th tr uk vi zh zh-HK zh-Hans-SG zh-Hant-HK zh-Hant-TW zh-SG zh-TW ");

        resourceNameToLocales.put("CurrencyNames",
                                  "  ar-AE ar-BH ar-DZ ar-EG ar-IQ ar-JO ar-KW ar-LB ar-LY ar-MA ar-OM ar-QA ar-SA ar-SD ar-SY ar-TN ar-YE be-BY bg-BG ca-ES cs-CZ da-DK de de-AT de-CH de-DE de-LU el-CY el-GR en-AU en-CA en-GB en-IE en-IN en-MT en-NZ en-PH en-SG en-ZA es es-AR es-BO es-CL es-CO es-CR es-CU es-DO es-EC es-ES es-GT es-HN es-MX es-NI es-PA es-PE es-PR es-PY es-SV es-US es-UY es-VE et-EE fi-FI fr fr-BE fr-CA fr-CH fr-FR fr-LU ga-IE hi-IN hr-HR hu-HU in-ID is-IS it it-CH it-IT iw-IL ja ja-JP ko ko-KR lt-LT lv-LV mk-MK ms-MY mt-MT nb nb-NO nl-BE nl-NL nn-NO no-NO pl-PL pt pt-BR pt-PT ro-RO ru-RU sk-SK sl-SI sq-AL sr-BA sr-CS sr-Latn-BA sr-Latn-ME sr-Latn-RS sr-ME sr-RS sv sv-SE th-TH tr-TR uk-UA vi-VN zh-CN zh-HK zh-Hans-CN zh-Hans-SG zh-Hant-HK zh-Hant-TW zh-SG zh-TW ");

        resourceNameToLocales.put("CalendarData",
                                  "  ar be bg ca cs da de el el-CY en-GB en-IE en-MT es es-ES es-US et fi fr fr-CA hi hr hu in-ID is it iw ja ko lt lv mk ms-MY mt mt-MT nb nb-NO nl nn-NO no pl pt pt-BR pt-PT ro ru sk sl sq sr sr-Latn-BA sr-Latn-ME sr-Latn-RS sv th tr uk vi zh ");

        resourceNameToLocales.put("AvailableLocales",
                                  " ar ar-AE ar-BH ar-DZ ar-EG ar-IQ ar-JO ar-KW ar-LB ar-LY ar-MA ar-OM ar-QA ar-SA ar-SD ar-SY ar-TN ar-YE be be-BY bg bg-BG ca ca-ES cs cs-CZ da da-DK de de-AT de-CH de-DE de-LU el el-CY el-GR en-AU en-CA en-GB en-IE en-IN en-MT en-NZ en-PH en-SG en-ZA es es-AR es-BO es-CL es-CO es-CR es-CU es-DO es-EC es-ES es-GT es-HN es-MX es-NI es-PA es-PE es-PR es-PY es-SV es-US es-UY es-VE et et-EE fi fi-FI fr fr-BE fr-CA fr-CH fr-FR fr-LU ga ga-IE hi hi-IN hr hr-HR hu hu-HU in in-ID is is-IS it it-CH it-IT iw iw-IL ja ja-JP ja-JP-JP ko ko-KR lt lt-LT lv lv-LV mk mk-MK ms ms-MY mt mt-MT nb nb-NO nl nl-BE nl-NL nn-NO no no-NO no-NO-NY pl pl-PL pt pt-BR pt-PT ro ro-RO ru ru-RU sk sk-SK sl sl-SI sq sq-AL sr sr-BA sr-CS sr-Latn sr-Latn-BA sr-Latn-ME sr-Latn-RS sr-ME sr-RS sv sv-SE th th-TH th-TH-TH tr tr-TR uk uk-UA vi vi-VN zh zh-CN zh-HK zh-Hans-CN zh-Hans-SG zh-Hant-HK zh-Hant-TW zh-SG zh-TW ");
    }

    /*
     * Gets the supported locales string based on the availability of
     * locale data resource bundles for each resource name.
     *
     * @param resourceName the resource name
     * @return the supported locale string for the passed in resource.
     */
    public static String getSupportedLocaleString(String resourceName) {
        return resourceNameToLocales.getOrDefault(resourceName, "");
    }

    @Override
    public Type getType() {
        return Type.JRE;
}

    @Override
    public String availableLanguageTags(String category) {
        return getSupportedLocaleString(category);
    }
}
