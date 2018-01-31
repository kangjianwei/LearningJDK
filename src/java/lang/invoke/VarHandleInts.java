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

final class VarHandleInts {

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
            return accessMode.at.accessModeType(receiverType, int.class);
        }

        @ForceInline
        static int get(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getInt(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static int getVolatile(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getIntVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static int getOpaque(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getIntOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static int getAcquire(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getIntAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadOnly.class, Object.class, int.class);
    }

    static final class FieldInstanceReadWrite extends FieldInstanceReadOnly {

        FieldInstanceReadWrite(Class<?> receiverType, long fieldOffset) {
            super(receiverType, fieldOffset, FieldInstanceReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldInstanceReadWrite handle, Object holder, int value) {
            UNSAFE.putInt(Objects.requireNonNull(handle.receiverType.cast(holder)),
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldInstanceReadWrite handle, Object holder, int value) {
            UNSAFE.putIntVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldInstanceReadWrite handle, Object holder, int value) {
            UNSAFE.putIntOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldInstanceReadWrite handle, Object holder, int value) {
            UNSAFE.putIntRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldInstanceReadWrite handle, Object holder, int expected, int value) {
            return UNSAFE.compareAndSetInt(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static int compareAndExchange(FieldInstanceReadWrite handle, Object holder, int expected, int value) {
            return UNSAFE.compareAndExchangeInt(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static int compareAndExchangeAcquire(FieldInstanceReadWrite handle, Object holder, int expected, int value) {
            return UNSAFE.compareAndExchangeIntAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static int compareAndExchangeRelease(FieldInstanceReadWrite handle, Object holder, int expected, int value) {
            return UNSAFE.compareAndExchangeIntRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldInstanceReadWrite handle, Object holder, int expected, int value) {
            return UNSAFE.weakCompareAndSetIntPlain(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldInstanceReadWrite handle, Object holder, int expected, int value) {
            return UNSAFE.weakCompareAndSetInt(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldInstanceReadWrite handle, Object holder, int expected, int value) {
            return UNSAFE.weakCompareAndSetIntAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldInstanceReadWrite handle, Object holder, int expected, int value) {
            return UNSAFE.weakCompareAndSetIntRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static int getAndSet(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndSetInt(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static int getAndSetAcquire(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndSetIntAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static int getAndSetRelease(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndSetIntRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static int getAndAdd(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndAddInt(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndAddAcquire(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndAddIntAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndAddRelease(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndAddIntRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }


        @ForceInline
        static int getAndBitwiseOr(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndBitwiseOrInt(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseOrRelease(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndBitwiseOrIntRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseOrAcquire(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndBitwiseOrIntAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseAnd(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndBitwiseAndInt(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseAndRelease(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndBitwiseAndIntRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseAndAcquire(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndBitwiseAndIntAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseXor(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndBitwiseXorInt(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseXorRelease(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndBitwiseXorIntRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseXorAcquire(FieldInstanceReadWrite handle, Object holder, int value) {
            return UNSAFE.getAndBitwiseXorIntAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadWrite.class, Object.class, int.class);
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
            return accessMode.at.accessModeType(null, int.class);
        }

        @ForceInline
        static int get(FieldStaticReadOnly handle) {
            return UNSAFE.getInt(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static int getVolatile(FieldStaticReadOnly handle) {
            return UNSAFE.getIntVolatile(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static int getOpaque(FieldStaticReadOnly handle) {
            return UNSAFE.getIntOpaque(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static int getAcquire(FieldStaticReadOnly handle) {
            return UNSAFE.getIntAcquire(handle.base,
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadOnly.class, null, int.class);
    }

    static final class FieldStaticReadWrite extends FieldStaticReadOnly {

        FieldStaticReadWrite(Object base, long fieldOffset) {
            super(base, fieldOffset, FieldStaticReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldStaticReadWrite handle, int value) {
            UNSAFE.putInt(handle.base,
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldStaticReadWrite handle, int value) {
            UNSAFE.putIntVolatile(handle.base,
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldStaticReadWrite handle, int value) {
            UNSAFE.putIntOpaque(handle.base,
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldStaticReadWrite handle, int value) {
            UNSAFE.putIntRelease(handle.base,
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldStaticReadWrite handle, int expected, int value) {
            return UNSAFE.compareAndSetInt(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }


        @ForceInline
        static int compareAndExchange(FieldStaticReadWrite handle, int expected, int value) {
            return UNSAFE.compareAndExchangeInt(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static int compareAndExchangeAcquire(FieldStaticReadWrite handle, int expected, int value) {
            return UNSAFE.compareAndExchangeIntAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static int compareAndExchangeRelease(FieldStaticReadWrite handle, int expected, int value) {
            return UNSAFE.compareAndExchangeIntRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldStaticReadWrite handle, int expected, int value) {
            return UNSAFE.weakCompareAndSetIntPlain(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldStaticReadWrite handle, int expected, int value) {
            return UNSAFE.weakCompareAndSetInt(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldStaticReadWrite handle, int expected, int value) {
            return UNSAFE.weakCompareAndSetIntAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldStaticReadWrite handle, int expected, int value) {
            return UNSAFE.weakCompareAndSetIntRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static int getAndSet(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndSetInt(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static int getAndSetAcquire(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndSetIntAcquire(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static int getAndSetRelease(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndSetIntRelease(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static int getAndAdd(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndAddInt(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndAddAcquire(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndAddIntAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndAddRelease(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndAddIntRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseOr(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndBitwiseOrInt(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseOrRelease(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndBitwiseOrIntRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseOrAcquire(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndBitwiseOrIntAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseAnd(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndBitwiseAndInt(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseAndRelease(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndBitwiseAndIntRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseAndAcquire(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndBitwiseAndIntAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseXor(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndBitwiseXorInt(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseXorRelease(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndBitwiseXorIntRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseXorAcquire(FieldStaticReadWrite handle, int value) {
            return UNSAFE.getAndBitwiseXorIntAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadWrite.class, null, int.class);
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
            return accessMode.at.accessModeType(int[].class, int.class, int.class);
        }


        @ForceInline
        static int get(Array handle, Object oarray, int index) {
            int[] array = (int[]) oarray;
            return array[index];
        }

        @ForceInline
        static void set(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            array[index] = value;
        }

        @ForceInline
        static int getVolatile(Array handle, Object oarray, int index) {
            int[] array = (int[]) oarray;
            return UNSAFE.getIntVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setVolatile(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            UNSAFE.putIntVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static int getOpaque(Array handle, Object oarray, int index) {
            int[] array = (int[]) oarray;
            return UNSAFE.getIntOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setOpaque(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            UNSAFE.putIntOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static int getAcquire(Array handle, Object oarray, int index) {
            int[] array = (int[]) oarray;
            return UNSAFE.getIntAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setRelease(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            UNSAFE.putIntRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean compareAndSet(Array handle, Object oarray, int index, int expected, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.compareAndSetInt(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static int compareAndExchange(Array handle, Object oarray, int index, int expected, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.compareAndExchangeInt(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static int compareAndExchangeAcquire(Array handle, Object oarray, int index, int expected, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.compareAndExchangeIntAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static int compareAndExchangeRelease(Array handle, Object oarray, int index, int expected, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.compareAndExchangeIntRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(Array handle, Object oarray, int index, int expected, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.weakCompareAndSetIntPlain(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSet(Array handle, Object oarray, int index, int expected, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.weakCompareAndSetInt(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(Array handle, Object oarray, int index, int expected, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.weakCompareAndSetIntAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(Array handle, Object oarray, int index, int expected, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.weakCompareAndSetIntRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static int getAndSet(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndSetInt(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static int getAndSetAcquire(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndSetIntAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static int getAndSetRelease(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndSetIntRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static int getAndAdd(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndAddInt(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static int getAndAddAcquire(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndAddIntAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static int getAndAddRelease(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndAddIntRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static int getAndBitwiseOr(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndBitwiseOrInt(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseOrRelease(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndBitwiseOrIntRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseOrAcquire(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndBitwiseOrIntAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseAnd(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndBitwiseAndInt(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseAndRelease(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndBitwiseAndIntRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseAndAcquire(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndBitwiseAndIntAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseXor(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndBitwiseXorInt(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseXorRelease(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndBitwiseXorIntRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static int getAndBitwiseXorAcquire(Array handle, Object oarray, int index, int value) {
            int[] array = (int[]) oarray;
            return UNSAFE.getAndBitwiseXorIntAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        static final VarForm FORM = new VarForm(Array.class, int[].class, int.class, int.class);
    }
}
