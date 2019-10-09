/*
 * Copyright (c) 1994, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.io.IOException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This class implements a hash table, which maps keys to values. Any
 * non-{@code null} object can be used as a key or as a value. <p>
 *
 * To successfully store and retrieve objects from a hashtable, the
 * objects used as keys must implement the {@code hashCode}
 * method and the {@code equals} method. <p>
 *
 * An instance of {@code Hashtable} has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of <i>buckets</i> in the hash table, and the
 * <i>initial capacity</i> is simply the capacity at the time the hash table
 * is created.  Note that the hash table is <i>open</i>: in the case of a "hash
 * collision", a single bucket stores multiple entries, which must be searched
 * sequentially.  The <i>load factor</i> is a measure of how full the hash
 * table is allowed to get before its capacity is automatically increased.
 * The initial capacity and load factor parameters are merely hints to
 * the implementation.  The exact details as to when and whether the rehash
 * method is invoked are implementation-dependent.<p>
 *
 * Generally, the default load factor (.75) offers a good tradeoff between
 * time and space costs.  Higher values decrease the space overhead but
 * increase the time cost to look up an entry (which is reflected in most
 * {@code Hashtable} operations, including {@code get} and {@code put}).<p>
 *
 * The initial capacity controls a tradeoff between wasted space and the
 * need for {@code rehash} operations, which are time-consuming.
 * No {@code rehash} operations will <i>ever</i> occur if the initial
 * capacity is greater than the maximum number of entries the
 * {@code Hashtable} will contain divided by its load factor.  However,
 * setting the initial capacity too high can waste space.<p>
 *
 * If many entries are to be made into a {@code Hashtable},
 * creating it with a sufficiently large capacity may allow the
 * entries to be inserted more efficiently than letting it perform
 * automatic rehashing as needed to grow the table. <p>
 *
 * This example creates a hashtable of numbers. It uses the names of
 * the numbers as keys:
 * <pre>   {@code
 *   Hashtable<String, Integer> numbers
 *     = new Hashtable<String, Integer>();
 *   numbers.put("one", 1);
 *   numbers.put("two", 2);
 *   numbers.put("three", 3);}</pre>
 *
 * <p>To retrieve a number, use the following code:
 * <pre>   {@code
 *   Integer n = numbers.get("two");
 *   if (n != null) {
 *     System.out.println("two = " + n);
 *   }}</pre>
 *
 * <p>The iterators returned by the {@code iterator} method of the collections
 * returned by all of this class's "collection view methods" are
 * <em>fail-fast</em>: if the Hashtable is structurally modified at any time
 * after the iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 * The Enumerations returned by Hashtable's {@link #keys keys} and
 * {@link #elements elements} methods are <em>not</em> fail-fast; if the
 * Hashtable is structurally modified at any time after the enumeration is
 * created then the results of enumerating are undefined.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>As of the Java 2 platform v1.2, this class was retrofitted to
 * implement the {@link Map} interface, making it a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 *
 * Java Collections Framework</a>.  Unlike the new collection
 * implementations, {@code Hashtable} is synchronized.  If a
 * thread-safe implementation is not needed, it is recommended to use
 * {@link HashMap} in place of {@code Hashtable}.  If a thread-safe
 * highly-concurrent implementation is desired, then it is recommended
 * to use {@link java.util.concurrent.ConcurrentHashMap} in place of
 * {@code Hashtable}.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Arthur van Hoff
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     Object#equals(java.lang.Object)
 * @see     Object#hashCode()
 * @see     Hashtable#rehash()
 * @see     Collection
 * @see     Map
 * @see     HashMap
 * @see     TreeMap
 * @since 1.0
 */

/*
 * Hashtable结构：数组+链表。key与value均不能为null
 *
 * 注：Hashtable线程安全，但性能一般。建议视情形使用HashMap或ConcurrentHashMap来代替
 */
public class Hashtable<K, V> extends Dictionary<K, V> implements Map<K, V>, Cloneable, Serializable {
    
    // Types of Enumerations/Iterations
    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;
    
    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;    // 哈希数组最大容量
    
    /**
     * The hash table data.
     */
    private transient Entry<?,?>[] table;   // 哈希数组（注：哈希数组的容量跟Hashtable可以存储的元素数量不是一回事）
    
    /**
     * The total number of entries in the hash table.
     */
    private transient int count;            // Hashtable中的元素数量
    
    /**
     * The load factor for the hashtable.
     *
     * @serial
     */
    private float loadFactor;   // Hashtable当前使用的装载因子
    
    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     *
     * @serial
     */
    private int threshold;      // Hashtable扩容阈值，默认为0.75*哈希数组容量
    
    /**
     * Each of these fields are initialized to contain an instance of the
     * appropriate view the first time this view is requested.  The views are
     * stateless, so there's no reason to create more than one of each.
     */
    private transient volatile Set<K> keySet;                   // Hashtable中key的集合
    private transient volatile Collection<V> values;            // Hashtable中value的集合
    private transient volatile Set<Map.Entry<K,V>> entrySet;    // Hashtable中entry的集合
    
    
    /**
     * The number of times this Hashtable has been structurally modified
     * Structural modifications are those that change the number of entries in
     * the Hashtable or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the Hashtable fail-fast.  (See ConcurrentModificationException).
     */
    private transient int modCount = 0;     // 记录Hashtable结构的修改次数
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs a new, empty hashtable with a default initial capacity (11)
     * and load factor (0.75).
     */
    public Hashtable() {
        this(11, 0.75f);
    }
    
    /**
     * Constructs a new, empty hashtable with the specified initial capacity
     * and default load factor (0.75).
     *
     * @param initialCapacity the initial capacity of the hashtable.
     *
     * @throws IllegalArgumentException if the initial capacity is less
     *                                  than zero.
     */
    public Hashtable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }
    
    /**
     * Constructs a new, empty hashtable with the specified initial
     * capacity and the specified load factor.
     *
     * @param initialCapacity the initial capacity of the hashtable.
     * @param loadFactor      the load factor of the hashtable.
     *
     * @throws IllegalArgumentException if the initial capacity is less
     *                                  than zero, or if the load factor is nonpositive.
     */
    public Hashtable(int initialCapacity, float loadFactor) {
        if(initialCapacity<0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        
        if(loadFactor<=0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load: " + loadFactor);
        }
        
        if(initialCapacity == 0) {
            initialCapacity = 1;
        }
        
        this.loadFactor = loadFactor;
        table = new Entry<?, ?>[initialCapacity];
        threshold = (int) Math.min(initialCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
    }
    
    /**
     * Constructs a new hashtable with the same mappings as the given
     * Map.  The hashtable is created with an initial capacity sufficient to
     * hold the mappings in the given Map and a default load factor (0.75).
     *
     * @param t the map whose mappings are to be placed in this map.
     *
     * @throws NullPointerException if the specified map is null.
     * @since 1.2
     */
    public Hashtable(Map<? extends K, ? extends V> t) {
        this(Math.max(2 * t.size(), 11), 0.75f);
        putAll(t);
    }
    
    /**
     * A constructor chained from {@link Properties} keeps Hashtable fields
     * uninitialized since they are not used.
     *
     * @param dummy a dummy parameter
     */
    Hashtable(Void dummy) {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Maps the specified {@code key} to the specified
     * {@code value} in this hashtable. Neither the key nor the
     * value can be {@code null}. <p>
     *
     * The value can be retrieved by calling the {@code get} method
     * with a key that is equal to the original key.
     *
     * @param key   the hashtable key
     * @param value the value
     *
     * @return the previous value of the specified key in this hashtable,
     * or {@code null} if it did not have one
     *
     * @throws NullPointerException if the key or value is
     *                              {@code null}
     * @see Object#equals(Object)
     * @see #get(Object)
     */
    // 将指定的元素（key-value）存入Hashtable，并返回旧值，允许覆盖
    public synchronized V put(K key, V value) {
        // Make sure the value is not null
        if(value == null) {
            throw new NullPointerException();
        }
        
        // Makes sure the key is not already in the hashtable.
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        @SuppressWarnings("unchecked")
        Entry<K, V> entry = (Entry<K, V>) tab[index];
        
        // 查找插入位置
        while(entry != null) {
            // 如果遇到了同位元素，则直接覆盖
            if((entry.hash == hash) && entry.key.equals(key)) {
                V old = entry.value;
                entry.value = value;
                return old;
            }
            
            entry = entry.next;
        }
        
        // 向table[index]处插入元素(头插法)，必要时需要扩容
        addEntry(hash, key, value, index);
        
        return null;
    }
    
    // 将指定的元素（key-value）存入Hashtable，并返回旧值，不允许覆盖
    @Override
    public synchronized V putIfAbsent(K key, V value) {
        // Make sure the value is not null
        Objects.requireNonNull(value);
        
        // Makes sure the key is not already in the hashtable.
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        @SuppressWarnings("unchecked")
        Entry<K, V> entry = (Entry<K, V>) tab[index];
        
        // 查找插入位置
        while(entry != null) {
            // 如果遇到了同位元素，则直接覆盖
            if((entry.hash == hash) && entry.key.equals(key)) {
                V old = entry.value;
                if(old == null) {
                    entry.value = value;
                }
                return old;
            }
            
            entry = entry.next;
        }
        
        // 向table[index]处插入元素(头插法)，必要时需要扩容
        addEntry(hash, key, value, index);
        
        return null;
    }
    
    /**
     * Copies all of the mappings from the specified map to this hashtable.
     * These mappings will replace any mappings that this hashtable had for any
     * of the keys currently in the specified map.
     *
     * @param t mappings to be stored in this map
     *
     * @throws NullPointerException if the specified map is null
     * @since 1.2
     */
    // 将指定Map中的元素存入到当前Map（允许覆盖）
    public synchronized void putAll(Map<? extends K, ? extends V> t) {
        for(Map.Entry<? extends K, ? extends V> e : t.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }
    
    /*▲ 存值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key.equals(k))},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     * @throws NullPointerException if the specified key is null
     * @see     #put(Object, Object)
     */
    // 根据指定的key获取对应的value，如果不存在，则返回null
    @SuppressWarnings("unchecked")
    public synchronized V get(Object key) {
        Entry<?,?> tab[] = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        for (Entry<?,?> e = tab[index] ; e != null ; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                return (V)e.value;
            }
        }
        
        return null;
    }
    
    // 根据指定的key获取对应的value，如果不存在，则返回指定的默认值defaultValue
    @Override
    public synchronized V getOrDefault(Object key, V defaultValue) {
        V result = get(key);
        return (null == result) ? defaultValue : result;
    }
    
    /*▲ 取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 移除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Removes the key (and its corresponding value) from this
     * hashtable. This method does nothing if the key is not in the hashtable.
     *
     * @param key the key that needs to be removed
     *
     * @return the value to which the key had been mapped in this hashtable,
     * or {@code null} if the key did not have a mapping
     *
     * @throws NullPointerException if the key is {@code null}
     */
    // 移除拥有指定key的元素，并返回刚刚移除的元素的值
    public synchronized V remove(Object key) {
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        @SuppressWarnings("unchecked")
        Entry<K, V> e = (Entry<K, V>) tab[index];
        
        for(Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
            if((e.hash == hash) && e.key.equals(key)) {
                if(prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                
                modCount++;
                count--;
                V oldValue = e.value;
                e.value = null;
                
                return oldValue;
            }
        }
        
        return null;
    }
    
    // 移除拥有指定key和value的元素，返回值表示是否移除成功
    @Override
    public synchronized boolean remove(Object key, Object value) {
        Objects.requireNonNull(value);
        
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        @SuppressWarnings("unchecked")
        Entry<K, V> e = (Entry<K, V>) tab[index];
        
        for(Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
            if((e.hash == hash) && e.key.equals(key) && e.value.equals(value)) {
                if(prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                
                e.value = null; // clear for gc
                modCount++;
                count--;
                
                return true;
            }
        }
        
        return false;
    }
    
    
    /**
     * Clears this hashtable so that it contains no keys.
     */
    // 清空Hashtable中所有元素
    public synchronized void clear() {
        Entry<?, ?>[] tab = table;
        for(int index = tab.length; --index >= 0; ) {
            tab[index] = null;
        }
        modCount++;
        count = 0;
    }
    
    /*▲ 移除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 替换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将拥有指定key的元素的值替换为newValue，并返回刚刚替换的元素的值（替换失败返回null）
    @Override
    public synchronized V replace(K key, V newValue) {
        Objects.requireNonNull(newValue);
        
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        @SuppressWarnings("unchecked")
        Entry<K, V> e = (Entry<K, V>) tab[index];
        
        while(e != null) {
            if((e.hash == hash) && e.key.equals(key)) {
                V oldValue = e.value;
                e.value = newValue;
                return oldValue;
            }
            
            e = e.next;
        }
        
        return null;
    }
    
    // 将拥有指定key和oldValue的元素的值替换为newValue，返回值表示是否成功替换
    @Override
    public synchronized boolean replace(K key, V oldValue, V newValue) {
        Objects.requireNonNull(oldValue);
        Objects.requireNonNull(newValue);
        
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        @SuppressWarnings("unchecked")
        Entry<K, V> e = (Entry<K, V>) tab[index];
        
        while(e != null) {
            if((e.hash == hash) && e.key.equals(key)) {
                if(e.value.equals(oldValue)) {
                    e.value = newValue;
                    return true;
                } else {
                    return false;
                }
            }
            
            e = e.next;
        }
        
        return false;
    }
    
    // 替换当前Hashtable中的所有元素，替换策略由function决定，function的入参是元素的key和value，出参作为新值
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);     // explicit check required in case
        
        final int expectedModCount = modCount;  // table is empty.
        
        Entry<K, V>[] tab = (Entry<K, V>[]) table;
        
        for(Entry<K, V> entry : tab) {
            while(entry != null) {
                entry.value = Objects.requireNonNull(function.apply(entry.key, entry.value));
                entry = entry.next;
                
                if(expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }
    
    /*▲ 替换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包含查询 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tests if the specified object is a key in this hashtable.
     *
     * @param key possible key
     *
     * @return {@code true} if and only if the specified object
     * is a key in this hashtable, as determined by the
     * {@code equals} method; {@code false} otherwise.
     *
     * @throws NullPointerException if the key is {@code null}
     * @see #contains(Object)
     */
    // 判断Hashtable中是否存在指定key的元素
    public synchronized boolean containsKey(Object key) {
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        for(Entry<?, ?> e = tab[index]; e != null; e = e.next) {
            if((e.hash == hash) && e.key.equals(key)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns true if this hashtable maps one or more keys to this value.
     *
     * <p>Note that this method is identical in functionality to {@link
     * #contains contains} (which predates the {@link Map} interface).
     *
     * @param value value whose presence in this hashtable is to be tested
     *
     * @return {@code true} if this map maps one or more keys to the
     * specified value
     *
     * @throws NullPointerException if the value is {@code null}
     * @since 1.2
     */
    // 判断Hashtable中是否存在指定value的元素
    public boolean containsValue(Object value) {
        return contains(value);
    }
    
    /**
     * Tests if some key maps into the specified value in this hashtable.
     * This operation is more expensive than the {@link #containsKey
     * containsKey} method.
     *
     * <p>Note that this method is identical in functionality to
     * {@link #containsValue containsValue}, (which is part of the
     * {@link Map} interface in the collections framework).
     *
     * @param      value   a value to search for
     * @return     {@code true} if and only if some key maps to the
     *             {@code value} argument in this hashtable as
     *             determined by the {@code equals} method;
     *             {@code false} otherwise.
     * @exception  NullPointerException  if the value is {@code null}
     */
    // 判断Hashtable中是否存在指定value的元素
    public synchronized boolean contains(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
        
        Entry<?,?> tab[] = table;
        
        int i = tab.length;
        
        while(i-- > 0) {
            for (Entry<?,?> e = tab[i] ; e != null ; e = e.next) {
                if (e.value.equals(value)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or {@code addAll}
     * operations.
     *
     * @since 1.2
     */
    // 获取Hashtable中key的集合
    public Set<K> keySet() {
        if(keySet == null) {
            // 将已有的Set包装为同步Set(线程安全)
            keySet = Collections.synchronizedSet(new KeySet(), this);
        }
        
        return keySet;
    }
    
    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own {@code remove} operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll} and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     *
     * @since 1.2
     */
    // 获取Hashtable中value的集合
    public Collection<V> values() {
        if(values == null) {
            // 将已有的容器包装为同步容器(线程安全)
            values = Collections.synchronizedCollection(new ValueCollection(), this);
        }
        
        return values;
    }
    
    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation, or through the
     * {@code setValue} operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Set.remove}, {@code removeAll}, {@code retainAll} and
     * {@code clear} operations.  It does not support the
     * {@code add} or {@code addAll} operations.
     *
     * @since 1.2
     */
    // 获取Hashtable中key-value对的集合
    public Set<Map.Entry<K, V>> entrySet() {
        if(entrySet == null) {
            // 将已有的Set包装为同步Set(线程安全)
            entrySet = Collections.synchronizedSet(new EntrySet(), this);
        }
        
        return entrySet;
    }
    
    
    /**
     * Returns an enumeration of the keys in this hashtable.
     * Use the Enumeration methods on the returned object to fetch the keys
     * sequentially. If the hashtable is structurally modified while enumerating
     * over the keys then the results of enumerating are undefined.
     *
     * @return  an enumeration of the keys in this hashtable.
     * @see     Enumeration
     * @see     #elements()
     * @see     #keySet()
     * @see     Map
     */
    // 返回key的遍历器(不允许删除元素)
    public synchronized Enumeration<K> keys() {
        return this.<K>getEnumeration(KEYS);
    }
    
    /**
     * Returns an enumeration of the values in this hashtable.
     * Use the Enumeration methods on the returned object to fetch the elements
     * sequentially. If the hashtable is structurally modified while enumerating
     * over the values then the results of enumerating are undefined.
     *
     * @return  an enumeration of the values in this hashtable.
     * @see     java.util.Enumeration
     * @see     #keys()
     * @see     #values()
     * @see     Map
     */
    // 返回value的遍历器(不允许删除元素)
    public synchronized Enumeration<V> elements() {
        return this.<V>getEnumeration(VALUES);
    }
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 遍历 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 遍历当前Map中的元素，并对其应用action操作，action的入参是元素的key和value
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);     // explicit check required in case
        
        // table is empty.
        final int expectedModCount = modCount;
        
        Entry<?, ?>[] tab = table;
        
        for(Entry<?, ?> entry : tab) {
            while(entry != null) {
                action.accept((K) entry.key, (V) entry.value);
                
                entry = entry.next;
                
                if(expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }
    
    /*▲ 遍历 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 重新映射 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * <p>This method will, on a best-effort basis, throw a
     * {@link java.util.ConcurrentModificationException} if the remapping
     * function modified this map during computation.
     *
     * @throws ConcurrentModificationException if it is detected that the
     *                                         remapping function modified this map
     */
    /*
     * 插入/删除/替换操作，返回新值（可能为null）
     * 此方法的主要意图：使用备用value和旧value创造的新value来更新旧value
     *
     * 注：以下流程图中，涉及到判断(◇)时，纵向代表【是】，横向代表【否】。此外，使用★代表计算。
     *
     *  ●查找同位元素●
     *      |
     *      ↓
     * ◇存在同位元素◇    --→ ★新value=备用value★ --→ ■如果新value不为null，【插入】新value■
     *      | 是         否
     *      ↓
     * ★新value=(旧value, 备用value)★
     *      |
     *      ↓
     * ◇新value不为null◇ --→ ■【删除】同位元素■
     *      | 是          否
     *      ↓
     * ■新value【替换】旧value■
     */
    @Override
    public synchronized V merge(K key, V bakValue, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        @SuppressWarnings("unchecked")
        Entry<K, V> e = (Entry<K, V>) tab[index];
        
        // 查找同位元素
        for(Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
            // 如果存在同位元素
            if(e.hash == hash && e.key.equals(key)) {
                int mc = modCount;
                
                // 计算新值
                V newValue = remappingFunction.apply(e.value, bakValue);
                
                if(mc != modCount) {
                    throw new ConcurrentModificationException();
                }
                
                // 如果新value不为null，则【替换】旧值
                if(newValue != null) {
                    e.value = newValue;
                    
                    // 如果新value为null，【删除】同位元素
                } else {
                    if(prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    modCount = mc + 1;
                    count--;
                }
                
                return newValue;
            }
        }
        
        // 如果不存在同位元素，且备用value(新值)不为null，则直接【插入】
        if(bakValue != null) {
            // 向table[index]处插入元素(头插法)，必要时需要扩容
            addEntry(hash, key, bakValue, index);
        }
        
        return bakValue;
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>This method will, on a best-effort basis, throw a
     * {@link java.util.ConcurrentModificationException} if the remapping
     * function modified this map during computation.
     *
     * @throws ConcurrentModificationException if it is detected that the
     *                                         remapping function modified this map
     */
    /*
     * 插入/删除/替换操作，返回新值（可能为null）
     * 此方法的主要意图：使用key和旧value创造的新value来更新旧value
     *
     * 注：以下流程图中，涉及到判断(◇)时，纵向代表【是】，横向代表【否】。此外，使用★代表计算。
     *
     *  ●查找同位元素●
     *      |
     *      ↓
     * ◇存在同位元素◇     --→ ★新value=(key, null)★
     *      | 是          否　           |
     *      ↓ 　　　                     |
     * ★新value=(key, 旧value)★        |
     *      ├---------------------------┘
     *      ↓
     * ◇新value不为null◇ --→ ■如果存在同位元素，则【删除】同位元素■
     *      | 是          否
     *      ↓
     * ■  存在同位元素，则新value【替换】旧value■
     * ■不存在同位元素，则【插入】新value       ■
     */
    @Override
    public synchronized V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        @SuppressWarnings("unchecked")
        Entry<K, V> e = (Entry<K, V>) tab[index];
        
        // 查找同位元素
        for(Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
            // 如果存在同位元素
            if(e.hash == hash && Objects.equals(e.key, key)) {
                int mc = modCount;
                
                // 计算新值
                V newValue = remappingFunction.apply(key, e.value);
                
                if(mc != modCount) {
                    throw new ConcurrentModificationException();
                }
                
                // 如果新value不为null，直接【替换】旧值
                if(newValue != null) {
                    e.value = newValue;
                    
                    // 如果新value为null，【删除】同位元素
                } else {
                    if(prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    
                    modCount = mc + 1;
                    count--;
                }
                
                return newValue;
            }
        }
        
        /* 至此，说明不存在同位元素 */
        
        int mc = modCount;
        
        // 计算新值
        V newValue = remappingFunction.apply(key, null);
        
        if(mc != modCount) {
            throw new ConcurrentModificationException();
        }
        
        // 如果新value不为null，【插入】新值
        if(newValue != null) {
            // 向table[index]处插入元素(头插法)，必要时需要扩容
            addEntry(hash, key, newValue, index);
        }
        
        return newValue;
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>This method will, on a best-effort basis, throw a
     * {@link java.util.ConcurrentModificationException} if the remapping
     * function modified this map during computation.
     *
     * @throws ConcurrentModificationException if it is detected that the
     *                                         remapping function modified this map
     */
    /*
     * 删除/替换操作，返回新值（可能为null）
     * 此方法的主要意图：存在同位元素时，使用key和旧value创造的新value来更新旧value
     *
     * 注：以下流程图中，涉及到判断(◇)时，纵向代表【是】，横向代表【否】。此外，使用★代表计算。
     *
     *  ●查找同位元素●
     *      |
     *      ↓
     * ◇存在同位元素◇
     *      | 是
     *      ↓
     * ★新value=(key, 旧value)★
     *      |
     *      ↓
     * ◇新value不为null◇ --→ ■【删除】同位元素■
     *      | 是          否
     *      ↓
     * ■新value【替换】旧value■
     */
    @Override
    public synchronized V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        @SuppressWarnings("unchecked")
        Entry<K, V> e = (Entry<K, V>) tab[index];
        
        // 查找同位元素
        for(Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
            // 如果存在同位元素
            if(e.hash == hash && e.key.equals(key)) {
                int mc = modCount;
                
                // 计算新值
                V newValue = remappingFunction.apply(key, e.value);
                
                if(mc != modCount) {
                    throw new ConcurrentModificationException();
                }
                
                // 如果新value不为null，直接【替换】旧值
                if(newValue != null) {
                    e.value = newValue;
                    
                    // 如果新value为null，【删除】同位元素
                } else {
                    if(prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    
                    modCount = mc + 1;
                    count--;
                }
                
                return newValue;
            }
        }
        
        return null;
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>This method will, on a best-effort basis, throw a
     * {@link java.util.ConcurrentModificationException} if the mapping
     * function modified this map during computation.
     *
     * @throws ConcurrentModificationException if it is detected that the
     *                                         mapping function modified this map
     */
    /*
     * 插入操作，返回新值（可能为null）
     * 此方法的主要意图：不存在同位元素时，使用key创造的新value来更新旧value。如果同位元素存在，则直接返回旧值。
     *
     * 注：以下流程图中，涉及到判断(◇)时，纵向代表【是】，横向代表【否】。此外，使用★代表计算。
     *
     *  ●查找同位元素●
     *      |
     *      ↓
     * ◇存在同位元素◇ --→ ★新value=(key)★
     *                否         |
     *                　         ↓
     *                　  ◇新value不为null◇
     *                　         |
     *                　         ↓
     *                　  ■不存在同位元素，则【插入】新value■
     */
    @Override
    public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        
        Entry<?, ?>[] tab = table;
        
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        
        @SuppressWarnings("unchecked")
        Entry<K, V> e = (Entry<K, V>) tab[index];
        
        // 查找同位元素
        for(; e != null; e = e.next) {
            // 如果存在同位元素，什么也不做，直接返回旧值
            if(e.hash == hash && e.key.equals(key)) {
                // Hashtable not accept null value
                return e.value;
            }
        }
        
        /* 至此，说明不存在同位元素 */
        
        int mc = modCount;
        
        // 计算新值
        V newValue = mappingFunction.apply(key);
        
        if(mc != modCount) {
            throw new ConcurrentModificationException();
        }
        
        // 如果新值不为null，则【插入】新value
        if(newValue != null) {
            // 向table[index]处插入元素(头插法)，必要时需要扩容
            addEntry(hash, key, newValue, index);
        }
        
        return newValue;
    }
    
    /*▲ 重新映射 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of keys in this hashtable.
     *
     * @return the number of keys in this hashtable.
     */
    // 获取Hashtable中的元素数量
    public synchronized int size() {
        return count;
    }
    
    /**
     * Tests if this hashtable maps no keys to values.
     *
     * @return {@code true} if this hashtable maps no keys to values;
     * {@code false} otherwise.
     */
    // 判断Hashtable是否为空集
    public synchronized boolean isEmpty() {
        return count == 0;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = 1421746759512286392L;
    
    
    /**
     * Save the state of the Hashtable to a stream (i.e., serialize it).
     *
     * @serialData The <i>capacity</i> of the Hashtable (the length of the
     * bucket array) is emitted (int), followed by the
     * <i>size</i> of the Hashtable (the number of key-value
     * mappings), followed by the key (Object) and value (Object)
     * for each key-value mapping represented by the Hashtable
     * The key-value mappings are emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
        writeHashtable(s);
    }
    
    /**
     * Reconstitute the Hashtable from a stream (i.e., deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        readHashtable(s);
    }
    
    /**
     * Perform serialization of the Hashtable to an ObjectOutputStream.
     * The Properties class overrides this method.
     */
    void writeHashtable(java.io.ObjectOutputStream s) throws IOException {
        Entry<Object, Object> entryStack = null;
        
        synchronized(this) {
            // Write out the threshold and loadFactor
            s.defaultWriteObject();
            
            // Write out the length and count of elements
            s.writeInt(table.length);
            s.writeInt(count);
            
            // Stack copies of the entries in the table
            for(Entry<?, ?> entry : table) {
                
                while(entry != null) {
                    entryStack = new Entry<>(0, entry.key, entry.value, entryStack);
                    entry = entry.next;
                }
            }
        }
        
        // Write out the key/value objects from the stacked entries
        while(entryStack != null) {
            s.writeObject(entryStack.key);
            s.writeObject(entryStack.value);
            entryStack = entryStack.next;
        }
    }
    
    /**
     * Perform deserialization of the Hashtable from an ObjectInputStream.
     * The Properties class overrides this method.
     */
    void readHashtable(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        // Read in the threshold and loadFactor
        s.defaultReadObject();
        
        // Validate loadFactor (ignore threshold - it will be re-computed)
        if(loadFactor<=0 || Float.isNaN(loadFactor))
            throw new StreamCorruptedException("Illegal Load: " + loadFactor);
        
        // Read the original length of the array and number of elements
        int origlength = s.readInt();
        int elements = s.readInt();
        
        // Validate # of elements
        if(elements<0)
            throw new StreamCorruptedException("Illegal # of Elements: " + elements);
        
        // Clamp original length to be more than elements / loadFactor
        // (this is the invariant enforced with auto-growth)
        origlength = Math.max(origlength, (int) (elements / loadFactor) + 1);
        
        // Compute new length with a bit of room 5% + 3 to grow but
        // no larger than the clamped original length.  Make the length
        // odd if it's large enough, this helps distribute the entries.
        // Guard against the length ending up zero, that's not valid.
        int length = (int) ((elements + elements / 20) / loadFactor) + 3;
        if(length>elements && (length & 1) == 0)
            length--;
        length = Math.min(length, origlength);
        
        if(length<0) { // overflow
            length = origlength;
        }
        
        // Check Map.Entry[].class since it's the nearest public type to
        // what we're actually creating.
        jdk.internal.misc.SharedSecrets.getJavaObjectInputStreamAccess().checkArray(s, Map.Entry[].class, length);
        table = new Entry<?, ?>[length];
        threshold = (int) Math.min(length * loadFactor, MAX_ARRAY_SIZE + 1);
        count = 0;
        
        // Read the number of elements and then all the key/value objects
        for(; elements>0; elements--) {
            @SuppressWarnings("unchecked")
            K key = (K) s.readObject();
            @SuppressWarnings("unchecked")
            V value = (V) s.readObject();
            // sync is eliminated for performance
            reconstitutionPut(table, key, value);
        }
    }
    
    /**
     * Called by Properties to write out a simulated threshold and loadfactor.
     */
    final void defaultWriteHashtable(java.io.ObjectOutputStream s, int length, float loadFactor) throws IOException {
        this.threshold = (int) Math.min(length * loadFactor, MAX_ARRAY_SIZE + 1);
        this.loadFactor = loadFactor;
        s.defaultWriteObject();
    }
    
    /**
     * The put method used by readObject. This is provided because put
     * is overridable and should not be called in readObject since the
     * subclass will not yet be initialized.
     *
     * <p>This differs from the regular put method in several ways. No
     * checking for rehashing is necessary since the number of elements
     * initially in the table is known. The modCount is not incremented and
     * there's no synchronization because we are creating a new instance.
     * Also, no return value is needed.
     */
    private void reconstitutionPut(Entry<?, ?>[] tab, K key, V value) throws StreamCorruptedException {
        if(value == null) {
            throw new java.io.StreamCorruptedException();
        }
        // Makes sure the key is not already in the hashtable.
        // This should not happen in deserialized version.
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for(Entry<?, ?> e = tab[index]; e != null; e = e.next) {
            if((e.hash == hash) && e.key.equals(key)) {
                throw new java.io.StreamCorruptedException();
            }
        }
        // Creates the new entry.
        @SuppressWarnings("unchecked")
        Entry<K, V> e = (Entry<K, V>) tab[index];
        tab[index] = new Entry<>(hash, key, value, e);
        count++;
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Compares the specified Object with this Map for equality,
     * as per the definition in the Map interface.
     *
     * @param  o object to be compared for equality with this hashtable
     * @return true if the specified Object is equal to this Map
     * @see Map#equals(Object)
     * @since 1.2
     */
    public synchronized boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof Map)) {
            return false;
        }
        
        Map<?,?> t = (Map<?,?>) o;
        
        if (t.size() != size()) {
            return false;
        }
        
        try {
            for (Map.Entry<K, V> e : entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(t.get(key) == null && t.containsKey(key))) {
                        return false;
                    }
                } else {
                    if (!value.equals(t.get(key))) {
                        return false;
                    }
                }
            }
        } catch (ClassCastException unused)   {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns the hash code value for this Map as per the definition in the
     * Map interface.
     *
     * @see Map#hashCode()
     * @since 1.2
     */
    public synchronized int hashCode() {
        /*
         * This code detects the recursion caused by computing the hash code
         * of a self-referential hash table and prevents the stack overflow
         * that would otherwise result.  This allows certain 1.1-era
         * applets with self-referential hash tables to work.  This code
         * abuses the loadFactor field to do double-duty as a hashCode
         * in progress flag, so as not to worsen the space performance.
         * A negative load factor indicates that hash code computation is
         * in progress.
         */
        int h = 0;
        if (count == 0 || loadFactor < 0) {
            return h;  // Returns zero
        }
        
        loadFactor = -loadFactor;  // Mark hashCode computation in progress
        Entry<?,?>[] tab = table;
        for (Entry<?,?> entry : tab) {
            while (entry != null) {
                h += entry.hashCode();
                entry = entry.next;
            }
        }
        
        loadFactor = -loadFactor;  // Mark hashCode computation complete
        
        return h;
    }
    
    /**
     * Returns a string representation of this {@code Hashtable} object
     * in the form of a set of entries, enclosed in braces and separated
     * by the ASCII characters "<code> ,&nbsp;</code>" (comma and space). Each
     * entry is rendered as the key, an equals sign {@code =}, and the
     * associated element, where the {@code toString} method is used to
     * convert the key and element to strings.
     *
     * @return  a string representation of this hashtable
     */
    public synchronized String toString() {
        int max = size() - 1;
        if (max == -1) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<K,V>> it = entrySet().iterator();
        
        sb.append('{');
        for (int i = 0; ; i++) {
            Map.Entry<K,V> e = it.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key   == this ? "(this Map)" : key.toString());
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value.toString());
            
            if (i == max) {
                return sb.append('}').toString();
            }
            sb.append(", ");
        }
    }
    
    /**
     * Creates a shallow copy of this hashtable. All the structure of the
     * hashtable itself is copied, but the keys and values are not cloned.
     * This is a relatively expensive operation.
     *
     * @return  a clone of the hashtable
     */
    public synchronized Object clone() {
        Hashtable<?,?> t = cloneHashtable();
        t.table = new Entry<?,?>[table.length];
        for (int i = table.length ; i-- > 0 ; ) {
            t.table[i] = (table[i] != null) ? (Entry<?,?>) table[i].clone() : null;
        }
        t.keySet = null;
        t.entrySet = null;
        t.values = null;
        t.modCount = 0;
        return t;
    }
    
    
    
    // 向table[index]处插入元素(头插法)，必要时需要扩容
    private void addEntry(int hash, K key, V value, int index) {
        Entry<?,?> tab[] = table;
        
        // 如果Hashtable中的元素数量已达扩容阙值，则需要扩容
        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();
            
            tab = table;
            
            hash = key.hashCode();
            
            // 重新计算当前元素需要插入的位置
            index = (hash & 0x7FFFFFFF) % tab.length;
        }
        
        // Creates the new entry.
        @SuppressWarnings("unchecked")
        Entry<K,V> e = (Entry<K,V>) tab[index];
        // 插入新元素
        tab[index] = new Entry<>(hash, key, value, e);
        
        count++;
        
        modCount++;
    }
    
    /**
     * Increases the capacity of and internally reorganizes this
     * hashtable, in order to accommodate and access its entries more
     * efficiently.  This method is called automatically when the
     * number of keys in the hashtable exceeds this hashtable's capacity
     * and load factor.
     */
    // 对哈希数组扩容
    @SuppressWarnings("unchecked")
    protected void rehash() {
        int oldCapacity = table.length;
        Entry<?,?>[] oldMap = table;
        
        /* overflow-conscious code */
        // 新容量翻倍
        int newCapacity = (oldCapacity << 1) + 1;
        // 确保新容量不超过MAX_ARRAY_SIZE
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            if (oldCapacity == MAX_ARRAY_SIZE) {
                // Keep running with MAX_ARRAY_SIZE buckets
                return;
            }
            
            newCapacity = MAX_ARRAY_SIZE;
        }
        
        Entry<?,?>[] newMap = new Entry<?,?>[newCapacity];
        
        modCount++;
        
        // 新的扩容阈值
        threshold = (int)Math.min(newCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
        
        table = newMap;
        
        // 扩容后，需要对原有的元素重新哈希
        for (int i = oldCapacity ; i-- > 0 ;) {
            for (Entry<K,V> old = (Entry<K,V>)oldMap[i] ; old != null ; ) {
                Entry<K,V> e = old;
                old = old.next;
                
                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = (Entry<K,V>)newMap[index];
                newMap[index] = e;
            }
        }
    }
    
    /** Calls super.clone() */
    // 返回当前Hashtable的一个拷贝
    final Hashtable<?,?> cloneHashtable() {
        try {
            return (Hashtable<?,?>)super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }
    
    // 返回指定类型元素的遍历器(不允许删除元素)，type类型可以是key/value/entry
    private <T> Enumeration<T> getEnumeration(int type) {
        if(count == 0) {
            return Collections.emptyEnumeration();
        } else {
            return new Enumerator<>(type, false);
        }
    }
    
    // 返回指定类型元素的遍历器(允许删除元素)，type类型可以是key/value/entry
    private <T> Iterator<T> getIterator(int type) {
        if(count == 0) {
            return Collections.emptyIterator();
        } else {
            return new Enumerator<>(type, true);
        }
    }
    
    
    
    
    
    
    /**
     * Hashtable bucket collision list entry
     */
    // Map中的键值对实体
    private static class Entry<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        V value;
        
        Entry<K, V> next;
        
        protected Entry(int hash, K key, V value, Entry<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
        
        public K getKey() {
            return key;
        }
        
        public V getValue() {
            return value;
        }
        
        public V setValue(V value) {
            if(value == null)
                throw new NullPointerException();
            
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        
        public boolean equals(Object o) {
            if(!(o instanceof Map.Entry)) {
                return false;
            }
            
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            
            return (key == null ? e.getKey() == null : key.equals(e.getKey())) && (value == null ? e.getValue() == null : value.equals(e.getValue()));
        }
        
        public int hashCode() {
            return hash ^ Objects.hashCode(value);
        }
        
        public String toString() {
            return key.toString() + "=" + value.toString();
        }
        
        @SuppressWarnings("unchecked")
        protected Object clone() {
            return new Entry<>(hash, key, value, (next == null ? null : (Entry<K, V>) next.clone()));
        }
    }
    
    
    
    // key的集合
    private class KeySet extends AbstractSet<K> {
        // 返回key的遍历器(允许删除元素)
        public Iterator<K> iterator() {
            return getIterator(KEYS);
        }
        
        public int size() {
            return count;
        }
        
        public boolean contains(Object o) {
            return containsKey(o);
        }
        
        public boolean remove(Object o) {
            return Hashtable.this.remove(o) != null;
        }
        
        public void clear() {
            Hashtable.this.clear();
        }
    }
    
    // value的集合
    private class ValueCollection extends AbstractCollection<V> {
        // 返回value的遍历器(允许删除元素)
        public Iterator<V> iterator() {
            return getIterator(VALUES);
        }
        
        public int size() {
            return count;
        }
        
        public boolean contains(Object o) {
            return containsValue(o);
        }
        
        public void clear() {
            Hashtable.this.clear();
        }
    }
    
    // entry的集合
    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        // 返回entry的遍历器(允许删除元素)
        public Iterator<Map.Entry<K, V>> iterator() {
            return getIterator(ENTRIES);
        }
        
        public boolean add(Map.Entry<K, V> o) {
            return super.add(o);
        }
        
        public boolean contains(Object o) {
            if(!(o instanceof Map.Entry)) {
                return false;
            }
            
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            Object key = entry.getKey();
            Entry<?, ?>[] tab = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;
            
            for(Entry<?, ?> e = tab[index]; e != null; e = e.next) {
                if(e.hash == hash && e.equals(entry)) {
                    return true;
                }
            }
            
            return false;
        }
        
        public boolean remove(Object o) {
            if(!(o instanceof Map.Entry)) {
                return false;
            }
            
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            Object key = entry.getKey();
            Entry<?, ?>[] tab = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;
            
            @SuppressWarnings("unchecked")
            Entry<K, V> e = (Entry<K, V>) tab[index];
            for(Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
                if(e.hash == hash && e.equals(entry)) {
                    if(prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    
                    e.value = null; // clear for gc.
                    modCount++;
                    count--;
                    return true;
                }
            }
            return false;
        }
        
        public int size() {
            return count;
        }
        
        public void clear() {
            Hashtable.this.clear();
        }
    }
    
    
    
    /**
     * A hashtable enumerator class.  This class implements both the
     * Enumeration and Iterator interfaces, but individual instances
     * can be created with the Iterator methods disabled.  This is necessary
     * to avoid unintentionally increasing the capabilities granted a user
     * by passing an Enumeration.
     */
    // 遍历器(枚举器和迭代器的组合)
    private class Enumerator<T> implements Enumeration<T>, Iterator<T> {
        
        /**
         * Indicates whether this Enumerator is serving as an Iterator or an Enumeration.  (true -> Iterator).
         */
        final boolean iterator; // 是否允许移除元素
        
        final int type; // 标记待遍历元素类型，这些元素可以是：key/value/entry
        
        final Entry<?, ?>[] table = Hashtable.this.table;   // 哈希数组
        
        int index = table.length;   // 记录当前需要遍历的哈希槽
        
        Entry<?, ?> entry;          // 记录下一个待遍历元素
        
        Entry<?, ?> lastReturned;   // 记录上次遍历命中的元素
        
        /**
         * The modCount value that the iterator believes that the backing
         * Hashtable should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        protected int expectedModCount = Hashtable.this.modCount;
        
        Enumerator(int type, boolean iterator) {
            this.type = type;
            this.iterator = iterator;
        }
        
        // 是否存在未遍历元素
        public boolean hasMoreElements() {
            Entry<?, ?> e = entry;
            int i = index;
            
            /* Use locals for faster loop iteration */
            while(e == null && i>0) {
                e = table[--i];
            }
            
            entry = e;
            index = i;
            
            return e != null;
        }
        
        // 返回下一个元素
        @SuppressWarnings("unchecked")
        public T nextElement() {
            Entry<?, ?> et = entry;
            int i = index;
            
            /* Use locals for faster loop iteration */
            while(et == null && i>0) {
                et = table[--i];
            }
            
            entry = et;
            index = i;
            
            if(et != null) {
                Entry<?, ?> e = lastReturned = entry;
                entry = e.next;
                return type == KEYS ? (T) e.key : (type == VALUES ? (T) e.value : (T) e);
            }
            
            throw new NoSuchElementException("Hashtable Enumerator");
        }
        
        // 是否存在下一个未遍历元素
        public boolean hasNext() {
            return hasMoreElements();
        }
        
        // 返回下一个元素
        public T next() {
            if(Hashtable.this.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            return nextElement();
        }
        
        // 移除上一个遍历的元素
        public void remove() {
            if(!iterator) {
                throw new UnsupportedOperationException();
            }
            
            if(lastReturned == null) {
                throw new IllegalStateException("Hashtable Enumerator");
            }
            
            if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            synchronized(Hashtable.this) {
                Entry<?, ?>[] tab = Hashtable.this.table;
                int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;
                
                @SuppressWarnings("unchecked")
                Entry<K, V> e = (Entry<K, V>) tab[index];
                
                for(Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
                    if(e == lastReturned) {
                        if(prev == null) {
                            tab[index] = e.next;
                        } else {
                            prev.next = e.next;
                        }
                        expectedModCount++;
                        lastReturned = null;
                        Hashtable.this.modCount++;
                        Hashtable.this.count--;
                        return;
                    }
                }
                
                throw new ConcurrentModificationException();
            }
        }
    }
    
}
