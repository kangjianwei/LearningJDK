/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * A buffered character-input stream that keeps track of line numbers.  This
 * class defines methods {@link #setLineNumber(int)} and {@link
 * #getLineNumber()} for setting and getting the current line number
 * respectively.
 *
 * <p> By default, line numbering begins at 0. This number increments at every
 * <a href="#lt">line terminator</a> as the data is read, and can be changed
 * with a call to {@code setLineNumber(int)}.  Note however, that
 * {@code setLineNumber(int)} does not actually change the current position in
 * the stream; it only changes the value that will be returned by
 * {@code getLineNumber()}.
 *
 * <p> A line is considered to be <a id="lt">terminated</a> by any one of a
 * line feed ('\n'), a carriage return ('\r'), or a carriage return followed
 * immediately by a linefeed.
 *
 * @author Mark Reinhold
 * @since 1.1
 */
// 支持按行读取的字符输入流，可以追踪行号
public class LineNumberReader extends BufferedReader {
    
    /** Maximum skip-buffer size */
    private static final int maxSkipBufferSize = 8192;
    
    /** The current line number */
    private int lineNumber = 0;     // 当前行号
    
    /** The line number of the mark, if any */
    private int markedLineNumber; // Defaults to 0
    
    /** If the next character is a line feed, skip it */
    private boolean skipLF;
    
    /** The skipLF flag when the mark was set */
    private boolean markedSkipLF;
    
    /** Skip buffer, null until allocated */
    private char[] skipBuffer = null;   // 临时存储跳过的字符
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Create a new line-numbering reader, using the default input-buffer
     * size.
     *
     * @param in A Reader object to provide the underlying stream
     */
    public LineNumberReader(Reader in) {
        super(in);
    }
    
    /**
     * Create a new line-numbering reader, reading characters into a buffer of
     * the given size.
     *
     * @param in A Reader object to provide the underlying stream
     * @param sz An int specifying the size of the buffer
     */
    public LineNumberReader(Reader in, int sz) {
        super(in, sz);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Read a single character.  <a href="#lt">Line terminators</a> are
     * compressed into single newline ('\n') characters.  Whenever a line
     * terminator is read the current line number is incremented.
     *
     * @return The character read, or -1 if the end of the stream has been
     * reached
     *
     * @throws IOException If an I/O error occurs
     */
    // 返回从字符输入流中读取的一个char，返回-1表示读取失败
    @SuppressWarnings("fallthrough")
    public int read() throws IOException {
        synchronized(lock) {
            int c = super.read();
            
            if(skipLF) {
                if(c == '\n') {
                    c = super.read();
                }
                skipLF = false;
            }
            
            switch(c) {
                case '\r':
                    skipLF = true;
                case '\n':          /* Fall through */
                    lineNumber++;
            }
            
            return c;
        }
    }
    
    /**
     * Read characters into a portion of an array.  Whenever a <a
     * href="#lt">line terminator</a> is read the current line number is
     * incremented.
     *
     * @param cbuf Destination buffer
     * @param off  Offset at which to start storing characters
     * @param len  Maximum number of characters to read
     *
     * @return The number of bytes read, or -1 if the end of the stream has
     * already been reached
     *
     * @throws IOException               If an I/O error occurs
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 从输入源读取len个字符存入cbuf的off处，返回实际读到的字符数
    @SuppressWarnings("fallthrough")
    public int read(char[] cbuf, int off, int len) throws IOException {
        synchronized(lock) {
            int n = super.read(cbuf, off, len);
            
            for(int i = off; i<off + n; i++) {
                int c = cbuf[i];
                
                if(skipLF) {
                    skipLF = false;
                    if(c == '\n') {
                        continue;
                    }
                }
                
                switch(c) {
                    case '\r':
                        skipLF = true;
                    case '\n':  /* Fall through */
                        lineNumber++;   // 行号计数
                }
            }
            
            return n;
        }
    }
    
    
    /**
     * Read a line of text.  Whenever a <a href="#lt">line terminator</a> is
     * read the current line number is incremented.
     *
     * @return A String containing the contents of the line, not including
     * any <a href="#lt">line termination characters</a>, or
     * {@code null} if the end of the stream has been reached
     *
     * @throws IOException If an I/O error occurs
     */
    // 读取一行的内容，每行的终止标记为'\r'或'\n'或'\r\n'或EOF
    public String readLine() throws IOException {
        synchronized(lock) {
            String l = super.readLine(skipLF);
            
            skipLF = false;
            
            if(l != null) {
                lineNumber++;
            }
            
            return l;
        }
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存档 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Mark the present position in the stream.  Subsequent calls to reset()
     * will attempt to reposition the stream to this point, and will also reset
     * the line number appropriately.
     *
     * @param readAheadLimit Limit on the number of characters that may be read while still
     *                       preserving the mark.  After reading this many characters,
     *                       attempting to reset the stream may fail.
     *
     * @throws IOException If an I/O error occurs
     */
    // 设置存档标记，readAheadLimit是存档区预读上限
    public void mark(int readAheadLimit) throws IOException {
        synchronized(lock) {
            super.mark(readAheadLimit);
            markedLineNumber = lineNumber;
            markedSkipLF = skipLF;
        }
    }
    
    /**
     * Reset the stream to the most recent mark.
     *
     * @throws IOException If the stream has not been marked, or if the mark has been
     *                     invalidated
     */
    // 对于支持设置存档的输入流，可以重置其"读游标"到存档区的起始位置
    public void reset() throws IOException {
        synchronized(lock) {
            super.reset();
            lineNumber = markedLineNumber;
            skipLF = markedSkipLF;
        }
    }
    
    /*▲ 存档 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Skip characters.
     *
     * @param n The number of characters to skip
     *
     * @return The number of characters actually skipped
     *
     * @throws IOException              If an I/O error occurs
     * @throws IllegalArgumentException If {@code n} is negative
     */
    // 读取中跳过n个字符，返回实际跳过的字符数
    public long skip(long n) throws IOException {
        if(n<0) {
            throw new IllegalArgumentException("skip() value is negative");
        }
        
        int nn = (int) Math.min(n, maxSkipBufferSize);
        
        synchronized(lock) {
            if((skipBuffer == null) || (skipBuffer.length<nn)) {
                skipBuffer = new char[nn];
            }
            
            long r = n;
            while(r>0) {
                int nc = read(skipBuffer, 0, (int) Math.min(r, nn));
                if(nc == -1) {
                    break;
                }
                
                r -= nc;
            }
            
            return n - r;
        }
    }
    
    
    /**
     * Get the current line number.
     *
     * @return The current line number
     *
     * @see #setLineNumber
     */
    // 返回当前行号
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Set the current line number.
     *
     * @param lineNumber An int specifying the line number
     *
     * @see #getLineNumber
     */
    // 修改当前行号(不会修改读游标的位置)
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
