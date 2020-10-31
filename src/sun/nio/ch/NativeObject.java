/*
 * Copyright (c) 2000, 2002, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;     // Formerly in sun.misc

import java.nio.ByteOrder;
import jdk.internal.misc.Unsafe;

/**
 * ## In the fullness of time, this class will be eliminated
 *
 * Proxies for objects that reside in native memory.
 */
// 代表一块底层内存块，包含了对内存块中值的操作方式
class NativeObject {
    
    protected static final Unsafe unsafe = Unsafe.getUnsafe();
    
    /** Cache for byte order */
    // 记录底层的字节存储顺序是大端还是小端
    private static ByteOrder byteOrder = null;
    
    /**
     * Cache for page size.
     * Lazily initialized via a data race; safe because ints are atomic.
     */
    // 内存分页大小（懒加载）
    private static int pageSize = -1;
    
    /**
     * Native allocation address;
     * may be smaller than the base address due to page-size rounding
     */
    // 记录新的本地内存块的真实起始地址，使用分页对齐时，allocationAddress<=address
    protected long allocationAddress;
    
    /** Native base address */
    // 记录新的本地内存块使用的起始地址
    private final long address;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Invoked only by AllocatedNativeObject */
    /*
     * size：待分配内存大小
     * pageAligned：是否进行分页对齐
     */
    protected NativeObject(int size, boolean pageAligned) {
        if(!pageAligned) {
            // 申请size字节的本地内存，并返回分配的真实内存地址
            this.allocationAddress = unsafe.allocateMemory(size);
            this.address = this.allocationAddress;
        } else {
            // 获取内存分页大小
            int ps = pageSize();
            // 分配内存，获取内存地址（这里需要按分页对齐，所以多分配一页，以待后续对齐操作）
            long addr = unsafe.allocateMemory(size + ps);
            // 记录新的本地内存块的真实起始地址
            this.allocationAddress = addr;
            // 记录新的本地内存块使用的起始地址（进行了按页对齐操作，会舍弃靠前的一部分内存）
            this.address = addr + ps - (addr & (ps - 1));
        }
    }
    
    /**
     * Creates a new native object that is based at the given native address.
     */
    NativeObject(long address) {
        this.allocationAddress = address;
        this.address = address;
    }
    
    /**
     * Creates a new native object allocated at the given native address but
     * whose base is at the additional offset.
     */
    NativeObject(long address, long offset) {
        this.allocationAddress = address;
        this.address = address + offset;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 值操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 注：以下方法中的offset参数均代表相对于内存块使用的起始地址address的偏移量
     */
    
    /**
     * Reads a byte starting at the given offset from base of this native object.
     *
     * @param offset The offset at which to read the byte
     *
     * @return The byte value read
     */
    // 获取指定地址处的byte值
    final byte getByte(int offset) {
        return unsafe.getByte(offset + address);
    }
    
    /**
     * Reads a short starting at the given offset from base of this native
     * object.
     *
     * @param offset The offset at which to read the short
     *
     * @return The short value read
     */
    // 获取指定地址处的short值
    final short getShort(int offset) {
        return unsafe.getShort(offset + address);
    }
    
    /**
     * Reads an int starting at the given offset from base of this native
     * object.
     *
     * @param offset The offset at which to read the int
     *
     * @return The int value read
     */
    // 获取指定地址处的int值
    final int getInt(int offset) {
        return unsafe.getInt(offset + address);
    }
    
    /**
     * Reads a long starting at the given offset from base of this native
     * object.
     *
     * @param offset The offset at which to read the long
     *
     * @return The long value read
     */
    // 获取指定地址处的long值
    final long getLong(int offset) {
        return unsafe.getLong(offset + address);
    }
    
    /**
     * Reads a char starting at the given offset from base of this native
     * object.
     *
     * @param offset The offset at which to read the char
     *
     * @return The char value read
     */
    // 获取指定地址处的char值
    final char getChar(int offset) {
        return unsafe.getChar(offset + address);
    }
    
    /**
     * Reads a float starting at the given offset from base of this native
     * object.
     *
     * @param offset The offset at which to read the float
     *
     * @return The float value read
     */
    // 获取指定地址处的float值
    final float getFloat(int offset) {
        return unsafe.getFloat(offset + address);
    }
    
    /**
     * Reads a double starting at the given offset from base of this native
     * object.
     *
     * @param offset The offset at which to read the double
     *
     * @return The double value read
     */
    // 获取指定地址处的double值
    final double getDouble(int offset) {
        return unsafe.getDouble(offset + address);
    }
    
    /**
     * Reads an address from this native object at the given offset and
     * constructs a native object using that address.
     *
     * @param offset The offset of the address to be read.  Note that the size of an
     *               address is implementation-dependent.
     *
     * @return The native object created using the address read from the
     * given offset
     */
    // 获取指定地址处的object值（通过object的地址[即指针]来获取值）
    NativeObject getObject(int offset) {
        long newAddress = 0L;
    
        switch(addressSize()) {
            case 8:
                newAddress = unsafe.getLong(offset + address);
                break;
            case 4:
                newAddress = unsafe.getInt(offset + address) & 0x00000000FFFFFFFF;
                break;
            default:
                throw new InternalError("Address size not supported");
        }
    
        return new NativeObject(newAddress);
    }
    
    
    /**
     * Writes a byte at the specified offset from this native object's
     * base address.
     *
     * @param offset The offset at which to write the byte
     * @param value  The byte value to be written
     */
    // 设置指定地址处的byte值为value
    final void putByte(int offset, byte value) {
        unsafe.putByte(offset + address, value);
    }
    
    /**
     * Writes a short at the specified offset from this native object's
     * base address.
     *
     * @param offset The offset at which to write the short
     * @param value  The short value to be written
     */
    // 设置指定地址处的short值为value
    final void putShort(int offset, short value) {
        unsafe.putShort(offset + address, value);
    }
    
    /**
     * Writes an int at the specified offset from this native object's
     * base address.
     *
     * @param offset The offset at which to write the int
     * @param value  The int value to be written
     */
    // 设置指定地址处的int值为value
    final void putInt(int offset, int value) {
        unsafe.putInt(offset + address, value);
    }
    
    /**
     * Writes a long at the specified offset from this native object's
     * base address.
     *
     * @param offset The offset at which to write the long
     * @param value  The long value to be written
     */
    // 设置指定地址处的long值为value
    final void putLong(int offset, long value) {
        unsafe.putLong(offset + address, value);
    }
    
    /**
     * Writes a char at the specified offset from this native object's
     * base address.
     *
     * @param offset The offset at which to write the char
     * @param value  The char value to be written
     */
    // 设置指定地址处的char值为value
    final void putChar(int offset, char value) {
        unsafe.putChar(offset + address, value);
    }
    
    /**
     * Writes a float at the specified offset from this native object's
     * base address.
     *
     * @param offset The offset at which to write the float
     * @param value  The float value to be written
     */
    // 设置指定地址处的float值为value
    final void putFloat(int offset, float value) {
        unsafe.putFloat(offset + address, value);
    }
    
    /**
     * Writes a double at the specified offset from this native object's
     * base address.
     *
     * @param offset The offset at which to write the double
     * @param value  The double value to be written
     */
    // 设置指定地址处的double值为value
    final void putDouble(int offset, double value) {
        unsafe.putDouble(offset + address, value);
    }
    
    /**
     * Writes the base address of the given native object at the given offset
     * of this native object.
     *
     * @param offset The offset at which the address is to be written.  Note that the
     *               size of an address is implementation-dependent.
     * @param ob     The native object whose address is to be written
     */
    // 设置指定地址处的object值为value（这里存储object的地址[即指针]信息）
    void putObject(int offset, NativeObject ob) {
        switch(addressSize()) {
            case 8:
                putLong(offset, ob.address);
                break;
            case 4:
                putInt(offset, (int) (ob.address & 0x00000000FFFFFFFF));
                break;
            default:
                throw new InternalError("Address size not supported");
        }
    }
    
    /*▲ 值操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the byte order of the underlying hardware.
     *
     * @return An instance of {@link java.nio.ByteOrder}
     */
    // 获取底层字节顺序
    static ByteOrder byteOrder() {
        if(byteOrder != null) {
            return byteOrder;
        }
        
        long a = unsafe.allocateMemory(8);
        try {
            unsafe.putLong(a, 0x0102030405060708L);
            byte b = unsafe.getByte(a);
            switch(b) {
                case 0x01:
                    byteOrder = ByteOrder.BIG_ENDIAN;
                    break;
                case 0x08:
                    byteOrder = ByteOrder.LITTLE_ENDIAN;
                    break;
                default:
                    assert false;
            }
        } finally {
            unsafe.freeMemory(a);
        }
        
        return byteOrder;
    }
    
    /**
     * Returns the page size of the underlying hardware.
     *
     * @return The page size, in bytes
     */
    // 获取内存分页大小
    static int pageSize() {
        int value = pageSize;
        
        if(value == -1) {
            pageSize = value = unsafe.pageSize();
        }
        
        return value;
    }
    
    // 获取新的本地内存块的真实起始地址
    long allocationAddress() {
        return allocationAddress;
    }
    
    /**
     * Returns the native base address of this native object.
     *
     * @return The native base address
     */
    // 获取新的本地内存块使用的起始地址
    long address() {
        return address;
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns the native architecture's address size in bytes.
     *
     * @return The address size of the native architecture
     */
    // 获取本机指针的大小（以字节为单位），此值为4或8
    static int addressSize() {
        return unsafe.addressSize();
    }
    
    /**
     * Creates a new native object starting at the given offset from the base
     * of this native object.
     *
     * @param offset The offset from the base of this native object that is to be
     *               the base of the new native object
     *
     * @return The newly created native object
     */
    // 在当前内存块上划分一块子内存
    NativeObject subObject(int offset) {
        return new NativeObject(offset + address);
    }
    
}
