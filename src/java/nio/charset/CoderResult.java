/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A description of the result state of a coder.
 *
 * <p> A charset coder, that is, either a decoder or an encoder, consumes bytes
 * (or characters) from an input buffer, translates them, and writes the
 * resulting characters (or bytes) to an output buffer.  A coding process
 * terminates for one of four categories of reasons, which are described by
 * instances of this class:
 *
 * <ul>
 *
 *   <li><p> <i>Underflow</i> is reported when there is no more input to be
 *   processed, or there is insufficient input and additional input is
 *   required.  This condition is represented by the unique result object
 *   {@link #UNDERFLOW}, whose {@link #isUnderflow() isUnderflow} method
 *   returns {@code true}.  </p></li>
 *
 *   <li><p> <i>Overflow</i> is reported when there is insufficient room
 *   remaining in the output buffer.  This condition is represented by the
 *   unique result object {@link #OVERFLOW}, whose {@link #isOverflow()
 *   isOverflow} method returns {@code true}.  </p></li>
 *
 *   <li><p> A <i>malformed-input error</i> is reported when a sequence of
 *   input units is not well-formed.  Such errors are described by instances of
 *   this class whose {@link #isMalformed() isMalformed} method returns
 *   {@code true} and whose {@link #length() length} method returns the length
 *   of the malformed sequence.  There is one unique instance of this class for
 *   all malformed-input errors of a given length.  </p></li>
 *
 *   <li><p> An <i>unmappable-character error</i> is reported when a sequence
 *   of input units denotes a character that cannot be represented in the
 *   output charset.  Such errors are described by instances of this class
 *   whose {@link #isUnmappable() isUnmappable} method returns {@code true} and
 *   whose {@link #length() length} method returns the length of the input
 *   sequence denoting the unmappable character.  There is one unique instance
 *   of this class for all unmappable-character errors of a given length.
 *   </p></li>
 *
 * </ul>
 *
 * <p> For convenience, the {@link #isError() isError} method returns {@code true}
 * for result objects that describe malformed-input and unmappable-character
 * errors but {@code false} for those that describe underflow or overflow
 * conditions.  </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

/*
 * CoderResult用来记录编码/解码过程的结束原因，其对象由CharsetEncoder或CharsetDecoder对象返回
 *
 * 编码/解码结果的四种状态：
 *
 * Underflow（下溢）
 *      正常情况，表示需要更多的输入。
 *      或者是输入CharBuffer内容不足；或者，如果它不为空，在没有额外的输入的情况下，余下的字符无法进行处理。
 *      更新CharBuffer的位置解决被编码器消耗的字符的问题。
 *      在CharBuffer中填入更多的编码字符（首先在缓冲区上调用compact()，如果是非空的情况）并再次调用encode()继续。
 *      如果结束了，用空CharBuffer调用encode()并且endOfInput为true，之后调用flush()确保所有的字节都被发送给ByteBuffer。
 *      下溢条件总是返回相同的对象实例：名为CharsetEncoder.UNDERFLOW的静态类变量。
 *      这就使您可以使用返回的对象句柄上的等号运算符（==）来对下溢进行检测。
 *
 * Overflow（上溢）
 *      表示编码器充满了输出ByteBuffer并且需要产生更多的编码输出。
 *      输入CharBuffer对象可能会或可能不会被耗尽。
 *      这是正常条件，不表示出错。
 *      您应该消耗ByteBuffer但是不应该扰乱CharBuffer，CharBuffer将更新它的位置，之后再次调用encode()，重复进行直到得到下溢结果。
 *      与下溢类似的，上溢返回一致的实例，CharsetEncoder.OVERFLOW，它可直接用于等式比较。
 *
 * Malformed input（有缺陷的输入）
 *      编码时，这个通常意味着字符包含16位的数值，不是有效的Unicode字符。
 *      对于解码来说，这意味着解码器遭遇了不识别的字节序列。
 *      返回的CoderResult实例将不是单一的参数，因为它是针对下溢和上溢的。
 *
 * Unmappable character（无法映射字符）
 *      表示编码器不能映射字符或字符序列到字节上。
 *      例如，如果您正在使用ISO-8859-1编码但您的输入CharBuffer包含非拉丁Unicode字符。
 *      对于解码，解码器知道输入字节序列但是不了解如何创建相符的字符。
 *
 *
 * 编码时，如果编码器遭遇了有缺陷的或不能映射的输入，就会返回结果对象。
 * 或者，也可以调用CharsetEncoder#canEncode检测独立的字符，或者字符序列，来确定它们是否能被编码。
 */
public class CoderResult {
    
    // 编码/解码结束后的状态，由字段tyoe存储
    private static final int CR_UNDERFLOW = 0;  // 下溢，输出缓冲区仍有空闲
    private static final int CR_OVERFLOW = 1;   // 上溢，输出缓冲区已经满了
    private static final int CR_ERROR_MIN = 2;  // 错误，包括CR_MALFORMED和CR_UNMAPPABLE
    private static final int CR_MALFORMED = 2;  // 有缺陷的输入（比如输入超出了Unicode字符范围）
    private static final int CR_UNMAPPABLE = 3; // 无法映射字符（比如输入的是Unicode字符，但是不被当前字符集识别）
    
    private final int type;     // 编码/解码结束后的状态
    
    
    /**
     * Result object indicating underflow, meaning that either the input buffer has been completely consumed or,
     * if the input buffer is not yet empty, that additional input is required.
     */
    // 发生下溢，所有下溢情形返回的唯一实例
    public static final CoderResult UNDERFLOW = new CoderResult(CR_UNDERFLOW, 0);
    
    /**
     * Result object indicating overflow, meaning that there is insufficient room in the output buffer.
     */
    // 发生上溢，所有上溢情形返回的唯一实例
    public static final CoderResult OVERFLOW = new CoderResult(CR_OVERFLOW, 0);
    
    // 发生错误，存在有缺陷的输入
    private static final CoderResult[] malformed4 = new CoderResult[] {
        new CoderResult(CR_MALFORMED, 1),
        new CoderResult(CR_MALFORMED, 2),
        new CoderResult(CR_MALFORMED, 3),
        new CoderResult(CR_MALFORMED, 4),
    };
    
    // 发生错误，存在无法映射字符
    private static final CoderResult[] unmappable4 = new CoderResult[] {
        new CoderResult(CR_UNMAPPABLE, 1),
        new CoderResult(CR_UNMAPPABLE, 2),
        new CoderResult(CR_UNMAPPABLE, 3),
        new CoderResult(CR_UNMAPPABLE, 4),
    };
    
    // 出现编码/解码错误的位置处的字节数（不同的字符集一次可识别的字符数不同）
    private final int length;
    
    
    private static final String[] names = {"UNDERFLOW", "OVERFLOW", "MALFORMED", "UNMAPPABLE"};
    
    
    
    private CoderResult(int type, int length) {
        this.type = type;
        this.length = length;
    }
    
    /**
     * Tells whether or not this object describes an underflow condition.
     *
     * @return {@code true} if, and only if, this object denotes underflow
     */
    // 发生下溢，输出缓冲区仍有空闲
    public boolean isUnderflow() {
        return (type == CR_UNDERFLOW);
    }
    
    /**
     * Tells whether or not this object describes an overflow condition.
     *
     * @return {@code true} if, and only if, this object denotes overflow
     */
    // 发生上溢，输出缓冲区已经满了
    public boolean isOverflow() {
        return (type == CR_OVERFLOW);
    }
    
    /**
     * Tells whether or not this object describes an error condition.
     *
     * @return {@code true} if, and only if, this object denotes either a
     * malformed-input error or an unmappable-character error
     */
    // 发生错误，包括CR_MALFORMED和CR_UNMAPPABLE
    public boolean isError() {
        return (type >= CR_ERROR_MIN);
    }
    
    /**
     * Tells whether or not this object describes a malformed-input error.
     *
     * @return {@code true} if, and only if, this object denotes a
     * malformed-input error
     */
    // 存在有缺陷的输入
    public boolean isMalformed() {
        return (type == CR_MALFORMED);
    }
    
    /**
     * Tells whether or not this object describes an unmappable-character
     * error.
     *
     * @return {@code true} if, and only if, this object denotes an
     * unmappable-character error
     */
    // 存在无法映射字符
    public boolean isUnmappable() {
        return (type == CR_UNMAPPABLE);
    }
    
    /**
     * Returns the length of the erroneous input described by this
     * object&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * @return The length of the erroneous input, a positive integer
     *
     * @throws UnsupportedOperationException If this object does not describe an error condition, that is,
     *                                       if the {@link #isError() isError} does not return {@code true}
     */
    // 返回发现的错误字节数
    public int length() {
        if(!isError())
            throw new UnsupportedOperationException();
        return length;
    }
    
    /**
     * Static factory method that returns the unique object describing a
     * malformed-input error of the given length.
     *
     * @param   length
     *          The given length
     *
     * @return  The requested coder-result object
     */
    // 返回malformed错误实例
    public static CoderResult malformedForLength(int length) {
        if(length<=0)
            throw new IllegalArgumentException("Non-positive length");
        if(length<=4)
            return malformed4[length - 1];
        return Cache.INSTANCE.malformed.computeIfAbsent(length, n -> new CoderResult(CR_MALFORMED, n));
    }
    
    /**
     * Static factory method that returns the unique result object describing
     * an unmappable-character error of the given length.
     *
     * @param   length
     *          The given length
     *
     * @return  The requested coder-result object
     */
    // 返回unmappable错误实例
    public static CoderResult unmappableForLength(int length) {
        if(length<=0)
            throw new IllegalArgumentException("Non-positive length");
        if(length<=4)
            return unmappable4[length - 1];
        return Cache.INSTANCE.unmappable.computeIfAbsent(length, n -> new CoderResult(CR_UNMAPPABLE, n));
    }
    
    
    /**
     * Returns a string describing this coder result.
     *
     * @return A descriptive string
     */
    public String toString() {
        String nm = names[type];
        return isError() ? nm + "[" + length + "]" : nm;
    }
    
    
    /**
     * Throws an exception appropriate to the result described by this object.
     *
     * @throws  BufferUnderflowException
     *          If this object is {@link #UNDERFLOW}
     *
     * @throws  BufferOverflowException
     *          If this object is {@link #OVERFLOW}
     *
     * @throws  MalformedInputException
     *          If this object represents a malformed-input error; the
     *          exception's length value will be that of this object
     *
     * @throws  UnmappableCharacterException
     *          If this object represents an unmappable-character error; the
     *          exceptions length value will be that of this object
     */
    public void throwException() throws CharacterCodingException {
        switch(type) {
            case CR_UNDERFLOW:
                throw new BufferUnderflowException();
            case CR_OVERFLOW:
                throw new BufferOverflowException();
            case CR_MALFORMED:
                throw new MalformedInputException(length);
            case CR_UNMAPPABLE:
                throw new UnmappableCharacterException(length);
            default:
                assert false;
        }
    }
    
    
    
    private static final class Cache {
        static final Cache INSTANCE = new Cache();
        
        final Map<Integer, CoderResult> unmappable = new ConcurrentHashMap<>();
        final Map<Integer, CoderResult> malformed = new ConcurrentHashMap<>();
        
        private Cache() {
        }
    }
}
