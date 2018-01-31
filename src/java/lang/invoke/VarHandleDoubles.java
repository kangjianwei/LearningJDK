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

final class VarHandleDoubles {

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
            return accessMode.at.accessModeType(receiverType, double.class);
        }

        @ForceInline
        static double get(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getDouble(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static double getVolatile(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getDoubleVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static double getOpaque(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getDoubleOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static double getAcquire(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getDoubleAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadOnly.class, Object.class, double.class);
    }

    static final class FieldInstanceReadWrite extends FieldInstanceReadOnly {

        FieldInstanceReadWrite(Class<?> receiverType, long fieldOffset) {
            super(receiverType, fieldOffset, FieldInstanceReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldInstanceReadWrite handle, Object holder, double value) {
            UNSAFE.putDouble(Objects.requireNonNull(handle.receiverType.cast(holder)),
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldInstanceReadWrite handle, Object holder, double value) {
            UNSAFE.putDoubleVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldInstanceReadWrite handle, Object holder, double value) {
            UNSAFE.putDoubleOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldInstanceReadWrite handle, Object holder, double value) {
            UNSAFE.putDoubleRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldInstanceReadWrite handle, Object holder, double expected, double value) {
            return UNSAFE.compareAndSetDouble(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static double compareAndExchange(FieldInstanceReadWrite handle, Object holder, double expected, double value) {
            return UNSAFE.compareAndExchangeDouble(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static double compareAndExchangeAcquire(FieldInstanceReadWrite handle, Object holder, double expected, double value) {
            return UNSAFE.compareAndExchangeDoubleAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static double compareAndExchangeRelease(FieldInstanceReadWrite handle, Object holder, double expected, double value) {
            return UNSAFE.compareAndExchangeDoubleRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldInstanceReadWrite handle, Object holder, double expected, double value) {
            return UNSAFE.weakCompareAndSetDoublePlain(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldInstanceReadWrite handle, Object holder, double expected, double value) {
            return UNSAFE.weakCompareAndSetDouble(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldInstanceReadWrite handle, Object holder, double expected, double value) {
            return UNSAFE.weakCompareAndSetDoubleAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldInstanceReadWrite handle, Object holder, double expected, double value) {
            return UNSAFE.weakCompareAndSetDoubleRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static double getAndSet(FieldInstanceReadWrite handle, Object holder, double value) {
            return UNSAFE.getAndSetDouble(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static double getAndSetAcquire(FieldInstanceReadWrite handle, Object holder, double value) {
            return UNSAFE.getAndSetDoubleAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static double getAndSetRelease(FieldInstanceReadWrite handle, Object holder, double value) {
            return UNSAFE.getAndSetDoubleRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static double getAndAdd(FieldInstanceReadWrite handle, Object holder, double value) {
            return UNSAFE.getAndAddDouble(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static double getAndAddAcquire(FieldInstanceReadWrite handle, Object holder, double value) {
            return UNSAFE.getAndAddDoubleAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static double getAndAddRelease(FieldInstanceReadWrite handle, Object holder, double value) {
            return UNSAFE.getAndAddDoubleRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }


        static final VarForm FORM = new VarForm(FieldInstanceReadWrite.class, Object.class, double.class);
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
            return accessMode.at.accessModeType(null, double.class);
        }

        @ForceInline
        static double get(FieldStaticReadOnly handle) {
            return UNSAFE.getDouble(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static double getVolatile(FieldStaticReadOnly handle) {
            return UNSAFE.getDoubleVolatile(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static double getOpaque(FieldStaticReadOnly handle) {
            return UNSAFE.getDoubleOpaque(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static double getAcquire(FieldStaticReadOnly handle) {
            return UNSAFE.getDoubleAcquire(handle.base,
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadOnly.class, null, double.class);
    }

    static final class FieldStaticReadWrite extends FieldStaticReadOnly {

        FieldStaticReadWrite(Object base, long fieldOffset) {
            super(base, fieldOffset, FieldStaticReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldStaticReadWrite handle, double value) {
            UNSAFE.putDouble(handle.base,
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldStaticReadWrite handle, double value) {
            UNSAFE.putDoubleVolatile(handle.base,
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldStaticReadWrite handle, double value) {
            UNSAFE.putDoubleOpaque(handle.base,
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldStaticReadWrite handle, double value) {
            UNSAFE.putDoubleRelease(handle.base,
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldStaticReadWrite handle, double expected, double value) {
            return UNSAFE.compareAndSetDouble(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }


        @ForceInline
        static double compareAndExchange(FieldStaticReadWrite handle, double expected, double value) {
            return UNSAFE.compareAndExchangeDouble(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static double compareAndExchangeAcquire(FieldStaticReadWrite handle, double expected, double value) {
            return UNSAFE.compareAndExchangeDoubleAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static double compareAndExchangeRelease(FieldStaticReadWrite handle, double expected, double value) {
            return UNSAFE.compareAndExchangeDoubleRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldStaticReadWrite handle, double expected, double value) {
            return UNSAFE.weakCompareAndSetDoublePlain(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldStaticReadWrite handle, double expected, double value) {
            return UNSAFE.weakCompareAndSetDouble(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldStaticReadWrite handle, double expected, double value) {
            return UNSAFE.weakCompareAndSetDoubleAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldStaticReadWrite handle, double expected, double value) {
            return UNSAFE.weakCompareAndSetDoubleRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static double getAndSet(FieldStaticReadWrite handle, double value) {
            return UNSAFE.getAndSetDouble(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static double getAndSetAcquire(FieldStaticReadWrite handle, double value) {
            return UNSAFE.getAndSetDoubleAcquire(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static double getAndSetRelease(FieldStaticReadWrite handle, double value) {
            return UNSAFE.getAndSetDoubleRelease(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static double getAndAdd(FieldStaticReadWrite handle, double value) {
            return UNSAFE.getAndAddDouble(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static double getAndAddAcquire(FieldStaticReadWrite handle, double value) {
            return UNSAFE.getAndAddDoubleAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static double getAndAddRelease(FieldStaticReadWrite handle, double value) {
            return UNSAFE.getAndAddDoubleRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadWrite.class, null, double.class);
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
            return accessMode.at.accessModeType(double[].class, double.class, int.class);
        }


        @ForceInline
        static double get(Array handle, Object oarray, int index) {
            double[] array = (double[]) oarray;
            return array[index];
        }

        @ForceInline
        static void set(Array handle, Object oarray, int index, double value) {
            double[] array = (double[]) oarray;
            array[index] = value;
        }

        @ForceInline
        static double getVolatile(Array handle, Object oarray, int index) {
            double[] array = (double[]) oarray;
            return UNSAFE.getDoubleVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setVolatile(Array handle, Object oarray, int index, double value) {
            double[] array = (double[]) oarray;
            UNSAFE.putDoubleVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static double getOpaque(Array handle, Object oarray, int index) {
            double[] array = (double[]) oarray;
            return UNSAFE.getDoubleOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setOpaque(Array handle, Object oarray, int index, double value) {
            double[] array = (double[]) oarray;
            UNSAFE.putDoubleOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static double getAcquire(Array handle, Object oarray, int index) {
            double[] array = (double[]) oarray;
            return UNSAFE.getDoubleAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setRelease(Array handle, Object oarray, int index, double value) {
            double[] array = (double[]) oarray;
            UNSAFE.putDoubleRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean compareAndSet(Array handle, Object oarray, int index, double expected, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.compareAndSetDouble(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static double compareAndExchange(Array handle, Object oarray, int index, double expected, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.compareAndExchangeDouble(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static double compareAndExchangeAcquire(Array handle, Object oarray, int index, double expected, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.compareAndExchangeDoubleAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static double compareAndExchangeRelease(Array handle, Object oarray, int index, double expected, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.compareAndExchangeDoubleRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(Array handle, Object oarray, int index, double expected, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.weakCompareAndSetDoublePlain(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSet(Array handle, Object oarray, int index, double expected, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.weakCompareAndSetDouble(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(Array handle, Object oarray, int index, double expected, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.weakCompareAndSetDoubleAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(Array handle, Object oarray, int index, double expected, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.weakCompareAndSetDoubleRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static double getAndSet(Array handle, Object oarray, int index, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.getAndSetDouble(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static double getAndSetAcquire(Array handle, Object oarray, int index, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.getAndSetDoubleAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static double getAndSetRelease(Array handle, Object oarray, int index, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.getAndSetDoubleRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static double getAndAdd(Array handle, Object oarray, int index, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.getAndAddDouble(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static double getAndAddAcquire(Array handle, Object oarray, int index, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.getAndAddDoubleAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static double getAndAddRelease(Array handle, Object oarray, int index, double value) {
            double[] array = (double[]) oarray;
            return UNSAFE.getAndAddDoubleRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        static final VarForm FORM = new VarForm(Array.class, double[].class, double.class, int.class);
    }
}
