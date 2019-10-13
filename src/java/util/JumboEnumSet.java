/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Private implementation class for EnumSet,
 * for "jumbo" enum types (i.e., those with more than 64 elements).
 *
 * @author Josh Bloch
 * @serial exclude
 * @since 1.5
 */
// 大容量的枚举集合，当枚举实例数量>64时可以使用此集合（本质是要求用到的枚举类型中的枚举常量数量>64）
class JumboEnumSet<E extends Enum<E>> extends EnumSet<E> {
    private static final long serialVersionUID = 334349849919042784L;
    
    /**
     * Bit vector representation of this set.  The ith bit of the jth
     * element of this array represents the  presence of universe[64*j +i]
     * in this set.
     */
    /*
     * 存储枚举实例（其实存储的是位置，枚举实例预先缓存在了universe中）
     * 这里采用了“打包”存储，即每64个实例存储在一个位置
     * 比如前64个实例都会存储在elements[0]处，而且每个实例都占据一个比特位
     */
    private long[] elements;
    
    // Redundant - maintained for performance
    private int size = 0;   // 元素数量
    
    
    
    JumboEnumSet(Class<E> elementType, Enum<?>[] universe) {
        super(elementType, universe);
        elements = new long[(universe.length + 63) >>> 6];
    }
    
    
    
    /*▼ 存值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Adds the specified element to this set if it is not already present.
     *
     * @param e element to be added to this set
     *
     * @return {@code true} if the set changed as a result of the call
     *
     * @throws NullPointerException if {@code e} is null
     */
    // 向Set中添加元素
    public boolean add(E e) {
        typeCheck(e);
        
        // 返回枚举实例的值
        int eOrdinal = e.ordinal();
        
        // 计算当前实例应当存储的位置
        int eWordNum = eOrdinal >>> 6;
        
        // 获取旧的实例集
        long oldElements = elements[eWordNum];
        
        // 添加实例
        elements[eWordNum] |= (1L << eOrdinal);
        
        // 如果成功添加了实例，则计数增一
        boolean result = (elements[eWordNum] != oldElements);
        if(result) {
            size++;
        }
        
        return result;
    }
    
    /**
     * Adds all of the elements in the specified collection to this set.
     *
     * @param c collection whose elements are to be added to this set
     *
     * @return {@code true} if this set changed as a result of the call
     *
     * @throws NullPointerException if the specified collection or any of
     *                              its elements are null
     */
    // 将指定容器中的元素添加到当前Set中
    public boolean addAll(Collection<? extends E> c) {
        if(!(c instanceof JumboEnumSet)) {
            return super.addAll(c);
        }
        
        JumboEnumSet<?> es = (JumboEnumSet<?>) c;
        
        // 如果枚举类型实例不同，则可能抛异常
        if(es.elementType != elementType) {
            if(es.isEmpty()) {
                return false;
            } else {
                throw new ClassCastException(es.elementType + " != " + elementType);
            }
        }
        
        // 添加元素
        for(int i = 0; i<elements.length; i++) {
            elements[i] |= es.elements[i];
        }
        
        // 重新统计元素数量
        return recalculateSize();
    }
    
    // 向当前枚举集合中添加指定范围内的枚举实例
    void addRange(E from, E to) {
        // 计算起始元素的索引
        int fromIndex = from.ordinal() >>> 6;
        
        // 计算终止元素的索引
        int toIndex = to.ordinal() >>> 6;
        
        if(fromIndex == toIndex) {
            // 指代[from, to]范围内的有效元素
            long bits = -1L >>> (from.ordinal() - to.ordinal() - 1);
            elements[fromIndex] = bits << from.ordinal();
        } else {
            elements[fromIndex] = (-1L << from.ordinal());
            for(int i = fromIndex + 1; i<toIndex; i++) {
                elements[i] = -1;
            }
            
            elements[toIndex] = -1L >>> (63 - to.ordinal());
        }
        
        size = to.ordinal() - from.ordinal() + 1;
    }
    
    // 将所有枚举实例添加到当前枚举集合中（universe指示了所有枚举实例）
    void addAll() {
        // 先将elements填满
        Arrays.fill(elements, -1);
        
        // 只保留有效位，>>>universe.length代表了所有有效元素的“范围”
        elements[elements.length - 1] >>>= -universe.length;
        
        size = universe.length;
    }
    
    // 获取当前枚举集合的补集
    void complement() {
        // 所有有效元素的“范围”
        long bits = -1L >>> -universe.length;
        
        // 保留补集
        for(int i = 0; i<elements.length; i++) {
            elements[i] = ~elements[i];
        }
        elements[elements.length - 1] &= bits;
        
        // 计算元素的数量
        size = universe.length - size;
    }
    
    /*▲ 存值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 移除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Removes the specified element from this set if it is present.
     *
     * @param e element to be removed from this set, if present
     *
     * @return {@code true} if the set contained the specified element
     */
    // 移除指定的元素，返回值指示是否移除成功
    public boolean remove(Object e) {
        if(e == null) {
            return false;
        }
        
        Class<?> eClass = e.getClass();
        if(eClass != elementType && eClass.getSuperclass() != elementType) {
            return false;
        }
        
        int eOrdinal = ((Enum<?>) e).ordinal();
        int eWordNum = eOrdinal >>> 6;
        
        long oldElements = elements[eWordNum];
        elements[eWordNum] &= ~(1L << eOrdinal);
        boolean result = (elements[eWordNum] != oldElements);
        if(result) {
            size--;
        }
        
        return result;
    }
    
    /**
     * Removes from this set all of its elements that are contained in
     * the specified collection.
     *
     * @param c elements to be removed from this set
     *
     * @return {@code true} if this set changed as a result of the call
     *
     * @throws NullPointerException if the specified collection is null
     */
    // (匹配则移除)移除当前集合中所有与给定容器中的元素匹配的元素
    public boolean removeAll(Collection<?> c) {
        if(!(c instanceof JumboEnumSet)) {
            return super.removeAll(c);
        }
        
        JumboEnumSet<?> es = (JumboEnumSet<?>) c;
        if(es.elementType != elementType) {
            return false;
        }
        
        for(int i = 0; i<elements.length; i++) {
            elements[i] &= ~es.elements[i];
        }
        
        // 重新统计元素数量
        return recalculateSize();
    }
    
    /**
     * Retains only the elements in this set that are contained in the
     * specified collection.
     *
     * @param c elements to be retained in this set
     *
     * @return {@code true} if this set changed as a result of the call
     *
     * @throws NullPointerException if the specified collection is null
     */
    // (不匹配则移除)移除当前集合中所有与给定容器中的元素不匹配的元素
    public boolean retainAll(Collection<?> c) {
        if(!(c instanceof JumboEnumSet)) {
            return super.retainAll(c);
        }
        
        JumboEnumSet<?> es = (JumboEnumSet<?>) c;
        if(es.elementType != elementType) {
            boolean changed = (size != 0);
            clear();
            return changed;
        }
        
        for(int i = 0; i<elements.length; i++) {
            elements[i] &= es.elements[i];
        }
        
        // 重新统计元素数量
        return recalculateSize();
    }
    
    /**
     * Removes all of the elements from this set.
     */
    // 清空当前集合中所有元素
    public void clear() {
        Arrays.fill(elements, 0);
        size = 0;
    }
    
    /*▲ 移除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包含查询 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if this set contains the specified element.
     *
     * @param e element to be checked for containment in this collection
     *
     * @return {@code true} if this set contains the specified element
     */
    // 判断当前集合中是否包含元素o
    public boolean contains(Object e) {
        if(e == null) {
            return false;
        }
        
        Class<?> eClass = e.getClass();
        if(eClass != elementType && eClass.getSuperclass() != elementType) {
            return false;
        }
        
        int eOrdinal = ((Enum<?>) e).ordinal();
        return (elements[eOrdinal >>> 6] & (1L << eOrdinal)) != 0;
    }
    
    /**
     * Returns {@code true} if this set contains all of the elements
     * in the specified collection.
     *
     * @param c collection to be checked for containment in this set
     *
     * @return {@code true} if this set contains all of the elements
     * in the specified collection
     *
     * @throws NullPointerException if the specified collection is null
     */
    // 判读指定容器中的元素是否都包含在当前集合中
    public boolean containsAll(Collection<?> c) {
        if(!(c instanceof JumboEnumSet))
            return super.containsAll(c);
        
        JumboEnumSet<?> es = (JumboEnumSet<?>) c;
        if(es.elementType != elementType) {
            return es.isEmpty();
        }
        
        for(int i = 0; i<elements.length; i++) {
            if((es.elements[i] & ~elements[i]) != 0) {
                return false;
            }
        }
        
        return true;
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 迭代 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an iterator over the elements contained in this set.  The
     * iterator traverses the elements in their <i>natural order</i> (which is
     * the order in which the enum constants are declared). The returned
     * Iterator is a "weakly consistent" iterator that will never throw {@link
     * ConcurrentModificationException}.
     *
     * @return an iterator over the elements contained in this set
     */
    // 返回当前集合的迭代器
    public Iterator<E> iterator() {
        return new EnumSetIterator<>();
    }
    
    /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of elements in this set.
     *
     * @return the number of elements in this set
     */
    // 返回当前集合的元素数量
    public int size() {
        return size;
    }
    
    /**
     * Returns {@code true} if this set contains no elements.
     *
     * @return {@code true} if this set contains no elements
     */
    // 判断当前集合是否为空
    public boolean isEmpty() {
        return size == 0;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Compares the specified object with this set for equality.  Returns
     * {@code true} if the given object is also a set, the two sets have
     * the same size, and every member of the given set is contained in
     * this set.
     *
     * @param o object to be compared for equality with this set
     *
     * @return {@code true} if the specified object is equal to this set
     */
    public boolean equals(Object o) {
        if(!(o instanceof JumboEnumSet)) {
            return super.equals(o);
        }
        
        JumboEnumSet<?> es = (JumboEnumSet<?>) o;
        if(es.elementType != elementType) {
            return size == 0 && es.size == 0;
        }
        
        return Arrays.equals(es.elements, elements);
    }
    
    public EnumSet<E> clone() {
        JumboEnumSet<E> result = (JumboEnumSet<E>) super.clone();
        result.elements = result.elements.clone();
        return result;
    }
    
    
    
    /**
     * Recalculates the size of the set.  Returns true if it's changed.
     */
    // 重新统计元素数量
    private boolean recalculateSize() {
        int oldSize = size;
        
        size = 0;
        
        for(long elt : elements) {
            size += Long.bitCount(elt);
        }
        
        return size != oldSize;
    }
    
    
    
    
    
    
    private class EnumSetIterator<E extends Enum<E>> implements Iterator<E> {
        /**
         * A bit vector representing the elements in the current "word"
         * of the set not yet returned by this iterator.
         */
        long unseen;
        
        /**
         * The index corresponding to unseen in the elements array.
         */
        int unseenIndex = 0;
        
        /**
         * The bit representing the last element returned by this iterator
         * but not removed, or zero if no such element exists.
         */
        long lastReturned = 0;
        
        /**
         * The index corresponding to lastReturned in the elements array.
         */
        int lastReturnedIndex = 0;
        
        EnumSetIterator() {
            unseen = elements[0];
        }
        
        @Override
        public boolean hasNext() {
            while(unseen == 0 && unseenIndex<elements.length - 1) {
                unseen = elements[++unseenIndex];
            }
            return unseen != 0;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }
            lastReturned = unseen & -unseen;
            lastReturnedIndex = unseenIndex;
            unseen -= lastReturned;
            return (E) universe[(lastReturnedIndex << 6) + Long.numberOfTrailingZeros(lastReturned)];
        }
        
        @Override
        public void remove() {
            if(lastReturned == 0) {
                throw new IllegalStateException();
            }
            
            final long oldElements = elements[lastReturnedIndex];
            elements[lastReturnedIndex] &= ~lastReturned;
            if(oldElements != elements[lastReturnedIndex]) {
                size--;
            }
            
            lastReturned = 0;
        }
    }
}
