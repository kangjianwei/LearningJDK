/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A Red-Black tree based {@link NavigableMap} implementation.
 * The map is sorted according to the {@linkplain Comparable natural
 * ordering} of its keys, or by a {@link Comparator} provided at map
 * creation time, depending on which constructor is used.
 *
 * <p>This implementation provides guaranteed log(n) time cost for the
 * {@code containsKey}, {@code get}, {@code put} and {@code remove}
 * operations.  Algorithms are adaptations of those in Cormen, Leiserson, and
 * Rivest's <em>Introduction to Algorithms</em>.
 *
 * <p>Note that the ordering maintained by a tree map, like any sorted map, and
 * whether or not an explicit comparator is provided, must be <em>consistent
 * with {@code equals}</em> if this sorted map is to correctly implement the
 * {@code Map} interface.  (See {@code Comparable} or {@code Comparator} for a
 * precise definition of <em>consistent with equals</em>.)  This is so because
 * the {@code Map} interface is defined in terms of the {@code equals}
 * operation, but a sorted map performs all key comparisons using its {@code
 * compareTo} (or {@code compare}) method, so two keys that are deemed equal by
 * this method are, from the standpoint of the sorted map, equal.  The behavior
 * of a sorted map <em>is</em> well-defined even if its ordering is
 * inconsistent with {@code equals}; it just fails to obey the general contract
 * of the {@code Map} interface.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a map concurrently, and at least one of the
 * threads modifies the map structurally, it <em>must</em> be synchronized
 * externally.  (A structural modification is any operation that adds or
 * deletes one or more mappings; merely changing the value associated
 * with an existing key is not a structural modification.)  This is
 * typically accomplished by synchronizing on some object that naturally
 * encapsulates the map.
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedSortedMap Collections.synchronizedSortedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map: <pre>
 *   SortedMap m = Collections.synchronizedSortedMap(new TreeMap(...));</pre>
 *
 * <p>The iterators returned by the {@code iterator} method of the collections
 * returned by all of this class's "collection view methods" are
 * <em>fail-fast</em>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:   <em>the fail-fast behavior of iterators
 * should be used only to detect bugs.</em>
 *
 * <p>All {@code Map.Entry} pairs returned by methods in this class
 * and its views represent snapshots of mappings at the time they were
 * produced. They do <strong>not</strong> support the {@code Entry.setValue}
 * method. (Note however that it is possible to change mappings in the
 * associated map using {@code put}.)
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Josh Bloch and Doug Lea
 * @see Map
 * @see HashMap
 * @see Hashtable
 * @see Comparable
 * @see Comparator
 * @see Collection
 * @since 1.2
 */

/*
 * TreeMap结构：红黑树(没有哈希数组，这一点不同于HashMap)。key不能为null，但value可以为null
 *
 * TreeMap中的key有序，可以升序也可以降序
 *
 * key的排序方式依赖于外部比较器（优先使用）和内部比较器
 *
 * 注：
 * 在无特殊说明的情形下，注释中提到的遍历都是指中序遍历当前的Map
 * 至于中序序列是递增还是递减，则由Map的特性决定（可能是升序Map，也可能是降序Map）
 *
 * 术语约定：
 * TreeMap中包含两种子视图：AscendingSubMap和DescendingSubMap
 * 在TreeMap及其子视图中，当提到靠左、靠前、靠右、靠后的元素时，指的是在正序Map下的排序
 * 而正序Map或逆序Map是由相关的内部比较器和外部比较器决定的
 */
public class TreeMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, Cloneable, Serializable {
    
    /**
     * The comparator used to maintain order in this tree map, or null if it uses the natural ordering of its keys.
     *
     * @serial
     */
    private final Comparator<? super K> comparator; // TreeMap中的外部比较器，如果不为null，会优先使用
    
    private transient Entry<K, V> root; // 红黑树的根（TreeMap根结点）
    
    // 代表红黑树结点颜色的常量
    private static final boolean RED = false;
    private static final boolean BLACK = true;
    
    /**
     * Fields initialized to contain an instance of the entry set view
     * the first time this view is requested.  Views are stateless, so
     * there's no reason to create more than one.
     */
    private transient EntrySet entrySet;                // 当前Map中key-value对的集合
    private transient KeySet<K> navigableKeySet;        // 当前Map中的key的集合
    private transient NavigableMap<K, V> descendingMap; // 【逆序】Map（实质是对当前Map实例的一个【逆序】包装）
    
    /**
     * The number of entries in the tree
     */
    private transient int size = 0;     // TreeMap中的元素数量
    
    /**
     * The number of structural modifications to the tree.
     */
    private transient int modCount = 0; // 记录TreeMap结构的修改次数
    
    /**
     * Dummy value serving as unmatchable fence key for unbounded SubMapIterators
     */
    private static final Object UNBOUNDED = new Object();
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs a new, empty tree map, using the natural ordering of its
     * keys.  All keys inserted into the map must implement the {@link
     * Comparable} interface.  Furthermore, all such keys must be
     * <em>mutually comparable</em>: {@code k1.compareTo(k2)} must not throw
     * a {@code ClassCastException} for any keys {@code k1} and
     * {@code k2} in the map.  If the user attempts to put a key into the
     * map that violates this constraint (for example, the user attempts to
     * put a string key into a map whose keys are integers), the
     * {@code put(Object key, Object value)} call will throw a
     * {@code ClassCastException}.
     */
    public TreeMap() {
        comparator = null;
    }
    
    /**
     * Constructs a new, empty tree map, ordered according to the given
     * comparator.  All keys inserted into the map must be <em>mutually
     * comparable</em> by the given comparator: {@code comparator.compare(k1,
     * k2)} must not throw a {@code ClassCastException} for any keys
     * {@code k1} and {@code k2} in the map.  If the user attempts to put
     * a key into the map that violates this constraint, the {@code put(Object
     * key, Object value)} call will throw a
     * {@code ClassCastException}.
     *
     * @param comparator the comparator that will be used to order this map.
     *                   If {@code null}, the {@linkplain Comparable natural
     *                   ordering} of the keys will be used.
     */
    public TreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }
    
    /**
     * Constructs a new tree map containing the same mappings as the given
     * map, ordered according to the <em>natural ordering</em> of its keys.
     * All keys inserted into the new map must implement the {@link
     * Comparable} interface.  Furthermore, all such keys must be
     * <em>mutually comparable</em>: {@code k1.compareTo(k2)} must not throw
     * a {@code ClassCastException} for any keys {@code k1} and
     * {@code k2} in the map.  This method runs in n*log(n) time.
     *
     * @param m the map whose mappings are to be placed in this map
     *
     * @throws ClassCastException   if the keys in m are not {@link Comparable},
     *                              or are not mutually comparable
     * @throws NullPointerException if the specified map is null
     */
    public TreeMap(Map<? extends K, ? extends V> m) {
        comparator = null;
        putAll(m);
    }
    
    /**
     * Constructs a new tree map containing the same mappings and
     * using the same ordering as the specified sorted map.  This
     * method runs in linear time.
     *
     * @param m the sorted map whose mappings are to be placed in this map,
     *          and whose comparator is to be used to sort this map
     *
     * @throws NullPointerException if the specified map is null
     */
    public TreeMap(SortedMap<K, ? extends V> m) {
        comparator = m.comparator();
        try {
            buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
        } catch(java.io.IOException | ClassNotFoundException cannotHappen) {
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}.
     * (A {@code null} return can also indicate that the map
     * previously associated {@code null} with {@code key}.)
     *
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    /*
     * 向当前Map中存储一个key-value对，返回值代表该位置存储之前的值
     *
     * 如果外部比较器comparator有效，则允许key为null
     * 否则，key不能为null，且需要实现内部比较器Comparable接口
     */
    public V put(K key, V value) {
        Entry<K, V> t = root;
        
        // 如果根结点为null，说明是首个结点
        if(t == null) {
            // 这里使用compare起到了校验作用
            compare(key, key); // type (and possibly null) check
            
            // 创建一个红黑树结点
            root = new Entry<>(key, value, null);
            size = 1;
            modCount++;
            return null;
        }
        
        int cmp;
        Entry<K, V> parent;
        
        // split comparator and comparable paths
        Comparator<? super K> cpr = comparator;
        
        /* 查找同位元素，如果找到，直接覆盖 */
        
        // 如果存在外部比较器
        if(cpr != null) {
            do {
                parent = t;
                cmp = cpr.compare(key, t.key);
                if(cmp<0) {
                    t = t.left;
                } else if(cmp>0) {
                    t = t.right;
                } else {
                    return t.setValue(value);
                }
            } while(t != null);
            
            // 如果不存在外部比较器，则要求key实现内部比较器Comparable接口
        } else {
            if(key == null) {
                throw new NullPointerException();
            }
            
            @SuppressWarnings("unchecked")
            Comparable<? super K> k = (Comparable<? super K>) key;
            do {
                parent = t;
                cmp = k.compareTo(t.key);
                if(cmp<0) {
                    t = t.left;
                } else if(cmp>0) {
                    t = t.right;
                } else {
                    return t.setValue(value);
                }
            } while(t != null);
        }
        
        /* 至此，说明没找到同位元素，需要新建一个元素插入到红黑树中 */
        
        Entry<K, V> e = new Entry<>(key, value, parent);
        if(cmp<0) {
            parent.left = e;
        } else {
            parent.right = e;
        }
        
        // 将元素e插入到红黑树后，可能会破坏其平衡性，所以这里需要做出调整，保持红黑树的平衡
        fixAfterInsertion(e);
        
        size++;
        
        modCount++;
        
        return null;
    }
    
    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings replace any mappings that this map had for any
     * of the keys currently in the specified map.
     *
     * @param map mappings to be stored in this map
     *
     * @throws ClassCastException   if the class of a key or value in
     *                              the specified map prevents it from being stored in this map
     * @throws NullPointerException if the specified map is null or
     *                              the specified map contains a null key and this map does not
     *                              permit null keys
     */
    // 将指定Map中的元素存入到当前Map（允许覆盖）
    public void putAll(Map<? extends K, ? extends V> map) {
        int mapSize = map.size();
        
        if(size == 0 && mapSize != 0 && map instanceof SortedMap) {
            Comparator<?> c = ((SortedMap<?, ?>) map).comparator();
            if(c == comparator || (c != null && c.equals(comparator))) {
                ++modCount;
                try {
                    buildFromSorted(mapSize, map.entrySet().iterator(), null, null);
                } catch(IOException | ClassNotFoundException cannotHappen) {
                }
                return;
            }
        }
        
        super.putAll(map);
    }
    
    /*▲ 存值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key} compares
     * equal to {@code k} according to the map's ordering, then this
     * method returns {@code v}; otherwise it returns {@code null}.
     * (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <em>necessarily</em>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    // 查找key对应的元素的值，如果不存在该元素，则返回null值
    public V get(Object key) {
        // 查找key对应的元素
        Entry<K, V> p = getEntry(key);
        return (p == null ? null : p.value);
    }
    
    /*▲ 取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 移除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Removes the mapping for this key from this TreeMap if present.
     *
     * @param key key for which mapping should be removed
     *
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}.
     * (A {@code null} return can also indicate that the map
     * previously associated {@code null} with {@code key}.)
     *
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    // 查找key对应的元素，并移除该元素
    public V remove(Object key) {
        Entry<K, V> p = getEntry(key);
        if(p == null) {
            return null;
        }
        
        V oldValue = p.value;
        
        // 将元素从红黑树中移除
        deleteEntry(p);
        
        return oldValue;
    }
    
    
    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    // 清空当前Map
    public void clear() {
        modCount++;
        size = 0;
        root = null;
    }
    
    /*▲ 移除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 替换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将拥有指定key的元素的值替换为newValue，并返回刚刚替换的元素的值（替换失败返回null）
    @Override
    public V replace(K key, V newValue) {
        // 查找key对应的元素
        Entry<K, V> p = getEntry(key);
        
        if(p != null) {
            V oldValue = p.value;
            p.value = newValue;
            return oldValue;
        }
        
        return null;
    }
    
    // 将拥有指定key和oldValue的元素的值替换为newValue，返回值表示是否成功替换
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        // 查找key对应的元素
        Entry<K, V> p = getEntry(key);
        
        if(p != null && Objects.equals(oldValue, p.value)) {
            p.value = newValue;
            return true;
        }
        
        return false;
    }
    
    // 替换当前当前Map中的所有元素，替换策略由function决定，function的入参是元素的key和value，出参作为新值
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        
        int expectedModCount = modCount;
        
        for(Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            e.value = function.apply(e.key, e.value);
            
            if(expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
    
    /*▲ 替换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包含查询 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested
     *
     * @return {@code true} if this map contains a mapping for the
     * specified key
     *
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    // 判断当前Map中是否存在指定key的元素
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }
    
    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value.  More formally, returns {@code true} if and only if
     * this map contains at least one mapping to a value {@code v} such
     * that {@code (value==null ? v==null : value.equals(v))}.  This
     * operation will probably require time linear in the map size for
     * most implementations.
     *
     * @param value value whose presence in this map is to be tested
     *
     * @return {@code true} if a mapping to {@code value} exists;
     * {@code false} otherwise
     *
     * @since 1.2
     */
    // 判断当前Map中是否存在指定value的元素
    public boolean containsValue(Object value) {
        for(Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            if(valEquals(value, e.value)) {
                return true;
            }
        }
        
        return false;
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a {@link Set} view of the keys contained in this map.
     *
     * <p>The set's iterator returns the keys in ascending order.
     * The set's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#SORTED}
     * and {@link Spliterator#ORDERED} with an encounter order that is ascending
     * key order.  The spliterator's comparator (see
     * {@link java.util.Spliterator#getComparator()}) is {@code null} if
     * the tree map's comparator (see {@link #comparator()}) is {@code null}.
     * Otherwise, the spliterator's comparator is the same as or imposes the
     * same total ordering as the tree map's comparator.
     *
     * <p>The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or {@code addAll}
     * operations.
     */
    // 获取当前Map中key的集合
    public Set<K> keySet() {
        return navigableKeySet();
    }
    
    /**
     * Returns a {@link Collection} view of the values contained in this map.
     *
     * <p>The collection's iterator returns the values in ascending order
     * of the corresponding keys. The collection's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#ORDERED}
     * with an encounter order that is ascending order of the corresponding
     * keys.
     *
     * <p>The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own {@code remove} operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll} and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     */
    // 获取当前Map中value的集合
    public Collection<V> values() {
        Collection<V> vs = values;
        if(vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }
    
    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     *
     * <p>The set's iterator returns the entries in ascending key order. The
     * set's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#SORTED} and
     * {@link Spliterator#ORDERED} with an encounter order that is ascending key
     * order.
     *
     * <p>The set is backed by the map, so changes to the map are
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
     */
    // 获取当前Map中key-value对的集合
    public Set<Map.Entry<K, V>> entrySet() {
        EntrySet es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 遍历 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 遍历TreeMap中的元素，并对其应用action操作，action的入参是元素的key和value
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = modCount;
        
        for(Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            action.accept(e.key, e.value);
            
            if(expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
    
    /*▲ 遍历 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ NavigableMap/SortedMap █████████████████████████████████████████████████████████████┓ */
    
    // 获取当前Map的外部比较器Comparator
    public Comparator<? super K> comparator() {
        return comparator;
    }
    
    
    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    // 返回遍历当前Map时的首个结点的key
    public K firstKey() {
        return key(getFirstEntry());
    }
    
    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    // 返回遍历当前Map时的末尾结点的key
    public K lastKey() {
        return key(getLastEntry());
    }
    
    /**
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     * @since 1.6
     */
    // 〖前驱〗获取一个key，该key是遍历当前Map时形参key的前驱
    public K lowerKey(K key) {
        return keyOrNull(getLowerEntry(key));
    }
    
    /**
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     * @since 1.6
     */
    // 〖后继〗获取一个key，该key是遍历当前Map时形参key的后继
    public K higherKey(K key) {
        return keyOrNull(getHigherEntry(key));
    }
    
    /**
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     * @since 1.6
     */
    // 【前驱】获取一个key，该key是遍历当前Map时形参key的前驱（包括key本身）
    public K floorKey(K key) {
        return keyOrNull(getFloorEntry(key));
    }
    
    /**
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     * @since 1.6
     */
    // 【后继】获取一个key，该key是遍历当前Map时形参key的后继（包括key本身）
    public K ceilingKey(K key) {
        return keyOrNull(getCeilingEntry(key));
    }
    
    
    /**
     * @since 1.6
     */
    // 获取遍历当前Map时的首个结点，然后将其包装为只读版本的Entry
    public Map.Entry<K, V> firstEntry() {
        return exportEntry(getFirstEntry());
    }
    
    /**
     * @since 1.6
     */
    // 返回遍历当前Map时的末尾结点，然后将其包装为只读版本的Entry
    public Map.Entry<K, V> lastEntry() {
        return exportEntry(getLastEntry());
    }
    
    /**
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     * @since 1.6
     */
    // 〖前驱〗获取一个结点并将其包装为只读版本的Entry，该结点包含的key，是遍历当前Map时形参k的前驱
    public Map.Entry<K, V> lowerEntry(K key) {
        return exportEntry(getLowerEntry(key));
    }
    
    /**
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     * @since 1.6
     */
    // 〖后继〗获取一个结点并将其包装为只读版本的Entry，该结点包含的key，是遍历当前Map时形参k的后继
    public Map.Entry<K, V> higherEntry(K key) {
        return exportEntry(getHigherEntry(key));
    }
    
    /**
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     * @since 1.6
     */
    // 【前驱】获取一个结点并将其包装为只读版本的Entry，该结点包含的key，是遍历当前Map时形参k的前驱（包括k本身）
    public Map.Entry<K, V> floorEntry(K key) {
        return exportEntry(getFloorEntry(key));
    }
    
    /**
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     * @since 1.6
     */
    // 【后继】获取一个结点并将其包装为只读版本的Entry，该结点包含的key，是遍历当前Map时形参k的后继（包括k本身）
    public Map.Entry<K, V> ceilingEntry(K key) {
        return exportEntry(getCeilingEntry(key));
    }
    
    
    /**
     * @since 1.6
     */
    // 移除遍历当前Map时的首个结点，并将其包装为只读版本的Entry后返回
    public Map.Entry<K, V> pollFirstEntry() {
        Entry<K, V> p = getFirstEntry();
        if(p != null) {
            deleteEntry(p);
        }
        
        return exportEntry(p);
    }
    
    /**
     * @since 1.6
     */
    // 移除遍历当前Map时的末尾结点，并将其包装为只读版本的Entry后返回
    public Map.Entry<K, V> pollLastEntry() {
        Entry<K, V> p = getLastEntry();
        if(p != null) {
            deleteEntry(p);
        }
        
        return exportEntry(p);
    }
    
    
    /**
     * @since 1.6
     */
    // 获取当前Map中的key的集合
    public NavigableSet<K> navigableKeySet() {
        KeySet<K> nks = navigableKeySet;
        return (nks != null) ? nks : (navigableKeySet = new KeySet<>(this));
    }
    
    /**
     * @since 1.6
     */
    // 获取【逆序】Map中的key的集合
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }
    
    
    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     if {@code fromKey} or {@code toKey} is
     *                                  null and this map uses natural ordering, or its comparator
     *                                  does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    // 获取【理论区间】为[fromKey, toKey)的SubMap
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }
    
    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     if {@code fromKey} or {@code toKey} is
     *                                  null and this map uses natural ordering, or its comparator
     *                                  does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    // 获取【理论区间】为〖fromKey, toKey〗的SubMap，区间下限/上限是否为闭区间由fromInclusive和toInclusive参数决定
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return new AscendingSubMap<>(this, false, fromKey, fromInclusive, false, toKey, toInclusive);
    }
    
    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     if {@code toKey} is null
     *                                  and this map uses natural ordering, or its comparator
     *                                  does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    // 获取【理论区间】上限为toKey(不包含)的SubMap
    public SortedMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }
    
    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     if {@code toKey} is null
     *                                  and this map uses natural ordering, or its comparator
     *                                  does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    // 获取【理论区间】上限为toKey的SubMap，区间上限是否为闭区间由inclusive参数决定
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return new AscendingSubMap<>(this, true, null, true, false, toKey, inclusive);
    }
    
    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     if {@code fromKey} is null
     *                                  and this map uses natural ordering, or its comparator
     *                                  does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    // 获取【理论区间】下限为fromKey(包含)的SubMap
    public SortedMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }
    
    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     if {@code fromKey} is null
     *                                  and this map uses natural ordering, or its comparator
     *                                  does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    // 获取【理论区间】下限为fromKey的SubMap，区间下限是否为闭区间由inclusive参数决定
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return new AscendingSubMap<>(this, false, fromKey, inclusive, true, null, true);
    }
    
    
    /**
     * @since 1.6
     */
    // 获取【逆序】Map（实质是对当前Map实例的一个【逆序】包装）
    public NavigableMap<K, V> descendingMap() {
        NavigableMap<K, V> km = descendingMap;
        return (km != null) ? km : (descendingMap = new DescendingSubMap<>(this, true, null, true, true, null, true));
    }
    
    /*▲ NavigableMap/SortedMap █████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    // 获取当前Map中的元素数量
    public int size() {
        return size;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static final long serialVersionUID = 919286545866124006L;
    
    /**
     * Save the state of the {@code TreeMap} instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <em>size</em> of the TreeMap (the number of key-value
     * mappings) is emitted (int), followed by the key (Object)
     * and value (Object) for each key-value mapping represented
     * by the TreeMap. The key-value mappings are emitted in
     * key-order (as determined by the TreeMap's Comparator,
     * or by the keys' natural ordering if the TreeMap has no
     * Comparator).
     */
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        // Write out the Comparator and any hidden stuff
        s.defaultWriteObject();
        
        // Write out size (number of Mappings)
        s.writeInt(size);
        
        // Write out keys and values (alternating)
        for(Map.Entry<K, V> e : entrySet()) {
            s.writeObject(e.getKey());
            s.writeObject(e.getValue());
        }
    }
    
    /**
     * Reconstitute the {@code TreeMap} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(final java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        // Read in the Comparator and any hidden stuff
        s.defaultReadObject();
        
        // Read in size
        int size = s.readInt();
        
        buildFromSorted(size, null, s, null);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 红黑树 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Balancing operations.
     *
     * Implementations of rebalancings during insertion and deletion are
     * slightly different than the CLR version.  Rather than using dummy
     * nilnodes, we use a set of accessors that deal properly with null.  They
     * are used to avoid messiness surrounding nullness checks in the main
     * algorithms.
     */
    
    /**
     * Delete node p, and then rebalance the tree.
     */
    // 将元素从红黑树中移除
    private void deleteEntry(Entry<K, V> p) {
        modCount++;
        size--;
        
        // If strictly internal, copy successor's element to p and then make p point to successor.
        if(p.left != null && p.right != null) {
            Entry<K, V> s = successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        } // p has 2 children
        
        // Start fixup at replacement node, if it exists.
        Entry<K, V> replacement = (p.left != null ? p.left : p.right);
        
        if(replacement != null) {
            // Link replacement to parent
            replacement.parent = p.parent;
            if(p.parent == null) {
                root = replacement;
            } else if(p == p.parent.left) {
                p.parent.left = replacement;
            } else {
                p.parent.right = replacement;
            }
            
            // Null out links so they are OK to use by fixAfterDeletion.
            p.left = p.right = p.parent = null;
            
            // Fix replacement
            if(p.color == BLACK) {
                fixAfterDeletion(replacement);
            }
        } else if(p.parent == null) { // return if we are the only node.
            root = null;
        } else { //  No children. Use self as phantom replacement and unlink.
            if(p.color == BLACK) {
                fixAfterDeletion(p);
            }
            
            if(p.parent != null) {
                if(p == p.parent.left) {
                    p.parent.left = null;
                } else if(p == p.parent.right) {
                    p.parent.right = null;
                }
                p.parent = null;
            }
        }
    }
    
    /** From CLR */
    // 将元素x插入到红黑树后，可能会破坏其平衡性，所以这里需要做出调整，保持红黑树的平衡
    private void fixAfterInsertion(Entry<K, V> x) {
        x.color = RED;
        
        while(x != null && x != root && x.parent.color == RED) {
            if(parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Entry<K, V> y = rightOf(parentOf(parentOf(x)));
                if(colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if(x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Entry<K, V> y = leftOf(parentOf(parentOf(x)));
                if(colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if(x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }
    
    /** From CLR */
    // 将元素x从红黑树移除后，可能会破坏其平衡性，所以这里需要做出调整，保持红黑树的平衡
    private void fixAfterDeletion(Entry<K, V> x) {
        while(x != root && colorOf(x) == BLACK) {
            if(x == leftOf(parentOf(x))) {
                Entry<K, V> sib = rightOf(parentOf(x));
                
                if(colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }
                
                if(colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if(colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else { // symmetric
                Entry<K, V> sib = leftOf(parentOf(x));
                
                if(colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }
                
                if(colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if(colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }
        
        setColor(x, BLACK);
    }
    
    /** From CLR */
    // 左旋
    private void rotateLeft(Entry<K, V> p) {
        if(p != null) {
            Entry<K, V> r = p.right;
            p.right = r.left;
            if(r.left != null) {
                r.left.parent = p;
            }
            r.parent = p.parent;
            if(p.parent == null) {
                root = r;
            } else if(p.parent.left == p) {
                p.parent.left = r;
            } else {
                p.parent.right = r;
            }
            r.left = p;
            p.parent = r;
        }
    }
    
    /** From CLR */
    // 右旋
    private void rotateRight(Entry<K, V> p) {
        if(p != null) {
            Entry<K, V> l = p.left;
            p.left = l.right;
            if(l.right != null) {
                l.right.parent = p;
            }
            l.parent = p.parent;
            if(p.parent == null) {
                root = l;
            } else if(p.parent.right == p) {
                p.parent.right = l;
            } else {
                p.parent.left = l;
            }
            l.right = p;
            p.parent = l;
        }
    }
    
    /**
     * Returns the first Entry in the TreeMap (according to the TreeMap's
     * key-sort function).  Returns null if the TreeMap is empty.
     */
    // 返回遍历当前Map时的首个结点
    final Entry<K, V> getFirstEntry() {
        Entry<K, V> p = root;
        if(p != null) {
            while(p.left != null) {
                p = p.left;
            }
        }
        return p;
    }
    
    /**
     * Returns the last Entry in the TreeMap (according to the TreeMap's
     * key-sort function).  Returns null if the TreeMap is empty.
     */
    // 返回遍历当前Map时的末尾结点
    final Entry<K, V> getLastEntry() {
        Entry<K, V> p = root;
        if(p != null) {
            while(p.right != null) {
                p = p.right;
            }
        }
        return p;
    }
    
    /**
     * Returns the predecessor of the specified Entry, or null if no such.
     */
    // 返回遍历当前Map时结点t的前驱
    static <K, V> Entry<K, V> predecessor(Entry<K, V> t) {
        if(t == null) {
            return null;
        }
        
        if(t.left != null) {
            Entry<K, V> p = t.left;
            while(p.right != null) {
                p = p.right;
            }
            return p;
        } else {
            Entry<K, V> p = t.parent;
            Entry<K, V> ch = t;
            while(p != null && ch == p.left) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }
    
    /**
     * Returns the successor of the specified Entry, or null if no such.
     */
    // 返回遍历当前Map时结点t的后继
    static <K, V> TreeMap.Entry<K, V> successor(Entry<K, V> t) {
        if(t == null) {
            return null;
        }
        
        if(t.right != null) {
            Entry<K, V> p = t.right;
            while(p.left != null) {
                p = p.left;
            }
            return p;
        } else {
            Entry<K, V> p = t.parent;
            Entry<K, V> ch = t;
            while(p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }
    
    /** Intended to be called only from TreeSet.readObject */
    void readTreeSet(int size, ObjectInputStream s, V defaultVal) throws IOException, ClassNotFoundException {
        buildFromSorted(size, null, s, defaultVal);
    }
    
    /** Intended to be called only from TreeSet.addAll */
    void addAllForTreeSet(SortedSet<? extends K> set, V defaultVal) {
        try {
            buildFromSorted(set.size(), set.iterator(), null, defaultVal);
        } catch(IOException | ClassNotFoundException cannotHappen) {
        }
    }
    
    /**
     * Linear time tree building algorithm from sorted data.  Can accept keys
     * and/or values from iterator or stream. This leads to too many
     * parameters, but seems better than alternatives.  The four formats
     * that this method accepts are:
     *
     * 1) An iterator of Map.Entries.  (it != null, defaultVal == null).
     * 2) An iterator of keys.         (it != null, defaultVal != null).
     * 3) A stream of alternating serialized keys and values.
     * (it == null, defaultVal == null).
     * 4) A stream of serialized keys. (it == null, defaultVal != null).
     *
     * It is assumed that the comparator of the TreeMap is already set prior
     * to calling this method.
     *
     * @param size       the number of keys (or key-value pairs) to be read from
     *                   the iterator or stream
     * @param it         If non-null, new entries are created from entries
     *                   or keys read from this iterator.
     * @param str        If non-null, new entries are created from keys and
     *                   possibly values read from this stream in serialized form.
     *                   Exactly one of it and str should be non-null.
     * @param defaultVal if non-null, this default value is used for
     *                   each value in the map.  If null, each value is read from
     *                   iterator or stream, as described above.
     *
     * @throws java.io.IOException    propagated from stream reads. This cannot
     *                                occur if str is null.
     * @throws ClassNotFoundException propagated from readObject.
     *                                This cannot occur if str is null.
     */
    private void buildFromSorted(int size, Iterator<?> it, ObjectInputStream str, V defaultVal) throws IOException, ClassNotFoundException {
        this.size = size;
        root = buildFromSorted(0, 0, size - 1, computeRedLevel(size), it, str, defaultVal);
    }
    
    /**
     * Recursive "helper method" that does the real work of the
     * previous method.  Identically named parameters have
     * identical definitions.  Additional parameters are documented below.
     * It is assumed that the comparator and size fields of the TreeMap are
     * already set prior to calling this method.  (It ignores both fields.)
     *
     * @param level    the current level of tree. Initial call should be 0.
     * @param lo       the first element index of this subtree. Initial should be 0.
     * @param hi       the last element index of this subtree.  Initial should be size-1.
     * @param redLevel the level at which nodes should be red.
     *                 Must be equal to computeRedLevel for tree of this size.
     */
    @SuppressWarnings("unchecked")
    private final Entry<K, V> buildFromSorted(int level, int lo, int hi, int redLevel, Iterator<?> it, ObjectInputStream str, V defaultVal) throws IOException, ClassNotFoundException {
        
        /*
         * Strategy: The root is the middlemost element. To get to it, we
         * have to first recursively construct the entire left subtree,
         * so as to grab all of its elements. We can then proceed with right
         * subtree.
         *
         * The lo and hi arguments are the minimum and maximum
         * indices to pull out of the iterator or stream for current subtree.
         * They are not actually indexed, we just proceed sequentially,
         * ensuring that items are extracted in corresponding order.
         */
        
        if(hi<lo) {
            return null;
        }
        
        int mid = (lo + hi) >>> 1;
        
        Entry<K, V> left = null;
        if(lo<mid) {
            left = buildFromSorted(level + 1, lo, mid - 1, redLevel, it, str, defaultVal);
        }
        
        // extract key and/or value from iterator or stream
        K key;
        V value;
        if(it != null) {
            if(defaultVal == null) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
                key = (K) entry.getKey();
                value = (V) entry.getValue();
            } else {
                key = (K) it.next();
                value = defaultVal;
            }
        } else { // use stream
            key = (K) str.readObject();
            value = (defaultVal != null ? defaultVal : (V) str.readObject());
        }
        
        Entry<K, V> middle = new Entry<>(key, value, null);
        
        // color nodes in non-full bottommost level red
        if(level == redLevel) {
            middle.color = RED;
        }
        
        if(left != null) {
            middle.left = left;
            left.parent = middle;
        }
        
        if(mid<hi) {
            Entry<K, V> right = buildFromSorted(level + 1, mid + 1, hi, redLevel, it, str, defaultVal);
            middle.right = right;
            right.parent = middle;
        }
        
        return middle;
    }
    
    /**
     * Finds the level down to which to assign all nodes BLACK.
     * This is the last `full' level of the complete binary tree produced by buildTree.
     * The remaining nodes are colored RED. (This makes a `nice' set of color assignments wrt future insertions.)
     * This level number is computed by finding the number of splits needed to reach the zeroeth node.
     *
     * @param size the (non-negative) number of keys in the tree to be built
     */
    private static int computeRedLevel(int size) {
        return 31 - Integer.numberOfLeadingZeros(size + 1);
    }
    
    // 设置结点颜色
    private static <K, V> void setColor(Entry<K, V> p, boolean c) {
        if(p != null) {
            p.color = c;
        }
    }
    
    // 判断结点是否为黑色
    private static <K, V> boolean colorOf(Entry<K, V> p) {
        return (p == null ? BLACK : p.color);
    }
    
    // 返回父结点
    private static <K, V> Entry<K, V> parentOf(Entry<K, V> p) {
        return (p == null ? null : p.parent);
    }
    
    // 返回左孩子
    private static <K, V> Entry<K, V> leftOf(Entry<K, V> p) {
        return (p == null) ? null : p.left;
    }
    
    // 返回右孩子
    private static <K, V> Entry<K, V> rightOf(Entry<K, V> p) {
        return (p == null) ? null : p.right;
    }
    
    /*▲ 红黑树 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /**
     * Returns this map's entry for the given key, or {@code null} if the map
     * does not contain an entry for the key.
     *
     * @return this map's entry for the given key, or {@code null} if the map
     * does not contain an entry for the key
     *
     * @throws ClassCastException   if the specified key cannot be compared
     *                              with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *                              and this map uses natural ordering, or its comparator
     *                              does not permit null keys
     */
    // 查找key对应的元素
    final Entry<K, V> getEntry(Object key) {
        // Offload comparator-based version for sake of performance
        if(comparator != null) {
            return getEntryUsingComparator(key);
        }
        
        if(key == null) {
            throw new NullPointerException();
        }
        
        /* 依赖内部比较器Comparable来查找key对应的元素 */
        
        @SuppressWarnings("unchecked")
        Comparable<? super K> k = (Comparable<? super K>) key;
        Entry<K, V> p = root;
        while(p != null) {
            int cmp = k.compareTo(p.key);
            if(cmp<0) {
                p = p.left;
            } else if(cmp>0) {
                p = p.right;
            } else {
                return p;
            }
        }
        return null;
    }
    
    /**
     * Version of getEntry using comparator. Split off from getEntry
     * for performance. (This is not worth doing for most methods,
     * that are less dependent on comparator performance, but is
     * worthwhile here.)
     */
    // 依赖外部比较器Comparator来查找key对应的元素
    final Entry<K, V> getEntryUsingComparator(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        Comparator<? super K> cpr = comparator;
        if(cpr != null) {
            Entry<K, V> p = root;
            while(p != null) {
                int cmp = cpr.compare(k, p.key);
                if(cmp<0) {
                    p = p.left;
                } else if(cmp>0) {
                    p = p.right;
                } else {
                    return p;
                }
            }
        }
        return null;
    }
    
    /**
     * Returns the entry for the greatest key less than the specified key;
     * if no such entry exists (i.e., the least key in the Tree is greater than the specified key), returns {@code null}.
     */
    // [< key] 获取尽量靠右侧的结点，该结点包含的targetKey满足：targetKey< key（这里的关系运算符标记的是元素遍历的先后关系）
    final Entry<K, V> getLowerEntry(K key) {
        Entry<K, V> p = root;
        while(p != null) {
            int cmp = compare(key, p.key);
            if(cmp>0) {
                if(p.right != null) {
                    p = p.right;
                } else {
                    return p;
                }
            } else {
                if(p.left != null) {
                    p = p.left;
                } else {
                    Entry<K, V> parent = p.parent;
                    Entry<K, V> ch = p;
                    while(parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }
        return null;
    }
    
    /**
     * Gets the entry for the least key greater than the specified key;
     * if no such entry exists, returns the entry for the least key greater than the specified key;
     * if no such entry exists returns {@code null}.
     */
    // [> key] 获取尽量靠左侧的结点，该结点包含的targetKey满足：targetKey> key（这里的关系运算符标记的是元素遍历的先后关系）
    final Entry<K, V> getHigherEntry(K key) {
        Entry<K, V> p = root;
        while(p != null) {
            int cmp = compare(key, p.key);
            if(cmp<0) {
                if(p.left != null) {
                    p = p.left;
                } else {
                    return p;
                }
            } else {
                if(p.right != null) {
                    p = p.right;
                } else {
                    Entry<K, V> parent = p.parent;
                    Entry<K, V> ch = p;
                    while(parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }
        return null;
    }
    
    /**
     * Gets the entry corresponding to the specified key;
     * if no such entry exists, returns the entry for the greatest key less than the specified key;
     * if no such entry exists, returns {@code null}.
     */
    // [<=key] 获取尽量靠右侧的结点，该结点包含的targetKey满足：targetKey<=key（这里的关系运算符标记的是元素遍历的先后关系）
    final Entry<K, V> getFloorEntry(K key) {
        Entry<K, V> p = root;
        while(p != null) {
            int cmp = compare(key, p.key);
            if(cmp>0) {
                if(p.right != null) {
                    p = p.right;
                } else {
                    return p;
                }
            } else if(cmp<0) {
                if(p.left != null) {
                    p = p.left;
                } else {
                    Entry<K, V> parent = p.parent;
                    Entry<K, V> ch = p;
                    while(parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            } else {
                return p;
            }
            
        }
        return null;
    }
    
    /**
     * Gets the entry corresponding to the specified key;
     * if no such entry exists, returns the entry for the least key greater than the specified key;
     * if no such entry exists (i.e., the greatest key in the Tree is less than the specified key), returns {@code null}.
     */
    // [>=key] 获取尽量靠左侧的结点，该结点包含的targetKey满足：targetKey>=key（这里的关系运算符标记的是元素遍历的先后关系）
    final Entry<K, V> getCeilingEntry(K key) {
        Entry<K, V> p = root;
        
        while(p != null) {
            int cmp = compare(key, p.key);
            if(cmp<0) {
                if(p.left != null) {
                    p = p.left;
                } else {
                    return p;
                }
            } else if(cmp>0) {
                if(p.right != null) {
                    p = p.right;
                } else {
                    Entry<K, V> parent = p.parent;
                    Entry<K, V> ch = p;
                    while(parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            } else {
                return p;
            }
        }
        return null;
    }
    
    /*▲ 查找 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    
    /*▼ Little utilities ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    /**
     * Test two values for equality.  Differs from o1.equals(o2) only in
     * that it copes with {@code null} o1 properly.
     */
    // 比较两个值是否相等
    static final boolean valEquals(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }
    
    /**
     * Return SimpleImmutableEntry for entry, or null if null
     */
    // 将e包装为只读版本的Entry
    static <K, V> Map.Entry<K, V> exportEntry(TreeMap.Entry<K, V> e) {
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<>(e);
    }
    
    /**
     * Return key for entry, or null if null
     */
    // 如果指定的结点不为null，获取其中的key，否则返回null
    static <K, V> K keyOrNull(TreeMap.Entry<K, V> e) {
        return (e == null) ? null : e.key;
    }
    
    /**
     * Returns the key corresponding to the specified Entry.
     *
     * @throws NoSuchElementException if the Entry is null
     */
    // 如果指定的结点不为null，获取其中的key，否则抛异常
    static <K> K key(Entry<K, ?> e) {
        if(e == null) {
            throw new NoSuchElementException();
        }
        return e.key;
    }
    
    /**
     * Compares two keys using the correct comparison method for this TreeMap.
     */
    // 比较k1和k2，优先使用外部比较器comparator
    @SuppressWarnings("unchecked")
    final int compare(Object k1, Object k2) {
        return comparator == null
            ? ((Comparable<? super K>) k1).compareTo((K) k2)
            : comparator.compare((K) k1, (K) k2);
    }
    
    /*▲ Little utilities ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    
    /**
     * Returns a shallow copy of this {@code TreeMap} instance. (The keys and
     * values themselves are not cloned.)
     *
     * @return a shallow copy of this map
     */
    public Object clone() {
        TreeMap<?, ?> clone;
        try {
            clone = (TreeMap<?, ?>) super.clone();
        } catch(CloneNotSupportedException e) {
            throw new InternalError(e);
        }
        
        // Put clone into "virgin" state (except for comparator)
        clone.root = null;
        clone.size = 0;
        clone.modCount = 0;
        clone.entrySet = null;
        clone.navigableKeySet = null;
        clone.descendingMap = null;
        
        // Initialize clone with our mappings
        try {
            clone.buildFromSorted(size, entrySet().iterator(), null, null);
        } catch(java.io.IOException | ClassNotFoundException cannotHappen) {
        }
        
        return clone;
    }
    
    
    
    // 当前Map中key的Iterator
    Iterator<K> keyIterator() {
        // 返回遍历当前Map时的首个结点，作为遍历的起点
        Entry<K, V> firstEntry = getFirstEntry();
        return new KeyIterator(firstEntry);
    }
    
    // 【逆序】Map中key的Iterator
    Iterator<K> descendingKeyIterator() {
        // 返回遍历当前Map时的末尾结点，作为遍历的起点
        Entry<K, V> lastEntry = getLastEntry();
        return new DescendingKeyIterator(lastEntry);
    }
    
    
    
    // 当前Map中key的Spliterator
    final Spliterator<K> keySpliterator() {
        return new KeySpliterator<>(this, null, null, 0, -1, 0);
    }
    
    // 【逆序】Map中key的Spliterator
    final Spliterator<K> descendingKeySpliterator() {
        return new DescendingKeySpliterator<>(this, null, null, 0, -2, 0);
    }
    
    /**
     * Currently, we support Spliterator-based versions only for the
     * full map, in either plain of descending form, otherwise relying
     * on defaults because size estimation for submaps would dominate
     * costs. The type tests needed to check these for key views are
     * not very nice but avoid disrupting existing class
     * structures. Callers must use plain default spliterators if this
     * returns null.
     */
    // 参数中指定的key的Spliterator
    static <K> Spliterator<K> keySpliteratorFor(NavigableMap<K, ?> m) {
        if(m instanceof TreeMap) {
            @SuppressWarnings("unchecked")
            TreeMap<K, Object> t = (TreeMap<K, Object>) m;
            return t.keySpliterator();
        }
        
        if(m instanceof DescendingSubMap) {
            @SuppressWarnings("unchecked")
            DescendingSubMap<K, ?> dm = (DescendingSubMap<K, ?>) m;
            TreeMap<K, ?> tm = dm.treeMap;
            if(dm == tm.descendingMap) {
                @SuppressWarnings("unchecked")
                TreeMap<K, Object> t = (TreeMap<K, Object>) tm;
                return t.descendingKeySpliterator();
            }
        }
        
        @SuppressWarnings("unchecked")
        NavigableSubMap<K, ?> sm = (NavigableSubMap<K, ?>) m;
        
        return sm.keySpliterator();
    }
    
    
    
    
    
    
    /**
     * Node in the Tree.  Doubles as a means to pass key-value pairs back to
     * user (see Map.Entry).
     */
    // TreeMap中的红黑树结点
    static final class Entry<K, V> implements Map.Entry<K, V> {
        K key;
        V value;
        Entry<K, V> left;
        Entry<K, V> right;
        Entry<K, V> parent;
        boolean color = BLACK;
        
        /**
         * Make a new cell with given key, value, and parent, and with
         * {@code null} child links, and BLACK color.
         */
        Entry(K key, V value, Entry<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }
        
        /**
         * Returns the key.
         *
         * @return the key
         */
        public K getKey() {
            return key;
        }
        
        /**
         * Returns the value associated with the key.
         *
         * @return the value associated with the key
         */
        public V getValue() {
            return value;
        }
        
        /**
         * Replaces the value currently associated with the key with the given
         * value.
         *
         * @return the value associated with the key before this method was
         * called
         */
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        
        public boolean equals(Object o) {
            if(!(o instanceof Map.Entry)) {
                return false;
            }
            
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            
            return valEquals(key, e.getKey()) && valEquals(value, e.getValue());
        }
        
        public int hashCode() {
            int keyHash = (key == null ? 0 : key.hashCode());
            int valueHash = (value == null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }
        
        public String toString() {
            return key + "=" + value;
        }
    }
    
    
    
    /**
     * Unlike Values and EntrySet, the KeySet class is static,
     * delegating to a NavigableMap to allow use by SubMaps,
     * which outweighs the ugliness of needing type-tests for the following
     * Iterator methods that are defined appropriately in main versus submap classes.
     */
    // key的集合（TreeMap和NavigableSubMap共用）
    static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final NavigableMap<E, ?> m; // 直接包装Map
        
        KeySet(NavigableMap<E, ?> map) {
            m = map;
        }
        
        
        
        /*▼ NavigableSet ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 返回Map使用的外部比较器
        public Comparator<? super E> comparator() {
            return m.comparator();
        }
        
        
        // 返回遍历当前Map时的首个结点的key
        public E first() {
            return m.firstKey();
        }
        
        // 返回遍历当前Map时的末尾结点的key
        public E last() {
            return m.lastKey();
        }
        
        // 〖前驱〗获取一个key，该key是遍历当前Map时形参e的前驱
        public E lower(E e) {
            return m.lowerKey(e);
        }
        
        // 〖后继〗获取一个key，该key是遍历当前Map时形参e的后继
        public E higher(E e) {
            return m.higherKey(e);
        }
        
        // 【前驱】获取一个key，该key是遍历当前Map时形参e的前驱（包括e本身）
        public E floor(E e) {
            return m.floorKey(e);
        }
        
        // 【后继】获取一个key，该key是遍历当前Map时形参e的后继（包括e本身）
        public E ceiling(E e) {
            return m.ceilingKey(e);
        }
        
        // 移除遍历当前Map时的首个结点，并返回其包含的key
        public E pollFirst() {
            Map.Entry<E, ?> e = m.pollFirstEntry();
            return (e == null) ? null : e.getKey();
        }
        
        // 移除遍历当前Map时的末尾结点，并返回其包含的key
        public E pollLast() {
            Map.Entry<E, ?> e = m.pollLastEntry();
            return (e == null) ? null : e.getKey();
        }
        
        
        // 获取[fromElement, toElement)范围内的Set
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }
        
        // 获取〖fromElement, toElement〗范围内的Set，是否为闭区间由fromInclusive和toInclusive参数决定
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            // 获取【理论区间】为〖fromKey, toKey〗的SubMap，区间下限/上限是否为闭区间由fromInclusive和toInclusive参数决定
            NavigableMap<E, ?> subMap = m.subMap(fromElement, fromInclusive, toElement, toInclusive);
            return new KeySet<>(subMap);
        }
        
        // 获取【理论区间】上限为toElement(不包含)的Set
        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }
        
        // 获取【理论区间】上限为toElement的Set，区间上限是否为闭区间由inclusive参数决定
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            // 获取【理论区间】上限为toElement的SubMap，区间上限是否为闭区间由inclusive参数决定
            NavigableMap<E, ?> headMap = m.headMap(toElement, inclusive);
            return new KeySet<>(headMap);
        }
        
        // 获取【理论区间】下限为fromElement(包含)的Set
        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
        
        // 获取【理论区间】下限为fromElement的Set，区间下限是否为闭区间由inclusive参数决定
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            // 获取【理论区间】下限为fromElement的SubMap，区间下限是否为闭区间由inclusive参数决定
            NavigableMap<E, ?> tailMap = m.tailMap(fromElement, inclusive);
            return new KeySet<>(tailMap);
        }
        
        
        // 获取【逆序】Set
        public NavigableSet<E> descendingSet() {
            NavigableMap<E, ?> map = m.descendingMap();
            return new KeySet<>(map);
        }
        
        
        // 返回当前Set的Iterator
        public Iterator<E> iterator() {
            if(m instanceof TreeMap) {
                return ((TreeMap<E, ?>) m).keyIterator();
            } else {
                return ((NavigableSubMap<E, ?>) m).keyIterator();
            }
        }
        
        // 返回【逆序】Set的Iterator
        public Iterator<E> descendingIterator() {
            if(m instanceof TreeMap) {
                return ((TreeMap<E, ?>) m).descendingKeyIterator();
            } else {
                return ((NavigableSubMap<E, ?>) m).descendingKeyIterator();
            }
        }
        
        
        // 返回当前Set的Spliterator
        public Spliterator<E> spliterator() {
            return keySpliteratorFor(m);
        }
        
        /*▲ NavigableSet ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        public int size() {
            return m.size();
        }
        
        public boolean isEmpty() {
            return m.isEmpty();
        }
        
        public boolean contains(Object key) {
            return m.containsKey(key);
        }
        
        public boolean remove(Object key) {
            int oldSize = size();
            m.remove(key);
            return size() != oldSize;
        }
        
        public void clear() {
            m.clear();
        }
        
    }
    
    // TreeMap中value的集合
    class Values extends AbstractCollection<V> {
        
        public int size() {
            return TreeMap.this.size();
        }
        
        public boolean contains(Object value) {
            return TreeMap.this.containsValue(value);
        }
        
        public boolean remove(Object value) {
            for(Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
                if(valEquals(e.getValue(), value)) {
                    deleteEntry(e);
                    return true;
                }
            }
            
            return false;
        }
        
        public void clear() {
            TreeMap.this.clear();
        }
        
        public Iterator<V> iterator() {
            return new ValueIterator(getFirstEntry());
        }
        
        public Spliterator<V> spliterator() {
            return new ValueSpliterator<>(TreeMap.this, null, null, 0, -1, 0);
        }
    }
    
    // TreeMap中key-value对的集合
    class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        
        public int size() {
            return TreeMap.this.size();
        }
        
        public boolean contains(Object e) {
            if(!(e instanceof Map.Entry)) {
                return false;
            }
            
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) e;
            
            Object value = entry.getValue();
            
            // 查找key对应的元素
            Entry<K, V> p = getEntry(entry.getKey());
            
            return p != null && valEquals(p.getValue(), value);
        }
        
        public boolean remove(Object e) {
            if(!(e instanceof Map.Entry)) {
                return false;
            }
            
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) e;
            
            Object value = entry.getValue();
            
            // 查找key对应的元素
            Entry<K, V> p = getEntry(entry.getKey());
            
            if(p != null && valEquals(p.getValue(), value)) {
                deleteEntry(p);
                return true;
            }
            
            return false;
        }
        
        public void clear() {
            TreeMap.this.clear();
        }
        
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator(getFirstEntry());
        }
        
        public Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator<>(TreeMap.this, null, null, 0, -1, 0);
        }
    }
    
    
    
    /**
     * Base class for TreeMap Iterators
     */
    // TreeMap中迭代器的基础实现
    abstract class PrivateEntryIterator<T> implements Iterator<T> {
        Entry<K, V> lastReturned;   // 上次遍历的结点
        Entry<K, V> next;           // 下次/即将遍历的结点
        int expectedModCount;
        
        PrivateEntryIterator(Entry<K, V> first) {
            expectedModCount = modCount;
            lastReturned = null;
            next = first;
        }
        
        // 是否包含下一个未遍历元素
        public final boolean hasNext() {
            return next != null;
        }
        
        // 移除上一个遍历的元素
        public void remove() {
            if(lastReturned == null) {
                throw new IllegalStateException();
            }
            
            if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            // deleted entries are replaced by their successors
            if(lastReturned.left != null && lastReturned.right != null) {
                /*
                 * 这里是正序遍历，删除待删除元素lastReturned对next会造成影响
                 * 此种情形下，lastReturned会由其后继取代，所以这里让next直接指向lastReturned（最终指向了后继）
                 */
                next = lastReturned;
            }
            
            deleteEntry(lastReturned);
            
            expectedModCount = modCount;
            
            lastReturned = null;
        }
        
        // 前驱
        final Entry<K, V> prevEntry() {
            Entry<K, V> e = next;
            if(e == null) {
                throw new NoSuchElementException();
            }
            
            if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            next = predecessor(e);
            lastReturned = e;
            return e;
        }
        
        // 后继
        final Entry<K, V> nextEntry() {
            Entry<K, V> e = next;
            if(e == null) {
                throw new NoSuchElementException();
            }
            
            if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            next = successor(e);
            lastReturned = e;
            return e;
        }
    }
    
    // TreeMap中key的迭代器
    final class KeyIterator extends PrivateEntryIterator<K> {
        KeyIterator(Entry<K, V> first) {
            super(first);
        }
        
        public K next() {
            return nextEntry().key;
        }
    }
    
    // TreeMap中value的迭代器
    final class ValueIterator extends PrivateEntryIterator<V> {
        ValueIterator(Entry<K, V> first) {
            super(first);
        }
        
        public V next() {
            return nextEntry().value;
        }
    }
    
    // TreeMap中key-value的迭代器
    final class EntryIterator extends PrivateEntryIterator<Map.Entry<K, V>> {
        EntryIterator(Entry<K, V> first) {
            super(first);
        }
        
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }
    
    // 【逆序】TreeMap中key的迭代器
    final class DescendingKeyIterator extends PrivateEntryIterator<K> {
        DescendingKeyIterator(Entry<K, V> first) {
            super(first);
        }
        
        public K next() {
            return prevEntry().key;
        }
        
        public void remove() {
            if(lastReturned == null) {
                throw new IllegalStateException();
            }
            
            if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            deleteEntry(lastReturned);
            lastReturned = null;
            expectedModCount = modCount;
        }
    }
    
    
    
    /**
     * Base class for spliterators.  Iteration starts at a given
     * origin and continues up to but not including a given fence (or
     * null for end).  At top-level, for ascending cases, the first
     * split uses the root as left-fence/right-origin. From there,
     * right-hand splits replace the current fence with its left
     * child, also serving as origin for the split-off spliterator.
     * Left-hands are symmetric. Descending versions place the origin
     * at the end and invert ascending split rules.  This base class
     * is non-committal about directionality, or whether the top-level
     * spliterator covers the whole tree. This means that the actual
     * split mechanics are located in subclasses. Some of the subclass
     * trySplit methods are identical (except for return types), but
     * not nicely factorable.
     *
     * Currently, subclass versions exist only for the full map
     * (including descending keys via its descendingMap).  Others are
     * possible but currently not worthwhile because submaps require
     * O(n) computations to determine size, which substantially limits
     * potential speed-ups of using custom Spliterators versus default
     * mechanics.
     *
     * To boostrap initialization, external constructors use
     * negative size estimates: -1 for ascend, -2 for descend.
     */
    // TreeMap的可分割迭代器的基础实现
    static class TreeMapSpliterator<K, V> {
        final TreeMap<K, V> tree;
        TreeMap.Entry<K, V> current; // traverser; initially first node in range
        TreeMap.Entry<K, V> fence;   // one past last, or null
        int side;                   // 0: top, -1: is a left split, +1: right
        int est;                    // size estimate (exact only for top-level)
        int expectedModCount;       // for CME checks
        
        TreeMapSpliterator(TreeMap<K, V> tree, TreeMap.Entry<K, V> origin, TreeMap.Entry<K, V> fence, int side, int est, int expectedModCount) {
            this.tree = tree;
            this.current = origin;
            this.fence = fence;
            this.side = side;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }
        
        public final long estimateSize() {
            return (long) getEstimate();
        }
        
        final int getEstimate() { // force initialization
            int s;
            TreeMap<K, V> t;
            if((s = est)<0) {
                if((t = tree) != null) {
                    current = (s == -1) ? t.getFirstEntry() : t.getLastEntry();
                    s = est = t.size;
                    expectedModCount = t.modCount;
                } else {
                    s = est = 0;
                }
            }
            return s;
        }
    }
    
    // TreeMap中key的可分割迭代器
    static final class KeySpliterator<K, V> extends TreeMapSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(TreeMap<K, V> tree, TreeMap.Entry<K, V> origin, TreeMap.Entry<K, V> fence, int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }
        
        public final Comparator<? super K> getComparator() {
            return tree.comparator;
        }
        
        public KeySpliterator<K, V> trySplit() {
            if(est<0) {
                getEstimate(); // force initialization
            }
            int d = side;
            TreeMap.Entry<K, V> e = current, f = fence, s = ((e == null || e == f) ? null :      // empty
                (d == 0) ? tree.root : // was top
                    (d>0) ? e.right :   // was right
                        (d<0 && f != null) ? f.left :    // was left
                            null);
            if(s != null && s != e && s != f && tree.compare(e.key, s.key)<0) {        // e not already past s
                side = 1;
                return new KeySpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }
        
        public void forEachRemaining(Consumer<? super K> action) {
            if(action == null) {
                throw new NullPointerException();
            }
            if(est<0) {
                getEstimate(); // force initialization
            }
            TreeMap.Entry<K, V> f = fence, e, p, pl;
            if((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e.key);
                    if((p = e.right) != null) {
                        while((pl = p.left) != null) {
                            p = pl;
                        }
                    } else {
                        while((p = e.parent) != null && e == p.right) {
                            e = p;
                        }
                    }
                } while((e = p) != null && e != f);
                if(tree.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
        
        public boolean tryAdvance(Consumer<? super K> action) {
            TreeMap.Entry<K, V> e;
            if(action == null) {
                throw new NullPointerException();
            }
            if(est<0) {
                getEstimate(); // force initialization
            }
            if((e = current) == null || e == fence) {
                return false;
            }
            current = successor(e);
            action.accept(e.key);
            if(tree.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return true;
        }
        
        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
        }
        
    }
    
    // TreeMap中value的可分割迭代器
    static final class ValueSpliterator<K, V> extends TreeMapSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(TreeMap<K, V> tree, TreeMap.Entry<K, V> origin, TreeMap.Entry<K, V> fence, int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }
        
        public ValueSpliterator<K, V> trySplit() {
            if(est<0) {
                getEstimate(); // force initialization
            }
            
            int d = side;
            TreeMap.Entry<K, V> e = current, f = fence, s = ((e == null || e == f) ? null :      // empty
                (d == 0) ? tree.root : // was top
                    (d>0) ? e.right :   // was right
                        (d<0 && f != null) ? f.left :    // was left
                            null);
            if(s != null && s != e && s != f && tree.compare(e.key, s.key)<0) {        // e not already past s
                side = 1;
                return new ValueSpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }
        
        public void forEachRemaining(Consumer<? super V> action) {
            if(action == null) {
                throw new NullPointerException();
            }
            if(est<0) {
                getEstimate(); // force initialization
            }
            TreeMap.Entry<K, V> f = fence, e, p, pl;
            if((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e.value);
                    if((p = e.right) != null) {
                        while((pl = p.left) != null) {
                            p = pl;
                        }
                    } else {
                        while((p = e.parent) != null && e == p.right) {
                            e = p;
                        }
                    }
                } while((e = p) != null && e != f);
                if(tree.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
        
        public boolean tryAdvance(Consumer<? super V> action) {
            TreeMap.Entry<K, V> e;
            if(action == null) {
                throw new NullPointerException();
            }
            if(est<0) {
                getEstimate(); // force initialization
            }
            if((e = current) == null || e == fence) {
                return false;
            }
            current = successor(e);
            action.accept(e.value);
            if(tree.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return true;
        }
        
        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.ORDERED;
        }
    }
    
    // TreeMap中key-value的可分割迭代器
    static final class EntrySpliterator<K, V> extends TreeMapSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(TreeMap<K, V> tree, TreeMap.Entry<K, V> origin, TreeMap.Entry<K, V> fence, int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }
        
        public EntrySpliterator<K, V> trySplit() {
            if(est<0)
                getEstimate(); // force initialization
            int d = side;
            TreeMap.Entry<K, V> e = current, f = fence, s = ((e == null || e == f) ? null :      // empty
                (d == 0) ? tree.root : // was top
                    (d>0) ? e.right :   // was right
                        (d<0 && f != null) ? f.left :    // was left
                            null);
            if(s != null && s != e && s != f && tree.compare(e.key, s.key)<0) {        // e not already past s
                side = 1;
                return new EntrySpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }
        
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            if(action == null) {
                throw new NullPointerException();
            }
            if(est<0) {
                getEstimate(); // force initialization
            }
            TreeMap.Entry<K, V> f = fence, e, p, pl;
            if((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e);
                    if((p = e.right) != null) {
                        while((pl = p.left) != null) {
                            p = pl;
                        }
                    } else {
                        while((p = e.parent) != null && e == p.right) {
                            e = p;
                        }
                    }
                } while((e = p) != null && e != f);
                if(tree.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
        
        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
            TreeMap.Entry<K, V> e;
            if(action == null) {
                throw new NullPointerException();
            }
            
            if(est<0) {
                getEstimate(); // force initialization
            }
            
            if((e = current) == null || e == fence) {
                return false;
            }
            current = successor(e);
            action.accept(e);
            if(tree.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return true;
        }
        
        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
        }
        
        @Override
        public Comparator<Map.Entry<K, V>> getComparator() {
            // Adapt or create a key-based comparator
            if(tree.comparator != null) {
                return Map.Entry.comparingByKey(tree.comparator);
            } else {
                return (Comparator<Map.Entry<K, V>> & Serializable) (e1, e2) -> {
                    @SuppressWarnings("unchecked")
                    Comparable<? super K> k1 = (Comparable<? super K>) e1.getKey();
                    return k1.compareTo(e2.getKey());
                };
            }
        }
    }
    
    // 【逆序】TreeMap中key的可分割迭代器
    static final class DescendingKeySpliterator<K, V> extends TreeMapSpliterator<K, V> implements Spliterator<K> {
        DescendingKeySpliterator(TreeMap<K, V> tree, TreeMap.Entry<K, V> origin, TreeMap.Entry<K, V> fence, int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }
        
        public DescendingKeySpliterator<K, V> trySplit() {
            if(est<0) {
                getEstimate(); // force initialization
            }
            
            int d = side;
            TreeMap.Entry<K, V> e = current, f = fence, s = ((e == null || e == f) ? null :      // empty
                (d == 0) ? tree.root : // was top
                    (d<0) ? e.left :    // was left
                        (d>0 && f != null) ? f.right :   // was right
                            null);
            
            if(s != null && s != e && s != f && tree.compare(e.key, s.key)>0) {       // e not already past s
                side = 1;
                return new DescendingKeySpliterator<>(tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            
            return null;
        }
        
        public void forEachRemaining(Consumer<? super K> action) {
            if(action == null) {
                throw new NullPointerException();
            }
            
            if(est<0) {
                getEstimate(); // force initialization
            }
            
            TreeMap.Entry<K, V> f = fence, e, p, pr;
            if((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e.key);
                    if((p = e.left) != null) {
                        while((pr = p.right) != null) {
                            p = pr;
                        }
                    } else {
                        while((p = e.parent) != null && e == p.left) {
                            e = p;
                        }
                    }
                } while((e = p) != null && e != f);
                if(tree.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
        
        public boolean tryAdvance(Consumer<? super K> action) {
            TreeMap.Entry<K, V> e;
            if(action == null) {
                throw new NullPointerException();
            }
            
            if(est<0) {
                getEstimate(); // force initialization
            }
            
            if((e = current) == null || e == fence) {
                return false;
            }
            
            current = predecessor(e);
            action.accept(e.key);
            if(tree.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return true;
        }
        
        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.DISTINCT | Spliterator.ORDERED;
        }
    }
    
    
    
    /**
     * @serial include
     */
    // 限制区间范围的TreeMap（也称SubMap），分为正序和逆序两种
    abstract static class NavigableSubMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, Serializable {
        
        private static final long serialVersionUID = -2102997345730753016L;
        
        /*
         * 注：为了表述方便，这里约定一些术语：
         * 　　【理论区间】：由subLow和subHigh确定的区间，在构造器中设置
         * 【backing区间】：treeMap中key的区间，该区间的范围与【理论区间】的范围未定
         * 　　【可视区间】：由【理论区间】跟【可视区间】的交集组成
         * 　　【实际区间】：在【可视区间】上应用fromStart和toEnd参数后形成的区间
         *                 当fromStart==true 时，可理解为【实际区间】允许将【可视区间】下限继续外延，即将区间下限看做无穷
         *                 当toEnd    ==true 时，可理解为【实际区间】允许将【可视区间】上限继续外延，即将区间上限看做无穷
         *                 当fromStart==false时，【实际区间】的下限就是【可视区间】的下限
         *                 当toEnd    ==false时，【实际区间】的上限就是【可视区间】的上限
         */
        
        /**
         * The backing map.
         */
        final TreeMap<K, V> treeMap;    // 提供【backing区间】，由bakLow到bakHigh组成
        
        /**
         * Endpoints are represented as triples (fromStart, lo, loInclusive) and (toEnd, hi, hiInclusive).
         * If fromStart is true, then the low (absolute) bound is the start of the backing map, and the other values are ignored.
         * Otherwise, if loInclusive is true, lo is the inclusive bound, else lo is the exclusive bound.
         * Similarly for the upper bound.
         */
        final K subLow, subHigh;                // 【理论区间】的下限(lo)/上限(hi)
        final boolean loInclusive, hiInclusive; // 【理论区间】的下限/上限是否为闭区间
        final boolean fromStart, toEnd;         // 是否将【可视区间】下限/上限继续外延
        
        // Views
        transient NavigableMap<K, V> descendingMapView;
        transient EntrySetView entrySetView;
        
        // internal utilities
        transient KeySet<K> navigableKeySetView;
        
        
        
        /*▼ 构造器 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        NavigableSubMap(TreeMap<K, V> treeMap, boolean fromStart, K subLow, boolean loInclusive, boolean toEnd, K subHigh, boolean hiInclusive) {
            if(!fromStart && !toEnd) {
                // 起始值需小于终止值，否则抛异常
                if(treeMap.compare(subLow, subHigh)>0) {
                    throw new IllegalArgumentException("fromKey > toKey");
                }
            } else {
                if(!fromStart) {
                    // 这里使用compare起到了校验作用
                    treeMap.compare(subLow, subLow);
                }
                
                if(!toEnd) {
                    // 这里使用compare起到了校验作用
                    treeMap.compare(subHigh, subHigh);
                }
            }
            
            this.treeMap = treeMap;
            
            this.subLow = subLow;
            this.subHigh = subHigh;
            
            this.loInclusive = loInclusive;
            this.hiInclusive = hiInclusive;
            
            this.fromStart = fromStart;
            this.toEnd = toEnd;
        }
        
        /*▲ 构造器 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 存值 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 将指定的元素（key-value）存入Map，并返回旧值，允许覆盖
        public final V put(K key, V value) {
            // 存值之前需要先校验key，不在【实际区间】抛出异常
            if(!inRange(key)) {
                throw new IllegalArgumentException("key out of range");
            }
            
            return treeMap.put(key, value);
        }
        
        /*▲ 存值 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 取值 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 查找key对应的元素的值，如果不存在该元素，则返回null值
        public final V get(Object key) {
            // 取值之前需要先校验key，不在【实际区间】返回null
            if(!inRange(key)) {
                return null;
            }
            
            return treeMap.get(key);
        }
        
        /*▲ 取值 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 移除 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 查找key对应的元素，并移除该元素
        public final V remove(Object key) {
            // 移除之前需要先校验key，不在【实际区间】不移除
            if(!inRange(key)) {
                return null;
            }
            
            return treeMap.remove(key);
        }
        
        /*▲ 移除 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 包含查询 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 判断Map中是否存在指定key的元素
        public final boolean containsKey(Object key) {
            // 判断之前需要先校验key，不在可信范围返回false
            if(!inRange(key)) {
                return false;
            }
            
            return treeMap.containsKey(key);
        }
        
        /*▲ 包含查询 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 视图 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 获取当前Map中key的集合
        public final Set<K> keySet() {
            return navigableKeySet();
        }
        
        /*▲ 视图 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ NavigableMap ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 获取【backing区间】中尽量靠左侧的【可视】元素，并返回其key
        public final K firstKey() {
            return key(subLowest());
        }
        
        // 获取【backing区间】中尽量靠右侧的【可视】元素，并返回其key
        public final K lastKey() {
            return key(subHighest());
        }
        
        /*
         * 〖前驱〗
         * 获取【backing区间】中尽量靠右侧的，且其targetKey<key的【可视】元素，并返回其key
         * （这里的关系运算符标记的是元素遍历的先后关系）
         */
        public final K lowerKey(K key) {
            return keyOrNull(subLower(key));
        }
        
        /*
         * 〖后继〗
         * 获取【backing区间】中尽量靠左侧的，且其targetKey>key的【可视】元素
         * （这里的关系运算符标记的是元素遍历的先后关系）
         */
        public final K higherKey(K key) {
            return keyOrNull(subHigher(key));
        }
        
        /*
         * 【前驱】
         * 获取【backing区间】中尽量靠右侧的，且其targetKey<=key的【可视】元素
         * （这里的关系运算符标记的是元素遍历的先后关系）
         */
        public final K floorKey(K key) {
            return keyOrNull(subFloor(key));
        }
        
        /*
         * 【后继】
         * 获取【backing区间】中尽量靠左侧的，且其targetKey>=key的【可视】元素
         * （这里的关系运算符标记的是元素遍历的先后关系）
         */
        public final K ceilingKey(K key) {
            return keyOrNull(subCeiling(key));
        }
        
        /*
         * 〖前驱〗
         * 获取【backing区间】中尽量靠右侧的，且其targetKey<key的【可视】元素，然后将其包装为只读entry返回
         * （这里的关系运算符标记的是元素遍历的先后关系）
         */
        public final Map.Entry<K, V> lowerEntry(K key) {
            return exportEntry(subLower(key));
        }
        
        /*
         * 〖后继〗
         * 获取【backing区间】中尽量靠左侧的，且其targetKey>key的【可视】元素，然后将其包装为只读entry返回
         * （这里的关系运算符标记的是元素遍历的先后关系）
         */
        public final Map.Entry<K, V> higherEntry(K key) {
            return exportEntry(subHigher(key));
        }
        
        /*
         * 【前驱】
         * 获取【backing区间】中尽量靠右侧的，且其targetKey<=key的【可视】元素，然后将其包装为只读entry返回
         * （这里的关系运算符标记的是元素遍历的先后关系）
         */
        public final Map.Entry<K, V> floorEntry(K key) {
            return exportEntry(subFloor(key));
        }
        
        /*
         * 【后继】
         * 获取【backing区间】中尽量靠左侧的，且其targetKey>=key的【可视】元素，然后将其包装为只读entry返回
         * （这里的关系运算符标记的是元素遍历的先后关系）
         */
        public final Map.Entry<K, V> ceilingEntry(K key) {
            return exportEntry(subCeiling(key));
        }
        
        // 获取【backing区间】中尽量靠左侧的【可视】元素，然后将其包装为只读entry返回
        public final Map.Entry<K, V> firstEntry() {
            return exportEntry(subLowest());
        }
        
        // 获取【backing区间】中尽量靠右侧的【可视】元素，然后将其包装为只读entry返回
        public final Map.Entry<K, V> lastEntry() {
            return exportEntry(subHighest());
        }
        
        // 移除【backing区间】中尽量靠左侧的【可视】元素，并将其包装为只读版本的Entry后返回
        public final Map.Entry<K, V> pollFirstEntry() {
            // 获取【backing区间】中尽量靠左侧的【可视】元素
            TreeMap.Entry<K, V> e = subLowest();
            if(e != null) {
                treeMap.deleteEntry(e);
            }
            
            return exportEntry(e);
        }
        
        // 移除【backing区间】中尽量靠右侧的【可视】元素，并将其包装为只读版本的Entry后返回
        public final Map.Entry<K, V> pollLastEntry() {
            // 获取【backing区间】中尽量靠右侧的【可视】元素
            TreeMap.Entry<K, V> e = subHighest();
            if(e != null) {
                treeMap.deleteEntry(e);
            }
            
            return exportEntry(e);
        }
        
        
        /*
         * 获取【理论区间】为[fromKey, toKey)的SubMap
         * 最终调用AscendingSubMap/DescendingSubMap中的subMap()
         */
        public final SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }
        
        /*
         * 获取【理论区间】上限为toKey(不包含)的SubMap
         * 最终调用AscendingSubMap/DescendingSubMap中的headMap()
         */
        public final SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }
        
        /*
         * 获取【理论区间】下限为fromKey(包含)的SubMap
         * 最终调用AscendingSubMap/DescendingSubMap中的tailMap()
         */
        public final SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }
        
        
        // 获取当前SubMap中的key的集合
        public final NavigableSet<K> navigableKeySet() {
            KeySet<K> nksv = navigableKeySetView;
            return (nksv != null) ? nksv : (navigableKeySetView = new TreeMap.KeySet<>(this));
        }
        
        // 获取【逆序】SubMap中的key的集合
        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }
        
        /*▲ NavigableMap ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 杂项 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 判断当前SubMap是否为空集
        public boolean isEmpty() {
            return (fromStart && toEnd) ? treeMap.isEmpty() : entrySet().isEmpty();
        }
        
        // 返回当前SubMap中的元素数量
        public int size() {
            return (fromStart && toEnd) ? treeMap.size() : entrySet().size();
        }
        
        /*▲ 杂项 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 抽象方法 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 获取【backing区间】中尽量靠左侧的【可视】元素
        abstract TreeMap.Entry<K, V> subLowest();
        
        // 获取【backing区间】中尽量靠右侧的【可视】元素
        abstract TreeMap.Entry<K, V> subHighest();
        
        // 获取【backing区间】中尽量靠右侧的，且其targetKey<key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        abstract TreeMap.Entry<K, V> subLower(K key);
        
        // 获取【backing区间】中尽量靠左侧的，且其targetKey>key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        abstract TreeMap.Entry<K, V> subHigher(K key);
        
        // 获取【backing区间】中尽量靠右侧的，且其targetKey<=key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        abstract TreeMap.Entry<K, V> subFloor(K key);
        
        // 获取【backing区间】中尽量靠左侧的，且其targetKey>=key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        abstract TreeMap.Entry<K, V> subCeiling(K key);
        
        /** Returns ascending iterator from the perspective of this submap */
        // 返回当前SubMap的Iterator
        abstract Iterator<K> keyIterator();
        
        /** Returns descending iterator from the perspective of this submap */
        // 返回【逆序】SubMap的Iterator
        abstract Iterator<K> descendingKeyIterator();
        
        // 返回当前SubMap的Spliterator
        abstract Spliterator<K> keySpliterator();
        
        /*▲ 抽象方法 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        // 判断指定的key是否在【实际区间】左侧溢出
        final boolean tooLow(Object key) {
            if(fromStart) {
                return false;
            }
            
            int c = treeMap.compare(key, subLow);
            
            return c<0 || (c == 0 && !loInclusive);
        }
        
        // 判断指定的key是否在【实际区间】右侧溢出
        final boolean tooHigh(Object key) {
            if(toEnd) {
                return false;
            }
            
            int c = treeMap.compare(key, subHigh);
            
            return c>0 || (c == 0 && !hiInclusive);
        }
        
        // 子视图申请闭区间时，判断指定的key是否位于父视图的【实际区间】内[既没有上溢，也没有下溢]
        final boolean inRange(Object key) {
            return !tooLow(key) && !tooHigh(key);
        }
        
        // 子视图申请开区间时，判断指定的key是否位于父视图的【实际区间】内[既没有上溢，也没有下溢]
        final boolean inClosedRange(Object key) {
            return (fromStart || treeMap.compare(key, subLow) >= 0)
                && (toEnd || treeMap.compare(subHigh, key) >= 0);
        }
        
        // 判断指定的key是否位于父视图的【实际区间】内，inclusive指示子视图在申请闭区间还是开区间
        final boolean inRange(Object key, boolean inclusive) {
            return inclusive ? inRange(key) : inClosedRange(key);
        }
        
        // 获取【backing区间】中尽量靠左侧的【可视】元素
        final TreeMap.Entry<K, V> absLowest() {
            TreeMap.Entry<K, V> e;
            
            // 如果【实际区间】下限无穷，则直接获取【backing区间】的首个元素
            if(fromStart) {
                e = treeMap.getFirstEntry();
                
                // 如果【实际区间】下限有穷，则从【backing区间】获取满足条件的元素
            } else {
                e = loInclusive
                    // 【理论区间】的下限为闭区间
                    ? treeMap.getCeilingEntry(subLow)   // [>=subLow] 获取【backing区间】尽量靠左侧的结点，该结点包含的targetKey满足：targetKey>=subLow（这里的关系运算符标记的是元素遍历的先后关系）
                    // 【理论区间】的下限为开区间
                    : treeMap.getHigherEntry(subLow);   // [> subLow] 获取【backing区间】尽量靠左侧的结点，该结点包含的targetKey满足：targetKey> subLow（这里的关系运算符标记的是元素遍历的先后关系）
            }
            
            if(e == null){
                return null;
            }
            
            // 如果指定的key在【实际区间】右侧溢出，则返回null。否则，返回e
            return tooHigh(e.key) ? null : e;
        }
        
        // 获取【backing区间】中尽量靠右侧的【可视】元素
        final TreeMap.Entry<K, V> absHighest() {
            TreeMap.Entry<K, V> e;
            
            // 如果【实际区间】上限无穷，则直接获取【backing区间】的末尾元素
            if(toEnd) {
                e = treeMap.getLastEntry();
                
                // 如果【实际区间】上限有穷，则从【backing区间】获取满足条件的元素
            } else {
                e = hiInclusive
                    // 【理论区间】的上限为闭区间
                    ? treeMap.getFloorEntry(subHigh)    // [<=subHigh] 获取【backing区间】尽量靠右侧的结点，该结点包含的targetKey满足：targetKey<=subHigh（这里的关系运算符标记的是元素遍历的先后关系）
                    // 【理论区间】的上限为开区间
                    : treeMap.getLowerEntry(subHigh);   // [< subHigh] 获取【backing区间】尽量靠右侧的结点，该结点包含的targetKey满足：targetKey< subHigh（这里的关系运算符标记的是元素遍历的先后关系）
            }
            
            if(e == null){
                return null;
            }
            
            // 如果指定的key在【实际区间】左侧溢出，则返回null。否则，返回e
            return tooLow(e.key) ? null : e;
        }
        
        // 获取【backing区间】中尽量靠右侧的，且其targetKey<key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        final TreeMap.Entry<K, V> absLower(K key) {
            // 如果指定的key在【实际区间】右侧溢出
            if(tooHigh(key)) {
                // 获取【backing区间】中尽量靠右侧的【可视】元素
                return absHighest();
            }
            
            // [< key] 获取【backing区间】尽量靠右侧的结点，该结点包含的targetKey满足：targetKey< key（这里的关系运算符标记的是元素遍历的先后关系）
            TreeMap.Entry<K, V> e = treeMap.getLowerEntry(key);
            
            if(e == null){
                return null;
            }
            
            // 如果指定的key在【实际区间】左侧溢出，则返回null。否则，返回e
            return tooLow(e.key) ? null : e;
        }
        
        // 获取【backing区间】中尽量靠左侧的，且其targetKey>key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        final TreeMap.Entry<K, V> absHigher(K key) {
            // 如果指定的key在【实际区间】左侧溢出
            if(tooLow(key)) {
                // 获取【backing区间】中尽量靠左侧的【可视】元素
                return absLowest();
            }
            
            // [> key] 获取【backing区间】尽量靠左侧的结点，该结点包含的targetKey满足：targetKey> key（这里的关系运算符标记的是元素遍历的先后关系）
            TreeMap.Entry<K, V> e = treeMap.getHigherEntry(key);
            
            if(e == null){
                return null;
            }
            
            // 如果指定的key在【实际区间】右侧溢出，则返回null。否则，返回e
            return tooHigh(e.key) ? null : e;
        }
        
        // 获取【backing区间】中尽量靠右侧的，且其targetKey<=key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        final TreeMap.Entry<K, V> absFloor(K key) {
            // 如果指定的key在【实际区间】右侧溢出
            if(tooHigh(key)) {
                // 获取【实际区间】中尽量靠右侧的【可视】元素
                return absHighest();
            }
            
            // [<=key] 获取【backing区间】尽量靠右侧的结点，该结点包含的targetKey满足：targetKey<=key（这里的关系运算符标记的是元素遍历的先后关系）
            TreeMap.Entry<K, V> e = treeMap.getFloorEntry(key);
            
            if(e == null){
                return null;
            }
            
            // 如果指定的key在【实际区间】左侧溢出，则返回null。否则，返回e
            return tooLow(e.key) ? null : e;
        }
        
        // 获取【backing区间】中尽量靠左侧的，且其targetKey>=key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        final TreeMap.Entry<K, V> absCeiling(K key) {
            // 如果指定的key在【实际区间】左侧溢出
            if(tooLow(key)) {
                // 获取【实际区间】中尽量靠左侧的【可视】元素
                return absLowest();
            }
            
            // [>=key] 获取【backing区间】尽量靠左侧的结点，该结点包含的targetKey满足：targetKey>=key（这里的关系运算符标记的是元素遍历的先后关系）
            TreeMap.Entry<K, V> e = treeMap.getCeilingEntry(key);
            
            if(e == null){
                return null;
            }
            
            // 如果指定的key在【实际区间】右侧溢出，则返回null。否则，返回e
            return tooHigh(e.key) ? null : e;
        }
        
        /** Return the absolute low fence for descending traversal */
        // 在【backing区间】上获取【理论区间】的下限屏障（向右越过该屏障就会进入【理论区间】）
        final TreeMap.Entry<K, V> absLowFence() {
            // 【可视区间】下限外延
            if(fromStart) {
                return null;
            }
            
            return loInclusive
                // 【理论区间】的下限为闭区间
                ? treeMap.getLowerEntry(subLow)     // [< subLow] 获取【backing区间】尽量靠右侧的结点，该结点包含的targetKey满足：targetKey< subLow（这里的关系运算符标记的是元素遍历的先后关系）
                // 【理论区间】的下限为开区间
                : treeMap.getFloorEntry(subLow);    // [<=subLow] 获取【backing区间】尽量靠右侧的结点，该结点包含的targetKey满足：targetKey<=subLow（这里的关系运算符标记的是元素遍历的先后关系）
        }
        
        /** Returns the absolute high fence for ascending traversal */
        // 在【backing区间】上获取【理论区间】的上限屏障（向左越过该屏障就会进入【理论区间】）
        final TreeMap.Entry<K, V> absHighFence() {
            // 【可视区间】上限外延
            if(toEnd) {
                return null;
            }
            
            return hiInclusive
                // 【理论区间】的上限为闭区间
                ? treeMap.getHigherEntry(subHigh)   // [> subHigh] 获取【backing区间】尽量靠左侧的结点，该结点包含的targetKey满足：targetKey> subHigh（这里的关系运算符标记的是元素遍历的先后关系）
                // 【理论区间】的上限为开区间
                : treeMap.getCeilingEntry(subHigh); // [>=subHigh] 获取【backing区间】尽量靠左侧的结点，该结点包含的targetKey满足：targetKey>=subHigh（这里的关系运算符标记的是元素遍历的先后关系）
        }
        
        
        
        // SubMap中的key-value对
        abstract class EntrySetView extends AbstractSet<Map.Entry<K, V>> {
            private transient int size = -1, sizeModCount;
            
            public int size() {
                if(fromStart && toEnd) {
                    return treeMap.size();
                }
                
                if(size == -1 || sizeModCount != treeMap.modCount) {
                    sizeModCount = treeMap.modCount;
                    size = 0;
                    for(Entry<K, V> kvEntry : this) {
                        size++;
                    }
                }
                
                return size;
            }
            
            public boolean isEmpty() {
                TreeMap.Entry<K, V> n = absLowest();
                return n == null || tooHigh(n.key);
            }
            
            public boolean contains(Object e) {
                if(!(e instanceof Map.Entry)) {
                    return false;
                }
                
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) e;
                Object key = entry.getKey();
                if(!inRange(key)) {
                    return false;
                }
                
                // 查找key对应的元素
                TreeMap.Entry<?, ?> node = treeMap.getEntry(key);
                
                return node != null && valEquals(node.getValue(), entry.getValue());
            }
            
            public boolean remove(Object e) {
                if(!(e instanceof Map.Entry)) {
                    return false;
                }
                
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) e;
                Object key = entry.getKey();
                if(!inRange(key)) {
                    return false;
                }
                
                // 查找key对应的元素
                TreeMap.Entry<K, V> node = treeMap.getEntry(key);
                
                if(node != null && valEquals(node.getValue(), entry.getValue())) {
                    treeMap.deleteEntry(node);
                    return true;
                }
                
                return false;
            }
        }
        
        /**
         * Iterators for SubMaps
         */
        // SubMap迭代器的基础实现（SubMap是指从原有Map中截取的部分）
        abstract class SubMapIterator<T> implements Iterator<T> {
            final Object fenceKey;              // 迭代器可以遍历到的上限
            TreeMap.Entry<K, V> lastReturned;   // 上次遍历的结点
            TreeMap.Entry<K, V> next;           // 下次/即将遍历的结点
            int expectedModCount;   // 修改次数，防止迭代过程中TreeMap结构被改变
            
            SubMapIterator(TreeMap.Entry<K, V> first, TreeMap.Entry<K, V> fence) {
                expectedModCount = treeMap.modCount;
                lastReturned = null;
                next = first;
                fenceKey = fence == null ? UNBOUNDED : fence.key;
            }
            
            // 是否存在未访问元素
            public final boolean hasNext() {
                return next != null && next.key != fenceKey;
            }
            
            // 更新next为其后继
            final TreeMap.Entry<K, V> nextEntry() {
                TreeMap.Entry<K, V> e = next;
                
                if(e == null || e.key == fenceKey) {
                    throw new NoSuchElementException();
                }
                
                if(treeMap.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                
                // 返回结点t在中序遍历中的后继（遍历顺序的依据是key）
                next = successor(e);
                
                lastReturned = e;
                
                return e;
            }
            
            // 更新next为其前驱
            final TreeMap.Entry<K, V> prevEntry() {
                TreeMap.Entry<K, V> e = next;
                
                if(e == null || e.key == fenceKey) {
                    throw new NoSuchElementException();
                }
                
                if(treeMap.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                
                // 返回结点t在中序遍历中的前驱（遍历顺序的依据是key）
                next = predecessor(e);
                
                lastReturned = e;
                
                return e;
            }
            
            // （在正序遍历中）移除上次访问过的元素
            final void removeAscending() {
                if(lastReturned == null) {
                    throw new IllegalStateException();
                }
                
                if(treeMap.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                
                // deleted entries are replaced by their successors
                if(lastReturned.left != null && lastReturned.right != null) {
                    /*
                     * 这里是正序遍历，删除待删除元素lastReturned对next会造成影响
                     * 此种情形下，lastReturned会由其后继取代，所以这里让next直接指向lastReturned（最终指向了后继）
                     */
                    next = lastReturned;
                }
                
                // 将元素从红黑树中移除
                treeMap.deleteEntry(lastReturned);
                
                lastReturned = null;
                
                expectedModCount = treeMap.modCount;
            }
            
            // （在逆序遍历中）移除上次访问过的元素
            final void removeDescending() {
                if(lastReturned == null) {
                    throw new IllegalStateException();
                }
                
                if(treeMap.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                
                /*
                 * 这里是逆序遍历，所以，删除待删除元素lastReturned对next没影响
                 * （因为lastReturned会由其后继取代，但该后继已被遍历过了）
                 */
                
                // 将元素从红黑树中移除
                treeMap.deleteEntry(lastReturned);
                
                lastReturned = null;
                
                expectedModCount = treeMap.modCount;
            }
            
        }
        
        /** Implement minimal Spliterator as KeySpliterator backup */
        // SubMap中Key的迭代器，且支持可分割的迭代器接口
        final class SubMapKeyIterator extends SubMapIterator<K> implements Spliterator<K> {
            SubMapKeyIterator(TreeMap.Entry<K, V> first, TreeMap.Entry<K, V> fence) {
                super(first, fence);
            }
            
            public final Comparator<? super K> getComparator() {
                return NavigableSubMap.this.comparator();
            }
            
            // （在正序遍历中）获取下一个未访问元素
            public K next() {
                return nextEntry().key;
            }
            
            // （在正序遍历中）移除上次访问过的元素
            public void remove() {
                removeAscending();
            }
            
            public Spliterator<K> trySplit() {
                return null;
            }
            
            public boolean tryAdvance(Consumer<? super K> action) {
                if(hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }
            
            public void forEachRemaining(Consumer<? super K> action) {
                while(hasNext()) {
                    action.accept(next());
                }
            }
            
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
            
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SORTED;
            }
        }
        
        // SubMap中key-value对的迭代器
        final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {
            SubMapEntryIterator(TreeMap.Entry<K, V> first, TreeMap.Entry<K, V> fence) {
                super(first, fence);
            }
            
            // （在正序遍历中）获取下一个未访问元素
            public Map.Entry<K, V> next() {
                return nextEntry();
            }
            
            // （在正序遍历中）移除上次访问过的元素
            public void remove() {
                removeAscending();
            }
        }
        
        // 【逆序】SubMap中Key的迭代器
        final class DescendingSubMapKeyIterator extends SubMapIterator<K> implements Spliterator<K> {
            DescendingSubMapKeyIterator(TreeMap.Entry<K, V> last, TreeMap.Entry<K, V> fence) {
                super(last, fence);
            }
            
            // （在逆序遍历中）获取下一个未访问元素
            public K next() {
                return prevEntry().key;
            }
            
            // （在逆序遍历中）移除上次访问过的元素
            public void remove() {
                removeDescending();
            }
            
            public Spliterator<K> trySplit() {
                return null;
            }
            
            public void forEachRemaining(Consumer<? super K> action) {
                while(hasNext()) {
                    action.accept(next());
                }
            }
            
            public boolean tryAdvance(Consumer<? super K> action) {
                if(hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }
            
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
            
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED;
            }
        }
        
        // 【逆序】SubMap中key-value对的迭代器
        final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {
            DescendingSubMapEntryIterator(TreeMap.Entry<K, V> last, TreeMap.Entry<K, V> fence) {
                super(last, fence);
            }
            
            // （在逆序遍历中）获取下一个未访问元素
            public Map.Entry<K, V> next() {
                return prevEntry();
            }
            
            // （在逆序遍历中）移除上次访问过的元素
            public void remove() {
                removeDescending();
            }
        }
        
    }
    
    /**
     * @serial include
     */
    // 【正序】限制区间范围的TreeMap
    static final class AscendingSubMap<K, V> extends NavigableSubMap<K, V> {
        private static final long serialVersionUID = 912986545866124060L;
        
        
        /*▼ 构造器 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        AscendingSubMap(TreeMap<K, V> treeMap, boolean fromStart, K lo, boolean loInclusive, boolean toEnd, K hi, boolean hiInclusive) {
            super(treeMap, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }
        
        /*▲ 构造器 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        /*▼ 视图 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        public Set<Map.Entry<K, V>> entrySet() {
            EntrySetView es = entrySetView;
            return (es != null) ? es : (entrySetView = new AscendingEntrySetView());
        }
        
        /*▲ 视图 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        /*▼ NavigableMap ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 获取当前SubMap的外部比较器Comparator
        public Comparator<? super K> comparator() {
            return treeMap.comparator();
        }
        
        
        /*
         * 获取【理论区间】为〖fromKey, toKey〗的SubMap，区间下限/上限是否为闭区间由fromInclusive和toInclusive参数决定
         * 注：【实际区间】下限/上限有穷
         */
        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            if(!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            
            if(!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            
            return new AscendingSubMap<>(treeMap, false, fromKey, fromInclusive, false, toKey, toInclusive);
        }
        
        /*
         * 获取【理论区间】上限为toKey的SubMap，区间上限是否为闭区间由inclusive参数决定
         * 注：【实际区间】上限有穷，但区间下限信息与父视图相同
         */
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            if(!inRange(toKey, inclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            
            return new AscendingSubMap<>(treeMap, fromStart, subLow, loInclusive, false, toKey, inclusive);
        }
        
        /*
         * 获取【理论区间】下限为fromKey的SubMap，区间下限是否为闭区间由inclusive参数决定
         * 注：【实际区间】下限有穷，但区间上限信息与父视图相同
         */
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            if(!inRange(fromKey, inclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            
            return new AscendingSubMap<>(treeMap, false, fromKey, inclusive, toEnd, subHigh, hiInclusive);
        }
        
        // 获取【逆序】SubMap
        public NavigableMap<K, V> descendingMap() {
            NavigableMap<K, V> mv = descendingMapView;
            return (mv != null) ? mv : (descendingMapView = new DescendingSubMap<>(treeMap, fromStart, subLow, loInclusive, toEnd, subHigh, hiInclusive));
        }
        
        /*▲ NavigableMap ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        /*▼ NavigableSubMap ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 获取【backing区间】中尽量靠左侧的【可视】元素
        TreeMap.Entry<K, V> subLowest() {
            return absLowest();
        }
        
        // 获取【backing区间】中尽量靠右侧的【可视】元素
        TreeMap.Entry<K, V> subHighest() {
            return absHighest();
        }
        
        // 获取【backing区间】中尽量靠右侧的，且其targetKey<key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        TreeMap.Entry<K, V> subLower(K key) {
            return absLower(key);
        }
        
        // 获取【backing区间】中尽量靠左侧的，且其targetKey>key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        TreeMap.Entry<K, V> subHigher(K key) {
            return absHigher(key);
        }
        
        // 获取【backing区间】中尽量靠右侧的，且其targetKey<=key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        TreeMap.Entry<K, V> subFloor(K key) {
            return absFloor(key);
        }
        
        // 获取【backing区间】中尽量靠左侧的，且其targetKey>=key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        TreeMap.Entry<K, V> subCeiling(K key) {
            return absCeiling(key);
        }
        
        // 返回当前SubMap【可视区间】的Iterator（当前是正序）
        Iterator<K> keyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }
        
        // 返回【逆序】SubMap【可视区间】的Iterator
        Iterator<K> descendingKeyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }
        
        // 返回当前SubMap【可视区间】的Spliterator
        Spliterator<K> keySpliterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }
        
        /*▲ NavigableSubMap ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        final class AscendingEntrySetView extends EntrySetView {
            public Iterator<Map.Entry<K, V>> iterator() {
                return new SubMapEntryIterator(absLowest(), absHighFence());
            }
        }
    }
    
    /**
     * @serial include
     */
    // 【逆序】限制区间范围的TreeMap
    static final class DescendingSubMap<K, V> extends NavigableSubMap<K, V> {
        private static final long serialVersionUID = 912986545866120460L;
        
        // 外部比较器的行为也要逆转
        private final Comparator<? super K> reverseComparator = Collections.reverseOrder(treeMap.comparator);
        
        
        /*▼ 构造器 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        DescendingSubMap(TreeMap<K, V> treeMap, boolean fromStart, K lo, boolean loInclusive, boolean toEnd, K hi, boolean hiInclusive) {
            super(treeMap, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }
        
        /*▲ 构造器 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        /*▼ 视图 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        public Set<Map.Entry<K, V>> entrySet() {
            EntrySetView es = entrySetView;
            return (es != null) ? es : (entrySetView = new DescendingEntrySetView());
        }
        
        /*▲ 视图 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        /*▼ NavigableMap ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 获取当前SubMap的外部比较器Comparator
        public Comparator<? super K> comparator() {
            return reverseComparator;
        }
        
        
        /*
         * 获取【理论区间】为〖fromKey, toKey〗的SubMap，区间<下限>/<上限>是否为闭区间由fromInclusive和toInclusive参数决定
         * 注：【实际区间】<下限>/<上限>有穷
         */
        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            if(!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            
            if(!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            
            return new DescendingSubMap<>(treeMap, false, toKey, toInclusive, false, fromKey, fromInclusive);
        }
        
        /*
         * 获取【理论区间】<上限>为toKey的SubMap，区间<上限>是否为闭区间由inclusive参数决定
         * 注：【实际区间】<上限>有穷，但区间<下限>信息与父视图相同
         */
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            if(!inRange(toKey, inclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            
            return new DescendingSubMap<>(treeMap, false, toKey, inclusive, toEnd, subHigh, hiInclusive);
        }
        
        /*
         * 获取【理论区间】<下限>为fromKey的SubMap，区间<下限>是否为闭区间由inclusive参数决定
         * 注：【实际区间】<下限>有穷，但区间<上限>信息与父视图相同
         */
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            if(!inRange(fromKey, inclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            
            return new DescendingSubMap<>(treeMap, fromStart, subLow, loInclusive, false, fromKey, inclusive);
        }
        
        // 获取【逆序】SubMap
        public NavigableMap<K, V> descendingMap() {
            NavigableMap<K, V> mv = descendingMapView;
            return (mv != null) ? mv : (descendingMapView = new AscendingSubMap<>(treeMap, fromStart, subLow, loInclusive, toEnd, subHigh, hiInclusive));
        }
        
        /*▲ NavigableMap ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        /*▼ NavigableSubMap ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        // 获取【backing区间】中尽量靠<右>侧的【可视】元素
        TreeMap.Entry<K, V> subLowest() {
            return absHighest();
        }
        
        // 获取【backing区间】中尽量靠<左>侧的【可视】元素
        TreeMap.Entry<K, V> subHighest() {
            return absLowest();
        }
        
        // 获取【backing区间】中尽量靠<左>侧的，且其targetKey>key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        TreeMap.Entry<K, V> subLower(K key) {
            return absHigher(key);
        }
        
        // 获取【backing区间】中尽量靠<右>侧的，且其targetKey<key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        TreeMap.Entry<K, V> subHigher(K key) {
            return absLower(key);
        }
        
        // 获取【backing区间】中尽量靠<左>侧的，且其targetKey>=key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        TreeMap.Entry<K, V> subFloor(K key) {
            return absCeiling(key);
        }
        
        // 获取【backing区间】中尽量靠<右>侧的，且其targetKey<=key的【可视】元素（这里的关系运算符标记的是元素遍历的先后关系）
        TreeMap.Entry<K, V> subCeiling(K key) {
            return absFloor(key);
        }
        
        // 返回当前SubMap的Iterator（当前是逆序）
        Iterator<K> keyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }
        
        // 返回【逆序】SubMap的Iterator
        Iterator<K> descendingKeyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }
        
        // 返回当前SubMap的Spliterator
        Spliterator<K> keySpliterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }
        
        /*▲ NavigableSubMap ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        final class DescendingEntrySetView extends EntrySetView {
            public Iterator<Map.Entry<K, V>> iterator() {
                return new DescendingSubMapEntryIterator(absHighest(), absLowFence());
            }
        }
    }
    
    
    
    /**
     * This class exists solely for the sake of serialization compatibility with previous releases of TreeMap that did not support NavigableMap.
     * It translates an old-version SubMap into a new-version AscendingSubMap.
     * This class is never otherwise used.
     *
     * @serial include
     */
    // 【未使用】
    private class SubMap extends AbstractMap<K, V> implements SortedMap<K, V>, Serializable {
        private static final long serialVersionUID = -6520786458950516097L;
        private boolean fromStart = false, toEnd = false;
        private K fromKey, toKey;
        
        public Set<Map.Entry<K, V>> entrySet() {
            throw new InternalError();
        }
        
        public Comparator<? super K> comparator() {
            throw new InternalError();
        }
        
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            throw new InternalError();
        }
        
        public SortedMap<K, V> headMap(K toKey) {
            throw new InternalError();
        }
        
        public SortedMap<K, V> tailMap(K fromKey) {
            throw new InternalError();
        }
        
        public K lastKey() {
            throw new InternalError();
        }
        
        public K firstKey() {
            throw new InternalError();
        }
        
        private Object readResolve() {
            return new AscendingSubMap<>(TreeMap.this, fromStart, fromKey, true, toEnd, toKey, false);
        }
    }
}
