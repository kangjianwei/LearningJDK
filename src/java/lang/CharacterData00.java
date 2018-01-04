// This file was generated AUTOMATICALLY from a template file Wed Aug 22 18:58:20 PDT 2018
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

/** 
 * The CharacterData00 class encapsulates the large tables once found in
 * java.lang.Character
*/

class CharacterData00 extends CharacterData {
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
        int props = A[Y[X[offset>>5]|((offset>>1)&0xF)]|(offset&0x1)];
        return props;
    }

    int getPropertiesEx(int ch) {
        char offset = (char)ch;
        int props = B[Y[X[offset>>5]|((offset>>1)&0xF)]|(offset&0x1)];
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
          if ((val & 0x07FC0000) == 0x07FC0000) {
            switch(ch) {
            case 0x0130: mapChar = 0x0069; break;
            case 0x023A: mapChar = 0x2C65; break;
            case 0x023E: mapChar = 0x2C66; break;
            case 0x0412: mapChar = 0x1C80; break;
            case 0x0414: mapChar = 0x1C81; break;
            case 0x041E: mapChar = 0x1C82; break;
            case 0x0421: mapChar = 0x1C83; break;
            case 0x042A: mapChar = 0x1C86; break;
            case 0x0462: mapChar = 0x1C87; break;
            case 0x10A0: mapChar = 0x2D00; break;
            case 0x10A1: mapChar = 0x2D01; break;
            case 0x10A2: mapChar = 0x2D02; break;
            case 0x10A3: mapChar = 0x2D03; break;
            case 0x10A4: mapChar = 0x2D04; break;
            case 0x10A5: mapChar = 0x2D05; break;
            case 0x10A6: mapChar = 0x2D06; break;
            case 0x10A7: mapChar = 0x2D07; break;
            case 0x10A8: mapChar = 0x2D08; break;
            case 0x10A9: mapChar = 0x2D09; break;
            case 0x10AA: mapChar = 0x2D0A; break;
            case 0x10AB: mapChar = 0x2D0B; break;
            case 0x10AC: mapChar = 0x2D0C; break;
            case 0x10AD: mapChar = 0x2D0D; break;
            case 0x10AE: mapChar = 0x2D0E; break;
            case 0x10AF: mapChar = 0x2D0F; break;
            case 0x10B0: mapChar = 0x2D10; break;
            case 0x10B1: mapChar = 0x2D11; break;
            case 0x10B2: mapChar = 0x2D12; break;
            case 0x10B3: mapChar = 0x2D13; break;
            case 0x10B4: mapChar = 0x2D14; break;
            case 0x10B5: mapChar = 0x2D15; break;
            case 0x10B6: mapChar = 0x2D16; break;
            case 0x10B7: mapChar = 0x2D17; break;
            case 0x10B8: mapChar = 0x2D18; break;
            case 0x10B9: mapChar = 0x2D19; break;
            case 0x10BA: mapChar = 0x2D1A; break;
            case 0x10BB: mapChar = 0x2D1B; break;
            case 0x10BC: mapChar = 0x2D1C; break;
            case 0x10BD: mapChar = 0x2D1D; break;
            case 0x10BE: mapChar = 0x2D1E; break;
            case 0x10BF: mapChar = 0x2D1F; break;
            case 0x10C0: mapChar = 0x2D20; break;
            case 0x10C1: mapChar = 0x2D21; break;
            case 0x10C2: mapChar = 0x2D22; break;
            case 0x10C3: mapChar = 0x2D23; break;
            case 0x10C4: mapChar = 0x2D24; break;
            case 0x10C5: mapChar = 0x2D25; break;
            case 0x10C7: mapChar = 0x2D27; break;
            case 0x10CD: mapChar = 0x2D2D; break;
            case 0x13A0: mapChar = 0xAB70; break;
            case 0x13A1: mapChar = 0xAB71; break;
            case 0x13A2: mapChar = 0xAB72; break;
            case 0x13A3: mapChar = 0xAB73; break;
            case 0x13A4: mapChar = 0xAB74; break;
            case 0x13A5: mapChar = 0xAB75; break;
            case 0x13A6: mapChar = 0xAB76; break;
            case 0x13A7: mapChar = 0xAB77; break;
            case 0x13A8: mapChar = 0xAB78; break;
            case 0x13A9: mapChar = 0xAB79; break;
            case 0x13AA: mapChar = 0xAB7A; break;
            case 0x13AB: mapChar = 0xAB7B; break;
            case 0x13AC: mapChar = 0xAB7C; break;
            case 0x13AD: mapChar = 0xAB7D; break;
            case 0x13AE: mapChar = 0xAB7E; break;
            case 0x13AF: mapChar = 0xAB7F; break;
            case 0x13B0: mapChar = 0xAB80; break;
            case 0x13B1: mapChar = 0xAB81; break;
            case 0x13B2: mapChar = 0xAB82; break;
            case 0x13B3: mapChar = 0xAB83; break;
            case 0x13B4: mapChar = 0xAB84; break;
            case 0x13B5: mapChar = 0xAB85; break;
            case 0x13B6: mapChar = 0xAB86; break;
            case 0x13B7: mapChar = 0xAB87; break;
            case 0x13B8: mapChar = 0xAB88; break;
            case 0x13B9: mapChar = 0xAB89; break;
            case 0x13BA: mapChar = 0xAB8A; break;
            case 0x13BB: mapChar = 0xAB8B; break;
            case 0x13BC: mapChar = 0xAB8C; break;
            case 0x13BD: mapChar = 0xAB8D; break;
            case 0x13BE: mapChar = 0xAB8E; break;
            case 0x13BF: mapChar = 0xAB8F; break;
            case 0x13C0: mapChar = 0xAB90; break;
            case 0x13C1: mapChar = 0xAB91; break;
            case 0x13C2: mapChar = 0xAB92; break;
            case 0x13C3: mapChar = 0xAB93; break;
            case 0x13C4: mapChar = 0xAB94; break;
            case 0x13C5: mapChar = 0xAB95; break;
            case 0x13C6: mapChar = 0xAB96; break;
            case 0x13C7: mapChar = 0xAB97; break;
            case 0x13C8: mapChar = 0xAB98; break;
            case 0x13C9: mapChar = 0xAB99; break;
            case 0x13CA: mapChar = 0xAB9A; break;
            case 0x13CB: mapChar = 0xAB9B; break;
            case 0x13CC: mapChar = 0xAB9C; break;
            case 0x13CD: mapChar = 0xAB9D; break;
            case 0x13CE: mapChar = 0xAB9E; break;
            case 0x13CF: mapChar = 0xAB9F; break;
            case 0x13D0: mapChar = 0xABA0; break;
            case 0x13D1: mapChar = 0xABA1; break;
            case 0x13D2: mapChar = 0xABA2; break;
            case 0x13D3: mapChar = 0xABA3; break;
            case 0x13D4: mapChar = 0xABA4; break;
            case 0x13D5: mapChar = 0xABA5; break;
            case 0x13D6: mapChar = 0xABA6; break;
            case 0x13D7: mapChar = 0xABA7; break;
            case 0x13D8: mapChar = 0xABA8; break;
            case 0x13D9: mapChar = 0xABA9; break;
            case 0x13DA: mapChar = 0xABAA; break;
            case 0x13DB: mapChar = 0xABAB; break;
            case 0x13DC: mapChar = 0xABAC; break;
            case 0x13DD: mapChar = 0xABAD; break;
            case 0x13DE: mapChar = 0xABAE; break;
            case 0x13DF: mapChar = 0xABAF; break;
            case 0x13E0: mapChar = 0xABB0; break;
            case 0x13E1: mapChar = 0xABB1; break;
            case 0x13E2: mapChar = 0xABB2; break;
            case 0x13E3: mapChar = 0xABB3; break;
            case 0x13E4: mapChar = 0xABB4; break;
            case 0x13E5: mapChar = 0xABB5; break;
            case 0x13E6: mapChar = 0xABB6; break;
            case 0x13E7: mapChar = 0xABB7; break;
            case 0x13E8: mapChar = 0xABB8; break;
            case 0x13E9: mapChar = 0xABB9; break;
            case 0x13EA: mapChar = 0xABBA; break;
            case 0x13EB: mapChar = 0xABBB; break;
            case 0x13EC: mapChar = 0xABBC; break;
            case 0x13ED: mapChar = 0xABBD; break;
            case 0x13EE: mapChar = 0xABBE; break;
            case 0x13EF: mapChar = 0xABBF; break;
            case 0x1E9E: mapChar = 0x00DF; break;
            case 0x1F88: mapChar = 0x1F80; break;
            case 0x1F89: mapChar = 0x1F81; break;
            case 0x1F8A: mapChar = 0x1F82; break;
            case 0x1F8B: mapChar = 0x1F83; break;
            case 0x1F8C: mapChar = 0x1F84; break;
            case 0x1F8D: mapChar = 0x1F85; break;
            case 0x1F8E: mapChar = 0x1F86; break;
            case 0x1F8F: mapChar = 0x1F87; break;
            case 0x1F98: mapChar = 0x1F90; break;
            case 0x1F99: mapChar = 0x1F91; break;
            case 0x1F9A: mapChar = 0x1F92; break;
            case 0x1F9B: mapChar = 0x1F93; break;
            case 0x1F9C: mapChar = 0x1F94; break;
            case 0x1F9D: mapChar = 0x1F95; break;
            case 0x1F9E: mapChar = 0x1F96; break;
            case 0x1F9F: mapChar = 0x1F97; break;
            case 0x1FA8: mapChar = 0x1FA0; break;
            case 0x1FA9: mapChar = 0x1FA1; break;
            case 0x1FAA: mapChar = 0x1FA2; break;
            case 0x1FAB: mapChar = 0x1FA3; break;
            case 0x1FAC: mapChar = 0x1FA4; break;
            case 0x1FAD: mapChar = 0x1FA5; break;
            case 0x1FAE: mapChar = 0x1FA6; break;
            case 0x1FAF: mapChar = 0x1FA7; break;
            case 0x1FBC: mapChar = 0x1FB3; break;
            case 0x1FCC: mapChar = 0x1FC3; break;
            case 0x1FFC: mapChar = 0x1FF3; break;
            case 0x2126: mapChar = 0x03C9; break;
            case 0x212A: mapChar = 0x006B; break;
            case 0x212B: mapChar = 0x00E5; break;
            case 0x2C62: mapChar = 0x026B; break;
            case 0x2C63: mapChar = 0x1D7D; break;
            case 0x2C64: mapChar = 0x027D; break;
            case 0x2C6D: mapChar = 0x0251; break;
            case 0x2C6E: mapChar = 0x0271; break;
            case 0x2C6F: mapChar = 0x0250; break;
            case 0x2C70: mapChar = 0x0252; break;
            case 0x2C7E: mapChar = 0x023F; break;
            case 0x2C7F: mapChar = 0x0240; break;
            case 0xA64A: mapChar = 0x1C88; break;
            case 0xA77D: mapChar = 0x1D79; break;
            case 0xA78D: mapChar = 0x0265; break;
            case 0xA7AA: mapChar = 0x0266; break;
            case 0xA7AB: mapChar = 0x025C; break;
            case 0xA7AC: mapChar = 0x0261; break;
            case 0xA7AD: mapChar = 0x026C; break;
            case 0xA7AE: mapChar = 0x026A; break;
            case 0xA7B0: mapChar = 0x029E; break;
            case 0xA7B1: mapChar = 0x0287; break;
            case 0xA7B2: mapChar = 0x029D; break;
            case 0xA7B3: mapChar = 0xAB53; break;
              // default mapChar is already set, so no
              // need to redo it here.
              // default       : mapChar = ch;
            }
          }
          else {
            int offset = val << 5 >> (5+18);
            mapChar = ch + offset;
          }
        }
        return mapChar;
    }

    int toUpperCase(int ch) {
        int mapChar = ch;
        int val = getProperties(ch);

        if ((val & 0x00010000) != 0) {
          if ((val & 0x07FC0000) == 0x07FC0000) {
            switch(ch) {
            case 0x017F: mapChar = 0x0053; break;
            case 0x023F: mapChar = 0x2C7E; break;
            case 0x0240: mapChar = 0x2C7F; break;
            case 0x0250: mapChar = 0x2C6F; break;
            case 0x0251: mapChar = 0x2C6D; break;
            case 0x0252: mapChar = 0x2C70; break;
            case 0x025C: mapChar = 0xA7AB; break;
            case 0x0261: mapChar = 0xA7AC; break;
            case 0x0265: mapChar = 0xA78D; break;
            case 0x0266: mapChar = 0xA7AA; break;
            case 0x026A: mapChar = 0xA7AE; break;
            case 0x026B: mapChar = 0x2C62; break;
            case 0x026C: mapChar = 0xA7AD; break;
            case 0x0271: mapChar = 0x2C6E; break;
            case 0x027D: mapChar = 0x2C64; break;
            case 0x0287: mapChar = 0xA7B1; break;
            case 0x029D: mapChar = 0xA7B2; break;
            case 0x029E: mapChar = 0xA7B0; break;
            case 0x1C80: mapChar = 0x0412; break;
            case 0x1C81: mapChar = 0x0414; break;
            case 0x1C82: mapChar = 0x041E; break;
            case 0x1C83: mapChar = 0x0421; break;
            case 0x1C84: mapChar = 0x0422; break;
            case 0x1C85: mapChar = 0x0422; break;
            case 0x1C86: mapChar = 0x042A; break;
            case 0x1C87: mapChar = 0x0462; break;
            case 0x1C88: mapChar = 0xA64A; break;
            case 0x1D79: mapChar = 0xA77D; break;
            case 0x1D7D: mapChar = 0x2C63; break;
            case 0x1F80: mapChar = 0x1F88; break;
            case 0x1F81: mapChar = 0x1F89; break;
            case 0x1F82: mapChar = 0x1F8A; break;
            case 0x1F83: mapChar = 0x1F8B; break;
            case 0x1F84: mapChar = 0x1F8C; break;
            case 0x1F85: mapChar = 0x1F8D; break;
            case 0x1F86: mapChar = 0x1F8E; break;
            case 0x1F87: mapChar = 0x1F8F; break;
            case 0x1F90: mapChar = 0x1F98; break;
            case 0x1F91: mapChar = 0x1F99; break;
            case 0x1F92: mapChar = 0x1F9A; break;
            case 0x1F93: mapChar = 0x1F9B; break;
            case 0x1F94: mapChar = 0x1F9C; break;
            case 0x1F95: mapChar = 0x1F9D; break;
            case 0x1F96: mapChar = 0x1F9E; break;
            case 0x1F97: mapChar = 0x1F9F; break;
            case 0x1FA0: mapChar = 0x1FA8; break;
            case 0x1FA1: mapChar = 0x1FA9; break;
            case 0x1FA2: mapChar = 0x1FAA; break;
            case 0x1FA3: mapChar = 0x1FAB; break;
            case 0x1FA4: mapChar = 0x1FAC; break;
            case 0x1FA5: mapChar = 0x1FAD; break;
            case 0x1FA6: mapChar = 0x1FAE; break;
            case 0x1FA7: mapChar = 0x1FAF; break;
            case 0x1FB3: mapChar = 0x1FBC; break;
            case 0x1FBE: mapChar = 0x0399; break;
            case 0x1FC3: mapChar = 0x1FCC; break;
            case 0x1FF3: mapChar = 0x1FFC; break;
            case 0x2C65: mapChar = 0x023A; break;
            case 0x2C66: mapChar = 0x023E; break;
            case 0x2D00: mapChar = 0x10A0; break;
            case 0x2D01: mapChar = 0x10A1; break;
            case 0x2D02: mapChar = 0x10A2; break;
            case 0x2D03: mapChar = 0x10A3; break;
            case 0x2D04: mapChar = 0x10A4; break;
            case 0x2D05: mapChar = 0x10A5; break;
            case 0x2D06: mapChar = 0x10A6; break;
            case 0x2D07: mapChar = 0x10A7; break;
            case 0x2D08: mapChar = 0x10A8; break;
            case 0x2D09: mapChar = 0x10A9; break;
            case 0x2D0A: mapChar = 0x10AA; break;
            case 0x2D0B: mapChar = 0x10AB; break;
            case 0x2D0C: mapChar = 0x10AC; break;
            case 0x2D0D: mapChar = 0x10AD; break;
            case 0x2D0E: mapChar = 0x10AE; break;
            case 0x2D0F: mapChar = 0x10AF; break;
            case 0x2D10: mapChar = 0x10B0; break;
            case 0x2D11: mapChar = 0x10B1; break;
            case 0x2D12: mapChar = 0x10B2; break;
            case 0x2D13: mapChar = 0x10B3; break;
            case 0x2D14: mapChar = 0x10B4; break;
            case 0x2D15: mapChar = 0x10B5; break;
            case 0x2D16: mapChar = 0x10B6; break;
            case 0x2D17: mapChar = 0x10B7; break;
            case 0x2D18: mapChar = 0x10B8; break;
            case 0x2D19: mapChar = 0x10B9; break;
            case 0x2D1A: mapChar = 0x10BA; break;
            case 0x2D1B: mapChar = 0x10BB; break;
            case 0x2D1C: mapChar = 0x10BC; break;
            case 0x2D1D: mapChar = 0x10BD; break;
            case 0x2D1E: mapChar = 0x10BE; break;
            case 0x2D1F: mapChar = 0x10BF; break;
            case 0x2D20: mapChar = 0x10C0; break;
            case 0x2D21: mapChar = 0x10C1; break;
            case 0x2D22: mapChar = 0x10C2; break;
            case 0x2D23: mapChar = 0x10C3; break;
            case 0x2D24: mapChar = 0x10C4; break;
            case 0x2D25: mapChar = 0x10C5; break;
            case 0x2D27: mapChar = 0x10C7; break;
            case 0x2D2D: mapChar = 0x10CD; break;
            case 0xAB53: mapChar = 0xA7B3; break;
            case 0xAB70: mapChar = 0x13A0; break;
            case 0xAB71: mapChar = 0x13A1; break;
            case 0xAB72: mapChar = 0x13A2; break;
            case 0xAB73: mapChar = 0x13A3; break;
            case 0xAB74: mapChar = 0x13A4; break;
            case 0xAB75: mapChar = 0x13A5; break;
            case 0xAB76: mapChar = 0x13A6; break;
            case 0xAB77: mapChar = 0x13A7; break;
            case 0xAB78: mapChar = 0x13A8; break;
            case 0xAB79: mapChar = 0x13A9; break;
            case 0xAB7A: mapChar = 0x13AA; break;
            case 0xAB7B: mapChar = 0x13AB; break;
            case 0xAB7C: mapChar = 0x13AC; break;
            case 0xAB7D: mapChar = 0x13AD; break;
            case 0xAB7E: mapChar = 0x13AE; break;
            case 0xAB7F: mapChar = 0x13AF; break;
            case 0xAB80: mapChar = 0x13B0; break;
            case 0xAB81: mapChar = 0x13B1; break;
            case 0xAB82: mapChar = 0x13B2; break;
            case 0xAB83: mapChar = 0x13B3; break;
            case 0xAB84: mapChar = 0x13B4; break;
            case 0xAB85: mapChar = 0x13B5; break;
            case 0xAB86: mapChar = 0x13B6; break;
            case 0xAB87: mapChar = 0x13B7; break;
            case 0xAB88: mapChar = 0x13B8; break;
            case 0xAB89: mapChar = 0x13B9; break;
            case 0xAB8A: mapChar = 0x13BA; break;
            case 0xAB8B: mapChar = 0x13BB; break;
            case 0xAB8C: mapChar = 0x13BC; break;
            case 0xAB8D: mapChar = 0x13BD; break;
            case 0xAB8E: mapChar = 0x13BE; break;
            case 0xAB8F: mapChar = 0x13BF; break;
            case 0xAB90: mapChar = 0x13C0; break;
            case 0xAB91: mapChar = 0x13C1; break;
            case 0xAB92: mapChar = 0x13C2; break;
            case 0xAB93: mapChar = 0x13C3; break;
            case 0xAB94: mapChar = 0x13C4; break;
            case 0xAB95: mapChar = 0x13C5; break;
            case 0xAB96: mapChar = 0x13C6; break;
            case 0xAB97: mapChar = 0x13C7; break;
            case 0xAB98: mapChar = 0x13C8; break;
            case 0xAB99: mapChar = 0x13C9; break;
            case 0xAB9A: mapChar = 0x13CA; break;
            case 0xAB9B: mapChar = 0x13CB; break;
            case 0xAB9C: mapChar = 0x13CC; break;
            case 0xAB9D: mapChar = 0x13CD; break;
            case 0xAB9E: mapChar = 0x13CE; break;
            case 0xAB9F: mapChar = 0x13CF; break;
            case 0xABA0: mapChar = 0x13D0; break;
            case 0xABA1: mapChar = 0x13D1; break;
            case 0xABA2: mapChar = 0x13D2; break;
            case 0xABA3: mapChar = 0x13D3; break;
            case 0xABA4: mapChar = 0x13D4; break;
            case 0xABA5: mapChar = 0x13D5; break;
            case 0xABA6: mapChar = 0x13D6; break;
            case 0xABA7: mapChar = 0x13D7; break;
            case 0xABA8: mapChar = 0x13D8; break;
            case 0xABA9: mapChar = 0x13D9; break;
            case 0xABAA: mapChar = 0x13DA; break;
            case 0xABAB: mapChar = 0x13DB; break;
            case 0xABAC: mapChar = 0x13DC; break;
            case 0xABAD: mapChar = 0x13DD; break;
            case 0xABAE: mapChar = 0x13DE; break;
            case 0xABAF: mapChar = 0x13DF; break;
            case 0xABB0: mapChar = 0x13E0; break;
            case 0xABB1: mapChar = 0x13E1; break;
            case 0xABB2: mapChar = 0x13E2; break;
            case 0xABB3: mapChar = 0x13E3; break;
            case 0xABB4: mapChar = 0x13E4; break;
            case 0xABB5: mapChar = 0x13E5; break;
            case 0xABB6: mapChar = 0x13E6; break;
            case 0xABB7: mapChar = 0x13E7; break;
            case 0xABB8: mapChar = 0x13E8; break;
            case 0xABB9: mapChar = 0x13E9; break;
            case 0xABBA: mapChar = 0x13EA; break;
            case 0xABBB: mapChar = 0x13EB; break;
            case 0xABBC: mapChar = 0x13EC; break;
            case 0xABBD: mapChar = 0x13ED; break;
            case 0xABBE: mapChar = 0x13EE; break;
            case 0xABBF: mapChar = 0x13EF; break;
              // ch must have a 1:M case mapping, but we
              // can't handle it here. Return ch.
              // since mapChar is already set, no need
              // to redo it here.
              //default       : mapChar = ch;
            }
          }
          else {
            int offset = val  << 5 >> (5+18);
            mapChar =  ch - offset;
          }
        }
        return mapChar;
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
        return mapChar;
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
            switch (ch) {
                case 0x0BF1: retval = 100; break;         // TAMIL NUMBER ONE HUNDRED
                case 0x0BF2: retval = 1000; break;        // TAMIL NUMBER ONE THOUSAND
                case 0x0D71: retval = 100; break;         // MALAYALAM NUMBER ONE HUNDRED
                case 0x0D72: retval = 1000; break;        // MALAYALAM NUMBER ONE THOUSAND
                case 0x1375: retval = 40; break;          // ETHIOPIC NUMBER FORTY
                case 0x1376: retval = 50; break;          // ETHIOPIC NUMBER FIFTY
                case 0x1377: retval = 60; break;          // ETHIOPIC NUMBER SIXTY
                case 0x1378: retval = 70; break;          // ETHIOPIC NUMBER SEVENTY
                case 0x1379: retval = 80; break;          // ETHIOPIC NUMBER EIGHTY
                case 0x137A: retval = 90; break;          // ETHIOPIC NUMBER NINETY
                case 0x137B: retval = 100; break;         // ETHIOPIC NUMBER HUNDRED
                case 0x137C: retval = 10000; break;       // ETHIOPIC NUMBER TEN THOUSAND
                case 0x215F: retval = 1; break;           // FRACTION NUMERATOR ONE
                case 0x216C: retval = 50; break;          // ROMAN NUMERAL FIFTY
                case 0x216D: retval = 100; break;         // ROMAN NUMERAL ONE HUNDRED
                case 0x216E: retval = 500; break;         // ROMAN NUMERAL FIVE HUNDRED
                case 0x216F: retval = 1000; break;        // ROMAN NUMERAL ONE THOUSAND
                case 0x217C: retval = 50; break;          // SMALL ROMAN NUMERAL FIFTY
                case 0x217D: retval = 100; break;         // SMALL ROMAN NUMERAL ONE HUNDRED
                case 0x217E: retval = 500; break;         // SMALL ROMAN NUMERAL FIVE HUNDRED
                case 0x217F: retval = 1000; break;        // SMALL ROMAN NUMERAL ONE THOUSAND
                case 0x2180: retval = 1000; break;        // ROMAN NUMERAL ONE THOUSAND C D
                case 0x2181: retval = 5000; break;        // ROMAN NUMERAL FIVE THOUSAND
                case 0x2182: retval = 10000; break;       // ROMAN NUMERAL TEN THOUSAND
                case 0x2186: retval = 50; break;          // ROMAN NUMERAL FIFTY EARLY FORM
                case 0x2187: retval = 50000; break;       // ROMAN NUMERAL FIFTY THOUSAND
                case 0x2188: retval = 100000; break;      // ROMAN NUMERAL ONE HUNDRED THOUSAND
                case 0x324B: retval = 40; break;          // CIRCLED NUMBER FORTY ON BLACK SQUARE
                case 0x324C: retval = 50; break;          // CIRCLED NUMBER FIFTY ON BLACK SQUARE
                case 0x324D: retval = 60; break;          // CIRCLED NUMBER SIXTY ON BLACK SQUARE
                case 0x324E: retval = 70; break;          // CIRCLED NUMBER SEVENTY ON BLACK SQUARE
                case 0x324F: retval = 80; break;          // CIRCLED NUMBER EIGHTY ON BLACK SQUARE
                case 0x325C: retval = 32; break;          // CIRCLED NUMBER THIRTY TWO
                case 0x325D: retval = 33; break;          // CIRCLED NUMBER THIRTY THREE
                case 0x325E: retval = 34; break;          // CIRCLED NUMBER THIRTY FOUR
                case 0x325F: retval = 35; break;          // CIRCLED NUMBER THIRTY FIVE
                case 0x32B1: retval = 36; break;          // CIRCLED NUMBER THIRTY SIX
                case 0x32B2: retval = 37; break;          // CIRCLED NUMBER THIRTY SEVEN
                case 0x32B3: retval = 38; break;          // CIRCLED NUMBER THIRTY EIGHT
                case 0x32B4: retval = 39; break;          // CIRCLED NUMBER THIRTY NINE
                case 0x32B5: retval = 40; break;          // CIRCLED NUMBER FORTY
                case 0x32B6: retval = 41; break;          // CIRCLED NUMBER FORTY ONE
                case 0x32B7: retval = 42; break;          // CIRCLED NUMBER FORTY TWO
                case 0x32B8: retval = 43; break;          // CIRCLED NUMBER FORTY THREE
                case 0x32B9: retval = 44; break;          // CIRCLED NUMBER FORTY FOUR
                case 0x32BA: retval = 45; break;          // CIRCLED NUMBER FORTY FIVE
                case 0x32BB: retval = 46; break;          // CIRCLED NUMBER FORTY SIX
                case 0x32BC: retval = 47; break;          // CIRCLED NUMBER FORTY SEVEN
                case 0x32BD: retval = 48; break;          // CIRCLED NUMBER FORTY EIGHT
                case 0x32BE: retval = 49; break;          // CIRCLED NUMBER FORTY NINE
                case 0x32BF: retval = 50; break;          // CIRCLED NUMBER FIFTY
                default:       retval = -2; break;
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
            switch(ch) {
                case 0x202A :
                    // This is the only char with LRE
                    directionality = Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING;
                    break;
                case 0x202B :
                    // This is the only char with RLE
                    directionality = Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING;
                    break;
                case 0x202C :
                    // This is the only char with PDF
                    directionality = Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT;
                    break;
                case 0x202D :
                    // This is the only char with LRO
                    directionality = Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE;
                    break;
                case 0x202E :
                    // This is the only char with RLO
                    directionality = Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
                    break;
                case 0x2066 :
                    // This is the only char with LRI
                    directionality = Character.DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE;
                    break;
                case 0x2067 :
                    // This is the only char with RLI
                    directionality = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE;
                    break;
                case 0x2068 :
                    // This is the only char with FSI
                    directionality = Character.DIRECTIONALITY_FIRST_STRONG_ISOLATE;
                    break;
                case 0x2069 :
                    // This is the only char with PDI
                    directionality = Character.DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE;
                    break;
                default :
                    directionality = Character.DIRECTIONALITY_UNDEFINED;
                    break;
            }
        }
        return directionality;
    }

    boolean isMirrored(int ch) {
        int props = getProperties(ch);
        return ((props & 0x80000000) != 0);
    }

    int toUpperCaseEx(int ch) {
        int mapChar = ch;
        int val = getProperties(ch);

        if ((val & 0x00010000) != 0) {
            if ((val & 0x07FC0000) != 0x07FC0000) {
                int offset = val  << 5 >> (5+18);
                mapChar =  ch - offset;
            }
            else {
                switch(ch) {
                    case 0x017F: mapChar = 0x0053; break;
                    case 0x023F: mapChar = 0x2C7E; break;
                    case 0x0240: mapChar = 0x2C7F; break;
                    case 0x0250: mapChar = 0x2C6F; break;
                    case 0x0251: mapChar = 0x2C6D; break;
                    case 0x0252: mapChar = 0x2C70; break;
                    case 0x025C: mapChar = 0xA7AB; break;
                    case 0x0261: mapChar = 0xA7AC; break;
                    case 0x0265: mapChar = 0xA78D; break;
                    case 0x0266: mapChar = 0xA7AA; break;
                    case 0x026A: mapChar = 0xA7AE; break;
                    case 0x026B: mapChar = 0x2C62; break;
                    case 0x026C: mapChar = 0xA7AD; break;
                    case 0x0271: mapChar = 0x2C6E; break;
                    case 0x027D: mapChar = 0x2C64; break;
                    case 0x0287: mapChar = 0xA7B1; break;
                    case 0x029D: mapChar = 0xA7B2; break;
                    case 0x029E: mapChar = 0xA7B0; break;
                    case 0x1C80: mapChar = 0x0412; break;
                    case 0x1C81: mapChar = 0x0414; break;
                    case 0x1C82: mapChar = 0x041E; break;
                    case 0x1C83: mapChar = 0x0421; break;
                    case 0x1C84: mapChar = 0x0422; break;
                    case 0x1C85: mapChar = 0x0422; break;
                    case 0x1C86: mapChar = 0x042A; break;
                    case 0x1C87: mapChar = 0x0462; break;
                    case 0x1C88: mapChar = 0xA64A; break;
                    case 0x1D79: mapChar = 0xA77D; break;
                    case 0x1D7D: mapChar = 0x2C63; break;
                    case 0x1FBE: mapChar = 0x0399; break;
                    case 0x2C65: mapChar = 0x023A; break;
                    case 0x2C66: mapChar = 0x023E; break;
                    case 0x2D00: mapChar = 0x10A0; break;
                    case 0x2D01: mapChar = 0x10A1; break;
                    case 0x2D02: mapChar = 0x10A2; break;
                    case 0x2D03: mapChar = 0x10A3; break;
                    case 0x2D04: mapChar = 0x10A4; break;
                    case 0x2D05: mapChar = 0x10A5; break;
                    case 0x2D06: mapChar = 0x10A6; break;
                    case 0x2D07: mapChar = 0x10A7; break;
                    case 0x2D08: mapChar = 0x10A8; break;
                    case 0x2D09: mapChar = 0x10A9; break;
                    case 0x2D0A: mapChar = 0x10AA; break;
                    case 0x2D0B: mapChar = 0x10AB; break;
                    case 0x2D0C: mapChar = 0x10AC; break;
                    case 0x2D0D: mapChar = 0x10AD; break;
                    case 0x2D0E: mapChar = 0x10AE; break;
                    case 0x2D0F: mapChar = 0x10AF; break;
                    case 0x2D10: mapChar = 0x10B0; break;
                    case 0x2D11: mapChar = 0x10B1; break;
                    case 0x2D12: mapChar = 0x10B2; break;
                    case 0x2D13: mapChar = 0x10B3; break;
                    case 0x2D14: mapChar = 0x10B4; break;
                    case 0x2D15: mapChar = 0x10B5; break;
                    case 0x2D16: mapChar = 0x10B6; break;
                    case 0x2D17: mapChar = 0x10B7; break;
                    case 0x2D18: mapChar = 0x10B8; break;
                    case 0x2D19: mapChar = 0x10B9; break;
                    case 0x2D1A: mapChar = 0x10BA; break;
                    case 0x2D1B: mapChar = 0x10BB; break;
                    case 0x2D1C: mapChar = 0x10BC; break;
                    case 0x2D1D: mapChar = 0x10BD; break;
                    case 0x2D1E: mapChar = 0x10BE; break;
                    case 0x2D1F: mapChar = 0x10BF; break;
                    case 0x2D20: mapChar = 0x10C0; break;
                    case 0x2D21: mapChar = 0x10C1; break;
                    case 0x2D22: mapChar = 0x10C2; break;
                    case 0x2D23: mapChar = 0x10C3; break;
                    case 0x2D24: mapChar = 0x10C4; break;
                    case 0x2D25: mapChar = 0x10C5; break;
                    case 0x2D27: mapChar = 0x10C7; break;
                    case 0x2D2D: mapChar = 0x10CD; break;
                    case 0xAB53: mapChar = 0xA7B3; break;
                    case 0xAB70: mapChar = 0x13A0; break;
                    case 0xAB71: mapChar = 0x13A1; break;
                    case 0xAB72: mapChar = 0x13A2; break;
                    case 0xAB73: mapChar = 0x13A3; break;
                    case 0xAB74: mapChar = 0x13A4; break;
                    case 0xAB75: mapChar = 0x13A5; break;
                    case 0xAB76: mapChar = 0x13A6; break;
                    case 0xAB77: mapChar = 0x13A7; break;
                    case 0xAB78: mapChar = 0x13A8; break;
                    case 0xAB79: mapChar = 0x13A9; break;
                    case 0xAB7A: mapChar = 0x13AA; break;
                    case 0xAB7B: mapChar = 0x13AB; break;
                    case 0xAB7C: mapChar = 0x13AC; break;
                    case 0xAB7D: mapChar = 0x13AD; break;
                    case 0xAB7E: mapChar = 0x13AE; break;
                    case 0xAB7F: mapChar = 0x13AF; break;
                    case 0xAB80: mapChar = 0x13B0; break;
                    case 0xAB81: mapChar = 0x13B1; break;
                    case 0xAB82: mapChar = 0x13B2; break;
                    case 0xAB83: mapChar = 0x13B3; break;
                    case 0xAB84: mapChar = 0x13B4; break;
                    case 0xAB85: mapChar = 0x13B5; break;
                    case 0xAB86: mapChar = 0x13B6; break;
                    case 0xAB87: mapChar = 0x13B7; break;
                    case 0xAB88: mapChar = 0x13B8; break;
                    case 0xAB89: mapChar = 0x13B9; break;
                    case 0xAB8A: mapChar = 0x13BA; break;
                    case 0xAB8B: mapChar = 0x13BB; break;
                    case 0xAB8C: mapChar = 0x13BC; break;
                    case 0xAB8D: mapChar = 0x13BD; break;
                    case 0xAB8E: mapChar = 0x13BE; break;
                    case 0xAB8F: mapChar = 0x13BF; break;
                    case 0xAB90: mapChar = 0x13C0; break;
                    case 0xAB91: mapChar = 0x13C1; break;
                    case 0xAB92: mapChar = 0x13C2; break;
                    case 0xAB93: mapChar = 0x13C3; break;
                    case 0xAB94: mapChar = 0x13C4; break;
                    case 0xAB95: mapChar = 0x13C5; break;
                    case 0xAB96: mapChar = 0x13C6; break;
                    case 0xAB97: mapChar = 0x13C7; break;
                    case 0xAB98: mapChar = 0x13C8; break;
                    case 0xAB99: mapChar = 0x13C9; break;
                    case 0xAB9A: mapChar = 0x13CA; break;
                    case 0xAB9B: mapChar = 0x13CB; break;
                    case 0xAB9C: mapChar = 0x13CC; break;
                    case 0xAB9D: mapChar = 0x13CD; break;
                    case 0xAB9E: mapChar = 0x13CE; break;
                    case 0xAB9F: mapChar = 0x13CF; break;
                    case 0xABA0: mapChar = 0x13D0; break;
                    case 0xABA1: mapChar = 0x13D1; break;
                    case 0xABA2: mapChar = 0x13D2; break;
                    case 0xABA3: mapChar = 0x13D3; break;
                    case 0xABA4: mapChar = 0x13D4; break;
                    case 0xABA5: mapChar = 0x13D5; break;
                    case 0xABA6: mapChar = 0x13D6; break;
                    case 0xABA7: mapChar = 0x13D7; break;
                    case 0xABA8: mapChar = 0x13D8; break;
                    case 0xABA9: mapChar = 0x13D9; break;
                    case 0xABAA: mapChar = 0x13DA; break;
                    case 0xABAB: mapChar = 0x13DB; break;
                    case 0xABAC: mapChar = 0x13DC; break;
                    case 0xABAD: mapChar = 0x13DD; break;
                    case 0xABAE: mapChar = 0x13DE; break;
                    case 0xABAF: mapChar = 0x13DF; break;
                    case 0xABB0: mapChar = 0x13E0; break;
                    case 0xABB1: mapChar = 0x13E1; break;
                    case 0xABB2: mapChar = 0x13E2; break;
                    case 0xABB3: mapChar = 0x13E3; break;
                    case 0xABB4: mapChar = 0x13E4; break;
                    case 0xABB5: mapChar = 0x13E5; break;
                    case 0xABB6: mapChar = 0x13E6; break;
                    case 0xABB7: mapChar = 0x13E7; break;
                    case 0xABB8: mapChar = 0x13E8; break;
                    case 0xABB9: mapChar = 0x13E9; break;
                    case 0xABBA: mapChar = 0x13EA; break;
                    case 0xABBB: mapChar = 0x13EB; break;
                    case 0xABBC: mapChar = 0x13EC; break;
                    case 0xABBD: mapChar = 0x13ED; break;
                    case 0xABBE: mapChar = 0x13EE; break;
                    case 0xABBF: mapChar = 0x13EF; break;
                    default       : mapChar = Character.ERROR; break;
                }
            }
        }
        return mapChar;
    }

    char[] toUpperCaseCharArray(int ch) {
        char[] upperMap = {(char)ch};
        int location = findInCharMap(ch);
        if (location != -1) {
            upperMap = charMap[location][1];
        }
        return upperMap;
    }


    /**
     * Finds the character in the uppercase mapping table.
     *
     * @param ch the <code>char</code> to search
     * @return the index location ch in the table or -1 if not found
     * @since 1.4
     */
     int findInCharMap(int ch) {
        if (charMap == null || charMap.length == 0) {
            return -1;
        }
        int top, bottom, current;
        bottom = 0;
        top = charMap.length;
        current = top/2;
        // invariant: top > current >= bottom && ch >= CharacterData.charMap[bottom][0]
        while (top - bottom > 1) {
            if (ch >= charMap[current][0][0]) {
                bottom = current;
            } else {
                top = current;
            }
            current = (top + bottom) / 2;
        }
        if (ch == charMap[current][0][0]) return current;
        else return -1;
    }

    static final CharacterData00 instance = new CharacterData00();
    private CharacterData00() {};

    // The following tables and code generated using:
  // java GenerateCharacter -string -plane 0 -template t:/workspace/open/make/data/characterdata/CharacterData00.java.template -spec t:/workspace/open/make/data/unicodedata/UnicodeData.txt -specialcasing t:/workspace/open/make/data/unicodedata/SpecialCasing.txt -proplist t:/workspace/open/make/data/unicodedata/PropList.txt -o t:/workspace/build/windows-x64-open/support/gensrc/java.base/java/lang/CharacterData00.java -usecharforbyte 11 4 1
      static final char[][][] charMap;
// The X table has 2048 entries for a total of 4096 bytes.

  static final char X[] = (
    "\000\020\040\060\100\120\140\160\200\220\240\260\300\320\340\360\200\u0100"+
    "\u0110\u0120\u0130\u0140\u0150\u0160\u0170\u0170\u0180\u0190\u01A0\u01B0\u01C0"+
    "\u01D0\u01E0\u01F0\u0200\200\u0210\200\u0220\200\200\u0230\u0240\u0250\u0260"+
    "\u0270\u0280\u0290\u02A0\u02B0\u02C0\u02D0\u02B0\u02B0\u02E0\u02F0\u0300\u0310"+
    "\u0320\u02B0\u02B0\u0330\u0340\u0350\u0360\u0370\u0380\u0390\u03A0\u03B0\u03C0"+
    "\u03D0\u03E0\u03F0\u0400\u0410\u0420\u0430\u0440\u0450\u0460\u0470\u0480\u0490"+
    "\u04A0\u04B0\u04C0\u04D0\u04E0\u04F0\u0500\u0510\u0520\u0530\u0540\u0550\u0560"+
    "\u0570\u0580\u0590\u05A0\u05B0\u05C0\u05D0\u05E0\u05F0\u0600\u0610\u0620\u0630"+
    "\u0640\u0650\u0660\u0670\u0680\u03A0\u0690\u06A0\u06B0\u03A0\u06C0\u06D0\u06E0"+
    "\u06F0\u0700\u0710\u0720\u03A0\u0730\u0740\u0750\u0760\u0770\u0780\u0790\u07A0"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u07B0\u0730\u07C0"+
    "\u07D0\u07E0\u0730\u07F0\u0730\u0800\u0810\u0820\u0780\u0780\u0830\u0840\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0850\u0860\u0730\u0730\u0870\u0880\u0890\u08A0\u08B0"+
    "\u0730\u08C0\u08D0\u08E0\u08F0\u0730\u0900\u0910\u0920\u0930\u0730\u0940\u0950"+
    "\u0960\u0970\u0980\u0730\u0990\u09A0\u09B0\u09C0\u0730\u09D0\u09E0\u09F0\u0A00"+
    "\u03A0\u03A0\u0A10\u0A20\u0A30\u0A40\u0A50\u0A60\u0730\u0A70\u0730\u0A80\u0A90"+
    "\u0AA0\u0AB0\u03A0\u0AC0\u0AD0\u0AE0\u0AF0\u0B00\u0B10\u0B20\u0B00\u0170\u0B30"+
    "\200\200\200\200\u0B40\200\200\200\u0B50\u0B60\u0B70\u0B80\u0B90\u0BA0\u0BB0"+
    "\u0BC0\u0BD0\u0BE0\u0BF0\u0C00\u0C10\u0C20\u0C30\u0C40\u0C50\u0C60\u0C70\u0C80"+
    "\u0C90\u0CA0\u0CB0\u0CC0\u0CD0\u0CE0\u0CF0\u0D00\u0D10\u0D20\u0D30\u0D40\u0D50"+
    "\u0D60\u0D70\u0D80\u0D90\u0DA0\u0DB0\u0DC0\u09B0\u0DD0\u0DE0\u0DF0\u0E00\u0E10"+
    "\u0E20\u0E30\u09B0\u09B0\u09B0\u09B0\u09B0\u0E40\u0E50\u0E60\u09B0\u09B0\u09B0"+
    "\u0E70\u09B0\u0E80\u09B0\u09B0\u09B0\u09B0\u09B0\u0E90\u0EA0\u09B0\u0EB0\u0EC0"+
    "\u0D70\u0D70\u0D70\u0D70\u0D70\u0D70\u0D70\u0D70\u0ED0\u0ED0\u0ED0\u0ED0\u0EE0"+
    "\u0EF0\u0F00\u0F10\u0F20\u0F30\u0F40\u0F50\u0F60\u0F70\u0F80\u0F90\u09B0\u0FA0"+
    "\u0FB0\u0FC0\u0FD0\u0FE0\u0FF0\u1000\u1010\u1020\u1030\u1040\200\200\200\u1050"+
    "\u1060\u1070\u0730\u1080\u1090\u10A0\u10A0\u10B0\u10C0\u10D0\u10E0\u03A0\u10F0"+
    "\u09B0\u09B0\u1100\u09B0\u09B0\u09B0\u09B0\u09B0\u09B0\u1110\u1120\u1130\u1140"+
    "\u0660\u0730\u1150\u0840\u0730\u1160\u1170\u1180\u0730\u0730\u1190\u11A0\u09B0"+
    "\u11B0\u11C0\u11D0\u11E0\u11F0\u11D0\u1200\u1210\u1220\u0D70\u0D70\u0D70\u1230"+
    "\u0D70\u0D70\u1240\u1250\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1270\u09B0\u09B0\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260"+
    "\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1260\u1280\u1290\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u12A0\u09B0\u12B0\u0AA0\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u12C0\u12D0\200\u12E0\u12F0\u0730\u0730"+
    "\u1300\u1310\u1320\200\u1330\u1340\u1350\u03A0\u1360\u1370\u1380\u0730\u1390"+
    "\u13A0\u13B0\u13C0\u13D0\u13E0\u13F0\u1400\u1410\u03E0\u1420\u1430\u1440\u0730"+
    "\u1450\u1460\u1470\u0730\u1480\u1490\u14A0\u14B0\u14C0\u14D0\u14E0\u1060\u1060"+
    "\u0730\u14F0\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730\u0730"+
    "\u1500\u1510\u1520\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530"+
    "\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530"+
    "\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530"+
    "\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530"+
    "\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530\u1530"+
    "\u1530\u1530\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1540"+
    "\u1540\u1540\u1540\u1540\u1540\u1540\u1540\u1260\u1260\u1260\u1550\u1260\u1560"+
    "\u1570\u1580\u1260\u1260\u1260\u1590\u1260\u1260\u15A0\u03A0\u15B0\u15C0\u15D0"+
    "\u02B0\u02B0\u15E0\u15F0\u02B0\u02B0\u02B0\u02B0\u02B0\u02B0\u02B0\u02B0\u02B0"+
    "\u02B0\u1600\u1610\u02B0\u1620\u02B0\u1630\u1640\u1650\u1660\u1670\u1680\u02B0"+
    "\u02B0\u02B0\u1690\u16A0\040\u16B0\u16C0\u16D0\u0950\u16E0\u16F0").toCharArray();

  // The Y table has 5888 entries for a total of 11776 bytes.

  static final char Y[] = (
    "\000\000\000\000\002\004\006\000\000\000\000\000\000\000\010\004\012\014\016"+
    "\020\022\024\026\030\032\032\032\032\032\034\036\040\042\044\044\044\044\044"+
    "\044\044\044\044\044\044\044\046\050\052\054\056\056\056\056\056\056\056\056"+
    "\056\056\056\056\060\062\064\000\000\066\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\070\072\072\074\076\100\102\104\106\110\112\020\114\116\120"+
    "\122\124\124\124\124\124\124\124\124\124\124\124\126\124\124\124\130\132\132"+
    "\132\132\132\132\132\132\132\132\132\134\132\132\132\136\140\140\140\140\140"+
    "\140\140\140\140\140\140\140\140\140\140\140\140\140\140\140\140\140\140\140"+
    "\142\140\140\140\144\146\146\146\146\146\146\146\150\140\140\140\140\140\140"+
    "\140\140\140\140\140\140\140\140\140\140\140\140\140\140\140\140\140\152\146"+
    "\146\150\154\140\140\156\160\162\164\166\170\160\172\174\140\176\200\202\140"+
    "\140\140\204\206\210\140\204\212\214\146\216\140\220\140\222\224\224\226\230"+
    "\232\226\234\146\146\146\146\146\146\146\236\140\140\140\140\140\140\140\140"+
    "\140\240\232\140\242\140\140\140\140\244\140\140\140\140\140\140\140\140\140"+
    "\210\210\210\246\250\252\254\256\260\140\140\140\140\140\262\264\266\270\272"+
    "\274\276\210\300\302\304\276\306\262\276\310\304\312\314\210\210\210\304\210"+
    "\316\320\210\304\322\324\326\210\210\330\332\210\210\210\304\276\210\210\210"+
    "\210\210\210\210\210\334\334\334\334\336\340\342\342\334\344\344\346\346\346"+
    "\346\346\342\344\344\344\344\344\344\344\334\334\350\344\344\344\352\354\344"+
    "\344\344\344\344\344\344\344\356\356\356\356\356\356\356\356\356\356\356\356"+
    "\356\356\356\356\356\356\360\356\356\356\356\356\356\356\356\356\356\356\356"+
    "\356\356\356\356\356\356\356\356\356\140\140\352\140\362\364\366\370\362\362"+
    "\344\372\374\376\u0100\u0102\u0104\124\124\124\124\124\124\124\124\u0106\124"+
    "\124\124\124\u0108\u010A\u010C\132\132\132\132\132\132\132\132\u010E\132\132"+
    "\132\132\u0110\u0112\u0114\u0116\u0118\u011A\140\140\140\140\140\140\140\140"+
    "\140\140\140\140\u011C\u011E\u0120\u0122\u0124\140\u0126\u0128\u012A\u012A"+
    "\u012A\u012A\u012A\u012A\u012A\u012A\124\124\124\124\124\124\124\124\124\124"+
    "\124\124\124\124\124\124\132\132\132\132\132\132\132\132\132\132\132\132\132"+
    "\132\132\132\u012C\u012C\u012C\u012C\u012C\u012C\u012C\u012C\140\u012E\356"+
    "\356\u0130\140\140\140\140\140\140\140\140\140\140\140\u0132\146\146\146\146"+
    "\146\146\u0134\140\140\140\140\140\140\140\140\140\140\140\140\140\140\140"+
    "\140\u0136\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138"+
    "\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u013A\u013C\u013E\u013E\u013E\u0140"+
    "\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142"+
    "\u0142\u0142\u0142\u0142\u0142\u0144\u0146\u0148\u014A\u014C\u014E\356\356"+
    "\356\356\356\356\356\356\356\356\356\356\356\356\356\u0150\u0150\u0150\u0150"+
    "\u0150\u0150\u0150\u0152\u0154\u0156\u0150\u0154\362\362\362\362\u0158\u0158"+
    "\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u015A\362"+
    "\362\u0158\u015C\u015E\362\362\362\362\362\u0160\u0160\u0160\u0162\u0164\u0166"+
    "\u0168\u016A\u0150\u0150\u0150\u0150\u0150\u016C\u016E\u0170\u0172\u0172\u0172"+
    "\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172"+
    "\u0174\u0172\u0172\u0172\u0172\u0176\u0150\u0150\u0150\u0150\u0150\u0150\u0178"+
    "\u0150\u0150\u0150\u017A\u017A\u017A\u017A\u017A\u017C\u017E\u0172\u0180\u0172"+
    "\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172"+
    "\u0172\u0172\u0172\u0182\u0150\u0150\u0150\u0184\u0186\u0178\u0150\u0188\u018A"+
    "\u018C\356\u0178\u0172\032\032\032\032\032\u0172\u018E\u0190\u0170\u0170\u0170"+
    "\u0170\u0170\u0170\u0170\u0192\u0176\u0172\u0172\u0172\u0172\u0172\u0172\u0172"+
    "\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0150\u0150\u0150\u0150\u0150"+
    "\u0150\u0150\u0150\356\356\356\356\356\u0194\u0196\u0172\u0172\u0172\u0172"+
    "\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0150\u0150\u0150\u0150\u0150"+
    "\u0180\362\362\362\362\362\362\362\u0198\u0198\u0198\u0198\u0198\u0158\u0158"+
    "\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158"+
    "\u0158\u019A\356\356\356\356\u019C\074\020\u019E\362\362\u0158\u0158\u0158"+
    "\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0150\356\u01A0\u0150\u0150"+
    "\u0150\u0150\u01A0\u0150\u01A0\u0150\u01A2\362\u01A4\u01A4\u01A4\u01A4\u01A4"+
    "\u01A4\u01A4\u015E\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158\u0158"+
    "\u0158\u0158\u019A\356\362\u015E\u0172\u0172\u0172\u0172\u0172\u01A6\362\362"+
    "\362\362\362\362\362\362\362\362\362\362\362\362\362\362\362\362\362\362\362"+
    "\362\362\362\362\362\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172"+
    "\u0172\u01A6\u0172\u0172\u0172\u0172\362\362\362\362\362\362\362\362\362\362"+
    "\362\u0150\u0150\u0150\u0150\u0150\u0150\356\u01A8\u0150\u0150\u0150\356\356"+
    "\356\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u01AA\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\u01AA\u01AC\u01AE\u01B0\u0150\u0150\u0150\u01AA\u01AE"+
    "\u01B2\u01AE\u01B4\356\u0178\u0150\224\224\224\224\224\u0150\u013E\u01B6\u01B6"+
    "\u01B6\u01B6\u01B6\u01B8\224\224\224\224\224\224\224\u01BA\u01AE\u01BC\224"+
    "\224\224\u01BE\u01BC\u01BE\u01BC\224\224\224\224\224\224\224\224\224\224\u01BE"+
    "\224\224\224\u01BE\u01BE\362\224\224\362\u01AC\u01AE\u01B0\u0150\u01C0\u01C2"+
    "\u01C4\u01C2\u01B2\u01BE\362\362\362\u01C2\362\362\224\u01BC\224\u0150\362"+
    "\u01B6\u01B6\u01B6\u01B6\u01B6\224\072\u01C6\u01C6\u01C8\u01CA\u01CC\362\u01CE"+
    "\u01AA\u01BC\224\224\u01BE\362\u01BC\u01BE\u01BC\224\224\224\224\224\224\224"+
    "\224\224\224\u01BE\224\224\224\u01BE\224\u01BC\u01BE\224\362\u0194\u01AE\u01B0"+
    "\u01C0\362\u01CE\u01C0\u01CE\u01A2\362\u01CE\362\362\362\u01BC\224\u01BE\u01BE"+
    "\362\362\362\u01B6\u01B6\u01B6\u01B6\u01B6\u0150\224\u01BA\362\362\362\362"+
    "\362\u01CE\u01AA\u01BC\224\224\224\224\u01BC\224\u01BC\224\224\224\224\224"+
    "\224\224\224\224\224\u01BE\224\224\224\u01BE\224\u01BC\224\224\362\u01AC\u01AE"+
    "\u01B0\u0150\u0150\u01CE\u01AA\u01C2\u01B2\362\u01BE\362\362\362\362\362\362"+
    "\362\224\u0150\362\u01B6\u01B6\u01B6\u01B6\u01B6\u01D0\362\362\362\u01BC\u0150"+
    "\u01A2\356\u01CE\u01AE\u01BC\224\224\224\u01BE\u01BC\u01BE\u01BC\224\224\224"+
    "\224\224\224\224\224\224\224\u01BE\224\224\224\u01BE\224\u01BC\224\224\362"+
    "\u01AC\u01B0\u01B0\u0150\u01C0\u01C2\u01C4\u01C2\u01B2\362\362\362\362\u01AA"+
    "\362\362\224\u01BC\224\u0150\362\u01B6\u01B6\u01B6\u01B6\u01B6\u01D2\u01C6"+
    "\u01C6\u01C6\362\362\362\362\362\u01D4\u01BC\224\224\u01BE\362\224\u01BE\224"+
    "\224\362\u01BC\u01BE\u01BE\224\362\u01BC\u01BE\362\224\u01BE\362\224\224\224"+
    "\224\224\224\362\362\u01AE\u01AA\u01C4\362\u01AE\u01C4\u01AE\u01B2\362\u01BE"+
    "\362\362\u01C2\362\362\362\362\362\362\362\u01B6\u01B6\u01B6\u01B6\u01B6\u01D6"+
    "\u01D8\u016A\u016A\u014C\u01DA\362\362\u01AA\u01AE\u01BC\224\224\224\u01BE"+
    "\224\u01BE\224\224\224\224\224\224\224\224\224\224\224\u01BE\224\224\224\224"+
    "\224\224\224\224\362\u01BC\u0150\u01AA\u01AE\u01C4\u0150\u01C0\u0150\u01A2"+
    "\362\362\362\u01CE\u01C0\224\u01BE\362\362\224\u0150\362\u01B6\u01B6\u01B6"+
    "\u01B6\u01B6\362\362\362\362\u01DC\u01DC\u01DE\u01E0\u01BA\u01AE\u01BC\224"+
    "\224\224\u01BE\224\u01BE\224\224\224\224\224\224\224\224\224\224\224\u01BE"+
    "\224\224\224\224\224\u01BC\224\224\362\u01AC\u01E2\u01AE\u01AE\u01C4\u01E4"+
    "\u01C4\u01AE\u01A2\362\362\362\u01C2\u01C4\362\362\362\u01BE\224\u0150\362"+
    "\u01B6\u01B6\u01B6\u01B6\u01B6\u01BC\u01BE\362\362\362\362\362\362\u0150\u01AE"+
    "\u01BC\224\224\224\u01BE\224\u01BE\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\u01B4\u01AC\u01AE\u01B0\u0150\u01C0"+
    "\u01AE\u01C4\u01AE\u01B2\u01E6\362\362\224\u01E8\u01C6\u01C6\u01C6\u01EA\224"+
    "\u0150\362\u01B6\u01B6\u01B6\u01B6\u01B6\u01D6\u01C6\u01C6\u01C6\u01EC\224"+
    "\224\224\362\u01AE\u01BC\224\224\224\224\224\224\224\224\u01BE\362\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\u01BC\224\224\224\224\u01BC\362\224"+
    "\224\224\u01BE\362\u0194\362\u01C2\u01AE\u0150\u01C0\u01C0\u01AE\u01AE\u01AE"+
    "\u01AE\362\362\362\u01B6\u01B6\u01B6\u01B6\u01B6\362\u01AE\u01EE\362\362\362"+
    "\362\362\u01BC\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\224\224\224\u01BA\224\u0150\u0150\u0150\u01C0\362\u01F0"+
    "\224\224\224\u01F2\356\356\u0178\u01F4\u01F6\u01F6\u01F6\u01F6\u01F6\u013E"+
    "\362\362\u01BC\u01BE\u01BE\u01BC\u01BE\u01BE\u01BC\362\362\362\224\224\u01BC"+
    "\224\224\224\u01BC\224\u01BC\u01BC\362\224\u01BC\224\u01BA\224\u0150\u0150"+
    "\u0150\u01CE\u01D4\362\224\224\u01BE\u01F8\356\356\u0178\362\u01F6\u01F6\u01F6"+
    "\u01F6\u01F6\362\224\224\u01E6\u01FA\u013E\u013E\u013E\u013E\u013E\u013E\u013E"+
    "\u01FC\u01FC\u01FA\356\u01FA\u01FA\u01FA\u01FE\u01FE\u01FE\u01FE\u01FE\u01C6"+
    "\u01C6\u01C6\u01C6\u01C6\u012E\u012E\u012E\022\022\u0200\224\224\224\224\u01BC"+
    "\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\u01BE"+
    "\362\u01CE\u0150\u0150\u0150\u0150\u0150\u0150\u01AA\u0150\356\u01F4\356\224"+
    "\224\u01BA\u0150\u0150\u0150\u0150\u0150\u01CE\u0150\u0150\u0150\u0150\u0150"+
    "\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u01C0"+
    "\u01FA\u01FA\u01FA\u01FA\u0202\u01FA\u01FA\u0204\u01FA\u013E\u013E\u01FC\u01FA"+
    "\u0206\u01EE\362\362\224\224\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\224\224\u01E8\u01B0\u0150\u01AA\u0150\u0150\u01A2\u01B2"+
    "\u0208\u01B0\u01D4\u01FE\u01FE\u01FE\u01FE\u01FE\u013E\u013E\u013E\224\224"+
    "\224\u01AE\u0150\224\224\u0150\u01D4\u020A\u020C\u01E8\u020A\u0200\u0200\224"+
    "\u01BA\u0150\u01D4\224\224\224\224\224\224\u01AA\u01B0\u020E\u0200\u0200\u0210"+
    "\u0212\u01F6\u01F6\u01F6\u01F6\u01F6\u0200\u01B0\u01FA\u0214\u0214\u0214\u0214"+
    "\u0214\u0214\u0214\u0214\u0214\u0214\u0214\u0214\u0214\u0214\u0214\u0214\u0214"+
    "\u0214\u0214\u0216\362\362\u0216\362\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\224\u01CC\u0218\224\224\224\224\224"+
    "\u01BE\224\224\362\224\224\224\u01BE\u01BE\224\224\362\224\224\224\224\u01BE"+
    "\224\224\362\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\u01BE\224\224\362\224\224\224\u01BE\u01BE\224\224\362\224\224\224\224\224"+
    "\224\224\u01BE\224\224\224\224\224\224\224\224\224\224\224\224\u01BE\224\224"+
    "\362\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\u01BE"+
    "\u014E\u0178\u013E\u013E\u013E\u013E\u021A\u021C\u021C\u021C\u021C\u021E\u0220"+
    "\u01C6\u01C6\u01C6\u0222\362\224\224\224\224\224\224\224\224\u016A\u016A\u016A"+
    "\u016A\u016A\362\362\362\u0214\u0214\u0214\u0214\u0214\u0214\u0214\u0214\u0224"+
    "\u0224\u0224\362\u0226\u0226\u0226\362\u0228\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\224\224\224\u01CC\u022A\224\224\224"+
    "\224\224\224\224\224\u022C\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\u022E\u0230\362\224\224\224\224\224\u01CC\u013E\u0232\u0234\224\224\224\u01BE"+
    "\362\362\362\224\224\224\224\224\224\u01BE\224\224\u0150\u0194\362\362\362"+
    "\362\362\224\224\224\224\224\224\224\224\224\u0150\u01F4\u01EE\362\362\362"+
    "\362\224\224\224\224\224\224\224\224\224\u0150\362\362\362\362\362\362\224"+
    "\224\224\224\224\224\u01BE\224\u01BE\u0150\362\362\362\362\362\362\224\224"+
    "\224\224\224\224\224\224\224\224\356\u01B0\u0150\u0150\u0150\u01AE\u01AE\u01AE"+
    "\u01AE\u01AA\u01B2\356\356\356\356\356\u013E\u01B8\u013E\u01D0\u01B4\362\u01FE"+
    "\u01FE\u01FE\u01FE\u01FE\362\362\362\u0236\u0236\u0236\u0236\u0236\362\362"+
    "\362\020\020\020\u0238\020\u023A\356\u023C\u01F6\u01F6\u01F6\u01F6\u01F6\362"+
    "\362\362\224\u023E\224\224\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\224\224\362\362\362\362\224\224\u01BA"+
    "\u01D4\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\u01BA"+
    "\u01BE\362\362\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\362\362\362\362\362\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\u01BE\u0150\u01AA\u01AE\u01B0\u01AA\u01AE\362\362\u01AE"+
    "\u01AA\u01AE\u01AE\u01B2\356\362\362\u01DA\362\020\u01B6\u01B6\u01B6\u01B6"+
    "\u01B6\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\362\224"+
    "\224\u01BE\362\362\362\362\362\224\224\224\224\224\224\362\362\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\362\362\362\u01F6\u01F6\u01F6\u01F6"+
    "\u01F6\u0240\362\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\224\224\224\224\224\224\224\224"+
    "\224\224\224\u01BA\u01AA\u01B0\362\u013E\224\224\224\224\224\224\224\224\224"+
    "\224\u01E8\u01AA\u0150\u0150\u0150\u01C0\u0208\u01AA\u01B0\u0150\u0150\u0150"+
    "\u01AA\u01AE\u01AE\u01B0\u01A2\356\356\356\u0194\u014E\u01FE\u01FE\u01FE\u01FE"+
    "\u01FE\362\362\362\u01F6\u01F6\u01F6\u01F6\u01F6\362\362\362\u013E\u013E\u013E"+
    "\u01B8\u013E\u013E\u013E\362\356\356\356\356\356\356\356\u0242\u0150\u0150"+
    "\u0244\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\224\u0208\u0150\u0150\u01AA\u01AA\u01AE\u01AE\u01AA\u020C"+
    "\224\224\224\362\362\u01F6\u01F6\u01F6\u01F6\u01F6\u013E\u013E\u013E\u01FC"+
    "\u01FA\u01FA\u01FA\u01FA\u012E\356\356\356\356\u01FA\u01FA\u01FA\u01FA\u0204"+
    "\362\u0150\u0244\224\224\224\224\224\224\224\224\224\224\224\224\224\224\u01E8"+
    "\u0150\u0150\u01AE\u0150\u0210\u0150\224\u01F6\u01F6\u01F6\u01F6\u01F6\224"+
    "\224\224\224\224\224\u0208\u0150\u01AE\u01B0\u01B0\u0150\u0200\362\362\362"+
    "\362\u013E\u013E\224\224\u01AE\u01AE\u01AE\u01AE\u0150\u0150\u0150\u0150\u01AE"+
    "\356\362\u0146\u013E\u013E\u01FE\u01FE\u01FE\u01FE\u01FE\362\u01BC\224\u01F6"+
    "\u01F6\u01F6\u01F6\u01F6\224\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\342\342\342\u013E\262\262\262\262\u0246\362\362\362\362\362\362\362"+
    "\362\362\362\362\u013E\u013E\u013E\u013E\362\362\362\362\356\u01F4\356\356"+
    "\356\356\356\356\u0248\356\356\356\u01AC\224\u01B4\224\224\u01AE\u01AC\u0212"+
    "\356\362\362\362\210\210\210\210\210\210\210\210\210\210\210\210\210\210\210"+
    "\210\210\210\210\210\210\210\334\334\334\334\334\334\334\334\334\334\334\334"+
    "\334\334\334\334\334\334\334\334\334\334\334\334\334\334\334\334\334\334\334"+
    "\u024A\210\210\210\210\210\210\u024C\210\304\210\210\210\210\210\210\210\210"+
    "\210\210\210\210\210\210\u024E\334\334\356\356\356\u0178\u0150\u0150\u0150"+
    "\u0150\u0150\u0150\u01A2\356\356\u014E\356\356\140\140\140\140\140\140\140"+
    "\140\140\140\140\262\262\u0250\210\u0252\u0254\u0254\u0254\u0254\u0256\u0256"+
    "\u0256\u0256\u0254\u0254\u0254\362\u0256\u0256\u0256\362\u0254\u0254\u0254"+
    "\u0254\u0256\u0256\u0256\u0256\u0254\u0254\u0254\u0254\u0256\u0256\u0256\u0256"+
    "\u0254\u0254\u0254\362\u0256\u0256\u0256\362\u0258\u0258\u0258\u0258\u025A"+
    "\u025A\u025A\u025A\u0254\u0254\u0254\u0254\u0256\u0256\u0256\u0256\u025C\u025E"+
    "\u025E\u0260\u0262\u0264\u0266\362\262\262\262\262\u0268\u0268\u0268\u0268"+
    "\262\262\262\262\u0268\u0268\u0268\u0268\262\262\262\262\u0268\u0268\u0268"+
    "\u0268\u0254\262\u0246\262\u0256\u026A\u026C\u026E\344\262\u0246\262\u0270"+
    "\u0270\u026C\344\u0254\262\362\262\u0256\u0272\u0274\344\u0254\262\u0276\262"+
    "\u0256\u0278\u027A\344\362\262\u0246\262\u027C\u027E\u026C\u0280\u0282\u0282"+
    "\u0282\u0284\u0282\u0286\u0288\u028A\u028C\u028C\u028C\020\u028E\u0290\u028E"+
    "\u0290\020\020\020\020\u0292\u0294\u0294\u0296\u0298\u0298\u029A\020\u029C"+
    "\u029E\020\u02A0\u02A2\020\u02A4\u02A6\020\020\020\020\020\u02A8\u02A2\020"+
    "\020\020\020\u02AA\u0288\u0288\u023C\u0294\u0294\u0288\u0288\u0288\u02AC\362"+
    "\110\110\110\u02AE\u02B0\u02B2\u02B4\u02B4\u02B4\u02B4\u02B4\u02AE\u02B0\u0230"+
    "\334\334\334\334\334\334\u02B6\362\072\072\072\072\072\072\072\072\072\072"+
    "\072\072\072\072\072\072\362\362\362\362\362\362\362\362\356\356\356\356\356"+
    "\356\u02B8\u0130\u02BA\u0130\u02BA\356\356\356\356\356\u0194\362\362\362\362"+
    "\362\362\362\u016A\u02BC\u016A\u02BE\u016A\u02C0\u0116\210\u0116\u02C2\u02BE"+
    "\u016A\u02C4\u0116\u0116\u016A\u016A\u016A\u02BC\u02C6\u02BC\u0214\u0116\u02C8"+
    "\u0116\u02CA\220\224\332\u016A\210\u0116\036\u0162\u02C4\210\210\u02CC\u016A"+
    "\u02CE\120\120\120\120\120\120\120\120\u02D0\u02D0\u02D0\u02D0\u02D0\u02D0"+
    "\u02D2\u02D2\u02D4\u02D4\u02D4\u02D4\u02D4\u02D4\u02D6\u02D6\u02D8\u02DA\u02DC"+
    "\u02D8\u02DE\u016A\362\362\u0162\u0162\u02E0\u016A\u016A\u0162\u016A\u016A"+
    "\u02E0\u02CC\u016A\u02E0\u016A\u016A\u016A\u02E0\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u0162\u016A\u02E0"+
    "\u02E0\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u0162\u0162\u0162\u0162\u0162\u0162\u02E2\u02E4\036\u0162"+
    "\u02E4\u02E4\u02E4\u0162\u02E2\u02E6\u02E2\036\u0162\u02E4\u02E4\u02E2\u02E4"+
    "\036\036\036\u0162\u02E2\u02E4\u02E4\u02E4\u02E4\u0162\u0162\u02E2\u02E2\u02E4"+
    "\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\036\u0162\u0162\u02E4\u02E4\u0162"+
    "\u0162\u0162\u0162\u02E2\036\036\u02E4\u02E4\u02E4\u02E4\u0162\u02E4\u02E4"+
    "\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4"+
    "\036\u02E2\u02E4\036\u0162\u0162\036\u0162\u0162\u0162\u0162\u02E4\u0162\u02E4"+
    "\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\036\u0162\u0162\u02E4\u0162"+
    "\u0162\u0162\u0162\u02E2\u02E4\u02E4\u0162\u02E4\u0162\u0162\u02E4\u02E4\u02E4"+
    "\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u0162\u02E4\u02E4\u02E4"+
    "\u02E4\u02E4\u02E4\u02E4\u02E4\u016A\u016A\u016A\u016A\022\022\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u02E4\u016A\u016A\u016A\u02E8"+
    "\u02EA\u016A\u016A\u016A\u016A\u016A\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u02EC\u02E0\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u02EE\u016A\u016A\u02CC\u0162\u0162\u0162\u0162\u0162\u0162\u0162\u0162"+
    "\u0162\u0162\u0162\u0162\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u0162\u0162"+
    "\u0162\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u01DA\362\362\362\362\362\362\362\362"+
    "\362\362\362\362\u016A\u016A\u016A\u016A\u016A\u01DA\362\362\362\362\362\362"+
    "\362\362\362\362\u02F0\u02F0\u02F0\u02F0\u02F0\u02F0\u02F0\u02F0\u02F0\u02F0"+
    "\u02F2\u02F2\u02F2\u02F2\u02F2\u02F2\u02F2\u02F2\u02F2\u02F2\u02F4\u02F4\u02F4"+
    "\u02F4\u02F4\u02F4\u02F4\u02F4\u02F4\u02F4\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u02F6\u02F6\u02F6\u02F6\u02F6\u02F6"+
    "\u02F6\u02F6\u02F6\u02F6\u02F6\u02F6\u02F6\u02F8\u02F8\u02F8\u02F8\u02F8\u02F8"+
    "\u02F8\u02F8\u02F8\u02F8\u02F8\u02F8\u02F8\u02FA\u02FC\u02FC\u02FC\u02FC\u02FE"+
    "\u0300\u0300\u0300\u0300\u0302\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u02CC\u016A\u016A\u016A\u016A\u02CC\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u0162\u0162\u0162"+
    "\u0162\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u02CC\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u02EC\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\022\022"+
    "\022\022\022\022\022\u0304\u0304\u0304\u0304\u0304\u02F0\u02F0\u02F0\u02F0"+
    "\u02F0\u0306\u0306\u0306\u0306\u0306\u016A\u016A\u016A\u016A\u016A\u016A\036"+
    "\u02E2\u0308\u030A\u02E4\u02E2\u02E4\u0162\u0162\u02E2\u02E4\036\u0162\u0162"+
    "\u02E4\036\u0162\u02E4\u02E4\022\022\022\022\022\u0162\u0162\u0162\u0162\u0162"+
    "\u0162\u0162\u0162\u0162\u0162\u0162\u0162\u0162\u0162\u0162\u0162\u0162\u0162"+
    "\u0162\u0162\u0162\u0162\u0162\u0162\u0162\u02B0\u030C\u030C\u030C\u030C\u030C"+
    "\u030C\u030C\u030C\u030C\u030C\u030A\u02E2\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4"+
    "\u02E4\u02E4\u02E4\u02E4\u0162\u0162\u0162\u0162\036\u0162\u0162\u0162\u02E4"+
    "\u02E4\u02E4\u0162\u02E2\u0162\u0162\u02E4\u02E4\036\u02E4\u0162\022\022\036"+
    "\u0162\u02E2\u02E2\u02E4\u0162\u02E4\u0162\u0162\u0162\u0162\u0162\u02E4\u02E4"+
    "\u02E4\u0162\022\u0162\u0162\u0162\u0162\u0162\u0162\u02E4\u02E4\u02E4\u02E4"+
    "\u02E4\u02E4\u02E4\u02E4\u02E4\036\u02E4\u02E4\u0162\036\036\u02E2\u02E2\u02E4"+
    "\036\u0162\u0162\u02E4\u0162\u0162\u0162\u02E4\036\u0162\u0162\u0162\u0162"+
    "\u0162\u0162\u0162\u0162\u0162\u0162\u0162\u02E2\036\u0162\u0162\u0162\u0162"+
    "\u0162\u02E4\u0162\u0162\u02E4\u02E4\u02E2\036\u02E2\036\u0162\u02E2\u02E4"+
    "\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4"+
    "\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u0162\u02E4\u02E4\u02E4\u02E4\u02E2"+
    "\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4"+
    "\u02E4\u02E4\u02E4\u02E4\u02E4\u02E4\036\u0162\u0162\036\036\u0162\u02E4\u02E4"+
    "\036\u0162\u0162\u02E4\036\u0162\u02E2\u0162\u02E2\u02E4\u02E4\u02E2\u0162"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u0162\u0162\u0162\u0162\u0162"+
    "\u0162\u0162\u0162\u0162\u0162\u02E0\u02CC\u0162\u0162\u02E0\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\362\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\362\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\362"+
    "\u014A\u016A\u016A\u016A\u016A\u016A\u01DA\u016A\u016A\u016A\u016A\u01DA\362"+
    "\362\362\362\362\362\362\362\362\362\362\362\u016A\u016A\362\362\362\362\362"+
    "\362\362\362\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138"+
    "\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u0138\u013A"+
    "\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142"+
    "\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u0142\u030E\140\u0214"+
    "\252\254\146\146\u0310\u0214\u0252\140\144\164\210\210\334\u0214\140\140\u0312"+
    "\u016A\u016A\u0314\146\u0316\356\140\362\362\u0318\020\u031A\020\262\262\262"+
    "\262\262\262\262\262\262\262\262\262\262\262\262\262\262\262\262\u031C\362"+
    "\362\u031C\362\224\224\224\224\224\224\224\224\224\224\224\224\362\362\362"+
    "\u013C\u01EE\362\362\362\362\362\362\u014E\224\224\224\224\224\224\224\224"+
    "\224\224\224\u01BE\362\362\362\362\224\224\224\u01BE\224\224\224\u01BE\224"+
    "\224\224\u01BE\224\224\224\u01BE\u0150\u0150\u0150\u0150\u0150\u0150\u0150"+
    "\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u0150\u0150\020\u031E\u031E\020"+
    "\u029C\u029E\u031E\020\020\020\020\u0320\020\u0238\u031E\020\u031E\022\022"+
    "\022\022\020\020\u0322\020\020\020\020\020\u028C\020\020\u0238\u0324\020\020"+
    "\020\362\362\362\362\362\362\362\362\362\362\362\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u014A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\362\362\362\362\362\362\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\u016A\362\362\362\362"+
    "\362\362\362\362\362\362\362\362\362\u016A\u016A\u016A\u016A\u016A\u016A\362"+
    "\362\012\020\u0326\u0328\022\022\022\022\022\u016A\022\022\022\022\u032A\u032C"+
    "\u032E\u0330\u0330\u0330\u0330\356\356\u0200\u0332\342\342\u016A\u0334\u0336"+
    "\u0338\u016A\224\224\224\224\224\224\224\224\224\224\224\u01BE\u014E\u033A"+
    "\u033C\u0218\224\224\224\224\224\224\224\224\224\224\224\224\224\u0338\342"+
    "\u0218\362\362\u01BC\224\224\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\224\u01BE\u01BC\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\u01BE\u01FA\u033E\u033E\u01FA\u01FA\u01FA\u01FA\u01FA\224"+
    "\224\224\224\224\224\224\224\224\224\224\224\224\u01BE\362\362\u016A\u016A"+
    "\362\362\362\362\362\362\224\224\224\224\224\224\224\224\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u02EC\u01DA"+
    "\u0340\u0340\u0340\u0340\u0340\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u0342\u0344\u01C6\u01C6\u0346\u0348"+
    "\u0348\u0348\u0348\u0348\120\120\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u016A\u02EE\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u01FA\u034A\120\120\120\120\120\120\120\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u016A\u016A\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u0204\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u02EC\u016A\u02EE\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u016A\u01FA\u01FA\u01FA\u01FA"+
    "\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u01FA\u02EC\u034C"+
    "\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C"+
    "\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C"+
    "\362\362\362\362\362\u034C\u034C\u034C\u034C\u034C\u034E\362\362\362\362\362"+
    "\362\362\362\362\362\224\224\224\224\224\224\224\224\224\224\u023E\224\224"+
    "\224\224\224\224\224\224\224\224\224\u01BE\362\u016A\u016A\u016A\u016A\u016A"+
    "\u016A\u016A\u016A\u016A\u016A\u016A\u01DA\362\362\362\362\224\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\u0350\020\224\224\224\224\224\224"+
    "\224\224\u01FE\u01FE\u01FE\u01FE\u01FE\224\362\362\362\362\362\362\362\362"+
    "\362\362\140\140\140\140\140\140\140\u01B4\u0130\u0352\u0150\u0150\u0150\u0150"+
    "\356\u0322\140\140\140\140\140\140\140\140\140\140\140\140\140\140\334\u0150"+
    "\224\224\224\u0354\u0354\u0354\u0354\u0356\356\u013E\u013E\u013E\362\362\362"+
    "\362\344\344\344\344\344\344\344\344\344\344\344\u0358\346\346\346\346\344"+
    "\140\140\140\140\140\140\140\210\140\140\140\140\140\140\140\140\140\140\140"+
    "\140\140\140\140\u024A\210\210\210\144\146\u0310\140\140\140\140\140\u035A"+
    "\u035C\u0310\220\140\140\210\140\140\140\140\140\140\140\140\140\140\u0214"+
    "\u0214\u035E\u0214\u0214\140\140\362\362\362\362\362\362\362\362\362\362\362"+
    "\362\362\362\362\u01BC\334\220\224\224\224\u01AC\224\u01AC\224\u01B4\224\224"+
    "\224\224\224\224\224\224\224\224\224\u01E8\u01B0\u01AA\u016A\u016A\362\362"+
    "\u01C6\u01C6\u01C6\u01FA\u0360\362\362\362\224\224\224\224\224\224\224\224"+
    "\224\224\020\020\362\362\362\362\u01AE\224\224\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\u01AE\u01AE"+
    "\u01AE\u01AE\u01AE\u01AE\u01AE\u01AE\u0178\362\362\362\362\u013E\u01F6\u01F6"+
    "\u01F6\u01F6\u01F6\362\362\362\356\356\356\356\356\356\356\356\356\224\224"+
    "\224\u013E\u022A\u022A\362\u01FE\u01FE\u01FE\u01FE\u01FE\224\224\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\u0150\u0150\u01A2\356\u013E\224\224\224"+
    "\224\224\224\224\224\224\224\224\u01BA\u0150\u0150\u0150\u0150\u0150\u020A"+
    "\362\362\362\362\362\u0146\224\224\224\224\224\224\224\224\224\224\224\224"+
    "\224\224\u01BE\362\224\224\224\224\224\224\224\224\224\u01B4\u01AE\u0150\u0150"+
    "\u01AE\u01AA\u01AE\u0362\u013E\u013E\u013E\u013E\u013E\u013E\u013C\u01F6\u01F6"+
    "\u01F6\u01F6\u01F6\362\362\u013E\224\224\u01B4\u0218\224\224\224\224\u01F6"+
    "\u01F6\u01F6\u01F6\u01F6\224\224\u01BE\224\224\224\224\u01BA\u0150\u0150\u01AA"+
    "\u01B0\u01AA\u01B0\u01C0\362\362\362\362\224\u01BA\224\224\224\224\u01AA\362"+
    "\u01F6\u01F6\u01F6\u01F6\u01F6\362\u013E\u013E\224\224\224\224\224\224\224"+
    "\224\u0218\224\224\u01E6\u01FA\u0212\u0248\224\224\224\224\224\224\224\224"+
    "\224\u01D4\u0150\u01D4\u01BA\u01D4\224\224\u01A2\u01B4\u01BE\362\362\362\362"+
    "\362\362\362\362\362\362\362\u01BC\u023E\u013E\224\224\224\224\224\u01E8\u0150"+
    "\u01AE\u013E\u023E\u0364\u0194\362\362\362\362\u01BC\224\224\u01BE\u01BC\224"+
    "\224\u01BE\u01BC\224\224\u01BE\362\362\362\362\224\224\224\u01BE\224\224\224"+
    "\u01BE\210\210\210\210\210\210\210\210\210\210\210\210\210\210\210\210\210"+
    "\304\210\210\210\u0366\334\334\210\210\210\362\362\362\362\362\262\262\262"+
    "\262\262\262\262\262\224\u01E8\u01B0\u01AE\u01AA\u0368\u0210\362\u01F6\u01F6"+
    "\u01F6\u01F6\u01F6\362\362\362\224\224\362\362\362\362\362\362\224\224\224"+
    "\224\224\224\224\224\224\224\224\u01BE\362\u01BC\224\224\224\224\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\362\362"+
    "\u036A\u036A\u036A\u036A\u036A\u036A\u036A\u036A\u036A\u036A\u036A\u036A\u036A"+
    "\u036A\u036A\u036A\u036C\u036C\u036C\u036C\u036C\u036C\u036C\u036C\u036C\u036C"+
    "\u036C\u036C\u036C\u036C\u036C\u036C\u034C\u034C\u034C\u034C\u034C\u036E\u034C"+
    "\u034C\u034C\u0370\u034C\u034C\u0372\u034C\u034C\u034C\u034C\u034C\u034C\u034C"+
    "\u034C\u034C\u034C\u034C\u034C\u0374\u034C\u034C\u034C\u034C\u034C\u034C\u034C"+
    "\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u0376\u0378\u034C\u034C\u034C\u034C"+
    "\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C"+
    "\u034C\u034C\u034C\u037A\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\362"+
    "\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C"+
    "\u034C\u034C\u034C\u034C\u034C\u034C\u034C\u034C\362\362\362\262\262\262\u0246"+
    "\362\362\362\362\362\u031C\262\262\362\362\u037C\u037E\u0158\u0158\u0158\u0158"+
    "\u0380\u0158\u0158\u0158\u0158\u0158\u0158\u015A\u0158\u0158\u015A\u015A\u0158"+
    "\u037C\u015A\u0158\u0158\u0158\u0158\u0158\u0172\u0172\u0172\u0172\u0172\u0172"+
    "\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0382\u0382"+
    "\u0382\u0382\u0382\u0382\u0382\u0382\362\362\362\362\362\362\362\362\u0196"+
    "\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172"+
    "\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0384\362\362\362\362\362"+
    "\362\362\362\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172"+
    "\u0172\u0172\u0172\u0172\u0172\362\u0172\u0172\u0172\u0172\u0172\u0172\u0172"+
    "\u0172\u0172\u0172\u0172\362\362\362\362\362\362\362\362\362\362\362\362\362"+
    "\362\362\362\362\362\362\362\u0172\u0172\u0172\u0172\u0172\u0172\u0386\362"+
    "\356\356\356\356\356\356\356\356\020\020\020\u0388\u038A\362\362\362\356\356"+
    "\356\356\356\356\356\356\u0320\u038C\u038E\u0384\u0384\u0384\u0384\u0384\u0384"+
    "\u0384\u038A\u0388\u038A\020\u02A0\u0390\034\u0392\u0394\020\u0396\u030C\u030C"+
    "\u0398\020\u039A\u02E4\u039C\u039E\u029A\362\362\u0172\u0172\u01A6\u0172\u0172"+
    "\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172\u0172"+
    "\u0172\u0172\u0172\u0172\u01A6\u03A0\u0318\014\016\020\022\024\026\030\032"+
    "\032\032\032\032\034\036\040\054\056\056\056\056\056\056\056\056\056\056\056"+
    "\056\060\062\u02B0\u02A6\022\020\224\224\224\224\224\u0218\224\224\224\224"+
    "\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\224\342"+
    "\362\224\224\224\362\224\224\224\362\224\224\224\362\224\u01BE\362\072\u03A2"+
    "\u014C\u03A4\u02CC\u0162\u02E0\u01DA\362\362\362\362\u03A6\u03A8\u016A\362").toCharArray();

  // The A table has 938 entries for a total of 3752 bytes.

  static final int A[] = new int[938];
  static final String A_DATA =
    "\u4800\u100F\u4800\u100F\u4800\u100F\u5800\u400F\u5000\u400F\u5800\u400F\u6000"+
    "\u400F\u5000\u400F\u5000\u400F\u5000\u400F\u6000\u400C\u6800\030\u6800\030"+
    "\u2800\030\u2800\u601A\u2800\030\u6800\030\u6800\030\uE800\025\uE800\026\u6800"+
    "\030\u2000\031\u3800\030\u2000\024\u3800\030\u3800\030\u1800\u3609\u1800\u3609"+
    "\u3800\030\u6800\030\uE800\031\u6800\031\uE800\031\u6800\030\u6800\030\202"+
    "\u7FE1\202\u7FE1\202\u7FE1\202\u7FE1\uE800\025\u6800\030\uE800\026\u6800\033"+
    "\u6800\u5017\u6800\033\201\u7FE2\201\u7FE2\201\u7FE2\201\u7FE2\uE800\025\u6800"+
    "\031\uE800\026\u6800\031\u4800\u100F\u4800\u100F\u5000\u100F\u3800\014\u6800"+
    "\030\u2800\u601A\u2800\u601A\u6800\034\u6800\030\u6800\033\u6800\034\000\u7005"+
    "\uE800\035\u6800\031\u4800\u1010\u6800\034\u6800\033\u2800\034\u2800\031\u1800"+
    "\u060B\u1800\u060B\u6800\033\u07FD\u7002\u6800\033\u1800\u050B\000\u7005\uE800"+
    "\036\u6800\u080B\u6800\u080B\u6800\u080B\u6800\030\202\u7001\202\u7001\202"+
    "\u7001\u6800\031\202\u7001\u07FD\u7002\201\u7002\201\u7002\201\u7002\u6800"+
    "\031\201\u7002\u061D\u7002\006\u7001\005\u7002\u07FF\uF001\u03A1\u7002\000"+
    "\u7002\006\u7001\005\u7002\006\u7001\005\u7002\u07FD\u7002\u061E\u7001\006"+
    "\u7001\u04F5\u7002\u034A\u7001\u033A\u7001\006\u7001\005\u7002\u0336\u7001"+
    "\u0336\u7001\006\u7001\005\u7002\000\u7002\u013E\u7001\u032A\u7001\u032E\u7001"+
    "\006\u7001\u033E\u7001\u067D\u7002\u034E\u7001\u0346\u7001\u0575\u7002\000"+
    "\u7002\u034E\u7001\u0356\u7001\u05F9\u7002\u035A\u7001\u036A\u7001\006\u7001"+
    "\005\u7002\u036A\u7001\000\u7002\000\u7002\005\u7002\u0366\u7001\u0366\u7001"+
    "\006\u7001\005\u7002\u036E\u7001\000\u7002\000\u7005\000\u7002\u0721\u7002"+
    "\000\u7005\000\u7005\012\uF001\007\uF003\011\uF002\012\uF001\007\uF003\011"+
    "\uF002\011\uF002\006\u7001\005\u7002\u013D\u7002\u07FD\u7002\012\uF001\u067E"+
    "\u7001\u0722\u7001\u05FA\u7001\000\u7002\u07FE\u7001\006\u7001\005\u7002\u0576"+
    "\u7001\u07FE\u7001\u07FD\u7002\u07FD\u7002\006\u7001\005\u7002\u04F6\u7001"+
    "\u0116\u7001\u011E\u7001\u07FD\u7002\u07FD\u7002\u07FD\u7002\u0349\u7002\u0339"+
    "\u7002\000\u7002\u0335\u7002\u0335\u7002\000\u7002\u0329\u7002\000\u7002\u032D"+
    "\u7002\u07FD\u7002\000\u7002\u0335\u7002\u07FD\u7002\000\u7002\u033D\u7002"+
    "\000\u7002\u07FD\u7002\u0345\u7002\u034D\u7002\000\u7002\u034D\u7002\u0355"+
    "\u7002\000\u7002\000\u7002\u0359\u7002\u0369\u7002\000\u7002\000\u7002\u0369"+
    "\u7002\u0369\u7002\u0115\u7002\u0365\u7002\u0365\u7002\u011D\u7002\000\u7002"+
    "\u036D\u7002\000\u7002\000\u7005\000\u7002\000\u7004\000\u7004\000\u7004\u6800"+
    "\u7004\u6800\u7004\000\u7004\000\u7004\000\u7004\u6800\033\u6800\033\u6800"+
    "\u7004\u6800\u7004\000\u7004\u6800\033\u6800\u7004\u6800\033\000\u7004\u6800"+
    "\033\u4000\u3006\u4000\u3006\u4000\u3006\u46B1\u3006\u7800\000\u7800\000\000"+
    "\u7004\u05F9\u7002\u05F9\u7002\u05F9\u7002\u6800\030\u01D2\u7001\232\u7001"+
    "\u6800\030\226\u7001\226\u7001\226\u7001\u7800\000\u0102\u7001\u7800\000\376"+
    "\u7001\376\u7001\u07FD\u7002\202\u7001\u7800\000\202\u7001\231\u7002\225\u7002"+
    "\225\u7002\225\u7002\u07FD\u7002\201\u7002\175\u7002\201\u7002\u0101\u7002"+
    "\375\u7002\375\u7002\042\u7001\371\u7002\345\u7002\000\u7001\000\u7001\000"+
    "\u7001\275\u7002\331\u7002\041\u7002\u0159\u7002\u0141\u7002\u07E5\u7002\u01D1"+
    "\u7002\u0712\u7001\u0181\u7002\u6800\031\006\u7001\005\u7002\u07E6\u7001\000"+
    "\u7002\u05FA\u7001\u05FA\u7001\u05FA\u7001\u0142\u7001\u0142\u7001\u0141\u7002"+
    "\u0141\u7002\000\034\u4000\u3006\u4000\007\u4000\007\076\u7001\006\u7001\005"+
    "\u7002\075\u7002\u7800\000\302\u7001\302\u7001\302\u7001\302\u7001\u7800\000"+
    "\u7800\000\000\u7004\000\030\000\030\u7800\000\301\u7002\301\u7002\301\u7002"+
    "\301\u7002\u07FD\u7002\u7800\000\000\030\u6800\024\u7800\000\u7800\000\u6800"+
    "\034\u6800\034\u2800\u601A\u7800\000\u4000\u3006\u4000\u3006\u4000\u3006\u0800"+
    "\024\u4000\u3006\u0800\030\u4000\u3006\u4000\u3006\u0800\030\u0800\u7005\u0800"+
    "\u7005\u0800\u7005\u7800\000\u0800\u7005\u0800\030\u0800\030\u7800\000\u3000"+
    "\u1010\u3000\u1010\u6800\031\u6800\031\u1000\031\u2800\030\u2800\030\u1000"+
    "\u601A\u3800\030\u1000\030\u6800\034\u6800\034\u4000\u3006\u1000\030\u1000"+
    "\u1010\u7800\000\u1000\030\u1000\030\u1000\u7005\u1000\u7005\u1000\u7004\u1000"+
    "\u7005\u1000\u7005\u4000\u3006\u4000\u3006\u4000\u3006\u3000\u3409\u3000\u3409"+
    "\u2800\030\u3000\030\u3000\030\u1000\030\u4000\u3006\u1000\u7005\u1000\030"+
    "\u1000\u7005\u4000\u3006\u3000\u1010\u6800\034\u4000\u3006\u4000\u3006\u1000"+
    "\u7004\u1000\u7004\u4000\u3006\u4000\u3006\u6800\034\u1000\u7005\u1000\034"+
    "\u1000\034\u1000\u7005\u7800\000\u1000\u1010\u4000\u3006\u7800\000\u7800\000"+
    "\u1000\u7005\u0800\u3409\u0800\u3409\u0800\u7005\u4000\u3006\u0800\u7004\u0800"+
    "\u7004\u0800\u7004\u7800\000\u0800\u7004\u4000\u3006\u4000\u3006\u4000\u3006"+
    "\u0800\030\u0800\030\u1000\u7005\u7800\000\u3000\u1010\u4000\u3006\u4000\u3006"+
    "\000\u3008\u4000\u3006\000\u7005\000\u3008\000\u3008\000\u3008\u4000\u3006"+
    "\000\u3008\u4000\u3006\000\u7005\u4000\u3006\000\u3749\000\u3749\000\030\000"+
    "\u7004\000\u7005\u4000\u3006\u7800\000\000\u7005\000\u7005\u7800\000\u4000"+
    "\u3006\u7800\000\u7800\000\000\u3008\000\u3008\u7800\000\000\u080B\000\u080B"+
    "\000\u080B\000\u06EB\000\034\u2800\u601A\000\u7005\000\030\u7800\000\u4000"+
    "\u3006\000\030\u2800\u601A\000\034\000\u7005\u4000\u3006\000\u7005\000\u074B"+
    "\000\u080B\000\u080B\u6800\034\u6800\034\u7800\000\u6800\u050B\u6800\u050B"+
    "\u6800\u04AB\u6800\u04AB\u6800\u04AB\000\034\000\u3008\000\u3006\000\u3006"+
    "\000\u3008\000\u7005\000\034\000\u7005\000\u3008\000\u080B\000\u7005\000\u080B"+
    "\000\034\000\030\u7800\000\u7800\000\u2800\u601A\000\u7004\u4000\u3006\u4000"+
    "\u3006\000\030\000\u3609\000\u3609\000\u7004\u7800\000\000\034\000\034\000"+
    "\030\000\034\000\u3409\000\u3409\000\u3008\000\u3008\u4000\u3006\000\034\000"+
    "\034\u7800\000\000\034\000\030\u4000\u3006\000\u3008\000\u3008\000\u3008\000"+
    "\u3008\000\u7005\u4000\u3006\000\u3008\000\u3008\u4000\u3006\000\u7005\000"+
    "\u3008\u07FE\u7001\u07FE\u7001\u7800\000\u07FE\u7001\000\u7004\000\u7005\000"+
    "\030\000\u070B\000\u070B\000\u070B\000\u070B\000\u042B\000\u054B\000\u080B"+
    "\000\u080B\u7800\000\042\u7001\042\u7001\041\u7002\041\u7002\u6800\024\000"+
    "\u7005\000\030\000\u7005\u6000\u400C\000\u7005\000\u7005\uE800\025\uE800\026"+
    "\u7800\000\000\u746A\000\u746A\000\u746A\000\u7005\u6800\u060B\u6800\u060B"+
    "\u6800\024\u6800\030\u6800\030\u4000\u3006\u4800\u1010\u7800\000\000\u7005"+
    "\000\u7004\000\u04EB\u7800\000\u4000\007\u7800\000\000\u3008\000\u7005\u07FD"+
    "\u7002\u7800\000\u4000\u3006\000\u3008\000\u7004\000\u7002\000\u7004\u07FD"+
    "\u7002\000\u7002\000\u7004\u07FD\u7002\355\u7002\u07FE\u7001\000\u7002\u07E1"+
    "\u7002\u07E1\u7002\u07E2\u7001\u07E2\u7001\u07FD\u7002\u07E1\u7002\u7800\000"+
    "\u07E2\u7001\u06D9\u7002\u06D9\u7002\u06A9\u7002\u06A9\u7002\u0671\u7002\u0671"+
    "\u7002\u0601\u7002\u0601\u7002\u0641\u7002\u0641\u7002\u0609\u7002\u0609\u7002"+
    "\u07FF\uF003\u07FF\uF003\u06DA\u7001\u06DA\u7001\u07FF\uF003\u6800\033\u07FD"+
    "\u7002\u6800\033\u06AA\u7001\u06AA\u7001\u0672\u7001\u0672\u7001\u7800\000"+
    "\u6800\033\u07FD\u7002\u07E5\u7002\u0642\u7001\u0642\u7001\u07E6\u7001\u6800"+
    "\033\u0602\u7001\u0602\u7001\u060A\u7001\u060A\u7001\u6800\033\u7800\000\u6000"+
    "\u400C\u6000\u400C\u6000\u400C\u6000\014\u6000\u400C\u4800\u1010\u4800\u1010"+
    "\u4800\u1010\000\u1010\u0800\u1010\u6800\024\u6800\024\u6800\035\u6800\036"+
    "\u6800\025\u6800\035\u6000\u400D\u5000\u400E\u7800\u1010\u7800\u1010\u7800"+
    "\u1010\u3800\014\u2800\030\u2800\030\u2800\030\u6800\030\u6800\030\uE800\035"+
    "\uE800\036\u6800\030\u6800\030\u6800\u5017\u6800\u5017\u6800\030\u3800\031"+
    "\uE800\025\uE800\026\u6800\030\u6800\031\u6800\030\u6800\030\u6000\u400C\u1800"+
    "\u060B\000\u7004\u2000\031\u2000\031\u6800\031\uE800\025\uE800\026\000\u7004"+
    "\u1800\u040B\u1800\u040B\000\u7004\u7800\000\u4000\u3006\u4000\007\u4000\007"+
    "\u4000\u3006\000\u7001\u6800\034\u6800\034\000\u7001\000\u7002\000\u7001\000"+
    "\u7001\000\u7002\u6800\031\000\u7001\u07FE\u7001\u6800\034\u2800\034\000\u7002"+
    "\162\u7001\000\u7001\u6800\034\u6800\031\161\u7002\000\034\102\u742A\102\u742A"+
    "\102\u780A\102\u780A\101\u762A\101\u762A\101\u780A\101\u780A\000\u780A\000"+
    "\u780A\000\u780A\006\u7001\005\u7002\000\u742A\000\u780A\u6800\u06EB\u6800"+
    "\031\u6800\034\u6800\031\uE800\031\uE800\031\uE800\031\u2000\031\u2800\031"+
    "\u6800\034\uE800\025\uE800\026\u6800\034\000\034\u6800\034\u6800\034\000\034"+
    "\u6800\u042B\u6800\u042B\u6800\u05AB\u6800\u05AB\u1800\u072B\u1800\u072B\152"+
    "\034\152\034\151\034\151\034\u6800\u06CB\u6800\u040B\u6800\u040B\u6800\u040B"+
    "\u6800\u040B\u6800\u058B\u6800\u058B\u6800\u058B\u6800\u058B\u6800\u042B\u6800"+
    "\u056B\u6800\u056B\u6800\u06EB\u6800\u06EB\uE800\031\uE800\025\uE800\026\u6800"+
    "\031\uE800\026\uE800\025\301\u7002\u7800\000\005\u7002\u07FE\u7001\000\u7002"+
    "\u6800\034\u6800\034\006\u7001\005\u7002\u4000\u3006\u7800\000\u6800\030\u6800"+
    "\030\u6800\u080B\u7800\000\u07FD\u7002\uE800\035\uE800\036\u6800\030\u6800"+
    "\024\u6800\030\u6800\u7004\u6800\025\u6800\030\u6800\034\000\u7004\000\u7005"+
    "\000\u772A\u6800\024\u6800\025\u6800\026\u6800\026\u6800\034\000\u740A\000"+
    "\u740A\000\u740A\u6800\024\000\u7004\000\u764A\000\u776A\000\u748A\000\u7004"+
    "\000\u7005\u6800\030\u4000\u3006\u6800\033\u6800\033\000\u7004\000\u05EB\000"+
    "\u05EB\000\u042B\000\u042B\000\u044B\000\u056B\000\u068B\000\u080B\u6800\034"+
    "\u6800\u048B\u6800\u048B\u6800\u048B\000\034\u6800\u080B\000\u7005\000\u7005"+
    "\000\u7005\u7800\000\000\u7004\u6800\030\u4000\007\u6800\030\000\u776A\000"+
    "\u776A\000\u776A\000\u762A\u6800\033\u6800\u7004\u6800\u7004\000\033\000\033"+
    "\006\u7001\u07FE\u7001\u7800\000\u2800\u601A\u2800\034\000\u3008\000\030\000"+
    "\u7004\000\u3008\000\u7002\000\033\000\u3008\000\030\000\023\000\023\000\022"+
    "\000\022\000\u7005\000\u7705\000\u7005\000\u76E5\000\u7545\000\u7005\000\u75C5"+
    "\000\u7005\000\u7005\000\u76A5\000\u7005\000\u7665\000\u7005\000\u75A5\u7800"+
    "\000\u0800\u7005\u4000\u3006\u0800\u7005\u0800\u7005\u2000\031\u1000\033\u1000"+
    "\033\u6800\026\u6800\025\u1000\u601A\u6800\034\u6800\030\u6800\025\u6800\026"+
    "\u6800\030\u6800\024\u6800\u5017\u6800\u5017\u6800\025\u6800\u5017\u6800\u5017"+
    "\u3800\030\u7800\000\u6800\030\u3800\030\u6800\024\uE800\025\uE800\026\u2800"+
    "\030\u2000\031\u2000\024\u6800\031\u7800\000\u6800\030\u2800\u601A\u7800\000"+
    "\u4800\u1010\u6800\031\u6800\033\u2800\u601A\u7800\000\u7800\000\u6800\u1010"+
    "\u6800\u1010\u6800\u1010";

  // The B table has 938 entries for a total of 1876 bytes.

  static final char B[] = (
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\001\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\001\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\001\001\001\000\000\000\000\000"+
    "\000\000\000\000\001\000\000\000\000\000\000\000\000\005\000\000\001\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\004\004\000\004\000\004"+
    "\004\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\004\000\000\000\000\000\000\000\000\000\000\004\000\004\000\000"+
    "\000\000\000\000\004\000\000\000\004\000\000\000\004\000\000\004\004\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\004"+
    "\004\000\000\000\000\000\000\004\004\004\000\000\004\004\004\004\004\000\000"+
    "\000\000\000\000\000\000\004\000\000\000\000\004\000\000\004\004\000\000\000"+
    "\000\000\000\000\000\000\000\004\000\000\000\000\004\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\004\004\004\004\000\000\000\004\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\004\004\000\000\000\004\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\004\000\000\000\000\000\001\000\001"+
    "\000\000\001\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\001\000\000\000\000\000\001\000\000\001\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\002\002"+
    "\002\002\001\001\001\001\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\006\006"+
    "\005\005\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\020\020\000\000\000\000\000\020\020"+
    "\020\000\000\020\020\020\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\020\020\020\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\004\000\000\004\000"+
    "\000\000\000\000\020\020\020\020\020\020\020\020\020\020\020\020\020\020\000"+
    "\000\004\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"+
    "\000\000\000\000\000\000\000").toCharArray();

  // In all, the character property tables require 19624 bytes.

    static {
            charMap = new char[][][] {
        { {'\u00DF'}, {'\u0053', '\u0053', } },
        { {'\u0130'}, {'\u0130', } },
        { {'\u0149'}, {'\u02BC', '\u004E', } },
        { {'\u01F0'}, {'\u004A', '\u030C', } },
        { {'\u0390'}, {'\u0399', '\u0308', '\u0301', } },
        { {'\u03B0'}, {'\u03A5', '\u0308', '\u0301', } },
        { {'\u0587'}, {'\u0535', '\u0552', } },
        { {'\u1E96'}, {'\u0048', '\u0331', } },
        { {'\u1E97'}, {'\u0054', '\u0308', } },
        { {'\u1E98'}, {'\u0057', '\u030A', } },
        { {'\u1E99'}, {'\u0059', '\u030A', } },
        { {'\u1E9A'}, {'\u0041', '\u02BE', } },
        { {'\u1F50'}, {'\u03A5', '\u0313', } },
        { {'\u1F52'}, {'\u03A5', '\u0313', '\u0300', } },
        { {'\u1F54'}, {'\u03A5', '\u0313', '\u0301', } },
        { {'\u1F56'}, {'\u03A5', '\u0313', '\u0342', } },
        { {'\u1F80'}, {'\u1F08', '\u0399', } },
        { {'\u1F81'}, {'\u1F09', '\u0399', } },
        { {'\u1F82'}, {'\u1F0A', '\u0399', } },
        { {'\u1F83'}, {'\u1F0B', '\u0399', } },
        { {'\u1F84'}, {'\u1F0C', '\u0399', } },
        { {'\u1F85'}, {'\u1F0D', '\u0399', } },
        { {'\u1F86'}, {'\u1F0E', '\u0399', } },
        { {'\u1F87'}, {'\u1F0F', '\u0399', } },
        { {'\u1F88'}, {'\u1F08', '\u0399', } },
        { {'\u1F89'}, {'\u1F09', '\u0399', } },
        { {'\u1F8A'}, {'\u1F0A', '\u0399', } },
        { {'\u1F8B'}, {'\u1F0B', '\u0399', } },
        { {'\u1F8C'}, {'\u1F0C', '\u0399', } },
        { {'\u1F8D'}, {'\u1F0D', '\u0399', } },
        { {'\u1F8E'}, {'\u1F0E', '\u0399', } },
        { {'\u1F8F'}, {'\u1F0F', '\u0399', } },
        { {'\u1F90'}, {'\u1F28', '\u0399', } },
        { {'\u1F91'}, {'\u1F29', '\u0399', } },
        { {'\u1F92'}, {'\u1F2A', '\u0399', } },
        { {'\u1F93'}, {'\u1F2B', '\u0399', } },
        { {'\u1F94'}, {'\u1F2C', '\u0399', } },
        { {'\u1F95'}, {'\u1F2D', '\u0399', } },
        { {'\u1F96'}, {'\u1F2E', '\u0399', } },
        { {'\u1F97'}, {'\u1F2F', '\u0399', } },
        { {'\u1F98'}, {'\u1F28', '\u0399', } },
        { {'\u1F99'}, {'\u1F29', '\u0399', } },
        { {'\u1F9A'}, {'\u1F2A', '\u0399', } },
        { {'\u1F9B'}, {'\u1F2B', '\u0399', } },
        { {'\u1F9C'}, {'\u1F2C', '\u0399', } },
        { {'\u1F9D'}, {'\u1F2D', '\u0399', } },
        { {'\u1F9E'}, {'\u1F2E', '\u0399', } },
        { {'\u1F9F'}, {'\u1F2F', '\u0399', } },
        { {'\u1FA0'}, {'\u1F68', '\u0399', } },
        { {'\u1FA1'}, {'\u1F69', '\u0399', } },
        { {'\u1FA2'}, {'\u1F6A', '\u0399', } },
        { {'\u1FA3'}, {'\u1F6B', '\u0399', } },
        { {'\u1FA4'}, {'\u1F6C', '\u0399', } },
        { {'\u1FA5'}, {'\u1F6D', '\u0399', } },
        { {'\u1FA6'}, {'\u1F6E', '\u0399', } },
        { {'\u1FA7'}, {'\u1F6F', '\u0399', } },
        { {'\u1FA8'}, {'\u1F68', '\u0399', } },
        { {'\u1FA9'}, {'\u1F69', '\u0399', } },
        { {'\u1FAA'}, {'\u1F6A', '\u0399', } },
        { {'\u1FAB'}, {'\u1F6B', '\u0399', } },
        { {'\u1FAC'}, {'\u1F6C', '\u0399', } },
        { {'\u1FAD'}, {'\u1F6D', '\u0399', } },
        { {'\u1FAE'}, {'\u1F6E', '\u0399', } },
        { {'\u1FAF'}, {'\u1F6F', '\u0399', } },
        { {'\u1FB2'}, {'\u1FBA', '\u0399', } },
        { {'\u1FB3'}, {'\u0391', '\u0399', } },
        { {'\u1FB4'}, {'\u0386', '\u0399', } },
        { {'\u1FB6'}, {'\u0391', '\u0342', } },
        { {'\u1FB7'}, {'\u0391', '\u0342', '\u0399', } },
        { {'\u1FBC'}, {'\u0391', '\u0399', } },
        { {'\u1FC2'}, {'\u1FCA', '\u0399', } },
        { {'\u1FC3'}, {'\u0397', '\u0399', } },
        { {'\u1FC4'}, {'\u0389', '\u0399', } },
        { {'\u1FC6'}, {'\u0397', '\u0342', } },
        { {'\u1FC7'}, {'\u0397', '\u0342', '\u0399', } },
        { {'\u1FCC'}, {'\u0397', '\u0399', } },
        { {'\u1FD2'}, {'\u0399', '\u0308', '\u0300', } },
        { {'\u1FD3'}, {'\u0399', '\u0308', '\u0301', } },
        { {'\u1FD6'}, {'\u0399', '\u0342', } },
        { {'\u1FD7'}, {'\u0399', '\u0308', '\u0342', } },
        { {'\u1FE2'}, {'\u03A5', '\u0308', '\u0300', } },
        { {'\u1FE3'}, {'\u03A5', '\u0308', '\u0301', } },
        { {'\u1FE4'}, {'\u03A1', '\u0313', } },
        { {'\u1FE6'}, {'\u03A5', '\u0342', } },
        { {'\u1FE7'}, {'\u03A5', '\u0308', '\u0342', } },
        { {'\u1FF2'}, {'\u1FFA', '\u0399', } },
        { {'\u1FF3'}, {'\u03A9', '\u0399', } },
        { {'\u1FF4'}, {'\u038F', '\u0399', } },
        { {'\u1FF6'}, {'\u03A9', '\u0342', } },
        { {'\u1FF7'}, {'\u03A9', '\u0342', '\u0399', } },
        { {'\u1FFC'}, {'\u03A9', '\u0399', } },
        { {'\uFB00'}, {'\u0046', '\u0046', } },
        { {'\uFB01'}, {'\u0046', '\u0049', } },
        { {'\uFB02'}, {'\u0046', '\u004C', } },
        { {'\uFB03'}, {'\u0046', '\u0046', '\u0049', } },
        { {'\uFB04'}, {'\u0046', '\u0046', '\u004C', } },
        { {'\uFB05'}, {'\u0053', '\u0054', } },
        { {'\uFB06'}, {'\u0053', '\u0054', } },
        { {'\uFB13'}, {'\u0544', '\u0546', } },
        { {'\uFB14'}, {'\u0544', '\u0535', } },
        { {'\uFB15'}, {'\u0544', '\u053B', } },
        { {'\uFB16'}, {'\u054E', '\u0546', } },
        { {'\uFB17'}, {'\u0544', '\u053D', } },
    };
        { // THIS CODE WAS AUTOMATICALLY CREATED BY GenerateCharacter:
            char[] data = A_DATA.toCharArray();
            assert (data.length == (938 * 2));
            int i = 0, j = 0;
            while (i < (938 * 2)) {
                int entry = data[i++] << 16;
                A[j++] = entry | data[i++];
            }
        }

    }        
}
