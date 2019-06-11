/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * A Charset implementation for reading PropertyResourceBundle, in order
 * for loading properties files. This first tries to load the properties
 * file with UTF-8 encoding). If it fails, then load the file with ISO-8859-1
 */
// 属性资源字符集，用于解码属性文件，默认使用UTF-8去解码
public class PropertyResourceBundleCharset extends Charset {
    
    // 该字符集是否为UTF-8
    private boolean strictUTF8 = false;
    
    // 构造解码ResourceBundle的字符集
    public PropertyResourceBundleCharset(boolean strictUTF8) {
        this(PropertyResourceBundleCharset.class.getCanonicalName(), null);
        this.strictUTF8 = strictUTF8;
    }
    
    public PropertyResourceBundleCharset(String canonicalName, String[] aliases) {
        super(canonicalName, aliases);
    }
    
    @Override
    public boolean contains(Charset cs) {
        return false;
    }
    
    // 创建解码器
    @Override
    public CharsetDecoder newDecoder() {
        return new PropertiesFileDecoder(this, 1.0f, 1.0f);
    }
    
    // 不支持编码器（没必要）
    @Override
    public CharsetEncoder newEncoder() {
        throw new UnsupportedOperationException("Encoding is not supported");
    }
    
    // 属性资源解码器
    private final class PropertiesFileDecoder extends CharsetDecoder {
        // 先使用UTF-8解码器尝试解码
        private CharsetDecoder cdUTF_8 = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        
        private CharsetDecoder cdISO_8859_1 = null;
        
        protected PropertiesFileDecoder(Charset cs, float averageCharsPerByte, float maxCharsPerByte) {
            super(cs, averageCharsPerByte, maxCharsPerByte);
        }
        
        // 将一个或多个byte解码为一个或多个char
        protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
            // 使用的字符集预设为ISO_8859_1
            if (Objects.nonNull(cdISO_8859_1)) {
                return cdISO_8859_1.decode(in, out, false);
            }
            
            /* 设置了其他解码器，则尝试使用UTF-8解码器解码 */
            
            in.mark();
            out.mark();
            
            // 使用UTF_8解码器解码字节流
            CoderResult cr = cdUTF_8.decode(in, out, false);
            
            if (cr.isUnderflow() || cr.isOverflow() || PropertyResourceBundleCharset.this.strictUTF8) {
                return cr;
            }
            
            // Invalid or unmappable UTF-8 sequence detected.
            // Switching to the ISO 8859-1 decorder.
            assert cr.isMalformed() || cr.isUnmappable();
            
            in.reset();
            out.reset();
            
            cdISO_8859_1 = StandardCharsets.ISO_8859_1.newDecoder();
            
            return cdISO_8859_1.decode(in, out, false);
        }
    }
}
