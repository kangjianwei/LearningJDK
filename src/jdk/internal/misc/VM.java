/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.misc;

import java.util.Map;
import java.util.Properties;

import static java.lang.Thread.State.BLOCKED;
import static java.lang.Thread.State.NEW;
import static java.lang.Thread.State.RUNNABLE;
import static java.lang.Thread.State.TERMINATED;
import static java.lang.Thread.State.TIMED_WAITING;
import static java.lang.Thread.State.WAITING;

/**
 * 虚拟机类
 */
public class VM {
    
    /**
     * The threadStatus field is set by the VM at state transition in the hotspot implementation.
     * Its value is set according to the JVM TI specification GetThreadState function.
     */
    private static final int JVMTI_THREAD_STATE_ALIVE = 0x0001;  // 线程状态：
    private static final int JVMTI_THREAD_STATE_TERMINATED = 0x0002;  // 线程状态：
    private static final int JVMTI_THREAD_STATE_RUNNABLE = 0x0004;  // 线程状态：RUNNABLE
    private static final int JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 0x0400;  // 线程状态：
    private static final int JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 0x0010;  // 线程状态：
    private static final int JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 0x0020;  // 线程状态：
    
    /** the init level when the VM is fully initialized */
    private static final int JAVA_LANG_SYSTEM_INITED = 1;   // VM初始化第一阶段已完成
    private static final int MODULE_SYSTEM_INITED = 2;   // VM初始化第二阶段已完成
    private static final int SYSTEM_LOADER_INITIALIZING = 3;   // VM初始化第三阶段已完成
    private static final int SYSTEM_BOOTED = 4;   // VM初始化第四阶段已完成
    private static final int SYSTEM_SHUTDOWN = 5;   // VM已关闭
    
    // 0, 1, 2, ...
    private static volatile int initLevel;
    
    /**
     * A user-settable upper limit on the maximum amount of allocatable direct buffer memory.
     * This value may be changed during VM initialization if "java" is launched with "-XX:MaxDirectMemorySize=<size>".
     * The initial value of this field is arbitrary;
     * during JRE initialization it will be reset to the value specified on the command line,
     * if any, otherwise to Runtime.getRuntime().maxMemory().
     */
    // 直接缓冲区默认大小
    private static long directMemory = 64 * 1024 * 1024;
    
    /**
     * User-controllable flag that determines if direct buffers should be page aligned.
     * The "-XX:+PageAlignDirectMemory" option can be used to force buffers, allocated by ByteBuffer.allocateDirect, to be page aligned.
     */
    // 直接缓冲区是否需要页对齐
    private static boolean pageAlignDirectMemory;
    
    // 虚拟机保存的原始系统属性集
    private static Map<String, String> savedProps;
    
    /** Current count of objects pending for finalization */
    // 记录当前挂起(待处理)的报废FinalReference数量
    private static volatile int finalRefCount;
    
    /** Peak count of objects pending for finalization */
    // 记录之前挂起(待处理)的报废FinalReference数量的最大值
    private static volatile int peakFinalRefCount;
    
    private static final Object lock = new Object();
    
    
    static {
        initialize();
    }
    
    
    /**
     * Sets the init level.
     *
     * @see java.lang.System#initPhase1
     * @see java.lang.System#initPhase2
     * @see java.lang.System#initPhase3
     */
    // 设置虚拟机级别，每次设置完之后，都要唤醒正在等待该级别的线程
    public static void initLevel(int level) {
        synchronized(lock) {
            if(level<=initLevel || level>SYSTEM_SHUTDOWN) {
                throw new InternalError("Bad level: " + level);
            }
            
            initLevel = level;
            
            lock.notifyAll();
        }
    }
    
    /**
     * Returns the current init level.
     */
    // 获取虚拟机当前所处的初始化级别
    public static int initLevel() {
        return initLevel;
    }
    
    /**
     * Waits for the init level to get the given value.
     *
     * @see java.lang.ref.Finalizer
     */
    // 等待虚拟机到达指定的启动级别
    public static void awaitInitLevel(int level) throws InterruptedException {
        synchronized(lock) {
            while(initLevel<level) {
                lock.wait();
            }
        }
    }
    
    /**
     * Returns {@code true} if the module system has been initialized.
     *
     * @see java.lang.System#initPhase2
     */
    // 判断VM初始化第二阶段是否已完成：模块系统是否已经初始化
    public static boolean isModuleSystemInited() {
        return VM.initLevel() >= MODULE_SYSTEM_INITED;
    }
    
    /**
     * Returns {@code true} if the VM is fully initialized.
     */
    // 判断VM初始化第四阶段是否已完成：虚拟机是否已完全启动
    public static boolean isBooted() {
        return initLevel >= SYSTEM_BOOTED;
    }
    
    /**
     * Returns {@code true} if the VM has been shutdown
     */
    // 判断VM是否已关闭
    public static boolean isShutdown() {
        return initLevel == SYSTEM_SHUTDOWN;
    }
    
    /**
     * Set shutdown state.  Shutdown completes when all registered shutdown
     * hooks have been run.
     *
     * @see java.lang.Shutdown
     */
    // 标记VM已关闭
    public static void shutdown() {
        initLevel(SYSTEM_SHUTDOWN);
    }
    
    /**
     * Returns the maximum amount of allocatable direct buffer memory.
     * The directMemory variable is initialized during system initialization in the saveAndRemoveProperties method.
     */
    // 返回直接缓冲区默认大小
    public static long maxDirectMemory() {
        return directMemory;
    }
    
    /**
     * Returns {@code true} if the direct buffers should be page aligned.
     * This variable is initialized by saveAndRemoveProperties.
     */
    // 判断直接缓冲区是否需要页对齐
    public static boolean isDirectMemoryPageAligned() {
        return pageAlignDirectMemory;
    }
    
    /**
     * Returns true if the given class loader is the bootstrap class loader or the platform class loader.
     */
    // 判断给定的类加载器是否为bootstrap类加载器或为platform类加载器
    public static boolean isSystemDomainLoader(ClassLoader loader) {
        return loader == null || loader == ClassLoader.getPlatformClassLoader();
    }
    
    /**
     * Returns the system property of the specified key saved at
     * system initialization time.  This method should only be used
     * for the system properties that are not changed during runtime.
     *
     * Note that the saved system properties do not include
     * the ones set by java.lang.VersionProps.init().
     */
    // 从原始系统属性集中获取拥有指定key的属性
    public static String getSavedProperty(String key) {
        if(savedProps == null) {
            throw new IllegalStateException("Not yet initialized");
        }
        
        return savedProps.get(key);
    }
    
    /**
     * Gets an unmodifiable view of the system properties saved at system
     * initialization time. This method should only be used
     * for the system properties that are not changed during runtime.
     *
     * Note that the saved system properties do not include
     * the ones set by java.lang.VersionProps.init().
     */
    // 返回虚拟机中保存的初始系统属性集
    public static Map<String, String> getSavedProperties() {
        if(savedProps == null) {
            throw new IllegalStateException("Not yet initialized");
        }
        
        return savedProps;
    }
    
    /**
     * Save a private copy of the system properties and remove the system properties that are not intended for public access.
     * This method can only be invoked during system initialization.
     */
    // 将指定的系统属性集设置给虚拟机，并移除几条可以外部设置的属性
    public static void saveAndRemoveProperties(Properties props) {
        if(initLevel() != 0) {
            throw new IllegalStateException("Wrong init level");
        }
        
        @SuppressWarnings({"unchecked"})
        Map<String, String> sp = Map.ofEntries(props.entrySet().toArray(new Map.Entry[0]));
        
        /*
         * only main thread is running at this time,
         * so savedProps and its content will be correctly published to threads started later
         */
        savedProps = sp;
        
        /*
         * Set the maximum amount of direct memory.  This value is controlled
         * by the vm option -XX:MaxDirectMemorySize=<size>.
         * The maximum amount of allocatable direct buffer memory (in bytes)
         * from the system property sun.nio.MaxDirectMemorySize set by the VM.
         * The system property will be removed.
         */
        String s = (String) props.remove("sun.nio.MaxDirectMemorySize");
        
        // 如果成功移除(说明之前已经有该参数)
        if(s != null) {
            // 如果之前为给出-XX:MaxDirectMemorySize，这里为其设置默认值
            if(s.equals("-1")) {
                // -XX:MaxDirectMemorySize not given, take default
                directMemory = Runtime.getRuntime().maxMemory();
            } else {
                long l = Long.parseLong(s);
                if(l>-1) {
                    directMemory = l;
                }
            }
        }
        
        // Check if direct buffers should be page aligned
        s = (String) props.remove("sun.nio.PageAlignDirectMemory");
        if("true".equals(s)) {
            pageAlignDirectMemory = true;
        }
        
        // Remove other private system properties used by java.lang.Integer.IntegerCache
        props.remove("java.lang.Integer.IntegerCache.high");
        
        // used by sun.launcher.LauncherHelper
        props.remove("sun.java.launcher.diag");
        
        // used by jdk.internal.loader.ClassLoaders
        props.remove("jdk.boot.class.path.append");
    }
    
    /**
     * Initialize any miscellaneous operating system settings that need to be set for the class libraries.
     */
    // 设置一些操作系统相关的选项
    public static void initializeOSEnvironment() {
        if(initLevel() == 0) {
            OSEnvironment.initialize();
        }
    }
    
    /**
     * Gets the number of objects pending for finalization.
     *
     * @return the number of objects pending for finalization.
     */
    // 返回当前挂起(报废)的FinalReference数量
    public static int getFinalRefCount() {
        return finalRefCount;
    }
    
    /**
     * Gets the peak number of objects pending for finalization.
     *
     * @return the peak number of objects pending for finalization.
     */
    // 返回之前挂起(报废)的FinalReference数量的最大值
    public static int getPeakFinalRefCount() {
        return peakFinalRefCount;
    }
    
    /**
     * Add {@code n} to the objects pending for finalization count.
     *
     * @param n an integer value to be added to the objects pending for finalization count
     */
    // 增加/减少挂起(待处理)的FinalReference数量
    public static void addFinalRefCount(int n) {
        // The caller must hold lock to synchronize the update.
        finalRefCount += n;
        
        if(finalRefCount>peakFinalRefCount) {
            peakFinalRefCount = finalRefCount;
        }
    }
    
    /**
     * Returns Thread.State for the given threadStatus
     */
    // 获取指定线程的状态信息(将虚拟机中的状态码转换为Java层的状态码)
    public static Thread.State toThreadState(int threadStatus) {
        if((threadStatus & JVMTI_THREAD_STATE_RUNNABLE) != 0) {
            return RUNNABLE;
        } else if((threadStatus & JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER) != 0) {
            return BLOCKED;
        } else if((threadStatus & JVMTI_THREAD_STATE_WAITING_INDEFINITELY) != 0) {
            return WAITING;
        } else if((threadStatus & JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT) != 0) {
            return TIMED_WAITING;
        } else if((threadStatus & JVMTI_THREAD_STATE_TERMINATED) != 0) {
            return TERMINATED;
        } else if((threadStatus & JVMTI_THREAD_STATE_ALIVE) == 0) {
            return NEW;
        } else {
            return RUNNABLE;
        }
    }
    
    /**
     * Get a nanosecond time stamp adjustment in the form of a single long.
     *
     * This value can be used to create an instant using
     * {@link java.time.Instant#ofEpochSecond(long, long)
     * java.time.Instant.ofEpochSecond(offsetInSeconds,
     * getNanoTimeAdjustment(offsetInSeconds))}.
     * <p>
     * The value returned has the best resolution available to the JVM on
     * the current system.
     * This is usually down to microseconds - or tenth of microseconds -
     * depending on the OS/Hardware and the JVM implementation.
     *
     * @param offsetInSeconds The offset in seconds from which the nanosecond
     *                        time stamp should be computed.
     *
     * @return A nanosecond time stamp adjustment in the form of a single long.
     * If the offset is too far off the current time, this method returns -1.
     * In that case, the caller should call this method again, passing a
     * more accurate offset.
     *
     * @apiNote The offset should be recent enough - so that
     * {@code offsetInSeconds} is within {@code +/- 2^32} seconds of the
     * current UTC time. If the offset is too far off, {@code -1} will be
     * returned. As such, {@code -1} must not be considered as a valid
     * nano time adjustment, but as an exception value indicating
     * that an offset closer to the current time should be used.
     */
    public static native long getNanoTimeAdjustment(long offsetInSeconds);
    
    /**
     * Returns the VM arguments for this runtime environment.
     *
     * @implNote The HotSpot JVM processes the input arguments from multiple sources in the following order:
     * 1. JAVA_TOOL_OPTIONS environment variable
     * 2. Options from JNI Invocation API
     * 3. _JAVA_OPTIONS environment variable
     *
     * If VM options file is specified via -XX:VMOptionsFile, the vm options file is read and expanded in place of -XX:VMOptionFile option.
     */
    public static native String[] getRuntimeArguments();
    
    /**
     * Returns the first user-defined class loader up the execution stack,
     * or the platform class loader if only code from the platform or bootstrap class loader is on the stack.
     */
    public static ClassLoader latestUserDefinedLoader() {
        ClassLoader loader = latestUserDefinedLoader0();
        return loader != null ? loader : ClassLoader.getPlatformClassLoader();
    }
    
    /**
     * Returns {@code true} if we are in a set UID program.
     */
    public static boolean isSetUID() {
        long uid = getuid();
        long euid = geteuid();
        long gid = getgid();
        long egid = getegid();
        return uid != euid || gid != egid;
    }
    
    /**
     * Returns the real user ID of the calling process, or -1 if the value is not available.
     */
    public static native long getuid();
    
    /**
     * Returns the effective user ID of the calling process, or -1 if the value is not available.
     */
    public static native long geteuid();
    
    /**
     * Returns the real group ID of the calling process, or -1 if the value is not available.
     */
    public static native long getgid();
    
    /**
     * Returns the effective group ID of the calling process, or -1 if the value is not available.
     */
    public static native long getegid();
    
    
    /*
     * Returns the first user-defined class loader up the execution stack,
     * or null if only code from the platform or bootstrap class loader is
     * on the stack.  VM does not keep a reference of platform loader and so
     * it returns null.
     *
     * This method should be replaced with StackWalker::walk and then we can
     * remove the logic in the VM.
     */
    private static native ClassLoader latestUserDefinedLoader0();
    
    private static native void initialize();
    
}
