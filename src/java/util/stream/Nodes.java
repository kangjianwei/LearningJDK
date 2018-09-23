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
// Node工厂，定义了六大类Node，应用于流水线的终端阶段
final class Nodes {
    
    private Nodes() {
        throw new Error("no instances");
    }
    
    /**
     * The maximum size of an array that can be allocated.
     */
    static final long MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    
    // IllegalArgumentException messages
    static final String BAD_SIZE = "Stream size exceeds max array size";
    
    @SuppressWarnings("rawtypes")
    private static final Node EMPTY_NODE = new EmptyNode.OfRef();
    private static final Node.OfInt EMPTY_INT_NODE = new EmptyNode.OfInt();
    private static final Node.OfLong EMPTY_LONG_NODE = new EmptyNode.OfLong();
    private static final Node.OfDouble EMPTY_DOUBLE_NODE = new EmptyNode.OfDouble();
    
    
    /*▼ 构造第(1)类Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Produces an empty node whose count is zero, has no children and no content.
     *
     * @param <T> the type of elements of the created node
     * @param shape the shape of the node to be created
     * @return an empty node.
     */
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
    
    /*▲ 构造第(1)类Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 构造第(2)类Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Produces a {@link Node} describing an array.
     *
     * <p>The node will hold a reference to the array and will not make a copy.
     *
     * @param <T> the type of elements held by the node
     * @param array the array
     * @return a node holding an array
     */
    // 返回ArrayNode
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
    // 返回IntArrayNode
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
    // 返回LongArrayNode
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
    // 返回DoubleArrayNode
    static Node.OfDouble node(final double[] array) {
        return new DoubleArrayNode(array);
    }
    
    /*▲ 构造第(2)类Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 构造第(3)、(4)类Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 返回Nodes.FixedNodeBuilder或Nodes.SpinedNodeBuilder
    static <T> Node.Builder<T> builder(long exactSizeIfKnown, IntFunction<T[]> generator) {
        return (exactSizeIfKnown >= 0 && exactSizeIfKnown < MAX_ARRAY_SIZE)
            ? new FixedNodeBuilder<>(exactSizeIfKnown, generator)    // 新建FixedNodeBuilder，该Builder可用于创建固定长度的Node
            : builder();                                             // 新建SpinedNodeBuilder，该Builder可用于创建可变长度的Node
    }
    
    /**
     * Produces a variable size {@link Node.Builder}.
     *
     * @param <T> the type of elements of the node builder
     * @return a {@code Node.Builder}
     */
    // 返回Nodes.SpinedNodeBuilder
    static <T> Node.Builder<T> builder() {
        return new SpinedNodeBuilder<>();
    }
    
    /**
     * Produces a {@link Node.Builder.OfInt}.
     *
     * @param exactSizeIfKnown -1 if a variable size builder is requested,
     * otherwise the exact capacity desired.  A fixed capacity builder will
     * fail if the wrong number of elements are added to the builder.
     * @return a {@code Node.Builder.OfInt}
     */
    // 返回Nodes.IntNodeBuilder或Nodes.IntNodeBuilder
    static Node.Builder.OfInt intBuilder(long exactSizeIfKnown) {
        return (exactSizeIfKnown >= 0 && exactSizeIfKnown < MAX_ARRAY_SIZE)
            ? new IntFixedNodeBuilder(exactSizeIfKnown)
            : intBuilder();
    }
    
    /**
     * Produces a variable size @{link Node.Builder.OfInt}.
     *
     * @return a {@code Node.Builder.OfInt}
     */
    // 返回Nodes.IntSpinedNodeBuilder
    static Node.Builder.OfInt intBuilder() {
        return new IntSpinedNodeBuilder();
    }
    
    /**
     * Produces a {@link Node.Builder.OfLong}.
     *
     * @param exactSizeIfKnown -1 if a variable size builder is requested,
     * otherwise the exact capacity desired.  A fixed capacity builder will
     * fail if the wrong number of elements are added to the builder.
     * @return a {@code Node.Builder.OfLong}
     */
    // 返回Nodes.LongFixedNodeBuilder或Nodes.LongSpinedNodeBuilder
    static Node.Builder.OfLong longBuilder(long exactSizeIfKnown) {
        return (exactSizeIfKnown >= 0 && exactSizeIfKnown < MAX_ARRAY_SIZE)
            ? new LongFixedNodeBuilder(exactSizeIfKnown)
            : longBuilder();
    }
    
    /**
     * Produces a variable size @{link Node.Builder.OfLong}.
     *
     * @return a {@code Node.Builder.OfLong}
     */
    // 返回Nodes.LongSpinedNodeBuilder
    static Node.Builder.OfLong longBuilder() {
        return new LongSpinedNodeBuilder();
    }
    
    /**
     * Produces a {@link Node.Builder.OfDouble}.
     *
     * @param exactSizeIfKnown -1 if a variable size builder is requested,
     * otherwise the exact capacity desired.  A fixed capacity builder will
     * fail if the wrong number of elements are added to the builder.
     * @return a {@code Node.Builder.OfDouble}
     */
    // 返回Nodes.DoubleFixedNodeBuilder或Nodes.DoubleSpinedNodeBuilder
    static Node.Builder.OfDouble doubleBuilder(long exactSizeIfKnown) {
        return (exactSizeIfKnown >= 0 && exactSizeIfKnown < MAX_ARRAY_SIZE)
            ? new DoubleFixedNodeBuilder(exactSizeIfKnown)
            : doubleBuilder();
    }
    
    /**
     * Produces a variable size @{link Node.Builder.OfDouble}.
     *
     * @return a {@code Node.Builder.OfDouble}
     */
    // 返回Nodes.DoubleSpinedNodeBuilder
    static Node.Builder.OfDouble doubleBuilder() {
        return new DoubleSpinedNodeBuilder();
    }
    
    /*▲ 构造第(3)、(4)类Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 构造第(5)类Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Produces a {@link Node} describing a {@link Collection}.
     * <p>
     * The node will hold a reference to the collection and will not make a copy.
     *
     * @param <T> the type of elements held by the node
     * @param c the collection
     * @return a node holding a collection
     */
    static <T> Node<T> node(Collection<T> c) {
        return new CollectionNode<>(c);
    }
    
    /*▲ 构造第(5)类Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /*▼ 构造第(6)类Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    // 将两个Node串联起来，并返回串联后的Node
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
    
    /*▲ 构造第(6)类Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ flatten，并行地给Node降维 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Flatten, in parallel, a {@link Node}.  A flattened node is one that has
     * no children.  If the node is already flat, it is simply returned.
     *
     * @implSpec
     * If a new node is to be created, the generator is used to create an array
     * whose length is {@link Node#count()}.  Then the node tree is traversed
     * and leaf node elements are placed in the array concurrently by leaf tasks
     * at the correct offsets.
     *
     * @param <T> type of elements contained by the node
     * @param node the node to flatten
     * @param generator the array factory used to create array instances
     * @return a flat {@code Node}
     */
    // 将当前的Node降维成非树形Node
    public static <T> Node<T> flatten(Node<T> node, IntFunction<T[]> generator) {
        // 非线性Node（一般指树形Node）
        if (node.getChildCount() > 0) {
            long size = node.count();
            if (size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            
            // 生成指定类型的数组以存储node中的数据
            T[] array = generator.apply((int) size);
            
            // 并行地将Node中的元素转存到线性数组中
            new ToArrayTask.OfRef<>(node, array, 0).invoke();
            
            // 返回线性Node
            return node(array);
        }
        
        // 线性Node，包括存储结构为二维数组的Node
        return node;
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
    // 将当前的Node降维成非树形Node
    public static Node.OfInt flattenInt(Node.OfInt node) {
        // 非线性Node（一般指树形Node）
        if (node.getChildCount() > 0) {
            long size = node.count();
            if (size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            
            // 生成int类型的数组以存储node中的数据
            int[] array = new int[(int) size];
            
            // 并行地将Node中的元素转存到线性数组中
            new ToArrayTask.OfInt(node, array, 0).invoke();
            
            // 返回线性Node
            return node(array);
        }
        
        // 线性Node，包括存储结构为二维数组的Node
        return node;
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
    // 将当前的Node降维成非树形Node
    public static Node.OfLong flattenLong(Node.OfLong node) {
        // 非线性Node（一般指树形Node）
        if (node.getChildCount() > 0) {
            long size = node.count();
            if (size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            
            // 生成long类型的数组以存储node中的数据
            long[] array = new long[(int) size];
            
            // 并行地将Node中的元素转存到线性数组中
            new ToArrayTask.OfLong(node, array, 0).invoke();
            
            // 返回线性Node
            return node(array);
        }
        
        // 线性Node，包括存储结构为二维数组的Node
        return node;
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
    // 将当前的Node降维成非树形Node
    public static Node.OfDouble flattenDouble(Node.OfDouble node) {
        // 非线性Node（一般指树形Node）
        if (node.getChildCount() > 0) {
            long size = node.count();
            if (size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            
            // 生成double类型的数组以存储node中的数据
            double[] array = new double[(int) size];
            
            // 并行地将Node中的元素转存到线性数组中
            new ToArrayTask.OfDouble(node, array, 0).invoke();
            
            // 返回线性Node
            return node(array);
        }
        
        // 线性Node，包括存储结构为二维数组的Node
        return node;
    }
    
    /*▲ flatten，并行地给Node降维 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ collect，并行计算/收集元素 ████████████████████████████████████████████████████████████████████████████████┓ */
    
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
    public static <P_IN, P_OUT> Node<P_OUT> collect(PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<P_OUT[]> generator) {
        // 返回输出的元素数量，如果未知或无穷，则返回-1
        long size = helper.exactOutputSizeIfKnown(spliterator);
        
        // 处理元素总量一定，但是子结点数量不确定的Node
        if(size >= 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            if(size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            
            P_OUT[] array = generator.apply((int) size);
            new SizedCollectorTask.OfRef<>(spliterator, helper, array).invoke();
            
            // 返回ArrayNode
            return node(array);
        }
        
        // 处理给定的元素，将计算结果存入树形Node
        Node<P_OUT> node = new CollectorTask.OfRef<>(helper, generator, spliterator).invoke();
        
        // 返回包含计算结果的Node（视需求将Node降维）
        return flattenTree ? flatten(node, generator) : node;
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
    public static <P_IN> Node.OfInt collectInt(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator, boolean flattenTree) {
        // 返回输出的元素数量，如果未知或无穷，则返回-1
        long size = helper.exactOutputSizeIfKnown(spliterator);
        
        // 处理元素总量一定，但是子结点数量不确定的Node
        if(size >= 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            if(size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            
            int[] array = new int[(int) size];
            new SizedCollectorTask.OfInt<>(spliterator, helper, array).invoke();
            
            // 返回ArrayNode
            return node(array);
        }
        
        // 处理给定的元素，将计算结果存入树形Node
        Node.OfInt node = new CollectorTask.OfInt<>(helper, spliterator).invoke();
        
        // 返回包含计算结果的Node（视需求将Node降维）
        return flattenTree ? flattenInt(node) : node;
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
    public static <P_IN> Node.OfLong collectLong(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator, boolean flattenTree) {
        // 返回输出的元素数量，如果未知或无穷，则返回-1
        long size = helper.exactOutputSizeIfKnown(spliterator);
        
        // 处理元素总量一定，但是子结点数量不确定的Node
        if(size >= 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            if(size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            
            long[] array = new long[(int) size];
            new SizedCollectorTask.OfLong<>(spliterator, helper, array).invoke();
            
            // 返回ArrayNode
            return node(array);
        }
        
        // 处理给定的元素，将计算结果存入树形Node
        Node.OfLong node = new CollectorTask.OfLong<>(helper, spliterator).invoke();
        
        // 返回包含计算结果的Node（视需求将Node降维）
        return flattenTree ? flattenLong(node) : node;
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
    public static <P_IN> Node.OfDouble collectDouble(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator, boolean flattenTree) {
        // 返回输出的元素数量，如果未知或无穷，则返回-1
        long size = helper.exactOutputSizeIfKnown(spliterator);
        
        // 处理元素总量一定，但是子结点数量不确定的Node
        if(size >= 0 && spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            if(size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            
            double[] array = new double[(int) size];
            new SizedCollectorTask.OfDouble<>(spliterator, helper, array).invoke();
            
            // 返回ArrayNode
            return node(array);
        }
        
        // 处理给定的元素，将计算结果存入树形Node
        Node.OfDouble node = new CollectorTask.OfDouble<>(helper, spliterator).invoke();
        
        // 返回包含计算结果的Node（视需求将Node降维）
        return flattenTree ? flattenDouble(node) : node;
    }
    
    /*▲ collect，并行计算/收集元素 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * @return an array generator for an array whose elements are of type T.
     */
    // 创建T[]类型的数组，数组长度由参数指定
    @SuppressWarnings("unchecked")
    static <T> IntFunction<T[]> castingArray() {
        return size -> (T[]) new Object[size];
    }
    
    
    
    
    
    
    /*▼ 第(1)类Node 空 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    
    // (1) 空Node，不包含有效数据
    private abstract static class EmptyNode<T, T_ARR, T_CONS>
        implements Node<T> {
        
        EmptyNode() { }
        
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
        
        public void forEach(T_CONS consumer) {
        }
        
        private static class OfRef<T> extends EmptyNode<T, T[], Consumer<? super T>> {
            private OfRef() {
                super();
            }
            
            @Override
            public Spliterator<T> spliterator() {
                return Spliterators.emptySpliterator();
            }
        }
        
        private static final class OfInt
            extends EmptyNode<Integer, int[], IntConsumer>
            implements Node.OfInt {
            
            OfInt() {
                // Avoid creation of special accessor
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
        
        private static final class OfLong
            extends EmptyNode<Long, long[], LongConsumer>
            implements Node.OfLong {
            
            OfLong() {
                // Avoid creation of special accessor
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
        
        private static final class OfDouble
            extends EmptyNode<Double, double[], DoubleConsumer>
            implements Node.OfDouble {
            
            OfDouble() {
                // Avoid creation of special accessor
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
    
    /*▲ 第(1)类Node 空 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 第(2)类Node 封装了数组的Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Node class for a reference array */
    // 【Node子类】封装了数组的Node
    private static class ArrayNode<T> implements Node<T> {
        final T[] array;
        int curSize;    // 数组元素数量
        
        // 新建ArrayNode，内部包含一个长度为size的空数组
        @SuppressWarnings("unchecked")
        ArrayNode(long size, IntFunction<T[]> generator) {
            if(size >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            this.array = generator.apply((int) size);
            this.curSize = 0;
        }
        
        // 从已有的数组新建ArrayNode
        ArrayNode(T[] array) {
            this.array = array;
            this.curSize = array.length;
        }
        
        // 返回描述此ArrayNode中元素的Spliterator（这里直接将内部数组包装到Spliterator后返回）
        @Override
        public Spliterator<T> spliterator() {
            return Arrays.spliterator(array, 0, curSize);
        }
        
        // 将ArrayNode的内容复制到数组dest中（这里直接进行数组拷贝）
        @Override
        public void copyInto(T[] dest, int destOffset) {
            System.arraycopy(array, 0, dest, destOffset, curSize);
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
        
        // 返回ArrayNode中包含的元素数量（这里直接返回数组中元素的个数）
        @Override
        public long count() {
            return curSize;
        }
        
        // 遍历ArrayNode中的元素，并在其上执行Consumer操作
        @Override
        public void forEach(Consumer<? super T> consumer) {
            for(int i = 0; i < curSize; i++) {
                consumer.accept(array[i]);
            }
        }
        
        @Override
        public String toString() {
            return String.format("ArrayNode[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 【Node子类】封装了int[]的Node
    private static class IntArrayNode implements Node.OfInt {
        final int[] array;
        int curSize;
        
        // 新建Node，内部包含一个长度为size的空数组
        IntArrayNode(long size) {
            if (size >= MAX_ARRAY_SIZE)
                throw new IllegalArgumentException(BAD_SIZE);
            this.array = new int[(int) size];
            this.curSize = 0;
        }
        
        // 从已有的数组新建Node
        IntArrayNode(int[] array) {
            this.array = array;
            this.curSize = array.length;
        }
        
        // 返回描述此Node中元素的Spliterator（这里是IntArraySpliterator）
        @Override
        public Spliterator.OfInt spliterator() {
            return Arrays.spliterator(array, 0, curSize);
        }
        
        // 将Node中的元素存入int数组后返回
        @Override
        public int[] asPrimitiveArray() {
            if (array.length == curSize) {
                return array;
            } else {
                return Arrays.copyOf(array, curSize);
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
        
        // 遍历Node中的元素，并在其上执行consumer操作
        @Override
        public void forEach(IntConsumer consumer) {
            for (int i = 0; i < curSize; i++) {
                consumer.accept(array[i]);
            }
        }
        
        @Override
        public String toString() {
            return String.format("IntArrayNode[%d][%s]",
                array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 为【Node子类】封装了long[]的Node
    private static class LongArrayNode implements Node.OfLong {
        final long[] array;
        int curSize;
        
        // 新建Node，内部包含一个长度为size的空数组
        LongArrayNode(long size) {
            if (size >= MAX_ARRAY_SIZE)
                throw new IllegalArgumentException(BAD_SIZE);
            this.array = new long[(int) size];
            this.curSize = 0;
        }
        
        LongArrayNode(long[] array) {
            this.array = array;
            this.curSize = array.length;
        }
        
        // 返回描述此Node中元素的Spliterator（这里是LongArraySpliterator）
        @Override
        public Spliterator.OfLong spliterator() {
            return Arrays.spliterator(array, 0, curSize);
        }
        
        // 将Node中的元素存入long数组后返回
        @Override
        public long[] asPrimitiveArray() {
            if (array.length == curSize) {
                return array;
            } else {
                return Arrays.copyOf(array, curSize);
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
        
        // 遍历Node中的元素，并在其上执行consumer操作
        @Override
        public void forEach(LongConsumer consumer) {
            for (int i = 0; i < curSize; i++) {
                consumer.accept(array[i]);
            }
        }
        
        @Override
        public String toString() {
            return String.format("LongArrayNode[%d][%s]",
                array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 【Node子类】封装了double[]的Node
    private static class DoubleArrayNode implements Node.OfDouble {
        final double[] array;
        int curSize;
        
        // 新建Node，内部包含一个长度为size的空数组
        DoubleArrayNode(long size) {
            if (size >= MAX_ARRAY_SIZE)
                throw new IllegalArgumentException(BAD_SIZE);
            this.array = new double[(int) size];
            this.curSize = 0;
        }
        
        DoubleArrayNode(double[] array) {
            this.array = array;
            this.curSize = array.length;
        }
        
        // 返回描述此Node中元素的Spliterator（这里是DoubleArraySpliterator）
        @Override
        public Spliterator.OfDouble spliterator() {
            return Arrays.spliterator(array, 0, curSize);
        }
        
        // 将Node中的元素存入double数组后返回
        @Override
        public double[] asPrimitiveArray() {
            if (array.length == curSize) {
                return array;
            } else {
                return Arrays.copyOf(array, curSize);
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
        
        // 遍历Node中的元素，并在其上执行consumer操作
        @Override
        public void forEach(DoubleConsumer consumer) {
            for (int i = 0; i < curSize; i++) {
                consumer.accept(array[i]);
            }
        }
        
        @Override
        public String toString() {
            return String.format("DoubleArrayNode[%d][%s]",
                array.length - curSize, Arrays.toString(array));
        }
    }
    
    /*▲ 第(2)类Node 封装了数组的Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 第(3)类Node 固定长度的Node，这些Node本身也是Sink，可以接收元素 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Fixed-sized builder class for reference nodes
     */
    // 【Node/Builder->Sink子类】用在Node长度固定的场景
    private static final class FixedNodeBuilder<T>
        extends ArrayNode<T>
        implements Node.Builder<T> {
        
        // 新建FixedNodeBuilder，内部包含一个长度为size的空数组
        FixedNodeBuilder(long size, IntFunction<T[]> generator) {
            super(size, generator);
            assert size < MAX_ARRAY_SIZE;
        }
        
        // 构建固定长度的Node
        @Override
        public Node<T> build() {
            if(curSize < array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", curSize, array.length));
            }
            return this;
        }
        
        // 激活流
        @Override
        public void begin(long size) {
            if(size != array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", size, array.length));
            }
            curSize = 0;
        }
        
        // 向ArrayNode存入一个元素
        @Override
        public void accept(T t) {
            if(curSize < array.length) {
                array[curSize++] = t;
            } else {
                throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", array.length));
            }
        }
        
        // 关闭流
        @Override
        public void end() {
            if(curSize < array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", curSize, array.length));
            }
        }
        
        @Override
        public String toString() {
            return String.format("FixedNodeBuilder[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 【Node/Builder->Sink子类】为int类型特化，用在Node长度固定的场景
    private static final class IntFixedNodeBuilder
        extends IntArrayNode
        implements Node.Builder.OfInt {
        
        IntFixedNodeBuilder(long size) {
            super(size);
            assert size < MAX_ARRAY_SIZE;
        }
        
        // 构建固定长度的Node
        @Override
        public Node.OfInt build() {
            if(curSize < array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", curSize, array.length));
            }
            
            return this;
        }
        
        // 激活流
        @Override
        public void begin(long size) {
            if(size != array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", size, array.length));
            }
            
            curSize = 0;
        }
        
        // 向ArrayNode存入一个元素
        @Override
        public void accept(int i) {
            if(curSize < array.length) {
                array[curSize++] = i;
            } else {
                throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", array.length));
            }
        }
        
        // 关闭流
        @Override
        public void end() {
            if(curSize < array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", curSize, array.length));
            }
        }
        
        @Override
        public String toString() {
            return String.format("IntFixedNodeBuilder[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 【Node/Builder->Sink子类】为long类型特化，用在Node长度固定的场景
    private static final class LongFixedNodeBuilder
        extends LongArrayNode
        implements Node.Builder.OfLong {
        
        LongFixedNodeBuilder(long size) {
            super(size);
            assert size < MAX_ARRAY_SIZE;
        }
        
        // 构建固定长度的Node
        @Override
        public Node.OfLong build() {
            if(curSize < array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", curSize, array.length));
            }
            
            return this;
        }
        
        // 激活流
        @Override
        public void begin(long size) {
            if(size != array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", size, array.length));
            }
            
            curSize = 0;
        }
        
        // 向ArrayNode存入一个元素
        @Override
        public void accept(long i) {
            if(curSize < array.length) {
                array[curSize++] = i;
            } else {
                throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", array.length));
            }
        }
        
        // 关闭流
        @Override
        public void end() {
            if(curSize < array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", curSize, array.length));
            }
        }
        
        @Override
        public String toString() {
            return String.format("LongFixedNodeBuilder[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    // 【Node/Builder->Sink子类】为double类型特化，用在Node长度固定的场景
    private static final class DoubleFixedNodeBuilder
        extends DoubleArrayNode
        implements Node.Builder.OfDouble {
        
        DoubleFixedNodeBuilder(long size) {
            super(size);
            assert size < MAX_ARRAY_SIZE;
        }
        
        // 构建固定长度的Node
        @Override
        public Node.OfDouble build() {
            if(curSize < array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", curSize, array.length));
            }
            
            return this;
        }
        
        // 激活流
        @Override
        public void begin(long size) {
            if(size != array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", size, array.length));
            }
            
            curSize = 0;
        }
        
        // 向ArrayNode存入一个元素
        @Override
        public void accept(double i) {
            if(curSize < array.length) {
                array[curSize++] = i;
            } else {
                throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", array.length));
            }
        }
        
        // 关闭流
        @Override
        public void end() {
            if(curSize < array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", curSize, array.length));
            }
        }
        
        @Override
        public String toString() {
            return String.format("DoubleFixedNodeBuilder[%d][%s]", array.length - curSize, Arrays.toString(array));
        }
    }
    
    /*▲ 第(3)类Node 固定长度的Node，这些Node本身也是Sink，可以接收元素 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 第(4)类Node 可变长度的Node，这些Node本身也是Sink，可以接收元素 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Variable-sized builder class for reference nodes
     */
    // 【Node/Builder->Sink/SpinedBuffer子类】用在Node长度可变的场景
    private static final class SpinedNodeBuilder<T>
        extends SpinedBuffer<T>
        implements Node<T>, Node.Builder<T> {
        
        private boolean building = false;   // 当前的流是否已被激活
        
        SpinedNodeBuilder() {
        } // Avoid creation of special accessor
        
        // 激活流
        @Override
        public void begin(long size) {
            assert !building : "was already building";
            building = true;
            clear();                // 清空SpinedBuffer
            ensureCapacity(size);   // 传入需要的容量，确保SpinedBuffer容量充足，不够的话就分配
        }
        
        // 向SpinedBuffer存入一个元素
        @Override
        public void accept(T t) {
            assert building : "not building";
            super.accept(t);
        }
        
        // 关闭流
        @Override
        public void end() {
            assert building : "was not building";
            building = false;
            // @@@ check begin(size) and size
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
    
    // 【Node/Builder->Sink/SpinedBuffer子类】为int类型特化，用在Node长度可变的场景
    private static final class IntSpinedNodeBuilder
        extends SpinedBuffer.OfInt
        implements Node.OfInt, Node.Builder.OfInt {
        private boolean building = false;
        
        IntSpinedNodeBuilder() {
        } // Avoid creation of special accessor
        
        // 激活流
        @Override
        public void begin(long size) {
            assert !building : "was already building";
            building = true;
            clear();
            ensureCapacity(size);
        }
        
        // 向SpinedBuffer存入一个元素
        @Override
        public void accept(int i) {
            assert building : "not building";
            super.accept(i);
        }
        
        // 关闭流
        @Override
        public void end() {
            assert building : "was not building";
            building = false;
            // @@@ check begin(size) and size
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
    
    // 【Node/Builder->Sink/SpinedBuffer子类】为long类型特化，用在Node长度可变的场景
    private static final class LongSpinedNodeBuilder
        extends SpinedBuffer.OfLong
        implements Node.OfLong, Node.Builder.OfLong {
        private boolean building = false;
        
        LongSpinedNodeBuilder() {
        } // Avoid creation of special accessor
        
        // 激活流
        @Override
        public void begin(long size) {
            assert !building : "was already building";
            building = true;
            clear();
            ensureCapacity(size);
        }
        
        // 向SpinedBuffer存入一个元素
        @Override
        public void accept(long i) {
            assert building : "not building";
            super.accept(i);
        }
        
        // 关闭流
        @Override
        public void end() {
            assert building : "was not building";
            building = false;
            // @@@ check begin(size) and size
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
    
    // 【Node/Builder->Sink/SpinedBuffer子类】为double类型特化，用在Node长度可变的场景
    private static final class DoubleSpinedNodeBuilder
        extends SpinedBuffer.OfDouble
        implements Node.OfDouble, Node.Builder.OfDouble {
        private boolean building = false;
        
        DoubleSpinedNodeBuilder() {
        } // Avoid creation of special accessor
        
        // 激活流
        @Override
        public void begin(long size) {
            assert !building : "was already building";
            building = true;
            clear();
            ensureCapacity(size);
        }
        
        // 向SpinedBuffer存入一个元素
        @Override
        public void accept(double i) {
            assert building : "not building";
            super.accept(i);
        }
        
        // 关闭流
        @Override
        public void end() {
            assert building : "was not building";
            building = false;
            // @@@ check begin(size) and size
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
    
    /*▲ 第(4)类Node 可变长度的Node，这些Node本身也是Sink，可以接收元素 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 第(5)类Node 包含了Collection的Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Node class for a Collection */
    private static final class CollectionNode<T> implements Node<T> {
        private final Collection<T> c;
        
        CollectionNode(Collection<T> c) {
            this.c = c;
        }
        
        @Override
        public Spliterator<T> spliterator() {
            return c.stream().spliterator();
        }
        
        @Override
        public void copyInto(T[] array, int offset) {
            for (T t : c)
                array[offset++] = t;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public T[] asArray(IntFunction<T[]> generator) {
            return c.toArray(generator.apply(c.size()));
        }
        
        @Override
        public long count() {
            return c.size();
        }
        
        @Override
        public void forEach(Consumer<? super T> consumer) {
            c.forEach(consumer);
        }
        
        @Override
        public String toString() {
            return String.format("CollectionNode[%d][%s]", c.size(), c);
        }
    }
    
    /*▲ 第(5)类Node 包含了Collection的Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 第(6)类Node 可串联Node ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Node class for an internal node with two or more children
     */
    // 【Node子类】可串联Node的抽象基类（可以串联成一棵树）
    private abstract static class AbstractConcNode<T, T_NODE extends Node<T>> implements Node<T> {
        // 包含左右两个子Node
        protected final T_NODE left;
        protected final T_NODE right;
        private final long size;        // 可串联Node中包含的元素数量
        
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
        
        // 返回子Node数量
        @Override
        public int getChildCount() {
            return 2;
        }
        
        // 返回指定索引处的子Node
        @Override
        public T_NODE getChild(int i) {
            if(i == 0) {
                return left;
            }
            
            if(i == 1) {
                return right;
            }
            
            throw new IndexOutOfBoundsException();
        }
        
        // 返回可串联Node中包含的元素数量
        @Override
        public long count() {
            return size;
        }
    }
    
    // 【Node子类】可串联Node
    static final class ConcNode<T>
        extends AbstractConcNode<T, Node<T>>
        implements Node<T> {
        
        ConcNode(Node<T> left, Node<T> right) {
            super(left, right);
        }
        
        // 返回子Node的Spliterator
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
        
        // 从当前Node生成一个子Node返回（把两个子Node看成整体去考虑）
        @Override
        public Node<T> truncate(long from, long to, IntFunction<T[]> generator) {
            if(from == 0 && to == count()) {
                return this;
            }
            
            long leftCount = left.count();
            
            if(from >= leftCount) {
                return right.truncate(from - leftCount, to - leftCount, generator);
            }
            
            if(to <= leftCount) {
                return left.truncate(from, to, generator);
            }
            
            return Nodes.conc(getShape(), left.truncate(from, leftCount, generator), right.truncate(0, to - leftCount, generator));
        }
        
        @Override
        public String toString() {
            if(count() < 32) {
                return String.format("ConcNode[%s.%s]", left, right);
            } else {
                return String.format("ConcNode[size=%d]", count());
            }
        }
        
        // 为基本类型特化的可串联Node
        private abstract static class OfPrimitive<E, T_CONS, T_ARR,
            T_SPLITR extends Spliterator.OfPrimitive<E, T_CONS, T_SPLITR>,
            T_NODE extends Node.OfPrimitive<E, T_CONS, T_ARR, T_SPLITR, T_NODE>>
            extends AbstractConcNode<E, T_NODE>
            implements Node.OfPrimitive<E, T_CONS, T_ARR, T_SPLITR, T_NODE> {
            
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
                if(count() < 32) {
                    return String.format("%s[%s.%s]", this.getClass().getName(), left, right);
                }
                
                return String.format("%s[size=%d]", this.getClass().getName(), count());
            }
        }
        
        // 为int类型特化的可串联Node
        static final class OfInt
            extends ConcNode.OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, Node.OfInt>
            implements Node.OfInt {
            
            OfInt(Node.OfInt left, Node.OfInt right) {
                super(left, right);
            }
            
            // 返回描述此Node中元素的Spliterator
            @Override
            public Spliterator.OfInt spliterator() {
                return new InternalNodeSpliterator.OfInt(this);
            }
        }
        
        // 为long类型特化的可串联Node
        static final class OfLong
            extends ConcNode.OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, Node.OfLong>
            implements Node.OfLong {
            
            OfLong(Node.OfLong left, Node.OfLong right) {
                super(left, right);
            }
            
            // 返回描述此Node中元素的Spliterator
            @Override
            public Spliterator.OfLong spliterator() {
                return new InternalNodeSpliterator.OfLong(this);
            }
        }
        
        // 为double类型特化的可串联Node
        static final class OfDouble
            extends ConcNode.OfPrimitive<Double, DoubleConsumer, double[], Spliterator.OfDouble, Node.OfDouble>
            implements Node.OfDouble {
            
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
    
    /*▲ 第(6)类Node 可串联Node ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 专用Spliterator ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Abstract class for spliterator for all internal node classes */
    // 【专用Spliterator】用于第(6)类Node的子Node
    private abstract static class InternalNodeSpliterator<T, S extends Spliterator<T>, N extends Node<T>>
        implements Spliterator<T> {
        // Node we are pointing to null if full traversal has occurred
        N curNode;  // 当前Node
        
        // next child of curNode to consume
        int curChildIndex;  // curNode的子Node索引（0或1）
        
        // The spliterator of the curNode if that node is last and has no children.
        // This spliterator will be delegated to for splitting and traversing.
        // null if curNode has children
        S lastNodeSpliterator;
        
        // spliterator used while traversing with tryAdvance
        // null if no partial traversal has occurred
        S tryAdvanceSpliterator;
        
        // node stack used when traversing to search and find leaf nodes
        // null if no partial traversal has occurred
        Deque<N> tryAdvanceStack;   // 双端队列，存储curNode的子Node
        
        InternalNodeSpliterator(N curNode) {
            this.curNode = curNode;
        }
        
        // 从容器的指定范围切割一段元素，将其打包到Spliterator后返回，特征值不变
        @Override
        @SuppressWarnings("unchecked")
        public final S trySplit() {
            // 已经全部遍历完，或者遍历了一部分，则无法分割
            if(curNode == null || tryAdvanceSpliterator != null) {
                return null; // Cannot split if fully or partially traversed
            }
            
            if(lastNodeSpliterator != null) {
                return (S) lastNodeSpliterator.trySplit();
            }
            
            // 左孩子
            if(curChildIndex < curNode.getChildCount() - 1) {
                return (S) curNode.getChild(curChildIndex++).spliterator();
            }
            
            curNode = (N) curNode.getChild(curChildIndex);
            if(curNode.getChildCount() == 0) {
                lastNodeSpliterator = (S) curNode.spliterator();
                return (S) lastNodeSpliterator.trySplit();
            }
            
            curChildIndex = 0;
            return (S) curNode.getChild(curChildIndex++).spliterator();
        }
        
        // 返回容器容量的估算值
        @Override
        public final long estimateSize() {
            if(curNode == null)
                return 0;
            
            // Will not reflect the effects of partial traversal.
            // This is compliant with the specification
            if(lastNodeSpliterator != null)
                return lastNodeSpliterator.estimateSize();
            
            long size = 0;
            for(int i = curChildIndex; i < curNode.getChildCount(); i++) {
                size += curNode.getChild(i).count();
            }
            return size;
        }
        
        @Override
        public final int characteristics() {
            return Spliterator.SIZED;
        }
        
        /**
         * Initiate a stack containing, in left-to-right order, the child nodes covered by this spliterator
         */
        // 将子Node存入双端队列，队头是左Node，队尾是右Node
        @SuppressWarnings("unchecked")
        protected final Deque<N> initStack() {
            // Bias size to the case where leaf nodes are close to this node 8 is the minimum initial capacity for the ArrayDeque implementation
            Deque<N> stack = new ArrayDeque<>(8);
            for(int i = curNode.getChildCount() - 1; i >= curChildIndex; i--) {
                stack.addFirst((N) curNode.getChild(i));
            }
            return stack;
        }
        
        /**
         * Depth first search, in left-to-right order, of the node tree, using an explicit stack, to find the next non-empty leaf node.
         */
        // 深度优先遍历Node树，直到找到一个非空的叶子节点（该Node包含有效元素）
        @SuppressWarnings("unchecked")
        protected final N findNextLeafNode(Deque<N> stack) {
            N n;
            // 一边找以便删除
            while((n = stack.pollFirst()) != null) {
                if(n.getChildCount() == 0) {    // 该Node不存在子Node
                    if(n.count() > 0) {
                        return n;
                    }
                } else {
                    for(int i = n.getChildCount() - 1; i >= 0; i--) {
                        stack.addFirst((N) n.getChild(i));
                    }
                }
            }
            
            return null;
        }
        
        @SuppressWarnings("unchecked")
        protected final boolean initTryAdvance() {
            if(curNode == null) {
                return false;
            }
            
            if(tryAdvanceSpliterator == null) {
                if(lastNodeSpliterator == null) {
                    // 初始化存放子Node的双端队列
                    tryAdvanceStack = initStack();
                    // 在双端队列中查找非空叶子Node
                    N leaf = findNextLeafNode(tryAdvanceStack);
                    // 找到了非空叶子Node
                    if(leaf != null) {
                        // 存储此Node的Spliterator
                        tryAdvanceSpliterator = (S) leaf.spliterator();
                    } else {
                        // A non-empty leaf node was not found
                        // No elements to traverse
                        curNode = null; // 找不到非空的叶子节点，不需要遍历
                        return false;
                    }
                } else {
                    tryAdvanceSpliterator = lastNodeSpliterator;
                }
            }
            
            return true;
        }
        
        private static final class OfRef<T>
            extends InternalNodeSpliterator<T, Spliterator<T>, Node<T>> {
            
            OfRef(Node<T> curNode) {
                super(curNode);
            }
            
            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                if(!initTryAdvance()) {
                    return false;
                }
                
                boolean hasNext = tryAdvanceSpliterator.tryAdvance(consumer);
                if(!hasNext) {
                    if(lastNodeSpliterator == null) {
                        // Advance to the spliterator of the next non-empty leaf node
                        Node<T> leaf = findNextLeafNode(tryAdvanceStack);
                        if(leaf != null) {
                            tryAdvanceSpliterator = leaf.spliterator();
                            // Since the node is not-empty the spliterator can be advanced
                            return tryAdvanceSpliterator.tryAdvance(consumer);
                        }
                    }
                    
                    // No more elements to traverse
                    curNode = null;
                }
                
                return hasNext;
            }
            
            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                if(curNode == null) {
                    return;
                }
                
                if(tryAdvanceSpliterator == null) {
                    if(lastNodeSpliterator == null) {
                        Deque<Node<T>> stack = initStack();
                        Node<T> leaf;
                        while((leaf = findNextLeafNode(stack)) != null) {
                            leaf.forEach(consumer);
                        }
                        curNode = null;
                    } else {
                        lastNodeSpliterator.forEachRemaining(consumer);
                    }
                } else {
                    while(tryAdvance(consumer)) {
                    }
                }
            }
        }
        
        private abstract static class OfPrimitive<T, T_CONS, T_ARR,
            T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>,
            N extends Node.OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, N>>
            extends InternalNodeSpliterator<T, T_SPLITR, N>
            implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            
            OfPrimitive(N cur) {
                super(cur);
            }
            
            @Override
            public boolean tryAdvance(T_CONS consumer) {
                if(!initTryAdvance()) {
                    return false;
                }
                
                boolean hasNext = tryAdvanceSpliterator.tryAdvance(consumer);
                if(!hasNext) {
                    if(lastNodeSpliterator == null) {
                        // Advance to the spliterator of the next non-empty leaf node
                        N leaf = findNextLeafNode(tryAdvanceStack);
                        if(leaf != null) {
                            tryAdvanceSpliterator = leaf.spliterator();
                            // Since the node is not-empty the spliterator can be advanced
                            return tryAdvanceSpliterator.tryAdvance(consumer);
                        }
                    }
                    // No more elements to traverse
                    curNode = null;
                }
                return hasNext;
            }
            
            @Override
            public void forEachRemaining(T_CONS consumer) {
                if(curNode == null) {
                    return;
                }
                
                if(tryAdvanceSpliterator == null) {
                    if(lastNodeSpliterator == null) {
                        Deque<N> stack = initStack();
                        N leaf;
                        while((leaf = findNextLeafNode(stack)) != null) {
                            leaf.forEach(consumer);
                        }
                        curNode = null;
                    } else {
                        lastNodeSpliterator.forEachRemaining(consumer);
                    }
                } else {
                    while(tryAdvance(consumer)) {
                    }
                }
            }
        }
        
        private static final class OfInt
            extends OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, Node.OfInt>
            implements Spliterator.OfInt {
            
            OfInt(Node.OfInt cur) {
                super(cur);
            }
        }
        
        private static final class OfLong
            extends OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, Node.OfLong>
            implements Spliterator.OfLong {
            
            OfLong(Node.OfLong cur) {
                super(cur);
            }
        }
        
        private static final class OfDouble
            extends OfPrimitive<Double, DoubleConsumer, double[], Spliterator.OfDouble, Node.OfDouble>
            implements Spliterator.OfDouble {
            
            OfDouble(Node.OfDouble cur) {
                super(cur);
            }
        }
    }
    
    /*▲ 专用Spliterator ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 专用Task ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 专用任务：用于将Node中的元素转存到线性数组中（可用于降维）
     * 如果是普通的Node（不包含子Node），直接将其内容拷贝到数组中返回
     * 如果是树形Node（包含子Node），则并行地从Node中拷贝内容
     */
    @SuppressWarnings("serial")
    private abstract static class ToArrayTask<T, T_NODE extends Node<T>, K extends ToArrayTask<T, T_NODE, K>>
        extends CountedCompleter<Void> {
        
        protected final T_NODE node;
        protected final int offset;
        
        ToArrayTask(T_NODE node, int offset) {
            this.node = node;
            this.offset = offset;
        }
        
        ToArrayTask(K parent, T_NODE node, int offset) {
            super(parent);
            this.node = node;
            this.offset = offset;
        }
        
        // 将当前Node中的内容复制到数组中
        abstract void copyNodeToArray();
        
        // 将当前Node的第childIndex个子结点包装为子任务，以待处理
        abstract K makeChild(int childIndex, int offset);
        
        // 将Node中的元素转存到线性数组中
        @Override
        public void compute() {
            ToArrayTask<T, T_NODE, K> task = this;
            
            while(true) {
                // 返回子Node数量
                int count = task.node.getChildCount();
                
                // 如果已经没有子Node
                if(count == 0) {
                    // 将当前Node中的内容复制到数组中
                    task.copyNodeToArray();
                    // 将当前任务标记为[已完成]，并将父任务的挂起计数减一
                    task.propagateCompletion();
                    return;
                }
                
                // 当前任务存在子Node的情形下，需要设置挂起次数
                task.setPendingCount(count - 1);
                
                int size = 0;
                int i = 0;
                
                // 由其他线程处理其余count-1个叶子结点
                while(i < count-1){
                    K leftTask = task.makeChild(i, task.offset + size);
                    // 累加各Node中包含的元素数量
                    size += leftTask.node.count();
                    // 处理叶子结点
                    leftTask.fork();
                    i++;
                }
                
                // 由当前线程处理最后一个叶子结点
                task = task.makeChild(i, task.offset + size);
            }
        }
        
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
            
            // 将当前Node的第childIndex个子结点包装为子任务，以待处理
            @Override
            OfRef<T> makeChild(int childIndex, int offset) {
                return new OfRef<>(this, node.getChild(childIndex), offset);
            }
            
            // 将当前Node中的内容复制到数组中
            @Override
            void copyNodeToArray() {
                // 将Node的内容复制到数组array中offset偏移处
                node.copyInto(array, offset);
            }
        }
        
        @SuppressWarnings("serial")
        private static class OfPrimitive<T, T_CONS, T_ARR,
            T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>,
            T_NODE extends Node.OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE>>
            extends ToArrayTask<T, T_NODE, OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE>> {
            
            private final T_ARR array;
            
            private OfPrimitive(T_NODE node, T_ARR array, int offset) {
                super(node, offset);
                this.array = array;
            }
            
            private OfPrimitive(OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE> parent, T_NODE node, int offset) {
                super(parent, node, offset);
                this.array = parent.array;
            }
            
            // 将当前Node的第childIndex个子结点包装为子任务，以待处理
            @Override
            OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE> makeChild(int childIndex, int offset) {
                return new OfPrimitive<>(this, node.getChild(childIndex), offset);
            }
            
            // 将当前Node中的内容复制到数组中
            @Override
            void copyNodeToArray() {
                // 将Node的内容复制到数组array中offset偏移处
                node.copyInto(array, offset);
            }
        }
        
        @SuppressWarnings("serial")
        private static final class OfInt extends OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, Node.OfInt> {
            private OfInt(Node.OfInt node, int[] array, int offset) {
                super(node, array, offset);
            }
        }
        
        @SuppressWarnings("serial")
        private static final class OfLong extends OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, Node.OfLong> {
            private OfLong(Node.OfLong node, long[] array, int offset) {
                super(node, array, offset);
            }
        }
        
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
    // 专用任务：用于处理元素总量一定，但是子结点数量不确定的Node
    @SuppressWarnings("serial")
    private abstract static class SizedCollectorTask<P_IN, P_OUT, T_SINK extends Sink<P_OUT>, K extends SizedCollectorTask<P_IN, P_OUT, T_SINK, K>>
        extends CountedCompleter<Void>
        implements Sink<P_OUT> {
        
        protected final Spliterator<P_IN> spliterator;
        protected final PipelineHelper<P_OUT> helper;
        protected final long targetSize;
        protected long offset;
        protected long length;
        // For Sink implementation
        protected int index, fence;
        
        SizedCollectorTask(Spliterator<P_IN> spliterator, PipelineHelper<P_OUT> helper, int arrayLength) {
            assert spliterator.hasCharacteristics(Spliterator.SUBSIZED);
            this.spliterator = spliterator;
            this.helper = helper;
            this.targetSize = AbstractTask.suggestTargetSize(spliterator.estimateSize());
            this.offset = 0;
            this.length = arrayLength;
        }
        
        SizedCollectorTask(K parent, Spliterator<P_IN> spliterator, long offset, long length, int arrayLength) {
            super(parent);
            assert spliterator.hasCharacteristics(Spliterator.SUBSIZED);
            this.spliterator = spliterator;
            this.helper = parent.helper;
            this.targetSize = parent.targetSize;
            this.offset = offset;
            this.length = length;
            
            if(offset < 0 || length < 0 || (offset + length - 1 >= arrayLength)) {
                throw new IllegalArgumentException(
                    String.format("offset and length interval [%d, %d + %d) is not within array size interval [0, %d)", offset, offset, length, arrayLength)
                );
            }
        }
        
        // 包装为子任务，以待处理
        abstract K makeChild(Spliterator<P_IN> spliterator, long offset, long size);
        
        @Override
        public void compute() {
            SizedCollectorTask<P_IN, P_OUT, T_SINK, K> task = this;
            
            // 将当前任务安排为right任务
            Spliterator<P_IN> rightSplit = spliterator;
            Spliterator<P_IN> leftSplit;
            
            // 从当前任务中切分一部分作为子任务
            while(rightSplit.estimateSize() > task.targetSize && (leftSplit = rightSplit.trySplit()) != null) {
                // 设置一个挂起计数
                task.setPendingCount(1);
                
                // 用其他线程处理切分出的子任务
                long leftSplitSize = leftSplit.estimateSize();
                task.makeChild(leftSplit, task.offset, leftSplitSize).fork();
                
                // 更细当前任务
                task = task.makeChild(rightSplit, task.offset + leftSplitSize, task.length - leftSplitSize);
            }
            
            assert task.offset + task.length < MAX_ARRAY_SIZE;
            
            @SuppressWarnings("unchecked")
            T_SINK sink = (T_SINK) task;
            
            // 当前线程处理当前任务（从后往前包装sink的同时，从前到后择取数据）
            task.helper.wrapAndCopyInto(sink, rightSplit);
            
            // 将当前任务标记为[已完成]，并将父任务的挂起计数减一
            task.propagateCompletion();
        }
        
        @Override
        public void begin(long size) {
            if(size > length) {
                throw new IllegalStateException("size passed to Sink.begin exceeds array length");
            }
            /*
             * Casts to int are safe since absolute size is verified to be within bounds
             * when the root concrete SizedCollectorTask is constructed with the shared array
             */
            index = (int) offset;
            fence = index + (int) length;
        }
        
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
            
            @Override
            public void accept(P_OUT value) {
                if(index >= fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(index));
                }
                array[index++] = value;
            }
            
            // 包装为子任务，以待处理
            @Override
            OfRef<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new OfRef<>(this, spliterator, offset, size);
            }
        }
        
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
            
            @Override
            public void accept(int value) {
                if(index >= fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(index));
                }
                array[index++] = value;
            }
            
            // 包装为子任务，以待处理
            @Override
            SizedCollectorTask.OfInt<P_IN> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new SizedCollectorTask.OfInt<>(this, spliterator, offset, size);
            }
        }
        
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
            
            @Override
            public void accept(long value) {
                if(index >= fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(index));
                }
                array[index++] = value;
            }
            
            // 包装为子任务，以待处理
            @Override
            SizedCollectorTask.OfLong<P_IN> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new SizedCollectorTask.OfLong<>(this, spliterator, offset, size);
            }
        }
        
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
            
            @Override
            public void accept(double value) {
                if(index >= fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(index));
                }
                array[index++] = value;
            }
            
            // 包装为子任务，以待处理
            @Override
            SizedCollectorTask.OfDouble<P_IN> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new SizedCollectorTask.OfDouble<>(this, spliterator, offset, size);
            }
        }
    }
    
    // 专用任务：将大任务拆分为树形的小任务去执行
    @SuppressWarnings("serial")
    private static class CollectorTask<P_IN, P_OUT, T_NODE extends Node<P_OUT>, T_BUILDER extends Node.Builder<P_OUT>>
        extends AbstractTask<P_IN, P_OUT, T_NODE, CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER>> {
        
        protected final PipelineHelper<P_OUT> helper;
        protected final LongFunction<T_BUILDER> builderFactory;
        protected final BinaryOperator<T_NODE> concFactory;
        
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
        
        // 任务结束时设置计算结果
        @Override
        public void onCompletion(CountedCompleter<?> caller) {
            if(!isLeaf()) {
                setLocalResult(concFactory.apply(leftChild.getLocalResult(), rightChild.getLocalResult()));
            }
            
            super.onCompletion(caller);
        }
        
        // 包装为子任务，以待处理
        @Override
        protected CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER> makeChild(Spliterator<P_IN> spliterator) {
            return new CollectorTask<>(this, spliterator);
        }
        
        // 计算当前任务，将计算结果包装到Node中返回
        @Override
        @SuppressWarnings("unchecked")
        protected T_NODE doLeaf() {
            T_BUILDER builder = builderFactory.apply(helper.exactOutputSizeIfKnown(spliterator));
            return (T_NODE) helper.wrapAndCopyInto(builder, spliterator).build();
        }
        
        @SuppressWarnings("serial")
        private static final class OfRef<P_IN, P_OUT> extends CollectorTask<P_IN, P_OUT, Node<P_OUT>, Node.Builder<P_OUT>> {
            OfRef(PipelineHelper<P_OUT> helper, IntFunction<P_OUT[]> generator, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, s -> builder(s, generator), ConcNode::new);
            }
        }
        
        @SuppressWarnings("serial")
        private static final class OfInt<P_IN> extends CollectorTask<P_IN, Integer, Node.OfInt, Node.Builder.OfInt> {
            OfInt(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, Nodes::intBuilder, ConcNode.OfInt::new);
            }
        }
        
        @SuppressWarnings("serial")
        private static final class OfLong<P_IN> extends CollectorTask<P_IN, Long, Node.OfLong, Node.Builder.OfLong> {
            OfLong(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, Nodes::longBuilder, ConcNode.OfLong::new);
            }
        }
        
        @SuppressWarnings("serial")
        private static final class OfDouble<P_IN> extends CollectorTask<P_IN, Double, Node.OfDouble, Node.Builder.OfDouble> {
            OfDouble(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, Nodes::doubleBuilder, ConcNode.OfDouble::new);
            }
        }
    }
    
    /*▲ 专用Task ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
