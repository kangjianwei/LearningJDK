/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.misc;

import jdk.internal.misc.VM;
import jdk.internal.ref.Cleaner;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import jdk.internal.vm.annotation.ForceInline;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * A collection of methods for performing low-level, unsafe operations.
 * Although the class and all methods are public, use of this class is
 * limited because only trusted code can obtain instances of it.
 *
 * <em>Note:</em> It is the resposibility of the caller to make sure
 * arguments are checked before methods of this class are
 * called. While some rudimentary checks are performed on the input,
 * the checks are best effort and when performance is an overriding
 * priority, as when methods of this class are optimized by the
 * runtime compiler, some or all checks (if any) may be elided. Hence,
 * the caller must not rely on the checks and corresponding
 * exceptions!
 *
 * @author John R. Rose
 * @see #getUnsafe
 */
/*
 * 用于执行低级别，不安全操作的方法集合。
 *
 * 该类包装了jdk.internal.misc.Unsafe类中的部分操作
 * 虽然类和所有方法都是公共的，但是使用这个类是有限的，因为只有可信代码才能获得它的实例。
 *
 * 该类支持在任意内存地址位置处读写数据，对于普通用户来说，使用起来还是比较危险的。
 *
 * 常用的场景：
 * --> 创建某个类的对象（不经过构造方法）
 * --> 本地内存操作：分配/释放内存，向内存存值，从内存中取值
 * --> 获取对象中某字段的地址，获取该字段存储的值（通过地址），为某地址处的字段赋值（可支持Volatile语义）
 * --> 对JVM内存中某对象的数组字段/变量直接操作
 * --> 原子操作(CAS)，设置/更新/增减值
 * --> 线程操作
 * --> 内存屏障
 */
public final class Unsafe {
    
    // 可通过反射获得此静态域
    private static final Unsafe theUnsafe = new Unsafe();
    
    // 另一个内部同名Unsafe类。此类中的方法实际上是委托给内部的theInternalUnsafe实例去完成的
    private static final jdk.internal.misc.Unsafe theInternalUnsafe = jdk.internal.misc.Unsafe.getUnsafe();
    
    /**
     * This constant differs from all results that will ever be returned from {@link #staticFieldOffset}, {@link #objectFieldOffset}, or {@link #arrayBaseOffset}.
     */
    // 无效的JVM内存偏移量标记
    public static final int INVALID_FIELD_OFFSET = jdk.internal.misc.Unsafe.INVALID_FIELD_OFFSET;
    
    /** The value of {@code addressSize()} */
    // 本地指针尺寸，4字节或8字节
    public static final int ADDRESS_SIZE = theInternalUnsafe.addressSize();
    
    
    /* 用于 #arrayBaseOffset 的值*/
    
    /** The value of {@code arrayBaseOffset(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_BOOLEAN_BASE_OFFSET;
    /** The value of {@code arrayBaseOffset(byte[].class)} */
    public static final int ARRAY_BYTE_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;
    /** The value of {@code arrayBaseOffset(short[].class)} */
    public static final int ARRAY_SHORT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET;
    /** The value of {@code arrayBaseOffset(char[].class)} */
    public static final int ARRAY_CHAR_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_CHAR_BASE_OFFSET;
    /** The value of {@code arrayBaseOffset(int[].class)} */
    public static final int ARRAY_INT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_INT_BASE_OFFSET;
    /** The value of {@code arrayBaseOffset(long[].class)} */
    public static final int ARRAY_LONG_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_LONG_BASE_OFFSET;
    /** The value of {@code arrayBaseOffset(float[].class)} */
    public static final int ARRAY_FLOAT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_FLOAT_BASE_OFFSET;
    /** The value of {@code arrayBaseOffset(double[].class)} */
    public static final int ARRAY_DOUBLE_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_DOUBLE_BASE_OFFSET;
    /** The value of {@code arrayBaseOffset(Object[].class)} */
    public static final int ARRAY_OBJECT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_OBJECT_BASE_OFFSET;
    
    
    /* 用于 #arrayIndexScale 的值*/
    
    /** The value of {@code arrayIndexScale(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_BOOLEAN_INDEX_SCALE;
    /** The value of {@code arrayIndexScale(byte[].class)} */
    public static final int ARRAY_BYTE_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_BYTE_INDEX_SCALE;
    /** The value of {@code arrayIndexScale(short[].class)} */
    public static final int ARRAY_SHORT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_SHORT_INDEX_SCALE;
    /** The value of {@code arrayIndexScale(char[].class)} */
    public static final int ARRAY_CHAR_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_CHAR_INDEX_SCALE;
    /** The value of {@code arrayIndexScale(int[].class)} */
    public static final int ARRAY_INT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_INT_INDEX_SCALE;
    /** The value of {@code arrayIndexScale(long[].class)} */
    public static final int ARRAY_LONG_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_LONG_INDEX_SCALE;
    /** The value of {@code arrayIndexScale(float[].class)} */
    public static final int ARRAY_FLOAT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_FLOAT_INDEX_SCALE;
    /** The value of {@code arrayIndexScale(double[].class)} */
    public static final int ARRAY_DOUBLE_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_DOUBLE_INDEX_SCALE;
    /** The value of {@code arrayIndexScale(Object[].class)} */
    public static final int ARRAY_OBJECT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_OBJECT_INDEX_SCALE;
    
    
    static {
        Reflection.registerMethodsToFilter(Unsafe.class, "getUnsafe");
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private Unsafe() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Provides the caller with the capability of performing unsafe
     * operations.
     *
     * <p>The returned {@code Unsafe} object should be carefully guarded
     * by the caller, since it can be used to read and write data at arbitrary
     * memory addresses.  It must never be passed to untrusted code.
     *
     * <p>Most methods in this class are very low-level, and correspond to a
     * small number of hardware instructions (on typical machines).  Compilers
     * are encouraged to optimize these methods accordingly.
     *
     * <p>Here is a suggested idiom for using unsafe operations:
     *
     * <pre> {@code
     * class MyTrustedClass {
     *   private static final Unsafe unsafe = Unsafe.getUnsafe();
     *   ...
     *   private long myCountAddress = ...;
     *   public int getCount() { return unsafe.getByte(myCountAddress); }
     * }}</pre>
     *
     * (It may assist compilers to make the local variable {@code final}.)
     *
     * @throws  SecurityException if the class loader of the caller
     *          class is not in the system domain in which all permissions
     *          are granted.
     */
    // 返回单例对象，只能从引导类加载器（bootstrap class loader）加载，被自定义类直接调用会抛出异常
    @CallerSensitive
    public static Unsafe getUnsafe() {
        // 获取getUnsafe()调用者所处的类
        Class<?> caller = Reflection.getCallerClass();
    
        // 校验ClassLoader，从自己编写的类中调用此方法会抛出异常
        if (!VM.isSystemDomainLoader(caller.getClassLoader())) {
            throw new SecurityException("Unsafe");
        }
    
        return theUnsafe;
    }
    
    
    
    /*▼ 构造Unsafe对象 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Allocates an instance but does not run any constructor.
     * Initializes the class if it has not yet been.
     */
    // 不调用构造方法就生成对象，但是该对象的字段会被赋为对应类型的"零值"，为该对象赋过的默认值也无效
    @ForceInline
    public Object allocateInstance(Class<?> cls) throws InstantiationException {
        return theInternalUnsafe.allocateInstance(cls);
    }
    
    /**
     * Defines a class but does not make it known to the class loader or system dictionary.
     * <p>
     * For each CP entry, the corresponding CP patch must either be null or have
     * the a format that matches its tag:
     * <ul>
     * <li>Integer, Long, Float, Double: the corresponding wrapper object type from java.lang
     * <li>Utf8: a string (must have suitable syntax if used as signature or name)
     * <li>Class: any java.lang.Class object
     * <li>String: any object (not just a java.lang.String)
     * <li>InterfaceMethodRef: (NYI) a method handle to invoke on that call site's arguments
     * </ul>
     * @param hostClass context for linkage, access control, protection domain, and class loader
     * @param data      bytes of a class file
     * @param cpPatches where non-null entries exist, they replace corresponding CP entries in data
     */
    // 定义(创建)一个虚拟机匿名类，该类不会被类加载器或系统目录发现
    @ForceInline
    public Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches) {
        return theInternalUnsafe.defineAnonymousClass(hostClass, data, cpPatches);
    }
    
    /*▲ 构造Unsafe对象 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reports the location of a given field in the storage allocation of its
     * class.  Do not expect to perform any sort of arithmetic on this offset;
     * it is just a cookie which is passed to the unsafe heap memory accessors.
     *
     * <p>Any given field will always have the same offset and base, and no
     * two distinct fields of the same class will ever have the same offset
     * and base.
     *
     * <p>As of 1.4.1, offsets for fields are represented as long values,
     * although the Sun JVM does not use the most significant 32 bits.
     * However, JVM implementations which store static fields at absolute
     * addresses can use long offsets and null base pointers to express
     * the field locations in a form usable by {@link #getInt(Object, long)}.
     * Therefore, code which will be ported to such JVMs on 64-bit platforms
     * must preserve all bits of static field offsets.
     *
     * @see #getInt(Object, long)
     */
    /*
     * 获取非静态字段的JVM偏移地址
     *
     * 通过该方法可以计算一个对象在内存中的空间大小，方法是：
     * 通过反射得到它的所有Field(包括父类继承得到的)，找出Field中偏移量最大值，然后对该最大偏移值填充字节数即为对象大小。
     *
     * 关于该方法的使用例子可以看下面的修改内存数据的例子；
     * putLong，putInt，putDouble，putChar，putObject等方法，直接修改内存数据（可以越过访问权限）
     *
     * 这里，还有put对应的get方法，很简单就是直接读取内存地址处的数据。
     */
    @ForceInline
    public long objectFieldOffset(Field f) {
        return theInternalUnsafe.objectFieldOffset(f);
    }
    
    /**
     * Reports the location of a given static field, in conjunction with {@link
     * #staticFieldBase}.
     * <p>Do not expect to perform any sort of arithmetic on this offset;
     * it is just a cookie which is passed to the unsafe heap memory accessors.
     *
     * <p>Any given field will always have the same offset, and no two distinct
     * fields of the same class will ever have the same offset.
     *
     * <p>As of 1.4.1, offsets for fields are represented as long values,
     * although the Sun JVM does not use the most significant 32 bits.
     * It is hard to imagine a JVM technology which needs more than
     * a few bits to encode an offset within a non-array object,
     * However, for consistency with other methods in this class,
     * this method reports its result as a long value.
     *
     * @see #getInt(Object, long)
     */
    // 获取静态字段的JVM偏移地址
    @ForceInline
    public long staticFieldOffset(Field f) {
        return theInternalUnsafe.staticFieldOffset(f);
    }
    
    /**
     * Reports the location of a given static field, in conjunction with {@link
     * #staticFieldOffset}.
     * <p>Fetch the base "Object", if any, with which static fields of the
     * given class can be accessed via methods like {@link #getInt(Object,
     * long)}.  This value may be null.  This value may refer to an object
     * which is a "cookie", not guaranteed to be a real Object, and it should
     * not be used in any way except as argument to the get and put routines in
     * this class.
     */
    // 获取静态字段所属的类对象
    @ForceInline
    public Object staticFieldBase(Field f) {
        return theInternalUnsafe.staticFieldBase(f);
    }
    
    /**
     * Detects if the given class may need to be initialized.
     * This is often needed in conjunction with obtaining the static field base of a class.
     *
     * @return false only if a call to {@code ensureClassInitialized} would have no effect
     */
    @ForceInline
    public boolean shouldBeInitialized(Class<?> c) {
        return theInternalUnsafe.shouldBeInitialized(c);
    }
    
    /**
     * Ensures the given class has been initialized.
     * This is often needed in conjunction with obtaining the static field base of a class.
     */
    @ForceInline
    public void ensureClassInitialized(Class<?> c) {
        theInternalUnsafe.ensureClassInitialized(c);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 本地内存操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Allocates a new block of native memory, of the given size in bytes.  The
     * contents of the memory are uninitialized; they will generally be
     * garbage.  The resulting native pointer will never be zero, and will be
     * aligned for all value types.  Dispose of this memory by calling {@link
     * #freeMemory}, or resize it with {@link #reallocateMemory}.
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if the size is negative or too large
     *         for the native size_t type
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *
     * @see #getByte(long)
     * @see #putByte(long, byte)
     */
    // 申请bytes字节的本地内存，并返回分配的内存地址
    @ForceInline
    public long allocateMemory(long bytes) {
        return theInternalUnsafe.allocateMemory(bytes);
    }
    
    /**
     * Resizes a new block of native memory, to the given size in bytes.  The
     * contents of the new block past the size of the old block are
     * uninitialized; they will generally be garbage.  The resulting native
     * pointer will be zero if and only if the requested size is zero.  The
     * resulting native pointer will be aligned for all value types.  Dispose
     * of this memory by calling {@link #freeMemory}, or resize it with {@link
     * #reallocateMemory}.  The address passed to this method may be null, in
     * which case an allocation will be performed.
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if the size is negative or too large
     *         for the native size_t type
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *
     * @see #allocateMemory
     */
    // 在地址address的基础上扩容，如果address为0，则效果与#allocateMemory一致
    @ForceInline
    public long reallocateMemory(long address, long bytes) {
        return theInternalUnsafe.reallocateMemory(address, bytes);
    }
    
    /**
     * Sets all bytes in a given block of memory to a fixed value (usually zero).
     * This provides a <em>single-register</em> addressing mode, as discussed in {@link #getInt(Object,long)}.
     *
     * <p>Equivalent to {@code setMemory(null, address, bytes, value)}.
     */
    // 为申请的内存批量填充初值，通常用0填充
    @ForceInline
    public void setMemory(long address, long bytes, byte value) {
        theInternalUnsafe.setMemory(address, bytes, value);
    }
    
    /**
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).
     *
     * <p>This method determines a block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * <p>The stores are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective address and
     * length are all even modulo 8, the stores take place in 'long' units.
     * If the effective address and length are (resp.) even modulo 4 or 2,
     * the stores take place in units of 'int' or 'short'.
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if any of the arguments is invalid
     *
     * @since 1.7
     */
    // 为对象o处的内存批量填充初值，通常用0填充
    @ForceInline
    public void setMemory(Object o, long offset, long bytes, byte value) {
        theInternalUnsafe.setMemory(o, offset, bytes, value);
    }
    
    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.  This provides a <em>single-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.
     *
     * Equivalent to {@code copyMemory(null, srcAddress, null, destAddress, bytes)}.
     */
    // 内存数据拷贝
    @ForceInline
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        theInternalUnsafe.copyMemory(srcAddress, destAddress, bytes);
    }
    
    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.
     *
     * <p>This method determines each block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object, long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * <p>The transfers are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective addresses and
     * length are all even modulo 8, the transfer takes place in 'long' units.
     * If the effective addresses and length are (resp.) even modulo 4 or 2,
     * the transfer takes place in units of 'int' or 'short'.
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if any of the arguments is invalid
     * @since 1.7
     */
    // 内存数据拷贝
    @ForceInline
    public void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        theInternalUnsafe.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
    }
    
    /**
     * Disposes of a block of native memory, as obtained from {@link
     * #allocateMemory} or {@link #reallocateMemory}.  The address passed to
     * this method may be null, in which case no action is taken.
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if any of the arguments is invalid
     *
     * @see #allocateMemory
     */
    // 用于释放allocateMemory和reallocateMemory申请的内存
    @ForceInline
    public void freeMemory(long address) {
        theInternalUnsafe.freeMemory(address);
    }
    
    
    
    /**
     * Fetches a native pointer from a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     *
     * <p>If the native pointer is less than 64 bits wide, it is extended as
     * an unsigned number to a Java long.  The pointer may be indexed by any
     * given byte offset, simply by adding that offset (as a simple integer) to
     * the long representing the pointer.  The number of bytes actually read
     * from the target address may be determined by consulting {@link
     * #addressSize}.
     *
     * @see #allocateMemory
     */
    // 从本地内存地址address处获取一个本地指针值
    @ForceInline
    public long getAddress(long address) {
        return theInternalUnsafe.getAddress(address);
    }
    
    /**
     * Stores a native pointer into a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     *
     * <p>The number of bytes actually written at the target address may be
     * determined by consulting {@link #addressSize}.
     *
     * @see #getAddress(long)
     */
    // 向本地内存地址address处存入一个本地指针值x
    @ForceInline
    public void putAddress(long address, long x) {
        theInternalUnsafe.putAddress(address, x);
    }
    
    /**
     * Reports the size in bytes of a native pointer, as stored via {@link
     * #putAddress}.  This value will be either 4 or 8.  Note that the sizes of
     * other primitive types (as stored in native memory blocks) is determined
     * fully by their information content.
     */
    // 检查通过{@link #putAddress}存储的本机指针的大小（以字节为单位）。此值为4或8。请注意，其他基本类型的大小（存储在本机内存块中）完全由其信息内容决定。
    @ForceInline
    public int addressSize() {
        return theInternalUnsafe.addressSize();
    }
    
    /*▲ 本地内存操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    
    
    
    /* 获取/设置字段值 ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ */
    
    /*
     * (1.1) 基于JVM内存地址，获取/设置字段值
     * - getXXX(o, offset)
     *    获取对象o中offset地址处对应的字段值
     *    对象o可以是数组
     *    offset的值由#objectFieldOffset或#staticFieldOffset获取
     *    也可以由#arrayBaseOffset[B]和#arrayIndexScale[S]共同构成：B + N * S
     *
     * - putXXX(o, offset, x)
     *     设置对象o中offset地址处对应的字段为新值x
     *
     * (1.2) 基于本地内存地址，获取/设置字段值
     * - getXXX(address)
     * - putXXX(address, x)
     *
     * (3) 基于JVM内存地址，获取/设置字段值，Ordered/Lazy版本
     *     不保证值的改变被其他线程立即看到。
     * - putOrderedXXX(o, offset, x)
     *
     * (4) 基于JVM内存地址，获取/设置字段值，Volatile版本
     * - getXXXVolatile(o, offset)
     * - putXXXVolatile(o, offset, x)
     */
    
    /*▼ (1.1) getXXX/putXXX 获取/设置字段值（基于JVM内存地址） ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @see #getInt(Object, long)
     */
    // 获取对象o中offset地址处对应的byte型字段的值
    @ForceInline
    public byte getByte(Object o, long offset) {
        return theInternalUnsafe.getByte(o, offset);
    }
    
    /**
     * @see #getInt(Object, long)
     */
    // 获取对象o中offset地址处对应的short型字段的值
    @ForceInline
    public short getShort(Object o, long offset) {
        return theInternalUnsafe.getShort(o, offset);
    }
    
    /**
     * Fetches a value from a given Java variable.
     * More specifically, fetches a field or array element within the given
     * object {@code o} at the given offset, or (if {@code o} is null)
     * from the memory address whose numerical value is the given offset.
     * <p>
     * The results are undefined unless one of the following cases is true:
     * <ul>
     * <li>The offset was obtained from {@link #objectFieldOffset} on
     * the {@link java.lang.reflect.Field} of some Java field and the object
     * referred to by {@code o} is of a class compatible with that
     * field's class.
     *
     * <li>The offset and object reference {@code o} (either null or
     * non-null) were both obtained via {@link #staticFieldOffset}
     * and {@link #staticFieldBase} (respectively) from the
     * reflective {@link Field} representation of some Java field.
     *
     * <li>The object referred to by {@code o} is an array, and the offset
     * is an integer of the form {@code B+N*S}, where {@code N} is
     * a valid index into the array, and {@code B} and {@code S} are
     * the values obtained by {@link #arrayBaseOffset} and {@link
     * #arrayIndexScale} (respectively) from the array's class.  The value
     * referred to is the {@code N}<em>th</em> element of the array.
     *
     * </ul>
     * <p>
     * If one of the above cases is true, the call references a specific Java
     * variable (field or array element).  However, the results are undefined
     * if that variable is not in fact of the type returned by this method.
     * <p>
     * This method refers to a variable by means of two parameters, and so
     * it provides (in effect) a <em>double-register</em> addressing mode
     * for Java variables.  When the object reference is null, this method
     * uses its offset as an absolute address.  This is similar in operation
     * to methods such as {@link #getInt(long)}, which provide (in effect) a
     * <em>single-register</em> addressing mode for non-Java variables.
     * However, because Java variables may have a different layout in memory
     * from non-Java variables, programmers should not assume that these
     * two addressing modes are ever equivalent.  Also, programmers should
     * remember that offsets from the double-register addressing mode cannot
     * be portably confused with longs used in the single-register addressing
     * mode.
     *
     * @param o      Java heap object in which the variable resides, if any, else
     *               null
     * @param offset indication of where the variable resides in a Java heap
     *               object, if any, else a memory address locating the variable
     *               statically
     *
     * @return the value fetched from the indicated Java variable
     *
     * @throws RuntimeException No defined exceptions are thrown, not even
     *                          {@link NullPointerException}
     */
    // 获取对象o中offset地址处对应的int型字段的值
    @ForceInline
    public int getInt(Object o, long offset) {
        return theInternalUnsafe.getInt(o, offset);
    }
    
    /**
     * @see #getInt(Object, long)
     */
    // 获取对象o中offset地址处对应的long型字段的值
    @ForceInline
    public long getLong(Object o, long offset) {
        return theInternalUnsafe.getLong(o, offset);
    }
    
    /**
     * @see #getInt(Object, long)
     */
    // 获取对象o中offset地址处对应的float型字段的值
    @ForceInline
    public float getFloat(Object o, long offset) {
        return theInternalUnsafe.getFloat(o, offset);
    }
    
    /**
     * @see #getInt(Object, long)
     */
    // 获取对象o中offset地址处对应的double型字段的值
    @ForceInline
    public double getDouble(Object o, long offset) {
        return theInternalUnsafe.getDouble(o, offset);
    }
    
    /**
     * @see #getInt(Object, long)
     */
    // 获取对象o中offset地址处对应的char型字段的值
    @ForceInline
    public char getChar(Object o, long offset) {
        return theInternalUnsafe.getChar(o, offset);
    }
    
    /**
     * @see #getInt(Object, long)
     */
    // 获取对象o中offset地址处对应的boolean型字段的值
    @ForceInline
    public boolean getBoolean(Object o, long offset) {
        return theInternalUnsafe.getBoolean(o, offset);
    }
    
    /**
     * Fetches a reference value from a given Java variable.
     *
     * @see #getInt(Object, long)
     */
    // 获取对象o中offset地址处对应的引用类型字段的值
    @ForceInline
    public Object getObject(Object o, long offset) {
        return theInternalUnsafe.getObject(o, offset);
    }
    
    
    
    /**
     * @see #putInt(Object, long, int)
     */
    // 设置对象o中offset地址处对应的byte型字段为新值x
    @ForceInline
    public void putByte(Object o, long offset, byte x) {
        theInternalUnsafe.putByte(o, offset, x);
    }
    
    /**
     * @see #putInt(Object, long, int)
     */
    // 设置对象o中offset地址处对应的short型字段为新值x
    @ForceInline
    public void putShort(Object o, long offset, short x) {
        theInternalUnsafe.putShort(o, offset, x);
    }
    
    /**
     * Stores a value into a given Java variable.
     * <p>
     * The first two parameters are interpreted exactly as with
     * {@link #getInt(Object, long)} to refer to a specific
     * Java variable (field or array element).  The given value
     * is stored into that variable.
     * <p>
     * The variable must be of the same type as the method
     * parameter {@code x}.
     *
     * @param o      Java heap object in which the variable resides, if any, else
     *               null
     * @param offset indication of where the variable resides in a Java heap
     *               object, if any, else a memory address locating the variable
     *               statically
     * @param x      the value to store into the indicated Java variable
     *
     * @throws RuntimeException No defined exceptions are thrown, not even
     *                          {@link NullPointerException}
     */
    // 设置对象o中offset地址处对应的int型字段为新值x
    @ForceInline
    public void putInt(Object o, long offset, int x) {
        theInternalUnsafe.putInt(o, offset, x);
    }
    
    /**
     * @see #putInt(Object, long, int)
     */
    // 设置对象o中offset地址处对应的long型字段为新值x
    @ForceInline
    public void putLong(Object o, long offset, long x) {
        theInternalUnsafe.putLong(o, offset, x);
    }
    
    /**
     * @see #putInt(Object, long, int)
     */
    // 设置对象o中offset地址处对应的float型字段为新值x
    @ForceInline
    public void putFloat(Object o, long offset, float x) {
        theInternalUnsafe.putFloat(o, offset, x);
    }
    
    /**
     * @see #putInt(Object, long, int)
     */
    // 设置对象o中offset地址处对应的double型字段为新值x
    @ForceInline
    public void putDouble(Object o, long offset, double x) {
        theInternalUnsafe.putDouble(o, offset, x);
    }
    
    /**
     * @see #putInt(Object, long, int)
     */
    // 设置对象o中offset地址处对应的char型字段为新值x
    @ForceInline
    public void putChar(Object o, long offset, char x) {
        theInternalUnsafe.putChar(o, offset, x);
    }
    
    /**
     * @see #putInt(Object, long, int)
     */
    // 设置对象o中offset地址处对应的boolean型字段为新值x
    @ForceInline
    public void putBoolean(Object o, long offset, boolean x) {
        theInternalUnsafe.putBoolean(o, offset, x);
    }
    
    /**
     * Stores a reference value into a given Java variable.
     * <p>
     * Unless the reference {@code x} being stored is either null
     * or matches the field type, the results are undefined.
     * If the reference {@code o} is non-null, card marks or
     * other store barriers for that object (if the VM requires them)
     * are updated.
     *
     * @see #putInt(Object, long, int)
     */
    // 设置对象o中offset地址处对应的引用类型字段为新值x
    @ForceInline
    public void putObject(Object o, long offset, Object x) {
        theInternalUnsafe.putObject(o, offset, x);
    }
    
    /*▲ (1.1) getXXX/putXXX 获取/设置字段值（基于JVM内存地址） ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (1.2) getXXX/putXXX 获取/设置字段值（基于本地内存地址） ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Fetches a value from a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     *
     * @see #allocateMemory
     */
    // 获取本地内存中address地址处对应的byte类型字段的值
    @ForceInline
    public byte getByte(long address) {
        return theInternalUnsafe.getByte(address);
    }
    
    /**
     * @see #getByte(long)
     */
    // 获取本地内存中address地址处对应的short类型字段的值
    @ForceInline
    public short getShort(long address) {
        return theInternalUnsafe.getShort(address);
    }
    
    /**
     * @see #getByte(long)
     */
    // 获取本地内存中address地址处对应的int类型字段的值
    @ForceInline
    public int getInt(long address) {
        return theInternalUnsafe.getInt(address);
    }
    
    /**
     * @see #getByte(long)
     */
    // 获取本地内存中address地址处对应的long类型字段的值
    @ForceInline
    public long getLong(long address) {
        return theInternalUnsafe.getLong(address);
    }
    
    /**
     * @see #getByte(long)
     */
    // 获取本地内存中address地址处对应的float类型字段的值
    @ForceInline
    public float getFloat(long address) {
        return theInternalUnsafe.getFloat(address);
    }
    
    /**
     * @see #getByte(long)
     */
    // 获取本地内存中address地址处对应的double类型字段的值
    @ForceInline
    public double getDouble(long address) {
        return theInternalUnsafe.getDouble(address);
    }
    
    /**
     * @see #getByte(long)
     */
    // 获取本地内存中address地址处对应的char类型字段的值
    @ForceInline
    public char getChar(long address) {
        return theInternalUnsafe.getChar(address);
    }
    
    
    
    /**
     * Stores a value into a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     *
     * @see #getByte(long)
     */
    // 设置本地内存中address地址处对应的byte型字段为新值x
    @ForceInline
    public void putByte(long address, byte x) {
        theInternalUnsafe.putByte(address, x);
    }
    
    /**
     * @see #putByte(long, byte)
     */
    // 设置本地内存中address地址处对应的short型字段为新值x
    @ForceInline
    public void putShort(long address, short x) {
        theInternalUnsafe.putShort(address, x);
    }
    
    /**
     * @see #putByte(long, byte)
     */
    // 设置本地内存中address地址处对应的int型字段为新值x
    @ForceInline
    public void putInt(long address, int x) {
        theInternalUnsafe.putInt(address, x);
    }
    
    /**
     * @see #putByte(long, byte)
     */
    // 设置本地内存中address地址处对应的long型字段为新值x
    @ForceInline
    public void putLong(long address, long x) {
        theInternalUnsafe.putLong(address, x);
    }
    
    /**
     * @see #putByte(long, byte)
     */
    // 设置本地内存中address地址处对应的float型字段为新值x
    @ForceInline
    public void putFloat(long address, float x) {
        theInternalUnsafe.putFloat(address, x);
    }
    
    /**
     * @see #putByte(long, byte)
     */
    // 设置本地内存中address地址处对应的double型字段为新值x
    @ForceInline
    public void putDouble(long address, double x) {
        theInternalUnsafe.putDouble(address, x);
    }
    
    /**
     * @see #putByte(long, byte)
     */
    // 设置本地内存中address地址处对应的char型字段为新值x
    @ForceInline
    public void putChar(long address, char x) {
        theInternalUnsafe.putChar(address, x);
    }
    
    /*▲ (1.2) getXXX/putXXX 获取/设置字段值（基于本地内存地址） ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (3) putOrderedXXX 获取/设置字段值（基于JVM内存地址），Ordered/Lazy版本 █████████████████████████████████████████████████████████████┓ */
    
    /** Ordered/Lazy version of {@link #putIntVolatile(Object, long, int)}  */
    // 设置对象o中offset地址处对应的int型字段为新值x
    @ForceInline
    public void putOrderedInt(Object o, long offset, int x) {
        theInternalUnsafe.putIntRelease(o, offset, x);
    }
    
    /**
     * Ordered/Lazy version of {@link #putLongVolatile(Object, long, long)}
     */
    // 设置对象o中offset地址处对应的long型字段为新值x
    @ForceInline
    public void putOrderedLong(Object o, long offset, long x) {
        theInternalUnsafe.putLongRelease(o, offset, x);
    }
    
    /**
     * Version of {@link #putObjectVolatile(Object, long, Object)}
     * that does not guarantee immediate visibility of the store to
     * other threads. This method is generally only useful if the
     * underlying field is a Java volatile (or if an array cell, one
     * that is otherwise only accessed using volatile accesses).
     *
     * Corresponds to C11 atomic_store_explicit(..., memory_order_release).
     */
    // 设置对象o中offset地址处对应的引用类型字段为新值x
    @ForceInline
    public void putOrderedObject(Object o, long offset, Object x) {
        theInternalUnsafe.putObjectRelease(o, offset, x);
    }
    
    /*▲ (3) putOrderedXXX 获取/设置字段值（基于JVM内存地址），Ordered/Lazy版本 █████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (4) getXXXVolatile/putXXXVolatile 获取/设置字段值（基于JVM内存地址），Volatile版本 ███████████████████████████████████████████┓ */
    
    /**
     * Volatile version of {@link #getByte(Object, long)}
     */
    // 获取对象o中offset地址处对应的byte型字段的值，支持volatile语义
    @ForceInline
    public byte getByteVolatile(Object o, long offset) {
        return theInternalUnsafe.getByteVolatile(o, offset);
    }
    
    /**
     * Volatile version of {@link #getShort(Object, long)}
     */
    // 获取对象o中offset地址处对应的short型字段的值，支持volatile语义
    @ForceInline
    public short getShortVolatile(Object o, long offset) {
        return theInternalUnsafe.getShortVolatile(o, offset);
    }
    
    /** Volatile version of {@link #getInt(Object, long)}  */
    // 获取对象o中offset地址处对应的int型字段的值，支持volatile语义
    @ForceInline
    public int getIntVolatile(Object o, long offset) {
        return theInternalUnsafe.getIntVolatile(o, offset);
    }
    
    /**
     * Volatile version of {@link #getLong(Object, long)}
     */
    // 获取对象o中offset地址处对应的long型字段的值，支持volatile语义
    @ForceInline
    public long getLongVolatile(Object o, long offset) {
        return theInternalUnsafe.getLongVolatile(o, offset);
    }
    
    /**
     * Volatile version of {@link #getFloat(Object, long)}
     */
    // 获取对象o中offset地址处对应的float型字段的值，支持volatile语义
    @ForceInline
    public float getFloatVolatile(Object o, long offset) {
        return theInternalUnsafe.getFloatVolatile(o, offset);
    }
    
    /**
     * Volatile version of {@link #getDouble(Object, long)}
     */
    // 获取对象o中offset地址处对应的double型字段的值，支持volatile语义
    @ForceInline
    public double getDoubleVolatile(Object o, long offset) {
        return theInternalUnsafe.getDoubleVolatile(o, offset);
    }
    
    /**
     * Volatile version of {@link #getChar(Object, long)}
     */
    // 获取对象o中offset地址处对应的char型字段的值，支持volatile语义
    @ForceInline
    public char getCharVolatile(Object o, long offset) {
        return theInternalUnsafe.getCharVolatile(o, offset);
    }
    
    /**
     * Volatile version of {@link #getBoolean(Object, long)}
     */
    // 获取对象o中offset地址处对应的boolean型字段的值，支持volatile语义
    @ForceInline
    public boolean getBooleanVolatile(Object o, long offset) {
        return theInternalUnsafe.getBooleanVolatile(o, offset);
    }
    
    /**
     * Fetches a reference value from a given Java variable, with volatile
     * load semantics. Otherwise identical to {@link #getObject(Object, long)}
     */
    // 获取对象o中offset地址处对应的引用类型字段的值，支持volatile语义
    @ForceInline
    public Object getObjectVolatile(Object o, long offset) {
        return theInternalUnsafe.getObjectVolatile(o, offset);
    }
    
    
    
    /**
     * Volatile version of {@link #putByte(Object, long, byte)}
     */
    // 设置对象o中offset地址处对应的byte型字段为新值x，支持volatile语义
    @ForceInline
    public void putByteVolatile(Object o, long offset, byte x) {
        theInternalUnsafe.putByteVolatile(o, offset, x);
    }
    
    /**
     * Volatile version of {@link #putShort(Object, long, short)}
     */
    // 设置对象o中offset地址处对应的short型字段为新值x，支持volatile语义
    @ForceInline
    public void putShortVolatile(Object o, long offset, short x) {
        theInternalUnsafe.putShortVolatile(o, offset, x);
    }
    
    /** Volatile version of {@link #putInt(Object, long, int)}  */
    // 设置对象o中offset地址处对应的int型字段为新值x，支持volatile语义
    @ForceInline
    public void putIntVolatile(Object o, long offset, int x) {
        theInternalUnsafe.putIntVolatile(o, offset, x);
    }
    
    /**
     * Volatile version of {@link #putLong(Object, long, long)}
     */
    // 设置对象o中offset地址处对应的long型字段为新值x，支持volatile语义
    @ForceInline
    public void putLongVolatile(Object o, long offset, long x) {
        theInternalUnsafe.putLongVolatile(o, offset, x);
    }
    
    /**
     * Volatile version of {@link #putFloat(Object, long, float)}
     */
    // 设置对象o中offset地址处对应的float型字段为新值x，支持volatile语义
    @ForceInline
    public void putFloatVolatile(Object o, long offset, float x) {
        theInternalUnsafe.putFloatVolatile(o, offset, x);
    }
    
    /**
     * Volatile version of {@link #putDouble(Object, long, double)}
     */
    // 设置对象o中offset地址处对应的double型字段为新值x，支持volatile语义
    @ForceInline
    public void putDoubleVolatile(Object o, long offset, double x) {
        theInternalUnsafe.putDoubleVolatile(o, offset, x);
    }
    
    /**
     * Volatile version of {@link #putChar(Object, long, char)}
     */
    // 设置对象o中offset地址处对应的char型字段为新值x，支持volatile语义
    @ForceInline
    public void putCharVolatile(Object o, long offset, char x) {
        theInternalUnsafe.putCharVolatile(o, offset, x);
    }
    
    /**
     * Volatile version of {@link #putBoolean(Object, long, boolean)}
     */
    // 设置对象o中offset地址处对应的boolean型字段为新值x，支持volatile语义
    @ForceInline
    public void putBooleanVolatile(Object o, long offset, boolean x) {
        theInternalUnsafe.putBooleanVolatile(o, offset, x);
    }
    
    /**
     * Stores a reference value into a given Java variable, with
     * volatile store semantics. Otherwise identical to {@link #putObject(Object, long, Object)}
     */
    // 设置对象o中offset地址处对应的引用类型字段为新值x，支持volatile语义
    @ForceInline
    public void putObjectVolatile(Object o, long offset, Object x) {
        theInternalUnsafe.putObjectVolatile(o, offset, x);
    }
    
    /*▲ (4) getXXXVolatile/putXXXVolatile 获取/设置字段值（基于JVM内存地址），Volatile版本 ███████████████████████████████████████████┛ */
    
    
    
    /*▼ 对数组字段/变量直接操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reports the offset of the first element in the storage allocation of a
     * given array class.  If {@link #arrayIndexScale} returns a non-zero value
     * for the same class, you may use that scale factor, together with this
     * base offset, to form new offsets to access elements of arrays of the
     * given class.
     *
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    // 寻找某类型数组中的元素时约定的起始偏移地址（更像是一个标记），与#arrayIndexScale配合使用
    @ForceInline
    public int arrayBaseOffset(Class<?> arrayClass) {
        return theInternalUnsafe.arrayBaseOffset(arrayClass);
    }
    
    /**
     * Reports the scale factor for addressing elements in the storage
     * allocation of a given array class.  However, arrays of "narrow" types
     * will generally not work properly with accessors like {@link
     * #getByte(Object, long)}, so the scale factor for such classes is reported
     * as zero.
     *
     * @see #arrayBaseOffset
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    // 某类型数组每个元素所占字节数，与#arrayBaseOffset配合使用
    @ForceInline
    public int arrayIndexScale(Class<?> arrayClass) {
        return theInternalUnsafe.arrayIndexScale(arrayClass);
    }
    
    /*▲ 对数组字段/变量直接操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    /* 获取/设置字段值 ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ */
    
    
    
    
    
    
    /* 原子操作，基于JVM内存操作 ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ */
    
    /*
     * (1) 设置值，失败后会重试，属于自旋锁&乐观锁
     * - getAndSetXXX(o, offset, newValue)
     *   返回对象o的offset地址处的值，并将该值原子性地设置为新值newValue
     *   设置新值newValue的时候，要保证该字段修改过程中没有被其他线程修改，否则不断自旋，直到成功修改
     *
     * (2) 更新值，如果待更新字段与期望值expected相等，则原子地更新目标字段。返回值代表更新成功或失败。
     * - compareAndSwapXXX(o, offset, expected, x)
     *   拿对象o中offset地址的field值与预期值expected作比较（内存值可能被其他线程修改掉，所以需要比较）。
     *   如果发现该值被修改，则返回false，否则，原子地更新该值为x，且返回true。
     *   @param offset   JVM内存偏移地址，对象o中某字段field的地址
     *   @param o        包含field值的对象
     *   @param expected field当前的期望值
     *   @param x        如果field的当前值与期望值expected相同，那么更新filed的值为这个新值x
     *   @return         更新成功返回true，更新失败返回false
     *
     * (3) 增减值，如果更新失败就不断尝试，属于乐观锁&自旋锁
     * - getAndAddXXX(o, offset, delta)
     *   返回对象o的offset地址处的值，并将该值原子性地增加delta（delta可以为负数）
     *
     */
    
    /*▼ (1) 设置值，失败后会重试，属于自旋锁&乐观锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o        object/array to update the field/element in
     * @param offset   field/element offset
     * @param newValue new value
     *
     * @return the previous value
     *
     * @since 1.8
     */
    // 返回对象o的offset地址处的值，并将该值原子性地设置为新值newValue
    @ForceInline
    public final int getAndSetInt(Object o, long offset, int newValue) {
        return theInternalUnsafe.getAndSetInt(o, offset, newValue);
    }
    
    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o        object/array to update the field/element in
     * @param offset   field/element offset
     * @param newValue new value
     *
     * @return the previous value
     *
     * @since 1.8
     */
    // 返回对象o的offset地址处的值，并将该值原子性地设置为新值newValue
    @ForceInline
    public final long getAndSetLong(Object o, long offset, long newValue) {
        return theInternalUnsafe.getAndSetLong(o, offset, newValue);
    }
    
    /**
     * Atomically exchanges the given reference value with the current
     * reference value of a field or array element within the given
     * object {@code o} at the given {@code offset}.
     *
     * @param o        object/array to update the field/element in
     * @param offset   field/element offset
     * @param newValue new value
     *
     * @return the previous value
     *
     * @since 1.8
     */
    // 返回对象o的offset地址处的值，并将该值原子性地设置为新值newValue
    @ForceInline
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        return theInternalUnsafe.getAndSetObject(o, offset, newValue);
    }
    
    /*▲ (1) 设置值，失败后会重试，属于自旋锁&乐观锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (2) 更新值，基于JVM内存操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Atomically updates Java variable to {@code x} if it is currently holding {@code expected}.
     *
     * This operation has memory semantics of a {@code volatile} read and write.
     * Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    // 拿期望值expected与对象o的offset地址处的当前值比较，如果两个值相等，将当前值更新为x
    @ForceInline
    public final boolean compareAndSwapInt(Object o, long offset, int expected, int x) {
        return theInternalUnsafe.compareAndSetInt(o, offset, expected, x);
    }
    
    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    // 拿期望值expected与对象o的offset地址处的当前值比较，如果两个值相等，将当前值更新为x
    @ForceInline
    public final boolean compareAndSwapLong(Object o, long offset, long expected, long x) {
        return theInternalUnsafe.compareAndSetLong(o, offset, expected, x);
    }
    
    /**
     * Atomically updates Java variable to {@code x} if it is currently holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    // 拿期望值expected与对象o的offset地址处的当前值比较，如果两个值相等，将当前值更新为x
    @ForceInline
    public final boolean compareAndSwapObject(Object o, long offset, Object expected, Object x) {
        return theInternalUnsafe.compareAndSetObject(o, offset, expected, x);
    }
    
    /*▲ (2) 更新值，基于JVM内存操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ (3) 增减值，如果更新失败就不断尝试，属于乐观锁&自旋锁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Atomically adds the given value to the current value of a field or array element within the given object {@code o} at the given {@code offset}.
     *
     * @param o      object/array to update the field/element in
     * @param offset field/element offset
     * @param delta  the value to add
     *
     * @return the previous value
     *
     * @since 1.8
     */
    // 返回对象o的offset地址处的值，并将该值原子性地增加delta
    @ForceInline
    public final int getAndAddInt(Object o, long offset, int delta) {
        return theInternalUnsafe.getAndAddInt(o, offset, delta);
    }
    
    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o      object/array to update the field/element in
     * @param offset field/element offset
     * @param delta  the value to add
     *
     * @return the previous value
     *
     * @since 1.8
     */
    // 返回对象o的offset地址处的值，并将该值原子性地增加delta
    @ForceInline
    public final long getAndAddLong(Object o, long offset, long delta) {
        return theInternalUnsafe.getAndAddLong(o, offset, delta);
    }
    
    /*▲ (3) 增减值，如果更新失败就不断尝试，属于乐观锁&自旋锁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    /* 原子操作，基于JVM内存操作 ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ */
    
    
    
    
    
    
    /*▼ 线程操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Unblocks the given thread blocked on {@code park}, or, if it is
     * not blocked, causes the subsequent call to {@code park} not to
     * block.  Note: this operation is "unsafe" solely because the
     * caller must somehow ensure that the thread has not been
     * destroyed. Nothing special is usually required to ensure this
     * when called from Java (in which there will ordinarily be a live
     * reference to the thread) but this is not nearly-automatically
     * so when calling from native code.
     *
     * @param thread the thread to unpark.
     */
    /*
     * unpark发给目标线程一个许可证，该许可证被park消费。
     * 该许可证可以先发给线程使用，
     * 也可以等线程陷入阻塞，等待许可证时再（由另一个线程）给它，进而唤醒线程。
     *
     * 连续重复发给线程的许可证只被视为一个许可证。
     *
     * 注：该方法形参必须为线程
     */
    @ForceInline
    public void unpark(Object thread) {
        theInternalUnsafe.unpark(thread);
    }
    
    /**
     * Blocks current thread, returning when a balancing
     * {@code unpark} occurs, or a balancing {@code unpark} has
     * already occurred, or the thread is interrupted, or, if not
     * absolute and time is not zero, the given time nanoseconds have
     * elapsed, or if absolute, the given deadline in milliseconds
     * since Epoch has passed, or spuriously (i.e., returning for no
     * "reason"). Note: This operation is in the Unsafe class only
     * because {@code unpark} is, so it would be strange to place it
     * elsewhere.
     */
    /*
     * 等待消费一个许可证，这会使线程陷入阻塞。
     * 如果提前给过许可，则线程继续执行。
     * 如果陷入阻塞后等待许可，则可由别的线程发给它许可。
     * 使用线程中断也可以唤醒陷入阻塞的线程。
     *
     * 参数absolute：true代表后面的time是一个绝对时间，是一个时间点；false代表后面的time是一个相对时间，相对于当前的时间间隔
     * 参数time：可以是一个毫秒数时间点，该时间点是相对于1970年1月1日0时0分0秒开始的【绝对时间】，或者是一个纳秒数时间间隔【相对时间】
     * 如果是相对时间，且time>0，代表阻塞在time时间后自动解除
     * 如果是相对时间，且time==0，代表永远阻塞，除非被主动唤醒
     */
    @ForceInline
    public void park(boolean isAbsolute, long time) {
        theInternalUnsafe.park(isAbsolute, time);
    }
    
    /*▲ 线程操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 内存屏障 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Ensures that loads before the fence will not be reordered with loads and
     * stores after the fence; a "LoadLoad plus LoadStore barrier".
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_acquire)
     * (an "acquire fence").
     *
     * A pure LoadLoad fence is not provided, since the addition of LoadStore
     * is almost always desired, and most current hardware instructions that
     * provide a LoadLoad barrier also provide a LoadStore barrier for free.
     * @since 1.8
     */
    @ForceInline
    public void loadFence() {
        theInternalUnsafe.loadFence();
    }
    
    /**
     * Ensures that loads and stores before the fence will not be reordered with
     * stores after the fence; a "StoreStore plus LoadStore barrier".
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_release)
     * (a "release fence").
     *
     * A pure StoreStore fence is not provided, since the addition of LoadStore
     * is almost always desired, and most current hardware instructions that
     * provide a StoreStore barrier also provide a LoadStore barrier for free.
     * @since 1.8
     */
    @ForceInline
    public void storeFence() {
        theInternalUnsafe.storeFence();
    }
    
    /**
     * Ensures that loads and stores before the fence will not be reordered
     * with loads and stores after the fence.  Implies the effects of both
     * loadFence() and storeFence(), and in addition, the effect of a StoreLoad
     * barrier.
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_seq_cst).
     * @since 1.8
     */
    @ForceInline
    public void fullFence() {
        theInternalUnsafe.fullFence();
    }
    
    /*▲ 内存屏障 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Gets the load average in the system run queue assigned
     * to the available processors averaged over various periods of time.
     * This method retrieves the given {@code nelem} samples and
     * assigns to the elements of the given {@code loadavg} array.
     * The system imposes a maximum of 3 samples, representing
     * averages over the last 1,  5,  and  15 minutes, respectively.
     *
     * @param loadavg an array of double of size nelems
     * @param nelems the number of samples to be retrieved and
     *        must be 1 to 3.
     *
     * @return the number of samples actually retrieved; or -1
     *         if the load average is unobtainable.
     */
    @ForceInline
    public int getLoadAverage(double[] loadavg, int nelems) {
        return theInternalUnsafe.getLoadAverage(loadavg, nelems);
    }
    
    /**
     * Reports the size in bytes of a native memory page (whatever that is).
     * This value will always be a power of two.
     */
    // 返回内存分页大小
    @ForceInline
    public int pageSize() {
        return theInternalUnsafe.pageSize();
    }
    
    /**
     * Invokes the given direct byte buffer's cleaner, if any.
     *
     * @param directBuffer a direct byte buffer
     * @throws NullPointerException if {@code directBuffer} is null
     * @throws IllegalArgumentException if {@code directBuffer} is non-direct,
     * or is a {@link java.nio.Buffer#slice slice}, or is a
     * {@link java.nio.Buffer#duplicate duplicate}
     * @since 9
     */
    // 直接缓冲区清理器
    public void invokeCleaner(ByteBuffer directBuffer) {
        // 如果非直接缓冲区（堆内存），抛异常
        if (!directBuffer.isDirect()) {
            throw new IllegalArgumentException("buffer is non-direct");
        }
        
        DirectBuffer db = (DirectBuffer)directBuffer;
        if (db.attachment() != null) {
            throw new IllegalArgumentException("duplicate or slice");
        }
        
        // 获取附着的清理器，清理缓冲区
        Cleaner cleaner = db.cleaner();
        if (cleaner != null) {
            cleaner.clean();
        }
    }
    
    
    
    /*▼ 异常 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Throws the exception without telling the verifier. */
    @ForceInline
    public void throwException(Throwable ee) {
        theInternalUnsafe.throwException(ee);
    }
    
    /*▲ 异常 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
