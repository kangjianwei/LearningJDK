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

import java.util.Objects;
import java.util.Formatter;
import java.util.Locale;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Prints formatted representations of objects to a text-output stream.  This
 * class implements all of the {@code print} methods found in {@link
 * PrintStream}.  It does not contain methods for writing raw bytes, for which
 * a program should use unencoded byte streams.
 *
 * <p> Unlike the {@link PrintStream} class, if automatic flushing is enabled
 * it will be done only when one of the {@code println}, {@code printf}, or
 * {@code format} methods is invoked, rather than whenever a newline character
 * happens to be output.  These methods use the platform's own notion of line
 * separator rather than the newline character.
 *
 * <p> Methods in this class never throw I/O exceptions, although some of its
 * constructors may.  The client may inquire as to whether any errors have
 * occurred by invoking {@link #checkError checkError()}.
 *
 * <p> This class always replaces malformed and unmappable character sequences with
 * the charset's default replacement string.
 * The {@linkplain java.nio.charset.CharsetEncoder} class should be used when more
 * control over the encoding process is required.
 *
 * @author Frank Yellin
 * @author Mark Reinhold
 * @since 1.1
 */
/*
 * 字符打印流：将输入源中的字符序列写入到指定的最终输出流
 *
 * 最终输出流是多样化的：可能是字符流，也可能是字节流，还可能是文件(被认为是字节流)
 *
 * 注：如果提供的输入不是字符序列，会先将其转换为字符序列。
 */
public class PrintWriter extends Writer {
    
    /**
     * The underlying character-output stream of this
     * {@code PrintWriter}.
     *
     * @since 1.2
     */
    /*
     * 最终输出流的包装
     *
     * 可能直接就是传入的字符输出流，也可能由传入的字节输出流或传入的文件对象装饰而来
     */
    protected Writer out;
    
    // 当传入的最终输出流就是PrintStream类型时，让psOut指向该最终输出流对象
    private PrintStream psOut = null;
    
    private Formatter formatter;
    
    /**
     * 是否开启自动刷新(默认是禁止的)
     *
     * 如果开启了自动刷新，写入结束后会刷新最终输出流
     */
    private final boolean autoFlush;
    
    // 是否发生了IO异常
    private boolean trouble = false;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new PrintWriter, without automatic line flushing.
     *
     * @param out A character-output stream
     */
    // 用指定的最终输出流构造字符打印流
    public PrintWriter(Writer out) {
        this(out, false);
    }
    
    /**
     * Creates a new PrintWriter.
     *
     * @param out       A character-output stream
     * @param autoFlush A boolean; if true, the {@code println},
     *                  {@code printf}, or {@code format} methods will
     *                  flush the output buffer
     */
    // 用指定的最终输出流构造字符打印流，autoFlush指示是否开启自动刷新
    public PrintWriter(Writer out, boolean autoFlush) {
        super(out);
        this.out = out;
        this.autoFlush = autoFlush;
    }
    
    /**
     * Creates a new PrintWriter, without automatic line flushing, from an
     * existing OutputStream.  This convenience constructor creates the
     * necessary intermediate OutputStreamWriter, which will convert characters
     * into bytes using the default character encoding.
     *
     * @param out An output stream
     *
     * @see java.io.OutputStreamWriter#OutputStreamWriter(java.io.OutputStream)
     */
    // 用指定的最终输出流构造字符打印流
    public PrintWriter(OutputStream out) {
        this(out, false);
    }
    
    /**
     * Creates a new PrintWriter from an existing OutputStream.  This
     * convenience constructor creates the necessary intermediate
     * OutputStreamWriter, which will convert characters into bytes using the
     * default character encoding.
     *
     * @param out       An output stream
     * @param autoFlush A boolean; if true, the {@code println},
     *                  {@code printf}, or {@code format} methods will
     *                  flush the output buffer
     *
     * @see java.io.OutputStreamWriter#OutputStreamWriter(java.io.OutputStream)
     */
    // 用指定的最终输出流构造字符打印流，autoFlush指示是否开启自动刷新
    public PrintWriter(OutputStream out, boolean autoFlush) {
        this(out, autoFlush, Charset.defaultCharset());
    }
    
    /**
     * Creates a new PrintWriter from an existing OutputStream.  This
     * convenience constructor creates the necessary intermediate
     * OutputStreamWriter, which will convert characters into bytes using the
     * specified charset.
     *
     * @param out       An output stream
     * @param autoFlush A boolean; if true, the {@code println},
     *                  {@code printf}, or {@code format} methods will
     *                  flush the output buffer
     * @param charset   A {@linkplain java.nio.charset.Charset charset}
     *
     * @since 10
     */
    // 用指定的最终输出流构造字符打印流，autoFlush指示是否开启自动刷新，charset指示编码字符时用到的字符集
    public PrintWriter(OutputStream out, boolean autoFlush, Charset charset) {
        this(new BufferedWriter(new OutputStreamWriter(out, charset)), autoFlush);
        
        // save print stream for error propagation
        if(out instanceof PrintStream) {
            psOut = (PrintStream) out;
        }
    }
    
    /**
     * Creates a new PrintWriter, without automatic line flushing, with the
     * specified file name.  This convenience constructor creates the necessary
     * intermediate {@link java.io.OutputStreamWriter OutputStreamWriter},
     * which will encode characters using the {@linkplain
     * java.nio.charset.Charset#defaultCharset() default charset} for this
     * instance of the Java virtual machine.
     *
     * @param fileName The name of the file to use as the destination of this writer.
     *                 If the file exists then it will be truncated to zero size;
     *                 otherwise, a new file will be created.  The output will be
     *                 written to the file and is buffered.
     *
     * @throws FileNotFoundException If the given string does not denote an existing, writable
     *                               regular file and a new regular file of that name cannot be
     *                               created, or if some other error occurs while opening or
     *                               creating the file
     * @throws SecurityException     If a security manager is present and {@link
     *                               SecurityManager#checkWrite checkWrite(fileName)} denies write
     *                               access to the file
     * @since 1.5
     */
    // 用指定名称的文件构造字符打印流
    public PrintWriter(String fileName) throws FileNotFoundException {
        this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName))), false);
    }
    
    /**
     * Creates a new PrintWriter, without automatic line flushing, with the
     * specified file name and charset.  This convenience constructor creates
     * the necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param fileName The name of the file to use as the destination of this writer.
     *                 If the file exists then it will be truncated to zero size;
     *                 otherwise, a new file will be created.  The output will be
     *                 written to the file and is buffered.
     * @param csn      The name of a supported {@linkplain java.nio.charset.Charset
     *                 charset}
     *
     * @throws FileNotFoundException        If the given string does not denote an existing, writable
     *                                      regular file and a new regular file of that name cannot be
     *                                      created, or if some other error occurs while opening or
     *                                      creating the file
     * @throws SecurityException            If a security manager is present and {@link
     *                                      SecurityManager#checkWrite checkWrite(fileName)} denies write
     *                                      access to the file
     * @throws UnsupportedEncodingException If the named charset is not supported
     * @since 1.5
     */
    // 用指定名称的文件构造字符打印流，encoding指示编码字符时用到的字符集名称
    public PrintWriter(String fileName, String encoding) throws FileNotFoundException, UnsupportedEncodingException {
        this(toCharset(encoding), new File(fileName));
    }
    
    /**
     * Creates a new PrintWriter, without automatic line flushing, with the
     * specified file name and charset.  This convenience constructor creates
     * the necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param fileName The name of the file to use as the destination of this writer.
     *                 If the file exists then it will be truncated to zero size;
     *                 otherwise, a new file will be created.  The output will be
     *                 written to the file and is buffered.
     * @param charset  A {@linkplain java.nio.charset.Charset charset}
     *
     * @throws IOException       if an I/O error occurs while opening or creating the file
     * @throws SecurityException If a security manager is present and {@link
     *                           SecurityManager#checkWrite checkWrite(fileName)} denies write
     *                           access to the file
     * @since 10
     */
    // 用指定名称的文件构造字符打印流，charset指示编码字符时用到的字符集
    public PrintWriter(String fileName, Charset charset) throws IOException {
        this(Objects.requireNonNull(charset, "charset"), new File(fileName));
    }
    
    /**
     * Creates a new PrintWriter, without automatic line flushing, with the
     * specified file.  This convenience constructor creates the necessary
     * intermediate {@link java.io.OutputStreamWriter OutputStreamWriter},
     * which will encode characters using the {@linkplain
     * java.nio.charset.Charset#defaultCharset() default charset} for this
     * instance of the Java virtual machine.
     *
     * @param file The file to use as the destination of this writer.  If the file
     *             exists then it will be truncated to zero size; otherwise, a new
     *             file will be created.  The output will be written to the file
     *             and is buffered.
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
    // 用指定的文件构造字符打印流
    public PrintWriter(File file) throws FileNotFoundException {
        this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))), false);
    }
    
    /**
     * Creates a new PrintWriter, without automatic line flushing, with the
     * specified file and charset.  This convenience constructor creates the
     * necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param file The file to use as the destination of this writer.  If the file
     *             exists then it will be truncated to zero size; otherwise, a new
     *             file will be created.  The output will be written to the file
     *             and is buffered.
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
    // 用指定的文件构造字符打印流，encoding指示编码字符时用到的字符集名称
    public PrintWriter(File file, String encoding) throws FileNotFoundException, UnsupportedEncodingException {
        this(toCharset(encoding), file);
    }
    
    /**
     * Creates a new PrintWriter, without automatic line flushing, with the
     * specified file and charset.  This convenience constructor creates the
     * necessary intermediate {@link java.io.OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param file    The file to use as the destination of this writer.  If the file
     *                exists then it will be truncated to zero size; otherwise, a new
     *                file will be created.  The output will be written to the file
     *                and is buffered.
     * @param charset A {@linkplain java.nio.charset.Charset charset}
     *
     * @throws IOException       if an I/O error occurs while opening or creating the file
     * @throws SecurityException If a security manager is present and {@link
     *                           SecurityManager#checkWrite checkWrite(file.getPath())}
     *                           denies write access to the file
     * @since 10
     */
    // 用指定的文件构造字符打印流，charset指示编码字符时用到的字符集
    public PrintWriter(File file, Charset charset) throws IOException {
        this(Objects.requireNonNull(charset, "charset"), file);
    }
    
    
    /** Private constructor */
    private PrintWriter(Charset charset, File file) throws FileNotFoundException {
        this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset)), false);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes a single character.
     *
     * @param c int specifying a character to be written.
     */
    // 将指定的字符写入到最终输出流
    public void write(int c) {
        try {
            synchronized(lock) {
                ensureOpen();
                out.write(c);
            }
        } catch(InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch(IOException x) {
            trouble = true;
        }
    }
    
    /**
     * Writes an array of characters.  This method cannot be inherited from the
     * Writer class because it must suppress I/O exceptions.
     *
     * @param buf Array of characters to be written
     */
    // 将指定的字符数组写入到最终输出流
    public void write(char[] buf) {
        write(buf, 0, buf.length);
    }
    
    /**
     * Writes A Portion of an array of characters.
     *
     * @param buf Array of characters
     * @param off Offset from which to start writing characters
     * @param len Number of characters to write
     *
     * @throws IndexOutOfBoundsException If the values of the {@code off} and {@code len} parameters
     *                                   cause the corresponding method of the underlying {@code Writer}
     *                                   to throw an {@code IndexOutOfBoundsException}
     */
    // 将指定字符数组中off处起的len个字符写入到最终输出流
    public void write(char[] buf, int off, int len) {
        try {
            synchronized(lock) {
                ensureOpen();
                out.write(buf, off, len);
            }
        } catch(InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch(IOException x) {
            trouble = true;
        }
    }
    
    /**
     * Writes a string.  This method cannot be inherited from the Writer class
     * because it must suppress I/O exceptions.
     *
     * @param s String to be written
     */
    // 将指定的字符串写入到最终输出流
    public void write(String s) {
        write(s, 0, s.length());
    }
    
    /**
     * Writes a portion of a string.
     *
     * @param s   A String
     * @param off Offset from which to start writing characters
     * @param len Number of characters to write
     *
     * @throws IndexOutOfBoundsException If the values of the {@code off} and {@code len} parameters
     *                                   cause the corresponding method of the underlying {@code Writer}
     *                                   to throw an {@code IndexOutOfBoundsException}
     */
    // 将指定字符串中off处起的len个字符写入到最终输出流
    public void write(String s, int off, int len) {
        try {
            synchronized(lock) {
                ensureOpen();
                out.write(s, off, len);
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
     * are written in exactly the manner of the {@link
     * #write(int)} method.
     *
     * @param b The {@code boolean} to be printed
     */
    // 将指定的布尔值(true/false)以写入(最终)字节输出流
    public void print(boolean b) {
        write(String.valueOf(b));
    }
    
    /**
     * Prints a character.  The character is translated into one or more bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the {@link
     * #write(int)} method.
     *
     * @param c The {@code char} to be printed
     */
    // 将指定的char写入到最终输出流
    public void print(char c) {
        write(c);
    }
    
    /**
     * Prints an integer.  The string produced by {@link
     * java.lang.String#valueOf(int)} is translated into bytes according
     * to the platform's default character encoding, and these bytes are
     * written in exactly the manner of the {@link #write(int)}
     * method.
     *
     * @param i The {@code int} to be printed
     *
     * @see java.lang.Integer#toString(int)
     */
    // 将指定的int写入到最终输出流
    public void print(int i) {
        write(String.valueOf(i));
    }
    
    /**
     * Prints a long integer.  The string produced by {@link
     * java.lang.String#valueOf(long)} is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the {@link #write(int)}
     * method.
     *
     * @param l The {@code long} to be printed
     *
     * @see java.lang.Long#toString(long)
     */
    // 将指定的long写入到最终输出流
    public void print(long l) {
        write(String.valueOf(l));
    }
    
    /**
     * Prints a floating-point number.  The string produced by {@link
     * java.lang.String#valueOf(float)} is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the {@link #write(int)}
     * method.
     *
     * @param f The {@code float} to be printed
     *
     * @see java.lang.Float#toString(float)
     */
    // 将指定的float写入到最终输出流
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
    // 将指定的double写入到最终输出流
    public void print(double d) {
        write(String.valueOf(d));
    }
    
    /**
     * Prints an array of characters.  The characters are converted into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the {@link #write(int)}
     * method.
     *
     * @param s The array of chars to be printed
     *
     * @throws NullPointerException If {@code s} is {@code null}
     */
    // 将指定的字符数组写入到最终输出流
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
    // 将指定的String写入到最终输出流
    public void print(String s) {
        write(String.valueOf(s));
    }
    
    /**
     * Prints an object.  The string produced by the {@link
     * java.lang.String#valueOf(Object)} method is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the {@link #write(int)}
     * method.
     *
     * @param obj The {@code Object} to be printed
     *
     * @see java.lang.Object#toString()
     */
    // 将指定的Object(字符串化之后)写入到最终输出流
    public void print(Object obj) {
        write(String.valueOf(obj));
    }
    
    
    /**
     * Terminates the current line by writing the line separator string.  The
     * line separator string is defined by the system property
     * {@code line.separator}, and is not necessarily a single newline
     * character ({@code '\n'}).
     */
    // 向最终输出流写入换行标记，如果开启了自动刷新，则会刷新最终输出流
    public void println() {
        newLine();
    }
    
    /**
     * Prints a boolean value and then terminates the line.  This method behaves
     * as though it invokes {@link #print(boolean)} and then
     * {@link #println()}.
     *
     * @param x the {@code boolean} value to be printed
     */
    /*
     * 将指定的布尔值(true/false)写入最终输出流，会加换行标记，
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public void println(boolean x) {
        synchronized(lock) {
            print(x);
            println();
        }
    }
    
    /**
     * Prints a character and then terminates the line.  This method behaves as
     * though it invokes {@link #print(char)} and then {@link
     * #println()}.
     *
     * @param x the {@code char} value to be printed
     */
    /*
     * 将指定的char写入最终输出流，会加换行标记，
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public void println(char x) {
        synchronized(lock) {
            print(x);
            println();
        }
    }
    
    /**
     * Prints an integer and then terminates the line.  This method behaves as
     * though it invokes {@link #print(int)} and then {@link
     * #println()}.
     *
     * @param x the {@code int} value to be printed
     */
    /*
     * 将指定的int写入最终输出流，会加换行标记，
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public void println(int x) {
        synchronized(lock) {
            print(x);
            println();
        }
    }
    
    /**
     * Prints a long integer and then terminates the line.  This method behaves
     * as though it invokes {@link #print(long)} and then
     * {@link #println()}.
     *
     * @param x the {@code long} value to be printed
     */
    /*
     * 将指定的long写入最终输出流，会加换行标记，
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public void println(long x) {
        synchronized(lock) {
            print(x);
            println();
        }
    }
    
    /**
     * Prints a floating-point number and then terminates the line.  This method
     * behaves as though it invokes {@link #print(float)} and then
     * {@link #println()}.
     *
     * @param x the {@code float} value to be printed
     */
    /*
     * 将指定的float写入最终输出流，会加换行标记，
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public void println(float x) {
        synchronized(lock) {
            print(x);
            println();
        }
    }
    
    /**
     * Prints a double-precision floating-point number and then terminates the
     * line.  This method behaves as though it invokes {@link
     * #print(double)} and then {@link #println()}.
     *
     * @param x the {@code double} value to be printed
     */
    /*
     * 将指定的double写入最终输出流，会加换行标记，
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public void println(double x) {
        synchronized(lock) {
            print(x);
            println();
        }
    }
    
    /**
     * Prints an array of characters and then terminates the line.  This method
     * behaves as though it invokes {@link #print(char[])} and then
     * {@link #println()}.
     *
     * @param x the array of {@code char} values to be printed
     */
    /*
     * 将指定的字符数组写入最终输出流，会加换行标记，
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public void println(char[] x) {
        synchronized(lock) {
            print(x);
            println();
        }
    }
    
    /**
     * Prints a String and then terminates the line.  This method behaves as
     * though it invokes {@link #print(String)} and then
     * {@link #println()}.
     *
     * @param x the {@code String} value to be printed
     */
    /*
     * 将指定的String写入最终输出流，会加换行标记，
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public void println(String x) {
        synchronized(lock) {
            print(x);
            println();
        }
    }
    
    /**
     * Prints an Object and then terminates the line.  This method calls
     * at first String.valueOf(x) to get the printed object's string value,
     * then behaves as
     * though it invokes {@link #print(String)} and then
     * {@link #println()}.
     *
     * @param x The {@code Object} to be printed.
     */
    /*
     * 将指定的Object(字符串化之后)写入最终输出流，会加换行标记，
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public void println(Object x) {
        String s = String.valueOf(x);
        synchronized(lock) {
            print(s);
            println();
        }
    }
    
    
    /**
     * Appends the specified character to this writer.
     *
     * <p> An invocation of this method of the form {@code out.append(c)}
     * behaves in exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.write(c)
     * }</pre>
     *
     * @param c The 16-bit character to append
     *
     * @return This writer
     *
     * @since 1.5
     */
    // 将指定的字符写入到最终输出流
    public PrintWriter append(char c) {
        write(c);
        return this;
    }
    
    /**
     * Appends the specified character sequence to this writer.
     *
     * <p> An invocation of this method of the form {@code out.append(csq)}
     * behaves in exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.write(csq.toString())
     * }</pre>
     *
     * <p> Depending on the specification of {@code toString} for the
     * character sequence {@code csq}, the entire sequence may not be
     * appended. For instance, invoking the {@code toString} method of a
     * character buffer will return a subsequence whose content depends upon
     * the buffer's position and limit.
     *
     * @param csq The character sequence to append.  If {@code csq} is
     *            {@code null}, then the four characters {@code "null"} are
     *            appended to this writer.
     *
     * @return This writer
     *
     * @since 1.5
     */
    // 将指定的字符序列写入到最终输出流
    public PrintWriter append(CharSequence csq) {
        write(String.valueOf(csq));
        return this;
    }
    
    /**
     * Appends a subsequence of the specified character sequence to this writer.
     *
     * <p> An invocation of this method of the form
     * {@code out.append(csq, start, end)}
     * when {@code csq} is not {@code null}, behaves in
     * exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.write(csq.subSequence(start, end).toString())
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
     * @return This writer
     *
     * @throws IndexOutOfBoundsException If {@code start} or {@code end} are negative, {@code start}
     *                                   is greater than {@code end}, or {@code end} is greater than
     *                                   {@code csq.length()}
     * @since 1.5
     */
    // 将csq[start, end)范围的字符序列写入到最终输出流
    public PrintWriter append(CharSequence csq, int start, int end) {
        if(csq == null) {
            csq = "null";
        }
        return append(csq.subSequence(start, end));
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 格式化输出 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * A convenience method to write a formatted string to this writer using
     * the specified format string and arguments.  If automatic flushing is
     * enabled, calls to this method will flush the output buffer.
     *
     * <p> An invocation of this method of the form
     * {@code out.printf(format, args)}
     * behaves in exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.format(format, args)
     * }</pre>
     *
     * @param format A format string as described in <a
     *               href="../util/Formatter.html#syntax">Format string syntax</a>.
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
     * @return This writer
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
    /*
     * 按指定的格式format向输出流写入参数列表args中的数据
     *
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public PrintWriter printf(String format, Object... args) {
        return format(format, args);
    }
    
    /**
     * A convenience method to write a formatted string to this writer using
     * the specified format string and arguments.  If automatic flushing is
     * enabled, calls to this method will flush the output buffer.
     *
     * <p> An invocation of this method of the form
     * {@code out.printf(l, format, args)}
     * behaves in exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.format(l, format, args)
     * }</pre>
     *
     * @param l      The {@linkplain java.util.Locale locale} to apply during
     *               formatting.  If {@code l} is {@code null} then no localization
     *               is applied.
     * @param format A format string as described in <a
     *               href="../util/Formatter.html#syntax">Format string syntax</a>.
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
     * @return This writer
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
    /*
     * 按指定区域的格式format向输出流写入参数列表args中的数据
     *
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public PrintWriter printf(Locale l, String format, Object... args) {
        return format(l, format, args);
    }
    
    
    /**
     * Writes a formatted string to this writer using the specified format
     * string and arguments.  If automatic flushing is enabled, calls to this
     * method will flush the output buffer.
     *
     * <p> The locale always used is the one returned by {@link
     * java.util.Locale#getDefault() Locale.getDefault()}, regardless of any
     * previous invocations of other formatting methods on this object.
     *
     * @param format A format string as described in <a
     *               href="../util/Formatter.html#syntax">Format string syntax</a>.
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
     * @return This writer
     *
     * @throws java.util.IllegalFormatException If a format string contains an illegal syntax, a format
     *                                          specifier that is incompatible with the given arguments,
     *                                          insufficient arguments given the format string, or other
     *                                          illegal conditions.  For specification of all possible
     *                                          formatting errors, see the <a
     *                                          href="../util/Formatter.html#detail">Details</a> section of the
     *                                          Formatter class specification.
     * @throws NullPointerException             If the {@code format} is {@code null}
     * @since 1.5
     */
    /*
     * 按指定的格式format向输出流写入参数列表args中的数据
     *
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public PrintWriter format(String format, Object... args) {
        try {
            synchronized(lock) {
                ensureOpen();
                
                if((formatter == null) || (formatter.locale() != Locale.getDefault())) {
                    formatter = new Formatter(this);
                }
                
                formatter.format(Locale.getDefault(), format, args);
                
                if(autoFlush) {
                    out.flush();
                }
            }
        } catch(InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch(IOException x) {
            trouble = true;
        }
        return this;
    }
    
    /**
     * Writes a formatted string to this writer using the specified format
     * string and arguments.  If automatic flushing is enabled, calls to this
     * method will flush the output buffer.
     *
     * @param l      The {@linkplain java.util.Locale locale} to apply during
     *               formatting.  If {@code l} is {@code null} then no localization
     *               is applied.
     * @param format A format string as described in <a
     *               href="../util/Formatter.html#syntax">Format string syntax</a>.
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
     * @return This writer
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
    /*
     * 按指定区域的格式format向输出流写入参数列表args中的数据
     *
     * 如果开启了自动刷新，则会刷新最终输出流
     */
    public PrintWriter format(Locale l, String format, Object... args) {
        try {
            synchronized(lock) {
                ensureOpen();
                
                if((formatter == null) || (formatter.locale() != l)) {
                    formatter = new Formatter(this, l);
                }
                
                formatter.format(l, format, args);
                
                if(autoFlush) {
                    out.flush();
                }
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
     * Flushes the stream.
     *
     * @see #checkError()
     */
    // 刷新当前字符打印流，本质是刷新内部封装的最终输出流
    public void flush() {
        try {
            synchronized(lock) {
                ensureOpen();
                out.flush();
            }
        } catch(IOException x) {
            trouble = true;
        }
    }
    
    /**
     * Closes the stream and releases any system resources associated
     * with it. Closing a previously closed stream has no effect.
     *
     * @see #checkError()
     */
    // 关闭当前字符打印流
    public void close() {
        try {
            synchronized(lock) {
                if(out == null) {
                    return;
                }
                out.close();
                out = null;
            }
        } catch(IOException x) {
            trouble = true;
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Flushes the stream if it's not closed and checks its error state.
     *
     * @return {@code true} if the print stream has encountered an error,
     * either on the underlying output stream or during a format
     * conversion.
     */
    public boolean checkError() {
        if(out != null) {
            flush();
        }
        
        if(out instanceof PrintWriter) {
            PrintWriter pw = (PrintWriter) out;
            return pw.checkError();
        } else if(psOut != null) {
            return psOut.checkError();
        }
        
        return trouble;
    }
    
    /**
     * Indicates that an error has occurred.
     *
     * <p> This method will cause subsequent invocations of {@link
     * #checkError()} to return {@code true} until {@link
     * #clearError()} is invoked.
     */
    protected void setError() {
        trouble = true;
    }
    
    /**
     * Clears the error state of this stream.
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
    
    
    
    // 向最终输出流写入换行标记，如果开启了自动刷新，则会刷新最终输出流
    private void newLine() {
        try {
            synchronized(lock) {
                ensureOpen();
                out.write(System.lineSeparator());
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
        Objects.requireNonNull(csn, "charsetName");
        
        try {
            return Charset.forName(csn);
        } catch(IllegalCharsetNameException | UnsupportedCharsetException unused) {
            // UnsupportedEncodingException should be thrown
            throw new UnsupportedEncodingException(csn);
        }
    }
    
    /** Checks to make sure that the stream has not been closed */
    // 确保最终输出流未关闭
    private void ensureOpen() throws IOException {
        if(out == null) {
            throw new IOException("Stream closed");
        }
    }
    
}
