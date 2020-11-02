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

import jdk.internal.HotSpotIntrinsicCandidate;

import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Utility methods for operating on and creating streams.
 *
 * <p>Unless otherwise stated, streams are created as sequential streams.  A
 * sequential stream can be transformed into a parallel stream by calling the
 * {@code parallel()} method on the created stream.
 *
 * @since 1.8
 */
/*
 * 流迭代器工厂，提供一些复杂的流迭代器的实现。
 *
 * 主要包含以下3类流迭代器：
 * [1] "单元素"流迭代器，子类会实现流构建器接口
 * [2] "拼接"流迭代器
 * [3] "区间"流迭代器
 */
final class Streams {
    
    private Streams() {
        throw new Error("no instances");
    }
    
    
    
    /*▼ "单元素"流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * "单元素"流迭代器的抽象实现
     *
     * 在子类实现实现中，还会扩展流构建器接口，
     * 这使得子类中不仅能构造单元素流，还能构造多元素流。
     */
    private abstract static class AbstractStreamBuilderImpl<T, S extends Spliterator<T>> implements Spliterator<T> {
        
        /*
         * >= 0 when building, < 0 when built
         * -1 == no elements
         * -2 == one element, held by first
         * -3 == two or more elements, held by buffer
         */
        /*
         * >=0 [待完成]状态
         * <0  [已完成]状态
         * -n  包含n-1个元素
         */ int count;
        
        // 禁止对单元素流迭代器分割
        @Override
        public S trySplit() {
            return null;
        }
        
        // 返回当前流迭代器内的元素，也适用于多元素流
        @Override
        public long estimateSize() {
            return -count - 1;
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED | Spliterator.IMMUTABLE;
        }
    }
    
    // "单元素"流迭代器(引用类型版本)
    static final class StreamBuilderImpl<T> extends AbstractStreamBuilderImpl<T, Spliterator<T>> implements Stream.Builder<T> {
        
        // 如果仅有一个元素，则存储该元素的值
        T first;
        
        // 如果有多个元素，则使用弹性缓冲区存储元素
        SpinedBuffer<T> buffer;
        
        /**
         * Constructor for building a stream of 0 or more elements.
         */
        // 构造一个空的流迭代器，流构建器处于[待完成]状态
        StreamBuilderImpl() {
        }
        
        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        // 构造单元素流迭代器，流构建器处于[已完成]状态
        StreamBuilderImpl(T t) {
            first = t;
            count = -2;
        }
        
        // 构建单元素流或多元素流，它们使用的流迭代器不一样
        @Override
        public Stream<T> build() {
            int c = count;
            
            // 如果流构建器处于[已完成]状态，则抛出异常
            if(c<0) {
                throw new IllegalStateException();
            }
            
            // 将流构建器从[待完成]状态切换到[已完成]状态
            count = -count - 1;
            
            /* Use this spliterator if 0 or 1 elements, otherwise use the spliterator of the spined buffer */
            // 元素数量<2时，创建一个单元素流
            if(c<2) {
                return StreamSupport.stream(this, false);
            }
            
            // 使用弹性缓冲区的流迭代器
            Spliterator<T> spliterator = buffer.spliterator();
            
            // 元素数量>=2时，创建多元素流
            return StreamSupport.stream(spliterator, false);
        }
        
        // 如果当前流构建器处于[待完成]状态，则可以向其中添加元素
        public Stream.Builder<T> add(T t) {
            accept(t);
            return this;
        }
        
        // 添加元素
        @Override
        public void accept(T t) {
            
            // 如果流构建器处于[已完成]状态，则抛出异常
            if(count<0) {
                throw new IllegalStateException();
            }
            
            // 如果流迭代器为空，则添加一个元素
            if(count == 0) {
                first = t;
                count++;
                return;
            }
            
            // 如果向流迭代器中添加2个以上的元素，需要使用弹性换城区
            if(count>0) {
                if(buffer == null) {
                    buffer = new SpinedBuffer<>();
                    buffer.accept(first);   // 别忘了把第一个元素添加进来
                    count++;
                }
                
                buffer.accept(t);
            }
        }
        
        // 消费单元素流迭代器中的元素，仅允许消费一次
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action);
            
            if(count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            
            return false;
        }
        
        // 消费单元素流迭代器中的元素，仅允许消费一次
        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            Objects.requireNonNull(action);
            
            if(count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }
    
    // "单元素"流迭代器(int类型版本)
    static final class IntStreamBuilderImpl extends AbstractStreamBuilderImpl<Integer, Spliterator.OfInt> implements IntStream.Builder, Spliterator.OfInt {
        
        // 如果仅有一个元素，则存储该元素的值
        int first;
        
        // 如果有多个元素，则使用弹性缓冲区存储元素
        SpinedBuffer.OfInt buffer;
        
        /**
         * Constructor for building a stream of 0 or more elements.
         */
        // 构造一个空的流迭代器，流构建器处于[待完成]状态
        IntStreamBuilderImpl() {
        }
        
        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        // 构造单元素流迭代器，流构建器处于[已完成]状态
        IntStreamBuilderImpl(int t) {
            first = t;
            count = -2;
        }
        
        // 构建单元素流或多元素流，它们使用的流迭代器不一样
        @Override
        public IntStream build() {
            int c = count;
            
            // 如果流构建器处于[已完成]状态，则抛出异常
            if(c<0) {
                throw new IllegalStateException();
            }
            
            // 将流构建器从[待完成]状态切换到[已完成]状态
            count = -count - 1;
            
            // 元素数量<2时，创建一个单元素流
            if(c<2) {
                return StreamSupport.intStream(this, false);
            }
            
            // 使用弹性缓冲区的流迭代器
            Spliterator.OfInt spliterator = buffer.spliterator();
            
            // 元素数量>=2时，创建多元素流
            return StreamSupport.intStream(spliterator, false);
        }
        
        // 添加元素
        @Override
        public void accept(int t) {
            
            // 如果流构建器处于[已完成]状态，则抛出异常
            if(count<0) {
                throw new IllegalStateException();
            }
            
            // 如果流迭代器为空，则添加一个元素
            if(count == 0) {
                first = t;
                count++;
                return;
            }
            
            // 如果向流迭代器中添加2个以上的元素，需要使用弹性换城区
            if(count>0) {
                if(buffer == null) {
                    buffer = new SpinedBuffer.OfInt();
                    buffer.accept(first);  // 别忘了把第一个元素添加进来
                    count++;
                }
                
                buffer.accept(t);
            }
        }
        
        // 消费单元素流迭代器中的元素，仅允许消费一次
        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action);
            
            if(count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            
            return false;
        }
        
        // 消费单元素流迭代器中的元素，仅允许消费一次
        @Override
        public void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action);
            
            if(count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }
    
    // "单元素"流迭代器(long类型版本)
    static final class LongStreamBuilderImpl extends AbstractStreamBuilderImpl<Long, Spliterator.OfLong> implements LongStream.Builder, Spliterator.OfLong {
        
        // 如果仅有一个元素，则存储该元素的值
        long first;
        
        // 如果有多个元素，则使用弹性缓冲区存储元素
        SpinedBuffer.OfLong buffer;
        
        /**
         * Constructor for building a stream of 0 or more elements.
         */
        // 构造一个空的流迭代器，流构建器处于[待完成]状态
        LongStreamBuilderImpl() {
        }
        
        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        // 构造单元素流迭代器，流构建器处于[已完成]状态
        LongStreamBuilderImpl(long t) {
            first = t;
            count = -2;
        }
        
        // 构建单元素流或多元素流，它们使用的流迭代器不一样
        @Override
        public LongStream build() {
            int c = count;
            
            // 如果流构建器处于[已完成]状态，则抛出异常
            if(c<0) {
                throw new IllegalStateException();
            }
            
            // 将流构建器从[待完成]状态切换到[已完成]状态
            count = -count - 1;
            
            // 元素数量<2时，创建一个单元素流
            if(c<2) {
                return StreamSupport.longStream(this, false);
            }
            
            // 使用弹性缓冲区的流迭代器
            Spliterator.OfLong spliterator = buffer.spliterator();
            
            // 元素数量>=2时，创建多元素流
            return StreamSupport.longStream(spliterator, false);
        }
        
        // 添加元素
        @Override
        public void accept(long t) {
            
            // 如果流构建器处于[已完成]状态，则抛出异常
            if(count<0) {
                throw new IllegalStateException();
            }
            
            // 如果流迭代器为空，则添加一个元素
            if(count == 0) {
                first = t;
                count++;
                return;
            }
            
            // 如果向流迭代器中添加2个以上的元素，需要使用弹性换城区
            if(count>0) {
                if(buffer == null) {
                    buffer = new SpinedBuffer.OfLong();
                    buffer.accept(first);  // 别忘了把第一个元素添加进来
                    count++;
                }
                
                buffer.accept(t);
            }
        }
        
        // 消费单元素流迭代器中的元素，仅允许消费一次
        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action);
            
            if(count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            
            return false;
        }
        
        // 消费单元素流迭代器中的元素，仅允许消费一次
        @Override
        public void forEachRemaining(LongConsumer action) {
            Objects.requireNonNull(action);
            
            if(count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }
    
    // "单元素"流迭代器(double类型版本)
    static final class DoubleStreamBuilderImpl extends AbstractStreamBuilderImpl<Double, Spliterator.OfDouble> implements DoubleStream.Builder, Spliterator.OfDouble {
        
        // 如果仅有一个元素，则存储该元素的值
        double first;
        
        // 如果有多个元素，则使用弹性缓冲区存储元素
        SpinedBuffer.OfDouble buffer;
        
        /**
         * Constructor for building a stream of 0 or more elements.
         */
        // 构造一个空的流迭代器，流构建器处于[待完成]状态
        DoubleStreamBuilderImpl() {
        }
        
        /**
         * Constructor for a singleton stream.
         *
         * @param t the single element
         */
        // 构造单元素流迭代器，流构建器处于[已完成]状态
        DoubleStreamBuilderImpl(double t) {
            first = t;
            count = -2;
        }
        
        // 构建单元素流或多元素流，它们使用的流迭代器不一样
        @Override
        public DoubleStream build() {
            int c = count;
            
            // 如果流构建器处于[已完成]状态，则抛出异常
            if(c<0) {
                throw new IllegalStateException();
            }
            
            // 将流构建器从[待完成]状态切换到[已完成]状态
            count = -count - 1;
            
            // 元素数量<2时，创建一个单元素流
            if(c<2) {
                return StreamSupport.doubleStream(this, false);
            }
            
            // 使用弹性缓冲区的流迭代器
            Spliterator.OfDouble spliterator = buffer.spliterator();
            
            // 元素数量>=2时，创建多元素流
            return StreamSupport.doubleStream(spliterator, false);
        }
        
        // 添加元素
        @Override
        public void accept(double t) {
            
            // 如果流构建器处于[已完成]状态，则抛出异常
            if(count<0) {
                throw new IllegalStateException();
            }
            
            // 如果流迭代器为空，则添加一个元素
            if(count == 0) {
                first = t;
                count++;
                return;
            }
            
            // 如果向流迭代器中添加2个以上的元素，需要使用弹性换城区
            if(count>0) {
                if(buffer == null) {
                    buffer = new SpinedBuffer.OfDouble();
                    buffer.accept(first);   // 别忘了把第一个元素添加进来
                    count++;
                }
                
                buffer.accept(t);
            }
        }
        
        // 消费单元素流迭代器中的元素，仅允许消费一次
        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Objects.requireNonNull(action);
            
            if(count == -2) {
                action.accept(first);
                count = -1;
                return true;
            }
            
            return false;
        }
        
        // 消费单元素流迭代器中的元素，仅允许消费一次
        @Override
        public void forEachRemaining(DoubleConsumer action) {
            Objects.requireNonNull(action);
            
            if(count == -2) {
                action.accept(first);
                count = -1;
            }
        }
    }
    
    /*▲ "单元素"流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ "拼接"流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // "拼接"流迭代器的抽象实现
    abstract static class ConcatSpliterator<T, T_SPLITR extends Spliterator<T>> implements Spliterator<T> {
        
        protected final T_SPLITR aSpliterator;
        protected final T_SPLITR bSpliterator;
        
        // True when no split has occurred, otherwise false
        boolean beforeSplit;    // 是否还未发生分割：trySplit()
        
        // Never read after splitting
        final boolean unsized;  // 容量是否未知
        
        public ConcatSpliterator(T_SPLITR aSpliterator, T_SPLITR bSpliterator) {
            this.aSpliterator = aSpliterator;
            this.bSpliterator = bSpliterator;
            this.beforeSplit = true;
            // The spliterator is known to be unsized before splitting if the sum of the estimates overflows.
            this.unsized = aSpliterator.estimateSize() + bSpliterator.estimateSize()<0;
        }
        
        // 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据
        @Override
        public T_SPLITR trySplit() {
            // 如果还未分割，则直接返回aSpliterator；否则，在bSpliterator的基础上分割
            @SuppressWarnings("unchecked")
            T_SPLITR ret = beforeSplit ? aSpliterator : (T_SPLITR) bSpliterator.trySplit();
            // 标记已经进行了分割
            beforeSplit = false;
            return ret;
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
            boolean hasNext;
            
            // 如果还未分割，则在aSpliterator和bSpliterator上查找下一个元素
            if(beforeSplit) {
                hasNext = aSpliterator.tryAdvance(consumer);
                if(!hasNext) {
                    beforeSplit = false;
                    hasNext = bSpliterator.tryAdvance(consumer);
                }
                
                // 如果已经进行过分割，则只在bSpliterator上查找下一个元素
            } else {
                hasNext = bSpliterator.tryAdvance(consumer);
            }
            
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
            // 如果还未分割，则需要先遍历aSpliterator
            if(beforeSplit) {
                aSpliterator.forEachRemaining(consumer);
            }
            
            // 如果已经进行过分割，则只需遍历bSpliterator
            bSpliterator.forEachRemaining(consumer);
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            if(beforeSplit) {
                // If one or both estimates are Long.MAX_VALUE then the sum
                // will either be Long.MAX_VALUE or overflow to a negative value
                long size = aSpliterator.estimateSize() + bSpliterator.estimateSize();
                return (size >= 0) ? size : Long.MAX_VALUE;
            }
            
            return bSpliterator.estimateSize();
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            if(beforeSplit) {
                // Concatenation loses DISTINCT and SORTED characteristics
                return aSpliterator.characteristics() & bSpliterator.characteristics() & ~(Spliterator.DISTINCT | Spliterator.SORTED | (unsized ? Spliterator.SIZED | Spliterator.SUBSIZED : 0));
            }
            
            return bSpliterator.characteristics();
        }
        
        /*
         * 对于具有SORTED特征值的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED特征值的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super T> getComparator() {
            if(beforeSplit) {
                throw new IllegalStateException();
            }
            
            return bSpliterator.getComparator();
        }
        
        
        // "拼接"流迭代器(引用类型版本)
        static class OfRef<T> extends ConcatSpliterator<T, Spliterator<T>> {
            OfRef(Spliterator<T> aSpliterator, Spliterator<T> bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }
        
        // "拼接"流迭代器(基本数值类型版本)
        private abstract static class OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends ConcatSpliterator<T, T_SPLITR> implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            private OfPrimitive(T_SPLITR aSpliterator, T_SPLITR bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
            
            /*
             * 尝试用action消费当前流迭代器中下一个元素。
             * 返回值指示是否找到了下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(T_CONS action) {
                boolean hasNext;
                
                if(beforeSplit) {
                    hasNext = aSpliterator.tryAdvance(action);
                    if(!hasNext) {
                        beforeSplit = false;
                        hasNext = bSpliterator.tryAdvance(action);
                    }
                } else {
                    hasNext = bSpliterator.tryAdvance(action);
                }
                
                return hasNext;
            }
            
            /*
             * 尝试用action逐个消费当前流迭代器中所有剩余元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public void forEachRemaining(T_CONS action) {
                if(beforeSplit) {
                    aSpliterator.forEachRemaining(action);
                }
                bSpliterator.forEachRemaining(action);
            }
        }
        
        // "拼接"流迭代器(int类型版本)
        static class OfInt extends ConcatSpliterator.OfPrimitive<Integer, IntConsumer, Spliterator.OfInt> implements Spliterator.OfInt {
            OfInt(Spliterator.OfInt aSpliterator, Spliterator.OfInt bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }
        
        // "拼接"流迭代器(long类型版本)
        static class OfLong extends ConcatSpliterator.OfPrimitive<Long, LongConsumer, Spliterator.OfLong> implements Spliterator.OfLong {
            OfLong(Spliterator.OfLong aSpliterator, Spliterator.OfLong bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }
        
        // "拼接"流迭代器(double类型版本)
        static class OfDouble extends ConcatSpliterator.OfPrimitive<Double, DoubleConsumer, Spliterator.OfDouble> implements Spliterator.OfDouble {
            OfDouble(Spliterator.OfDouble aSpliterator, Spliterator.OfDouble bSpliterator) {
                super(aSpliterator, bSpliterator);
            }
        }
    }
    
    /*▲ "拼接"流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ "区间"流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * An {@code int} range spliterator.
     */
    // "区间"流迭代器(int类型版本)
    static final class RangeIntSpliterator implements Spliterator.OfInt {
        
        /**
         * The spliterator size below which the spliterator will be split
         * at the mid-point to produce balanced splits. Above this size the
         * spliterator will be split at a ratio of
         * 1:(RIGHT_BALANCED_SPLIT_RATIO - 1)
         * to produce right-balanced splits.
         *
         * <p>Such splitting ensures that for very large ranges that the left
         * side of the range will more likely be processed at a lower-depth
         * than a balanced tree at the expense of a higher-depth for the right
         * side of the range.
         *
         * <p>This is optimized for cases such as IntStream.range(0, Integer.MAX_VALUE)
         * that is likely to be augmented with a limit operation that limits the
         * number of elements to a count lower than this threshold.
         */
        private static final int BALANCED_SPLIT_THRESHOLD = 1 << 24;
        /**
         * The split ratio of the left and right split when the spliterator
         * size is above BALANCED_SPLIT_THRESHOLD.
         */
        private static final int RIGHT_BALANCED_SPLIT_RATIO = 1 << 3;
        
        /**
         * Can never be greater that upTo, this avoids overflow if upper bound
         * is Long.MAX_VALUE
         * All elements are traversed if from == upTo & last == 0
         */
        // 游标起点
        private int from;
        
        // 游标终点
        private final int upTo;
        
        /**
         * 1 if the range is closed and the last element has not been traversed
         * Otherwise, 0 if the range is open, or is a closed range and all
         * elements have been traversed
         */
        // 是否包含游标终点(即右侧是否为闭区间)，大于0表示包含，否则表示不包含
        private int last;
        
        RangeIntSpliterator(int from, int upTo, boolean closed) {
            this(from, upTo, closed ? 1 : 0);
        }
        
        private RangeIntSpliterator(int from, int upTo, int last) {
            this.from = from;
            this.upTo = upTo;
            this.last = last;
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         * 注：从这里分割出的子Spliterator，其右区间是开区间，即last==0。
         */
        @Override
        public Spliterator.OfInt trySplit() {
            long size = estimateSize();
            if(size<=1) {
                return null;
            }
            
            // Left split always has a half-open range
            return new RangeIntSpliterator(from, from = from + splitPoint(size), 0);
        }
        
        /*
         * 尝试用consumer消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(IntConsumer consumer) {
            Objects.requireNonNull(consumer);
            
            if(from<upTo) {
                consumer.accept(from++);
                return true;
            }
            
            // from==upTo
            if(last>0) {
                last = 0;
                consumer.accept(from);
                return true;
            }
            
            return false;
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        @HotSpotIntrinsicCandidate
        public void forEachRemaining(IntConsumer consumer) {
            Objects.requireNonNull(consumer);
            
            while(from<upTo) {
                consumer.accept(from++);
            }
            
            // from==upTo
            if(last>0) {
                last = 0;
                // Last element of closed range
                consumer.accept(from);
            }
        }
        
        // 返回当前流迭代器中包含的元素数量
        @Override
        public long estimateSize() {
            // Ensure ranges of size > Integer.MAX_VALUE report the correct size
            return ((long) upTo) - from + last;
        }
        
        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.DISTINCT | Spliterator.SORTED;
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
        
        // 计算子Spliterator应当包含的元素数量
        private int splitPoint(long size) {
            int d = (size<BALANCED_SPLIT_THRESHOLD) ? 2 : RIGHT_BALANCED_SPLIT_RATIO;
            
            // Cast to int is safe since:
            //   2 <= size < 2^32
            //   2 <= d <= 8
            return (int) (size / d);
        }
    }
    
    /**
     * A {@code long} range spliterator.
     *
     * This implementation cannot be used for ranges whose size is greater
     * than Long.MAX_VALUE
     */
    // "区间"流迭代器(long类型版本)
    static final class RangeLongSpliterator implements Spliterator.OfLong {
        
        /**
         * The spliterator size below which the spliterator will be split
         * at the mid-point to produce balanced splits. Above this size the
         * spliterator will be split at a ratio of
         * 1:(RIGHT_BALANCED_SPLIT_RATIO - 1)
         * to produce right-balanced splits.
         *
         * <p>Such splitting ensures that for very large ranges that the left
         * side of the range will more likely be processed at a lower-depth
         * than a balanced tree at the expense of a higher-depth for the right
         * side of the range.
         *
         * <p>This is optimized for cases such as LongStream.range(0, Long.MAX_VALUE)
         * that is likely to be augmented with a limit operation that limits the
         * number of elements to a count lower than this threshold.
         */
        private static final long BALANCED_SPLIT_THRESHOLD = 1 << 24;
        /**
         * The split ratio of the left and right split when the spliterator
         * size is above BALANCED_SPLIT_THRESHOLD.
         */
        private static final long RIGHT_BALANCED_SPLIT_RATIO = 1 << 3;
        
        /**
         * Can never be greater that upTo, this avoids overflow if upper bound
         * is Long.MAX_VALUE
         * All elements are traversed if from == upTo & last == 0
         */
        // 游标起点
        private long from;
        
        // 游标终点
        private final long upTo;
        
        /**
         * 1 if the range is closed and the last element has not been traversed
         * Otherwise, 0 if the range is open, or is a closed range and all
         * elements have been traversed
         */
        // 是否包含游标终点，大于0表示包含，否则表示不包含
        private int last;
        
        RangeLongSpliterator(long from, long upTo, boolean closed) {
            this(from, upTo, closed ? 1 : 0);
        }
        
        private RangeLongSpliterator(long from, long upTo, int last) {
            assert upTo - from + last>0;
            
            this.from = from;
            this.upTo = upTo;
            this.last = last;
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         * 注：从这里分割出的子Spliterator，其右区间是开区间，即last==0。
         */
        @Override
        public Spliterator.OfLong trySplit() {
            long size = estimateSize();
            if(size<=1) {
                return null;
            }
            
            // Left split always has a half-open range
            return new RangeLongSpliterator(from, from = from + splitPoint(size), 0);
        }
        
        /*
         * 尝试用consumer消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(LongConsumer consumer) {
            Objects.requireNonNull(consumer);
            
            if(from<upTo) {
                consumer.accept(from++);
                return true;
            }
            
            if(last>0) {
                last = 0;
                consumer.accept(from);
                return true;
            }
            
            return false;
        }
        
        /*
         * 尝试用action逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public void forEachRemaining(LongConsumer consumer) {
            Objects.requireNonNull(consumer);
            
            while(from<upTo) {
                consumer.accept(from++);
            }
            
            // from==upTo
            if(last>0) {
                last = 0;
                // Last element of closed range
                consumer.accept(from);
            }
        }
        
        // 返回当前流迭代器中包含的元素数量
        @Override
        public long estimateSize() {
            return upTo - from + last;
        }
        
        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.DISTINCT | Spliterator.SORTED;
        }
        
        /*
         * 对于具有SORTED特征值的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED特征值的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super Long> getComparator() {
            return null;
        }
        
        // 计算子Spliterator应当包含的元素数量
        private long splitPoint(long size) {
            long d = (size<BALANCED_SPLIT_THRESHOLD) ? 2 : RIGHT_BALANCED_SPLIT_RATIO;
            
            // 2 <= size <= Long.MAX_VALUE
            return size / d;
        }
    }
    
    /*▲ "区间"流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Given two Runnables, return a Runnable that executes both in sequence,
     * even if the first throws an exception, and if both throw exceptions, add
     * any exceptions thrown by the second as suppressed exceptions of the first.
     */
    // 组合两个Runnable以顺序执行
    static Runnable composeWithExceptions(Runnable a, Runnable b) {
        return () -> {
            try {
                a.run();
            } catch(Throwable e1) {
                try {
                    b.run();
                } catch(Throwable e2) {
                    try {
                        /*
                         * 为当前异常添加一个抑制(次要)异常
                         *
                         * 注：这种机制弥补了"Cause"的缺陷，当存在多个待抛异常时，可以用此种方式来区分主次
                         */
                        e1.addSuppressed(e2);
                    } catch(Throwable ignore) {
                    }
                }
    
                throw e1;
            }
    
            b.run();
        };
    }
    
    /**
     * Given two streams, return a Runnable that
     * executes both of their {@link BaseStream#close} methods in sequence,
     * even if the first throws an exception, and if both throw exceptions, add
     * any exceptions thrown by the second as suppressed exceptions of the first.
     */
    // 组合两个流以顺序关闭
    static Runnable composedClose(BaseStream<?, ?> a, BaseStream<?, ?> b) {
        return () -> {
            try {
                a.close();
            } catch(Throwable e1) {
                try {
                    b.close();
                } catch(Throwable e2) {
                    try {
                        e1.addSuppressed(e2);
                    } catch(Throwable ignore) {
                    }
                }
                throw e1;
            }
    
            b.close();
        };
    }
    
}
