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

final class VarHandleChars {

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
            return accessMode.at.accessModeType(receiverType, char.class);
        }

        @ForceInline
        static char get(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getChar(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static char getVolatile(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getCharVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static char getOpaque(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getCharOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        @ForceInline
        static char getAcquire(FieldInstanceReadOnly handle, Object holder) {
            return UNSAFE.getCharAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadOnly.class, Object.class, char.class);
    }

    static final class FieldInstanceReadWrite extends FieldInstanceReadOnly {

        FieldInstanceReadWrite(Class<?> receiverType, long fieldOffset) {
            super(receiverType, fieldOffset, FieldInstanceReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldInstanceReadWrite handle, Object holder, char value) {
            UNSAFE.putChar(Objects.requireNonNull(handle.receiverType.cast(holder)),
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldInstanceReadWrite handle, Object holder, char value) {
            UNSAFE.putCharVolatile(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldInstanceReadWrite handle, Object holder, char value) {
            UNSAFE.putCharOpaque(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldInstanceReadWrite handle, Object holder, char value) {
            UNSAFE.putCharRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldInstanceReadWrite handle, Object holder, char expected, char value) {
            return UNSAFE.compareAndSetChar(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static char compareAndExchange(FieldInstanceReadWrite handle, Object holder, char expected, char value) {
            return UNSAFE.compareAndExchangeChar(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static char compareAndExchangeAcquire(FieldInstanceReadWrite handle, Object holder, char expected, char value) {
            return UNSAFE.compareAndExchangeCharAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static char compareAndExchangeRelease(FieldInstanceReadWrite handle, Object holder, char expected, char value) {
            return UNSAFE.compareAndExchangeCharRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldInstanceReadWrite handle, Object holder, char expected, char value) {
            return UNSAFE.weakCompareAndSetCharPlain(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldInstanceReadWrite handle, Object holder, char expected, char value) {
            return UNSAFE.weakCompareAndSetChar(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldInstanceReadWrite handle, Object holder, char expected, char value) {
            return UNSAFE.weakCompareAndSetCharAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldInstanceReadWrite handle, Object holder, char expected, char value) {
            return UNSAFE.weakCompareAndSetCharRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static char getAndSet(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndSetChar(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static char getAndSetAcquire(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndSetCharAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static char getAndSetRelease(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndSetCharRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static char getAndAdd(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndAddChar(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndAddAcquire(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndAddCharAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndAddRelease(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndAddCharRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }


        @ForceInline
        static char getAndBitwiseOr(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndBitwiseOrChar(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseOrRelease(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndBitwiseOrCharRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseOrAcquire(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndBitwiseOrCharAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseAnd(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndBitwiseAndChar(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseAndRelease(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndBitwiseAndCharRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseAndAcquire(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndBitwiseAndCharAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseXor(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndBitwiseXorChar(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseXorRelease(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndBitwiseXorCharRelease(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseXorAcquire(FieldInstanceReadWrite handle, Object holder, char value) {
            return UNSAFE.getAndBitwiseXorCharAcquire(Objects.requireNonNull(handle.receiverType.cast(holder)),
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldInstanceReadWrite.class, Object.class, char.class);
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
            return accessMode.at.accessModeType(null, char.class);
        }

        @ForceInline
        static char get(FieldStaticReadOnly handle) {
            return UNSAFE.getChar(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static char getVolatile(FieldStaticReadOnly handle) {
            return UNSAFE.getCharVolatile(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static char getOpaque(FieldStaticReadOnly handle) {
            return UNSAFE.getCharOpaque(handle.base,
                                 handle.fieldOffset);
        }

        @ForceInline
        static char getAcquire(FieldStaticReadOnly handle) {
            return UNSAFE.getCharAcquire(handle.base,
                                 handle.fieldOffset);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadOnly.class, null, char.class);
    }

    static final class FieldStaticReadWrite extends FieldStaticReadOnly {

        FieldStaticReadWrite(Object base, long fieldOffset) {
            super(base, fieldOffset, FieldStaticReadWrite.FORM);
        }

        @ForceInline
        static void set(FieldStaticReadWrite handle, char value) {
            UNSAFE.putChar(handle.base,
                             handle.fieldOffset,
                             value);
        }

        @ForceInline
        static void setVolatile(FieldStaticReadWrite handle, char value) {
            UNSAFE.putCharVolatile(handle.base,
                                     handle.fieldOffset,
                                     value);
        }

        @ForceInline
        static void setOpaque(FieldStaticReadWrite handle, char value) {
            UNSAFE.putCharOpaque(handle.base,
                                   handle.fieldOffset,
                                   value);
        }

        @ForceInline
        static void setRelease(FieldStaticReadWrite handle, char value) {
            UNSAFE.putCharRelease(handle.base,
                                    handle.fieldOffset,
                                    value);
        }

        @ForceInline
        static boolean compareAndSet(FieldStaticReadWrite handle, char expected, char value) {
            return UNSAFE.compareAndSetChar(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }


        @ForceInline
        static char compareAndExchange(FieldStaticReadWrite handle, char expected, char value) {
            return UNSAFE.compareAndExchangeChar(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static char compareAndExchangeAcquire(FieldStaticReadWrite handle, char expected, char value) {
            return UNSAFE.compareAndExchangeCharAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static char compareAndExchangeRelease(FieldStaticReadWrite handle, char expected, char value) {
            return UNSAFE.compareAndExchangeCharRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(FieldStaticReadWrite handle, char expected, char value) {
            return UNSAFE.weakCompareAndSetCharPlain(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSet(FieldStaticReadWrite handle, char expected, char value) {
            return UNSAFE.weakCompareAndSetChar(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(FieldStaticReadWrite handle, char expected, char value) {
            return UNSAFE.weakCompareAndSetCharAcquire(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(FieldStaticReadWrite handle, char expected, char value) {
            return UNSAFE.weakCompareAndSetCharRelease(handle.base,
                                               handle.fieldOffset,
                                               expected,
                                               value);
        }

        @ForceInline
        static char getAndSet(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndSetChar(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static char getAndSetAcquire(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndSetCharAcquire(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static char getAndSetRelease(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndSetCharRelease(handle.base,
                                          handle.fieldOffset,
                                          value);
        }

        @ForceInline
        static char getAndAdd(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndAddChar(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndAddAcquire(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndAddCharAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndAddRelease(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndAddCharRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseOr(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndBitwiseOrChar(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseOrRelease(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndBitwiseOrCharRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseOrAcquire(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndBitwiseOrCharAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseAnd(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndBitwiseAndChar(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseAndRelease(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndBitwiseAndCharRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseAndAcquire(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndBitwiseAndCharAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseXor(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndBitwiseXorChar(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseXorRelease(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndBitwiseXorCharRelease(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseXorAcquire(FieldStaticReadWrite handle, char value) {
            return UNSAFE.getAndBitwiseXorCharAcquire(handle.base,
                                       handle.fieldOffset,
                                       value);
        }

        static final VarForm FORM = new VarForm(FieldStaticReadWrite.class, null, char.class);
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
            return accessMode.at.accessModeType(char[].class, char.class, int.class);
        }


        @ForceInline
        static char get(Array handle, Object oarray, int index) {
            char[] array = (char[]) oarray;
            return array[index];
        }

        @ForceInline
        static void set(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            array[index] = value;
        }

        @ForceInline
        static char getVolatile(Array handle, Object oarray, int index) {
            char[] array = (char[]) oarray;
            return UNSAFE.getCharVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setVolatile(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            UNSAFE.putCharVolatile(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static char getOpaque(Array handle, Object oarray, int index) {
            char[] array = (char[]) oarray;
            return UNSAFE.getCharOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setOpaque(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            UNSAFE.putCharOpaque(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static char getAcquire(Array handle, Object oarray, int index) {
            char[] array = (char[]) oarray;
            return UNSAFE.getCharAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase);
        }

        @ForceInline
        static void setRelease(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            UNSAFE.putCharRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static boolean compareAndSet(Array handle, Object oarray, int index, char expected, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.compareAndSetChar(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static char compareAndExchange(Array handle, Object oarray, int index, char expected, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.compareAndExchangeChar(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static char compareAndExchangeAcquire(Array handle, Object oarray, int index, char expected, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.compareAndExchangeCharAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static char compareAndExchangeRelease(Array handle, Object oarray, int index, char expected, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.compareAndExchangeCharRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(Array handle, Object oarray, int index, char expected, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.weakCompareAndSetCharPlain(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSet(Array handle, Object oarray, int index, char expected, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.weakCompareAndSetChar(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(Array handle, Object oarray, int index, char expected, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.weakCompareAndSetCharAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(Array handle, Object oarray, int index, char expected, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.weakCompareAndSetCharRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    expected,
                    value);
        }

        @ForceInline
        static char getAndSet(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndSetChar(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static char getAndSetAcquire(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndSetCharAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static char getAndSetRelease(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndSetCharRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static char getAndAdd(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndAddChar(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static char getAndAddAcquire(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndAddCharAcquire(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static char getAndAddRelease(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndAddCharRelease(array,
                    (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                    value);
        }

        @ForceInline
        static char getAndBitwiseOr(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndBitwiseOrChar(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseOrRelease(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndBitwiseOrCharRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseOrAcquire(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndBitwiseOrCharAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseAnd(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndBitwiseAndChar(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseAndRelease(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndBitwiseAndCharRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseAndAcquire(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndBitwiseAndCharAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseXor(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndBitwiseXorChar(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseXorRelease(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndBitwiseXorCharRelease(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        @ForceInline
        static char getAndBitwiseXorAcquire(Array handle, Object oarray, int index, char value) {
            char[] array = (char[]) oarray;
            return UNSAFE.getAndBitwiseXorCharAcquire(array,
                                       (((long) Preconditions.checkIndex(index, array.length, AIOOBE_SUPPLIER)) << handle.ashift) + handle.abase,
                                       value);
        }

        static final VarForm FORM = new VarForm(Array.class, char[].class, char.class, int.class);
    }
}
