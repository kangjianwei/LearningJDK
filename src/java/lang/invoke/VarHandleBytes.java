/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.invoke;

import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.ForceInline;

import java.util.Objects;

import static java.lang.invoke.MethodHandleStatics.UNSAFE;

// -- This file was mechanically generated: Do not edit! -- //

final class VarHandleBytes {

    static class FieldInstanceReadOnly extends VarHandle {
        final long fieldOffset;
        final Class<?> receiverType;

        FieldInstanceReadOnly(Class<?> receiverType, long fieldOffset) {
            this(receiverType, fieldOffset, FieldInstanceReadOnly.FORM);
        }

        protected FieldInstanceReadOnly(Class<?> receiverType, long fieldOffset,
                                        VarForm form) {
            super(form);
            this.fieldOffset = fieldOffset;
            this.receiverType = receiverType;
        }

        @Override
        final MethodType accessModeTypeUncached(AccessMode accessMode) {
            return accessMode.at.accessModeType(receiverType, byte.class);
        }

        @ForceInline
        static byte get(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getByte(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static byte getVolatile(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getByteVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static byte getOpaque(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getByteOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static byte getAcquire(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getByteAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadOnly.class, Object.class, byte.class);
    }

    static final class FieldInstanceReadWrite extends FieldInstanceReadOnly {

        FieldInstanceReadWrite(Class<?> receiverType, long fieldOffset) {
            super(receiverType, fieldOffset, FieldInstanceReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldInstanceReadWrite handle, Object holder, byte value) {
            UNSAFE.putByte(Objects.requireNonNull(handle.receiverType.cast(holder)),
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldInstanceReadWrite handle, Object holder, byte value) {
            UNSAFE.putByteVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldInstanceReadWrite handle, Object holder, byte value) {
            UNSAFE.putByteOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldInstanceReadWrite handle, Object holder, byte value) {
            UNSAFE.putByteRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldInstanceReadWrite handle, Object holder, byte expected, byte value) {
            return UNSAFE.compareAndSetByte(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static byte compareAndExchange(FieldInstanceReadWrite handle, Object holder, byte expected, byte value) {
            return UNSAFE.compareAndExchangeByte(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static byte compareAndExchangeAcquire(FieldInstanceReadWrite handle, Object holder, byte expected, byte value) {
            return UNSAFE.compareAndExchangeByteAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static byte compareAndExchangeRelease(FieldInstanceReadWrite handle, Object holder, byte expected, byte value) {
            return UNSAFE.compareAndExchangeByteRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldInstanceReadWrite handle, Object holder, byte expected, byte value) {
            return UNSAFE.weakCompareAndSetBytePlain(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldInstanceReadWrite handle, Object holder, byte expected, byte value) {
            return UNSAFE.weakCompareAndSetByte(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldInstanceReadWrite handle, Object holder, byte expected, byte value) {
            return UNSAFE.weakCompareAndSetByteAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldInstanceReadWrite handle, Object holder, byte expected, byte value) {
            return UNSAFE.weakCompareAndSetByteRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static byte getAndSet(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndSetByte(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static byte getAndSetAcquire(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndSetByteAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static byte getAndSetRelease(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndSetByteRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static byte getAndAdd(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndAddByte(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndAddAcquire(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndAddByteAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndAddRelease(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndAddByteRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }


        @ForceInline
        static byte getAndBitwiseOr(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndBitwiseOrByte(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseOrRelease(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndBitwiseOrByteRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseOrAcquire(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndBitwiseOrByteAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseAnd(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndBitwiseAndByte(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseAndRelease(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndBitwiseAndByteRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseAndAcquire(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndBitwiseAndByteAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseXor(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndBitwiseXorByte(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseXorRelease(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndBitwiseXorByteRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseXorAcquire(FieldInstanceReadWrite handle, Object holder, byte value) {
            return UNSAFE.getAndBitwiseXorByteAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadWrite.class, Object.class, byte.class);
    }


    static class FieldStaticReadOnly extends VarHandle {
        final Object base;
        final long fieldOffset;

        FieldStaticReadOnly(Object base, long fieldOffset) {
            this(base, fieldOffset, FieldStaticReadOnly.FORM);
        }

        protected FieldStaticReadOnly(Object base, long fieldOffset,
                                      VarForm form) {
            super(form);
            this.base = base;
            this.fieldOffset = fieldOffset;
        }

        @Override
        final MethodType accessModeTypeUncached(AccessMode accessMode) {
            return accessMode.at.accessModeType(null, byte.class);
        }

        @ForceInline
        static byte get(FieldStaticReadOnly handle) {
            return UNSAFE.getByte(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static byte getVolatile(FieldStaticReadOnly handle) {
            return UNSAFE.getByteVolatile(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static byte getOpaque(FieldStaticReadOnly handle) {
            return UNSAFE.getByteOpaque(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static byte getAcquire(FieldStaticReadOnly handle) {
            return UNSAFE.getByteAcquire(handle.base,
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadOnly.class, null, byte.class);
    }

    static final class FieldStaticReadWrite extends FieldStaticReadOnly {

        FieldStaticReadWrite(Object base, long fieldOffset) {
            super(base, fieldOffset, FieldStaticReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldStaticReadWrite handle, byte value) {
            UNSAFE.putByte(handle.base,
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldStaticReadWrite handle, byte value) {
            UNSAFE.putByteVolatile(handle.base,
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldStaticReadWrite handle, byte value) {
            UNSAFE.putByteOpaque(handle.base,
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldStaticReadWrite handle, byte value) {
            UNSAFE.putByteRelease(handle.base,
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldStaticReadWrite handle, byte expected, byte value) {
            return UNSAFE.compareAndSetByte(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }


        @ForceInline
        static byte compareAndExchange(FieldStaticReadWrite handle, byte expected, byte value) {
            return UNSAFE.compareAndExchangeByte(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static byte compareAndExchangeAcquire(FieldStaticReadWrite handle, byte expected, byte value) {
            return UNSAFE.compareAndExchangeByteAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static byte compareAndExchangeRelease(FieldStaticReadWrite handle, byte expected, byte value) {
            return UNSAFE.compareAndExchangeByteRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldStaticReadWrite handle, byte expected, byte value) {
            return UNSAFE.weakCompareAndSetBytePlain(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldStaticReadWrite handle, byte expected, byte value) {
            return UNSAFE.weakCompareAndSetByte(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldStaticReadWrite handle, byte expected, byte value) {
            return UNSAFE.weakCompareAndSetByteAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldStaticReadWrite handle, byte expected, byte value) {
            return UNSAFE.weakCompareAndSetByteRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static byte getAndSet(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndSetByte(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static byte getAndSetAcquire(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndSetByteAcquire(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static byte getAndSetRelease(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndSetByteRelease(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static byte getAndAdd(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndAddByte(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndAddAcquire(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndAddByteAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndAddRelease(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndAddByteRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseOr(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndBitwiseOrByte(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseOrRelease(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndBitwiseOrByteRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseOrAcquire(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndBitwiseOrByteAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseAnd(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndBitwiseAndByte(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseAndRelease(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndBitwiseAndByteRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseAndAcquire(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndBitwiseAndByteAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseXor(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndBitwiseXorByte(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseXorRelease(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndBitwiseXorByteRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseXorAcquire(FieldStaticReadWrite handle, byte value) {
            return UNSAFE.getAndBitwiseXorByteAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadWrite.class, null, byte.class);
    }


    static final class Array extends VarHandle {
        final int abase;
        final int ashift;

        Array(int abase, int ashift) {
            super(Array.FORM);
            this.abase = abase;
            this.ashift = ashift;
        }

        @Override
        final MethodType accessModeTypeUncached(AccessMode accessMode) {
            return accessMode.at.accessModeType(byte[].class, byte.class, int.class);
        }


        @ForceInline
        static byte get(Array handle, Object oarray, int index) {
            byte[] array = (byte[]) oarray;
            return array[index];
        }

        @ForceInline
        static void set(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            array[index] = value;
        }

        @ForceInline
        static byte getVolatile(Array handle, Object oarray, int index) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getByteVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setVolatile(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            UNSAFE.putByteVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static byte getOpaque(Array handle, Object oarray, int index) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getByteOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setOpaque(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            UNSAFE.putByteOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static byte getAcquire(Array handle, Object oarray, int index) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getByteAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setRelease(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            UNSAFE.putByteRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean compareAndSet(Array handle, Object oarray, int index, byte expected, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.compareAndSetByte(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static byte compareAndExchange(Array handle, Object oarray, int index, byte expected, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.compareAndExchangeByte(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static byte compareAndExchangeAcquire(Array handle, Object oarray, int index, byte expected, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.compareAndExchangeByteAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static byte compareAndExchangeRelease(Array handle, Object oarray, int index, byte expected, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.compareAndExchangeByteRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(Array handle, Object oarray, int index, byte expected, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.weakCompareAndSetBytePlain(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSet(Array handle, Object oarray, int index, byte expected, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.weakCompareAndSetByte(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(Array handle, Object oarray, int index, byte expected, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.weakCompareAndSetByteAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(Array handle, Object oarray, int index, byte expected, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.weakCompareAndSetByteRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static byte getAndSet(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndSetByte(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static byte getAndSetAcquire(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndSetByteAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static byte getAndSetRelease(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndSetByteRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static byte getAndAdd(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndAddByte(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static byte getAndAddAcquire(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndAddByteAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static byte getAndAddRelease(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndAddByteRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static byte getAndBitwiseOr(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndBitwiseOrByte(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseOrRelease(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndBitwiseOrByteRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseOrAcquire(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndBitwiseOrByteAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseAnd(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndBitwiseAndByte(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseAndRelease(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndBitwiseAndByteRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseAndAcquire(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndBitwiseAndByteAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseXor(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndBitwiseXorByte(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseXorRelease(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndBitwiseXorByteRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static byte getAndBitwiseXorAcquire(Array handle, Object oarray, int index, byte value) {
            byte[] array = (byte[]) oarray;
            return UNSAFE.getAndBitwiseXorByteAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        static final VarForm FORM = new VarForm(Array.class, byte[].class, byte.class, int.class);
    }
}
