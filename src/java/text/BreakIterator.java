/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.lang.ref.SoftReference;
import java.text.spi.BreakIteratorProvider;
import java.util.Locale;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleServiceProviderPool;

/**
 * The BreakIterator class implements methods for finding the location of boundaries in text.
 * Instances of BreakIterator maintain a current position and scan over text returning the index of characters where boundaries occur.
 * Internally, BreakIterator scans text using a CharacterIterator, and is thus able to scan text held by any object implementing that protocol.
 * A StringCharacterIterator is used to scan String objects passed to setText.
 *
 *
 * You use the factory methods provided by this class to create instances of various types of break iterators.
 * In particular, use getWordInstance, getLineInstance, getSentenceInstance, and getCharacterInstance to create BreakIterators that perform word, line, sentence, and character boundary analysis respectively.
 * A single BreakIterator can work only on one unit (word, line, sentence, and so on).
 * You must use a different iterator for each unit boundary analysis you wish to perform.
 *
 * Line:
 * Line boundary analysis determines where a text string can be broken when line-wrapping.
 * The mechanism correctly handles punctuation and hyphenated words.
 * Actual line breaking needs to also consider the available line width and is handled by higher-level software.
 *
 * Sentence:
 * Sentence boundary analysis allows selection with correct interpretation of periods within numbers and abbreviations, and trailing punctuation marks such as quotation marks and parentheses.
 *
 * Word:
 * Word boundary analysis is used by search and replace functions, as well as within text editing applications that allow the user to select words with a double click.
 * Word selection provides correct interpretation of punctuation marks within and following words.
 * Characters that are not part of a word, such as symbols or punctuation marks, have word-breaks on both sides.
 *
 * Characte:
 * Character boundary analysis allows users to interact with characters as they expect to, for example, when moving the cursor through a text string.
 * Character boundary analysis provides correct navigation through character strings, regardless of how the character is stored.
 * The boundaries returned may be those of supplementary characters, combining character sequences, or ligature clusters.
 * For example, an accented character might be stored as a base character and a diacritical mark. What users consider to be a character can differ between languages.
 *
 *
 * The BreakIterator instances returned by the factory methods of this class are intended for use with natural languages only, not for programming language text.
 * It is however possible to define subclasses that tokenize a programming language.
 *
 *
 * Examples:
 *
 * // Creating and using text boundaries:
 * public static void main(String args[]) {
 *      if (args.length == 1) {
 *          String stringToExamine = args[0];
 *          //print each word in order
 *          BreakIterator boundary = BreakIterator.getWordInstance();
 *          boundary.setText(stringToExamine);
 *          printEachForward(boundary, stringToExamine);
 *          //print each sentence in reverse order
 *          boundary = BreakIterator.getSentenceInstance(Locale.US);
 *          boundary.setText(stringToExamine);
 *          printEachBackward(boundary, stringToExamine);
 *          printFirst(boundary, stringToExamine);
 *          printLast(boundary, stringToExamine);
 *      }
 * }
 *
 *
 * // Print each element in order:
 * public static void printEachForward(BreakIterator boundary, String source) {
 *     int start = boundary.first();
 *     for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {
 *          System.out.println(source.substring(start,end));
 *     }
 * }
 *
 *
 * // Print each element in reverse order:
 * public static void printEachBackward(BreakIterator boundary, String source) {
 *     int end = boundary.last();
 *     for (int start = boundary.previous();
 *          start != BreakIterator.DONE;
 *          end = start, start = boundary.previous()) {
 *         System.out.println(source.substring(start,end));
 *     }
 * }
 *
 *
 * // Print first element:
 * public static void printFirst(BreakIterator boundary, String source) {
 *     int start = boundary.first();
 *     int end = boundary.next();
 *     System.out.println(source.substring(start,end));
 * }
 *
 *
 * // Print last element:
 * public static void printLast(BreakIterator boundary, String source) {
 *     int end = boundary.last();
 *     int start = boundary.previous();
 *     System.out.println(source.substring(start,end));
 * }
 *
 *
 * // Print the element at a specified position:
 * public static void printAt(BreakIterator boundary, int pos, String source) {
 *     int end = boundary.following(pos);
 *     int start = boundary.previous();
 *     System.out.println(source.substring(start,end));
 * }
 *
 *
 * // Find the next word:
 * public static int nextWordStartAfter(int pos, String text) {
 *     BreakIterator wb = BreakIterator.getWordInstance();
 *     wb.setText(text);
 *     int last = wb.following(pos);
 *     int current = wb.next();
 *     while (current != BreakIterator.DONE) {
 *         for (int p = last; p < current; p++) {
 *             if (Character.isLetter(text.codePointAt(p)))
 *                 return last;
 *         }
 *         last = current;
 *         current = wb.next();
 *     }
 *     return BreakIterator.DONE;
 * }
 *
 * 语料：
 * (The iterator returned by BreakIterator.getWordInstance() is unique in that
 * the break positions it returns don't represent both the start and end of the
 * thing being iterated over.  That is, a sentence-break iterator returns breaks
 * that each represent the end of one sentence and the beginning of the next.
 * With the word-break iterator, the characters between two boundaries might be a
 * word, or they might be the punctuation or whitespace between two words.  The
 * above code uses a simple heuristic to determine which boundary is the beginning
 * of a word: If the characters between this boundary and the next boundary
 * include at least one letter (this can be an alphabetical letter, a CJK ideograph,
 * a Hangul syllable, a Kana character, etc.), then the text between this boundary
 * and the next is a word; otherwise, it's the material between words.)
 *
 * @see CharacterIterator
 * @since 1.1
 */
/*
 * 继承关系：
 *
 *   LocaleServiceProvider
 *           ∧                                     BreakIterator
 *           ∣ extends                                 ∧extends
 *   BreakIteratorProvider                ┌─────────────∣───────────────┐
 *           ∧                           │    RuleBasedBreakIterator    │
 *           ∣ extends         创建       │             ∧              │
 * BreakIteratorProviderImpl -----------> │              │extends        │
 *                                        │ DictionaryBasedBreakIterator │
 *                                        └──────────────────────────────┘
 *
 * BreakIterator类是抽象的分词器，给出了在文本中定位边界的接口方法，其行为细节由分词器工厂构造的分词器实现。
 *
 * 在内部，BreakIterator使用CharacterIterator扫描文本，因此能够扫描实现该协议的任何对象所持有的文本。
 * 一般使用StringCharacterIterator用于扫描传递给setText的String对象。
 * 分词器采用的分词策略由底层算法定义。
 *
 * 要注意的是，有效元素范围是：[起点，终点)，是个左闭右开的区间，即起点同时也是第一个元素，但终点是最后一个元素的后面那个位置。
 *
 * 另外，此类的工厂方法返回的BreakIterator实例仅用于自然语言，不适用于编程语言文本。
 *
 * 如无特别说明，代码中“索引”的单位是char，即遇到四字节符号，其一个符号代表两个索引单位
 */
public abstract class BreakIterator implements Cloneable {
    /**
     * DONE is returned by previous(), next(), next(int), preceding(int) and following(int) when either the first or last text boundary has been reached.
     */
    // 用作错误、异常、无效元素标记
    public static final int DONE = -1;
    
    private static final int CHARACTER_INDEX = 0;   // 指示创建字符分词器
    private static final int WORD_INDEX      = 1;   // 指示创建单词分词器
    private static final int LINE_INDEX      = 2;   // 指示创建行分词器
    private static final int SENTENCE_INDEX  = 3;   // 指示创建语句分词器
    
    // 存放包装了分词器缓存的软引用数组，从type上划分为4类，每一类可能对应不同的语言环境
    @SuppressWarnings("unchecked")
    private static final SoftReference<BreakIteratorCache>[] iterCache = (SoftReference<BreakIteratorCache>[]) new SoftReference<?>[4];
    
    /**
     * Constructor. BreakIterator is stateless and has no default behavior.
     */
    protected BreakIterator() {
    }
    
    /**
     * Returns character index of the text boundary that was most
     * recently returned by next(), next(int), previous(), first(), last(),
     * following(int) or preceding(int). If any of these methods returns
     * BreakIterator.DONE because either first or last text boundary
     * has been reached, it returns the first or last text boundary depending on
     * which one is reached.
     *
     * @return The text boundary returned from the above methods, first or last
     * text boundary.
     *
     * @see #next()
     * @see #next(int)
     * @see #previous()
     * @see #first()
     * @see #last()
     * @see #following(int)
     * @see #preceding(int)
     */
    // 返回游标当前的索引，游标的有效范围是[起点,终点]，而有效元素的范围是[起点，终点)
    public abstract int current();
    
    /**
     * Returns the first boundary.
     * The iterator's current position is set to the first text boundary.
     *
     * @return The character index of the first text boundary.
     */
    // 返回整个文本的起点索引，并将游标设置到起点
    public abstract int first();
    
    /**
     * Returns the last boundary.
     * The iterator's current position is set to the last text boundary.
     *
     * @return The character index of the last text boundary.
     */
    // 返回整个文本的终点索引（此处由DONE标记），并将游标设置到终点
    public abstract int last();
    
    /**
     * Returns the boundary preceding the current boundary.
     * If the current boundary is the first text boundary, it returns BreakIterator.DONE and the iterator's current position is unchanged.
     * Otherwise, the iterator's current position is set to the boundary preceding the current boundary.
     *
     * @return The character index of the previous text boundary or
     * BreakIterator.DONE if the current boundary is the first text
     * boundary.
     */
    // 返回offset索引之前出现的最近一个左边界，且游标也要前移。如果已经在起点（即第一个元素），则返回DONE，游标设置为起点。
    public abstract int previous();
    
    /**
     * Returns the boundary following the current boundary.
     * If the current boundary is the last text boundary, it returns BreakIterator.DONE and the iterator's current position is unchanged.
     * Otherwise, the iterator's current position is set to the boundary following the current boundary.
     *
     * @return The character index of the next text boundary or
     * BreakIterator.DONE if the current boundary is the last text
     * boundary.
     * Equivalent to next(1).
     *
     * @see #next(int)
     */
    // 返回当前索引所在的元素的下一个元素（字符/单词/行/句子）的左边界索引，且游标也要后移。如果已经在最后一个元素上或在终点上，则返回DONE，游标设置为终点。
    public abstract int next();
    
    /**
     * Returns the nth boundary from the current boundary.
     * If either the first or last text boundary has been reached, it returns BreakIterator.DONE and the current position is set to either the first or last text boundary depending on which one is reached.
     * Otherwise, the iterator's current position is set to the new boundary.
     * For example, if the iterator's current position is the mth text boundary and three more boundaries exist from the current boundary to the last text boundary,  the next(2) call will return m + 2.
     * The new text position is set to the (m + 2)th text boundary. A next(4) call would return
     * BreakIterator.DONE and the last text boundary would become the new text position.
     *
     * @param n which boundary to return.
     *          A value of 0 does nothing.  Negative values move to previous boundaries and positive values move to later boundaries.
     *
     * @return The character index of the nth boundary from the current position or BreakIterator.DONE if either first or last text boundary has been reached.
     */
    // 返回游标前进/后退n个元素后的左边界索引，并将游标挪到相应的新位置。新位置的有效范围是[first(), last()]。超出范围时返回DONE，游标设到起点或终点。
    // 注意n可以为负值，代表前移。
    public abstract int next(int n);
    
    /**
     * Returns the first boundary following the specified character offset.
     * If the specified offset equals to the last text boundary, it returns BreakIterator.DONE and the iterator's current position is unchanged.
     * Otherwise, the iterator's current position is set to the returned boundary.
     * The value returned is always greater than the offset or the value BreakIterator.DONE.
     *
     * @param offset the character offset to begin scanning.
     *
     * @return The first boundary after the specified offset or BreakIterator.DONE if the last text boundary is passed in as the offset.
     *
     * @throws IllegalArgumentException if the specified offset is less than the first text boundary or greater than the last text boundary.
     */
    // 返回offset索引所在的元素的下一个元素的左边界，游标也会跟着移动。如果offset不在有效范围内，或下一个元素不存在，则返回DONE
    public abstract int following(int offset);
    
    /**
     * Get the text being scanned
     *
     * @return the text being scanned
     */
    // 返回分词器关联的文本迭代器
    public abstract CharacterIterator getText();
    
    /**
     * Set a new text for scanning.  The current scan
     * position is reset to first().
     *
     * @param newText new text to scan.
     */
    // 为分词器设置文本迭代器
    public abstract void setText(CharacterIterator newText);
    
    
    /**
     * Set a new text string to be scanned.  The current scan
     * position is reset to first().
     *
     * @param newText new text to scan.
     */
    // 为分词器设置语料（待分析文本），实际操作时会先将语料包装到一个文本迭代器中以便接下来遍历
    public void setText(String newText) {
        // 设置文本迭代器，此处默认使用StringCharacterIterator
        setText(new StringCharacterIterator(newText));
    }
    
    /**
     * Returns the last boundary preceding the specified character offset. If the
     * specified offset equals to the first text boundary, it returns
     * BreakIterator.DONE and the iterator's current position is unchanged.
     * Otherwise, the iterator's current position is set to the returned boundary.
     * The value returned is always less than the offset or the value
     * BreakIterator.DONE.
     *
     * @param offset the character offset to begin scanning.
     *
     * @return The last boundary before the specified offset or
     * BreakIterator.DONE if the first text boundary is passed in
     * as the offset.
     *
     * @throws IllegalArgumentException if the specified offset is less than
     *                                  the first text boundary or greater than the last text boundary.
     * @since 1.2
     */
    // 返回offset索引左侧出现的最近一个左边界
    public int preceding(int offset) {
        // NOTE:  This implementation is here solely because we can't add new abstract methods to an existing class.
        // There is almost ALWAYS a better, faster way to do this.
        int pos = following(offset);
        while(pos >= offset && pos != DONE) {
            pos = previous();
        }
        return pos;
    }
    
    /**
     * Returns true if the specified character offset is a text boundary.
     *
     * @param offset the character offset to check.
     *
     * @return true if "offset" is a boundary position,
     * false otherwise.
     *
     * @throws IllegalArgumentException if the specified offset is less than
     *                                  the first text boundary or greater than the last text boundary.
     * @since 1.2
     */
    // 判断offset索引处是否是一个（左）边界。
    public boolean isBoundary(int offset) {
        // NOTE: This implementation probably is wrong for most situations
        // because it fails to take into account the possibility that a
        // CharacterIterator passed to setText() may not have a begin offset
        // of 0.  But since the abstract BreakIterator doesn't have that
        // knowledge, it assumes the begin offset is 0.  If you subclass
        // BreakIterator, copy the SimpleTextBoundary implementation of this
        // function into your subclass.  [This should have been abstract at
        // this level, but it's too late to fix that now.]
        // 可能会导致BUG，只适用于非空串...
        if(offset == 0) {
            return true;
        }
        int boundary = following(offset - 1);
        if(boundary == DONE) {
            throw new IllegalArgumentException();
        }
        return boundary == offset;
    }
    
    
    
    /*▼ 分词器工厂 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回分词器
    private static BreakIterator getBreakInstance(Locale locale, int type) {
        // 存在包装了type类型的分词器缓存的软引用
        if(iterCache[type] != null) {
            // 取出该缓存
            BreakIteratorCache cache = iterCache[type].get();
            // 判断缓存是否为空（因为该缓存被软引用包装，所以存在被清理掉的可能性）
            if(cache != null) {
                // 分词器缓存存在，且语言类别也匹配
                if(cache.getLocale().equals(locale)) {
                    // 返回该缓存的克隆对象。由于该对象被软引用包装，所以不直接返回它本身。
                    return cache.createBreakInstance();
                }
            }
        }
        
        // 由以上分析可知，缓存保存的是最近使用的分词器，而分词器受类型和语言环境的限制，所以缓存很可能用不上。
        
        // 未找到匹配的缓存时，新建一个分词器实例
        BreakIterator result = createBreakInstance(locale, type);
        
        // 将分词器打包成分词器缓存对象，再用软引用包装好，存入软引用数组。
        BreakIteratorCache cache = new BreakIteratorCache(locale, result);
        iterCache[type] = new SoftReference<>(cache);
        
        return result;
    }
    
    // 根据本地语言信息locale及指定的分词器类型type来创建分词器
    private static BreakIterator createBreakInstance(Locale locale, int type) {
        LocaleProviderAdapter adapter = LocaleProviderAdapter.getAdapter(BreakIteratorProvider.class, locale);
        BreakIterator iterator = createBreakInstance(adapter, locale, type);
        if(iterator == null) {
            iterator = createBreakInstance(LocaleProviderAdapter.forJRE(), locale, type);
        }
        return iterator;
    }
    
    // 根据本地语言信息locale及指定的分词器类型type来创建分词器，
    private static BreakIterator createBreakInstance(LocaleProviderAdapter adapter, Locale locale, int type) {
        BreakIteratorProvider breakIteratorProvider = adapter.getBreakIteratorProvider();
        BreakIterator iterator = null;
        // 创建各种类型的分词器，会在底层加载不同的分词策略
        switch(type) {
            case CHARACTER_INDEX:
                iterator = breakIteratorProvider.getCharacterInstance(locale);
                break;
            case WORD_INDEX:
                iterator = breakIteratorProvider.getWordInstance(locale);
                break;
            case LINE_INDEX:
                iterator = breakIteratorProvider.getLineInstance(locale);
                break;
            case SENTENCE_INDEX:
                iterator = breakIteratorProvider.getSentenceInstance(locale);
                break;
        }
        return iterator;
    }
    
    /*▲ 分词器工厂 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字符/符号分词器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new BreakIterator instance
     * for <a href="BreakIterator.html#character">character breaks</a>
     * for the given locale.
     *
     * @param locale the desired locale
     *
     * @return A break iterator for character breaks
     *
     * @throws NullPointerException if locale is null
     */
    /*
     * 返回一个用于定位字符边界的BreakIterator，需要指定语言环境。
     * 该字符准确地说应该是“符号”，可能是一字节、两字节、四字节
     */
    public static BreakIterator getCharacterInstance(Locale locale) {
        return getBreakInstance(locale, CHARACTER_INDEX);
    }
    
    /**
     * Returns a new BreakIterator instance
     * for <a href="BreakIterator.html#character">character breaks</a>
     * for the {@linkplain Locale#getDefault() default locale}.
     *
     * @return A break iterator for character breaks
     */
    // 返回一个用于定位字符边界的BreakIterator，使用默认的语言环境。
    public static BreakIterator getCharacterInstance() {
        return getCharacterInstance(Locale.getDefault());
    }
    
    /*▲ 字符/符号分词器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ 单词分词器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new BreakIterator instance for <a href="BreakIterator.html#word">word breaks</a> for the given locale.
     *
     * @param locale the desired locale
     *
     * @return A break iterator for word breaks
     *
     * @throws NullPointerException if locale is null
     */
    /*
     * 返回一个用于定位单词边界的BreakIterator，需要指定语言环境。
     *
     * 这在搜索和替换功能，以及“双击选择某个单词”或“查找所有的单词”这种操作中很有用。
     * 此类型的BreakIterator确保每一个“合法”单词的开始和结束位置有一个边界（分别是首字母和尾字母）。
     * 空白符和标点可用来分割"合法"单词（因为空白符和标点的边界就是自身）。
     * 这里的“合法”单词含义广泛：
     * "123"、"012abc"、"a-b-c"、"obj.method"、"中国"等都算“合法”单词。
     * 标点符号一般不会被独立提取的，它会被掺杂到单词中间。
     */
    public static BreakIterator getWordInstance(Locale locale) {
        return getBreakInstance(locale, WORD_INDEX);
    }
    
    /**
     * Returns a new BreakIterator instance for <a href="BreakIterator.html#word">word breaks</a> for the {@linkplain Locale#getDefault() default locale}.
     *
     * @return A break iterator for word breaks
     */
    // 返回一个用于定位单词边界的BreakIterator，使用默认的语言环境。
    public static BreakIterator getWordInstance() {
        return getWordInstance(Locale.getDefault());
    }
    
    /*▲ 单词分词器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 行分词器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new BreakIterator instance for <a href="BreakIterator.html#line">line breaks</a> for the given locale.
     *
     * @param locale the desired locale
     *
     * @return A break iterator for line breaks
     *
     * @throws NullPointerException if locale is null
     */
    /*
     * 返回一个用于定位行边界的BreakIterator，需要指定语言环境。
     *
     * 确定哪些边界后面可以跟换行符（单词间不换行，标点一般会被分配到上一行）
     */
    public static BreakIterator getLineInstance(Locale locale) {
        return getBreakInstance(locale, LINE_INDEX);
    }
    
    /**
     * Returns a new BreakIterator instance
     * for <a href="BreakIterator.html#line">line breaks</a>
     * for the {@linkplain Locale#getDefault() default locale}.
     *
     * @return A break iterator for line breaks
     */
    // 返回一个用于定位行边界的BreakIterator，使用默认的语言环境。
    public static BreakIterator getLineInstance() {
        return getLineInstance(Locale.getDefault());
    }
    
    /*▲ 行分词器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 句子分词器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new BreakIterator instance
     * for <a href="BreakIterator.html#sentence">sentence breaks</a>
     * for the given locale.
     *
     * @param locale the desired locale
     *
     * @return A break iterator for sentence breaks
     *
     * @throws NullPointerException if locale is null
     */
    // 返回一个用于定位句子边界的BreakIterator，需要指定语言环境。
    public static BreakIterator getSentenceInstance(Locale locale) {
        return getBreakInstance(locale, SENTENCE_INDEX);
    }
    
    /**
     * Returns a new BreakIterator instance
     * for <a href="BreakIterator.html#sentence">sentence breaks</a>
     * for the {@linkplain Locale#getDefault() default locale}.
     *
     * @return A break iterator for sentence breaks
     */
    // 返回一个用于定位句子边界的BreakIterator，使用默认的语言环境。
    public static BreakIterator getSentenceInstance() {
        return getSentenceInstance(Locale.getDefault());
    }
    
    /*▲ 句子分词器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns an array of all locales for which the get*Instance methods of this class can return localized instances.
     * The returned array represents the union of locales supported by the Java runtime and by installed {@link BreakIteratorProvider BreakIteratorProvider} implementations.
     * It must contain at least a Locale instance equal to {@link Locale#US Locale.US}.
     *
     * @return An array of locales for which localized BreakIterator instances are available.
     */
    // 返回所有为BreakIterator实例提供可用的本地化语言环境的配置信息
    public static synchronized Locale[] getAvailableLocales() {
        LocaleServiceProviderPool pool = LocaleServiceProviderPool.getPool(BreakIteratorProvider.class);
        return pool.getAvailableLocales();
    }
    
    /**
     * Create a copy of this iterator
     *
     * @return A copy of this
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch(CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
    
    // 指定locale下的分词器缓存对象
    private static final class BreakIteratorCache {
        private BreakIterator iter;
        private Locale locale;
        
        BreakIteratorCache(Locale locale, BreakIterator iter) {
            this.locale = locale;
            this.iter = (BreakIterator) iter.clone();
        }
        
        Locale getLocale() {
            return locale;
        }
        
        BreakIterator createBreakInstance() {
            return (BreakIterator) iter.clone();
        }
    }
}
