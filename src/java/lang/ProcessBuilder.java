/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import sun.security.action.GetPropertyAction;

/**
 * This class is used to create operating system processes.
 *
 * <p>Each {@code ProcessBuilder} instance manages a collection
 * of process attributes.  The {@link #start()} method creates a new
 * {@link Process} instance with those attributes.  The {@link
 * #start()} method can be invoked repeatedly from the same instance
 * to create new subprocesses with identical or related attributes.
 * <p>
 * The {@link #startPipeline startPipeline} method can be invoked to create
 * a pipeline of new processes that send the output of each process
 * directly to the next process.  Each process has the attributes of
 * its respective ProcessBuilder.
 *
 * <p>Each process builder manages these process attributes:
 *
 * <ul>
 *
 * <li>a <i>command</i>, a list of strings which signifies the
 * external program file to be invoked and its arguments, if any.
 * Which string lists represent a valid operating system command is
 * system-dependent.  For example, it is common for each conceptual
 * argument to be an element in this list, but there are operating
 * systems where programs are expected to tokenize command line
 * strings themselves - on such a system a Java implementation might
 * require commands to contain exactly two elements.
 *
 * <li>an <i>environment</i>, which is a system-dependent mapping from
 * <i>variables</i> to <i>values</i>.  The initial value is a copy of
 * the environment of the current process (see {@link System#getenv()}).
 *
 * <li>a <i>working directory</i>.  The default value is the current
 * working directory of the current process, usually the directory
 * named by the system property {@code user.dir}.
 *
 * <li><a id="redirect-input">a source of <i>standard input</i></a>.
 * By default, the subprocess reads input from a pipe.  Java code
 * can access this pipe via the output stream returned by
 * {@link Process#getOutputStream()}.  However, standard input may
 * be redirected to another source using
 * {@link #redirectInput(Redirect) redirectInput}.
 * In this case, {@link Process#getOutputStream()} will return a
 * <i>null output stream</i>, for which:
 *
 * <ul>
 * <li>the {@link OutputStream#write(int) write} methods always
 * throw {@code IOException}
 * <li>the {@link OutputStream#close() close} method does nothing
 * </ul>
 *
 * <li><a id="redirect-output">a destination for <i>standard output</i>
 * and <i>standard error</i></a>.  By default, the subprocess writes standard
 * output and standard error to pipes.  Java code can access these pipes
 * via the input streams returned by {@link Process#getOutputStream()} and
 * {@link Process#getErrorStream()}.  However, standard output and
 * standard error may be redirected to other destinations using
 * {@link #redirectOutput(Redirect) redirectOutput} and
 * {@link #redirectError(Redirect) redirectError}.
 * In this case, {@link Process#getInputStream()} and/or
 * {@link Process#getErrorStream()} will return a <i>null input
 * stream</i>, for which:
 *
 * <ul>
 * <li>the {@link InputStream#read() read} methods always return
 * {@code -1}
 * <li>the {@link InputStream#available() available} method always returns
 * {@code 0}
 * <li>the {@link InputStream#close() close} method does nothing
 * </ul>
 *
 * <li>a <i>redirectErrorStream</i> property.  Initially, this property
 * is {@code false}, meaning that the standard output and error
 * output of a subprocess are sent to two separate streams, which can
 * be accessed using the {@link Process#getInputStream()} and {@link
 * Process#getErrorStream()} methods.
 *
 * <p>If the value is set to {@code true}, then:
 *
 * <ul>
 * <li>standard error is merged with the standard output and always sent
 * to the same destination (this makes it easier to correlate error
 * messages with the corresponding output)
 * <li>the common destination of standard error and standard output can be
 * redirected using
 * {@link #redirectOutput(Redirect) redirectOutput}
 * <li>any redirection set by the
 * {@link #redirectError(Redirect) redirectError}
 * method is ignored when creating a subprocess
 * <li>the stream returned from {@link Process#getErrorStream()} will
 * always be a <a href="#redirect-output">null input stream</a>
 * </ul>
 *
 * </ul>
 *
 * <p>Modifying a process builder's attributes will affect processes
 * subsequently started by that object's {@link #start()} method, but
 * will never affect previously started processes or the Java process
 * itself.
 *
 * <p>Most error checking is performed by the {@link #start()} method.
 * It is possible to modify the state of an object so that {@link
 * #start()} will fail.  For example, setting the command attribute to
 * an empty list will not throw an exception unless {@link #start()}
 * is invoked.
 *
 * <p><strong>Note that this class is not synchronized.</strong>
 * If multiple threads access a {@code ProcessBuilder} instance
 * concurrently, and at least one of the threads modifies one of the
 * attributes structurally, it <i>must</i> be synchronized externally.
 *
 * <p>Starting a new process which uses the default working directory
 * and environment is easy:
 *
 * <pre> {@code
 * Process p = new ProcessBuilder("myCommand", "myArg").start();
 * }</pre>
 *
 * <p>Here is an example that starts a process with a modified working
 * directory and environment, and redirects standard output and error
 * to be appended to a log file:
 *
 * <pre> {@code
 * ProcessBuilder pb =
 *   new ProcessBuilder("myCommand", "myArg1", "myArg2");
 * Map<String, String> env = pb.environment();
 * env.put("VAR1", "myValue");
 * env.remove("OTHERVAR");
 * env.put("VAR2", env.get("VAR1") + "suffix");
 * pb.directory(new File("myDir"));
 * File log = new File("log");
 * pb.redirectErrorStream(true);
 * pb.redirectOutput(Redirect.appendTo(log));
 * Process p = pb.start();
 * assert pb.redirectInput() == Redirect.PIPE;
 * assert pb.redirectOutput().file() == log;
 * assert p.getInputStream().read() == -1;
 * }</pre>
 *
 * <p>To start a process with an explicit set of environment
 * variables, first call {@link java.util.Map#clear() Map.clear()}
 * before adding environment variables.
 *
 * <p>
 * Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * @author Martin Buchholz
 * @since 1.5
 */
// 进程构造器
public final class ProcessBuilder {
    private List<String> command;               // 待执行命令列表
    private Map<String, String> environment;    // 环境变量
    private File directory;                     // 工作目录
    private Redirect[] redirects;               // 重定向信息，默认使用"Redirect.PIPE"
    
    private boolean redirectErrorStream;        // 是否将错误输出合并到了普通输出中
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs a process builder with the specified operating
     * system program and arguments.  This is a convenience
     * constructor that sets the process builder's command to a string
     * list containing the same strings as the {@code command}
     * array, in the same order.  It is not checked whether
     * {@code command} corresponds to a valid operating system
     * command.
     *
     * @param command a string array containing the program and its arguments
     */
    public ProcessBuilder(String... command) {
        this.command = new ArrayList<>(command.length);
        Collections.addAll(this.command, command);
    }
    
    /**
     * Constructs a process builder with the specified operating
     * system program and arguments.  This constructor does <i>not</i>
     * make a copy of the {@code command} list.  Subsequent
     * updates to the list will be reflected in the state of the
     * process builder.  It is not checked whether
     * {@code command} corresponds to a valid operating system
     * command.
     *
     * @param command the list containing the program and its arguments
     */
    public ProcessBuilder(List<String> command) {
        if(command == null) {
            throw new NullPointerException();
        }
        this.command = command;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 命令 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets this process builder's operating system program and
     * arguments.  This is a convenience method that sets the command
     * to a string list containing the same strings as the
     * {@code command} array, in the same order.  It is not
     * checked whether {@code command} corresponds to a valid
     * operating system command.
     *
     * @param command a string array containing the program and its arguments
     *
     * @return this process builder
     */
    /**
     * 为进程构造器设置待执行命令，
     * 如：command("myCommand", "myArg1", "myArg2");
     */
    public ProcessBuilder command(String... command) {
        this.command = new ArrayList<>(command.length);
        Collections.addAll(this.command, command);
        return this;
    }
    
    /**
     * Sets this process builder's operating system program and
     * arguments.  This method does <i>not</i> make a copy of the
     * {@code command} list.  Subsequent updates to the list will
     * be reflected in the state of the process builder.  It is not
     * checked whether {@code command} corresponds to a valid
     * operating system command.
     *
     * @param command the list containing the program and its arguments
     *
     * @return this process builder
     */
    /**
     * 为进程构造器设置待执行命令，
     * 如：
     */
    public ProcessBuilder command(List<String> command) {
        if(command == null) {
            throw new NullPointerException();
        }
        this.command = command;
        return this;
    }
    
    
    /**
     * Returns this process builder's operating system program and
     * arguments.  The returned list is <i>not</i> a copy.  Subsequent
     * updates to the list will be reflected in the state of this
     * process builder.
     *
     * @return this process builder's program and its arguments
     */
    // 返回进程构造器中的待执行命令列表
    public List<String> command() {
        return command;
    }
    
    /*▲ 命令 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 环境变量 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /** Only for use by Runtime.exec(...envp...) */
    /*
     * 为进程构造器设置环境变量
     *
     * 非public方法，目前仅用于Runtime#exec()方法
     */
    ProcessBuilder environment(String[] envp) {
        assert environment == null;
        
        if(envp != null) {
            // 构造指定容量的空Map以存放环境变量信息
            environment = ProcessEnvironment.emptyEnvironment(envp.length);
            
            assert environment != null;
            
            // 遍历参数中的环境变量
            for(String envstring : envp) {
                // Before 1.5, we blindly passed invalid envstrings to the child process.
                // We would like to throw an exception, but do not, for compatibility with old broken code.
                
                // Silently discard any trailing junk.
                if(envstring.indexOf('\u0000') != -1) {
                    envstring = envstring.replaceFirst("\u0000.*", "");
                }
                
                int eqlsign = envstring.indexOf('=', ProcessEnvironment.MIN_NAME_LENGTH);
                
                // Silently ignore envstrings lacking the required `='.
                if(eqlsign != -1) {
                    environment.put(envstring.substring(0, eqlsign), envstring.substring(eqlsign + 1));
                }
            }
        }
        
        return this;
    }
    
    
    /**
     * Returns a string map view of this process builder's environment.
     *
     * Whenever a process builder is created, the environment is
     * initialized to a copy of the current process environment (see
     * {@link System#getenv()}).  Subprocesses subsequently started by
     * this object's {@link #start()} method will use this map as
     * their environment.
     *
     * <p>The returned object may be modified using ordinary {@link
     * java.util.Map Map} operations.  These modifications will be
     * visible to subprocesses started via the {@link #start()}
     * method.  Two {@code ProcessBuilder} instances always
     * contain independent process environments, so changes to the
     * returned map will never be reflected in any other
     * {@code ProcessBuilder} instance or the values returned by
     * {@link System#getenv System.getenv}.
     *
     * <p>If the system does not support environment variables, an
     * empty map is returned.
     *
     * <p>The returned map does not permit null keys or values.
     * Attempting to insert or query the presence of a null key or
     * value will throw a {@link NullPointerException}.
     * Attempting to query the presence of a key or value which is not
     * of type {@link String} will throw a {@link ClassCastException}.
     *
     * <p>The behavior of the returned map is system-dependent.  A
     * system may not allow modifications to environment variables or
     * may forbid certain variable names or values.  For this reason,
     * attempts to modify the map may fail with
     * {@link UnsupportedOperationException} or
     * {@link IllegalArgumentException}
     * if the modification is not permitted by the operating system.
     *
     * <p>Since the external format of environment variable names and
     * values is system-dependent, there may not be a one-to-one
     * mapping between them and Java's Unicode strings.  Nevertheless,
     * the map is implemented in such a way that environment variables
     * which are not modified by Java code will have an unmodified
     * native representation in the subprocess.
     *
     * <p>The returned map and its collection views may not obey the
     * general contract of the {@link Object#equals} and
     * {@link Object#hashCode} methods.
     *
     * <p>The returned map is typically case-sensitive on all platforms.
     *
     * <p>If a security manager exists, its
     * {@link SecurityManager#checkPermission checkPermission} method
     * is called with a
     * {@link RuntimePermission}{@code ("getenv.*")} permission.
     * This may result in a {@link SecurityException} being thrown.
     *
     * <p>When passing information to a Java subprocess,
     * <a href=System.html#EnvironmentVSSystemProperties>system properties</a>
     * are generally preferred over environment variables.
     *
     * @return this process builder's environment
     *
     * @throws SecurityException if a security manager exists and its
     *                           {@link SecurityManager#checkPermission checkPermission}
     *                           method doesn't allow access to the process environment
     * @see Runtime#exec(String[], String[], java.io.File)
     * @see System#getenv()
     */
    /*
     * 返回进程构造器使用的环境变量列表。
     * 可能是外部设置的，也可能是系统现有的。
     */
    public Map<String, String> environment() {
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkPermission(new RuntimePermission("getenv.*"));
        }
        
        // 如果没有为进程构造器显式设置环境变量，则使用系统现有的环境变量
        if(environment == null) {
            // 获取系统现有环境变量列表(副本)
            environment = ProcessEnvironment.environment();
        }
        
        assert environment != null;
        
        return environment;
    }
    
    /*▲ 环境变量 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工作目录 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets this process builder's working directory.
     *
     * Subprocesses subsequently started by this object's {@link
     * #start()} method will use this as their working directory.
     * The argument may be {@code null} -- this means to use the
     * working directory of the current Java process, usually the
     * directory named by the system property {@code user.dir},
     * as the working directory of the child process.
     *
     * @param directory the new working directory
     *
     * @return this process builder
     */
    // 为进程构造器设定工作目录
    public ProcessBuilder directory(File directory) {
        this.directory = directory;
        return this;
    }
    
    /**
     * Returns this process builder's working directory.
     *
     * Subprocesses subsequently started by this object's {@link
     * #start()} method will use this as their working directory.
     * The returned value may be {@code null} -- this means to use
     * the working directory of the current Java process, usually the
     * directory named by the system property {@code user.dir},
     * as the working directory of the child process.
     *
     * @return this process builder's working directory
     */
    // 返回进程构造器使用的工作目录
    public File directory() {
        return directory;
    }
    
    /*▲ 工作目录 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 重定向 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Sets this process builder's standard input source to a file.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code redirectInput(file)}
     * behaves in exactly the same way as the invocation
     * {@link #redirectInput(Redirect) redirectInput}
     * {@code (Redirect.from(file))}.
     *
     * @param file the new standard input source
     *
     * @return this process builder
     *
     * @since 1.7
     */
    // 设置输入重定向为file，进程从file中获取输入
    public ProcessBuilder redirectInput(File file) {
        return redirectInput(Redirect.from(file));
    }
    
    /**
     * Sets this process builder's standard output destination to a file.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code redirectOutput(file)}
     * behaves in exactly the same way as the invocation
     * {@link #redirectOutput(Redirect) redirectOutput}
     * {@code (Redirect.to(file))}.
     *
     * @param file the new standard output destination
     *
     * @return this process builder
     *
     * @since 1.7
     */
    // 设置输出重定向为file，进程向file中写入普通数据
    public ProcessBuilder redirectOutput(File file) {
        return redirectOutput(Redirect.to(file));
    }
    
    /**
     * Sets this process builder's standard error destination to a file.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code redirectError(file)}
     * behaves in exactly the same way as the invocation
     * {@link #redirectError(Redirect) redirectError}
     * {@code (Redirect.to(file))}.
     *
     * @param file the new standard error destination
     *
     * @return this process builder
     *
     * @since 1.7
     */
    // 设置错误重定向为file，进程向file中写入错误数据
    public ProcessBuilder redirectError(File file) {
        return redirectError(Redirect.to(file));
    }
    
    
    /**
     * Sets this process builder's standard input source.
     *
     * Subprocesses subsequently started by this object's {@link #start()}
     * method obtain their standard input from this source.
     *
     * <p>If the source is {@link Redirect#PIPE Redirect.PIPE}
     * (the initial value), then the standard input of a
     * subprocess can be written to using the output stream
     * returned by {@link Process#getOutputStream()}.
     * If the source is set to any other value, then
     * {@link Process#getOutputStream()} will return a
     * <a href="#redirect-input">null output stream</a>.
     *
     * @param source the new standard input source
     *
     * @return this process builder
     *
     * @throws IllegalArgumentException if the redirect does not correspond to a valid source
     *                                  of data, that is, has type
     *                                  {@link Redirect.Type#WRITE WRITE} or
     *                                  {@link Redirect.Type#APPEND APPEND}
     * @since 1.7
     */
    // 设置输入重定向为source，进程从source中获取输入
    public ProcessBuilder redirectInput(Redirect source) {
        if(source.type() == Redirect.Type.WRITE || source.type() == Redirect.Type.APPEND) {
            throw new IllegalArgumentException("Redirect invalid for reading: " + source);
        }
    
        redirects()[0] = source;
    
        return this;
    }
    
    /**
     * Sets this process builder's standard output destination.
     *
     * Subprocesses subsequently started by this object's {@link #start()}
     * method send their standard output to this destination.
     *
     * <p>If the destination is {@link Redirect#PIPE Redirect.PIPE}
     * (the initial value), then the standard output of a subprocess
     * can be read using the input stream returned by {@link
     * Process#getInputStream()}.
     * If the destination is set to any other value, then
     * {@link Process#getInputStream()} will return a
     * <a href="#redirect-output">null input stream</a>.
     *
     * @param destination the new standard output destination
     *
     * @return this process builder
     *
     * @throws IllegalArgumentException if the redirect does not correspond to a valid
     *                                  destination of data, that is, has type
     *                                  {@link Redirect.Type#READ READ}
     * @since 1.7
     */
    // 设置输出重定向为destination，进程向destination中写入普通数据
    public ProcessBuilder redirectOutput(Redirect destination) {
        if(destination.type() == Redirect.Type.READ) {
            throw new IllegalArgumentException("Redirect invalid for writing: " + destination);
        }
    
        redirects()[1] = destination;
    
        return this;
    }
    
    /**
     * Sets this process builder's standard error destination.
     *
     * Subprocesses subsequently started by this object's {@link #start()}
     * method send their standard error to this destination.
     *
     * <p>If the destination is {@link Redirect#PIPE Redirect.PIPE}
     * (the initial value), then the error output of a subprocess
     * can be read using the input stream returned by {@link
     * Process#getErrorStream()}.
     * If the destination is set to any other value, then
     * {@link Process#getErrorStream()} will return a
     * <a href="#redirect-output">null input stream</a>.
     *
     * <p>If the {@link #redirectErrorStream() redirectErrorStream}
     * attribute has been set {@code true}, then the redirection set
     * by this method has no effect.
     *
     * @param destination the new standard error destination
     *
     * @return this process builder
     *
     * @throws IllegalArgumentException if the redirect does not correspond to a valid
     *                                  destination of data, that is, has type
     *                                  {@link Redirect.Type#READ READ}
     * @since 1.7
     */
    // 设置错误重定向为destination，进程向destination中写入错误数据
    public ProcessBuilder redirectError(Redirect destination) {
        if(destination.type() == Redirect.Type.READ) {
            throw new IllegalArgumentException("Redirect invalid for writing: " + destination);
        }
    
        redirects()[2] = destination;
    
        return this;
    }
    
    
    /**
     * Returns this process builder's standard input source.
     *
     * Subprocesses subsequently started by this object's {@link #start()}
     * method obtain their standard input from this source.
     * The initial value is {@link Redirect#PIPE Redirect.PIPE}.
     *
     * @return this process builder's standard input source
     *
     * @since 1.7
     */
    // 返回输入重定向信息
    public Redirect redirectInput() {
        return (redirects == null) ? Redirect.PIPE : redirects[0];
    }
    
    /**
     * Returns this process builder's standard output destination.
     *
     * Subprocesses subsequently started by this object's {@link #start()}
     * method redirect their standard output to this destination.
     * The initial value is {@link Redirect#PIPE Redirect.PIPE}.
     *
     * @return this process builder's standard output destination
     *
     * @since 1.7
     */
    // 返回输出重定向信息
    public Redirect redirectOutput() {
        return (redirects == null) ? Redirect.PIPE : redirects[1];
    }
    
    /**
     * Returns this process builder's standard error destination.
     *
     * Subprocesses subsequently started by this object's {@link #start()}
     * method redirect their standard error to this destination.
     * The initial value is {@link Redirect#PIPE Redirect.PIPE}.
     *
     * @return this process builder's standard error destination
     *
     * @since 1.7
     */
    // 返回错误重定向信息
    public Redirect redirectError() {
        return (redirects == null) ? Redirect.PIPE : redirects[2];
    }
    
    /*▲ 重定向 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 启动 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Starts a new process using the attributes of this process builder.
     *
     * <p>The new process will
     * invoke the command and arguments given by {@link #command()},
     * in a working directory as given by {@link #directory()},
     * with a process environment as given by {@link #environment()}.
     *
     * <p>This method checks that the command is a valid operating
     * system command.  Which commands are valid is system-dependent,
     * but at the very least the command must be a non-empty list of
     * non-null strings.
     *
     * <p>A minimal set of system dependent environment variables may
     * be required to start a process on some operating systems.
     * As a result, the subprocess may inherit additional environment variable
     * settings beyond those in the process builder's {@link #environment()}.
     *
     * <p>If there is a security manager, its
     * {@link SecurityManager#checkExec checkExec}
     * method is called with the first component of this object's
     * {@code command} array as its argument. This may result in
     * a {@link SecurityException} being thrown.
     *
     * <p>Starting an operating system process is highly system-dependent.
     * Among the many things that can go wrong are:
     * <ul>
     * <li>The operating system program file was not found.
     * <li>Access to the program file was denied.
     * <li>The working directory does not exist.
     * <li>Invalid character in command argument, such as NUL.
     * </ul>
     *
     * <p>In such cases an exception will be thrown.  The exact nature
     * of the exception is system-dependent, but it will always be a
     * subclass of {@link IOException}.
     *
     * <p>If the operating system does not support the creation of
     * processes, an {@link UnsupportedOperationException} will be thrown.
     *
     * <p>Subsequent modifications to this process builder will not
     * affect the returned {@link Process}.
     *
     * @return a new {@link Process} object for managing the subprocess
     *
     * @throws NullPointerException          if an element of the command list is null
     * @throws IndexOutOfBoundsException     if the command is an empty list (has size {@code 0})
     * @throws SecurityException             if a security manager exists and
     *                                       <ul>
     *
     *                                       <li>its
     *                                       {@link SecurityManager#checkExec checkExec}
     *                                       method doesn't allow creation of the subprocess, or
     *
     *                                       <li>the standard input to the subprocess was
     *                                       {@linkplain #redirectInput redirected from a file}
     *                                       and the security manager's
     *                                       {@link SecurityManager#checkRead(String) checkRead} method
     *                                       denies read access to the file, or
     *
     *                                       <li>the standard output or standard error of the
     *                                       subprocess was
     *                                       {@linkplain #redirectOutput redirected to a file}
     *                                       and the security manager's
     *                                       {@link SecurityManager#checkWrite(String) checkWrite} method
     *                                       denies write access to the file
     *
     *                                       </ul>
     * @throws UnsupportedOperationException If the operating system does not support the creation of processes.
     * @throws IOException                   if an I/O error occurs
     * @see Runtime#exec(String[], String[], java.io.File)
     */
    // 启动当前构造的进程
    public Process start() throws IOException {
        return start(redirects);
    }
    
    /**
     * Starts a Process for each ProcessBuilder, creating a pipeline of
     * processes linked by their standard output and standard input streams.
     * The attributes of each ProcessBuilder are used to start the respective
     * process except that as each process is started, its standard output
     * is directed to the standard input of the next.  The redirects for standard
     * input of the first process and standard output of the last process are
     * initialized using the redirect settings of the respective ProcessBuilder.
     * All other {@code ProcessBuilder} redirects should be
     * {@link Redirect#PIPE Redirect.PIPE}.
     * <p>
     * All input and output streams between the intermediate processes are
     * not accessible.
     * The {@link Process#getOutputStream standard input} of all processes
     * except the first process are <i>null output streams</i>
     * The {@link Process#getInputStream standard output} of all processes
     * except the last process are <i>null input streams</i>.
     * <p>
     * The {@link #redirectErrorStream()} of each ProcessBuilder applies to the
     * respective process.  If set to {@code true}, the error stream is written
     * to the same stream as standard output.
     * <p>
     * If starting any of the processes throws an Exception, all processes
     * are forcibly destroyed.
     * <p>
     * The {@code startPipeline} method performs the same checks on
     * each ProcessBuilder as does the {@link #start} method. The new process
     * will invoke the command and arguments given by {@link #command()},
     * in a working directory as given by {@link #directory()},
     * with a process environment as given by {@link #environment()}.
     * <p>
     * This method checks that the command is a valid operating
     * system command.  Which commands are valid is system-dependent,
     * but at the very least the command must be a non-empty list of
     * non-null strings.
     * <p>
     * A minimal set of system dependent environment variables may
     * be required to start a process on some operating systems.
     * As a result, the subprocess may inherit additional environment variable
     * settings beyond those in the process builder's {@link #environment()}.
     * <p>
     * If there is a security manager, its
     * {@link SecurityManager#checkExec checkExec}
     * method is called with the first component of this object's
     * {@code command} array as its argument. This may result in
     * a {@link SecurityException} being thrown.
     * <p>
     * Starting an operating system process is highly system-dependent.
     * Among the many things that can go wrong are:
     * <ul>
     * <li>The operating system program file was not found.
     * <li>Access to the program file was denied.
     * <li>The working directory does not exist.
     * <li>Invalid character in command argument, such as NUL.
     * </ul>
     * <p>
     * In such cases an exception will be thrown.  The exact nature
     * of the exception is system-dependent, but it will always be a
     * subclass of {@link IOException}.
     * <p>
     * If the operating system does not support the creation of
     * processes, an {@link UnsupportedOperationException} will be thrown.
     * <p>
     * Subsequent modifications to this process builder will not
     * affect the returned {@link Process}.
     *
     * @param builders a List of ProcessBuilders
     *
     * @return a {@code List<Process>}es started from the corresponding
     * ProcessBuilder
     *
     * @throws IllegalArgumentException      any of the redirects except the
     *                                       standard input of the first builder and the standard output of
     *                                       the last builder are not {@link Redirect#PIPE}.
     * @throws NullPointerException          if an element of the command list is null or
     *                                       if an element of the ProcessBuilder list is null or
     *                                       the builders argument is null
     * @throws IndexOutOfBoundsException     if the command is an empty list (has size {@code 0})
     * @throws SecurityException             if a security manager exists and
     *                                       <ul>
     *                                       <li>its
     *                                       {@link SecurityManager#checkExec checkExec}
     *                                       method doesn't allow creation of the subprocess, or
     *                                       <li>the standard input to the subprocess was
     *                                       {@linkplain #redirectInput redirected from a file}
     *                                       and the security manager's
     *                                       {@link SecurityManager#checkRead(String) checkRead} method
     *                                       denies read access to the file, or
     *                                       <li>the standard output or standard error of the
     *                                       subprocess was
     *                                       {@linkplain #redirectOutput redirected to a file}
     *                                       and the security manager's
     *                                       {@link SecurityManager#checkWrite(String) checkWrite} method
     *                                       denies write access to the file
     *                                       </ul>
     * @throws UnsupportedOperationException If the operating system does not support the creation of processes
     * @throws IOException                   if an I/O error occurs
     * @apiNote For example to count the unique imports for all the files in a file hierarchy
     * on a Unix compatible platform:
     * <pre>{@code
     * String directory = "/home/duke/src";
     * ProcessBuilder[] builders = {
     *              new ProcessBuilder("find", directory, "-type", "f"),
     * new ProcessBuilder("xargs", "grep", "-h", "^import "),
     * new ProcessBuilder("awk", "{print $2;}"),
     * new ProcessBuilder("sort", "-u")};
     * List<Process> processes = ProcessBuilder.startPipeline(
     *         Arrays.asList(builders));
     * Process last = processes.get(processes.size()-1);
     * try (InputStream is = last.getInputStream();
     *         Reader isr = new InputStreamReader(is);
     *         BufferedReader r = new BufferedReader(isr)) {
     *     long count = r.lines().count();
     * }
     * }</pre>
     * @since 9
     */
    // 通过指定的进程构造器来构造一系列串联的进程，并启动它们
    public static List<Process> startPipeline(List<ProcessBuilder> builders) throws IOException {
        // Accumulate and check the builders
        final int numBuilders = builders.size();
    
        List<Process> processes = new ArrayList<>(numBuilders);
    
        try {
            Redirect prevOutput = null;
        
            // 遍历所有进程构造器
            for(int index = 0; index<builders.size(); index++) {
                ProcessBuilder builder = builders.get(index);
            
                Redirect[] redirects = builder.redirects();
            
                // 非表头的进程
                if(index>0) {
                    // check the current Builder to see if it can take input from the previous
                    if(builder.redirectInput() != Redirect.PIPE) {
                        throw new IllegalArgumentException("builder redirectInput()" + " must be PIPE except for the first builder: " + builder.redirectInput());
                    }
                
                    // 非表头进程的输入是前驱的输出
                    redirects[0] = prevOutput;
                }
            
                // 非表尾的进程
                if(index<numBuilders - 1) {
                    // check all but the last stage has output = PIPE
                    if(builder.redirectOutput() != Redirect.PIPE) {
                        throw new IllegalArgumentException("builder redirectOutput()" + " must be PIPE except for the last builder: " + builder.redirectOutput());
                    }
                
                    // 非表尾进程的输出是后继的输入：将输出设置为一个特殊的"管道"重定向，用作占位
                    redirects[1] = new RedirectPipeImpl();  // placeholder for new output
                }
            
                // 构造并启动进程
                Process process = builder.start(redirects);
            
                // 记录该进程
                processes.add(process);
            
                // 记录当前进程的输出流，以便作为下一个进程的输入流
                prevOutput = redirects[1];
            }
        } catch(Exception ex) {
            // Cleanup processes already started
            processes.forEach(Process::destroyForcibly);
        
            processes.forEach(p -> {
                try {
                    p.waitFor();    // Wait for it to exit
                } catch(InterruptedException ie) {
                    // If interrupted; continue with next Process
                    Thread.currentThread().interrupt();
                }
            });
            throw ex;
        }
    
        return processes;
    }
    
    /*▲ 启动 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Tells whether this process builder merges standard error and standard output.
     *
     * <p>If this property is {@code true}, then any error output
     * generated by subprocesses subsequently started by this object's
     * {@link #start()} method will be merged with the standard
     * output, so that both can be read using the
     * {@link Process#getInputStream()} method.  This makes it easier
     * to correlate error messages with the corresponding output.
     * The initial value is {@code false}.
     *
     * @return this process builder's {@code redirectErrorStream} property
     */
    // 判断是否将错误输出合并到了普通输出中
    public boolean redirectErrorStream() {
        return redirectErrorStream;
    }
    
    /**
     * Sets this process builder's {@code redirectErrorStream} property.
     *
     * <p>If this property is {@code true}, then any error output
     * generated by subprocesses subsequently started by this object's
     * {@link #start()} method will be merged with the standard
     * output, so that both can be read using the
     * {@link Process#getInputStream()} method.  This makes it easier
     * to correlate error messages with the corresponding output.
     * The initial value is {@code false}.
     *
     * @param redirectErrorStream the new property value
     *
     * @return this process builder
     */
    // 设置是否将错误输出合并到普通输出中
    public ProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        return this;
    }
    
    /**
     * Sets the source and destination for subprocess standard I/O
     * to be the same as those of the current Java process.
     *
     * <p>This is a convenience method.  An invocation of the form
     * <pre> {@code
     * pb.inheritIO()
     * }</pre>
     * behaves in exactly the same way as the invocation
     * <pre> {@code
     * pb.redirectInput(Redirect.INHERIT)
     *   .redirectOutput(Redirect.INHERIT)
     *   .redirectError(Redirect.INHERIT)
     * }</pre>
     *
     * This gives behavior equivalent to most operating system
     * command interpreters, or the standard C library function
     * {@code system()}.
     *
     * @return this process builder
     *
     * @since 1.7
     */
    // 将当前进程的重定向信息全部填充为Redirect.INHERIT
    public ProcessBuilder inheritIO() {
        Arrays.fill(redirects(), Redirect.INHERIT);
        return this;
    }
    
    
    /**
     * Return the array of redirects, creating the default as needed.
     *
     * @return the array of redirects
     */
    // 返回当前进程的重定向信息
    private Redirect[] redirects() {
        if(redirects == null) {
            redirects = new Redirect[]{Redirect.PIPE, Redirect.PIPE, Redirect.PIPE};
        }
        
        return redirects;
    }
    
    /**
     * Start a new Process using an explicit array of redirects.
     * See {@link #start} for details of starting each Process.
     *
     * @param redirect array of redirects for stdin, stdout, stderr
     *
     * @return the new Process
     *
     * @throws IOException if an I/O error occurs
     */
    // 构造并启动进程
    private Process start(Redirect[] redirects) throws IOException {
        // Must convert to array first -- a malicious user-supplied list might try to circumvent the security check.
        String[] cmdarray = command.toArray(new String[0]).clone();
        
        // 遍历命令行参数，确保没有null参数
        for(String arg : cmdarray) {
            if(arg == null) {
                throw new NullPointerException();
            }
        }
        
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkExec(cmdarray[0]);
        }
        
        // 工作目录
        String dir = directory == null ? null : directory.toString();
        
        // 遍历命令行参数，确保没有"\0"参数
        for(int i = 1; i<cmdarray.length; i++) {
            if(cmdarray[i].indexOf('\u0000') >= 0) {
                throw new IOException("invalid null character in command");
            }
        }
        
        try {
            return ProcessImpl.start(cmdarray, environment, dir, redirects, redirectErrorStream);
        } catch(IOException | IllegalArgumentException e) {
            String exceptionInfo = ": " + e.getMessage();
            Throwable cause = e;
            if((e instanceof IOException) && security != null) {
                // Can not disclose the fail reason for read-protected files.
                try {
                    security.checkRead(cmdarray[0]);
                } catch(SecurityException se) {
                    exceptionInfo = "";
                    cause = se;
                }
            }
            
            // It's much easier for us to create a high-quality error message than the low-level C code which found the problem.
            throw new IOException("Cannot run program \"" + cmdarray[0] + "\"" + (dir == null ? "" : " (in directory \"" + dir + "\")") + exceptionInfo, cause);
        }
    }
    
    
    /**
     * Represents a source of subprocess input or a destination of
     * subprocess output.
     *
     * Each {@code Redirect} instance is one of the following:
     *
     * <ul>
     * <li>the special value {@link #PIPE Redirect.PIPE}
     * <li>the special value {@link #INHERIT Redirect.INHERIT}
     * <li>the special value {@link #DISCARD Redirect.DISCARD}
     * <li>a redirection to read from a file, created by an invocation of
     *     {@link Redirect#from Redirect.from(File)}
     * <li>a redirection to write to a file,  created by an invocation of
     *     {@link Redirect#to Redirect.to(File)}
     * <li>a redirection to append to a file, created by an invocation of
     *     {@link Redirect#appendTo Redirect.appendTo(File)}
     * </ul>
     *
     * <p>Each of the above categories has an associated unique
     * {@link Type Type}.
     *
     * @since 1.7
     */
    // 进程的重定向信息
    public abstract static class Redirect {
        
        // 特定的空文件，用来存储丢弃的输出
        private static final File NULL_FILE = new File((GetPropertyAction.privilegedGetProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null"));
        
        
        /**
         * Indicates that subprocess I/O will be connected to the current Java process over a pipe.
         *
         * This is the default handling of subprocess standard I/O.
         *
         * <p>It will always be true that
         * <pre> {@code
         * Redirect.PIPE.file() == null &&
         * Redirect.PIPE.type() == Redirect.Type.PIPE
         * }</pre>
         */
        // "管道"重定向，指示子进程的IO将通过管道连接到Java进程
        public static final Redirect PIPE = new Redirect() {
            public Type type() {
                return Type.PIPE;
            }
            
            public String toString() {
                return type().toString();
            }
        };
        
        /**
         * Indicates that subprocess I/O source or destination will be the same as those of the current process.
         * This is the normal behavior of most operating system command interpreters (shells).
         *
         * <p>It will always be true that
         * <pre> {@code
         * Redirect.INHERIT.file() == null &&
         * Redirect.INHERIT.type() == Redirect.Type.INHERIT
         * }</pre>
         */
        // "继承"重定向，指示子进程的IO与当前进程的IO一致
        public static final Redirect INHERIT = new Redirect() {
            public Type type() {
                return Type.INHERIT;
            }
            
            public String toString() {
                return type().toString();
            }
        };
        
        /**
         * Indicates that subprocess output will be discarded.
         * A typical implementation discards the output by writing to an operating system specific "null file".
         *
         * <p>It will always be true that
         * <pre> {@code
         * Redirect.DISCARD.file() is the filename appropriate for the operating system
         * and may be null &&
         * Redirect.DISCARD.type() == Redirect.Type.WRITE
         * }</pre>
         *
         * @since 9
         */
        // "写"重定向，指示子进程的输出将被丢弃，典型的实现是通过写入特定于操作系统的“空文件”来丢弃输出
        public static final Redirect DISCARD = new Redirect() {
            public Type type() {
                return Type.WRITE;
            }
            
            public File file() {
                return NULL_FILE;
            }
            
            boolean append() {
                return false;
            }
            
            public String toString() {
                return type().toString();
            }
            
        };
        
        
        /**
         * Returns a redirect to read from the specified file.
         *
         * <p>It will always be true that
         * <pre> {@code
         * Redirect.from(file).file() == file &&
         * Redirect.from(file).type() == Redirect.Type.READ
         * }</pre>
         *
         * @param file The {@code File} for the {@code Redirect}.
         *
         * @return a redirect to read from the specified file
         */
        // "读"重定向，进程可从此处读取数据
        public static Redirect from(final File file) {
            if(file == null) {
                throw new NullPointerException();
            }
            
            return new Redirect() {
                public Type type() {
                    return Type.READ;
                }
                
                public File file() {
                    return file;
                }
                
                public String toString() {
                    return "redirect to read from file \"" + file + "\"";
                }
            };
        }
        
        /**
         * Returns a redirect to write to the specified file.
         * If the specified file exists when the subprocess is started,
         * its previous contents will be discarded.
         *
         * <p>It will always be true that
         * <pre> {@code
         * Redirect.to(file).file() == file &&
         * Redirect.to(file).type() == Redirect.Type.WRITE
         * }</pre>
         *
         * @param file The {@code File} for the {@code Redirect}.
         *
         * @return a redirect to write to the specified file
         */
        // "写"重定向，进程可向此处写入数据
        public static Redirect to(final File file) {
            if(file == null) {
                throw new NullPointerException();
            }
            
            return new Redirect() {
                public Type type() {
                    return Type.WRITE;
                }
                
                public File file() {
                    return file;
                }
                
                boolean append() {
                    return false;
                }
                
                public String toString() {
                    return "redirect to write to file \"" + file + "\"";
                }
            };
        }
        
        /**
         * Returns a redirect to append to the specified file.
         * Each write operation first advances the position to the
         * end of the file and then writes the requested data.
         * Whether the advancement of the position and the writing
         * of the data are done in a single atomic operation is
         * system-dependent and therefore unspecified.
         *
         * <p>It will always be true that
         * <pre> {@code
         * Redirect.appendTo(file).file() == file &&
         * Redirect.appendTo(file).type() == Redirect.Type.APPEND
         * }</pre>
         *
         * @param file The {@code File} for the {@code Redirect}.
         *
         * @return a redirect to append to the specified file
         */
        // "追加"重定向，进程可向此处写入(追加)数据
        public static Redirect appendTo(final File file) {
            if(file == null) {
                throw new NullPointerException();
            }
            
            return new Redirect() {
                public Type type() {
                    return Type.APPEND;
                }
                
                public File file() {
                    return file;
                }
                
                boolean append() {
                    return true;
                }
                
                public String toString() {
                    return "redirect to append to file \"" + file + "\"";
                }
                
            };
        }
        
        
        /**
         * No public constructors.  Clients must use predefined
         * static {@code Redirect} instances or factory methods.
         */
        private Redirect() {
        }
        
        /**
         * Returns the type of this {@code Redirect}.
         *
         * @return the type of this {@code Redirect}
         */
        public abstract Type type();
        
        /**
         * Returns the {@link File} source or destination associated
         * with this redirect, or {@code null} if there is no such file.
         *
         * @return the file associated with this redirect,
         * or {@code null} if there is no such file
         */
        public File file() {
            return null;
        }
        
        /**
         * When redirected to a destination file, indicates if the output
         * is to be written to the end of the file.
         */
        boolean append() {
            throw new UnsupportedOperationException();
        }
        
        /**
         * Compares the specified object with this {@code Redirect} for
         * equality.  Returns {@code true} if and only if the two
         * objects are identical or both objects are {@code Redirect}
         * instances of the same type associated with non-null equal
         * {@code File} instances.
         */
        public boolean equals(Object obj) {
            if(obj == this) {
                return true;
            }
            
            if(!(obj instanceof Redirect)) {
                return false;
            }
            
            Redirect r = (Redirect) obj;
            if(r.type() != this.type()) {
                return false;
            }
            
            assert this.file() != null;
            
            return this.file().equals(r.file());
        }
        
        /**
         * Returns a hash code value for this {@code Redirect}.
         *
         * @return a hash code value for this {@code Redirect}
         */
        public int hashCode() {
            File file = file();
            if(file == null) {
                return super.hashCode();
            } else {
                return file.hashCode();
            }
        }
        
        /**
         * The type of a {@link Redirect}.
         */
        public enum Type {
            /**
             * The type of {@link Redirect#PIPE Redirect.PIPE}.
             */
            PIPE,
            
            /**
             * The type of {@link Redirect#INHERIT Redirect.INHERIT}.
             */
            INHERIT,
            
            /**
             * The type of redirects returned from {@link Redirect#from Redirect.from(File)}.
             */
            READ,
            
            /**
             * The type of redirects returned from {@link Redirect#to Redirect.to(File)}.
             */
            WRITE,
            
            /**
             * The type of redirects returned from {@link Redirect#appendTo Redirect.appendTo(File)}.
             */
            APPEND
        }
    }
    
    /**
     * Private implementation subclass of Redirect that holds a FileDescriptor for the output of a previously started Process.
     * The FileDescriptor is used as the standard input of the next Process to be started.
     */
    // 特殊的"管道"重定向，这里的文件描述符是空的，后续会为其关联JVM内返回的句柄
    static class RedirectPipeImpl extends Redirect {
        final FileDescriptor fd;
        
        RedirectPipeImpl() {
            this.fd = new FileDescriptor();
        }
        
        @Override
        public Type type() {
            return Type.PIPE;
        }
        
        FileDescriptor getFd() {
            return fd;
        }
        
        @Override
        public String toString() {
            return type().toString();
        }
        
    }
    
    /**
     * Implements a <a href="#redirect-output">null input stream</a>.
     */
    // 无法读取的输入流
    static class NullInputStream extends InputStream {
        static final NullInputStream INSTANCE = new NullInputStream();
        
        private NullInputStream() {
        }
        
        public int read() {
            return -1;
        }
        
        public int available() {
            return 0;
        }
    }
    
    /**
     * Implements a <a href="#redirect-input">null output stream</a>.
     */
    // 无法写入的输出流
    static class NullOutputStream extends OutputStream {
        static final NullOutputStream INSTANCE = new NullOutputStream();
        
        private NullOutputStream() {
        }
        
        public void write(int b) throws IOException {
            throw new IOException("Stream closed");
        }
    }
    
}
