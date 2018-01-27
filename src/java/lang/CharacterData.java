/*
 * Copyright (c) 2006, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * 字符属性包装器，不同编码范围的字符有不同的实现。
 *
 * 封装了 java.lang.Character 中的一些属性，还提供了一些常用的字符操作
 * 将每个字符的属性打包为一组二进制数据，以便在使用字符时做出准确、快速的判断
 */
abstract class CharacterData {
    /*
     * Character <= 0xff (basic latin) is handled by internal fast-path to avoid initializing large tables.
     * Note: performance of this "fast-path" code may be sub-optimal in negative cases for some accessors due to complicated ranges.
     * Should revisit after optimization of table initialization.
     *
     * 字符属性包装器工厂，生成不同管理范围内字符的字符属性包装器
     *
     * 当字符范围在[0x0, 0xff)之间时，采用fast-path优化，即不需要建立复杂的大表（数组）
     */
    static final CharacterData of(int ch) {
        if(ch >>> 8 == 0) {     // fast-path
            return CharacterDataLatin1.instance;
        } else {
            switch(ch >>> 16) {  //plane 00-16
                case (0):
                    return CharacterData00.instance;
                case (1):
                    return CharacterData01.instance;
                case (2):
                    return CharacterData02.instance;
                case (14):
                    return CharacterData0E.instance;
                case (15):   // Private Use
                case (16):   // Private Use
                    return CharacterDataPrivateUse.instance;
                default:
                    return CharacterDataUndefined.instance;
            }
        }
    }
    
    
    
    /*▼ 判断字符属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // true：该字符是Java空白符
    abstract boolean isWhitespace(int ch);
    
    // true：该字符是镜像字符，如<>[]()
    abstract boolean isMirrored(int ch);
    
    // true：该字符可位于Java标识符起始位置
    abstract boolean isJavaIdentifierStart(int ch);
    
    // true：该字符可位于Java标识符的非起始部分
    abstract boolean isJavaIdentifierPart(int ch);
    
    // true：该字符可位于Unicode标识符的起始部分
    abstract boolean isUnicodeIdentifierStart(int ch);
    
    // true：该字符可位于Unicode标识符的非起始部分
    abstract boolean isUnicodeIdentifierPart(int ch);
    
    // true：该字符在标识符内是可忽略的
    abstract boolean isIdentifierIgnorable(int ch);
    
    // true：该字符是扩展的小写字符
    boolean isOtherLowercase(int ch) {
        return false;
    }
    
    // true：该字符是扩展的大写字符
    boolean isOtherUppercase(int ch) {
        return false;
    }
    
    // true：该字符是扩展的字母[Alphabetic]字符
    boolean isOtherAlphabetic(int ch) {
        return false;
    }
    
    // true：该字符是表意字符
    boolean isIdeographic(int ch) {
        return false;
    }
    
    /*▲ 判断字符属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 转为小写形式
    abstract int toLowerCase(int ch);
    
    // 转为大写形式
    abstract int toUpperCase(int ch);
    
    // 转为TitleCase形式
    abstract int toTitleCase(int ch);
    
    // 转为大写形式（考虑出现的扩展字符）
    // need to implement for JSR204
    int toUpperCaseEx(int ch) {
        return toUpperCase(ch);
    }
    
    // 将字符ch存入字符数组
    char[] toUpperCaseCharArray(int ch) {
        return null;
    }
    
    /*▲ 转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 获取字符属性编码
    abstract int getProperties(int ch);
    
    // 获取字符类型
    abstract int getType(int ch);
    
    // 返回字符ch表示的进制数值，如字母a或A将返回10（可用于16进制中）
    abstract int digit(int ch, int radix);
    
    // 返回字符ch在进制运算中表示的数值，如输入'3'返回3，输入'A'或'a'返回10
    abstract int getNumericValue(int ch);
    
    // 获取该字符的方向属性
    abstract byte getDirectionality(int ch);
}
