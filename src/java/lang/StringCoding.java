/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;
import sun.nio.cs.ArrayDecoder;
import sun.nio.cs.ArrayEncoder;
import sun.nio.cs.HistoricallyNamedCharset;

import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;

import static java.lang.Character.highSurrogate;
import static java.lang.Character.isSupplementaryCodePoint;
import static java.lang.Character.isSurrogate;
import static java.lang.Character.lowSurrogate;
import static java.lang.String.COMPACT_STRINGS;
import static java.lang.String.LATIN1;
import static java.lang.String.UTF16;
import static java.lang.StringUTF16.putChar;

// 用于解码[decoding]/编码[encoding]字符串的工具类
class StringCoding {
    // 每个线程缓存的解码器
    private static final ThreadLocal<SoftReference<StringDecoder>> decoder = new ThreadLocal<>();
    
    // 每个线程缓存的编码器
    private static final ThreadLocal<SoftReference<StringEncoder>> encoder = new ThreadLocal<>();
    
    /* The cached Result for each thread */
    // 为当前线程指定缓存的Result对象（此刻还没缓存进去呢）
    private static final ThreadLocal<Result> resultCached = new ThreadLocal<>() {
        protected Result initialValue() {
            return new Result();
        }
    };
    
    // 缓存的常用字符集实例
    private static final Charset ISO_8859_1 = sun.nio.cs.ISO_8859_1.INSTANCE;
    private static final Charset US_ASCII = sun.nio.cs.US_ASCII.INSTANCE;
    private static final Charset UTF_8 = sun.nio.cs.UTF_8.INSTANCE;
    
    private static char repl = '\ufffd';
    
    private StringCoding() {
    }
    
    // 判断数组元素是否全部为ANSI字符，即字符范围在[0x0, 0x80)
    @HotSpotIntrinsicCandidate
    public static boolean hasNegatives(byte[] ba, int off, int len) {
        for(int i = off; i < off + len; i++) {
            // byte的非负范围是[0, 80)，此时可表示ANSI字符
            if(ba[i] < 0) {
                return true;
            }
        }
        return false;
    }
    
    
    
    /*▼ encode ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 编码String，需要先将String内部的byte[]转为char[]，然后以JVM默认字符集格式对char[]进行编码，并返回编码后的byte[]
    static byte[] encode(byte coder, byte[] val) {
        // JVM默认字符集
        Charset cs = Charset.defaultCharset();
        
        // UTF_8可表示的字符范围：整个Unicode字符集
        if(cs == UTF_8) {
            // 编码String，返回UTF-8格式的byte[]。如发生编码错误，替换错误的码元为单字节'?'
            return encodeUTF8(coder, val, true);
        }
        
        // ISO_8859_1可表示的字符范围[0x0, 0xFF)
        if(cs == ISO_8859_1) {
            // 编码String，返回ISO-8859-1格式的byte[]。如发生编码错误，替换错误的单元为单字节'?'
            return encode8859_1(coder, val);
        }
        
        // US_ASCII可表示的字符范围[0x0, 0x80)
        if(cs == US_ASCII) {
            // 编码String，返回ASCII格式的byte[]。如发生编码错误，替换错误的单元为单字节'?'
            return encodeASCII(coder, val);
        }
        
        // 取出当前线程关联的字符串编码器缓存，看与JVM默认字符集是否匹配
        StringEncoder se = deref(encoder);
        
        if(se == null   // 无缓存的编码器，或者：
            || !cs.name().equals(se.cs.name())) {   // JVM默认字符集与缓存的编码器支持的字符集不同
            se = new StringEncoder(cs, cs.name());  // 新建一个支持JVM默认字符集的编码器
            
            // 将字符串编码器放入缓存，并关联到当前线程
            set(encoder, se);
        }
        
        // 返回编码后的byte[]
        return se.encode(coder, val);
    }
    
    // 编码String，返回charsetName字符集格式的byte[]
    static byte[] encode(String charsetName, byte coder, byte[] val) throws UnsupportedEncodingException {
        // 取出当前线程关联的字符串编码器缓存
        StringEncoder se = deref(encoder);
        String csn = (charsetName == null) ? "ISO-8859-1" : charsetName;
        if((se == null) || !(csn.equals(se.requestedCharsetName()) || csn.equals(se.charsetName()))) {
            se = null;
            try {
                Charset cs = lookupCharset(csn);
                if(cs != null) {
                    if(cs == UTF_8) {
                        // 编码String，返回UTF-8格式的byte[]。如发生编码错误，替换错误的码元为单字节'?'
                        return encodeUTF8(coder, val, true);
                    }
                    if(cs == ISO_8859_1) {
                        // 编码String，返回ISO-8859-1格式的byte[]。如发生编码错误，替换错误的单元为单字节'?'
                        return encode8859_1(coder, val);
                    }
                    if(cs == US_ASCII) {
                        // 编码String，返回ASCII格式的byte[]。如发生编码错误，替换错误的单元为单字节'?'
                        return encodeASCII(coder, val);
                    }
                    se = new StringEncoder(cs, csn);
                }
            } catch(IllegalCharsetNameException x) {
            }
            
            if(se == null) {
                throw new UnsupportedEncodingException(csn);
            }
            
            // 将字符串编码器放入缓存，并关联到当前线程
            set(encoder, se);
        }
        
        // 返回编码后的byte[]
        return se.encode(coder, val);
    }
    
    // 编码String，返回cs字符集格式的byte[]
    static byte[] encode(Charset cs, byte coder, byte[] val) {
        if(cs == UTF_8) {
            // 编码String，返回UTF-8格式的byte[]。如发生编码错误，替换错误的码元为单字节'?'
            return encodeUTF8(coder, val, true);
        }
        if(cs == ISO_8859_1) {
            // 编码String，返回ISO-8859-1格式的byte[]。如发生编码错误，替换错误的单元为单字节'?'
            return encode8859_1(coder, val);
        }
        if(cs == US_ASCII) {
            // 编码String，返回ASCII格式的byte[]。如发生编码错误，替换错误的单元为单字节'?'
            return encodeASCII(coder, val);
        }
        
        /* 编码String，返回其他编码格式的byte[]。*/
        
        CharsetEncoder ce = cs.newEncoder();
        // fastpath for ascii compatible
        if(coder == LATIN1
            && ((ce instanceof ArrayEncoder)
            && ((ArrayEncoder) ce).isASCIICompatible()
            && !hasNegatives(val, 0, val.length))) {    // 判断数组元素是否全部为ANSI字符，即字符范围在[0x0, 0x80)
            return Arrays.copyOf(val, val.length);
        }
        int len = val.length >> coder;  // assume LATIN1=0/UTF16=1;
        int en = scale(len, ce.maxBytesPerChar());
        byte[] ba = new byte[en];
        if(len == 0) {
            return ba;
        }
        ce.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).reset();
        if(ce instanceof ArrayEncoder) {
            int blen = (coder == LATIN1) ? ((ArrayEncoder) ce).encodeFromLatin1(val, 0, len, ba) : ((ArrayEncoder) ce).encodeFromUTF16(val, 0, len, ba);
            if(blen != -1) {
                return safeTrim(ba, blen, true);
            }
        }
        boolean isTrusted = cs.getClass().getClassLoader0() == null || System.getSecurityManager() == null;
        char[] ca = (coder == LATIN1) ? StringLatin1.toChars(val) : StringUTF16.toChars(val);
        ByteBuffer bb = ByteBuffer.wrap(ba);
        CharBuffer cb = CharBuffer.wrap(ca, 0, len);
        try {
            CoderResult cr = ce.encode(cb, bb, true);
            if(!cr.isUnderflow())
                cr.throwException();
            cr = ce.flush(bb);
            if(!cr.isUnderflow())
                cr.throwException();
        } catch(CharacterCodingException x) {
            throw new Error(x);
        }
        return safeTrim(ba, bb.position(), isTrusted);
    }
    
    /**
     * @param coder     String类别，分为LATIN1-String和UTF16-String，参见String中的注释
     * @param val       存储String的字节数组，在Windows上显示为LATIN1编码或UTF-16LE编码
     * @param doReplace 当转码发生错误时，错误的码元是否接受使用'?'去替换，如果不接受，则抛出异常
     *
     * @return 返回UTF-8编码格式的String。
     */
    // 编码String，返回UTF-8格式的byte[]
    private static byte[] encodeUTF8(byte coder, byte[] val, boolean doReplace) {
        if(coder == UTF16) {
            // 编码UTF16-string，返回UTF-8格式的byte[]。
            return encodeUTF8_UTF16(val, doReplace);
        }
        
        /* 否则，需要编码LATIN1-String，返回UTF-8格式的byte[]*/
        
        // 判断数组元素是否全部为ANSI字符，即字符范围在[0x0, 0x80)
        if(!hasNegatives(val, 0, val.length)) {
            // 返回val的一份拷贝（都是单字节）
            return Arrays.copyOf(val, val.length);
        }
        
        // 字符范围在[0x80, 0xFF)
        int dp = 0;
        byte[] dst = new byte[val.length << 1];
        for(byte c : val) {
            // 处理[0x80, 0x800)范围内的字符，存储为两个字节：110x-xxxx|10xx-xxxx
            if(c < 0) {
                dst[dp++] = (byte) (0xc0 | ((c & 0xff) >> 6));
                dst[dp++] = (byte) (0x80 | (c & 0x3f));
            } else {    // 处理[0x0, 0x80)范围内的字符，存储为一个字节：0xxx-xxxx
                dst[dp++] = c;
            }
        }
        
        // 分配的空间恰好用完
        if(dp == dst.length) {
            return dst;
        }
        
        // 后面有多余的空间，则将其去掉
        return Arrays.copyOf(dst, dp);
    }
    
    /**
     * @param val       存储了UTF16-string的字节数组，在Windows上显示为UTF-16LE编码
     * @param doReplace 当转码发生错误时，错误的码元是否接受使用'?'去替换
     *
     * @return 返回UTF-8格式的byte[]。
     */
    // 编码UTF16-string，返回UTF-8格式的byte[]
    private static byte[] encodeUTF8_UTF16(byte[] val, boolean doReplace) {
        int dp = 0; // dst游标，统计转换后的符号所占的字节数
        int sp = 0; // src游标，遍历原字节数组val中的符号
        
        // 原UTF16-string中，最多可能容纳的符号个数（按char计算）。
        int sl = val.length >> 1;
        
        /*
         * 创建新数组用来容纳转换后的字节序列
         * 这里处理的是UTF16-string，即其符号由UTF16形式的两个字节或四个字节组成
         * 所以这里按最大容量去开辟一个字节数组用来存储转码后的符号
         *
         * 该最大容量确定的依据是：
         * UTF-16两字节符号转换成UTF-8后，可能为1字节、2字节、3字节符号
         * UTF-16四字节符号转换成UTF-8后，还是4字节符号
         */
        byte[] dst = new byte[sl * 3];
        
        char c;
        
        /*
         * 快速解析字符串前面编码范围在[0x0, 0x80)之间的符号（其实就是ASCII码）
         * 经过压缩（如0x0035--->0x35）后，存入UTF-8编码表示的一个byte
         */
        while(sp < sl && (c = StringUTF16.getChar(val, sp)) < '\u0080') {
            // ascii fast loop;
            dst[dp++] = (byte) c;
            sp++;
        }
        
        // 从第一个非ASCII码char开始遍历，当然，后面还可能遇到ASCII码char
        while(sp < sl) {
            c = StringUTF16.getChar(val, sp++);
            
            /*
             * (1).对于[0x    0, 0x    80)，有效位数为 0~ 7位，存储为一个字节：0xxx-xxxx
             * (2).对于[0x   80, 0x   800)，有效位数为 8~11位，存储为两个字节：110x-xxxx|10xx-xxxx
             * (3).对于[0x  800, 0x  FFFF)，有效位数为12~16位，存储为三个字节：1110-xxxx|10xx-xxxx|10xx-xxxx
             * (4).对于[0x10000, 0x10FFFF)，有效位数为17~21位，存储为四个字节：1111-0xxx|10xx-xxxx|10xx-xxxx|10xx-xxxx
             */
            if(c < 0x80) {  // (1)
                dst[dp++] = (byte) c;
            } else if(c < 0x800) {  // (2)
                dst[dp++] = (byte) (0xc0 | (c >> 6));   // 先存高6位
                dst[dp++] = (byte) (0x80 | (c & 0x3f)); // 再存低6位
            } else if(Character.isSurrogate(c)) {   // (4) 处于Unicode代理字符区域
                int uc = -1;
                char c2;
                if(Character.isHighSurrogate(c) && sp < sl && Character.isLowSurrogate(c2 = StringUTF16.getChar(val, sp))) {
                    // 高、低代理区的码点值 ---> Unicode符号编码值
                    uc = Character.toCodePoint(c, c2);
                }
                // 出现了异常的码元，即单独出现了高代理单元或低代理单元
                if(uc < 0) {
                    if(doReplace) {
                        // 如果接受替换，则将异常的码元（两个byte）替换为一个单字节'?'
                        dst[dp++] = '?';
                    } else {
                        // 如果不接受替换，则抛出异常
                        throwUnmappable(sp - 1, 1); // or 2, does not matter here
                    }
                } else {
                    dst[dp++] = (byte) (0xf0 | ((uc >> 18)));
                    dst[dp++] = (byte) (0x80 | ((uc >> 12) & 0x3f));
                    dst[dp++] = (byte) (0x80 | ((uc >> 6) & 0x3f));
                    dst[dp++] = (byte) (0x80 | (uc & 0x3f));
                    sp++;  // 2 chars
                }
            } else {    // (3) 3 bytes, 16 bits
                dst[dp++] = (byte) (0xe0 | ((c >> 12)));
                dst[dp++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                dst[dp++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        
        // 新数组刚好填充完
        if(dp == dst.length) {
            return dst;
        }
        
        // 新数组空间没用完，则压缩空间（去掉后面没有使用的部分）
        return Arrays.copyOf(dst, dp);
    }
    
    // 编码String，返回ISO-8859-1格式的byte[]。如发生编码错误，替换错误的单元为单字节'?'
    private static byte[] encode8859_1(byte coder, byte[] val) {
        return encode8859_1(coder, val, true);
    }
    
    /**
     * @param coder     String类别，分为LATIN1-String和UTF16-String，参见String中的注释
     * @param val       存储String的字节数组，在Windows上显示为UTF-16LE编码
     * @param doReplace 当转码发生错误时，错误的单元是否接受使用'?'去替换，如果不接受，则抛出异常
     * @return          返回ISO-8859-1格式的byte[]。
     */
    // 编码String，返回以ISO-8859-1格式的byte[]。如发生编码错误，替换错误的单元为单字节'?'
    private static byte[] encode8859_1(byte coder, byte[] val, boolean doReplace) {
        // 编码LATIN1-String，返回ISO-8859-1格式的byte[]。
        if(coder == LATIN1) {
            // 两种格式完全一样，直接返回副本即可
            return Arrays.copyOf(val, val.length);
        }
        
        /* 否则，编码UTF16-String，返回ISO-8859-1格式的byte[]。 */
        
        // dst数组剩余容量，初始化为原UTF16-string中，最多可能容纳的符号个数（按char计算）。
        int len = val.length >> 1;
        
        // 创建新数组用来容纳转换后的字节序列
        byte[] dst = new byte[len];
        
        int dp = 0; // dst游标，统计转换后的符号所占的字节数
        int sp = 0; // src游标，遍历原字节数组val中的符号，也代表已经编码完成的符号数量
        
        // 总共需要编码的符号数量，初始化为原UTF16-string中，最多可能容纳的符号个数
        int sl = len;
        
        while(sp < sl) {
            // 编码UTF16-String，返回成功处理的ISO-8859-1符号数量。
            int ret = implEncodeISOArray(val, sp, dst, dp, len);
            
            sp = sp + ret;
            dp = dp + ret;
            
            // 编码过程中遇到了超出ISO-8859-1表示范围的符号
            if(ret != len) {
                // 如果不允许替换这些异常byte，则抛出异常
                if(!doReplace) {
                    throwUnmappable(sp, 1);
                }
                
                // 将UTF16-String内部的字节转换为char后返回
                char c = StringUTF16.getChar(val, sp++);
                // 如果遇到了Unicode增补字符号
                if(Character.isHighSurrogate(c) && sp < sl && Character.isLowSurrogate(StringUTF16.getChar(val, sp))) {
                    sp++;
                }
                // 替换当前超出ISO_8859_1表示范围的单元（两个byte或四个byte）为一个单字节'?'
                dst[dp++] = '?';
                
                // 更新dst数组剩余容量
                len = sl - sp;
            }
        }
        
        // 刚好填满新数组
        if(dp == dst.length) {
            return dst;
        }
        
        // 去掉新数组后面多余的空间
        return Arrays.copyOf(dst, dp);
    }
    
    /**
     * @param coder String类别，分为LATIN1-String和UTF16-String，参见String中的注释
     * @param val   存储String的字节数组，在Windows上显示为UTF-16LE编码
     * @return      返回ASCII格式的byte[]
     */
    // 编码String，返回以ASCII格式的byte[]。如发生编码错误，替换错误的单元为单字节'?'
    private static byte[] encodeASCII(byte coder, byte[] val) {
        // 编码LATIN1-String，返回ASCII格式的byte[]。
        if(coder == LATIN1) {
            byte[] dst = new byte[val.length];
            for(int i = 0; i < val.length; i++) {
                if(val[i] < 0) {
                    // 替换当前超出ASCII表示范围的单个字节为一个单字节'?'
                    dst[i] = '?';
                } else {
                    dst[i] = val[i];
                }
            }
            return dst;
        }
        
        /* 否则，编码UTF16-String，返回ASCII格式的byte[]。 */
        
        // dst数组剩余容量，初始化为原UTF16-string中，最多可能容纳的符号个数（按char计算）。
        int len = val.length >> 1;
        
        // 创建新数组用来容纳转换后的字节序列
        byte[] dst = new byte[len];
        
        // dst游标，统计转换后的符号所占的字节数
        int dp = 0;
        
        for(int i = 0; i < len; i++) {
            // 将UTF16-String内部的字节转换为char后返回
            char c = StringUTF16.getChar(val, i);
            if(c < 0x80) {
                dst[dp++] = (byte) c;
                continue;
            }
            // 编码过程中遇到了超出ASCII表示范围的byte
            if(Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(StringUTF16.getChar(val, i + 1))) {
                i++;
            }
            dst[dp++] = '?';    // 替换当前超出ISO_8859_1表示范围的单元（两个byte或四个byte）为一个单字节'?'
        }
        
        if(len == dp) {
            return dst;
        }
        
        return Arrays.copyOf(dst, dp);
    }
    
    /**
     * 编码UTF16-String，编码过程中，如遇到超出表示范围的byte，则停止转换
     *
     * @param sa  转换前的字节序列
     * @param sp  遍历sa的游标
     * @param da  转换后的字节序列
     * @param dp  遍历dp的游标
     * @param len 待转换的字节数量
     *
     * @return    返回成功处理的ISO-8859-1符号数量。
     */
    // 编码UTF16-String，返回成功处理的ISO-8859-1符号数量。
    @HotSpotIntrinsicCandidate
    private static int implEncodeISOArray(byte[] sa, int sp, byte[] da, int dp, int len) {
        int i = 0;
        for(; i < len; i++) {
            // 将UTF16-String内部的字节转换为char后返回
            char c = StringUTF16.getChar(sa, sp++);
            // 如果该char的编码超出了ISO-8859-1编码可表示的形式，则结束解码
            if(c > '\u00FF')
                break;
            da[dp++] = (byte) c;
        }
        return i;
    }
    
    /**
     * Throws iae, instead of replacing, if unmappable.
     */
    // 编码String，返回UTF-8格式的byte[]。如发生编码错误，抛出异常。
    static byte[] getBytesUTF8NoRepl(String s) {
        return encodeUTF8(s.coder(), s.value(), false);
    }
    
    /**
     * Throws CCE, instead of replacing, if unmappable.
     */
    // 编码String，返回指定字符集格式的byte[]
    static byte[] getBytesNoRepl(String s, Charset cs) throws CharacterCodingException {
        try {
            return getBytesNoRepl1(s, cs);
        } catch(IllegalArgumentException e) {
            //getBytesNoRepl1 throws IAE with UnmappableCharacterException or CCE as the cause
            Throwable cause = e.getCause();
            if(cause instanceof UnmappableCharacterException) {
                throw (UnmappableCharacterException) cause;
            }
            throw (CharacterCodingException) cause;
        }
    }
    
    // 编码String，返回指定字符集格式的byte[]
    static byte[] getBytesNoRepl1(String s, Charset cs) {
        byte[] val = s.value();
        byte coder = s.coder();
        
        // 返回UTF-8格式的byte[]
        if(cs == UTF_8) {
            if(isASCII(val)) {
                return val;
            }
            return encodeUTF8(coder, val, false);
        }
        
        // 返回ISO_8859_1格式的byte[]
        if(cs == ISO_8859_1) {
            if(coder == LATIN1) {
                return val;
            }
            return encode8859_1(coder, val, false);
        }
        
        // 返回US_ASCII格式的byte[]
        if(cs == US_ASCII) {
            if(coder == LATIN1) {
                if(isASCII(val)) {
                    return val;
                } else {
                    throwUnmappable(val);
                }
            }
        }
        
        CharsetEncoder ce = cs.newEncoder();
        // fastpath for ascii compatible
        if(coder == LATIN1 && (((ce instanceof ArrayEncoder) && ((ArrayEncoder) ce).isASCIICompatible() && isASCII(val)))) {
            return val;
        }
        
        int len = val.length >> coder;  // assume LATIN1=0/UTF16=1;
        int en = scale(len, ce.maxBytesPerChar());
        byte[] ba = new byte[en];
        if(len == 0) {
            return ba;
        }
        if(ce instanceof ArrayEncoder) {
            int blen = (coder == LATIN1) ? ((ArrayEncoder) ce).encodeFromLatin1(val, 0, len, ba) : ((ArrayEncoder) ce).encodeFromUTF16(val, 0, len, ba);
            if(blen != -1) {
                return safeTrim(ba, blen, true);
            }
        }
        boolean isTrusted = cs.getClass().getClassLoader0() == null || System.getSecurityManager() == null;
        char[] ca = (coder == LATIN1) ? StringLatin1.toChars(val) : StringUTF16.toChars(val);
        ByteBuffer bb = ByteBuffer.wrap(ba);
        CharBuffer cb = CharBuffer.wrap(ca, 0, len);
        try {
            CoderResult cr = ce.encode(cb, bb, true);
            if(!cr.isUnderflow())
                cr.throwException();
            cr = ce.flush(bb);
            if(!cr.isUnderflow())
                cr.throwException();
        } catch(CharacterCodingException x) {
            throw new IllegalArgumentException(x);
        }
        
        return safeTrim(ba, bb.position(), isTrusted);
    }
    
    /*▲ encode ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ decode ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 以JVM默认字符集格式解码byte[]，返回结果集
    static Result decode(byte[] ba, int off, int len) {
        Charset cs = Charset.defaultCharset();
        
        if(cs == UTF_8) {
            // 以UTF-8格式解码byte[]，返回结果集
            return decodeUTF8(ba, off, len, true);
        }
        if(cs == ISO_8859_1) {
            // 以Latin1格式解码byte[]，返回结果集
            return decodeLatin1(ba, off, len);
        }
        if(cs == US_ASCII) {
            // 以ASCII格式解码byte[]，返回结果集
            return decodeASCII(ba, off, len);
        }
        
        /* 解码其他字符集格式的byte[]，返回结果集 */
        
        // 取出当前线程关联的字符串解码器缓存
        StringDecoder sd = deref(decoder);
        if(sd == null || !cs.name().equals(sd.cs.name())) {
            sd = new StringDecoder(cs, cs.name());
            // 将字符串解码器放入缓存，并关联到当前线程
            set(decoder, sd);
        }
        
        // 返回解码后的结果集
        return sd.decode(ba, off, len);
    }
    
    // 以charsetName格式解析byte[]，返回结果集
    static Result decode(String charsetName, byte[] ba, int off, int len) throws UnsupportedEncodingException {
        // 取出当前线程关联的字符串解码器缓存
        StringDecoder sd = deref(decoder);
        
        // 默认按"ISO-8859-1"格式解析byte[]
        String csn = (charsetName == null) ? "ISO-8859-1" : charsetName;
        
        if((sd == null) // 没有缓存的解码器，或者：
            || !(csn.equals(sd.requestedCharsetName()) || csn.equals(sd.charsetName()))) {  // 缓存的解码器不支持设定的字符集
            
            // 丢掉缓存中的解码器
            sd = null;
            try {
                // 在系统中查找相应的字符集
                Charset cs = lookupCharset(csn);
                if(cs != null) {
                    if(cs == UTF_8) {
                        return decodeUTF8(ba, off, len, true);
                    }
                    if(cs == ISO_8859_1) {
                        return decodeLatin1(ba, off, len);
                    }
                    if(cs == US_ASCII) {
                        return decodeASCII(ba, off, len);
                    }
                    sd = new StringDecoder(cs, csn);
                }
            } catch(IllegalCharsetNameException x) {
            }
            if(sd == null)
                throw new UnsupportedEncodingException(csn);
            
            // 将字符串解码器放入缓存，并关联到当前线程
            set(decoder, sd);
        }
        
        // 返回解码后的结果集
        return sd.decode(ba, off, len);
    }
    
    // 以cs格式解码byte[]，返回结果集
    static Result decode(Charset cs, byte[] ba, int off, int len) {
        if(cs == UTF_8) {
            return decodeUTF8(ba, off, len, true);
        }
        if(cs == ISO_8859_1) {
            return decodeLatin1(ba, off, len);
        }
        if(cs == US_ASCII) {
            return decodeASCII(ba, off, len);
        }
        
        /*
         * (1) We never cache the "external" cs, the only benefit of creating an additional StringDe/Encoder object to wrap it is to share the de/encode() method.
         *     These SD/E objects are short-lived, the young-gen gc should be able to take care of them well.
         *     But the best approach is still not to generate them if not really necessary.
         * (2) The defensive copy of the input byte/char[] has a big performance impact, as well as the outgoing result byte/char[].
         *     Need to do the optimization check of (sm==null && classLoader0==null) for both.
         * (3) There might be a timing gap in isTrusted setting. getClassLoader0() is only checked (and then isTrusted gets set) when (SM==null).
         *     It is possible that the SM==null for now but then SM is NOT null later when safeTrim() is invoked...
         *     the "safe" way to do is to redundant check (... && (isTrusted || SM == null || getClassLoader0())) in trim but it then can be argued that the SM is null
         *     when the operation is started...
         */
        CharsetDecoder cd = cs.newDecoder();
        // ascii fastpath
        if((cd instanceof ArrayDecoder)
            && ((ArrayDecoder) cd).isASCIICompatible()
            && !hasNegatives(ba, off, len)) {   // 判断数组元素是否全部为ANSI字符，即字符范围在[0x0, 0x80)
            return decodeLatin1(ba, off, len);
        }
        int en = scale(len, cd.maxCharsPerByte());
        if(len == 0) {
            return new Result().with();
        }
        cd.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).reset();
        char[] ca = new char[en];
        if(cd instanceof ArrayDecoder) {
            int clen = ((ArrayDecoder) cd).decode(ba, off, len, ca);
            return new Result().with(ca, 0, clen);
        }
        if(cs.getClass().getClassLoader0() != null && System.getSecurityManager() != null) {
            ba = Arrays.copyOfRange(ba, off, off + len);
            off = 0;
        }
        ByteBuffer bb = ByteBuffer.wrap(ba, off, len);
        CharBuffer cb = CharBuffer.wrap(ca);
        try {
            CoderResult cr = cd.decode(bb, cb, true);
            if(!cr.isUnderflow())
                cr.throwException();
            cr = cd.flush(cb);
            if(!cr.isUnderflow())
                cr.throwException();
        } catch(CharacterCodingException x) {
            // Substitution is always enabled,
            // so this shouldn't happen
            throw new Error(x);
        }
        return new Result().with(ca, 0, cb.position());
    }
    
    // 以ASCII格式解码byte[]，返回结果集
    private static Result decodeASCII(byte[] ba, int off, int len) {
        Result result = resultCached.get();
        if(COMPACT_STRINGS && !hasNegatives(ba, off, len)) {    // 判断数组元素是否全部为ANSI字符，即字符范围在[0x0, 0x80)
            return result.with(Arrays.copyOfRange(ba, off, off + len), LATIN1);
        }
        byte[] dst = new byte[len << 1];
        int dp = 0;
        while(dp < len) {
            int b = ba[off++];
            putChar(dst, dp++, (b >= 0) ? (char) b : repl);
        }
        return result.with(dst, UTF16);
    }
    
    // 以Latin1格式解码byte[]，返回结果集
    private static Result decodeLatin1(byte[] ba, int off, int len) {
        Result result = resultCached.get();
        if(COMPACT_STRINGS) {
            return result.with(Arrays.copyOfRange(ba, off, off + len), LATIN1);
        } else {
            // 从LATIN-String内部的字节转为UTF16-String内部的字节后返回
            return result.with(StringLatin1.inflate(ba, off, len), UTF16);
        }
    }
    
    // 以UTF-8格式解码byte[]，返回结果集
    private static Result decodeUTF8(byte[] src, int sp, int len, boolean doReplace) {
        // ascii-bais, which has a relative impact to the non-ascii-only bytes
        if(COMPACT_STRINGS && !hasNegatives(src, sp, len))  // 判断数组元素是否全部为ANSI字符，即字符范围在[0x0, 0x80)
            return resultCached.get().with(Arrays.copyOfRange(src, sp, sp + len), LATIN1);
        return decodeUTF8_0(src, sp, len, doReplace);
    }
    
    // 以UTF-8格式解码byte[]，返回结果集
    private static Result decodeUTF8_0(byte[] src, int sp, int len, boolean doReplace) {
        Result ret = resultCached.get();
        
        int sl = sp + len;
        int dp = 0;
        byte[] dst = new byte[len];
        
        if(COMPACT_STRINGS) {
            while(sp < sl) {
                int b1 = src[sp];
                if(b1 >= 0) {
                    dst[dp++] = (byte) b1;
                    sp++;
                    continue;
                }
                if((b1 == (byte) 0xc2 || b1 == (byte) 0xc3) && sp + 1 < sl) {
                    int b2 = src[sp + 1];
                    if(!isNotContinuation(b2)) {
                        dst[dp++] = (byte) (((b1 << 6) ^ b2) ^ (((byte) 0xC0 << 6) ^ ((byte) 0x80 << 0)));
                        sp += 2;
                        continue;
                    }
                }
                // anything not a latin1, including the repl we have to go with the utf16
                break;
            }
            if(sp == sl) {
                if(dp != dst.length) {
                    dst = Arrays.copyOf(dst, dp);
                }
                return ret.with(dst, LATIN1);
            }
        }
        if(dp == 0) {
            dst = new byte[len << 1];
        } else {
            byte[] buf = new byte[len << 1];
            StringLatin1.inflate(dst, 0, buf, 0, dp);
            dst = buf;
        }
        while(sp < sl) {
            int b1 = src[sp++];
            if(b1 >= 0) {
                putChar(dst, dp++, (char) b1);
            } else if((b1 >> 5) == -2 && (b1 & 0x1e) != 0) {
                if(sp < sl) {
                    int b2 = src[sp++];
                    if(isNotContinuation(b2)) {
                        if(!doReplace) {
                            throwMalformed(sp - 1, 1);
                        }
                        putChar(dst, dp++, repl);
                        sp--;
                    } else {
                        putChar(dst, dp++, (char) (((b1 << 6) ^ b2) ^ (((byte) 0xC0 << 6) ^ ((byte) 0x80 << 0))));
                    }
                    continue;
                }
                if(!doReplace) {
                    throwMalformed(sp, 1);  // underflow()
                }
                putChar(dst, dp++, repl);
                break;
            } else if((b1 >> 4) == -2) {
                if(sp + 1 < sl) {
                    int b2 = src[sp++];
                    int b3 = src[sp++];
                    if(isMalformed3(b1, b2, b3)) {
                        if(!doReplace) {
                            throwMalformed(sp - 3, 3);
                        }
                        putChar(dst, dp++, repl);
                        sp -= 3;
                        sp += malformedN(src, sp, 3);
                    } else {
                        char c = (char) ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ (((byte) 0xE0 << 12) ^ ((byte) 0x80 << 6) ^ ((byte) 0x80 << 0))));
                        if(isSurrogate(c)) {
                            if(!doReplace) {
                                throwMalformed(sp - 3, 3);
                            }
                            putChar(dst, dp++, repl);
                        } else {
                            putChar(dst, dp++, c);
                        }
                    }
                    continue;
                }
                if(sp < sl && isMalformed3_2(b1, src[sp])) {
                    if(!doReplace) {
                        throwMalformed(sp - 1, 2);
                    }
                    putChar(dst, dp++, repl);
                    continue;
                }
                if(!doReplace) {
                    throwMalformed(sp, 1);
                }
                putChar(dst, dp++, repl);
                break;
            } else if((b1 >> 3) == -2) {
                if(sp + 2 < sl) {
                    int b2 = src[sp++];
                    int b3 = src[sp++];
                    int b4 = src[sp++];
                    int uc = ((b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ (b4 ^ (((byte) 0xF0 << 18) ^ ((byte) 0x80 << 12) ^ ((byte) 0x80 << 6) ^ ((byte) 0x80 << 0))));
                    if(isMalformed4(b2, b3, b4) || !isSupplementaryCodePoint(uc)) { // shortest form check
                        if(!doReplace) {
                            throwMalformed(sp - 4, 4);
                        }
                        putChar(dst, dp++, repl);
                        sp -= 4;
                        sp += malformedN(src, sp, 4);
                    } else {
                        putChar(dst, dp++, highSurrogate(uc));  // 返回高代理处的码元（char）
                        putChar(dst, dp++, lowSurrogate(uc));   // 返回低代理处的码元（char）
                    }
                    continue;
                }
                b1 &= 0xff;
                if(b1 > 0xf4 || sp < sl && isMalformed4_2(b1, src[sp] & 0xff)) {
                    if(!doReplace) {
                        throwMalformed(sp - 1, 1);  // or 2
                    }
                    putChar(dst, dp++, repl);
                    continue;
                }
                if(!doReplace) {
                    throwMalformed(sp - 1, 1);
                }
                sp++;
                putChar(dst, dp++, repl);
                if(sp < sl && isMalformed4_3(src[sp])) {
                    continue;
                }
                break;
            } else {
                if(!doReplace) {
                    throwMalformed(sp - 1, 1);
                }
                putChar(dst, dp++, repl);
            }
        }
        if(dp != len) {
            dst = Arrays.copyOf(dst, dp << 1);
        }
        return ret.with(dst, UTF16);
    }
    
    /**
     * Throws iae, instead of replacing, if malformed or unmappable.
     */
    // 以UTF-8格式解码byte[]，进而构造String
    static String newStringUTF8NoRepl(byte[] src, int off, int len) {
        if(COMPACT_STRINGS && !hasNegatives(src, off, len)) // 判断数组元素是否全部为ANSI字符，即字符范围在[0x0, 0x80)
            return new String(Arrays.copyOfRange(src, off, off + len), LATIN1);
        Result ret = decodeUTF8_0(src, off, len, false);
        return new String(ret.value, ret.coder);
    }
    
    // 以cs格式解码src，进而构造String
    static String newStringNoRepl(byte[] src, Charset cs) throws CharacterCodingException {
        try {
            return newStringNoRepl1(src, cs);
        } catch(IllegalArgumentException e) {
            //newStringNoRepl1 throws IAE with MalformedInputException or CCE as the cause
            Throwable cause = e.getCause();
            if(cause instanceof MalformedInputException) {
                throw (MalformedInputException) cause;
            }
            throw (CharacterCodingException) cause;
        }
    }
    
    // 以cs格式解码src，进而构造String
    static String newStringNoRepl1(byte[] src, Charset cs) {
        if(cs == UTF_8) {
            if(COMPACT_STRINGS && isASCII(src))
                return new String(src, LATIN1);
            Result ret = decodeUTF8_0(src, 0, src.length, false);
            return new String(ret.value, ret.coder);
        }
        if(cs == ISO_8859_1) {
            return newStringLatin1(src);
        }
        if(cs == US_ASCII) {
            if(isASCII(src)) {
                return newStringLatin1(src);
            } else {
                throwMalformed(src);
            }
        }
        
        CharsetDecoder cd = cs.newDecoder();
        // ascii fastpath
        if((cd instanceof ArrayDecoder) && ((ArrayDecoder) cd).isASCIICompatible() && isASCII(src)) {
            return newStringLatin1(src);
        }
        int len = src.length;
        if(len == 0) {
            return "";
        }
        int en = scale(len, cd.maxCharsPerByte());
        char[] ca = new char[en];
        if(cs.getClass().getClassLoader0() != null && System.getSecurityManager() != null) {
            src = Arrays.copyOf(src, len);
        }
        ByteBuffer bb = ByteBuffer.wrap(src);
        CharBuffer cb = CharBuffer.wrap(ca);
        try {
            CoderResult cr = cd.decode(bb, cb, true);
            if(!cr.isUnderflow())
                cr.throwException();
            cr = cd.flush(cb);
            if(!cr.isUnderflow())
                cr.throwException();
        } catch(CharacterCodingException x) {
            throw new IllegalArgumentException(x);  // todo
        }
        Result ret = resultCached.get().with(ca, 0, cb.position());
        return new String(ret.value, ret.coder);
    }
    
    // 返回包装了byte[]的String
    private static String newStringLatin1(byte[] src) {
        if(COMPACT_STRINGS)
            return new String(src, LATIN1);
        // 从LATIN-String内部的字节转为UTF16-String内部的字节后，包装到UTF16-String中返回
        return new String(StringLatin1.inflate(src, 0, src.length), UTF16);
    }
    
    /*▲ decode ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 编码器/解码器缓存 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 取出当前线程关联的字符串解码/编码器缓存
    private static <T> T deref(ThreadLocal<SoftReference<T>> tl) {
        SoftReference<T> sr = tl.get();
        if(sr == null)
            return null;
        return sr.get();
    }
    
    // 将字符串解码器/编码器放入缓存，并关联到当前线程
    private static <T> void set(ThreadLocal<SoftReference<T>> tl, T ob) {
        tl.set(new SoftReference<>(ob));
    }
    
    /*▲ 编码器/解码器缓存 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * 在系统中查找指定名称的字符集实例
     * @param csn 字符集名称，可以是别名
     * @return    返回查找结果
     */
    // 返回从系统中查找到的字符集
    private static Charset lookupCharset(String csn) {
        // 返回查找到的字符集，查找过程中会将找到的字符集缓存到Charset内时一级缓存
        if(Charset.isSupported(csn)) {
            try {
                // 返回查找到的字符集
                return Charset.forName(csn);
            } catch(UnsupportedCharsetException x) {
                throw new Error(x);
            }
        }
        return null;
    }
    
    // 将给定的字节数组修剪为给定的长度
    private static byte[] safeTrim(byte[] ba, int len, boolean isTrusted) {
        if(len == ba.length && (isTrusted || System.getSecurityManager() == null))
            return ba;
        else
            return Arrays.copyOf(ba, len);
    }
    
    // 计算在指定字符集下，解码/编码原字符串最多需要多少字节
    private static int scale(int len, float expansionFactor) {
        // We need to perform double, not float, arithmetic; otherwise
        // we lose low order bits when len is larger than 2**24.
        return (int) (len * (double) expansionFactor);
    }
    
    /**
     * Print a message directly to stderr, bypassing all character conversion methods.
     *
     * @param msg message to print
     */
    // 直接向标准错误流打印消息，绕过所有字符转换方法。
    private static native void err(String msg);
    
    // 判断数组元素是否全部为ANSI字符，即字符范围在[0x0, 0x80)
    private static boolean isASCII(byte[] src) {
        return !hasNegatives(src, 0, src.length);
    }
    
    // for nb == 3/4
    private static int malformedN(byte[] src, int sp, int nb) {
        if(nb == 3) {
            int b1 = src[sp++];
            int b2 = src[sp++];    // no need to lookup b3
            return ((b1 == (byte) 0xe0 && (b2 & 0xe0) == 0x80) || isNotContinuation(b2)) ? 1 : 2;
        } else if(nb == 4) { // we don't care the speed here
            int b1 = src[sp++] & 0xff;
            int b2 = src[sp++] & 0xff;
            if(b1 > 0xf4 || (b1 == 0xf0 && (b2 < 0x90 || b2 > 0xbf)) || (b1 == 0xf4 && (b2 & 0xf0) != 0x80) || isNotContinuation(b2))
                return 1;
            if(isNotContinuation(src[sp++]))
                return 2;
            return 3;
        }
        assert false;
        return -1;
    }
    
    private static boolean isNotContinuation(int b) {
        return (b & 0xc0) != 0x80;
    }
    
    private static boolean isMalformed3(int b1, int b2, int b3) {
        return (b1 == (byte) 0xe0 && (b2 & 0xe0) == 0x80) || (b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80;
    }
    
    private static boolean isMalformed3_2(int b1, int b2) {
        return (b1 == (byte) 0xe0 && (b2 & 0xe0) == 0x80) || (b2 & 0xc0) != 0x80;
    }
    
    private static boolean isMalformed4(int b2, int b3, int b4) {
        return (b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80 || (b4 & 0xc0) != 0x80;
    }
    
    private static boolean isMalformed4_2(int b1, int b2) {
        return (b1 == 0xf0 && (b2 < 0x90 || b2 > 0xbf)) || (b1 == 0xf4 && (b2 & 0xf0) != 0x80) || (b2 & 0xc0) != 0x80;
    }
    
    private static boolean isMalformed4_3(int b3) {
        return (b3 & 0xc0) != 0x80;
    }
    
    
    
    /*▼ 越界检查 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static void throwMalformed(int off, int nb) {
        String msg = "malformed input off : " + off + ", length : " + nb;
        throw new IllegalArgumentException(msg, new MalformedInputException(nb));
    }
    
    private static void throwMalformed(byte[] val) {
        int dp = 0;
        while(dp < val.length && val[dp] >= 0) {
            dp++;
        }
        throwMalformed(dp, 1);
    }
    
    private static void throwUnmappable(int off, int nb) {
        String msg = "malformed input off : " + off + ", length : " + nb;
        throw new IllegalArgumentException(msg, new UnmappableCharacterException(nb));
    }
    
    private static void throwUnmappable(byte[] val) {
        int dp = 0;
        while(dp < val.length && val[dp] >= 0) {
            dp++;
        }
        throwUnmappable(dp, 1);
    }
    
    /*▲ 越界检查 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 存储用于构造String的字节信息和编码信息
    static class Result {
        byte[] value;
        byte coder;
        
        Result with() {
            coder = COMPACT_STRINGS ? LATIN1 : UTF16;
            value = new byte[0];
            return this;
        }
        
        Result with(byte[] val, byte coder) {
            this.coder = coder;
            value = val;
            return this;
        }
        
        // 将val转换为适用于当前环境的byte[]存入Result以用作构造String
        Result with(char[] val, int off, int len) {
            if(String.COMPACT_STRINGS) {
                // 将UTF16-String内部的字节转换为LATIN1-String内部的字节
                byte[] bs = StringUTF16.compress(val, off, len);
                if(bs != null) {
                    value = bs;
                    coder = LATIN1;
                    return this;
                }
            }
            coder = UTF16;
            // 将val中的char批量转换为UTF16-String内部的字节，并返回
            value = StringUTF16.toBytes(val, off, len);
            return this;
        }
        
    }
    
    // 字符串解码器，内部调用根据不同的字符集信息调用相应的字符解码器
    static class StringDecoder {
        protected final Result result;
        private final String requestedCharsetName;  // 解析byte[]时要求使用的编码格式
        private final Charset cs;                   // requestedCharsetName对应的编码集
        private final CharsetDecoder cd;            // cs对应的字节解码器，字节进来，字符出去
        private final boolean isASCIICompatible;    // 是否向前兼容ASCII字符集
        
        StringDecoder(Charset cs, String rcn) {
            this.requestedCharsetName = rcn;
            this.cs = cs;
            this.cd = cs.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            this.result = new Result();
            this.isASCIICompatible = (cd instanceof ArrayDecoder) && ((ArrayDecoder) cd).isASCIICompatible();
        }
        
        String charsetName() {
            if(cs instanceof HistoricallyNamedCharset)
                return ((HistoricallyNamedCharset) cs).historicalName();
            return cs.name();
        }
        
        final String requestedCharsetName() {
            return requestedCharsetName;
        }
        
        // 使用字节解码器cd解码ba，然后把解码后的char[]转为byte[]存入结果集Result
        Result decode(byte[] ba, int off, int len) {
            if(len == 0) {
                return result.with();
            }
            
            // fastpath for ascii compatible
            if(isASCIICompatible && !hasNegatives(ba, off, len)) {// 判断数组元素是否全部为ANSI字符，即字符范围在[0x0, 0x80)
                if(COMPACT_STRINGS) {
                    return result.with(Arrays.copyOfRange(ba, off, off + len), LATIN1);
                } else {
                    return result.with(StringLatin1.inflate(ba, off, len), UTF16);
                }
            }
            
            int en = scale(len, cd.maxCharsPerByte());
            char[] ca = new char[en];
            if(cd instanceof ArrayDecoder) {
                int clen = ((ArrayDecoder) cd).decode(ba, off, len, ca);
                return result.with(ca, 0, clen);
            }
            cd.reset();
            ByteBuffer bb = ByteBuffer.wrap(ba, off, len);
            CharBuffer cb = CharBuffer.wrap(ca);
            try {
                CoderResult cr = cd.decode(bb, cb, true);
                if(!cr.isUnderflow())
                    cr.throwException();
                cr = cd.flush(cb);
                if(!cr.isUnderflow())
                    cr.throwException();
            } catch(CharacterCodingException x) {
                // Substitution is always enabled,
                // so this shouldn't happen
                throw new Error(x);
            }
            
            // 解码后的ca转为byte[]存入result以构造String
            return result.with(ca, 0, cb.position());
        }
    }
    
    // 字符串编码器，内部调用根据不同的字符集信息调用相应的字符编码器
    private static class StringEncoder {
        private final String requestedCharsetName;  // 解析byte[]时要求使用的编码格式
        private Charset cs;                         // requestedCharsetName对应的编码集
        private CharsetEncoder ce;                  // cs对应的字符编码器，字符进来，字节出去
        private final boolean isASCIICompatible;    // 是否向前兼容ASCII字符集
        private final boolean isTrusted;            // 不属于StandardCharsets提供的字符集不被信任
        
        private StringEncoder(Charset cs, String rcn) {
            this.requestedCharsetName = rcn;
            this.cs = cs;
            this.ce = cs.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            this.isASCIICompatible = (ce instanceof ArrayEncoder) && ((ArrayEncoder) ce).isASCIICompatible();
            /*
             * JVM加载类时依次用到四种类加载器（四个阶段）：
             *   1. Bootstrap class loader，该加载器用C++实现，它是虚拟机的内置类加载器，通常表示为null，并且没有父级。
             *      这个加载器，java程序无法引用到，所以此时会有：getClassLoader0() == null
             *   2. Platform class loader，Platform类加载器可以看到所有Platform类，它们可以用作ClassLoader实例的父级。
             *      Platform类包括Java SE Platform API和它们的实现类，以及由Platform类加载器或其祖先定义的特定于JDK的运行时类。
             *      注：Java 9之前使用的是Extension Loader
             *   3. System class loader，也被称为Application class loader，这与Platform加载器不同。
             *      System加载器通常用于在应用程序类路径，模块路径和JDK特定工具上定义类。
             *      Platform类加载器是System类加载器的父级或祖先，所有Platform类都是可见的。
             *   4. 用户自定义加载器，自己定义从哪里加载类的二进制流。
             *
             * 加载标准字符集发生在第一个阶段，此时getClassLoader0() == null成立，即字符集被信任
             * 非标准字符集在其他阶段加载，此时getClassLoader0() != null，即字符集不被信任
             */
            this.isTrusted = (cs.getClass().getClassLoader0() == null);
        }
        
        String charsetName() {
            if(cs instanceof HistoricallyNamedCharset)
                return ((HistoricallyNamedCharset) cs).historicalName();
            return cs.name();
        }
        
        final String requestedCharsetName() {
            return requestedCharsetName;
        }
        
        // 使用字符编码器ce将String编码成相应编码格式的byte[]后返回
        byte[] encode(byte coder, byte[] val) {
            // 快速处理兼容ascii码的LATIN1-String
            if(coder == LATIN1
                && isASCIICompatible
                && !hasNegatives(val, 0, val.length)) {// 判断数组元素是否全部为ANSI字符，即字符范围在[0x0, 0x80)
                return Arrays.copyOf(val, val.length);
            }
            
            // 初始化为原String中，最多可容纳的符号个数（按char计算）。
            int len = val.length >> coder;  // 预设 LATIN1=0/UTF16=1;
            
            // ba数组最大容量。初始化为该编码下，存放原字符串需要多少字节。（需要先计算该编码中，一个char需要占用几个byte）
            int en = scale(len, ce.maxBytesPerChar());
            
            // 创建新数组用来容纳转换后的字节序列
            byte[] ba = new byte[en];
            
            if(len == 0) {
                return ba;
            }
            
            if(ce instanceof ArrayEncoder) {
                int blen = (coder == LATIN1)
                    ? ((ArrayEncoder) ce).encodeFromLatin1(val, 0, len, ba)
                    : ((ArrayEncoder) ce).encodeFromUTF16(val, 0, len, ba);
                
                if(blen != -1) {
                    return safeTrim(ba, blen, isTrusted);
                }
            }
            
            char[] ca = (coder == LATIN1) ? StringLatin1.toChars(val) : StringUTF16.toChars(val);
            ce.reset();
            ByteBuffer bb = ByteBuffer.wrap(ba);
            CharBuffer cb = CharBuffer.wrap(ca, 0, len);
            try {
                CoderResult cr = ce.encode(cb, bb, true);
                if(!cr.isUnderflow())
                    cr.throwException();
                cr = ce.flush(bb);
                if(!cr.isUnderflow())
                    cr.throwException();
            } catch(CharacterCodingException x) {
                // Substitution is always enabled, so this shouldn't happen
                throw new Error(x);
            }
            return safeTrim(ba, bb.position(), isTrusted);
        }
    }
}
