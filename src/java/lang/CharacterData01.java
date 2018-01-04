// This file was generated AUTOMATICALLY from a template file Wed Aug 22 18:58:21 PDT 2018
/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/** The CharacterData class encapsulates the large tables once found in
 *  java.lang.Character. 
 */

class CharacterData01 extends CharacterData {
    /* The character properties are currently encoded into 32 bits in the following manner:
        1 bit   mirrored property
        4 bits  directionality property
        9 bits  signed offset used for converting case
        1 bit   if 1, adding the signed offset converts the character to lowercase
        1 bit   if 1, subtracting the signed offset converts the character to uppercase
        1 bit   if 1, this character has a titlecase equivalent (possibly itself)
        3 bits  0  may not be part of an identifier
                1  ignorable control; may continue a Unicode identifier or Java identifier
                2  may continue a Java identifier but not a Unicode identifier (unused)
                3  may continue a Unicode identifier or Java identifier
                4  is a Java whitespace character
                5  may start or continue a Java identifier;
                   may continue but not start a Unicode identifier (underscores)
                6  may start or continue a Java identifier but not a Unicode identifier ($)
                7  may start or continue a Unicode identifier or Java identifier
                Thus:
                   5, 6, 7 may start a Java identifier
                   1, 2, 3, 5, 6, 7 may continue a Java identifier
                   7 may start a Unicode identifier
                   1, 3, 5, 7 may continue a Unicode identifier
                   1 is ignorable within an identifier
                   4 is Java whitespace
        2 bits  0  this character has no numeric property
                1  adding the digit offset to the character code and then
                   masking with 0x1F will produce the desired numeric value
                2  this character has a "strange" numeric value
                3  a Java supradecimal digit: adding the digit offset to the
                   character code, then masking with 0x1F, then adding 10
                   will produce the desired numeric value
        5 bits  digit offset
        5 bits  character type

        The encoding of character properties is subject to change at any time.
     */

    int getProperties(int ch) {
        char offset = (char)ch;
        int props = A[(Y[(X[offset>>5]<<4)|((offset>>1)&0xF)]<<1)|(offset&0x1)];
        return props;
    }

    int getPropertiesEx(int ch) {
        char offset = (char)ch;
        int props = B[(Y[(X[offset>>5]<<4)|((offset>>1)&0xF)]<<1)|(offset&0x1)];
        return props;
    }

    int getType(int ch) {
        int props = getProperties(ch);
        return (props & 0x1F);
    }

    boolean isOtherLowercase(int ch) {
        int props = getPropertiesEx(ch);
        return (props & 0x0001) != 0;
    }

    boolean isOtherUppercase(int ch) {
        int props = getPropertiesEx(ch);
        return (props & 0x0002) != 0;
    }
 
    boolean isOtherAlphabetic(int ch) {
        int props = getPropertiesEx(ch);
        return (props & 0x0004) != 0;
    }

    boolean isIdeographic(int ch) {
        int props = getPropertiesEx(ch);
        return (props & 0x0010) != 0;
    }

    boolean isJavaIdentifierStart(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00007000) >= 0x00005000);
    }

    boolean isJavaIdentifierPart(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00003000) != 0);
    }

    boolean isUnicodeIdentifierStart(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00007000) == 0x00007000);
    }

    boolean isUnicodeIdentifierPart(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00001000) != 0);
    }

    boolean isIdentifierIgnorable(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00007000) == 0x00001000);
    }

    int toLowerCase(int ch) {
        int mapChar = ch;
        int val = getProperties(ch);

        if ((val & 0x00020000) != 0) {
            int offset = val << 5 >> (5+18);
            mapChar = ch + offset;
        }
        return  mapChar;
    }

    int toUpperCase(int ch) {
        int mapChar = ch;
        int val = getProperties(ch);

        if ((val & 0x00010000) != 0) {
            int offset = val  << 5 >> (5+18);
            mapChar =  ch - offset;
        }
        return  mapChar;
    }

    int toTitleCase(int ch) {
        int mapChar = ch;
        int val = getProperties(ch);

        if ((val & 0x00008000) != 0) {
            // There is a titlecase equivalent.  Perform further checks:
            if ((val & 0x00010000) == 0) {
                // The character does not have an uppercase equivalent, so it must
                // already be uppercase; so add 1 to get the titlecase form.
                mapChar = ch + 1;
            }
            else if ((val & 0x00020000) == 0) {
                // The character does not have a lowercase equivalent, so it must
                // already be lowercase; so subtract 1 to get the titlecase form.
                mapChar = ch - 1;
            }
            // else {
            // The character has both an uppercase equivalent and a lowercase
            // equivalent, so it must itself be a titlecase form; return it.
            // return ch;
            //}
        }
        else if ((val & 0x00010000) != 0) {
            // This character has no titlecase equivalent but it does have an
            // uppercase equivalent, so use that (subtract the signed case offset).
            mapChar = toUpperCase(ch);
        }
        return  mapChar;
    }

    int digit(int ch, int radix) {
        int value = -1;
        if (radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX) {
            int val = getProperties(ch);
            int kind = val & 0x1F;
            if (kind == Character.DECIMAL_DIGIT_NUMBER) {
                value = ch + ((val & 0x3E0) >> 5) & 0x1F;
            }
            else if ((val & 0xC00) == 0x00000C00) {
                // Java supradecimal digit
                value = (ch + ((val & 0x3E0) >> 5) & 0x1F) + 10;
            }
        }
        return (value < radix) ? value : -1;
    }

    int getNumericValue(int ch) {
        int val = getProperties(ch);
        int retval = -1;

        switch (val & 0xC00) {
        default: // cannot occur
        case (0x00000000):         // not numeric
            retval = -1;
            break;
        case (0x00000400):              // simple numeric
            retval = ch + ((val & 0x3E0) >> 5) & 0x1F;
            break;
        case (0x00000800)      :       // "strange" numeric
            switch(ch) {
            case 0x10113: retval = 40; break;      // AEGEAN NUMBER FORTY
            case 0x10114: retval = 50; break;      // AEGEAN NUMBER FIFTY
            case 0x10115: retval = 60; break;      // AEGEAN NUMBER SIXTY
            case 0x10116: retval = 70; break;      // AEGEAN NUMBER SEVENTY
            case 0x10117: retval = 80; break;      // AEGEAN NUMBER EIGHTY
            case 0x10118: retval = 90; break;      // AEGEAN NUMBER NINETY
            case 0x10119: retval = 100; break;     // AEGEAN NUMBER ONE HUNDRED
            case 0x1011A: retval = 200; break;     // AEGEAN NUMBER TWO HUNDRED
            case 0x1011B: retval = 300; break;     // AEGEAN NUMBER THREE HUNDRED
            case 0x1011C: retval = 400; break;     // AEGEAN NUMBER FOUR HUNDRED
            case 0x1011D: retval = 500; break;     // AEGEAN NUMBER FIVE HUNDRED
            case 0x1011E: retval = 600; break;     // AEGEAN NUMBER SIX HUNDRED
            case 0x1011F: retval = 700; break;     // AEGEAN NUMBER SEVEN HUNDRED
            case 0x10120: retval = 800; break;     // AEGEAN NUMBER EIGHT HUNDRED
            case 0x10121: retval = 900; break;     // AEGEAN NUMBER NINE HUNDRED
            case 0x10122: retval = 1000; break;    // AEGEAN NUMBER ONE THOUSAND
            case 0x10123: retval = 2000; break;    // AEGEAN NUMBER TWO THOUSAND
            case 0x10124: retval = 3000; break;    // AEGEAN NUMBER THREE THOUSAND
            case 0x10125: retval = 4000; break;    // AEGEAN NUMBER FOUR THOUSAND
            case 0x10126: retval = 5000; break;    // AEGEAN NUMBER FIVE THOUSAND
            case 0x10127: retval = 6000; break;    // AEGEAN NUMBER SIX THOUSAND
            case 0x10128: retval = 7000; break;    // AEGEAN NUMBER SEVEN THOUSAND
            case 0x10129: retval = 8000; break;    // AEGEAN NUMBER EIGHT THOUSAND
            case 0x1012A: retval = 9000; break;    // AEGEAN NUMBER NINE THOUSAND
            case 0x1012B: retval = 10000; break;   // AEGEAN NUMBER TEN THOUSAND
            case 0x1012C: retval = 20000; break;   // AEGEAN NUMBER TWENTY THOUSAND
            case 0x1012D: retval = 30000; break;   // AEGEAN NUMBER THIRTY THOUSAND
            case 0x1012E: retval = 40000; break;   // AEGEAN NUMBER FORTY THOUSAND
            case 0x1012F: retval = 50000; break;   // AEGEAN NUMBER FIFTY THOUSAND
            case 0x10130: retval = 60000; break;   // AEGEAN NUMBER SIXTY THOUSAND
            case 0x10131: retval = 70000; break;   // AEGEAN NUMBER SEVENTY THOUSAND
            case 0x10132: retval = 80000; break;   // AEGEAN NUMBER EIGHTY THOUSAND
            case 0x10133: retval = 90000; break;   // AEGEAN NUMBER NINETY THOUSAND
            case 0x10144: retval = 50; break;      // GREEK ACROPHONIC ATTIC FIFTY
            case 0x10145: retval = 500; break;     // GREEK ACROPHONIC ATTIC FIVE HUNDRED
            case 0x10146: retval = 5000; break;    // GREEK ACROPHONIC ATTIC FIVE THOUSAND
            case 0x10147: retval = 50000; break;   // GREEK ACROPHONIC ATTIC FIFTY THOUSAND
            case 0x1014A: retval = 50; break;      // GREEK ACROPHONIC ATTIC FIFTY TALENTS
            case 0x1014B: retval = 100; break;     // GREEK ACROPHONIC ATTIC ONE HUNDRED TALENTS
            case 0x1014C: retval = 500; break;     // GREEK ACROPHONIC ATTIC FIVE HUNDRED TALENTS
            case 0x1014D: retval = 1000; break;    // GREEK ACROPHONIC ATTIC ONE THOUSAND TALENTS
            case 0x1014E: retval = 5000; break;    // GREEK ACROPHONIC ATTIC FIVE THOUSAND TALENTS
            case 0x10151: retval = 50; break;      // GREEK ACROPHONIC ATTIC FIFTY STATERS
            case 0x10152: retval = 100; break;     // GREEK ACROPHONIC ATTIC ONE HUNDRED STATERS
            case 0x10153: retval = 500; break;     // GREEK ACROPHONIC ATTIC FIVE HUNDRED STATERS
            case 0x10154: retval = 1000; break;    // GREEK ACROPHONIC ATTIC ONE THOUSAND STATERS
            case 0x10155: retval = 10000; break;   // GREEK ACROPHONIC ATTIC TEN THOUSAND STATERS
            case 0x10156: retval = 50000; break;   // GREEK ACROPHONIC ATTIC FIFTY THOUSAND STATERS
            case 0x10166: retval = 50; break;      // GREEK ACROPHONIC TROEZENIAN FIFTY
            case 0x10167: retval = 50; break;      // GREEK ACROPHONIC TROEZENIAN FIFTY ALTERNATE FORM
            case 0x10168: retval = 50; break;      // GREEK ACROPHONIC HERMIONIAN FIFTY
            case 0x10169: retval = 50; break;      // GREEK ACROPHONIC THESPIAN FIFTY
            case 0x1016A: retval = 100; break;     // GREEK ACROPHONIC THESPIAN ONE HUNDRED
            case 0x1016B: retval = 300; break;     // GREEK ACROPHONIC THESPIAN THREE HUNDRED
            case 0x1016C: retval = 500; break;     // GREEK ACROPHONIC EPIDAUREAN FIVE HUNDRED
            case 0x1016D: retval = 500; break;     // GREEK ACROPHONIC TROEZENIAN FIVE HUNDRED
            case 0x1016E: retval = 500; break;     // GREEK ACROPHONIC THESPIAN FIVE HUNDRED
            case 0x1016F: retval = 500; break;     // GREEK ACROPHONIC CARYSTIAN FIVE HUNDRED
            case 0x10170: retval = 500; break;     // GREEK ACROPHONIC NAXIAN FIVE HUNDRED
            case 0x10171: retval = 1000; break;    // GREEK ACROPHONIC THESPIAN ONE THOUSAND
            case 0x10172: retval = 5000; break;    // GREEK ACROPHONIC THESPIAN FIVE THOUSAND
            case 0x10174: retval = 50; break;      // GREEK ACROPHONIC STRATIAN FIFTY MNAS
            case 0x102ED: retval = 40; break;      // COPTIC EPACT NUMBER FORTY
            case 0x102EE: retval = 50; break;      // COPTIC EPACT NUMBER FIFTY
            case 0x102EF: retval = 60; break;      // COPTIC EPACT NUMBER SIXTY
            case 0x102F0: retval = 70; break;      // COPTIC EPACT NUMBER SEVENTY
            case 0x102F1: retval = 80; break;      // COPTIC EPACT NUMBER EIGHTY
            case 0x102F2: retval = 90; break;      // COPTIC EPACT NUMBER NINETY
            case 0x102F3: retval = 100; break;     // COPTIC EPACT NUMBER ONE HUNDRED
            case 0x102F4: retval = 200; break;     // COPTIC EPACT NUMBER TWO HUNDRED
            case 0x102F5: retval = 300; break;     // COPTIC EPACT NUMBER THREE HUNDRED
            case 0x102F6: retval = 400; break;     // COPTIC EPACT NUMBER FOUR HUNDRED
            case 0x102F7: retval = 500; break;     // COPTIC EPACT NUMBER FIVE HUNDRED
            case 0x102F8: retval = 600; break;     // COPTIC EPACT NUMBER SIX HUNDRED
            case 0x102F9: retval = 700; break;     // COPTIC EPACT NUMBER SEVEN HUNDRED
            case 0x102FA: retval = 800; break;     // COPTIC EPACT NUMBER EIGHT HUNDRED
            case 0x102FB: retval = 900; break;     // COPTIC EPACT NUMBER NINE HUNDRED
            case 0x10323: retval = 50; break;      // OLD ITALIC NUMERAL FIFTY
            case 0x10341: retval = 90; break;      // GOTHIC LETTER NINETY
            case 0x1034A: retval = 900; break;     // GOTHIC LETTER NINE HUNDRED
            case 0x103D5: retval = 100; break;     // OLD PERSIAN NUMBER HUNDRED
            case 0x1085D: retval = 100; break;     // IMPERIAL ARAMAIC NUMBER ONE HUNDRED
            case 0x1085E: retval = 1000; break;    // IMPERIAL ARAMAIC NUMBER ONE THOUSAND
            case 0x1085F: retval = 10000; break;   // IMPERIAL ARAMAIC NUMBER TEN THOUSAND
            case 0x108AF: retval = 100; break;     // NABATAEAN NUMBER ONE HUNDRED
            case 0x108FF: retval = 100; break;     // HATRAN NUMBER ONE HUNDRED
            case 0x10919: retval = 100; break;     // PHOENICIAN NUMBER ONE HUNDRED
            case 0x109CC: retval = 40; break;      // MEROITIC CURSIVE NUMBER FORTY
            case 0x109CD: retval = 50; break;      // MEROITIC CURSIVE NUMBER FIFTY
            case 0x109CE: retval = 60; break;      // MEROITIC CURSIVE NUMBER SIXTY
            case 0x109CF: retval = 70; break;      // MEROITIC CURSIVE NUMBER SEVENTY
            case 0x109D2: retval = 100; break;     // MEROITIC CURSIVE NUMBER ONE HUNDRED
            case 0x109D3: retval = 200; break;     // MEROITIC CURSIVE NUMBER TWO HUNDRED
            case 0x109D4: retval = 300; break;     // MEROITIC CURSIVE NUMBER THREE HUNDRED
            case 0x109D5: retval = 400; break;     // MEROITIC CURSIVE NUMBER FOUR HUNDRED
            case 0x109D6: retval = 500; break;     // MEROITIC CURSIVE NUMBER FIVE HUNDRED
            case 0x109D7: retval = 600; break;     // MEROITIC CURSIVE NUMBER SIX HUNDRED
            case 0x109D8: retval = 700; break;     // MEROITIC CURSIVE NUMBER SEVEN HUNDRED
            case 0x109D9: retval = 800; break;     // MEROITIC CURSIVE NUMBER EIGHT HUNDRED
            case 0x109DA: retval = 900; break;     // MEROITIC CURSIVE NUMBER NINE HUNDRED
            case 0x109DB: retval = 1000; break;    // MEROITIC CURSIVE NUMBER ONE THOUSAND
            case 0x109DC: retval = 2000; break;    // MEROITIC CURSIVE NUMBER TWO THOUSAND
            case 0x109DD: retval = 3000; break;    // MEROITIC CURSIVE NUMBER THREE THOUSAND
            case 0x109DE: retval = 4000; break;    // MEROITIC CURSIVE NUMBER FOUR THOUSAND
            case 0x109DF: retval = 5000; break;    // MEROITIC CURSIVE NUMBER FIVE THOUSAND
            case 0x109E0: retval = 6000; break;    // MEROITIC CURSIVE NUMBER SIX THOUSAND
            case 0x109E1: retval = 7000; break;    // MEROITIC CURSIVE NUMBER SEVEN THOUSAND
            case 0x109E2: retval = 8000; break;    // MEROITIC CURSIVE NUMBER EIGHT THOUSAND
            case 0x109E3: retval = 9000; break;    // MEROITIC CURSIVE NUMBER NINE THOUSAND
            case 0x109E4: retval = 10000; break;   // MEROITIC CURSIVE NUMBER TEN THOUSAND
            case 0x109E5: retval = 20000; break;   // MEROITIC CURSIVE NUMBER TWENTY THOUSAND
            case 0x109E6: retval = 30000; break;   // MEROITIC CURSIVE NUMBER THIRTY THOUSAND
            case 0x109E7: retval = 40000; break;   // MEROITIC CURSIVE NUMBER FORTY THOUSAND
            case 0x109E8: retval = 50000; break;   // MEROITIC CURSIVE NUMBER FIFTY THOUSAND
            case 0x109E9: retval = 60000; break;   // MEROITIC CURSIVE NUMBER SIXTY THOUSAND
            case 0x109EA: retval = 70000; break;   // MEROITIC CURSIVE NUMBER SEVENTY THOUSAND
            case 0x109EB: retval = 80000; break;   // MEROITIC CURSIVE NUMBER EIGHTY THOUSAND
            case 0x109EC: retval = 90000; break;   // MEROITIC CURSIVE NUMBER NINETY THOUSAND
            case 0x109ED: retval = 100000; break;  // MEROITIC CURSIVE NUMBER ONE HUNDRED THOUSAND
            case 0x109EE: retval = 200000; break;  // MEROITIC CURSIVE NUMBER TWO HUNDRED THOUSAND
            case 0x109EF: retval = 300000; break;  // MEROITIC CURSIVE NUMBER THREE HUNDRED THOUSAND
            case 0x109F0: retval = 400000; break;  // MEROITIC CURSIVE NUMBER FOUR HUNDRED THOUSAND
            case 0x109F1: retval = 500000; break;  // MEROITIC CURSIVE NUMBER FIVE HUNDRED THOUSAND
            case 0x109F2: retval = 600000; break;  // MEROITIC CURSIVE NUMBER SIX HUNDRED THOUSAND
            case 0x109F3: retval = 700000; break;  // MEROITIC CURSIVE NUMBER SEVEN HUNDRED THOUSAND
            case 0x109F4: retval = 800000; break;  // MEROITIC CURSIVE NUMBER EIGHT HUNDRED THOUSAND
            case 0x109F5: retval = 900000; break;  // MEROITIC CURSIVE NUMBER NINE HUNDRED THOUSAND
            case 0x10A46: retval = 100; break;     // KHAROSHTHI NUMBER ONE HUNDRED
            case 0x10A47: retval = 1000; break;    // KHAROSHTHI NUMBER ONE THOUSAND
            case 0x10A7E: retval = 50; break;      // OLD SOUTH ARABIAN NUMBER FIFTY
            case 0x10AEF: retval = 100; break;     // MANICHAEAN NUMBER ONE HUNDRED
            case 0x10B5E: retval = 100; break;     // INSCRIPTIONAL PARTHIAN NUMBER ONE HUNDRED
            case 0x10B5F: retval = 1000; break;    // INSCRIPTIONAL PARTHIAN NUMBER ONE THOUSAND
            case 0x10B7E: retval = 100; break;     // INSCRIPTIONAL PAHLAVI NUMBER ONE HUNDRED
            case 0x10B7F: retval = 1000; break;    // INSCRIPTIONAL PAHLAVI NUMBER ONE THOUSAND
            case 0x10BAF: retval = 100; break;     // PSALTER PAHLAVI NUMBER ONE HUNDRED
            case 0x10CFD: retval = 50; break;      // OLD HUNGARIAN NUMBER FIFTY
            case 0x10CFE: retval = 100; break;     // OLD HUNGARIAN NUMBER ONE HUNDRED
            case 0x10CFF: retval = 1000; break;    // OLD HUNGARIAN NUMBER ONE THOUSAND
            case 0x10E6C: retval = 40; break;      // RUMI NUMBER FORTY
            case 0x10E6D: retval = 50; break;      // RUMI NUMBER FIFTY
            case 0x10E6E: retval = 60; break;      // RUMI NUMBER SIXTY
            case 0x10E6F: retval = 70; break;      // RUMI NUMBER SEVENTY
            case 0x10E70: retval = 80; break;      // RUMI NUMBER EIGHTY
            case 0x10E71: retval = 90; break;      // RUMI NUMBER NINETY
            case 0x10E72: retval = 100; break;     // RUMI NUMBER ONE HUNDRED
            case 0x10E73: retval = 200; break;     // RUMI NUMBER TWO HUNDRED
            case 0x10E74: retval = 300; break;     // RUMI NUMBER THREE HUNDRED
            case 0x10E75: retval = 400; break;     // RUMI NUMBER FOUR HUNDRED
            case 0x10E76: retval = 500; break;     // RUMI NUMBER FIVE HUNDRED
            case 0x10E77: retval = 600; break;     // RUMI NUMBER SIX HUNDRED
            case 0x10E78: retval = 700; break;     // RUMI NUMBER SEVEN HUNDRED
            case 0x10E79: retval = 800; break;     // RUMI NUMBER EIGHT HUNDRED
            case 0x10E7A: retval = 900; break;     // RUMI NUMBER NINE HUNDRED
            case 0x1105E: retval = 40; break;      // BRAHMI NUMBER FORTY
            case 0x1105F: retval = 50; break;      // BRAHMI NUMBER FIFTY
            case 0x11060: retval = 60; break;      // BRAHMI NUMBER SIXTY
            case 0x11061: retval = 70; break;      // BRAHMI NUMBER SEVENTY
            case 0x11062: retval = 80; break;      // BRAHMI NUMBER EIGHTY
            case 0x11063: retval = 90; break;      // BRAHMI NUMBER NINETY
            case 0x11064: retval = 100; break;     // BRAHMI NUMBER ONE HUNDRED
            case 0x11065: retval = 1000; break;    // BRAHMI NUMBER ONE THOUSAND
            case 0x11C66: retval = 40; break;      // BHAIKSUKI NUMBER FORTY
            case 0x11C67: retval = 50; break;      // BHAIKSUKI NUMBER FIFTY
            case 0x11C68: retval = 60; break;      // BHAIKSUKI NUMBER SIXTY
            case 0x11C69: retval = 70; break;      // BHAIKSUKI NUMBER SEVENTY
            case 0x11C6A: retval = 80; break;      // BHAIKSUKI NUMBER EIGHTY
            case 0x11C6B: retval = 90; break;      // BHAIKSUKI NUMBER NINETY
            case 0x11C6C: retval = 100; break;     // BHAIKSUKI HUNDREDS UNIT MARK
            case 0x111ED: retval = 40; break;      // SINHALA ARCHAIC NUMBER FORTY
            case 0x111EE: retval = 50; break;      // SINHALA ARCHAIC NUMBER FIFTY
            case 0x111EF: retval = 60; break;      // SINHALA ARCHAIC NUMBER SIXTY
            case 0x111F0: retval = 70; break;      // SINHALA ARCHAIC NUMBER SEVENTY
            case 0x111F1: retval = 80; break;      // SINHALA ARCHAIC NUMBER EIGHTY
            case 0x111F2: retval = 90; break;      // SINHALA ARCHAIC NUMBER NINETY
            case 0x111F3: retval = 100; break;     // SINHALA ARCHAIC NUMBER ONE HUNDRED
            case 0x111F4: retval = 1000; break;    // SINHALA ARCHAIC NUMBER ONE THOUSAND
            case 0x118ED: retval = 40; break;      // WARANG CITI NUMBER FORTY
            case 0x118EE: retval = 50; break;      // WARANG CITI NUMBER FIFTY
            case 0x118EF: retval = 60; break;      // WARANG CITI NUMBER SIXTY
            case 0x118F0: retval = 70; break;      // WARANG CITI NUMBER SEVENTY
            case 0x118F1: retval = 80; break;      // WARANG CITI NUMBER EIGHTY
            case 0x118F2: retval = 90; break;      // WARANG CITI NUMBER NINETY
            case 0x12432: retval = 216000; break;  // CUNEIFORM NUMERIC SIGN SHAR2 TIMES GAL PLUS DISH
            case 0x12433: retval = 432000; break;  // CUNEIFORM NUMERIC SIGN SHAR2 TIMES GAL PLUS MIN
            case 0x12467: retval = 40; break;      // CUNEIFORM NUMERIC SIGN ELAMITE FORTY
            case 0x12468: retval = 50; break;      // CUNEIFORM NUMERIC SIGN ELAMITE FIFTY
            case 0x16B5C: retval = 100; break;     // PAHAWH HMONG NUMBER HUNDREDS
            case 0x16B5D: retval = 10000; break;   // PAHAWH HMONG NUMBER TEN THOUSANDS
            case 0x16B5E: retval = 1000000; break; // PAHAWH HMONG NUMBER MILLIONS
            case 0x16B5F: retval = 100000000; break;// PAHAWH HMONG NUMBER HUNDRED MILLIONS
            case 0x1D36C: retval = 40; break;      // COUNTING ROD TENS DIGIT FOUR
            case 0x1D36D: retval = 50; break;      // COUNTING ROD TENS DIGIT FIVE
            case 0x1D36E: retval = 60; break;      // COUNTING ROD TENS DIGIT SIX
            case 0x1D36F: retval = 70; break;      // COUNTING ROD TENS DIGIT SEVEN
            case 0x1D370: retval = 80; break;      // COUNTING ROD TENS DIGIT EIGHT
            case 0x1D371: retval = 90; break;      // COUNTING ROD TENS DIGIT NINE
            default: retval = -2; break;
            }
            
            break;
        case (0x00000C00):           // Java supradecimal
            retval = (ch + ((val & 0x3E0) >> 5) & 0x1F) + 10;
            break;
        }
        return retval;
    }

    boolean isWhitespace(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00007000) == 0x00004000);
    }

    byte getDirectionality(int ch) {
        int val = getProperties(ch);
        byte directionality = (byte)((val & 0x78000000) >> 27);
        if (directionality == 0xF ) {
            directionality = Character.DIRECTIONALITY_UNDEFINED;
        }
        return directionality;
    }

    boolean isMirrored(int ch) {
        int props = getProperties(ch);
        return ((props & 0x80000000) != 0);
    }

    static final CharacterData instance = new CharacterData01();
    private CharacterData01() {};

    // The following tables and code generated using:
  // java GenerateCharacter -string -plane 1 -template t:/workspace/open/make/data/characterdata/CharacterData01.java.template -spec t:/workspace/open/make/data/unicodedata/UnicodeData.txt -specialcasing t:/workspace/open/make/data/unicodedata/SpecialCasing.txt -proplist t:/workspace/open/make/data/unicodedata/PropList.txt -o t:/workspace/build/windows-x64-open/support/gensrc/java.base/java/lang/CharacterData01.java -usecharforbyte 11 4 1
  // The X table has 2048 entries for a total of 4096 bytes.

  static final char X[] = (
    "\000\001\002\003\004\004\004\005\006\007\010\011\012\013\014\015\003\003\003"+
    "\003\016\004\017\020\004\021\022\023\024\004\025\003\026\027\030\004\031\032"+
    "\033\034\004\035\004\036\003\003\003\003\004\004\004\004\004\004\004\004\004"+
    "\037\040\041\003\003\003\003\042\043\044\045\046\047\003\050\051\052\003\003"+
    "\053\054\055\056\057\060\061\062\063\003\064\065\053\066\067\070\071\072\003"+
    "\003\053\053\073\003\074\075\076\077\003\003\003\003\003\003\003\003\003\003"+
    "\003\100\003\003\003\003\003\003\003\003\003\003\003\003\101\102\103\104\105"+
    "\106\107\110\111\112\113\114\115\116\117\120\121\122\003\003\123\124\125\126"+
    "\127\130\131\132\003\003\003\003\004\133\134\003\004\135\136\003\003\003\003"+
    "\003\004\137\140\003\004\141\142\143\004\144\145\003\146\147\003\003\003\003"+
    "\003\003\003\003\003\003\003\150\151\152\003\003\003\003\003\003\003\003\153"+
    "\154\155\004\156\157\004\160\003\003\003\003\003\003\003\003\161\162\163\164"+
    "\165\166\003\003\167\170\171\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\004\004\004\004\004\004\004\004\004\004"+
    "\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\172"+
    "\003\003\003\173\174\175\176\004\004\004\004\004\004\177\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004"+
    "\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\004\200"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\004\004\004\004\004\004\004"+
    "\004\004\004\004\004\004\004\004\004\004\004\201\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\004\004\004\004"+
    "\004\004\004\004\004\004\004\004\004\004\004\004\004\160\202\203\003\003\204"+
    "\205\004\206\207\210\211\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\004\004\212\213\214"+
    "\003\003\215\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\217\216\216\216\216\216\216\216\216\216\216\216\216\216\216"+
    "\216\216\216\216\216\216\216\216\216\220\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\004\004\004\004\004\004\004\004\202\003\003\221\216\216\216\216\216"+
    "\216\216\216\216\216\216\222\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\004\004\004\223\224\225\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\226\226\226\226\226\226\226\227"+
    "\226\230\226\231\232\233\226\234\235\235\236\003\003\003\003\003\235\235\237"+
    "\240\003\003\003\003\241\242\243\244\245\246\247\250\251\252\253\254\255\241"+
    "\242\256\244\257\260\261\250\262\263\264\265\266\267\270\271\272\273\274\226"+
    "\226\226\226\226\226\226\226\226\226\226\226\226\226\226\226\275\276\275\277"+
    "\300\301\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\302\303\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\053\053\053\053\053\053"+
    "\304\003\305\306\307\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\310\311\312\313\314\315\003\316\003\003\003\003\003\003\003"+
    "\003\235\317\235\235\320\321\322\323\324\325\326\327\330\331\003\332\333\334"+
    "\335\336\003\003\003\003\235\235\235\235\235\235\235\337\235\235\235\235\235"+
    "\235\235\235\235\235\235\235\235\235\235\235\235\235\235\235\235\235\340\341"+
    "\235\235\235\320\235\235\340\003\317\235\342\235\343\344\003\003\317\345\346"+
    "\347\350\003\351\352\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\003\003\003").toCharArray();

  // The Y table has 3760 entries for a total of 7520 bytes.

  static final char Y[] = (
    "\000\000\000\000\000\000\001\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\002\000\000\000\000\000\000\000\000\000\002\000\001\000\000\000\000\000\000"+
    "\000\003\000\000\000\000\000\000\000\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\002\003"+
    "\003\004\005\003\006\007\007\007\007\010\011\012\012\012\012\012\012\012\012"+
    "\012\012\012\012\012\012\012\012\003\013\014\014\014\014\015\016\015\015\017"+
    "\015\015\020\021\015\015\022\023\024\025\026\027\030\031\015\015\015\015\015"+
    "\015\032\033\034\035\036\036\036\036\036\036\036\036\037\040\041\036\036\036"+
    "\036\036\036\003\003\042\003\003\003\003\003\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\014\014\014\014\014\014\014\014\014"+
    "\014\014\014\014\014\014\014\014\014\014\014\014\014\043\003\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\002\003\000\000\000\000\000\000\000"+
    "\000\002\003\003\003\003\003\003\003\044\045\045\045\045\046\047\050\050\050"+
    "\050\050\050\050\003\003\051\052\003\003\003\003\001\000\000\000\000\000\000"+
    "\000\000\000\053\000\000\000\000\054\003\003\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\055\055\056\003\003\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\057\000\000\003\003\000\000"+
    "\000\000\060\061\062\003\003\003\003\003\063\063\063\063\063\063\063\063\063"+
    "\063\063\063\063\063\063\063\063\063\063\063\064\064\064\064\064\064\064\064"+
    "\064\064\064\064\064\064\064\064\064\064\064\064\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\003\065\065"+
    "\065\065\065\003\003\003\063\063\063\063\063\063\063\063\063\063\063\063\063"+
    "\063\063\063\063\063\003\003\064\064\064\064\064\064\064\064\064\064\064\064"+
    "\064\064\064\064\064\064\003\003\000\000\000\000\003\003\003\003\000\000\000"+
    "\000\000\000\000\000\000\000\003\003\003\003\003\057\003\003\003\003\003\003"+
    "\003\003\000\000\000\000\000\000\000\000\000\000\000\002\003\003\003\003\000"+
    "\000\000\000\000\000\000\000\000\000\000\003\003\003\003\003\000\000\000\000"+
    "\003\003\003\003\003\003\003\003\003\003\003\003\066\066\066\003\067\066\066"+
    "\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066"+
    "\066\070\067\003\067\070\066\066\066\066\066\066\066\066\066\066\066\071\072"+
    "\073\074\075\066\066\066\066\066\066\066\066\066\066\066\076\077\100\100\101"+
    "\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\067\003\003\003"+
    "\102\103\104\105\106\003\003\003\003\003\003\003\003\066\066\066\066\066\066"+
    "\066\066\066\067\066\003\003\107\110\111\066\066\066\066\066\066\066\066\066"+
    "\066\066\112\113\100\003\114\066\066\066\066\066\066\066\066\066\066\066\066"+
    "\066\003\003\071\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066"+
    "\066\066\066\066\066\066\066\066\066\066\066\066\066\003\003\075\066\115\115"+
    "\115\115\115\116\075\075\003\075\075\075\075\075\075\075\075\075\075\075\075"+
    "\075\075\075\075\075\075\075\075\075\075\075\117\055\120\056\003\003\055\055"+
    "\066\066\070\066\070\066\066\066\066\066\066\066\066\066\066\066\066\066\003"+
    "\003\121\122\003\123\115\115\124\075\003\003\003\003\125\125\125\125\126\003"+
    "\003\003\066\066\066\066\066\066\066\066\066\066\066\066\066\066\127\130\066"+
    "\066\066\066\066\066\066\066\066\066\066\066\066\066\127\101\066\066\066\066"+
    "\131\066\066\066\066\066\066\066\066\066\066\066\066\066\132\122\003\133\105"+
    "\106\125\125\125\126\003\003\003\003\066\066\066\066\066\066\066\066\066\066"+
    "\066\003\114\134\134\134\066\066\066\066\066\066\066\066\066\066\066\003\072"+
    "\072\135\075\066\066\066\066\066\066\066\066\066\067\003\003\072\072\135\075"+
    "\066\066\066\066\066\066\066\066\066\003\003\003\071\125\126\003\003\003\003"+
    "\003\136\137\140\106\003\003\003\003\003\003\003\003\066\066\066\066\067\003"+
    "\003\003\003\003\003\003\003\003\003\003\141\141\141\141\141\141\141\141\141"+
    "\141\141\141\141\141\141\141\141\141\141\141\141\141\141\141\141\142\003\003"+
    "\003\003\003\003\143\143\143\143\143\143\143\143\143\143\143\143\143\143\143"+
    "\143\143\143\143\143\143\143\143\143\143\144\003\003\003\145\146\075\147\147"+
    "\147\147\147\150\151\151\151\151\151\151\151\151\151\152\153\154\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\055\055\055\055\055\055\055\155\156\156\156\003\003\157\157"+
    "\157\157\157\160\034\034\034\034\161\161\161\161\161\003\003\003\003\003\003"+
    "\003\123\121\154\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\162\153\055\163\164\155\165\156\156\003\003\003"+
    "\003\003\003\003\000\000\000\000\000\000\000\000\000\000\000\000\002\003\003"+
    "\003\166\166\166\166\166\003\003\003\055\167\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\170\055\055\153\055\055\171\122\172\172"+
    "\172\172\172\156\156\003\003\003\003\003\003\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\173\156\002\003\003\003\003\055\154\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\174\162\055\055\055\055\163\175\000\176\156\156\121\155\003\166"+
    "\166\166\166\166\176\176\156\177\200\200\200\200\201\202\012\012\012\203\003"+
    "\003\003\003\003\000\000\000\000\000\000\000\000\000\001\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\162\153\055\162\204\205\156\156\156\056\000\000"+
    "\000\002\002\000\000\001\000\000\000\000\000\000\000\001\000\000\000\000\176"+
    "\003\003\003\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\170\162\153\055\055\171\122\003\003\166\166\166"+
    "\166\166\003\003\003\055\162\001\000\000\000\002\001\002\001\000\000\000\000"+
    "\000\000\000\000\000\000\002\000\000\000\002\000\001\000\000\003\206\162\163"+
    "\162\207\210\207\210\211\003\002\003\003\210\003\003\001\000\000\162\003\121"+
    "\121\121\122\003\121\121\122\003\003\003\003\003\000\000\000\000\000\000\000"+
    "\000\000\000\174\162\055\055\055\055\162\205\163\206\000\176\156\156\166\166"+
    "\166\166\166\057\057\003\000\000\000\000\000\000\000\000\162\153\055\055\163"+
    "\163\162\153\163\121\000\212\003\003\003\003\166\166\166\166\166\003\003\003"+
    "\000\000\000\000\000\000\000\174\162\055\055\003\162\162\055\164\155\156\156"+
    "\156\156\156\156\156\156\156\156\156\000\000\055\003\000\000\000\000\000\000"+
    "\000\000\162\153\055\055\055\163\153\164\213\156\002\003\003\003\003\003\166"+
    "\166\166\166\166\003\003\003\134\134\134\134\134\134\214\003\003\003\003\003"+
    "\003\003\003\003\000\000\000\000\000\170\153\162\055\055\055\215\003\003\003"+
    "\003\065\065\065\065\065\003\003\003\003\003\003\003\003\003\003\003\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\003\120\055\162\055\055\153\055"+
    "\171\003\003\166\166\166\166\166\216\156\217\220\220\220\220\220\220\220\220"+
    "\220\220\220\220\220\220\220\220\221\221\221\221\221\221\221\221\221\221\221"+
    "\221\221\221\221\221\065\065\065\065\065\201\202\012\012\203\003\003\003\003"+
    "\003\001\170\055\055\163\153\167\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\173\205\055\163\170\055\213\156\156\156\222"+
    "\003\003\003\003\170\055\055\163\153\055\000\000\000\000\003\000\000\055\055"+
    "\055\055\055\055\163\121\156\005\156\156\005\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\000\000\000\000\000\000\000\000\000\000\000\000\002"+
    "\003\003\003\000\000\000\000\002\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\174\055\055\055\056\055\055\055\223\176\156\156"+
    "\003\003\003\003\003\166\166\166\166\166\224\224\224\224\224\216\012\012\012"+
    "\203\003\156\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\003"+
    "\055\055\055\055\055\055\055\055\055\055\055\210\055\055\055\163\055\153\056"+
    "\003\003\003\003\000\000\000\002\000\001\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\170\055\055\056\003\056\055\120\055\205"+
    "\121\170\003\003\003\003\166\166\166\166\166\003\003\003\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\003\003\003\225\225\225\225\226\226\226\227"+
    "\230\230\231\232\232\232\232\233\233\234\235\236\236\236\230\237\240\241\242"+
    "\243\232\244\245\246\247\250\251\252\253\254\254\255\256\257\260\232\261\241"+
    "\241\241\241\241\241\241\262\226\226\263\156\156\005\003\003\003\003\003\000"+
    "\000\003\003\003\003\003\003\003\003\003\003\003\003\003\003\000\000\000\000"+
    "\000\000\000\002\003\003\003\003\003\003\003\003\000\000\000\002\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\002\065\065\065\065\065\003\003\156\003\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\003\121\121\155\003\003\003\003\003\000\000\000"+
    "\000\000\000\000\000\055\055\055\213\156\156\014\014\264\264\217\003\003\003"+
    "\003\003\166\166\166\166\166\265\012\012\012\001\000\000\000\000\000\000\000"+
    "\000\000\000\003\003\001\000\000\000\000\000\000\000\000\000\003\003\003\003"+
    "\003\003\003\003\000\000\002\003\003\003\003\003\174\162\162\162\162\162\162"+
    "\162\162\162\162\162\162\162\162\162\162\162\162\162\162\162\162\207\003\003"+
    "\003\003\003\003\003\123\121\266\264\264\264\264\264\264\264\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\003\267\267\267\267\267\267\267\267"+
    "\267\267\267\267\267\267\267\267\267\267\267\267\267\267\270\003\003\003\003"+
    "\003\003\003\003\003\267\267\267\267\267\267\267\267\267\270\003\003\003\003"+
    "\003\003\003\003\003\003\003\003\003\003\267\267\267\267\267\267\267\267\267"+
    "\267\267\267\267\267\267\267\267\267\267\267\267\267\003\003\000\000\000\000"+
    "\000\002\003\003\000\000\000\000\000\000\002\003\000\000\000\000\002\003\003"+
    "\003\000\000\000\000\000\003\043\213\271\271\003\003\003\003\003\003\003\003"+
    "\003\003\003\003\003\003\014\014\014\014\014\014\014\014\014\014\014\014\014"+
    "\014\014\014\014\014\014\014\014\014\014\014\014\014\014\003\003\003\003\003"+
    "\014\014\014\041\013\014\014\014\014\014\014\014\014\014\014\014\014\014\272"+
    "\215\121\014\272\273\273\274\271\271\271\275\121\121\121\276\043\121\121\121"+
    "\014\014\014\014\014\014\014\014\014\014\014\014\014\014\014\121\121\014\014"+
    "\014\014\014\014\014\014\014\014\014\014\014\041\003\003\003\003\003\003\003"+
    "\003\003\003\003\036\036\036\036\036\036\036\036\036\036\036\036\036\036\036"+
    "\036\036\121\277\003\003\003\003\003\003\003\003\003\003\003\003\003\036\036"+
    "\036\036\036\036\036\036\036\036\036\042\003\003\003\003\300\300\300\300\300"+
    "\301\012\012\012\003\003\003\003\003\003\003\302\302\302\302\302\302\302\302"+
    "\302\302\302\302\302\303\303\303\303\303\303\303\303\303\303\303\303\303\302"+
    "\302\302\302\302\302\302\302\302\302\302\302\302\303\303\303\304\303\303\303"+
    "\303\303\303\303\303\303\302\302\302\302\302\302\302\302\302\302\302\302\302"+
    "\303\303\303\303\303\303\303\303\303\303\303\303\303\305\302\003\305\306\305"+
    "\306\302\305\302\302\302\302\303\303\307\307\303\303\303\307\303\303\303\303"+
    "\303\302\302\302\302\302\302\302\302\302\302\302\302\302\303\303\303\303\303"+
    "\303\303\303\303\303\303\303\303\302\306\302\305\306\302\302\302\305\302\302"+
    "\302\305\303\303\303\303\303\303\303\303\303\303\303\303\303\302\306\302\305"+
    "\302\302\305\305\003\302\302\302\305\303\303\303\303\303\303\303\303\303\303"+
    "\303\303\303\302\302\302\302\302\302\302\302\302\302\302\302\302\303\303\303"+
    "\303\303\303\303\303\303\303\303\303\303\302\302\302\302\302\302\302\303\303"+
    "\303\303\303\303\303\303\303\302\303\303\303\303\303\303\303\303\303\303\303"+
    "\303\303\302\302\302\302\302\302\302\302\302\302\302\302\302\303\303\303\303"+
    "\303\303\303\303\303\303\303\303\303\302\302\302\302\302\302\302\302\303\303"+
    "\303\003\302\302\302\302\302\302\302\302\302\302\302\302\310\303\303\303\303"+
    "\303\303\303\303\303\303\303\303\311\303\303\303\302\302\302\302\302\302\302"+
    "\302\302\302\302\302\310\303\303\303\303\303\303\303\303\303\303\303\303\311"+
    "\303\303\303\302\302\302\302\302\302\302\302\302\302\302\302\310\303\303\303"+
    "\303\303\303\303\303\303\303\303\303\311\303\303\303\302\302\302\302\302\302"+
    "\302\302\302\302\302\302\310\303\303\303\303\303\303\303\303\303\303\303\303"+
    "\311\303\303\303\302\302\302\302\302\302\302\302\302\302\302\302\310\303\303"+
    "\303\303\303\303\303\303\303\303\303\303\311\303\303\303\312\003\313\313\313"+
    "\313\313\314\314\314\314\314\315\315\315\315\315\316\316\316\316\316\317\317"+
    "\317\317\317\121\121\121\121\121\121\121\121\121\121\121\121\121\121\121\121"+
    "\121\121\121\121\121\121\121\121\121\121\121\276\014\043\121\121\121\121\121"+
    "\121\121\121\276\014\014\014\043\014\014\014\014\014\014\014\276\320\156\156"+
    "\003\003\003\003\003\003\003\123\121\121\123\121\121\121\121\121\121\121\003"+
    "\003\003\003\003\003\003\003\055\055\055\056\055\055\055\055\055\055\055\055"+
    "\056\120\055\055\055\120\056\055\055\056\003\003\003\003\003\003\003\003\003"+
    "\003\066\066\067\102\103\103\103\103\121\121\121\122\003\003\003\003\321\321"+
    "\321\321\321\321\321\321\321\321\321\321\321\321\321\321\321\322\322\322\322"+
    "\322\322\322\322\322\322\322\322\322\322\322\322\322\121\205\121\122\003\003"+
    "\323\323\323\323\323\003\003\125\324\324\325\324\324\324\324\324\324\324\324"+
    "\324\324\324\324\324\325\326\326\325\325\324\324\324\324\326\324\324\325\325"+
    "\003\003\003\326\003\325\325\325\325\324\325\326\326\325\325\325\325\325\325"+
    "\326\326\325\324\326\324\324\324\326\324\324\325\324\326\326\324\324\324\324"+
    "\324\325\324\324\324\324\324\324\324\324\003\003\325\324\325\324\324\325\324"+
    "\324\324\324\324\324\324\324\003\003\003\003\003\003\003\003\003\003\327\003"+
    "\003\003\003\003\003\003\036\036\036\036\036\036\003\003\036\036\036\036\036"+
    "\036\036\036\036\036\036\036\036\036\036\036\036\036\003\003\003\003\003\003"+
    "\036\036\036\036\036\036\036\042\330\036\036\036\036\036\036\036\330\036\036"+
    "\036\036\036\036\036\330\036\036\036\036\036\036\036\036\036\036\036\036\036"+
    "\036\036\036\036\036\003\003\003\003\003\331\332\332\332\332\333\334\003\014"+
    "\014\014\014\014\014\014\014\014\014\014\014\014\014\014\041\335\335\335\335"+
    "\335\335\335\335\335\335\335\335\335\014\014\014\335\335\335\335\335\335\335"+
    "\335\335\335\335\335\335\036\003\003\335\335\335\335\335\335\335\335\335\335"+
    "\335\335\335\014\014\014\014\014\014\014\014\014\014\014\014\014\014\014\014"+
    "\014\041\003\003\003\003\003\003\003\003\003\003\003\003\014\014\014\014\014"+
    "\014\014\014\014\014\014\014\014\014\041\003\003\003\003\003\003\014\014\014"+
    "\014\014\014\014\014\014\014\014\014\014\014\014\014\014\014\014\014\014\014"+
    "\003\003\014\014\014\014\041\003\003\003\014\003\003\003\003\003\003\003\036"+
    "\036\036\003\003\003\003\003\003\003\003\003\003\003\003\003\036\036\036\036"+
    "\036\036\036\036\036\036\036\036\036\336\337\337\036\036\036\036\036\036\036"+
    "\036\036\036\042\003\003\003\003\003\036\036\036\036\036\036\042\003\036\036"+
    "\036\036\042\003\003\003\036\036\036\036\003\003\003\003\036\036\036\036\036"+
    "\003\003\003\036\036\036\036\003\003\003\003\036\036\036\036\036\036\036\036"+
    "\036\036\036\036\036\036\036\003\003\003\003\003\003\003\003\003\036\036\036"+
    "\036\036\036\036\036\036\036\036\036\036\036\036\042\036\036\036\036\036\036"+
    "\042\003\036\036\036\036\036\036\036\036\036\036\036\036\036\036\003\003\003"+
    "\003\003\003\003\003\003\003\036\036\036\036\036\036\036\036\036\036\036\036"+
    "\003\003\003\003\042\003\003\003\003\003\003\003\036\036\036\036\036\036\036"+
    "\036\036\036\036\042\003\003\003\003\003\003\003\003\003\003\003\003").toCharArray();

  // The A table has 448 entries for a total of 1792 bytes.

  static final int A[] = new int[448];
  static final String A_DATA =
    "\000\u7005\000\u7005\u7800\000\000\u7005\000\u7005\u7800\000\u7800\000\u7800"+
    "\000\000\030\u6800\030\000\030\u7800\000\u7800\000\000\u074B\000\u074B\000"+
    "\u074B\000\u074B\000\u046B\000\u058B\000\u080B\000\u080B\000\u080B\u7800\000"+
    "\000\034\000\034\000\034\u6800\u780A\u6800\u780A\u6800\u77EA\u6800\u744A\u6800"+
    "\u77AA\u6800\u742A\u6800\u780A\u6800\u76CA\u6800\u774A\u6800\u780A\u6800\u780A"+
    "\u6800\u766A\u6800\u752A\u6800\u750A\u6800\u74EA\u6800\u74EA\u6800\u74CA\u6800"+
    "\u74AA\u6800\u748A\u6800\u74CA\u6800\u754A\u6800\u752A\u6800\u750A\u6800\u74EA"+
    "\u6800\u74CA\u6800\u772A\u6800\u780A\u6800\u764A\u6800\u780A\u6800\u080B\u6800"+
    "\u080B\u6800\u080B\u6800\u080B\u6800\034\u6800\034\u6800\034\u6800\u06CB\u6800"+
    "\u080B\u6800\034\000\034\000\034\u7800\000\u6800\034\u7800\000\000\034\u4000"+
    "\u3006\u4000\u3006\u1800\u040B\u1800\u040B\u1800\u040B\u1800\u040B\u1800\u052B"+
    "\u1800\u064B\u1800\u080B\u1800\u080B\u1800\u080B\000\u042B\000\u048B\000\u050B"+
    "\000\u080B\000\u7005\000\u780A\000\u780A\u7800\000\u4000\u3006\u4000\u3006"+
    "\u4000\u3006\u7800\000\u7800\000\000\030\000\030\000\u760A\000\u760A\000\u76EA"+
    "\000\u740A\000\u780A\242\u7001\242\u7001\241\u7002\241\u7002\000\u3409\000"+
    "\u3409\u0800\u7005\u0800\u7005\u0800\u7005\u7800\000\u7800\000\u0800\u7005"+
    "\u7800\000\u0800\030\u0800\u052B\u0800\u052B\u0800\u052B\u0800\u05EB\u0800"+
    "\u070B\u0800\u080B\u0800\u080B\u0800\u080B\u0800\u7005\u0800\034\u0800\034"+
    "\u0800\u050B\u0800\u050B\u0800\u050B\u0800\u058B\u0800\u06AB\u7800\000\u0800"+
    "\u074B\u0800\u074B\u0800\u074B\u0800\u074B\u0800\u072B\u0800\u072B\u0800\u07AB"+
    "\u0800\u04CB\u0800\u080B\u7800\000\u0800\u04CB\u0800\u052B\u0800\u05AB\u0800"+
    "\u06CB\u0800\u080B\u0800\u056B\u0800\u066B\u0800\u078B\u0800\u080B\u7800\000"+
    "\u6800\030\u0800\u042B\u0800\u042B\u0800\u054B\u0800\u066B\u0800\u7005\u4000"+
    "\u3006\u7800\000\u4000\u3006\u4000\u3006\u4000\u3006\u4000\u3006\u7800\000"+
    "\u7800\000\u4000\u3006\u0800\u04CB\u0800\u05EB\u0800\030\u0800\030\u0800\030"+
    "\u7800\000\u0800\u7005\u0800\u048B\u0800\u080B\u0800\030\u0800\034\u0800\u7005"+
    "\u0800\u7005\u4000\u3006\u7800\000\u0800\u06CB\u6800\030\u6800\030\u0800\u05CB"+
    "\u0800\u06EB\u7800\000\u0800\u070B\u0800\u070B\u0800\u070B\u0800\u070B\u0800"+
    "\u07AB\u0902\u7001\u0902\u7001\u0902\u7001\u7800\000\u0901\u7002\u0901\u7002"+
    "\u0901\u7002\u7800\000\u0800\u04EB\u0800\u054B\u0800\u05CB\u0800\u080B\u3000"+
    "\u042B\u3000\u042B\u3000\u054B\u3000\u066B\u3000\u080B\u3000\u080B\u3000\u080B"+
    "\u7800\000\000\u3008\u4000\u3006\000\u3008\000\u7005\u4000\u3006\000\030\000"+
    "\030\000\030\u6800\u05EB\u6800\u05EB\u6800\u070B\u6800\u042B\000\u3749\000"+
    "\u3749\000\u3008\000\u3008\u4000\u3006\000\u3008\000\u3008\u4000\u3006\000"+
    "\030\000\u1010\000\u3609\000\u3609\u4000\u3006\000\u7005\000\u7005\u4000\u3006"+
    "\u4000\u3006\u4000\u3006\000\u3549\000\u3549\000\u7005\u4000\u3006\000\u7005"+
    "\000\u3008\000\u3008\000\u7005\000\u7005\000\030\u7800\000\000\u040B\000\u040B"+
    "\000\u040B\000\u040B\000\u052B\000\u064B\000\u080B\000\u080B\u7800\000\u4000"+
    "\u3006\000\u3008\u4000\u3006\u4000\u3006\u4000\u3006\000\u7005\000\u3008\u7800"+
    "\000\u7800\000\000\u3008\000\u3008\000\u3008\000\030\000\u7005\u4000\u3006"+
    "\000\030\u6800\030\u7800\000\000\u3008\u4000\u3006\000\u060B\000\u072B\000"+
    "\030\000\034\202\u7001\202\u7001\201\u7002\201\u7002\000\030\u4000\u3006\000"+
    "\u3008\000\u3006\000\u04EB\000\u04EB\000\u744A\000\u744A\000\u776A\000\u776A"+
    "\000\u776A\000\u76AA\000\u76AA\000\u76AA\000\u76AA\000\u758A\000\u758A\000"+
    "\u758A\000\u746A\000\u746A\000\u746A\000\u77EA\000\u77EA\000\u77CA\000\u77CA"+
    "\000\u77CA\000\u76AA\000\u768A\000\u768A\000\u768A\000\u780A\000\u780A\000"+
    "\u75AA\000\u75AA\000\u75AA\000\u758A\000\u752A\000\u750A\000\u750A\000\u74EA"+
    "\000\u74CA\000\u74AA\000\u74CA\000\u74CA\000\u74AA\000\u748A\000\u748A\000"+
    "\u746A\000\u746A\000\u744A\000\u742A\000\u740A\000\u770A\000\u770A\000\u770A"+
    "\000\u764A\000\u764A\000\u764A\000\u764A\000\u762A\000\u762A\000\u760A\000"+
    "\u752A\000\u752A\000\u780A\000\u776A\000\u776A\u7800\000\000\u7004\000\u7004"+
    "\u7800\000\000\u05EB\u4000\u3006\000\u7004\000\u7005\000\u7005\000\u7005\u7800"+
    "\000\u4800\u1010\u4800\u1010\000\034\000\u3008\000\u3008\000\u3008\000\u3008"+
    "\u4800\u1010\u4800\u1010\u4000\u3006\u4000\u3006\000\034\u4000\u3006\u6800"+
    "\034\000\u042B\000\u042B\000\u054B\000\u066B\000\u7001\000\u7001\000\u7002"+
    "\000\u7002\000\u7002\u7800\000\000\u7001\u7800\000\u7800\000\000\u7001\u7800"+
    "\000\000\u7002\000\u7001\000\031\000\u7002\uE800\031\000\u7001\000\u7002\u1800"+
    "\u3649\u1800\u3649\u1800\u3509\u1800\u3509\u1800\u37C9\u1800\u37C9\u1800\u3689"+
    "\u1800\u3689\u1800\u3549\u1800\u3549\000\034\000\030\u088A\u7001\u088A\u7001"+
    "\u0889\u7002\u0889\u7002\u0800\u3609\u0800\u3609\u1000\u7005\u1000\u7005\u7800"+
    "\000\u1000\u7005\u1000\u7005\u7800\000\u6800\031\u6800\031\u7800\000\u6800"+
    "\034\u1800\u040B\u1800\u07EB\u1800\u07EB\u1800\u07EB\u1800\u07EB\u6800\u06AB"+
    "\u6800\u068B\u7800\000\000\034\000\034\u6800\034\u6800\033\u6800\033\u6800"+
    "\033";

  // The B table has 448 entries for a total of 896 bytes.

  static final char B[] = (
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\004\004\004\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\004\000\004\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\004\004\004\000\000\000\000\000\000\000\000\000\000\000"+
    "\004\004\004\004\004\000\000\000\000\000\004\000\000\004\004\000\000\000\000"+
    "\000\000\004\000\000\000\000\000\000\000\000\000\000\000\000\000\000\004\000"+
    "\000\004\000\000\004\000\000\004\004\000\000\000\004\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\004\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\020\020\020\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\006\006\000\000\000\000").toCharArray();

  // In all, the character property tables require 13408 bytes.

    static {
                { // THIS CODE WAS AUTOMATICALLY CREATED BY GenerateCharacter:
            char[] data = A_DATA.toCharArray();
            assert (data.length == (448 * 2));
            int i = 0, j = 0;
            while (i < (448 * 2)) {
                int entry = data[i++] << 16;
                A[j++] = entry | data[i++];
            }
        }

    }        
}
