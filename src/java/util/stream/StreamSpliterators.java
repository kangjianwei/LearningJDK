/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Spliterator implementations for wrapping and delegating spliterators, used
 * in the implementation of the {@link Stream#spliterator()} method.
 *
 * @since 1.8
 */
/*
 * 流迭代器工厂，用来构建复杂类型的流迭代器。
 *
 * 主要包括以下6种流迭代器：
 * [1] "包装"流迭代器
 * [2] "惰性"流迭代器
 * [3] "分片"流迭代器
 * [4] "无序"流迭代器
 * [5] "去重"流迭代器
 * [6] "无限"流迭代器
 */
class StreamSpliterators {
    
    /*▼ "包装"流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Abstract wrapping spliterator that binds to the spliterator of a
     * pipeline helper on first operation.
     *
     * <p>This spliterator is not late-binding and will bind to the source
     * spliterator when first operated on.
     *
     * <p>A wrapping spliterator produced from a sequential stream
     * cannot be split if there are stateful operations present.
     */
    // "包装"流迭代器的抽象实现，该流迭代器可用来获取helper阶段的输出元素
    private abstract static class AbstractWrappingSpliterator<P_IN, P_OUT, T_BUFFER extends AbstractSpinedBuffer> implements Spliterator<P_OUT> {
        
        // 某个流阶段
        final PipelineHelper<P_OUT> helper;
        
        /**
         * Supplier for the source spliterator.
         * Client provides either a spliterator or a supplier.
         */
        // (相对于helper阶段的)属于上个(depth==0)的流阶段的流迭代器工厂，其中生产的流迭代器包含了当前所有待访问元素
        private Supplier<Spliterator<P_IN>> spliteratorSupplier;
        
        /**
         * Source spliterator.
         * Either provided from client or obtained from supplier.
         */
        // (相对于helper阶段的)属于上个(depth==0)的流阶段的流迭代器，包含了当前所有待访问元素
        Spliterator<P_IN> spliterator;
        
        /**
         * True if this spliterator supports splitting
         */
        // 是否需要并行执行
        final boolean isParallel;
        
        /**
         * Sink chain for the downstream stages of the pipeline, ultimately
         * leading to the buffer. Used during partial traversal.
         */
        /*
         * (相对于helper阶段的)属于上个(depth==1)的流阶段的sink。
         * 通过该sink所在的链条，可以将spliterator中的元素择取到buffer中。
         */ Sink<P_IN> bufferSink;
        
        /**
         * A function that advances one element of the spliterator, pushing
         * it to bufferSink.  Returns whether any elements were processed.
         * Used during partial traversal.
         */
        /*
         * 函数表达式，其作用是：使用bufferSink消费流迭代器spliterator中的元素。
         * 消费成功则返回true，否则返回false。
         */ BooleanSupplier pusher;
        
        /** Buffer into which elements are pushed.  Used during partial traversal. */
        // 存储最终收集到的元素
        T_BUFFER buffer;
        
        /** Next element to consume from the buffer, used during partial traversal */
        // buffer中的元素数量
        long nextToConsume;
        
        /**
         * True if full traversal has occurred (with possible cancellation).
         * If doing a partial traversal, there may be still elements in buffer.
         */
        /*
         * 是否停止继续访问spliterator中的元素
         *
         * 如果在择取元素时遇到了短路操作，无需再访问元素；
         * 或者，spliterator中的元素都已经被访问完了；
         * 那么在上述情形下，finished为false，即不再需要继续访问spliterator中的元素。
         * 否则，finished为true。
         */ boolean finished;
        
        /**
         * Construct an AbstractWrappingSpliterator from a
         * {@code Supplier<Spliterator>}.
         */
        AbstractWrappingSpliterator(PipelineHelper<P_OUT> helper, Supplier<Spliterator<P_IN>> spliteratorSupplier, boolean parallel) {
            this.helper = helper;
            this.spliteratorSupplier = spliteratorSupplier;
            this.spliterator = null;
            this.isParallel = parallel;
        }
        
        /**
         * Construct an AbstractWrappingSpliterator from a
         * {@code Spliterator}.
         */
        AbstractWrappingSpliterator(PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, boolean parallel) {
            this.helper = helper;
            this.spliteratorSupplier = null;
            this.spliterator = spliterator;
            this.isParallel = parallel;
        }
        
        /**
         * Invokes the shape-specific constructor with the provided arguments and returns the result.
         */
        // 使用指定的流迭代器，重新构造一个"包装"流迭代器
        abstract AbstractWrappingSpliterator<P_IN, P_OUT, ?> wrap(Spliterator<P_IN> spliterator);
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator<P_OUT> trySplit() {
            if(isParallel && buffer == null && !finished) {
                // 确保流迭代器已经初始化
                init();
                
                Spliterator<P_IN> splitSpliterator = spliterator.trySplit();
                if(splitSpliterator == null) {
                    return null;
                }
                
                // 使用指定的流迭代器，重新构造一个"包装"流迭代器
                return wrap(splitSpliterator);
            }
            
            return null;
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public final long estimateSize() {
            // 确保流迭代器已经初始化(准备待遍历数据)
            init();
            
            /*
             * Use the estimate of the wrapped spliterator
             * Note this may not be accurate if there are filter/flatMap
             * operations filtering or adding elements to the stream
             */
            return spliterator.estimateSize();
        }
        
        /*
         * 初始时，尝试返回流迭代器中的元素总量。如果无法获取精确值，则返回-1。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         *
         * 注：通常在流迭代器拥有SIZED参数时可以获取到一个精确值。
         */
        @Override
        public final long getExactSizeIfKnown() {
            // 确保流迭代器已经初始化(准备待遍历数据)
            init();
            
            // 获取helper流阶段的组合参数
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            return StreamOpFlag.SIZED.isKnown(streamAndOpFlags) ? spliterator.getExactSizeIfKnown() : -1;
        }
        
        // 返回流迭代器的参数
        @Override
        public final int characteristics() {
            // 确保流迭代器已经初始化(准备待遍历数据)
            init();
            
            // 获取helper流阶段的组合参数
            int streamAndOpFlags = helper.getStreamAndOpFlags();
            
            // 从组合参数中提取出属于流(STREAM)的参数，且只提取包含"01"的位
            int streamFlags = StreamOpFlag.toStreamFlags(streamAndOpFlags);
            
            // 将流参数转换为流分割器参数
            int characteristics = StreamOpFlag.toCharacteristics(streamFlags);
            
            /*
             * Mask off the size and uniform characteristics and replace with those of the spliterator
             * Note that a non-uniform spliterator can change from something with an exact size to an estimate for a sub-split,
             * for example, with HashSet where the size is known at the top level spliterator
             * but for sub-splits only an estimate is known
             */
            if((characteristics & Spliterator.SIZED) != 0) {
                // 先去掉SIZED和SUBSIZED标记
                characteristics &= ~(Spliterator.SIZED | Spliterator.SUBSIZED);
                characteristics |= (spliterator.characteristics() & (Spliterator.SIZED | Spliterator.SUBSIZED));
            }
            
            return characteristics;
        }
        
        /*
         * 对于具有SORTED特征值的容器来说，
         * 如果该容器使用Comparator排序，则返回其Comparator；
         * 如果该容器使用Comparable实现自然排序，则返回null；
         *
         * 对于不具有SORTED特征值的容器来说，抛出异常。
         */
        @Override
        public Comparator<? super P_OUT> getComparator() {
            if(!hasCharacteristics(SORTED)) {
                throw new IllegalStateException();
            }
            
            return null;
        }
        
        
        /**
         * Called before advancing to set up spliterator, if needed.
         */
        // 确保流迭代器已经初始化(准备待遍历数据)
        final void init() {
            if(spliterator != null) {
                return;
            }
            
            spliterator = spliteratorSupplier.get();
            spliteratorSupplier = null;
        }
        
        /**
         * Initializes buffer, sink chain, and pusher for a shape-specific implementation.
         */
        // 初始化buffer、bufferSink、pusher，为"部分遍历"做准备
        abstract void initPartialTraversalState();
        
        /**
         * If the buffer is empty, push elements into the sink chain until
         * the source is empty or cancellation is requested.
         *
         * @return whether there are elements to consume from the buffer
         */
        /*
         * 从spliterator中择取数据，把满足过滤条件的数据填充到buffer中。
         * 只要成功向buffer中填充了任意一个元素，则此处返回true；
         * 否则，返回false，表示buffer中没有任何数据。
         */
        private boolean fillBuffer() {
            
            // 如果缓冲区为空，则尝试向其中填充元素
            while(buffer.count() == 0) {
                /*
                 * 如果bufferSink不希望再择取数据(遇到了短路操作)，
                 * 或者，spliterator中的元素已经被bufferSink消费完了，
                 * 那么接下来要考虑终止数据的择取过程。
                 */
                if(bufferSink.cancellationRequested() || !pusher.getAsBoolean()) {
                    if(finished) {
                        return false;
                    }
                    
                    bufferSink.end(); // might trigger more elements
                    
                    // 结束访问
                    finished = true;
                }
            }
            
            return true;
        }
        
        /**
         * Get an element from the source, pushing it into the sink chain,
         * setting up the buffer if needed
         *
         * @return whether there are elements to consume from the buffer
         */
        /*
         * 判断是否存在未访问过的元素
         *
         * 先在缓冲区中查找，如果缓冲区不为null，且包含未访问过的数据，则返回true。
         * 如果缓冲区为null，或者缓冲区中的数据都被访问过了，
         * 那么需要去spliterator中择取数据，并填充到buffer中。
         * 填充过程结束后，如果找到了新的未访问元素，则依然返回true，否则，返回false。
         */
        final boolean doAdvance() {
            
            // 尝试从spliterator中择取数据并存入buffer，只要成功存入任一元素，则返回true
            if(buffer == null) {
                if(finished) {
                    return false;
                }
                
                // 确保流迭代器已经初始化(准备待遍历数据)
                init();
                
                // 初始化buffer、bufferSink、pusher，为"部分遍历"做准备
                initPartialTraversalState();
                
                // 初始化缓冲区中的数据量为0
                nextToConsume = 0;
                
                // 获取流迭代器中的元素数量
                long sizeIfKnown = spliterator.getExactSizeIfKnown();
                
                // 初始化bufferSink的状态，准备择取数据
                bufferSink.begin(sizeIfKnown);
                
                /*
                 * 从spliterator中择取数据，把满足过滤条件的数据填充到buffer中。
                 * 只要成功向buffer中填充了任意一个元素，则此处返回true；
                 * 否则，返回false，表示buffer中没有任何数据。
                 */
                return fillBuffer();
                
                // 如果buffer中已有数据，则尝试访问之前未访问的元素
            } else {
                
                // 访问下一个元素
                ++nextToConsume;
                
                // 判断buffer中是否有未访问过的数据
                boolean hasNext = nextToConsume<buffer.count();
                
                // 如果buffer中的数据已经全部被访问过了，则清空buffer，并再次尝试填充buffer
                if(!hasNext) {
                    nextToConsume = 0;
                    buffer.clear();
                    hasNext = fillBuffer();
                }
                
                return hasNext;
            }
        }
        
        
        @Override
        public final String toString() {
            return String.format("%s[%s]", getClass().getName(), spliterator);
        }
    }
    
    // "包装"流迭代器的引用类型版本，该流迭代器可用来获取helper阶段的输出元素
    static final class WrappingSpliterator<P_IN, P_OUT> extends AbstractWrappingSpliterator<P_IN, P_OUT, SpinedBuffer<P_OUT>> {
        
        WrappingSpliterator(PipelineHelper<P_OUT> ph, Supplier<Spliterator<P_IN>> supplier, boolean parallel) {
            super(ph, supplier, parallel);
        }
        
        WrappingSpliterator(PipelineHelper<P_OUT> ph, Spliterator<P_IN> spliterator, boolean parallel) {
            super(ph, spliterator, parallel);
        }
        
        // 使用指定的流迭代器，重新构造一个"包装"流迭代器
        @Override
        WrappingSpliterator<P_IN, P_OUT> wrap(Spliterator<P_IN> spliterator) {
            return new WrappingSpliterator<>(helper, spliterator, isParallel);
        }
        
        /*
         * 使用consumer消费当前流迭代器中的元素。
         * 如果存在未访问的元素可被消费，则返回true；否则，返回false。
         *
         * 注：这里对spliterator进行了包装，即这里消费的元素必须先经过整个流上的sink链的择取。
         */
        @Override
        public boolean tryAdvance(Consumer<? super P_OUT> consumer) {
            Objects.requireNonNull(consumer);
            
            /*
             * 判断是否存在未访问过的元素
             *
             * 先在缓冲区中查找，如果缓冲区不为null，且包含未访问过的数据，则返回true。
             * 如果缓冲区为null，或者缓冲区中的数据都被访问过了，
             * 那么需要去spliterator中择取数据，并填充到buffer中。
             * 填充过程结束后，如果找到了新的未访问元素，则依然返回true，否则，返回false。
             */
            boolean hasNext = doAdvance();
            
            // 如果在缓冲区中找到了未访问元素，则获取该元素，并对其进行择取操作
            if(hasNext) {
                P_OUT e = buffer.get(nextToConsume);
                consumer.accept(e);
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
        public void forEachRemaining(Consumer<? super P_OUT> consumer) {
            // 如果缓冲区为null，且仍然允许继续访问spliterator中的元素
            if(buffer == null && !finished) {
                Objects.requireNonNull(consumer);
                
                // 确保流迭代器已经初始化(准备待遍历数据)
                init();
                
                // 为即将进入的终端阶段构造一个sink
                Sink<P_OUT> downSink = new Sink<P_OUT>() {
                    @Override
                    public void accept(P_OUT e) {
                        consumer.accept(e);
                    }
                };
                
                /*
                 * 从downSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
                 * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
                 * 最终择取出的数据往往被存入了downSink代表的容器当中。
                 *
                 * downSink   : (相对于helper的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
                 * spliterator: 流迭代器，作为数据源，包含了当前所有待访问的元素
                 */
                helper.wrapAndCopyInto(downSink, spliterator);
                
                // 结束访问
                finished = true;
                
                // 缓冲区不为null，或者已经禁止继续访问spliterator中的元素时，直接使用tryAdvance()访问缓冲区中剩余未访问的元素
            } else {
                while(tryAdvance(consumer)) {
                }
            }
        }
        
        // 初始化buffer、bufferSink、pusher，为"部分遍历"做准备
        @Override
        void initPartialTraversalState() {
            
            // 初始化一个弹性缓冲区
            buffer = new SpinedBuffer<>();
            
            // 为即将进入的终端阶段构造一个sink
            Sink<P_OUT> downSink = new Sink<P_OUT>() {
                @Override
                public void accept(P_OUT e) {
                    // 向buffer存入一个元素
                    buffer.accept(e);
                }
            };
            
            /*
             * 从downSink开始，逆向遍历流，构造并返回(相对于helper阶段的)属于上个(depth==1)的流阶段的sink。
             *
             * 返回的sink与downSink组成一个完整的链条，以便处理属于上个(depth==0)的流阶段输出的数据。
             * 经过该sink链条处理过的数据，会被downSink所在的流阶段输出给downSink的下游阶段。
             *
             * downSink: (相对于helper阶段的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             */
            bufferSink = helper.wrapSink(downSink);
            
            // 初始化一个对spliterator中元素的待执行操作
            pusher = new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() {
                    return spliterator.tryAdvance(bufferSink);
                }
            };
        }
    }
    
    // "包装"流迭代器的的int类型版本，该流迭代器可用来获取helper阶段的输出元素
    static final class IntWrappingSpliterator<P_IN> extends AbstractWrappingSpliterator<P_IN, Integer, SpinedBuffer.OfInt> implements Spliterator.OfInt {
        IntWrappingSpliterator(PipelineHelper<Integer> ph, Supplier<Spliterator<P_IN>> supplier, boolean parallel) {
            super(ph, supplier, parallel);
        }
        
        IntWrappingSpliterator(PipelineHelper<Integer> ph, Spliterator<P_IN> spliterator, boolean parallel) {
            super(ph, spliterator, parallel);
        }
        
        // 使用指定的流迭代器，重新构造一个"包装"流迭代器
        @Override
        AbstractWrappingSpliterator<P_IN, Integer, ?> wrap(Spliterator<P_IN> spliterator) {
            return new IntWrappingSpliterator<>(helper, spliterator, isParallel);
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator.OfInt trySplit() {
            return (Spliterator.OfInt) super.trySplit();
        }
        
        /*
         * 使用consumer消费当前流迭代器中的元素。
         * 如果存在未访问的元素可被消费，则返回true；否则，返回false。
         *
         * 注：这里对spliterator进行了包装，即这里消费的元素必须先经过整个流上的sink链的择取。
         */
        @Override
        public boolean tryAdvance(IntConsumer consumer) {
            Objects.requireNonNull(consumer);
            
            /*
             * 判断是否存在未访问过的元素
             *
             * 先在缓冲区中查找，如果缓冲区不为null，且包含未访问过的数据，则返回true。
             * 如果缓冲区为null，或者缓冲区中的数据都被访问过了，
             * 那么需要去spliterator中择取数据，并填充到buffer中。
             * 填充过程结束后，如果找到了新的未访问元素，则依然返回true，否则，返回false。
             */
            boolean hasNext = doAdvance();
            
            // 如果在缓冲区中找到了未访问元素，则获取该元素，并对其进行择取操作
            if(hasNext) {
                int e = buffer.get(nextToConsume);
                consumer.accept(e);
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
        public void forEachRemaining(IntConsumer consumer) {
            // 如果缓冲区为null，且仍然允许继续访问spliterator中的元素
            if(buffer == null && !finished) {
                Objects.requireNonNull(consumer);
                
                // 确保流迭代器已经初始化(准备待遍历数据)
                init();
                
                // 为即将进入的终端阶段构造一个sink
                Sink<Integer> downSink = new Sink<Integer>() {
                    @Override
                    public void accept(Integer e) {
                        consumer.accept(e);
                    }
                };
                
                /*
                 * 从downSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
                 * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
                 * 最终择取出的数据往往被存入了downSink代表的容器当中。
                 *
                 * downSink   : (相对于helper的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
                 * spliterator: 流迭代器，作为数据源，包含了当前所有待访问的元素
                 */
                helper.wrapAndCopyInto(downSink, spliterator);
                
                // 结束访问
                finished = true;
                
                // 缓冲区不为null，或者已经禁止继续访问spliterator中的元素时，直接使用tryAdvance()访问缓冲区中剩余未访问的元素
            } else {
                do {
                } while(tryAdvance(consumer));
            }
        }
        
        // 初始化buffer、bufferSink、pusher，为"部分遍历"做准备
        @Override
        void initPartialTraversalState() {
            
            // 初始化一个弹性缓冲区
            buffer = new SpinedBuffer.OfInt();
            
            // 为即将进入的终端阶段构造一个sink
            Sink.OfInt downSink = new Sink.OfInt() {
                @Override
                public void accept(int e) {
                    buffer.accept(e);
                }
            };
            
            /*
             * 从downSink开始，逆向遍历流，构造并返回(相对于helper阶段的)属于上个(depth==1)的流阶段的sink。
             *
             * 返回的sink与downSink组成一个完整的链条，以便处理属于上个(depth==0)的流阶段输出的数据。
             * 经过该sink链条处理过的数据，会被downSink所在的流阶段输出给downSink的下游阶段。
             *
             * downSink: (相对于helper阶段的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             */
            bufferSink = helper.wrapSink(downSink);
            
            // 初始化一个对spliterator中元素的待执行操作
            pusher = new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() {
                    return spliterator.tryAdvance(bufferSink);
                }
            };
        }
    }
    
    // "包装"流迭代器的的long类型版本，该流迭代器可用来获取helper阶段的输出元素
    static final class LongWrappingSpliterator<P_IN> extends AbstractWrappingSpliterator<P_IN, Long, SpinedBuffer.OfLong> implements Spliterator.OfLong {
        LongWrappingSpliterator(PipelineHelper<Long> ph, Supplier<Spliterator<P_IN>> supplier, boolean parallel) {
            super(ph, supplier, parallel);
        }
        
        LongWrappingSpliterator(PipelineHelper<Long> ph, Spliterator<P_IN> spliterator, boolean parallel) {
            super(ph, spliterator, parallel);
        }
        
        // 使用指定的流迭代器，重新构造一个"包装"流迭代器
        @Override
        AbstractWrappingSpliterator<P_IN, Long, ?> wrap(Spliterator<P_IN> spliterator) {
            return new LongWrappingSpliterator<>(helper, spliterator, isParallel);
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator.OfLong trySplit() {
            return (Spliterator.OfLong) super.trySplit();
        }
        
        /*
         * 使用consumer消费当前流迭代器中的元素。
         * 如果存在未访问的元素可被消费，则返回true；否则，返回false。
         *
         * 注：这里对spliterator进行了包装，即这里消费的元素必须先经过整个流上的sink链的择取。
         */
        @Override
        public boolean tryAdvance(LongConsumer consumer) {
            Objects.requireNonNull(consumer);
            
            /*
             * 判断是否存在未访问过的元素
             *
             * 先在缓冲区中查找，如果缓冲区不为null，且包含未访问过的数据，则返回true。
             * 如果缓冲区为null，或者缓冲区中的数据都被访问过了，
             * 那么需要去spliterator中择取数据，并填充到buffer中。
             * 填充过程结束后，如果找到了新的未访问元素，则依然返回true，否则，返回false。
             */
            boolean hasNext = doAdvance();
            
            // 如果在缓冲区中找到了未访问元素，则获取该元素，并对其进行择取操作
            if(hasNext) {
                long e = buffer.get(nextToConsume);
                consumer.accept(e);
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
        public void forEachRemaining(LongConsumer consumer) {
            if(buffer == null && !finished) {
                Objects.requireNonNull(consumer);
                
                // 确保流迭代器已经初始化(准备待遍历数据)
                init();
                
                // 为即将进入的终端阶段构造一个sink
                Sink<Long> downSink = new Sink<Long>() {
                    @Override
                    public void accept(Long e) {
                        consumer.accept(e);
                    }
                };
                
                /*
                 * 从downSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
                 * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
                 * 最终择取出的数据往往被存入了downSink代表的容器当中。
                 *
                 * downSink   : (相对于helper的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
                 * spliterator: 流迭代器，作为数据源，包含了当前所有待访问的元素
                 */
                helper.wrapAndCopyInto(downSink, spliterator);
                
                // 结束访问
                finished = true;
                
                // 缓冲区不为null，或者已经禁止继续访问spliterator中的元素时，直接使用tryAdvance()访问缓冲区中剩余未访问的元素
            } else {
                while(tryAdvance(consumer)) {
                }
            }
        }
        
        // 初始化buffer、bufferSink、pusher，为"部分遍历"做准备
        @Override
        void initPartialTraversalState() {
            
            // 初始化一个弹性缓冲区
            buffer = new SpinedBuffer.OfLong();
            
            // 为即将进入的终端阶段构造一个sink
            Sink.OfLong downSink = new Sink.OfLong() {
                @Override
                public void accept(long e) {
                    buffer.accept(e);
                }
            };
            
            /*
             * 从downSink开始，逆向遍历流，构造并返回(相对于helper阶段的)属于上个(depth==1)的流阶段的sink。
             *
             * 返回的sink与downSink组成一个完整的链条，以便处理属于上个(depth==0)的流阶段输出的数据。
             * 经过该sink链条处理过的数据，会被downSink所在的流阶段输出给downSink的下游阶段。
             *
             * downSink: (相对于helper阶段的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             */
            bufferSink = helper.wrapSink(downSink);
            
            // 初始化一个对spliterator中元素的待执行操作
            pusher = new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() {
                    return spliterator.tryAdvance(bufferSink);
                }
            };
        }
    }
    
    // "包装"流迭代器的的double类型版本，该流迭代器可用来获取helper阶段的输出元素
    static final class DoubleWrappingSpliterator<P_IN> extends AbstractWrappingSpliterator<P_IN, Double, SpinedBuffer.OfDouble> implements Spliterator.OfDouble {
        DoubleWrappingSpliterator(PipelineHelper<Double> ph, Supplier<Spliterator<P_IN>> supplier, boolean parallel) {
            super(ph, supplier, parallel);
        }
        
        DoubleWrappingSpliterator(PipelineHelper<Double> ph, Spliterator<P_IN> spliterator, boolean parallel) {
            super(ph, spliterator, parallel);
        }
        
        // 使用指定的流迭代器，重新构造一个"包装"流迭代器
        @Override
        AbstractWrappingSpliterator<P_IN, Double, ?> wrap(Spliterator<P_IN> spliterator) {
            return new DoubleWrappingSpliterator<>(helper, spliterator, isParallel);
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator.OfDouble trySplit() {
            return (Spliterator.OfDouble) super.trySplit();
        }
        
        /*
         * 使用consumer消费当前流迭代器中的元素。
         * 如果存在未访问的元素可被消费，则返回true；否则，返回false。
         *
         * 注：这里对spliterator进行了包装，即这里消费的元素必须先经过整个流上的sink链的择取。
         */
        @Override
        public boolean tryAdvance(DoubleConsumer consumer) {
            Objects.requireNonNull(consumer);
            
            /*
             * 判断是否存在未访问过的元素
             *
             * 先在缓冲区中查找，如果缓冲区不为null，且包含未访问过的数据，则返回true。
             * 如果缓冲区为null，或者缓冲区中的数据都被访问过了，
             * 那么需要去spliterator中择取数据，并填充到buffer中。
             * 填充过程结束后，如果找到了新的未访问元素，则依然返回true，否则，返回false。
             */
            boolean hasNext = doAdvance();
            
            // 如果在缓冲区中找到了未访问元素，则获取该元素，并对其进行择取操作
            if(hasNext) {
                double e = buffer.get(nextToConsume);
                consumer.accept(e);
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
        public void forEachRemaining(DoubleConsumer consumer) {
            if(buffer == null && !finished) {
                Objects.requireNonNull(consumer);
                
                // 确保流迭代器已经初始化(准备待遍历数据)
                init();
                
                // 为即将进入的终端阶段构造一个sink
                Sink<Double> downSink = new Sink<Double>() {
                    @Override
                    public void accept(Double e) {
                        consumer.accept(e);
                    }
                };
                
                /*
                 * 从downSink开始，逆向遍历流，构造并返回属于上个(depth==1)的流阶段的sink，
                 * 然后从返回的sink开始，顺着整个sink链条择取来自spliterator中的数据，
                 * 最终择取出的数据往往被存入了downSink代表的容器当中。
                 *
                 * downSink   : (相对于helper的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
                 * spliterator: 流迭代器，作为数据源，包含了当前所有待访问的元素
                 */
                helper.wrapAndCopyInto(downSink, spliterator);
                
                // 结束访问
                finished = true;
                
                // 缓冲区不为null，或者已经禁止继续访问spliterator中的元素时，直接使用tryAdvance()访问缓冲区中剩余未访问的元素
            } else {
                while(tryAdvance(consumer)) {
                }
            }
        }
        
        // 初始化buffer、bufferSink、pusher，为"部分遍历"做准备
        @Override
        void initPartialTraversalState() {
            
            // 初始化一个弹性缓冲区
            buffer = new SpinedBuffer.OfDouble();
            
            // 为即将进入的终端阶段构造一个sink
            Sink.OfDouble downSink = new Sink.OfDouble() {
                @Override
                public void accept(double e) {
                    buffer.accept(e);
                }
            };
            
            /*
             * 从downSink开始，逆向遍历流，构造并返回(相对于helper阶段的)属于上个(depth==1)的流阶段的sink。
             *
             * 返回的sink与downSink组成一个完整的链条，以便处理属于上个(depth==0)的流阶段输出的数据。
             * 经过该sink链条处理过的数据，会被downSink所在的流阶段输出给downSink的下游阶段。
             *
             * downSink: (相对于helper阶段的)下个流阶段的sink。如果downSink位于模拟的终端阶段，则该sink的作用通常是收集数据。
             */
            bufferSink = helper.wrapSink(downSink);
            
            // 初始化一个对spliterator中元素的待执行操作
            pusher = new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() {
                    return spliterator.tryAdvance(bufferSink);
                }
            };
        }
    }
    
    /*▲ "包装"流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    /*▼ "惰性"流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Spliterator implementation that delegates to an underlying spliterator,
     * acquiring the spliterator from a {@code Supplier<Spliterator>} on the
     * first call to any spliterator method.
     *
     * @param <T>
     */
    /*
     * "惰性"流迭代器的引用类型版本
     *
     * "惰性"的含义是使用流迭代器时，需要从流迭代器工厂中获取
     */
    static class DelegatingSpliterator<T, T_SPLITR extends Spliterator<T>> implements Spliterator<T> {
        private final Supplier<? extends T_SPLITR> supplier;    // 流迭代器工厂
        private T_SPLITR spliterator;                           // 流迭代器
        
        DelegatingSpliterator(Supplier<? extends T_SPLITR> supplier) {
            this.supplier = supplier;
        }
        
        // 从流迭代器工厂中获取到流迭代器
        T_SPLITR get() {
            if(spliterator == null) {
                spliterator = supplier.get();
            }
            
            return spliterator;
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        @SuppressWarnings("unchecked")
        public T_SPLITR trySplit() {
            return (T_SPLITR) get().trySplit();
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
            return get().tryAdvance(consumer);
        }
        
        /*
         * 尝试用consumer逐个消费当前流迭代器中所有剩余元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public void forEachRemaining(Consumer<? super T> consumer) {
            get().forEachRemaining(consumer);
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return get().estimateSize();
        }
        
        /*
         * 初始时，尝试返回流迭代器中的元素总量。如果无法获取精确值，则返回-1。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         *
         * 注：通常在流迭代器拥有SIZED参数时可以获取到一个精确值。
         */
        @Override
        public long getExactSizeIfKnown() {
            return get().getExactSizeIfKnown();
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return get().characteristics();
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
            return get().getComparator();
        }
        
        @Override
        public String toString() {
            return getClass().getName() + "[" + get() + "]";
        }
        
        
        // "惰性"流迭代器的数值类型版本
        static class OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends DelegatingSpliterator<T, T_SPLITR> implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            OfPrimitive(Supplier<? extends T_SPLITR> supplier) {
                super(supplier);
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
                return get().tryAdvance(consumer);
            }
            
            /*
             * 尝试用consumer逐个消费当前流迭代器中所有剩余元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public void forEachRemaining(T_CONS consumer) {
                get().forEachRemaining(consumer);
            }
        }
        
        // "惰性"流迭代器的int类型版本
        static final class OfInt extends OfPrimitive<Integer, IntConsumer, Spliterator.OfInt> implements Spliterator.OfInt {
            OfInt(Supplier<Spliterator.OfInt> supplier) {
                super(supplier);
            }
        }
        
        // "惰性"流迭代器的long类型版本
        static final class OfLong extends OfPrimitive<Long, LongConsumer, Spliterator.OfLong> implements Spliterator.OfLong {
            OfLong(Supplier<Spliterator.OfLong> supplier) {
                super(supplier);
            }
        }
        
        // "惰性"流迭代器的double类型版本
        static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, Spliterator.OfDouble> implements Spliterator.OfDouble {
            OfDouble(Supplier<Spliterator.OfDouble> supplier) {
                super(supplier);
            }
        }
    }
    
    /*▲ "惰性"流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    /*▼ "分片"流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * "分片"流迭代器面对的是一个有效元素与无效元素(例如将被跳过的元素)混杂的数据流。
     * 有些片区的元素需要被忽略，而有些片区的元素需要被保留到下一个流中。
     */
    
    /**
     * A slice Spliterator from a source Spliterator that reports
     * {@code SUBSIZED}.
     */
    // "分片"流迭代器的抽象实现
    abstract static class SliceSpliterator<T, T_SPLITR extends Spliterator<T>> {
        
        // The spliterator to slice
        T_SPLITR spliterator;   // 待分割的流迭代器
        
        /*
         * 注：有效元素是指需要保留到下一个流中的元素
         */
        
        // The start index of the slice
        final long sliceOrigin; // 有效元素的起始索引
        // One past the last index of the slice
        final long sliceFence;  // 有效元素的终止索引
        
        // current (absolute) index, modified on advance/split
        long index; // 切片的起始索引
        // one past last (absolute) index or sliceFence, which ever is smaller
        long fence; // 切片的终止索引的上限
        
        SliceSpliterator(T_SPLITR spliterator, long sliceOrigin, long sliceFence, long origin, long fence) {
            assert spliterator.hasCharacteristics(Spliterator.SUBSIZED);
            
            this.spliterator = spliterator;
            this.sliceOrigin = sliceOrigin;
            this.sliceFence = sliceFence;
            this.index = origin;
            this.fence = fence;
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         * 这里切割出的分片数据中，必须包含有效元素，同时也允许掺杂无效元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        public T_SPLITR trySplit() {
            if(sliceOrigin >= fence) {
                return null;
            }
            
            if(index >= fence) {
                return null;
            }
            
            /*
             * Keep splitting until the left and right splits intersect with the slice thereby ensuring the size estimate decreases.
             * This also avoids creating empty spliterators which can result in existing and additionally created F/J tasks that perform redundant work on no elements.
             */
            while(true) {
                @SuppressWarnings("unchecked")
                T_SPLITR leftSplit = (T_SPLITR) spliterator.trySplit();
                if(leftSplit == null) {
                    return null;
                }
                
                long leftSplitFenceUnbounded = index + leftSplit.estimateSize();
                long leftSplitFence = Math.min(leftSplitFenceUnbounded, sliceFence);
                
                if(sliceOrigin >= leftSplitFence) {
                    /*
                     * The left split does not intersect with, and is to the left of, the slice
                     * The right split does intersect
                     * Discard the left split and split further with the right split
                     */
                    index = leftSplitFence;
                } else if(leftSplitFence >= sliceFence) {
                    /*
                     * The right split does not intersect with, and is to the right of, the slice
                     * The left split does intersect
                     * Discard the right split and split further with the left split
                     */
                    spliterator = leftSplit;
                    fence = leftSplitFence;
                } else if(index >= sliceOrigin && leftSplitFenceUnbounded<=sliceFence) {
                    /*
                     * The left split is contained within the slice, return the underlying left split
                     * Right split is contained within or intersects with the slice
                     */
                    index = leftSplitFence;
                    return leftSplit;
                } else {
                    /*
                     * The left split intersects with the slice
                     * Right split is contained within or intersects with the slice
                     */
                    return makeSpliterator(leftSplit, sliceOrigin, sliceFence, index, index = leftSplitFence);
                }
            }
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        public long estimateSize() {
            return (sliceOrigin<fence) ? fence - Math.max(sliceOrigin, index) : 0;
        }
        
        // 返回流迭代器的参数
        public int characteristics() {
            return spliterator.characteristics();
        }
        
        // 根据给定的参数构造一个子Spliterator
        protected abstract T_SPLITR makeSpliterator(T_SPLITR spliterator, long sliceOrigin, long sliceFence, long origin, long fence);
        
        
        // "分片"流迭代器(引用类型版本)
        static final class OfRef<T> extends SliceSpliterator<T, Spliterator<T>> implements Spliterator<T> {
            OfRef(Spliterator<T> spliterator, long sliceOrigin, long sliceFence) {
                this(spliterator, sliceOrigin, sliceFence, 0, Math.min(spliterator.estimateSize(), sliceFence));
            }
            
            private OfRef(Spliterator<T> spliterator, long sliceOrigin, long sliceFence, long origin, long fence) {
                super(spliterator, sliceOrigin, sliceFence, origin, fence);
            }
            
            /*
             * 尝试用action消费当前流迭代器中下一个元素。
             * 返回值指示是否找到了下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                
                if(sliceOrigin >= fence) {
                    return false;
                }
                
                // 跳过需要跳过的元素
                while(sliceOrigin>index) {
                    spliterator.tryAdvance(e -> {});
                    index++;
                }
                
                if(index >= fence) {
                    return false;
                }
                
                // 标记下一个元素被访问了
                index++;
                
                // 访问下一个元素
                return spliterator.tryAdvance(action);
            }
            
            /*
             * 尝试用action逐个消费当前流迭代器中所有剩余元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                
                if(sliceOrigin >= fence) {
                    return;
                }
                
                if(index >= fence) {
                    return;
                }
                
                // 如果当前切片中的数据均在有效元素范围内，则可以直接遍历
                if(index >= sliceOrigin && (index + spliterator.estimateSize())<=sliceFence) {
                    // The spliterator is contained within the slice
                    spliterator.forEachRemaining(action);
                    index = fence;
                    
                    // 如果当前切片中掺杂了无效元素，则需要跳过这些无效元素
                } else {
                    // 跳过无效元素
                    while(sliceOrigin>index) {
                        spliterator.tryAdvance(e -> {});
                        index++;
                    }
                    
                    // 遍历有效元素
                    for(; index<fence; index++) {
                        spliterator.tryAdvance(action);
                    }
                }
            }
            
            // 根据给定的参数构造一个子Spliterator
            @Override
            protected Spliterator<T> makeSpliterator(Spliterator<T> spliterator, long sliceOrigin, long sliceFence, long origin, long fence) {
                return new OfRef<>(spliterator, sliceOrigin, sliceFence, origin, fence);
            }
        }
        
        // "分片"流迭代器(基本数值类型版本)
        abstract static class OfPrimitive<T, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_CONS> extends SliceSpliterator<T, T_SPLITR> implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            OfPrimitive(T_SPLITR spliterator, long sliceOrigin, long sliceFence) {
                this(spliterator, sliceOrigin, sliceFence, 0, Math.min(spliterator.estimateSize(), sliceFence));
            }
            
            private OfPrimitive(T_SPLITR spliterator, long sliceOrigin, long sliceFence, long origin, long fence) {
                super(spliterator, sliceOrigin, sliceFence, origin, fence);
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
                Objects.requireNonNull(action);
                
                if(sliceOrigin >= fence) {
                    return false;
                }
                
                // 跳过需要跳过的元素
                while(sliceOrigin>index) {
                    spliterator.tryAdvance(emptyConsumer());
                    index++;
                }
                
                if(index >= fence) {
                    return false;
                }
                
                // 标记下一个元素被访问了
                index++;
                
                // 访问下一个元素
                return spliterator.tryAdvance(action);
            }
            
            /*
             * 尝试用action逐个消费当前流迭代器中所有剩余元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public void forEachRemaining(T_CONS action) {
                Objects.requireNonNull(action);
                
                if(sliceOrigin >= fence) {
                    return;
                }
                
                if(index >= fence) {
                    return;
                }
                
                // 如果当前切片中的数据均在有效元素范围内，则可以直接遍历
                if(index >= sliceOrigin && (index + spliterator.estimateSize())<=sliceFence) {
                    // The spliterator is contained within the slice
                    spliterator.forEachRemaining(action);
                    index = fence;
                    
                    // 如果当前切片中掺杂了无效元素，则需要跳过这些无效元素
                } else {
                    // 跳过无效元素
                    while(sliceOrigin>index) {
                        spliterator.tryAdvance(emptyConsumer());
                        index++;
                    }
                    
                    // 遍历有效元素
                    for(; index<fence; index++) {
                        spliterator.tryAdvance(action);
                    }
                }
            }
            
            // 一个无实质效果的消费操作，用来消费那些无效的元素
            protected abstract T_CONS emptyConsumer();
        }
        
        // "分片"流迭代器(int类型版本)
        static final class OfInt extends OfPrimitive<Integer, Spliterator.OfInt, IntConsumer> implements Spliterator.OfInt {
            OfInt(Spliterator.OfInt spliterator, long sliceOrigin, long sliceFence) {
                super(spliterator, sliceOrigin, sliceFence);
            }
            
            OfInt(Spliterator.OfInt spliterator, long sliceOrigin, long sliceFence, long origin, long fence) {
                super(spliterator, sliceOrigin, sliceFence, origin, fence);
            }
            
            // 根据给定的参数构造一个子Spliterator
            @Override
            protected Spliterator.OfInt makeSpliterator(Spliterator.OfInt spliterator, long sliceOrigin, long sliceFence, long origin, long fence) {
                return new SliceSpliterator.OfInt(spliterator, sliceOrigin, sliceFence, origin, fence);
            }
            
            // 一个无实质效果的消费操作，用来消费那些无效的元素
            @Override
            protected IntConsumer emptyConsumer() {
                return e -> {};
            }
        }
        
        // "分片"流迭代器(long类型版本)
        static final class OfLong extends OfPrimitive<Long, Spliterator.OfLong, LongConsumer> implements Spliterator.OfLong {
            OfLong(Spliterator.OfLong spliterator, long sliceOrigin, long sliceFence) {
                super(spliterator, sliceOrigin, sliceFence);
            }
            
            OfLong(Spliterator.OfLong spliterator, long sliceOrigin, long sliceFence, long origin, long fence) {
                super(spliterator, sliceOrigin, sliceFence, origin, fence);
            }
            
            // 根据给定的参数构造一个子Spliterator
            @Override
            protected Spliterator.OfLong makeSpliterator(Spliterator.OfLong spliterator, long sliceOrigin, long sliceFence, long origin, long fence) {
                return new SliceSpliterator.OfLong(spliterator, sliceOrigin, sliceFence, origin, fence);
            }
            
            // 一个无实质效果的消费操作，用来消费那些无效的元素
            @Override
            protected LongConsumer emptyConsumer() {
                return e -> {};
            }
        }
        
        // "分片"流迭代器(double类型版本)
        static final class OfDouble extends OfPrimitive<Double, Spliterator.OfDouble, DoubleConsumer> implements Spliterator.OfDouble {
            OfDouble(Spliterator.OfDouble spliterator, long sliceOrigin, long sliceFence) {
                super(spliterator, sliceOrigin, sliceFence);
            }
            
            OfDouble(Spliterator.OfDouble spliterator, long sliceOrigin, long sliceFence, long origin, long fence) {
                super(spliterator, sliceOrigin, sliceFence, origin, fence);
            }
            
            // 根据给定的参数构造一个子Spliterator
            @Override
            protected Spliterator.OfDouble makeSpliterator(Spliterator.OfDouble spliterator, long sliceOrigin, long sliceFence, long origin, long fence) {
                return new SliceSpliterator.OfDouble(spliterator, sliceOrigin, sliceFence, origin, fence);
            }
            
            // 一个无实质效果的消费操作，用来消费那些无效的元素
            @Override
            protected DoubleConsumer emptyConsumer() {
                return e -> {};
            }
        }
    }
    
    /*▲ "分片"流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    /*▼ "无序"流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * A slice Spliterator that does not preserve order, if any, of a source Spliterator.
     *
     * Note: The source spliterator may report {@code ORDERED} since that
     * spliterator be the result of a previous pipeline stage that was
     * collected to a {@code Node}. It is the order of the pipeline stage
     * that governs whether this slice spliterator is to be used or not.
     */
    // "无序"流迭代器的抽象实现
    abstract static class UnorderedSliceSpliterator<T, T_SPLITR extends Spliterator<T>> {
        
        static final int CHUNK_SIZE = 1 << 7;
        
        protected final int chunkSize;
        
        // The spliterator to slice
        protected final T_SPLITR spliterator;
        
        protected final boolean unlimited;  // 保存到下一个流中的数据是否无限制
        private final long skipThreshold;   // 待跳过元素的上限，0表示未知
        private final AtomicLong permits;
        
        UnorderedSliceSpliterator(T_SPLITR spliterator, long skip, long limit) {
            this.spliterator = spliterator;
            this.unlimited = limit<0;
            this.skipThreshold = limit >= 0 ? limit : 0;
            this.chunkSize = limit >= 0 ? (int) Math.min(CHUNK_SIZE, ((skip + limit) / AbstractTask.getLeafTarget()) + 1) : CHUNK_SIZE;
            this.permits = new AtomicLong(limit >= 0 ? skip + limit : skip);
        }
        
        UnorderedSliceSpliterator(T_SPLITR spliterator, UnorderedSliceSpliterator<T, T_SPLITR> parent) {
            this.spliterator = spliterator;
            this.unlimited = parent.unlimited;
            this.permits = parent.permits;
            this.skipThreshold = parent.skipThreshold;
            this.chunkSize = parent.chunkSize;
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        public final T_SPLITR trySplit() {
            // Stop splitting when there are no more limit permits
            if(permits.get() == 0) {
                return null;
            }
            
            @SuppressWarnings("unchecked")
            T_SPLITR childSpliterator = (T_SPLITR) spliterator.trySplit();
            if(childSpliterator == null) {
                return null;
            }
            
            return makeSpliterator(childSpliterator);
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        public final long estimateSize() {
            return spliterator.estimateSize();
        }
        
        // 返回流迭代器的参数
        public final int characteristics() {
            return spliterator.characteristics() & ~(Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED);
        }
        
        // 构造一个子迭代器
        protected abstract T_SPLITR makeSpliterator(T_SPLITR spliterator);
        
        /**
         * Acquire permission to skip or process elements.  The caller must
         * first acquire the elements, then consult this method for guidance
         * as to what to do with the data.
         *
         * <p>We use an {@code AtomicLong} to atomically maintain a counter,
         * which is initialized as skip+limit if we are limiting, or skip only
         * if we are not limiting.  The user should consult the method
         * {@code checkPermits()} before acquiring data elements.
         *
         * @param numElements the number of elements the caller has in hand
         *
         * @return the number of elements that should be processed; any
         * remaining elements should be discarded.
         */
        // 申请许可证，以便跳过或处理元素
        protected final long acquirePermits(long numElements) {
            long remainingPermits;
            long grabbing;
    
            // permits never increase, and don't decrease below zero
            assert numElements>0;
    
            do {
                remainingPermits = permits.get();
                if(remainingPermits == 0) {
                    return unlimited ? numElements : 0;
                }
        
                grabbing = Math.min(remainingPermits, numElements);
            } while(grabbing>0 && !permits.compareAndSet(remainingPermits, remainingPermits - grabbing));
    
            if(unlimited) {
                return Math.max(numElements - grabbing, 0);
            }
    
            if(remainingPermits>skipThreshold) {
                return Math.max(grabbing - (remainingPermits - skipThreshold), 0);
            }
    
            return grabbing;
        }
        
        /** Call to check if permits might be available before acquiring data */
        protected final PermitStatus permitStatus() {
            if(permits.get()>0) {
                return PermitStatus.MAYBE_MORE;
            }
    
            if(unlimited) {
                return PermitStatus.UNLIMITED;
            }
    
            return PermitStatus.NO_MORE;
        }
        
        
        enum PermitStatus {
            NO_MORE, MAYBE_MORE, UNLIMITED
        }
        
        
        // "无序"流迭代器(引用类型版本)
        static final class OfRef<T> extends UnorderedSliceSpliterator<T, Spliterator<T>> implements Spliterator<T>, Consumer<T> {
            T tmpSlot;
            
            OfRef(Spliterator<T> spliterator, long skip, long limit) {
                super(spliterator, skip, limit);
            }
            
            OfRef(Spliterator<T> spliterator, OfRef<T> parent) {
                super(spliterator, parent);
            }
            
            // 保存上次遍历的元素，可能有效，也可能无效
            @Override
            public final void accept(T t) {
                tmpSlot = t;
            }
            
            /*
             * 尝试用action消费当前流迭代器中下一个元素。
             * 返回值指示是否找到了下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                
                while(permitStatus() != PermitStatus.NO_MORE) {
                    // 消费(存储)一个元素
                    if(!spliterator.tryAdvance(this)) {
                        return false;
                    }
                    
                    // 申请一个许可，申请成功的话，则可以访问有效元素
                    if(acquirePermits(1) == 1) {
                        action.accept(tmpSlot);
                        tmpSlot = null;
                        return true;
                    }
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
            public void forEachRemaining(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                
                ArrayBuffer.OfRef<T> sb = null;
                PermitStatus permitStatus;
                
                while((permitStatus = permitStatus()) != PermitStatus.NO_MORE) {
                    if(permitStatus == PermitStatus.MAYBE_MORE) {
                        // Optimistically traverse elements up to a threshold of chunkSize
                        if(sb == null) {
                            sb = new ArrayBuffer.OfRef<>(chunkSize);
                        } else {
                            sb.reset();
                        }
                        
                        long permitsRequested = 0;
                        do {
                        } while(spliterator.tryAdvance(sb) && ++permitsRequested<chunkSize);
                        
                        if(permitsRequested == 0) {
                            return;
                        }
                        
                        sb.forEach(action, acquirePermits(permitsRequested));
                    } else {
                        // Must be UNLIMITED; let 'er rip
                        spliterator.forEachRemaining(action);
                        return;
                    }
                }
            }
            
            // 构造一个子迭代器
            @Override
            protected Spliterator<T> makeSpliterator(Spliterator<T> spliterator) {
                return new UnorderedSliceSpliterator.OfRef<>(spliterator, this);
            }
        }
        
        /**
         * Concrete sub-types must also be an instance of type {@code T_CONS}.
         *
         * @param <T_BUFF> the type of the spined buffer. Must also be a type of
         *                 {@code T_CONS}.
         */
        // "无序"流迭代器(基本数值类型版本)
        abstract static class OfPrimitive<T, T_CONS, T_BUFF extends ArrayBuffer.OfPrimitive<T_CONS>, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends UnorderedSliceSpliterator<T, T_SPLITR> implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            OfPrimitive(T_SPLITR spliterator, long skip, long limit) {
                super(spliterator, skip, limit);
            }
            
            OfPrimitive(T_SPLITR spliterator, UnorderedSliceSpliterator.OfPrimitive<T, T_CONS, T_BUFF, T_SPLITR> parent) {
                super(spliterator, parent);
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
                Objects.requireNonNull(action);
                @SuppressWarnings("unchecked")
                T_CONS consumer = (T_CONS) this;
                
                while(permitStatus() != PermitStatus.NO_MORE) {
                    if(!spliterator.tryAdvance(consumer)) {
                        return false;
                    }
                    
                    if(acquirePermits(1) == 1) {
                        acceptConsumed(action);
                        return true;
                    }
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
            public void forEachRemaining(T_CONS action) {
                Objects.requireNonNull(action);
                
                T_BUFF sb = null;
                PermitStatus permitStatus;
                while((permitStatus = permitStatus()) != PermitStatus.NO_MORE) {
                    if(permitStatus == PermitStatus.MAYBE_MORE) {
                        // Optimistically traverse elements up to a threshold of chunkSize
                        if(sb == null) {
                            sb = bufferCreate(chunkSize);
                        } else {
                            sb.reset();
                        }
                        
                        @SuppressWarnings("unchecked")
                        T_CONS sbc = (T_CONS) sb;
                        
                        long permitsRequested = 0;
                        
                        do {
                        } while(spliterator.tryAdvance(sbc) && ++permitsRequested<chunkSize);
                        
                        if(permitsRequested == 0) {
                            return;
                        }
                        
                        sb.forEach(action, acquirePermits(permitsRequested));
                    } else {
                        // Must be UNLIMITED; let 'er rip
                        spliterator.forEachRemaining(action);
                        return;
                    }
                }
            }
            
            // 消费有效元素
            protected abstract void acceptConsumed(T_CONS action);
            
            // 构造缓冲区
            protected abstract T_BUFF bufferCreate(int initialCapacity);
        }
        
        // "无序"流迭代器(int类型版本)
        static final class OfInt extends OfPrimitive<Integer, IntConsumer, ArrayBuffer.OfInt, Spliterator.OfInt> implements Spliterator.OfInt, IntConsumer {
            
            int tmpValue;
            
            OfInt(Spliterator.OfInt spliterator, long skip, long limit) {
                super(spliterator, skip, limit);
            }
            
            OfInt(Spliterator.OfInt spliterator, UnorderedSliceSpliterator.OfInt parent) {
                super(spliterator, parent);
            }
            
            // 保存上次遍历的元素，可能有效，也可能无效
            @Override
            public void accept(int value) {
                tmpValue = value;
            }
            
            // 消费有效元素
            @Override
            protected void acceptConsumed(IntConsumer action) {
                action.accept(tmpValue);
            }
            
            // 构造缓冲区
            @Override
            protected ArrayBuffer.OfInt bufferCreate(int initialCapacity) {
                return new ArrayBuffer.OfInt(initialCapacity);
            }
            
            // 构造一个子迭代器
            @Override
            protected Spliterator.OfInt makeSpliterator(Spliterator.OfInt spliterator) {
                return new UnorderedSliceSpliterator.OfInt(spliterator, this);
            }
        }
        
        // "无序"流迭代器(long类型版本)
        static final class OfLong extends OfPrimitive<Long, LongConsumer, ArrayBuffer.OfLong, Spliterator.OfLong> implements Spliterator.OfLong, LongConsumer {
            
            long tmpValue;
            
            OfLong(Spliterator.OfLong spliterator, long skip, long limit) {
                super(spliterator, skip, limit);
            }
            
            OfLong(Spliterator.OfLong spliterator, UnorderedSliceSpliterator.OfLong parent) {
                super(spliterator, parent);
            }
            
            // 保存上次遍历的元素，可能有效，也可能无效
            @Override
            public void accept(long value) {
                tmpValue = value;
            }
            
            // 消费有效元素
            @Override
            protected void acceptConsumed(LongConsumer action) {
                action.accept(tmpValue);
            }
            
            // 构造缓冲区
            @Override
            protected ArrayBuffer.OfLong bufferCreate(int initialCapacity) {
                return new ArrayBuffer.OfLong(initialCapacity);
            }
            
            // 构造一个子迭代器
            @Override
            protected Spliterator.OfLong makeSpliterator(Spliterator.OfLong spliterator) {
                return new UnorderedSliceSpliterator.OfLong(spliterator, this);
            }
        }
        
        // "无序"流迭代器(double类型版本)
        static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, ArrayBuffer.OfDouble, Spliterator.OfDouble> implements Spliterator.OfDouble, DoubleConsumer {
            
            double tmpValue;
            
            OfDouble(Spliterator.OfDouble spliterator, long skip, long limit) {
                super(spliterator, skip, limit);
            }
            
            OfDouble(Spliterator.OfDouble spliterator, UnorderedSliceSpliterator.OfDouble parent) {
                super(spliterator, parent);
            }
            
            // 保存上次遍历的元素，可能有效，也可能无效
            @Override
            public void accept(double value) {
                tmpValue = value;
            }
            
            // 消费有效元素
            @Override
            protected void acceptConsumed(DoubleConsumer action) {
                action.accept(tmpValue);
            }
            
            // 构造缓冲区
            @Override
            protected ArrayBuffer.OfDouble bufferCreate(int initialCapacity) {
                return new ArrayBuffer.OfDouble(initialCapacity);
            }
            
            // 构造一个子迭代器
            @Override
            protected Spliterator.OfDouble makeSpliterator(Spliterator.OfDouble spliterator) {
                return new UnorderedSliceSpliterator.OfDouble(spliterator, this);
            }
        }
    }
    
    /*▲ "无序"流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    /*▼ "去重"流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * A wrapping spliterator that only reports distinct elements of the
     * underlying spliterator. Does not preserve size and encounter order.
     */
    // "去重"流迭代器
    static final class DistinctSpliterator<T> implements Spliterator<T>, Consumer<T> {
        
        // The value to represent null in the ConcurrentHashMap
        private static final Object NULL_VALUE = new Object();
        
        // The underlying spliterator
        private final Spliterator<T> spliterator;   // 包含了待去重的数据源
        
        // ConcurrentHashMap holding distinct elements as keys
        private final ConcurrentHashMap<T, Boolean> seen;   // 持有不重复的元素
        
        // Temporary element, only used with tryAdvance
        private T tmpSlot;  // 记录刚刚访问过的元素，该元素可能已经被操作过，也可能没被操作过
        
        DistinctSpliterator(Spliterator<T> spliterator) {
            this(spliterator, new ConcurrentHashMap<>());
        }
        
        private DistinctSpliterator(Spliterator<T> spliterator, ConcurrentHashMap<T, Boolean> seen) {
            this.spliterator = spliterator;
            this.seen = seen;
        }
        
        /*
         * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：子Spliterator的参数可能发生改变
         */
        @Override
        public Spliterator<T> trySplit() {
            Spliterator<T> split = spliterator.trySplit();
            
            // 每次分割时，还需要带着之前的访问记忆seen
            return (split != null) ? new DistinctSpliterator<>(split, seen) : null;
        }
        
        // 消费(暂存)遇到的元素
        @Override
        public void accept(T t) {
            this.tmpSlot = t;
        }
        
        /*
         * 尝试用action消费当前流迭代器中下一个元素。
         * 返回值指示是否找到了下一个元素。
         *
         * 注1：该操作可能会引起内部游标的变化
         * 注2：该操作可能会顺着sink链向下游传播
         */
        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            // 访问下一个元素
            while(spliterator.tryAdvance(this)) {
                // 如果该元素还未处理过
                if(seen.putIfAbsent(mapNull(tmpSlot), Boolean.TRUE) == null) {
                    // 处理当前元素
                    action.accept(tmpSlot);
                    tmpSlot = null;
                    return true;
                }
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
        public void forEachRemaining(Consumer<? super T> action) {
            spliterator.forEachRemaining(t -> {
                if(seen.putIfAbsent(mapNull(t), Boolean.TRUE) == null) {
                    action.accept(t);
                }
            });
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return spliterator.estimateSize();
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return (spliterator.characteristics() & ~(Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.SORTED | Spliterator.ORDERED)) | Spliterator.DISTINCT;
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
            return spliterator.getComparator();
        }
        
        // 映射指定的元素，对null值特殊处理
        @SuppressWarnings("unchecked")
        private T mapNull(T e) {
            return e != null ? e : (T) NULL_VALUE;
        }
    }
    
    /*▲ "去重"流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    /*▼ "无限"流迭代器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * "无限"流迭代器中的元素数量是无限的，且数据由Supplier提供。
     */
    
    /**
     * A Spliterator that infinitely supplies elements in no particular order.
     *
     * <p>Splitting divides the estimated size in two and stops when the
     * estimate size is 0.
     *
     * <p>The {@code forEachRemaining} method if invoked will never terminate.
     * The {@code tryAdvance} method always returns true.
     */
    // "无限"流迭代器的抽象实现
    abstract static class InfiniteSupplyingSpliterator<T> implements Spliterator<T> {
        long estimate;
        
        protected InfiniteSupplyingSpliterator(long estimate) {
            this.estimate = estimate;
        }
        
        /*
         * 初始时，返回流迭代器中的元素总量(可能不精确)。
         * 如果数据量无限、未知、计算成本过高，则可以返回Long.MAX_VALUE。
         * 当访问过流迭代器中的元素后，此处的返回值可能是元素总量，也可能是剩余未访问的元素数量，依实现而定。
         */
        @Override
        public long estimateSize() {
            return estimate;
        }
        
        // 返回流迭代器的参数
        @Override
        public int characteristics() {
            return IMMUTABLE;
        }
        
        
        // "无限"流迭代器(引用类型版本)
        static final class OfRef<T> extends InfiniteSupplyingSpliterator<T> {
            final Supplier<? extends T> supplier;
            
            OfRef(long size, Supplier<? extends T> supplier) {
                super(size);
                this.supplier = supplier;
            }
            
            /*
             * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：子Spliterator的参数可能发生改变
             */
            @Override
            public Spliterator<T> trySplit() {
                if(estimate == 0) {
                    return null;
                }
                
                return new InfiniteSupplyingSpliterator.OfRef<>(estimate >>>= 1, supplier);
            }
            
            /*
             * 尝试用action消费当前流迭代器中下一个元素。
             * 返回值指示是否找到了下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                
                action.accept(supplier.get());
                
                return true;
            }
        }
        
        // "无限"流迭代器(int类型版本)
        static final class OfInt extends InfiniteSupplyingSpliterator<Integer> implements Spliterator.OfInt {
            final IntSupplier supplier;
            
            OfInt(long size, IntSupplier supplier) {
                super(size);
                this.supplier = supplier;
            }
            
            /*
             * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：子Spliterator的参数可能发生改变
             */
            @Override
            public Spliterator.OfInt trySplit() {
                if(estimate == 0) {
                    return null;
                }
                
                return new InfiniteSupplyingSpliterator.OfInt(estimate = estimate >>> 1, supplier);
            }
            
            /*
             * 尝试用action消费当前流迭代器中下一个元素。
             * 返回值指示是否找到了下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action);
                
                action.accept(supplier.getAsInt());
                
                return true;
            }
        }
        
        // "无限"流迭代器(long类型版本)
        static final class OfLong extends InfiniteSupplyingSpliterator<Long> implements Spliterator.OfLong {
            final LongSupplier supplier;
            
            OfLong(long size, LongSupplier supplier) {
                super(size);
                this.supplier = supplier;
            }
            
            /*
             * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：子Spliterator的参数可能发生改变
             */
            @Override
            public Spliterator.OfLong trySplit() {
                if(estimate == 0) {
                    return null;
                }
                
                return new InfiniteSupplyingSpliterator.OfLong(estimate = estimate >>> 1, supplier);
            }
            
            /*
             * 尝试用action消费当前流迭代器中下一个元素。
             * 返回值指示是否找到了下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(LongConsumer action) {
                Objects.requireNonNull(action);
                
                action.accept(supplier.getAsLong());
                
                return true;
            }
        }
        
        // "无限"流迭代器(double类型版本)
        static final class OfDouble extends InfiniteSupplyingSpliterator<Double> implements Spliterator.OfDouble {
            final DoubleSupplier supplier;
            
            OfDouble(long size, DoubleSupplier supplier) {
                super(size);
                this.supplier = supplier;
            }
            
            /*
             * 返回子Spliterator，该子Spliterator内持有原Spliterator的部分数据。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：子Spliterator的参数可能发生改变
             */
            @Override
            public Spliterator.OfDouble trySplit() {
                if(estimate == 0) {
                    return null;
                }
                
                return new InfiniteSupplyingSpliterator.OfDouble(estimate = estimate >>> 1, supplier);
            }
            
            /*
             * 尝试用action消费当前流迭代器中下一个元素。
             * 返回值指示是否找到了下一个元素。
             *
             * 注1：该操作可能会引起内部游标的变化
             * 注2：该操作可能会顺着sink链向下游传播
             */
            @Override
            public boolean tryAdvance(DoubleConsumer action) {
                Objects.requireNonNull(action);
                
                action.accept(supplier.getAsDouble());
                
                return true;
            }
        }
    }
    
    /*▲ "无限"流迭代器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 用于"无序"流迭代器的一个线性存储
    abstract static class ArrayBuffer {
        int index;
        
        void reset() {
            index = 0;
        }
        
        static final class OfRef<T> extends ArrayBuffer implements Consumer<T> {
            final Object[] array;
            
            OfRef(int size) {
                this.array = new Object[size];
            }
            
            @Override
            public void accept(T t) {
                array[index++] = t;
            }
            
            public void forEach(Consumer<? super T> action, long fence) {
                for(int i = 0; i<fence; i++) {
                    @SuppressWarnings("unchecked")
                    T t = (T) array[i];
                    action.accept(t);
                }
            }
        }
        
        abstract static class OfPrimitive<T_CONS> extends ArrayBuffer {
            int index;
            
            abstract void forEach(T_CONS action, long fence);
            
            @Override
            void reset() {
                index = 0;
            }
        }
        
        static final class OfInt extends OfPrimitive<IntConsumer> implements IntConsumer {
            final int[] array;
            
            OfInt(int size) {
                this.array = new int[size];
            }
            
            @Override
            public void accept(int t) {
                array[index++] = t;
            }
            
            @Override
            public void forEach(IntConsumer action, long fence) {
                for(int i = 0; i<fence; i++) {
                    action.accept(array[i]);
                }
            }
        }
        
        static final class OfLong extends OfPrimitive<LongConsumer> implements LongConsumer {
            final long[] array;
            
            OfLong(int size) {
                this.array = new long[size];
            }
            
            @Override
            public void accept(long t) {
                array[index++] = t;
            }
            
            @Override
            public void forEach(LongConsumer action, long fence) {
                for(int i = 0; i<fence; i++) {
                    action.accept(array[i]);
                }
            }
        }
        
        static final class OfDouble extends OfPrimitive<DoubleConsumer> implements DoubleConsumer {
            final double[] array;
            
            OfDouble(int size) {
                this.array = new double[size];
            }
            
            @Override
            public void accept(double t) {
                array[index++] = t;
            }
            
            @Override
            void forEach(DoubleConsumer action, long fence) {
                for(int i = 0; i<fence; i++) {
                    action.accept(array[i]);
                }
            }
        }
    }
    
}

