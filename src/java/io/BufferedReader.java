/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Reads text from a character-input stream, buffering characters so as to
 * provide for the efficient reading of characters, arrays, and lines.
 *
 * <p> The buffer size may be specified, or the default size may be used.  The
 * default is large enough for most purposes.
 *
 * <p> In general, each read request made of a Reader causes a corresponding
 * read request to be made of the underlying character or byte stream.  It is
 * therefore advisable to wrap a BufferedReader around any Reader whose read()
 * operations may be costly, such as FileReaders and InputStreamReaders.  For
 * example,
 *
 * <pre>
 * BufferedReader in
 *   = new BufferedReader(new FileReader("foo.in"));
 * </pre>
 *
 * will buffer the input from the specified file.  Without buffering, each
 * invocation of read() or readLine() could cause bytes to be read from the
 * file, converted into characters, and then returned, which can be very
 * inefficient.
 *
 * <p> Programs that use DataInputStreams for textual input can be localized by
 * replacing each DataInputStream with an appropriate BufferedReader.
 *
 * @author Mark Reinhold
 * @see FileReader
 * @see InputStreamReader
 * @see java.nio.file.Files#newBufferedReader
 * @since 1.1
 */
/*
 * 带有内部缓存区的字符输入流
 *
 * 读取数据时，会先从包装的输入流中读取数据，然后暂存在内部缓冲区中，
 * 最后对外开放缓冲区，避免了频繁读取输入流造成的低效问题。
 */
public class BufferedReader extends Reader {
    
    private static final int UNMARKED = -1;
    private static final int INVALIDATED = -2;
    
    private static int defaultCharBufferSize = 8192;
    
    private static int defaultExpectedLineLength = 80;
    
    private Reader in;  // 包装的字符输入流
    
    private char[] cb;      // 内部缓冲区
    private int nChars;     // 内部缓冲区中现有字符数
    private int nextChar;   // 内部缓冲区中下一个该读的字符
    
    private int markedChar = UNMARKED;  // 存档标记
    
    /** Valid only when markedChar > 0 */
    private int readAheadLimit = 0;     // 存档区预读上限
    
    /** If the next character is a line feed, skip it */
    private boolean skipLF = false; // 上一个字符是否为'\r'，如果是的话，需要跳过下一个紧邻的'\n'
    
    /** The skipLF flag when the mark was set */
    private boolean markedSkipLF = false;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a buffering character-input stream that uses a default-sized
     * input buffer.
     *
     * @param in A Reader
     */
    public BufferedReader(Reader in) {
        this(in, defaultCharBufferSize);
    }
    
    /**
     * Creates a buffering character-input stream that uses an input buffer of
     * the specified size.
     *
     * @param in A Reader
     * @param sz Input-buffer size
     *
     * @throws IllegalArgumentException If {@code sz <= 0}
     */
    public BufferedReader(Reader in, int sz) {
        super(in);
        if(sz<=0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.in = in;
        cb = new char[sz];
        nextChar = nChars = 0;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads a single character.
     *
     * @return The character read, as an integer in the range
     * 0 to 65535 ({@code 0x00-0xffff}), or -1 if the
     * end of the stream has been reached
     *
     * @throws IOException If an I/O error occurs
     */
    // 返回从字符输入流中读取的一个char，返回-1表示读取失败
    public int read() throws IOException {
        synchronized(lock) {
            ensureOpen();
            
            for(; ; ) {
                // 如果现有缓冲区中的字符已经读完
                if(nextChar >= nChars) {
                    // 更新内部缓冲区，重新向其中填充字符
                    fill();
                    
                    // 如果输入流中已经没有可读字符
                    if(nextChar >= nChars) {
                        return -1;
                    }
                }
                
                if(skipLF) {
                    skipLF = false;
                    if(cb[nextChar] == '\n') {
                        nextChar++;
                        continue;
                    }
                }
                
                return cb[nextChar++];
            }
        }
    }
    
    /**
     * Reads characters into a portion of an array.
     *
     * <p> This method implements the general contract of the corresponding
     * <code>{@link Reader#read(char[], int, int) read}</code> method of the
     * <code>{@link Reader}</code> class.  As an additional convenience, it
     * attempts to read as many characters as possible by repeatedly invoking
     * the <code>read</code> method of the underlying stream.  This iterated
     * <code>read</code> continues until one of the following conditions becomes
     * true: <ul>
     *
     * <li> The specified number of characters have been read,
     *
     * <li> The <code>read</code> method of the underlying stream returns
     * <code>-1</code>, indicating end-of-file, or
     *
     * <li> The <code>ready</code> method of the underlying stream
     * returns <code>false</code>, indicating that further input requests
     * would block.
     *
     * </ul> If the first <code>read</code> on the underlying stream returns
     * <code>-1</code> to indicate end-of-file then this method returns
     * <code>-1</code>.  Otherwise this method returns the number of characters
     * actually read.
     *
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many characters as possible in the same fashion.
     *
     * <p> Ordinarily this method takes characters from this stream's character
     * buffer, filling it from the underlying stream as necessary.  If,
     * however, the buffer is empty, the mark is not valid, and the requested
     * length is at least as large as the buffer, then this method will read
     * characters directly from the underlying stream into the given array.
     * Thus redundant <code>BufferedReader</code>s will not copy data
     * unnecessarily.
     *
     * @param cbuf Destination buffer
     * @param off  Offset at which to start storing characters
     * @param len  Maximum number of characters to read
     *
     * @return The number of characters read, or -1 if the end of the
     * stream has been reached
     *
     * @throws IOException               If an I/O error occurs
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 从输入源读取len个字符存入cbuf的off处，返回实际读到的字符数
    public int read(char[] cbuf, int off, int len) throws IOException {
        synchronized(lock) {
            ensureOpen();
            
            if((off<0) || (off>cbuf.length) || (len<0) || ((off + len)>cbuf.length) || ((off + len)<0)) {
                throw new IndexOutOfBoundsException();
            } else if(len == 0) {
                return 0;
            }
            
            // 从输入源读取len个字符存入cbuf的off处，返回实际读到的字符数
            int n = read1(cbuf, off, len);
            if(n<=0) {
                return n;
            }
            
            while((n<len) && in.ready()) {
                // 从输入源读取len - n个字符存入cbuf的off + n处，返回实际读到的字符数
                int n1 = read1(cbuf, off + n, len - n);
                if(n1<=0) {
                    break;
                }
                
                n += n1;
            }
            
            return n;
        }
    }
    
    
    /**
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), a carriage return
     * followed immediately by a line feed, or by reaching the end-of-file
     * (EOF).
     *
     * @return A String containing the contents of the line, not including
     * any line-termination characters, or null if the end of the
     * stream has been reached without reading any characters
     *
     * @throws IOException If an I/O error occurs
     * @see java.nio.file.Files#readAllLines
     */
    // 读取一行的内容
    public String readLine() throws IOException {
        /*
         * 读取一行内容，每行的终止标记为'\r'或'\n'或'\r\n'或EOF。
         * 如果调用readLine()时遇到的首个字符是'\n'，不会忽略它
         */
        return readLine(false);
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 存档 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether this stream supports the mark() operation, which it does.
     */
    // 判断当前输入流是否支持存档标记，当前流支持
    public boolean markSupported() {
        return true;
    }
    
    /**
     * Marks the present position in the stream.  Subsequent calls to reset()
     * will attempt to reposition the stream to this point.
     *
     * @param readAheadLimit Limit on the number of characters that may be
     *                       read while still preserving the mark. An attempt
     *                       to reset the stream after reading characters
     *                       up to this limit or beyond may fail.
     *                       A limit value larger than the size of the input
     *                       buffer will cause a new buffer to be allocated
     *                       whose size is no smaller than limit.
     *                       Therefore large values should be used with care.
     *
     * @throws IllegalArgumentException If {@code readAheadLimit < 0}
     * @throws IOException              If an I/O error occurs
     */
    // 设置存档标记，readAheadLimit是存档区预读上限
    public void mark(int readAheadLimit) throws IOException {
        if(readAheadLimit<0) {
            throw new IllegalArgumentException("Read-ahead limit < 0");
        }
        
        synchronized(lock) {
            ensureOpen();
            this.readAheadLimit = readAheadLimit;
            markedChar = nextChar;
            markedSkipLF = skipLF;
        }
    }
    
    /**
     * Resets the stream to the most recent mark.
     *
     * @throws IOException If the stream has never been marked,
     *                     or if the mark has been invalidated
     */
    // 对于支持设置存档的输入流，可以重置其"读游标"到存档区的起始位置
    public void reset() throws IOException {
        synchronized(lock) {
            ensureOpen();
            
            if(markedChar<0) {
                throw new IOException((markedChar == INVALIDATED) ? "Mark invalid" : "Stream not marked");
            }
            
            nextChar = markedChar;
            skipLF = markedSkipLF;
        }
    }
    
    /*▲ 存档 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 关闭输入流
    public void close() throws IOException {
        synchronized(lock) {
            if(in == null) {
                return;
            }
            
            try {
                in.close();
            } finally {
                in = null;
                cb = null;
            }
        }
    }
    
    /**
     * Tells whether this stream is ready to be read.  A buffered character
     * stream is ready if the buffer is not empty, or if the underlying
     * character stream is ready.
     *
     * @throws IOException If an I/O error occurs
     */
    // 判断当前流是否已准备好被读取
    public boolean ready() throws IOException {
        synchronized(lock) {
            ensureOpen();
            
            /*
             * If newline needs to be skipped and the next char to be read
             * is a newline character, then just skip it right away.
             */
            if(skipLF) {
                /*
                 * Note that in.ready() will return true
                 * if and only if the next read on the stream will not block.
                 */
                if(nextChar >= nChars && in.ready()) {
                    // 更新内部缓冲区，重新向其中填充字符
                    fill();
                }
                
                if(nextChar<nChars) {
                    if(cb[nextChar] == '\n') {
                        nextChar++;
                    }
                    
                    skipLF = false;
                }
            }
            
            return (nextChar<nChars) || in.ready();
        }
    }
    
    /**
     * Skips characters.
     *
     * @param n The number of characters to skip
     *
     * @return The number of characters actually skipped
     *
     * @throws IllegalArgumentException If <code>n</code> is negative.
     * @throws IOException              If an I/O error occurs
     */
    // 读取中跳过n个字符，返回实际跳过的字符数
    public long skip(long n) throws IOException {
        if(n<0L) {
            throw new IllegalArgumentException("skip value is negative");
        }
        
        synchronized(lock) {
            ensureOpen();
            
            long r = n;
            
            while(r>0) {
                if(nextChar >= nChars) {
                    // 更新内部缓冲区，重新向其中填充字符
                    fill();
                }
                
                if(nextChar >= nChars) /* EOF */ {
                    break;
                }
                
                if(skipLF) {
                    skipLF = false;
                    if(cb[nextChar] == '\n') {
                        nextChar++;
                    }
                }
                
                long d = nChars - nextChar;
                
                if(r<=d) {
                    nextChar += r;
                    r = 0;
                    break;
                } else {
                    r -= d;
                    nextChar = nChars;
                }
            }
            
            return n - r;
        }
    }
    
    /**
     * Returns a {@code Stream}, the elements of which are lines read from
     * this {@code BufferedReader}.  The {@link Stream} is lazily populated,
     * i.e., read only occurs during the
     * <a href="../util/stream/package-summary.html#StreamOps">terminal
     * stream operation</a>.
     *
     * <p> The reader must not be operated on during the execution of the
     * terminal stream operation. Otherwise, the result of the terminal stream
     * operation is undefined.
     *
     * <p> After execution of the terminal stream operation there are no
     * guarantees that the reader will be at a specific position from which to
     * read the next character or line.
     *
     * <p> If an {@link IOException} is thrown when accessing the underlying
     * {@code BufferedReader}, it is wrapped in an {@link
     * UncheckedIOException} which will be thrown from the {@code Stream}
     * method that caused the read to take place. This method will return a
     * Stream if invoked on a BufferedReader that is closed. Any operation on
     * that stream that requires reading from the BufferedReader after it is
     * closed, will cause an UncheckedIOException to be thrown.
     *
     * @return a {@code Stream<String>} providing the lines of text
     * described by this {@code BufferedReader}
     *
     * @since 1.8
     */
    // 返回"行"的流，可用来按行获取输入
    public Stream<String> lines() {
        Iterator<String> iter = new Iterator<>() {
            String nextLine = null;
            
            @Override
            public boolean hasNext() {
                if(nextLine != null) {
                    return true;
                }
                
                try {
                    nextLine = readLine();
                    return (nextLine != null);
                } catch(IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            
            @Override
            public String next() {
                if(nextLine != null || hasNext()) {
                    String line = nextLine;
                    nextLine = null;
                    return line;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
        
        Spliterator<String> spliterator = Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED | Spliterator.NONNULL);
        return StreamSupport.stream(spliterator, false);
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Reads a line of text.
     * A line is considered to be terminated by any one of a line feed ('\n'), a carriage return ('\r'),
     * a carriage return followed immediately by a line feed, or by reaching the end-of-file (EOF).
     *
     * @param ignoreLF If true, the next '\n' will be skipped
     *
     * @return A String containing the contents of the line,
     * not including any line-termination characters,
     * or null if the end of the stream has been reached without reading any characters
     *
     * @throws IOException If an I/O error occurs
     * @see java.io.LineNumberReader#readLine()
     */
    /*
     * 读取一行内容，每行的终止标记为'\r'或'\n'或'\r\n'或EOF。
     *
     * ignoreLF：如果调用readLine()时遇到的首个字符是'\n'，是否忽略它
     */
    String readLine(boolean ignoreLF) throws IOException {
        StringBuffer s = null;
        int startChar;
        
        synchronized(lock) {
            ensureOpen();
            
            // 是否需要忽略缓冲区起始或每行开头的'\n'
            boolean omitLF = ignoreLF || skipLF;
            
            for(; ; ) {
                // 如果现有缓冲区中的字符已经读完
                if(nextChar >= nChars) {
                    // 更新内部缓冲区，重新向其中填充字符
                    fill();
                }
                
                // 如果输入流中已经没有可读字符
                if(nextChar >= nChars) { /* EOF */
                    if(s != null && s.length()>0) {
                        return s.toString();    // 返回当前行内容
                    } else {
                        return null;
                    }
                }
                
                // 是否到了一行的终点
                boolean eol = false;
                char c = 0;
                int i;
                
                /* Skip a leftover '\n', if necessary */
                // 如果需要跳过'\n'，且确实遇到了'\n'
                if(omitLF && (cb[nextChar] == '\n')) {
                    nextChar++; // 忽略'\n'
                }
                
                skipLF = false;
                omitLF = false;
                
                for(i = nextChar; i<nChars; i++) {
                    c = cb[i];
                    
                    if((c == '\n') || (c == '\r')) {
                        eol = true; // 遇到一行终点
                        break;
                    }
                }
                
                startChar = nextChar;
                nextChar = i;
                
                // 如果遇到了一行的终点
                if(eol) {
                    String str;
                    
                    if(s == null) {
                        str = new String(cb, startChar, i - startChar);
                    } else {
                        s.append(cb, startChar, i - startChar);
                        str = s.toString();
                    }
                    
                    nextChar++;
                    
                    // 最后遇到的是'\r'
                    if(c == '\r') {
                        skipLF = true;  // 需要跳过下一个紧邻的'\n'
                    }
                    
                    return str;
                }
                
                if(s == null) {
                    s = new StringBuffer(defaultExpectedLineLength);
                }
                
                s.append(cb, startChar, i - startChar);
            }
        }
    }
    
    /**
     * Reads characters into a portion of an array, reading from the underlying stream if necessary.
     */
    // 从输入源读取len个字符存入cbuf的off处，返回实际读到的字符数
    private int read1(char[] cbuf, int off, int len) throws IOException {
        if(nextChar >= nChars) {
            /*
             * If the requested length is at least as large as the buffer,
             * and if there is no mark/reset activity,
             * and if line feeds are not being skipped,
             * do not bother to copy the characters into the local buffer.
             * In this way buffered streams will cascade harmlessly.
             */
            if(len >= cb.length && markedChar<=UNMARKED && !skipLF) {
                return in.read(cbuf, off, len);
            }
            
            // 更新内部缓冲区，重新向其中填充字符
            fill();
        }
        
        if(nextChar >= nChars) {
            return -1;
        }
        
        if(skipLF) {
            skipLF = false;
            
            // 跳过紧随'\r'之后的'\n'
            if(cb[nextChar] == '\n') {
                nextChar++;
                
                if(nextChar >= nChars) {
                    // 更新内部缓冲区，重新向其中填充字符
                    fill();
                }
                
                if(nextChar >= nChars) {
                    return -1;
                }
            }
        }
        
        int n = Math.min(len, nChars - nextChar);
        
        System.arraycopy(cb, nextChar, cbuf, off, n);
        
        nextChar += n;
        
        return n;
    }
    
    /**
     * Fills the input buffer, taking the mark into account if it is valid.
     */
    // 更新内部缓冲区，重新向其中填充字符
    private void fill() throws IOException {
        int dst;
        
        // 如果不存在有效的存档标记
        if(markedChar<=UNMARKED) {
            dst = 0;
            
            // 如果存在存档标记
        } else {
            // 获取存档区已读过的字符数
            int delta = nextChar - markedChar;
            
            // 超出预读限制
            if(delta >= readAheadLimit) {
                /* Gone past read-ahead limit: Invalidate mark */
                markedChar = INVALIDATED;   // 存档标记作废，因为无法再放下标记的字符了
                readAheadLimit = 0;
                dst = 0;
                
                // 未超出预读限制
            } else {
                
                // 如果预读限制未超出cb容量，则把存档区读过的字符挪到cb的开头
                if(readAheadLimit<=cb.length) {
                    /* Shuffle in the current buffer */
                    System.arraycopy(cb, markedChar, cb, 0, delta);
                    
                    // 如果预读限制过大，则新建缓冲区，以存放存档区读过的字符
                } else {
                    /* Reallocate buffer to accommodate read-ahead limit */
                    char[] ncb = new char[readAheadLimit];
                    System.arraycopy(cb, markedChar, ncb, 0, delta);
                    cb = ncb;
                    
                }
                
                markedChar = 0;
                dst = delta;
                
                nextChar = nChars = delta;
            }
        }
        
        int n;
        do {
            /*
             * 尝试从字符输入流中读取cb.length - dst个char，并将其填充到cb的dst处。
             * 返回实际填充的字符数量，返回-1表示读取失败
             */
            n = in.read(cb, dst, cb.length - dst);
        } while(n == 0);
        
        if(n>0) {
            nChars = dst + n;
            nextChar = dst;
        }
    }
    
    /** Checks to make sure that the stream has not been closed */
    // 确保输入流处于开启状态
    private void ensureOpen() throws IOException {
        if(in == null) {
            throw new IOException("Stream closed");
        }
    }
    
}
