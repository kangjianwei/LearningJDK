/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;

/**
 * An immutable container for describing an ordered sequence of elements of some type {@code T}.
 *
 * <p>A {@code Node} contains a fixed number of elements, which can be accessed
 * via the {@link #count}, {@link #spliterator}, {@link #forEach},
 * {@link #asArray}, or {@link #copyInto} methods.  A {@code Node} may have zero
 * or more child {@code Node}s; if it has no children (accessed via
 * {@link #getChildCount} and {@link #getChild(int)}, it is considered <em>flat
 * </em> or a <em>leaf</em>; if it has children, it is considered an
 * <em>internal</em> node.  The size of an internal node is the sum of sizes of
 * its children.
 *
 * @param <T> the type of elements.
 *
 * @apiNote <p>A {@code Node} typically does not store the elements directly, but instead
 * mediates access to one or more existing (effectively immutable) data
 * structures such as a {@code Collection}, array, or a set of other
 * {@code Node}s.  Commonly {@code Node}s are formed into a tree whose shape
 * corresponds to the computation tree that produced the elements that are
 * contained in the leaf nodes.  The use of {@code Node} within the stream
 * framework is largely to avoid copying data unnecessarily during parallel
 * operations.
 * @since 1.8
 */

/*
 * 一个不可变的容器，用于描述某种类型{T}的有序元素序列
 *
 * {Node}包含固定数量的元素，可以通过{#count}，{#spliterator}，{#forEach}，{#asArray}或{#copyInto}方法访问这些元素。
 * {Node}可以有零个或多个子节点；
 * 如果它没有子节点（通过{#getChildCount}和{#getChild(int)}访问，则它被认为是平面或叶子;
 * 如果它有子节点，则它被认为是内部节点。
 * 内部节点的大小是总和其子女的大小。
 *
 * {Node}通常不直接存储元素，而是对一个或多个现有数据结构的访问，例如{Collection}，数组或一组其他{Node} 。
 *
 * 在流框架中使用{Node}主要是为了避免在并行操作期间不必要地复制数据。
 */
interface Node<T> {
    
    /**
     * Returns a {@link Spliterator} describing the elements contained in this
     * {@code Node}.
     *
     * @return a {@code Spliterator} describing the elements contained in this
     * {@code Node}
     */
    // 返回描述此Node中元素的Spliterator
    Spliterator<T> spliterator();
    
    /**
     * Traverses the elements of this node, and invoke the provided
     * {@code Consumer} with each element.  Elements are provided in encounter
     * order if the source for the {@code Node} has a defined encounter order.
     *
     * @param consumer a {@code Consumer} that is to be invoked with each
     *                 element in this {@code Node}
     */
    // 遍历Node中的元素，并在其上执行Consumer操作
    void forEach(Consumer<? super T> consumer);
    
    /**
     * Provides an array view of the contents of this node.
     *
     * <p>Depending on the underlying implementation, this may return a
     * reference to an internal array rather than a copy.  Since the returned
     * array may be shared, the returned array should not be modified.  The
     * {@code generator} function may be consulted to create the array if a new
     * array needs to be created.
     *
     * @param generator a factory function which takes an integer parameter and
     *                  returns a new, empty array of that size and of the appropriate
     *                  array type
     *
     * @return an array containing the contents of this {@code Node}
     */
    // 返回Node内部数据的数组视图
    T[] asArray(IntFunction<T[]> generator);
    
    /**
     * Copies the content of this {@code Node} into an array, starting at a
     * given offset into the array.  It is the caller's responsibility to ensure
     * there is sufficient room in the array, otherwise unspecified behaviour
     * will occur if the array length is less than the number of elements
     * contained in this node.
     *
     * @param array  the array into which to copy the contents of this
     *               {@code Node}
     * @param offset the starting offset within the array
     *
     * @throws IndexOutOfBoundsException if copying would cause access of data
     *                                   outside array bounds
     * @throws NullPointerException      if {@code array} is {@code null}
     */
    // 将Node的内容复制到数组array中offset偏移处
    void copyInto(T[] array, int offset);
    
    /**
     * Returns the number of elements contained in this node.
     *
     * @return the number of elements contained in this node
     */
    // 返回Node中包含的元素数量
    long count();
    
    /**
     * Returns the number of child nodes of this node.
     *
     * @return the number of child nodes
     *
     * @implSpec The default implementation returns zero.
     */
    // 返回子Node数量
    default int getChildCount() {
        return 0;
    }
    
    /**
     * Retrieves the child {@code Node} at a given index.
     *
     * @param i the index to the child node
     *
     * @return the child node
     *
     * @throws IndexOutOfBoundsException if the index is less than 0 or greater
     *                                   than or equal to the number of child nodes
     * @implSpec The default implementation always throws
     * {@code IndexOutOfBoundsException}.
     */
    // 返回指定索引处的子Node
    default Node<T> getChild(int i) {
        throw new IndexOutOfBoundsException();
    }
    
    /**
     * Return a node describing a subsequence of the elements of this node,
     * starting at the given inclusive start offset and ending at the given exclusive end offset.
     *
     * @param from      The (inclusive) starting offset of elements to include, must
     *                  be in range 0..count().
     * @param to        The (exclusive) end offset of elements to include, must be
     *                  in range 0..count().
     * @param generator A function to be used to create a new array, if needed,
     *                  for reference nodes.
     *
     * @return the truncated node
     */
    // 从当前Node生成一个子Node返回
    default Node<T> truncate(long from, long to, IntFunction<T[]> generator) {
        if(from == 0 && to == count())
            return this;
        
        // 获取描述此Node中元素的Spliterator
        Spliterator<T> spliterator = spliterator();
        
        long size = to - from;
        
        // 生成FixedNodeBuilder/SpinedNodeBuilder
        Node.Builder<T> nodeBuilder = Nodes.builder(size, generator);
        nodeBuilder.begin(size);
        
        // 对容器中的所有元素执行一个空操作
        for(int i = 0; i < from && spliterator.tryAdvance(e -> { }); i++) {
        }
        
        // 需要对所有元素进行操作
        if(to == count()) {
            // 遍历Node内每个元素，在其上执行相应的择取操作
            spliterator.forEachRemaining(nodeBuilder);
        } else {    // 对部分元素执行择取操作
            for(int i = 0; i < size && spliterator.tryAdvance(nodeBuilder); i++) {
            }
        }
        nodeBuilder.end();
        
        // 返回FixedNodeBuilder/SpinedNodeBuilder
        return nodeBuilder.build();
    }
    
    /**
     * Gets the {@code StreamShape} associated with this {@code Node}.
     *
     * @return the stream shape associated with this node
     *
     * @implSpec The default in {@code Node} returns
     * {@code StreamShape.REFERENCE}
     */
    // 返回流的形状：引用类型
    default StreamShape getShape() {
        return StreamShape.REFERENCE;
    }
    
    
    
    /*▼ 专用Sink，这里称作：【Builder->Sink接口】 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * A mutable builder for a {@code Node} that implements {@link Sink}, which builds a flat node containing the elements that have been pushed to it.
     */
    // 专用Sink，用来创建各种类型的Node
    interface Builder<T> extends Sink<T> {
        
        /**
         * Builds the node.  Should be called after all elements have been
         * pushed and signalled with an invocation of {@link Sink#end()}.
         *
         * @return the resulting {@code Node}
         */
        // 构建Node
        Node<T> build();
        
        /**
         * Specialized {@code Node.Builder} for int elements
         */
        // 构建为int类型特化的【Builder->Sink接口】
        interface OfInt
            extends Node.Builder<Integer>, Sink.OfInt {
            // 构建Node
            @Override
            Node.OfInt build();
        }
        
        /**
         * Specialized {@code Node.Builder} for long elements
         */
        // 构建为long类型特化的【Builder->Sink接口】
        interface OfLong
            extends Node.Builder<Long>, Sink.OfLong {
            // 构建Node
            @Override
            Node.OfLong build();
        }
        
        /**
         * Specialized {@code Node.Builder} for double elements
         */
        // 构建为double类型特化的【Builder->Sink接口】
        interface OfDouble
            extends Node.Builder<Double>, Sink.OfDouble {
            // 构建Node
            @Override
            Node.OfDouble build();
        }
    }
    
    /*▲ 专用Sink，这里称作：【Builder->Sink接口】 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 为基本类型特化的【Node子接口】 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 为基本类型特化的【Node子接口】
    interface OfPrimitive<T, T_CONS, T_ARR, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_NODE extends OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE>>
        extends Node<T> {
        
        /**
         * {@inheritDoc}
         *
         * @return a {@link Spliterator.OfPrimitive} describing the elements of
         * this node
         */
        // 返回描述此Node中元素的Spliterator
        @Override
        T_SPLITR spliterator();
        
        /**
         * Traverses the elements of this node, and invoke the provided
         * {@code action} with each element.
         *
         * @param action a consumer that is to be invoked with each
         *               element in this {@code Node.OfPrimitive}
         */
        // 遍历Node中的元素，并在其上执行action操作
        @SuppressWarnings("overloads")
        void forEach(T_CONS action);
        
        /**
         * Copies the content of this {@code Node} into a primitive array,
         * starting at a given offset into the array.  It is the caller's
         * responsibility to ensure there is sufficient room in the array.
         *
         * @param array  the array into which to copy the contents of this
         *               {@code Node}
         * @param offset the starting offset within the array
         *
         * @throws IndexOutOfBoundsException if copying would cause access of
         *                                   data outside array bounds
         * @throws NullPointerException      if {@code array} is {@code null}
         */
        // 将Node的内容复制到数组array中
        void copyInto(T_ARR array, int offset);
        
        // 从当前Node生成一个子Node返回
        T_NODE truncate(long from, long to, IntFunction<T[]> generator);
        
        /**
         * Views this node as a primitive array.
         *
         * <p>Depending on the underlying implementation this may return a
         * reference to an internal array rather than a copy.  It is the callers
         * responsibility to decide if either this node or the array is utilized
         * as the primary reference for the data.</p>
         *
         * @return an array containing the contents of this {@code Node}
         */
        // 将Node中的元素存入基本类型数组后返回
        T_ARR asPrimitiveArray();
        
        /**
         * Creates a new primitive array.
         *
         * @param count the length of the primitive array.
         *
         * @return the new primitive array.
         */
        // 创建基本类型数组
        T_ARR newArray(int count);
        
        // 返回指定索引处的子Node
        @Override
        default T_NODE getChild(int i) {
            throw new IndexOutOfBoundsException();
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec the default implementation invokes the generator to create
         * an instance of a boxed primitive array with a length of
         * {@link #count()} and then invokes {@link #copyInto(T[], int)} with
         * that array at an offset of 0.
         */
        // 返回Node内部数据的数组视图
        @Override
        default T[] asArray(IntFunction<T[]> generator) {
            if(java.util.stream.Tripwire.ENABLED)
                java.util.stream.Tripwire.trip(getClass(), "{0} calling Node.OfPrimitive.asArray");
            
            long size = count();
            if(size >= Nodes.MAX_ARRAY_SIZE)
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            T[] boxed = generator.apply((int) count());
            copyInto(boxed, 0);
            return boxed;
        }
    }
    
    /**
     * Specialized {@code Node} for int elements
     */
    // 为int类型特化的【Node子接口】
    interface OfInt
        extends OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, OfInt> {
        
        /**
         * {@inheritDoc}
         *
         * @param consumer a {@code Consumer} that is to be invoked with each
         *                 element in this {@code Node}.  If this is an
         *                 {@code IntConsumer}, it is cast to {@code IntConsumer} so the
         *                 elements may be processed without boxing.
         */
        // 遍历Node中的元素，并在其上执行Consumer操作
        @Override
        default void forEach(Consumer<? super Integer> consumer) {
            if(consumer instanceof IntConsumer) {
                forEach((IntConsumer) consumer);
            } else {
                if(Tripwire.ENABLED)
                    Tripwire.trip(getClass(), "{0} calling Node.OfInt.forEachRemaining(Consumer)");
                spliterator().forEachRemaining(consumer);
            }
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec the default implementation invokes {@link #asPrimitiveArray()} to
         * obtain an int[] array then and copies the elements from that int[]
         * array into the boxed Integer[] array.  This is not efficient and it
         * is recommended to invoke {@link #copyInto(Object, int)}.
         */
        // 将Node的内容复制到数组boxed中
        @Override
        default void copyInto(Integer[] boxed, int offset) {
            if(Tripwire.ENABLED)
                Tripwire.trip(getClass(), "{0} calling Node.OfInt.copyInto(Integer[], int)");
            
            int[] array = asPrimitiveArray();
            for(int i = 0; i < array.length; i++) {
                boxed[offset + i] = array[i];
            }
        }
        
        // 从当前Node生成一个子Node返回
        @Override
        default Node.OfInt truncate(long from, long to, IntFunction<Integer[]> generator) {
            if(from == 0 && to == count())
                return this;
            long size = to - from;
            Spliterator.OfInt spliterator = spliterator();
            Node.Builder.OfInt nodeBuilder = Nodes.intBuilder(size);
            nodeBuilder.begin(size);
            for(int i = 0; i < from && spliterator.tryAdvance((IntConsumer) e -> { }); i++) {
            }
            if(to == count()) {
                spliterator.forEachRemaining((IntConsumer) nodeBuilder);
            } else {
                for(int i = 0; i < size && spliterator.tryAdvance((IntConsumer) nodeBuilder); i++) {
                }
            }
            nodeBuilder.end();
            return nodeBuilder.build();
        }
        
        // 创建int类型数组
        @Override
        default int[] newArray(int count) {
            return new int[count];
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec The default in {@code Node.OfInt} returns
         * {@code StreamShape.INT_VALUE}
         */
        // 返回流的形状：int类型
        default StreamShape getShape() {
            return StreamShape.INT_VALUE;
        }
    }
    
    /**
     * Specialized {@code Node} for long elements
     */
    // 为long类型特化的【Node子接口】
    interface OfLong
        extends OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, OfLong> {
        
        /**
         * {@inheritDoc}
         *
         * @param consumer A {@code Consumer} that is to be invoked with each
         *                 element in this {@code Node}.  If this is an
         *                 {@code LongConsumer}, it is cast to {@code LongConsumer} so
         *                 the elements may be processed without boxing.
         */
        // 遍历Node中的元素，并在其上执行Consumer操作
        @Override
        default void forEach(Consumer<? super Long> consumer) {
            if(consumer instanceof LongConsumer) {
                forEach((LongConsumer) consumer);
            } else {
                if(Tripwire.ENABLED)
                    Tripwire.trip(getClass(), "{0} calling Node.OfLong.forEachRemaining(Consumer)");
                spliterator().forEachRemaining(consumer);
            }
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec the default implementation invokes {@link #asPrimitiveArray()}
         * to obtain a long[] array then and copies the elements from that
         * long[] array into the boxed Long[] array.  This is not efficient and
         * it is recommended to invoke {@link #copyInto(Object, int)}.
         */
        // 将Node的内容复制到数组boxed中
        @Override
        default void copyInto(Long[] boxed, int offset) {
            if(Tripwire.ENABLED)
                Tripwire.trip(getClass(), "{0} calling Node.OfInt.copyInto(Long[], int)");
            
            long[] array = asPrimitiveArray();
            for(int i = 0; i < array.length; i++) {
                boxed[offset + i] = array[i];
            }
        }
        
        // 从当前Node生成一个子Node返回
        @Override
        default Node.OfLong truncate(long from, long to, IntFunction<Long[]> generator) {
            if(from == 0 && to == count())
                return this;
            long size = to - from;
            Spliterator.OfLong spliterator = spliterator();
            Node.Builder.OfLong nodeBuilder = Nodes.longBuilder(size);
            nodeBuilder.begin(size);
            for(int i = 0; i < from && spliterator.tryAdvance((LongConsumer) e -> { }); i++) {
            }
            if(to == count()) {
                spliterator.forEachRemaining((LongConsumer) nodeBuilder);
            } else {
                for(int i = 0; i < size && spliterator.tryAdvance((LongConsumer) nodeBuilder); i++) {
                }
            }
            nodeBuilder.end();
            return nodeBuilder.build();
        }
        
        // 创建long类型数组
        @Override
        default long[] newArray(int count) {
            return new long[count];
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec The default in {@code Node.OfLong} returns
         * {@code StreamShape.LONG_VALUE}
         */
        // 返回流的形状：long类型
        default StreamShape getShape() {
            return StreamShape.LONG_VALUE;
        }
    }
    
    /**
     * Specialized {@code Node} for double elements
     */
    // 为double类型特化的【Node子接口】
    interface OfDouble
        extends OfPrimitive<Double, DoubleConsumer, double[], Spliterator.OfDouble, OfDouble> {
        
        /**
         * {@inheritDoc}
         *
         * @param consumer A {@code Consumer} that is to be invoked with each
         *                 element in this {@code Node}.  If this is an
         *                 {@code DoubleConsumer}, it is cast to {@code DoubleConsumer}
         *                 so the elements may be processed without boxing.
         */
        // 遍历Node中的元素，并在其上执行Consumer操作
        @Override
        default void forEach(Consumer<? super Double> consumer) {
            if(consumer instanceof DoubleConsumer) {
                forEach((DoubleConsumer) consumer);
            } else {
                if(Tripwire.ENABLED)
                    Tripwire.trip(getClass(), "{0} calling Node.OfLong.forEachRemaining(Consumer)");
                spliterator().forEachRemaining(consumer);
            }
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec the default implementation invokes {@link #asPrimitiveArray()}
         * to obtain a double[] array then and copies the elements from that
         * double[] array into the boxed Double[] array.  This is not efficient
         * and it is recommended to invoke {@link #copyInto(Object, int)}.
         */
        // 将Node的内容复制到数组boxed中
        @Override
        default void copyInto(Double[] boxed, int offset) {
            if(Tripwire.ENABLED)
                Tripwire.trip(getClass(), "{0} calling Node.OfDouble.copyInto(Double[], int)");
            
            double[] array = asPrimitiveArray();
            for(int i = 0; i < array.length; i++) {
                boxed[offset + i] = array[i];
            }
        }
        
        // 从当前Node生成一个子Node返回
        @Override
        default Node.OfDouble truncate(long from, long to, IntFunction<Double[]> generator) {
            if(from == 0 && to == count())
                return this;
            long size = to - from;
            Spliterator.OfDouble spliterator = spliterator();
            Node.Builder.OfDouble nodeBuilder = Nodes.doubleBuilder(size);
            nodeBuilder.begin(size);
            for(int i = 0; i < from && spliterator.tryAdvance((DoubleConsumer) e -> { }); i++) {
            }
            if(to == count()) {
                spliterator.forEachRemaining((DoubleConsumer) nodeBuilder);
            } else {
                for(int i = 0; i < size && spliterator.tryAdvance((DoubleConsumer) nodeBuilder); i++) {
                }
            }
            nodeBuilder.end();
            return nodeBuilder.build();
        }
        
        // 创建double类型数组
        @Override
        default double[] newArray(int count) {
            return new double[count];
        }
        
        /**
         * {@inheritDoc}
         *
         * @implSpec The default in {@code Node.OfDouble} returns
         * {@code StreamShape.DOUBLE_VALUE}
         */
        // 返回流的形状：double类型
        default StreamShape getShape() {
            return StreamShape.DOUBLE_VALUE;
        }
    }
    
    /*▲ 为基本类型特化的【Node子接口】 ████████████████████████████████████████████████████████████████████████████████┛ */
}
