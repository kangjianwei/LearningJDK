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


class ByteBufferAsCharBufferRL                  // package-private
    extends ByteBufferAsCharBufferL
{







    ByteBufferAsCharBufferRL(ByteBuffer bb) {   // package-private












        super(bb);

    }

    ByteBufferAsCharBufferRL(ByteBuffer bb,
                                     int mark, int pos, int lim, int cap,
                                     long addr)
    {






        super(bb, mark, pos, lim, cap, addr);

    }

    @Override
    Object base() {
        return bb.hb;
    }

    public CharBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        long addr = byteOffset(pos);
        return new ByteBufferAsCharBufferRL(bb, -1, 0, rem, rem, addr);
    }

    public CharBuffer duplicate() {
        return new ByteBufferAsCharBufferRL(bb,
                                                    this.markValue(),
                                                    this.position(),
                                                    this.limit(),
                                                    this.capacity(),
                                                    address);
    }

    public CharBuffer asReadOnlyBuffer() {








        return duplicate();

    }


































    public CharBuffer put(char x) {






        throw new ReadOnlyBufferException();

    }

    public CharBuffer put(int i, char x) {






        throw new ReadOnlyBufferException();

    }

    public CharBuffer compact() {

















        throw new ReadOnlyBufferException();

    }

    public boolean isDirect() {
        return bb.isDirect();
    }

    public boolean isReadOnly() {
        return true;
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
        return new ByteBufferAsCharBufferRL(bb,
                                                  -1,
                                                  pos + start,
                                                  pos + end,
                                                  capacity(),
                                                  address);
    }




    public ByteOrder order() {




        return ByteOrder.LITTLE_ENDIAN;

    }


    ByteOrder charRegionOrder() {
        return order();
    }

}
