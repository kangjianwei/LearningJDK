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

import java.util.Formatter;
import java.util.Locale;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A {@code PrintStream} adds functionality to another output stream,
 * namely the ability to print representations of various data values
 * conveniently.  Two other features are provided as well.  Unlike other output
 * streams, a {@code PrintStream} never throws an
 * {@code IOException}; instead, exceptional situations merely set an
 * internal flag that can be tested via the {@code checkError} method.
 * Optionally, a {@code PrintStream} can be created so as to flush
 * automatically; this means that the {@code flush} method is
 * automatically invoked after a byte array is written, one of the
 * {@code println} methods is invoked, or a newline character or byte
 * ({@code '\n'}) is written.
 *
 * <p> All characters printed by a {@code PrintStream} are converted into
 * bytes using the given encoding or charset, or platform's default character
 * encoding if not specified.
 * The {@link PrintWriter} class should be used in situations that require
 *  writing characters rather than bytes.
 *
 * <p> This class always replaces malformed and unmappable character sequences with
 * the charset's default replacement string.
 * The {@linkplain java.nio.charset.CharsetEncoder} class should be used when more
 * control over the encoding process is required.
 *
 * @author     Frank Yellin
 * @author     Mark Reinhold
 * @since      1.0
 */
/*
 * 字节打印流：将输入源中的数据以字节形式写入到指定的最终输出流
 *
 * 最终输出流是多样化的：可能是字节流，也可能是文件(被认为是字节流)
 *
 *  ┌──────── streamEncoder (输出流编码器，带缓冲区)
 *  │            ↑
 *  |            │
 *  │    ┌──→ charOut (带有编码器的字符输出流)
 *  │    |       ↑
 *  ↓    |       │
 * this ─┼──→ textOut (字符输出流，带缓冲区)
 *       ┃
 *       ┃
 *       ┗━━→ out (字节输出流)
 *
 * 如果输入源是单个字节，或者是字节数组，则可以直接将数据写入out中。
 *
 * 如果输入源非字节，则需要先将其转换为字符序列，
 * 然后经过：
 *          textOut ==> [charOut ==> streamEncoder] ==> this ==> out
 * 这个流程，将字符序列转换为字节，再最终写入到out中。
 */
public class PrintStream extends FilterOutputStream implements Appendable, Closeable {
    
    /**
     * Track both the text- and character-output streams, so that their buffers
     * can be flushed without flushing the entire stream.
     */
    private BufferedWriter textOut;     // 带有内部缓存区的字符输出流
    
    private OutputStreamWriter charOut; // 带有编码器的字节输出流：将指定的字符序列转换为字节后输出到最终输出流
    
    private Formatter formatter;
    
    /**
     * 是否开启自动刷新(默认是禁止的)
     *
     * 一般来说，如果开启了自动刷新，且待写数据中存在'\n'时，会刷新最终输出流
     */
    private final boolean autoFlush;
    
    /** To avoid recursive closing */
    // 输出流是否已关闭
    private boolean closing = false;
    
    // 是否发生了IO异常
    private boolean trouble = false;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new print stream.  This stream will not flush automatically.
     *
     * @param out The output stream to which values and objects will be
     *            printed
     *
     * @see java.io.PrintWriter#PrintWriter(java.io.OutputStream)
     */
    // 用指定的最终输出流构造字节打印流
    public PrintStream(OutputStream out) {
        this(out, false);
    }
    
    /**
     * Creates a new print stream.
     *
     * @param out       The output stream to which values and objects will be
     *                  printed
     * @param autoFlush A boolean; if true, the output buffer will be flushed
     *                  whenever a byte array is written, one of the
     *                  {@code println} methods is invoked, or a newline
     *                  character or byte ({@code '\n'}) is written
     *
     * @see java.io.PrintWriter#PrintWriter(java.io.OutputStream, boolean)
     */
    // 用指定的最终输出流构造字节打印流，autoFlush指示是否开启自动刷新
    public PrintStream(OutputStream out, boolean autoFlush) {
        this(autoFlush, requireNonNull(out, "Null output stream"));
    }
    
    /**
     * Creates a new print stream.
     *
     * @param out       The output stream to which values and objects will be
     *                  printed
     * @param autoFlush A boolean; if true, the output buffer will be flushed
     *                  whenever a byte array is written, one of the
     *                  {@code println} methods is invoked, or a newline
     *                  character or byte ({@code '\n'}) is written
     * @param encoding  The name of a supported
     *                  <a href="../lang/package-summary.html#charenc">
     *                  character encoding</a>
     *
     * @throws UnsupportedEncodingException If the named encoding is not supported
     * @since 1.4
     */
    // 用指定的最终输出流构造字节打印流，autoFlush指示是否开启自动刷新，encoding指示编码字符时用到的字符集名称
    public PrintStream(OutputStream out, boolean autoFlush, String encoding) throws UnsupportedEncodingException {
        this(requireNonNull(out, "Null output stream"), autoFlush, toCharset(encoding));
    }
    
    /**
     * Creates a new print stream, with the specified OutputStream, automatic line
     * flushing and charset.  This convenience constructor creates the necessary
     * intermediate {@link java.io.OutputStreamWriter OutputStreamWriter},
     * which will encode characters using the provided charset.
     *
     * @param out       The output stream to which values and objects will be
     *                  printed
     * @param autoFlush A boolean; if true, the output buffer will be flushed
     *                  whenever a byte array is written, one of the
     *                  {@code println} methods is invoked, or a newline
     *                  character or byte ({@code '\n'}) is written
     * @param charset   A {@linkplain java.nio.charset.Charset charset}
     *
     * @since 10
     */
    // 用指定的最终输出流构造字节打印流，autoFlush指示是否开启自动刷新，charset指示编码字符时用到的字符集
    public PrintStream(OutputStream out, boolean autoFlush, Charset charset) {
        super(out);
        
        this.autoFlush = autoFlush;
        this.charOut = new OutputStreamWriter(this, charset);
        this.textOut = new BufferedWriter(charOut);
    }
    
    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file name.  This convenience constructor creates
     * the necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the
     * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}
     * for this instance of the Java virtual machine.
     *
     * @param fileName The name of the file to use as the destination of this print
     *                 stream.  If the file exists, then it will be truncated to
     *                 zero size; otherwise, a new file will be created.  The output
     *                 will be written to the file and is buffered.
     *
     * @throws FileNotFoundException If the given file object does not denote an existing, writable
     *                               regular file and a new regular file of that name cannot be
     *                               created, or if some other error occurs while opening or
     *                               creating the file
     * @throws SecurityException     If a security manager is present and {@link
     *                               SecurityManager#checkWrite checkWrite(fileName)} denies write
     *                               access to the file
     * @since 1.5
     */
    // 用指定名称的文件构造字节打印流
    public PrintStream(String fileName) throws FileNotFoundException {
        this(false, new FileOutputStream(fileName));
    }
    
    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file name and charset.  This convenience constructor creates
     * the necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param fileName The name of the file to use as the destination of this print
     *                 stream.  If the file exists, then it will be truncated to
     *                 zero size; otherwise, a new file will be created.  The output
     *                 will be written to the file and is buffered.
     * @param csn      The name of a supported {@linkplain java.nio.charset.Charset
     *                 charset}
     *
     * @throws FileNotFoundException        If the given file object does not denote an existing, writable
     *                                      regular file and a new regular file of that name cannot be
     *                                      created, or if some other error occurs while opening or
     *                                      creating the file
     * @throws SecurityException            If a security manager is present and {@link
     *                                      SecurityManager#checkWrite checkWrite(fileName)} denies write
     *                                      access to the file
     * @throws UnsupportedEncodingException If the named charset is not supported
     * @since 1.5
     */
    // 用指定名称的文件构造字节打印流，encoding指示编码字符时用到的字符集名称
    public PrintStream(String fileName, String encoding) throws FileNotFoundException, UnsupportedEncodingException {
        // ensure charset is checked before the file is opened
        this(false, toCharset(encoding), new FileOutputStream(fileName));
    }
    
    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file name and charset.  This convenience constructor creates
     * the necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param fileName The name of the file to use as the destination of this print
     *                 stream.  If the file exists, then it will be truncated to
     *                 zero size; otherwise, a new file will be created.  The output
     *                 will be written to the file and is buffered.
     * @param charset  A {@linkplain java.nio.charset.Charset charset}
     *
     * @throws IOException       if an I/O error occurs while opening or creating the file
     * @throws SecurityException If a security manager is present and {@link
     *                           SecurityManager#checkWrite checkWrite(fileName)} denies write
     *                           access to the file
     * @since 10
     */
    // 用指定名称的文件构造字节打印流，charset指示编码字符时用到的字符集
    public PrintStream(String fileName, Charset charset) throws IOException {
        this(false, requireNonNull(charset, "charset"), new FileOutputStream(fileName));
    }
    
    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file.  This convenience constructor creates the necessary
     * intermediate {@link java.io.OutputStreamWriter OutputStreamWriter},
     * which will encode characters using the {@linkplain
     * java.nio.charset.Charset#defaultCharset() default charset} for this
     * instance of the Java virtual machine.
     *
     * @param file The file to use as the destination of this print stream.  If the
     *             file exists, then it will be truncated to zero size; otherwise,
     *             a new file will be created.  The output will be written to the
     *             file and is buffered.
     *
     * @throws FileNotFoundException If the given file object does not denote an existing, writable
     *                               regular file and a new regular file of that name cannot be
     *                               created, or if some other error occurs while opening or
     *                               creating the file
     * @throws SecurityException     If a security manager is present and {@link
     *                               SecurityManager#checkWrite checkWrite(file.getPath())}
     *                               denies write access to the file
     * @since 1.5
     */
    // 用指定的文件构造字节打印流
    public PrintStream(File file) throws FileNotFoundException {
        this(false, new FileOutputStream(file));
    }
    
    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file and charset.  This convenience constructor creates
     * the necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param file The file to use as the destination of this print stream.  If the
     *             file exists, then it will be truncated to zero size; otherwise,
     *             a new file will be created.  The output will be written to the
     *             file and is buffered.
     * @param csn  The name of a supported {@linkplain java.nio.charset.Charset
     *             charset}
     *
     * @throws FileNotFoundException        If the given file object does not denote an existing, writable
     *                                      regular file and a new regular file of that name cannot be
     *                                      created, or if some other error occurs while opening or
     *                                      creating the file
     * @throws SecurityException            If a security manager is present and {@link
     *                                      SecurityManager#checkWrite checkWrite(file.getPath())}
     *                                      denies write access to the file
     * @throws UnsupportedEncodingException If the named charset is not supported
     * @since 1.5
     */
    // 用指定的文件构造字节打印流，encoding指示编码字符时用到的字符集名称
    public PrintStream(File file, String encoding) throws FileNotFoundException, UnsupportedEncodingException {
        // ensure charset is checked before the file is opened
        this(false, toCharset(encoding), new FileOutputStream(file));
    }
    
    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file and charset.  This convenience constructor creates
     * the necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param file    The file to use as the destination of this print stream.  If the
     *                file exists, then it will be truncated to zero size; otherwise,
     *                a new file will be created.  The output will be written to the
     *                file and is buffered.
     * @param charset A {@linkplain java.nio.charset.Charset charset}
     *
     * @throws IOException       if an I/O error occurs while opening or creating the file
     * @throws SecurityException If a security manager is present and {@link
     *                           SecurityManager#checkWrite checkWrite(file.getPath())}
     *                           denies write access to the file
     * @since 10
     */
    // 用指定的文件构造字节打印流，charset指示编码字符时用到的字符集
    public PrintStream(File file, Charset charset) throws IOException {
        this(false, requireNonNull(charset, "charset"), new FileOutputStream(file));
    }
    
    
    /** Private constructors */
    private PrintStream(boolean autoFlush, OutputStream out) {
        super(out);
        
        this.autoFlush = autoFlush;
        this.charOut = new OutputStreamWriter(this);
        this.textOut = new BufferedWriter(charOut);
    }
    
    /**
     * Variant of the private constructor so that the given charset name can be verified before evaluating the OutputStream argument.
     * Used by constructors creating a FileOutputStream that also take a charset name.
     */
    private PrintStream(boolean autoFlush, Charset charset, OutputStream out) {
        this(out, autoFlush, charset);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes the specified byte to this stream.  If the byte is a newline and
     * automatic flushing is enabled then the {@code flush} method will be
     * invoked.
     *
     * <p> Note that the byte is written as given; to write a character that
     * will be translated according to the platform's default character
     * encoding, use the {@code print(char)} or {@code println(char)}
     * methods.
     *
     * @param b The byte to be written
     *
     * @see #print(char)
     * @see #println(char)
     */
    /*
     * 将指定的字节写入(最终)字节输出流
     * 如果开启了自动刷新，则需要刷新最终输出流。
     */
    public void write(int b) {
        try {
            synchronized(this) {
                ensureOpen();
                
                // 将指定的字节写入到输出流
                out.write(b);
                
                // 如果遇到了'\n'，且开启了自动刷新，则刷新最终输出流
                if((b == '\n') && autoFlush) {
                    out.flush();    // 将out内部缓冲区(如果存在)中的字节写入到输出流
                }
            }
        } catch(InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch(IOException x) {
            trouble = true;
        }
    }
    
    /**
     * Writes {@code len} bytes from the specified byte array starting at
     * offset {@code off} to this stream.  If automatic flushing is
     * enabled then the {@code flush} method will be invoked.
     *
     * <p> Note that the bytes will be written as given; to write characters
     * that will be translated according to the platform's default character
     * encoding, use the {@code print(char)} or {@code println(char)}
     * methods.
     *
     * @param buf A byte array
     * @param off Offset from which to start taking bytes
     * @param len Number of bytes to write
     */
    /*
     * 将字节数组buf中off处起的len个字节写入(最终)字节输出流，
     * 如果开启了自动刷新，则需要刷新最终输出流(不管buf中有没有'\n')。
     */
    public void write(byte[] buf, int off, int len) {
        try {
            synchronized(this) {
                ensureOpen();
                
                // 将字节数组buf中off处起的len个字节写入到输出流
                out.write(buf, off, len);
                
                // 如果开启了自动刷新，则刷新最终输出流
                if(autoFlush) {
                    out.flush();
                }
            }
        } catch(InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch(IOException x) {
            trouble = true;
        }
    }
    
    
    /**
     * Prints a boolean value.  The string produced by {@link
     * java.lang.String#valueOf(boolean)} is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * {@link #write(int)} method.
     *
     * @param b The {@code boolean} to be printed
     */
    // 将指定的布尔值(true/false)以字节形式写入(最终)字节输出流
    public void print(boolean b) {
        write(String.valueOf(b));
    }
    
    /**
     * Prints a character.  The character is translated into one or more bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * {@link #write(int)} method.
     *
     * @param c The {@code char} to be printed
     */
    /*
     * 将指定的char以字节形式写入(最终)字节输出流，
     * 如果开启了自动刷新，且待写字符是'\n'，则需要刷新最终输出流。
     */
    public void print(char c) {
        write(String.valueOf(c));
    }
    
    /**
     * Prints an integer.  The string produced by {@link
     * java.lang.String#valueOf(int)} is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * {@link #write(int)} method.
     *
     * @param i The {@code int} to be printed
     *
     * @see java.lang.Integer#toString(int)
     */
    // 将指定的int以字节形式写入(最终)字节输出流
    public void print(int i) {
        write(String.valueOf(i));
    }
    
    /**
     * Prints a long integer.  The string produced by {@link
     * java.lang.String#valueOf(long)} is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * {@link #write(int)} method.
     *
     * @param l The {@code long} to be printed
     *
     * @see java.lang.Long#toString(long)
     */
    // 将指定的long以字节形式写入(最终)字节输出流
    public void print(long l) {
        write(String.valueOf(l));
    }
    
    /**
     * Prints a floating-point number.  The string produced by {@link
     * java.lang.String#valueOf(float)} is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * {@link #write(int)} method.
     *
     * @param f The {@code float} to be printed
     *
     * @see java.lang.Float#toString(float)
     */
    // 将指定的float以字节形式写入(最终)字节输出流
    public void print(float f) {
        write(String.valueOf(f));
    }
    
    /**
     * Prints a double-precision floating-point number.  The string produced by
     * {@link java.lang.String#valueOf(double)} is translated into
     * bytes according to the platform's default character encoding, and these
     * bytes are written in exactly the manner of the {@link
     * #write(int)} method.
     *
     * @param d The {@code double} to be printed
     *
     * @see java.lang.Double#toString(double)
     */
    // 将指定的double以字节形式写入(最终)字节输出流
    public void print(double d) {
        write(String.valueOf(d));
    }
    
    /**
     * Prints an array of characters.  The characters are converted into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * {@link #write(int)} method.
     *
     * @param s The array of chars to be printed
     *
     * @throws NullPointerException If {@code s} is {@code null}
     */
    /*
     * 将字符数组以字节形式写入(最终)字节输出流，
     * 如果开启了自动刷新，且待写字符数组中包含'\n'，则需要刷新最终输出流。
     */
    public void print(char[] s) {
        write(s);
    }
    
    /**
     * Prints a string.  If the argument is {@code null} then the string
     * {@code "null"} is printed.  Otherwise, the string's characters are
     * converted into bytes according to the platform's default character
     * encoding, and these bytes are written in exactly the manner of the
     * {@link #write(int)} method.
     *
     * @param s The {@code String} to be printed
     */
    /*
     * 将字符串String以字节形式写入输出流，
     * 如果开启了自动刷新，且待写字符串中包含'\n'，则需要刷新最终输出流。
     */
    public void print(String s) {
        write(String.valueOf(s));
    }
    
    /**
     * Prints an object.  The string produced by the {@link
     * java.lang.String#valueOf(Object)} method is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * {@link #write(int)} method.
     *
     * @param obj The {@code Object} to be printed
     *
     * @see java.lang.Object#toString()
     */
    /*
     * 将Object字符串化之后以字节形式写入输出流，
     * 如果开启了自动刷新，且Object字符串化之后包含'\n'，则需要刷新最终输出流。
     */
    public void print(Object obj) {
        write(String.valueOf(obj));
    }
    
    
    /**
     * Terminates the current line by writing the line separator string.  The
     * line separator string is defined by the system property
     * {@code line.separator}, and is not necessarily a single newline
     * character ({@code '\n'}).
     */
    /*
     * 向(最终)字节输出流写入换行标记，
     * 如果开启了自动刷新，则会在写完之后刷新最终输出流。
     */
    public void println() {
        newLine();
    }
    
    /**
     * Prints a boolean and then terminate the line.  This method behaves as
     * though it invokes {@link #print(boolean)} and then
     * {@link #println()}.
     *
     * @param x The {@code boolean} to be printed
     */
    /*
     * 将指定的布尔值(true/false)以字节形式写入(最终)字节输出流，会加换行标记，
     * 如果开启了自动刷新，则会在写完之后刷新最终输出流。
     */
    public void println(boolean x) {
        synchronized(this) {
            print(x);
            newLine();
        }
    }
    
    /**
     * Prints a character and then terminate the line.  This method behaves as
     * though it invokes {@link #print(char)} and then
     * {@link #println()}.
     *
     * @param x The {@code char} to be printed.
     */
    /*
     * 将指定的char以字节形式写入(最终)字节输出流，会加换行标记，
     * 如果开启了自动刷新，则会在写完之后刷新最终输出流。
     */
    public void println(char x) {
        synchronized(this) {
            print(x);
            newLine();
        }
    }
    
    /**
     * Prints an integer and then terminate the line.  This method behaves as
     * though it invokes {@link #print(int)} and then
     * {@link #println()}.
     *
     * @param x The {@code int} to be printed.
     */
    /*
     * 将指定的int以字节形式写入(最终)字节输出流，会加换行标记，
     * 如果开启了自动刷新，则会在写完之后刷新最终输出流。
     */
    public void println(int x) {
        synchronized(this) {
            print(x);
            newLine();
        }
    }
    
    /**
     * Prints a long and then terminate the line.  This method behaves as
     * though it invokes {@link #print(long)} and then
     * {@link #println()}.
     *
     * @param x a The {@code long} to be printed.
     */
    /*
     * 将指定的long以字节形式写入(最终)字节输出流，会加换行标记，
     * 如果开启了自动刷新，则会在写完之后刷新最终输出流。
     */
    public void println(long x) {
        synchronized(this) {
            print(x);
            newLine();
        }
    }
    
    /**
     * Prints a float and then terminate the line.  This method behaves as
     * though it invokes {@link #print(float)} and then
     * {@link #println()}.
     *
     * @param x The {@code float} to be printed.
     */
    /*
     * 将指定的float以字节形式写入(最终)字节输出流，会加换行标记，
     * 如果开启了自动刷新，则会在写完之后刷新最终输出流。
     */
    public void println(float x) {
        synchronized(this) {
            print(x);
            newLine();
        }
    }
    
    /**
     * Prints a double and then terminate the line.  This method behaves as
     * though it invokes {@link #print(double)} and then
     * {@link #println()}.
     *
     * @param x The {@code double} to be printed.
     */
    /*
     * 将指定的double以字节形式写入(最终)字节输出流，会加换行标记，
     * 如果开启了自动刷新，则会在写完之后刷新最终输出流。
     */
    public void println(double x) {
        synchronized(this) {
            print(x);
            newLine();
        }
    }
    
    /**
     * Prints an array of characters and then terminate the line.  This method
     * behaves as though it invokes {@link #print(char[])} and
     * then {@link #println()}.
     *
     * @param x an array of chars to print.
     */
    /*
     * 将字符数组以字节形式写入(最终)字节输出流，会加换行标记，
     * 如果开启了自动刷新，则会在写完之后刷新最终输出流。
     */
    public void println(char[] x) {
        synchronized(this) {
            print(x);
            newLine();
        }
    }
    
    /**
     * Prints a String and then terminate the line.  This method behaves as
     * though it invokes {@link #print(String)} and then
     * {@link #println()}.
     *
     * @param x The {@code String} to be printed.
     */
    /*
     * 将指定的String以字节形式写入(最终)字节输出流，会加换行标记，
     * 如果开启了自动刷新，则会在写完之后刷新最终输出流。
     */
    public void println(String x) {
        synchronized(this) {
            print(x);
            newLine();
        }
    }
    
    /**
     * Prints an Object and then terminate the line.  This method calls
     * at first String.valueOf(x) to get the printed object's string value,
     * then behaves as
     * though it invokes {@link #print(String)} and then
     * {@link #println()}.
     *
     * @param x The {@code Object} to be printed.
     */
    /*
     * 将指定的Object以字节形式写入(最终)字节输出流，会加换行标记，
     * 如果开启了自动刷新，则会在写完之后刷新最终输出流。
     */
    public void println(Object x) {
        String s = String.valueOf(x);
        synchronized(this) {
            print(s);
            newLine();
        }
    }
    
    
    /**
     * Appends the specified character to this output stream.
     *
     * <p> An invocation of this method of the form {@code out.append(c)}
     * behaves in exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.print(c)
     * }</pre>
     *
     * @param c The 16-bit character to append
     *
     * @return This output stream
     *
     * @since 1.5
     */
    /*
     * 将指定的char以字节形式写入(最终)字节输出流，
     * 如果开启了自动刷新，且待写字符是'\n'，则需要刷新最终输出流。
     */
    public PrintStream append(char c) {
        print(c);
        return this;
    }
    
    /**
     * Appends the specified character sequence to this output stream.
     *
     * <p> An invocation of this method of the form {@code out.append(csq)}
     * behaves in exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.print(csq.toString())
     * }</pre>
     *
     * <p> Depending on the specification of {@code toString} for the
     * character sequence {@code csq}, the entire sequence may not be
     * appended.  For instance, invoking then {@code toString} method of a
     * character buffer will return a subsequence whose content depends upon
     * the buffer's position and limit.
     *
     * @param csq The character sequence to append.  If {@code csq} is
     *            {@code null}, then the four characters {@code "null"} are
     *            appended to this output stream.
     *
     * @return This output stream
     *
     * @since 1.5
     */
    /*
     * 将字符序列csq以字节形式写入输出流，
     * 如果开启了自动刷新，且字符序列中包含'\n'，则需要刷新最终输出流。
     */
    public PrintStream append(CharSequence csq) {
        print(csq);
        return this;
    }
    
    /**
     * Appends a subsequence of the specified character sequence to this output
     * stream.
     *
     * <p> An invocation of this method of the form
     * {@code out.append(csq, start, end)} when
     * {@code csq} is not {@code null}, behaves in
     * exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.print(csq.subSequence(start, end).toString())
     * }</pre>
     *
     * @param csq   The character sequence from which a subsequence will be
     *              appended.  If {@code csq} is {@code null}, then characters
     *              will be appended as if {@code csq} contained the four
     *              characters {@code "null"}.
     * @param start The index of the first character in the subsequence
     * @param end   The index of the character following the last character in the
     *              subsequence
     *
     * @return This output stream
     *
     * @throws IndexOutOfBoundsException If {@code start} or {@code end} are negative, {@code start}
     *                                   is greater than {@code end}, or {@code end} is greater than
     *                                   {@code csq.length()}
     * @since 1.5
     */
    /*
     * 将csq[start, end)范围的字符序列以字节形式写入输出流，
     * 如果开启了自动刷新，且待写字符序列中包含'\n'，则需要刷新最终输出流。
     */
    public PrintStream append(CharSequence csq, int start, int end) {
        if(csq == null) {
            csq = "null";
        }
        
        return append(csq.subSequence(start, end));
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 格式化输出 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * A convenience method to write a formatted string to this output stream
     * using the specified format string and arguments.
     *
     * <p> An invocation of this method of the form
     * {@code out.printf(format, args)} behaves
     * in exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.format(format, args)
     * }</pre>
     *
     * @param format A format string as described in <a
     *               href="../util/Formatter.html#syntax">Format string syntax</a>
     * @param args   Arguments referenced by the format specifiers in the format
     *               string.  If there are more arguments than format specifiers, the
     *               extra arguments are ignored.  The number of arguments is
     *               variable and may be zero.  The maximum number of arguments is
     *               limited by the maximum dimension of a Java array as defined by
     *               <cite>The Java&trade; Virtual Machine Specification</cite>.
     *               The behaviour on a
     *               {@code null} argument depends on the <a
     *               href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @return This output stream
     *
     * @throws java.util.IllegalFormatException If a format string contains an illegal syntax, a format
     *                                          specifier that is incompatible with the given arguments,
     *                                          insufficient arguments given the format string, or other
     *                                          illegal conditions.  For specification of all possible
     *                                          formatting errors, see the <a
     *                                          href="../util/Formatter.html#detail">Details</a> section of the
     *                                          formatter class specification.
     * @throws NullPointerException             If the {@code format} is {@code null}
     * @since 1.5
     */
    // 按指定的格式format向输出流写入参数列表args中的数据
    public PrintStream printf(String format, Object... args) {
        return format(format, args);
    }
    
    /**
     * A convenience method to write a formatted string to this output stream
     * using the specified format string and arguments.
     *
     * <p> An invocation of this method of the form
     * {@code out.printf(l, format, args)} behaves
     * in exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.format(l, format, args)
     * }</pre>
     *
     * @param l      The {@linkplain java.util.Locale locale} to apply during
     *               formatting.  If {@code l} is {@code null} then no localization
     *               is applied.
     * @param format A format string as described in <a
     *               href="../util/Formatter.html#syntax">Format string syntax</a>
     * @param args   Arguments referenced by the format specifiers in the format
     *               string.  If there are more arguments than format specifiers, the
     *               extra arguments are ignored.  The number of arguments is
     *               variable and may be zero.  The maximum number of arguments is
     *               limited by the maximum dimension of a Java array as defined by
     *               <cite>The Java&trade; Virtual Machine Specification</cite>.
     *               The behaviour on a
     *               {@code null} argument depends on the <a
     *               href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @return This output stream
     *
     * @throws java.util.IllegalFormatException If a format string contains an illegal syntax, a format
     *                                          specifier that is incompatible with the given arguments,
     *                                          insufficient arguments given the format string, or other
     *                                          illegal conditions.  For specification of all possible
     *                                          formatting errors, see the <a
     *                                          href="../util/Formatter.html#detail">Details</a> section of the
     *                                          formatter class specification.
     * @throws NullPointerException             If the {@code format} is {@code null}
     * @since 1.5
     */
    // 按指定区域的格式format向输出流写入参数列表args中的数据
    public PrintStream printf(Locale locale, String format, Object... args) {
        return format(locale, format, args);
    }
    
    
    /**
     * Writes a formatted string to this output stream using the specified
     * format string and arguments.
     *
     * <p> The locale always used is the one returned by {@link
     * java.util.Locale#getDefault(Locale.Category)} with
     * {@link java.util.Locale.Category#FORMAT FORMAT} category specified,
     * regardless of any previous invocations of other formatting methods on
     * this object.
     *
     * @param format A format string as described in <a
     *               href="../util/Formatter.html#syntax">Format string syntax</a>
     * @param args   Arguments referenced by the format specifiers in the format
     *               string.  If there are more arguments than format specifiers, the
     *               extra arguments are ignored.  The number of arguments is
     *               variable and may be zero.  The maximum number of arguments is
     *               limited by the maximum dimension of a Java array as defined by
     *               <cite>The Java&trade; Virtual Machine Specification</cite>.
     *               The behaviour on a
     *               {@code null} argument depends on the <a
     *               href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @return This output stream
     *
     * @throws java.util.IllegalFormatException If a format string contains an illegal syntax, a format
     *                                          specifier that is incompatible with the given arguments,
     *                                          insufficient arguments given the format string, or other
     *                                          illegal conditions.  For specification of all possible
     *                                          formatting errors, see the <a
     *                                          href="../util/Formatter.html#detail">Details</a> section of the
     *                                          formatter class specification.
     * @throws NullPointerException             If the {@code format} is {@code null}
     * @since 1.5
     */
    // 按指定的格式format向输出流写入参数列表args中的数据
    public PrintStream format(String format, Object... args) {
        try {
            synchronized(this) {
                ensureOpen();
                
                if((formatter == null) || (formatter.locale() != Locale.getDefault(Locale.Category.FORMAT))) {
                    formatter = new Formatter((Appendable) this);
                }
                
                formatter.format(Locale.getDefault(Locale.Category.FORMAT), format, args);
            }
        } catch(InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch(IOException x) {
            trouble = true;
        }
        
        return this;
    }
    
    /**
     * Writes a formatted string to this output stream using the specified
     * format string and arguments.
     *
     * @param l      The {@linkplain java.util.Locale locale} to apply during
     *               formatting.  If {@code l} is {@code null} then no localization
     *               is applied.
     * @param format A format string as described in <a
     *               href="../util/Formatter.html#syntax">Format string syntax</a>
     * @param args   Arguments referenced by the format specifiers in the format
     *               string.  If there are more arguments than format specifiers, the
     *               extra arguments are ignored.  The number of arguments is
     *               variable and may be zero.  The maximum number of arguments is
     *               limited by the maximum dimension of a Java array as defined by
     *               <cite>The Java&trade; Virtual Machine Specification</cite>.
     *               The behaviour on a
     *               {@code null} argument depends on the <a
     *               href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @return This output stream
     *
     * @throws java.util.IllegalFormatException If a format string contains an illegal syntax, a format
     *                                          specifier that is incompatible with the given arguments,
     *                                          insufficient arguments given the format string, or other
     *                                          illegal conditions.  For specification of all possible
     *                                          formatting errors, see the <a
     *                                          href="../util/Formatter.html#detail">Details</a> section of the
     *                                          formatter class specification.
     * @throws NullPointerException             If the {@code format} is {@code null}
     * @since 1.5
     */
    // 按指定区域的格式format向输出流写入参数列表args中的数据
    public PrintStream format(Locale l, String format, Object... args) {
        try {
            synchronized(this) {
                ensureOpen();
                
                if((formatter == null) || (formatter.locale() != l)) {
                    formatter = new Formatter(this, l);
                }
                
                formatter.format(l, format, args);
            }
        } catch(InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch(IOException x) {
            trouble = true;
        }
        
        return this;
    }
    
    /*▲ 格式化输出 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Flushes the stream.  This is done by writing any buffered output bytes to
     * the underlying output stream and then flushing that stream.
     *
     * @see java.io.OutputStream#flush()
     */
    // 刷新当前字节打印流，本质是刷新内部封装的最终输出流
    public void flush() {
        synchronized(this) {
            try {
                ensureOpen();
                out.flush();
            } catch(IOException x) {
                trouble = true;
            }
        }
    }
    
    /**
     * Closes the stream.  This is done by flushing the stream and then closing
     * the underlying output stream.
     *
     * @see java.io.OutputStream#close()
     */
    // 关闭当前字节打印流
    public void close() {
        synchronized(this) {
            if(!closing) {
                closing = true;
                
                try {
                    textOut.close();    // 关闭内部的字符输出流
                    out.close();        // 关闭内部的字节输出流
                } catch(IOException x) {
                    trouble = true;
                }
                
                textOut = null;
                charOut = null;
                out = null;
            }
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Flushes the stream and checks its error state. The internal error state
     * is set to {@code true} when the underlying output stream throws an
     * {@code IOException} other than {@code InterruptedIOException},
     * and when the {@code setError} method is invoked.  If an operation
     * on the underlying output stream throws an
     * {@code InterruptedIOException}, then the {@code PrintStream}
     * converts the exception back into an interrupt by doing:
     * <pre>{@code
     *     Thread.currentThread().interrupt();
     * }</pre>
     * or the equivalent.
     *
     * @return {@code true} if and only if this stream has encountered an
     * {@code IOException} other than
     * {@code InterruptedIOException}, or the
     * {@code setError} method has been invoked
     */
    public boolean checkError() {
        if(out != null) {
            flush();
        }
        
        if(out instanceof PrintStream) {
            PrintStream ps = (PrintStream) out;
            return ps.checkError();
        }
        
        return trouble;
    }
    
    /**
     * Sets the error state of the stream to {@code true}.
     *
     * <p> This method will cause subsequent invocations of {@link
     * #checkError()} to return {@code true} until
     * {@link #clearError()} is invoked.
     *
     * @since 1.1
     */
    protected void setError() {
        trouble = true;
    }
    
    /**
     * Clears the internal error state of this stream.
     *
     * <p> This method will cause subsequent invocations of {@link
     * #checkError()} to return {@code false} until another write
     * operation fails and invokes {@link #setError()}.
     *
     * @since 1.6
     */
    protected void clearError() {
        trouble = false;
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 将字符数组buf以字节形式写入到输出流
    private void write(char[] buf) {
        try {
            synchronized(this) {
                ensureOpen();
                
                /*
                 * 将字符数组buf的内容写入到输出流，会经过下面的流程：
                 *
                 * textOut ==> [charOut ==> streamEncoder] ==> out
                 */
                textOut.write(buf);
                
                // 将textOut内部缓冲区中的字符刷新到输出流
                textOut.flushBuffer();
                
                /*
                 * 刷新charOut相当于刷新它内部的输出流编码器，
                 * 该操作会将输出流编码器内部缓冲区中的字节写到最终输出流中，
                 * 但是不会刷新输出流编码器内的最终输出流(在此处对应着out对象)
                 */
                charOut.flushBuffer();
                
                // 如果开启了自动刷新
                if(autoFlush) {
                    for(char c : buf) {
                        // 每遇到一次'\n'，就将最终输出流out刷新一次
                        if(c == '\n') {
                            out.flush();
                        }
                    }
                }
            }
        } catch(InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch(IOException x) {
            trouble = true;
        }
    }
    
    /*
     * 将字符串s以字节形式写入输出流，
     * 如果开启了自动刷新，且待写字符串中包含'\n'，则需要刷新最终输出流。
     */
    private void write(String s) {
        try {
            synchronized(this) {
                ensureOpen();
                
                /*
                 * 将字符串s中的字符写入到输出流，会经过下面的流程：
                 *
                 * textOut ==> [charOut ==> streamEncoder] ==> out
                 */
                textOut.write(s);
                
                // 将textOut内部缓冲区中的字符刷新到输出流
                textOut.flushBuffer();
                
                /*
                 * 刷新charOut相当于刷新它内部的输出流编码器，
                 * 该操作会将输出流编码器内部缓冲区中的字节写到最终输出流中，
                 * 但是不会刷新输出流编码器内的最终输出流(在此处对应着out对象)
                 */
                charOut.flushBuffer();
                
                // 如果开启了自动刷新，且待写字符串中包含'\n'，则需要刷新输出流
                if(autoFlush && (s.indexOf('\n') >= 0)) {
                    out.flush();
                }
            }
        } catch(InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch(IOException x) {
            trouble = true;
        }
    }
    
    // 向(最终)字节输出流写入换行标记，如果开启了自动刷新，则会刷新最终输出流
    private void newLine() {
        try {
            synchronized(this) {
                ensureOpen();
                textOut.newLine();
                textOut.flushBuffer();
                charOut.flushBuffer();
                if(autoFlush) {
                    out.flush();
                }
            }
        } catch(InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch(IOException x) {
            trouble = true;
        }
    }
    
    /**
     * Returns a charset object for the given charset name.
     *
     * @throws NullPointerException         is csn is null
     * @throws UnsupportedEncodingException if the charset is not supported
     */
    // 返回查找到的字符集（如果不存在则异常）
    private static Charset toCharset(String csn) throws UnsupportedEncodingException {
        requireNonNull(csn, "charsetName");
        
        try {
            return Charset.forName(csn);
        } catch(IllegalCharsetNameException | UnsupportedCharsetException unused) {
            // UnsupportedEncodingException should be thrown
            throw new UnsupportedEncodingException(csn);
        }
    }
    
    /** Check to make sure that the stream has not been closed */
    // 确保最终输出流未关闭
    private void ensureOpen() throws IOException {
        if(out == null) {
            throw new IOException("Stream closed");
        }
    }
    
    /**
     * requireNonNull is explicitly declared here so as not to create an extra
     * dependency on java.util.Objects.requireNonNull. PrintStream is loaded
     * early during system initialization.
     */
    private static <T> T requireNonNull(T obj, String message) {
        if(obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }
    
}
