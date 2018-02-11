/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A precomputed hash map.
 *
 * <p> Subclasses of this class are of the following form:
 *
 * <blockquote><pre>
 * class FooMap
 *     extends sun.util.PreHashedMap&lt;String&gt;
 * {
 *
 *     private FooMap() {
 *         super(ROWS, SIZE, SHIFT, MASK);
 *     }
 *
 *     protected void init(Object[] ht) {
 *         ht[0] = new Object[] { "key-1", value_1 };
 *         ht[1] = new Object[] { "key-2", value_2, new Object { "key-3", value_3 } };
 *         ...
 *     }
 *
 * }</pre></blockquote>
 *
 * <p> The {@code init} method is invoked by the {@code PreHashedMap}
 * constructor with an object array long enough for the map's rows.  The method
 * must construct the hash chain for each row and store it in the appropriate
 * element of the array.
 *
 * <p> Each entry in the map is represented by a unique hash-chain node.  The
 * final node of a hash chain is a two-element object array whose first element
 * is the entry's key and whose second element is the entry's value.  A
 * non-final node of a hash chain is a three-element object array whose first
 * two elements are the entry's key and value and whose third element is the
 * next node in the chain.
 *
 * <p> Instances of this class are mutable and are not safe for concurrent
 * access.  They may be made immutable and thread-safe via the appropriate
 * methods in the {@link java.util.Collections} utility class.
 *
 * <p> In the JDK build, subclasses of this class are typically created via the
 * {@code Hasher} program in the {@code make/tools/Hasher} directory.
 *
 * @author Mark Reinhold
 * @see java.util.AbstractMap
 * @since 1.5
 */
/*
 * 特制的Map工具类，专为标准字符集类StandardCharsets打造。
 *
 * 该Map中的数据按行排列，每一行可能包含多个键值对。
 * 同一行出现的键的哈希值是相同的，不在同一行的键的哈希值不同。
 *
 * 例如：
 * 1  key1-value1
 * 2  key2-value2, key3-value3, key4-value4
 * 3  key5-value6, key5-value6
 *
 * 这6个键值对数据可以存储为：
 * Object[] ht = new Object[4];
 * ht[1] = new Object{key1, value1};
 * ht[2] = new Object{key2, value2, new Object{key3, value3}, new Object{key4, value4}};
 * ht[2] = new Object{key5, value5, new Object{key6, value6}};
 */
public abstract class PreHashedMap<V> extends AbstractMap<String, V> {
    
    private final int rows; // 掩码
    private final int size; // 键值对数量
    private final int shift;
    private final int mask; // 映射到行数的掩码
    private final Object[] ht;  // 包含了很多行的键值对数据，数组中每个元素是一行，每行可能包含多个键值对
    
    /**
     * Creates a new map.
     *
     * <p> This constructor invokes the {@link #init init} method, passing it a
     * newly-constructed row array that is {@code rows} elements long.
     *
     * @param rows  The number of rows in the map
     * @param size  The number of entries in the map
     * @param shift The value by which hash codes are right-shifted
     * @param mask  The value with which hash codes are masked after being shifted
     */
    protected PreHashedMap(int rows, int size, int shift, int mask) {
        this.rows = rows;
        this.size = size;
        this.shift = shift;
        this.mask = mask;
        this.ht = new Object[rows];
        init(ht);
    }
    
    /**
     * Initializes this map.
     *
     * <p> This method must construct the map's hash chains and store them into
     * the appropriate elements of the given hash-table row array.
     *
     * @param ht The row array to be initialized
     */
    // 初始化Map，由子类实现
    protected abstract void init(Object[] ht);
    
    // 根据key返回value
    public V get(Object k) {
        // 先把key按照指定的规则哈希化
        int h = (k.hashCode() >> shift) & mask;
        
        // 根据键的哈希值返回整行数据
        Object[] a = (Object[]) ht[h];
        
        if(a == null)
            return null;
        
        for(; ; ) {
            // key匹配，则返回value
            if(a[0].equals(k))
                return toV(a[1]);
            
            if(a.length < 3)
                return null;
            
            // 尝试其他的可能
            a = (Object[]) a[2];
        }
    }
    
    /**
     * @throws UnsupportedOperationException If the given key is not part of this map's initial key set
     */
    // 设置键值对，并返回成功设置的value
    public V put(String k, V v) {
        // 先把key按照指定的规则哈希化
        int h = (k.hashCode() >> shift) & mask;
        Object[] a = (Object[]) ht[h];
        if(a == null)
            throw new UnsupportedOperationException(k);
        
        for(; ; ) {
            if(a[0].equals(k)) {
                V ov = toV(a[1]);
                a[1] = v;
                return ov;
            }
            
            if(a.length < 3)
                throw new UnsupportedOperationException(k);
            
            a = (Object[]) a[2];
        }
    }
    
    // 返回key的集合
    public Set<String> keySet() {
        return new AbstractSet<>() {
            
            public int size() {
                return size;
            }
            
            public Iterator<String> iterator() {
                return new Iterator<>() {
                    Object[] a = null;  // value
                    String cur = null;  // key
                    
                    private int i = -1;
                    
                    // 是否还有下一个key
                    public boolean hasNext() {
                        if(cur != null)
                            return true;
                        return findNext();
                    }
                    
                    // 反回下一个key
                    public String next() {
                        if(cur == null) {
                            if(!findNext())
                                throw new NoSuchElementException();
                        }
                        String s = cur;
                        cur = null;
                        return s;
                    }
                    
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
    
                    // 按行遍历查找key
                    private boolean findNext() {
                        if(a != null) {
                            // 如果同一行存在多个键值对
                            if(a.length == 3) {
                                a = (Object[]) a[2];
                                cur = (String) a[0];
                                return true;
                            }
                            // 否则到下一行遍历
                            i++;
                            a = null;
                        }
                        cur = null;
                        if(i >= rows)
                            return false;
                        if(i < 0 || ht[i] == null) {
                            do {
                                if(++i >= rows)
                                    return false;
                            } while(ht[i] == null);
                        }
                        a = (Object[]) ht[i];
                        cur = (String) a[0];
                        return true;
                    }
                    
                };
            }
        };
    }
    
    // 返回键值对实体的集合
    public Set<Entry<String, V>> entrySet() {
        return new AbstractSet<>() {
    
            public int size() {
                return size;
            }
    
            public Iterator<Entry<String, V>> iterator() {
                return new Iterator<>() {
                    final Iterator<String> i = keySet().iterator();
                    
                    // 有没有entry取决于有没有key
                    public boolean hasNext() {
                        return i.hasNext();
                    }
    
                    public Map.Entry<String, V> next() {
                        return new Map.Entry<>() {
                            String k = i.next();
    
                            public String getKey() {
                                return k;
                            }
    
                            public V getValue() {
                                return get(k);
                            }
    
                            public int hashCode() {
                                V v = get(k);
                                return (k.hashCode() + (v == null ? 0 : v.hashCode()));
                            }
    
                            public boolean equals(Object ob) {
                                if(ob == this)
                                    return true;
                                if(!(ob instanceof Map.Entry))
                                    return false;
                                Map.Entry<?, ?> that = (Map.Entry<?, ?>) ob;
                                return (this.getKey() == null ? that.getKey() == null : this.getKey().equals(that.getKey()))
                                    && (this.getValue() == null ? that.getValue() == null : this.getValue().equals(that.getValue()));
                            }
    
                            public V setValue(V v) {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
    
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
    
                };
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    private V toV(Object x) {
        return (V) x;
    }
    
}
