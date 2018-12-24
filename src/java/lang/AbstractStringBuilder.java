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

import jdk.internal.math.FloatingDecimal;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * A mutable sequence of characters.
 * <p>
 * Implements a modifiable string. At any point in time it contains some
 * particular sequence of characters, but the length and content of the
 * sequence can be changed through certain method calls.
 *
 * <p>Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * @author Michael McCloskey
 * @author Martin Buchholz
 * @author Ulf Zibis
 * @since 1.5
 */
// 字符序列的抽象实现，是StringBuilder和StringBuffer的父类
abstract class AbstractStringBuilder implements Appendable, CharSequence {
    
    /**
     * The maximum size of array to allocate (unless necessary).
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    
    /**
     * The value is used for character storage.
     */
    // 以字节形式存储字符序列
    byte[] value;
    
    /**
     * The id of the encoding used to encode the bytes in {@code value}.
     */
    // 当前字符序列的编码：LATIN1或UTF16，由此可将ASB分为LATIN1-ASB或UTF16-ASB两类
    byte coder;
    
    /**
     * The count is the number of characters used.
     */
    // 当前ASB内包含的char的数量
    int count;
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * This no-arg constructor is necessary for serialization of subclasses.
     */
    // 构造空的ASB
    AbstractStringBuilder() {
    }
    
    /**
     * Creates an AbstractStringBuilder of the specified capacity.
     */
    // 构造指定容量的ASB，内容为空
    AbstractStringBuilder(int capacity) {
        if(String.COMPACT_STRINGS) {
            value = new byte[capacity];
            coder = String.LATIN1;
        } else {
            value = StringUTF16.newBytesFor(capacity);
            coder = String.UTF16;
        }
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 添加 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Documentation in subclasses because of synchro difference
     */
    // 向ASB末尾添加一个字符序列
    @Override
    public AbstractStringBuilder append(CharSequence s) {
        if(s == null) {
            return appendNull();
        }
        if(s instanceof String) {
            return this.append((String) s);
        }
        if(s instanceof AbstractStringBuilder) {
            return this.append((AbstractStringBuilder) s);
        }
        return this.append(s, 0, s.length());
    }
    
    /**
     * Appends a subsequence of the specified {@code CharSequence} to this
     * sequence.
     * <p>
     * Characters of the argument {@code s}, starting at
     * index {@code start}, are appended, in order, to the contents of
     * this sequence up to the (exclusive) index {@code end}. The length
     * of this sequence is increased by the value of {@code end - start}.
     * <p>
     * Let <i>n</i> be the length of this character sequence just prior to
     * execution of the {@code append} method. Then the character at
     * index <i>k</i> in this character sequence becomes equal to the
     * character at index <i>k</i> in this sequence, if <i>k</i> is less than
     * <i>n</i>; otherwise, it is equal to the character at index
     * <i>k+start-n</i> in the argument {@code s}.
     * <p>
     * If {@code s} is {@code null}, then this method appends
     * characters as if the s parameter was a sequence containing the four
     * characters {@code "null"}.
     *
     * @param s     the sequence to append.
     * @param start the starting index of the subsequence to be appended.
     * @param end   the end index of the subsequence to be appended.
     *
     * @return a reference to this object.
     *
     * @throws IndexOutOfBoundsException if
     *                                   {@code start} is negative, or
     *                                   {@code start} is greater than {@code end} or
     *                                   {@code end} is greater than {@code s.length()}
     */
    // 向ASB末尾添加一个子序列，该子序列取自字符序列s的[start, end)范围
    @Override
    public AbstractStringBuilder append(CharSequence s, int start, int end) {
        if(s == null) {
            s = "null";
        }
        checkRange(start, end, s.length());
        int len = end - start;
        ensureCapacityInternal(count + len);
        appendChars(s, start, end);
        return this;
    }
    
    /**
     * Appends the specified string to this character sequence.
     * <p>
     * The characters of the {@code String} argument are appended, in
     * order, increasing the length of this sequence by the length of the
     * argument. If {@code str} is {@code null}, then the four
     * characters {@code "null"} are appended.
     * <p>
     * Let <i>n</i> be the length of this character sequence just prior to
     * execution of the {@code append} method. Then the character at
     * index <i>k</i> in the new character sequence is equal to the character
     * at index <i>k</i> in the old character sequence, if <i>k</i> is less
     * than <i>n</i>; otherwise, it is equal to the character at index
     * <i>k-n</i> in the argument {@code str}.
     *
     * @param str a string.
     *
     * @return a reference to this object.
     */
    // 向ASB末尾添加一个字符串str
    public AbstractStringBuilder append(String str) {
        if(str == null) {
            return appendNull();
        }
        int len = str.length();
        ensureCapacityInternal(count + len);
        // 向ASB的index索引处插入一个字符串str
        putStringAt(count, str);
        count += len;
        return this;
    }
    
    /**
     * Documentation in subclasses because of synchro difference
     */
    // 向ASB末尾添加一个StringBuffer
    public AbstractStringBuilder append(StringBuffer sb) {
        return this.append((AbstractStringBuilder) sb);
    }
    
    /**
     * @since 1.8
     */
    // 向ASB末尾添加一个ASB序列
    AbstractStringBuilder append(AbstractStringBuilder asb) {
        if(asb == null) {
            return appendNull();
        }
        int len = asb.length();
        ensureCapacityInternal(count + len);
        if(getCoder() != asb.getCoder()) {
            inflate();
        }
        // 将abs内部字节存入当前的ASB
        asb.getBytes(value, count, coder);
        count += len;
        return this;
    }
    
    /**
     * Appends the string representation of the {@code char} array
     * argument to this sequence.
     * <p>
     * The characters of the array argument are appended, in order, to
     * the contents of this sequence. The length of this sequence
     * increases by the length of the argument.
     * <p>
     * The overall effect is exactly as if the argument were converted
     * to a string by the method {@link String#valueOf(char[])},
     * and the characters of that string were then
     * {@link #append(String) appended} to this character sequence.
     *
     * @param str the characters to be appended.
     *
     * @return a reference to this object.
     */
    // 向ASB末尾添加一个字符序列
    public AbstractStringBuilder append(char[] str) {
        int len = str.length;
        ensureCapacityInternal(count + len);
        appendChars(str, 0, len);
        return this;
    }
    
    /**
     * Appends the string representation of a subarray of the
     * {@code char} array argument to this sequence.
     * <p>
     * Characters of the {@code char} array {@code str}, starting at
     * index {@code offset}, are appended, in order, to the contents
     * of this sequence. The length of this sequence increases
     * by the value of {@code len}.
     * <p>
     * The overall effect is exactly as if the arguments were converted
     * to a string by the method {@link String#valueOf(char[], int, int)},
     * and the characters of that string were then
     * {@link #append(String) appended} to this character sequence.
     *
     * @param str    the characters to be appended.
     * @param offset the index of the first {@code char} to append.
     * @param len    the number of {@code char}s to append.
     *
     * @return a reference to this object.
     *
     * @throws IndexOutOfBoundsException if {@code offset < 0} or {@code len < 0}
     *                                   or {@code offset+len > str.length}
     */
    // 向ASB末尾添加一个子序列，该子序列取自字符数组s的[offset, offset+len)范围
    public AbstractStringBuilder append(char str[], int offset, int len) {
        int end = offset + len;
        checkRange(offset, end, str.length);
        ensureCapacityInternal(count + len);
        appendChars(str, offset, end);
        return this;
    }
    
    /**
     * Appends the string representation of the {@code Object} argument.
     * <p>
     * The overall effect is exactly as if the argument were converted
     * to a string by the method {@link String#valueOf(Object)},
     * and the characters of that string were then
     * {@link #append(String) appended} to this character sequence.
     *
     * @param obj an {@code Object}.
     *
     * @return a reference to this object.
     */
    // 向ASB末尾添加一个Object值的字符串序列
    public AbstractStringBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }
    
    /**
     * Appends the string representation of the {@code boolean}
     * argument to the sequence.
     * <p>
     * The overall effect is exactly as if the argument were converted
     * to a string by the method {@link String#valueOf(boolean)},
     * and the characters of that string were then
     * {@link #append(String) appended} to this character sequence.
     *
     * @param b a {@code boolean}.
     *
     * @return a reference to this object.
     */
    // 向ASB末尾添加一个boolean值的字符串序列
    public AbstractStringBuilder append(boolean b) {
        ensureCapacityInternal(count + (b ? 4 : 5));
        int count = this.count;
        byte[] val = this.value;
        if(isLatin1()) {
            if(b) {
                val[count++] = 't';
                val[count++] = 'r';
                val[count++] = 'u';
                val[count++] = 'e';
            } else {
                val[count++] = 'f';
                val[count++] = 'a';
                val[count++] = 'l';
                val[count++] = 's';
                val[count++] = 'e';
            }
        } else {
            if(b) {
                count = StringUTF16.putCharsAt(val, count, 't', 'r', 'u', 'e');
            } else {
                count = StringUTF16.putCharsAt(val, count, 'f', 'a', 'l', 's', 'e');
            }
        }
        this.count = count;
        return this;
    }
    
    /**
     * Appends the string representation of the {@code char}
     * argument to this sequence.
     * <p>
     * The argument is appended to the contents of this sequence.
     * The length of this sequence increases by {@code 1}.
     * <p>
     * The overall effect is exactly as if the argument were converted
     * to a string by the method {@link String#valueOf(char)},
     * and the character in that string were then
     * {@link #append(String) appended} to this character sequence.
     *
     * @param c a {@code char}.
     *
     * @return a reference to this object.
     */
    // 向ASB末尾添加一个char值的字符串序列
    @Override
    public AbstractStringBuilder append(char c) {
        ensureCapacityInternal(count + 1);
        if(isLatin1() && StringLatin1.canEncode(c)) {
            value[count++] = (byte) c;
        } else {
            if(isLatin1()) {
                inflate();
            }
            StringUTF16.putCharSB(value, count++, c);
        }
        return this;
    }
    
    /**
     * Appends the string representation of the {@code int}
     * argument to this sequence.
     * <p>
     * The overall effect is exactly as if the argument were converted
     * to a string by the method {@link String#valueOf(int)},
     * and the characters of that string were then
     * {@link #append(String) appended} to this character sequence.
     *
     * @param i an {@code int}.
     *
     * @return a reference to this object.
     */
    // 向ASB末尾添加一个int值的字符串序列
    public AbstractStringBuilder append(int i) {
        int count = this.count;
        int spaceNeeded = count + Integer.stringSize(i);
        ensureCapacityInternal(spaceNeeded);
        if(isLatin1()) {
            Integer.getChars(i, spaceNeeded, value);
        } else {
            StringUTF16.getChars(i, count, spaceNeeded, value);
        }
        this.count = spaceNeeded;
        return this;
    }
    
    /**
     * Appends the string representation of the {@code long}
     * argument to this sequence.
     * <p>
     * The overall effect is exactly as if the argument were converted
     * to a string by the method {@link String#valueOf(long)},
     * and the characters of that string were then
     * {@link #append(String) appended} to this character sequence.
     *
     * @param l a {@code long}.
     *
     * @return a reference to this object.
     */
    // 向ASB末尾添加一个long值的字符串序列
    public AbstractStringBuilder append(long l) {
        int count = this.count;
        int spaceNeeded = count + Long.stringSize(l);
        ensureCapacityInternal(spaceNeeded);
        if(isLatin1()) {
            Long.getChars(l, spaceNeeded, value);
        } else {
            StringUTF16.getChars(l, count, spaceNeeded, value);
        }
        this.count = spaceNeeded;
        return this;
    }
    
    /**
     * Appends the string representation of the {@code float}
     * argument to this sequence.
     * <p>
     * The overall effect is exactly as if the argument were converted
     * to a string by the method {@link String#valueOf(float)},
     * and the characters of that string were then
     * {@link #append(String) appended} to this character sequence.
     *
     * @param f a {@code float}.
     *
     * @return a reference to this object.
     */
    // 向ASB末尾添加一个float值的字符串序列
    public AbstractStringBuilder append(float f) {
        FloatingDecimal.appendTo(f, this);
        return this;
    }
    
    /**
     * Appends the string representation of the {@code double}
     * argument to this sequence.
     * <p>
     * The overall effect is exactly as if the argument were converted
     * to a string by the method {@link String#valueOf(double)},
     * and the characters of that string were then
     * {@link #append(String) appended} to this character sequence.
     *
     * @param d a {@code double}.
     *
     * @return a reference to this object.
     */
    // 向ASB末尾添加一个double值的字符串序列
    public AbstractStringBuilder append(double d) {
        FloatingDecimal.appendTo(d, this);
        return this;
    }
    
    /**
     * Appends the string representation of the {@code codePoint}
     * argument to this sequence.
     *
     * <p> The argument is appended to the contents of this sequence.
     * The length of this sequence increases by
     * {@link Character#charCount(int) Character.charCount(codePoint)}.
     *
     * <p> The overall effect is exactly as if the argument were
     * converted to a {@code char} array by the method
     * {@link Character#toChars(int)} and the character in that array
     * were then {@link #append(char[]) appended} to this character
     * sequence.
     *
     * @param codePoint a Unicode code point
     *
     * @return a reference to this object.
     *
     * @throws IllegalArgumentException if the specified
     *                                  {@code codePoint} isn't a valid Unicode code point
     */
    // 向ASB末尾添加一个由Unicode码点值表示的char的字符串序列
    public AbstractStringBuilder appendCodePoint(int codePoint) {
        if(Character.isBmpCodePoint(codePoint)) {
            return append((char) codePoint);
        }
        return append(Character.toChars(codePoint));
    }
    
    // 向ASB末尾添加一个子序列，该子序列取自字符数组s的[off, end)范围
    private final void appendChars(char[] s, int off, int end) {
        int count = this.count;
        if(isLatin1()) {
            byte[] val = this.value;
            for(int i = off, j = count; i<end; i++) {
                char c = s[i];
                if(StringLatin1.canEncode(c)) {
                    val[j++] = (byte) c;
                } else {
                    this.count = count = j;
                    inflate();
                    StringUTF16.putCharsSB(this.value, j, s, i, end);
                    this.count = count + end - i;
                    return;
                }
            }
        } else {
            StringUTF16.putCharsSB(this.value, count, s, off, end);
        }
        this.count = count + end - off;
    }
    
    // 向ASB末尾添加一个子序列，该子序列取自字符序列s的[start, end)范围
    private final void appendChars(CharSequence s, int off, int end) {
        if(isLatin1()) {
            byte[] val = this.value;
            for(int i = off, j = count; i<end; i++) {
                char c = s.charAt(i);
                if(StringLatin1.canEncode(c)) {
                    val[j++] = (byte) c;
                } else {
                    count = j;
                    inflate();
                    StringUTF16.putCharsSB(this.value, j, s, i, end);
                    count += end - i;
                    return;
                }
            }
        } else {
            // 将LATIN1-CS内部的字节批量转换为UTF16-SB内部的字节后，存入value
            StringUTF16.putCharsSB(this.value, count, s, off, end);
        }
        count += end - off;
    }
    
    // 添加一个字符串："null"
    private AbstractStringBuilder appendNull() {
        ensureCapacityInternal(count + 4);
        int count = this.count;
        byte[] val = this.value;
        if(isLatin1()) {
            val[count++] = 'n';
            val[count++] = 'u';
            val[count++] = 'l';
            val[count++] = 'l';
        } else {
            // 将4个char依次存入UTF16-SB内部的字节
            count = StringUTF16.putCharsAt(val, count, 'n', 'u', 'l', 'l');
        }
        this.count = count;
        return this;
    }
    
    /*▲ 添加 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 删除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Removes the characters in a substring of this sequence.
     * The substring begins at the specified {@code start} and extends to
     * the character at index {@code end - 1} or to the end of the
     * sequence if no such character exists. If
     * {@code start} is equal to {@code end}, no changes are made.
     *
     * @param start The beginning index, inclusive.
     * @param end   The ending index, exclusive.
     *
     * @return This object.
     *
     * @throws StringIndexOutOfBoundsException if {@code start}
     *                                         is negative, greater than {@code length()}, or
     *                                         greater than {@code end}.
     */
    // 删除[start, end)范围内的char
    public AbstractStringBuilder delete(int start, int end) {
        int count = this.count;
        if(end>count) {
            end = count;
        }
        checkRangeSIOOBE(start, end, count);
        int len = end - start;  // 计算删除元素的数量
        if(len>0) {
            // 从end处的char开始，将后续所有的char平移-len个单位
            shift(end, -len);
            this.count = count - len;
        }
        return this;
    }
    
    /**
     * Removes the {@code char} at the specified position in this
     * sequence. This sequence is shortened by one {@code char}.
     *
     * <p>Note: If the character at the given index is a supplementary
     * character, this method does not remove the entire character. If
     * correct handling of supplementary characters is required,
     * determine the number of {@code char}s to remove by calling
     * {@code Character.charCount(thisSequence.codePointAt(index))},
     * where {@code thisSequence} is this sequence.
     *
     * @param index Index of {@code char} to remove
     *
     * @return This object.
     *
     * @throws StringIndexOutOfBoundsException if the {@code index}
     *                                         is negative or greater than or equal to
     *                                         {@code length()}.
     */
    // 删除索引为index的char
    public AbstractStringBuilder deleteCharAt(int index) {
        String.checkIndex(index, count);
        // 从index+1处的char开始，将后续所有的char平移-1个单位，即删除一个cahr
        shift(index + 1, -1);
        count--;
        return this;
    }
    
    /*▲ 删除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 插入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Inserts the specified {@code CharSequence} into this sequence.
     * <p>
     * The characters of the {@code CharSequence} argument are inserted,
     * in order, into this sequence at the indicated offset, moving up
     * any characters originally above that position and increasing the length
     * of this sequence by the length of the argument s.
     * <p>
     * The result of this method is exactly the same as if it were an
     * invocation of this object's
     * {@link #insert(int, CharSequence, int, int) insert}(dstOffset, s, 0, s.length())
     * method.
     *
     * <p>If {@code s} is {@code null}, then the four characters
     * {@code "null"} are inserted into this sequence.
     *
     * @param dstOffset the offset.
     * @param s         the sequence to be inserted
     *
     * @return a reference to this object.
     *
     * @throws IndexOutOfBoundsException if the offset is invalid.
     */
    // 向ASB的dstOffset索引处插入一个子序列s
    public AbstractStringBuilder insert(int dstOffset, CharSequence s) {
        if(s == null) {
            s = "null";
        }
        if(s instanceof String) {
            return this.insert(dstOffset, (String) s);
        }
        return this.insert(dstOffset, s, 0, s.length());
    }
    
    /**
     * Inserts a subsequence of the specified {@code CharSequence} into
     * this sequence.
     * <p>
     * The subsequence of the argument {@code s} specified by
     * {@code start} and {@code end} are inserted,
     * in order, into this sequence at the specified destination offset, moving
     * up any characters originally above that position. The length of this
     * sequence is increased by {@code end - start}.
     * <p>
     * The character at index <i>k</i> in this sequence becomes equal to:
     * <ul>
     * <li>the character at index <i>k</i> in this sequence, if
     * <i>k</i> is less than {@code dstOffset}
     * <li>the character at index <i>k</i>{@code +start-dstOffset} in
     * the argument {@code s}, if <i>k</i> is greater than or equal to
     * {@code dstOffset} but is less than {@code dstOffset+end-start}
     * <li>the character at index <i>k</i>{@code -(end-start)} in this
     * sequence, if <i>k</i> is greater than or equal to
     * {@code dstOffset+end-start}
     * </ul><p>
     * The {@code dstOffset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     * <p>The start argument must be nonnegative, and not greater than
     * {@code end}.
     * <p>The end argument must be greater than or equal to
     * {@code start}, and less than or equal to the length of s.
     *
     * <p>If {@code s} is {@code null}, then this method inserts
     * characters as if the s parameter was a sequence containing the four
     * characters {@code "null"}.
     *
     * @param dstOffset the offset in this sequence.
     * @param s         the sequence to be inserted.
     * @param start     the starting index of the subsequence to be inserted.
     * @param end       the end index of the subsequence to be inserted.
     *
     * @return a reference to this object.
     *
     * @throws IndexOutOfBoundsException if {@code dstOffset}
     *                                   is negative or greater than {@code this.length()}, or
     *                                   {@code start} or {@code end} are negative, or
     *                                   {@code start} is greater than {@code end} or
     *                                   {@code end} is greater than {@code s.length()}
     */
    // 向ASB的dstOffset索引处插入一个子序列，该子序列取自字符序列s的[start, end)范围
    public AbstractStringBuilder insert(int dstOffset, CharSequence s, int start, int end) {
        if(s == null) {
            s = "null";
        }
        String.checkOffset(dstOffset, count);
        checkRange(start, end, s.length());
        int len = end - start;
        ensureCapacityInternal(count + len);
        shift(dstOffset, len);
        count += len;
        putCharsAt(dstOffset, s, start, end);
        return this;
    }
    
    /**
     * Inserts the string representation of the {@code char} array
     * argument into this sequence.
     * <p>
     * The characters of the array argument are inserted into the
     * contents of this sequence at the position indicated by
     * {@code offset}. The length of this sequence increases by
     * the length of the argument.
     * <p>
     * The overall effect is exactly as if the second argument were
     * converted to a string by the method {@link String#valueOf(char[])},
     * and the characters of that string were then
     * {@link #insert(int, String) inserted} into this character
     * sequence at the indicated offset.
     * <p>
     * The {@code offset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     *
     * @param offset the offset.
     * @param str    a character array.
     *
     * @return a reference to this object.
     *
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     */
    // 向ASB的offset索引处插入一个子符序列str
    public AbstractStringBuilder insert(int offset, char[] str) {
        String.checkOffset(offset, count);
        int len = str.length;
        ensureCapacityInternal(count + len);
        shift(offset, len);
        count += len;
        putCharsAt(offset, str, 0, len);
        return this;
    }
    
    /**
     * Inserts the string representation of a subarray of the {@code str}
     * array argument into this sequence. The subarray begins at the
     * specified {@code offset} and extends {@code len} {@code char}s.
     * The characters of the subarray are inserted into this sequence at
     * the position indicated by {@code index}. The length of this
     * sequence increases by {@code len} {@code char}s.
     *
     * @param index  position at which to insert subarray.
     * @param str    A {@code char} array.
     * @param offset the index of the first {@code char} in subarray to
     *               be inserted.
     * @param len    the number of {@code char}s in the subarray to
     *               be inserted.
     *
     * @return This object
     *
     * @throws StringIndexOutOfBoundsException if {@code index}
     *                                         is negative or greater than {@code length()}, or
     *                                         {@code offset} or {@code len} are negative, or
     *                                         {@code (offset+len)} is greater than
     *                                         {@code str.length}.
     */
    // 向ASB的index索引处插入一个子序列，该子序列取自字符序列str的[offset, offset+len)范围
    public AbstractStringBuilder insert(int index, char[] str, int offset, int len) {
        String.checkOffset(index, count);
        checkRangeSIOOBE(offset, offset + len, str.length);
        ensureCapacityInternal(count + len);
        shift(index, len);
        count += len;
        putCharsAt(index, str, offset, offset + len);
        return this;
    }
    
    /**
     * Inserts the string into this character sequence.
     * <p>
     * The characters of the {@code String} argument are inserted, in
     * order, into this sequence at the indicated offset, moving up any
     * characters originally above that position and increasing the length
     * of this sequence by the length of the argument. If
     * {@code str} is {@code null}, then the four characters
     * {@code "null"} are inserted into this sequence.
     * <p>
     * The character at index <i>k</i> in the new character sequence is
     * equal to:
     * <ul>
     * <li>the character at index <i>k</i> in the old character sequence, if
     * <i>k</i> is less than {@code offset}
     * <li>the character at index <i>k</i>{@code -offset} in the
     * argument {@code str}, if <i>k</i> is not less than
     * {@code offset} but is less than {@code offset+str.length()}
     * <li>the character at index <i>k</i>{@code -str.length()} in the
     * old character sequence, if <i>k</i> is not less than
     * {@code offset+str.length()}
     * </ul><p>
     * The {@code offset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     *
     * @param offset the offset.
     * @param str    a string.
     *
     * @return a reference to this object.
     *
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     */
    // 向ASB的offset索引处插入一个字符串str
    public AbstractStringBuilder insert(int offset, String str) {
        String.checkOffset(offset, count);
        if(str == null) {
            str = "null";
        }
        int len = str.length();
        ensureCapacityInternal(count + len);
        shift(offset, len);
        count += len;
        // 向ASB的offset索引处插入一个字符串str
        putStringAt(offset, str);
        return this;
    }
    
    /**
     * Inserts the string representation of the {@code Object}
     * argument into this character sequence.
     * <p>
     * The overall effect is exactly as if the second argument were
     * converted to a string by the method {@link String#valueOf(Object)},
     * and the characters of that string were then
     * {@link #insert(int, String) inserted} into this character
     * sequence at the indicated offset.
     * <p>
     * The {@code offset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     *
     * @param offset the offset.
     * @param obj    an {@code Object}.
     *
     * @return a reference to this object.
     *
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     */
    // 向ASB的offset索引处插入一个Object值的字符串序列
    public AbstractStringBuilder insert(int offset, Object obj) {
        return insert(offset, String.valueOf(obj));
    }
    
    /**
     * Inserts the string representation of the {@code boolean}
     * argument into this sequence.
     * <p>
     * The overall effect is exactly as if the second argument were
     * converted to a string by the method {@link String#valueOf(boolean)},
     * and the characters of that string were then
     * {@link #insert(int, String) inserted} into this character
     * sequence at the indicated offset.
     * <p>
     * The {@code offset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     *
     * @param offset the offset.
     * @param b      a {@code boolean}.
     *
     * @return a reference to this object.
     *
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     */
    // 向ASB的offset索引处插入一个boolean值的字符串序列
    public AbstractStringBuilder insert(int offset, boolean b) {
        return insert(offset, String.valueOf(b));
    }
    
    /**
     * Inserts the string representation of the {@code char}
     * argument into this sequence.
     * <p>
     * The overall effect is exactly as if the second argument were
     * converted to a string by the method {@link String#valueOf(char)},
     * and the character in that string were then
     * {@link #insert(int, String) inserted} into this character
     * sequence at the indicated offset.
     * <p>
     * The {@code offset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     *
     * @param offset the offset.
     * @param c      a {@code char}.
     *
     * @return a reference to this object.
     *
     * @throws IndexOutOfBoundsException if the offset is invalid.
     */
    // 向ASB的offset索引处插入一个char值的字符串序列
    public AbstractStringBuilder insert(int offset, char c) {
        String.checkOffset(offset, count);
        ensureCapacityInternal(count + 1);
        shift(offset, 1);
        count += 1;
        if(isLatin1() && StringLatin1.canEncode(c)) {
            value[offset] = (byte) c;
        } else {
            if(isLatin1()) {
                inflate();
            }
            StringUTF16.putCharSB(value, offset, c);
        }
        return this;
    }
    
    /**
     * Inserts the string representation of the second {@code int}
     * argument into this sequence.
     * <p>
     * The overall effect is exactly as if the second argument were
     * converted to a string by the method {@link String#valueOf(int)},
     * and the characters of that string were then
     * {@link #insert(int, String) inserted} into this character
     * sequence at the indicated offset.
     * <p>
     * The {@code offset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     *
     * @param offset the offset.
     * @param i      an {@code int}.
     *
     * @return a reference to this object.
     *
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     */
    // 向ASB的offset索引处插入一个int值的字符串序列
    public AbstractStringBuilder insert(int offset, int i) {
        return insert(offset, String.valueOf(i));
    }
    
    /**
     * Inserts the string representation of the {@code long}
     * argument into this sequence.
     * <p>
     * The overall effect is exactly as if the second argument were
     * converted to a string by the method {@link String#valueOf(long)},
     * and the characters of that string were then
     * {@link #insert(int, String) inserted} into this character
     * sequence at the indicated offset.
     * <p>
     * The {@code offset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     *
     * @param offset the offset.
     * @param l      a {@code long}.
     *
     * @return a reference to this object.
     *
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     */
    // 向ASB的offset索引处插入一个long值的字符串序列
    public AbstractStringBuilder insert(int offset, long l) {
        return insert(offset, String.valueOf(l));
    }
    
    /**
     * Inserts the string representation of the {@code float}
     * argument into this sequence.
     * <p>
     * The overall effect is exactly as if the second argument were
     * converted to a string by the method {@link String#valueOf(float)},
     * and the characters of that string were then
     * {@link #insert(int, String) inserted} into this character
     * sequence at the indicated offset.
     * <p>
     * The {@code offset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     *
     * @param offset the offset.
     * @param f      a {@code float}.
     *
     * @return a reference to this object.
     *
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     */
    // 向ASB的offset索引处插入一个float值的字符串序列
    public AbstractStringBuilder insert(int offset, float f) {
        return insert(offset, String.valueOf(f));
    }
    
    /**
     * Inserts the string representation of the {@code double}
     * argument into this sequence.
     * <p>
     * The overall effect is exactly as if the second argument were
     * converted to a string by the method {@link String#valueOf(double)},
     * and the characters of that string were then
     * {@link #insert(int, String) inserted} into this character
     * sequence at the indicated offset.
     * <p>
     * The {@code offset} argument must be greater than or equal to
     * {@code 0}, and less than or equal to the {@linkplain #length() length}
     * of this sequence.
     *
     * @param offset the offset.
     * @param d      a {@code double}.
     *
     * @return a reference to this object.
     *
     * @throws StringIndexOutOfBoundsException if the offset is invalid.
     */
    // 向ASB的offset索引处插入一个double值的字符串序列
    public AbstractStringBuilder insert(int offset, double d) {
        return insert(offset, String.valueOf(d));
    }
    
    /*▲ 插入 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 替换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Replaces the characters in a substring of this sequence
     * with characters in the specified {@code String}. The substring
     * begins at the specified {@code start} and extends to the character
     * at index {@code end - 1} or to the end of the
     * sequence if no such character exists. First the
     * characters in the substring are removed and then the specified
     * {@code String} is inserted at {@code start}. (This
     * sequence will be lengthened to accommodate the
     * specified String if necessary.)
     *
     * @param start The beginning index, inclusive.
     * @param end   The ending index, exclusive.
     * @param str   String that will replace previous contents.
     *
     * @return This object.
     *
     * @throws StringIndexOutOfBoundsException if {@code start}
     *                                         is negative, greater than {@code length()}, or
     *                                         greater than {@code end}.
     */
    // 用str替换ASB在[start, end)范围内的字符序列
    public AbstractStringBuilder replace(int start, int end, String str) {
        int count = this.count;
        if(end>count) {
            end = count;
        }
        checkRangeSIOOBE(start, end, count);
        int len = str.length();
        int newCount = count + len - (end - start);
        ensureCapacityInternal(newCount);
        shift(end, newCount - count);
        this.count = newCount;
        // 向ASB的start索引处插入一个字符串str
        putStringAt(start, str);
        return this;
    }
    
    /*▲ 替换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向ASB的index索引处插入一个字符串str
    private final void putStringAt(int index, String str) {
        if(getCoder() != str.coder()) {
            inflate();
        }
        str.getBytes(value, index, coder);
    }
    
    // 向ASB的index索引处插入一个子序列，该子序列取自字符序列s的[off, end)范围
    private final void putCharsAt(int index, CharSequence s, int off, int end) {
        if(isLatin1()) {
            byte[] val = this.value;
            for(int i = off, j = index; i<end; i++) {
                char c = s.charAt(i);
                if(StringLatin1.canEncode(c)) {
                    val[j++] = (byte) c;
                } else {
                    inflate();
                    StringUTF16.putCharsSB(this.value, j, s, i, end);
                    return;
                }
            }
        } else {
            StringUTF16.putCharsSB(this.value, index, s, off, end);
        }
    }
    
    // 将s[off, end)内部的char批量转换为字节后，存入ASB的index处
    private final void putCharsAt(int index, char[] s, int off, int end) {
        if(isLatin1()) {
            byte[] val = this.value;
            for(int i = off, j = index; i<end; i++) {
                char c = s[i];
                if(StringLatin1.canEncode(c)) {
                    val[j++] = (byte) c;
                } else {
                    inflate();
                    StringUTF16.putCharsSB(this.value, j, s, i, end);
                    return;
                }
            }
        } else {
            StringUTF16.putCharsSB(this.value, index, s, off, end);
        }
    }
    
    // 从offset处的char开始，将后续所有的char平移n个单位，n的正负决定了前移或者后移
    private void shift(int offset, int n) {
        System.arraycopy(value, offset << coder, value, (offset + n) << coder, (count - offset) << coder);
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 求子串 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new {@code String} that contains a subsequence of
     * characters currently contained in this character sequence. The
     * substring begins at the specified index and extends to the end of
     * this sequence.
     *
     * @param start The beginning index, inclusive.
     *
     * @return The new string.
     *
     * @throws StringIndexOutOfBoundsException if {@code start} is
     *                                         less than zero, or greater than the length of this object.
     */
    // 求ASB在[start, ∞)范围内的子串
    public String substring(int start) {
        return substring(start, count);
    }
    
    /**
     * Returns a new {@code String} that contains a subsequence of
     * characters currently contained in this sequence. The
     * substring begins at the specified {@code start} and
     * extends to the character at index {@code end - 1}.
     *
     * @param start The beginning index, inclusive.
     * @param end   The ending index, exclusive.
     *
     * @return The new string.
     *
     * @throws StringIndexOutOfBoundsException if {@code start}
     *                                         or {@code end} are negative or greater than
     *                                         {@code length()}, or {@code start} is
     *                                         greater than {@code end}.
     */
    // 求ASB在[start, start+end)范围内的子串
    public String substring(int start, int end) {
        checkRangeSIOOBE(start, end, count);
        if(isLatin1()) {
            return StringLatin1.newString(value, start, end - start);
        }
        return StringUTF16.newString(value, start, end - start);
    }
    
    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     *
     * <p> An invocation of this method of the form
     *
     * <pre>{@code
     * sb.subSequence(begin,&nbsp;end)}</pre>
     *
     * behaves in exactly the same way as the invocation
     *
     * <pre>{@code
     * sb.substring(begin,&nbsp;end)}</pre>
     *
     * This method is provided so that this class can
     * implement the {@link CharSequence} interface.
     *
     * @param start the start index, inclusive.
     * @param end   the end index, exclusive.
     *
     * @return the specified subsequence.
     *
     * @throws IndexOutOfBoundsException if {@code start} or {@code end} are negative,
     *                                   if {@code end} is greater than {@code length()},
     *                                   or if {@code start} is greater than {@code end}
     * @spec JSR-51
     */
    // 求ASB在[start, start+end)范围内的子串
    @Override
    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }
    
    /*▲ 求子串 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找子串位置 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring.
     *
     * <p>The returned index is the smallest value {@code k} for which:
     * <pre>{@code
     * this.toString().startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param str the substring to search for.
     *
     * @return the index of the first occurrence of the specified substring,
     * or {@code -1} if there is no such occurrence.
     */
    // 返回子串str在当前主串ASB中第一次出现的位置
    public int indexOf(String str) {
        return indexOf(str, 0);
    }
    
    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring, starting at the specified index.
     *
     * <p>The returned index is the smallest value {@code k} for which:
     * <pre>{@code
     *     k >= Math.min(fromIndex, this.length()) &&
     *                   this.toString().startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param str       the substring to search for.
     * @param fromIndex the index from which to start the search.
     *
     * @return the index of the first occurrence of the specified substring,
     * starting at the specified index,
     * or {@code -1} if there is no such occurrence.
     */
    // 返回子串str在当前主串ASB中第一次出现的位置（从主串fromIndex处向后搜索）
    public int indexOf(String str, int fromIndex) {
        return String.indexOf(value, coder, count, str, fromIndex);
    }
    
    /**
     * Returns the index within this string of the last occurrence of the
     * specified substring.  The last occurrence of the empty string "" is
     * considered to occur at the index value {@code this.length()}.
     *
     * <p>The returned index is the largest value {@code k} for which:
     * <pre>{@code
     * this.toString().startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param str the substring to search for.
     *
     * @return the index of the last occurrence of the specified substring,
     * or {@code -1} if there is no such occurrence.
     */
    // 返回子串str在当前主串ASB中最后一次出现的位置
    public int lastIndexOf(String str) {
        return lastIndexOf(str, count);
    }
    
    /**
     * Returns the index within this string of the last occurrence of the
     * specified substring, searching backward starting at the specified index.
     *
     * <p>The returned index is the largest value {@code k} for which:
     * <pre>{@code
     *     k <= Math.min(fromIndex, this.length()) &&
     *                   this.toString().startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param str       the substring to search for.
     * @param fromIndex the index to start the search from.
     *
     * @return the index of the last occurrence of the specified substring,
     * searching backward from the specified index,
     * or {@code -1} if there is no such occurrence.
     */
    // 返回子串str在当前主串ASB中最后一次出现的位置（从主串fromIndex处向前搜索）
    public int lastIndexOf(String str, int fromIndex) {
        return String.lastIndexOf(value, coder, count, str, fromIndex);
    }
    
    /*▲ 查找子串位置 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取char/char[] ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the {@code char} value in this sequence at the specified index.
     * The first {@code char} value is at index {@code 0}, the next at index
     * {@code 1}, and so on, as in array indexing.
     * <p>
     * The index argument must be greater than or equal to
     * {@code 0}, and less than the length of this sequence.
     *
     * <p>If the {@code char} value specified by the index is a
     * <a href="Character.html#unicode">surrogate</a>, the surrogate
     * value is returned.
     *
     * @param index the index of the desired {@code char} value.
     *
     * @return the {@code char} value at the specified index.
     *
     * @throws IndexOutOfBoundsException if {@code index} is
     *                                   negative or greater than or equal to {@code length()}.
     */
    // 返回ASB内index索引处的char
    @Override
    public char charAt(int index) {
        String.checkIndex(index, count);
        if(isLatin1()) {
            return (char) (value[index] & 0xff);
        }
        return StringUTF16.charAt(value, index);
    }
    
    /**
     * Characters are copied from this sequence into the
     * destination character array {@code dst}. The first character to
     * be copied is at index {@code srcBegin}; the last character to
     * be copied is at index {@code srcEnd-1}. The total number of
     * characters to be copied is {@code srcEnd-srcBegin}. The
     * characters are copied into the subarray of {@code dst} starting
     * at index {@code dstBegin} and ending at index:
     * <pre>{@code
     * dstbegin + (srcEnd-srcBegin) - 1
     * }</pre>
     *
     * @param srcBegin start copying at this offset.
     * @param srcEnd   stop copying at this offset.
     * @param dst      the array to copy the data into.
     * @param dstBegin offset into {@code dst}.
     *
     * @throws IndexOutOfBoundsException if any of the following is true:
     *                                   <ul>
     *                                   <li>{@code srcBegin} is negative
     *                                   <li>{@code dstBegin} is negative
     *                                   <li>the {@code srcBegin} argument is greater than
     *                                   the {@code srcEnd} argument.
     *                                   <li>{@code srcEnd} is greater than
     *                                   {@code this.length()}.
     *                                   <li>{@code dstBegin+srcEnd-srcBegin} is greater than
     *                                   {@code dst.length}
     *                                   </ul>
     */
    // 将ASB[srcBegin, srcEnd)内的字节批量转换为char后存入dst
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        checkRangeSIOOBE(srcBegin, srcEnd, count);  // compatible to old version
        int n = srcEnd - srcBegin;
        checkRange(dstBegin, dstBegin + n, dst.length);
        if(isLatin1()) {
            StringLatin1.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        } else {
            StringUTF16.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        }
    }
    
    /**
     * The character at the specified index is set to {@code ch}. This
     * sequence is altered to represent a new character sequence that is
     * identical to the old character sequence, except that it contains the
     * character {@code ch} at position {@code index}.
     * <p>
     * The index argument must be greater than or equal to
     * {@code 0}, and less than the length of this sequence.
     *
     * @param index the index of the character to modify.
     * @param ch    the new character.
     *
     * @throws IndexOutOfBoundsException if {@code index} is
     *                                   negative or greater than or equal to {@code length()}.
     */
    // 将ch存入ASB的index索引处
    public void setCharAt(int index, char ch) {
        String.checkIndex(index, count);
        if(isLatin1() && StringLatin1.canEncode(ch)) {
            value[index] = (byte) ch;
        } else {
            if(isLatin1()) {
                inflate();
            }
            StringUTF16.putCharSB(value, index, ch);
        }
    }
    
    /*▲ 获取char/char[] ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 码点/码元 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the character (Unicode code point) at the specified
     * index. The index refers to {@code char} values
     * (Unicode code units) and ranges from {@code 0} to
     * {@link #length()}{@code  - 1}.
     *
     * <p> If the {@code char} value specified at the given index
     * is in the high-surrogate range, the following index is less
     * than the length of this sequence, and the
     * {@code char} value at the following index is in the
     * low-surrogate range, then the supplementary code point
     * corresponding to this surrogate pair is returned. Otherwise,
     * the {@code char} value at the given index is returned.
     *
     * @param index the index to the {@code char} values
     *
     * @return the code point value of the character at the
     * {@code index}
     *
     * @throws IndexOutOfBoundsException if the {@code index}
     *                                   argument is negative or not less than the length of this
     *                                   sequence.
     */
    // 返回ASB内index索引处的Unicode编码（从前到后试探）
    public int codePointAt(int index) {
        int count = this.count;
        byte[] value = this.value;
        String.checkIndex(index, count);
        if(isLatin1()) {
            return value[index] & 0xff;
        }
        return StringUTF16.codePointAtSB(value, index, count);
    }
    
    /**
     * Returns the character (Unicode code point) before the specified
     * index. The index refers to {@code char} values
     * (Unicode code units) and ranges from {@code 1} to {@link
     * #length()}.
     *
     * <p> If the {@code char} value at {@code (index - 1)}
     * is in the low-surrogate range, {@code (index - 2)} is not
     * negative, and the {@code char} value at {@code (index -
     * 2)} is in the high-surrogate range, then the
     * supplementary code point value of the surrogate pair is
     * returned. If the {@code char} value at {@code index -
     * 1} is an unpaired low-surrogate or a high-surrogate, the
     * surrogate value is returned.
     *
     * @param index the index following the code point that should be returned
     *
     * @return the Unicode code point value before the given index.
     *
     * @throws IndexOutOfBoundsException if the {@code index}
     *                                   argument is less than 1 or greater than the length
     *                                   of this sequence.
     */
    // 返回ASB内index-1索引处的Unicode编码（从后往前试探）
    public int codePointBefore(int index) {
        int i = index - 1;
        if(i<0 || i >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        if(isLatin1()) {
            return value[i] & 0xff;
        }
        return StringUTF16.codePointBeforeSB(value, index);
    }
    
    /**
     * Returns the number of Unicode code points in the specified text
     * range of this sequence. The text range begins at the specified
     * {@code beginIndex} and extends to the {@code char} at
     * index {@code endIndex - 1}. Thus the length (in
     * {@code char}s) of the text range is
     * {@code endIndex-beginIndex}. Unpaired surrogates within
     * this sequence count as one code point each.
     *
     * @param beginIndex the index to the first {@code char} of
     *                   the text range.
     * @param endIndex   the index after the last {@code char} of
     *                   the text range.
     *
     * @return the number of Unicode code points in the specified text
     * range
     *
     * @throws IndexOutOfBoundsException if the
     *                                   {@code beginIndex} is negative, or {@code endIndex}
     *                                   is larger than the length of this sequence, or
     *                                   {@code beginIndex} is larger than {@code endIndex}.
     */
    // 统计ASB中指定码元范围内存在多少个Unicode符号
    public int codePointCount(int beginIndex, int endIndex) {
        if(beginIndex<0 || endIndex>count || beginIndex>endIndex) {
            throw new IndexOutOfBoundsException();
        }
        if(isLatin1()) {
            return endIndex - beginIndex;
        }
        return StringUTF16.codePointCountSB(value, beginIndex, endIndex);
    }
    
    /**
     * Returns the index within this sequence that is offset from the
     * given {@code index} by {@code codePointOffset} code
     * points. Unpaired surrogates within the text range given by
     * {@code index} and {@code codePointOffset} count as
     * one code point each.
     *
     * @param index           the index to be offset
     * @param codePointOffset the offset in code points
     *
     * @return the index within this sequence
     *
     * @throws IndexOutOfBoundsException if {@code index}
     *                                   is negative or larger then the length of this sequence,
     *                                   or if {@code codePointOffset} is positive and the subsequence
     *                                   starting with {@code index} has fewer than
     *                                   {@code codePointOffset} code points,
     *                                   or if {@code codePointOffset} is negative and the subsequence
     *                                   before {@code index} has fewer than the absolute value of
     *                                   {@code codePointOffset} code points.
     */
    // 返回从index偏移codePointOffset个Unicode符号后新的索引值，codePointOffset的正负决定了偏移方向
    public int offsetByCodePoints(int index, int codePointOffset) {
        if(index<0 || index>count) {
            throw new IndexOutOfBoundsException();
        }
        return Character.offsetByCodePoints(this, index, codePointOffset);
    }
    
    /*▲ 码点/码元 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 流 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * @since 9
     */
    // 将当前char序列转为流序列，序列中每个元素是char
    @Override
    public IntStream chars() {
        // Reuse String-based spliterator. This requires a supplier to capture the value and count when the terminal operation is executed
        return StreamSupport.intStream(() -> {
            // The combined set of field reads are not atomic and thread safe but bounds checks will ensure no unsafe reads from the byte array
            byte[] val = this.value;
            int count = this.count;
            byte coder = this.coder;
            return coder == String.LATIN1 ? new StringLatin1.CharsSpliterator(val, 0, count, 0) : new StringUTF16.CharsSpliterator(val, 0, count, 0);
        }, Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED, false);
    }
    
    /**
     * {@inheritDoc}
     *
     * @since 9
     */
    // 将当前Unicode符号序列转为流序列，序列中每个元素是Unicode符号
    @Override
    public IntStream codePoints() {
        // Reuse String-based spliterator. This requires a supplier to capture the value and count when the terminal operation is executed
        return StreamSupport.intStream(() -> {
            // The combined set of field reads are not atomic and thread safe but bounds checks will ensure no unsafe reads from the byte array
            byte[] val = this.value;
            int count = this.count;
            byte coder = this.coder;
            return coder == String.LATIN1 ? new StringLatin1.CharsSpliterator(val, 0, count, 0) : new StringUTF16.CodePointsSpliterator(val, 0, count, 0);
        }, Spliterator.ORDERED, false);
    }
    
    /*▲ 流 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 容量 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current capacity. The capacity is the amount of storage
     * available for newly inserted characters, beyond which an allocation
     * will occur.
     *
     * @return the current capacity
     */
    // 返回当前ASB的容量（可以容纳的char的数量）
    public int capacity() {
        return value.length >> coder;
    }
    
    /**
     * Ensures that the capacity is at least equal to the specified minimum.
     * If the current capacity is less than the argument, then a new internal
     * array is allocated with greater capacity. The new capacity is the
     * larger of:
     * <ul>
     * <li>The {@code minimumCapacity} argument.
     * <li>Twice the old capacity, plus {@code 2}.
     * </ul>
     * If the {@code minimumCapacity} argument is nonpositive, this
     * method takes no action and simply returns.
     * Note that subsequent operations on this object can reduce the
     * actual capacity below that requested here.
     *
     * @param minimumCapacity the minimum desired capacity.
     */
    // 确保ASB内部拥有最小容量minimumCapacity
    public void ensureCapacity(int minimumCapacity) {
        if(minimumCapacity>0) {
            ensureCapacityInternal(minimumCapacity);
        }
    }
    
    /**
     * Attempts to reduce storage used for the character sequence.
     * If the buffer is larger than necessary to hold its current sequence of
     * characters, then it may be resized to become more space efficient.
     * Calling this method may, but is not required to, affect the value
     * returned by a subsequent call to the {@link #capacity()} method.
     */
    // 缩减ABS容量以恰好容纳其内容
    public void trimToSize() {
        int length = count << coder;
        if(length<value.length) {
            value = Arrays.copyOf(value, length);
        }
    }
    
    /**
     * Sets the length of the character sequence.
     * The sequence is changed to a new character sequence
     * whose length is specified by the argument. For every nonnegative
     * index <i>k</i> less than {@code newLength}, the character at
     * index <i>k</i> in the new character sequence is the same as the
     * character at index <i>k</i> in the old sequence if <i>k</i> is less
     * than the length of the old character sequence; otherwise, it is the
     * null character {@code '\u005Cu0000'}.
     *
     * In other words, if the {@code newLength} argument is less than
     * the current length, the length is changed to the specified length.
     * <p>
     * If the {@code newLength} argument is greater than or equal
     * to the current length, sufficient null characters
     * ({@code '\u005Cu0000'}) are appended so that
     * length becomes the {@code newLength} argument.
     * <p>
     * The {@code newLength} argument must be greater than or equal
     * to {@code 0}.
     *
     * @param newLength the new length
     *
     * @throws IndexOutOfBoundsException if the
     *                                   {@code newLength} argument is negative.
     */
    // 扩展ASB容量，多出来的部分用0填充，且设置ASB的长度为newLength
    public void setLength(int newLength) {
        if(newLength<0) {
            throw new StringIndexOutOfBoundsException(newLength);
        }
        ensureCapacityInternal(newLength);
        if(count<newLength) {
            if(isLatin1()) {
                StringLatin1.fillNull(value, count, newLength);
            } else {
                StringUTF16.fillNull(value, count, newLength);
            }
        }
        count = newLength;
    }
    
    /**
     * Returns a capacity at least as large as the given minimum capacity.
     * Returns the current capacity increased by the same amount + 2 if
     * that suffices.
     * Will not return a capacity greater than
     * {@code (MAX_ARRAY_SIZE >> coder)} unless the given minimum capacity
     * is greater than that.
     *
     * @param minCapacity the desired minimum capacity
     *
     * @throws OutOfMemoryError if minCapacity is less than zero or
     *                          greater than (Integer.MAX_VALUE >> coder)
     */
    // 根据申请的容量minCapacity，计算实际可使用的容量（防止容量越界）
    private int newCapacity(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = value.length >> coder;
        
        // 默认将容量翻倍
        int newCapacity = (oldCapacity << 1) + 2;
        
        // 翻倍后的容量不满足申请的容量
        if(newCapacity - minCapacity<0) {
            newCapacity = minCapacity;
        }
        
        // 允许的最大的容量
        int SAFE_BOUND = MAX_ARRAY_SIZE >> coder;
        
        return (newCapacity>0 && SAFE_BOUND - newCapacity >= 0) ? newCapacity : hugeCapacity(minCapacity);
    }
    
    // 处理可能越界的情形，确保返回一个安全容量
    private int hugeCapacity(int minCapacity) {
        int SAFE_BOUND = MAX_ARRAY_SIZE >> coder;
        int UNSAFE_BOUND = Integer.MAX_VALUE >> coder;
        if(UNSAFE_BOUND - minCapacity<0) { // overflow
            throw new OutOfMemoryError();
        }
        return (minCapacity>SAFE_BOUND) ? minCapacity : SAFE_BOUND;
    }
    
    /**
     * For positive values of {@code minimumCapacity}, this method
     * behaves like {@code ensureCapacity}, however it is never
     * synchronized.
     * If {@code minimumCapacity} is non positive due to numeric
     * overflow, this method throws {@code OutOfMemoryError}.
     */
    // 确保ASB内部拥有最小容量minimumCapacity（容量按存储char的能力计算）
    private void ensureCapacityInternal(int minimumCapacity) {
        // overflow-conscious code
        int oldCapacity = value.length >> coder;
        // 扩容
        if(minimumCapacity - oldCapacity>0) {
            value = Arrays.copyOf(value, newCapacity(minimumCapacity) << coder);
        }
    }
    
    /*▲ 容量 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 逆置 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Causes this character sequence to be replaced by the reverse of
     * the sequence. If there are any surrogate pairs included in the
     * sequence, these are treated as single characters for the
     * reverse operation. Thus, the order of the high-low surrogates
     * is never reversed.
     *
     * Let <i>n</i> be the character length of this character sequence
     * (not the length in {@code char} values) just prior to
     * execution of the {@code reverse} method. Then the
     * character at index <i>k</i> in the new character sequence is
     * equal to the character at index <i>n-k-1</i> in the old
     * character sequence.
     *
     * <p>Note that the reverse operation may result in producing
     * surrogate pairs that were unpaired low-surrogates and
     * high-surrogates before the operation. For example, reversing
     * "\u005CuDC00\u005CuD800" produces "\u005CuD800\u005CuDC00" which is
     * a valid surrogate pair.
     *
     * @return a reference to this object.
     */
    // 逆置ASB
    public AbstractStringBuilder reverse() {
        byte[] val = this.value;
        int count = this.count;
        int coder = this.coder;
        int n = count - 1;
        if(String.COMPACT_STRINGS && coder == String.LATIN1) {
            for(int j = (n - 1) >> 1; j >= 0; j--) {
                int k = n - j;
                byte cj = val[j];
                val[j] = val[k];
                val[k] = cj;
            }
        } else {
            StringUTF16.reverse(val, count);
        }
        return this;
    }
    
    /*▲ 逆置 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 比较 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Compares the objects of two AbstractStringBuilder implementations lexicographically.
     *
     * @since 11
     */
    // 比较两个ASB的内容
    int compareTo(AbstractStringBuilder another) {
        if(this == another) {
            return 0;
        }
        
        byte val1[] = value;
        byte val2[] = another.value;
        int count1 = this.count;
        int count2 = another.count;
        
        if(coder == another.coder) {
            return isLatin1() ? StringLatin1.compareTo(val1, val2, count1, count2) : StringUTF16.compareTo(val1, val2, count1, count2);
        }
        
        return isLatin1() ? StringLatin1.compareToUTF16(val1, val2, count1, count2) : StringUTF16.compareToLatin1(val1, val2, count1, count2);
    }
    
    /*▲ 比较 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ ASB属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Needed by {@code String} for the contentEquals method.
     */
    // 返回当前ASB的内部存储结构
    final byte[] getValue() {
        return value;
    }
    
    // 返回当前ASB的编码
    final byte getCoder() {
        return String.COMPACT_STRINGS ? coder : String.UTF16;
    }
    
    // 判断当前ASB是否为Latin1-ASB
    final boolean isLatin1() {
        return String.COMPACT_STRINGS && coder == String.LATIN1;
    }
    
    /**
     * Returns the length (character count).
     *
     * @return the length of the sequence of characters currently
     * represented by this object
     */
    // 返回当前ASB内包含的char的数量
    @Override
    public int length() {
        return count;
    }
    
    /*▲ ASB属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Invoker guarantees it is in UTF16 (inflate itself for asb), if two
     * coders are different and the dstBegin has enough space
     *
     * @param dstBegin  the char index, not offset of byte[]
     * @param coder     the coder of dst[]
     */
    // 将ASB的内部字节存入dst
    void getBytes(byte dst[], int dstBegin, byte coder) {
        if(this.coder == coder) {
            System.arraycopy(value, 0, dst, dstBegin << coder, count << coder);
        } else {        // this.coder == LATIN && coder == UTF16
            // 从LATIN1-String内部的字节转为UTF16-String内部的字节
            StringLatin1.inflate(value, 0, dst, dstBegin, count);
        }
    }
    
    /**
     * If the coder is "isLatin1", this inflates the internal 8-bit storage
     * to 16-bit <hi=0, low> pair storage.
     */
    // 将LATIN1-ASB转为UTF16-ASB
    private void inflate() {
        if(!isLatin1()) {
            return;
        }
        
        byte[] buf = StringUTF16.newBytesFor(value.length);
        StringLatin1.inflate(value, 0, buf, 0, count);
        this.value = buf;
        this.coder = String.UTF16;
    }
    
    /** for readObject() */
    // 用于序列化。将value[off, off+len)内的char转为byte存入ASB
    void initBytes(char[] value, int off, int len) {
        if(String.COMPACT_STRINGS) {
            this.value = StringUTF16.compress(value, off, len);
            if(this.value != null) {
                this.coder = String.LATIN1;
                return;
            }
        }
        this.coder = String.UTF16;
        this.value = StringUTF16.toBytes(value, off, len);
    }
    
    /**
     * Returns a string representing the data in this sequence.
     * A new {@code String} object is allocated and initialized to
     * contain the character sequence currently represented by this
     * object. This {@code String} is then returned. Subsequent
     * changes to this sequence do not affect the contents of the
     * {@code String}.
     *
     * @return a string representation of this sequence of characters.
     */
    @Override
    public abstract String toString();
    
    
    
    /*▼ 越界检查 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /* IndexOutOfBoundsException, if out of bounds */
    // 保证0<=start<=end<=len
    private static void checkRange(int start, int end, int len) {
        if(start<0 || start>end || end>len) {
            throw new IndexOutOfBoundsException("start " + start + ", end " + end + ", length " + len);
        }
    }
    
    /* StringIndexOutOfBoundsException, if out of bounds */
    // 保证0<=start<=end<=len
    private static void checkRangeSIOOBE(int start, int end, int len) {
        if(start<0 || start>end || end>len) {
            throw new StringIndexOutOfBoundsException("start " + start + ", end " + end + ", length " + len);
        }
    }
    
    /*▲ 越界检查 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
