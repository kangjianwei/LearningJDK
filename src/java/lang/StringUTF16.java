/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.HotSpotIntrinsicCandidate;

import java.util.Arrays;
import java.util.Locale;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.LATIN1;
import static java.lang.String.UTF16;

// UTF16-String
final class StringUTF16 {
    static final int HI_BYTE_SHIFT, LO_BYTE_SHIFT;          // 大小端标记
    static final int MAX_LENGTH = Integer.MAX_VALUE >> 1;
    
    // 设置大小端标记
    static {
        if(isBigEndian()) {
            HI_BYTE_SHIFT = 8;
            LO_BYTE_SHIFT = 0;
        } else {
            HI_BYTE_SHIFT = 0;
            LO_BYTE_SHIFT = 8;
        }
    }
    
    
    
    /*▼ 获取char/char[] ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将UTF16-String内部的字节转换为char后返回
    @HotSpotIntrinsicCandidate
    static char getChar(byte[] val, int index) {
        assert index >= 0 && index < length(val) : "Trusted caller missed bounds check";
        index <<= 1;    // 获取字符序列
        return (char) (((val[index++] & 0xff) << HI_BYTE_SHIFT) | ((val[index] & 0xff) << LO_BYTE_SHIFT));
    }
    
    // 将UTF16-String内部的字节转换为char后返回，加入范围检查
    public static char charAt(byte[] value, int index) {
        // 越界检查
        checkIndex(index, value);
        return getChar(value, index);
    }
    
    // 将UTF16-String内部的字节批量转换为char后存入dst
    @HotSpotIntrinsicCandidate
    public static void getChars(byte[] value, int srcBegin, int srcEnd, char dst[], int dstBegin) {
        // 范围检查
        if(srcBegin < srcEnd) {
            checkBoundsOffCount(srcBegin, srcEnd - srcBegin, value);
        }
        for(int i = srcBegin; i < srcEnd; i++) {
            // 将UTF16-String内部的字节转换为char后返回
            dst[dstBegin++] = getChar(value, i);
        }
    }
    
    // 将UTF16-String内部的字节全部转换为char后返回
    public static char[] toChars(byte[] value) {
        char[] dst = new char[value.length >> 1];
        // 将UTF16-String内部的字节批量转换为char后存入dst
        getChars(value, 0, dst.length, dst, 0);
        return dst;
    }
    
    /*▲ 获取char/char[] ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取byte/byte[] ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将c的两个低字节转换为UTF16-String内部的字节后，存入val的index处
    @HotSpotIntrinsicCandidate
    static void putChar(byte[] val, int index, int c) {
        assert index >= 0 && index < length(val) : "Trusted caller missed bounds check";
        index <<= 1;
        val[index++] = (byte) (c >> HI_BYTE_SHIFT);
        val[index] = (byte) (c >> LO_BYTE_SHIFT);
    }
    
    // 将c的两个低字节转换为UTF16-SB内部的字节后，存入val的index处，加入了范围检查
    public static void putCharSB(byte[] val, int index, int c) {
        checkIndex(index, val);
        putChar(val, index, c);
    }
    
    // 将s[off, end)内部的字节批量转换为UTF16-SB内部的字节后，存入val的index处
    public static void putCharsSB(byte[] val, int index, CharSequence s, int off, int end) {
        checkBoundsBeginEnd(index, index + end - off, val);
        for(int i = off; i < end; i++) {
            putChar(val, index++, s.charAt(i));
        }
    }
    
    // 将str[off, end)内部的char批量转换为UTF16-SB内部的字节后，存入val的index处
    private static void putChars(byte[] val, int index, char[] str, int off, int end) {
        while(off < end) {
            putChar(val, index++, str[off++]);
        }
    }
    
    // 将ca[off, end)内部的char批量转换为UTF16-SB内部的字节后，存入val的index处，加入范围检查
    public static void putCharsSB(byte[] val, int index, char[] ca, int off, int end) {
        checkBoundsBeginEnd(index, index + end - off, val);
        putChars(val, index, ca, off, end);
    }
    
    // 将4个char依次存入UTF16-SB内部的字节
    public static int putCharsAt(byte[] value, int i, char c1, char c2, char c3, char c4) {
        int end = i + 4;
        checkBoundsBeginEnd(i, end, value);
        putChar(value, i++, c1);
        putChar(value, i++, c2);
        putChar(value, i++, c3);
        putChar(value, i++, c4);
        assert (i == end);
        return end;
    }
    
    // 将5个char依次存入UTF16-SB内部的字节
    public static int putCharsAt(byte[] value, int i, char c1, char c2, char c3, char c4, char c5) {
        int end = i + 5;
        checkBoundsBeginEnd(i, end, value);
        putChar(value, i++, c1);
        putChar(value, i++, c2);
        putChar(value, i++, c3);
        putChar(value, i++, c4);
        putChar(value, i++, c5);
        assert (i == end);
        return end;
    }
    
    // 将char转换为UTF16-String内部的字节，并返回
    public static byte[] toBytes(char c) {
        byte[] result = new byte[2];
        putChar(result, 0, c);
        return result;
    }
    
    // 将value[off, off+len)中的char批量转换为UTF16-S内部的字节，并返回
    @HotSpotIntrinsicCandidate
    public static byte[] toBytes(char[] value, int off, int len) {
        // 创建长度为2*len的字节数组
        byte[] val = newBytesFor(len);
        for(int i = 0; i < len; i++) {
            // 将value[off]转换为UTF16-String内部的字节，存入val
            putChar(val, i, value[off]);
            off++;
        }
        return val;
    }
    
    /**
     * int[] val = new int[]{0x56DB, 0x6761, 0x2A6A5};  // 分别是【四】【条】【𪚥】这三个字的Unicode编码值
     * toBytes(val, 0, 3);  // 返回字节数组：[0x56, 0xDB, 0x67, 0x61, 0xD8, 0x69, 0xDE, 0xA5]
     * 注：0x2A6A5是一个增补字符的编码，需要先将其拆分为高低代理单元对<0xD869,0xDEA5>，然后再转为字节存储
     */
    // 将val中的一组Unicode值批量转换为UTF16-String内部的字节，存入val的index处，再返回
    public static byte[] toBytes(int[] val, int index, int len) {
        final int end = index + len;
        
        // Pass 1: 根据Unicode值，计算码元（char）的个数
        
        int n = len;    // 计算需要占几个char的空间
        for(int i = index; i < end; i++) {
            int cp = val[i];
            if(Character.isBmpCodePoint(cp)) {
                continue;
            } else if(Character.isValidCodePoint(cp)) {
                n++;    // 如果是增补字符，则意味着需要多占一个char的空间
            } else {
                throw new IllegalArgumentException(Integer.toString(cp));
            }
        }
        
        // Pass 2: 填充高低代理对
        
        // 创建长度为2*n的字节数组
        byte[] buf = newBytesFor(n);
        for(int i = index, j = 0; i < end; i++, j++) {
            int cp = val[i];
            if(Character.isBmpCodePoint(cp)) {
                putChar(buf, j, cp);
            } else {
                putChar(buf, j++, Character.highSurrogate(cp)); // 返回高代理处的码元（char）
                putChar(buf, j,   Character.lowSurrogate(cp));  // 返回低代理处的码元（char）
            }
        }
        return buf;
    }
    
    // 返回Unicode增补符号cp的四字节表示
    static byte[] toBytesSupplementary(int cp) {
        byte[] result = new byte[4];
        putChar(result, 0, Character.highSurrogate(cp));    // 返回高代理处的码元（char）
        putChar(result, 1, Character.lowSurrogate(cp));     // 返回低代理处的码元（char）
        return result;
    }
    
    // 将src(LATIN1-String)内部的字节批量转换为UTF16-String内部的字节，存入dst的dstOff处
    public static void inflate(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        // 下标检查
        checkBoundsOffCount(dstOff, len, dst);
        for(int i = 0; i < len; i++) {
            putChar(dst, dstOff++, src[srcOff++] & 0xff);
        }
    }
    
    /*▲ 获取byte/byte[] ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * 先将UTF16-String内部的字节存入一个char，再取这个char的一个低字节存入LATIN1-String内部的字节
     * 如遇转换后的char在Latin1字符集之外，即遇到超出[0x00, 0xFF)范围的char，则停止压缩。
     *
     * byte[] src = new byte[]{0x00,0x12, 0x00,0x34, 0x00,0x56};
     * byte[] dst = new byte[2];
     * compress(src, 1, dst, 0, 2); // dst：[0x34, 0x56]
     */
    // 将UTF16-String内部的字节转换为LATIN1-String内部的字节，加入范围检查
    @HotSpotIntrinsicCandidate
    public static int compress(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        // 下标范围检查
        checkBoundsOffCount(srcOff, len, src);
        for(int i = 0; i < len; i++) {
            // 将UTF16-String内部的字节转换为char后返回
            char c = getChar(src, srcOff);
            // 超出了Latin1字符集表示范围
            if(c > 0xFF) {
                len = 0;
                break;
            }
            dst[dstOff] = (byte) c;
            srcOff++;
            dstOff++;
        }
        
        // 返回压缩成功的字节对数量，如果中途失败，则为0
        return len;
    }
    
    /**
     * 先将UTF16-String内部的字节存入一个char，再取这个char的一个低字节存入LATIN1-String内部的字节，再返回
     * 如遇转换后的char在Latin1字符集之外，即遇到超出[0x00, 0xFF)范围的char，则停止压缩。
     *
     * byte[] val = new byte[]{0x00,0x12, 0x00,0x34, 0x00,0x56};
     * compress(val, 1, 2); // 返回字节数组：[0x34, 0x56]
     */
    // 将UTF16-String内部的字节转换为LATIN1-String内部的字节后，再返回
    public static byte[] compress(byte[] val, int off, int len) {
        byte[] ret = new byte[len];
        if(compress(val, off, ret, 0, len) == len) {
            // 如果成功完成指定范围内的压缩，则返回压缩后的字符序列的字节表示
            return ret;
        }
        // 如果不能完成压缩任务，则返回nll
        return null;
    }
    
    /**
     * 将UTF16-String内部的字节转换为LATIN1-String内部的字节
     * 如遇char在Latin1字符集之外，即遇到超出[0x00, 0xFF)范围的char，则停止压缩。
     *
     * char[] src = new char[]{'\u0012', '\u0034', '\u0056'};
     * byte[] dst = new byte[2];
     * compress(src, 1, dst, 0, 2); // dst：[0x34, 0x56]
     */
    // 将UTF16-String内部的字节转换为LATIN1-String内部的字节
    @HotSpotIntrinsicCandidate
    public static int compress(char[] src, int srcOff, byte[] dst, int dstOff, int len) {
        for(int i = 0; i < len; i++) {
            char c = src[srcOff];
            if(c > 0xFF) {
                len = 0;
                break;
            }
            dst[dstOff] = (byte) c;
            srcOff++;
            dstOff++;
        }
        // 返回成功成功压缩的char的数量，如果中途失败，则为0
        return len;
    }
    
    /**
     * 将UTF16-String内部的字节转换为LATIN1-String内部的字节
     * 如遇char在Latin1字符集之外，即遇到超出[0x00, 0xFF)范围的char，则停止压缩。
     *
     * char[] src = new char[]{'\u0012', '\u0034', '\u0056'};
     * compress(src, 1, 2); // 返回字节数组：[0x34, 0x56]
     */
    // 将UTF16-String内部的字节转换为LATIN1-String内部的字节
    public static byte[] compress(char[] val, int off, int len) {
        byte[] ret = new byte[len];
        if(compress(val, off, ret, 0, len) == len) {
            // 如果成功完成指定范围内的压缩，则返回压缩后的字符序列的字节表示
            return ret;
        }
        // 如果不能完成压缩任务，则返回nll
        return null;
    }
    
    /*
     * 将UTF16-String内部的字节转换为LATIN1-String内部的字节，类似于压缩
     * 要想数据无损转换，则原始字节对表示的char必须在[0x00, 0xFF]范围
     *
     * byte[] value = new byte[]{0x12,0x34, 0x56,0x78, 0xAB,0xCD};
     * getBytes(value, 1, 3, dst, 0);   // dst数组：[0x78, 0xCD]
     */
    public static void getBytes(byte[] value, int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        srcBegin <<= 1;
        srcEnd <<= 1;
        for(int i = srcBegin + (1 >> LO_BYTE_SHIFT); i < srcEnd; i += 2) {
            dst[dstBegin++] = value[i];
        }
    }
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 大小写转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 小写转换，需要指定语言环境,其中value存储了str的字节表现形式
    public static String toLowerCase(String str, byte[] value, Locale locale) {
        if(locale == null) {
            throw new NullPointerException();
        }
        int first;
        boolean hasSurr = false;
        final int len = value.length >> 1;
        
        // Now check if there are any characters that need to be changed, or are surrogate
        for(first = 0; first < len; first++) {
            int cp = (int) getChar(value, first);
            if(Character.isSurrogate((char) cp)) {
                hasSurr = true;
                break;
            }
            if(cp != Character.toLowerCase(cp)) {  // no need to check Character.ERROR
                break;
            }
        }
        if(first == len)
            return str;
        byte[] result = new byte[value.length];
        System.arraycopy(value, 0, result, 0, first << 1);  // Just copy the first few
        // lowerCase characters.
        String lang = locale.getLanguage();
        if(lang == "tr" || lang == "az" || lang == "lt") {
            return toLowerCaseEx(str, value, result, first, locale, true);
        }
        if(hasSurr) {
            return toLowerCaseEx(str, value, result, first, locale, false);
        }
        int bits = 0;
        for(int i = first; i < len; i++) {
            int cp = (int) getChar(value, i);
            if(cp == '\u03A3' ||                       // GREEK CAPITAL LETTER SIGMA
                Character.isSurrogate((char) cp)) {
                return toLowerCaseEx(str, value, result, i, locale, false);
            }
            if(cp == '\u0130') {                       // LATIN CAPITAL LETTER I WITH DOT ABOVE
                return toLowerCaseEx(str, value, result, i, locale, true);
            }
            cp = Character.toLowerCase(cp);
            if(!Character.isBmpCodePoint(cp)) {
                return toLowerCaseEx(str, value, result, i, locale, false);
            }
            bits |= cp;
            putChar(result, i, cp);
        }
        if(bits > 0xFF) {
            return new String(result, UTF16);
        } else {
            return newString(result, 0, len);
        }
    }
    
    // 大写转换，需要指定语言环境,其中value存储了str的字节表现形式
    public static String toUpperCase(String str, byte[] value, Locale locale) {
        if(locale == null) {
            throw new NullPointerException();
        }
        int first;
        boolean hasSurr = false;
        final int len = value.length >> 1;
        
        // Now check if there are any characters that need to be changed, or are surrogate
        for(first = 0; first < len; first++) {
            int cp = (int) getChar(value, first);
            if(Character.isSurrogate((char) cp)) {
                hasSurr = true;
                break;
            }
            if(cp != Character.toUpperCaseEx(cp)) {   // no need to check Character.ERROR
                break;
            }
        }
        if(first == len) {
            return str;
        }
        byte[] result = new byte[value.length];
        System.arraycopy(value, 0, result, 0, first << 1); // Just copy the first few
        // upperCase characters.
        String lang = locale.getLanguage();
        if(lang == "tr" || lang == "az" || lang == "lt") {
            return toUpperCaseEx(str, value, result, first, locale, true);
        }
        if(hasSurr) {
            return toUpperCaseEx(str, value, result, first, locale, false);
        }
        int bits = 0;
        for(int i = first; i < len; i++) {
            int cp = (int) getChar(value, i);
            if(Character.isSurrogate((char) cp)) {
                return toUpperCaseEx(str, value, result, i, locale, false);
            }
            cp = Character.toUpperCaseEx(cp);
            if(!Character.isBmpCodePoint(cp)) {    // Character.ERROR is not bmp
                return toUpperCaseEx(str, value, result, i, locale, false);
            }
            bits |= cp;
            putChar(result, i, cp);
        }
        if(bits > 0xFF) {
            return new String(result, UTF16);
        } else {
            return newString(result, 0, len);
        }
    }
    
    // 小写转换，处理增补字符以及一些特殊语言的场景
    private static String toLowerCaseEx(String str, byte[] value, byte[] result, int first, Locale locale, boolean localeDependent) {
        assert (result.length == value.length);
        assert (first >= 0);
        int resultOffset = first;
        int length = value.length >> 1;
        int srcCount;
        for(int i = first; i < length; i += srcCount) {
            int srcChar = getChar(value, i);
            int lowerChar;
            char[] lowerCharArray;
            srcCount = 1;
            if(Character.isSurrogate((char) srcChar)) {
                srcChar = codePointAt(value, i, length);
                srcCount = Character.charCount(srcChar);
            }
            if(localeDependent || srcChar == '\u03A3' ||  // GREEK CAPITAL LETTER SIGMA
                srcChar == '\u0130') {  // LATIN CAPITAL LETTER I WITH DOT ABOVE
                lowerChar = ConditionalSpecialCasing.toLowerCaseEx(str, i, locale);
            } else {
                lowerChar = Character.toLowerCase(srcChar);
            }
            if(Character.isBmpCodePoint(lowerChar)) {    // Character.ERROR is not a bmp
                putChar(result, resultOffset++, lowerChar);
            } else {
                if(lowerChar == Character.ERROR) {
                    lowerCharArray = ConditionalSpecialCasing.toLowerCaseCharArray(str, i, locale);
                } else {
                    // 解码，Unicode码点值 ---> char，对于增补平面区码点值，需要拆分成高、低代理单元再存储
                    lowerCharArray = Character.toChars(lowerChar);
                }
                /* Grow result if needed */
                int mapLen = lowerCharArray.length;
                if(mapLen > srcCount) {
                    // 创建长度为2*len的字节数组
                    byte[] result2 = newBytesFor((result.length >> 1) + mapLen - srcCount);
                    System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                assert resultOffset >= 0;
                assert resultOffset + mapLen <= length(result);
                for(int x = 0; x < mapLen; ++x) {
                    putChar(result, resultOffset++, lowerCharArray[x]);
                }
            }
        }
        return newString(result, 0, resultOffset);
    }
    
    // 大写转换，处理增补字符以及一些特殊语言的场景
    private static String toUpperCaseEx(String str, byte[] value, byte[] result, int first, Locale locale, boolean localeDependent) {
        assert (result.length == value.length);
        assert (first >= 0);
        int resultOffset = first;
        int length = value.length >> 1;
        int srcCount;
        for(int i = first; i < length; i += srcCount) {
            int srcChar = getChar(value, i);
            int upperChar;
            char[] upperCharArray;
            srcCount = 1;
            if(Character.isSurrogate((char) srcChar)) {
                srcChar = codePointAt(value, i, length);
                srcCount = Character.charCount(srcChar);
            }
            if(localeDependent) {
                upperChar = ConditionalSpecialCasing.toUpperCaseEx(str, i, locale);
            } else {
                upperChar = Character.toUpperCaseEx(srcChar);
            }
            if(Character.isBmpCodePoint(upperChar)) {
                putChar(result, resultOffset++, upperChar);
            } else {
                if(upperChar == Character.ERROR) {
                    if(localeDependent) {
                        upperCharArray = ConditionalSpecialCasing.toUpperCaseCharArray(str, i, locale);
                    } else {
                        upperCharArray = Character.toUpperCaseCharArray(srcChar);
                    }
                } else {
                    // 解码，Unicode码点值 ---> char，对于增补平面区码点值，需要拆分成高、低代理单元再存储
                    upperCharArray = Character.toChars(upperChar);
                }
                /* Grow result if needed */
                int mapLen = upperCharArray.length;
                if(mapLen > srcCount) {
                    // 创建长度为2*len的字节数组
                    byte[] result2 = newBytesFor((result.length >> 1) + mapLen - srcCount);
                    System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                assert resultOffset >= 0;
                assert resultOffset + mapLen <= length(result);
                for(int x = 0; x < mapLen; ++x) {
                    putChar(result, resultOffset++, upperCharArray[x]);
                }
            }
        }
        return newString(result, 0, resultOffset);
    }
    
    /*▲ 大小写转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 码点/码元 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * ▶ 1 返回byte[]:value中某处所代表的符号的Unicode编码（从前到后试探）
     *
     * byte[] value = new byte[]{0x56,0xDB, 0x67,0x61, 0xD8,0x69, 0xDE,0xA5};
     * codePointAt(value, 1, 2, true);  // 返回0x6761，这是汉字【条】的Unicode编码值
     * codePointAt(value, 2, 4, true);  // 返回0x2A6A5，这是汉字【𪚥】的Unicode编码值，这是个增补字符，其UTF-16BE编码的编码为：\uD869\uDEA5
     * codePointAt(value, 2, 3, true);  // 返回0xD869，索引限制了继续往后判断，所以只返回了一个高代理单元，无法构成正确的Unicode符号
     * codePointAt(value, 3, 4, true);  // 返回0xDEA5，后面没有字节了，所以只返回了一个低代理单元，无法构成正确的Unicode符号
     */
    // ▶ 1 返回UTF16-S中某处符号（双字节/四字节）的Unicode编码
    private static int codePointAt(byte[] value, int index, int end, boolean checked) {
        assert index < end;
        if(checked) {
            checkIndex(index, value);
        }
        
        // 将UTF16-String内部的字节转换为char后返回
        char c1 = getChar(value, index);
        
        // 如果出现了增补字符，一次遍历4个字节
        if(Character.isHighSurrogate(c1) && ++index < end) {
            if(checked) {
                checkIndex(index, value);
            }
            char c2 = getChar(value, index);
            if(Character.isLowSurrogate(c2)) {
                // 高、低代理区的码点值 ---> Unicode符号编码值
                return Character.toCodePoint(c1, c2);
            }
        }
        return c1;
    }
    
    // ▶ 1-1 返回UTF16-SB中某处符号（双字节/四字节）的Unicode编码（从前到后试探）
    public static int codePointAtSB(byte[] val, int index, int end) {
        return codePointAt(val, index, end, true /*checked*/);
    }
    
    // ▶ 1-2 返回UTF16-String中某处符号（双字节/四字节）的Unicode编码（从前到后试探）
    public static int codePointAt(byte[] value, int index, int end) {
        return codePointAt(value, index, end, false /*unchecked*/);
    }
    
    /*
     * byte[] value = new byte[]{0x56,0xDB, 0x67,0x61, 0xD8,0x69, 0xDE,0xA5}; // 四条𪚥
     * codePointAt(value, 2, true);  // 返回0x6761，这是汉字【条】的Unicode编码值
     * codePointAt(value, 4, true);  // 返回0x2A6A5，这是汉字【𪚥】的Unicode编码值，这是个增补字符，其UTF-16BE编码的编码为：\uD869\uDEA5
     * codePointAt(value, 3, true);  // 返回0xD869，只返回了一个高代理单元，无法构成正确的Unicode符号
     */
    // ▶ 2 返回UTF16-S中某处(index-1)符号（双字节/四字节）的Unicode编码（从后往前试探）
    private static int codePointBefore(byte[] value, int index, boolean checked) {
        --index;
        if(checked) {
            checkIndex(index, value);
        }
        char c2 = getChar(value, index);
        if(Character.isLowSurrogate(c2) && index > 0) {
            --index;
            if(checked) {
                checkIndex(index, value);
            }
            char c1 = getChar(value, index);
            if(Character.isHighSurrogate(c1)) {
                // 高、低代理区的码点值 ---> Unicode符号编码值
                return Character.toCodePoint(c1, c2);
            }
        }
        return c2;
    }
    
    // ▶ 2-1 返回UTF16-SB中某处(index-1)符号（双字节/四字节）的Unicode编码（从后往前试探）
    public static int codePointBeforeSB(byte[] val, int index) {
        return codePointBefore(val, index, true /*checked*/);
    }
    
    // ▶ 2-2 返回UTF16-String中某处(index-1)符号（双字节/四字节）的Unicode编码（从后往前试探）
    public static int codePointBefore(byte[] value, int index) {
        return codePointBefore(value, index, false /* unchecked */);
    }
    
    /**
     * 四U+56DB，条U+6761，𪚥U+2A6A5，其中𪚥的UTF-16大端法表示形式是：\uD869\uDEA5
     * byte[] value = new byte[]{0x56,0xDB, 0x67,0x61, 0xD8,0x69, 0xDE,0xA5};
     * codePointCount(value, 0, 4, true);  // 返回3，识别了全部三个Unicode符号
     * codePointCount(value, 0, 3, true);  // 返回3，识别出了前两个Unicode符号和一个只存在高代理单元的符号
     * codePointCount(value, 0, 2, true);  // 返回2，识别了前两个Unicode符号
     */
    // ▶ 3 统计UTF16-S中指定码元范围内存在多少个Unicode符号
    private static int codePointCount(byte[] value, int beginIndex, int endIndex, boolean checked) {
        assert beginIndex <= endIndex;
        
        int count = endIndex - beginIndex;  // 待判断的码元个数
        int i = beginIndex;
        if(checked && i < endIndex) {
            checkBoundsBeginEnd(i, endIndex, value);
        }
        
        // 以码元为单位，判断其是否属于四字节字符，如果是的话，修正Unicode符号个数
        for(; i < endIndex - 1; ) {
            if(Character.isHighSurrogate(getChar(value, i++)) && Character.isLowSurrogate(getChar(value, i))) {
                count--;
                i++;
            }
        }
        return count;
    }
    
    // ▶ 3-1 统计UTF16-SB中指定码元范围内存在多少个Unicode符号
    public static int codePointCountSB(byte[] val, int beginIndex, int endIndex) {
        return codePointCount(val, beginIndex, endIndex, true /*checked*/);
    }
    
    // ▶ 3-2 统计UTF16-String中指定码元范围内存在多少个Unicode符号
    public static int codePointCount(byte[] value, int beginIndex, int endIndex) {
        return codePointCount(value, beginIndex, endIndex, false /*unchecked*/);
    }
    
    /*▲ 码点/码元 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 比较/判等 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // ▶ 1 比较两个UTF16-String的字节值，需要先将它们同时转为char再比较
    private static int compareValues(byte[] value, byte[] other, int len1, int len2) {
        int lim = Math.min(len1, len2);
        for(int k = 0; k < lim; k++) {
            char c1 = getChar(value, k);
            char c2 = getChar(other, k);
            if(c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }
    
    // ▶ 1-1 比较两个UTF16-String的字节值，需要先将它们同时转为char再比较
    @HotSpotIntrinsicCandidate
    public static int compareTo(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = length(other);
        return compareValues(value, other, len1, len2);
    }
    
    // ▶ 1-2 比较两个UTF16-String的字节值，需要先将它们同时转为char再比较，加入了范围检查
    public static int compareTo(byte[] value, byte[] other, int len1, int len2) {
        checkOffset(len1, value);
        checkOffset(len2, other);
        
        return compareValues(value, other, len1, len2);
    }
    
    // ▶ 2 比较UTF16-String的字节值(value)和Latin1-String的字节值(other)，需要先将它们同时转为char再比较
    @HotSpotIntrinsicCandidate
    public static int compareToLatin1(byte[] value, byte[] other) {
        return -StringLatin1.compareToUTF16(other, value);
    }
    
    // ▶ 3 比较UTF16-String的字节值(value)和Latin1-String的字节值(other)，需要先将它们同时转为char再比较
    public static int compareToLatin1(byte[] value, byte[] other, int len1, int len2) {
        return -StringLatin1.compareToUTF16(other, value, len2, len1);
    }
    
    // ▶ 4 忽略大小写地比较两个UTF16-String的字节值
    public static int compareToCI(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = length(other);
        int lim = Math.min(len1, len2);
        for(int k = 0; k < lim; k++) {
            char c1 = getChar(value, k);
            char c2 = getChar(other, k);
            if(c1 != c2) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if(c1 != c2) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if(c1 != c2) {
                        return c1 - c2;
                    }
                }
            }
        }
        return len1 - len2;
    }
    
    // ▶ 5 忽略大小写地比较两个UTF16-String的字节值
    public static boolean regionMatchesCI(byte[] value, int toffset, byte[] other, int ooffset, int len) {
        int last = toffset + len;
        
        assert toffset >= 0 && ooffset >= 0;
        assert ooffset + len <= length(other);
        assert last <= length(value);
        
        while(toffset < last) {
            char c1 = getChar(value, toffset++);
            char c2 = getChar(other, ooffset++);
            if(c1 == c2) {
                continue;
            }
            // try converting both characters to uppercase.
            // If the results match, then the comparison scan should continue.
            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if(u1 == u2) {
                continue;
            }
            // Unfortunately, conversion to uppercase does not work properly for the Georgian alphabet, which has strange rules about case conversion.
            // So we need to make one last check before exiting.
            if(Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                continue;
            }
            return false;
        }
        return true;
    }
    
    // ▶ 6 忽略大小写地比较UTF16-String的字节值(value)和Latin1-String的字节值(other)，需要先将它们同时转为char再比较
    public static int compareToCI_Latin1(byte[] value, byte[] other) {
        return -StringLatin1.compareToCI_UTF16(other, value);
    }
    
    // ▶ 7 忽略大小写地比较UTF16-String的字节值(value)和Latin1-String的字节值(other)，需要先将它们同时转为char再比较
    public static boolean regionMatchesCI_Latin1(byte[] value, int toffset, byte[] other, int ooffset, int len) {
        return StringLatin1.regionMatchesCI_UTF16(other, ooffset, value, toffset, len);
    }
    
    // true：两个UTF16-String内容相等
    @HotSpotIntrinsicCandidate
    public static boolean equals(byte[] value, byte[] other) {
        if(value.length == other.length) {
            int len = value.length >> 1;
            for(int i = 0; i < len; i++) {
                if(getChar(value, i) != getChar(other, i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    // true：LATIN1-String v1 和 UTF16-String v2 表示的内涵一致
    public static boolean contentEquals(byte[] v1, byte[] v2, int len) {
        checkBoundsOffCount(0, len, v2);
        for(int i = 0; i < len; i++) {
            if((char) (v1[i] & 0xff) != getChar(v2, i)) {
                return false;
            }
        }
        return true;
    }
    
    // true：UTF16-String value 和 CharSequence:cs 表示的内涵一致
    public static boolean contentEquals(byte[] value, CharSequence cs, int len) {
        checkOffset(len, value);
        for(int i = 0; i < len; i++) {
            if(getChar(value, i) != cs.charAt(i)) {
                return false;
            }
        }
        return true;
    }
    
    /*▲ 比较/判等 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找Unicode符号下标 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 注意此节中术语【位置】与byte数组中的元素索引的区别
     *
     * 比如对于byte[] bs = {0x12,0x34, 0x56,0x78}：
     * 我们说字符'\u5678'在bs中的位置是1。
     */
    
    /**
     * byte[] value = new byte[]{0x12,0x34, 0x56,0x78, 0xAB,0xCD};
     * indexOfCharUnsafe(value, 0x5678, 0, 3);    // 返回1
     */
    // ▶ 1 返回基本符号ch在UTF16-String的字节值value中的下标
    private static int indexOfCharUnsafe(byte[] value, int ch, int fromIndex, int max) {
        for(int i = fromIndex; i < max; i++) {
            if(getChar(value, i) == ch) {
                return i;
            }
        }
        return -1;
    }
    
    // ▶ 1-1 返回基本符号ch在UTF16-String的字节值value中的下标，加入范围检查
    @HotSpotIntrinsicCandidate
    private static int indexOfChar(byte[] value, int ch, int fromIndex, int max) {
        checkBoundsBeginEnd(fromIndex, max, value);
        return indexOfCharUnsafe(value, ch, fromIndex, max);
    }
    
    /**
     * Handles (rare) calls of indexOf with a supplementary character.
     */
    /*
     * ▶ 2 返回增补符号ch在UTF16-String的字节值value中的下标，加入范围检查
     *
     * byte[] value = new byte[]{0x56,0xDB, 0x67,0x61, 0xD8,0x69, 0xDE,0xA5};
     * indexOfSupplementary(value, 0x2A6A5, 0, 4);  // 返回2
     * 因为0x2A6A5这个Unicode编码值拆成UTF-16编码大端表示法后就是：0xD869 0xDEA5
     */
    private static int indexOfSupplementary(byte[] value, int ch, int fromIndex, int max) {
        if(Character.isValidCodePoint(ch)) {
            final char hi = Character.highSurrogate(ch);    // 返回高代理处的码元（char）
            final char lo = Character.lowSurrogate(ch);     // 返回低代理处的码元（char）
            checkBoundsBeginEnd(fromIndex, max, value);
            for(int i = fromIndex; i < max - 1; i++) {
                if(getChar(value, i) == hi && getChar(value, i + 1) == lo) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    // ▶ 3 返回符号ch在UTF16-String的字节值value中的下标，内部包括了范围检查
    public static int indexOf(byte[] value, int ch, int fromIndex) {
        int max = value.length >> 1;
        if(fromIndex < 0) {
            fromIndex = 0;
        } else if(fromIndex >= max) {
            // Note: fromIndex might be near -1>>>1.
            return -1;
        }
        if(ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            // handle most cases here (ch is a BMP code point or a negative value (invalid code point))
            return indexOfChar(value, ch, fromIndex, max);
        } else {
            return indexOfSupplementary(value, ch, fromIndex, max);
        }
    }
    
    // 返回Unicode符号ch在UTF16-String的字节值value中最后一次出现的下标
    public static int lastIndexOf(byte[] value, int ch, int fromIndex) {
        if(ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            // handle most cases here (ch is a BMP code point or a negative value (invalid code point))
            int i = Math.min(fromIndex, (value.length >> 1) - 1);
            for(; i >= 0; i--) {
                if(getChar(value, i) == ch) {
                    return i;
                }
            }
            return -1;
        } else {
            return lastIndexOfSupplementary(value, ch, fromIndex);
        }
    }
    
    /**
     * Handles (rare) calls of lastIndexOf with a supplementary character.
     */
    // 返回增补符号ch在UTF16-String的字节值value中最后一次出现的下标
    private static int lastIndexOfSupplementary(final byte[] value, int ch, int fromIndex) {
        if(Character.isValidCodePoint(ch)) {
            char hi = Character.highSurrogate(ch);  // 返回高代理处的码元（char）
            char lo = Character.lowSurrogate(ch);   // 返回低代理处的码元（char）
            int i = Math.min(fromIndex, (value.length >> 1) - 2);
            for(; i >= 0; i--) {
                if(getChar(value, i) == hi && getChar(value, i + 1) == lo) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /*▲ 查找Unicode符号下标 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找子串下标 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 比对两个UTF16-String，返回子串str在主串value中第一次出现的下标
    @HotSpotIntrinsicCandidate
    public static int indexOf(byte[] value, byte[] str) {
        if(str.length == 0) {
            return 0;
        }
        if(value.length < str.length) {
            return -1;
        }
        return indexOfUnsafe(value, length(value), str, length(str), 0);
    }
    
    /*
     * 比对两个UTF16-String，返回子串str在主串value中第一次出现的下标
     * 搜索时只比对主串的前valueCount个字符和子串的前strCount个字符，且从主串的fromIndex索引处向后搜索
     */
    @HotSpotIntrinsicCandidate
    public static int indexOf(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex) {
        checkBoundsBeginEnd(fromIndex, valueCount, value);
        checkBoundsBeginEnd(0, strCount, str);
        return indexOfUnsafe(value, valueCount, str, strCount, fromIndex);
    }
    
    /**
     * 比对两个UTF16-String，返回子串str在主串value中第一次出现的下标
     * 搜索时只比对主串的前srcCount个字符和子串的前tgtCount个字符，且从主串fromIndex索引处向后搜索
     */
    private static int indexOfUnsafe(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex) {
        assert fromIndex >= 0;
        assert strCount > 0;
        assert strCount <= length(str);
        assert valueCount >= strCount;
        
        char first = getChar(str, 0);   // 子串第一个字符
        // 主串长度-子串长度
        int max = (valueCount - strCount);
        for(int i = fromIndex; i <= max; i++) {
            // 用i遍历主串，直到主串和子串第一个字符相等为止
            if(getChar(value, i) != first) {
                while(++i <= max && getChar(value, i) != first)
                    ;
            }
            // 找到了第一个相等字符，此时游标i最远也必须满足i<=valueCount - strCount，否则就没必要比较了，可画图理解
            if(i <= max) {
                int j = i + 1;
                int end = j + strCount - 1;
                for(int k = 1; j < end && getChar(value, j) == getChar(str, k); j++, k++)
                    ;
                if(j == end) {
                    // Found whole string.
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * Handles indexOf Latin1 substring in UTF16 string.
     */
    // 比对UTF16-String主串src和Latin1-String子串str，返回子串str在主串value中第一次出现的下标
    @HotSpotIntrinsicCandidate
    public static int indexOfLatin1(byte[] value, byte[] str) {
        if(str.length == 0) {
            return 0;
        }
        if(length(value) < str.length) {
            return -1;
        }
        return indexOfLatin1Unsafe(value, length(value), str, str.length, 0);
    }
    
    // 比对UTF16-String主串src和Latin1-String子串tgt，返回子串str在主串src中第一次出现的下标，加入了范围检查（从主串fromIndex索引处向后搜索）
    @HotSpotIntrinsicCandidate
    public static int indexOfLatin1(byte[] src, int srcCount, byte[] tgt, int tgtCount, int fromIndex) {
        checkBoundsBeginEnd(fromIndex, srcCount, src);
        String.checkBoundsBeginEnd(0, tgtCount, tgt.length);
        return indexOfLatin1Unsafe(src, srcCount, tgt, tgtCount, fromIndex);
    }
    
    // 比对UTF16-String主串src和Latin1-String子串tgt，返回子串tgt在主串src中第一次出现的下标（从主串fromIndex索引处向后搜索）
    public static int indexOfLatin1Unsafe(byte[] src, int srcCount, byte[] tgt, int tgtCount, int fromIndex) {
        assert fromIndex >= 0;
        assert tgtCount > 0;
        assert tgtCount <= tgt.length;
        assert srcCount >= tgtCount;
        
        char first = (char) (tgt[0] & 0xff);    // 子串第一个字符
        int max = (srcCount - tgtCount);
        for(int i = fromIndex; i <= max; i++) {
            // Look for first character.
            if(getChar(src, i) != first) {
                while(++i <= max && getChar(src, i) != first)
                    ;
            }
            // Found first character, now look at the rest of v2
            if(i <= max) {
                int j = i + 1;
                int end = j + tgtCount - 1;
                for(int k = 1; j < end && getChar(src, j) == (tgt[k] & 0xff); j++, k++)
                    ;
                if(j == end) {
                    // Found whole string.
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * 比对UTF16-String主串src和Latin1子串tgt，返回子串tgt在主串src中最后一次出现的下标
     * 搜索时只比对主串的前srcCount个字符和子串的前tgtCount个字符，且从主串的fromIndex索引处向前搜索
     */
    public static int lastIndexOf(byte[] src, int srcCount, byte[] tgt, int tgtCount, int fromIndex) {
        assert fromIndex >= 0;
        assert tgtCount > 0;
        assert tgtCount <= length(tgt);
        
        int min = tgtCount - 1;
        int i = min + fromIndex;
        int strLastIndex = tgtCount - 1;
        
        checkIndex(strLastIndex, tgt);
        char strLastChar = getChar(tgt, strLastIndex);
        
        checkIndex(i, src);

startSearchForLastChar:
        while(true) {
            while(i >= min && getChar(src, i) != strLastChar) {
                i--;
            }
            if(i < min) {
                return -1;
            }
            int j = i - 1;
            int start = j - strLastIndex;
            int k = strLastIndex - 1;
            while(j > start) {
                if(getChar(src, j--) != getChar(tgt, k--)) {
                    i--;
                    continue startSearchForLastChar;
                }
            }
            return start + 1;
        }
    }
    
    /**
     * 比对UTF16-String主串src和Latin1-String子串tgt，返回子串tgt在主串src中最后一次出现的下标
     * 搜索时只比对主串的前srcCount个字符和子串的前tgtCount个字符，且从主串的fromIndex索引处向前搜索
     */
    public static int lastIndexOfLatin1(byte[] src, int srcCount, byte[] tgt, int tgtCount, int fromIndex) {
        assert fromIndex >= 0;
        assert tgtCount > 0;
        assert tgtCount <= tgt.length;
        int min = tgtCount - 1;
        int i = min + fromIndex;
        int strLastIndex = tgtCount - 1;
        
        char strLastChar = (char) (tgt[strLastIndex] & 0xff);
        
        checkIndex(i, src);

startSearchForLastChar:
        while(true) {
            while(i >= min && getChar(src, i) != strLastChar) {
                i--;
            }
            if(i < min) {
                return -1;
            }
            int j = i - 1;
            int start = j - strLastIndex;
            int k = strLastIndex - 1;
            while(j > start) {
                if(getChar(src, j--) != (tgt[k--] & 0xff)) {
                    i--;
                    continue startSearchForLastChar;
                }
            }
            return start + 1;
        }
    }
    
    /*▲ 查找子串下标 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 替换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 使用newChar替换UTF16-String中的oldChar，并返回替换后的String
    public static String replace(byte[] value, char oldChar, char newChar) {
        int len = value.length >> 1;
        int i = -1;
        // 先查找第一个oldchar的位置
        while(++i < len) {
            if(getChar(value, i) == oldChar) {
                break;
            }
        }
        if(i < len) {
            byte buf[] = new byte[value.length];
            for(int j = 0; j < i; j++) {
                putChar(buf, j, getChar(value, j)); // TBD:arraycopy?
            }
            while(i < len) {
                char c = getChar(value, i);
                putChar(buf, i, c == oldChar ? newChar : c);
                i++;
            }
            // 尝试压缩
            if(String.COMPACT_STRINGS && !StringLatin1.canEncode(oldChar) && StringLatin1.canEncode(newChar)) {
                // 将UTF16-String内部的字节转换为LATIN1-String内部的字节后，再返回
                byte[] val = compress(buf, 0, len);
                if(val != null) {
                    return new String(val, LATIN1);
                }
            }
            return new String(buf, UTF16);
        }
        return null;
    }
    
    /*▲ 替换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 修剪 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回删掉首尾【空格字符】的UTF16-String
    public static String trim(byte[] value) {
        int length = value.length >> 1;
        int len = length;
        int st = 0;
        while(st < len && getChar(value, st) <= ' ') {
            st++;
        }
        while(st < len && getChar(value, len - 1) <= ' ') {
            len--;
        }
        return ((st > 0) || (len < length)) ? new String(Arrays.copyOfRange(value, st << 1, len << 1), UTF16) : null;
    }
    
    // 返回删掉首尾【空白符】的UTF16-String
    public static String strip(byte[] value) {
        int length = value.length >> 1;
        // 返回UTF16-String开头的空白符号数量
        int left = indexOfNonWhitespace(value);
        if(left == length) {
            return "";
        }
        // 返回UTF16-String结尾的空白符号数量
        int right = lastIndexOfNonWhitespace(value);
        return ((left > 0) || (right < length)) ? newString(value, left, right - left) : null;
    }
    
    // 返回UTF16-String起始处首个非空白字符的索引
    public static int indexOfNonWhitespace(byte[] value) {
        int length = value.length >> 1;
        int left = 0;
        while(left < length) {
            // 返回UTF16-String中某处符号（双字节/四字节）的Unicode编码
            int codepoint = codePointAt(value, left, length);
            if(codepoint != ' ' && codepoint != '\t' && !Character.isWhitespace(codepoint)) {
                break;
            }
            left += Character.charCount(codepoint);
        }
        return left;
    }
    
    // 返回UTF16-String结尾处首个空白字符的索引
    public static int lastIndexOfNonWhitespace(byte[] value) {
        int length = value.length >> 1;
        int right = length;
        while(0 < right) {
            int codepoint = codePointBefore(value, right);
            if(codepoint != ' ' && codepoint != '\t' && !Character.isWhitespace(codepoint)) {
                break;
            }
            right -= Character.charCount(codepoint);
        }
        return right;
    }
    
    // 返回去掉起始处连续空白符的UTF16-String
    public static String stripLeading(byte[] value) {
        int length = value.length >> 1;
        // 返回UTF16-String开头的空白符号数量
        int left = indexOfNonWhitespace(value);
        if(left == length) {
            return "";
        }
        return (left != 0) ? newString(value, left, length - left) : null;
    }
    
    // 返回去掉结尾处连续空白符的UTF16-String
    public static String stripTrailing(byte[] value) {
        int length = value.length >> 1;
        // 返回UTF16-String结尾的空白符号数量
        int right = lastIndexOfNonWhitespace(value);
        if(right == 0) {
            return "";
        }
        return (right != length) ? newString(value, 0, right) : null;
    }
    
    /*▲ 修剪 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 逆置 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 逆置UTF16-String（无增补符号）
    public static void reverse(byte[] val, int count) {
        checkOffset(count, val);
        
        int n = count - 1;
        boolean hasSurrogates = false;
        
        // 让k和j变成对称的位置，不断交换对称位置的char
        for(int j = (n - 1) >> 1; j >= 0; j--) {
            int k = n - j;
            char cj = getChar(val, j);
            char ck = getChar(val, k);
            putChar(val, j, ck);
            putChar(val, k, cj);
            // 记下有增补符号以便后续处理
            if(Character.isSurrogate(cj) || Character.isSurrogate(ck)) {
                hasSurrogates = true;
            }
        }
        
        if(hasSurrogates) {
            // 处理有增补符号的情形
            reverseAllValidSurrogatePairs(val, count);
        }
    }
    
    /**
     * Outlined helper method for reverse()
     */
    // 处理UTF16-String中那些增补符号（经过上述方法之后，增补字符全变成了逆序，所以只要转过来就好了）
    private static void reverseAllValidSurrogatePairs(byte[] val, int count) {
        for(int i = 0; i < count - 1; i++) {
            char c2 = getChar(val, i);
            if(Character.isLowSurrogate(c2)) {
                char c1 = getChar(val, i + 1);
                if(Character.isHighSurrogate(c1)) {
                    putChar(val, i++, c1);
                    putChar(val, i, c2);
                }
            }
        }
    }
    
    /*▲ 逆置 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 整数 ---> 字节数组 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * This is a variant of {@link Integer#getChars(int, int, byte[])}, but for UTF-16 coder.
     *
     * @param i     value to convert
     * @param index next index, after the least significant digit
     * @param buf   target buffer, UTF16-coded.
     *
     * @return index of the most significant digit or minus sign, if present
     */
    // ▶ 1 将整数i的每一数位拆分为byte存储到buf中（将来用作字符串显示）
    static int getChars(int i, int index, byte[] buf) {
        int q, r;
        int charPos = index;
        
        boolean negative = (i < 0);
        if(!negative) {
            i = -i;
        }
        
        // Get 2 digits/iteration using ints
        while(i <= -100) {
            q = i / 100;        // 抹掉个位数与十位数
            r = (q * 100) - i;  // 暂存抹掉的个位数与十位数
            i = q;              // 处理剩下的数据
            putChar(buf, --charPos, Integer.DigitOnes[r]);  // 存储个位数
            putChar(buf, --charPos, Integer.DigitTens[r]);  // 存储十位数
        }
        
        // 处理剩下的数据（绝对值在100以内）
        q = i / 10;             // 抹掉个位数
        r = (q * 10) - i;       // 暂存抹掉的个位数
        putChar(buf, --charPos, '0' + r);   // 存储个位数（如果）
        
        // 如果q<0说明还剩最后一位数据
        if(q < 0) {
            putChar(buf, --charPos, '0' - q);
        }
        
        // 处理符号位
        if(negative) {
            putChar(buf, --charPos, '-');
        }
        
        return charPos;
    }
    
    // ▶ 1-1 将整数i的每一数位拆分为byte存储到value中（将来用作字符串显示），加入范围检查
    public static int getChars(int i, int begin, int end, byte[] value) {
        checkBoundsBeginEnd(begin, end, value);
        int pos = getChars(i, end, value);
        assert begin == pos;
        return pos;
    }
    
    /**
     * This is a variant of {@link Long#getChars(long, int, byte[])}, but for UTF-16 coder.
     *
     * @param i     value to convert
     * @param index next index, after the least significant digit
     * @param buf   target buffer, UTF16-coded.
     *
     * @return index of the most significant digit or minus sign, if present
     */
    // ▶ 2 将整数i的每一数位拆分为byte存储到buf中（将来用作字符串显示）
    static int getChars(long i, int index, byte[] buf) {
        long q;
        int r;
        int charPos = index;
        
        boolean negative = (i < 0);
        if(!negative) {
            i = -i;
        }
        
        // Get 2 digits/iteration using longs until quotient fits into an int
        while(i <= Integer.MIN_VALUE) {
            q = i / 100;
            r = (int) ((q * 100) - i);
            i = q;
            putChar(buf, --charPos, Integer.DigitOnes[r]);
            putChar(buf, --charPos, Integer.DigitTens[r]);
        }
        
        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int) i;
        while(i2 <= -100) {
            q2 = i2 / 100;
            r = (q2 * 100) - i2;
            i2 = q2;
            putChar(buf, --charPos, Integer.DigitOnes[r]);
            putChar(buf, --charPos, Integer.DigitTens[r]);
        }
        
        // We know there are at most two digits left at this point.
        q2 = i2 / 10;
        r = (q2 * 10) - i2;
        putChar(buf, --charPos, '0' + r);
        
        // Whatever left is the remaining digit.
        if(q2 < 0) {
            putChar(buf, --charPos, '0' - q2);
        }
        
        if(negative) {
            putChar(buf, --charPos, '-');
        }
        return charPos;
    }
    
    // ▶ 2-1 将整数i的每一数位拆分为byte存储到value中（将来用作字符串显示），加入范围检查
    public static int getChars(long l, int begin, int end, byte[] value) {
        checkBoundsBeginEnd(begin, end, value);
        int pos = getChars(l, end, value);
        assert begin == pos;
        return pos;
    }
    
    /*▲ 整数 ---> 字节数组 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 流 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将UTF16-String按行转为流序列，序列中每个元素都代表一行（遇到\n或\r才换行）
    static Stream<String> lines(byte[] value) {
        return StreamSupport.stream(new LinesSpliterator(value), false);
    }
    
    /*▲ 流 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回UTF16-String的字节值value中包含的char的个数
    public static int length(byte[] value) {
        return value.length >> 1;
    }
    
    // 返回长度为2*len的字节数组
    public static byte[] newBytesFor(int len) {
        if(len < 0) {
            throw new NegativeArraySizeException();
        }
        if(len > MAX_LENGTH) {
            throw new OutOfMemoryError("UTF16 String size is " + len + ", should be less than " + MAX_LENGTH);
        }
        return new byte[len << 1];
    }
    
    // 用val在[index, index+len)范围内的byte值创建String
    public static String newString(byte[] val, int index, int len) {
        if(String.COMPACT_STRINGS) {
            // 将UTF16-String内部的字节转换为LATIN1-String内部的字节后，再返回
            byte[] buf = compress(val, index, len);
            // 如果上述转换成功，说明截取的子串中包含的char都在[0, 0xFF]范围内，直接返回压缩后的子串
            if(buf != null) {
                return new String(buf, LATIN1);
            }
        }
        int last = index + len;
        return new String(Arrays.copyOfRange(val, index << 1, last << 1), UTF16);
    }
    
    // 将val[fromIndex, toIndex)的内存单元清零
    public static void fillNull(byte[] val, int index, int end) {
        Arrays.fill(val, index << 1, end << 1, (byte) 0);
    }
    
    /*
     * 判断字节存储是大端还是小端
     *
     * 例如对于整数：0x1234，其存储方式如下：
     * ------------低地址-----高地址------>
     * 内存地址    0x1000    0x1001
     * 小 端 法     0x34      0x12
     * 大 端 法     0x12      0x34
     *
     * 一般操作系统都是小端，而通讯协议是大端的。
     */
    private static native boolean isBigEndian();
    
    
    public static int hashCode(byte[] value) {
        int h = 0;
        int length = value.length >> 1;
        for(int i = 0; i < length; i++) {
            h = 31 * h + getChar(value, i);
        }
        return h;
    }
    
    
    
    //▼ 越界检查 ████████████████████████████████████████████████████████████████████████████████
    
    public static void checkIndex(int off, byte[] val) {
        String.checkIndex(off, length(val));
    }
    
    public static void checkOffset(int off, byte[] val) {
        String.checkOffset(off, length(val));
    }
    
    public static void checkBoundsBeginEnd(int begin, int end, byte[] val) {
        String.checkBoundsBeginEnd(begin, end, length(val));
    }
    
    public static void checkBoundsOffCount(int offset, int count, byte[] val) {
        String.checkBoundsOffCount(offset, count, length(val));
    }
    
    //▲ 越界检查 ████████████████████████████████████████████████████████████████████████████████
    
    
    
    // 按char分割元素的Spliterator，即CharsSpliterator的每个元素都是char
    static class CharsSpliterator implements Spliterator.OfInt {
        private final byte[] array;
        private final int fence;  // one past last index
        private final int cs;
        private int index;        // current index, modified on advance/split
        
        CharsSpliterator(byte[] array, int acs) {
            this(array, 0, array.length >> 1, acs);
        }
        
        CharsSpliterator(byte[] array, int origin, int fence, int acs) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.cs = acs | Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
        
        @Override
        public boolean tryAdvance(IntConsumer action) {
            if(action == null)
                throw new NullPointerException();
            int i = index;
            if(i >= 0 && i < fence) {
                action.accept(charAt(array, i));
                index++;
                return true;
            }
            return false;
        }
        
        @Override
        public void forEachRemaining(IntConsumer action) {
            byte[] a;
            int i, hi; // hoist accesses and checks from loop
            if(action == null)
                throw new NullPointerException();
            if(((a = array).length >> 1) >= (hi = fence) && (i = index) >= 0 && i < (index = hi)) {
                do {
                    action.accept(charAt(a, i));
                } while(++i < hi);
            }
        }
        
        @Override
        public OfInt trySplit() {
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid) ? null : new CharsSpliterator(array, lo, index = mid, cs);
        }
        
        @Override
        public long estimateSize() {
            return (long) (fence - index);
        }
        
        @Override
        public int characteristics() {
            return cs;
        }
    }
    
    // 按Unicode符号分割元素的Spliterator，即CodePointsSpliterator的每个元素都是Unicode符号
    static class CodePointsSpliterator implements Spliterator.OfInt {
        private final byte[] array;
        private final int fence;  // one past last index
        private final int cs;
        private int index;        // current index, modified on advance/split
        
        CodePointsSpliterator(byte[] array, int acs) {
            this(array, 0, array.length >> 1, acs);
        }
        
        CodePointsSpliterator(byte[] array, int origin, int fence, int acs) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.cs = acs | Spliterator.ORDERED;
        }
        
        @Override
        public boolean tryAdvance(IntConsumer action) {
            if(action == null)
                throw new NullPointerException();
            if(index >= 0 && index < fence) {
                index = advance(array, index, fence, action);
                return true;
            }
            return false;
        }
        
        @Override
        public void forEachRemaining(IntConsumer action) {
            byte[] a;
            int i, hi; // hoist accesses and checks from loop
            if(action == null)
                throw new NullPointerException();
            if(((a = array).length >> 1) >= (hi = fence) && (i = index) >= 0 && i < (index = hi)) {
                do {
                    i = advance(a, i, hi, action);
                } while(i < hi);
            }
        }
        
        @Override
        public OfInt trySplit() {
            int lo = index, mid = (lo + fence) >>> 1;
            if(lo >= mid)
                return null;
            
            int midOneLess;
            // If the mid-point intersects a surrogate pair
            if(Character.isLowSurrogate(charAt(array, mid)) && Character.isHighSurrogate(charAt(array, midOneLess = (mid - 1)))) {
                // If there is only one pair it cannot be split
                if(lo >= midOneLess)
                    return null;
                // Shift the mid-point to align with the surrogate pair
                return new CodePointsSpliterator(array, lo, index = midOneLess, cs);
            }
            return new CodePointsSpliterator(array, lo, index = mid, cs);
        }
        
        @Override
        public long estimateSize() {
            return (long) (fence - index);
        }
        
        @Override
        public int characteristics() {
            return cs;
        }
        
        // Advance one code point from the index, i, and return the next index to advance from
        private static int advance(byte[] a, int i, int hi, IntConsumer action) {
            char c1 = charAt(a, i++);
            int cp = c1;
            if(Character.isHighSurrogate(c1) && i < hi) {
                char c2 = charAt(a, i);
                if(Character.isLowSurrogate(c2)) {
                    i++;
                    // 高、低代理区的码点值 ---> Unicode符号编码值
                    cp = Character.toCodePoint(c1, c2);
                }
            }
            action.accept(cp);
            return i;
        }
    }
    
    // 按行分割元素的Spliterator，即LinesSpliterator的每个元素都是行
    private static final class LinesSpliterator implements Spliterator<String> {
        private final int fence;  // one past last index
        private byte[] value;
        private int index;        // current index, modified on advance/split
        
        LinesSpliterator(byte[] value) {
            this(value, 0, value.length >>> 1);
        }
        
        LinesSpliterator(byte[] value, int start, int length) {
            this.value = value;
            this.index = start;
            this.fence = start + length;
        }
        
        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            if(action == null) {
                throw new NullPointerException("tryAdvance action missing");
            }
            if(index != fence) {
                action.accept(next());
                return true;
            }
            return false;
        }
        
        @Override
        public void forEachRemaining(Consumer<? super String> action) {
            if(action == null) {
                throw new NullPointerException("forEachRemaining action missing");
            }
            while(index != fence) {
                action.accept(next());
            }
        }
        
        @Override
        public Spliterator<String> trySplit() {
            int half = (fence + index) >>> 1;
            int mid = skipLineSeparator(indexOfLineSeparator(half));
            if(mid < fence) {
                int start = index;
                index = mid;
                return new LinesSpliterator(value, start, mid - start);
            }
            return null;
        }
        
        @Override
        public long estimateSize() {
            return fence - index + 1;
        }
        
        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;
        }
        
        private int indexOfLineSeparator(int start) {
            for(int current = start; current < fence; current++) {
                char ch = getChar(value, current);
                if(ch == '\n' || ch == '\r') {
                    return current;
                }
            }
            return fence;
        }
        
        private int skipLineSeparator(int start) {
            if(start < fence) {
                if(getChar(value, start) == '\r') {
                    int next = start + 1;
                    if(next < fence && getChar(value, next) == '\n') {
                        return next + 1;
                    }
                }
                return start + 1;
            }
            return fence;
        }
        
        private String next() {
            int start = index;
            int end = indexOfLineSeparator(start);
            index = skipLineSeparator(end);
            return newString(value, start, end - start);
        }
    }
}
