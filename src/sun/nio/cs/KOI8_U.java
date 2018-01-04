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

package sun.nio.cs;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import sun.nio.cs.StandardCharsets;
import sun.nio.cs.SingleByte;
import sun.nio.cs.HistoricallyNamedCharset;
import static sun.nio.cs.CharsetMapping.*;

public class KOI8_U extends Charset implements HistoricallyNamedCharset
{
    public KOI8_U() {
        super("KOI8-U", StandardCharsets.aliases_KOI8_U());
    }

    public String historicalName() {
        return "KOI8_U";
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII")) || (cs instanceof KOI8_U));
    }

    public CharsetDecoder newDecoder() {
        return new SingleByte.Decoder(this, b2c, true);
    }

    public CharsetEncoder newEncoder() {
        return new SingleByte.Encoder(this, c2b, c2bIndex, true);
    }

    private final static String b2cTable = 
        "\u2500\u2502\u250C\u2510\u2514\u2518\u251C\u2524" +      // 0x80 - 0x87
        "\u252C\u2534\u253C\u2580\u2584\u2588\u258C\u2590" +      // 0x88 - 0x8f
        "\u2591\u2592\u2593\u2320\u25A0\u2219\u221A\u2248" +      // 0x90 - 0x97
        "\u2264\u2265\u00A0\u2321\u00B0\u00B2\u00B7\u00F7" +      // 0x98 - 0x9f
        "\u2550\u2551\u2552\u0451\u0454\u2554\u0456\u0457" +      // 0xa0 - 0xa7
        "\u2557\u2558\u2559\u255A\u255B\u0491\u255D\u255E" +      // 0xa8 - 0xaf
        "\u255F\u2560\u2561\u0401\u0404\u2563\u0406\u0407" +      // 0xb0 - 0xb7
        "\u2566\u2567\u2568\u2569\u256A\u0490\u256C\u00A9" +      // 0xb8 - 0xbf
        "\u044E\u0430\u0431\u0446\u0434\u0435\u0444\u0433" +      // 0xc0 - 0xc7
        "\u0445\u0438\u0439\u043A\u043B\u043C\u043D\u043E" +      // 0xc8 - 0xcf
        "\u043F\u044F\u0440\u0441\u0442\u0443\u0436\u0432" +      // 0xd0 - 0xd7
        "\u044C\u044B\u0437\u0448\u044D\u0449\u0447\u044A" +      // 0xd8 - 0xdf
        "\u042E\u0410\u0411\u0426\u0414\u0415\u0424\u0413" +      // 0xe0 - 0xe7
        "\u0425\u0418\u0419\u041A\u041B\u041C\u041D\u041E" +      // 0xe8 - 0xef
        "\u041F\u042F\u0420\u0421\u0422\u0423\u0416\u0412" +      // 0xf0 - 0xf7
        "\u042C\u042B\u0417\u0428\u042D\u0429\u0427\u042A" +      // 0xf8 - 0xff
        "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007" +      // 0x00 - 0x07
        "\b\t\n\u000B\f\r\u000E\u000F" +      // 0x08 - 0x0f
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +      // 0x10 - 0x17
        "\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F" +      // 0x18 - 0x1f
        "\u0020\u0021\"\u0023\u0024\u0025\u0026\'" +      // 0x20 - 0x27
        "\u0028\u0029\u002A\u002B\u002C\u002D\u002E\u002F" +      // 0x28 - 0x2f
        "\u0030\u0031\u0032\u0033\u0034\u0035\u0036\u0037" +      // 0x30 - 0x37
        "\u0038\u0039\u003A\u003B\u003C\u003D\u003E\u003F" +      // 0x38 - 0x3f
        "\u0040\u0041\u0042\u0043\u0044\u0045\u0046\u0047" +      // 0x40 - 0x47
        "\u0048\u0049\u004A\u004B\u004C\u004D\u004E\u004F" +      // 0x48 - 0x4f
        "\u0050\u0051\u0052\u0053\u0054\u0055\u0056\u0057" +      // 0x50 - 0x57
        "\u0058\u0059\u005A\u005B\\\u005D\u005E\u005F" +      // 0x58 - 0x5f
        "\u0060\u0061\u0062\u0063\u0064\u0065\u0066\u0067" +      // 0x60 - 0x67
        "\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F" +      // 0x68 - 0x6f
        "\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077" +      // 0x70 - 0x77
        "\u0078\u0079\u007A\u007B\u007C\u007D\u007E\u007F" ;      // 0x78 - 0x7f


    private final static char[] b2c = b2cTable.toCharArray();
    private final static char[] c2b = new char[0x500];
    private final static char[] c2bIndex = new char[0x100];

    static {
        char[] b2cMap = b2c;
        char[] c2bNR = null;
        SingleByte.initC2B(b2cMap, c2bNR, c2b, c2bIndex);
    }
}
