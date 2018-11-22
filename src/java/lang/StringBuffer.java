/*
 * Copyright (c) 1994, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ObjectStreamField;
import java.io.Serializable;

/**
 * A thread-safe, mutable sequence of characters.
 * A string buffer is like a {@link String}, but can be modified. At any
 * point in time it contains some particular sequence of characters, but
 * the length and content of the sequence can be changed through certain
 * method calls.
 * <p>
 * String buffers are safe for use by multiple threads. The methods
 * are synchronized where necessary so that all the operations on any
 * particular instance behave as if they occur in some serial order
 * that is consistent with the order of the method calls made by each of
 * the individual threads involved.
 * <p>
 * The principal operations on a {@code StringBuffer} are the
 * {@code append} and {@code insert} methods, which are
 * overloaded so as to accept data of any type. Each effectively
 * converts a given datum to a string and then appends or inserts the
 * characters of that string to the string buffer. The
 * {@code append} method always adds these characters at the end
 * of the buffer; the {@code insert} method adds the characters at
 * a specified point.
 * <p>
 * For example, if {@code z} refers to a string buffer object
 * whose current contents are {@code "start"}, then
 * the method call {@code z.append("le")} would cause the string
 * buffer to contain {@code "startle"}, whereas
 * {@code z.insert(4, "le")} would alter the string buffer to
 * contain {@code "starlet"}.
 * <p>
 * In general, if sb refers to an instance of a {@code StringBuffer},
 * then {@code sb.append(x)} has the same effect as
 * {@code sb.insert(sb.length(), x)}.
 * <p>
 * Whenever an operation occurs involving a source sequence (such as
 * appending or inserting from a source sequence), this class synchronizes
 * only on the string buffer performing the operation, not on the source.
 * Note that while {@code StringBuffer} is designed to be safe to use
 * concurrently from multiple threads, if the constructor or the
 * {@code append} or {@code insert} operation is passed a source sequence
 * that is shared across threads, the calling code must ensure
 * that the operation has a consistent and unchanging view of the source
 * sequence for the duration of the operation.
 * This could be satisfied by the caller holding a lock during the
 * operation's call, by using an immutable source sequence, or by not
 * sharing the source sequence across threads.
 * <p>
 * Every string buffer has a capacity. As long as the length of the
 * character sequence contained in the string buffer does not exceed
 * the capacity, it is not necessary to allocate a new internal
 * buffer array. If the internal buffer overflows, it is
 * automatically made larger.
 * <p>
 * Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 * <p>
 * As of  release JDK 5, this class has been supplemented with an equivalent
 * class designed for use by a single thread, {@link StringBuilder}.  The
 * {@code StringBuilder} class should generally be used in preference to
 * this one, as it supports all of the same operations but it is faster, as
 * it performs no synchronization.
 *
 * @author Arthur van Hoff
 * @apiNote {@code StringBuffer} implements {@code Comparable} but does not override
 * {@link Object#equals equals}. Thus, the natural ordering of {@code StringBuffer}
 * is inconsistent with equals. Care should be exercised if {@code StringBuffer}
 * objects are used as keys in a {@code SortedMap} or elements in a {@code SortedSet}.
 * See {@link Comparable}, {@link java.util.SortedMap SortedMap}, or
 * {@link java.util.SortedSet SortedSet} for more information.
 * @see java.lang.StringBuilder
 * @see java.lang.String
 * @since 1.0
 */
/*
 * 线程安全的字符序列，适合多线程下操作大量字符，内部实现为字节数组
 * 线程安全的原理是涉及到修改StringBuffer的操作被synchronized修饰
 */
public final class StringBuffer extends AbstractStringBuilder implements Serializable, Comparable<StringBuffer>, CharSequence {
    
    /**
     * A cache of the last value returned by toString. Cleared whenever the StringBuffer is modified.
     */
    // 调用toString()后生成的缓存，用于存储ASB中的字符序列。每次更改ASB都会清理缓存
    private transient String toStringCache;
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs a string buffer with no characters in it and an initial capacity of 16 characters.
     */
    @HotSpotIntrinsicCandidate
    public StringBuffer() {
        super(16);
    }
    
    /**
     * Constructs a string buffer with no characters in it and
     * the specified initial capacity.
     *
     * @param capacity the initial capacity.
     *
     * @throws NegativeArraySizeException if the {@code capacity}
     *                                    argument is less than {@code 0}.
     */
    @HotSpotIntrinsicCandidate
    public StringBuffer(int capacity) {
        super(capacity);
    }
    
    /**
     * Constructs a string buffer initialized to the contents of the
     * specified string. The initial capacity of the string buffer is
     * {@code 16} plus the length of the string argument.
     *
     * @param str the initial contents of the buffer.
     */
    @HotSpotIntrinsicCandidate
    public StringBuffer(String str) {
        super(str.length() + 16);
        append(str);
    }
    
    /**
     * Constructs a string buffer that contains the same characters
     * as the specified {@code CharSequence}. The initial capacity of
     * the string buffer is {@code 16} plus the length of the
     * {@code CharSequence} argument.
     * <p>
     * If the length of the specified {@code CharSequence} is
     * less than or equal to zero, then an empty buffer of capacity
     * {@code 16} is returned.
     *
     * @param seq the sequence to copy.
     *
     * @since 1.5
     */
    public StringBuffer(CharSequence seq) {
        this(seq.length() + 16);
        append(seq);
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 添加 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向StringBuffer末尾添加一个字符序列
    /**
     * Appends the specified {@code CharSequence} to this
     * sequence.
     * <p>
     * The characters of the {@code CharSequence} argument are appended,
     * in order, increasing the length of this sequence by the length of the
     * argument.
     *
     * <p>The result of this method is exactly the same as if it were an
     * invocation of this.append(s, 0, s.length());
     *
     * <p>This method synchronizes on {@code this}, the destination
     * object, but does not synchronize on the source ({@code s}).
     *
     * <p>If {@code s} is {@code null}, then the four characters
     * {@code "null"} are appended.
     *
     * @param s the {@code CharSequence} to append.
     *
     * @return a reference to this object.
     *
     * @since 1.5
     */
    @Override
    public synchronized StringBuffer append(CharSequence s) {
        toStringCache = null;
        super.append(s);
        return this;
    }
    
    // 向StringBuffer末尾添加一个子序列，该子序列取自字符序列s的[start, end)范围
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @since 1.5
     */
    @Override
    public synchronized StringBuffer append(CharSequence s, int start, int end) {
        toStringCache = null;
        super.append(s, start, end);
        return this;
    }
    
    // 向StringBuffer末尾添加一个字符串str
    @Override
    @HotSpotIntrinsicCandidate
    public synchronized StringBuffer append(String str) {
        toStringCache = null;
        super.append(str);
        return this;
    }
    
    // 向StringBuffer末尾添加一个StringBuffer
    /**
     * Appends the specified {@code StringBuffer} to this sequence.
     * <p>
     * The characters of the {@code StringBuffer} argument are appended,
     * in order, to the contents of this {@code StringBuffer}, increasing the
     * length of this {@code StringBuffer} by the length of the argument.
     * If {@code sb} is {@code null}, then the four characters
     * {@code "null"} are appended to this {@code StringBuffer}.
     * <p>
     * Let <i>n</i> be the length of the old character sequence, the one
     * contained in the {@code StringBuffer} just prior to execution of the
     * {@code append} method. Then the character at index <i>k</i> in
     * the new character sequence is equal to the character at index <i>k</i>
     * in the old character sequence, if <i>k</i> is less than <i>n</i>;
     * otherwise, it is equal to the character at index <i>k-n</i> in the
     * argument {@code sb}.
     * <p>
     * This method synchronizes on {@code this}, the destination
     * object, but does not synchronize on the source ({@code sb}).
     *
     * @param sb the {@code StringBuffer} to append.
     *
     * @return a reference to this object.
     *
     * @since 1.4
     */
    public synchronized StringBuffer append(StringBuffer sb) {
        toStringCache = null;
        super.append(sb);
        return this;
    }
    
    // 向StringBuffer末尾添加一个StringBuffer序列
    /**
     * @since 1.8
     */
    @Override
    synchronized StringBuffer append(AbstractStringBuilder asb) {
        toStringCache = null;
        super.append(asb);
        return this;
    }
    
    // 向StringBuffer末尾添加一个字符序列
    @Override
    public synchronized StringBuffer append(char[] str) {
        toStringCache = null;
        super.append(str);
        return this;
    }
    
    // 向StringBuffer末尾添加一个子序列，该子序列取自字符数组s的[offset, offset+len)范围
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public synchronized StringBuffer append(char[] str, int offset, int len) {
        toStringCache = null;
        super.append(str, offset, len);
        return this;
    }
    
    // 向StringBuffer末尾添加一个Object值的字符串序列
    @Override
    public synchronized StringBuffer append(Object obj) {
        toStringCache = null;
        super.append(String.valueOf(obj));
        return this;
    }
    
    // 向StringBuffer末尾添加一个boolean值的字符串序列
    @Override
    public synchronized StringBuffer append(boolean b) {
        toStringCache = null;
        super.append(b);
        return this;
    }
    
    // 向StringBuffer末尾添加一个char值的字符串序列
    @Override
    @HotSpotIntrinsicCandidate
    public synchronized StringBuffer append(char c) {
        toStringCache = null;
        super.append(c);
        return this;
    }
    
    // 向StringBuffer末尾添加一个int值的字符串序列
    @Override
    @HotSpotIntrinsicCandidate
    public synchronized StringBuffer append(int i) {
        toStringCache = null;
        super.append(i);
        return this;
    }
    
    // 向StringBuffer末尾添加一个long值的字符串序列
    @Override
    public synchronized StringBuffer append(long l) {
        toStringCache = null;
        super.append(l);
        return this;
    }
    
    // 向StringBuffer末尾添加一个float值的字符串序列
    @Override
    public synchronized StringBuffer append(float f) {
        toStringCache = null;
        super.append(f);
        return this;
    }
    
    // 向StringBuffer末尾添加一个double值的字符串序列
    @Override
    public synchronized StringBuffer append(double d) {
        toStringCache = null;
        super.append(d);
        return this;
    }
    
    // 向StringBuffer末尾添加一个由Unicode码点值表示的char的字符串序列
    /**
     * @since 1.5
     */
    @Override
    public synchronized StringBuffer appendCodePoint(int codePoint) {
        toStringCache = null;
        super.appendCodePoint(codePoint);
        return this;
    }
    
    /*▲ 添加 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 删除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     * @since 1.2
     */
    // 删除[start, end)范围内的char
    @Override
    public synchronized StringBuffer delete(int start, int end) {
        toStringCache = null;
        super.delete(start, end);
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     * @since 1.2
     */
    // 删除索引为index的char
    @Override
    public synchronized StringBuffer deleteCharAt(int index) {
        toStringCache = null;
        super.deleteCharAt(index);
        return this;
    }
    
    /*▲ 删除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 插入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @since 1.5
     */
    // 向StringBuffer的dstOffset索引处插入一个子序列s
    @Override
    public StringBuffer insert(int dstOffset, CharSequence s) {
        // Note, synchronization achieved via invocations of other StringBuffer methods
        // after narrowing of s to specific type
        // Ditto for toStringCache clearing
        super.insert(dstOffset, s);
        return this;
    }
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @since 1.5
     */
    // 向StringBuffer的dstOffset索引处插入一个子序列，该子序列取自字符序列s的[start, end)范围
    @Override
    public synchronized StringBuffer insert(int dstOffset, CharSequence s, int start, int end) {
        toStringCache = null;
        super.insert(dstOffset, s, start, end);
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 向StringBuffer的offset索引处插入一个子符序列str
    @Override
    public synchronized StringBuffer insert(int offset, char[] str) {
        toStringCache = null;
        super.insert(offset, str);
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     * @since 1.2
     */
    // 向StringBuffer的index索引处插入一个子序列，该子序列取自字符序列str的[offset, offset+len)范围
    @Override
    public synchronized StringBuffer insert(int index, char[] str, int offset, int len) {
        toStringCache = null;
        super.insert(index, str, offset, len);
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 向StringBuffer的offset索引处插入一个字符串str
    @Override
    public synchronized StringBuffer insert(int offset, String str) {
        toStringCache = null;
        super.insert(offset, str);
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 向StringBuffer的offset索引处插入一个Object值的字符串序列
    @Override
    public synchronized StringBuffer insert(int offset, Object obj) {
        toStringCache = null;
        super.insert(offset, String.valueOf(obj));
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 向StringBuffer的offset索引处插入一个boolean值的字符串序列
    @Override
    public StringBuffer insert(int offset, boolean b) {
        // Note, synchronization achieved via invocation of StringBuffer insert(int, String)
        // after conversion of b to String by super class method
        // Ditto for toStringCache clearing
        super.insert(offset, b);
        return this;
    }
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 向StringBuffer的offset索引处插入一个char值的字符串序列
    @Override
    public synchronized StringBuffer insert(int offset, char c) {
        toStringCache = null;
        super.insert(offset, c);
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 向StringBuffer的offset索引处插入一个int值的字符串序列
    @Override
    public StringBuffer insert(int offset, int i) {
        // Note, synchronization achieved via invocation of StringBuffer insert(int, String)
        // after conversion of i to String by super class method
        // Ditto for toStringCache clearing
        super.insert(offset, i);
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 向StringBuffer的offset索引处插入一个long值的字符串序列
    @Override
    public StringBuffer insert(int offset, long l) {
        // Note, synchronization achieved via invocation of StringBuffer insert(int, String)
        // after conversion of l to String by super class method
        // Ditto for toStringCache clearing
        super.insert(offset, l);
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 向StringBuffer的offset索引处插入一个float值的字符串序列
    @Override
    public StringBuffer insert(int offset, float f) {
        // Note, synchronization achieved via invocation of StringBuffer insert(int, String)
        // after conversion of f to String by super class method
        // Ditto for toStringCache clearing
        super.insert(offset, f);
        return this;
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     */
    // 向StringBuffer的offset索引处插入一个double值的字符串序列
    @Override
    public StringBuffer insert(int offset, double d) {
        // Note, synchronization achieved via invocation of StringBuffer insert(int, String)
        // after conversion of d to String by super class method
        // Ditto for toStringCache clearing
        super.insert(offset, d);
        return this;
    }
    
    /*▲ 插入 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 替换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     * @since 1.2
     */
    // 用StringBuffer替换ASB在[start, end)范围内的字符序列
    @Override
    public synchronized StringBuffer replace(int start, int end, String str) {
        toStringCache = null;
        super.replace(start, end, str);
        return this;
    }
    
    /*▲ 替换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 求子串 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     * @since 1.2
     */
    // 求StringBuffer在[start, ∞)范围内的子串
    @Override
    public synchronized String substring(int start) {
        return substring(start, count);
    }
    
    /**
     * @throws StringIndexOutOfBoundsException {@inheritDoc}
     * @since 1.2
     */
    // 求StringBuffer在[start, start+end)范围内的子串
    @Override
    public synchronized String substring(int start, int end) {
        return super.substring(start, end);
    }
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @since 1.4
     */
    // 求StringBuffer在[start, start+end)范围内的子串
    @Override
    public synchronized CharSequence subSequence(int start, int end) {
        return super.substring(start, end);
    }
    
    /*▲ 求子串 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找子串位置 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @since 1.4
     */
    // 返回子串str在当前主串StringBuffer中第一次出现的位置
    @Override
    public int indexOf(String str) {
        // Note, synchronization achieved via invocations of other StringBuffer methods
        return super.indexOf(str);
    }
    
    /**
     * @since 1.4
     */
    // 返回子串str在当前主串StringBuffer中第一次出现的位置（从主串fromIndex处向后搜索）
    @Override
    public synchronized int indexOf(String str, int fromIndex) {
        return super.indexOf(str, fromIndex);
    }
    
    /**
     * @since 1.4
     */
    // 返回子串str在当前主串StringBuffer中最后一次出现的位置
    @Override
    public int lastIndexOf(String str) {
        // Note, synchronization achieved via invocations of other StringBuffer methods
        return lastIndexOf(str, count);
    }
    
    /**
     * @since 1.4
     */
    // 返回子串str在当前主串StringBuffer中最后一次出现的位置（从主串fromIndex处向前搜索）
    @Override
    public synchronized int lastIndexOf(String str, int fromIndex) {
        return super.lastIndexOf(str, fromIndex);
    }
    
    /*▲ 查找子串位置 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 逆置 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @since 1.0.2
     */
    // 逆置StringBuffer
    @Override
    public synchronized StringBuffer reverse() {
        toStringCache = null;
        super.reverse();
        return this;
    }
    
    /*▲ 逆置 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 比较 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Compares two {@code StringBuffer} instances lexicographically. This method
     * follows the same rules for lexicographical comparison as defined in the
     * {@linkplain java.lang.CharSequence#compare(java.lang.CharSequence,
     * java.lang.CharSequence)  CharSequence.compare(this, another)} method.
     *
     * <p>
     * For finer-grained, locale-sensitive String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param another the {@code StringBuffer} to be compared with
     *
     * @return the value {@code 0} if this {@code StringBuffer} contains the same
     * character sequence as that of the argument {@code StringBuffer}; a negative integer
     * if this {@code StringBuffer} is lexicographically less than the
     * {@code StringBuffer} argument; or a positive integer if this {@code StringBuffer}
     * is lexicographically greater than the {@code StringBuffer} argument.
     *
     * @implNote This method synchronizes on {@code this}, the current object, but not
     * {@code StringBuffer another} with which {@code this StringBuffer} is compared.
     * @since 11
     */
    // 比较两个StringBuffer的内容
    @Override
    public synchronized int compareTo(StringBuffer another) {
        return super.compareTo(another);
    }
    
    /*▲ 比较 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取char/char[] ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @see #length()
     */
    // 返回StringBuffer内index索引处的char
    @Override
    public synchronized char charAt(int index) {
        return super.charAt(index);
    }
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 将StringBuffer[srcBegin, srcEnd)内的字节批量转换为char后存入dst
    @Override
    public synchronized void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        super.getChars(srcBegin, srcEnd, dst, dstBegin);
    }
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @see #length()
     */
    // 将ch存入StringBuffer的index索引处
    @Override
    public synchronized void setCharAt(int index, char ch) {
        toStringCache = null;
        super.setCharAt(index, ch);
    }
    
    /*▲ 获取char/char[] ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 码点/码元 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @since 1.5
     */
    // 返回StringBuffer内index索引处的Unicode编码（从前到后试探）
    @Override
    public synchronized int codePointAt(int index) {
        return super.codePointAt(index);
    }
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @since 1.5
     */
    // 返回StringBuffer内index-1索引处的Unicode编码（从后往前试探）
    @Override
    public synchronized int codePointBefore(int index) {
        return super.codePointBefore(index);
    }
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @since 1.5
     */
    // 统计StringBuffer中指定码元范围内存在多少个Unicode符号
    @Override
    public synchronized int codePointCount(int beginIndex, int endIndex) {
        return super.codePointCount(beginIndex, endIndex);
    }
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @since 1.5
     */
    // 返回从index偏移codePointOffset个Unicode符号后新的索引值，codePointOffset的正负决定了偏移方向
    @Override
    public synchronized int offsetByCodePoints(int index, int codePointOffset) {
        return super.offsetByCodePoints(index, codePointOffset);
    }
    
    /*▲ 码点/码元 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 容量 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回当前StringBuffer的容量（可以容纳的char的数量）
    @Override
    public synchronized int capacity() {
        return super.capacity();
    }
    
    // 确保StringBuffer内部拥有最小容量minimumCapacity
    @Override
    public synchronized void ensureCapacity(int minimumCapacity) {
        super.ensureCapacity(minimumCapacity);
    }
    
    /**
     * @since 1.5
     */
    // 缩减StringBuffer容量以恰好容纳其内容
    @Override
    public synchronized void trimToSize() {
        super.trimToSize();
    }
    
    /**
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @see #length()
     */
    // 扩展StringBuffer容量，多出来的部分用0填充，且设置StringBuffer的长度为newLength
    @Override
    public synchronized void setLength(int newLength) {
        toStringCache = null;
        super.setLength(newLength);
    }
    
    /*▲ 容量 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ StringBuffer属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回当前StringBuffe内包含的char的数量
    @Override
    public synchronized int length() {
        return count;
    }
    
    /*▲ StringBuffer属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    static final long serialVersionUID = 3388685877147921107L;
    
    /**
     * Serializable fields for StringBuffer.
     *
     * @serialField value  char[]
     * The backing character array of this StringBuffer.
     * @serialField count int
     * The number of characters in this StringBuffer.
     * @serialField shared  boolean
     * A flag indicating whether the backing array is shared.
     * The value is ignored upon deserialization.
     */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("value", char[].class),
        new ObjectStreamField("count", Integer.TYPE),
        new ObjectStreamField("shared", Boolean.TYPE)
    };
    
    /**
     * readObject is called to restore the state of the StringBuffer from
     * a stream.
     */
    private synchronized void writeObject(ObjectOutputStream s) throws IOException {
        ObjectOutputStream.PutField fields = s.putFields();
        char[] val = new char[capacity()];
        if(isLatin1()) {
            StringLatin1.getChars(value, 0, count, val, 0);
        } else {
            StringUTF16.getChars(value, 0, count, val, 0);
        }
        fields.put("value", val);
        fields.put("count", count);
        fields.put("shared", false);
        s.writeFields();
    }
    
    /**
     * readObject is called to restore the state of the StringBuffer from
     * a stream.
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = s.readFields();
        char[] val = (char[]) fields.get("value", null);
        initBytes(val, 0, val.length);
        count = fields.get("count", 0);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 将StringBuffer的内部字节存入dst
    synchronized void getBytes(byte dst[], int dstBegin, byte coder) {
        super.getBytes(dst, dstBegin, coder);
    }
    
    @Override
    @HotSpotIntrinsicCandidate
    public synchronized String toString() {
        if(toStringCache == null) {
            return toStringCache = isLatin1() ? StringLatin1.newString(value, 0, count) : StringUTF16.newString(value, 0, count);
        }
        return new String(toStringCache);
    }
    
}
