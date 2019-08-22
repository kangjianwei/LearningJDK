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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import jdk.internal.misc.SharedSecrets;

/**
 * Resizable-array implementation of the {@code List} interface.  Implements
 * all optional list operations, and permits all elements, including
 * {@code null}.  In addition to implementing the {@code List} interface,
 * this class provides methods to manipulate the size of the array that is
 * used internally to store the list.  (This class is roughly equivalent to
 * {@code Vector}, except that it is unsynchronized.)
 *
 * <p>The {@code size}, {@code isEmpty}, {@code get}, {@code set},
 * {@code iterator}, and {@code listIterator} operations run in constant
 * time.  The {@code add} operation runs in <i>amortized constant time</i>,
 * that is, adding n elements requires O(n) time.  All of the other operations
 * run in linear time (roughly speaking).  The constant factor is low compared
 * to that for the {@code LinkedList} implementation.
 *
 * <p>Each {@code ArrayList} instance has a <i>capacity</i>.  The capacity is
 * the size of the array used to store the elements in the list.  It is always
 * at least as large as the list size.  As elements are added to an ArrayList,
 * its capacity grows automatically.  The details of the growth policy are not
 * specified beyond the fact that adding an element has constant amortized
 * time cost.
 *
 * <p>An application can increase the capacity of an {@code ArrayList} instance
 * before adding a large number of elements using the {@code ensureCapacity}
 * operation.  This may reduce the amount of incremental reallocation.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access an {@code ArrayList} instance concurrently,
 * and at least one of the threads modifies the list structurally, it
 * <i>must</i> be synchronized externally.  (A structural modification is
 * any operation that adds or deletes one or more elements, or explicitly
 * resizes the backing array; merely setting the value of an element is not
 * a structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the list.
 *
 * If no such object exists, the list should be "wrapped" using the
 * {@link Collections#synchronizedList Collections.synchronizedList}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the list:<pre>
 *   List list = Collections.synchronizedList(new ArrayList(...));</pre>
 *
 * <p id="fail-fast">
 * The iterators returned by this class's {@link #iterator() iterator} and
 * {@link #listIterator(int) listIterator} methods are <em>fail-fast</em>:
 * if the list is structurally modified at any time after the iterator is
 * created, in any way except through the iterator's own
 * {@link ListIterator#remove() remove} or
 * {@link ListIterator#add(Object) add} methods, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of
 * concurrent modification, the iterator fails quickly and cleanly, rather
 * than risking arbitrary, non-deterministic behavior at an undetermined
 * time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:  <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @param <E> the type of elements in this list
 *
 * @author Josh Bloch
 * @author Neal Gafter
 * @see Collection
 * @see List
 * @see LinkedList
 * @see Vector
 * @since 1.2
 */
// 顺序表：线性表的顺序存储结构，内部使用数组实现，非线程安全
public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
    
    /**
     * Default initial capacity.
     */
    private static final int DEFAULT_CAPACITY = 10;
    
    /**
     * The maximum size of array to allocate (unless necessary).
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    
    /**
     * Shared empty array instance used for empty instances.
     */
    private static final Object[] EMPTY_ELEMENTDATA = {};
    
    /**
     * Shared empty array instance used for default sized empty instances. We
     * distinguish this from EMPTY_ELEMENTDATA to know how much to inflate when
     * first element is added.
     */
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
    
    /**
     * The array buffer into which the elements of the ArrayList are stored.
     * The capacity of the ArrayList is the length of this array buffer. Any
     * empty ArrayList with elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA
     * will be expanded to DEFAULT_CAPACITY when the first element is added.
     */
    // 存储当前顺序表的元素
    transient Object[] elementData; // non-private to simplify nested class access
    
    /**
     * The size of the ArrayList (the number of elements it contains).
     *
     * @serial
     */
    // 元素数量
    private int size;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }
    
    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     *
     * @throws IllegalArgumentException if the specified initial capacity
     *                                  is negative
     */
    public ArrayList(int initialCapacity) {
        if(initialCapacity>0) {
            this.elementData = new Object[initialCapacity];
        } else if(initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
    }
    
    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     *
     * @throws NullPointerException if the specified collection is null
     */
    public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();
        
        if((size = elementData.length) != 0) {
            /*
             * defend against c.toArray (incorrectly) not returning Object[]
             * (see e.g. https://bugs.openjdk.java.net/browse/JDK-6260652)
             */
            if(elementData.getClass() != Object[].class) {
                elementData = Arrays.copyOf(elementData, size, Object[].class);
            }
        } else {
            // replace with empty array.
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Appends the specified element to the end of this list.
     *
     * @param e element to be appended to this list
     *
     * @return {@code true} (as specified by {@link Collection#add})
     */
    // 将元素e追加到当前顺序表中
    public boolean add(E e) {
        modCount++;
        add(e, elementData, size);
        return true;
    }
    
    /**
     * Inserts the specified element at the specified position in this
     * list. Shifts the element currently at that position (if any) and
     * any subsequent elements to the right (adds one to their indices).
     *
     * @param index   index at which the specified element is to be inserted
     * @param element element to be inserted
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 将元素element添加到顺序表index处
    public void add(int index, E element) {
        rangeCheckForAdd(index);
        
        modCount++;
        
        final int s;
        Object[] elementData;
        
        // 如果顺序表已满，则需要扩容
        if((s = size) == (elementData = this.elementData).length) {
            // 对当前顺序表扩容
            elementData = grow();
        }
        
        // 移动元素
        System.arraycopy(elementData, index, elementData, index + 1, s - index);
        
        // 插入元素
        elementData[index] = element;
        
        size = s + 1;
    }
    
    
    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the
     * specified collection's Iterator.  The behavior of this operation is
     * undefined if the specified collection is modified while the operation
     * is in progress.  (This implies that the behavior of this call is
     * undefined if the specified collection is this list, and this
     * list is nonempty.)
     *
     * @param c collection containing elements to be added to this list
     *
     * @return {@code true} if this list changed as a result of the call
     *
     * @throws NullPointerException if the specified collection is null
     */
    // 将指定容器中的元素追加到当前顺序表中
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        
        modCount++;
        
        int numNew = a.length;
        if(numNew == 0) {
            return false;
        }
        
        Object[] elementData;
        final int s;
        
        if(numNew>(elementData = this.elementData).length - (s = size)) {
            elementData = grow(s + numNew);
        }
        
        System.arraycopy(a, 0, elementData, s, numNew);
        
        size = s + numNew;
        
        return true;
    }
    
    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param index index at which to insert the first element from the
     *              specified collection
     * @param c     collection containing elements to be added to this list
     *
     * @return {@code true} if this list changed as a result of the call
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException      if the specified collection is null
     */
    // 将指定容器中的元素添加到当前顺序表的index处
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        
        Object[] a = c.toArray();
        modCount++;
        int numNew = a.length;
        if(numNew == 0) {
            return false;
        }
        Object[] elementData;
        final int s;
        if(numNew>(elementData = this.elementData).length - (s = size)) {
            elementData = grow(s + numNew);
        }
        
        int numMoved = s - index;
        if(numMoved>0) {
            System.arraycopy(elementData, index, elementData, index + numNew, numMoved);
        }
        System.arraycopy(a, 0, elementData, index, numNew);
        size = s + numNew;
        return true;
    }
    
    /*▲ 存值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return
     *
     * @return the element at the specified position in this list
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 获取指定索引处的元素
    public E get(int index) {
        Objects.checkIndex(index, size);
        return elementData(index);
    }
    
    /*▲ 取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 移除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).
     *
     * @param index the index of the element to be removed
     *
     * @return the element that was removed from the list
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 移除索引index处的元素，返回被移除的元素
    public E remove(int index) {
        Objects.checkIndex(index, size);
        final Object[] es = elementData;
        
        @SuppressWarnings("unchecked")
        E oldValue = (E) es[index];
        
        // 移除es[index]
        fastRemove(es, index);
        
        return oldValue;
    }
    
    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If the list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * {@code i} such that
     * {@code Objects.equals(o, get(i))}
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     *
     * @return {@code true} if this list contained the specified element
     */
    // 移除指定的元素，返回值指示是否移除成功
    public boolean remove(Object o) {
        final Object[] es = elementData;
        final int size = this.size;
        int i = 0;
found:
        {
            if(o == null) {
                for(; i<size; i++) {
                    if(es[i] == null) {
                        break found;
                    }
                }
            } else {
                for(; i<size; i++) {
                    if(o.equals(es[i])) {
                        break found;
                    }
                }
            }
            return false;
        }
        
        // 移除es[index]
        fastRemove(es, i);
        
        return true;
    }
    
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 移除满足条件的元素，移除条件由filter决定，返回值指示是否移除成功
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return removeIf(filter, 0, size);
    }
    
    
    /**
     * Removes from this list all of its elements that are contained in the
     * specified collection.
     *
     * @param c collection containing elements to be removed from this list
     *
     * @return {@code true} if this list changed as a result of the call
     *
     * @throws ClassCastException   if the class of an element of this list
     *                              is incompatible with the specified collection
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *                              specified collection does not permit null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>),
     *                              or if the specified collection is null
     * @see Collection#contains(Object)
     */
    // (匹配则移除)移除当前顺序表中所有与给定容器中的元素匹配的元素
    public boolean removeAll(Collection<?> c) {
        return batchRemove(c, false, 0, size);
    }
    
    /**
     * Retains only the elements in this list that are contained in the
     * specified collection.  In other words, removes from this list all
     * of its elements that are not contained in the specified collection.
     *
     * @param c collection containing elements to be retained in this list
     *
     * @return {@code true} if this list changed as a result of the call
     *
     * @throws ClassCastException   if the class of an element of this list
     *                              is incompatible with the specified collection
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *                              specified collection does not permit null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>),
     *                              or if the specified collection is null
     * @see Collection#contains(Object)
     */
    // (不匹配则移除)移除当前顺序表中所有与给定容器中的元素不匹配的元素
    public boolean retainAll(Collection<?> c) {
        return batchRemove(c, true, 0, size);
    }
    
    
    /**
     * Removes from this list all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * This call shortens the list by {@code (toIndex - fromIndex)} elements.
     * (If {@code toIndex==fromIndex}, this operation has no effect.)
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} or
     *                                   {@code toIndex} is out of range
     *                                   ({@code fromIndex < 0 ||
     *                                   toIndex > size() ||
     *                                   toIndex < fromIndex})
     */
    // 移除当前顺序表[fromIndex,toIndex]之间的元素
    protected void removeRange(int fromIndex, int toIndex) {
        if(fromIndex>toIndex) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(fromIndex, toIndex));
        }
        
        modCount++;
        
        // 移除lo~hi之间的元素
        shiftTailOverGap(elementData, fromIndex, toIndex);
    }
    
    
    /**
     * Removes all of the elements from this list.  The list will
     * be empty after this call returns.
     */
    // 清空当前顺序表中的元素
    public void clear() {
        modCount++;
        final Object[] es = elementData;
        for(int to = size, i = size = 0; i<to; i++) {
            es[i] = null;
        }
    }
    
    /*▲ 移除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 替换 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Replaces the element at the specified position in this list with
     * the specified element.
     *
     * @param index   index of the element to replace
     * @param element element to be stored at the specified position
     *
     * @return the element previously at the specified position
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 将index处的元素更新为element，并返回旧元素
    public E set(int index, E element) {
        Objects.checkIndex(index, size);
        E oldValue = elementData(index);
        elementData[index] = element;
        return oldValue;
    }
    
    
    // 更新当前顺序表中所有元素，更新策略由operator决定
    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        replaceAllRange(operator, 0, size);
        modCount++;
    }
    
    /*▲ 替换 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包含查询 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that
     * {@code Objects.equals(o, e)}.
     *
     * @param o element whose presence in this list is to be tested
     *
     * @return {@code true} if this list contains the specified element
     */
    // 判断当前顺序表中是否包含指定的元素
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 定位 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the lowest index {@code i} such that
     * {@code Objects.equals(o, get(i))},
     * or -1 if there is no such index.
     */
    // 返回指定元素的正序索引(正序查找首个匹配的元素)
    public int indexOf(Object o) {
        // 在[0, size)之间正序搜索元素o，返回首个匹配的索引
        return indexOfRange(o, 0, size);
    }
    
    /**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the highest index {@code i} such that
     * {@code Objects.equals(o, get(i))},
     * or -1 if there is no such index.
     */
    // 返回指定元素的逆序索引(逆序查找首个匹配的元素)
    public int lastIndexOf(Object o) {
        // 在[0, size)之间逆序搜索元素o，返回首个匹配的索引
        return lastIndexOfRange(o, 0, size);
    }
    
    /*▲ 定位 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a view of the portion of this list between the specified
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.  (If
     * {@code fromIndex} and {@code toIndex} are equal, the returned list is
     * empty.)  The returned list is backed by this list, so non-structural
     * changes in the returned list are reflected in this list, and vice-versa.
     * The returned list supports all of the optional list operations.
     *
     * <p>This method eliminates the need for explicit range operations (of
     * the sort that commonly exist for arrays).  Any operation that expects
     * a list can be used as a range operation by passing a subList view
     * instead of a whole list.  For example, the following idiom
     * removes a range of elements from a list:
     * <pre>
     *      list.subList(from, to).clear();
     * </pre>
     * Similar idioms may be constructed for {@link #indexOf(Object)} and
     * {@link #lastIndexOf(Object)}, and all of the algorithms in the
     * {@link Collections} class can be applied to a subList.
     *
     * <p>The semantics of the list returned by this method become undefined if
     * the backing list (i.e., this list) is <i>structurally modified</i> in
     * any way other than via the returned list.  (Structural modifications are
     * those that change the size of this list, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException  {@inheritDoc}
     */
    // 返回[fromIndex, toIndex)之间的元素的视图
    public List<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size);
        return new SubList<>(this, fromIndex, toIndex);
    }
    
    
    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list in
     * proper sequence
     */
    public Object[] toArray() {
        return Arrays.copyOf(elementData, size);
    }
    
    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element); the runtime type of the returned
     * array is that of the specified array.  If the list fits in the
     * specified array, it is returned therein.  Otherwise, a new array is
     * allocated with the runtime type of the specified array and the size of
     * this list.
     *
     * <p>If the list fits in the specified array with room to spare
     * (i.e., the array has more elements than the list), the element in
     * the array immediately following the end of the collection is set to
     * {@code null}.  (This is useful in determining the length of the
     * list <i>only</i> if the caller knows that the list does not contain
     * any null elements.)
     *
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     *
     * @return an array containing the elements of the list
     *
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this list
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if(a.length<size){
            // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
        }
        
        System.arraycopy(elementData, 0, a, 0, size);
        
        if(a.length>size) {
            a[size] = null;
        }
        
        return a;
    }
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 迭代 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 遍历当前顺序表中的元素，并对其应用指定的择取操作
    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        final Object[] es = elementData;
        final int size = this.size;
        for(int i = 0; modCount == expectedModCount && i<size; i++) {
            action.accept(elementAt(es, i));
        }
        if(modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
    
    
    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>The returned iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    // 返回当前顺序表的一个迭代器
    public Iterator<E> iterator() {
        return new Itr();
    }
    
    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence).
     *
     * <p>The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @see #listIterator(int)
     */
    // 返回当前顺序表的一个增强的迭代器，且设定下一个待遍历元素为索引0处的元素
    public ListIterator<E> listIterator() {
        return new ListItr(0);
    }
    
    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * The specified index indicates the first element that would be
     * returned by an initial call to {@link ListIterator#next next}.
     * An initial call to {@link ListIterator#previous previous} would
     * return the element with the specified index minus one.
     *
     * <p>The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 返回当前顺序表的一个增强的迭代器，且设定下一个待遍历元素为索引index处的元素
    public ListIterator<E> listIterator(int index) {
        rangeCheckForAdd(index);
        return new ListItr(index);
    }
    
    
    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, and {@link Spliterator#ORDERED}.
     * Overriding implementations should document the reporting of additional
     * characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this list
     *
     * @since 1.8
     */
    // 返回一个可分割的迭代器
    @Override
    public Spliterator<E> spliterator() {
        return new ArrayListSpliterator(0, -1, 0);
    }
    
    /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    // 返回当前顺序表的元素数量
    public int size() {
        return size;
    }
    
    /**
     * Returns {@code true} if this list contains no elements.
     *
     * @return {@code true} if this list contains no elements
     */
    // 判断当前顺序表是否为空
    public boolean isEmpty() {
        return size == 0;
    }
    
    
    // 使用指定的比较器对当前顺序表内的元素进行排序
    @Override
    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super E> c) {
        final int expectedModCount = modCount;
        Arrays.sort((E[]) elementData, 0, size, c);
        if(modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }
    
    
    /**
     * Trims the capacity of this {@code ArrayList} instance to be the
     * list's current size.  An application can use this operation to minimize
     * the storage of an {@code ArrayList} instance.
     */
    // 重新设置顺序表的容量，如果新容量小于元素数量，则会移除超出新容量的元素
    public void trimToSize() {
        modCount++;
        if(size<elementData.length) {
            elementData = (size == 0) ? EMPTY_ELEMENTDATA : Arrays.copyOf(elementData, size);
        }
    }
    
    /**
     * Increases the capacity of this {@code ArrayList} instance, if
     * necessary, to ensure that it can hold at least the number of elements
     * specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    // 确保当前顺序表至少拥有minCapacity的容量
    public void ensureCapacity(int minCapacity) {
        if(minCapacity>elementData.length && !(elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA && minCapacity<=DEFAULT_CAPACITY)) {
            modCount++;
            grow(minCapacity);
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static final long serialVersionUID = 8683452581122892189L;
    
    
    /**
     * Saves the state of the {@code ArrayList} instance to a stream
     * (that is, serializes it).
     *
     * @param s the stream
     *
     * @throws IOException if an I/O error occurs
     * @serialData The length of the array backing the {@code ArrayList}
     * instance is emitted (int), followed by all of its elements
     * (each an {@code Object}) in the proper order.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        // Write out element count, and any hidden stuff
        int expectedModCount = modCount;
        s.defaultWriteObject();
        
        // Write out size as capacity for behavioral compatibility with clone()
        s.writeInt(size);
        
        // Write out all elements in the proper order.
        for(int i = 0; i<size; i++) {
            s.writeObject(elementData[i]);
        }
        
        if(modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
    
    /**
     * Reconstitutes the {@code ArrayList} instance from a stream (that is,
     * deserializes it).
     *
     * @param s the stream
     *
     * @throws ClassNotFoundException if the class of a serialized object
     *                                could not be found
     * @throws IOException            if an I/O error occurs
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        
        // Read in size, and any hidden stuff
        s.defaultReadObject();
        
        // Read in capacity
        s.readInt(); // ignored
        
        if(size>0) {
            // like clone(), allocate array based upon size not capacity
            SharedSecrets.getJavaObjectInputStreamAccess().checkArray(s, Object[].class, size);
            Object[] elements = new Object[size];
            
            // Read in all elements in the proper order.
            for(int i = 0; i<size; i++) {
                elements[i] = s.readObject();
            }
            
            elementData = elements;
        } else if(size == 0) {
            elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new InvalidObjectException("Invalid size: " + size);
        }
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }
        
        if(!(o instanceof List)) {
            return false;
        }
        
        final int expectedModCount = modCount;
        // ArrayList can be subclassed and given arbitrary behavior, but we can
        // still deal with the common case where o is ArrayList precisely
        boolean equal = (o.getClass() == ArrayList.class) ? equalsArrayList((ArrayList<?>) o) : equalsRange((List<?>) o, 0, size);
        
        checkForComodification(expectedModCount);
        return equal;
    }
    
    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int expectedModCount = modCount;
        int hash = hashCodeRange(0, size);
        checkForComodification(expectedModCount);
        return hash;
    }
    
    /**
     * Returns a shallow copy of this {@code ArrayList} instance.  (The
     * elements themselves are not copied.)
     *
     * @return a clone of this {@code ArrayList} instance
     */
    public Object clone() {
        try {
            ArrayList<?> v = (ArrayList<?>) super.clone();
            v.elementData = Arrays.copyOf(elementData, size);
            v.modCount = 0;
            return v;
        } catch(CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }
    
    
    
    // 返回元素es[index]
    @SuppressWarnings("unchecked")
    static <E> E elementAt(Object[] es, int index) {
        return (E) es[index];
    }
    
    // 在[start, end)之间正序搜索元素o，返回首个匹配的索引
    int indexOfRange(Object o, int start, int end) {
        Object[] es = elementData;
        if(o == null) {
            for(int i = start; i<end; i++) {
                if(es[i] == null) {
                    return i;
                }
            }
        } else {
            for(int i = start; i<end; i++) {
                if(o.equals(es[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    // 在[start, end)之间逆序搜索元素o，返回首个匹配的索引
    int lastIndexOfRange(Object o, int start, int end) {
        Object[] es = elementData;
        if(o == null) {
            for(int i = end - 1; i >= start; i--) {
                if(es[i] == null) {
                    return i;
                }
            }
        } else {
            for(int i = end - 1; i >= start; i--) {
                if(o.equals(es[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) elementData[index];
    }
    
    boolean equalsRange(List<?> other, int from, int to) {
        final Object[] es = elementData;
        if(to>es.length) {
            throw new ConcurrentModificationException();
        }
        var oit = other.iterator();
        for(; from<to; from++) {
            if(!oit.hasNext() || !Objects.equals(es[from], oit.next())) {
                return false;
            }
        }
        return !oit.hasNext();
    }
    
    int hashCodeRange(int from, int to) {
        final Object[] es = elementData;
        if(to>es.length) {
            throw new ConcurrentModificationException();
        }
        int hashCode = 1;
        for(int i = from; i<to; i++) {
            Object e = es[i];
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }
    
    boolean batchRemove(Collection<?> c, boolean complement, final int from, final int end) {
        Objects.requireNonNull(c);
        final Object[] es = elementData;
        int r;
        // Optimize for initial run of survivors
        for(r = from; ; r++) {
            if(r == end) {
                return false;
            }
            
            if(c.contains(es[r]) != complement) {
                break;
            }
        }
        int w = r++;
        try {
            for(Object e; r<end; r++) {
                if(c.contains(e = es[r]) == complement) {
                    es[w++] = e;
                }
            }
        } catch(Throwable ex) {
            // Preserve behavioral compatibility with AbstractCollection,
            // even if c.contains() throws.
            System.arraycopy(es, r, es, w, end - r);
            w += end - r;
            throw ex;
        } finally {
            modCount += end - w;
            shiftTailOverGap(es, w, end);
        }
        return true;
    }
    
    /**
     * Removes all elements satisfying the given predicate, from index i (inclusive) to index end (exclusive).
     */
    boolean removeIf(Predicate<? super E> filter, int i, final int end) {
        Objects.requireNonNull(filter);
        
        int expectedModCount = modCount;
        
        final Object[] es = elementData;
        
        // Optimize for initial run of survivors
        while(i<end && !filter.test(elementAt(es, i))) {
            i++;
        }
        
        /*
         * Tolerate predicates that reentrantly access the collection for read (but writers still get CME),
         * so traverse once to find elements to delete, a second pass to physically expunge.
         */
        if(i<end) {
            final int beg = i;
            final long[] deathRow = nBits(end - beg);
            
            deathRow[0] = 1L;   // set bit 0
            
            for(i = beg + 1; i<end; i++) {
                if(filter.test(elementAt(es, i))) {
                    setBit(deathRow, i - beg);
                }
            }
            
            if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            modCount++;
            int w = beg;
            for(i = beg; i<end; i++) {
                if(isClear(deathRow, i - beg)) {
                    es[w++] = es[i];
                }
            }
            shiftTailOverGap(es, w, end);
            return true;
        } else {
            if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return false;
        }
    }
    
    void checkInvariants() {
        // assert size >= 0;
        // assert size == elementData.length || elementData[size] == null;
    }
    
    // 大容量处理
    private static int hugeCapacity(int minCapacity) {
        if(minCapacity<0) {
            // overflow
            throw new OutOfMemoryError();
        }
        
        return (minCapacity>MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }
    
    /**
     * A version used in checking (fromIndex > toIndex) condition
     */
    private static String outOfBoundsMsg(int fromIndex, int toIndex) {
        return "From Index: " + fromIndex + " > To Index: " + toIndex;
    }
    
    private static long[] nBits(int n) {
        return new long[((n - 1) >> 6) + 1];
    }
    
    private static void setBit(long[] bits, int i) {
        bits[i >> 6] |= 1L << i;
    }
    
    private static boolean isClear(long[] bits, int i) {
        return (bits[i >> 6] & (1L << i)) == 0;
    }
    
    // 对当前顺序表扩容
    private Object[] grow() {
        return grow(size + 1);
    }
    
    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     *
     * @throws OutOfMemoryError if minCapacity is less than zero
     */
    // 对当前顺序表扩容，minCapacity是申请的容量
    private Object[] grow(int minCapacity) {
        // 根据申请的容量，返回一个合适的新容量
        int newCapacity = newCapacity(minCapacity);
        return elementData = Arrays.copyOf(elementData, newCapacity);
    }
    
    /**
     * Returns a capacity at least as large as the given minimum capacity.
     * Returns the current capacity increased by 50% if that suffices.
     * Will not return a capacity greater than MAX_ARRAY_SIZE unless
     * the given minimum capacity is greater than MAX_ARRAY_SIZE.
     *
     * @param minCapacity the desired minimum capacity
     *
     * @throws OutOfMemoryError if minCapacity is less than zero
     */
    // 根据申请的容量，返回一个合适的新容量
    private int newCapacity(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = elementData.length;   // 旧容量
        int newCapacity = oldCapacity + (oldCapacity >> 1); // 预期新容量（增加0.5倍）
        
        // 如果预期新容量小于申请的容量
        if(newCapacity - minCapacity<=0) {
            // 如果数组还未初始化
            if(elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
                // 返回一个初始容量
                return Math.max(DEFAULT_CAPACITY, minCapacity);
            }
            
            // 溢出
            if(minCapacity<0) {
                // overflow
                throw new OutOfMemoryError();
            }
            
            return minCapacity;
        }
        
        // 在预期新容量大于申请的容量时，按新容量走
        return (newCapacity - MAX_ARRAY_SIZE<=0) ? newCapacity : hugeCapacity(minCapacity);
    }
    
    /**
     * This helper method split out from add(E) to keep method
     * bytecode size under 35 (the -XX:MaxInlineSize default value),
     * which helps when add(E) is called in a C1-compiled loop.
     */
    // 将元素e添加到elementData[s]
    private void add(E e, Object[] elementData, int s) {
        // 元素填满数组时，需要扩容
        if(s == elementData.length) {
            elementData = grow();
        }
        
        elementData[s] = e;
        
        size = s + 1;
    }
    
    private boolean equalsArrayList(ArrayList<?> other) {
        final int otherModCount = other.modCount;
        final int s = size;
        boolean equal;
        if(equal = (s == other.size)) {
            final Object[] otherEs = other.elementData;
            final Object[] es = elementData;
            if(s>es.length || s>otherEs.length) {
                throw new ConcurrentModificationException();
            }
            for(int i = 0; i<s; i++) {
                if(!Objects.equals(es[i], otherEs[i])) {
                    equal = false;
                    break;
                }
            }
        }
        other.checkForComodification(otherModCount);
        return equal;
    }
    
    private void checkForComodification(final int expectedModCount) {
        if(modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
    
    /**
     * Private remove method that skips bounds checking and does not return the value removed.
     */
    // 移除es[i]
    private void fastRemove(Object[] es, int i) {
        modCount++;
        final int newSize;
        if((newSize = size - 1)>i) {
            System.arraycopy(es, i + 1, es, i, newSize - i);
        }
        es[size = newSize] = null;
    }
    
    /** Erases the gap from lo to hi, by sliding down following elements. */
    // 移除lo~hi之间的元素
    private void shiftTailOverGap(Object[] es, int lo, int hi) {
        System.arraycopy(es, hi, es, lo, size - hi);
        for(int to = size, i = (size -= hi - lo); i<to; i++) {
            es[i] = null;
        }
    }
    
    /**
     * A version of rangeCheck used by add and addAll.
     */
    private void rangeCheckForAdd(int index) {
        if(index>size || index<0)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
    
    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     */
    private String outOfBoundsMsg(int index) {
        return "Index: " + index + ", Size: " + size;
    }
    
    // 更新当前顺序表中[i, end)之间的元素，更新策略由operator决定
    private void replaceAllRange(UnaryOperator<E> operator, int i, int end) {
        Objects.requireNonNull(operator);
        
        final int expectedModCount = modCount;
        final Object[] es = elementData;
        
        while(modCount == expectedModCount && i<end) {
            // 获取元素es[index]
            E element = elementAt(es, i);
            es[i] = operator.apply(element);
            i++;
        }
        
        if(modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
    
    
    
    
    
    
    /**
     * An optimized version of AbstractList.Itr
     */
    private class Itr implements Iterator<E> {
        // 下一个待遍历元素的游标
        int cursor;       // index of next element to return
        
        // 刚刚遍历过的元素的索引
        int lastRet = -1; // index of last element returned; -1 if no such
        
        int expectedModCount = modCount;
        
        // prevent creating a synthetic constructor
        Itr() {
        }
        
        public boolean hasNext() {
            return cursor != size;
        }
        
        @SuppressWarnings("unchecked")
        public E next() {
            checkForComodification();
            
            int i = cursor;
            
            if(i >= size) {
                throw new NoSuchElementException();
            }
            
            Object[] elementData = ArrayList.this.elementData;
            
            if(i >= elementData.length) {
                throw new ConcurrentModificationException();
            }
            
            cursor = i + 1;
            
            return (E) elementData[lastRet = i];
        }
        
        public void remove() {
            if(lastRet<0) {
                throw new IllegalStateException();
            }
            
            checkForComodification();
            
            try {
                ArrayList.this.remove(lastRet);
                cursor = lastRet;
                lastRet = -1;
                expectedModCount = modCount;
            } catch(IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
        
        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final int size = ArrayList.this.size;
            int i = cursor;
            if(i<size) {
                final Object[] es = elementData;
                if(i >= es.length) {
                    throw new ConcurrentModificationException();
                }
                for(; i<size && modCount == expectedModCount; i++) {
                    action.accept(elementAt(es, i));
                }
                // update once at end to reduce heap write traffic
                cursor = i;
                lastRet = i - 1;
                checkForComodification();
            }
        }
        
        final void checkForComodification() {
            if(modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
    
    /**
     * An optimized version of AbstractList.ListItr
     */
    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {
            super();
            cursor = index;
        }
        
        public boolean hasPrevious() {
            return cursor != 0;
        }
        
        public int previousIndex() {
            return cursor - 1;
        }
        
        @SuppressWarnings("unchecked")
        public E previous() {
            checkForComodification();
            
            int i = cursor - 1;
            
            if(i<0) {
                throw new NoSuchElementException();
            }
            
            Object[] elementData = ArrayList.this.elementData;
            if(i >= elementData.length) {
                throw new ConcurrentModificationException();
            }
            
            cursor = i;
            
            return (E) elementData[lastRet = i];
        }
        
        public int nextIndex() {
            return cursor;
        }
        
        public void add(E e) {
            checkForComodification();
            
            try {
                int i = cursor;
                ArrayList.this.add(i, e);
                cursor = i + 1;
                lastRet = -1;
                expectedModCount = modCount;
            } catch(IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
        
        public void set(E e) {
            if(lastRet<0) {
                throw new IllegalStateException();
            }
            
            checkForComodification();
            
            try {
                ArrayList.this.set(lastRet, e);
            } catch(IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
        
    }
    
    /** Index-based split-by-two, lazily initialized Spliterator */
    final class ArrayListSpliterator implements Spliterator<E> {
        
        /*
         * If ArrayLists were immutable, or structurally immutable (no
         * adds, removes, etc), we could implement their spliterators
         * with Arrays.spliterator. Instead we detect as much
         * interference during traversal as practical without
         * sacrificing much performance. We rely primarily on
         * modCounts. These are not guaranteed to detect concurrency
         * violations, and are sometimes overly conservative about
         * within-thread interference, but detect enough problems to
         * be worthwhile in practice. To carry this out, we (1) lazily
         * initialize fence and expectedModCount until the latest
         * point that we need to commit to the state we are checking
         * against; thus improving precision.  (This doesn't apply to
         * SubLists, that create spliterators with current non-lazy
         * values).  (2) We perform only a single
         * ConcurrentModificationException check at the end of forEach
         * (the most performance-sensitive method). When using forEach
         * (as opposed to iterators), we can normally only detect
         * interference after actions, not before. Further
         * CME-triggering checks apply to all other possible
         * violations of assumptions for example null or too-small
         * elementData array given its size(), that could only have
         * occurred due to interference.  This allows the inner loop
         * of forEach to run without any further checks, and
         * simplifies lambda-resolution. While this does entail a
         * number of checks, note that in the common case of
         * list.stream().forEach(a), no checks or other computation
         * occur anywhere other than inside forEach itself.  The other
         * less-often-used methods cannot take advantage of most of
         * these streamlinings.
         */
        
        private int index; // current index, modified on advance/split
        private int fence; // -1 until used; then one past last index
        private int expectedModCount; // initialized when fence set
        
        /** Creates new spliterator covering the given range. */
        ArrayListSpliterator(int origin, int fence, int expectedModCount) {
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }
        
        public ArrayListSpliterator trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid)
                ? null
                : new ArrayListSpliterator(lo, index = mid, expectedModCount);    // divide range in half unless too small
        }
        
        public boolean tryAdvance(Consumer<? super E> action) {
            if(action == null) {
                throw new NullPointerException();
            }
            int hi = getFence(), i = index;
            if(i<hi) {
                index = i + 1;
                @SuppressWarnings("unchecked")
                E e = (E) elementData[i];
                action.accept(e);
                if(modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                return true;
            }
            return false;
        }
        
        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi, mc; // hoist accesses and checks from loop
            Object[] a;
            if(action == null) {
                throw new NullPointerException();
            }
            if((a = elementData) != null) {
                if((hi = fence)<0) {
                    mc = modCount;
                    hi = size;
                } else
                    mc = expectedModCount;
                if((i = index) >= 0 && (index = hi)<=a.length) {
                    for(; i<hi; ++i) {
                        @SuppressWarnings("unchecked")
                        E e = (E) a[i];
                        action.accept(e);
                    }
                    if(modCount == mc) {
                        return;
                    }
                }
            }
            throw new ConcurrentModificationException();
        }
        
        public long estimateSize() {
            return getFence() - index;
        }
        
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
        
        private int getFence() { // initialize fence to size on first use
            int hi; // (a specialized variant appears in method forEach)
            if((hi = fence)<0) {
                expectedModCount = modCount;
                hi = fence = size;
            }
            return hi;
        }
    }
    
    private static class SubList<E> extends AbstractList<E> implements RandomAccess {
        private final ArrayList<E> root;
        private final SubList<E> parent;
        private final int offset;
        private int size;
        
        /**
         * Constructs a sublist of an arbitrary ArrayList.
         */
        public SubList(ArrayList<E> root, int fromIndex, int toIndex) {
            this.root = root;
            this.parent = null;
            this.offset = fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = root.modCount;
        }
        
        /**
         * Constructs a sublist of another SubList.
         */
        private SubList(SubList<E> parent, int fromIndex, int toIndex) {
            this.root = parent.root;
            this.parent = parent;
            this.offset = parent.offset + fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = root.modCount;
        }
        
        public E set(int index, E element) {
            Objects.checkIndex(index, size);
            checkForComodification();
            E oldValue = root.elementData(offset + index);
            root.elementData[offset + index] = element;
            return oldValue;
        }
        
        public E get(int index) {
            Objects.checkIndex(index, size);
            checkForComodification();
            return root.elementData(offset + index);
        }
        
        public int size() {
            checkForComodification();
            return size;
        }
        
        public void add(int index, E element) {
            rangeCheckForAdd(index);
            checkForComodification();
            root.add(offset + index, element);
            updateSizeAndModCount(1);
        }
        
        public E remove(int index) {
            Objects.checkIndex(index, size);
            checkForComodification();
            E result = root.remove(offset + index);
            updateSizeAndModCount(-1);
            return result;
        }
        
        public boolean addAll(Collection<? extends E> c) {
            return addAll(this.size, c);
        }
        
        public boolean addAll(int index, Collection<? extends E> c) {
            rangeCheckForAdd(index);
            int cSize = c.size();
            if(cSize == 0) {
                return false;
            }
            checkForComodification();
            root.addAll(offset + index, c);
            updateSizeAndModCount(cSize);
            return true;
        }
        
        public void replaceAll(UnaryOperator<E> operator) {
            root.replaceAllRange(operator, offset, offset + size);
        }
        
        public boolean removeAll(Collection<?> c) {
            return batchRemove(c, false);
        }
        
        public boolean retainAll(Collection<?> c) {
            return batchRemove(c, true);
        }
        
        public boolean removeIf(Predicate<? super E> filter) {
            checkForComodification();
            int oldSize = root.size;
            boolean modified = root.removeIf(filter, offset, offset + size);
            if(modified) {
                updateSizeAndModCount(root.size - oldSize);
            }
            return modified;
        }
        
        public Object[] toArray() {
            checkForComodification();
            return Arrays.copyOfRange(root.elementData, offset, offset + size);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            checkForComodification();
            if(a.length<size) {
                return (T[]) Arrays.copyOfRange(root.elementData, offset, offset + size, a.getClass());
            }
            System.arraycopy(root.elementData, offset, a, 0, size);
            if(a.length>size) {
                a[size] = null;
            }
            return a;
        }
        
        public boolean equals(Object o) {
            if(o == this) {
                return true;
            }
            
            if(!(o instanceof List)) {
                return false;
            }
            
            boolean equal = root.equalsRange((List<?>) o, offset, offset + size);
            checkForComodification();
            return equal;
        }
        
        public int hashCode() {
            int hash = root.hashCodeRange(offset, offset + size);
            checkForComodification();
            return hash;
        }
        
        public int indexOf(Object o) {
            int index = root.indexOfRange(o, offset, offset + size);
            checkForComodification();
            return index >= 0 ? index - offset : -1;
        }
        
        public int lastIndexOf(Object o) {
            int index = root.lastIndexOfRange(o, offset, offset + size);
            checkForComodification();
            return index >= 0 ? index - offset : -1;
        }
        
        public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }
        
        public Iterator<E> iterator() {
            return listIterator();
        }
        
        public ListIterator<E> listIterator(int index) {
            checkForComodification();
            rangeCheckForAdd(index);
            
            return new ListIterator<E>() {
                int cursor = index;
                int lastRet = -1;
                int expectedModCount = root.modCount;
                
                public boolean hasNext() {
                    return cursor != SubList.this.size;
                }
                
                @SuppressWarnings("unchecked")
                public E next() {
                    checkForComodification();
                    int i = cursor;
                    if(i >= SubList.this.size) {
                        throw new NoSuchElementException();
                    }
                    Object[] elementData = root.elementData;
                    if(offset + i >= elementData.length) {
                        throw new ConcurrentModificationException();
                    }
                    cursor = i + 1;
                    return (E) elementData[offset + (lastRet = i)];
                }
                
                public boolean hasPrevious() {
                    return cursor != 0;
                }
                
                @SuppressWarnings("unchecked")
                public E previous() {
                    checkForComodification();
                    int i = cursor - 1;
                    if(i<0) {
                        throw new NoSuchElementException();
                    }
                    Object[] elementData = root.elementData;
                    if(offset + i >= elementData.length) {
                        throw new ConcurrentModificationException();
                    }
                    cursor = i;
                    return (E) elementData[offset + (lastRet = i)];
                }
                
                public void forEachRemaining(Consumer<? super E> action) {
                    Objects.requireNonNull(action);
                    final int size = SubList.this.size;
                    int i = cursor;
                    if(i<size) {
                        final Object[] es = root.elementData;
                        if(offset + i >= es.length) {
                            throw new ConcurrentModificationException();
                        }
                        for(; i<size && modCount == expectedModCount; i++) {
                            action.accept(elementAt(es, offset + i));
                        }
                        // update once at end to reduce heap write traffic
                        cursor = i;
                        lastRet = i - 1;
                        checkForComodification();
                    }
                }
                
                public int nextIndex() {
                    return cursor;
                }
                
                public int previousIndex() {
                    return cursor - 1;
                }
                
                public void remove() {
                    if(lastRet<0) {
                        throw new IllegalStateException();
                    }
                    checkForComodification();
                    
                    try {
                        SubList.this.remove(lastRet);
                        cursor = lastRet;
                        lastRet = -1;
                        expectedModCount = root.modCount;
                    } catch(IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }
                
                public void set(E e) {
                    if(lastRet<0) {
                        throw new IllegalStateException();
                    }
                    checkForComodification();
                    
                    try {
                        root.set(offset + lastRet, e);
                    } catch(IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }
                
                public void add(E e) {
                    checkForComodification();
                    
                    try {
                        int i = cursor;
                        SubList.this.add(i, e);
                        cursor = i + 1;
                        lastRet = -1;
                        expectedModCount = root.modCount;
                    } catch(IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }
                
                final void checkForComodification() {
                    if(root.modCount != expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                }
            };
        }
        
        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return new SubList<>(this, fromIndex, toIndex);
        }
        
        public Spliterator<E> spliterator() {
            checkForComodification();
            
            // ArrayListSpliterator not used here due to late-binding
            return new Spliterator<E>() {
                private int index = offset; // current index, modified on advance/split
                private int fence = -1; // -1 until used; then one past last index
                private int expectedModCount; // initialized when fence set
                
                public ArrayList<E>.ArrayListSpliterator trySplit() {
                    int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
                    // ArrayListSpliterator can be used here as the source is already bound
                    return (lo >= mid)
                        ? null // divide range in half unless too small
                        : root.new ArrayListSpliterator(lo, index = mid, expectedModCount);
                }
                
                public boolean tryAdvance(Consumer<? super E> action) {
                    Objects.requireNonNull(action);
                    int hi = getFence(), i = index;
                    if(i<hi) {
                        index = i + 1;
                        @SuppressWarnings("unchecked")
                        E e = (E) root.elementData[i];
                        action.accept(e);
                        if(root.modCount != expectedModCount) {
                            throw new ConcurrentModificationException();
                        }
                        return true;
                    }
                    return false;
                }
                
                public void forEachRemaining(Consumer<? super E> action) {
                    Objects.requireNonNull(action);
                    int i, hi, mc; // hoist accesses and checks from loop
                    ArrayList<E> lst = root;
                    Object[] a;
                    if((a = lst.elementData) != null) {
                        if((hi = fence)<0) {
                            mc = modCount;
                            hi = offset + size;
                        } else
                            mc = expectedModCount;
                        if((i = index) >= 0 && (index = hi)<=a.length) {
                            for(; i<hi; ++i) {
                                @SuppressWarnings("unchecked")
                                E e = (E) a[i];
                                action.accept(e);
                            }
                            if(lst.modCount == mc) {
                                return;
                            }
                        }
                    }
                    throw new ConcurrentModificationException();
                }
                
                public long estimateSize() {
                    return getFence() - index;
                }
                
                public int characteristics() {
                    return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
                }
                
                private int getFence() { // initialize fence to size on first use
                    int hi; // (a specialized variant appears in method forEach)
                    if((hi = fence)<0) {
                        expectedModCount = modCount;
                        hi = fence = offset + size;
                    }
                    return hi;
                }
            };
        }
        
        protected void removeRange(int fromIndex, int toIndex) {
            checkForComodification();
            root.removeRange(offset + fromIndex, offset + toIndex);
            updateSizeAndModCount(fromIndex - toIndex);
        }
        
        private boolean batchRemove(Collection<?> c, boolean complement) {
            checkForComodification();
            int oldSize = root.size;
            boolean modified = root.batchRemove(c, complement, offset, offset + size);
            if(modified) {
                updateSizeAndModCount(root.size - oldSize);
            }
            return modified;
        }
        
        private void rangeCheckForAdd(int index) {
            if(index<0 || index>this.size) {
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
            }
        }
        
        private String outOfBoundsMsg(int index) {
            return "Index: " + index + ", Size: " + this.size;
        }
        
        private void checkForComodification() {
            if(root.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
        
        private void updateSizeAndModCount(int sizeChange) {
            SubList<E> slist = this;
            do {
                slist.size += sizeChange;
                slist.modCount = root.modCount;
                slist = slist.parent;
            } while(slist != null);
        }
    }
    
}
