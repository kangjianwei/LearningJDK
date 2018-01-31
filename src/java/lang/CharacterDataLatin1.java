// This file was generated AUTOMATICALLY from a template file Wed Aug 22 18:58:18 PDT 2018
/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * 字符属性包装器的一个实现
 *
 * 封装了 java.lang.Character 中的一些属性，还提供了一些常用的字符操作
 * 将每个字符的属性打包为一组32-bit的二进制数据，以便在使用字符时做出准确、快速的判断
 * 处理的字符范围是：[0, 255]
 */
class CharacterDataLatin1 extends CharacterData {
    static final CharacterDataLatin1 instance = new CharacterDataLatin1();
    
    /*
     * 下表生成方式：
     * java GenerateCharacter -template t:/workspace/open/make/data/characterdata/CharacterDataLatin1.java.template
     * -spec t:/workspace/open/make/data/unicodedata/UnicodeData.txt
     * -specialcasing t:/workspace/open/make/data/unicodedata/SpecialCasing.txt
     * -proplist t:/workspace/open/make/data/unicodedata/PropList.txt
     * -o t:/workspace/build/windows-x64-open/support/gensrc/java.base/java/lang/CharacterDataLatin1.java
     * -usecharforbyte
     * -latin1 8
     *
     *
     * 总共封装了Layin1字符集中的256个字符
     *
     * 字符属性序列被编码为32-bit的二进制序列，且将其分为10个区域，不同区域含义如下：
     *
     * ⑴ ⑵      ⑶    ⑷⑸⑹ ⑺  ⑻   ⑼    ⑽
     * 0-0000-000000000-0-0-0-000-00-00000-00000
     *
     * ⑴ 1 bit：镜像属性。像()[]<>这类型字符，此组值为1。
     * ⑵ 4 bit：方向属性[directionality property]
     * ⑶ 9 bit：符号偏移量，用于大小写转换
     * ⑷ 1 bit：如果为1，代表大写。原始字符序列添加符号偏移量后可转为小写。
     * ⑸ 1 bit：如果为1，代表小写。原始字符序列减去符号偏移量后可转为大写。
     * ⑹ 1 bit：如果为1，该字符存在titlecase表现形式，可能为自身。
     * ⑺ 3 bit：表示该字符可以出现的位置，规定四种位置：
     *                             起始位置   非起始位置
     *                Java标识符      <1>        <2>
     *             Unicode标识符      <3>        <4>
     *
     *           存在8中不同取值：
     *             0/000 <-><-><-><->
     *             1/001 <-><2><-><4>，可忽略（ignorable control; may continue a Unicode identifier or Java identifier）
     *             2/010 <-><2><-><->（may continue a Java identifier but not a Unicode identifier (unused)）
     *             3/011 <-><2><-><4>（may continue a Unicode identifier or Java identifier）
     *             4/100 Java空白符（Java whitespace character）
     *             5/101 <1><2><-><4>，如下划线_
     *             6/110 <1><2><-><->，如下划线$
     *             7/111 <1><2><3><4>
     *           注：
     *             5、6、7 可位于Java标识符起始位置
     *             1、2、3、5、6、7 可位于Java标识符非起始位置
     *             7 可位于Unicode标识符起始位置
     *             1、3、5、7 可位于Unicode标识符非起始位置
     *             1 在标识符内是可忽略的
     *             4 是Java空白符
     * ⑻ 2 bit：存在4种不同取值：
     *           0 此字符没有数值属性
     *           1 简单的数值字符，如0~9。adding the digit offset to the character code and then masking with 0x1F will produce the desired numeric value
     *           2 非常规数值字符，如第189号索引处的½，表示二分之一
     *           3 超级数值字符，如a~z、A~Z，可用在进制表示当中
     * ⑼ 5 bit：数值偏移量
     * ⑽ 5 bit：字符类型（参见Character类中的"Unicode符号分类代号"）
     *
     * 使用二进制表示下表（空白字符和控制字符用■代替显示）：
     * 000┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 001┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 002┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 003┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 004┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 005┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 006┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 007┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 008┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 009┃■┃ 0-1011-000000000-0-0-0-100-00-00000-01111
     * 010┃■┃ 0-1010-000000000-0-0-0-100-00-00000-01111
     * 011┃■┃ 0-1011-000000000-0-0-0-100-00-00000-01111
     * 012┃■┃ 0-1100-000000000-0-0-0-100-00-00000-01111
     * 013┃■┃ 0-1010-000000000-0-0-0-100-00-00000-01111
     * 014┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 015┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 016┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 017┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 018┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 019┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 020┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 021┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 022┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 023┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 024┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 025┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 026┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 027┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 028┃■┃ 0-1010-000000000-0-0-0-100-00-00000-01111
     * 029┃■┃ 0-1010-000000000-0-0-0-100-00-00000-01111
     * 030┃■┃ 0-1010-000000000-0-0-0-100-00-00000-01111
     * 031┃■┃ 0-1011-000000000-0-0-0-100-00-00000-01111
     * 032┃■┃ 0-1100-000000000-0-0-0-100-00-00000-01100
     * 033┃!┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 034┃"┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 035┃#┃ 0-0101-000000000-0-0-0-000-00-00000-11000
     * 036┃$┃ 0-0101-000000000-0-0-0-110-00-00000-11010
     * 037┃%┃ 0-0101-000000000-0-0-0-000-00-00000-11000
     * 038┃&┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 039┃'┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 040┃(┃ 1-1101-000000000-0-0-0-000-00-00000-10101
     * 041┃)┃ 1-1101-000000000-0-0-0-000-00-00000-10110
     * 042┃*┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 043┃+┃ 0-0100-000000000-0-0-0-000-00-00000-11001
     * 044┃,┃ 0-0111-000000000-0-0-0-000-00-00000-11000
     * 045┃-┃ 0-0100-000000000-0-0-0-000-00-00000-10100
     * 046┃.┃ 0-0111-000000000-0-0-0-000-00-00000-11000
     * 047┃/┃ 0-0111-000000000-0-0-0-000-00-00000-11000
     * 048┃0┃ 0-0011-000000000-0-0-0-011-01-10000-01001
     * 049┃1┃ 0-0011-000000000-0-0-0-011-01-10000-01001
     * 050┃2┃ 0-0011-000000000-0-0-0-011-01-10000-01001
     * 051┃3┃ 0-0011-000000000-0-0-0-011-01-10000-01001
     * 052┃4┃ 0-0011-000000000-0-0-0-011-01-10000-01001
     * 053┃5┃ 0-0011-000000000-0-0-0-011-01-10000-01001
     * 054┃6┃ 0-0011-000000000-0-0-0-011-01-10000-01001
     * 055┃7┃ 0-0011-000000000-0-0-0-011-01-10000-01001
     * 056┃8┃ 0-0011-000000000-0-0-0-011-01-10000-01001
     * 057┃9┃ 0-0011-000000000-0-0-0-011-01-10000-01001
     * 058┃:┃ 0-0111-000000000-0-0-0-000-00-00000-11000
     * 059┃;┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 060┃<┃ 1-1101-000000000-0-0-0-000-00-00000-11001
     * 061┃=┃ 0-1101-000000000-0-0-0-000-00-00000-11001
     * 062┃>┃ 1-1101-000000000-0-0-0-000-00-00000-11001
     * 063┃?┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 064┃@┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 065┃A┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 066┃B┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 067┃C┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 068┃D┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 069┃E┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 070┃F┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 071┃G┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 072┃H┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 073┃I┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 074┃J┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 075┃K┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 076┃L┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 077┃M┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 078┃N┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 079┃O┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 080┃P┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 081┃Q┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 082┃R┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 083┃S┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 084┃T┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 085┃U┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 086┃V┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 087┃W┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 088┃X┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 089┃Y┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 090┃Z┃ 0-0000-000100000-1-0-0-111-11-11111-00001
     * 091┃[┃ 1-1101-000000000-0-0-0-000-00-00000-10101
     * 092┃\┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 093┃]┃ 1-1101-000000000-0-0-0-000-00-00000-10110
     * 094┃^┃ 0-1101-000000000-0-0-0-000-00-00000-11011
     * 095┃_┃ 0-1101-000000000-0-0-0-101-00-00000-10111
     * 096┃`┃ 0-1101-000000000-0-0-0-000-00-00000-11011
     * 097┃a┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 098┃b┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 099┃c┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 100┃d┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 101┃e┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 102┃f┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 103┃g┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 104┃h┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 105┃i┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 106┃j┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 107┃k┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 108┃l┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 109┃m┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 110┃n┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 111┃o┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 112┃p┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 113┃q┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 114┃r┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 115┃s┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 116┃t┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 117┃u┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 118┃v┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 119┃w┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 120┃x┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 121┃y┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 122┃z┃ 0-0000-000100000-0-1-0-111-11-11111-00010
     * 123┃{┃ 1-1101-000000000-0-0-0-000-00-00000-10101
     * 124┃|┃ 0-1101-000000000-0-0-0-000-00-00000-11001
     * 125┃}┃ 1-1101-000000000-0-0-0-000-00-00000-10110
     * 126┃~┃ 0-1101-000000000-0-0-0-000-00-00000-11001
     * 127┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 128┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 129┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 130┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 131┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 132┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 133┃■┃ 0-1010-000000000-0-0-0-001-00-00000-01111
     * 134┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 135┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 136┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 137┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 138┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 139┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 140┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 141┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 142┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 143┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 144┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 145┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 146┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 147┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 148┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 149┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 150┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 151┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 152┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 153┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 154┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 155┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 156┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 157┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 158┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 159┃■┃ 0-1001-000000000-0-0-0-001-00-00000-01111
     * 160┃ ┃ 0-0111-000000000-0-0-0-000-00-00000-01100
     * 161┃¡┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 162┃¢┃ 0-0101-000000000-0-0-0-110-00-00000-11010
     * 163┃£┃ 0-0101-000000000-0-0-0-110-00-00000-11010
     * 164┃¤┃ 0-0101-000000000-0-0-0-110-00-00000-11010
     * 165┃¥┃ 0-0101-000000000-0-0-0-110-00-00000-11010
     * 166┃¦┃ 0-1101-000000000-0-0-0-000-00-00000-11100
     * 167┃§┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 168┃¨┃ 0-1101-000000000-0-0-0-000-00-00000-11011
     * 169┃©┃ 0-1101-000000000-0-0-0-000-00-00000-11100
     * 170┃ª┃ 0-0000-000000000-0-0-0-111-00-00000-00101
     * 171┃«┃ 1-1101-000000000-0-0-0-000-00-00000-11101
     * 172┃¬┃ 0-1101-000000000-0-0-0-000-00-00000-11001
     * 173┃­┃ 0-1001-000000000-0-0-0-001-00-00000-10000
     * 174┃®┃ 0-1101-000000000-0-0-0-000-00-00000-11100
     * 175┃¯┃ 0-1101-000000000-0-0-0-000-00-00000-11011
     * 176┃°┃ 0-0101-000000000-0-0-0-000-00-00000-11100
     * 177┃±┃ 0-0101-000000000-0-0-0-000-00-00000-11001
     * 178┃²┃ 0-0011-000000000-0-0-0-000-01-10000-01011
     * 179┃³┃ 0-0011-000000000-0-0-0-000-01-10000-01011
     * 180┃´┃ 0-1101-000000000-0-0-0-000-00-00000-11011
     * 181┃µ┃ 0-0000-111111111-0-1-0-111-00-00000-00010
     * 182┃¶┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 183┃·┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 184┃¸┃ 0-1101-000000000-0-0-0-000-00-00000-11011
     * 185┃¹┃ 0-0011-000000000-0-0-0-000-01-01000-01011
     * 186┃º┃ 0-0000-000000000-0-0-0-111-00-00000-00101
     * 187┃»┃ 1-1101-000000000-0-0-0-000-00-00000-11110
     * 188┃¼┃ 0-1101-000000000-0-0-0-000-10-00000-01011
     * 189┃½┃ 0-1101-000000000-0-0-0-000-10-00000-01011
     * 190┃¾┃ 0-1101-000000000-0-0-0-000-10-00000-01011
     * 191┃¿┃ 0-1101-000000000-0-0-0-000-00-00000-11000
     * 192┃À┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 193┃Á┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 194┃Â┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 195┃Ã┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 196┃Ä┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 197┃Å┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 198┃Æ┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 199┃Ç┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 200┃È┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 201┃É┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 202┃Ê┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 203┃Ë┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 204┃Ì┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 205┃Í┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 206┃Î┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 207┃Ï┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 208┃Ð┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 209┃Ñ┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 210┃Ò┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 211┃Ó┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 212┃Ô┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 213┃Õ┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 214┃Ö┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 215┃×┃ 0-1101-000000000-0-0-0-000-00-00000-11001
     * 216┃Ø┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 217┃Ù┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 218┃Ú┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 219┃Û┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 220┃Ü┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 221┃Ý┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 222┃Þ┃ 0-0000-000100000-1-0-0-111-00-00000-00001
     * 223┃ß┃ 0-0000-111111111-0-1-0-111-00-00000-00010
     * 224┃à┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 225┃á┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 226┃â┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 227┃ã┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 228┃ä┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 229┃å┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 230┃æ┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 231┃ç┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 232┃è┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 233┃é┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 234┃ê┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 235┃ë┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 236┃ì┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 237┃í┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 238┃î┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 239┃ï┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 240┃ð┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 241┃ñ┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 242┃ò┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 243┃ó┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 244┃ô┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 245┃õ┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 246┃ö┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 247┃÷┃ 0-1101-000000000-0-0-0-000-00-00000-11001
     * 248┃ø┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 249┃ù┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 250┃ú┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 251┃û┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 252┃ü┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 253┃ý┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 254┃þ┃ 0-0000-000100000-0-1-0-111-00-00000-00010
     * 255┃ÿ┃ 0-0000-110000111-0-1-0-111-00-00000-00010
     */
    static final int A[] = {
        0x4800100F,  //   0   Cc, ignorable
        0x4800100F,  //   1   Cc, ignorable
        0x4800100F,  //   2   Cc, ignorable
        0x4800100F,  //   3   Cc, ignorable
        0x4800100F,  //   4   Cc, ignorable
        0x4800100F,  //   5   Cc, ignorable
        0x4800100F,  //   6   Cc, ignorable
        0x4800100F,  //   7   Cc, ignorable
        0x4800100F,  //   8   Cc, ignorable
        0x5800400F,  //   9   Cc, S, whitespace
        0x5000400F,  //  10   Cc, B, whitespace
        0x5800400F,  //  11   Cc, S, whitespace
        0x6000400F,  //  12   Cc, WS, whitespace
        0x5000400F,  //  13   Cc, B, whitespace
        0x4800100F,  //  14   Cc, ignorable
        0x4800100F,  //  15   Cc, ignorable
        0x4800100F,  //  16   Cc, ignorable
        0x4800100F,  //  17   Cc, ignorable
        0x4800100F,  //  18   Cc, ignorable
        0x4800100F,  //  19   Cc, ignorable
        0x4800100F,  //  20   Cc, ignorable
        0x4800100F,  //  21   Cc, ignorable
        0x4800100F,  //  22   Cc, ignorable
        0x4800100F,  //  23   Cc, ignorable
        0x4800100F,  //  24   Cc, ignorable
        0x4800100F,  //  25   Cc, ignorable
        0x4800100F,  //  26   Cc, ignorable
        0x4800100F,  //  27   Cc, ignorable
        0x5000400F,  //  28   Cc, B, whitespace
        0x5000400F,  //  29   Cc, B, whitespace
        0x5000400F,  //  30   Cc, B, whitespace
        0x5800400F,  //  31   Cc, S, whitespace
        0x6000400C,  //  32   Zs, WS, whitespace
        0x68000018,  //  33   Po, ON
        0x68000018,  //  34   Po, ON
        0x28000018,  //  35   Po, ET
        0x2800601A,  //  36   Sc, ET, currency
        0x28000018,  //  37   Po, ET
        0x68000018,  //  38   Po, ON
        0x68000018,  //  39   Po, ON
        -0x17FFFFEB,  //  40   No, hasUpper (subtract 511), hasLower (add 511), hasTitle, identifier start, supradecimal 31
        -0x17FFFFEA,  //  41   Nl, hasUpper (subtract 511), hasLower (add 511), hasTitle, identifier start, supradecimal 31
        0x68000018,  //  42   Po, ON
        0x20000019,  //  43   Sm, ES
        0x38000018,  //  44   Po, CS
        0x20000014,  //  45   Pd, ES
        0x38000018,  //  46   Po, CS
        0x38000018,  //  47   Po, CS
        0x18003609,  //  48   Nd, EN, identifier part, decimal 16
        0x18003609,  //  49   Nd, EN, identifier part, decimal 16
        0x18003609,  //  50   Nd, EN, identifier part, decimal 16
        0x18003609,  //  51   Nd, EN, identifier part, decimal 16
        0x18003609,  //  52   Nd, EN, identifier part, decimal 16
        0x18003609,  //  53   Nd, EN, identifier part, decimal 16
        0x18003609,  //  54   Nd, EN, identifier part, decimal 16
        0x18003609,  //  55   Nd, EN, identifier part, decimal 16
        0x18003609,  //  56   Nd, EN, identifier part, decimal 16
        0x18003609,  //  57   Nd, EN, identifier part, decimal 16
        0x38000018,  //  58   Po, CS
        0x68000018,  //  59   Po, ON
        -0x17FFFFE7,  //  60   Me, hasUpper (subtract 511), hasLower (add 511), hasTitle, identifier start, supradecimal 31
        0x68000019,  //  61   Sm, ON
        -0x17FFFFE7,  //  62   Me, hasUpper (subtract 511), hasLower (add 511), hasTitle, identifier start, supradecimal 31
        0x68000018,  //  63   Po, ON
        0x68000018,  //  64   Po, ON
        0x00827FE1,  //  65   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  66   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  67   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  68   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  69   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  70   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  71   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  72   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  73   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  74   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  75   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  76   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  77   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  78   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  79   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  80   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  81   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  82   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  83   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  84   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  85   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  86   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  87   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  88   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  89   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        0x00827FE1,  //  90   Lu, L, hasLower (add 32), identifier start, supradecimal 31
        -0x17FFFFEB,  //  91   No, hasUpper (subtract 511), hasLower (add 511), hasTitle, identifier start, supradecimal 31
        0x68000018,  //  92   Po, ON
        -0x17FFFFEA,  //  93   Nl, hasUpper (subtract 511), hasLower (add 511), hasTitle, identifier start, supradecimal 31
        0x6800001B,  //  94   Sk, ON
        0x68005017,  //  95   Pc, ON, underscore
        0x6800001B,  //  96   Sk, ON
        0x00817FE2,  //  97   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  //  98   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  //  99   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 100   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 101   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 102   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 103   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 104   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 105   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 106   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 107   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 108   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 109   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 110   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 111   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 112   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 113   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 114   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 115   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 116   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 117   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 118   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 119   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 120   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 121   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        0x00817FE2,  // 122   Ll, L, hasUpper (subtract 32), identifier start, supradecimal 31
        -0x17FFFFEB,  // 123   No, hasUpper (subtract 511), hasLower (add 511), hasTitle, identifier start, supradecimal 31
        0x68000019,  // 124   Sm, ON
        -0x17FFFFEA,  // 125   Nl, hasUpper (subtract 511), hasLower (add 511), hasTitle, identifier start, supradecimal 31
        0x68000019,  // 126   Sm, ON
        0x4800100F,  // 127   Cc, ignorable
        0x4800100F,  // 128   Cc, ignorable
        0x4800100F,  // 129   Cc, ignorable
        0x4800100F,  // 130   Cc, ignorable
        0x4800100F,  // 131   Cc, ignorable
        0x4800100F,  // 132   Cc, ignorable
        0x5000100F,  // 133   Cc, B, ignorable
        0x4800100F,  // 134   Cc, ignorable
        0x4800100F,  // 135   Cc, ignorable
        0x4800100F,  // 136   Cc, ignorable
        0x4800100F,  // 137   Cc, ignorable
        0x4800100F,  // 138   Cc, ignorable
        0x4800100F,  // 139   Cc, ignorable
        0x4800100F,  // 140   Cc, ignorable
        0x4800100F,  // 141   Cc, ignorable
        0x4800100F,  // 142   Cc, ignorable
        0x4800100F,  // 143   Cc, ignorable
        0x4800100F,  // 144   Cc, ignorable
        0x4800100F,  // 145   Cc, ignorable
        0x4800100F,  // 146   Cc, ignorable
        0x4800100F,  // 147   Cc, ignorable
        0x4800100F,  // 148   Cc, ignorable
        0x4800100F,  // 149   Cc, ignorable
        0x4800100F,  // 150   Cc, ignorable
        0x4800100F,  // 151   Cc, ignorable
        0x4800100F,  // 152   Cc, ignorable
        0x4800100F,  // 153   Cc, ignorable
        0x4800100F,  // 154   Cc, ignorable
        0x4800100F,  // 155   Cc, ignorable
        0x4800100F,  // 156   Cc, ignorable
        0x4800100F,  // 157   Cc, ignorable
        0x4800100F,  // 158   Cc, ignorable
        0x4800100F,  // 159   Cc, ignorable
        0x3800000C,  // 160   Zs, CS
        0x68000018,  // 161   Po, ON
        0x2800601A,  // 162   Sc, ET, currency
        0x2800601A,  // 163   Sc, ET, currency
        0x2800601A,  // 164   Sc, ET, currency
        0x2800601A,  // 165   Sc, ET, currency
        0x6800001C,  // 166   So, ON
        0x68000018,  // 167   Po, ON
        0x6800001B,  // 168   Sk, ON
        0x6800001C,  // 169   So, ON
        -0xFFFF8FFB,  // 170   Sk, hasUpper (subtract 511), hasLower (add 511), hasTitle, supradecimal 31
        -0x17FFFFE3,  // 171   Lt, hasUpper (subtract 511), hasLower (add 511), hasTitle, identifier start, supradecimal 31
        0x68000019,  // 172   Sm, ON
        0x48001010,  // 173   Cf, ignorable
        0x6800001C,  // 174   So, ON
        0x6800001B,  // 175   Sk, ON
        0x2800001C,  // 176   So, ET
        0x28000019,  // 177   Sm, ET
        0x1800060B,  // 178   No, EN, decimal 16
        0x1800060B,  // 179   No, EN, decimal 16
        0x6800001B,  // 180   Sk, ON
        0x07FD7002,  // 181   Ll, L, hasUpper (subtract 511), identifier start
        0x68000018,  // 182   Po, ON
        0x68000018,  // 183   Po, ON
        0x6800001B,  // 184   Sk, ON
        0x1800050B,  // 185   No, EN, decimal 8
        -0xFFFF8FFB,  // 186   Sk, hasUpper (subtract 511), hasLower (add 511), hasTitle, supradecimal 31
        -0x17FFFFE2,  // 187   Ll, hasUpper (subtract 511), hasLower (add 511), hasTitle, identifier start, supradecimal 31
        0x6800080B,  // 188   No, ON, strange
        0x6800080B,  // 189   No, ON, strange
        0x6800080B,  // 190   No, ON, strange
        0x68000018,  // 191   Po, ON
        0x00827001,  // 192   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 193   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 194   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 195   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 196   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 197   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 198   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 199   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 200   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 201   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 202   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 203   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 204   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 205   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 206   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 207   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 208   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 209   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 210   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 211   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 212   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 213   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 214   Lu, L, hasLower (add 32), identifier start
        0x68000019,  // 215   Sm, ON
        0x00827001,  // 216   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 217   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 218   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 219   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 220   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 221   Lu, L, hasLower (add 32), identifier start
        0x00827001,  // 222   Lu, L, hasLower (add 32), identifier start
        0x07FD7002,  // 223   Ll, L, hasUpper (subtract 511), identifier start
        0x00817002,  // 224   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 225   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 226   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 227   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 228   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 229   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 230   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 231   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 232   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 233   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 234   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 235   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 236   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 237   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 238   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 239   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 240   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 241   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 242   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 243   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 244   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 245   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 246   Ll, L, hasUpper (subtract 32), identifier start
        0x68000019,  // 247   Sm, ON
        0x00817002,  // 248   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 249   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 250   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 251   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 252   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 253   Ll, L, hasUpper (subtract 32), identifier start
        0x00817002,  // 254   Ll, L, hasUpper (subtract 32), identifier start
        0x061D7002   // 255   Ll, L, hasUpper (subtract 391), identifier start
    };
    
    static final char B[] = {
        0x0000,  //   0   unassigned, L
        0x0000,  //   1   unassigned, L
        0x0000,  //   2   unassigned, L
        0x0000,  //   3   unassigned, L
        0x0000,  //   4   unassigned, L
        0x0000,  //   5   unassigned, L
        0x0000,  //   6   unassigned, L
        0x0000,  //   7   unassigned, L
        0x0000,  //   8   unassigned, L
        0x0000,  //   9   unassigned, L
        0x0000,  //  10   unassigned, L
        0x0000,  //  11   unassigned, L
        0x0000,  //  12   unassigned, L
        0x0000,  //  13   unassigned, L
        0x0000,  //  14   unassigned, L
        0x0000,  //  15   unassigned, L
        0x0000,  //  16   unassigned, L
        0x0000,  //  17   unassigned, L
        0x0000,  //  18   unassigned, L
        0x0000,  //  19   unassigned, L
        0x0000,  //  20   unassigned, L
        0x0000,  //  21   unassigned, L
        0x0000,  //  22   unassigned, L
        0x0000,  //  23   unassigned, L
        0x0000,  //  24   unassigned, L
        0x0000,  //  25   unassigned, L
        0x0000,  //  26   unassigned, L
        0x0000,  //  27   unassigned, L
        0x0000,  //  28   unassigned, L
        0x0000,  //  29   unassigned, L
        0x0000,  //  30   unassigned, L
        0x0000,  //  31   unassigned, L
        0x0000,  //  32   unassigned, L
        0x0000,  //  33   unassigned, L
        0x0000,  //  34   unassigned, L
        0x0000,  //  35   unassigned, L
        0x0000,  //  36   unassigned, L
        0x0000,  //  37   unassigned, L
        0x0000,  //  38   unassigned, L
        0x0000,  //  39   unassigned, L
        0x0000,  //  40   unassigned, L
        0x0000,  //  41   unassigned, L
        0x0000,  //  42   unassigned, L
        0x0000,  //  43   unassigned, L
        0x0000,  //  44   unassigned, L
        0x0000,  //  45   unassigned, L
        0x0000,  //  46   unassigned, L
        0x0000,  //  47   unassigned, L
        0x0000,  //  48   unassigned, L
        0x0000,  //  49   unassigned, L
        0x0000,  //  50   unassigned, L
        0x0000,  //  51   unassigned, L
        0x0000,  //  52   unassigned, L
        0x0000,  //  53   unassigned, L
        0x0000,  //  54   unassigned, L
        0x0000,  //  55   unassigned, L
        0x0000,  //  56   unassigned, L
        0x0000,  //  57   unassigned, L
        0x0000,  //  58   unassigned, L
        0x0000,  //  59   unassigned, L
        0x0000,  //  60   unassigned, L
        0x0000,  //  61   unassigned, L
        0x0000,  //  62   unassigned, L
        0x0000,  //  63   unassigned, L
        0x0000,  //  64   unassigned, L
        0x0000,  //  65   unassigned, L
        0x0000,  //  66   unassigned, L
        0x0000,  //  67   unassigned, L
        0x0000,  //  68   unassigned, L
        0x0000,  //  69   unassigned, L
        0x0000,  //  70   unassigned, L
        0x0000,  //  71   unassigned, L
        0x0000,  //  72   unassigned, L
        0x0000,  //  73   unassigned, L
        0x0000,  //  74   unassigned, L
        0x0000,  //  75   unassigned, L
        0x0000,  //  76   unassigned, L
        0x0000,  //  77   unassigned, L
        0x0000,  //  78   unassigned, L
        0x0000,  //  79   unassigned, L
        0x0000,  //  80   unassigned, L
        0x0000,  //  81   unassigned, L
        0x0000,  //  82   unassigned, L
        0x0000,  //  83   unassigned, L
        0x0000,  //  84   unassigned, L
        0x0000,  //  85   unassigned, L
        0x0000,  //  86   unassigned, L
        0x0000,  //  87   unassigned, L
        0x0000,  //  88   unassigned, L
        0x0000,  //  89   unassigned, L
        0x0000,  //  90   unassigned, L
        0x0000,  //  91   unassigned, L
        0x0000,  //  92   unassigned, L
        0x0000,  //  93   unassigned, L
        0x0000,  //  94   unassigned, L
        0x0000,  //  95   unassigned, L
        0x0000,  //  96   unassigned, L
        0x0000,  //  97   unassigned, L
        0x0000,  //  98   unassigned, L
        0x0000,  //  99   unassigned, L
        0x0000,  // 100   unassigned, L
        0x0000,  // 101   unassigned, L
        0x0000,  // 102   unassigned, L
        0x0000,  // 103   unassigned, L
        0x0000,  // 104   unassigned, L
        0x0000,  // 105   unassigned, L
        0x0000,  // 106   unassigned, L
        0x0000,  // 107   unassigned, L
        0x0000,  // 108   unassigned, L
        0x0000,  // 109   unassigned, L
        0x0000,  // 110   unassigned, L
        0x0000,  // 111   unassigned, L
        0x0000,  // 112   unassigned, L
        0x0000,  // 113   unassigned, L
        0x0000,  // 114   unassigned, L
        0x0000,  // 115   unassigned, L
        0x0000,  // 116   unassigned, L
        0x0000,  // 117   unassigned, L
        0x0000,  // 118   unassigned, L
        0x0000,  // 119   unassigned, L
        0x0000,  // 120   unassigned, L
        0x0000,  // 121   unassigned, L
        0x0000,  // 122   unassigned, L
        0x0000,  // 123   unassigned, L
        0x0000,  // 124   unassigned, L
        0x0000,  // 125   unassigned, L
        0x0000,  // 126   unassigned, L
        0x0000,  // 127   unassigned, L
        0x0000,  // 128   unassigned, L
        0x0000,  // 129   unassigned, L
        0x0000,  // 130   unassigned, L
        0x0000,  // 131   unassigned, L
        0x0000,  // 132   unassigned, L
        0x0000,  // 133   unassigned, L
        0x0000,  // 134   unassigned, L
        0x0000,  // 135   unassigned, L
        0x0000,  // 136   unassigned, L
        0x0000,  // 137   unassigned, L
        0x0000,  // 138   unassigned, L
        0x0000,  // 139   unassigned, L
        0x0000,  // 140   unassigned, L
        0x0000,  // 141   unassigned, L
        0x0000,  // 142   unassigned, L
        0x0000,  // 143   unassigned, L
        0x0000,  // 144   unassigned, L
        0x0000,  // 145   unassigned, L
        0x0000,  // 146   unassigned, L
        0x0000,  // 147   unassigned, L
        0x0000,  // 148   unassigned, L
        0x0000,  // 149   unassigned, L
        0x0000,  // 150   unassigned, L
        0x0000,  // 151   unassigned, L
        0x0000,  // 152   unassigned, L
        0x0000,  // 153   unassigned, L
        0x0000,  // 154   unassigned, L
        0x0000,  // 155   unassigned, L
        0x0000,  // 156   unassigned, L
        0x0000,  // 157   unassigned, L
        0x0000,  // 158   unassigned, L
        0x0000,  // 159   unassigned, L
        0x0000,  // 160   unassigned, L
        0x0000,  // 161   unassigned, L
        0x0000,  // 162   unassigned, L
        0x0000,  // 163   unassigned, L
        0x0000,  // 164   unassigned, L
        0x0000,  // 165   unassigned, L
        0x0000,  // 166   unassigned, L
        0x0000,  // 167   unassigned, L
        0x0000,  // 168   unassigned, L
        0x0000,  // 169   unassigned, L
        0x0001,  // 170   Lu, L
        0x0000,  // 171   unassigned, L
        0x0000,  // 172   unassigned, L
        0x0000,  // 173   unassigned, L
        0x0000,  // 174   unassigned, L
        0x0000,  // 175   unassigned, L
        0x0000,  // 176   unassigned, L
        0x0000,  // 177   unassigned, L
        0x0000,  // 178   unassigned, L
        0x0000,  // 179   unassigned, L
        0x0000,  // 180   unassigned, L
        0x0000,  // 181   unassigned, L
        0x0000,  // 182   unassigned, L
        0x0000,  // 183   unassigned, L
        0x0000,  // 184   unassigned, L
        0x0000,  // 185   unassigned, L
        0x0001,  // 186   Lu, L
        0x0000,  // 187   unassigned, L
        0x0000,  // 188   unassigned, L
        0x0000,  // 189   unassigned, L
        0x0000,  // 190   unassigned, L
        0x0000,  // 191   unassigned, L
        0x0000,  // 192   unassigned, L
        0x0000,  // 193   unassigned, L
        0x0000,  // 194   unassigned, L
        0x0000,  // 195   unassigned, L
        0x0000,  // 196   unassigned, L
        0x0000,  // 197   unassigned, L
        0x0000,  // 198   unassigned, L
        0x0000,  // 199   unassigned, L
        0x0000,  // 200   unassigned, L
        0x0000,  // 201   unassigned, L
        0x0000,  // 202   unassigned, L
        0x0000,  // 203   unassigned, L
        0x0000,  // 204   unassigned, L
        0x0000,  // 205   unassigned, L
        0x0000,  // 206   unassigned, L
        0x0000,  // 207   unassigned, L
        0x0000,  // 208   unassigned, L
        0x0000,  // 209   unassigned, L
        0x0000,  // 210   unassigned, L
        0x0000,  // 211   unassigned, L
        0x0000,  // 212   unassigned, L
        0x0000,  // 213   unassigned, L
        0x0000,  // 214   unassigned, L
        0x0000,  // 215   unassigned, L
        0x0000,  // 216   unassigned, L
        0x0000,  // 217   unassigned, L
        0x0000,  // 218   unassigned, L
        0x0000,  // 219   unassigned, L
        0x0000,  // 220   unassigned, L
        0x0000,  // 221   unassigned, L
        0x0000,  // 222   unassigned, L
        0x0000,  // 223   unassigned, L
        0x0000,  // 224   unassigned, L
        0x0000,  // 225   unassigned, L
        0x0000,  // 226   unassigned, L
        0x0000,  // 227   unassigned, L
        0x0000,  // 228   unassigned, L
        0x0000,  // 229   unassigned, L
        0x0000,  // 230   unassigned, L
        0x0000,  // 231   unassigned, L
        0x0000,  // 232   unassigned, L
        0x0000,  // 233   unassigned, L
        0x0000,  // 234   unassigned, L
        0x0000,  // 235   unassigned, L
        0x0000,  // 236   unassigned, L
        0x0000,  // 237   unassigned, L
        0x0000,  // 238   unassigned, L
        0x0000,  // 239   unassigned, L
        0x0000,  // 240   unassigned, L
        0x0000,  // 241   unassigned, L
        0x0000,  // 242   unassigned, L
        0x0000,  // 243   unassigned, L
        0x0000,  // 244   unassigned, L
        0x0000,  // 245   unassigned, L
        0x0000,  // 246   unassigned, L
        0x0000,  // 247   unassigned, L
        0x0000,  // 248   unassigned, L
        0x0000,  // 249   unassigned, L
        0x0000,  // 250   unassigned, L
        0x0000,  // 251   unassigned, L
        0x0000,  // 252   unassigned, L
        0x0000,  // 253   unassigned, L
        0x0000,  // 254   unassigned, L
        0x0000   // 255   unassigned, L
    };
    
    /*
     * 生成方式：
     *
     * for (char i = 0; i < 256; i++) {
     *     int v = -1;
     *
     *     if (i >= '0' && i <= '9') {
     *         v = i - '0';
     *     }
     *
     *     else if (i >= 'A' && i <= 'Z') {
     *         v = i - 'A' + 10;
     *     } else if (i >= 'a' && i <= 'z') {
     *         v = i - 'a' + 10;
     *     }
     *
     *     if (i % 20 == 0) {
     *         System.out.println();
     *     }
     *
     *     System.out.printf("%2d, ", v);
     * }
     *
     * 预先生成数组，运行销量更高
     *
     * 反映某个字符在某个进制中代表的数值
     */
    private static final byte[] DIGITS = new byte[]{
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         0,  1,  2,  3,  4,  5,  6,  7,  8,  9, -1, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
        25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
        25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    
    static char[] sharpsMap = new char[]{'S', 'S'};
    
    static {
    }
    
    private CharacterDataLatin1() {
    }
    
    
    
    /*▼ 判断字符属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // true：该字符是Java空白符
    boolean isWhitespace(int ch) {
        // 获取字符属性编码
        int props = getProperties(ch);
        return ((props & 0x00007000) == 0x00004000);
    }
    
    // true：该字符是镜像字符，如<>[]()
    boolean isMirrored(int ch) {
        // 获取字符属性编码
        int props = getProperties(ch);
        return ((props & 0x80000000) != 0);
    }
    
    // true：该字符可位于Java标识符起始位置
    boolean isJavaIdentifierStart(int ch) {
        // 获取字符属性编码
        int props = getProperties(ch);
        return ((props & 0x00007000) >= 0x00005000);
    }
    
    // true：该字符可位于Java标识符的非起始部分
    boolean isJavaIdentifierPart(int ch) {
        // 获取字符属性编码
        int props = getProperties(ch);
        return ((props & 0x00003000) != 0);
    }
    
    // true：该字符可位于Unicode标识符的起始部分
    boolean isUnicodeIdentifierStart(int ch) {
        // 获取字符属性编码
        int props = getProperties(ch);
        return ((props & 0x00007000) == 0x00007000);
    }
    
    // true：该字符可位于Unicode标识符的非起始部分
    boolean isUnicodeIdentifierPart(int ch) {
        int props = getProperties(ch);
        return ((props & 0x00001000) != 0);
    }
    
    // true：该字符在标识符内是可忽略的
    boolean isIdentifierIgnorable(int ch) {
        // 获取字符属性编码
        int props = getProperties(ch);
        return ((props & 0x00007000) == 0x00001000);
    }
    
    // true：该字符是扩展的小写字符
    boolean isOtherLowercase(int ch) {
        int props = getPropertiesEx(ch);
        return (props & 0x0001) != 0;
    }
    
    // true：该字符是扩展的大写字符
    boolean isOtherUppercase(int ch) {
        int props = getPropertiesEx(ch);
        return (props & 0x0002) != 0;
    }
    
    // true：该字符是扩展的字母[Alphabetic]字符
    boolean isOtherAlphabetic(int ch) {
        int props = getPropertiesEx(ch);
        return (props & 0x0004) != 0;
    }
    
    // true：该字符是表意字符
    boolean isIdeographic(int ch) {
        int props = getPropertiesEx(ch);
        return (props & 0x0010) != 0;
    }
    
    /*▲ 判断字符属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ 获取字符属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取字符类型[字符属性后5位]（其含义参见Character类中的"Unicode符号分类代号"）
    int getType(int ch) {
        // 获取字符属性编码
        int props = getProperties(ch);
        return (props & 0x1F);
    }
    
    // 获取字符属性编码，定义在数组A中
    int getProperties(int ch) {
        char offset = (char) ch;
        int props = A[offset];
        return props;
    }
    
    // 获取扩展的字符属性编码，定义在数组B中
    int getPropertiesEx(int ch) {
        char offset = (char) ch;
        int props = B[offset];
        return props;
    }
    
    // 获取该字符的方向属性（文本有不同的书写方向，参见Character类中的"Unicode双向字符类型"）
    byte getDirectionality(int ch) {
        // 获取字符属性编码
        int val = getProperties(ch);
        
        byte directionality = (byte) ((val & 0x78000000) >> 27);
        
        if(directionality == 0xF) {
            directionality = -1;
        }
        return directionality;
    }
    
    /*▲ 获取字符属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ 字符属性转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 转为小写形式
    int toLowerCase(int ch) {
        int mapChar = ch;
        // 获取字符属性编码
        int val = getProperties(ch);
        
        if(((val & 0x00020000) != 0)    // 确定为大写形式
            && ((val & 0x07FC0000) != 0x07FC0000)) {
            // 先左移再右移，将除⑶之外的字符都抹为0，只剩第⑶部分的偏移量信息
            int offset = val << 5 >> (5 + 18);
            mapChar = ch + offset;
        }
        
        return mapChar;
    }
    
    // 转为大写形式
    int toUpperCase(int ch) {
        int mapChar = ch;
        // 获取字符属性编码
        int val = getProperties(ch);
        
        if((val & 0x00010000) != 0) {   // 确定为小写形式
            // 处理普通字符
            if((val & 0x07FC0000) != 0x07FC0000) {
                // 先左移再右移，将除⑶之外的字符都抹为0，只剩第⑶部分的偏移量信息
                int offset = val << 5 >> (5 + 18);
                mapChar = ch - offset;
            } else if(ch == 0x00B5) {   // 索引181处有个特殊的小写字母：µ，需要特殊处理
                mapChar = 0x039C;
            }
            
            // 未考虑索引223处的扩展的特殊小写字母ß
        }
        return mapChar;
    }
    
    // 转为大写形式（考虑出现的扩展字符）
    int toUpperCaseEx(int ch) {
        int mapChar = ch;
        // 获取字符属性编码
        int val = getProperties(ch);
        
        if((val & 0x00010000) != 0) {   // 确定为小写形式
            // 处理普通字符
            if((val & 0x07FC0000) != 0x07FC0000) {
                // 先左移再右移，将除⑶之外的字符都抹为0，只剩第⑶部分的偏移量信息
                int offset = val << 5 >> (5 + 18);
                mapChar = ch - offset;
            } else {
                switch(ch) {
                    // map overflow characters
                    case 0x00B5:
                        // 索引181处有个特殊的小写字母：µ，需要特殊处理
                        mapChar = 0x039C;
                        break;
                    default:
                        // 考虑索引223处扩展的特殊小写字母ß
                        mapChar = Character.ERROR;
                        break;
                }
            }
        }
        return mapChar;
    }
    
    // 将字符ch存入字符数组，如果是索引223处的字符ß，需要特殊处理
    char[] toUpperCaseCharArray(int ch) {
        char[] upperMap = {(char) ch};
        if(ch == 0x00DF) {
            // 处理索引223处扩展的特殊小写字母ß
            upperMap = sharpsMap;
        }
        return upperMap;
    }
    
    // 转为TitleCase形式，在这种字符集中，就是简单地转为大写形式
    int toTitleCase(int ch) {
        // 转为大写形式
        return toUpperCase(ch);
    }
    
    /*▲ 字符属性转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回字符ch表示的进制数值，如字母a或A将返回10（可用于16进制中）
    int digit(int ch, int radix) {
        int value = DIGITS[ch];
        return (value >= 0 && value < radix && radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX) ? value : -1;
    }
    
    // 返回字符ch在进制运算中表示的数值，如输入'3'返回3，输入'A'或'a'返回10
    int getNumericValue(int ch) {
        // 获取字符属性编码
        int val = getProperties(ch);
        int retval = -1;
        
        switch(val & 0xC00) {
            default:
                // cannot occur
            case (0x00000000):  // 0.非数值字符
                retval = -1;
                break;
            case (0x00000400):  // 1.简单数值字符，如0~9
                /*
                 * 0b01-0000 == (val & 0x3E0) >> 5
                 * 0b11-0000 == '0'
                 * 0b11-0001 == '1'
                 * 0b11-0002 == '2'
                 * 0b11-0003 == '3'
                 * 0b11-0004 == '4'
                 * 0b11-0005 == '5'
                 * 0b11-0006 == '6'
                 * 0b11-0007 == '7'
                 * 0b11-0008 == '8'
                 * 0b11-0009 == '9'
                 */
                retval = ch + ((val & 0x3E0) >> 5) & 0x1F;
                break;
            case (0x00000800):  // 2.非常规数值字符，如第189号索引处的½
                retval = -2;
                break;
            case (0x00000C00):  // 3.超级数值字符，如a~z、A~Z，可用在进制表示当中。
                /*
                 * 0b001-1111 == (val & 0x3E0) >> 5
                 * 0b100-0001 == 'A'
                 * 0b100-0010 == 'B'
                 * 0b100-0011 == 'C'
                 * 0b110-0001 == 'a'
                 * 0b110-0010 == 'b'
                 * 0b110-0011 == 'c'
                 */
                retval = (ch + ((val & 0x3E0) >> 5) & 0x1F) + 10;
                break;
        }
        return retval;
    }
}

