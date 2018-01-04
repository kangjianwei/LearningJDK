/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.Unsafe;
import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.ForceInline;

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Objects;

import static java.lang.invoke.MethodHandleStatics.UNSAFE;

// -- This file was mechanically generated: Do not edit! -- //

final class VarHandleByteArrayAsLongs extends VarHandleByteArrayBase {

    static final int ALIGN = Long.BYTES - 1;

    @ForceInline
    static long convEndian(boolean big, long n) {
        return big == BE ? n : Long.reverseBytes(n);
    }


    private static abstract class ByteArrayViewVarHandle extends VarHandle {
        final boolean be;

        ByteArrayViewVarHandle(VarForm form, boolean be) {
            super(form);
            this.be = be;
        }
    }

    static final class ArrayHandle extends ByteArrayViewVarHandle {

        ArrayHandle(boolean be) {
            super(ArrayHandle.FORM, be);
        }

        @Override
        final MethodType accessModeTypeUncached(AccessMode accessMode) {
            return accessMode.at.accessModeType(byte[].class, long.class, int.class);
        }

        @ForceInline
        static int index(byte[] ba, int index) {
            return Preconditions.checkIndex(index, ba.length - ALIGN, null);
        }

        @ForceInline
        static long address(byte[] ba, int index) {
            long address = ((long) index) + Unsafe.ARRAY_BYTE_BASE_OFFSET;
            if ((address & ALIGN) != 0)
                throw newIllegalStateExceptionForMisalignedAccess(index);
            return address;
        }

        @ForceInline
        static long get(ArrayHandle handle, Object oba, int index) {
            byte[] ba = (byte[]) oba;
            return UNSAFE.getLongUnaligned(
                    ba,
                    ((long) index(ba, index)) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                    handle.be);
        }

        @ForceInline
        static void set(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            UNSAFE.putLongUnaligned(
                    ba,
                    ((long) index(ba, index)) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                    value,
                    handle.be);
        }

        @ForceInline
        static long getVolatile(ArrayHandle handle, Object oba, int index) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.getLongVolatile(
                                      ba,
                                      address(ba, index(ba, index))));
        }

        @ForceInline
        static void setVolatile(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            UNSAFE.putLongVolatile(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, value));
        }

        @ForceInline
        static long getAcquire(ArrayHandle handle, Object oba, int index) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.getLongAcquire(
                                      ba,
                                      address(ba, index(ba, index))));
        }

        @ForceInline
        static void setRelease(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            UNSAFE.putLongRelease(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, value));
        }

        @ForceInline
        static long getOpaque(ArrayHandle handle, Object oba, int index) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.getLongOpaque(
                                      ba,
                                      address(ba, index(ba, index))));
        }

        @ForceInline
        static void setOpaque(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            UNSAFE.putLongOpaque(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, value));
        }

        @ForceInline
        static boolean compareAndSet(ArrayHandle handle, Object oba, int index, long expected, long value) {
            byte[] ba = (byte[]) oba;
            return UNSAFE.compareAndSetLong(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, expected), convEndian(handle.be, value));
        }

        @ForceInline
        static long compareAndExchange(ArrayHandle handle, Object oba, int index, long expected, long value) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.compareAndExchangeLong(
                                      ba,
                                      address(ba, index(ba, index)),
                                      convEndian(handle.be, expected), convEndian(handle.be, value)));
        }

        @ForceInline
        static long compareAndExchangeAcquire(ArrayHandle handle, Object oba, int index, long expected, long value) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.compareAndExchangeLongAcquire(
                                      ba,
                                      address(ba, index(ba, index)),
                                      convEndian(handle.be, expected), convEndian(handle.be, value)));
        }

        @ForceInline
        static long compareAndExchangeRelease(ArrayHandle handle, Object oba, int index, long expected, long value) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.compareAndExchangeLongRelease(
                                      ba,
                                      address(ba, index(ba, index)),
                                      convEndian(handle.be, expected), convEndian(handle.be, value)));
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(ArrayHandle handle, Object oba, int index, long expected, long value) {
            byte[] ba = (byte[]) oba;
            return UNSAFE.weakCompareAndSetLongPlain(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, expected), convEndian(handle.be, value));
        }

        @ForceInline
        static boolean weakCompareAndSet(ArrayHandle handle, Object oba, int index, long expected, long value) {
            byte[] ba = (byte[]) oba;
            return UNSAFE.weakCompareAndSetLong(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, expected), convEndian(handle.be, value));
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(ArrayHandle handle, Object oba, int index, long expected, long value) {
            byte[] ba = (byte[]) oba;
            return UNSAFE.weakCompareAndSetLongAcquire(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, expected), convEndian(handle.be, value));
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(ArrayHandle handle, Object oba, int index, long expected, long value) {
            byte[] ba = (byte[]) oba;
            return UNSAFE.weakCompareAndSetLongRelease(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, expected), convEndian(handle.be, value));
        }

        @ForceInline
        static long getAndSet(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.getAndSetLong(
                                      ba,
                                      address(ba, index(ba, index)),
                                      convEndian(handle.be, value)));
        }

        @ForceInline
        static long getAndSetAcquire(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.getAndSetLongAcquire(
                                      ba,
                                      address(ba, index(ba, index)),
                                      convEndian(handle.be, value)));
        }

        @ForceInline
        static long getAndSetRelease(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.getAndSetLongRelease(
                                      ba,
                                      address(ba, index(ba, index)),
                                      convEndian(handle.be, value)));
        }

        @ForceInline
        static long getAndAdd(ArrayHandle handle, Object oba, int index, long delta) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndAddLong(
                        ba,
                        address(ba, index(ba, index)),
                        delta);
            } else {
                return getAndAddConvEndianWithCAS(ba, index, delta);
            }
        }

        @ForceInline
        static long getAndAddAcquire(ArrayHandle handle, Object oba, int index, long delta) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndAddLongAcquire(
                        ba,
                        address(ba, index(ba, index)),
                        delta);
            } else {
                return getAndAddConvEndianWithCAS(ba, index, delta);
            }
        }

        @ForceInline
        static long getAndAddRelease(ArrayHandle handle, Object oba, int index, long delta) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndAddLongRelease(
                        ba,
                        address(ba, index(ba, index)),
                        delta);
            } else {
                return getAndAddConvEndianWithCAS(ba, index, delta);
            }
        }

        @ForceInline
        static long getAndAddConvEndianWithCAS(byte[] ba, int index, long delta) {
            long nativeExpectedValue, expectedValue;
            long offset = address(ba, index(ba, index));
            do {
                nativeExpectedValue = UNSAFE.getLongVolatile(ba, offset);
                expectedValue = Long.reverseBytes(nativeExpectedValue);
            } while (!UNSAFE.weakCompareAndSetLong(ba, offset,
                    nativeExpectedValue, Long.reverseBytes(expectedValue + delta)));
            return expectedValue;
        }

        @ForceInline
        static long getAndBitwiseOr(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseOrLong(
                        ba,
                        address(ba, index(ba, index)),
                        value);
            } else {
                return getAndBitwiseOrConvEndianWithCAS(ba, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseOrRelease(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseOrLongRelease(
                        ba,
                        address(ba, index(ba, index)),
                        value);
            } else {
                return getAndBitwiseOrConvEndianWithCAS(ba, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseOrAcquire(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseOrLongAcquire(
                        ba,
                        address(ba, index(ba, index)),
                        value);
            } else {
                return getAndBitwiseOrConvEndianWithCAS(ba, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseOrConvEndianWithCAS(byte[] ba, int index, long value) {
            long nativeExpectedValue, expectedValue;
            long offset = address(ba, index(ba, index));
            do {
                nativeExpectedValue = UNSAFE.getLongVolatile(ba, offset);
                expectedValue = Long.reverseBytes(nativeExpectedValue);
            } while (!UNSAFE.weakCompareAndSetLong(ba, offset,
                    nativeExpectedValue, Long.reverseBytes(expectedValue | value)));
            return expectedValue;
        }

        @ForceInline
        static long getAndBitwiseAnd(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseAndLong(
                        ba,
                        address(ba, index(ba, index)),
                        value);
            } else {
                return getAndBitwiseAndConvEndianWithCAS(ba, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseAndRelease(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseAndLongRelease(
                        ba,
                        address(ba, index(ba, index)),
                        value);
            } else {
                return getAndBitwiseAndConvEndianWithCAS(ba, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseAndAcquire(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseAndLongAcquire(
                        ba,
                        address(ba, index(ba, index)),
                        value);
            } else {
                return getAndBitwiseAndConvEndianWithCAS(ba, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseAndConvEndianWithCAS(byte[] ba, int index, long value) {
            long nativeExpectedValue, expectedValue;
            long offset = address(ba, index(ba, index));
            do {
                nativeExpectedValue = UNSAFE.getLongVolatile(ba, offset);
                expectedValue = Long.reverseBytes(nativeExpectedValue);
            } while (!UNSAFE.weakCompareAndSetLong(ba, offset,
                    nativeExpectedValue, Long.reverseBytes(expectedValue & value)));
            return expectedValue;
        }

        @ForceInline
        static long getAndBitwiseXor(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseXorLong(
                        ba,
                        address(ba, index(ba, index)),
                        value);
            } else {
                return getAndBitwiseXorConvEndianWithCAS(ba, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseXorRelease(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseXorLongRelease(
                        ba,
                        address(ba, index(ba, index)),
                        value);
            } else {
                return getAndBitwiseXorConvEndianWithCAS(ba, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseXorAcquire(ArrayHandle handle, Object oba, int index, long value) {
            byte[] ba = (byte[]) oba;
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseXorLongAcquire(
                        ba,
                        address(ba, index(ba, index)),
                        value);
            } else {
                return getAndBitwiseXorConvEndianWithCAS(ba, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseXorConvEndianWithCAS(byte[] ba, int index, long value) {
            long nativeExpectedValue, expectedValue;
            long offset = address(ba, index(ba, index));
            do {
                nativeExpectedValue = UNSAFE.getLongVolatile(ba, offset);
                expectedValue = Long.reverseBytes(nativeExpectedValue);
            } while (!UNSAFE.weakCompareAndSetLong(ba, offset,
                    nativeExpectedValue, Long.reverseBytes(expectedValue ^ value)));
            return expectedValue;
        }

        static final VarForm FORM = new VarForm(ArrayHandle.class, byte[].class, long.class, int.class);
    }


    static final class ByteBufferHandle extends ByteArrayViewVarHandle {

        ByteBufferHandle(boolean be) {
            super(ByteBufferHandle.FORM, be);
        }

        @Override
        final MethodType accessModeTypeUncached(AccessMode accessMode) {
            return accessMode.at.accessModeType(ByteBuffer.class, long.class, int.class);
        }

        @ForceInline
        static int index(ByteBuffer bb, int index) {
            return Preconditions.checkIndex(index, UNSAFE.getInt(bb, BUFFER_LIMIT) - ALIGN, null);
        }

        @ForceInline
        static int indexRO(ByteBuffer bb, int index) {
            if (UNSAFE.getBoolean(bb, BYTE_BUFFER_IS_READ_ONLY))
                throw new ReadOnlyBufferException();
            return Preconditions.checkIndex(index, UNSAFE.getInt(bb, BUFFER_LIMIT) - ALIGN, null);
        }

        @ForceInline
        static long address(ByteBuffer bb, int index) {
            long address = ((long) index) + UNSAFE.getLong(bb, BUFFER_ADDRESS);
            if ((address & ALIGN) != 0)
                throw newIllegalStateExceptionForMisalignedAccess(index);
            return address;
        }

        @ForceInline
        static long get(ByteBufferHandle handle, Object obb, int index) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return UNSAFE.getLongUnaligned(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    ((long) index(bb, index)) + UNSAFE.getLong(bb, BUFFER_ADDRESS),
                    handle.be);
        }

        @ForceInline
        static void set(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            UNSAFE.putLongUnaligned(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    ((long) indexRO(bb, index)) + UNSAFE.getLong(bb, BUFFER_ADDRESS),
                    value,
                    handle.be);
        }

        @ForceInline
        static long getVolatile(ByteBufferHandle handle, Object obb, int index) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.getLongVolatile(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, index(bb, index))));
        }

        @ForceInline
        static void setVolatile(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            UNSAFE.putLongVolatile(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, value));
        }

        @ForceInline
        static long getAcquire(ByteBufferHandle handle, Object obb, int index) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.getLongAcquire(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, index(bb, index))));
        }

        @ForceInline
        static void setRelease(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            UNSAFE.putLongRelease(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, value));
        }

        @ForceInline
        static long getOpaque(ByteBufferHandle handle, Object obb, int index) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.getLongOpaque(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, index(bb, index))));
        }

        @ForceInline
        static void setOpaque(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            UNSAFE.putLongOpaque(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, value));
        }

        @ForceInline
        static boolean compareAndSet(ByteBufferHandle handle, Object obb, int index, long expected, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return UNSAFE.compareAndSetLong(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, expected), convEndian(handle.be, value));
        }

        @ForceInline
        static long compareAndExchange(ByteBufferHandle handle, Object obb, int index, long expected, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.compareAndExchangeLong(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, indexRO(bb, index)),
                                      convEndian(handle.be, expected), convEndian(handle.be, value)));
        }

        @ForceInline
        static long compareAndExchangeAcquire(ByteBufferHandle handle, Object obb, int index, long expected, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.compareAndExchangeLongAcquire(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, indexRO(bb, index)),
                                      convEndian(handle.be, expected), convEndian(handle.be, value)));
        }

        @ForceInline
        static long compareAndExchangeRelease(ByteBufferHandle handle, Object obb, int index, long expected, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.compareAndExchangeLongRelease(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, indexRO(bb, index)),
                                      convEndian(handle.be, expected), convEndian(handle.be, value)));
        }

        @ForceInline
        static boolean weakCompareAndSetPlain(ByteBufferHandle handle, Object obb, int index, long expected, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return UNSAFE.weakCompareAndSetLongPlain(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, expected), convEndian(handle.be, value));
        }

        @ForceInline
        static boolean weakCompareAndSet(ByteBufferHandle handle, Object obb, int index, long expected, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return UNSAFE.weakCompareAndSetLong(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, expected), convEndian(handle.be, value));
        }

        @ForceInline
        static boolean weakCompareAndSetAcquire(ByteBufferHandle handle, Object obb, int index, long expected, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return UNSAFE.weakCompareAndSetLongAcquire(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, expected), convEndian(handle.be, value));
        }

        @ForceInline
        static boolean weakCompareAndSetRelease(ByteBufferHandle handle, Object obb, int index, long expected, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return UNSAFE.weakCompareAndSetLongRelease(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, expected), convEndian(handle.be, value));
        }

        @ForceInline
        static long getAndSet(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.getAndSetLong(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, indexRO(bb, index)),
                                      convEndian(handle.be, value)));
        }

        @ForceInline
        static long getAndSetAcquire(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.getAndSetLongAcquire(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, indexRO(bb, index)),
                                      convEndian(handle.be, value)));
        }

        @ForceInline
        static long getAndSetRelease(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.getAndSetLongRelease(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, indexRO(bb, index)),
                                      convEndian(handle.be, value)));
        }

        @ForceInline
        static long getAndAdd(ByteBufferHandle handle, Object obb, int index, long delta) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndAddLong(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        delta);
            } else {
                return getAndAddConvEndianWithCAS(bb, index, delta);
            }
        }

        @ForceInline
        static long getAndAddAcquire(ByteBufferHandle handle, Object obb, int index, long delta) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndAddLongAcquire(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        delta);
            } else {
                return getAndAddConvEndianWithCAS(bb, index, delta);
            }
        }

        @ForceInline
        static long getAndAddRelease(ByteBufferHandle handle, Object obb, int index, long delta) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndAddLongRelease(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        delta);
            } else {
                return getAndAddConvEndianWithCAS(bb, index, delta);
            }
        }

        @ForceInline
        static long getAndAddConvEndianWithCAS(ByteBuffer bb, int index, long delta) {
            long nativeExpectedValue, expectedValue;
            Object base = UNSAFE.getObject(bb, BYTE_BUFFER_HB);
            long offset = address(bb, indexRO(bb, index));
            do {
                nativeExpectedValue = UNSAFE.getLongVolatile(base, offset);
                expectedValue = Long.reverseBytes(nativeExpectedValue);
            } while (!UNSAFE.weakCompareAndSetLong(base, offset,
                    nativeExpectedValue, Long.reverseBytes(expectedValue + delta)));
            return expectedValue;
        }

        @ForceInline
        static long getAndBitwiseOr(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseOrLong(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        value);
            } else {
                return getAndBitwiseOrConvEndianWithCAS(bb, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseOrRelease(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseOrLongRelease(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        value);
            } else {
                return getAndBitwiseOrConvEndianWithCAS(bb, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseOrAcquire(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseOrLongAcquire(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        value);
            } else {
                return getAndBitwiseOrConvEndianWithCAS(bb, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseOrConvEndianWithCAS(ByteBuffer bb, int index, long value) {
            long nativeExpectedValue, expectedValue;
            Object base = UNSAFE.getObject(bb, BYTE_BUFFER_HB);
            long offset = address(bb, indexRO(bb, index));
            do {
                nativeExpectedValue = UNSAFE.getLongVolatile(base, offset);
                expectedValue = Long.reverseBytes(nativeExpectedValue);
            } while (!UNSAFE.weakCompareAndSetLong(base, offset,
                    nativeExpectedValue, Long.reverseBytes(expectedValue | value)));
            return expectedValue;
        }

        @ForceInline
        static long getAndBitwiseAnd(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseAndLong(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        value);
            } else {
                return getAndBitwiseAndConvEndianWithCAS(bb, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseAndRelease(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseAndLongRelease(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        value);
            } else {
                return getAndBitwiseAndConvEndianWithCAS(bb, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseAndAcquire(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseAndLongAcquire(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        value);
            } else {
                return getAndBitwiseAndConvEndianWithCAS(bb, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseAndConvEndianWithCAS(ByteBuffer bb, int index, long value) {
            long nativeExpectedValue, expectedValue;
            Object base = UNSAFE.getObject(bb, BYTE_BUFFER_HB);
            long offset = address(bb, indexRO(bb, index));
            do {
                nativeExpectedValue = UNSAFE.getLongVolatile(base, offset);
                expectedValue = Long.reverseBytes(nativeExpectedValue);
            } while (!UNSAFE.weakCompareAndSetLong(base, offset,
                    nativeExpectedValue, Long.reverseBytes(expectedValue & value)));
            return expectedValue;
        }


        @ForceInline
        static long getAndBitwiseXor(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseXorLong(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        value);
            } else {
                return getAndBitwiseXorConvEndianWithCAS(bb, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseXorRelease(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseXorLongRelease(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        value);
            } else {
                return getAndBitwiseXorConvEndianWithCAS(bb, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseXorAcquire(ByteBufferHandle handle, Object obb, int index, long value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            if (handle.be == BE) {
                return UNSAFE.getAndBitwiseXorLongAcquire(
                        UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                        address(bb, indexRO(bb, index)),
                        value);
            } else {
                return getAndBitwiseXorConvEndianWithCAS(bb, index, value);
            }
        }

        @ForceInline
        static long getAndBitwiseXorConvEndianWithCAS(ByteBuffer bb, int index, long value) {
            long nativeExpectedValue, expectedValue;
            Object base = UNSAFE.getObject(bb, BYTE_BUFFER_HB);
            long offset = address(bb, indexRO(bb, index));
            do {
                nativeExpectedValue = UNSAFE.getLongVolatile(base, offset);
                expectedValue = Long.reverseBytes(nativeExpectedValue);
            } while (!UNSAFE.weakCompareAndSetLong(base, offset,
                    nativeExpectedValue, Long.reverseBytes(expectedValue ^ value)));
            return expectedValue;
        }

        static final VarForm FORM = new VarForm(ByteBufferHandle.class, ByteBuffer.class, long.class, int.class);
    }
}
