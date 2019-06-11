/*
 * Copyright (c) 1994, 2004, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * The string tokenizer class allows an application to break a
 * string into tokens. The tokenization method is much simpler than
 * the one used by the {@code StreamTokenizer} class. The
 * {@code StringTokenizer} methods do not distinguish among
 * identifiers, numbers, and quoted strings, nor do they recognize
 * and skip comments.
 * <p>
 * The set of delimiters (the characters that separate tokens) may
 * be specified either at creation time or on a per-token basis.
 * <p>
 * An instance of {@code StringTokenizer} behaves in one of two
 * ways, depending on whether it was created with the
 * {@code returnDelims} flag having the value {@code true}
 * or {@code false}:
 * <ul>
 * <li>If the flag is {@code false}, delimiter characters serve to
 * separate tokens. A token is a maximal sequence of consecutive
 * characters that are not delimiters.
 * <li>If the flag is {@code true}, delimiter characters are themselves
 * considered to be tokens. A token is thus either one delimiter
 * character, or a maximal sequence of consecutive characters that are
 * not delimiters.
 * </ul><p>
 * A {@code StringTokenizer} object internally maintains a current
 * position within the string to be tokenized. Some operations advance this
 * current position past the characters processed.<p>
 * A token is returned by taking a substring of the string that was used to
 * create the {@code StringTokenizer} object.
 * <p>
 * The following is one example of the use of the tokenizer. The code:
 * <blockquote><pre>
 *     StringTokenizer st = new StringTokenizer("this is a test");
 *     while (st.hasMoreTokens()) {
 *         System.out.println(st.nextToken());
 *     }
 * </pre></blockquote>
 * <p>
 * prints the following output:
 * <blockquote><pre>
 *     this
 *     is
 *     a
 *     test
 * </pre></blockquote>
 *
 * <p>
 * {@code StringTokenizer} is a legacy class that is retained for
 * compatibility reasons although its use is discouraged in new code. It is
 * recommended that anyone seeking this functionality use the {@code split}
 * method of {@code String} or the java.util.regex package instead.
 * <p>
 * The following example illustrates how the {@code String.split}
 * method can be used to break up a string into its basic tokens:
 * <blockquote><pre>
 *     String[] result = "this is a test".split("\\s");
 *     for (int x=0; x&lt;result.length; x++)
 *         System.out.println(result[x]);
 * </pre></blockquote>
 * <p>
 * prints the following output:
 * <blockquote><pre>
 *     this
 *     is
 *     a
 *     test
 * </pre></blockquote>
 *
 * @author unascribed
 * @see java.io.StreamTokenizer
 * @since 1.0
 */
// 字符串分割器，将字符串根据指定的分割符号切割成一段一段的标记，分隔符可以是四字节字符
public class StringTokenizer implements Enumeration<Object> {
    private String str;         // 待解析字符串
    private String delimiters;  // 分割符
    private boolean retDelims;  // 分割字符串时是否返回分割符本身
    
    private int currentPosition;    // 游标，语义在下一个待解析符号的索引和下下个待解析符号的索引之间切换
    private int newPosition;        // 临时存储下一个待解析符号的索引
    private int maxPosition;        // 待解析字符串长度（包含的char的个数）
    
    private boolean delimsChanged;  // 分隔符是否发生改变
    
    /**
     * maxDelimCodePoint stores the value of the delimiter character with the
     * highest value. It is used to optimize the detection of delimiter
     * characters.
     *
     * It is unlikely to provide any optimization benefit in the
     * hasSurrogates case because most string characters will be
     * smaller than the limit, but we keep it so that the two code
     * paths remain similar.
     */
    // 分隔符中最大的Unicode编码值
    private int maxDelimCodePoint;
    
    /**
     * If delimiters include any surrogates (including surrogate
     * pairs), hasSurrogates is true and the tokenizer uses the
     * different code path. This is because String.indexOf(int)
     * doesn't handle unpaired surrogates as a single character.
     */
    // 是否存在位于Unicode代理区的符号
    private boolean hasSurrogates = false;
    
    /**
     * When hasSurrogates is true, delimiters are converted to code
     * points and isDelimiter(int) is used to determine if the given
     * codepoint is a delimiter.
     */
    // 存储分隔符的Unicode编码
    private int[] delimiterCodePoints;
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs a string tokenizer for the specified string. All
     * characters in the {@code delim} argument are the delimiters
     * for separating tokens.
     * <p>
     * If the {@code returnDelims} flag is {@code true}, then the delimiter characters are also returned as tokens.
     * Each delimiter is returned as a string of length one.
     * If the flag is {@code false}, the delimiter characters are skipped and only serve as separators between tokens.
     * <p>
     * Note that if {@code delim} is {@code null}, this constructor does
     * not throw an exception. However, trying to invoke other methods on the
     * resulting {@code StringTokenizer} may result in a
     * {@code NullPointerException}.
     *
     * @param str          a string to be parsed.
     * @param delimiters   the delimiters.
     * @param returnDelims flag indicating whether to return the delimiters as tokens.
     *
     * @throws NullPointerException if str is {@code null}
     */
    // 初始化待解析字符串与分割符，参数returnDelims表示在分割字符串时是否输出分割符本身
    public StringTokenizer(String str, String delimiters, boolean returnDelims) {
        this.str = str;
        this.delimiters = delimiters;
        retDelims = returnDelims;
        currentPosition = 0;
        newPosition = -1;
        delimsChanged = false;
        maxPosition = str.length();
        // 预处理分割符
        setMaxDelimCodePoint();
    }
    
    /**
     * Constructs a string tokenizer for the specified string. The
     * characters in the {@code delim} argument are the delimiters
     * for separating tokens. Delimiter characters themselves will not
     * be treated as tokens.
     * <p>
     * Note that if {@code delim} is {@code null}, this constructor does
     * not throw an exception. However, trying to invoke other methods on the
     * resulting {@code StringTokenizer} may result in a
     * {@code NullPointerException}.
     *
     * @param str   a string to be parsed.
     * @param delim the delimiters.
     *
     * @throws NullPointerException if str is {@code null}
     */
    // 初始化待解析字符串与分割符，分割符本身不作为输出
    public StringTokenizer(String str, String delim) {
        this(str, delim, false);
    }
    
    /**
     * Constructs a string tokenizer for the specified string. The
     * tokenizer uses the default delimiter set, which is
     * <code>"&nbsp;&#92;t&#92;n&#92;r&#92;f"</code>: the space character,
     * the tab character, the newline character, the carriage-return character,
     * and the form-feed character. Delimiter characters themselves will
     * not be treated as tokens.
     *
     * @param str a string to be parsed.
     *
     * @throws NullPointerException if str is {@code null}
     */
    // 初始化待解析字符串，默认使用" \t\n\r\f"作为分割符
    public StringTokenizer(String str) {
        this(str, " \t\n\r\f", false);
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 解析标记 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 常用方式：
     *
     * StringTokenizer st = new StringTokenizer(...);
     *
     * while (st.hasMoreElements()) {
     *     System.out.println(st.nextToken());
     * }
     */
    
    /**
     * Tests if there are more tokens available from this tokenizer's string.
     * If this method returns {@code true}, then a subsequent call to
     * {@code nextToken} with no argument will successfully return a token.
     *
     * @return {@code true} if and only if there is at least one token
     * in the string after the current position; {@code false}
     * otherwise.
     */
    // 待解析字符串中是否仍存在未解析的符号
    public boolean hasMoreTokens() {
        /*
         * Temporarily store this position and use it in the following
         * nextToken() method only if the delimiters haven't been changed in
         * that nextToken() invocation.
         */
        // 返回下一个待解析符号的索引
        newPosition = skipDelimiters(currentPosition);
        
        return (newPosition<maxPosition);
    }
    
    /**
     * Returns the next token from this string tokenizer.
     *
     * @return the next token from this string tokenizer.
     *
     * @throws NoSuchElementException if there are no more tokens in this
     *                                tokenizer's string.
     */
    // 返回本次解析出的字符序列，并更新游标到搜索下一个待解析符号的起点
    public String nextToken() {
        /*
         * If next position already computed in hasMoreElements() and
         * delimiters have changed between the computation and this invocation,
         * then use the computed value.
         */
        
        /* 更新游标为下一个待解析符号的索引 */
    
        // 如果newPosition有效，且分割符没有发生变化
        if(newPosition >= 0 && !delimsChanged) {
            // 此处的newPosition需由hasMoreTokens()计算
            currentPosition = newPosition;
        } else {
            // 返回下一个待解析符号的索引
            currentPosition = skipDelimiters(currentPosition);
        }
        
        /* Reset these anyway */
        delimsChanged = false;  // 此处假定分隔符不再变化
        newPosition = -1;       // newPosition重置为无效位置
        
        // 如果已经没有待解析的符号了，则抛出异常
        if(currentPosition >= maxPosition) {
            throw new NoSuchElementException();
        }
        
        // 记录下一个待解析符号的索引
        int start = currentPosition;
    
        // 返回下下个待解析符号的索引
        currentPosition = scanToken(currentPosition);
        
        // 截取出当前解析到的字符串
        return str.substring(start, currentPosition);
    }
    
    /**
     * Returns the next token in this string tokenizer's string. First,
     * the set of characters considered to be delimiters by this
     * {@code StringTokenizer} object is changed to be the characters in
     * the string {@code delim}. Then the next token in the string
     * after the current position is returned. The current position is
     * advanced beyond the recognized token.  The new delimiter set
     * remains the default after this call.
     *
     * @param delim the new delimiters.
     *
     * @return the next token, after switching to the new delimiter set.
     *
     * @throws NoSuchElementException if there are no more tokens in this
     *                                tokenizer's string.
     * @throws NullPointerException   if delim is {@code null}
     */
    // 更新分隔符delim，并返回在新的分隔符下解析出的字符序列
    public String nextToken(String delim) {
        delimiters = delim;
        
        /* delimiter string specified, so set the appropriate flag. */
        delimsChanged = true;
        
        // 预处理分割符
        setMaxDelimCodePoint();
    
        // 返回本次解析出的字符序列，并更新游标到搜索下一个待解析符号的起点
        return nextToken();
    }
    
    /**
     * Returns the same value as the {@code hasMoreTokens}
     * method. It exists so that this class can implement the
     * {@code Enumeration} interface.
     *
     * @return {@code true} if there are more tokens;
     * {@code false} otherwise.
     *
     * @see java.util.Enumeration
     * @see java.util.StringTokenizer#hasMoreTokens()
     */
    // 是否存在未解析的元素（实现Enumeration接口）
    public boolean hasMoreElements() {
        return hasMoreTokens();
    }
    
    /**
     * Returns the same value as the {@code nextToken} method,
     * except that its declared return value is {@code Object} rather than
     * {@code String}. It exists so that this class can implement the
     * {@code Enumeration} interface.
     *
     * @return the next token in the string.
     *
     * @throws NoSuchElementException if there are no more tokens in this
     *                                tokenizer's string.
     * @see java.util.Enumeration
     * @see java.util.StringTokenizer#nextToken()
     */
    // 返回本次解析到的元素（实现Enumeration接口）
    public Object nextElement() {
        return nextToken();
    }
    
    /*▲ 解析标记 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Calculates the number of times that this tokenizer's
     * {@code nextToken} method can be called before it generates an
     * exception. The current position is not advanced.
     *
     * @return the number of tokens remaining in the string using the current
     * delimiter set.
     *
     * @see java.util.StringTokenizer#nextToken()
     */
    // 返回预计可解析出的元素数量（从下一个待解析符号的索引处开始统计）
    public int countTokens() {
        int count = 0;
        int currpos = currentPosition;
        while(currpos<maxPosition) {
            // 查找下一个待解析符号的索引
            currpos = skipDelimiters(currpos);
            if(currpos >= maxPosition) {
                break;
            }
            // 返回下下个待解析符号的索引
            currpos = scanToken(currpos);
            count++;
        }
        
        return count;
    }
    
    /**
     * Set maxDelimCodePoint to the highest char in the delimiter set.
     */
    // 预处理分割符（考虑分割符中存在四字节符号的情形）
    private void setMaxDelimCodePoint() {
        // 如果未指定分隔符
        if(delimiters == null) {
            maxDelimCodePoint = 0;
            return;
        }
        
        int m = 0;      // 记录指定的分隔符中的字符的最大Unicode编码值
        int c;          // Unicode编码
        int count = 0;  // 统计Unicode符号数量
        
        for(int i = 0; i<delimiters.length(); i += Character.charCount(c)) {
            c = delimiters.charAt(i);
            
            // 如果字符c位于代理区
            if(c >= Character.MIN_HIGH_SURROGATE && c<=Character.MAX_LOW_SURROGATE) {
                // 获取c的Unicode编码
                c = delimiters.codePointAt(i);
                hasSurrogates = true;
            }
            
            if(m<c) {
                m = c;
            }
            
            count++;
        }
        
        // 分隔符中最大的Unicode编码值
        maxDelimCodePoint = m;
        
        // 将每个分隔符的Unicode编码存储到delimiterCodePoints数组
        if(hasSurrogates) {
            delimiterCodePoints = new int[count];
            for(int i = 0, j = 0; i<count; i++, j += Character.charCount(c)) {
                c = delimiters.codePointAt(j);
                delimiterCodePoints[i] = c;
            }
        }
    }
    
    /**
     * Skips delimiters starting from the specified position.
     * If retDelims is false, returns the index of the first non-delimiter character at or after startPos.
     * If retDelims is true, startPos is returned.
     */
    // 返回下一个待解析符号的索引
    private int skipDelimiters(int startPos) {
        if(delimiters == null) {
            throw new NullPointerException();
        }
        
        int position = startPos;
        
        // 当不需要输出分隔符时，查找待解析字符串中首个不属于分隔符的字符索引（从startPos索引开始查找）
        while(!retDelims && position<maxPosition) {
            // 不存在代理区符号的情形
            if(!hasSurrogates) {
                // 获取待解析字符串position处的字符
                char c = str.charAt(position);
                
                // 如果字符c不属于分隔符，则可以结束查找了
                if((c>maxDelimCodePoint) || (delimiters.indexOf(c)<0)) {
                    break;
                }
                
                position++;
            } else {
                int c = str.codePointAt(position);
                if((c>maxDelimCodePoint) || !isDelimiter(c)) {
                    break;
                }
                position += Character.charCount(c);
            }
        }
        
        return position;
    }
    
    /**
     * Skips ahead from startPos and returns the index of the next delimiter character encountered, or maxPosition if no such delimiter is found.
     */
    // 返回下下个待解析符号的索引
    private int scanToken(int startPos) {
        int position = startPos;
        
        // 在待解析字符串中查找下一个分隔符的索引，保存到position
        while(position<maxPosition) {
            if(!hasSurrogates) {
                char c = str.charAt(position);
                // 如果字符c属于分隔符，则可以结束查找了
                if((c<=maxDelimCodePoint) && (delimiters.indexOf(c) >= 0)) {
                    break;
                }
                position++;
            } else {
                int c = str.codePointAt(position);
                if((c<=maxDelimCodePoint) && isDelimiter(c)) {
                    break;
                }
                position += Character.charCount(c);
            }
        }
        
        // 如果需要输出分隔符
        if(retDelims){
            // 如果startPos位置就是一个分隔符
            if(startPos == position) {
                if(!hasSurrogates) {
                    char c = str.charAt(position);
                    // 如果字符c属于分隔符，游标前进一个位置
                    if((c<=maxDelimCodePoint) && (delimiters.indexOf(c) >= 0)) {
                        position++;
                    }
                } else {
                    int c = str.codePointAt(position);
                    if((c<=maxDelimCodePoint) && isDelimiter(c)) {
                        position += Character.charCount(c);
                    }
                }
            }
        }
        
        return position;
    }
    
    // 判断codePoint代表的符号是否属于分割符
    private boolean isDelimiter(int codePoint) {
        for(int delimiterCodePoint : delimiterCodePoints) {
            if(delimiterCodePoint == codePoint) {
                return true;
            }
        }
        return false;
    }
}
