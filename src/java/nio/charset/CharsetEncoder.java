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

// -- This file was mechanically generated: Do not edit! -- //

package java.nio.charset;

import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

/**
 * An engine that can transform a sequence of sixteen-bit Unicode characters into a sequence of
 * bytes in a specific charset.
 *
 * <a id="steps"></a>
 *
 * <p> The input character sequence is provided in a character buffer or a series
 * of such buffers.  The output byte sequence is written to a byte buffer
 * or a series of such buffers.  An encoder should always be used by making
 * the following sequence of method invocations, hereinafter referred to as an
 * <i>encoding operation</i>:
 *
 * <ol>
 *
 * <li><p> Reset the encoder via the {@link #reset reset} method, unless it
 * has not been used before; </p></li>
 *
 * <li><p> Invoke the {@link #encode encode} method zero or more times, as
 * long as additional input may be available, passing {@code false} for the
 * {@code endOfInput} argument and filling the input buffer and flushing the
 * output buffer between invocations; </p></li>
 *
 * <li><p> Invoke the {@link #encode encode} method one final time, passing
 * {@code true} for the {@code endOfInput} argument; and then </p></li>
 *
 * <li><p> Invoke the {@link #flush flush} method so that the encoder can
 * flush any internal state to the output buffer. </p></li>
 *
 * </ol>
 *
 * Each invocation of the {@link #encode encode} method will encode as many
 * characters as possible from the input buffer, writing the resulting bytes
 * to the output buffer.  The {@link #encode encode} method returns when more
 * input is required, when there is not enough room in the output buffer, or
 * when an encoding error has occurred.  In each case a {@link CoderResult}
 * object is returned to describe the reason for termination.  An invoker can
 * examine this object and fill the input buffer, flush the output buffer, or
 * attempt to recover from an encoding error, as appropriate, and try again.
 *
 * <a id="ce"></a>
 *
 * <p> There are two general types of encoding errors.  If the input character
 * sequence is not a legal sixteen-bit Unicode sequence then the input is considered <i>malformed</i>.  If
 * the input character sequence is legal but cannot be mapped to a valid
 * byte sequence in the given charset then an <i>unmappable character</i> has been encountered.
 *
 * <a id="cae"></a>
 *
 * <p> How an encoding error is handled depends upon the action requested for
 * that type of error, which is described by an instance of the {@link
 * CodingErrorAction} class.  The possible error actions are to {@linkplain
 * CodingErrorAction#IGNORE ignore} the erroneous input, {@linkplain
 * CodingErrorAction#REPORT report} the error to the invoker via
 * the returned {@link CoderResult} object, or {@linkplain CodingErrorAction#REPLACE
 * replace} the erroneous input with the current value of the
 * replacement byte array.  The replacement
 *
 *
 * is initially set to the encoder's default replacement, which often
 * (but not always) has the initial value&nbsp;<code>{</code>&nbsp;<code>(byte)'?'</code>&nbsp;<code>}</code>;
 *
 *
 *
 *
 *
 * its value may be changed via the {@link #replaceWith(byte[])
 * replaceWith} method.
 *
 * <p> The default action for malformed-input and unmappable-character errors
 * is to {@linkplain CodingErrorAction#REPORT report} them.  The
 * malformed-input error action may be changed via the {@link
 * #onMalformedInput(CodingErrorAction) onMalformedInput} method; the
 * unmappable-character action may be changed via the {@link
 * #onUnmappableCharacter(CodingErrorAction) onUnmappableCharacter} method.
 *
 * <p> This class is designed to handle many of the details of the encoding
 * process, including the implementation of error actions.  An encoder for a
 * specific charset, which is a concrete subclass of this class, need only
 * implement the abstract {@link #encodeLoop encodeLoop} method, which
 * encapsulates the basic encoding loop.  A subclass that maintains internal
 * state should, additionally, override the {@link #implFlush implFlush} and
 * {@link #implReset implReset} methods.
 *
 * <p> Instances of this class are not safe for use by multiple concurrent
 * threads.  </p>
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @see ByteBuffer
 * @see CharBuffer
 * @see Charset
 * @see CharsetDecoder
 * @since 1.4
 */

// 字符编码器。字符进来，字节出去，完成对字符序列的编码操作
public abstract class CharsetEncoder {
    private static final int ST_RESET = 0;
    private static final int ST_CODING = 1;
    private static final int ST_END = 2;
    private static final int ST_FLUSHED = 3;
    
    private static String stateNames[] = {"RESET", "CODING", "CODING_END", "FLUSHED"};
    
    private final Charset charset;  // 当前编码器对应的字符集
    
    /*
     * 表示编码每个给定的字符所需的平均字节数量。
     * 当编码字符时，编码运算法则可以选择调节字节边界，或者一些字符可以编码成大于其自身字节的字节（例如UTF-8编码）
     * 该字段可用来确定ByteBuffer的近似尺寸，ByteBuffer包含了给定字符的编码字节
     */
    private final float averageBytesPerChar;
    
    /*
     * 表示编码每个给定的字符所需的最大字节数量。
     * 该字段的值乘以被编码的字符数量将得出最坏情况下输出缓冲区的大小。
     */
    private final float maxBytesPerChar;
    
    private byte[] replacement; // 替换元素
    
    private CodingErrorAction malformedInputAction = CodingErrorAction.REPORT;
    private CodingErrorAction unmappableCharacterAction = CodingErrorAction.REPORT;
    
    private int state = ST_RESET;
    
    private WeakReference<CharsetDecoder> cachedDecoder = null;
    
    
    /**
     * Initializes a new encoder.  The new encoder will have the given
     * bytes-per-char and replacement values.
     *
     * @param cs                  The charset that created this encoder
     * @param averageBytesPerChar A positive float value indicating the expected number of
     *                            bytes that will be produced for each input character
     * @param maxBytesPerChar     A positive float value indicating the maximum number of
     *                            bytes that will be produced for each input character
     * @param replacement         The initial replacement; must not be {@code null}, must have
     *                            non-zero length, must not be longer than maxBytesPerChar,
     *                            and must be {@linkplain #isLegalReplacement legal}
     *
     * @throws IllegalArgumentException If the preconditions on the parameters do not hold
     */
    protected CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar, byte[] replacement) {
        this.charset = cs;
        if(averageBytesPerChar<=0.0f) {
            throw new IllegalArgumentException("Non-positive " + "averageBytesPerChar");
        }
        if(maxBytesPerChar<=0.0f) {
            throw new IllegalArgumentException("Non-positive " + "maxBytesPerChar");
        }
        if(averageBytesPerChar>maxBytesPerChar) {
            throw new IllegalArgumentException("averageBytesPerChar" + " exceeds " + "maxBytesPerChar");
        }
        this.replacement = replacement;
        this.averageBytesPerChar = averageBytesPerChar;
        this.maxBytesPerChar = maxBytesPerChar;
        replaceWith(replacement);
    }
    
    /**
     * Initializes a new encoder.  The new encoder will have the given
     * bytes-per-char values and its replacement will be the
     * byte array <code>{</code>&nbsp;<code>(byte)'?'</code>&nbsp;<code>}</code>.
     *
     * @param cs                  The charset that created this encoder
     * @param averageBytesPerChar A positive float value indicating the expected number of
     *                            bytes that will be produced for each input character
     * @param maxBytesPerChar     A positive float value indicating the maximum number of
     *                            bytes that will be produced for each input character
     *
     * @throws IllegalArgumentException If the preconditions on the parameters do not hold
     */
    protected CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar) {
        this(cs, averageBytesPerChar, maxBytesPerChar, new byte[]{(byte) '?'});
    }
    
    
    
    /*▼ 编码 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Convenience method that encodes the remaining content of a single input
     * character buffer into a newly-allocated byte buffer.
     *
     * <p> This method implements an entire <a href="#steps">encoding
     * operation</a>; that is, it resets this encoder, then it encodes the
     * characters in the given character buffer, and finally it flushes this
     * encoder.  This method should therefore not be invoked if an encoding
     * operation is already in progress.  </p>
     *
     * @param in The input character buffer
     *
     * @return A newly-allocated byte buffer containing the result of the
     * encoding operation.  The buffer's position will be zero and its
     * limit will follow the last byte written.
     *
     * @throws IllegalStateException        If an encoding operation is already in progress
     * @throws MalformedInputException      If the character sequence starting at the input buffer's current
     *                                      position is not a legal sixteen-bit Unicode sequence and the current malformed-input action
     *                                      is {@link CodingErrorAction#REPORT}
     * @throws UnmappableCharacterException If the character sequence starting at the input buffer's current
     *                                      position cannot be mapped to an equivalent byte sequence and
     *                                      the current unmappable-character action is {@link
     *                                      CodingErrorAction#REPORT}
     */
    // 编码字符序列，将编码结果写入到字节缓冲区返回
    public final ByteBuffer encode(CharBuffer in) throws CharacterCodingException {
        // 编码所有字符需要的大概容量
        int n = (int) (in.remaining() * averageBytesPerChar());
        
        // 输出缓冲区，用来存储编码后的字节序列
        ByteBuffer out = ByteBuffer.allocate(n);
        
        if((n == 0) && (in.remaining() == 0)) {
            return out;
        }
        
        reset();
        
        for(; ; ) {
            CoderResult cr = in.hasRemaining()  // 判断是否还有未解析的输入
                ? encode(in, out, true)         // 如果存在未解析的输入，则继续编码
                : CoderResult.UNDERFLOW;
            
            // 发生下溢，输出缓冲区仍有空闲
            if(cr.isUnderflow()) {
                // 刷新输出缓冲区，代表这次解析结束了
                cr = flush(out);
            }
            
            // 发生下溢，输出缓冲区仍有空闲
            if(cr.isUnderflow()) {
                break;
            }
            
            // 输入太多，输出缓冲区不够用了，则需要扩容
            if(cr.isOverflow()) {
                n = 2 * n + 1;    // Ensure progress; n might be 0!
                ByteBuffer o = ByteBuffer.allocate(n);  // 创建新缓冲区
                out.flip(); // 将旧缓冲区从写模式切换到度模式
                o.put(out); // 将旧缓冲区中的内容读到新缓冲区中
                out = o;    // 用out指向新缓冲区
                continue;
            }
            
            cr.throwException();
        }
        
        // 编码完成后，将输出缓冲区从写模式切换到读模式
        out.flip();
        
        // 返回编码结果
        return out;
    }
    
    /**
     * Encodes as many characters as possible from the given input buffer,
     * writing the results to the given output buffer.
     *
     * <p> The buffers are read from, and written to, starting at their current
     * positions.  At most {@link Buffer#remaining in.remaining()} characters
     * will be read and at most {@link Buffer#remaining out.remaining()}
     * bytes will be written.  The buffers' positions will be advanced to
     * reflect the characters read and the bytes written, but their marks and
     * limits will not be modified.
     *
     * <p> In addition to reading characters from the input buffer and writing
     * bytes to the output buffer, this method returns a {@link CoderResult}
     * object to describe its reason for termination:
     *
     * <ul>
     *
     * <li><p> {@link CoderResult#UNDERFLOW} indicates that as much of the
     * input buffer as possible has been encoded.  If there is no further
     * input then the invoker can proceed to the next step of the
     * <a href="#steps">encoding operation</a>.  Otherwise this method
     * should be invoked again with further input.  </p></li>
     *
     * <li><p> {@link CoderResult#OVERFLOW} indicates that there is
     * insufficient space in the output buffer to encode any more characters.
     * This method should be invoked again with an output buffer that has
     * more {@linkplain Buffer#remaining remaining} bytes. This is
     * typically done by draining any encoded bytes from the output
     * buffer.  </p></li>
     *
     * <li><p> A {@linkplain CoderResult#malformedForLength
     * malformed-input} result indicates that a malformed-input
     * error has been detected.  The malformed characters begin at the input
     * buffer's (possibly incremented) position; the number of malformed
     * characters may be determined by invoking the result object's {@link
     * CoderResult#length() length} method.  This case applies only if the
     * {@linkplain #onMalformedInput malformed action} of this encoder
     * is {@link CodingErrorAction#REPORT}; otherwise the malformed input
     * will be ignored or replaced, as requested.  </p></li>
     *
     * <li><p> An {@linkplain CoderResult#unmappableForLength
     * unmappable-character} result indicates that an
     * unmappable-character error has been detected.  The characters that
     * encode the unmappable character begin at the input buffer's (possibly
     * incremented) position; the number of such characters may be determined
     * by invoking the result object's {@link CoderResult#length() length}
     * method.  This case applies only if the {@linkplain #onUnmappableCharacter
     * unmappable action} of this encoder is {@link
     * CodingErrorAction#REPORT}; otherwise the unmappable character will be
     * ignored or replaced, as requested.  </p></li>
     *
     * </ul>
     *
     * In any case, if this method is to be reinvoked in the same encoding
     * operation then care should be taken to preserve any characters remaining
     * in the input buffer so that they are available to the next invocation.
     *
     * <p> The {@code endOfInput} parameter advises this method as to whether
     * the invoker can provide further input beyond that contained in the given
     * input buffer.  If there is a possibility of providing additional input
     * then the invoker should pass {@code false} for this parameter; if there
     * is no possibility of providing further input then the invoker should
     * pass {@code true}.  It is not erroneous, and in fact it is quite
     * common, to pass {@code false} in one invocation and later discover that
     * no further input was actually available.  It is critical, however, that
     * the final invocation of this method in a sequence of invocations always
     * pass {@code true} so that any remaining unencoded input will be treated
     * as being malformed.
     *
     * <p> This method works by invoking the {@link #encodeLoop encodeLoop}
     * method, interpreting its results, handling error conditions, and
     * reinvoking it as necessary.  </p>
     *
     * @param in         The input character buffer
     * @param out        The output byte buffer
     * @param endOfInput {@code true} if, and only if, the invoker can provide no
     *                   additional input characters beyond those in the given buffer
     *
     * @return A coder-result object describing the reason for termination
     *
     * @throws IllegalStateException If an encoding operation is already in progress and the previous
     *                               step was an invocation neither of the {@link #reset reset}
     *                               method, nor of this method with a value of {@code false} for
     *                               the {@code endOfInput} parameter, nor of this method with a
     *                               value of {@code true} for the {@code endOfInput} parameter
     *                               but a return value indicating an incomplete encoding operation
     * @throws CoderMalfunctionError If an invocation of the encodeLoop method threw
     *                               an unexpected exception
     */
    /*
     * 从给定的输入缓冲区中编码尽可能多的字符，将结果写入给定的输出缓冲区。
     * endOfInput=true表示当发生下溢时，输入立即结束，即不会再提供更多输入
     * endOfInput=false表示当发生下溢时，可能还会有后续的输入
     */
    public final CoderResult encode(CharBuffer in, ByteBuffer out, boolean endOfInput) {
        int newState = endOfInput ? ST_END : ST_CODING;
        
        if((state != ST_RESET) && (state != ST_CODING) && !(endOfInput && (state == ST_END))) {
            throwIllegalStateException(state, newState);
        }
        
        state = newState;
        
        for(; ; ) {
            
            CoderResult cr;
            try {
                // 将一个或多个char编码为一个或多个byte
                cr = encodeLoop(in, out);
            } catch(BufferUnderflowException x) {
                throw new CoderMalfunctionError(x);
            } catch(BufferOverflowException x) {
                throw new CoderMalfunctionError(x);
            }
            
            // 发生上溢，输出缓冲区已经满了
            if(cr.isOverflow()) {
                return cr;
            }
            
            // 发生下溢，输出缓冲区仍有空闲
            if(cr.isUnderflow()) {
                // 如果endOfInput=true，即要求输入立即结束，但是仍有未解析的字符时，那些未解析字符被视为非法输入
                if(endOfInput && in.hasRemaining()) {
                    cr = CoderResult.malformedForLength(in.remaining());
                    // Fall through to malformed-input case
                } else {
                    return cr;
                }
            }
            
            /* 发生了编码错误 */
            
            CodingErrorAction action = null;
            if(cr.isMalformed()) {
                action = malformedInputAction;
            } else if(cr.isUnmappable()) {
                action = unmappableCharacterAction;
            } else {    // 抛异常：AssertionError
                assert false : cr.toString();
            }
            
            // 停止编码
            if(action == CodingErrorAction.REPORT) {
                return cr;
            }
            
            // 替换错误的字符
            if(action == CodingErrorAction.REPLACE) {
                // 输出缓冲区的剩余空间已经不足以存放替换字符了
                if(out.remaining()<replacement.length) {
                    return CoderResult.OVERFLOW;
                }
                // 写入替换字符
                out.put(replacement);
            }
            
            // 跳过错误的字符
            if((action == CodingErrorAction.IGNORE) || (action == CodingErrorAction.REPLACE)) {
                // Skip erroneous input either way
                in.position(in.position() + cr.length());
                continue;
            }
            
            assert false;
        }
        
    }
    
    /**
     * Encodes one or more characters into one or more bytes.
     *
     * <p> This method encapsulates the basic encoding loop, encoding as many
     * characters as possible until it either runs out of input, runs out of room
     * in the output buffer, or encounters an encoding error.  This method is
     * invoked by the {@link #encode encode} method, which handles result
     * interpretation and error recovery.
     *
     * <p> The buffers are read from, and written to, starting at their current
     * positions.  At most {@link Buffer#remaining in.remaining()} characters
     * will be read, and at most {@link Buffer#remaining out.remaining()}
     * bytes will be written.  The buffers' positions will be advanced to
     * reflect the characters read and the bytes written, but their marks and
     * limits will not be modified.
     *
     * <p> This method returns a {@link CoderResult} object to describe its
     * reason for termination, in the same manner as the {@link #encode encode}
     * method.  Most implementations of this method will handle encoding errors
     * by returning an appropriate result object for interpretation by the
     * {@link #encode encode} method.  An optimized implementation may instead
     * examine the relevant error action and implement that action itself.
     *
     * <p> An implementation of this method may perform arbitrary lookahead by
     * returning {@link CoderResult#UNDERFLOW} until it receives sufficient
     * input.  </p>
     *
     * @param in  The input character buffer
     * @param out The output byte buffer
     *
     * @return A coder-result object describing the reason for termination
     */
    // 将一个或多个char编码为一个或多个byte
    protected abstract CoderResult encodeLoop(CharBuffer in, ByteBuffer out);
    
    /*▲ 编码 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether or not this encoder can encode the given character.
     *
     * <p> This method returns {@code false} if the given character is a
     * surrogate character; such characters can be interpreted only when they
     * are members of a pair consisting of a high surrogate followed by a low
     * surrogate.  The {@link #canEncode(java.lang.CharSequence)
     * canEncode(CharSequence)} method may be used to test whether or not a
     * character sequence can be encoded.
     *
     * <p> This method may modify this encoder's state; it should therefore not
     * be invoked if an <a href="#steps">encoding operation</a> is already in
     * progress.
     *
     * <p> The default implementation of this method is not very efficient; it
     * should generally be overridden to improve performance.  </p>
     *
     * @param c The given character
     *
     * @return {@code true} if, and only if, this encoder can encode
     * the given character
     *
     * @throws IllegalStateException If an encoding operation is already in progress
     */
    // 判断字符c能都被编码器正确编码
    public boolean canEncode(char c) {
        CharBuffer cb = CharBuffer.allocate(1);
        cb.put(c);
        cb.flip();
        return canEncode(cb);
    }
    
    /**
     * Tells whether or not this encoder can encode the given character
     * sequence.
     *
     * <p> If this method returns {@code false} for a particular character
     * sequence then more information about why the sequence cannot be encoded
     * may be obtained by performing a full <a href="#steps">encoding
     * operation</a>.
     *
     * <p> This method may modify this encoder's state; it should therefore not
     * be invoked if an encoding operation is already in progress.
     *
     * <p> The default implementation of this method is not very efficient; it
     * should generally be overridden to improve performance.  </p>
     *
     * @param cs The given character sequence
     *
     * @return {@code true} if, and only if, this encoder can encode
     * the given character without throwing any exceptions and without
     * performing any replacements
     *
     * @throws IllegalStateException If an encoding operation is already in progress
     */
    // 判断字符序列cs能否被编码器正确编码
    public boolean canEncode(CharSequence cs) {
        CharBuffer cb;
        if(cs instanceof CharBuffer)
            cb = ((CharBuffer) cs).duplicate();
        else
            cb = CharBuffer.wrap(cs.toString());
        return canEncode(cb);
    }
    
    /*
     * 检查字符序列是否能被正确编码。
     *
     * 该方法在一个临时的缓冲区内对输入的字符序列编码。
     * 这将引起编码器内部状态的改变，所以当编码器正在进行时不应调用这些方法。
     * 开始编码处理前，可以使用这些方法检测您的输入。
     */
    private boolean canEncode(CharBuffer cb) {
        if(state == ST_FLUSHED) {
            reset();
        } else if(state != ST_RESET) {
            throwIllegalStateException(state, ST_CODING);
        }
        CodingErrorAction ma = malformedInputAction();
        CodingErrorAction ua = unmappableCharacterAction();
        try {
            onMalformedInput(CodingErrorAction.REPORT);
            onUnmappableCharacter(CodingErrorAction.REPORT);
            encode(cb);
        } catch(CharacterCodingException x) {
            return false;
        } finally {
            onMalformedInput(ma);
            onUnmappableCharacter(ua);
            reset();
        }
        return true;
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns the charset that created this encoder.
     *
     * @return This encoder's charset
     */
    // 返回当前编码器对应的字符集
    public final Charset charset() {
        return charset;
    }
    
    /**
     * Returns the average number of bytes that will be produced for each
     * character of input.  This heuristic value may be used to estimate the size
     * of the output buffer required for a given input sequence.
     *
     * @return The average number of bytes produced
     * per character of input
     */
    // 返回编码每个给定的字符所需的平均字节数量
    public final float averageBytesPerChar() {
        return averageBytesPerChar;
    }
    
    /**
     * Returns the maximum number of bytes that will be produced for each
     * character of input.  This value may be used to compute the worst-case size
     * of the output buffer required for a given input sequence.
     *
     * @return The maximum number of bytes that will be produced per
     * character of input
     */
    // 返回编码每个给定的字符所需的最大字节数量
    public final float maxBytesPerChar() {
        return maxBytesPerChar;
    }
    
    
    
    /**
     * Returns this encoder's replacement value.
     *
     * @return This encoder's current replacement,
     * which is never {@code null} and is never empty
     */
    // 返回替换元素
    public final byte[] replacement() {
        return Arrays.copyOf(replacement, replacement.length);
    }
    
    /**
     * Changes this encoder's replacement value.
     *
     * <p> This method invokes the {@link #implReplaceWith implReplaceWith}
     * method, passing the new replacement, after checking that the new
     * replacement is acceptable.  </p>
     *
     * @param newReplacement The new replacement; must not be
     *                       {@code null}, must have non-zero length,
     *
     *
     *
     *
     *
     *                       must not be longer than the value returned by the
     *                       {@link #maxBytesPerChar() maxBytesPerChar} method, and
     *                       must be {@link #isLegalReplacement legal}
     *
     * @return This encoder
     *
     * @throws IllegalArgumentException If the preconditions on the parameter do not hold
     */
    // 设置替换元素
    public final CharsetEncoder replaceWith(byte[] newReplacement) {
        if(newReplacement == null) {
            throw new IllegalArgumentException("Null replacement");
        }
        int len = newReplacement.length;
        if(len == 0) {
            throw new IllegalArgumentException("Empty replacement");
        }
        if(len>maxBytesPerChar) {
            throw new IllegalArgumentException("Replacement too long");
        }
        
        
        if(!isLegalReplacement(newReplacement)) {
            throw new IllegalArgumentException("Illegal replacement");
        }
        
        this.replacement = Arrays.copyOf(newReplacement, newReplacement.length);
        
        implReplaceWith(this.replacement);
        
        return this;
    }
    
    /**
     * Tells whether or not the given byte array is a legal replacement value
     * for this encoder.
     *
     * <p> A replacement is legal if, and only if, it is a legal sequence of
     * bytes in this encoder's charset; that is, it must be possible to decode
     * the replacement into one or more sixteen-bit Unicode characters.
     *
     * <p> The default implementation of this method is not very efficient; it
     * should generally be overridden to improve performance.  </p>
     *
     * @param repl The byte array to be tested
     *
     * @return {@code true} if, and only if, the given byte array
     * is a legal replacement value for this encoder
     */
    // 判断给定的字节数组是否为此编码器的合法替换值
    public boolean isLegalReplacement(byte[] repl) {
        WeakReference<CharsetDecoder> wr = cachedDecoder;
        CharsetDecoder dec = null;
        if((wr == null) || ((dec = wr.get()) == null)) {
            dec = charset().newDecoder();
            dec.onMalformedInput(CodingErrorAction.REPORT);
            dec.onUnmappableCharacter(CodingErrorAction.REPORT);
            cachedDecoder = new WeakReference<CharsetDecoder>(dec);
        } else {
            dec.reset();
        }
        ByteBuffer bb = ByteBuffer.wrap(repl);
        CharBuffer cb = CharBuffer.allocate((int) (bb.remaining() * dec.maxCharsPerByte()));
        CoderResult cr = dec.decode(bb, cb, true);
        return !cr.isError();
    }
    
    
    
    /**
     * Returns this encoder's current action for malformed-input errors.
     *
     * @return The current malformed-input action, which is never {@code null}
     */
    public CodingErrorAction malformedInputAction() {
        return malformedInputAction;
    }
    
    /**
     * Returns this encoder's current action for unmappable-character errors.
     *
     * @return The current unmappable-character action, which is never
     * {@code null}
     */
    public CodingErrorAction unmappableCharacterAction() {
        return unmappableCharacterAction;
    }
    
    /**
     * Changes this encoder's action for malformed-input errors.
     *
     * <p> This method invokes the {@link #implOnMalformedInput
     * implOnMalformedInput} method, passing the new action.  </p>
     *
     * @param newAction The new action; must not be {@code null}
     *
     * @return This encoder
     *
     * @throws IllegalArgumentException If the precondition on the parameter does not hold
     */
    // 注册回调：发生Malformed错误时如何处理
    public final CharsetEncoder onMalformedInput(CodingErrorAction newAction) {
        if(newAction == null) {
            throw new IllegalArgumentException("Null action");
        }
        malformedInputAction = newAction;
        implOnMalformedInput(newAction);
        return this;
    }
    
    /**
     * Changes this encoder's action for unmappable-character errors.
     *
     * <p> This method invokes the {@link #implOnUnmappableCharacter
     * implOnUnmappableCharacter} method, passing the new action.  </p>
     *
     * @param newAction The new action; must not be {@code null}
     *
     * @return This encoder
     *
     * @throws IllegalArgumentException If the precondition on the parameter does not hold
     */
    // 注册回调：发生Unmappable错误时如何处理
    public final CharsetEncoder onUnmappableCharacter(CodingErrorAction newAction) {
        if(newAction == null) {
            throw new IllegalArgumentException("Null action");
        }
        unmappableCharacterAction = newAction;
        implOnUnmappableCharacter(newAction);
        return this;
    }
    
    
    
    /**
     * Flushes this encoder.
     *
     * <p> Some encoders maintain internal state and may need to write some
     * final bytes to the output buffer once the overall input sequence has
     * been read.
     *
     * <p> Any additional output is written to the output buffer beginning at
     * its current position.  At most {@link Buffer#remaining out.remaining()}
     * bytes will be written.  The buffer's position will be advanced
     * appropriately, but its mark and limit will not be modified.
     *
     * <p> If this method completes successfully then it returns {@link
     * CoderResult#UNDERFLOW}.  If there is insufficient room in the output
     * buffer then it returns {@link CoderResult#OVERFLOW}.  If this happens
     * then this method must be invoked again, with an output buffer that has
     * more room, in order to complete the current <a href="#steps">encoding
     * operation</a>.
     *
     * <p> If this encoder has already been flushed then invoking this method
     * has no effect.
     *
     * <p> This method invokes the {@link #implFlush implFlush} method to
     * perform the actual flushing operation.  </p>
     *
     * @param out The output byte buffer
     *
     * @return A coder-result object, either {@link CoderResult#UNDERFLOW} or
     * {@link CoderResult#OVERFLOW}
     *
     * @throws IllegalStateException If the previous step of the current encoding operation was an
     *                               invocation neither of the {@link #flush flush} method nor of
     *                               the three-argument {@link
     *                               #encode(CharBuffer, ByteBuffer, boolean) encode} method
     *                               with a value of {@code true} for the {@code endOfInput}
     *                               parameter
     */
    // 刷新输出缓冲区
    public final CoderResult flush(ByteBuffer out) {
        // 发生下溢时应该停止再编码字符
        if(state == ST_END) {
            CoderResult cr = implFlush(out);
            
            // 发生下溢，输出缓冲区仍有空闲
            if(cr.isUnderflow()) {
                // 改为已刷新状态
                state = ST_FLUSHED;
            }
            
            return cr;
        }
        
        if(state != ST_FLUSHED) {
            throwIllegalStateException(state, ST_FLUSHED);
        }
        
        return CoderResult.UNDERFLOW; // Already flushed
    }
    
    /**
     * Resets this encoder, clearing any internal state.
     *
     * <p> This method resets charset-independent state and also invokes the
     * {@link #implReset() implReset} method in order to perform any
     * charset-specific reset actions.  </p>
     *
     * @return This encoder
     */
    // 重置编码器状态
    public final CharsetEncoder reset() {
        implReset();
        state = ST_RESET;
        return this;
    }
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reports a change to this encoder's replacement value.
     *
     * <p> The default implementation of this method does nothing.  This method
     * should be overridden by encoders that require notification of changes to
     * the replacement.  </p>
     *
     * @param newReplacement The replacement value
     */
    protected void implReplaceWith(byte[] newReplacement) {
    }
    
    /**
     * Reports a change to this encoder's malformed-input action.
     *
     * <p> The default implementation of this method does nothing.  This method
     * should be overridden by encoders that require notification of changes to
     * the malformed-input action.  </p>
     *
     * @param newAction The new action
     */
    protected void implOnMalformedInput(CodingErrorAction newAction) {
    }
    
    /**
     * Reports a change to this encoder's unmappable-character action.
     *
     * <p> The default implementation of this method does nothing.  This method
     * should be overridden by encoders that require notification of changes to
     * the unmappable-character action.  </p>
     *
     * @param newAction The new action
     */
    protected void implOnUnmappableCharacter(CodingErrorAction newAction) {
    }
    
    /**
     * Flushes this encoder.
     *
     * <p> The default implementation of this method does nothing, and always
     * returns {@link CoderResult#UNDERFLOW}.  This method should be overridden
     * by encoders that may need to write final bytes to the output buffer
     * once the entire input sequence has been read. </p>
     *
     * @param out The output byte buffer
     *
     * @return A coder-result object, either {@link CoderResult#UNDERFLOW} or
     * {@link CoderResult#OVERFLOW}
     */
    protected CoderResult implFlush(ByteBuffer out) {
        return CoderResult.UNDERFLOW;
    }
    
    /**
     * Resets this encoder, clearing any charset-specific internal state.
     *
     * <p> The default implementation of this method does nothing.  This method
     * should be overridden by encoders that maintain internal state.  </p>
     */
    protected void implReset() {
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    private void throwIllegalStateException(int from, int to) {
        throw new IllegalStateException("Current state = " + stateNames[from] + ", new state = " + stateNames[to]);
    }
}
