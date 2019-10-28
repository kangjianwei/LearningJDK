/*
 * Copyright (c) 1996, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * ObjectInput extends the DataInput interface to include the reading of
 * objects. DataInput includes methods for the input of primitive types,
 * ObjectInput extends that interface to include objects, arrays, and Strings.
 *
 * @author unascribed
 * @see java.io.InputStream
 * @see java.io.ObjectOutputStream
 * @see java.io.ObjectInputStream
 * @since 1.1
 */
// 对象输入流，用于反序列化
public interface ObjectInput extends DataInput, AutoCloseable {
    
    /**
     * Read and return an object. The class that implements this interface
     * defines where the object is "read" from.
     *
     * @return the object read from the stream
     *
     * @throws java.lang.ClassNotFoundException If the class of a serialized
     *                                          object cannot be found.
     * @throws IOException                      If any of the usual Input/Output
     *                                          related exceptions occur.
     */
    // 从输入流反序列化对象，并将其返回
    Object readObject() throws ClassNotFoundException, IOException;
    
    /**
     * Reads a byte of data. This method will block if no input is
     * available.
     *
     * @return the byte read, or -1 if the end of the
     * stream is reached.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 从输入流读取一个字节并返回；如果返回-1，表示读取失败
    int read() throws IOException;
    
    /**
     * Reads into an array of bytes.  This method will
     * block until some input is available.
     *
     * @param b the buffer into which the data is read
     *
     * @return the actual number of bytes read, -1 is
     * returned when the end of the stream is reached.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 从输入流读取足量的字节填充字节数组b，返回实际填充的字节数
    int read(byte[] b) throws IOException;
    
    /**
     * Reads into an array of bytes.  This method will
     * block until some input is available.
     *
     * @param b   the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     *
     * @return the actual number of bytes read, -1 is
     * returned when the end of the stream is reached.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 从输入流读取len字节填充到字节数组b的off处，返回实际填充的字节数
    int read(byte[] b, int off, int len) throws IOException;
    
    /**
     * Skips n bytes of input.
     *
     * @param n the number of bytes to be skipped
     *
     * @return the actual number of bytes skipped.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 跳过n个字节
    long skip(long n) throws IOException;
    
    /**
     * Returns the number of bytes that can be read
     * without blocking.
     *
     * @return the number of available bytes.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 返回剩余可读字节数
    int available() throws IOException;
    
    /**
     * Closes the input stream. Must be called
     * to release any resources associated with
     * the stream.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 关闭输入流
    void close() throws IOException;
    
}
