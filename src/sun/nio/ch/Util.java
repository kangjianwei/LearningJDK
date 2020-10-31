/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import jdk.internal.misc.TerminatingThreadLocal;
import jdk.internal.misc.Unsafe;
import jdk.internal.ref.Cleaner;
import sun.security.action.GetPropertyAction;

// 用于各类通道操作中工具类，主要负责缓冲区操作与对齐检查，
public class Util {
    
    /* The number of temp buffers in our pool */
    // Buffer缓冲池容量
    private static final int TEMP_BUF_POOL_SIZE = IOUtil.IOV_MAX;
    
    /* The max size allowed for a cached temp buffer, in bytes */
    // Buffer缓冲池中缓存的Buffer被允许的最大尺寸(bytes)
    private static final long MAX_CACHED_BUFFER_SIZE = getMaxCachedBufferSize();
    
    /* Per-thread cache of temporary direct buffers */
    // 线程私有的Buffer缓冲池
    private static ThreadLocal<BufferCache> bufferCache = new TerminatingThreadLocal<>() {
        // 初始化一个Buffer缓冲池
        @Override
        protected BufferCache initialValue() {
            return new BufferCache();
        }
        
        // 当前线程结束前，完成对Buffer缓冲池的清理
        @Override
        protected void threadTerminated(BufferCache cache) { // will never be null
            while(!cache.isEmpty()) {
                // 获取Buffer缓冲池首位处的Direct Buffer
                ByteBuffer bb = cache.removeFirst();
                // 释放直接缓存区的内存
                free(bb);
            }
        }
    };
    
    private static Unsafe unsafe = Unsafe.getUnsafe();
    
    // 内存分页大小
    private static int pageSize = -1;
    
    // 一个可读写、直接缓冲区的构造器
    private static volatile Constructor<?> directByteBufferConstructor;
    
    // 一个只读、直接缓冲区的构造器
    private static volatile Constructor<?> directByteBufferRConstructor;
    
    
    
    /*▼ 获取直接缓冲区 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a temporary buffer of at least the given size
     */
    // 获取一块容量至少为size个字节的直接缓冲区
    public static ByteBuffer getTemporaryDirectBuffer(int size) {
        /*
         * If a buffer of this size is too large for the cache,
         * there should not be a buffer in the cache that is at least as large.
         * So we'll just create a new one.
         * Also, we don't have to remove the buffer from the cache (as this method does below) given that we won't put the new buffer in the cache.
         */
        
        // 如果给定的尺寸超过了Buffer缓冲池对Buffer尺寸的约束
        if(isBufferTooLarge(size)) {
            /* 此时，无法从Buffer缓存池中取到合适的Buffer */
            
            // 创建直接内存缓冲区DirectByteBuffer
            return ByteBuffer.allocateDirect(size);
        }
        
        // 获取当前线程私有的Buffer缓冲池
        BufferCache cache = bufferCache.get();
        
        // 从Buffer缓冲池中取出一个容量至少为size的Direct Buffer
        ByteBuffer buf = cache.get(size);
        if(buf != null) {
            // 如果取到了合适的Buffer
            return buf;
        }
        
        /*
         * No suitable buffer in the cache so we need to allocate a new one.
         * To avoid the cache growing then we remove the first buffer from the cache and free it.
         */
        if(!cache.isEmpty()) {
            // 获取Buffer缓冲池首位处的Direct Buffer，并释放其内存
            buf = cache.removeFirst();
            free(buf);
        }
        
        // 创建直接内存缓冲区DirectByteBuffer
        return ByteBuffer.allocateDirect(size);
    }
    
    /**
     * Returns a temporary buffer of at least the given size and aligned to the alignment
     */
    // 获取一块容量至少为size个字节的直接缓冲区（限定了对齐单元的尺寸为alignment）
    public static ByteBuffer getTemporaryAlignedDirectBuffer(int size, int alignment) {
        // 如果给定的尺寸超过了Buffer缓冲池对Buffer尺寸的约束
        if(isBufferTooLarge(size)) {
            /* 此时，无法从Buffer缓存池中取到合适的Buffer */
            
            /*
             * 创建直接内存缓冲区DirectByteBuffer
             * 多分配了alignment - 1字节的空间，已待后续字节对齐校准
             */
            ByteBuffer direct = ByteBuffer.allocateDirect(size + alignment - 1);
            
            // 切片，需要先以unitSize为基准，对position和limit的范围进行字节对齐校准
            return direct.alignedSlice(alignment);
        }
        
        // 获取当前线程私有的Buffer缓冲池
        BufferCache cache = bufferCache.get();
        
        // 从Buffer缓冲池中取出一个容量至少为size的Direct Buffer
        ByteBuffer buf = cache.get(size);
        // 如果取到了合适的Buffer
        if(buf != null) {
            // 如果该Buffer已经对齐，则返回该Buffer
            if(buf.alignmentOffset(0, alignment) == 0) {
                return buf;
            }
        } else {
            // 没有取到合适Buffer
            if(!cache.isEmpty()) {
                // 获取Buffer缓冲池首位处的Direct Buffer，并释放其内存
                buf = cache.removeFirst();
                free(buf);
            }
        }
        
        /*
         * 创建直接内存缓冲区DirectByteBuffer
         * 多分配了alignment - 1字节的空间，已备后续字节对齐校准
         */
        ByteBuffer direct = ByteBuffer.allocateDirect(size + alignment - 1);
        
        // 切片，需要先以unitSize为基准，对position和limit的范围进行字节对齐校准
        return direct.alignedSlice(alignment);
    }
    
    /*▲ 获取直接缓冲区 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Buffer缓冲池 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Releases a temporary buffer by returning to the cache or freeing it.
     */
    // 采用FILO的形式(入栈模式)将buf放入Buffer缓存池以待复用
    public static void releaseTemporaryDirectBuffer(ByteBuffer buf) {
        offerFirstTemporaryDirectBuffer(buf);
    }
    
    /**
     * Releases a temporary buffer by returning to the cache or freeing it. If
     * returning to the cache then insert it at the start so that it is
     * likely to be returned by a subsequent call to getTemporaryDirectBuffer.
     */
    // 采用FILO的形式(入栈模式)将buf放入Buffer缓存池以待复用
    static void offerFirstTemporaryDirectBuffer(ByteBuffer buf) {
        // If the buffer is too large for the cache we don't have to check the cache. We'll just free it.
        if(isBufferTooLarge(buf)) {
            // 如果给定的buf尺寸超过了Buffer缓冲池对Buffer尺寸的约束，则不进入缓冲池，直接释放它
            free(buf);
            return;
        }
        
        assert buf != null;
        
        // 获取线程私有的Buffer缓冲池
        BufferCache cache = bufferCache.get();
        
        // 采用FILO的形式(入栈模式)将buf放入Buffer缓存池以待复用
        if(!cache.offerFirst(buf)) {
            // 如果缓冲池已经满了，则释放该Buffer
            free(buf);
        }
    }
    
    /**
     * Releases a temporary buffer by returning to the cache or freeing it. If
     * returning to the cache then insert it at the end. This makes it
     * suitable for scatter/gather operations where the buffers are returned to
     * cache in same order that they were obtained.
     */
    // 采用FIFO的形式(入队模式)将buf存入Buffer缓存池以待复用
    static void offerLastTemporaryDirectBuffer(ByteBuffer buf) {
        // If the buffer is too large for the cache we don't have to check the cache. We'll just free it.
        if(isBufferTooLarge(buf)) {
            // 如果给定的Buffer尺寸超过了Buffer缓冲池对Buffer尺寸的约束，则不进入缓冲池，直接释放它
            free(buf);
            return;
        }
        
        assert buf != null;
        
        // 获取线程私有的Buffer缓冲池
        BufferCache cache = bufferCache.get();
        
        // 采用FIFO的形式(入队模式)将buf存入Buffer缓存池以待复用
        if(!cache.offerLast(buf)) {
            // 如果缓冲池已经满了，则释放该Buffer
            free(buf);
        }
    }
    
    /**
     * Returns the max size allowed for a cached temp buffers, in
     * bytes. It defaults to Long.MAX_VALUE. It can be set with the
     * jdk.nio.maxCachedBufferSize property. Even though
     * ByteBuffer.capacity() returns an int, we're using a long here
     * for potential future-proofing.
     */
    // 返回Buffer缓冲池中缓存的Buffer被允许的最大尺寸(bytes)
    private static long getMaxCachedBufferSize() {
        String s = GetPropertyAction.privilegedGetProperty("jdk.nio.maxCachedBufferSize");
        
        if(s != null) {
            try {
                long m = Long.parseLong(s);
                if(m >= 0) {
                    return m;
                } else {
                    // if it's negative, ignore the system property
                }
            } catch(NumberFormatException e) {
                // if the string is not well formed, ignore the system property
            }
        }
        
        return Long.MAX_VALUE;
    }
    
    /**
     * Returns true if a buffer of this size is too large to be
     * added to the buffer cache, false otherwise.
     */
    // 判断给定的尺寸是否超过了Buffer缓冲池对Buffer尺寸的约束
    private static boolean isBufferTooLarge(int size) {
        return size>MAX_CACHED_BUFFER_SIZE;
    }
    
    /**
     * Returns true if the buffer is too large to be added to the
     * buffer cache, false otherwise.
     */
    // 判断给定的Buffer尺寸是否超过了Buffer缓冲池对Buffer尺寸的约束
    private static boolean isBufferTooLarge(ByteBuffer buf) {
        return isBufferTooLarge(buf.capacity());
    }
    
    /**
     * Frees the memory for the given direct buffer
     */
    // 清理直接缓冲区（释放其所占内存）
    private static void free(ByteBuffer buf) {
        // 获取该缓冲区的清理器
        Cleaner cleaner = ((DirectBuffer) buf).cleaner();
        // 对追踪对象进行清理，在Reference类中完成
        cleaner.clean();
    }
    
    /*▲ Buffer缓冲池 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Unsafe ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 该工具类缓存的Unsafe
    static Unsafe unsafe() {
        return unsafe;
    }
    
    // 返回内存分页大小
    static int pageSize() {
        if(pageSize == -1) {
            pageSize = unsafe().pageSize();
        }
        return pageSize;
    }
    
    // 擦除Buffer中的数据（全部填充为0）
    static void erase(ByteBuffer bb) {
        unsafe.setMemory(((DirectBuffer) bb).address(), bb.capacity(), (byte) 0);
    }
    
    // 获取本地内存中address地址处对应的byte类型字段的值
    private static byte _get(long address) {
        return unsafe.getByte(address);
    }
    
    // 设置本地内存中address地址处对应的byte型字段为新值x
    private static void _put(long address, byte x) {
        unsafe.putByte(address, x);
    }
    
    /*▲ Unsafe ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ MappedByteBuffer ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 创建一块可读写的基于内存的直接字节缓冲区
    static MappedByteBuffer newMappedByteBuffer(int size, long addr, FileDescriptor fd, Runnable unmapper) {
        MappedByteBuffer dbb;
        
        if(directByteBufferConstructor == null) {
            // 生成一个可读写、直接缓冲区的构造器
            initDBBConstructor();
        }
        
        try {
            // 创建基于内存的直接字节缓冲区
            dbb = (MappedByteBuffer) directByteBufferConstructor.newInstance(new Object[]{size, addr, fd, unmapper});
        } catch(InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new InternalError(e);
        }
        
        return dbb;
    }
    
    // 创建一块只读的基于内存的直接字节缓冲区
    static MappedByteBuffer newMappedByteBufferR(int size, long addr, FileDescriptor fd, Runnable unmapper) {
        MappedByteBuffer dbb;
        
        if(directByteBufferRConstructor == null) {
            // 生成一个只读、直接缓冲区的构造器
            initDBBRConstructor();
        }
        
        try {
            // 创建基于内存的直接字节缓冲区
            dbb = (MappedByteBuffer) directByteBufferRConstructor.newInstance(new Object[]{size, addr, fd, unmapper});
        } catch(InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new InternalError(e);
        }
        
        return dbb;
    }
    
    // 生成一个可读写、直接缓冲区的构造器
    private static void initDBBConstructor() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Class<?> cl = Class.forName("java.nio.DirectByteBuffer");
                    Constructor<?> ctor = cl.getDeclaredConstructor(int.class, long.class, FileDescriptor.class, Runnable.class);
                    ctor.setAccessible(true);
                    directByteBufferConstructor = ctor;
                } catch(ClassNotFoundException | NoSuchMethodException | IllegalArgumentException | ClassCastException x) {
                    throw new InternalError(x);
                }
                return null;
            }
        });
    }
    
    // 生成一个只读、直接缓冲区的构造器
    private static void initDBBRConstructor() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Class<?> cl = Class.forName("java.nio.DirectByteBufferR");
                    Constructor<?> ctor = cl.getDeclaredConstructor(int.class, long.class, FileDescriptor.class, Runnable.class);
                    ctor.setAccessible(true);
                    directByteBufferRConstructor = ctor;
                } catch(ClassNotFoundException | NoSuchMethodException | IllegalArgumentException | ClassCastException x) {
                    throw new InternalError(x);
                }
                return null;
            }
        });
    }
    
    /*▲ MappedByteBuffer ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 对齐检查 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 确保(address + pos)是alignment的整数倍，否则抛异常
    static void checkBufferPositionAligned(ByteBuffer bb, int pos, int alignment) throws IOException {
        if(bb.alignmentOffset(pos, alignment) != 0) {
            throw new IOException("Current location of the bytebuffer (" + pos + ") is not a multiple of the block size (" + alignment + ")");
        }
    }
    
    // 确保rem是alignment的整数倍，否则抛异常
    static void checkRemainingBufferSizeAligned(int rem, int alignment) throws IOException {
        if(rem % alignment != 0) {
            throw new IOException("Number of remaining bytes (" + rem + ") is not a multiple of the block size (" + alignment + ")");
        }
    }
    
    // 确保position是alignment的整数倍，否则抛异常
    static void checkChannelPositionAligned(long position, int alignment) throws IOException {
        if(position % alignment != 0) {
            throw new IOException("Channel position (" + position + ") is not a multiple of the block size (" + alignment + ")");
        }
    }
    
    /*▲ 对齐检查 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 获取bufs[offset, offset+length-1]范围内的子序列
    static ByteBuffer[] subsequence(ByteBuffer[] bufs, int offset, int length) {
        if((offset == 0) && (length == bufs.length)) {
            return bufs;
        }
        
        ByteBuffer[] subBufs = new ByteBuffer[length];
        for(int i = 0; i<length; i++) {
            subBufs[i] = bufs[offset + i];
        }
        
        return subBufs;
    }
    
    // 将指定的set包装为一个容量不可增长的集合（可以减少）
    static <E> Set<E> ungrowableSet(final Set<E> set) {
        return new Set<E>() {
            public int size() {
                return set.size();
            }
            
            public boolean isEmpty() {
                return set.isEmpty();
            }
            
            public boolean contains(Object o) {
                return set.contains(o);
            }
            
            public Object[] toArray() {
                return set.toArray();
            }
            
            public <T> T[] toArray(T[] a) {
                return set.toArray(a);
            }
            
            public String toString() {
                return set.toString();
            }
            
            public Iterator<E> iterator() {
                return set.iterator();
            }
            
            public boolean equals(Object o) {
                return set.equals(o);
            }
            
            public int hashCode() {
                return set.hashCode();
            }
            
            public void clear() {
                set.clear();
            }
            
            public boolean remove(Object o) {
                return set.remove(o);
            }
            
            public boolean containsAll(Collection<?> coll) {
                return set.containsAll(coll);
            }
            
            public boolean removeAll(Collection<?> coll) {
                return set.removeAll(coll);
            }
            
            public boolean retainAll(Collection<?> coll) {
                return set.retainAll(coll);
            }
            
            public boolean add(E o) {
                throw new UnsupportedOperationException();
            }
            
            public boolean addAll(Collection<? extends E> coll) {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    
    
    /**
     * A simple cache of direct buffers.
     */
    // 线程私有的Buffer缓冲池，用于缓存多个Direct Buffer
    private static class BufferCache {
        /* the array of buffers */
        // Buffer缓冲池的内部结构
        private ByteBuffer[] buffers;
        
        /* the number of buffers in the cache */
        // 当前缓存的Buffer的数量
        private int count;
        
        /* the index of the first valid buffer (undefined if count == 0) */
        // 游标（LIFO-栈顶，FIFO-队头）
        private int start;
        
        BufferCache() {
            buffers = new ByteBuffer[TEMP_BUF_POOL_SIZE];
        }
        
        /**
         * Removes and returns a buffer from the cache of at least the given
         * size (or null if no suitable buffer is found).
         */
        // 从Buffer缓冲池中取出一个容量至少为size的Direct Buffer
        ByteBuffer get(int size) {
            // Don't call this if the buffer would be too large.
            assert !isBufferTooLarge(size);
            
            if(count == 0) {
                return null;  // cache is empty
            }
            
            ByteBuffer[] buffers = this.buffers;
            
            // search for suitable buffer (often the first buffer will do)
            ByteBuffer buf = buffers[start];
            
            // 如果当前取到的Buffer容量不符要求
            if(buf.capacity()<size) {
                buf = null;
                int i = start;
                
                // 如果缓冲池还有遍历的余地
                while((i = next(i)) != start) {
                    ByteBuffer bb = buffers[i];
                    if(bb == null) {
                        // 已经没缓存了
                        break;
                    }
                    
                    // 找到了符合要求的缓存
                    if(bb.capacity() >= size) {
                        buf = bb;
                        break;
                    }
                }
                
                if(buf == null) {
                    return null;
                }
                
                /* move first element to here to avoid re-packing */
                // 将start处的元素移动到i处，可以避免移动大量元素
                buffers[i] = buffers[start];
            }
            
            // 按次序移除首个缓存的Buffer
            buffers[start] = null;
            
            // 更新start到下一个缓存的Buffer的下标
            start = next(start);
            
            // 计数减一
            count--;
            
            /* prepare the buffer and return it */
            
            // 丢弃备忘，游标归零
            buf.rewind();
            
            // 设置新的上界为size
            buf.limit(size);
            
            return buf;
        }
        
        // 采用FILO的形式(入栈模式)将buf放入Buffer缓存池以待复用
        boolean offerFirst(ByteBuffer buf) {
            // Don't call this if the buffer is too large.
            assert !isBufferTooLarge(buf);
            
            if(count >= TEMP_BUF_POOL_SIZE) {
                return false;
            }
            
            start = (start + TEMP_BUF_POOL_SIZE - 1) % TEMP_BUF_POOL_SIZE;
            buffers[start] = buf;
            count++;
            
            return true;
        }
        
        // 采用FIFO的形式(入队模式)将buf存入Buffer缓存池以待复用
        boolean offerLast(ByteBuffer buf) {
            // Don't call this if the buffer is too large.
            assert !isBufferTooLarge(buf);
            
            if(count >= TEMP_BUF_POOL_SIZE) {
                return false;
            }
            
            int next = (start + count) % TEMP_BUF_POOL_SIZE;
            buffers[next] = buf;
            count++;
            
            return true;
        }
        
        // 判断Buffer缓冲池是否为空
        boolean isEmpty() {
            return count == 0;
        }
        
        // 获取Buffer缓冲池首位处的Direct Buffer
        ByteBuffer removeFirst() {
            assert count>0;
            ByteBuffer buf = buffers[start];
            buffers[start] = null;
            start = next(start);
            count--;
            return buf;
        }
        
        // 计算下一个缓存的Buffer的下标
        private int next(int i) {
            return (i + 1) % TEMP_BUF_POOL_SIZE;
        }
    }
    
}
