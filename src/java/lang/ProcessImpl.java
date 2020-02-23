/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.ref.CleanerFactory;

/**
 * This class is for the exclusive use of ProcessBuilder.start() to create new processes.
 *
 * @author Martin Buchholz
 * @since 1.5
 */
// 进程实现类
final class ProcessImpl extends Process {
    
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();
    
    // Windows platforms support a forcible kill signal.
    static final boolean SUPPORTS_NORMAL_TERMINATION = false;   // windows平台支持强制结束进程
    
    private static final int VERIFICATION_CMD_BAT = 0;
    private static final int VERIFICATION_WIN32 = 1;
    private static final int VERIFICATION_LEGACY = 2;
    
    /**
     * We guarantee the only command file execution for implicit [cmd.exe] run.
     * http://technet.microsoft.com/en-us/library/bb490954.aspx
     */
    private static final char[][] ESCAPE_VERIFICATION = {{' ', '\t', '<', '>', '&', '|', '^'}, {' ', '\t', '<', '>'}, {' ', '\t'}};
    
    private static final int STILL_ACTIVE = getStillActive();   // 进程存活的状态码
    
    private final long handle;  // 指向本地子进程的句柄
    
    private final ProcessHandle processHandle;  // Java层的进程句柄
    
    private OutputStream stdin_stream;  // 链接到进程输入的输出流，可以向此写入进程想要的输入
    private InputStream stdout_stream;  // 链接到进程输出的输入流，可以从此读取进程正常输出的内容
    private InputStream stderr_stream;  // 链接到进程错误的输入流，可以从此读取进程异常输出的内容
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 构造一个进程，并启动它
    private ProcessImpl(String[] cmd, final String envblock, final String path, final long[] stdHandles, final boolean redirectErrorStream) throws IOException {
        
        boolean allowAmbiguousCommands = false;
        
        SecurityManager security = System.getSecurityManager();
        if(security == null) {
            allowAmbiguousCommands = true;
            
            String value = System.getProperty("jdk.lang.Process.allowAmbiguousCommands");
            if(value != null) {
                allowAmbiguousCommands = !"false".equalsIgnoreCase(value);
            }
        }
        
        // 命令行字符串
        String cmdstr;
        
        if(allowAmbiguousCommands) {
            /* Legacy mode */
            
            /* Normalize path if possible */
            String executablePath = new File(cmd[0]).getPath();
            
            /* No worry about internal, unpaired ["], and redirection/piping */
            if(needsEscaping(VERIFICATION_LEGACY, executablePath)) {
                // 将executablePath用双引号扩起来
                executablePath = quoteString(executablePath);
            }
            
            /* legacy mode doesn't worry about extended verification */
            // 创建命令行字符串
            cmdstr = createCommandLine(VERIFICATION_LEGACY, executablePath, cmd);
        } else {
            // 可执行文件路径
            String executablePath;
            
            try {
                // 获取可执行文件路径
                executablePath = getExecutablePath(cmd[0]);
            } catch(IllegalArgumentException e) {
                // Workaround for the calls like
                // Runtime.getRuntime().exec("\"C:\\Program Files\\foo\" bar")
                
                // No chance to avoid CMD/BAT injection, except to do the work
                // right from the beginning. Otherwise we have too many corner
                // cases from
                //    Runtime.getRuntime().exec(String[] cmd [, ...])
                // calls with internal ["] and escape sequences.
                
                // Restore original command line.
                StringBuilder join = new StringBuilder();
                
                // terminal space in command line is ok
                for(String s : cmd) {
                    join.append(s).append(' ');
                }
                
                // 将命令字符串参数解析为可执行文件名称和程序参数
                cmd = getTokensFromCommand(join.toString());    /* Parse the command line again */
                
                // 获取可执行文件路径
                executablePath = getExecutablePath(cmd[0]);
                
                /* Check new executable name once more */
                if(security != null) {
                    security.checkExec(executablePath);
                }
            }
            
            /* We need the extended verification procedure for CMD files. */
            int verificationType = isShellFile(executablePath)  // 判断待执行对象是否为shell文件(以cmd或bat结尾)
                ? VERIFICATION_CMD_BAT : VERIFICATION_WIN32;
            
            /*
             * Quotation protects from interpretation of the [path] argument as start of longer path with spaces.
             * Quotation has no influence to [.exe] extension heuristic.
             */
            // 将executablePath用双引号扩起来
            executablePath = quoteString(executablePath);
            
            // 创建命令行字符串
            cmdstr = createCommandLine(verificationType, executablePath, cmd);
        }
        
        // 创建并执行新的进程，返回本地子进程句柄
        handle = create(cmdstr, envblock, path, stdHandles, redirectErrorStream);
        
        /* Register a cleaning function to close the handle */
        CleanerFactory.cleaner().register(this, () -> closeHandle(handle)); // 注册清理器，以便后续关闭指定句柄处的进程
        
        // 获取进程号(pid)
        int pid = getProcessId0(handle);
        
        // 构造进程号为pid的进程句柄并返回
        processHandle = ProcessHandleImpl.getInternal(pid);
        
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                if(stdHandles[0] == -1L) {
                    stdin_stream = ProcessBuilder.NullOutputStream.INSTANCE;
                } else {
                    FileDescriptor stdin_fd = new FileDescriptor();
                    fdAccess.setHandle(stdin_fd, stdHandles[0]);
                    stdin_stream = new BufferedOutputStream(new FileOutputStream(stdin_fd));
                }
                
                if(stdHandles[1] == -1L) {
                    stdout_stream = ProcessBuilder.NullInputStream.INSTANCE;
                } else {
                    FileDescriptor stdout_fd = new FileDescriptor();
                    fdAccess.setHandle(stdout_fd, stdHandles[1]);
                    stdout_stream = new BufferedInputStream(new PipeInputStream(stdout_fd));
                }
                
                if(stdHandles[2] == -1L) {
                    stderr_stream = ProcessBuilder.NullInputStream.INSTANCE;
                } else {
                    FileDescriptor stderr_fd = new FileDescriptor();
                    fdAccess.setHandle(stderr_fd, stdHandles[2]);
                    stderr_stream = new PipeInputStream(stderr_fd);
                }
                
                return null;
            }
        });
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 输入/输出 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回与进程输入端关联的输出流，可以由此向进程写入输入数据
    public OutputStream getOutputStream() {
        return stdin_stream;
    }
    
    // 返回与进程输出端关联的输入流，可以由此从进程读取输出数据
    public InputStream getInputStream() {
        return stdout_stream;
    }
    
    // 返回与进程错误输出端关联的输入流，可以由此从进程读取错误日志
    public InputStream getErrorStream() {
        return stderr_stream;
    }
    
    /*▲ 输入/输出 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 等待 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /*
     * 【同步】等待进程结束，在结束之前一直阻塞
     *
     * 使waitFor()所处线程进入阻塞，直到当前进程结束后，再切换到waitFor()所处线程。
     * 进程结束后，会返回其退出的状态码。一般来说，正常退出时返回0。
     * 如果线程发生中断，则抛出中断异常。
     */
    public int waitFor() throws InterruptedException {
        // 等待指定的进程执行完成后返回
        waitForInterruptibly(handle);
        
        // 测试当前线程是否已经中断，线程的中断状态会被清
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        
        // 返回进程退出的状态码，如果进程还未结束，则会抛异常
        return exitValue();
    }
    
    /*
     * 【同步】等待进程结束，超时后抛异常
     *
     * 使waitFor()所处线程进入阻塞，直到当前进程结束后，或者直到超时后，再切换到waitFor()所处线程。
     * 进程结束后，会返回其退出的状态码。一般来说，正常退出时返回0。
     * 子类应覆盖此实现。
     */
    @Override
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        long remainingNanos = unit.toNanos(timeout);    // throw NPE before other conditions
        
        if(getExitCodeProcess(handle) != STILL_ACTIVE) {
            return true;
        }
        
        if(timeout<=0) {
            return false;
        }
        
        long deadline = System.nanoTime() + remainingNanos;
        
        do {
            // Round up to next millisecond
            // 将指定的纳秒换算为毫秒
            long msTimeout = TimeUnit.NANOSECONDS.toMillis(remainingNanos + 999_999L);
            
            // 等待指定的进程执行完成后返回，或者超时后返回
            waitForTimeoutInterruptibly(handle, msTimeout);
            
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            
            if(getExitCodeProcess(handle) != STILL_ACTIVE) {
                return true;
            }
            
            remainingNanos = deadline - System.nanoTime();
            
            // 如果已经超时，则直接退出
        } while(remainingNanos>0);
        
        return (getExitCodeProcess(handle) != STILL_ACTIVE);
    }
    
    /*
     * 返回一个阶段：该阶段的执行结果是当前进程退出时的状态码(如果没执行完，则会等待它执行完)；
     * 该方法可以看做是【异步】等待进程结束的一种手段。
     *
     * 如果等待进程结束的过程中抛出了中断异常，则会为执行该阶段任务的线程设置中断标记。
     */
    @Override
    public CompletableFuture<Process> onExit() {
        // 获取当前进程的进程号
        long pid = pid();
        
        // 返回一个任务阶段，该阶段完成时将以进程pid的退出状态作为完成标记
        CompletableFuture<Integer> future = ProcessHandleImpl.completion(pid, false);
        
        /*
         * 异步执行BiFunction任务，返回该任务所属阶段(可从中获取执行结果)
         *
         * 该任务【需要】等待上游任务future执行完，并使用该上游的执行结果和抛出的异常作为入参
         *
         * 这里的BiFunction任务只是简单地返回了future自身，没有做后续操作
         */
        return future.handleAsync((Integer exitStatus, Throwable unusedThrowable) -> this);
    }
    
    /*▲ 等待 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 终止 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 终止指定的进程，退出状态码为1
    @Override
    public void destroy() {
        terminateProcess(handle);
    }
    
    // 强制终止进程
    @Override
    public Process destroyForcibly() {
        destroy();
        return this;
    }
    
    // 返回当前平台对结束进程的支持状况；返回true表示支持正常终止，返回false表示支持强制终止
    @Override
    public boolean supportsNormalTermination() {
        return ProcessImpl.SUPPORTS_NORMAL_TERMINATION;
    }
    
    /*▲ 终止 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态/属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回进程号
    @Override
    public long pid() {
        return processHandle.pid();
    }
    
    // 判断当前进程是否处于活动状态
    @Override
    public boolean isAlive() {
        return isProcessAlive(handle);
    }
    
    // 返回进程退出的状态码；通常来说，状态码为零表示成功退出，状态码不为零表示执行中发生了状况
    public int exitValue() {
        int exitCode = getExitCodeProcess(handle);
        
        // 如果进程依然存活，则抛出异常
        if(exitCode == STILL_ACTIVE) {
            throw new IllegalThreadStateException("process has not exited");
        }
        
        return exitCode;
    }
    
    // 返回进程句柄
    @Override
    public ProcessHandle toHandle() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        
        return processHandle;
    }
    
    /*▲ 状态/属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * The {@code toString} method returns a string consisting of
     * the native process ID of the process and the exit value of the process.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        int exitCode = getExitCodeProcess(handle);
        return new StringBuilder("Process[pid=").append(pid()).append(", exitValue=").append(exitCode == STILL_ACTIVE ? "\"not exited\"" : exitCode).append("]").toString();
    }
    
    
    /** System-dependent portion of ProcessBuilder.start() */
    // 启动进程
    static Process start(String[] cmdarray, Map<String, String> environment, String dir, ProcessBuilder.Redirect[] redirects, boolean redirectErrorStream) throws IOException {
        // 将Map类型的环境变量转换为原始环境变量块
        String envblock = ProcessEnvironment.toEnvironmentBlock(environment);
        
        FileInputStream f0 = null;
        FileOutputStream f1 = null;
        FileOutputStream f2 = null;
        
        try {
            // 存储进程输入流、输出流、错误流的句柄(当进程启动后，此处存储的值可能会被JVM修改)
            long[] stdHandles;
            
            // 未设置重定向信息，则默认行为与"Redirect.PIPE"一致
            if(redirects == null) {
                stdHandles = new long[]{-1L, -1L, -1L};
            } else {
                stdHandles = new long[3];
                
                if(redirects[0] == Redirect.PIPE) {
                    stdHandles[0] = -1L;
                } else if(redirects[0] == Redirect.INHERIT) {
                    stdHandles[0] = fdAccess.getHandle(FileDescriptor.in);
                } else if(redirects[0] instanceof ProcessBuilder.RedirectPipeImpl) {
                    stdHandles[0] = fdAccess.getHandle(((ProcessBuilder.RedirectPipeImpl) redirects[0]).getFd());
                } else {
                    f0 = new FileInputStream(redirects[0].file());
                    stdHandles[0] = fdAccess.getHandle(f0.getFD());
                }
                
                if(redirects[1] == Redirect.PIPE) {
                    stdHandles[1] = -1L;
                } else if(redirects[1] == Redirect.INHERIT) {
                    stdHandles[1] = fdAccess.getHandle(FileDescriptor.out);
                } else if(redirects[1] instanceof ProcessBuilder.RedirectPipeImpl) {
                    stdHandles[1] = fdAccess.getHandle(((ProcessBuilder.RedirectPipeImpl) redirects[1]).getFd());
                } else {
                    boolean append = redirects[1].append();
                    
                    // 打开指定文件的输出流，append指示是否使用追加模式
                    f1 = newFileOutputStream(redirects[1].file(), append);
                    stdHandles[1] = fdAccess.getHandle(f1.getFD());
                }
                
                if(redirects[2] == Redirect.PIPE) {
                    stdHandles[2] = -1L;
                } else if(redirects[2] == Redirect.INHERIT) {
                    stdHandles[2] = fdAccess.getHandle(FileDescriptor.err);
                } else if(redirects[2] instanceof ProcessBuilder.RedirectPipeImpl) {
                    stdHandles[2] = fdAccess.getHandle(((ProcessBuilder.RedirectPipeImpl) redirects[2]).getFd());
                } else {
                    boolean append = redirects[2].append();
                    f2 = newFileOutputStream(redirects[2].file(), append);
                    stdHandles[2] = fdAccess.getHandle(f2.getFD());
                }
            }
            
            // 构造一个进程，并启动它
            Process p = new ProcessImpl(cmdarray, envblock, dir, stdHandles, redirectErrorStream);
            
            if(redirects == null) {
                return p;
            }
            
            // Copy the handles's if they are to be redirected to another process
            if(stdHandles[0] >= 0 && redirects[0] instanceof ProcessBuilder.RedirectPipeImpl) {
                fdAccess.setHandle(((ProcessBuilder.RedirectPipeImpl) redirects[0]).getFd(), stdHandles[0]);
            }
            
            if(stdHandles[1] >= 0 && redirects[1] instanceof ProcessBuilder.RedirectPipeImpl) {
                fdAccess.setHandle(((ProcessBuilder.RedirectPipeImpl) redirects[1]).getFd(), stdHandles[1]);
            }
            
            if(stdHandles[2] >= 0 && redirects[2] instanceof ProcessBuilder.RedirectPipeImpl) {
                fdAccess.setHandle(((ProcessBuilder.RedirectPipeImpl) redirects[2]).getFd(), stdHandles[2]);
            }
            
            return p;
        } finally {
            // In theory, close() can throw IOException (although it is rather unlikely to happen here)
            try {
                if(f0 != null) {
                    f0.close();
                }
            } finally {
                try {
                    if(f1 != null) {
                        f1.close();
                    }
                } finally {
                    if(f2 != null) {
                        f2.close();
                    }
                }
            }
        }
        
    }
    
    /**
     * Open a file for writing.
     * If {@code append} is {@code true} then the file is opened for atomic append directly and a FileOutputStream constructed with the resulting handle.
     * This is because a FileOutputStream created to append to a file does not open the file in a manner that guarantees that writes by the child process will be atomic.
     */
    // 打开指定文件的输出流，append指示是否使用追加模式
    private static FileOutputStream newFileOutputStream(File file, boolean append) throws IOException {
        if(!append) {
            // 构造非追加模式下的输出流
            return new FileOutputStream(file);
        }
        
        // 返回File的本地化路径(忽略最后的'\'，除非是根目录)
        String path = file.getPath();
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkWrite(path);
        }
        
        // 创建或打开文件，返回文件句柄
        long handle = openForAtomicAppend(path);
        
        // 创建文件描述符
        final FileDescriptor fd = new FileDescriptor();
        
        // 为文件描述符设置文件句柄
        fdAccess.setHandle(fd, handle);
        
        return AccessController.doPrivileged(new PrivilegedAction<FileOutputStream>() {
            public FileOutputStream run() {
                return new FileOutputStream(fd);
            }
        });
    }
    
    /**
     * Parses the command string parameter into the executable name and program arguments.
     *
     * The command string is broken into tokens.
     * The token separator is a space or quota character.
     * The space inside quotation is not a token separator.
     * There are no escape sequences.
     */
    // 将命令字符串参数解析为可执行文件名称和程序参数
    private static String[] getTokensFromCommand(String command) {
        ArrayList<String> matchList = new ArrayList<>(8);
        
        Matcher regexMatcher = LazyPattern.PATTERN.matcher(command);
        
        while(regexMatcher.find()) {
            matchList.add(regexMatcher.group());
        }
        
        return matchList.toArray(new String[0]);
    }
    
    // 创建命令行字符串
    private static String createCommandLine(int verificationType, final String executablePath, final String[] cmd) {
        StringBuilder cmdbuf = new StringBuilder(80);
        
        cmdbuf.append(executablePath);
        
        for(int i = 1; i<cmd.length; ++i) {
            cmdbuf.append(' ');
            
            String s = cmd[i];
            
            if(needsEscaping(verificationType, s)) {
                cmdbuf.append('"').append(s);
                
                // The code protects the [java.exe] and console command line
                // parser, that interprets the [\"] combination as an escape
                // sequence for the ["] char.
                //     http://msdn.microsoft.com/en-us/library/17w5ykft.aspx
                //
                // If the argument is an FS path, doubling of the tail [\]
                // char is not a problem for non-console applications.
                //
                // The [\"] sequence is not an escape sequence for the [cmd.exe]
                // command line parser. The case of the [""] tail escape
                // sequence could not be realized due to the argument validation
                // procedure.
                if((verificationType != VERIFICATION_CMD_BAT) && s.endsWith("\\")) {
                    cmdbuf.append('\\');
                }
                
                cmdbuf.append('"');
            } else {
                cmdbuf.append(s);
            }
        }
        
        return cmdbuf.toString();
    }
    
    // 判断arg是否在双引号之内，noQuotesInside指示arg内部是否禁止双引号存在
    private static boolean isQuoted(boolean noQuotesInside, String arg, String errorMessage) {
        int lastPos = arg.length() - 1;
        
        if(lastPos >= 1 && arg.charAt(0) == '"' && arg.charAt(lastPos) == '"') {
            // The argument has already been quoted.
            if(noQuotesInside) {
                if(arg.indexOf('"', 1) != lastPos) {
                    // There is ["] inside.
                    throw new IllegalArgumentException(errorMessage);
                }
            }
            
            return true;
        }
        
        if(noQuotesInside) {
            if(arg.indexOf('"') >= 0) {
                // There is ["] inside.
                throw new IllegalArgumentException(errorMessage);
            }
        }
        
        return false;
    }
    
    // 判断arg中是否存在待转义字符
    private static boolean needsEscaping(int verificationType, String arg) {
        /*
         * Switch off MS heuristic for internal ["].
         * Please, use the explicit [cmd.exe] call
         * if you need the internal ["].
         *    Example: "cmd.exe", "/C", "Extended_MS_Syntax"
         *
         * For [.exe] or [.com] file the unpaired/internal ["] in the argument is not a problem.
         */
        
        // 判断arg是否在双引号之内，如果verificationType是VERIFICATION_CMD_BAT，则arg内部禁止双引号存在
        boolean argIsQuoted = isQuoted((verificationType == VERIFICATION_CMD_BAT), arg, "Argument has embedded quote, use the explicit CMD.EXE call.");
        if(argIsQuoted) {
            return false;
        }
        
        char[] testEscape = ESCAPE_VERIFICATION[verificationType];
        for(char ch : testEscape) {
            if(arg.indexOf(ch) >= 0) {
                return true;
            }
        }
        
        return false;
    }
    
    // 获取可执行文件路径
    private static String getExecutablePath(String path) throws IOException {
        boolean pathIsQuoted = isQuoted(true, path, "Executable name has embedded quote, split the arguments");
        
        // Win32 CreateProcess requires path to be normalized
        File fileToRun = new File(pathIsQuoted ? path.substring(1, path.length() - 1) : path);
        
        // From the [CreateProcess] function documentation:
        //
        // "If the file name does not contain an extension, .exe is appended.
        // Therefore, if the file name extension is .com, this parameter
        // must include the .com extension. If the file name ends in
        // a period (.) with no extension, or if the file name contains a path,
        // .exe is not appended."
        //
        // "If the file name !does not contain a directory path!,
        // the system searches for the executable file in the following
        // sequence:..."
        //
        // In practice ANY non-existent path is extended by [.exe] extension
        // in the [CreateProcess] funcion with the only exception:
        // the path ends by (.)
        
        return fileToRun.getPath();
    }
    
    // 获取进程存活的状态码
    private static native int getStillActive();
    
    /*
     * 获取一个已结束进程的退出状态码。
     * 状态码为零表示成功退出，状态码不为零表示执行中发生了状况。
     */
    private static native int getExitCodeProcess(long handle);
    
    // 等待指定的进程执行完成后返回
    private static native void waitForInterruptibly(long handle);
    
    // 等待指定的进程执行完成后返回，或者超时后返回
    private static native void waitForTimeoutInterruptibly(long handle, long timeout);
    
    // 终止指定的进程，退出状态码为1
    private static native void terminateProcess(long handle);
    
    // 返回指定进程的pid
    private static native int getProcessId0(long handle);
    
    // 判断指定句柄处的进程是否处于活动状态
    private static native boolean isProcessAlive(long handle);
    
    /**
     * Create a process using the win32 function CreateProcess.
     * The method is synchronized due to MS kb315939 problem.
     * All native handles should restore the inherit flag at the end of call.
     *
     * @param cmdstr              the Windows command line
     * @param envblock            NUL-separated, double-NUL-terminated list of
     *                            environment strings in VAR=VALUE form
     * @param dir                 the working directory of the process, or null if
     *                            inheriting the current directory from the parent process
     * @param stdHandles          array of windows HANDLEs.  Indexes 0, 1, and
     *                            2 correspond to standard input, standard output and
     *                            standard error, respectively.  On input, a value of -1
     *                            means to create a pipe to connect child and parent
     *                            processes.  On output, a value which is not -1 is the
     *                            parent pipe handle corresponding to the pipe which has
     *                            been created.  An element of this array is -1 on input
     *                            if and only if it is <em>not</em> -1 on output.
     * @param redirectErrorStream redirectErrorStream attribute
     *
     * @return the native subprocess HANDLE returned by CreateProcess
     */
    // 创建并执行新的进程，返回本地子进程句柄
    private static synchronized native long create(String cmdstr, String envblock, String dir, long[] stdHandles, boolean redirectErrorStream) throws IOException;
    
    /**
     * Opens a file for atomic append. The file is created if it doesn't already exist.
     *
     * @param path the file to open or create
     *
     * @return the native HANDLE
     */
    // 创建或打开文件，返回文件句柄
    private static native long openForAtomicAppend(String path) throws IOException;
    
    // 关闭指定句柄处的进程
    private static native boolean closeHandle(long handle);
    
    // 判断待执行对象是否为shell文件(以cmd或bat结尾)
    private boolean isShellFile(String executablePath) {
        String upPath = executablePath.toUpperCase();
        return (upPath.endsWith(".CMD") || upPath.endsWith(".BAT"));
    }
    
    // 将arg用双引号扩起来
    private String quoteString(String arg) {
        return '"' + arg + '"';
    }
    
    
    private static class LazyPattern {
        // Escape-support version: "(\")((?:\\\\\\1|.)+?)\\1|([^\\s\"]+)";
        private static final Pattern PATTERN = Pattern.compile("[^\\s\"]+|\"[^\"]*\"");
    }
    
}
