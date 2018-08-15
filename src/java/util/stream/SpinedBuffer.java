/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;

/**
 * An ordered collection of elements.  Elements can be added, but not removed.
 * Goes through a building phase, during which elements can be added, and a
 * traversal phase, during which elements can be traversed in order but no
 * further modifications are possible.
 *
 * <p> One or more arrays are used to store elements. The use of a multiple
 * arrays has better performance characteristics than a single array used by
 * {@link ArrayList}, as when the capacity of the list needs to be increased
 * no copying of elements is required.  This is usually beneficial in the case
 * where the results will be traversed a small number of times.
 *
 * @param <E> the type of elements in this list
 * @since 1.8
 */

// 容量可变的缓冲区，使用一维数组和二维数组做元素缓冲区，只能存取，不能修改，用在流的终端阶段收集数据
class SpinedBuffer<E>
    extends AbstractSpinedBuffer
    implements Consumer<E>, Iterable<E> {
    /*
     * We optimistically hope that all the data will fit into the first chunk,
     * so we try to avoid inflating the spine[] and priorElementCount[] arrays
     * prematurely.  So methods must be prepared to deal with these arrays being
     * null.  If spine is non-null, then spineIndex points to the current chunk
     * within the spine, otherwise it is zero.  The spine and priorElementCount
     * arrays are always the same size, and for any i <= spineIndex,
     * priorElementCount[i] is the sum of the sizes of all the prior chunks.
     *
     * The curChunk pointer is always valid.  The elementIndex is the index of
     * the next element to be written in curChunk; this may be past the end of
     * curChunk so we have to check before writing. When we inflate the spine
     * array, curChunk becomes the first element in it.  When we clear the
     * buffer, we discard all chunks except the first one, which we clear,
     * restoring it to the initial single-chunk state.
     */
    
    private static final int SPLITERATOR_CHARACTERISTICS = Spliterator.SIZED | Spliterator.ORDERED | Spliterator.SUBSIZED;
    
    /**
     * Chunk that we're currently writing into; may or may not be aliased with
     * the first element of the spine.
     */
    // 一维缓存，初始时为curChunk分配容量，之后将为spine分配容量，并让curChunk指向spine新分配的行
    protected E[] curChunk;
    
    /**
     * All chunks, or null if there is only one chunk.
     */
    /*
     * 二维缓存，初始时不分配容量，只是让spine[0]指向curChunk，之后为spine的每一行分配容量
     * spine中的每一行称作一个chunk
     */
    protected E[][] spine;
    
    /**
     * Constructs an empty list with an initial capacity of sixteen.
     */
    @SuppressWarnings("unchecked")
    SpinedBuffer() {
        super();
        curChunk = (E[]) new Object[1 << initialChunkPower];
    }
    
    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
    @SuppressWarnings("unchecked")
    SpinedBuffer(int initialCapacity) {
        super(initialCapacity);
        curChunk = (E[]) new Object[1 << initialChunkPower];
    }
    
    // 向SpinedBuffer存入一个元素
    @Override
    public void accept(E e) {
        // 如果一维缓存已满，需要扩容
        if (elementIndex == curChunk.length) {
            // 初始化二维缓存
            inflateSpine();
            if (spineIndex+1 >= spine.length || spine[spineIndex+1] == null) {
                // 扩容
                increaseCapacity();
            }
            elementIndex = 0;
            ++spineIndex;
            curChunk = spine[spineIndex];
        }
        
        curChunk[elementIndex++] = e;
    }
    
    /**
     * Retrieve the element at the specified index.
     */
    // 返回索引index处的元素
    public E get(long index) {
        // @@@ can further optimize by caching last seen spineIndex,
        // which is going to be right most of the time
        
        /*
         * Casts to int are safe since the spine array index is the index minus the prior element count from the current spine
         */
        //
        if (spineIndex == 0) {
            if (index < elementIndex)
                return curChunk[((int) index)];
            else
                throw new IndexOutOfBoundsException(Long.toString(index));
        }
        
        if (index >= count())
            throw new IndexOutOfBoundsException(Long.toString(index));
        
        for (int j=0; j <= spineIndex; j++)
            if (index < priorElementCount[j] + spine[j].length)
                return spine[j][((int) (index - priorElementCount[j]))];
        
        throw new IndexOutOfBoundsException(Long.toString(index));
    }
    
    /**
     * Copy the elements, starting at the specified offset, into the specified
     * array.
     */
    // 将SpinedBuffer中的内容复制到数组array的offset偏移中
    public void copyInto(E[] array, int offset) {
        long finalOffset = offset + count();
        if (finalOffset > array.length || finalOffset < offset) {
            throw new IndexOutOfBoundsException("does not fit");
        }
        
        if (spineIndex == 0)
            System.arraycopy(curChunk, 0, array, offset, elementIndex);
        else {
            // full chunks
            for (int i=0; i < spineIndex; i++) {
                System.arraycopy(spine[i], 0, array, offset, spine[i].length);
                offset += spine[i].length;
            }
            if (elementIndex > 0) {
                System.arraycopy(curChunk, 0, array, offset, elementIndex);
            }
        }
    }
    
    /**
     * Create a new array using the specified array factory, and copy the
     * elements into it.
     */
    // 将SpinedBuffer中的内容复制到数组中返回
    public E[] asArray(IntFunction<E[]> arrayFactory) {
        long size = count();
        if (size >= Nodes.MAX_ARRAY_SIZE)
            throw new IllegalArgumentException(Nodes.BAD_SIZE);
        E[] result = arrayFactory.apply((int) size);
        copyInto(result, 0);
        return result;
    }
    
    // 清空SpinedBuffer
    @Override
    public void clear() {
        if (spine != null) {
            curChunk = spine[0];
            for (int i=0; i<curChunk.length; i++)
                curChunk[i] = null;
            spine = null;
            priorElementCount = null;
        }
        else {
            for (int i=0; i<elementIndex; i++) {
                curChunk[i] = null;
            }
        }
        elementIndex = 0;
        spineIndex = 0;
    }
    
    // 遍历SpinedBuffer中的元素，并在其上应用consumer函数
    @Override
    public void forEach(Consumer<? super E> consumer) {
        // completed chunks, if any
        for (int j = 0; j < spineIndex; j++)
            for (E t : spine[j])
                consumer.accept(t);
        
        // current chunk
        for (int i=0; i<elementIndex; i++)
            consumer.accept(curChunk[i]);
    }
    
    /**
     * Returns the current capacity of the buffer
     */
    // 返回当前SpinedBuffer的容量
    protected long capacity() {
        return (spineIndex == 0)
            ? curChunk.length
            : priorElementCount[spineIndex] + spine[spineIndex].length;
    }
    
    /**
     * Ensure that the buffer has at least capacity to hold the target size
     */
    // 传入需要的容量，确保SpinedBuffer容量充足，不够的话就分配
    @SuppressWarnings("unchecked")
    protected final void ensureCapacity(long targetSize) {
        // 返回当前SpinedBuffer的容量
        long capacity = capacity();
        // 需要的容量比当前容量大，则需要申请新的空间
        if (targetSize > capacity) {
            // 确保二维缓存已初始化
            inflateSpine();
            for (int i=spineIndex+1; targetSize > capacity; i++) {
                // 如果二维缓存也不够用了，需要对二维缓存扩容
                if (i >= spine.length) {
                    int newSpineSize = spine.length * 2;
                    spine = Arrays.copyOf(spine, newSpineSize);
                    priorElementCount = Arrays.copyOf(priorElementCount, newSpineSize);
                }
                
                // 返回即将分配的chunk应当包含的元素个数
                int nextChunkSize = chunkSize(i);
                // 为二维缓存增加一行的空间
                spine[i] = (E[]) new Object[nextChunkSize];
                // 累计元素个数
                priorElementCount[i] = priorElementCount[i-1] + spine[i-1].length;
                capacity += nextChunkSize;
            }
        }
    }
    
    /**
     * Force the buffer to increase its capacity.
     */
    // 扩容，比当前容量多一个元素，往往会分配更多的空间
    protected void increaseCapacity() {
        // 确保SpinedBuffer容量充足
        ensureCapacity(capacity() + 1);
    }
    
    // 初始化二维缓存
    @SuppressWarnings("unchecked")
    private void inflateSpine() {
        if (spine == null) {
            spine = (E[][]) new Object[MIN_SPINE_SIZE][];
            priorElementCount = new long[MIN_SPINE_SIZE];
            spine[0] = curChunk;
        }
    }
    
    // 返回适用于该SpinedBuffer的Iterator
    @Override
    public Iterator<E> iterator() {
        // 返回(4)类Spliterator：将Spliterator适配到Iterator来使用
        return Spliterators.iterator(spliterator());
    }
    
    /**
     * Return a {@link Spliterator} describing the contents of the buffer.
     */
    // 返回描述此SpinedBuffer中元素的Spliterator
    public Spliterator<E> spliterator() {
        class Splitr implements Spliterator<E> {
            // The current spine index
            int splSpineIndex;                  // 二维缓存起始索引
            // Last spine index
            final int lastSpineIndex;           // 二维缓存终点索引
            // The current element index into the current spine
            int splElementIndex;                // 一维缓存起始索引
            // Last spine's last element index + 1
            final int lastSpineElementFence;    // 一维缓存终点索引
            
            /*
             * When splSpineIndex >= lastSpineIndex
             * and splElementIndex >= lastSpineElementFence
             * then this spliterator is fully traversed
             * tryAdvance can set splSpineIndex > spineIndex if the last spine is full
             */
            // The current spine array
            E[] splChunk;   // 存储当前的chunk，内容会随着遍历而变化
            
            Splitr(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                this.splSpineIndex = firstSpineIndex;
                this.lastSpineIndex = lastSpineIndex;
                this.splElementIndex = firstSpineElementIndex;
                this.lastSpineElementFence = lastSpineElementFence;
                assert spine != null || firstSpineIndex == 0 && lastSpineIndex == 0;
                splChunk = (spine == null) ? curChunk : spine[firstSpineIndex];
            }
            
            // 返回当前情境中的元素数量（可能是估算值）
            @Override
            public long estimateSize() {
                return (splSpineIndex == lastSpineIndex)
                    ? (long) lastSpineElementFence - splElementIndex
                    : priorElementCount[lastSpineIndex] + lastSpineElementFence -   // # of elements prior to end -
                    priorElementCount[splSpineIndex] - splElementIndex;           // # of elements prior to current
            }
            
            @Override
            public int characteristics() {
                return SPLITERATOR_CHARACTERISTICS;
            }
            
            // 对容器中的单个当前元素执行择取操作
            @Override
            public boolean tryAdvance(Consumer<? super E> consumer) {
                Objects.requireNonNull(consumer);
                
                if(splSpineIndex < lastSpineIndex || (splSpineIndex == lastSpineIndex && splElementIndex < lastSpineElementFence)) {
                    consumer.accept(splChunk[splElementIndex++]);
                    
                    if(splElementIndex == splChunk.length) {
                        splElementIndex = 0;
                        ++splSpineIndex;
                        if(spine != null && splSpineIndex <= lastSpineIndex)
                            splChunk = spine[splSpineIndex];
                    }
                    return true;
                }
                return false;
            }
            
            // 遍历容器内每个元素，在其上执行相应的择取操作
            @Override
            public void forEachRemaining(Consumer<? super E> consumer) {
                Objects.requireNonNull(consumer);
                
                if(splSpineIndex < lastSpineIndex || (splSpineIndex == lastSpineIndex && splElementIndex < lastSpineElementFence)) {
                    int i = splElementIndex;
                    // completed chunks, if any
                    for(int sp = splSpineIndex; sp < lastSpineIndex; sp++) {
                        E[] chunk = spine[sp];
                        for(; i < chunk.length; i++) {
                            consumer.accept(chunk[i]);
                        }
                        i = 0;
                    }
                    // last (or current uncompleted) chunk
                    E[] chunk = (splSpineIndex == lastSpineIndex) ? splChunk : spine[lastSpineIndex];
                    int hElementIndex = lastSpineElementFence;
                    for(; i < hElementIndex; i++) {
                        consumer.accept(chunk[i]);
                    }
                    // mark consumed
                    splSpineIndex = lastSpineIndex;
                    splElementIndex = lastSpineElementFence;
                }
            }
            
            // 从容器的指定范围切割一段元素，将其打包到Spliterator后返回，特征值不变
            @Override
            public Spliterator<E> trySplit() {
                if(splSpineIndex < lastSpineIndex) {    // 丢弃未完成的那行chunk
                    // split just before last chunk (if it is full this means 50:50 split)
                    Spliterator<E> ret = new Splitr(splSpineIndex, lastSpineIndex - 1, splElementIndex, spine[lastSpineIndex - 1].length);
                    // position to start of last chunk
                    splSpineIndex = lastSpineIndex;
                    splElementIndex = 0;
                    splChunk = spine[splSpineIndex];
                    return ret;
                } else if(splSpineIndex == lastSpineIndex) {    // 折半
                    int t = (lastSpineElementFence - splElementIndex) / 2;
                    if(t == 0)
                        return null;
                    else {
                        Spliterator<E> ret = Arrays.spliterator(splChunk, splElementIndex, splElementIndex + t);
                        splElementIndex += t;
                        return ret;
                    }
                } else {
                    return null;
                }
            }
        }
        
        return new Splitr(0, spineIndex, 0, elementIndex);
    }
    
    @Override
    public String toString() {
        List<E> list = new ArrayList<>();
        forEach(list::add);
        return "SpinedBuffer:" + list.toString();
    }
    
    /**
     * An ordered collection of primitive values.  Elements can be added, but
     * not removed. Goes through a building phase, during which elements can be
     * added, and a traversal phase, during which elements can be traversed in
     * order but no further modifications are possible.
     *
     * <p> One or more arrays are used to store elements. The use of a multiple
     * arrays has better performance characteristics than a single array used by
     * {@link ArrayList}, as when the capacity of the list needs to be increased
     * no copying of elements is required.  This is usually beneficial in the case
     * where the results will be traversed a small number of times.
     *
     * @param <E>      the wrapper type for this primitive type
     * @param <T_ARR>  the array type for this primitive type
     * @param <T_CONS> the Consumer type for this primitive type
     */
    // 为基本类型特化的SpinedBuffer
    abstract static class OfPrimitive<E, T_ARR, T_CONS>
        extends AbstractSpinedBuffer
        implements Iterable<E> {
        
        /*
         * We optimistically hope that all the data will fit into the first chunk,
         * so we try to avoid inflating the spine[] and priorElementCount[] arrays
         * prematurely.  So methods must be prepared to deal with these arrays being
         * null.  If spine is non-null, then spineIndex points to the current chunk
         * within the spine, otherwise it is zero.  The spine and priorElementCount
         * arrays are always the same size, and for any i <= spineIndex,
         * priorElementCount[i] is the sum of the sizes of all the prior chunks.
         *
         * The curChunk pointer is always valid.  The elementIndex is the index of
         * the next element to be written in curChunk; this may be past the end of
         * curChunk so we have to check before writing. When we inflate the spine
         * array, curChunk becomes the first element in it.  When we clear the
         * buffer, we discard all chunks except the first one, which we clear,
         * restoring it to the initial single-chunk state.
         */
        
        // The chunk we're currently writing into
        T_ARR curChunk; // 一维缓存
        
        // All chunks, or null if there is only one chunk
        T_ARR[] spine;  // 二维缓存
        
        /**
         * Constructs an empty list with an initial capacity of sixteen.
         */
        OfPrimitive() {
            super();
            curChunk = newArray(1 << initialChunkPower);
        }
        
        /**
         * Constructs an empty list with the specified initial capacity.
         *
         * @param initialCapacity the initial capacity of the list
         *
         * @throws IllegalArgumentException if the specified initial capacity
         *                                  is negative
         */
        OfPrimitive(int initialCapacity) {
            super(initialCapacity);
            curChunk = newArray(1 << initialChunkPower);
        }
        
        // 返回适用于该SpinedBuffer的Iterator
        @Override
        public abstract Iterator<E> iterator();
        
        // 遍历SpinedBuffer中的元素，并在其上应用consumer函数
        @Override
        public abstract void forEach(Consumer<? super E> consumer);
        
        /** Create a new array of the proper type and size */
        // 创建T_ARR类型的一维数组
        public abstract T_ARR newArray(int size);
        
        /** Create a new array-of-array of the proper type and size */
        // 创建T_ARR类型的二维数组
        protected abstract T_ARR[] newArrayArray(int size);
        
        /** Get the length of an array */
        // 返回数组array的容量
        protected abstract int arrayLength(T_ARR array);
        
        /** Iterate an array with the provided consumer */
        // 遍历数组form到to范围的元素，在其上应用consumer函数
        protected abstract void arrayForEach(T_ARR array, int from, int to, T_CONS consumer);
        
        // 传入需要的容量，确保SpinedBuffer容量充足，不够的话就分配
        protected final void ensureCapacity(long targetSize) {
            long capacity = capacity();
            if(targetSize > capacity) {
                inflateSpine();
                for(int i = spineIndex + 1; targetSize > capacity; i++) {
                    if(i >= spine.length) {
                        int newSpineSize = spine.length * 2;
                        spine = Arrays.copyOf(spine, newSpineSize);
                        priorElementCount = Arrays.copyOf(priorElementCount, newSpineSize);
                    }
                    // 返回即将分配的chunk应当包含的元素个数
                    int nextChunkSize = chunkSize(i);
                    spine[i] = newArray(nextChunkSize);
                    priorElementCount[i] = priorElementCount[i - 1] + arrayLength(spine[i - 1]);
                    capacity += nextChunkSize;
                }
            }
        }
        
        // 将SpinedBuffer中的内容复制到数组array的offset偏移中
        public void copyInto(T_ARR array, int offset) {
            long finalOffset = offset + count();
            if(finalOffset > arrayLength(array) || finalOffset < offset) {
                throw new IndexOutOfBoundsException("does not fit");
            }
            
            if(spineIndex == 0)
                System.arraycopy(curChunk, 0, array, offset, elementIndex);
            else {
                // full chunks
                for(int i = 0; i < spineIndex; i++) {
                    System.arraycopy(spine[i], 0, array, offset, arrayLength(spine[i]));
                    offset += arrayLength(spine[i]);
                }
                if(elementIndex > 0)
                    System.arraycopy(curChunk, 0, array, offset, elementIndex);
            }
        }
        
        // 将SpinedBuffer中的元素存入基本类型数组后返回
        public T_ARR asPrimitiveArray() {
            long size = count();
            if(size >= Nodes.MAX_ARRAY_SIZE)
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            T_ARR result = newArray((int) size);
            copyInto(result, 0);
            return result;
        }
        
        // 清空SpinedBuffer
        public void clear() {
            if(spine != null) {
                curChunk = spine[0];
                spine = null;
                priorElementCount = null;
            }
            elementIndex = 0;
            spineIndex = 0;
        }
        
        // 遍历SpinedBuffer中的元素，并在其上应用consumer函数
        @SuppressWarnings("overloads")
        public void forEach(T_CONS consumer) {
            // completed chunks, if any
            for(int j = 0; j < spineIndex; j++)
                arrayForEach(spine[j], 0, arrayLength(spine[j]), consumer);
            
            // current chunk
            arrayForEach(curChunk, 0, elementIndex, consumer);
        }
        
        // 返回当前SpinedBuffer的容量
        protected long capacity() {
            return (spineIndex == 0)
                ? arrayLength(curChunk)
                : priorElementCount[spineIndex] + arrayLength(spine[spineIndex]);
        }
        
        // 扩容，比当前容量多一个元素，往往会分配更多的空间
        protected void increaseCapacity() {
            ensureCapacity(capacity() + 1);
        }
        
        // 查找索引index处的元素所在的chunk的索引
        protected int chunkFor(long index) {
            if(spineIndex == 0) {
                if(index < elementIndex)
                    return 0;
                else
                    throw new IndexOutOfBoundsException(Long.toString(index));
            }
            
            if(index >= count())
                throw new IndexOutOfBoundsException(Long.toString(index));
            
            for(int j = 0; j <= spineIndex; j++)
                if(index < priorElementCount[j] + arrayLength(spine[j]))
                    return j;
            
            throw new IndexOutOfBoundsException(Long.toString(index));
        }
        
        // 预存，即判断当前容量是否充足，不充足的话需要扩容
        protected void preAccept() {
            if(elementIndex == arrayLength(curChunk)) {
                inflateSpine();
                if(spineIndex + 1 >= spine.length || spine[spineIndex + 1] == null)
                    increaseCapacity();
                elementIndex = 0;
                ++spineIndex;
                curChunk = spine[spineIndex];
            }
        }
        
        // 初始化二维缓存
        private void inflateSpine() {
            if(spine == null) {
                spine = newArrayArray(MIN_SPINE_SIZE);
                priorElementCount = new long[MIN_SPINE_SIZE];
                spine[0] = curChunk;
            }
        }
        
        // Spliterator抽象基类，用来描述为基本类型特化的SpinedBuffer中的元素
        abstract class BaseSpliterator<T_SPLITR extends Spliterator.OfPrimitive<E, T_CONS, T_SPLITR>>
            implements Spliterator.OfPrimitive<E, T_CONS, T_SPLITR> {
            // Last spine index
            final int lastSpineIndex;
            // Last spine's last element index + 1
            final int lastSpineElementFence;
            // The current spine index
            int splSpineIndex;
            // The current element index into the current spine
            int splElementIndex;
            
            // When splSpineIndex >= lastSpineIndex and
            // splElementIndex >= lastSpineElementFence then
            // this spliterator is fully traversed
            // tryAdvance can set splSpineIndex > spineIndex if the last spine is full
            // The current spine array
            T_ARR splChunk;
            
            BaseSpliterator(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                this.splSpineIndex = firstSpineIndex;
                this.lastSpineIndex = lastSpineIndex;
                this.splElementIndex = firstSpineElementIndex;
                this.lastSpineElementFence = lastSpineElementFence;
                assert spine != null || firstSpineIndex == 0 && lastSpineIndex == 0;
                splChunk = (spine == null) ? curChunk : spine[firstSpineIndex];
            }
            
            abstract T_SPLITR newSpliterator(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence);
            
            abstract void arrayForOne(T_ARR array, int index, T_CONS consumer);
            
            abstract T_SPLITR arraySpliterator(T_ARR array, int offset, int len);
            
            @Override
            public long estimateSize() {
                return (splSpineIndex == lastSpineIndex) ? (long) lastSpineElementFence - splElementIndex : // # of elements prior to end -
                    priorElementCount[lastSpineIndex] + lastSpineElementFence -
                        // # of elements prior to current
                        priorElementCount[splSpineIndex] - splElementIndex;
            }
            
            @Override
            public int characteristics() {
                return SPLITERATOR_CHARACTERISTICS;
            }
            
            @Override
            public boolean tryAdvance(T_CONS consumer) {
                Objects.requireNonNull(consumer);
                
                if(splSpineIndex < lastSpineIndex || (splSpineIndex == lastSpineIndex && splElementIndex < lastSpineElementFence)) {
                    arrayForOne(splChunk, splElementIndex++, consumer);
                    
                    if(splElementIndex == arrayLength(splChunk)) {
                        splElementIndex = 0;
                        ++splSpineIndex;
                        if(spine != null && splSpineIndex <= lastSpineIndex)
                            splChunk = spine[splSpineIndex];
                    }
                    return true;
                }
                return false;
            }
            
            @Override
            public void forEachRemaining(T_CONS consumer) {
                Objects.requireNonNull(consumer);
                
                if(splSpineIndex < lastSpineIndex || (splSpineIndex == lastSpineIndex && splElementIndex < lastSpineElementFence)) {
                    int i = splElementIndex;
                    // completed chunks, if any
                    for(int sp = splSpineIndex; sp < lastSpineIndex; sp++) {
                        T_ARR chunk = spine[sp];
                        arrayForEach(chunk, i, arrayLength(chunk), consumer);
                        i = 0;
                    }
                    // last (or current uncompleted) chunk
                    T_ARR chunk = (splSpineIndex == lastSpineIndex) ? splChunk : spine[lastSpineIndex];
                    arrayForEach(chunk, i, lastSpineElementFence, consumer);
                    // mark consumed
                    splSpineIndex = lastSpineIndex;
                    splElementIndex = lastSpineElementFence;
                }
            }
            
            @Override
            public T_SPLITR trySplit() {
                if(splSpineIndex < lastSpineIndex) {
                    // split just before last chunk (if it is full this means 50:50 split)
                    T_SPLITR ret = newSpliterator(splSpineIndex, lastSpineIndex - 1, splElementIndex, arrayLength(spine[lastSpineIndex - 1]));
                    // position us to start of last chunk
                    splSpineIndex = lastSpineIndex;
                    splElementIndex = 0;
                    splChunk = spine[splSpineIndex];
                    return ret;
                } else if(splSpineIndex == lastSpineIndex) {
                    int t = (lastSpineElementFence - splElementIndex) / 2;
                    if(t == 0)
                        return null;
                    else {
                        T_SPLITR ret = arraySpliterator(splChunk, splElementIndex, t);
                        splElementIndex += t;
                        return ret;
                    }
                } else {
                    return null;
                }
            }
        }
    }
    
    /**
     * An ordered collection of {@code int} values.
     */
    // 为int类型特化的SpinedBuffer
    static class OfInt
        extends SpinedBuffer.OfPrimitive<Integer, int[], IntConsumer>
        implements IntConsumer {
        
        OfInt() {
        }
        
        OfInt(int initialCapacity) {
            super(initialCapacity);
        }
        
        // 遍历SpinedBuffer中的元素，并在其上应用consumer函数
        @Override
        public void forEach(Consumer<? super Integer> consumer) {
            if(consumer instanceof IntConsumer) {
                forEach((IntConsumer) consumer);
            } else {
                if(Tripwire.ENABLED)
                    Tripwire.trip(getClass(), "{0} calling SpinedBuffer.OfInt.forEach(Consumer)");
                spliterator().forEachRemaining(consumer);
            }
        }
        
        // 创建int[]数组
        @Override
        public int[] newArray(int size) {
            return new int[size];
        }
        
        // 创建int[][]数组
        @Override
        protected int[][] newArrayArray(int size) {
            return new int[size][];
        }
        
        // 将元素i存入SpinedBuffer
        @Override
        public void accept(int i) {
            preAccept();
            curChunk[elementIndex++] = i;
        }
        
        // 返回索引index处的元素
        public int get(long index) {
            // Casts to int are safe since the spine array index is the index minus the prior element count from the current spine
            int ch = chunkFor(index);
            if(spineIndex == 0 && ch == 0)
                return curChunk[(int) index];
            else
                return spine[ch][(int) (index - priorElementCount[ch])];
        }
        
        // 返回数组array的容量
        @Override
        protected int arrayLength(int[] array) {
            return array.length;
        }
        
        // 遍历数组form到to范围的元素，在其上应用consumer函数
        @Override
        protected void arrayForEach(int[] array, int from, int to, IntConsumer consumer) {
            for(int i = from; i < to; i++) {
                consumer.accept(array[i]);
            }
        }
        
        // 返回适用于该SpinedBuffer的Iterator
        @Override
        public PrimitiveIterator.OfInt iterator() {
            return Spliterators.iterator(spliterator());
        }
        
        // 返回描述此SpinedBuffer中元素的Spliterator
        public Spliterator.OfInt spliterator() {
            class Splitr
                extends BaseSpliterator<Spliterator.OfInt>
                implements Spliterator.OfInt {
                
                Splitr(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                    super(firstSpineIndex, lastSpineIndex, firstSpineElementIndex, lastSpineElementFence);
                }
                
                @Override
                Splitr newSpliterator(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                    return new Splitr(firstSpineIndex, lastSpineIndex, firstSpineElementIndex, lastSpineElementFence);
                }
                
                @Override
                void arrayForOne(int[] array, int index, IntConsumer consumer) {
                    consumer.accept(array[index]);
                }
                
                @Override
                Spliterator.OfInt arraySpliterator(int[] array, int offset, int len) {
                    return Arrays.spliterator(array, offset, offset + len);
                }
            }
            
            return new Splitr(0, spineIndex, 0, elementIndex);
        }
        
        @Override
        public String toString() {
            int[] array = asPrimitiveArray();
            if(array.length < 200) {
                return String.format("%s[length=%d, chunks=%d]%s", getClass().getSimpleName(), array.length, spineIndex, Arrays.toString(array));
            } else {
                int[] array2 = Arrays.copyOf(array, 200);
                return String.format("%s[length=%d, chunks=%d]%s...", getClass().getSimpleName(), array.length, spineIndex, Arrays.toString(array2));
            }
        }
    }
    
    /**
     * An ordered collection of {@code long} values.
     */
    // 为long类型特化的SpinedBuffer
    static class OfLong
        extends SpinedBuffer.OfPrimitive<Long, long[], LongConsumer>
        implements LongConsumer {
        
        OfLong() {
        }
        
        OfLong(int initialCapacity) {
            super(initialCapacity);
        }
        
        // 遍历SpinedBuffer中的元素，并在其上应用consumer函数
        @Override
        public void forEach(Consumer<? super Long> consumer) {
            if(consumer instanceof LongConsumer) {
                forEach((LongConsumer) consumer);
            } else {
                if(Tripwire.ENABLED)
                    Tripwire.trip(getClass(), "{0} calling SpinedBuffer.OfLong.forEach(Consumer)");
                spliterator().forEachRemaining(consumer);
            }
        }
        
        // 创建long[]数组
        @Override
        public long[] newArray(int size) {
            return new long[size];
        }
        
        // 创建long[][]数组
        @Override
        protected long[][] newArrayArray(int size) {
            return new long[size][];
        }
        
        // 将元素i存入SpinedBuffer
        @Override
        public void accept(long i) {
            preAccept();
            curChunk[elementIndex++] = i;
        }
        
        // 返回索引index处的元素
        public long get(long index) {
            // Casts to int are safe since the spine array index is the index minus
            // the prior element count from the current spine
            int ch = chunkFor(index);
            if(spineIndex == 0 && ch == 0)
                return curChunk[(int) index];
            else
                return spine[ch][(int) (index - priorElementCount[ch])];
        }
        
        // 返回数组array的容量
        @Override
        protected int arrayLength(long[] array) {
            return array.length;
        }
        
        // 遍历数组form到to范围的元素，在其上应用consumer函数
        @Override
        protected void arrayForEach(long[] array, int from, int to, LongConsumer consumer) {
            for(int i = from; i < to; i++)
                consumer.accept(array[i]);
        }
        
        // 返回适用于该SpinedBuffer的Iterator
        @Override
        public PrimitiveIterator.OfLong iterator() {
            return Spliterators.iterator(spliterator());
        }
        
        // 返回描述此SpinedBuffer中元素的Spliterator
        public Spliterator.OfLong spliterator() {
            class Splitr extends BaseSpliterator<Spliterator.OfLong> implements Spliterator.OfLong {
                Splitr(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                    super(firstSpineIndex, lastSpineIndex, firstSpineElementIndex, lastSpineElementFence);
                }
                
                @Override
                Splitr newSpliterator(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                    return new Splitr(firstSpineIndex, lastSpineIndex, firstSpineElementIndex, lastSpineElementFence);
                }
                
                @Override
                void arrayForOne(long[] array, int index, LongConsumer consumer) {
                    consumer.accept(array[index]);
                }
                
                @Override
                Spliterator.OfLong arraySpliterator(long[] array, int offset, int len) {
                    return Arrays.spliterator(array, offset, offset + len);
                }
            }
            return new Splitr(0, spineIndex, 0, elementIndex);
        }
        
        @Override
        public String toString() {
            long[] array = asPrimitiveArray();
            if(array.length < 200) {
                return String.format("%s[length=%d, chunks=%d]%s", getClass().getSimpleName(), array.length, spineIndex, Arrays.toString(array));
            } else {
                long[] array2 = Arrays.copyOf(array, 200);
                return String.format("%s[length=%d, chunks=%d]%s...", getClass().getSimpleName(), array.length, spineIndex, Arrays.toString(array2));
            }
        }
    }
    
    /**
     * An ordered collection of {@code double} values.
     */
    // 为double类型特化的SpinedBuffer
    static class OfDouble extends SpinedBuffer.OfPrimitive<Double, double[], DoubleConsumer>
        implements DoubleConsumer {
        
        OfDouble() {
        }
        
        OfDouble(int initialCapacity) {
            super(initialCapacity);
        }
        
        // 遍历SpinedBuffer中的元素，并在其上应用consumer函数
        @Override
        public void forEach(Consumer<? super Double> consumer) {
            if(consumer instanceof DoubleConsumer) {
                forEach((DoubleConsumer) consumer);
            } else {
                if(Tripwire.ENABLED)
                    Tripwire.trip(getClass(), "{0} calling SpinedBuffer.OfDouble.forEach(Consumer)");
                spliterator().forEachRemaining(consumer);
            }
        }
        
        // 创建double[]数组
        @Override
        public double[] newArray(int size) {
            return new double[size];
        }
        
        // 创建double[][]数组
        @Override
        protected double[][] newArrayArray(int size) {
            return new double[size][];
        }
        
        // 将元素i存入SpinedBuffer
        @Override
        public void accept(double i) {
            preAccept();
            curChunk[elementIndex++] = i;
        }
        
        // 返回索引index处的元素
        public double get(long index) {
            // Casts to int are safe since the spine array index is the index minus
            // the prior element count from the current spine
            int ch = chunkFor(index);
            if(spineIndex == 0 && ch == 0)
                return curChunk[(int) index];
            else
                return spine[ch][(int) (index - priorElementCount[ch])];
        }
        
        // 返回数组array的容量
        @Override
        protected int arrayLength(double[] array) {
            return array.length;
        }
        
        // 遍历数组form到to范围的元素，在其上应用consumer函数
        @Override
        protected void arrayForEach(double[] array, int from, int to, DoubleConsumer consumer) {
            for(int i = from; i < to; i++)
                consumer.accept(array[i]);
        }
        
        // 返回适用于该SpinedBuffer的Iterator
        @Override
        public PrimitiveIterator.OfDouble iterator() {
            return Spliterators.iterator(spliterator());
        }
        
        // 返回描述此SpinedBuffer中元素的Spliterator
        public Spliterator.OfDouble spliterator() {
            class Splitr extends BaseSpliterator<Spliterator.OfDouble> implements Spliterator.OfDouble {
                Splitr(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                    super(firstSpineIndex, lastSpineIndex, firstSpineElementIndex, lastSpineElementFence);
                }
                
                @Override
                Splitr newSpliterator(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                    return new Splitr(firstSpineIndex, lastSpineIndex, firstSpineElementIndex, lastSpineElementFence);
                }
                
                @Override
                void arrayForOne(double[] array, int index, DoubleConsumer consumer) {
                    consumer.accept(array[index]);
                }
                
                @Override
                Spliterator.OfDouble arraySpliterator(double[] array, int offset, int len) {
                    return Arrays.spliterator(array, offset, offset + len);
                }
            }
            return new Splitr(0, spineIndex, 0, elementIndex);
        }
        
        @Override
        public String toString() {
            double[] array = asPrimitiveArray();
            if(array.length < 200) {
                return String.format("%s[length=%d, chunks=%d]%s", getClass().getSimpleName(), array.length, spineIndex, Arrays.toString(array));
            } else {
                double[] array2 = Arrays.copyOf(array, 200);
                return String.format("%s[length=%d, chunks=%d]%s...", getClass().getSimpleName(), array.length, spineIndex, Arrays.toString(array2));
            }
        }
    }
}
