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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CountedCompleter;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;

/**
 * Factory methods for constructing implementations of {@link Node} and
 * {@link Node.Builder} and their primitive specializations.  Fork/Join tasks
 * for collecting output from a {@link PipelineHelper} to a {@link Node} and
 * flattening {@link Node}s.
 *
 * @since 1.8
 */
/*
 * Node工厂，提供一些常用的Node的实现
 *
 * 主要包含以下6类Node：
 * [1] "空"Node
 * [2] 普通"数组"Node
 * [3] 增强"数组"Node
 * [4] "弹性缓冲区"Node
 * [5] Collection-Node
 * [6] "树状"Node
 *
 * 还提供了一些辅助类，包括：
 * [1] "树状"Node的Spliterator
 * [2] "并行复制"任务
 * [3] 线性"并行择取"任务
 * [4] 树状"并行择取"任务
 *
 * 在方法层面，除了提供构造各类Node的工厂方法，还提供了flatten操作和collect操作的实现。
 */
final class Nodes {
    
    /**
     * The maximum size of an array that can be allocated.
     */
    static final long MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    
    // IllegalArgumentException messages
    static final String BAD_SIZE = "Stream size exceeds max array size";
    
    
    private Nodes() {
        throw new Error("no instances");
    }
    
    
    
    /*▼ (1) 构造"空"Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    @SuppressWarnings("rawtypes")
    private static final Node EMPTY_NODE = new EmptyNode.OfRef();
    private static final Node.OfInt EMPTY_INT_NODE = new EmptyNode.OfInt();
    private static final Node.OfLong EMPTY_LONG_NODE = new EmptyNode.OfLong();
    private static final Node.OfDouble EMPTY_DOUBLE_NODE = new EmptyNode.OfDouble();
    
    
    /**
     * Produces an empty node whose count is zero, has no children and no content.
     *
     * @param <T> the type of elements of the created node
     * @param shape the shape of the node to be created
     * @return an empty node.
     */
    // 构造"空"Node
    @SuppressWarnings("unchecked")
    static <T> Node<T> emptyNode(StreamShape shape) {
        switch (shape) {
            case REFERENCE:    return (Node<T>) EMPTY_NODE;
            case INT_VALUE:    return (Node<T>) EMPTY_INT_NODE;
            case LONG_VALUE:   return (Node<T>) EMPTY_LONG_NODE;
            case DOUBLE_VALUE: return (Node<T>) EMPTY_DOUBLE_NODE;
            default:
                throw new IllegalStateException("Unknown shape " + shape);
        }
    }
    
    /*▲ (1) 构造"空"Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (2) 构造普通"数组"Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Produces a {@link Node} describing an array.
     *
     * <p>The node will hold a reference to the array and will not make a copy.
     *
     * @param <T> the type of elements held by the node
     * @param array the array
     * @return a node holding an array
     */
    // 构造普通"数组"Node(引用类型版本)
    static <T> Node<T> node(T[] array) {
        return new ArrayNode<>(array);
    }
    
    /**
     * Produces a {@link Node.OfInt} describing an int[] array.
     *
     * <p>The node will hold a reference to the array and will not make a copy.
     *
     * @param array the array
     * @return a node holding an array
     */
    // 构造普通"数组"Node(int类型版本)
    static Node.OfInt node(int[] array) {
        return new IntArrayNode(array);
    }
    
    /**
     * Produces a {@link Node.OfLong} describing a long[] array.
     * <p>
     * The node will hold a reference to the array and will not make a copy.
     *
     * @param array the array
     * @return a node holding an array
     */
    // 构造普通"数组"Node(long类型版本)
    static Node.OfLong node(final long[] array) {
        return new LongArrayNode(array);
    }
    
    /**
     * Produces a {@link Node.OfDouble} describing a double[] array.
     *
     * <p>The node will hold a reference to the array and will not make a copy.
     *
     * @param array the array
     * @return a node holding an array
     */
    // 构造普通"数组"Node(double类型版本)
    static Node.OfDouble node(final double[] array) {
        return new DoubleArrayNode(array);
    }
    
    /*▲ (2) 构造普通"数组"Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (3)(4) 构造增强"数组"Node或"弹性缓冲区"Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 无论是增强"数组"Node还是"弹性缓冲区"Node，本质都是用数组存储元素的
     */
    
    /**
     * Produces a {@link Node.Builder}.
     *
     * @param exactSizeIfKnown -1 if a variable size builder is requested,
     * otherwise the exact capacity desired.  A fixed capacity builder will
     * fail if the wrong number of elements are added to the builder.
     * @param generator the array factory
     * @param <T> the type of elements of the node builder
     * @return a {@code Node.Builder}
     */
    // 构造增强"数组"Node或"弹性缓冲区"Node(引用类型版本)
    static <T> Node.Builder<T> builder(long exactSizeIfKnown, IntFunction<T[]> generator) {
        // 长度已知且固定
        if(exactSizeIfKnown >= 0 && exactSizeIfKnown<MAX_ARRAY_SIZE) {
            // 构造增强"数组"Node(引用类型版本)
            return new FixedNodeBuilder<>(exactSizeIfKnown, generator);
        }
    
        // 构造"弹性缓冲区"Node(引用类型版本)
        return builder();
    }
    
    /**
     * Produces a {@link Node.Builder.OfInt}.
     *
     * @param exactSizeIfKnown -1 if a variable size builder is requested,
     * otherwise the exact capacity desired.  A fixed capacity builder will
     * fail if the wrong number of elements are added to the builder.
     * @return a {@code Node.Builder.OfInt}
     */
    // 构造增强"数组"Node或"弹性缓冲区"Node(int类型版本)
    static Node.Builder.OfInt intBuilder(long exactSizeIfKnown) {
        // 长度已知且固定
        if(exactSizeIfKnown >= 0 && exactSizeIfKnown<MAX_ARRAY_SIZE) {
            // 构造增强"数组"Node(int类型版本)
            return new IntFixedNodeBuilder(exactSizeIfKnown);
        }
    
        // 构造"弹性缓冲区"Node(int类型版本)
        return intBuilder();
    }
    
    /**
     * Produces a {@link Node.Builder.OfLong}.
     *
     * @param exactSizeIfKnown -1 if a variable size builder is requested,
     * otherwise the exact capacity desired.  A fixed capacity builder will
     * fail if the wrong number of elements are added to the builder.
     * @return a {@code Node.Builder.OfLong}
     */
    // 构造增强"数组"Node或"弹性缓冲区"Node(long类型版本)
    static Node.Builder.OfLong longBuilder(long exactSizeIfKnown) {
        // 长度已知且固定
        if(exactSizeIfKnown >= 0 && exactSizeIfKnown<MAX_ARRAY_SIZE) {
            // 构造增强"数组"Node(long类型版本)
            return new LongFixedNodeBuilder(exactSizeIfKnown);
        }
    
        // 构造"弹性缓冲区"Node(long类型版本)
        return longBuilder();
    }
    
    /**
     * Produces a {@link Node.Builder.OfDouble}.
     *
     * @param exactSizeIfKnown -1 if a variable size builder is requested,
     *                         otherwise the exact capacity desired.  A fixed capacity builder will
     *                         fail if the wrong number of elements are added to the builder.
     *
     * @return a {@code Node.Builder.OfDouble}
     */
    // 构造增强"数组"Node或"弹性缓冲区"Node(double类型版本)
    static Node.Builder.OfDouble doubleBuilder(long exactSizeIfKnown) {
        // 长度已知且固定
        if(exactSizeIfKnown >= 0 && exactSizeIfKnown<MAX_ARRAY_SIZE) {
            // 构造增强"数组"Node(double类型版本)
            return new DoubleFixedNodeBuilder(exactSizeIfKnown);
        }
        
        // 构造"弹性缓冲区"Node(double类型版本)
        return doubleBuilder();
    }
    
    
    /**
     * Produces a variable size {@link Node.Builder}.
     *
     * @param <T> the type of elements of the node builder
     *
     * @return a {@code Node.Builder}
     */
    // 构造"弹性缓冲区"Node(引用类型版本)
    static <T> Node.Builder<T> builder() {
        return new SpinedNodeBuilder<>();
    }
    
    /**
     * Produces a variable size @{link Node.Builder.OfInt}.
     *
     * @return a {@code Node.Builder.OfInt}
     */
    // 构造"弹性缓冲区"Node(int类型版本)
    static Node.Builder.OfInt intBuilder() {
        return new IntSpinedNodeBuilder();
    }
    
    /**
     * Produces a variable size @{link Node.Builder.OfLong}.
     *
     * @return a {@code Node.Builder.OfLong}
     */
    // 构造"弹性缓冲区"Node(long类型版本)
    static Node.Builder.OfLong longBuilder() {
        return new LongSpinedNodeBuilder();
    }
    
    /**
     * Produces a variable size @{link Node.Builder.OfDouble}.
     *
     * @return a {@code Node.Builder.OfDouble}
     */
    // 构造"弹性缓冲区"Node(double类型版本)
    static Node.Builder.OfDouble doubleBuilder() {
        return new DoubleSpinedNodeBuilder();
    }
    
    /*▲ (3)(4) 构造增强"数组"Node或"弹性缓冲区"Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (5) 构造Collection-Node(引用类型版本) ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Produces a {@link Node} describing a {@link Collection}.
     * <p>
     * The node will hold a reference to the collection and will not make a copy.
     *
     * @param <T>        the type of elements held by the node
     * @param collection the collection
     *
     * @return a node holding a collection
     */
    // 构造Collection-Node(引用类型版本)
    static <T> Node<T> node(Collection<T> collection) {
        return new CollectionNode<>(collection);
    }
    
    /*▲ (5) 构造Collection-Node(引用类型版本) ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ (6) 构造"树状"Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Produces a concatenated {@link Node} that has two or more children.
     * <p>The count of the concatenated node is equal to the sum of the count
     * of each child. Traversal of the concatenated node traverses the content
     * of each child in encounter order of the list of children. Splitting a
     * spliterator obtained from the concatenated node preserves the encounter
     * order of the list of children.
     *
     * <p>The result may be a concatenated node, the input sole node if the size
     * of the list is 1, or an empty node.
     *
     * @param <T> the type of elements of the concatenated node
     * @param shape the shape of the concatenated node to be created
     * @param left the left input node
     * @param right the right input node
     * @return a {@code Node} covering the elements of the input nodes
     * @throws IllegalStateException if all {@link Node} elements of the list
     * are an not instance of type supported by this factory.
     */
    // 构造"树状"Node：将两个Node按树形链接起来，并返回链接后的Node
    @SuppressWarnings("unchecked")
    static <T> Node<T> conc(StreamShape shape, Node<T> left, Node<T> right) {
        switch (shape) {
            case REFERENCE:
                return new ConcNode<>(left, right);
            case INT_VALUE:
                return (Node<T>) new ConcNode.OfInt((Node.OfInt) left, (Node.OfInt) right);
            case LONG_VALUE:
                return (Node<T>) new ConcNode.OfLong((Node.OfLong) left, (Node.OfLong) right);
            case DOUBLE_VALUE:
                return (Node<T>) new ConcNode.OfDouble((Node.OfDouble) left, (Node.OfDouble) right);
            default:
                throw new IllegalStateException("Unknown shape " + shape);
        }
    }
    
    /*▲ (6) 构造"树状"Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ flatten操作：将树状Node转换为普通"数组"Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Flatten, in parallel, a {@link Node}.  A flattened node is one that has no children.
     * If the node is already flat, it is simply returned.
     *
     * @param <T>       type of elements contained by the node
     * @param node      the node to flatten
     * @param generator the array factory used to create array instances
     *
     * @return a flat {@code Node}
     *
     * @implSpec If a new node is to be created, the generator is used to create an array
     * whose length is {@link Node#count()}.  Then the node tree is traversed
     * and leaf node elements are placed in the array concurrently by leaf tasks
     * at the correct offsets.
     */
    /*
     * 尝试将树状Node转换为普通"数组"Node；如果node不是树状node，那直接就返回了。
     * 将树状Node中的元素并行地复制到generator生成的数组中，然后再将该数组封装成普通"数组"Node后返回(引用类型版本)。
     */
    public static <T> Node<T> flatten(Node<T> node, IntFunction<T[]> generator) {
        // 如果给定的node已经没有子Node，则直接返回
        if(node.getChildCount()<=0) {
            return node;
        }
        
        long size = node.count();
        if(size >= MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        
        // 生成指定类型的数组以存储node中的数据
        T[] array = generator.apply((int) size);
        
        // 构造"并行复制"任务：将树状node中的元素并行地复制到数组array中
        ToArrayTask.OfRef<T> task = new ToArrayTask.OfRef<>(node, array, 0);
        
        // 提交"并行复制"任务到线程池
        task.invoke();
        
        // 由给定的数组构造普通"数组"Node(引用类型版本)
        return node(array);
    }
    
    /**
     * Flatten, in parallel, a {@link Node.OfInt}.  A flattened node is one that
     * has no children.  If the node is already flat, it is simply returned.
     *
     * @implSpec
     * If a new node is to be created, a new int[] array is created whose length
     * is {@link Node#count()}.  Then the node tree is traversed and leaf node
     * elements are placed in the array concurrently by leaf tasks at the
     * correct offsets.
     *
     * @param node the node to flatten
     * @return a flat {@code Node.OfInt}
     */
    /*
     * 尝试将树状Node转换为普通"数组"Node；如果node不是树状node，那直接就返回了。
     * 将树状Node中的元素并行地复制到int数组中，然后再将该数组封装成普通"数组"Node后返回(int类型版本)。
     */
    public static Node.OfInt flattenInt(Node.OfInt node) {
        // 如果给定的node已经没有子Node，则直接返回
        if(node.getChildCount()<=0) {
            return node;
        }
    
        long size = node.count();
        if(size >= MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
    
        // 生成int类型的数组以存储node中的数据
        int[] array = new int[(int) size];
    
        // 构造"并行复制"任务：将树状node中的元素并行地复制到数组array中
        ToArrayTask.OfInt task = new ToArrayTask.OfInt(node, array, 0);
    
        // 提交"并行复制"任务到线程池
        task.invoke();
    
        // 由给定的数组构造普通"数组"Node(引用类型版本)
        return node(array);
    }
    
    /**
     * Flatten, in parallel, a {@link Node.OfLong}.  A flattened node is one that
     * has no children.  If the node is already flat, it is simply returned.
     *
     * @implSpec
     * If a new node is to be created, a new long[] array is created whose length
     * is {@link Node#count()}.  Then the node tree is traversed and leaf node
     * elements are placed in the array concurrently by leaf tasks at the
     * correct offsets.
     *
     * @param node the node to flatten
     * @return a flat {@code Node.OfLong}
     */
    /*
     * 尝试将树状Node转换为普通"数组"Node；如果node不是树状node，那直接就返回了。
     * 将树状Node中的元素并行地复制到long数组中，然后再将该数组封装成普通"数组"Node后返回(long类型版本)。
     */
    public static Node.OfLong flattenLong(Node.OfLong node) {
        // 如果给定的node已经没有子Node，则直接返回
        if(node.getChildCount()<=0) {
            return node;
        }
    
        long size = node.count();
        if(size >= MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
    
        // 生成long类型的数组以存储node中的数据
        long[] array = new long[(int) size];
    
        // 构造"并行复制"任务：将树状node中的元素并行地复制到数组array中
        ToArrayTask.OfLong task = new ToArrayTask.OfLong(node, array, 0);
    
        // 提交"并行复制"任务到线程池
        task.invoke();
    
        // 由给定的数组构造普通"数组"Node(引用类型版本)
        return node(array);
    
    }
    
    /**
     * Flatten, in parallel, a {@link Node.OfDouble}.  A flattened node is one that
     * has no children.  If the node is already flat, it is simply returned.
     *
     * @implSpec
     * If a new node is to be created, a new double[] array is created whose length
     * is {@link Node#count()}.  Then the node tree is traversed and leaf node
     * elements are placed in the array concurrently by leaf tasks at the
     * correct offsets.
     *
     * @param node the node to flatten
     * @return a flat {@code Node.OfDouble}
     */
    /*
     * 尝试将树状Node转换为普通"数组"Node；如果node不是树状node，那直接就返回了。
     * 将树状Node中的元素并行地复制到double数组中，然后再将该数组封装成普通"数组"Node后返回(double类型版本)。
     */
    public static Node.OfDouble flattenDouble(Node.OfDouble node) {
        // 如果给定的node已经没有子Node，则直接返回
        if(node.getChildCount()<=0) {
            return node;
        }
    
        long size = node.count();
        if(size >= MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
    
        // 生成double类型的数组以存储node中的数据
        double[] array = new double[(int) size];
    
        // 构造"并行复制"任务：将树状node中的元素并行地复制到数组array中
        ToArrayTask.OfDouble task = new ToArrayTask.OfDouble(node, array, 0);
    
        // 提交"并行复制"任务到线程池
        task.invoke();
    
        // 由给定的数组构造普通"数组"Node(引用类型版本)
        return node(array);
    }
    
    /*▲ flatten操作：将树状Node转换为普通"数组"Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ collect操作：并行择取/筛选元素 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Collect, in parallel, elements output from a pipeline and describe those elements with a {@link Node}.
     *
     * @param helper      the pipeline helper describing the pipeline
     * @param flattenTree whether a conc node should be flattened into a node
     *                    describing an array before returning
     * @param generator   the array generator
     *
     * @return a {@link Node} describing the output elements
     *
     * @implSpec If the exact size of the output from the pipeline is known and the source
     * {@link Spliterator} has the {@link Spliterator#SUBSIZED} characteristic,
     * then a flat {@link Node} will be returned whose content is an array,
     * since the size is known the array can be constructed in advance and
     * output elements can be placed into the array concurrently by leaf
     * tasks at the correct offsets.  If the exact size is not known, output
     * elements are collected into a conc-node whose shape mirrors that
     * of the computation. This conc-node can then be flattened in
     * parallel to produce a flat {@code Node} if desired.
     */
    /*
     * 并行搜集元素，中间依然会经过sink的择取操作(引用类型版本)。
     * 将spliterator中的元素并行地收集到generator生成的数组中，然后将该数组封装到Node中返回。
     */
    public static <P_IN, P_OUT> Node<P_OUT> collect(PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<P_OUT[]> generator) {
        
        /*
         * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
         * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         *
         * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
         */
        long sizeIfKnown = helper.exactOutputSizeIfKnown(spliterator);
        
        // 如果可以获取到一个精确的元素量
        if(sizeIfKnown >= 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            if(sizeIfKnown >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            
            // 生成指定容量的数组
            P_OUT[] array = generator.apply((int) sizeIfKnown);
            
            // 构造线性"并行择取"任务：将spliterator中的元素并行地择取/筛选到指定的数组中
            SizedCollectorTask.OfRef<P_IN, P_OUT> task = new SizedCollectorTask.OfRef<>(spliterator, helper, array);
            
            // 提交"并行复制"任务到线程池
            task.invoke();
            
            // 由给定的数组构造普通"数组"Node(引用类型版本)
            return node(array);
            
            
            // 如果spliterator中的数据量未知，则需要使用树状"并行择取"任务
        } else {
            // 构造树状"并行择取"任务：将spliterator中的元素并行地择取/筛选到指定的Node(数组)中
            CollectorTask.OfRef<P_IN, P_OUT> task = new CollectorTask.OfRef<>(helper, generator, spliterator);
            
            // 提交树状"并行择取"任务到线程池，并将任务执行结果存入Node后返回
            Node<P_OUT> node = task.invoke();
            
            // 返回包含计算结果的Node（视需求将Node降维）
            if(flattenTree) {
                // 将树状Node中的元素并行地复制到generator生成的数组中，然后再将该数组封装成普通"数组"Node后返回(引用类型版本)
                return flatten(node, generator);
            }
            
            return node;
        }
    }
    
    /**
     * Collect, in parallel, elements output from an int-valued pipeline and
     * describe those elements with a {@link Node.OfInt}.
     *
     * @param <P_IN>      the type of elements from the source Spliterator
     * @param helper      the pipeline helper describing the pipeline
     * @param flattenTree whether a conc node should be flattened into a node
     *                    describing an array before returning
     *
     * @return a {@link Node.OfInt} describing the output elements
     *
     * @implSpec If the exact size of the output from the pipeline is known and the source
     * {@link Spliterator} has the {@link Spliterator#SUBSIZED} characteristic,
     * then a flat {@link Node} will be returned whose content is an array,
     * since the size is known the array can be constructed in advance and
     * output elements can be placed into the array concurrently by leaf
     * tasks at the correct offsets.  If the exact size is not known, output
     * elements are collected into a conc-node whose shape mirrors that
     * of the computation. This conc-node can then be flattened in
     * parallel to produce a flat {@code Node.OfInt} if desired.
     */
    /*
     * 并行搜集元素，中间依然会经过sink的择取操作(int类型版本)。
     * 将spliterator中的元素并行地收集到int数组中，然后将该数组封装到Node中返回。
     */
    public static <P_IN> Node.OfInt collectInt(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator, boolean flattenTree) {
    
        /*
         * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
         * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         *
         * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
         */
        long sizeIfKnown = helper.exactOutputSizeIfKnown(spliterator);
    
        // 处理元素总量一定，但是子结点数量不确定的Node
        if(sizeIfKnown >= 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            if(sizeIfKnown >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
        
            int[] array = new int[(int) sizeIfKnown];
        
            // 构造线性"并行择取"任务：将spliterator中的元素并行地择取/筛选到指定的数组中
            SizedCollectorTask.OfInt<P_IN> task = new SizedCollectorTask.OfInt<>(spliterator, helper, array);
        
            // 提交"并行复制"任务到线程池
            task.invoke();
        
            // 由给定的数组构造普通"数组"Node(引用类型版本)
            return node(array);
        
        
            // 如果spliterator中的数据量未知，则需要使用树状"并行择取"任务
        } else {
            // 构造树状"并行择取"任务：将spliterator中的元素并行地择取/筛选到指定的Node(数组)中
            CollectorTask.OfInt<P_IN> task = new CollectorTask.OfInt<>(helper, spliterator);
        
            // 提交树状"并行择取"任务到线程池，并将任务执行结果存入Node后返回
            Node.OfInt node = task.invoke();
        
            /*
             * 如果返回的node是树状node，且要求对返回的node进行降维，
             * 则这里需要将树状node中的内容复制到非树状node后返回。
             */
            if(flattenTree) {
                return flattenInt(node);
            }
    
            return node;
        }
    }
    
    /**
     * Collect, in parallel, elements output from a long-valued pipeline and
     * describe those elements with a {@link Node.OfLong}.
     *
     * @param <P_IN>      the type of elements from the source Spliterator
     * @param helper      the pipeline helper describing the pipeline
     * @param flattenTree whether a conc node should be flattened into a node
     *                    describing an array before returning
     *
     * @return a {@link Node.OfLong} describing the output elements
     *
     * @implSpec If the exact size of the output from the pipeline is known and the source
     * {@link Spliterator} has the {@link Spliterator#SUBSIZED} characteristic,
     * then a flat {@link Node} will be returned whose content is an array,
     * since the size is known the array can be constructed in advance and
     * output elements can be placed into the array concurrently by leaf
     * tasks at the correct offsets.  If the exact size is not known, output
     * elements are collected into a conc-node whose shape mirrors that
     * of the computation. This conc-node can then be flattened in
     * parallel to produce a flat {@code Node.OfLong} if desired.
     */
    /*
     * 并行搜集元素，中间依然会经过sink的择取操作(long类型版本)。
     * 将spliterator中的元素并行地收集到long数组中，然后将该数组封装到Node中返回。
     */
    public static <P_IN> Node.OfLong collectLong(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator, boolean flattenTree) {
    
        /*
         * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
         * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         *
         * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
         */
        long sizeIfKnown = helper.exactOutputSizeIfKnown(spliterator);
    
        // 处理元素总量一定，但是子结点数量不确定的Node
        if(sizeIfKnown >= 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            if(sizeIfKnown >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
        
            long[] array = new long[(int) sizeIfKnown];
        
            // 构造线性"并行择取"任务：将spliterator中的元素并行地择取/筛选到指定的数组中
            SizedCollectorTask.OfLong<P_IN> task = new SizedCollectorTask.OfLong<>(spliterator, helper, array);
        
            // 提交"并行复制"任务到线程池
            task.invoke();
        
            // 由给定的数组构造普通"数组"Node(引用类型版本)
            return node(array);
        
        
            // 如果spliterator中的数据量未知，则需要使用树状"并行择取"任务
        } else {
            // 构造树状"并行择取"任务：将spliterator中的元素并行地择取/筛选到指定的Node(数组)中
            CollectorTask.OfLong<P_IN> task = new CollectorTask.OfLong<>(helper, spliterator);
        
            // 提交树状"并行择取"任务到线程池，并将任务执行结果存入Node后返回
            Node.OfLong node = task.invoke();
        
            /*
             * 如果返回的node是树状node，且要求对返回的node进行降维，
             * 则这里需要将树状node中的内容复制到非树状node后返回。
             */
            if(flattenTree) {
                return flattenLong(node);
            }
            
            return node;
        }
    }
    
    /**
     * Collect, in parallel, elements output from n double-valued pipeline and
     * describe those elements with a {@link Node.OfDouble}.
     *
     * @param <P_IN>      the type of elements from the source Spliterator
     * @param helper      the pipeline helper describing the pipeline
     * @param flattenTree whether a conc node should be flattened into a node
     *                    describing an array before returning
     *
     * @return a {@link Node.OfDouble} describing the output elements
     *
     * @implSpec If the exact size of the output from the pipeline is known and the source
     * {@link Spliterator} has the {@link Spliterator#SUBSIZED} characteristic,
     * then a flat {@link Node} will be returned whose content is an array,
     * since the size is known the array can be constructed in advance and
     * output elements can be placed into the array concurrently by leaf
     * tasks at the correct offsets.  If the exact size is not known, output
     * elements are collected into a conc-node whose shape mirrors that
     * of the computation. This conc-node can then be flattened in
     * parallel to produce a flat {@code Node.OfDouble} if desired.
     */
    /*
     * 并行搜集元素，中间依然会经过sink的择取操作(double类型版本)。
     * 将spliterator中的元素并行地收集到double数组中，然后将该数组封装到Node中返回。
     */
    public static <P_IN> Node.OfDouble collectDouble(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator, boolean flattenTree) {
    
        /*
         * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
         * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         *
         * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
         */
        long sizeIfKnown = helper.exactOutputSizeIfKnown(spliterator);
    
        // 处理元素总量一定，但是子结点数量不确定的Node
        if(sizeIfKnown >= 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            if(sizeIfKnown >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
        
            double[] array = new double[(int) sizeIfKnown];
        
            // 构造线性"并行择取"任务：将spliterator中的元素并行地择取/筛选到指定的数组中
            SizedCollectorTask.OfDouble<P_IN> task = new SizedCollectorTask.OfDouble<>(spliterator, helper, array);
        
            // 提交"并行复制"任务到线程池
            task.invoke();
        
            // 由给定的数组构造普通"数组"Node(引用类型版本)
            return node(array);
        
        
            // 如果spliterator中的数据量未知，则需要使用树状"并行择取"任务
        } else {
            // 构造树状"并行择取"任务：将spliterator中的元素并行地择取/筛选到指定的Node(数组)中
            CollectorTask.OfDouble<P_IN> task = new CollectorTask.OfDouble<>(helper, spliterator);
        
            // 提交树状"并行择取"任务到线程池，并将任务执行结果存入Node后返回
            Node.OfDouble node = task.invoke();
        
            /*
             * 如果返回的node是树状node，且要求对返回的node进行降维，
             * 则这里需要将树状node中的内容复制到非树状node后返回。
             */
            if(flattenTree) {
                return flattenDouble(node);
            }
        
            return node;
        }
    }
    
    /*▲ collect操作：并行择取/筛选元素 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * @return an array generator for an array whose elements are of type T.
     */
    // 返回用于创建T类型数组的函数表达式
    @SuppressWarnings("unchecked")
    static <T> IntFunction<T[]> castingArray() {
        return size -> (T[]) new Object[size];
    }
    
    
    
    
    
    
    
    
    
    
    
    
    /*▼ [1] "空"Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * "空"Node中不包含任何有效数据
     */
    
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    
    // "空"Node的抽象实现
    private abstract static class EmptyNode<T, T_ARR, T_CONS> implements Node<T> {
        
        EmptyNode() {
        }
        
        public void forEach(T_CONS consumer) {
        }
        
        @Override
        public T[] asArray(IntFunction<T[]> generator) {
            return generator.apply(0);
        }
        
        public void copyInto(T_ARR array, int offset) {
        }
        
        @Override
        public long count() {
            return 0;
        }
        
        
        // "空"Node(引用类型版本)
        private static class OfRef<T> extends EmptyNode<T, T[], Consumer<? super T>> {
            private OfRef() {
                super();
            }
            
            @Override
            public Spliterator<T> spliterator() {
                return Spliterators.emptySpliterator();
            }
        }
        
        // "空"Node(int类型版本)
        private static final class OfInt extends EmptyNode<Integer, int[], IntConsumer> implements Node.OfInt {
            
            OfInt() {
            }
            
            @Override
            public Spliterator.OfInt spliterator() {
                return Spliterators.emptyIntSpliterator();
            }
            
            // 返回长度为0的int数组
            @Override
            public int[] asPrimitiveArray() {
                return EMPTY_INT_ARRAY;
            }
        }
        
        // "空"Node(long类型版本)
        private static final class OfLong extends EmptyNode<Long, long[], LongConsumer> implements Node.OfLong {
            
            OfLong() {
            }
            
            @Override
            public Spliterator.OfLong spliterator() {
                return Spliterators.emptyLongSpliterator();
            }
            
            // 返回长度为0的long数组
            @Override
            public long[] asPrimitiveArray() {
                return EMPTY_LONG_ARRAY;
            }
        }
        
        // "空"Node(double类型版本)
        private static final class OfDouble extends EmptyNode<Double, double[], DoubleConsumer> implements Node.OfDouble {
            
            OfDouble() {
            }
            
            @Override
            public Spliterator.OfDouble spliterator() {
                return Spliterators.emptyDoubleSpliterator();
            }
            
            // 返回长度为0的double数组
            @Override
            public double[] asPrimitiveArray() {
                return EMPTY_DOUBLE_ARRAY;
            }
        }
    }
    
    /*▲ [1] "空"Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ [2] 普通"数组"Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 普通"数组"Node中的有效数据被存储在一个【定长】数组中
     */
    
    /** Node class for a reference array */
    // 普通"数组"Node(引用类型版本)
    private static class ArrayNode<T> implements Node<T> {
        final T[] array;
        int curSize;    // 数组元素数量
        
        // 从已有的数组新建ArrayNode
        ArrayNode(T[] array) {
            this.array = array;
            this.curSize = array.length;
        }
        
        // 新建ArrayNode，内部包含一个长度为size的空数组
        @SuppressWarnings("unchecked")
        ArrayNode(long size, IntFunction<T[]> generator) {
            if(size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            
            this.array = generator.apply((int) size);
            
            this.curSize = 0;
        }
        
        // 返回当前Node的流迭代器
        @Override
        public Spliterator<T> spliterator() {
            return Arrays.spliterator(array, 0, curSize);
        }
        
        // 遍历ArrayNode中的元素，并在其上执行Consumer操作
        @Override
        public void forEach(Consumer<? super T> consumer) {
            for(int i = 0; i<curSize; i++) {
                consumer.accept(array[i]);
            }
        }
        
        // 返回ArrayNode内部数据的数组视图（这里直接返回内部数组）
        @Override
        public T[] asArray(IntFunction<T[]> generator) {
            if(array.length == curSize) {
                return array;
            } else {
                throw new IllegalStateException();
            }
        }
        
        // 将ArrayNode的内容复制到数组dest中（这里直接进行数组拷贝）
        @Override
        public void copyInto(T[] dest, int destOffset) {
            System.arraycopy(array, 0, dest, destOffset, curSize);
        }
        
        // 返回ArrayNode中包含的元素数量（这里直接返回数组中元素的个数）
        @Override
        public long count() {
            return curSize;
        }
        
        @Override
        public String toString() {
            return String.format("ArrayNode[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 普通"数组"Node(int类型版本)
    private static class IntArrayNode implements Node.OfInt {
        final int[] array;
        int curSize;
        
        // 从已有的数组新建Node
        IntArrayNode(int[] array) {
            this.array = array;
            this.curSize = array.length;
        }
        
        // 新建Node，内部包含一个长度为size的空数组
        IntArrayNode(long size) {
            if(size >= MAX_ARRAY_SIZE)
                throw new IllegalArgumentException(BAD_SIZE);
            this.array = new int[(int) size];
            this.curSize = 0;
        }
        
        // 返回描述此Node中元素的Spliterator（这里是IntArraySpliterator）
        @Override
        public Spliterator.OfInt spliterator() {
            return Arrays.spliterator(array, 0, curSize);
        }
        
        // 遍历Node中的元素，并在其上执行consumer操作
        @Override
        public void forEach(IntConsumer consumer) {
            for(int i = 0; i<curSize; i++) {
                consumer.accept(array[i]);
            }
        }
        
        // 将Node的内容复制到数组dest中（这里直接进行数组拷贝）
        @Override
        public void copyInto(int[] dest, int destOffset) {
            System.arraycopy(array, 0, dest, destOffset, curSize);
        }
        
        // 返回Node中包含的元素数量（这里直接返回数组中元素的个数）
        @Override
        public long count() {
            return curSize;
        }
        
        // 将Node中的元素存入int数组后返回
        @Override
        public int[] asPrimitiveArray() {
            if(array.length == curSize) {
                return array;
            } else {
                return Arrays.copyOf(array, curSize);
            }
        }
        
        @Override
        public String toString() {
            return String.format("IntArrayNode[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 普通"数组"Node(long类型版本)
    private static class LongArrayNode implements Node.OfLong {
        final long[] array;
        int curSize;
        
        LongArrayNode(long[] array) {
            this.array = array;
            this.curSize = array.length;
        }
        
        // 新建Node，内部包含一个长度为size的空数组
        LongArrayNode(long size) {
            if(size >= MAX_ARRAY_SIZE)
                throw new IllegalArgumentException(BAD_SIZE);
            this.array = new long[(int) size];
            this.curSize = 0;
        }
        
        // 返回描述此Node中元素的Spliterator（这里是LongArraySpliterator）
        @Override
        public Spliterator.OfLong spliterator() {
            return Arrays.spliterator(array, 0, curSize);
        }
        
        // 遍历Node中的元素，并在其上执行consumer操作
        @Override
        public void forEach(LongConsumer consumer) {
            for(int i = 0; i<curSize; i++) {
                consumer.accept(array[i]);
            }
        }
        
        // 将Node的内容复制到数组dest中（这里直接进行数组拷贝）
        @Override
        public void copyInto(long[] dest, int destOffset) {
            System.arraycopy(array, 0, dest, destOffset, curSize);
        }
        
        // 返回Node中包含的元素数量（这里直接返回数组中元素的个数）
        @Override
        public long count() {
            return curSize;
        }
        
        // 将Node中的元素存入long数组后返回
        @Override
        public long[] asPrimitiveArray() {
            if(array.length == curSize) {
                return array;
            } else {
                return Arrays.copyOf(array, curSize);
            }
        }
        
        @Override
        public String toString() {
            return String.format("LongArrayNode[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 普通"数组"Node(double类型版本)
    private static class DoubleArrayNode implements Node.OfDouble {
        final double[] array;
        int curSize;
        
        DoubleArrayNode(double[] array) {
            this.array = array;
            this.curSize = array.length;
        }
        
        // 新建Node，内部包含一个长度为size的空数组
        DoubleArrayNode(long size) {
            if(size >= MAX_ARRAY_SIZE)
                throw new IllegalArgumentException(BAD_SIZE);
            this.array = new double[(int) size];
            this.curSize = 0;
        }
        
        // 返回描述此Node中元素的Spliterator（这里是DoubleArraySpliterator）
        @Override
        public Spliterator.OfDouble spliterator() {
            return Arrays.spliterator(array, 0, curSize);
        }
        
        // 遍历Node中的元素，并在其上执行consumer操作
        @Override
        public void forEach(DoubleConsumer consumer) {
            for(int i = 0; i<curSize; i++) {
                consumer.accept(array[i]);
            }
        }
        
        // 将Node的内容复制到数组dest中（这里直接进行数组拷贝）
        @Override
        public void copyInto(double[] dest, int destOffset) {
            System.arraycopy(array, 0, dest, destOffset, curSize);
        }
        
        // 返回Node中包含的元素数量（这里直接返回数组中元素的个数）
        @Override
        public long count() {
            return curSize;
        }
        
        // 将Node中的元素存入double数组后返回
        @Override
        public double[] asPrimitiveArray() {
            if(array.length == curSize) {
                return array;
            } else {
                return Arrays.copyOf(array, curSize);
            }
        }
        
        @Override
        public String toString() {
            return String.format("DoubleArrayNode[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    /*▲ [2] 普通"数组"Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ [3] 增强"数组"Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 增强"数组"Node中的有效数据被存储在一个【定长】数组中
     *
     * 与普通"数组"Node不同的是，增强"数组"Node实现了Node.Builder接口，这意味着：
     * 1.构造增强"数组"Node需要使用工厂方法build();
     * 2.增强"数组"Node兼具Sink的功能，即支持对上游发来的元素直接做出进一步的择取。
     */
    
    /**
     * Fixed-sized builder class for reference nodes
     */
    // 增强"数组"Node(引用类型版本)
    private static final class FixedNodeBuilder<T> extends ArrayNode<T> implements Node.Builder<T> {
        
        // 新建FixedNodeBuilder，内部包含一个长度为size的空数组
        FixedNodeBuilder(long size, IntFunction<T[]> generator) {
            super(size, generator);
            assert size<MAX_ARRAY_SIZE;
        }
        
        // 构建固定长度的Node
        @Override
        public Node<T> build() {
            if(curSize<array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", curSize, array.length));
            }
            return this;
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size != array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", size, array.length));
            }
            
            curSize = 0;
        }
        
        /*
         * 对上游发来的引用类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(T t) {
            if(curSize >= array.length) {
                throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", array.length));
            }
            
            // 向ArrayNode存入一个元素
            array[curSize++] = t;
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            if(curSize<array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", curSize, array.length));
            }
        }
        
        @Override
        public String toString() {
            return String.format("FixedNodeBuilder[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 增强"数组"Node(int类型版本)
    private static final class IntFixedNodeBuilder extends IntArrayNode implements Node.Builder.OfInt {
        
        IntFixedNodeBuilder(long size) {
            super(size);
            assert size<MAX_ARRAY_SIZE;
        }
        
        // 构建固定长度的Node
        @Override
        public Node.OfInt build() {
            if(curSize<array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", curSize, array.length));
            }
            
            return this;
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size != array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", size, array.length));
            }
            
            curSize = 0;
        }
        
        /*
         * 对上游发来的int类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(int i) {
            if(curSize >= array.length) {
                throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", array.length));
            }
            
            // 向ArrayNode存入一个元素
            array[curSize++] = i;
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            if(curSize<array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", curSize, array.length));
            }
        }
        
        @Override
        public String toString() {
            return String.format("IntFixedNodeBuilder[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 增强"数组"Node(long类型版本)
    private static final class LongFixedNodeBuilder extends LongArrayNode implements Node.Builder.OfLong {
        
        LongFixedNodeBuilder(long size) {
            super(size);
            assert size<MAX_ARRAY_SIZE;
        }
        
        // 构建固定长度的Node
        @Override
        public Node.OfLong build() {
            if(curSize<array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", curSize, array.length));
            }
            
            return this;
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size != array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", size, array.length));
            }
            
            curSize = 0;
        }
        
        /*
         * 对上游发来的long类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(long i) {
            if(curSize >= array.length) {
                throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", array.length));
            }
            
            // 向ArrayNode存入一个元素
            array[curSize++] = i;
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            if(curSize<array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", curSize, array.length));
            }
        }
        
        @Override
        public String toString() {
            return String.format("LongFixedNodeBuilder[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 增强"数组"Node(double类型版本)
    private static final class DoubleFixedNodeBuilder extends DoubleArrayNode implements Node.Builder.OfDouble {
        
        DoubleFixedNodeBuilder(long size) {
            super(size);
            assert size<MAX_ARRAY_SIZE;
        }
        
        // 构建固定长度的Node
        @Override
        public Node.OfDouble build() {
            if(curSize<array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", curSize, array.length));
            }
            
            return this;
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size != array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", size, array.length));
            }
            
            curSize = 0;
        }
        
        /*
         * 对上游发来的double类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(double i) {
            if(curSize >= array.length) {
                throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", array.length));
            }
            
            // 向ArrayNode存入一个元素
            array[curSize++] = i;
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            if(curSize<array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", curSize, array.length));
            }
        }
        
        @Override
        public String toString() {
            return String.format("DoubleFixedNodeBuilder[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    /*▲ [3] 增强"数组"Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ [4] "弹性缓冲区"Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * "弹性缓冲区"Node中的有效数据被存储在一个类型为SpinedBuffer的弹性缓冲区中。
     *
     * "弹性缓冲区"Node还实现了Node.Builder接口，这意味着：
     * 1.构造"弹性缓冲区"Node需要使用工厂方法build();
     * 2."弹性缓冲区"Node兼具Sink的功能，即支持对上游发来的元素直接做出进一步的择取。
     */
    
    /**
     * Variable-sized builder class for reference nodes
     */
    // "弹性缓冲区"Node(引用类型版本)
    private static final class SpinedNodeBuilder<T> extends SpinedBuffer<T> implements Node<T>, Node.Builder<T> {
        
        private boolean building = false;   // 当前的流是否已被激活
        
        SpinedNodeBuilder() {
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            assert !building : "was already building";
            
            building = true;
            
            // 清空弹性缓冲区
            clear();
            
            // 确保弹性缓冲区容量充足；targetSize是期望的容量
            ensureCapacity(size);
        }
        
        /*
         * 对上游发来的引用类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(T t) {
            assert building : "not building";
            
            // 向SpinedBuffer存入一个元素
            super.accept(t);
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            assert building : "was not building";
            building = false;
        }
        
        // 构建可变长度的Node
        @Override
        public Node<T> build() {
            assert !building : "during building";
            return this;
        }
        
        // 返回描述此SpinedNodeBuilder中元素的Spliterator
        @Override
        public Spliterator<T> spliterator() {
            assert !building : "during building";
            return super.spliterator();
        }
        
        // 遍历SpinedNodeBuilder中的元素，并在其上应用consumer函数
        @Override
        public void forEach(Consumer<? super T> consumer) {
            assert !building : "during building";
            super.forEach(consumer);
        }
        
        // 将SpinedBuffer中的内容复制到数组array的offset偏移中
        @Override
        public void copyInto(T[] array, int offset) {
            assert !building : "during building";
            super.copyInto(array, offset);
        }
        
        // 将SpinedBuffer中的内容复制到数组中返回
        @Override
        public T[] asArray(IntFunction<T[]> arrayFactory) {
            assert !building : "during building";
            return super.asArray(arrayFactory);
        }
        
    }
    
    // "弹性缓冲区"Node(int类型版本)
    private static final class IntSpinedNodeBuilder extends SpinedBuffer.OfInt implements Node.OfInt, Node.Builder.OfInt {
        private boolean building = false;
        
        // Avoid creation of special accessor
        IntSpinedNodeBuilder() {
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            assert !building : "was already building";
            building = true;
            clear();
            ensureCapacity(size);
        }
        
        /*
         * 对上游发来的int类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(int i) {
            assert building : "not building";
            
            // 向SpinedBuffer存入一个元素
            super.accept(i);
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            assert building : "was not building";
            building = false;
        }
        
        // 构建可变长度的Node
        @Override
        public Node.OfInt build() {
            assert !building : "during building";
            return this;
        }
        
        // 返回描述此SpinedNodeBuilder中元素的Spliterator
        @Override
        public Spliterator.OfInt spliterator() {
            assert !building : "during building";
            return super.spliterator();
        }
        
        // 遍历SpinedNodeBuilder中的元素，并在其上应用consumer函数
        @Override
        public void forEach(IntConsumer consumer) {
            assert !building : "during building";
            super.forEach(consumer);
        }
        
        // 将SpinedBuffer中的内容复制到数组array的offset偏移中
        @Override
        public void copyInto(int[] array, int offset) throws IndexOutOfBoundsException {
            assert !building : "during building";
            super.copyInto(array, offset);
        }
        
        // 将SpinedBuffer中的内容复制到数组中返回
        @Override
        public int[] asPrimitiveArray() {
            assert !building : "during building";
            return super.asPrimitiveArray();
        }
    }
    
    // "弹性缓冲区"Node(long类型版本)
    private static final class LongSpinedNodeBuilder extends SpinedBuffer.OfLong implements Node.OfLong, Node.Builder.OfLong {
        private boolean building = false;
        
        // Avoid creation of special accessor
        LongSpinedNodeBuilder() {
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            assert !building : "was already building";
            building = true;
            clear();
            ensureCapacity(size);
        }
        
        /*
         * 对上游发来的long类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(long i) {
            assert building : "not building";
            
            // 向SpinedBuffer存入一个元素
            super.accept(i);
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            assert building : "was not building";
            building = false;
        }
        
        // 构建可变长度的Node
        @Override
        public Node.OfLong build() {
            assert !building : "during building";
            return this;
        }
        
        // 返回描述此SpinedNodeBuilder中元素的Spliterator
        @Override
        public Spliterator.OfLong spliterator() {
            assert !building : "during building";
            return super.spliterator();
        }
        
        // 遍历SpinedNodeBuilder中的元素，并在其上应用consumer函数
        @Override
        public void forEach(LongConsumer consumer) {
            assert !building : "during building";
            super.forEach(consumer);
        }
        
        // 将SpinedBuffer中的内容复制到数组array的offset偏移中
        @Override
        public void copyInto(long[] array, int offset) {
            assert !building : "during building";
            super.copyInto(array, offset);
        }
        
        // 将SpinedBuffer中的内容复制到数组中返回
        @Override
        public long[] asPrimitiveArray() {
            assert !building : "during building";
            return super.asPrimitiveArray();
        }
    }
    
    // "弹性缓冲区"Node(double类型版本)
    private static final class DoubleSpinedNodeBuilder extends SpinedBuffer.OfDouble implements Node.OfDouble, Node.Builder.OfDouble {
        private boolean building = false;
        
        // Avoid creation of special accessor
        DoubleSpinedNodeBuilder() {
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            assert !building : "was already building";
            building = true;
            clear();
            ensureCapacity(size);
        }
        
        /*
         * 对上游发来的double类型的值进行择取。
         * 如果上游存在多个元素，该方法通常会被反复调用。
         */
        @Override
        public void accept(double i) {
            assert building : "not building";
            
            // 向SpinedBuffer存入一个元素
            super.accept(i);
        }
        
        /*
         * 关闭sink链，结束本轮计算。
         *
         * 如果所有目标元素已经处理完了(不一定要处理全部元素)，则需要调用此方法，
         * 在当前方法中，通常需要清除一些有状态中间操作的状态信息，或者释放终端阶段的一些相关资源。
         *
         * 该方法应当在已经得到目标数据之后被调用。
         */
        @Override
        public void end() {
            assert building : "was not building";
            building = false;
        }
        
        // 构建可变长度的Node
        @Override
        public Node.OfDouble build() {
            assert !building : "during building";
            return this;
        }
        
        // 遍历SpinedNodeBuilder中的元素，并在其上应用consumer函数
        @Override
        public Spliterator.OfDouble spliterator() {
            assert !building : "during building";
            return super.spliterator();
        }
        
        // 将SpinedBuffer中的内容复制到数组array的offset偏移中
        @Override
        public void forEach(DoubleConsumer consumer) {
            assert !building : "during building";
            super.forEach(consumer);
        }
        
        // 将SpinedBuffer中的内容复制到数组array的offset偏移中
        @Override
        public void copyInto(double[] array, int offset) {
            assert !building : "during building";
            super.copyInto(array, offset);
        }
        
        // 将SpinedBuffer中的内容复制到数组中返回
        @Override
        public double[] asPrimitiveArray() {
            assert !building : "during building";
            return super.asPrimitiveArray();
        }
    }
    
    /*▲ [4] "弹性缓冲区"Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ [5] Collection-Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * Collection-Node中的有效数据被存储在一个Collection中
     */
    
    /** Node class for a Collection */
    // Collection-Node(引用类型版本)
    private static final class CollectionNode<T> implements Node<T> {
        private final Collection<T> collection;
        
        CollectionNode(Collection<T> collection) {
            this.collection = collection;
        }
        
        @Override
        public Spliterator<T> spliterator() {
            return collection.stream().spliterator();
        }
        
        @Override
        public void copyInto(T[] array, int offset) {
            for (T t : collection) {
                array[offset++] = t;
            }
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public T[] asArray(IntFunction<T[]> generator) {
            return collection.toArray(generator.apply(collection.size()));
        }
        
        @Override
        public long count() {
            return collection.size();
        }
        
        @Override
        public void forEach(Consumer<? super T> consumer) {
            collection.forEach(consumer);
        }
        
        @Override
        public String toString() {
            return String.format("CollectionNode[%d][%s]", collection.size(), collection);
        }
    }
    
    /*▲ [5] Collection-Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ [6] "树状"Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * "树状"Node的常规实现是二叉树，即每个Node最多有两个子Node。
     */
    
    /**
     * Node class for an internal node with two or more children
     */
    // "树状"Node的抽象实现
    private abstract static class AbstractConcNode<T, T_NODE extends Node<T>> implements Node<T> {
        // 包含左右两个子Node
        protected final T_NODE left;    // 左孩子Node
        protected final T_NODE right;   // 右孩子Node
        private final long size;        // "树状"Node中包含的元素数量
        
        AbstractConcNode(T_NODE left, T_NODE right) {
            this.left = left;
            this.right = right;
            
            /*
             * The Node count will be required when the Node spliterator is obtained
             * and it is cheaper to aggressively calculate bottom up as the tree is built rather
             * than later on from the top down traversing the tree
             */
            this.size = left.count() + right.count();
        }
        
        // 返回子Node数量(由于是二叉树，所以总是返回2)
        @Override
        public int getChildCount() {
            return 2;
        }
        
        // 返回指定索引处的子Node(0是左孩子Node，1是右孩子Node)
        @Override
        public T_NODE getChild(int index) {
            if(index == 0) {
                return left;
            }
            
            if(index == 1) {
                return right;
            }
            
            throw new IndexOutOfBoundsException();
        }
        
        // 返回"树状"Node中包含的元素数量(包含子Node中的元素数量)
        @Override
        public long count() {
            return size;
        }
    }
    
    // "树状"Node(引用类型版本)
    static final class ConcNode<T> extends AbstractConcNode<T, Node<T>> implements Node<T> {
        
        ConcNode(Node<T> left, Node<T> right) {
            super(left, right);
        }
        
        // 返回当前Node的流迭代器
        @Override
        public Spliterator<T> spliterator() {
            return new Nodes.InternalNodeSpliterator.OfRef<>(this);
        }
        
        // 将两个子Node的内容复制到数组array中
        @Override
        public void copyInto(T[] array, int offset) {
            Objects.requireNonNull(array);
            left.copyInto(array, offset);
            // Cast to int is safe since it is the callers responsibility to ensure that there is sufficient room in the array
            right.copyInto(array, offset + (int) left.count());
        }
        
        // 返回Node内部数据的数组视图（将两个子Node的内容复制到一个数组）
        @Override
        public T[] asArray(IntFunction<T[]> generator) {
            long size = count();
            if(size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            T[] array = generator.apply((int) size);
            copyInto(array, 0);
            return array;
        }
        
        // 依次遍历两个子Node中的元素，并在其上执行Consumer操作
        @Override
        public void forEach(Consumer<? super T> consumer) {
            left.forEach(consumer);
            right.forEach(consumer);
        }
        
        /*
         * 将Node中[from, to)范围内的元素打包到新建的子Node中返回
         * 注：打包过程可能伴随着进一步的择取
         */
        @Override
        public Node<T> truncate(long from, long to, IntFunction<T[]> generator) {
            if(from == 0 && to == count()) {
                return this;
            }
            
            // 左子树Node包含的元素数量
            long leftCount = left.count();
            
            // 所有元素位于右子树
            if(from >= leftCount) {
                return right.truncate(from - leftCount, to - leftCount, generator);
            }
            
            // 所有元素位于左子树
            if(to<=leftCount) {
                return left.truncate(from, to, generator);
            }
            
            // 元素分布在左子树和右子树上，构造新的"树状"Node
            return Nodes.conc(getShape(), left.truncate(from, leftCount, generator), right.truncate(0, to - leftCount, generator));
        }
        
        @Override
        public String toString() {
            if(count()<32) {
                return String.format("ConcNode[%s.%s]", left, right);
            } else {
                return String.format("ConcNode[size=%d]", count());
            }
        }
        
        
        // "树状"Node(基本数值类型版本)
        private abstract static class OfPrimitive<E, T_CONS, T_ARR, T_SPLITR extends Spliterator.OfPrimitive<E, T_CONS, T_SPLITR>, T_NODE extends Node.OfPrimitive<E, T_CONS, T_ARR, T_SPLITR, T_NODE>> extends AbstractConcNode<E, T_NODE> implements Node.OfPrimitive<E, T_CONS, T_ARR, T_SPLITR, T_NODE> {
            OfPrimitive(T_NODE left, T_NODE right) {
                super(left, right);
            }
            
            // 依次遍历两个子Node中的元素，并在其上执行consumer操作
            @Override
            public void forEach(T_CONS consumer) {
                left.forEach(consumer);
                right.forEach(consumer);
            }
            
            // 将两个子Node的内容复制到数组array中
            @Override
            public void copyInto(T_ARR array, int offset) {
                left.copyInto(array, offset);
                // Cast to int is safe since it is the callers responsibility to ensure that there is sufficient room in the array
                right.copyInto(array, offset + (int) left.count());
            }
            
            // 将Node中的元素存入基本类型数组后返回
            @Override
            public T_ARR asPrimitiveArray() {
                long size = count();
                if(size >= MAX_ARRAY_SIZE) {
                    throw new IllegalArgumentException(BAD_SIZE);
                }
                T_ARR array = newArray((int) size);
                copyInto(array, 0);
                return array;
            }
            
            @Override
            public String toString() {
                if(count()<32) {
                    return String.format("%s[%s.%s]", this.getClass().getName(), left, right);
                }
                
                return String.format("%s[size=%d]", this.getClass().getName(), count());
            }
        }
        
        // "树状"Node(int类型版本)
        static final class OfInt extends ConcNode.OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, Node.OfInt> implements Node.OfInt {
            OfInt(Node.OfInt left, Node.OfInt right) {
                super(left, right);
            }
            
            // 返回描述此Node中元素的Spliterator
            @Override
            public Spliterator.OfInt spliterator() {
                return new InternalNodeSpliterator.OfInt(this);
            }
        }
        
        // "树状"Node(long类型版本)
        static final class OfLong extends ConcNode.OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, Node.OfLong> implements Node.OfLong {
            OfLong(Node.OfLong left, Node.OfLong right) {
                super(left, right);
            }
            
            // 返回描述此Node中元素的Spliterator
            @Override
            public Spliterator.OfLong spliterator() {
                return new InternalNodeSpliterator.OfLong(this);
            }
        }
        
        // "树状"Node(double类型版本)
        static final class OfDouble extends ConcNode.OfPrimitive<Double, DoubleConsumer, double[], Spliterator.OfDouble, Node.OfDouble> implements Node.OfDouble {
            OfDouble(Node.OfDouble left, Node.OfDouble right) {
                super(left, right);
            }
            
            // 返回描述此Node中元素的Spliterator
            @Override
            public Spliterator.OfDouble spliterator() {
                return new InternalNodeSpliterator.OfDouble(this);
            }
        }
        
    }
    
    /*▲ [6] "树状"Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    /*▼ "树状"Node的Spliterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Abstract class for spliterator for all internal node classes */
    // "树状"Node的Spliterator的抽象实现：专门用来访问"树状"Node中的元素
    private abstract static class InternalNodeSpliterator<T, S extends Spliterator<T>, N extends Node<T>> implements Spliterator<T> {
        
        /** Node we are pointing to null if full traversal has occurred */
        N curNode;              // 当前"树状"Node
        
        /** next child of curNode to consume */
        int curChildIndex;      // curNode的子Node索引(0或1)，指示下次应当分割哪个结点
        
        /**
         * The spliterator of the curNode if that node is last and has no children.
         * This spliterator will be delegated to for splitting and traversing.
         * null if curNode has children
         */
        S lastNodeSpliterator;  // 初始值是最后一个结点(最右下方没有孩子结点的那个结点)的流迭代器，之后每次分割都在此对象上进行
        
        /**
         * spliterator used while traversing with tryAdvance
         * null if no partial traversal has occurred
         */
        S tryAdvanceSpliterator;    // 记录下个非空叶子结点的流迭代器
        
        /**
         * node stack used when traversing to search and find leaf nodes
         * null if no partial traversal has occurred
         */
        Deque<N> tryAdvanceStack;   // 双端队列，存储curNode的(所有)子Node
        
        InternalNodeSpliterator(N curNode) {
            this.curNode = curNode;
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        @SuppressWarnings("unchecked")
        public final S trySplit() {
            
            // 已经全部遍历完，或者遍历了一部分，则无法分割
            if(curNode == null || tryAdvanceSpliterator != null) {
                return null; // Cannot split if fully or partially traversed
            }
            
            // 如果lastNodeSpliterator仍然未被分割完，则直接从该对象上分割
            if(lastNodeSpliterator != null) {
                return (S) lastNodeSpliterator.trySplit();
            }
            
            /*
             * 如果curChildIndex为0，则返回curNode的左子树的流迭代器。
             *
             * curChildIndex更新为1。
             */
            if(curChildIndex<curNode.getChildCount() - 1) {
                return (S) curNode.getChild(curChildIndex++).spliterator();
            }
            
            // 如果curChildIndex为1，则更新curNode为curNode的右孩子
            curNode = (N) curNode.getChild(curChildIndex);
            
            // 如果curNode已经没有孩子结点，说明此时没有孩子结点可分割了，curNode成了遍历的最后一个结点
            if(curNode.getChildCount() == 0) {
                // 返回curNode自身的流迭代器，将其记录为最后一个结点
                lastNodeSpliterator = (S) curNode.spliterator();
                
                // 继续分割最后一个结点的流迭代器
                return (S) lastNodeSpliterator.trySplit();
                
                // 反之，如果curNode仍然有孩子结点
            } else {
                // 重置curChildIndex为0
                curChildIndex = 0;
                
                // 继续返回curNode的左子树的流迭代器，并且将curChildIndex更新为1
                return (S) curNode.getChild(curChildIndex++).spliterator();
            }
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public final long estimateSize() {
            // 如果所有(子)结点都遍历完了，直接返回false
            if(curNode == null) {
                return 0;
            }
            
            /*
             * Will not reflect the effects of partial traversal.
             * This is compliant with the specification
             */
            // 如果当前已经是最后一个结点，直接统计该结点包含的元素数量就行
            if(lastNodeSpliterator != null) {
                return lastNodeSpliterator.estimateSize();
            }
            
            long size = 0;
            
            // 否则，需要遍历curNode的子结点，累计所有子结点中包含的元素数量
            for(int i = curChildIndex; i<curNode.getChildCount(); i++) {
                size += curNode.getChild(i).count();
            }
            
            return size;
        }
        
        // 返回流迭代器的参数
        @Override
        public final int characteristics() {
            return Spliterator.SIZED;
        }
        
        /**
         * Initiate a stack containing, in left-to-right order, the child nodes covered by this spliterator
         */
        // 将curNode未分割走的孩子结点添加到双端队列；队头是左Node，队尾是右Node
        @SuppressWarnings("unchecked")
        protected final Deque<N> initStack() {
            // Bias size to the case where leaf nodes are close to this node 8 is the minimum initial capacity for the ArrayDeque implementation
            Deque<N> stack = new ArrayDeque<>(8);
            
            // 逆序遍历curNode未分割走的孩子结点
            for(int i = curNode.getChildCount() - 1; i >= curChildIndex; i--) {
                stack.addFirst((N) curNode.getChild(i));
            }
            
            return stack;
        }
        
        /**
         * Depth first search, in left-to-right order, of the node tree, using an explicit stack, to find the next non-empty leaf node.
         */
        // 深度优先遍历Node树，直到遇到一个包含有效元素的非空叶子节点后返回该Node
        @SuppressWarnings("unchecked")
        protected final N findNextLeafNode(Deque<N> stack) {
            N node;
    
            // 一边找以便删除
            while((node = stack.pollFirst()) != null) {
                // 如果该node不存在子Node
                if(node.getChildCount() == 0) {
                    // 如果该node中包含有效元素，则返回该node
                    if(node.count()>0) {
                        return node;
                    }
            
                    // 如果该node存在子Node，则逆序遍历其孩子结点，并将其继续添加到队列的开头
                } else {
                    for(int i = node.getChildCount() - 1; i >= 0; i--) {
                        stack.addFirst((N) node.getChild(i));
                    }
                }
            }
    
            return null;
        }
        
        // 查找下一个非空叶子结点，并尝试初始化其流迭代器；返回值指示是否获取到了下个待遍历结点的流迭代器
        @SuppressWarnings("unchecked")
        protected final boolean initTryAdvance() {
            // 如果所有(子)结点都遍历完了，直接返回false
            if(curNode == null) {
                return false;
            }
    
            // 如果已经记录了下一个非空叶子结点的流迭代器，直接返回true
            if(tryAdvanceSpliterator != null) {
                return true;
            }
    
            // 如果当前已经分割到了最后一个结点上，则设置tryAdvanceSpliterator为最后一个结点的流迭代器
            if(lastNodeSpliterator != null) {
                tryAdvanceSpliterator = lastNodeSpliterator;
            } else {
                // 将curNode未分割走的孩子结点添加到双端队列；队头是左Node，队尾是右Node
                tryAdvanceStack = initStack();
        
                // 深度优先遍历Node树，直到遇到一个包含有效元素的非空叶子节点后返回该Node
                N leaf = findNextLeafNode(tryAdvanceStack);
        
                // 找到了非空叶子结点
                if(leaf != null) {
                    // 存储此Node的Spliterator
                    tryAdvanceSpliterator = (S) leaf.spliterator();
            
                    // 如果没有找到非空叶子结点，说明所有(子)结点都已经遍历完了
                } else {
                    // A non-empty leaf node was not found
                    // No elements to traverse
                    curNode = null; // 置空curNode
                    return false;
                }
            }
    
            return true;
        }
        
        
        // "树状"Node的Spliterator(引用类型版本)
        private static final class OfRef<T> extends InternalNodeSpliterator<T, Spliterator<T>, Node<T>> {
            
            OfRef(Node<T> curNode) {
                super(curNode);
            }
            
            /*
             * 尝试用consumer消费当前流迭代器中下一个元素。
             * 返回值指示是否找到了下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                // 如果没找获取到下个待遍历结点的流迭代器，直接返回false，表示无法遍历下个元素了
                if(!initTryAdvance()) {
                    return false;
                }
                
                // 尝试用consumer消费当前tryAdvanceSpliterator中下一个元素
                boolean hasNext = tryAdvanceSpliterator.tryAdvance(consumer);
                // 如果消费成功，则返回true
                if(hasNext) {
                    return true;
                }
                
                /*
                 * 消费失败的话，说明tryAdvanceSpliterator关联的结点已经被遍历完了，
                 * 那么接下来需要再次查找下一个非空叶子结点。
                 */
                
                // 如果当前还没到最后一个结点
                if(lastNodeSpliterator == null) {
                    // 深度优先遍历Node树，直到遇到一个包含有效元素的非空叶子节点后返回该Node
                    Node<T> leaf = findNextLeafNode(tryAdvanceStack);
                    
                    // 如果找到了有效的非空叶子结点
                    if(leaf != null) {
                        // 记录该非空叶子结点流迭代器
                        tryAdvanceSpliterator = leaf.spliterator();
                        
                        // 尝试用consumer消费当前tryAdvanceSpliterator中下一个元素，返回值指示是否找到了下一个元素
                        return tryAdvanceSpliterator.tryAdvance(consumer);
                    }
                }
                
                /*
                 * 如果经过上面的尝试任然没有找到下一个非空叶子结点，说明所有结点都已经遍历过了，
                 * 接下来置空curNode，表示遍历结束。
                 */
                curNode = null;
                
                return hasNext;
            }
            
            /*
             * 尝试用consumer逐个消费当前流迭代器中所有剩余元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                
                // 如果所有(子)结点都遍历完了，直接返回false
                if(curNode == null) {
                    return;
                }
                
                // 如果已经记录了某个非空叶子结点的流迭代器，则直接从该流迭代器出发，遍历整棵Node树
                if(tryAdvanceSpliterator != null) {
                    while(tryAdvance(consumer)) {
                    }
                } else {
                    // 如果当前已经是最后一个结点，则遍历最后这个结点就可以
                    if(lastNodeSpliterator != null) {
                        lastNodeSpliterator.forEachRemaining(consumer);
                        
                        // 遍历curNode的所有子结点中的元素
                    } else {
                        // 将curNode未分割走的孩子结点添加到双端队列；队头是左Node，队尾是右Node
                        Deque<Node<T>> stack = initStack();
                        Node<T> leaf;
                        
                        // 深度优先遍历Node树，直到遇到一个包含有效元素的非空叶子节点后返回该Node
                        while((leaf = findNextLeafNode(stack)) != null) {
                            // 遍历该子结点中的元素
                            leaf.forEach(consumer);
                        }
                        
                        // 标记所有元素已经遍历完
                        curNode = null;
                    }
                }
            }
        }
        
        // "树状"Node的Spliterator(基本数值类型版本)
        private abstract static class OfPrimitive<T, T_CONS, T_ARR, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, N extends Node.OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, N>> extends InternalNodeSpliterator<T, T_SPLITR, N> implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            
            OfPrimitive(N cur) {
                super(cur);
            }
            
            /*
             * 尝试用consumer消费当前流迭代器中下一个元素。
             * 返回值指示是否找到了下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(T_CONS consumer) {
                // 如果没找获取到下个待遍历结点的流迭代器，直接返回false，表示无法遍历下个元素了
                if(!initTryAdvance()) {
                    return false;
                }
                
                // 尝试用consumer消费当前tryAdvanceSpliterator中下一个元素
                boolean hasNext = tryAdvanceSpliterator.tryAdvance(consumer);
                // 如果消费成功，则返回true
                if(hasNext) {
                    return true;
                }
                
                /*
                 * 消费失败的话，说明tryAdvanceSpliterator关联的结点已经被遍历完了，
                 * 那么接下来需要再次查找下一个非空叶子结点。
                 */
                
                // 如果当前还没到最后一个结点
                if(lastNodeSpliterator == null) {
                    // 深度优先遍历Node树，直到遇到一个包含有效元素的非空叶子节点后返回该Node
                    N leaf = findNextLeafNode(tryAdvanceStack);
                    
                    // 如果找到了有效的非空叶子结点
                    if(leaf != null) {
                        // 记录该非空叶子结点流迭代器
                        tryAdvanceSpliterator = leaf.spliterator();
                        
                        // 尝试用consumer消费当前tryAdvanceSpliterator中下一个元素，返回值指示是否找到了下一个元素
                        return tryAdvanceSpliterator.tryAdvance(consumer);
                    }
                }
                
                /*
                 * 如果经过上面的尝试任然没有找到下一个非空叶子结点，说明所有结点都已经遍历过了，
                 * 接下来置空curNode，表示遍历结束。
                 */
                curNode = null;
                
                return hasNext;
            }
            
            /*
             * 尝试用consumer逐个消费当前流迭代器中所有剩余元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public void forEachRemaining(T_CONS consumer) {
                
                // 如果所有(子)结点都遍历完了，直接返回false
                if(curNode == null) {
                    return;
                }
                
                // 如果已经记录了某个非空叶子结点的流迭代器，则直接从该流迭代器出发，遍历整棵Node树
                if(tryAdvanceSpliterator != null) {
                    while(tryAdvance(consumer)) {
                    }
                } else {
                    // 如果当前已经是最后一个结点，则遍历最后这个结点就可以
                    if(lastNodeSpliterator != null) {
                        lastNodeSpliterator.forEachRemaining(consumer);
                        
                        // 遍历curNode的所有子结点中的元素
                    } else {
                        // 将curNode未分割走的孩子结点添加到双端队列；队头是左Node，队尾是右Node
                        Deque<N> stack = initStack();
                        N leaf;
                        
                        // 深度优先遍历Node树，直到遇到一个包含有效元素的非空叶子节点后返回该Node
                        while((leaf = findNextLeafNode(stack)) != null) {
                            // 遍历该子结点中的元素
                            leaf.forEach(consumer);
                        }
                        
                        // 标记所有元素已经遍历完
                        curNode = null;
                    }
                }
            }
        }
        
        // "树状"Node的Spliterator(int类型版本)
        private static final class OfInt extends OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, Node.OfInt> implements Spliterator.OfInt {
            OfInt(Node.OfInt cur) {
                super(cur);
            }
        }
        
        // "树状"Node的Spliterator(long类型版本)
        private static final class OfLong extends OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, Node.OfLong> implements Spliterator.OfLong {
            OfLong(Node.OfLong cur) {
                super(cur);
            }
        }
        
        // "树状"Node的Spliterator(double类型版本)
        private static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, double[], Spliterator.OfDouble, Node.OfDouble> implements Spliterator.OfDouble {
            OfDouble(Node.OfDouble cur) {
                super(cur);
            }
        }
    }
    
    /*▲ "树状"Node的Spliterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    /*▼ 专用Task ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // "并行复制"任务的抽象实现：将树状node中的元素并行地复制到指定的数组中
    @SuppressWarnings("serial")
    private abstract static class ToArrayTask<T, T_NODE extends Node<T>, K extends ToArrayTask<T, T_NODE, K>> extends CountedCompleter<Void> {
        
        protected final T_NODE node;    // 当前task包含的node，有效数据就存储在该node
        protected final int offset;     // 有效数据在node中的起始索引
        
        ToArrayTask(T_NODE node, int offset) {
            this.node = node;
            this.offset = offset;
        }
        
        ToArrayTask(K parent, T_NODE node, int offset) {
            super(parent);
            this.node = node;
            this.offset = offset;
        }
        
        // 将当前Node的第childIndex个子结点包装为子任务，以待处理
        abstract K makeChild(int childIndex, int offset);
        
        // 将node中的元素复制到指定的数组中
        abstract void copyNodeToArray();
        
        // 并行处理当前task，最终目的是将node中的元素全部复制到指定的数组内
        @Override
        public void compute() {
            ToArrayTask<T, T_NODE, K> task = this;
            
            while(true) {
                // 获取task内node的子Node数量
                int count = task.node.getChildCount();
                
                // 如果task内的node已经没有子Node了，说明该task不需要再拆分了，即可以直接处理了
                if(count == 0) {
                    // 将task内node中的元素复制到数组array的offset索引处
                    task.copyNodeToArray();
                    // 将task标记为[已完成]，并将父任务的挂起计数减一
                    task.propagateCompletion();
                    return;
                }
                
                /*
                 * 如果task内的node存在子Node，则设置task的挂起次数为count-1；
                 * 注：设置count-1的原因是可以由task分出count子task，
                 * 　　当前线程会执行其中一个子task，而其他线程会执行count-1个子task。
                 */
                task.setPendingCount(count - 1);
                
                int size = 0;
                int i = 0;
                
                // 接下来，先由其他线程处理其余count-1个子task(结点)
                while(i<count - 1) {
                    
                    // 将task内node的第i个子结点包装为一个子task，以待处理
                    K childTask = task.makeChild(i, task.offset + size);
                    
                    // 累计各个子task处理的元素数量
                    size += childTask.node.count();
                    
                    // 将子任务交给其他工作线程去完成
                    childTask.fork();
                    
                    i++;
                }
                
                // 再由当前线程处理最后一个子任务
                task = task.makeChild(i, task.offset + size);
            }
        }
        
        
        // "并行复制"任务(引用类型版本)
        @SuppressWarnings("serial")
        private static final class OfRef<T> extends ToArrayTask<T, Node<T>, OfRef<T>> {
            private final T[] array;
            
            private OfRef(Node<T> node, T[] array, int offset) {
                super(node, offset);
                this.array = array;
            }
            
            private OfRef(OfRef<T> parent, Node<T> node, int offset) {
                super(parent, node, offset);
                this.array = parent.array;
            }
            
            // 将当前Node的第childIndex个子结点包装为一个子任务，以待处理
            @Override
            OfRef<T> makeChild(int childIndex, int offset) {
                return new OfRef<>(this, node.getChild(childIndex), offset);
            }
            
            // 将node中的元素复制到数组array的offset索引处
            @Override
            void copyNodeToArray() {
                node.copyInto(array, offset);
            }
        }
        
        // "并行复制"任务(基本数值类型版本)
        @SuppressWarnings("serial")
        private static class OfPrimitive<T, T_CONS, T_ARR, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_NODE extends Node.OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE>> extends ToArrayTask<T, T_NODE, OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE>> {
            private final T_ARR array;
            
            private OfPrimitive(T_NODE node, T_ARR array, int offset) {
                super(node, offset);
                this.array = array;
            }
            
            private OfPrimitive(OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE> parent, T_NODE node, int offset) {
                super(parent, node, offset);
                this.array = parent.array;
            }
            
            // 将当前Node的第childIndex个子结点包装为一个子任务，以待处理
            @Override
            OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE> makeChild(int childIndex, int offset) {
                return new OfPrimitive<>(this, node.getChild(childIndex), offset);
            }
            
            // 将node中的元素复制到数组array的offset索引处
            @Override
            void copyNodeToArray() {
                // 将Node的内容复制到数组array中offset偏移处
                node.copyInto(array, offset);
            }
        }
        
        // "并行复制"任务(int类型版本)
        @SuppressWarnings("serial")
        private static final class OfInt extends OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, Node.OfInt> {
            private OfInt(Node.OfInt node, int[] array, int offset) {
                super(node, array, offset);
            }
        }
        
        // "并行复制"任务(long类型版本)
        @SuppressWarnings("serial")
        private static final class OfLong extends OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, Node.OfLong> {
            private OfLong(Node.OfLong node, long[] array, int offset) {
                super(node, array, offset);
            }
        }
        
        // "并行复制"任务(double类型版本)
        @SuppressWarnings("serial")
        private static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, double[], Spliterator.OfDouble, Node.OfDouble> {
            private OfDouble(Node.OfDouble node, double[] array, int offset) {
                super(node, array, offset);
            }
        }
    }
    
    /*
     * This and subclasses are not intended to be serializable
     */
    /*
     * 线性"并行择取"任务的抽象实现：将spliterator中的元素并行地择取/筛选到指定的数组中。
     * 如果每次分割出来的子spliterator包含的元素数量固定，则需要使用这种任务。
     */
    @SuppressWarnings("serial")
    private abstract static class SizedCollectorTask<P_IN, P_OUT, T_SINK extends Sink<P_OUT>, K extends SizedCollectorTask<P_IN, P_OUT, T_SINK, K>> extends CountedCompleter<Void> implements Sink<P_OUT> {
        
        protected final PipelineHelper<P_OUT> helper;   // 当前操作的流
        protected final Spliterator<P_IN> spliterator;  // 流迭代器，作为数据源
        protected long offset;  // 当前task包含的数据在spliterator中的起始索引
        protected long length;  // 当前task包含的数据量
        
        protected final long targetSize;    // 每个子任务(建议)包含的元素数量；如果某个task包含的数据量超过这个阈值，则需要考虑对该task进行拆分
        
        // For Sink implementation
        protected int index, fence; // 用于sink的起始索引与上限值
        
        SizedCollectorTask(Spliterator<P_IN> spliterator, PipelineHelper<P_OUT> helper, int arrayLength) {
            assert spliterator.hasCharacteristics(Spliterator.SUBSIZED);
            
            this.helper = helper;
            this.spliterator = spliterator;
            this.offset = 0;
            this.length = arrayLength;
            
            /*
             * 初始时，返回流迭代器中的元素总量(可能不精确)。
             * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
             * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
             */
            long size = spliterator.estimateSize();
            
            // 根据传入的元素总量，返回每个子任务(建议)包含的元素数量
            this.targetSize = AbstractTask.suggestTargetSize(size);
        }
        
        SizedCollectorTask(K parent, Spliterator<P_IN> spliterator, long offset, long length, int arrayLength) {
            super(parent);
    
            assert spliterator.hasCharacteristics(Spliterator.SUBSIZED);
    
            this.helper = parent.helper;
            this.spliterator = spliterator;
            this.offset = offset;
            this.length = length;
    
            this.targetSize = parent.targetSize;
    
            if(offset<0 || length<0 || (offset + length - 1 >= arrayLength)) {
                throw new IllegalArgumentException(String.format("offset and length interval [%d, %d + %d) is not within array size interval [0, %d)", offset, offset, length, arrayLength));
            }
        }
        
        // 将当前task内的流迭代器中offset处起的size个元素封装到一个子任务中，以待处理
        abstract K makeChild(Spliterator<P_IN> spliterator, long offset, long size);
        
        // 并行处理当前task，最终目的是将spliterator中的元素全部收集到指定的数组内
        @Override
        public void compute() {
            SizedCollectorTask<P_IN, P_OUT, T_SINK, K> task = this;
            
            // 将当前任务安排为right任务
            Spliterator<P_IN> rightSplit = spliterator;
            Spliterator<P_IN> leftSplit;
            
            // 从当前任务中切分一部分作为子任务
            while(true) {
                
                // 获取right任务剩余元素数量
                long rightSplitSize = rightSplit.estimateSize();
                
                // 如果right任务的数据量已经满足要求，则无需再拆分
                if(rightSplitSize<=task.targetSize) {
                    break;
                }
                
                // 如果right任务数据量过大，则需要拆分其流迭代器
                leftSplit = rightSplit.trySplit();
                if(leftSplit == null) {
                    break;
                }
                
                // 设置一个挂起计数，原因是leftTask属于task的子任务，且leftTask会交给其他线程去完成
                task.setPendingCount(1);
                
                // 获取left任务剩余元素数量
                long leftSplitSize = leftSplit.estimateSize();
                
                // 封装left任务
                K leftTask = task.makeChild(leftSplit, task.offset, leftSplitSize);
                
                // 将left任务提交给线程池，由别的工作线程去处理
                leftTask.fork();
                
                // 把剩余元素封装为right任务
                K rightTask = task.makeChild(rightSplit, task.offset + leftSplitSize, task.length - leftSplitSize);
                
                // 更新task为right任务
                task = rightTask;
            }
            
            assert task.offset + task.length<MAX_ARRAY_SIZE;
            
            /* 至此，task中的数据量已在targetSize范围内，可以由当前线程直接处理了 */
            
            // 注：这里的task本身就是一个sink
            @SuppressWarnings("unchecked")
            T_SINK downSink = (T_SINK) task;
            
            /*
             * 由当前线程处理task
             *
             * 从downSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
             * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
             * 最终择取出的数据往往被存入了downSink代表的容器当中。
             *
             * terminalSink: (相对于task.helper的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             * spliterator : 流迭代器，作为数据源，包含了当前所有待访问的元素
             */
            task.helper.wrapAndCopyInto(downSink, rightSplit);
            
            // 将task标记为[已完成]，并将父任务的挂起计数减一
            task.propagateCompletion();
        }
        
        /*
         * 激活sink链上所有sink，完成一些初始化工作，准备接收数据。
         *
         * 对于终端阶段的sink，通常在begin()里初始化接收数据的容器。
         * 对于有状态的中间阶段的sink，通常在begin()里初始化相关的状态信息。
         *
         * 该方法应当在处理所有数据之前被调用。
         *
         * size: 上游发来的元素数量；如果数据量未知或无限，该值通常是-1
         */
        @Override
        public void begin(long size) {
            if(size>length) {
                throw new IllegalStateException("size passed to Sink.begin exceeds array length");
            }
            
            /*
             * Casts to int are safe since absolute size is verified to be within bounds
             * when the root concrete SizedCollectorTask is constructed with the shared array
             */
            index = (int) offset;           // 初始化待访问元素的起始索引
            fence = index + (int) length;   // 初始化待访问元素的终止索引
        }
        
        
        // 线性"并行择取"任务(引用类型版本)
        @SuppressWarnings("serial")
        static final class OfRef<P_IN, P_OUT> extends SizedCollectorTask<P_IN, P_OUT, Sink<P_OUT>, OfRef<P_IN, P_OUT>> implements Sink<P_OUT> {
            private final P_OUT[] array;
            
            OfRef(Spliterator<P_IN> spliterator, PipelineHelper<P_OUT> helper, P_OUT[] array) {
                super(spliterator, helper, array.length);
                this.array = array;
            }
            
            OfRef(OfRef<P_IN, P_OUT> parent, Spliterator<P_IN> spliterator, long offset, long length) {
                super(parent, spliterator, offset, length, parent.array.length);
                this.array = parent.array;
            }
            
            /*
             * 对上游发来的引用类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(P_OUT value) {
                if(index >= fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(index));
                }
                
                // 向当前任务的数组中存入一个元素
                array[index++] = value;
            }
            
            // 将当前task内的流迭代器中offset处起的size个元素封装到一个子任务中，以待处理
            @Override
            OfRef<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new OfRef<>(this, spliterator, offset, size);
            }
        }
        
        // 线性"并行择取"任务(int类型版本)
        @SuppressWarnings("serial")
        static final class OfInt<P_IN> extends SizedCollectorTask<P_IN, Integer, Sink.OfInt, OfInt<P_IN>> implements Sink.OfInt {
            private final int[] array;
            
            OfInt(Spliterator<P_IN> spliterator, PipelineHelper<Integer> helper, int[] array) {
                super(spliterator, helper, array.length);
                this.array = array;
            }
            
            OfInt(SizedCollectorTask.OfInt<P_IN> parent, Spliterator<P_IN> spliterator, long offset, long length) {
                super(parent, spliterator, offset, length, parent.array.length);
                this.array = parent.array;
            }
            
            /*
             * 对上游发来的int类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(int value) {
                if(index >= fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(index));
                }
                
                // 向当前任务的数组中存入一个元素
                array[index++] = value;
            }
            
            // 将当前task内的流迭代器中offset处起的size个元素封装到一个子任务中，以待处理
            @Override
            SizedCollectorTask.OfInt<P_IN> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new SizedCollectorTask.OfInt<>(this, spliterator, offset, size);
            }
        }
        
        // 线性"并行择取"任务(long类型版本)
        @SuppressWarnings("serial")
        static final class OfLong<P_IN> extends SizedCollectorTask<P_IN, Long, Sink.OfLong, OfLong<P_IN>> implements Sink.OfLong {
            private final long[] array;
            
            OfLong(Spliterator<P_IN> spliterator, PipelineHelper<Long> helper, long[] array) {
                super(spliterator, helper, array.length);
                this.array = array;
            }
            
            OfLong(SizedCollectorTask.OfLong<P_IN> parent, Spliterator<P_IN> spliterator, long offset, long length) {
                super(parent, spliterator, offset, length, parent.array.length);
                this.array = parent.array;
            }
            
            /*
             * 对上游发来的long类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(long value) {
                if(index >= fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(index));
                }
                
                // 向当前任务的数组中存入一个元素
                array[index++] = value;
            }
            
            // 将当前task内的流迭代器中offset处起的size个元素封装到一个子任务中，以待处理
            @Override
            SizedCollectorTask.OfLong<P_IN> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new SizedCollectorTask.OfLong<>(this, spliterator, offset, size);
            }
        }
        
        // 线性"并行择取"任务(double类型版本)
        @SuppressWarnings("serial")
        static final class OfDouble<P_IN> extends SizedCollectorTask<P_IN, Double, Sink.OfDouble, OfDouble<P_IN>> implements Sink.OfDouble {
            private final double[] array;
            
            OfDouble(Spliterator<P_IN> spliterator, PipelineHelper<Double> helper, double[] array) {
                super(spliterator, helper, array.length);
                this.array = array;
            }
            
            OfDouble(SizedCollectorTask.OfDouble<P_IN> parent, Spliterator<P_IN> spliterator, long offset, long length) {
                super(parent, spliterator, offset, length, parent.array.length);
                this.array = parent.array;
            }
            
            /*
             * 对上游发来的double类型的值进行择取。
             * 如果上游存在多个元素，该方法通常会被反复调用。
             */
            @Override
            public void accept(double value) {
                if(index >= fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(index));
                }
                
                // 向当前任务的数组中存入一个元素
                array[index++] = value;
            }
            
            // 将当前task内的流迭代器中offset处起的size个元素封装到一个子任务中，以待处理
            @Override
            SizedCollectorTask.OfDouble<P_IN> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new SizedCollectorTask.OfDouble<>(this, spliterator, offset, size);
            }
        }
    }
    
    /*
     * 树状"并行择取"任务：将spliterator中的元素并行地择取/筛选到指定的Node(数组)中。
     * 如果每次分割出来的子spliterator包含的元素数量不固定，则需要使用这种任务。
     */
    @SuppressWarnings("serial")
    private static class CollectorTask<P_IN, P_OUT, T_NODE extends Node<P_OUT>, T_BUILDER extends Node.Builder<P_OUT>> extends AbstractTask<P_IN, P_OUT, T_NODE, CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER>> {
        
        protected final PipelineHelper<P_OUT> helper;               // 流
        protected final LongFunction<T_BUILDER> builderFactory;     // builderFactory生成一个对象，该对象既是用于终端阶段的sink，又是作为终端阶段收集元素的容器
        protected final BinaryOperator<T_NODE> concFactory;         // 连接子任务的函数表达式：当某个树状任务的左右孩子任务执行完成后，使用该表达式来连接左右孩子任务的执行结果
        
        CollectorTask(PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, LongFunction<T_BUILDER> builderFactory, BinaryOperator<T_NODE> concFactory) {
            super(helper, spliterator);
            this.helper = helper;
            this.builderFactory = builderFactory;
            this.concFactory = concFactory;
        }
        
        CollectorTask(CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER> parent, Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            helper = parent.helper;
            builderFactory = parent.builderFactory;
            concFactory = parent.concFactory;
        }
        
        // 返回一个子任务，该子任务的数据源是spliterator，以待处理
        @Override
        protected CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER> makeChild(Spliterator<P_IN> spliterator) {
            return new CollectorTask<>(this, spliterator);
        }
        
        /*
         * 在当前(子)任务执行完成后，需要执行该回调方法。
         *
         * 参数中的caller是促进当前任务完成的子任务(只有子任务完成了，父任务才可能完成)。
         * 如果当前任务没有子任务，或者并不关心子任务，则参数caller的值可以直接传入当前任务。
         *
         * 注：该方法在complete()或tryComplete()中被回调
         */
        @Override
        public void onCompletion(CountedCompleter<?> caller) {
            // 如果当前任务不是叶子任务
            if(!isLeaf()) {
                /*
                 * 对左右(叶子)任务的执行结果进行"合成"，返回"合成"后的node。
                 * 至于具体的合成策略是什么，则取决于函数表达式concFactory。
                 */
                T_NODE node = concFactory.apply(leftChild.getLocalResult(), rightChild.getLocalResult());
                
                // 设置当前任务的执行结果
                setLocalResult(node);
            }
            
            // 执行父类回调：清空当前任务内的核心参数
            super.onCompletion(caller);
        }
        
        // 计算helper中的任务，计算结果会存入一个Node后返回
        @Override
        @SuppressWarnings("unchecked")
        protected T_NODE doLeaf() {
    
            /*
             * 初始时，尝试返回spliterator中的元素总量。如果无法获取精确值，则返回-1。
             * 当访问过spliterator中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
             *
             * 注：通常在流拥有SIZED参数(相当于spliterator有SIZED参数)时可以获取到一个精确值。
             */
            long sizeIfKnown = helper.exactOutputSizeIfKnown(spliterator);
    
            // nodeBuilderSink是一个Node构建器-Sink，这是用于终端阶段的sink
            T_BUILDER nodeBuilderSink = builderFactory.apply(sizeIfKnown);
    
            /*
             * 从nodeBuilderSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
             * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
             * 最终择取出的数据往往被存入了nodeBuilderSink代表的容器当中。
             *
             * nodeBuilderSink: (相对于helper的)下个流阶段的sink。如果nodeBuilderSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             * spliterator    : 流迭代器，作为数据源，包含了当前所有待访问的元素
             */
            helper.wrapAndCopyInto(nodeBuilderSink, spliterator);
    
            // 返回构造的node
            return (T_NODE) nodeBuilderSink.build();
        }
        
        
        // 树状"并行择取"任务(引用类型版本)
        @SuppressWarnings("serial")
        private static final class OfRef<P_IN, P_OUT> extends CollectorTask<P_IN, P_OUT, Node<P_OUT>, Node.Builder<P_OUT>> {
            OfRef(PipelineHelper<P_OUT> helper, IntFunction<P_OUT[]> generator, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, s -> builder(s, generator), ConcNode::new);
            }
        }
        
        // 树状"并行择取"任务(int类型版本)
        @SuppressWarnings("serial")
        private static final class OfInt<P_IN> extends CollectorTask<P_IN, Integer, Node.OfInt, Node.Builder.OfInt> {
            OfInt(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, Nodes::intBuilder, ConcNode.OfInt::new);
            }
        }
        
        // 树状"并行择取"任务(long类型版本)
        @SuppressWarnings("serial")
        private static final class OfLong<P_IN> extends CollectorTask<P_IN, Long, Node.OfLong, Node.Builder.OfLong> {
            OfLong(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, Nodes::longBuilder, ConcNode.OfLong::new);
            }
        }
        
        // 树状"并行择取"任务(double类型版本)
        @SuppressWarnings("serial")
        private static final class OfDouble<P_IN> extends CollectorTask<P_IN, Double, Node.OfDouble, Node.Builder.OfDouble> {
            OfDouble(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, Nodes::doubleBuilder, ConcNode.OfDouble::new);
            }
        }
    }
    
    /*▲ 专用Task ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
