/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.util.zip;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class for zipfile name and comment decoding and encoding
 */
// zip编/解码器
class ZipCoder {
    // 访问java.lang包下特定方法的后门
    private static final jdk.internal.misc.JavaLangAccess JLA = jdk.internal.misc.SharedSecrets.getJavaLangAccess();
    
    // UTF_8.ArrayEn/Decoder is stateless, so make it singleton.
    private static ZipCoder utf8 = new UTF8(UTF_8); // 默认使用UTF8字符集
    
    private Charset cs;         // 当前编/解码器使用的字符集
    private CharsetDecoder dec; // 字节解码器。字节进来，字符出去，完成对字节序列的解码操作
    private CharsetEncoder enc; // 字符编码器。字符进来，字节出去，完成对字符序列的编码操作
    
    // 构造指定字符集的Zip编/解码器
    private ZipCoder(Charset cs) {
        this.cs = cs;
    }
    
    // 返回指定字符集的Zip编/解码器
    public static ZipCoder get(Charset charset) {
        if(charset == UTF_8) {
            return utf8;
        }
        
        return new ZipCoder(charset);
    }
    
    // 返回zip编码器
    protected CharsetEncoder encoder() {
        if(enc == null) {
            enc = cs.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        }
        return enc;
    }
    
    // 返回zip解码器
    protected CharsetDecoder decoder() {
        if(dec == null) {
            dec = cs.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        }
        return dec;
    }
    
    /* assume invoked only if "this" is not utf8 */
    // 编码：将字符串中的字符编码为UTF8格式的字节后返回
    byte[] getBytesUTF8(String s) {
        return utf8.getBytes(s);
    }
    
    // 解码：将ba中前len个字节依据UTF8格式解码为字符串后返回
    String toStringUTF8(byte[] ba, int len) {
        return utf8.toString(ba, 0, len);
    }
    
    // 解码：将ba中off处起的len个字节依据UTF8格式解码为字符串后返回
    String toStringUTF8(byte[] ba, int off, int len) {
        return utf8.toString(ba, off, len);
    }
    
    // 编码：将字符串中的字符编码为字节后返回
    byte[] getBytes(String s) {
        try {
            // 将字符串s包装到buffer
            CharBuffer buffer = CharBuffer.wrap(s);
            // 返回zip编码器
            CharsetEncoder encoder = encoder();
            // 编码字符序列，将编码结果写入到字节缓冲区返回
            ByteBuffer bb = encoder.encode(buffer);
            int pos = bb.position();
            int limit = bb.limit();
            if(bb.hasArray() && pos == 0 && limit == bb.capacity()) {
                return bb.array();
            }
            byte[] bytes = new byte[bb.limit() - bb.position()];
            bb.get(bytes);
            return bytes;
        } catch(CharacterCodingException x) {
            throw new IllegalArgumentException(x);
        }
    }
    
    // 解码：将ba中所有字节解码为字符串后返回
    String toString(byte[] ba) {
        return toString(ba, 0, ba.length);
    }
    
    // 解码：将ba中前len个字节解码为字符串后返回
    String toString(byte[] ba, int len) {
        return toString(ba, 0, len);
    }
    
    // 解码：将ba中off处起的len个字节解码为字符串后返回
    String toString(byte[] ba, int off, int len) {
        try {
            // 包装一个字节数组到buffer（包装一部分）
            ByteBuffer buffer = ByteBuffer.wrap(ba, off, len);
            // 获取zip解码器
            CharsetDecoder decoder = decoder();
            // 解码字节序列，将解码结果写入到字符缓冲区返回
            CharBuffer charBuffer = decoder.decode(buffer);
            // 将当前字符序列转为字符串返回
            return charBuffer.toString();
        } catch(CharacterCodingException x) {
            throw new IllegalArgumentException(x);
        }
    }
    
    // 当前编/解码器是否为UTF8格式，默认为false
    boolean isUTF8() {
        return false;
    }
    
    // UTF8编码格式的编/解码器
    static final class UTF8 extends ZipCoder {
        
        UTF8(Charset utf8) {
            super(utf8);
        }
        
        @Override
        boolean isUTF8() {
            return true;
        }
        
        // 编码：编码String，返回UTF-8格式的byte[]
        @Override
        byte[] getBytes(String s) {
            return JLA.getBytesUTF8NoRepl(s);
        }
        
        // 解码：以UTF-8格式解码byte[]，进而构造String
        @Override
        String toString(byte[] ba, int off, int length) {
            return JLA.newStringUTF8NoRepl(ba, off, length);
        }
    }
}
