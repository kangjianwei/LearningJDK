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

final class VarHandleShorts {

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
            return accessMode.at.accessModeType(receiverType, short.class);
        }

        @ForceInline
        static short get(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getShort(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static short getVolatile(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getShortVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static short getOpaque(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getShortOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static short getAcquire(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getShortAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadOnly.class, Object.class, short.class);
    }

    static final class FieldInstanceReadWrite extends FieldInstanceReadOnly {

        FieldInstanceReadWrite(Class<?> receiverType, long fieldOffset) {
            super(receiverType, fieldOffset, FieldInstanceReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldInstanceReadWrite handle, Object holder, short value) {
            UNSAFE.putShort(Objects.requireNonNull(handle.receiverType.cast(holder)),
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldInstanceReadWrite handle, Object holder, short value) {
            UNSAFE.putShortVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldInstanceReadWrite handle, Object holder, short value) {
            UNSAFE.putShortOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldInstanceReadWrite handle, Object holder, short value) {
            UNSAFE.putShortRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldInstanceReadWrite handle, Object holder, short expected, short value) {
            return UNSAFE.compareAndSetShort(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static short compareAndExchange(FieldInstanceReadWrite handle, Object holder, short expected, short value) {
            return UNSAFE.compareAndExchangeShort(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static short compareAndExchangeAcquire(FieldInstanceReadWrite handle, Object holder, short expected, short value) {
            return UNSAFE.compareAndExchangeShortAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static short compareAndExchangeRelease(FieldInstanceReadWrite handle, Object holder, short expected, short value) {
            return UNSAFE.compareAndExchangeShortRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldInstanceReadWrite handle, Object holder, short expected, short value) {
            return UNSAFE.weakCompareAndSetShortPlain(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldInstanceReadWrite handle, Object holder, short expected, short value) {
            return UNSAFE.weakCompareAndSetShort(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldInstanceReadWrite handle, Object holder, short expected, short value) {
            return UNSAFE.weakCompareAndSetShortAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldInstanceReadWrite handle, Object holder, short expected, short value) {
            return UNSAFE.weakCompareAndSetShortRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static short getAndSet(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndSetShort(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static short getAndSetAcquire(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndSetShortAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static short getAndSetRelease(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndSetShortRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static short getAndAdd(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndAddShort(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndAddAcquire(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndAddShortAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndAddRelease(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndAddShortRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }


        @ForceInline
        static short getAndBitwiseOr(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndBitwiseOrShort(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseOrRelease(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndBitwiseOrShortRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseOrAcquire(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndBitwiseOrShortAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseAnd(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndBitwiseAndShort(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseAndRelease(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndBitwiseAndShortRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseAndAcquire(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndBitwiseAndShortAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseXor(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndBitwiseXorShort(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseXorRelease(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndBitwiseXorShortRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseXorAcquire(FieldInstanceReadWrite handle, Object holder, short value) {
            return UNSAFE.getAndBitwiseXorShortAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadWrite.class, Object.class, short.class);
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
            return accessMode.at.accessModeType(null, short.class);
        }

        @ForceInline
        static short get(FieldStaticReadOnly handle) {
            return UNSAFE.getShort(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static short getVolatile(FieldStaticReadOnly handle) {
            return UNSAFE.getShortVolatile(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static short getOpaque(FieldStaticReadOnly handle) {
            return UNSAFE.getShortOpaque(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static short getAcquire(FieldStaticReadOnly handle) {
            return UNSAFE.getShortAcquire(handle.base,
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadOnly.class, null, short.class);
    }

    static final class FieldStaticReadWrite extends FieldStaticReadOnly {

        FieldStaticReadWrite(Object base, long fieldOffset) {
            super(base, fieldOffset, FieldStaticReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldStaticReadWrite handle, short value) {
            UNSAFE.putShort(handle.base,
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldStaticReadWrite handle, short value) {
            UNSAFE.putShortVolatile(handle.base,
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldStaticReadWrite handle, short value) {
            UNSAFE.putShortOpaque(handle.base,
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldStaticReadWrite handle, short value) {
            UNSAFE.putShortRelease(handle.base,
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldStaticReadWrite handle, short expected, short value) {
            return UNSAFE.compareAndSetShort(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }


        @ForceInline
        static short compareAndExchange(FieldStaticReadWrite handle, short expected, short value) {
            return UNSAFE.compareAndExchangeShort(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static short compareAndExchangeAcquire(FieldStaticReadWrite handle, short expected, short value) {
            return UNSAFE.compareAndExchangeShortAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static short compareAndExchangeRelease(FieldStaticReadWrite handle, short expected, short value) {
            return UNSAFE.compareAndExchangeShortRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldStaticReadWrite handle, short expected, short value) {
            return UNSAFE.weakCompareAndSetShortPlain(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldStaticReadWrite handle, short expected, short value) {
            return UNSAFE.weakCompareAndSetShort(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldStaticReadWrite handle, short expected, short value) {
            return UNSAFE.weakCompareAndSetShortAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldStaticReadWrite handle, short expected, short value) {
            return UNSAFE.weakCompareAndSetShortRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static short getAndSet(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndSetShort(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static short getAndSetAcquire(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndSetShortAcquire(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static short getAndSetRelease(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndSetShortRelease(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static short getAndAdd(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndAddShort(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndAddAcquire(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndAddShortAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndAddRelease(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndAddShortRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseOr(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndBitwiseOrShort(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseOrRelease(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndBitwiseOrShortRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseOrAcquire(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndBitwiseOrShortAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseAnd(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndBitwiseAndShort(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseAndRelease(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndBitwiseAndShortRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseAndAcquire(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndBitwiseAndShortAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseXor(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndBitwiseXorShort(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseXorRelease(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndBitwiseXorShortRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseXorAcquire(FieldStaticReadWrite handle, short value) {
            return UNSAFE.getAndBitwiseXorShortAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadWrite.class, null, short.class);
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
            return accessMode.at.accessModeType(short[].class, short.class, int.class);
        }


        @ForceInline
        static short get(Array handle, Object oarray, int index) {
            short[] array = (short[]) oarray;
            return array[index];
        }

        @ForceInline
        static void set(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            array[index] = value;
        }

        @ForceInline
        static short getVolatile(Array handle, Object oarray, int index) {
            short[] array = (short[]) oarray;
            return UNSAFE.getShortVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setVolatile(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            UNSAFE.putShortVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static short getOpaque(Array handle, Object oarray, int index) {
            short[] array = (short[]) oarray;
            return UNSAFE.getShortOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setOpaque(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            UNSAFE.putShortOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static short getAcquire(Array handle, Object oarray, int index) {
            short[] array = (short[]) oarray;
            return UNSAFE.getShortAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setRelease(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            UNSAFE.putShortRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean compareAndSet(Array handle, Object oarray, int index, short expected, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.compareAndSetShort(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static short compareAndExchange(Array handle, Object oarray, int index, short expected, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.compareAndExchangeShort(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static short compareAndExchangeAcquire(Array handle, Object oarray, int index, short expected, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.compareAndExchangeShortAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static short compareAndExchangeRelease(Array handle, Object oarray, int index, short expected, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.compareAndExchangeShortRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(Array handle, Object oarray, int index, short expected, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.weakCompareAndSetShortPlain(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSet(Array handle, Object oarray, int index, short expected, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.weakCompareAndSetShort(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(Array handle, Object oarray, int index, short expected, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.weakCompareAndSetShortAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(Array handle, Object oarray, int index, short expected, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.weakCompareAndSetShortRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static short getAndSet(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndSetShort(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static short getAndSetAcquire(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndSetShortAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static short getAndSetRelease(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndSetShortRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static short getAndAdd(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndAddShort(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static short getAndAddAcquire(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndAddShortAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static short getAndAddRelease(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndAddShortRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static short getAndBitwiseOr(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndBitwiseOrShort(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseOrRelease(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndBitwiseOrShortRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseOrAcquire(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndBitwiseOrShortAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseAnd(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndBitwiseAndShort(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseAndRelease(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndBitwiseAndShortRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseAndAcquire(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndBitwiseAndShortAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseXor(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndBitwiseXorShort(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseXorRelease(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndBitwiseXorShortRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static short getAndBitwiseXorAcquire(Array handle, Object oarray, int index, short value) {
            short[] array = (short[]) oarray;
            return UNSAFE.getAndBitwiseXorShortAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        static final VarForm FORM = new VarForm(Array.class, short[].class, short.class, int.class);
    }
}
