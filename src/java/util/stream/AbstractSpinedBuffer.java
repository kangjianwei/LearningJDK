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

/**
 * Base class for a data structure for gathering elements into a buffer and then
 * iterating them. Maintains an array of increasingly sized arrays, so there is
 * no copying cost associated with growing the data structure.
 *
 * @since 1.8
 */

/*
 * 容量可变的缓冲区抽象基类，使用chunk作为存储结构
 *
 * 一个chunk往往是一个一维数组
 * 多个chunk组成一个二维数组
 *
 * 该抽象基类主要提供了控制chunk容量的参数
 */
abstract class AbstractSpinedBuffer {
    /**
     * Minimum power-of-two for the first chunk.
     */
    /*
     * 第一个chunk的初始容量系数
     * 比如MIN_CHUNK_POWER = 4，则为第一个chunk分配1<<4的容量
     */
    public static final int MIN_CHUNK_POWER = 4;
    
    /**
     * Minimum size for the first chunk.
     */
    // 第一个chunk的容量
    public static final int MIN_CHUNK_SIZE = 1 << MIN_CHUNK_POWER;
    
    /**
     * Max power-of-two for chunks.
     */
    // 分配chunk时的最大容量系数
    public static final int MAX_CHUNK_POWER = 30;
    
    /**
     * Minimum array size for array-of-chunks.
     */
    /*
     * 缓存chunk的数组的最小容量
     * 注：一个chunk往往是一个数组，所以缓存chunk的数组往往是二维数组
     * 因此，这个容量往往是某个二维数组的行数
     */
    public static final int MIN_SPINE_SIZE = 8;
    
    /**
     * log2 of the size of the first chunk.
     */
    /*
     * 新建一个chunk时使用的容量系数
     * 初始时，该系数等于MIN_CHUNK_POWER，后续会增大
     * 初始化时，该系数不超过32
     * 扩容过程中，该系数不超过MAX_CHUNK_POWER，即不超过30
     */
    protected final int initialChunkPower;
    
    /**
     * Index of the *next* element to write; may point into, or just outside of,
     * the current chunk.
     */
    // 指向当前chunk中的元素索引（往往是一维数组的列索引）
    protected int elementIndex;
    
    /**
     * Index of the *current* chunk in the spine array, if the spine array is
     * non-null.
     */
    // 指向chunk的索引（往往是二维数组的行索引）
    protected int spineIndex;
    
    /**
     * Count of elements in all prior chunks.
     */
    /*
     * 记录当前Chunk之前已经存可多少个元素
     * 比如priorElementCount[0]总是等于0，代表索引为0的chunk之前没有元素
     * priorElementCount[5]=n代表索引为5的chunk之前有n个元素
     */
    protected long[] priorElementCount;
    
    /**
     * Construct with an initial capacity of 16.
     */
    protected AbstractSpinedBuffer() {
        this.initialChunkPower = MIN_CHUNK_POWER;
    }
    
    /**
     * Construct with a specified initial capacity.
     *
     * @param initialCapacity The minimum expected number of elements
     */
    protected AbstractSpinedBuffer(int initialCapacity) {
        if(initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        
        /*
         * Integer.numberOfLeadingZeros()
         * 返回整数i的二进制位左边连续的0的个数，范围在0~32
         * 这里，initialChunkPower范围也在0~32
         */
        this.initialChunkPower = Math.max(MIN_CHUNK_POWER, Integer.SIZE - Integer.numberOfLeadingZeros(initialCapacity - 1));
    }
    
    /**
     * Remove all data from the buffer
     */
    // 清空SpinedBuffer
    public abstract void clear();
    
    /**
     * Is the buffer currently empty?
     */
    // 判断是否存在元素
    public boolean isEmpty() {
        return (spineIndex == 0) && (elementIndex == 0);
    }
    
    /**
     * How many elements are currently in the buffer?
     */
    // 返回所有chunk中已存入的元素个数
    public long count() {
        return (spineIndex == 0)
            ? elementIndex
            : priorElementCount[spineIndex] + elementIndex;
    }
    
    /**
     * How big should the nth chunk be?
     */
    // 返回即将分配的chunk应当包含的元素个数
    protected int chunkSize(int n) {
        int power = (n == 0 || n == 1)
            ? initialChunkPower
            : Math.min(initialChunkPower + n - 1, AbstractSpinedBuffer.MAX_CHUNK_POWER);
        return 1 << power;
    }
}
