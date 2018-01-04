/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs.ext;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import sun.nio.cs.HistoricallyNamedCharset;
import sun.nio.cs.DoubleByte;
import sun.nio.cs.*;

public class JIS_X_0208_Solaris extends Charset
                        implements HistoricallyNamedCharset
{
    public JIS_X_0208_Solaris() {
        super("x-JIS0208_Solaris", ExtendedCharsets.aliasesFor("x-JIS0208_Solaris"));
    }

        public String historicalName() { return "JIS0208"; }

    public boolean contains(Charset cs) {
        return (cs instanceof JIS_X_0208_Solaris);
    }

    public CharsetDecoder newDecoder() {
        initb2c();
        return new  DoubleByte.Decoder_DBCSONLY(this, b2c, b2cSB, 0x21, 0x7e, false);
    }

    public CharsetEncoder newEncoder() {
        initc2b();
        return new DoubleByte.Encoder_DBCSONLY(this, new byte[]{ (byte)0x21, (byte)0x29 }, c2b, c2bIndex, false);
    }

    
    static final String b2cSBStr =
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" ;

        static final String[] b2cStr = {
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFF1F\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" ,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        "\u2460\u2461\u2462\u2463\u2464\u2465\u2466\u2467" + 
        "\u2468\u2469\u246A\u246B\u246C\u246D\u246E\u246F" + 
        "\u2470\u2471\u2472\u2473\u2160\u2161\u2162\u2163" + 
        "\u2164\u2165\u2166\u2167\u2168\u2169\uFFFD\u3349" + 
        "\u3314\u3322\u334D\u3318\u3327\u3303\u3336\u3351" + 
        "\u3357\u330D\u3326\u3323\u332B\u334A\u333B\u339C" + 
        "\u339D\u339E\u338E\u338F\u33C4\u33A1\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\u337B\u301D" + 
        "\u301F\u2116\u33CD\u2121\u32A4\u32A5\u32A6\u32A7" + 
        "\u32A8\u3231\u3232\u3239\u337E\u337D\u337C\u2252" + 
        "\u2261\u222B\u222E\u2211\u221A\u22A5\u2220\u221F" + 
        "\u22BF\u2235\u2229\u222A\uFFFD\uFFFD" ,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        "\u7E8A\u891C\u9348\u9288\u84DC\u4FC9\u70BB\u6631" + 
        "\u68C8\u92F9\u66FB\u5F45\u4E28\u4EE1\u4EFC\u4F00" + 
        "\u4F03\u4F39\u4F56\u4F92\u4F8A\u4F9A\u4F94\u4FCD" + 
        "\u5040\u5022\u4FFF\u501E\u5046\u5070\u5042\u5094" + 
        "\u50F4\u50D8\u514A\u5164\u519D\u51BE\u51EC\u5215" + 
        "\u529C\u52A6\u52C0\u52DB\u5300\u5307\u5324\u5372" + 
        "\u5393\u53B2\u53DD\uFA0E\u549C\u548A\u54A9\u54FF" + 
        "\u5586\u5759\u5765\u57AC\u57C8\u57C7\uFA0F\uFA10" + 
        "\u589E\u58B2\u590B\u5953\u595B\u595D\u5963\u59A4" + 
        "\u59BA\u5B56\u5BC0\u752F\u5BD8\u5BEC\u5C1E\u5CA6" + 
        "\u5CBA\u5CF5\u5D27\u5D53\uFA11\u5D42\u5D6D\u5DB8" + 
        "\u5DB9\u5DD0\u5F21\u5F34\u5F67\u5FB7" ,
        "\u5FDE\u605D\u6085\u608A\u60DE\u60D5\u6120\u60F2" + 
        "\u6111\u6137\u6130\u6198\u6213\u62A6\u63F5\u6460" + 
        "\u649D\u64CE\u654E\u6600\u6615\u663B\u6609\u662E" + 
        "\u661E\u6624\u6665\u6657\u6659\uFA12\u6673\u6699" + 
        "\u66A0\u66B2\u66BF\u66FA\u670E\uF929\u6766\u67BB" + 
        "\u6852\u67C0\u6801\u6844\u68CF\uFA13\u6968\uFA14" + 
        "\u6998\u69E2\u6A30\u6A6B\u6A46\u6A73\u6A7E\u6AE2" + 
        "\u6AE4\u6BD6\u6C3F\u6C5C\u6C86\u6C6F\u6CDA\u6D04" + 
        "\u6D87\u6D6F\u6D96\u6DAC\u6DCF\u6DF8\u6DF2\u6DFC" + 
        "\u6E39\u6E5C\u6E27\u6E3C\u6EBF\u6F88\u6FB5\u6FF5" + 
        "\u7005\u7007\u7028\u7085\u70AB\u710F\u7104\u715C" + 
        "\u7146\u7147\uFA15\u71C1\u71FE\u72B1" ,
        "\u72BE\u7324\uFA16\u7377\u73BD\u73C9\u73D6\u73E3" + 
        "\u73D2\u7407\u73F5\u7426\u742A\u7429\u742E\u7462" + 
        "\u7489\u749F\u7501\u756F\u7682\u769C\u769E\u769B" + 
        "\u76A6\uFA17\u7746\u52AF\u7821\u784E\u7864\u787A" + 
        "\u7930\uFA18\uFA19\uFA1A\u7994\uFA1B\u799B\u7AD1" + 
        "\u7AE7\uFA1C\u7AEB\u7B9E\uFA1D\u7D48\u7D5C\u7DB7" + 
        "\u7DA0\u7DD6\u7E52\u7F47\u7FA1\uFA1E\u8301\u8362" + 
        "\u837F\u83C7\u83F6\u8448\u84B4\u8553\u8559\u856B" + 
        "\uFA1F\u85B0\uFA20\uFA21\u8807\u88F5\u8A12\u8A37" + 
        "\u8A79\u8AA7\u8ABE\u8ADF\uFA22\u8AF6\u8B53\u8B7F" + 
        "\u8CF0\u8CF4\u8D12\u8D76\uFA23\u8ECF\uFA24\uFA25" + 
        "\u9067\u90DE\uFA26\u9115\u9127\u91DA" ,
        "\u91D7\u91DE\u91ED\u91EE\u91E4\u91E5\u9206\u9210" + 
        "\u920A\u923A\u9240\u923C\u924E\u9259\u9251\u9239" + 
        "\u9267\u92A7\u9277\u9278\u92E7\u92D7\u92D9\u92D0" + 
        "\uFA27\u92D5\u92E0\u92D3\u9325\u9321\u92FB\uFA28" + 
        "\u931E\u92FF\u931D\u9302\u9370\u9357\u93A4\u93C6" + 
        "\u93DE\u93F8\u9431\u9445\u9448\u9592\uF9DC\uFA29" + 
        "\u969D\u96AF\u9733\u973B\u9743\u974D\u974F\u9751" + 
        "\u9755\u9857\u9865\uFA2A\uFA2B\u9927\uFA2C\u999E" + 
        "\u9A4E\u9AD9\u9ADC\u9B75\u9B72\u9B8F\u9BB1\u9BBB" + 
        "\u9C00\u9D70\u9D6B\uFA2D\u9E19\u9ED1\uFFFD\uFFFD" + 
        "\u2170\u2171\u2172\u2173\u2174\u2175\u2176\u2177" + 
        "\u2178\u2179\u3052\u00A6\uFF07\uFF02" ,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        "\u2170\u2171\u2172\u2173\u2174\u2175\u2176\u2177" + 
        "\u2178\u2179\u2160\u2161\u2162\u2163\u2164\u2165" + 
        "\u2166\u2167\u2168\u2169\u3052\u00A6\uFF07\uFF02" + 
        "\u3231\u2116\u2121\u306E\u7E8A\u891C\u9348\u9288" + 
        "\u84DC\u4FC9\u70BB\u6631\u68C8\u92F9\u66FB\u5F45" + 
        "\u4E28\u4EE1\u4EFC\u4F00\u4F03\u4F39\u4F56\u4F92" + 
        "\u4F8A\u4F9A\u4F94\u4FCD\u5040\u5022\u4FFF\u501E" + 
        "\u5046\u5070\u5042\u5094\u50F4\u50D8\u514A\u5164" + 
        "\u519D\u51BE\u51EC\u5215\u529C\u52A6\u52C0\u52DB" + 
        "\u5300\u5307\u5324\u5372\u5393\u53B2\u53DD\uFA0E" + 
        "\u549C\u548A\u54A9\u54FF\u5586\u5759\u5765\u57AC" + 
        "\u57C8\u57C7\uFA0F\uFA10\u589E\u58B2" ,
        "\u590B\u5953\u595B\u595D\u5963\u59A4\u59BA\u5B56" + 
        "\u5BC0\u752F\u5BD8\u5BEC\u5C1E\u5CA6\u5CBA\u5CF5" + 
        "\u5D27\u5D53\uFA11\u5D42\u5D6D\u5DB8\u5DB9\u5DD0" + 
        "\u5F21\u5F34\u5F67\u5FB7\u5FDE\u605D\u6085\u608A" + 
        "\u60DE\u60D5\u6120\u60F2\u6111\u6137\u6130\u6198" + 
        "\u6213\u62A6\u63F5\u6460\u649D\u64CE\u654E\u6600" + 
        "\u6615\u663B\u6609\u662E\u661E\u6624\u6665\u6657" + 
        "\u6659\uFA12\u6673\u6699\u66A0\u66B2\u66BF\u66FA" + 
        "\u670E\uF929\u6766\u67BB\u6852\u67C0\u6801\u6844" + 
        "\u68CF\uFA13\u6968\uFA14\u6998\u69E2\u6A30\u6A6B" + 
        "\u6A46\u6A73\u6A7E\u6AE2\u6AE4\u6BD6\u6C3F\u6C5C" + 
        "\u6C86\u6C6F\u6CDA\u6D04\u6D87\u6D6F" ,
        "\u6D96\u6DAC\u6DCF\u6DF8\u6DF2\u6DFC\u6E39\u6E5C" + 
        "\u6E27\u6E3C\u6EBF\u6F88\u6FB5\u6FF5\u7005\u7007" + 
        "\u7028\u7085\u70AB\u710F\u7104\u715C\u7146\u7147" + 
        "\uFA15\u71C1\u71FE\u72B1\u72BE\u7324\uFA16\u7377" + 
        "\u73BD\u73C9\u73D6\u73E3\u73D2\u7407\u73F5\u7426" + 
        "\u742A\u7429\u742E\u7462\u7489\u749F\u7501\u756F" + 
        "\u7682\u769C\u769E\u769B\u76A6\uFA17\u7746\u52AF" + 
        "\u7821\u784E\u7864\u787A\u7930\uFA18\uFA19\uFA1A" + 
        "\u7994\uFA1B\u799B\u7AD1\u7AE7\uFA1C\u7AEB\u7B9E" + 
        "\uFA1D\u7D48\u7D5C\u7DB7\u7DA0\u7DD6\u7E52\u7F47" + 
        "\u7FA1\uFA1E\u8301\u8362\u837F\u83C7\u83F6\u8448" + 
        "\u84B4\u8553\u8559\u856B\uFA1F\u85B0" ,
        "\uFA20\uFA21\u8807\u88F5\u8A12\u8A37\u8A79\u8AA7" + 
        "\u8ABE\u8ADF\uFA22\u8AF6\u8B53\u8B7F\u8CF0\u8CF4" + 
        "\u8D12\u8D76\uFA23\u8ECF\uFA24\uFA25\u9067\u90DE" + 
        "\uFA26\u9115\u9127\u91DA\u91D7\u91DE\u91ED\u91EE" + 
        "\u91E4\u91E5\u9206\u9210\u920A\u923A\u9240\u923C" + 
        "\u924E\u9259\u9251\u9239\u9267\u92A7\u9277\u9278" + 
        "\u92E7\u92D7\u92D9\u92D0\uFA27\u92D5\u92E0\u92D3" + 
        "\u9325\u9321\u92FB\uFA28\u931E\u92FF\u931D\u9302" + 
        "\u9370\u9357\u93A4\u93C6\u93DE\u93F8\u9431\u9445" + 
        "\u9448\u9592\uF9DC\uFA29\u969D\u96AF\u9733\u973B" + 
        "\u9743\u974D\u974F\u9751\u9755\u9857\u9865\uFA2A" + 
        "\uFA2B\u9927\uFA2C\u999E\u9A4E\u9AD9" ,
        "\u9ADC\u9B75\u9B72\u9B8F\u9BB1\u9BBB\u9C00\u9D70" + 
        "\u9D6B\uFA2D\u9E19\u9ED1\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" + 
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" ,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        };

    static char[][] b2c = new char[b2cStr.length][];
    static char[] b2cSB;
    private static volatile boolean b2cInitialized = false;

    static void initb2c() {
        if (b2cInitialized)
            return;
        synchronized (b2c) {
            if (b2cInitialized)
                return;
            for (int i = 0; i < b2cStr.length; i++) {
                if (b2cStr[i] == null)
                    b2c[i] = DoubleByte.B2C_UNMAPPABLE;
                else
                    b2c[i] = b2cStr[i].toCharArray();
            }
            b2cSB = b2cSBStr.toCharArray();
            b2cInitialized = true;
        }
    }

    static char[] c2b = new char[0x5200];
    static char[] c2bIndex = new char[0x100];
    private static volatile boolean c2bInitialized = false;

    static void initc2b() {
        if (c2bInitialized)
            return;
        synchronized (c2b) {
            if (c2bInitialized)
                return;
            String b2cNR =
        "\u7921\u7E8A\u7922\u891C\u7923\u9348\u7924\u9288" + 
        "\u7925\u84DC\u7926\u4FC9\u7927\u70BB\u7928\u6631" + 
        "\u7929\u68C8\u792A\u92F9\u792B\u66FB\u792C\u5F45" + 
        "\u792D\u4E28\u792E\u4EE1\u792F\u4EFC\u7930\u4F00" + 
        "\u7931\u4F03\u7932\u4F39\u7933\u4F56\u7934\u4F92" + 
        "\u7935\u4F8A\u7936\u4F9A\u7937\u4F94\u7938\u4FCD" + 
        "\u7939\u5040\u793A\u5022\u793B\u4FFF\u793C\u501E" + 
        "\u793D\u5046\u793E\u5070\u793F\u5042\u7940\u5094" + 
        "\u7941\u50F4\u7942\u50D8\u7943\u514A\u7944\u5164" + 
        "\u7945\u519D\u7946\u51BE\u7947\u51EC\u7948\u5215" + 
        "\u7949\u529C\u794A\u52A6\u794B\u52C0\u794C\u52DB" + 
        "\u794D\u5300\u794E\u5307\u794F\u5324\u7950\u5372" + 
        "\u7951\u5393\u7952\u53B2\u7953\u53DD\u7954\uFA0E" + 
        "\u7955\u549C\u7956\u548A\u7957\u54A9\u7958\u54FF" + 
        "\u7959\u5586\u795A\u5759\u795B\u5765\u795C\u57AC" + 
        "\u795D\u57C8\u795E\u57C7\u795F\uFA0F\u7960\uFA10" + 
        "\u7961\u589E\u7962\u58B2\u7963\u590B\u7964\u5953" + 
        "\u7965\u595B\u7966\u595D\u7967\u5963\u7968\u59A4" + 
        "\u7969\u59BA\u796A\u5B56\u796B\u5BC0\u796C\u752F" + 
        "\u796D\u5BD8\u796E\u5BEC\u796F\u5C1E\u7970\u5CA6" + 
        "\u7971\u5CBA\u7972\u5CF5\u7973\u5D27\u7974\u5D53" + 
        "\u7975\uFA11\u7976\u5D42\u7977\u5D6D\u7978\u5DB8" + 
        "\u7979\u5DB9\u797A\u5DD0\u797B\u5F21\u797C\u5F34" + 
        "\u797D\u5F67\u797E\u5FB7\u7A21\u5FDE\u7A22\u605D" + 
        "\u7A23\u6085\u7A24\u608A\u7A25\u60DE\u7A26\u60D5" + 
        "\u7A27\u6120\u7A28\u60F2\u7A29\u6111\u7A2A\u6137" + 
        "\u7A2B\u6130\u7A2C\u6198\u7A2D\u6213\u7A2E\u62A6" + 
        "\u7A2F\u63F5\u7A30\u6460\u7A31\u649D\u7A32\u64CE" + 
        "\u7A33\u654E\u7A34\u6600\u7A35\u6615\u7A36\u663B" + 
        "\u7A37\u6609\u7A38\u662E\u7A39\u661E\u7A3A\u6624" + 
        "\u7A3B\u6665\u7A3C\u6657\u7A3D\u6659\u7A3E\uFA12" + 
        "\u7A3F\u6673\u7A40\u6699\u7A41\u66A0\u7A42\u66B2" + 
        "\u7A43\u66BF\u7A44\u66FA\u7A45\u670E\u7A46\uF929" + 
        "\u7A47\u6766\u7A48\u67BB\u7A49\u6852\u7A4A\u67C0" + 
        "\u7A4B\u6801\u7A4C\u6844\u7A4D\u68CF\u7A4E\uFA13" + 
        "\u7A4F\u6968\u7A50\uFA14\u7A51\u6998\u7A52\u69E2" + 
        "\u7A53\u6A30\u7A54\u6A6B\u7A55\u6A46\u7A56\u6A73" + 
        "\u7A57\u6A7E\u7A58\u6AE2\u7A59\u6AE4\u7A5A\u6BD6" + 
        "\u7A5B\u6C3F\u7A5C\u6C5C\u7A5D\u6C86\u7A5E\u6C6F" + 
        "\u7A5F\u6CDA\u7A60\u6D04\u7A61\u6D87\u7A62\u6D6F" + 
        "\u7A63\u6D96\u7A64\u6DAC\u7A65\u6DCF\u7A66\u6DF8" + 
        "\u7A67\u6DF2\u7A68\u6DFC\u7A69\u6E39\u7A6A\u6E5C" + 
        "\u7A6B\u6E27\u7A6C\u6E3C\u7A6D\u6EBF\u7A6E\u6F88" + 
        "\u7A6F\u6FB5\u7A70\u6FF5\u7A71\u7005\u7A72\u7007" + 
        "\u7A73\u7028\u7A74\u7085\u7A75\u70AB\u7A76\u710F" + 
        "\u7A77\u7104\u7A78\u715C\u7A79\u7146\u7A7A\u7147" + 
        "\u7A7B\uFA15\u7A7C\u71C1\u7A7D\u71FE\u7A7E\u72B1" + 
        "\u7B21\u72BE\u7B22\u7324\u7B23\uFA16\u7B24\u7377" + 
        "\u7B25\u73BD\u7B26\u73C9\u7B27\u73D6\u7B28\u73E3" + 
        "\u7B29\u73D2\u7B2A\u7407\u7B2B\u73F5\u7B2C\u7426" + 
        "\u7B2D\u742A\u7B2E\u7429\u7B2F\u742E\u7B30\u7462" + 
        "\u7B31\u7489\u7B32\u749F\u7B33\u7501\u7B34\u756F" + 
        "\u7B35\u7682\u7B36\u769C\u7B37\u769E\u7B38\u769B" + 
        "\u7B39\u76A6\u7B3A\uFA17\u7B3B\u7746\u7B3C\u52AF" + 
        "\u7B3D\u7821\u7B3E\u784E\u7B3F\u7864\u7B40\u787A" + 
        "\u7B41\u7930\u7B42\uFA18\u7B43\uFA19\u7B44\uFA1A" + 
        "\u7B45\u7994\u7B46\uFA1B\u7B47\u799B\u7B48\u7AD1" + 
        "\u7B49\u7AE7\u7B4A\uFA1C\u7B4B\u7AEB\u7B4C\u7B9E" + 
        "\u7B4D\uFA1D\u7B4E\u7D48\u7B4F\u7D5C\u7B50\u7DB7" + 
        "\u7B51\u7DA0\u7B52\u7DD6\u7B53\u7E52\u7B54\u7F47" + 
        "\u7B55\u7FA1\u7B56\uFA1E\u7B57\u8301\u7B58\u8362" + 
        "\u7B59\u837F\u7B5A\u83C7\u7B5B\u83F6\u7B5C\u8448" + 
        "\u7B5D\u84B4\u7B5E\u8553\u7B5F\u8559\u7B60\u856B" + 
        "\u7B61\uFA1F\u7B62\u85B0\u7B63\uFA20\u7B64\uFA21" + 
        "\u7B65\u8807\u7B66\u88F5\u7B67\u8A12\u7B68\u8A37" + 
        "\u7B69\u8A79\u7B6A\u8AA7\u7B6B\u8ABE\u7B6C\u8ADF" + 
        "\u7B6D\uFA22\u7B6E\u8AF6\u7B6F\u8B53\u7B70\u8B7F" + 
        "\u7B71\u8CF0\u7B72\u8CF4\u7B73\u8D12\u7B74\u8D76" + 
        "\u7B75\uFA23\u7B76\u8ECF\u7B77\uFA24\u7B78\uFA25" + 
        "\u7B79\u9067\u7B7A\u90DE\u7B7B\uFA26\u7B7C\u9115" + 
        "\u7B7D\u9127\u7B7E\u91DA\u7C21\u91D7\u7C22\u91DE" + 
        "\u7C23\u91ED\u7C24\u91EE\u7C25\u91E4\u7C26\u91E5" + 
        "\u7C27\u9206\u7C28\u9210\u7C29\u920A\u7C2A\u923A" + 
        "\u7C2B\u9240\u7C2C\u923C\u7C2D\u924E\u7C2E\u9259" + 
        "\u7C2F\u9251\u7C30\u9239\u7C31\u9267\u7C32\u92A7" + 
        "\u7C33\u9277\u7C34\u9278\u7C35\u92E7\u7C36\u92D7" + 
        "\u7C37\u92D9\u7C38\u92D0\u7C39\uFA27\u7C3A\u92D5" + 
        "\u7C3B\u92E0\u7C3C\u92D3\u7C3D\u9325\u7C3E\u9321" + 
        "\u7C3F\u92FB\u7C40\uFA28\u7C41\u931E\u7C42\u92FF" + 
        "\u7C43\u931D\u7C44\u9302\u7C45\u9370\u7C46\u9357" + 
        "\u7C47\u93A4\u7C48\u93C6\u7C49\u93DE\u7C4A\u93F8" + 
        "\u7C4B\u9431\u7C4C\u9445\u7C4D\u9448\u7C4E\u9592" + 
        "\u7C4F\uF9DC\u7C50\uFA29\u7C51\u969D\u7C52\u96AF" + 
        "\u7C53\u9733\u7C54\u973B\u7C55\u9743\u7C56\u974D" + 
        "\u7C57\u974F\u7C58\u9751\u7C59\u9755\u7C5A\u9857" + 
        "\u7C5B\u9865\u7C5C\uFA2A\u7C5D\uFA2B\u7C5E\u9927" + 
        "\u7C5F\uFA2C\u7C60\u999E\u7C61\u9A4E\u7C62\u9AD9" + 
        "\u7C63\u9ADC\u7C64\u9B75\u7C65\u9B72\u7C66\u9B8F" + 
        "\u7C67\u9BB1\u7C68\u9BBB\u7C69\u9C00\u7C6A\u9D70" + 
        "\u7C6B\u9D6B\u7C6C\uFA2D\u7C6D\u9E19\u7C6E\u9ED1" + 
        "\u7C71\u2170\u7C72\u2171\u7C73\u2172\u7C74\u2173" + 
        "\u7C75\u2174\u7C76\u2175\u7C77\u2176\u7C78\u2177" + 
        "\u7C79\u2178\u7C7A\u2179\u7C7B\u3052\u7C7C\u00A6" + 
        "\u7C7D\uFF07\u7C7E\uFF02\u932B\u2160\u932C\u2161" + 
        "\u932D\u2162\u932E\u2163\u932F\u2164\u9330\u2165" + 
        "\u9331\u2166\u9332\u2167\u9333\u2168\u9334\u2169" + 
        "\u9339\u3231\u933A\u2116\u933B\u2121" ;

            String c2bNR = null;
            DoubleByte.Encoder.initC2B(b2cStr, b2cSBStr, b2cNR, c2bNR,
                                       0x21, 0x7e,
                                       c2b, c2bIndex);
            c2bInitialized = true;
        }
    }
}
