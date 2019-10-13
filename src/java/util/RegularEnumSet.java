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
 * Private implementation class for EnumSet, for "regular sized" enum types
 * (i.e., those with 64 or fewer enum constants).
 *
 * @author Josh Bloch
 * @serial exclude
 * @since 1.5
 */
// 小容量的枚举集合，当枚举实例数量<=64时可以使用此集合（本质是要求用到的枚举类型中的枚举常量数量<=64）
class RegularEnumSet<E extends Enum<E>> extends EnumSet<E> {
    private static final long serialVersionUID = 3411599620347842686L;
    
    /**
     * Bit vector representation of this set.  The 2^k bit indicates the
     * presence of universe[k] in this set.
     */
    /*
     * 存储枚举实例（其实存储的是位置，枚举实例预先缓存在了universe中）
     * 这里采用了“打包”存储，即把64个实例都存储在此处
     */
    private long elements = 0L;
    
    
    
    RegularEnumSet(Class<E> elementType, Enum<?>[] universe) {
        super(elementType, universe);
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
        
        long oldElements = elements;
        
        // 返回枚举实例的值
        int eOrdinal = e.ordinal();
        
        // 添加实例
        elements |= (1L << eOrdinal);
        
        return elements != oldElements;
    }
    
    /**
     * Adds all of the elements in the specified collection to this set.
     *
     * @param c collection whose elements are to be added to this set
     *
     * @return {@code true} if this set changed as a result of the call
     *
     * @throws NullPointerException if the specified collection or any
     *                              of its elements are null
     */
    // 将指定容器中的元素添加到当前Set中
    public boolean addAll(Collection<? extends E> c) {
        if(!(c instanceof RegularEnumSet)) {
            return super.addAll(c);
        }
        
        RegularEnumSet<?> es = (RegularEnumSet<?>) c;
        
        // 如果枚举类型实例不同，则可能抛异常
        if(es.elementType != elementType) {
            if(es.isEmpty()) {
                return false;
            } else {
                throw new ClassCastException(es.elementType + " != " + elementType);
            }
        }
        
        long oldElements = elements;
        
        // 添加元素
        elements |= es.elements;
        
        return elements != oldElements;
    }
    
    // 向当前枚举集合中添加指定范围内的枚举实例
    void addRange(E from, E to) {
        // 指代[from, to]范围内的有效元素
        long bits = -1L >>> (from.ordinal() - to.ordinal() - 1);
        
        elements = bits << from.ordinal();
    }
    
    // 将所有枚举实例添加到当前枚举集合中（universe指示了所有枚举实例）
    void addAll() {
        if(universe.length != 0) {
            elements = -1L >>> -universe.length;
        }
    }
    
    // 获取当前枚举集合的补集
    void complement() {
        if(universe.length != 0) {
            // 所有有效元素的“范围”
            long bits = -1L >>> -universe.length;
            
            // 保留补集
            elements = ~elements;
            elements &= bits;  // Mask unused bits
        }
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
        
        long oldElements = elements;
        elements &= ~(1L << ((Enum<?>) e).ordinal());
        return elements != oldElements;
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
        if(!(c instanceof RegularEnumSet)) {
            return super.removeAll(c);
        }
        
        RegularEnumSet<?> es = (RegularEnumSet<?>) c;
        if(es.elementType != elementType) {
            return false;
        }
        
        long oldElements = elements;
        elements &= ~es.elements;
        return elements != oldElements;
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
        if(!(c instanceof RegularEnumSet)) {
            return super.retainAll(c);
        }
        
        RegularEnumSet<?> es = (RegularEnumSet<?>) c;
        if(es.elementType != elementType) {
            boolean changed = (elements != 0);
            elements = 0;
            return changed;
        }
        
        long oldElements = elements;
        elements &= es.elements;
        return elements != oldElements;
    }
    
    /**
     * Removes all of the elements from this set.
     */
    // 清空当前集合中所有元素
    public void clear() {
        elements = 0;
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
        
        return (elements & (1L << ((Enum<?>) e).ordinal())) != 0;
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
        if(!(c instanceof RegularEnumSet)) {
            return super.containsAll(c);
        }
        
        RegularEnumSet<?> es = (RegularEnumSet<?>) c;
        if(es.elementType != elementType) {
            return es.isEmpty();
        }
        
        return (es.elements & ~elements) == 0;
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 迭代 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an iterator over the elements contained in this set.  The
     * iterator traverses the elements in their <i>natural order</i> (which is
     * the order in which the enum constants are declared). The returned
     * Iterator is a "snapshot" iterator that will never throw {@link
     * ConcurrentModificationException}; the elements are traversed as they
     * existed when this call was invoked.
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
        // 返回二进制位中值为1的bit位的数量（把long值i表示为二进制形式）
        return Long.bitCount(elements);
    }
    
    /**
     * Returns {@code true} if this set contains no elements.
     *
     * @return {@code true} if this set contains no elements
     */
    // 判断当前集合是否为空
    public boolean isEmpty() {
        return elements == 0;
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
        if(!(o instanceof RegularEnumSet)) {
            return super.equals(o);
        }
        
        RegularEnumSet<?> es = (RegularEnumSet<?>) o;
        if(es.elementType != elementType) {
            return elements == 0 && es.elements == 0;
        }
        
        return es.elements == elements;
    }
    
    
    
    
    
    
    // 枚举集合迭代器
    private class EnumSetIterator<E extends Enum<E>> implements Iterator<E> {
        
        /**
         * A bit vector representing the elements in the set not yet returned by this iterator.
         */
        long unseen;    // 待遍历元素集合
        
        /**
         * The bit representing the last element returned by this iterator
         * but not removed, or zero if no such element exists.
         */
        long lastReturned = 0;  // 上一次遍历过的元素
        
        EnumSetIterator() {
            unseen = elements;
        }
        
        public boolean hasNext() {
            return unseen != 0;
        }
        
        @SuppressWarnings("unchecked")
        public E next() {
            if(unseen == 0) {
                throw new NoSuchElementException();
            }
            
            lastReturned = unseen & -unseen;
            
            unseen -= lastReturned;
            
            // 返回二进制位中末尾连续的0的个数（把int值i表示为二进制形式）
            int index = Long.numberOfTrailingZeros(lastReturned);
            
            return (E) universe[index];
        }
        
        public void remove() {
            if(lastReturned == 0) {
                throw new IllegalStateException();
            }
            
            elements &= ~lastReturned;
            
            lastReturned = 0;
        }
    }
}
