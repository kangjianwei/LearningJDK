/*
 * Copyright (c) 1996, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * Abstract class for reading filtered character streams.
 * The abstract class <code>FilterReader</code> itself
 * provides default methods that pass all requests to
 * the contained stream. Subclasses of <code>FilterReader</code>
 * should override some of these methods and may also provide
 * additional methods and fields.
 *
 * @author Mark Reinhold
 * @since 1.1
 */
// 对输入流的简单包装，由子类实现具体的包装行为
public abstract class FilterReader extends Reader {
    
    /**
     * The underlying character-input stream.
     */
    protected Reader in;    // 包装的输入流
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new filtered reader.
     *
     * @param in a Reader object providing the underlying stream.
     *
     * @throws NullPointerException if <code>in</code> is <code>null</code>
     */
    protected FilterReader(Reader in) {
        super(in);
        this.in = in;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads a single character.
     *
     * @throws IOException If an I/O error occurs
     */
    public int read() throws IOException {
        return in.read();
    }
    
    /**
     * Reads characters into a portion of an array.
     *
     * @throws IOException               If an I/O error occurs
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public int read(char[] cbuf, int off, int len) throws IOException {
        return in.read(cbuf, off, len);
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存档 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether this stream supports the mark() operation.
     */
    // 判断当前输入流是否支持存档标记
    public boolean markSupported() {
        return in.markSupported();
    }
    
    /**
     * Marks the present position in the stream.
     *
     * @throws IOException If an I/O error occurs
     */
    // 设置存档标记，readAheadLimit是存档区预读上限
    public void mark(int readAheadLimit) throws IOException {
        in.mark(readAheadLimit);
    }
    
    /**
     * Resets the stream.
     *
     * @throws IOException If an I/O error occurs
     */
    // 对于支持设置存档的输入流，可以重置其"读游标"到存档区的起始位置
    public void reset() throws IOException {
        in.reset();
    }
    
    /*▲ 存档 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭输入流
    public void close() throws IOException {
        in.close();
    }
    
    /**
     * Skips characters.
     *
     * @throws IOException If an I/O error occurs
     */
    // 读取中跳过n个字符，返回实际跳过的字符数
    public long skip(long n) throws IOException {
        return in.skip(n);
    }
    
    /**
     * Tells whether this stream is ready to be read.
     *
     * @throws IOException If an I/O error occurs
     */
    // 判断当前流是否已准备好被读取
    public boolean ready() throws IOException {
        return in.ready();
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
