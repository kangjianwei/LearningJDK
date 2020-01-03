/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

/**
 * Utility methods for packing/unpacking primitive values in/out of byte arrays
 * using big-endian byte ordering.
 */
// 在指定的字节数组上读取/写入基本类型数据的工具类
class Bits {
    
    /*
     * Methods for unpacking primitive values from byte arrays starting at given offsets.
     */
    
    // 从字节数组b的off处读取boolean值
    static boolean getBoolean(byte[] b, int off) {
        return b[off] != 0;
    }
    
    // 从字节数组b的off处读取char值
    static char getChar(byte[] b, int off) {
        return (char) (((b[off + 1] & 0xFF) << 0)
            + ((b[off + 0] & 0xFF) << 8));
    }
    
    // 从字节数组b的off处读取short值
    static short getShort(byte[] b, int off) {
        return (short) (((b[off + 1] & 0xFF) << 0)
            + ((b[off + 0] & 0xFF) << 8));
    }
    
    // 从字节数组b的off处读取int值
    static int getInt(byte[] b, int off) {
        return ((b[off + 3] & 0xFF) << 0)
            + ((b[off + 2] & 0xFF) << 8)
            + ((b[off + 1] & 0xFF) << 16)
            + ((b[off + 0] & 0xFF) << 24);
    }
    
    // 从字节数组b的off处读取long值
    static long getLong(byte[] b, int off) {
        return ((b[off + 7] & 0xFFL) << 0)
            + ((b[off + 6] & 0xFFL) << 8)
            + ((b[off + 5] & 0xFFL) << 16)
            + ((b[off + 4] & 0xFFL) << 24)
            + ((b[off + 3] & 0xFFL) << 32)
            + ((b[off + 2] & 0xFFL) << 40)
            + ((b[off + 1] & 0xFFL) << 48)
            + (((long) b[off]) << 56);
    }
    
    // 从字节数组b的off处读取float值
    static float getFloat(byte[] b, int off) {
        return Float.intBitsToFloat(getInt(b, off));
    }
    
    // 从字节数组b的off处读取double值
    static double getDouble(byte[] b, int off) {
        return Double.longBitsToDouble(getLong(b, off));
    }
    
    
    /*
     * Methods for packing primitive values into byte arrays starting at given offsets.
     */
    
    // 向字节数组b的off处写入boolean值val
    static void putBoolean(byte[] b, int off, boolean val) {
        b[off] = (byte) (val ? 1 : 0);
    }
    
    // 向字节数组b的off处写入char值val（大端法）
    static void putChar(byte[] b, int off, char val) {
        b[off + 1] = (byte) (val);
        b[off]     = (byte) (val >>> 8);
    }
    
    // 向字节数组b的off处写入short值val（大端法）
    static void putShort(byte[] b, int off, short val) {
        b[off + 1] = (byte) (val);
        b[off]     = (byte) (val >>> 8);
    }
    
    // 向字节数组b的off处写入int值val（大端法）
    static void putInt(byte[] b, int off, int val) {
        b[off + 3] = (byte) (val);
        b[off + 2] = (byte) (val >>> 8);
        b[off + 1] = (byte) (val >>> 16);
        b[off]     = (byte) (val >>> 24);
    }
    
    // 向字节数组b的off处写入long值val（大端法）
    static void putLong(byte[] b, int off, long val) {
        b[off + 7] = (byte) (val);
        b[off + 6] = (byte) (val >>> 8);
        b[off + 5] = (byte) (val >>> 16);
        b[off + 4] = (byte) (val >>> 24);
        b[off + 3] = (byte) (val >>> 32);
        b[off + 2] = (byte) (val >>> 40);
        b[off + 1] = (byte) (val >>> 48);
        b[off]     = (byte) (val >>> 56);
    }
    
    // 向字节数组b的off处写入float值val（大端法）
    static void putFloat(byte[] b, int off, float val) {
        putInt(b, off, Float.floatToIntBits(val));
    }
    
    // 向字节数组b的off处写入double值val（大端法）
    static void putDouble(byte[] b, int off, double val) {
        putLong(b, off, Double.doubleToLongBits(val));
    }
    
}
