/*
 * Copyright (c) 1994, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;
import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.misc.TerminatingThreadLocal;
import jdk.internal.misc.VM;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.nio.ch.Interruptible;
import sun.security.util.SecurityConstants;

/**
 * A <i>thread</i> is a thread of execution in a program. The Java
 * Virtual Machine allows an application to have multiple threads of
 * execution running concurrently.
 * <p>
 * Every thread has a priority. Threads with higher priority are
 * executed in preference to threads with lower priority. Each thread
 * may or may not also be marked as a daemon. When code running in
 * some thread creates a new {@code Thread} object, the new
 * thread has its priority initially set equal to the priority of the
 * creating thread, and is a daemon thread if and only if the
 * creating thread is a daemon.
 * <p>
 * When a Java Virtual Machine starts up, there is usually a single
 * non-daemon thread (which typically calls the method named
 * {@code main} of some designated class). The Java Virtual
 * Machine continues to execute threads until either of the following
 * occurs:
 * <ul>
 * <li>The {@code exit} method of class {@code Runtime} has been
 * called and the security manager has permitted the exit operation
 * to take place.
 * <li>All threads that are not daemon threads have died, either by
 * returning from the call to the {@code run} method or by
 * throwing an exception that propagates beyond the {@code run}
 * method.
 * </ul>
 * <p>
 * There are two ways to create a new thread of execution. One is to
 * declare a class to be a subclass of {@code Thread}. This
 * subclass should override the {@code run} method of class
 * {@code Thread}. An instance of the subclass can then be
 * allocated and started. For example, a thread that computes primes
 * larger than a stated value could be written as follows:
 * <hr><blockquote><pre>
 *     class PrimeThread extends Thread {
 *         long minPrime;
 *         PrimeThread(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * <blockquote><pre>
 *     PrimeThread p = new PrimeThread(143);
 *     p.start();
 * </pre></blockquote>
 * <p>
 * The other way to create a thread is to declare a class that
 * implements the {@code Runnable} interface. That class then
 * implements the {@code run} method. An instance of the class can
 * then be allocated, passed as an argument when creating
 * {@code Thread}, and started. The same example in this other
 * style looks like the following:
 * <hr><blockquote><pre>
 *     class PrimeRun implements Runnable {
 *         long minPrime;
 *         PrimeRun(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * <blockquote><pre>
 *     PrimeRun p = new PrimeRun(143);
 *     new Thread(p).start();
 * </pre></blockquote>
 * <p>
 * Every thread has a name for identification purposes. More than
 * one thread may have the same name. If a name is not specified when
 * a thread is created, a new name is generated for it.
 * <p>
 * Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * @author unascribed
 * @see Runnable
 * @see Runtime#exit(int)
 * @see #run()
 * @see #stop()
 * @since 1.0
 */
// 线程
public class Thread implements Runnable {
    
    /**
     * The minimum priority that a thread can have.
     */
    public static final int MIN_PRIORITY = 1;   // 线程最小优先级
    /**
     * The default priority that is assigned to a thread.
     */
    public static final int NORM_PRIORITY = 5;  // 线程默认优先级
    /**
     * The maximum priority that a thread can have.
     */
    public static final int MAX_PRIORITY = 10;  // 线程最大优先级
    
    
    /*▼ 线程属性 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┓ */
    
    private volatile String name; // 线程名称
    
    private ThreadGroup group; // 当前线程所处的线程组
    
    private int priority; // 线程优先级
    
    /** Whether or not the thread is a daemon thread. */
    private boolean daemon = false; // 当前线程是否为守护线程，默认与父线程属性一致
    
    /**
     * Thread ID
     */
    private final long tid; // 线程ID
    
    /** What will be run. */
    private Runnable target; // 当前线程将要执行的动作
    
    /** The context ClassLoader for this thread */
    private ClassLoader contextClassLoader; // 线程上下文类加载器
    
    /** The inherited AccessControlContext of this thread */
    private AccessControlContext inheritedAccessControlContext; // 此线程继承的AccessControlContext
    
    /**
     * The requested stack size for this thread, or 0 if the creator did not specify a stack size.
     * It is up to the VM to do whatever it likes with this number; some VMs will ignore it.
     */
    private final long stackSize;   // 设置当前线程的栈帧深度（不是所有系统都支持）
    
    
    /** For autonumbering anonymous threads. */
    private static int threadInitNumber; // 下一个线程编号，用于合成线程名
    
    /** For generating thread ID */
    private static long threadSeqNumber; // 下一个线程ID
    
    /*▲ 线程属性 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┛ */
    
    
    // 除非显式设置，否则为空，用于处理未捕获的异常的接口对象
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    
    // 除非显式设置，否则为空，用于处理未捕获的异常的接口对象
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;
    
    
    /**
     * Java thread status for tools, default indicates thread 'not yet started'
     */
    private volatile int threadStatus;  // 线程状态
    
    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];  // 空栈帧
    
    /**
     * The argument supplied to the current call to java.util.concurrent.locks.LockSupport.park.
     * Set by (private) java.util.concurrent.locks.LockSupport.setBlocker
     * Accessed using java.util.concurrent.locks.LockSupport.getBlocker
     */
    volatile Object parkBlocker;    // 此对象不为null时说明线程进入了park（阻塞）状态，参见LockSupport
    
    /**
     * The object in which this thread is blocked in an interruptible I/O operation, if any.
     * The blocker's interrupt method should be invoked after setting this thread's interrupt status.
     */
    // 线程中断回调标记，设置此标记后，可在线程被中断时调用标记对象的回调方法
    private volatile Interruptible blocker;
    
    // 临时使用的锁，在设置/获取线程中断回调标记时使用
    private final Object blockerLock = new Object();    // 中断线程时
    
    /**
     * ThreadLocal values pertaining to this thread.
     * This map is maintained by the ThreadLocal class.
     */
    // 线程局部缓存，这是一个键值对组合，为当前线程关联一些“独享”变量，ThreadLocal是key。
    ThreadLocal.ThreadLocalMap threadLocals = null;
    
    /**
     * InheritableThreadLocal values pertaining to this thread.
     * This map is maintained by the InheritableThreadLocal class.
     */
    // 从父线程继承而来的线程局部缓存，由InheritableThreadLocal维护
    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;
    
    
    /*
     * The following three initially uninitialized fields are exclusively managed by class java.util.concurrent.ThreadLocalRandom.
     * These fields are used to build the high-performance PRNGs in the concurrent code, and we can not risk accidental false sharing.
     * Hence, the fields are isolated with @Contended.
     *
     * 以下三个字段由java.util.concurrent.ThreadLocalRandom管理
     * 这些字段用于在并发代码中构建高性能非重复随机值
     */
    
    /** The current seed for a ThreadLocalRandom */
    // 本地化的原始种子
    @jdk.internal.vm.annotation.Contended("tlr")
    long threadLocalRandomSeed;
    /** Secondary seed isolated from public ThreadLocalRandom sequence */
    // 本地化的辅助种子
    @jdk.internal.vm.annotation.Contended("tlr")
    int threadLocalRandomSecondarySeed;
    /** Probe hash value; nonzero if threadLocalRandomSeed initialized */
    // 本地化的探测值，如果ThreadLocalRandom已经初始化，则该值不为0
    @jdk.internal.vm.annotation.Contended("tlr")
    int threadLocalRandomProbe;
    
    
    /* 以下字段由虚拟机设置 */
    
    /** Fields reserved for exclusive use by the JVM */
    private boolean stillborn = false;
    
    /** JVM-private state that persists after native thread termination. */
    private long nativeParkEventPointer;
    
    private long eetop;
    
    
    
    static {
        registerNatives();
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup, Runnable, String) Thread}
     * {@code (null, null, gname)}, where {@code gname} is a newly generated
     * name. Automatically generated names are of the form
     * {@code "Thread-"+}<i>n</i>, where <i>n</i> is an integer.
     */
    // ▶ 1-1-1
    public Thread() {
        this(null, null, "Thread-" + nextThreadNum(), 0);
    }
    
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup, Runnable, String) Thread}
     * {@code (null, target, gname)}, where {@code gname} is a newly generated
     * name. Automatically generated names are of the form
     * {@code "Thread-"+}<i>n</i>, where <i>n</i> is an integer.
     *
     * @param target the object whose {@code run} method is invoked when this thread
     *               is started. If {@code null}, this classes {@code run} method does
     *               nothing.
     */
    // ▶ 1-1-2
    public Thread(Runnable target) {
        this(null, target, "Thread-" + nextThreadNum(), 0);
    }
    
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup, Runnable, String) Thread}
     * {@code (group, target, gname)} ,where {@code gname} is a newly generated
     * name. Automatically generated names are of the form
     * {@code "Thread-"+}<i>n</i>, where <i>n</i> is an integer.
     *
     * @param group  the thread group. If {@code null} and there is a security
     *               manager, the group is determined by {@linkplain
     *               SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *               If there is not a security manager or {@code
     *               SecurityManager.getThreadGroup()} returns {@code null}, the group
     *               is set to the current thread's thread group.
     * @param target the object whose {@code run} method is invoked when this thread
     *               is started. If {@code null}, this thread's run method is invoked.
     *
     * @throws SecurityException if the current thread cannot create a thread in the specified
     *                           thread group
     */
    // ▶ 1-1-3
    public Thread(ThreadGroup group, Runnable target) {
        this(group, target, "Thread-" + nextThreadNum(), 0);
    }
    
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup, Runnable, String) Thread}
     * {@code (null, null, name)}.
     *
     * @param name the name of the new thread
     */
    // ▶ 1-1-4
    public Thread(String name) {
        this(null, null, name, 0);
    }
    
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup, Runnable, String) Thread}
     * {@code (group, null, name)}.
     *
     * @param group the thread group. If {@code null} and there is a security
     *              manager, the group is determined by {@linkplain
     *              SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *              If there is not a security manager or {@code
     *              SecurityManager.getThreadGroup()} returns {@code null}, the group
     *              is set to the current thread's thread group.
     * @param name  the name of the new thread
     *
     * @throws SecurityException if the current thread cannot create a thread in the specified
     *                           thread group
     */
    // ▶ 1-1-5
    public Thread(ThreadGroup group, String name) {
        this(group, null, name, 0);
    }
    
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup, Runnable, String) Thread}
     * {@code (null, target, name)}.
     *
     * @param target the object whose {@code run} method is invoked when this thread
     *               is started. If {@code null}, this thread's run method is invoked.
     * @param name   the name of the new thread
     */
    // ▶ 1-1-6
    public Thread(Runnable target, String name) {
        this(null, target, name, 0);
    }
    
    /**
     * Allocates a new {@code Thread} object so that it has {@code target}
     * as its run object, has the specified {@code name} as its name,
     * and belongs to the thread group referred to by {@code group}.
     *
     * <p>If there is a security manager, its
     * {@link SecurityManager#checkAccess(ThreadGroup) checkAccess}
     * method is invoked with the ThreadGroup as its argument.
     *
     * <p>In addition, its {@code checkPermission} method is invoked with
     * the {@code RuntimePermission("enableContextClassLoaderOverride")}
     * permission when invoked directly or indirectly by the constructor
     * of a subclass which overrides the {@code getContextClassLoader}
     * or {@code setContextClassLoader} methods.
     *
     * <p>The priority of the newly created thread is set equal to the
     * priority of the thread creating it, that is, the currently running
     * thread. The method {@linkplain #setPriority setPriority} may be
     * used to change the priority to a new value.
     *
     * <p>The newly created thread is initially marked as being a daemon
     * thread if and only if the thread creating it is currently marked
     * as a daemon thread. The method {@linkplain #setDaemon setDaemon}
     * may be used to change whether or not a thread is a daemon.
     *
     * @param group  the thread group. If {@code null} and there is a security
     *               manager, the group is determined by {@linkplain
     *               SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *               If there is not a security manager or {@code
     *               SecurityManager.getThreadGroup()} returns {@code null}, the group
     *               is set to the current thread's thread group.
     * @param target the object whose {@code run} method is invoked when this thread
     *               is started. If {@code null}, this thread's run method is invoked.
     * @param name   the name of the new thread
     *
     * @throws SecurityException if the current thread cannot create a thread in the specified
     *                           thread group or cannot override the context class loader methods.
     */
    // ▶ 1-1-7
    public Thread(ThreadGroup group, Runnable target, String name) {
        this(group, target, name, 0);
    }
    
    /**
     * Allocates a new {@code Thread} object so that it has {@code target}
     * as its run object, has the specified {@code name} as its name,
     * and belongs to the thread group referred to by {@code group}, and has
     * the specified <i>stack size</i>.
     *
     * <p>This constructor is identical to {@link
     * #Thread(ThreadGroup, Runnable, String)} with the exception of the fact
     * that it allows the thread stack size to be specified.  The stack size
     * is the approximate number of bytes of address space that the virtual
     * machine is to allocate for this thread's stack.  <b>The effect of the
     * {@code stackSize} parameter, if any, is highly platform dependent.</b>
     *
     * <p>On some platforms, specifying a higher value for the
     * {@code stackSize} parameter may allow a thread to achieve greater
     * recursion depth before throwing a {@link StackOverflowError}.
     * Similarly, specifying a lower value may allow a greater number of
     * threads to exist concurrently without throwing an {@link
     * OutOfMemoryError} (or other internal error).  The details of
     * the relationship between the value of the {@code stackSize} parameter
     * and the maximum recursion depth and concurrency level are
     * platform-dependent.  <b>On some platforms, the value of the
     * {@code stackSize} parameter may have no effect whatsoever.</b>
     *
     * <p>The virtual machine is free to treat the {@code stackSize}
     * parameter as a suggestion.  If the specified value is unreasonably low
     * for the platform, the virtual machine may instead use some
     * platform-specific minimum value; if the specified value is unreasonably
     * high, the virtual machine may instead use some platform-specific
     * maximum.  Likewise, the virtual machine is free to round the specified
     * value up or down as it sees fit (or to ignore it completely).
     *
     * <p>Specifying a value of zero for the {@code stackSize} parameter will
     * cause this constructor to behave exactly like the
     * {@code Thread(ThreadGroup, Runnable, String)} constructor.
     *
     * <p><i>Due to the platform-dependent nature of the behavior of this
     * constructor, extreme care should be exercised in its use.
     * The thread stack size necessary to perform a given computation will
     * likely vary from one JRE implementation to another.  In light of this
     * variation, careful tuning of the stack size parameter may be required,
     * and the tuning may need to be repeated for each JRE implementation on
     * which an application is to run.</i>
     *
     * <p>Implementation note: Java platform implementers are encouraged to
     * document their implementation's behavior with respect to the
     * {@code stackSize} parameter.
     *
     * @param group     the thread group. If {@code null} and there is a security
     *                  manager, the group is determined by {@linkplain
     *                  SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *                  If there is not a security manager or {@code
     *                  SecurityManager.getThreadGroup()} returns {@code null}, the group
     *                  is set to the current thread's thread group.
     * @param target    the object whose {@code run} method is invoked when this thread
     *                  is started. If {@code null}, this thread's run method is invoked.
     * @param name      the name of the new thread
     * @param stackSize the desired stack size for the new thread, or zero to indicate
     *                  that this parameter is to be ignored.
     *
     * @throws SecurityException if the current thread cannot create a thread in the specified
     *                           thread group
     * @since 1.4
     */
    // ▶ 1-1
    public Thread(ThreadGroup group, Runnable target, String name, long stackSize) {
        this(group, target, name, stackSize, null, true);
    }
    
    /**
     * Allocates a new {@code Thread} object so that it has {@code target}
     * as its run object, has the specified {@code name} as its name,
     * belongs to the thread group referred to by {@code group}, has
     * the specified {@code stackSize}, and inherits initial values for
     * {@linkplain InheritableThreadLocal inheritable thread-local} variables
     * if {@code inheritThreadLocals} is {@code true}.
     *
     * <p> This constructor is identical to {@link
     * #Thread(ThreadGroup, Runnable, String, long)} with the added ability to
     * suppress, or not, the inheriting of initial values for inheritable
     * thread-local variables from the constructing thread. This allows for
     * finer grain control over inheritable thread-locals. Care must be taken
     * when passing a value of {@code false} for {@code inheritThreadLocals},
     * as it may lead to unexpected behavior if the new thread executes code
     * that expects a specific thread-local value to be inherited.
     *
     * <p> Specifying a value of {@code true} for the {@code inheritThreadLocals}
     * parameter will cause this constructor to behave exactly like the
     * {@code Thread(ThreadGroup, Runnable, String, long)} constructor.
     *
     * @param group               the thread group. If {@code null} and there is a security
     *                            manager, the group is determined by {@linkplain
     *                            SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *                            If there is not a security manager or {@code
     *                            SecurityManager.getThreadGroup()} returns {@code null}, the group
     *                            is set to the current thread's thread group.
     * @param target              the object whose {@code run} method is invoked when this thread
     *                            is started. If {@code null}, this thread's run method is invoked.
     * @param name                the name of the new thread
     * @param stackSize           the desired stack size for the new thread, or zero to indicate
     *                            that this parameter is to be ignored
     * @param inheritThreadLocals if {@code true}, inherit initial values for inheritable
     *                            thread-locals from the constructing thread, otherwise no initial
     *                            values are inherited
     *
     * @throws SecurityException if the current thread cannot create a thread in the specified
     *                           thread group
     * @since 9
     */
    // ▶ 1-2
    public Thread(ThreadGroup group, Runnable target, String name, long stackSize, boolean inheritThreadLocals) {
        this(group, target, name, stackSize, null, inheritThreadLocals);
    }
    
    /**
     * Creates a new Thread that inherits the given AccessControlContext
     * but thread-local variables are not inherited.
     * This is not a public constructor.
     */
    // ▶ 1-3
    Thread(Runnable target, AccessControlContext acc) {
        this(null, target, "Thread-" + nextThreadNum(), 0, acc, false);
    }
    
    /**
     * Initializes a Thread.
     *
     * @param g                   the Thread group
     * @param target              the object whose run() method gets called
     * @param name                the name of the new Thread
     * @param stackSize           the desired stack size for the new thread, or
     *                            zero to indicate that this parameter is to be ignored.
     * @param acc                 the AccessControlContext to inherit, or
     *                            AccessController.getContext() if null
     * @param inheritThreadLocals if {@code true}, inherit initial values for
     *                            inheritable thread-locals from the constructing thread
     */
    // ▶ 1
    private Thread(ThreadGroup g, Runnable target, String name, long stackSize, AccessControlContext acc, boolean inheritThreadLocals) {
        // 线程必须有名称，没有主动设置的话就使用默认名称
        if(name == null) {
            throw new NullPointerException("name cannot be null");
        }
        
        this.name = name; // 线程名称
        
        Thread parent = currentThread();
        SecurityManager security = System.getSecurityManager();
        if(g == null) {
            /* Determine if it's an applet or not */
            
            // If there is a security manager, ask the security manager what to do.
            if(security != null) {
                g = security.getThreadGroup();
            }
            
            // If the security manager doesn't have a strong opinion on the matter, use the parent thread group.
            if(g == null) {
                g = parent.getThreadGroup();
            }
        }
        
        // checkAccess regardless of whether or not threadgroup is explicitly passed in.
        g.checkAccess();
        
        // Do we have the required permissions?
        if(security != null) {
            if(isCCLOverridden(getClass())) {
                security.checkPermission(SecurityConstants.SUBCLASS_IMPLEMENTATION_PERMISSION);
            }
        }
        
        // 将当前线程视为未启动线程，并在其线程组中计数
        g.addUnstarted();
        
        this.group = g; // 线程组
        
        this.daemon = parent.isDaemon(); // 守护线程
        
        this.priority = parent.getPriority(); // 线程优先级
        
        if(security == null || isCCLOverridden(parent.getClass())) {
            this.contextClassLoader = parent.getContextClassLoader();
        } else {
            this.contextClassLoader = parent.contextClassLoader;
        }
        
        this.inheritedAccessControlContext = acc != null ? acc : AccessController.getContext(); // 此线程继承的AccessControlContext
        
        this.target = target; // 当前线程将要执行的动作
        
        setPriority(priority);
        
        // 如果需要继承父线程的键值对组合<ThreadLocal, Object>，且该键值对存在
        if(inheritThreadLocals && parent.inheritableThreadLocals != null) {
            // 创建新的map，并继承父线程的数据
            this.inheritableThreadLocals = ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        }
        
        /* Stash the specified stack size in case the VM cares */
        this.stackSize = stackSize;
        
        /* Set thread ID */
        this.tid = nextThreadID();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取线程 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns a reference to the currently executing thread object.
     *
     * @return the currently executing thread.
     */
    // 返回调用此方法的当前线程
    @HotSpotIntrinsicCandidate
    public static native Thread currentThread();
    
    /*▲ 获取线程 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 线程属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns this thread's name.
     *
     * @return this thread's name.
     *
     * @see #setName(String)
     */
    public final String getName() {
        return name;
    }
    
    /**
     * Changes the name of this thread to be equal to the argument {@code name}.
     * <p>
     * First the {@code checkAccess} method of this thread is called
     * with no arguments. This may result in throwing a
     * {@code SecurityException}.
     *
     * @param name the new name for this thread.
     *
     * @throws SecurityException if the current thread cannot modify this
     *                           thread.
     * @see #getName
     * @see #checkAccess()
     */
    public final synchronized void setName(String name) {
        checkAccess();
        
        if(name == null) {
            throw new NullPointerException("name cannot be null");
        }
        
        this.name = name;
        
        if(threadStatus != 0) {
            setNativeName(name);
        }
    }
    
    /**
     * Returns the thread group to which this thread belongs.
     * This method returns null if this thread has died
     * (been stopped).
     *
     * @return this thread's thread group.
     */
    public final ThreadGroup getThreadGroup() {
        return group;
    }
    
    /**
     * Tests if this thread is a daemon thread.
     *
     * @return {@code true} if this thread is a daemon thread;
     * {@code false} otherwise.
     *
     * @see #setDaemon(boolean)
     */
    // 返回true代表当前线程是守护线程
    public final boolean isDaemon() {
        return daemon;
    }
    
    /**
     * Marks this thread as either a {@linkplain #isDaemon daemon} thread
     * or a user thread. The Java Virtual Machine exits when the only
     * threads running are all daemon threads.
     *
     * <p> This method must be invoked before the thread is started.
     *
     * @param on if {@code true}, marks this thread as a daemon thread
     *
     * @throws IllegalThreadStateException if this thread is {@linkplain #isAlive alive}
     * @throws SecurityException           if {@link #checkAccess} determines that the current
     *                                     thread cannot modify this thread
     */
    // 设置当前线程为守护线程/非守护线程
    public final void setDaemon(boolean on) {
        checkAccess();
        if(isAlive()) {
            throw new IllegalThreadStateException();
        }
        daemon = on;
    }
    
    /**
     * Returns this thread's priority.
     *
     * @return this thread's priority.
     *
     * @see #setPriority
     */
    // 返回线程优先级
    public final int getPriority() {
        return priority;
    }
    
    /**
     * Changes the priority of this thread.
     * <p>
     * First the {@code checkAccess} method of this thread is called
     * with no arguments. This may result in throwing a {@code SecurityException}.
     * <p>
     * Otherwise, the priority of this thread is set to the smaller of
     * the specified {@code newPriority} and the maximum permitted
     * priority of the thread's thread group.
     *
     * @param newPriority priority to set this thread to
     *
     * @throws IllegalArgumentException If the priority is not in the
     *                                  range {@code MIN_PRIORITY} to
     *                                  {@code MAX_PRIORITY}.
     * @throws SecurityException        if the current thread cannot modify
     *                                  this thread.
     * @see #getPriority
     * @see #checkAccess()
     * @see #getThreadGroup()
     * @see #MAX_PRIORITY
     * @see #MIN_PRIORITY
     * @see ThreadGroup#getMaxPriority()
     */
    // 设置线程优先级
    public final void setPriority(int newPriority) {
        ThreadGroup g;
        
        checkAccess();
        
        if(newPriority>MAX_PRIORITY || newPriority<MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        
        if((g = getThreadGroup()) != null) {
            if(newPriority>g.getMaxPriority()) {
                newPriority = g.getMaxPriority();
            }
            
            setPriority0(priority = newPriority);
        }
    }
    
    /**
     * Returns the identifier of this Thread.  The thread ID is a positive
     * {@code long} number generated when this thread was created.
     * The thread ID is unique and remains unchanged during its lifetime.
     * When a thread is terminated, this thread ID may be reused.
     *
     * @return this thread's ID.
     *
     * @since 1.5
     */
    // 获取线程ID
    public long getId() {
        return tid;
    }
    
    /**
     * Returns the context {@code ClassLoader} for this thread. The context
     * {@code ClassLoader} is provided by the creator of the thread for use
     * by code running in this thread when loading classes and resources.
     * If not {@linkplain #setContextClassLoader set}, the default is the
     * {@code ClassLoader} context of the parent thread. The context
     * {@code ClassLoader} of the
     * primordial thread is typically set to the class loader used to load the
     * application.
     *
     * @return the context {@code ClassLoader} for this thread, or {@code null}
     * indicating the system class loader (or, failing that, the
     * bootstrap class loader)
     *
     * @throws SecurityException if a security manager is present, and the caller's class loader
     *                           is not {@code null} and is not the same as or an ancestor of the
     *                           context class loader, and the caller does not have the
     *                           {@link RuntimePermission}{@code ("getClassLoader")}
     * @since 1.2
     */
    // 获取当前线程上下文类加载器
    @CallerSensitive
    public ClassLoader getContextClassLoader() {
        if(contextClassLoader == null) {
            return null;
        }
        
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            ClassLoader.checkClassLoaderPermission(contextClassLoader, Reflection.getCallerClass());
        }
        
        return contextClassLoader;
    }
    
    /**
     * Sets the context ClassLoader for this Thread. The context
     * ClassLoader can be set when a thread is created, and allows
     * the creator of the thread to provide the appropriate class loader,
     * through {@code getContextClassLoader}, to code running in the thread
     * when loading classes and resources.
     *
     * <p>If a security manager is present, its {@link
     * SecurityManager#checkPermission(java.security.Permission) checkPermission}
     * method is invoked with a {@link RuntimePermission RuntimePermission}{@code
     * ("setContextClassLoader")} permission to see if setting the context
     * ClassLoader is permitted.
     *
     * @param cl the context ClassLoader for this Thread, or null  indicating the
     *           system class loader (or, failing that, the bootstrap class loader)
     *
     * @throws SecurityException if the current thread cannot set the context ClassLoader
     * @since 1.2
     */
    // 设置线程上下文类加载器
    public void setContextClassLoader(ClassLoader cl) {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("setContextClassLoader"));
        }
    
        contextClassLoader = cl;
    }
    
    
    /* 以下方法用于构造器的默认行为 */
    
    // 获取下一个线程编号，用于合成线程名
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }
    
    // 获取下一个线程ID
    private static synchronized long nextThreadID() {
        return ++threadSeqNumber;
    }
    
    /*▲ 线程属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 线程状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Causes this thread to begin execution; the Java Virtual Machine
     * calls the {@code run} method of this thread.
     * <p>
     * The result is that two threads are running concurrently: the
     * current thread (which returns from the call to the
     * {@code start} method) and the other thread (which executes its
     * {@code run} method).
     * <p>
     * It is never legal to start a thread more than once.
     * In particular, a thread may not be restarted once it has completed
     * execution.
     *
     * @throws IllegalThreadStateException if the thread was already started.
     * @see #run()
     * @see #stop()
     */
    // 启动线程，线程状态从NEW进入RUNNABLE
    public synchronized void start() {
        /*
         * This method is not invoked for the main method thread or "system"
         * group threads created/set up by the VM. Any new functionality added
         * to this method in the future may have to also be added to the VM.
         *
         * A zero status value corresponds to state "NEW".
         */
        if(threadStatus != 0)
            throw new IllegalThreadStateException();
        
        /*
         * Notify the group that this thread is about to be started so that it can be added to the group's list of threads
         * and the group's unstarted count can be decremented.
         */
        // 将当前线程加入到所在的线程组，记录为活跃线程
        group.add(this);
        
        boolean started = false;
        
        try {
            start0();
            started = true;
        } finally {
            try {
                // 线程启动失败，将其从线程组中删除，未启动线程数量重新加一
                if(!started) {
                    group.threadStartFailed(this);
                }
            } catch(Throwable ignore) {
                // do nothing. If start0 threw a Throwable then it will be passed up the call stack
            }
        }
    }
    
    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds, subject to
     * the precision and accuracy of system timers and schedulers. The thread
     * does not lose ownership of any monitors.
     *
     * @param millis the length of time to sleep in milliseconds
     *
     * @throws IllegalArgumentException if the value of {@code millis} is negative
     * @throws InterruptedException     if any thread has interrupted the current thread. The
     *                                  <i>interrupted status</i> of the current thread is
     *                                  cleared when this exception is thrown.
     */
    // 使线程进入TIMED_WAITING状态，millis毫秒后自己醒来（不释放锁）
    public static native void sleep(long millis) throws InterruptedException;
    
    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds plus the specified
     * number of nanoseconds, subject to the precision and accuracy of system
     * timers and schedulers. The thread does not lose ownership of any
     * monitors.
     *
     * @param millis the length of time to sleep in milliseconds
     * @param nanos  {@code 0-999999} additional nanoseconds to sleep
     *
     * @throws IllegalArgumentException if the value of {@code millis} is negative, or the value of
     *                                  {@code nanos} is not in the range {@code 0-999999}
     * @throws InterruptedException     if any thread has interrupted the current thread. The
     *                                  <i>interrupted status</i> of the current thread is
     *                                  cleared when this exception is thrown.
     */
    /*
     * 使线程进入TIMED_WAITING状态
     * 至少等待millis毫秒，nanos是一个纳秒级的附加时间，用来微调millis参数（不释放锁）
     */
    public static void sleep(long millis, int nanos) throws InterruptedException {
        if(millis<0) {
            throw new IllegalArgumentException("timeout value is negative");
        }
        
        // 纳秒值的取值在1毫秒之内
        if(nanos<0 || nanos>999999) {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }
        
        // 类似四舍五入，近似到1毫秒
        if(nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }
        
        sleep(millis);
    }
    
    /**
     * Waits for this thread to die.
     *
     * <p> An invocation of this method behaves in exactly the same
     * way as the invocation
     *
     * <blockquote>
     * {@linkplain #join(long) join}{@code (0)}
     * </blockquote>
     *
     * @throws InterruptedException if any thread has interrupted the current thread. The
     *                              <i>interrupted status</i> of the current thread is
     *                              cleared when this exception is thrown.
     */
    /*
     * 使线程进入WAITING状态
     * 让join()方法所在线程进入等待，直到调用join()的线程死亡之后，再去执行join()方法所在线程
     */
    public final void join() throws InterruptedException {
        join(0);
    }
    
    /**
     * Waits at most {@code millis} milliseconds for this thread to
     * die. A timeout of {@code 0} means to wait forever.
     *
     * <p> This implementation uses a loop of {@code this.wait} calls
     * conditioned on {@code this.isAlive}. As a thread terminates the
     * {@code this.notifyAll} method is invoked. It is recommended that
     * applications not use {@code wait}, {@code notify}, or
     * {@code notifyAll} on {@code Thread} instances.
     *
     * @param millis the time to wait in milliseconds
     *
     * @throws IllegalArgumentException if the value of {@code millis} is negative
     * @throws InterruptedException     if any thread has interrupted the current thread. The
     *                                  <i>interrupted status</i> of the current thread is
     *                                  cleared when this exception is thrown.
     */
    // 使线程进入WAITING或TIMED_WAITING状态
    public final synchronized void join(long millis) throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;
        
        if(millis<0) {
            throw new IllegalArgumentException("timeout value is negative");
        }
        
        if(millis == 0) {
            while(isAlive()) {
                wait(0);
            }
        } else {
            while(isAlive()) {
                long delay = millis - now;
                if(delay<=0) {
                    break;
                }
                
                wait(delay);
                
                // 记录流逝的时间，保证中途不能被唤醒
                now = System.currentTimeMillis() - base;
            }
        }
    }
    
    /**
     * Waits at most {@code millis} milliseconds plus
     * {@code nanos} nanoseconds for this thread to die.
     *
     * <p> This implementation uses a loop of {@code this.wait} calls
     * conditioned on {@code this.isAlive}. As a thread terminates the
     * {@code this.notifyAll} method is invoked. It is recommended that
     * applications not use {@code wait}, {@code notify}, or
     * {@code notifyAll} on {@code Thread} instances.
     *
     * @param millis the time to wait in milliseconds
     * @param nanos  {@code 0-999999} additional nanoseconds to wait
     *
     * @throws IllegalArgumentException if the value of {@code millis} is negative, or the value
     *                                  of {@code nanos} is not in the range {@code 0-999999}
     * @throws InterruptedException     if any thread has interrupted the current thread. The
     *                                  <i>interrupted status</i> of the current thread is
     *                                  cleared when this exception is thrown.
     */
    // 使线程进入WAITING或TIMED_WAITING状态
    public final synchronized void join(long millis, int nanos) throws InterruptedException {
        
        if(millis<0) {
            throw new IllegalArgumentException("timeout value is negative");
        }
        
        // 纳秒的取值在1毫秒之内
        if(nanos<0 || nanos>999999) {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }
        
        // 类似四舍五入
        if(nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }
        
        join(millis);
    }
    
    /**
     * A hint to the scheduler that the current thread is willing to yield its current use of a processor.
     * The scheduler is free to ignore this hint.
     *
     * <p> Yield is a heuristic attempt to improve relative progression
     * between threads that would otherwise over-utilise a CPU. Its use
     * should be combined with detailed profiling and benchmarking to
     * ensure that it actually has the desired effect.
     *
     * <p> It is rarely appropriate to use this method. It may be useful
     * for debugging or testing purposes, where it may help to reproduce
     * bugs due to race conditions. It may also be useful when designing
     * concurrency control constructs such as the ones in the
     * {@link java.util.concurrent.locks} package.
     */
    // 当前线程让出CPU时间片，大家重新抢占执行权
    public static native void yield();
    
    /**
     * Interrupts this thread.
     *
     * <p> Unless the current thread is interrupting itself, which is
     * always permitted, the {@link #checkAccess() checkAccess} method
     * of this thread is invoked, which may cause a {@link
     * SecurityException} to be thrown.
     *
     * <p> If this thread is blocked in an invocation of the {@link
     * Object#wait() wait()}, {@link Object#wait(long) wait(long)}, or {@link
     * Object#wait(long, int) wait(long, int)} methods of the {@link Object}
     * class, or of the {@link #join()}, {@link #join(long)}, {@link
     * #join(long, int)}, {@link #sleep(long)}, or {@link #sleep(long, int)},
     * methods of this class, then its interrupt status will be cleared and it
     * will receive an {@link InterruptedException}.
     *
     * <p> If this thread is blocked in an I/O operation upon an {@link
     * java.nio.channels.InterruptibleChannel InterruptibleChannel}
     * then the channel will be closed, the thread's interrupt
     * status will be set, and the thread will receive a {@link
     * java.nio.channels.ClosedByInterruptException}.
     *
     * <p> If this thread is blocked in a {@link java.nio.channels.Selector}
     * then the thread's interrupt status will be set and it will return
     * immediately from the selection operation, possibly with a non-zero
     * value, just as if the selector's {@link
     * java.nio.channels.Selector#wakeup wakeup} method were invoked.
     *
     * <p> If none of the previous conditions hold then this thread's interrupt
     * status will be set. </p>
     *
     * <p> Interrupting a thread that is not alive need not have any effect.
     *
     * @throws SecurityException if the current thread cannot modify this thread
     * @revised 6.0
     * @spec JSR-51
     */
    // 中断线程（只是给线程预设一个标记，不是立即让线程停下来）
    public void interrupt() {
        // 如果由别的线程对当前线程发起中断
        if(this != Thread.currentThread()) {
            checkAccess();
            
            // thread may be blocked in an I/O operation
            synchronized(blockerLock) {
                Interruptible b = blocker;
                // 如果存在线程中断回调标记
                if(b != null) {
                    interrupt0();  // set interrupt status
                    b.interrupt(this);
                    return;
                }
            }
        }
        
        // set interrupt status
        interrupt0();
    }
    
    /**
     * Set the blocker field; invoked via jdk.internal.misc.SharedSecrets from java.nio code
     */
    // 为当前线程设置一个线程中断回调标记，以便在线程被中断时调用该标记的回调方法
    static void blockedOn(Interruptible b) {
        Thread me = Thread.currentThread();
        synchronized(me.blockerLock) {
            me.blocker = b;
        }
    }
    
    /**
     * Tests whether this thread has been interrupted.  The <i>interrupted
     * status</i> of the thread is unaffected by this method.
     *
     * <p>A thread interruption ignored because a thread was not alive
     * at the time of the interrupt will be reflected by this method
     * returning false.
     *
     * @return {@code true} if this thread has been interrupted;
     * {@code false} otherwise.
     *
     * @revised 6.0
     * @see #interrupted()
     */
    // （非静态）测试线程是否已经中断，线程的中断状态不受影响
    public boolean isInterrupted() {
        return isInterrupted(false);
    }
    
    /**
     * Tests whether the current thread has been interrupted.  The
     * <i>interrupted status</i> of the thread is cleared by this method.  In
     * other words, if this method were to be called twice in succession, the
     * second call would return false (unless the current thread were
     * interrupted again, after the first call had cleared its interrupted
     * status and before the second call had examined it).
     *
     * <p>A thread interruption ignored because a thread was not alive
     * at the time of the interrupt will be reflected by this method
     * returning false.
     *
     * @return {@code true} if the current thread has been interrupted;
     * {@code false} otherwise.
     *
     * @revised 6.0
     * @see #isInterrupted()
     */
    // （静态）测试当前线程是否已经中断，线程的中断状态会被清除
    public static boolean interrupted() {
        return currentThread().isInterrupted(true);
    }
    
    /**
     * Tests if this thread is alive. A thread is alive if it has been started and has not yet died.
     *
     * @return {@code true} if this thread is alive; {@code false} otherwise.
     */
    // 当前线程是否仍然存活（没有到达TERMINATED状态）
    public final native boolean isAlive();
    
    
    /**
     * Returns the state of this thread.
     * This method is designed for use in monitoring of the system state,
     * not for synchronization control.
     *
     * @return this thread's state.
     *
     * @since 1.5
     */
    // 返回当前线程所处的状态
    public State getState() {
        // get current thread state
        return VM.toThreadState(threadStatus);
    }
    
    
    /*
     * 一个线程不应该由其他线程来强制中断或停止，而是应该由线程自己自行停止。
     * 所以，以下状态方法已经废弃，不再推荐使用。
     * 建议使用interrupt()设置中断标记，然后让线程自身处理该中断位（决定中断还是停止还是继续运行）
     */
    
    
    /**
     * Forces the thread to stop executing.
     * <p>
     * If there is a security manager installed, its {@code checkAccess}
     * method is called with {@code this}
     * as its argument. This may result in a
     * {@code SecurityException} being raised (in the current thread).
     * <p>
     * If this thread is different from the current thread (that is, the current
     * thread is trying to stop a thread other than itself), the
     * security manager's {@code checkPermission} method (with a
     * {@code RuntimePermission("stopThread")} argument) is called in
     * addition.
     * Again, this may result in throwing a
     * {@code SecurityException} (in the current thread).
     * <p>
     * The thread represented by this thread is forced to stop whatever
     * it is doing abnormally and to throw a newly created
     * {@code ThreadDeath} object as an exception.
     * <p>
     * It is permitted to stop a thread that has not yet been started.
     * If the thread is eventually started, it immediately terminates.
     * <p>
     * An application should not normally try to catch
     * {@code ThreadDeath} unless it must do some extraordinary
     * cleanup operation (note that the throwing of
     * {@code ThreadDeath} causes {@code finally} clauses of
     * {@code try} statements to be executed before the thread
     * officially dies).  If a {@code catch} clause catches a
     * {@code ThreadDeath} object, it is important to rethrow the
     * object so that the thread actually dies.
     * <p>
     * The top-level error handler that reacts to otherwise uncaught
     * exceptions does not print out a message or otherwise notify the
     * application if the uncaught exception is an instance of
     * {@code ThreadDeath}.
     *
     * @throws SecurityException if the current thread cannot
     *                           modify this thread.
     * @see #interrupt()
     * @see #checkAccess()
     * @see #run()
     * @see #start()
     * @see ThreadDeath
     * @see ThreadGroup#uncaughtException(Thread, Throwable)
     * @see SecurityManager#checkAccess(Thread)
     * @see SecurityManager#checkPermission
     * @deprecated This method is inherently unsafe.  Stopping a thread with
     * Thread.stop causes it to unlock all of the monitors that it
     * has locked (as a natural consequence of the unchecked
     * {@code ThreadDeath} exception propagating up the stack).  If
     * any of the objects previously protected by these monitors were in
     * an inconsistent state, the damaged objects become visible to
     * other threads, potentially resulting in arbitrary behavior.  Many
     * uses of {@code stop} should be replaced by code that simply
     * modifies some variable to indicate that the target thread should
     * stop running.  The target thread should check this variable
     * regularly, and return from its run method in an orderly fashion
     * if the variable indicates that it is to stop running.  If the
     * target thread waits for long periods (on a condition variable,
     * for example), the {@code interrupt} method should be used to
     * interrupt the wait.
     * For more information, see
     * <a href="{@docRoot}/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html">Why
     * are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     */
    @Deprecated(since = "1.2")
    public final void stop() {
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            checkAccess();
            if(this != Thread.currentThread()) {
                security.checkPermission(SecurityConstants.STOP_THREAD_PERMISSION);
            }
        }
        
        // A zero status value corresponds to "NEW", it can't change to not-NEW because we hold the lock.
        if(threadStatus != 0) {
            resume(); // Wake up thread if it was suspended; no-op otherwise
        }
        
        // The VM can handle all thread states
        stop0(new ThreadDeath());
    }
    
    /**
     * Suspends this thread.
     * <p>
     * First, the {@code checkAccess} method of this thread is called
     * with no arguments. This may result in throwing a
     * {@code SecurityException }(in the current thread).
     * <p>
     * If the thread is alive, it is suspended and makes no further
     * progress unless and until it is resumed.
     *
     * @throws SecurityException if the current thread cannot modify
     *                           this thread.
     * @see #checkAccess
     * @deprecated This method has been deprecated, as it is
     * inherently deadlock-prone.  If the target thread holds a lock on the
     * monitor protecting a critical system resource when it is suspended, no
     * thread can access this resource until the target thread is resumed. If
     * the thread that would resume the target thread attempts to lock this
     * monitor prior to calling {@code resume}, deadlock results.  Such
     * deadlocks typically manifest themselves as "frozen" processes.
     * For more information, see
     * <a href="{@docRoot}/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html">Why
     * are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     */
    @Deprecated(since = "1.2")
    public final void suspend() {
        checkAccess();
        suspend0();
    }
    
    /**
     * Resumes a suspended thread.
     * <p>
     * First, the {@code checkAccess} method of this thread is called
     * with no arguments. This may result in throwing a
     * {@code SecurityException} (in the current thread).
     * <p>
     * If the thread is alive but suspended, it is resumed and is
     * permitted to make progress in its execution.
     *
     * @throws SecurityException if the current thread cannot modify this
     *                           thread.
     * @see #checkAccess
     * @see #suspend()
     * @deprecated This method exists solely for use with {@link #suspend},
     * which has been deprecated because it is deadlock-prone.
     * For more information, see
     * <a href="{@docRoot}/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html">Why
     * are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     */
    @Deprecated(since = "1.2")
    public final void resume() {
        checkAccess();
        resume0();
    }
    
    /*▲ 线程状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 线程动作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * If this thread was constructed using a separate
     * {@code Runnable} run object, then that
     * {@code Runnable} object's {@code run} method is called;
     * otherwise, this method does nothing and returns.
     * <p>
     * Subclasses of {@code Thread} should override this method.
     *
     * @see #start()
     * @see #stop()
     * @see #Thread(ThreadGroup, Runnable, String)
     */
    // 线程执行的动作
    @Override
    public void run() {
        if(target != null) {
            target.run();
        }
    }
    
    /*▲ 线程动作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 栈帧 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an array of stack trace elements representing the stack dump
     * of this thread.  This method will return a zero-length array if
     * this thread has not started, has started but has not yet been
     * scheduled to run by the system, or has terminated.
     * If the returned array is of non-zero length then the first element of
     * the array represents the top of the stack, which is the most recent
     * method invocation in the sequence.  The last element of the array
     * represents the bottom of the stack, which is the least recent method
     * invocation in the sequence.
     *
     * <p>If there is a security manager, and this thread is not
     * the current thread, then the security manager's
     * {@code checkPermission} method is called with a
     * {@code RuntimePermission("getStackTrace")} permission
     * to see if it's ok to get the stack trace.
     *
     * <p>Some virtual machines may, under some circumstances, omit one
     * or more stack frames from the stack trace.  In the extreme case,
     * a virtual machine that has no stack trace information concerning
     * this thread is permitted to return a zero-length array from this
     * method.
     *
     * @return an array of {@code StackTraceElement},
     * each represents one stack frame.
     *
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkPermission} method doesn't allow
     *                           getting the stack trace of thread.
     * @see SecurityManager#checkPermission
     * @see RuntimePermission
     * @see Throwable#getStackTrace
     * @since 1.5
     */
    // 当前线程中的栈帧
    public StackTraceElement[] getStackTrace() {
        if(this != Thread.currentThread()) {
            // check for getStackTrace permission
            SecurityManager security = System.getSecurityManager();
            if(security != null) {
                security.checkPermission(SecurityConstants.GET_STACK_TRACE_PERMISSION);
            }
            // optimization so we do not call into the vm for threads that have not yet started or have terminated
            if(!isAlive()) {
                return EMPTY_STACK_TRACE;
            }
            StackTraceElement[][] stackTraceArray = dumpThreads(new Thread[]{this});
            StackTraceElement[] stackTrace = stackTraceArray[0];
            // a thread that was alive during the previous isAlive call may have since terminated, therefore not having a stacktrace.
            if(stackTrace == null) {
                stackTrace = EMPTY_STACK_TRACE;
            }
            return stackTrace;
        } else {
            return (new Exception()).getStackTrace();
        }
    }
    
    /**
     * Returns a map of stack traces for all live threads.
     * The map keys are threads and each map value is an array of
     * {@code StackTraceElement} that represents the stack dump
     * of the corresponding {@code Thread}.
     * The returned stack traces are in the format specified for
     * the {@link #getStackTrace getStackTrace} method.
     *
     * <p>The threads may be executing while this method is called.
     * The stack trace of each thread only represents a snapshot and
     * each stack trace may be obtained at different time.  A zero-length
     * array will be returned in the map value if the virtual machine has
     * no stack trace information about a thread.
     *
     * <p>If there is a security manager, then the security manager's
     * {@code checkPermission} method is called with a
     * {@code RuntimePermission("getStackTrace")} permission as well as
     * {@code RuntimePermission("modifyThreadGroup")} permission
     * to see if it is ok to get the stack trace of all threads.
     *
     * @return a {@code Map} from {@code Thread} to an array of
     * {@code StackTraceElement} that represents the stack trace of
     * the corresponding thread.
     *
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkPermission} method doesn't allow
     *                           getting the stack trace of thread.
     * @see #getStackTrace
     * @see SecurityManager#checkPermission
     * @see RuntimePermission
     * @see Throwable#getStackTrace
     * @since 1.5
     */
    // 当前JVM中所有活跃线程的栈帧
    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        // check for getStackTrace permission
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkPermission(SecurityConstants.GET_STACK_TRACE_PERMISSION);
            security.checkPermission(SecurityConstants.MODIFY_THREADGROUP_PERMISSION);
        }
        
        // Get a snapshot of the list of all threads
        Thread[] threads = getThreads();
        StackTraceElement[][] traces = dumpThreads(threads);
        Map<Thread, StackTraceElement[]> m = new HashMap<>(threads.length);
        for(int i = 0; i<threads.length; i++) {
            StackTraceElement[] stackTrace = traces[i];
            if(stackTrace != null) {
                m.put(threads[i], stackTrace);
            }
            // else terminated so we don't put it in the map
        }
        return m;
    }
    
    private static native StackTraceElement[][] dumpThreads(Thread[] threads);
    
    /*▲ 栈帧 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 未捕获异常处理器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the default handler invoked when a thread abruptly terminates
     * due to an uncaught exception. If the returned value is {@code null},
     * there is no default.
     *
     * @return the default uncaught exception handler for all threads
     *
     * @see #setDefaultUncaughtExceptionHandler
     * @since 1.5
     */
    // 返回当前线程内[默认的]未捕获异常处理器
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtExceptionHandler;
    }
    
    /**
     * Set the default handler invoked when a thread abruptly terminates
     * due to an uncaught exception, and no other handler has been defined
     * for that thread.
     *
     * <p>Uncaught exception handling is controlled first by the thread, then
     * by the thread's {@link ThreadGroup} object and finally by the default
     * uncaught exception handler. If the thread does not have an explicit
     * uncaught exception handler set, and the thread's thread group
     * (including parent thread groups)  does not specialize its
     * {@code uncaughtException} method, then the default handler's
     * {@code uncaughtException} method will be invoked.
     * <p>By setting the default uncaught exception handler, an application
     * can change the way in which uncaught exceptions are handled (such as
     * logging to a specific device, or file) for those threads that would
     * already accept whatever &quot;default&quot; behavior the system
     * provided.
     *
     * <p>Note that the default uncaught exception handler should not usually
     * defer to the thread's {@code ThreadGroup} object, as that could cause
     * infinite recursion.
     *
     * @param eh the object to use as the default uncaught exception handler.
     *           If {@code null} then there is no default handler.
     *
     * @throws SecurityException if a security manager is present and it denies
     *                           {@link RuntimePermission}{@code ("setDefaultUncaughtExceptionHandler")}
     * @see #setUncaughtExceptionHandler
     * @see #getUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    // 向当前线程注册[默认的]未捕获异常处理器
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(new RuntimePermission("setDefaultUncaughtExceptionHandler"));
        }
        
        defaultUncaughtExceptionHandler = eh;
    }
    
    /**
     * Returns the handler invoked when this thread abruptly terminates
     * due to an uncaught exception. If this thread has not had an
     * uncaught exception handler explicitly set then this thread's
     * {@code ThreadGroup} object is returned, unless this thread
     * has terminated, in which case {@code null} is returned.
     *
     * @return the uncaught exception handler for this thread
     *
     * @since 1.5
     */
    // 获取注册的未捕获异常处理器
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        if(uncaughtExceptionHandler != null) {
            // 如果显式设置过未捕获异常处理器，这里直接返回
            return uncaughtExceptionHandler;
        }
        
        /*
         * 如果没有设置未捕获异常处理器，则返回当前线程所处的线程组
         *
         * 线程组本身也实现了UncaughtExceptionHandler接口，
         * 在线程组中，它会通过获取到该线程[默认的]的未捕获异常处理器，
         * 然后再调用其回调方法。
         */
        return group;
    }
    
    /**
     * Set the handler invoked when this thread abruptly terminates
     * due to an uncaught exception.
     * <p>A thread can take full control of how it responds to uncaught
     * exceptions by having its uncaught exception handler explicitly set.
     * If no such handler is set then the thread's {@code ThreadGroup}
     * object acts as its handler.
     *
     * @param eh the object to use as this thread's uncaught exception
     *           handler. If {@code null} then this thread has no explicit handler.
     *
     * @throws SecurityException if the current thread is not allowed to
     *                           modify this thread.
     * @see #setDefaultUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    // 向当前线程注册默认的未捕获异常处理器
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        checkAccess();
        uncaughtExceptionHandler = eh;
    }
    
    /**
     * Dispatch an uncaught exception to the handler. This method is
     * intended to be called only by the JVM.
     */
    // 很关键的一步：当前线程内出现未捕获异常时，JVM会调用此方法
    private void dispatchUncaughtException(Throwable e) {
        // 获取未捕获异常处理器
        UncaughtExceptionHandler handler = getUncaughtExceptionHandler();
        // 处理未捕获的异常
        handler.uncaughtException(this, e);
    }
    
    /*▲ 未捕获异常处理器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns {@code true} if and only if the current thread holds the
     * monitor lock on the specified object.
     *
     * <p>This method is designed to allow a program to assert that
     * the current thread already holds a specified lock:
     * <pre>
     *     assert Thread.holdsLock(obj);
     * </pre>
     *
     * @param obj the object on which to test lock ownership
     *
     * @return {@code true} if the current thread holds the monitor lock on
     * the specified object.
     *
     * @throws NullPointerException if obj is {@code null}
     * @since 1.4
     */
    // 判断是否只有当前线程持有obj锁
    public static native boolean holdsLock(Object obj);
    
    /**
     * Returns an estimate of the number of active threads in the current
     * thread's {@linkplain java.lang.ThreadGroup thread group} and its
     * subgroups. Recursively iterates over all subgroups in the current
     * thread's thread group.
     *
     * <p> The value returned is only an estimate because the number of
     * threads may change dynamically while this method traverses internal
     * data structures, and might be affected by the presence of certain
     * system threads. This method is intended primarily for debugging
     * and monitoring purposes.
     *
     * @return an estimate of the number of active threads in the current
     * thread's thread group and in any other thread group that
     * has the current thread's thread group as an ancestor
     */
    // 递归获取当前线程所在线程组的所有线程数量（可能与实际数量有出入，因为线程数量动态变化），建议仅用作监视目的
    public static int activeCount() {
        return currentThread().getThreadGroup().activeCount();
    }
    
    /**
     * Copies into the specified array every active thread in the current
     * thread's thread group and its subgroups. This method simply
     * invokes the {@link java.lang.ThreadGroup#enumerate(Thread[])}
     * method of the current thread's thread group.
     *
     * <p> An application might use the {@linkplain #activeCount activeCount}
     * method to get an estimate of how big the array should be, however
     * <i>if the array is too short to hold all the threads, the extra threads
     * are silently ignored.</i>  If it is critical to obtain every active
     * thread in the current thread's thread group and its subgroups, the
     * invoker should verify that the returned int value is strictly less
     * than the length of {@code tarray}.
     *
     * <p> Due to the inherent race condition in this method, it is recommended
     * that the method only be used for debugging and monitoring purposes.
     *
     * @param tarray an array into which to put the list of threads
     *
     * @return the number of threads put into the array
     *
     * @throws SecurityException if {@link java.lang.ThreadGroup#checkAccess} determines that
     *                           the current thread cannot access its thread group
     */
    // 递归获取当前线程所在线程组的所有线程（可能与实际状态有出入，因为线程数量动态变化），建议仅用作监视目的
    public static int enumerate(Thread tarray[]) {
        return currentThread().getThreadGroup().enumerate(tarray);
    }
    
    /**
     * Prints a stack trace of the current thread to the standard error stream.
     * This method is used only for debugging.
     */
    // 生成一个异常栈信息，仅用作测试
    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }
    
    // 获取当前JVM内所有线程
    private static native Thread[] getThreads();
    
    /**
     * Indicates that the caller is momentarily unable to progress, until the
     * occurrence of one or more actions on the part of other activities. By
     * invoking this method within each iteration of a spin-wait loop construct,
     * the calling thread indicates to the runtime that it is busy-waiting.
     * The runtime may take action to improve the performance of invoking
     * spin-wait loop constructions.
     *
     * @apiNote As an example consider a method in a class that spins in a loop until
     * some flag is set outside of that method. A call to the {@code onSpinWait}
     * method should be placed inside the spin loop.
     * <pre>{@code
     *     class EventHandler {
     *         volatile boolean eventNotificationNotReceived;
     *         void waitForEventAndHandleIt() {
     *             while ( eventNotificationNotReceived ) {
     *                 java.lang.Thread.onSpinWait();
     *             }
     *             readAndProcessEvent();
     *         }
     *
     *         void readAndProcessEvent() {
     *             // Read event from some source and process it
     *              . . .
     *         }
     *     }
     * }</pre>
     * <p>
     * The code above would remain correct even if the {@code onSpinWait}
     * method was not called at all. However on some architectures the Java
     * Virtual Machine may issue the processor instructions to address such
     * code patterns in a more beneficial way.
     * @since 9
     */
    // 标记线程处于忙等待(busy-waiting)状态，减小线程上下文切换的开销，参见StampedLock
    @HotSpotIntrinsicCandidate
    public static void onSpinWait() {
    }
    
    /**
     * Counts the number of stack frames in this thread. The thread must
     * be suspended.
     *
     * @return the number of stack frames in this thread.
     *
     * @throws IllegalThreadStateException if this thread is not
     *                                     suspended.
     * @see StackWalker
     * @deprecated The definition of this call depends on {@link #suspend},
     * which is deprecated.  Further, the results of this call
     * were never well-defined.
     * This method is subject to removal in a future version of Java SE.
     */
    @Deprecated(since = "1.2", forRemoval = true)
    public native int countStackFrames();
    
    /**
     * Removes from the specified map any keys that have been enqueued on the specified reference queue.
     */
    static void processQueue(ReferenceQueue<Class<?>> queue, ConcurrentMap<? extends WeakReference<Class<?>>, ?> map) {
        Reference<? extends Class<?>> ref;
        while((ref = queue.poll()) != null) {
            map.remove(ref);
        }
    }
    
    /**
     * Performs reflective checks on given subclass to verify that it doesn't
     * override security-sensitive non-final methods.  Returns true if the
     * subclass overrides any of the methods, false otherwise.
     */
    private static boolean auditSubclass(final Class<?> subcl) {
        Boolean result = AccessController.doPrivileged(new PrivilegedAction<>() {
            public Boolean run() {
                for(Class<?> cl = subcl; cl != Thread.class; cl = cl.getSuperclass()) {
                    try {
                        cl.getDeclaredMethod("getContextClassLoader", new Class<?>[0]);
                        return Boolean.TRUE;
                    } catch(NoSuchMethodException ex) {
                    }
                    try {
                        Class<?>[] params = {ClassLoader.class};
                        cl.getDeclaredMethod("setContextClassLoader", params);
                        return Boolean.TRUE;
                    } catch(NoSuchMethodException ex) {
                    }
                }
                return Boolean.FALSE;
            }
        });
        return result;
    }
    
    /**
     * Verifies that this (possibly subclass) instance can be constructed without violating security constraints:
     * the subclass must not override security-sensitive non-final methods,
     * or else the "enableContextClassLoaderOverride" RuntimePermission is checked.
     */
    private static boolean isCCLOverridden(Class<?> cl) {
        if(cl == Thread.class) {
            return false;
        }
        
        processQueue(Caches.subclassAuditsQueue, Caches.subclassAudits);
        
        WeakClassKey key = new WeakClassKey(cl, Caches.subclassAuditsQueue);
        
        Boolean result = Caches.subclassAudits.get(key);
        if(result == null) {
            result = auditSubclass(cl);
            Caches.subclassAudits.putIfAbsent(key, result);
        }
        
        return result;
    }
    
    /**
     * Determines if the currently running thread has permission to
     * modify this thread.
     * <p>
     * If there is a security manager, its {@code checkAccess} method
     * is called with this thread as its argument. This may result in
     * throwing a {@code SecurityException}.
     *
     * @throws SecurityException if the current thread is not allowed to
     *                           access this thread.
     * @see SecurityManager#checkAccess(Thread)
     */
    public final void checkAccess() {
        SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkAccess(this);
        }
    }
    
    
    /**
     * This method is called by the system to give a Thread
     * a chance to clean up before it actually exits.
     */
    private void exit() {
        if(threadLocals != null // 存在ThreadLocal键值对
            && TerminatingThreadLocal.REGISTRY.isPresent()) {   // 存在TerminatingThreadLocal类型的键
            /*
             * 如果在当前线程中注册过TerminatingThreadLocal类型的键，这里就会体现出来
             * TerminatingThreadLocal.REGISTRY是TerminatingThreadLocal的内部属性，其本质是一个关联了【容器】的ThreadLocal
             * 该【容器】内存储了当前线程内注册的所有TerminatingThreadLocal
             *
             * 此处使用TerminatingThreadLocal中的回调，在线程结束前，
             * 对TerminatingThreadLocal关联的值做一些收尾操作
             */
            TerminatingThreadLocal.threadTerminated();
        }
        if(group != null) {
            group.threadTerminated(this);
            group = null;
        }
        /* Aggressively null out all reference fields: see bug 4006245 */
        target = null;
        /* Speed the release of some of these resources */
        threadLocals = null;
        inheritableThreadLocals = null;
        inheritedAccessControlContext = null;
        blocker = null;
        uncaughtExceptionHandler = null;
    }
    
    
    
    /**
     * Returns a string representation of this thread, including the
     * thread's name, priority, and thread group.
     *
     * @return a string representation of this thread.
     */
    public String toString() {
        ThreadGroup group = getThreadGroup();
        if(group != null) {
            return "Thread[" + getName() + "," + getPriority() + "," + group.getName() + "]";
        } else {
            return "Thread[" + getName() + "," + getPriority() + "," + "" + "]";
        }
    }
    
    /**
     * Throws CloneNotSupportedException as a Thread can not be meaningfully
     * cloned. Construct a new Thread instead.
     *
     * @throws CloneNotSupportedException always
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    
    
    /** Make sure registerNatives is the first thing <clinit> does. */
    private static native void registerNatives();
    
    /* Some private helper methods */
    private native void setPriority0(int newPriority);
    private native void start0();
    private native void stop0(Object o);
    private native void suspend0();
    private native void resume0();
    private native void interrupt0();
    private native void setNativeName(String name);
    
    /**
     * Tests if some Thread has been interrupted.
     * The interrupted state is reset or not based on the value of ClearInterrupted that is passed.
     */
    @HotSpotIntrinsicCandidate
    private native boolean isInterrupted(boolean ClearInterrupted);
    
    
    
    /**
     * A thread state.  A thread can be in one of the following states:
     * <ul>
     * <li>{@link #NEW}<br>
     * A thread that has not yet started is in this state.
     * </li>
     * <li>{@link #RUNNABLE}<br>
     * A thread executing in the Java virtual machine is in this state.
     * </li>
     * <li>{@link #BLOCKED}<br>
     * A thread that is blocked waiting for a monitor lock
     * is in this state.
     * </li>
     * <li>{@link #WAITING}<br>
     * A thread that is waiting indefinitely for another thread to
     * perform a particular action is in this state.
     * </li>
     * <li>{@link #TIMED_WAITING}<br>
     * A thread that is waiting for another thread to perform an action
     * for up to a specified waiting time is in this state.
     * </li>
     * <li>{@link #TERMINATED}<br>
     * A thread that has exited is in this state.
     * </li>
     * </ul>
     *
     * <p>
     * A thread can be in only one state at a given point in time.
     * These states are virtual machine states which do not reflect
     * any operating system thread states.
     *
     * @see #getState
     * @since 1.5
     */
    /*
     * 线程状态标记
     *
     *     ●
     *     ↓
     *    NEW
     *     ↓       ┌→ WAITING
     *     ↓       |     ↓
     *  RUNNABLE ←-┼→ BLOCKED
     *     ↓       |     ↑
     *     ↓       └→ TIMED_WAITING
     * TERMINATED
     *
     *
     * 6种状态，10个转换：
     * -------------------------
     *  1.NEW      -> RUNNABLE
     *  2.RUNNABLE -> TERMINATED
     * -------------------------
     *  3.RUNNABLE -> BLOCKED
     *  4.BLOCKED  -> RUNNABLE
     * -------------------------
     *  5.RUNNABLE -> WAITING
     *  6.WAITING  -> RUNNABLE
     * -------------------------
     *  7.RUNNABLE      -> TIMED_WAITING
     *  8.TIMED_WAITING -> RUNNABLE
     * -------------------------
     *  9.WAITING       -> BLOCKED
     * 10.TIMED_WAITING -> BLOCKED
     * -------------------------
     */
    public enum State {
        /**
         * Thread state for a thread which has not yet started.
         */
        // 尚未启动的线程
        NEW,
        
        /**
         * Thread state for a runnable thread.  A thread in the runnable
         * state is executing in the Java virtual machine but it may
         * be waiting for other resources from the operating system
         * such as processor.
         */
        // 正在执行的线程
        RUNNABLE,
        
        /**
         * Thread state for a thread blocked waiting for a monitor lock.
         * A thread in the blocked state is waiting for a monitor lock to enter a synchronized block/method
         * or reenter a synchronized block/method after calling {@link Object#wait() Object.wait}.
         */
        /*
         * 阻塞状态，正在积极争取获得锁的使用权
         *
         * 可能的情形：
         * 1. 一开始就没抢到锁，但一直在等待机会
         * 2. 抢到了锁，但是调用了Object#wait()，随后又被notify唤醒，进入抢锁状态
         * 3. 抢到了锁，但是调用了Object#wait(long)，随后或者被notify唤醒，或者超时后自动醒来，然后进入抢锁状态
         */
        BLOCKED,
        
        /**
         * Thread state for a waiting thread.
         * A thread is in the waiting state due to calling one of the following methods:
         * <ul>
         * <li>{@link Object#wait() Object.wait} with no timeout</li>
         * <li>{@link #join() Thread.join} with no timeout</li>
         * <li>{@link LockSupport#park() LockSupport.park}</li>
         * </ul>
         *
         * <p>A thread in the waiting state is waiting for another thread to perform a particular action.
         *
         * For example, a thread that has called {@code Object.wait()} on an object is waiting for another thread to call {@code Object.notify()} or {@code Object.notifyAll()} on that object.
         * A thread that has called {@code Thread.join()} is waiting for a specified thread to terminate.
         */
        /*
         * 等待状态，正在等待被唤醒
         *
         * 可能的情形：
         * 1. 抢到了锁，但是调用了Object#wait()，进入了漫长的等待。如果中途被notify唤醒唤醒，则进入RUNNABLE或BLOCKED状态
         * 2. 调用了join()方法，join的内部实现也是wait
         * 3. 调用了LockSupport#park()方法
         */
        WAITING,
        
        /**
         * Thread state for a waiting thread with a specified waiting time.
         * A thread is in the timed waiting state due to calling one of
         * the following methods with a specified positive waiting time:
         * <ul>
         * <li>{@link #sleep(long) Thread.sleep}</li>
         * <li>{@link Object#wait(long) Object.wait} with timeout</li>
         * <li>{@link #join(long) Thread.join} with timeout</li>
         * <li>{@link LockSupport#parkNanos LockSupport.parkNanos}</li>
         * <li>{@link LockSupport#parkUntil LockSupport.parkUntil}</li>
         * </ul>
         */
        /*
         * 带有时间间隔的等待状态，正在等待自己醒来或被唤醒
         *
         * 可能的情形：
         * Thread.sleep(long)
         * Object#wait(long)
         * join(long)
         * LockSupport#parkNanos(Object, long)
         * LockSupport#parkUntil(Object, long)
         */
        TIMED_WAITING,
        
        /**
         * Thread state for a terminated thread.
         * The thread has completed execution.
         */
        // 线程已执行完动作并结束
        TERMINATED;
    }
    
    /**
     * Interface for handlers invoked when a {@code Thread} abruptly
     * terminates due to an uncaught exception.
     * <p>When a thread is about to terminate due to an uncaught exception
     * the Java Virtual Machine will query the thread for its
     * {@code UncaughtExceptionHandler} using
     * {@link #getUncaughtExceptionHandler} and will invoke the handler's
     * {@code uncaughtException} method, passing the thread and the
     * exception as arguments.
     * If a thread has not had its {@code UncaughtExceptionHandler}
     * explicitly set, then its {@code ThreadGroup} object acts as its
     * {@code UncaughtExceptionHandler}. If the {@code ThreadGroup} object
     * has no
     * special requirements for dealing with the exception, it can forward
     * the invocation to the {@linkplain #getDefaultUncaughtExceptionHandler
     * default uncaught exception handler}.
     *
     * @see #setDefaultUncaughtExceptionHandler
     * @see #setUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    // 未捕获异常处理接口
    @FunctionalInterface
    public interface UncaughtExceptionHandler {
        /**
         * Method invoked when the given thread terminates due to the given uncaught exception.
         * Any exception thrown by this method will be ignored by the Java Virtual Machine.
         *
         * @param t the thread
         * @param e the exception
         */
        // JVM检测到未捕获异常时的回调方法
        void uncaughtException(Thread t, Throwable e);
    }
    
    /**
     * Weak key for Class objects.
     **/
    static class WeakClassKey extends WeakReference<Class<?>> {
        /**
         * saved value of the referent's identity hash code, to maintain
         * a consistent hash code after the referent has been cleared
         */
        private final int hash;
        
        /**
         * Create a new WeakClassKey to the given object, registered
         * with a queue.
         */
        WeakClassKey(Class<?> cl, ReferenceQueue<Class<?>> refQueue) {
            super(cl, refQueue);
            hash = System.identityHashCode(cl);
        }
        
        /**
         * Returns the identity hash code of the original referent.
         */
        @Override
        public int hashCode() {
            return hash;
        }
        
        /**
         * Returns true if the given object is this identical
         * WeakClassKey instance, or, if this object's referent has not
         * been cleared, if the given object is another WeakClassKey
         * instance with the identical non-null referent as this one.
         */
        @Override
        public boolean equals(Object obj) {
            if(obj == this)
                return true;
            
            if(obj instanceof WeakClassKey) {
                Object referent = get();
                return (referent != null) && (referent == ((WeakClassKey) obj).get());
            } else {
                return false;
            }
        }
    }
    
    /**
     * cache of subclass security audit results. Replace with ConcurrentReferenceHashMap when/if it appears in a future
     * release
     */
    private static class Caches {
        /** cache of subclass security audit results */
        static final ConcurrentMap<WeakClassKey, Boolean> subclassAudits = new ConcurrentHashMap<>();
        
        /** queue for WeakReferences to audited subclasses */
        static final ReferenceQueue<Class<?>> subclassAuditsQueue = new ReferenceQueue<>();
    }
}
