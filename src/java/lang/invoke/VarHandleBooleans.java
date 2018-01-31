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

final class VarHandleBooleans {

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
            return accessMode.at.accessModeType(receiverType, boolean.class);
        }

        @ForceInline
        static boolean get(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getBoolean(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static boolean getVolatile(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getBooleanVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static boolean getOpaque(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getBooleanOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static boolean getAcquire(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getBooleanAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadOnly.class, Object.class, boolean.class);
    }

    static final class FieldInstanceReadWrite extends FieldInstanceReadOnly {

        FieldInstanceReadWrite(Class<?> receiverType, long fieldOffset) {
            super(receiverType, fieldOffset, FieldInstanceReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldInstanceReadWrite handle, Object holder, boolean value) {
            UNSAFE.putBoolean(Objects.requireNonNull(handle.receiverType.cast(holder)),
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldInstanceReadWrite handle, Object holder, boolean value) {
            UNSAFE.putBooleanVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldInstanceReadWrite handle, Object holder, boolean value) {
            UNSAFE.putBooleanOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldInstanceReadWrite handle, Object holder, boolean value) {
            UNSAFE.putBooleanRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldInstanceReadWrite handle, Object holder, boolean expected, boolean value) {
            return UNSAFE.compareAndSetBoolean(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean compareAndExchange(FieldInstanceReadWrite handle, Object holder, boolean expected, boolean value) {
            return UNSAFE.compareAndExchangeBoolean(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean compareAndExchangeAcquire(FieldInstanceReadWrite handle, Object holder, boolean expected, boolean value) {
            return UNSAFE.compareAndExchangeBooleanAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean compareAndExchangeRelease(FieldInstanceReadWrite handle, Object holder, boolean expected, boolean value) {
            return UNSAFE.compareAndExchangeBooleanRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldInstanceReadWrite handle, Object holder, boolean expected, boolean value) {
            return UNSAFE.weakCompareAndSetBooleanPlain(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldInstanceReadWrite handle, Object holder, boolean expected, boolean value) {
            return UNSAFE.weakCompareAndSetBoolean(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldInstanceReadWrite handle, Object holder, boolean expected, boolean value) {
            return UNSAFE.weakCompareAndSetBooleanAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldInstanceReadWrite handle, Object holder, boolean expected, boolean value) {
            return UNSAFE.weakCompareAndSetBooleanRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean getAndSet(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndSetBoolean(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static boolean getAndSetAcquire(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndSetBooleanAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static boolean getAndSetRelease(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndSetBooleanRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static boolean getAndBitwiseOr(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndBitwiseOrBoolean(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseOrRelease(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndBitwiseOrBooleanRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseOrAcquire(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndBitwiseOrBooleanAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseAnd(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndBitwiseAndBoolean(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseAndRelease(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndBitwiseAndBooleanRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseAndAcquire(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndBitwiseAndBooleanAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseXor(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndBitwiseXorBoolean(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseXorRelease(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndBitwiseXorBooleanRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseXorAcquire(FieldInstanceReadWrite handle, Object holder, boolean value) {
            return UNSAFE.getAndBitwiseXorBooleanAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadWrite.class, Object.class, boolean.class);
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
            return accessMode.at.accessModeType(null, boolean.class);
        }

        @ForceInline
        static boolean get(FieldStaticReadOnly handle) {
            return UNSAFE.getBoolean(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static boolean getVolatile(FieldStaticReadOnly handle) {
            return UNSAFE.getBooleanVolatile(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static boolean getOpaque(FieldStaticReadOnly handle) {
            return UNSAFE.getBooleanOpaque(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static boolean getAcquire(FieldStaticReadOnly handle) {
            return UNSAFE.getBooleanAcquire(handle.base,
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadOnly.class, null, boolean.class);
    }

    static final class FieldStaticReadWrite extends FieldStaticReadOnly {

        FieldStaticReadWrite(Object base, long fieldOffset) {
            super(base, fieldOffset, FieldStaticReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldStaticReadWrite handle, boolean value) {
            UNSAFE.putBoolean(handle.base,
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldStaticReadWrite handle, boolean value) {
            UNSAFE.putBooleanVolatile(handle.base,
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldStaticReadWrite handle, boolean value) {
            UNSAFE.putBooleanOpaque(handle.base,
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldStaticReadWrite handle, boolean value) {
            UNSAFE.putBooleanRelease(handle.base,
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldStaticReadWrite handle, boolean expected, boolean value) {
            return UNSAFE.compareAndSetBoolean(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }


        @ForceInline
        static boolean compareAndExchange(FieldStaticReadWrite handle, boolean expected, boolean value) {
            return UNSAFE.compareAndExchangeBoolean(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean compareAndExchangeAcquire(FieldStaticReadWrite handle, boolean expected, boolean value) {
            return UNSAFE.compareAndExchangeBooleanAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean compareAndExchangeRelease(FieldStaticReadWrite handle, boolean expected, boolean value) {
            return UNSAFE.compareAndExchangeBooleanRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldStaticReadWrite handle, boolean expected, boolean value) {
            return UNSAFE.weakCompareAndSetBooleanPlain(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldStaticReadWrite handle, boolean expected, boolean value) {
            return UNSAFE.weakCompareAndSetBoolean(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldStaticReadWrite handle, boolean expected, boolean value) {
            return UNSAFE.weakCompareAndSetBooleanAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldStaticReadWrite handle, boolean expected, boolean value) {
            return UNSAFE.weakCompareAndSetBooleanRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean getAndSet(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndSetBoolean(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static boolean getAndSetAcquire(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndSetBooleanAcquire(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static boolean getAndSetRelease(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndSetBooleanRelease(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static boolean getAndBitwiseOr(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndBitwiseOrBoolean(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseOrRelease(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndBitwiseOrBooleanRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseOrAcquire(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndBitwiseOrBooleanAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseAnd(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndBitwiseAndBoolean(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseAndRelease(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndBitwiseAndBooleanRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseAndAcquire(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndBitwiseAndBooleanAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseXor(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndBitwiseXorBoolean(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseXorRelease(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndBitwiseXorBooleanRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseXorAcquire(FieldStaticReadWrite handle, boolean value) {
            return UNSAFE.getAndBitwiseXorBooleanAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadWrite.class, null, boolean.class);
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
            return accessMode.at.accessModeType(boolean[].class, boolean.class, int.class);
        }


        @ForceInline
        static boolean get(Array handle, Object oarray, int index) {
            boolean[] array = (boolean[]) oarray;
            return array[index];
        }

        @ForceInline
        static void set(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            array[index] = value;
        }

        @ForceInline
        static boolean getVolatile(Array handle, Object oarray, int index) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getBooleanVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setVolatile(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            UNSAFE.putBooleanVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean getOpaque(Array handle, Object oarray, int index) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getBooleanOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setOpaque(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            UNSAFE.putBooleanOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean getAcquire(Array handle, Object oarray, int index) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getBooleanAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setRelease(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            UNSAFE.putBooleanRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean compareAndSet(Array handle, Object oarray, int index, boolean expected, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.compareAndSetBoolean(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean compareAndExchange(Array handle, Object oarray, int index, boolean expected, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.compareAndExchangeBoolean(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean compareAndExchangeAcquire(Array handle, Object oarray, int index, boolean expected, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.compareAndExchangeBooleanAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean compareAndExchangeRelease(Array handle, Object oarray, int index, boolean expected, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.compareAndExchangeBooleanRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(Array handle, Object oarray, int index, boolean expected, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.weakCompareAndSetBooleanPlain(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSet(Array handle, Object oarray, int index, boolean expected, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.weakCompareAndSetBoolean(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(Array handle, Object oarray, int index, boolean expected, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.weakCompareAndSetBooleanAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(Array handle, Object oarray, int index, boolean expected, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.weakCompareAndSetBooleanRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean getAndSet(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndSetBoolean(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean getAndSetAcquire(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndSetBooleanAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean getAndSetRelease(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndSetBooleanRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean getAndBitwiseOr(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndBitwiseOrBoolean(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseOrRelease(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndBitwiseOrBooleanRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseOrAcquire(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndBitwiseOrBooleanAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseAnd(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndBitwiseAndBoolean(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseAndRelease(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndBitwiseAndBooleanRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseAndAcquire(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndBitwiseAndBooleanAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseXor(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndBitwiseXorBoolean(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseXorRelease(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndBitwiseXorBooleanRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static boolean getAndBitwiseXorAcquire(Array handle, Object oarray, int index, boolean value) {
            boolean[] array = (boolean[]) oarray;
            return UNSAFE.getAndBitwiseXorBooleanAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        static final VarForm FORM = new VarForm(Array.class, boolean[].class, boolean.class, int.class);
    }
}
