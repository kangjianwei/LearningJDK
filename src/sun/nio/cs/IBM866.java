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

public class IBM866 extends Charset implements HistoricallyNamedCharset
{
    public IBM866() {
        super("IBM866", StandardCharsets.aliases_IBM866());
    }

    public String historicalName() {
        return "Cp866";
    }

    public boolean contains(Charset cs) {
        return (cs instanceof IBM866);
    }

    public CharsetDecoder newDecoder() {
        return new SingleByte.Decoder(this, b2c, false);
    }

    public CharsetEncoder newEncoder() {
        return new SingleByte.Encoder(this, c2b, c2bIndex, false);
    }

    private final static String b2cTable = 
        "\u0410\u0411\u0412\u0413\u0414\u0415\u0416\u0417" +      // 0x80 - 0x87
        "\u0418\u0419\u041A\u041B\u041C\u041D\u041E\u041F" +      // 0x88 - 0x8f
        "\u0420\u0421\u0422\u0423\u0424\u0425\u0426\u0427" +      // 0x90 - 0x97
        "\u0428\u0429\u042A\u042B\u042C\u042D\u042E\u042F" +      // 0x98 - 0x9f
        "\u0430\u0431\u0432\u0433\u0434\u0435\u0436\u0437" +      // 0xa0 - 0xa7
        "\u0438\u0439\u043A\u043B\u043C\u043D\u043E\u043F" +      // 0xa8 - 0xaf
        "\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556" +      // 0xb0 - 0xb7
        "\u2555\u2563\u2551\u2557\u255D\u255C\u255B\u2510" +      // 0xb8 - 0xbf
        "\u2514\u2534\u252C\u251C\u2500\u253C\u255E\u255F" +      // 0xc0 - 0xc7
        "\u255A\u2554\u2569\u2566\u2560\u2550\u256C\u2567" +      // 0xc8 - 0xcf
        "\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256B" +      // 0xd0 - 0xd7
        "\u256A\u2518\u250C\u2588\u2584\u258C\u2590\u2580" +      // 0xd8 - 0xdf
        "\u0440\u0441\u0442\u0443\u0444\u0445\u0446\u0447" +      // 0xe0 - 0xe7
        "\u0448\u0449\u044A\u044B\u044C\u044D\u044E\u044F" +      // 0xe8 - 0xef
        "\u0401\u0451\u0404\u0454\u0407\u0457\u040E\u045E" +      // 0xf0 - 0xf7
        "\u00B0\u2219\u00B7\u221A\u2116\u00A4\u25A0\u00A0" +      // 0xf8 - 0xff
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
