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
 * 弹性缓冲区的抽象实现
 *
 * 弹性缓冲区是一个二维数组，该二维数组又由多个一维数组(chunk)组成。
 * 当前抽象类主要提供了控制弹性缓冲区中一维数组和二维数组的容量的参数。
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
     * Max power-of-two for chunks.
     */
    // chunk的最大容量系数
    public static final int MAX_CHUNK_POWER = 30;
    
    /**
     * Minimum size for the first chunk.
     */
    // 第一个chunk的最小容量
    public static final int MIN_CHUNK_SIZE = 1 << MIN_CHUNK_POWER;
    
    /**
     * Minimum array size for array-of-chunks.
     */
    /*
     * 弹性缓冲区的最小容量
     *
     *
     */
    public static final int MIN_SPINE_SIZE = 8;
    
    /**
     * log2 of the size of the first chunk.
     */
    /*
     * 新建一个chunk时使用的容量系数
     *
     * 初始时，该系数等于MIN_CHUNK_POWER，后续会增大。
     */
    protected final int initialChunkPower;
    
    /**
     * Index of the *next* element to write; may point into, or just outside of, the current chunk.
     */
    // 追踪当前chunk中的元素的索引，用来指示当前的一维缓存是否已满
    protected int elementIndex;
    
    /**
     * Index of the *current* chunk in the spine array, if the spine array is non-null.
     */
    // 二维缓存的行索引，指向当前chunk
    protected int spineIndex;
    
    /**
     * Count of elements in all prior chunks.
     */
    /*
     * 记录当前chunk之前已经存储了多少个元素
     *
     * 比如priorElementCount[0]总是等于0，代表二维缓冲区索引为0的chunk之前没有元素；
     * priorElementCount[1]=n代表二维缓冲区索引为1的chunk之前已经存储了n个元素。
     *
     * 设置此变量的必要性在于二维缓冲区是一个参差数组，即二维缓冲区的每一行包含的元素并不相等。
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
        if(initialCapacity<0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        
        /*
         * Integer.numberOfLeadingZeros()
         * 返回整数i的二进制位左边连续的0的个数，范围在0~32
         * 这里，initialChunkPower范围也在0~32
         */
        this.initialChunkPower = Math.max(MIN_CHUNK_POWER, Integer.SIZE - Integer.numberOfLeadingZeros(initialCapacity - 1));
    }
    
    /**
     * Is the buffer currently empty?
     */
    // 判断弹性缓冲区中是否为空
    public boolean isEmpty() {
        return (spineIndex == 0) && (elementIndex == 0);
    }
    
    /**
     * How many elements are currently in the buffer?
     */
    // 返回弹性缓冲区中所有元素数量
    public long count() {
        if(spineIndex == 0) {
            return elementIndex;
        }
        
        return priorElementCount[spineIndex] + elementIndex;
    }
    
    /**
     * Remove all data from the buffer
     */
    // 清空弹性缓冲区
    public abstract void clear();
    
    /**
     * How big should the nth chunk be?
     */
    // 返回下一个新建chunk的容量
    protected int chunkSize(int n) {
        int power;
        
        if(n == 0 || n == 1) {
            power = initialChunkPower;
        } else {
            power = Math.min(initialChunkPower + n - 1, AbstractSpinedBuffer.MAX_CHUNK_POWER);
        }
        
        return 1 << power;
    }
    
}
