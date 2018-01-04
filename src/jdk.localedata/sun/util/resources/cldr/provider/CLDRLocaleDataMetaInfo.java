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
package sun.util.resources.cldr.provider;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import sun.util.locale.provider.LocaleDataMetaInfo;
import sun.util.locale.provider.LocaleProviderAdapter;

public class CLDRLocaleDataMetaInfo implements LocaleDataMetaInfo {
    private static final Map<String, String> resourceNameToLocales = new HashMap<>();

    static {
        resourceNameToLocales.put("AvailableLocales",
              " af af-NA af-ZA agq agq-CM ak ak-GH am am-ET ar ar-001 ar-AE ar-BH ar-DJ ar-DZ ar-EG ar-EH ar-ER ar-IL ar-IQ ar-JO ar-KM ar-KW ar-LB ar-LY ar-MA ar-MR ar-OM ar-PS ar-QA ar-SA ar-SD ar-SO ar-SS ar-SY ar-TD ar-TN ar-YE as as-IN asa asa-TZ ast ast-ES az az-Cyrl az-Cyrl-AZ az-Latn az-Latn-AZ bas bas-CM be be-BY bem bem-ZM bez bez-TZ bg bg-BG bm bm-ML bn bn-BD bn-IN bo bo-CN bo-IN br br-FR brx brx-IN bs bs-Cyrl bs-Cyrl-BA bs-Latn bs-Latn-BA ca ca-AD ca-ES ca-ES-VALENCIA ca-FR ca-IT ccp ccp-BD ccp-IN ce ce-RU cgg cgg-UG chr chr-US ckb ckb-IQ ckb-IR cs cs-CZ cu cu-RU cy cy-GB da da-DK da-GL dav dav-KE de de-AT de-BE de-CH de-DE de-IT de-LI de-LU dje dje-NE dsb dsb-DE dua dua-CM dyo dyo-SN dz dz-BT ebu ebu-KE ee ee-GH ee-TG el el-CY el-GR en-001 en-150 en-AG en-AI en-AS en-AT en-AU en-BB en-BE en-BI en-BM en-BS en-BW en-BZ en-CA en-CC en-CH en-CK en-CM en-CX en-CY en-DE en-DG en-DK en-DM en-ER en-FI en-FJ en-FK en-FM en-GB en-GD en-GG en-GH en-GI en-GM en-GU en-GY en-HK en-IE en-IL en-IM en-IN en-IO en-JE en-JM en-KE en-KI en-KN en-KY en-LC en-LR en-LS en-MG en-MH en-MO en-MP en-MS en-MT en-MU en-MW en-MY en-NA en-NF en-NG en-NL en-NR en-NU en-NZ en-PG en-PH en-PK en-PN en-PR en-PW en-RW en-SB en-SC en-SD en-SE en-SG en-SH en-SI en-SL en-SS en-SX en-SZ en-TC en-TK en-TO en-TT en-TV en-TZ en-UG en-UM en-VC en-VG en-VI en-VU en-WS en-ZA en-ZM en-ZW eo eo-001 es es-419 es-AR es-BO es-BR es-BZ es-CL es-CO es-CR es-CU es-DO es-EA es-EC es-ES es-GQ es-GT es-HN es-IC es-MX es-NI es-PA es-PE es-PH es-PR es-PY es-SV es-US es-UY es-VE et et-EE eu eu-ES ewo ewo-CM fa fa-AF fa-IR ff ff-CM ff-GN ff-MR ff-SN fi fi-FI fil fil-PH fo fo-DK fo-FO fr fr-BE fr-BF fr-BI fr-BJ fr-BL fr-CA fr-CD fr-CF fr-CG fr-CH fr-CI fr-CM fr-DJ fr-DZ fr-FR fr-GA fr-GF fr-GN fr-GP fr-GQ fr-HT fr-KM fr-LU fr-MA fr-MC fr-MF fr-MG fr-ML fr-MQ fr-MR fr-MU fr-NC fr-NE fr-PF fr-PM fr-RE fr-RW fr-SC fr-SN fr-SY fr-TD fr-TG fr-TN fr-VU fr-WF fr-YT fur fur-IT fy fy-NL ga ga-IE gd gd-GB gl gl-ES gsw gsw-CH gsw-FR gsw-LI gu gu-IN guz guz-KE gv gv-IM ha ha-GH ha-NE ha-NG haw haw-US he he-IL hi hi-IN hr hr-BA hr-HR hsb hsb-DE hu hu-HU hy hy-AM id id-ID ig ig-NG ii ii-CN is is-IS it it-CH it-IT it-SM it-VA ja ja-JP jgo jgo-CM jmc jmc-TZ ka ka-GE kab kab-DZ kam kam-KE kde kde-TZ kea kea-CV khq khq-ML ki ki-KE kk kk-KZ kkj kkj-CM kl kl-GL kln kln-KE km km-KH kn kn-IN ko ko-KP ko-KR kok kok-IN ks ks-IN ksb ksb-TZ ksf ksf-CM ksh ksh-DE kw kw-GB ky ky-KG lag lag-TZ lb lb-LU lg lg-UG lkt lkt-US ln ln-AO ln-CD ln-CF ln-CG lo lo-LA lrc lrc-IQ lrc-IR lt lt-LT lu lu-CD luo luo-KE luy luy-KE lv lv-LV mas mas-KE mas-TZ mer mer-KE mfe mfe-MU mg mg-MG mgh mgh-MZ mgo mgo-CM mk mk-MK ml ml-IN mn mn-MN mr mr-IN ms ms-BN ms-MY ms-SG mt mt-MT mua mua-CM my my-MM mzn mzn-IR naq naq-NA nb nb-NO nb-SJ nd nd-ZW nds nds-DE nds-NL ne ne-IN ne-NP nl nl-AW nl-BE nl-BQ nl-CW nl-NL nl-SR nl-SX nmg nmg-CM nn nn-NO nnh nnh-CM nus nus-SS nyn nyn-UG om om-ET om-KE or or-IN os os-GE os-RU pa pa-Arab pa-Arab-PK pa-Guru pa-Guru-IN pl pl-PL prg prg-001 ps ps-AF pt pt-AO pt-BR pt-CH pt-CV pt-GQ pt-GW pt-LU pt-MO pt-MZ pt-PT pt-ST pt-TL qu qu-BO qu-EC qu-PE rm rm-CH rn rn-BI ro ro-MD ro-RO rof rof-TZ ru ru-BY ru-KG ru-KZ ru-MD ru-RU ru-UA rw rw-RW rwk rwk-TZ sah sah-RU saq saq-KE sbp sbp-TZ sd sd-PK se se-FI se-NO se-SE seh seh-MZ ses ses-ML sg sg-CF shi shi-Latn shi-Latn-MA shi-Tfng shi-Tfng-MA si si-LK sk sk-SK sl sl-SI smn smn-FI sn sn-ZW so so-DJ so-ET so-KE so-SO sq sq-AL sq-MK sq-XK sr sr-Cyrl sr-Cyrl-BA sr-Cyrl-ME sr-Cyrl-RS sr-Cyrl-XK sr-Latn sr-Latn-BA sr-Latn-ME sr-Latn-RS sr-Latn-XK sv sv-AX sv-FI sv-SE sw sw-CD sw-KE sw-TZ sw-UG ta ta-IN ta-LK ta-MY ta-SG te te-IN teo teo-KE teo-UG tg tg-TJ th th-TH ti ti-ER ti-ET tk tk-TM to to-TO tr tr-CY tr-TR tt tt-RU twq twq-NE tzm tzm-MA ug ug-CN uk uk-UA ur ur-IN ur-PK uz uz-Arab uz-Arab-AF uz-Cyrl uz-Cyrl-UZ uz-Latn uz-Latn-UZ vai vai-Latn vai-Latn-LR vai-Vaii vai-Vaii-LR vi vi-VN vo vo-001 vun vun-TZ wae wae-CH wo wo-SN xog xog-UG yav yav-CM yi yi-001 yo yo-BJ yo-NG yue yue-Hans yue-Hans-CN yue-Hant yue-Hant-HK zgh zgh-MA zh zh-Hans zh-Hans-CN zh-Hans-HK zh-Hans-MO zh-Hans-SG zh-Hant zh-Hant-HK zh-Hant-MO zh-Hant-TW zu zu-ZA");
    }

    @Override
    public LocaleProviderAdapter.Type getType() {
        return LocaleProviderAdapter.Type.CLDR;
    }

    @Override
    public String availableLanguageTags(String category) {
        return resourceNameToLocales.getOrDefault(category, "");
    }

}