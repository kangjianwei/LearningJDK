/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs.ext;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import sun.nio.cs.StandardCharsets;
import sun.nio.cs.SingleByte;
import sun.nio.cs.HistoricallyNamedCharset;
import static sun.nio.cs.CharsetMapping.*;

public class IBM420 extends Charset implements HistoricallyNamedCharset
{
    public IBM420() {
        super("IBM420", ExtendedCharsets.aliasesFor("IBM420"));
    }

    public String historicalName() {
        return "Cp420";
    }

    public boolean contains(Charset cs) {
        return (cs instanceof IBM420);
    }

    public CharsetDecoder newDecoder() {
        return new SingleByte.Decoder(this, b2c, false);
    }

    public CharsetEncoder newEncoder() {
        return new SingleByte.Encoder(this, c2b, c2bIndex, false);
    }

    private final static String b2cTable = 
        "\u0634\u0061\u0062\u0063\u0064\u0065\u0066\u0067" +      // 0x80 - 0x87
        "\u0068\u0069\uFEB7\u0635\uFEBB\u0636\uFEBF\u0637" +      // 0x88 - 0x8f
        "\u0638\u006A\u006B\u006C\u006D\u006E\u006F\u0070" +      // 0x90 - 0x97
        "\u0071\u0072\u0639\uFECA\uFECB\uFECC\u063A\uFECE" +      // 0x98 - 0x9f
        "\uFECF\u00F7\u0073\u0074\u0075\u0076\u0077\u0078" +      // 0xa0 - 0xa7
        "\u0079\u007A\uFED0\u0641\uFED3\u0642\uFED7\u0643" +      // 0xa8 - 0xaf
        "\uFEDB\u0644\uFEF5\uFEF6\uFEF7\uFEF8\uFFFD\uFFFD" +      // 0xb0 - 0xb7
        "\uFEFB\uFEFC\uFEDF\u0645\uFEE3\u0646\uFEE7\u0647" +      // 0xb8 - 0xbf
        "\u061B\u0041\u0042\u0043\u0044\u0045\u0046\u0047" +      // 0xc0 - 0xc7
        "\u0048\u0049\u00AD\uFEEB\uFFFD\uFEEC\uFFFD\u0648" +      // 0xc8 - 0xcf
        "\u061F\u004A\u004B\u004C\u004D\u004E\u004F\u0050" +      // 0xd0 - 0xd7
        "\u0051\u0052\u0649\uFEF0\u064A\uFEF2\uFEF3\u0660" +      // 0xd8 - 0xdf
        "\u00D7\uFFFD\u0053\u0054\u0055\u0056\u0057\u0058" +      // 0xe0 - 0xe7
        "\u0059\u005A\u0661\u0662\uFFFD\u0663\u0664\u0665" +      // 0xe8 - 0xef
        "\u0030\u0031\u0032\u0033\u0034\u0035\u0036\u0037" +      // 0xf0 - 0xf7
        "\u0038\u0039\uFFFD\u0666\u0667\u0668\u0669\u009F" +      // 0xf8 - 0xff
        "\u0000\u0001\u0002\u0003\u009C\t\u0086\u007F" +      // 0x00 - 0x07
        "\u0097\u008D\u008E\u000B\f\r\u000E\u000F" +      // 0x08 - 0x0f
        "\u0010\u0011\u0012\u0013\u009D\n\b\u0087" +      // 0x10 - 0x17
        "\u0018\u0019\u0092\u008F\u001C\u001D\u001E\u001F" +      // 0x18 - 0x1f
        "\u0080\u0081\u0082\u0083\u0084\n\u0017\u001B" +      // 0x20 - 0x27
        "\u0088\u0089\u008A\u008B\u008C\u0005\u0006\u0007" +      // 0x28 - 0x2f
        "\u0090\u0091\u0016\u0093\u0094\u0095\u0096\u0004" +      // 0x30 - 0x37
        "\u0098\u0099\u009A\u009B\u0014\u0015\u009E\u001A" +      // 0x38 - 0x3f
        "\u0020\u00A0\u0651\uFE7D\u0640\u200B\u0621\u0622" +      // 0x40 - 0x47
        "\uFE82\u0623\u00A2\u002E\u003C\u0028\u002B\u007C" +      // 0x48 - 0x4f
        "\u0026\uFE84\u0624\uFFFD\uFFFD\u0626\u0627\uFE8E" +      // 0x50 - 0x57
        "\u0628\uFE91\u0021\u0024\u002A\u0029\u003B\u00AC" +      // 0x58 - 0x5f
        "\u002D\u002F\u0629\u062A\uFE97\u062B\uFE9B\u062C" +      // 0x60 - 0x67
        "\uFE9F\u062D\u00A6\u002C\u0025\u005F\u003E\u003F" +      // 0x68 - 0x6f
        "\uFEA3\u062E\uFEA7\u062F\u0630\u0631\u0632\u0633" +      // 0x70 - 0x77
        "\uFEB3\u060C\u003A\u0023\u0040\'\u003D\"" ;      // 0x78 - 0x7f


    private final static char[] b2c = b2cTable.toCharArray();
    private final static char[] c2b = new char[0x500];
    private final static char[] c2bIndex = new char[0x100];

    static {
        char[] b2cMap = b2c;
        char[] c2bNR = null;
        // remove non-roundtrip entries
        b2cMap = b2cTable.toCharArray();
        b2cMap[165] = UNMAPPABLE_DECODING;

        // non-roundtrip c2b only entries
        c2bNR = (
        "\u0015\u0085\u0042\uFE7C\u0046\uFE80\u0047\uFE81" +
        "\u0049\uFE83\u004B\u066C\u004B\uFF0E\u004C\uFF1C" +
        "\u004D\uFF08\u004E\uFF0B\u004F\uFF5C\u0050\uFF06" +
        "\u0052\uFE85\u0052\uFE86\u0055\uFE89\u0055\uFE8A" +
        "\u0055\uFE8B\u0055\uFE8C\u0056\u0625\u0056\uFE87" +
        "\u0056\uFE8D\u0057\uFE88\u0058\uFE8F\u0058\uFE90" +
        "\u0059\uFE92\u005A\uFF01\u005B\uFF04\\\u066D" +
        "\\\uFF0A\u005D\uFF09\u005E\uFF1B\u0060\uFF0D" +
        "\u0061\uFF0F\u0062\uFE93\u0062\uFE94\u0063\uFE95" +
        "\u0063\uFE96\u0064\uFE98\u0065\uFE99\u0065\uFE9A" +
        "\u0066\uFE9C\u0067\uFE9D\u0067\uFE9E\u0068\uFEA0" +
        "\u0069\uFEA1\u0069\uFEA2\u006B\u066B\u006B\uFF0C" +
        "\u006C\u066A\u006C\uFF05\u006D\uFF3F\u006E\uFF1E" +
        "\u006F\uFF1F\u0070\uFEA4\u0071\uFEA5\u0071\uFEA6" +
        "\u0072\uFEA8\u0073\uFEA9\u0073\uFEAA\u0074\uFEAB" +
        "\u0074\uFEAC\u0075\uFEAD\u0075\uFEAE\u0076\uFEAF" +
        "\u0076\uFEB0\u0077\uFEB1\u0077\uFEB2\u0078\uFEB4" +
        "\u007A\uFF1A\u007B\uFF03\u007C\uFF20\u007D\uFF07" +
        "\u007E\uFF1D\u007F\uFF02\u0080\uFEB5\u0080\uFEB6" +
        "\u0081\uFF41\u0082\uFF42\u0083\uFF43\u0084\uFF44" +
        "\u0085\uFF45\u0086\uFF46\u0087\uFF47\u0088\uFF48" +
        "\u0089\uFF49\u008A\uFEB8\u008B\uFEB9\u008B\uFEBA" +
        "\u008C\uFEBC\u008D\uFEBD\u008D\uFEBE\u008E\uFEC0" +
        "\u008F\uFEC1\u008F\uFEC2\u008F\uFEC3\u008F\uFEC4" +
        "\u0090\uFEC5\u0090\uFEC6\u0090\uFEC7\u0090\uFEC8" +
        "\u0091\uFF4A\u0092\uFF4B\u0093\uFF4C\u0094\uFF4D" +
        "\u0095\uFF4E\u0096\uFF4F\u0097\uFF50\u0098\uFF51" +
        "\u0099\uFF52\u009A\uFEC9\u009E\uFECD\u00A2\uFF53" +
        "\u00A3\uFF54\u00A4\uFF55\u00A5\uFF56\u00A6\uFF57" +
        "\u00A7\uFF58\u00A8\uFF59\u00A9\uFF5A\u00AB\uFED1" +
        "\u00AB\uFED2\u00AC\uFED4\u00AD\uFED5\u00AD\uFED6" +
        "\u00AE\uFED8\u00AF\uFED9\u00AF\uFEDA\u00B0\uFEDC" +
        "\u00B1\uFEDD\u00B1\uFEDE\u00B8\uFEF9\u00B9\uFEFA" +
        "\u00BA\uFEE0\u00BB\uFEE1\u00BB\uFEE2\u00BC\uFEE4" +
        "\u00BD\uFEE5\u00BD\uFEE6\u00BE\uFEE8\u00BF\uFEE9" +
        "\u00BF\uFEEA\u00C1\uFF21\u00C2\uFF22\u00C3\uFF23" +
        "\u00C4\uFF24\u00C5\uFF25\u00C6\uFF26\u00C7\uFF27" +
        "\u00C8\uFF28\u00C9\uFF29\u00CF\uFEED\u00CF\uFEEE" +
        "\u00D1\uFF2A\u00D2\uFF2B\u00D3\uFF2C\u00D4\uFF2D" +
        "\u00D5\uFF2E\u00D6\uFF2F\u00D7\uFF30\u00D8\uFF31" +
        "\u00D9\uFF32\u00DA\uFEEF\u00DC\uFEF1\u00DE\uFEF4" +
        "\u00E2\uFF33\u00E3\uFF34\u00E4\uFF35\u00E5\uFF36" +
        "\u00E6\uFF37\u00E7\uFF38\u00E8\uFF39\u00E9\uFF3A" +
        "\u00F0\uFF10\u00F1\uFF11\u00F2\uFF12\u00F3\uFF13" +
        "\u00F4\uFF14\u00F5\uFF15\u00F6\uFF16\u00F7\uFF17" +
        "\u00F8\uFF18\u00F9\uFF19").toCharArray();

        SingleByte.initC2B(b2cMap, c2bNR, c2b, c2bIndex);
    }
}
