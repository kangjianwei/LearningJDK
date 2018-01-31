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

final class VarHandleFloats {

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
            return accessMode.at.accessModeType(receiverType, float.class);
        }

        @ForceInline
        static float get(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getFloat(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static float getVolatile(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getFloatVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static float getOpaque(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getFloatOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static float getAcquire(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getFloatAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadOnly.class, Object.class, float.class);
    }

    static final class FieldInstanceReadWrite extends FieldInstanceReadOnly {

        FieldInstanceReadWrite(Class<?> receiverType, long fieldOffset) {
            super(receiverType, fieldOffset, FieldInstanceReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldInstanceReadWrite handle, Object holder, float value) {
            UNSAFE.putFloat(Objects.requireNonNull(handle.receiverType.cast(holder)),
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldInstanceReadWrite handle, Object holder, float value) {
            UNSAFE.putFloatVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldInstanceReadWrite handle, Object holder, float value) {
            UNSAFE.putFloatOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldInstanceReadWrite handle, Object holder, float value) {
            UNSAFE.putFloatRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldInstanceReadWrite handle, Object holder, float expected, float value) {
            return UNSAFE.compareAndSetFloat(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static float compareAndExchange(FieldInstanceReadWrite handle, Object holder, float expected, float value) {
            return UNSAFE.compareAndExchangeFloat(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static float compareAndExchangeAcquire(FieldInstanceReadWrite handle, Object holder, float expected, float value) {
            return UNSAFE.compareAndExchangeFloatAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static float compareAndExchangeRelease(FieldInstanceReadWrite handle, Object holder, float expected, float value) {
            return UNSAFE.compareAndExchangeFloatRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldInstanceReadWrite handle, Object holder, float expected, float value) {
            return UNSAFE.weakCompareAndSetFloatPlain(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldInstanceReadWrite handle, Object holder, float expected, float value) {
            return UNSAFE.weakCompareAndSetFloat(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldInstanceReadWrite handle, Object holder, float expected, float value) {
            return UNSAFE.weakCompareAndSetFloatAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldInstanceReadWrite handle, Object holder, float expected, float value) {
            return UNSAFE.weakCompareAndSetFloatRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static float getAndSet(FieldInstanceReadWrite handle, Object holder, float value) {
            return UNSAFE.getAndSetFloat(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static float getAndSetAcquire(FieldInstanceReadWrite handle, Object holder, float value) {
            return UNSAFE.getAndSetFloatAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static float getAndSetRelease(FieldInstanceReadWrite handle, Object holder, float value) {
            return UNSAFE.getAndSetFloatRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static float getAndAdd(FieldInstanceReadWrite handle, Object holder, float value) {
            return UNSAFE.getAndAddFloat(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static float getAndAddAcquire(FieldInstanceReadWrite handle, Object holder, float value) {
            return UNSAFE.getAndAddFloatAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static float getAndAddRelease(FieldInstanceReadWrite handle, Object holder, float value) {
            return UNSAFE.getAndAddFloatRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }


        static final VarForm FORM = new VarForm(FieldInstanceReadWrite.class, Object.class, float.class);
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
            return accessMode.at.accessModeType(null, float.class);
        }

        @ForceInline
        static float get(FieldStaticReadOnly handle) {
            return UNSAFE.getFloat(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static float getVolatile(FieldStaticReadOnly handle) {
            return UNSAFE.getFloatVolatile(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static float getOpaque(FieldStaticReadOnly handle) {
            return UNSAFE.getFloatOpaque(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static float getAcquire(FieldStaticReadOnly handle) {
            return UNSAFE.getFloatAcquire(handle.base,
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadOnly.class, null, float.class);
    }

    static final class FieldStaticReadWrite extends FieldStaticReadOnly {

        FieldStaticReadWrite(Object base, long fieldOffset) {
            super(base, fieldOffset, FieldStaticReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldStaticReadWrite handle, float value) {
            UNSAFE.putFloat(handle.base,
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldStaticReadWrite handle, float value) {
            UNSAFE.putFloatVolatile(handle.base,
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldStaticReadWrite handle, float value) {
            UNSAFE.putFloatOpaque(handle.base,
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldStaticReadWrite handle, float value) {
            UNSAFE.putFloatRelease(handle.base,
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldStaticReadWrite handle, float expected, float value) {
            return UNSAFE.compareAndSetFloat(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }


        @ForceInline
        static float compareAndExchange(FieldStaticReadWrite handle, float expected, float value) {
            return UNSAFE.compareAndExchangeFloat(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static float compareAndExchangeAcquire(FieldStaticReadWrite handle, float expected, float value) {
            return UNSAFE.compareAndExchangeFloatAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static float compareAndExchangeRelease(FieldStaticReadWrite handle, float expected, float value) {
            return UNSAFE.compareAndExchangeFloatRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldStaticReadWrite handle, float expected, float value) {
            return UNSAFE.weakCompareAndSetFloatPlain(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldStaticReadWrite handle, float expected, float value) {
            return UNSAFE.weakCompareAndSetFloat(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldStaticReadWrite handle, float expected, float value) {
            return UNSAFE.weakCompareAndSetFloatAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldStaticReadWrite handle, float expected, float value) {
            return UNSAFE.weakCompareAndSetFloatRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static float getAndSet(FieldStaticReadWrite handle, float value) {
            return UNSAFE.getAndSetFloat(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static float getAndSetAcquire(FieldStaticReadWrite handle, float value) {
            return UNSAFE.getAndSetFloatAcquire(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static float getAndSetRelease(FieldStaticReadWrite handle, float value) {
            return UNSAFE.getAndSetFloatRelease(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static float getAndAdd(FieldStaticReadWrite handle, float value) {
            return UNSAFE.getAndAddFloat(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static float getAndAddAcquire(FieldStaticReadWrite handle, float value) {
            return UNSAFE.getAndAddFloatAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static float getAndAddRelease(FieldStaticReadWrite handle, float value) {
            return UNSAFE.getAndAddFloatRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadWrite.class, null, float.class);
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
            return accessMode.at.accessModeType(float[].class, float.class, int.class);
        }


        @ForceInline
        static float get(Array handle, Object oarray, int index) {
            float[] array = (float[]) oarray;
            return array[index];
        }

        @ForceInline
        static void set(Array handle, Object oarray, int index, float value) {
            float[] array = (float[]) oarray;
            array[index] = value;
        }

        @ForceInline
        static float getVolatile(Array handle, Object oarray, int index) {
            float[] array = (float[]) oarray;
            return UNSAFE.getFloatVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setVolatile(Array handle, Object oarray, int index, float value) {
            float[] array = (float[]) oarray;
            UNSAFE.putFloatVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static float getOpaque(Array handle, Object oarray, int index) {
            float[] array = (float[]) oarray;
            return UNSAFE.getFloatOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setOpaque(Array handle, Object oarray, int index, float value) {
            float[] array = (float[]) oarray;
            UNSAFE.putFloatOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static float getAcquire(Array handle, Object oarray, int index) {
            float[] array = (float[]) oarray;
            return UNSAFE.getFloatAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setRelease(Array handle, Object oarray, int index, float value) {
            float[] array = (float[]) oarray;
            UNSAFE.putFloatRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean compareAndSet(Array handle, Object oarray, int index, float expected, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.compareAndSetFloat(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static float compareAndExchange(Array handle, Object oarray, int index, float expected, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.compareAndExchangeFloat(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static float compareAndExchangeAcquire(Array handle, Object oarray, int index, float expected, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.compareAndExchangeFloatAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static float compareAndExchangeRelease(Array handle, Object oarray, int index, float expected, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.compareAndExchangeFloatRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(Array handle, Object oarray, int index, float expected, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.weakCompareAndSetFloatPlain(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSet(Array handle, Object oarray, int index, float expected, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.weakCompareAndSetFloat(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(Array handle, Object oarray, int index, float expected, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.weakCompareAndSetFloatAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(Array handle, Object oarray, int index, float expected, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.weakCompareAndSetFloatRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static float getAndSet(Array handle, Object oarray, int index, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.getAndSetFloat(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static float getAndSetAcquire(Array handle, Object oarray, int index, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.getAndSetFloatAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static float getAndSetRelease(Array handle, Object oarray, int index, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.getAndSetFloatRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static float getAndAdd(Array handle, Object oarray, int index, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.getAndAddFloat(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static float getAndAddAcquire(Array handle, Object oarray, int index, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.getAndAddFloatAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static float getAndAddRelease(Array handle, Object oarray, int index, float value) {
            float[] array = (float[]) oarray;
            return UNSAFE.getAndAddFloatRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        static final VarForm FORM = new VarForm(Array.class, float[].class, float.class, int.class);
    }
}
