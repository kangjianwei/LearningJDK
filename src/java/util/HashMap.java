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
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import jdk.internal.misc.SharedSecrets;

/**
 * Hash table based implementation of the {@code Map} interface.  This
 * implementation provides all of the optional map operations, and permits
 * {@code null} values and the {@code null} key.  (The {@code HashMap}
 * class is roughly equivalent to {@code Hashtable}, except that it is
 * unsynchronized and permits nulls.)  This class makes no guarantees as to
 * the order of the map; in particular, it does not guarantee that the order
 * will remain constant over time.
 *
 * <p>This implementation provides constant-time performance for the basic
 * operations ({@code get} and {@code put}), assuming the hash function
 * disperses the elements properly among the buckets.  Iteration over
 * collection views requires time proportional to the "capacity" of the
 * {@code HashMap} instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the initial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.
 *
 * <p>An instance of {@code HashMap} has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created.  The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased.  When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the hash table is <i>rehashed</i> (that is, internal data
 * structures are rebuilt) so that the hash table has approximately twice the
 * number of buckets.
 *
 * <p>As a general rule, the default load factor (.75) offers a good
 * tradeoff between time and space costs.  Higher values decrease the
 * space overhead but increase the lookup cost (reflected in most of
 * the operations of the {@code HashMap} class, including
 * {@code get} and {@code put}).  The expected number of entries in
 * the map and its load factor should be taken into account when
 * setting its initial capacity, so as to minimize the number of
 * rehash operations.  If the initial capacity is greater than the
 * maximum number of entries divided by the load factor, no rehash
 * operations will ever occur.
 *
 * <p>If many mappings are to be stored in a {@code HashMap}
 * instance, creating it with a sufficiently large capacity will allow
 * the mappings to be stored more efficiently than letting it perform
 * automatic rehashing as needed to grow the table.  Note that using
 * many keys with the same {@code hashCode()} is a sure way to slow
 * down performance of any hash table. To ameliorate impact, when keys
 * are {@link Comparable}, this class may use comparison order among
 * keys to help break ties.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash map concurrently, and at least one of
 * the threads modifies the map structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more mappings; merely changing the value
 * associated with a key that an instance already contains is not a
 * structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 *
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new HashMap(...));</pre>
 *
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Doug Lea
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Neal Gafter
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.2
 */

/*
 * HashMap结构：哈希数组+链表/红黑树，key和value均可以为null
 *
 * 存储元素时，需要调用key的hashCode()方法，计算出一个哈希值
 * 1.哈希值相同的元素，必定位于同一个哈希槽（链）上，但不能确定这两个元素是不是同位元素
 *   在进一步判断key如果相等（必要时需要调用equals()方法）时，才能确定这两个元素属于同位元素
 *   如果是存储同位元素，需要考虑是否允许覆盖旧值的问题
 * 2.哈希值不同的元素，它们也可能位于同一个哈希槽（链）上，但它们肯定不是同位元素
 *
 * 注：HashMap非线程安全。如果需要考虑并发，则需要使用ConcurrentHashMap
 */
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
    
    /*
     * Implementation notes.
     *
     * This map usually acts as a binned (bucketed) hash table, but
     * when bins get too large, they are transformed into bins of
     * TreeNodes, each structured similarly to those in
     * java.util.TreeMap. Most methods try to use normal bins, but
     * relay to TreeNode methods when applicable (simply by checking
     * instanceof a node).  Bins of TreeNodes may be traversed and
     * used like any others, but additionally support faster lookup
     * when overpopulated. However, since the vast majority of bins in
     * normal use are not overpopulated, checking for existence of
     * tree bins may be delayed in the course of table methods.
     *
     * Tree bins (i.e., bins whose elements are all TreeNodes) are
     * ordered primarily by hashCode, but in the case of ties, if two
     * elements are of the same "class C implements Comparable<C>",
     * type then their compareTo method is used for ordering. (We
     * conservatively check generic types via reflection to validate
     * this -- see method comparableClassFor).  The added complexity
     * of tree bins is worthwhile in providing worst-case O(log n)
     * operations when keys either have distinct hashes or are
     * orderable, Thus, performance degrades gracefully under
     * accidental or malicious usages in which hashCode() methods
     * return values that are poorly distributed, as well as those in
     * which many keys share a hashCode, so long as they are also
     * Comparable. (If neither of these apply, we may waste about a
     * factor of two in time and space compared to taking no
     * precautions. But the only known cases stem from poor user
     * programming practices that are already so slow that this makes
     * little difference.)
     *
     * Because TreeNodes are about twice the size of regular nodes, we
     * use them only when bins contain enough nodes to warrant use
     * (see TREEIFY_THRESHOLD). And when they become too small (due to
     * removal or resizing) they are converted back to plain bins.  In
     * usages with well-distributed user hashCodes, tree bins are
     * rarely used.  Ideally, under random hashCodes, the frequency of
     * nodes in bins follows a Poisson distribution
     * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
     * parameter of about 0.5 on average for the default resizing
     * threshold of 0.75, although with a large variance because of
     * resizing granularity. Ignoring variance, the expected
     * occurrences of list size k are (exp(-0.5) * pow(0.5, k) /
     * factorial(k)). The first values are:
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * more: less than 1 in ten million
     *
     * The root of a tree bin is normally its first node.  However,
     * sometimes (currently only upon Iterator.remove), the root might
     * be elsewhere, but can be recovered following parent links
     * (method TreeNode.root()).
     *
     * All applicable internal methods accept a hash code as an
     * argument (as normally supplied from a public method), allowing
     * them to call each other without recomputing user hashCodes.
     * Most internal methods also accept a "tab" argument, that is
     * normally the current table, but may be a new or old one when
     * resizing or converting.
     *
     * When bin lists are treeified, split, or untreeified, we keep
     * them in the same relative access/traversal order (i.e., field
     * Node.next) to better preserve locality, and to slightly
     * simplify handling of splits and traversals that invoke
     * iterator.remove. When using comparators on insertion, to keep a
     * total ordering (or as close as is required here) across
     * rebalancings, we compare classes and identityHashCodes as
     * tie-breakers.
     *
     * The use and transitions among plain vs tree modes is
     * complicated by the existence of subclass LinkedHashMap. See
     * below for hook methods defined to be invoked upon insertion,
     * removal and access that allow LinkedHashMap internals to
     * otherwise remain independent of these mechanics. (This also
     * requires that a map instance be passed to some utility methods
     * that may create new nodes.)
     *
     * The concurrent-programming-like SSA-based coding style helps
     * avoid aliasing errors amid all of the twisty pointer operations.
     */
    
    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;        // 哈希数组最大容量
    
    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;     // 哈希数组默认容量
    
    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;     // HashMap默认装载因子（负荷系数）
    
    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2 and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     */
    static final int TREEIFY_THRESHOLD = 8;     // 某个哈希槽（链）上的元素数量增加到此值后，这些元素进入波动期，即将从链表转换为红黑树
    
    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * Should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts
     * between resizing and treeification thresholds.
     */
    static final int MIN_TREEIFY_CAPACITY = 64; // 哈希数组的容量至少增加到此值，且满足TREEIFY_THRESHOLD的要求时，将链表转换为红黑树
    
    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     */
    static final int UNTREEIFY_THRESHOLD = 6;   // 哈希槽（链）上的红黑树上的元素数量减少到此值时，将红黑树转换为链表
    
    /**
     * The table, initialized on first use, and resized as
     * necessary. When allocated, length is always a power of two.
     * (We also tolerate length zero in some operations to allow
     * bootstrapping mechanics that are currently not needed.)
     */
    transient Node<K,V>[] table;    // 哈希数组（注：哈希数组的容量跟HashMap可以存储的元素数量不是一回事）
    
    /**
     * The number of key-value mappings contained in this map.
     */
    transient int size;             // HashMap中的元素数量
    
    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor; // HashMap当前使用的装载因子
    
    /**
     * The next size value at which to resize (capacity * load factor).
     *
     * @serial
     *
     * The javadoc description is true upon serialization.
     * Additionally, if the table array has not been allocated, this field holds the initial array capacity,
     * or zero signifying DEFAULT_INITIAL_CAPACITY.
     */
    int threshold;          // HashMap扩容阈值，【一般】由（哈希数组容量*HashMap装载因子）计算而来，HashMap中元素数量超过该阈值时，哈希数组需要扩容
    
    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     */
    transient Set<Map.Entry<K,V>> entrySet; // entry集合
    
    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    transient int modCount;         // 记录HashMap结构的修改次数
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs an empty {@code HashMap} with the default initial capacity (16) and the default load factor (0.75).
     */
    // 初始化一个哈希数组容量为16，装载因子为0.75的HashMap
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }
    
    /**
     * Constructs an empty {@code HashMap} with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    // 初始化一个哈希数组容量为initialCapacity，装载因子为0.75的HashMap
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Constructs an empty {@code HashMap} with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    // 初始化一个哈希数组容量为initialCapacity，装载因子为loadFactor的HashMap
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
        
        // 初始化装载因子
        this.loadFactor = loadFactor;
        
        // 用初始容量信息来初始化HashMap扩容阈值，该阈值后续将作为初始化哈希数组的容量依据
        this.threshold = tableSizeFor(initialCapacity);
    }
    
    /**
     * Constructs a new {@code HashMap} with the same mappings as the
     * specified {@code Map}.  The {@code HashMap} is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified {@code Map}.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
    // 使用指定的HashMap中的元素来初始化一个新的HashMap
    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        // 将指定HashMap中的元素存入到当前HashMap（允许覆盖）
        putMapEntries(m, false);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     */
    // 将指定的元素（key-value）存入HashMap，并返回旧值，允许覆盖
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }
    
    // 将指定的元素（key-value）存入HashMap，并返回旧值，不允许覆盖
    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }
    
    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     *
     * @throws NullPointerException if the specified map is null
     */
    // 将指定Map中的元素存入到当前Map（允许覆盖）
    public void putAll(Map<? extends K, ? extends V> map) {
        putMapEntries(map, true);
    }
    
    /*▲ 存值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    // 根据指定的key获取对应的value，如果不存在，则返回null
    public V get(Object key) {
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        Node<K,V> e = getNode(hash(key), key);
        
        return e == null ? null : e.value;
    }
    
    // 根据指定的key获取对应的value，如果不存在，则返回指定的默认值defaultValue
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        Node<K,V> e = getNode(hash(key), key);
        
        return e == null ? defaultValue : e.value;
    }
    
    /*▲ 取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 移除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map
     *
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}.
     * (A {@code null} return can also indicate that the map
     * previously associated {@code null} with {@code key}.)
     */
    // 移除拥有指定key的元素，并返回刚刚移除的元素的值
    public V remove(Object key) {
        Node<K, V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ? null : e.value;
    }
    
    // 移除拥有指定key和value的元素，返回值表示是否移除成功
    @Override
    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }
    
    
    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    // 清空HashMap中所有元素
    public void clear() {
        Node<K, V>[] tab;
        modCount++;
        if((tab = table) != null && size>0) {
            size = 0;
            Arrays.fill(tab, null);
        }
    }
    
    /*▲ 移除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 替换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 将拥有指定key的元素的值替换为newValue，并返回刚刚替换的元素的值（替换失败返回null）
    @Override
    public V replace(K key, V newValue) {
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        Node<K,V> e = getNode(hash(key), key);
        
        if (e != null) {
            V oldValue = e.value;
            e.value = newValue;
            afterNodeAccess(e);
            return oldValue;
        }
        
        return null;
    }
    
    // 将拥有指定key和oldValue的元素的值替换为newValue，返回值表示是否成功替换
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        Node<K,V> e = getNode(hash(key), key);
        
        V v;
        
        if (e != null && ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            e.value = newValue;
            afterNodeAccess(e);
            return true;
        }
        
        return false;
    }
    
    // 替换当前HashMap中的所有元素，替换策略由function决定，function的入参是元素的key和value，出参作为新值
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Node<K,V>[] tab;
        
        if (function == null) {
            throw new NullPointerException();
        }
        
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (Node<K,V> e : tab) {
                for (; e != null; e = e.next) {
                    e.value = function.apply(e.key, e.value);
                }
            }
            
            if (modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
    }
    
    /*▲ 替换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包含查询 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if this map contains a mapping for the
     * specified key.
     *
     * @param key The key whose presence in this map is to be tested
     *
     * @return {@code true} if this map contains a mapping for the specified
     * key.
     */
    // 判断HashMap中是否存在指定key的元素
    public boolean containsKey(Object key) {
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        Node<K,V> e = getNode(hash(key), key);
        
        return e != null;
    }
    
    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     *
     * @return {@code true} if this map maps one or more keys to the
     * specified value
     */
    // 判断HashMap中是否存在指定value的元素
    public boolean containsValue(Object value) {
        Node<K, V>[] tab;
        V v;
        
        if((tab = table) != null && size>0) {
            for(Node<K, V> e : tab) {
                for(; e != null; e = e.next) {
                    if((v = e.value) == value || (value != null && value.equals(v))) {
                        return true;
                    }
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
     * @return a set view of the keys contained in this map
     */
    // 获取HashMap中key的集合
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if(ks == null) {
            ks = new KeySet();
            keySet = ks;
        }
        return ks;
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
     * @return a view of the values contained in this map
     */
    // 获取HashMap中value的集合
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
     * @return a set view of the mappings contained in this map
     */
    // 获取HashMap中key-value对的集合
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 遍历 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 遍历HashMap中的元素，并对其应用action操作，action的入参是元素的key和value
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Node<K, V>[] tab;
        if(action == null) {
            throw new NullPointerException();
        }
        
        if(size>0 && (tab = table) != null) {
            int mc = modCount;
            for(Node<K, V> e : tab) {
                for(; e != null; e = e.next) {
                    action.accept(e.key, e.value);
                }
            }
            
            if(modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
    }
    
    /*▲ 遍历 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 重新映射 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     *
     * <p>This method will, on a best-effort basis, throw a
     * {@link ConcurrentModificationException} if it is detected that the
     * remapping function modifies this map during computation.
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
     * ◇存在同位元素◇    --→ ★新value=备用value★ --→ ■【插入】新value■
     *      | 是         否
     *      ↓
     * ◇旧value不为null  --→ ★新value=备用value★ --→ ■新value【替换】旧value■
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
    public V merge(K key, V bakValue, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if(bakValue == null) {
            throw new NullPointerException();
        }
        
        if(remappingFunction == null) {
            throw new NullPointerException();
        }
        
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        
        if(size>threshold || (tab = table) == null || (n = tab.length) == 0) {
            // 初始化哈希数组，或者对哈希数组扩容，返回新的哈希数组
            tab = resize();
            
            n = tab.length;
        }
        
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        if((first = tab[i = (n - 1) & hash]) != null) {
            if(first instanceof TreeNode) {
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            } else {
                Node<K, V> e = first;
                K k;
                do {
                    if(e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while((e = e.next) != null);
            }
        }
        
        // 如果找到了同位元素
        if(old != null) {
            V newValue;
            
            // 确定应用到目标元素上的新值
            if(old.value != null) {
                int mc = modCount;
                newValue = remappingFunction.apply(old.value, bakValue);
                if(mc != modCount) {
                    throw new ConcurrentModificationException();
                }
            } else {
                newValue = bakValue;
            }
            
            // 如果新值不为null，直接替换旧值
            if(newValue != null) {
                old.value = newValue;
                afterNodeAccess(old);
            } else {
                // 如果新值为null，则会移除该元素
                removeNode(hash, key, null, false, true);
            }
            
            return newValue;
        }
        
        // 如果没找到目标元素，但是传入的value不为null，则向HashMap中插入新元素
        if(bakValue != null) {
            if(t != null) {
                t.putTreeVal(this, tab, hash, key, bakValue);
            } else {
                tab[i] = newNode(hash, key, bakValue, first);
                
                if(binCount >= TREEIFY_THRESHOLD - 1) {
                    treeifyBin(tab, hash);
                }
            }
            
            ++modCount;
            
            ++size;
            
            afterNodeInsertion(true);
        }
        
        return bakValue;
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>This method will, on a best-effort basis, throw a
     * {@link ConcurrentModificationException} if it is detected that the
     * remapping function modifies this map during computation.
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
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if(remappingFunction == null) {
            throw new NullPointerException();
        }
        
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        
        if(size>threshold || (tab = table) == null || (n = tab.length) == 0) {
            // 初始化哈希数组，或者对哈希数组扩容，返回新的哈希数组
            tab = resize();
            
            n = tab.length;
        }
        
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        if((first = tab[i = (n - 1) & hash]) != null) {
            if(first instanceof TreeNode) {
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            } else {
                Node<K, V> e = first;
                K k;
                do {
                    if(e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while((e = e.next) != null);
            }
        }
        
        V oldValue = (old == null) ? null : old.value;
        
        int mc = modCount;
        
        // 利用key和旧的value计算一个新的value
        V newValue = remappingFunction.apply(key, oldValue);
        
        if(mc != modCount) {
            throw new ConcurrentModificationException();
        }
        
        // 如果存在同位元素
        if(old != null) {
            if(newValue != null) {
                old.value = newValue;
                afterNodeAccess(old);
            } else {
                removeNode(hash, key, null, false, true);
            }
        } else if(newValue != null) {
            if(t != null) {
                t.putTreeVal(this, tab, hash, key, newValue);
            } else {
                tab[i] = newNode(hash, key, newValue, first);
                if(binCount >= TREEIFY_THRESHOLD - 1) {
                    treeifyBin(tab, hash);
                }
            }
            modCount = mc + 1;
            ++size;
            afterNodeInsertion(true);
        }
        
        return newValue;
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>This method will, on a best-effort basis, throw a
     * {@link ConcurrentModificationException} if it is detected that the
     * remapping function modifies this map during computation.
     *
     * @throws ConcurrentModificationException if it is detected that the
     *                                         remapping function modified this map
     */
    /*
     * 删除/替换操作，返回新值（可能为null）
     * 此方法的主要意图：存在同位元素，且旧value不为null时，使用key和旧value创造的新value来更新旧value
     *
     * 注：以下流程图中，涉及到判断(◇)时，纵向代表【是】，横向代表【否】。此外，使用★代表计算。
     *
     *  ●查找同位元素●
     *      |
     *      ↓
     * ◇存在同位元素 && 旧value不为null◇
     *      |
     *      ↓
     * ★新value=(key, 旧value)★
     *      |
     *      ↓
     * ◇新value不为null◇ --→ ■【删除】同位元素■
     *      |
     *      ↓
     * ■新value【替换】旧value■
     */
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if(remappingFunction == null) {
            throw new NullPointerException();
        }
        
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        Node<K, V> e = getNode(hash(key), key);
        
        V oldValue;
        
        // 如果同位元素存在，且旧的值不为null
        if(e != null && (oldValue = e.value) != null) {
            int mc = modCount;
            
            // 利用key和旧的value计算一个新的value
            V newValue = remappingFunction.apply(key, oldValue);
            
            if(mc != modCount) {
                throw new ConcurrentModificationException();
            }
            
            // 如果新的value不为null，则【替换】
            if(newValue != null) {
                e.value = newValue;
                afterNodeAccess(e);
                return newValue;
            }
            
            // 如果新的value为null，则【删除】
            removeNode(hash(key), key, null, false, true);
        }
        
        return null;
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>This method will, on a best-effort basis, throw a
     * {@link ConcurrentModificationException} if it is detected that the
     * mapping function modifies this map during computation.
     *
     * @throws ConcurrentModificationException if it is detected that the
     *                                         mapping function modified this map
     */
    /*
     * 插入/替换操作，返回新值（可能为null）
     * 此方法的主要意图：不存在同位元素，或旧value为null时，使用key创造的新value来更新旧value。如果同位元素旧值不为空，则直接返回旧值。
     *
     * 注：以下流程图中，涉及到判断(◇)时，纵向代表【是】，横向代表【否】。此外，使用★代表计算。
     *
     *  ●查找同位元素●
     *      |
     *      ↓
     * ◇存在同位元素 && 旧value不为null◇ --→ ★新value=(key)★
     *                                  否          |
     *                                     　       ↓
     *                                     　◇新value不为null◇
     *                                     　       |
     *                                     　       ↓
     *                                     　■  存在同位元素，则新value【替换】旧value■
     *                                     　■不存在同位元素，则【插入】新value       ■
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if(mappingFunction == null) {
            throw new NullPointerException();
        }
        
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        
        if(size>threshold || (tab = table) == null || (n = tab.length) == 0) {
            // 初始化哈希数组，或者对哈希数组扩容，返回新的哈希数组
            tab = resize();
            
            n = tab.length;
        }
        
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        if((first = tab[i = (n - 1) & hash]) != null) {
            if(first instanceof TreeNode) {
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            } else {
                Node<K, V> e = first;
                K k;
                do {
                    if(e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while((e = e.next) != null);
            }
            
            V oldValue;
            
            // 如果同位元素存在，且旧的value不是null，直接返回
            if(old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        
        int mc = modCount;
        
        // 对key应用mappingFunction函数，计算出一个新的value
        V newValue = mappingFunction.apply(key);
        
        if(mc != modCount) {
            throw new ConcurrentModificationException();
        }
        
        // 新的value为null
        if(newValue == null) {
            return null;
        }
        
        // 同位元素存在，且旧的value为null，新的value不为null，则用新的value【替换】旧的value
        if(old != null) {
            old.value = newValue;
            afterNodeAccess(old);
            return newValue;
        }
        
        // 如果不存在同位元素，则【插入】key和新的value为一个新元素
        if(t != null) {
            t.putTreeVal(this, tab, hash, key, newValue);
        } else {
            tab[i] = newNode(hash, key, newValue, first);
            
            if(binCount >= TREEIFY_THRESHOLD - 1) {
                treeifyBin(tab, hash);
            }
        }
        
        modCount = mc + 1;
        
        ++size;
        
        afterNodeInsertion(true);
        
        return newValue;
    }
    
    /*▲ 重新映射 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ LinkedHashMap ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * The following package-protected methods are designed to be overridden by LinkedHashMap, but not by any other subclass.
     * Nearly all other internal methods are also package-protected but are declared final, so can be used by LinkedHashMap, view classes, and HashSet.
     */
    
    // 创建一个普通Node
    Node<K, V> newNode(int hash, K key, V value, Node<K, V> next) {
        return new Node<>(hash, key, value, next);
    }
    
    // 创建一个红黑树的TreeNode
    TreeNode<K, V> newTreeNode(int hash, K key, V value, Node<K, V> next) {
        return new TreeNode<>(hash, key, value, next);
    }
    
    // 从红黑树的TreeNode转换为一个普通Node
    Node<K, V> replacementNode(Node<K, V> p, Node<K, V> next) {
        return new Node<>(p.hash, p.key, p.value, next);
    }
    
    // 从普通Node转换为一个红黑树的TreeNode
    TreeNode<K, V> replacementTreeNode(Node<K, V> p, Node<K, V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }
    
    /**
     * Reset to initial default state.  Called by clone and readObject.
     */
    // 重置当前HashMap，清空一切参数
    void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }
    
    // 插入一个新元素时的回调
    void afterNodeInsertion(boolean evict) {
    }
    
    // 移除一个新元素时的回调
    void afterNodeRemoval(Node<K, V> p) {
    }
    
    // 访问一个新元素时的回调
    void afterNodeAccess(Node<K, V> p) {
    }
    
    /* Called only from writeObject, to ensure compatible ordering */
    // 用于序列化过程
    void internalWriteEntries(ObjectOutputStream s) throws IOException {
        Node<K,V>[] tab;
        if (size > 0 && (tab = table) != null) {
            for (Node<K,V> e : tab) {
                for (; e != null; e = e.next) {
                    s.writeObject(e.key);
                    s.writeObject(e.value);
                }
            }
        }
    }
    
    /*▲ LinkedHashMap ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    // 获取HashMap中的元素数量
    public int size() {
        return size;
    }
    
    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings
     */
    // 判断HashMap是否为空集
    public boolean isEmpty() {
        return size == 0;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static final long serialVersionUID = 362498820763181265L;
    
    /**
     * Saves this map to a stream (that is, serializes it).
     *
     * @param s the stream
     *
     * @throws IOException if an I/O error occurs
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     * bucket array) is emitted (int), followed by the
     * <i>size</i> (an int, the number of key-value
     * mappings), followed by the key (Object) and value (Object)
     * for each key-value mapping.  The key-value mappings are
     * emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
        int buckets = capacity();
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        s.writeInt(buckets);
        s.writeInt(size);
        internalWriteEntries(s);
    }
    
    /**
     * Reconstitutes this map from a stream (that is, deserializes it).
     *
     * @param s the stream
     *
     * @throws ClassNotFoundException if the class of a serialized object
     *                                could not be found
     * @throws IOException            if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        // Read in the threshold (ignored), loadfactor, and any hidden stuff
        s.defaultReadObject();
        reinitialize();
        if(loadFactor<=0 || Float.isNaN(loadFactor)) {
            throw new InvalidObjectException("Illegal load factor: " + loadFactor);
        }
        s.readInt();                // Read and ignore number of buckets
        int mappings = s.readInt(); // Read number of mappings (size)
        if(mappings<0) {
            throw new InvalidObjectException("Illegal mappings count: " + mappings);
        } else if(mappings>0) { // (if zero, use defaults)
            // Size the table using given load factor only if within
            // range of 0.25...4.0
            float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
            float fc = (float) mappings / lf + 1.0f;
            int cap = ((fc<DEFAULT_INITIAL_CAPACITY) ? DEFAULT_INITIAL_CAPACITY : (fc >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : tableSizeFor((int) fc));
            float ft = (float) cap * lf;
            threshold = ((cap<MAXIMUM_CAPACITY && ft<MAXIMUM_CAPACITY) ? (int) ft : Integer.MAX_VALUE);
            
            // Check Map.Entry[].class since it's the nearest public type to
            // what we're actually creating.
            SharedSecrets.getJavaObjectInputStreamAccess().checkArray(s, Map.Entry[].class, cap);
            @SuppressWarnings({"rawtypes", "unchecked"})
            Node<K, V>[] tab = (Node<K, V>[]) new Node[cap];
            table = tab;
            
            // Read the keys and values, and put the mappings in the HashMap
            for(int i = 0; i<mappings; i++) {
                @SuppressWarnings("unchecked")
                K key = (K) s.readObject();
                @SuppressWarnings("unchecked")
                V value = (V) s.readObject();
                putVal(hash(key), key, value, false, false);
            }
        }
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns a shallow copy of this {@code HashMap} instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        HashMap<K, V> result;
        try {
            result = (HashMap<K, V>) super.clone();
        } catch(CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
        result.reinitialize();
        // 将当前HashMap中的元素存入到result（允许覆盖）
        result.putMapEntries(this, false);
        return result;
    }
    
    
    
    /**
     * Computes key.hashCode() and spreads (XORs) higher bits of hash to lower.
     * Because the table uses power-of-two masking, sets of hashes that vary only in bits above the current mask will always collide.
     * (Among known examples are sets of Float keys holding consecutive whole numbers in small tables.)
     * So we apply a transform that spreads the impact of higher bits downward.
     * There is a tradeoff between speed, utility, and quality of bit-spreading.
     * Because many common sets of hashes are already reasonably distributed (so don't benefit from spreading),
     * and because we use trees to handle large sets of collisions in bins,
     * we just XOR some shifted bits in the cheapest possible way to reduce systematic lossage,
     * as well as to incorporate impact of the highest bits that would otherwise never be used in index calculations because of table bounds.
     */
    /*
     * 计算key的哈希值，在这个过程中会调用key的hashCode()方法
     *
     * key是一个对象的引用（可以看成地址）
     * 理论上讲，key的值是否相等，跟计算出的哈希值是否相等，没有必然联系，一切都取决于hashCode()这个方法
     */
    static final int hash(Object key) {
        int h;
        return (key == null)
            ? 0
            : (h = key.hashCode()) ^ (h >>> 16);
    }
    
    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */
    /*
     * 检查Comparable接口是否有效
     *
     * 假设存在X类型的实例x，
     * 如果X类型实现了Comparable接口，且接口中的类型参数是X自身，则返回X.class，
     * 否则，将返回null，这代表X没有实现“有效”的Comparable接口
     *
     * 例如：
     * class X implements Comparable<X> {
     *     // 。。。
     * }
     *
     * X x = new X();
     *
     * 则：comparableClassFor(x)返回X.class
     */
    static Class<?> comparableClassFor(Object x) {
        // 是否实现了Comparable接口
        if(x instanceof Comparable) {
            Class<?> c = x.getClass();
            
            // 快速判断。对于String类型的实例，则直接返回String.class
            if(c == String.class) {
                // bypass checks
                return c;
            }
            
            // 获取当前类的父接口（可识别泛型类型和非泛型类型）
            Type[] ts = c.getGenericInterfaces();
            
            if(ts != null) {
                for(Type t : ts) {
                    if(t instanceof ParameterizedType) {
                        ParameterizedType p = (ParameterizedType) t;
                        if(p.getRawType() == Comparable.class){
                            // 获取参数化类型中的实际参数（argument）
                            Type[] as = p.getActualTypeArguments();
                            if(as != null) {
                                if(as.length == 1){
                                    /*
                                     * 进一步判断，是否实现了“有效”的Comparable接口，
                                     * “有效”的含义是，Comparable接口的参数为该类本身，
                                     * 因为只有这样，才能对该类进行有效的内部比较
                                     */
                                    if(as[0] == c){
                                        // type arg is c
                                        return c;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Returns k.compareTo(x) if x matches kc (k's screened comparable class), else 0.
     */
    // 返回k和x的比较结果
    @SuppressWarnings({"rawtypes", "unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return x == null || x.getClass() != kc
            ? 0
            : ((Comparable) k).compareTo(x);
    }
    
    /**
     * Returns a power of two size for the given target capacity.
     */
    /*
     * 根据预期的容量cap计算出HashMap中的哈希数组实际需要分配的容量
     * 如果输入值是2的冪，则原样返回，如果不是2的冪，则向上取就近的冪
     * 比如输入13，返回16，输入17，返回32
     */
    static final int tableSizeFor(int cap) {
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n<0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
    
    /**
     * Implements Map.putAll and Map constructor.
     *
     * @param m     the map
     * @param evict false when initially constructing this map, else true (relayed to method afterNodeInsertion).
     */
    // 将指定HashMap中的元素存入到当前HashMap（允许覆盖）
    final void putMapEntries(Map<? extends K, ? extends V> map, boolean evict) {
        int s = map.size();
        if(s>0) {
            // 如果当前HashMap的哈希数组还未初始化
            if(table == null) { // pre-size
                // 由HashMap中的元素数量反推哈希数组的最低容量要求
                float ft = ((float) s / loadFactor) + 1.0F;
                int t = ((ft<(float) MAXIMUM_CAPACITY) ? (int) ft : MAXIMUM_CAPACITY);
                // 计算HashMap扩容阈值
                if(t>threshold) {
                    threshold = tableSizeFor(t);
                }
                
                // 如果当前HashMap的哈希数组已存在，但是容量不足，则需要扩容
            } else if(s>threshold) {
                // 初始化哈希数组，或者对哈希数组扩容，返回新的哈希数组
                resize();
            }
            
            for(Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                // 向HashMap中存入新的元素，允许覆盖
                putVal(hash(key), key, value, false, evict);
            }
        }
    }
    
    /**
     * Implements Map.get and related methods.
     *
     * @param hash hash for key
     * @param key  the key
     *
     * @return the node, or null if none
     */
    // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
    final Node<K, V> getNode(int hash, Object key) {
        Node<K, V>[] tab;
        Node<K, V> first, e;
        int n;
        K k;
        
        if((tab = table) != null && (n = tab.length)>0 && (first = tab[(n - 1) & hash]) != null) {
            /*
             * 对哈希槽（链）中的首个元素进行判断
             *
             * 只有哈希值一致（还说明不了key是否一致），且key也相同（必要时需要用到equals()方法）时，
             * 这里才认定是存在同位元素（在HashMap中占据相同位置的元素）
             */
            if(first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k)))) {
                return first;
            }
            
            if((e = first.next) != null) {
                // 如果是红黑树元素，则在红黑树中查找
                if(first instanceof TreeNode) {
                    return ((TreeNode<K, V>) first).getTreeNode(hash, key);
                }
                
                // 遍历哈希槽（链）上后续的元素
                do {
                    if(e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                        return e;
                    }
                } while((e = e.next) != null);
            }
        }
        
        return null;
    }
    
    /**
     * Implements Map.put and related methods.
     *
     * @param hash         hash for key
     * @param key          the key
     * @param value        the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict        if false, the table is in creation mode.
     *
     * @return previous value, or null if none
     */
    /*
     * 向当前Map中存入新的元素，并返回旧元素
     *
     * hash         key的哈希值
     * onlyIfAbsent 是否需要维持原状（不覆盖旧值）
     * evict        if false, the table is in creation mode.
     *
     * 返回同位元素的旧值（在当前Map中占据相同位置的元素）
     * 如果不存在同位元素，即插入了新元素，则返回null
     * 如果存在同位元素，但同位元素的旧值为null，那么也返回null
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
        Node<K, V>[] tab;   // 指向当前哈希数组
        Node<K, V> p;       // 指向待插入元素应当插入的位置
        int n, i;
        
        // 如果哈希数组还未初始化，或者容量无效，则需要初始化一个哈希数组
        if((tab = table) == null || (n = tab.length) == 0) {
            // 初始化哈希数组，或者对哈希数组扩容，返回新的哈希数组
            tab = resize();
            
            n = tab.length;
        }
        
        // p指向hash所在的哈希槽（链）上的首个元素
        p = tab[i = (n - 1) & hash];
        
        // 如果哈希槽为空，则在该槽上放置首个元素（普通Node）
        if(p == null) {
            tab[i] = newNode(hash, key, value, null);
            
            // 如果哈希槽不为空，则需要在哈希槽后面链接更多的元素
        } else {
            Node<K, V> e;
            K k;
            
            /*
             * 对哈希槽中的首个元素进行判断
             *
             * 只有哈希值一致（还说明不了key是否一致），且key也相同（必要时需要用到equals()方法）时，
             * 这里才认定是存在同位元素（在HashMap中占据相同位置的元素）
             */
            if(p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) {
                e = p;
                
                // 如果该哈希槽上链接的是红黑树结点，则需要调用红黑树的插入方法
            } else if(p instanceof TreeNode) {
                e = ((TreeNode<K, V>) p).putTreeVal(this, tab, hash, key, value);
            } else {
                // 遍历哈希槽后面链接的其他元素（binCount统计的是插入新元素之前遍历过的元素数量）
                for(int binCount = 0; ; ++binCount) {
                    // 如果没有找到同位元素，则需要插入新元素
                    if((e = p.next) == null) {
                        // 插入一个普通结点
                        p.next = newNode(hash, key, value, null);
                        
                        // 哈希槽（链）上的元素数量增加到TREEIFY_THRESHOLD后，这些元素进入波动期，即将从链表转换为红黑树
                        if(binCount >= TREEIFY_THRESHOLD - 1) { // -1 for 1st
                            treeifyBin(tab, hash);
                        }
                        
                        break;
                    }
                    
                    /*
                     * 对哈希槽后面链接的其他元素进行判断
                     *
                     * 只有哈希值一致（还说明不了key是否一致），且key也相同（必要时需要用到equals()方法）时，
                     * 这里才认定是存在同位元素（在HashMap中占据相同位置的元素）
                     */
                    if(e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                        break;
                    }
                    
                    p = e;
                }
            }
            
            // 如果存在同位元素（在HashMap中占据相同位置的元素）
            if(e != null) { // existing mapping for key
                // 获取旧元素的值
                V oldValue = e.value;
                
                // 如果不需要维持原状（可以覆盖旧值），或者旧值为null
                if(!onlyIfAbsent || oldValue == null) {
                    // 更新旧值
                    e.value = value;
                }
                
                afterNodeAccess(e);
                
                // 返回覆盖前的旧值
                return oldValue;
            }
        }
        
        // HashMap的更改次数加一
        ++modCount;
        
        // 如果哈希数组的容量已超过阈值，则需要对哈希数组扩容
        if(++size>threshold) {
            // 初始化哈希数组，或者对哈希数组扩容，返回新的哈希数组
            resize();
        }
        
        afterNodeInsertion(evict);
        
        // 如果插入的是全新的元素，在这里返回null
        return null;
    }
    
    /**
     * Initializes or doubles table size.
     * If null, allocates in accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion,
     * the elements from each bin must either stay at same index,
     * or move with a power of two offset in the new table.
     *
     * @return the table
     */
    /*
     * 初始化哈希数组，或者对哈希数组扩容，返回新的哈希数组
     *
     * 注：哈希数组的容量跟HashMap存放的元素数量没有必然联系
     *    哈希数组只存放一系列同位元素（在HashMap中占据相同位置的元素）中最早进来的那个
     */
    final Node<K, V>[] resize() {
        Node<K, V>[] oldTab = table;
        
        // 旧容量
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        // 旧阈值
        int oldThr = threshold;
        
        // 新容量
        int newCap;
        // 新阈值，初始化为0
        int newThr = 0;
        
        // 如果哈希数组已经初始化（非首次进来）
        if(oldCap>0) {
            // 如果哈希表数组容量已经超过最大容量
            if(oldCap >= MAXIMUM_CAPACITY) {
                // 将HashMap的阈值更新为允许的最大值
                threshold = Integer.MAX_VALUE;
                // 不需要更改哈希数组（容量未发生变化），直接返回
                return oldTab;
            } else {
                // 尝试将哈希表数组容量加倍
                newCap = oldCap << 1;
                
                // 如果容量成功加倍（没有达到上限），则将阈值也加倍
                if(newCap<MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY) {
                    newThr = oldThr << 1; // double threshold
                }
            }
            
            // 如果哈希数组还未初始化（首次进来）
        } else {
            /* initial capacity was placed in threshold */
            // 如果实例化HashMap时已经指定了初始容量，则将哈希数组当前容量初始化为与旧阈值一样大（初始容量与旧阈值的计算关系参见tableSizeFor()方法）
            if(oldThr>0) {
                newCap = oldThr;
                
                /* zero initial threshold signifies using defaults */
                // 如果实例化HashMap时没有指定初始容量，则使用默认的容量与阈值
            } else {
                newCap = DEFAULT_INITIAL_CAPACITY;
                newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
            }
        }
        
        /*
         * 至此，如果newThr==0，则可能有以下两种情形：
         * 1.哈希数组已经初始化，且哈希数组的容量还未超出最大容量，
         *   但是，在执行了加倍操作后，哈希数组的容量达到了上限
         * 2.哈希数组还未初始化，但在实例化HashMap时指定了初始容量
         */
        if(newThr == 0) {
            float ft = (float) newCap * loadFactor;
            newThr = newCap<MAXIMUM_CAPACITY && ft<(float) MAXIMUM_CAPACITY
                ? (int) ft  // 针对第二种情况，将阈值更新为初始容量*装载因子
                : Integer.MAX_VALUE;    // 针对第一种情况，将阈值更新为最大值
        }
        
        // 更新阈值
        threshold = newThr;
        
        // 至此，说明哈希数组需要初始化，或者需要扩容，即创建新的哈希数组
        @SuppressWarnings({"rawtypes", "unchecked"})
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        
        table = newTab;
        
        // 如果是扩容，则需要将旧元素复制到新容器
        if(oldTab != null) {
            for(int j = 0; j<oldCap; ++j) {
                Node<K, V> e = oldTab[j];
                
                // 如果当前哈希槽上存在元素
                if(e != null) {
                    // 置空该哈希槽
                    oldTab[j] = null;
                    
                    // 如果该哈希槽上只有一个元素
                    if(e.next == null) {
                        // 由于总容量变了，所以需要重新哈希
                        newTab[e.hash & (newCap - 1)] = e;
                        
                        // 如果该哈希槽上链接了不止一个元素，且该元素是TreeNode类型
                    } else if(e instanceof TreeNode) {
                        // 拆分红黑树以适应新的容量要求
                        ((TreeNode<K, V>) e).split(this, newTab, j, oldCap);
                        
                        // 如果该哈希槽上链接了不止一个元素，且该元素是普通Node类型
                    } else { // preserve order
                        Node<K, V> loHead = null, loTail = null;
                        Node<K, V> hiHead = null, hiTail = null;
                        Node<K, V> next;
                        
                        // 这里跟split()操作类似，将原有的结点分成两拨，以适应新的容量需求
                        do {
                            next = e.next;
                            
                            if((e.hash & oldCap) == 0) {
                                if(loTail == null) {
                                    loHead = e;
                                } else {
                                    loTail.next = e;
                                }
                                loTail = e;
                            } else {
                                if(hiTail == null) {
                                    hiHead = e;
                                } else {
                                    hiTail.next = e;
                                }
                                hiTail = e;
                            }
                        } while((e = next) != null);
                        
                        if(loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        
                        if(hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        
        return newTab;
    }
    
    /**
     * Replaces all linked nodes in bin at index for given hash unless
     * table is too small, in which case resizes instead.
     */
    /*
     * 观察哈希槽（链）上处于波动期的元素，以决定下一步是扩容还是将链表转换为红黑树
     *
     * tab：待转换的链表
     * hash：某元素的哈希值
     */
    final void treeifyBin(Node<K, V>[] tab, int hash) {
        int n;
        
        // 哈希数组的容量还未达到形成一棵红黑树的最低要求
        if(tab == null || (n = tab.length)<MIN_TREEIFY_CAPACITY) {
            // 初始化哈希数组，或者对哈希数组扩容，返回新的哈希数组
            resize();
            
            // 满足从链表转换为红黑树的要求
        } else {
            // 计算哈希槽索引
            int index = (n - 1) & hash;
            
            Node<K, V> e = tab[index];
            
            if(e!= null) {
                TreeNode<K, V> hd = null, tl = null;
                
                do {
                    // 将元素e从链表结点转换为红黑树结点
                    TreeNode<K, V> p = replacementTreeNode(e, null);
                    
                    if(tl == null) {
                        hd = p;
                    } else {
                        p.prev = tl;
                        tl.next = p;
                    }
                    
                    tl = p;
                } while((e = e.next) != null);
                
                if((tab[index] = hd) != null) {
                    // 遍历hd链表上的所有元素，创建一棵红黑树
                    hd.treeify(tab);
                }
            }
        }
    }
    
    /* These methods are also used when serializing HashSets */
    // 获取HashMap当前使用的装载因子
    final float loadFactor() {
        return loadFactor;
    }
    
    // 获取哈希数组的容量
    final int capacity() {
        return (table != null)
            ? table.length
            : (threshold>0) ? threshold : DEFAULT_INITIAL_CAPACITY;
    }
    
    /**
     * Implements Map.remove and related methods.
     *
     * @param hash       hash for key
     * @param key        the key
     * @param value      the value to match if matchValue, else ignored
     * @param matchValue if true only remove if value is equal
     * @param movable    if false do not move other nodes while removing
     *
     * @return the node, or null if none
     */
    /*
     * 从HashMap中移除指定的元素，并返回刚刚移除的元素（移除失败返回null）
     *
     * matchValue 移除元素时是否需要考虑value的匹配问题
     * movable    移除元素后如果红黑树根结点发生了变化，那么是否需要改变结点在链表上的顺序
     */
    final Node<K, V> removeNode(int hash, Object key, Object value, boolean matchValue, boolean movable) {
        Node<K, V>[] tab;
        Node<K, V> p;
        int n, index;
        if((tab = table) != null && (n = tab.length)>0 && (p = tab[index = (n - 1) & hash]) != null) {
            Node<K, V> node = null, e;
            K k;
            V v;
            
            /* 根据给定的key和hash（由key计算而来）查找对应的（同位）元素 */
            
            if(p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) {
                node = p;
            } else if((e = p.next) != null) {
                if(p instanceof TreeNode) {
                    node = ((TreeNode<K, V>) p).getTreeNode(hash, key);
                } else {
                    do {
                        if(e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while((e = e.next) != null);
                }
            }
            
            /*
             * 从HashMap中移除匹配的元素
             * 可能只需要匹配hash和key就行，也可能还要匹配value，这取决于matchValue参数
             */
            
            if(node != null && (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))) {
                if(node instanceof TreeNode) {
                    ((TreeNode<K, V>) node).removeTreeNode(this, tab, movable);
                } else if(node == p) {
                    tab[index] = node.next;
                } else {
                    p.next = node.next;
                }
                
                ++modCount;
                
                --size;
                
                afterNodeRemoval(node);
                
                return node;
            }
        }
        
        return null;
    }
    
    
    
    
    
    
    /**
     * Basic hash bin node, used for most entries.  (See below for
     * TreeNode subclass, and in LinkedHashMap for its Entry subclass.)
     */
    // HashMap中的普通结点信息，每个Node代表一个元素，里面包含了key和value的信息
    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next;
        
        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
        
        public final K getKey() {
            return key;
        }
        
        public final V getValue() {
            return value;
        }
        
        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }
        
        public final String toString() {
            return key + "=" + value;
        }
        
        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }
        
        public final boolean equals(Object o) {
            if(o == this) {
                return true;
            }
            
            if(o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
            }
            
            return false;
        }
    }
    
    /**
     * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn
     * extends Node) so can be used as extension of either regular or
     * linked node.
     */
    // HashMap中的红黑树结点
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        boolean red;
        
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        
        
        // 仍然需要维护next链接
        TreeNode(int hash, K key, V val, Node<K, V> next) {
            super(hash, key, val, next);
        }
        
        
        
        /*▼ 创建/反创建 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        /**
         * Forms tree of the nodes linked from this node.
         */
        /*
         * 遍历当前TreeNode链表上的所有元素，创建一棵红黑树
         *
         * 创建完成后，当前的TreeNode元素以及后面链接的那串元素具有链表与红黑树两种结构
         * 其中，链表是靠着结点的prev和next来链接的
         * 除此之外，由于创建红黑树的过程中，根结点可能会发生动态变化，
         * 所以，在红黑树创建完成后，当前的TreeNode元素可能是，也可能不是红黑树的根结点，
         * 如果当前的TreeNode元素不是红黑树的根结点，那么它在原来链表上的相对位置也会发生变化（通过moveRootToFront()方法调整）
         */
        final void treeify(Node<K, V>[] tab) {
            TreeNode<K, V> root = null;
            
            // 遍历待插入元素
            for(TreeNode<K, V> x = this, next; x != null; x = next) {
                next = (TreeNode<K, V>) x.next;
                x.left = x.right = null;
                
                // 创建根结点
                if(root == null) {
                    x.parent = null;    // 根结点parent为null
                    x.red = false;      // 根结点为黑色
                    root = x;           // root指向根结点
                } else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    
                    // 遍历红黑树，将k所代表的元素插入到红黑树中合适的位置
                    for(TreeNode<K, V> p = root; ; ) {
                        int dir, ph;
                        K pk = p.key;
                        
                        /*
                         * 判断待插入元素的插入位置，是向左查找？还是向右查找？
                         * 判断依据依次为：
                         *                                                                                                          ┌──不相同，可以得出结果 ★
                         *           ┌──不相同，直接得出结果 ★                                    ┌──类型一致───使用compareTo()方法判断──┤
                         * 判断hash ──┤                           ┌──有效───检查参与比较的元素类型──┤                                   └───相同───┐
                         *           └──相同───检查Comparable接口──┤                             └───类型不一致───────────────────────────────────┼──调用tieBreakOrder()方法，可以得出结果 ★
                         *                                       └───无效──────────────────────────────────────────────────────────────────────┘
                         *
                         *
                         */
                        
                        // 待插入结点的hash值较小，则向左搜寻合适的插入位置
                        if((ph = p.hash)>h) {
                            dir = -1;
                            
                            // 待插入结点的hash值较大，则向右搜寻合适的插入位置
                        } else if(ph<h) {
                            dir = 1;
                            
                            // 待插入结点的hash值出现了雷同
                        } else if((kc == null && (kc = comparableClassFor(k)) == null)  // 如果k（的类类型）没有实现“有效”的Comparable接口
                            || (dir = compareComparables(kc, k, pk)) == 0) {    // 或者，待插入元素与已存在元素类型不同，无法比较
                            // 终极判等方式：使用对象的类名和对象本身的哈希码去比较两个对象的大小，而且返回值一定是-1或者是1
                            dir = tieBreakOrder(k, pk);
                        }
                        
                        // 向指定的位置插入结点
                        TreeNode<K, V> xp = p;
                        if((p = (dir<=0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if(dir<=0) {
                                xp.left = x;
                            } else {
                                xp.right = x;
                            }
                            
                            // 将元素x插入到红黑树root后，可能会破坏其平衡性，所以这里需要做出调整，保持红黑树的平衡
                            root = balanceInsertion(root, x);
                            
                            break;
                        }
                    }
                }
            }
            
            /*
             * 由于红黑树根结点可能发生了变化，所以需要检查/调整链表顺序：
             * 将root放入哈希数组tab的合适位置，且将root指向的元素移动到链表的头部
             */
            moveRootToFront(tab, root);
        }
        
        /**
         * Returns a list of non-TreeNodes replacing those linked from this node.
         */
        // 遍历当前TreeNode红黑树上所有元素，创建一个链表
        final Node<K, V> untreeify(HashMap<K, V> map) {
            Node<K, V> hd = null, tl = null;
            
            for(Node<K, V> q = this; q != null; q = q.next) {
                Node<K, V> p = map.replacementNode(q, null);
                if(tl == null) {
                    hd = p;
                } else {
                    tl.next = p;
                }
                tl = p;
            }
            
            return hd;
        }
        
        /*▲ 创建/反创建 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        
        /*▼ 插入/删除 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
        
        /**
         * Tree version of putVal.
         */
        // 向当前红黑树中插入元素
        final TreeNode<K, V> putTreeVal(HashMap<K, V> map, Node<K, V>[] tab, int h, K k, V v) {
            Class<?> kc = null;
            
            boolean searched = false;
            
            // 获取当前红黑树的根结点
            TreeNode<K, V> root = (parent != null) ? root() : this;
            
            for(TreeNode<K, V> p = root; ; ) {
                int dir, ph;
                K pk;
                if((ph = p.hash)>h) {
                    dir = -1;
                } else if(ph<h) {
                    dir = 1;
                } else if((pk = p.key) == k || (k != null && k.equals(pk))) {
                    return p;
                } else if((kc == null && (kc = comparableClassFor(k)) == null) || (dir = compareComparables(kc, k, pk)) == 0) {
                    if(!searched) {
                        TreeNode<K, V> q, ch;
                        searched = true;
                        if(((ch = p.left) != null && (q = ch.find(h, k, kc)) != null) || ((ch = p.right) != null && (q = ch.find(h, k, kc)) != null)) {
                            return q;
                        }
                    }
                    
                    // 终极判等方式：使用对象的类名和对象本身的哈希码去比较两个对象的大小，而且返回值一定是-1或者是1
                    dir = tieBreakOrder(k, pk);
                }
                
                TreeNode<K, V> xp = p;
                if((p = (dir<=0) ? p.left : p.right) == null) {
                    Node<K, V> xpn = xp.next;
                    TreeNode<K, V> x = map.newTreeNode(h, k, v, xpn);
                    if(dir<=0) {
                        xp.left = x;
                    } else {
                        xp.right = x;
                    }
                    xp.next = x;
                    x.parent = x.prev = xp;
                    if(xpn != null) {
                        ((TreeNode<K, V>) xpn).prev = x;
                    }
                    
                    // 将元素x插入到红黑树root后，可能会破坏其平衡性，所以这里需要做出调整，保持红黑树的平衡
                    TreeNode<K, V> r = balanceInsertion(root, x);
                    
                    /*
                     * 由于红黑树根结点可能发生了变化，所以需要检查/调整链表顺序：
                     * 将r放入哈希数组tab的合适位置，且将r指向的元素移动到链表的头部
                     */
                    moveRootToFront(tab, r);
                    
                    return null;
                }
            }
        }
        
        /**
         * Removes the given node, that must be present before this call.
         * This is messier than typical red-black deletion code because we
         * cannot swap the contents of an interior node with a leaf
         * successor that is pinned by "next" pointers that are accessible
         * independently during traversal. So instead we swap the tree
         * linkages. If the current tree appears to have too few nodes,
         * the bin is converted back to a plain bin. (The test triggers
         * somewhere between 2 and 6 nodes, depending on tree structure).
         */
        // 将元素从红黑树中移除
        final void removeTreeNode(HashMap<K, V> map, Node<K, V>[] tab, boolean movable) {
            int n;
            if(tab == null || (n = tab.length) == 0) {
                return;
            }
            
            int index = (n - 1) & hash;
            
            TreeNode<K, V> first = (TreeNode<K, V>) tab[index], root = first, rl;
            TreeNode<K, V> succ = (TreeNode<K, V>) next, pred = prev;
            if(pred == null) {
                tab[index] = first = succ;
            } else {
                pred.next = succ;
            }
            
            if(succ != null) {
                succ.prev = pred;
            }
            
            if(first == null) {
                return;
            }
            
            if(root.parent != null) {
                // 获取当前红黑树的根结点
                root = root.root();
            }
            
            if(root == null
                || (movable && (root.right == null || (rl = root.left) == null || rl.left == null))) {
                // 遍历红黑树first上所有元素，创建一个链表
                tab[index] = first.untreeify(map);  // too small
                return;
            }
            
            TreeNode<K, V> p = this, pl = left, pr = right, replacement;
            if(pl != null && pr != null) {
                TreeNode<K, V> s = pr, sl;
                
                // find successor
                while((sl = s.left) != null) {
                    s = sl;
                }
                
                boolean c = s.red;
                s.red = p.red;
                p.red = c; // swap colors
                TreeNode<K, V> sr = s.right;
                TreeNode<K, V> pp = p.parent;
                if(s == pr) { // p was s's direct parent
                    p.parent = s;
                    s.right = p;
                } else {
                    TreeNode<K, V> sp = s.parent;
                    if((p.parent = sp) != null) {
                        if(s == sp.left) {
                            sp.left = p;
                        } else {
                            sp.right = p;
                        }
                    }
                    
                    if((s.right = pr) != null) {
                        pr.parent = s;
                    }
                }
                
                p.left = null;
                if((p.right = sr) != null) {
                    sr.parent = p;
                }
                
                if((s.left = pl) != null) {
                    pl.parent = s;
                }
                
                if((s.parent = pp) == null) {
                    root = s;
                } else if(p == pp.left) {
                    pp.left = s;
                } else {
                    pp.right = s;
                }
                
                if(sr != null) {
                    replacement = sr;
                } else {
                    replacement = p;
                }
            } else if(pl != null) {
                replacement = pl;
            } else if(pr != null) {
                replacement = pr;
            } else {
                replacement = p;
            }
            
            if(replacement != p) {
                TreeNode<K, V> pp = replacement.parent = p.parent;
                if(pp == null) {
                    root = replacement;
                } else if(p == pp.left) {
                    pp.left = replacement;
                } else {
                    pp.right = replacement;
                }
                
                p.left = p.right = p.parent = null;
            }
            
            TreeNode<K, V> r = p.red
                ? root
                : balanceDeletion(root, replacement);   // 将元素x从红黑树root移除后，可能会破坏其平衡性，所以这里需要做出调整，保持红黑树的平衡
            
            if(replacement == p) {  // detach
                TreeNode<K, V> pp = p.parent;
                p.parent = null;
                if(pp != null) {
                    if(p == pp.left) {
                        pp.left = null;
                    } else if(p == pp.right) {
                        pp.right = null;
                    }
                }
            }
            
            if(movable) {
                /*
                 * 由于红黑树根结点可能发生了变化，所以需要检查/调整链表顺序：
                 * 将r放入哈希数组tab的合适位置，且将r指向的元素移动到链表的头部
                 */
                moveRootToFront(tab, r);
            }
        }
        
        // 将元素x插入到红黑树root后，可能会破坏其平衡性，所以这里需要做出调整，保持红黑树的平衡
        static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root, TreeNode<K, V> x) {
            // 新插入的结点一律先设置为红色
            x.red = true;
            
            for(TreeNode<K, V> xp, xpp, xppl, xppr; ; ) {
                if((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                
                if(!xp.red || (xpp = xp.parent) == null) {
                    return root;
                }
                
                if(xp == (xppl = xpp.left)) {
                    if((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if(x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        
                        if(xp != null) {
                            xp.red = false;
                            if(xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else {
                    if(xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if(x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if(xp != null) {
                            xp.red = false;
                            if(xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }
        
        // 将元素x从红黑树root移除后，可能会破坏其平衡性，所以这里需要做出调整，保持红黑树的平衡
        static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root, TreeNode<K, V> x) {
            for(TreeNode<K, V> xp, xpl, xpr; ; ) {
                if(x == null || x == root) {
                    return root;
                } else if((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if(x.red) {
                    x.red = false;
                    return root;
                } else if((xpl = xp.left) == x) {
                    if((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    
                    if(xpr == null) {
                        x = xp;
                    } else {
                        TreeNode<K, V> sl = xpr.left, sr = xpr.right;
                        if((sr == null || !sr.red) && (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if(sr == null || !sr.red) {
                                if(sl != null) {
                                    sl.red = false;
                                }
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ? null : xp.right;
                            }
                            
                            if(xpr != null) {
                                xpr.red = (xp != null) && xp.red;
                                if((sr = xpr.right) != null) {
                                    sr.red = false;
                                }
                            }
                            
                            if(xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            
                            x = root;
                        }
                    }
                } else { // symmetric
                    if(xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    
                    if(xpl == null) {
                        x = xp;
                    } else {
                        TreeNode<K, V> sl = xpl.left, sr = xpl.right;
                        if((sl == null || !sl.red) && (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if(sl == null || !sl.red) {
                                if(sr != null) {
                                    sr.red = false;
                                }
                                
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ? null : xp.left;
                            }
                            
                            if(xpl != null) {
                                xpl.red = (xp != null) && xp.red;
                                if((sl = xpl.left) != null) {
                                    sl.red = false;
                                }
                            }
                            
                            if(xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            
                            x = root;
                        }
                    }
                }
            }
        }
        
        // 左旋
        static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root, TreeNode<K, V> p) {
            TreeNode<K, V> r, pp, rl;
            
            if(p != null && (r = p.right) != null) {
                if((rl = p.right = r.left) != null) {
                    rl.parent = p;
                }
                
                if((pp = r.parent = p.parent) == null) {
                    (root = r).red = false;
                } else if(pp.left == p) {
                    pp.left = r;
                } else {
                    pp.right = r;
                }
                
                r.left = p;
                p.parent = r;
            }
            
            return root;
        }
        
        // 右旋
        static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root, TreeNode<K, V> p) {
            TreeNode<K, V> l, pp, lr;
            
            if(p != null && (l = p.left) != null) {
                if((lr = p.left = l.right) != null) {
                    lr.parent = p;
                }
                
                if((pp = l.parent = p.parent) == null) {
                    (root = l).red = false;
                } else if(pp.right == p) {
                    pp.right = l;
                } else {
                    pp.left = l;
                }
                
                l.right = p;
                p.parent = l;
            }
            
            return root;
        }
        
        /*▲ 插入/删除 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
        
        
        /**
         * Ensures that the given root is the first node of its bin.
         */
        /*
         * 在红黑树根结点可能发生变化时，需要检查/调整链表顺序：
         * 将root放入哈希数组tab的合适位置，且将root指向的元素移动到链表的头部
         */
        static <K, V> void moveRootToFront(Node<K, V>[] tab, TreeNode<K, V> root) {
            int n;
            
            if(root != null && tab != null && (n = tab.length)>0) {
                int index = (n - 1) & root.hash;
                
                TreeNode<K, V> first = (TreeNode<K, V>) tab[index];
                
                if(root != first) {
                    tab[index] = root;
                    
                    TreeNode<K, V> rp = root.prev;
                    Node<K, V> rn = root.next;
                    
                    if(rn != null) {
                        ((TreeNode<K, V>) rn).prev = rp;
                    }
                    
                    if(rp != null) {
                        rp.next = rn;
                    }
                    
                    if(first != null) {
                        first.prev = root;
                    }
                    
                    root.next = first;
                    root.prev = null;
                }
                
                assert checkInvariants(root);
            }
        }
        
        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        // 终极判等方式：使用对象的类名和对象本身的哈希码去比较两个对象的大小，而且返回值一定是-1或者是1
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if(a == null
                || b == null
                || (d = a.getClass().getName().compareTo(b.getClass().getName())) == 0) {
                d = (System.identityHashCode(a)<=System.identityHashCode(b) ? -1 : 1);
            }
            
            return d;
        }
        
        /**
         * Returns root of tree containing this node.
         */
        // 获取当前红黑树的根结点
        final TreeNode<K, V> root() {
            for(TreeNode<K, V> r = this, p; ; ) {
                if((p = r.parent) == null) {
                    return r;
                }
                r = p;
            }
        }
        
        /**
         * Finds the node starting at root p with the given hash and key.
         * The kc argument caches comparableClassFor(key) upon first use
         * comparing keys.
         */
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        final TreeNode<K, V> find(int hash, Object key, Class<?> kc) {
            TreeNode<K, V> p = this;
            do {
                int ph, dir;
                K pk;
                
                TreeNode<K, V> pl = p.left, pr = p.right, q;
                if((ph = p.hash)>hash) {
                    p = pl;
                } else if(ph<hash) {
                    p = pr;
                } else if((pk = p.key) == key || (key != null && key.equals(pk))) {
                    return p;
                } else if(pl == null) {
                    p = pr;
                } else if(pr == null) {
                    p = pl;
                } else if((kc != null || (kc = comparableClassFor(key)) != null)
                    && (dir = compareComparables(kc, key, pk)) != 0) {
                    p = (dir<0) ? pl : pr;
                } else if((q = pr.find(hash, key, kc)) != null) {
                    return q;
                } else {
                    p = pl;
                }
            } while(p != null);
            
            return null;
        }
        
        /**
         * Calls find for root node.
         */
        // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
        final TreeNode<K, V> getTreeNode(int hash, Object key) {
            return ((parent != null) ? root() : this).find(hash, key, null);
        }
        
        /**
         * Splits nodes in a tree bin into lower and upper tree bins,
         * or untreeifies if now too small. Called only from resize;
         * see above discussion about split bits and indices.
         *
         * @param map   the map
         * @param tab   the table for recording bin heads
         * @param index the index of the table being split
         * @param bit   the bit of hash to split on
         */
        // 拆分红黑树以适应新的容量要求，因为扩容会导致index哈希槽处的那些元素进行再哈希，并最多分成两拨（bit为2的冪，一般传入的值是哈希数组旧容量）
        final void split(HashMap<K, V> map, Node<K, V>[] tab, int index, int bit) {
            TreeNode<K, V> b = this;
            
            // Relink into lo and hi lists, preserving order
            TreeNode<K, V> loHead = null, loTail = null;
            TreeNode<K, V> hiHead = null, hiTail = null;
            
            int lc = 0, hc = 0;
            
            for(TreeNode<K, V> e = b, next; e != null; e = next) {
                next = (TreeNode<K, V>) e.next;
                
                e.next = null;
                
                // 扩容后，元素e的位置不会改变（低区间）
                if((e.hash & bit) == 0) {
                    if((e.prev = loTail) == null) {
                        loHead = e;
                    } else {
                        loTail.next = e;
                    }
                    loTail = e;
                    ++lc;
                    
                    // 扩容后，元素e会进入新的位置（高区间）
                } else {
                    if((e.prev = hiTail) == null) {
                        hiHead = e;
                    } else {
                        hiTail.next = e;
                    }
                    hiTail = e;
                    ++hc;
                }
            }
            
            if(loHead != null) {
                if(lc<=UNTREEIFY_THRESHOLD) {
                    // 遍历红黑树loHead上所有元素，创建一个链表
                    tab[index] = loHead.untreeify(map);
                } else {
                    tab[index] = loHead;
                    
                    if(hiHead != null) {
                        // 遍历loHead链表上的所有元素，创建一棵红黑树
                        loHead.treeify(tab);
                    }
                    
                    // (else is already treeified)
                }
            }
            
            if(hiHead != null) {
                if(hc<=UNTREEIFY_THRESHOLD) {
                    // 遍历红黑树hiHead上所有元素，创建一个链表
                    tab[index + bit] = hiHead.untreeify(map);
                } else {
                    tab[index + bit] = hiHead;
                    
                    if(loHead != null) {
                        // 遍历hiHead链表上的所有元素，创建一棵红黑树
                        hiHead.treeify(tab);
                    }
                }
            }
        }
        
        
        /**
         * Recursive invariant check
         */
        static <K, V> boolean checkInvariants(TreeNode<K, V> t) {
            TreeNode<K, V> tp = t.parent, tl = t.left, tr = t.right, tb = t.prev, tn = (TreeNode<K, V>) t.next;
            
            if(tb != null && tb.next != t) {
                return false;
            }
            
            if(tn != null && tn.prev != t) {
                return false;
            }
            
            if(tp != null && t != tp.left && t != tp.right) {
                return false;
            }
            
            if(tl != null && (tl.parent != t || tl.hash>t.hash)) {
                return false;
            }
            
            if(tr != null && (tr.parent != t || tr.hash<t.hash)) {
                return false;
            }
            
            if(t.red && tl != null && tl.red && tr != null && tr.red) {
                return false;
            }
            
            if(tl != null && !checkInvariants(tl)) {
                return false;
            }
            
            return tr == null || checkInvariants(tr);
        }
    }
    
    
    
    // HashMap中key的集合
    final class KeySet extends AbstractSet<K> {
        
        public final int size() {
            return size;
        }
        
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
        
        public final void clear() {
            HashMap.this.clear();
        }
        
        public final boolean contains(Object o) {
            return containsKey(o);
        }
        
        public final Iterator<K> iterator() {
            return new KeyIterator();
        }
        
        public final Spliterator<K> spliterator() {
            return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        
        public final void forEach(Consumer<? super K> action) {
            Node<K, V>[] tab;
            if(action == null) {
                throw new NullPointerException();
            }
            
            if(size>0 && (tab = table) != null) {
                int mc = modCount;
                for(Node<K, V> e : tab) {
                    for(; e != null; e = e.next) {
                        action.accept(e.key);
                    }
                }
                
                if(modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }
    
    // HashMap中value的集合
    final class Values extends AbstractCollection<V> {
        
        public final int size() {
            return size;
        }
        
        public final void clear() {
            HashMap.this.clear();
        }
        
        public final boolean contains(Object o) {
            return containsValue(o);
        }
        
        public final Iterator<V> iterator() {
            return new ValueIterator();
        }
        
        public final Spliterator<V> spliterator() {
            return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        
        public final void forEach(Consumer<? super V> action) {
            Node<K, V>[] tab;
            if(action == null) {
                throw new NullPointerException();
            }
            
            if(size>0 && (tab = table) != null) {
                int mc = modCount;
                for(Node<K, V> e : tab) {
                    for(; e != null; e = e.next)
                        action.accept(e.value);
                }
                
                if(modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }
    
    // HashMap中key-value的集合，Entry的本质就是Node
    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public final int size() {
            return size;
        }
        
        public final void clear() {
            HashMap.this.clear();
        }
        
        public final boolean contains(Object o) {
            if(!(o instanceof Map.Entry)) {
                return false;
            }
            
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object key = e.getKey();
            
            // 根据给定的key和hash（由key计算而来）查找对应的（同位）元素，如果找不到，则返回null
            Node<K, V> candidate = getNode(hash(key), key);
            
            return candidate != null && candidate.equals(e);
        }
        
        public final boolean remove(Object o) {
            if(o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            
            return false;
        }
        
        public final Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }
        
        public final Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        
        public final void forEach(Consumer<? super Map.Entry<K, V>> action) {
            Node<K, V>[] tab;
            if(action == null) {
                throw new NullPointerException();
            }
            
            if(size>0 && (tab = table) != null) {
                int mc = modCount;
                for(Node<K, V> e : tab) {
                    for(; e != null; e = e.next)
                        action.accept(e);
                }
                
                if(modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }
    
    
    
    // HashMap迭代器
    abstract class HashIterator {
        // 当前正在处理元素
        Node<K, V> next;        // next entry to return
        // 下次即将即将处理的元素
        Node<K, V> current;     // current entry
        
        // 记录HashMap当前的修改次数，后续遍历时如果发现修改次数发生了变化，则返回失败信息
        int expectedModCount;   // for fast-fail
        
        // 下一个待处理元素所在的哈希槽索引
        int index;              // current slot
        
        HashIterator() {
            expectedModCount = modCount;
            Node<K, V>[] t = table;
            current = next = null;
            index = 0;
            if(t != null && size>0) {
                // advance to first entry
                do {
                } while(index<t.length && (next = t[index++]) == null);
            }
        }
        
        public final boolean hasNext() {
            return next != null;
        }
        
        final Node<K, V> nextNode() {
            Node<K, V>[] t;
            Node<K, V> e = next;
            
            if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            if(e == null) {
                throw new NoSuchElementException();
            }
            
            if((next = (current = e).next) == null && (t = table) != null) {
                do {
                } while(index<t.length && (next = t[index++]) == null);
            }
            
            return e;
        }
        
        public final void remove() {
            Node<K, V> p = current;
            
            if(p == null) {
                throw new IllegalStateException();
            }
            
            if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            current = null;
            
            removeNode(p.hash, p.key, null, false, false);
            
            expectedModCount = modCount;
        }
    }
    
    // key的迭代器
    final class KeyIterator extends HashIterator implements Iterator<K> {
        public final K next() {
            return nextNode().key;
        }
    }
    
    // value的迭代器
    final class ValueIterator extends HashIterator implements Iterator<V> {
        public final V next() {
            return nextNode().value;
        }
    }
    
    // key-value对的迭代器
    final class EntryIterator extends HashIterator implements Iterator<Map.Entry<K, V>> {
        public final Map.Entry<K, V> next() {
            return nextNode();
        }
    }
    
    
    
    // HashMap的可分割迭代器
    static class HashMapSpliterator<K, V> {
        final HashMap<K, V> map;
        Node<K, V> current;          // current node
        int index;                  // current index, modified on advance/split
        int fence;                  // one past last index
        int est;                    // size estimate
        int expectedModCount;       // for comodification checks
        
        HashMapSpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }
        
        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
        
        // initialize fence and size on first use
        final int getFence() {
            int hi;
            
            if((hi = fence)<0) {
                HashMap<K, V> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                Node<K, V>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            
            return hi;
        }
    }
    
    // key的可分割迭代器
    static final class KeySpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }
        
        // 从容器的指定范围切割一段元素，将其打包到Spliterator后返回，特征值不变（这里会切割前一半元素出来）
        public KeySpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null : new KeySpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
        }
        
        // 对容器中的单个当前元素执行择取操作
        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if(action == null) {
                throw new NullPointerException();
            }
            
            Node<K, V>[] tab = map.table;
            
            if(tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while(current != null || index<hi) {
                    if(current == null) {
                        current = tab[index++];
                    } else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if(map.modCount != expectedModCount) {
                            throw new ConcurrentModificationException();
                        }
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        // 遍历容器内每个元素，在其上执行相应的择取操作
        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if(action == null) {
                throw new NullPointerException();
            }
            
            HashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if((hi = fence)<0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else {
                mc = expectedModCount;
            }
            
            if(tab != null && tab.length >= hi && (i = index) >= 0 && (i<(index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if(p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while(p != null || i<hi);
                
                if(m.modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
        
        public int characteristics() {
            return (fence<0 || est == map.size ? Spliterator.SIZED : 0) | Spliterator.DISTINCT;
        }
    }
    
    // value的可分割迭代器
    static final class ValueSpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }
        
        // 从容器的指定范围切割一段元素，将其打包到Spliterator后返回，特征值不变（这里会切割前一半元素出来）
        public ValueSpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null : new ValueSpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
        }
        
        // 对容器中的单个当前元素执行择取操作
        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if(action == null) {
                throw new NullPointerException();
            }
            
            Node<K, V>[] tab = map.table;
            if(tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while(current != null || index<hi) {
                    if(current == null) {
                        current = tab[index++];
                    } else {
                        V v = current.value;
                        current = current.next;
                        action.accept(v);
                        if(map.modCount != expectedModCount) {
                            throw new ConcurrentModificationException();
                        }
                        
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        // 遍历容器内每个元素，在其上执行相应的择取操作
        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if(action == null) {
                throw new NullPointerException();
            }
            HashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if((hi = fence)<0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else {
                mc = expectedModCount;
            }
            
            if(tab != null && tab.length >= hi && (i = index) >= 0 && (i<(index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if(p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.value);
                        p = p.next;
                    }
                } while(p != null || i<hi);
                
                if(m.modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
        
        public int characteristics() {
            return (fence<0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }
    
    // key-value的可分割迭代器
    static final class EntrySpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }
        
        // 从容器的指定范围切割一段元素，将其打包到Spliterator后返回，特征值不变（这里会切割前一半元素出来）
        public EntrySpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null : new EntrySpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
        }
        
        // 对容器中的单个当前元素执行择取操作
        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
            int hi;
            if(action == null) {
                throw new NullPointerException();
            }
            Node<K, V>[] tab = map.table;
            if(tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while(current != null || index<hi) {
                    if(current == null) {
                        current = tab[index++];
                    } else {
                        Node<K, V> e = current;
                        current = current.next;
                        action.accept(e);
                        if(map.modCount != expectedModCount) {
                            throw new ConcurrentModificationException();
                        }
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        // 遍历容器内每个元素，在其上执行相应的择取操作
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            int i, hi, mc;
            if(action == null) {
                throw new NullPointerException();
            }
            
            HashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if((hi = fence)<0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else {
                mc = expectedModCount;
            }
            
            if(tab != null && tab.length >= hi && (i = index) >= 0 && (i<(index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if(p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while(p != null || i<hi);
                
                if(m.modCount != mc) {
                    throw new ConcurrentModificationException();
                }
            }
        }
        
        public int characteristics() {
            return (fence<0 || est == map.size ? Spliterator.SIZED : 0) | Spliterator.DISTINCT;
        }
    }
    
}
