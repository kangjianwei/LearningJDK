/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Native;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.security.AccessController.doPrivileged;

/**
 * ProcessHandleImpl is the implementation of ProcessHandle.
 *
 * @see Process
 * @since 9
 */
// Java层进程句柄的实现
final class ProcessHandleImpl implements ProcessHandle {
    
    /**
     * The start time should match any value.
     * Typically, this is because the OS can not supply it.
     * The process is known to exist but not the exact start time.
     */
    private final long STARTTIME_ANY = 0L;
    
    /** The start time of a Process that does not exist. */
    private final long STARTTIME_PROCESS_UNKNOWN = -1;
    
    /** Return value from waitForProcessExit0 indicating the process is not a child. */
    @Native
    private static final int NOT_A_CHILD = -2;
    
    /** Default size of stack for reaper processes. */
    private static long REAPER_DEFAULT_STACKSIZE = 128 * 1024;
    
    /** Cache the ProcessHandle of this process. */
    // 缓存加载ProcessHandleImpl类的进程的句柄，只会被初始化一次
    private static final ProcessHandleImpl current;
    
    /** Map of pids to ExitCompletions. */
    // 缓存异步等待退出的进程
    private static final ConcurrentMap<Long, ExitCompletion> completions = new ConcurrentHashMap<>();
    
    /** The pid of this ProcessHandle. */
    private final long pid;     // 进程号
    
    /**
     * The start time of this process.
     * If STARTTIME_ANY, the start time of the process is not available from the os.
     * If greater than zero, the start time of the process.
     */
    private final long startTime;   // 进程的起始时间
    
    /** The thread pool of "process reaper" daemon threads. */
    private static final Executor processReaperExecutor = doPrivileged((PrivilegedAction<Executor>) () -> {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        
        while(tg.getParent() != null) {
            tg = tg.getParent();
        }
        
        ThreadGroup systemThreadGroup = tg;
        
        final long stackSize = Boolean.getBoolean("jdk.lang.processReaperUseDefaultStackSize") ? 0 : REAPER_DEFAULT_STACKSIZE;
        
        // 守护线程的工厂，这里创建的线程拥有最大优先级
        ThreadFactory threadFactory = grimReaper -> {
            Thread t = new Thread(systemThreadGroup, grimReaper, "process reaper", stackSize, false);
            t.setDaemon(true);
            // A small attempt (probably futile) to avoid priority inversion
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        };
        
        return Executors.newCachedThreadPool(threadFactory);
    });
    
    
    static {
        initNative();
        
        // 获取当前进程的进程号
        long pid = getCurrentPid0();
        
        // 构造Java层的进程句柄
        current = new ProcessHandleImpl(pid, isAlive0(pid));
    }
    
    
    /**
     * Private constructor.
     * Instances are created by the {@code get(long)} factory.
     *
     * @param pid the pid for this instance
     */
    // 构造Java层的(子)进程句柄
    private ProcessHandleImpl(long pid, long startTime) {
        this.pid = pid;
        this.startTime = startTime;
    }
    
    
    /**
     * Returns the ProcessHandle for the current native process.
     *
     * @return The ProcessHandle for the OS process.
     *
     * @throws SecurityException if RuntimePermission("manageProcess") is not granted
     */
    // 返回加载ProcessHandleImpl类的进程的句柄
    public static ProcessHandleImpl current() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
    
        return current;
    }
    
    /**
     * Returns a ProcessHandle for the parent process.
     *
     * @return a ProcessHandle of the parent process; {@code null} is returned
     * if the child process does not have a parent
     *
     * @throws SecurityException if permission is not granted by the
     *                           security policy
     */
    // 返回当前进程的父进程句柄
    public Optional<ProcessHandle> parent() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
    
        // 返回父进程的进程号
        long ppid = parent0(pid, startTime);
        if(ppid<=0) {
            return Optional.empty();
        }
    
        return get(ppid);
    }
    
    // 返回当前进程的直接子进程
    @Override
    public Stream<ProcessHandle> children() {
        /*
         * The native OS code selects based on matching the requested parent pid.
         * If the original parent exits, the pid may have been re-used for this newer process.
         * Processes started by the original parent (now dead) will all have start times less than the start of this newer parent.
         * Processes started by this newer parent will have start times equal or after this parent.
         */
        return children(pid).filter(ph -> startTime<=((ProcessHandleImpl) ph).startTime);
    }
    
    // 返回当前进程的后代进程
    @Override
    public Stream<ProcessHandle> descendants() {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        
        int size = 100;
        long[] pids = null;
        long[] ppids = null;
        long[] starttimes = null;
        while(pids == null || size>pids.length) {
            pids = new long[size];
            ppids = new long[size];
            starttimes = new long[size];
            size = getProcessPids0(0, pids, ppids, starttimes);
        }
        
        int next = 0;       // index of next process to check
        int count = -1;     // count of subprocesses scanned
        long ppid = pid;    // start looking for this parent
        long ppStart = 0;
        
        // Find the start time of the parent
        for(int i = 0; i<size; i++) {
            if(pids[i] == ppid) {
                ppStart = starttimes[i];
                break;
            }
        }
        
        do {
            // Scan from next to size looking for ppid with child start time the same or later than the parent.
            // If found, exchange it with index next
            for(int i = next; i<size; i++) {
                if(ppids[i] == ppid && ppStart<=starttimes[i]) {
                    swap(pids, i, next);
                    swap(ppids, i, next);
                    swap(starttimes, i, next);
                    next++;
                }
            }
            ppid = pids[++count];   // pick up the next pid to scan for
            ppStart = starttimes[count];    // and its start time
        } while(count<next);
        
        final long[] cpids = pids;
        final long[] stimes = starttimes;
        
        return IntStream.range(0, count).mapToObj(i -> new ProcessHandleImpl(cpids[i], stimes[i]));
    }
    
    /**
     * Returns a ProcessHandle for an existing native process.
     *
     * @param pid the native process identifier
     *
     * @return The ProcessHandle for the pid if the process is alive;
     * or {@code null} if the process ID does not exist in the native system.
     *
     * @throws SecurityException if RuntimePermission("manageProcess") is not granted
     */
    // 返回进程号为pid的进程句柄
    static Optional<ProcessHandle> get(long pid) {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        
        // 如果指定的进程仍然存活，返回进程的起始时间
        long start = isAlive0(pid);
        
        return (start >= 0) ? Optional.of(new ProcessHandleImpl(pid, start)) : Optional.empty();
    }
    
    /**
     * Returns a ProcessHandle for an existing native process known to be alive.
     * The startTime of the process is retrieved and stored in the ProcessHandle.
     * It does not perform a security check since it is called from ProcessImpl.
     *
     * @param pid of the known to exist process
     *
     * @return a ProcessHandle corresponding to an existing Process instance
     */
    // 返回进程号为pid的进程句柄
    static ProcessHandleImpl getInternal(long pid) {
        return new ProcessHandleImpl(pid, isAlive0(pid));
    }
    
    /**
     * Returns a Stream of the children of a process or all processes.
     *
     * @param pid the pid of the process for which to find the children; 0 for all processes
     *
     * @return a stream of ProcessHandles
     */
    // 如果pid为0，返回所有已知进程；否则，返回pid进程的直接子进程
    static Stream<ProcessHandle> children(long pid) {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        
        int size = 100;
        long[] childpids = null;
        long[] starttimes = null;
        while(childpids == null || size>childpids.length) {
            childpids = new long[size];
            starttimes = new long[size];
            size = getProcessPids0(pid, childpids, null, starttimes);
        }
        
        final long[] cpids = childpids;
        final long[] stimes = starttimes;
        
        return IntStream.range(0, size).mapToObj(i -> new ProcessHandleImpl(cpids[i], stimes[i]));
    }
    
    
    /*
     * 返回一个阶段：该阶段的执行结果是当前进程退出时的状态码(如果没执行完，则会等待它执行完)；
     * 该方法可以看做是【异步】等待进程结束的一种手段。
     *
     * 如果等待进程结束的过程中抛出了中断异常，则会为执行该阶段任务的线程设置中断标记。
     */
    @Override
    public CompletableFuture<ProcessHandle> onExit() {
        if(this.equals(current)) {
            throw new IllegalStateException("onExit for current process not allowed");
        }
        
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
    
    
    /**
     * Returns the native process ID.
     * A {@code long} is used to be able to fit the system specific binary values
     * for the process.
     *
     * @return the native process ID
     */
    // 返回当前进程的进程号
    @Override
    public long pid() {
        return pid;
    }
    
    // 返回当前进程的快照信息
    @Override
    public ProcessHandle.Info info() {
        return ProcessHandleImpl.Info.info(pid, startTime);
    }
    
    /**
     * Tests whether the process represented by this {@code ProcessHandle} is alive.
     *
     * @return {@code true} if the process represented by this
     * {@code ProcessHandle} object has not yet terminated.
     *
     * @since 9
     */
    // 判断当前进程是否处于活动状态
    @Override
    public boolean isAlive() {
        long start = isAlive0(pid);
        return start >= 0 && (start == startTime || start == 0 || startTime == 0);
    }
    
    // 返回当前平台对结束进程的支持状况；返回true表示支持正常终止，返回false表示支持强制终止
    @Override
    public boolean supportsNormalTermination() {
        return ProcessImpl.SUPPORTS_NORMAL_TERMINATION;
    }
    
    // 终止当前进程；如果当前进程是加载ProcessHandleImpl类的进程，则无法销毁
    @Override
    public boolean destroy() {
        return destroyProcess(false);
    }
    
    // 强制终止当前进程；如果当前进程是加载ProcessHandleImpl类的进程，则无法销毁
    @Override
    public boolean destroyForcibly() {
        return destroyProcess(true);
    }
    
    
    @Override
    public String toString() {
        return Long.toString(pid);
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(pid);
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        
        if(obj instanceof ProcessHandleImpl) {
            ProcessHandleImpl other = (ProcessHandleImpl) obj;
            return (pid == other.pid) && (startTime == other.startTime || startTime == 0 || other.startTime == 0);
        }
        
        return false;
    }
    
    @Override
    public int compareTo(ProcessHandle other) {
        return Long.compare(pid, ((ProcessHandleImpl) other).pid);
    }
    
    
    /**
     * Returns a CompletableFuture that completes with process exit status when the process completes.
     *
     * @param shouldReap true if the exit value should be reaped
     */
    // 返回一个任务阶段，该阶段完成时将以进程pid的退出状态作为完成标记
    static CompletableFuture<Integer> completion(long pid, boolean shouldReap) {
        
        // 获取指定进程号pid关联的阶段
        ExitCompletion completion = completions.get(pid);   // check canonicalizing cache 1st
        
        /*
         * re-try until we get a completion that shouldReap => isReaping
         *
         * 如果completion为null，说明指定进程号pid关联的阶段不存在，此时可以插入新的阶段。
         *
         * 如果completion不为null，说明：
         * 1.进入该方法时，已经存在目标阶段，则需要进一步判断是否可以重试替换，如果允许重试，则重试到成功替换为止；
         * 2.进入该方法时，不存在目标阶段，但是在插入新阶段时，被别的线程抢先了，此时也需要进一步判断是否可以重试插入；如果重试失败了，则需要持续重试，直到成功.
         *
         * 允许重试的条件是：前一次设置shouldReap为false，后一次设置shouldReap为true
         */
        while(completion == null || (shouldReap && !completion.isReaping)) {
            
            // 构造一个新的阶段
            ExitCompletion newCompletion = new ExitCompletion(shouldReap);
            
            // 如果completions中还没有key为pid的阶段，尝试添加新的阶段
            if(completion == null) {
                // 此处应当返回null；如果不为null，说明可能是别的线程抢先存入了元素，此时，插入元素失败，返回插入前的元素
                completion = completions.putIfAbsent(pid, newCompletion);
                
                // 如果completions中已经存在key为pid的阶段，则尝试替换
            } else {
                completion = completions.replace(pid, completion, newCompletion) ? null                  // 替换成功，返回null
                    : completions.get(pid); // 替换失败时，返回替换前的阶段
            }
            
            // 如果completion不为null，说明插入/替换失败，需要再次进入循环来重试
            if(completion != null) {
                continue;
            }
            
            // newCompletion has just been installed successfully
            completion = newCompletion;
            
            // spawn a thread to wait for and deliver the exit value
            processReaperExecutor.execute(new Runnable() {
                // Use inner class to avoid lambda stack overhead
                public void run() {
                    // 等待进程退出
                    int exitValue = waitForProcessExit0(pid, shouldReap);
                    
                    // 如果该pid不是子进程
                    if(exitValue == NOT_A_CHILD) {
                        // pid not alive or not a child of this process.
                        // If it is alive wait for it to terminate
                        long sleep = 300;     // initial milliseconds to sleep
                        int incr = 30;        // increment to the sleep time
                        
                        long startTime = isAlive0(pid);
                        long origStart = startTime;
                        
                        while(startTime >= 0) {
                            try {
                                Thread.sleep(Math.min(sleep, 5000L)); // no more than 5 sec
                                sleep += incr;
                            } catch(InterruptedException ie) {
                                // ignore and retry
                            }
                            
                            startTime = isAlive0(pid);  // recheck if it is alive
                            
                            if(startTime>0 && origStart>0 && startTime != origStart) {
                                // start time changed (and is not zero), pid is not the same process
                                break;
                            }
                        }
                        
                        exitValue = 0;
                    }
                    
                    // 进程已退出，将阶段的执行结果设置为退出状态码
                    newCompletion.complete(exitValue);
                    
                    // remove from cache afterwards
                    completions.remove(pid, newCompletion);
                }
            });
        }
        
        return completion;
    }
    
    /**
     * Destroy the process for this ProcessHandle.
     * The native code checks the start time before sending the termination request.
     *
     * @param force {@code true} if the process should be terminated forcibly;
     *              else {@code false} for a normal termination
     */
    // 销毁当前进程，force指示是否强制销毁；如果当前进程是加载ProcessHandleImpl类的进程，则无法销毁
    boolean destroyProcess(boolean force) {
        if(this.equals(current)) {
            throw new IllegalStateException("destroy of current process not allowed");
        }
        
        return destroy0(pid, startTime, force);
    }
    
    /**
     * Wait for the process to exit, return the value.
     * Conditionally reap the value if requested
     *
     * @param pid       the processId
     * @param reapvalue if true, the value is retrieved, else return the value and leave the process waitable
     *
     * @return the value or -1 if an error occurs
     */
    // 等待进程退出
    private static native int waitForProcessExit0(long pid, boolean reapvalue);
    
    /**
     * Return the pid of the current process.
     *
     * @return the pid of the  current process
     */
    // 返回当前进程的进程号
    private static native long getCurrentPid0();
    
    /**
     * Returns the parent of the native pid argument.
     *
     * @param pid       the process id
     * @param startTime the startTime of the process
     *
     * @return the parent of the native pid; if any, otherwise -1
     */
    // 返回父进程的进程号
    private static native long parent0(long pid, long startTime);
    
    /**
     * Returns the number of pids filled in to the array.
     *
     * @param pid        if {@code pid} equals zero, then all known processes are returned;
     *                   otherwise only direct child process pids are returned
     * @param pids       an allocated long array to receive the pids
     * @param ppids      an allocated long array to receive the parent pids; may be null
     * @param starttimes an allocated long array to receive the child start times; may be null
     *
     * @return if greater than or equals to zero is the number of pids in the array;
     * if greater than the length of the arrays, the arrays are too small
     */
    // 如果pid为0，返回所有已知进程；否则，返回pid进程的直接子进程
    private static native int getProcessPids0(long pid, long[] pids, long[] ppids, long[] starttimes);
    
    /**
     * Signal the process to terminate.
     * The process is signaled only if its start time matches the known start time.
     *
     * @param pid       process id to kill
     * @param startTime the start time of the process
     * @param forcibly  true to forcibly terminate (SIGKILL vs SIGTERM)
     *
     * @return true if the process was signaled without error; false otherwise
     */
    // 销毁进程号为pid的进程，forcibly指示是否强制销毁
    private static native boolean destroy0(long pid, long startTime, boolean forcibly);
    
    /**
     * Returns the process start time depending on whether the pid is alive.
     * This must not reap the exitValue.
     *
     * @param pid the pid to check
     *
     * @return the start time in milliseconds since 1970, 0 if the start time cannot be determined, -1 if the pid does not exist.
     */
    // 如果指定的进程仍然存活，返回进程的起始时间
    private static native long isAlive0(long pid);
    
    // Swap two elements in an array
    private static void swap(long[] array, int x, int y) {
        long v = array[x];
        array[x] = array[y];
        array[y] = v;
    }
    
    private static native void initNative();
    
    
    /**
     * Implementation of ProcessHandle.Info.
     * Information snapshot about a process.
     * The attributes of a process vary by operating system and are not available
     * in all implementations.  Additionally, information about other processes
     * is limited by the operating system privileges of the process making the request.
     * If a value is not available, either a {@code null} or {@code -1} is stored.
     * The accessor methods return {@code null} if the value is not available.
     */
    // 进程快照
    static class Info implements ProcessHandle.Info {
        String command;
        String commandLine;
        String[] arguments;
        long startTime;
        long totalTime;
        String user;
        
        static {
            initIDs();
        }
        
        Info() {
            command = null;
            commandLine = null;
            arguments = null;
            startTime = -1L;
            totalTime = -1L;
            user = null;
        }
        
        /**
         * Returns the Info object with the fields from the process.
         * Whatever fields are provided by native are returned.
         * If the startTime of the process does not match the provided
         * startTime then an empty Info is returned.
         *
         * @param pid       the native process identifier
         * @param startTime the startTime of the process being queried
         *
         * @return ProcessHandle.Info non-null; individual fields may be null
         * or -1 if not available.
         */
        public static ProcessHandle.Info info(long pid, long startTime) {
            Info info = new Info();
            info.info0(pid);
            if(startTime != info.startTime) {
                info.command = null;
                info.arguments = null;
                info.startTime = -1L;
                info.totalTime = -1L;
                info.user = null;
            }
            return info;
        }
        
        @Override
        public Optional<String> command() {
            return Optional.ofNullable(command);
        }
        
        @Override
        public Optional<String> commandLine() {
            if(command != null && arguments != null) {
                return Optional.of(command + " " + String.join(" ", arguments));
            } else {
                return Optional.ofNullable(commandLine);
            }
        }
        
        @Override
        public Optional<String[]> arguments() {
            return Optional.ofNullable(arguments);
        }
        
        @Override
        public Optional<Instant> startInstant() {
            return (startTime>0) ? Optional.of(Instant.ofEpochMilli(startTime)) : Optional.empty();
        }
        
        @Override
        public Optional<Duration> totalCpuDuration() {
            return (totalTime != -1) ? Optional.of(Duration.ofNanos(totalTime)) : Optional.empty();
        }
        
        @Override
        public Optional<String> user() {
            return Optional.ofNullable(user);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(60);
            
            sb.append('[');
            if(user != null) {
                sb.append("user: ");
                sb.append(user());
            }
            
            if(command != null) {
                if(sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append("cmd: ");
                sb.append(command);
            }
            
            if(arguments != null && arguments.length>0) {
                if(sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append("args: ");
                sb.append(Arrays.toString(arguments));
            }
            
            if(commandLine != null) {
                if(sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append("cmdLine: ");
                sb.append(commandLine);
            }
            
            if(startTime>0) {
                if(sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append("startTime: ");
                sb.append(startInstant());
            }
            
            if(totalTime != -1) {
                if(sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append("totalTime: ");
                sb.append(totalCpuDuration().toString());
            }
            
            sb.append(']');
            return sb.toString();
        }
        
        /**
         * Initialization of JNI fieldIDs.
         */
        private static native void initIDs();
        
        /**
         * Fill in this Info instance with information about the native process.
         * If values are not available the native code does not modify the field.
         *
         * @param pid of the native process
         */
        private native void info0(long pid);
    }
    
    // 等待完成退出的阶段
    private static class ExitCompletion extends CompletableFuture<Integer> {
        final boolean isReaping;
        
        ExitCompletion(boolean isReaping) {
            this.isReaping = isReaping;
        }
    }
    
}
