/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.Unsafe;


class ByteBufferAsDoubleBufferL                  // package-private
    extends DoubleBuffer
{



    protected final ByteBuffer bb;



    ByteBufferAsDoubleBufferL(ByteBuffer bb) {   // package-private

        super(-1, 0,
              bb.remaining() >> 3,
              bb.remaining() >> 3);
        this.bb = bb;
        // enforce limit == capacity
        int cap = this.capacity();
        this.limit(cap);
        int pos = this.position();
        assert (pos <= cap);
        address = bb.address;



    }

    ByteBufferAsDoubleBufferL(ByteBuffer bb,
                                     int mark, int pos, int lim, int cap,
                                     long addr)
    {

        super(mark, pos, lim, cap);
        this.bb = bb;
        address = addr;
        assert address >= bb.address;



    }

    @Override
    Object base() {
        return bb.hb;
    }

    public DoubleBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        long addr = byteOffset(pos);
        return new ByteBufferAsDoubleBufferL(bb, -1, 0, rem, rem, addr);
    }

    public DoubleBuffer duplicate() {
        return new ByteBufferAsDoubleBufferL(bb,
                                                    this.markValue(),
                                                    this.position(),
                                                    this.limit(),
                                                    this.capacity(),
                                                    address);
    }

    public DoubleBuffer asReadOnlyBuffer() {

        return new ByteBufferAsDoubleBufferRL(bb,
                                                 this.markValue(),
                                                 this.position(),
                                                 this.limit(),
                                                 this.capacity(),
                                                 address);



    }



    private int ix(int i) {
        int off = (int) (address - bb.address);
        return (i << 3) + off;
    }

    protected long byteOffset(long i) {
        return (i << 3) + address;
    }

    public double get() {
        long x = UNSAFE.getLongUnaligned(bb.hb, byteOffset(nextGetIndex()),
            false);
        return Double.longBitsToDouble(x);
    }

    public double get(int i) {
        long x = UNSAFE.getLongUnaligned(bb.hb, byteOffset(checkIndex(i)),
            false);
        return Double.longBitsToDouble(x);
    }











    public DoubleBuffer put(double x) {

        long y = Double.doubleToRawLongBits(x);
        UNSAFE.putLongUnaligned(bb.hb, byteOffset(nextPutIndex()), y,
            false);
        return this;



    }

    public DoubleBuffer put(int i, double x) {

        long y = Double.doubleToRawLongBits(x);
        UNSAFE.putLongUnaligned(bb.hb, byteOffset(checkIndex(i)), y,
            false);
        return this;



    }

    public DoubleBuffer compact() {

        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        ByteBuffer db = bb.duplicate();
        db.limit(ix(lim));
        db.position(ix(0));
        ByteBuffer sb = db.slice();
        sb.position(pos << 3);
        sb.compact();
        position(rem);
        limit(capacity());
        discardMark();
        return this;



    }

    public boolean isDirect() {
        return bb.isDirect();
    }

    public boolean isReadOnly() {
        return false;
    }











































    public ByteOrder order() {




        return ByteOrder.LITTLE_ENDIAN;

    }






}
