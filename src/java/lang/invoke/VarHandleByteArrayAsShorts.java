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

final class VarHandleByteArrayAsShorts extends VarHandleByteArrayBase {

    static final int ALIGN = Short.BYTES - 1;

    @ForceInline
    static short convEndian(boolean big, short n) {
        return big == BE ? n : Short.reverseBytes(n);
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
            return accessMode.at.accessModeType(byte[].class, short.class, int.class);
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
        static short get(ArrayHandle handle, Object oba, int index) {
            byte[] ba = (byte[]) oba;
            return UNSAFE.getShortUnaligned(
                    ba,
                    ((long) index(ba, index)) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                    handle.be);
        }

        @ForceInline
        static void set(ArrayHandle handle, Object oba, int index, short value) {
            byte[] ba = (byte[]) oba;
            UNSAFE.putShortUnaligned(
                    ba,
                    ((long) index(ba, index)) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                    value,
                    handle.be);
        }

        @ForceInline
        static short getVolatile(ArrayHandle handle, Object oba, int index) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.getShortVolatile(
                                      ba,
                                      address(ba, index(ba, index))));
        }

        @ForceInline
        static void setVolatile(ArrayHandle handle, Object oba, int index, short value) {
            byte[] ba = (byte[]) oba;
            UNSAFE.putShortVolatile(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, value));
        }

        @ForceInline
        static short getAcquire(ArrayHandle handle, Object oba, int index) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.getShortAcquire(
                                      ba,
                                      address(ba, index(ba, index))));
        }

        @ForceInline
        static void setRelease(ArrayHandle handle, Object oba, int index, short value) {
            byte[] ba = (byte[]) oba;
            UNSAFE.putShortRelease(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, value));
        }

        @ForceInline
        static short getOpaque(ArrayHandle handle, Object oba, int index) {
            byte[] ba = (byte[]) oba;
            return convEndian(handle.be,
                              UNSAFE.getShortOpaque(
                                      ba,
                                      address(ba, index(ba, index))));
        }

        @ForceInline
        static void setOpaque(ArrayHandle handle, Object oba, int index, short value) {
            byte[] ba = (byte[]) oba;
            UNSAFE.putShortOpaque(
                    ba,
                    address(ba, index(ba, index)),
                    convEndian(handle.be, value));
        }

        static final VarForm FORM = new VarForm(ArrayHandle.class, byte[].class, short.class, int.class);
    }


    static final class ByteBufferHandle extends ByteArrayViewVarHandle {

        ByteBufferHandle(boolean be) {
            super(ByteBufferHandle.FORM, be);
        }

        @Override
        final MethodType accessModeTypeUncached(AccessMode accessMode) {
            return accessMode.at.accessModeType(ByteBuffer.class, short.class, int.class);
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
        static short get(ByteBufferHandle handle, Object obb, int index) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return UNSAFE.getShortUnaligned(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    ((long) index(bb, index)) + UNSAFE.getLong(bb, BUFFER_ADDRESS),
                    handle.be);
        }

        @ForceInline
        static void set(ByteBufferHandle handle, Object obb, int index, short value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            UNSAFE.putShortUnaligned(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    ((long) indexRO(bb, index)) + UNSAFE.getLong(bb, BUFFER_ADDRESS),
                    value,
                    handle.be);
        }

        @ForceInline
        static short getVolatile(ByteBufferHandle handle, Object obb, int index) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.getShortVolatile(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, index(bb, index))));
        }

        @ForceInline
        static void setVolatile(ByteBufferHandle handle, Object obb, int index, short value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            UNSAFE.putShortVolatile(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, value));
        }

        @ForceInline
        static short getAcquire(ByteBufferHandle handle, Object obb, int index) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.getShortAcquire(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, index(bb, index))));
        }

        @ForceInline
        static void setRelease(ByteBufferHandle handle, Object obb, int index, short value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            UNSAFE.putShortRelease(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, value));
        }

        @ForceInline
        static short getOpaque(ByteBufferHandle handle, Object obb, int index) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            return convEndian(handle.be,
                              UNSAFE.getShortOpaque(
                                      UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                                      address(bb, index(bb, index))));
        }

        @ForceInline
        static void setOpaque(ByteBufferHandle handle, Object obb, int index, short value) {
            ByteBuffer bb = (ByteBuffer) Objects.requireNonNull(obb);
            UNSAFE.putShortOpaque(
                    UNSAFE.getObject(bb, BYTE_BUFFER_HB),
                    address(bb, indexRO(bb, index)),
                    convEndian(handle.be, value));
        }

        static final VarForm FORM = new VarForm(ByteBufferHandle.class, ByteBuffer.class, short.class, int.class);
    }
}
