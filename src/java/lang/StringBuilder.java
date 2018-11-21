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

import jdk.internal.HotSpotIntrinsicCandidate;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A mutable sequence of characters.  This class provides an API compatible
 * with {@code StringBuffer}, but with no guarantee of synchronization.
 * This class is designed for use as a drop-in replacement for
 * {@code StringBuffer} in places where the string buffer was being
 * used by a single thread (as is generally the case).   Where possible,
 * it is recommended that this class be used in preference to
 * {@code StringBuffer} as it will be faster under most implementations.
 *
 * <p>The principal operations on a {@code StringBuilder} are the
 * {@code append} and {@code insert} methods, which are
 * overloaded so as to accept data of any type. Each effectively
 * converts a given datum to a string and then appends or inserts the
 * characters of that string to the string builder. The
 * {@code append} method always adds these characters at the end
 * of the builder; the {@code insert} method adds the characters at
 * a specified point.
 * <p>
 * For example, if {@code z} refers to a string builder object
 * whose current contents are "{@code start}", then
 * the method call {@code z.append("le")} would cause the string
 * builder to contain "{@code startle}", whereas
 * {@code z.insert(4, "le")} would alter the string builder to
 * contain "{@code starlet}".
 * <p>
 * In general, if sb refers to an instance of a {@code StringBuilder},
 * then {@code sb.append(x)} has the same effect as
 * {@code sb.insert(sb.length(), x)}.
 * <p>
 * Every string builder has a capacity. As long as the length of the
 * character sequence contained in the string builder does not exceed
 * the capacity, it is not necessary to allocate a new internal
 * buffer. If the internal buffer overflows, it is automatically made larger.
 *
 * <p>Instances of {@code StringBuilder} are not safe for
 * use by multiple threads. If such synchronization is required then it is
 * recommended that {@link java.lang.StringBuffer} be used.
 *
 * <p>Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * @author Michael McCloskey
 * @apiNote {@code StringBuilder} implements {@code Comparable} but does not override
 * {@link Object#equals equals}. Thus, the natural ordering of {@code StringBuilder}
 * is inconsistent with equals. Care should be exercised if {@code StringBuilder}
 * objects are used as keys in a {@code SortedMap} or elements in a {@code SortedSet}.
 * See {@link Comparable}, {@link java.util.SortedMap SortedMap}, or
 * {@link java.util.SortedSet SortedSet} for more information.
 * @see java.lang.StringBuffer
 * @see java.lang.String
 * @since 1.5
 */
// 非线程安全的字符序列，适合单线程下操作大量字符，内部实现为字节数组
public final class StringBuilder extends AbstractStringBuilder implements Serializable, Comparable<StringBuilder>, CharSequence {
    
    /** use serialVersionUID for interoperability */
    static final long serialVersionUID = 4383685877147921099L;
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs a string builder with no characters in it and an
     * initial capacity of 16 characters.
     */
    @HotSpotIntrinsicCandidate
    public StringBuilder() {
        super(16);
    }
    
    /**
     * Constructs a string builder with no characters in it and an
     * initial capacity specified by the {@code capacity} argument.
     *
     * @param capacity the initial capacity.
     *
     * @throws NegativeArraySizeException if the {@code capacity} argument is less than {@code 0}.
     */
    @HotSpotIntrinsicCandidate
    public StringBuilder(int capacity) {
        super(capacity);
    }
    
    /**
     * Constructs a string builder initialized to the contents of the
     * specified string. The initial capacity of the string builder is
     * {@code 16} plus the length of the string argument.
     *
     * @param str the initial contents of the buffer.
     */
    @HotSpotIntrinsicCandidate
    public StringBuilder(String str) {
        super(str.length() + 16);
        append(str);
    }
    
    /**
     * Constructs a string builder that contains the same characters
     * as the specified {@code CharSequence}. The initial capacity of
     * the string builder is {@code 16} plus the length of the
     * {@code CharSequence} argument.
     *
     * @param seq the sequence to copy.
     */
    public StringBuilder(CharSequence seq) {
        this(seq.length() + 16);
        append(seq);
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 添加 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向StringBuilder末尾添加一个字符序列
    @Override
    public StringBuilder append(CharSequence s) {
        super.append(s);
        return this;
    }
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 向StringBuilder末尾添加一个字符序列，该子序列取自字符序列s的[start, end)范围
    @Override
    public StringBuilder append(CharSequence s, int start, int end) {
        super.append(s, start, end);
        return this;
    }
    
    // 向StringBuilder末尾添加一个字符串str
    @Override
    @HotSpotIntrinsicCandidate
    public StringBuilder append(String str) {
        super.append(str);
        return this;
    }
    
    // 向StringBuilder末尾添加一个StringBuffer
    /**
     * Appends the specified {@code StringBuffer} to this sequence.
     * <p>
     * The characters of the {@code StringBuffer} argument are appended,
     * in order, to this sequence, increasing the
     * length of this sequence by the length of the argument.
     * If {@code sb} is {@code null}, then the four characters
     * {@code "null"} are appended to this sequence.
     * <p>
     * Let <i>n</i> be the length of this character sequence just prior to
     * execution of the {@code append} method. Then the character at index
     * <i>k</i> in the new character sequence is equal to the character at
     * index <i>k</i> in the old character sequence, if <i>k</i> is less than
     * <i>n</i>; otherwise, it is equal to the character at index <i>k-n</i>
     * in the argument {@code sb}.
     *
     * @param sb the {@code StringBuffer} to append.
     *
     * @return a reference to this object.
     */
    public StringBuilder append(StringBuffer sb) {
        super.append(sb);
        return this;
    }
    
    // 向StringBuilder末尾添加一个字符序列
    @Override
    public StringBuilder append(char[] str) {
        super.append(str);
        return this;
    }
    
    // 向StringBuilder末尾添加一个子序列，该子序列取自字符数组s的[offset, offset+len)范围
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder append(char[] str, int offset, int len) {
        super.append(str, offset, len);
        return this;
    }
    
    // 向StringBuilder末尾添加一个Object值的字符串序列
    @Override
    public StringBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }
    
    // 向StringBuilder末尾添加一个boolean值的字符串序列
    @Override
    public StringBuilder append(boolean b) {
        super.append(b);
        return this;
    }
    
    // 向StringBuilder末尾添加一个char值的字符串序列
    @Override
    @HotSpotIntrinsicCandidate
    public StringBuilder append(char c) {
        super.append(c);
        return this;
    }
    
    // 向StringBuilder末尾添加一个int值的字符串序列
    @Override
    @HotSpotIntrinsicCandidate
    public StringBuilder append(int i) {
        super.append(i);
        return this;
    }
    
    // 向StringBuilder末尾添加一个long值的字符串序列
    @Override
    public StringBuilder append(long l) {
        super.append(l);
        return this;
    }
    
    // 向StringBuilder末尾添加一个float值的字符串序列
    @Override
    public StringBuilder append(float f) {
        super.append(f);
        return this;
    }
    
    // 向StringBuilder末尾添加一个double值的字符串序列
    @Override
    public StringBuilder append(double d) {
        super.append(d);
        return this;
    }
    
    // 向StringBuilder末尾添加一个由Unicode码点值表示的char的字符串序列
    /**
     * @since 1.5
     */
    @Override
    public StringBuilder appendCodePoint(int codePoint) {
        super.appendCodePoint(codePoint);
        return this;
    }
    
    /*▲ 添加 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 删除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 删除[start, end)范围内的char
    @Override
    public StringBuilder delete(int start, int end) {
        super.delete(start, end);
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 删除索引为index的char
    @Override
    public StringBuilder deleteCharAt(int index) {
        super.deleteCharAt(index);
        return this;
    }
    
    /*▲ 删除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 插入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向StringBuilder的dstOffset索引处插入一个子序列s
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int dstOffset, CharSequence s) {
        super.insert(dstOffset, s);
        return this;
    }
    
    // 向StringBuilder的dstOffset索引处插入一个子序列，该子序列取自字符序列s的[start, end)范围
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int dstOffset, CharSequence s, int start, int end) {
        super.insert(dstOffset, s, start, end);
        return this;
    }
    
    // 向StringBuilder的offset索引处插入一个子符序列str
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int offset, char[] str) {
        super.insert(offset, str);
        return this;
    }
    
    // 向StringBuilder的index索引处插入一个子序列，该子序列取自字符序列str的[offset, offset+len)范围
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int index, char[] str, int offset, int len) {
        super.insert(index, str, offset, len);
        return this;
    }
    
    // 向StringBuilder的offset索引处插入一个字符串str
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int offset, String str) {
        super.insert(offset, str);
        return this;
    }
    
    // 向StringBuilder的offset索引处插入一个Object值的字符串序列
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int offset, Object obj) {
        super.insert(offset, obj);
        return this;
    }
    
    // 向StringBuilder的offset索引处插入一个boolean值的字符串序列
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int offset, boolean b) {
        super.insert(offset, b);
        return this;
    }
    
    // 向StringBuilder的offset索引处插入一个char值的字符串序列
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int offset, char c) {
        super.insert(offset, c);
        return this;
    }
    
    // 向StringBuilder的offset索引处插入一个int值的字符串序列
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int offset, int i) {
        super.insert(offset, i);
        return this;
    }
    
    // 向StringBuilder的offset索引处插入一个long值的字符串序列
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int offset, long l) {
        super.insert(offset, l);
        return this;
    }
    
    // 向StringBuilder的offset索引处插入一个float值的字符串序列
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int offset, float f) {
        super.insert(offset, f);
        return this;
    }
    
    // 向StringBuilder的offset索引处插入一个double值的字符串序列
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public StringBuilder insert(int offset, double d) {
        super.insert(offset, d);
        return this;
    }
    
    /*▲ 插入 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 替换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 用str替换StringBuilder在[start, end)范围内的字符序列
    @Override
    public StringBuilder replace(int start, int end, String str) {
        super.replace(start, end, str);
        return this;
    }
    
    /*▲ 替换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 求子串 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*▲ 求子串 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找子串位置 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回子串str在当前主串StringBuilder中第一次出现的位置
    @Override
    public int indexOf(String str) {
        return super.indexOf(str);
    }
    
    // 返回子串str在当前主串StringBuilder中第一次出现的位置（从主串fromIndex处向后搜索）
    @Override
    public int indexOf(String str, int fromIndex) {
        return super.indexOf(str, fromIndex);
    }
    
    // 返回子串str在当前主串StringBuilder中最后一次出现的位置
    @Override
    public int lastIndexOf(String str) {
        return super.lastIndexOf(str);
    }
    
    // 返回子串str在当前主串StringBuilder中最后一次出现的位置（从主串fromIndex处向前搜索）
    @Override
    public int lastIndexOf(String str, int fromIndex) {
        return super.lastIndexOf(str, fromIndex);
    }
    
    /*▲ 查找子串位置 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 逆置 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 逆置StringBuilder
    @Override
    public StringBuilder reverse() {
        super.reverse();
        return this;
    }
    
    /*▲ 逆置 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 比较 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Compares two {@code StringBuilder} instances lexicographically. This method
     * follows the same rules for lexicographical comparison as defined in the
     * {@linkplain java.lang.CharSequence#compare(java.lang.CharSequence,
     * java.lang.CharSequence)  CharSequence.compare(this, another)} method.
     *
     * <p>
     * For finer-grained, locale-sensitive String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param another the {@code StringBuilder} to be compared with
     *
     * @return the value {@code 0} if this {@code StringBuilder} contains the same
     * character sequence as that of the argument {@code StringBuilder}; a negative integer
     * if this {@code StringBuilder} is lexicographically less than the
     * {@code StringBuilder} argument; or a positive integer if this {@code StringBuilder}
     * is lexicographically greater than the {@code StringBuilder} argument.
     *
     * @since 11
     */
    // 比较两个StringBuilder的内容
    @Override
    public int compareTo(StringBuilder another) {
        return super.compareTo(another);
    }
    
    /*▲ 比较 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Save the state of the {@code StringBuilder} instance to a stream
     * (that is, serialize it).
     *
     * @serialData the number of characters currently stored in the string
     * builder ({@code int}), followed by the characters in the
     * string builder ({@code char[]}).   The length of the
     * {@code char} array may be greater than the number of
     * characters currently stored in the string builder, in which
     * case extra characters are ignored.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(count);
        char[] val = new char[capacity()];
        if(isLatin1()) {
            StringLatin1.getChars(value, 0, count, val, 0);
        } else {
            StringUTF16.getChars(value, 0, count, val, 0);
        }
        s.writeObject(val);
    }
    
    /**
     * readObject is called to restore the state of the StringBuffer from a stream.
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        count = s.readInt();
        char[] val = (char[]) s.readObject();
        initBytes(val, 0, val.length);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    @Override
    @HotSpotIntrinsicCandidate
    public String toString() {
        // Create a copy, don't share the array
        return isLatin1() ? StringLatin1.newString(value, 0, count) : StringUTF16.newString(value, 0, count);
    }
    
}
