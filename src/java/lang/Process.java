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

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * {@code Process} provides control of native processes started by
 * ProcessBuilder.start and Runtime.exec.
 * The class provides methods for performing input from the process, performing
 * output to the process, waiting for the process to complete,
 * checking the exit status of the process, and destroying (killing)
 * the process.
 * The {@link ProcessBuilder#start()} and
 * {@link Runtime#exec(String[], String[], File) Runtime.exec}
 * methods create a native process and return an instance of a
 * subclass of {@code Process} that can be used to control the process
 * and obtain information about it.
 *
 * <p>The methods that create processes may not work well for special
 * processes on certain native platforms, such as native windowing
 * processes, daemon processes, Win16/DOS processes on Microsoft
 * Windows, or shell scripts.
 *
 * <p>By default, the created process does not have its own terminal
 * or console.  All its standard I/O (i.e. stdin, stdout, stderr)
 * operations will be redirected to the parent process, where they can
 * be accessed via the streams obtained using the methods
 * {@link #getOutputStream()},
 * {@link #getInputStream()}, and
 * {@link #getErrorStream()}.
 * The parent process uses these streams to feed input to and get output
 * from the process.  Because some native platforms only provide
 * limited buffer size for standard input and output streams, failure
 * to promptly write the input stream or read the output stream of
 * the process may cause the process to block, or even deadlock.
 *
 * <p>Where desired, <a href="ProcessBuilder.html#redirect-input">
 * process I/O can also be redirected</a>
 * using methods of the {@link ProcessBuilder} class.
 *
 * <p>The process is not killed when there are no more references to
 * the {@code Process} object, but rather the process
 * continues executing asynchronously.
 *
 * <p>There is no requirement that the process represented by a {@code
 * Process} object execute asynchronously or concurrently with respect
 * to the Java process that owns the {@code Process} object.
 *
 * <p>As of 1.5, {@link ProcessBuilder#start()} is the preferred way
 * to create a {@code Process}.
 *
 * <p>Subclasses of Process should override the {@link #onExit()} and
 * {@link #toHandle()} methods to provide a fully functional Process including the
 * {@linkplain #pid() process id},
 * {@linkplain #info() information about the process},
 * {@linkplain #children() direct children}, and
 * {@linkplain #descendants() direct children plus descendants of those children} of the process.
 * Delegating to the underlying Process or ProcessHandle is typically
 * easiest and most efficient.
 *
 * @since 1.0
 */
// 进程
public abstract class Process {
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Default constructor for Process.
     */
    public Process() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 输入/输出 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the output stream connected to the normal input of the
     * process.  Output to the stream is piped into the standard
     * input of the process represented by this {@code Process} object.
     *
     * <p>If the standard input of the process has been redirected using
     * {@link ProcessBuilder#redirectInput(Redirect)
     * ProcessBuilder.redirectInput}
     * then this method will return a
     * <a href="ProcessBuilder.html#redirect-input">null output stream</a>.
     *
     * <p>Implementation note: It is a good idea for the returned
     * output stream to be buffered.
     *
     * @return the output stream connected to the normal input of the
     * process
     */
    // 返回与进程输入端关联的输出流，可以由此向进程写入输入数据
    public abstract OutputStream getOutputStream();
    
    /**
     * Returns the input stream connected to the normal output of the
     * process.  The stream obtains data piped from the standard
     * output of the process represented by this {@code Process} object.
     *
     * <p>If the standard output of the process has been redirected using
     * {@link ProcessBuilder#redirectOutput(Redirect)
     * ProcessBuilder.redirectOutput}
     * then this method will return a
     * <a href="ProcessBuilder.html#redirect-output">null input stream</a>.
     *
     * <p>Otherwise, if the standard error of the process has been
     * redirected using
     * {@link ProcessBuilder#redirectErrorStream(boolean)
     * ProcessBuilder.redirectErrorStream}
     * then the input stream returned by this method will receive the
     * merged standard output and the standard error of the process.
     *
     * <p>Implementation note: It is a good idea for the returned
     * input stream to be buffered.
     *
     * @return the input stream connected to the normal output of the
     * process
     */
    // 返回与进程输出端关联的输入流，可以由此从进程读取输出数据
    public abstract InputStream getInputStream();
    
    /**
     * Returns the input stream connected to the error output of the
     * process.  The stream obtains data piped from the error output
     * of the process represented by this {@code Process} object.
     *
     * <p>If the standard error of the process has been redirected using
     * {@link ProcessBuilder#redirectError(Redirect)
     * ProcessBuilder.redirectError} or
     * {@link ProcessBuilder#redirectErrorStream(boolean)
     * ProcessBuilder.redirectErrorStream}
     * then this method will return a
     * <a href="ProcessBuilder.html#redirect-output">null input stream</a>.
     *
     * <p>Implementation note: It is a good idea for the returned
     * input stream to be buffered.
     *
     * @return the input stream connected to the error output of
     * the process
     */
    // 返回与进程错误输出端关联的输入流，可以由此从进程读取错误日志
    public abstract InputStream getErrorStream();
    
    /*▲ 输入/输出 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 等待 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Causes the current thread to wait, if necessary, until the
     * process represented by this {@code Process} object has
     * terminated.  This method returns immediately if the process
     * has already terminated.  If the process has not yet
     * terminated, the calling thread will be blocked until the
     * process exits.
     *
     * @return the exit value of the process represented by this
     * {@code Process} object.  By convention, the value
     * {@code 0} indicates normal termination.
     *
     * @throws InterruptedException if the current thread is
     *                              {@linkplain Thread#interrupt() interrupted} by another
     *                              thread while it is waiting, then the wait is ended and
     *                              an {@link InterruptedException} is thrown.
     */
    /*
     * 【同步】等待进程结束，在结束之前一直阻塞
     *
     * 使waitFor()所处线程进入阻塞，直到当前进程结束后，再切换到waitFor()所处线程。
     * 进程结束后，会返回其退出的状态码。一般来说，正常退出时返回0。
     * 如果线程发生中断，则抛出中断异常。
     */
    public abstract int waitFor() throws InterruptedException;
    
    /**
     * Causes the current thread to wait, if necessary, until the
     * process represented by this {@code Process} object has
     * terminated, or the specified waiting time elapses.
     *
     * <p>If the process has already terminated then this method returns
     * immediately with the value {@code true}.  If the process has not
     * terminated and the timeout value is less than, or equal to, zero, then
     * this method returns immediately with the value {@code false}.
     *
     * <p>The default implementation of this methods polls the {@code exitValue}
     * to check if the process has terminated. Concrete implementations of this
     * class are strongly encouraged to override this method with a more
     * efficient implementation.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the {@code timeout} argument
     *
     * @return {@code true} if the process has exited and {@code false} if
     * the waiting time elapsed before the process has exited.
     *
     * @throws InterruptedException if the current thread is interrupted
     *                              while waiting.
     * @throws NullPointerException if unit is null
     * @since 1.8
     */
    /*
     * 【同步】等待进程结束，超时后抛异常
     *
     * 使waitFor()所处线程进入阻塞，直到当前进程结束后，或者直到超时后，再切换到waitFor()所处线程。
     * 进程结束后，会返回其退出的状态码。一般来说，正常退出时返回0。
     * 子类应覆盖此实现。
     */
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        long startTime = System.nanoTime();
        long rem = unit.toNanos(timeout);
        
        do {
            try {
                // 返回进程退出的状态码，如果进程还未结束，则会抛异常
                exitValue();
                return true;
            } catch(IllegalThreadStateException ex) {
                if(rem>0) {
                    Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
                }
            }
            
            rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
        } while(rem>0);
        
        return false;
    }
    
    /**
     * Returns a {@code CompletableFuture<Process>} for the termination of the Process.
     * The {@link java.util.concurrent.CompletableFuture} provides the ability
     * to trigger dependent functions or actions that may be run synchronously
     * or asynchronously upon process termination.
     * When the process has terminated the CompletableFuture is
     * {@link java.util.concurrent.CompletableFuture#complete completed} regardless
     * of the exit status of the process.
     * <p>
     * Calling {@code onExit().get()} waits for the process to terminate and returns
     * the Process. The future can be used to check if the process is
     * {@linkplain java.util.concurrent.CompletableFuture#isDone done} or to
     * {@linkplain java.util.concurrent.CompletableFuture#get() wait} for it to terminate.
     * {@linkplain java.util.concurrent.CompletableFuture#cancel(boolean) Cancelling}
     * the CompletableFuture does not affect the Process.
     * <p>
     * Processes returned from {@link ProcessBuilder#start} override the
     * default implementation to provide an efficient mechanism to wait
     * for process exit.
     *
     * @return a new {@code CompletableFuture<Process>} for the Process
     *
     * @apiNote Using {@link #onExit() onExit} is an alternative to
     * {@link #waitFor() waitFor} that enables both additional concurrency
     * and convenient access to the result of the Process.
     * Lambda expressions can be used to evaluate the result of the Process
     * execution.
     * If there is other processing to be done before the value is used
     * then {@linkplain #onExit onExit} is a convenient mechanism to
     * free the current thread and block only if and when the value is needed.
     * <br>
     * For example, launching a process to compare two files and get a boolean if they are identical:
     * <pre> {@code   Process p = new ProcessBuilder("cmp", "f1", "f2").start();
     *    Future<Boolean> identical = p.onExit().thenApply(p1 -> p1.exitValue() == 0);
     *    ...
     *    if (identical.get()) { ... }
     * }</pre>
     * @implSpec This implementation executes {@link #waitFor()} in a separate thread
     * repeatedly until it returns successfully. If the execution of
     * {@code waitFor} is interrupted, the thread's interrupt status is preserved.
     * <p>
     * When {@link #waitFor()} returns successfully the CompletableFuture is
     * {@linkplain java.util.concurrent.CompletableFuture#complete completed} regardless
     * of the exit status of the process.
     *
     * This implementation may consume a lot of memory for thread stacks if a
     * large number of processes are waited for concurrently.
     * <p>
     * External implementations should override this method and provide
     * a more efficient implementation. For example, to delegate to the underlying
     * process, it can do the following:
     * <pre>{@code
     *    public CompletableFuture<Process> onExit() {
     *       return delegate.onExit().thenApply(p -> this);
     *    }
     * }</pre>
     * @apiNote The process may be observed to have terminated with {@link #isAlive}
     * before the ComputableFuture is completed and dependent actions are invoked.
     * @since 9
     */
    /*
     * 返回一个阶段：该阶段的执行结果是当前进程退出时的状态码(如果没执行完，则会等待它执行完)；
     * 该方法可以看做是【异步】等待进程结束的一种手段。
     *
     * 如果等待进程结束的过程中抛出了中断异常，则会为执行该阶段任务的线程设置中断标记。
     */
    public CompletableFuture<Process> onExit() {
        // 异步执行Supplier任务，返回该任务所属阶段(可从中获取执行结果)
        return CompletableFuture.supplyAsync(() -> waitForInternal());
    }
    
    /*▲ 等待 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 销毁 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Kills the process.
     * Whether the process represented by this {@code Process} object is
     * {@linkplain #supportsNormalTermination normally terminated} or not is
     * implementation dependent.
     * Forcible process destruction is defined as the immediate termination of a
     * process, whereas normal termination allows the process to shut down cleanly.
     * If the process is not alive, no action is taken.
     * <p>
     * The {@link java.util.concurrent.CompletableFuture} from {@link #onExit} is
     * {@linkplain java.util.concurrent.CompletableFuture#complete completed}
     * when the process has terminated.
     */
    // 终止进程
    public abstract void destroy();
    
    /**
     * Kills the process forcibly. The process represented by this
     * {@code Process} object is forcibly terminated.
     * Forcible process destruction is defined as the immediate termination of a
     * process, whereas normal termination allows the process to shut down cleanly.
     * If the process is not alive, no action is taken.
     * <p>
     * The {@link java.util.concurrent.CompletableFuture} from {@link #onExit} is
     * {@linkplain java.util.concurrent.CompletableFuture#complete completed}
     * when the process has terminated.
     * <p>
     * Invoking this method on {@code Process} objects returned by
     * {@link ProcessBuilder#start} and {@link Runtime#exec} forcibly terminate
     * the process.
     *
     * @return the {@code Process} object representing the
     * process forcibly destroyed
     *
     * @implSpec The default implementation of this method invokes {@link #destroy}
     * and so may not forcibly terminate the process.
     * @implNote Concrete implementations of this class are strongly encouraged to override
     * this method with a compliant implementation.
     * @apiNote The process may not terminate immediately.
     * i.e. {@code isAlive()} may return true for a brief period
     * after {@code destroyForcibly()} is called. This method
     * may be chained to {@code waitFor()} if needed.
     * @since 1.8
     */
    // 强制终止进程
    public Process destroyForcibly() {
        destroy();
        return this;
    }
    
    /**
     * Returns {@code true} if the implementation of {@link #destroy} is to
     * normally terminate the process,
     * Returns {@code false} if the implementation of {@code destroy}
     * forcibly and immediately terminates the process.
     * <p>
     * Invoking this method on {@code Process} objects returned by
     * {@link ProcessBuilder#start} and {@link Runtime#exec} return
     * {@code true} or {@code false} depending on the platform implementation.
     *
     * @return {@code true} if the implementation of {@link #destroy} is to
     * normally terminate the process;
     * otherwise, {@link #destroy} forcibly terminates the process
     *
     * @throws UnsupportedOperationException if the Process implementation
     *                                       does not support this operation
     * @implSpec This implementation throws an instance of
     * {@link java.lang.UnsupportedOperationException} and performs no other action.
     * @since 9
     */
    // 返回当前平台对结束进程的支持状况；返回true表示支持正常终止，返回false表示支持强制终止
    public boolean supportsNormalTermination() {
        throw new UnsupportedOperationException(this.getClass() + ".supportsNormalTermination() not supported");
    }
    
    /*▲ 销毁 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态/属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the native process ID of the process.
     * The native process ID is an identification number that the operating
     * system assigns to the process.
     *
     * @return the native process id of the process
     *
     * @throws UnsupportedOperationException if the Process implementation
     *                                       does not support this operation
     * @implSpec The implementation of this method returns the process id as:
     * {@link #toHandle toHandle().pid()}.
     * @since 9
     */
    // 返回进程号
    public long pid() {
        return toHandle().pid();
    }
    
    /**
     * Tests whether the process represented by this {@code Process} is
     * alive.
     *
     * @return {@code true} if the process represented by this
     * {@code Process} object has not yet terminated.
     *
     * @since 1.8
     */
    // 判断当前进程是否处于活动状态
    public boolean isAlive() {
        try {
            // 返回进程退出的状态码，如果进程还未结束，则会抛异常
            exitValue();
            return false;
        } catch(IllegalThreadStateException e) {
            return true;
        }
    }
    
    /**
     * Returns the exit value for the process.
     *
     * @return the exit value of the process represented by this {@code Process} object.
     * By convention, the value {@code 0} indicates normal termination.
     *
     * @throws IllegalThreadStateException if the process represented by this {@code Process} object has not yet terminated
     */
    // 返回进程退出的状态码；通常来说，状态码为零表示成功退出，状态码不为零表示执行中发生了状况
    public abstract int exitValue();
    
    /**
     * Returns a ProcessHandle for the Process.
     *
     * {@code Process} objects returned by {@link ProcessBuilder#start} and
     * {@link Runtime#exec} implement {@code toHandle} as the equivalent of
     * {@link ProcessHandle#of(long) ProcessHandle.of(pid)} including the
     * check for a SecurityManager and {@code RuntimePermission("manageProcess")}.
     *
     * @return Returns a ProcessHandle for the Process
     *
     * @throws UnsupportedOperationException if the Process implementation
     *                                       does not support this operation
     * @throws SecurityException             if a security manager has been installed and
     *                                       it denies RuntimePermission("manageProcess")
     * @implSpec This implementation throws an instance of
     * {@link java.lang.UnsupportedOperationException} and performs no other action.
     * Subclasses should override this method to provide a ProcessHandle for the
     * process.  The methods {@link #pid}, {@link #info}, {@link #children},
     * and {@link #descendants}, unless overridden, operate on the ProcessHandle.
     * @since 9
     */
    // 返回进程句柄
    public ProcessHandle toHandle() {
        throw new UnsupportedOperationException(this.getClass() + ".toHandle() not supported");
    }
    
    /*▲ 状态/属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 快照 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a snapshot of information about the process.
     *
     * <p> A {@link ProcessHandle.Info} instance has accessor methods
     * that return information about the process if it is available.
     *
     * @return a snapshot of information about the process, always non-null
     *
     * @throws UnsupportedOperationException if the Process implementation
     *                                       does not support this operation
     * @implSpec This implementation returns information about the process as:
     * {@link #toHandle toHandle().info()}.
     * @since 9
     */
    // 返回当前进程的快照信息
    public ProcessHandle.Info info() {
        return toHandle().info();
    }
    
    /**
     * Returns a snapshot of the direct children of the process.
     * The parent of a direct child process is the process.
     * Typically, a process that is {@linkplain #isAlive not alive} has no children.
     * <p>
     * <em>Note that processes are created and terminate asynchronously.
     * There is no guarantee that a process is {@linkplain #isAlive alive}.
     * </em>
     *
     * @return a sequential Stream of ProcessHandles for processes that are
     * direct children of the process
     *
     * @throws UnsupportedOperationException if the Process implementation
     *                                       does not support this operation
     * @throws SecurityException             if a security manager has been installed and
     *                                       it denies RuntimePermission("manageProcess")
     * @implSpec This implementation returns the direct children as:
     * {@link #toHandle toHandle().children()}.
     * @since 9
     */
    // 返回当前进程的直接子进程
    public Stream<ProcessHandle> children() {
        return toHandle().children();
    }
    
    /**
     * Returns a snapshot of the descendants of the process.
     * The descendants of a process are the children of the process
     * plus the descendants of those children, recursively.
     * Typically, a process that is {@linkplain #isAlive not alive} has no children.
     * <p>
     * <em>Note that processes are created and terminate asynchronously.
     * There is no guarantee that a process is {@linkplain #isAlive alive}.
     * </em>
     *
     * @return a sequential Stream of ProcessHandles for processes that
     * are descendants of the process
     *
     * @throws UnsupportedOperationException if the Process implementation
     *                                       does not support this operation
     * @throws SecurityException             if a security manager has been installed and
     *                                       it denies RuntimePermission("manageProcess")
     * @implSpec This implementation returns all children as:
     * {@link #toHandle toHandle().descendants()}.
     * @since 9
     */
    // 返回当前进程的后代进程
    public Stream<ProcessHandle> descendants() {
        return toHandle().descendants();
    }
    
    /*▲ 快照 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Wait for the process to exit by calling {@code waitFor}.
     * If the thread is interrupted, remember the interrupted state to
     * be restored before returning. Use ForkJoinPool.ManagedBlocker
     * so that the number of workers in case ForkJoinPool is used is
     * compensated when the thread blocks in waitFor().
     *
     * @return the Process
     */
    // 等待进程结束，如果等待过程中抛出了中断异常，则会为当前线程设置中断标记
    private Process waitForInternal() {
        boolean interrupted = false;
        
        while(true) {
            try {
                ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
                    @Override
                    public boolean block() throws InterruptedException {
                        waitFor();
                        return true;
                    }
                    
                    @Override
                    public boolean isReleasable() {
                        return !isAlive();
                    }
                });
                
                break;
            } catch(InterruptedException x) {
                interrupted = true;
            }
        }
        
        // 如果上面发生了中断异常
        if(interrupted) {
            // 中断线程（只是给线程预设一个标记，不是立即让线程停下来）
            Thread.currentThread().interrupt();
        }
        
        return this;
    }
    
    
    /**
     * An input stream for a subprocess pipe that skips by reading bytes instead of seeking,
     * the underlying pipe does not support seek.
     */
    static class PipeInputStream extends FileInputStream {
        
        PipeInputStream(FileDescriptor fd) {
            super(fd);
        }
        
        @Override
        public long skip(long n) throws IOException {
            long remaining = n;
            int nr;
            
            if(n<=0) {
                return 0;
            }
            
            int size = (int) Math.min(2048, remaining);
            byte[] skipBuffer = new byte[size];
            while(remaining>0) {
                nr = read(skipBuffer, 0, (int) Math.min(size, remaining));
                if(nr<0) {
                    break;
                }
                remaining -= nr;
            }
            
            return n - remaining;
        }
    }
    
}
