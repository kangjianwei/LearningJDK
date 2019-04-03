/*
 * Copyright (c) 1995, 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.util.Arrays;

/**
 * The {@code StreamTokenizer} class takes an input stream and
 * parses it into "tokens", allowing the tokens to be
 * read one at a time. The parsing process is controlled by a table
 * and a number of flags that can be set to various states. The
 * stream tokenizer can recognize identifiers, numbers, quoted
 * strings, and various comment styles.
 * <p>
 * Each byte read from the input stream is regarded as a character
 * in the range {@code '\u005Cu0000'} through {@code '\u005Cu00FF'}.
 * The character value is used to look up five possible attributes of
 * the character: <i>white space</i>, <i>alphabetic</i>,
 * <i>numeric</i>, <i>string quote</i>, and <i>comment character</i>.
 * Each character can have zero or more of these attributes.
 * <p>
 * In addition, an instance has four flags. These flags indicate:
 * <ul>
 * <li>Whether line terminators are to be returned as tokens or treated
 * as white space that merely separates tokens.
 * <li>Whether C-style comments are to be recognized and skipped.
 * <li>Whether C++-style comments are to be recognized and skipped.
 * <li>Whether the characters of identifiers are converted to lowercase.
 * </ul>
 * <p>
 * A typical application first constructs an instance of this class,
 * sets up the syntax tables, and then repeatedly loops calling the
 * {@code nextToken} method in each iteration of the loop until
 * it returns the value {@code TT_EOF}.
 *
 * @author James Gosling
 * @see java.io.StreamTokenizer#nextToken()
 * @see java.io.StreamTokenizer#TT_EOF
 * @since 1.0
 */
// 从输入流中的字符串里提取匹配的标记，用作分割标记的分隔符为ISO-8859-1符号
public class StreamTokenizer {
    /*
     * 类型图
     *
     * 哪些类型的标记需要保存，就在该标记所处的区域设置标记分类符号
     * 类型图并不限制哪些符号必须放在哪些区域，全靠人为指定
     * 不指定标记分类符号的区域，默认为普通符号区域
     */
    private byte[] ctype = new byte[256];
    /**
     * 标记分类符号，下面的排列顺序亦是它们的解析顺序
     *
     * 除数字区外，其他区的符号均允许自定义
     * 排在下面的区域可以覆盖排在上面的区域的设置结果
     * 数字区是固定的'0'~'9'、'.'、'-'，不支持自定义，但支持被空白区覆盖
     */
    private static final byte CT_WHITESPACE =  1; // 空白区，默认是0~' '
    private static final byte CT_DIGIT      =  2; // 数字区，限制为'0'~'9'，'.'，'-'
    private static final byte CT_ALPHA      =  4; // 字母区，默认是'a'~'z'，'A'~'Z'，128+32~255
    private static final byte CT_QUOTE      =  8; // 引号区，默认是字符"和'
    private static final byte CT_COMMENT    = 16; // 注释区，默认是'/'
    
    
    /**
     * The next character to be considered by the nextToken method.
     * May also be NEED_CHAR to indicate that a new character should be read,
     * or SKIP_LF to indicate that a new character should be read and,
     * if it is a '\n' character, it should be discarded and a second new character should be read.
     */
    // 上次结束时捕获的符号（可能无意义）
    private int peekc = NEED_CHAR;
    private static final int NEED_CHAR = Integer.MAX_VALUE;     // 为peekc初始化，表示需要读取新值
    private static final int SKIP_LF = Integer.MAX_VALUE - 1;   // 上次以\r结束
    
    
    /**
     * After a call to the {@code nextToken} method, this field
     * contains the type of the token just read. For a single character
     * token, its value is the single character, converted to an integer.
     * For a quoted string token, its value is the quote character.
     * Otherwise, its value is one of the following:
     * <ul>
     * <li>{@code TT_WORD} indicates that the token is a word.
     * <li>{@code TT_NUMBER} indicates that the token is a number.
     * <li>{@code TT_EOL} indicates that the end of line has been read.
     * The field can only have this value if the
     * {@code eolIsSignificant} method has been called with the
     * argument {@code true}.
     * <li>{@code TT_EOF} indicates that the end of the input stream
     * has been reached.
     * </ul>
     * <p>
     * The initial value of this field is -4.
     *
     * @see java.io.StreamTokenizer#eolIsSignificant(boolean)
     * @see java.io.StreamTokenizer#nextToken()
     * @see java.io.StreamTokenizer#quoteChar(int)
     * @see java.io.StreamTokenizer#TT_EOF
     * @see java.io.StreamTokenizer#TT_EOL
     * @see java.io.StreamTokenizer#TT_NUMBER
     * @see java.io.StreamTokenizer#TT_WORD
     */
    public int ttype = TT_NOTHING;  // 存储返回值的类型，具体类型参见nextToken()方法
    /**
     * A constant indicating that the end of the stream has been read.
     */
    public static final int TT_EOF = -1;    // 捕获了文件结束符
    /**
     * A constant indicating that the end of the line has been read.
     */
    public static final int TT_EOL = '\n';  // 捕获了行尾结束符
    /**
     * A constant indicating that a number token has been read.
     */
    public static final int TT_NUMBER = -2; // 捕获了数字
    /**
     * A constant indicating that a word token has been read.
     */
    public static final int TT_WORD = -3;   // 捕获了字符串
    /**
     * A constant indicating that no token has been read, used for initializing ttype.
     * made available as the part of the API in a future release.
     *
     * FIXME This could be made public and
     */
    private static final int TT_NOTHING = -4;   // 没有有效信息
    
    
    /** The line number of the last token read */
    private int LINENO = 1; // 当前读到的标记处于第几行
    
    private boolean pushedBack; // 是否需要显示上次捕获的有效值
    
    private boolean forceLower; // 是否将捕获到的字符串转为小写形式
    
    private boolean eolIsSignificantP = false;      // 遇到行尾标记\r或\n时是否结束读取
    private boolean slashStarCommentsP = false;     // 是否处理/*风格的注释
    private boolean slashSlashCommentsP = false;    // 是否处理//风格的注释
    
    
    /* Only one of these will be non-null */
    private Reader reader = null;       // 输入
    private InputStream input = null;   // 输入
    
    private char[] buf = new char[20];  // 缓冲区，暂存识别到的非数字序列
    
    /**
     * If the current token is a word token, this field contains a
     * string giving the characters of the word token. When the current
     * token is a quoted string token, this field contains the body of
     * the string.
     * <p>
     * The current token is a word when the value of the
     * {@code ttype} field is {@code TT_WORD}. The current token is
     * a quoted string token when the value of the {@code ttype} field is
     * a quote character.
     * <p>
     * The initial value of this field is null.
     *
     * @see java.io.StreamTokenizer#quoteChar(int)
     * @see java.io.StreamTokenizer#TT_WORD
     * @see java.io.StreamTokenizer#ttype
     */
    public String sval; // 保存识别到的非数字序列
    
    /**
     * If the current token is a number, this field contains the value
     * of that number. The current token is a number when the value of
     * the {@code ttype} field is {@code TT_NUMBER}.
     * <p>
     * The initial value of this field is 0.0.
     *
     * @see java.io.StreamTokenizer#TT_NUMBER
     * @see java.io.StreamTokenizer#ttype
     */
    public double nval; // 保存识别到的数字序列
    
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a stream tokenizer that parses the specified input
     * stream. The stream tokenizer is initialized to the following
     * default state:
     * <ul>
     * <li>All byte values {@code 'A'} through {@code 'Z'},
     * {@code 'a'} through {@code 'z'}, and
     * {@code '\u005Cu00A0'} through {@code '\u005Cu00FF'} are
     * considered to be alphabetic.
     * <li>All byte values {@code '\u005Cu0000'} through
     * {@code '\u005Cu0020'} are considered to be white space.
     * <li>{@code '/'} is a comment character.
     * <li>Single quote {@code '\u005C''} and double quote {@code '"'}
     * are string quote characters.
     * <li>Numbers are parsed.
     * <li>Ends of lines are treated as white space, not as separate tokens.
     * <li>C-style and C++-style comments are not recognized.
     * </ul>
     *
     * @param is an input stream.
     *
     * @see java.io.BufferedReader
     * @see java.io.InputStreamReader
     * @see java.io.StreamTokenizer#StreamTokenizer(java.io.Reader)
     * @deprecated As of JDK version 1.1, the preferred way to tokenize an
     * input stream is to convert it into a character stream, for example:
     * <blockquote><pre>
     *   Reader r = new BufferedReader(new InputStreamReader(is));
     *   StreamTokenizer st = new StreamTokenizer(r);
     * </pre></blockquote>
     */
    @Deprecated
    public StreamTokenizer(InputStream is) {
        this();
        if(is == null) {
            throw new NullPointerException();
        }
        input = is;
    }
    
    /**
     * Create a tokenizer that parses the given character stream.
     *
     * @param r a Reader object providing the input stream.
     *
     * @since 1.1
     */
    public StreamTokenizer(Reader r) {
        this();
        if(r == null) {
            throw new NullPointerException();
        }
        reader = r;
    }
    
    /** Private constructor that initializes everything except the streams. */
    private StreamTokenizer() {
        wordChars('a', 'z');
        wordChars('A', 'Z');
        wordChars(128 + 32, 255);
        whitespaceChars(0, ' ');
        commentChar('/');
        quoteChar('"');
        quoteChar('\'');
        parseNumbers();
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取标记 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Parses the next token from the input stream of this tokenizer.
     * The type of the next token is returned in the {@code ttype}
     * field. Additional information about the token may be in the
     * {@code nval} field or the {@code sval} field of this
     * tokenizer.
     * <p>
     * Typical clients of this
     * class first set up the syntax tables and then sit in a loop
     * calling nextToken to parse successive tokens until TT_EOF
     * is returned.
     *
     * @return the value of the {@code ttype} field.
     *
     * @throws IOException if an I/O error occurs.
     * @see java.io.StreamTokenizer#nval
     * @see java.io.StreamTokenizer#sval
     * @see java.io.StreamTokenizer#ttype
     */
    /*
     * 根据预设的类型图，从输入流中解析出下一个匹配的标记
     *
     * 处理顺序：空白区-->数字区-->字母区-->引号区-->注释区
     *
     * 返回值(ttype)含义：
     * TT_EOF    --> 遇到了文件结尾，peekc无意义
     * TT_EOL    --> 遇到了行结尾，如果peekc为SKIP_LF，代表上次捕获到了\r，否则无意义
     * -         --> 捕获了'-'之后，又捕获了一个'0'~'9'或'.'之外的异常符号，peekc保存这个异常符号以待下次解析
     * TT_NUMBER --> 成功捕获了一个数字，并存储在nval中，peekc保存继这个数字后捕获的下一个符号以待下次解析
     * TT_WORD   --> 成功捕获了一个字符串，并存储在sval中，peekc保存继这个字符串后捕获的下一个符号（肯定不是数字符号）以待下次解析
     * 引号      --> 成功捕获了引号内的字符串，并存储在sval中，如果发现了成对的引号，peekc存储NEED_CHAR标记，否则，peek存储未知标记以待下次解析
     * /        --> 当/没有被预设为注释区符号，但是需要处理//或/*类的注释，且当前没有匹配成功时，使用peekc存储/，并返回/
     * 其他符号  --> 超出255范围的，保存到sval中，否则忽略掉
     */
    public int nextToken() throws IOException {
        // 返回当前保存的有效值的类型
        if(pushedBack) {
            pushedBack = false;
            return ttype;
        }
        
        byte[] ct = ctype;
        
        // 置空，以容纳读取到的非数字标记
        sval = null;
        
        // 获取上次结束时捕获的符号（可能无意义）
        int c = peekc;
    
        // 如果上次是在文件结尾结束的
        if(c<0) {
            // 表示当前的c需要新值
            c = NEED_CHAR;
        }
    
        // 如果上次是在\r处结尾的
        if(c == SKIP_LF) {
            c = read();
            
            // 如果到了文件结尾
            if(c<0) {
                return ttype = TT_EOF;
            }
            
            // 标记稍后跳过\n
            if(c == '\n') {
                c = NEED_CHAR;
            }
        }
        
        // 如果当前的c需要新值
        if(c == NEED_CHAR) {
            c = read();
            // 如果到了文件结尾，输入流该终止了
            if(c<0) {
                return ttype = TT_EOF;
            }
        }
        
        ttype = c;              /* Just to be safe */
        
        /* Set peekc so that the next invocation of nextToken will read another character unless peekc is reset in this invocation */
        peekc = NEED_CHAR;
        
        // 捕获的字符的类型（对于超出256范围的符号一律视为字母）
        int ctype = c<256 ? ct[c] : CT_ALPHA;
    
        // 如果遇到了空白区的符号
        while((ctype & CT_WHITESPACE) != 0) {
            // 如果是换行符
            if(c == '\r') {
                LINENO++;   // 行号增加
                
                // 如果遇到行尾标记就结束
                if(eolIsSignificantP) {
                    peekc = SKIP_LF;
                    return ttype = TT_EOL;
                }
                
                // 继续捕获下一个字符，以确认是不是\n
                c = read();
                if(c == '\n') {
                    // 如果是\n，跳过它
                    c = read();
                }
            } else {
                if(c == '\n') {
                    LINENO++;
    
                    // 如果遇到行尾标记就结束
                    if(eolIsSignificantP) {
                        return ttype = TT_EOL;
                    }
                }
                
                // 捕获下个字符
                c = read();
            }
            
            // 如果到了文件结尾
            if(c<0) {
                return ttype = TT_EOF;
            }
            
            // 超出256范围的符号被视为字母
            ctype = c<256 ? ct[c] : CT_ALPHA;
        }
        
        // 如果遇到了数字区的符号
        if((ctype & CT_DIGIT) != 0) {
            boolean neg = false;
            
            if(c == '-') {
                c = read();
                
                // 如果捕获了负号，但后面不是预期的数字符号，则需要退出
                if(c != '.' && (c<'0' || c>'9')) {
                    peekc = c;  // 记录本次退出时捕获的符号
                    return ttype = '-'; // 返回符号类型
                }
                neg = true;
            }
            
            double v = 0;       // 保存读到的数字
            int decexp = 0;     // 记录读取的数字被放大的系数（如decexp==2代表放大了100倍）
            int seendot = 0;    // 记录是否存在小数点
            // 处理数字
            while(true) {
                if(c == '.' && seendot == 0) {
                    seendot = 1;
                } else if('0'<=c && c<='9') {
                    v = v * 10 + (c - '0');
                    decexp += seendot;
                } else {
                    break;
                }
                c = read();
            }
            peekc = c;
            
            // 小数点后存在数字
            if(decexp != 0) {
                double denom = 10;
                decexp--;
                while(decexp>0) {
                    denom *= 10;
                    decexp--;
                }
                /* Do one division of a likely-to-be-more-accurate number */
                v = v / denom;  // 把放大的倍数缩小回去
            }
            
            // 保存读到的数字
            nval = neg ? -v : v;
            
            return ttype = TT_NUMBER;
        }
    
        // 如果遇到了字母区的符号
        if((ctype & CT_ALPHA) != 0) {
            int i = 0;
            
            do {
                if(i >= buf.length) {
                    // 缓冲区扩容
                    buf = Arrays.copyOf(buf, buf.length * 2);
                }
                
                // 存储字母
                buf[i++] = (char) c;
                
                // 读取下一个符号
                c = read();
                
                ctype = c<0
                    ? CT_WHITESPACE // 遇到流结束时，标记为空白符号（看似有些反常，但其实这个标记只用来退出循环，后续就丢弃了）
                    : c<256
                      ? ct[c]       // 解析符号类型
                      : CT_ALPHA;   // 超出256范围的符号被认为是字母
                
                // 如果读到了字母或数字，则继续读取（说明紧跟在字母后面的数字也被当成普通字符串）
            } while((ctype & (CT_ALPHA | CT_DIGIT)) != 0);
            
            // 保存继这个字符串之后捕获到的下一个符号
            peekc = c;
            
            // 存储字符串
            sval = String.copyValueOf(buf, 0, i);
            
            // 将捕获到的字符串转为小写形式
            if(forceLower) {
                sval = sval.toLowerCase();
            }
            
            return ttype = TT_WORD;
        }
    
        // 如果遇到了引号
        if((ctype & CT_QUOTE) != 0) {
            // 暂存当前的符号（类型）【引号】
            ttype = c;
            
            int i = 0;
            
            /*
             * Invariants (because \Octal needs a lookahead):
             *   (i)  c contains char value
             *   (ii) d contains the lookahead
             */
            // 查看下一个符号是啥
            int d = read();
            
            // 如果下一个符号不是引号（是引号就返回引号之间的字符串并退出），且不是行尾标记
            while(d >= 0 && d != ttype && d != '\n' && d != '\r') {
                // 如果下个符号是\，则说明遇到了转义符，继续读下去
                if(d == '\\') {
                    // 获取\后面的符号
                    c = read();
                    
                    // 这里允许的字符编号范围：0~255，转换为八进制即\0~\377
                    int first = c;
                    
                    // 遇到了第一个八进制符号
                    if(c >= '0' && c<='7') {
                        c = c - '0';
                        int c2 = read();
    
                        // 遇到了第二个八进制符号
                        if('0'<=c2 && c2<='7') {
                            c = (c << 3) + (c2 - '0');
                            c2 = read();
    
                            // 遇到了第三个八进制符号
                            if('0'<=c2 && c2<='7' && first<='3') {
                                c = (c << 3) + (c2 - '0');
                                // 至此，成功获取了一个\0~\377范围的八进制符号，继续读取下一个符号
                                d = read();
                            } else {
                                d = c2;
                            }
                        } else {
                            d = c2;
                        }
                        
                        // 如果\后面不是数字，则尝试解析为转义符号
                    } else {
                        switch(c) {
                            case 'a':
                                c = 0x7;
                                break;
                            case 'b':
                                c = '\b';
                                break;
                            case 'f':
                                c = 0xC;
                                break;
                            case 'n':
                                c = '\n';
                                break;
                            case 'r':
                                c = '\r';
                                break;
                            case 't':
                                c = '\t';
                                break;
                            case 'v':
                                c = 0xB;
                                break;
                        }
                        
                        // 如果该符号无法解析为转义符，则忽略\的存在，并继续读取\后面的符号
                        d = read();
                    }
                } else {
                    c = d;
                    d = read();
                } // if(d == '\\')
                
                if(i >= buf.length) {
                    buf = Arrays.copyOf(buf, buf.length * 2);
                }
                
                /*
                 * 至此，d总是存储了下一个待解析的符号
                 *
                 * 而c存储的值可能是：
                 * 如果\后面是数字型的转义符，则用c来保存该数字
                 * 如果\后面是非数字符号，且不是正确的转义符号，依然用c来保存
                 * 如果不存在\，则c保存这个未知符号
                 */
                buf[i++] = (char) c;
            } // while
            
            /*
             * If we broke out of the loop because we found a matching quote character then arrange to read a new character next time around;
             * otherwise, save the character.
             */
            // 如果d是引号，标记下次需要读取新字符，否则，按原样存储以待解析
            peekc = (d == ttype) ? NEED_CHAR : d;
            
            // 保存引号区内的字符串
            sval = String.copyValueOf(buf, 0, i);
            
            return ttype;
        }
        
        // 如果遇到了/，且需要处理/*或者//风格的注释
        if(c == '/' && (slashSlashCommentsP || slashStarCommentsP)) {
            // 获取/后面的符号
            c = read();
            
            // 匹配到了/*风格的注释
            if(c == '*' && slashStarCommentsP) {
                int prevc = 0;
                
                // 遇到*/则关闭注释
                while((c = read()) != '/' || prevc != '*') {
                    // 遇到换行符
                    if(c == '\r') {
                        // 行号增加
                        LINENO++;
                        // 处理了\r，继续读取
                        c = read();
                        if(c == '\n') {
                            // 处理了\r\n，继续读取
                            c = read();
                        }
                    } else {
                        // 遇到回车符
                        if(c == '\n') {
                            // 行号增加
                            LINENO++;
                            // 继续读取
                            c = read();
                        }
                    }
                    
                    // 遇到了文件结束符
                    if(c<0) {
                        return ttype = TT_EOF;
                    }
                    
                    prevc = c;
                }
                
                // 如果注释被正常关闭，忽略注释中包含的内容，开始下一轮读取
                return nextToken();
    
                // 匹配到了//风格的注释
            } else if(c == '/' && slashSlashCommentsP) {
                // 一致读到行尾
                while((c = read()) != '\n' && c != '\r' && c >= 0)
                    ;
                
                // 记录行尾标记
                peekc = c;
                
                // 忽略该行内容，进入下一轮读取
                return nextToken();
            } else {
                // 如果/属于预设的注释符号，则读取并丢弃整行内容
                if((ct['/'] & CT_COMMENT) != 0) {
                    while((c = read()) != '\n' && c != '\r' && c >= 0)
                        ;
                    // 记录行尾标记
                    peekc = c;
                    // 忽略该行内容，进入下一轮读取
                    return nextToken();
                } else {
                    // 如果/不是预设的注释符号，那么记录并返回/符号
                    peekc = c;
                    return ttype = '/';
                }
            }
        }
    
        // 如果遇到了注释区的符号（默认是/，意味着/开头到行尾的内容会被忽略）
        if((ctype & CT_COMMENT) != 0) {
            // 一直读到行尾
            while((c = read()) != '\n' && c != '\r' && c >= 0)
                ;
            // 存储行尾符号
            peekc = c;
            // 忽略该行内容，进入下一轮读取
            return nextToken();
        }
        
        return ttype = c;
    }
    
    /*▲ 获取标记 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 类型图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Specifies that all characters <i>c</i> in the range
     * <code>low&nbsp;&lt;=&nbsp;<i>c</i>&nbsp;&lt;=&nbsp;high</code>
     * are word constituents. A word token consists of a word constituent
     * followed by zero or more word constituents or number constituents.
     *
     * @param low the low end of the range.
     * @param high  the high end of the range.
     */
    // 指定low到high区域为字母区
    public void wordChars(int low, int high) {
        if(low<0) {
            low = 0;
        }
        
        if(high >= ctype.length) {
            high = ctype.length - 1;
        }
        
        while(low<=high) {
            ctype[low++] |= CT_ALPHA;
        }
    }
    
    /**
     * Specifies that all characters c in the range low~high are white space characters.
     * White space characters serve only to separate tokens in the input stream.
     *
     * Any other attribute settings for the characters in the specified range are cleared.
     *
     * @param low the low end of the range.
     * @param high  the high end of the range.
     */
    // 指定low到high区域为空白符号区
    public void whitespaceChars(int low, int high) {
        if(low<0) {
            low = 0;
        }
        
        if(high >= ctype.length) {
            high = ctype.length - 1;
        }
        
        while(low<=high) {
            ctype[low++] = CT_WHITESPACE;
        }
    }
    
    /**
     * Specifies that numbers should be parsed by this tokenizer. The
     * syntax table of this tokenizer is modified so that each of the twelve
     * characters:
     * <blockquote><pre>
     *      0 1 2 3 4 5 6 7 8 9 . -
     * </pre></blockquote>
     * <p>
     * has the "numeric" attribute.
     * <p>
     * When the parser encounters a word token that has the format of a
     * double precision floating-point number, it treats the token as a
     * number rather than a word, by setting the {@code ttype}
     * field to the value {@code TT_NUMBER} and putting the numeric
     * value of the token into the {@code nval} field.
     *
     * @see java.io.StreamTokenizer#nval
     * @see java.io.StreamTokenizer#TT_NUMBER
     * @see java.io.StreamTokenizer#ttype
     */
    // 指定'0'到'9'以及'.'和'-'区域为数字区（不支持自定义，但可被空白区覆盖）
    public void parseNumbers() {
        for(int i = '0'; i<='9'; i++) {
            ctype[i] |= CT_DIGIT;
        }
        
        ctype['.'] |= CT_DIGIT;
        ctype['-'] |= CT_DIGIT;
    }
    
    /**
     * Specified that the character argument starts a single-line
     * comment. All characters from the comment character to the end of
     * the line are ignored by this stream tokenizer.
     *
     * <p>Any other attribute settings for the specified character are cleared.
     *
     * @param ch the character.
     */
    // 指定ch所处区域为注释符号区（一般认为是#或/开头的序列）
    public void commentChar(int ch) {
        if(ch >= 0 && ch<ctype.length) {
            ctype[ch] = CT_COMMENT;
        }
    }
    
    /**
     * Specifies that matching pairs of this character delimit string
     * constants in this tokenizer.
     * <p>
     * When the {@code nextToken} method encounters a string
     * constant, the {@code ttype} field is set to the string
     * delimiter and the {@code sval} field is set to the body of
     * the string.
     * <p>
     * If a string quote character is encountered, then a string is
     * recognized, consisting of all characters after (but not including)
     * the string quote character, up to (but not including) the next
     * occurrence of that same string quote character, or a line
     * terminator, or end of file. The usual escape sequences such as
     * {@code "\u005Cn"} and {@code "\u005Ct"} are recognized and
     * converted to single characters as the string is parsed.
     *
     * <p>Any other attribute settings for the specified character are cleared.
     *
     * @param ch the character.
     *
     * @see java.io.StreamTokenizer#nextToken()
     * @see java.io.StreamTokenizer#sval
     * @see java.io.StreamTokenizer#ttype
     */
    // 指定ch所处区域为引号符号区（例如"和'）
    public void quoteChar(int ch) {
        if(ch >= 0 && ch<ctype.length) {
            ctype[ch] = CT_QUOTE;
        }
    }
    
    /**
     * Specifies that all characters <i>c</i> in the range
     * <code>low&nbsp;&lt;=&nbsp;<i>c</i>&nbsp;&lt;=&nbsp;high</code>
     * are "ordinary" in this tokenizer. See the
     * {@code ordinaryChar} method for more information on a
     * character being ordinary.
     *
     * @param low the low end of the range.
     * @param high  the high end of the range.
     *
     * @see java.io.StreamTokenizer#ordinaryChar(int)
     */
    // 指定low到high区域为普通符号区
    public void ordinaryChars(int low, int high) {
        if(low<0) {
            low = 0;
        }
        
        if(high >= ctype.length) {
            high = ctype.length - 1;
        }
        
        while(low<=high) {
            ctype[low++] = 0;
        }
    }
    
    /**
     * Specifies that the character argument is "ordinary"
     * in this tokenizer. It removes any special significance the
     * character has as a comment character, word component, string
     * delimiter, white space, or number character. When such a character
     * is encountered by the parser, the parser treats it as a
     * single-character token and sets {@code ttype} field to the
     * character value.
     *
     * <p>Making a line terminator character "ordinary" may interfere
     * with the ability of a {@code StreamTokenizer} to count
     * lines. The {@code lineno} method may no longer reflect
     * the presence of such terminator characters in its line count.
     *
     * @param ch the character.
     *
     * @see java.io.StreamTokenizer#ttype
     */
    // 指定ch所处区域为普通符号区
    public void ordinaryChar(int ch) {
        if(ch >= 0 && ch<ctype.length) {
            ctype[ch] = 0;
        }
    }
    
    /**
     * Resets this tokenizer's syntax table so that all characters are
     * "ordinary." See the {@code ordinaryChar} method
     * for more information on a character being ordinary.
     *
     * @see java.io.StreamTokenizer#ordinaryChar(int)
     */
    // 重置类型图
    public void resetSyntax() {
        for(int i = ctype.length; --i >= 0; ) {
            ctype[i] = 0;
        }
    }
    
    /*▲ 类型图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Determines whether or not ends of line are treated as tokens.
     * If the flag argument is true, this tokenizer treats end of lines
     * as tokens; the {@code nextToken} method returns
     * {@code TT_EOL} and also sets the {@code ttype} field to
     * this value when an end of line is read.
     * <p>
     * A line is a sequence of characters ending with either a
     * carriage-return character ({@code '\u005Cr'}) or a newline
     * character ({@code '\u005Cn'}). In addition, a carriage-return
     * character followed immediately by a newline character is treated
     * as a single end-of-line token.
     * <p>
     * If the {@code flag} is false, end-of-line characters are
     * treated as white space and serve only to separate tokens.
     *
     * @param flag {@code true} indicates that end-of-line characters
     *             are separate tokens; {@code false} indicates that
     *             end-of-line characters are white space.
     *
     * @see java.io.StreamTokenizer#nextToken()
     * @see java.io.StreamTokenizer#ttype
     * @see java.io.StreamTokenizer#TT_EOL
     */
    // 遇到行尾标记\r或\n时是否结束读取
    public void eolIsSignificant(boolean flag) {
        eolIsSignificantP = flag;
    }
    
    /**
     * Determines whether or not the tokenizer recognizes C-style comments.
     * If the flag argument is {@code true}, this stream tokenizer
     * recognizes C-style comments. All text between successive
     * occurrences of {@code /*} and <code>*&#47;</code> are discarded.
     * <p>
     * If the flag argument is {@code false}, then C-style comments
     * are not treated specially.
     *
     * @param flag {@code true} indicates to recognize and ignore
     *             C-style comments.
     */
    // 是否处理/*风格的注释
    public void slashStarComments(boolean flag) {
        slashStarCommentsP = flag;
    }
    
    /**
     * Determines whether or not the tokenizer recognizes C++-style comments.
     * If the flag argument is {@code true}, this stream tokenizer
     * recognizes C++-style comments. Any occurrence of two consecutive
     * slash characters ({@code '/'}) is treated as the beginning of
     * a comment that extends to the end of the line.
     * <p>
     * If the flag argument is {@code false}, then C++-style
     * comments are not treated specially.
     *
     * @param flag {@code true} indicates to recognize and ignore
     *             C++-style comments.
     */
    // 是否处理//风格的注释
    public void slashSlashComments(boolean flag) {
        slashSlashCommentsP = flag;
    }
    
    /**
     * Determines whether or not word token are automatically lowercased.
     * If the flag argument is {@code true}, then the value in the
     * {@code sval} field is lowercased whenever a word token is
     * returned (the {@code ttype} field has the
     * value {@code TT_WORD} by the {@code nextToken} method
     * of this tokenizer.
     * <p>
     * If the flag argument is {@code false}, then the
     * {@code sval} field is not modified.
     *
     * @param fl {@code true} indicates that all word tokens should
     *           be lowercased.
     *
     * @see java.io.StreamTokenizer#nextToken()
     * @see java.io.StreamTokenizer#ttype
     * @see java.io.StreamTokenizer#TT_WORD
     */
    // 是否将捕获到的字符串转为小写形式
    public void lowerCaseMode(boolean fl) {
        forceLower = fl;
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Causes the next call to the {@code nextToken} method of this tokenizer to return the current value in the {@code ttype} field,
     * and not to modify the value in the {@code nval} or {@code sval} field.
     *
     * @see java.io.StreamTokenizer#nextToken()
     * @see java.io.StreamTokenizer#nval
     * @see java.io.StreamTokenizer#sval
     * @see java.io.StreamTokenizer#ttype
     */
    /*
     * 指示下次调用nextToken()时，返回当前识别到的有效值的类型，
     * 然后根据返回的类型进一步获取有效值。
     *
     * 该效果只维持一次（如有需要，则应该多次设置）
     */
    public void pushBack() {
        /* No-op if nextToken() not called */
        if(ttype != TT_NOTHING) {
            pushedBack = true;
        }
    }
    
    /**
     * Return the current line number.
     *
     * @return the current line number of this stream tokenizer.
     */
    // 返回当前读到的标记所在的行号
    public int lineno() {
        return LINENO;
    }
    
    /**
     * Returns the string representation of the current stream token and
     * the line number it occurs on.
     *
     * <p>The precise string returned is unspecified, although the following
     * example can be considered typical:
     *
     * <blockquote><pre>Token['a'], line 10</pre></blockquote>
     *
     * @return a string representation of the token
     *
     * @see java.io.StreamTokenizer#nval
     * @see java.io.StreamTokenizer#sval
     * @see java.io.StreamTokenizer#ttype
     */
    public String toString() {
        String ret;
        switch(ttype) {
            case TT_EOF:
                ret = "EOF";
                break;
            case TT_EOL:
                ret = "EOL";
                break;
            case TT_WORD:
                ret = sval;
                break;
            case TT_NUMBER:
                ret = "n=" + nval;
                break;
            case TT_NOTHING:
                ret = "NOTHING";
                break;
            default: {
                /*
                 * ttype is the first character of either a quoted string or
                 * is an ordinary character. ttype can definitely not be less
                 * than 0, since those are reserved values used in the previous
                 * case statements
                 */
                if(ttype<256 && ((ctype[ttype] & CT_QUOTE) != 0)) {
                    ret = sval;
                    break;
                }
    
                char[] s = new char[3];
                s[0] = s[2] = '\'';
                s[1] = (char) ttype;
                ret = new String(s);
                break;
            }
        }
        return "Token[" + ret + "], line " + LINENO;
    }
    
    /** Read the next character */
    private int read() throws IOException {
        if(reader != null) {
            return reader.read();
        } else if(input != null) {
            return input.read();
        } else {
            throw new IllegalStateException();
        }
    }
    
}
