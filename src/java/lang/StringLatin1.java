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
import static java.lang.String.checkOffset;

// LATIN1-String
final class StringLatin1 {
    
    /*▼ 获取char/char[] ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将LATIN1-String内部的字节转换为char后返回
    public static char getChar(byte[] val, int index) {
        return (char) (val[index] & 0xff);
    }
    
    // 将LATIN1-String内部的字节转换为char后返回，加入范围检查
    public static char charAt(byte[] value, int index) {
        if(index < 0 || index >= value.length) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return (char) (value[index] & 0xff);
    }
    
    // 将LATIN1-String内部的字节批量转换为char后存入dst
    public static void getChars(byte[] value, int srcBegin, int srcEnd, char dst[], int dstBegin) {
        inflate(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }
    
    // 将LATIN1-String内部的字节批量转换为char后存入dst
    @HotSpotIntrinsicCandidate
    public static void inflate(byte[] src, int srcOff, char[] dst, int dstOff, int len) {
        for(int i = 0; i < len; i++) {
            dst[dstOff++] = (char) (src[srcOff++] & 0xff);
        }
    }
    
    // 将LATIN1-String内部的字节全部转换为char后返回
    public static char[] toChars(byte[] value) {
        char[] dst = new char[value.length];
        // 将LATIN1-String内部的字节批量转换为char后存入dst
        inflate(value, 0, dst, 0, value.length);
        return dst;
    }
    
    /*▲ 获取char/char[] ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取byte/byte[] ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将c的一个低字节转换为LATIN-String内部的字节，存入val
    public static void putChar(byte[] val, int index, int c) {
        //assert (canEncode(c));
        val[index] = (byte) (c);
    }
    
    // 将char转换为LATIN1-String内部的字节，并返回
    public static byte[] toBytes(char c) {
        return new byte[]{(byte) c};
    }
    
    // 将val[off, off+len)中的一组Unicode值批量转换为LATIN1-String内部的字节，再返回
    public static byte[] toBytes(int[] val, int off, int len) {
        byte[] ret = new byte[len];
        for(int i = 0; i < len; i++) {
            int cp = val[off++];
            // 如果当前序列不在Latin1编码范围内，转码失败
            if(!canEncode(cp)) {
                return null;
            }
            ret[i] = (byte) cp;
        }
        return ret;
    }
    
    // 从LATIN1-String内部的字节转为UTF16-String内部的字节
    @HotSpotIntrinsicCandidate
    public static void inflate(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        StringUTF16.inflate(src, srcOff, dst, dstOff, len);
    }
    
    // 从LATIN1-String内部的字节转为UTF16-String内部的字节后返回
    public static byte[] inflate(byte[] value, int off, int len) {
        // 创建长度为2*len的字节数组
        byte[] ret = StringUTF16.newBytesFor(len);
        // 从LATIN-String内部的字节转为UTF16-String内部的字节
        inflate(value, off, ret, 0, len);
        return ret;
    }
    
    // 将LATIN1-String内部的字节转换为LATIN1-String内部的字节，由于都是单字节，直接用了复制
    public static void getBytes(byte[] value, int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }
    
    /*▲ 获取byte/byte[] ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 大小写转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 小写转换，需要指定语言环境
    public static String toLowerCase(String str, byte[] value, Locale locale) {
        if(locale == null) {
            throw new NullPointerException();
        }
        int first;
        final int len = value.length;
        // Now check if there are any characters that need to be changed, or are surrogate
        for(first = 0; first < len; first++) {
            int cp = value[first] & 0xff;
            if(cp != Character.toLowerCase(cp)) {  // no need to check Character.ERROR
                break;
            }
        }
        if(first == len)
            return str;
        String lang = locale.getLanguage();
        if(lang == "tr" || lang == "az" || lang == "lt") {
            return toLowerCaseEx(str, value, first, locale, true);
        }
        byte[] result = new byte[len];
        System.arraycopy(value, 0, result, 0, first);  // Just copy the first few
        // lowerCase characters.
        for(int i = first; i < len; i++) {
            int cp = value[i] & 0xff;
            cp = Character.toLowerCase(cp);
            if(!canEncode(cp)) {                      // not a latin1 character
                return toLowerCaseEx(str, value, first, locale, false);
            }
            result[i] = (byte) cp;
        }
        return new String(result, LATIN1);
    }
    
    // 大写转换，需要指定语言环境
    public static String toUpperCase(String str, byte[] value, Locale locale) {
        if(locale == null) {
            throw new NullPointerException();
        }
        int first;
        final int len = value.length;
        
        // Now check if there are any characters that need to be changed, or are surrogate
        for(first = 0; first < len; first++) {
            int cp = value[first] & 0xff;
            if(cp != Character.toUpperCaseEx(cp)) {   // no need to check Character.ERROR
                break;
            }
        }
        if(first == len) {
            return str;
        }
        String lang = locale.getLanguage();
        if(lang == "tr" || lang == "az" || lang == "lt") {
            return toUpperCaseEx(str, value, first, locale, true);
        }
        byte[] result = new byte[len];
        System.arraycopy(value, 0, result, 0, first);  // Just copy the first few
        // upperCase characters.
        for(int i = first; i < len; i++) {
            int cp = value[i] & 0xff;
            cp = Character.toUpperCaseEx(cp);
            if(!canEncode(cp)) {                      // not a latin1 character
                return toUpperCaseEx(str, value, first, locale, false);
            }
            result[i] = (byte) cp;
        }
        return new String(result, LATIN1);
    }
    
    // 小写转换，处理增补字符以及一些特殊语言的场景
    private static String toLowerCaseEx(String str, byte[] value, int first, Locale locale, boolean localeDependent) {
        // 创建长度为2*len的字节数组
        byte[] result = StringUTF16.newBytesFor(value.length);
        int resultOffset = 0;
        for(int i = 0; i < first; i++) {
            StringUTF16.putChar(result, resultOffset++, value[i] & 0xff);
        }
        for(int i = first; i < value.length; i++) {
            int srcChar = value[i] & 0xff;
            int lowerChar;
            char[] lowerCharArray;
            if(localeDependent) {
                lowerChar = ConditionalSpecialCasing.toLowerCaseEx(str, i, locale);
            } else {
                lowerChar = Character.toLowerCase(srcChar);
            }
            if(Character.isBmpCodePoint(lowerChar)) {    // Character.ERROR is not a bmp
                StringUTF16.putChar(result, resultOffset++, lowerChar);
            } else {
                if(lowerChar == Character.ERROR) {
                    lowerCharArray = ConditionalSpecialCasing.toLowerCaseCharArray(str, i, locale);
                } else {
                    // 解码，Unicode码点值 ---> char，对于增补平面区码点值，需要拆分成高、低代理单元再存储
                    lowerCharArray = Character.toChars(lowerChar);
                }
                /* Grow result if needed */
                int mapLen = lowerCharArray.length;
                if(mapLen > 1) {
                    // 创建长度为2*len的字节数组
                    byte[] result2 = StringUTF16.newBytesFor((result.length >> 1) + mapLen - 1);
                    System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                for(int x = 0; x < mapLen; ++x) {
                    StringUTF16.putChar(result, resultOffset++, lowerCharArray[x]);
                }
            }
        }
        return StringUTF16.newString(result, 0, resultOffset);
    }
    
    // 大写转换，处理增补字符以及一些特殊语言的场景
    private static String toUpperCaseEx(String str, byte[] value, int first, Locale locale, boolean localeDependent) {
        // 创建长度为2*len的字节数组
        byte[] result = StringUTF16.newBytesFor(value.length);
        int resultOffset = 0;
        for(int i = 0; i < first; i++) {
            StringUTF16.putChar(result, resultOffset++, value[i] & 0xff);
        }
        for(int i = first; i < value.length; i++) {
            int srcChar = value[i] & 0xff;
            int upperChar;
            char[] upperCharArray;
            if(localeDependent) {
                upperChar = ConditionalSpecialCasing.toUpperCaseEx(str, i, locale);
            } else {
                upperChar = Character.toUpperCaseEx(srcChar);
            }
            if(Character.isBmpCodePoint(upperChar)) {
                StringUTF16.putChar(result, resultOffset++, upperChar);
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
                if(mapLen > 1) {
                    byte[] result2 = StringUTF16.newBytesFor((result.length >> 1) + mapLen - 1);
                    System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                for(int x = 0; x < mapLen; ++x) {
                    StringUTF16.putChar(result, resultOffset++, upperCharArray[x]);
                }
            }
        }
        return StringUTF16.newString(result, 0, resultOffset);
    }
    
    /*▲ 大小写转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 码点/码元 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回Latin1-String中某处符号（单字节）的Unicode编码
    public static int codePointAt(byte[] value, int index, int end) {
        return value[index] & 0xff;
    }
    
    // 返回Latin1-String中某处(index-1)符号（单字节）的Unicode编码（从后往前试探）
    public static int codePointBefore(byte[] value, int index) {
        return value[index - 1] & 0xff;
    }
    
    // 统计Latin1-String中指定字节范围内存在多少个Unicode符号
    public static int codePointCount(byte[] value, int beginIndex, int endIndex) {
        return endIndex - beginIndex;
    }
    
    /*▲ 码点/码元 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    //▼ 比较/判等 ████████████████████████████████████████████████████████████████████████████████
    
    // ▶ 1 比较两个Latin1-String的字节值
    public static int compareTo(byte[] value, byte[] other, int len1, int len2) {
        int lim = Math.min(len1, len2);
        for(int k = 0; k < lim; k++) {
            if(value[k] != other[k]) {
                return getChar(value, k) - getChar(other, k);
            }
        }
        return len1 - len2;
    }
    
    // ▶ 1-1 比较两个Latin1-String的字节值
    @HotSpotIntrinsicCandidate
    public static int compareTo(byte[] value, byte[] other) {
        int len1 = value.length;
        int len2 = other.length;
        return compareTo(value, other, len1, len2);
    }
    
    // ▶ 2 比较Latin1-String的字节值(value)和UTF16-String的字节值(other)，需要先将它们同时转为char再比较
    private static int compareToUTF16Values(byte[] value, byte[] other, int len1, int len2) {
        int lim = Math.min(len1, len2);
        for(int k = 0; k < lim; k++) {
            // 解码，一个byte ---> 一个char
            char c1 = getChar(value, k);
            // 将UTF16-String内部的字节转换为char后返回
            char c2 = StringUTF16.getChar(other, k);
            if(c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }
    
    // ▶ 2-1 比较Latin1-String的字节值(value)和UTF16-String的字节值(other)，需要先将它们同时转为char再比较，加入范围检查
    public static int compareToUTF16(byte[] value, byte[] other, int len1, int len2) {
        checkOffset(len1, length(value));
        checkOffset(len2, StringUTF16.length(other));
        
        return compareToUTF16Values(value, other, len1, len2);
    }
    
    // ▶ 2-2 比较Latin1-String的字节值(value)和UTF16-String的字节值(other)，需要先将它们同时转为char再比较
    @HotSpotIntrinsicCandidate
    public static int compareToUTF16(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = StringUTF16.length(other);
        return compareToUTF16Values(value, other, len1, len2);
    }
    
    // 忽略大小写地比较两个Latin1-String的字节值
    public static int compareToCI(byte[] value, byte[] other) {
        int len1 = value.length;
        int len2 = other.length;
        int lim = Math.min(len1, len2);
        for(int k = 0; k < lim; k++) {
            if(value[k] != other[k]) {
                // 都转为大写形式
                char c1 = (char) CharacterDataLatin1.instance.toUpperCase(getChar(value, k));
                char c2 = (char) CharacterDataLatin1.instance.toUpperCase(getChar(other, k));
                if(c1 != c2) {
                    // 都转为小写形式
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
    
    // 忽略大小写地比较Latin1-String的字节值(value)和UTF16-String的字节值(other)，需要先将它们同时转为char再比较
    public static int compareToCI_UTF16(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = StringUTF16.length(other);
        int lim = Math.min(len1, len2);
        for(int k = 0; k < lim; k++) {
            char c1 = getChar(value, k);
            // 将UTF16-String内部的字节转换为char后返回
            char c2 = StringUTF16.getChar(other, k);
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
    
    // 忽略大小写地比较两个Latin1-String的字节值，需要先将它们同时转为char再比较
    public static boolean regionMatchesCI(byte[] value, int toffset, byte[] other, int ooffset, int len) {
        int last = toffset + len;
        while(toffset < last) {
            char c1 = (char) (value[toffset++] & 0xff);
            char c2 = (char) (other[ooffset++] & 0xff);
            if(c1 == c2) {
                continue;
            }
            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if(u1 == u2) {
                continue;
            }
            if(Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                continue;
            }
            return false;
        }
        return true;
    }
    
    // 忽略大小写地比较Latin1-String的字节值(value)和UTF16-String的字节值(other)，需要先将它们同时转为char再比较
    public static boolean regionMatchesCI_UTF16(byte[] value, int toffset, byte[] other, int ooffset, int len) {
        int last = toffset + len;
        while(toffset < last) {
            char c1 = (char) (value[toffset++] & 0xff);
            // 将UTF16-String内部的字节转换为char后返回
            char c2 = StringUTF16.getChar(other, ooffset++);
            if(c1 == c2) {
                continue;
            }
            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if(u1 == u2) {
                continue;
            }
            if(Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                continue;
            }
            return false;
        }
        return true;
    }
    
    // true：两个Latin1-String内容相等
    @HotSpotIntrinsicCandidate
    public static boolean equals(byte[] value, byte[] other) {
        if(value.length == other.length) {
            for(int i = 0; i < value.length; i++) {
                if(value[i] != other[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    //▲ 比较/判等 ████████████████████████████████████████████████████████████████████████████████
    
    
    
    /*▼ 查找Unicode符号下标 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // ▶ 返回符号ch在Latin1-String的字节值value中的下标
    public static int indexOf(byte[] value, int ch, int fromIndex) {
        if(!canEncode(ch)) {
            return -1;
        }
        int max = value.length;
        if(fromIndex < 0) {
            fromIndex = 0;
        } else if(fromIndex >= max) {
            // Note: fromIndex might be near -1>>>1.
            return -1;
        }
        byte c = (byte) ch;
        for(int i = fromIndex; i < max; i++) {
            if(value[i] == c) {
                return i;
            }
        }
        return -1;
    }
    
    // 返回Unicode符号ch在Latin1-String的字节值value中最后一次出现的下标
    public static int lastIndexOf(final byte[] value, int ch, int fromIndex) {
        if(!canEncode(ch)) {
            return -1;
        }
        int off = Math.min(fromIndex, value.length - 1);
        for(; off >= 0; off--) {
            if(value[off] == (byte) ch) {
                return off;
            }
        }
        return -1;
    }
    
    /*▲ 查找Unicode符号下标 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找子串位置 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 比对两个Latin1-String，返回子串str在主串value中第一次出现的位置
    @HotSpotIntrinsicCandidate
    public static int indexOf(byte[] value, byte[] str) {
        if(str.length == 0) {
            return 0;
        }
        
        if(value.length == 0) {
            return -1;
        }
        
        return indexOf(value, value.length, str, str.length, 0);
    }
    
    /*
     * 比对两个Latin1-String，返回子串str在主串value中第一次出现的位置
     * 搜索时只比对主串的前valueCount个字符和子串的前strCount个字符，且从主串的fromIndex索引处向后搜索
     */
    @HotSpotIntrinsicCandidate
    public static int indexOf(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex) {
        byte first = str[0];
        int max = (valueCount - strCount);
        for(int i = fromIndex; i <= max; i++) {
            // Look for first character.
            if(value[i] != first) {
                while(++i <= max && value[i] != first)
                    ;
            }
            // Found first character, now look at the rest of value
            if(i <= max) {
                int j = i + 1;
                int end = j + strCount - 1;
                for(int k = 1; j < end && value[j] == str[k]; j++, k++)
                    ;
                if(j == end) {
                    // Found whole string.
                    return i;
                }
            }
        }
        return -1;
    }
    
    /*
     * 比对两个Latin1-String，返回子串tgt在主串src中最后一次出现的位置
     * 搜索时只比对主串的前srcCount个字符和子串的前tgtCount个字符，且从主串的fromIndex索引处向前搜索
     */
    public static int lastIndexOf(byte[] src, int srcCount, byte[] tgt, int tgtCount, int fromIndex) {
        int min = tgtCount - 1;
        int i = min + fromIndex;
        int strLastIndex = tgtCount - 1;
        char strLastChar = (char) (tgt[strLastIndex] & 0xff);

startSearchForLastChar:
        while(true) {
            while(i >= min && (src[i] & 0xff) != strLastChar) {
                i--;
            }
            if(i < min) {
                return -1;
            }
            int j = i - 1;
            int start = j - strLastIndex;
            int k = strLastIndex - 1;
            while(j > start) {
                if((src[j--] & 0xff) != (tgt[k--] & 0xff)) {
                    i--;
                    continue startSearchForLastChar;
                }
            }
            return start + 1;
        }
    }
    
    /*▲ 查找子串位置 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 替换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 使用newChar替换Latin1-String中的oldChar，并返回替换后的String
    public static String replace(byte[] value, char oldChar, char newChar) {
        if(canEncode(oldChar)) {
            int len = value.length;
            int i = -1;
            while(++i < len) {
                if(value[i] == (byte) oldChar) {
                    break;
                }
            }
            if(i < len) {
                if(canEncode(newChar)) {
                    byte buf[] = new byte[len];
                    for(int j = 0; j < i; j++) {    // TBD arraycopy?
                        buf[j] = value[j];
                    }
                    while(i < len) {
                        byte c = value[i];
                        buf[i] = (c == (byte) oldChar) ? (byte) newChar : c;
                        i++;
                    }
                    return new String(buf, LATIN1);
                } else {
                    // 创建长度为2*len的字节数组
                    byte[] buf = StringUTF16.newBytesFor(len);
                    // inflate from latin1 to UTF16
                    inflate(value, 0, buf, 0, i);
                    while(i < len) {
                        char c = (char) (value[i] & 0xff);
                        StringUTF16.putChar(buf, i, (c == oldChar) ? newChar : c);
                        i++;
                    }
                    return new String(buf, UTF16);
                }
            }
        }
        return null; // for string to return this;
    }
    
    /*▲ 替换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 修剪 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回删掉首尾【空格字符】的Latin1-String
    public static String trim(byte[] value) {
        int len = value.length;
        int st = 0;
        while((st < len) && ((value[st] & 0xff) <= ' ')) {
            st++;
        }
        while((st < len) && ((value[len - 1] & 0xff) <= ' ')) {
            len--;
        }
        return ((st > 0) || (len < value.length)) ? newString(value, st, len - st) : null;
    }
    
    // 返回删掉首尾【空白符】的Latin1-String
    public static String strip(byte[] value) {
        // 返回Latin1-String起始处首个非空白字符的索引
        int left = indexOfNonWhitespace(value);
        if(left == value.length) {
            return "";
        }
        // 返回Latin1-String结尾处首个空白字符的索引
        int right = lastIndexOfNonWhitespace(value);
        return ((left > 0) || (right < value.length)) ? newString(value, left, right - left) : null;
    }
    
    // 返回Latin1-String起始处首个非空白字符的索引
    public static int indexOfNonWhitespace(byte[] value) {
        int length = value.length;
        int left = 0;
        while(left < length) {
            char ch = (char) (value[left] & 0xff);
            if(ch != ' ' && ch != '\t' && !Character.isWhitespace(ch)) {
                break;
            }
            left++;
        }
        return left;
    }
    
    // 返回Latin1-String结尾处首个空白字符的索引
    public static int lastIndexOfNonWhitespace(byte[] value) {
        int length = value.length;
        int right = length;
        while(0 < right) {
            char ch = (char) (value[right - 1] & 0xff);
            if(ch != ' ' && ch != '\t' && !Character.isWhitespace(ch)) {
                break;
            }
            right--;
        }
        return right;
    }
    
    // 返回去掉起始处连续空白符的Latin1-String
    public static String stripLeading(byte[] value) {
        // 返回Latin1-String起始处首个非空白字符的索引
        int left = indexOfNonWhitespace(value);
        if(left == value.length) {
            return "";
        }
        return (left != 0) ? newString(value, left, value.length - left) : null;
    }
    
    // 返回去掉结尾处连续空白符的Latin1-String
    public static String stripTrailing(byte[] value) {
        // 返回Latin1-String结尾处首个空白字符的索引
        int right = lastIndexOfNonWhitespace(value);
        if(right == 0) {
            return "";
        }
        return (right != value.length) ? newString(value, 0, right) : null;
    }
    
    /*▲ 修剪 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 流 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将Latin1-String按行转为流序列，序列中每个元素都代表一行（遇到\n或\r才换行）
    static Stream<String> lines(byte[] value) {
        return StreamSupport.stream(new LinesSpliterator(value), false);
    }
    
    /*▲ 流 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 返回Latin1-String的字节值value中包含的char的个数
    public static int length(byte[] value) {
        return value.length;
    }
    
    // true：表示当前序列在Latin1编码范围内
    public static boolean canEncode(int cp) {
        return cp >>> 8 == 0;
    }
    
    // 用val在[index, index+len)范围内的byte值创建String
    public static String newString(byte[] val, int index, int len) {
        return new String(Arrays.copyOfRange(val, index, index + len), LATIN1);
    }
    
    // 将val[fromIndex, toIndex)的内存单元清零
    public static void fillNull(byte[] val, int index, int end) {
        Arrays.fill(val, index, end, (byte) 0);
    }
    
    
    public static int hashCode(byte[] value) {
        int h = 0;
        for(byte v : value) {
            h = 31 * h + (v & 0xff);
        }
        return h;
    }
    
    
    
    // 按char分割元素的Spliterator，即CharsSpliterator的每个元素都是char
    static class CharsSpliterator implements Spliterator.OfInt {
        private final byte[] array;
        private int index;          // current index, modified on advance/split
        private final int fence;    // one past last index
        private final int cs;
        
        CharsSpliterator(byte[] array, int acs) {
            this(array, 0, array.length, acs);
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
            if(index >= 0 && index < fence) {
                action.accept(array[index++] & 0xff);
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
            if((a = array).length >= (hi = fence) && (i = index) >= 0 && i < (index = hi)) {
                do {
                    action.accept(a[i] & 0xff);
                } while(++i < hi);
            }
        }
        
        @Override
        public OfInt trySplit() {
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid)
                ? null
                : new CharsSpliterator(array, lo, index = mid, cs);
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
    
    // 按行分割元素的Spliterator，即LinesSpliterator的每个元素都是行
    private final static class LinesSpliterator implements Spliterator<String> {
        private byte[] value;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index
        
        LinesSpliterator(byte[] value) {
            this(value, 0, value.length);
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
                byte ch = value[current];
                if(ch == '\n' || ch == '\r') {
                    return current;
                }
            }
            return fence;
        }
        
        private int skipLineSeparator(int start) {
            if(start < fence) {
                if(value[start] == '\r') {
                    int next = start + 1;
                    if(next < fence && value[next] == '\n') {
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
