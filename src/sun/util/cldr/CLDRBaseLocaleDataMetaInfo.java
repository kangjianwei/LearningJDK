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
package sun.util.cldr;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import sun.util.locale.provider.LocaleDataMetaInfo;
import sun.util.locale.provider.LocaleProviderAdapter;

public class CLDRBaseLocaleDataMetaInfo implements LocaleDataMetaInfo {
    private static final Map<String, String> resourceNameToLocales = new HashMap<>();
    private static final Map<Locale, String[]> parentLocalesMap = new HashMap<>();
    private static final Map<String, String> languageAliasMap = new HashMap<>();

    static {
        resourceNameToLocales.put("AvailableLocales",
              " en en-US en-US-POSIX");
        parentLocalesMap.put(Locale.forLanguageTag("es-419"),
             new String[] {
                 "", "es-AR", "es-BO", "es-BR", "es-BZ", "es-CL", "es-CO", "es-CR", 
                 "es-CU", "es-DO", "es-EC", "es-GT", "es-HN", "es-MX", "es-NI", "es-PA", 
                 "es-PE", "es-PR", "es-PY", "es-SV", "es-US", "es-UY", "es-VE", 
             });
        parentLocalesMap.put(Locale.ROOT,
             new String[] {
                 "", "az-Arab", "az-Cyrl", "bm-Nkoo", "bs-Cyrl", "en-Dsrt", "en-Shaw", 
                 "ha-Arab", "iu-Latn", "mn-Mong", "ms-Arab", "pa-Arab", "shi-Latn", 
                 "sr-Latn", "uz-Arab", "uz-Cyrl", "vai-Latn", "yue-Hans", "zh-Hant", 
             });
        parentLocalesMap.put(Locale.forLanguageTag("en-001"),
             new String[] {
                 "", "en-150", "en-AG", "en-AI", "en-AU", "en-BB", "en-BE", "en-BM", 
                 "en-BS", "en-BW", "en-BZ", "en-CA", "en-CC", "en-CK", "en-CM", "en-CX", 
                 "en-CY", "en-DG", "en-DM", "en-ER", "en-FJ", "en-FK", "en-FM", "en-GB", 
                 "en-GD", "en-GG", "en-GH", "en-GI", "en-GM", "en-GY", "en-HK", "en-IE", 
                 "en-IL", "en-IM", "en-IN", "en-IO", "en-JE", "en-JM", "en-KE", "en-KI", 
                 "en-KN", "en-KY", "en-LC", "en-LR", "en-LS", "en-MG", "en-MO", "en-MS", 
                 "en-MT", "en-MU", "en-MW", "en-MY", "en-NA", "en-NF", "en-NG", "en-NR", 
                 "en-NU", "en-NZ", "en-PG", "en-PH", "en-PK", "en-PN", "en-PW", "en-RW", 
                 "en-SB", "en-SC", "en-SD", "en-SG", "en-SH", "en-SL", "en-SS", "en-SX", 
                 "en-SZ", "en-TC", "en-TK", "en-TO", "en-TT", "en-TV", "en-TZ", "en-UG", 
                 "en-VC", "en-VG", "en-VU", "en-WS", "en-ZA", "en-ZM", "en-ZW", 
             });
        parentLocalesMap.put(Locale.forLanguageTag("zh-Hant-HK"),
             new String[] {
                 "", "zh-Hant-MO", 
             });
        parentLocalesMap.put(Locale.forLanguageTag("pt-PT"),
             new String[] {
                 "", "pt-AO", "pt-CH", "pt-CV", "pt-GQ", "pt-GW", "pt-LU", "pt-MO", 
                 "pt-MZ", "pt-ST", "pt-TL", 
             });
        parentLocalesMap.put(Locale.forLanguageTag("en-150"),
             new String[] {
                 "", "en-AT", "en-CH", "en-DE", "en-DK", "en-FI", "en-NL", "en-SE", 
                 "en-SI", 
             });
                languageAliasMap.put("pa-PK", "pa-Arab-PK");
                languageAliasMap.put("zh-hakka", "hak");
                languageAliasMap.put("i-ami", "ami");
                languageAliasMap.put("ug-Arab-CN", "ug-CN");
                languageAliasMap.put("aa-SAAHO", "ssy");
                languageAliasMap.put("ha-Latn-GH", "ha-GH");
                languageAliasMap.put("zh-xiang", "hsn");
                languageAliasMap.put("kk-Cyrl-KZ", "kk-KZ");
                languageAliasMap.put("zh-CN", "zh-Hans-CN");
                languageAliasMap.put("scc", "sr");
                languageAliasMap.put("i-tay", "tay");
                languageAliasMap.put("sgn-CH-DE", "sgg");
                languageAliasMap.put("yue-HK", "yue-Hant-HK");
                languageAliasMap.put("mo", "ro-MD");
                languageAliasMap.put("bs-BA", "bs-Latn-BA");
                languageAliasMap.put("scr", "hr");
                languageAliasMap.put("sr-XK", "sr-Cyrl-XK");
                languageAliasMap.put("i-tsu", "tsu");
                languageAliasMap.put("zh-SG", "zh-Hans-SG");
                languageAliasMap.put("i-klingon", "tlh");
                languageAliasMap.put("zh-min-nan", "nan");
                languageAliasMap.put("zh-guoyu", "zh");
                languageAliasMap.put("ks-Arab-IN", "ks-IN");
                languageAliasMap.put("i-pwn", "pwn");
                languageAliasMap.put("az-AZ", "az-Latn-AZ");
                languageAliasMap.put("sgn-BE-NL", "vgt");
                languageAliasMap.put("zh-MO", "zh-Hant-MO");
                languageAliasMap.put("ky-Cyrl-KG", "ky-KG");
                languageAliasMap.put("jw", "jv");
                languageAliasMap.put("ms-Latn-MY", "ms-MY");
                languageAliasMap.put("sr-ME", "sr-Latn-ME");
                languageAliasMap.put("i-navajo", "nv");
                languageAliasMap.put("sh", "sr-Latn");
                languageAliasMap.put("pa-IN", "pa-Guru-IN");
                languageAliasMap.put("tzm-Latn-MA", "tzm-MA");
                languageAliasMap.put("i-bnn", "bnn");
                languageAliasMap.put("i-tao", "tao");
                languageAliasMap.put("sr-BA", "sr-Cyrl-BA");
                languageAliasMap.put("uz-AF", "uz-Arab-AF");
                languageAliasMap.put("uz-UZ", "uz-Latn-UZ");
                languageAliasMap.put("zh-HK", "zh-Hant-HK");
                languageAliasMap.put("zh-TW", "zh-Hant-TW");
                languageAliasMap.put("cnr", "sr-ME");
                languageAliasMap.put("sr-RS", "sr-Cyrl-RS");
                languageAliasMap.put("mn-Cyrl-MN", "mn-MN");
                languageAliasMap.put("ms-Latn-SG", "ms-SG");
                languageAliasMap.put("shi-MA", "shi-Tfng-MA");
                languageAliasMap.put("ha-Latn-NE", "ha-NE");
                languageAliasMap.put("ha-Latn-NG", "ha-NG");
                languageAliasMap.put("ms-Latn-BN", "ms-BN");
                languageAliasMap.put("i-hak", "hak");
                languageAliasMap.put("sgn-BE-FR", "sfb");
                languageAliasMap.put("art-lojban", "jbo");
                languageAliasMap.put("i-lux", "lb");
                languageAliasMap.put("vai-LR", "vai-Vaii-LR");
                languageAliasMap.put("yue-CN", "yue-Hans-CN");
                languageAliasMap.put("tl", "fil");
    }

    private static class TZCanonicalIDMapHolder {
        static final Map<String, String> tzCanonicalIDMap = new HashMap<>(600);
        static {
            tzCanonicalIDMap.put("fmksa", "Pacific/Kosrae");
            tzCanonicalIDMap.put("ruyks", "Asia/Yakutsk");
            tzCanonicalIDMap.put("ltvno", "Europe/Vilnius");
            tzCanonicalIDMap.put("caydq", "America/Dawson_Creek");
            tzCanonicalIDMap.put("brsao", "America/Sao_Paulo");
            tzCanonicalIDMap.put("Brazil/East", "America/Sao_Paulo");
            tzCanonicalIDMap.put("pwror", "Pacific/Palau");
            tzCanonicalIDMap.put("chzrh", "Europe/Zurich");
            tzCanonicalIDMap.put("svsal", "America/El_Salvador");
            tzCanonicalIDMap.put("vistt", "America/St_Thomas");
            tzCanonicalIDMap.put("America/Virgin", "America/St_Thomas");
            tzCanonicalIDMap.put("ruikt", "Asia/Irkutsk");
            tzCanonicalIDMap.put("gpmsb", "America/Marigot");
            tzCanonicalIDMap.put("aruaq", "America/Argentina/San_Juan");
            tzCanonicalIDMap.put("sesto", "Europe/Stockholm");
            tzCanonicalIDMap.put("uymvd", "America/Montevideo");
            tzCanonicalIDMap.put("mxmty", "America/Monterrey");
            tzCanonicalIDMap.put("esmad", "Europe/Madrid");
            tzCanonicalIDMap.put("grath", "Europe/Athens");
            tzCanonicalIDMap.put("aqcas", "Antarctica/Casey");
            tzCanonicalIDMap.put("bjptn", "Africa/Porto-Novo");
            tzCanonicalIDMap.put("cayda", "America/Dawson");
            tzCanonicalIDMap.put("pnpcn", "Pacific/Pitcairn");
            tzCanonicalIDMap.put("ruovb", "Asia/Novosibirsk");
            tzCanonicalIDMap.put("mxhmo", "America/Hermosillo");
            tzCanonicalIDMap.put("sjlyr", "Arctic/Longyearbyen");
            tzCanonicalIDMap.put("Atlantic/Jan_Mayen", "Arctic/Longyearbyen");
            tzCanonicalIDMap.put("uztas", "Asia/Tashkent");
            tzCanonicalIDMap.put("aqddu", "Antarctica/DumontDUrville");
            tzCanonicalIDMap.put("camon", "America/Moncton");
            tzCanonicalIDMap.put("gmt", "Etc/GMT");
            tzCanonicalIDMap.put("Etc/GMT+0", "Etc/GMT");
            tzCanonicalIDMap.put("Etc/GMT-0", "Etc/GMT");
            tzCanonicalIDMap.put("Etc/GMT0", "Etc/GMT");
            tzCanonicalIDMap.put("Etc/Greenwich", "Etc/GMT");
            tzCanonicalIDMap.put("GMT", "Etc/GMT");
            tzCanonicalIDMap.put("GMT+0", "Etc/GMT");
            tzCanonicalIDMap.put("GMT-0", "Etc/GMT");
            tzCanonicalIDMap.put("GMT0", "Etc/GMT");
            tzCanonicalIDMap.put("Greenwich", "Etc/GMT");
            tzCanonicalIDMap.put("usome", "America/Nome");
            tzCanonicalIDMap.put("uasip", "Europe/Simferopol");
            tzCanonicalIDMap.put("vnsgn", "Asia/Saigon");
            tzCanonicalIDMap.put("Asia/Ho_Chi_Minh", "Asia/Saigon");
            tzCanonicalIDMap.put("nimga", "America/Managua");
            tzCanonicalIDMap.put("joamm", "Asia/Amman");
            tzCanonicalIDMap.put("rwkgl", "Africa/Kigali");
            tzCanonicalIDMap.put("etadd", "Africa/Addis_Ababa");
            tzCanonicalIDMap.put("kgfru", "Asia/Bishkek");
            tzCanonicalIDMap.put("iddjj", "Asia/Jayapura");
            tzCanonicalIDMap.put("cayek", "America/Rankin_Inlet");
            tzCanonicalIDMap.put("sbhir", "Pacific/Guadalcanal");
            tzCanonicalIDMap.put("mzmpm", "Africa/Maputo");
            tzCanonicalIDMap.put("rugdx", "Asia/Magadan");
            tzCanonicalIDMap.put("canpg", "America/Nipigon");
            tzCanonicalIDMap.put("brrbr", "America/Rio_Branco");
            tzCanonicalIDMap.put("America/Porto_Acre", "America/Rio_Branco");
            tzCanonicalIDMap.put("Brazil/Acre", "America/Rio_Branco");
            tzCanonicalIDMap.put("est5edt", "EST5EDT");
            tzCanonicalIDMap.put("cobog", "America/Bogota");
            tzCanonicalIDMap.put("aiaxa", "America/Anguilla");
            tzCanonicalIDMap.put("cayev", "America/Inuvik");
            tzCanonicalIDMap.put("hntgu", "America/Tegucigalpa");
            tzCanonicalIDMap.put("ptlis", "Europe/Lisbon");
            tzCanonicalIDMap.put("Portugal", "Europe/Lisbon");
            tzCanonicalIDMap.put("gugum", "Pacific/Guam");
            tzCanonicalIDMap.put("cynic", "Asia/Nicosia");
            tzCanonicalIDMap.put("Europe/Nicosia", "Asia/Nicosia");
            tzCanonicalIDMap.put("tglfw", "Africa/Lome");
            tzCanonicalIDMap.put("auhba", "Australia/Hobart");
            tzCanonicalIDMap.put("Australia/Tasmania", "Australia/Hobart");
            tzCanonicalIDMap.put("gldkshvn", "America/Danmarkshavn");
            tzCanonicalIDMap.put("sgsin", "Asia/Singapore");
            tzCanonicalIDMap.put("Singapore", "Asia/Singapore");
            tzCanonicalIDMap.put("usphx", "America/Phoenix");
            tzCanonicalIDMap.put("US/Arizona", "America/Phoenix");
            tzCanonicalIDMap.put("szqmn", "Africa/Mbabane");
            tzCanonicalIDMap.put("mkskp", "Europe/Skopje");
            tzCanonicalIDMap.put("pgraw", "Pacific/Bougainville");
            tzCanonicalIDMap.put("rubax", "Asia/Barnaul");
            tzCanonicalIDMap.put("pkkhi", "Asia/Karachi");
            tzCanonicalIDMap.put("umjon", "Pacific/Johnston");
            tzCanonicalIDMap.put("ssjub", "Africa/Juba");
            tzCanonicalIDMap.put("glthu", "America/Thule");
            tzCanonicalIDMap.put("iodga", "Indian/Chagos");
            tzCanonicalIDMap.put("brfen", "America/Noronha");
            tzCanonicalIDMap.put("Brazil/DeNoronha", "America/Noronha");
            tzCanonicalIDMap.put("glgoh", "America/Godthab");
            tzCanonicalIDMap.put("jmkin", "America/Jamaica");
            tzCanonicalIDMap.put("Jamaica", "America/Jamaica");
            tzCanonicalIDMap.put("aumel", "Australia/Melbourne");
            tzCanonicalIDMap.put("Australia/Victoria", "Australia/Melbourne");
            tzCanonicalIDMap.put("auldh", "Australia/Lord_Howe");
            tzCanonicalIDMap.put("Australia/LHI", "Australia/Lord_Howe");
            tzCanonicalIDMap.put("bqkra", "America/Kralendijk");
            tzCanonicalIDMap.put("somgq", "Africa/Mogadishu");
            tzCanonicalIDMap.put("mrnkc", "Africa/Nouakchott");
            tzCanonicalIDMap.put("capnt", "America/Pangnirtung");
            tzCanonicalIDMap.put("auldc", "Australia/Lindeman");
            tzCanonicalIDMap.put("azbak", "Asia/Baku");
            tzCanonicalIDMap.put("cnurc", "Asia/Urumqi");
            tzCanonicalIDMap.put("Asia/Kashgar", "Asia/Urumqi");
            tzCanonicalIDMap.put("shshn", "Atlantic/St_Helena");
            tzCanonicalIDMap.put("gtgua", "America/Guatemala");
            tzCanonicalIDMap.put("nglos", "Africa/Lagos");
            tzCanonicalIDMap.put("rukra", "Asia/Krasnoyarsk");
            tzCanonicalIDMap.put("pfgmr", "Pacific/Gambier");
            tzCanonicalIDMap.put("yeade", "Asia/Aden");
            tzCanonicalIDMap.put("caybx", "America/Blanc-Sablon");
            tzCanonicalIDMap.put("cahal", "America/Halifax");
            tzCanonicalIDMap.put("Canada/Atlantic", "America/Halifax");
            tzCanonicalIDMap.put("caycb", "America/Cambridge_Bay");
            tzCanonicalIDMap.put("cfbgf", "Africa/Bangui");
            tzCanonicalIDMap.put("thbkk", "Asia/Bangkok");
            tzCanonicalIDMap.put("lccas", "America/St_Lucia");
            tzCanonicalIDMap.put("usanc", "America/Anchorage");
            tzCanonicalIDMap.put("US/Alaska", "America/Anchorage");
            tzCanonicalIDMap.put("rsbeg", "Europe/Belgrade");
            tzCanonicalIDMap.put("aqdav", "Antarctica/Davis");
            tzCanonicalIDMap.put("uslax", "America/Los_Angeles");
            tzCanonicalIDMap.put("US/Pacific", "America/Los_Angeles");
            tzCanonicalIDMap.put("US/Pacific-New", "America/Los_Angeles");
            tzCanonicalIDMap.put("mxmzt", "America/Mazatlan");
            tzCanonicalIDMap.put("Mexico/BajaSur", "America/Mazatlan");
            tzCanonicalIDMap.put("tkfko", "Pacific/Fakaofo");
            tzCanonicalIDMap.put("unk", "Etc/Unknown");
            tzCanonicalIDMap.put("usboi", "America/Boise");
            tzCanonicalIDMap.put("tcgdt", "America/Grand_Turk");
            tzCanonicalIDMap.put("gygeo", "America/Guyana");
            tzCanonicalIDMap.put("dzalg", "Africa/Algiers");
            tzCanonicalIDMap.put("usmoc", "America/Kentucky/Monticello");
            tzCanonicalIDMap.put("altia", "Europe/Tirane");
            tzCanonicalIDMap.put("jesth", "Europe/Jersey");
            tzCanonicalIDMap.put("nlams", "Europe/Amsterdam");
            tzCanonicalIDMap.put("mmrgn", "Asia/Rangoon");
            tzCanonicalIDMap.put("Asia/Yangon", "Asia/Rangoon");
            tzCanonicalIDMap.put("kygec", "America/Cayman");
            tzCanonicalIDMap.put("djjib", "Africa/Djibouti");
            tzCanonicalIDMap.put("bbbgi", "America/Barbados");
            tzCanonicalIDMap.put("zmlun", "Africa/Lusaka");
            tzCanonicalIDMap.put("esceu", "Africa/Ceuta");
            tzCanonicalIDMap.put("mxchi", "America/Chihuahua");
            tzCanonicalIDMap.put("nzcht", "Pacific/Chatham");
            tzCanonicalIDMap.put("NZ-CHAT", "Pacific/Chatham");
            tzCanonicalIDMap.put("tvfun", "Pacific/Funafuti");
            tzCanonicalIDMap.put("slfna", "Africa/Freetown");
            tzCanonicalIDMap.put("usmnm", "America/Menominee");
            tzCanonicalIDMap.put("inccu", "Asia/Calcutta");
            tzCanonicalIDMap.put("Asia/Kolkata", "Asia/Calcutta");
            tzCanonicalIDMap.put("plwaw", "Europe/Warsaw");
            tzCanonicalIDMap.put("Poland", "Europe/Warsaw");
            tzCanonicalIDMap.put("aqsyw", "Antarctica/Syowa");
            tzCanonicalIDMap.put("arbue", "America/Buenos_Aires");
            tzCanonicalIDMap.put("America/Argentina/Buenos_Aires", "America/Buenos_Aires");
            tzCanonicalIDMap.put("mykul", "Asia/Kuala_Lumpur");
            tzCanonicalIDMap.put("rusred", "Asia/Srednekolymsk");
            tzCanonicalIDMap.put("mxtij", "America/Tijuana");
            tzCanonicalIDMap.put("America/Ensenada", "America/Tijuana");
            tzCanonicalIDMap.put("Mexico/BajaNorte", "America/Tijuana");
            tzCanonicalIDMap.put("rukvx", "Europe/Kirov");
            tzCanonicalIDMap.put("hrzag", "Europe/Zagreb");
            tzCanonicalIDMap.put("itrom", "Europe/Rome");
            tzCanonicalIDMap.put("papty", "America/Panama");
            tzCanonicalIDMap.put("gqssg", "Africa/Malabo");
            tzCanonicalIDMap.put("cuhav", "America/Havana");
            tzCanonicalIDMap.put("Cuba", "America/Havana");
            tzCanonicalIDMap.put("mnuln", "Asia/Ulaanbaatar");
            tzCanonicalIDMap.put("Asia/Ulan_Bator", "Asia/Ulaanbaatar");
            tzCanonicalIDMap.put("brpvh", "America/Porto_Velho");
            tzCanonicalIDMap.put("gblon", "Europe/London");
            tzCanonicalIDMap.put("Europe/Belfast", "Europe/London");
            tzCanonicalIDMap.put("GB", "Europe/London");
            tzCanonicalIDMap.put("GB-Eire", "Europe/London");
            tzCanonicalIDMap.put("gwoxb", "Africa/Bissau");
            tzCanonicalIDMap.put("kipho", "Pacific/Enderbury");
            tzCanonicalIDMap.put("ttpos", "America/Port_of_Spain");
            tzCanonicalIDMap.put("ommct", "Asia/Muscat");
            tzCanonicalIDMap.put("lrmlw", "Africa/Monrovia");
            tzCanonicalIDMap.put("imdgs", "Europe/Isle_of_Man");
            tzCanonicalIDMap.put("npktm", "Asia/Katmandu");
            tzCanonicalIDMap.put("Asia/Kathmandu", "Asia/Katmandu");
            tzCanonicalIDMap.put("robuh", "Europe/Bucharest");
            tzCanonicalIDMap.put("rukuf", "Europe/Samara");
            tzCanonicalIDMap.put("muplu", "Indian/Mauritius");
            tzCanonicalIDMap.put("aubne", "Australia/Brisbane");
            tzCanonicalIDMap.put("Australia/Queensland", "Australia/Brisbane");
            tzCanonicalIDMap.put("mqfdf", "America/Martinique");
            tzCanonicalIDMap.put("aueuc", "Australia/Eucla");
            tzCanonicalIDMap.put("mlbko", "Africa/Bamako");
            tzCanonicalIDMap.put("Africa/Timbuktu", "Africa/Bamako");
            tzCanonicalIDMap.put("audrw", "Australia/Darwin");
            tzCanonicalIDMap.put("Australia/North", "Australia/Darwin");
            tzCanonicalIDMap.put("asppg", "Pacific/Pago_Pago");
            tzCanonicalIDMap.put("Pacific/Samoa", "Pacific/Pago_Pago");
            tzCanonicalIDMap.put("US/Samoa", "Pacific/Pago_Pago");
            tzCanonicalIDMap.put("lkcmb", "Asia/Colombo");
            tzCanonicalIDMap.put("lvrix", "Europe/Riga");
            tzCanonicalIDMap.put("lbbey", "Asia/Beirut");
            tzCanonicalIDMap.put("sdkrt", "Africa/Khartoum");
            tzCanonicalIDMap.put("aganu", "America/Antigua");
            tzCanonicalIDMap.put("frpar", "Europe/Paris");
            tzCanonicalIDMap.put("cmdla", "Africa/Douala");
            tzCanonicalIDMap.put("kwkwi", "Asia/Kuwait");
            tzCanonicalIDMap.put("jptyo", "Asia/Tokyo");
            tzCanonicalIDMap.put("Japan", "Asia/Tokyo");
            tzCanonicalIDMap.put("kitrw", "Pacific/Tarawa");
            tzCanonicalIDMap.put("cayyn", "America/Swift_Current");
            tzCanonicalIDMap.put("sttms", "Africa/Sao_Tome");
            tzCanonicalIDMap.put("metgd", "Europe/Podgorica");
            tzCanonicalIDMap.put("bddac", "Asia/Dhaka");
            tzCanonicalIDMap.put("Asia/Dacca", "Asia/Dhaka");
            tzCanonicalIDMap.put("cator", "America/Toronto");
            tzCanonicalIDMap.put("Canada/Eastern", "America/Toronto");
            tzCanonicalIDMap.put("cgbzv", "Africa/Brazzaville");
            tzCanonicalIDMap.put("tldil", "Asia/Dili");
            tzCanonicalIDMap.put("jeruslm", "Asia/Jerusalem");
            tzCanonicalIDMap.put("Asia/Tel_Aviv", "Asia/Jerusalem");
            tzCanonicalIDMap.put("Israel", "Asia/Jerusalem");
            tzCanonicalIDMap.put("gsgrv", "Atlantic/South_Georgia");
            tzCanonicalIDMap.put("usyak", "America/Yakutat");
            tzCanonicalIDMap.put("brbvb", "America/Boa_Vista");
            tzCanonicalIDMap.put("utc", "Etc/UTC");
            tzCanonicalIDMap.put("Etc/UCT", "Etc/UTC");
            tzCanonicalIDMap.put("Etc/Universal", "Etc/UTC");
            tzCanonicalIDMap.put("Etc/Zulu", "Etc/UTC");
            tzCanonicalIDMap.put("UCT", "Etc/UTC");
            tzCanonicalIDMap.put("UTC", "Etc/UTC");
            tzCanonicalIDMap.put("Universal", "Etc/UTC");
            tzCanonicalIDMap.put("Zulu", "Etc/UTC");
            tzCanonicalIDMap.put("cdfih", "Africa/Kinshasa");
            tzCanonicalIDMap.put("veccs", "America/Caracas");
            tzCanonicalIDMap.put("bmbda", "Atlantic/Bermuda");
            tzCanonicalIDMap.put("dmdom", "America/Dominica");
            tzCanonicalIDMap.put("mtmla", "Europe/Malta");
            tzCanonicalIDMap.put("idjkt", "Asia/Jakarta");
            tzCanonicalIDMap.put("gaza", "Asia/Gaza");
            tzCanonicalIDMap.put("mwblz", "Africa/Blantyre");
            tzCanonicalIDMap.put("msmni", "America/Montserrat");
            tzCanonicalIDMap.put("pyasu", "America/Asuncion");
            tzCanonicalIDMap.put("khpnh", "Asia/Phnom_Penh");
            tzCanonicalIDMap.put("aubhq", "Australia/Broken_Hill");
            tzCanonicalIDMap.put("Australia/Yancowinna", "Australia/Broken_Hill");
            tzCanonicalIDMap.put("artuc", "America/Argentina/Tucuman");
            tzCanonicalIDMap.put("cayxy", "America/Whitehorse");
            tzCanonicalIDMap.put("Canada/Yukon", "America/Whitehorse");
            tzCanonicalIDMap.put("lytip", "Africa/Tripoli");
            tzCanonicalIDMap.put("Libya", "Africa/Tripoli");
            tzCanonicalIDMap.put("basjj", "Europe/Sarajevo");
            tzCanonicalIDMap.put("knbas", "America/St_Kitts");
            tzCanonicalIDMap.put("smsai", "Europe/San_Marino");
            tzCanonicalIDMap.put("cnsha", "Asia/Shanghai");
            tzCanonicalIDMap.put("Asia/Chongqing", "Asia/Shanghai");
            tzCanonicalIDMap.put("Asia/Chungking", "Asia/Shanghai");
            tzCanonicalIDMap.put("Asia/Harbin", "Asia/Shanghai");
            tzCanonicalIDMap.put("PRC", "Asia/Shanghai");
            tzCanonicalIDMap.put("arctc", "America/Catamarca");
            tzCanonicalIDMap.put("America/Argentina/Catamarca", "America/Catamarca");
            tzCanonicalIDMap.put("America/Argentina/ComodRivadavia", "America/Catamarca");
            tzCanonicalIDMap.put("lavte", "Asia/Vientiane");
            tzCanonicalIDMap.put("fmpni", "Pacific/Ponape");
            tzCanonicalIDMap.put("Pacific/Pohnpei", "Pacific/Ponape");
            tzCanonicalIDMap.put("tzdar", "Africa/Dar_es_Salaam");
            tzCanonicalIDMap.put("ruvog", "Europe/Volgograd");
            tzCanonicalIDMap.put("ghacc", "Africa/Accra");
            tzCanonicalIDMap.put("deber", "Europe/Berlin");
            tzCanonicalIDMap.put("cyfmg", "Asia/Famagusta");
            tzCanonicalIDMap.put("bfoua", "Africa/Ouagadougou");
            tzCanonicalIDMap.put("ruuly", "Europe/Ulyanovsk");
            tzCanonicalIDMap.put("prsju", "America/Puerto_Rico");
            tzCanonicalIDMap.put("auper", "Australia/Perth");
            tzCanonicalIDMap.put("Australia/West", "Australia/Perth");
            tzCanonicalIDMap.put("cayzf", "America/Yellowknife");
            tzCanonicalIDMap.put("mnhvd", "Asia/Hovd");
            tzCanonicalIDMap.put("sydam", "Asia/Damascus");
            tzCanonicalIDMap.put("ecgye", "America/Guayaquil");
            tzCanonicalIDMap.put("eetll", "Europe/Tallinn");
            tzCanonicalIDMap.put("braux", "America/Araguaina");
            tzCanonicalIDMap.put("nrinu", "Pacific/Nauru");
            tzCanonicalIDMap.put("ugkla", "Africa/Kampala");
            tzCanonicalIDMap.put("zajnb", "Africa/Johannesburg");
            tzCanonicalIDMap.put("cayzs", "America/Coral_Harbour");
            tzCanonicalIDMap.put("America/Atikokan", "America/Coral_Harbour");
            tzCanonicalIDMap.put("mxmam", "America/Matamoros");
            tzCanonicalIDMap.put("totbu", "Pacific/Tongatapu");
            tzCanonicalIDMap.put("htpap", "America/Port-au-Prince");
            tzCanonicalIDMap.put("uslui", "America/Louisville");
            tzCanonicalIDMap.put("America/Kentucky/Louisville", "America/Louisville");
            tzCanonicalIDMap.put("dkcph", "Europe/Copenhagen");
            tzCanonicalIDMap.put("usden", "America/Denver");
            tzCanonicalIDMap.put("America/Shiprock", "America/Denver");
            tzCanonicalIDMap.put("Navajo", "America/Denver");
            tzCanonicalIDMap.put("US/Mountain", "America/Denver");
            tzCanonicalIDMap.put("cccck", "Indian/Cocos");
            tzCanonicalIDMap.put("eheai", "Africa/El_Aaiun");
            tzCanonicalIDMap.put("hkhkg", "Asia/Hong_Kong");
            tzCanonicalIDMap.put("Hongkong", "Asia/Hong_Kong");
            tzCanonicalIDMap.put("usdet", "America/Detroit");
            tzCanonicalIDMap.put("US/Michigan", "America/Detroit");
            tzCanonicalIDMap.put("ptpdl", "Atlantic/Azores");
            tzCanonicalIDMap.put("sndkr", "Africa/Dakar");
            tzCanonicalIDMap.put("ustel", "America/Indiana/Tell_City");
            tzCanonicalIDMap.put("pfnhv", "Pacific/Marquesas");
            tzCanonicalIDMap.put("pelim", "America/Lima");
            tzCanonicalIDMap.put("silju", "Europe/Ljubljana");
            tzCanonicalIDMap.put("usind", "America/Indianapolis");
            tzCanonicalIDMap.put("America/Fort_Wayne", "America/Indianapolis");
            tzCanonicalIDMap.put("America/Indiana/Indianapolis", "America/Indianapolis");
            tzCanonicalIDMap.put("US/East-Indiana", "America/Indianapolis");
            tzCanonicalIDMap.put("bzbze", "America/Belize");
            tzCanonicalIDMap.put("eslpa", "Atlantic/Canary");
            tzCanonicalIDMap.put("brssa", "America/Bahia");
            tzCanonicalIDMap.put("aedxb", "Asia/Dubai");
            tzCanonicalIDMap.put("aqrot", "Antarctica/Rothera");
            tzCanonicalIDMap.put("vavat", "Europe/Vatican");
            tzCanonicalIDMap.put("arush", "America/Argentina/Ushuaia");
            tzCanonicalIDMap.put("afkbl", "Asia/Kabul");
            tzCanonicalIDMap.put("arcor", "America/Cordoba");
            tzCanonicalIDMap.put("America/Argentina/Cordoba", "America/Cordoba");
            tzCanonicalIDMap.put("America/Rosario", "America/Cordoba");
            tzCanonicalIDMap.put("vcsvd", "America/St_Vincent");
            tzCanonicalIDMap.put("usnyc", "America/New_York");
            tzCanonicalIDMap.put("US/Eastern", "America/New_York");
            tzCanonicalIDMap.put("fkpsy", "Atlantic/Stanley");
            tzCanonicalIDMap.put("usndnsl", "America/North_Dakota/New_Salem");
            tzCanonicalIDMap.put("uswlz", "America/Indiana/Winamac");
            tzCanonicalIDMap.put("gigib", "Europe/Gibraltar");
            tzCanonicalIDMap.put("brstm", "America/Santarem");
            tzCanonicalIDMap.put("bymsq", "Europe/Minsk");
            tzCanonicalIDMap.put("aqplm", "Antarctica/Palmer");
            tzCanonicalIDMap.put("uaiev", "Europe/Kiev");
            tzCanonicalIDMap.put("kmyva", "Indian/Comoro");
            tzCanonicalIDMap.put("rutof", "Asia/Tomsk");
            tzCanonicalIDMap.put("egcai", "Africa/Cairo");
            tzCanonicalIDMap.put("Egypt", "Africa/Cairo");
            tzCanonicalIDMap.put("mxoji", "America/Ojinaga");
            tzCanonicalIDMap.put("dosdq", "America/Santo_Domingo");
            tzCanonicalIDMap.put("sxphi", "America/Lower_Princes");
            tzCanonicalIDMap.put("kicxi", "Pacific/Kiritimati");
            tzCanonicalIDMap.put("bolpb", "America/La_Paz");
            tzCanonicalIDMap.put("mvmle", "Indian/Maldives");
            tzCanonicalIDMap.put("mxmex", "America/Mexico_City");
            tzCanonicalIDMap.put("Mexico/General", "America/Mexico_City");
            tzCanonicalIDMap.put("ushnl", "Pacific/Honolulu");
            tzCanonicalIDMap.put("US/Hawaii", "Pacific/Honolulu");
            tzCanonicalIDMap.put("casjf", "America/St_Johns");
            tzCanonicalIDMap.put("Canada/Newfoundland", "America/St_Johns");
            tzCanonicalIDMap.put("nzakl", "Pacific/Auckland");
            tzCanonicalIDMap.put("Antarctica/South_Pole", "Pacific/Auckland");
            tzCanonicalIDMap.put("NZ", "Pacific/Auckland");
            tzCanonicalIDMap.put("mst7mdt", "MST7MDT");
            tzCanonicalIDMap.put("mncoq", "Asia/Choibalsan");
            tzCanonicalIDMap.put("ptfnc", "Atlantic/Madeira");
            tzCanonicalIDMap.put("auadl", "Australia/Adelaide");
            tzCanonicalIDMap.put("Australia/South", "Australia/Adelaide");
            tzCanonicalIDMap.put("armdz", "America/Mendoza");
            tzCanonicalIDMap.put("America/Argentina/Mendoza", "America/Mendoza");
            tzCanonicalIDMap.put("pmmqc", "America/Miquelon");
            tzCanonicalIDMap.put("ruchita", "Asia/Chita");
            tzCanonicalIDMap.put("ytmam", "Indian/Mayotte");
            tzCanonicalIDMap.put("mxmid", "America/Merida");
            tzCanonicalIDMap.put("caiql", "America/Iqaluit");
            tzCanonicalIDMap.put("mxcun", "America/Cancun");
            tzCanonicalIDMap.put("ummdy", "Pacific/Midway");
            tzCanonicalIDMap.put("arrgl", "America/Argentina/Rio_Gallegos");
            tzCanonicalIDMap.put("utcw10", "Etc/GMT+10");
            tzCanonicalIDMap.put("HST", "Etc/GMT+10");
            tzCanonicalIDMap.put("brern", "America/Eirunepe");
            tzCanonicalIDMap.put("utcw11", "Etc/GMT+11");
            tzCanonicalIDMap.put("utcw12", "Etc/GMT+12");
            tzCanonicalIDMap.put("wfmau", "Pacific/Wallis");
            tzCanonicalIDMap.put("lsmsu", "Africa/Maseru");
            tzCanonicalIDMap.put("brmao", "America/Manaus");
            tzCanonicalIDMap.put("Brazil/West", "America/Manaus");
            tzCanonicalIDMap.put("cdfbm", "Africa/Lubumbashi");
            tzCanonicalIDMap.put("nawdh", "Africa/Windhoek");
            tzCanonicalIDMap.put("bebru", "Europe/Brussels");
            tzCanonicalIDMap.put("kzaau", "Asia/Aqtau");
            tzCanonicalIDMap.put("twtpe", "Asia/Taipei");
            tzCanonicalIDMap.put("ROC", "Asia/Taipei");
            tzCanonicalIDMap.put("utcw05", "Etc/GMT+5");
            tzCanonicalIDMap.put("EST", "Etc/GMT+5");
            tzCanonicalIDMap.put("utcw06", "Etc/GMT+6");
            tzCanonicalIDMap.put("fihel", "Europe/Helsinki");
            tzCanonicalIDMap.put("iedub", "Europe/Dublin");
            tzCanonicalIDMap.put("Eire", "Europe/Dublin");
            tzCanonicalIDMap.put("utcw07", "Etc/GMT+7");
            tzCanonicalIDMap.put("MST", "Etc/GMT+7");
            tzCanonicalIDMap.put("utcw08", "Etc/GMT+8");
            tzCanonicalIDMap.put("utcw09", "Etc/GMT+9");
            tzCanonicalIDMap.put("utcw01", "Etc/GMT+1");
            tzCanonicalIDMap.put("pgpom", "Pacific/Port_Moresby");
            tzCanonicalIDMap.put("utcw02", "Etc/GMT+2");
            tzCanonicalIDMap.put("utcw03", "Etc/GMT+3");
            tzCanonicalIDMap.put("utcw04", "Etc/GMT+4");
            tzCanonicalIDMap.put("bgsof", "Europe/Sofia");
            tzCanonicalIDMap.put("usknx", "America/Indiana/Knox");
            tzCanonicalIDMap.put("America/Knox_IN", "America/Indiana/Knox");
            tzCanonicalIDMap.put("US/Indiana-Starke", "America/Indiana/Knox");
            tzCanonicalIDMap.put("bsnas", "America/Nassau");
            tzCanonicalIDMap.put("clipc", "Pacific/Easter");
            tzCanonicalIDMap.put("Chile/EasterIsland", "Pacific/Easter");
            tzCanonicalIDMap.put("clscl", "America/Santiago");
            tzCanonicalIDMap.put("Chile/Continental", "America/Santiago");
            tzCanonicalIDMap.put("aqmaw", "Antarctica/Mawson");
            tzCanonicalIDMap.put("isrey", "Atlantic/Reykjavik");
            tzCanonicalIDMap.put("Iceland", "Atlantic/Reykjavik");
            tzCanonicalIDMap.put("cathu", "America/Thunder_Bay");
            tzCanonicalIDMap.put("fotho", "Atlantic/Faeroe");
            tzCanonicalIDMap.put("Atlantic/Faroe", "Atlantic/Faeroe");
            tzCanonicalIDMap.put("ncnou", "Pacific/Noumea");
            tzCanonicalIDMap.put("idpnk", "Asia/Pontianak");
            tzCanonicalIDMap.put("nenim", "Africa/Niamey");
            tzCanonicalIDMap.put("cagoo", "America/Goose_Bay");
            tzCanonicalIDMap.put("uauzh", "Europe/Uzhgorod");
            tzCanonicalIDMap.put("mhkwa", "Pacific/Kwajalein");
            tzCanonicalIDMap.put("Kwajalein", "Pacific/Kwajalein");
            tzCanonicalIDMap.put("livdz", "Europe/Vaduz");
            tzCanonicalIDMap.put("ruuus", "Asia/Sakhalin");
            tzCanonicalIDMap.put("brmcz", "America/Maceio");
            tzCanonicalIDMap.put("zwhre", "Africa/Harare");
            tzCanonicalIDMap.put("careb", "America/Resolute");
            tzCanonicalIDMap.put("rukhndg", "Asia/Khandyga");
            tzCanonicalIDMap.put("careg", "America/Regina");
            tzCanonicalIDMap.put("Canada/East-Saskatchewan", "America/Regina");
            tzCanonicalIDMap.put("Canada/Saskatchewan", "America/Regina");
            tzCanonicalIDMap.put("rupkc", "Asia/Kamchatka");
            tzCanonicalIDMap.put("arsla", "America/Argentina/Salta");
            tzCanonicalIDMap.put("mykch", "Asia/Kuching");
            tzCanonicalIDMap.put("tjdyu", "Asia/Dushanbe");
            tzCanonicalIDMap.put("cafne", "America/Fort_Nelson");
            tzCanonicalIDMap.put("fjsuv", "Pacific/Fiji");
            tzCanonicalIDMap.put("pfppt", "Pacific/Tahiti");
            tzCanonicalIDMap.put("bibjm", "Africa/Bujumbura");
            tzCanonicalIDMap.put("aqmcm", "Antarctica/McMurdo");
            tzCanonicalIDMap.put("usmtm", "America/Metlakatla");
            tzCanonicalIDMap.put("uzskd", "Asia/Samarkand");
            tzCanonicalIDMap.put("cacfq", "America/Creston");
            tzCanonicalIDMap.put("ecgps", "Pacific/Galapagos");
            tzCanonicalIDMap.put("ruvvo", "Asia/Vladivostok");
            tzCanonicalIDMap.put("aqtrl", "Antarctica/Troll");
            tzCanonicalIDMap.put("usjnu", "America/Juneau");
            tzCanonicalIDMap.put("cawnp", "America/Winnipeg");
            tzCanonicalIDMap.put("Canada/Central", "America/Winnipeg");
            tzCanonicalIDMap.put("kpfnj", "Asia/Pyongyang");
            tzCanonicalIDMap.put("kzkzo", "Asia/Qyzylorda");
            tzCanonicalIDMap.put("wsapw", "Pacific/Apia");
            tzCanonicalIDMap.put("uswsq", "America/Indiana/Petersburg");
            tzCanonicalIDMap.put("gncky", "Africa/Conakry");
            tzCanonicalIDMap.put("usxul", "America/North_Dakota/Beulah");
            tzCanonicalIDMap.put("rukgd", "Europe/Kaliningrad");
            tzCanonicalIDMap.put("tfpfr", "Indian/Kerguelen");
            tzCanonicalIDMap.put("utce10", "Etc/GMT-10");
            tzCanonicalIDMap.put("utce11", "Etc/GMT-11");
            tzCanonicalIDMap.put("mpspn", "Pacific/Saipan");
            tzCanonicalIDMap.put("utce12", "Etc/GMT-12");
            tzCanonicalIDMap.put("utce13", "Etc/GMT-13");
            tzCanonicalIDMap.put("arirj", "America/Argentina/La_Rioja");
            tzCanonicalIDMap.put("aumqi", "Antarctica/Macquarie");
            tzCanonicalIDMap.put("utce14", "Etc/GMT-14");
            tzCanonicalIDMap.put("ckrar", "Pacific/Rarotonga");
            tzCanonicalIDMap.put("arjuj", "America/Jujuy");
            tzCanonicalIDMap.put("America/Argentina/Jujuy", "America/Jujuy");
            tzCanonicalIDMap.put("aqvos", "Antarctica/Vostok");
            tzCanonicalIDMap.put("momfm", "Asia/Macau");
            tzCanonicalIDMap.put("Asia/Macao", "Asia/Macau");
            tzCanonicalIDMap.put("ussit", "America/Sitka");
            tzCanonicalIDMap.put("rudyr", "Asia/Anadyr");
            tzCanonicalIDMap.put("aolad", "Africa/Luanda");
            tzCanonicalIDMap.put("ruasf", "Europe/Astrakhan");
            tzCanonicalIDMap.put("adalv", "Europe/Andorra");
            tzCanonicalIDMap.put("rurtw", "Europe/Saratov");
            tzCanonicalIDMap.put("caedm", "America/Edmonton");
            tzCanonicalIDMap.put("Canada/Mountain", "America/Edmonton");
            tzCanonicalIDMap.put("utce07", "Etc/GMT-7");
            tzCanonicalIDMap.put("aukns", "Australia/Currie");
            tzCanonicalIDMap.put("utce08", "Etc/GMT-8");
            tzCanonicalIDMap.put("utce09", "Etc/GMT-9");
            tzCanonicalIDMap.put("caffs", "America/Rainy_River");
            tzCanonicalIDMap.put("awaua", "America/Aruba");
            tzCanonicalIDMap.put("utce01", "Etc/GMT-1");
            tzCanonicalIDMap.put("utce02", "Etc/GMT-2");
            tzCanonicalIDMap.put("utce03", "Etc/GMT-3");
            tzCanonicalIDMap.put("utce04", "Etc/GMT-4");
            tzCanonicalIDMap.put("utce05", "Etc/GMT-5");
            tzCanonicalIDMap.put("utce06", "Etc/GMT-6");
            tzCanonicalIDMap.put("gfcay", "America/Cayenne");
            tzCanonicalIDMap.put("irthr", "Asia/Tehran");
            tzCanonicalIDMap.put("Iran", "Asia/Tehran");
            tzCanonicalIDMap.put("qadoh", "Asia/Qatar");
            tzCanonicalIDMap.put("usoea", "America/Indiana/Vincennes");
            tzCanonicalIDMap.put("ruoms", "Asia/Omsk");
            tzCanonicalIDMap.put("uaozh", "Europe/Zaporozhye");
            tzCanonicalIDMap.put("iqbgw", "Asia/Baghdad");
            tzCanonicalIDMap.put("fimhq", "Europe/Mariehamn");
            tzCanonicalIDMap.put("caglb", "America/Glace_Bay");
            tzCanonicalIDMap.put("czprg", "Europe/Prague");
            tzCanonicalIDMap.put("kzguw", "Asia/Atyrau");
            tzCanonicalIDMap.put("ruunera", "Asia/Ust-Nera");
            tzCanonicalIDMap.put("erasm", "Africa/Asmera");
            tzCanonicalIDMap.put("Africa/Asmara", "Africa/Asmera");
            tzCanonicalIDMap.put("pst8pdt", "PST8PDT");
            tzCanonicalIDMap.put("cvrai", "Atlantic/Cape_Verde");
            tzCanonicalIDMap.put("mgtnr", "Indian/Antananarivo");
            tzCanonicalIDMap.put("atvie", "Europe/Vienna");
            tzCanonicalIDMap.put("bnbwn", "Asia/Brunei");
            tzCanonicalIDMap.put("gpbbr", "America/Guadeloupe");
            tzCanonicalIDMap.put("srpbm", "America/Paramaribo");
            tzCanonicalIDMap.put("btthi", "Asia/Thimphu");
            tzCanonicalIDMap.put("Asia/Thimbu", "Asia/Thimphu");
            tzCanonicalIDMap.put("ancur", "America/Curacao");
            tzCanonicalIDMap.put("fmtkk", "Pacific/Truk");
            tzCanonicalIDMap.put("Pacific/Chuuk", "Pacific/Truk");
            tzCanonicalIDMap.put("Pacific/Yap", "Pacific/Truk");
            tzCanonicalIDMap.put("galbv", "Africa/Libreville");
            tzCanonicalIDMap.put("crsjo", "America/Costa_Rica");
            tzCanonicalIDMap.put("cst6cdt", "CST6CDT");
            tzCanonicalIDMap.put("ausyd", "Australia/Sydney");
            tzCanonicalIDMap.put("Australia/ACT", "Australia/Sydney");
            tzCanonicalIDMap.put("Australia/Canberra", "Australia/Sydney");
            tzCanonicalIDMap.put("Australia/NSW", "Australia/Sydney");
            tzCanonicalIDMap.put("kzura", "Asia/Oral");
            tzCanonicalIDMap.put("krsel", "Asia/Seoul");
            tzCanonicalIDMap.put("ROK", "Asia/Seoul");
            tzCanonicalIDMap.put("nfnlk", "Pacific/Norfolk");
            tzCanonicalIDMap.put("rumow", "Europe/Moscow");
            tzCanonicalIDMap.put("W-SU", "Europe/Moscow");
            tzCanonicalIDMap.put("skbts", "Europe/Bratislava");
            tzCanonicalIDMap.put("gpsbh", "America/St_Barthelemy");
            tzCanonicalIDMap.put("scmaw", "Indian/Mahe");
            tzCanonicalIDMap.put("tdndj", "Africa/Ndjamena");
            tzCanonicalIDMap.put("bhbah", "Asia/Bahrain");
            tzCanonicalIDMap.put("mcmon", "Europe/Monaco");
            tzCanonicalIDMap.put("hebron", "Asia/Hebron");
            tzCanonicalIDMap.put("mxpvr", "America/Bahia_Banderas");
            tzCanonicalIDMap.put("gmbjl", "Africa/Banjul");
            tzCanonicalIDMap.put("brbel", "America/Belem");
            tzCanonicalIDMap.put("kzakx", "Asia/Aqtobe");
            tzCanonicalIDMap.put("brcgr", "America/Campo_Grande");
            tzCanonicalIDMap.put("cavan", "America/Vancouver");
            tzCanonicalIDMap.put("Canada/Pacific", "America/Vancouver");
            tzCanonicalIDMap.put("vgtov", "America/Tortola");
            tzCanonicalIDMap.put("clpuq", "America/Punta_Arenas");
            tzCanonicalIDMap.put("usndcnt", "America/North_Dakota/Center");
            tzCanonicalIDMap.put("mxstis", "America/Santa_Isabel");
            tzCanonicalIDMap.put("phmnl", "Asia/Manila");
            tzCanonicalIDMap.put("idmak", "Asia/Makassar");
            tzCanonicalIDMap.put("Asia/Ujung_Pandang", "Asia/Makassar");
            tzCanonicalIDMap.put("kenbo", "Africa/Nairobi");
            tzCanonicalIDMap.put("brcgb", "America/Cuiaba");
            tzCanonicalIDMap.put("hubud", "Europe/Budapest");
            tzCanonicalIDMap.put("amevn", "Asia/Yerevan");
            tzCanonicalIDMap.put("gdgnd", "America/Grenada");
            tzCanonicalIDMap.put("arluq", "America/Argentina/San_Luis");
            tzCanonicalIDMap.put("ruyek", "Asia/Yekaterinburg");
            tzCanonicalIDMap.put("tmasb", "Asia/Ashgabat");
            tzCanonicalIDMap.put("Asia/Ashkhabad", "Asia/Ashgabat");
            tzCanonicalIDMap.put("usaeg", "America/Indiana/Marengo");
            tzCanonicalIDMap.put("tntun", "Africa/Tunis");
            tzCanonicalIDMap.put("runoz", "Asia/Novokuznetsk");
            tzCanonicalIDMap.put("lulux", "Europe/Luxembourg");
            tzCanonicalIDMap.put("mdkiv", "Europe/Chisinau");
            tzCanonicalIDMap.put("Europe/Tiraspol", "Europe/Chisinau");
            tzCanonicalIDMap.put("trist", "Europe/Istanbul");
            tzCanonicalIDMap.put("Asia/Istanbul", "Europe/Istanbul");
            tzCanonicalIDMap.put("Turkey", "Europe/Istanbul");
            tzCanonicalIDMap.put("nuiue", "Pacific/Niue");
            tzCanonicalIDMap.put("brrec", "America/Recife");
            tzCanonicalIDMap.put("kzala", "Asia/Almaty");
            tzCanonicalIDMap.put("globy", "America/Scoresbysund");
            tzCanonicalIDMap.put("debsngn", "Europe/Busingen");
            tzCanonicalIDMap.put("uschi", "America/Chicago");
            tzCanonicalIDMap.put("US/Central", "America/Chicago");
            tzCanonicalIDMap.put("vuvli", "Pacific/Efate");
            tzCanonicalIDMap.put("brfor", "America/Fortaleza");
            tzCanonicalIDMap.put("cxxch", "Indian/Christmas");
            tzCanonicalIDMap.put("noosl", "Europe/Oslo");
            tzCanonicalIDMap.put("ciabj", "Africa/Abidjan");
            tzCanonicalIDMap.put("bwgbe", "Africa/Gaborone");
            tzCanonicalIDMap.put("mhmaj", "Pacific/Majuro");
            tzCanonicalIDMap.put("macas", "Africa/Casablanca");
            tzCanonicalIDMap.put("usadk", "America/Adak");
            tzCanonicalIDMap.put("America/Atka", "America/Adak");
            tzCanonicalIDMap.put("US/Aleutian", "America/Adak");
            tzCanonicalIDMap.put("saruh", "Asia/Riyadh");
            tzCanonicalIDMap.put("usinvev", "America/Indiana/Vevay");
            tzCanonicalIDMap.put("gggci", "Europe/Guernsey");
            tzCanonicalIDMap.put("rereu", "Indian/Reunion");
            tzCanonicalIDMap.put("getbs", "Asia/Tbilisi");
            tzCanonicalIDMap.put("umawk", "Pacific/Wake");
        }
    }

    @Override
    public LocaleProviderAdapter.Type getType() {
        return LocaleProviderAdapter.Type.CLDR;
    }

    @Override
    public String availableLanguageTags(String category) {
        return resourceNameToLocales.getOrDefault(category, "");
    }

    @Override
    public Map<String, String> getLanguageAliasMap() {
        return languageAliasMap;
    }

    @Override
    public Map<String, String> tzCanonicalIDs() {
        return TZCanonicalIDMapHolder.tzCanonicalIDMap;
    }

    public Map<Locale, String[]> parentLocales() {
        return parentLocalesMap;
    }
}