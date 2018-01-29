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

/**
 * This interface defines a protocol for bidirectional iteration over text.
 * The iterator iterates over a bounded sequence of characters.  Characters
 * are indexed with values beginning with the value returned by getBeginIndex() and
 * continuing through the value returned by getEndIndex()-1.
 * <p>
 * Iterators maintain a current character index, whose valid range is from
 * getBeginIndex() to getEndIndex(); the value getEndIndex() is included to allow
 * handling of zero-length text ranges and for historical reasons.
 * The current index can be retrieved by calling getIndex() and set directly
 * by calling setIndex(), first(), and last().
 * <p>
 * The methods previous() and next() are used for iteration. They return DONE if
 * they would move outside the range from getBeginIndex() to getEndIndex() -1,
 * signaling that the iterator has reached the end of the sequence. DONE is
 * also returned by other methods to indicate that the current index is
 * outside this range.
 *
 * <P>Examples:<P>
 *
 * Traverse the text from start to finish
 * <pre>{@code
 * public void traverseForward(CharacterIterator iter) {
 *     for(char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
 *         processChar(c);
 *     }
 * }
 * }</pre>
 *
 * Traverse the text backwards, from end to start
 * <pre>{@code
 * public void traverseBackward(CharacterIterator iter) {
 *     for(char c = iter.last(); c != CharacterIterator.DONE; c = iter.previous()) {
 *         processChar(c);
 *     }
 * }
 * }</pre>
 *
 * Traverse both forward and backward from a given position in the text.
 * Calls to notBoundary() in this example represents some
 * additional stopping criteria.
 * <pre>{@code
 * public void traverseOut(CharacterIterator iter, int pos) {
 *     for (char c = iter.setIndex(pos);
 *              c != CharacterIterator.DONE && notBoundary(c);
 *              c = iter.next()) {
 *     }
 *     int end = iter.getIndex();
 *     for (char c = iter.setIndex(pos);
 *             c != CharacterIterator.DONE && notBoundary(c);
 *             c = iter.previous()) {
 *     }
 *     int start = iter.getIndex();
 *     processSection(start, end);
 * }
 * }</pre>
 *
 * @since 1.1
 * @see StringCharacterIterator
 * @see AttributedCharacterIterator
 */

/*
 * 该接口定义了用于双向迭代文本时的协议。
 *
 * 该协议中，使用迭代器用来迭代有界字符序列。
 * 有效字符范围是：[getBeginIndex(), getEndIndex()-1]。
 *
 * 迭代器需要维护一个游标来指向当前字符索引，其有效范围是从[getBeginIndex(), getEndIndex()]。
 *
 * 可以通过调用getIndex()来检索当前游标索引，并通过调用setIndex()，first()和last()直接设置游标索引。
 * 方法previous()和next()用于迭代。如果游标超出有效字符范围，则返回DONE，表示迭代器已到达有效序列的边界。
 * 其他方法中也可能会返回DONE，以指示当前索引超出有效字符的范围。
 */
public interface CharacterIterator extends Cloneable {
    
    /**
     * Constant that is returned when the iterator has reached either the end or the beginning of the text.
     * The value is '\\uFFFF', the "not a character" value which should not occur in any valid Unicode string.
     */
    // 迭代器访问无效的位置时，应当返回此标记
    char DONE = '\uFFFF';
    
    /**
     * Sets the position to the specified position in the text and returns that
     * character.
     *
     * @param position the position within the text.  Valid values range from
     *                 getBeginIndex() to getEndIndex().  An IllegalArgumentException is thrown
     *                 if an invalid value is supplied.
     *
     * @return the character at the specified position or DONE if the specified position is equal to getEndIndex()
     */
    // 重置游标为position，并返回该处的char。
    char setIndex(int position);
    
    /**
     * Sets the position to getBeginIndex() and returns the character at that position.
     *
     * @return the first character in the text, or DONE if the text is empty
     *
     * @see #getBeginIndex()
     */
    // 将设置游标到getBeginIndex()（第一个有效字符）的位置，并返回该char。
    char first();
    
    /**
     * Sets the position to getEndIndex()-1 (getEndIndex() if the text is empty) and returns the character at that position.
     *
     * @return the last character in the text, or DONE if the text is empty
     *
     * @see #getEndIndex()
     */
    // 将设置游标到getEndIndex()-1（最后一个有效字符）的位置。如果文本为空，则设置游标到getEndIndex()位置。最后返回该char。
    char last();
    
    /**
     * Gets the character at the current position (as returned by getIndex()).
     *
     * @return the character at the current position or DONE if the current position is off the end of the text.
     *
     * @see #getIndex()
     */
    // 返回游标当前指向的char。如果游标指向了终点end，则返回无效字符DONE。
    char current();
    
    /**
     * Decrements the iterator's index by one and returns the character at the new index.
     * If the current index is getBeginIndex(), the index remains at getBeginIndex() and a value of DONE is returned.
     *
     * @return the character at the new position or DONE if the current position is equal to getBeginIndex().
     */
    // 如果游标未到起点，则先前移游标，再返回指向的char。否则游标位置不变，且返回DONE。
    char previous();
    
    /**
     * Increments the iterator's index by one and returns the character at the new index.
     * If the resulting index is greater or equal to getEndIndex(), the current index is reset to getEndIndex() and a value of DONE is returned.
     *
     * @return the character at the new position or DONE if the new position is off the end of the text range.
     */
    // 如果游标未到终点，则先后移游标，再返回指向的char。否则游标设置在终点，且返回DONE。
    char next();
    
    /**
     * Returns the start index of the text.
     *
     * @return the index at which the text begins.
     */
    // 返回游标起点
    int getBeginIndex();
    
    /**
     * Returns the end index of the text.
     * This index is the index of the first character following the end of the text.
     *
     * @return the index after the last character in the text
     */
    // 返回游标终点
    int getEndIndex();
    
    /**
     * Returns the current index.
     *
     * @return the current index.
     */
    // 返回游标当前位置
    int getIndex();
    
    /**
     * Create a copy of this iterator
     *
     * @return A copy of this
     */
    // 创建当前迭代器的一份复制品
    Object clone();
}
