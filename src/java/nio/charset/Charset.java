/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.charset;

import jdk.internal.misc.VM;
import sun.nio.cs.ThreadLocalCoders;
import sun.security.action.GetPropertyAction;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.spi.CharsetProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A named mapping between sequences of sixteen-bit Unicode <a
 * href="../../lang/Character.html#unicode">code units</a> and sequences of
 * bytes.  This class defines methods for creating decoders and encoders and
 * for retrieving the various names associated with a charset.  Instances of
 * this class are immutable.
 *
 * <p> This class also defines static methods for testing whether a particular
 * charset is supported, for locating charset instances by name, and for
 * constructing a map that contains every charset for which support is
 * available in the current Java virtual machine.  Support for new charsets can
 * be added via the service-provider interface defined in the {@link
 * CharsetProvider} class.
 *
 * <p> All of the methods defined in this class are safe for use by multiple
 * concurrent threads.
 *
 *
 * <a id="names"></a><a id="charenc"></a>
 * <h2>Charset names</h2>
 *
 * <p> Charsets are named by strings composed of the following characters:
 *
 * <ul>
 *
 * <li> The uppercase letters {@code 'A'} through {@code 'Z'}
 * (<code>'&#92;u0041'</code>&nbsp;through&nbsp;<code>'&#92;u005a'</code>),
 *
 * <li> The lowercase letters {@code 'a'} through {@code 'z'}
 * (<code>'&#92;u0061'</code>&nbsp;through&nbsp;<code>'&#92;u007a'</code>),
 *
 * <li> The digits {@code '0'} through {@code '9'}
 * (<code>'&#92;u0030'</code>&nbsp;through&nbsp;<code>'&#92;u0039'</code>),
 *
 * <li> The dash character {@code '-'}
 * (<code>'&#92;u002d'</code>,&nbsp;<small>HYPHEN-MINUS</small>),
 *
 * <li> The plus character {@code '+'}
 * (<code>'&#92;u002b'</code>,&nbsp;<small>PLUS SIGN</small>),
 *
 * <li> The period character {@code '.'}
 * (<code>'&#92;u002e'</code>,&nbsp;<small>FULL STOP</small>),
 *
 * <li> The colon character {@code ':'}
 * (<code>'&#92;u003a'</code>,&nbsp;<small>COLON</small>), and
 *
 * <li> The underscore character {@code '_'}
 * (<code>'&#92;u005f'</code>,&nbsp;<small>LOW&nbsp;LINE</small>).
 *
 * </ul>
 *
 * A charset name must begin with either a letter or a digit.  The empty string
 * is not a legal charset name.  Charset names are not case-sensitive; that is,
 * case is always ignored when comparing charset names.  Charset names
 * generally follow the conventions documented in <a
 * href="http://www.ietf.org/rfc/rfc2278.txt"><i>RFC&nbsp;2278:&nbsp;IANA Charset
 * Registration Procedures</i></a>.
 *
 * <p> Every charset has a <i>canonical name</i> and may also have one or more
 * <i>aliases</i>.  The canonical name is returned by the {@link #name() name} method
 * of this class.  Canonical names are, by convention, usually in upper case.
 * The aliases of a charset are returned by the {@link #aliases() aliases}
 * method.
 *
 * <p><a id="hn">Some charsets have an <i>historical name</i> that is defined for
 * compatibility with previous versions of the Java platform.</a>  A charset's
 * historical name is either its canonical name or one of its aliases.  The
 * historical name is returned by the {@code getEncoding()} methods of the
 * {@link java.io.InputStreamReader#getEncoding InputStreamReader} and {@link
 * java.io.OutputStreamWriter#getEncoding OutputStreamWriter} classes.
 *
 * <p><a id="iana"> </a>If a charset listed in the <a
 * href="http://www.iana.org/assignments/character-sets"><i>IANA Charset
 * Registry</i></a> is supported by an implementation of the Java platform then
 * its canonical name must be the name listed in the registry. Many charsets
 * are given more than one name in the registry, in which case the registry
 * identifies one of the names as <i>MIME-preferred</i>.  If a charset has more
 * than one registry name then its canonical name must be the MIME-preferred
 * name and the other names in the registry must be valid aliases.  If a
 * supported charset is not listed in the IANA registry then its canonical name
 * must begin with one of the strings {@code "X-"} or {@code "x-"}.
 *
 * <p> The IANA charset registry does change over time, and so the canonical
 * name and the aliases of a particular charset may also change over time.  To
 * ensure compatibility it is recommended that no alias ever be removed from a
 * charset, and that if the canonical name of a charset is changed then its
 * previous canonical name be made into an alias.
 *
 *
 * <h2>Standard charsets</h2>
 *
 *
 *
 * <p><a id="standard">Every implementation of the Java platform is required to support the
 * following standard charsets.</a>  Consult the release documentation for your
 * implementation to see if any other charsets are supported.  The behavior
 * of such optional charsets may differ between implementations.
 *
 * <blockquote><table class="striped" style="width:80%">
 * <caption style="display:none">Description of standard charsets</caption>
 * <thead>
 * <tr><th scope="col" style="text-align:left">Charset</th><th scope="col" style="text-align:left">Description</th></tr>
 * </thead>
 * <tbody>
 * <tr><th scope="row" style="vertical-align:top">{@code US-ASCII}</th>
 * <td>Seven-bit ASCII, a.k.a. {@code ISO646-US},
 * a.k.a. the Basic Latin block of the Unicode character set</td></tr>
 * <tr><th scope="row" style="vertical-align:top"><code>ISO-8859-1&nbsp;&nbsp;</code></th>
 * <td>ISO Latin Alphabet No. 1, a.k.a. {@code ISO-LATIN-1}</td></tr>
 * <tr><th scope="row" style="vertical-align:top">{@code UTF-8}</th>
 * <td>Eight-bit UCS Transformation Format</td></tr>
 * <tr><th scope="row" style="vertical-align:top">{@code UTF-16BE}</th>
 * <td>Sixteen-bit UCS Transformation Format,
 * big-endian byte&nbsp;order</td></tr>
 * <tr><th scope="row" style="vertical-align:top">{@code UTF-16LE}</th>
 * <td>Sixteen-bit UCS Transformation Format,
 * little-endian byte&nbsp;order</td></tr>
 * <tr><th scope="row" style="vertical-align:top">{@code UTF-16}</th>
 * <td>Sixteen-bit UCS Transformation Format,
 * byte&nbsp;order identified by an optional byte-order mark</td></tr>
 * </tbody>
 * </table></blockquote>
 *
 * <p> The {@code UTF-8} charset is specified by <a
 * href="http://www.ietf.org/rfc/rfc2279.txt"><i>RFC&nbsp;2279</i></a>; the
 * transformation format upon which it is based is specified in
 * Amendment&nbsp;2 of ISO&nbsp;10646-1 and is also described in the <a
 * href="http://www.unicode.org/unicode/standard/standard.html"><i>Unicode
 * Standard</i></a>.
 *
 * <p> The {@code UTF-16} charsets are specified by <a
 * href="http://www.ietf.org/rfc/rfc2781.txt"><i>RFC&nbsp;2781</i></a>; the
 * transformation formats upon which they are based are specified in
 * Amendment&nbsp;1 of ISO&nbsp;10646-1 and are also described in the <a
 * href="http://www.unicode.org/unicode/standard/standard.html"><i>Unicode
 * Standard</i></a>.
 *
 * <p> The {@code UTF-16} charsets use sixteen-bit quantities and are
 * therefore sensitive to byte order.  In these encodings the byte order of a
 * stream may be indicated by an initial <i>byte-order mark</i> represented by
 * the Unicode character <code>'&#92;uFEFF'</code>.  Byte-order marks are handled
 * as follows:
 *
 * <ul>
 *
 * <li><p> When decoding, the {@code UTF-16BE} and {@code UTF-16LE}
 * charsets interpret the initial byte-order marks as a <small>ZERO-WIDTH
 * NON-BREAKING SPACE</small>; when encoding, they do not write
 * byte-order marks. </p></li>
 *
 *
 * <li><p> When decoding, the {@code UTF-16} charset interprets the
 * byte-order mark at the beginning of the input stream to indicate the
 * byte-order of the stream but defaults to big-endian if there is no
 * byte-order mark; when encoding, it uses big-endian byte order and writes
 * a big-endian byte-order mark. </p></li>
 *
 * </ul>
 *
 * In any case, byte order marks occurring after the first element of an
 * input sequence are not omitted since the same code is used to represent
 * <small>ZERO-WIDTH NON-BREAKING SPACE</small>.
 *
 * <p> Every instance of the Java virtual machine has a default charset, which
 * may or may not be one of the standard charsets.  The default charset is
 * determined during virtual-machine startup and typically depends upon the
 * locale and charset being used by the underlying operating system. </p>
 *
 * <p>The {@link StandardCharsets} class defines constants for each of the
 * standard charsets.
 *
 * <h2>Terminology</h2>
 *
 * <p> The name of this class is taken from the terms used in
 * <a href="http://www.ietf.org/rfc/rfc2278.txt"><i>RFC&nbsp;2278</i></a>.
 * In that document a <i>charset</i> is defined as the combination of
 * one or more coded character sets and a character-encoding scheme.
 * (This definition is confusing; some other software systems define
 * <i>charset</i> as a synonym for <i>coded character set</i>.)
 *
 * <p> A <i>coded character set</i> is a mapping between a set of abstract
 * characters and a set of integers.  US-ASCII, ISO&nbsp;8859-1,
 * JIS&nbsp;X&nbsp;0201, and Unicode are examples of coded character sets.
 *
 * <p> Some standards have defined a <i>character set</i> to be simply a
 * set of abstract characters without an associated assigned numbering.
 * An alphabet is an example of such a character set.  However, the subtle
 * distinction between <i>character set</i> and <i>coded character set</i>
 * is rarely used in practice; the former has become a short form for the
 * latter, including in the Java API specification.
 *
 * <p> A <i>character-encoding scheme</i> is a mapping between one or more
 * coded character sets and a set of octet (eight-bit byte) sequences.
 * UTF-8, UTF-16, ISO&nbsp;2022, and EUC are examples of
 * character-encoding schemes.  Encoding schemes are often associated with
 * a particular coded character set; UTF-8, for example, is used only to
 * encode Unicode.  Some schemes, however, are associated with multiple
 * coded character sets; EUC, for example, can be used to encode
 * characters in a variety of Asian coded character sets.
 *
 * <p> When a coded character set is used exclusively with a single
 * character-encoding scheme then the corresponding charset is usually
 * named for the coded character set; otherwise a charset is usually named
 * for the encoding scheme and, possibly, the locale of the coded
 * character sets that it supports.  Hence {@code US-ASCII} is both the
 * name of a coded character set and of the charset that encodes it, while
 * {@code EUC-JP} is the name of the charset that encodes the
 * JIS&nbsp;X&nbsp;0201, JIS&nbsp;X&nbsp;0208, and JIS&nbsp;X&nbsp;0212
 * coded character sets for the Japanese language.
 *
 * <p> The native character encoding of the Java programming language is
 * UTF-16.  A charset in the Java platform therefore defines a mapping
 * between sequences of sixteen-bit UTF-16 code units (that is, sequences
 * of chars) and sequences of bytes. </p>
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @see CharsetDecoder
 * @see CharsetEncoder
 * @see CharsetProvider
 * @see Character
 * @since 1.4
 */

// 字符集抽象基类，主要封装了查找、创建字符集实例，以及对字符序列进行编码与解码的操作
public abstract class Charset implements Comparable<Charset> {
    // 标准字符集
    private static final CharsetProvider standardProvider = new sun.nio.cs.StandardCharsets();
    
    private static final String[] zeroAliases = new String[0];
    
    // 缓存最近查找/使用的Charset
    private static volatile Object[] cache1; // "Level 1" cache
    private static volatile Object[] cache2; // "Level 2" cache
    
    // Thread-local gate to prevent recursive provider lookups
    private static ThreadLocal<ThreadLocal<?>> gate = new ThreadLocal<>();
    
    private static volatile Charset defaultCharset;
    
    private final String name;          // tickles a bug in oldjavac
    private final String[] aliases;     // tickles a bug in oldjavac
    private Set<String> aliasSet = null;
    
    
    /**
     * Initializes a new charset with the given canonical name and alias set.
     *
     * @param canonicalName The canonical name of this charset
     * @param aliases       An array of this charset's aliases, or null if it has no aliases
     *
     * @throws IllegalCharsetNameException If the canonical name or any of the aliases are illegal
     */
    // 初始化[规范名-别名集合]组成的字符集实例
    protected Charset(String canonicalName, String[] aliases) {
        String[] as = Objects.requireNonNullElse(aliases, zeroAliases);
        
        // Skip checks for the standard, built-in Charsets we always load during initialization.
        if(canonicalName != "ISO-8859-1" && canonicalName != "US-ASCII" && canonicalName != "UTF-8") {
            // 检查给定的字符集规范名是否符合命名规范
            checkName(canonicalName);
            for(String s : as) {
                // 检查给定的字符集别名是否符合命名规范
                checkName(s);
            }
        }
        
        this.name = canonicalName;
        this.aliases = as;
    }
    
    
    
    /*▼ 查找/创建字符集 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the default charset of this Java virtual machine.
     *
     * The default charset is determined during virtual-machine startup and typically depends upon the locale and charset of the underlying operating system.
     *
     * @return A charset object for the default charset
     *
     * @since 1.5
     */
    /*
     * 返回Java虚拟机的默认字符集。
     * 默认字符集在虚拟机启动期间确定，通常取决于底层操作系统的区域设置和字符集。
     */
    public static Charset defaultCharset() {
        if(defaultCharset == null) {
            synchronized(Charset.class) {
                // 从系统属性file.encoding中获取字符集信息
                String csn = GetPropertyAction.privilegedGetProperty("file.encoding");
                Charset cs = lookup(csn);
                if(cs != null) {
                    defaultCharset = cs;
                } else {
                    defaultCharset = sun.nio.cs.UTF_8.INSTANCE;
                }
            }
        }
        return defaultCharset;
    }
    
    /**
     * Returns a charset object for the named charset.
     *
     * @param charsetName The name of the requested charset; may be either
     *                    a canonical name or an alias
     *
     * @return A charset object for the named charset
     *
     * @throws IllegalCharsetNameException If the given charset name is illegal
     * @throws IllegalArgumentException    If the given {@code charsetName} is null
     * @throws UnsupportedCharsetException If no support for the named charset is available
     *                                     in this instance of the Java virtual machine
     */
    // true：返回查找到的字符集（如果不存在则异常）
    public static Charset forName(String charsetName) {
        // 返回查找到的字符集
        Charset cs = lookup(charsetName);
        if(cs != null)
            return cs;
        throw new UnsupportedCharsetException(charsetName);
    }
    
    /**
     * 从此处开始字符集的查找，查找大致分两步：
     *   第一步：查找Charset中的缓存（有一级缓存和二级缓存）；
     *     1.1 先访问一级缓存cache1
     *     1.2 再访问二级缓存cache2
     *   第二步：通过访问CharsetProvider（字符集提供商）来查找字符集：
     *     2.1 在StandardCharsets（标准"字符集"提供商）中查找字符集；
     *     2.2 在ExtendedCharsets（扩展"字符集"提供商）中查找字符集；
     *     2.3 在自定义的字符集提供商（可注册给ServiceLoader）中查找字符集。
     * 在字符集提供商中查找时，往往也要先查找其内部的缓存，其他流程差不多。
     *
     * @param charsetName 待查找的字符集名称
     * @return 返回查找结果
     */
    // 返回查找到的字符集
    private static Charset lookup(String charsetName) {
        if(charsetName == null)
            throw new IllegalArgumentException("Null charset name");
        Object[] a;
        if((a = cache1) != null && charsetName.equals(a[0])){
            // 在一级缓存中找到了匹配的字符集，直接返回
            return (Charset) a[1];
        }
        // We expect most programs to use one Charset repeatedly.
        // We convey a hint to this effect to the VM by putting the level 1 cache miss code in a separate method.
        return lookup2(charsetName);
    }
    
    // 返回查找到的字符集，在Charset的一级缓存中未找到时会执行到此步
    private static Charset lookup2(String charsetName) {
        Object[] a;
        
        // 在二级缓存中找到了匹配的字符集
        if((a = cache2) != null && charsetName.equals(a[0])) {
            // 交换两个缓存中的字符集（包装一级缓存中存的是最近使用的字符集）
            cache2 = cache1;
            cache1 = a;
            return (Charset) a[1];
        }
        
        // 在Charset的而级缓存中也未找到时会执行到此步
        Charset cs;
        if((cs = standardProvider.charsetForName(charsetName)) != null  // 在StandardCharsets中查找
            || (cs = lookupExtendedCharset(charsetName)) != null        // 在ExtendedCharsets中查找
            || (cs = lookupViaProviders(charsetName)) != null) {        // 在自定义的字符集提供商中查找
            // 如果找到了字符集，将其存储到Charset的内部缓存中
            cache(charsetName, cs);
            return cs;
        }
        
        // 只有在上面的查找过程中找不到字符集时，才会执行到此，检查给定的字符集名称是否符合命名规范
        checkName(charsetName);
        
        return null;
    }
    
    // 在ExtendedCharsets中查找相应的字符集
    private static Charset lookupExtendedCharset(String charsetName) {
        if(!VM.isBooted())  // see lookupViaProviders()
            return null;
        // 返回装载的ExtendedProvider
        CharsetProvider[] ecps = ExtendedProviderHolder.extendedProviders;
        for(CharsetProvider cp : ecps) {
            Charset cs = cp.charsetForName(charsetName);
            if(cs != null)
                return cs;
        }
        return null;
    }
    
    // 在自定义的字符集提供商中查找相应的字符集
    private static Charset lookupViaProviders(final String charsetName) {
        
        // The runtime startup sequence looks up standard charsets as a consequence of the VM's invocation of System.initializeSystemClass in order to,
        // e.g., set system properties and encode filenames.
        // At that point the application class loader has not been initialized,
        // however, so we can't look for providers because doing so will cause that loader to be prematurely initialized with incomplete information.
        if(!VM.isBooted())
            return null;
        
        if(gate.get() != null)
            // Avoid recursive provider lookups
            return null;
        
        try {
            gate.set(gate);
            
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                public Charset run() {
                    Iterator<CharsetProvider> i = providers();
                    while(i.hasNext()) {
                        CharsetProvider cp = i.next();
                        Charset cs = cp.charsetForName(charsetName);
                        if(cs != null)
                            return cs;
                    }
                    return null;
                }
            });
        } finally {
            gate.set(null);
        }
    }
    
    // 将查找字符集过程中找到的字符集存储到Charset的内部缓存中
    private static void cache(String charsetName, Charset cs) {
        cache2 = cache1;
        cache1 = new Object[]{charsetName, cs};
    }
    
    /*▲ 查找/创建字符集 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字符集提供商 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs a sorted map from canonical charset names to charset objects.
     *
     * <p> The map returned by this method will have one entry for each charset
     * for which support is available in the current Java virtual machine.  If
     * two or more supported charsets have the same canonical name then the
     * resulting map will contain just one of them; which one it will contain
     * is not specified. </p>
     *
     * <p> The invocation of this method, and the subsequent use of the
     * resulting map, may cause time-consuming disk or network I/O operations
     * to occur.  This method is provided for applications that need to
     * enumerate all of the available charsets, for example to allow user
     * charset selection.  This method is not used by the {@link #forName
     * forName} method, which instead employs an efficient incremental lookup
     * algorithm.
     *
     * <p> This method may return different results at different times if new
     * charset providers are dynamically made available to the current Java
     * virtual machine.  In the absence of such changes, the charsets returned
     * by this method are exactly those that can be retrieved via the {@link
     * #forName forName} method.  </p>
     *
     * @return An immutable, case-insensitive map from canonical charset names
     * to charset objects
     */
    // 返回一个有序映射，该映射中包含了所有可以找到的安全的字符集提供商
    public static SortedMap<String, Charset> availableCharsets() {
        return AccessController.doPrivileged(new PrivilegedAction<>() {
            public SortedMap<String, Charset> run() {
                TreeMap<String, Charset> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                
                // 在StandardCharsets中查找系统支持的字符集，并将其存入映射m，忽略映射中已经存在的字符集。
                put(standardProvider.charsets(), m);
                
                // 返回装载的ExtendedProvider，存入映射m
                CharsetProvider[] ecps = ExtendedProviderHolder.extendedProviders;
                for(CharsetProvider ecp : ecps) {
                    // 将扩展支持的字符集存入m，忽略映射中已经存在的字符集。
                    put(ecp.charsets(), m);
                }
                
                // 返回装载的自定义数据集提供商，存入映射m
                for(Iterator<CharsetProvider> i = providers(); i.hasNext(); ) {
                    CharsetProvider cp = i.next();
                    
                    put(cp.charsets(), m);
                }
                
                return Collections.unmodifiableSortedMap(m);
            }
        });
    }
    
    /**
     * Creates an iterator that walks over the available providers, ignoring those whose lookup or instantiation causes a security exception to be thrown.
     * Should be invoked with full privileges.
     */
    // 创建一个遍历自定义数据集提供商的迭代器，忽略那些存在安全隐患的数据集提供商。
    private static Iterator<CharsetProvider> providers() {
        return new Iterator<>() {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            ServiceLoader<CharsetProvider> sl = ServiceLoader.load(CharsetProvider.class, cl);
            Iterator<CharsetProvider> i = sl.iterator();
            CharsetProvider next = null;
            
            public boolean hasNext() {
                return getNext();
            }
            
            public CharsetProvider next() {
                if(!getNext())
                    throw new NoSuchElementException();
                CharsetProvider n = next;
                next = null;
                return n;
            }
            
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
            private boolean getNext() {
                while(next == null) {
                    try {
                        if(!i.hasNext())
                            return false;
                        next = i.next();
                    } catch(ServiceConfigurationError sce) {
                        if(sce.getCause() instanceof SecurityException) {
                            // Ignore security exceptions
                            continue;
                        }
                        throw sce;
                    }
                }
                return true;
            }
            
        };
    }
    
    // 将给定迭代器中包含的字符集存储到指定映射中，忽略映射中已经存在的字符集。
    private static void put(Iterator<Charset> i, Map<String, Charset> m) {
        while(i.hasNext()) {
            // 返回迭代器中规范名对应的字符集实例[参见StandardCharsets]
            Charset cs = i.next();
            if(!m.containsKey(cs.name()))   // 如果m中不包含此字符集名称，则存入该字符集实例
                m.put(cs.name(), cs);
        }
    }
    
    /*▲ 字符集提供商 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 编码/解码 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs a new encoder for this charset.
     *
     * @return A new encoder for this charset
     *
     * @throws UnsupportedOperationException If this charset does not support encoding
     */
    // 返回当前字符集实现类的一个字符编码器
    public abstract CharsetEncoder newEncoder();
    
    /**
     * Constructs a new decoder for this charset.
     *
     * @return A new decoder for this charset
     */
    // 返回当前字符集实现类的一个字符解码器
    public abstract CharsetDecoder newDecoder();
    
    /**
     * Tells whether or not this charset supports encoding.
     *
     * <p> Nearly all charsets support encoding.  The primary exceptions are
     * special-purpose <i>auto-detect</i> charsets whose decoders can determine
     * which of several possible encoding schemes is in use by examining the
     * input byte sequence.  Such charsets do not support encoding because
     * there is no way to determine which encoding should be used on output.
     * Implementations of such charsets should override this method to return
     * {@code false}. </p>
     *
     * @return {@code true} if, and only if, this charset supports encoding
     */
    /*
     * 表示这个字符集是否允许编码。
     * 几乎所有的字符集都支持编码。
     * 主要的例外情况是带有解码器的字符集，它们可以检测字节序列是如何编码的，并且之后会选择一个合适的解码方案。
     * 这些字符集通常只支持解码并且不创建自己的编码。
     */
    public boolean canEncode() {
        return true;
    }
    
    /**
     * Convenience method that encodes Unicode characters into bytes in this
     * charset.
     *
     * <p> An invocation of this method upon a charset {@code cs} returns the
     * same result as the expression
     *
     * <pre>
     *     cs.newEncoder()
     *       .onMalformedInput(CodingErrorAction.REPLACE)
     *       .onUnmappableCharacter(CodingErrorAction.REPLACE)
     *       .encode(bb); </pre>
     *
     * except that it is potentially more efficient because it can cache
     * encoders between successive invocations.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.  In order to
     * detect such sequences, use the {@link
     * CharsetEncoder#encode(CharBuffer)} method directly.  </p>
     *
     * @param cb The char buffer to be encoded
     *
     * @return A byte buffer containing the encoded characters
     */
    // 编码字符序列cb，返回编码后的字节序列
    public final ByteBuffer encode(CharBuffer cb) {
        try {
            return ThreadLocalCoders.encoderFor(this)   // 生成编码器
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .encode(cb);    // 开始编码
        } catch(CharacterCodingException x) {
            throw new Error(x);         // Can't happen
        }
    }
    
    /**
     * Convenience method that encodes a string into bytes in this charset.
     *
     * <p> An invocation of this method upon a charset {@code cs} returns the
     * same result as the expression
     *
     * <pre>
     *     cs.encode(CharBuffer.wrap(s)); </pre>
     *
     * @param str The string to be encoded
     *
     * @return A byte buffer containing the encoded characters
     */
    // 编码字符序列str，返回编码后的字节序列
    public final ByteBuffer encode(String str) {
        return encode(CharBuffer.wrap(str));
    }
    
    /**
     * Convenience method that decodes bytes in this charset into Unicode characters.
     *
     * <p> An invocation of this method upon a charset {@code cs} returns the
     * same result as the expression
     *
     * <pre>
     *     cs.newDecoder()
     *       .onMalformedInput(CodingErrorAction.REPLACE)
     *       .onUnmappableCharacter(CodingErrorAction.REPLACE)
     *       .decode(bb); </pre>
     *
     * except that it is potentially more efficient because it can cache
     * decoders between successive invocations.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement byte array.  In order
     * to detect such sequences, use the {@link
     * CharsetDecoder#decode(ByteBuffer)} method directly.  </p>
     *
     * @param bb The byte buffer to be decoded
     *
     * @return A char buffer containing the decoded characters
     */
    // 解码字节序列bb，返回解码后的字符序列
    public final CharBuffer decode(ByteBuffer bb) {
        try {
            return ThreadLocalCoders.decoderFor(this)   // 生成解码器
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .decode(bb);    // 开始解码
        } catch(CharacterCodingException x) {
            throw new Error(x);         // Can't happen
        }
    }
    
    /*▲ 编码/解码 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns this charset's canonical name.
     *
     * @return The canonical name of this charset
     */
    // 返回当前字符集的规范名称
    public final String name() {
        return name;
    }
    
    /**
     * Returns a set containing this charset's aliases.
     *
     * @return An immutable set of this charset's aliases
     */
    // 返回当前字符集支持的别名
    public final Set<String> aliases() {
        if(aliasSet != null)
            return aliasSet;
        int n = aliases.length;
        HashSet<String> hs = new HashSet<>(n);
        Collections.addAll(hs, aliases);
        aliasSet = Collections.unmodifiableSet(hs);
        return aliasSet;
    }
    
    /**
     * Returns this charset's human-readable name for the default locale.
     *
     * <p> The default implementation of this method simply returns this
     * charset's canonical name.  Concrete subclasses of this class may
     * override this method in order to provide a localized display name. </p>
     *
     * @return The display name of this charset in the default locale
     */
    // 返回当前字符集规范名在默认语言环境中的可读名称，一般等同于规范名
    public String displayName() {
        return name;
    }
    
    /**
     * Returns this charset's human-readable name for the given locale.
     *
     * <p> The default implementation of this method simply returns this
     * charset's canonical name.  Concrete subclasses of this class may
     * override this method in order to provide a localized display name. </p>
     *
     * @param locale The locale for which the display name is to be retrieved
     *
     * @return The display name of this charset in the given locale
     */
    // 返回当前字符集别名在默认语言环境中的可读名称，一般等同于别名
    public String displayName(Locale locale) {
        return name;
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Checks that the given string is a legal charset name. </p>
     *
     * @param s A purported charset name
     *
     * @throws IllegalCharsetNameException If the given name is not a legal charset name
     */
    // 检查给定的字符集名称（可能是规范名或别名）是否满足命名规范
    private static void checkName(String s) {
        int n = s.length();
        if(n == 0) {
            throw new IllegalCharsetNameException(s);
        }
        for(int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if(c >= 'A' && c <= 'Z')
                continue;
            if(c >= 'a' && c <= 'z')
                continue;
            if(c >= '0' && c <= '9')
                continue;
            if(c == '-' && i != 0)
                continue;
            if(c == '+' && i != 0)
                continue;
            if(c == ':' && i != 0)
                continue;
            if(c == '_' && i != 0)
                continue;
            if(c == '.' && i != 0)
                continue;
            throw new IllegalCharsetNameException(s);
        }
    }
    
    /**
     * Tells whether the named charset is supported.
     *
     * @param charsetName The name of the requested charset; may be either
     *                    a canonical name or an alias
     *
     * @return {@code true} if, and only if, support for the named charset
     * is available in the current Java virtual machine
     *
     * @throws IllegalCharsetNameException If the given charset name is illegal
     * @throws IllegalArgumentException    If the given {@code charsetName} is null
     */
    // true：系统是否支持此字符集
    public static boolean isSupported(String charsetName) {
        return (lookup(charsetName) != null);
    }
    
    /**
     * Tells whether or not this charset is registered in the <a href="http://www.iana.org/assignments/character-sets">IANA Charset Registry</a>.
     *
     * @return {@code true} if, and only if, this charset is known by its implementor to be registered with the IANA
     */
    /*
     * 返回true表示当前字符集已在IANA（互联网数字分配机构，管理域名、协议分配和其他数字资源）注册
     *
     * IANA是维护字符集名称的权威登记机构。
     * 如果给出的Charset对象表示在IANA注册的字符集，那么isRegistered()方法将返回true。
     * 如果是这样的话，那么Charset对象需要满足几个条件：
     *  字符集的规范名称应与在IANA注册的名称相符。
     *  如果IANA用同一个字符集注册了多个名称，对象返回的规范名称应该与IANA注册中的MIME-首选名称相符。
     *  如果字符集名称从注册中移除，那么当前的规范名称应保留为别名。
     *  如果字符集没有在IANA注册，它的规范名称必须以“X-”或“x-”开头。
     *
     * 如果是自定义字符集，那么应当让isRegistered()返回false，并以“X-”或“x-”开头命名字符集
     */
    public final boolean isRegistered() {
        return !name.startsWith("X-") && !name.startsWith("x-");
    }
    
    /**
     * Tells whether or not this charset contains the given charset.
     *
     * <p> A charset <i>C</i> is said to <i>contain</i> a charset <i>D</i> if,
     * and only if, every character representable in <i>D</i> is also
     * representable in <i>C</i>.  If this relationship holds then it is
     * guaranteed that every string that can be encoded in <i>D</i> can also be
     * encoded in <i>C</i> without performing any replacements.
     *
     * <p> That <i>C</i> contains <i>D</i> does not imply that each character
     * representable in <i>C</i> by a particular byte sequence is represented
     * in <i>D</i> by the same byte sequence, although sometimes this is the
     * case.
     *
     * <p> Every charset contains itself.
     *
     * <p> This method computes an approximation of the containment relation:
     * If it returns {@code true} then the given charset is known to be
     * contained by this charset; if it returns {@code false}, however, then
     * it is not necessarily the case that the given charset is not contained
     * in this charset.
     *
     * @param cs The given charset
     *
     * @return {@code true} if the given charset is contained in this charset
     */
    // true：给定的字符集与当前的字符集实现类匹配
    public abstract boolean contains(Charset cs);
    
    /**
     * Compares this charset to another.
     *
     * <p> Charsets are ordered by their canonical names, without regard to
     * case. </p>
     *
     * @param that The charset to which this charset is to be compared
     *
     * @return A negative integer, zero, or a positive integer as this charset
     * is less than, equal to, or greater than the specified charset
     */
    // 忽略大小写地比较两个字符集的规范名称
    public final int compareTo(Charset that) {
        return (name().compareToIgnoreCase(that.name()));
    }
    
    
    
    /**
     * Computes a hashcode for this charset.
     *
     * @return An integer hashcode
     */
    public final int hashCode() {
        return name().hashCode();
    }
    
    /**
     * Tells whether or not this object is equal to another.
     *
     * <p> Two charsets are equal if, and only if, they have the same canonical
     * names.  A charset is never equal to any other type of object.  </p>
     *
     * @return {@code true} if, and only if, this charset is equal to the
     * given object
     */
    public final boolean equals(Object ob) {
        if(!(ob instanceof Charset))
            return false;
        if(this == ob)
            return true;
        return name.equals(((Charset) ob).name());
    }
    
    /**
     * Returns a string describing this charset.
     *
     * @return A string describing this charset
     */
    public final String toString() {
        return name();
    }
    
    
    
    // 访问ExtendedCharsets[扩展字符集提供商]的工具（需要通过权限审核）
    private static class ExtendedProviderHolder {
        static final CharsetProvider[] extendedProviders = extendedProviders();
        
        // 返回装载的ExtendedProvider
        private static CharsetProvider[] extendedProviders() {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                public CharsetProvider[] run() {
                    CharsetProvider[] cps = new CharsetProvider[1];
                    int n = 0;
                    ServiceLoader<CharsetProvider> sl = ServiceLoader.loadInstalled(CharsetProvider.class);
                    // 将加载到的字符集提供商保存起来，如果提供商过多，需要扩容
                    for(CharsetProvider cp : sl) {
                        if(n + 1 > cps.length) {
                            cps = Arrays.copyOf(cps, cps.length << 1);
                        }
                        cps[n++] = cp;
                    }
                    return n == cps.length ? cps : Arrays.copyOf(cps, n);
                }
            });
        }
    }
}
