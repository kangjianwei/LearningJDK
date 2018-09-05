/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio;

import java.util.Spliterator;
import java.util.function.IntConsumer;

/**
 * A Spliterator.OfInt for sources that traverse and split elements maintained in a CharBuffer.
 *
 * @implNote The implementation is based on the code for the Array-based spliterators.
 */

// 专用Spliterator，应用于字符序列的流中
class CharBufferSpliterator implements Spliterator.OfInt {
    private final CharBuffer buffer;    // 字符缓冲区
    private final int limit;
    private int index;                  // 当前元素索引，会被advance/split改变
    
    CharBufferSpliterator(CharBuffer buffer) {
        this(buffer, buffer.position(), buffer.limit());
    }
    
    CharBufferSpliterator(CharBuffer buffer, int origin, int limit) {
        assert origin<=limit;
        this.buffer = buffer;
        this.index = (origin<=limit) ? origin : limit;
        this.limit = limit;
    }
    
    // 采用折半分割
    @Override
    public OfInt trySplit() {
        int lo = index, mid = (lo + limit) >>> 1;
        return (lo >= mid)
            ? null
            : new CharBufferSpliterator(buffer, lo, index = mid);
    }
    
    // 遍历元素，执行择取操作
    @Override
    public void forEachRemaining(IntConsumer action) {
        if(action == null)
            throw new NullPointerException();
        CharBuffer cb = buffer;
        int i = index;
        int hi = limit;
        index = hi;
        while(i<hi) {
            action.accept(cb.getUnchecked(i++));
        }
    }
    
    // 对当前元素执行择取操作
    @Override
    public boolean tryAdvance(IntConsumer action) {
        if(action == null)
            throw new NullPointerException();
        if(index >= 0 && index<limit) {
            action.accept(buffer.getUnchecked(index++));
            return true;
        }
        return false;
    }
    
    // 返回元素数量
    @Override
    public long estimateSize() {
        return (long) (limit - index);
    }
    
    // 返回该Buffer的特征值
    @Override
    public int characteristics() {
        return Buffer.SPLITERATOR_CHARACTERISTICS;
    }
}
