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
import jdk.internal.vm.annotation.Stable;

import java.io.ObjectStreamField;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Native;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The {@code String} class represents character strings. All string literals in Java programs, such as {@code "abc"}, are implemented as instances of this class.
 * <p>
 * Strings are constant; their values cannot be changed after they are created. String buffers support mutable strings. Because String objects are immutable they can be shared. For example:
 * <blockquote><pre>
 *     String str = "abc";
 * </pre></blockquote><p>
 * is equivalent to:
 * <blockquote><pre>
 *     char data[] = {'a', 'b', 'c'};
 *     String str = new String(data);
 * </pre></blockquote><p>
 * Here are some more examples of how strings can be used:
 * <blockquote><pre>
 *     System.out.println("abc");
 *     String cde = "cde";
 *     System.out.println("abc" + cde);
 *     String c = "abc".substring(2,3);
 *     String d = cde.substring(1, 2);
 * </pre></blockquote>
 * <p>
 * The class {@code String} includes methods for examining individual characters of the sequence, for comparing strings, for searching strings, for extracting substrings, and for creating a copy of a string with all characters translated to uppercase or to lowercase. Case mapping is based on the Unicode Standard version specified by the {@link Character Character} class.
 * <p>
 * The Java language provides special support for the string concatenation operator (&nbsp;+&nbsp;), and for conversion of other objects to strings. For additional information on string concatenation and conversion, see <i>The Java&trade; Language Specification</i>.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be thrown.
 *
 * <p>A {@code String} represents a string in the UTF-16 format
 * in which <em>supplementary characters</em> are represented by <em>surrogate pairs</em> (see the section <a href="Character.html#unicode">Unicode Character Representations</a> in the {@code Character} class for more information). Index values refer to {@code char} code units, so a supplementary character uses two positions in a {@code String}.
 * <p>The {@code String} class provides methods for dealing with
 * Unicode code points (i.e., characters), in addition to those for dealing with Unicode code units (i.e., {@code char} values).
 *
 * <p>Unless otherwise noted, methods for comparing Strings do not take locale
 * into account.  The {@link java.text.Collator} class provides methods for finer-grain, locale-sensitive String comparison.
 *
 * @author Lee Boynton
 * @author Arthur van Hoff
 * @author Martin Buchholz
 * @author Ulf Zibis
 * @implNote The implementation of the string concatenation operator is left to the discretion of a Java compiler, as long as the compiler ultimately conforms to <i>The Java&trade; Language Specification</i>. For example, the {@code javac} compiler may implement the operator with {@code StringBuffer}, {@code StringBuilder}, or {@code java.lang.invoke.StringConcatFactory} depending on the JDK version. The implementation of string conversion is typically through the method {@code toString}, defined by {@code Object} and inherited by all classes in Java.
 * @jls 15.18.1 String Concatenation Operator +
 * @see Object#toString()
 * @see StringBuffer
 * @see StringBuilder
 * @see Charset
 * @since 1.0
 */

/*
 * 从JDK9开始，String对象不再以char[]形式存储，而是以名为value的byte[]形式存储。
 *
 * value有一个名为coder的编码标记，该标记有两种取值：LATIN1和UTF-16（UTF-16使用大端法还是小端法取决于系统）。
 *
 * Java中存储String的byte数组的默认编码是LATIN1（即ISO-8859-1）和UTF16。
 *
 * String由一系列Unicode符号组成，根据这些符号的Unicode编码范围[0x0, 0x10FFFF]，将其分为两类：
 *   符号1. 在[0x0, 0xFF]范围内的符号（属于LATIN1/ISO_8859_1字符集范围）
 *   符号2. 在其他范围内的Unicode符号
 * 对于第一类符号，其二进制形式仅用一个byte即可容纳，对于第二类符号，其二进制形式需用两个或四个UTF-16形式的byte存储。
 *
 * 由此，JDK内部将String的存储方式也分为两类：
 *   第一类：String只包含符号1。这种类型的String里，每个符号使用一个byte存储。coder==LATIN1
 *   第二类：String包含第二类符号。这种类型的String里，每个符号使用两个或四个UTF-16形式的byte存储（即使遇到符号1也使用两个byte存储）。coder==UTF16
 *
 * 为了便于后续描述这两类字符串，此处将第一类字符串称为LATIN1-String，将第二类字符串称为UTF16-String。
 *
 * 另注：
 * 鉴于windows中以小端法存储数据，所以存储String的字节数组value也以UTF16小端法显示。
 * 在后续的动态操作中，会将String转换为其他的编码（例如UTF_8、ISO_8859_1、US_ASCII、GBK等）形式。
 * 如果不另指定编码形式，则以JVM的当前默认的字符集为依据去转换String。
 *
 * ★ 关于大端小端：
 * 1.char永远是UTF-16大端
 * 2.String（内置的value）永远取决于系统，在windows上是UTF-16小端
 * 3 String外面的byte[]，大小端取决于当时转换中所用的编码格式
 */
public final class String implements Serializable, Comparable<String>, CharSequence {
    
    /**
     * use serialVersionUID from JDK 1.0.2 for interoperability
     */
    private static final long serialVersionUID = -6849794470754667710L;
    
    /**
     * Class String is special cased within the Serialization Stream Protocol.
     *
     * A String instance is written into an ObjectOutputStream according to
     * <a href="{@docRoot}/../specs/serialization/protocol.html#stream-elements">
     * Object Serialization Specification, Section 6.2, "Stream Elements"</a>
     */
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0];
    
    /**
     * A Comparator that orders {@code String} objects as by {@code compareToIgnoreCase}. This comparator is serializable.
     * <p>
     * Note that this Comparator does <em>not</em> take locale into account, and will result in an unsatisfactory ordering for certain locales.
     * The {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @see java.text.Collator
     * @since 1.2
     */
    public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();
    
    /**
     * If String compaction is disabled, the bytes in {@code value} are always encoded in UTF16.
     * For methods with several possible implementation paths, when String compaction is disabled, only one code path is taken.
     *
     * The instance field value is generally opaque to optimizing JIT compilers.
     * Therefore, in performance-sensitive place, an explicit check of the static boolean {@code COMPACT_STRINGS} is done
     * first before checking the {@code coder} field since the static boolean {@code COMPACT_STRINGS}
     * would be constant folded away by an optimizing JIT compiler.
     *
     * The idioms for these cases are as follows.
     *
     * For code such as:
     *
     * if (coder == LATIN1) { ... }
     *
     * can be written more optimally as
     *
     * if (coder() == LATIN1) { ... }
     *
     * or:
     *
     * if (COMPACT_STRINGS && coder == LATIN1) { ... }
     *
     * An optimizing JIT compiler can fold the above conditional as:
     *
     * COMPACT_STRINGS == true  => if (coder == LATIN1) { ... }
     * COMPACT_STRINGS == false => if (false)           { ... }
     *
     * @implNote The actual value for this field is injected by JVM.
     * The static initialization block is used to set the value here to communicate that this static final field is not statically foldable,
     * and to avoid any possible circular dependency during vm initialization.
     */
    // 如果禁用字符串压缩，则其字符始终以UTF-16编码，默认设置为true
    static final boolean COMPACT_STRINGS;
    
    /*
     * Latin1是ISO-8859-1的别名，有些环境下写作Latin-1。ISO-8859-1编码是单字节编码，向下兼容ASCII。
     * 其编码范围是0x00-0xFF，0x00-0x7F之间完全和ASCII一致，0x80-0x9F之间是控制字符，0xA0-0xFF之间是文字符号。
     */
    @Native
    static final byte LATIN1 = 0;
    @Native
    static final byte UTF16 = 1;
    
    /**
     * The value is used for character storage.
     *
     * @implNote This field is trusted by the VM, and is a subject to constant folding if String instance is constant.
     * Overwriting this field after construction will cause problems.
     *
     * Additionally, it is marked with {@link Stable} to trust the contents of the array.
     * No other facility in JDK provides this functionality (yet).
     * {@link Stable} is safe here, because value is never null.
     */
    /*
     * 以字节形式存储String中的char，即存储码元
     *
     * 如果是纯英文字符，则采用压缩存储，一个byte代表一个char。
     * 出现汉字等符号后，汉字可占多个byte，且一个英文字符也将占有2个byte。
     *
     * windows上使用小端法存字符串。
     * 如果输入是：String s = "\u56DB\u6761\uD869\uDEA5"; // "四条𪚥"，"𪚥"在UTF16中占4个字节
     * 则value中存储（十六进制）：[DB, 56, 61, 67, 69, D8, A5, DE]
     */
    @Stable
    private final byte[] value;
    
    /**
     * The identifier of the encoding used to encode the bytes in {@code value}. The supported values in this implementation are LATIN1 and UTF16。
     *
     * @implNote This field is trusted by the VM, and is a subject to constant folding if String instance is constant.
     * Overwriting this field after construction will cause problems.
     */
    // 当前字符串的编码：LATIN1(0)或UTF16(1)
    private final byte coder;
    
    /**
     * Cache the hash code for the string
     */
    // 当前字符串哈希码，初始值默认为0
    private int hash; // Default to 0
    
    
    static {
        /*
         * 默认情形下，虚拟机会开启“紧凑字符串”选项，即令COMPACT_STRINGS = true。
         * 可以在虚拟机参数上设置-XX:-CompactStrings来关闭“紧凑字符串”选项。
         * 如果COMPACT_STRINGS == true，则String会有LATIN1或UTF16两种存储形式。否则，只使用UTF16形式。
         */
        COMPACT_STRINGS = true;
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Initializes a newly created {@code String} object so that it represents an empty character sequence.
     * Note that use of this constructor is unnecessary since Strings are immutable.
     */
    // ▶ 0 构造空串
    public String() {
        this.value = "".value;
        this.coder = "".coder;
    }
    
    /**
     * Package private constructor which shares value array for speed.
     */
    // ▶ 1 构造包含指定字节序列和字符串编码的String
    String(byte[] value, byte coder) {
        this.value = value;
        this.coder = coder;
    }
    
    /**
     * Initializes a newly created {@code String} object so that it represents the same sequence of characters as the argument;
     * in other words, the newly created string is a copy of the argument string. Unless an explicit copy of {@code original} is needed,
     * use of this constructor is unnecessary since Strings are immutable.
     *
     * @param original A {@code String}
     */
    // ▶ 2 构造String的副本（哈希值都一样）
    @HotSpotIntrinsicCandidate
    public String(String original) {
        this.value = original.value;
        this.coder = original.coder;
        this.hash = original.hash;
    }
    
    /**
     * Allocates a new string that contains the sequence of characters currently contained in the string buffer argument.
     * The contents of the string buffer are copied; subsequent modification of the string buffer does not affect the newly created string.
     *
     * @param buffer A {@code StringBuffer}
     */
    // ▶ 2-1 构造与buffer内容完全一致的字符串（哈希值都一样）
    public String(StringBuffer buffer) {
        this(buffer.toString());
    }
    
    /**
     * Package private constructor.
     * Trailing Void argument is there for disambiguating it against other (public) constructors.
     *
     * Stores the char[] value into a byte[] that each byte represents the8 low-order bits of the corresponding character,
     * if the char[] contains only latin1 character.
     * Or a byte[] that stores all characters in their byte sequences defined by the {@code StringUTF16}.
     */
    // ▶ 3 将指定范围的char序列打包成String。参数sig仅用作占位，以消除与构造器<3-2>的歧义
    String(char[] value, int off, int len, Void sig) {
        // 空串
        if(len == 0) {
            this.value = "".value;
            this.coder = "".coder;
            return;
        }
        
        // 允许对字符串压缩存储
        if(COMPACT_STRINGS) {
            // 将UTF16-String内部的字节转换为LATIN1-String内部的字节
            byte[] val = StringUTF16.compress(value, off, len);
            if(val != null) {
                this.value = val;
                this.coder = LATIN1;
                return;
            }
        }
        
        this.coder = UTF16;
        // 将value中的char批量转换为UTF16-String内部的字节，并返回
        this.value = StringUTF16.toBytes(value, off, len);
    }
    
    /**
     * Allocates a new {@code String} so that it represents the sequence of characters currently contained in the character array argument.
     * The contents of the character array are copied; subsequent modification of the character array does not affect the newly created string.
     *
     * @param value The initial value of the string
     */
    // ▶ 3-1 将指定的char序列打包成String。
    public String(char value[]) {
        this(value, 0, value.length, null);
    }
    
    /**
     * Allocates a new {@code String} that contains characters from a subarray of the character array argument.
     * The {@code offset} argument is the index of the first character of the subarray and the {@code count} argument specifies the length of the subarray.
     * The contents of the subarray are copied; subsequent modification of the character array does not affect the newly created string.
     *
     * @param value  Array that is the source of characters
     * @param offset The initial offset
     * @param count  The length
     *
     * @throws IndexOutOfBoundsException If {@code offset} is negative, {@code count} is negative, or {@code offset} is greater than {@code value.length - count}
     */
    // ▶ 3-2 将指定范围的char序列打包成String。加入越界检查
    public String(char value[], int offset, int count) {
        this(value, offset, count, rangeCheck(value, offset, count));
    }
    
    /**
     * Constructs a new {@code String} by decoding the specified subarray of bytes using the platform's default charset.
     * The length of the new {@code String} is a function of the charset, and hence may not be equal to the length of the subarray.
     *
     * The behavior of this constructor when the given bytes are not valid in the default charset is unspecified.
     * The {@link java.nio.charset.CharsetDecoder} class should be used when more control over the decoding process is required.
     *
     * @param bytes  The bytes to be decoded into characters
     * @param offset The index of the first byte to decode
     * @param length The number of bytes to decode
     *
     * @throws IndexOutOfBoundsException If {@code offset} is negative, {@code length} is negative,
     * or {@code offset} is greater than {@code bytes.length - length}
     * @since 1.1
     */
    // ▶ 4 按JVM默认字符集格式解码指定范围的字节序列，进而构造String
    public String(byte bytes[], int offset, int length) {
        checkBoundsOffCount(offset, length, bytes.length);
        // 以JVM默认字符集格式解码byte[]，返回结果集
        StringCoding.Result ret = StringCoding.decode(bytes, offset, length);
        this.value = ret.value;
        this.coder = ret.coder;
    }
    
    /**
     * Constructs a new {@code String} by decoding the specified array of bytes using the platform's default charset.
     * The length of the new {@code String} is a function of the charset, and hence may not be equal to the length of the byte array.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the default charset is unspecified.
     * The {@link java.nio.charset.CharsetDecoder} class should be used when more control over the decoding process is required.
     *
     * @param bytes The bytes to be decoded into characters
     *
     * @since 1.1
     */
    // ▶ 4-1 按JVM默认字符集格式解码指定的字节序列，进而构造String
    public String(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }
    
    /**
     * Constructs a new {@code String} by decoding the specified subarray of bytes using the specified charset.
     * The length of the new {@code String} is a function of the charset, and hence may not be equal to the length of the subarray.
     *
     * <p> The behavior of this constructor when the given bytes are not valid in the given charset is unspecified.
     * The {@link java.nio.charset.CharsetDecoder} class should be used when more control over the decoding process is required.
     *
     * @param bytes       The bytes to be decoded into characters
     * @param offset      The index of the first byte to decode
     * @param length      The number of bytes to decode
     * @param charsetName The name of a supported {@linkplain Charset charset}
     *
     * @throws UnsupportedEncodingException If the named charset is not supported
     * @throws IndexOutOfBoundsException    If {@code offset} is negative, {@code length} is negative,
     * or {@code offset} is greater than {@code bytes.length - length}
     * @since 1.1
     */
    // ▶ 5 按charsetName格式解码指定范围的字节序列，进而构造String
    public String(byte bytes[], int offset, int length, String charsetName) throws UnsupportedEncodingException {
        if(charsetName == null)
            throw new NullPointerException("charsetName");
        checkBoundsOffCount(offset, length, bytes.length);
        // 以charsetName格式解析byte[]，返回结果集
        StringCoding.Result ret = StringCoding.decode(charsetName, bytes, offset, length);
        this.value = ret.value;
        this.coder = ret.coder;
    }
    
    /**
     * Constructs a new {@code String} by decoding the specified array of bytes using the specified {@linkplain Charset charset}.
     * The length of the new {@code String} is a function of the charset, and hence may not be equal to the length of the byte array.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the given charset is unspecified.
     * The {@link java.nio.charset.CharsetDecoder} class should be used when more control over the decoding process is required.
     *
     * @param bytes       The bytes to be decoded into characters
     * @param charsetName The name of a supported {@linkplain Charset charset}
     *
     * @throws UnsupportedEncodingException If the named charset is not supported
     * @since 1.1
     */
    // ▶ 5-1 按charsetName格式解码指定的字节序列，进而构造String
    public String(byte bytes[], String charsetName) throws UnsupportedEncodingException {
        this(bytes, 0, bytes.length, charsetName);
    }
    
    /**
     * Constructs a new {@code String} by decoding the specified subarray of bytes using the specified {@linkplain Charset charset}.
     * The length of the new {@code String} is a function of the charset, and hence may not be equal to the length of the subarray.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.
     * The {@link java.nio.charset.CharsetDecoder} class should be used when more control over the decoding process is required.
     *
     * @param bytes   The bytes to be decoded into characters
     * @param offset  The index of the first byte to decode
     * @param length  The number of bytes to decode
     * @param charset The {@linkplain Charset charset} to be used to decode the {@code bytes}
     *
     * @throws IndexOutOfBoundsException If {@code offset} is negative, {@code length} is negative,
     * or {@code offset} is greater than {@code bytes.length - length}
     * @since 1.6
     */
    // ▶ 6 按charset格式解码解码指定范围的字节序列，返回解码后的字符序列
    public String(byte bytes[], int offset, int length, Charset charset) {
        if(charset == null)
            throw new NullPointerException("charset");
        checkBoundsOffCount(offset, length, bytes.length);
        // 以charset格式解码byte[]，返回结果集
        StringCoding.Result ret = StringCoding.decode(charset, bytes, offset, length);
        this.value = ret.value;
        this.coder = ret.coder;
    }
    
    /**
     * Constructs a new {@code String} by decoding the specified array of bytes using the specified {@linkplain Charset charset}.
     * The length of the new {@code String} is a function of the charset, and hence may not be equal to the length of the byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.
     * The {@link java.nio.charset.CharsetDecoder} class should be used when more control over the decoding process is required.
     *
     * @param bytes   The bytes to be decoded into characters
     * @param charset The {@linkplain Charset charset} to be used to decode the {@code bytes}
     *
     * @since 1.6
     */
    // ▶ 6-1 按charset格式解码指定的字节序列，返回解码后的字符序列
    public String(byte bytes[], Charset charset) {
        this(bytes, 0, bytes.length, charset);
    }
    
    /**
     * Package private constructor. Trailing Void argument is there for
     * disambiguating it against other (public) constructors.
     */
    // ▶ 7 按照字符序列asb内部的字节序列构造String。参数sig仅用作占位，以消除与构造器<7-1>的歧义
    String(AbstractStringBuilder asb, Void sig) {
        byte[] val = asb.getValue();
        int length = asb.length();
        if(asb.isLatin1()) {
            this.coder = LATIN1;
            this.value = Arrays.copyOfRange(val, 0, length);
        } else {
            if(COMPACT_STRINGS) {
                // 将UTF16-String内部的字节转换为LATIN1-String内部的字节后，再返回
                byte[] buf = StringUTF16.compress(val, 0, length);
                if(buf != null) {
                    this.coder = LATIN1;
                    this.value = buf;
                    return;
                }
            }
            this.coder = UTF16;
            this.value = Arrays.copyOfRange(val, 0, length << 1);
        }
    }
    
    /**
     * Allocates a new string that contains the sequence of characters currently contained in the string builder argument.
     * The contents of the string builder are copied; subsequent modification of the string builder does not affect the newly created string.
     *
     * <p> This constructor is provided to ease migration to {@code
     * StringBuilder}. Obtaining a string from a string builder via the {@code toString} method is likely to run faster and is generally preferred.
     *
     * @param builder A {@code StringBuilder}
     *
     * @since 1.5
     */
    // ▶ 7-1 按照字符序列builder内部的字节序列构造String
    public String(StringBuilder builder) {
        this(builder, null);
    }
    
    /**
     * Allocates a new {@code String} that contains characters from a subarray of the <a href="Character.html#unicode">Unicode code point</a> array argument.
     * The {@code offset} argument is the index of the first code point of the subarray and the {@code count} argument specifies the length of the subarray.
     * The contents of the subarray are converted to {@code char}s; subsequent modification of the {@code int} array does not affect the newly created string.
     *
     * @param codePoints Array that is the source of Unicode code points
     * @param offset     The initial offset
     * @param count      The length
     *
     * @throws IllegalArgumentException  If any invalid Unicode code point is found in {@code codePoints}
     * @throws IndexOutOfBoundsException If {@code offset} is negative, {@code count} is negative,
     * or {@code offset} is greater than {@code codePoints.length - count}
     * @since 1.5
     */
    // ▶ 8 将codePoints中的一组Unicode值转换为UTF16编码值，再以字节形式存入String(大小端由系统环境决定)
    public String(int[] codePoints, int offset, int count) {
        // 范围检查
        checkBoundsOffCount(offset, count, codePoints.length);
        // 空串
        if(count == 0) {
            this.value = "".value;
            this.coder = "".coder;
            return;
        }
        // 可压缩的字符串
        if(COMPACT_STRINGS) {
            // 将codePoints中的一组Unicode值批量转换为LATIN1-String内部的字节，再返回
            byte[] val = StringLatin1.toBytes(codePoints, offset, count);
            if(val != null) {
                this.coder = LATIN1;
                this.value = val;
                return;
            }
        }
        this.coder = UTF16;
        // 将codePoints中的一组Unicode值批量转换为UTF16-String内部的字节，再返回
        this.value = StringUTF16.toBytes(codePoints, offset, count);
    }
    
    /**
     * Allocates a new {@code String} constructed from a subarray of an array of 8-bit integer values.
     *
     * <p> The {@code offset} argument is the index of the first byte of the
     * subarray, and the {@code count} argument specifies the length of the subarray.
     *
     * <p> Each {@code byte} in the subarray is converted to a {@code char} as
     * specified in the {@link #String(byte[], int) String(byte[],int)} constructor.
     *
     * @param ascii  The bytes to be converted to characters
     * @param hibyte The top 8 bits of each 16-bit Unicode code unit
     * @param offset The initial offset
     * @param count  The length
     *
     * @throws IndexOutOfBoundsException If {@code offset} is negative, {@code count} is negative,
     * or {@code offset} is greater than {@code ascii.length - count}
     * @see #String(byte[], int)
     * @see #String(byte[], int, int, String)
     * @see #String(byte[], int, int, Charset)
     * @see #String(byte[], int, int)
     * @see #String(byte[], String)
     * @see #String(byte[], Charset)
     * @see #String(byte[])
     *
     * @deprecated This method does not properly convert bytes into characters.
     * As of JDK&nbsp;1.1, the preferred way to do this is via the {@code String} constructors that take a {@link Charset},
     * charset name, or that use the platform's default charset.
     */
    // ▶ 9 ※ 过时
    @Deprecated(since = "1.1")
    public String(byte ascii[], int hibyte, int offset, int count) {
        checkBoundsOffCount(offset, count, ascii.length);
        if(count == 0) {
            this.value = "".value;
            this.coder = "".coder;
            return;
        }
        if(COMPACT_STRINGS && (byte) hibyte == 0) {
            this.value = Arrays.copyOfRange(ascii, offset, offset + count);
            this.coder = LATIN1;
        } else {
            hibyte <<= 8;
            // 创建长度为2*len的字节数组
            byte[] val = StringUTF16.newBytesFor(count);
            for(int i = 0; i < count; i++) {
                // 将形参三的两个低字节转换为UTF16-String内部的字节，存入val
                StringUTF16.putChar(val, i, hibyte | (ascii[offset++] & 0xff));
            }
            this.value = val;
            this.coder = UTF16;
        }
    }
    
    /**
     * Allocates a new {@code String} containing characters constructed from an array of 8-bit integer values.
     * Each character <i>c</i> in the resulting string is constructed from the corresponding component
     * <i>b</i> in the byte array such that:
     *
     * <blockquote><pre>
     *     <b><i>c</i></b> == (char)(((hibyte &amp; 0xff) &lt;&lt; 8)
     *                         | (<b><i>b</i></b> &amp; 0xff))
     * </pre></blockquote>
     *
     * @param ascii  The bytes to be converted to characters
     * @param hibyte The top 8 bits of each 16-bit Unicode code unit
     *
     * @see #String(byte[], int, int, String)
     * @see #String(byte[], int, int, Charset)
     * @see #String(byte[], int, int)
     * @see #String(byte[], String)
     * @see #String(byte[], Charset)
     * @see #String(byte[])
     * @deprecated This method does not properly convert bytes into characters.
     * As of JDK&nbsp;1.1, the preferred way to do this is via the {@code String} constructors that take a {@link Charset},
     * charset name, or that use the platform's default charset.
     */
    // ▶ 9-1 ※ 过时
    @Deprecated(since = "1.1")
    public String(byte ascii[], int hibyte) {
        this(ascii, hibyte, 0, ascii.length);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取byte/byte[] ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Encodes this {@code String} into a sequence of bytes using the platform's default charset, storing the result into a new byte array.
     *
     * <p> The behavior of this method when this string cannot be encoded in the default charset is unspecified.
     * The {@link java.nio.charset.CharsetEncoder} class should be used when more control over the encoding process is required.
     *
     * @return The resultant byte array
     *
     * @since 1.1
     */
    // 编码String，返回JVM默认字符集格式的byte[]
    public byte[] getBytes() {
        // coder指示了String是LATIN1-String还是UTF16-string。
        return StringCoding.encode(coder(), value);
    }
    
    /**
     * Encodes this {@code String} into a sequence of bytes using the named charset, storing the result into a new byte array.
     *
     * <p> The behavior of this method when this string cannot be encoded in the given charset is unspecified.
     * The {@link java.nio.charset.CharsetEncoder} class should be used when more control over the encoding process is required.
     *
     * @param charsetName The name of a supported {@linkplain Charset charset}
     *
     * @return The resultant byte array
     *
     * @throws UnsupportedEncodingException If the named charset is not supported
     * @since 1.1
     */
    // 编码String，返回charsetName字符集格式的byte[]
    public byte[] getBytes(String charsetName) throws UnsupportedEncodingException {
        if(charsetName == null)
            throw new NullPointerException();
        // coder指示了String是LATIN1-String还是UTF16-string。
        return StringCoding.encode(charsetName, coder(), value);
    }
    
    /**
     * Encodes this {@code String} into a sequence of bytes using the given {@linkplain Charset charset}, storing the result into a new byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character sequences with this charset's default replacement byte array.
     * The {@link java.nio.charset.CharsetEncoder} class should be used when more control over the encoding process is required.
     *
     * @param charset The {@linkplain Charset} to be used to encode the {@code String}
     *
     * @return The resultant byte array
     *
     * @since 1.6
     */
    // 编码String，返回charset字符集格式的byte[]
    public byte[] getBytes(Charset charset) {
        if(charset == null)
            throw new NullPointerException();
        // coder指示了String是LATIN1-String还是UTF16-string。
        return StringCoding.encode(charset, coder(), value);
    }
    
    /**
     * Copies characters from this string into the destination byte array.
     * Each byte receives the 8 low-order bits of the corresponding character.
     * The eight high-order bits of each character are not copied and do not participate in the transfer in any way.
     *
     * The first character to be copied is at index {@code srcBegin}; the last character to be copied is at index {@code srcEnd-1}.
     * The total number of characters to be copied is {@code srcEnd-srcBegin}.
     * The characters, converted to bytes, are copied into the subarray of {@code dst} starting at index {@code dstBegin} and ending at index:
     *
     * <blockquote><pre>
     *     dstBegin + (srcEnd-srcBegin) - 1
     * </pre></blockquote>
     *
     * @param srcBegin Index of the first character in the string to copy
     * @param srcEnd   Index after the last character in the string to copy
     * @param dst      The destination array
     * @param dstBegin The start offset in the destination array
     *
     * @throws IndexOutOfBoundsException If any of the following is true:
     *                                   <ul>
     *                                   <li> {@code srcBegin} is negative
     *                                   <li> {@code srcBegin} is greater than {@code srcEnd}
     *                                   <li> {@code srcEnd} is greater than the length of this String
     *                                   <li> {@code dstBegin} is negative
     *                                   <li> {@code dstBegin+(srcEnd-srcBegin)} is larger than {@code
     *                                   dst.length}
     *                                   </ul>
     * @deprecated This method does not properly convert characters into bytes.
     * As of JDK&nbsp;1.1, the preferred way to do this is via the {@link #getBytes()} method, which uses the platform's default charset.
     *
     * ※ 已过时，设计有缺陷
     *
     * 编码String，只保留原char中的低byte。
     * 这意味着，只能正确处理[0x00, 0xFF]范围内的字符，对于超出范围的字节，则将其抛弃。
     *
     * 例如：
     * byte[] value = new byte[4]{0x12,0x34, 0x56,0x78};
     * byte[] dst = new byte[4];
     * s.getBytes(value, 0, 2, dst, 0);  // 字节数组dst：[34, 78]
     *
     * 只有原字节对表示的char在[0x00, 0xFF]范围内，才能得到一个正确的压缩
     */
    // ※ 已过时，设计有缺陷
    @Deprecated(since = "1.1")
    public void getBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        checkBoundsBeginEnd(srcBegin, srcEnd, length());
        Objects.requireNonNull(dst);
        checkBoundsOffCount(dstBegin, srcEnd - srcBegin, dst.length);
        
        if(isLatin1()) {
            // 将LATIN1-String内部的字节转换为LATIN1-String内部的字节
            StringLatin1.getBytes(value, srcBegin, srcEnd, dst, dstBegin);
        } else {
            // 将UTF16-String内部的字节转换为LATIN1-String内部的字节
            StringUTF16.getBytes(value, srcBegin, srcEnd, dst, dstBegin);
        }
    }
    
    /*▲ 获取byte/byte[] ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取char/char[] ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the {@code char} value at the specified index.
     * An index ranges from {@code 0} to {@code length() - 1}.
     * The first {@code char} value of the sequence is at index {@code 0}, the next at index {@code 1}, and so on, as for array indexing.
     *
     * If the {@code char} value specified by the index is a <a href="Character.html#unicode">surrogate</a>, the surrogate value is returned.
     *
     * @param index the index of the {@code char} value.
     *
     * @return the {@code char} value at the specified index of this string. The first {@code char} value is at index {@code 0}.
     *
     * @throws IndexOutOfBoundsException if the {@code index} argument is negative or not less than the length of this string.
     */
    // 将String内部的字节转换为char后返回
    public char charAt(int index) {
        // 可以用压缩的Latin1字符集表示
        if(isLatin1()) {
            // 将LATIN1-String内部的字节转换为char后返回
            return StringLatin1.charAt(value, index);
        } else {
            // 将UTF16-String内部的字节转换为char后返回
            return StringUTF16.charAt(value, index);
        }
    }
    
    /**
     * Copies characters from this string into the destination character array.
     * <p>
     * The first character to be copied is at index {@code srcBegin}; the last character to be copied is at index {@code srcEnd-1}
     * (thus the total number of characters to be copied is {@code srcEnd-srcBegin}).
     * The characters are copied into the subarray of {@code dst} starting at index {@code dstBegin} and ending at index:
     * <blockquote><pre>
     *     dstBegin + (srcEnd-srcBegin) - 1
     * </pre></blockquote>
     *
     * @param srcBegin index of the first character in the string to copy.
     * @param srcEnd   index after the last character in the string to copy.
     * @param dst      the destination array.
     * @param dstBegin the start offset in the destination array.
     *
     * @throws IndexOutOfBoundsException If any of the following is true:
     *                                   <ul><li>{@code srcBegin} is negative.
     *                                   <li>{@code srcBegin} is greater than {@code srcEnd}
     *                                   <li>{@code srcEnd} is greater than the length of this
     *                                   string
     *                                   <li>{@code dstBegin} is negative
     *                                   <li>{@code dstBegin+(srcEnd-srcBegin)} is larger than
     *                                   {@code dst.length}</ul>
     */
    // 将String内部的字节批量转换为char后存入dst
    public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
        checkBoundsBeginEnd(srcBegin, srcEnd, length());
        checkBoundsOffCount(dstBegin, srcEnd - srcBegin, dst.length);
        // 可以用压缩的Latin1字符集表示
        if(isLatin1()) {
            // 将LATIN1-String内部的字节批量转换为char后存入dst
            StringLatin1.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        } else {
            // 将UTF16-String内部的字节批量转换为char后存入dst
            StringUTF16.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        }
    }
    
    /**
     * Converts this string to a new character array.
     *
     * @return a newly allocated character array whose length is the length of this string
     * and whose contents are initialized to contain the character sequence represented by this string.
     */
    // 将当前字符串的存储形式转换为char[]
    public char[] toCharArray() {
        return isLatin1()
            ? StringLatin1.toChars(value)   // 将LATIN1-String内部的字节全部转换为char后返回
            : StringUTF16.toChars(value);   // 将UTF16-String内部的字节全部转换为char后返回
    }
    
    /*▲ 获取char/char[] ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字符串化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the string representation of the {@code boolean} argument.
     *
     * @param b a {@code boolean}.
     *
     * @return if the argument is {@code true}, a string equal to {@code "true"} is returned;
     * otherwise, a string equal to {@code "false"} is returned.
     */
    public static String valueOf(boolean b) {
        return b ? "true" : "false";
    }
    
    /**
     * Returns the string representation of the {@code char} argument.
     *
     * @param c a {@code char}.
     *
     * @return a string of length {@code 1} containing as its single character the argument {@code c}.
     */
    public static String valueOf(char c) {
        // 将char转换为LATIN1-String内部的字节，并返回
        if(COMPACT_STRINGS && StringLatin1.canEncode(c)) {
            return new String(StringLatin1.toBytes(c), LATIN1);
        }
        
        // 将char转换为UTF16-String内部的字节，并返回
        return new String(StringUTF16.toBytes(c), UTF16);
    }
    
    /**
     * Returns the string representation of the {@code int} argument.
     * <p>
     * The representation is exactly the one returned by the {@code Integer.toString} method of one argument.
     *
     * @param i an {@code int}.
     *
     * @return a string representation of the {@code int} argument.
     *
     * @see Integer#toString(int, int)
     */
    public static String valueOf(int i) {
        return Integer.toString(i);
    }
    
    /**
     * Returns the string representation of the {@code long} argument.
     * <p>
     * The representation is exactly the one returned by the {@code Long.toString} method of one argument.
     *
     * @param l a {@code long}.
     *
     * @return a string representation of the {@code long} argument.
     *
     * @see Long#toString(long)
     */
    public static String valueOf(long l) {
        return Long.toString(l);
    }
    
    /**
     * Returns the string representation of the {@code float} argument.
     * <p>
     * The representation is exactly the one returned by the {@code Float.toString} method of one argument.
     *
     * @param f a {@code float}.
     *
     * @return a string representation of the {@code float} argument.
     *
     * @see Float#toString(float)
     */
    public static String valueOf(float f) {
        return Float.toString(f);
    }
    
    /**
     * Returns the string representation of the {@code double} argument.
     * <p>
     * The representation is exactly the one returned by the {@code Double.toString} method of one argument.
     *
     * @param d a {@code double}.
     *
     * @return a  string representation of the {@code double} argument.
     *
     * @see Double#toString(double)
     */
    public static String valueOf(double d) {
        return Double.toString(d);
    }
    
    /**
     * Returns the string representation of the {@code Object} argument.
     *
     * @param obj an {@code Object}.
     *
     * @return if the argument is {@code null}, then a string equal to {@code "null"};
     * otherwise, the value of {@code obj.toString()} is returned.
     *
     * @see Object#toString()
     */
    public static String valueOf(Object obj) {
        return (obj == null) ? "null" : obj.toString();
    }
    
    
    /**
     * Returns the string representation of the {@code char} array argument.
     * The contents of the character array are copied;
     * subsequent modification of the character array does not affect the returned string.
     *
     * @param data the character array.
     *
     * @return a {@code String} that contains the characters of the character array.
     */
    public static String valueOf(char data[]) {
        return new String(data);
    }
    
    /**
     * Returns the string representation of a specific subarray of the {@code char} array argument.
     * <p>
     * The {@code offset} argument is the index of the first character of the subarray.
     * The {@code count} argument specifies the length of the subarray.
     * The contents of the subarray are copied; subsequent modification of the character array does not affect the returned string.
     *
     * @param data   the character array.
     * @param offset initial offset of the subarray.
     * @param count  length of the subarray.
     *
     * @return a {@code String} that contains the characters of the specified subarray of the character array.
     *
     * @throws IndexOutOfBoundsException if {@code offset} is negative,
     *                                   or {@code count} is negative, or {@code offset+count} is larger than {@code data.length}.
     */
    public static String valueOf(char data[], int offset, int count) {
        return new String(data, offset, count);
    }
    
    
    /**
     * Equivalent to {@link #valueOf(char[])}.
     *
     * @param data the character array.
     *
     * @return a {@code String} that contains the characters of the character array.
     */
    public static String copyValueOf(char data[]) {
        return new String(data);
    }
    
    /**
     * Equivalent to {@link #valueOf(char[], int, int)}.
     *
     * @param data   the character array.
     * @param offset initial offset of the subarray.
     * @param count  length of the subarray.
     *
     * @return a {@code String} that contains the characters of the specified subarray of the character array.
     *
     * @throws IndexOutOfBoundsException if {@code offset} is negative,
     *                                   or {@code count} is negative, or {@code offset+count} is larger than {@code data.length}.
     */
    public static String copyValueOf(char data[], int offset, int count) {
        return new String(data, offset, count);
    }
    
    /*▲ 字符串化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找子串下标 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the index within this string of the first occurrence of the specified substring.
     *
     * <p>The returned index is the smallest value {@code k} for which:
     * <pre>{@code
     * this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param str the substring to search for.
     *
     * @return the index of the first occurrence of the specified substring, or {@code -1} if there is no such occurrence.
     */
    // 返回子串str在主串src中第一次出现的下标
    public int indexOf(String str) {
        if(coder() == str.coder()) {
            return isLatin1() ? StringLatin1.indexOf(value, str.value) : StringUTF16.indexOf(value, str.value);
        }
        if(coder() == LATIN1) {  // str.coder == UTF16
            return -1;
        }
        return StringUTF16.indexOfLatin1(value, str.value);
    }
    
    /**
     * Returns the index within this string of the first occurrence of the specified substring, starting at the specified index.
     *
     * <p>The returned index is the smallest value {@code k} for which:
     * <pre>{@code
     *     k >= Math.min(fromIndex, this.length()) &&
     *                   this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param str       the substring to search for.
     * @param fromIndex the index from which to start the search.
     *
     * @return the index of the first occurrence of the specified substring,
     * starting at the specified index, or {@code -1} if there is no such occurrence.
     */
    // 返回子串str在当前主串String中第一次出现的下标（从主串fromIndex处向后搜索）
    public int indexOf(String str, int fromIndex) {
        return indexOf(value, coder(), length(), str, fromIndex);
    }
    
    /**
     * Returns the index within this string of the last occurrence of the specified substring.
     * The last occurrence of the empty string "" is considered to occur at the index value {@code this.length()}.
     *
     * <p>The returned index is the largest value {@code k} for which:
     * <pre>{@code
     * this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param str the substring to search for.
     *
     * @return the index of the last occurrence of the specified substring, or {@code -1} if there is no such occurrence.
     */
    // 返回从某处开始，子串str在主串src中最后一次出现的下标
    public int lastIndexOf(String str) {
        return lastIndexOf(str, length());
    }
    
    /**
     * Returns the index within this string of the last occurrence of the specified substring, searching backward starting at the specified index.
     *
     * <p>The returned index is the largest value {@code k} for which:
     * <pre>{@code
     *     k <= Math.min(fromIndex, this.length()) &&
     *                   this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param str       the substring to search for.
     * @param fromIndex the index to start the search from.
     *
     * @return the index of the last occurrence of the specified substring,
     * searching backward from the specified index, or {@code -1} if there is no such occurrence.
     */
    // 返回子串str在当前主串String中最后一次出现的下标（从主串fromIndex处向前搜索）
    public int lastIndexOf(String str, int fromIndex) {
        return lastIndexOf(value, coder(), length(), str, fromIndex);
    }
    
    /*▲ 查找子串下标 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找Unicode符号下标 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the index within this string of the first occurrence of the specified character.
     * If a character with value {@code ch} occurs in the character sequence represented by this {@code String} object,
     * then the index (in Unicode code units) of the first such occurrence is returned.
     * For values of {@code ch} in the range from 0 to 0xFFFF (inclusive), this is the smallest value <i>k</i> such that:
     * <blockquote><pre>
     * this.charAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the smallest value <i>k</i> such that:
     * <blockquote><pre>
     * this.codePointAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true. In either case, if no such character occurs in this string, then {@code -1} is returned.
     *
     * @param ch a character (Unicode code point).
     *
     * @return the index of the first occurrence of the character in the character sequence represented by this object,
     * or {@code -1} if the character does not occur.
     */
    // 从串首向后搜索，返回在当前[字符序列]中首次遇到Unicode符号ch时的下标
    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }
    
    /**
     * Returns the index within this string of the first occurrence of the specified character, starting the search at the specified index.
     * <p>
     * If a character with value {@code ch} occurs in the character sequence
     * represented by this {@code String} object at an index no smaller than {@code fromIndex}, then the index of the first such occurrence is returned.
     * For values of {@code ch} in the range from 0 to 0xFFFF (inclusive), this is the smallest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.charAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &gt;= fromIndex)
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the smallest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.codePointAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &gt;= fromIndex)
     * </pre></blockquote>
     * is true. In either case, if no such character occurs in this string at or after position {@code fromIndex}, then {@code -1} is returned.
     *
     * <p>
     * There is no restriction on the value of {@code fromIndex}.
     * If it is negative, it has the same effect as if it were zero:
     * this entire string may be searched. If it is greater than the length of this string,
     * it has the same effect as if it were equal to the length of this string: {@code -1} is returned.
     *
     * <p>All indices are specified in {@code char} values
     * (Unicode code units).
     *
     * @param ch        a character (Unicode code point).
     * @param fromIndex the index to start the search from.
     *
     * @return the index of the first occurrence of the character in the character sequence represented by this object that is greater
     * than or equal to {@code fromIndex}, or {@code -1} if the character does not occur.
     */
    // 从字符索引fromIndex处向后搜索，返回在当前[字符序列]中首次遇到Unicode符号ch时的下标
    public int indexOf(int ch, int fromIndex) {
        return isLatin1() ? StringLatin1.indexOf(value, ch, fromIndex) : StringUTF16.indexOf(value, ch, fromIndex);
    }
    
    /**
     * Returns the index within this string of the last occurrence of the specified character.
     * For values of {@code ch} in the range from 0 to 0xFFFF (inclusive),
     * the index (in Unicode code units) returned is the largest value <i>k</i> such that:
     * <blockquote><pre>
     * this.charAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the largest value <i>k</i> such that:
     * <blockquote><pre>
     * this.codePointAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true.  In either case, if no such character occurs in this string, then {@code -1} is returned.
     * The {@code String} is searched backwards starting at the last character.
     *
     * @param ch a character (Unicode code point).
     *
     * @return the index of the last occurrence of the character in the character sequence represented by this object,
     * or {@code -1} if the character does not occur.
     */
    // 从串尾向前搜索，返回在当前[字符序列]中首次遇到Unicode符号ch时的下标
    public int lastIndexOf(int ch) {
        return lastIndexOf(ch, length() - 1);
    }
    
    /**
     * Returns the index within this string of the last occurrence of the specified character,
     * searching backward starting at the specified index.
     * For values of {@code ch} in the range from 0 to 0xFFFF (inclusive), the index returned is the largest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.charAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &lt;= fromIndex)
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the largest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.codePointAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &lt;= fromIndex)
     * </pre></blockquote>
     * is true. In either case, if no such character occurs in this string at or before position {@code fromIndex}, then {@code -1} is returned.
     *
     * <p>All indices are specified in {@code char} values
     * (Unicode code units).
     *
     * @param ch        a character (Unicode code point).
     * @param fromIndex the index to start the search from.
     *                  There is no restriction on the value of {@code fromIndex}.
     *                  If it is greater than or equal to the length of this string,
     *                  it has the same effect as if it were equal to one less than the length of this string:
     *                  this entire string may be searched. If it is negative, it has the same effect as if it were -1: -1 is returned.
     *
     * @return the index of the last occurrence of the character in the character sequence represented by this object
     * that is less than or equal to {@code fromIndex}, or {@code -1} if the character does not occur before that point.
     */
    // 从字符索引fromIndex处向前搜索，返回在当前[字符序列]中首次遇到Unicode符号ch时的下标
    public int lastIndexOf(int ch, int fromIndex) {
        return isLatin1()
            ? StringLatin1.lastIndexOf(value, ch, fromIndex)
            : StringUTF16.lastIndexOf(value, ch, fromIndex);
    }
    
    /*▲ 查找Unicode符号下标 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 码点/码元 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the character (Unicode code point) at the specified index.
     * The index refers to {@code char} values (Unicode code units) and ranges from {@code 0} to {@link #length()}{@code  - 1}.
     *
     * <p> If the {@code char} value specified at the given index
     * is in the high-surrogate range, the following index is less than the length of this {@code String},
     * and the {@code char} value at the following index is in the low-surrogate range,
     * then the supplementary code point corresponding to this surrogate pair is returned.
     * Otherwise, the {@code char} value at the given index is returned.
     *
     * @param index the index to the {@code char} values
     *
     * @return the code point value of the character at the {@code index}
     *
     * @throws IndexOutOfBoundsException if the {@code index} argument is negative or not less than the length of this string.
     * @since 1.5
     *
     *
     * 此方法用来返回索引index处码元的码点值，如果该码元位于Unicode编码的高代理区，则需返回完整的Unicode编码
     *
     * Java字符编码中涉及到的码点跟码元跟Unicode中涉及到的概率略有不同
     *
     * 不是每个码点值都代表一个Unicode符号（打印在屏幕上与预期的效果一致）：
     * 对于英文符号，一个Unicode符号可用一个字节表示，如"0" -->"\u0030"，这就是它的码点值
     * 常用中文符号，一个Unicode符号可用两个字节表示，如"哈"-->"\u54c8"，这就是它的码点值
     * 一些生僻汉字，一个“合法符号”可用四个字节表示，如"𫠝"-->"\uD86E\uDC1D"，这不是它的码点值，它的码点值需要再次计算
     *
     * 注：对于4个字节的字符，需要将4个字节进行特殊转换才能求得码点值
     *
     * 补充：
     * 一个码元是两个字节组成的char（UTF-16）
     * 一个码元不一定是一个Unicode符号，比如四字节字符
     * 同理，一个char也不一定能表示一个Unicode符号，比如四字节字符
     *
     * Unicode字符集划分为17个区域：
     * [U+ 0000 ~ U+  FFFF]：基本平面（1个区域）
     * [U+10000 ~ U+10FFFF]：其他平面（16个区域）
     *
     * Unicode值小于0x10000的符号用等于该值的16位整数来表示
     * Unicode值大于0x10000的符号需要拼凑
     *
     * 基本平面区域内，有个特殊的代理区：[U+D800 ~ U+DFFF]。
     * 代理区中单个16-bit单元无意义，两个16-bit单元合一起才有意义。
     * 代理区分为两段：
     *      高代理区             低代理区
     * U+D800 ---- U+DBFF | 0xDC00 ---- 0xDFFF
     *      前导代理             后尾代理
     * 取一个高代理区的16-bit单元和一个低代理区的16-bit单元，就合成了一个大于0x10000的符号
     *
     * 具体转换规则：
     * 1.基本平面代理区的码点值 ---> 其他平面的字符X的码点值
     *   X = 0x10000 + (高代理-0xD800)<<10 + 低代理-0xDC00
     *
     * 2.其他平面的字符X的码点值 ---> 基本平面代理区的码点值
     *   1>. Y = X-0x10000
     *   2>. 高代理 = Y>>10 + 0xD800     // 右移了10位
     *   3>. 低代理 = Y&0x3FF + 0xDC00  // 保留低10位
     *
     * 注：
     * 如果给出的索引位置不合适，则可能无法得到预期的码点
     *
     * 举例，在多字节字符中：
     * 哈 = "\u54c8";
     * 𫠝 = "\uD86E\uDC1D";
     * 对于：
     * String s = "\u54c8\uD86E\uDC1D\u54c8";
     * s.codePointAt(0) == "\u54c8"的Unicode值        // 哈
     * s.codePointAt(1) == "\uD86E\uDC1D"的Unicode值  // 𫠝
     * s.codePointAt(2) == "\uDC1D"的Unicode值        // ?
     * s.codePointAt(3) == "\u54c8"                   // 哈
     */
    // 返回String中index处符号（一字节/双字节/四字节）的Unicode编码（从前到后试探）
    public int codePointAt(int index) {
        // 可以用压缩的Latin1字符集表示
        if(isLatin1()) {
            checkIndex(index, value.length);
            // 返回单字节符号码点
            return value[index] & 0xff;
        }
        
        // 计算码元（char）数量
        int length = value.length >> 1;
        checkIndex(index, length);
        
        // 返回UTF16-String中某处符号（双字节/四字节）的Unicode编码
        return StringUTF16.codePointAt(value, index, length);
    }
    
    /**
     * Returns the character (Unicode code point) before the specified index.
     * The index refers to {@code char} values (Unicode code units) and ranges from {@code 1} to {@link CharSequence#length() length}.
     *
     * If the {@code char} value at {@code (index - 1)} is in the low-surrogate range, {@code (index - 2)} is not negative,
     * and the {@code char} value at {@code (index - 2)} is in the high-surrogate range,
     * then the supplementary code point value of the surrogate pair is returned.
     * If the {@code char} value at {@code index - 1} is an unpaired low-surrogate or a high-surrogate, the surrogate value is returned.
     *
     * @param index the index following the code point that should be returned
     *
     * @return the Unicode code point value before the given index.
     *
     * @throws IndexOutOfBoundsException if the {@code index} argument is less than 1 or greater than the length of this string.
     * @since 1.5
     *
     * 返回索引index-1处码元的码点值，如果该码元位于Unicode编码的低代理区，则需返回完整的Unicode编码
     *
     * 如果索引位置不合适，则之前这个码点可能不能显示为正确的字符。
     * 举例，在多字节字符中：
     * 哈 = "\u54c8";
     * 𫠝 = "\uD86E\uDC1D";
     * 对于：
     * String s = "\u54c8\uD86E\uDC1D\u54c8";
     * s.codePointBefore(1) == "\u54c8"         // 哈
     * s.codePointBefore(2) == "\uD86E"         // ? （索引落在了高代理区）
     * s.codePointBefore(3) == "\uD86E\uDC1D"   // 𫠝（索引落在了低代理区）
     * s.codePointBefore(4) == "\u54c8"         // 哈
     */
    // 返回String中某处(index-1)符号的Unicode编码（从后往前试探）
    public int codePointBefore(int index) {
        int i = index - 1;
        if(i < 0 || i >= length()) {
            throw new StringIndexOutOfBoundsException(index);
        }
        // 可以用压缩的Latin1字符集表示
        if(isLatin1()) {
            return (value[i] & 0xff);
        }
        return StringUTF16.codePointBefore(value, index);
    }
    
    /**
     * Returns the number of Unicode code points in the specified text range of this {@code String}.
     * The text range begins at the specified {@code beginIndex} and extends to the {@code char} at index {@code endIndex - 1}.
     * Thus the length (in {@code char}s) of the text range is {@code endIndex-beginIndex}. Unpaired surrogates within the text range count as one code point each.
     *
     * @param beginIndex the index to the first {@code char} of the text range.
     * @param endIndex   the index after the last {@code char} of the text range.
     *
     * @return the number of Unicode code points in the specified text range
     *
     * @throws IndexOutOfBoundsException if the {@code beginIndex} is negative,
     * or {@code endIndex} is larger than the length of this {@code String},
     * or {@code beginIndex} is larger than {@code endIndex}.
     * @since 1.5
     *
     * 返回指定码元范围内存在多少个Unicode符号
     * 例如，求整个String内的Unicode符号个数：
     *   codePointCount(0, s.length())
     */
    // 统计String中指定码元范围内存在多少个Unicode符号
    public int codePointCount(int beginIndex, int endIndex) {
        if(beginIndex < 0 || beginIndex > endIndex || endIndex > length()) {
            throw new IndexOutOfBoundsException();
        }
        // 可以用压缩的Latin1字符集表示
        if(isLatin1()) {
            return endIndex - beginIndex;
        }
        return StringUTF16.codePointCount(value, beginIndex, endIndex);
    }
    
    /**
     * Returns the index within this {@code String} that is offset from the given {@code index} by {@code codePointOffset} code points.
     * Unpaired surrogates within the text range given by {@code index} and {@code codePointOffset} count as one code point each.
     *
     * @param index           the index to be offset
     * @param codePointOffset the offset in code points
     *
     * @return the index within this {@code String}
     *
     * @throws IndexOutOfBoundsException if {@code index} is negative or larger then the length of this {@code String},
     * or if {@code codePointOffset} is positive and the substring starting with {@code index} has fewer than {@code codePointOffset} code points,
     * or if {@code codePointOffset} is negative and the substring before {@code index} has fewer than the absolute value of {@code codePointOffset} code points.
     * @since 1.5
     *
     * 返回index偏移codePointOffset个Unicode符号后新的索引值
     * codePointOffset的正负决定了偏移方向
     *
     * 考虑多字节字符：
     * String s = "\u54c8\uD86E\uDC1D\u54c8";   // \uD86E\uDC1D代表一个Unicode符号
     * s.offsetByCodePoints(0, 1) == 1
     * s.offsetByCodePoints(0, 2) == 3
     */
    // 返回index偏移codePointOffset个Unicode符号后新的索引值
    public int offsetByCodePoints(int index, int codePointOffset) {
        if(index < 0 || index>length()) {
            throw new IndexOutOfBoundsException();
        }
        return Character.offsetByCodePoints(this, index, codePointOffset);
    }
    
    /*▲ 码点/码元 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 大小写转换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Converts all of the characters in this {@code String} to lower case using the rules of the default locale.
     * This is equivalent to calling {@code toLowerCase(Locale.getDefault())}.
     * <p>
     * <b>Note:</b> This method is locale sensitive, and may produce unexpected
     * results if used for strings that are intended to be interpreted locale independently.
     * Examples are programming language identifiers, protocol keys, and HTML tags.
     * For instance, {@code "TITLE".toLowerCase()} in a Turkish locale returns {@code "t\u005Cu0131tle"},
     * where '\u005Cu0131' is the LATIN SMALL LETTER DOTLESS I character.
     * To obtain correct results for locale insensitive strings, use {@code toLowerCase(Locale.ROOT)}.
     *
     * @return the {@code String}, converted to lowercase.
     *
     * @see String#toLowerCase(Locale)
     */
    // 转为小写，本地语言环境
    public String toLowerCase() {
        return toLowerCase(Locale.getDefault());
    }
    
    /**
     * Converts all of the characters in this {@code String} to lower case using the rules of the given {@code Locale}.
     * Case mapping is based on the Unicode Standard version specified by the {@link Character Character} class.
     * Since case mappings are not always 1:1 char mappings, the resulting {@code String} may be a different length than the original {@code String}.
     * <p>
     * Examples of lowercase  mappings are in the following table:
     * <table class="plain">
     * <caption style="display:none">Lowercase mapping examples showing language code of locale, upper case, lower case, and description</caption>
     * <thead>
     * <tr>
     * <th scope="col">Language Code of Locale</th>
     * <th scope="col">Upper Case</th>
     * <th scope="col">Lower Case</th>
     * <th scope="col">Description</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>tr (Turkish)</td>
     * <th scope="row" style="font-weight:normal; text-align:left">&#92;u0130</th>
     * <td>&#92;u0069</td>
     * <td>capital letter I with dot above -&gt; small letter i</td>
     * </tr>
     * <tr>
     * <td>tr (Turkish)</td>
     * <th scope="row" style="font-weight:normal; text-align:left">&#92;u0049</th>
     * <td>&#92;u0131</td>
     * <td>capital letter I -&gt; small letter dotless i </td>
     * </tr>
     * <tr>
     * <td>(all)</td>
     * <th scope="row" style="font-weight:normal; text-align:left">French Fries</th>
     * <td>french fries</td>
     * <td>lowercased all chars in String</td>
     * </tr>
     * <tr>
     * <td>(all)</td>
     * <th scope="row" style="font-weight:normal; text-align:left">
     * &Iota;&Chi;&Theta;&Upsilon;&Sigma;</th>
     * <td>&iota;&chi;&theta;&upsilon;&sigma;</td>
     * <td>lowercased all chars in String</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param locale use the case transformation rules for this locale
     *
     * @return the {@code String}, converted to lowercase.
     *
     * @see String#toLowerCase()
     * @see String#toUpperCase()
     * @see String#toUpperCase(Locale)
     * @since 1.1
     */
    // 转为小写，需要指定语言环境
    public String toLowerCase(Locale locale) {
        return isLatin1()
            ? StringLatin1.toLowerCase(this, value, locale) : StringUTF16.toLowerCase(this, value, locale);
    }
    
    /**
     * Converts all of the characters in this {@code String} to upper case using the rules of the default locale.
     * This method is equivalent to {@code toUpperCase(Locale.getDefault())}.
     * <p>
     * <b>Note:</b> This method is locale sensitive, and may produce unexpected
     * results if used for strings that are intended to be interpreted locale independently.
     * Examples are programming language identifiers, protocol keys, and HTML tags.
     * For instance, {@code "title".toUpperCase()} in a Turkish locale returns {@code "T\u005Cu0130TLE"},
     * where '\u005Cu0130' is the LATIN CAPITAL LETTER I WITH DOT ABOVE character.
     * To obtain correct results for locale insensitive strings, use {@code toUpperCase(Locale.ROOT)}.
     *
     * @return the {@code String}, converted to uppercase.
     *
     * @see String#toUpperCase(Locale)
     */
    // 转为大写，本地语言环境
    public String toUpperCase() {
        return toUpperCase(Locale.getDefault());
    }
    
    /**
     * Converts all of the characters in this {@code String} to upper case using the rules of the given {@code Locale}.
     * Case mapping is based on the Unicode Standard version specified by the {@link Character Character} class.
     * Since case mappings are not always 1:1 char mappings, the resulting {@code String} may be a different length than the original {@code String}.
     * <p>
     * Examples of locale-sensitive and 1:M case mappings are in the following table.
     *
     * <table class="plain">
     * <caption style="display:none">Examples of locale-sensitive and 1:M case mappings.
     * Shows Language code of locale, lower case, upper case, and description.</caption>
     * <thead>
     * <tr>
     * <th scope="col">Language Code of Locale</th>
     * <th scope="col">Lower Case</th>
     * <th scope="col">Upper Case</th>
     * <th scope="col">Description</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>tr (Turkish)</td>
     * <th scope="row" style="font-weight:normal; text-align:left">&#92;u0069</th>
     * <td>&#92;u0130</td>
     * <td>small letter i -&gt; capital letter I with dot above</td>
     * </tr>
     * <tr>
     * <td>tr (Turkish)</td>
     * <th scope="row" style="font-weight:normal; text-align:left">&#92;u0131</th>
     * <td>&#92;u0049</td>
     * <td>small letter dotless i -&gt; capital letter I</td>
     * </tr>
     * <tr>
     * <td>(all)</td>
     * <th scope="row" style="font-weight:normal; text-align:left">&#92;u00df</th>
     * <td>&#92;u0053 &#92;u0053</td>
     * <td>small letter sharp s -&gt; two letters: SS</td>
     * </tr>
     * <tr>
     * <td>(all)</td>
     * <th scope="row" style="font-weight:normal; text-align:left">Fahrvergn&uuml;gen</th>
     * <td>FAHRVERGN&Uuml;GEN</td>
     * <td></td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param locale use the case transformation rules for this locale
     *
     * @return the {@code String}, converted to uppercase.
     *
     * @see String#toUpperCase()
     * @see String#toLowerCase()
     * @see String#toLowerCase(Locale)
     * @since 1.1
     */
    // 转为大写，需要指定语言环境
    public String toUpperCase(Locale locale) {
        return isLatin1()
            ? StringLatin1.toUpperCase(this, value, locale)
            : StringUTF16.toUpperCase(this, value, locale);
    }
    
    /*▲ 大小写转换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 求子串 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a string that is a substring of this string.
     * The substring begins with the character at the specified index and extends to the end of this string. <p> Examples:
     * <blockquote><pre>
     * "unhappy".substring(2) returns "happy"
     * "Harbison".substring(3) returns "bison"
     * "emptiness".substring(9) returns "" (an empty string)
     * </pre></blockquote>
     *
     * @param beginIndex the beginning index, inclusive.
     *
     * @return the specified substring.
     *
     * @throws IndexOutOfBoundsException if {@code beginIndex} is negative or larger than the length of this {@code String} object.
     */
    // 截取beginIndex起始处的字符序列，以String形式返回
    public String substring(int beginIndex) {
        if(beginIndex < 0) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        int subLen = length() - beginIndex;
        if(subLen < 0) {
            throw new StringIndexOutOfBoundsException(subLen);
        }
        if(beginIndex == 0) {
            return this;
        }
        return isLatin1()
            ? StringLatin1.newString(value, beginIndex, subLen)
            : StringUTF16.newString(value, beginIndex, subLen);
    }
    
    /**
     * Returns a string that is a substring of this string.
     * The substring begins at the specified {@code beginIndex} and extends to the character at index {@code endIndex - 1}.
     * Thus the length of the substring is {@code endIndex-beginIndex}.
     * <p>
     * Examples:
     * <blockquote><pre>
     * "hamburger".substring(4, 8) returns "urge"
     * "smiles".substring(1, 5) returns "mile"
     * </pre></blockquote>
     *
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex   the ending index, exclusive.
     *
     * @return the specified substring.
     *
     * @throws IndexOutOfBoundsException if the {@code beginIndex} is negative,
     * or {@code endIndex} is larger than the length of this {@code String} object,
     * or {@code beginIndex} is larger than {@code endIndex}.
     */
    // 截取[beginIndex, endIndex)范围内的字符序列，以String形式返回
    public String substring(int beginIndex, int endIndex) {
        int length = length();
        checkBoundsBeginEnd(beginIndex, endIndex, length);
        int subLen = endIndex - beginIndex;
        if(beginIndex == 0 && endIndex == length) {
            return this;
        }
        return isLatin1()
            ? StringLatin1.newString(value, beginIndex, subLen)
            : StringUTF16.newString(value, beginIndex, subLen);
    }
    
    /**
     * Returns a character sequence that is a subsequence of this sequence.
     *
     * <p> An invocation of this method of the form
     *
     * <blockquote><pre>
     * str.subSequence(begin,&nbsp;end)</pre></blockquote>
     *
     * behaves in exactly the same way as the invocation
     *
     * <blockquote><pre>
     * str.substring(begin,&nbsp;end)</pre></blockquote>
     *
     * @param beginIndex the begin index, inclusive.
     * @param endIndex   the end index, exclusive.
     *
     * @return the specified subsequence.
     *
     * @throws IndexOutOfBoundsException if {@code beginIndex} or {@code endIndex} is negative,
     * if {@code endIndex} is greater than {@code length()}, or if {@code beginIndex} is greater than {@code endIndex}
     * @apiNote This method is defined so that the {@code String} class can implement the {@link CharSequence} interface.
     * @spec JSR-51
     * @since 1.4
     */
    // 截取[beginIndex, endIndex)范围内的字符序列，以CharSequence形式返回
    public CharSequence subSequence(int beginIndex, int endIndex) {
        return this.substring(beginIndex, endIndex);
    }
    
    /**
     * Splits this string around matches of the given <a href="../util/regex/Pattern.html#sum">regular expression</a>.
     *
     * <p> This method works as if by invoking the two-argument {@link #split(String, int) split} method with the given expression and a limit argument of zero.
     * Trailing empty strings are therefore not included in the resulting array.
     *
     * <p> The string {@code "boo:and:foo"}, for example, yields the following
     * results with these expressions:
     *
     * <blockquote><table class="plain">
     * <caption style="display:none">Split examples showing regex and result</caption>
     * <thead>
     * <tr>
     * <th scope="col">Regex</th>
     * <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" style="text-weight:normal">:</th>
     * <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><th scope="row" style="text-weight:normal">o</th>
     * <td>{@code { "b", "", ":and:f" }}</td></tr>
     * </tbody>
     * </table></blockquote>
     *
     * @param regex the delimiting regular expression
     *
     * @return the array of strings computed by splitting this string around matches of the given regular expression
     *
     * @throws PatternSyntaxException if the regular expression's syntax is invalid
     * @spec JSR-51
     * @see Pattern
     * @since 1.4
     */
    /*
     * 将当前String从与正则regex匹配的地方切割，返回切割后的子串集合（忽略结尾空串），
     * 参见Pattern#split(CharSequence)。
     */
    public String[] split(String regex) {
        return split(regex, 0);
    }
    
    /**
     * Splits this string around matches of the given
     * <a href="../util/regex/Pattern.html#sum">regular expression</a>.
     *
     * <p> The array returned by this method contains each substring of this
     * string that is terminated by another substring that matches the given expression or is terminated by the end of the string.
     * The substrings in the array are in the order in which they occur in this string.
     * If the expression does not match any part of the input then the resulting array has just one element, namely this string.
     *
     * <p> When there is a positive-width match at the beginning of this
     * string then an empty leading substring is included at the beginning of the resulting array.
     * A zero-width match at the beginning however never produces such empty leading substring.
     *
     * <p> The {@code limit} parameter controls the number of times the
     * pattern is applied and therefore affects the length of the resulting array.
     * <ul>
     * <li><p>
     * If the <i>limit</i> is positive then the pattern will be applied at most <i>limit</i>&nbsp;-&nbsp;1 times,
     * the array's length will be no greater than <i>limit</i>,
     * and the array's last entry will contain all input beyond the last matched delimiter.</p></li>
     *
     * <li><p>
     * If the <i>limit</i> is zero then the pattern will be applied as many times as possible,
     * the array can have any length, and trailing empty strings will be discarded.</p></li>
     *
     * <li><p>
     * If the <i>limit</i> is negative then the pattern will be applied as many times as possible and the array can have any length.</p></li>
     * </ul>
     *
     * <p> The string {@code "boo:and:foo"}, for example, yields the
     * following results with these parameters:
     *
     * <blockquote><table class="plain">
     * <caption style="display:none">Split example showing regex, limit, and result</caption>
     * <thead>
     * <tr>
     * <th scope="col">Regex</th>
     * <th scope="col">Limit</th>
     * <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">:</th>
     * <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">2</th>
     * <td>{@code { "boo", "and:foo" }}</td></tr>
     * <tr><!-- : -->
     * <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     * <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><!-- : -->
     * <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-2</th>
     * <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">o</th>
     * <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     * <td>{@code { "b", "", ":and:f", "", "" }}</td></tr>
     * <tr><!-- o -->
     * <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-2</th>
     * <td>{@code { "b", "", ":and:f", "", "" }}</td></tr>
     * <tr><!-- o -->
     * <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">0</th>
     * <td>{@code { "b", "", ":and:f" }}</td></tr>
     * </tbody>
     * </table></blockquote>
     *
     * <p> An invocation of this method of the form
     * <i>str.</i>{@code split(}<i>regex</i>{@code ,}&nbsp;<i>n</i>{@code )}
     * yields the same result as the expression
     *
     * <blockquote>
     * <code>
     * {@link Pattern}.{@link Pattern#compile compile}(<i>regex</i>).{@link Pattern#split(CharSequence, int) split}(<i>str</i>,&nbsp;<i>n</i>)
     * </code>
     * </blockquote>
     *
     * @param regex the delimiting regular expression
     * @param limit the result threshold, as described above
     *
     * @return the array of strings computed by splitting this string around matches of the given regular expression
     *
     * @throws PatternSyntaxException if the regular expression's syntax is invalid
     * @spec JSR-51
     * @see Pattern
     * @since 1.4
     */
    /*
     * 将当前String从与正则regex匹配的地方切割，返回切割后的子串集合，参数limit用来限制返回的子串数量，
     * 参见Pattern#split(CharSequence, int)。
     */
    public String[] split(String regex, int limit) {
        /*
         * fastpath if the regex is a
         * (1)one-char String and this character is not one of the RegEx's meta characters ".$|()[{^?*+\\", or
         * (2)two-char String and the first char is the backslash and the second is not the ascii digit or ascii letter.
         */
        char ch = 0;
    
        if((regex.length() == 1 && ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1)
            || (regex.length() == 2
            && regex.charAt(0) == '\\'
            && (((ch = regex.charAt(1)) - '0') | ('9' - ch)) < 0
            && ((ch - 'a') | ('z' - ch)) < 0
            && ((ch - 'A') | ('Z' - ch)) < 0)
        ) {
            if(ch<Character.MIN_HIGH_SURROGATE || ch>Character.MAX_LOW_SURROGATE) {
                int off = 0;
                int next = 0;
                boolean limited = limit>0;
                ArrayList<String> list = new ArrayList<>();
                while((next = indexOf(ch, off)) != -1) {
                    if(!limited || list.size()<limit - 1) {
                        list.add(substring(off, next));
                        off = next + 1;
                    } else {    // last one
                        //assert (list.size() == limit - 1);
                        int last = length();
                        list.add(substring(off, last));
                        off = last;
                        break;
                    }
                }
                // If no match was found, return this
                if(off == 0)
                    return new String[]{this};
                
                // Add remaining segment
                if(!limited || list.size()<limit)
                    list.add(substring(off, length()));
                
                // Construct result
                int resultSize = list.size();
                if(limit == 0) {
                    while(resultSize>0 && list.get(resultSize - 1).length() == 0) {
                        resultSize--;
                    }
                }
                
                String[] result = new String[resultSize];
                
                return list.subList(0, resultSize).toArray(result);
            }
        }
        
        return Pattern.compile(regex).split(this, limit);
    }
    
    /*▲ 求子串 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 替换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Replaces the first substring of this string that matches the given <a href="../util/regex/Pattern.html#sum">regular expression</a>
     * with the given replacement.
     *
     * <p> An invocation of this method of the form
     * <i>str</i>{@code .replaceFirst(}<i>regex</i>{@code ,} <i>repl</i>{@code )}
     * yields exactly the same result as the expression
     *
     * <blockquote>
     * <code>
     * {@link Pattern}.{@link Pattern#compile compile} (<i>regex</i>).
     * {@link Pattern#matcher(CharSequence) matcher}(<i>str</i>).
     * {@link java.util.regex.Matcher#replaceFirst replaceFirst}(<i>repl</i>)
     * </code>
     * </blockquote>
     *
     * <p>
     * Note that backslashes ({@code \}) and dollar signs ({@code $}) in the replacement string may cause the results to be different
     * than if it were being treated as a literal replacement string;
     * see {@link java.util.regex.Matcher#replaceFirst}.
     * Use {@link java.util.regex.Matcher#quoteReplacement} to suppress the special meaning of these characters, if desired.
     *
     * @param regex       the regular expression to which this string is to be matched
     * @param replacement the string to be substituted for the first match
     *
     * @return The resulting {@code String}
     *
     * @throws PatternSyntaxException if the regular expression's syntax is invalid
     * @spec JSR-51
     * @see Pattern
     * @since 1.4
     */
    // 使用replacement替换正则表达式regex匹配到的首个子串，replacement可以是捕获组的引用
    public String replaceFirst(String regex, String replacement) {
        return Pattern.compile(regex).matcher(this).replaceFirst(replacement);
    }
    
    /**
     * Replaces each substring of this string that matches the given <a href="../util/regex/Pattern.html#sum">regular expression</a>
     * with the given replacement.
     *
     * <p> An invocation of this method of the form
     * <i>str</i>{@code .replaceAll(}<i>regex</i>{@code ,} <i>repl</i>{@code )}
     * yields exactly the same result as the expression
     *
     * <blockquote>
     * <code>
     * {@link Pattern}.{@link Pattern#compile compile}(<i>regex</i>).{@link Pattern#matcher(CharSequence) matcher}(<i>str</i>).
     * {@link java.util.regex.Matcher#replaceAll replaceAll}(<i>repl</i>)
     * </code>
     * </blockquote>
     *
     * <p>
     * Note that backslashes ({@code \}) and dollar signs ({@code $}) in the replacement string may cause the results to be different than
     * if it were being treated as a literal replacement string;
     * see {@link java.util.regex.Matcher#replaceAll Matcher.replaceAll}.
     * Use {@link java.util.regex.Matcher#quoteReplacement} to suppress the special meaning of these characters, if desired.
     *
     * @param regex       the regular expression to which this string is to be matched
     * @param replacement the string to be substituted for each match
     *
     * @return The resulting {@code String}
     *
     * @throws PatternSyntaxException if the regular expression's syntax is invalid
     * @spec JSR-51
     * @see Pattern
     * @since 1.4
     */
    // 使用replacement替换正则表达式regex匹配到的全部子串，replacement可以是捕获组的引用
    public String replaceAll(String regex, String replacement) {
        return Pattern.compile(regex).matcher(this).replaceAll(replacement);
    }
    
    /**
     * Replaces each substring of this string that matches the literal target sequence with the specified literal replacement sequence.
     * The replacement proceeds from the beginning of the string to the end, for example,
     * replacing "aa" with "b" in the string "aaa" will result in "ba" rather than "ab".
     *
     * @param target      The sequence of char values to be replaced
     * @param replacement The replacement sequence of char values
     *
     * @return The resulting string
     *
     * @since 1.5
     */
    // 使用replacement替换target，并返回替换后的String
    public String replace(CharSequence target, CharSequence replacement) {
        String tgtStr = target.toString();
        String replStr = replacement.toString();
        int j = indexOf(tgtStr);
        if(j < 0) {
            return this;
        }
        int tgtLen = tgtStr.length();
        int tgtLen1 = Math.max(tgtLen, 1);
        int thisLen = length();
        
        int newLenHint = thisLen - tgtLen + replStr.length();
        if(newLenHint < 0) {
            throw new OutOfMemoryError();
        }
        StringBuilder sb = new StringBuilder(newLenHint);
        int i = 0;
        do {
            sb.append(this, i, j).append(replStr);
            i = j + tgtLen;
        } while(j < thisLen && (j = indexOf(tgtStr, j + tgtLen1)) > 0);
        
        return sb.append(this, i, thisLen).toString();
    }
    
    /**
     * Returns a string resulting from replacing all occurrences of {@code oldChar} in this string with {@code newChar}.
     * <p>
     * If the character {@code oldChar} does not occur in the character sequence represented by this {@code String} object,
     * then a reference to this {@code String} object is returned. Otherwise,
     * a {@code String} object is returned that represents a character sequence identical to the character sequence represented by this {@code String} object,
     * except that every occurrence of {@code oldChar} is replaced by an occurrence of {@code newChar}.
     * <p>
     * Examples:
     * <blockquote><pre>
     * "mesquite in your cellar".replace('e', 'o')
     *         returns "mosquito in your collar"
     * "the war of baronets".replace('r', 'y')
     *         returns "the way of bayonets"
     * "sparring with a purple porpoise".replace('p', 't')
     *         returns "starring with a turtle tortoise"
     * "JonL".replace('q', 'x') returns "JonL" (no change)
     * </pre></blockquote>
     *
     * @param oldChar the old character.
     * @param newChar the new character.
     *
     * @return a string derived from this string by replacing every occurrence of {@code oldChar} with {@code newChar}.
     */
    // 使用newChar替换String中的oldChar，并返回替换后的String
    public String replace(char oldChar, char newChar) {
        if(oldChar != newChar) {
            String ret = isLatin1()
                ? StringLatin1.replace(value, oldChar, newChar)
                : StringUTF16.replace(value, oldChar, newChar);
            if(ret != null) {
                return ret;
            }
        }
        return this;
    }
    
    /*▲ 替换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 修剪 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a string whose value is this string, with all leading and trailing space removed,
     * where space is defined as any character whose codepoint is less than or equal to {@code 'U+0020'} (the space character).
     * <p>
     * If this {@code String} object represents an empty character sequence,
     * or the first and last characters of character sequence represented by this {@code String} object both have codes that are not space (as defined above),
     * then a reference to this {@code String} object is returned.
     * <p>
     * Otherwise, if all characters in this string are space (as defined above), then a  {@code String} object representing an empty string is returned.
     * <p>
     * Otherwise, let <i>k</i> be the index of the first character in the string whose code is not a space (as defined above) and let
     * <i>m</i> be the index of the last character in the string whose code
     * is not a space (as defined above). A {@code String} object is returned,
     * representing the substring of this string that begins with the character at index <i>k</i> and ends with the character at index <i>m</i>-that is,
     * the result of {@code this.substring(k, m + 1)}.
     * <p>
     * This method may be used to trim space (as defined above) from the beginning and end of a string.
     *
     * @return a string whose value is this string, with all leading and trailing space removed, or this string if it has no leading or trailing space.
     */
    // 返回删掉首尾【空格字符】的String
    public String trim() {
        String ret = isLatin1() ? StringLatin1.trim(value) : StringUTF16.trim(value);
        return ret == null ? this : ret;
    }
    
    /**
     * Returns a string whose value is this string, with all leading and trailing {@link Character#isWhitespace(int) white space} removed.
     * <p>
     * If this {@code String} object represents an empty string,
     * or if all code points in this string are {@link Character#isWhitespace(int) white space},
     * then an empty string is returned.
     * <p>
     * Otherwise, returns a substring of this string beginning with the first code point
     * that is not a {@link Character#isWhitespace(int) white space} up to and including the last code point
     * that is not a {@link Character#isWhitespace(int) white space}.
     * <p>
     * This method may be used to strip {@link Character#isWhitespace(int) white space} from the beginning and end of a string.
     *
     * @return a string whose value is this string, with all leading and trailing white space removed
     *
     * @see Character#isWhitespace(int)
     * @since 11
     */
    // 返回删掉首尾【空白符】的String
    public String strip() {
        String ret = isLatin1()
            ? StringLatin1.strip(value)
            : StringUTF16.strip(value);
        return ret == null ? this : ret;
    }
    
    /**
     * Returns a string whose value is this string, with all leading {@link Character#isWhitespace(int) white space} removed.
     * <p>
     * If this {@code String} object represents an empty string,
     * or if all code points in this string are {@link Character#isWhitespace(int) white space},
     * then an empty string is returned.
     * <p>
     * Otherwise, returns a substring of this string beginning with the first code point
     * that is not a {@link Character#isWhitespace(int) white space} up to to and including the last code point of this string.
     * <p>
     * This method may be used to trim {@link Character#isWhitespace(int) white space} from the beginning of a string.
     *
     * @return a string whose value is this string, with all leading white space removed
     *
     * @see Character#isWhitespace(int)
     * @since 11
     */
    // 返回去掉起始处连续空白符的String
    public String stripLeading() {
        String ret = isLatin1()
            ? StringLatin1.stripLeading(value)
            : StringUTF16.stripLeading(value);
        return ret == null ? this : ret;
    }
    
    /**
     * Returns a string whose value is this string, with all trailing {@link Character#isWhitespace(int) white space} removed.
     * <p>
     * If this {@code String} object represents an empty string,
     * or if all characters in this string are {@link Character#isWhitespace(int) white space},
     * then an empty string is returned.
     * <p>
     * Otherwise, returns a substring of this string beginning with the first code point of this string up to
     * and including the last code point that is not a {@link Character#isWhitespace(int) white space}.
     * <p>
     * This method may be used to trim {@link Character#isWhitespace(int) white space} from the end of a string.
     *
     * @return a string whose value is this string, with all trailing white space removed
     *
     * @see Character#isWhitespace(int)
     * @since 11
     */
    // 返回去掉结尾处连续空白符的String
    public String stripTrailing() {
        String ret = isLatin1()
            ? StringLatin1.stripTrailing(value)
            : StringUTF16.stripTrailing(value);
        return ret == null ? this : ret;
    }
    
    /*▲ 修剪 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 拼接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new String composed of copies of the {@code CharSequence elements} joined together
     * with a copy of the specified {@code delimiter}.
     *
     * <blockquote>For example,
     * <pre>{@code
     *     String message = String.join("-", "Java", "is", "cool");
     *     // message returned is: "Java-is-cool"
     * }</pre></blockquote>
     *
     * Note that if an element is null, then {@code "null"} is added.
     *
     * @param delimiter the delimiter that separates each element
     * @param elements  the elements to join together.
     *
     * @return a new {@code String} that is composed of the {@code elements} separated by the {@code delimiter}
     *
     * @throws NullPointerException If {@code delimiter} or {@code elements} is {@code null}
     * @see StringJoiner
     * @since 1.8
     */
    // 拼接子串elements，中间用分隔符delimiter隔开（借助字符串拼接器StringJoiner来实现）
    public static String join(CharSequence delimiter, CharSequence... elements) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(elements);
        // Number of elements not likely worth Arrays.stream overhead.
        StringJoiner joiner = new StringJoiner(delimiter);
        for(CharSequence cs : elements) {
            joiner.add(cs);
        }
        return joiner.toString();
    }
    
    /**
     * Returns a new {@code String} composed of copies of the {@code CharSequence elements} joined together
     * with a copy of the specified {@code delimiter}.
     *
     * <blockquote>For example,
     * <pre>{@code
     *     List<String> strings = List.of("Java", "is", "cool");
     *     String message = String.join(" ", strings);
     *     //message returned is: "Java is cool"
     *
     *     Set<String> strings =
     *         new LinkedHashSet<>(List.of("Java", "is", "very", "cool"));
     *     String message = String.join("-", strings);
     *     //message returned is: "Java-is-very-cool"
     * }</pre></blockquote>
     *
     * Note that if an individual element is {@code null}, then {@code "null"} is added.
     *
     * @param delimiter a sequence of characters that is used to separate each of the {@code elements} in the resulting {@code String}
     * @param elements  an {@code Iterable} that will have its {@code elements} joined together.
     *
     * @return a new {@code String} that is composed from the {@code elements} argument
     *
     * @throws NullPointerException If {@code delimiter} or {@code elements} is {@code null}
     * @see #join(CharSequence, CharSequence...)
     * @see StringJoiner
     * @since 1.8
     */
    // 拼接子串elements，中间用分隔符delimiter隔开（借助字符串拼接器StringJoiner来实现）
    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(elements);
        StringJoiner joiner = new StringJoiner(delimiter);
        for(CharSequence cs : elements) {
            joiner.add(cs);
        }
        return joiner.toString();
    }
    
    /**
     * Concatenates the specified string to the end of this string.
     * <p>
     * If the length of the argument string is {@code 0},
     * then this {@code String} object is returned. Otherwise,
     * a {@code String} object is returned that represents a character sequence
     * that is the concatenation of the character sequence represented by this {@code String} object
     * and the character sequence represented by the argument string.<p> Examples:
     * <blockquote><pre>
     * "cares".concat("s") returns "caress"
     * "to".concat("get").concat("her") returns "together"
     * </pre></blockquote>
     *
     * @param str the {@code String} that is concatenated to the end of this {@code String}.
     *
     * @return a string that represents the concatenation of this object's characters followed by the string argument's characters.
     */
    // 将str拼接到原字符串末尾
    public String concat(String str) {
        int olen = str.length();
        if(olen == 0) {
            return this;
        }
        if(coder() == str.coder()) {
            byte[] val = this.value;
            byte[] oval = str.value;
            int len = val.length + oval.length;
            byte[] buf = Arrays.copyOf(val, len);
            System.arraycopy(oval, 0, buf, val.length, oval.length);
            return new String(buf, coder);
        }
        int len = length();
        // 创建长度为2*（len+olen）的字节数组
        byte[] buf = StringUTF16.newBytesFor(len + olen);
        
        // 字符串类型不一样时，统一转为UTF16-String
        getBytes(buf, 0, UTF16);
        str.getBytes(buf, len, UTF16);
        return new String(buf, UTF16);
    }
    
    /*▲ 拼接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 匹配 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tests if the substring of this string beginning at the specified index starts with the specified prefix.
     *
     * @param prefix  the prefix.
     * @param toffset where to begin looking in this string.
     *
     * @return {@code true} if the character sequence represented by the argument is a prefix of the substring of this object starting at index {@code toffset};
     * {@code false} otherwise. The result is {@code false} if {@code toffset} is negative or greater than the length of this {@code String} object;
     * otherwise the result is the same as the result of the expression
     * <pre> this.substring(toffset).startsWith(prefix) </pre>
     */
    // 判断指定范围的字符串是否以prefix开头，从下标toffset处开始搜索
    public boolean startsWith(String prefix, int toffset) {
        // Note: toffset might be near -1>>>1.
        if(toffset < 0 || toffset > length() - prefix.length()) {
            return false;
        }
        byte ta[] = value;
        byte pa[] = prefix.value;
        int po = 0;
        int pc = pa.length;
        if(coder() == prefix.coder()) {
            int to = isLatin1() ? toffset : toffset << 1;
            while(po < pc) {
                if(ta[to++] != pa[po++]) {
                    return false;
                }
            }
        } else {
            // 可以用压缩的Latin1字符集表示
            if(isLatin1()) {  // && pcoder == UTF16
                return false;
            }
            // coder == UTF16 && pcoder == LATIN1)
            while(po < pc) {
                if(StringUTF16.getChar(ta, toffset++) != (pa[po++] & 0xff)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Tests if this string starts with the specified prefix.
     *
     * @param prefix the prefix.
     *
     * @return {@code true} if the character sequence represented by the argument is a prefix of the character sequence represented by this string;
     * {@code false} otherwise.
     * Note also that {@code true} will be returned if the argument is an empty string
     * or is equal to this {@code String} object as determined by the {@link #equals(Object)} method.
     *
     * @since 1.0
     */
    // 判断指定范围的字符串是否以prefix开头，从下标0处开始搜索
    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }
    
    /**
     * Tests if this string ends with the specified suffix.
     *
     * @param suffix the suffix.
     *
     * @return {@code true} if the character sequence represented by the argument is a suffix of the character sequence represented by this object;
     * {@code false} otherwise.
     * Note that the result will be {@code true} if the argument is the empty string or is equal to this {@code String} object
     * as determined by the {@link #equals(Object)} method.
     */
    // 判断字符串是否以suffix结尾
    public boolean endsWith(String suffix) {
        return startsWith(suffix, length() - suffix.length());
    }
    
    /**
     * Returns true if and only if this string contains the specified sequence of char values.
     *
     * @param s the sequence to search for
     *
     * @return true if this string contains {@code s}, false otherwise
     *
     * @since 1.5
     */
    // 判断字符串内是否包含子序列s
    public boolean contains(CharSequence s) {
        return indexOf(s.toString()) >= 0;
    }
    
    /**
     * Tells whether or not this string matches the given <a href="../util/regex/Pattern.html#sum">regular expression</a>.
     *
     * <p> An invocation of this method of the form
     * <i>str</i>{@code .matches(}<i>regex</i>{@code )} yields exactly the
     * same result as the expression
     *
     * <blockquote>
     * {@link Pattern}.{@link Pattern#matches(String, CharSequence) matches(<i>regex</i>, <i>str</i>)}
     * </blockquote>
     *
     * @param regex the regular expression to which this string is to be matched
     *
     * @return {@code true} if, and only if, this string matches the given regular expression
     *
     * @throws PatternSyntaxException if the regular expression's syntax is invalid
     * @spec JSR-51
     * @see Pattern
     * @since 1.4
     */
    // 判断当前字符串是否与给定的正则regex匹配，参见Pattern#matches(String, CharSequence)和Matcher#matches()
    public boolean matches(String regex) {
        return Pattern.matches(regex, this);
    }
    
    /*▲ 匹配 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 流 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a stream of {@code int} zero-extending the {@code char} values from this sequence.
     * Any char which maps to a <a href="{@docRoot}/java.base/java/lang/Character.html#unicode">surrogate code point</a> is passed through uninterpreted.
     *
     * @return an IntStream of char values from this sequence
     *
     * @since 9
     */
    // 将当前char序列转为流序列，序列中每个元素是char
    @Override
    public IntStream chars() {
        return StreamSupport.intStream(
            isLatin1()
                ? new StringLatin1.CharsSpliterator(value, Spliterator.IMMUTABLE)
                : new StringUTF16.CharsSpliterator(value, Spliterator.IMMUTABLE),
            false
        );
    }
    
    /**
     * Returns a stream of code point values from this sequence.
     * Any surrogate pairs encountered in the sequence are combined as if by {@linkplain Character#toCodePoint Character.toCodePoint}
     * and the result is passed to the stream. Any other code units, including ordinary BMP characters, unpaired surrogates,
     * and undefined code units, are zero-extended to {@code int} values which are then passed to the stream.
     *
     * @return an IntStream of Unicode code points from this sequence
     *
     * @since 9
     */
    // 将当前Unicode符号序列转为流序列，序列中每个元素是Unicode符号
    @Override
    public IntStream codePoints() {
        return StreamSupport.intStream(
            isLatin1()
                ? new StringLatin1.CharsSpliterator(value, Spliterator.IMMUTABLE)
                : new StringUTF16.CodePointsSpliterator(value, Spliterator.IMMUTABLE), false
        );
    }
    
    /**
     * Returns a stream of lines extracted from this string, separated by line terminators.
     * <p>
     * A <i>line terminator</i> is one of the following:
     * a line feed character {@code "\n"} (U+000A), a carriage return character {@code "\r"} (U+000D),
     * or a carriage return followed immediately by a line feed {@code "\r\n"} (U+000D U+000A).
     * <p>
     * A <i>line</i> is either a sequence of zero or more characters followed by a line terminator,
     * or it is a sequence of one or more characters followed by the end of the string.
     * A line does not include the line terminator.
     * <p>
     * The stream returned by this method contains the lines from this string in the order in which they occur.
     *
     * @return the stream of lines extracted from this string
     *
     * @apiNote This definition of <i>line</i> implies that an empty string has zero lines
     * and that there is no empty line following a line terminator at the end of a string.
     * @implNote This method provides better performance than split("\R") by supplying elements lazily and by faster search of new line terminators.
     * @since 11
     */
    // 将String按行转为流序列，序列中每个元素都代表一行（遇到\n或\r才换行）
    public Stream<String> lines() {
        return isLatin1() ? StringLatin1.lines(value) : StringUTF16.lines(value);
    }
    
    /*▲ 流 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 格式化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a formatted string using the specified format string and arguments.
     *
     * <p> The locale always used is the one returned by {@link
     * Locale#getDefault(Locale.Category) Locale.getDefault(Locale.Category)} with {@link Locale.Category#FORMAT FORMAT} category specified.
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>
     * @param args   Arguments referenced by the format specifiers in the format string.
     *               If there are more arguments than format specifiers, the extra arguments are ignored.
     *               The number of arguments is variable and may be zero.
     *               The maximum number of arguments is limited by the maximum dimension of a Java array as defined by
     *               <cite>The Java&trade; Virtual Machine Specification</cite>.
     *               The behaviour on a {@code null} argument depends on the <a href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @return A formatted string
     *
     * @throws java.util.IllegalFormatException If a format string contains an illegal syntax,
     *                                          a format specifier that is incompatible with the given arguments, insufficient arguments given the format string,
     *                                          or other illegal conditions.
     *                                          For specification of all possible formatting errors,
     *                                          see the <a href="../util/Formatter.html#detail">Details</a> section of the formatter class specification.
     * @see Formatter
     * @since 1.5
     */
    // 返回对指定的字符串片段进行格式化之后的结果
    public static String format(String format, Object... args) {
        return new Formatter().format(format, args).toString();
    }
    
    /**
     * Returns a formatted string using the specified locale, format string, and arguments.
     *
     * @param l      The {@linkplain Locale locale} to apply during formatting.
     *               If {@code l} is {@code null} then no localization is applied.
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>
     * @param args   Arguments referenced by the format specifiers in the format string.
     *               If there are more arguments than format specifiers, the extra arguments are ignored.
     *               The number of arguments is variable and may be zero.
     *               The maximum number of arguments is limited by the maximum dimension of a Java array as defined by
     *               <cite>The Java&trade; Virtual Machine Specification</cite>.
     *               The behaviour on a {@code null} argument depends on the
     *               <a href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @return A formatted string
     *
     * @throws java.util.IllegalFormatException If a format string contains an illegal syntax,
     *                                          a format specifier that is incompatible with the given arguments,
     *                                          insufficient arguments given the format string, or other illegal conditions.
     *                                          For specification of all possible formatting errors,
     *                                          see the <a href="../util/Formatter.html#detail">Details</a> section of the formatter class specification
     * @see Formatter
     * @since 1.5
     */
    // 返回对指定的字符串片段进行格式化之后的结果(会使用指定区域的格式习惯)
    public static String format(Locale locale, String format, Object... args) {
        return new Formatter(locale).format(format, args).toString();
    }
    
    /*▲ 格式化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 比较 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Compares two strings lexicographically.
     * The comparison is based on the Unicode value of each character in the strings.
     * The character sequence represented by this {@code String} object is compared lexicographically to the character sequence
     * represented by the argument string.
     * The result is a negative integer if this {@code String} object lexicographically precedes the argument string.
     * The result is a positive integer if this {@code String} object lexicographically follows the argument string.
     * The result is zero if the strings are equal; {@code compareTo} returns {@code 0} exactly when the {@link #equals(Object)} method would return {@code true}.
     * <p>
     * This is the definition of lexicographic ordering.
     * If two strings are different, then either they have different characters at some index that is a valid index for both strings,
     * or their lengths are different, or both.
     * If they have different characters at one or more index positions, let <i>k</i> be the smallest such index;
     * then the string whose character at position <i>k</i> has the smaller value, as determined by using the {@code <} operator,
     * lexicographically precedes the other string.
     * In this case, {@code compareTo} returns the difference of the two character values at position {@code k} in the two string -- that is, the value:
     * <blockquote><pre>
     * this.charAt(k)-anotherString.charAt(k)
     * </pre></blockquote>
     * If there is no index position at which they differ, then the shorter string lexicographically precedes the longer string.
     * In this case, {@code compareTo} returns the difference of the lengths of the strings -- that is, the value:
     * <blockquote><pre>
     * this.length()-anotherString.length()
     * </pre></blockquote>
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param anotherString the {@code String} to be compared.
     *
     * @return the value {@code 0} if the argument string is equal to this string;
     * a value less than {@code 0} if this string is lexicographically less than the string argument;
     * and a value greater than {@code 0} if this string is lexicographically greater than the string argument.
     */
    // 比较两个String，区分大小写
    public int compareTo(String anotherString) {
        byte v1[] = value;
        byte v2[] = anotherString.value;
        
        // 编码方式一致
        if(coder() == anotherString.coder()) {
            return isLatin1() ? StringLatin1.compareTo(v1, v2) : StringUTF16.compareTo(v1, v2);
        }
        
        // 编码方式不一致
        return isLatin1() ? StringLatin1.compareToUTF16(v1, v2) : StringUTF16.compareToLatin1(v1, v2);
    }
    
    /**
     * Compares two strings lexicographically, ignoring case differences.
     * This method returns an integer whose sign is that of calling {@code compareTo} with normalized versions
     * of the strings where case differences have been eliminated by calling {@code Character.toLowerCase(Character.toUpperCase(character))}
     * on each character.
     * <p>
     * Note that this method does <em>not</em> take locale into account,
     * and will result in an unsatisfactory ordering for certain locales.
     * The {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @param str the {@code String} to be compared.
     *
     * @return a negative integer, zero, or a positive integer as the specified String is greater than,
     * equal to, or less than this String, ignoring case considerations.
     *
     * @see java.text.Collator
     * @since 1.2
     */
    // 忽略大小写地比较两个String
    public int compareToIgnoreCase(String str) {
        return CASE_INSENSITIVE_ORDER.compare(this, str);
    }
    
    /**
     * Tests if two string regions are equal.
     * <p>
     * A substring of this {@code String} object is compared to a substring of the argument other.
     * The result is true if these substrings represent identical character sequences.
     * The substring of this {@code String} object to be compared begins at index {@code toffset} and has length {@code len}.
     * The substring of other to be compared begins at index {@code ooffset} and has length {@code len}.
     * The result is {@code false} if and only if at least one of the following is true:
     * <ul><li>{@code toffset} is negative.
     * <li>{@code ooffset} is negative.
     * <li>{@code toffset+len} is greater than the length of this
     * {@code String} object.
     * <li>{@code ooffset+len} is greater than the length of the other
     * argument.
     * <li>There is some nonnegative integer <i>k</i> less than {@code len}
     * such that: {@code this.charAt(toffset + }<i>k</i>{@code ) != other.charAt(ooffset + }
     * <i>k</i>{@code )}
     * </ul>
     *
     * <p>Note that this method does <em>not</em> take locale into account.  The
     * {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @param toffset the starting offset of the subregion in this string.
     * @param other   the string argument.
     * @param ooffset the starting offset of the subregion in the string argument.
     * @param len     the number of characters to compare.
     *
     * @return {@code true} if the specified subregion of this string exactly matches the specified subregion of the string argument;
     * {@code false} otherwise.
     */
    // 比较两个String指定的区域，区分大小写
    public boolean regionMatches(int toffset, String other, int ooffset, int len) {
        byte tv[] = value;
        byte ov[] = other.value;
        
        // Note: toffset, ooffset, or len might be near -1>>>1.
        if((ooffset < 0) || (toffset < 0) || (toffset > (long) length() - len) || (ooffset > (long) other.length() - len)) {
            return false;
        }
        
        if(coder() == other.coder()) {
            if(!isLatin1() && (len > 0)) {
                toffset = toffset << 1;
                ooffset = ooffset << 1;
                len = len << 1;
            }
            while(len-- > 0) {
                if(tv[toffset++] != ov[ooffset++]) {
                    return false;
                }
            }
        } else {
            if(coder() == LATIN1) {
                while(len-- > 0) {
                    if(StringLatin1.getChar(tv, toffset++) != StringUTF16.getChar(ov, ooffset++)) {
                        return false;
                    }
                }
            } else {
                while(len-- > 0) {
                    if(StringUTF16.getChar(tv, toffset++) != StringLatin1.getChar(ov, ooffset++)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Tests if two string regions are equal.
     * <p>
     * A substring of this {@code String} object is compared to a substring of the argument {@code other}.
     * The result is {@code true} if these substrings represent character sequences that are the same,
     * ignoring case if and only if {@code ignoreCase} is true.
     * The substring of this {@code String} object to be compared begins at index {@code toffset} and has length {@code len}.
     * The substring of {@code other} to be compared begins at index {@code ooffset} and has length {@code len}.
     * The result is {@code false} if and only if at least one of the following is true:
     * <ul><li>{@code toffset} is negative.
     * <li>{@code ooffset} is negative.
     * <li>{@code toffset+len} is greater than the length of this
     * {@code String} object.
     * <li>{@code ooffset+len} is greater than the length of the other
     * argument.
     * <li>{@code ignoreCase} is {@code false} and there is some nonnegative
     * integer <i>k</i> less than {@code len} such that:
     * <blockquote><pre>
     * this.charAt(toffset+k) != other.charAt(ooffset+k)
     * </pre></blockquote>
     * <li>{@code ignoreCase} is {@code true} and there is some nonnegative
     * integer <i>k</i> less than {@code len} such that:
     * <blockquote><pre>
     * Character.toLowerCase(Character.toUpperCase(this.charAt(toffset+k))) !=
     * Character.toLowerCase(Character.toUpperCase(other.charAt(ooffset+k)))
     * </pre></blockquote>
     * </ul>
     *
     * <p>Note that this method does <em>not</em> take locale into account,
     * and will result in unsatisfactory results for certain locales when {@code ignoreCase} is {@code true}.
     * The {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @param ignoreCase if {@code true}, ignore case when comparing characters.
     * @param toffset    the starting offset of the subregion in this string.
     * @param other      the string argument.
     * @param ooffset    the starting offset of the subregion in the string argument.
     * @param len        the number of characters to compare.
     *
     * @return {@code true} if the specified subregion of this string matches the specified subregion of the string argument;
     * {@code false} otherwise. Whether the matching is exact or case insensitive depends on the {@code ignoreCase} argument.
     */
    // 比较两个String指定的区域，是否忽略大小写由ignoreCase决定
    public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) {
        if(!ignoreCase) {
            return regionMatches(toffset, other, ooffset, len);
        }
        // Note: toffset, ooffset, or len might be near -1>>>1.
        if((ooffset < 0) || (toffset < 0) || (toffset > (long) length() - len) || (ooffset > (long) other.length() - len)) {
            return false;
        }
        byte tv[] = value;
        byte ov[] = other.value;
        if(coder() == other.coder()) {
            return isLatin1()
                ? StringLatin1.regionMatchesCI(tv, toffset, ov, ooffset, len)
                : StringUTF16.regionMatchesCI(tv, toffset, ov, ooffset, len);
        }
        return isLatin1()
            ? StringLatin1.regionMatchesCI_UTF16(tv, toffset, ov, ooffset, len)
            : StringUTF16.regionMatchesCI_Latin1(tv, toffset, ov, ooffset, len);
    }
    
    /*▲ 比较 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 判等 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Compares this string to the specified object.
     * The result is {@code true} if and only if the argument is not {@code null} and is a {@code String} object
     * that represents the same sequence of characters as this object.
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param anObject The object to compare this {@code String} against
     *
     * @return {@code true} if the given object represents a {@code String} equivalent to this string, {@code false} otherwise
     *
     * @see #compareTo(String)
     * @see #equalsIgnoreCase(String)
     */
    // 比较两个String的内容是否相等
    public boolean equals(Object anObject) {
        if(this == anObject) {
            return true;
        }
        if(anObject instanceof String) {
            String aString = (String) anObject;
            if(coder() == aString.coder()) {
                return isLatin1()
                    ? StringLatin1.equals(value, aString.value)
                    : StringUTF16.equals(value, aString.value);
            }
        }
        return false;
    }
    
    /**
     * Compares this {@code String} to another {@code String}, ignoring case considerations.
     * Two strings are considered equal ignoring case if they are of the same length and corresponding characters
     * in the two strings are equal ignoring case.
     *
     * <p> Two characters {@code c1} and {@code c2} are considered the same
     * ignoring case if at least one of the following is true:
     * <ul>
     * <li> The two characters are the same (as compared by the
     * {@code ==} operator)
     * <li> Calling {@code Character.toLowerCase(Character.toUpperCase(char))}
     * on each character produces the same result
     * </ul>
     *
     * <p>Note that this method does <em>not</em> take locale into account, and
     * will result in unsatisfactory results for certain locales.  The {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @param anotherString The {@code String} to compare this {@code String} against
     *
     * @return {@code true} if the argument is not {@code null} and it represents an equivalent {@code String} ignoring case; {@code false} otherwise
     *
     * @see #equals(Object)
     */
    // 忽略大小写地比较两个字符串是否相等
    public boolean equalsIgnoreCase(String anotherString) {
        return (this == anotherString)
            ? true
            : (anotherString != null) && (anotherString.length() == length()) && regionMatches(true, 0, anotherString, 0, length());
    }
    
    /**
     * Compares this string to the specified {@code StringBuffer}.
     * The result is {@code true} if and only if this {@code String} represents the same sequence of characters as the specified {@code StringBuffer}.
     * This method synchronizes on the {@code StringBuffer}.
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param sb The {@code StringBuffer} to compare this {@code String} against
     *
     * @return {@code true} if this {@code String} represents the same sequence of characters as the specified {@code StringBuffer},
     * {@code false} otherwise
     *
     * @since 1.4
     */
    // 比较String和StringBuffer的内容是否相等
    public boolean contentEquals(StringBuffer sb) {
        return contentEquals((CharSequence) sb);
    }
    
    /**
     * Compares this string to the specified {@code CharSequence}.
     * The result is {@code true} if and only if this {@code String} represents the same sequence of char values as the specified sequence.
     * Note that if the {@code CharSequence} is a {@code StringBuffer} then the method synchronizes on it.
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param cs The sequence to compare this {@code String} against
     *
     * @return {@code true} if this {@code String} represents the same sequence of char values as the specified sequence, {@code false} otherwise
     *
     * @since 1.5
     */
    // 比较String和CharSequence的内容是否相等
    public boolean contentEquals(CharSequence cs) {
        // Argument is a StringBuffer, StringBuilder
        if(cs instanceof AbstractStringBuilder) {
            if(cs instanceof StringBuffer) {
                synchronized(cs) {
                    return nonSyncContentEquals((AbstractStringBuilder) cs);
                }
            } else {
                return nonSyncContentEquals((AbstractStringBuilder) cs);
            }
        }
        
        // Argument is a String
        if(cs instanceof String) {
            return equals(cs);
        }
        
        // Argument is a generic CharSequence
        int n = cs.length();
        if(n != length()) {
            return false;
        }
        byte[] val = this.value;
        // 可以用压缩的Latin1字符集表示
        if(isLatin1()) {
            for(int i = 0; i < n; i++) {
                if((val[i] & 0xff) != cs.charAt(i)) {
                    return false;
                }
            }
        } else {
            return StringUTF16.contentEquals(val, cs, n);
        }
        
        return true;
    }
    
    // 判等。如果是UTF-16字符串和AbstractStringBuilder比较，则一定返回false。
    private boolean nonSyncContentEquals(AbstractStringBuilder sb) {
        int len = length();
        if(len != sb.length()) {
            return false;
        }
        byte v1[] = value;
        byte v2[] = sb.getValue();
        if(coder() == sb.getCoder()) {
            int n = v1.length;
            for(int i = 0; i < n; i++) {
                if(v1[i] != v2[i]) {
                    return false;
                }
            }
        } else {
            if(!isLatin1()) {  // utf16 str and latin1 abs can never be "equal"
                return false;
            }
            return StringUTF16.contentEquals(v1, v2, len);
        }
        return true;
    }
    
    /*▲ 判等 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 判空 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if, and only if, {@link #length()} is {@code 0}.
     *
     * @return {@code true} if {@link #length()} is {@code 0}, otherwise {@code false}
     *
     * @since 1.6
     */
    // true：String为空串
    public boolean isEmpty() {
        return value.length == 0;
    }
    
    /**
     * Returns {@code true} if the string is empty or contains only {@link Character#isWhitespace(int) white space} codepoints, otherwise {@code false}.
     *
     * @return {@code true} if the string is empty or contains only {@link Character#isWhitespace(int) white space} codepoints, otherwise {@code false}
     *
     * @see Character#isWhitespace(int)
     * @since 11
     */
    // true：String为空串，或仅包含空白符号
    public boolean isBlank() {
        return indexOfNonWhitespace() == length();
    }
    
    /*▲ 判空 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the length of this string.
     * The length is equal to the number of <a href="Character.html#unicode">Unicode code units</a> in the string.
     *
     * @return the length of the sequence of characters represented by this object.
     */
    // 返回String中包含的char的个数，但不一定是Unicode符号个数。
    public int length() {
        return value.length >> coder();
    }
    
    
    
    /**
     * Returns a canonical representation for the string object.
     * <p>
     * A pool of strings, initially empty, is maintained privately by the class {@code String}.
     * <p>
     * When the intern method is invoked,
     * if the pool already contains a string equal to this {@code String} object as determined by the {@link #equals(Object)} method,
     * then the string from the pool is returned. Otherwise,
     * this {@code String} object is added to the pool and a reference to this {@code String} object is returned.
     * <p>
     * It follows that for any two strings {@code s} and {@code t}, {@code s.intern() == t.intern()}
     * is {@code true} if and only if {@code s.equals(t)} is {@code true}.
     * <p>
     * All literal strings and string-valued constant expressions are interned. String literals are defined in section 3.10.5 of the
     * <cite>The Java&trade; Language Specification</cite>.
     *
     * @return a string that has the same contents as this string, but is guaranteed to be from a pool of unique strings.
     *
     * @jls 3.10.5 String Literals
     */
    // 在常量池中查找该字符串，如果找到，就返回常量池中等值字符串的地址，否则，就返回原地址。
    public native String intern();
    
    /**
     * Returns a string whose value is the concatenation of this string repeated {@code count} times.
     * <p>
     * If this string is empty or count is zero then the empty string is returned.
     *
     * @param count number of times to repeat
     *
     * @return A string composed of this string repeated {@code count} times or the empty string if this string is empty or count is zero
     *
     * @throws IllegalArgumentException if the {@code count} is negative.
     * @since 11
     */
    // 返回该字符串重复count次后的结果
    public String repeat(int count) {
        if(count < 0) {
            throw new IllegalArgumentException("count is negative: " + count);
        }
        
        if(count == 1) {
            return this;
        }
        
        final int len = value.length;
        if(len == 0 || count == 0) {
            return "";
        }
    
        if(len == 1) {
            final byte[] single = new byte[count];
            Arrays.fill(single, value[0]);
            return new String(single, coder);
        }
        
        if(Integer.MAX_VALUE / count < len) {
            throw new OutOfMemoryError("Repeating " + len + " bytes String " + count + " times will produce a String exceeding maximum size.");
        }
    
        final int limit = len * count;
        final byte[] multiple = new byte[limit];
        System.arraycopy(value, 0, multiple, 0, len);
        
        int copied = len;
        for(; copied < limit - copied; copied <<= 1) {
            System.arraycopy(multiple, 0, multiple, copied, copied);
        }
        
        System.arraycopy(multiple, 0, multiple, copied, limit - copied);
        
        return new String(multiple, coder);
    }
    
    
    
    /**
     * Returns a hash code for this string. The hash code for a {@code String} object is computed as
     * <blockquote><pre>
     * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
     * </pre></blockquote>
     * using {@code int} arithmetic, where {@code s[i]} is the
     * <i>i</i>th character of the string, {@code n} is the length of
     * the string, and {@code ^} indicates exponentiation. (The hash value of the empty string is zero.)
     *
     * @return a hash code value for this object.
     */
    // 计算String的哈希值，计算公式为：s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]。空串的哈希值为0。
    public int hashCode() {
        int h = hash;
        if(h == 0 && value.length>0) {
            hash = h = isLatin1() ? StringLatin1.hashCode(value) : StringUTF16.hashCode(value);
        }
        return h;
    }
    
    /**
     * This object (which is already a string!) is itself returned.
     *
     * @return the string itself.
     */
    public String toString() {
        return this;
    }
    
    
    
    /*▼ 越界检查 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    static void checkIndex(int index, int length) {
        if(index < 0 || index >= length) {
            throw new StringIndexOutOfBoundsException("index " + index + ",length " + length);
        }
    }
    
    /**
     * StringIndexOutOfBoundsException  if {@code offset} is negative or greater than {@code length}.
     */
    static void checkOffset(int offset, int length) {
        if(offset < 0 || offset > length) {
            throw new StringIndexOutOfBoundsException("offset " + offset + ",length " + length);
        }
    }
    
    /**
     * Check {@code offset}, {@code count} against {@code 0} and {@code length} bounds.
     *
     * @throws StringIndexOutOfBoundsException If {@code offset} is negative, {@code count} is negative,
     * or {@code offset} is greater than {@code length - count}
     */
    static void checkBoundsOffCount(int offset, int count, int length) {
        if(offset < 0 || count < 0 || offset > length - count) {
            throw new StringIndexOutOfBoundsException("offset " + offset + ", count " + count + ", length " + length);
        }
    }
    
    /**
     * Check {@code begin}, {@code end} against {@code 0} and {@code length} bounds.
     *
     * @throws StringIndexOutOfBoundsException If {@code begin} is negative,
     *                                         {@code begin} is greater than {@code end}, or {@code end} is greater than {@code length}.
     */
    static void checkBoundsBeginEnd(int begin, int end, int length) {
        if(begin<0 || begin>end || end>length) {
            throw new StringIndexOutOfBoundsException("begin " + begin + ", end " + end + ", length " + length);
        }
    }
    
    private static Void rangeCheck(char[] value, int offset, int count) {
        checkBoundsOffCount(offset, count, value.length);
        return null;
    }
    
    /*▲ 越界检查 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 如果字符串可压缩，则返回coder字段的值。否则，始终返回UTF-16。
    byte coder() {
        return COMPACT_STRINGS ? coder : UTF16;
    }
    
    // true：表示该字符串可以用压缩的Latin1字符集表示（一个字节对应了一个符号）
    private boolean isLatin1() {
        return COMPACT_STRINGS && coder == LATIN1;
    }
    
    /**
     * Copy character bytes from this string into dst starting at dstBegin.
     * This method doesn't perform any range checking.
     *
     * Invoker guarantees: dst is in UTF16 (inflate itself for asb),
     * if two coders are different, and dst is big enough (range check)
     *
     * @param dstBegin the char index, not offset of byte[]
     * @param coder    the coder of dst[]
     */
    // 拷贝String中的字节到dst数组
    void getBytes(byte dst[], int dstBegin, byte coder) {
        if(coder() == coder) {
            System.arraycopy(value, 0, dst, dstBegin << coder, value.length);
        } else {
            /* 如果两个coder不同，则将源字符串当做LATIN-String对待 */
            // 从LATIN-String内部的字节转为UTF16-String内部的字节
            StringLatin1.inflate(value, 0, dst, dstBegin, value.length);
        }
    }
    
    // 返回存储String的byte数组
    byte[] value() {
        return value;
    }
    
    /**
     * Returns the string representation of the {@code codePoint} argument.
     *
     * @param codePoint a {@code codePoint}.
     *
     * @return a string of length {@code 1} or {@code 2} containing as its single character the argument {@code codePoint}.
     *
     * @throws IllegalArgumentException if the specified {@code codePoint} is not a {@linkplain Character#isValidCodePoint valid Unicode code point}.
     */
    // 转换Unicode符号的codePoint为字节表示，再包装到String返回
    static String valueOfCodePoint(int codePoint) {
        // 先将码点值编码为byte数组，再将其解码为String序列
        
        if(COMPACT_STRINGS && StringLatin1.canEncode(codePoint)) {
            // 解码单字节符号
            return new String(StringLatin1.toBytes((char) codePoint), LATIN1);
        } else if(Character.isBmpCodePoint(codePoint)) {
            // 解码双字节符号
            return new String(StringUTF16.toBytes((char) codePoint), UTF16);
        } else if(Character.isSupplementaryCodePoint(codePoint)) {
            // 解码四字节符号
            return new String(StringUTF16.toBytesSupplementary(codePoint), UTF16);
        }
        
        throw new IllegalArgumentException(format("Not a valid Unicode code point: 0x%X", codePoint));
    }
    
    /**
     * Code shared by String and AbstractStringBuilder to do searches.
     * The source is the character array being searched, and the target is the string being searched for.
     *
     * @param src       the characters being searched.
     * @param srcCoder  the coder of the source string.
     * @param srcCount  length of the source string.
     * @param tgtStr    the characters being searched for.
     * @param fromIndex the index to begin searching from.
     */
    // 返回子串tgstr在主串src中第一次出现的下标（从主串fromIndex处向后搜索）
    static int indexOf(byte[] src, byte srcCoder, int srcCount, String tgtStr, int fromIndex) {
        byte[] tgt = tgtStr.value;
        byte tgtCoder = tgtStr.coder();
        int tgtCount = tgtStr.length();
        
        if(fromIndex >= srcCount) {
            return (tgtCount == 0 ? srcCount : -1);
        }
        if(fromIndex<0) {
            fromIndex = 0;
        }
        if(tgtCount == 0) {
            return fromIndex;
        }
        if(tgtCount>srcCount) {
            return -1;
        }
        if(srcCoder == tgtCoder) {
            return srcCoder == LATIN1 ? StringLatin1.indexOf(src, srcCount, tgt, tgtCount, fromIndex) : StringUTF16.indexOf(src, srcCount, tgt, tgtCount, fromIndex);
        }
        if(srcCoder == LATIN1) {    //  && tgtCoder == UTF16
            return -1;
        }
        // 比对UTF16-String主串src和Latin1子串tgt，返回子串str在主串src中第一次出现的位置，加入了范围检查
        return StringUTF16.indexOfLatin1(src, srcCount, tgt, tgtCount, fromIndex);
    }
    
    /**
     * Code shared by String and AbstractStringBuilder to do searches.
     * The source is the character array being searched, and the target is the string being searched for.
     *
     * @param src       the characters being searched.
     * @param srcCoder  coder handles the mapping between bytes/chars
     * @param srcCount  count of the source string.
     * @param fromIndex the index to begin searching from.
     */
    // 返回子串tgtStr在主串src中最后一次出现的下标（从主串fromIndex处向前搜索）
    static int lastIndexOf(byte[] src, byte srcCoder, int srcCount, String tgtStr, int fromIndex) {
        byte[] tgt = tgtStr.value;
        byte tgtCoder = tgtStr.coder();
        int tgtCount = tgtStr.length();
        
        // Check arguments; return immediately where possible. For consistency, don't check for null str.
        int rightIndex = srcCount - tgtCount;
        if(fromIndex>rightIndex) {
            fromIndex = rightIndex;
        }
        if(fromIndex<0) {
            return -1;
        }
        /* Empty string always matches. */
        if(tgtCount == 0) {
            return fromIndex;
        }
        if(srcCoder == tgtCoder) {
            return srcCoder == LATIN1 ? StringLatin1.lastIndexOf(src, srcCount, tgt, tgtCount, fromIndex) : StringUTF16.lastIndexOf(src, srcCount, tgt, tgtCount, fromIndex);
        }
        if(srcCoder == LATIN1) {    // && tgtCoder == UTF16
            return -1;
        }
        // 比对UTF16-String主串src和Latin1子串tgt，返回子串str在主串src中最后一次出现的位置
        return StringUTF16.lastIndexOfLatin1(src, srcCount, tgt, tgtCount, fromIndex);
    }
    
    // 返回String起始处首个非空白字符的索引
    private int indexOfNonWhitespace() {
        // 可以用压缩的Latin1字符集表示
        if(isLatin1()) {
            return StringLatin1.indexOfNonWhitespace(value);
        } else {
            return StringUTF16.indexOfNonWhitespace(value);
        }
    }
    
    
    // 字符串比较器
    private static class CaseInsensitiveComparator implements Comparator<String>, Serializable {
        // use serialVersionUID from JDK 1.2.2 for interoperability
        private static final long serialVersionUID = 8575799808933029326L;
        
        // 忽略大小写地比较两个字符串
        public int compare(String s1, String s2) {
            byte v1[] = s1.value;
            byte v2[] = s2.value;
            if(s1.coder() == s2.coder()) {
                return s1.isLatin1() ? StringLatin1.compareToCI(v1, v2)
                    : StringUTF16.compareToCI(v1, v2);
            }
            return s1.isLatin1() ? StringLatin1.compareToCI_UTF16(v1, v2) : StringUTF16.compareToCI_Latin1(v1, v2);
        }
        
        /**
         * Replaces the de-serialized object.
         */
        private Object readResolve() {
            return CASE_INSENSITIVE_ORDER;
        }
    }
    
}
