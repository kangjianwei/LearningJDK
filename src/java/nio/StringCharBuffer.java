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

package java.nio;

/* If the sequence is a string, use reflection to share its array */

// 只读、非直接缓冲区，内部存储结构实现为CharSequence的子类（包括CharBuffer和String）
class StringCharBuffer extends CharBuffer {
    
    CharSequence str;   // 缓冲区
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    StringCharBuffer(CharSequence s, int start, int end) { // package-private
        super(-1, start, end, s.length());
        int n = s.length();
        if((start < 0) || (start > n) || (end < start) || (end > n))
            throw new IndexOutOfBoundsException();
        str = s;
        this.isReadOnly = true;
    }
    
    private StringCharBuffer(CharSequence s, int mark, int pos, int limit, int cap, int offset) {
        super(mark, pos, limit, cap, null, offset);
        str = s;
        this.isReadOnly = true;
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 只读/非直接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // StringCharBuffer是只读缓冲区，返回true
    public final boolean isReadOnly() {
        return true;
    }
    
    // StringCharBuffer是非直接缓冲区，返回false
    public boolean isDirect() {
        return false;
    }
    
    /*▲ 只读/非直接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    public CharBuffer slice() {
        return new StringCharBuffer(str, -1, 0, this.remaining(), this.remaining(), offset + this.position());
    }
    
    // 副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public CharBuffer duplicate() {
        return new StringCharBuffer(str, markValue(), position(), limit(), capacity(), offset);
    }
    
    // 只读副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public CharBuffer asReadOnlyBuffer() {
        return duplicate();
    }
    
    // 副本，新缓冲区的【活跃区域】取自旧缓冲区【活跃区域】的[start，end)部分
    public final CharBuffer subSequence(int start, int end) {
        try {
            int pos = position();
            return new StringCharBuffer(str, -1, pos + checkIndex(start, pos), pos + checkIndex(end, pos), capacity(), offset);
        } catch(IllegalArgumentException x) {
            throw new IndexOutOfBoundsException();
        }
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 读取position+offset处的char，然后递增position
    public final char get() {
        return str.charAt(nextGetIndex() + offset);
    }
    
    // 读取index+offset处的char（有越界检查）
    public final char get(int index) {
        return str.charAt(checkIndex(index) + offset);
    }
    
    // 读取index+offset处的char（无越界检查）
    char getUnchecked(int index) {
        return str.charAt(index + offset);
    }
    
    /*▲ get ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 只读缓冲区，禁止写入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    public final CharBuffer put(char c) {
        throw new ReadOnlyBufferException();
    }
    
    public final CharBuffer put(int index, char c) {
        throw new ReadOnlyBufferException();
    }
    
    /*▲ 只读缓冲区，禁止写入 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 禁止压缩，因为禁止写入，压缩没意义
    public final CharBuffer compact() {
        throw new ReadOnlyBufferException();
    }
    
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回该缓冲区的字节序（本类的字节序与机器字平台相同）
    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }
    
    // 返回‘char’的字节序（此类中返回null）
    ByteOrder charRegionOrder() {
        return null;
    }
    /*▲ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 比较 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 比较两个缓冲区是否相同
    public boolean equals(Object ob) {
        if(this == ob)
            return true;
        if(!(ob instanceof CharBuffer))
            return false;
        CharBuffer that = (CharBuffer) ob;
        if(this.remaining() != that.remaining())
            return false;
        return BufferMismatch.mismatch(this, this.position(), that, that.position(), this.remaining()) < 0;
    }
    
    public int compareTo(CharBuffer that) {
        int i = BufferMismatch.mismatch(this, this.position(), that, that.position(), Math.min(this.remaining(), that.remaining()));
        if(i >= 0) {
            return Character.compare(this.get(this.position() + i), that.get(that.position() + i));
        }
        return this.remaining() - that.remaining();
    }
    
    /*▲ 比较 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 构造新的子串
    final String toString(int start, int end) {
        return str.subSequence(start + offset, end + offset).toString();
    }
}
