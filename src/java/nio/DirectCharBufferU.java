/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

// -- This file was mechanically generated: Do not edit! -- //

package java.nio;

import java.io.FileDescriptor;
import java.lang.ref.Reference;
import jdk.internal.misc.VM;
import jdk.internal.ref.Cleaner;
import sun.nio.ch.DirectBuffer;


class DirectCharBufferU

    extends CharBuffer



    implements DirectBuffer
{



    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(char[].class);

    // Cached unaligned-access capability
    protected static final boolean UNALIGNED = Bits.unaligned();

    // Base address, used in all indexing calculations
    // NOTE: moved up to Buffer.java for speed in JNI GetDirectBufferAddress
    //    protected long address;

    // An object attached to this buffer. If this buffer is a view of another
    // buffer then we use this field to keep a reference to that buffer to
    // ensure that its memory isn't freed before we are done with it.
    private final Object att;

    public Object attachment() {
        return att;
    }




































    public Cleaner cleaner() { return null; }


















































































    // For duplicates and slices
    //
    DirectCharBufferU(DirectBuffer db,         // package-private
                               int mark, int pos, int lim, int cap,
                               int off)
    {

        super(mark, pos, lim, cap);
        address = db.address() + off;



        att = db;




    }

    @Override
    Object base() {
        return null;
    }

    public CharBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        int off = (pos << 1);
        assert (off >= 0);
        return new DirectCharBufferU(this, -1, 0, rem, rem, off);
    }










    public CharBuffer duplicate() {
        return new DirectCharBufferU(this,
                                              this.markValue(),
                                              this.position(),
                                              this.limit(),
                                              this.capacity(),
                                              0);
    }

    public CharBuffer asReadOnlyBuffer() {

        return new DirectCharBufferRU(this,
                                           this.markValue(),
                                           this.position(),
                                           this.limit(),
                                           this.capacity(),
                                           0);



    }



    public long address() {
        return address;
    }

    private long ix(int i) {
        return address + ((long)i << 1);
    }

    public char get() {
        try {
            return ((UNSAFE.getChar(ix(nextGetIndex()))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }

    public char get(int i) {
        try {
            return ((UNSAFE.getChar(ix(checkIndex(i)))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }


    char getUnchecked(int i) {
        try {
            return ((UNSAFE.getChar(ix(i))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }


    public CharBuffer get(char[] dst, int offset, int length) {

        if (((long)length << 1) > Bits.JNI_COPY_TO_ARRAY_THRESHOLD) {
            checkBounds(offset, length, dst.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if (length > rem)
                throw new BufferUnderflowException();

            long dstOffset = ARRAY_BASE_OFFSET + ((long)offset << 1);
            try {

                if (order() != ByteOrder.nativeOrder())
                    UNSAFE.copySwapMemory(null,
                                          ix(pos),
                                          dst,
                                          dstOffset,
                                          (long)length << 1,
                                          (long)1 << 1);
                else

                    UNSAFE.copyMemory(null,
                                      ix(pos),
                                      dst,
                                      dstOffset,
                                      (long)length << 1);
            } finally {
                Reference.reachabilityFence(this);
            }
            position(pos + length);
        } else {
            super.get(dst, offset, length);
        }
        return this;



    }



    public CharBuffer put(char x) {

        try {
            UNSAFE.putChar(ix(nextPutIndex()), ((x)));
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;



    }

    public CharBuffer put(int i, char x) {

        try {
            UNSAFE.putChar(ix(checkIndex(i)), ((x)));
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;



    }

    public CharBuffer put(CharBuffer src) {

        if (src instanceof DirectCharBufferU) {
            if (src == this)
                throw createSameBufferException();
            DirectCharBufferU sb = (DirectCharBufferU)src;

            int spos = sb.position();
            int slim = sb.limit();
            assert (spos <= slim);
            int srem = (spos <= slim ? slim - spos : 0);

            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);

            if (srem > rem)
                throw new BufferOverflowException();
            try {
                UNSAFE.copyMemory(sb.ix(spos), ix(pos), (long)srem << 1);
            } finally {
                Reference.reachabilityFence(sb);
                Reference.reachabilityFence(this);
            }
            sb.position(spos + srem);
            position(pos + srem);
        } else if (src.hb != null) {

            int spos = src.position();
            int slim = src.limit();
            assert (spos <= slim);
            int srem = (spos <= slim ? slim - spos : 0);

            put(src.hb, src.offset + spos, srem);
            src.position(spos + srem);

        } else {
            super.put(src);
        }
        return this;



    }

    public CharBuffer put(char[] src, int offset, int length) {

        if (((long)length << 1) > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD) {
            checkBounds(offset, length, src.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if (length > rem)
                throw new BufferOverflowException();

            long srcOffset = ARRAY_BASE_OFFSET + ((long)offset << 1);
            try {

                if (order() != ByteOrder.nativeOrder())
                    UNSAFE.copySwapMemory(src,
                                          srcOffset,
                                          null,
                                          ix(pos),
                                          (long)length << 1,
                                          (long)1 << 1);
                else

                    UNSAFE.copyMemory(src,
                                      srcOffset,
                                      null,
                                      ix(pos),
                                      (long)length << 1);
            } finally {
                Reference.reachabilityFence(this);
            }
            position(pos + length);
        } else {
            super.put(src, offset, length);
        }
        return this;



    }

    public CharBuffer compact() {

        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        try {
            UNSAFE.copyMemory(ix(pos), ix(0), (long)rem << 1);
        } finally {
            Reference.reachabilityFence(this);
        }
        position(rem);
        limit(capacity());
        discardMark();
        return this;



    }

    public boolean isDirect() {
        return true;
    }

    public boolean isReadOnly() {
        return false;
    }




    public String toString(int start, int end) {
        if ((end > limit()) || (start > end))
            throw new IndexOutOfBoundsException();
        try {
            int len = end - start;
            char[] ca = new char[len];
            CharBuffer cb = CharBuffer.wrap(ca);
            CharBuffer db = this.duplicate();
            db.position(start);
            db.limit(end);
            cb.put(db);
            return new String(ca);
        } catch (StringIndexOutOfBoundsException x) {
            throw new IndexOutOfBoundsException();
        }
    }


    // --- Methods to support CharSequence ---

    public CharBuffer subSequence(int start, int end) {
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        pos = (pos <= lim ? pos : lim);
        int len = lim - pos;

        if ((start < 0) || (end > len) || (start > end))
            throw new IndexOutOfBoundsException();
        return new DirectCharBufferU(this,
                                            -1,
                                            pos + start,
                                            pos + end,
                                            capacity(),
                                            offset);
    }







    public ByteOrder order() {





        return ((ByteOrder.nativeOrder() != ByteOrder.BIG_ENDIAN)
                ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

    }




    ByteOrder charRegionOrder() {
        return order();
    }











}
