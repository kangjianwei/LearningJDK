/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs.ext;

import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Abstract base class for charset providers.
 *
 * @author Mark Reinhold
 */
// 扩展的"字符集"提供商的抽象基类
public class AbstractCharsetProvider extends CharsetProvider {
    
    /* Maps canonical names to class names */
    // [字符集规范名--字符集类名]映射
    private Map<String, String> classMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    
    /* Maps alias names to canonical names */
    // [字符集别名--字符集规范名]映射
    private Map<String, String> aliasMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    
    /* Maps canonical names to alias-name arrays */
    // [字符集规范名--字符集别名数组]映射
    private Map<String, String[]> aliasNameMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    
    /* Maps canonical names to soft references that hold cached instances */
    // [字符集规范名--字符集实例]映射
    private Map<String, SoftReference<Charset>> cache = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    
    private String packagePrefix;
    
    protected AbstractCharsetProvider() {
        packagePrefix = "sun.nio.cs";
    }
    
    protected AbstractCharsetProvider(String pkgPrefixName) {
        packagePrefix = pkgPrefixName;
    }
    
    /* Add an entry to the given map, but only if no mapping yet exists for the given name. */
    // 向指定的map中添加键值对
    private static <K, V> void put(Map<K, V> m, K name, V value) {
        if(!m.containsKey(name)) {
            m.put(name, value);
        }
    }
    
    // 从指定的map中移除键值对
    private static <K, V> void remove(Map<K, V> m, K name) {
        V x = m.remove(name);
        assert (x != null);
    }
    
    // 根据字符集名称查找字符集实例
    public final Charset charsetForName(String charsetName) {
        synchronized(this) {
            init();
            // 先尝试将给定的名称转为规范名
            return lookup(canonicalize(charsetName));
        }
    }
    
    // 返回用于遍历"字符集"的迭代器
    public final Iterator<Charset> charsets() {
        final ArrayList<String> ks;
        
        synchronized(this) {
            init();
            ks = new ArrayList<>(classMap.keySet());
        }
        
        return new Iterator<Charset>() {
            Iterator<String> i = ks.iterator();
            
            public boolean hasNext() {
                return i.hasNext();
            }
            
            public Charset next() {
                String csn = i.next();
                synchronized(AbstractCharsetProvider.this) {
                    return lookup(csn);
                }
            }
            
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    // 根据规范名，返回别名集合
    public final String[] aliases(String charsetName) {
        synchronized(this) {
            init();
            return aliasNameMap.get(charsetName);
        }
    }
    
    /* Declare support for the given charset */
    // 统一设置字符集相关的各种映射
    protected void charset(String name, String className, String[] aliases) {
        synchronized(this) {
            put(classMap, name, className);
            for(int i = 0; i < aliases.length; i++)
                put(aliasMap, aliases[i], name);
            put(aliasNameMap, name, aliases);
            cache.clear();
        }
    }
    
    // 从字符集相关的映射中移除给定的字符集信息
    protected void deleteCharset(String name, String[] aliases) {
        synchronized(this) {
            remove(classMap, name);
            for(int i = 0; i < aliases.length; i++)
                remove(aliasMap, aliases[i]);
            remove(aliasNameMap, name);
            cache.clear();
        }
    }
    
    // 给定的字符集名称是否存在
    protected boolean hasCharset(String name) {
        synchronized(this) {
            return classMap.containsKey(name);
        }
    }
    
    /* Late initialization hook, needed by some providers */
    // 需要子类完成字符集相关映射的初始化过程
    protected void init() {
    }
    
    // 从别名系统中查看给定的字符集名称是否存在，如果存在，返回其规范名
    private String canonicalize(String charsetName) {
        String acn = aliasMap.get(charsetName);
        return (acn != null) ? acn : charsetName;
    }
    
    private Charset lookup(String csn) {
        // 先检查缓存：[字符集规范名--字符集实例]映射
        SoftReference<Charset> sr = cache.get(csn);
        if(sr != null) {
            Charset cs = sr.get();
            if(cs != null)
                return cs;
        }
        
        // 在[字符集规范名--字符集类名]映射中进一步检查是否支持该字符集
        String cln = classMap.get(csn);
        
        if(cln == null)
            return null;
        
        // 实例化该字符集，并缓存它
        try {
            Class<?> c = Class.forName(packagePrefix + "." + cln, true, this.getClass().getClassLoader());
            
            @SuppressWarnings("deprecation")
            Charset cs = (Charset) c.newInstance();
            cache.put(csn, new SoftReference<Charset>(cs));
            return cs;
        } catch(ClassNotFoundException x) {
            return null;
        } catch(IllegalAccessException x) {
            return null;
        } catch(InstantiationException x) {
            return null;
        }
    }
}
