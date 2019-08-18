/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import jdk.internal.vm.annotation.Stable;

/**
 * Container class for immutable collections. Not part of the public API.
 * Mainly for namespace management and shared infrastructure.
 *
 * Serial warnings are suppressed throughout because all implementation
 * classes use a serial proxy and thus have no need to declare serialVersionUID.
 */
// 只读容器，禁止写操作，内部使用
@SuppressWarnings("serial")
class ImmutableCollections {
    
    /**
     * A "salt" value used for randomizing iteration order. This is initialized once
     * and stays constant for the lifetime of the JVM. It need not be truly random, but
     * it needs to vary sufficiently from one run to the next so that iteration order
     * will vary between JVM runs.
     */
    static final int SALT;
    
    static {
        long nt = System.nanoTime();
        SALT = (int) ((nt >>> 32) ^ nt);
    }
    
    /**
     * The reciprocal of load factor. Given a number of elements
     * to store, multiply by this factor to get the table size.
     */
    static final int EXPAND_FACTOR = 2;
    
    /** No instances. */
    private ImmutableCollections() {
    }
    
    // 对只读容器执行写操作时，会抛出异常
    static UnsupportedOperationException uoe() {
        return new UnsupportedOperationException();
    }
    
    // 只读的一元容器，禁止写操作
    static abstract class AbstractImmutableCollection<E> extends AbstractCollection<E> {
        // all mutating methods throw UnsupportedOperationException
        @Override
        public boolean add(E e) {
            throw uoe();
        }
        
        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw uoe();
        }
        
        @Override
        public void clear() {
            throw uoe();
        }
        
        @Override
        public boolean remove(Object o) {
            throw uoe();
        }
        
        @Override
        public boolean removeAll(Collection<?> c) {
            throw uoe();
        }
        
        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            throw uoe();
        }
        
        @Override
        public boolean retainAll(Collection<?> c) {
            throw uoe();
        }
    }
    
    
    /*▼ List ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回空集(只读的List容器)
    @SuppressWarnings("unchecked")
    static <E> List<E> emptyList() {
        return (List<E>) ListN.EMPTY_LIST;
    }
    
    /* make a copy, short-circuiting based on implementation class */
    // 元素复制
    @SuppressWarnings("unchecked")
    static <E> List<E> listCopy(Collection<? extends E> coll) {
        if(coll instanceof AbstractImmutableList && coll.getClass() != SubList.class) {
            return (List<E>) coll;
        } else {
            return (List<E>) List.of(coll.toArray());
        }
    }
    
    // List迭代器
    static final class ListItr<E> implements ListIterator<E> {
        
        @Stable
        private final List<E> list; // 待遍历的数据集
        
        @Stable
        private final int size;     // 游标终点
        
        private int cursor;         // 指向下一个待遍历元素的游标
        
        @Stable
        private final boolean isListIterator;   // 是否启用ListIterator接口的方法
        
        ListItr(List<E> list, int size) {
            this.list = list;
            this.size = size;
            this.cursor = 0;
            isListIterator = false;
        }
        
        ListItr(List<E> list, int size, int index) {
            this.list = list;
            this.size = size;
            this.cursor = index;
            isListIterator = true;
        }
        
        public boolean hasNext() {
            return cursor != size;
        }
        
        public E next() {
            try {
                int i = cursor;
                E next = list.get(i);
                cursor = i + 1;
                return next;
            } catch(IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }
        
        public void remove() {
            throw uoe();
        }
        
        public boolean hasPrevious() {
            if(!isListIterator) {
                throw uoe();
            }
            return cursor != 0;
        }
        
        public E previous() {
            if(!isListIterator) {
                throw uoe();
            }
            
            try {
                int i = cursor - 1;
                E previous = list.get(i);
                cursor = i;
                return previous;
            } catch(IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }
        
        public int nextIndex() {
            if(!isListIterator) {
                throw uoe();
            }
            return cursor;
        }
        
        public int previousIndex() {
            if(!isListIterator) {
                throw uoe();
            }
            return cursor - 1;
        }
        
        public void set(E e) {
            throw uoe();
        }
        
        public void add(E e) {
            throw uoe();
        }
    }
    
    static final class SubList<E> extends AbstractImmutableList<E> implements RandomAccess {
        
        @Stable
        private final List<E> root;
        
        @Stable
        private final int offset;
        
        @Stable
        private final int size;
        
        private SubList(List<E> root, int offset, int size) {
            this.root = root;
            this.offset = offset;
            this.size = size;
        }
        
        public E get(int index) {
            Objects.checkIndex(index, size);
            return root.get(offset + index);
        }
        
        public int size() {
            return size;
        }
        
        public Iterator<E> iterator() {
            return new ListItr<>(this, size());
        }
        
        public ListIterator<E> listIterator(int index) {
            rangeCheck(index);
            return new ListItr<>(this, size(), index);
        }
        
        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return SubList.fromSubList(this, fromIndex, toIndex);
        }
        
        /**
         * Constructs a sublist of another SubList.
         */
        static <E> SubList<E> fromSubList(SubList<E> parent, int fromIndex, int toIndex) {
            return new SubList<>(parent.root, parent.offset + fromIndex, toIndex - fromIndex);
        }
        
        /**
         * Constructs a sublist of an arbitrary AbstractImmutableList, which is
         * not a SubList itself.
         */
        static <E> SubList<E> fromList(List<E> list, int fromIndex, int toIndex) {
            return new SubList<>(list, fromIndex, toIndex - fromIndex);
        }
        
        private void rangeCheck(int index) {
            if(index<0 || index>size) {
                throw outOfBounds(index);
            }
        }
    }
    
    // 只读的List容器，禁止写操作
    static abstract class AbstractImmutableList<E> extends AbstractImmutableCollection<E> implements List<E>, RandomAccess {
        
        // all mutating methods throw UnsupportedOperationException
        @Override
        public void add(int index, E element) {
            throw uoe();
        }
        
        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            throw uoe();
        }
        
        @Override
        public E remove(int index) {
            throw uoe();
        }
        
        @Override
        public E set(int index, E element) {
            throw uoe();
        }
        
        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            throw uoe();
        }
        
        @Override
        public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }
        
        @Override
        public int indexOf(Object o) {
            Objects.requireNonNull(o);
            for(int i = 0, s = size(); i<s; i++) {
                if(o.equals(get(i))) {
                    return i;
                }
            }
            return -1;
        }
        
        @Override
        public int lastIndexOf(Object o) {
            Objects.requireNonNull(o);
            for(int i = size() - 1; i >= 0; i--) {
                if(o.equals(get(i))) {
                    return i;
                }
            }
            return -1;
        }
        
        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            int size = size();
            subListRangeCheck(fromIndex, toIndex, size);
            return SubList.fromList(this, fromIndex, toIndex);
        }
        
        @Override
        public Iterator<E> iterator() {
            return new ListItr<E>(this, size());
        }
        
        @Override
        public ListIterator<E> listIterator() {
            return listIterator(0);
        }
        
        @Override
        public ListIterator<E> listIterator(final int index) {
            int size = size();
            if(index<0 || index>size) {
                throw outOfBounds(index);
            }
            return new ListItr<E>(this, size, index);
        }
        
        @Override
        public boolean equals(Object o) {
            if(o == this) {
                return true;
            }
            
            if(!(o instanceof List)) {
                return false;
            }
            
            Iterator<?> oit = ((List<?>) o).iterator();
            for(int i = 0, s = size(); i<s; i++) {
                if(!oit.hasNext() || !get(i).equals(oit.next())) {
                    return false;
                }
            }
            return !oit.hasNext();
        }
        
        @Override
        public int hashCode() {
            int hash = 1;
            for(int i = 0, s = size(); i<s; i++) {
                hash = 31 * hash + get(i).hashCode();
            }
            return hash;
        }
        
        @Override
        public void sort(Comparator<? super E> c) {
            throw uoe();
        }
        
        static void subListRangeCheck(int fromIndex, int toIndex, int size) {
            if(fromIndex<0) {
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            }
            if(toIndex>size) {
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            }
            if(fromIndex>toIndex) {
                throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
            }
        }
        
        IndexOutOfBoundsException outOfBounds(int index) {
            return new IndexOutOfBoundsException("Index: " + index + " Size: " + size());
        }
    }
    
    // 只读的List容器(简单版本，最多包含两个元素)，禁止写操作
    static final class List12<E> extends AbstractImmutableList<E> implements Serializable {
        @Stable
        private final E e0;
        @Stable
        private final E e1;
        
        List12(E e0) {
            this.e0 = Objects.requireNonNull(e0);
            this.e1 = null;
        }
        
        List12(E e0, E e1) {
            this.e0 = Objects.requireNonNull(e0);
            this.e1 = Objects.requireNonNull(e1);
        }
        
        @Override
        public E get(int index) {
            if(index == 0) {
                return e0;
            } else if(index == 1 && e1 != null) {
                return e1;
            }
            throw outOfBounds(index);
        }
        
        @Override
        public int size() {
            return e1 != null ? 2 : 1;
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }
        
        private Object writeReplace() {
            if(e1 == null) {
                return new CollSer(CollSer.IMM_LIST, e0);
            } else {
                return new CollSer(CollSer.IMM_LIST, e0, e1);
            }
        }
        
    }
    
    // 只读的List容器(复杂版本，允许包含N个元素)，禁止写操作
    static final class ListN<E> extends AbstractImmutableList<E> implements Serializable {
        
        static final List<?> EMPTY_LIST = new ListN<>();
        
        @Stable
        private final E[] elements;
        
        @SafeVarargs
        ListN(E... input) {
            // copy and check manually to avoid TOCTOU
            @SuppressWarnings("unchecked")
            E[] tmp = (E[]) new Object[input.length]; // implicit nullcheck of input
            for(int i = 0; i<input.length; i++) {
                tmp[i] = Objects.requireNonNull(input[i]);
            }
            elements = tmp;
        }
        
        @Override
        public E get(int index) {
            return elements[index];
        }
        
        @Override
        public boolean isEmpty() {
            return size() == 0;
        }
        
        @Override
        public int size() {
            return elements.length;
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }
        
        private Object writeReplace() {
            return new CollSer(CollSer.IMM_LIST, elements);
        }
        
    }
    
    /*▲ List ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Set ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回空集(只读的Set容器)
    @SuppressWarnings("unchecked")
    static <E> Set<E> emptySet() {
        return (Set<E>) SetN.EMPTY_SET;
    }
    
    // 只读的Set容器，禁止写操作
    static abstract class AbstractImmutableSet<E> extends AbstractImmutableCollection<E> implements Set<E> {
        
        @Override
        public boolean equals(Object o) {
            if(o == this) {
                return true;
            } else if(!(o instanceof Set)) {
                return false;
            }
            
            Collection<?> c = (Collection<?>) o;
            if(c.size() != size()) {
                return false;
            }
            for(Object e : c) {
                if(e == null || !contains(e)) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public abstract int hashCode();
        
    }
    
    // 只读的Set容器(简单版本，最多包含两个元素)，禁止写操作
    static final class Set12<E> extends AbstractImmutableSet<E> implements Serializable {
        @Stable
        final E e0;
        @Stable
        final E e1;
        
        Set12(E e0) {
            this.e0 = Objects.requireNonNull(e0);
            this.e1 = null;
        }
        
        Set12(E e0, E e1) {
            if(e0.equals(Objects.requireNonNull(e1))) { // implicit nullcheck of e0
                throw new IllegalArgumentException("duplicate element: " + e0);
            }
            
            this.e0 = e0;
            this.e1 = e1;
        }
        
        @Override
        public boolean contains(Object o) {
            return o.equals(e0) || o.equals(e1); // implicit nullcheck of o
        }
        
        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {
                private int idx = size();
                
                @Override
                public boolean hasNext() {
                    return idx>0;
                }
                
                @Override
                public E next() {
                    if(idx == 1) {
                        idx = 0;
                        return SALT >= 0 || e1 == null ? e0 : e1;
                    } else if(idx == 2) {
                        idx = 1;
                        return SALT >= 0 ? e1 : e0;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }
        
        @Override
        public int size() {
            return (e1 == null) ? 1 : 2;
        }
        
        @Override
        public int hashCode() {
            return e0.hashCode() + (e1 == null ? 0 : e1.hashCode());
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }
        
        private Object writeReplace() {
            if(e1 == null) {
                return new CollSer(CollSer.IMM_SET, e0);
            } else {
                return new CollSer(CollSer.IMM_SET, e0, e1);
            }
        }
        
    }
    
    /**
     * An array-based Set implementation. The element array must be strictly
     * larger than the size (the number of contained elements) so that at
     * least one null is always present.
     *
     * @param <E> the element type
     */
    // 只读的Set容器(复杂版本，允许包含N个元素)，禁止写操作
    static final class SetN<E> extends AbstractImmutableSet<E> implements Serializable {
        
        static final Set<?> EMPTY_SET = new SetN<>();
        
        @Stable
        final E[] elements;
        @Stable
        final int size;
        
        @SafeVarargs
        @SuppressWarnings("unchecked")
        SetN(E... input) {
            size = input.length; // implicit nullcheck of input
            
            elements = (E[]) new Object[EXPAND_FACTOR * input.length];
            
            for(E e : input) {
                int idx = probe(e); // implicit nullcheck of e
                if(idx >= 0) {
                    throw new IllegalArgumentException("duplicate element: " + e);
                } else {
                    elements[-(idx + 1)] = e;
                }
            }
        }
        
        @Override
        public boolean contains(Object o) {
            Objects.requireNonNull(o);
            return size>0 && probe(o) >= 0;
        }
        
        @Override
        public Iterator<E> iterator() {
            return new SetNIterator();
        }
        
        @Override
        public int size() {
            return size;
        }
        
        @Override
        public int hashCode() {
            int h = 0;
            for(E e : elements) {
                if(e != null) {
                    h += e.hashCode();
                }
            }
            return h;
        }
        
        // returns index at which element is present; or if absent,
        // (-i - 1) where i is location where element should be inserted.
        // Callers are relying on this method to perform an implicit nullcheck of pe
        private int probe(Object pe) {
            int idx = Math.floorMod(pe.hashCode(), elements.length);
            while(true) {
                E ee = elements[idx];
                if(ee == null) {
                    return -idx - 1;
                } else if(pe.equals(ee)) {
                    return idx;
                } else if(++idx == elements.length) {
                    idx = 0;
                }
            }
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }
        
        private Object writeReplace() {
            Object[] array = new Object[size];
            int dest = 0;
            for(Object o : elements) {
                if(o != null) {
                    array[dest++] = o;
                }
            }
            return new CollSer(CollSer.IMM_SET, array);
        }
        
        private final class SetNIterator implements Iterator<E> {
            
            private int remaining;
            
            private int idx;
            
            SetNIterator() {
                remaining = size();
                if(remaining>0) {
                    idx = Math.floorMod(SALT, elements.length);
                }
            }
            
            @Override
            public boolean hasNext() {
                return remaining>0;
            }
            
            @Override
            public E next() {
                if(hasNext()) {
                    E element;
                    // skip null elements
                    while((element = elements[nextIndex()]) == null) {
                    }
                    remaining--;
                    return element;
                } else {
                    throw new NoSuchElementException();
                }
            }
            
            private int nextIndex() {
                int idx = this.idx;
                if(SALT >= 0) {
                    if(++idx >= elements.length) {
                        idx = 0;
                    }
                } else {
                    if(--idx<0) {
                        idx = elements.length - 1;
                    }
                }
                return this.idx = idx;
            }
        }
    }
    
    /*▲ Set ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Map ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回空集(只读的Map容器)
    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> emptyMap() {
        return (Map<K, V>) MapN.EMPTY_MAP;
    }
    
    // 只读的Map容器，禁止写操作
    abstract static class AbstractImmutableMap<K, V> extends AbstractMap<K, V> implements Serializable {
        
        @Override
        public V put(K key, V value) {
            throw uoe();
        }
        
        @Override
        public V putIfAbsent(K key, V value) {
            throw uoe();
        }
        
        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw uoe();
        }
        
        @Override
        public V remove(Object key) {
            throw uoe();
        }
        
        @Override
        public boolean remove(Object key, Object value) {
            throw uoe();
        }
        
        @Override
        public void clear() {
            throw uoe();
        }
        
        @Override
        public V replace(K key, V value) {
            throw uoe();
        }
        
        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            throw uoe();
        }
        
        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> f) {
            throw uoe();
        }
        
        @Override
        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> rf) {
            throw uoe();
        }
        
        @Override
        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> rf) {
            throw uoe();
        }
        
        @Override
        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> rf) {
            throw uoe();
        }
        
        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mf) {
            throw uoe();
        }
        
    }
    
    // 只读的Map容器(简单版本，最多包含一个键值对)，禁止写操作
    static final class Map1<K, V> extends AbstractImmutableMap<K, V> {
        @Stable
        private final K k0;
        @Stable
        private final V v0;
        
        Map1(K k0, V v0) {
            this.k0 = Objects.requireNonNull(k0);
            this.v0 = Objects.requireNonNull(v0);
        }
        
        @Override
        public boolean containsKey(Object o) {
            return o.equals(k0); // implicit nullcheck of o
        }
        
        @Override
        public boolean containsValue(Object o) {
            return o.equals(v0); // implicit nullcheck of o
        }
        
        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            // 将指定的非空的键值对包装为只读对象
            Map.Entry<K, V> entry = new KeyValueHolder<>(k0, v0);
            
            return Set.of(entry);
        }
        
        @Override
        public int hashCode() {
            return k0.hashCode() ^ v0.hashCode();
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }
        
        private Object writeReplace() {
            return new CollSer(CollSer.IMM_MAP, k0, v0);
        }
    }
    
    /**
     * An array-based Map implementation. There is a single array "table" that
     * contains keys and values interleaved: table[0] is kA, table[1] is vA,
     * table[2] is kB, table[3] is vB, etc. The table size must be even. It must
     * also be strictly larger than the size (the number of key-value pairs contained
     * in the map) so that at least one null key is always present.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    // 只读的Map容器(简单版本，允许包含N个键值对)，禁止写操作
    static final class MapN<K, V> extends AbstractImmutableMap<K, V> {
        
        static final Map<?, ?> EMPTY_MAP = new MapN<>();
        
        @Stable
        final Object[] table; // pairs of key, value
        
        @Stable
        final int size; // number of pairs
        
        MapN(Object... input) {
            if((input.length & 1) != 0) { // implicit nullcheck of input
                throw new InternalError("length is odd");
            }
            size = input.length >> 1;
            
            int len = EXPAND_FACTOR * input.length;
            len = (len + 1) & ~1; // ensure table is even length
            table = new Object[len];
            
            for(int i = 0; i<input.length; i += 2) {
                @SuppressWarnings("unchecked")
                K k = Objects.requireNonNull((K) input[i]);
                @SuppressWarnings("unchecked")
                V v = Objects.requireNonNull((V) input[i + 1]);
                int idx = probe(k);
                if(idx >= 0) {
                    throw new IllegalArgumentException("duplicate key: " + k);
                } else {
                    int dest = -(idx + 1);
                    table[dest] = k;
                    table[dest + 1] = v;
                }
            }
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public V get(Object o) {
            if(size == 0) {
                Objects.requireNonNull(o);
                return null;
            }
            int i = probe(o);
            if(i >= 0) {
                return (V) table[i + 1];
            } else {
                return null;
            }
        }
        
        @Override
        public boolean containsKey(Object o) {
            Objects.requireNonNull(o);
            return size>0 && probe(o) >= 0;
        }
        
        @Override
        public boolean containsValue(Object o) {
            Objects.requireNonNull(o);
            for(int i = 1; i<table.length; i += 2) {
                Object v = table[i];
                if(v != null && o.equals(v)) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public int size() {
                    return MapN.this.size;
                }
                
                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    return new MapNIterator();
                }
            };
        }
        
        @Override
        public int size() {
            return size;
        }
        
        @Override
        public int hashCode() {
            int hash = 0;
            for(int i = 0; i<table.length; i += 2) {
                Object k = table[i];
                if(k != null) {
                    hash += k.hashCode() ^ table[i + 1].hashCode();
                }
            }
            return hash;
        }
        
        // returns index at which the probe key is present; or if absent,
        // (-i - 1) where i is location where element should be inserted.
        // Callers are relying on this method to perform an implicit nullcheck
        // of pk.
        private int probe(Object pk) {
            int idx = Math.floorMod(pk.hashCode(), table.length >> 1) << 1;
            while(true) {
                @SuppressWarnings("unchecked")
                K ek = (K) table[idx];
                if(ek == null) {
                    return -idx - 1;
                } else if(pk.equals(ek)) {
                    return idx;
                } else if((idx += 2) == table.length) {
                    idx = 0;
                }
            }
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new InvalidObjectException("not serial proxy");
        }
        
        private Object writeReplace() {
            Object[] array = new Object[2 * size];
            int len = table.length;
            int dest = 0;
            for(int i = 0; i<len; i += 2) {
                if(table[i] != null) {
                    array[dest++] = table[i];
                    array[dest++] = table[i + 1];
                }
            }
            return new CollSer(CollSer.IMM_MAP, array);
        }
        
        class MapNIterator implements Iterator<Map.Entry<K, V>> {
            
            private int remaining;
            
            private int idx;
            
            MapNIterator() {
                remaining = size();
                if(remaining>0) {
                    idx = Math.floorMod(SALT, table.length >> 1) << 1;
                }
            }
            
            @Override
            public boolean hasNext() {
                return remaining>0;
            }
            
            @Override
            public Map.Entry<K, V> next() {
                if(hasNext()) {
                    while(table[nextIndex()] == null) {
                    }
                    
                    // 将指定的非空的键值对包装为只读对象
                    @SuppressWarnings("unchecked")
                    Map.Entry<K, V> entry = new KeyValueHolder<>((K) table[idx], (V) table[idx + 1]);
                    
                    remaining--;
                    
                    return entry;
                } else {
                    throw new NoSuchElementException();
                }
            }
            
            private int nextIndex() {
                int idx = this.idx;
                if(SALT >= 0) {
                    if((idx += 2) >= table.length) {
                        idx = 0;
                    }
                } else {
                    if((idx -= 2)<0) {
                        idx = table.length - 2;
                    }
                }
                return this.idx = idx;
            }
        }
    }
    
    /*▲ Map ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
