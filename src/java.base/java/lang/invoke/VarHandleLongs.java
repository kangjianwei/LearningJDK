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

final class VarHandleLongs {

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
            return accessMode.at.accessModeType(receiverType, long.class);
        }

        @ForceInline
        static long get(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getLong(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static long getVolatile(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getLongVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static long getOpaque(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getLongOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static long getAcquire(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getLongAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadOnly.class, Object.class, long.class);
    }

    static final class FieldInstanceReadWrite extends FieldInstanceReadOnly {

        FieldInstanceReadWrite(Class<?> receiverType, long fieldOffset) {
            super(receiverType, fieldOffset, FieldInstanceReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldInstanceReadWrite handle, Object holder, long value) {
            UNSAFE.putLong(Objects.requireNonNull(handle.receiverType.cast(holder)),
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldInstanceReadWrite handle, Object holder, long value) {
            UNSAFE.putLongVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldInstanceReadWrite handle, Object holder, long value) {
            UNSAFE.putLongOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldInstanceReadWrite handle, Object holder, long value) {
            UNSAFE.putLongRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldInstanceReadWrite handle, Object holder, long expected, long value) {
            return UNSAFE.compareAndSetLong(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static long compareAndExchange(FieldInstanceReadWrite handle, Object holder, long expected, long value) {
            return UNSAFE.compareAndExchangeLong(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static long compareAndExchangeAcquire(FieldInstanceReadWrite handle, Object holder, long expected, long value) {
            return UNSAFE.compareAndExchangeLongAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static long compareAndExchangeRelease(FieldInstanceReadWrite handle, Object holder, long expected, long value) {
            return UNSAFE.compareAndExchangeLongRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldInstanceReadWrite handle, Object holder, long expected, long value) {
            return UNSAFE.weakCompareAndSetLongPlain(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldInstanceReadWrite handle, Object holder, long expected, long value) {
            return UNSAFE.weakCompareAndSetLong(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldInstanceReadWrite handle, Object holder, long expected, long value) {
            return UNSAFE.weakCompareAndSetLongAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldInstanceReadWrite handle, Object holder, long expected, long value) {
            return UNSAFE.weakCompareAndSetLongRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static long getAndSet(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndSetLong(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static long getAndSetAcquire(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndSetLongAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static long getAndSetRelease(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndSetLongRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static long getAndAdd(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndAddLong(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndAddAcquire(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndAddLongAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndAddRelease(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndAddLongRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }


        @ForceInline
        static long getAndBitwiseOr(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndBitwiseOrLong(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseOrRelease(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndBitwiseOrLongRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseOrAcquire(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndBitwiseOrLongAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseAnd(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndBitwiseAndLong(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseAndRelease(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndBitwiseAndLongRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseAndAcquire(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndBitwiseAndLongAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseXor(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndBitwiseXorLong(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseXorRelease(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndBitwiseXorLongRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseXorAcquire(FieldInstanceReadWrite handle, Object holder, long value) {
            return UNSAFE.getAndBitwiseXorLongAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadWrite.class, Object.class, long.class);
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
            return accessMode.at.accessModeType(null, long.class);
        }

        @ForceInline
        static long get(FieldStaticReadOnly handle) {
            return UNSAFE.getLong(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static long getVolatile(FieldStaticReadOnly handle) {
            return UNSAFE.getLongVolatile(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static long getOpaque(FieldStaticReadOnly handle) {
            return UNSAFE.getLongOpaque(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static long getAcquire(FieldStaticReadOnly handle) {
            return UNSAFE.getLongAcquire(handle.base,
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadOnly.class, null, long.class);
    }

    static final class FieldStaticReadWrite extends FieldStaticReadOnly {

        FieldStaticReadWrite(Object base, long fieldOffset) {
            super(base, fieldOffset, FieldStaticReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldStaticReadWrite handle, long value) {
            UNSAFE.putLong(handle.base,
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldStaticReadWrite handle, long value) {
            UNSAFE.putLongVolatile(handle.base,
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldStaticReadWrite handle, long value) {
            UNSAFE.putLongOpaque(handle.base,
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldStaticReadWrite handle, long value) {
            UNSAFE.putLongRelease(handle.base,
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldStaticReadWrite handle, long expected, long value) {
            return UNSAFE.compareAndSetLong(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }


        @ForceInline
        static long compareAndExchange(FieldStaticReadWrite handle, long expected, long value) {
            return UNSAFE.compareAndExchangeLong(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static long compareAndExchangeAcquire(FieldStaticReadWrite handle, long expected, long value) {
            return UNSAFE.compareAndExchangeLongAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static long compareAndExchangeRelease(FieldStaticReadWrite handle, long expected, long value) {
            return UNSAFE.compareAndExchangeLongRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldStaticReadWrite handle, long expected, long value) {
            return UNSAFE.weakCompareAndSetLongPlain(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldStaticReadWrite handle, long expected, long value) {
            return UNSAFE.weakCompareAndSetLong(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldStaticReadWrite handle, long expected, long value) {
            return UNSAFE.weakCompareAndSetLongAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldStaticReadWrite handle, long expected, long value) {
            return UNSAFE.weakCompareAndSetLongRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static long getAndSet(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndSetLong(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static long getAndSetAcquire(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndSetLongAcquire(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static long getAndSetRelease(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndSetLongRelease(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static long getAndAdd(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndAddLong(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndAddAcquire(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndAddLongAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndAddRelease(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndAddLongRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseOr(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndBitwiseOrLong(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseOrRelease(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndBitwiseOrLongRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseOrAcquire(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndBitwiseOrLongAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseAnd(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndBitwiseAndLong(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseAndRelease(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndBitwiseAndLongRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseAndAcquire(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndBitwiseAndLongAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseXor(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndBitwiseXorLong(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseXorRelease(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndBitwiseXorLongRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseXorAcquire(FieldStaticReadWrite handle, long value) {
            return UNSAFE.getAndBitwiseXorLongAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadWrite.class, null, long.class);
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
            return accessMode.at.accessModeType(long[].class, long.class, int.class);
        }


        @ForceInline
        static long get(Array handle, Object oarray, int index) {
            long[] array = (long[]) oarray;
            return array[index];
        }

        @ForceInline
        static void set(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            array[index] = value;
        }

        @ForceInline
        static long getVolatile(Array handle, Object oarray, int index) {
            long[] array = (long[]) oarray;
            return UNSAFE.getLongVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setVolatile(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            UNSAFE.putLongVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static long getOpaque(Array handle, Object oarray, int index) {
            long[] array = (long[]) oarray;
            return UNSAFE.getLongOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setOpaque(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            UNSAFE.putLongOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static long getAcquire(Array handle, Object oarray, int index) {
            long[] array = (long[]) oarray;
            return UNSAFE.getLongAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setRelease(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            UNSAFE.putLongRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean compareAndSet(Array handle, Object oarray, int index, long expected, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.compareAndSetLong(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static long compareAndExchange(Array handle, Object oarray, int index, long expected, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.compareAndExchangeLong(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static long compareAndExchangeAcquire(Array handle, Object oarray, int index, long expected, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.compareAndExchangeLongAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static long compareAndExchangeRelease(Array handle, Object oarray, int index, long expected, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.compareAndExchangeLongRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(Array handle, Object oarray, int index, long expected, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.weakCompareAndSetLongPlain(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSet(Array handle, Object oarray, int index, long expected, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.weakCompareAndSetLong(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(Array handle, Object oarray, int index, long expected, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.weakCompareAndSetLongAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(Array handle, Object oarray, int index, long expected, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.weakCompareAndSetLongRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static long getAndSet(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndSetLong(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static long getAndSetAcquire(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndSetLongAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static long getAndSetRelease(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndSetLongRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static long getAndAdd(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndAddLong(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static long getAndAddAcquire(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndAddLongAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static long getAndAddRelease(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndAddLongRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static long getAndBitwiseOr(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndBitwiseOrLong(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseOrRelease(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndBitwiseOrLongRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseOrAcquire(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndBitwiseOrLongAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseAnd(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndBitwiseAndLong(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseAndRelease(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndBitwiseAndLongRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseAndAcquire(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndBitwiseAndLongAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseXor(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndBitwiseXorLong(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseXorRelease(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndBitwiseXorLongRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static long getAndBitwiseXorAcquire(Array handle, Object oarray, int index, long value) {
            long[] array = (long[]) oarray;
            return UNSAFE.getAndBitwiseXorLongAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        static final VarForm FORM = new VarForm(Array.class, long[].class, long.class, int.class);
    }
}
