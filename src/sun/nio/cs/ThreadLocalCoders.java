/*
 * Copyright (c) 2001, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * Utility class for caching per-thread decoders and encoders.
 */

// 工厂类，利用ThreadLocal为当前线程缓存编码器和解码器
public class ThreadLocalCoders {
    
    private static final int CACHE_SIZE = 3;
    
    private static Cache encoderCache = new Cache(CACHE_SIZE) {
        // 创建编码器
        Object create(Object name) {
            if(name instanceof String) {
                return Charset.forName((String) name).newEncoder();
            }
            
            if(name instanceof Charset) {
                return ((Charset) name).newEncoder();
            }
            assert false;
            return null;
        }
        
        boolean hasName(Object ob, Object name) {
            if(name instanceof String) {
                return (((CharsetEncoder) ob).charset().name().equals(name));
            }
            if(name instanceof Charset) {
                return ((CharsetEncoder) ob).charset().equals(name);
            }
            return false;
        }
    };
    
    private static Cache decoderCache = new Cache(CACHE_SIZE) {
        // 创建解码器
        Object create(Object name) {
            if(name instanceof String) {
                return Charset.forName((String) name).newDecoder();
            }
            
            if(name instanceof Charset) {
                return ((Charset) name).newDecoder();
            }
            assert false;
            return null;
        }
        
        // 指定的字符集name是否存在（ob是已经存在的字符集）
        boolean hasName(Object ob, Object name) {
            if(name instanceof String) {
                return (((CharsetDecoder) ob).charset().name().equals(name));
            }
            if(name instanceof Charset) {
                return ((CharsetDecoder) ob).charset().equals(name);
            }
            return false;
        }
    };
    
    // 返回编码器（先从缓冲中查找）
    public static CharsetEncoder encoderFor(Object name) {
        CharsetEncoder ce = (CharsetEncoder) encoderCache.forName(name);
        ce.reset();
        return ce;
    }
    
    // 返回解码器（先从缓冲中查找）
    public static CharsetDecoder decoderFor(Object name) {
        CharsetDecoder cd = (CharsetDecoder) decoderCache.forName(name);
        cd.reset();
        return cd;
    }
    
    // 编码/解码器缓存
    private abstract static class Cache {
        
        private final int size; // 缓存大小
        
        // Thread-local reference to array of cached objects, in LRU order
        private ThreadLocal<Object[]> cache = new ThreadLocal<>();
        
        Cache(int size) {
            this.size = size;
        }
        
        // 创建编码/解码器，name可能是字符集名，也可能是字符集实例
        abstract Object create(Object name);
        
        // 指定的字符集name是否存在（ob是已经存在的字符集）
        abstract boolean hasName(Object ob, Object name);
        
        Object forName(Object name) {
            Object[] oa = cache.get();
            if(oa == null) {    // 没有缓存
                oa = new Object[size];
                cache.set(oa);
            } else {    // 存在缓存，则在缓存中查找
                for(int i = 0; i<oa.length; i++) {
                    Object ob = oa[i];
                    if(ob == null)
                        continue;
                    if(hasName(ob, name)) {
                        if(i>0) {
                            // 如果查找的字符集存在，则将其挪到缓存的最前面
                            moveToFront(oa, i);
                        }
                        return ob;
                    }
                }
            }
            
            // 如果缓存中没有查找的字符集，则需要新建
            Object ob = create(name);
            oa[oa.length - 1] = ob;
            moveToFront(oa, oa.length - 1);
            return ob;
        }
        
        // 将oa[i]移动到oa[0]处
        private void moveToFront(Object[] oa, int i) {
            Object ob = oa[i];
            System.arraycopy(oa, 0, oa, 1, i);
            oa[0] = ob;
        }
    }
}
