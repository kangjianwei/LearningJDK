/*
 * Copyright (c) 1995, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * This class implements a vector of bits that grows as needed. Each
 * component of the bit set has a {@code boolean} value. The
 * bits of a {@code BitSet} are indexed by nonnegative integers.
 * Individual indexed bits can be examined, set, or cleared. One
 * {@code BitSet} may be used to modify the contents of another
 * {@code BitSet} through logical AND, logical inclusive OR, and
 * logical exclusive OR operations.
 *
 * <p>By default, all bits in the set initially have the value
 * {@code false}.
 *
 * <p>Every bit set has a current size, which is the number of bits
 * of space currently in use by the bit set. Note that the size is
 * related to the implementation of a bit set, so it may change with
 * implementation. The length of a bit set relates to logical length
 * of a bit set and is defined independently of implementation.
 *
 * <p>Unless otherwise noted, passing a null parameter to any of the
 * methods in a {@code BitSet} will result in a
 * {@code NullPointerException}.
 *
 * <p>A {@code BitSet} is not safe for multithreaded use without
 * external synchronization.
 *
 * @author Arthur van Hoff
 * @author Michael McCloskey
 * @author Martin Buchholz
 * @since 1.0
 */
/*
 * 位集
 *
 * 位集中的元素是单个二进制位。
 * 对于在海量数据中判断某个数据的存在性问题，使用位集可以节省空间。
 */
public class BitSet implements Cloneable, Serializable {
    
    /*
     * BitSets are packed into arrays of "words."  Currently a word is
     * a long, which consists of 64 bits, requiring 6 address bits.
     * The choice of word size is determined purely by performance concerns.
     */
    private static final int ADDRESS_BITS_PER_WORD = 6;
    
    // 每个word所占的位数：64bit，(这里所谓的word就是long)
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;
    
    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = 0xffffffffffffffffL;
    
    /**
     * The internal field corresponding to the serialField "bits".
     */
    /*
     * 存储word，其实就是存储位集中的bit
     * 在计算位的时候，需要从左到右取出word，并从右到左排列。
     */
    private long[] words;
    
    /**
     * The number of words in the logical size of this BitSet.
     */
    // 位集正在被使用的字的长度
    private transient int wordsInUse = 0;
    
    /**
     * Whether the size of "words" is user-specified.  If so, we assume
     * the user knows what he's doing and try harder to preserve it.
     */
    // 字的大小是否为用户指定
    private transient boolean sizeIsSticky = false;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new bit set. All bits are initially {@code false}.
     */
    // 构造可以存储64个bit的位集(即1个word)
    public BitSet() {
        // 由给定的bit数量，构造一个匹配大小的long数组存储bit
        initWords(BITS_PER_WORD);
        
        sizeIsSticky = false;
    }
    
    /**
     * Creates a bit set whose initial size is large enough to explicitly
     * represent bits with indices in the range {@code 0} through
     * {@code nbits-1}. All bits are initially {@code false}.
     *
     * @param nbits the initial size of the bit set
     *
     * @throws NegativeArraySizeException if the specified initial size
     *                                    is negative
     */
    // 构造至少可以存储nbits个bit的位集
    public BitSet(int nbits) {
        // nbits can't be negative; size 0 is OK
        if(nbits<0) {
            throw new NegativeArraySizeException("nbits < 0: " + nbits);
        }
    
        // 由给定的bit数量，构造一个匹配大小的long数组存储bit
        initWords(nbits);
    
        sizeIsSticky = true;
    }
    
    /**
     * Creates a bit set using words as the internal representation.
     * The last word (if there is one) must be non-zero.
     */
    // 构造可以存储words.length个word的位集，且该位集正在被使用
    private BitSet(long[] words) {
        this.words = words;
        this.wordsInUse = words.length;
        checkInvariants();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new bit set containing all the bits in the given long array.
     *
     * <p>More precisely,
     * <br>{@code BitSet.valueOf(longs).get(n) == ((longs[n/64] & (1L<<(n%64))) != 0)}
     * <br>for all {@code n < 64 * longs.length}.
     *
     * <p>This method is equivalent to
     * {@code BitSet.valueOf(LongBuffer.wrap(longs))}.
     *
     * @param longs a long array containing a little-endian representation
     *              of a sequence of bits to be used as the initial bits of the
     *              new bit set
     *
     * @return a {@code BitSet} containing all the bits in the long array
     *
     * @since 1.7
     */
    /*
     * 用指定的long数组构造位集
     *
     * 从后向前遍历数组longs，丢弃遇到的值为0的元素，直到遇到首个不为0的元素后，将该元素以及之前的元素保存到位集中。
     */
    public static BitSet valueOf(long[] longs) {
        int n;
        
        // 从后向前遍历，丢弃0元素，直到遇到首个非0元素
        for(n = longs.length; n>0 && longs[n - 1] == 0; n--)
            ;
        
        return new BitSet(Arrays.copyOf(longs, n));
    }
    
    /**
     * Returns a new bit set containing all the bits in the given long
     * buffer between its position and limit.
     *
     * <p>More precisely,
     * <br>{@code BitSet.valueOf(lb).get(n) == ((lb.get(lb.position()+n/64) & (1L<<(n%64))) != 0)}
     * <br>for all {@code n < 64 * lb.remaining()}.
     *
     * <p>The long buffer is not modified by this method, and no
     * reference to the buffer is retained by the bit set.
     *
     * @param lb a long buffer containing a little-endian representation
     *           of a sequence of bits between its position and limit, to be
     *           used as the initial bits of the new bit set
     *
     * @return a {@code BitSet} containing all the bits in the buffer in the
     * specified range
     *
     * @since 1.7
     */
    /*
     * 用指定的long缓冲区构造位集
     *
     * 从后向前遍历缓冲区lb，丢弃遇到的值为0的元素，直到遇到首个不为0的元素后，将该元素以及之前的元素保存到位集中。
     */
    public static BitSet valueOf(LongBuffer lb) {
        lb = lb.slice();
        int n;
        
        // 从后向前遍历，丢弃0元素，直到遇到首个非0元素
        for(n = lb.remaining(); n>0 && lb.get(n - 1) == 0; n--)
            ;
        
        long[] words = new long[n];
        lb.get(words);
        return new BitSet(words);
    }
    
    /**
     * Returns a new bit set containing all the bits in the given byte array.
     *
     * <p>More precisely,
     * <br>{@code BitSet.valueOf(bytes).get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
     * <br>for all {@code n <  8 * bytes.length}.
     *
     * <p>This method is equivalent to
     * {@code BitSet.valueOf(ByteBuffer.wrap(bytes))}.
     *
     * @param bytes a byte array containing a little-endian
     *              representation of a sequence of bits to be used as the
     *              initial bits of the new bit set
     *
     * @return a {@code BitSet} containing all the bits in the byte array
     *
     * @since 1.7
     */
    /*
     * 用指定的bytes数组构造位集
     *
     * 从后向前遍历数组bytes，丢弃遇到的值为0的元素，直到遇到首个不为0的元素后，将该元素以及之前的元素保存到位集中。
     */
    public static BitSet valueOf(byte[] bytes) {
        return BitSet.valueOf(ByteBuffer.wrap(bytes));
    }
    
    /**
     * Returns a new bit set containing all the bits in the given byte
     * buffer between its position and limit.
     *
     * <p>More precisely,
     * <br>{@code BitSet.valueOf(bb).get(n) == ((bb.get(bb.position()+n/8) & (1<<(n%8))) != 0)}
     * <br>for all {@code n < 8 * bb.remaining()}.
     *
     * <p>The byte buffer is not modified by this method, and no
     * reference to the buffer is retained by the bit set.
     *
     * @param bb a byte buffer containing a little-endian representation
     *           of a sequence of bits between its position and limit, to be
     *           used as the initial bits of the new bit set
     *
     * @return a {@code BitSet} containing all the bits in the buffer in the
     * specified range
     *
     * @since 1.7
     */
    /*
     * 用指定的byte缓冲区构造位集
     *
     * 从后向前遍历缓冲区bb，丢弃遇到的值为0的元素，直到遇到首个不为0的元素后，将该元素以及之前的元素保存到位集中。
     */
    public static BitSet valueOf(ByteBuffer bb) {
        bb = bb.slice().order(ByteOrder.LITTLE_ENDIAN);
        
        int n;
        
        // 从后向前遍历，丢弃0元素，直到遇到首个非0元素
        for(n = bb.remaining(); n>0 && bb.get(n - 1) == 0; n--)
            ;
        
        long[] words = new long[(n + 7) / 8];
        
        bb.limit(n);
        
        int i = 0;
        
        while(bb.remaining() >= 8) {
            // 一次读8个字节，按long解析，将position增加8个单位
            words[i++] = bb.getLong();
        }
        
        for(int remaining = bb.remaining(), j = 0; j<remaining; j++) {
            words[i] |= (bb.get() & 0xffL) << (8 * j);
        }
        
        return new BitSet(words);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 数组化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a new long array containing all the bits in this bit set.
     *
     * <p>More precisely, if
     * <br>{@code long[] longs = s.toLongArray();}
     * <br>then {@code longs.length == (s.length()+63)/64} and
     * <br>{@code s.get(n) == ((longs[n/64] & (1L<<(n%64))) != 0)}
     * <br>for all {@code n < 64 * longs.length}.
     *
     * @return a long array containing a little-endian representation
     * of all the bits in this bit set
     *
     * @since 1.7
     */
    // 以long数组形式返回位集中的数据
    public long[] toLongArray() {
        return Arrays.copyOf(words, wordsInUse);
    }
    
    /**
     * Returns a new byte array containing all the bits in this bit set.
     *
     * <p>More precisely, if
     * <br>{@code byte[] bytes = s.toByteArray();}
     * <br>then {@code bytes.length == (s.length()+7)/8} and
     * <br>{@code s.get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
     * <br>for all {@code n < 8 * bytes.length}.
     *
     * @return a byte array containing a little-endian representation
     * of all the bits in this bit set
     *
     * @since 1.7
     */
    // 以byte数组形式返回位集中的数据
    public byte[] toByteArray() {
        int n = wordsInUse;
        if(n == 0) {
            return new byte[0];
        }
        
        int len = 8 * (n - 1);
        for(long x = words[n - 1]; x != 0; x >>>= 8) {
            len++;
        }
        
        byte[] bytes = new byte[len];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for(int i = 0; i<n - 1; i++) {
            bb.putLong(words[i]);
        }
        
        for(long x = words[n - 1]; x != 0; x >>>= 8) {
            bb.put((byte) (x & 0xff));
        }
        
        return bytes;
    }
    
    /*▲ 数组化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取/设置/清除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the value of the bit with the specified index. The value
     * is {@code true} if the bit with the index {@code bitIndex}
     * is currently set in this {@code BitSet}; otherwise, the result
     * is {@code false}.
     *
     * @param bitIndex the bit index
     *
     * @return the value of the bit with the specified index
     *
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    // 判断bitIndex处的bit是否为1(bitIndex需要从右往左计数)
    public boolean get(int bitIndex) {
        if(bitIndex<0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        
        checkInvariants();
        
        int wordIndex = wordIndex(bitIndex);
        
        return (wordIndex<wordsInUse) && ((words[wordIndex] & (1L << bitIndex)) != 0);
    }
    
    /**
     * Returns a new {@code BitSet} composed of bits from this {@code BitSet}
     * from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     *
     * @param fromIndex index of the first bit to include
     * @param toIndex   index after the last bit to include
     *
     * @return a new {@code BitSet} from a range of this {@code BitSet}
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    // 返回一个子位集，其包含原位集[fromIndex, toIndex)范围内的数据
    public BitSet get(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        
        checkInvariants();
        
        int len = length();
        
        // If no set bits in range return empty bitset
        if(len<=fromIndex || fromIndex == toIndex) {
            return new BitSet(0);
        }
        
        // An optimization
        if(toIndex>len) {
            toIndex = len;
        }
        
        BitSet result = new BitSet(toIndex - fromIndex);
        int targetWords = wordIndex(toIndex - fromIndex - 1) + 1;
        int sourceIndex = wordIndex(fromIndex);
        boolean wordAligned = ((fromIndex & BIT_INDEX_MASK) == 0);
        
        // Process all words but the last word
        for(int i = 0; i<targetWords - 1; i++, sourceIndex++) {
            result.words[i] = wordAligned ? words[sourceIndex] : (words[sourceIndex] >>> fromIndex) | (words[sourceIndex + 1] << -fromIndex);
        }
        
        // Process the last word
        long lastWordMask = WORD_MASK >>> -toIndex;
        
        /* straddles source words */
        result.words[targetWords - 1] = ((toIndex - 1) & BIT_INDEX_MASK)<(fromIndex & BIT_INDEX_MASK) ? ((words[sourceIndex] >>> fromIndex) | (words[sourceIndex + 1] & lastWordMask) << -fromIndex) : ((words[sourceIndex] & lastWordMask) >>> fromIndex);
        
        // Set wordsInUse correctly
        result.wordsInUse = targetWords;
        
        result.recalculateWordsInUse();
        result.checkInvariants();
        
        return result;
    }
    
    /**
     * Sets the bit at the specified index to the specified value.
     *
     * @param bitIndex a bit index
     * @param value    a boolean value to set
     *
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    /*
     * 对指定索引处的bit进行设置/清除
     *
     * bitIndex: bit索引，从右往左计数
     * value   : 设置(true)还是清除(false)
     */
    public void set(int bitIndex, boolean value) {
        if(value) {
            set(bitIndex);
        } else {
            clear(bitIndex);
        }
    }
    
    /**
     * Sets the bit at the specified index to {@code true}.
     *
     * @param bitIndex a bit index
     *
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.0
     */
    /*
     * 将位集中指定索引处的bit设置为1。
     * 注：索引是从右往左计数的。
     */
    public void set(int bitIndex) {
        if(bitIndex<0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        
        // 计算指定索引处的bit所在的word的索引
        int wordIndex = wordIndex(bitIndex);
        
        // 尝试扩容位集，使其足够存放wordIndex + 1个bit
        expandTo(wordIndex);
        
        // 设置bit
        words[wordIndex] |= (1L << bitIndex); // Restores invariants
        
        checkInvariants();
    }
    
    /**
     * Sets the bit specified by the index to {@code false}.
     *
     * @param bitIndex the index of the bit to be cleared
     *
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.0
     */
    /*
     * 将位集中指定索引处的bit设置为0。
     * 注：索引是从右往左计数的。
     */
    public void clear(int bitIndex) {
        if(bitIndex<0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        
        // 计算指定索引处的bit所在的word的索引
        int wordIndex = wordIndex(bitIndex);
        // 如果越界，直接返回
        if(wordIndex >= wordsInUse) {
            return;
        }
        
        // 清除bit
        words[wordIndex] &= ~(1L << bitIndex);
        
        // 重新计算使用的word的数量
        recalculateWordsInUse();
        
        checkInvariants();
    }
    
    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to the specified value.
     *
     * @param fromIndex index of the first bit to be set
     * @param toIndex   index after the last bit to be set
     * @param value     value to set the selected bits to
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    /*
     * 对[fromIndex, toIndex)处的bit进行批量设置/清除
     *
     * bitIndex: bit索引，从右往左计数
     * value   : 设置(true)还是清除(false)
     */
    public void set(int fromIndex, int toIndex, boolean value) {
        if(value) {
            set(fromIndex, toIndex);
        } else {
            clear(fromIndex, toIndex);
        }
    }
    
    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to {@code true}.
     *
     * @param fromIndex index of the first bit to be set
     * @param toIndex   index after the last bit to be set
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    /*
     * 将[fromIndex, toIndex)处的bit批量设置为1。
     * 注：索引是从右往左计数的。
     */
    public void set(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        
        if(fromIndex == toIndex) {
            return;
        }
        
        // Increase capacity if necessary
        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);
        
        // 尝试扩容位集，使其足够存放wordIndex + 1个bit
        expandTo(endWordIndex);
        
        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if(startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex] |= (firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex] |= firstWordMask;
            
            // Handle intermediate words, if any
            for(int i = startWordIndex + 1; i<endWordIndex; i++)
                words[i] = WORD_MASK;
            
            // Handle last word (restores invariants)
            words[endWordIndex] |= lastWordMask;
        }
        
        checkInvariants();
    }
    
    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to {@code false}.
     *
     * @param fromIndex index of the first bit to be cleared
     * @param toIndex   index after the last bit to be cleared
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    /*
     * 将[fromIndex, toIndex)处的bit批量设置为0。
     * 注：索引是从右往左计数的。
     */
    public void clear(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        
        if(fromIndex == toIndex) {
            return;
        }
        
        int startWordIndex = wordIndex(fromIndex);
        if(startWordIndex >= wordsInUse) {
            return;
        }
        
        int endWordIndex = wordIndex(toIndex - 1);
        if(endWordIndex >= wordsInUse) {
            toIndex = length();
            endWordIndex = wordsInUse - 1;
        }
        
        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if(startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex] &= ~(firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex] &= ~firstWordMask;
            
            // Handle intermediate words, if any
            for(int i = startWordIndex + 1; i<endWordIndex; i++) {
                words[i] = 0;
            }
            
            // Handle last word
            words[endWordIndex] &= ~lastWordMask;
        }
        
        recalculateWordsInUse();
        
        checkInvariants();
    }
    
    /**
     * Sets all of the bits in this BitSet to {@code false}.
     *
     * @since 1.4
     */
    // 清除所有的bit(都设置为0)
    public void clear() {
        while(wordsInUse>0) {
            words[--wordsInUse] = 0;
        }
    }
    
    /*▲ 获取/设置/清除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 翻转 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets the bit at the specified index to the complement of its
     * current value.
     *
     * @param bitIndex the index of the bit to flip
     *
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    // 翻转指定索引处的bit，即从0到1，或从1到0(索引从右往左计数)
    public void flip(int bitIndex) {
        if(bitIndex<0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        
        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);
        
        words[wordIndex] ^= (1L << bitIndex);
        
        recalculateWordsInUse();
        
        checkInvariants();
    }
    
    /**
     * Sets each bit from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to the complement of its current
     * value.
     *
     * @param fromIndex index of the first bit to flip
     * @param toIndex   index after the last bit to flip
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    // 翻转[fromIndex, toIndex)处的bit，即从0到1，或从1到0(索引从右往左计数)
    public void flip(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        
        if(fromIndex == toIndex) {
            return;
        }
        
        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);
        expandTo(endWordIndex);
        
        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if(startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex] ^= (firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex] ^= firstWordMask;
            
            // Handle intermediate words, if any
            for(int i = startWordIndex + 1; i<endWordIndex; i++) {
                words[i] ^= WORD_MASK;
            }
            
            // Handle last word
            words[endWordIndex] ^= lastWordMask;
        }
        
        recalculateWordsInUse();
        checkInvariants();
    }
    
    /*▲ 翻转 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 定位 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the index of the first bit that is set to {@code true}
     * that occurs on or after the specified starting index. If no such
     * bit exists then {@code -1} is returned.
     *
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     *
     * <pre> {@code
     * for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
     *     // operate on index i here
     *     if (i == Integer.MAX_VALUE) {
     *         break; // or (i+1) would overflow
     *     }
     * }}</pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     *
     * @return the index of the next set bit, or {@code -1} if there
     * is no such bit
     *
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    // 返回下一个值为1的bit索引，需要从fromIndex处开始，从右向左查找
    public int nextSetBit(int fromIndex) {
        if(fromIndex<0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
    
        checkInvariants();
    
        int u = wordIndex(fromIndex);
        if(u >= wordsInUse) {
            return -1;
        }
    
        long word = words[u] & (WORD_MASK << fromIndex);
    
        while(true) {
            if(word != 0) {
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            if(++u == wordsInUse) {
                return -1;
            }
            word = words[u];
        }
    }
    
    /**
     * Returns the index of the first bit that is set to {@code false}
     * that occurs on or after the specified starting index.
     *
     * @param fromIndex the index to start checking from (inclusive)
     *
     * @return the index of the next clear bit
     *
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    // 返回下一个值为0的bit索引，需要从fromIndex处开始，从右向左查找
    public int nextClearBit(int fromIndex) {
        /*
         * Neither spec nor implementation handle bitsets of maximal length.
         * See 4816253.
         */
        if(fromIndex<0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
    
        checkInvariants();
    
        int u = wordIndex(fromIndex);
        if(u >= wordsInUse)
            return fromIndex;
    
        long word = ~words[u] & (WORD_MASK << fromIndex);
    
        while(true) {
            if(word != 0) {
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
        
            if(++u == wordsInUse) {
                return wordsInUse * BITS_PER_WORD;
            }
        
            word = ~words[u];
        }
    }
    
    /**
     * Returns the index of the nearest bit that is set to {@code true}
     * that occurs on or before the specified starting index.
     * If no such bit exists, or if {@code -1} is given as the
     * starting index, then {@code -1} is returned.
     *
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     *
     * <pre> {@code
     * for (int i = bs.length(); (i = bs.previousSetBit(i-1)) >= 0; ) {
     *     // operate on index i here
     * }}</pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     *
     * @return the index of the previous set bit, or {@code -1} if there
     * is no such bit
     *
     * @throws IndexOutOfBoundsException if the specified index is less
     *                                   than {@code -1}
     * @since 1.7
     */
    // 返回前一个值为1的bit索引，需要从fromIndex处开始，从左向右查找
    public int previousSetBit(int fromIndex) {
        if(fromIndex<0) {
            if(fromIndex == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException("fromIndex < -1: " + fromIndex);
        }
    
        checkInvariants();
    
        int u = wordIndex(fromIndex);
        if(u >= wordsInUse) {
            return length() - 1;
        }
    
        long word = words[u] & (WORD_MASK >>> -(fromIndex + 1));
    
        while(true) {
            if(word != 0) {
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            }
        
            if(u-- == 0) {
                return -1;
            }
        
            word = words[u];
        }
    }
    
    /**
     * Returns the index of the nearest bit that is set to {@code false}
     * that occurs on or before the specified starting index.
     * If no such bit exists, or if {@code -1} is given as the
     * starting index, then {@code -1} is returned.
     *
     * @param fromIndex the index to start checking from (inclusive)
     *
     * @return the index of the previous clear bit, or {@code -1} if there
     * is no such bit
     *
     * @throws IndexOutOfBoundsException if the specified index is less
     *                                   than {@code -1}
     * @since 1.7
     */
    // 返回前一个值为0的bit索引，需要从fromIndex处开始，从左向右查找
    public int previousClearBit(int fromIndex) {
        if(fromIndex<0) {
            if(fromIndex == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException("fromIndex < -1: " + fromIndex);
        }
    
        checkInvariants();
    
        int u = wordIndex(fromIndex);
        if(u >= wordsInUse) {
            return fromIndex;
        }
    
        long word = ~words[u] & (WORD_MASK >>> -(fromIndex + 1));
    
        while(true) {
            if(word != 0) {
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            }
        
            if(u-- == 0) {
                return -1;
            }
        
            word = ~words[u];
        }
    }
    
    /*▲ 定位 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 运算 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Performs a logical <b>AND</b> of this target bit set with the
     * argument bit set. This bit set is modified so that each bit in it
     * has the value {@code true} if and only if it both initially
     * had the value {@code true} and the corresponding bit in the
     * bit set argument also had the value {@code true}.
     *
     * @param set a bit set
     */
    // 对两个位集中的bit做"逻辑与"操作
    public void and(BitSet set) {
        if(this == set) {
            return;
        }
    
        while(wordsInUse>set.wordsInUse) {
            words[--wordsInUse] = 0;
        }
    
        // Perform logical AND on words in common
        for(int i = 0; i<wordsInUse; i++) {
            words[i] &= set.words[i];
        }
    
        recalculateWordsInUse();
        checkInvariants();
    }
    
    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value {@code true} if and only if it either already had the
     * value {@code true} or the corresponding bit in the bit set
     * argument has the value {@code true}.
     *
     * @param set a bit set
     */
    // 对两个位集中的bit做"逻辑或"操作
    public void or(BitSet set) {
        if(this == set) {
            return;
        }
    
        int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
    
        if(wordsInUse<set.wordsInUse) {
            ensureCapacity(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }
    
        // Perform logical OR on words in common
        for(int i = 0; i<wordsInCommon; i++) {
            words[i] |= set.words[i];
        }
    
        // Copy any remaining words
        if(wordsInCommon<set.wordsInUse) {
            System.arraycopy(set.words, wordsInCommon, words, wordsInCommon, wordsInUse - wordsInCommon);
        }
    
        // recalculateWordsInUse() is unnecessary
        checkInvariants();
    }
    
    /**
     * Performs a logical <b>XOR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value {@code true} if and only if one of the following
     * statements holds:
     * <ul>
     * <li>The bit initially has the value {@code true}, and the
     *     corresponding bit in the argument has the value {@code false}.
     * <li>The bit initially has the value {@code false}, and the
     *     corresponding bit in the argument has the value {@code true}.
     * </ul>
     *
     * @param set a bit set
     */
    // 对两个位集中的bit做"异或"操作
    public void xor(BitSet set) {
        int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
    
        if(wordsInUse<set.wordsInUse) {
            ensureCapacity(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }
    
        // Perform logical XOR on words in common
        for(int i = 0; i<wordsInCommon; i++) {
            words[i] ^= set.words[i];
        }
    
        // Copy any remaining words
        if(wordsInCommon<set.wordsInUse) {
            System.arraycopy(set.words, wordsInCommon, words, wordsInCommon, set.wordsInUse - wordsInCommon);
        }
    
        recalculateWordsInUse();
        checkInvariants();
    }
    
    /**
     * Clears all of the bits in this {@code BitSet} whose corresponding
     * bit is set in the specified {@code BitSet}.
     *
     * @param set the {@code BitSet} with which to mask this
     *            {@code BitSet}
     *
     * @since 1.2
     */
    // 对照当前位集与入参中的位集，如果某个位置上的bit在入参位集中的值为1，则将其相应地从当前位集中清除
    public void andNot(BitSet set) {
        // Perform logical (a & !b) on words in common
        for(int i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--) {
            words[i] &= ~set.words[i];
        }
        
        recalculateWordsInUse();
        checkInvariants();
    }
    
    /*▲ 运算 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ bit数量 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of bits of space actually in use by this
     * {@code BitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the number of bits currently in this bit set
     */
    // 位集中有效bit的数量(会统计高位连续的0)
    public int size() {
        return words.length * BITS_PER_WORD;
    }
    
    /**
     * Returns the "logical size" of this {@code BitSet}: the index of
     * the highest set bit in the {@code BitSet} plus one. Returns zero
     * if the {@code BitSet} contains no set bits.
     *
     * @return the logical size of this {@code BitSet}
     *
     * @since 1.2
     */
    // 返回当前位集使用的bit数量(不统计高位连续的0)
    public int length() {
        if(wordsInUse == 0) {
            return 0;
        }
        
        return BITS_PER_WORD * (wordsInUse - 1) + (BITS_PER_WORD - Long.numberOfLeadingZeros(words[wordsInUse - 1]));
    }
    
    /*▲ bit数量 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns true if this {@code BitSet} contains no bits that are set
     * to {@code true}.
     *
     * @return boolean indicating whether this {@code BitSet} is empty
     *
     * @since 1.4
     */
    // 判断位集是否为0(里面的bit全为0)
    public boolean isEmpty() {
        return wordsInUse == 0;
    }
    
    /**
     * Returns the number of bits set to {@code true} in this {@code BitSet}.
     *
     * @return the number of bits set to {@code true} in this {@code BitSet}
     *
     * @since 1.4
     */
    // 返回位集中bit为1的位的数量
    public int cardinality() {
        int sum = 0;
        
        for(int i = 0; i<wordsInUse; i++) {
            sum += Long.bitCount(words[i]);
        }
        
        return sum;
    }
    
    /**
     * Returns true if the specified {@code BitSet} has any bits set to
     * {@code true} that are also set to {@code true} in this {@code BitSet}.
     *
     * @param set {@code BitSet} to intersect with
     *
     * @return boolean indicating whether this {@code BitSet} intersects
     * the specified {@code BitSet}
     *
     * @since 1.4
     */
    // 判断两个位集中为1的位是否有交集
    public boolean intersects(BitSet set) {
        for(int i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--) {
            if((words[i] & set.words[i]) != 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns a stream of indices for which this {@code BitSet}
     * contains a bit in the set state. The indices are returned
     * in order, from lowest to highest. The size of the stream
     * is the number of bits in the set state, equal to the value
     * returned by the {@link #cardinality()} method.
     *
     * <p>The stream binds to this bit set when the terminal stream operation
     * commences (specifically, the spliterator for the stream is
     * <a href="Spliterator.html#binding"><em>late-binding</em></a>).  If the
     * bit set is modified during that operation then the result is undefined.
     *
     * @return a stream of integers representing set indices
     *
     * @since 1.8
     */
    // 将bit流化，该流中的元素是值为1的bit的索引
    public IntStream stream() {
        class BitSetSpliterator implements Spliterator.OfInt {
            private int index; // current bit index for a set bit
            private int fence; // -1 until used; then one past last bit index
            private int est;   // size estimate
            private boolean root; // true if root and not split
    
            // root == true then size estimate is accurate
            // index == -1 or index >= fence if fully traversed
            // Special case when the max bit set is Integer.MAX_VALUE
    
            BitSetSpliterator(int origin, int fence, int est, boolean root) {
                this.index = origin;
                this.fence = fence;
                this.est = est;
                this.root = root;
            }
    
            @Override
            public OfInt trySplit() {
                int hi = getFence();
                int lo = index;
                if(lo<0) {
                    return null;
                }
        
                // Lower the fence to be the upper bound of last bit set
                // The index is the first bit set, thus this spliterator
                // covers one bit and cannot be split, or two or more bits
                hi = fence = (hi<Integer.MAX_VALUE || !get(Integer.MAX_VALUE)) ? previousSetBit(hi - 1) + 1 : Integer.MAX_VALUE;
        
                // Find the mid point
                int mid = (lo + hi) >>> 1;
                if(lo >= mid) {
                    return null;
                }
        
                // Raise the index of this spliterator to be the next set bit
                // from the mid point
                index = nextSetBit(mid, wordIndex(hi - 1));
                root = false;
        
                // Don't lower the fence (mid point) of the returned spliterator,
                // traversal or further splitting will do that work
                return new BitSetSpliterator(lo, mid, est >>>= 1, false);
            }
    
            @Override
            public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action);
        
                int hi = getFence();
                int i = index;
                if(i<0 || i >= hi) {
                    // Check if there is a final bit set for Integer.MAX_VALUE
                    if(i == Integer.MAX_VALUE && hi == Integer.MAX_VALUE) {
                        index = -1;
                        action.accept(Integer.MAX_VALUE);
                        return true;
                    }
                    return false;
                }
        
                index = nextSetBit(i + 1, wordIndex(hi - 1));
                action.accept(i);
                return true;
            }
    
            @Override
            public void forEachRemaining(IntConsumer action) {
                Objects.requireNonNull(action);
        
                int hi = getFence();
                int i = index;
                index = -1;
        
                if(i >= 0 && i<hi) {
                    action.accept(i++);
            
                    int u = wordIndex(i);      // next lower word bound
                    int v = wordIndex(hi - 1); // upper word bound

words_loop:
                    for(; u<=v && i<=hi; u++, i = u << ADDRESS_BITS_PER_WORD) {
                        long word = words[u] & (WORD_MASK << i);
                        while(word != 0) {
                            i = (u << ADDRESS_BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
                            if(i >= hi) {
                                // Break out of outer loop to ensure check of
                                // Integer.MAX_VALUE bit set
                                break words_loop;
                            }
                    
                            // Flip the set bit
                            word &= ~(1L << i);
                    
                            action.accept(i);
                        }
                    }
                }
        
                // Check if there is a final bit set for Integer.MAX_VALUE
                if(i == Integer.MAX_VALUE && hi == Integer.MAX_VALUE) {
                    action.accept(Integer.MAX_VALUE);
                }
            }
    
            /*
             * 初始时，返回流迭代器中的元素总量(可能不精确)。
             * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
             * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
             */
            @Override
            public long estimateSize() {
                getFence(); // force init
                return est;
            }
    
            @Override
            public int characteristics() {
                // Only sized when root and not split
                return (root ? Spliterator.SIZED : 0) | Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED;
            }
    
            /*
             * 对于具有SORTED特征值的容器来说，
             * 如果该容器使用Comparator排序，则返回其Comparator；
             * 如果该容器使用Comparable实现自然排序，则返回null；
             *
             * 对于不具有SORTED特征值的容器来说，抛出异常。
             */
            @Override
            public Comparator<? super Integer> getComparator() {
                return null;
            }
    
            private int getFence() {
                int hi;
                if((hi = fence)<0) {
                    // Round up fence to maximum cardinality for allocated words
                    // This is sufficient and cheap for sequential access
                    // When splitting this value is lowered
                    hi = fence = (wordsInUse >= wordIndex(Integer.MAX_VALUE)) ? Integer.MAX_VALUE : wordsInUse << ADDRESS_BITS_PER_WORD;
                    est = cardinality();
                    index = nextSetBit(0);
                }
                return hi;
            }
        }
    
        return StreamSupport.intStream(new BitSetSpliterator(0, -1, 0, true), false);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Given a bit index, return word index containing it.
     */
    // 计算指定索引处的bit所在的word的索引
    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }
    
    /*
     * 由给定的bit数量，构造一个匹配大小的long数组存储bit。
     * 这里会向上取整，比如输入60，则虽然不足64位，却也构造包含一个long的word数组。
     */
    private void initWords(int nbits) {
        // 计算指定索引处的bit所在的word的索引
        int size = wordIndex(nbits - 1);
        words = new long[size + 1];
    }
    
    /**
     * Ensures that the BitSet can accommodate a given wordIndex,
     * temporarily violating the invariants.  The caller must
     * restore the invariants before returning to the user,
     * possibly using recalculateWordsInUse().
     *
     * @param wordIndex the index to be accommodated.
     */
    // 尝试扩容位集，使其足够存放wordIndex + 1个bit
    private void expandTo(int wordIndex) {
        int wordsRequired = wordIndex + 1;
        if(wordsInUse<wordsRequired) {
            ensureCapacity(wordsRequired);
            wordsInUse = wordsRequired;
        }
    }
    
    /**
     * Ensures that the BitSet can hold enough words.
     *
     * @param wordsRequired the minimum acceptable number of words.
     */
    // 位集容量扩容
    private void ensureCapacity(int wordsRequired) {
        // 容量充足则直接返回
        if(words.length >= wordsRequired) {
            return;
        }
        
        // Allocate larger of doubled size or required size
        int request = Math.max(2 * words.length, wordsRequired);
        words = Arrays.copyOf(words, request);
        sizeIsSticky = false;
    }
    
    /**
     * Sets the field wordsInUse to the logical size in words of the bit set.
     * WARNING:This method assumes that the number of words actually in use is
     * less than or equal to the current value of wordsInUse!
     */
    // 重新计算使用的word的数量
    private void recalculateWordsInUse() {
        // Traverse the bitset until a used word is found
        int i;
        
        for(i = wordsInUse - 1; i >= 0; i--) {
            if(words[i] != 0) {
                break;
            }
        }
        
        wordsInUse = i + 1; // The new logical size
    }
    
    /**
     * Attempts to reduce internal storage used for the bits in this bit set.
     * Calling this method may, but is not required to, affect the value
     * returned by a subsequent call to the {@link #size()} method.
     */
    private void trimToSize() {
        if(wordsInUse != words.length) {
            words = Arrays.copyOf(words, wordsInUse);
            checkInvariants();
        }
    }
    
    /**
     * Returns the index of the first bit that is set to {@code true}
     * that occurs on or after the specified starting index and up to and
     * including the specified word index
     * If no such bit exists then {@code -1} is returned.
     *
     * @param fromIndex   the index to start checking from (inclusive)
     * @param toWordIndex the last word index to check (inclusive)
     *
     * @return the index of the next set bit, or {@code -1} if there
     * is no such bit
     */
    private int nextSetBit(int fromIndex, int toWordIndex) {
        int u = wordIndex(fromIndex);
        // Check if out of bounds
        if(u>toWordIndex)
            return -1;
    
        long word = words[u] & (WORD_MASK << fromIndex);
    
        while(true) {
            if(word != 0) {
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            // Check if out of bounds
            if(++u>toWordIndex) {
                return -1;
            }
            word = words[u];
        }
    }
    
    /**
     * Checks that fromIndex ... toIndex is a valid range of bit indices.
     */
    private static void checkRange(int fromIndex, int toIndex) {
        if(fromIndex<0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        if(toIndex<0) {
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        }
        if(fromIndex>toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + " > toIndex: " + toIndex);
        }
    }
    
    /**
     * Every public method must preserve these invariants.
     */
    private void checkInvariants() {
        assert (wordsInUse == 0 || words[wordsInUse - 1] != 0);
        assert (wordsInUse >= 0 && wordsInUse<=words.length);
        assert (wordsInUse == words.length || words[wordsInUse] == 0);
    }
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /* use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = 7997698588986878753L;
    
    /**
     * @serialField bits long[]
     *
     * The bits in this BitSet.  The ith bit is stored in bits[i/64] at
     * bit position i % 64 (where bit position 0 refers to the least
     * significant bit and 63 refers to the most significant bit).
     */
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("bits", long[].class),};
    
    /**
     * Save the state of the {@code BitSet} instance to a stream (i.e.,
     * serialize it).
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        
        checkInvariants();
        
        if(!sizeIsSticky) {
            trimToSize();
        }
        
        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("bits", words);
        s.writeFields();
    }
    
    /**
     * Reconstitute the {@code BitSet} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        
        ObjectInputStream.GetField fields = s.readFields();
        words = (long[]) fields.get("bits", null);
        
        // Assume maximum length then find real length
        // because recalculateWordsInUse assumes maintenance
        // or reduction in logical size
        wordsInUse = words.length;
        recalculateWordsInUse();
        sizeIsSticky = (words.length>0 && words[words.length - 1] == 0L); // heuristic
        checkInvariants();
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns a string representation of this bit set. For every index
     * for which this {@code BitSet} contains a bit in the set
     * state, the decimal representation of that index is included in
     * the result. Such indices are listed in order from lowest to
     * highest, separated by ",&nbsp;" (a comma and a space) and
     * surrounded by braces, resulting in the usual mathematical
     * notation for a set of integers.
     *
     * <p>Example:
     * <pre>
     * BitSet drPepper = new BitSet();</pre>
     * Now {@code drPepper.toString()} returns "{@code {}}".
     * <pre>
     * drPepper.set(2);</pre>
     * Now {@code drPepper.toString()} returns "{@code {2}}".
     * <pre>
     * drPepper.set(4);
     * drPepper.set(10);</pre>
     * Now {@code drPepper.toString()} returns "{@code {2, 4, 10}}".
     *
     * @return a string representation of this bit set
     */
    public String toString() {
        checkInvariants();
        
        int numBits = (wordsInUse>128) ? cardinality() : wordsInUse * BITS_PER_WORD;
        StringBuilder b = new StringBuilder(6 * numBits + 2);
        b.append('{');
        
        int i = nextSetBit(0);
        if(i != -1) {
            b.append(i);
            while(true) {
                if(++i<0)
                    break;
                if((i = nextSetBit(i))<0)
                    break;
                int endOfRun = nextClearBit(i);
                do {
                    b.append(", ").append(i);
                } while(++i != endOfRun);
            }
        }
        
        b.append('}');
        return b.toString();
    }
    
    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and is a {@code Bitset} object that has
     * exactly the same set of bits set to {@code true} as this bit
     * set. That is, for every nonnegative {@code int} index {@code k},
     * <pre>((BitSet)obj).get(k) == this.get(k)</pre>
     * must be true. The current sizes of the two bit sets are not compared.
     *
     * @param obj the object to compare with
     *
     * @return {@code true} if the objects are the same;
     * {@code false} otherwise
     *
     * @see #size()
     */
    public boolean equals(Object obj) {
        if(!(obj instanceof BitSet))
            return false;
        if(this == obj)
            return true;
        
        BitSet set = (BitSet) obj;
        
        checkInvariants();
        set.checkInvariants();
        
        if(wordsInUse != set.wordsInUse)
            return false;
        
        // Check words in use by both BitSets
        for(int i = 0; i<wordsInUse; i++)
            if(words[i] != set.words[i])
                return false;
        
        return true;
    }
    
    /**
     * Cloning this {@code BitSet} produces a new {@code BitSet}
     * that is equal to it.
     * The clone of the bit set is another bit set that has exactly the
     * same bits set to {@code true} as this bit set.
     *
     * @return a clone of this bit set
     *
     * @see #size()
     */
    public Object clone() {
        if(!sizeIsSticky)
            trimToSize();
        
        try {
            BitSet result = (BitSet) super.clone();
            result.words = words.clone();
            result.checkInvariants();
            return result;
        } catch(CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
    
    /**
     * Returns the hash code value for this bit set. The hash code depends
     * only on which bits are set within this {@code BitSet}.
     *
     * <p>The hash code is defined to be the result of the following
     * calculation:
     * <pre> {@code
     * public int hashCode() {
     *     long h = 1234;
     *     long[] words = toLongArray();
     *     for (int i = words.length; --i >= 0; )
     *         h ^= words[i] * (i + 1);
     *     return (int)((h >> 32) ^ h);
     * }}</pre>
     * Note that the hash code changes if the set of bits is altered.
     *
     * @return the hash code value for this bit set
     */
    public int hashCode() {
        long h = 1234;
        for(int i = wordsInUse; --i >= 0; )
            h ^= words[i] * (i + 1);
        
        return (int) ((h >> 32) ^ h);
    }
    
}
