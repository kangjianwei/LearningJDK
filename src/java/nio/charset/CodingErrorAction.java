/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A typesafe enumeration for coding-error actions.
 *
 * <p> Instances of this class are used to specify how malformed-input and
 * unmappable-character errors are to be handled by charset <a
 * href="CharsetDecoder.html#cae">decoders</a> and <a
 * href="CharsetEncoder.html#cae">encoders</a>.  </p>
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

// 表示发生编码错误（遇到有缺陷的输入或无法映射字符）时该如何处理
public class CodingErrorAction {
    
    /**
     * Action indicating that a coding error is to be reported,
     * either by returning a {@link CoderResult} object or by throwing a {@link CharacterCodingException},
     * whichever is appropriate for the method implementing the coding process.
     */
    // 创建CharsetEncoder时的默认行为。这个行为表示发生错误时，停止编码/解码，并返回CoderResult向调用者报告此错误
    public static final CodingErrorAction REPORT = new CodingErrorAction("REPORT");
    
    /**
     * Action indicating that a coding error is to be handled by dropping the erroneous input and resuming the coding operation.
     */
    // 发生错误时，跳过错误的序列，并继续编码/解码
    public static final CodingErrorAction IGNORE = new CodingErrorAction("IGNORE");
    
    /**
     * Action indicating that a coding error is to be handled by dropping the erroneous input,
     * appending the coder's replacement value to the output buffer,
     * and resuming the coding operation.
     */
    // 发生错误时，用预置的替换序列代替错误的序列，并继续编码/解码
    public static final CodingErrorAction REPLACE = new CodingErrorAction("REPLACE");
    
    private String name;
    
    private CodingErrorAction(String name) {
        this.name = name;
    }
    
    /**
     * Returns a string describing this action.
     *
     * @return A descriptive string
     */
    public String toString() {
        return name;
    }
}
